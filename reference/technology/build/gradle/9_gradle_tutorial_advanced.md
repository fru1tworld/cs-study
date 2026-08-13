# 튜토리얼: 고급 (Advanced)

## Gradle 고급 튜토리얼 1편: 플러그인 프로젝트 초기화

> 원문: https://docs.gradle.org/current/userguide/part1_gradle_init_plugin.html

### 개요

고급 튜토리얼 시리즈는 앞선 초급·중급 편에서 다룬 "빌드를 사용하는 법"에서 한 걸음 나아가, "빌드 로직을 재사용 가능한 플러그인으로 직접 작성하는 법"을 다룸. 1편에서는 실습 예제로 빌드 결과를 Slack 채널에 알려주는 커스텀 플러그인을 목표로 잡고, 그 첫 단계인 프로젝트 초기화와 "Hello World" 수준의 플러그인 뼈대를 만드는 과정을 살펴봄.

### 사전 준비물

- Gradle이 로컬에 설치되어 있어야 함 (`gradle -v`로 버전 확인)
- IntelliJ IDEA Community Edition — 플러그인 코드 탐색 및 작성용

### 1) 플러그인 프로젝트 초기화

일반 애플리케이션이 아닌 플러그인을 만드는 것이므로 `gradle init`의 타입 옵션이 달라짐.

```bash
$ mkdir plugin-tutorial
$ cd plugin-tutorial
$ gradle init --type kotlin-gradle-plugin --dsl kotlin
```

- Groovy로 작성하려면 `--type groovy-gradle-plugin --dsl groovy`를 사용
- `--type` 값이 `java-application`이 아니라 `*-gradle-plugin`인 점이 이전 튜토리얼과의 핵심 차이

### 2) 생성된 디렉터리 구조

```text
plugin-tutorial/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── gradlew, gradlew.bat
├── settings.gradle.kts
└── plugin/
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin(또는 groovy)/
        ├── test/kotlin(또는 groovy)/
        └── functionalTest/kotlin(또는 groovy)/
```

- 서브프로젝트 이름이 `app`이 아니라 `plugin`으로 생성됨
- `test` 외에 `functionalTest` 소스 세트가 별도로 생성됨 → 단위 테스트와 "플러그인을 실제 빌드에 적용해보는" 통합 테스트를 구분해서 작성하도록 스캐폴딩됨
- 나머지 wrapper·버전 카탈로그 구조는 이전 애플리케이션 프로젝트와 동일

### 3) `plugin/build.gradle.kts` 살펴보기

```kotlin
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

gradlePlugin {
    plugins.create("greeting") {
        id = "org.example.greeting"
        implementationClass = "org.example.PluginTutorialPlugin"
    }
}
```

- `java-gradle-plugin` 플러그인이 핵심: 플러그인 메타데이터(`id` → 구현 클래스 매핑)를 자동 생성 → 플러그인 개발에 특화된 컨벤션·검증 태스크, 게시(publish) 편의 기능까지 함께 제공
- `gradlePlugin { plugins.create(...) }` 블록에서 플러그인 ID와 진입점 클래스를 연결
  - `id`: 다른 빌드 스크립트에서 `plugins { id("org.example.greeting") }`로 적용할 때 쓰는 식별자
  - `implementationClass`: 실제 로직이 담긴 클래스의 정규화된 이름

### 4) "Hello World" 플러그인 코드

```kotlin
class PluginTutorialPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'org.example.greeting'")
            }
        }
    }
}
```

- 모든 Gradle 플러그인은 `Plugin<Project>` 인터페이스를 구현해야 함
- `apply(project: Project)`가 플러그인의 진입점 — 이 빌드에 플러그인이 적용되는 순간 Gradle이 호출
- 내부에서 `project.tasks.register("greeting") { ... }`로 새 태스크를 등록 → `doLast { }` 블록에 실행할 로직(여기서는 콘솔 출력)을 넣음
- 즉 플러그인의 본질은 "프로젝트 객체를 받아 태스크·확장·설정을 추가하는 코드"라는 점을 이 최소 예제로 확인 가능

### 핵심 포인트: 플러그인은 어떻게 동작을 검증하는가

- 다른 빌드 스크립트에서 `plugins { id("org.example.greeting") }`로 플러그인을 적용하면, `apply()`가 실행되며 `greeting` 태스크가 등록됨
- `./gradlew greeting`을 실행하면 `Hello from plugin 'org.example.greeting'`이 출력됨
- 이 흐름은 이후 만들 모든 커스텀 태스크·확장(extension)에도 그대로 적용되는 기본 검증 패턴: "플러그인 적용 → 태스크 등록 확인 → 실행해서 동작 확인"

### 5) 이번 튜토리얼의 목표: Slack 알림 플러그인

이 시리즈에서 최종적으로 완성할 플러그인은 다음 기능을 갖춤.

- `sendTestSlackMessage` 같은 태스크로 임의의 Slack 메시지 전송
- 빌드가 끝났을 때 성공/실패 여부를 자동으로 Slack에 보고
- `FlowAction` 등 최신 Gradle API를 활용해 빌드 생명주기(lifecycle) 종료 시점에 훅을 걸어 동작

### 6) Slack API 준비 작업

플러그인이 실제로 메시지를 보내려면 Slack 쪽 설정이 먼저 필요함.

1. Slack 워크스페이스 관리자 권한 확보
2. Slack 앱 생성 페이지에서 앱 매니페스트로 앱 생성
3. `chat:write` OAuth 스코프 권한 추가
4. `xoxb-`로 시작하는 봇 토큰 발급 및 안전하게 보관

