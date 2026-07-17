# Gradle 중급 튜토리얼 1편: 프로젝트 초기화와 애플리케이션 실행

> **원문:** https://docs.gradle.org/current/userguide/part1_gradle_init_project.html

## 개요

초급 튜토리얼에서 다룬 `gradle init`과 디렉터리 구조를 한 단계 더 깊이 파고들어, 생성된 코드를 실제로 실행·테스트·패키징까지 해보는 편입니다. 이 문서를 마치면 다음을 할 수 있게 됩니다.

- Java 애플리케이션 프로젝트를 초기화
- 생성된 디렉터리 구조와 소스 코드를 읽어내기
- `./gradlew run`으로 애플리케이션을 실행
- `--scan` 옵션으로 Build Scan을 생성해 빌드 내역을 시각적으로 확인
- `./gradlew build`로 실행 가능한 배포 아카이브(zip/tar)를 만들기

## 사전 준비물

- Gradle이 로컬에 설치되어 있어야 함
- (선택) IntelliJ IDEA Community Edition — 코드 탐색용

## 1) 프로젝트 초기화

```bash
$ mkdir authoring-tutorial
$ cd authoring-tutorial
$ gradle init --type java-application --dsl kotlin
```

- `--dsl kotlin` 대신 `--dsl groovy`를 쓰면 Groovy DSL로 생성됨
- 이어지는 대화형 질문은 기본값을 그대로 선택해도 무방

## 2) 생성된 디렉터리 구조

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

- 초급 편에서 본 구조와 동일하지만, 이번에는 `src/main`·`src/test` 밑에 실제로 생성되는 자바 소스와 테스트 코드까지 확인
- `GRADLE_USER_HOME`(기본값 `~/.gradle`)은 프로젝트 밖에 위치하는 전역 저장소로, Gradle 배포판 캐시·다운로드한 의존성·로그 등을 담아 여러 프로젝트가 공유

## 3) 설정 파일 다시 보기

`settings.gradle.kts`는 빌드 이름과 서브프로젝트 목록만 선언하는 최소 구성입니다.

```kotlin
rootProject.name = "authoring-tutorial"
include("app")
```

`app/build.gradle.kts`는 실제 빌드 동작을 정의합니다.

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

## 4) 생성된 소스 코드 훑어보기

- `App.java`: `getGreeting()`이 문자열을 반환하고 `main()`이 이를 출력하는 최소 예제
- `AppTest.java`: JUnit Jupiter의 `@Test`로 `getGreeting()`이 `null`이 아님을 검증하는 단위 테스트 1개
- 별도로 손대지 않아도 곧바로 실행·테스트가 가능한 상태로 스캐폴딩됨

## 5) 애플리케이션 실행하기

```bash
$ ./gradlew run
```

`application` 플러그인이 추가한 `run` 태스크가 `application.mainClass`로 지정된 클래스의 `main()`을 호출합니다. 콘솔에는 실행된 태스크 목록과 함께 `Hello World!` 같은 출력, 이어서 `BUILD SUCCESSFUL` 메시지가 나타납니다.

## 핵심 포인트: `build`가 실제로 하는 일

- `./gradlew build`를 실행하면 컴파일 → 테스트 → 아카이브 생성까지 한 번에 이어지는 태스크 체인이 동작합니다.
- 대표적으로 `compileJava → classes → jar → startScripts → distZip/distTar → assemble`, `compileTestJava → test → check` 흐름을 거쳐 최종적으로 `build`가 완료됩니다.
- 즉 `build`는 단일 동작이 아니라, 의존 관계로 연결된 여러 태스크가 순서대로 실행된 결과입니다.

## 6) 배포 아카이브 만들기

```bash
$ ./gradlew build
```

빌드가 끝나면 `app/build/distributions/` 아래에 `app.zip`, `app.tar` 두 종류의 아카이브가 생성됩니다. 각 아카이브에는 애플리케이션 실행 스크립트와 필요한 의존성 JAR이 함께 담겨 있어, 압축을 풀기만 하면 다른 환경에서도 바로 실행할 수 있습니다.

## 7) Build Scan으로 빌드 들여다보기

```bash
$ ./gradlew build --scan
```

- 최초 실행 시 Gradle 이용 약관 동의를 물어보며, 동의하면 `scans.gradle.com`에 업로드된 리포트 링크가 출력됩니다.
- 리포트에는 실행된 태스크 목록, 다운로드된 의존성, 각 단계별 소요 시간 등 빌드 전반의 성능·구성 정보가 시각화되어 있어, 로컬 로그만으로 파악하기 어려운 병목이나 캐시 적중 여부를 확인하는 데 유용합니다.

## 다음 단계

이 편에서는 초기화된 프로젝트를 실제로 실행·테스트·패키징하고 Build Scan으로 결과를 들여다보는 과정을 다뤘습니다. 다음 편에서는 "빌드 라이프사이클(The Build Lifecycle)"을 통해 `build` 같은 명령이 내부적으로 어떤 단계(초기화·설정·실행)를 거치는지 살펴봅니다.
