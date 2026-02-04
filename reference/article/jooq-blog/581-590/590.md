# Java 코딩 시 10가지 미묘한 모범 사례

> 원문: https://blog.jooq.org/10-subtle-best-practices-when-coding-java/

이것은 "명백한" 것들에 대한 목록이 아닙니다. "변수에 대해 모든 곳에서 final을 사용하라", "인터페이스 대신 구체적인 구현체를 사용하지 말라", 또는 가장 논쟁적인 "코드에 주석을 달아라" 같은 것들을 이야기하는 것이 아닙니다.

이 글은 아마도 여러분이 이전에 들어보지 못했을 수도 있는, 좀 더 미묘한 주제들에 대해 다룹니다. 적어도, 우리가 항상 생각해보지는 않을 주제들입니다.

(저는 4년간 jOOQ를 유지보수하면서 이러한 것들에 민감해졌습니다. jOOQ는 많은 내부 API와 클라이언트 코드가 정말 모범적으로 동작하길 원하는 공개 API를 가진 복잡한 라이브러리입니다)

이 블로그 게시물은 다음 관련 게시물과 함께 읽으면 좋습니다:

- Java 코딩 시 10가지 미묘한 모범 사례
- 더 나은 API를 작성하기 위한 10가지 팁

## 1. C++ 소멸자를 기억하라

C++ 소멸자를 기억하시나요? 아니요? 운이 좋으시네요. C++로 메모리 누수가 없는 프로그램을 작성하는 것은 정말 고통스러웠습니다. 하지만 소멸자의 좋은 점도 기억하세요: 할당의 역순으로 메모리를 정리해야 했습니다. 이것은 Java에서도 중요합니다!

잘 설계된 API에서는 "소멸자" 의미론 개념이 여기저기서 나타납니다:

- JUnit의 `@Before` 및 `@After` 어노테이션
- JDBC `Connection`, `Statement`, `ResultSet`의 열기/닫기
- `InputStream`, `OutputStream`의 열기/닫기
- 이벤트 리스너 등록/해제

구체적인 예를 살펴보겠습니다. 테스트 수명주기 후크를 구현할 때:

```java
@Before
public void before() {
    // 리소스 1 설정
    // 리소스 2 설정
    // 리소스 3 설정
}

@After
public void after() {
    // 리소스 3 정리 (역순!)
    // 리소스 2 정리
    // 리소스 1 정리
}
```

super 메서드를 호출할 때도 마찬가지입니다:

```java
@Override
public void beforeMethod() {
    super.beforeMethod(); // 먼저 부모 설정
    // 자식 설정
}

@Override
public void afterMethod() {
    // 자식 정리
    super.afterMethod(); // 마지막에 부모 정리
}
```

이런 규칙을 따르지 않으면 미묘한 버그, 리소스 누수, 심지어 교착 상태까지 발생할 수 있습니다.

## 2. 초기 SPI 진화 판단을 믿지 말라

API(Application Programming Interface)는 애플리케이션이 라이브러리와 상호작용하는 방식입니다. SPI(Service Provider Interface)는 라이브러리가 애플리케이션과 상호작용하는 방식입니다. API를 설계하는 것도 어렵지만, SPI를 설계하는 것은 훨씬 더 어렵습니다.

이런 SPI 메서드를 제공한다고 가정해 봅시다:

```java
public interface EventListener {
    void onEvent(Event event);
}
```

시간이 지나면서 새로운 이벤트 유형이 추가되고, 이제 청취자가 어떤 유형의 이벤트를 수신할지 제어하고 싶다고 가정합니다. 다음과 같이 하고 싶을 수 있습니다:

```java
public interface EventListener {
    void onEvent(Event event);

    // 새로운 메서드 추가
    boolean wantsEvent(EventType type);
}
```

하지만 이것은 기존 구현을 깨뜨립니다! 처음부터 다음과 같이 했어야 합니다:

```java
public interface EventListener {
    void onEvent(EventContext context);
}

public interface EventContext {
    Event getEvent();
    // 나중에 새로운 메서드를 추가할 수 있음
    // EventType[] getWantedEventTypes();
}
```

컨텍스트/파라미터 객체를 사용하면 API를 확장하면서도 기존 구현을 깨뜨리지 않을 수 있습니다. 항상 여러분의 SPI 메서드를 단일 인자 메서드로 설계하고, 그 인자는 컨텍스트/파라미터 객체여야 합니다.

## 3. 익명, 로컬, 또는 내부 클래스를 반환하지 말라

