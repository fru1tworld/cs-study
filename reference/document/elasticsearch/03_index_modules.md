# 인덱스 모듈 (Index Modules)

> 이 문서는 Elasticsearch 9.x 공식 문서의 Index Modules 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html

## 목차

1. [개요](#개요)
2. [정적 인덱스 설정](#정적-인덱스-설정)
3. [동적 인덱스 설정](#동적-인덱스-설정)
4. [설정 적용 및 관리](#설정-적용-및-관리)

---

## 개요

인덱스 모듈은 인덱스별로 생성되는 모듈로, 인덱스와 관련된 모든 측면을 제어합니다. 이 문서에서는 특정 인덱스 모듈에 속하지 않는 일반적인 인덱스 설정에 대해 다룹니다.

인덱스 수준 설정은 두 가지 유형으로 나뉩니다:

- 정적(Static) 설정: 인덱스 생성 시점이나 닫힌 인덱스에서만 설정할 수 있습니다.
- 동적(Dynamic) 설정: 업데이트 인덱스 설정 API를 사용하여 실시간 인덱스에서 변경할 수 있습니다.

> 주의: 닫힌 인덱스에서 정적 또는 동적 인덱스 설정을 변경하면 잘못된 설정이 적용될 수 있으며, 이 경우 인덱스를 삭제하고 다시 생성하지 않으면 인덱스를 다시 열 수 없게 될 수 있습니다.

---

## 정적 인덱스 설정

정적 인덱스 설정은 인덱스 생성 시점에만 설정할 수 있으며, 닫힌 인덱스에서는 수정할 수 없습니다.

### index.number_of_shards

인덱스가 가져야 하는 기본 샤드의 수입니다.

- 기본값: `1`
- 제한사항: 인덱스 생성 시점에만 설정할 수 있습니다. 닫힌 인덱스에서는 변경할 수 없습니다.
- 최대값: 인덱스당 샤드 수는 `1024`개로 제한됩니다.

> 참고: 샤드 수가 인덱스당 1024개로 제한되는 것은 리소스 할당으로 인해 클러스터를 불안정하게 만들 수 있는 인덱스의 우발적인 생성을 방지하기 위한 안전 제한입니다. 이 제한은 `export ES_JAVA_OPTS="-Des.index.max_number_of_shards=128"` 시스템 속성을 지정하여 클러스터의 일부인 모든 노드에서 수정할 수 있습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.number_of_shards": 3
  }
}
```

### index.number_of_routing_shards

`index.number_of_shards`와 함께 문서를 기본 샤드로 라우팅하는 데 사용되는 정수 값입니다.

- 목적: 인덱스 분할을 가능하게 합니다
- 기본값: 기본 샤드 수에 따라 달라지며, 2의 인수로 분할하여 최대 1024개의 샤드까지 분할할 수 있도록 설계되었습니다

Elasticsearch는 분할 연산에서 문서를 인덱스 전체에 분배하기 위해 이 값을 사용합니다. 예를 들어, `number_of_routing_shards`가 30(`5 x 2 x 3`)으로 설정된 5개의 샤드 인덱스는 2 또는 3의 인수로 분할될 수 있습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.number_of_shards": 5,
    "index.number_of_routing_shards": 30
  }
}
```

> 참고: 커스텀 라우팅을 사용하는 오래된 인덱스를 리인덱싱하는 경우, 이전과 동일한 문서 분포를 유지하려면 동일한 `index.number_of_routing_shards` 값을 사용해야 합니다.

### index.codec

저장된 데이터의 압축 방법입니다.

| 값 | 설명 |
|----|------|
| `default` | LZ4 압축을 사용합니다 |
| `best_compression` | ZSTD 압축을 사용하며, `default`에 비해 최대 약 28% 낮은 저장 공간 사용량과 유사한 인덱싱 처리량을 제공하지만, 저장된 필드 성능에 영향을 미칩니다 |

`best_compression`을 사용하면 저장된 필드 성능이 느려집니다. 자주 사용되는 저장된 필드의 경우 id로 가져오기 지연 시간에서 10~33% 차이가 있고, 적중 수가 많은 검색 지연 시간에서 약 10% 차이가 있습니다. 압축 유형을 업데이트해도 세그먼트가 병합될 때까지 기존 세그먼트에는 영향을 미치지 않습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.codec": "best_compression"
  }
}
```

### index.mode

인덱스 모드를 통해 특정 도메인에 적합한 설정을 제어합니다.

| 값 | 설명 |
|----|------|
| `null` | 표준 인덱싱이며 기본 동작입니다 |
| `standard` | 기본 설정으로 표준 인덱싱 |
| `lookup` | ES\|QL에서 LOOKUP JOIN을 위해 특별히 조정된 설정으로, 샤드 수가 1개로 제한됩니다 |
| `time_series` | 데이터 스트림에서 메트릭 저장에 최적화된 설정 |
| `logsdb` | 데이터 스트림에서 로그에 최적화된 설정 |

```json
PUT /my-index-000001
{
  "settings": {
    "index.mode": "time_series"
  }
}
```

### index.routing_partition_size

커스텀 라우팅 값이 지정될 수 있는 샤드의 수입니다.

- 기본값: `1`
- 제한사항: 인덱스 생성 시점에만 설정할 수 있습니다
- 조건: `index.number_of_routing_shards`보다 작아야 합니다(둘 다 1인 경우 제외)

이 설정은 `_routing` 필드와 함께 사용하여 적중할 가능성이 있는 샤드의 수를 줄이면서 불균형한 클러스터의 위험을 줄이는 데 도움이 됩니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.routing_partition_size": 3
  }
}
```

