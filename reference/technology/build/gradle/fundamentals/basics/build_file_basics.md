# 빌드 파일 기초 (Build File Basics)

> **원문:** https://docs.gradle.org/current/userguide/build_file_basics.html

## 개요
Gradle 빌드에서 **빌드 스크립트**는 프로젝트를 어떻게 빌드할지 정의하는 핵심 파일입니다. 파일명은 사용하는 DSL에 따라 `build.gradle`(Groovy) 또는 `build.gradle.kts`(Kotlin) 중 하나이며, 하나의 Gradle 빌드에는 최소 한 개의 빌드 스크립트가 필요합니다. 멀티 프로젝트 구성이라면 보통 각 서브프로젝트가 자신의 루트 디렉터리에 별도의 빌드 스크립트를 갖습니다.

## 빌드 스크립트가 다루는 3가지 요소

빌드 스크립트는 대체로 다음 세 가지를 정의합니다.

- **플러그인(Plugins)** — Gradle의 기능을 확장하고 프로젝트에 태스크와 관례(convention)를 추가합니다. 플러그인을 "적용(apply)"하면 컴파일, 테스트, 패키징 등의 부가 기능을 사용할 수 있게 됩니다.
- **의존성(Dependencies)** — 소스 코드를 컴파일·실행·테스트하는 데 필요한 외부 라이브러리를 선언합니다. 크게 두 종류로 나뉩니다.
  - Gradle/빌드 스크립트 자체가 필요로 하는 의존성 (예: 빌드 로직에 쓰이는 플러그인·라이브러리)
  - 프로젝트 소스 코드가 필요로 하는 의존성 (실제 애플리케이션 라이브러리)
- **관례 속성(Convention Properties)** — 플러그인이 프로젝트에 추가하는 속성·메서드로, 플러그인이 제공하는 기능을 세부적으로 설정할 때 사용합니다 (예: 애플리케이션의 메인 클래스 지정).

## 예시로 보는 구조

애플리케이션용 빌드 파일은 보통 아래와 같은 세 블록으로 구성됩니다.

```kotlin
plugins {
    application               // 실행 가능한 JVM 애플리케이션 지원 (Java 플러그인 내포)
}

dependencies {
    implementation("...")     // 프로젝트 코드에서 사용하는 라이브러리
    testImplementation("...") // 테스트 코드에서 사용하는 라이브러리
    testRuntimeOnly("...")    // 테스트 실행 시에만 필요한 런타임 의존성
}

application {
    mainClass.set("com.example.Main") // 관례 속성으로 실행 진입점 지정
}
```

- `plugins {}` 블록: 어떤 플러그인을 적용할지 선언 (예: `application` 플러그인은 내부적으로 Java 플러그인도 함께 적용).
- `dependencies {}` 블록: `implementation`, `testImplementation`, `testRuntimeOnly` 등 의존성 구성(configuration) 종류별로 라이브러리를 선언.
- 플러그인이 제공하는 관례 블록(위 예시의 `application {}`): 플러그인 동작을 세부 조정.

## Groovy DSL vs Kotlin DSL

Gradle 빌드 스크립트를 작성할 수 있는 언어는 Groovy DSL과 Kotlin DSL 두 가지뿐입니다. 기능적으로는 동등하며 문법 스타일만 다릅니다.

| 구분 | Groovy DSL | Kotlin DSL |
|---|---|---|
| 파일명 | `build.gradle` | `build.gradle.kts` |
| 타입 체크 | 동적 타입, 간결한 문법 | 정적 타입, IDE 자동완성/검증에 유리 |

## 빌드 스크립트가 하는 일

- 의존성 선언 및 관리
- 태스크(task) 설정 및 커스터마이징
- 버전 카탈로그(version catalog), 컨벤션 플러그인(convention plugin) 등과의 연동

빌드 스크립트는 Gradle의 **구성 단계(configuration phase)**에서 실행되며, 이후 실제 빌드 실행(태스크 실행 단계)의 기반을 마련하는 역할을 합니다.

## 핵심 포인트

- 빌드 스크립트 = 플러그인 + 의존성 + 관례 속성을 선언하는 파일.
- 파일명과 문법은 Groovy(`build.gradle`)와 Kotlin(`build.gradle.kts`) DSL 중 선택.
- `plugins {}`로 기능을 확장하고, `dependencies {}`로 외부 라이브러리를 연결하며, 플러그인이 제공하는 관례 블록으로 세부 설정을 한다.
- 멀티 프로젝트에서는 서브프로젝트마다 별도의 빌드 스크립트를 가질 수 있다.
