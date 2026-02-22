# Java 8 스트림은 진정으로 지연 평가되는가? 완전히는 아니다!

> 원문: https://blog.jooq.org/are-java-8-streams-truly-lazy-not-completely/

*알림: 이 이슈는 Java 8(8u222)에서 수정되었습니다. Zheka Kozlov의 댓글에 감사드립니다.*

최근 글에서 저는 프로그래머들이 스트림을 사용할 때 항상 "필터 먼저, 맵 나중에" 전략을 적용해야 한다고 보여드렸습니다.

제가 제공한 예제는 다음과 같았습니다:

```java
hugeCollection
    .stream()
    .limit(2)
    .map(e -> superExpensiveMapping(e))
    .collect(Collectors.toList());
```

이 경우, `limit()` 연산이 필터링을 구현하며, 이는 매핑 전에 수행되어야 합니다. 여러 독자들이 이 경우 `limit()`와 `map()` 연산의 순서는 중요하지 않다고 정확하게 언급했는데, 왜냐하면 Java 8 스트림 API에서 대부분의 연산은 지연(lazy) 평가되기 때문입니다.

다시 말해: `collect()` 터미널 연산은 스트림으로부터 값을 지연 방식으로 가져오고, `limit(5)` 연산이 끝에 도달하면 `map()`이 앞에 오든 뒤에 오든 상관없이 더 이상 새로운 값을 생성하지 않습니다. 이는 다음과 같이 쉽게 증명할 수 있습니다:

```java
import java.util.stream.Stream;

public class LazyStream {
    public static void main(String[] args) {
        Stream.iterate(0, i -> i + 1)
              .map(i -> i + 1)
              .peek(i -> System.out.println("Map: " + i))
              .limit(5)
              .forEach(i -> {});

        System.out.println();
        System.out.println();

        Stream.iterate(0, i -> i + 1)
              .limit(5)
              .map(i -> i + 1)
              .peek(i -> System.out.println("Map: " + i))
              .forEach(i -> {});
    }
}
```

출력 결과는 다음과 같습니다:

```
Map: 1
Map: 2
Map: 3
Map: 4
Map: 5

Map: 1
Map: 2
Map: 3
Map: 4
Map: 5
```

## 하지만 항상 그런 것은 아닙니다!

이 최적화는 구현 세부 사항이며, 일반적으로 이러한 최적화에 의존하지 않고 "필터 먼저, 맵 나중에" 규칙을 철저히 적용하는 것이 현명합니다. 특히, Java 8의 `flatMap()` 구현은 지연 평가되지 않습니다.

```java
import java.util.stream.Stream;

public class LazyStream {
    public static void main(String[] args) {
        Stream.iterate(0, i -> i + 1)
              .flatMap(i -> Stream.of(i, i, i, i))
              .map(i -> i + 1)
              .peek(i -> System.out.println("Map: " + i))
              .limit(5)
              .forEach(i -> {});

        System.out.println();
        System.out.println();

        Stream.iterate(0, i -> i + 1)
              .flatMap(i -> Stream.of(i, i, i, i))
              .limit(5)
              .map(i -> i + 1)
              .peek(i -> System.out.println("Map: " + i))
              .forEach(i -> {});
    }
}
```

결과는 이제 다음과 같습니다:

```
Map: 1
Map: 1
Map: 1
Map: 1
Map: 2
Map: 2
Map: 2
Map: 2

Map: 1
Map: 1
Map: 1
Map: 1
Map: 2
```

첫 번째 스트림 파이프라인은 limit을 적용하기 전에 flatMap된 8개의 모든 값을 매핑하는 반면, 두 번째 스트림 파이프라인은 먼저 스트림을 5개 요소로 제한한 다음 그것들만 매핑합니다. 이유는 `flatMap()` 구현에 있습니다:

```java
// ReferencePipeline.flatMap() 내부
try (Stream<? extends R> result = mapper.apply(u)) {
    if (result != null)
        result.sequential().forEach(downstream);
}
```

보시다시피, `flatMap()` 연산의 결과는 터미널 `forEach()` 연산으로 즉시(eager) 소비되며, 우리의 경우 항상 네 개의 값을 모두 생성하여 다음 연산으로 보냅니다. 따라서 `flatMap()`은 지연 평가되지 않으며, 그 다음 연산은 모든 결과를 받게 됩니다. 이는 Java 8에 해당됩니다. 물론 미래의 Java 버전에서는 이것이 개선될 수 있습니다. 우리는 먼저 필터링하고, 나중에 매핑하는 것이 좋겠습니다.

## 업데이트: JDK 10에서 flatMap()이 수정됨

Tagir Valeev에게 수정이 예정되어 있다고 알려주셔서 감사합니다:

> 참고로 flatMap은 Java 10에서 수정됩니다.
>
> — Tagir Valeev (@tagir_valeev) 2018년 1월 25일

관련 링크: https://bugs.openjdk.java.net/browse/JDK-8075939 http://hg.openjdk.java.net/jdk/jdk10/rev/fca88bbbafb9

그러나 Stream.iterator()를 사용할 때 여전히 버그가 있습니다: https://bugs.openjdk.java.net/browse/JDK-8267359
