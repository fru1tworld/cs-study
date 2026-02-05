# Java 8 금요일: API 설계자여, 조심하라

> 원문: https://blog.jooq.org/java-8-friday-api-designers-be-careful/

2014년 5월 16일, lukaseder 작성

Data Geekery에서 우리는 Java를 사랑합니다. jOOQ의 유창한 API와 쿼리 DSL에 깊이 투자한 개발자로서, 우리는 Java 8이 우리 생태계에 가져올 잠재력에 흥분하고 있습니다.

## Java 8 금요일

매주 금요일, 우리는 람다 표현식과 확장 메서드를 활용한 새로운 튜토리얼 스타일의 Java 8 기능을 선보입니다. 소스 코드는 GitHub에서 확인할 수 있습니다.

## 간결한 함수형 API 설계

Java 8은 API 설계를 더 흥미롭게 만들었지만 동시에 더 도전적으로 만들었습니다. 성공적인 API 설계자는 이제 객체 지향적 측면과 함께 함수형 측면도 고려해야 합니다. 다음과 같은 메서드만 제공하는 것 대신에:

```java
void performAction(Parameter parameter);
object.performAction(new Parameter(...));
```

지연 평가를 위한 함수형 오버로드를 제공하는 것을 고려하세요:

```java
void performAction(Parameter parameter);
void performAction(Supplier<Parameter> parameter);
object.performAction(() -> new Parameter(...));
```

### JDK 의존성

`Supplier` 타입은 JDK 8 전용입니다. 이전 Java 버전을 지원하려면 `Callable`을 사용하세요 (Java 5부터 사용 가능):

```java
void performAction(Callable<Parameter> parameter);
object.performAction(() -> new Parameter(...));
```

`Callable`은 람다 표현식과 중첩 클래스에서 checked 예외를 던지는 것을 허용합니다.

### 오버로딩 주의사항

`Parameter`와 `Supplier<Parameter>`를 오버로딩하는 것은 잘 작동하지만, 유사한 오버로드는 피하세요:

```java
void performAction(Supplier<Parameter> parameter);
void performAction(Callable<Parameter> parameter);
```

람다 표현식은 이들을 구별할 수 없어서, 클라이언트 코드가 람다를 효과적으로 사용하는 것을 방해합니다.

### void 호환 vs 값 호환

JLS §15.27.2는 "void 호환(void-compatible)"과 "값 호환(value-compatible)" 람다 표현식을 구분합니다. `Consumer`와 `Function` 타입을 안전하게 오버로드할 수 있습니다:

```java
interface Consumer<T> {
    void accept(T t);  // void 호환
}

interface Function<T, R> {
    R apply(T t);      // 값 호환
}
```

이러한 호출은 모호하지 않습니다:

```java
run(i -> {});      // run(Consumer)만 적용됨
run(i -> 1);       // run(Function)만 적용됨
```

jOOQ 3.4의 트랜잭션 API가 이를 우아하게 보여줍니다:

```java
ctx.transaction(c -> {
    DSL.using(c).insertInto(...).execute();
});

Integer result = ctx.transaction(c -> {
    DSL.using(c).update(...).execute();
    return 42;
});
```

그러나 JDK 1.8.0_05와 Eclipse Kepler 기준으로, 이 모호성 해결에는 버그가 있습니다:
- JDK-8029718
- Eclipse 434642

안전을 위해, 오버로딩을 완전히 피하는 것을 고려하세요.

### 제네릭 메서드는 SAM이 아니다

제네릭 메서드를 가진 단일 추상 메서드(SAM) 인터페이스는 람다 대상으로 적합하지 않습니다:

```java
interface NotASAM {
    <T> void run(T t);
}
```

JLS §15.27.3에 따르면, 람다 표현식은 "함수 타입에 타입 매개변수가 없어야 한다"라는 조건을 요구합니다.

## 지금 해야 할 것

API 설계자는 Java 8을 사용하여 단위 테스트와 통합 테스트를 작성해야 합니다. 이러한 언어 기능은 매우 미묘합니다 - 올바르게 사용하려면 연습과 회귀 테스트가 필요합니다. 메서드를 오버로딩하기 전에, 람다로 원래 메서드를 호출하는 기존 클라이언트 코드를 깨뜨리지 않을지 확인하세요.
