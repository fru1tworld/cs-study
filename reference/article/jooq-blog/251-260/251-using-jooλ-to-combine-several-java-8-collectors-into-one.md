# jOOλ를 사용하여 여러 Java 8 Collector를 하나로 결합하기

> 원문: https://blog.jooq.org/using-joo%ce%bb-to-combine-several-java-8-collectors-into-one/

저는 최근 mykong의 글을 읽었습니다. 그 글에서는 Java 8 Stream을 사용하여 Map을 두 개의 List로 변환하는 방법, 즉 키 목록과 값 목록을 각각 추출하는 방법을 보여주었습니다.

```java
// 원본 접근 방식 - 맵을 두 번 순회함
List<Integer> result = map.entrySet().stream()
    .map(x -> x.getKey())
    .collect(Collectors.toList());

List<String> result2 = map.entrySet().stream()
    .map(x -> x.getValue())
    .collect(Collectors.toList());
```

위 코드는 동일한 Map을 두 번 순회합니다. 한 번은 키를 추출하기 위해, 또 한 번은 값을 추출하기 위해서입니다.

## 모든 문제에 Java 8 Stream을 억지로 끼워넣지 마세요

위의 예제는 Java 8 Stream을 모든 문제에 억지로 적용하려는 전형적인 사례입니다. 하지만 이 작업은 Stream 없이 훨씬 더 간단하고 빠르게 수행할 수 있습니다:

```java
List<Integer> result1 = new ArrayList<>(map.keySet());
List<String> result2 = new ArrayList<>(map.values());
```

이 방법이 더 간결하고, Stream 파이프라인에서 불필요한 객체 생성을 피할 수 있어 더 효율적입니다.

## jOOλ를 사용한 단일 패스 솔루션

만약 정말로 Stream을 사용하고 싶고, 동시에 컬렉션을 한 번만 순회하고 싶다면 어떻게 해야 할까요? jOOλ(jOOL) 라이브러리의 `Tuple.collectors()` 메서드를 사용하면 여러 Collector를 하나로 결합하여 단일 연산으로 처리할 수 있습니다:

```java
Tuple2<List<Integer>, List<String>> result =
map.entrySet()
    .stream()
    .collect(Tuple.collectors(
        Collectors.mapping(Entry::getKey, Collectors.toList()),
        Collectors.mapping(Entry::getValue, Collectors.toList())
    ));
```

이 코드는 Map을 단 한 번만 순회하면서 키 목록과 값 목록을 동시에 수집합니다. 결과는 두 개의 List를 담고 있는 `Tuple2` 객체입니다.

## jOOλ Seq를 사용한 더 간결한 방법

jOOλ의 `Seq`를 사용하면 훨씬 더 간결한 문법으로 같은 결과를 얻을 수 있습니다. `Seq`는 `collect()` 메서드에 여러 Collector를 직접 전달할 수 있는 문법적 편의 기능(syntax sugar)을 제공합니다:

```java
Tuple2<List<Integer>, List<String>> result =
Seq.seq(map)
   .collect(
        Collectors.mapping(Tuple2::v1, Collectors.toList()),
        Collectors.mapping(Tuple2::v2, Collectors.toList())
   );
```

`Seq.seq(map)`을 호출하면 Map의 엔트리들이 `Tuple2<K, V>` 스트림으로 변환됩니다. 여기서 `v1()`은 키를, `v2()`는 값을 반환합니다.

결과 출력:

```
([50, 20, 40, 10, 30], [dragonfruit, orange, watermelon, apple, banana])
```

## 결론

새로운 기능이 등장했다고 해서 모든 곳에 적용해야 하는 것은 아닙니다. Java 8 Stream API는 강력한 도구이지만, 때로는 전통적인 컬렉션 메서드가 더 간단하고 효율적인 해결책이 될 수 있습니다.

Stream을 정말로 사용해야 하는 상황이라면, jOOλ 같은 라이브러리가 Stream API의 한계를 극복하는 우아한 솔루션을 제공합니다. `Tuple.collectors()`를 사용하면 여러 Collector를 하나로 결합하여 데이터를 한 번만 순회하면서 여러 결과를 동시에 수집할 수 있습니다.

jOOλ 라이브러리는 GitHub에서 확인할 수 있습니다: https://github.com/jOOQ/jOOL
