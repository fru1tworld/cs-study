# ES|QL (Elasticsearch Query Language)

## 1. ES|QL 개요

### 1.1 ES|QL이란?

ES|QL(Elasticsearch Query Language)은 데이터를 필터링, 변환, 분석하기 위한 파이프라인 기반 쿼리 언어입니다. ES|QL을 사용하면 특정 이벤트를 찾고, 통계 분석을 수행하며, 시각화를 생성할 수 있습니다.

ES|QL은 Elasticsearch에 저장된 데이터와 향후 다른 런타임의 데이터를 필터링, 변환, 분석하는 강력한 방법을 제공합니다. 최종 사용자, SRE 팀, 애플리케이션 개발자 및 관리자가 쉽게 배우고 사용할 수 있도록 설계되었습니다.

### 1.2 주요 특징

- 파이프라인 기반: ES|QL은 파이프(|)를 사용하여 데이터를 단계별로 조작하고 변환합니다. 이 접근 방식을 통해 사용자는 일련의 작업을 구성할 수 있으며, 한 작업의 출력이 다음 작업의 입력이 됩니다.

- 고성능 실행 엔진: 새로운 ES|QL 실행 엔진은 성능을 염두에 두고 설계되었습니다. 행 단위가 아닌 블록 단위로 작동하며, 벡터화 및 캐시 지역성을 목표로 하고, 특수화 및 멀티스레딩을 수용합니다.

- 새로운 엔드포인트: ES|QL은 Query DSL로 변환되지 않고 `_query` 엔드포인트를 통해 Elasticsearch 내에서 직접 실행됩니다.

- 다양한 사용 환경: Elasticsearch API, Kibana Discover, 대시보드, Dev Tools, Elastic Security 및 Observability의 분석 도구에서 ES|QL을 사용할 수 있습니다.

### 1.3 ES|QL 사용 가능 환경

- Elasticsearch `_query` 엔드포인트를 통한 프로그래밍 방식 접근
- Kibana Discover
- Kibana 대시보드
- Dev Tools
- Elastic Security 분석 도구
- Elastic Observability 분석 도구

---

## 2. ES|QL 구문 (Syntax)

### 2.1 기본 구조

ES|QL 쿼리는 소스 명령(source command)으로 시작하고, 선택적으로 처리 명령(processing commands)이 파이프 문자(|)로 구분되어 뒤따릅니다.

```
소스-명령
| 처리-명령1
| 처리-명령2
```

쿼리의 결과는 최종 처리 명령이 생성하는 테이블입니다.

### 2.2 구문 규칙

- 대소문자 구분: ES|QL 키워드는 대소문자를 구분하지 않습니다.
- 가독성: 각 처리 명령을 새 줄에 작성하고 들여쓰기를 추가할 수 있습니다.
- 기본 제한: 명시적인 LIMIT 없이 ES|QL 쿼리를 실행하면 암시적으로 1000개의 제한이 적용됩니다.

### 2.3 기본 예제

```esql
FROM kibana_sample_data_logs
| LIMIT 10
| KEEP @timestamp, clientip, machine.os
```

설명:
- `FROM` 명령은 인덱스, 데이터뷰 또는 별칭을 선택하고 테이블로 이벤트를 반환합니다.
- `LIMIT` 명령은 반환할 이벤트 수를 지정합니다.
- `KEEP` 명령은 반환할 필드를 지정합니다.

### 2.4 주석

ES|QL은 주석을 지원합니다:

```esql
FROM employees
| WHERE emp_no == 10001  // 특정 직원 필터링
| KEEP first_name, last_name, salary
```

### 2.5 이스케이프 및 특수 문자

특수 문자가 포함된 인덱스 이름을 이스케이프하려면 큰따옴표(`"`) 또는 세 개의 큰따옴표(`"""`)를 사용합니다.

```esql
FROM "my-index-*"
| LIMIT 10
```

### 2.6 날짜 수학 (Date Math)

인덱스, 별칭 및 데이터 스트림을 참조할 때 날짜 수학을 사용할 수 있습니다. 이는 시계열 데이터에 유용합니다.

```esql
FROM "<logs-{now/d}>"
| LIMIT 10
```

---

## 3. ES|QL 명령 (Commands)

ES|QL 명령은 두 가지 유형으로 나뉩니다:
- 소스 명령 (Source Commands): 쿼리의 시작점으로, 데이터를 가져옵니다.
- 처리 명령 (Processing Commands): 입력 테이블을 수정합니다.

### 3.1 소스 명령

#### 3.1.1 FROM

`FROM` 명령은 데이터 스트림, 인덱스 또는 별칭에서 데이터가 포함된 테이블을 반환합니다. 결과 테이블의 각 행은 문서를 나타내고, 각 열은 필드에 해당합니다.

구문:
```esql
FROM <인덱스_패턴>
```

예제:
```esql
// 단일 인덱스
FROM employees
| LIMIT 10

// 와일드카드 패턴
FROM projects*
| LIMIT 10

// 여러 인덱스 (쉼표로 구분)
FROM employees, contractors
| LIMIT 10
```

메타데이터 필드 사용:
```esql
FROM employees METADATA _index, _id
| KEEP _index, _id, first_name, last_name
```

지원되는 메타데이터 필드:
- `_index`: 문서가 속한 인덱스 (keyword 타입)
- `_id`: 소스 문서의 ID
- `_score`: 관련성 점수 (전문 검색 시)

#### 3.1.2 ROW

`ROW` 명령은 지정한 값으로 하나 이상의 열이 있는 행을 생성합니다. 테스트에 유용합니다.

구문:
```esql
ROW <column1>=<value1>, <column2>=<value2>, ...
```

예제:
```esql
ROW a=1, b="hello", c=true

ROW name="John Doe", age=30, is_active=true
| EVAL greeting = CONCAT("Hello, ", name)
```

