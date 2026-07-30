# Gradle 기초: 핵심 개념과 구성 파일

## Gradle 핵심 개념 (Core Concepts)

> **원문:** https://docs.gradle.org/current/userguide/gradle_basics.html

### 개요

Gradle은 빌드 스크립트에 담긴 정보를 바탕으로 소프트웨어의 빌드, 테스트, 배포를 자동화하는 빌드 도구입니다. 설정은 Groovy 또는 Kotlin DSL로 작성하며, 현재 JVM 계열(Java, Kotlin)뿐 아니라 C++, Swift 프로젝트에서도 권장되는 빌드 시스템입니다.

### 핵심 용어

Gradle을 이해하려면 아래 여섯 가지 개념을 먼저 잡아야 합니다.

- **빌드(Build)**: 결과물을 만들어내는 과정과 환경 전체. 하나의 빌드는 하나 이상의 프로젝트와 그 빌드 스크립트로 구성됩니다.
- **프로젝트(Project)**: 빌드 대상이 되는 소프트웨어 단위. 애플리케이션이나 라이브러리 하나가 프로젝트가 됩니다. 하나의 빌드 안에 여러 프로젝트(멀티 프로젝트 빌드)가 존재할 수 있습니다.
- **태스크(Task)**: 컴파일, 테스트 실행처럼 실제로 수행되는 작업의 최소 단위. Gradle 빌드는 결국 태스크들의 실행 그래프입니다.
- **빌드 스크립트(Build Script)**: `build.gradle` 또는 `build.gradle.kts` 파일. 해당 프로젝트에서 어떤 태스크와 의존성을 사용할지 정의합니다.
- **플러그인(Plugin)**: Gradle의 기능을 확장하는 단위. 예를 들어 Java 플러그인은 컴파일·테스트·패키징 태스크를 한 번에 추가해 줍니다.
- **의존성(Dependency)**: 프로젝트가 필요로 하는 외부 라이브러리나 내부 모듈 등의 리소스.

### 핵심 포인트: 개념 간 관계

- 빌드 ⊃ 프로젝트 ⊃ 태스크라는 포함 관계로 이해하면 쉽습니다. 빌드 하나에 여러 프로젝트, 프로젝트 하나에 여러 태스크가 속합니다.
- 플러그인은 프로젝트에 새로운 태스크나 설정을 "주입"하는 역할을 하고, 의존성은 태스크(주로 컴파일·테스트 태스크)가 동작하는 데 필요한 재료를 공급합니다.

### 표준 프로젝트 구조

일반적인 Gradle 프로젝트는 다음과 같은 구성을 가집니다.

```text
project/
├── gradle/              # Wrapper 관련 파일 및 설정
├── gradlew              # Wrapper 실행 스크립트 (Unix)
├── gradlew.bat          # Wrapper 실행 스크립트 (Windows)
├── settings.gradle(.kts) # 루트 프로젝트 및 서브프로젝트 목록 정의
├── build.gradle(.kts)   # 프로젝트별 빌드 스크립트
└── src/                 # 소스 코드
```

- `settings.gradle(.kts)`는 이 빌드에 어떤 프로젝트들이 포함되는지를 선언하는 최상위 파일입니다.
- `build.gradle(.kts)`는 각 프로젝트마다 존재할 수 있으며, 해당 프로젝트에 적용할 플러그인·의존성·태스크 설정을 담습니다.
- `gradlew`, `gradlew.bat` 파일이 있다는 것은 그 프로젝트가 Gradle 빌드라는 신호이자, Wrapper를 사용하고 있다는 표시입니다.

### Gradle을 실행하는 세 가지 방법

1. **IDE 통합**: Android Studio, IntelliJ IDEA, VS Code, Eclipse, NetBeans 등 대부분의 주요 IDE가 Gradle을 기본 내장하고 있어 GUI에서 바로 빌드/실행이 가능합니다.
2. **시스템에 직접 설치한 Gradle 사용**: `gradle` 명령을 전역으로 설치해 사용하는 방식.
3. **Gradle Wrapper 사용 (권장)**: 프로젝트에 포함된 스크립트로, 프로젝트가 지정한 특정 버전의 Gradle을 자동으로 내려받아 실행합니다. 팀원 전체가 동일한 Gradle 버전으로 빌드하도록 보장하기 때문에 공식적으로 권장되는 실행 방식입니다.

