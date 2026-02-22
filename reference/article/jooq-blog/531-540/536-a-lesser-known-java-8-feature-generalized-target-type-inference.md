# 잘 알려지지 않은 Java 8 기능: 일반화된 대상 타입 추론

> 원문: https://blog.jooq.org/a-lesser-known-java-8-feature-generalized-target-type-inference/

Java 8에서는 JEP 101을 통해 "일반화된 대상 타입 추론(Generalized Target-Type Inference)"이라는 새로운 기능이 도입되었습니다. 이 기능의 목적은 컴파일러가 제네릭 타입을 문맥에서 추론할 수 있게 하여 더 간결한 코드를 작성할 수 있도록 하는 것입니다.

## 핵심 개념

다음과 같은 제네릭 `List` 클래스가 있다고 가정해 봅시다:

```java
class List<E> {
    static <Z> List<Z> nil() { ... }
    static <Z> List<Z> cons(Z head, List<Z> tail) { ... }
    E head() { ... }
}
```

## 기존의 장황한 방식

Java 8 이전에는 개발자들이 명시적인 타입 매개변수를 제공해야 했습니다:

```java
// 타입 매개변수를 명시적으로 지정해야 함
List.cons(42, List.<Integer>nil());
String s = List.<String>nil().head();
```

## 새로운 간결한 방식

일반화된 대상 타입 추론 기능을 사용하면 컴파일러가 문맥에서 타입을 추론할 수 있으므로 다음과 같이 더 간결하게 작성할 수 있습니다:

```java
// 컴파일러가 타입을 추론
List.cons(42, List.nil());
String s = List.nil().head();
```

## 세 가지 타입 추론 시나리오

저자는 개선된 타입 추론이 적용되는 세 가지 상황을 식별했습니다:

1. 할당 기반 추론: `List<String> l = List.nil();`
2. 인자 기반 추론: `List.cons(42, List.nil());`
3. 체이닝된 메서드 호출: `String s = List.nil().head();`

## 구현의 현실

그러나 JDK 8로 실제 테스트해 본 결과, 예상과 다른 결과가 나왔습니다. 첫 번째와 두 번째 시나리오(할당 기반 추론과 인자 기반 추론)는 잘 작동하지만, 세 번째 시나리오(체이닝된 메서드 호출)는 작동하지 않았습니다.

컴파일러는 다음과 같은 오류를 발생시켰습니다:

```
error: incompatible types: Object cannot be converted to String
```

즉, 컴파일러가 `.head()` 호출의 문맥을 기반으로 `List.nil()`이 `List<String>`을 반환해야 한다는 것을 추론하지 못했습니다.

## 제한 사항의 원인

이러한 제한은 Java 타입 시스템의 복잡성에서 비롯됩니다. 체이닝된 메서드 호출을 통한 지연된 타입 추론은 구현하기가 매우 어려웠고, 이는 기술적인 실수가 아니라 Java 타입 시스템 자체의 복잡성을 반영합니다.

## 결론

일반화된 대상 타입 추론 기능은 당초 목표했던 완전한 비전을 달성하지는 못했지만, 일상적인 Java 코딩에서 의미 있는 개선을 제공합니다. 특히 jOOQ나 Streams API와 같은 플루언트 API 설계에서 유용하게 활용될 수 있습니다. 다만 완벽한 타입 추론은 여전히 어려운 과제로 남아 있습니다.

이 기능은 메서드 인자 타입 기반의 추론은 구현되어 컴파일되지만, 체이닝된 메서드 호출에 대한 타입 추론은 아직 완전히 구현되지 않았다는 점을 기억해야 합니다.