다중값 열:
```esql
ROW key=[1, 2, 3, 4, 5]
| MV_EXPAND key
```

#### 3.1.3 SHOW

`SHOW` 명령은 배포 및 기능에 대한 정보를 반환합니다.

구문:
```esql
SHOW INFO
```

예제:
```esql
SHOW INFO
```

이 명령은 배포의 버전, 빌드 날짜 및 해시를 반환합니다.

#### 3.1.4 TS (Time Series) - 기술 프리뷰

`TS` 명령은 시계열 데이터 스트림에서 메트릭 값을 집계하는 데 사용됩니다.

특징:
- 시계열 분석은 시간 버킷에 걸쳐 메트릭 값을 요약하는 집계 쿼리를 기반으로 합니다.
- ES|QL 컴퓨트 엔진을 통한 벡터화된 쿼리 실행을 추가합니다.
- DSL 쿼리 대비 10배 이상의 성능 향상을 제공합니다.

---

### 3.2 처리 명령

#### 3.2.1 WHERE

`WHERE` 명령은 제공된 조건이 true로 평가되는 모든 행을 포함하는 테이블을 생성합니다.

구문:
```esql
WHERE <조건>
```

예제:
```esql
// 기본 필터링
FROM employees
| WHERE salary > 50000
| LIMIT 10

// 날짜 수학을 사용한 시간 범위 필터링
FROM sample_data
| WHERE @timestamp > NOW() - 1 hour

// 여러 조건
FROM employees
| WHERE department == "Engineering" AND years_of_experience >= 5
| KEEP first_name, last_name, department
```

#### 3.2.2 EVAL

`EVAL` 명령은 계산된 값으로 테이블에 열을 추가합니다.

구문:
```esql
EVAL <새_열_이름> = <표현식>
```

예제:
```esql
// 단위 변환
FROM sample_data
| EVAL duration_ms = event_duration / 1000000.0

// 여러 열 계산
FROM kibana_sample_data_logs
| STATS avg_bytes = AVG(bytes) BY geo.src
| EVAL avg_bytes_kb = ROUND(avg_bytes / 1024, 2)
| EVAL avg_bytes_mb = ROUND(avg_bytes / (1024 * 1024), 4)
```

#### 3.2.3 STATS ... BY

`STATS` 명령은 AVG, COUNT, SUM 등의 집계 함수를 사용하여 데이터를 그룹화하고 집계합니다.

구문:
```esql
STATS <집계_표현식> [BY <그룹_필드>]
```

지원되는 집계 함수:
- `AVG`: 평균값
- `COUNT`: 개수
- `COUNT_DISTINCT`: 고유 값 개수
- `MAX`: 최대값
- `MIN`: 최소값
- `MEDIAN`: 중앙값
- `MEDIAN_ABSOLUTE_DEVIATION`: 중앙값 절대 편차
- `PERCENTILE`: 백분위수
- `SUM`: 합계
- `STD_DEV`: 표준 편차
- `VARIANCE`: 분산
- `VALUES`: 모든 고유 값
- `TOP`: 상위 N개 값
- `WEIGHTED_AVG`: 가중 평균

예제:
```esql
// 기본 집계
FROM employees
| STATS avg_salary = AVG(salary), max_salary = MAX(salary)

// 그룹별 집계
FROM employees
| STATS
    avg_salary = AVG(salary),
    median_salary = MEDIAN(salary),
    headcount = COUNT(*)
  BY department
| SORT avg_salary DESC

// 여러 그룹 필드
FROM kibana_sample_data_logs
| STATS total = COUNT(*) BY machine.os, geo.src
| SORT total DESC
| LIMIT 20
```

#### 3.2.4 INLINE STATS (INLINESTATS) - 프리뷰

`INLINESTATS` 명령은 그룹별로 통계를 계산하고 결과를 원래 행에 새 열로 추가합니다. STATS와 달리 행을 축소하지 않습니다.

구문:
```esql
INLINESTATS <집계_표현식> [BY <그룹_필드>]
```

예제:
```esql
// 각 행에 그룹 통계 추가
FROM employees
| INLINESTATS avg_dept_salary = AVG(salary) BY department
| EVAL salary_vs_avg = salary - avg_dept_salary
| KEEP first_name, department, salary, avg_dept_salary, salary_vs_avg
```

동작 방식:
입력 데이터:
| a | b  |
|---|-----|
| 1 | 10 |
| 1 | 20 |
| 2 | 20 |
| 2 | 15 |

`INLINESTATS MIN(b) BY a` 실행 후:
| a | b  | MIN(b) |
|---|-----|--------|
| 1 | 10 | 10     |
| 1 | 20 | 10     |
| 2 | 20 | 15     |
| 2 | 15 | 15     |

#### 3.2.5 SORT

`SORT` 명령은 하나 이상의 열을 기준으로 테이블을 정렬합니다.

구문:
```esql
SORT <열> [ASC|DESC] [NULLS FIRST|NULLS LAST]
```

예제:
```esql
// 오름차순 정렬
FROM employees
| SORT salary ASC
| LIMIT 10

// 내림차순 정렬
FROM kibana_sample_data_logs
| STATS total = COUNT(machine.os) BY machine.os
| SORT total DESC

// 여러 열로 정렬
FROM employees
| SORT department ASC, salary DESC
| LIMIT 20
```

#### 3.2.6 LIMIT

`LIMIT` 명령은 반환할 행 수를 제한합니다.

구문:
```esql
LIMIT <숫자>
```

제한 사항:
- 쿼리는 LIMIT 값에 관계없이 10,000개 이상의 행을 반환하지 않습니다. 이는 구성 가능한 상한선입니다.
- Kibana Discover에서는 10,000개 이상의 행을 표시하지 않습니다.

예제:
```esql
FROM employees
| SORT salary DESC
| LIMIT 5
```

