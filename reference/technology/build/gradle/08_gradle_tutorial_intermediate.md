# 튜토리얼: 중급 (Intermediate)

## Gradle 중급 튜토리얼 1편: 프로젝트 초기화와 애플리케이션 실행

> 원문: https://docs.gradle.org/current/userguide/part1_gradle_init_project.html

### 개요

초급 튜토리얼에서 `gradle init`과 디렉터리 구조를 익혔다면, 이번에는 생성된 코드를 실행하고 테스트한 뒤 패키징한다. 이 과정을 통해 다음 작업을 익힌다.

- Java 애플리케이션 프로젝트 초기화
- 생성된 디렉터리 구조와 소스 코드 읽어내기
- `./gradlew run`으로 애플리케이션 실행
- `--scan` 옵션으로 Build Scan을 생성해 빌드 내역을 시각적으로 확인
- `./gradlew build`로 실행 가능한 배포 아카이브(zip/tar) 생성

### 사전 준비물

- Gradle이 로컬에 설치되어 있어야 함
- (선택) IntelliJ IDEA Community Edition: 코드 탐색용

### 1) 프로젝트 초기화

```bash
$ mkdir authoring-tutorial
$ cd authoring-tutorial
$ gradle init --type java-application --dsl kotlin
```

- `--dsl kotlin` 대신 `--dsl groovy`를 쓰면 Groovy DSL로 생성됨
- 이어지는 대화형 질문은 기본값을 그대로 선택해도 됨

### 2) 생성된 디렉터리 구조

```text
authoring-tutorial/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── gradlew, gradlew.bat
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/java/.../App.java
        └── test/java/.../AppTest.java
```

- 초급 편에서 본 구조와 동일하지만, 이번에는 `src/main`, `src/test` 밑에 실제로 생성되는 자바 소스와 테스트 코드까지 확인
- `GRADLE_USER_HOME`(기본값 `~/.gradle`)은 프로젝트 밖에 위치하는 전역 저장소로, Gradle 배포판 캐시, 다운로드한 의존성, 로그 등을 담아 여러 프로젝트가 공유

### 3) 설정 파일 다시 보기

- `settings.gradle.kts`는 빌드 이름과 서브프로젝트 목록만 선언하는 최소 구성임

```kotlin
rootProject.name = "authoring-tutorial"
include("app")
```

- `app/build.gradle.kts`는 실제 빌드 동작을 정의함

```kotlin
plugins { id("application") }

dependencies {
    implementation(libs.guava)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass = "org.example.App"
}
```

- `application` 플러그인: `run`, `build`, `assemble` 등 애플리케이션 실행/배포용 태스크를 자동으로 추가
- `dependencies`의 `libs.guava`, `libs.junit.jupiter`는 `gradle/libs.versions.toml` 버전 카탈로그를 참조하는 표기법
- `java.toolchain`으로 빌드에 쓸 JDK 버전을 고정해, 로컬 JDK 버전과 무관하게 일관된 빌드를 보장
- `tasks.named<Test>("test") { useJUnitPlatform() }`로 JUnit 5 실행 방식을 지정

### 4) 생성된 소스 코드 훑어보기

- `App.java`: `getGreeting()`이 문자열을 반환하고 `main()`이 이를 출력하는 최소 예제
- `AppTest.java`: JUnit Jupiter의 `@Test`로 `getGreeting()`이 `null`이 아님을 검증하는 단위 테스트 1개
- 별도로 손대지 않아도 곧바로 실행, 테스트가 가능한 상태로 스캐폴딩됨

### 5) 애플리케이션 실행하기

```bash
$ ./gradlew run
```

- `application` 플러그인이 추가한 `run` 태스크가 `application.mainClass`로 지정된 클래스의 `main()`을 호출함
  - 콘솔에는 실행된 태스크 목록과 함께 `Hello World!` 같은 출력, 이어서 `BUILD SUCCESSFUL` 메시지가 나타남

### 6) 배포 아카이브 만들기

