# Java 레거시는 계속 증가하고 있다

> 원문: https://blog.jooq.org/the-java-legacy-is-constantly-growing/

JDK API를 사용할 때 가끔 매우 흥미로운 것들을 발견할 수 있습니다. `Class.getConstructors()` 메서드를 살펴보겠습니다. 그 시그니처는 다음과 같습니다:

```java
Constructor<?>[] getConstructors()
```

그런데 흥미롭게도, `Class.getConstructor(Class...)` 메서드는 다음과 같은 시그니처를 가지고 있습니다:

```java
Constructor<T> getConstructor(Class<?>... parameterTypes)
```

왜 `getConstructors()`가 `Constructor<T>[]`가 아닌 `Constructor<?>[]`를 반환할까요?

## Javadoc의 설명

Javadoc에는 다음과 같이 설명되어 있습니다:

> "이 메서드가 `Constructor<T>` 객체들의 배열(즉, 이 클래스의 생성자들의 배열)을 반환하지만, 이 메서드의 반환 타입은 예상할 수 있는 `Constructor<T>[]`가 아니라 `Constructor<?>[]`입니다. 이렇게 덜 구체적인 반환 타입이 필요한 이유는, 이 메서드에서 반환된 후에 배열이 다른 클래스들의 Constructor 객체들을 담도록 수정될 수 있기 때문이며, 이는 `Constructor<T>[]`의 타입 보장을 위반하게 됩니다."

와우, 잠시 생각해 보겠습니다.

## Java 1.0 / Oak: 배열

배열은 Java 1.0(Oak의 후속)에서 도입되었습니다. 배열은 Collections API보다 수년 앞서 존재했습니다(Collections API는 Java 1.2에서 도입됨). 배열은 공변적(covariant)입니다. 이것은 컴파일 타임에 잡을 수 없는 런타임 문제를 야기합니다:

```java
Object[] objects = new String[1];
objects[0] = Integer.valueOf(1); // ArrayStoreException 런타임 에러
```

## Java 1.1: 리플렉션 API

리플렉션 API가 Java 1.1에서 도입되었을 때, `Class.getConstructors()` 메서드는 `Constructor[]`를 반환했습니다. Collections API가 없었기 때문에 이것은 합리적인 선택이었습니다. 하지만 이것은 다음과 같은 작업을 가능하게 했습니다:

```java
Constructor[] constructors = String.class.getConstructors();
constructors[0] = Object.class.getConstructor();
```

물론, 위의 코드는 `constructors` 배열이 결국 외부로 유출될 수 있고, 누군가 그 배열이 오직 `String` 생성자들만 포함한다고 잘못 가정하고 반복 작업을 수행할 수 있기 때문에 실제로 문제가 됩니다.

## Java 1.2: Collections API

Java 1.2에서 Collections API가 도입되었을 때, 이것은 리플렉션 API를 재설계할 수 있는 기회였습니다. 하지만 이미 너무 늦었습니다. 하위 호환성이 Java의 제1원칙이기 때문입니다. 만약 Collections API가 먼저 도입되었더라면, 아마도 다음과 같이 되었을 것입니다:

```java
List<Constructor> getConstructors()
```

컬렉션 기반 접근 방식은 훨씬 더 좋았을 것입니다. 왜냐하면 컬렉션은 배열과 달리 불변(immutable)으로 만들 수 있기 때문입니다.

## Java 1.5: 제네릭

Java 1.5에서 제네릭이 도입되었을 때, `Constructor[]`에서 `Constructor<?>[]`로의 변경이 위에서 문서화된 이유로 이루어졌습니다. 대안으로 `List<Constructor<T>> getConstructors()`와 같은 것이 훨씬 더 나았을 것입니다.

## 결론

Java는 이러한 종류의 주의사항들로 가득 차 있습니다. 대부분의 것들은 Javadoc에 문서화되어 있습니다. 많은 것들은 Stack Overflow에 문서화되어 있습니다. 그것들을 배우고, 수년간의 설계 결정들로 인해 겸손하게 성장한 언어를 받아들여야 합니다. Brian Goetz가 "Stewardship: the Sobering Parts"에서 이에 대해 이야기했습니다. 그의 요점: 이런 것들을 수십 년 동안 유지하는 것은 매우 어렵습니다. 그래서 Java 레거시는 계속 증가하고 있습니다.
