# 빌드 스크립트 작성하기: 중급 (Writing Build Scripts)

> **원문:** https://docs.gradle.org/current/userguide/writing_build_scripts_intermediate.html

## 개요

빌드 스크립트(`build.gradle` / `build.gradle.kts`)는 사실 일반적인 스크립트 파일이 아니라, Gradle이 내부적으로 특정 API 객체(주로 `Project`)를 대상으로 실행하는 설정 코드입니다. 이 문서는 빌드 스크립트가 "왜 이렇게 짧게 쓰이는지", "블록 안에서 이름만 적으면 왜 동작하는지"를 이해하기 위한 중급 개념을 다룹니다.

## 블록과 위임(Delegate) / 수신자(Receiver)

- 빌드 스크립트는 크게 **구문(statement)**과 **블록(block)** 두 가지로 구성됩니다. `plugins {}`, `repositories {}`, `dependencies {}` 같은 블록은 Groovy의 클로저 또는 Kotlin의 람다입니다.
- 블록 내부의 메서드 호출은 실제로는 특정 대상 객체에게 전달됩니다.
  - Groovy: 블록의 **delegate**가 호출을 받습니다.
  - Kotlin: 블록의 **receiver**(즉 `this`)가 호출을 받습니다.
- 예를 들어 `dependencies { implementation(...) }`에서 `implementation(...)`은 실제로는 `DependencyHandler`에게 위임되는 호출입니다. 필요하다면 `project.dependencies.implementation(...)`처럼 완전한 경로로도 쓸 수 있습니다.

```kotlin
repositories {
    mavenCentral() // 이 호출은 RepositoryHandler에게 위임됨
}
```

## 핵심 포인트: 짧게 쓸 수 있는 이유

- 블록 안에서 이름만 적어도 동작하는 이유는 "생략"이 아니라 "위임/수신자 지정"입니다. Gradle DSL은 각 블록마다 어떤 객체가 메서드 호출을 받을지 미리 정해 놓습니다.
- 이 구조를 알면, 알 수 없는 블록을 만났을 때 "이 블록의 delegate/receiver가 어떤 타입인가?"를 먼저 확인하는 습관이 생깁니다.

## 변수: 로컬 변수 vs Extra Property

빌드 스크립트에서 값을 저장하는 방법은 두 가지로 구분됩니다.

- **로컬 변수(local variable)**: `val`(Kotlin), `def`(Groovy)로 선언하며, 선언된 스코프 안에서만 보입니다. 각 언어의 일반적인 변수와 동일합니다.

```kotlin
val dest = "dest"
tasks.register<Copy>("copy") {
    from("source")
    into(dest)
}
```

- **Extra Property(확장 속성)**: `project`처럼 확장 가능한 객체에 사용자 정의 값을 붙이는 방식입니다. Kotlin은 `extra["key"]`, Groovy는 `ext { }` 블록을 사용합니다.

```groovy
ext {
    springVersion = "3.1.0.RELEASE"
    emailNotification = "build@master.org"
}
```

## 핵심 포인트: 두 변수의 차이

- 로컬 변수는 언어 차원의 스코프 규칙을 따르므로 해당 블록/스크립트 밖에서는 존재하지 않습니다.
- Extra Property는 소유 객체(`project` 등)에 붙기 때문에, 서브프로젝트에서 부모 프로젝트의 확장 속성을 읽어오는 등 스코프를 넘는 값 공유가 가능합니다. 값을 여러 태스크나 스크립트 조각에서 재사용하고 싶다면 Extra Property가 적합합니다.

## 실행 순서: 위에서 아래로

- Gradle은 빌드 스크립트를 위에서 아래로 순차 평가합니다. 설정 블록 바깥의 코드는 즉시(eager) 실행됩니다.
- 즉, 변수를 태스크 설정 블록보다 먼저 선언해야 하며, 선언 순서가 실제 동작에 영향을 줍니다.
- "지금 당장" 실행되는 것이 아니라 "나중에, 필요할 때" 평가되어야 하는 값은 `Provider` 같은 지연(lazy) API를 사용해야 합니다. 이는 태스크 설정 회피(configuration avoidance)와도 연결되는 개념입니다.

