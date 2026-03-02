# Kotlin this 표현식

## 개요

this 표현식은 Kotlin에서 현재 수신자를 나타냅니다. `this` 키워드는 컨텍스트에 따라 다른 스코프를 참조하는 데 사용됩니다.

## 사용 컨텍스트

### 클래스에서
클래스의 멤버에서 `this`는 해당 클래스의 현재 객체를 참조합니다.

### 확장 함수와 함수 리터럴에서
확장 함수나 수신자가 있는 함수 리터럴에서 `this`는 점의 왼쪽에 전달된 수신자 매개변수를 나타냅니다.

### 기본 동작
`this`에 한정자가 없으면 가장 안쪽 둘러싸는 스코프를 참조합니다.

---

## 한정된 this

외부 스코프 (클래스, 확장 함수, 또는 레이블이 지정된 수신자가 있는 함수 리터럴)에서 `this`에 접근하려면 `this@label` 구문을 사용합니다. 여기서 `@label`은 스코프의 레이블입니다.

### 예제:
```kotlin
class A { // 암시적 레이블 @A
    inner class B { // 암시적 레이블 @B
        fun Int.foo() { // 암시적 레이블 @foo
            val a = this@A // A의 this
            val b = this@B // B의 this
            val c = this // foo()의 수신자, Int
            val c1 = this@foo // foo()의 수신자, Int

            val funLit = lambda@ fun String.() {
                val d = this // funLit의 수신자, String
            }

            val funLit2 = { s: String ->
                // 둘러싸는 람다 표현식에 수신자가 없으므로
                // foo()의 수신자
                val d1 = this
            }
        }
    }
}
```

---

## 암시적 this

`this`에서 멤버 함수를 호출할 때 `this.` 부분을 생략할 수 있습니다. 그러나 같은 이름의 비멤버 함수가 존재하면 대신 호출될 수 있으므로 주의하세요.

### 예제:
```kotlin
fun main() {
    fun printLine() {
        println("Local function")
    }

    class A {
        fun printLine() {
            println("Member function")
        }

        fun invokePrintLine(omitThis: Boolean = false) {
            if (omitThis) printLine() else this.printLine()
        }
    }

    A().invokePrintLine() // Member function
    A().invokePrintLine(omitThis = true) // Local function
}
```