실행을 확인했으면 `build`로 컴파일, 테스트, 아카이브 생성을 함께 수행한다. `build`는 여러 태스크의 의존 관계로 이루어져 있다. 대표적으로 `compileJava → classes → jar → startScripts → distZip/distTar → assemble`과 `compileTestJava → test → check` 흐름을 거쳐 완료된다.

```bash
$ ./gradlew build
```

- 빌드가 끝나면 `app/build/distributions/` 아래에 `app.zip`, `app.tar` 두 종류의 아카이브가 생성됨
  - 각 아카이브에는 애플리케이션 실행 스크립트와 필요한 의존성 JAR이 함께 담겨 있어, 압축을 풀기만 하면 다른 환경에서도 바로 실행 가능

### 7) Build Scan으로 빌드 들여다보기

```bash
$ ./gradlew build --scan
```

- 최초 실행 시 Gradle 이용 약관 동의를 물어보며, 동의하면 `scans.gradle.com`에 업로드된 리포트 링크가 출력됨
- 리포트에는 실행된 태스크 목록, 다운로드된 의존성, 각 단계별 소요 시간 등 빌드 전반의 성능, 구성 정보가 시각화되어 있어, 로컬 로그만으로 파악하기 어려운 병목이나 캐시 적중 여부를 확인하는 데 유용함

## Gradle 중급 튜토리얼 2편: 빌드 생명주기 이해하기

> 원문: https://docs.gradle.org/current/userguide/part2_build_lifecycle.html

### 개요

1편에서 초기화한 Java 프로젝트에 로그를 추가하고 `./gradlew task1`을 실행한다. 로그의 출력 순서를 보면 Gradle의 초기화, 구성, 실행 단계가 각각 언제 동작하는지 확인할 수 있다.

### 사전 준비물

- 1편에서 만든 `authoring-tutorial` 프로젝트(`gradle init --type java-application`)가 그대로 있어야 함

### 세 단계 복습

- 초기화(Initialization): 이 빌드에 어떤 프로젝트가 참여하는지 결정하고, 프로젝트마다 `Project` 인스턴스를 생성
- 구성(Configuration): 참여하는 모든 프로젝트의 빌드 스크립트를 평가해 `Project` 객체를 채우고, 실행할 태스크 집합을 결정
- 실행(Execution): 앞서 결정된 태스크들을 실제로 실행

### 실습 1: settings 파일에 로그 찍기

- `settings.gradle(.kts)` 맨 위에 `println`을 한 줄 추가함

```kotlin
// settings.gradle.kts
println("SETTINGS FILE: This is executed during the initialization phase")
```

- 이 파일은 항상 초기화 단계에서 평가되므로, 여기 적힌 코드는 다른 무엇보다 먼저 출력됨

### 실습 2: 빌드 스크립트에 태스크 두 개 등록하기

- `app/build.gradle.kts` 맨 아래에 `task1`, `task2`를 각각 `register`로 선언하고 `named`로 다시 참조해 `doFirst`/`doLast`를 붙임

```kotlin
println("BUILD SCRIPT: This is executed during the configuration phase")

tasks.register("task1") {
    println("REGISTER TASK1: configuration phase")
}
tasks.register("task2") {
    println("REGISTER TASK2: configuration phase")
}

tasks.named("task1") {
    println("NAMED TASK1: configuration phase")
    doFirst { println("TASK1 doFirst: execution phase") }
    doLast  { println("TASK1 doLast: execution phase") }
}
// task2도 동일한 패턴으로 named + doFirst/doLast 구성
```

- `register { ... }`, `named { ... }` 블록 본문 자체는 구성 단계에서 즉시 실행됨
- `doFirst`/`doLast`로 감싼 코드만 실행 단계로 지연됨

### 실습 3: `task1` 실행 결과로 확인하는 생명주기

```text
$ ./gradlew task1

SETTINGS FILE: ...                 ← ① 초기화
> Configure project :app
BUILD SCRIPT: ...
REGISTER TASK1: ...
NAMED TASK1: ...                   ← ② 구성
> Task :app:task1
TASK1 doFirst: ...
TASK1 doLast: ...                  ← ③ 실행

BUILD SUCCESSFUL in 25s
```