명령 순서의 중요성:
```esql
// 정렬 후 제한 (상위 3개)
FROM employees
| SORT salary DESC
| LIMIT 3

// 제한 후 정렬 (임의의 3개를 정렬) - 다른 결과!
FROM employees
| LIMIT 3
| SORT salary DESC
```

#### 3.2.7 KEEP

`KEEP` 명령은 반환할 열과 그 순서를 지정합니다. 와일드카드를 지원합니다.

구문:
```esql
KEEP <열1>, <열2>, ...
```

예제:
```esql
// 특정 열만 유지
FROM employees
| KEEP first_name, last_name, salary, department

// 와일드카드 사용
FROM employees
| KEEP first_*, *_date, salary
```

#### 3.2.8 DROP

`DROP` 명령은 테이블에서 열을 제거합니다.

구문:
```esql
DROP <열1>, <열2>, ...
```

예제:
```esql
FROM employees
| DROP internal_id, temp_field, debug_info
```

#### 3.2.9 RENAME

`RENAME` 명령은 하나 이상의 열 이름을 변경합니다.

구문:
```esql
RENAME <기존_이름> AS <새_이름>
```

주의 사항:
- 새 이름이 기존 열 이름과 충돌하면 기존 열이 삭제됩니다.
- 여러 열이 같은 이름으로 변경되면 가장 오른쪽 열만 유지됩니다.

예제:
```esql
FROM employees
| RENAME first_name AS fname, last_name AS lname
| KEEP fname, lname, salary
```

#### 3.2.10 DISSECT

`DISSECT` 명령은 구분자 기반 패턴을 사용하여 문자열을 분해하고 지정된 키를 열로 추출합니다.

특징:
- 구분자 기반으로 작동하여 GROK보다 빠릅니다.
- 데이터가 일관되게 반복될 때 잘 작동합니다.
- 모든 일치된 값은 keyword 문자열 데이터 타입으로 출력됩니다.

구문:
```esql
DISSECT <필드> "<패턴>"
```

예제:
```esql
FROM logs
| DISSECT message "%{timestamp} %{level} %{message_text}"

// 로그 에이전트 파싱
FROM kibana_sample_data_logs
| DISSECT agent "%{browser}/%{version}"
| KEEP browser, version, @timestamp
```

#### 3.2.11 GROK

`GROK` 명령은 정규 표현식 기반 패턴을 사용하여 문자열을 분석하고 지정된 키를 열로 추출합니다.

특징:
- 정규 표현식 기반으로 DISSECT보다 강력하지만 일반적으로 느립니다.
- 텍스트 구조가 행마다 다를 때 더 나은 선택입니다.
- Oniguruma 정규 표현식 라이브러리를 사용합니다.
- 사용자 정의 패턴이나 다중 패턴은 지원하지 않습니다.

구문:
```esql
GROK <필드> "<패턴>"
```

예제:
```esql
FROM kibana_sample_data_logs
| GROK agent "%{WORD:browser}/%{NUMBER:version}"
| KEEP browser, version, @timestamp

// 복잡한 로그 파싱
FROM logs
| GROK message "%{IP:client_ip} - %{USER:user} \\[%{HTTPDATE:timestamp}\\] \"%{WORD:method} %{URIPATHPARAM:request}\""
```

#### 3.2.12 DISSECT와 GROK 하이브리드 사용

일부 라인이 일관되게 반복되지만 전체 라인은 그렇지 않은 하이브리드 사용 사례에서 DISSECT와 GROK를 함께 사용할 수 있습니다.

```esql
FROM logs
| DISSECT message "%{timestamp} %{rest}"
| GROK rest "%{LOGLEVEL:level} %{GREEDYDATA:details}"
```

#### 3.2.13 ENRICH

`ENRICH` 명령은 다른 인덱스의 데이터로 테이블을 보강합니다.

구문:
```esql
ENRICH <정책_이름> ON <매치_필드> [WITH <필드1>, <필드2>, ...]
```

특징:
- 기본적으로 정책에 정의된 모든 보강 필드가 열로 추가됩니다.
- `WITH`를 사용하여 추가할 필드를 명시적으로 선택할 수 있습니다.
- `WITH new_name=<field>`를 사용하여 열 이름을 변경할 수 있습니다.
- 이름 충돌 시 새로 생성된 열이 기존 열을 덮어씁니다.

예제:
```esql
FROM kibana_sample_data_logs
| STATS avg_bytes = AVG(bytes) BY geo.src
| EVAL avg_bytes_kb = ROUND(avg_bytes / 1024, 2)
| ENRICH geo-data ON geo.src WITH continent, country
| KEEP avg_bytes_kb, geo.src, country, continent
```

#### 3.2.14 MV_EXPAND

`MV_EXPAND` 명령은 다중값 열을 값당 하나의 행으로 확장하고 다른 열을 복제합니다.

구문:
```esql
MV_EXPAND <다중값_열>
```

주의 사항:
- MV_EXPAND로 생성된 출력 행은 순서가 지정되지 않으며 이전 SORT를 준수하지 않을 수 있습니다.

예제:
```esql
ROW tags=["elasticsearch", "search", "analytics"]
| MV_EXPAND tags

// 실제 사용 예
FROM products
| MV_EXPAND categories
| STATS product_count = COUNT(*) BY categories
```

#### 3.2.15 LOOKUP JOIN

`LOOKUP JOIN` 명령은 ES|QL 쿼리 결과 테이블과 지정된 조회 인덱스의 일치하는 레코드를 결합합니다.

특징:
- Elasticsearch의 첫 번째 SQL 스타일 JOIN입니다.
- LEFT OUTER JOIN으로 동작합니다.
- "lookup" 인덱스 모드에 의존합니다.
- 조회 인덱스는 단일 샤드이므로 20억 개의 문서로 제한됩니다.
- Elasticsearch 9.2부터 Cross-Cluster Search(CCS)를 지원합니다.

