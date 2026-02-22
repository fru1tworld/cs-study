# 중첩 타입의 Top 5 사용 사례

> 원문: https://blog.jooq.org/top-5-use-cases-for-nested-types/

2015년 2월 2일 lukaseder 작성

얼마 전 reddit에서 정적 내부 클래스(Static Inner Classes)에 대한 흥미로운 토론이 있었습니다. 먼저, 기본적인 Java 역사 지식을 간단히 복습해 봅시다. Java 언어는 네 가지 수준의 클래스 중첩을 제공하며, 여기서 "Java 언어"라고 말하는 것은 이러한 구조가 단순한 "문법적 설탕(syntax sugar)"이기 때문입니다. JVM에는 이런 것이 존재하지 않으며, JVM은 오직 일반 클래스만 알고 있습니다.

## (정적) 중첩 클래스

```java
class Outer {
    static class Inner {
    }
}
```

이 경우, `Inner`는 공유되는 공통 네임스페이스를 제외하면 `Outer`와 완전히 독립적입니다.

## 내부 클래스

```java
class Outer {
    class Inner {
    }
}
```

이 경우, `Inner` 인스턴스는 자신을 감싸는 `Outer` 인스턴스에 대한 암시적 참조를 가집니다. 다시 말해, 연관된 `Outer` 인스턴스 없이는 `Inner` 인스턴스가 존재할 수 없습니다. 이러한 인스턴스를 생성하는 Java 방식은 다음과 같습니다:

```java
Outer.Inner yikes = new Outer().new Inner();
```

완전히 어색해 보이는 것이 사실 많은 의미가 있습니다. `Outer` 내부 어딘가에서 `Inner` 인스턴스를 생성하는 것을 생각해 보세요:

```java
class Outer {
    class Inner {
    }

    void somewhereInside() {
        // 우리는 이미 Outer의 스코프 안에 있습니다.
        // Inner를 명시적으로 한정할 필요가 없습니다.
        Inner aaahOK;

        // 이것이 우리가 익숙하게 작성하는 방식입니다.
        aaahOK = new Inner();

        // 모든 다른 로컬 스코프 메서드처럼, 우리는
        // "this"에서 역참조하여 Inner 생성자에
        // 접근할 수 있습니다. 우리는 단지
        // "this"를 거의 작성하지 않을 뿐입니다.
        aaahOK = this.new Inner();
    }
}
```

`public`이나 `abstract` 키워드처럼, `static` 키워드는 중첩 인터페이스에서 암시적이라는 점에 주목하세요. 다음의 가상 문법이 처음에는 익숙해 보일 수 있지만...:

```java
class Outer {
    <non-static> interface Inner {
        default void doSomething() {
            Outer.this.doSomething();
        }
    }

    void doSomething() {}
}
```

...위의 코드를 작성하는 것은 불가능합니다. `<non-static>` 키워드가 없다는 것 외에, "내부 인터페이스"가 불가능해야 하는 명백한 이유는 없어 보입니다. 저는 평소처럼 추측합니다 - 하위 호환성 및/또는 다중 상속과 관련된 정말 극단적인 경우의 주의사항이 있어서 이것을 막고 있을 것입니다.

## 로컬 클래스

```java
class Outer {
    void somewhereInside() {
        class Inner {
        }
    }
}
```

로컬 클래스는 아마도 Java에서 가장 덜 알려진 기능 중 하나일 것입니다. 왜냐하면 이에 대한 용도가 거의 없기 때문입니다. 로컬 클래스는 스코프가 감싸는 메서드까지만 확장되는 명명된 타입입니다. 명백한 사용 사례는 그 메서드 내에서 그러한 타입을 여러 번 재사용하고 싶을 때입니다. 예를 들어, JavaFX 애플리케이션에서 여러 개의 비슷한 리스너를 구성할 때가 있습니다.

## 익명 클래스

```java
class Outer {
    Serializable dummy = new Serializable() {};
}
```

익명 클래스는 단 하나의 인스턴스만 있는 다른 타입의 서브타입입니다.

## 중첩 클래스의 Top 5 사용 사례

익명, 로컬, 내부 클래스는 모두 정적 컨텍스트에서 정의되지 않는 한 감싸는 인스턴스에 대한 참조를 유지합니다. 이러한 클래스의 인스턴스가 자신의 스코프 밖으로 누출되면 많은 문제가 발생할 수 있습니다. 그 문제에 대해 더 알고 싶다면 우리 기사를 읽어보세요: "영리하게 굴지 마세요: 이중 중괄호 안티 패턴(Don't be Clever: The Double Curly Braces Anti Pattern)." 하지만 종종 감싸는 인스턴스로부터 이득을 얻고 싶을 때가 있습니다. 실제 구현을 공개하지 않고 반환할 수 있는 일종의 "메시지" 객체를 갖는 것이 꽤 유용할 수 있습니다:

```java
class Outer {

    // 이 구현은 private입니다...
    private class Inner implements Message {
        @Override
        public void getMessage() {
            Outer.this.someoneCalledMe();
        }
    }

    // ... 하지만 Message 타입으로
    // 반환할 수 있습니다
    Message hello() {
        return new Inner();
    }

    void someoneCalledMe() {}
}
```

하지만 (정적) 중첩 클래스의 경우, `Inner` 인스턴스가 어떤 `Outer` 인스턴스와도 완전히 독립적이기 때문에 감싸는 스코프가 없습니다. 그렇다면 최상위 레벨 타입 대신 이러한 중첩 클래스를 사용하는 것의 요점은 무엇일까요?

### 1. 외부 타입과의 연관

전 세계에 "이 (내부) 타입은 이 (외부) 타입과 완전히 관련되어 있으며, 그 자체로는 의미가 없다"고 전달하고 싶다면 타입들을 중첩할 수 있습니다. 예를 들어, `Map`과 `Map.Entry`가 이렇게 되어 있습니다:

```java
public interface Map<K, V> {
    interface Entry<K, V> {
    }
}
```

### 2. 외부 타입의 바깥에서 숨기기

패키지(기본) 가시성이 타입에 충분하지 않다면, 감싸는 타입과 감싸는 타입의 모든 다른 중첩 타입에서만 사용 가능한 `private static` 클래스를 만들 수 있습니다. 이것이 정말로 정적 중첩 클래스의 주요 사용 사례입니다.

```java
class Outer {
    private static class Inner {
    }
}

class Outer2 {
    Outer.Inner nope;
}
```

### 3. 보호된 타입

이것은 정말 드문 사용 사례이지만, 클래스 계층 내에서 특정 타입의 서브타입에서만 사용 가능하게 만들고 싶은 타입이 필요할 때가 있습니다. 이것이 `protected static` 클래스의 사용 사례입니다:

```java
class Parent {
    protected static class OnlySubtypesCanSeeMe {
    }

    protected OnlySubtypesCanSeeMe someMethod() {
        return new OnlySubtypesCanSeeMe();
    }
}

class Child extends Parent {
    OnlySubtypesCanSeeMe wow = someMethod();
}
```

### 4. 모듈 에뮬레이션

Ceylon과 달리 Java는 일급 모듈이 없습니다. Maven이나 OSGi를 사용하면 Java의 빌드(Maven) 또는 런타임(OSGi) 환경에 일부 모듈형 동작을 추가할 수 있지만, 코드에서 모듈을 표현하고 싶다면 이것은 실제로 불가능합니다. 그러나 정적 중첩 클래스를 사용하여 관례에 의해 모듈을 설정할 수 있습니다. `java.util.stream` 패키지를 살펴봅시다. 이것을 모듈로 간주할 수 있으며, 이 모듈 내에는 내부 `java.util.stream.Nodes` 클래스와 같은 몇 가지 "하위 모듈" 또는 타입 그룹이 있습니다. 대략 다음과 같이 생겼습니다:

```java
final class Nodes {
    private Nodes() {}
    private static abstract class AbstractConcNode {}
    static final class ConcNode {
        static final class OfInt {}
        static final class OfLong {}
    }
    private static final class FixedNodeBuilder {}
    // ...
}
```

이 `Nodes`의 일부는 모든 `java.util.stream` 패키지에서 사용 가능하므로, 이렇게 작성된 방식으로 우리는 다음과 같은 것을 가지고 있다고 말할 수 있습니다:

- `java.util.stream` "모듈"에만 보이는 합성 `java.util.stream.nodes` 하위 패키지
- 역시 `java.util.stream` "모듈"에만 보이는 몇 가지 `java.util.stream.nodes.*` 타입들
- 합성 `java.util.stream.nodes` 패키지의 몇 가지 "최상위 레벨" 함수들(정적 메서드)

제게는 Ceylon처럼 많이 보입니다!

### 5. 미용적 이유

마지막 부분은 다소 지루합니다. 또는 어떤 사람들은 흥미로울 수 있습니다. 이것은 취향, 또는 작성의 용이함에 관한 것입니다. 일부 클래스는 너무 작고 중요하지 않아서 다른 클래스 안에 작성하는 것이 더 쉽습니다. `.java` 파일을 절약합니다. 왜 안 되겠습니까.

## 결론

Java 8 시대에, Java 언어의 매우 오래된 기능들에 대해 생각하는 것이 극도로 흥미진진하지는 않을 수 있습니다. 정적 중첩 클래스는 몇 가지 틈새 사용 사례에 대해 잘 이해된 도구입니다. 하지만 이 기사에서 가져가야 할 것은 이것입니다. 클래스를 중첩할 때마다, 감싸는 인스턴스에 대한 참조가 절대적으로 필요하지 않다면 반드시 `static`으로 만드세요. 그 참조가 언제 프로덕션에서 당신의 애플리케이션을 폭파시킬지 모릅니다.