- ① 초기화: `settings.gradle(.kts)`를 실행해 참여 프로젝트를 정하고 `Project` 객체를 만듦
- ② 구성: `build.gradle(.kts)`를 실행해 각 프로젝트를 채우고, 의존성을 해석하며 태스크 그래프를 만듦
- ③ 실행: 커맨드라인에서 요청한 태스크와 그 선행 태스크를 실제로 실행

위 로그에는 `task1`의 등록과 설정 블록만 나타나며, `task2`의 `REGISTER TASK2`/`NAMED TASK2`는 출력되지 않는다. 요청한 `task1`이 `task2`에 의존하지 않으므로 Gradle이 `task2`의 구성을 건너뛴 것이다. 이처럼 필요한 태스크만 구성하는 최적화를 태스크 구성 회피라고 한다. 태스크가 많은 대형 멀티 프로젝트에서는 이 방식으로 구성 시간을 크게 줄일 수 있다.

## Gradle 튜토리얼 Part 3: 멀티 프로젝트 빌드

> 원문: https://docs.gradle.org/current/userguide/part3_multi_project_builds.html

### 개요

Part 1에서 초기화하고 Part 2에서 생명주기를 확인한 `authoring-tutorial` 예제는 루트 프로젝트와 `app` 서브프로젝트로 구성되어 있다. 이번에는 여기에 새 서브프로젝트를 추가한다. 이어서 별도의 Gradle 빌드를 포함하는 컴포짓 빌드를 살펴보며, 모듈을 나누는 두 방식의 차이를 확인한다.

### 멀티 프로젝트 빌드의 기본 골격

- 멀티 프로젝트 빌드는 세 가지 요소로 이루어짐

- 루트 `settings.gradle(.kts)`: 어떤 서브프로젝트들이 이 빌드에 속하는지 선언하는 진입점
- 서브프로젝트별 `build.gradle(.kts)`: 각 모듈이 자신만의 플러그인, 의존성, 태스크를 독립적으로 정의
- 서브프로젝트별 소스 디렉터리: 모듈 간 코드가 물리적으로도 분리됨

- 예제에서는 이미 `app`이라는 서브프로젝트가 존재하는 상태에서 시작함

### 서브프로젝트 추가하기: `lib`

- 새 서브프로젝트를 추가하는 과정은 다음 순서로 진행됨

- 1\. 루트에 `lib` 디렉터리를 만들고 `build.gradle.kts`를 작성해 Java 플러그인, 테스트 의존성(JUnit Jupiter), 외부 의존성(Guava) 등을 선언
- 2\. `lib/src/main/java/...` 아래에 실제 소스 코드(`CustomLib` 클래스 등)를 작성
- 3\. 루트 `settings.gradle(.kts)`에 `lib`을 추가

   ```kotlin
   include("app", "lib")
   ```

- 4\. `app/build.gradle.kts`에서 `lib`을 프로젝트 의존성으로 선언

   ```kotlin
   dependencies {
       implementation(project(":lib"))
   }
   ```

- 5\. `App.java`에서 `CustomLib`을 import해 사용하도록 코드를 수정한 뒤 `./gradlew run`으로 확인

- 이 과정을 마치면 Gradle이 `app`을 빌드하기 전에 의존 관계에 따라 `lib`을 먼저 빌드하고, 두 모듈이 각각 독립적으로도 빌드될 수 있음을 확인 가능

여기서 `implementation(project(":lib"))`의 콜론 경로는 같은 빌드의 서브프로젝트를 가리킨다. 외부 리포지토리의 라이브러리는 `implementation("com.google.guava:guava:33.3.1-jre")`처럼 GAV 좌표로 지정한다. 두 종류의 의존성을 같은 `dependencies {}` 블록에 함께 선언할 수 있다.

### 컴포짓 빌드란 무엇인가

컴포짓 빌드(Composite Build)는 독립된 Gradle 빌드를 다른 빌드에 포함해 결합하는 구조다. 앞서 추가한 서브프로젝트가 하나의 빌드 안에서 모듈을 나눈 것이라면, 여기서는 각 빌드의 독립성을 유지한 채 함께 사용한다. 원문에서는 다음과 같은 활용 목적을 제시한다.

