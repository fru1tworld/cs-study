# 빌드 로직 작성: 라이프사이클, 스크립트, 태스크

## Gradle 빌드 생명주기 (Gradle Build Lifecycle)

> 원문: https://docs.gradle.org/current/userguide/build_lifecycle_intermediate.html

### 개요

빌드 생명주기란 Gradle이 빌드 스크립트와 소스 코드를 실제 결과물로 바꾸기 위해 거치는 일련의 단계. 빌드 환경 초기화 → 프로젝트 설정 → 태스크 실행 순서로 진행되며, 이 세 단계는 항상 고정된 순서로만 실행됨.

### 세 가지 빌드 단계

Gradle 빌드는 언제나 아래 세 단계를 순서대로 거침.

#### 1단계: 초기화(Initialization)

- `settings.gradle(.kts)` 설정 파일을 찾아 `Settings` 인스턴스 생성
- 설정 파일을 평가해서 이 빌드에 어떤 프로젝트와 컴포짓 빌드(included build)가 포함되는지 결정
- 발견된 프로젝트마다 `Project` 인스턴스 생성

#### 2단계: 구성(Configuration)

- 빌드에 참여하는 모든 프로젝트의 빌드 스크립트 평가
- 각 태스크의 입력·출력 등 설정 정보 처리
- 요청된 태스크를 기준으로 태스크 그래프 생성

#### 3단계: 실행(Execution)

- 구성 단계에서 만들어진 태스크 그래프를 바탕으로 선택된 태스크만 스케줄링해서 실행
- 필요한 경우 여러 태스크를 병렬로 실행 가능

### 핵심 포인트: 어떤 코드가 언제 실행되는가

빌드 스크립트에 적힌 코드가 정확히 어느 단계에서 실행되는지는 흔히 헷갈리는 부분. 다음 예시로 구분 가능.

```kotlin
// 구성 단계에서 실행
println("configuration phase")

tasks.register("configured") {
    // 이 블록 본문도 구성 단계에서 실행
    println("still configuration phase")
}

tasks.register("test") {
    doLast {
        // doFirst/doLast 블록 내부만 실행 단계에서 실행
        println("execution phase")
    }
}
```

- 태스크 등록 블록(`tasks.register { ... }`) 본문 자체는 구성 단계에서 실행됨
- `doFirst`/`doLast`로 감싼 코드만 실행 단계로 미뤄짐
- Gradle은 실행하도록 요청받은 태스크와 그 의존 태스크만 구성함 → `./gradlew test`를 실행하면 `test`와 관련 없는 다른 태스크(`configured` 등)는 구성조차 되지 않음 → 대규모 멀티 프로젝트 빌드에서도 필요 없는 부분을 건너뛰어 구성 시간 절약 가능

### 태스크 그래프(Task Graph)

- 빌드 스크립트와 플러그인은 태스크 사이의 의존 관계를 선언함. 예를 들어 `assembleDocs`가 `buildHtml`에, `createDocs`가 `assembleDocs`에 의존한다고 선언하면 Gradle은 `buildHtml → assembleDocs → createDocs` 순서의 그래프를 구성함
- 의존 관계는 `dependsOn`처럼 명시적으로 선언할 수도 있고, 태스크의 입력·출력을 연결하는 방식으로 암묵적으로 선언할 수도 있음
- Gradle은 태스크를 실행하기 전에 반드시 전체 그래프를 먼저 구성함 → 구성 단계가 끝나야 실행 단계로 넘어갈 수 있음
- 빌드에 참여하는 모든 프로젝트의 태스크를 통틀어 하나의 방향성 비순환 그래프(DAG, Directed Acyclic Graph)를 이룸. 순환 의존이 있으면 그래프를 만들 수 없어 빌드 실패

### 핵심 포인트: 생명주기 후킹

- 빌드 로직이 복잡해지면 특정 단계가 시작되거나 끝나는 시점에 커스텀 동작을 끼워 넣고 싶을 때가 있음
- Gradle은 이런 요구를 위해 초기화·구성·실행 각 단계의 주요 이벤트를 감지할 수 있는 리스너·콜백 API를 제공함
- 구체적인 후킹 지점과 사용법은 별도의 "Lifecycle API" 문서에서 다루므로, 이 문서는 세 단계의 흐름과 태스크 그래프 개념에 집중

