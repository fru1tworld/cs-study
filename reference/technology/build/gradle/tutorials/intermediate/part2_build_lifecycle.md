# Gradle 중급 튜토리얼 2편: 빌드 생명주기 이해하기

> **원문:** https://docs.gradle.org/current/userguide/part2_build_lifecycle.html

## 개요

1편에서 초기화한 Java 프로젝트를 그대로 이용해, `./gradlew task1` 한 번을 실행했을 때 Gradle 내부에서 초기화·구성·실행이라는 세 단계가 실제로 어떤 순서와 시점에 동작하는지 로그로 직접 확인해 보는 실습 편입니다.

## 사전 준비물

- 1편에서 만든 `authoring-tutorial` 프로젝트(`gradle init --type java-application`)가 그대로 있어야 함

## 세 단계 복습

- **초기화(Initialization)**: 이 빌드에 어떤 프로젝트가 참여하는지 결정하고, 프로젝트마다 `Project` 인스턴스를 생성
- **구성(Configuration)**: 참여하는 모든 프로젝트의 빌드 스크립트를 평가해 `Project` 객체를 채우고, 실행할 태스크 집합을 결정
- **실행(Execution)**: 앞서 결정된 태스크들을 실제로 실행

## 실습 1: settings 파일에 로그 찍기

`settings.gradle(.kts)` 맨 위에 `println`을 한 줄 추가합니다.

```kotlin
// settings.gradle.kts
println("SETTINGS FILE: This is executed during the initialization phase")
```

- 이 파일은 항상 **초기화 단계**에서 평가되므로, 여기 적힌 코드는 다른 무엇보다 먼저 출력됨

## 실습 2: 빌드 스크립트에 태스크 두 개 등록하기

`app/build.gradle.kts` 맨 아래에 `task1`, `task2`를 각각 `register`로 선언하고 `named`로 다시 참조해 `doFirst`/`doLast`를 붙입니다.

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

- `register { ... }`, `named { ... }` 블록 **본문 자체**는 구성 단계에서 즉시 실행됨
- `doFirst`/`doLast`로 감싼 코드만 실행 단계로 지연됨

## 실습 3: `task1` 실행 결과로 확인하는 생명주기

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

| 구간 | 단계 | 이 시점에 Gradle이 하는 일 |
|---|---|---|
| ① | 초기화 | `settings.gradle(.kts)`를 실행해 참여 프로젝트를 정하고 `Project` 객체를 만듦 |
| ② | 구성 | `build.gradle(.kts)`를 실행해 각 프로젝트를 채우고, 의존성을 해석하며 태스크 그래프를 만듦 |
| ③ | 실행 | 커맨드라인에서 요청한 태스크와 그 선행 태스크를 실제로 실행 |

## 핵심 포인트: 태스크 구성 회피(Task Configuration Avoidance)

- 위 로그를 보면 `task1`의 등록·명명 블록만 출력되고, `task2`와 관련된 `REGISTER TASK2`/`NAMED TASK2` 로그는 전혀 나타나지 않음
- `task1`이 `task2`에 의존하지 않으므로, Gradle은 커맨드라인에서 요청받지 않은 `task2`를 애초에 구성하지 않고 건너뜀
- 이렇게 필요한 태스크만 골라 구성하는 최적화를 **태스크 구성 회피**라고 부르며, 태스크 수가 많은 대형 멀티 프로젝트일수록 구성 단계 시간을 크게 줄여줌

## 정리

- 빌드 스크립트의 코드는 크게 "구성 단계에서 즉시 도는 코드"와 "`doFirst`/`doLast`로 감싸 실행 단계로 미룬 코드"로 나뉜다는 점을 로그로 직접 확인함
- Gradle은 실행 대상으로 지정된 태스크와 그 의존 태스크만 구성하며, 나머지는 구성 자체를 건너뛴다(태스크 구성 회피)
- 이 실습에서 살펴본 초기화 → 구성 → 실행 흐름은 이후 멀티 프로젝트 빌드, 커스텀 태스크 작성에서도 그대로 적용되는 기본 골격

## 다음 단계

다음 편에서는 하나의 빌드에 여러 서브프로젝트를 두는 "멀티 프로젝트 빌드(Multi-Project Builds)"를 다루며, 이번 편에서 익힌 초기화·구성·실행 단계가 서브프로젝트가 여러 개일 때 어떻게 확장되는지 살펴봅니다.