- 프로젝트 빌드 로직 자체를 별도로 분리해 재사용
- 독립적으로 개발된 여러 빌드(예: 플러그인과 그 플러그인을 쓰는 애플리케이션)를 하나로 묶기
- 지나치게 커진 하나의 빌드를 여러 개의 격리된 작은 빌드로 쪼개기

### 빌드에 빌드 추가하기: `license-plugin`

- 컴포짓 빌드를 실습하기 위해 별도의 Gradle 플러그인 프로젝트를 만들어 메인 빌드에 포함시킴

- 1\. `gradle/license-plugin` 디렉터리에서 `gradle init --type kotlin-gradle-plugin`(또는 Groovy 버전)을 실행해 플러그인 전용 빌드를 새로 생성
  - 이 빌드는 자신만의 `settings.gradle(.kts)`, 소스, 테스트, Gradle Wrapper를 갖춘 완전히 독립된 프로젝트임
- 2\. 루트 `settings.gradle(.kts)`에 다음을 추가해 이 빌드를 포함

   ```kotlin
   includeBuild("gradle/license-plugin")
   ```

- 3\. `./gradlew projects`를 실행하면 루트 프로젝트 아래 서브프로젝트(`app`, `lib`) 목록과 별도로, 포함된 빌드(included builds) 섹션에 `license-plugin`이 표시됨

- 이후 태스크를 실행할 때 대상 범위가 뚜렷하게 구분됨

```bash
./gradlew build                          # app, lib 등 서브프로젝트 전체 빌드
./gradlew :app:build                     # app과 그 의존 대상만 빌드
./gradlew :license-plugin:plugin:build   # 포함된 빌드 내부의 플러그인만 빌드
```

이 대상 구분은 빌드의 격리 수준과도 연결된다. `include()`로 추가한 서브프로젝트는 같은 빌드에 속해 설정, 버전, 의존성 해석 규칙을 공유하고 하나의 `settings.gradle(.kts)`가 전체를 조율한다. `includeBuild()`로 포함한 빌드는 자기만의 `settings.gradle(.kts)`를 가지며, 메인 빌드는 플러그인이나 라이브러리 같은 결과물을 가져다 쓴다. 이런 격리 덕분에 `license-plugin`을 별도 팀이 독립적으로 개발하고 테스트하거나 여러 저장소에서 재사용하기 쉽다.

### 언제 무엇을 쓸까

- 모듈 수가 늘어나고 서로 의존 관계가 있는 애플리케이션(모바일 앱, 웹 앱, API, 라이브러리, 문서 모듈 등)을 한 저장소에서 관리한다면 멀티 프로젝트 빌드(서브프로젝트)가 기본 선택지
- 빌드 로직 자체(컨벤션 플러그인)를 분리해 재사용하거나, 라이브러리를 패치해서 테스트하는 등 "빌드와 빌드 사이"의 결합이 필요하다면 컴포짓 빌드(`includeBuild`)가 더 적합

## Part 4. Settings 파일 작성하기 (Writing the Settings File)

> 원문: https://docs.gradle.org/current/userguide/part4_settings_file.html

### 개요

- Intermediate Tutorial의 4번째 파트로, `settings.gradle(.kts)` 파일이 왜 모든 Gradle 빌드의 진입점(entry point)인지, 그리고 그 안에서 어떤 API와 DSL 요소들을 사용할 수 있는지를 다룸
  - Part 1(자바 앱 초기화), Part 2(빌드 라이프사이클), Part 3(서브프로젝트와 별도 빌드 추가)을 이해했다는 전제 위에서 진행됨

### 빌드 스크립트도 결국 "코드"다

`settings.gradle(.kts)`와 `build.gradle(.kts)`는 모두 Kotlin 또는 Groovy 코드이므로 일반 프로그램처럼 API 호출, 변수, 조건문을 사용할 수 있다. 스크립트에서 호출하는 요소의 출처는 크게 세 가지다.

- Gradle API: 예) `Settings` 인터페이스의 `getRootProject()`
- DSL 블록: 예) `plugins { }` 블록
- 플러그인이 제공하는 확장(extension): 예) `java` 플러그인이 제공하는 `implementation()`, `api()`