```bash
$ ./gradlew build
```

> 참고로 `gradle build`, `gradle test`, `gradle clean build`처럼 원하는 태스크 이름을 명령줄에 나열해 실행할 수 있으며, 여러 태스크를 한 줄에 이어서 지정할 수도 있습니다.

### 핵심 포인트: 왜 Wrapper를 써야 하나

- 로컬에 Gradle이 설치되어 있지 않아도 빌드가 가능합니다.
- 프로젝트가 지정한 버전이 강제되므로 "내 컴퓨터에서는 됐는데" 문제를 줄여줍니다.
- CI 환경에서도 별도의 Gradle 설치 없이 동일하게 동작합니다.

### 공식 문서의 구성 체계

이 페이지는 Gradle 유저 가이드 전체의 진입점 역할을 하며, 아래와 같이 난이도별로 문서를 안내합니다.

- **Fundamentals(기초)**: Core Concepts를 포함해 총 10개의 기초 모듈로 구성 (빌드 스크립트, 의존성 관리, 태스크, 플러그인, 빌드 스캔 등).
- **Intermediate(중급)**: 멀티 프로젝트 빌드, 빌드 생명주기, 빌드 스크립트 심화 내용.
- **Advanced(고급)**: 플러그인 개발, 커스텀 태스크 작성 등.
- **Reference(레퍼런스)**: CLI 옵션, 플러그인 목록, 태스크, 의존성 관리, 플랫폼별(Java/C++/Swift 등) 세부 안내.
- 그 외 CI/CD 연동(GitHub Actions, GitLab, Jenkins 등), 성능(빌드 캐시, 설정 캐시, Isolated Projects), 보안(공급망 보안, 의존성 검증) 섹션도 별도로 제공됩니다.

### 핵심 포인트: 이 문서를 읽는 순서

- Gradle을 처음 접한다면 이 페이지(Core Concepts) → 빌드 스크립트 기초 → 플러그인 기초 → 의존성 관리 기초 순으로 Fundamentals 챕터를 따라가는 것이 권장 경로입니다.
- 이미 기본 개념을 아는 상태에서 멀티 모듈 프로젝트를 다뤄야 한다면 Intermediate의 멀티 프로젝트 빌드 문서로 바로 넘어가면 됩니다.

---

## 빌드 파일 기초 (Build File Basics)

> **원문:** https://docs.gradle.org/current/userguide/build_file_basics.html

### 개요
Gradle 빌드에서 **빌드 스크립트**는 프로젝트를 어떻게 빌드할지 정의하는 핵심 파일입니다. 파일명은 사용하는 DSL에 따라 `build.gradle`(Groovy) 또는 `build.gradle.kts`(Kotlin) 중 하나이며, 하나의 Gradle 빌드에는 최소 한 개의 빌드 스크립트가 필요합니다. 멀티 프로젝트 구성이라면 보통 각 서브프로젝트가 자신의 루트 디렉터리에 별도의 빌드 스크립트를 갖습니다.

### 빌드 스크립트가 다루는 3가지 요소

빌드 스크립트는 대체로 다음 세 가지를 정의합니다.

- **플러그인(Plugins)** — Gradle의 기능을 확장하고 프로젝트에 태스크와 관례(convention)를 추가합니다. 플러그인을 "적용(apply)"하면 컴파일, 테스트, 패키징 등의 부가 기능을 사용할 수 있게 됩니다.
- **의존성(Dependencies)** — 소스 코드를 컴파일·실행·테스트하는 데 필요한 외부 라이브러리를 선언합니다. 크게 두 종류로 나뉩니다.
  - Gradle/빌드 스크립트 자체가 필요로 하는 의존성 (예: 빌드 로직에 쓰이는 플러그인·라이브러리)
  - 프로젝트 소스 코드가 필요로 하는 의존성 (실제 애플리케이션 라이브러리)