Swing 개발자들은 아마 손가락 관절이 다 닳도록 익명 클래스를 작성했을 것입니다. 하지만 메서드에서 익명, 로컬, 또는 내부 클래스를 반환하면 외부 인스턴스에 대한 암시적 참조를 유지하게 됩니다.

```java
public Runnable getAction() {
    // 이 익명 클래스는 외부 인스턴스에 대한 참조를 유지합니다
    return new Runnable() {
        @Override
        public void run() {
            // ...
        }
    };
}
```

이것은 다음과 같은 문제를 일으킬 수 있습니다:
- 메모리 누수: 반환된 객체가 외부 객체의 수명보다 오래 지속되면, 외부 객체가 가비지 컬렉션되지 않습니다
- 직렬화 문제: 외부 객체가 `Serializable`이 아니면 직렬화가 실패합니다

해결책: 정적 클래스나 최상위 클래스로 추출하세요.

```java
public Runnable getAction() {
    return new MyRunnable();
}

// 정적 중첩 클래스 - 외부 참조 없음
private static class MyRunnable implements Runnable {
    @Override
    public void run() {
        // ...
    }
}
```

## 4. 지금 SAM을 작성하기 시작하라

Java 8 람다가 등장했고, 람다를 최대한 활용하려면 API가 SAM(Single Abstract Method) 인터페이스를 받아들여야 합니다.

```java
// SAM 인터페이스
public interface EventHandler {
    void handle(Event event);
}

// 이 API 메서드는 람다와 함께 사용할 수 있습니다
public void registerHandler(EventHandler handler) {
    // ...
}

// 사용 예
registerHandler(event -> System.out.println(event));
```

SAM을 설계할 때 몇 가지 규칙:
- 메서드는 하나의 추상 메서드만 가져야 합니다
- 인자를 하나만 받는 것이 이상적입니다 (하나의 컨텍스트 객체)
- 이렇게 하면 클라이언트 코드가 훨씬 깔끔해집니다

## 5. API 메서드에서 null을 반환하지 말라

null에 대해 많은 글이 쓰여졌지만, 다시 한번 강조하겠습니다. API 메서드에서 null을 반환하면 클라이언트 코드에 부담을 줍니다.

나쁜 예:
```java
public String getMessage() {
    if (condition) {
        return message;
    }
    return null; // 나쁨!
}
```

좋은 예:
```java
public String getMessage() {
    if (condition) {
        return message;
    }
    return ""; // 빈 문자열 반환
}

// 또는 Java 8+
public Optional<String> getMessage() {
    if (condition) {
        return Optional.of(message);
    }
    return Optional.empty();
}
```

null을 반환해야 하는 경우가 있다면, 최소한 문서화하세요. 하지만 가능하다면 빈 객체, 빈 컬렉션, 또는 `Optional`을 사용하세요.

## 6. null 배열이나 리스트를 절대 반환하지 말라

이것은 위의 규칙과 비슷하지만, 배열과 컬렉션에 특히 적용됩니다.

`java.io.File.list()`를 보세요:
```java
// File.list()는 디렉토리가 아니면 null을 반환합니다
String[] files = directory.list();
if (files != null) { // null 체크 필요!
    for (String file : files) {
        // ...
    }
}
```

이것은 정말 나쁜 설계입니다. null 대신 빈 배열을 반환했다면:
```java
String[] files = directory.list();
for (String file : files) { // null 체크 불필요
    // ...
}
```

null 배열이나 리스트를 반환하면:
- 클라이언트가 항상 null 체크를 해야 합니다
- `NullPointerException` 가능성이 있습니다
- 코드가 불필요하게 복잡해집니다

항상 빈 배열이나 빈 컬렉션을 반환하세요:
```java
public List<String> getItems() {
    if (condition) {
        return items;
    }
    return Collections.emptyList(); // null 대신 빈 리스트
}

public String[] getItems() {
    if (condition) {
        return items;
    }
    return new String[0]; // null 대신 빈 배열
}
```

## 7. 상태를 피하고, 함수형으로 작성하라

HTTP는 무상태이고, 모든 멋진 것들은 RESTful입니다. 상태 전이는 잘 정의된 이유로 수행됩니다. Java에서도 마찬가지여야 합니다.

나쁜 예 (상태 유지):
```java
public class StringTransformer {
    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public void trim() {
        this.value = this.value.trim();
    }

    public void toUpperCase() {
        this.value = this.value.toUpperCase();
    }

    public String getValue() {
        return this.value;
    }
}
```

