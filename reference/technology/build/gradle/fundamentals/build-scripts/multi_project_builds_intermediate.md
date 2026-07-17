# 멀티 프로젝트 빌드 구조화하기 (Structuring Multi-Project Builds)

> **원문:** https://docs.gradle.org/current/userguide/multi_project_builds_intermediate.html

## 개요

애플리케이션이 커지면 하나의 프로젝트에 모든 소스를 몰아넣기보다 기능·계층별로 모듈을 쪼개는 편이 유지보수에 유리합니다. Gradle은 이런 요구를 멀티 프로젝트 빌드(멀티 모듈 프로젝트라고도 부름)로 지원하며, 하나의 빌드는 정확히 하나의 루트 프로젝트와 그 아래 여러 서브프로젝트로 구성됩니다.

## 디렉터리 구조

가장 단순한 형태는 루트 아래에 서브프로젝트 디렉터리를 나란히 두고, 각 서브프로젝트가 자신만의 빌드 스크립트를 갖는 방식입니다.

```text
.
├── gradlew
├── gradlew.bat
├── settings.gradle(.kts)
├── sub-project-1/build.gradle(.kts)
├── sub-project-2/build.gradle(.kts)
└── sub-project-3/build.gradle(.kts)
```

루트의 `settings.gradle.kts`는 이 빌드에 어떤 서브프로젝트가 속하는지를 선언하는 유일한 진입점입니다.

```kotlin
include("sub-project-1", "sub-project-2", "sub-project-3")
```

- 각 서브프로젝트 디렉터리에는 자체 `build.gradle(.kts)`가 있어야 하며, 여기에 해당 모듈만의 플러그인·의존성·태스크를 정의합니다.
- 루트 프로젝트 자체에도 `build.gradle(.kts)`를 둘 수 있지만, 보통은 공통 설정을 모으는 용도로만 가볍게 사용합니다.

## 프로젝트 경로(Project Path)

Gradle은 각 프로젝트를 콜론(`:`)으로 구분되는 경로로 식별합니다. 루트 프로젝트의 경로는 `:` 하나뿐이고 이름을 붙이지 않아도 되며, 서브프로젝트는 `:서브프로젝트이름` 형태를 가집니다. 전체 프로젝트 목록과 경로는 `gradle projects` 명령으로 확인할 수 있습니다.

### 이름으로 태스크 실행

```bash
./gradlew test
```

이렇게 태스크 이름만 주면, 현재 디렉터리를 기준으로 그 이름의 태스크를 가진 모든 하위 프로젝트에서 해당 태스크가 실행됩니다. 계층 아래로 내려가며 이름이 일치하는 태스크를 전부 찾아 실행하고, 어디에도 없으면 오류로 처리됩니다.

### 정규화된 이름으로 태스크 실행

특정 서브프로젝트 하나만 대상으로 하려면 프로젝트 경로를 태스크 이름 앞에 붙인 정규화된 이름을 사용합니다.

```bash
./gradlew :sub-project-1:build
./gradlew :sub-project-3:tasks
```

## 핵심 포인트: 태스크 실행 범위 구분

- 이름만 지정하면 "범위 실행"(하위 트리 전체에서 이름이 일치하는 태스크를 모두 실행), 경로를 붙이면 "정밀 실행"(단 하나의 프로젝트, 단 하나의 태스크만 실행)이라는 차이를 기억해 두면 CI 스크립트나 부분 빌드를 짤 때 헷갈리지 않습니다.

## buildSrc로 빌드 로직 공유하기

여러 서브프로젝트가 같은 플러그인 설정이나 커스텀 태스크를 반복해서 써야 한다면, 그 로직을 `buildSrc`라는 특별한 디렉터리에 모아 재사용할 수 있습니다.