- **관례 속성(Convention Properties)** — 플러그인이 프로젝트에 추가하는 속성·메서드로, 플러그인이 제공하는 기능을 세부적으로 설정할 때 사용합니다 (예: 애플리케이션의 메인 클래스 지정).

### 예시로 보는 구조

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

### Groovy DSL vs Kotlin DSL

Gradle 빌드 스크립트를 작성할 수 있는 언어는 Groovy DSL과 Kotlin DSL 두 가지뿐입니다. 기능적으로는 동등하며 문법 스타일만 다릅니다.

| 구분 | Groovy DSL | Kotlin DSL |
|---|---|---|
| 파일명 | `build.gradle` | `build.gradle.kts` |
| 타입 체크 | 동적 타입, 간결한 문법 | 정적 타입, IDE 자동완성/검증에 유리 |

### 빌드 스크립트가 하는 일

- 의존성 선언 및 관리
- 태스크(task) 설정 및 커스터마이징
- 버전 카탈로그(version catalog), 컨벤션 플러그인(convention plugin) 등과의 연동

빌드 스크립트는 Gradle의 **구성 단계**(configuration phase)에서 실행되며, 이후 실제 빌드 실행(태스크 실행 단계)의 기반을 마련하는 역할을 합니다.

### 핵심 포인트

- 빌드 스크립트 = 플러그인 + 의존성 + 관례 속성을 선언하는 파일.
- 파일명과 문법은 Groovy(`build.gradle`)와 Kotlin(`build.gradle.kts`) DSL 중 선택.
- `plugins {}`로 기능을 확장하고, `dependencies {}`로 외부 라이브러리를 연결하며, 플러그인이 제공하는 관례 블록으로 세부 설정을 한다.
- 멀티 프로젝트에서는 서브프로젝트마다 별도의 빌드 스크립트를 가질 수 있다.

---

## Settings 파일 기초 (Settings File Basics)

> **원문:** https://docs.gradle.org/current/userguide/settings_file_basics.html

### 개요
`settings.gradle(.kts)`는 모든 Gradle 프로젝트의 진입점 역할을 하는 파일입니다. Gradle은 실제 빌드 로직(태스크 실행, 의존성 해석 등)을 수행하기 전에 이 파일을 먼저 읽어 프로젝트 전체 구조를 파악합니다.

### 언제 필요한가
- **단일 프로젝트 빌드**: settings 파일이 없어도 동작합니다. 파일이 없으면 Gradle은 해당 빌드를 단일 프로젝트로 취급합니다.
- **멀티 프로젝트 빌드**: settings 파일이 필수입니다. 포함된 모든 서브프로젝트를 이 파일에 명시적으로 선언해야 하기 때문입니다.

### 파일 이름과 위치
- Groovy DSL을 쓰면 `settings.gradle`, Kotlin DSL을 쓰면 `settings.gradle.kts`로 작성합니다. Gradle 스크립트에 허용되는 언어는 이 두 가지뿐입니다.
- 프로젝트를 최초에 인식시키는 파일이므로 항상 **프로젝트 루트 디렉터리**에 위치해야 합니다.

### settings 파일이 담당하는 핵심 역할 두 가지

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

### 핵심 포인트: 평가 시점이 다르다
settings 스크립트는 **다른 어떤 build 스크립트보다 먼저 평가**됩니다. 이 특성 때문에 settings 파일은 프로젝트 구조 선언 외에도 빌드 전역(build-wide)에 영향을 주는 설정을 다루기에 적합한 위치가 됩니다. 대표적으로 다음과 같은 것들이 여기서 다뤄집니다.
- 플러그인 관리(plugin management)
- 복합 빌드(included builds) 구성
- 버전 카탈로그(version catalogs) 설정