구문:
```esql
LOOKUP JOIN <조회_인덱스> ON <매치_필드>
```

예제:
```esql
// 서비스 소유자 정보 조인
FROM app_logs
| LOOKUP JOIN service_owners ON service_id

// IP 위협 인텔리전스 조인
FROM firewall_logs
| LOOKUP JOIN threat_intel ON source_ip
| WHERE threat_level == "high"
```

---

## 4. ES|QL 함수 및 연산자

### 4.1 집계 함수 (Aggregation Functions)

STATS 및 INLINE STATS 명령에서 지원되는 집계 함수:

| 함수 | 설명 |
|------|------|
| `AVG(field)` | 입력 값의 산술 평균 |
| `COUNT(*)` | 행의 총 개수 |
| `COUNT(field)` | 필드 값의 개수 (null 제외) |
| `COUNT_DISTINCT(field)` | 고유 값의 개수 |
| `MAX(field)` | 최대값 |
| `MIN(field)` | 최소값 |
| `MEDIAN(field)` | 중앙값 |
| `MEDIAN_ABSOLUTE_DEVIATION(field)` | 중앙값 절대 편차 |
| `PERCENTILE(field, percentile)` | 지정된 백분위수 값 |
| `SUM(field)` | 합계 |
| `STD_DEV(field)` | 표준 편차 |
| `VARIANCE(field)` | 분산 |
| `VALUES(field)` | 모든 고유 값 |
| `TOP(field, limit, order)` | 상위/하위 N개 값 |
| `WEIGHTED_AVG(value, weight)` | 가중 평균 |
| `SAMPLE(field, n)` | 무작위 n개 샘플 |

예제:
```esql
FROM employees
| STATS
    avg_salary = AVG(salary),
    max_salary = MAX(salary),
    min_salary = MIN(salary),
    median_salary = MEDIAN(salary),
    total_employees = COUNT(*),
    unique_departments = COUNT_DISTINCT(department),
    salary_sum = SUM(salary),
    salary_stddev = STD_DEV(salary)
  BY department
| SORT avg_salary DESC
```

### 4.2 시계열 집계 함수 (Time-Series Aggregate Functions)

| 함수 | 설명 |
|------|------|
| `AVG_OVER_TIME` | 시간에 따른 평균 |
| `COUNT_OVER_TIME` | 시간에 따른 개수 |
| `MAX_OVER_TIME` | 시간에 따른 최대값 |
| `MIN_OVER_TIME` | 시간에 따른 최소값 |
| `SUM_OVER_TIME` | 시간에 따른 합계 |
| `FIRST_OVER_TIME` | 시간 범위의 첫 번째 값 |
| `LAST_OVER_TIME` | 시간 범위의 마지막 값 |
| `DELTA` | 첫 번째와 마지막 값의 차이 |
| `DERIV` | 초당 변화율 |
| `RATE` | 초당 증가율 |

### 4.3 수학 함수 (Mathematical Functions)

| 함수 | 설명 |
|------|------|
| `ABS(n)` | 절대값 |
| `ACOS(n)` | 아크코사인 |
| `ASIN(n)` | 아크사인 |
| `ATAN(n)` | 아크탄젠트 |
| `ATAN2(y, x)` | 두 인수의 아크탄젠트 |
| `CBRT(n)` | 세제곱근 |
| `CEIL(n)` | 올림 (가장 작은 정수 >= n) |
| `COS(n)` | 코사인 |
| `COSH(n)` | 하이퍼볼릭 코사인 |
| `E()` | 오일러 수 (e) |
| `EXP(n)` | e의 n승 |
| `FLOOR(n)` | 내림 (가장 큰 정수 <= n) |
| `HYPOT(a, b)` | 피타고라스 정리 (sqrt(a^2 + b^2)) |
| `LOG(n)` | 자연 로그 |
| `LOG10(n)` | 밑이 10인 로그 |
| `PI()` | 원주율 (π) |
| `POW(base, exp)` | 거듭제곱 |
| `ROUND(n, decimals)` | 반올림 |
| `SIGNUM(n)` | 부호 (-1, 0, 1) |
| `SIN(n)` | 사인 |
| `SINH(n)` | 하이퍼볼릭 사인 |
| `SQRT(n)` | 제곱근 |
| `TAN(n)` | 탄젠트 |
| `TANH(n)` | 하이퍼볼릭 탄젠트 |
| `TAU()` | 타우 (2π) |

예제:
```esql
FROM measurements
| EVAL
    absolute_value = ABS(temperature_diff),
    rounded = ROUND(price, 2),
    ceiling = CEIL(score),
    floor_val = FLOOR(score),
    power = POW(base, exponent),
    square_root = SQRT(area)
| KEEP absolute_value, rounded, ceiling, floor_val, power, square_root
```

### 4.4 문자열 함수 (String Functions)

