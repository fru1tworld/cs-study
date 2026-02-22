# Java에서 Checked Exception을 Runtime Exception처럼 던지기

> 원문: https://blog.jooq.org/throw-checked-exceptions-like-runtime-exceptions-in-java/

2012년 9월 14일 (2012년 9월 25일 업데이트)

이 글에서는 Java에서 `throws` 절이나 `catch` 블록 없이 checked exception을 던지는 기법을 소개합니다. 이 기법은 제네릭 타입 소거(generic type erasure)를 활용하여 Java의 checked exception 컴파일 타임 검사를 우회합니다.

## 코드 예제

다음 코드를 살펴보겠습니다:

```java
public class Test {
    // 여기에 throws 절이 없음
    public static void main(String[] args) {
        doThrow(new SQLException());
    }

    static void doThrow(Exception e) {
        Test.<RuntimeException> doThrow0(e);
    }

    @SuppressWarnings("unchecked")
    static <E extends Exception> void doThrow0(Exception e) throws E {
        throw (E) e;
    }
}
```

## 작동 원리

제네릭 타입 소거로 인해 컴파일러는 "실제로는 컴파일되지 않아야 할" 코드를 생성합니다. `doThrow0()` 메서드는 제네릭 예외 타입 `E`를 던진다고 선언하는데, 호출 지점에서 타입 소거로 인해 컴파일러는 이를 `RuntimeException`으로 취급합니다.

핵심 포인트:
- `doThrow()` 메서드는 제네릭 `doThrow0()` 메서드를 호출합니다
- `doThrow0()`는 타입 파라미터 `<E extends Exception>`을 사용합니다
- `(E) e` 캐스트는 타입 소거를 이용하여 checked exception을 던집니다

## 바이트코드 분석

생성된 바이트코드를 보면 `doThrow()`가 어떤 예외 처리 선언 없이 `doThrow0()`를 호출한다는 것을 알 수 있습니다. 바이트코드는 JVM이 불평 없이 `athrow` 명령어를 실행한다는 것을 보여줍니다. JVM은 던져진 checked exception에 대해 아무런 문제도 제기하지 않습니다.

## 핵심 통찰

이 기법은 중요한 사실을 드러냅니다: checked exception과 unchecked exception은 단순한 문법적 설탕(syntactic sugar)에 불과합니다. 이 구분은 JVM 바이트코드 수준이 아닌 컴파일러 수준에서만 존재합니다. JVM 자체는 checked exception에 대한 개념이 없으며, 이는 순수하게 Java 언어의 컴파일 타임 기능입니다.

## 의의

이 기법을 통해 우리는 Java의 타입 시스템이 제네릭과 타입 소거를 통해 우회될 수 있다는 것을 알 수 있습니다. 물론 이 기법을 실제 프로덕션 코드에서 남용하는 것은 권장되지 않지만, Java의 예외 처리 메커니즘과 JVM의 동작 방식을 이해하는 데 있어 귀중한 통찰을 제공합니다.
