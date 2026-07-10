# Druid 네이티브 쿼리

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

## 목차

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

## 네이티브 쿼리 개요

Druid의 네이티브 쿼리는 JSON 객체로 표현하며, Broker 또는 Router 프로세스에 제출합니다. 네이티브 쿼리는 비교적 저수준 API로, Druid 내부에서 연산이 수행되는 방식과 밀접하게 대응합니다. Druid는 가볍고 빠른 쿼리에 맞게 설계되었으므로, 복잡한 분석은 여러 쿼리를 순차적으로 조합해야 할 때가 많습니다.

### 쿼리 제출

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

### 쿼리 타입

| 분류 | 쿼리 타입 |
| --- | --- |
| 집계(aggregation) 쿼리 | Timeseries, TopN, GroupBy |
| 메타데이터(metadata) 쿼리 | TimeBoundary, SegmentMetadata, DatasourceMetadata |
| 기타 쿼리 | Scan, Search |

여러 쿼리 타입이 모두 요구 사항을 충족한다면, 각 용도에 맞게 최적화된 Timeseries나 TopN을 우선 사용하기를 권장합니다. 둘 다 적합하지 않을 때는 가장 유연한 GroupBy 쿼리를 사용합니다.

### 쿼리 취소

쿼리 제출 시 지정한 `queryId`로 실행 중인 쿼리를 취소할 수 있습니다.

```
DELETE /druid/v2/{queryId}
```

```bash
curl -X DELETE "http://host:port/druid/v2/abc123"
```

### 오류 응답

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

## Timeseries 쿼리

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

### 속성

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

### Grand total

쿼리 컨텍스트에 `"grandTotal": true`를 추가하면 전체 합계 행을 함께 반환합니다. Grand total 행은 결과의 마지막에 타임스탬프 없이 추가되며, 후처리 집계도 grand total 값을 기준으로 계산됩니다.

### 빈 버킷 처리

Druid는 기본적으로 결과 내부의 빈 시간 버킷을 해당 집계 함수의 기본값으로 채웁니다(zero-filling). 예를 들어 `longSum`이라면 0으로 채웁니다. 빈 버킷을 결과에서 제외하려면 쿼리 컨텍스트에 `"skipEmptyBuckets": "true"`를 지정합니다.

---

## TopN 쿼리

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

### 속성

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

### 응답 예시

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

### 다중 값 디멘션에서의 동작

TopN 쿼리는 다중 값(multi-value) 디멘션 기준 그룹핑을 지원합니다. 다중 값 디멘션으로 그룹핑하면 매칭된 행의 모든 값이 각각 하나의 그룹을 만들기 때문에, 결과 그룹 수가 입력 행 수보다 많아질 수 있습니다. 필터 조건에 매칭되는 값만 남기고 성능도 개선하려면 filtered dimensionSpec을 적용합니다.

### 근사(approximation) 동작과 정확도

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

## GroupBy 쿼리

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

### 속성

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

### 다중 값 디멘션에서의 동작

다중 값 디멘션으로 그룹핑하면 매칭된 행의 모든 값이 값별로 하나씩 그룹을 만들어, 결과 그룹이 행 수보다 많아질 수 있습니다. filtered dimensionSpec으로 필터에 매칭되는 값만 남기면 결과를 제한하면서 성능도 개선할 수 있습니다.

### subtotalsSpec

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

### 메모리와 디스크 스필

GroupBy 쿼리의 자원 사용은 다음 파라미터로 제어합니다.

- `druid.processing.buffer.sizeBytes`: 쿼리당 off-heap 해시 테이블 크기(바이트)
- `druid.query.groupBy.maxSelectorDictionarySize`: 세그먼트별 on-heap 딕셔너리 한도
- `druid.query.groupBy.maxMergingDictionarySize`: 쿼리별 on-heap 딕셔너리 한도
- `druid.query.groupBy.maxOnDiskStorage`: 쿼리별 디스크 스필(spill) 한도(바이트, 0이면 비활성)
- `druid.query.groupBy.maxSpillFileCount`: 실패 전까지 허용하는 최대 스필 파일 수

