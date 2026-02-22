# SQL UPDATE .. RETURNING으로 DML을 더 효율적으로 실행하는 방법

> 원문: https://blog.jooq.org/how-to-use-sql-update-returning-to-run-dml-more-efficiently/

## 소개

고객 사이트에서 "slow-by-slow(느림-느림)" PL/SQL 루프를 효율적인 집합 기반 UPDATE 문으로 리팩토링하여 코드 라인 수를 줄이고 성능을 개선했습니다. 이 기법은 Oracle UPDATE 문에 적용되며, INSERT, DELETE, MERGE 문에서도 다양한 데이터베이스에 걸쳐 구현할 수 있습니다.

## 스키마 설정

```sql
-- 테이블 정의
CREATE TABLE t (
  id NUMBER(10) GENERATED ALWAYS AS IDENTITY NOT NULL PRIMARY KEY,
  category NUMBER(10) NOT NULL,
  counter NUMBER(10),
  text VARCHAR2(10) NOT NULL
);

-- 샘플 데이터
INSERT INTO t (category, text)
SELECT dbms_random.value(1, 10), dbms_random.string('a', 10)
FROM dual
CONNECT BY level <= 100;

-- 데이터 출력
SELECT *
FROM t
ORDER BY counter DESC NULLS LAST, category, id;
```

샘플 데이터 출력:
```
ID   CATEGORY   COUNTER   TEXT
16   1                    UIXSzJxDez
25   1                    hkvvrTRbTC
29   1                    IBOJYveDgf
44   1                    VhcwOugrWB
46   1                    gBJFJrPQYy
47   1                    bVzfHznOUj
10   2                    KpHHgsRXwR
11   2                    vpkhTrkaaU
14   2                    fDlNtRdvBE
```

## Row-by-Row PL/SQL 접근 방식

```sql
SET SERVEROUTPUT ON
DECLARE
  v_text VARCHAR2(2000);
  v_updated PLS_INTEGER := 0;
BEGIN
  FOR r IN (
    SELECT * FROM t WHERE category = 1
  ) LOOP
    v_updated := v_updated + 1;

    IF v_text IS NULL THEN
      v_text := r.text;
    ELSE
      v_text := v_text || ', ' || r.text;
    END IF;

    IF r.counter IS NULL THEN
      UPDATE t SET counter = 1 WHERE id = r.id;
    ELSE
      UPDATE t SET counter = counter + 1 WHERE id = r.id;
    END IF;
  END LOOP;

  COMMIT;
  dbms_output.put_line('Rows updated: ' || v_updated);
  dbms_output.put_line('Returned:     ' || v_text);
END;
/
```

결과:
```
Rows updated: 6
Returned:     UIXSzJxDez, hkvvrTRbTC, IBOJYveDgf, VhcwOugrWB, gBJFJrPQYy, bVzfHznOUj
```

업데이트 후 데이터:
```
ID   CATEGORY   COUNTER   TEXT
16   1          1         UIXSzJxDez
25   1          1         hkvvrTRbTC
29   1          1         IBOJYveDgf
44   1          1         VhcwOugrWB
46   1          1         gBJFJrPQYy
47   1          1         bVzfHznOUj
10   2                    KpHHgsRXwR
11   2                    vpkhTrkaaU
14   2                    fDlNtRdvBE
```

## 집합 기반 SQL 접근 방식 (Oracle)

```sql
SET SERVEROUTPUT ON
DECLARE
  v_text VARCHAR2(2000);
  v_updated PLS_INTEGER := 0;
BEGIN
  UPDATE t
  SET counter = nvl(counter, 0) + 1
  WHERE category = 1
  RETURNING
    listagg (text, ', ') WITHIN GROUP (ORDER BY text),
    count(*)
  INTO
    v_text,
    v_updated;

  COMMIT;
  dbms_output.put_line('Rows updated: ' || v_updated);
  dbms_output.put_line('Returned:     ' || v_text);
END;
/
```

출력 (row-by-row 접근 방식과 동일):
```
Rows updated: 6
Returned:     UIXSzJxDez, hkvvrTRbTC, IBOJYveDgf, VhcwOugrWB, gBJFJrPQYy, bVzfHznOUj
```

## 로직 비교

집합 기반 접근 방식은 네 가지 영역의 로직을 매핑합니다:

1. 카테고리 조건식: SELECT에서 UPDATE WHERE 절로 이동
2. 행 개수: 카운터 변수 대신 RETURNING 절의 COUNT(*)로 대체
3. 문자열 연결: 수동 연결 대신 RETURNING에서 LISTAGG() 함수 사용
4. 실제 업데이트: 행별 개별 업데이트 대신 단일 벌크 UPDATE 문