| 함수 | 설명 |
|------|------|
| `BIT_LENGTH(s)` | 비트 단위 길이 |
| `BYTE_LENGTH(s)` | 바이트 단위 길이 |
| `CONCAT(s1, s2, ...)` | 문자열 연결 |
| `CONTAINS(s, substring)` | 부분 문자열 포함 여부 |
| `ENDS_WITH(s, suffix)` | 접미사로 끝나는지 확인 |
| `FROM_BASE64(s)` | Base64 디코딩 |
| `HASH(algorithm, s)` | 해시 생성 |
| `LEFT(s, length)` | 왼쪽에서 length 문자 |
| `LENGTH(s)` | 문자열 길이 |
| `LOCATE(substring, s)` | 부분 문자열 위치 (1부터 시작) |
| `LTRIM(s)` | 왼쪽 공백 제거 |
| `MD5(s)` | MD5 해시 |
| `REPEAT(s, n)` | 문자열 n번 반복 |
| `REPLACE(s, regex, replacement)` | 정규식 대체 |
| `REVERSE(s)` | 문자열 뒤집기 |
| `RIGHT(s, length)` | 오른쪽에서 length 문자 |
| `RTRIM(s)` | 오른쪽 공백 제거 |
| `SHA1(s)` | SHA1 해시 |
| `SHA256(s)` | SHA256 해시 |
| `SPACE(n)` | n개의 공백 |
| `SPLIT(s, delimiter)` | 구분자로 분할 |
| `STARTS_WITH(s, prefix)` | 접두사로 시작하는지 확인 |
| `SUBSTRING(s, start, length)` | 부분 문자열 추출 |
| `TO_BASE64(s)` | Base64 인코딩 |
| `TO_LOWER(s)` | 소문자로 변환 |
| `TO_UPPER(s)` | 대문자로 변환 |
| `TRIM(s)` | 양쪽 공백 제거 |
| `URL_ENCODE(s)` | URL 인코딩 |
| `URL_DECODE(s)` | URL 디코딩 |

예제:
```esql
FROM users
| EVAL
    full_name = CONCAT(first_name, " ", last_name),
    name_upper = TO_UPPER(first_name),
    email_domain = SUBSTRING(email, LOCATE("@", email) + 1, LENGTH(email)),
    trimmed = TRIM(description),
    has_prefix = STARTS_WITH(username, "admin")
| KEEP full_name, name_upper, email_domain, trimmed, has_prefix
```

### 4.5 날짜/시간 함수 (Date-Time Functions)

| 함수 | 설명 |
|------|------|
| `DATE_DIFF(unit, start, end)` | 두 날짜의 차이 |
| `DATE_EXTRACT(part, date)` | 날짜 부분 추출 |
| `DATE_FORMAT(format, date)` | 날짜 형식화 |
| `DATE_PARSE(format, string)` | 문자열을 날짜로 파싱 |
| `DATE_TRUNC(interval, date)` | 날짜 잘라내기 |
| `DAY_NAME(date)` | 요일 이름 |
| `MONTH_NAME(date)` | 월 이름 |
| `NOW()` | 현재 시간 |
| `TRANGE(from, to)` | 시간 범위 필터 |

예제:
```esql
FROM employees
| EVAL
    year_hired = DATE_TRUNC(1 year, hire_date),
    years_employed = DATE_DIFF("year", hire_date, NOW()),
    formatted_date = DATE_FORMAT("yyyy-MM-dd", hire_date),
    day = DAY_NAME(hire_date)
| KEEP first_name, hire_date, year_hired, years_employed, formatted_date, day

// 날짜 파싱
FROM logs
| EVAL parsed_date = DATE_PARSE("yyyy-MM-dd HH:mm:ss", timestamp_string)
```

### 4.6 조건 함수 (Conditional Functions)

| 함수 | 설명 |
|------|------|
| `CASE(cond1, val1, cond2, val2, ..., default)` | 조건부 값 반환 |
| `COALESCE(v1, v2, ...)` | 첫 번째 non-null 값 |
| `GREATEST(v1, v2, ...)` | 최대값 |
| `LEAST(v1, v2, ...)` | 최소값 |
| `NULLIF(v1, v2)` | v1==v2이면 null, 아니면 v1 |

CASE 함수:
- 조건과 값의 쌍을 받아 첫 번째 true 조건에 해당하는 값을 반환합니다.
- 인수 개수가 홀수이면 마지막 인수가 기본값입니다.
- 인수 개수가 짝수이고 조건이 일치하지 않으면 null을 반환합니다.

예제:
```esql
FROM employees
| EVAL
    salary_tier = CASE(
        salary >= 100000, "High",
        salary >= 50000, "Medium",
        "Low"
    ),
    display_name = COALESCE(nickname, first_name, "Unknown"),
    max_value = GREATEST(score1, score2, score3),
    min_value = LEAST(score1, score2, score3)
| KEEP first_name, salary, salary_tier, display_name, max_value, min_value
```

### 4.7 타입 변환 함수 (Type Conversion Functions)

| 함수 | 설명 |
|------|------|
| `TO_BOOLEAN(v)` | 불리언으로 변환 |
| `TO_DOUBLE(v)` | double로 변환 |
| `TO_INTEGER(v)` | 정수로 변환 |
| `TO_LONG(v)` | long으로 변환 |
| `TO_STRING(v)` | 문자열로 변환 |
| `TO_IP(v)` | IP 주소로 변환 |
| `TO_DATETIME(v)` | 날짜/시간으로 변환 |
| `TO_VERSION(v)` | 버전으로 변환 |
| `TO_GEOPOINT(v)` | 지리적 포인트로 변환 |
| `TO_GEOSHAPE(v)` | 지리적 도형으로 변환 |
| `TO_CARTESIANPOINT(v)` | 데카르트 포인트로 변환 |
| `TO_CARTESIANSHAPE(v)` | 데카르트 도형으로 변환 |
| `TO_DENSE_VECTOR(v)` | 밀집 벡터로 변환 |

타입 변환 구문:
```esql
// 함수 형태
EVAL client_ip = TO_IP(client_ip)

// 축약 구문
EVAL client_ip = client_ip::IP
```

여러 인덱스에서 타입 불일치 처리:
```esql
FROM index1, index2
| EVAL client_ip = TO_IP(client_ip)  // union 타입을 단일 타입으로 변환
| KEEP client_ip, @timestamp
```

### 4.8 다중값 함수 (Multivalue Functions)

다중값 필드를 처리하는 함수:

