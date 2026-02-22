# Java의 검사 예외는 단지 이상한 유니온 타입이다

> 원문: https://blog.jooq.org/javas-checked-exceptions-are-just-weird-union-types/

저자: lukaseder
작성일: 2021년 11월 1일

---

## 서론

최근 Reddit에서 "[Sealed 인터페이스로 검사 예외 밀반입하기(Smuggling Checked Exceptions with Sealed Interfaces)](https://www.reddit.com/r/java/comments/qipxm4/smuggling_checked_exceptions_with_sealed/)"에 대한 흥미로운 토론이 있었습니다. 이 글은 그 토론에서 영감을 받아 작성되었습니다. 핵심 통찰은 이것입니다: "Java는 유니온 타입이 유행하기 전부터 이미 가지고 있었다" (물론 몇 가지 단서가 붙지만요).

---

## 유니온 타입이란 무엇인가?

유니온 타입은 여러 타입 중 하나가 될 수 있는 값을 나타냅니다. JVM 언어인 Ceylon과 현대의 TypeScript가 이 개념을 잘 보여줍니다.

TypeScript 예제:

```typescript
function printId(id: number | string) {
  console.log("Your ID is: " + id);
}
printId(101);      // OK
printId("202");    // OK
printId({ myID: 22342 }); // Error
```

유니온 타입을 지원하는 언어로는 C++, PHP, Python 등이 있습니다.

---

## 구조적 타이핑 vs. 명목적 타이핑

구조적 타이핑 (유니온 타입):
- 장점: 어디서든 즉석에서(ad-hoc) 유니온을 생성할 수 있으며, 기존 타입 계층을 수정할 필요가 없습니다
- 단점: 공식적인 문서화와 재사용성이 떨어집니다

명목적 타이핑 (Java의 접근 방식):

```java
interface FieldOrRow {}
interface Field<T> extends FieldOrRow {}
interface Row extends FieldOrRow {}
```

Java 17 sealed 타입:

```java
sealed interface FieldOrRow permits Field<T>, Row {}
sealed interface Field<T> extends FieldOrRow permits ... {}
```

---

## 유니온 타입으로서의 검사 예외

여기서 획기적인 관찰이 나옵니다: 메서드 반환 타입과 검사 예외를 결합하면 유니온 타입처럼 작동합니다.

전통적인 선언:

```java
public String getTitle(int id) throws SQLException;
```

유니온 타입으로 개념적으로 동등한 표현:

```java
// 가상의 Java 문법:
public String|SQLException getTitle(int id);
```

호출자는 두 가지 분기를 모두 처리해야 합니다:

```java
try {
    String title = getTitle(1);
    doSomethingWith(title);
}
catch (SQLException e) {
    handle(e);
}
```

### 다중 예외 예제

현재 Java 리플렉션 API:

```java
public Object invoke(Object obj, Object... args)
throws
    IllegalAccessException,
    IllegalArgumentException,
    InvocationTargetException
```

유니온 타입 문법으로 표현하면:

```java
// 가상의 문법:
public Object
    | IllegalAccessException
    | IllegalArgumentException
    | InvocationTargetException invoke(Object obj, Object... args)
```

현재의 catch 블록 (이미 유니온과 유사함):

```java
try {
    Object returnValue = method.invoke(obj);
}
catch (IllegalAccessException | IllegalArgumentException e) {
    handle1(e);
}
catch (InvocationTargetException e) {
    handle2(e);
}
```

패턴 매칭 대안 (Java 17+):

```java
Object result = method.invoke(obj);

switch (result) {
    case IllegalAccessException,
         IllegalArgumentException e -> handle1(e);
    case InvocationTargetException e -> handle2(e);
    case Object returnValue -> doSomethingWith(returnValue);
}
```

catch 블록은 철저성 검사(exhaustiveness checking)를 제공합니다—모든 예외 타입이 반드시 처리되어야 합니다.

---

## 검사 예외로 유니온 타입 에뮬레이션하기

저자는 래퍼 예외를 사용한 창의적인 구현을 보여줍니다:

기본 예외 클래스:

```java
class E extends Exception {
    @Override
    public Throwable fillInStackTrace() {
        return this; // 성능 최적화
    }
}

class EString extends E {
    String s;
    EString(String s) { this.s = s; }
}

class Eint extends E {
    int i;
    Eint(int i) { this.i = i; }
}
```

2개 항목용 Switch 유틸리티:

```java
class Switch2<E1 extends E, E2 extends E> {
    E1 e1;
    E2 e2;

    private Switch2(E1 e1, E2 e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    static <E1 extends E, E2 extends E> Switch2<E1, E2> of1(E1 e1) {
        return new Switch2<>(e1, null);
    }

    static <E1 extends E, E2 extends E> Switch2<E1, E2> of2(E2 e2) {
        return new Switch2<>(null, e2);
    }

    void check() throws E1, E2 {
        if (e1 != null)
            throw e1;
        else
            throw e2;
    }
}
```

사용법과 철저성 검사:

```java
Switch2<EString, Eint> s = Switch2.of1(new EString("hello"));

// 컴파일 실패 - Eint가 catch되지 않음
try {
    s.check();
}
catch (EString e) {}

// 컴파일 성공 - 모든 타입이 처리됨
try {
    s.check();
}
catch (EString e) {}
catch (Eint e) {}
```

---

## 결론

저자는 Java의 미래에 유니온 타입과 인터섹션 타입이 "일급 시민(first class)"이 되어 jOOQ와 같은 API에 도움이 되기를 희망합니다. 현재로서는 검사 예외가 Java에서 유니온 타입에 가장 가까운 근사치를 나타내며, catch 블록이 철저성 검사 메커니즘을 제공합니다.

---

## 태그

ADT, 대수적 데이터 타입, Ceylon, 철저성 검사, 인터섹션 타입, Java, 명목적 타이핑, 패턴 매칭, sealed 타입, 구조적 타이핑, 합 타입, 태그된 유니온 타입, 유니온 타입, 태그되지 않은 유니온 타입
