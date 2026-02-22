# jOOQ와 JDK 10의 로컬 변수 타입 추론 사용하기

> 원문: https://blog.jooq.org/using-jdk-10s-local-variable-type-inference-with-jooq/

JDK 9가 성공적으로 출시된 후, 개발자들은 JDK 10의 얼리 액세스 버전을 탐색할 수 있게 되었습니다. 대부분의 Java 개발자들에게 가장 흥미로운 기능은 아마도 [JEP 286: 로컬 변수 타입 추론](http://openjdk.java.net/jeps/286)일 것입니다. 이 기능을 통해 `var` 키워드를 사용할 수 있게 되었습니다.

## 비표기 타입(Non-denotable Types)과 익명 클래스

`var` 키워드는 개발자들이 비표기 타입(non-denotable types)을 다룰 수 있게 해줍니다. 비표기 타입이란 "이름을 부여할 수 없는" 타입을 의미합니다. 이전에는 이러한 타입을 참조하기 어려웠습니다.

예를 들어, 슈퍼타입의 메서드를 오버라이드하지 않는 메서드가 있는 익명 클래스의 경우를 살펴보겠습니다:

```java
(new Object() {
    void m() {
        System.out.println("m");
    }
    void n() {
        System.out.println("n");
    }
}).m();
```

Java 10 이전에는 익명 클래스 인스턴스에서 하나의 메서드만 호출할 수 있었습니다. 인스턴스 참조가 즉시 손실되기 때문에, 위와 같이 `m()`만 호출하거나 `n()`만 호출할 수 있었습니다.

Java 10의 `var` 키워드를 사용하면, 전체 표현식을 로컬 변수에 할당하면서 익명 타입을 보존할 수 있어 모든 메서드에 접근할 수 있게 됩니다:

```java
var o = new Object() {
    void m() {
        System.out.println("m");
    }
    void n() {
        System.out.println("n");
    }
};

o.m();
o.n();
```

여기서 `o`의 타입은 비표기 타입입니다 - 이름을 부여할 수 없습니다.

## jOOQ에서의 적용

jOOQ의 API는 선택된 컬럼에 따라 서로 다른 `Record[N]<T1, T2, ..., T[N]>` 타입을 생성합니다. 타입 안전성은 유지되지만, 많은 컬럼을 다룰 때 명시적 타입 선언이 매우 장황해집니다.

### 루프 변수

`var` 키워드는 이를 단순화합니다.

이전:

```java
for (Record3<String, String, String> r : using(con)
        .select(c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME)
        .from(c))
    System.out.println(
        r.value1() + "." + r.value2() + "." + r.value3());
```

이후:

```java
for (var r : using(con)
        .select(c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME)
        .from(c))
    System.out.println(
        r.value1() + "." + r.value2() + "." + r.value3());
```

타입 안전성은 그대로 유지됩니다 - 3개의 컬럼을 가진 레코드에서 `r.value4()`와 같은 지원되지 않는 메서드를 호출하려고 하면 컴파일 오류가 발생합니다.

### 동적 SQL 생성

이 기능은 동적 SQL 생성에도 유용합니다. 타입 안전성을 희생하지 않으면서 서브쿼리와 조건문을 위한 더 깔끔한 변수 선언이 가능합니다:

```java
var subq = select(t.TABLE_SCHEMA, t.TABLE_NAME)
          .from(t)
          .where(t.TABLE_NAME.eq("TABLES"));

var pred = row(c.TABLE_SCHEMA, c.TABLE_NAME).in(subq);

var q = using(con).selectFrom(c).where(pred);
```

## 결론

이것이 "게임 체인저"는 아니지만, Kotlin이나 Scala와 같은 언어에서 전환하는 개발자들에게는 안도감을 줍니다. Java 10은 jOOQ 사용자들에게 "정말로 유용한" 릴리스입니다.

## 논의

한 댓글 작성자가 명시적 타입 없이는 가독성이 떨어지고 인지 부하가 증가한다는 우려를 제기했습니다. IDE가 이를 보완해야 할 것이라고 제안했습니다. 저자는 이러한 비판이 이미 Java에서 널리 사용되는 플루언트 API와 람다에도 동일하게 적용된다고 반박하며, 이 현대화를 수용할 것을 권장했습니다.
