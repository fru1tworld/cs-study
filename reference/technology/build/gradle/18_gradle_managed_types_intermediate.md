# Gradle 관리형 타입 (Gradle Managed Types)

> **원문:** https://docs.gradle.org/current/userguide/gradle_managed_types_intermediate.html

## 개요

빌드 스크립트를 작성할 때 흔히 Groovy나 Kotlin의 일반 타입(`String`, `File` 등)을 그대로 쓰게 되지만, Gradle은 이를 대체할 자체 타입 집합을 제공합니다. `Property<T>`, `Provider<T>` 같은 이 타입들을 **관리형 타입(Managed Types)**이라 부르며, 값을 즉시 계산하지 않고 필요한 시점까지 미뤄두는 **지연(lazy) 평가** 방식으로 동작합니다. 관리형 타입을 쓰면 증분 빌드, 빌드 캐시, 설정 캐시(Configuration Cache)가 제대로 작동하고, 태스크의 입력·출력을 Gradle이 정확히 추적할 수 있습니다.

## 왜 일반 타입이 아니라 관리형 타입인가

일반 타입으로 값을 다루면 스크립트가 로드되는 즉시(설정 단계) 값이 계산되어 버립니다. 반면 관리형 타입은 실제로 값이 필요해지는 실행 단계까지 계산을 늦춥니다.

```kotlin
// Eager: 지금 즉시 평가됨
val version = project.version.toString()
val file = File("build/output.txt")

// Lazy: 나중에 평가됨
val outputFile: RegularFileProperty = project.objects.fileProperty()
outputFile.set(layout.buildDirectory.file("output.txt"))
```

문서가 제시한 데모 태스크를 보면 차이가 뚜렷합니다. `String`으로 만든 값(eager)은 설정 단계에서 이미 `"Hello from eager"`라는 실제 문자열로 확정되지만, `objects.property(String)`로 만든 값(lazy)은 설정 단계에서는 아직 `DefaultProperty`라는 "값을 담을 그릇" 상태일 뿐이고, `doLast` 블록에서 `.get()`을 호출해야 비로소 `"Hello from lazy"`라는 실제 값이 계산됩니다.

## 핵심 포인트: Eager 평가가 문제인 이유

- Eager 값은 태스크가 실제로 실행되지 않아도 이미 계산이 끝나 있습니다. 설정 단계에서 불필요한 연산이 발생하는 셈입니다.
- 이렇게 미리 확정된 값은 Gradle이 설정 캐시에 안전하게 저장할 수 없어 **설정 캐시 호환성을 깨뜨립니다.**
- 반대로 Lazy 값은 계산을 미뤄두는 덕분에 Gradle이 캐싱, 병렬 실행, 설정 단계 회피(configuration avoidance) 같은 최적화를 적용할 여지를 만들어 줍니다.

## 자주 쓰는 관리형 타입

| 타입 | 용도 | 사용 예 |
|---|---|---|
| `Property<T>` | 지연 계산되는 단일 스칼라 값 | 버전 문자열 |
| `ListProperty<T>` | 지연 계산되는 리스트 | 컴파일러 옵션 목록 |
| `MapProperty<K, V>` | 지연 계산되는 맵 | 환경 변수, POM 속성 맵 |
| `RegularFileProperty` | 단일 파일(입력/출력)에 대한 지연 참조 | 입출력 파일 하나 |
| `DirectoryProperty` | 디렉터리에 대한 지연 참조 | 결과물이 담길 디렉터리 |
| `Provider<T>` | 읽기 전용으로 지연 계산되는 값 | 다른 태스크 출력에 대한 의존 |

- `Property`, `RegularFileProperty`, `DirectoryProperty`는 값을 **쓸 수 있는(set)** 컨테이너이고, `Provider`는 **읽기 전용**입니다. `Property`도 `Provider`의 하위 타입이라 어디서든 `Provider`가 필요한 자리에 대신 넣을 수 있습니다.
- 이 표에 있는 타입 대부분은 `project.objects`(ObjectFactory)를 통해 생성합니다.

## 설정 캐시와 함께 보는 실전 예제

같은 일을 하더라도 값을 즉시 꺼내 쓰면 설정 캐시와 호환되지 않고, `Property`로 감싸 지연 계산하면 호환됩니다.

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

두 코드 모두 `doLast` 블록 안에 있다는 점은 같지만, 첫 번째는 `project.version`이라는 프로젝트 상태를 태스크 실행 로직 안에서 직접 참조하고 있어 문제가 됩니다. 두 번째는 필요한 값을 미리 `Property`에 캡슐화해두고, 실행 시점에는 그 `Property` 자체만 참조하기 때문에 Gradle이 안전하게 직렬화·캐시할 수 있습니다.

## 핵심 포인트: 관리형 타입을 쓸 때 기억할 것

- 태스크 로직은 가능한 한 실행 단계에서만 평가되도록 작성하는 것이 기본 원칙입니다. 설정 단계에서 무거운 계산을 미리 해두면 그만큼 매 빌드마다 낭비되는 시간이 늘어납니다.
- `String`, `File` 같은 일반 타입 대신 `Property<T>`, `RegularFileProperty`, `Provider<T>` 등을 습관적으로 사용하면 증분 빌드·빌드 캐시·설정 캐시라는 세 가지 성능 기능을 모두 안전하게 누릴 수 있습니다.
- 관리형 타입은 결국 "값 그 자체"가 아니라 "값을 나중에 계산하는 방법"을 감싸는 래퍼라고 이해하면, 왜 태스크 입력·출력 선언에 이 타입들이 필수적으로 쓰이는지도 자연스럽게 연결됩니다.
