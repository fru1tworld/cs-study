# Gradle 플러그인 기초

> **원문:** https://docs.gradle.org/current/userguide/plugin_basics.html

## 개요
Gradle 자체는 의존성 해석, 태스크 오케스트레이션, 증분 빌드 같은 핵심 인프라만 담당하고, 실제로 자주 쓰는 기능(자바 컴파일, 안드로이드 빌드, 아티팩트 배포 등)의 대부분은 **플러그인**이 제공합니다. 즉 Gradle은 "얇은 코어 + 플러그인 확장" 구조를 지향하는 빌드 시스템입니다.

## 플러그인이 하는 일
플러그인을 적용하면 빌드 스크립트에 다음과 같은 것들이 추가됩니다.

- **새로운 태스크** — 예: `compileJava`, `test`
- **새로운 설정(configuration)** — 예: `implementation`, `runtimeOnly`
- **DSL 요소** — 예: `application { ... }`, `publishing { ... }`

플러그인 하나를 적용하는 것만으로 위 세 가지가 한꺼번에 빌드에 편입되는 경우가 많습니다. 예를 들어 `java-library` 플러그인은 자바 소스 컴파일용 태스크, `implementation`/`api` 같은 설정, 관련 DSL을 동시에 가져옵니다.

## 플러그인 적용 문법
플러그인은 빌드 스크립트 상단의 `plugins { }` 블록에서 **플러그인 ID**(전역적으로 유일한 식별자)와 필요하면 **버전**을 지정해 적용합니다.

```kotlin
plugins {
    id("«plugin id»").version("«plugin version»")
}
```

여러 플러그인을 동시에 적용하는 것도 자연스러운 패턴입니다.

```kotlin
plugins {
    id("java-library")
    id("com.diffplug.spotless").version("6.25.0")
}
```

위 예시는 자바 라이브러리 컴파일 기능과 코드 포맷팅 도구(Spotless)를 함께 활성화합니다.

## 플러그인의 세 가지 분류

### 1. 코어(Core) 플러그인
Gradle 배포판에 기본 내장된 플러그인으로, Gradle 팀이 직접 관리합니다. 별도의 다운로드나 버전 지정 없이 짧은 이름만으로 바로 적용할 수 있는 것이 특징입니다.

```kotlin
plugins {
    id("java-library")
}
```

### 2. 커뮤니티(Community) 플러그인
Gradle 생태계의 서드파티 개발자·조직이 만들어 보통 **Gradle Plugin Portal**에 공개한 플러그인입니다. 코어 플러그인과 달리 ID와 **버전을 함께** 명시해야 하며, 빌드 실행 시 Gradle이 자동으로 다운로드합니다.

```kotlin
plugins {
    id("org.springframework.boot").version("3.1.5")
}
```

### 3. 커스텀 / 로컬 플러그인
조직이나 개인이 단일 프로젝트 또는 멀티 프로젝트 빌드 전용으로 직접 작성한 플러그인입니다. 배포 방식과 무관하게 적용 문법은 커뮤니티 플러그인과 동일하게 이름(ID)으로 지정합니다.

```kotlin
plugins {
    id("my.custom-conventions")
}
```

## 핵심 포인트
- Gradle의 실질적 기능 대부분은 코어가 아니라 **플러그인**에서 나온다.
- 플러그인은 태스크·설정(configuration)·DSL을 한 번에 빌드에 추가하는 확장 단위다.
- 적용은 항상 `plugins { id("...") }` 형태이며, 코어 플러그인만 버전 지정 없이 짧은 이름으로 쓸 수 있다.
- 플러그인은 **코어(내장) / 커뮤니티(Plugin Portal 배포) / 커스텀(자체 제작)** 세 부류로 나뉘고, 커뮤니티·커스텀 플러그인은 보통 버전을 함께 지정한다.
