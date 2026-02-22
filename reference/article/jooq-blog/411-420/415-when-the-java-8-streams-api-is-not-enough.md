# Java 8 Streams API가 충분하지 않을 때

> 원문: https://blog.jooq.org/when-the-java-8-streams-api-is-not-enough/

Java 8은 - 언제나 그렇듯 - 타협과 하위 호환성의 릴리스였습니다. JSR-335 전문가 그룹이 일부 청중과 특정 기능의 범위나 실현 가능성에 대해 동의하지 않았을 수도 있는 릴리스였습니다. Brian Goetz가 다음에 대해 구체적으로 설명한 내용을 참조하세요:

- Java 8 기본 메서드에서 "final"이 허용되지 않는 이유
- Java 8 기본 메서드에서 "synchronized"가 허용되지 않는 이유

하지만 오늘은 Streams API의 "단점"에 초점을 맞추겠습니다. 또는 Brian Goetz가 아마도 표현했을 것처럼: 설계 목표를 고려할 때 범위 밖의 것들에 대해 이야기하겠습니다.

## 병렬 스트림?

병렬 컴퓨팅은 어렵고, 예전에는 고통스러웠습니다. 사람들은 Java 7과 함께 처음 출시되었을 때 새로운(지금은 오래된) Fork / Join API를 정확히 좋아하지 않았습니다. 반대로, 그리고 분명히, `Stream.parallel()`을 호출하는 간결함은 비교할 수 없습니다. 하지만 많은 사람들은 실제로 병렬 컴퓨팅이 필요하지 않습니다(멀티스레딩과 혼동하지 마세요!). 95%의 경우, 사람들은 아마도 더 강력한 Streams API를 선호했을 것이고, 또는 아마도 다양한 `Iterable` 하위 타입에 수많은 멋진 메서드가 있는 일반적으로 더 강력한 Collections API를 선호했을 것입니다. 하지만 `Iterable`을 변경하는 것은 위험합니다. 잠재적인 `Iterable.stream()` 메서드를 통해 `Iterable`을 `Stream`으로 변환하는 것처럼 당연한 것조차도 판도라의 상자를 열 위험이 있어 보입니다!

## 순차 스트림!

그래서 JDK가 제공하지 않는다면, 우리가 직접 만듭니다! 스트림 자체는 꽤 훌륭합니다. 스트림은 잠재적으로 무한할 수 있고, 그것은 멋진 기능입니다. 대부분 - 특히 함수형 프로그래밍에서 - 컬렉션의 크기는 그다지 중요하지 않습니다. 왜냐하면 우리는 함수를 사용하여 요소별로 변환하기 때문입니다. 스트림이 순수하게 순차적이라는 것을 인정한다면, 우리는 이러한 꽤 멋진 메서드들도 가질 수 있습니다(일부는 병렬 스트림에서도 가능할 것입니다):

- `cycle()` - 모든 스트림을 무한하게 만드는 보장된 방법
- `duplicate()` - 스트림을 두 개의 동등한 스트림으로 복제
- `foldLeft()` - `reduce()`의 순차적이고 비결합적인 대안
- `foldRight()` - `reduce()`의 순차적이고 비결합적인 대안
- `limitUntil()` - 조건자를 만족하는 첫 번째 레코드 이전의 레코드들로 스트림 제한
- `limitWhile()` - 조건자를 만족하지 않는 첫 번째 레코드 이전의 레코드들로 스트림 제한
- `maxBy()` - 스트림을 최대 _매핑된_ 값으로 축소
- `minBy()` - 스트림을 최소 _매핑된_ 값으로 축소
- `partition()` - 스트림을 조건자를 만족하는 스트림과 만족하지 않는 스트림 두 개로 분할
- `reverse()` - 역순으로 새 스트림 생성
- `skipUntil()` - 조건자가 만족될 때까지 레코드 건너뛰기
- `skipWhile()` - 조건자가 만족되는 동안 레코드 건너뛰기
- `slice()` - 스트림의 일부를 가져오기, 즉 `skip()`과 `limit()` 결합
- `splitAt()` - 주어진 위치에서 스트림을 두 개의 스트림으로 분할
- `unzip()` - 쌍의 스트림을 두 개의 스트림으로 분할
- `zip()` - 두 개의 스트림을 쌍의 단일 스트림으로 병합
- `zipWithIndex()` - 스트림을 해당 인덱스 스트림과 병합하여 쌍의 단일 스트림으로 만들기

