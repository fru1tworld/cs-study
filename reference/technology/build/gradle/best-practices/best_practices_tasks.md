# 태스크 작성 모범 사례 (Best Practices for Tasks)

> **원문:** https://docs.gradle.org/current/userguide/best_practices_tasks.html

## 개요

Gradle 태스크는 편하게 쓰려고 하면 성능과 캐시 정확성을 갉아먹기 쉬운 함정이 많은 영역입니다. 대부분의 문제는 "구성 단계(configuration phase)에서 값을 너무 일찍 확정해버린다"는 한 가지 패턴에서 비롯됩니다. 아래 항목들은 그 함정을 피하고 최신 상태(up-to-date) 검사·빌드 캐시·구성 캐시가 제대로 동작하도록 태스크를 설계하는 규칙들입니다.

## 1. dependsOn은 라이프사이클 태스크에만 쓴다

- `dependsOn`은 "무조건 먼저 실행되어야 한다"는 거친(coarse-grained) 관계만 표현할 뿐, Gradle에게 "왜" 그 태스크가 필요한지는 알려주지 못합니다.
- 액션이 있는 태스크 사이의 관계는 `dependsOn`이 아니라 **입력·출력 와이어링**으로 표현해야 합니다. 그래야 Gradle이 실제로 필요한 파일만 추적해 불필요한 재실행을 줄일 수 있습니다.

```kotlin
// 지양: dependsOn만으로 관계를 표현
tasks.register<SimpleTranslationTask>("translateBad") {
    dependsOn(tasks.named("helloWorld"))
}

// 권장: 출력을 입력으로 직접 연결 (암묵적 의존성 생성)
tasks.register<SimpleTranslationTask>("translateGood") {
    inputs.file(tasks.named<SimplePrintingTask>("helloWorld").map { messageFile })
}
```

- `dependsOn`은 자신은 액션이 없고 하위 태스크들을 묶어주기만 하는 라이프사이클 태스크(`build`, `check` 등)에만 사용합니다.

## 2. 캐시 여부는 애너테이션으로, cacheIf는 지양한다

- 태스크 타입에 `@CacheableTask` 또는 `@DisableCachingByDefault`를 붙여 캐시 가능 여부를 **타입 수준**에서 고정합니다.
- 인스턴스마다 `outputs.cacheIf { ... }`를 호출하는 방식은 실수하기 쉽고(설정을 빼먹으면 캐시가 적용되지 않음), 런타임에 조건을 매번 평가해야 해서 비효율적입니다.

```kotlin
// 지양: 인스턴스별로 캐시 조건 설정 (누락 위험)
tasks.register<BadCalculatorTask>("addBad1") {
    outputs.cacheIf { true }
}
tasks.register<BadCalculatorTask>("addBad2") {
    // 캐시 설정을 깜빡함
}

// 권장: 타입에 애너테이션 부여
@CacheableTask
abstract class GoodCalculatorTask : DefaultTask() { /* ... */ }
```

## 3. 구성 단계에서 Provider.get()을 호출하지 않는다

- `get()`은 값을 즉시 확정시키는 호출입니다. 구성 단계에서 호출하면 아직 최종 값이 아닌 상태를 읽어버리거나, 구성 캐시 미스를 유발하거나, Gradle이 자동으로 추적해줄 태스크 간 의존성 연결이 끊어질 수 있습니다.
- 값을 가공해야 한다면 `map()`/`flatMap()`으로 지연 변환을 걸어두고, 실제 실행 시점에 읽히도록 합니다.

```kotlin
// 지양: 구성 시점에 즉시 값 확정
tasks.register<MyTask>("avoidThis") {
    myInput = "currentEnvironment=${currentEnvironment.get()}"
}

// 권장: map()으로 지연 평가
tasks.register<MyTask>("doThis") {
    myInput = currentEnvironment.map { "currentEnvironment=$it" }
}
```

## 4. 커스텀 태스크에는 group과 description을 채운다

- `group`은 짧고 소문자로, 태스크의 목적(문서화, 검증, 릴리스, 배포 등)을 나타내야 합니다.
- 이 정보는 `./gradlew tasks` 결과에 그대로 노출됩니다. `group`이 없는 태스크는 `--all` 옵션을 주지 않으면 목록에서 숨겨지고, 있어도 "Other tasks"처럼 애매한 분류에 묻혀 찾기 어렵습니다.

```kotlin
// 지양: 분류 정보 없음 → "Other tasks"에 묻힘
tasks.register("generateDocs") { /* ... */ }

// 권장: 그룹과 설명 명시 → "Documentation tasks"에 정리됨
tasks.register("generateDocs") {
    group = "documentation"
    description = "소스 파일로부터 프로젝트 문서를 생성합니다."
}
```

## 5. FileCollection/Configuration에 즉시(eager) API를 쓰지 않는다

