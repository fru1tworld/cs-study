# 잘 알려지지 않은 jOOλ 기능: 유용한 Collector들

> 원문: https://blog.jooq.org/lesser-known-jooλ-features-useful-collectors/

*작성자: Lukas Eder*
*게시일: 2019년 2월 11일 (2021년 4월 7일 업데이트)*

jOOλ는 우리의 두 번째로 인기 있는 라이브러리입니다. 이 라이브러리는 JDK의 Stream API에 대한 유용한 확장 기능들을 구현하고 있으며, 특히 스트림이 순차적(sequential)으로만 사용될 때 유용합니다. 실제로 대부분의 개발자들이 Java에서 스트림을 사용하는 방식이 바로 순차 스트림입니다.

다음은 jOOλ의 스트림 확장 기능 예시입니다:

- `cycle()` - 시퀀스를 무한히 반복합니다
- `duplicate()` - 동일한 두 개의 스트림으로 이루어진 튜플을 생성합니다
- `intersperse()` - 요소들 사이에 값을 삽입합니다
- `reverse()` - 요소의 순서를 뒤집습니다

jOOλ는 또한 유용한 Collector 세트를 함께 제공합니다. 이 Collector들은 JDK 스트림과 jOOλ의 `Seq` 타입 모두에서 사용할 수 있습니다.

대부분의 Collector들은 `org.jooq.lambda.Agg` 클래스에서 사용할 수 있습니다. 여기서 "Agg"는 집계(aggregations)를 의미합니다. 이러한 Collector들은 SQL 집계 함수에서 영감을 받았습니다.

## 카운팅 연산

JDK에서 제공하는 표준 카운팅 메서드 외에도, jOOλ는 고유 값 카운팅 기능을 제공하며, 이는 SQL 기능을 반영합니다.

```java
@Test
public void testCount() {
    assertEquals(7L, (long)
        Stream.of(1, 2, 3, 3, 4, 4, 5)
              .collect(Agg.count()));
    assertEquals(5L, (long)
        Stream.of(1, 2, 3, 3, 4, 4, 5)
              .collect(Agg.countDistinct()));
    assertEquals(5L, (long)
        Stream.of(A("a", 1),
                  A("b", 2),
                  A("c", 3),
                  A("d", 3),
                  A("e", 4),
                  A("f", 4),
                  A("g", 5))
              .collect(Agg.countDistinctBy(a -> a.l)));
    assertEquals(7L, (long)
        Stream.of(A("a", 1),
                  A("b", 2),
                  A("c", 3),
                  A("d", 3),
                  A("e", 4),
                  A("f", 4),
                  A("g", 5))
              .collect(Agg.countDistinctBy(a -> a.s)));
}
```

위 예제에서 사용된 헬퍼 클래스는 다음과 같습니다:

```java
class A {
    final String s;
    final long l;
    A(String s, long l) {
        this.s = s;
        this.l = l;
    }

    static A A(String s, long l) {
        return new A(s, l);
    }
}
```

이 예제는 간단한 카운트, 고유 값 카운트, 그리고 매핑된 값에 기반한 고유 값 카운트를 보여줍니다. 7개의 전체 요소 대 5개의 고유 값을 비교하여 카운팅하는 방법을 확인할 수 있습니다.

## 백분위수 계산

백분위수(percentile)도 스트림에서 멋지게 계산할 수 있습니다. 왜 안 되겠습니까? 스트림의 내용이 `Comparable`을 구현하거나, 사용자 정의 `Comparator`를 제공하면, 백분위수는 쉽게 계산됩니다.

jOOλ는 SQL의 `percentile_disc` 의미론을 구현합니다. 이는 연속적인 보간(interpolation)을 계산하는 `percentile_cont`와는 다릅니다. `percentile_disc`는 실제 스트림에 존재하는 불연속(discrete) 값을 반환합니다.

