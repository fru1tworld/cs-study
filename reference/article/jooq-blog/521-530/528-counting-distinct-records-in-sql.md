# SQL에서 고유 레코드 카운팅하기

> 원문: https://blog.jooq.org/counting-distinct-records-in-sql/

MySQL에서는 `COUNT()` 집계 함수에 여러 컬럼을 사용하여 고유 레코드를 카운트할 수 있습니다:

```sql
SELECT COUNT(DISTINCT FIRST_NAME, LAST_NAME)
FROM CUSTOMERS
```

MySQL 문서에 따르면, 이는 "NULL이 아닌 서로 다른 expr 값을 가진 행의 개수를 반환"합니다.

## SQL 표준 방식

이 기능은 MySQL 전용입니다(HSQLDB도 지원하긴 합니다). 하지만 SQL-99 표준에서는 실제로 행 값 표현식(row value expression)을 사용하는 다른 문법을 명시하고 있습니다:

```sql
SELECT COUNT(DISTINCT (FIRST_NAME, LAST_NAME))
FROM CUSTOMERS
```

핵심적인 차이점은 `DISTINCT`가 함수 호출이 아니라 행 값 표현식에 적용되는 키워드라는 것입니다. PostgreSQL과 HSQLDB는 이 표준 문법을 지원합니다.

## 다른 데이터베이스를 위한 대안

위 두 가지 방식을 모두 지원하지 않는 데이터베이스의 경우, 서브쿼리를 사용한 우회 방법이 있습니다:

```sql
SELECT COUNT(*)
FROM (
  SELECT DISTINCT FIRST_NAME, LAST_NAME
  FROM CUSTOMERS
) t
```

## jOOQ 솔루션

jOOQ는 여러 데이터베이스 간의 SQL 방언을 표준화하여, `DSL.countDistinct()` 메서드를 통해 이러한 차이를 자동으로 처리합니다. 향후 버전에서는 `GROUP BY` 절과 여러 집계 함수를 포함하는 더 복잡한 변환도 처리할 예정입니다.

---

저자: Lukas Eder
게시일: 2013년 12월 17일