### 정리

- Gradle 빌드는 초기화 → 구성 → 실행이라는 고정된 순서를 따르며, 각 단계의 역할이 명확히 분리됨
- 빌드 스크립트 코드가 구성 단계에서 도는지 실행 단계에서 도는지 구분하는 것이 태스크 작성의 기본
- 태스크 그래프는 실행 전에 완성되는 DAG이며, 요청된 태스크와 그 의존 관계만 구성 대상에 포함됨

---

## 빌드 스크립트 작성하기: 중급 (Writing Build Scripts)

> 원문: https://docs.gradle.org/current/userguide/writing_build_scripts_intermediate.html

### 개요

빌드 스크립트(`build.gradle` / `build.gradle.kts`)는 사실 일반적인 스크립트 파일이 아니라, Gradle이 내부적으로 특정 API 객체(주로 `Project`)를 대상으로 실행하는 설정 코드. 이 문서는 빌드 스크립트가 "왜 이렇게 짧게 쓰이는지", "블록 안에서 이름만 적으면 왜 동작하는지"를 이해하기 위한 중급 개념을 다룸.

### 블록과 위임(Delegate) / 수신자(Receiver)

- 빌드 스크립트는 크게 구문(statement)과 블록(block) 두 가지로 구성됨. `plugins {}`, `repositories {}`, `dependencies {}` 같은 블록은 Groovy의 클로저 또는 Kotlin의 람다
- 블록 내부의 메서드 호출은 실제로는 특정 대상 객체에게 전달됨
  - Groovy: 블록의 delegate가 호출을 받음
  - Kotlin: 블록의 receiver(즉 `this`)가 호출을 받음
- 예를 들어 `dependencies { implementation(...) }`에서 `implementation(...)`은 실제로는 `DependencyHandler`에게 위임되는 호출. 필요하다면 `project.dependencies.implementation(...)`처럼 완전한 경로로도 사용 가능

```kotlin
repositories {
    mavenCentral() // 이 호출은 RepositoryHandler에게 위임됨
}
```

### 핵심 포인트: 짧게 쓸 수 있는 이유

- 블록 안에서 이름만 적어도 동작하는 이유는 "생략"이 아니라 "위임/수신자 지정"임. Gradle DSL은 각 블록마다 어떤 객체가 메서드 호출을 받을지 미리 정해 놓음
- 이 구조를 알면, 알 수 없는 블록을 만났을 때 "이 블록의 delegate/receiver가 어떤 타입인가"를 먼저 확인하는 습관 형성

### 변수: 로컬 변수 vs Extra Property

빌드 스크립트에서 값을 저장하는 방법은 두 가지로 구분됨.

- 로컬 변수(local variable): `val`(Kotlin), `def`(Groovy)로 선언하며, 선언된 스코프 안에서만 보임. 각 언어의 일반적인 변수와 동일

```kotlin
val dest = "dest"
tasks.register<Copy>("copy") {
    from("source")
    into(dest)
}
```

- Extra Property(확장 속성): `project`처럼 확장 가능한 객체에 사용자 정의 값을 붙이는 방식. Kotlin은 `extra["key"]`, Groovy는 `ext { }` 블록을 사용

```groovy
ext {
    springVersion = "3.1.0.RELEASE"
    emailNotification = "build@master.org"
}
```

### 핵심 포인트: 두 변수의 차이

- 로컬 변수는 언어 차원의 스코프 규칙을 따르므로 해당 블록/스크립트 밖에서는 존재하지 않음
- Extra Property는 소유 객체(`project` 등)에 붙기 때문에, 서브프로젝트에서 부모 프로젝트의 확장 속성을 읽어오는 등 스코프를 넘는 값 공유 가능. 값을 여러 태스크나 스크립트 조각에서 재사용하고 싶다면 Extra Property가 적합

### 실행 순서: 위에서 아래로

- Gradle은 빌드 스크립트를 위에서 아래로 순차 평가함. 설정 블록 바깥의 코드는 즉시(eager) 실행됨
- 즉 변수를 태스크 설정 블록보다 먼저 선언해야 하며, 선언 순서가 실제 동작에 영향을 줌
- "지금 당장" 실행되는 것이 아니라 "나중에, 필요할 때" 평가되어야 하는 값은 `Provider` 같은 지연(lazy) API 사용 필요. 이는 태스크 설정 회피(configuration avoidance)와도 연결되는 개념

