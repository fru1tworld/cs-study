# Druid SQL

> 원본: https://druid.apache.org/docs/latest/querying/sql
> 원본: https://druid.apache.org/docs/latest/querying/sql-data-types
> 원본: https://druid.apache.org/docs/latest/querying/sql-scalar
> 원본: https://druid.apache.org/docs/latest/querying/sql-aggregations
> 원본: https://druid.apache.org/docs/latest/querying/sql-metadata-tables
> 원본: https://druid.apache.org/docs/latest/querying/sql-translation
> 원본: https://druid.apache.org/docs/latest/api-reference/sql-api

Druid SQL의 문법, 데이터 타입, 스칼라·집계 함수, 메타데이터 테이블, 네이티브 쿼리 변환 과정, SQL API를 정리합니다.

---

## 목차

1. [Druid SQL 개요](#druid-sql-개요)
2. [SELECT 문법](#select-문법)
3. [데이터 타입](#데이터-타입)
4. [스칼라 함수](#스칼라-함수)
5. [집계 함수](#집계-함수)
6. [메타데이터 테이블](#메타데이터-테이블)
7. [SQL → 네이티브 쿼리 변환](#sql--네이티브-쿼리-변환)
8. [SQL API](#sql-api)

---

## Druid SQL 개요

Druid SQL은 Apache Calcite 기반의 파서와 플래너를 사용합니다. SQL 쿼리는 Broker에서 네이티브 쿼리로 변환된 뒤 데이터 서버에서 실행되므로, 네이티브 쿼리 대비 변환에 드는 약간의 오버헤드를 제외하면 성능 저하가 거의 없습니다.

### 식별자와 리터럴

- **식별자(identifier)**: 큰따옴표로 선택적으로 감쌉니다. 식별자 내부의 큰따옴표는 두 번 써서 이스케이프합니다(`"My ""very own"" identifier"`). 식별자는 대소문자를 구분하며 암묵적 변환이 없습니다.
- **문자열 리터럴**: 작은따옴표를 사용합니다(`'foo'`). 유니코드 이스케이프는 `U&'fo\00F6'` 형태로 씁니다.
- **숫자 리터럴**: `100`(정수), `100.0`(실수), `1.0e5`(지수 표기)
- **타임스탬프 리터럴**: `TIMESTAMP '2000-01-01 00:00:00'`
- **인터벌 리터럴**: `INTERVAL '1' HOUR`, `INTERVAL '1 02:03' DAY TO MINUTE`, `INTERVAL '1-2' YEAR TO MONTH`

### 예약어

Druid는 Apache Calcite의 예약어에 더해 `CLUSTERED`, `PARTITIONED`를 예약어로 사용합니다. 예약어를 식별자로 쓰려면 큰따옴표로 감쌉니다.

```sql
SELECT "PARTITIONED" from druid.table
```

### 동적 파라미터

물음표(`?`) 자리표시자를 쿼리에 넣고 실행 시점에 값을 바인딩합니다. HTTP POST와 JDBC API 모두 지원합니다.

```json
{
  "query": "SELECT doubleArrayColumn from druid.table where ARRAY_CONTAINS(doubleArrayColumn, ?)",
  "parameters": [
    {
      "type": "ARRAY",
      "value": [-25.7, null, 36.85]
    }
  ]
}
```

타입 추론이 모호하면 CAST로 명시합니다.

```sql
SELECT * FROM druid.foo WHERE dim1 like CONCAT('%', CAST (? AS VARCHAR), '%')
```

IN 필터에는 `SCALAR_IN_ARRAY`와 배열 파라미터를 조합합니다.

```sql
SELECT count(city) from druid.table where SCALAR_IN_ARRAY(city, ?)
```

### SET 문

쿼리 컨텍스트 파라미터를 쿼리 앞에 지정합니다. SET 문은 같은 요청 안의 쿼리에만 적용되며, SELECT·INSERT·REPLACE 쿼리에서 사용할 수 있습니다. 리터럴 값만 허용하므로 배열이나 JSON 객체는 API의 `context` 필드를 사용해야 하며, 둘 다 있으면 SET이 우선합니다.

```sql
SET useApproximateTopN = false;
SET sqlTimeZone = 'America/Los_Angeles';
SET timeout = 90000;
SELECT some_column, COUNT(*) FROM druid.foo WHERE other_column = 'foo' GROUP BY 1 ORDER BY 2 DESC
```

---

## SELECT 문법

Druid SQL의 SELECT 문은 다음 구조를 따릅니다.

```
[ EXPLAIN PLAN FOR ]
[ WITH tableName [ ( column1, column2, ... ) ] AS ( query ) ]
SELECT [ ALL | DISTINCT ] { * | exprs }
FROM { <table> | (<subquery>) | <o1> [ INNER | LEFT ] JOIN <o2> ON condition }
[ PIVOT (...) ]
[ UNPIVOT (...) ]
[ CROSS JOIN UNNEST(source_expression) as table_alias_name(column_alias_name) ]
[ WHERE expr ]
[ GROUP BY [ exprs | GROUPING SETS | ROLLUP | CUBE ] ]
[ HAVING expr ]
[ ORDER BY expr [ ASC | DESC ], ... ]
[ LIMIT limit ]
[ OFFSET offset ]
[ UNION ALL <another query> ]
```

### FROM 절

FROM 절에서 참조할 수 있는 대상은 다음과 같습니다.

- `druid` 스키마의 테이블 데이터소스(기본 스키마이므로 접두사 생략 가능)
- `lookup` 스키마의 lookup(예: `lookup.countries`)
- 서브쿼리
- 호환되는 소스 간의 JOIN(조인 조건은 동등 비교여야 함)
- `INFORMATION_SCHEMA`, `sys` 스키마의 메타데이터 테이블

### PIVOT

PIVOT 연산자는 집계를 수행하면서 행 값을 컬럼으로 변환합니다.

```
PIVOT (aggregation_function(column_to_aggregate)
       FOR column_with_values_to_pivot
       IN (pivoted_column1 [, pivoted_column2 ...]))
```

`cityName` 값을 컬럼으로 펼치는 예시입니다.

```sql
SELECT user, channel, ba_sum_deleted, ny_sum_deleted
FROM "wikipedia"
PIVOT (SUM(deleted) AS "sum_deleted"
       FOR "cityName"
       IN ( 'Buenos Aires' AS ba, 'New York' AS ny))
WHERE ba_sum_deleted IS NOT NULL OR ny_sum_deleted IS NOT NULL
LIMIT 15
```

### UNPIVOT

UNPIVOT 연산자는 기존 컬럼 값을 행으로 변환합니다.

```
UNPIVOT (values_column
         FOR names_column
         IN (unpivoted_column1 [, unpivoted_column2 ... ]))
```

`added`, `deleted` 컬럼을 행으로 펼치는 예시입니다.

```sql
SELECT channel, user, action, SUM(changes) AS total_changes
FROM "wikipedia"
UNPIVOT ( changes FOR action IN ("added", "deleted") )
WHERE channel LIKE '#ar%'
GROUP BY channel, user, action
LIMIT 15
```

### UNNEST

UNNEST 절은 ARRAY 타입 값을 개별 행으로 펼칩니다.

```sql
SELECT column_alias_name
FROM datasource
CROSS JOIN UNNEST(source_expression1) AS table_alias_name1(column_alias_name1)
CROSS JOIN UNNEST(source_expression2) AS table_alias_name2(column_alias_name2) ...
```

주요 특징은 다음과 같습니다.

- 소스는 테이블, 필터링한 부분 집합, JOIN 결과 모두 가능합니다.
- 소스 표현식은 ARRAY 타입이어야 하며, 다중값 VARCHAR는 `MV_TO_ARRAY(dimension)`로 변환합니다.
- `ARRAY[dim1,dim2]`, `ARRAY_CONCAT(dim1,dim2)` 같은 표현식도 사용할 수 있습니다.
- alias 절(`AS table_alias_name(column_alias_name)`)은 필수는 아니지만 권장합니다.
- 한 쿼리에서 UNNEST를 여러 번 사용할 수 있습니다.
- 대부분의 경우 데이터소스와 UNNEST 함수 사이에 CROSS JOIN이 필요합니다.
- Druid는 내부적으로 `j0.unnest` 가상 컬럼을 사용합니다.
- UNNEST는 펼치는 원본 배열의 순서를 유지합니다.

제약 사항: 중복 값과 null을 제거하지 않으며, 중첩 JSON(COMPLEX) 타입 내부의 복합 객체 배열은 지원하지 않습니다.

### WHERE 절

FROM 테이블의 컬럼을 필터링하며 네이티브 필터로 변환됩니다. 문자열과 숫자는 암묵적 타입 변환으로 비교할 수 있지만, 성능을 위해 명시적으로 캐스팅하는 것이 좋습니다.

```sql
WHERE stringDim = '1'
```

문자열 디멘션(dimension)을 숫자 목록과 비교할 때는 문자열 목록으로 씁니다.

```sql
WHERE stringDim IN ('1', '2', '3')
```

`WHERE col1 IN (SELECT foo FROM ...)` 형태의 서브쿼리도 지원합니다.

### GROUP BY 절

표현식뿐 아니라 서수 위치(예: `GROUP BY 2`)로도 지정할 수 있으며, 다음과 같은 다중 그룹핑을 지원합니다.

- `GROUP BY GROUPING SETS ( (country, city), () )`
- `GROUP BY ROLLUP (country, city)` — 여러 grouping set과 동등
- `GROUP BY CUBE (country, city)` — 모든 조합을 계산

특정 행에 적용되지 않는 그룹핑 컬럼은 `NULL`이 됩니다. 원래 데이터의 NULL과 구분하려면 `GROUPING` 집계를 사용합니다.

### HAVING / ORDER BY

- HAVING 절은 GROUP BY 실행 후 결과를 필터링하며, 그룹핑 컬럼 또는 집계 값을 참조할 수 있습니다. GROUP BY가 있는 쿼리에서만 사용합니다.
- ORDER BY 절은 표현식 또는 서수 위치를 참조할 수 있습니다. 비집계 쿼리에서는 `__time`으로만 정렬할 수 있고, 집계 쿼리에서는 임의의 컬럼으로 정렬할 수 있습니다.

### LIMIT / OFFSET

- LIMIT은 반환 행 수를 제한합니다. 상황에 따라 Druid가 limit을 데이터 서버로 push down하여 성능을 높이며, 네이티브 Scan·TopN 쿼리 타입으로 실행되는 쿼리에서는 항상 push down합니다.
- OFFSET은 지정한 수만큼 행을 건너뜁니다. LIMIT과 함께 쓰면 OFFSET을 먼저 적용한 뒤 LIMIT을 적용합니다. 예를 들어 `LIMIT 100 OFFSET 10`은 10번째 행부터 100개를 반환합니다.
- 건너뛴 행도 내부적으로 생성한 뒤 버리므로, OFFSET을 크게 잡으면 추가 리소스를 소모합니다. OFFSET은 네이티브 Scan과 GroupBy 쿼리 타입에서만 지원합니다.

### UNION ALL

**최상위 UNION ALL**은 쿼리의 가장 바깥에서 사용합니다(서브쿼리나 FROM 절에서는 불가). 각 쿼리를 순차 실행하고 결과를 이어 붙이며, 결과 전체에 GROUP BY·ORDER BY 등의 연산을 적용할 수 없습니다.

```sql
SELECT COUNT(*) FROM tbl WHERE my_column = 'value1'
UNION ALL
SELECT COUNT(*) FROM tbl WHERE my_column = 'value2'
```

**테이블 수준 UNION ALL**은 FROM 절의 서브쿼리 안에서 사용하며, 하위 서브쿼리는 표현식·별칭·JOIN·GROUP BY·ORDER BY가 없는 단순 테이블 SELECT여야 합니다. 각 테이블에서 같은 컬럼을 같은 순서로 선택해야 하고, 컬럼 타입이 같거나 서로 암묵적 캐스팅이 가능해야 합니다.

```sql
SELECT col1, COUNT(*)
FROM (
  SELECT col1, col2, col3 FROM tbl1
  UNION ALL
  SELECT col1, col2, col3 FROM tbl2
)
GROUP BY col1
```

`TABLE(APPEND())`로 같은 효과를 낼 수도 있습니다.

```sql
SELECT col1, COUNT(*) from TABLE(APPEND('tbl1', 'tbl2'))
```

### EXPLAIN PLAN

쿼리 앞에 `EXPLAIN PLAN FOR`를 붙이면 실제로 실행하지 않고 네이티브 쿼리 변환 결과를 확인할 수 있습니다.

```sql
EXPLAIN PLAN FOR SELECT ...
```

---

## 데이터 타입

Druid가 네이티브로 지원하는 기본 컬럼 타입은 다음과 같습니다.

- `LONG`: 64비트 부호 있는 정수
- `FLOAT`: 32비트 부동소수점
- `DOUBLE`: 64비트 부동소수점
- `STRING`: UTF-8 문자열과 문자열 배열
- `COMPLEX`: 중첩 JSON, hyperUnique, approxHistogram, DataSketches 등 비표준 타입
- `ARRAY`: 위 타입들로 구성된 배열

### 표준 타입 매핑

| SQL 타입 | Druid 런타임 타입 | 기본값 | 비고 |
|----------|-------------------|--------|------|
| CHAR | STRING | `''` | |
| VARCHAR | STRING | `''` | Druid STRING 컬럼은 VARCHAR로 표시됩니다 |
| DECIMAL | DOUBLE | `0.0` | 고정소수점이 아닌 부동소수점 연산을 사용합니다 |
| FLOAT | FLOAT | `0.0` | Druid FLOAT 컬럼은 FLOAT로 표시됩니다 |
| REAL | DOUBLE | `0.0` | |
| DOUBLE | DOUBLE | `0.0` | Druid DOUBLE 컬럼은 DOUBLE로 표시됩니다 |
| BOOLEAN | LONG | `false` | |
| TINYINT | LONG | `0` | |
| SMALLINT | LONG | `0` | |
| INTEGER | LONG | `0` | |
| BIGINT | LONG | `0` | `__time`을 제외한 Druid LONG 컬럼은 BIGINT로 표시됩니다 |
| TIMESTAMP | LONG | `0` (1970-01-01 UTC) | 문자열과 타임스탬프 간 캐스팅은 표준 SQL 형식을 가정합니다 |
| DATE | LONG | `0` (1970-01-01) | TIMESTAMP를 DATE로 캐스팅하면 일 단위로 내림합니다 |
| ARRAY | ARRAY | `NULL` | Druid 네이티브 배열 타입은 SQL 배열로 동작합니다 |
| OTHER | COMPLEX | 없음 | hyperUnique, approxHistogram 등을 표현합니다 |

### 타임스탬프 처리

Druid는 `__time` 컬럼을 포함한 타임스탬프를 LONG으로 다루며, 값은 1970-01-01 00:00:00 UTC 이후의 밀리초 수(윤초 제외)입니다. 따라서 Druid의 타임스탬프는 시간대 정보를 담지 않습니다.

### 캐스팅 규칙

- Druid 런타임 타입이 같은 SQL 타입 간 캐스팅은 효과가 없습니다(명시된 예외 제외).
- 런타임 타입이 다르면 Druid에서 런타임 캐스팅을 수행합니다.
- 캐스팅에 실패하면 NULL로 대체합니다(예: `CAST('foo' AS BIGINT)`).

### 배열

Druid의 `ARRAY` 타입은 표준 SQL 배열처럼 동작하며, 그룹핑 시 배열 전체가 일치하는 값끼리 묶입니다. `UNNEST` 연산자로 배열 원소를 개별 행으로 펼쳐 원소 단위 연산을 수행할 수 있습니다. SQL 기반 인제스천에서 배열을 적재하려면 `arrayIngestMode` 파라미터를 `"array"`로 설정해야 합니다. 결과는 기본적으로 JSON 문자열로 직렬화되며, `sqlStringifyArrays` 컨텍스트 파라미터로 제어합니다.

### 다중값 문자열

Druid 네이티브 타입 시스템에서는 문자열이 여러 값을 가질 수 있습니다. 다중값 문자열 디멘션은 SQL에서 VARCHAR 타입으로 표시되며 문법상 일반 VARCHAR처럼 사용할 수 있습니다. 표준 VARCHAR 함수는 행마다 모든 값에 적용됩니다. `MV_` 접두사가 붙은 다중값 전용 함수는 값을 배열처럼 처리하면서 VARCHAR 타입을 유지합니다. 그룹핑을 적용하면 암묵적 UNNEST가 일어나 단일값 VARCHAR 결과를 만듭니다.

### NULL과 불리언 논리

- Druid는 기본적으로 NULL 값을 ANSI SQL 표준과 유사하게 처리합니다.
- 필터 처리와 불리언 표현식 평가에는 SQL 3치 논리(three-valued logic)를 사용합니다.

### 중첩 컬럼

Druid는 네이티브 `COMPLEX<json>` 타입으로 세그먼트에 중첩 데이터 구조를 저장할 수 있습니다. 중첩 데이터는 JSON 함수로 추출·파싱·직렬화하거나 새 구조를 만들 수 있습니다. COMPLEX 타입은 전용 처리 없이 그룹핑·직접 필터링·집계에 사용하면 동작이 정의되지 않습니다.

---

## 스칼라 함수

대표적인 함수를 카테고리별로 정리합니다. 전체 목록은 원본 문서를 참고합니다.

### 숫자 함수

| 함수 | 설명 |
|------|------|
| `ABS(expr)` | 절댓값을 반환합니다 |
| `ROUND(expr[, digits])` | 지정한 소수 자릿수로 반올림합니다. digits가 음수면 소수점 왼쪽 자리에서 반올림합니다 |
| `POWER(expr, power)` | 거듭제곱을 계산합니다 |
| `SQRT(expr)` | 제곱근을 반환합니다 |
| `MOD(x, y)` | x를 y로 나눈 나머지를 반환합니다 |
| `BITWISE_AND(expr1, expr2)` | 비트 AND 연산을 수행합니다 |
| `SAFE_DIVIDE(x, y)` | 0으로 나누면 오류 대신 null을 반환하는 나눗셈입니다 |

### 문자열 함수

| 함수 | 설명 |
|------|------|
| `CONCAT(expr[, expr, ...])` | 표현식 목록을 이어 붙입니다 |
| `LENGTH(expr)` | UTF-16 코드 단위 기준 문자열 길이를 반환합니다 |
| `UPPER(expr)` | 문자열을 모두 대문자로 변환합니다 |
| `SUBSTRING(expr, index[, length])` | 1부터 시작하는 index 위치에서 부분 문자열을 추출합니다 |
| `REGEXP_EXTRACT(expr, pattern[, index])` | 정규식 패턴을 적용해 매칭 그룹을 추출합니다 |
| `REPLACE(expr, substring, replacement)` | 모든 substring 출현을 치환합니다 |
| `TRIM([direction] [chars FROM] expr)` | 문자열 양끝에서 지정 문자를 제거합니다 |

### 날짜·시간 함수

| 함수 | 설명 |
|------|------|
| `CURRENT_TIMESTAMP` | 현재 타임스탬프를 UTC 기준으로 반환합니다(다른 시간대를 지정하지 않는 한) |
| `DATE_TRUNC(unit, timestamp_expr)` | 타임스탬프를 내림해 새 타임스탬프로 반환합니다 |
| `TIME_FLOOR(timestamp_expr, period[, origin[, timezone]])` | ISO 8601 period 기준으로 내림합니다 |
| `TIME_EXTRACT(timestamp_expr, unit[, timezone])` | 시간 구성 요소를 숫자로 추출합니다 |
| `TIME_PARSE(string_expr[, pattern[, timezone]])` | 패턴으로 문자열을 파싱해 타임스탬프로 변환합니다 |
| `TIMESTAMPDIFF(unit, timestamp1, timestamp2)` | 두 타임스탬프 간 부호 있는 시간 차이를 반환합니다 |

### 축약(reduction) 함수

| 함수 | 설명 |
|------|------|
| `GREATEST([expr1, ...])` | 0개 이상의 표현식을 평가해 최댓값을 반환합니다 |
| `LEAST([expr1, ...])` | 0개 이상의 표현식을 평가해 최솟값을 반환합니다 |

### IP 주소 함수

| 함수 | 설명 |
|------|------|
| `IPV4_MATCH(address, subnet)` | 주소가 subnet 리터럴에 속하면 true, 아니면 false를 반환합니다 |
| `IPV4_PARSE(address)` | 주소를 정수로 저장되는 IPv4 주소로 파싱합니다 |
| `IPV6_MATCH(address, subnet)` | IPv6 주소가 subnet에 속하면 1, 아니면 0을 반환합니다 |

### 스케치 함수 (DataSketches)

| 함수 | 설명 |
|------|------|
| `HLL_SKETCH_ESTIMATE(expr[, round])` | HLL 스케치에서 고유 개수 추정치를 반환합니다 |
| `THETA_SKETCH_ESTIMATE(expr)` | Theta 스케치에서 고유 개수 추정치를 반환합니다 |
| `DS_GET_QUANTILE(expr, fraction)` | quantiles 스케치에서 분위수 추정치를 반환합니다 |

### 기타 함수

| 함수 | 설명 |
|------|------|
| `CASE expr WHEN value1 THEN result1 [...] END` | 단순 CASE 표현식입니다 |
| `CASE WHEN boolean_expr1 THEN result1 [...] END` | 조건 검색 CASE 표현식입니다 |
| `CAST(value AS TYPE)` | 타입을 변환합니다 |
| `COALESCE(value1, value2, ...)` | 첫 번째 non-null 값을 반환합니다 |
| `NULLIF(value1, value2)` | 두 값이 같으면 NULL, 다르면 value1을 반환합니다 |

---

## 집계 함수

### 공통 동작

- **FILTER 절**: `FILTER(WHERE condition)`으로 조건에 맞는 행만 집계할 수 있습니다. Druid는 이를 네이티브 filtered aggregator로 변환하므로, 한 쿼리 안에서 집계마다 다른 필터를 적용할 수 있습니다.
- **NULL 처리**: 선택된 행이 없으면 집계 함수는 초기값을 반환합니다. 필터가 모든 행을 제외하거나 그룹 집계에서 매칭이 없을 때 발생합니다.
- **DISTINCT**: `COUNT`, `ARRAY_AGG`, `STRING_AGG`만 DISTINCT 키워드를 허용합니다.
- **비결정적 순서**: 세그먼트 간 집계 연산 순서는 결정적이지 않으므로, 교환법칙이 성립하지 않는 집계 함수는 같은 쿼리에서도 결과가 달라질 수 있습니다. float/double 타입 연산에서 변동이 나타날 수 있으며, `ROUND` 함수로 완화할 수 있습니다.

### 기본 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `COUNT(*)` | 행 수를 셉니다 | `0` |
| `COUNT([DISTINCT] expr)` | 값 개수를 셉니다. DISTINCT는 `useApproximateCountDistinct=false`가 아닌 한 근사 계산을 사용합니다 | `0` |
| `SUM(expr)` | 숫자 값의 합을 구합니다 | `null` |
| `MIN(expr)` | 최솟값을 구합니다 | `null` |
| `MAX(expr)` | 최댓값을 구합니다 | `null` |
| `AVG(expr)` | 평균을 구합니다 | `null` |
| `ANY_VALUE(expr, [maxBytesPerValue, [aggregateMultipleValues]])` | null을 포함해 만나는 임의의 값을 반환하며 조기 반환에 최적화되어 있습니다 | `null` |

### 근사 고유 개수(approximate distinct count)

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `APPROX_COUNT_DISTINCT(expr)` | `druid.sql.approxCountDistinct.function`에 지정된 알고리즘으로 근사 고유 개수를 계산합니다 | `0` |
| `APPROX_COUNT_DISTINCT_BUILTIN(expr)` | Druid 내장 HyperLogLog 변형입니다. 문자열, 숫자, 사전 빌드된 hyperUnique 컬럼에 사용합니다 | `0` |
| `APPROX_COUNT_DISTINCT_DS_HLL(expr, [lgK, tgtHllType])` | DataSketches HLL 구현으로 일반적으로 더 나은 정확도를 제공합니다 | `0` |
| `APPROX_COUNT_DISTINCT_DS_THETA(expr, [size])` | DataSketches Theta 스케치를 사용합니다 | `0` |

### 분위수(quantile)

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `APPROX_QUANTILE(expr, probability, [resolution])` | **Deprecated.** approximate histogram을 사용합니다. probability는 0과 1 사이(경계 제외)입니다 | `NaN` |
| `APPROX_QUANTILE_FIXED_BUCKETS(expr, probability, numBuckets, lowerLimit, upperLimit, [outlierHandlingMode])` | 고정 버킷 히스토그램 기반 분위수입니다. approximate histogram 익스텐션이 필요합니다 | `0.0` |
| `APPROX_QUANTILE_DS(expr, probability, [k])` | DataSketches quantiles 스케치를 사용하며, 분포에 무관하게 동작하는 더 우수한 알고리즘입니다 | `NaN` |

### 시간 기준 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `EARLIEST(expr, [maxBytesPerValue])` | 가장 이른 non-null 타임스탬프를 가진 행의 값을 반환합니다 | `null` |
| `EARLIEST_BY(expr, timestampExpr, [maxBytesPerValue])` | 지정한 타임스탬프 기준으로 가장 이른 값을 반환합니다. rollup이 활성화된 테이블에서는 지정한 타임스탬프를 무시합니다 | `null` |
| `LATEST(expr, [maxBytesPerValue])` | 가장 늦은 non-null 타임스탬프를 가진 행의 값을 반환합니다 | `null` |
| `LATEST_BY(expr, timestampExpr, [maxBytesPerValue])` | 지정한 타임스탬프 기준으로 가장 늦은 값을 반환합니다. rollup이 활성화된 테이블에서는 지정한 타임스탬프를 무시합니다 | `null` |

### 배열·문자열 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `ARRAY_AGG([DISTINCT] expr, [size])` | 값을 배열로 모읍니다. size 기본값은 1024바이트이며 ORDER BY는 지원하지 않습니다 | `null` |
| `ARRAY_CONCAT_AGG([DISTINCT] expr, [size])` | 배열 입력을 이어 붙입니다. null 배열은 무시하지만 null 원소는 포함합니다 | `null` |
| `STRING_AGG([DISTINCT] expr, [separator, [size]])` | 값을 구분자로 연결한 문자열을 만듭니다. null을 무시하며 size 기본값은 1024바이트입니다 | `null` |
| `LISTAGG([DISTINCT] expr, [separator, [size]])` | STRING_AGG의 동의어입니다 | `null` |

### 통계 함수

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `VAR_POP(expr)` | 모분산 | `null` |
| `VAR_SAMP(expr)` | 표본분산 | `null` |
| `VARIANCE(expr)` | 표본분산(별칭) | `null` |
| `STDDEV_POP(expr)` | 모표준편차 | `null` |
| `STDDEV_SAMP(expr)` | 표본표준편차 | `null` |
| `STDDEV(expr)` | 표본표준편차(별칭) | `null` |

### 비트 연산 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `BIT_AND(expr)` | 모든 입력에 걸친 비트 AND | `null` |
| `BIT_OR(expr)` | 모든 입력에 걸친 비트 OR | `null` |
| `BIT_XOR(expr)` | 모든 입력에 걸친 비트 XOR | `null` |

### 스케치 생성 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `DS_HLL(expr, [lgK, tgtHllType])` | HLL 스케치를 생성합니다(DataSketches 익스텐션) | `'0'` (STRING) |
| `DS_THETA(expr, [size])` | Theta 스케치를 생성합니다(DataSketches 익스텐션) | `'0.0'` (STRING) |
| `DS_QUANTILES_SKETCH(expr, [k])` | quantiles 스케치를 생성합니다(DataSketches 익스텐션) | `'0'` (STRING) |
| `DS_TUPLE_DOUBLES(expr [, nominalEntries])` | double 배열을 담는 Tuple 스케치를 생성합니다(DataSketches 익스텐션) | 없음 |
| `TDIGEST_QUANTILE(expr, quantileFraction, [compression])` | T-Digest 분위수입니다. compression 기본값은 100입니다 | `Double.NaN` |
| `TDIGEST_GENERATE_SKETCH(expr, [compression])` | T-Digest 스케치를 생성합니다 | 빈 스케치(STRING) |

### 기타 특수 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `GROUPING(expr, expr...)` | GROUPING SETS에서 어떤 디멘션이 포함되었는지를 숫자로 나타냅니다 | 없음 |
| `BLOOM_FILTER(expr, numEntries)` | 지정한 최대 고유 값 수에 대한 bloom filter를 생성합니다 | 빈 base64 STRING |
| `SPECTATOR_COUNT(expr)` | Spectator 히스토그램의 관측 개수를 셉니다 | `0` |
| `SPECTATOR_PERCENTILE(expr, percentile)` | Spectator 히스토그램에서 근사 백분위수(0–100)를 구합니다 | `NaN` |

---

## 메타데이터 테이블

INFORMATION_SCHEMA 테이블과 sys 테이블은 `TIME_PARSE`, `APPROX_QUANTILE_DS` 같은 Druid 전용 함수를 지원하지 않으며 표준 SQL 함수만 사용할 수 있습니다.

### INFORMATION_SCHEMA

**SCHEMATA** — 알려진 모든 스키마를 나열합니다: `druid`(일반 데이터소스), `lookup`(lookup), `sys`(시스템 메타데이터), `INFORMATION_SCHEMA`(가상 테이블).

| 컬럼 | 타입 |
|------|------|
| CATALOG_NAME | VARCHAR |
| SCHEMA_NAME | VARCHAR |
| SCHEMA_OWNER | VARCHAR |
| DEFAULT_CHARACTER_SET_CATALOG | VARCHAR |
| DEFAULT_CHARACTER_SET_SCHEMA | VARCHAR |
| DEFAULT_CHARACTER_SET_NAME | VARCHAR |
| SQL_PATH | VARCHAR |

**TABLES** — 알려진 모든 테이블과 스키마를 나열합니다.

| 컬럼 | 타입 | 비고 |
|------|------|------|
| TABLE_CATALOG | VARCHAR | 항상 `druid` |
| TABLE_SCHEMA | VARCHAR | 소속 스키마 |
| TABLE_NAME | VARCHAR | `druid` 스키마에서는 dataSource 이름 |
| TABLE_TYPE | VARCHAR | "TABLE" 또는 "SYSTEM_TABLE" |
| IS_JOINABLE | VARCHAR | JOIN 우측에 직접 사용할 수 있으면 "YES", 아니면 "NO" |
| IS_BROADCAST | VARCHAR | 모든 노드에 브로드캐스트되면 "YES", 아니면 "NO" |

**COLUMNS** — 모든 테이블의 컬럼을 나열합니다. 주요 컬럼: `TABLE_CATALOG`(항상 `druid`), `TABLE_SCHEMA`, `TABLE_NAME`, `COLUMN_NAME`, `ORDINAL_POSITION`(저장 순서), `IS_NULLABLE`, `DATA_TYPE`(SQL 데이터 타입), `NUMERIC_PRECISION`, `NUMERIC_SCALE`, `DATETIME_PRECISION`, `JDBC_TYPE`(java.sql.Types 코드). `COLUMN_DEFAULT`, `CHARACTER_MAXIMUM_LENGTH`, `CHARACTER_OCTET_LENGTH`는 사용하지 않습니다.

```sql
SELECT "ORDINAL_POSITION", "COLUMN_NAME", "IS_NULLABLE", "DATA_TYPE", "JDBC_TYPE"
FROM INFORMATION_SCHEMA.COLUMNS
WHERE "TABLE_NAME" = 'foo'
```

**ROUTINES** — 알려진 모든 함수를 나열합니다.

| 컬럼 | 타입 | 비고 |
|------|------|------|
| ROUTINE_CATALOG | VARCHAR | 항상 `druid` |
| ROUTINE_SCHEMA | VARCHAR | 항상 `INFORMATION_SCHEMA` |
| ROUTINE_NAME | VARCHAR | 함수 이름 |
| ROUTINE_TYPE | VARCHAR | 항상 `FUNCTION` |
| IS_AGGREGATOR | VARCHAR | 집계 함수면 "YES", 아니면 "NO" |
| SIGNATURES | VARCHAR | 하나 이상의 함수 시그니처 |

```sql
SELECT "ROUTINE_CATALOG", "ROUTINE_SCHEMA", "ROUTINE_NAME", "ROUTINE_TYPE", "IS_AGGREGATOR", "SIGNATURES"
FROM "INFORMATION_SCHEMA"."ROUTINES"
WHERE "IS_AGGREGATOR" = 'YES'
```

### sys 스키마

**SEGMENTS** — published 여부와 관계없이 모든 세그먼트 정보를 담습니다.

| 컬럼 | 타입 | 비고 |
|------|------|------|
| segment_id | VARCHAR | 고유 식별자 |
| datasource | VARCHAR | 데이터소스 이름 |
| start | VARCHAR | 인터벌 시작(ISO 8601) |
| end | VARCHAR | 인터벌 끝(ISO 8601) |
| size | BIGINT | 세그먼트 크기(바이트) |
| version | VARCHAR | 버전 문자열(ISO 8601 타임스탬프, 높을수록 최신) |
| partition_num | BIGINT | 파티션 번호(datasource+interval+version 안에서 고유) |
| num_replicas | BIGINT | 현재 복제본 수 |
| num_rows | BIGINT | 행 수(모르면 0, 스트림 인제스천 세그먼트는 지연될 수 있음) |
| is_active | BIGINT | 데이터소스 최신 상태에 속하면 true |
| is_published | BIGINT | 메타데이터 저장소에 published되고 used로 표시되면 1 |
| is_available | BIGINT | Historical 또는 실시간 태스크가 서빙 중이면 1 |
| is_realtime | BIGINT | 실시간에서만 서빙하면 1, Historical이 서빙하면 0 |
| is_overshadowed | BIGINT | published 상태이면서 다른 세그먼트에 완전히 가려졌으면 1 |
| shard_spec | VARCHAR | JSON 직렬화된 ShardSpec |
| dimensions | VARCHAR | JSON 직렬화된 디멘션 목록 |
| metrics | VARCHAR | JSON 직렬화된 metric 목록 |
| last_compaction_state | VARCHAR | JSON 직렬화된 compaction 태스크 설정(compaction 이력이 없으면 null) |
| replication_factor | BIGINT | 티어 전체에 필요한 복제본 수(아직 평가 전이면 -1) |

활성 세그먼트 조회 예시입니다.

```sql
SELECT * FROM sys.segments
WHERE datasource = 'wikipedia'
AND is_active = 1
```

데이터소스별 통계 예시입니다.

```sql
SELECT
    datasource,
    SUM("size") AS total_size,
    CASE WHEN SUM("size") = 0 THEN 0 ELSE SUM("size") / (COUNT(*) FILTER(WHERE "size" > 0)) END AS avg_size,
    CASE WHEN SUM(num_rows) = 0 THEN 0 ELSE SUM("num_rows") / (COUNT(*) FILTER(WHERE num_rows > 0)) END AS avg_num_rows,
    COUNT(*) AS num_segments
FROM sys.segments
WHERE is_active = 1
GROUP BY 1
ORDER BY 2 DESC
```

compaction이 수행된 세그먼트 조회 예시입니다.

```sql
SELECT * FROM sys.segments WHERE is_active = 1 AND last_compaction_state IS NOT NULL
```

**SERVERS** — 클러스터에서 발견된 모든 서버를 나열합니다.

| 컬럼 | 타입 | 비고 |
|------|------|------|
| server | VARCHAR | 서버 이름(host:port 형식) |
| host | VARCHAR | 호스트명 |
| plaintext_port | BIGINT | 비보안 포트(비활성화면 -1) |
| tls_port | BIGINT | TLS 포트(비활성화면 -1) |
| server_type | VARCHAR | COORDINATOR, OVERLORD, BROKER, ROUTER, HISTORICAL, MIDDLE_MANAGER, PEON |
| tier | VARCHAR | 분배 티어(HISTORICAL 전용) |
| current_size | BIGINT | 현재 세그먼트 크기 합(바이트, HISTORICAL 전용) |
| max_size | BIGINT | 권장 최대 세그먼트 크기(HISTORICAL 전용) |
| is_leader | BIGINT | 리더면 1, 아니면 0, 리더 개념이 없으면 null |
| start_time | STRING | 서버가 자신을 알린 ISO 8601 타임스탬프 |
| version | VARCHAR | Druid 버전 |
| build_revision | VARCHAR | 빌드의 git 커밋 |
| labels | VARCHAR | `druid.labels`로 지정한 서버 레이블 |
| available_processors | BIGINT | 사용 가능한 CPU 프로세서 수 |
| total_memory | BIGINT | 전체 메모리(바이트) |

**SERVER_SEGMENTS** — 서버와 세그먼트를 연결합니다. `server`(servers 테이블 기본 키), `segment_id`(segments 테이블 기본 키) 두 컬럼으로 구성됩니다.

```sql
SELECT count(segments.segment_id) as num_segments from sys.segments as segments
INNER JOIN sys.server_segments as server_segments
ON segments.segment_id  = server_segments.segment_id
INNER JOIN sys.servers as servers
ON servers.server = server_segments.server
WHERE segments.datasource = 'wikipedia'
GROUP BY servers.server;
```

**TASKS** — 실행 중이거나 최근 완료된 인제스천 태스크 정보입니다. 주요 컬럼: `task_id`, `group_id`, `type`, `datasource`, `created_time`, `queue_insertion_time`, `status`(RUNNING/FAILED/SUCCESS), `runner_status`(완료 태스크는 NONE, 진행 중은 RUNNING/WAITING/PENDING), `duration`(완료 태스크의 소요 밀리초), `location`(실행 중인 host:port), `host`, `plaintext_port`, `tls_port`, `error_msg`(실패 태스크의 상세 오류).

```sql
SELECT * FROM sys.tasks WHERE status='FAILED';
```

**SUPERVISORS** — supervisor 정보입니다. 주요 컬럼: `supervisor_id`, `datasource`, `state`(UNHEALTHY_SUPERVISOR, UNHEALTHY_TASKS, PENDING, RUNNING, SUSPENDED, STOPPING), `detailed_state`, `healthy`(정상이면 1), `type`(kafka, kinesis, materialized_view), `source`(Kafka 토픽이나 Kinesis 스트림 등), `suspended`, `spec`(JSON 직렬화된 supervisor 스펙).

```sql
SELECT * FROM sys.supervisors WHERE healthy=0;
```

**SERVER_PROPERTIES** — 각 서버에 설정된 런타임 프로퍼티를 노출합니다. 컬럼: `server`(host:port), `service_name`(`druid.service` 값), `node_roles`(쉼표 구분 역할 목록), `property`, `value`.

```sql
SELECT * FROM sys.server_properties WHERE server='192.168.1.1:8081'
```

**QUERIES** — 실험적 기능으로, Broker에 `druid.sql.planner.enableSysQueriesTable=true` 설정이 필요하며 현재는 Dart 엔진 쿼리만 표시합니다. 컬럼: `id`(Dart의 dartQueryId), `engine`(예: msq-dart), `state`(ACCEPTED, RUNNING, SUCCESS, FAILED, CANCELED), `info`(sqlQueryId, sql, identity, startTime 등을 담은 JSON). 완료된 쿼리의 보존은 `druid.msq.dart.controller.maxRetainedReportCount`와 `druid.msq.dart.controller.maxRetainedReportDuration`이 결정합니다.

```sql
SELECT *
FROM sys.queries
WHERE  engine = 'msq-dart'
  AND state IN ('SUCCESS', 'FAILED', 'CANCELED')
```

---

## SQL → 네이티브 쿼리 변환

Druid SQL은 Broker에서 네이티브 쿼리로 변환된 뒤 실행됩니다.

### 모범 사례

1. `__time` 컬럼 필터가 네이티브 `"intervals"` 필터로 변환되는지 확인합니다. 그래야 성능이 좋습니다.
2. JOIN 안의 서브쿼리를 피합니다. 타입 불일치로 생기는 암묵적 서브쿼리를 포함해 성능과 확장성에 영향을 줍니다.
3. 조건절 위치에 주의합니다. Druid는 JOIN 너머로 조건을 push down하지 못하므로 comma join을 피합니다.
4. 네이티브 쿼리가 어떻게 실행되는지 Query execution 문서로 이해합니다.
5. EXPLAIN PLAN과 요청 로깅으로 실제 실행되는 네이티브 쿼리를 확인합니다.
6. 변환이 비효율적인 사례를 발견하면 재현 가능한 테스트 케이스와 함께 GitHub에 보고합니다.

### EXPLAIN PLAN 출력 해석

EXPLAIN PLAN은 세 개의 컬럼을 반환합니다.

- **PLAN**: Druid가 실행할 네이티브 쿼리의 JSON 배열
- **RESOURCES**: 사용하는 리소스 설명
- **ATTRIBUTES**: `statementType`, `targetDataSource`, `partitionedBy`, `clusteredBy`, `replaceTimeChunks` 등 쿼리 메타데이터

### 쿼리 타입 선택

Druid SQL은 네 가지 네이티브 쿼리 타입 중 하나를 자동으로 선택합니다.

| 쿼리 타입 | 사용 조건 |
|-----------|-----------|
| Scan | GROUP BY, DISTINCT가 없는 비집계 쿼리 |
| Timeseries | `FLOOR(__time TO unit)` 또는 `TIME_FLOOR(__time, period)`만으로 그룹핑하고, 다른 그룹핑·HAVING·중첩이 없는 쿼리 |
| TopN | 단일 컬럼 그룹핑에 ORDER BY와 LIMIT이 있는 쿼리. 결과가 근사일 수 있으며 `"useApproximateTopN": "false"`로 비활성화할 수 있음 |
| GroupBy | 그 밖의 모든 집계. 정확한 결과를 내며 디스크로 spill될 수 있음 |

### 시간 필터 변환

다음 패턴은 네이티브 `"intervals"` 필터로 변환됩니다.

```sql
__time >= TIMESTAMP '2000-01-01 00:00:00'                -- 절대 시간
__time >= CURRENT_TIMESTAMP - INTERVAL '8' HOUR          -- 상대 시간
FLOOR(__time TO DAY) = TIMESTAMP '2000-01-01 00:00:00'   -- 특정 일자
```

### JOIN 변환

1. **직접 변환**: 동등 조건의 lookup 또는 서브쿼리 조인은 네이티브 join 데이터소스로 그대로 변환됩니다.
2. **서브쿼리 삽입**: 직접 변환할 수 없는 조인(예: 우측에 표현식이 있는 경우)은 자동으로 서브쿼리로 감쌉니다.
3. **재배열 없음**: Druid SQL은 조인 순서를 최적화하지 않습니다.

### 서브쿼리 변환

서브쿼리는 일반적으로 네이티브 query 데이터소스로 변환됩니다. `WHERE col1 IN (SELECT foo FROM ...)` 형태의 WHERE 절 서브쿼리는 inner join으로 변환됩니다.

### 근사 계산

- **COUNT(DISTINCT)**: 기본적으로 HyperLogLog 근사를 사용합니다. `"useApproximateCountDistinct": "false"`로 정확한 계산으로 전환할 수 있습니다.
- **TopN**: 근사 알고리즘을 사용하며 `"useApproximateTopN": "false"`로 비활성화합니다.
- **스케치 함수**: `APPROX_QUANTILE_DS` 등은 항상 근사입니다. 한도에 걸리면 `approxQuantileDsMaxStreamLength`(기본값 1,000,000,000)를 조정합니다.

### 지원하지 않는 기능

SQL에서 지원하지 않는 기능은 다음과 같습니다.

- 시스템 테이블이 참여하는 JOIN
- 동등 비교가 아닌 JOIN 조건
- 상수가 들어간 JOIN 조건
- 다중값 디멘션 컬럼에 대한 JOIN
- 비집계 쿼리에서 `__time` 이외 컬럼의 ORDER BY
- DDL·DML 문
- 시스템 테이블에 대한 Druid 전용 함수

네이티브에는 있지만 SQL에서 사용할 수 없는 기능은 inline 데이터소스, 공간(spatial) 필터가 있으며, 다중값 디멘션은 부분적으로만 지원되고 알려진 비일관성이 있습니다.

---

## SQL API

### 쿼리 실행: POST /druid/v2/sql

JSON 또는 텍스트 형식으로 쿼리를 받아 결과를 반환합니다. JSON 요청 본문의 필드는 다음과 같습니다.

| 필드 | 설명 |
|------|------|
| `query` | SQL 쿼리 문자열. 컨텍스트 파라미터를 위한 여러 개의 SET 문을 포함할 수 있습니다 |
| `resultFormat` | 결과 형식: `object`, `array`, `objectLines`, `arrayLines`, `csv` |
| `header` | true면 첫 행에 컬럼 이름을 포함합니다 |
| `typesHeader` | Druid 런타임 타입 정보를 추가합니다(`header: true` 필요) |
| `sqlTypesHeader` | SQL 타입 정보를 추가합니다(`header: true` 필요) |
| `context` | SQL 쿼리 컨텍스트 파라미터 JSON 객체 |
| `parameters` | 파라미터화 쿼리의 type/value 객체 목록 |

텍스트 형식 요청 예시입니다.

```bash
echo 'SELECT 1' | curl -H 'Content-Type: text/plain' \
  http://ROUTER_IP:ROUTER_PORT/druid/v2/sql --data @-
```

### 결과 형식

| 형식 | Content-Type | 구조 |
|------|--------------|------|
| `object` | application/json | 필드 이름을 가진 객체들의 JSON 배열 |
| `array` | application/json | 배열들의 JSON 배열 |
| `objectLines` | text/plain | 줄바꿈으로 구분한 JSON 객체 |
| `arrayLines` | text/plain | 줄바꿈으로 구분한 JSON 배열 |
| `csv` | text/csv | 필드를 이스케이프한 쉼표 구분 값 |

### 응답 헤더

- `X-Druid-SQL-Query-Id`: 자동 생성되거나 직접 지정한 SQL 쿼리 식별자입니다. 쿼리 취소에 필요합니다.
- `X-Druid-SQL-Header-Included`: header, typesHeader, sqlTypesHeader 조건이 충족되면 `yes`를 반환합니다.

### 오류 처리와 응답 잘림

응답 전송 전에 발생한 오류는 HTTP 500과 함께 `error`, `errorMessage`, `errorClass`, `host` 필드를 담은 JSON으로 반환됩니다. 스트리밍 형식에서는 응답이 중간에 잘리면 마지막 줄바꿈 문자가 없으므로 이를 통해 잘림을 감지할 수 있습니다.

### 쿼리 취소: DELETE /druid/v2/sql/{sqlQueryId}

성공하면 HTTP 202를 반환합니다. 취소는 best-effort 방식이라 요청 후에도 쿼리가 잠시 계속 실행될 수 있습니다.

### 딥 스토리지(deep storage) 쿼리 (MSQ): POST /druid/v2/sql/statements

MSQ 태스크 엔진 기반의 비동기 쿼리 엔드포인트로, `executionMode: ASYNC`와 대용량 결과를 위한 `selectDestination: durableStorage`를 지원합니다. 부속 엔드포인트는 다음과 같습니다.

- `GET /druid/v2/sql/statements/{queryId}` — 상태 조회
- `GET /druid/v2/sql/statements/{queryId}/results` — 결과 조회
- 취소 엔드포인트도 별도로 제공합니다