- 이 토큰이 이후 플러그인 코드에서 Slack API를 호출할 때 인증 수단으로 사용됨

### 다음 단계

이 편에서는 플러그인 프로젝트를 초기화하고 "Hello World" 수준의 최소 플러그인으로 태스크 등록·적용·실행 흐름을 확인함. 다음 편 "확장(Extension) 추가하기"에서는 빌드 스크립트에서 플러그인 동작을 설정할 수 있도록 확장 객체를 도입하는 과정을 다룸.

---

## Gradle 고급 튜토리얼 2편: 플러그인에 확장(Extension) 추가하기

> 원문: https://docs.gradle.org/current/userguide/part2_add_extension.html

### 개요

1편에서 만든 플러그인 프로젝트 뼈대에 이번에는 확장(Extension)을 붙임. 확장은 사용자가 빌드 스크립트에서 아래처럼 DSL 블록으로 플러그인을 설정할 수 있게 해주는 장치.

```kotlin
slack {
    token = "..."
    channel = "#builds"
    message = "Build completed!"
}
```

이 `slack { }` 블록은 내부적으로 프로퍼티를 가진 Kotlin/Groovy 클래스에 매핑되고, Gradle이 그 값을 플러그인 로직에 주입함. 예제는 Slack으로 메시지를 보내는 가상의 플러그인을 계속 확장해 나가는 방식으로 진행.

### 왜 확장이 필요한가

- 확장이 없으면 플러그인 설정값을 메서드 호출이나 하드코딩으로 넘겨야 함 → 빌드 스크립트가 지저분해짐
- 확장을 쓰면 "설정(무엇을)"과 "동작(어떻게)"이 분리됨 → 사용자는 선언적인 블록만 채우고 실제 처리는 플러그인 내부에 맡길 수 있음
- 확장 프로퍼티를 `Property<T>` 타입으로 선언하면 값 평가가 지연됨 → Configuration Cache와도 자연스럽게 호환

### 1단계: 플러그인 클래스 이름 바꾸기

1편에서 생성된 템플릿 클래스 `PluginTutorialPlugin`을 실제 목적에 맞게 `SlackPlugin`으로 바꿈.

- `PluginTutorialPlugin.kt` (또는 `.groovy`) → `SlackPlugin.kt`
- 클래스 선언에 `abstract` 키워드를 추가

```kotlin
abstract class SlackPlugin : Plugin<Project> {}
```

- `plugin/build.gradle.kts`의 `gradlePlugin { }` 블록에서 `implementationClass`도 `org.example.SlackPlugin`으로 함께 갱신
- 플러그인 클래스를 `abstract`로 두는 이유: 이후 Gradle이 클래스를 데코레이션해 확장·태스크와 같은 방식으로 필요한 기능을 런타임에 주입할 수 있게 하기 위함

### 2단계: 확장 클래스 정의하기

사용자가 설정할 값의 "모양"을 나타내는 `SlackExtension` 클래스를 새로 만듦.

```kotlin
abstract class SlackExtension {
    abstract val token: Property<String>
    abstract val channel: Property<String>
    abstract val message: Property<String>
}
```

- 클래스와 프로퍼티를 모두 `abstract`로 선언하면, Gradle이 런타임에 구현체를 생성해주는 관리형 프로퍼티(managed property)가 됨 → getter/setter나 `Property<T>` 인스턴스 생성 코드를 직접 작성할 필요 없음
- `Property<String>`으로 선언했기 때문에 값이 실제로 필요해지는 시점까지 평가가 미뤄지는 지연 평가(lazy evaluation)가 적용됨

### 3단계: 플러그인에서 확장 등록하기

`apply()` 메서드 안에서 `extensions.create(...)`로 확장을 등록하고, 등록된 확장 인스턴스를 그대로 활용해 태스크를 구성함.

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

- 첫 번째 인자 `"slack"`은 빌드 스크립트에서 쓰일 DSL 블록 이름을 결정함 → 이 이름이 곧 사용자가 작성하는 `slack { }` 블록명이 됨
- `extensions.create(...)`가 반환하는 객체를 그대로 `apply()` 지역 변수로 잡아두면, 같은 메서드에서 등록하는 태스크 설정에 바로 활용 가능

### 확장 값이 흘러가는 흐름

1. 사용자가 빌드 스크립트에서 `slack { token = ...; channel = ...; message = ... }`를 작성
2. Gradle이 이 블록을 플러그인이 등록해둔 `SlackExtension` 인스턴스의 프로퍼티에 매핑
3. 태스크 실행 로직(`doLast { }` 등) 안에서 `extension.message.get()`처럼 `.get()`을 호출하는 순간에야 실제 값이 확정

이 순서 덕분에 확장 설정과 태스크 실행 시점이 분리되고, 값 확정이 실행 단계까지 미뤄져 Configuration Cache와도 문제없이 맞물림.

### 핵심 포인트

- 확장은 "빌드 스크립트 DSL 블록 ↔ 프로퍼티를 가진 클래스"를 연결하는 다리 → 플러그인 설정을 선언적으로 만들어줌
- 확장 클래스와 그 프로퍼티를 `abstract` + `Property<T>`로 선언하면 Gradle이 구현체 생성과 지연 평가를 대신 처리(관리형 프로퍼티)
- `project.extensions.create("이름", 클래스)`에서 지정한 "이름"이 곧 사용자가 쓰게 될 DSL 블록 이름
- 확장에 담긴 값은 `.get()`을 호출하는 시점(보통 태스크 실행 시)까지 평가가 미뤄짐 → 설정 시점과 실행 시점을 분리해 생각하는 것이 핵심