## jOOλ의 새로운 Seq 타입이 이 모든 것을 수행합니다

위의 모든 것은 jOOλ의 일부입니다. jOOλ(발음은 "jewel" 또는 "dju-lambda", URL 등에서는 jOOL로도 작성됨)는 jOOQ 통합 테스트를 Java 8로 구현할 때 우리 자체 개발 필요에서 탄생한 ASL 2.0 라이선스 라이브러리입니다. Java 8은 집합, 튜플, 레코드 및 모든 SQL 관련 것들에 대해 추론하는 테스트를 작성하는 데 매우 적합합니다. 하지만 Streams API는 약간 불충분하게 느껴지므로, 우리는 JDK의 Streams를 우리 자체 `Seq` 타입으로 래핑했습니다(Seq는 sequence / sequential Stream을 의미):

```java
// 스트림을 시퀀스로 래핑
Seq<Integer> seq1 = seq(Stream.of(1, 2, 3));

// 또는 값에서 직접 시퀀스 생성
Seq<Integer> seq2 = Seq.of(1, 2, 3);
```

우리는 `Seq`를 JDK `Stream` 인터페이스를 확장하는 새로운 인터페이스로 만들었으므로, 다른 Java API와 완전히 상호 운용 가능하게 `Seq`를 사용할 수 있습니다 - 기존 메서드는 변경되지 않습니다:

```java
public interface Seq<T> extends Stream<T> {

    /
     * 기본 {@link Stream} 구현.
     */
    Stream<T> stream();

	// [...]
}
```

이제 함수형 프로그래밍은 튜플이 없으면 재미가 반밖에 안 됩니다. 불행히도 Java에는 내장 튜플이 없고, 제네릭을 사용하여 튜플 라이브러리를 만드는 것은 쉽지만, Java를 Scala, C# 또는 VB.NET과 비교할 때 튜플은 여전히 구문적으로 이류 시민입니다. 그럼에도 불구하고...

## jOOλ에는 튜플도 있습니다

우리는 차수 1-8의 튜플을 생성하는 코드 생성기를 실행했습니다(향후 더 추가할 수 있으며, 예를 들어 Scala와 jOOQ의 "마법 같은" 차수 22에 맞추기 위해). 그리고 라이브러리에 이러한 튜플이 있으면, 라이브러리에는 해당 함수도 필요합니다. 이러한 `TupleN`과 `FunctionN` 타입의 본질은 다음과 같이 요약됩니다:

```java
public class Tuple3<T1, T2, T3>
implements
    Tuple,
	Comparable<Tuple3<T1, T2, T3>>,
	Serializable, Cloneable {

    public final T1 v1;
    public final T2 v2;
    public final T3 v3;

	// [...]
}
```

그리고

```java
@FunctionalInterface
public interface Function3<T1, T2, T3, R> {

    default R apply(Tuple3<T1, T2, T3> args) {
        return apply(args.v1, args.v2, args.v3);
    }

    R apply(T1 v1, T2 v2, T3 v3);
}
```

Tuple 타입에는 더 많은 기능이 있지만, 오늘은 생략하겠습니다.

참고로, Java 클래스와 튜플이 근본적으로 어떻게 다른지에 대한 흥미로운 관점이 있습니다. 클래스가 ORM 관점에서 SQL 관계형 튜플을 모델링하는 데 적합해 보이지만, SQL의 행 값 표현식(즉, 튜플) 개념은 Java 클래스로 모델링할 수 있는 것과 상당히 다릅니다.

## 몇 가지 jOOλ 예제

### Zipping

