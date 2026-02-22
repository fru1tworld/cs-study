# SQL로 아카이브 로그의 델타를 스냅샷으로 집계하는 방법

> 원문: https://blog.jooq.org/how-to-aggregate-an-archive-logs-deltas-into-a-snapshot-with-sql/

## 서론

SQL 교육 고객 중 한 분이 "주어진 시점에 특정 레코드의 스냅샷을 얻기 위해 아카이브 로그의 델타들을 병합하는" 계층적 쿼리를 최적화해 달라는 도전을 제시했습니다. 이 글에서는 JSON, 윈도우 함수, CROSS APPLY, 그리고 CASE 표현식 집계를 포함한 SQL Server 기능을 사용한 솔루션을 보여드리겠습니다.

## 문제 설명: EAV 모델 구현

고객은 스키마 불확실성을 위해 Entity-Attribute-Value(EAV) 시스템을 설계했습니다. 두 가지 구현 접근 방식이 있습니다:

클래식 SQL 테이블:
```sql
CREATE TABLE eav_classic (
  entity_type     VARCHAR (100) NOT NULL,
  entity_id       BIGINT        NOT NULL,
  attribute_name  VARCHAR (100) NOT NULL,
  attribute_type  VARCHAR (100) NOT NULL,
  attribute_value VARCHAR (100)     NULL,
  CONSTRAINT eav_classic_pk
    PRIMARY KEY (entity_type, entity_id, attribute_name)
);
```

JSON 기반 테이블:
```sql
CREATE TABLE eav_json (
  entity_type     VARCHAR (100)   NOT NULL,
  entity_id       BIGINT          NOT NULL,
  attributes      VARCHAR (10000) NOT NULL
    CHECK (ISJSON(attributes) = 1),
  CONSTRAINT eav_json_pk
    PRIMARY KEY (entity_type, entity_id)
);
```

## EAV 테이블 버전 관리

```sql
CREATE TABLE history (
  id          BIGINT IDENTITY (1, 1) NOT NULL PRIMARY KEY,
  ts          DATETIME               NOT NULL,
  entity_type VARCHAR(100)           NOT NULL,
  entity_id   BIGINT                 NOT NULL,
  delta       VARCHAR(8000)          NOT NULL
    CHECK (ISJSON(delta) = 1)
);

INSERT INTO history (entity_type, entity_id, ts, delta)
VALUES
  ('Person', 1, '2000-01-01 00:00:00', '{"first_name": "John", "last_name": "Doe"}'),
  ('Person', 1, '2000-01-01 01:00:00', '{"age": 37}'),
  ('Person', 1, '2000-01-01 02:00:00', '{"age": 38}'),
  ('Person', 1, '2000-01-01 03:00:00', '{"city": "New York"}'),
  ('Person', 1, '2000-01-01 04:00:00', '{"city": "Zurich", "age": null}');
```

이 델타 로그는 Person 엔티티에 대한 순차적인 업데이트를 나타내며, 전통적인 UPDATE 문과 동일합니다.

## 스냅샷 진화 테이블

| TIME | FIRST_NAME | LAST_NAME | AGE | CITY |
|------|-----------|-----------|-----|------|
| 00:00:00 | John | Doe | | |
| 01:00:00 | John | Doe | 37 | |
| 02:00:00 | John | Doe | 38 | |
| 03:00:00 | John | Doe | 38 | New York |
| 04:00:00 | John | Doe | | Zurich |

## 대안: SQL:2011 Temporal 테이블

SQL Server 2016 이상에서는 내장된 시간 쿼리를 지원합니다:
```sql
SELECT * FROM Person
FOR SYSTEM_TIME AS OF '2000-01-01 02:00:00.0000000';
```

Oracle은 유사한 플래시백 쿼리를 제공하지만 특정 구성이 필요합니다.

## 윈도우 함수를 사용한 커스텀 솔루션

각 속성에 대해 가장 최근 델타를 찾으려면:

```sql
SELECT [key], value, row_number() OVER (
  PARTITION BY [key] ORDER BY ts DESC) rn
FROM history OUTER APPLY openjson(delta)
ORDER BY [key], ts;
```

rn = 1로 필터링한 결과:

| key | value |
|-----|-------|
| first_name | John |
| last_name | Doe |
| age | (null) |
| city | Zurich |

## 완전한 스냅샷 재구성 쿼리

```sql
SELECT
  '{'
+ string_agg(
    CASE type WHEN 0 THEN NULL ELSE
      '"' + replace([key], '"', '\"') + '": ' +
      CASE type WHEN 1 THEN '"' + replace(value, '"', '\"') + '"'
      ELSE value END
    END, ', ')
+ '}'
FROM (
  SELECT *, row_number() OVER (
    PARTITION BY [key] ORDER BY ts DESC) rn
  FROM history
  OUTER APPLY openjson(delta)
  WHERE ts <= '2000-01-01 04:00:00'
) t
WHERE rn = 1;
```

출력: `{"city": "Zurich", "first_name": "John", "last_name": "Doe"}`

## 모든 스냅샷의 전체 히스토리

```sql
SELECT ts, (
  SELECT
    '{'
  + string_agg(
      CASE type WHEN 0 THEN NULL ELSE
        '"' + replace([key], '"', '\"') + '": ' +
        CASE type WHEN 1 THEN '"' + replace(value, '"', '\"') + '"'
        ELSE value END
      END, ', ')
  + '}'
  FROM (
    SELECT *, row_number() OVER (
      PARTITION BY [key] ORDER BY ts DESC) rn
    FROM history
    OUTER APPLY openjson(delta)
    WHERE ts <= x.ts
  ) t
  WHERE rn = 1
)
FROM history x
GROUP BY ts;
```

| ts | (스냅샷 JSON) |
|----|-----------------|
| 00:00:00 | {"first_name": "John", "last_name": "Doe"} |
| 01:00:00 | {"age": 37, "first_name": "John", "last_name": "Doe"} |
| 02:00:00 | {"age": 38, "first_name": "John", "last_name": "Doe"} |
| 03:00:00 | {"age": 38, "city": "New York", "first_name": "John", "last_name": "Doe"} |
| 04:00:00 | {"city": "Zurich", "first_name": "John", "last_name": "Doe"} |

## 핵심 기법

이 솔루션은 "속성별 델타 순위를 매기기 위한 윈도우 함수, JSON을 확장하기 위한 OUTER APPLY, 그리고 객체를 재구성하기 위한 STRING_AGG"를 활용하여, 전용 Temporal 테이블 기능 없이도 감사 및 아카이브 요구사항을 위한 실용적인 SQL 역량을 보여줍니다.
