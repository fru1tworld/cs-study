# 태스크 생성과 등록 (Creating and Registering Tasks)

> **원문:** https://docs.gradle.org/current/userguide/writing_tasks_intermediate.html

## 개요

Gradle에서 태스크를 만드는 방법은 크게 두 갈래로 나뉩니다. 하나는 즉석에서 동작을 정의하는 **인라인 등록**이고, 다른 하나는 재사용 가능한 **커스텀 태스크 타입(클래스)**을 만들어 등록하는 방식입니다. 어떤 방식을 쓰든 태스크는 "무슨 타입인가"와 "무엇을 실행하는가"라는 두 가지 요소로 구성되며, 여기에 입력·출력을 선언해두면 Gradle이 변경 여부를 추적해 불필요한 재실행을 건너뛸 수 있습니다.

## 태스크를 등록하는 두 가지 방식

- **내장 타입 활용**: `Copy`, `Delete`, `Zip`, `Jar`처럼 Gradle이나 플러그인이 미리 제공하는 타입을 그대로 등록해 사용합니다.

  ```kotlin
  tasks.register<Delete>("removeOutput") {
      delete(layout.buildDirectory.file("outputs/1.txt"))
  }
  ```

- **인라인 등록**: 별도 타입 없이 `tasks.register("이름") { ... }` 블록 안에 `doLast` 등으로 동작을 바로 기술합니다. 간단한 스크립트용 태스크에 적합하지만 재사용이 어렵습니다.

  ```kotlin
  tasks.register("hello") {
      doLast { println("Hello world!") }
  }
  ```

- **커스텀 태스크 타입**: `DefaultTask`를 상속한 클래스를 정의하고, 실행할 메서드에 `@TaskAction`을 붙입니다. 로직이 클래스에 담기므로 여러 프로젝트·빌드에서 같은 태스크를 재사용할 수 있습니다.

  ```kotlin
  abstract class HelloTask : DefaultTask() {
      @TaskAction
      fun hello() { println("hello from HelloTask") }
  }

  tasks.register<HelloTask>("hello")
  ```

## 태스크 액션: doFirst / doLast

태스크가 실제로 수행하는 동작은 **실행 단계(execution phase)**에서 순서대로 실행되는 "액션(action)"들의 목록입니다.

- `doFirst { ... }`: 기존 액션들 앞에 새 액션을 끼워 넣습니다.
- `doLast { ... }`: 기존 액션들 뒤에 새 액션을 추가합니다.
- 같은 태스크에 여러 번 `doFirst`/`doLast`를 호출하면 호출 순서대로 액션이 누적되어, 하나의 태스크가 여러 단계를 순차적으로 실행하게 됩니다.

```kotlin
tasks.register("hello") {
    doFirst { println("Hello Venus") }
    doLast { println("Hello Earth") }
}
```

## 입력과 출력 선언하기

커스텀 태스크 타입에서는 `@Input`, `@InputFile`, `@InputDirectory`, `@OutputFile`, `@OutputDirectory` 같은 애너테이션으로 태스크가 무엇을 읽고 무엇을 만들어내는지 명시할 수 있습니다.

```kotlin
abstract class CreateAFileTask : DefaultTask() {
    @get:Input
    abstract val fileText: Property<String>

    @OutputFile
    val myFile: File = File("myfile.txt")

    @TaskAction
    fun action() {
        myFile.writeText(fileText.get())
    }
}
```

- 이 선언이 있어야 Gradle이 "입력이 그대로이고 출력도 이미 존재하면 재실행하지 않는다"는 **최신 상태(up-to-date) 판단**을 할 수 있습니다.
- 입출력을 선언하지 않은 태스크는 매번 무조건 재실행되므로, 빌드 성능 측면에서 손해가 큽니다.

## 그룹과 설명 부여하기

- `group`: `./gradlew tasks` 실행 시 태스크가 어떤 카테고리 아래 표시될지 결정합니다.
- `description`: 해당 태스크가 무슨 일을 하는지 한 줄로 설명하는 문서 역할을 합니다.

```kotlin
tasks.register<HelloTask>("hello") {
    group = "Custom tasks"
    description = "A lovely greeting task."
}
```