### 다음 단계

확장으로 사용자 설정을 받는 방법을 익혔다면, 다음 편인 "[커스텀 태스크 만들기](part3_create_custom_task.html)"에서는 이 값들을 실제로 소비하는 전용 태스크 클래스를 만듦.

---

## Gradle 고급 튜토리얼 3편: 커스텀 태스크 만들기

> 원문: https://docs.gradle.org/current/userguide/part3_create_custom_task.html

### 개요

2편에서는 `SlackExtension`으로 사용자 설정값을 받는 구조를 만들었지만, 실제로 그 값을 소비해 무언가를 실행하는 로직은 아직 없었음. 이번 편에서는 Slack 메시지를 전송하는 실제 동작을 담은 커스텀 태스크 타입 `SlackTask`를 만들고, 이를 플러그인의 `apply()`에 등록해 사용자가 `./gradlew sendTestSlackMessage` 명령으로 실행할 수 있도록 연결함.

### 왜 커스텀 태스크 타입이 필요한가

- 플러그인이 새로운 기능(여기서는 "Slack 알림 보내기")을 제공하려면, 그 로직을 재사용 가능하고 설정 가능한 하나의 태스크 타입으로 캡슐화하는 것이 일반적
- 로직을 태스크 클래스 안에 가둬두면, 플러그인은 이를 등록만 하면 되고 사용자는 세부 구현을 몰라도 태스크를 실행 가능
- `doLast { }` 같은 인라인 액션 대신 전용 클래스를 만들면 입력값 검증, 어노테이션 기반 메타데이터(`@Input` 등), 단위 테스트 작성이 훨씬 쉬워짐

### 1단계: SlackTask 클래스 정의

`DefaultTask`를 상속하는 추상 클래스로 태스크를 정의하고, 토큰·채널·메시지를 입력 프로퍼티로 선언함.

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

- `@Input`은 이 프로퍼티가 태스크의 입력값임을 Gradle에 알려주는 어노테이션 → 입력·출력을 명시하면 Gradle의 증분 빌드나 캐싱 판단에 활용 가능
- 실제 동작은 `@TaskAction`이 붙은 메서드에만 작성 → 이 메서드는 태스크가 실행될 때 정확히 한 번 호출되는 지점
- 실패 처리를 명시적으로 해주는 것이 중요 → 외부 API(Slack) 호출이 실패하면 예외를 던져 빌드 자체를 실패시켜야, 사용자가 조용히 넘어가는 문제를 방지 가능

### 2단계: 외부 라이브러리 의존성 추가

`SlackTask`가 Slack Web API 클라이언트를 사용하므로, 플러그인 프로젝트의 `build.gradle(.kts)`에 해당 라이브러리를 `implementation` 의존성으로 추가 필요.

```kotlin
dependencies {
    implementation("com.slack.api:slack-api-client:1.45.3")
}
```

- 플러그인 자체도 결국 하나의 JVM 프로젝트이므로, 태스크 로직이 필요로 하는 외부 라이브러리는 이렇게 일반적인 의존성 선언으로 끌어옴

### 3단계: 플러그인에 태스크 등록하기

`SlackPlugin.apply()` 안에서 `tasks.register(...)`로 `SlackTask` 타입의 태스크를 등록하고, 2편에서 만든 확장의 값을 태스크 프로퍼티에 연결함.

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

- `tasks.register("이름", 클래스)`는 태스크를 즉시 생성하지 않고 지연 등록(lazy registration) → 실제로 실행되거나 설정이 필요해지는 시점까지 인스턴스화를 미룰 수 있어 불필요한 태스크 생성 비용을 줄임
- `group`과 `description`을 지정해두면 `./gradlew tasks` 같은 도움말 출력에서 사용자가 태스크를 쉽게 찾을 수 있음
- 확장 프로퍼티(`extension.token` 등)를 태스크 프로퍼티에 `.set(...)`으로 그대로 연결하는 것이 핵심 → `Property<T>`끼리는 값 자체가 아니라 프로바이더(provider) 관계로 연결되므로, 사용자가 빌드 스크립트에서 `slack { }` 블록 값을 나중에 바꿔도 태스크 실행 시점에 최신 값이 반영됨

### 값이 흐르는 전체 그림

1. 사용자가 빌드 스크립트에서 `slack { token = ...; channel = ...; message = ... }`로 값을 채움
2. `SlackPlugin.apply()`에서 이 확장 인스턴스를 태스크 프로퍼티에 연결해둠
3. 사용자가 `./gradlew sendTestSlackMessage`를 실행하면 `SlackTask.send()`가 호출되고, 그 시점에 `.get()`으로 확장 값이 확정되어 Slack API로 전달됨

플러그인 클래스는 스스로 API를 호출하지 않음. "설정(Extension) → 연결(Plugin) → 실행(Task)"의 역할 분리를 유지하는 것이 이번 편의 핵심 설계.

### 핵심 포인트

- 플러그인이 제공하는 기능은 `doLast` 같은 즉석 코드가 아니라, `DefaultTask`를 상속한 전용 클래스로 캡슐화하는 것이 재사용성과 테스트 용이성 면에서 유리
- 태스크 입력은 `@Input`(및 `@InputFile`, `@OutputFile` 등) 어노테이션으로 명시해야 Gradle이 증분 빌드·캐싱을 올바르게 판단 가능
- 태스크가 사용하는 외부 라이브러리는 플러그인 프로젝트의 `build.gradle(.kts)`에 일반 의존성으로 추가
- `tasks.register(...)`로 태스크를 지연 등록하고, `configure { }` 블록에서 확장 프로퍼티를 태스크 프로퍼티에 연결해 "설정과 실행의 분리"를 완성
- 외부 API 실패 등 예외 상황은 태스크 안에서 명시적으로 예외를 던져 빌드를 실패시켜야, 사용자가 문제를 놓치지 않음

