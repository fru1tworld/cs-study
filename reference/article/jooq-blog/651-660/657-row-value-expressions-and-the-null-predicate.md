# 행 값 표현식과 NULL 술어

> 원문: https://blog.jooq.org/row-value-expressions-and-the-null-predicate/

행 값 표현식(Row Value Expression)은 SQL-1992 이후로 SQL 표준의 일부였지만, 모든 데이터베이스에서 올바르게 구현된 것은 아닙니다. 행 값 표현식은 여러 컬럼을 하나의 단위로 다룰 수 있게 해주는 강력한 SQL 기능입니다.

이 글에서는 두 가지 핵심 표현식을 살펴봅니다:

- `(A, B) IS NULL`
- `(A, B) IS NOT NULL`

## SQL-1992 표준 정의

SQL-1992 표준의 섹션 8.6에 따르면, 다음과 같은 규칙이 정의되어 있습니다:

1. 행의 모든 값이 null이면 "R IS NULL"은 true로 평가되고, 그렇지 않으면 false입니다.

2. 어떤 값도 null이 아니면 "R IS NOT NULL"은 true이고, 그렇지 않으면 false입니다.

3. 중요한 점: "R IS NOT NULL"은 행의 차수(degree)가 1인 경우를 제외하고는 "NOT R IS NULL"과 동일한 결과를 갖지 않습니다.

## 핵심 등가 관계 설명

다음을 살펴보겠습니다:

```sql
(A, B) IS NOT NULL
-- 이것은 다음과 같습니다...
A IS NOT NULL AND B IS NOT NULL
-- 이것은 또한 다음과 같습니다...
NOT(A IS NULL OR B IS NULL)
```

반면에:

```sql
NOT((A, B) IS NULL)
-- 이것은 다음과 같습니다...
NOT(A IS NULL AND B IS NULL)
```

이 두 표현식이 동일하지 않다는 것이 핵심입니다. 첫 번째는 "모든 값이 null이 아니다"를 의미하고, 두 번째는 "모든 값이 null인 것은 아니다"(즉, 적어도 하나는 null이 아니다)를 의미합니다.

## 진리표

| 표현식 | IS NULL | IS NOT NULL | NOT IS NULL | NOT IS NOT NULL |
|---|---|---|---|---|
| 차수 1: null | true | false | false | true |
| 차수 1: not null | false | true | true | false |
| 차수 >1: 모두 null | true | false | false | true |
| 차수 >1: 일부 null | false | false | true | true |
| 차수 >1: null 없음 | false | true | true | false |

진리표에서 주목해야 할 중요한 행은 "차수 >1: 일부 null" 행입니다. 이 경우 `IS NULL`과 `IS NOT NULL` 모두 false를 반환합니다. 이것은 많은 개발자들이 놓치는 미묘하지만 중요한 구분점입니다.

## 결론

jOOQ 3.0에서는 행 값 표현식과 관련 술어에 대한 타입 안전한 지원이 도입되어, 다양한 SQL 방언에서 이러한 표현식을 올바르게 사용할 수 있게 되었습니다.
