# Java 컬렉션 API의 기이한 점들

> 원문: https://blog.jooq.org/java-collections-api-quirks/

2013년 3월 17일 lukaseder 작성

우리는 Java 컬렉션 API에 대해 모든 것을 알고 있다고 생각하는 경향이 있습니다. List, Set, Map, Iterable, Iterator를 자유자재로 다룹니다. Java 8의 컬렉션 API 개선 사항도 준비되어 있습니다. 하지만 가끔씩, JDK의 깊은 곳과 긴 하위 호환성의 역사에서 비롯된 이상한 기이한 점들을 마주치게 됩니다. 수정 불가능한(unmodifiable) 컬렉션을 살펴보겠습니다.

## 수정 불가능한 컬렉션

컬렉션이 수정 가능한지 아닌지는 컬렉션 API에 반영되어 있지 않습니다. 수정 가능한 하위 타입이 확장할 수 있는 불변 List, Set, Collection 기본 타입이 없습니다. 따라서 다음과 같은 API는 JDK에 존재하지 않습니다:

```java
// 컬렉션 API의 불변 부분
public interface Collection {
  boolean  contains(Object o);
  boolean  containsAll(Collection<?> c);
  boolean  isEmpty();
  int      size();
  Object[] toArray();
  <T> T[]  toArray(T[] array);
}

// 컬렉션 API의 수정 가능한 부분
public interface MutableCollection
extends Collection {
  boolean  add(E e);
  boolean  addAll(Collection<? extends E> c);
  void     clear();
  boolean  remove(Object o);
  boolean  removeAll(Collection<?> c);
  boolean  retainAll(Collection<?> c);
}
```

Java 초기에 이런 방식으로 구현되지 않은 데에는 이유가 있었을 것입니다. 아마도 수정 가능성(mutability)은 타입 계층 구조에서 자체 타입을 차지할 만큼 가치 있는 기능으로 여겨지지 않았을 것입니다. 그래서 `unmodifiableList()`, `unmodifiableSet()`, `unmodifiableCollection()` 등의 유용한 메서드를 가진 Collections 헬퍼 클래스가 등장했습니다. 하지만 수정 불가능한 컬렉션을 사용할 때 주의하세요! Javadoc에 매우 이상한 내용이 언급되어 있습니다:

> "반환된 컬렉션은 hashCode와 equals 연산을 백업 컬렉션에 전달하지 않고, Object의 equals와 hashCode 메서드에 의존합니다. 이것은 백업 컬렉션이 set이나 list인 경우에 이러한 연산의 계약을 유지하기 위해 필요합니다."

"이러한 연산의 계약을 유지하기 위해". 상당히 모호합니다. 그 이면의 이유는 무엇일까요? 다음 Stack Overflow 답변에서 좋은 설명을 찾을 수 있습니다:

> "UnmodifiableList는 UnmodifiableCollection이지만, 그 반대는 참이 아닙니다 - List를 감싸는 UnmodifiableCollection은 UnmodifiableList가 아닙니다. 따라서 동일한 List a를 감싸는 UnmodifiableCollection과 동일한 List a를 감싸는 UnmodifiableList를 비교하면, 두 래퍼는 동일하지 않아야 합니다. 만약 감싸진 리스트에 그대로 전달했다면, 둘은 동일할 것입니다."

이 추론은 맞지만, 그 결과는 다소 예상치 못할 수 있습니다.

## 핵심 요점

핵심은 `Collection.equals()`에 의존할 수 없다는 것입니다. `List.equals()`와 `Set.equals()`는 잘 정의되어 있지만, `Collection.equals()`는 신뢰하지 마세요. 의미 있게 동작하지 않을 수 있습니다. 메서드 시그니처에서 Collection을 받을 때 이것을 명심하세요:

```java
public class MyClass {
  public void doStuff(Collection<?> collection) {
    // 여기서 collection.equals()에 의존하지 마세요!
  }
}
```

---

## 댓글

Frisian (2013년 3월 25일 09:03)

수정 불가능한 컬렉션을 제공하지 않은 것은 의도적인 설계 결정이었습니다. equals()에 관해서, 저는 컬렉션 동등성을 사용 사례에 따라 다르다고 생각하는 경향이 있습니다. 특히 리스트의 경우에요. 때로는 JavaDoc에 정의된 '강한' equals()가 필요하고, 때로는 '약한' 것(예: 동일한 항목이지만 반드시 같은 순서일 필요는 없음)으로 충분합니다. 결과적으로 저는 두 컬렉션을 비교할 때 비교의 세부 사항을 명시적으로 만들기 위해 Comparator를 구현합니다.
결론: 기이한 점이 아니라, 하나의 해결책이 모든 상황에 맞지 않는 곳에서 신중한 결정을 한 것입니다.

lukaseder (2013년 3월 25일 09:10)

설계 결정에 대한 링크 감사합니다. 그것을 찾아보기 귀찮았거든요 :-)

저는 개인적으로 컬렉션 동등성이 기본적으로 '반복 동등성'으로 구현되어야 한다고 생각하는 경향이 있습니다. 즉, 두 컬렉션이 동일하다면 그들의 Iterator가 동일한 수의 동일한 객체를 반환해야 합니다. 하위 타입은 그런 계약을 더 세분화할 수 있지만, 현재의 List와 Set 계약의 요점을 잘 이해하지 못하겠습니다. 사실, List와 Set이 자체 타입을 가질 자격이 있다고 생각하지 않습니다. 둘 다 그냥 특정 Collection 구현일 뿐입니다. `get(int)`와 다른 메서드들은 Collection으로 올려져야 합니다. 왜냐하면 그 메서드도 반복 동작을 통해 정의될 수 있기 때문입니다... 하지만 이것은 제 생각일 뿐이고, 약 20년 늦었네요 ;-)

Comparator 아이디어가 마음에 듭니다. 현재 상황에서 약간의 명시성은 해가 되지 않을 것입니다...
