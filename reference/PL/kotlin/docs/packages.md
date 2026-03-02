# Kotlin 패키지와 임포트

## 개요

소스 파일은 코드를 네임스페이스로 구성하기 위해 패키지 선언으로 시작할 수 있습니다.

## 패키지 선언

```kotlin
package org.example

fun printMessage() { /*...*/ }
class Message { /*...*/ }
```

소스 파일의 모든 내용(클래스, 함수 등)은 선언된 패키지에 속합니다. 위 예제에서:
- `printMessage()`의 전체 이름: `org.example.printMessage`
- `Message`의 전체 이름: `org.example.Message`

패키지가 지정되지 않으면 내용은 이름이 없는 기본 패키지에 속합니다.

---

## 기본 임포트

다음 패키지들은 모든 Kotlin 파일에 자동으로 임포트됩니다:

- `kotlin.*`
- `kotlin.annotation.*`
- `kotlin.collections.*`
- `kotlin.comparisons.*`
- `kotlin.io.*`
- `kotlin.ranges.*`
- `kotlin.sequences.*`
- `kotlin.text.*`

### 플랫폼별 임포트

**JVM:**
- `java.lang.*`
- `kotlin.jvm.*`

**JS:**
- `kotlin.js.*`

---

## 임포트 지시문

### 단일 이름 임포트

```kotlin
import org.example.Message  // Message는 이제 한정자 없이 접근 가능
```

### 와일드카드 임포트

```kotlin
import org.example.*  // 'org.example'의 모든 것이 접근 가능
```

### 이름 충돌 처리

```kotlin
import org.example.Message           // Message에 접근 가능
import org.test.Message as TestMessage  // TestMessage는 'org.test.Message'를 나타냄
```

### 임포트 가능한 선언

- 최상위 함수와 프로퍼티
- 객체 선언에서 선언된 함수와 프로퍼티
- 열거형 상수

---

## 최상위 선언의 가시성

최상위 선언이 `private`으로 표시되면 해당 선언은 선언된 파일 내에서만 private입니다. (자세한 내용은 가시성 수정자를 참조하세요.)
