# 그대의 메서드를 "Equals"라 이름짓지 말지어다

> 원문: https://blog.jooq.org/thou-shalt-not-name-thy-method-equals/

(물론, 진짜로 [`Object.equals()`](http://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-)를 오버라이드하는 경우는 예외입니다). 저는 사용자 Frank가 올린 꽤 흥미로운 [Stack Overflow 질문](https://stackoverflow.com/q/28563304/521799)을 우연히 발견했습니다:

> 왜 Java의 Area#equals 메서드는 Object#equals를 오버라이드하지 않나요?

흥미롭게도, [`Area.equals(Area)`](http://docs.oracle.com/javase/8/docs/api/java/awt/geom/Area.html#equals-java.awt.geom.Area-) 메서드는 `Object.equals()`에서 선언된 `Object` 인자 대신 `Area` 인자를 받습니다. 이로 인해 Frank가 발견한 것처럼 꽤 불쾌한 동작이 발생합니다:

```java
@org.junit.Test
public void testEquals() {
    java.awt.geom.Area a = new java.awt.geom.Area();
    java.awt.geom.Area b = new java.awt.geom.Area();
    assertTrue(a.equals(b)); // -> true

    java.lang.Object o = b;
    assertTrue(a.equals(o)); // -> false
}
```

기술적으로는 AWT의 Area가 이런 방식으로 구현된 것이 맞습니다(`hashCode()`도 구현되어 있지 않으니까요). 하지만 Java가 메서드를 해석하는 방식과 프로그래머들이 위와 같이 작성된 코드를 이해하는 방식을 고려하면, equals 메서드를 오버로드하는 것은 정말 끔찍한 아이디어입니다.

## static equals도 안 됩니다

이 규칙들은 static `equals()` 메서드에도 똑같이 적용됩니다. 예를 들어 [Apache Commons Lang](http://commons.apache.org/proper/commons-lang/)의

```java
ObjectUtils.equals(Object o1, Object o2)
```

같은 경우가 있습니다.

여기서 혼란이 발생하는 이유는 이 equals 메서드를 static import 할 수 없기 때문입니다:

```java
import static org.apache.commons.lang.ObjectUtils.equals;
```

이제 다음과 같이 작성하면:

```java
equals(obj1, obj2);
```

컴파일러 에러가 발생합니다:

> The method equals(Object) in the type Object is not applicable for the arguments (..., ...)

이렇게 되는 이유는 현재 클래스와 그 상위 타입의 스코프에 있는 메서드들이 항상 이런 방식으로 import한 것들을 가리기(shadow) 때문입니다. 다음 예제도 마찬가지로 동작하지 않습니다:

```java
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

public class Test {
  void test() {
    defaultIfNull(null, null);
    // ^^ 여기서 컴파일 에러 발생
  }

  void defaultIfNull() {
  }
}
```

[자세한 내용은 이 Stack Overflow 질문을 참고하세요](https://stackoverflow.com/q/7890853/521799).

## 결론

결론은 간단합니다. `Object`에 선언된 메서드들은 _절대로_ 오버로드하지 마세요 (오버라이드는 물론 괜찮습니다). 여기에는 다음이 포함됩니다:

* `clone()`
* `equals()`
* `finalize()`
* `getClass()`
* `hashCode()`
* `notify()`
* `notifyAll()`
* `toString()`
* `wait()`

물론, 애초에 이 메서드들이 `Object`에 선언되지 않았다면 좋았겠지만, 그 배는 20년 전에 이미 떠났습니다.
