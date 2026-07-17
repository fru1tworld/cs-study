# Settings 파일 기초 (Settings File Basics)

> **원문:** https://docs.gradle.org/current/userguide/settings_file_basics.html

## 개요
`settings.gradle(.kts)`는 모든 Gradle 프로젝트의 진입점 역할을 하는 파일입니다. Gradle은 실제 빌드 로직(태스크 실행, 의존성 해석 등)을 수행하기 전에 이 파일을 먼저 읽어 프로젝트 전체 구조를 파악합니다.

## 언제 필요한가
- **단일 프로젝트 빌드**: settings 파일이 없어도 동작합니다. 파일이 없으면 Gradle은 해당 빌드를 단일 프로젝트로 취급합니다.
- **멀티 프로젝트 빌드**: settings 파일이 필수입니다. 포함된 모든 서브프로젝트를 이 파일에 명시적으로 선언해야 하기 때문입니다.

## 파일 이름과 위치
- Groovy DSL을 쓰면 `settings.gradle`, Kotlin DSL을 쓰면 `settings.gradle.kts`로 작성합니다. Gradle 스크립트에 허용되는 언어는 이 두 가지뿐입니다.
- 프로젝트를 최초에 인식시키는 파일이므로 항상 **프로젝트 루트 디렉터리**에 위치해야 합니다.

## settings 파일이 담당하는 핵심 역할 두 가지

**1. 루트 프로젝트 이름 지정**

```kotlin
rootProject.name = "root-project"
```
모든 빌드는 정확히 하나의 루트 프로젝트를 가지며, 그 이름은 settings 파일에서 지정합니다.

**2. 서브프로젝트 포함**

```kotlin
include("sub-project-a")
include("sub-project-b")
```
`include()` 함수로 멀티 프로젝트 구조에 포함될 서브프로젝트들을 선언합니다.

이 두 선언에 따라 아래와 같은 디렉터리 구조가 만들어집니다.

```
.
├── settings.gradle(.kts)
├── sub-project-a
│   └── build.gradle(.kts)
└── sub-project-b
    └── build.gradle(.kts)
```

## 핵심 포인트: 평가 시점이 다르다
settings 스크립트는 **다른 어떤 build 스크립트보다 먼저 평가**됩니다. 이 특성 때문에 settings 파일은 프로젝트 구조 선언 외에도 빌드 전역(build-wide)에 영향을 주는 설정을 다루기에 적합한 위치가 됩니다. 대표적으로 다음과 같은 것들이 여기서 다뤄집니다.
- 플러그인 관리(plugin management)
- 복합 빌드(included builds) 구성
- 버전 카탈로그(version catalogs) 설정

## settings.gradle(.kts) vs build.gradle(.kts)
같은 `.gradle(.kts)` 확장자를 쓰지만 역할이 완전히 다릅니다.

| 구분 | settings.gradle(.kts) | build.gradle(.kts) |
|---|---|---|
| 평가 시점 | 가장 먼저 평가됨 | settings 파일 이후 평가됨 |
| 다루는 대상 | 프로젝트 전체 구조(루트 이름, 서브프로젝트 목록) | 개별 프로젝트의 태스크·의존성·플러그인 적용 |
| 존재 위치 | 프로젝트 루트에 1개만 존재 | 루트 및 각 서브프로젝트별로 존재 가능 |
| 필수 여부 | 멀티 프로젝트에서는 필수 | 프로젝트 설정에 따라 다름 |

## 요약
- settings 파일은 "이 빌드에 어떤 프로젝트들이 존재하는가"를 정의하는 곳이고, build 파일은 "각 프로젝트가 무엇을 하는가"를 정의하는 곳이다.
- 멀티 프로젝트 빌드를 구성한다면 `rootProject.name` 지정과 `include()`를 통한 서브프로젝트 등록이 가장 먼저 챙겨야 할 작업이다.
- 다른 build 스크립트보다 먼저 실행된다는 점 때문에, 플러그인 관리나 버전 카탈로그처럼 빌드 전역 설정을 놓기에 적절한 위치이다.
