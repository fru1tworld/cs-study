# Gradle 튜토리얼 1편: 프로젝트 초기화

> **원문:** https://docs.gradle.org/current/userguide/part1_gradle_init.html

## 개요

Gradle 초보자 튜토리얼의 첫 단계로, 간단한 Java 애플리케이션을 직접 만들어보면서 태스크·플러그인·프로젝트 구조 같은 핵심 개념을 몸으로 익히는 데 목적이 있습니다. 이 문서를 마치면 다음을 할 수 있게 됩니다.

- `gradle init`으로 새 프로젝트 뼈대를 생성
- Wrapper(`gradlew`)로 프로젝트를 빌드
- Gradle의 표준 디렉터리 구조를 파악
- 자동 생성된 설정 파일들의 역할을 이해

## 사전 준비물

- Gradle이 로컬에 설치되어 있어야 함 (`gradle` 명령 실행 가능 여부로 확인)
- (선택) IntelliJ IDEA Community Edition — 프로젝트 구조를 시각적으로 확인할 때 사용

실습은 macOS 셸 기준이지만 Windows/Linux에서도 동일한 절차가 적용되며, IDE는 무엇을 쓰든 상관없습니다.

## 프로젝트 생성 절차

### 1) 작업 디렉터리 준비

```bash
$ mkdir tutorial
$ cd tutorial
```

### 2) `gradle init`으로 스캐폴딩

```bash
$ gradle init --type java-application --dsl kotlin
```

- `--type java-application`: Java 애플리케이션 템플릿 지정
- `--dsl kotlin`: 빌드 스크립트를 Kotlin DSL로 작성 (Groovy를 쓰려면 `--dsl groovy`)
- 이후 대화형 질문이 이어지는데, 튜토리얼에서는 기본값을 그대로 선택해도 무방합니다.

## 생성된 프로젝트 구조

`gradle init` 실행 후 아래와 비슷한 구조가 만들어집니다.

```text
tutorial/
├── .gradle/                # 프로젝트별 캐시 (자동 생성)
├── gradle/
│   ├── libs.versions.toml  # 버전 카탈로그
│   └── wrapper/            # Wrapper 설정·JAR
├── gradlew                 # Wrapper 실행 스크립트 (Unix)
├── gradlew.bat             # Wrapper 실행 스크립트 (Windows)
├── settings.gradle.kts     # 루트 프로젝트 및 서브프로젝트 선언
└── app/                    # 애플리케이션 서브프로젝트
    ├── build.gradle.kts
    └── src/
```

각 요소의 역할은 다음과 같습니다.

- **`.gradle/`**: Gradle이 빌드 성능을 위해 자동으로 만드는 프로젝트별 캐시 디렉터리로, 직접 건드릴 일은 없습니다.
- **`gradle/`**: Wrapper가 사용할 특정 Gradle 버전의 설정과 JAR을 담습니다.
- **`libs.versions.toml`**: 프로젝트 전체에서 쓰는 의존성 버전을 한곳에 모아 관리하는 버전 카탈로그입니다.
- **`gradlew` / `gradlew.bat`**: 시스템에 Gradle이 없어도 지정된 버전으로 빌드할 수 있게 해주는 실행 스크립트로, 버전 관리에 반드시 포함시켜야 합니다.
- **`app/`**: 실제 소스 코드와 해당 서브프로젝트만의 빌드 스크립트를 담는 서브프로젝트 디렉터리입니다.

## 핵심 포인트: Wrapper를 쓰는 이유

- 최초 실행 시 필요한 Gradle 배포판을 자동으로 내려받아 로컬 캐시에 저장합니다.
- 팀원·CI 모두 프로젝트가 지정한 동일한 Gradle 버전으로 빌드하게 되어 "버전 불일치" 문제를 원천 차단합니다.
- 시스템에 Gradle을 따로 설치하지 않아도 되므로, 실무에서는 `gradle` 명령보다 `./gradlew`를 쓰는 것이 표준 관행입니다.

## Wrapper로 빌드하기

```bash
$ ./gradlew build      # macOS/Linux
$ gradlew.bat build    # Windows
```

처음 실행하면 지정된 버전의 Gradle 배포판(예: `gradle-8.13-bin.zip`)을 다운로드한 뒤 빌드를 진행하며, 성공 시 `BUILD SUCCESSFUL` 메시지와 실행된 태스크 수가 출력됩니다. 빌드가 끝나면 `app/build/` 디렉터리에 컴파일 결과물 등 생성물이 쌓입니다.

## 프로젝트 구조 이해: 빌드와 프로젝트

- **빌드(Build)**: 여러 컴포넌트를 함께 개발·배포하기 위해 묶어놓은 소프트웨어 단위 전체. `tutorial`이 하나의 빌드입니다.
- **프로젝트(Project)**: 라이브러리, 애플리케이션, 플러그인 등 개별 구성 요소. 하나의 빌드 안에 여러 프로젝트(루트 + 서브프로젝트)가 존재할 수 있습니다.
- 이 예제에서는 `tutorial`이 루트 프로젝트, `app`이 그 안에 포함된 서브프로젝트입니다.

> 루트 디렉터리에는 별도의 `build.gradle(.kts)`를 두지 않는 것이 권장됩니다. 각 서브프로젝트가 자기 설정을 독립적으로 갖도록 하는 것이 Gradle의 일반적인 관례입니다.

## 설정 파일 뜯어보기

### `settings.gradle.kts` (루트)

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tutorial"
include("app")
```

- `rootProject.name`으로 빌드 전체의 이름을 지정
- `include("app")`으로 `app` 디렉터리를 서브프로젝트로 등록 — 이 선언이 있어야 Gradle이 해당 폴더를 프로젝트로 인식합니다.

### `app/build.gradle.kts` (서브프로젝트)

```kotlin
plugins {
    application
}

dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass = "org.example.App"
}
```

- **`plugins { application }`**: 실행 가능한 CLI 애플리케이션을 만들기 위한 플러그인 적용
- **`repositories`**: 의존성을 어디서 내려받을지 지정 (예: Maven Central)
- **`dependencies`**: 컴파일/테스트에 필요한 라이브러리 선언
- **`java.toolchain`**: 빌드에 사용할 JDK 버전 고정
- **`application.mainClass`**: 실행 시 진입점이 될 메인 클래스 지정
- **`tasks.named<Test>("test")`**: 테스트 실행 방식(예: JUnit Platform 사용) 커스터마이징

## 핵심 포인트: 파일 두 개로 나눠 생각하기

- `settings.gradle(.kts)`는 "이 빌드에 어떤 프로젝트들이 있는가"를 정의하는 지도 역할을 합니다.
- `build.gradle(.kts)`는 "이 프로젝트가 무엇을 어떻게 빌드하는가"를 정의하는 실행 설명서 역할을 합니다.
- 서브프로젝트가 늘어나면 각자의 `build.gradle(.kts)`가 추가되지만, `settings.gradle(.kts)`는 루트에 하나만 존재합니다.

## (선택) IDE에서 열어보기

IntelliJ IDEA를 쓴다면 `settings.gradle.kts` 파일을 더블클릭해서 열면 프로젝트 구조를 트리 형태로 확인할 수 있고, 설정 파일에 문법 강조와 자동완성이 적용됩니다.

## 다음 단계

이 편에서는 프로젝트 초기화, Wrapper의 역할, 표준 디렉터리 구조, 핵심 설정 파일의 의미를 다뤘습니다. 다음 편인 "[태스크 실행하기](part2_gradle_tasks.html)"에서는 실제로 빌드 태스크를 실행하고 그 동작 방식을 살펴봅니다.