### index.soft_deletes.enabled

> Deprecated in 7.6.0: 소프트 삭제 생성은 8.0에서 제거되었으며, 이 설정은 더 이상 사용할 수 없습니다. 소프트 삭제 비활성화를 지원해야 하는 인덱스에 쓰기를 허용하지 않으려면 8.0으로 업그레이드하기 전에 데이터 마이그레이션을 수행하세요.

인덱스에서 소프트 삭제가 활성화되었는지 여부를 나타냅니다.

- 기본값: `true`
- 제한사항: Elasticsearch 6.5.0 이후에 생성된 인덱스에서만 인덱스 생성 시점에 구성할 수 있습니다

### index.soft_deletes.retention_lease.period

만료되기 전에 샤드 히스토리 보존 리스를 유지하는 최대 기간입니다.

- 기본값: `12h`

샤드 히스토리 보존 리스는 소프트 삭제가 Lucene 인덱스에서 병합되는 동안에도 보존되도록 보장합니다. 병합 전에 소프트 삭제가 복제되면 복구 소스로서 히스토리를 사용하여 피어 복구를 완료할 수 있습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.soft_deletes.retention_lease.period": "24h"
  }
}
```

### index.load_fixed_bitset_filters_eagerly

중첩 쿼리에 대해 캐시된 필터가 미리 로드되는지 여부를 나타냅니다.

- 기본값: `true`
- 옵션: `true` 또는 `false`

```json
PUT /my-index-000001
{
  "settings": {
    "index.load_fixed_bitset_filters_eagerly": false
  }
}
```

### index.shard.check_on_startup

샤드를 열기 전에 손상 여부를 확인합니다.

| 값 | 설명 |
|----|------|
| `false` | 샤드를 열 때 손상 검사를 수행하지 않습니다(기본값) |
| `checksum` | 샤드의 모든 파일에 대해 체크섬을 확인합니다 |
| `true` | 파일에 대해 체크섬 확인과 논리적 일관성 검사를 모두 수행합니다. 또한 Lucene 인덱스의 논리적 무결성을 확인합니다 |

> 경고: 전문 사용자 전용입니다. 이 설정은 샤드 시작 시 매우 비용이 많이 드는 처리를 활성화하며, 데이터가 손상된 샤드를 진단하기 위해 일시적으로만 사용해야 합니다. 완료되면 반드시 `false`로 다시 설정해야 합니다.

손상이 감지되면 `check_on_startup` 옵션에 관계없이 샤드를 열 수 없습니다. 손상에 대한 응답으로 진행하는 방법에 대해서는 인덱스 문제 해결을 참조하세요.

```json
PUT /my-index-000001
{
  "settings": {
    "index.shard.check_on_startup": "checksum"
  }
}
```

---

## 동적 인덱스 설정

동적 인덱스 설정은 업데이트 인덱스 설정 API를 사용하여 실시간 인덱스에서 변경할 수 있습니다.

### index.number_of_replicas

각 기본 샤드가 가지는 복제본의 수입니다.

- 기본값: `1`

> 경고: 이 값을 0으로 설정하면 노드 재시작 중 일시적인 가용성 손실이 발생하거나 데이터 손상의 경우 영구적인 데이터 손실이 발생할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.number_of_replicas": 2
}
```

### index.auto_expand_replicas

클러스터의 데이터 노드 수에 따라 복제본 수를 자동으로 확장합니다.

