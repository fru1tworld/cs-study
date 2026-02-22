# SQL DISTINCT는 함수가 아니다

> 원문: https://blog.jooq.org/sql-distinct-is-not-a-function/

게시일: 2020년 3월 2일, 작성자: lukaseder

## 서론

SQL 사용자들 사이에 `DISTINCT`에 관한 널리 퍼진 오해가 존재한다. 많은 사람들이 이것이 괄호로 묶인 인자를 받는 호출 가능한 함수처럼 동작한다고 믿는다. 최근 Stack Overflow의 한 질문이 이러한 혼란을 잘 보여주었는데, 누군가가 다음과 같이 시도했다:

```sql
SELECT DISTINCT (emp.id), emp.fname, emp.name
FROM employee emp;
```

질문자는 `(emp.id)` 주위의 괄호가 특별한 `DISTINCT` 동작을 만들어낸다고 믿었다 - 즉, ID만이 중복 제거의 기준이 되고 성능이 향상될 것이라고 생각한 것이다.

## 이것은 잘못된 생각이다

이러한 가정들은 근본적으로 틀렸다. 괄호를 포함하는 것과 생략하는 것 사이에는 의미론적 차이도 성능 차이도 존재하지 않는다. 괄호는 단순히 연산자 우선순위에 영향을 주는 것처럼 표현식을 그룹화할 뿐이다. 다음의 유사한 예를 살펴보자:

```sql
SELECT DISTINCT (emp.id + 1) * 2, emp.fname, emp.name
FROM employee emp;
```

여기서 괄호는 곱셈보다 덧셈이 먼저 수행되도록 보장한다 - "DISTINCT가 해당 표현식에 함수로서 작용"하는 것이 아니다. `DISTINCT` 연산자는 항상 투영(projection) *이후에* 일관되게 적용되며, *전체* 투영에 영향을 미친다.

논리적으로 문법은 다음과 같이 읽혀야 한다:

```
FROM employee
SELECT id, fname, name
DISTINCT
```

## 어쨌든 그것이 의미하는 바는 무엇일까?

만약 `DISTINCT`가 단일 컬럼에만 선택적으로 적용된다면, 결과는 모호해질 것이다. 다음 데이터셋이 주어졌다고 가정해보자:

| id | fname | name |
|----|-------|------|
| 1  | A     | A    |
| 1  | B     | B    |

`DISTINCT id`만 선택하면 하나의 행이 나온다. 그러나 `FNAME`과 `NAME`도 함께 투영하면 문제가 생긴다: "어떤 행이 선택되어야 하는가?" SQL은 정의되지 않은 동작을 피하므로, 부분적인 `DISTINCT`는 불가능하다. `DISTINCT`는 합리적으로 완전한 투영에만 적용될 수 있다.

## 예외: PostgreSQL

PostgreSQL은 표준을 확장하여 `DISTINCT ON`을 제공하며, 이를 통해 선택적인 중복 제거가 가능하다:

```sql
WITH emp (id, fname, name) AS (
  VALUES (1, 'A', 'A'),
         (1, 'B', 'B')
)
SELECT DISTINCT ON (id) id, fname, name
FROM emp
ORDER BY id, fname, name
```

결과:

| id | fname | name |
|----|-------|------|
| 1  | A     | A    |

저자는 `DISTINCT ON`이 SQL 초보자에게 이미 어려운 개념을 더욱 복잡하게 만들기 때문에 좋아하지 않는다. 개선된 문법은 의미론을 상당히 명확하게 해줄 것이다:

```
FROM emp
SELECT id, fname, name
ORDER BY id, fname, name
DISTINCT ON (id)
```

## 더 나은 대안들

현대 SQL은 "카테고리별 상위 1개" 쿼리를 더 명확하게 표현할 수 있는 윈도우 함수를 제공한다. `ROW_NUMBER()`나 `QUALIFY` 절(Snowflake/Teradata에서 지원)이 그 예이다.

## 댓글 섹션의 논의

Joe Celko는 역사적으로 SQL에 `SELECT DISTINCT`와 `SELECT ALL` 연산자가 모두 포함되어 있었다고 언급했다.

Peter Tolgyesi는 `GROUP BY`가 `MIN()` 같은 집계 함수와 함께 유사한 로직을 표현할 수 있다고 제안했다.

Lukasz는 `QUALIFY ROW_NUMBER() OVER()`를 사용한 윈도우 함수가 더 나은 대안이라고 주장했다.

David Fetter는 `DISTINCT`를 불완전한 제약 조건 분석을 나타내는 "코드 스멜(code smell)"로 특징지었으며, `GROUP BY`와 윈도우 함수가 더 엄밀한 접근 방식이라고 설명했다.