### Settings 객체란

- Gradle은 초기화(initialization) 단계에서 프로젝트 루트의 settings 파일을 찾아 `Settings` 객체를 생성함
- 이 객체의 핵심 역할 중 하나는 빌드에 포함될 프로젝트 목록을 선언하는 것
- `Settings` 인터페이스가 제공하는 메서드/속성은 settings 파일 안에서 별도의 delegation 없이 바로 호출 가능
  - 대표적으로 다음이 있음
  - `include()`: 서브프로젝트를 빌드에 포함
  - `includeBuild()`: 별도의 복합 빌드(composite build)를 포함
  - `rootProject.name`: 루트 프로젝트 이름 지정

### 예제로 보는 settings 파일 구성 요소

- 공식 튜토리얼은 아래와 같은 하나의 settings 파일 예시를 통해 각 구성 요소가 어떤 API/DSL에서 온 것인지 설명함

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "authoring-tutorial"

include("app")
include("lib")

includeBuild("gradle/license-plugin")
```

각 줄이 어디서 유래하는지 정리하면 다음과 같다.

- `plugins { }`: `PluginDependenciesSpec` API의 블록
- `id(...)`: `PluginDependenciesSpec` API의 메서드
- `rootProject.name = ...`: `Settings` API의 `getRootProject()`
- `include(...)`: `Settings` API의 메서드
- `includeBuild(...)`: `Settings` API의 메서드

Settings 스크립트는 모든 build 스크립트보다 먼저 평가된다. 그래서 플러그인 관리, 프로젝트 구조, 외부 빌드와의 관계처럼 빌드 전역에 영향을 주는 설정이 이 파일에 모인다. 위 예제에서도 `include()`는 같은 빌드의 서브프로젝트를 추가하고, `includeBuild()`는 별도의 독립된 빌드를 포함한다.

## Part 5. Build 스크립트 작성하기 (Writing a Build Script)

> **원문:** https://docs.gradle.org/current/userguide/part5_build_scripts.html

### 개요

- Intermediate Tutorial의 5번째 파트로, `build.gradle(.kts)` 파일을 실제로 뜯어보면서 각 블록이 어떤 API에서 왔는지, 그리고 커스텀 플러그인을 만들 때 build 스크립트를 어떻게 고쳐 나가는지를 다룸
  - Part 1(프로젝트 초기화), Part 2(빌드 라이프사이클), Part 3(서브프로젝트 구성), Part 4(settings 파일)를 이해했다는 전제 위에서 진행되며, 예제는 Gradle plugin 프로젝트(`java-gradle-plugin` 적용)를 기준으로 함

### Project 객체와 build 스크립트의 관계

- 구성(configuration) 단계에서 Gradle은 루트와 서브프로젝트 디렉터리에서 build 스크립트를 찾고, 이를 발견하면 `Project` 객체를 하나 만들어 붙임
- `Project` 객체의 역할은 크게 세 가지
  - 태스크(Task) 모음 생성
  - 플러그인 적용
  - 의존성 조회/해석
- `Project` 인터페이스가 제공하는 메서드, 속성은 build 스크립트 안에서 한정자 없이 바로 호출 가능

```kotlin
defaultTasks("some-task")      // Project.defaultTasks()로 위임
reportsDir = file("reports")   // Project.file() + Java 플러그인의 확장 속성
```

- 공식 튜토리얼이 예로 드는 Gradle 플러그인 프로젝트의 build 스크립트는 아래 요소들로 구성됨

```kotlin
plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm") version "2.2.0-RC2"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