### 예제로 보는 구성 요소별 역할

```kotlin
plugins {
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:32.1.1-jre")
}

application {
    mainClass = "com.example.Main"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register<Zip>("zip-reports") {
    from("Reports/")
}
```

- `plugins {}`: `PluginDependenciesSpec`를 통해 플러그인 적용. 플러그인은 기능을 모듈화해서 프로젝트에 주입하는 단위
- `repositories {}`: 의존성을 어디서 내려받을지 지정
- `dependencies {}`: `implementation`, `testImplementation` 같은 컨피규레이션에 라이브러리 등록
- `application {}` 같은 확장(extension) 블록: 플러그인이 `Project`의 `ExtensionContainer`에 추가해 둔 설정 영역. 플러그인을 적용하면 이런 커스텀 설정 블록이 새로 생김
- `tasks.register(...)`: 새 태스크 등록. 즉시 인스턴스를 만드는 옛 방식인 `create()`보다, 필요할 때까지 생성을 미루는 `register()` 권장(태스크 설정 회피)
- `tasks.named(...)`: 이미 존재하는 태스크(예: 플러그인이 만들어 둔 `test` 태스크)를 찾아 추가로 설정

### 핵심 포인트: register vs named

- `register()`는 "새로 만들기", `named()`는 "이미 있는 걸 찾아서 설정하기"
- 둘 다 즉시 태스크 인스턴스를 생성하지 않고 필요한 시점까지 지연시키는 설계로, 대규모 빌드에서 불필요한 태스크 생성/설정 비용 절감

### 프로퍼티 접근: 한정자 없이 쓰는 이유

빌드 스크립트에서 `println(name)`처럼 `project.` 없이 프로퍼티를 쓸 수 있는 것도 위임/수신자 구조 때문.

```kotlin
println(name)            // 축약형
println(project.name)    // 완전한 표기 — 결과는 동일
```

- Groovy는 한정자가 없는 참조를 동적으로 `Project` 객체에 위임함
- Kotlin은 빌드 스크립트 자체를 `Project`의 확장(extension)으로 컴파일하기 때문에 프로퍼티를 직접 참조 가능

세팅 스크립트(`settings.gradle(.kts)`)에서도 동일한 원리가 적용되며, 이 경우 대상 객체는 `Settings`.

```groovy
rootProject.name = "my-awesome-project"
println(name)  // settings 스크립트에서는 보통 rootProject.name을 가리킴
```

### 기본 임포트(Default Imports)

Gradle은 자주 쓰는 클래스들을 자동으로 임포트해 둠. 예를 들어 `org.gradle.api.tasks.StopExecutionException`을 전체 경로로 쓰지 않고 `StopExecutionException`만 써도 됨 → 빌드 스크립트가 장황해지지 않음

### 정리

- 빌드 스크립트가 짧고 선언적으로 보이는 이유는 블록마다 정해진 delegate/receiver에게 호출이 위임되기 때문
- 값 저장은 스코프가 좁은 로컬 변수와, 프로젝트 트리 전체에서 공유 가능한 Extra Property로 구분해서 사용
- 스크립트는 위에서 아래로 즉시 평가되므로 변수·태스크 선언 순서가 실제 동작에 영향을 미치며, 지연 평가가 필요하면 `Provider` API 사용
- `register()`/`named()`로 태스크 설정을 지연시키고, 플러그인이 추가하는 확장 블록(`application {}` 등)으로 세부 설정을 이어감

---

## 태스크 생성과 등록 (Creating and Registering Tasks)

> 원문: https://docs.gradle.org/current/userguide/writing_tasks_intermediate.html

### 개요

Gradle에서 태스크를 만드는 방법은 크게 두 갈래로 나뉨. 하나는 즉석에서 동작을 정의하는 인라인 등록이고, 다른 하나는 재사용 가능한 커스텀 태스크 타입(클래스)을 만들어 등록하는 방식. 어떤 방식을 쓰든 태스크는 "무슨 타입인가"와 "무엇을 실행하는가"라는 두 가지 요소로 구성되며, 여기에 입력·출력을 선언해두면 Gradle이 변경 여부를 추적해 불필요한 재실행을 건너뛸 수 있음.

