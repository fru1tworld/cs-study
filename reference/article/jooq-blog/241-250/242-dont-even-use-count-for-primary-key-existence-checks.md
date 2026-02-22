> 원문: https://blog.jooq.org/dont-even-use-count-for-primary-key-existence-checks/

# 기본 키(Primary Key) 존재 여부 확인에 COUNT(*)도 사용하지 마세요

이 글에서는 기본 키 존재 여부를 확인할 때 `COUNT(*)`를 `EXISTS()`로 대체해야 하는 이유를 설명합니다. 기술적으로 둘 다 0 또는 1을 반환하지만, 차이점이 있습니다.

## 핵심 논점

`EXISTS()`는 단순한 개수 세기가 아닌 존재 여부 확인이라는 의도를 명확하게 전달하며, 데이터베이스에 따라 성능 차이가 발생합니다.

Oracle 11g XE: `EXISTS()` 방식이 `COUNT(*)`에 비해 근소하지만 측정 가능한 성능 향상을 보여주었습니다.

PostgreSQL 9.5: 극적인 차이를 보였습니다. "Statement 1: 00:00:13.32556" 대 "Statement 2: 00:00:08.350491"로, PostgreSQL의 `COUNT(*)` 연산에 대한 알려진 성능 문제를 여실히 보여줍니다.

SQL Server 2014: 두 접근 방식 간에 측정 가능한 성능 차이가 없었습니다.

## 코드 예제

두 가지 Oracle 접근 방식을 비교합니다:

```sql
-- 접근 방식 1: COUNT(*)
SELECT count(*) FROM film WHERE film_id = 1;

-- 접근 방식 2: EXISTS()
SELECT CASE WHEN EXISTS (
  SELECT * FROM film WHERE film_id = 1
) THEN 1 ELSE 0 END FROM dual;
```

jOOQ 사용자의 경우, 라이브러리가 `fetchExists()` 메서드를 통해 이러한 최적화를 자동으로 처리합니다.

## 결론

모든 쿼리에서 일관되게 `EXISTS()`를 사용하면 의도가 더 명확해지고, 특히 PostgreSQL에서 성능상의 이점을 얻을 수 있습니다. 따라서 데이터베이스 플랫폼과 관계없이 `EXISTS()`를 사용하는 것이 모범 사례(Best Practice)입니다.
