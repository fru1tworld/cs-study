# 코틀린/Native

# Kotlin/Native 시작하기 - 완전 가이드

> **원문:** https://kotlinlang.org/docs/native-get-started.html

## 개요

이 튜토리얼은 Kotlin/Native 애플리케이션을 만드는 세 가지 방법을 다룹니다:
1. **IntelliJ IDEA** (IDE 접근 방식)
2. **Gradle** (빌드 시스템)
3. **명령줄 컴파일러** (직접 컴파일)

Kotlin/Native는 다양한 타겟(Linux, macOS, Windows)으로 컴파일합니다. 크로스 플랫폼 컴파일이 가능하지만, 이 튜토리얼은 동일 플랫폼 컴파일에 중점을 둡니다.

**Mac 사용자**: 먼저 Xcode 명령줄 도구를 설치하세요.

---

## 방법 1: IntelliJ IDEA 사용

### 프로젝트 생성

1. 최신 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 다운로드 (Community 또는 Ultimate Edition)
2. 프로젝트 템플릿 복제:
   - **File | New | Project from Version Control** 선택
   - URL: `https://github.com/Kotlin/kmp-native-wizard`
3. `gradle/libs.versions.toml`을 열고 최신 Kotlin 버전 확인:
   ```
   [versions]
   kotlin = "2.3.10"
   ```
4. Gradle 파일 다시 로드 제안을 따름

### 빌드 및 실행

1. `src/nativeMain/kotlin/Main.kt` 열기
2. 거터에서 녹색 실행 아이콘 누르기
3. IntelliJ IDEA가 Gradle을 통해 코드를 실행하고 Run 탭에 출력 표시

### 애플리케이션 업데이트

**기본 이름 입력:**

```kotlin
fun main() {
    println("Hello, enter your name:")
    val name = readln()
}
```

**이름의 글자 수 세기:**

```kotlin
fun main() {
    println("Hello, enter your name:")
    val name = readln()

    name.replace(" ", "").let {
        println("Your name contains ${it.length} letters")
    }
}
```

Gradle 입력 지원을 위해 `build.gradle.kts` 업데이트:

```kotlin
kotlin {
    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
                runTaskProvider?.configure {
                    standardInput = System.`in`
                }
            }
        }
    }
}
```

**고유 글자 수 세기:**

```kotlin
fun String.countDistinctCharacters() = lowercase().toList().distinct().count()

fun main() {
    println("Hello, enter your name:")
    val name = readln()

    name.replace(" ", "").let {
        println("Your name contains ${it.length} letters")
        println("Your name contains ${it.countDistinctCharacters()} unique letters")
    }
}
```

---

## 방법 2: Gradle 사용

### 프로젝트 파일 생성

1. 호환되는 [Gradle 버전](https://gradle.org/install/) 설치

2. `build.gradle.kts` 생성:

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.10"
}

repositories {
    mavenCentral()
}

kotlin {
    macosArm64("native") {      // macOS에서
    // linuxArm64("native")      // Linux에서
    // mingwX64("native")        // Windows에서
        binaries {
            executable()
        }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "9.0.0"
    distributionType = Wrapper.DistributionType.BIN
}
```

3. 빈 `settings.gradle(.kts)` 파일 생성

4. `src/nativeMain/kotlin/hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/Native!")
}
```

### 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew nativeBinaries

# 실행 파일 실행
build/bin/native/debugExecutable/<project_name>.kexe
```

이렇게 하면 `build/bin/native/debugExecutable` 및 `releaseExecutable` 디렉토리가 생성됩니다.

### IDE에서 열기

- **File | Open** 선택
- 프로젝트 디렉토리 선택
- IntelliJ IDEA가 Kotlin/Native 프로젝트를 자동으로 감지

---

## 방법 3: 명령줄 컴파일러 사용

### 다운로드 및 설치