### 다음 단계

커스텀 태스크와 플러그인 등록까지 마쳤다면, 다음 편인 "[단위 테스트 작성하기](part4_unit_test.html)"에서는 이 `SlackTask`의 동작을 단위 테스트로 검증하는 방법을 다룸.

---

## Gradle 어드밴스드 튜토리얼 4편: 단위 테스트 작성하기

> 원문: https://docs.gradle.org/current/userguide/part4_unit_test.html

### 개요

플러그인도 결국 코드이기 때문에, 배포하기 전에 "제대로 동작하는가"를 검증할 방법이 필요함. 4편은 1~3편에서 만든 확장(extension)과 커스텀 태스크가 실제로 의도대로 등록되는지를 단위 테스트로 확인하는 단계. `gradle init`이 생성해주는 기본 테스트 파일을 출발점으로 삼아, 플러그인 이름에 맞게 다듬고 내용을 보강하는 흐름으로 진행.

### 선행 조건

1~3편에서 다룬 내용이 전제.

- 1편: 플러그인 프로젝트 초기화
- 2편: 확장(extension) 추가
- 3편: 커스텀 태스크 작성

### 기본 테스트 파일부터 시작하기

`gradle init`으로 플러그인 프로젝트를 만들면, 플러그인을 적용하고 태스크 존재 여부를 확인하는 샘플 테스트가 함께 생성됨. 이 샘플은 그대로도 동작하지만, 이름이 프로젝트 초기 템플릿(`PluginTutorialPluginTest`) 그대로라 실제 플러그인과 무관해 보임. 가장 먼저 할 일은 파일명과 클래스명을 지금 만들고 있는 플러그인에 맞게 바꾸는 것.

- Kotlin: `PluginTutorialPluginTest.kt` → `SlackPluginTest.kt`
- Groovy: `PluginTutorialPluginTest.groovy` → `SlackPluginTest.groovy`

### 핵심 도구: `ProjectBuilder`

단위 테스트는 실제 빌드를 실행하지 않고도 플러그인 로직만 떼어서 검증할 수 있게 해주는 `ProjectBuilder`를 중심으로 짜여짐. `ProjectBuilder.builder().build()`는 파일시스템에 실제 빌드 산출물을 만들지 않는, 메모리상의 "가짜" `Project` 객체를 하나 만들어줌. 여기에 플러그인을 적용해보고, 기대한 태스크가 등록됐는지만 확인하면 되므로 실행 속도가 빠르고 테스트가 가벼움.

```kotlin
// Kotlin + kotlin.test
val project = ProjectBuilder.builder().build()
project.plugins.apply("org.example.slack")
assertNotNull(project.tasks.findByName("sendTestSlackMessage"))
```

```groovy
// Groovy + Spock
given:
def project = ProjectBuilder.builder().build()

when:
project.plugins.apply("org.example.slack")

then:
project.tasks.findByName("sendTestSlackMessage") != null
```

두 버전 모두 구조는 동일함.

1. `ProjectBuilder`로 가상 프로젝트를 만듦
2. `project.plugins.apply(...)`로 플러그인을 적용함
3. `project.tasks.findByName(...)`로 태스크가 등록됐는지 확인함

### 이 테스트가 검증하는 것 / 검증하지 못하는 것

- 검증하는 것: 플러그인이 에러 없이 적용되는가, 커스텀 태스크가 기대한 이름으로 등록되는가
- 검증하지 못하는 것: 태스크를 실제로 실행했을 때의 동작이나 결과물 → `ProjectBuilder`가 만드는 프로젝트는 태스크를 등록해볼 수 있는 대상일 뿐, 실제 빌드 실행 파이프라인을 태우지는 않기 때문

즉 이 단계의 테스트는 "플러그인이 프로젝트에 올바르게 연결됐는가"라는 가장 기초적인 전제를 빠르게 확인하는 용도이고, 태스크가 실행됐을 때의 실제 동작까지 확인하려면 다음 편에서 다루는 기능 테스트(functional test, TestKit)가 필요함.

### 테스트 실행

```bash
./gradlew test
```

테스트가 통과하면, 플러그인이 커스텀 태스크를 의도한 대로 등록하고 있다는 뜻.

### 핵심 포인트

- `ProjectBuilder`는 실제 빌드 없이 메모리상의 가상 프로젝트를 만들어, 플러그인 적용과 태스크 등록만 빠르게 검증하는 단위 테스트용 도구
- 테스트 흐름은 "가상 프로젝트 생성 → 플러그인 적용 → `findByName`으로 태스크 존재 확인"의 세 단계로 단순
- 이 수준의 테스트는 태스크가 등록됐는지만 보장하며, 태스크 실행 결과 같은 실제 동작 검증은 다루지 못한다는 한계를 명확히 알아둘 필요
- `gradle init`이 만들어주는 기본 테스트 파일의 이름과 내용을 플러그인에 맞게 고쳐 쓰는 것이 실질적인 첫 작업 → 이후 편에서 다룰 기능 테스트와 짝을 이뤄 플러그인 검증 체계를 완성

---

## Gradle 어드밴스드 튜토리얼 5편: 데이터플로우 액션 추가하기

> 원문: https://docs.gradle.org/current/userguide/part5_add_dataflow_action.html

### 개요