```text
.
├── settings.gradle(.kts)
├── buildSrc
│   ├── build.gradle.kts
│   └── src/main/kotlin/shared-build-conventions.gradle.kts
├── sub-project-1/build.gradle(.kts)
├── sub-project-2/build.gradle(.kts)
└── sub-project-3/build.gradle(.kts)
```

`buildSrc`는 별도로 `include()` 하지 않아도 Gradle이 자동으로 인식해서 특수한 서브프로젝트처럼 취급하고, 다른 모든 프로젝트가 빌드되기 전에 먼저 컴파일합니다. 여기서 만든 컨벤션 플러그인을 각 서브프로젝트의 `build.gradle(.kts)`에서 `plugins { id("shared-build-conventions") }` 형태로 적용하는 식입니다.

## 핵심 포인트: buildSrc의 장단점

- **장점**: 설정 파일에 별도로 등록할 필요 없이 자동으로 인식되어 시작하기 쉽습니다. 작은~중간 규모 프로젝트에서 빠르게 공통 로직을 모으는 데 적합합니다.
- **단점**: `buildSrc` 코드가 바뀌면 전체 빌드가 무효화되어 다시 컴파일되므로, 프로젝트가 커질수록 이 비용이 부담스러워집니다. 또한 여러 독립된 빌드 사이에서는 재사용할 수 없고, 어디까지나 "하나의 빌드 내부"에서만 유효합니다.

## build-logic을 이용한 컴포짓 빌드

더 큰 규모로 확장하려면 `buildSrc` 대신 별도의 Gradle 빌드를 만들어 메인 빌드에 포함시키는 컴포짓 빌드(Composite Build) 방식을 씁니다. 관례적으로 이 디렉터리를 `build-logic`이라 부릅니다.

```text
.
├── settings.gradle(.kts)
├── build-logic
│   ├── settings.gradle.kts
│   └── conventions
│       ├── build.gradle.kts
│       └── src/main/kotlin/shared-build-conventions.gradle.kts
├── sub-project-1/build.gradle(.kts)
├── sub-project-2/build.gradle(.kts)
└── sub-project-3/build.gradle(.kts)
```

`build-logic`은 자기 자신만의 `settings.gradle.kts`를 가진, 독립적으로 빌드·테스트가 가능한 하나의 완전한 Gradle 프로젝트입니다. 루트 빌드는 `includeBuild()`로 이를 끌어와 사용합니다.

```kotlin
// 루트 settings.gradle.kts
include("sub-project-1", "sub-project-2", "sub-project-3")
includeBuild("build-logic")
```

## 핵심 포인트: buildSrc vs build-logic 선택 기준

- 원문은 컴포짓 빌드가 "서브프로젝트 간이 아니라 빌드와 빌드 사이에 로직을 공유하거나, 공유 빌드 로직에 대한 접근을 격리하는 데 가장 적합하다"고 설명합니다.
- `build-logic`은 독립된 빌드이므로 별도 팀이 따로 개발·테스트한 뒤 필요한 시점에 메인 빌드로 통합할 수 있고, 여러 저장소·여러 메인 빌드에서 동일한 `build-logic`을 재사용하기도 쉽습니다.
- 반대로 당장 프로토타입 수준이거나 단일 빌드 안에서만 로직을 쓴다면 `buildSrc`가 더 간단합니다. 즉, 규모와 재사용 범위가 커질수록 `buildSrc`에서 `build-logic` 기반 컴포짓 빌드로 옮겨가는 것이 Gradle이 권장하는 성장 경로입니다.
- 서브프로젝트 자체도 내부적으로 또 다른 컴포짓 빌드를 포함할 수 있어, 필요하다면 다단계로 중첩된 빌드 구조도 구성할 수 있습니다.

## 더 알아보기

- 빌드 로직 공유의 세부 내용은 "Sharing Build Logic between Subprojects" 문서에서, 컴포짓 빌드 자체의 동작 원리는 "Composite Builds" 문서에서 더 깊이 다룹니다.
