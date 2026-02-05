# Java 8 금요일: 더 나은 예외 처리

> 원문: https://blog.jooq.org/java-8-friday-better-exceptions/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 우리는 jOOQ의 유창한 API와 쿼리 DSL을 정말 좋아하기 때문에, Java 8이 우리 생태계에 가져올 모든 것들에 대해 매우 흥분하고 있습니다.

## Java 8 금요일

매주 금요일마다 Java 8의 새로운 튜토리얼 스타일 블로그 포스트를 게시하며, 람다 표현식, 확장 메서드 및 기타 훌륭한 기능들을 활용하는 여러 가지 방법을 보여드립니다. 소스 코드는 GitHub에서 찾을 수 있습니다.

## 더 나은 예외 처리

최근에 저는 새로운 예외 테스트 메서드를 제안하는 흥미로운 JUnit GitHub 이슈 #706을 발견했습니다:

```java
ExpectedException#expect(Throwable, Callable)
```

한 제안에서는 예외를 가로채서 호출자에게 반환하는 인터셉터를 만드는 것이었습니다:

```java
assertEquals(Exception.class,
    thrown(() -> foo()).getClass());
assertEquals("yikes!",
    thrown(() -> foo()).getMessage());
```

## 제안된 해결책

저는 throwable을 위한 커스텀 함수형 인터페이스를 제안합니다:

```java
@FunctionalInterface
interface ThrowableRunnable {
    void run() throws Throwable;
}
```

그런 다음 두 개의 단언(assertion) 메서드를 작성할 수 있습니다:

```java
// 예외가 발생하는지만 확인하는 단순한 버전
static void assertThrows(
    Class<? extends Throwable> throwable,
    ThrowableRunnable runnable
) {
    assertThrows(throwable, runnable, t -> {});
}

// 예외를 추가로 검사할 수 있는 확장 버전
static void assertThrows(
    Class<? extends Throwable> throwable,
    ThrowableRunnable runnable,
    Consumer<Throwable> exceptionConsumer
) {
    boolean fail = false;
    try {
        runnable.run();
        fail = true;
    }
    catch (Throwable t) {
        if (!throwable.isInstance(t))
            Assert.fail("잘못된 예외 타입");
        exceptionConsumer.accept(t);
    }
    if (fail)
        Assert.fail("예외가 발생하지 않았습니다");
}
```

## 사용 예제

이제 이 유틸리티 메서드들을 다음과 같이 사용할 수 있습니다:

```java
// 단순히 예외가 발생하는지 확인
assertThrows(Exception.class,
    () -> { throw new Exception(); });

// 예외가 발생하는지 확인하고 메시지도 검증
assertThrows(Exception.class,
    () -> { throw new Exception("Message"); },
    e -> assertEquals("Message", e.getMessage()));
```

## 예외 무시 헬퍼

때로는 예외를 "삼키고" 싶을 때가 있습니다. 이를 위한 헬퍼 메서드도 작성할 수 있습니다:

```java
// 예외를 단순히 무시하는 버전
static void withExceptions(
    ThrowableRunnable runnable
) {
    withExceptions(runnable, t -> {});
}

// 예외가 발생하면 컨슈머에게 전달하는 버전
static void withExceptions(
    ThrowableRunnable runnable,
    Consumer<Throwable> exceptionConsumer
) {
    try {
        runnable.run();
    }
    catch (Throwable t) {
        exceptionConsumer.accept(t);
    }
}
```

이러한 유틸리티 메서드들은 편리함을 제공하지만, 전통적인 try-catch-finally 블록을 완전히 대체하지는 못한다는 점에 유의하세요. 적절한 예외 타이핑과 try-with-resources 지원이 부족하기 때문입니다.

## 결론

Java 8의 람다 표현식을 활용하면 예외 처리 테스트 코드를 더욱 간결하고 읽기 쉽게 작성할 수 있습니다. 앞으로 더 많은 예제를 보려면 Java 8 금요일 시리즈를 계속 지켜봐 주세요!
