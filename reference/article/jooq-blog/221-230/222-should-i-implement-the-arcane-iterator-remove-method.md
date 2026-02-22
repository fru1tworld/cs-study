> 원문: https://blog.jooq.org/should-i-implement-the-arcane-iterator-remove-method-yes-you-probably-should/

# 난해한 Iterator.remove() 메서드를 구현해야 할까? 네, (아마도) 구현해야 합니다

## 개요

이 글에서는 가변 컬렉션(mutable collection)을 위한 커스텀 `Iterator` 구현에 `remove()` 메서드를 포함해야 하는지에 대해 다룹니다.

## 핵심 내용

### Iterator.remove()가 하는 일

이 메서드는 반복(iteration) 중에 요소를 제거할 수 있게 해줍니다. 예를 들어, 개발자는 컬렉션에서 null 값을 필터링할 수 있습니다:

```java
Iterator<Integer> it = collection.iterator();
while (it.hasNext())
    if (it.next() == null)
        it.remove();
```

최신 Java에서는 `Collection.removeIf()`가 더 깔끔한 대안으로 제공됩니다.

### 왜 구현해야 하는가

권장 사항은 간단합니다: 컬렉션이 가변(mutable)이라면 `remove()`를 구현하세요. 그 이유는? "`Collection.removeIf()`의 기본 구현은 모든 컬렉션 타입에서 범용적으로 동작하기 위해 정확히 `Iterator.remove()` 메서드에 의존합니다."

JDK의 기본 `removeIf()` 구현은 내부적으로 `Iterator.remove()`를 사용합니다. 이를 구현하지 않으면, 커스텀 컬렉션이 이 표준 API 메서드와 제대로 동작하지 않습니다.

### 더 넓은 원칙

한 댓글 작성자는 인터페이스의 모든 메서드를 구현하는 것(작업을 차단해야 할 정당한 이유가 없다면)이 기존 코드와의 더 나은 상호 운용성(interoperability)을 보장한다고 언급했습니다.

## 결론

`Iterator.remove()`가 난해한 기능처럼 보일 수 있지만, 가변 커스텀 컬렉션에서 이를 구현하면 이에 의존하는 표준 Java API와의 호환성이 보장됩니다.