`maxOnDiskStorage`가 0보다 크면, 메모리 한도에 도달했을 때 부분 집계된 레코드를 정렬해 디스크로 내려씁니다.

### 성능 튜닝

- **Limit pushdown**: 가능한 경우 groupBy 쿼리의 limitSpec을 Historical의 세그먼트까지 내려보내 불필요한 중간 결과를 조기에 잘라냅니다. orderBy 필드가 그룹핑 키의 부분집합이면 기본으로 적용되며, `forceLimitPushDown` 컨텍스트 플래그로 제어합니다.
- **해시 테이블**: open addressing과 선형 탐사(linear probing)를 사용합니다. 초기 버킷 1024개, 최대 load factor 0.7이 기본이며, `bufferGrouperInitialBuckets`와 `bufferGrouperMaxLoadFactor`로 조정합니다.
- **Parallel combine**: 정렬된 집계 결과 병합에 처리 스레드를 추가로 사용하는 기능으로, 기본은 비활성입니다. `numParallelCombineThreads`로 제어하며, 데이터 스필이 필요하고 Historical 쿼리당 병합 버퍼 2개를 사용합니다.

### 대안 쿼리

- **Timeseries**: 시간 기준으로만 그룹핑한다면 완전 스트리밍으로 동작하는 Timeseries가 더 낫습니다.
- **TopN**: 단일 디멘션 그룹핑, 특히 metric 기준 정렬에는 TopN이 더 빠릅니다.

### 중첩 groupBy

중첩 groupBy(데이터소스 타입이 `query`)는 Broker가 내부 groupBy 쿼리를 먼저 일반적인 방식으로 실행하고, 그 결과 스트림 위에서 외부 쿼리를 실행합니다. 이때 off-heap fact map과 디스크로 스필 가능한 on-heap 문자열 딕셔너리를 사용합니다.

### 런타임 설정 속성

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

### 배열 기반 결과 형식

컨텍스트에서 `resultAsArray`를 true로 지정하면 각 행이 위치 기반 배열로 반환됩니다. 타임스탬프(선택), 디멘션, aggregator, post-aggregator 순서입니다. 이 스키마는 응답에 포함되지 않으므로 발행한 쿼리로부터 직접 계산해야 합니다.

---

## Scan 쿼리

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

### 속성

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

### 결과 형식

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

### 시간 정렬

Scan 쿼리는 타임스탬프 기준 정렬을 지원하지만 제약이 있습니다. 시간 정렬을 사용하면 세그먼트 ID가 null로 표시되며, 결과 limit이 `druid.query.scan.maxRowsQueuedForOrdering`보다 작거나, 스캔하는 모든 세그먼트의 파티션 수가 `druid.query.scan.maxSegmentPartitionsOrderedInMemory`보다 적어야 합니다.

시간 정렬에는 두 가지 전략을 사용합니다.

1. **Priority queue**: 세그먼트를 순차적으로 열면서 타임스탬프 기준으로 크기가 제한된 우선순위 큐를 유지합니다.
2. **N-way merge**: 파티션별 압축 해제 버퍼를 병렬로 열고, 파티션별로 미리 정렬된 결과를 병합합니다.

### 설정 속성

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

## Search 쿼리

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

### 속성

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

### 응답 예시

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

### 정렬 방식

`sort`에는 `lexicographic`(기본값), `alphanumeric`, `strlen`, `numeric`을 지정할 수 있습니다.

### 실행 전략

쿼리 컨텍스트의 `searchStrategy`로 실행 전략을 선택합니다.

- **useIndexes**(기본값): 검색 디멘션을 비트맵 인덱스 지원 여부에 따라 두 그룹으로 나눈 뒤, 인덱스 기반 실행 계획과 커서 기반 실행 계획을 각각 적용합니다.
- **cursorOnly**: 커서 기반 실행 계획만 생성합니다. 행을 직접 읽으며 조건식을 평가하므로, 선택도(selectivity)가 낮은 필터에서 유리할 수 있습니다.

### SearchQuerySpec 종류

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

## 필터

필터는 SQL의 WHERE 절에 해당하며, 어떤 행을 포함할지 지정하는 JSON 객체입니다. 기본적으로 3값 불리언 논리(three-value Boolean logic)를 따릅니다.