## 예제로 보는 구성 요소별 역할

```kotlin
plugins {
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:32.1.1-jre")
}

application {
    mainClass = "com.example.Main"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register<Zip>("zip-reports") {
    from("Reports/")
}
```

- **`plugins {}`**: `PluginDependenciesSpec`를 통해 플러그인을 적용합니다. 플러그인은 기능을 모듈화해서 프로젝트에 주입하는 단위입니다.
- **`repositories {}`**: 의존성을 어디서 내려받을지 지정합니다.
- **`dependencies {}`**: `implementation`, `testImplementation` 같은 컨피규레이션에 라이브러리를 등록합니다.
- **`application {}` 같은 확장(extension) 블록**: 플러그인이 `Project`의 `ExtensionContainer`에 추가해 둔 설정 영역입니다. 플러그인을 적용하면 이런 커스텀 설정 블록이 새로 생깁니다.
- **`tasks.register(...)`**: 새 태스크를 등록합니다. 즉시 인스턴스를 만드는 옛 방식인 `create()`보다, 필요할 때까지 생성을 미루는 `register()`가 권장됩니다(태스크 설정 회피).
- **`tasks.named(...)`**: 이미 존재하는 태스크(예: 플러그인이 만들어 둔 `test` 태스크)를 찾아 추가로 설정합니다.

## 핵심 포인트: register vs named

- `register()`는 "새로 만들기", `named()`는 "이미 있는 걸 찾아서 설정하기"입니다.
- 둘 다 즉시 태스크 인스턴스를 생성하지 않고 필요한 시점까지 지연시키는 설계로, 대규모 빌드에서 불필요한 태스크 생성/설정 비용을 줄여줍니다.

## 프로퍼티 접근: 한정자 없이 쓰는 이유

빌드 스크립트에서 `println(name)`처럼 `project.` 없이 프로퍼티를 쓸 수 있는 것도 위임/수신자 구조 때문입니다.

```kotlin
println(name)            // 축약형
println(project.name)    // 완전한 표기 — 결과는 동일
```

- Groovy는 한정자가 없는 참조를 동적으로 `Project` 객체에 위임합니다.
- Kotlin은 빌드 스크립트 자체를 `Project`의 확장(extension)으로 컴파일하기 때문에 프로퍼티를 직접 참조할 수 있습니다.

세팅 스크립트(`settings.gradle(.kts)`)에서도 동일한 원리가 적용되며, 이 경우 대상 객체는 `Settings`입니다.

```groovy
rootProject.name = "my-awesome-project"
println(name)  // settings 스크립트에서는 보통 rootProject.name을 가리킴
```

## 기본 임포트(Default Imports)

Gradle은 자주 쓰는 클래스들을 자동으로 임포트해 둡니다. 예를 들어 `org.gradle.api.tasks.StopExecutionException`을 전체 경로로 쓰지 않고 `StopExecutionException`만 써도 됩니다. 덕분에 빌드 스크립트가 장황해지지 않습니다.

## 정리

- 빌드 스크립트가 짧고 선언적으로 보이는 이유는 블록마다 정해진 delegate/receiver에게 호출이 위임되기 때문입니다.
- 값 저장은 스코프가 좁은 로컬 변수와, 프로젝트 트리 전체에서 공유 가능한 Extra Property로 구분해서 씁니다.
- 스크립트는 위에서 아래로 즉시 평가되므로 변수·태스크 선언 순서가 실제 동작에 영향을 미치며, 지연 평가가 필요하면 `Provider` API를 사용합니다.
- `register()`/`named()`로 태스크 설정을 지연시키고, 플러그인이 추가하는 확장 블록(`application {}` 등)으로 세부 설정을 이어갑니다.
