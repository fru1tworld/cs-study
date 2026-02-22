# Java 8에 Iterable.stream()이 없다는 것은 정말 안타깝다

> 원문: https://blog.jooq.org/really-too-bad-that-java-8-doesnt-have-iterable-stream/

최근 Stack Overflow에서 이런 흥미로운 질문이 있었습니다: [왜 Iterable은 stream()과 parallelStream() 메서드를 제공하지 않는가?](http://stackoverflow.com/q/23114015/521799)

처음에는 `Iterable`을 `Stream`으로 변환하는 것을 간단하게 만드는 것이 직관적으로 보일 수 있습니다. 왜냐하면 이 둘은 90%의 사용 사례에서 거의 같은 것이기 때문입니다.

전문가 그룹은 Stream API를 병렬 처리가 가능하도록 만드는 데 강한 초점을 맞췄지만, 매일 Java로 작업하는 사람이라면 `Stream`이 순차적인 형태에서 가장 유용하다는 것을 즉시 알아챌 것입니다.

그리고 `Iterable`은 바로 그것입니다 - 병렬화에 대한 보장이 없는 순차적 스트림입니다.

사실, `Iterable`의 하위 타입들은 그러한 메서드를 가지고 있습니다. `Collection` 인터페이스를 보면:

```java
default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
}
```

`Iterable`을 `Stream`으로 변환하는 것은 매우 쉽습니다. 아래와 같이 작성하면 됩니다:

```java
Stream s = StreamSupport.stream(iterable.spliterator(), false);
```

Brian Goetz 자신이 위의 Stack Overflow 질문에 답변을 달았습니다. 그 이유는 다음과 같습니다:

> 이것은 전문가 그룹에서 논의되었지만, 주된 이유는 `Iterable`이 너무 일반적이기 때문입니다. `Iterable`의 유일한 구조는 `Iterator<T> iterator()`이지만, 일부 `Iterable`의 시맨틱은 `Stream`보다 `IntStream`을 반환하는 것이 더 적절할 수 있습니다.

저는 이것이 매우 근거 박약한 이유라고 생각합니다.

비록 순차적 처리가 JDK 8 API의 주요 설계 목표는 아니었지만, 그것은 정말로 우리 모두에게 가장 큰 이점입니다. 이제 동의하든 그렇지 않든(그리고 아마도 JDK 9에서 이 누락된 메서드가 `Iterable` API에 추가될 것이라고 확신합니다), 위와 같이 직접 메서드를 작성할 수 있습니다:

```java
public static <T> Stream<T> stream(Iterable<T> i) {
    return StreamSupport.stream(i.spliterator(), false);
}
```

혹은 jOOL을 사용하여 `Seq.seq(iterable)`을 호출할 수도 있습니다.

그리고 솔직히 말해서, 99%의 사용 사례에서 반환되기를 원하는 타입은 `IntStream` 타입이 아닌 `Stream` 타입입니다. 그리고 누군가가 정말로 `IntStream`을 원한다면(이것은 오래된 레거시 Java API의 많은 나쁜 것들보다 훨씬 더 모호한 이유입니다), 그들은 단지 `intStream()` 메서드를 선언하면 됩니다. 결국, 만약 누군가가 정말로 `int` 원시 타입으로 작업하면서 `Iterable<Integer>`를 작성할 정도로 "미쳤다면", 그들은 아마도 작은 우회로를 받아들일 것입니다.

## jOOQ에서의 영향

이것이 jOOQ에 미치는 영향을 예로 들어보겠습니다. jOOQ의 `ResultQuery` 타입은 `Iterable`을 구현하므로, foreach 루프에서 직접 사용할 수 있습니다:

```java
for (MyTableRecord rec : DSL
    .using(configuration)
    .selectFrom(MY_TABLE)
    .where(MY_TABLE.ID.eq(1))) {

    // rec로 무언가를 수행
}
```

이것은 정말 편리합니다. 하지만 Stream API를 직접 사용하려면 먼저 `.fetch()`를 호출하여 데이터를 즉시 가져와야 합니다. 이렇게 하면 지연 평가 시맨틱이 깨집니다:

```java
DSL.using(configuration)
   .selectFrom(MY_TABLE)
   .where(MY_TABLE.ID.eq(1))
   .fetch()  // 즉시 평가 강제
   .stream()
   .map(...)
   .collect(...);
```

`Iterable`에 `stream()` 메서드가 있었다면, 결과를 지연 스트리밍하면서 Stream API의 모든 기능을 활용할 수 있었을 것입니다.

## 결론

다음 세 가지는 정말 귀찮은 부분입니다:

1. 다소 모호한 `StreamSupport` 타입을 기억해야 합니다. `Stream`에서 직접 사용할 수 없습니다.
2. `Iterator`와 `Spliterator`의 차이를 이해해야 합니다 - 대부분의 단순한 사용 사례에서는 불필요한 인지적 부담입니다.
3. 명시적으로 `false`를 전달하여 "병렬화 없음"을 나타내야 합니다 - 이미 암시된 정보를 반복하는 것입니다.

이 모든 마찰이 `Iterable`에 간단한 default 메서드 하나만 추가하면 해결될 수 있었습니다. Java 8의 설계 결정이 이 부분에서는 아쉬움을 남깁니다.
