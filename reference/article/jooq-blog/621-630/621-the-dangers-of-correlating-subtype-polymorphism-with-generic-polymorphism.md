# 서브타입 다형성과 제네릭 다형성을 연관시키는 것의 위험성

> 원문: https://blog.jooq.org/the-dangers-of-correlating-subtype-polymorphism-with-generic-polymorphism/

Java 5에서 제네릭 다형성이 도입되었습니다. 이것은 Java 언어에 훌륭한 추가 기능이었습니다. 우리 모두 알고 있듯이, 제네릭은 공변성(covariance)을 허용하여 복잡한 타입 시스템을 가능하게 합니다:

```java
// 이것은 컴파일됩니다
List<? extends Number> c = new ArrayList<Integer>();
```

물론, 공변성과 반공변성(contravariance)에 대한 규칙은 항상 직관적이지 않으며, 올바르게 적용하기까지 시간이 걸립니다. 그러나 공변성과 함께 제네릭 다형성을 사용하면 제네릭 인자로 서브타입 다형성을 사용할 수 있습니다.

서브타입과 제네릭의 공변성을 고려해보세요:

```java
<E extends Serializable> void serialize(
    Collection<E> collection) {}
```

위 메서드는 Collection 타입의 임의의 서브타입과 제네릭 타입 인자가 `Serializable`의 임의의 서브타입인 제네릭 타입 인자를 받습니다. 따라서 다음을 전달할 수 있습니다:

- `Collection<Serializable>`
- `Collection<Integer>`
- `ArrayList<Serializable>`
- `ArrayList<Integer>`
- 기타 등등...

지금까지는 좋습니다.

## 서브타입 다형성과 제네릭 다형성을 연관시키기

타입을 만들 때 상황이 흥미로워집니다:

```java
class IntegerList extends ArrayList<Integer> {}
class StringSet extends HashSet<String> {}
```

위와 같이 서브타입 다형성과 제네릭 다형성을 연관시킬 수 있습니다. 이것은 때때로 타이핑을 줄이거나 구체적인 타입이 필요한 와일드카드를 피하기 위해 수행됩니다. 하지만 드문 경우에만 좋은 아이디어입니다. 명시적 타입의 수가 폭발적으로 증가하기 때문입니다. 서브타입과 제네릭 타입 계층의 데카르트 곱을 확장하기 시작하면 말입니다.

## 연관을 제네릭하게 만들기

때때로 연관은 제네릭하게 만들어집니다. 다음 예제를 고려해보세요:

```java
// AnyContainer는 AnyObject를 포함할 수 있습니다
class AnyContainer<E extends AnyObject> {}
class AnyObject {}

// PhysicalContainer는 PhysicalObject만 포함합니다
class PhysicalContainer<E extends PhysicalObject>
  extends AnyContainer<E> {}
class PhysicalObject extends AnyObject {}

// FruitContainer는 Fruit만 포함하며,
// Fruit는 PhysicalObject입니다
class FruitContainer<E extends Fruit>
  extends PhysicalContainer<E> {}
class Fruit extends PhysicalObject {}
```

위 예제에서 서브타입 다형성과 제네릭 다형성은 연관되어 있지만 여전히 제네릭합니다.

## 재귀적 제네릭 매개변수

서브타입이 자신의 타입을 인식해야 하는 경우가 있습니다. 이것은 `Comparable`을 구현하거나 공변적 반환 타입을 사용하는 경우 전형적입니다. 위 예제를 재귀적 제네릭 매개변수를 사용하여 다시 작성해보겠습니다:

```java
// AnyContainer는 AnyObject를 포함할 수 있습니다
class AnyContainer<E extends AnyObject<E>> {}
class AnyObject<O extends AnyObject<O>> {}

// PhysicalContainer는 PhysicalObject만 포함합니다
class PhysicalContainer<E extends PhysicalObject<E>>
  extends AnyContainer<E> {}
class PhysicalObject<O extends PhysicalObject<O>>
  extends AnyObject<O> {}

// FruitContainer는 Fruit만 포함하며,
// Fruit는 PhysicalObject입니다
class FruitContainer<E extends Fruit<E>>
  extends PhysicalContainer<E> {}
class Fruit<O extends Fruit<O>>
  extends PhysicalObject<O> {}
```

이것은 시스템에 추가적인 타입 안전성을 제공합니다. 이 접근 방식은 `java.lang.Enum`에서도 볼 수 있습니다:

```java
public class Enum<E extends Enum<E>>
implements Comparable<E> {
  public final int compareTo(E other) { ... }
  public final Class<E> getDeclaringClass() { ... }
}

enum MyEnum {}

// 이것은 다음의 구문 설탕입니다:
final class MyEnum extends Enum<MyEnum> {}
```

## 위험은 어디에 있는가?

위 예제에서 한 가지를 발견했을 것입니다. 생성된 `MyEnum` 클래스는 `final`입니다. 대부분의 경우, 재귀적 자기 연관의 종료는 `final` 클래스에서만 수행되어야 합니다.

다음 두 예제를 비교해보세요:

```java
// "위험함"
class Apple extends Fruit<Apple> {}

// "안전함"
final class Apple extends Fruit<Apple> {}
```

왜 final이 아닌 `Apple`이 "위험"할까요? 확장된 `AnyObject` 계약을 고려해보세요:

```java
class AnyObject<O extends AnyObject<O>>
  implements Comparable<O> {

  @Override
  public int compareTo(O other) { ... }
  public AnyContainer<O> container() { ... }
}
```

위 계약에서, `compareTo()` 메서드는 `Comparable<O>`에서 상속됩니다. `container()` 메서드는 공변적 반환 타입을 사용합니다. 이 계약이 의미하는 바는 `AnyObject`의 모든 서브타입은 동일한 서브타입과만 비교될 수 있다는 것입니다. 또한 컨테이너의 공변적 반환 타입 덕분에 `AnyObject` 서브타입의 컨테이너에 대한 참조를 얻을 수 있습니다.

그런데, `Apple`이 final이 아니면 어떻게 될까요? 다음을 가질 수 있습니다:

```java
class GoldenDelicious extends Apple {}
class Gala extends Apple {}
```

이제 다음이 가능합니다:

```java
GoldenDelicious g1 = new GoldenDelicious();
Gala g2 = new Gala();

// 이것은 의도된 것이 아니었습니다!
g1.compareTo(g2);
```

컨테이너의 경우도 마찬가지입니다:

```java
class Fruit<O extends Fruit<O>>
  extends PhysicalObject<O> {

  @Override
  public FruitContainer<O> container() { ... }
}

GoldenDelicious g = new GoldenDelicious();

// 컨테이너가 Apple 타입입니다. GoldenDelicious가 아닙니다!
FruitContainer<Apple> c = g.container();
```

## 결론

서브타입 다형성과 제네릭 다형성은 직교하는 타입 축을 확장합니다. 이들을 연관시키는 것은 타입 시스템의 설계 냄새(design smell)가 될 수 있습니다. 동일한 타입에서 이들을 연관시키는 것은 위험합니다. 올바르게 하기 어렵기 때문입니다. 특히 재귀적 제네릭 타입 정의를 만드는 경우, 사용자들은 여러분의 기본 타입의 서브타입에서 재귀적 제네릭 타입 정의를 종료하려고 할 것이며, 그것은 일반 클래스나 인터페이스가 아닌 `final` 클래스에서만 수행되어야 합니다. 재귀적 제네릭 타입 정의가 정말로 올바른 선택인지 다시 한번 신중하게 생각해보세요.
