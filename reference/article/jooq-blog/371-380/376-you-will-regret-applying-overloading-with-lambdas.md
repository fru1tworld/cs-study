# 람다와 오버로딩을 적용하면 후회할 것이다!

> 원문: https://blog.jooq.org/you-will-regret-applying-overloading-with-lambdas/

2015년 1월 29일 lukaseder 작성

좋은 API를 작성하는 것은 어렵다. 극도로 어렵다. 사용자가 여러분의 API를 좋아하게 만들려면 엄청나게 많은 것들을 고려해야 한다. 다음 사항들 사이에서 적절한 균형을 찾아야 한다:

1. 유용성
2. 사용성
3. 하위 호환성
4. 상위 호환성

우리는 이전에 "좋고 규칙적인 API를 설계하는 방법"이라는 글에서 이 주제에 대해 블로그에 글을 올린 적이 있다. 오늘은 Java 8이 어떻게 규칙을 바꾸는지 살펴볼 것이다.

## Java 8이 규칙을 바꾼다

그렇다! 오버로딩은 두 가지 차원에서 편의성을 제공하는 좋은 도구다:

- 인자 타입 대안 제공
- 인자 기본값 제공

위의 예시는 JDK에서 다음과 같이 찾아볼 수 있다:

```java
public class Arrays {

    // 인자 타입 대안
    public static void sort(int[] a) { ... }
    public static void sort(long[] a) { ... }

    // 인자 기본값
    public static IntStream stream(int[] array) { ... }
    public static IntStream stream(int[] array,
        int startInclusive,
        int endExclusive) { ... }
}
```

jOOQ API는 당연히 이러한 편의 기능으로 가득하다. jOOQ는 SQL을 위한 DSL이기 때문에 약간 남용할 수도 있다:

```java
public interface DSLContext {
    <T1> SelectSelectStep<Record1<T1>>
        select(SelectField<T1> field1);

    <T1, T2> SelectSelectStep<Record2<T1, T2>>
        select(SelectField<T1> field1,
               SelectField<T2> field2);

    <T1, T2, T3> SelectSelectStep<Record3<T1, T2, T3>>
        select(SelectField<T1> field1,
               SelectField<T2> field2,
               SelectField<T3> field3);

    <T1, T2, T3, T4> SelectSelectStep<Record4<T1, T2, T3, T4>>
        select(SelectField<T1> field1,
               SelectField<T2> field2,
               SelectField<T3> field3,
               SelectField<T4> field4);

    // 그리고 계속...
}
```

Ceylon과 같은 언어들은 위의 내용이 Java에서 오버로딩을 사용해야 하는 유일하고 합리적인 이유라고 주장함으로써 이 편의성 아이디어를 한 단계 더 발전시킨다. 그래서 Ceylon의 창시자들은 자신들의 언어에서 오버로딩을 완전히 제거하고, 위의 내용을 유니온 타입과 실제 인자 기본값으로 대체했다. 예를 들면:

```java
// 유니온 타입
void sort(int[]|long[] a) { ... }

// 기본 인자 값
IntStream stream(int[] array,
    int startInclusive = 0,
    int endInclusive = array.length) { ... }
```

Ceylon에 대한 더 많은 정보는 "Java에 있었으면 하는 Ceylon 언어 기능 Top 10"을 읽어보라. Java에서는 안타깝게도 유니온 타입이나 인자 기본값을 사용할 수 없다. 그래서 API 소비자들에게 편의 메서드를 제공하기 위해 오버로딩을 사용해야 한다. 하지만 메서드 인자가 함수형 인터페이스라면, 메서드 오버로딩과 관련하여 Java 7과 Java 8 사이에 상황이 극적으로 변했다. 여기 JavaFX의 예시가 있다.

## JavaFX의 "불친절한" ObservableList

JavaFX는 JDK 컬렉션 타입들을 "관찰 가능(observable)"하게 만들어 확장한다. JDK 1.0과 Swing 이전 시대의 공룡 타입인 `Observable`과 혼동하지 말라. JavaFX 자체의 `Observable`은 본질적으로 다음과 같다:

```java
public interface Observable {
  void addListener(InvalidationListener listener);
  void removeListener(InvalidationListener listener);
}
```

그리고 다행히도 이 `InvalidationListener`는 함수형 인터페이스다:

```java
@FunctionalInterface
public interface InvalidationListener {
  void invalidated(Observable observable);
}
```

이것은 훌륭하다. 왜냐하면 다음과 같은 것들을 할 수 있기 때문이다:

```java
Observable awesome =
    FXCollections.observableArrayList();
awesome.addListener(fantastic -> splendid.cheer());
```

