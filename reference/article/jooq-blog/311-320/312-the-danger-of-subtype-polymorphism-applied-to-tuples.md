# 튜플에 적용된 하위 타입 다형성의 위험

> 원문: https://blog.jooq.org/the-danger-of-subtype-polymorphism-applied-to-tuples/

Java 8은 람다와 스트림을 도입했지만 네이티브 튜플 지원은 부족합니다. jOOλ 라이브러리(Java 8의 누락된 부분)는 튜플을 간단한 값 타입 컨테이너로 구현합니다. 기본적인 튜플 클래스는 다음 패턴을 따릅니다:

```java
public class Tuple2<T1, T2> {
    public final T1 v1;
    public final T2 v2;

    public Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
}

public class Tuple3<T1, T2, T3> {
    public final T1 v1;
    public final T2 v2;
    public final T3 v3;

    public Tuple3(T1 v1, T2 v2, T3 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }
}
```

소스 코드 생성은 튜플 클래스를 작성하는 지루한 작업을 처리합니다.

## 여러 언어에서의 튜플

jOOλ는 차수 0-16의 튜플을 제공합니다. C#과 .NET은 차수 1-8을 지원합니다. Javatuples 라이브러리는 기억하기 쉬운 이름으로 차수 1-10을 제공합니다:

Unit<A>, Pair<A,B>, Triplet<A,B,C>, Quartet<A,B,C,D>, Quintet, Sextet, Septet, Octet, Ennead, Decade.

jOOQ는 Scala의 관례를 따라 차수 22까지의 Record 타입을 포함합니다.

## 핵심 문제: 타입 계층 구조의 위험성

`Tuple3`와 `Tuple2`가 구조적 유사성을 공유하지만, `Tuple3`가 `Tuple2`를 확장하게 만드는 것은 "당신이 할 수 있는 최악의 일"입니다. 둘 다 튜플이므로 공통 `Tuple` 인터페이스를 통해 공통 기능을 공유하는 것은 말이 됩니다. 하지만 차수는 절대로 상속 체인의 일부가 되어서는 안 됩니다.

### 순열 문제

`Tuple3`가 `Tuple2`를 확장하면, 더 높은 차수의 튜플이 더 낮은 차수의 튜플과 할당 호환이 됩니다:

```java
Tuple2<String, Integer> t2 = tuple("A", 1, 2, 3, "B");
```

이 할당은 작동해서는 안 되지만, 그렇게 될 것입니다. 문제는: 튜플 차수를 줄일 때 "가능한 속성의 순열이 엄청나게 많다"는 것입니다. 가장 오른쪽 속성을 삭제하는 것과 같은 기본 선택이 모든 사용 사례에 적합하지는 않습니다.

### 타입 시스템의 의미

jOOQ API는 상속이 타입 안전성을 깨뜨리는 이유를 보여줍니다. 다음은 올바르게 컴파일됩니다:

```java
TABLE1.COL1.in(select(TABLE2.COL1).from(TABLE2))
```

다음은 컴파일되어서는 안 됩니다:

```java
TABLE1.COL1.in(select(TABLE2.COL1, TABLE2.COL2).from(TABLE2))
```

메서드 시그니처가 타입 안전성을 보장합니다:

```java
public interface Field<T> {
    Condition in(Select<? extends Record1<T>> select);
}
```

"다행히 `Record2`는 `Record1`을 확장하지 않습니다." 만약 확장했다면, `Select<Record2<Type1, Type2>>`가 하위 타입 다형성을 통해 `Select<Record1<Type1>>`을 만족시킬 수 있기 때문에 잘못된 SQL이 컴파일될 것입니다.

## 해결책: 상속보다 합성

튜플 타입은 근본적으로 호환되지 않습니다. 강력한 타입 시스템은 유사한 타입, 관련된 타입, 호환 가능한 타입을 구별해야 합니다. 상속은 수십 년 전의 "결함이 있고 과도하게 설계된 객체 지향 개념"을 나타냅니다. 대신 합성을 선호하세요 - 할당 대신 변환하세요.

jOOλ는 매핑을 통한 변환을 가능하게 합니다:

```java
// (1, 3)을 생성
Tuple2<String, Integer> t2_4 =
    tuple("A", 1, 2, 3, "B")
    .map((v1, v2, v3, v4, v5) -> tuple(v2, v4));

// ("A", 2)를 생성
Tuple2<String, Integer> t1_3 =
    tuple("A", 1, 2, 3, "B")
    .map((v1, v2, v3, v4, v5) -> tuple(v1, v3));
```

이 접근 방식은 불변 값에서 작동하여 타입 안전성을 위반하지 않고 추출과 재결합을 허용합니다.
