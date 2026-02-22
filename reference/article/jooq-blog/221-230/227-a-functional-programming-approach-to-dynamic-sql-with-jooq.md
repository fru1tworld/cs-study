> 원문: https://blog.jooq.org/a-functional-programming-approach-to-dynamic-sql-with-jooq/

# jOOQ를 활용한 동적 SQL의 함수형 프로그래밍 접근법

## 동적 쿼리 구성

모든 jOOQ 문(statement)은 기술적으로 동적입니다. 런타임에 표현식 트리(expression tree)를 재구성하기 때문입니다. DSL은 미리 컴파일된 문을 실행하는 것이 아니라, 단계별로 `org.jooq.Select` 객체를 생성합니다.

## 기본 동적 조건절(Predicate)

조건부 WHERE 절을 사용하여 쿼리를 구성하는 방법을 살펴보겠습니다:

```java
Condition condition = DSL.trueCondition();
if (hasFirstNameSearch())
    condition = condition.and(FIRST_NAME.like(firstNameSearch()));
if (hasLastNameSearch())
    condition = condition.and(LAST_NAME.like(lastNameSearch()));
```

## 함수형 접근법

명령형(imperative) 구성 방식 대신, 함수를 매개변수로 전달하여 재사용 가능한 쿼리 템플릿을 만드는 것이 권장됩니다:

```java
public static ResultQuery<Record2<String, String>> actors(
    Function<Actor, Condition> where
) {
    return ctx.select(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
              .from(ACTOR)
              .where(where.apply(ACTOR));
}
```

## 가변 인자(Varargs) 함수

최대한의 유연성을 위해 여러 조건 함수를 받아서 결합할 수 있습니다:

```java
@SafeVarargs
public static ResultQuery<Record2<String, String>> actors(
    Function<Actor, Condition>... where
) {
    return dsl().select(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
                .from(ACTOR)
                .where(Arrays.stream(where)
                             .map(f -> f.apply(ACTOR))
                             .collect(Collectors.toList()));
}
```

## 장점

이러한 함수형 전략 패턴(strategy pattern) 접근법은 전통적인 객체지향 프로그래밍(OOP) 구현보다 더 깔끔한 코드 재사용을 가능하게 합니다. 또한 "수십 개의 공통 테이블 표현식(CTE, Common Table Expression)을 동적으로 조립"하는 것과 같은 복잡한 시나리오로도 확장할 수 있습니다.
