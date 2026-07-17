# Gradle 테스트 모범 사례 (Best Practices for Testing)

> **원문:** https://docs.gradle.org/current/userguide/best_practices_testing.html

## 개요

커스텀 태스크와 플러그인은 대개 `build.gradle(.kts)` 안에 인라인으로 작성하며 프로토타이핑을 시작합니다. 기능이 안정되면 `buildSrc`나 별도의 플러그인 프로젝트로 뽑아내야 하는데, 그 순간부터 TestKit을 이용한 **기능 테스트(functional test)**가 가능해집니다. 이 문서는 단 하나의 규칙, "커스텀 태스크·플러그인은 TestKit으로 테스트하라"를 하나의 예시로 깊게 다룹니다.

## 규칙: 커스텀 태스크·플러그인은 TestKit으로 검증하라

- **Do**: 코드가 `build.gradle` 밖으로(=`buildSrc` 또는 독립 플러그인 프로젝트로) 나가는 즉시, TestKit 기반 기능 테스트를 세트로 갖춘다.
- **Do**: 성숙한 빌드는 커스텀 타입이 "선언대로" 동작함을 보장하는 기능 테스트를 반드시 포함한다.
- **Don't**: 인라인 상태로 남겨둔 채 "동작하는 것처럼 보이니 괜찮다"고 넘기지 않는다. 캐시 가능성·입력 선언·와이어링 실수는 눈으로 봐서는 잘 드러나지 않는다.

## Don't: 문제가 있는 예시

아래처럼 `build.gradle.kts`에 인라인으로 정의한 태스크·플러그인에는 테스트 없이는 잡기 힘든 버그가 네 가지 섞여 있습니다.

```kotlin
@CacheableTask                       // (1) 매번 다른 결과인데 캐시 가능 표시
abstract class MyTask : DefaultTask() {
    private final val today = Instant.now()   // (2) 선언되지 않은 입력값
    // ...
}

project.tasks.withType<MyTask>().configureEach {
    lastName.convention(extension.firstName)   // (3) firstName을 잘못 연결
    // ...
}

tasks.named<MyTask>("task2") {
    greeter = "Bonjour"               // (4) 태스크 프로퍼티가 아닌 빌드스크립트 변수에 대입
}
```

1. **비결정적 출력에 `@CacheableTask`**: 실행 시각에 따라 결과가 달라지는데도 캐시 대상으로 표시했다.
2. **현재 시각을 선언되지 않은 입력으로 사용**: `@Input`으로 선언하지 않으면 Gradle이 변경 여부를 추적할 수 없다.
3. **프로퍼티 와이어링 실수**: `lastName`이 확장의 `firstName`에 연결되어 있다.
4. **엉뚱한 변수에 대입**: 태스크의 `greeting` 프로퍼티가 아니라 빌드스크립트 지역 변수 `greeter`에 값을 넣었다.

이런 실수는 코드 리뷰만으로 발견하기 어렵고, 기능 테스트를 작성하는 과정에서 자연스럽게 드러납니다.

## Do: build-logic + functionalTest 구조로 분리

- **Do**: 커스텀 타입을 `build-logic`이라는 포함 빌드(composite build)로 뽑아내고, `main`과 별도로 `functionalTest` 소스셋을 둔다.
- **Do**: 시간처럼 비결정적인 값은 `@Input`으로 선언하고, 플러그인 설정(`configureEach`) 안에서 `Instant.now()`를 단 한 번만 호출해 모든 태스크에 동일한 값을 준다.
- **Do**: 프로퍼티 와이어링(`lastName.convention(extension.getLastName())`)이 실제 의도한 필드를 가리키는지 테스트로 고정한다.

```
build-logic/
├── src/main/java/...        (MyTask, MyPlugin 본체)
└── src/functionalTest/java/... (MyPluginFunctionalTest)
```

TestKit 테스트는 느리고 무거운 편이라 단위 테스트와 뒤섞지 않고 `functionalTest`라는 전용 소스셋으로 분리하는 것이 관례입니다.

## functionalTest 소스셋 구성

`JvmTestSuite`로 소스셋을 만들고 `gradlePlugin`에 등록해야 `java-gradle-plugin`이 플러그인 코드를 테스트 클래스패스에 자동으로 얹어줍니다.

```kotlin
val functionalTest = testing.suites.register("functionalTest", JvmTestSuite::class) {
    useJUnitJupiter()
    dependencies {
        implementation(project())
        implementation(gradleTestKit())
    }
}

tasks.check { dependsOn(functionalTest) }

gradlePlugin {
    testSourceSets(functionalTest.get().sources)
}
```

- **Don't**: `build-logic`(포함 빌드)의 테스트가 루트 프로젝트의 `check`를 실행하면 같이 돌 것이라고 가정하지 않는다. 포함 빌드는 산출물만 제공할 뿐 내부 테스트는 자동 실행되지 않으므로, `./gradlew :build-logic:check`처럼 명시적으로 호출해야 한다.

## 기능 테스트 시나리오 네 가지

`GradleRunner`로 임시 프로젝트를 만들어 실제 빌드를 돌리며, 아래 네 관점을 각각 검증합니다.

| 테스트 | 확인 대상 |
|---|---|
| `testTaskRegistration` | `tasks --all` 결과에 태스크가 의도한 그룹·이름으로 나타나는가 |
| `testTaskExecution` | 태스크를 실행하면 출력 파일에 기대한 내용이 쓰이는가 |
| `testTaskDeterminism` | 같은 입력으로 두 번 실행(`--rerun-tasks`)해도 출력이 동일한가 |
| `testTaskCacheability` | 빌드 캐시를 켠 채 두 번째 실행이 `FROM_CACHE`로 뜨는가 |

```java
BuildResult result = GradleRunner.create()
    .withProjectDir(testProjectDir)
    .withPluginClasspath()
    .withArguments("--build-cache", "task1")
    .build();

assertEquals(TaskOutcome.FROM_CACHE, result.task(":task1").getOutcome());
```

캐시 검증은 여기서 더 나아가, 입력값을 바꿨을 때 실제로 재실행되는지(relocatability 포함)까지 확인하면 더 견고해집니다.

## 핵심 포인트

- 커스텀 타입은 "인라인 프로토타입 → `buildSrc`/독립 플러그인으로 추출 → TestKit 기능 테스트 추가"라는 자연스러운 성숙 경로를 따른다.
- 캐시 가능(`@CacheableTask`) 선언, 입력값의 `@Input` 명시, 프로퍼티 와이어링, 변수 대입 실수는 코드만 봐서는 놓치기 쉽고 기능 테스트가 가장 효과적으로 잡아낸다.
- 기능 테스트는 무겁기 때문에 `functionalTest` 같은 전용 소스셋으로 분리하고, `java-gradle-plugin`의 `testSourceSets`에 등록해 클래스패스를 자동으로 연결한다.
- 포함 빌드(`includeBuild`)의 테스트는 루트 프로젝트 `check`에 묶이지 않으므로 `:build-logic:check`처럼 명시적으로 실행해야 한다.
- 등록 여부, 실행 결과, 결정성(determinism), 캐시 가능성(cacheability)까지 네 겹으로 검증하는 것이 TestKit 기반 테스트의 기본 틀이다.
