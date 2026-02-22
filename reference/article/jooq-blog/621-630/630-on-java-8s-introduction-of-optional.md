# Java 8의 Optional 도입에 대하여

> 원문: https://blog.jooq.org/on-java-8s-introduction-of-optional/

2013년 4월 11일 lukaseder 작성

저는 최근에 JDK 8에 추가된 `Optional` 타입을 발견했습니다. Optional 타입은 `NullPointerException`을 피하기 위한 방법으로, 메서드에서 `Optional` 반환 값을 받는 API 소비자들이 실제 반환 값을 사용하기 위해 "존재 여부" 검사를 수행하도록 "강제"합니다.

"공백"과 "초기" 상태는 이미 튜링에게 알려져 있었습니다. "중립" 또는 "제로" 상태가 1800년대 에이다 러브레이스 시대로 거슬러 올라가는 배비지 엔진에서 필요했다고 주장할 수도 있습니다. 반면에 수학자들은 "아무것도 없음"과 "내부에 아무것도 없는 집합"인 "공집합"을 구분하는 것을 선호합니다. 이는 예를 들어 Scala에서 구현된 "NONE"과 "SOME"과 잘 비교됩니다.

어쨌든, 저는 Java의 `Optional`에 대해 생각해 보았습니다. Java 9가 결국 Ceylon과 유사한 문법적 설탕을 JLS에 추가하여 언어 수준에서 `Optional`을 활용하게 되더라도, 제가 이것을 좋아하게 될지 정말 확신이 서지 않습니다.

Java는 놀라울 정도로 하위 호환성을 유지하기 때문에, 기존 API들 중 어느 것도 `Optional`을 반환하도록 개조되지 않을 것입니다. 예를 들어, 다음과 같은 것은 JDK 8에 나타나지 않을 것입니다:

```java
public interface List<E> {
    Optional<E> get(int index);
    [...]
}
```

`Optional` 변수에 `null`을 할당할 수 있을 뿐만 아니라, "Optional"의 부재가 "SOME"의 의미를 보장하지 않습니다. 왜냐하면 리스트들은 여전히 "벌거벗은" `null` 값을 반환할 것이기 때문입니다. 두 가지 사고방식을 혼합하면, 하나 대신 두 번의 검사를 하게 됩니다:

```java
Optional<T> optional = // [...]
T nonOptional = list.get(index);

// 편집증적이라면, 이중 검사를 할 것입니다!
if (optional != null && optional.isPresent()) {
    // 작업 수행
}

// 여기서는 아마도 값을 신뢰할 수 없습니다
if (nonOptional != null) {
    // 작업 수행
}
```

따라서... Java의 해결책에 대해 저는 -1입니다.

### 추가 읽을거리

* Java 8에서 null 참조를 사용할 변명은 더 이상 없다
* The Java Posse User Group
* Lambda Dev 메일링 리스트 (Optional != @Nullable)
* Lambda Dev 메일링 리스트 (Optional 클래스는 단지 Value일 뿐)
