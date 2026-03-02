# 봉인 클래스와 인터페이스

## 개요

봉인 클래스와 인터페이스는 클래스 계층의 제어된 상속을 제공합니다. 봉인 클래스의 모든 직접 서브클래스는 컴파일 시점에 알려져 있습니다. 봉인 클래스가 정의된 모듈과 패키지 외부에서는 다른 서브클래스가 나타날 수 없습니다.

**주요 이점:**
- 미리 정의된 유한한 서브클래스로 클래스 상속 제한
- 패턴 매칭과 상태 관리를 위한 타입 안전 설계
- 라이브러리를 위한 견고한 공개 API

## 봉인 클래스 또는 인터페이스 선언

`sealed` 수정자를 사용합니다:

```kotlin
// 봉인 인터페이스 생성
sealed interface Error

// 봉인 인터페이스 Error를 구현하는 봉인 클래스 생성
sealed class IOError(): Error

// 봉인 클래스 'IOError'를 확장하는 서브클래스 정의
class FileReadError(val file: File): IOError()
class DatabaseError(val source: DataSource): IOError()

// 'Error' 봉인 인터페이스를 구현하는 싱글톤 객체 생성
object RuntimeError : Error
```

### 생성자

봉인 클래스는 항상 추상이며 직접 인스턴스화할 수 없습니다. 서브클래스를 위한 생성자를 포함할 수 있습니다:

```kotlin
sealed class Error(val message: String) {
    class NetworkError : Error("Network failure")
    class DatabaseError : Error("Database cannot be reached")
    class UnknownError : Error("An unknown error has occurred")
}

fun main() {
    val errors = listOf(Error.NetworkError(), Error.DatabaseError(), Error.UnknownError())
    errors.forEach { println(it.message) }
}
// 출력:
// Network failure
// Database cannot be reached
// An unknown error has occurred
```

생성자는 `protected` (기본값) 또는 `private` 가시성만 가질 수 있습니다:

```kotlin
sealed class IOError {
    constructor() { /*...*/ }  // 기본적으로 protected
    private constructor(description: String): this() { /*...*/ }
    // public/internal 생성자는 허용되지 않음
}
```

## 상속 규칙

**직접 서브클래스**는 반드시:
- 같은 패키지에 선언
- 최상위 또는 명명된 클래스, 인터페이스, 객체 내부에 중첩
- 적절한 한정 이름 (지역이나 익명 아님)

**제한사항:**
- `enum` 클래스는 봉인 클래스를 확장할 수 없지만 봉인 인터페이스는 구현 가능
- 간접 서브클래스에는 제한 없음

```kotlin
sealed interface Error

// 봉인 인터페이스를 구현하는 enum (허용)
enum class ErrorType : Error {
    FILE_ERROR, DATABASE_ERROR
}

// 봉인 인터페이스를 확장하는 봉인 클래스 (같은 패키지 내에서만 확장 가능)
sealed class IOError(): Error

// 봉인 인터페이스를 확장하는 open 클래스 (어디서든 확장 가능)
open class CustomError(): Error
```

### 멀티플랫폼 프로젝트

직접 서브클래스는 같은 소스 세트에 있어야 합니다. 봉인 클래스가 `expect`와 `actual` 수정자를 사용하면 두 버전 모두 각각의 소스 세트에서 서브클래스를 가질 수 있습니다.

## when 표현식과 봉인 클래스 사용

핵심 이점: **`else` 절 없이 완전한 타입 검사**

```kotlin
sealed class Error {
    class FileReadError(val file: String): Error()
    class DatabaseError(val source: String): Error()
    object RuntimeError : Error()
}

fun log(e: Error) = when(e) {
    is Error.FileReadError -> println("Error while reading file ${e.file}")
    is Error.DatabaseError -> println("Error while reading from database ${e.source}")
    Error.RuntimeError -> println("Runtime error")
    // else 절이 필요 없음 - 모든 케이스 커버!
}

fun main() {
    val errors = listOf(
        Error.FileReadError("example.txt"),
        Error.DatabaseError("usersDatabase"),
        Error.RuntimeError
    )
    errors.forEach { log(it) }
}
```

## 사용 사례 시나리오

### UI 애플리케이션의 상태 관리

```kotlin
sealed class UIState {
    data object Loading : UIState()
    data class Success(val data: String) : UIState()
    data class Error(val exception: Exception) : UIState()
}

fun updateUI(state: UIState) {
    when (state) {
        is UIState.Loading -> showLoadingIndicator()
        is UIState.Success -> showData(state.data)
        is UIState.Error -> showError(state.exception)
    }
}
```

### 결제 방법 처리

```kotlin
sealed class Payment {
    data class CreditCard(val number: String, val expiryDate: String) : Payment()
    data class PayPal(val email: String) : Payment()
    data object Cash : Payment()
}

fun processPayment(payment: Payment) {
    when (payment) {
        is Payment.CreditCard -> processCreditCardPayment(payment.number, payment.expiryDate)
        is Payment.PayPal -> processPayPalPayment(payment.email)
        is Payment.Cash -> processCashPayment()
    }
}
```

### API 요청-응답 처리

```kotlin
sealed interface ApiRequest

data class LoginRequest(val username: String, val password: String) : ApiRequest
object LogoutRequest : ApiRequest

sealed class ApiResponse {
    data class UserSuccess(val user: UserData) : ApiResponse()
    data object UserNotFound : ApiResponse()
    data class Error(val message: String) : ApiResponse()
}

data class UserData(val userId: String, val name: String, val email: String)

fun handleRequest(request: ApiRequest): ApiResponse {
    return when (request) {
        is LoginRequest -> {
            if (isValidUser(request.username, request.password)) {
                ApiResponse.UserSuccess(UserData("userId", "userName", "userEmail"))
            } else {
                ApiResponse.Error("Invalid username or password")
            }
        }
        is LogoutRequest -> ApiResponse.UserSuccess(UserData("userId", "userName", "userEmail"))
    }
}
```

## 요약

봉인 클래스가 제공하는 것:
- 제어된 타입 안전 상속
- 완전한 `when` 표현식 검사
- 명확한 API 계약
- 더 나은 코드 유지보수성