- 기본값: `false` (비활성화됨)
- 형식: 대시로 구분된 하한 및 상한(예: `0-5`) 또는 상한에 `all` 사용(예: `0-all`)

> 참고: 자동 확장된 복제본 수는 할당 필터링 규칙만 고려하고, 노드당 총 샤드와 같은 다른 할당 규칙은 무시합니다. 적용 가능한 규칙이 너무 많은 샤드를 할당하지 못하게 하면 이로 인해 클러스터 상태가 YELLOW가 될 수 있습니다.

상한이 `all`이면 샤드 할당 인식 및 클러스터당 노드당 동일한 샤드 할당은 자동 확장 결정 중에 무시됩니다.

```json
PUT /my-index-000001/_settings
{
  "index.auto_expand_replicas": "0-5"
}
```

### index.search.idle.after

검색 또는 가져오기 요청을 받지 않은 샤드가 검색 유휴 상태로 간주되기 전까지 대기하는 시간입니다.

- 기본값: `30s`

```json
PUT /my-index-000001/_settings
{
  "index.search.idle.after": "60s"
}
```

### index.refresh_interval

최근 인덱스 변경 사항을 검색에 표시하도록 새로 고침 작업을 수행하는 빈도입니다.

- 기본값: Elasticsearch에서 `1s`, Serverless에서 `5s` (Serverless에서는 최소값도 `5s`)
- 특수 값: `-1`은 새로 고침을 비활성화합니다

`index.search.idle.after` 초 동안 검색 트래픽이 없으면 샤드는 검색 요청을 받을 때까지 백그라운드 새로 고침을 받지 않습니다. 검색 요청을 기다리는 유휴 샤드에 도달하는 검색은 다음 백그라운드 새로 고침을 기다리며(1초 이내), 이는 검색 유휴 샤드에서 검색 대기 시간이 다소 증가하는 것처럼 보일 수 있습니다. 이 동작은 검색에 완전한 응답성이 필요한 인덱스에서 새로 고침이 발생하도록 명시적으로 `index.refresh_interval`을 설정하여 방지할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.refresh_interval": "30s"
}
```

### index.max_result_window

이 인덱스에 대한 검색의 `from + size` 최대값입니다.

- 기본값: `10000`

검색 요청은 `from + size`에 비례하는 힙 메모리와 시간을 차지합니다. 이 제한은 해당 메모리를 제한하기 위해 존재합니다. 더 깊은 페이징에 대한 효율성을 위해 Scroll 또는 Search After를 참조하세요.

```json
PUT /my-index-000001/_settings
{
  "index.max_result_window": 20000
}
```

### index.max_inner_result_window

이 인덱스에 대한 내부 적중 정의 및 상위 적중 집계의 `from + size` 최대값입니다.

- 기본값: `100`

내부 적중 및 상위 적중 집계는 `from + size`에 비례하는 힙 메모리와 시간을 차지합니다. 이 제한은 해당 메모리를 제한하기 위해 존재합니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_inner_result_window": 200
}
```

### index.max_rescore_window

이 인덱스에 대한 검색에서 재점수 요청의 `window_size` 최대값입니다.

- 기본값: `index.max_result_window`와 동일 (기본적으로 10000)

검색 요청은 `max(window_size, from + size)`에 비례하는 힙 메모리와 시간을 차지합니다. 이 제한은 해당 메모리를 제한하기 위해 존재합니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_rescore_window": 5000
}
```

### index.max_docvalue_fields_search

쿼리에서 허용되는 `docvalue_fields`의 최대 수입니다.

- 기본값: `100`

Doc-value 필드는 필드별, 문서별 탐색을 발생시킬 수 있으므로 비용이 많이 듭니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_docvalue_fields_search": 150
}
```

### index.max_script_fields

쿼리에서 허용되는 `script_fields`의 최대 수입니다.

- 기본값: `32`

```json
PUT /my-index-000001/_settings
{
  "index.max_script_fields": 64
}
```

### index.max_ngram_diff

NGramTokenizer 및 NGramTokenFilter에 대해 `min_gram`과 `max_gram` 간의 최대 허용 차이입니다.

- 기본값: `1`

```json
PUT /my-index-000001/_settings
{
  "index.max_ngram_diff": 2
}
```

### index.max_shingle_diff

shingle 토큰 필터에 대해 `max_shingle_size`와 `min_shingle_size` 간의 최대 허용 차이입니다.

- 기본값: `3`

```json
PUT /my-index-000001/_settings
{
  "index.max_shingle_diff": 5
}
```

