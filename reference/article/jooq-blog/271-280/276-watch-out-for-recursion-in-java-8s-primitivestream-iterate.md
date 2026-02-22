# Java 8의 [Primitive]Stream.iterate()에서 재귀를 주의하라

> 원문: https://blog.jooq.org/watch-out-for-recursion-in-java-8s-primitivestream-iterate/

Java 8의 `Stream.iterate()`와 `LongStream.iterate()` 사이에는 재귀적인 코드에서 예상치 못한 동작을 유발하는 중요한 구현 차이가 있습니다.

## 문제 상황

참조 타입 스트림은 재귀적 반복에서 정상적으로 동작합니다:

```java
public static Stream<Long> longs() {
    return Stream.iterate(1L, i ->
        1L + longs().skip(i - 1L)
                    .findFirst()
                    .get());
}
```

그러나 원시 타입 스트림은 `StackOverflowError`와 함께 실패합니다:

```java
public static LongStream longs() {
    return LongStream.iterate(1L, i ->
        1L + longs().skip(i - 1L)
                    .findFirst()
                    .getAsLong());
}
```

## 근본 원인

참조 타입 이터레이터는 먼저 시드(seed) 값을 반환한 후, 그 다음에 이전 값에 반복 함수를 적용하여 반복을 진행합니다.

반면에, 원시 타입 스트림 이터레이터는 한 값을 미리 가져옵니다(pre-fetch). 즉, 초기화 시점에 즉시 함수를 적용합니다.

## 결과

이 미리 가져오기(pre-fetching)는 두 가지 문제를 야기합니다:

1. 비용이 많이 드는 반복 함수의 최적화 문제: 반복 함수가 비용이 많이 드는 경우, 미리 가져오기로 인해 불필요한 계산이 발생할 수 있습니다.

2. 재귀적 이터레이터 사용 시 무한 재귀: 재귀적으로 이터레이터를 사용하면 무한 재귀가 발생합니다.

## 해결책

원시 타입 스트림의 이 메서드에서는 재귀를 피하세요.

이 문제는 JDK 9에서 기능 개선(버그 JDK-8072727)의 부수 효과로 수정되었습니다.