| 함수 | 설명 |
|------|------|
| `MV_APPEND(v1, v2)` | 두 다중값을 결합 |
| `MV_AVG(mv)` | 다중값의 평균 |
| `MV_CONCAT(mv, delimiter)` | 다중값을 문자열로 연결 |
| `MV_CONTAINS(mv, value)` | 값 포함 여부 |
| `MV_COUNT(mv)` | 다중값의 개수 |
| `MV_DEDUPE(mv)` | 중복 제거 |
| `MV_FIRST(mv)` | 첫 번째 값 |
| `MV_LAST(mv)` | 마지막 값 |
| `MV_MAX(mv)` | 최대값 |
| `MV_MIN(mv)` | 최소값 |
| `MV_MEDIAN(mv)` | 중앙값 |
| `MV_PERCENTILE(mv, p)` | 백분위수 |
| `MV_SORT(mv)` | 정렬 |
| `MV_SLICE(mv, start, end)` | 슬라이스 |
| `MV_SUM(mv)` | 합계 |
| `MV_UNION(mv1, mv2)` | 합집합 |
| `MV_ZIP(mv1, mv2)` | 쌍으로 결합 |

예제:
```esql
ROW a=[3, 5, 1, 6]
| EVAL
    avg_a = MV_AVG(a),
    count_a = MV_COUNT(a),
    max_a = MV_MAX(a),
    min_a = MV_MIN(a)

// 다중값을 문자열로 변환
ROW username=["Dave", "Mike", "Keith", "Ben"]
| EVAL combined = MV_CONCAT(username, ", ")
```

집계 함수와 함께 사용:
```esql
FROM employees
| STATS avg_salary_change = ROUND(AVG(MV_AVG(salary_change)), 10)
```

### 4.9 전문 검색 함수 (Full-Text Search Functions)

#### MATCH

`MATCH` 함수는 지정된 필드에서 매치 쿼리를 수행합니다. Elasticsearch Query DSL의 match 쿼리와 동일합니다.

사용 가능한 필드 타입:
- text, semantic_text 등 text 계열
- keyword, boolean, date, 숫자 타입

구문:
```esql
// 함수 형태
WHERE MATCH(field, "search terms")

// 축약 연산자 형태
WHERE field:"search terms"
```

예제:
```esql
// 기본 매치
FROM library
| WHERE MATCH(title, "elasticsearch guide")
| LIMIT 10

// 옵션 포함
FROM library
| WHERE MATCH(author, "Frank Herbert", {"minimum_should_match": 2, "operator": "AND"})
| LIMIT 5

// 축약 형태
FROM articles
| WHERE content:"machine learning"
| LIMIT 10
```

#### MATCH_PHRASE

`MATCH_PHRASE` 함수는 정확한 구문을 검색합니다.

```esql
FROM articles
| WHERE MATCH_PHRASE(content, "quick brown fox")
| LIMIT 10
```

#### QSTR

`QSTR` 함수는 Query DSL의 query_string 쿼리와 동일한 기능을 제공합니다. 와일드카드, 불리언 로직, 다중 필드 검색 등 고급 검색 패턴을 지원합니다.

```esql
FROM logs
| WHERE QSTR("status:error AND (service:auth* OR service:api*)")
| LIMIT 100
```

관련성 점수:
```esql
// 점수를 얻으려면 METADATA _score를 명시적으로 요청해야 합니다
FROM articles METADATA _score
| WHERE MATCH(content, "elasticsearch")
| SORT _score DESC
| LIMIT 10
```

### 4.10 공간 함수 (Spatial Functions)

#### 거리 및 관계 함수

| 함수 | 설명 |
|------|------|
| `ST_DISTANCE(geom1, geom2)` | 두 지오메트리 간의 거리 |
| `ST_INTERSECTS(geom1, geom2)` | 교차 여부 |
| `ST_DISJOINT(geom1, geom2)` | 분리 여부 (ST_INTERSECTS의 반대) |
| `ST_CONTAINS(geom1, geom2)` | 포함 여부 |
| `ST_WITHIN(geom1, geom2)` | 내부에 있는지 여부 |

#### 지오메트리 속성 함수

| 함수 | 설명 |
|------|------|
| `ST_X(point)` | X 좌표 (경도) |
| `ST_Y(point)` | Y 좌표 (위도) |
| `ST_NPOINTS(geom)` | 포인트 개수 |
| `ST_ENVELOPE(geom)` | 바운딩 박스 |
| `ST_XMAX(geom)` | 최대 X 좌표 |
| `ST_XMIN(geom)` | 최소 X 좌표 |
| `ST_YMAX(geom)` | 최대 Y 좌표 |
| `ST_YMIN(geom)` | 최소 Y 좌표 |

#### 그리드 인코딩 함수

| 함수 | 설명 |
|------|------|
| `ST_GEOHASH(point, precision)` | Geohash 인코딩 |
| `ST_GEOTILE(point, zoom)` | 지도 타일 인코딩 |
| `ST_GEOHEX(point, precision)` | H3 육각형 인코딩 |

예제:
```esql
FROM airports
| WHERE ST_DISTANCE(location, TO_GEOPOINT("POINT(-73.935242 40.730610)")) < 50000
| EVAL distance_km = ST_DISTANCE(location, TO_GEOPOINT("POINT(-73.935242 40.730610)")) / 1000
| KEEP name, city, distance_km
| SORT distance_km
| LIMIT 10
```

### 4.11 IP 함수

| 함수 | 설명 |
|------|------|
| `CIDR_MATCH(ip, cidr_block)` | IP가 CIDR 블록에 포함되는지 확인 |

예제:
```esql
FROM firewall_logs
| WHERE CIDR_MATCH(source_ip, "10.0.0.0/8") OR CIDR_MATCH(source_ip, "192.168.0.0/16")
| STATS count = COUNT(*) BY action
```

### 4.12 연산자 (Operators)

#### 비교 연산자

| 연산자 | 설명 |
|--------|------|
| `==` | 같음 |
| `!=` | 같지 않음 |
| `<` | 보다 작음 |
| `<=` | 보다 작거나 같음 |
| `>` | 보다 큼 |
| `>=` | 보다 크거나 같음 |