불행히도, 우리가 아마도 할 것 같은 일을 하면 상황이 더 복잡해진다. 즉, `Observable`을 선언하는 대신에, 훨씬 더 유용한 `ObservableList`로 선언하고 싶을 것이다:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();
awesome.addListener(fantastic -> splendid.cheer());
```

그런데 이제 두 번째 줄에서 컴파일 오류가 발생한다:

```
awesome.addListener(fantastic -> splendid.cheer());
//      ^^^^^^^^^^^
// The method addListener(ListChangeListener<? super String>)
// is ambiguous for the type ObservableList<String>
```

왜냐하면 본질적으로:

```java
public interface ObservableList<E>
extends List<E>, Observable {
    void addListener(ListChangeListener<? super E> listener);
}
```

그리고:

```java
@FunctionalInterface
public interface ListChangeListener<E> {
    void onChanged(Change<? extends E> c);
}
```

이제 다시, Java 8 이전에 두 리스너 타입은 완전히 모호하지 않게 구별 가능했고, 여전히 그렇다. 명명된 타입을 전달함으로써 쉽게 호출할 수 있다. 우리의 원래 코드는 다음과 같이 작성하면 여전히 작동할 것이다:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();
InvalidationListener hearYe =
    fantastic -> splendid.cheer();
awesome.addListener(hearYe);
```

또는:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();
awesome.addListener((InvalidationListener)
    fantastic -> splendid.cheer());
```

또는 심지어:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();
awesome.addListener((Observable fantastic) ->
    splendid.cheer());
```

이 모든 방법들은 모호성을 제거할 것이다. 하지만 솔직히, 람다를 명시적으로 타입 지정하거나 인자 타입을 지정해야 한다면 람다는 절반밖에 멋지지 않다. 우리는 자동 완성을 수행하고 컴파일러 자체만큼이나 타입을 추론하는 데 도움을 줄 수 있는 현대적인 IDE를 가지고 있다. 만약 우리가 정말로 다른 `addListener()` 메서드, 즉 ListChangeListener를 받는 메서드를 호출하고 싶다면 어떨까. 다음 중 하나를 작성해야 할 것이다:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();

// 아. 여기서 "String"을 반복해야 한다는 것을 기억하라
ListChangeListener<String> hearYe =
    fantastic -> splendid.cheer();
awesome.addListener(hearYe);
```

또는:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();

// 아. 여기서 "String"을 반복해야 한다는 것을 기억하라
awesome.addListener((ListChangeListener<String>)
    fantastic -> splendid.cheer());
```

또는 심지어:

```java
ObservableList<String> awesome =
    FXCollections.observableArrayList();

// 뭐야... "extends" String?? 하지만 이것이 필요한 것이다...
awesome.addListener((Change<? extends String> fantastic) ->
    splendid.cheer());
```

## 오버로딩하지 말지어다. 조심해야 하느니라.

API 설계는 어렵다. 이전에도 어려웠고, 이제 더 어려워졌다. Java 8에서는 API 메서드의 인자 중 하나라도 함수형 인터페이스라면, 해당 API 메서드를 오버로딩하기 전에 두 번 생각하라. 그리고 오버로딩을 진행하기로 결론지었다면, 이것이 정말 좋은 생각인지 세 번째로 다시 생각하라. 납득이 안 되는가? JDK를 자세히 살펴보라. 예를 들어 `java.util.stream.Stream` 타입을 보라. 동일한 수의 함수형 인터페이스 인자를 가지고, 그 인자들이 다시 동일한 수의 메서드 인자를 받는 (우리의 이전 `addListener()` 예시처럼) 오버로딩된 메서드를 몇 개나 볼 수 있는가? 없다. 오버로드 인자 수가 다른 오버로드들이 있다. 예를 들어:

```java
<R> R collect(Supplier<R> supplier,
              BiConsumer<R, ? super T> accumulator,
              BiConsumer<R, R> combiner);

<R, A> R collect(Collector<? super T, A, R> collector);
```

`collect()`를 호출할 때 어떤 모호성도 갖지 않을 것이다. 하지만 인자 수가 다르지 않고, 인자 자체의 메서드 인자 수도 다르지 않을 때, 메서드 이름이 다르다. 예를 들어:

```java
<R> Stream<R> map(Function<? super T, ? extends R> mapper);
IntStream mapToInt(ToIntFunction<? super T> mapper);
LongStream mapToLong(ToLongFunction<? super T> mapper);
DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);
```

이제, 이것은 호출 지점에서 매우 짜증나는 일이다. 왜냐하면 관련된 다양한 타입을 기반으로 어떤 메서드를 사용해야 하는지 미리 생각해야 하기 때문이다. 하지만 이것이 이 딜레마에 대한 유일한 해결책이다. 그러므로 기억하라: 람다와 오버로딩을 적용하면 후회할 것이다!

이 글이 마음에 들었는가? 다음 글들도 좋아할 것이다:

- 좋고 규칙적인 API를 설계하는 방법
- Java 코딩 시 10가지 미묘한 모범 사례
- DRY 유지하기: 메서드 오버로딩