지금까지 만든 `SlackPlugin`은 사용자가 `sendTestSlackMessage` 태스크를 직접 실행해야만 Slack 알림을 보낼 수 있었음. 5편에서는 여기서 한 단계 나아가, "빌드가 끝나면 자동으로" 알림을 보내는 기능을 추가함. 핵심은 Gradle이 제공하는 데이터플로우 API(Dataflow API)를 이용해 빌드 생명주기(lifecycle)에 훅을 거는 것.

### 선행 조건

1~4편에서 다룬 내용이 전제.

- 1편: 플러그인 프로젝트 초기화
- 2편: 확장(extension) 추가
- 3편: 커스텀 태스크 작성
- 4편: 단위 테스트 작성

### 왜 `BuildListener` 대신 데이터플로우 API인가

빌드가 끝난 뒤 특정 동작을 실행하고 싶을 때, 예전에는 `BuildListener` 같은 리스너 API를 직접 등록하는 방식을 씀. 하지만 이런 방식은 Configuration Cache와 호환되지 않는다는 문제가 있음 → 매 빌드마다 설정 단계를 다시 수행해야 하므로 캐시로 얻을 수 있는 속도 이점을 포기해야 함.

데이터플로우 액션(Dataflow Action)은 이 문제를 해결하기 위해 등장한 최신 방식으로, 선언적(declarative)이고 격리된(isolated) 단위로 동작을 기술하기 때문에 Configuration Cache와 완전히 호환됨. "빌드가 끝났다"처럼 특정 이벤트에 반응해 실행되는 지연(lazy) 작업 단위라고 이해하면 됨.

### 데이터플로우 API의 세 가지 구성 요소

- `FlowScope`: 어떤 조건에서 `FlowAction`을 실행할지 등록하는 서비스
- `FlowAction<T>`: 실제로 실행될 로직을 구현하는 인터페이스
- `FlowParameters`: `FlowAction`에 전달되는 입력값을 담는 컨테이너

플러그인 클래스에 `FlowScope`와 `FlowProviders`를 주입받아, "언제(scope) 무엇을(action, parameters) 실행할지"를 연결하는 구조.

### 1. FlowAction 구현 — `SlackBuildFlowAction`

빌드 종료 시 Slack 메시지를 보내는 로직을 `FlowAction<Params>`를 구현하는 클래스로 작성함.

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

- `Params` 인터페이스: `FlowParameters`를 확장해 이 액션이 필요로 하는 입력(`token`, `channel`, `buildFailed`)을 정의함 → 태스크의 `@Input` 속성 선언과 마찬가지로, 각 값을 `@get:Input`으로 표시
- `execute(parameters)`: 액션이 실행되는 시점에 호출되는 유일한 메서드 → 여기서 `buildFailed` 값에 따라 성공/실패 메시지를 구성하고 Slack API로 전송
- 이 클래스는 아직 "언제 실행되는지", "파라미터 값이 어디서 오는지"는 모름 — 그건 다음 단계에서 플러그인이 채워줌

### 2. 플러그인에서 FlowAction 등록하기

`SlackPlugin`의 `apply()`에 두 서비스를 주입받아 액션을 등록함.

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

- `getFlowScope().always(...)`: `always` 스코프로 등록하면 빌드가 성공하든 실패하든 항상 이 액션이 실행됨 → 등록 시점에 람다로 `spec.parameters`를 채워, 확장(`extension`)에 담긴 값을 액션의 입력으로 연결
- `getFlowProviders().buildWorkResult`: 빌드 결과(성공/실패 여부)를 제공하는 프로바이더 → `.map { it.failure.isPresent }`로 "실패 여부" 불(boolean) 값을 뽑아내 `buildFailed` 파라미터에 지연 바인딩
- 결과적으로 태스크 등록(3편)과 확장 정의(2편)는 그대로 두고, `apply()` 끝에 "빌드가 끝나면 이 액션을 실행하라"는 등록 코드만 추가하는 형태

### 태스크 실행과 데이터플로우 액션의 차이

- `sendTestSlackMessage` 태스크: 사용자가 `./gradlew sendTestSlackMessage`로 명시적으로 호출해야 실행
- `SlackBuildFlowAction`: 태스크 실행 여부와 무관하게, 빌드가 끝나는 순간 자동으로 실행

즉 같은 플러그인 안에 "수동으로 트리거하는 알림"과 "빌드 결과에 반응하는 자동 알림"이라는 두 가지 통지 경로가 공존.

### 핵심 포인트

- `BuildListener` 같은 옛 리스너 API는 Configuration Cache와 호환되지 않아 캐시 이점을 깎아먹음 → 빌드 생명주기에 반응하는 로직은 데이터플로우 API(`FlowAction`)로 작성하는 것이 최신 권장 방식
- 데이터플로우 액션은 `FlowScope`(언제 실행할지 등록) · `FlowAction<T>`(실행 로직) · `FlowParameters`(입력값 컨테이너) 세 요소로 구성 → 바이너리 플러그인의 Plugin·Task·Extension 3분할 구조와 유사한 관심사 분리
- `FlowAction` 자체는 파라미터가 어디서 오는지 모름 → 플러그인의 `apply()`에서 `flowScope.always(...) { spec -> ... }` 형태로 등록하면서 파라미터 값을 채워 넣는 책임을 짐
- `FlowProviders.buildWorkResult`처럼 빌드 결과를 지연 평가로 제공하는 프로바이더를 활용하면, 빌드가 끝난 뒤의 성공/실패 상태를 액션 파라미터로 안전하게 흘려보낼 수 있음

---

## 6. 기능 테스트 작성하기

