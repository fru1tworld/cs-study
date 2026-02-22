# Java 8, 9, 10에서 인터페이스 기본 메서드(Default Method)에 대한 올바른 리플렉션 접근법

> 원문: https://blog.jooq.org/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/

## 문제 개요

Java의 `Proxy` API를 인터페이스 기본 메서드(default method)와 함께 사용할 때, 개발자들은 해당 기본 메서드를 리플렉션(reflection)으로 호출하는 과정에서 어려움을 겪습니다. Stack Overflow에 제시된 해결책들은 특정 상황에서만 작동하며, 모든 Java 버전에서 동작하지 않습니다.

## 핵심 문제

프록시(proxy) 기본 메서드에 대한 단순한 리플렉션은 실패합니다:

```java
Duck duck = (Duck) Proxy.newProxyInstance(
    Thread.currentThread().getContextClassLoader(),
    new Class[] { Duck.class },
    (proxy, method, args) -> {
        method.invoke(proxy);  // 무한 재귀 발생
        return null;
    }
);
```

이 코드는 프록시와 호출 핸들러(invocation handler) 사이의 무한 재귀로 인해 중첩된 `UndeclaredThrowableException` 오류를 발생시킵니다.

## Java 버전별 해결 방법

### Java 8 해결책

작동하는 접근법은 리플렉션을 사용하여 패키지 프라이빗(package-private) `MethodHandles.Lookup` 생성자에 접근하는 것입니다:

```java
Constructor<Lookup> constructor = Lookup.class
    .getDeclaredConstructor(Class.class);
constructor.setAccessible(true);
constructor.newInstance(Duck.class)
    .in(Duck.class)
    .unreflectSpecial(method, Duck.class)
    .bindTo(proxy)
    .invokeWithArguments();
```

이 방법은 private 접근 가능한 인터페이스와 private 접근 불가능한 인터페이스 모두를 일관되게 처리하지만, Java 9 이상의 모듈 캡슐화(module encapsulation)를 위반합니다.

### Java 9 이상 해결책

`MethodHandles.privateLookupIn()` 메서드가 적절한 접근법을 제공합니다:

```java
MethodHandles.privateLookupIn(type, MethodHandles.lookup())
    .findSpecial(type, "quack",
        MethodType.methodType(void.class, new Class[0]),
        type)
    .bindTo(proxy)
    .invokeWithArguments();
```

대안으로 `Lookup.findSpecial()`도 작동합니다:

```java
MethodHandles.lookup()
    .findSpecial(type, "quack",
        MethodType.methodType(void.class, new Class[0]),
        type)
    .bindTo(proxy)
    .invokeWithArguments();
```

## 주요 발견 사항

포괄적인 테스트를 통해 버전별 복잡한 문제들이 드러났습니다:

- Java 8: `findSpecial()` 메서드는 private 접근이 불가능한 인터페이스에서 `IllegalAccessException`으로 실패합니다
- Java 9 이상: 리플렉션 기반 Lookup 생성자 접근법은 엄격한 모듈 규칙 하에서 `InaccessibleObjectException`을 발생시킵니다
- Java 9 이상: 적절한 모듈 접근이 설정되면 `privateLookupIn()`과 `findSpecial()`이 안정적으로 작동합니다

## 라이브러리 해결책

jOOR 리플렉션 라이브러리는 런타임 Java 버전에 따라 적절한 접근법을 자동으로 선택하여 이러한 복잡성을 해결합니다. 다음과 같이 단순화된 코드를 사용할 수 있습니다:

```java
Reflect.on(new Object()).as(PrivateAccessible.class).quack();
```

## 결론

단일 리플렉션 기법으로 모든 Java 버전에서 작동하는 것은 없습니다. 개발자들은 버전별 코드 경로를 유지하거나, 이러한 차이점을 추상화해주는 라이브러리를 사용해야 합니다. 이상적으로는 Proxy API 자체가 향상된 lookup 컨텍스트 처리를 통해 기본 메서드에 대한 더 나은 지원을 제공해야 합니다.
