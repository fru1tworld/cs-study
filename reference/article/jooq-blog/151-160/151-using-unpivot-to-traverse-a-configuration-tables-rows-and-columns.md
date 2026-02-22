# UNPIVOT을 사용하여 설정 테이블의 행과 열 순회하기

> 원문: https://blog.jooq.org/using-unpivot-to-traverse-a-configuration-tables-rows-and-columns/

이 글에서는 SQL의 `UNPIVOT` 절을 사용하여 비정규화된 설정 테이블을 정규화된 쿼리 가능한 형식으로 변환하는 방법을 설명합니다.

## 문제 상황

설정 테이블은 하루에 여러 번 읽히지만, 일반적인 비정규화 구조는 순회하기가 어렵습니다. 다음과 같은 `RULE` 테이블이 있다고 가정해 봅시다:

```sql
CREATE TABLE rule (
  name     VARCHAR2(50)         NOT NULL PRIMARY KEY,
  enabled  NUMBER(1)  DEFAULT 1 NOT NULL CHECK (enabled IN (0,1)),
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  flag1    NUMBER(3)  DEFAULT 0 NOT NULL,
  flag2    NUMBER(3)  DEFAULT 0 NOT NULL,
  flag3    NUMBER(3)  DEFAULT 0 NOT NULL,
  flag4    NUMBER(3)  DEFAULT 0 NOT NULL,
  flag5    NUMBER(3)  DEFAULT 0 NOT NULL
);
```

이 테이블은 여러 플래그 컬럼(FLAG1-FLAG5)을 별도의 필드로 저장합니다. 이런 구조는 편집하기에는 편리하지만, 프로그래밍 방식으로 읽기에는 번거롭습니다.

## UNPIVOT 해결책

`UNPIVOT`은 넓은 데이터(wide data)를 긴 형식(long format)으로 변환합니다:

```sql
SELECT name, flag, value
FROM rule
UNPIVOT (
  value FOR flag IN (flag1, flag2, flag3, flag4, flag5)
)
WHERE enabled = 1
AND value > 0
ORDER BY priority, value;
```

이 쿼리는 각 플래그가 별도의 레코드가 되는 정규화된 행을 생성하며, 비활성화된 항목은 자동으로 필터링됩니다.

## 성능 이점

테스트 결과 상당한 효율성 향상이 있었습니다. `UNPIVOT` 방식은 테이블에 한 번만 접근하는 반면, 동등한 `UNION ALL` 대안은 여러 번의 테이블 스캔을 수행합니다.

### UNION ALL 대안 (비교용)

```sql
SELECT name, 'FLAG1' AS flag, flag1 AS value FROM rule
WHERE enabled = 1 AND flag1 > 0
UNION ALL
SELECT name, 'FLAG2' AS flag, flag2 AS value FROM rule
WHERE enabled = 1 AND flag2 > 0
UNION ALL
SELECT name, 'FLAG3' AS flag, flag3 AS value FROM rule
WHERE enabled = 1 AND flag3 > 0
UNION ALL
SELECT name, 'FLAG4' AS flag, flag4 AS value FROM rule
WHERE enabled = 1 AND flag4 > 0
UNION ALL
SELECT name, 'FLAG5' AS flag, flag5 AS value FROM rule
WHERE enabled = 1 AND flag5 > 0
ORDER BY priority, value;
```

벤치마크 결과, `UNION ALL` 버전은 여러 실행에서 일관되게 약 2배 느렸습니다.

## 고급 기법: 규칙 경계 감지하기

`LEAD()`와 `LAG()` 같은 윈도우 함수(Window Functions)를 사용하면 결과 집합 내에서 각 규칙이 시작하고 끝나는 위치를 식별할 수 있습니다. 이를 통해 설정 데이터를 정교하게 절차적으로 소비할 수 있습니다.

```sql
SELECT
  name,
  flag,
  value,
  CASE WHEN LAG(name) OVER (ORDER BY priority, value) IS DISTINCT FROM name
       THEN 1 ELSE 0 END AS rule_start,
  CASE WHEN LEAD(name) OVER (ORDER BY priority, value) IS DISTINCT FROM name
       THEN 1 ELSE 0 END AS rule_end
FROM rule
UNPIVOT (
  value FOR flag IN (flag1, flag2, flag3, flag4, flag5)
)
WHERE enabled = 1
AND value > 0
ORDER BY priority, value;
```

## 결론

`UNPIVOT`은 비정규화된 설정 테이블을 효율적으로 순회할 수 있는 강력한 SQL 기능입니다. 단일 테이블 접근으로 성능 이점을 제공하며, 윈도우 함수와 결합하면 더욱 정교한 데이터 처리가 가능합니다.

### 지원 데이터베이스

- Oracle
- SQL Server
- PostgreSQL (crosstab 함수 사용)
- 기타 UNPIVOT을 지원하는 데이터베이스
