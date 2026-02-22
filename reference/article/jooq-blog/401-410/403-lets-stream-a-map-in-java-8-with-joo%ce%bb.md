# jOOλ로 Java 8에서 Map을 스트림하자

> 원문: https://blog.jooq.org/lets-stream-a-map-in-java-8-with-joo%ce%bb/

Java 8에서는 편리하게 다음과 같이 할 수 있을 것이라고 기대할 수 있습니다:

```java
map.stream()
   .filter(...)
   .map(...)
   .collect(...);
```

## 확인된 주요 문제

Java 8의 `Map` 인터페이스에는 편리한 `stream()` 메서드가 없습니다. 개발자들은 다음과 같은 것을 기대할 것입니다:

```java
public interface Map<K, V> {
    default Stream<Entry<K, V>> stream() {
        return entrySet().stream();
    }
}
```

하지만 이 메서드는 JDK에 존재하지 않습니다.

## 부재의 이유

이 기사에서는 몇 가지 가능한 설명을 제시합니다:

- "entrySet()이 keySet()이나 values()보다 선택되어야 할 명확한 선호도가 없다"
- Map은 기술적으로 Collection이 아니며 Iterable을 구현하지 않는다
- 이것은 Java 8 언어 발전 그룹의 설계 목표가 아니었다
- 시간 제약으로 구현이 방해되었다

## 증거로서의 forEach() 메서드

저자는 `Map.forEach()`가 Map 스트리밍이 왜 중요한지를 보여준다고 주장합니다. 이 메서드는 표준 `Consumer`가 아닌 `BiConsumer`를 받아들이는데, 이는 JDK 전체에서 드문 경우입니다. 이 단일 사용 사례에 의해 주로 `BiConsumer`가 만들어졌다는 것은 Map이 처음부터 더 나은 스트리밍 지원을 받을 자격이 있었음을 시사합니다.

## jOOλ 솔루션

이 격차를 해결하기 위해 jOOQ 팀은 jOOλ(jOO-Lambda)를 만들었습니다. 이것은 `Map`을 향상된 함수형 기능을 제공하는 `Seq` 타입으로 감쌉니다.

사용 예시 - Map을 튜플 목록으로 변환:

```java
Map<Integer, String> map = new LinkedHashMap<>();
map.put(1, "a");
map.put(2, "b");
map.put(3, "c");

Seq.seq(map).toList()
// 반환값: [tuple(1,"a"), tuple(2,"b"), tuple(3,"c")]
```

키와 값 교환:

```java
Seq.seq(map)
   .map(Tuple2::swap)
   .toMap(Tuple2::v1, Tuple2::v2)
// 결과: {a=1, b=2, c=3}
```

## 표준 JDK 접근 방식 (비교용)

표준 Java만 사용한 동등한 코드:

```java
map.entrySet()
   .stream()
   .collect(Collectors.toMap(
       Map.Entry::getValue,
       Map.Entry::getKey
   ))
```

저자는 이 접근 방식이 작동하지만 상당한 장황함으로 인해 jOOλ의 깔끔한 구문에 비해 코드를 읽고 쓰기가 더 어렵다고 지적합니다.

## 결론

jOOλ 라이브러리는 일반적인 컬렉션 작업에 대해 더 많은 함수형 프로그래밍 편의 기능을 제공함으로써 Java 8의 API 격차를 해결합니다.
