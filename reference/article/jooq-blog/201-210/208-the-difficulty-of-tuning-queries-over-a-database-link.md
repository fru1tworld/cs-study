> 원문: https://blog.jooq.org/the-difficulty-of-tuning-queries-over-a-database-link-or-how-i-learned-to-stop-worrying-and-love-the-duallink-table/

# 데이터베이스 링크를 통한 쿼리 튜닝의 어려움

## 요약

이 글에서는 Oracle 데이터베이스 링크(Database Link)를 통해 실행되는 SQL 쿼리를 최적화할 때의 어려움, 특히 조인(Join)과 함께 `EXISTS` 술어(Predicate)를 사용할 때 발생하는 문제를 살펴봅니다.

## 핵심 문제

데이터베이스 링크를 통해 원격 테이블을 쿼리할 때, Oracle의 옵티마이저(Optimizer)는 `EXISTS` 술어를 원격 데이터베이스로 전파하지 못합니다. 이로 인해 비효율적인 실행 계획이 생성됩니다. 구체적으로, 원격 데이터베이스가 더 효율적인 `SEMI JOIN` 대신 `HASH JOIN`을 실행하여 불필요한 행들을 실체화(Materialize)하게 됩니다.

## 해결책: DUAL@LINK

해결 방법은 미묘하지만 중요한 변경을 포함합니다. `FROM` 절에서 로컬 `DUAL` 테이블 대신 `DUAL@LOOPBACK`(원격 DUAL 테이블)을 사용하는 것입니다.

변경 전 (비효율적):
```sql
SELECT CASE WHEN EXISTS (
  SELECT * FROM t@loopback
  JOIN u@loopback USING (a)
  WHERE t.b BETWEEN 0 AND 1000
) THEN 1 ELSE 0 END FROM dual;
```

변경 후 (최적화됨):
```sql
SELECT CASE WHEN EXISTS (
  SELECT * FROM t@loopback
  JOIN u@loopback USING (a)
  WHERE t.b BETWEEN 0 AND 1000
) THEN 1 ELSE 0 END FROM dual@loopback;
```

## 성능 영향

대용량 결과 집합(0-1000개 행)의 경우, `DUAL@LOOPBACK` 접근 방식은 로컬 DUAL 테이블을 사용하는 것보다 약 5배 빠른 성능을 보여주었습니다. 이는 로컬에서 직접 쿼리를 실행하는 것보다는 느리지만 상당한 개선입니다.

## 모범 사례

저자는 다음과 같은 대안을 선호도 순으로 권장합니다:

1. 복잡한 로직을 캡슐화하기 위해 원격 저장 프로시저(Stored Procedure) 생성
2. 실용적인 해결책으로 `DUAL@LINK` 사용
3. 가능하다면 데이터베이스 링크 사용을 아예 피하기