### selector 필터

가장 단순한 필터로, 특정 디멘션 값과의 일치를 검사합니다.

```json
{ "type": "selector", "dimension": "someColumn", "value": "hello" }
```

### equality 필터

selector를 대체하는 최신 필터로, 모든 컬럼 타입을 지원하며 null과는 매칭되지 않습니다.

```json
{ "type": "equals", "column": "someColumn", "matchValueType": "STRING", "matchValue": "hello" }
```

### null 필터

NULL 값 매칭 전용 필터입니다.

```json
{ "type": "null", "column": "someColumn" }
```

### bound 필터

사전순(lexicographic) 또는 숫자(numeric) 순서 기반 범위 필터입니다.

```json
{ "type": "bound", "dimension": "age", "lower": "21", "upper": "31", "ordering": "numeric" }
```

### range 필터

bound 필터를 대체하는 SQL 호환 범위 필터로, null과는 매칭되지 않습니다.

```json
{ "type": "range", "column": "age", "matchValueType": "LONG", "lower": 21, "upper": 31 }
```

### like 필터

SQL LIKE처럼 `%`와 `_` 와일드카드를 지원합니다.

```json
{ "type": "like", "dimension": "last_name", "pattern": "D%" }
```

### regex 필터

Java 정규식 패턴과 디멘션 값을 매칭합니다.

```json
{ "type": "regex", "dimension": "someColumn", "pattern": "^50.*" }
```

### in 필터

문자열 집합에 포함되는 값을 매칭합니다.

```json
{ "type": "in", "dimension": "outlaw", "values": ["Good", "Bad", "Ugly"] }
```

### arrayContainsElement 필터

배열 컬럼이 특정 원소를 포함하는지 검사합니다.

```json
{ "type": "arrayContainsElement", "column": "someArrayColumn", "elementMatchValueType": "STRING", "elementMatchValue": "hello" }
```

### 논리 필터(and / or / not)

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

### interval 필터

long 밀리초 값을 담은 컬럼에 ISO-8601 interval 기반 범위 필터를 적용합니다.

```json
{ "type": "interval", "dimension": "__time", "intervals": [
  "2014-10-01T00:00:00.000Z/2014-10-07T00:00:00.000Z"
] }
```

### search 필터

부분 문자열 매칭 필터로, 대소문자 구분 옵션을 지원합니다.

```json
{ "type": "search", "dimension": "product", "query": {
  "type": "insensitive_contains", "value": "foo"
} }
```

### expression 필터

Druid 표현식(expression) 시스템으로 임의의 조건을 기술합니다.

```json
{ "type": "expression", "expression": "((product_type == 42) && (!is_deleted))" }
```

### javascript 필터

JavaScript 함수를 조건식으로 사용합니다. JavaScript 기능은 기본적으로 비활성화되어 있습니다.

```json
{ "type": "javascript", "dimension": "name", "function": "function(x) { return(x >= 'bar' && x <= 'foo') }" }
```

### columnComparison 필터

디멘션끼리 비교합니다. 비교 시 값을 문자열로 변환합니다.

```json
{ "type": "columnComparison", "dimensions": ["someColumn", { "type": "default", "dimension": "someLongColumn" }] }
```

### true / false 필터

`true` 필터는 모든 값과 매칭되며 필터를 임시로 무력화할 때 사용하고, `false` 필터는 아무 값과도 매칭되지 않아 빈 결과를 강제할 때 사용합니다.

```json
{ "type": "true" }
```

```json
{ "type": "false" }
```

### 타임스탬프 컬럼 필터링

타임스탬프 컬럼은 디멘션 이름 `__time`으로 참조합니다. long 밀리초 값 비교, 포맷 변환을 위한 extraction 함수, ISO-8601 interval을 지원합니다.

```json
{ "type": "equals", "dimension": "__time", "matchValueType": "LONG", "value": 124457387532 }
```

### extraction 함수와 함께 필터링

