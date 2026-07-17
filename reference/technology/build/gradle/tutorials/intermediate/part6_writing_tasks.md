# Gradle 인터미디에이트 튜토리얼 6편: 태스크 작성하기

> **원문:** https://docs.gradle.org/current/userguide/part6_writing_tasks.html

## 개요

1~5편에서 Java 앱 초기화, 빌드 생명주기, 멀티 프로젝트 구성, 설정/빌드 파일 구조를 다뤘다면, 이번 6편은 그 위에서 실제로 "내 손으로 태스크를 만드는" 단계다. 목표는 두 가지다.

- 빌드 스크립트 안에서 간단한 태스크를 등록하고, 이미 등록된 태스크를 다시 손보는 방법을 익힌다.
- `DefaultTask`를 상속한 커스텀 태스크 클래스를 직접 작성해, 재사용 가능한 로직을 태스크로 캡슐화한다.

## 태스크의 정체: 액션들의 묶음

Gradle 문서는 태스크를 "일련의 액션(action)을 담은, 실행 가능한 코드 조각"이라고 정의한다. 하나의 태스크는 다음 요소로 구성된다.

- `doFirst { }` / `doLast { }`로 추가하는 실행 액션들
- `dependsOn(...)`으로 표현하는 다른 태스크와의 선후 관계
- (커스텀 타입이라면) 입력·출력을 나타내는 프로퍼티

## register()로 새 태스크 만들기, named()로 기존 태스크 손보기

두 메서드는 이름이 비슷해 보이지만 역할이 다르다.

- `tasks.register("이름") { ... }` — 아직 존재하지 않는 태스크를 새로 등록한다.
- `tasks.named("이름") { ... }` — 이미 등록된 태스크를 찾아 설정 블록을 덧붙인다. 존재하지 않는 이름을 넘기면 오류가 난다.

```kotlin
tasks.register("task1") {
    println("설정 단계에서 실행됨")
}

tasks.named("task1") {
    doFirst { println("실행 단계 시작 시 실행됨") }
    doLast { println("실행 단계 끝에 실행됨") }
}
```

여기서 눈에 띄는 점은, `register`/`named` 블록 안에 바로 쓴 `println`은 **구성 단계(configuration phase)**에서 즉시 찍히고, `doFirst`/`doLast` 안에 넣은 코드만 **실행 단계(execution phase)**로 미뤄진다는 것이다. 이 구분을 헷갈리면 "왜 이 로그가 태스크를 실행하지도 않았는데 찍히지" 하는 혼란을 겪기 쉽다.

## 커스텀 태스크 작성하기: LicenseTask 예제

인라인 등록만으로는 로직을 여러 곳에서 재사용하기 어렵다. 문서는 `DefaultTask`를 상속하는 `LicenseTask` 클래스를 예로 들어, 소스 파일마다 라이선스 헤더를 붙여주는 태스크를 직접 작성해본다.

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

구조를 뜯어보면 지금까지 배운 규칙이 그대로 재확인된다.

- 실제 동작은 `@TaskAction`이 붙은 메서드 안에만 있다 — 이 메서드가 실행 단계에서 호출되는 "본체"다.
- 라이선스 파일 경로는 `@Input`으로 선언된 프로퍼티다 — 이 값이 바뀌었는지가 재실행 여부를 가르는 기준이 된다.
- 클래스로 뽑아뒀기 때문에 `tasks.register<LicenseTask>("addLicense")`처럼 여러 프로젝트·여러 빌드에서 같은 로직을 그대로 재사용할 수 있다.

## @Input이 하는 일: 다시 실행할지 판단하는 근거

문서는 `@Input` 애너테이션의 역할을 다음과 같이 요약한다. *"Gradle은 `@Input`으로 선언된 값을 보고 태스크를 다시 실행해야 하는지 판단한다. 이전에 실행된 적이 없거나, 이전 실행 이후로 입력 값이 바뀌었다면 Gradle은 태스크를 실행한다."*

즉, 커스텀 태스크를 쓸모 있게 만드는 것은 `@TaskAction` 메서드의 로직이 아니라, 그 로직이 의존하는 값을 `@Input`(또는 `@InputFile`, `@OutputFile` 등)으로 정직하게 선언하는 작업이다. 이 선언이 빠지면 Gradle은 "무엇이 바뀌었는지" 판단할 근거가 없어, 매번 무조건 태스크를 다시 돌리게 된다.

## 핵심 포인트

- `register()`는 태스크를 새로 만들고, `named()`는 이미 등록된 태스크에 설정을 추가한다 — 이름은 비슷하지만 대상 존재 여부가 다르다.
- 등록/설정 블록에 직접 쓴 코드는 구성 단계에서, `doFirst`/`doLast`나 `@TaskAction` 메서드 안 코드는 실행 단계에서 동작한다는 시점 차이를 구분해야 한다.
- 재사용 가능한 태스크가 필요하다면 `DefaultTask`를 상속한 클래스를 만들고, 실행 로직은 `@TaskAction` 메서드에, 재실행 판단 기준이 되는 값은 `@Input` 계열 애너테이션에 담는다.
- 입력을 선언하지 않은 태스크는 Gradle이 변경 여부를 알 수 없어 항상 재실행되므로, 커스텀 태스크를 작성할 때 입력 선언은 선택이 아니라 기본 규칙에 가깝다.

## 다음 편 예고

7편에서는 지금까지 빌드 스크립트 안에 직접 써온 태스크·설정 로직을 플러그인(plugin)으로 뽑아내는 방법을 다룬다.