### 태스크를 등록하는 두 가지 방식

- 내장 타입 활용: `Copy`, `Delete`, `Zip`, `Jar`처럼 Gradle이나 플러그인이 미리 제공하는 타입을 그대로 등록해 사용

  ```kotlin
  tasks.register<Delete>("removeOutput") {
      delete(layout.buildDirectory.file("outputs/1.txt"))
  }
  ```

- 인라인 등록: 별도 타입 없이 `tasks.register("이름") { ... }` 블록 안에 `doLast` 등으로 동작을 바로 기술. 간단한 스크립트용 태스크에 적합하지만 재사용은 어려움

  ```kotlin
  tasks.register("hello") {
      doLast { println("Hello world!") }
  }
  ```

- 커스텀 태스크 타입: `DefaultTask`를 상속한 클래스를 정의하고, 실행할 메서드에 `@TaskAction`을 붙임. 로직이 클래스에 담기므로 여러 프로젝트·빌드에서 같은 태스크 재사용 가능

  ```kotlin
  abstract class HelloTask : DefaultTask() {
      @TaskAction
      fun hello() { println("hello from HelloTask") }
  }

  tasks.register<HelloTask>("hello")
  ```

### 태스크 액션: doFirst / doLast

태스크가 실제로 수행하는 동작은 실행 단계(execution phase)에서 순서대로 실행되는 "액션(action)"들의 목록.

- `doFirst { ... }`: 기존 액션들 앞에 새 액션을 끼워 넣음
- `doLast { ... }`: 기존 액션들 뒤에 새 액션을 추가함
- 같은 태스크에 여러 번 `doFirst`/`doLast`를 호출하면 호출 순서대로 액션이 누적되어, 하나의 태스크가 여러 단계를 순차적으로 실행하게 됨

```kotlin
tasks.register("hello") {
    doFirst { println("Hello Venus") }
    doLast { println("Hello Earth") }
}
```

### 입력과 출력 선언하기

커스텀 태스크 타입에서는 `@Input`, `@InputFile`, `@InputDirectory`, `@OutputFile`, `@OutputDirectory` 같은 애너테이션으로 태스크가 무엇을 읽고 무엇을 만들어내는지 명시 가능.

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

- 이 선언이 있어야 Gradle이 "입력이 그대로이고 출력도 이미 존재하면 재실행하지 않는다"는 최신 상태(up-to-date) 판단 가능
- 입출력을 선언하지 않은 태스크는 매번 무조건 재실행되므로, 빌드 성능 측면에서 손해가 큼

### 그룹과 설명 부여하기

- `group`: `./gradlew tasks` 실행 시 태스크가 어떤 카테고리 아래 표시될지 결정
- `description`: 해당 태스크가 무슨 일을 하는지 한 줄로 설명하는 문서 역할

```kotlin
tasks.register<HelloTask>("hello") {
    group = "Custom tasks"
    description = "A lovely greeting task."
}
```

두 속성 모두 빌드 동작에는 영향을 주지 않지만, 협업하는 다른 개발자가 `tasks` 목록만 보고도 태스크 용도를 파악할 수 있게 해주는 최소한의 문서화 장치.

### 태스크 의존성: dependsOn

`dependsOn()`으로 "이 태스크가 실행되기 전에 반드시 저 태스크가 끝나야 한다"는 관계를 선언.

```kotlin
tasks.register("intro") {
    dependsOn("hello")
    doLast { println("I'm Gradle") }
}

tasks.register("hello") {
    doLast { println("Hello world!") }
}
```

- 의존 대상 태스크가 아직 등록되지 않은 시점이라도 이름(문자열)만으로 `dependsOn` 선언 가능. Gradle은 태스크 그래프를 구성하는 시점에 실제 태스크를 찾아 연결함
- 이 방식은 `task_basics`에서 다룬, `build`가 `compileJava`·`test`·`jar`에 의존하는 것과 동일한 메커니즘

### 이미 등록된 태스크 수정하기: tasks.named()

한 번 등록한 태스크에 나중에 추가 설정을 덧붙이고 싶다면 `tasks.register()`를 다시 호출하는 게 아니라 `tasks.named()`로 접근함.