좋은 예 (함수형):
```java
public class StringTransformer {
    public String transform(String value) {
        return value.trim().toUpperCase();
    }
}
```

또는 더 나은 예 (메서드 체이닝):
```java
public class StringTransformer {
    private final String value;

    public StringTransformer(String value) {
        this.value = value;
    }

    public StringTransformer trim() {
        return new StringTransformer(value.trim());
    }

    public StringTransformer toUpperCase() {
        return new StringTransformer(value.toUpperCase());
    }

    public String getValue() {
        return value;
    }
}

// 사용 예
String result = new StringTransformer(input)
    .trim()
    .toUpperCase()
    .getValue();
```

함수형 접근의 장점:
- 스레드 안전성
- 테스트 용이성
- 코드의 예측 가능성

## 8. equals()를 단락 평가하라

`equals()` 메서드를 구현할 때, 가장 먼저 동일성(identity) 비교를 하세요. 대규모 객체 그래프에서 성능이 크게 향상됩니다.

```java
@Override
public boolean equals(Object obj) {
    // 1. 동일성 비교 - 가장 빠름
    if (this == obj) return true;

    // 2. null 체크
    if (obj == null) return false;

    // 3. 타입 비교
    if (getClass() != obj.getClass()) return false;

    // 4. 실제 필드 비교 (가장 비용이 큼)
    MyClass other = (MyClass) obj;
    return Objects.equals(this.field1, other.field1)
        && Objects.equals(this.field2, other.field2);
}
```

처음 세 가지 검사는 매우 빠르고, 대부분의 경우 여기서 반환됩니다. 비싼 필드 비교는 정말 필요할 때만 수행됩니다.

## 9. 메서드를 기본적으로 final로 만들어 보라

어떤 분들은 동의하지 않을 수 있지만, 저는 강력히 주장합니다: 기본적으로 모든 것을 `final`로 만드세요. 클래스, 메서드, 변수.

```java
public class MyClass {
    public final void importantMethod() {
        // 이 메서드는 하위 클래스에서 오버라이드할 수 없습니다
    }
}
```

특히 정적 메서드의 경우, "오버라이딩"(실제로는 "섀도잉")은 정말 위험한 버그를 만듭니다:

```java
public class Parent {
    public static void doSomething() {
        System.out.println("Parent");
    }
}

public class Child extends Parent {
    public static void doSomething() { // 섀도잉 - 위험!
        System.out.println("Child");
    }
}

Parent p = new Child();
p.doSomething(); // "Parent"를 출력 - 놀랍지 않나요?
```

이것은 매우 혼란스럽습니다. 정적 메서드는 인스턴스가 아닌 참조 타입에 따라 호출됩니다. `final`을 사용하면 이런 실수를 방지할 수 있습니다.

## 10. method(T...) 시그니처를 피하라

가변 인자(varargs)와 제네릭을 함께 사용하면 문제가 생깁니다. 특히 오버로딩과 함께 사용할 때:

```java
public <T> void doSomething(T... args) {
    // ...
}

public void doSomething(String... args) {
    // ...
}

// 어떤 메서드가 호출될까요?
doSomething("a", "b"); // 모호함!
```

컴파일러는 어떤 메서드를 호출해야 하는지 안정적으로 결정할 수 없습니다. 이는 다음과 같은 문제를 일으킵니다:
- 컴파일 경고 또는 오류
- 예상치 못한 메서드 바인딩
- 제네릭 타입 정보 손실

대안:
```java
// 고정된 수의 파라미터 사용
public <T> void doSomething(T arg1) { ... }
public <T> void doSomething(T arg1, T arg2) { ... }
public <T> void doSomething(T arg1, T arg2, T arg3) { ... }

// 또는 리스트/배열 사용
public <T> void doSomething(List<T> args) { ... }
```

## 결론

이러한 모범 사례들은 처음에는 미묘해 보일 수 있지만, 장기적으로 유지보수 가능하고 확장 가능한 코드를 작성하는 데 큰 도움이 됩니다. 특히 라이브러리나 프레임워크를 개발할 때 이러한 원칙들을 따르면, 여러분의 API를 사용하는 다른 개발자들에게 더 나은 경험을 제공할 수 있습니다.

API 설계는 한 번 하고 끝나는 것이 아닙니다. 확장성, 테스트 용이성, 그리고 장기적인 유지보수 비용을 항상 고려해야 합니다.
