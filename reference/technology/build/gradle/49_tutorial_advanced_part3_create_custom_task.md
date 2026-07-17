# Gradle 고급 튜토리얼 3편: 커스텀 태스크 만들기

> **원문:** https://docs.gradle.org/current/userguide/part3_create_custom_task.html

## 개요

2편에서는 `SlackExtension`으로 사용자 설정값을 받는 구조를 만들었지만, 실제로 그 값을 소비해 무언가를 실행하는 로직은 아직 없었습니다. 이번 편에서는 Slack 메시지를 전송하는 실제 동작을 담은 **커스텀 태스크 타입** `SlackTask`를 만들고, 이를 플러그인의 `apply()`에 등록해 사용자가 `./gradlew sendTestSlackMessage` 명령으로 실행할 수 있도록 연결합니다.

## 왜 커스텀 태스크 타입이 필요한가

- 플러그인이 새로운 기능(여기서는 "Slack 알림 보내기")을 제공하려면, 그 로직을 재사용 가능하고 설정 가능한 하나의 태스크 타입으로 캡슐화하는 것이 일반적입니다.
- 로직을 태스크 클래스 안에 가둬두면, 플러그인은 이를 등록만 하면 되고 사용자는 세부 구현을 몰라도 태스크를 실행할 수 있습니다.
- `doLast { }` 같은 인라인 액션 대신 전용 클래스를 만들면 입력값 검증, 어노테이션 기반 메타데이터(`@Input` 등), 단위 테스트 작성이 훨씬 쉬워집니다.

## 1단계: SlackTask 클래스 정의

`DefaultTask`를 상속하는 추상 클래스로 태스크를 정의하고, 토큰·채널·메시지를 입력 프로퍼티로 선언합니다.

```kotlin
abstract class SlackTask : DefaultTask() {
    @get:Input
    abstract val token: Property<String>
    @get:Input
    abstract val channel: Property<String>
    @get:Input
    abstract val message: Property<String>

    @TaskAction
    fun send() {
        val slack = Slack.getInstance()
        val methods = slack.methods(token.get())
        val request = ChatPostMessageRequest.builder()
            .channel(channel.get())
            .text(message.get())
            .build()

        val response = methods.chatPostMessage(request)
        if (!response.isOk) {
            throw RuntimeException("Slack message failed: ${response.error}")
        }
    }
}
```

- `@Input`은 이 프로퍼티가 태스크의 입력값임을 Gradle에 알려주는 어노테이션입니다. 입력·출력을 명시하면 Gradle의 증분 빌드나 캐싱 판단에 활용될 수 있습니다.
- 실제 동작은 `@TaskAction`이 붙은 메서드에만 작성합니다. 이 메서드는 태스크가 실행될 때 정확히 한 번 호출되는 지점입니다.
- 실패 처리를 명시적으로 해주는 것이 중요합니다. 외부 API(Slack) 호출이 실패하면 예외를 던져 빌드 자체를 실패시켜야, 사용자가 조용히 넘어가는 문제를 방지할 수 있습니다.

## 2단계: 외부 라이브러리 의존성 추가

`SlackTask`가 Slack Web API 클라이언트를 사용하므로, 플러그인 프로젝트의 `build.gradle(.kts)`에 해당 라이브러리를 `implementation` 의존성으로 추가해야 합니다.

```kotlin
dependencies {
    implementation("com.slack.api:slack-api-client:1.45.3")
}
```

- 플러그인 자체도 결국 하나의 JVM 프로젝트이므로, 태스크 로직이 필요로 하는 외부 라이브러리는 이렇게 일반적인 의존성 선언으로 끌어옵니다.

## 3단계: 플러그인에 태스크 등록하기

`SlackPlugin.apply()` 안에서 `tasks.register(...)`로 `SlackTask` 타입의 태스크를 등록하고, 2편에서 만든 확장의 값을 태스크 프로퍼티에 연결합니다.

```kotlin
override fun apply(project: Project) {
    val extension = project.extensions.create("slack", SlackExtension::class.java)

    val taskProvider = project.tasks.register("sendTestSlackMessage", SlackTask::class.java)
    taskProvider.configure {
        it.group = "notification"
        it.description = "Sends a test message to Slack using the configured token and channel."

        it.token.set(extension.token)
        it.channel.set(extension.channel)
        it.message.set(extension.message)
    }
}
```

- `tasks.register("이름", 클래스)`는 태스크를 즉시 생성하지 않고 지연 등록(lazy registration)한다. 실제로 실행되거나 설정이 필요해지는 시점까지 인스턴스화를 미룰 수 있어 불필요한 태스크 생성 비용을 줄인다.
- `group`과 `description`을 지정해두면 `./gradlew tasks` 같은 도움말 출력에서 사용자가 태스크를 쉽게 찾을 수 있다.
- 확장 프로퍼티(`extension.token` 등)를 태스크 프로퍼티에 `.set(...)`으로 그대로 연결하는 것이 핵심이다. `Property<T>`끼리는 값 자체가 아니라 프로바이더(provider) 관계로 연결되므로, 사용자가 빌드 스크립트에서 `slack { }` 블록 값을 나중에 바꿔도 태스크 실행 시점에 최신 값이 반영된다.

## 값이 흐르는 전체 그림

1. 사용자가 빌드 스크립트에서 `slack { token = ...; channel = ...; message = ... }`로 값을 채운다.
2. `SlackPlugin.apply()`에서 이 확장 인스턴스를 태스크 프로퍼티에 연결해둔다.
3. 사용자가 `./gradlew sendTestSlackMessage`를 실행하면 `SlackTask.send()`가 호출되고, 그 시점에 `.get()`으로 확장 값이 확정되어 Slack API로 전달된다.

플러그인 클래스는 스스로 API를 호출하지 않는다. "설정(Extension) → 연결(Plugin) → 실행(Task)"의 역할 분리를 유지하는 것이 이번 편의 핵심 설계다.

## 핵심 포인트

- 플러그인이 제공하는 기능은 `doLast` 같은 즉석 코드가 아니라, `DefaultTask`를 상속한 전용 클래스로 캡슐화하는 것이 재사용성과 테스트 용이성 면에서 유리하다.
- 태스크 입력은 `@Input`(및 `@InputFile`, `@OutputFile` 등) 어노테이션으로 명시해야 Gradle이 증분 빌드·캐싱을 올바르게 판단할 수 있다.
- 태스크가 사용하는 외부 라이브러리는 플러그인 프로젝트의 `build.gradle(.kts)`에 일반 의존성으로 추가한다.
- `tasks.register(...)`로 태스크를 지연 등록하고, `configure { }` 블록에서 확장 프로퍼티를 태스크 프로퍼티에 연결해 "설정과 실행의 분리"를 완성한다.
- 외부 API 실패 등 예외 상황은 태스크 안에서 명시적으로 예외를 던져 빌드를 실패시켜야, 사용자가 문제를 놓치지 않는다.

## 다음 단계

커스텀 태스크와 플러그인 등록까지 마쳤다면, 다음 편인 "[단위 테스트 작성하기](part4_unit_test.html)"에서는 이 `SlackTask`의 동작을 단위 테스트로 검증하는 방법을 다룹니다.