```java
// (tuple(1, "a"), tuple(2, "b"), tuple(3, "c"))
Seq.of(1, 2, 3).zip(Seq.of("a", "b", "c"));

// ("1:a", "2:b", "3:c")
Seq.of(1, 2, 3).zip(
    Seq.of("a", "b", "c"),
    (x, y) -> x + ":" + y
);

// (tuple("a", 0), tuple("b", 1), tuple("c", 2))
Seq.of("a", "b", "c").zipWithIndex();

// tuple((1, 2, 3), (a, b, c))
Seq.unzip(Seq.of(
    tuple(1, "a"),
    tuple(2, "b"),
    tuple(3, "c")
));
```

이것은 이미 튜플이 매우 유용해진 경우입니다. 두 개의 스트림을 하나로 "zip"할 때, 우리는 두 값을 결합하는 래퍼 값 타입이 필요합니다. 전통적으로 사람들은 빠르고 간편한 솔루션을 위해 `Object[]`를 사용했을 수 있지만, 배열은 속성 타입이나 차수를 나타내지 않습니다. 불행히도 Java 컴파일러는 `Seq<T>`의 `<T>` 타입의 유효한 바운드에 대해 추론할 수 없습니다. 이것이 우리가 (인스턴스 메서드 대신) 정적 `unzip()` 메서드만 가질 수 있는 이유이며, 그 시그니처는 다음과 같습니다:

```java
// 이것은 작동함
static <T1, T2> Tuple2<Seq<T1>, Seq<T2>>
    unzip(Stream<Tuple2<T1, T2>> stream) { ... }

// 이것은 작동하지 않음:
interface Seq<T> extends Stream<T> {
    Tuple2<Seq<???>, Seq<???>> unzip();
}
```

### 건너뛰기와 제한

```java
// (3, 4, 5)
Seq.of(1, 2, 3, 4, 5).skipWhile(i -> i < 3);

// (3, 4, 5)
Seq.of(1, 2, 3, 4, 5).skipUntil(i -> i == 3);

// (1, 2)
Seq.of(1, 2, 3, 4, 5).limitWhile(i -> i < 3);

// (1, 2)
Seq.of(1, 2, 3, 4, 5).limitUntil(i -> i == 3);
```

다른 함수형 라이브러리들은 아마도 skip(예: drop)과 limit(예: take)에 다른 용어를 사용할 것입니다. 결국 그것은 별로 중요하지 않습니다. 우리는 기존 Stream API에 이미 존재하는 용어를 선택했습니다: `Stream.skip()`과 `Stream.limit()`.

### 폴딩

```java
// "abc"
Seq.of("a", "b", "c").foldLeft("", (u, t) -> t + u);

// "cba"
Seq.of("a", "b", "c").foldRight("", (t, u) -> t + u);
```

`Stream.reduce()` 연산은 병렬화를 위해 설계되었습니다. 이것은 전달된 함수가 다음과 같은 중요한 속성을 가져야 함을 의미합니다:

- 결합성
- 비간섭
- 무상태

하지만 때로는 위의 속성을 가지지 않는 함수로 스트림을 "축소"하고 싶을 때가 있고, 결과적으로 축소가 병렬화 가능한지 신경 쓰지 않을 것입니다. 이것이 "폴딩"이 등장하는 부분입니다. 축소와 폴딩의 다양한 차이점에 대한 좋은 설명은 (Scala에서) 함수형 프로그래밍 문헌에서 볼 수 있습니다.

### 분할

```java
// tuple((1, 2, 3), (1, 2, 3))
Seq.of(1, 2, 3).duplicate();

// tuple((1, 3, 5), (2, 4, 6))
Seq.of(1, 2, 3, 4, 5, 6).partition(i -> i % 2 != 0)

// tuple((1, 2), (3, 4, 5))
Seq.of(1, 2, 3, 4, 5).splitAt(2);
```

위의 함수들은 모두 한 가지 공통점이 있습니다: 단일 스트림에서 작동하여 독립적으로 소비할 수 있는 두 개의 새 스트림을 생성합니다. 분명히 이것은 내부적으로 부분적으로 소비된 스트림의 버퍼를 유지하기 위해 일부 메모리가 소비되어야 함을 의미합니다. 예를 들어:

- 복제는 한 스트림에서 소비되었지만 다른 스트림에서는 소비되지 않은 모든 값을 추적해야 합니다
- 파티셔닝은 드롭된 모든 값을 잃지 않으면서 조건자를 만족하는(또는 만족하지 않는) 다음 값으로 빠르게 이동해야 합니다
- 분할은 분할 인덱스로 빠르게 이동해야 할 수 있습니다

진정한 함수형 재미를 위해, 가능한 `splitAt()` 구현을 살펴보겠습니다:

```java
static <T> Tuple2<Seq<T>, Seq<T>>
splitAt(Stream<T> stream, long position) {
    return seq(stream)
          .zipWithIndex()
          .partition(t -> t.v2 < position)
          .map((v1, v2) -> tuple(
              v1.map(t -> t.v1),
              v2.map(t -> t.v1)
          ));
}
```

... 또는 주석과 함께:

```java
static <T> Tuple2<Seq<T>, Seq<T>>
splitAt(Stream<T> stream, long position) {
    // 스트림에 jOOλ 기능 추가
    // -> 로컬 타입: Seq<T>
    return seq(stream)

    // 스트림의 각 요소와 함께
    // 스트림 위치 추적
    // -> 로컬 타입: Seq<Tuple2<T, Long>>
          .zipWithIndex()

    // 위치에서 스트림 분할
    // -> 로컬 타입: Tuple2<Seq<Tuple2<T, Long>>,
    //                       Seq<Tuple2<T, Long>>>
          .partition(t -> t.v2 < position)

    // zipWithIndex에서 인덱스 다시 제거
    // -> 로컬 타입: Tuple2<Seq<T>, Seq<T>>
          .map((v1, v2) -> tuple(
              v1.map(t -> t.v1),
              v2.map(t -> t.v1)
          ));
}
```

멋지지 않나요? 반면에 `partition()`의 가능한 구현은 조금 더 복잡합니다. 여기서는 새로운 `Spliterator` 대신 `Iterator`를 사용하여 간단하게 표현했습니다:

```java
static <T> Tuple2<Seq<T>, Seq<T>> partition(
        Stream<T> stream,
        Predicate<? super T> predicate
) {
    final Iterator<T> it = stream.iterator();
    final LinkedList<T> buffer1 = new LinkedList<>();
    final LinkedList<T> buffer2 = new LinkedList<>();

    class Partition implements Iterator<T> {

        final boolean b;

        Partition(boolean b) {
            this.b = b;
        }

        void fetch() {
            while (buffer(b).isEmpty() && it.hasNext()) {
                T next = it.next();
                buffer(predicate.test(next)).offer(next);
            }
        }

        LinkedList<T> buffer(boolean test) {
            return test ? buffer1 : buffer2;
        }

        @Override
        public boolean hasNext() {
            fetch();
            return !buffer(b).isEmpty();
        }

        @Override
        public T next() {
            return buffer(b).poll();
        }
    }

    return tuple(
        seq(new Partition(true)),
        seq(new Partition(false))
    );
}
```

## 지금 바로 jOOλ를 받고 기여하세요!

위의 모든 것은 GitHub에서 무료로 사용할 수 있는 jOOλ의 일부입니다. jOOλ보다 훨씬 더 나아가는 부분적으로 Java-8-ready인 본격적인 라이브러리 functionaljava가 이미 있습니다. 하지만 우리는 Java 8의 Streams API에서 정말 부족한 것은 순차 스트림에 매우 유용한 몇 가지 메서드뿐이라고 믿습니다. 이전 포스트에서 우리는 JDBC를 위한 간단한 래퍼를 사용하여 문자열 기반 SQL에 람다를 가져오는 방법을 보여주었습니다. 오늘 우리는 jOOλ로 멋진 함수형 및 순차 스트림 처리를 매우 쉽게 작성하는 방법을 보여주었습니다. 가까운 미래에 더 많은 jOOλ의 좋은 것들을 기대해 주세요(그리고 물론 풀 리퀘스트는 매우 환영합니다!)
