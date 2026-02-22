# PostgreSQL이 놀라운 이유

> 원문: https://blog.jooq.org/why-postgresql-is-so-awesome/

PostgreSQL은 다른 많은 데이터베이스와 구별되는 독특한 특성을 가지고 있습니다. 바로 술어(predicate)를 특별한 구문으로 요구하지 않고 boolean 타입으로 평가되는 일반적인 표현식으로 취급한다는 점입니다.

## 핵심 장점: 표현식으로서의 술어

PostgreSQL 문서에 따르면, "선택적 WHERE 절은 WHERE condition이라는 일반적인 형식을 가지며, 여기서 condition은 boolean 타입의 결과로 평가되는 모든 표현식입니다."

이로 인해 술어를 여러 SQL 절에 배치할 수 있습니다:

SELECT 절:
```sql
SELECT a, b, c = d, e IN (SELECT x FROM y)
FROM t
```

GROUP BY 절:
```sql
SELECT count(*)
FROM t
GROUP BY c = d, e IN (SELECT x FROM y)
```

ORDER BY 절:
```sql
SELECT *
FROM t
ORDER BY c = d, e IN (SELECT x FROM y)
```

## 표준 SQL 우회 방법

표준을 준수하는 SQL에서는 술어를 값으로 변환하기 위해 CASE 표현식이 필요하므로, 쿼리가 더 장황해지고 가독성이 떨어집니다.

## jOOQ 통합 영향

jOOQ 라이브러리는 PostgreSQL에서는 술어를 컬럼 표현식으로 직접 렌더링하고, 다른 데이터베이스에서는 CASE 표현식을 사용하여 기능을 에뮬레이션함으로써 이 동작을 표준화할 수 있습니다.

예제 코드:
```java
DSL.using(configuration)
   .select(T.A, T.B, field(T.C.eq(T.D)))
   .from(T);
```

## 지원 데이터베이스

Derby, H2, HSQLDB, MariaDB, MySQL, PostgreSQL, SQLite가 이 기능을 지원하지만, 성능 우려가 있을 수 있으므로 실행 계획을 모니터링해야 합니다.
