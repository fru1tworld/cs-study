# SQL Server 트릭: 누락된 ORDER BY 절 우회하기

> 원문: https://blog.jooq.org/sql-server-trick-circumvent-missing-order-by-clause/

게시일: 2014년 5월 13일
저자: lukaseder

## 개요

SQL Server는 엄격한 SQL 표준 준수를 강제하여, 다른 데이터베이스에서는 허용되는 특정 표현식을 금지합니다. 이 글에서는 윈도우 함수와 OFFSET 연산에서 ORDER BY 절이 누락된 경우의 우회 방법을 설명합니다.

## 주요 문제점과 해결책

### 문제 1: 비결정적 윈도우 함수

SQL Server는 명시적 정렬이 없는 윈도우 함수를 거부합니다:

```sql
-- SQL Server에서 실패함
SELECT ROW_NUMBER() OVER ()
```

이 문제는 비결정성에서 비롯됩니다. 엄격한 정렬 없이는 실행할 때마다 결과가 달라질 수 있기 때문입니다.

### 문제 2: 상수 ORDER BY 절이 작동하지 않음

표준 상수 표현식은 윈도우 함수에서 실패합니다:

```sql
-- 이것은 작동하지 않음:
SELECT ROW_NUMBER() OVER (ORDER BY 'a')

-- 하지만 이것은 작동함:
SELECT a
FROM (VALUES (1), (2), (3), (4)) t(a)
ORDER BY 'a'
OFFSET 3 ROWS
```

### 문제 3: 제한된 컬럼 가용성

모든 컨텍스트에서 정렬에 사용할 수 있는 컬럼이 있는 것은 아닙니다. 윈도우 함수에는 적합한 컬럼 참조가 없을 수 있어, 유효한 ORDER BY 표현식에 제약이 생깁니다.

## 해결책: 유사 상수 표현식

`@@version`이나 `@@language` 같은 시스템 변수를 사용하면 신뢰할 수 있는 더미 ORDER BY 절을 제공할 수 있습니다:

```sql
-- 이것은 항상 작동함:
SELECT ROW_NUMBER() OVER (ORDER BY @@version)

-- 이것도 유효함:
SELECT a
FROM (VALUES (1), (2), (3), (4)) t(a)
ORDER BY @@version
OFFSET 3 ROWS
```

이러한 표현식은 실행 간에 상수로 유지되면서 SQL Server의 구문 요구사항을 충족합니다.

## 구현 참고사항

jOOQ 3.4부터는 라이브러리가 자동으로 합성 ORDER BY 절을 생성하여, 이러한 엣지 케이스에서도 벤더에 구애받지 않는 SQL 생성을 가능하게 합니다.