#### 논리 연산자

| 연산자 | 설명 |
|--------|------|
| `AND` | 논리 AND |
| `OR` | 논리 OR |
| `NOT` | 논리 NOT |

#### 산술 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | 더하기 |
| `-` | 빼기 |
| `*` | 곱하기 |
| `/` | 나누기 |
| `%` | 나머지 (모듈로) |

#### 기타 연산자

| 연산자 | 설명 |
|--------|------|
| `LIKE` | 패턴 매칭 |
| `RLIKE` | 정규식 매칭 |
| `IN (v1, v2, ...)` | 값 포함 여부 |
| `IS NULL` | null 여부 |
| `IS NOT NULL` | non-null 여부 |

예제:
```esql
FROM employees
| WHERE
    salary > 50000
    AND department IN ("Engineering", "Product")
    AND manager IS NOT NULL
    AND first_name LIKE "J*"
| KEEP first_name, last_name, salary, department
```

---

## 5. ES|QL REST API

### 5.1 API 엔드포인트

ES|QL 쿼리를 실행하려면 `POST /_query` 엔드포인트를 사용합니다.

### 5.2 기본 요청

```json
POST /_query
{
  "query": "FROM employees | WHERE salary > 50000 | LIMIT 10"
}
```

### 5.3 응답 형식

지원되는 형식:
- `json` (기본값)
- `csv`
- `tsv`
- `txt`
- `yaml`
- `cbor`
- `smile`
- `arrow`

형식 지정 방법:
```
POST /_query?format=csv
```

또는 HTTP 헤더 사용:
```
Accept: text/csv
```

### 5.4 컬럼형 결과

기본적으로 ES|QL은 결과를 행으로 반환합니다. json, yaml, cbor, smile 형식의 경우 결과를 컬럼형으로 반환할 수 있습니다.

```json
POST /_query?format=json
{
  "query": "FROM employees | LIMIT 5",
  "columnar": true
}
```

컬럼형 응답 예제:
```json
{
  "columns": [
    {"name": "first_name", "type": "keyword"},
    {"name": "salary", "type": "long"}
  ],
  "values": [
    ["John", "Jane", "Bob", "Alice", "Charlie"],
    [50000, 60000, 55000, 70000, 45000]
  ]
}
```

### 5.5 필터 매개변수

Query DSL 쿼리를 filter 매개변수에 지정하여 ES|QL 쿼리가 실행되는 문서 집합을 필터링할 수 있습니다.

```json
POST /_query
{
  "query": "FROM employees | STATS avg_salary = AVG(salary) BY department",
  "filter": {
    "range": {
      "hire_date": {
        "gte": "2020-01-01"
      }
    }
  }
}
```

### 5.6 프로파일링

`profile` 매개변수를 true로 설정하면 쿼리 실행 방법에 대한 정보가 포함된 추가 프로파일 객체가 포함됩니다.

```json
POST /_query
{
  "query": "FROM employees | WHERE salary > 50000 | LIMIT 10",
  "profile": true
}
```

### 5.7 매개변수

쿼리 매개변수화를 위해 `params` 배열을 사용할 수 있습니다:

```json
POST /_query
{
  "query": "FROM employees | WHERE department == ? AND salary > ?",
  "params": ["Engineering", 50000]
}
```

---

## 6. Kibana에서 ES|QL 사용

### 6.1 Discover에서 ES|QL 시작하기

1. Kibana에서 Discover 로 이동합니다.
2. 애플리케이션 메뉴 바에서 Try ES|QL 을 선택합니다.
3. ES|QL 편집기가 활성화되어 쿼리를 작성할 수 있습니다.

### 6.2 ES|QL 편집기 기능

- 자동 완성: 명령, 함수, 필드 이름에 대한 자동 완성
- 구문 강조: ES|QL 쿼리의 구문 강조
- 인라인 문서: 함수 및 명령에 대한 도움말

### 6.3 시각화

ES|QL 결과를 기반으로 Kibana에서 시각화를 생성할 수 있습니다:
- 테이블
- 막대 차트
- 라인 차트
- 기타 Lens 시각화

### 6.4 제한 사항

- Discover는 최대 10,000개의 행만 표시합니다.
- Discover는 최대 50개의 열만 표시합니다.
- CSV 내보내기는 최대 10,000개의 행으로 제한됩니다.
- ES|QL 모드에서는 필터 바 인터페이스가 비활성화됩니다.

---

## 7. ES|QL 제한 사항

### 7.1 결과 크기 제한

- 쿼리는 LIMIT 명령의 값에 관계없이 10,000개 이상의 행을 반환하지 않습니다.
- 이 제한은 `esql.query.result_truncation_max_size` 설정으로 구성할 수 있습니다.
- 기본 암시적 LIMIT는 1,000입니다.

### 7.2 지원되지 않는 필드 타입

지원되지 않는 타입의 열을 쿼리하면 오류가 반환됩니다. 쿼리에서 명시적으로 사용하지 않는 지원되지 않는 타입의 열은 null 값으로 반환됩니다 (중첩 필드 제외).

지원되지 않거나 제한된 타입:
- `nested`: 전혀 반환되지 않음
- `flattened`: ES|QL에서 지원되지 않음
- 공간 타입: SORT 명령에서 지원되지 않음
- `aggregate_metric_double`: TO_AGGREGATE_METRIC_DOUBLE 함수 사용 필요
- `dense_vector`: 특정 함수 사용 필요

### 7.3 _source 필드 제한

ES|QL은 `_source` 필드가 비활성화된 구성을 지원하지 않습니다.

### 7.4 텍스트 필드 제한