```kotlin
tasks.named("hello") {
    doFirst { println("Hello Venus") }
}
tasks.named("hello") {
    doLast { println("Hello Jupiter") }
}
```

- `named()`도 `register()`처럼 지연(lazy) 방식으로 동작해, 실제로 태스크가 필요해지는 시점까지 설정 블록 실행을 미룸
- 플러그인이 만들어둔 태스크를 빌드 스크립트에서 커스터마이징할 때 자주 쓰는 패턴

### 태스크의 두 가지 성격: 액션형 vs 라이프사이클형

- 액션형 태스크(actionable task): 실제로 수행할 액션이 붙어 있는 태스크. 예) `compileJava`
- 라이프사이클 태스크(lifecycle task): 자신은 아무 액션도 갖지 않고, 여러 하위 태스크를 `dependsOn`으로 묶어 "묶음 실행 지점" 역할만 하는 태스크. 예) `build`, `assemble`, `check`

이 구분을 알면 `build`를 실행했을 때 왜 로그에 별다른 출력 없이 하위 태스크들만 줄줄이 실행되는지 이해 가능.

### 태스크 작성 시 성능 체크리스트

- `register()`를 기본으로 사용: `create()`는 즉시 태스크를 설정하지만 `register()`는 실제로 필요해질 때까지 설정을 지연시켜, 사용하지 않는 태스크의 구성 비용 절감
- 입력/출력을 반드시 선언: 그래야 Gradle이 최신 상태 검사를 수행해 불필요한 재실행을 건너뜀
- 실행 로직은 `@TaskAction`/`doLast` 안에만 배치: 설정 블록(구성 단계)에서 무거운 작업을 수행하면 태스크를 실행하지 않아도 매 빌드마다 그 비용 발생
- `group`과 `description`을 채움: 태스크 탐색성과 협업 편의성을 높이는 최소한의 관행

### 핵심 포인트

- 태스크는 "타입(무엇으로 만들어졌는가)"과 "액션(무엇을 하는가)"의 조합이며, 내장 타입·인라인 등록·커스텀 타입 세 가지 방식으로 생성 가능
- `@Input`/`@OutputFile` 등으로 입출력을 선언해야 Gradle의 최신 상태 검사와 증분 빌드가 제대로 동작함
- `doFirst`/`doLast`는 액션을 앞뒤로 누적하고, `dependsOn`은 태스크 간 실행 순서를, `tasks.named()`는 이미 등록된 태스크에 대한 후속 설정을 담당
- `register()` 위주의 지연 구성과 명확한 입출력 선언은 대규모 빌드에서 불필요한 구성·실행 비용을 줄이는 핵심 습관

---

## Gradle 관리형 타입 (Gradle Managed Types)

> 원문: https://docs.gradle.org/current/userguide/gradle_managed_types_intermediate.html

### 개요

빌드 스크립트를 작성할 때 흔히 Groovy나 Kotlin의 일반 타입(`String`, `File` 등)을 그대로 쓰게 되지만, Gradle은 이를 대체할 자체 타입 집합을 제공함. `Property<T>`, `Provider<T>` 같은 이 타입들을 관리형 타입(Managed Types)이라 부르며, 값을 즉시 계산하지 않고 필요한 시점까지 미뤄두는 지연(lazy) 평가 방식으로 동작함. 관리형 타입을 쓰면 증분 빌드, 빌드 캐시, 설정 캐시(Configuration Cache)가 제대로 작동하고, 태스크의 입력·출력을 Gradle이 정확히 추적 가능.

### 왜 일반 타입이 아니라 관리형 타입인가

일반 타입으로 값을 다루면 스크립트가 로드되는 즉시(설정 단계) 값이 계산되어 버림. 반면 관리형 타입은 실제로 값이 필요해지는 실행 단계까지 계산을 늦춤.

```kotlin
// Eager: 지금 즉시 평가됨
val version = project.version.toString()
val file = File("build/output.txt")

// Lazy: 나중에 평가됨
val outputFile: RegularFileProperty = project.objects.fileProperty()
outputFile.set(layout.buildDirectory.file("output.txt"))
```

