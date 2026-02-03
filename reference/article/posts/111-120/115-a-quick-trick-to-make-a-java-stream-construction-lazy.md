# Java Stream 생성을 지연시키는 빠른 트릭

> 원문: https://blog.jooq.org/a-quick-trick-to-make-a-java-stream-construction-lazy/

## 서론

Stream API의 가장 큰 장점 중 하나는 지연 평가(laziness)입니다. 전체 파이프라인은 지연 방식으로 구성되어, SQL 실행 계획과 유사하게 일련의 명령어 집합으로 저장됩니다. 터미널 연산을 호출할 때만 파이프라인이 시작됩니다. 이때도 여전히 지연 방식이 적용되어 일부 연산은 단락(short circuit)될 수 있습니다. 일부 서드파티 라이브러리는 완전히 지연되지 않는 스트림을 생성합니다. 예를 들어, jOOQ는 버전 3.12까지 `ResultQuery.stream()`을 호출하면 스트림이 이후에 소비되는지 여부와 관계없이 SQL 쿼리를 즉시 실행했습니다:

```java
try (var stream = ctx.select(T.A, T.B).from(T).stream()) {
    // 여기서 스트림을 소비하지 않음
}
```

이것은 아마도 클라이언트 코드의 버그일 수 있지만, 이 경우 구문을 실행하지 않는 것이 여전히 유용한 기능일 수 있습니다. 물론 예외적으로, 쿼리에 `FOR UPDATE` 절이 포함된 경우에는 사용자가 결과에 관심이 없다면 아마도 `Query.execute()`를 대신 사용할 것입니다.

지연 평가가 도움이 되는 더 흥미로운 예시는 쿼리를 즉시 실행하고 싶지 않은 경우입니다. 아직 실행하기에 적절하지 않은 스레드에 있을 수도 있기 때문입니다. 또는 가능한 모든 예외가 결과가 소비되는 곳, 즉 터미널 연산이 호출되는 곳에서 발생하기를 원할 수도 있습니다. 예를 들어:

```java
try (var stream = ctx.select(T.A, T.B).from(T).stream()) {
    consumeElsewhere(stream);
}
```

그리고 다음과 같이:

```java
public void consumeElsewhere(Stream<? extends Record> stream) {
    runOnSomeOtherThread(() -> {
        stream.map(r -> someMapping(r))
              .forEach(r -> someConsumer(r));
    });
}
```

## 해결책

이 문제는 jOOQ 3.13에서 수정되고 있지만, 이전 버전의 jOOQ에 묶여 있거나 다른 라이브러리가 같은 동작을 할 수도 있습니다. 다행히 서드파티 스트림을 빠르게 "지연"시키는 쉬운 트릭이 있습니다. flatMap을 사용하세요! 대신 다음과 같이 작성하면 됩니다:

```java
try (var stream = Stream.of(1).flatMap(
    i -> ctx.select(T.A, T.B).from(T).stream()
)) {
    consumeElsewhere(stream);
}
```

다음의 간단한 테스트는 이제 `stream()`이 지연 방식으로 생성됨을 보여줍니다:

```java
public class LazyStream {

    @Test(expected = RuntimeException.class)
    public void testEager() {
        Stream<String> stream = stream();
    }

    @Test
    public void testLazyNoTerminalOp() {
        Stream<String> stream = Stream.of(1).flatMap(i -> stream());
    }

    @Test(expected = RuntimeException.class)
    public void testLazyTerminalOp() {
        Optional<String> result = stream().findAny();
    }

    public Stream<String> stream() {
        String[] array = { "heavy", "array", "creation" };

        // 발생할 수 있는 리소스 문제
        if (true)
            throw new RuntimeException();

        return Stream.of(array);
    }
}
```

## 주의 사항

사용 중인 JDK 버전에 따라 위의 접근 방식에는 자체적으로 심각한 문제가 있을 수 있습니다. 예를 들어, JDK 8의 이전 버전에서는 `flatMap()` 자체가 전혀 지연되지 않을 수 있습니다! JDK 8u222를 포함한 더 최신 버전의 JDK에서는 해당 문제가 수정되었습니다.