- 전문 검색 함수(MATCH, QSTR) 외의 ES|QL 텍스트 필드 쿼리는 대소문자를 구분합니다.
- 매핑 작업이 ES|QL 쿼리에 적용되지 않아 오탐지 또는 미탐지가 발생할 수 있습니다.
- 모범 사례: 텍스트 필드 대신 keyword 하위 필드를 명시적으로 쿼리하세요.

### 7.5 전문 검색 제한

전문 검색 함수(MATCH)는 FROM 소스 명령 직후 또는 가까운 위치의 WHERE 명령에서 사용해야 합니다. 그렇지 않으면 유효성 검사 오류가 발생합니다.

```esql
// 올바른 사용
FROM articles
| WHERE MATCH(content, "elasticsearch")
| LIMIT 10

// 오류 발생 가능
FROM articles
| EVAL content_length = LENGTH(content)
| WHERE MATCH(content, "elasticsearch")  // 유효성 검사 오류
| LIMIT 10
```

### 7.6 대규모 쿼리 제한

필터 없이 많은 인덱스를 한 번에 쿼리하면 Kibana에서 다음과 같은 오류가 발생할 수 있습니다:
```
[esql] > Unexpected error from Elasticsearch: The content length (536885793) is bigger than the maximum allowed string (536870888).
```

---

## 8. 실전 예제

### 8.1 로그 분석

```esql
// 최근 1시간의 오류 로그 분석
FROM logs-*
| WHERE @timestamp > NOW() - 1 hour AND level == "error"
| STATS error_count = COUNT(*) BY service, error_type
| SORT error_count DESC
| LIMIT 20
```

### 8.2 사용자 활동 분석

```esql
FROM user_activity
| WHERE @timestamp > NOW() - 1 day
| STATS
    page_views = COUNT(*),
    unique_pages = COUNT_DISTINCT(page_url),
    avg_duration = AVG(duration_ms)
  BY user_id
| SORT page_views DESC
| LIMIT 100
```

### 8.3 성능 메트릭 분석

```esql
FROM metrics-*
| WHERE @timestamp > NOW() - 6 hours
| EVAL response_time_ms = response_time / 1000000
| STATS
    avg_response = AVG(response_time_ms),
    p95_response = PERCENTILE(response_time_ms, 95),
    p99_response = PERCENTILE(response_time_ms, 99),
    request_count = COUNT(*)
  BY service_name, endpoint
| WHERE avg_response > 100
| SORT p99_response DESC
```

### 8.4 데이터 보강 및 변환

```esql
FROM orders
| EVAL order_date = DATE_TRUNC(1 day, created_at)
| STATS
    total_revenue = SUM(amount),
    order_count = COUNT(*),
    avg_order_value = AVG(amount)
  BY order_date, region
| ENRICH region-info ON region WITH country, continent
| EVAL revenue_per_order = ROUND(total_revenue / order_count, 2)
| SORT order_date DESC, total_revenue DESC
```

### 8.5 지리적 분석

```esql
FROM stores
| EVAL distance_km = ST_DISTANCE(location, TO_GEOPOINT("POINT(-122.4194 37.7749)")) / 1000
| WHERE distance_km < 50
| STATS
    total_sales = SUM(daily_sales),
    store_count = COUNT(*)
  BY BUCKET(distance_km, 10) AS distance_bucket
| SORT distance_bucket
```

### 8.6 보안 로그 분석

```esql
FROM firewall_logs METADATA _index
| WHERE @timestamp > NOW() - 24 hours
| EVAL
    is_internal = CIDR_MATCH(source_ip, "10.0.0.0/8") OR CIDR_MATCH(source_ip, "192.168.0.0/16"),
    is_blocked = action == "DENY"
| STATS
    blocked_count = COUNT(*) WHERE is_blocked,
    total_count = COUNT(*)
  BY source_ip, is_internal
| EVAL block_rate = ROUND(blocked_count * 100.0 / total_count, 2)
| WHERE block_rate > 50
| SORT blocked_count DESC
| LIMIT 50
```

### 8.7 텍스트 로그 파싱

```esql
FROM raw_logs
| GROK message "%{IP:client_ip} - %{USER:user} \\[%{HTTPDATE:timestamp}\\] \"%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:http_version}\" %{NUMBER:status} %{NUMBER:bytes}"
| EVAL
    status_code = TO_INTEGER(status),
    response_bytes = TO_INTEGER(bytes)
| STATS
    request_count = COUNT(*),
    total_bytes = SUM(response_bytes),
    error_count = COUNT(*) WHERE status_code >= 400
  BY method, LEFT(request, 50) AS endpoint_prefix
| SORT request_count DESC
```

### 8.8 다중값 필드 처리

```esql
FROM products
| MV_EXPAND tags
| STATS
    product_count = COUNT(*),
    avg_price = AVG(price)
  BY tags
| SORT product_count DESC
| LIMIT 20
```

---

## 9. 참고 자료

- [ES|QL 레퍼런스](https://www.elastic.co/docs/reference/query-languages/esql)
- [ES|QL 구문 레퍼런스](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-syntax.html)
- [ES|QL 명령](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-commands.html)
- [ES|QL 함수 및 연산자](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-functions-operators.html)
- [ES|QL REST API](https://www.elastic.co/docs/reference/query-languages/esql/esql-rest)
- [ES|QL 제한 사항](https://www.elastic.co/docs/reference/query-languages/esql/limitations)
- [Kibana에서 ES|QL 사용하기](https://www.elastic.co/docs/explore-analyze/query-filter/languages/esql-kibana)
- [ES|QL 전문 검색](https://www.elastic.co/search-labs/blog/filtering-in-esql-full-text-search-match-qstr-kql)
- [ES|QL LOOKUP JOIN](https://www.elastic.co/docs/reference/query-languages/esql/esql-lookup-join)
- [ES|QL 시계열 지원](https://www.elastic.co/search-labs/blog/esql-elasticsearch-9-2-multi-field-joins-ts-command)