> 원문: https://docs.gradle.org/current/userguide/part6_functional_test.html

### 개요

지금까지 만든 `slack` 플러그인은 단위 테스트로 클래스 하나만 떼어서 검증함. 이번 단계에서는 TestKit을 이용해 기능 테스트(functional test)를 추가함. 단위 테스트가 "클래스가 혼자서도 잘 동작하는가"를 본다면, 기능 테스트는 "이 플러그인을 실제 빌드에 적용했을 때 진짜로 원하는 대로 동작하는가"를 확인함. 즉, 가짜 프로젝트 디렉터리에 실제 `build.gradle`을 만들고 Gradle을 직접 실행시켜 그 결과(로그, 태스크 성공 여부)를 검증하는 방식.

이 실습은 사전에 1~5단계(프로젝트 초기화, 확장 작성, 커스텀 태스크, 단위 테스트, DataFlow 액션 적용)가 끝나 있다는 전제로 진행.

### 1. 기존 스캐폴딩 테스트 손보기

`gradle init`으로 플러그인 프로젝트를 만들면 이름이 `PluginTutorialPluginFunctionalTest`인 기능 테스트 파일이 자동으로 생성됨. 이 파일을 플러그인 이름에 맞춰 `SlackPluginFunctionalTest`로 바꾸고 내용을 아래처럼 다시 작성함.

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

- `@TempDir`로 테스트마다 새로 생기는 임시 디렉터리를 프로젝트 루트로 사용
- 그 안에 빈 `settings.gradle`과, 플러그인을 적용하면서 `slack { ... }` 확장에 값을 채운 `build.gradle`을 직접 써 넣음
- `GradleRunner`로 실제 Gradle 실행을 흉내 냄 → `withPluginClasspath()`를 호출해야 테스트 중인 플러그인 코드가 클래스패스에 실려 임시 빌드에서도 인식됨. `forwardOutput()`은 실행 로그를 테스트 콘솔에 그대로 흘려보내 디버깅을 도움
- 마지막으로 `result.output`(빌드 로그 문자열)에 원하는 메시지가 들어 있는지 검증

Groovy DSL(Spock)로 작성할 때도 뼈대는 동일함. `@TempDir`/`given-when-then` 블록만 문법이 다를 뿐, "임시 디렉터리에 빌드 스크립트를 쓰고 → `GradleRunner`로 실행하고 → 출력을 검증한다"는 흐름은 그대로.

### 2. 이 테스트가 검증하는 것

단위 테스트로는 확인할 수 없던, 실제 사용자 관점의 시나리오를 검증한다는 점이 핵심.

- 플러그인이 `plugins { id(...) }`로 실제 빌드에 정상 적용되는가
- 사용자가 `slack { }` 확장 블록으로 넣은 값(`token`, `channel`, `message`)이 태스크까지 그대로 전달되는가
- 태스크를 실행했을 때 실제로 원하는 동작(Slack 메시지 전송)이 일어나고, 그 결과가 로그에 남는가

이런 점에서 기능 테스트는 사실상 "이 플러그인을 가져다 쓰는 소비자 프로젝트를 흉내 낸 최소한의 통합 테스트"라고 볼 수 있음.

### 3. 기능 테스트 소스셋과 태스크 연결

기능 테스트 코드는 `src/functionalTest/...`처럼 별도 소스셋에 위치하며, `build.gradle.kts`에는 이를 인식시키는 설정이 이미 준비되어 있음(`gradle init`이 스캐폴딩해줌).

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

- 새 소스셋(`functionalTest`)을 만들고, 그 의존성 설정이 `test` 계열 설정을 이어받도록(`extendsFrom`) 연결 → 테스트 프레임워크 등 공통 의존성을 중복 선언하지 않게 함
- 이 소스셋만 실행하는 전용 `Test` 태스크(`functionalTest`)를 등록
- `gradlePlugin.testSourceSets.add(...)`로 등록하면 `java-gradle-plugin`의 플러그인 검증(validation) 대상에도 포함됨
- `check` 태스크가 `functionalTest`에 의존하게 만들어, `./gradlew check` 한 번으로 단위 테스트와 기능 테스트가 모두 실행되게 함

### 4. 실행 방법

Slack 플러그인은 실제 토큰이 필요하므로, 테스트 실행 전에 환경 변수를 설정해야 함.

```bash
export SLACK_TOKEN="xoxb-..."
```

이후 아래 명령으로 기능 테스트만 따로 돌리거나, 단위 테스트까지 한꺼번에 돌릴 수 있음.

```bash
./gradlew :functionalTest   # 기능 테스트만
./gradlew check             # 단위 테스트 + 기능 테스트
```

### 핵심 포인트

- 기능 테스트는 임시 프로젝트 디렉터리에 실제 빌드 스크립트를 만들고 `GradleRunner`로 진짜 Gradle을 실행시켜, 플러그인이 "실전에서" 기대대로 동작하는지 검증
- `GradleRunner`를 쓸 때는 `withPluginClasspath()`로 테스트 중인 플러그인 코드를 클래스패스에 포함시키는 것이 필수이며, `forwardOutput()`은 디버깅용 로그 확인에 유용
- 검증은 태스크 성공 여부뿐 아니라 `result.output`에 담긴 빌드 로그 문자열로도 이루어짐 → 확장 블록에 넣은 값이 실제 동작(예: 메시지 전송)까지 이어지는지 확인 가능
- `functionalTest`라는 별도 소스셋과 전용 태스크를 두고 `check`에 연결해두면, 단위 테스트와 기능 테스트를 한 번의 `./gradlew check`로 함께 실행 가능

---

