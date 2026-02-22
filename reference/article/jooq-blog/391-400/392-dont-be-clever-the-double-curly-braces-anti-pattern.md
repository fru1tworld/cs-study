# "영리"하지 마라: 이중 중괄호 안티 패턴

> 원문: https://blog.jooq.org/dont-be-clever-the-double-curly-braces-anti-pattern/

작성자: Lukas Eder

---

누군가가 이중 중괄호 안티 패턴(이중 중괄호 초기화라고도 불림)에 대한 글을 올렸습니다. 이것이 왜 안티 패턴인지 한번 살펴보겠습니다.

먼저, 이중 중괄호 초기화가 무엇인지 보겠습니다:

```java
Map source = new HashMap(){{
    put("firstName", "John");
    put("lastName", "Smith");
    put("organizations", new HashMap(){{
        put("0", new HashMap(){{
            put("id", "1234");
        }});
        put("abc", new HashMap(){{
            put("id", "5678");
        }});
    }});
}};
```

어떤 사람들은 위의 코드가 Java에서 JSON과 유사한 데이터 구조를 생성하는 "깔끔한" 방법처럼 보인다고 생각할 수 있습니다.

하지만 절대 그렇지 않습니다!

## 구문적으로 무슨 일이 일어나는가?

위의 코드가 실제로 무엇을 하는지 이해해 봅시다. 두 가지 요소를 살펴보겠습니다:

### 익명 클래스

우리는 `HashMap`을 확장하는 익명 클래스를 생성하고 있습니다. 다음 코드를 보세요:

```java
new HashMap() {
}
```

이것은 `HashMap`의 새로운 서브클래스를 생성하고, 거기에 어떤 메서드도 오버라이드하거나 추가하지 않습니다. 그리 흥미롭지 않죠!

### 인스턴스 초기화자

위의 익명 클래스에 인스턴스 초기화자를 추가하고 있습니다:

```java
new HashMap() {{
    put("firstName", "John");
    put("lastName", "Smith");
}}
```

인스턴스 초기화자에 있는 코드는 생성자가 호출된 후에 실행됩니다. 따라서 `HashMap`에 대해 `super()`가 호출된 후에 두 개의 항목을 삽입하게 됩니다. 기술적으로는 작동합니다.

## 이것이 왜 안티 패턴인가

위의 코드가 무엇을 하는지 알았으니, 이제 이것이 왜 안티 패턴인지 살펴보겠습니다. 세 가지 이유가 있습니다:

### 1. 가독성

가장 덜 중요한 이유입니다: 가독성은 항상 취향의 문제입니다. 하지만 대부분의 사람들은 위의 코드가 몇 개의 일반적인 `put()` 문장으로 작성하는 것보다 더 읽기 어렵다는 데 동의할 것입니다. 또한 위의 구조가 코드처럼 보이지만 실제로는 JSON처럼 보이는 데이터이기 때문에 가독성 측면에서 꽤 불쾌합니다.

### 2. 클래스 수

위의 코드를 실행하면 여러 개의 익명 클래스가 생성됩니다. 예를 들어:

```
Test$1$1$1.class
Test$1$1$2.class
Test$1$1.class
Test$1.class
Test.class
```

익명 `HashMap` 클래스를 많이 사용할수록 컴파일러가 더 많은 클래스 파일을 생성하고, 이 모든 파일들은 클래스 로더에 의해 로드되어야 합니다. 이는 순수한 오버헤드입니다.

### 3. 메모리 누수

가장 중요한 이유입니다! 익명 클래스는 자신이 둘러싸인 인스턴스에 대한 참조를 유지합니다. 다음 코드를 보세요:

```java
public class ReallyHeavyObject {

    // 많은 메모리를 사용하는 필드들

    public Map quickHarmlessMethod() {
        Map source = new HashMap(){{
            put("firstName", "John");
            put("lastName", "Smith");
            put("organizations", new HashMap(){{
                put("0", new HashMap(){{
                    put("id", "1234");
                }});
                put("abc", new HashMap(){{
                    put("id", "5678");
                }});
            }});
        }};

        return source;
    }
}
```

반환된 `Map`은 이제 둘러싸고 있는 `ReallyHeavyObject` 인스턴스에 대한 참조를 포함하게 됩니다. 이 참조를 바로 보지는 못하겠지만, 존재합니다. 지금 아무도 그것에 대해 생각하지 않을 것입니다. 하지만 미래의 유지보수 개발자가 나중에 `Map`을 어딘가에 저장하기 위해 리팩토링할 수 있습니다. 예를 들어 캐시에 저장할 수 있습니다.

갑자기 `ReallyHeavyObject`가 가비지 컬렉션에서 제외됩니다!

소프트웨어의 "노후화"에 대해 이야기할 때, 이것이 일어나는 일 중 하나입니다. 사람들은 더 이상 원래의 맥락을 이해하지 못하고, 발생하지 말아야 할 일들이 발생합니다.

## 결론

"영리"하지 마세요. 절대로 이중 중괄호 초기화를 사용하지 마세요.

---

## 대안

이중 중괄호 초기화 대신 다음 대안들을 사용하세요:

- 전통적인 방식: 여러 문장으로 명시적으로 초기화
  ```java
  Map map = new HashMap();
  map.put("firstName", "John");
  map.put("lastName", "Smith");
  ```

- Guava 사용: `ImmutableMap.of()` 또는 `ImmutableMap.Builder`
  ```java
  Map map = ImmutableMap.of("firstName", "John", "lastName", "Smith");
  ```

- Java 9+: `Map.ofEntries()` 또는 `Map.of()`
  ```java
  Map map = Map.of("firstName", "John", "lastName", "Smith");
  ```
