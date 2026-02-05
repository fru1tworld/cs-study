# SQL GROUP BY와 집계를 Java 8로 번역하는 방법

> 원문: https://blog.jooq.org/how-to-translate-sql-group-by-and-aggregations-to-java-8/

Stack Overflow에서 [Hugo Prudente가 흥미로운 질문을 했습니다](http://stackoverflow.com/q/28342814/521799):

> Java 8을 사용하여 데이터베이스 쿼리처럼 데이터를 그룹화하는 방법은 무엇인가요?

그는 기본적으로 JDBC에서 원시 결과 집합을 얻고, Java 8에서 이 결과를 멋지게 집계하면서 `GROUP BY`를 적용하여 SQL의 도움 없이도 값에 대한 `MIN()`, `MAX()`, `AVG()`와 같은 집계 함수를 수행하고 싶었습니다.

## SQL은 선언적입니다. 함수형 프로그래밍은 그렇지 않습니다.

SQL은 완전히 선언적인 언어입니다. Java 8과 같은 함수형(또는 "함수형 비슷한") 프로그래밍 언어는 선언적이지 않습니다. 선언적으로 *읽히는* 편리한 API를 사용하면, 결과물이 완전히 선언적인 언어로 작성된 것처럼 느껴질 수 있지만 실제로는 그렇지 않습니다.

선언적 프로그래밍이란 무엇을 의미할까요? 원하는 결과를 선언한 후 언어/엔진이 해당 결과를 얻기 위해 (가장) 좋은 알고리즘을 찾도록 하는 것입니다. 예를 들어, 다음과 같은 SQL 문을 작성할 때:

```sql
SELECT z, w, MIN(x), MAX(x), AVG(x), MIN(y), MAX(y), AVG(y)
FROM table
GROUP BY z, w;
```

... 우리는 엔진에게 원하는 것을 알려주면, 엔진은 가장 적합한 알고리즘을 찾아낼 것입니다. 데이터가 이미 인덱스로 사전 집계되어 있지 않은 한, 아마도 전체 테이블(또는 인덱스)을 스캔하고, 해시 기반 GROUP BY를 수행하고, 개별 열에 대한 집계 함수를 계산할 것입니다.

반면에 Java 8에서는 일부 [Collector API](http://docs.oracle.com/javase/8/docs/api/java/util/stream/Collector.html) 연산자를 사용하여 원하는 것을 표현할 수 있지만, 그렇게 하는 동안에도 우리는 여전히 알고리즘을 명시적으로 표현하고 있습니다. 우리는 그저 "선언적 읽기"를 생성하고 있을 뿐입니다. 차이가 미묘하지만, 중요합니다.

그건 그렇고: [jOOQ와 함께 데이터 변환에 Java 8을 사용하는 방법](https://blog.jooq.org/java-8-friday-goodies-easy-as-pie-local-caching/)에 관심이 있으시다면, 이 블로그 게시물을 확인하세요.

## Java 8 스트림과 컬렉터 배우기

이 블로그 게시물을 보시기 전에, [Eugen Paraschiv가 @Baeldung에 작성한 훌륭한 Java 8 Collectors 가이드](https://www.baeldung.com/java-8-collectors)를 잠시 살펴보세요.

## 집계 구현하기

위에서 언급했듯이, 우리가 구현하고자 하는 SQL 문은 다음과 같습니다:

```sql
SELECT z, w, MIN(x), MAX(x), AVG(x), MIN(y), MAX(y), AVG(y)
FROM table
GROUP BY z, w;
```

우리의 예제에서는 대략 SQL 테이블 행에 해당하는 다음 클래스를 사용할 것입니다:

```java
class A {
    final int w;
    final int x;
    final int y;
    final int z;

    A(int w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "A{" +
                "w=" + w +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
```

또한 다음의 스트림 데이터를 사용합니다:

```java
Stream<A> stream = Stream.of(
    new A(1, 1, 1, 1),
    new A(1, 2, 3, 1),
    new A(9, 8, 6, 4),
    new A(9, 9, 7, 4),
    new A(2, 3, 4, 5),
    new A(2, 4, 4, 5),
    new A(2, 5, 5, 5));
```

이제 `z`와 `w`로 그룹화해 보겠습니다. Stream API에는 `GROUP BY`에 대한 편리한 방법이 없습니다. 대신 컬렉터를 사용해야 합니다. 다음 그룹화를 작성할 수 있습니다:

```java
stream.collect(Collectors.groupingBy(...));
```

Stream API를 따라가면, `Collectors.groupingBy()`는 그룹화 함수와 두 번째 컬렉터를 인수로 받아들입니다. 두 번째 컬렉터는 "downstream" 컬렉터라고 하며, 첫 번째 컬렉터의 결과에 적용됩니다. 따라서 이 경우 첫 번째 그룹화 함수는 단순히 튜플 `(z, w)`를 추출해야 합니다.

하지만 JDK에는 튜플 유형이 없습니다. 그래서 우리는 튜플을 만들어야 합니다. 예를 들어:

```java
public class Tuple2<T1, T2> {
    public final T1 v1;
    public final T2 v2;

    public T1 v1() {
        return v1;
    }

    public T2 v2() {
        return v2;
    }

    public Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
}

public interface Tuple {
    static <T1, T2> Tuple2<T1, T2> tuple(T1 v1, T2 v2) {
        return new Tuple2<>(v1, v2);
    }
}
```

[jOOλ](https://github.com/jOOQ/jOOL)과 같은 오픈 소스 라이브러리를 사용할 수도 있습니다. jOOλ은 `Tuple0`부터 `Tuple16`까지 제공하며, 편리한 팩토리 메서드가 있습니다. 이것을 사용하면 다음을 작성할 수 있습니다:

```java
Map<Tuple2<Integer, Integer>, List<A>> map =
Stream.of(
    new A(1, 1, 1, 1),
    new A(1, 2, 3, 1),
    new A(9, 8, 6, 4),
    new A(9, 9, 7, 4),
    new A(2, 3, 4, 5),
    new A(2, 4, 4, 5),
    new A(2, 5, 5, 5))
.collect(Collectors.groupingBy(
    a -> tuple(a.z, a.w)
));

map.entrySet().forEach(System.out::println);
```

이렇게 하면 다음과 같은 결과가 출력됩니다:

```
(1, 1)=[A{w=1, x=1, y=1, z=1}, A{w=1, x=2, y=3, z=1}]
(4, 9)=[A{w=9, x=8, y=6, z=4}, A{w=9, x=9, y=7, z=4}]
(5, 2)=[A{w=2, x=3, y=4, z=5}, A{w=2, x=4, y=4, z=5}, A{w=2, x=5, y=5, z=5}]
```

훌륭합니다! 우리가 원하는 SQL GROUP BY 기능은 아니지만, 데이터가 이제 각 그룹화 키에 대해 올바르게 그룹화되어 있습니다.

## 집계 추가하기

이제 x와 y 값에 대해 `MIN()`, `MAX()`, `AVG()`를 수행해야 합니다.

이 시점에서 JDK는 보다 편리한 기능을 거의 제공하지 않습니다. 우리의 목표를 달성하려면 저수준 `Collector.of()` API를 사용해야 합니다. 다행히도 JDK에는 `IntSummaryStatistics`라는 유용한 것이 있습니다. 이것은 `COUNT`, `SUM`, `MIN`, `MAX`를 수집하며, `AVG`는 `SUM/COUNT`로 쉽게 도출됩니다.

다음과 같이 컬렉터를 구현할 수 있습니다:

```java
Map<
    Tuple2<Integer, Integer>,
    Tuple2<IntSummaryStatistics, IntSummaryStatistics>
> map = Stream.of(
    new A(1, 1, 1, 1),
    new A(1, 2, 3, 1),
    new A(9, 8, 6, 4),
    new A(9, 9, 7, 4),
    new A(2, 3, 4, 5),
    new A(2, 4, 4, 5),
    new A(2, 5, 5, 5))
.collect(Collectors.groupingBy(
    a -> tuple(a.z, a.w),

    // "downstream" 컬렉터
    Collector.of(

        // supplier
        () -> tuple(
            new IntSummaryStatistics(),
            new IntSummaryStatistics()
        ),

        // accumulator
        (r, t) -> {
            r.v1.accept(t.x);
            r.v2.accept(t.y);
        },

        // combiner
        (r1, r2) -> {
            r1.v1.combine(r2.v1);
            r1.v2.combine(r2.v2);

            return r1;
        }
    )
));

map.entrySet().forEach(System.out::println);
```

위 코드는 다음을 출력합니다:

```
(1, 1)=(IntSummaryStatistics{count=2, sum=3, min=1, average=1.500000, max=2},
        IntSummaryStatistics{count=2, sum=4, min=1, average=2.000000, max=3})
(4, 9)=(IntSummaryStatistics{count=2, sum=17, min=8, average=8.500000, max=9},
        IntSummaryStatistics{count=2, sum=13, min=6, average=6.500000, max=7})
(5, 2)=(IntSummaryStatistics{count=3, sum=12, min=3, average=4.000000, max=5},
        IntSummaryStatistics{count=3, sum=13, min=4, average=4.333333, max=5})
```

훌륭합니다!

## jOOλ를 사용하여 단순화하기

jOOλ를 사용하면, 기본적으로 위의 코드를 라이브러리에 넣을 수 있습니다. jOOλ의 `Tuple.collectors()`와 편리한 `Seq.groupBy()` 메서드를 사용하여 위 내용을 다음과 같이 작성할 수 있습니다:

```java
Map<
    Tuple2<Integer, Integer>,
    Tuple2<IntSummaryStatistics, IntSummaryStatistics>
> map =

// Seq는 jOOλ의 확장된 스트림입니다
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

    // 컬렉터들을 튜플로 결합
    Tuple.collectors(
        Collectors.summarizingInt(a -> a.x),
        Collectors.summarizingInt(a -> a.y)
    )
);
```

이렇게 하면 동일한 결과가 나옵니다.

`Tuple.collectors()`의 구현은 `Collector.of()` 논리를 일반화하여 임의 개수의 컬렉터를 튜플로 결합합니다. 예를 들어, `Tuple2` 버전은 다음과 같이 구현됩니다:

```java
// 두 개의 컬렉터를 하나의 튜플 컬렉터로 결합
static <T, A1, A2, D1, D2>
       Collector<T, Tuple2<A1, A2>, Tuple2<D1, D2>>
collectors(
    Collector<T, A1, D1> collector1
  , Collector<T, A2, D2> collector2
) {
    return Collector.of(
        () -> tuple(
            collector1.supplier().get()
          , collector2.supplier().get()
        ),
        (a, t) -> {
            collector1.accumulator().accept(a.v1, t);
            collector2.accumulator().accept(a.v2, t);
        },
        (a1, a2) -> tuple(
            collector1.combiner().apply(a1.v1, a2.v1)
          , collector2.combiner().apply(a1.v2, a2.v2)
        ),
        a -> tuple(
            collector1.finisher().apply(a.v1)
          , collector2.finisher().apply(a.v2)
        )
    );
}
```

## 결론

보시다시피, JDK는 스트림과 컬렉터를 사용하여 기본적인 데이터 변환을 수행할 수 있도록 합니다. 하지만 API는 의도적으로 저수준이며, 많은 개선의 여지를 남기고 있습니다. jOOλ은 JDK를 몇 가지 유용한 기능으로 확장하지만, 많은 고급 함수형 프로그래밍 기능(예: 모나드, Either 타입 등)을 제공하지는 않습니다. 이러한 기능이 필요하시다면, [functionaljava](https://github.com/functionaljava/functionaljava)나 [Vavr](https://github.com/vavr-io/vavr)을 추천드립니다.

한 가지 언급해야 할 점: 이 블로그 게시물이 작성될 당시 IDE의 Java 8 지네릭 추론 지원은 여전히 버그가 많았습니다. 이러한 복잡한 지네릭 연산을 컴파일 오류 없이 처리하려면 약간의 인내심이 필요할 수 있습니다. 하지만 Java 9나 10에서 값 타입과 지네릭 특수화가 도입되면 개선될 것입니다.

물론, SQL과 jOOQ를 함께 사용하면 이러한 유형의 보고서 작업이 훨씬 더 쉬워집니다. 이러한 보고서에는 SQL을 사용하고, 함수형 변환에는 Java 8을 사용하는 것이 좋습니다. 그래야 두 세계의 장점을 모두 활용할 수 있습니다!