### settings.gradle(.kts) vs build.gradle(.kts)
같은 `.gradle(.kts)` 확장자를 쓰지만 역할이 완전히 다릅니다.

| 구분 | settings.gradle(.kts) | build.gradle(.kts) |
|---|---|---|
| 평가 시점 | 가장 먼저 평가됨 | settings 파일 이후 평가됨 |
| 다루는 대상 | 프로젝트 전체 구조(루트 이름, 서브프로젝트 목록) | 개별 프로젝트의 태스크·의존성·플러그인 적용 |
| 존재 위치 | 프로젝트 루트에 1개만 존재 | 루트 및 각 서브프로젝트별로 존재 가능 |
| 필수 여부 | 멀티 프로젝트에서는 필수 | 프로젝트 설정에 따라 다름 |

### 요약
- settings 파일은 "이 빌드에 어떤 프로젝트들이 존재하는가"를 정의하는 곳이고, build 파일은 "각 프로젝트가 무엇을 하는가"를 정의하는 곳이다.
- 멀티 프로젝트 빌드를 구성한다면 `rootProject.name` 지정과 `include()`를 통한 서브프로젝트 등록이 가장 먼저 챙겨야 할 작업이다.
- 다른 build 스크립트보다 먼저 실행된다는 점 때문에, 플러그인 관리나 버전 카탈로그처럼 빌드 전역 설정을 놓기에 적절한 위치이다.

---

## 태스크 기초 (Task Basics)

> **원문:** https://docs.gradle.org/current/userguide/task_basics.html

### 개요
**태스크**(Task)는 Gradle 빌드가 수행하는 "독립적인 작업 단위"를 말합니다. 소스 코드 컴파일, JAR 생성, Javadoc 생성, 아티팩트 배포 등 빌드 과정에서 이루어지는 개별 작업이 모두 태스크로 표현되며, 태스크는 Gradle 빌드를 구성하는 가장 기본적인 블록입니다.

태스크가 다루는 대표적인 작업 유형은 다음과 같습니다.

- 소스 코드 컴파일
- 테스트 실행
- 결과물 패키징 (JAR, APK 등)
- 문서 생성 (Javadoc 등)
- 저장소로 아티팩트 배포

각 태스크는 독립적으로 동작하지만 다른 태스크의 완료를 전제로 실행되도록 서로 의존할 수 있습니다. Gradle은 이 의존 관계 정보를 바탕으로 가장 효율적인 실행 순서를 계산하고, 이미 최신 상태인 태스크는 건너뜁니다.

### 태스크 실행하기

프로젝트 루트 디렉터리에서 Gradle Wrapper를 이용해 태스크를 실행합니다.

```bash
./gradlew build
```

- 위 명령은 `build` 태스크뿐 아니라 그 태스크가 의존하는 모든 태스크를 함께 실행합니다.
- `application` 플러그인을 적용한 프로젝트라면 `run` 태스크로 애플리케이션을 바로 실행할 수 있습니다 (`./gradlew run`). 이때 Gradle은 컴파일 등 필요한 선행 작업을 자동으로 처리한 뒤 애플리케이션을 구동합니다.

### 사용 가능한 태스크 목록 확인하기

빌드 스크립트와 적용된 플러그인이 어떤 태스크를 제공하는지는 `tasks` 태스크로 확인할 수 있습니다.

```bash
./gradlew tasks
```

- 결과는 **Application tasks**, **Build tasks**, **Documentation tasks**, **Other tasks** 등 카테고리별로 정리되어 출력됩니다.
- 목록에서 확인한 임의의 태스크는 `./gradlew <태스크 이름>` 형태로 바로 실행할 수 있습니다.

### 태스크 간 의존성

대부분의 태스크는 홀로 동작하지 않고 다른 태스크에 의존합니다. Gradle은 어떤 태스크가 어떤 태스크에 의존하는지 파악하고 있으며, 이를 바탕으로 올바른 순서로 자동 실행합니다.