1. [Kotlin GitHub 릴리스](https://github.com/JetBrains/kotlin/releases/tag/v2.3.10)로 이동
2. `kotlin-native-prebuilt-<os>-<arch>-2.3.10.tar.gz` 다운로드
3. 아카이브 압축 해제
4. PATH에 추가:

```bash
export PATH="/<path to compiler>/kotlin-native/bin:$PATH"
```

**요구 사항**: Java 1.8 이상

### 프로그램 생성

`hello.kt` 생성:

```kotlin
fun main() {
    println("Hello, Kotlin/Native!")
}
```

### 컴파일 및 실행

```bash
# 컴파일
kotlinc-native hello.kt -o hello

# 실행
./hello.kexe    # macOS/Linux
./hello.exe     # Windows
```

`-o` 옵션은 출력 파일명을 지정합니다. 컴파일러는 `.kexe` (macOS/Linux) 또는 `.exe` (Windows)를 생성합니다.

---

## 타겟 플랫폼

| 플랫폼 | 타겟 |
|--------|------|
| macOS (ARM) | `macosArm64` |
| macOS (x64) | `macosX64` |
| Linux (ARM) | `linuxArm64` |
| Linux (x64) | `linuxX64` |
| Windows | `mingwX64` |
| iOS (ARM) | `iosArm64` |
| iOS 시뮬레이터 | `iosSimulatorArm64` |

---

## 다음 단계

- [C 상호운용 및 libcurl을 사용한 앱 만들기](native-app-with-c-and-libcurl.html) 튜토리얼 완료
- 고급 [Gradle 빌드 스크립트](/docs/multiplatform/multiplatform-dsl-reference.html) 학습
- [Gradle 문서](gradle.html) 탐색

---

# Kotlin/Native 개요

> **원문:** https://kotlinlang.org/docs/native-overview.html

## Kotlin/Native란?

**Kotlin/Native**는 Kotlin 코드를 가상 머신 없이 실행되는 네이티브 바이너리로 컴파일하는 기술입니다. 다음을 특징으로 합니다:
- Kotlin 컴파일러를 위한 LLVM 기반 백엔드
- Kotlin 표준 라이브러리의 네이티브 구현

## Kotlin/Native를 사용하는 이유?

Kotlin/Native는 다음을 위해 설계되었습니다:
- **VM이 없는 플랫폼**: 임베디드 장치, iOS 및 기타 제한된 환경
- **자체 포함 프로그램**: 추가 런타임이나 VM 불필요
- **쉬운 통합**: C, C++, Swift, Objective-C 및 기타 언어의 기존 프로젝트와 원활하게 작동

## 지원되는 타겟 플랫폼

- **Linux**
- **Windows** (MinGW를 통해)
- **Android NDK**
- **Apple 플랫폼**: macOS, iOS, tvOS, watchOS
  - *Xcode 및 명령줄 도구 필요*

## 상호운용성 기능

### C와의 상호운용성

- Kotlin 코드에서 직접 기존 C 라이브러리 사용
- 제공되는 튜토리얼:
  - C 헤더가 있는 동적 라이브러리 생성
  - C 타입을 Kotlin으로 매핑
  - libcurl로 네이티브 HTTP 클라이언트 빌드

### Swift/Objective-C와의 상호운용성

- Objective-C를 통한 양방향 상호운용성
- macOS 및 iOS의 Swift/Objective-C 애플리케이션에서 직접 Kotlin 코드 사용
- Kotlin에서 Apple 프레임워크 생성

## 코드 공유

- **플랫폼 라이브러리**: POSIX, gzip, OpenGL, Metal, Foundation 및 Apple 프레임워크용 사전 빌드된 라이브러리
- **Kotlin Multiplatform**: Android, iOS, JVM, 웹 및 네이티브 플랫폼 간 코드 공유

## 메모리 관리

- JVM 및 Go와 유사한 자동 메모리 관리자
- 내장 추적 가비지 컬렉터
- Swift/Objective-C의 ARC (Automatic Reference Counting)와 통합
- 사용량을 최적화하고 할당 급증을 방지하는 커스텀 메모리 할당자

## 주요 장점

1. **네이티브 성능**: JIT 컴파일 오버헤드 없이 네이티브 속도로 실행
2. **작은 바이너리**: VM 없이 자체 포함된 실행 파일
3. **직접 메모리 접근**: 시스템 수준 프로그래밍 가능
4. **플랫폼 간 공유**: 단일 코드베이스로 여러 플랫폼 지원

## 사용 사례

- **iOS 앱 개발**: Kotlin Multiplatform으로 Android와 iOS 간 코드 공유
- **임베디드 시스템**: 리소스가 제한된 환경에서 실행
- **명령줄 도구**: 빠른 시작 시간과 작은 바이너리
- **게임 개발**: 네이티브 성능이 필요한 경우
- **시스템 프로그래밍**: C 라이브러리와의 통합

## 시작하기

- [Kotlin/Native 시작하기](native-get-started.html)
- [C 상호운용 및 libcurl을 사용한 앱 만들기](native-app-with-c-and-libcurl.html)
- [Gradle 빌드 스크립트 참조](/docs/multiplatform/multiplatform-dsl-reference.html)

## 다음 단계

- C 라이브러리 사용 방법 알아보기
- Swift/Objective-C와의 상호운용성 탐색
- 메모리 관리 이해하기

---

# C와의 상호운용성

> **원문:** https://kotlinlang.org/docs/native-c-interop.html

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

---

# Swift/Objective-C와의 상호운용성

> **원문:** https://kotlinlang.org/docs/native-objc-interop.html

## 개요

Kotlin/Native는 Objective-C를 통해 Swift와의 간접적인 상호운용성을 제공합니다. 이 문서는 Swift/Objective-C 코드에서 Kotlin 선언을 사용하는 것과 그 반대 경우를 다룹니다.

**참고:** Objective-C 라이브러리 임포트는 베타 상태입니다. Objective-C 라이브러리에서 cinterop으로 생성된 모든 Kotlin 선언은 `@ExperimentalForeignApi` 어노테이션이 필요합니다.

## Swift/Objective-C 라이브러리를 Kotlin으로 임포트

Objective-C 프레임워크 및 시스템 라이브러리는 적절히 임포트되면 Kotlin 코드에서 사용할 수 있습니다:
- [정의 파일](native-definition-file.html)을 통해 라이브러리 임포트 구성
- [네이티브 언어 상호운용 컴파일 구성](/docs/multiplatform/multiplatform-configure-compilations.html)

**Swift 지원:** 순수 Swift 모듈은 아직 지원되지 않습니다. Swift 라이브러리는 API가 `@objc`로 Objective-C에 내보내진 경우에만 사용할 수 있습니다.

## Swift/Objective-C에서 Kotlin 사용

Swift/Objective-C에서 사용하려면 Kotlin 모듈을 프레임워크로 컴파일해야 합니다:
- [바이너리 선언](/docs/multiplatform/multiplatform-build-native-binaries.html#declare-binaries)
- 참조: [Kotlin Multiplatform 샘플 프로젝트](https://github.com/Kotlin/kmm-basic-sample)

### 가시성 제어

#### `@HiddenFromObjC`로 선언 숨기기

```kotlin
@HiddenFromObjC
fun internalFunction() { }
```

이 어노테이션은 다른 Kotlin 모듈에서는 계속 볼 수 있으면서 Objective-C와 Swift에서 선언을 숨깁니다. 또는 `internal` 수정자를 사용하여 컴파일 모듈 내에서 가시성을 제한합니다.

#### `@ShouldRefineInSwift` 사용

생성된 Objective-C 헤더에서 함수나 프로퍼티를 `swift_private`로 표시합니다:

```kotlin
@ShouldRefineInSwift
fun complexOperation() { }
```

이러한 선언은 `__` 접두사를 갖게 되어 Swift에서 보이지 않지만 Swift 친화적인 래퍼를 만드는 데 사용할 수 있습니다.

### `@ObjCName`으로 선언 이름 변경

```kotlin
@ObjCName(swiftName = "MySwiftArray")
class MyKotlinArray {
    @ObjCName("index")
    fun indexOf(@ObjCName("of") element: String): Int = TODO()
}

// Swift에서 사용
let array = MySwiftArray()
let index = array.index(of: "element")
```

### KDoc 주석으로 문서화

KDoc 주석은 Objective-C 주석으로 변환되어 생성된 프레임워크에 포함됩니다:

```kotlin
/**
 * 인수의 합을 출력합니다.
 * 합이 32비트 정수에 맞지 않는 경우를 적절히 처리합니다.
 */
fun printSum(a: Int, b: Int) = println(a.toLong() + b)
```

Objective-C 헤더를 생성합니다:

```objc
/**
 * 인수의 합을 출력합니다.
 * 합이 32비트 정수에 맞지 않는 경우를 적절히 처리합니다.
 */
+ (void)printSumA:(int32_t)a b:(int32_t)b __attribute__((swift_name("printSum(a:b:)")));
```

**KDoc 내보내기 비활성화** `build.gradle.kts`에서:

```kotlin
kotlin {
    iosArm64 {
        binaries {
            framework {
                baseName = "sdk"
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                exportKdoc.set(false)
            }
        }
    }
}
```

## 타입 매핑

| Kotlin | Swift | Objective-C | 참고 |
|--------|-------|-------------|------|
| `class` | `class` | `@interface` | |
| `interface` | `protocol` | `@protocol` | |
| 프로퍼티 | 프로퍼티 | 프로퍼티 | |
| 메서드 | 메서드 | 메서드 | |
| `enum class` | `class` | `@interface` | |
| `suspend` | `async`/`completionHandler:` | `completionHandler:` | |
| `@Throws fun` | `throws` | `error:(NSError**)error` | |
| 확장 | 확장 | 카테고리 멤버 | |
| `companion` 멤버 | 클래스 메서드/프로퍼티 | 클래스 메서드/프로퍼티 | |
| `null` | `nil` | `nil` | |
| 원시 타입 | 원시 타입/`NSNumber` | | |
| `Unit` | `Void` | `void` | |
| `String` | `String` | `NSString` | |
| `List` | `Array` | `NSArray` | |
| `MutableList` | `NSMutableArray` | `NSMutableArray` | |
| `Set` | `Set` | `NSSet` | |
| `Map` | `Dictionary` | `NSDictionary` | |
| 함수 타입 | 함수 타입 | 블록 포인터 타입 | |

### 클래스

**이름 변환:** Objective-C 프로토콜은 `Protocol` 접미사가 있는 인터페이스로 임포트됩니다 (예: `@protocol Foo` → `interface FooProtocol`). Kotlin 클래스 이름은 프레임워크 이름에 따라 Objective-C로 임포트될 때 접두사가 붙습니다.

**강한 링킹:** Kotlin에서 사용되는 Objective-C 클래스는 강하게 링크됩니다. Swift/Objective-C 래퍼를 사용하여 클래스 가용성을 확인하고 "Symbol not found" 충돌을 방지합니다.

### 이니셜라이저

- Swift/Objective-C 이니셜라이저는 Kotlin 생성자 또는 `create`라는 팩토리 메서드로 임포트됩니다
- 확장 이니셜라이저는 `create` 팩토리 메서드로 임포트됩니다 (Kotlin에는 확장 생성자가 없음)
- Kotlin 생성자는 Swift/Objective-C에서 이니셜라이저로 임포트됩니다

**예시:**

```kotlin
class ViewController : UIViewController {
    @OverrideInit
    constructor(coder: NSCoder) : super(coder)
}
```

### 최상위 함수 및 프로퍼티

최상위 Kotlin 함수는 생성된 클래스(파일당 하나)를 통해 접근할 수 있습니다:

```kotlin
// MyLibraryUtils.kt
package my.library
fun foo() {}
```

Swift에서 호출:

```swift
MyLibraryUtilsKt.foo()
```

### 오류 및 예외

모든 Kotlin 예외는 검사되지 않습니다; Swift에는 검사 오류만 있습니다. 던질 수 있는 메서드를 표시하려면 `@Throws`를 사용합니다:

```kotlin
@Throws(IOException::class)
fun readFile(path: String): String { }
```

**동작:**
- `@Throws`가 있는 비-`suspend` 함수: Objective-C에서 `NSError*` 생성 메서드로, Swift에서 `throws`로 매핑
- `suspend` 함수: 항상 완료 핸들러에 `NSError*/Error` 매개변수 있음
- `@Throws` 클래스와 일치하는 예외는 `NSError`로 전파됨; 그 외는 프로그램 종료
- Swift/Objective-C 오류 메서드는 아직 예외 던지기로 Kotlin에 임포트되지 않음

### 열거형

```kotlin
enum class Colors { RED, GREEN, BLUE }
```

Swift에서 접근:

```swift
Colors.red
Colors.green
Colors.blue

// 기본 케이스와 함께 switch 사용
switch color {
    case .red: print("It's red")
    case .green: print("It's green")
    case .blue: print("It's blue")
    default: fatalError("No such color")
}
```

### Suspend 함수

Kotlin `suspend` 함수는 다음으로 표현됩니다:
- **Objective-C:** 완료 핸들러 콜백이 있는 함수
- **Swift 5.5+:** `async` 함수 (실험적, 제한 있음; [KT-47610](https://youtrack.jetbrains.com/issue/KT-47610) 참조)

### 확장 및 카테고리 멤버

- Objective-C 카테고리 멤버와 Swift 확장은 Kotlin 확장으로 임포트됩니다
- Kotlin에서 오버라이드할 수 없음; 확장 이니셜라이저는 생성자로 사용 불가
- **예외 (Kotlin 1.8.20+):** NSView (AppKit) 및 UIView (UIKit) 카테고리 멤버는 클래스 멤버로 임포트됨 (오버라이드 가능)

다른 타입에 대한 Kotlin 확장은 수신자 매개변수가 있는 최상위 선언으로 처리됩니다:
- Kotlin `String`
- 컬렉션 타입
- `interface` 타입
- 원시 타입
- `inline` 클래스
- `Any` 타입
- 함수 타입
- Objective-C 클래스 및 프로토콜

### Kotlin 싱글톤

```kotlin
object MyObject {
    val x = "Some value"
}

class MyClass {
    companion object {
        val x = "Some value"
    }
}
```

Swift/Objective-C에서 접근:

```swift
MyObject.shared
MyObject.shared.x
MyClass.companion
MyClass.Companion.shared
```

### 원시 타입

Kotlin 원시 타입 박스는 특수 Swift/Objective-C 클래스로 매핑됩니다 (예: `kotlin.Int` → Swift에서 `KotlinInt`, Objective-C에서 `${prefix}Int`). 이들은 `NSNumber`에서 파생되지만 `NSNumber`로/에서 수동 캐스팅이 필요합니다.

### 문자열

Kotlin `String`은 Objective-C `NSString`으로 내보낸 후 Swift가 복사합니다. 오버헤드를 피하려면 Objective-C `NSString`으로 직접 접근합니다:

```swift
let nsString = map as NSString
```

**NSMutableString:** Kotlin에서 사용 불가; 모든 인스턴스는 Kotlin으로 전달될 때 복사됩니다.

### 컬렉션

**Kotlin → Objective-C → Swift:**

Swift는 Objective-C 동등물을 통해 Kotlin 컬렉션을 암묵적으로 변환합니다 (성능 비용). Objective-C 타입으로 명시적 캐스팅:

```kotlin
val map: Map<String, String>
```

```swift
// 암묵적 변환 피하기
let nsMap: NSDictionary = map as NSDictionary
(nsMap[key] as? NSString)?.length ?? 0

// 대신
map[key]?.count ?? 0  // 비싼 변환
```

**Swift → Objective-C → Kotlin:**

`NSMutableSet`과 `NSMutableDictionary`는 자동으로 변환되지 않습니다. Kotlin 컬렉션을 명시적으로 생성:

```swift
let mutableSet: KotlinMutableSet = /* ... */
```

### 함수 타입

Kotlin 함수 타입 객체는 Swift에서 클로저로, Objective-C에서 블록으로 변환됩니다. 함수 타입의 원시 타입은 박싱된 표현으로 매핑됩니다; `Unit` 반환 값은 `KotlinUnit` 싱글톤으로 매핑:

```kotlin
fun foo(block: (Int) -> Unit) { }
```

Swift 시그니처:

```swift
func foo(block: (KotlinInt) -> KotlinUnit)

// 다음으로 호출
foo { bar($0 as! Int32)
    return KotlinUnit()
}
```

### 제네릭

Objective-C는 클래스에 경량 제네릭을 지원합니다; Swift는 타입 정보를 위해 이를 임포트할 수 있습니다. 제네릭 정보는 변환 중에 손실될 수 있습니다.

**제한 사항:**
- 클래스에만 제네릭, 프로토콜이나 함수에는 아님
- **Null 가능성:** 제네릭 타입 매개변수는 Objective-C에서 기본적으로 nullable. 상한으로 non-nullable 강제:

```kotlin
class Sample<T : Any>() {  // Objective-C에서 non-nullable 강제
    fun myVal(): T
}
```

**헤더에서 제네릭 비활성화:**

```kotlin
binaries.framework {
    freeCompilerArgs += "-Xno-objc-generics"
}
```

### 전방 선언

`objcnames.classes` 및 `objcnames.protocols` 패키지를 사용하여 임포트:

```kotlin
import objcnames.protocols.ForwardDeclaredProtocolProtocol

fun test() {
    consumeProtocol(
        produceProtocol() as objcnames.protocols.ForwardDeclaredProtocolProtocol
    )
}
```

## 매핑된 타입 간 캐스팅

`as` 캐스트를 사용하여 Kotlin과 Swift/Objective-C 타입 간 변환:

```kotlin
@file:Suppress("CAST_NEVER_SUCCEEDS")
import platform.Foundation.*

val nsNumber = 42 as NSNumber
val nsArray = listOf(1, 2, 3) as NSArray
val nsString = "Hello" as NSString
val string = nsString as String
```

불가능해 보이는 캐스트에 대한 IDE 경고를 억제하려면 `@Suppress("CAST_NEVER_SUCCEEDS")`를 사용합니다.

## 서브클래싱

### Swift/Objective-C에서 Kotlin 클래스 및 인터페이스 서브클래싱

Kotlin 클래스와 인터페이스는 Swift/Objective-C에서 자유롭게 서브클래싱할 수 있습니다.

### Kotlin에서 Swift/Objective-C 클래스 및 프로토콜 서브클래싱

`final` Kotlin 클래스를 사용하여 서브클래싱합니다 (Swift/Objective-C 타입을 상속하는 비-`final` 클래스는 지원되지 않음):

```kotlin
class ViewController : UIViewController {
    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    override fun viewDidLoad() {
        super.viewDidLoad()
    }
}
```

**이니셜라이저 오버라이드:** 임포트된 이니셜라이저를 오버라이드하려면 Kotlin 생성자에 `@OverrideInit` 어노테이션을 사용합니다.

**충돌하는 시그니처:** Objective-C에서 충돌하는 오버로드를 무시하려면 `@ObjCSignatureOverride` 어노테이션을 사용합니다:

```kotlin
@ObjCSignatureOverride
class MyClass : ObjCBase()
```

**지정 이니셜라이저 검사 비활성화:** `.def` 파일에서:

```
disableDesignatedInitializerChecks = true
```

## 지원되지 않는 기능

다음은 생성된 프레임워크 헤더에 제대로 노출되지 않습니다:

- 인라인 클래스 (기본 원시 타입 또는 `id`로 매핑)
- 표준 Kotlin 컬렉션 인터페이스를 구현하는 사용자 정의 클래스
- Objective-C 클래스의 Kotlin 서브클래스

## 관련 리소스

- [Kotlin-Swift interopedia](https://github.com/kotlin-hands-on/kotlin-swift-interopedia) - Swift에서 Kotlin 사용 예시
- [Swift/Objective-C ARC와의 통합](native-arc-integration.html) - GC/ARC 통합 세부 사항
- [C와의 상호운용성](native-c-interop.html) - C 상호운용 예시

---

# Kotlin/Native 메모리 관리

> **원문:** https://kotlinlang.org/docs/native-memory-manager.html

## 개요

Kotlin/Native는 JVM, Go 및 기타 주류 기술과 유사한 현대적인 메모리 관리자를 사용하며 다음과 같은 주요 기능을 제공합니다:

- 객체는 모든 스레드에서 접근 가능한 공유 힙에 저장됩니다
- 주기적인 추적 가비지 컬렉션이 "루트"(로컬 및 전역 변수)에서 도달할 수 없는 객체를 수집합니다

## 가비지 컬렉터

### 작동 방식

GC는 다음과 같은 특성을 가진 **stop-the-world 마크 및 동시 스윕 컬렉터**로 기능합니다:

- 별도의 스레드에서 실행되며 메모리 압박 휴리스틱 또는 타이머가 트리거합니다
- 여러 스레드(애플리케이션 스레드, GC 스레드 및 선택적 마커 스레드)에서 마크 큐를 병렬로 처리합니다
- 애플리케이션 스레드는 기본적으로 객체 마킹 중에 일시 중지됩니다
- 약한 참조는 GC 일시 중지 시간을 최소화하기 위해 동시에 처리됩니다

### 수동으로 가비지 컬렉션 활성화

```kotlin
kotlin.native.internal.GC.collect()
```

이것은 가비지 컬렉션을 강제하고 완료될 때까지 기다립니다.

### GC 성능 모니터링

**`build.gradle.kts`에서 로깅 활성화:**

```
-Xruntime-logs=gc=info
```

로그는 `stderr`에 출력됩니다.

**Apple 플랫폼에서**, signpost와 함께 Xcode Instruments 사용:

1. `gradle.properties`에 설정:
   ```
   kotlin.native.binary.enableSafepointSignposts=true
   ```

2. Xcode 열기 → Product | Profile (Cmd + I)

3. `os_signpost` 템플릿 선택

4. 다음으로 구성:
   - Subsystem: `org.kotlinlang.native.runtime`
   - Category: `safepoint`

5. record를 클릭하여 그래프에서 GC 일시 중지를 파란색 블롭으로 시각화

### GC 성능 최적화

GC 일시 중지 시간을 줄이기 위해 **동시 마킹** (실험적) 활성화:

```
kotlin.native.binary.gc=cms
```

### 가비지 컬렉션 비활성화

권장되지 않지만 테스트 또는 단기 프로그램에 가능:

```
kotlin.native.binary.gc=noop
```

**경고**: 메모리 소비가 지속적으로 증가합니다. 시스템 메모리 고갈 위험이 있습니다.

### 병렬 마킹 비활성화

단일 스레드 마킹의 경우 (큰 힙에서 일시 중지 시간이 증가할 수 있음):

```
kotlin.native.binary.gcMarkSingleThreaded=true
```

## 메모리 소비

### 메모리 소비 모니터링

**메모리 누수 확인:**

```kotlin
import kotlin.native.internal.*
import kotlin.test.*

class Resource

val global = mutableListOf<Resource>()

@OptIn(ExperimentalStdlibApi::class)
fun getUsage(): Long {
    GC.collect()
    return GC.lastGCInfo!!.memoryUsageAfter["heap"]!!.totalObjectsSizeBytes
}

fun run() {
    global.add(Resource())
    global.clear()
}

@Test
fun test() {
    val before = getUsage()
    run()
    val after = getUsage()
    assertEquals(before, after)
}
```

**Apple 플랫폼에서 메모리 추적** - Xcode Instruments의 VM Tracker를 통해 (태깅이 활성화된 기본 할당자 필요).

### 메모리 소비 조정

#### 1. Kotlin 업데이트

메모리 관리자 개선을 위해 Kotlin을 최신 상태로 유지합니다.

#### 2. 할당자 페이징 비활성화

```
kotlin.native.binary.pagedAllocator=false
```

메모리는 페이지 대신 객체별로 예약됩니다. 엄격한 메모리 제한이나 시작 최적화에 유용합니다.

#### 3. Latin-1 문자열 인코딩 활성화

기본적으로 문자열은 UTF-16을 사용합니다 (문자당 2바이트). ASCII 문자에는 Latin-1 활성화 (1바이트):

```
kotlin.native.binary.latin1Strings=true
```

바이너리 크기와 메모리 소비를 줄입니다. Latin-1이 아닌 문자는 UTF-16으로 폴백합니다.

**참고**: 실험적 상태에서 `String.pin()`, `String.usePinned()`, `String.refTo()`가 덜 효율적입니다.

## 백그라운드에서 단위 테스트

모킹하지 않는 한 단위 테스트에서 `Dispatchers.Main`을 사용하지 마세요. `kotlinx-coroutines-test`의 `Dispatchers.setMain()`을 사용하거나 백그라운드 테스트 런처를 구현합니다:

```kotlin
package testlauncher

import platform.CoreFoundation.*
import kotlin.native.concurrent.*
import kotlin.native.internal.test.*
import kotlin.system.*

fun mainBackground(args: Array<String>) {
    val worker = Worker.start(name = "main-background")
    worker.execute(TransferMode.SAFE, { args.freeze() }) {
        val result = testLauncherEntryPoint(it)
        exitProcess(result)
    }
    CFRunLoopRun()
    error("CFRunLoopRun should never return")
}
```

다음으로 컴파일: `-e testlauncher.mainBackground`

## 메모리 관리 모범 사례

### 순환 참조 방지

순환 참조는 메모리 누수를 일으킬 수 있습니다. 약한 참조를 사용하여 순환을 끊습니다:

```kotlin
import kotlin.native.ref.WeakReference

class Node {
    var parent: WeakReference<Node>? = null
    var children: MutableList<Node> = mutableListOf()
}
```

### 대용량 객체 관리

대용량 객체를 오래 유지하면 메모리 압박이 증가합니다. 필요하지 않을 때 참조를 null로 설정합니다:

```kotlin
var largeData: ByteArray? = loadLargeData()
// 사용 후
largeData = null
// GC가 다음 사이클에서 메모리 회수 가능
```

### 네이티브 리소스 정리

네이티브 리소스(파일 핸들, 소켓 등)는 명시적으로 정리해야 합니다:

```kotlin
val file = fopen("data.txt", "r")
try {
    // 파일 사용
} finally {
    fclose(file)
}
```

## 다음 단계

- [레거시 메모리 관리자에서 마이그레이션](native-migration-guide.html)
- [Swift/Objective-C ARC 통합 세부사항 확인](native-arc-integration.html)