gradlePlugin {
    plugins.create("greeting") {
        id = "license.greeting"
        implementationClass = "license.LicensePlugin"
    }
}
```

각 블록이 어디서 온 것인지 정리하면 다음과 같다.

- `plugins { }`: 코어 플러그인(`java-gradle-plugin`)과 언어 플러그인(Kotlin/Groovy) 적용
  - 출처: `PluginDependenciesSpec`
- `repositories { }`: 의존성을 받아올 저장소 지정(`mavenCentral()`)
  - 출처: `Project.repositories()`
- `dependencies { }`: 컴파일/런타임에 필요한 라이브러리 선언
  - 출처: `Project.dependencies()`
- `gradlePlugin { }`: 개발 중인 플러그인의 id, 구현 클래스 정의
  - 출처: Java Gradle Plugin 플러그인이 추가한 확장
- `tasks.register(...)` / `tasks.named(...)`: 새 태스크 등록 / 기존 태스크 설정
  - 출처: `TaskContainer`

- `plugins { }`는 코어 플러그인(버전 불필요)과 커뮤니티 플러그인(버전 필요)을 함께 선언 가능
- `dependencies { }`에서 `implementation(...)`으로 선언한 의존성은 컨피규레이션(configuration)이라는 스코프에 귀속되며, 컴파일 타임/런타임/둘 다 중 어디에 필요한지를 결정함
  - `implementation`은 보통 런타임 클래스패스에만 필요한 경우에 씀
- `gradlePlugin { }` 같은 블록은 "dependency configuration"과는 별개의 개념으로, 적용된 플러그인을 설정(configure)하는 블록임
  - `java-gradle-plugin`을 적용하면 개발 중인 플러그인의 정보를 반드시 `gradlePlugin { }`으로 채워야 함
- 태스크는 `tasks.register()`로 새로 만들거나 `tasks.named()`로 이미 존재하는 태스크(예: 플러그인이 만들어 둔 `test`)를 찾아 설정함

### 실습: greeting 플러그인을 license 플러그인으로 바꾸기

- 튜토리얼은 `gradle init`이 생성한 예제 플러그인(`greeting`)을 개조해서, 소스 파일에 라이선스 헤더를 자동으로 붙이는 `license` 플러그인으로 만들어 가는 과정을 보여줌

- 1\. build 스크립트 수정: `gradlePlugin { }` 블록의 플러그인 이름, id를 변경

```kotlin
gradlePlugin {
    plugins.create("license") {         // 이름을 license로 변경
        id = "com.tutorial.license"     // id를 com.tutorial.license로 변경
        implementationClass = "license.LicensePlugin"
    }
}
```

- 2\. 서브프로젝트에 플러그인 적용: `app` 서브프로젝트의 build 스크립트에서 `id(...)`로 방금 정의한 플러그인을 적용

```kotlin
plugins {
    application
    id("com.tutorial.license")
}
```

- 3\. 플러그인이 노출하는 태스크 확인: `LicensePlugin`은 아직 실제 라이선스 로직 없이, `apply()` 안에서 `greeting`이라는 태스크 하나만 등록

```kotlin
class LicensePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("greeting") {
            doLast { println("Hello from plugin 'com.tutorial.greeting'") }
        }
    }
}
```

- 4\. 태스크 목록/실행으로 검증: `./gradlew tasks --all`을 실행하면 `app` 프로젝트 아래 `app:greeting` 태스크가 새로 보이고, `./gradlew :app:greeting`으로 실행하면 `Hello from plugin 'com.tutorial.greeting'`이 출력됨

`application`, `gradlePlugin { }` 같은 블록과 속성은 해당 플러그인이 `Project`에 확장(extension)을 추가해야 사용할 수 있다. 낯선 블록을 만나면 어떤 플러그인이 제공하는지 확인하면 된다. 이 예제에서 `app`에만 `license`를 적용한 것처럼 서브프로젝트마다 플러그인을 다르게 적용할 수 있으므로, 같은 저장소에서도 사용 가능한 블록과 태스크가 달라진다.

## Gradle 중급 튜토리얼 6편: 태스크 작성하기

> **원문:** https://docs.gradle.org/current/userguide/part6_writing_tasks.html

### 개요

- 1~5편에서 Java 앱 초기화, 빌드 생명주기, 멀티 프로젝트 구성, 설정/빌드 파일 구조를 다뤘다면, 이번 6편은 그 위에서 실제로 "내 손으로 태스크를 만드는" 단계다
  - 목표는 두 가지다

- 빌드 스크립트 안에서 간단한 태스크를 등록하고, 이미 등록된 태스크를 다시 손보는 방법을 익힌다
- `DefaultTask`를 상속한 커스텀 태스크 클래스를 직접 작성해, 재사용 가능한 로직을 태스크로 캡슐화함

### 태스크의 정체: 액션들의 묶음

- Gradle 문서는 태스크를 "일련의 액션(action)을 담은, 실행 가능한 코드 조각"이라고 정의함
  - 하나의 태스크는 다음 요소로 구성됨

- `doFirst { }` / `doLast { }`로 추가하는 실행 액션들
- `dependsOn(...)`으로 표현하는 다른 태스크와의 선후 관계
- (커스텀 타입이라면) 입력, 출력을 나타내는 프로퍼티

### register()로 새 태스크 만들기, named()로 기존 태스크 손보기

- 두 메서드는 이름이 비슷해 보이지만 역할이 다름

- `tasks.register("이름") { ... }`: 아직 존재하지 않는 태스크를 새로 등록함
- `tasks.named("이름") { ... }`: 이미 등록된 태스크를 찾아 설정 블록을 추가함
  - 존재하지 않는 이름을 넘기면 오류 발생

```kotlin
tasks.register("task1") {
    println("설정 단계에서 실행됨")
}

