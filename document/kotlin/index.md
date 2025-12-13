# Kotlin 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 문서: https://kotlinlang.org/docs/
> Kotlin 버전: 2.1.0 / kotlinx-coroutines: 1.10.2

## 개요

Kotlin은 JetBrains에서 개발한 현대적인 프로그래밍 언어입니다. JVM, Android, JavaScript, Native를 타겟으로 하며, Java와 100% 상호 운용이 가능합니다.

### 주요 특징

- ** 간결성**: 보일러플레이트 코드 최소화
- ** 안전성**: Null Safety로 NPE 방지
- ** 상호 운용성**: Java와 완벽한 호환
- ** 도구 친화적**: IDE 지원 우수

---

## 문서 목록

### 기본

| 문서 | 설명 |
|------|------|
| [basics.md](./basics.md) | 기본 문법, 변수, 조건문, 반복문, 연산자 |
| [types.md](./types.md) | 타입 시스템, 숫자, 문자열, 배열, Any/Unit/Nothing |
| [null-safety.md](./null-safety.md) | Null Safety, Safe Call, Elvis 연산자 |

### 객체 지향

| 문서 | 설명 |
|------|------|
| [classes.md](./classes.md) | 클래스, 상속, 인터페이스, Data/Sealed/Enum 클래스 |
| [functions.md](./functions.md) | 함수, 람다, 고차 함수, 연산자 오버로딩 |
| [scope-functions.md](./scope-functions.md) | let, run, with, apply, also |

### 컬렉션

| 문서 | 설명 |
|------|------|
| [collections.md](./collections.md) | List, Set, Map, 컬렉션 연산, Sequence |

### 비동기 프로그래밍

| 문서 | 설명 |
|------|------|
| [coroutines.md](./coroutines.md) | Coroutines, Dispatchers, Job, Channel |
| [flow.md](./flow.md) | Flow, StateFlow, SharedFlow |

---

## 빠른 참조

### 변수 선언

```kotlin
val name: String = "John"    // 불변
var count = 0                // 가변, 타입 추론
const val MAX = 100          // 컴파일 타임 상수
```

### Null Safety

```kotlin
val name: String? = null     // Nullable
name?.length                 // Safe call
name ?: "default"            // Elvis operator
name!!.length                // Not-null assertion
```

### 함수

```kotlin
fun greet(name: String = "World") = "Hello, $name"
fun String.exclaim() = "$this!"
```

### 클래스

```kotlin
data class User(val id: Long, val name: String)
sealed class Result<out T> { ... }
object Singleton { ... }
```

### 컬렉션

```kotlin
val list = listOf(1, 2, 3)
val map = mapOf("a" to 1)
list.filter { it > 1 }.map { it * 2 }
```

### 스코프 함수

| 함수 | 객체 참조 | 반환 값 |
|------|----------|--------|
| let | it | 람다 결과 |
| run | this | 람다 결과 |
| with | this | 람다 결과 |
| apply | this | 객체 |
| also | it | 객체 |

### Coroutines

```kotlin
launch { delay(1000); println("World") }
async { fetchData() }.await()
flow { emit(1); emit(2) }.collect { println(it) }
```

---

## 참고 문서

- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Kotlin Playground](https://play.kotlinlang.org/)
