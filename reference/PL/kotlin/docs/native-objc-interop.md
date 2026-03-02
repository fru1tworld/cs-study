# Swift/Objective-C와의 상호운용성

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

Objective-C는 클래스에 대해 경량 제네릭을 지원합니다; Swift는 타입 정보를 위해 이를 임포트할 수 있습니다. 제네릭 정보는 변환 중에 손실될 수 있습니다.

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
