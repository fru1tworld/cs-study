# Java 8 디펜더 메서드에서 "Super"를 호출하는 문법

> 원문: https://blog.jooq.org/syntax-for-calling-super-in-java-8-defender-methods/

Java 8에 새롭게 도입될 디펜더 메서드(defender method)에 대한 논의가 활발하게 진행되고 있습니다. 디펜더 메서드는 인터페이스에 기본 구현을 제공할 수 있는 기능으로, 현재는 "디폴트 메서드(default method)"라고도 불립니다.

## 문제 상황

클래스/인터페이스 계층 구조에서 구현된 인터페이스의 디폴트 메서드를 참조하는 방법에 대한 문제가 제기되었습니다.

## 예제 상황

다음과 같은 시나리오를 살펴보겠습니다:

```java
interface K {
  int m() default { return 88; }
}

interface J extends K {
  int m() default { return K.super.m(); }
                        // ^^^^^^^^^^^^ 이것을 어떻게 표현해야 할까?
}
```

핵심 질문은 이것입니다: 자식 인터페이스에서 부모 인터페이스의 디폴트 메서드를 오버라이딩할 때, 부모의 디폴트 메서드를 어떤 문법으로 참조해야 할까요?

## 제안된 해결책들

이 목적을 달성하기 위해 여러 가지 문법이 제안되었습니다:

- `K.super.m()`
- `super.K.m()`
- `((K) super).m()`
- `K::m()`
- `K.default.m()`
- `super<K>.m()`
- `super(K).m()`
- `super(K.class).m()`
- `super[K].m()`

## 맥락

이 논의는 OpenJDK 람다 개발 메일링 리스트에서 시작되었으며, Java 언어 설계자들이 복잡한 상속 시나리오에서 디폴트 메서드 호출을 처리하는 최선의 접근 방식에 대해 활발하게 토론하고 있었습니다.

---

참고: 최종적으로 Java 8에서는 `K.super.m()` 문법이 채택되었습니다. 이 문법을 사용하면 특정 인터페이스의 디폴트 메서드를 명시적으로 호출할 수 있습니다.
