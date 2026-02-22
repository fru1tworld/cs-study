# Java에서 private final 필드를 수정하는 지저분한 해킹 기법

> 원문: https://blog.jooq.org/a-dirt-ugly-hack-to-modify-private-final-fields-in-java/

우리 모두는 때때로 리플렉션을 사용합니다. Java의 `Field.setAccessible()`과 유사한 메서드를 통해 가시성(visibility)을 조작하기도 합니다. 하지만 이 글은 한 단계 더 나아가 Java에서 private (static) final 필드를 수정하는 방법을 보여줍니다.

이 도구를 선택할 때는 두 번 생각하세요 ;-)

---

참고: 이 글은 Sebastian Zarnekow의 외부 블로그 글 ["Java Hacks - Changing Final Fields"](https://zarnekow.blogspot.com/2013/01/java-hacks-changing-final-fields.html)를 소개하는 내용입니다.

## 핵심 개념

### 인스턴스 final 필드 수정

Java의 리플렉션 API를 사용하면 `setAccessible()` 메서드를 통해 가시성 제한을 우회할 수 있습니다. 인스턴스 필드의 경우, final로 선언되어 있어도 값을 수정할 수 있습니다.

### static final 필드 수정

static final 필드의 경우 과정이 더 복잡합니다. 단순히 `setAccessible()`을 호출하는 것만으로는 변경이 불가능하며, JVM이 `IllegalAccessException`을 던집니다.

해결책은 `Field.modifiers`에 리플렉션을 사용하여 final 수정자를 제거한 뒤, 이후에 값을 변경하는 것입니다.

### 중요한 제한 사항

이 기법은 컴파일러가 호출 지점에 직접 인라인하는 리터럴(숫자 또는 문자열 리터럴)로 초기화된 static final 필드에는 작동하지 않습니다. 하지만 로거나 싱글톤과 같은 다른 일반적인 static 타입은 런타임에 수정할 수 있습니다.

### 주의 사항

멀티스레딩에 대한 우려가 있습니다. final 필드는 volatile이 아니기 때문에, Java 메모리 모델은 다른 스레드가 변경 사항을 관찰한다는 것을 보장하지 않습니다. 따라서 이 "기능"은 가능하면 피하는 것이 좋습니다.

---

태그: final, hacks, java, reflection

게시일: 2013년 1월 30일
