# 일반적인 SQL 절과 Java 8 스트림에서의 동등물

> 원문: https://blog.jooq.org/common-sql-clauses-and-their-equivalents-in-java-8-streams/

## 소개

이 글에서는 SQL의 선언적 프로그래밍 패러다임이 Java 8의 함수형 Stream API와 jOOλ의 Seq 확장에 어떻게 매핑되는지 탐구합니다. SQL이 진정한 선언적 언어(데이터베이스 옵티마이저가 실행 전략을 선택할 수 있게 해주는)인 반면, Java의 함수형 프로그래밍은 명령적입니다—여러분이 정확한 연산 순서를 정의합니다.

## 튜플

기반이 되는 것은 jOOλ의 `Tuple` 타입으로, 이는 동일성이 아닌 값 기반 타입입니다. 두 튜플 `(1, 'A')`는 객체 동일성과 관계없이 동등한 것으로 간주됩니다.

```java
public class Tuple2<T1, T2> {
    public final T1 v1;
    public final T2 v2;
    public Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
}
```

## FROM 절 = 스트림 생성

SQL 예제:
```sql
SELECT *
FROM (VALUES(1, 1), (2, 2)) t(v1, v2)
```

Java 동등물:
```java
Stream.of(tuple(1, 1), tuple(2, 2))
      .forEach(System.out::println);
```

## CROSS JOIN = flatMap()

카르테시안 곱은 한 스트림의 모든 요소를 다른 스트림의 모든 요소와 결합하여 `size(t1) * size(t2)` 행을 생성합니다.

SQL:
```sql
SELECT *
FROM (VALUES(1), (2)) t1(v1)
CROSS JOIN (VALUES('A'), ('B')) t2(v2)
```

Java:
```java
List<Integer> s1 = Arrays.asList(1, 2);
List<String> s2 = Arrays.asList("A", "B");

s1.stream()
  .flatMap(v1 -> s2.stream()
                   .map(v2 -> tuple(v1, v2)))
  .forEach(System.out::println);
```

jOOλ 사용:
```java
Seq<Integer> s1 = Seq.of(1, 2);
Seq<String> s2 = Seq.of("A", "B");
Seq<String> s3 = Seq.of("X", "Y");

s1.crossJoin(s2)
  .crossJoin(s3)
  .forEach(System.out::println);
```

## INNER JOIN = flatMap()과 filter()

내부 조인은 조건자를 사용하여 카르테시안 곱을 필터링합니다. 그러나 단순한 접근 방식은 필터링하기 전에 전체 카르테시안 곱을 생성합니다—조건자를 내부 스트림으로 푸시하는 것에 비해 비효율적입니다.

비효율적인 SQL 스타일 접근 방식:
```java
List<Integer> s1 = Arrays.asList(1, 2);
List<Integer> s2 = Arrays.asList(1, 3);

s1.stream()
  .flatMap(v1 -> s2.stream()
                   .map(v2 -> tuple(v1, v2)))
  .filter(t -> Objects.equals(t.v1, t.v2))
  .forEach(System.out::println);
```

더 효율적인 접근 방식:
```java
s1.stream()
  .flatMap(v1 -> s2.stream()
                   .filter(v2 -> Objects.equals(v1, v2))
                   .map(v2 -> tuple(v1, v2)))
  .forEach(System.out::println);
```

jOOλ 사용:
```java
Seq<Integer> s1 = Seq.of(1, 2);
Seq<Integer> s2 = Seq.of(1, 3);

s1.innerJoin(s2, (t, u) -> Objects.equals(t, u))
  .forEach(System.out::println);
```

## LEFT OUTER JOIN = flatMap()과 filter() 및 onEmpty()

왼쪽 외부 조인은 왼쪽 요소당 최소 하나의 행을 보장하며, 조인 조건이 실패할 때 오른쪽 값에 `null`을 사용합니다.

SQL:
```sql
SELECT *
FROM (VALUES(1), (2)) t1(v1)
LEFT OUTER JOIN (VALUES(1), (3)) t2(v2)
ON t1.v1 = t2.v2
```

jOOλ를 사용한 Java:
```java
Seq<Integer> s1 = Seq.of(1, 2);
Seq<Integer> s2 = Seq.of(1, 3);

s1.leftOuterJoin(s2, (t, u) -> Objects.equals(t, u))
  .forEach(System.out::println);
```

출력: `(1, 1)` 및 `(2, null)`

## RIGHT OUTER JOIN = 역방향 LEFT OUTER JOIN

의미상 오른쪽 외부 조인은 인자가 바뀐 왼쪽 외부 조인과 같습니다.

```java
s1.rightOuterJoin(s2, (t, u) -> Objects.equals(t, u))
  .forEach(System.out::println);
```

## WHERE = filter()

`WHERE` 절은 `Stream.filter()`에 직접 매핑됩니다.

SQL:
```sql
SELECT *
FROM (VALUES(1), (2), (3)) t(v)
WHERE v % 2 = 0
```

Java:
```java
Stream<Integer> s = Stream.of(1, 2, 3);
s.filter(v -> v % 2 == 0)
 .forEach(System.out::println);
```

## GROUP BY = collect()

`GROUP BY`는 튜플 집합을 그룹으로 변환하고, 각 그룹에 집계가 적용됩니다. Stream API의 `collect()`는 `Map`을 생성하는 종료 연산입니다.