## 성능 결과

테스트 1: 실행당 6개 행 업데이트 (5 x 10,000회 실행)

Row-by-row 접근 방식 평균: 2.43714초
집합 기반 접근 방식 평균: 1.04562초
성능 향상: 약 2.5배 빠름

샘플 실행 결과는 일관된 결과를 보여줍니다:
```
Run 1, Statement 1: 2.63841 (avg: 2.43714)
Run 1, Statement 2: 1.11019 (avg: 1.04562)
Run 2, Statement 1: 2.35626 (avg: 2.43714)
Run 2, Statement 2: 1.05716 (avg: 1.04562)
Run 3, Statement 1: 2.38004 (avg: 2.43714)
Run 3, Statement 2: 1.05153 (avg: 1.04562)
Run 4, Statement 1: 2.47451 (avg: 2.43714)
Run 4, Statement 2: 1.00921 (avg: 1.04562)
Run 5, Statement 1: 2.33649 (avg: 2.43714)
Run 5, Statement 2: 1.00000 (avg: 1.04562)
```

테스트 2: 전체 100개 행 업데이트 (5 x 2,000회 실행)

Row-by-row 접근 방식 평균: 11.98154초
집합 기반 접근 방식 평균: 1.73926초
성능 향상: 약 7배 빠름

샘플 실행:
```
Run 1, Statement 1: 10.21833 (avg: 11.98154)
Run 1, Statement 2: 1.21913 (avg: 1.73926)
Run 2, Statement 1: 10.17014 (avg: 11.98154)
Run 2, Statement 2: 3.02793 (avg: 1.73926)
Run 3, Statement 1: 9.44462 (avg: 11.98154)
Run 3, Statement 2: 1.00000 (avg: 1.73926)
Run 4, Statement 1: 20.54692 (avg: 11.98154)
Run 4, Statement 2: 1.19356 (avg: 1.73926)
Run 5, Statement 1: 9.52769 (avg: 11.98154)
Run 5, Statement 2: 2.25568 (avg: 1.73926)
```

## 주의사항

벌크 업데이트는 row-by-row 업데이트보다 훨씬 좋습니다. 옵티마이저가 어떤 행이 미리 업데이트될지 알 때 더 효율적으로 계획을 세울 수 있기 때문입니다. 그러나 많은 프로세스가 벌크 업데이트 중에 동일한 데이터를 읽는 상황에서는 락과 로그 파일 경합이 발생할 수 있습니다. 하나의 접근 방식이 모든 시나리오에 맞지는 않지만, 결과 집합을 순회하며 데이터를 업데이트할 때마다 단일 SQL 문으로 동일한 목표를 달성할 수 있는지 고려해 보세요. 대개 대답은 "예"입니다.

## 데이터베이스 지원

### Oracle
집계 함수와 함께 RETURNING 절 사용:
```sql
UPDATE t
SET counter = nvl(counter, 0) + 1
WHERE category = 1
RETURNING
  listagg (text, ', ') WITHIN GROUP (ORDER BY text),
  count(*)
INTO
  v_text,
  v_updated;
```

### Firebird
Oracle과 정확히 동일하게 지원: RETURNING 구문

### PostgreSQL
RETURNING을 지원하지만 제한이 있습니다. RETURNING에서 직접 집계 함수를 사용할 수 없습니다. 대신 CTE로 감싸야 합니다:
```sql
WITH cte AS (
  UPDATE tab
  SET i = 10 * i
  RETURNING *
)
SELECT COUNT(*), STRING_AGG(i::text, ',' ORDER BY i)
FROM cte;
```

### SQL Server
OUTPUT 절을 제공하지만 Oracle보다 덜 강력합니다. OUTPUT 절에서 집계 함수를 허용하지 않습니다:
```
Error: An aggregate may not appear in the OUTPUT clause.
(오류: OUTPUT 절에 집계가 나타날 수 없습니다.)
```

### DB2
FINAL TABLE 래퍼로 SQL 표준을 구현합니다. 가장 우아한 접근 방식:
```sql
SELECT
  listagg (text, ', ') WITHIN GROUP (ORDER BY id),
  count(*)
FROM FINAL TABLE (
  UPDATE t
  SET counter = nvl(counter, 0) + 1
  WHERE category = 1
)
```

DB2 구문은 우아함과 SQL 표준 준수로 주목할 만합니다.

## 핵심 요점

RETURNING 절이 있는 집합 기반 SQL UPDATE 작업은 코드 복잡성을 줄이고 단일 작업에서 수정된 행 데이터를 검색할 수 있게 하면서 row-by-row 접근 방식보다 상당한 성능 향상을 제공합니다.