### index.max_refresh_listeners

인덱스의 각 샤드에서 사용할 수 있는 새로 고침 리스너의 최대 수입니다.

이 리스너들은 `refresh=wait_for`를 구현하는 데 사용됩니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_refresh_listeners": 100
}
```

### index.analyze.max_token_count

_analyze API를 사용하여 생성할 수 있는 최대 토큰 수입니다.

- 기본값: `10000`

```json
PUT /my-index-000001/_settings
{
  "index.analyze.max_token_count": 20000
}
```

### index.highlight.max_analyzed_offset

오프셋이나 용어 벡터 없이 인덱싱된 텍스트에 대한 하이라이트 요청에서 분석할 최대 문자 수입니다.

- 기본값: `1000000`

```json
PUT /my-index-000001/_settings
{
  "index.highlight.max_analyzed_offset": 500000
}
```

### index.max_terms_count

Terms Query에서 사용할 수 있는 최대 용어 수입니다.

- 기본값: `65536`

```json
PUT /my-index-000001/_settings
{
  "index.max_terms_count": 100000
}
```

### index.max_regex_length

Regexp 쿼리 또는 prefix 쿼리에서 `regexp` 값의 최대 길이입니다.

- 기본값: `1000`

```json
PUT /my-index-000001/_settings
{
  "index.max_regex_length": 2000
}
```

### index.query.default_field

특정 쿼리 유형에 대해 기본적으로 검색할 필드와 일치하는 와일드카드(`*`) 패턴입니다.

- 기본값: `*` (메타데이터를 제외하고 용어 수준 쿼리에 적합한 모든 필드와 일치)
- 타입: 문자열 또는 문자열 배열
- 적용 대상: More like this, Multi-match, Query string, Simple query string 쿼리

```json
PUT /my-index-000001/_settings
{
  "index.query.default_field": ["title", "body"]
}
```

### index.routing.allocation.enable

이 인덱스에 대한 샤드 할당을 제어합니다.

| 값 | 설명 |
|----|------|
| `all` | 모든 샤드에 대해 샤드 할당을 허용합니다(기본값) |
| `primaries` | 기본 샤드에 대해서만 샤드 할당을 허용합니다 |
| `new_primaries` | 새로 생성된 기본 샤드에 대해서만 샤드 할당을 허용합니다 |
| `none` | 샤드 할당을 허용하지 않습니다 |

```json
PUT /my-index-000001/_settings
{
  "index.routing.allocation.enable": "primaries"
}
```

### index.routing.rebalance.enable

이 인덱스에 대한 샤드 재균형을 활성화합니다.

| 값 | 설명 |
|----|------|
| `all` | 모든 샤드에 대해 샤드 재균형을 허용합니다(기본값) |
| `primaries` | 기본 샤드에 대해서만 샤드 재균형을 허용합니다 |
| `replicas` | 복제본 샤드에 대해서만 샤드 재균형을 허용합니다 |
| `none` | 샤드 재균형을 허용하지 않습니다 |

```json
PUT /my-index-000001/_settings
{
  "index.routing.rebalance.enable": "none"
}
```

### index.gc_deletes

삭제된 문서의 버전 번호가 추가 버전 관리 작업에 사용할 수 있도록 유지되는 기간입니다.

- 기본값: `60s`

```json
PUT /my-index-000001/_settings
{
  "index.gc_deletes": "120s"
}
```

### index.default_pipeline

이 인덱스에 대한 기본 인제스트 파이프라인입니다.

- 인덱스 요청에 `pipeline` 파라미터가 지정되지 않은 경우 사용됩니다
- 기본 파이프라인이 존재하지 않으면 인덱스 요청이 실패합니다
- 요청별로 `pipeline` 파라미터를 사용하여 재정의할 수 있습니다
- 특수 값: `_none`은 기본 파이프라인이 없음을 나타냅니다

```json
PUT /my-index-000001/_settings
{
  "index.default_pipeline": "my-pipeline"
}
```

### index.final_pipeline

이 인덱스에 대한 최종 인제스트 파이프라인입니다.

- 요청 파이프라인과 기본 파이프라인 후에 항상 실행됩니다
- 최종 파이프라인이 존재하지 않으면 인덱스 요청이 실패합니다
- 특수 값: `_none`은 최종 파이프라인이 없음을 나타냅니다

> 주의: 최종 파이프라인을 사용하여 `_index` 필드를 변경할 수 없습니다. 파이프라인이 `_index` 필드를 변경하려고 하면 인덱싱 요청이 실패합니다.

```json
PUT /my-index-000001/_settings
{
  "index.final_pipeline": "my-final-pipeline"
}
```

### index.hidden

인덱스가 와일드카드 표현식에서 기본적으로 숨겨져야 하는지 여부를 나타냅니다.

- 기본값: `false`
- 옵션: `true` 또는 `false`

요청별로 `expand_wildcards` 파라미터를 사용하여 이 동작을 제어할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.hidden": true
}
```

