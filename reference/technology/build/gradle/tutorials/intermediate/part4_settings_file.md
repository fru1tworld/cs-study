# Part 4. Settings 파일 작성하기 (Writing the Settings File)

> **원문:** https://docs.gradle.org/current/userguide/part4_settings_file.html

## 개요
Intermediate Tutorial의 4번째 파트로, `settings.gradle(.kts)` 파일이 왜 모든 Gradle 빌드의 진입점(entry point)인지, 그리고 그 안에서 어떤 API와 DSL 요소들을 사용할 수 있는지를 다룹니다. Part 1(자바 앱 초기화), Part 2(빌드 라이프사이클), Part 3(서브프로젝트와 별도 빌드 추가)을 이해했다는 전제 위에서 진행됩니다.

## 빌드 스크립트도 결국 "코드"다
- `settings.gradle(.kts)`와 `build.gradle(.kts)`는 모두 Kotlin 또는 Groovy로 작성된 코드입니다. 즉 일반 프로그램처럼 API 호출, 변수, 조건문 등을 그대로 사용할 수 있습니다.
- 스크립트 안에서 호출 가능한 요소는 세 가지 범주로 나뉩니다.
  - **Gradle API**: 예) `Settings` 인터페이스의 `getRootProject()`
  - **DSL 블록**: 예) `plugins { }` 블록
  - **플러그인이 제공하는 확장(extension)**: 예) `java` 플러그인이 제공하는 `implementation()`, `api()`

## Settings 객체란
- Gradle은 초기화(initialization) 단계에서 프로젝트 루트의 settings 파일을 찾아 `Settings` 객체를 생성합니다.
- 이 객체의 핵심 역할 중 하나는 **빌드에 포함될 프로젝트 목록을 선언**하는 것입니다.
- `Settings` 인터페이스가 제공하는 메서드/속성은 settings 파일 안에서 별도의 delegation 없이 바로 호출할 수 있습니다. 대표적으로 다음이 있습니다.
  - `include()` — 서브프로젝트를 빌드에 포함
  - `includeBuild()` — 별도의 복합 빌드(composite build)를 포함
  - `rootProject.name` — 루트 프로젝트 이름 지정

## 예제로 보는 settings 파일 구성 요소
공식 튜토리얼은 아래와 같은 하나의 settings 파일 예시를 통해 각 구성 요소가 어떤 API/DSL에서 온 것인지 설명합니다.

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "authoring-tutorial"

include("app")
include("lib")

includeBuild("gradle/license-plugin")
```

각 줄이 어디서 유래하는지 정리하면 다음과 같습니다.

| 코드 | 출처 |
|---|---|
| `plugins { }` | `PluginDependenciesSpec` API의 블록 |
| `id(...)` | `PluginDependenciesSpec` API의 메서드 |
| `rootProject.name = ...` | `Settings` API의 `getRootProject()` |
| `include(...)` | `Settings` API의 메서드 |
| `includeBuild(...)` | `Settings` API의 메서드 |

## 핵심 포인트: settings 파일은 "선언"과 "구성"이 뒤섞인 코드다
- 이 파일 하나에서 플러그인 해석 방식(`plugins{}`), 프로젝트 구조(`rootProject.name`, `include()`), 그리고 외부 빌드와의 관계(`includeBuild()`)를 동시에 다룹니다.
- 이 요소들이 한 파일에 모이는 이유는 앞서 다른 노트에서 정리했듯, settings 스크립트가 **모든 build 스크립트보다 먼저 평가**되기 때문입니다. 따라서 빌드 전역에 영향을 주는 설정(플러그인 관리, 복합 빌드 포함 등)을 여기서 처리하는 것이 자연스럽습니다.
- `include()`와 `includeBuild()`는 이름이 비슷하지만 역할이 다릅니다. `include()`는 같은 빌드 안의 서브프로젝트를, `includeBuild()`는 별도의 독립된 빌드를 가져와 조합하는 것입니다.

## 요약
- settings 파일도 build 파일과 마찬가지로 Kotlin/Groovy로 작성되는 "코드"이며, Gradle API·DSL 블록·플러그인 확장을 자유롭게 조합해서 쓸 수 있다.
- Gradle은 초기화 단계에서 settings 파일을 읽어 `Settings` 객체를 만들고, 이 객체를 통해 프로젝트 구조(루트 이름, 서브프로젝트, 포함된 빌드)를 결정한다.
- 하나의 예시 파일 안에 `plugins{}`, `rootProject.name`, `include()`, `includeBuild()`가 함께 등장하는 것은 우연이 아니라, settings 파일이 빌드 전역 설정을 담당하는 위치이기 때문이다.

다음 단계는 Build 스크립트 작성하기(Writing a Build Script)로 이어집니다.
