# jOOλ의 Sneaky Throw로 검사 예외 회피하기

> 원문: https://blog.jooq.org/use-jooλs-sneaky-throw-to-avoid-checked-exceptions/

## 검사 예외의 문제점

Java의 검사 예외(checked exception) 처리는 장황한 코드 패턴을 만들어냅니다. `Class.forName()`과 같은 간단한 연산도 다음과 같이 불편한 우회 방법이 필요합니다:

```java
public class Test {
    static final Class<?> klass;

    static {
        try {
            klass = Class.forName("org.h2.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
```

## jOOλ의 해결책

`Sneaky` 클래스는 JDK 함수형 인터페이스를 감싸서 검사 예외 선언을 제거하는 유틸리티 메서드를 제공합니다. 이를 통해 더 깔끔한 문법이 가능해집니다:

```java
public class Test {
    static final Class<?> klass = Sneaky.supplier(
        () -> Class.forName("org.h2.Driver")
    ).get();
}
```

예외는 있는 그대로 전파됩니다. "JVM은 예외의 검사 여부(checked-ness)를 신경 쓰지 않습니다."

## 스트림 사용 예제

다음과 같은 장황한 패턴 대신:

```java
Stream
    .generate(
        () -> {
            try {
                return Class.forName("org.h2.Driver");
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        })
```

jOOλ의 접근 방식을 사용하세요:

```java
Stream
    .generate(Sneaky.supplier(
        () -> Class.forName("org.h2.Driver")))
    .limit(1)
    .forEach(System.out::println);
```

jOOλ 라이브러리: https://github.com/jOOQ/jOOL