spatial 필터를 제외한 모든 필터는 extraction 함수를 지원하며, 필터링 전에 디멘션 값에 적용됩니다. 별도의 extraction 필터는 deprecated이므로, extraction 함수를 지정한 selector 필터를 사용합니다.

```json
{ "type": "selector", "dimension": "product", "value": "bar_1", "extractionFn": {
  "type": "lookup", "lookup": { "type": "map", "map": { "product_1": "bar_1", "product_5": "bar_1" } }
} }
```

### 참고 사항

- SQL 플래너는 `sqlUseBoundAndSelectors`를 활성화하지 않는 한 selector·bound 필터 대신 equality·null·range 필터를 사용합니다.
- 다중 값 문자열 컬럼은 값 중 하나라도 필터를 만족하면 매칭됩니다.
- 숫자 컬럼에 문자열 매칭 값을 지정하면 자동으로 형 변환합니다.

---

## 집계

집계(aggregation)는 데이터 인제스천(ingestion) 시점에 롤업의 일부로 지정할 수도 있고, 쿼리 시점에 지정할 수도 있습니다.

### count

Druid 행 수를 셉니다.

```json
{ "type": "count", "name": "count" }
```

`count`는 Druid 행 수를 세는 것이므로, 인제스천 시 롤업 설정에 따라 원본 이벤트 수와 다를 수 있습니다.

### sum 계열

| 타입 | 설명 |
| --- | --- |
| `longSum` | 64비트 부호 있는 정수 합 |
| `doubleSum` | 64비트 부동소수점 합 |
| `floatSum` | 32비트 부동소수점 합 |

```json
{ "type": "longSum", "name": "sumLong", "fieldName": "aLong" }
```

sum 계열 aggregator는 `fieldName` 또는 `expression` 중 하나를 지정해야 합니다.

### min / max 계열

`doubleMin`, `doubleMax`, `floatMin`, `floatMax`, `longMin`, `longMax` 여섯 가지가 있습니다.

```json
{ "type": "doubleMin", "name": "maxDouble", "fieldName": "aDouble" }
```

### first / last 계열

숫자형은 `doubleFirst`, `doubleLast`, `floatFirst`, `floatLast`, `longFirst`, `longLast`를 지원합니다.

```json
{ "type": "doubleFirst", "name": "firstDouble", "fieldName": "aDouble" }
```

문자열형은 `stringFirst`, `stringLast`를 지원하며 `maxStringBytes`(기본값 1024)를 지정할 수 있습니다.

```json
{ "type": "stringFirst", "name": "firstString", "fieldName": "aString", "maxStringBytes": 2048 }
```

롤업이 적용된 세그먼트에 first/last aggregator를 사용하면 롤업된 값을 반환할 뿐, 인제스천된 원본 데이터의 첫/마지막 값을 반환하지 않습니다.

### any 계열

만난 값 중 아무 값이나 반환하며, 쿼리 시점에서만 사용할 수 있습니다. 숫자형은 `doubleAny`, `floatAny`, `longAny`, 문자열형은 `stringAny`이며, `stringAny`는 `aggregateMultipleValues` 플래그(기본값 true)를 지원합니다.

```json
{ "type": "stringAny", "name": "anyString", "fieldName": "aString", "maxStringBytes": 2048 }
```

### doubleMean

산술 평균을 계산하며, 쿼리 시점에서만 사용할 수 있습니다.

```json
{ "type": "doubleMean", "name": "aMean", "fieldName": "aDouble" }
```

### 근사 집계

**Count distinct**

- DataSketches Theta Sketch: 합집합·교집합·차집합 등 집합 연산 지원
- DataSketches HLL Sketch: 메모리 사용량이 더 적고 집합 연산 미지원
- Cardinality / HyperUnique: 내장 레거시 구현. DataSketches 계열 사용을 권장

**분위수(quantile)·히스토그램**

- DataSketches Quantiles Sketch: 공식적인 오차 범위를 제공하므로 권장
- Moments Sketch: 실험적. 병합 속도에 최적화되어 있으나 정확도가 분포에 의존
- Fixed Buckets Histogram: 성능이 데이터에 의존
- Approximate Histogram: 분포에 따라 왜곡이 발생해 deprecated

### expression aggregator

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

### javascript aggregator

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

