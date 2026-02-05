# API 메서드 오버로딩에 주의하라

> 원문: https://blog.jooq.org/overload-api-methods-with-care/

메서드 오버로딩은 API 설계에서 강력한 개념이며, 특히 API가 플루언트 API(fluent API)이거나 DSL(Domain Specific Language)인 경우에 더욱 그렇다. jOOQ가 바로 이런 경우인데, 라이브러리와 다양한 방식으로 상호작용하기 위해 동일한 메서드 이름을 자주 사용하고자 하기 때문이다.

## 예제: jOOQ Condition

```java
package org.jooq;

public interface Condition {

    // "AND" 연산의 다양한 오버로딩 형태:
    Condition and(Condition other);
    Condition and(String sql);
    Condition and(String sql, Object... bindings);

    // [...]
}
```

이 모든 메서드는 "AND" 연산자를 사용하여 두 조건을 서로 연결한다. 이상적으로 구현체들은 서로 의존하여 단일 장애 지점(single point of failure)을 만든다. 이렇게 하면 DRY 원칙을 유지할 수 있다:

```java
package org.jooq.impl;

abstract class AbstractCondition implements Condition {

    // 단일 장애 지점
    @Override
    public final Condition and(Condition other) {
        return new CombinedCondition(
            Operator.AND, Arrays.asList(this, other));
    }

    // 다른 메서드에 위임하는 "편의 메서드"
    @Override
    public final Condition and(String sql) {
        return and(condition(sql));
    }

    @Override
    public final Condition and(String sql, Object... bindings) {
        return and(condition(sql, bindings));
    }
}
```

## 제네릭과 오버로딩의 문제

Eclipse로 개발할 때, Java 5의 세계는 실제보다 더 화려해 보인다. 가변 인자(varargs)와 제네릭(generics)은 Java 5에서 문법적 설탕(syntactic sugar)으로 도입되었다. 이들은 실제로 JVM에는 그런 형태로 존재하지 않는다. 즉, 컴파일러가 메서드 호출을 올바르게 연결하고, 필요한 경우 타입을 추론하며, 경우에 따라 합성 메서드를 생성해야 한다. JLS(Java Language Specification)에 따르면, 오버로딩된 메서드에 가변 인자/제네릭이 사용될 때 많은 모호성이 발생한다.

### 제네릭에 대해 자세히 살펴보자:

jOOQ에서 유용한 기능 중 하나는 상수 값을 필드와 동일하게 취급하는 것이다. 많은 곳에서 필드 인자는 다음과 같이 오버로딩된다:

```java
// 이것은 편의 메서드이다:
public static <T> Field<T> myFunction(Field<T> field, T value) {
    return myFunction(field, val(value));
}

// 이 메서드와 동등하다.
public static <T> Field<T> myFunction(Field<T> field, Field<T> value) {
    return MyFunction<T>(field, value);
}
```

위 코드는 대부분의 경우 매우 잘 동작한다. 위 API를 다음과 같이 사용할 수 있다:

```java
Field<Integer> field1  = //...
Field<String>  field2  = //...

Field<Integer> result1 = myFunction(field1, 1);
Field<String>  result2 = myFunction(field2, "abc");
```

하지만 `<T>`가 `Object`에 바인딩될 때 문제가 발생한다!

```java
// 이것은 동작하지만...
Field<Object>  field3  = //...
Field<Object>  result3 = myFunction(field3, new Object());

// ... 이것은 동작하지 않는다!
Field<Object>  field4  = //...
Field<Object>  result4 = myFunction(field4, field4);
Field<Object>  result4 = myFunction(field4, (Field) field4);
Field<Object>  result4 = myFunction(field4, (Field<Object>) field4);
```

`<T>`가 `Object`에 바인딩되면, 갑자기 두 메서드 모두 적용 가능해지며, JLS에 따르면 어느 쪽도 더 구체적(more specific)이지 않다! Eclipse 컴파일러는 보통 좀 더 관대하여(이 경우 직관적으로 두 번째 메서드에 연결하지만), javac 컴파일러는 이 호출을 어떻게 처리해야 할지 알지 못한다. 그리고 이를 우회할 방법이 없다. field4를 `Field`나 `Field<Object>`로 캐스팅하여 링커가 두 번째 메서드에 연결하도록 강제할 수 없다. 이것은 API 설계자에게 상당히 나쁜 소식이다. 이 특수한 경우에 대한 더 자세한 내용은 다음 Stack Overflow 질문을 참고하라. 나는 이것을 Oracle과 Eclipse 양쪽에 버그로 보고했다. 어느 컴파일러 구현이 올바른지 지켜보자: https://stackoverflow.com/questions/5361513/reference-is-ambiguous-with-generics

## 정적 임포트와 가변 인자의 문제

API 설계자에게는 더 많은 문제가 있으며, 이에 대해서는 다른 기회에 문서화하겠다.
