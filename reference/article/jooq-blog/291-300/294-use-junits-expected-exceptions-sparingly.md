# JUnit의 expected 예외를 아껴서 사용하라

> 원문: https://blog.jooq.org/use-junits-expected-exceptions-sparingly/

이 글에서는 예외 테스트에 JUnit의 `@Test(expected = ...)` 어노테이션을 사용하는 것에 반대하며, 대신 명시적인 try-catch 블록이나 Java 8의 함수형 접근 방식을 권장한다.

## 코드 예제

원래 방식 (try-catch):
```java
@Test
public void testValueOfIntInvalid() {
    try {
        ubyte((UByte.MIN_VALUE) - 1);
        fail();
    }
    catch (NumberFormatException e) {}
    try {
        ubyte((UByte.MAX_VALUE) + 1);
        fail();
    }
    catch (NumberFormatException e) {}
}
```

어노테이션 방식 (비판 대상):
```java
@Test(expected = NumberFormatException.class)
public void testValueOfShortInvalidCase1() {
    ubyte((short) ((UByte.MIN_VALUE) - 1));
}

@Test(expected = NumberFormatException.class)
public void testValueOfShortInvalidCase2() {
    ubyte((short) ((UByte.MAX_VALUE) + 1));
}
```

검증이 추가된 향상된 try-catch:
```java
try {
    ubyte((UByte.MIN_VALUE) - 1);
    fail("실패 사유");
}
catch (NumberFormatException e) {
    assertEquals("some message", e.getMessage());
    assertNull(e.getCause());
    // ... 추가 단언문들
}
```

Java 8 함수형 인터페이스:
```java
@FunctionalInterface
public interface Executable {
    void execute() throws Exception;
}
```

JUnit Lambda 단언 메서드:
```java
public static void assertThrows(
    Class<? extends Throwable> expected,
    Executable executable) {
    expectThrows(expected, executable);
}

public static <T extends Throwable> T expectThrows(
    Class<T> expectedType,
    Executable executable) {
    try {
        executable.execute();
    }
    catch (Throwable actualException) {
        if (expectedType.isInstance(actualException)) {
            return (T) actualException;
        }
        else {
            String message = Assertions.format(
                expectedType.getName(),
                actualException.getClass().getName(),
                "unexpected exception type thrown;");
            throw new AssertionFailedError(message, actualException);
        }
    }
    throw new AssertionFailedError(
        String.format(
            "Expected %s to be thrown, but nothing was thrown.",
            expectedType.getName()));
}
```

Java 8 함수형 접근 방식 (권장):
```java
expectThrows(NumberFormatException.class, () ->
    ubyte((UByte.MIN_VALUE) - 1));
```

예외 검증 포함:
```java
Exception e = expectThrows(NumberFormatException.class, () ->
    ubyte((UByte.MIN_VALUE) - 1));
assertEquals("abc", e.getMessage());
```

## @Test(expected = ...)에 반대하는 네 가지 주요 논거

### 1. 코드 감소 효과 없음

의미적으로 동등한 정보가 두 접근 방식 모두에 존재한다: 테스트 대상 메서드 호출, 실패 메시지/메서드 이름, 그리고 예외 타입. 의미 있는 세부 사항을 고려하면 어느 버전도 인지 부하나 코드 줄 수를 진정으로 줄이지 못한다.

### 2. 리팩토링의 한계

어노테이션을 사용하면 예외 타입만 테스트할 수 있다는 제약이 생긴다. 개발자가 나중에 예외 메시지나 원인을 검증해야 할 경우, 전체 테스트 구조를 리팩토링해야 한다. try-catch 접근 방식은 기존 catch 블록 내에서 검증 로직을 간단히 추가할 수 있다.

### 3. 의미적 단위 테스트

원래 메서드 이름 `testValueOfIntInvalid()`는 하나의 의미적 단위를 설명한다: "일반적으로 잘못된 입력에 대한 UByte의 valueOf() 동작". 이를 별도의 테스트 메서드(`testValueOfShortInvalidCase1`, `testValueOfShortInvalidCase2`)로 분리하면 개념적 단위가 부적절하게 단편화되어, 논리적 일관성보다 테스트 제약 사항에 맞춰 API 설계를 결정하게 된다.

### 4. 제어 흐름에 부적합한 어노테이션

어노테이션의 적절한 용도는 네 가지로 식별된다:
- 처리 가능한 문서화 (예: `@Deprecated`)
- 커스텀 타입 수식자 (예: `@Override`)
- 관점 지향 프로그래밍 (예: `@Transactional`)

예외 동작 테스트는 제어 흐름 구조화에 해당하며, 이는 이러한 적절한 용도에 포함되지 않는다. 어노테이션은 경직되고 조합 불가능한 제어 메커니즘을 만드는 반면, 객체 지향이든 함수형이든 명시적 프로그래밍은 유연성과 명확성을 제공한다.

## 핵심 인용구

"테스트 때문에 내 코드에 설계나 의미적 변형을 강요하지 마라."

## 권장 해결책: Java 8

JUnit Lambda의 `Executable` 인터페이스를 사용한 함수형 접근 방식이 우수한 이점을 제공한다:

```java
expectThrows(NumberFormatException.class, () ->
    ubyte((UByte.MIN_VALUE) - 1));
```

이 방식은 try-catch의 장황함을 제거하면서도 예외 검증을 가능하게 한다:

```java
Exception e = expectThrows(NumberFormatException.class, () ->
    ubyte((UByte.MIN_VALUE) - 1));
assertEquals("abc", e.getMessage());
```

## 결론

"함수형 프로그래밍은 항상 어노테이션을 이긴다." JUnit 5의 `@Test` 어노테이션이 의도적으로 `expected` 속성을 제외한 것은 이러한 관점을 뒷받침한다. 명시적인 함수형 접근 방식은 어노테이션 기반 제약보다 더 깔끔하고, 유지보수가 용이하며, 유연한 예외 테스트를 제공한다.