## Gradle 어드밴스드 튜토리얼 7편: 소비자 프로젝트로 플러그인 검증하기

> 원문: https://docs.gradle.org/current/userguide/part7_use_consumer_project.html

### 개요

1~6편을 거치며 `org.example.slack` 플러그인은 확장(extension), 커스텀 태스크, 단위 테스트, 데이터플로우 액션, 기능 테스트까지 갖춘 상태가 됨. 하지만 지금까지의 검증은 전부 플러그인 프로젝트 "안에서" 이뤄진 것. 7편은 플러그인을 실제로 "가져다 쓰는" 별도의 소비자(consumer) 프로젝트를 만들어, 진짜 사용자가 겪을 환경과 최대한 비슷한 조건에서 플러그인을 검증해보는 단계. Gradle Plugin Portal 같은 저장소에 게시하지 않고도 이런 검증이 가능하도록, Gradle의 복합 빌드(Composite Build) 기능을 사용함.

### 선행 조건

1~6편에서 만든 결과물이 이번 편의 출발점.

- 1편: 플러그인 프로젝트 초기화
- 2편: 확장(extension) 추가
- 3편: 커스텀 태스크 작성
- 4편: 단위 테스트 작성
- 5편: 데이터플로우 액션(`FlowAction`) 추가 — 빌드 종료 시 슬랙 알림 전송
- 6편: 기능 테스트 작성

### 1단계: 소비자 프로젝트 생성과 복합 빌드 구성

먼저 기존 `plugin/` 디렉터리 옆에 `consumer/` 디렉터리를 새로 만듦. `consumer`는 `settings.gradle(.kts)`와 `build.gradle(.kts)`를 자체적으로 가지는, `plugin`과는 독립된 별도 프로젝트.

```
.
├── gradlew
├── settings.gradle.kts
├── plugin/
└── consumer/
    ├── settings.gradle.kts
    └── build.gradle.kts
```

이렇게 두 프로젝트를 나란히 두고, 루트 `settings.gradle.kts`에서 `plugin`을 포함된 빌드(included build)로 등록해 하나의 복합 빌드로 묶음.

```kotlin
// settings.gradle.kts (root)
rootProject.name = "plugin-tutorial"
includeBuild("consumer")
```

아울러 복합 빌드로 묶이면 `plugin` 프로젝트가 루트의 버전 카탈로그(`libs.versions.toml`)에 더 이상 접근할 수 없으므로, `plugin/build.gradle.kts`의 플러그인 ID·의존성 선언을 카탈로그 참조가 아닌 문자열/좌표 직접 명시 방식으로 바꿔줘야 함.

### 2단계: 소비자 프로젝트를 플러그인에 연결하기

`consumer/settings.gradle.kts`에 `pluginManagement { }` 블록을 추가해, 소비자 프로젝트가 `plugins { }` 블록에서 `org.example.slack`을 찾을 때 `plugin` 프로젝트를 참조하도록 지정함.

```kotlin
// consumer/settings.gradle.kts
pluginManagement {
    includeBuild("../plugin")
    repositories {
        gradlePluginPortal()
    }
}
rootProject.name = "consumer"
```

핵심은 `pluginManagement` 안의 `includeBuild("../plugin")`. 이 한 줄로 `plugin` 프로젝트가 제공하는 모든 플러그인이 ID만으로 다른 프로젝트에서 바로 적용 가능해짐.

### 3단계: 소비자 프로젝트에서 플러그인 적용·설정하기

`consumer/build.gradle.kts`에서 평소처럼 `plugins { }` 블록으로 플러그인을 적용하고, 확장 블록(`slack { }`)에 값을 채움.

```kotlin
plugins {
    id("java")
    id("org.example.slack")
}

repositories { mavenCentral() }

slack {
    token.set(System.getenv("SLACK_TOKEN"))
    channel.set("#social")
    message.set("Hello from consumer via composite build!")
}
```

일반적으로 `plugins { }`에서 외부 플러그인을 쓰려면 `id("...") version "..."`처럼 버전을 명시해야 하지만, 여기서는 버전을 적지 않음 → 복합 빌드로 연결된 포함 빌드의 플러그인은 Gradle이 자동으로 해당 소스를 찾아 해석해주기 때문. 즉 게시된 아티팩트의 버전 번호를 신경 쓸 필요 없이, `plugin` 디렉터리의 현재 소스 상태가 곧바로 적용됨.

### 4단계: 소비자 프로젝트 실행으로 동작 확인하기

플러그인이 등록한 커스텀 태스크는 `consumer` 디렉터리에서 직접, 또는 루트에서 전체 경로로 실행 가능.

```bash
$ ./gradlew sendTestSlackMessage            # consumer 디렉터리에서
$ ./gradlew :consumer:sendTestSlackMessage  # 루트 디렉터리에서
$ ./gradlew :consumer:build                 # build 실행 시 FlowAction이 슬랙 알림도 함께 전송
```

`:consumer:build`처럼 표준 라이프사이클 태스크를 돌리면, 5편에서 등록해둔 `FlowAction`이 빌드 종료 시점에 자동으로 실행되어 슬랙 메시지를 보내는 것까지 한 번에 확인 가능.

### 핵심 포인트

