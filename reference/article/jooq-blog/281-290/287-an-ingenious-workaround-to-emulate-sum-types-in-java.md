# Java에서 유니온 타입의 응용을 에뮬레이션하는 기발한 해결 방법

> 원문: https://blog.jooq.org/an-ingenious-workaround-to-emulate-sum-types-in-java/

저는 최근 jOOQ와 jOOλ에서 튜플에 `forEach()` 메서드를 구현하고 싶었습니다. 아이디어는 다음과 같이 작성하는 것입니다:

```java
tuple(1, "a", null).forEach(System.out::println);
```

그러면 다음과 같이 출력됩니다:

```
1
a
null
```

매우 쉽습니다. 하지만 `forEach()` 메서드의 인자 타입은 무엇이 되어야 할까요? 메서드 시그니처는 다음과 같을 것입니다:

```java
void forEach(Consumer<???> consumer)
```

간단히 `Consumer<Object>`를 사용할 수도 있지만, 이는 좋지 않습니다. 타입 안전하지 않기 때문입니다. 예를 들어 `Tuple2<Integer, Long>`이 있다면, 최소한 `Consumer<Number>`를 전달하여 튜플 요소에 대해 `Number.doubleValue()`와 같은 메서드를 호출할 수 있어야 합니다. 물론, 튜플 멤버로 `String`을 추가하면 `Consumer<Object>`로 다운그레이드되어야 합니다.

제가 정말 원했던 것은 유니온 타입(Union types)에 대한 지원이었습니다.

유니온 타입은 합 타입(Sum types) 또는 태그된 유니온(Tagged unions)이라고도 불리며, [대수적 데이터 타입](https://en.wikipedia.org/wiki/Algebraic_data_type)에서 알려져 있습니다. 저는 대략 다음과 같이 작성하고 싶었습니다:

```java
// 이것은 유효한 Java가 아닙니다
<T super T1 | T2 | ... | TN>
```

이것은 "타입 집합의 공통 상위 타입"에 대해 일종의 패턴 매칭을 수행하는 것을 의미합니다.

반공변(contravariant) 하한선과 관련하여 Java에서 유니온 타입은 실제로 예외의 다중 catch 블록 외부에서는 지원되지 않습니다. 예외 처리에서는 다음과 같이 작성할 수 있습니다:

```java
try {
    // ...
}
catch (E1 | E2 | E3 e) {
    // 이렇게 하면 E1, E2, E3의 공통 상위 타입에 대해 패턴 매칭됩니다
    // 예를 들어 모두 RuntimeException을 확장한다면,
    // e의 타입은 RuntimeException이 됩니다
}
```

하지만 메서드 시그니처에서는 사용할 수 없습니다.

## 해결 방법

Daniel Dietrich([vavr](https://github.com/vavr-io/vavr) 라이브러리의 저자)가 먼저 이 아이디어를 가지고 있었습니다. [2016년 2월 16일 트윗](https://twitter.com/danieldietrich/status/699595013000552448)에서 Daniel은 다음과 같이 제안했습니다:

> "@lukaseder static 메서드로 시도해 보세요 `<T, T1 extends T, ... Tn extends T> Seq<T> toSeq(T1 t1, ..., Tn tn) { ... }`"

기발합니다! 정적 메서드를 사용하면 제네릭 타입 제약 조건이 단순히 "반대 방향으로" 지정됩니다.

왜냐하면 `T1 extends T`이면, 필연적으로 `T super T1`이기 때문입니다.

이것이 작동하는 방식을 살펴보겠습니다:

```java
static <T, T1 extends T, T2 extends T, T3 extends T>
void forEach(
    Tuple3<T1, T2, T3> tuple,
    Consumer<? super T> consumer
) {
    consumer.accept(tuple.v1);
    consumer.accept(tuple.v2);
    consumer.accept(tuple.v3);
}
```

`T1`, `T2`, `T3`가 모두 `T`를 확장(extend)하도록 제한함으로써, 컴파일러는 최소 상계(Least Upper Bound, LUB)를 추론합니다. 이것은 공통 상위 타입입니다.

예를 들어:

```java
Tuple2<Integer, Long> t = tuple(1, 2L);

// 컴파일됨!
// T는 Number로 추론되고,
// Number에는 doubleValue() 메서드가 있습니다
forEach(t, c -> System.out.println(c.doubleValue()));
```

컴파일러는 `T`를 `Number`로 추론합니다. 왜냐하면 `Integer`와 `Long` 모두 `Number`를 확장하기 때문입니다. 따라서 consumer는 `Number` 타입의 요소를 받을 수 있고, `doubleValue()` 메서드를 안전하게 호출할 수 있습니다.

이제 `String`을 추가하면 어떻게 될까요?

```java
Tuple3<Integer, Long, String> t = tuple(1, 2L, "a");

// 컴파일 오류!
// T는 Object로 추론되고,
// Object에는 doubleValue() 메서드가 없습니다
forEach(t, c -> System.out.println(c.doubleValue()));
```

예상대로 `String`을 추가하면 컴파일이 실패합니다. `Integer`, `Long`, `String`의 최소 상계는 `Object`이고, `Object`에는 `doubleValue()` 메서드가 없기 때문입니다.

## 결론

이 기법은 Java의 타입 추론과 제네릭 바운드를 비전통적인 방식으로 활용합니다. 컴파일러가 공통 상위 타입을 발견하고 타입 안전성을 강제하도록 하면서, 유니온 타입과 유사한 동작을 시뮬레이션할 수 있습니다.

이 기법은 Daniel이 vavr의 패턴 매칭 API에서 사용한다고 알려져 있습니다.

참고로 [Benji Weber](https://benjiweber.co.uk/blog/2014/10/13/anonymous-intersection-types-in-java/)도 Java에서 익명 교차 타입(Anonymous Intersection Types)에 대해 관련 작업을 수행했습니다.
