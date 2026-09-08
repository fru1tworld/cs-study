# Gradle 기초: 핵심 개념과 구성 파일

## Gradle 핵심 개념 (Core Concepts)

> 원문: https://docs.gradle.org/current/userguide/gradle_basics.html

### 개요

Gradle은 빌드 스크립트를 바탕으로 소프트웨어의 빌드, 테스트, 배포를 자동화한다. 설정은 Groovy 또는 Kotlin DSL로 작성하며, JVM 계열(Java, Kotlin)뿐 아니라 C++, Swift 프로젝트에서도 권장되는 빌드 시스템이다.

### 핵심 용어

Gradle의 빌드 구성을 읽으려면 빌드, 프로젝트, 태스크가 어떻게 연결되는지 먼저 알아야 한다. 다음 여섯 가지 용어가 그 관계를 설명한다.

- 빌드(Build): 결과물을 만들어내는 과정과 환경 전체
  - 하나의 빌드는 하나 이상의 프로젝트와 그 빌드 스크립트로 구성됨
- 프로젝트(Project): 빌드 대상이 되는 소프트웨어 단위
  - 애플리케이션이나 라이브러리 하나가 프로젝트가 됨
  - 하나의 빌드 안에 여러 프로젝트(멀티 프로젝트 빌드)가 존재 가능
- 태스크(Task): 컴파일, 테스트 실행처럼 실제로 수행되는 작업의 최소 단위
  - Gradle 빌드는 결국 태스크들의 실행 그래프임
- 빌드 스크립트(Build Script): `build.gradle` 또는 `build.gradle.kts` 파일
  - 해당 프로젝트에서 어떤 태스크와 의존성을 사용할지 정의함
- 플러그인(Plugin): Gradle의 기능을 확장하는 단위
  - 예를 들어 Java 플러그인은 컴파일, 테스트, 패키징 태스크를 한 번에 추가함
- 의존성(Dependency): 프로젝트가 필요로 하는 외부 라이브러리나 내부 모듈 등의 리소스

하나의 빌드에는 여러 프로젝트가, 각 프로젝트에는 여러 태스크가 속한다. 플러그인을 적용하면 프로젝트에 새로운 태스크와 설정이 추가되고, 의존성은 컴파일이나 테스트 같은 태스크가 동작하는 데 필요한 재료를 공급한다.

### 표준 프로젝트 구조

이 개념들은 프로젝트 안에서 다음과 같은 파일과 디렉터리로 나타난다.

```text
project/
├── gradle/              # Wrapper 관련 파일 및 설정
├── gradlew              # Wrapper 실행 스크립트 (Unix)
├── gradlew.bat          # Wrapper 실행 스크립트 (Windows)
├── settings.gradle(.kts) # 루트 프로젝트 및 서브프로젝트 목록 정의
├── build.gradle(.kts)   # 프로젝트별 빌드 스크립트
└── src/                 # 소스 코드
```

- `settings.gradle(.kts)`는 이 빌드에 포함된 프로젝트들을 선언하는 최상위 파일임
- `build.gradle(.kts)`는 각 프로젝트마다 존재 가능 → 해당 프로젝트에 적용할 플러그인, 의존성, 태스크 설정을 담음
- `gradlew`, `gradlew.bat` 파일 존재 = 해당 프로젝트가 Gradle 빌드이며 Wrapper를 사용 중이라는 표시임

### Gradle을 실행하는 세 가지 방법

- 1\. IDE 통합: Android Studio, IntelliJ IDEA, VS Code, Eclipse, NetBeans 등 대부분의 주요 IDE가 Gradle을 기본 내장 → GUI에서 바로 빌드, 실행 가능
- 2\. 시스템에 직접 설치한 Gradle 사용: `gradle` 명령을 전역으로 설치해 사용하는 방식
- 3\. Gradle Wrapper 사용(권장): 프로젝트에 포함된 스크립트로, 프로젝트가 지정한 특정 버전의 Gradle을 자동으로 내려받아 실행함 → 팀원 전체가 동일한 Gradle 버전으로 빌드하도록 보장 → 공식 권장 실행 방식