- `Configuration`이나 `FileCollection`에서 `.size()`, `.isEmpty()`, `.getFiles()`, `.asPath()`, `.toList()` 같은 메서드를 구성 단계에서 호출하면, 그 순간 의존성 해석(resolution)이 강제로 일어납니다.
- 이는 구성 캐시 미스, 너무 이른 시점의 잘못된 평가, 태스크 간 자동 의존성 연결 붕괴로 이어집니다.

```kotlin
// 지양: isEmpty() 호출이 구성 단계에 해석을 강제함
tasks.register<FileCounterTask>("badCountingTask") {
    if (!configurations.runtimeClasspath.get().isEmpty()) {
        countMe.from(configurations.runtimeClasspath)
    }
}

// 권장: 지연 프로퍼티에 그대로 할당, 해석은 실행 시점으로 미룸
tasks.register<FileCounterTask>("goodCountingTask") {
    countMe.from(configurations.runtimeClasspath)
}
```

## 6. 태스크 실행 전에 Configuration을 미리 resolve()하지 않는다

- `resolve()`를 호출해 얻은 파일 집합은 "어떤 태스크가 이 파일을 만들었는지"에 대한 참조 정보를 잃어버립니다.
- 그 결과 소비자(consumer) 태스크와 생산자(producer) 태스크 사이의 암묵적 의존성이 끊어져, 실행 순서가 보장되지 않고 파일이 아직 생성되지 않은 채로 참조될 수 있습니다.

```kotlin
// 지양: resolve()로 즉시 해석 → 암묵적 의존성 소실
tasks.register("badClasspathPrinter", BadClasspathPrinter::class) {
    classpath = configurations.named("runtimeClasspath").get().resolve()
}

// 권장: ConfigurableFileCollection에 지연 연결
tasks.register("goodClasspathPrinter", GoodClasspathPrinter::class.java) {
    classpath.from(configurations.named("runtimeClasspath"))
}
```

## 7. 경로 민감도(PathSensitivity)를 상황에 맞게 낮춘다

- 태스크는 보통 입력 파일의 **내용**에만 관심이 있을 뿐, 그 파일이 디스크의 어느 경로에 있는지는 중요하지 않습니다.
- 기본값인 `ABSOLUTE`는 경로 전체를 캐시 키에 포함시켜, 다른 머신이나 다른 체크아웃 위치에서는 빌드 캐시·구성 캐시가 전혀 재사용되지 않게 만듭니다.
- 단일 파일 입력에는 `PathSensitivity.NONE`을, 디렉터리 입력에는 상대 구조가 의미 있는 경우 `PathSensitivity.RELATIVE`를 지정합니다.

```kotlin
// 지양: 절대 경로까지 캐시 키에 포함됨
@InputFile
@PathSensitive(PathSensitivity.ABSOLUTE)
abstract val candidatesFile: RegularFileProperty

// 권장: 내용만 비교, 경로가 달라도 UP-TO-DATE 유지
@InputFile
@PathSensitive(PathSensitivity.NONE)
abstract val candidatesFile: RegularFileProperty
```

## 8. 출력 파일/디렉터리는 태스크마다 겹치지 않게 한다

- 여러 태스크가 같은 출력 디렉터리를 공유하면, 한 태스크가 그 디렉터리에 쓴 결과가 다른 태스크의 출력과 뒤섞여 "겹치는 출력(overlapping output)"이 발생합니다.
- 이 경우 실제로 바뀐 파일이 없어도, 같은 디렉터리를 쓰는 다른 태스크가 실행되었다는 사실만으로 소비자 태스크가 무효화되어 불필요하게 재실행됩니다.

```kotlin
// 지양: greeterA, greeterB가 같은 디렉터리를 출력으로 공유
val greeterA = tasks.register<GreetingTask>("greeterA") {
    outputDirectory = layout.buildDirectory.dir("greetings")
}
tasks.register<GreetingTask>("greeterB") {
    outputDirectory = layout.buildDirectory.dir("greetings")
}
// consumer는 greeterA의 출력만 쓰지만, greeterB 실행만으로도 무효화됨

// 권장: 각 태스크가 자신만의 출력 파일을 갖도록 분리
val greeterA = tasks.register<GreetingTask>("greeterA") {
    outputFile = layout.buildDirectory.dir("greetings").map { it.file("a.txt") }
}
tasks.register<GreetingTask>("greeterB") {
    outputFile = layout.buildDirectory.dir("greetings").map { it.file("b.txt") }
}
```

## 핵심 포인트

- 태스크 간 관계는 `dependsOn`이 아니라 입력·출력 와이어링으로 표현해야 Gradle이 정확한 최신 상태 검사를 할 수 있다.
- 캐시 가능 여부(`@CacheableTask`)는 인스턴스가 아니라 타입에 선언하고, `Provider.get()`이나 `Configuration.resolve()` 같은 즉시 평가 API는 구성 단계에서 호출하지 않는다.
- `group`/`description`으로 태스크를 문서화하고, `@PathSensitivity`를 적절히 낮춰 캐시 재사용성(relocatability)을 확보한다.
- 태스크마다 고유한 출력 파일/디렉터리를 갖도록 설계해야 불필요한 재실행과 캐시 무효화를 막을 수 있다.