### index.dense_vector.hnsw_filter_heuristic

HNSW 그래프 벡터에 대해 필터링된 검색을 실행하는 데 사용할 휴리스틱입니다.

| 값 | 설명 |
|----|------|
| `acorn` | 쿼리 필터 기준과 일치하는 벡터만 검색합니다. 일반적으로 더 빠르며 정확한 결과를 보장하기 위해 더 높은 `num_candidates`가 필요할 수 있습니다(기본값) |
| `fanout` | 모든 벡터를 쿼리 벡터와 비교합니다. 느릴 수 있지만 더 높은 재현율을 제공할 수 있습니다 |

```json
PUT /my-index-000001/_settings
{
  "index.dense_vector.hnsw_filter_heuristic": "fanout"
}
```

### index.esql.stored_fields_sequential_proportion

ES|QL이 저장된 필드를 로드하는 전략에 대한 튜닝 파라미터입니다.

- 범위: 0.0 ~ 1.0
- 기본값: `0.2`

문서가 10KB보다 작은 인덱스는 이 값을 낮춰 텍스트 필드 로딩을 개선할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.esql.stored_fields_sequential_proportion": 0.1
}
```

### index.dense_vector.hnsw_early_termination

HNSW 그래프에 대한 knn 쿼리에 인내 기반 조기 종료 전략을 적용할지 여부입니다.

- 기본값: Elasticsearch 9.3으로 생성된 인덱스의 경우 `true`, 이전 버전의 경우 `false`
- 적용 대상: hnsw, int8_hnsw, int4_hnsw, bbq_hnsw 인덱스 유형을 가진 `dense_vector` 필드

```json
PUT /my-index-000001/_settings
{
  "index.dense_vector.hnsw_early_termination": true
}
```

### index.use_time_series_doc_values_format

시계열 doc values 형식을 사용해야 하는지 여부를 나타냅니다.

- 기본값: `index.mode`가 `time_series` 또는 `logsdb`인 경우 `true`, 그렇지 않으면 `false`

```json
PUT /my-index-000001/_settings
{
  "index.use_time_series_doc_values_format": true
}
```

---

## 설정 적용 및 관리

### 인덱스 생성 시 설정

인덱스를 생성할 때 정적 및 동적 설정을 모두 지정할 수 있습니다:

```json
PUT /my-index-000001
{
  "settings": {
    "index.number_of_shards": 3,
    "index.number_of_replicas": 2,
    "index.refresh_interval": "30s",
    "index.codec": "best_compression"
  }
}
```

### 동적 설정 업데이트

실시간 인덱스에서 동적 설정을 업데이트하려면:

```json
PUT /my-index-000001/_settings
{
  "index.number_of_replicas": 1,
  "index.refresh_interval": "10s"
}
```

### 현재 설정 조회

인덱스의 현재 설정을 조회하려면:

```json
GET /my-index-000001/_settings
```

특정 설정만 조회하려면:

```json
GET /my-index-000001/_settings/index.number_of_replicas
```

### 기본값으로 재설정

동적 설정을 기본값으로 재설정하려면 `null`을 사용합니다:

```json
PUT /my-index-000001/_settings
{
  "index.refresh_interval": null
}
```

---

## 요약

Elasticsearch 인덱스 모듈은 인덱스의 동작을 세밀하게 제어할 수 있는 다양한 설정을 제공합니다:

1. 정적 설정 은 인덱스 생성 시에만 설정할 수 있으며, 샤드 수, 압축 코덱, 인덱스 모드 등을 포함합니다
2. 동적 설정 은 실시간으로 변경할 수 있으며, 복제본 수, 새로 고침 간격, 검색 제한 등을 포함합니다
3. 적절한 설정을 통해 성능, 저장 공간, 가용성 간의 균형을 맞출 수 있습니다
4. `number_of_replicas`를 0으로 설정하면 데이터 손실 위험이 있으므로 주의해야 합니다

---

## 참고 자료

- [Elasticsearch Index Modules 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html)
- [Index Settings API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-update-settings.html)
- [Create Index API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html)