```bash
$ ./gradlew build
```

> 참고: `gradle build`, `gradle test`, `gradle clean build`처럼 원하는 태스크 이름을 명령줄에 나열해 실행 가능 → 여러 태스크를 한 줄에 이어서 지정 가능

Wrapper를 쓰면 로컬과 CI 환경에 Gradle을 별도로 설치하지 않아도 프로젝트가 지정한 버전으로 빌드할 수 있다. 개발 환경마다 Gradle 버전이 달라 생기는 문제도 줄어든다.

## 빌드 파일 기초 (Build File Basics)

> 원문: https://docs.gradle.org/current/userguide/build_file_basics.html

### 개요

빌드 스크립트는 프로젝트를 어떻게 빌드할지 정의한다. 사용하는 DSL에 따라 `build.gradle`(Groovy) 또는 `build.gradle.kts`(Kotlin)라는 이름을 쓰며, 하나의 Gradle 빌드에는 최소 한 개의 빌드 스크립트가 필요하다. 멀티 프로젝트 구성에서는 보통 각 서브프로젝트가 자신의 루트 디렉터리에 별도의 빌드 스크립트를 둔다.

### 빌드 스크립트가 다루는 3가지 요소

- 빌드 스크립트는 대체로 다음 세 가지를 정의함

- 플러그인(Plugins): Gradle의 기능을 확장하고 프로젝트에 태스크와 관례(convention)를 추가함
  - 플러그인을 "적용(apply)"하면 컴파일, 테스트, 패키징 등의 부가 기능을 사용 가능해짐
- 의존성(Dependencies): 소스 코드를 컴파일, 실행, 테스트하는 데 필요한 외부 라이브러리를 선언함
  - 크게 두 종류로 나뉨
  - Gradle/빌드 스크립트 자체가 필요로 하는 의존성(예: 빌드 로직에 쓰이는 플러그인, 라이브러리)
  - 프로젝트 소스 코드가 필요로 하는 의존성(실제 애플리케이션 라이브러리)
- 관례 속성(Convention Properties): 플러그인이 프로젝트에 추가하는 속성, 메서드로, 플러그인이 제공하는 기능을 세부적으로 설정할 때 사용함(예: 애플리케이션의 메인 클래스 지정)

### 예시로 보는 구조

- 애플리케이션용 빌드 파일은 보통 아래와 같은 세 블록으로 구성됨

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

- `plugins {}` 블록: 어떤 플러그인을 적용할지 선언 (예: `application` 플러그인은 내부적으로 Java 플러그인도 함께 적용)
- `dependencies {}` 블록: `implementation`, `testImplementation`, `testRuntimeOnly` 등 의존성 구성(configuration) 종류별로 라이브러리를 선언
- 플러그인이 제공하는 관례 블록(위 예시의 `application {}`): 플러그인 동작을 세부 조정

### Groovy DSL vs Kotlin DSL

- Gradle 빌드 스크립트를 작성할 수 있는 언어는 Groovy DSL과 Kotlin DSL 두 가지뿐임
  - 기능적으로는 동등하며 문법 스타일만 다름

- Groovy DSL
  - 파일명: `build.gradle`
  - 타입 체크: 동적 타입, 간결한 문법
- Kotlin DSL
  - 파일명: `build.gradle.kts`
  - 타입 체크: 정적 타입, IDE 자동완성, 검증에 유리

### 빌드 스크립트가 하는 일

- 의존성 선언 및 관리
- 태스크(task) 설정 및 커스터마이징
- 버전 카탈로그(version catalog), 컨벤션 플러그인(convention plugin) 등과의 연동

- 빌드 스크립트는 Gradle의 구성 단계(configuration phase)에서 실행됨 → 이후 실제 빌드 실행(태스크 실행 단계)의 기반을 마련하는 역할

## Settings 파일 기초 (Settings File Basics)

> 원문: https://docs.gradle.org/current/userguide/settings_file_basics.html

### 개요

빌드 스크립트가 개별 프로젝트의 작업을 정의한다면, `settings.gradle(.kts)`는 빌드에 포함될 프로젝트의 구조를 정의한다. Gradle은 태스크 실행이나 의존성 해석에 앞서 이 파일을 읽어 전체 구조를 파악한다.

