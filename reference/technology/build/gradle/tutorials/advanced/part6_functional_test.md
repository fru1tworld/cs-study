# 6. 기능 테스트 작성하기

> **원문:** https://docs.gradle.org/current/userguide/part6_functional_test.html

## 개요
지금까지 만든 `slack` 플러그인은 단위 테스트로 클래스 하나만 떼어서 검증했습니다. 이번 단계에서는 TestKit을 이용해 **기능 테스트(functional test)**를 추가합니다. 단위 테스트가 "클래스가 혼자서도 잘 동작하는가"를 본다면, 기능 테스트는 "이 플러그인을 실제 빌드에 적용했을 때 진짜로 원하는 대로 동작하는가"를 확인합니다. 즉, 가짜 프로젝트 디렉터리에 실제 `build.gradle`을 만들고 Gradle을 직접 실행시켜 그 결과(로그, 태스크 성공 여부)를 검증하는 방식입니다.

이 실습은 사전에 1~5단계(프로젝트 초기화, 확장 작성, 커스텀 태스크, 단위 테스트, DataFlow 액션 적용)가 끝나 있다는 전제로 진행됩니다.

## 1. 기존 스캐폴딩 테스트 손보기
`gradle init`으로 플러그인 프로젝트를 만들면 이름이 `PluginTutorialPluginFunctionalTest`인 기능 테스트 파일이 자동으로 생성되어 있습니다. 이 파일을 플러그인 이름에 맞춰 `SlackPluginFunctionalTest`로 바꾸고 내용을 아래처럼 다시 작성합니다.

```kotlin
class SlackPluginFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    @Test fun `can run task`() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins { id('org.example.slack') }
            slack {
                token.set(System.getenv("SLACK_TOKEN"))
                channel.set("#social")
                message.set("Hello from Gradle!")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .apply { forwardOutput(); withPluginClasspath(); withProjectDir(projectDir) }
            .build()

        assertTrue(result.output.contains("Slack message sent successfully"))
    }
}
```

- `@TempDir`로 테스트마다 새로 생기는 임시 디렉터리를 프로젝트 루트로 사용한다.
- 그 안에 빈 `settings.gradle`과, 플러그인을 적용하면서 `slack { ... }` 확장에 값을 채운 `build.gradle`을 직접 써 넣는다.
- `GradleRunner`로 실제 Gradle 실행을 흉내 낸다. `withPluginClasspath()`를 호출해야 테스트 중인 플러그인 코드가 클래스패스에 실려 임시 빌드에서도 인식된다. `forwardOutput()`은 실행 로그를 테스트 콘솔에 그대로 흘려보내 디버깅을 돕는다.
- 마지막으로 `result.output`(빌드 로그 문자열)에 원하는 메시지가 들어 있는지 검증한다.

Groovy DSL(Spock)로 작성할 때도 뼈대는 동일합니다. `@TempDir`/`given-when-then` 블록만 문법이 다를 뿐, "임시 디렉터리에 빌드 스크립트를 쓰고 → `GradleRunner`로 실행하고 → 출력을 검증한다"는 흐름은 그대로입니다.

## 2. 이 테스트가 검증하는 것
단위 테스트로는 확인할 수 없던, 실제 사용자 관점의 시나리오를 검증한다는 점이 핵심입니다.

- 플러그인이 `plugins { id(...) }`로 실제 빌드에 정상 적용되는가
- 사용자가 `slack { }` 확장 블록으로 넣은 값(`token`, `channel`, `message`)이 태스크까지 그대로 전달되는가
- 태스크를 실행했을 때 실제로 원하는 동작(Slack 메시지 전송)이 일어나고, 그 결과가 로그에 남는가

이런 점에서 기능 테스트는 사실상 "이 플러그인을 가져다 쓰는 소비자 프로젝트를 흉내 낸 최소한의 통합 테스트"라고 볼 수 있습니다.

## 3. 기능 테스트 소스셋과 태스크 연결
기능 테스트 코드는 `src/functionalTest/...`처럼 별도 소스셋에 위치하며, `build.gradle.kts`에는 이를 인식시키는 설정이 이미 준비되어 있습니다(`gradle init`이 스캐폴딩해줌).

```kotlin
val functionalTestSourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named("check") { dependsOn(functionalTest) }
```

- 새 소스셋(`functionalTest`)을 만들고, 그 의존성 설정이 `test` 계열 설정을 이어받도록(`extendsFrom`) 연결해 테스트 프레임워크 등 공통 의존성을 중복 선언하지 않게 한다.
- 이 소스셋만 실행하는 전용 `Test` 태스크(`functionalTest`)를 등록한다.
- `gradlePlugin.testSourceSets.add(...)`로 등록하면 `java-gradle-plugin`의 플러그인 검증(validation) 대상에도 포함된다.
- `check` 태스크가 `functionalTest`에 의존하게 만들어, `./gradlew check` 한 번으로 단위 테스트와 기능 테스트가 모두 실행되게 한다.

## 4. 실행 방법
Slack 플러그인은 실제 토큰이 필요하므로, 테스트 실행 전에 환경 변수를 설정해야 합니다.

```bash
export SLACK_TOKEN="xoxb-..."
```

이후 아래 명령으로 기능 테스트만 따로 돌리거나, 단위 테스트까지 한꺼번에 돌릴 수 있습니다.

```bash
./gradlew :functionalTest   # 기능 테스트만
./gradlew check             # 단위 테스트 + 기능 테스트
```

## 핵심 포인트
- 기능 테스트는 임시 프로젝트 디렉터리에 실제 빌드 스크립트를 만들고 `GradleRunner`로 진짜 Gradle을 실행시켜, 플러그인이 "실전에서" 기대대로 동작하는지 검증한다.
- `GradleRunner`를 쓸 때는 `withPluginClasspath()`로 테스트 중인 플러그인 코드를 클래스패스에 포함시키는 것이 필수이며, `forwardOutput()`은 디버깅용 로그 확인에 유용하다.
- 검증은 태스크 성공 여부뿐 아니라 `result.output`에 담긴 빌드 로그 문자열로도 이루어져, 확장 블록에 넣은 값이 실제 동작(예: 메시지 전송)까지 이어지는지 확인할 수 있다.
- `functionalTest`라는 별도 소스셋과 전용 태스크를 두고 `check`에 연결해두면, 단위 테스트와 기능 테스트를 한 번의 `./gradlew check`로 함께 실행할 수 있다.
