# Gradle 어드밴스드 튜토리얼 5편: 데이터플로우 액션 추가하기

> **원문:** https://docs.gradle.org/current/userguide/part5_add_dataflow_action.html

## 개요

지금까지 만든 `SlackPlugin`은 사용자가 `sendTestSlackMessage` 태스크를 직접 실행해야만 Slack 알림을 보낼 수 있었다. 5편에서는 여기서 한 단계 나아가, "빌드가 끝나면 자동으로" 알림을 보내는 기능을 추가한다. 핵심은 Gradle이 제공하는 **데이터플로우 API(Dataflow API)** 를 이용해 빌드 생명주기(lifecycle)에 훅을 거는 것이다.

## 선행 조건

1~4편에서 다룬 내용이 전제가 된다.

- 1편: 플러그인 프로젝트 초기화
- 2편: 확장(extension) 추가
- 3편: 커스텀 태스크 작성
- 4편: 단위 테스트 작성

## 왜 `BuildListener` 대신 데이터플로우 API인가

빌드가 끝난 뒤 특정 동작을 실행하고 싶을 때, 예전에는 `BuildListener` 같은 리스너 API를 직접 등록하는 방식을 썼다. 하지만 이런 방식은 **Configuration Cache**와 호환되지 않는다는 문제가 있다. 즉 매 빌드마다 설정 단계를 다시 수행해야 하므로 캐시로 얻을 수 있는 속도 이점을 포기해야 한다.

**데이터플로우 액션(Dataflow Action)** 은 이 문제를 해결하기 위해 등장한 최신 방식으로, 선언적(declarative)이고 격리된(isolated) 단위로 동작을 기술하기 때문에 Configuration Cache와 완전히 호환된다. "빌드가 끝났다"처럼 특정 이벤트에 반응해 실행되는 지연(lazy) 작업 단위라고 이해하면 된다.

## 데이터플로우 API의 세 가지 구성 요소

| 구성 요소 | 역할 |
|---|---|
| **`FlowScope`** | 어떤 조건에서 `FlowAction`을 실행할지 등록하는 서비스 |
| **`FlowAction<T>`** | 실제로 실행될 로직을 구현하는 인터페이스 |
| **`FlowParameters`** | `FlowAction`에 전달되는 입력값을 담는 컨테이너 |

플러그인 클래스에 `FlowScope`와 `FlowProviders`를 주입받아, "언제(scope) 무엇을(action, parameters) 실행할지"를 연결하는 구조다.

## 1. FlowAction 구현 — `SlackBuildFlowAction`

빌드 종료 시 Slack 메시지를 보내는 로직을 `FlowAction<Params>`를 구현하는 클래스로 작성한다.

```kotlin
abstract class SlackBuildFlowAction : FlowAction<SlackBuildFlowAction.Params> {

    interface Params : FlowParameters {
        @get:Input val token: Property<String>
        @get:Input val channel: Property<String>
        @get:Input val buildFailed: Property<Boolean>
    }

    override fun execute(parameters: Params) {
        val status = if (parameters.buildFailed.get()) "Build failed" else "Build succeeded"
        // Slack API로 status 메시지 전송
    }
}
```

- **`Params` 인터페이스**: `FlowParameters`를 확장해 이 액션이 필요로 하는 입력(`token`, `channel`, `buildFailed`)을 정의한다. 태스크의 `@Input` 속성 선언과 마찬가지로, 각 값을 `@get:Input`으로 표시한다.
- **`execute(parameters)`**: 액션이 실행되는 시점에 호출되는 유일한 메서드다. 여기서 `buildFailed` 값에 따라 성공/실패 메시지를 구성하고 Slack API로 전송한다.
- 이 클래스는 아직 "언제 실행되는지", "파라미터 값이 어디서 오는지"는 모른다 — 그건 다음 단계에서 플러그인이 채워준다.

## 2. 플러그인에서 FlowAction 등록하기

`SlackPlugin`의 `apply()`에 두 서비스를 주입받아 액션을 등록한다.

```kotlin
abstract class SlackPlugin : Plugin<Project> {
    @Inject abstract fun getFlowScope(): FlowScope
    @Inject abstract fun getFlowProviders(): FlowProviders

    override fun apply(project: Project) {
        val extension = project.extensions.create("slack", SlackExtension::class.java)
        // ... 태스크 등록은 3편과 동일 ...

        getFlowScope().always(SlackBuildFlowAction::class.java) { spec ->
            spec.parameters.token.set(extension.token)
            spec.parameters.channel.set(extension.channel)
            spec.parameters.buildFailed.set(
                getFlowProviders().buildWorkResult.map { it.failure.isPresent }
            )
        }
    }
}
```

- **`getFlowScope().always(...)`**: `always` 스코프로 등록하면 빌드가 성공하든 실패하든 **항상** 이 액션이 실행된다. 등록 시점에 람다로 `spec.parameters`를 채워, 확장(`extension`)에 담긴 값을 액션의 입력으로 연결한다.
- **`getFlowProviders().buildWorkResult`**: 빌드 결과(성공/실패 여부)를 제공하는 프로바이더다. `.map { it.failure.isPresent }`로 "실패 여부" 불(boolean) 값을 뽑아내 `buildFailed` 파라미터에 지연 바인딩한다.
- 결과적으로 태스크 등록(3편)과 확장 정의(2편)는 그대로 두고, `apply()` 끝에 "빌드가 끝나면 이 액션을 실행하라"는 등록 코드만 추가하는 형태다.

## 태스크 실행과 데이터플로우 액션의 차이

- `sendTestSlackMessage` 태스크: 사용자가 `./gradlew sendTestSlackMessage`로 **명시적으로 호출**해야 실행된다.
- `SlackBuildFlowAction`: 태스크 실행 여부와 무관하게, **빌드가 끝나는 순간 자동으로** 실행된다.

즉 같은 플러그인 안에 "수동으로 트리거하는 알림"과 "빌드 결과에 반응하는 자동 알림"이라는 두 가지 통지 경로가 공존하게 된다.

## 핵심 포인트

- `BuildListener` 같은 옛 리스너 API는 Configuration Cache와 호환되지 않아 캐시 이점을 깎아먹는다. 빌드 생명주기에 반응하는 로직은 데이터플로우 API(`FlowAction`)로 작성하는 것이 최신 권장 방식이다.
- 데이터플로우 액션은 `FlowScope`(언제 실행할지 등록) · `FlowAction<T>`(실행 로직) · `FlowParameters`(입력값 컨테이너) 세 요소로 구성되며, 이는 바이너리 플러그인의 Plugin·Task·Extension 3분할 구조와 유사한 관심사 분리다.
- `FlowAction` 자체는 파라미터가 어디서 오는지 모르며, 플러그인의 `apply()`에서 `flowScope.always(...) { spec -> ... }` 형태로 등록하면서 파라미터 값을 채워 넣는 책임을 진다.
- `FlowProviders.buildWorkResult`처럼 빌드 결과를 지연 평가로 제공하는 프로바이더를 활용하면, 빌드가 끝난 뒤의 성공/실패 상태를 액션 파라미터로 안전하게 흘려보낼 수 있다.
