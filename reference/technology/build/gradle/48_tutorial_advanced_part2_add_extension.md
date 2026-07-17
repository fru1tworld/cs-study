# Gradle 고급 튜토리얼 2편: 플러그인에 확장(Extension) 추가하기

> **원문:** https://docs.gradle.org/current/userguide/part2_add_extension.html

## 개요

1편에서 만든 플러그인 프로젝트 뼈대에 이번에는 **확장(Extension)**을 붙여봅니다. 확장은 사용자가 빌드 스크립트에서 아래처럼 DSL 블록으로 플러그인을 설정할 수 있게 해주는 장치입니다.

```kotlin
slack {
    token = "..."
    channel = "#builds"
    message = "Build completed!"
}
```

이 `slack { }` 블록은 내부적으로 프로퍼티를 가진 Kotlin/Groovy 클래스에 매핑되고, Gradle이 그 값을 플러그인 로직에 주입해줍니다. 예제는 Slack으로 메시지를 보내는 가상의 플러그인을 계속 확장해 나가는 방식으로 진행됩니다.

## 왜 확장이 필요한가

- 확장이 없으면 플러그인 설정값을 메서드 호출이나 하드코딩으로 넘겨야 해서 빌드 스크립트가 지저분해집니다.
- 확장을 쓰면 "설정(무엇을)"과 "동작(어떻게)"이 분리되어, 사용자는 선언적인 블록만 채우고 실제 처리는 플러그인 내부에 맡길 수 있습니다.
- 확장 프로퍼티를 `Property<T>` 타입으로 선언하면 값 평가가 지연되고, Configuration Cache와도 자연스럽게 호환됩니다.

## 1단계: 플러그인 클래스 이름 바꾸기

1편에서 생성된 템플릿 클래스 `PluginTutorialPlugin`을 실제 목적에 맞게 `SlackPlugin`으로 바꿉니다.

- `PluginTutorialPlugin.kt` (또는 `.groovy`) → `SlackPlugin.kt`
- 클래스 선언에 `abstract` 키워드를 추가

```kotlin
abstract class SlackPlugin : Plugin<Project> {}
```

- `plugin/build.gradle.kts`의 `gradlePlugin { }` 블록에서 `implementationClass`도 `org.example.SlackPlugin`으로 함께 갱신합니다.
- 플러그인 클래스를 `abstract`로 두는 이유는, 이후 Gradle이 클래스를 데코레이션해 확장·태스크와 같은 방식으로 필요한 기능을 런타임에 주입할 수 있게 하기 위함입니다.

## 2단계: 확장 클래스 정의하기

사용자가 설정할 값의 "모양"을 나타내는 `SlackExtension` 클래스를 새로 만듭니다.

```kotlin
abstract class SlackExtension {
    abstract val token: Property<String>
    abstract val channel: Property<String>
    abstract val message: Property<String>
}
```

- 클래스와 프로퍼티를 모두 `abstract`로 선언하면, Gradle이 런타임에 구현체를 생성해주는 **관리형 프로퍼티(managed property)**가 됩니다. 즉, getter/setter나 `Property<T>` 인스턴스 생성 코드를 직접 작성할 필요가 없습니다.
- `Property<String>`으로 선언했기 때문에 값이 실제로 필요해지는 시점까지 평가가 미뤄지는 지연 평가(lazy evaluation)가 적용됩니다.

## 3단계: 플러그인에서 확장 등록하기

`apply()` 메서드 안에서 `extensions.create(...)`로 확장을 등록하고, 등록된 확장 인스턴스를 그대로 활용해 태스크를 구성합니다.

```kotlin
override fun apply(project: Project) {
    val extension = project.extensions.create(
        "slack", SlackExtension::class.java
    )

    project.tasks.register("sendTestSlackMessage") {
        it.doLast {
            println("${extension.message.get()} to ${extension.channel.get()}")
        }
    }
}
```

- 첫 번째 인자 `"slack"`은 빌드 스크립트에서 쓰일 DSL 블록 이름을 결정합니다. 이 이름이 곧 사용자가 작성하는 `slack { }` 블록명이 됩니다.
- `extensions.create(...)`가 반환하는 객체를 그대로 `apply()` 지역 변수로 잡아두면, 같은 메서드에서 등록하는 태스크 설정에 바로 활용할 수 있습니다.

## 확장 값이 흘러가는 흐름

1. 사용자가 빌드 스크립트에서 `slack { token = ...; channel = ...; message = ... }`를 작성한다.
2. Gradle은 이 블록을 플러그인이 등록해둔 `SlackExtension` 인스턴스의 프로퍼티에 매핑한다.
3. 태스크 실행 로직(`doLast { }` 등) 안에서 `extension.message.get()`처럼 `.get()`을 호출하는 순간에야 실제 값이 확정된다.

이 순서 덕분에 확장 설정과 태스크 실행 시점이 분리되고, 값 확정이 실행 단계까지 미뤄져 Configuration Cache와도 문제없이 맞물립니다.

## 핵심 포인트

- 확장은 "빌드 스크립트 DSL 블록 ↔ 프로퍼티를 가진 클래스"를 연결하는 다리이며, 플러그인 설정을 선언적으로 만들어준다.
- 확장 클래스와 그 프로퍼티를 `abstract` + `Property<T>`로 선언하면 Gradle이 구현체 생성과 지연 평가를 대신 처리해준다(관리형 프로퍼티).
- `project.extensions.create("이름", 클래스)`에서 지정한 "이름"이 곧 사용자가 쓰게 될 DSL 블록 이름이 된다.
- 확장에 담긴 값은 `.get()`을 호출하는 시점(보통 태스크 실행 시)까지 평가가 미뤄지므로, 설정 시점과 실행 시점을 분리해 생각하는 것이 핵심이다.

## 다음 단계

확장으로 사용자 설정을 받는 방법을 익혔다면, 다음 편인 "[커스텀 태스크 만들기](part3_create_custom_task.html)"에서는 이 값들을 실제로 소비하는 전용 태스크 클래스를 만들어봅니다.
