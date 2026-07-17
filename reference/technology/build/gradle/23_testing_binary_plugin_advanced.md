# Gradle 바이너리 플러그인 테스트하기

> **원문:** https://docs.gradle.org/current/userguide/testing_binary_plugin_advanced.html

## 개요
플러그인도 결국 코드이므로 검증이 필요합니다. Gradle은 플러그인 전용 테스트 도구인 **TestKit**을 제공해, 플러그인이 적용된 상태의 "진짜" 빌드를 프로그램적으로 실행하고 결과를 검증할 수 있게 해줍니다. 잘 만든 플러그인은 보통 두 단계로 테스트를 나눕니다.

- **단위 테스트** — 클래스 하나만 떼어서 빠르게 검증
- **기능 테스트** — 실제 빌드를 임시 디렉터리에서 돌려보며 통째로 검증

이전 문서에서 만든 두 파일 크기를 비교하는 `filesizediff` 플러그인을 예로 들면, 소스 트리는 다음과 같이 테스트 코드 영역이 추가된 모습이 됩니다.

```
plugin/
├── build.gradle.kts
└── src/
    ├── main/java/org/example/          (플러그인 본체)
    ├── test/java/org/example/          (단위 테스트)
    └── functionalTest/java/org/example/ (기능 테스트)
```

## 1. 단위 테스트 — `ProjectBuilder`
단위 테스트는 전체 Gradle 실행 없이, 가벼운 가상 프로젝트 위에서 플러그인 로직만 검증합니다. 확인하는 내용은 대개 두 가지입니다.

- 플러그인이 에러 없이 적용되는가
- 태스크·확장이 의도한 이름으로 등록되는가

```java
Project project = ProjectBuilder.builder().build();
project.getPlugins().apply("org.example.filesizediff");

assertNotNull(project.getTasks().findByName("fileSizeDiff"));
```

`ProjectBuilder`가 만들어주는 프로젝트는 실제 빌드를 실행하지 않는 "적용 대상"일 뿐이라, 태스크 실행 결과(출력 파일 등)까지는 검증하지 못합니다. 그래서 등록 여부 같은 기계적인 부분만 빠르게 확인하는 용도로 씁니다.

## 2. 기능 테스트 — TestKit과 `GradleRunner`
기능 테스트(End-to-End 테스트)는 임시 디렉터리에 실제 `build.gradle`을 만들고, `GradleRunner`로 진짜 Gradle 빌드를 실행해 결과를 확인합니다. 단위 테스트로는 볼 수 없는 부분, 즉 다음을 검증합니다.

- 플러그인이 실제 빌드에 정상 적용되는가
- 태스크가 실행되어 기대한 출력을 만드는가
- 사용자가 DSL(확장 블록)로 값을 넣었을 때 그대로 반영되는가

```java
@TempDir File projectDir;

@BeforeEach
void setup() throws IOException {
    writeString(new File(projectDir, "settings.gradle"), "");
    writeString(new File(projectDir, "build.gradle"), """
        plugins { id("org.example.filesizediff") }
        diff { file1 = file("a.txt"); file2 = file("b.txt") }
    """);
}

@Test
void canDiffTwoFilesOfTheSameSize() throws IOException {
    BuildResult result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments("fileSizeDiff")
        .build();

    assertTrue(result.getOutput().contains("Files have the same size"));
    assertEquals(TaskOutcome.SUCCESS, result.task(":fileSizeDiff").getOutcome());
}
```

- `@TempDir`는 테스트마다 새 임시 폴더를 만들고 종료 후 자동으로 지워준다.
- `@BeforeEach`에서 최소한의 `settings.gradle`/`build.gradle`을 미리 써두고, 각 테스트는 그 위에 입력 파일만 얹어 시나리오를 구성한다.
- `GradleRunner`는 실제 Gradle 실행을 흉내 내는 API로, `withProjectDir()`(대상 디렉터리) → `withPluginClasspath()`(테스트 중인 플러그인 코드를 클래스패스에 주입) → `withArguments()`(실행할 태스크) → `build()`(실행 및 결과 반환) 순서로 조립한다.
- 결과는 `BuildResult`로 돌아오며, 로그 출력 문자열과 `TaskOutcome`(SUCCESS/FAILED/UP_TO_DATE 등)을 함께 검증할 수 있다.

## 기능 테스트를 위한 소스셋 구성
Gradle은 `functionalTest`처럼 이름을 붙인 커스텀 소스셋을 자동으로 인식하지 않으므로, 빌드 스크립트에 직접 등록해야 합니다. 다행히 `gradle init`으로 플러그인 프로젝트를 만들면 이 설정을 자동으로 만들어 줍니다.

```kotlin
plugins {
    `java-gradle-plugin`
}

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

- 새 소스셋(`functionalTest`)을 만들고, 의존성 설정(`configurations`)이 기존 `test` 계열 설정을 이어받도록(`extendsFrom`) 연결한다.
- 그 소스셋을 실행할 `Test` 타입 태스크를 별도로 등록하고, `check` 태스크가 이 태스크에도 의존하게 만들어 `./gradlew check` 한 번으로 단위 테스트와 기능 테스트가 모두 돌게 한다.
- `gradlePlugin.testSourceSets.add(...)`로 등록해두면, `java-gradle-plugin`이 플러그인 검증(validation) 대상에도 이 소스셋을 포함시킨다.

## 소비자 프로젝트로 한 번 더 확인하기 (선택)
TestKit 기반 테스트 외에, 플러그인을 실제로 "가져다 쓰는" 별도 프로젝트를 만들어 눈으로 직접 확인해볼 수도 있습니다. `includeBuild()`로 플러그인 프로젝트를 복합 빌드에 끌어와 적용합니다.

```kotlin
// settings.gradle.kts
rootProject.name = "consumer"
includeBuild("plugin")
```

```kotlin
// build.gradle.kts
plugins { id("org.example.filesizediff") }
diff { file1 = file("a.txt"); file2 = file("b.txt") }
```

이후 `a.txt`, `b.txt`를 실제로 만들어두고 `./gradlew fileSizeDiff`를 실행하면, 콘솔에 찍히는 로그로 플러그인이 실제 환경에서 기대대로 동작하는지 확인할 수 있습니다. 자동화된 테스트를 대체하는 것은 아니지만, 개발 중 빠르게 손으로 찔러보는 용도로 유용합니다.

## 핵심 포인트
- 플러그인 테스트는 **단위 테스트(`ProjectBuilder`)**로 빠르게 등록 여부를 확인하고, **기능 테스트(TestKit `GradleRunner`)**로 실제 빌드 동작까지 검증하는 이중 구조가 기본이다.
- `GradleRunner`는 `withProjectDir → withPluginClasspath → withArguments → build()` 순으로 조립해 임시 디렉터리에 진짜 빌드를 실행시키는 API다.
- `functionalTest`처럼 커스텀 소스셋을 쓰려면 소스셋 생성, 설정 상속(`extendsFrom`), 전용 `Test` 태스크 등록, `check`와의 연결까지 직접 잡아줘야 하며 `gradle init`이 이를 스캐폴딩해준다.
- `includeBuild()`로 연결한 별도의 소비자 프로젝트는 자동화 테스트를 대체하지 않지만, 실제 사용 환경에서 플러그인을 손으로 검증하는 보조 수단이 된다.
