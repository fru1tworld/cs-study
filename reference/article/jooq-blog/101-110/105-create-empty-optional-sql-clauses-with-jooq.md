# jOOQ로 빈 선택적 SQL 절 생성하기

> 원문: https://blog.jooq.org/create-empty-optional-sql-clauses-with-jooq/

jOOQ에서 가장 자주 묻는 질문 중 하나는 동적 SQL 문을 올바르게 구성하는 방법입니다. 모든 jOOQ 쿼리는 내부적으로 동적입니다. jOOQ의 표현식 트리 모델 덕분에, 어떤 쿼리 부분이든 변수, 메서드 또는 조건식에서 가져와 쿼리에 조합할 수 있습니다. 그러나 일부 쿼리 절은 선택적이며, 생략하고 싶을 수 있습니다. 이 글에서는 코드 가독성을 깨뜨리지 않으면서 조건부로 쿼리 절을 추가하는 방법을 설명합니다.

## 하지 말아야 할 방법

일부 개발자들은 jOOQ에서 동적 SQL을 작성할 때 `XYZStep` 타입을 로컬 변수에 할당한 후 조건부로 절을 추가하려고 합니다:

```java
SelectConditionStep<?> c =
ctx.select(T.A, T.B)
   .from(T)
   .where(T.C.eq(1));

if (something)
    c = c.and(T.D.eq(2));

Result<?> result = c.fetch();
```

문제점: 이 접근 방식은 지저분하고 유지보수하기 어렵습니다. `XYZStep` 타입은 보조적인 역할을 합니다. 이들은 동적 SQL이 정적 SQL처럼 "보이도록" 만들기 위해 설계되었지만, 로컬 변수에 할당하거나 메서드에서 반환해서는 안 됩니다.

## 쿼리를 부분으로 구성하기

권장되는 함수형 접근 방식은 쿼리 구성 요소를 별도로 빌드하는 것입니다:

```java
DSLContext ctx = ...;

Condition where = T.C.eq(1);

if (something)
    where = where.and(T.D.eq(2));

Result<?> result =
ctx.select(T.A, T.B)
   .from(T)
   .where(where)
   .fetch();
```

이 접근 방식은 쿼리의 각 부분을 개별적으로 조합할 수 있게 해주며, 최종 쿼리 구성은 깔끔하게 유지됩니다.

## No-Op 표현식으로 가독성 유지하기

조건을 로컬에 유지하면서 가독성을 유지하려면 "no-op" 절을 사용합니다:

```java
Result<?> result =
ctx.select(T.A, T.B)
   .from(T)
   .where(T.C.eq(1))
   .and(something
      ? T.D.eq(2)
      : DSL.noCondition())
   .fetch();
```

핵심 메서드들:
- `DSL.noCondition()`: SQL 내용을 생성하지 않습니다 (더 깔끔한 출력을 위해 권장됨)
- `DSL.trueCondition()`: `TRUE` 또는 `1 = 1`로 렌더링됩니다
- `DSL.falseCondition()`: `FALSE` 또는 `1 = 0`으로 렌더링됩니다

중요 참고사항: `noCondition()`은 항등원(identity)으로 작동하지 않습니다. 만약 `noCondition()`이 WHERE 절에 남은 유일한 조건이라면, WHERE 절 자체가 생성되지 않습니다.

## 다양한 쿼리 타입을 위한 No-Op 표현식

### org.jooq.Condition (조건/술어)

선택적 WHERE/HAVING 절에는 `DSL.noCondition()`을 사용합니다.

### org.jooq.Field (컬럼 표현식)

선택적 프로젝션(SELECT 절의 컬럼)에는 인라인된 빈 값을 사용합니다:

```java
Result<Record2<String, String>> result =
ctx.select(T.A, something ? T.B : DSL.inline("").as(T.B))
   .from(T)
   .where(T.C.eq(1))
   .fetch();
```

### org.jooq.Table (테이블 표현식)

조건부 JOIN에 사용합니다:

```java
Result<?> result =
ctx.select(T.A, T.B, something ? U.X : inline(""))
   .from(
      something
      ? T.join(U).on(T.Y.eq(U.Y))
      : T)
   .where(T.C.eq(1))
   .fetch();
```

## 조건부 서브쿼리를 사용한 UNION 예제

```java
Result<Record2<String, String>> result =
ctx.select(T.A, T.B)
   .from(T)
   .union(
      something
        ? select(U.A, U.B).from(U)
        : select(inline(""), inline("")).where(falseCondition())
   )
   .fetch();
```

이 예제에서 `something`이 false일 때, UNION의 두 번째 부분은 빈 문자열 상수를 선택하고 `FALSE` 조건을 사용하여 결과가 없도록 합니다.

## 핵심 원칙

1. XYZStep 타입은 보조적입니다: 이들은 동적 SQL이 정적 SQL처럼 "보이도록" 만들기 위해 설계되었지만, 로컬 변수에 할당하거나 메서드에서 반환해서는 안 됩니다.

2. 모든 jOOQ 쿼리는 동적입니다: 표현식 트리 설계 덕분에 모든 쿼리 부분을 변수, 메서드 또는 조건식에서 조합할 수 있으며, 모든 것을 로컬에 유지하면서 가독성을 유지할 수 있습니다.

이 글에서는 함수형, 표현식 기반 접근 방식이 유지보수 가능한 동적 SQL 쿼리를 빌드하는 데 명령형 제어 흐름보다 우수하다는 것을 강조합니다.
