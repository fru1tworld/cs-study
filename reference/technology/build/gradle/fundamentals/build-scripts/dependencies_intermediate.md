# Gradle 의존성 선언 심화

> **원문:** https://docs.gradle.org/current/userguide/dependencies_intermediate.html

## 개요

기초 단계에서는 `implementation`, `api` 같은 컨피규레이션에 라이브러리를 선언하는 방법을 다뤘다. 이 문서는 한 단계 더 들어가서 다음 네 가지를 정리한다.

- 의존성 표기법 중 어떤 방식을 써야 하는지
- 버전 카탈로그로 여러 모듈의 버전을 한곳에서 관리하는 방법
- 버전을 강제(enforce)하거나 제약(constrain)하는 방법
- 동일 기능을 제공하는 라이브러리가 충돌할 때 캐퍼빌리티(capability)로 해결하는 방법

## 의존성 표기법: 문자열 vs 맵

`dependencies {}` 블록 안에서 라이브러리를 지정하는 방식은 두 가지가 있지만, 권장되는 것은 하나뿐이다.

```kotlin
dependencies {
    implementation("com.google.guava:guava:30.0-jre")
    runtimeOnly("org.apache.commons:commons-lang3:3.14.0")
}
```

- 위처럼 `"group:name:version"` 형태의 **단일 문자열 표기법**이 표준이다.
- `group`, `name`, `version` 키를 나열하는 **맵 표기법**은 Gradle 9.1.0부터 지원 중단(deprecated)되었고, Gradle 10부터는 빌드가 실패한다.
- 어떤 컨피규레이션(`implementation`, `runtimeOnly`, `testImplementation` 등)을 쓸 수 있는지는 적용된 플러그인(java-library, Android, Kotlin Multiplatform 등)에 따라 달라진다.

### 핵심 포인트

맵 표기법으로 작성된 레거시 빌드 스크립트가 있다면 Gradle 10 이전에 단일 문자열 표기법으로 옮겨야 마이그레이션 리스크가 없다.

## 버전 카탈로그로 버전 중앙화하기

여러 서브프로젝트가 같은 라이브러리를 각자 다른 버전으로 선언하는 문제를 막기 위해, `gradle/libs.versions.toml` 파일 하나에 버전과 좌표를 모아둔다.

```toml
[versions]
guava = "33.3.1-jre"
junit-jupiter = "5.11.3"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit-jupiter" }
```

빌드 스크립트에서는 `libs` 접근자로 참조한다.

```kotlin
dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
}
```

- 버전을 올릴 때 TOML 파일 한 곳만 고치면 모든 서브프로젝트에 반영된다.
- IDE가 카탈로그 메타데이터를 인식해 자동완성을 지원한다.

## 버전 강제와 제약

전이 의존성이 예상치 못한 버전을 끌고 오는 경우, 두 가지 방법으로 버전을 통제할 수 있다.

### 1) 의존성 선언에 직접 제약을 붙이는 방법

```kotlin
dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.5.4")
    implementation("commons-codec:commons-codec") {
        version { strictly("1.9") }
    }
}
```

`strictly()`는 해당 버전 범위를 벗어나는 어떤 치환도 허용하지 않도록 강제한다.

### 2) `constraints {}` 블록으로 별도 선언하는 방법

```kotlin
dependencies {
    implementation("org.apache.httpcomponents:httpclient")
    constraints {
        implementation("org.apache.httpcomponents:httpclient:4.5.3") {
            because("이전 버전에 이 애플리케이션에 영향을 주는 버그가 있음")
        }
        implementation("commons-codec:commons-codec:1.11") {
            because("httpclient가 끌어오는 1.9 버전에 버그가 있음")
        }
    }
}
```

### 핵심 포인트

- `constraints {}` 블록은 의존성 자체를 추가하지 않고 버전 범위만 좁히는 용도라서, 실제 의존성 선언과 제약 이유를 분리해 관리할 수 있다.
- `because()`로 왜 그 버전을 강제했는지 남겨두면, 나중에 다른 개발자가 제약을 지우거나 바꿔도 되는지 판단하기 쉬워진다.

## 캐퍼빌리티로 중복 기능 라이브러리 충돌 해결

서로 다른 좌표를 가진 두 라이브러리가 같은 기능(예: XPath 처리)을 제공하면, Gradle은 이를 별개의 의존성으로 인식해 둘 다 클래스패스에 올려버린다. 이때 런타임 충돌이 날 수 있다.

```kotlin
dependencies {
    implementation("jaxen:jaxen:1.1.6")     // XPath 기능 제공
    implementation("org.jdom:jdom2:2.0.6")  // 동일한 XPath 기능 제공
}
```

해결 방법은 두 라이브러리에 같은 캐퍼빌리티를 부여해 "둘 중 하나만 선택되어야 하는 대체 관계"임을 Gradle에 알려주는 것이다.

```kotlin
dependencies {
    components {
        withModule("jaxen:jaxen") {
            allVariants { withCapabilities { addCapability("xml", "xpath-support", "1.0") } }
        }
        withModule("org.jdom:jdom2") {
            allVariants { withCapabilities { addCapability("xml", "xpath-support", "1.0") } }
        }
    }
    implementation("jaxen:jaxen:1.1.6") {
        capabilities { requireCapability("xml:xpath-support") }
    }
}
```

- 메타데이터 규칙으로 두 모듈에 동일한 캐퍼빌리티(`xml:xpath-support`)를 선언한다.
- 의존성 선언 쪽에서 `requireCapability()`로 원하는 쪽을 명시하면, Gradle이 나머지를 충돌 후보로 인식해 하나만 선택한다.

### 핵심 포인트

캐퍼빌리티는 "같은 좌표"가 아니라 "같은 역할"을 하는 라이브러리 간의 충돌을 표현하는 수단이다. 버전 충돌(같은 모듈, 다른 버전)과는 다른 문제이므로 혼동하지 않아야 한다.

## 정리

- 의존성은 단일 문자열 표기법(`"group:name:version"`)으로 선언하며, 맵 표기법은 Gradle 10에서 사라진다.
- 버전 카탈로그(`libs.versions.toml`)로 멀티 프로젝트 전체의 버전을 한 곳에서 관리한다.
- `strictly()`나 `constraints {}` + `because()`로 전이 의존성의 버전을 강제하고, 그 이유를 문서화한다.
- 동일 기능을 제공하는 서로 다른 라이브러리는 캐퍼빌리티를 부여해 대체 관계로 만들고 충돌을 해소한다.
