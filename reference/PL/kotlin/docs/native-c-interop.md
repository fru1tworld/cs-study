# C와의 상호운용성

이 문서는 **cinterop** 도구를 사용한 Kotlin/Native의 C 라이브러리 상호운용성을 다룹니다.

## 개요

- C 라이브러리 임포트는 **베타** 상태이며 `@ExperimentalForeignApi` 어노테이션이 필요합니다
- cinterop 도구는 C 헤더를 분석하고 Kotlin 바인딩을 자동으로 생성합니다
- 생성된 스텁은 IDE 코드 완성 및 네비게이션을 가능하게 합니다

## 프로젝트 설정

1. 바인딩에 포함할 내용을 설명하는 [정의 파일](native-definition-file.html) 생성 및 구성
2. cinterop을 포함하도록 Gradle 빌드 파일 구성
3. 실행 파일을 생성하기 위해 컴파일 및 실행

실습 경험을 위해 [C 상호운용을 사용한 앱 만들기](native-app-with-c-and-libcurl.html) 튜토리얼을 참조하세요.

**참고:** 플랫폼 라이브러리(POSIX, Win32, Apple 프레임워크)는 사용자 정의 구성이 필요하지 않습니다.

---

## 바인딩

### 기본 상호운용 타입

| C 타입 | Kotlin 매핑 |
|--------|-------------|
| 정수/부동소수점 | 동일한 너비의 Kotlin 대응 |
| 포인터 및 배열 | `CPointer<T>?` |
| 열거형 | Kotlin 열거형 또는 정수 값 |
| 구조체/공용체 | 점 표기법 필드 접근이 있는 타입 |
| `typedef` | `typealias` |

**Lvalue 표현** (메모리 위치 값):
- 구조체: 구조체와 동일한 이름
- Kotlin 열거형: `${type}.Var`
- `CPointer<T>`: `CPointerVar<T>`
- 기타 타입: `${type}Var`

### 포인터 타입

```kotlin
val path = getenv("PATH")?.toKString() ?: ""

import kotlinx.cinterop.*
@OptIn(ExperimentalForeignApi::class)
fun shift(ptr: CPointer<ByteVar>, length: Int) {
    for (index in 0 .. length - 2) {
        ptr[index] = ptr[index + 1]
    }
}
```

**주요 연산:**
- `.pointed`는 포인터가 가리키는 lvalue를 반환
- `.ptr`은 lvalue를 포인터로 변환
- `void*`는 `COpaquePointer`로 매핑 (모든 포인터의 상위 타입)

**포인터 캐스팅:**

```kotlin
val intPtr = bytePtr.reinterpret<IntVar>()
val longValue = ptr.toLong()
val originalPtr = longValue.toCPointer<IntVar>()
```

### 메모리 할당

```kotlin
@file:OptIn(ExperimentalForeignApi::class)
import kotlinx.cinterop.*

// nativeHeap 사용 (수동 free 필요)
val buffer = nativeHeap.allocArray<ByteVar>(size)
nativeHeap.free(buffer)

// memScoped 사용 (자동 free)
val fileSize = memScoped {
    val statBuf = alloc<stat>()
    val error = stat("/", statBuf.ptr)
    statBuf.st_size
}
```

### 바인딩에 포인터 전달

C 함수 포인터 매개변수는 `CValuesRef<T>`로 매핑됩니다. 다음으로 시퀀스를 구성합니다:

```kotlin
// C: void foo(int* elements, int count);
// Kotlin:
foo(cValuesOf(1, 2, 3), 3)

// 다른 방법:
intArrayOf(1, 2, 3).toCValues()
listOf(ptr1, ptr2).toCValues()
```

### 문자열

`const char*` 타입의 매개변수는 Kotlin `String`으로 매핑됩니다:

```kotlin
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
memScoped {
    LoadCursorA(null, "cursor.bmp".cstr.ptr)  // UTF-8
    LoadCursorW(null, "cursor.bmp".wcstr.ptr) // UTF-16
}

// 수동 변환:
val cString = kotlinString.cstr.getPointer(nativeHeap)
val kotlinStr = cPointer.toKString()
```

자동 변환을 건너뛰려면 `.def` 파일에 추가:

```
noStringConversion = LoadCursorA LoadCursorW
```

### 스코프 로컬 포인터

```kotlin
memScoped {
    items = arrayOfNulls<CPointer<ITEM>?>(6)
    arrayOf("one", "two").forEachIndexed { index, value ->
        items[index] = value.cstr.ptr
    }
    menu = new_menu("Menu".cstr.ptr, items.toCValues().ptr)
}
```

### 값으로 구조체 전달 및 수신

C 함수가 값으로 구조체를 받거나 반환할 때 `CValue<T>`를 사용합니다:

```kotlin
// 구조체 값에서 필드 읽기
val fieldValue = structValue.useContents { field }

// 구조체 값 생성
val structVal = cValue {
    field1 = 10
    field2 = 20
}

// 수정된 복사본 생성
val newStructVal = structValue.copy {
    field1 = 30
}
```

### 콜백

```kotlin
// Kotlin 함수를 C 함수 포인터로 변환
staticCFunction(::kotlinFunction)

// StableRef로 사용자 데이터 전달
val stableRef = StableRef.create(kotlinObject)
val voidPtr = stableRef.asCPointer()

// 콜백에서 언래핑
val stableRef = voidPtr.asStableRef<KotlinClass>()
val kotlinRef = stableRef.get()

stableRef.dispose() // 메모리 누수 방지
```

### 이식성

플랫폼 종속 타입(`long`, `size_t`)의 경우 `.convert()`를 사용합니다:

```kotlin
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun zeroMemory(buffer: COpaquePointer, size: Int) {
    memset(buffer, 0, size.convert<size_t>())
}
```

### 객체 피닝

**`.usePinned()` 사용:**

```kotlin
val buffer = ByteArray(1024)
buffer.usePinned { pinned ->
    recv(fd, pinned.addressOf(0), buffer.size.convert(), 0)
}
```

**`.refTo()` 사용:**

```kotlin
val buffer = ByteArray(1024)
recv(fd, buffer.refTo(0), buffer.size.convert(), 0)
```

### 전방 선언

`cnames` 패키지를 통해 전방 선언을 임포트합니다:

```kotlin
import cnames.structs.ForwardDeclaredStruct

fun test() {
    consumeStruct(produceStruct() as CPointer<cnames.structs.ForwardDeclaredStruct>)
}
```

---

## 다음 단계

관련 튜토리얼:
- [C에서 원시 데이터 타입 매핑](mapping-primitive-data-types-from-c.html)
- [C에서 구조체 및 공용체 타입 매핑](mapping-struct-union-types-from-c.html)
- [C에서 함수 포인터 매핑](mapping-function-pointers-from-c.html)
- [C에서 문자열 매핑](mapping-strings-from-c.html)