문서가 제시한 데모 태스크를 보면 차이가 뚜렷함. `String`으로 만든 값(eager)은 설정 단계에서 이미 `"Hello from eager"`라는 실제 문자열로 확정되지만, `objects.property(String)`로 만든 값(lazy)은 설정 단계에서는 아직 `DefaultProperty`라는 "값을 담을 그릇" 상태일 뿐이고, `doLast` 블록에서 `.get()`을 호출해야 비로소 `"Hello from lazy"`라는 실제 값이 계산됨.

### 핵심 포인트: Eager 평가가 문제인 이유

- Eager 값은 태스크가 실제로 실행되지 않아도 이미 계산이 끝나 있음 → 설정 단계에서 불필요한 연산 발생
- 이렇게 미리 확정된 값은 Gradle이 설정 캐시에 안전하게 저장할 수 없어 설정 캐시 호환성을 깨뜨림
- 반대로 Lazy 값은 계산을 미뤄두는 덕분에 Gradle이 캐싱, 병렬 실행, 설정 단계 회피(configuration avoidance) 같은 최적화를 적용할 여지를 만들어 줌

### 자주 쓰는 관리형 타입

- `Property<T>`: 지연 계산되는 단일 스칼라 값
  - 사용 예: 버전 문자열
- `ListProperty<T>`: 지연 계산되는 리스트
  - 사용 예: 컴파일러 옵션 목록
- `MapProperty<K, V>`: 지연 계산되는 맵
  - 사용 예: 환경 변수, POM 속성 맵
- `RegularFileProperty`: 단일 파일(입력/출력)에 대한 지연 참조
  - 사용 예: 입출력 파일 하나
- `DirectoryProperty`: 디렉터리에 대한 지연 참조
  - 사용 예: 결과물이 담길 디렉터리
- `Provider<T>`: 읽기 전용으로 지연 계산되는 값
  - 사용 예: 다른 태스크 출력에 대한 의존

- `Property`, `RegularFileProperty`, `DirectoryProperty`는 값을 쓸 수 있는(set) 컨테이너이고, `Provider`는 읽기 전용. `Property`도 `Provider`의 하위 타입이라 어디서든 `Provider`가 필요한 자리에 대신 넣을 수 있음
- 이 목록에 있는 타입 대부분은 `project.objects`(ObjectFactory)를 통해 생성함

### 설정 캐시와 함께 보는 실전 예제

같은 일을 하더라도 값을 즉시 꺼내 쓰면 설정 캐시와 호환되지 않고, `Property`로 감싸 지연 계산하면 호환됨.

```kotlin
// 설정 캐시 비호환: doLast 안에서도 project.version을 직접, 즉시 읽음
tasks.register("printVersion") {
    doLast {
        val eager_version = project.version.toString()
        println("Version is $eager_version")
    }
}

// 설정 캐시 호환: Property에 값을 담아두고, get()으로 실행 시점에만 꺼냄
tasks.register("printVersionLazy") {
    val lazy_version: Property<String> = project.objects.property(String::class.java)
    lazy_version.set(project.version.toString())
    doLast {
        println("Version is ${lazy_version.get()}")
    }
}
```

두 코드 모두 `doLast` 블록 안에 있다는 점은 같지만, 첫 번째는 `project.version`이라는 프로젝트 상태를 태스크 실행 로직 안에서 직접 참조하고 있어 문제가 됨. 두 번째는 필요한 값을 미리 `Property`에 캡슐화해두고, 실행 시점에는 그 `Property` 자체만 참조하기 때문에 Gradle이 안전하게 직렬화·캐시 가능.

### 핵심 포인트: 관리형 타입을 쓸 때 기억할 것

- 태스크 로직은 가능한 한 실행 단계에서만 평가되도록 작성하는 것이 기본 원칙. 설정 단계에서 무거운 계산을 미리 해두면 그만큼 매 빌드마다 낭비되는 시간이 늘어남
- `String`, `File` 같은 일반 타입 대신 `Property<T>`, `RegularFileProperty`, `Provider<T>` 등을 습관적으로 사용하면 증분 빌드·빌드 캐시·설정 캐시라는 세 가지 성능 기능을 모두 안전하게 누릴 수 있음
- 관리형 타입은 결국 "값 그 자체"가 아니라 "값을 나중에 계산하는 방법"을 감싸는 래퍼라고 이해하면, 왜 태스크 입력·출력 선언에 이 타입들이 필수적으로 쓰이는지도 자연스럽게 연결됨
