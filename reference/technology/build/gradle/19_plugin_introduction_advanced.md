# Gradle 플러그인 심화 소개

> **원문:** https://docs.gradle.org/current/userguide/plugin_introduction_advanced.html

## 개요
플러그인 기초 문서에서는 "플러그인을 어떻게 적용하는가"를 다뤘다면, 이 문서는 한 단계 더 들어가 "플러그인이 빌드 라이프사이클의 어느 시점에, 어떤 범위에 개입하는가"와 "누가 만들고 어떻게 작성하는가"를 정리한다. 즉 플러그인을 **적용 시점(타입)**, **출처**, **작성 방식** 세 축으로 나눠서 이해하는 것이 핵심이다.

## 적용 시점에 따른 세 가지 플러그인 타입
플러그인이 구현하는 인터페이스에 따라 개입할 수 있는 빌드 라이프사이클 범위가 결정된다.

| 타입 | 구현 인터페이스 | 개입 범위 | 적용 위치 |
|---|---|---|---|
| Init 플러그인 | `Plugin<Gradle>` | 전역(모든 빌드) | `~/.gradle/init.gradle[.kts]` 또는 `--init-script` |
| Settings 플러그인 | `Plugin<Settings>` | 빌드 레이아웃(포함 프로젝트, 플러그인 해석 방식 등) | `settings.gradle[.kts]` |
| Project 플러그인 | `Plugin<Project>` | 개별 프로젝트(태스크, 의존성 등) | `build.gradle[.kts]` |

- **Init 플러그인**: 빌드가 시작되기 *전에* Gradle 런타임 자체를 설정한다. 머신에 있는 모든 빌드에 공통으로 걸리는 전역 설정(사내 저장소 강제 지정 등)에 적합하다.
- **Settings 플러그인**: 멀티 프로젝트 구성이나 플러그인 해석 규칙처럼 "빌드가 어떤 모양을 가질지"를 결정하는 단계에서 동작한다.
- **Project 플러그인**: 실무에서 가장 많이 접하는 형태로, `plugins { id("...") }`로 적용해 태스크·설정(configuration)·DSL을 추가하는 익숙한 방식이다.

## 플러그인의 범위(Scope)는 인터페이스가 결정한다
플러그인이 `Plugin<Gradle>` / `Plugin<Settings>` / `Plugin<Project>` 중 무엇을 구현했는지가 곧 그 플러그인이 손댈 수 있는 대상의 경계다. Project 플러그인은 Settings나 Gradle 전역 설정에 개입할 수 없고, 반대로 Init 플러그인은 전역적이라 개별 프로젝트의 세부 태스크보다는 상위 정책(리포지토리, 인증 등)을 다루는 데 쓰인다. 플러그인을 설계할 때 "이 로직이 정말 전역/빌드 레이아웃/프로젝트 중 어느 층위에 속하는가"를 먼저 정하는 것이 인터페이스 선택의 기준이 된다.

## 출처에 따른 세 가지 분류
기초 문서의 코어/커뮤니티/커스텀 분류와 동일한 축이지만, 여기서는 "누가 작성하고 배포 책임을 지는가" 관점으로 다시 정리된다.

- **코어 플러그인**: `java`, `application`처럼 Gradle 배포판에 내장되어 있어 별도로 작성하거나 배포할 필요가 없다.
- **커뮤니티 플러그인**: 커뮤니티가 만들어 주로 [Gradle Plugin Portal](https://plugins.gradle.org/)에 바이너리 JAR 형태로 공개한 것. 사용할 때는 ID와 버전을 함께 지정한다.
- **로컬 플러그인**: 특정 조직·프로젝트를 위해 직접 작성한 플러그인. 여러 서브프로젝트에 공통 로직(컨벤션)을 나눠 쓰고 싶을 때 특히 유용하다.

## 직접 만들 때의 두 가지 작성 방식
로컬 플러그인을 작성하기로 했다면, 구현 형태를 두 가지 중에서 고를 수 있다.

### 1. Precompiled Script 플러그인
`.gradle.kts`/`.gradle` 스크립트 파일 자체를 플러그인처럼 다루는 방식이다. 빌드 중에 자동으로 컴파일되며, 별도의 JAR 배포 없이 간단한 컨벤션(공통 자바 설정, 공통 테스트 설정 등)을 묶어 재사용하기에 적합하다.

```kotlin
// buildSrc/src/main/kotlin/my.java-conventions.gradle.kts
plugins {
    id("java")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
```

### 2. Binary 플러그인
자바·코틀린·그루비 등으로 작성해 JAR로 패키징하는 정식 플러그인 형태다. 여러 빌드에서 재사용하거나 Plugin Portal 같은 저장소에 배포해 커뮤니티 플러그인처럼 공유할 수 있다. Precompiled Script 플러그인보다 초기 작성 비용이 크지만, 배포·버전 관리·테스트 측면에서 더 정식화된 구조를 갖춘다.

## 핵심 포인트
- 플러그인은 구현 인터페이스(`Plugin<Gradle>`/`Plugin<Settings>`/`Plugin<Project>`)에 따라 **Init / Settings / Project** 세 타입으로 나뉘고, 이 인터페이스가 곧 개입 가능한 범위를 정한다.
- 실무에서 다루는 대부분의 플러그인은 **Project 플러그인**이며, `plugins { }` 블록으로 적용하는 익숙한 패턴이 여기 해당한다.
- 출처 관점에서는 **코어(내장) / 커뮤니티(Plugin Portal) / 로컬(자체 작성)** 로 나뉘는데, 이는 기초 문서의 분류와 같은 축을 다른 각도로 본 것이다.
- 로컬 플러그인을 직접 작성할 때는 **Precompiled Script 플러그인**(간단한 컨벤션 묶음)과 **Binary 플러그인**(JAR로 배포 가능한 정식 플러그인) 중에서 목적에 맞게 선택한다.
- 더 깊은 구현 방법은 Gradle 공식 문서의 "Pre-Compiled Script Plugins" 후속 문서에서 다룬다.