### 언제 필요한가

- 단일 프로젝트 빌드: settings 파일이 없어도 동작함 → 파일이 없으면 Gradle은 해당 빌드를 단일 프로젝트로 취급
- 멀티 프로젝트 빌드: 포함된 모든 서브프로젝트를 이 파일에 명시적으로 선언 필요 → settings 파일 필수

### 파일 이름과 위치

- Groovy DSL을 쓰면 `settings.gradle`, Kotlin DSL을 쓰면 `settings.gradle.kts`로 작성
  - Gradle 스크립트에 허용되는 언어는 이 두 가지뿐임
- 프로젝트를 최초에 인식시키는 파일 → 항상 프로젝트 루트 디렉터리에 위치 필요

### settings 파일이 담당하는 핵심 역할 두 가지

- 1\. 루트 프로젝트 이름 지정

```kotlin
rootProject.name = "root-project"
```
- 모든 빌드는 정확히 하나의 루트 프로젝트를 가짐 → 그 이름은 settings 파일에서 지정

- 2\. 서브프로젝트 포함

```kotlin
include("sub-project-a")
include("sub-project-b")
```
- `include()` 함수로 멀티 프로젝트 구조에 포함될 서브프로젝트들을 선언

- 이 두 선언에 따라 아래와 같은 디렉터리 구조가 만들어짐

```
.
├── settings.gradle(.kts)
├── sub-project-a
│   └── build.gradle(.kts)
└── sub-project-b
    └── build.gradle(.kts)
```

settings 스크립트는 모든 build 스크립트보다 먼저 평가된다. 따라서 프로젝트 구조뿐 아니라 빌드 전체(build-wide)에 영향을 주는 다음 설정도 이곳에서 다루기 적합하다.

- 플러그인 관리(plugin management)
- 복합 빌드(included builds) 구성
- 버전 카탈로그(version catalogs) 설정

### settings.gradle(.kts) vs build.gradle(.kts)

- 같은 `.gradle(.kts)` 확장자를 쓰지만 역할이 완전히 다름

- settings.gradle(.kts)
  - 평가 시점: 가장 먼저 평가됨
  - 다루는 대상: 프로젝트 전체 구조(루트 이름, 서브프로젝트 목록)
  - 존재 위치: 프로젝트 루트에 1개만 존재
  - 필수 여부: 멀티 프로젝트에서는 필수
- build.gradle(.kts)
  - 평가 시점: settings 파일 이후 평가됨
  - 다루는 대상: 개별 프로젝트의 태스크, 의존성, 플러그인 적용
  - 존재 위치: 루트 및 각 서브프로젝트별로 존재 가능
  - 필수 여부: 프로젝트 설정에 따라 다름

## 태스크 기초 (Task Basics)

> 원문: https://docs.gradle.org/current/userguide/task_basics.html

### 개요

태스크(Task)는 Gradle 빌드가 수행하는 독립적인 작업 단위다. 소스 코드 컴파일부터 아티팩트 배포까지 빌드 과정의 개별 작업을 태스크로 표현한다.

태스크가 다루는 대표적인 작업 유형은 다음과 같다.

- 소스 코드 컴파일
- 테스트 실행
- 결과물 패키징 (JAR, APK 등)
- 문서 생성 (Javadoc 등)
- 저장소로 아티팩트 배포

- 각 태스크는 독립적으로 동작하지만 다른 태스크의 완료를 전제로 실행되도록 서로 의존 가능 → Gradle은 이 의존 관계 정보를 바탕으로 가장 효율적인 실행 순서를 계산하고, 이미 최신 상태인 태스크는 건너뜀

### 태스크 실행하기

- 프로젝트 루트 디렉터리에서 Gradle Wrapper를 이용해 태스크를 실행함

```bash
./gradlew build
```

- 위 명령은 `build` 태스크뿐 아니라 그 태스크가 의존하는 모든 태스크를 함께 실행함
- `application` 플러그인을 적용한 프로젝트라면 `run` 태스크로 애플리케이션을 바로 실행 가능(`./gradlew run`) → 이때 Gradle은 컴파일 등 필요한 선행 작업을 자동으로 처리한 뒤 애플리케이션을 구동함

