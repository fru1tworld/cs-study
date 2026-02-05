# Java 8 금요일: Java 8의 어두운 면

> 원문: https://blog.jooq.org/java-8-friday-the-dark-side-of-java-8/

Data Geekery에서는 Java 8에 매우 흥분하고 있습니다. 우리는 우리 자신이 Java 8에 대해 생각하도록 훈련시켜 왔으며, 이 블로그에서 가능하면 매주 금요일 "Java 8 금요일" 시리즈를 통해 Java 8의 장점들을 공유해 왔습니다.

하지만 모든 좋은 것에는 어두운 면이 있습니다. 이번 글에서는 Java 8에서 그다지 훌륭하지 않은 부분들, 즉 더 나아질 수 있었거나 실제로 혼란스러운 부분들을 살펴보겠습니다.

## 1. 오버로딩이 더 나빠졌다

오버로딩, 제네릭, 가변인자는 결코 친구가 아니었습니다. 하지만 다음과 같은 경우에는 더 악화됩니다. `Callable`과 `Supplier`를 받는 메서드를 오버로딩한다고 가정해 보겠습니다:

```java
void run(Callable<Integer> c) throws Exception {
    System.out.println(c.call());
}

void run(Supplier<Integer> s) {
    System.out.println(s.get());
}
```

이제 람다 인자로 이 메서드들을 간단히 호출할 수 없습니다:

```java
// 컴파일되지 않음
run(() -> 42);
```

컴파일러는 어떤 오버로드를 사용해야 할지 결정할 수 없습니다. 따라서 명시적 캐스팅을 사용해야 합니다:

```java
run((Callable<Integer>) () -> 42);
run((Supplier<Integer>) () -> 42);
```

또는 더 장황한 익명 클래스 구문으로 돌아가야 합니다:

```java
run(new Callable<Integer>() {
    @Override
    public Integer call() {
        return 42;
    }
});
```

실제로 이것은 jOOQ API에서 상당히 파멸적인데, jOOQ는 `Field<T>` 타입과 `T` 타입으로 많은 메서드를 오버로딩했기 때문입니다. Java 8에서 SAM(Single Abstract Method) 타입인 `Field<T>`는 이러한 오버로딩 문제에 취약합니다.

## 2. 디폴트 메서드의 한계

### final 한정자 제한

디폴트 메서드는 `final`로 선언할 수 없습니다. 이는 API 설계자가 오버라이드되어서는 안 되는 봉인된 편의 메서드를 만들 수 없게 합니다.

```java
interface Foo {
    // 이렇게 할 수 없음!
    default final void bar() {
        // 절대 변경되어서는 안 되는 기본 구현
    }
}
```

### synchronized 한정자 생략

`synchronized` 키워드는 인터페이스 디폴트 메서드에서 허용되지 않아, 인터페이스 수준에서 계약 강제를 제한합니다.

```java
interface Foo {
    // 이렇게 할 수 없음!
    default synchronized void bar() {
        // 스레드 안전이 보장되어야 하는 기본 구현
    }
}
```

### "default" 키워드 불규칙성

`default` 키워드의 필요성은 추상 클래스와 비교할 때 일관성이 없어 보입니다. 추상 클래스에서는 메서드 본문만으로도 추상 여부가 결정됩니다. "있어야 할 모습"은 불필요한 키워드를 완전히 제거하는 것일 것입니다.

```java
// 현재 방식
interface Foo {
    default void bar() {}
}

// 더 나을 수 있었던 방식
interface Foo {
    void bar() {}
}
```

## 3. 디폴트 메서드가 거의 구현되지 않았다

`List`와 같은 컬렉션 인터페이스에는 `equals()`와 `hashCode()` 메서드에 대한 합리적인 디폴트 구현이 없어, 커스텀 컬렉션 구현을 단순화할 기회를 놓쳤습니다.

예를 들어, 두 리스트가 같은지 비교하는 것은 매우 단순합니다 - 그들이 같은 요소를 같은 순서로 포함하고 있는지 확인하면 됩니다. 이것은 인터페이스에서 쉽게 디폴트 메서드로 구현될 수 있었습니다:

```java
interface List<E> extends Collection<E> {
    @Override
    default boolean equals(Object o) {
        // 표준 리스트 동등성 구현
        if (o == this) return true;
        if (!(o instanceof List)) return false;
        // ...나머지 구현
    }

    @Override
    default int hashCode() {
        // 표준 리스트 해시코드 구현
        int hashCode = 1;
        for (E e : this)
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        return hashCode;
    }
}
```

## 4. 함수형 인터페이스 명명 혼란

`java.util.function` 패키지는 일관성 없는 명명을 통해 "기묘한 현 상태"를 보여줍니다:

### 기본형 타입의 특별 대우

`int`, `long`, `double`은 특별한 주목을 받지만, `byte`, `short`, `float`, `char`에 대한 기본형 변형은 누락되어 있습니다.

### 비논리적인 타입 이름 변형

타입 이름이 비논리적으로 변합니다:
- `Consumer` - 값을 소비하고 아무것도 반환하지 않음
- `Supplier` - 값을 공급(반환)함
- `Predicate` - boolean을 반환함
- `Function` - 하나의 인자를 받아 값을 반환함
- `UnaryOperator` - 같은 타입의 인자를 받아 반환함
- `BinaryOperator` - 같은 타입의 두 인자를 받아 같은 타입을 반환함
- `BiFunction` - 두 인자를 받아 값을 반환함
- `BiConsumer` - 두 값을 소비함

### Boolean의 이등 시민 대우

Boolean은 `BooleanSupplier` 또는 `Predicate`로 나타나는 "이등 시민"입니다.

### 결정 트리의 복잡성

이 기사는 개발자가 자신의 함수 시그니처에 어떤 타입 이름이 적용되는지 기억하는 데 직면하는 복잡성을 보여주는 결정 트리를 제공합니다:

| 반환 타입 | 인자 없음 | 인자 1개 | 인자 2개 |
|----------|----------|---------|---------|
| void | Runnable | Consumer | BiConsumer |
| T | Supplier | UnaryOperator/Function | BinaryOperator/BiFunction |
| boolean | BooleanSupplier | Predicate | BiPredicate |
| int | IntSupplier | ToIntFunction/IntUnaryOperator | ToIntBiFunction/IntBinaryOperator |
| long | LongSupplier | ToLongFunction/LongUnaryOperator | ToLongBiFunction/LongBinaryOperator |
| double | DoubleSupplier | ToDoubleFunction/DoubleUnaryOperator | ToDoubleBiFunction/DoubleBinaryOperator |

이 모든 것을 기억해야 합니다!

## 결론

Java 8은 강력한 기능들을 도입하지만, 이 글은 "Stack Overflow가 곧 혼란스러운 프로그래머들의 질문으로 폭발할 것"이라고 강조합니다. 이 언어는 학습과 사용 패턴을 복잡하게 만드는 "기묘한" 설계 결정들을 유지하고 있습니다.

람다 표현식과 디폴트 메서드가 아무리 훌륭하더라도, 레거시와의 이전 버전 호환성 유지, 다양한 API 설계 결정 간의 일관성 부족, 그리고 새로운 개념들(특히 함수형 인터페이스)의 명명 규칙 복잡성은 Java 8을 배우고 마스터하는 것을 어렵게 만듭니다.

모든 새로운 언어 기능과 마찬가지로, 신중하게 사용하고 그 한계를 이해하는 것이 중요합니다. Java 8의 어두운 면을 인식하면 그 밝은 면을 더 효과적으로 활용할 수 있을 것입니다.
