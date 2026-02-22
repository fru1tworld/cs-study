# Java 8 FunctionalInterface를 로컬 메서드로 (남)용하기

> 원문: https://blog.jooq.org/abusing-java-8-functionalinterfaces-as-local-methods/

Scala나 Ceylon 같은 언어들은 중첩 함수 또는 로컬 함수를 일반적인 관용구로 지원합니다. 예를 들어, Scala에서는 함수 내부에 함수를 정의할 수 있습니다:

```scala
def f() = {
  def g() = "a string!"
  g() + "– says g"
}
```

Java는 로컬 함수에 대한 네이티브 지원이 없지만, Java 8의 람다 표현식을 로컬 변수에 할당함으로써 이를 우회할 수 있는 방법을 제공합니다.

## Java로 변환하기

위의 Scala 예제를 Java 코드로 변환하면 다음과 같습니다:

```java
String f() {
    Supplier<String> g = () -> "a string!";
    return g.get() + "- says g";
}
```

## 실용적인 사용 사례: 테스트

더 실질적인 활용 예시가 jOOλ 유닛 테스트에 있습니다. 이 패턴은 여러 스트림 결합 연산에서 `Stream.close()` 의미론이 올바르게 동작하는지 검증합니다:

```java
@Test
public void testCloseCombineTwoSeqs() {
    // 두 개의 스트림을 받아 Seq를 반환하는 BiFunction을 테스트하는 로컬 함수
    Consumer<BiFunction<Stream<Integer>, Stream<Integer>, Seq<?>>> test = f -> {
        AtomicBoolean closed1 = new AtomicBoolean();
        AtomicBoolean closed2 = new AtomicBoolean();

        Stream s1 = Stream.of(1, 2).onClose(() -> closed1.set(true));
        Stream s2 = Stream.of(3).onClose(() -> closed2.set(true));

        try (Seq s3 = f.apply(s1, s2)) {
            s3.collect(Collectors.toList());
        }

        assertTrue(closed1.get());
        assertTrue(closed2.get());
    };

    // 다양한 스트림 결합 메서드에 대해 테스트 실행
    test.accept((s1, s2) -> seq(s1).concat(s2));
    test.accept((s1, s2) -> seq(s1).crossJoin(s2));
    test.accept((s1, s2) -> seq(s1).innerJoin(s2, (a, b) -> true));
    test.accept((s1, s2) -> seq(s1).leftOuterJoin(s2, (a, b) -> true));
    test.accept((s1, s2) -> seq(s1).rightOuterJoin(s2, (a, b) -> true));
}
```

로컬 함수 `test`는 두 개의 스트림을 받아 `Seq`를 반환하는 `BiFunction`을 인자로 받습니다. 이를 통해 여러 테스트 시나리오에서 코드를 재사용할 수 있습니다.

## private 메서드보다 나은 점

이 접근 방식은 private 메서드를 정의하는 것보다 더 편리합니다. 왜냐하면 로컬 스코프로 인해 테스트 함수가 단일 테스트 메서드 컨텍스트를 벗어나지 않기 때문입니다. 전통적인 Java에서는 private 메서드나 로컬 클래스가 필요했을 것인데, 둘 다 더 장황한 해결책입니다.

## 한계점

그러나 이 패턴에서는 표준 메서드에 비해 재귀 구현이 더 어렵다는 점을 유의해야 합니다. 람다 표현식에서는 자기 자신을 쉽게 참조할 수 없기 때문에 재귀적인 로컬 함수를 만들기가 까다롭습니다.

## 결론

Java 8의 함수형 인터페이스와 람다 표현식을 활용하면, 비록 완벽하지는 않지만 다른 언어들이 제공하는 중첩 함수와 유사한 기능을 구현할 수 있습니다. 특히 테스트 코드에서 반복되는 로직을 캡슐화할 때 매우 유용한 패턴입니다.