- 플러그인을 게시하지 않고도 "실제 사용자처럼" 검증하려면, 별도의 `consumer` 프로젝트를 만들고 복합 빌드(`includeBuild`)로 `plugin` 프로젝트와 엮는 방식이 표준적인 접근
- 연결은 두 군데에서 이뤄짐 — 루트 `settings.gradle.kts`의 `includeBuild("consumer")`(consumer를 복합 빌드에 포함)와, `consumer/settings.gradle.kts`의 `pluginManagement { includeBuild("../plugin") }`(플러그인 ID 해석 경로 지정)
- 복합 빌드로 포함된 플러그인은 `plugins { id("...") }`에서 버전 없이 바로 적용 가능 → Gradle이 포함 빌드의 소스를 그 자리에서 바로 참조하기 때문
- `consumer`에서 태스크를 실행하는 방법은 일반 서브프로젝트와 동일하게 `./gradlew <task>`(해당 디렉터리) 또는 `./gradlew :consumer:<task>`(루트) 두 가지이며, `build`처럼 라이프사이클 태스크를 돌리면 앞서 만든 `FlowAction` 같은 부가 로직도 함께 트리거됨
- 이 방식의 가장 큰 이점은 게시 과정 없이 즉시 반복(iterate) 가능하다는 점 — `plugin` 코드를 수정하면 바로 `consumer`에서 재실행해 결과를 확인 가능. 다만 TestKit 기반 자동화 테스트(4·6편)를 대체하는 것은 아니고, 눈으로 직접 확인하는 보조 수단에 가까움

---

## Gradle 심화 튜토리얼 8장 - 플러그인 로컬 배포

> 원문: https://docs.gradle.org/current/userguide/part8_publish_locally.html

### 개요

지금까지 초기화, 확장(extension) 설계, 커스텀 태스크 작성, 단위·기능 테스트를 거쳐 플러그인을 완성함. 마지막 단계는 이 플러그인이 실제로 배포 가능한 형태인지 검증하는 것. 이 장에서는 플러그인을 로컬 Maven 저장소에 배포한 뒤, 별도의 컨슈머(consumer) 프로젝트에서 마치 외부 저장소에서 받아온 것처럼 소비해 보면서 배포 파이프라인 전체를 눈으로 확인함.

### 1. 배포에 필요한 메타데이터 추가

플러그인 프로젝트의 빌드 스크립트에 두 가지를 추가해야 함.

- `maven-publish` 플러그인 적용 — 기존에 쓰던 `java-gradle-plugin`, 그리고 Kotlin/Groovy 언어 플러그인과 함께 선언
- `group`과 `version` 명시 — 이 두 값이 없으면 `publish` 태스크 자체가 실패함

```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.example"
version = "1.0.0"
```

이 값들은 배포되는 아티팩트를 식별하는 좌표(coordinate) 역할을 하므로, 임의의 값이 아니라 실제로 관리할 버전 체계를 반영해야 함.

### 2. 로컬 폴더로 배포하기

원격 저장소 대신 프로젝트 빌드 디렉터리 하위의 폴더를 배포 대상으로 지정함.

```kotlin
publishing {
    repositories {
        maven {
            name = "localRepo"
            url = layout.buildDirectory.dir("local-repo").get().asFile.toURI()
        }
    }
}
```

배포는 다음 명령으로 실행함.

```bash
./gradlew :plugin:publishAllPublicationsToLocalRepoRepository
```

- 태스크 이름은 `publishAllPublicationsTo<저장소 이름>Repository` 패턴을 따름 → `localRepo`라는 이름을 그대로 반영
- 실행 결과 `plugin/build/local-repo/` 아래에 플러그인 마커, JAR, `pom.xml` 등 표준 Maven 배포물이 생성됨

### 3. 컨슈머 프로젝트에서 로컬 저장소 바라보기

플러그인을 사용하는 별도 프로젝트의 `settings.gradle(.kts)`에서 `pluginManagement.repositories`에 방금 만든 로컬 경로를 추가함.

```kotlin
pluginManagement {
    repositories {
        maven {
            url = file("../plugin/build/local-repo").toURI()
        }
        gradlePluginPortal()
    }
}
```

- 로컬 경로를 먼저 등록하면 Gradle이 플러그인 해석 시 이곳을 우선 탐색함
- `gradlePluginPortal()`은 그다음 순위 → 로컬에 없는 나머지 플러그인은 여전히 정상적으로 받아옴

### 4. 컨슈머 빌드에 플러그인 적용

컨슈머 프로젝트의 `build.gradle(.kts)`에서 방금 배포한 버전을 명시적으로 지정해 플러그인을 적용함.

```kotlin
plugins {
    id("org.example.slack") version "1.0.0"
}
```

이 상태로 컨슈머 프로젝트의 태스크를 실행해 보면, 사내/오픈소스 저장소에서 내려받은 플러그인을 쓸 때와 동일한 방식으로 동작하는지 확인 가능.

### 핵심 포인트

- 플러그인을 로컬 Maven 저장소에 배포해 보는 것은, 실제 배포 전에 "이 플러그인이 외부에서도 정상적으로 설치·적용되는가"를 검증하는 가장 간단한 리허설
- `group`/`version`이 없으면 `publish` 태스크가 실패하므로 배포용 빌드 스크립트에서는 반드시 명시 필요
- 배포 대상 저장소에 붙인 이름(`localRepo`)이 곧 실행할 태스크 이름(`publishAllPublicationsToLocalRepoRepository`)에 그대로 반영됨
- 컨슈머 프로젝트는 `pluginManagement { repositories { ... } }`에 로컬 경로를 추가하는 것만으로, 별도 설정 없이 로컬에서 만든 플러그인을 원격 플러그인처럼 가져다 쓸 수 있음
- 이 과정을 통과하면 해당 플러그인은 "배포 가능(publishable)"할 뿐 아니라 "소비 가능(consumable)"함, 즉 실제 배포 파이프라인에 올릴 준비가 되었다는 것이 증명됨