두 속성 모두 빌드 동작에는 영향을 주지 않지만, 협업하는 다른 개발자가 `tasks` 목록만 보고도 태스크 용도를 파악할 수 있게 해주는 최소한의 문서화 장치입니다.

## 태스크 의존성: dependsOn

`dependsOn()`으로 "이 태스크가 실행되기 전에 반드시 저 태스크가 끝나야 한다"는 관계를 선언합니다.

```kotlin
tasks.register("intro") {
    dependsOn("hello")
    doLast { println("I'm Gradle") }
}

tasks.register("hello") {
    doLast { println("Hello world!") }
}
```

- 의존 대상 태스크가 아직 등록되지 않은 시점이라도 이름(문자열)만으로 `dependsOn`을 선언할 수 있습니다. Gradle은 태스크 그래프를 구성하는 시점에 실제 태스크를 찾아 연결합니다.
- 이 방식은 `task_basics`에서 다룬, `build`가 `compileJava`·`test`·`jar`에 의존하는 것과 동일한 메커니즘입니다.

## 이미 등록된 태스크 수정하기: tasks.named()

한 번 등록한 태스크에 나중에 추가 설정을 덧붙이고 싶다면 `tasks.register()`를 다시 호출하는 게 아니라 `tasks.named()`로 접근합니다.

```kotlin
tasks.named("hello") {
    doFirst { println("Hello Venus") }
}
tasks.named("hello") {
    doLast { println("Hello Jupiter") }
}
```

- `named()`도 `register()`처럼 지연(lazy) 방식으로 동작해, 실제로 태스크가 필요해지는 시점까지 설정 블록 실행을 미룹니다.
- 플러그인이 만들어둔 태스크를 빌드 스크립트에서 커스터마이징할 때 자주 쓰는 패턴입니다.

## 태스크의 두 가지 성격: 액션형 vs 라이프사이클형

- **액션형 태스크(actionable task)**: 실제로 수행할 액션이 붙어 있는 태스크. 예) `compileJava`.
- **라이프사이클 태스크(lifecycle task)**: 자신은 아무 액션도 갖지 않고, 여러 하위 태스크를 `dependsOn`으로 묶어 "묶음 실행 지점" 역할만 하는 태스크. 예) `build`, `assemble`, `check`.

이 구분을 알면 `build`를 실행했을 때 왜 로그에 별다른 출력 없이 하위 태스크들만 줄줄이 실행되는지 이해할 수 있습니다.

## 태스크 작성 시 성능 체크리스트

- **`register()`를 기본으로 사용한다**: `create()`는 즉시 태스크를 설정하지만 `register()`는 실제로 필요해질 때까지 설정을 지연시켜, 사용하지 않는 태스크의 구성 비용을 아낀다.
- **입력/출력을 반드시 선언한다**: 그래야 Gradle이 최신 상태 검사를 수행해 불필요한 재실행을 건너뛴다.
- **실행 로직은 `@TaskAction`/`doLast` 안에만 둔다**: 설정 블록(구성 단계)에서 무거운 작업을 수행하면 태스크를 실행하지 않아도 매 빌드마다 그 비용이 발생한다.
- **`group`과 `description`을 채운다**: 태스크 탐색성과 협업 편의성을 높이는 최소한의 관행이다.

## 핵심 포인트

- 태스크는 "타입(무엇으로 만들어졌는가)"과 "액션(무엇을 하는가)"의 조합이며, 내장 타입·인라인 등록·커스텀 타입 세 가지 방식으로 만들 수 있다.
- `@Input`/`@OutputFile` 등으로 입출력을 선언해야 Gradle의 최신 상태 검사와 증분 빌드가 제대로 동작한다.
- `doFirst`/`doLast`는 액션을 앞뒤로 누적하고, `dependsOn`은 태스크 간 실행 순서를, `tasks.named()`는 이미 등록된 태스크에 대한 후속 설정을 담당한다.
- `register()` 위주의 지연 구성과 명확한 입출력 선언은 대규모 빌드에서 불필요한 구성·실행 비용을 줄이는 핵심 습관이다.