tasks.named("task1") {
    doFirst { println("실행 단계 시작 시 실행됨") }
    doLast { println("실행 단계 끝에 실행됨") }
}
```

- 여기서 눈에 띄는 점은, `register`/`named` 블록 안에 바로 쓴 `println`은 구성 단계(configuration phase)에서 즉시 찍히고, `doFirst`/`doLast` 안에 넣은 코드만 실행 단계(execution phase)로 미뤄진다는 것임
  - 이 구분을 헷갈리면 "왜 이 로그가 태스크를 실행하지도 않았는데 찍히지" 하는 혼란을 겪기 쉬움

### 커스텀 태스크 작성하기: LicenseTask 예제

- 인라인 등록만으로는 로직을 여러 곳에서 재사용하기 어려움
  - 문서는 `DefaultTask`를 상속하는 `LicenseTask` 클래스를 예로 들어, 소스 파일마다 라이선스 헤더를 붙여주는 태스크를 직접 작성함

```kotlin
abstract class LicenseTask : DefaultTask() {
    @Input
    val licenseFilePath = project.layout.settingsDirectory
        .file("license.txt").asFile.path

    @TaskAction
    fun action() {
        val licenseText = File(licenseFilePath).readText()
        // .java 파일을 찾아 licenseText를 파일 맨 앞에 덧붙인다
    }
}
```

- 구조를 뜯어보면 지금까지 배운 규칙이 그대로 재확인됨

- 실제 동작은 `@TaskAction`이 붙은 메서드 안에만 있으며, 이 메서드가 실행 단계에서 호출됨
- 라이선스 파일 경로는 `@Input`으로 선언된 프로퍼티이며, 이 값의 변경 여부가 재실행을 결정하는 기준이 됨
- 클래스로 뽑아뒀기 때문에 `tasks.register<LicenseTask>("addLicense")`처럼 여러 프로젝트, 여러 빌드에서 같은 로직을 그대로 재사용할 수 있음

### @Input이 하는 일: 다시 실행할지 판단하는 근거

- 문서는 `@Input` 애너테이션의 역할을 다음과 같이 요약함
  - "Gradle은 `@Input`으로 선언된 값을 보고 태스크를 다시 실행해야 하는지 판단한다. 이전에 실행된 적이 없거나, 이전 실행 이후로 입력 값이 바뀌었다면 Gradle은 태스크를 실행한다."

- 즉, 커스텀 태스크를 쓸모 있게 만드는 것은 `@TaskAction` 메서드의 로직이 아니라, 그 로직이 의존하는 값을 `@Input`(또는 `@InputFile`, `@OutputFile` 등)으로 빠짐없이 선언하는 작업임
  - 이 선언이 빠지면 Gradle은 "무엇이 바뀌었는지" 판단할 근거가 없어, 매번 무조건 태스크를 다시 돌리게 됨

## Gradle 중급 튜토리얼 7편: 플러그인 작성하기

> **원문:** https://docs.gradle.org/current/userguide/part7_writing_plugins.html

### 개요

6편에서 만든 `LicenseTask`를 빌드에 사용하려면 해당 클래스를 태스크로 등록해야 한다. 이 등록 과정을 플러그인으로 묶으면 필요한 프로젝트에 같은 기능을 적용할 수 있다. 중급 튜토리얼의 마지막인 7편에서는 `LicenseTask`를 `LicensePlugin`에 연결하고 서브프로젝트에 적용해 실행한다.

### 선행 조건

- 1~6편에서 다룬 내용이 이번 편의 전제가 됨

- 1편: Java 앱 초기화
- 2편: 빌드 생명주기
- 3편: 서브프로젝트와 별도 빌드 추가
- 4편: settings 파일
- 5편: build 스크립트
- 6편: `LicenseTask` 같은 커스텀 태스크 작성

### 태스크와 플러그인의 연결: `apply()` 안에서 태스크 등록

- 플러그인 클래스는 `Plugin<Project>` 인터페이스를 구현하고, `apply(project: Project)` 메서드 안에서 `project.tasks.register<T>(...)`를 호출해 태스크를 등록함
  - 이 시점에 `description`과 `group`을 지정해두면, 나중에 `./gradlew tasks`로 태스크 목록을 훑어볼 때 이 태스크가 어떤 플러그인에서 왔는지, 무슨 일을 하는지 바로 파악 가능

```kotlin
class LicensePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<LicenseTask>("license") {
            description = "add a license header to source code"
            group = "from license plugin"
        }
    }
}
```

이 예제에서 플러그인은 이미 만든 `LicenseTask`를 프로젝트의 `license` 태스크로 등록한다. 실행 로직은 태스크 클래스에 두고, 플러그인은 그 태스크를 프로젝트에 연결하는 역할을 맡는다.

### 플러그인이 참조하는 리소스: license.txt

6편의 `LicenseTask`는 `project.layout.settingsDirectory.file("license.txt")` 경로를 `@Input`으로 참조한다. 태스크 클래스와 별도로 이 경로에 데이터 파일을 준비해야 실행할 수 있으므로, 프로젝트 루트에 Apache License 2.0 문구를 담은 `license.txt`를 만든다.

### 서브프로젝트에 플러그인 적용하기

- 플러그인은 `plugins { }` 블록에서 플러그인 ID로 적용함
  - 3편에서 별도 빌드로 분리해둔 `license-plugin`이 `com.tutorial.license`라는 ID로 게시돼 있다고 가정하고, 이를 `app` 서브프로젝트의 build 스크립트에 추가함

```kotlin
// app/build.gradle.kts
plugins {
    application
    id("com.tutorial.license")
}
```

- 적용이 제대로 됐는지는 `./gradlew :app:tasks`로 확인함
  - 출력에 "From license plugin tasks"라는 그룹이 새로 생기고, 그 아래 `license` 태스크와 방금 지정한 설명이 함께 나타난다면 플러그인이 정상적으로 프로젝트에 연결된 것임

### 태스크 실행과 결과 확인

- `./gradlew :app:license`를 실행하면 `LicenseTask`의 `@TaskAction` 메서드가 동작해 `app` 서브프로젝트의 `.java` 소스 파일 맨 앞에 라이선스 헤더가 삽입됨
  - 실행 전후로 같은 소스 파일(`App.java`)을 비교해보면, 헤더 텍스트가 파일 맨 위에 그대로 붙어 있는 것을 확인할 수 있음
  - 빌드 로그에는 플러그인이 속한 `license-plugin` 프로젝트의 컴파일, 태스크들이 대부분 `UP-TO-DATE`로 표시되고, 실제로 새로 실행된 것은 `app:license` 하나뿐이라는 점도 확인 가능. 이전 편들에서 다룬 최신 상태(up-to-date) 검사와 캐싱이 여기서도 그대로 작동하는 모습임

플러그인 개발의 자세한 내용은 다음 단계인 고급 튜토리얼에서 이어서 다룬다.