### filtered aggregator

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

### grouping aggregator

groupBy 쿼리의 `subtotalsSpec`과 함께 사용하며, 각 행이 어떤 디멘션 조합으로 그룹핑되었는지 비트로 인코딩해 반환합니다. 부분 그룹핑에서 제외된 디멘션의 비트가 1이 됩니다.

```json
{ "type": "grouping", "name": "someGrouping", "groupings": ["dim1", "dim2"] }
```

---

## 그래뉼래리티

그래뉼래리티(granularity)는 데이터를 시간 단위로 버킷팅하는 방법을 결정합니다. simple, duration, period 세 가지 방식으로 지정합니다.

### Simple 그래뉼래리티

문자열로 지정하는 사전 정의된 시간 버킷이며, UTC 기준입니다.

```
all, none, second, minute, five_minute, ten_minute, fifteen_minute,
thirty_minute, hour, six_hour, eight_hour, day, week, month, quarter, year
```

- `all`: 모든 데이터를 하나의 버킷으로 집계합니다.
- `none`: 밀리초 단위(내부 인덱스 해상도와 동일)로 버킷팅합니다.

Timeseries 쿼리에서는 `none`을 사용하지 않아야 합니다. 모든 밀리초마다 결과를 만들고 내부의 빈 버킷을 0으로 채우기 때문입니다.

groupBy 쿼리를 `hour` 그래뉼래리티로 실행하면 시간 단위 버킷별 결과가, `day`로 실행하면 일 단위 버킷별 결과가 반환됩니다. groupBy에서는 빈 버킷을 모두 버립니다.

### Duration 그래뉼래리티

밀리초 단위 길이로 지정하며, 기준점(`origin`)을 선택적으로 지정합니다. 다음은 2시간 버킷입니다.

```json
{ "type": "duration", "duration": 7200000 }
```

`origin`을 지정하면 해당 시각부터 버킷을 나눕니다. 다음은 매시 30분을 기준으로 1시간 단위로 자르는 예시입니다.

```json
{ "type": "duration", "duration": 3600000, "origin": "2012-01-01T00:30:00Z" }
```

### Period 그래뉼래리티

ISO-8601 기간 형식으로 지정하며, 시간대(`timeZone`)와 기준점(`origin`)을 선택적으로 지정합니다.

```json
{ "type": "period", "period": "P2D", "timeZone": "America/Los_Angeles" }
```

```json
{ "type": "period", "period": "P3M", "timeZone": "America/Los_Angeles", "origin": "2012-02-01T00:00:00-08:00" }
```

시간대 지원은 Joda Time 라이브러리가 제공하며, 표준 IANA 시간대를 사용합니다.

---

## 쿼리 컨텍스트

쿼리 컨텍스트는 쿼리 계획과 실행 방식을 제어하는 파라미터 모음입니다.

### 컨텍스트 지정 방법

- **네이티브 쿼리**: 쿼리 JSON 안에 `context` 객체로 포함합니다.
- **웹 콘솔**: Query 뷰에서 Edit query context를 열어 JSON 파라미터를 추가합니다.
- **JDBC 드라이버**: 데이터베이스 연결 시 프로퍼티로 지정합니다.
- **SQL SET 문**: `SET sqlTimeZone = 'America/Los_Angeles';`처럼 지정합니다. 단, JDBC 연결에서는 SET 문을 사용할 수 없습니다.
- **런타임 프로퍼티**: `druid.query.default.context.{PARAMETER}={VALUE}` 형식으로 기본값을 지정합니다.

우선순위는 낮은 순서부터 내장 기본값 → 런타임 프로퍼티 → Broker 동적 설정 → HTTP 요청의 context 객체 → SET 문입니다.

### 일반 파라미터

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

### 쿼리 타입별 파라미터

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

### 벡터화 파라미터

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `vectorize` | `true` | 벡터화 실행 제어. `false`, `true`, `force` 지정 가능 |
| `vectorSize` | 512 | 벡터화 쿼리 처리 시 행 배치 크기 |
| `vectorizeVirtualColumns` | `true` | 가상 컬럼의 벡터화 활성화 여부 |
