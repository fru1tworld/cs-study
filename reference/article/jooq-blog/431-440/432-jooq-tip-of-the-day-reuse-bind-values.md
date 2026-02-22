# jOOQ 오늘의 팁: 바인드 값 재사용하기

> 원문: https://blog.jooq.org/jooq-tip-of-the-day-reuse-bind-values/

게시일: 2014년 7월 28일 | 작성자: lukaseder

## 개요

이 블로그 포스트는 jOOQ의 추상 구문 트리(AST) 구현이 개발자들에게 SQL 쿼리의 여러 부분에서 바인드 값을 재사용할 수 있게 해주어, 코드의 표현력을 높이고 반복을 줄여주는 방법을 보여줍니다.

## 핵심 개념

jOOQ는 SQL 문을 텍스트로 직렬화하기 전에 AST로 모델링하기 때문에, 쿼리 구성 요소를 자유롭게 조작할 수 있습니다. 실용적인 예로, 동일한 값이 쿼리에서 여러 번 나타나는 시나리오가 있습니다.

## 문제점

SQL에서는 종종 값을 반복적으로 선언해야 합니다:

```sql
SELECT 1
FROM   TABLE
WHERE  TABLE.COL < 1

SELECT 2
FROM   TABLE
WHERE  TABLE.COL < 2
```

## jOOQ 솔루션

값을 하드코딩하는 대신, 재사용 가능한 파라미터 참조를 생성합니다:

```java
Param<Integer> value = val(1);

Select<?> query =
DSL.using(configuration)
   .select(value.as("a"))
   .from(TABLE)
   .where(TABLE.COL.lt(value));

assertEquals(1, (int) query.fetchOne("a"));

// 이후 실행을 위해 값을 변경
value.setValue(2);
assertEquals(2, (int) query.fetchOne("a"));
```

## 이점

SQL 문의 AST를 직접 조작함으로써, 개발자들은 전체 쿼리를 재생성하는 대신 "방향 그래프"를 변환하여, 더 깔끔하고 유지보수하기 쉬운 코드 패턴을 구현할 수 있습니다.