```java
assertEquals(
    Optional.empty(),
    Stream.<Integer> of().collect(percentile(0.25)));
assertEquals(
    Optional.of(1),
    Stream.of(1).collect(percentile(0.25)));
assertEquals(
    Optional.of(1),
    Stream.of(1, 2).collect(percentile(0.25)));
assertEquals(
    Optional.of(1),
    Stream.of(1, 2, 3).collect(percentile(0.25)));
assertEquals(
    Optional.of(1),
    Stream.of(1, 2, 3, 4).collect(percentile(0.25)));
assertEquals(
    Optional.of(2),
    Stream.of(1, 2, 3, 4, 10).collect(percentile(0.25)));
assertEquals(
    Optional.of(2),
    Stream.of(1, 2, 3, 4, 10, 9).collect(percentile(0.25)));
assertEquals(
    Optional.of(2),
    Stream.of(1, 2, 3, 4, 10, 9, 3).collect(percentile(0.25)));
assertEquals(
    Optional.of(2),
    Stream.of(1, 2, 3, 4, 10, 9, 3, 3).collect(percentile(0.25)));
assertEquals(
    Optional.of(3),
    Stream.of(1, 2, 3, 4, 10, 9, 3, 3, 20).collect(percentile(0.25)));
assertEquals(
    Optional.of(3),
    Stream.of(1, 2, 3, 4, 10, 9, 3, 3, 20, 21).collect(percentile(0.25)));
assertEquals(
    Optional.of(3),
    Stream.of(1, 2, 3, 4, 10, 9, 3, 3, 20, 21, 22).collect(percentile(0.25)));
```

빈 스트림에서는 `Optional.empty()`가 반환되어 우아하게 처리됩니다.

스트림 값에서 직접 백분위수를 계산하거나, 다른 값으로 정렬하거나, 사용자 정의 함수를 통해 매핑하는 등 다양한 오버로드를 지원합니다.

특별한 백분위수의 경우:
- 최솟값(`min()`)은 0% 백분위수에 해당합니다
- 중앙값(`median()`)은 50% 백분위수에 해당합니다
- 최댓값(`max()`)은 100% 백분위수에 해당합니다

이러한 특별한 경우들은 전용 메서드 이름을 가지고 있습니다.

## 최빈값(Mode)

최빈값은 어떨까요? 즉, 스트림에서 가장 자주 나타나는 값 말입니다. `Agg.mode()`를 사용하면 쉽습니다.

```java
assertEquals(
    Optional.of(1),
    Stream.of(1, 1, 1, 2, 3, 4).collect(Agg.mode()));
assertEquals(
    Optional.of(1),
    Stream.of(1, 1, 2, 2, 3, 4).collect(Agg.mode()));
assertEquals(
    Optional.of(2),
    Stream.of(1, 1, 2, 2, 2, 4).collect(Agg.mode()));
```

이 통계적 측정값은 스트림에서 가장 자주 발생하는 값을 식별하는 데 유용합니다. 동점(빈도가 같은 경우)이 있을 때는 표준 통계 규칙을 따릅니다.

## 추가적인 유용한 Collector들

jOOλ는 다음과 같은 다양한 집계자를 추가로 제공합니다:

- 비트 연산: `bitAnd()`와 `bitOr()`는 비트 연산자를 사용하여 숫자를 집계합니다
- 문자열 연산: `commonPrefix()`와 `commonSuffix()`는 공통 문자열 패턴을 찾습니다
- 매칭 술어: `allMatch()`, `anyMatch()`, `noneMatch()`는 Stream의 동작을 흉내냅니다. PostgreSQL의 `EVERY()` 집계 함수와 유사한 기능입니다
- 순위 함수: `rank()`, `denseRank()`, `percentRank()`는 가상 집합(hypothetical set) 함수로, 값이 스트림에 포함될 경우 어떤 순위를 가질지 계산합니다
- 값 선택: `first()`와 `last()`는 스트림의 초기 값과 최종 값을 추출합니다

## 집계 결합하기

jOOλ를 사용할 때 마지막으로 중요한 기능 중 하나는 SQL에서처럼 집계를 결합하는 기능입니다.

여러 Collector들을 `Tuple.collectors()`를 사용하여 결합하면 여러 집계를 동시에 계산할 수 있습니다:

```java
var percentiles =
Stream.of(1, 2, 3, 4, 10, 9, 3, 3).collect(
  Tuple.collectors(
    Agg.<Integer>percentile(0.0),
    Agg.<Integer>percentile(0.25),
    Agg.<Integer>percentile(0.5),
    Agg.<Integer>percentile(0.75),
    Agg.<Integer>percentile(1.0)
  )
);

System.out.println(percentiles);
// 출력: (Optional[1], Optional[2], Optional[3], Optional[4], Optional[10])
```

이 접근 방식은 SQL의 다중 컬럼 집계 패턴과 유사합니다. 한 번의 패스로 여러 통계를 효율적으로 계산할 수 있게 해줍니다.