예를 들어 `build` 태스크를 실행하면, `build`가 의존하는 `compileJava`, `test`, `jar` 등의 태스크가 먼저 실행된 뒤 최종적으로 `build`가 완료됩니다. 개발자가 실행 순서를 직접 지정할 필요가 없으며, Gradle이 의존성 그래프를 따라 순서를 결정합니다.

### 핵심 포인트

- 태스크는 컴파일·테스트·패키징·문서화·배포 등 빌드를 구성하는 독립적인 작업 단위이다.
- `./gradlew <태스크 이름>`으로 태스크를 직접 실행할 수 있으며, 해당 태스크가 의존하는 다른 태스크도 함께 실행된다.
- `./gradlew tasks`로 현재 프로젝트에서 사용 가능한 태스크를 카테고리별로 확인할 수 있다.
- 태스크 간 의존 관계는 Gradle이 자동으로 파악하여 올바른 순서로 실행하고, 이미 최신 상태인 태스크는 건너뛴다.

---

## Gradle 플러그인 기초

> **원문:** https://docs.gradle.org/current/userguide/plugin_basics.html

### 개요
Gradle 자체는 의존성 해석, 태스크 오케스트레이션, 증분 빌드 같은 핵심 인프라만 담당하고, 실제로 자주 쓰는 기능(자바 컴파일, 안드로이드 빌드, 아티팩트 배포 등)의 대부분은 **플러그인**이 제공합니다. 즉 Gradle은 "얇은 코어 + 플러그인 확장" 구조를 지향하는 빌드 시스템입니다.

### 플러그인이 하는 일
플러그인을 적용하면 빌드 스크립트에 다음과 같은 것들이 추가됩니다.

- **새로운 태스크** — 예: `compileJava`, `test`
- **새로운 설정(configuration)** — 예: `implementation`, `runtimeOnly`
- **DSL 요소** — 예: `application { ... }`, `publishing { ... }`

플러그인 하나를 적용하는 것만으로 위 세 가지가 한꺼번에 빌드에 편입되는 경우가 많습니다. 예를 들어 `java-library` 플러그인은 자바 소스 컴파일용 태스크, `implementation`/`api` 같은 설정, 관련 DSL을 동시에 가져옵니다.

### 플러그인 적용 문법
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

### 플러그인의 세 가지 분류

#### 1. 코어(Core) 플러그인
Gradle 배포판에 기본 내장된 플러그인으로, Gradle 팀이 직접 관리합니다. 별도의 다운로드나 버전 지정 없이 짧은 이름만으로 바로 적용할 수 있는 것이 특징입니다.

```kotlin
plugins {
    id("java-library")
}
```

#### 2. 커뮤니티(Community) 플러그인
Gradle 생태계의 서드파티 개발자·조직이 만들어 보통 **Gradle Plugin Portal**에 공개한 플러그인입니다. 코어 플러그인과 달리 ID와 **버전을 함께** 명시해야 하며, 빌드 실행 시 Gradle이 자동으로 다운로드합니다.

```kotlin
plugins {
    id("org.springframework.boot").version("3.1.5")
}
```

#### 3. 커스텀 / 로컬 플러그인
조직이나 개인이 단일 프로젝트 또는 멀티 프로젝트 빌드 전용으로 직접 작성한 플러그인입니다. 배포 방식과 무관하게 적용 문법은 커뮤니티 플러그인과 동일하게 이름(ID)으로 지정합니다.

```kotlin
plugins {
    id("my.custom-conventions")
}
```

### 핵심 포인트
- Gradle의 실질적 기능 대부분은 코어가 아니라 **플러그인**에서 나온다.
- 플러그인은 태스크·설정(configuration)·DSL을 한 번에 빌드에 추가하는 확장 단위다.
- 적용은 항상 `plugins { id("...") }` 형태이며, 코어 플러그인만 버전 지정 없이 짧은 이름으로 쓸 수 있다.
- 플러그인은 **코어(내장) / 커뮤니티(Plugin Portal 배포) / 커스텀(자체 제작)** 세 부류로 나뉘고, 커뮤니티·커스텀 플러그인은 보통 버전을 함께 지정한다.