### 사용 가능한 태스크 목록 확인하기

- 빌드 스크립트와 적용된 플러그인이 어떤 태스크를 제공하는지는 `tasks` 태스크로 확인 가능

```bash
./gradlew tasks
```

- 결과는 `Application tasks`, `Build tasks`, `Documentation tasks`, `Other tasks` 등 카테고리별로 정리되어 출력됨
- 목록에서 확인한 임의의 태스크는 `./gradlew <태스크 이름>` 형태로 바로 실행 가능

### 태스크 간 의존성

대부분의 태스크는 다른 태스크의 결과가 있어야 실행할 수 있다. Gradle은 이 의존 관계를 파악해 실행 순서를 결정한다. 예를 들어 `build`를 실행하면 그 태스크가 의존하는 `compileJava`, `test`, `jar` 등이 먼저 실행된 뒤 `build`가 완료된다. 개발자가 매번 순서를 지정할 필요 없이 Gradle이 의존성 그래프를 따라 처리한다.

## Gradle 플러그인 기초

> 원문: https://docs.gradle.org/current/userguide/plugin_basics.html

### 개요

앞에서 본 컴파일과 테스트 태스크를 모든 프로젝트에 직접 정의할 필요는 없다. 자바 컴파일, 안드로이드 빌드, 아티팩트 배포처럼 자주 쓰는 기능은 대부분 플러그인이 제공한다. Gradle 자체는 의존성 해석, 태스크 오케스트레이션, 증분 빌드 같은 핵심 인프라를 담당한다.

### 플러그인이 하는 일

- 플러그인을 적용하면 빌드 스크립트에 다음과 같은 것들이 추가됨

- 새로운 태스크: `compileJava`, `test` 등
- 새로운 설정(configuration): `implementation`, `runtimeOnly` 등
- DSL 요소: `application { ... }`, `publishing { ... }` 등

하나의 플러그인이 이 세 가지를 함께 추가하는 경우가 많다. `java-library`를 적용하면 자바 소스 컴파일용 태스크와 `implementation`/`api` 같은 설정, 관련 DSL을 한 번에 사용할 수 있다.

### 플러그인 적용 문법

- 플러그인은 빌드 스크립트 상단의 `plugins { }` 블록에서 플러그인 ID(전역적으로 유일한 식별자)와 필요하면 버전을 지정해 적용함

```kotlin
plugins {
    id("«plugin id»").version("«plugin version»")
}
```

- 여러 플러그인을 동시에 적용하는 것도 자연스러운 패턴임

```kotlin
plugins {
    id("java-library")
    id("com.diffplug.spotless").version("6.25.0")
}
```

- 위 예시는 자바 라이브러리 컴파일 기능과 코드 포맷팅 도구(Spotless)를 함께 활성화함

### 플러그인의 세 가지 분류

#### 1. 코어(Core) 플러그인

- Gradle 배포판에 기본 내장된 플러그인 → Gradle 팀이 직접 관리함
  - 별도의 다운로드나 버전 지정 없이 짧은 이름만으로 바로 적용 가능한 것이 특징

```kotlin
plugins {
    id("java-library")
}
```

#### 2. 커뮤니티(Community) 플러그인

- Gradle 생태계의 서드파티 개발자, 조직이 만들어 보통 Gradle Plugin Portal에 공개한 플러그인임
  - 코어 플러그인과 달리 ID와 버전을 함께 명시 필요 → 빌드 실행 시 Gradle이 자동으로 다운로드함

```kotlin
plugins {
    id("org.springframework.boot").version("3.1.5")
}
```

#### 3. 커스텀 / 로컬 플러그인

- 조직이나 개인이 단일 프로젝트 또는 멀티 프로젝트 빌드 전용으로 직접 작성한 플러그인임
  - 배포 방식과 무관하게 적용 문법은 커뮤니티 플러그인과 동일하게 이름(ID)으로 지정함

```kotlin
plugins {
    id("my.custom-conventions")
}
```
