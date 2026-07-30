# Druid 쿼리: SQL과 네이티브 쿼리

## Druid SQL

> 원본: https://druid.apache.org/docs/latest/querying/sql
> 원본: https://druid.apache.org/docs/latest/querying/sql-data-types
> 원본: https://druid.apache.org/docs/latest/querying/sql-scalar
> 원본: https://druid.apache.org/docs/latest/querying/sql-aggregations
> 원본: https://druid.apache.org/docs/latest/querying/sql-metadata-tables
> 원본: https://druid.apache.org/docs/latest/querying/sql-translation
> 원본: https://druid.apache.org/docs/latest/api-reference/sql-api

Druid SQL의 문법, 데이터 타입, 스칼라·집계 함수, 메타데이터 테이블, 네이티브 쿼리 변환 과정, SQL API를 정리합니다.

---

### 목차

1. [Druid SQL 개요](#druid-sql-개요)
2. [SELECT 문법](#select-문법)
3. [데이터 타입](#데이터-타입)
4. [스칼라 함수](#스칼라-함수)
5. [집계 함수](#집계-함수)
6. [메타데이터 테이블](#메타데이터-테이블)
7. [SQL → 네이티브 쿼리 변환](#sql--네이티브-쿼리-변환)
8. [SQL API](#sql-api)

---

### Druid SQL 개요

Druid SQL은 Apache Calcite 기반의 파서와 플래너를 사용합니다. SQL 쿼리는 Broker에서 네이티브 쿼리로 변환된 뒤 데이터 서버에서 실행되므로, 네이티브 쿼리 대비 변환에 드는 약간의 오버헤드를 제외하면 성능 저하가 거의 없습니다.

#### 식별자와 리터럴

- **식별자(identifier)**: 큰따옴표로 선택적으로 감쌉니다. 식별자 내부의 큰따옴표는 두 번 써서 이스케이프합니다(`"My ""very own"" identifier"`). 식별자는 대소문자를 구분하며 암묵적 변환이 없습니다.
- **문자열 리터럴**: 작은따옴표를 사용합니다(`'foo'`). 유니코드 이스케이프는 `U&'fo\00F6'` 형태로 씁니다.
- **숫자 리터럴**: `100`(정수), `100.0`(실수), `1.0e5`(지수 표기)
- **타임스탬프 리터럴**: `TIMESTAMP '2000-01-01 00:00:00'`
- **인터벌 리터럴**: `INTERVAL '1' HOUR`, `INTERVAL '1 02:03' DAY TO MINUTE`, `INTERVAL '1-2' YEAR TO MONTH`

#### 예약어

Druid는 Apache Calcite의 예약어에 더해 `CLUSTERED`, `PARTITIONED`를 예약어로 사용합니다. 예약어를 식별자로 쓰려면 큰따옴표로 감쌉니다.

```sql
SELECT "PARTITIONED" from druid.table
```

#### 동적 파라미터

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

#### SET 문

쿼리 컨텍스트 파라미터를 쿼리 앞에 지정합니다. SET 문은 같은 요청 안의 쿼리에만 적용되며, SELECT·INSERT·REPLACE 쿼리에서 사용할 수 있습니다. 리터럴 값만 허용하므로 배열이나 JSON 객체는 API의 `context` 필드를 사용해야 하며, 둘 다 있으면 SET이 우선합니다.

```sql
SET useApproximateTopN = false;
SET sqlTimeZone = 'America/Los_Angeles';
SET timeout = 90000;
SELECT some_column, COUNT(*) FROM druid.foo WHERE other_column = 'foo' GROUP BY 1 ORDER BY 2 DESC
```

---

### SELECT 문법

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

#### FROM 절

FROM 절에서 참조할 수 있는 대상은 다음과 같습니다.

- `druid` 스키마의 테이블 데이터소스(기본 스키마이므로 접두사 생략 가능)
- `lookup` 스키마의 lookup(예: `lookup.countries`)
- 서브쿼리
- 호환되는 소스 간의 JOIN(조인 조건은 동등 비교여야 함)
- `INFORMATION_SCHEMA`, `sys` 스키마의 메타데이터 테이블

#### PIVOT

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

#### UNPIVOT

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

#### UNNEST

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

#### WHERE 절

FROM 테이블의 컬럼을 필터링하며 네이티브 필터로 변환됩니다. 문자열과 숫자는 암묵적 타입 변환으로 비교할 수 있지만, 성능을 위해 명시적으로 캐스팅하는 것이 좋습니다.

```sql
WHERE stringDim = '1'
```

문자열 디멘션(dimension)을 숫자 목록과 비교할 때는 문자열 목록으로 씁니다.

```sql
WHERE stringDim IN ('1', '2', '3')
```

`WHERE col1 IN (SELECT foo FROM ...)` 형태의 서브쿼리도 지원합니다.

#### GROUP BY 절

표현식뿐 아니라 서수 위치(예: `GROUP BY 2`)로도 지정할 수 있으며, 다음과 같은 다중 그룹핑을 지원합니다.

- `GROUP BY GROUPING SETS ( (country, city), () )`
- `GROUP BY ROLLUP (country, city)` — 여러 grouping set과 동등
- `GROUP BY CUBE (country, city)` — 모든 조합을 계산

특정 행에 적용되지 않는 그룹핑 컬럼은 `NULL`이 됩니다. 원래 데이터의 NULL과 구분하려면 `GROUPING` 집계를 사용합니다.

#### HAVING / ORDER BY

- HAVING 절은 GROUP BY 실행 후 결과를 필터링하며, 그룹핑 컬럼 또는 집계 값을 참조할 수 있습니다. GROUP BY가 있는 쿼리에서만 사용합니다.
- ORDER BY 절은 표현식 또는 서수 위치를 참조할 수 있습니다. 비집계 쿼리에서는 `__time`으로만 정렬할 수 있고, 집계 쿼리에서는 임의의 컬럼으로 정렬할 수 있습니다.

#### LIMIT / OFFSET

- LIMIT은 반환 행 수를 제한합니다. 상황에 따라 Druid가 limit을 데이터 서버로 push down하여 성능을 높이며, 네이티브 Scan·TopN 쿼리 타입으로 실행되는 쿼리에서는 항상 push down합니다.
- OFFSET은 지정한 수만큼 행을 건너뜁니다. LIMIT과 함께 쓰면 OFFSET을 먼저 적용한 뒤 LIMIT을 적용합니다. 예를 들어 `LIMIT 100 OFFSET 10`은 10번째 행부터 100개를 반환합니다.
- 건너뛴 행도 내부적으로 생성한 뒤 버리므로, OFFSET을 크게 잡으면 추가 리소스를 소모합니다. OFFSET은 네이티브 Scan과 GroupBy 쿼리 타입에서만 지원합니다.

#### UNION ALL

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

#### EXPLAIN PLAN

쿼리 앞에 `EXPLAIN PLAN FOR`를 붙이면 실제로 실행하지 않고 네이티브 쿼리 변환 결과를 확인할 수 있습니다.

```sql
EXPLAIN PLAN FOR SELECT ...
```

---

### 데이터 타입

Druid가 네이티브로 지원하는 기본 컬럼 타입은 다음과 같습니다.

- `LONG`: 64비트 부호 있는 정수
- `FLOAT`: 32비트 부동소수점
- `DOUBLE`: 64비트 부동소수점
- `STRING`: UTF-8 문자열과 문자열 배열
- `COMPLEX`: 중첩 JSON, hyperUnique, approxHistogram, DataSketches 등 비표준 타입
- `ARRAY`: 위 타입들로 구성된 배열

#### 표준 타입 매핑

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

#### 타임스탬프 처리

Druid는 `__time` 컬럼을 포함한 타임스탬프를 LONG으로 다루며, 값은 1970-01-01 00:00:00 UTC 이후의 밀리초 수(윤초 제외)입니다. 따라서 Druid의 타임스탬프는 시간대 정보를 담지 않습니다.

#### 캐스팅 규칙

- Druid 런타임 타입이 같은 SQL 타입 간 캐스팅은 효과가 없습니다(명시된 예외 제외).
- 런타임 타입이 다르면 Druid에서 런타임 캐스팅을 수행합니다.
- 캐스팅에 실패하면 NULL로 대체합니다(예: `CAST('foo' AS BIGINT)`).

#### 배열

Druid의 `ARRAY` 타입은 표준 SQL 배열처럼 동작하며, 그룹핑 시 배열 전체가 일치하는 값끼리 묶입니다. `UNNEST` 연산자로 배열 원소를 개별 행으로 펼쳐 원소 단위 연산을 수행할 수 있습니다. SQL 기반 인제스천에서 배열을 적재하려면 `arrayIngestMode` 파라미터를 `"array"`로 설정해야 합니다. 결과는 기본적으로 JSON 문자열로 직렬화되며, `sqlStringifyArrays` 컨텍스트 파라미터로 제어합니다.

#### 다중값 문자열

Druid 네이티브 타입 시스템에서는 문자열이 여러 값을 가질 수 있습니다. 다중값 문자열 디멘션은 SQL에서 VARCHAR 타입으로 표시되며 문법상 일반 VARCHAR처럼 사용할 수 있습니다. 표준 VARCHAR 함수는 행마다 모든 값에 적용됩니다. `MV_` 접두사가 붙은 다중값 전용 함수는 값을 배열처럼 처리하면서 VARCHAR 타입을 유지합니다. 그룹핑을 적용하면 암묵적 UNNEST가 일어나 단일값 VARCHAR 결과를 만듭니다.

#### NULL과 불리언 논리

- Druid는 기본적으로 NULL 값을 ANSI SQL 표준과 유사하게 처리합니다.
- 필터 처리와 불리언 표현식 평가에는 SQL 3치 논리(three-valued logic)를 사용합니다.

#### 중첩 컬럼

Druid는 네이티브 `COMPLEX<json>` 타입으로 세그먼트에 중첩 데이터 구조를 저장할 수 있습니다. 중첩 데이터는 JSON 함수로 추출·파싱·직렬화하거나 새 구조를 만들 수 있습니다. COMPLEX 타입은 전용 처리 없이 그룹핑·직접 필터링·집계에 사용하면 동작이 정의되지 않습니다.

---

### 스칼라 함수

대표적인 함수를 카테고리별로 정리합니다. 전체 목록은 원본 문서를 참고합니다.

#### 숫자 함수

| 함수 | 설명 |
|------|------|
| `ABS(expr)` | 절댓값을 반환합니다 |
| `ROUND(expr[, digits])` | 지정한 소수 자릿수로 반올림합니다. digits가 음수면 소수점 왼쪽 자리에서 반올림합니다 |
| `POWER(expr, power)` | 거듭제곱을 계산합니다 |
| `SQRT(expr)` | 제곱근을 반환합니다 |
| `MOD(x, y)` | x를 y로 나눈 나머지를 반환합니다 |
| `BITWISE_AND(expr1, expr2)` | 비트 AND 연산을 수행합니다 |
| `SAFE_DIVIDE(x, y)` | 0으로 나누면 오류 대신 null을 반환하는 나눗셈입니다 |

#### 문자열 함수

| 함수 | 설명 |
|------|------|
| `CONCAT(expr[, expr, ...])` | 표현식 목록을 이어 붙입니다 |
| `LENGTH(expr)` | UTF-16 코드 단위 기준 문자열 길이를 반환합니다 |
| `UPPER(expr)` | 문자열을 모두 대문자로 변환합니다 |
| `SUBSTRING(expr, index[, length])` | 1부터 시작하는 index 위치에서 부분 문자열을 추출합니다 |
| `REGEXP_EXTRACT(expr, pattern[, index])` | 정규식 패턴을 적용해 매칭 그룹을 추출합니다 |
| `REPLACE(expr, substring, replacement)` | 모든 substring 출현을 치환합니다 |
| `TRIM([direction] [chars FROM] expr)` | 문자열 양끝에서 지정 문자를 제거합니다 |

#### 날짜·시간 함수

| 함수 | 설명 |
|------|------|
| `CURRENT_TIMESTAMP` | 현재 타임스탬프를 UTC 기준으로 반환합니다(다른 시간대를 지정하지 않는 한) |
| `DATE_TRUNC(unit, timestamp_expr)` | 타임스탬프를 내림해 새 타임스탬프로 반환합니다 |
| `TIME_FLOOR(timestamp_expr, period[, origin[, timezone]])` | ISO 8601 period 기준으로 내림합니다 |
| `TIME_EXTRACT(timestamp_expr, unit[, timezone])` | 시간 구성 요소를 숫자로 추출합니다 |
| `TIME_PARSE(string_expr[, pattern[, timezone]])` | 패턴으로 문자열을 파싱해 타임스탬프로 변환합니다 |
| `TIMESTAMPDIFF(unit, timestamp1, timestamp2)` | 두 타임스탬프 간 부호 있는 시간 차이를 반환합니다 |

#### 축약(reduction) 함수

| 함수 | 설명 |
|------|------|
| `GREATEST([expr1, ...])` | 0개 이상의 표현식을 평가해 최댓값을 반환합니다 |
| `LEAST([expr1, ...])` | 0개 이상의 표현식을 평가해 최솟값을 반환합니다 |

#### IP 주소 함수

| 함수 | 설명 |
|------|------|
| `IPV4_MATCH(address, subnet)` | 주소가 subnet 리터럴에 속하면 true, 아니면 false를 반환합니다 |
| `IPV4_PARSE(address)` | 주소를 정수로 저장되는 IPv4 주소로 파싱합니다 |
| `IPV6_MATCH(address, subnet)` | IPv6 주소가 subnet에 속하면 1, 아니면 0을 반환합니다 |

#### 스케치 함수 (DataSketches)

| 함수 | 설명 |
|------|------|
| `HLL_SKETCH_ESTIMATE(expr[, round])` | HLL 스케치에서 고유 개수 추정치를 반환합니다 |
| `THETA_SKETCH_ESTIMATE(expr)` | Theta 스케치에서 고유 개수 추정치를 반환합니다 |
| `DS_GET_QUANTILE(expr, fraction)` | quantiles 스케치에서 분위수 추정치를 반환합니다 |

#### 기타 함수

| 함수 | 설명 |
|------|------|
| `CASE expr WHEN value1 THEN result1 [...] END` | 단순 CASE 표현식입니다 |
| `CASE WHEN boolean_expr1 THEN result1 [...] END` | 조건 검색 CASE 표현식입니다 |
| `CAST(value AS TYPE)` | 타입을 변환합니다 |
| `COALESCE(value1, value2, ...)` | 첫 번째 non-null 값을 반환합니다 |
| `NULLIF(value1, value2)` | 두 값이 같으면 NULL, 다르면 value1을 반환합니다 |

---

### 집계 함수

#### 공통 동작

- **FILTER 절**: `FILTER(WHERE condition)`으로 조건에 맞는 행만 집계할 수 있습니다. Druid는 이를 네이티브 filtered aggregator로 변환하므로, 한 쿼리 안에서 집계마다 다른 필터를 적용할 수 있습니다.
- **NULL 처리**: 선택된 행이 없으면 집계 함수는 초기값을 반환합니다. 필터가 모든 행을 제외하거나 그룹 집계에서 매칭이 없을 때 발생합니다.
- **DISTINCT**: `COUNT`, `ARRAY_AGG`, `STRING_AGG`만 DISTINCT 키워드를 허용합니다.
- **비결정적 순서**: 세그먼트 간 집계 연산 순서는 결정적이지 않으므로, 교환법칙이 성립하지 않는 집계 함수는 같은 쿼리에서도 결과가 달라질 수 있습니다. float/double 타입 연산에서 변동이 나타날 수 있으며, `ROUND` 함수로 완화할 수 있습니다.

#### 기본 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `COUNT(*)` | 행 수를 셉니다 | `0` |
| `COUNT([DISTINCT] expr)` | 값 개수를 셉니다. DISTINCT는 `useApproximateCountDistinct=false`가 아닌 한 근사 계산을 사용합니다 | `0` |
| `SUM(expr)` | 숫자 값의 합을 구합니다 | `null` |
| `MIN(expr)` | 최솟값을 구합니다 | `null` |
| `MAX(expr)` | 최댓값을 구합니다 | `null` |
| `AVG(expr)` | 평균을 구합니다 | `null` |
| `ANY_VALUE(expr, [maxBytesPerValue, [aggregateMultipleValues]])` | null을 포함해 만나는 임의의 값을 반환하며 조기 반환에 최적화되어 있습니다 | `null` |

#### 근사 고유 개수(approximate distinct count)

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `APPROX_COUNT_DISTINCT(expr)` | `druid.sql.approxCountDistinct.function`에 지정된 알고리즘으로 근사 고유 개수를 계산합니다 | `0` |
| `APPROX_COUNT_DISTINCT_BUILTIN(expr)` | Druid 내장 HyperLogLog 변형입니다. 문자열, 숫자, 사전 빌드된 hyperUnique 컬럼에 사용합니다 | `0` |
| `APPROX_COUNT_DISTINCT_DS_HLL(expr, [lgK, tgtHllType])` | DataSketches HLL 구현으로 일반적으로 더 나은 정확도를 제공합니다 | `0` |
| `APPROX_COUNT_DISTINCT_DS_THETA(expr, [size])` | DataSketches Theta 스케치를 사용합니다 | `0` |

#### 분위수(quantile)

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `APPROX_QUANTILE(expr, probability, [resolution])` | **Deprecated.** approximate histogram을 사용합니다. probability는 0과 1 사이(경계 제외)입니다 | `NaN` |
| `APPROX_QUANTILE_FIXED_BUCKETS(expr, probability, numBuckets, lowerLimit, upperLimit, [outlierHandlingMode])` | 고정 버킷 히스토그램 기반 분위수입니다. approximate histogram 익스텐션이 필요합니다 | `0.0` |
| `APPROX_QUANTILE_DS(expr, probability, [k])` | DataSketches quantiles 스케치를 사용하며, 분포에 무관하게 동작하는 더 우수한 알고리즘입니다 | `NaN` |

#### 시간 기준 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `EARLIEST(expr, [maxBytesPerValue])` | 가장 이른 non-null 타임스탬프를 가진 행의 값을 반환합니다 | `null` |
| `EARLIEST_BY(expr, timestampExpr, [maxBytesPerValue])` | 지정한 타임스탬프 기준으로 가장 이른 값을 반환합니다. rollup이 활성화된 테이블에서는 지정한 타임스탬프를 무시합니다 | `null` |
| `LATEST(expr, [maxBytesPerValue])` | 가장 늦은 non-null 타임스탬프를 가진 행의 값을 반환합니다 | `null` |
| `LATEST_BY(expr, timestampExpr, [maxBytesPerValue])` | 지정한 타임스탬프 기준으로 가장 늦은 값을 반환합니다. rollup이 활성화된 테이블에서는 지정한 타임스탬프를 무시합니다 | `null` |

#### 배열·문자열 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `ARRAY_AGG([DISTINCT] expr, [size])` | 값을 배열로 모읍니다. size 기본값은 1024바이트이며 ORDER BY는 지원하지 않습니다 | `null` |
| `ARRAY_CONCAT_AGG([DISTINCT] expr, [size])` | 배열 입력을 이어 붙입니다. null 배열은 무시하지만 null 원소는 포함합니다 | `null` |
| `STRING_AGG([DISTINCT] expr, [separator, [size]])` | 값을 구분자로 연결한 문자열을 만듭니다. null을 무시하며 size 기본값은 1024바이트입니다 | `null` |
| `LISTAGG([DISTINCT] expr, [separator, [size]])` | STRING_AGG의 동의어입니다 | `null` |

#### 통계 함수

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `VAR_POP(expr)` | 모분산 | `null` |
| `VAR_SAMP(expr)` | 표본분산 | `null` |
| `VARIANCE(expr)` | 표본분산(별칭) | `null` |
| `STDDEV_POP(expr)` | 모표준편차 | `null` |
| `STDDEV_SAMP(expr)` | 표본표준편차 | `null` |
| `STDDEV(expr)` | 표본표준편차(별칭) | `null` |

#### 비트 연산 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `BIT_AND(expr)` | 모든 입력에 걸친 비트 AND | `null` |
| `BIT_OR(expr)` | 모든 입력에 걸친 비트 OR | `null` |
| `BIT_XOR(expr)` | 모든 입력에 걸친 비트 XOR | `null` |

#### 스케치 생성 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `DS_HLL(expr, [lgK, tgtHllType])` | HLL 스케치를 생성합니다(DataSketches 익스텐션) | `'0'` (STRING) |
| `DS_THETA(expr, [size])` | Theta 스케치를 생성합니다(DataSketches 익스텐션) | `'0.0'` (STRING) |
| `DS_QUANTILES_SKETCH(expr, [k])` | quantiles 스케치를 생성합니다(DataSketches 익스텐션) | `'0'` (STRING) |
| `DS_TUPLE_DOUBLES(expr [, nominalEntries])` | double 배열을 담는 Tuple 스케치를 생성합니다(DataSketches 익스텐션) | 없음 |
| `TDIGEST_QUANTILE(expr, quantileFraction, [compression])` | T-Digest 분위수입니다. compression 기본값은 100입니다 | `Double.NaN` |
| `TDIGEST_GENERATE_SKETCH(expr, [compression])` | T-Digest 스케치를 생성합니다 | 빈 스케치(STRING) |

#### 기타 특수 집계

| 함수 | 설명 | 기본값 |
|------|------|--------|
| `GROUPING(expr, expr...)` | GROUPING SETS에서 어떤 디멘션이 포함되었는지를 숫자로 나타냅니다 | 없음 |
| `BLOOM_FILTER(expr, numEntries)` | 지정한 최대 고유 값 수에 대한 bloom filter를 생성합니다 | 빈 base64 STRING |
| `SPECTATOR_COUNT(expr)` | Spectator 히스토그램의 관측 개수를 셉니다 | `0` |
| `SPECTATOR_PERCENTILE(expr, percentile)` | Spectator 히스토그램에서 근사 백분위수(0–100)를 구합니다 | `NaN` |

---

### 메타데이터 테이블

INFORMATION_SCHEMA 테이블과 sys 테이블은 `TIME_PARSE`, `APPROX_QUANTILE_DS` 같은 Druid 전용 함수를 지원하지 않으며 표준 SQL 함수만 사용할 수 있습니다.

#### INFORMATION_SCHEMA

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

#### sys 스키마

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

### SQL → 네이티브 쿼리 변환

Druid SQL은 Broker에서 네이티브 쿼리로 변환된 뒤 실행됩니다.

#### 모범 사례

1. `__time` 컬럼 필터가 네이티브 `"intervals"` 필터로 변환되는지 확인합니다. 그래야 성능이 좋습니다.
2. JOIN 안의 서브쿼리를 피합니다. 타입 불일치로 생기는 암묵적 서브쿼리를 포함해 성능과 확장성에 영향을 줍니다.
3. 조건절 위치에 주의합니다. Druid는 JOIN 너머로 조건을 push down하지 못하므로 comma join을 피합니다.
4. 네이티브 쿼리가 어떻게 실행되는지 Query execution 문서로 이해합니다.
5. EXPLAIN PLAN과 요청 로깅으로 실제 실행되는 네이티브 쿼리를 확인합니다.
6. 변환이 비효율적인 사례를 발견하면 재현 가능한 테스트 케이스와 함께 GitHub에 보고합니다.

#### EXPLAIN PLAN 출력 해석

EXPLAIN PLAN은 세 개의 컬럼을 반환합니다.

- **PLAN**: Druid가 실행할 네이티브 쿼리의 JSON 배열
- **RESOURCES**: 사용하는 리소스 설명
- **ATTRIBUTES**: `statementType`, `targetDataSource`, `partitionedBy`, `clusteredBy`, `replaceTimeChunks` 등 쿼리 메타데이터

#### 쿼리 타입 선택

Druid SQL은 네 가지 네이티브 쿼리 타입 중 하나를 자동으로 선택합니다.

| 쿼리 타입 | 사용 조건 |
|-----------|-----------|
| Scan | GROUP BY, DISTINCT가 없는 비집계 쿼리 |
| Timeseries | `FLOOR(__time TO unit)` 또는 `TIME_FLOOR(__time, period)`만으로 그룹핑하고, 다른 그룹핑·HAVING·중첩이 없는 쿼리 |
| TopN | 단일 컬럼 그룹핑에 ORDER BY와 LIMIT이 있는 쿼리. 결과가 근사일 수 있으며 `"useApproximateTopN": "false"`로 비활성화할 수 있음 |
| GroupBy | 그 밖의 모든 집계. 정확한 결과를 내며 디스크로 spill될 수 있음 |

#### 시간 필터 변환

다음 패턴은 네이티브 `"intervals"` 필터로 변환됩니다.

```sql
__time >= TIMESTAMP '2000-01-01 00:00:00'                -- 절대 시간
__time >= CURRENT_TIMESTAMP - INTERVAL '8' HOUR          -- 상대 시간
FLOOR(__time TO DAY) = TIMESTAMP '2000-01-01 00:00:00'   -- 특정 일자
```

#### JOIN 변환

1. **직접 변환**: 동등 조건의 lookup 또는 서브쿼리 조인은 네이티브 join 데이터소스로 그대로 변환됩니다.
2. **서브쿼리 삽입**: 직접 변환할 수 없는 조인(예: 우측에 표현식이 있는 경우)은 자동으로 서브쿼리로 감쌉니다.
3. **재배열 없음**: Druid SQL은 조인 순서를 최적화하지 않습니다.

#### 서브쿼리 변환

서브쿼리는 일반적으로 네이티브 query 데이터소스로 변환됩니다. `WHERE col1 IN (SELECT foo FROM ...)` 형태의 WHERE 절 서브쿼리는 inner join으로 변환됩니다.

#### 근사 계산

- **COUNT(DISTINCT)**: 기본적으로 HyperLogLog 근사를 사용합니다. `"useApproximateCountDistinct": "false"`로 정확한 계산으로 전환할 수 있습니다.
- **TopN**: 근사 알고리즘을 사용하며 `"useApproximateTopN": "false"`로 비활성화합니다.
- **스케치 함수**: `APPROX_QUANTILE_DS` 등은 항상 근사입니다. 한도에 걸리면 `approxQuantileDsMaxStreamLength`(기본값 1,000,000,000)를 조정합니다.

#### 지원하지 않는 기능

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

### SQL API

#### 쿼리 실행: POST /druid/v2/sql

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

#### 결과 형식

| 형식 | Content-Type | 구조 |
|------|--------------|------|
| `object` | application/json | 필드 이름을 가진 객체들의 JSON 배열 |
| `array` | application/json | 배열들의 JSON 배열 |
| `objectLines` | text/plain | 줄바꿈으로 구분한 JSON 객체 |
| `arrayLines` | text/plain | 줄바꿈으로 구분한 JSON 배열 |
| `csv` | text/csv | 필드를 이스케이프한 쉼표 구분 값 |

#### 응답 헤더

- `X-Druid-SQL-Query-Id`: 자동 생성되거나 직접 지정한 SQL 쿼리 식별자입니다. 쿼리 취소에 필요합니다.
- `X-Druid-SQL-Header-Included`: header, typesHeader, sqlTypesHeader 조건이 충족되면 `yes`를 반환합니다.

#### 오류 처리와 응답 잘림

응답 전송 전에 발생한 오류는 HTTP 500과 함께 `error`, `errorMessage`, `errorClass`, `host` 필드를 담은 JSON으로 반환됩니다. 스트리밍 형식에서는 응답이 중간에 잘리면 마지막 줄바꿈 문자가 없으므로 이를 통해 잘림을 감지할 수 있습니다.

#### 쿼리 취소: DELETE /druid/v2/sql/{sqlQueryId}

성공하면 HTTP 202를 반환합니다. 취소는 best-effort 방식이라 요청 후에도 쿼리가 잠시 계속 실행될 수 있습니다.

#### 딥 스토리지(deep storage) 쿼리 (MSQ): POST /druid/v2/sql/statements

MSQ 태스크 엔진 기반의 비동기 쿼리 엔드포인트로, `executionMode: ASYNC`와 대용량 결과를 위한 `selectDestination: durableStorage`를 지원합니다. 부속 엔드포인트는 다음과 같습니다.

- `GET /druid/v2/sql/statements/{queryId}` — 상태 조회
- `GET /druid/v2/sql/statements/{queryId}/results` — 결과 조회
- 취소 엔드포인트도 별도로 제공합니다

---

## Druid 네이티브 쿼리

> 원본: https://druid.apache.org/docs/latest/querying/
> 원본: https://druid.apache.org/docs/latest/querying/timeseriesquery
> 원본: https://druid.apache.org/docs/latest/querying/topnquery
> 원본: https://druid.apache.org/docs/latest/querying/groupbyquery
> 원본: https://druid.apache.org/docs/latest/querying/scan-query
> 원본: https://druid.apache.org/docs/latest/querying/searchquery
> 원본: https://druid.apache.org/docs/latest/querying/filters
> 원본: https://druid.apache.org/docs/latest/querying/aggregations
> 원본: https://druid.apache.org/docs/latest/querying/granularities
> 원본: https://druid.apache.org/docs/latest/querying/query-context

JSON 기반 네이티브 쿼리의 제출 방법과 주요 쿼리 타입(timeseries, topN, groupBy, scan, search), 그리고 쿼리를 구성하는 필터·집계·그래뉼래리티·쿼리 컨텍스트를 정리합니다.

---

### 목차

1. [네이티브 쿼리 개요](#네이티브-쿼리-개요)
2. [Timeseries 쿼리](#timeseries-쿼리)
3. [TopN 쿼리](#topn-쿼리)
4. [GroupBy 쿼리](#groupby-쿼리)
5. [Scan 쿼리](#scan-쿼리)
6. [Search 쿼리](#search-쿼리)
7. [필터](#필터)
8. [집계](#집계)
9. [그래뉼래리티](#그래뉼래리티)
10. [쿼리 컨텍스트](#쿼리-컨텍스트)

---

### 네이티브 쿼리 개요

Druid의 네이티브 쿼리는 JSON 객체로 표현하며, Broker 또는 Router 프로세스에 제출합니다. 네이티브 쿼리는 비교적 저수준 API로, Druid 내부에서 연산이 수행되는 방식과 밀접하게 대응합니다. Druid는 가볍고 빠른 쿼리에 맞게 설계되었으므로, 복잡한 분석은 여러 쿼리를 순차적으로 조합해야 할 때가 많습니다.

#### 쿼리 제출

네이티브 쿼리는 다음 HTTP 엔드포인트로 POST합니다.

```
POST <queryable_host>:<port>/druid/v2/?pretty
```

curl 예시는 다음과 같습니다.

```bash
curl -X POST '<queryable_host>:<port>/druid/v2/?pretty' \
  -H 'Content-Type:application/json' \
  -H 'Accept:application/json' \
  -d @<query_json_file>
```

- `Content-Type`/`Accept` 헤더로 `application/x-jackson-smile`도 사용할 수 있습니다. `Accept` 헤더를 생략하면 `Content-Type` 값을 그대로 따릅니다.
- 퀵스타트 구성이라면 호스트는 `localhost:8888`을 사용합니다.
- 웹 콘솔의 Query 뷰에 JSON을 붙여넣으면 에디터가 JSON 모드로 전환되어 네이티브 쿼리를 직접 실행할 수 있습니다.

#### 쿼리 타입

| 분류 | 쿼리 타입 |
| --- | --- |
| 집계(aggregation) 쿼리 | Timeseries, TopN, GroupBy |
| 메타데이터(metadata) 쿼리 | TimeBoundary, SegmentMetadata, DatasourceMetadata |
| 기타 쿼리 | Scan, Search |

여러 쿼리 타입이 모두 요구 사항을 충족한다면, 각 용도에 맞게 최적화된 Timeseries나 TopN을 우선 사용하기를 권장합니다. 둘 다 적합하지 않을 때는 가장 유연한 GroupBy 쿼리를 사용합니다.

#### 쿼리 취소

쿼리 제출 시 지정한 `queryId`로 실행 중인 쿼리를 취소할 수 있습니다.

```
DELETE /druid/v2/{queryId}
```

```bash
curl -X DELETE "http://host:port/druid/v2/abc123"
```

#### 오류 응답

쿼리가 실패하면 다음 구조의 JSON을 반환합니다.

```json
{
  "error": "Query timeout",
  "errorMessage": "Timeout waiting for task.",
  "errorClass": "java.util.concurrent.TimeoutException",
  "host": "druid1.example.com:8083"
}
```

| 오류 코드 | HTTP 코드 | 설명 |
| --- | --- | --- |
| SQL parse failed | 400 | SQL 쿼리 파싱 실패 |
| Plan validation failed | 400 | SQL 쿼리 검증 실패 |
| Resource limit exceeded | 400 | 설정된 한도 초과(예: groupBy의 maxResults) |
| Query capacity exceeded | 429 | 실행 시점의 자원 부족 |
| Unsupported operation | 501 | 지원하지 않는 연산 시도 |
| Query timeout | 504 | 쿼리 실행 시간 한도 초과 |
| Query interrupted | 500 | 쿼리 중단(JVM 종료 등) |
| Query cancelled | 500 | 취소 API로 쿼리 취소됨 |
| Truncated response context | 500 | 중간 response context가 7KiB 한도를 초과해 잘림 |
| Unknown exception | 500 | 그 밖의 예외. `errorMessage`와 `errorClass`를 확인 |

보안이 적용된 클러스터에서 인증 실패는 HTTP 401, 인가 실패는 HTTP 403을 반환합니다.

---

### Timeseries 쿼리

Timeseries 쿼리는 지정한 기간을 그래뉼래리티 단위로 버킷팅해 시계열 형태의 집계 결과를 반환합니다.

```json
{
  "queryType": "timeseries",
  "dataSource": "sample_datasource",
  "granularity": "day",
  "descending": "true",
  "filter": {
    "type": "and",
    "fields": [
      { "type": "selector", "dimension": "sample_dimension1", "value": "sample_value1" },
      { "type": "or",
        "fields": [
          { "type": "selector", "dimension": "sample_dimension2", "value": "sample_value2" },
          { "type": "selector", "dimension": "sample_dimension3", "value": "sample_value3" }
        ]
      }
    ]
  },
  "aggregations": [
    { "type": "longSum", "name": "sample_name1", "fieldName": "sample_fieldName1" },
    { "type": "doubleSum", "name": "sample_name2", "fieldName": "sample_fieldName2" }
  ],
  "postAggregations": [
    { "type": "arithmetic",
      "name": "sample_divide",
      "fn": "/",
      "fields": [
        { "type": "fieldAccess", "name": "postAgg__sample_name1", "fieldName": "sample_name1" },
        { "type": "fieldAccess", "name": "postAgg__sample_name2", "fieldName": "sample_name2" }
      ]
    }
  ],
  "intervals": [ "2012-01-01T00:00:00.000/2012-01-03T00:00:00.000" ]
}
```

#### 속성

| 속성 | 설명 | 필수 여부 |
| --- | --- | --- |
| `queryType` | 항상 `"timeseries"` | 예 |
| `dataSource` | 조회 대상 데이터소스. 관계형 데이터베이스의 테이블에 해당 | 예 |
| `descending` | 정렬 방향. 기본값은 `false`(오름차순) | 아니요 |
| `intervals` | 쿼리 대상 시간 범위를 나타내는 ISO-8601 interval 목록 | 예 |
| `granularity` | 결과를 버킷팅할 그래뉼래리티 | 예 |
| `filter` | 필터 | 아니요 |
| `virtualColumns` | 집계·후처리 집계에서 참조할 수 있는 가상 컬럼 목록 | 아니요 |
| `aggregations` | 집계 목록 | 아니요 |
| `postAggregations` | 후처리 집계(post-aggregation) 목록 | 아니요 |
| `limit` | 반환 결과 수 제한 정수. 기본은 무제한 | 아니요 |
| `context` | 쿼리 컨텍스트 | 아니요 |

#### Grand total

쿼리 컨텍스트에 `"grandTotal": true`를 추가하면 전체 합계 행을 함께 반환합니다. Grand total 행은 결과의 마지막에 타임스탬프 없이 추가되며, 후처리 집계도 grand total 값을 기준으로 계산됩니다.

#### 빈 버킷 처리

Druid는 기본적으로 결과 내부의 빈 시간 버킷을 해당 집계 함수의 기본값으로 채웁니다(zero-filling). 예를 들어 `longSum`이라면 0으로 채웁니다. 빈 버킷을 결과에서 제외하려면 쿼리 컨텍스트에 `"skipEmptyBuckets": "true"`를 지정합니다.

---

### TopN 쿼리

TopN 쿼리는 단일 디멘션을 기준으로 지정한 metric에 따라 정렬한 상위 N개 결과를 반환합니다. 같은 용도라면 GroupBy 쿼리보다 훨씬 빠르고 자원을 적게 사용합니다.

```json
{
  "queryType": "topN",
  "dataSource": "sample_data",
  "dimension": "sample_dim",
  "threshold": 5,
  "metric": "count",
  "granularity": "all",
  "filter": {
    "type": "and",
    "fields": [
      { "type": "selector", "dimension": "dim1", "value": "some_value" },
      { "type": "selector", "dimension": "dim2", "value": "some_other_val" }
    ]
  },
  "aggregations": [
    { "type": "longSum", "name": "count", "fieldName": "count" },
    { "type": "doubleSum", "name": "some_metric", "fieldName": "some_metric" }
  ],
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "average",
      "fn": "/",
      "fields": [
        { "type": "fieldAccess", "name": "some_metric", "fieldName": "some_metric" },
        { "type": "fieldAccess", "name": "count", "fieldName": "count" }
      ]
    }
  ],
  "intervals": [ "2013-08-31T00:00:00.000/2013-09-03T00:00:00.000" ]
}
```

#### 속성

| 속성 | 설명 | 필수 여부 |
| --- | --- | --- |
| `queryType` | 항상 `"topN"`. Druid가 쿼리 해석 방식을 결정할 때 가장 먼저 확인하는 값 | 예 |
| `dataSource` | 조회 대상 데이터소스 | 예 |
| `intervals` | ISO-8601 interval 목록 | 예 |
| `granularity` | 결과 버킷팅 그래뉼래리티 | 예 |
| `filter` | 필터 | 아니요 |
| `virtualColumns` | 가상 컬럼 목록. 그룹핑 디멘션이나 집계·후처리 집계 입력으로 참조 가능 | 아니요(기본값 없음) |
| `aggregations` | 집계 목록 | 숫자형 metricSpec이면 `aggregations` 또는 `postAggregations` 중 하나 필수 |
| `postAggregations` | 후처리 집계 목록 | 숫자형 metricSpec이면 `aggregations` 또는 `postAggregations` 중 하나 필수 |
| `dimension` | 상위 목록을 뽑을 디멘션을 지정하는 문자열 또는 JSON 객체(DimensionSpec) | 예 |
| `threshold` | topN의 N에 해당하는 정수(상위 몇 개를 반환할지) | 예 |
| `metric` | 정렬 기준 metric을 지정하는 문자열 또는 JSON 객체(TopNMetricSpec) | 예 |
| `context` | 쿼리 컨텍스트 | 아니요 |

#### 응답 예시

```json
[
  {
    "timestamp": "2013-08-31T00:00:00.000Z",
    "result": [
      { "dim1": "dim1_val",         "count": 111, "some_metrics": 10669, "average": 96.11711711711712 },
      { "dim1": "another_dim1_val", "count": 88,  "some_metrics": 28344, "average": 322.09090909090907 },
      { "dim1": "dim1_val3",        "count": 70,  "some_metrics": 871,   "average": 12.442857142857143 },
      { "dim1": "dim1_val4",        "count": 62,  "some_metrics": 815,   "average": 13.14516129032258 },
      { "dim1": "dim1_val5",        "count": 60,  "some_metrics": 2787,  "average": 46.45 }
    ]
  }
]
```

#### 다중 값 디멘션에서의 동작

TopN 쿼리는 다중 값(multi-value) 디멘션 기준 그룹핑을 지원합니다. 다중 값 디멘션으로 그룹핑하면 매칭된 행의 모든 값이 각각 하나의 그룹을 만들기 때문에, 결과 그룹 수가 입력 행 수보다 많아질 수 있습니다. 필터 조건에 매칭되는 값만 남기고 성능도 개선하려면 filtered dimensionSpec을 적용합니다.

#### 근사(approximation) 동작과 정확도

TopN 알고리즘은 본질적으로 근사 알고리즘입니다. 각 세그먼트에서 상위 1000개(기본값, 정확히는 `max(1000, threshold)`)의 로컬 결과를 반환해 병합한 뒤 전역 topN을 결정합니다. 이 한도는 쿼리 컨텍스트의 `minTopNThreshold`로 쿼리별로 조정할 수 있습니다.

- 디멘션의 고유 값이 1000개 이하이면 순위와 집계 값 모두 정확합니다. 정확도 문제는 고유 값이 1000개를 초과할 때만 발생합니다.
- 상위권 분포가 고른 경우에 잘 맞습니다. 어떤 값이 시간별로는 겨우 상위 1000위 안에 들지만 전체적으로는 상위 500위라면, 순위가 부정확하거나 집계가 불완전할 수 있습니다.
- 고유 값이 1000개를 넘는 디멘션에서 정확한 순위와 정확한 집계가 필요하면 groupBy 쿼리를 실행하고 결과를 직접 정렬해야 합니다.
- 근사 순위 + 정확한 집계가 필요하면 쿼리를 두 번 실행합니다. 첫 번째 쿼리로 근사 topN 값을 구하고, 두 번째 쿼리에서 그 디멘션 값들로 필터링해 다시 조회합니다.

첫 번째 쿼리(근사 순위) 예시입니다.

```json
{
  "aggregations": [
    { "fieldName": "L_QUANTITY_longSum", "name": "L_QUANTITY_", "type": "longSum" }
  ],
  "dataSource": "tpch_year",
  "dimension": "l_orderkey",
  "granularity": "all",
  "intervals": [ "1900-01-09T00:00:00.000Z/2992-01-10T00:00:00.000Z" ],
  "metric": "L_QUANTITY_",
  "queryType": "topN",
  "threshold": 2
}
```

두 번째 쿼리(정확한 집계) 예시입니다.

```json
{
  "aggregations": [
    { "fieldName": "L_TAX_doubleSum", "name": "L_TAX_", "type": "doubleSum" },
    { "fieldName": "L_DISCOUNT_doubleSum", "name": "L_DISCOUNT_", "type": "doubleSum" },
    { "fieldName": "L_EXTENDEDPRICE_doubleSum", "name": "L_EXTENDEDPRICE_", "type": "doubleSum" },
    { "fieldName": "L_QUANTITY_longSum", "name": "L_QUANTITY_", "type": "longSum" },
    { "name": "count", "type": "count" }
  ],
  "dataSource": "tpch_year",
  "dimension": "l_orderkey",
  "filter": {
    "fields": [
      { "dimension": "l_orderkey", "type": "selector", "value": "103136" },
      { "dimension": "l_orderkey", "type": "selector", "value": "1648672" }
    ],
    "type": "or"
  },
  "granularity": "all",
  "intervals": [ "1900-01-09T00:00:00.000Z/2992-01-10T00:00:00.000Z" ],
  "metric": "L_QUANTITY_",
  "queryType": "topN",
  "threshold": 2
}
```

---

### GroupBy 쿼리

GroupBy 쿼리는 여러 디멘션을 기준으로 그룹핑해 집계 결과를 반환하는, 가장 유연한 집계 쿼리입니다.

```json
{
  "queryType": "groupBy",
  "dataSource": "sample_datasource",
  "granularity": "day",
  "dimensions": ["country", "device"],
  "limitSpec": {
    "type": "default",
    "limit": 5000,
    "columns": ["country", "data_transfer"]
  },
  "filter": {
    "type": "and",
    "fields": [
      { "type": "selector", "dimension": "carrier", "value": "AT&T" },
      {
        "type": "or",
        "fields": [
          { "type": "selector", "dimension": "make", "value": "Apple" },
          { "type": "selector", "dimension": "make", "value": "Samsung" }
        ]
      }
    ]
  },
  "aggregations": [
    { "type": "longSum", "name": "total_usage", "fieldName": "user_count" },
    { "type": "doubleSum", "name": "data_transfer", "fieldName": "data_transfer" }
  ],
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "avg_usage",
      "fn": "/",
      "fields": [
        { "type": "fieldAccess", "fieldName": "data_transfer" },
        { "type": "fieldAccess", "fieldName": "total_usage" }
      ]
    }
  ],
  "intervals": [ "2012-01-01T00:00:00.000/2012-01-03T00:00:00.000" ],
  "having": {
    "type": "greaterThan",
    "aggregation": "total_usage",
    "value": 100
  }
}
```

#### 속성

| 속성 | 설명 | 필수 여부 |
| --- | --- | --- |
| `queryType` | 항상 `"groupBy"` | 예 |
| `dataSource` | 조회 대상 데이터소스 | 예 |
| `dimensions` | 그룹핑 기준 디멘션 또는 DimensionSpec의 JSON 목록 | 예 |
| `virtualColumns` | 가상 컬럼 목록 | 아니요 |
| `limitSpec` | 결과 정렬·제한을 지정하는 LimitSpec | 아니요 |
| `having` | 집계 결과에 적용하는 having 절 필터 | 아니요 |
| `granularity` | 쿼리 그래뉼래리티 | 예 |
| `filter` | 필터 | 아니요 |
| `aggregations` | 집계 목록 | 아니요 |
| `postAggregations` | 후처리 집계 목록 | 아니요 |
| `intervals` | ISO-8601 interval 목록 | 예 |
| `subtotalsSpec` | 추가로 그룹핑할 디멘션 부분집합의 목록 | 아니요 |
| `context` | 추가 플래그를 담는 JSON 객체 | 아니요 |

#### 다중 값 디멘션에서의 동작

다중 값 디멘션으로 그룹핑하면 매칭된 행의 모든 값이 값별로 하나씩 그룹을 만들어, 결과 그룹이 행 수보다 많아질 수 있습니다. filtered dimensionSpec으로 필터에 매칭되는 값만 남기면 결과를 제한하면서 성능도 개선할 수 있습니다.

#### subtotalsSpec

`subtotalsSpec`을 사용하면 한 번의 쿼리로 여러 부분 그룹핑(sub-grouping)을 계산할 수 있습니다. 각 원소는 디멘션의 `outputName` 부분집합입니다.

```json
{
  "type": "groupBy",
  "dimensions": [
    { "type": "default", "dimension": "d1col", "outputName": "D1" },
    { "type": "extraction", "dimension": "d2col", "outputName": "D2", "extractionFn": "extraction_func" },
    { "type": "lookup", "dimension": "d3col", "outputName": "D3", "name": "my_lookup" }
  ],
  "subtotalsSpec": [ ["D1", "D2", "D3"], ["D1", "D3"], ["D3"] ]
}
```

각 부분 그룹핑의 결과 집합은 순서대로 이어 붙여 반환하며, 해당 그룹핑에서 제외된 디멘션은 null 값으로 반환합니다.

#### 메모리와 디스크 스필

GroupBy 쿼리의 자원 사용은 다음 파라미터로 제어합니다.

- `druid.processing.buffer.sizeBytes`: 쿼리당 off-heap 해시 테이블 크기(바이트)
- `druid.query.groupBy.maxSelectorDictionarySize`: 세그먼트별 on-heap 딕셔너리 한도
- `druid.query.groupBy.maxMergingDictionarySize`: 쿼리별 on-heap 딕셔너리 한도
- `druid.query.groupBy.maxOnDiskStorage`: 쿼리별 디스크 스필(spill) 한도(바이트, 0이면 비활성)
- `druid.query.groupBy.maxSpillFileCount`: 실패 전까지 허용하는 최대 스필 파일 수

`maxOnDiskStorage`가 0보다 크면, 메모리 한도에 도달했을 때 부분 집계된 레코드를 정렬해 디스크로 내려씁니다.

#### 성능 튜닝

- **Limit pushdown**: 가능한 경우 groupBy 쿼리의 limitSpec을 Historical의 세그먼트까지 내려보내 불필요한 중간 결과를 조기에 잘라냅니다. orderBy 필드가 그룹핑 키의 부분집합이면 기본으로 적용되며, `forceLimitPushDown` 컨텍스트 플래그로 제어합니다.
- **해시 테이블**: open addressing과 선형 탐사(linear probing)를 사용합니다. 초기 버킷 1024개, 최대 load factor 0.7이 기본이며, `bufferGrouperInitialBuckets`와 `bufferGrouperMaxLoadFactor`로 조정합니다.
- **Parallel combine**: 정렬된 집계 결과 병합에 처리 스레드를 추가로 사용하는 기능으로, 기본은 비활성입니다. `numParallelCombineThreads`로 제어하며, 데이터 스필이 필요하고 Historical 쿼리당 병합 버퍼 2개를 사용합니다.

#### 대안 쿼리

- **Timeseries**: 시간 기준으로만 그룹핑한다면 완전 스트리밍으로 동작하는 Timeseries가 더 낫습니다.
- **TopN**: 단일 디멘션 그룹핑, 특히 metric 기준 정렬에는 TopN이 더 빠릅니다.

#### 중첩 groupBy

중첩 groupBy(데이터소스 타입이 `query`)는 Broker가 내부 groupBy 쿼리를 먼저 일반적인 방식으로 실행하고, 그 결과 스트림 위에서 외부 쿼리를 실행합니다. 이때 off-heap fact map과 디스크로 스필 가능한 on-heap 문자열 딕셔너리를 사용합니다.

#### 런타임 설정 속성

| 속성 | 설명 | 기본값 |
| --- | --- | --- |
| `druid.query.groupBy.maxSelectorDictionarySize` | 세그먼트별 문자열 딕셔너리 최대 크기 | 0(자동) |
| `druid.query.groupBy.maxMergingDictionarySize` | 쿼리별 문자열 딕셔너리 최대 크기 | 0(자동) |
| `druid.query.groupBy.maxOnDiskStorage` | 쿼리별 디스크 스필 최대 크기 | 0(비활성) |
| `druid.query.groupBy.maxSpillFileCount` | 최대 스필 파일 수 | Integer.MAX_VALUE |
| `druid.query.groupBy.singleThreaded` | 병합을 단일 스레드로 수행 | false |
| `druid.query.groupBy.bufferGrouperInitialBuckets` | 해시 테이블 초기 버킷 수 | 0(1024) |
| `druid.query.groupBy.bufferGrouperMaxLoadFactor` | 해시 테이블 최대 load factor | 0(0.7) |
| `druid.query.groupBy.forceHashAggregation` | 해시 기반 집계 강제 | false |
| `druid.query.groupBy.intermediateCombineDegree` | combining 트리 차수 | 8 |
| `druid.query.groupBy.numParallelCombineThreads` | parallel combine 스레드 수 | 1(비활성) |
| `druid.query.groupBy.applyLimitPushDownToSegment` | 세그먼트 스캔 단계에서 limit 적용 | false |

대부분의 설정은 쿼리 컨텍스트로 개별 쿼리에서 재정의할 수 있습니다. 주요 컨텍스트 키: `maxOnDiskStorage`, `maxSpillFileCount`, `groupByIsSingleThreaded`, `bufferGrouperInitialBuckets`, `bufferGrouperMaxLoadFactor`, `forceHashAggregation`, `forceLimitPushDown`, `applyLimitPushDownToSegment`, `groupByEnableMultiValueUnnesting`, `deferExpressionDimensions`, `sortByDimsFirst`, `mergeThreadLocal`, `maxSelectorDictionarySize`, `maxMergingDictionarySize`, `resultAsArray`.

#### 배열 기반 결과 형식

컨텍스트에서 `resultAsArray`를 true로 지정하면 각 행이 위치 기반 배열로 반환됩니다. 타임스탬프(선택), 디멘션, aggregator, post-aggregator 순서입니다. 이 스키마는 응답에 포함되지 않으므로 발행한 쿼리로부터 직접 계산해야 합니다.

---

### Scan 쿼리

Scan 쿼리는 집계 없이 원본 Druid 행을 스트리밍 방식으로 반환합니다.

```json
{
  "queryType": "scan",
  "dataSource": "wikipedia",
  "resultFormat": "list",
  "columns": ["__time", "isRobot", "page", "added", "isAnonymous", "user", "deleted"],
  "intervals": [ "2016-01-01/2017-01-02" ],
  "batchSize": 20480,
  "limit": 2
}
```

#### 속성

| 속성 | 설명 | 필수 여부 |
| --- | --- | --- |
| `queryType` | 항상 `"scan"` | 예 |
| `dataSource` | 조회 대상 데이터소스 | 예 |
| `intervals` | ISO-8601 interval 목록 | 예 |
| `resultFormat` | 결과 형식. `list` 또는 `compactedList`(기본값 `list`) | 아니요 |
| `filter` | 필터 | 아니요 |
| `columns` | 반환할 디멘션/metric 목록. 비워 두면 전체 컬럼 반환 | 아니요 |
| `batchSize` | 버퍼링할 최대 행 수(기본값 20480) | 아니요 |
| `limit` | 반환할 총 행 수 | 아니요 |
| `offset` | 처음에 건너뛸 행 수 | 아니요 |
| `order` | 시간 정렬. `ascending`, `descending`, `none`(기본값) | 아니요 |
| `context` | 쿼리 컨텍스트 | 아니요 |

#### 결과 형식

`list` 형식은 각 이벤트를 JSON 객체로 반환합니다.

```json
[{
  "segmentId": "wikipedia_2016-06-27T00:00:00.000Z_2016-06-28T00:00:00.000Z_2024-12-17T13:08:03.142Z",
  "columns": ["__time", "isRobot", "page", "added", "isAnonymous", "user", "deleted"],
  "events": [{
    "__time": 1466985611080,
    "isRobot": "true",
    "page": "Salo Toraut",
    "added": 31,
    "isAnonymous": "false",
    "user": "Lsjbot",
    "deleted": 0
  }],
  "rowSignature": [{ "name": "__time", "type": "LONG" }]
}]
```

`compactedList` 형식은 각 이벤트를 값 배열로 반환합니다.

```json
[{
  "segmentId": "wikipedia_2016-06-27T00:00:00.000Z_2016-06-28T00:00:00.000Z_2024-12-17T13:08:03.142Z",
  "columns": ["__time", "isRobot", "page", "added", "isAnonymous", "user", "deleted"],
  "events": [
    [1466985611080, "true", "Salo Toraut", 31, "false", "Lsjbot", 0],
    [1466985634959, "false", "Bailando 2015", 2, "true", "181.230.118.178", 0]
  ],
  "rowSignature": [{ "name": "__time", "type": "LONG" }]
}]
```

#### 시간 정렬

Scan 쿼리는 타임스탬프 기준 정렬을 지원하지만 제약이 있습니다. 시간 정렬을 사용하면 세그먼트 ID가 null로 표시되며, 결과 limit이 `druid.query.scan.maxRowsQueuedForOrdering`보다 작거나, 스캔하는 모든 세그먼트의 파티션 수가 `druid.query.scan.maxSegmentPartitionsOrderedInMemory`보다 적어야 합니다.

시간 정렬에는 두 가지 전략을 사용합니다.

1. **Priority queue**: 세그먼트를 순차적으로 열면서 타임스탬프 기준으로 크기가 제한된 우선순위 큐를 유지합니다.
2. **N-way merge**: 파티션별 압축 해제 버퍼를 병렬로 열고, 파티션별로 미리 정렬된 결과를 병합합니다.

#### 설정 속성

| 속성 | 설명 | 값 범위 | 기본값 |
| --- | --- | --- | --- |
| `druid.query.scan.maxRowsQueuedForOrdering` | 시간 정렬 사용 시 큐에 쌓을 수 있는 최대 행 수 | [1, 2147483647] 정수 | 100000 |
| `druid.query.scan.maxSegmentPartitionsOrderedInMemory` | 시간 정렬 사용 시 Historical당 처리 가능한 최대 세그먼트 파티션 수 | [1, 2147483647] 정수 | 50 |

두 값 모두 쿼리 컨텍스트의 `maxRowsQueuedForOrdering`, `maxSegmentPartitionsOrderedInMemory`로 개별 쿼리에서 재정의할 수 있습니다.

```json
{
  "maxRowsQueuedForOrdering": 100001,
  "maxSegmentPartitionsOrderedInMemory": 100
}
```

레거시 모드(`legacy`)는 현재 버전에서 제거되었습니다.

---

### Search 쿼리

Search 쿼리는 검색어에 매칭되는 디멘션 값을 반환합니다.

```json
{
  "queryType": "search",
  "dataSource": "sample_datasource",
  "granularity": "day",
  "searchDimensions": [ "dim1", "dim2" ],
  "query": {
    "type": "insensitive_contains",
    "value": "Ke"
  },
  "sort": { "type": "lexicographic" },
  "intervals": [ "2013-01-01T00:00:00.000/2013-01-03T00:00:00.000" ]
}
```

#### 속성

| 속성 | 설명 | 필수 여부 |
| --- | --- | --- |
| `queryType` | 항상 `"search"` | 예 |
| `dataSource` | 조회 대상 데이터소스 | 예 |
| `granularity` | 결과 버킷팅 그래뉼래리티 | 아니요(기본값 `all`) |
| `filter` | 필터 | 아니요 |
| `limit` | Historical 프로세스당 최대 결과 수 | 아니요(기본값 1000) |
| `intervals` | ISO-8601 interval 목록 | 예 |
| `searchDimensions` | 검색 대상 디멘션 목록. 생략하면 모든 디멘션 검색 | 아니요 |
| `virtualColumns` | 가상 컬럼 목록 | 아니요 |
| `query` | SearchQuerySpec 객체 | 예 |
| `sort` | 결과 정렬 방식 | 아니요 |
| `context` | 쿼리 컨텍스트 | 아니요 |

#### 응답 예시

```json
[
  {
    "timestamp": "2013-01-01T00:00:00.000Z",
    "result": [
      { "dimension": "dim1", "value": "Ke$ha", "count": 3 },
      { "dimension": "dim2", "value": "Ke$haForPresident", "count": 1 }
    ]
  }
]
```

#### 정렬 방식

`sort`에는 `lexicographic`(기본값), `alphanumeric`, `strlen`, `numeric`을 지정할 수 있습니다.

#### 실행 전략

쿼리 컨텍스트의 `searchStrategy`로 실행 전략을 선택합니다.

- **useIndexes**(기본값): 검색 디멘션을 비트맵 인덱스 지원 여부에 따라 두 그룹으로 나눈 뒤, 인덱스 기반 실행 계획과 커서 기반 실행 계획을 각각 적용합니다.
- **cursorOnly**: 커서 기반 실행 계획만 생성합니다. 행을 직접 읽으며 조건식을 평가하므로, 선택도(selectivity)가 낮은 필터에서 유리할 수 있습니다.

#### SearchQuerySpec 종류

**insensitive_contains** — 대소문자 구분 없이 부분 문자열 포함 여부를 검사합니다.

```json
{ "type": "insensitive_contains", "value": "some_value" }
```

**fragment** — 여러 조각(fragment)이 모두 포함되는지 검사합니다.

```json
{ "type": "fragment", "case_sensitive": false, "values": ["fragment1", "fragment2"] }
```

**contains** — 대소문자 구분 여부를 지정해 부분 문자열 포함 여부를 검사합니다.

```json
{ "type": "contains", "case_sensitive": true, "value": "some_value" }
```

**regex** — 정규식 매칭입니다.

```json
{ "type": "regex", "pattern": "some_pattern" }
```

---

### 필터

필터는 SQL의 WHERE 절에 해당하며, 어떤 행을 포함할지 지정하는 JSON 객체입니다. 기본적으로 3값 불리언 논리(three-value Boolean logic)를 따릅니다.

#### selector 필터

가장 단순한 필터로, 특정 디멘션 값과의 일치를 검사합니다.

```json
{ "type": "selector", "dimension": "someColumn", "value": "hello" }
```

#### equality 필터

selector를 대체하는 최신 필터로, 모든 컬럼 타입을 지원하며 null과는 매칭되지 않습니다.

```json
{ "type": "equals", "column": "someColumn", "matchValueType": "STRING", "matchValue": "hello" }
```

#### null 필터

NULL 값 매칭 전용 필터입니다.

```json
{ "type": "null", "column": "someColumn" }
```

#### bound 필터

사전순(lexicographic) 또는 숫자(numeric) 순서 기반 범위 필터입니다.

```json
{ "type": "bound", "dimension": "age", "lower": "21", "upper": "31", "ordering": "numeric" }
```

#### range 필터

bound 필터를 대체하는 SQL 호환 범위 필터로, null과는 매칭되지 않습니다.

```json
{ "type": "range", "column": "age", "matchValueType": "LONG", "lower": 21, "upper": 31 }
```

#### like 필터

SQL LIKE처럼 `%`와 `_` 와일드카드를 지원합니다.

```json
{ "type": "like", "dimension": "last_name", "pattern": "D%" }
```

#### regex 필터

Java 정규식 패턴과 디멘션 값을 매칭합니다.

```json
{ "type": "regex", "dimension": "someColumn", "pattern": "^50.*" }
```

#### in 필터

문자열 집합에 포함되는 값을 매칭합니다.

```json
{ "type": "in", "dimension": "outlaw", "values": ["Good", "Bad", "Ugly"] }
```

#### arrayContainsElement 필터

배열 컬럼이 특정 원소를 포함하는지 검사합니다.

```json
{ "type": "arrayContainsElement", "column": "someArrayColumn", "elementMatchValueType": "STRING", "elementMatchValue": "hello" }
```

#### 논리 필터(and / or / not)

```json
{ "type": "and", "fields": [
  { "type": "equals", "column": "col1", "matchValueType": "STRING", "matchValue": "a" },
  { "type": "equals", "column": "col2", "matchValueType": "LONG", "matchValue": 1234 }
] }
```

```json
{ "type": "or", "fields": [
  { "type": "equals", "column": "col1", "matchValueType": "STRING", "matchValue": "a" },
  { "type": "null", "column": "col3" }
] }
```

```json
{ "type": "not", "field": { "type": "null", "column": "someColumn" } }
```

#### interval 필터

long 밀리초 값을 담은 컬럼에 ISO-8601 interval 기반 범위 필터를 적용합니다.

```json
{ "type": "interval", "dimension": "__time", "intervals": [
  "2014-10-01T00:00:00.000Z/2014-10-07T00:00:00.000Z"
] }
```

#### search 필터

부분 문자열 매칭 필터로, 대소문자 구분 옵션을 지원합니다.

```json
{ "type": "search", "dimension": "product", "query": {
  "type": "insensitive_contains", "value": "foo"
} }
```

#### expression 필터

Druid 표현식(expression) 시스템으로 임의의 조건을 기술합니다.

```json
{ "type": "expression", "expression": "((product_type == 42) && (!is_deleted))" }
```

#### javascript 필터

JavaScript 함수를 조건식으로 사용합니다. JavaScript 기능은 기본적으로 비활성화되어 있습니다.

```json
{ "type": "javascript", "dimension": "name", "function": "function(x) { return(x >= 'bar' && x <= 'foo') }" }
```

#### columnComparison 필터

디멘션끼리 비교합니다. 비교 시 값을 문자열로 변환합니다.

```json
{ "type": "columnComparison", "dimensions": ["someColumn", { "type": "default", "dimension": "someLongColumn" }] }
```

#### true / false 필터

`true` 필터는 모든 값과 매칭되며 필터를 임시로 무력화할 때 사용하고, `false` 필터는 아무 값과도 매칭되지 않아 빈 결과를 강제할 때 사용합니다.

```json
{ "type": "true" }
```

```json
{ "type": "false" }
```

#### 타임스탬프 컬럼 필터링

타임스탬프 컬럼은 디멘션 이름 `__time`으로 참조합니다. long 밀리초 값 비교, 포맷 변환을 위한 extraction 함수, ISO-8601 interval을 지원합니다.

```json
{ "type": "equals", "dimension": "__time", "matchValueType": "LONG", "value": 124457387532 }
```

#### extraction 함수와 함께 필터링

spatial 필터를 제외한 모든 필터는 extraction 함수를 지원하며, 필터링 전에 디멘션 값에 적용됩니다. 별도의 extraction 필터는 deprecated이므로, extraction 함수를 지정한 selector 필터를 사용합니다.

```json
{ "type": "selector", "dimension": "product", "value": "bar_1", "extractionFn": {
  "type": "lookup", "lookup": { "type": "map", "map": { "product_1": "bar_1", "product_5": "bar_1" } }
} }
```

#### 참고 사항

- SQL 플래너는 `sqlUseBoundAndSelectors`를 활성화하지 않는 한 selector·bound 필터 대신 equality·null·range 필터를 사용합니다.
- 다중 값 문자열 컬럼은 값 중 하나라도 필터를 만족하면 매칭됩니다.
- 숫자 컬럼에 문자열 매칭 값을 지정하면 자동으로 형 변환합니다.

---

### 집계

집계(aggregation)는 데이터 인제스천(ingestion) 시점에 롤업의 일부로 지정할 수도 있고, 쿼리 시점에 지정할 수도 있습니다.

#### count

Druid 행 수를 셉니다.

```json
{ "type": "count", "name": "count" }
```

`count`는 Druid 행 수를 세는 것이므로, 인제스천 시 롤업 설정에 따라 원본 이벤트 수와 다를 수 있습니다.

#### sum 계열

| 타입 | 설명 |
| --- | --- |
| `longSum` | 64비트 부호 있는 정수 합 |
| `doubleSum` | 64비트 부동소수점 합 |
| `floatSum` | 32비트 부동소수점 합 |

```json
{ "type": "longSum", "name": "sumLong", "fieldName": "aLong" }
```

sum 계열 aggregator는 `fieldName` 또는 `expression` 중 하나를 지정해야 합니다.

#### min / max 계열

`doubleMin`, `doubleMax`, `floatMin`, `floatMax`, `longMin`, `longMax` 여섯 가지가 있습니다.

```json
{ "type": "doubleMin", "name": "maxDouble", "fieldName": "aDouble" }
```

#### first / last 계열

숫자형은 `doubleFirst`, `doubleLast`, `floatFirst`, `floatLast`, `longFirst`, `longLast`를 지원합니다.

```json
{ "type": "doubleFirst", "name": "firstDouble", "fieldName": "aDouble" }
```

문자열형은 `stringFirst`, `stringLast`를 지원하며 `maxStringBytes`(기본값 1024)를 지정할 수 있습니다.

```json
{ "type": "stringFirst", "name": "firstString", "fieldName": "aString", "maxStringBytes": 2048 }
```

롤업이 적용된 세그먼트에 first/last aggregator를 사용하면 롤업된 값을 반환할 뿐, 인제스천된 원본 데이터의 첫/마지막 값을 반환하지 않습니다.

#### any 계열

만난 값 중 아무 값이나 반환하며, 쿼리 시점에서만 사용할 수 있습니다. 숫자형은 `doubleAny`, `floatAny`, `longAny`, 문자열형은 `stringAny`이며, `stringAny`는 `aggregateMultipleValues` 플래그(기본값 true)를 지원합니다.

```json
{ "type": "stringAny", "name": "anyString", "fieldName": "aString", "maxStringBytes": 2048 }
```

#### doubleMean

산술 평균을 계산하며, 쿼리 시점에서만 사용할 수 있습니다.

```json
{ "type": "doubleMean", "name": "aMean", "fieldName": "aDouble" }
```

#### 근사 집계

**Count distinct**

- DataSketches Theta Sketch: 합집합·교집합·차집합 등 집합 연산 지원
- DataSketches HLL Sketch: 메모리 사용량이 더 적고 집합 연산 미지원
- Cardinality / HyperUnique: 내장 레거시 구현. DataSketches 계열 사용을 권장

**분위수(quantile)·히스토그램**

- DataSketches Quantiles Sketch: 공식적인 오차 범위를 제공하므로 권장
- Moments Sketch: 실험적. 병합 속도에 최적화되어 있으나 정확도가 분포에 의존
- Fixed Buckets Histogram: 성능이 데이터에 의존
- Approximate Histogram: 분포에 따라 왜곡이 발생해 deprecated

#### expression aggregator

Druid 표현식으로 커스텀 집계를 정의합니다.

```json
{
  "type": "expression",
  "name": "expression_count",
  "fields": [],
  "initialValue": "0",
  "fold": "__acc + 1",
  "combine": "__acc + expression_count"
}
```

주요 속성은 다음과 같습니다.

| 속성 | 설명 |
| --- | --- |
| `initialValue` | 누산기(accumulator) 초기 상태 |
| `fold` | 행 단위 누산 표현식 |
| `combine` | 세그먼트 간 병합 표현식 |
| `finalize` | 출력 변환 표현식(선택) |
| `compare` | 비교자 표현식(선택) |

#### javascript aggregator

JavaScript 함수로 집계를 정의합니다. JavaScript 기능은 기본적으로 비활성화되어 있습니다.

```json
{
  "type": "javascript",
  "name": "sum(log(x)*y) + 10",
  "fieldNames": ["x", "y"],
  "fnAggregate": "function(current, a, b) { return current + (Math.log(a) * b); }",
  "fnCombine": "function(partialA, partialB) { return partialA + partialB; }",
  "fnReset": "function() { return 10; }"
}
```

#### filtered aggregator

임의의 aggregator를 필터로 감싸, 필터에 매칭되는 행만 집계합니다.

```json
{
  "type": "filtered",
  "name": "filteredSumLong",
  "filter": {
    "type": "selector",
    "dimension": "someColumn",
    "value": "abcdef"
  },
  "aggregator": {
    "type": "longSum",
    "name": "sumLong",
    "fieldName": "aLong"
  }
}
```

#### grouping aggregator

groupBy 쿼리의 `subtotalsSpec`과 함께 사용하며, 각 행이 어떤 디멘션 조합으로 그룹핑되었는지 비트로 인코딩해 반환합니다. 부분 그룹핑에서 제외된 디멘션의 비트가 1이 됩니다.

```json
{ "type": "grouping", "name": "someGrouping", "groupings": ["dim1", "dim2"] }
```

---

### 그래뉼래리티

그래뉼래리티(granularity)는 데이터를 시간 단위로 버킷팅하는 방법을 결정합니다. simple, duration, period 세 가지 방식으로 지정합니다.

#### Simple 그래뉼래리티

문자열로 지정하는 사전 정의된 시간 버킷이며, UTC 기준입니다.

```
all, none, second, minute, five_minute, ten_minute, fifteen_minute,
thirty_minute, hour, six_hour, eight_hour, day, week, month, quarter, year
```

- `all`: 모든 데이터를 하나의 버킷으로 집계합니다.
- `none`: 밀리초 단위(내부 인덱스 해상도와 동일)로 버킷팅합니다.

Timeseries 쿼리에서는 `none`을 사용하지 않아야 합니다. 모든 밀리초마다 결과를 만들고 내부의 빈 버킷을 0으로 채우기 때문입니다.

groupBy 쿼리를 `hour` 그래뉼래리티로 실행하면 시간 단위 버킷별 결과가, `day`로 실행하면 일 단위 버킷별 결과가 반환됩니다. groupBy에서는 빈 버킷을 모두 버립니다.

#### Duration 그래뉼래리티

밀리초 단위 길이로 지정하며, 기준점(`origin`)을 선택적으로 지정합니다. 다음은 2시간 버킷입니다.

```json
{ "type": "duration", "duration": 7200000 }
```

`origin`을 지정하면 해당 시각부터 버킷을 나눕니다. 다음은 매시 30분을 기준으로 1시간 단위로 자르는 예시입니다.

```json
{ "type": "duration", "duration": 3600000, "origin": "2012-01-01T00:30:00Z" }
```

#### Period 그래뉼래리티

ISO-8601 기간 형식으로 지정하며, 시간대(`timeZone`)와 기준점(`origin`)을 선택적으로 지정합니다.

```json
{ "type": "period", "period": "P2D", "timeZone": "America/Los_Angeles" }
```

```json
{ "type": "period", "period": "P3M", "timeZone": "America/Los_Angeles", "origin": "2012-02-01T00:00:00-08:00" }
```

시간대 지원은 Joda Time 라이브러리가 제공하며, 표준 IANA 시간대를 사용합니다.

---

### 쿼리 컨텍스트

쿼리 컨텍스트는 쿼리 계획과 실행 방식을 제어하는 파라미터 모음입니다.

#### 컨텍스트 지정 방법

- **네이티브 쿼리**: 쿼리 JSON 안에 `context` 객체로 포함합니다.
- **웹 콘솔**: Query 뷰에서 Edit query context를 열어 JSON 파라미터를 추가합니다.
- **JDBC 드라이버**: 데이터베이스 연결 시 프로퍼티로 지정합니다.
- **SQL SET 문**: `SET sqlTimeZone = 'America/Los_Angeles';`처럼 지정합니다. 단, JDBC 연결에서는 SET 문을 사용할 수 없습니다.
- **런타임 프로퍼티**: `druid.query.default.context.{PARAMETER}={VALUE}` 형식으로 기본값을 지정합니다.

우선순위는 낮은 순서부터 내장 기본값 → 런타임 프로퍼티 → Broker 동적 설정 → HTTP 요청의 context 객체 → SET 문입니다.

#### 일반 파라미터

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `timeout` | `druid.server.http.defaultQueryTimeout` | 밀리초 단위 쿼리 타임아웃. 초과 시 미완료 쿼리를 취소 |
| `priority` | 0 | 우선순위가 높은 쿼리가 연산 자원을 먼저 배정받음 |
| `lane` | `null` | 쿼리 lane. 쿼리 부류별 사용량 제한에 사용 |
| `queryId` | 자동 생성 | 쿼리 고유 식별자 |
| `brokerService` | `null` | 이 쿼리를 라우팅할 Broker 서비스 |
| `useCache` | `true` | 쿼리 캐시 사용 여부 |
| `populateCache` | `true` | 결과를 쿼리 캐시에 저장할지 여부 |
| `useResultLevelCache` | `true` | 결과 수준(result level) 캐시 사용 여부 |
| `populateResultLevelCache` | `true` | 결과를 결과 수준 캐시에 저장할지 여부 |
| `bySegment` | `false` | 결과를 세그먼트 단위로 묶어 반환 |
| `finalize` | 해당 없음 | 집계 결과의 finalize 수행 여부 |
| `maxScatterGatherBytes` | `druid.server.http.maxScatterGatherBytes` | 데이터 프로세스에서 수집하는 최대 바이트 수 |
| `maxQueuedBytes` | `druid.broker.http.maxQueuedBytes` | 백프레셔 발생 전 쿼리당 큐잉 가능한 최대 바이트 수 |
| `maxSubqueryRows` | `druid.server.http.maxSubqueryRows` | 서브쿼리가 생성할 수 있는 행 수 상한 |
| `maxSubqueryBytes` | `druid.server.http.maxSubqueryBytes` | 서브쿼리가 생성할 수 있는 바이트 수 상한 |
| `serializeDateTimeAsLong` | `false` | true면 Broker 결과에서 DateTime을 long으로 직렬화 |
| `serializeDateTimeAsLongInner` | `false` | true면 Broker와 데이터 프로세스 간 전송에서 DateTime을 long으로 직렬화 |
| `enableParallelMerge` | `true` | Broker에서 결과 병렬 병합 활성화 |
| `parallelMergeParallelism` | `druid.processing.merge.parallelism` | 결과 병합에 사용할 최대 병렬 스레드 수 |
| `parallelMergeInitialYieldRows` | `druid.processing.merge.initialYieldNumRows` | 병합 태스크가 fork 전에 yield할 행 수 |
| `parallelMergeSmallBatchRows` | `druid.processing.merge.smallBatchNumRows` | 병합 태스크의 결과 배치 크기 |
| `useFilterCNF` | `false` | 쿼리 필터를 논리곱 정규형(CNF)으로 변환 |
| `secondaryPartitionPruning` | `true` | Broker에서 2차 파티션 프루닝(pruning) 활성화 |
| `debug` | `false` | 디버깅 출력과 예외 스택 트레이스 활성화 |
| `setProcessingThreadNames` | `true` | 스레드 덤프 해석을 돕도록 처리 스레드 이름 설정 |

#### 쿼리 타입별 파라미터

**TopN**

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `minTopNThreshold` | 1000 | 각 세그먼트에서 병합용으로 반환할 상위 로컬 결과 수 |

**Timeseries**

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `skipEmptyBuckets` | `false` | zero-filling을 끄고 결과가 있는 버킷만 반환 |

**Join 필터**

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `enableJoinFilterPushDown` | `true` | 조인 대상 행을 줄이도록 필터 push down 시도 |
| `enableJoinFilterRewrite` | `true` | 베이스 테이블이 아닌 컬럼을 참조하는 필터 재작성 |
| `enableJoinFilterRewriteValueColumnFilters` | `false` | 비 베이스 테이블의 키가 아닌 컬럼에 대한 필터 재작성 |
| `enableRewriteJoinToFilter` | `true` | 조인을 부분 또는 전체적으로 베이스 테이블 필터로 변환 |
| `joinFilterRewriteMaxSize` | 10000 | 필터 재작성 시 상관 값 집합의 최대 크기 |

#### 벡터화 파라미터

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `vectorize` | `true` | 벡터화 실행 제어. `false`, `true`, `force` 지정 가능 |
| `vectorSize` | 512 | 벡터화 쿼리 처리 시 행 배치 크기 |
| `vectorizeVirtualColumns` | `true` | 가상 컬럼의 벡터화 활성화 여부 |