GROUP BY 없는 단순 집계:
```java
Stream<Integer> s = Stream.of(1, 2, 3);
int sum = s.collect(Collectors.summingInt(i -> i));
```

GROUP BY 사용:
```java
Stream<Integer> s = Stream.of(1, 2, 3);

Map<Integer, List<Integer>> map = s.collect(
    Collectors.groupingBy(v -> v % 2)
);
```

jOOλ를 사용한 정교한 GROUP BY:
```java
class A {
    final int w, x, y, z;
    A(int w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

Map<Tuple2<Integer, Integer>,
    Tuple2<IntSummaryStatistics, IntSummaryStatistics>> map =
Seq.of(
    new A(1, 1, 1, 1),
    new A(1, 2, 3, 1),
    new A(9, 8, 6, 4),
    new A(9, 9, 7, 4),
    new A(2, 3, 4, 5),
    new A(2, 4, 4, 5),
    new A(2, 5, 5, 5))
.groupBy(
    a -> tuple(a.z, a.w),
    Tuple.collectors(
        Collectors.summarizingInt(a -> a.x),
        Collectors.summarizingInt(a -> a.y)
    )
);
```

## HAVING = filter()

`collect()`가 `Map`을 생성한 후, Map의 엔트리에서 생성된 새 스트림에서 필터링이 수행되어야 합니다.

SQL:
```sql
SELECT v % 2, count(v)
FROM (VALUES(1), (2), (3)) t(v)
GROUP BY v % 2
HAVING count(v) > 1
```

Java:
```java
Stream<Integer> s = Stream.of(1, 2, 3);

s.collect(Collectors.groupingBy(
      v -> v % 2,
      Collectors.summarizingInt(i -> i)
  ))
  .entrySet()
  .stream()
  .filter(e -> e.getValue().getCount() > 1)
  .forEach(System.out::println);
```

## SELECT = map()

`SELECT`는 `Stream.map()`을 통해 튜플을 변환합니다.

SQL:
```sql
SELECT t.v1 * 3, t.v2 + 5
FROM (VALUES(1, 1), (2, 2)) t(v1, v2)
```

Java:
```java
Stream.of(tuple(1, 1), tuple(2, 2))
      .map(t -> tuple(t.v1 * 3, t.v2 + 5))
      .forEach(System.out::println);
```

## DISTINCT = distinct()

변환 후 중복 튜플을 제거합니다.

SQL:
```sql
SELECT DISTINCT t.v1 * 3, t.v2 + 5
FROM (VALUES(1, 1), (2, 2), (2, 2)) t(v1, v2)
```

Java:
```java
Stream.of(tuple(1, 1), tuple(2, 2), tuple(2, 2))
      .map(t -> tuple(t.v1 * 3, t.v2 + 5))
      .distinct()
      .forEach(System.out::println);
```

## UNION ALL = concat()

중복을 제거하지 않고 두 스트림을 연결합니다.

SQL:
```sql
SELECT * FROM (VALUES(1), (2)) t(v)
UNION ALL
SELECT * FROM (VALUES(1), (3)) t(v)
```

Java:
```java
Stream<Integer> s1 = Stream.of(1, 2);
Stream<Integer> s2 = Stream.of(1, 3);
Stream.concat(s1, s2).forEach(System.out::println);
```

## UNION = concat()과 distinct()

연결 후 중복 제거.

SQL:
```sql
SELECT * FROM (VALUES(1), (2)) t(v)
UNION
SELECT * FROM (VALUES(1), (3)) t(v)
```

Java:
```java
Stream.concat(s1, s2)
      .distinct()
      .forEach(System.out::println);
```

## ORDER BY = sorted()

정렬은 `Stream.sorted()`를 사용합니다.

SQL:
```sql
SELECT * FROM (VALUES(1), (4), (3)) t(v)
ORDER BY v
```

Java:
```java
Stream<Integer> s = Stream.of(1, 4, 3);
s.sorted()
 .forEach(System.out::println);
```

## LIMIT = limit()

출력 크기를 제한합니다.

SQL:
```sql
SELECT * FROM (VALUES(1), (4), (3)) t(v)
LIMIT 2
```

Java:
```java
Stream.of(1, 4, 3)
      .limit(2)
      .forEach(System.out::println);
```

## OFFSET = skip()

초기 행을 건너뜁니다.

SQL:
```sql
SELECT * FROM (VALUES(1), (4), (3)) t(v)
OFFSET 1
```

Java:
```java
Stream.of(1, 4, 3)
      .skip(1)
      .forEach(System.out::println);
```

## 결론

SQL의 선언적 모델과 Java 8의 함수형 접근 방식은 유사한 변환을 다르게 표현합니다. SQL은 제약 조건, 인덱스, 통계를 고려하여 쿼리 플래너에게 최적화를 위임합니다. 반면에 함수형 프로그래밍은 실행 순서를 명시적으로 지정해야 합니다—본질적으로 하나의 특정 실행 계획을 강제하는 것입니다. SQL이 더 높은 수준의 추상화를 제공하는 반면, Stream API는 프로그래밍적 제어가 필요할 때 더 커스터마이징 가능한 알고리즘을 가능하게 합니다.
