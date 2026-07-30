# Elasticsearch 인덱스 모듈과 인덱스 템플릿

## 인덱스 모듈 (Index Modules)

> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html

### 목차

1. [개요](#개요)
2. [정적 인덱스 설정](#정적-인덱스-설정)
3. [동적 인덱스 설정](#동적-인덱스-설정)
4. [설정 적용 및 관리](#설정-적용-및-관리)

---

### 개요

인덱스 모듈은 인덱스별로 생성되며, 인덱스와 관련된 모든 측면을 제어합니다. 여기서는 특정 인덱스 모듈에 속하지 않는 일반적인 인덱스 설정을 다룹니다.

인덱스 수준 설정은 두 가지 유형으로 나뉩니다:

- 정적(Static) 설정: 인덱스 생성 시점이나 닫힌 인덱스에서만 설정할 수 있습니다.
- 동적(Dynamic) 설정: 업데이트 인덱스 설정 API를 사용하여 실시간 인덱스에서 변경할 수 있습니다.

> 주의: 닫힌 인덱스에서 정적 또는 동적 인덱스 설정을 변경하면 잘못된 설정이 적용될 수 있으며, 이 경우 인덱스를 삭제하고 다시 생성하지 않으면 인덱스를 다시 열 수 없게 될 수 있습니다.

---

### 정적 인덱스 설정

정적 인덱스 설정은 인덱스 생성 시점 또는 닫힌 인덱스에서만 설정할 수 있습니다.

#### index.number_of_shards

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

#### index.number_of_routing_shards

`index.number_of_shards`와 함께 문서를 기본 샤드로 라우팅하는 데 사용되는 정수 값입니다.

- 목적: 인덱스 분할을 가능하게 합니다
- 기본값: 기본 샤드 수에 따라 달라지며, 2의 인수로 분할하여 최대 1024개의 샤드까지 분할할 수 있도록 설계되었습니다

Elasticsearch는 분할 연산 시 이 값을 사용해 문서를 인덱스 전체에 분배합니다. 예를 들어, `number_of_routing_shards`가 30(`5 x 2 x 3`)으로 설정된 5개 샤드 인덱스는 2 또는 3의 인수로 분할할 수 있습니다.

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

#### index.codec

저장된 데이터의 압축 방법입니다.

| 값 | 설명 |
|----|------|
| `default` | LZ4 압축을 사용합니다 |
| `best_compression` | ZSTD 압축을 사용하며, `default`에 비해 최대 약 28% 낮은 저장 공간 사용량과 유사한 인덱싱 처리량을 제공하지만, 저장된 필드 성능에 영향을 미칩니다 |

`best_compression`을 사용하면 저장된 필드 조회 성능이 저하됩니다. 자주 사용되는 저장 필드의 경우 ID 기반 가져오기 지연 시간이 10~33%, 다수의 적중이 발생하는 검색 지연 시간이 약 10% 늘어날 수 있습니다. 압축 유형을 변경해도 세그먼트가 병합될 때까지 기존 세그먼트에는 영향을 미치지 않습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.codec": "best_compression"
  }
}
```

#### index.mode

인덱스 모드를 통해 특정 도메인에 적합한 설정을 제어합니다.

| 값 | 설명 |
|----|------|
| `null` | 표준 인덱싱이며 기본 동작입니다 |
| `standard` | 기본 설정으로 표준 인덱싱 |
| `lookup` | ES\|QL에서 LOOKUP JOIN을 위해 특별히 조정된 설정으로, 샤드 수가 1개로 제한됩니다 |
| `time_series` | 데이터 스트림에서 메트릭 저장에 최적화된 설정 |
| `logsdb` | 데이터 스트림에서 로그에 최적화된 설정 |
| `vectordb_document` | 벡터 검색 사용 사례에 최적화된 설정 |

```json
PUT /my-index-000001
{
  "settings": {
    "index.mode": "time_series"
  }
}
```

#### index.routing_partition_size

커스텀 라우팅 값이 지정될 수 있는 샤드의 수입니다.

- 기본값: `1`
- 제한사항: 인덱스 생성 시점에만 설정할 수 있습니다
- 조건: `index.number_of_routing_shards`보다 작아야 합니다(둘 다 1인 경우 제외)

이 설정은 `_routing` 필드와 함께 사용하여, 탐색 대상 샤드 수를 줄이면서도 불균형한 클러스터 위험을 완화하는 데 도움이 됩니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.routing_partition_size": 3
  }
}
```

#### index.soft_deletes.enabled

> Deprecated in 7.6.0: 이 설정은 7.6.0부터 사용 중단(deprecated)되었습니다.

인덱스에서 소프트 삭제가 활성화되었는지 여부를 나타냅니다.

- 기본값: `true`
- 제한사항: Elasticsearch 6.5.0 이후에 생성된 인덱스에서만 인덱스 생성 시점에 구성할 수 있습니다

#### index.soft_deletes.retention_lease.period

만료되기 전에 샤드 히스토리 보존 리스를 유지하는 최대 기간입니다.

- 기본값: `12h`

샤드 히스토리 보존 리스는 소프트 삭제가 Lucene 인덱스에서 병합되는 중에도 보존되도록 보장합니다. 병합 전에 소프트 삭제가 복제되면 히스토리를 복구 소스로 활용하여 피어 복구를 완료할 수 있습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.soft_deletes.retention_lease.period": "24h"
  }
}
```

#### index.load_fixed_bitset_filters_eagerly

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

#### index.shard.check_on_startup

샤드를 열기 전에 손상 여부를 확인합니다.

| 값 | 설명 |
|----|------|
| `false` | 샤드를 열 때 손상 검사를 수행하지 않습니다(기본값) |
| `checksum` | 샤드의 모든 파일에 대해 체크섬을 확인합니다 |
| `true` | 파일에 대해 체크섬 확인과 논리적 일관성 검사를 모두 수행합니다. 또한 Lucene 인덱스의 논리적 무결성을 확인합니다 |

> 경고: 전문 사용자 전용입니다. 이 설정은 샤드 시작 시 매우 비용이 많이 드는 처리를 활성화하며, 데이터가 손상된 샤드를 진단하기 위해 일시적으로만 사용해야 합니다. 완료되면 반드시 `false`로 다시 설정해야 합니다.

손상이 감지되면 `check_on_startup` 설정값과 관계없이 샤드를 열 수 없습니다. 손상 발생 시 대응 방법은 인덱스 문제 해결 문서를 참조하세요.

```json
PUT /my-index-000001
{
  "settings": {
    "index.shard.check_on_startup": "checksum"
  }
}
```

---

### 동적 인덱스 설정

동적 인덱스 설정은 업데이트 인덱스 설정 API를 사용하여 실시간 인덱스에서 변경할 수 있습니다.

#### index.number_of_replicas

각 기본 샤드가 가지는 복제본의 수입니다.

- 기본값: `1`

> 경고: 이 값을 0으로 설정하면 노드 재시작 중 일시적인 가용성 손실이 발생하거나 데이터 손상의 경우 영구적인 데이터 손실이 발생할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.number_of_replicas": 2
}
```

#### index.auto_expand_replicas

클러스터의 데이터 노드 수에 따라 복제본 수를 자동으로 확장합니다.

- 기본값: `false` (비활성화됨)
- 형식: 대시로 구분된 하한 및 상한(예: `0-5`) 또는 상한에 `all` 사용(예: `0-all`)

> 참고: 자동 확장된 복제본 수는 할당 필터링 규칙만 고려하고, 노드당 총 샤드와 같은 다른 할당 규칙은 무시합니다. 적용 가능한 규칙이 너무 많은 샤드를 할당하지 못하게 하면 이로 인해 클러스터 상태가 YELLOW가 될 수 있습니다.

상한이 `all`이면 샤드 할당 인식 및 노드당 동일한 샤드 할당 규칙은 자동 확장 결정 시 무시됩니다.

```json
PUT /my-index-000001/_settings
{
  "index.auto_expand_replicas": "0-5"
}
```

#### index.search.idle.after

검색 또는 가져오기 요청을 받지 않은 샤드가 검색 유휴 상태로 간주되기 전까지 대기하는 시간입니다.

- 기본값: `30s`

```json
PUT /my-index-000001/_settings
{
  "index.search.idle.after": "60s"
}
```

#### index.refresh_interval

최근 인덱스 변경 사항을 검색에 표시하도록 새로 고침 작업을 수행하는 빈도입니다.

- 기본값: Elasticsearch에서 `1s`, Serverless에서 `5s` (Serverless에서는 최소값도 `5s`)
- 특수 값: `-1`은 새로 고침을 비활성화합니다

`index.search.idle.after`에 설정된 시간 동안 검색 트래픽이 없으면, 샤드는 검색 요청을 받을 때까지 백그라운드 새로 고침을 수행하지 않습니다. 유휴 상태의 샤드에 검색 요청이 도달하면 다음 백그라운드 새로 고침(1초 이내)을 기다리므로, 검색 지연 시간이 다소 늘어나는 것처럼 보일 수 있습니다. 검색 응답성이 중요한 인덱스에서는 `index.refresh_interval`을 명시적으로 설정하여 이 동작을 방지할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.refresh_interval": "30s"
}
```

#### index.max_result_window

이 인덱스에 대한 검색의 `from + size` 최대값입니다.

- 기본값: `10000`

검색 요청은 `from + size`에 비례하는 힙 메모리와 시간을 사용합니다. 이 제한은 해당 메모리 사용을 억제하기 위해 존재합니다. 깊은 페이징이 필요한 경우 Scroll 또는 Search After를 사용하세요.

```json
PUT /my-index-000001/_settings
{
  "index.max_result_window": 20000
}
```

#### index.max_inner_result_window

이 인덱스에 대한 내부 적중 정의 및 상위 적중 집계의 `from + size` 최대값입니다.

- 기본값: `100`

내부 적중 및 상위 적중 집계는 `from + size`에 비례하는 힙 메모리와 시간을 사용합니다. 이 제한은 해당 메모리 사용을 억제하기 위해 존재합니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_inner_result_window": 200
}
```

#### index.max_rescore_window

이 인덱스에 대한 검색에서 재점수 요청의 `window_size` 최대값입니다.

- 기본값: `index.max_result_window`와 동일 (기본적으로 10000)

검색 요청은 `max(window_size, from + size)`에 비례하는 힙 메모리와 시간을 사용합니다. 이 제한은 해당 메모리 사용을 억제하기 위해 존재합니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_rescore_window": 5000
}
```

#### index.max_docvalue_fields_search

쿼리에서 허용되는 `docvalue_fields`의 최대 수입니다.

- 기본값: `100`

Doc-value 필드는 필드 및 문서별 탐색을 유발할 수 있어 비용이 많이 듭니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_docvalue_fields_search": 150
}
```

#### index.max_script_fields

쿼리에서 허용되는 `script_fields`의 최대 수입니다.

- 기본값: `32`

```json
PUT /my-index-000001/_settings
{
  "index.max_script_fields": 64
}
```

#### index.max_ngram_diff

NGramTokenizer 및 NGramTokenFilter에 대해 `min_gram`과 `max_gram` 간의 최대 허용 차이입니다.

- 기본값: `1`

```json
PUT /my-index-000001/_settings
{
  "index.max_ngram_diff": 2
}
```

#### index.max_shingle_diff

shingle 토큰 필터에 대해 `max_shingle_size`와 `min_shingle_size` 간의 최대 허용 차이입니다.

- 기본값: `3`

```json
PUT /my-index-000001/_settings
{
  "index.max_shingle_diff": 5
}
```

#### index.max_refresh_listeners

인덱스의 각 샤드에서 사용할 수 있는 새로 고침 리스너의 최대 수입니다.

이 리스너들은 `refresh=wait_for`를 구현하는 데 사용됩니다.

```json
PUT /my-index-000001/_settings
{
  "index.max_refresh_listeners": 100
}
```

#### index.analyze.max_token_count

_analyze API를 사용하여 생성할 수 있는 최대 토큰 수입니다.

- 기본값: `10000`

```json
PUT /my-index-000001/_settings
{
  "index.analyze.max_token_count": 20000
}
```

#### index.highlight.max_analyzed_offset

오프셋이나 용어 벡터 없이 인덱싱된 텍스트에 대한 하이라이트 요청에서 분석할 최대 문자 수입니다.

- 기본값: `1000000`

```json
PUT /my-index-000001/_settings
{
  "index.highlight.max_analyzed_offset": 500000
}
```

#### index.max_terms_count

Terms Query에서 사용할 수 있는 최대 용어 수입니다.

- 기본값: `65536`

```json
PUT /my-index-000001/_settings
{
  "index.max_terms_count": 100000
}
```

#### index.max_regex_length

Regexp 쿼리 또는 prefix 쿼리에서 `regexp` 값의 최대 길이입니다.

- 기본값: `1000`

```json
PUT /my-index-000001/_settings
{
  "index.max_regex_length": 2000
}
```

#### index.query.default_field

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

#### index.routing.allocation.enable

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

#### index.routing.rebalance.enable

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

#### index.gc_deletes

삭제된 문서의 버전 번호가 추가 버전 관리 작업에 사용할 수 있도록 유지되는 기간입니다.

- 기본값: `60s`

```json
PUT /my-index-000001/_settings
{
  "index.gc_deletes": "120s"
}
```

#### index.default_pipeline

이 인덱스에 대한 기본 인제스트 파이프라인입니다.

- 인덱스 요청에 `pipeline` 파라미터가 지정되지 않은 경우 적용됩니다
- 지정된 기본 파이프라인이 존재하지 않으면 인덱스 요청이 실패합니다
- 요청별로 `pipeline` 파라미터를 지정하면 재정의할 수 있습니다
- 특수 값: `_none`은 기본 파이프라인을 사용하지 않음을 의미합니다

```json
PUT /my-index-000001/_settings
{
  "index.default_pipeline": "my-pipeline"
}
```

#### index.final_pipeline

이 인덱스에 대한 최종 인제스트 파이프라인입니다.

- 요청 파이프라인 및 기본 파이프라인 다음에 항상 실행됩니다
- 지정된 최종 파이프라인이 존재하지 않으면 인덱스 요청이 실패합니다
- 특수 값: `_none`은 최종 파이프라인을 사용하지 않음을 의미합니다

> 주의: 최종 파이프라인을 사용하여 `_index` 필드를 변경할 수 없습니다. 파이프라인이 `_index` 필드를 변경하려고 하면 인덱싱 요청이 실패합니다.

```json
PUT /my-index-000001/_settings
{
  "index.final_pipeline": "my-final-pipeline"
}
```

#### index.hidden

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

#### index.dense_vector.hnsw_filter_heuristic

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

#### index.esql.stored_fields_sequential_proportion

ES|QL이 저장된 필드를 로드하는 전략에 대한 튜닝 파라미터입니다.

- 범위: 0.0 ~ 1.0
- 기본값: `0.2`

문서 크기가 10KB 미만인 인덱스는 이 값을 낮추면 텍스트 필드 로딩 성능을 개선할 수 있습니다.

```json
PUT /my-index-000001/_settings
{
  "index.esql.stored_fields_sequential_proportion": 0.1
}
```

#### index.dense_vector.hnsw_early_termination

HNSW 그래프에 대한 knn 쿼리에 인내 기반 조기 종료 전략을 적용할지 여부입니다.

- 기본값: Elasticsearch 9.3으로 생성된 인덱스의 경우 `true`, 이전 버전의 경우 `false`
- 적용 대상: hnsw, int8_hnsw, int4_hnsw, bbq_hnsw 인덱스 유형을 가진 `dense_vector` 필드

```json
PUT /my-index-000001/_settings
{
  "index.dense_vector.hnsw_early_termination": true
}
```

#### index.use_time_series_doc_values_format

시계열 doc values 형식을 사용해야 하는지 여부를 나타냅니다.

- 기본값: `index.mode`가 `time_series` 또는 `logsdb`인 경우 `true`, 그렇지 않으면 `false`

```json
PUT /my-index-000001/_settings
{
  "index.use_time_series_doc_values_format": true
}
```

---

### 설정 적용 및 관리

#### 인덱스 생성 시 설정

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

#### 동적 설정 업데이트

실시간 인덱스에서 동적 설정을 업데이트하려면:

```json
PUT /my-index-000001/_settings
{
  "index.number_of_replicas": 1,
  "index.refresh_interval": "10s"
}
```

#### 현재 설정 조회

인덱스의 현재 설정을 조회하려면:

```json
GET /my-index-000001/_settings
```

특정 설정만 조회하려면:

```json
GET /my-index-000001/_settings/index.number_of_replicas
```

#### 기본값으로 재설정

동적 설정을 기본값으로 재설정하려면 `null`을 사용합니다:

```json
PUT /my-index-000001/_settings
{
  "index.refresh_interval": null
}
```

---

### 요약

Elasticsearch 인덱스 모듈은 인덱스의 동작을 세밀하게 제어할 수 있는 다양한 설정을 제공합니다:

1. 정적 설정은 인덱스 생성 시 또는 닫힌 인덱스에서만 설정할 수 있으며, 샤드 수, 압축 코덱, 인덱스 모드 등이 포함됩니다
2. 동적 설정은 실시간으로 변경할 수 있으며, 복제본 수, 새로 고침 간격, 검색 제한 등이 포함됩니다
3. 적절한 설정을 통해 성능, 저장 공간, 가용성 간의 균형을 맞출 수 있습니다
4. `number_of_replicas`를 0으로 설정하면 데이터 손실 위험이 있으므로 주의해야 합니다

---

### 참고 자료

- [Elasticsearch Index Modules 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html)
- [Index Settings API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-update-settings.html)
- [Create Index API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html)

---

## 인덱스 템플릿 (Index Templates)

> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html

### 목차

1. [개요](#개요)
2. [템플릿 유형](#템플릿-유형)
3. [인덱스 템플릿](#인덱스-템플릿)
4. [컴포넌트 템플릿](#컴포넌트-템플릿)
5. [템플릿 우선순위와 병합](#템플릿-우선순위와-병합)
6. [내장 인덱스 템플릿과 충돌 방지](#내장-인덱스-템플릿과-충돌-방지)
7. [인덱스 템플릿 API](#인덱스-템플릿-api)
8. [컴포넌트 템플릿 API](#컴포넌트-템플릿-api)
9. [템플릿 시뮬레이션](#템플릿-시뮬레이션)
10. [데이터 스트림용 템플릿](#데이터-스트림용-템플릿)
11. [모범 사례](#모범-사례)

---

### 개요

템플릿은 인덱스나 데이터 스트림을 생성할 때 Elasticsearch가 설정, 매핑 및 기타 구성을 적용하는 메커니즘입니다. 구성은 인덱스 생성 전에 정의되며, 인덱스가 수동으로 생성되거나 문서 인덱싱으로 생성될 때 일치하는 템플릿이 적용할 설정과 매핑을 결정합니다.

#### 템플릿의 주요 역할

- 설정 (Settings): 샤드 수, 레플리카 수, 리프레시 간격 등 인덱스 설정 정의
- 매핑 (Mappings): 필드 타입, 분석기, 동적 매핑 규칙 등 정의
- 별칭 (Aliases): 인덱스에 대한 별칭 정의
- 수명 주기 (Lifecycle): 데이터 스트림의 수명 주기 관리 설정

#### 적용 조건

템플릿 사용 시 다음 조건이 적용됩니다:

- 컴포저블 인덱스 템플릿은 레거시 템플릿(Elasticsearch 7.8에서 deprecated)을 대체합니다. 레거시 템플릿은 컴포저블 템플릿이 일치하지 않을 때만 적용됩니다.
- 인덱스 생성 요청에서 명시적으로 지정된 설정은 인덱스 템플릿과 컴포넌트 템플릿의 설정보다 우선합니다.
- 인덱스 템플릿 설정은 컴포넌트 템플릿 설정보다 우선합니다.
- 여러 템플릿이 일치할 경우, 우선순위가 가장 높은 템플릿이 적용됩니다.
- 내장 Elasticsearch 인덱스 템플릿과 이름 패턴이 충돌하지 않도록 주의해야 합니다.

---

### 템플릿 유형

Elasticsearch는 두 가지 유형의 템플릿을 제공합니다:

#### 인덱스 템플릿 (Index Templates)

인덱스 템플릿은 인덱스나 데이터 스트림을 생성할 때 적용되는 주요 구성 객체입니다.

- `index_patterns`를 사용하여 인덱스 이름과 매칭
- `priority` 값을 통해 충돌 해결
- 설정, 매핑, 별칭을 직접 정의하거나 컴포넌트 템플릿을 참조할 수 있음
- 데이터 스트림 또는 일반 인덱스 생성 여부 지정

#### 컴포넌트 템플릿 (Component Templates)

컴포넌트 템플릿은 설정, 매핑, 별칭을 정의하는 재사용 가능한 빌딩 블록입니다.

- 직접 적용되지 않음: 인덱스 템플릿에서 참조되어야만 적용됨
- 재사용 가능: 여러 인덱스 템플릿에서 공유 가능
- 모듈화: 매핑, 설정, 별칭을 별도로 관리 가능

인덱스 템플릿과 참조된 컴포넌트 템플릿을 함께 컴포저블 템플릿(composable templates) 이라고 합니다.

---

### 인덱스 템플릿

인덱스 템플릿은 인덱스 생성 시 적용되는 구성을 정의합니다. 인덱스 템플릿에 지정된 매핑, 설정, 별칭은 생성되는 각 인덱스에 적용됩니다. 이 값들은 인덱스 템플릿이 참조하는 컴포넌트 템플릿에서 올 수도 있습니다.

#### 인덱스 템플릿 관리

인덱스 템플릿은 다음을 통해 생성하고 관리할 수 있습니다:
- Kibana의 Index Management 페이지
- 인덱스 템플릿 API

#### 기본 인덱스 템플릿 예제

```json
PUT _index_template/my-index-template
{
  "index_patterns": ["my-index-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "_source": {
        "enabled": true
      },
      "properties": {
        "host_name": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        }
      }
    },
    "aliases": {
      "my-alias": {}
    }
  },
  "priority": 200,
  "version": 1,
  "_meta": {
    "description": "내 인덱스용 템플릿"
  }
}
```

#### 컴포넌트 템플릿을 참조하는 인덱스 템플릿 예제

```json
PUT _index_template/template_1
{
  "index_patterns": ["te*", "bar*"],
  "template": {
    "settings": {
      "number_of_shards": 1
    },
    "mappings": {
      "_source": {
        "enabled": true
      },
      "properties": {
        "host_name": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        }
      }
    },
    "aliases": {
      "mydata": {}
    }
  },
  "priority": 501,
  "composed_of": ["component_template1", "runtime_component_template"],
  "version": 3,
  "_meta": {
    "description": "내 사용자 정의 템플릿"
  }
}
```

#### 인덱스 템플릿 파라미터

##### 필수 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `index_patterns` | string 또는 string[] | 데이터 스트림 및 인덱스 이름을 매칭하는 와일드카드(`*`) 표현식 배열 |

##### 선택적 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `template` | object | 별칭, 매핑, 설정을 포함하는 템플릿 구성 |
| `composed_of` | string[] | 병합할 컴포넌트 템플릿 이름의 순서가 있는 목록 |
| `priority` | number | 여러 템플릿이 매칭될 때 우선순위. 기본값은 0 (가장 낮은 우선순위) |
| `version` | number | 외부 버전 관리를 위한 버전 번호 |
| `_meta` | object | 클러스터 상태에 저장되는 사용자 정의 메타데이터 |
| `data_stream` | object | 데이터 스트림을 생성하려면 이 파라미터 포함 (빈 객체도 가능) |
| `allow_auto_create` | boolean | `action.auto_create_index` 클러스터 설정 재정의 |
| `ignore_missing_component_templates` | string[] | 누락된 컴포넌트 템플릿 참조 처리 |
| `deprecated` | boolean | 템플릿을 deprecated로 표시 |

##### template 객체 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `aliases` | object | 인덱스 별칭 구성 (filter, routing, is_write_index 등) |
| `mappings` | object | 필드 매핑 정의 (properties, dynamic, _source 등) |
| `settings` | object | 인덱스 설정 (샤드 수, 레플리카 수 등) |
| `lifecycle` | object | 데이터 스트림 수명 주기 (data_retention, downsampling) - 8.11.0+ |

---

### 컴포넌트 템플릿

컴포넌트 템플릿은 설정, 매핑, 별칭을 정의하는 재사용 가능한 빌딩 블록입니다. 컴포넌트 템플릿은 인덱스에 직접 적용되지 않으며, 인덱스 템플릿에서 참조되어야만 적용됩니다.

#### 컴포넌트 템플릿 관리

컴포넌트 템플릿은 다음을 통해 생성하고 관리할 수 있습니다:
- Kibana의 Index Management 페이지
- 컴포넌트 템플릿 API

#### 내장 컴포넌트 템플릿

Elasticsearch에는 다음과 같은 내장 컴포넌트 템플릿이 포함되어 있습니다:

- `logs-mappings`
- `logs-settings`
- `metrics-mappings`
- `metrics-settings`
- `synthetics-mapping`
- `synthetics-settings`

#### 기본 컴포넌트 템플릿 예제

```json
PUT _component_template/component_template1
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        }
      }
    }
  }
}
```

#### 런타임 필드를 포함한 컴포넌트 템플릿 예제

```json
PUT _component_template/runtime_component_template
{
  "template": {
    "mappings": {
      "runtime": {
        "day_of_week": {
          "type": "keyword",
          "script": {
            "source": "emit(doc['@timestamp'].value.dayOfWeekEnum.getDisplayName(TextStyle.FULL, Locale.ENGLISH))"
          }
        }
      }
    }
  }
}
```

#### 설정용 컴포넌트 템플릿 예제

```json
PUT _component_template/my-settings
{
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1,
      "index.lifecycle.name": "my-lifecycle-policy"
    }
  },
  "_meta": {
    "description": "설정용 컴포넌트 템플릿"
  }
}
```

#### 별칭용 컴포넌트 템플릿 예제

```json
PUT _component_template/my-aliases
{
  "template": {
    "aliases": {
      "my-alias": {
        "filter": {
          "term": {
            "status": "active"
          }
        },
        "routing": "1"
      }
    }
  }
}
```

#### 컴포넌트 템플릿 파라미터

##### 필수 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `template` | object | 별칭, 매핑, 설정을 포함하는 템플릿 구성 |

##### 선택적 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `version` | number | 외부 버전 관리를 위한 버전 번호 |
| `_meta` | object | 클러스터 상태에 저장되는 사용자 정의 메타데이터 |
| `deprecated` | boolean | 템플릿을 deprecated로 표시 |

##### template.aliases 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `filter` | object | 문서 접근을 제한하는 쿼리 |
| `index_routing` | string | 인덱싱 작업에 사용할 라우팅 값 |
| `search_routing` | string | 검색 작업에 사용할 라우팅 값 |
| `routing` | string | 인덱스 및 검색 작업 모두에 사용할 라우팅 값 |
| `is_hidden` | boolean | 별칭 숨김 여부 (기본값: false) |
| `is_write_index` | boolean | 쓰기 인덱스 지정 여부 (기본값: false) |

##### template.mappings 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `properties` | object | 필드 이름과 데이터 타입 정의 |
| `dynamic` | string | 동적 매핑 동작 (`strict`, `runtime`, `true`, `false`) |
| `date_detection` | boolean | 자동 날짜 필드 감지 |
| `numeric_detection` | boolean | 자동 숫자 필드 감지 |
| `_source` | object | 소스 필드 저장 제어 (enabled, includes, excludes) |
| `_routing` | object | 필수 라우팅 필드 지정 |
| `runtime` | object | 런타임 필드 정의 |
| `enabled` | boolean | 매핑 활성화/비활성화 |
| `subobjects` | boolean | 하위 객체 허용 여부 |

##### template.lifecycle 속성 (데이터 스트림용)

| 속성 | 타입 | 설명 |
|-----|------|------|
| `data_retention` | string | 문서의 최소 저장 기간 |
| `enabled` | boolean | 수명 주기 활성화 여부 (기본값: true) |
| `downsampling` | array | 다운샘플링 구성 배열 |

---

### 템플릿 우선순위와 병합

#### 우선순위 (Priority)

우선순위는 새 데이터 스트림이나 인덱스가 생성될 때 인덱스 템플릿의 우선 적용 순서를 결정합니다.

- 우선순위가 가장 높은 인덱스 템플릿이 선택됩니다.
- 우선순위가 지정되지 않으면 템플릿은 우선순위 0(가장 낮음)으로 처리됩니다.

```json
PUT _index_template/high-priority-template
{
  "index_patterns": ["logs-*"],
  "priority": 500,
  "template": {
    "settings": {
      "number_of_shards": 2
    }
  }
}

PUT _index_template/low-priority-template
{
  "index_patterns": ["logs-*"],
  "priority": 100,
  "template": {
    "settings": {
      "number_of_shards": 1
    }
  }
}
```

위 예제에서 `logs-my-app` 인덱스를 생성하면 `high-priority-template`이 적용되어 샤드 수가 2가 됩니다.

#### 컴포넌트 템플릿 병합 순서

`composed_of` 필드에 여러 컴포넌트 템플릿이 지정되면 지정된 순서대로 병합됩니다. 즉, 나중에 지정된 컴포넌트 템플릿이 이전 것을 재정의합니다.

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["base-mappings", "extended-mappings", "final-settings"],
  "priority": 200
}
```

병합 순서:
1. `base-mappings`가 먼저 적용됨
2. `extended-mappings`가 `base-mappings`를 재정의
3. `final-settings`가 최종 재정의
4. 인덱스 템플릿 자체의 `template` 설정이 마지막으로 적용

#### 전체 우선순위 규칙

1. 인덱스 생성 요청에서 명시적으로 지정된 설정이 가장 높은 우선순위
2. 인덱스 템플릿의 `template` 섹션 설정
3. 컴포넌트 템플릿 설정 (나중에 지정된 것이 우선)

```json
// 인덱스 생성 시 명시적 설정이 템플릿 설정을 재정의
PUT my-index-000001
{
  "settings": {
    "number_of_shards": 5
  }
}
```

#### 매핑 병합

매핑 정의는 재귀적으로 병합됩니다. 동일한 필드에 서로 다른 설정이 정의된 경우 나중 설정이 우선합니다.

```json
// 컴포넌트 템플릿 1
PUT _component_template/base-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "message": {
          "type": "text"
        },
        "timestamp": {
          "type": "date"
        }
      }
    }
  }
}

// 컴포넌트 템플릿 2 - message 필드를 keyword로 재정의
PUT _component_template/override-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "message": {
          "type": "keyword"
        }
      }
    }
  }
}

// 인덱스 템플릿
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["base-mappings", "override-mappings"]
}
```

결과: `message` 필드는 `keyword` 타입이 되고, `timestamp` 필드는 `date` 타입으로 유지됩니다.

---

### 내장 인덱스 템플릿과 충돌 방지

Elasticsearch는 다음 패턴에 대해 우선순위 100 인 내장 인덱스 템플릿을 유지합니다:

- `.kibana-reporting*`
- `logs-*-*`
- `metrics-*-*`
- `synthetics-*-*`
- `profiling-*`
- `security_solution-*-*`

Fleet 통합은 우선순위 200 까지의 유사한 중복 패턴을 생성합니다.

#### 충돌 방지 전략

##### 1. Fleet 또는 Elastic Agent 사용 시

Fleet이나 Elastic Agent를 사용하는 경우 사용자 정의 템플릿에 100보다 낮은 우선순위를 할당합니다.

```json
PUT _index_template/my-custom-template
{
  "index_patterns": ["logs-*-*"],
  "priority": 50,
  "template": {
    "settings": {
      "number_of_replicas": 2
    }
  }
}
```

##### 2. Fleet을 사용하지 않을 때

Fleet이나 Elastic Agent를 사용하지 않고 `logs-*`와 같은 인덱스 패턴에 대한 템플릿을 생성하려면 500보다 높은 우선순위를 할당합니다.

```json
PUT _index_template/my-logs-template
{
  "index_patterns": ["logs-*"],
  "priority": 501,
  "template": {
    "settings": {
      "number_of_shards": 3
    }
  }
}
```

##### 3. 중복되지 않는 인덱스 패턴 사용

내장 패턴과 중복되지 않는 고유한 인덱스 패턴을 사용합니다.

```json
PUT _index_template/my-app-logs
{
  "index_patterns": ["myapp-logs-*"],
  "priority": 200,
  "template": {
    "settings": {
      "number_of_shards": 1
    }
  }
}
```

##### 4. 이름 지정 규칙

- 사용자 정의 인덱스 템플릿 이름에 `@` 기호를 사용하지 마세요.
- Elastic Stack 버전 9.1부터 Fleet은 `fleet-synced-integrations*` 인덱스를 사용하므로 이 이름을 피해야 합니다.

##### 5. 내장 템플릿 비활성화 (권장하지 않음)

```yaml
# elasticsearch.yml
stack.templates.enabled: false
```

> 경고: 이 설정은 Elastic Agent 통합과 Fleet이 올바르게 작동하지 않게 할 수 있으므로 권장하지 않습니다.

---

### 인덱스 템플릿 API

#### 인덱스 템플릿 생성/업데이트

```
PUT /_index_template/{template_name}
POST /_index_template/{template_name}
```

##### 경로 파라미터

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `template_name` | 예 | 인덱스 또는 템플릿 이름 |

##### 쿼리 파라미터

| 파라미터 | 기본값 | 설명 |
|---------|-------|------|
| `create` | false | `true`면 기존 템플릿 대체 방지 |
| `master_timeout` | 30s | 마스터 노드 연결 대기 시간 |
| `cause` | - | 템플릿 생성/업데이트 이유 (사용자 정의) |

##### 예제

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "template": {
    "settings": {
      "number_of_shards": 1
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "message": {
          "type": "text"
        }
      }
    }
  },
  "priority": 200,
  "composed_of": ["my-component-template"],
  "version": 1,
  "_meta": {
    "description": "내 애플리케이션용 템플릿"
  }
}
```

#### 인덱스 템플릿 조회

```
GET /_index_template
GET /_index_template/{template_name}
GET /_index_template/{template_name_pattern}
```

##### 예제

```json
// 모든 인덱스 템플릿 조회
GET _index_template

// 특정 템플릿 조회
GET _index_template/my-template

// 와일드카드로 조회
GET _index_template/my-*
```

##### 응답 예시

```json
{
  "index_templates": [
    {
      "name": "my-template",
      "index_template": {
        "index_patterns": ["my-*"],
        "template": {
          "settings": {
            "index": {
              "number_of_shards": "1"
            }
          },
          "mappings": {
            "properties": {
              "@timestamp": {
                "type": "date"
              }
            }
          }
        },
        "composed_of": ["my-component-template"],
        "priority": 200,
        "version": 1
      }
    }
  ]
}
```

#### 인덱스 템플릿 삭제

```
DELETE /_index_template/{template_name}
```

##### 예제

```json
// 단일 템플릿 삭제
DELETE _index_template/my-template

// 와일드카드로 여러 템플릿 삭제
DELETE _index_template/my-*
```

#### 인덱스 템플릿 존재 여부 확인

```
HEAD /_index_template/{template_name}
```

##### 예제

```json
HEAD _index_template/my-template
```

응답:
- `200`: 템플릿 존재
- `404`: 템플릿 미존재

---

### 컴포넌트 템플릿 API

#### 컴포넌트 템플릿 생성/업데이트

```
PUT /_component_template/{template_name}
POST /_component_template/{template_name}
```

##### 경로 파라미터

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `template_name` | 예 | 컴포넌트 템플릿 이름 |

##### 쿼리 파라미터

| 파라미터 | 기본값 | 설명 |
|---------|-------|------|
| `create` | false | `true`면 기존 템플릿 대체 방지 |
| `master_timeout` | 30s | 마스터 노드 연결 대기 시간 |
| `cause` | - | 템플릿 생성/업데이트 이유 (사용자 정의) |

##### 예제

```json
PUT _component_template/my-component-template
{
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "_source": {
        "enabled": false
      },
      "properties": {
        "host_name": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        }
      }
    }
  },
  "version": 1,
  "_meta": {
    "description": "기본 매핑 및 설정용 컴포넌트 템플릿"
  }
}
```

#### 컴포넌트 템플릿 조회

```
GET /_component_template
GET /_component_template/{template_name}
GET /_component_template/{template_name_pattern}
```

##### 예제

```json
// 모든 컴포넌트 템플릿 조회
GET _component_template

// 특정 템플릿 조회
GET _component_template/my-component-template

// 와일드카드로 조회
GET _component_template/my-*
```

#### 컴포넌트 템플릿 삭제

```
DELETE /_component_template/{template_name}
```

##### 예제

```json
DELETE _component_template/my-component-template
```

> 참고: 인덱스 템플릿에서 참조 중인 컴포넌트 템플릿은 삭제할 수 없습니다.

#### 컴포넌트 템플릿 존재 여부 확인

```
HEAD /_component_template/{template_name}
```

---

### 템플릿 시뮬레이션

템플릿을 실제로 적용하기 전에 효과를 테스트할 수 있습니다.

#### 인덱스 템플릿 시뮬레이션

특정 인덱스 이름에 어떤 설정이 적용될지 확인합니다.

```
POST /_index_template/_simulate_index/{index_name}
```

##### 예제

```json
POST _index_template/_simulate_index/my-index-000001
```

##### 응답 예시

```json
{
  "template": {
    "settings": {
      "index": {
        "number_of_shards": "1",
        "number_of_replicas": "1"
      }
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        }
      }
    },
    "aliases": {}
  },
  "overlapping": [
    {
      "name": "other-template",
      "index_patterns": ["my-*"]
    }
  ]
}
```

#### 템플릿 구성 시뮬레이션

기존 인덱스 템플릿의 구성을 시뮬레이션합니다.

```
POST /_index_template/_simulate/{template_name}
```

##### 예제

```json
POST _index_template/_simulate/my-template
```

#### 새 템플릿 정의 시뮬레이션

템플릿을 저장하지 않고 새 템플릿 정의의 효과를 시뮬레이션합니다.

```json
POST _index_template/_simulate
{
  "index_patterns": ["my-*"],
  "template": {
    "settings": {
      "number_of_shards": 2
    }
  },
  "composed_of": ["my-component-template"]
}
```

---

### 데이터 스트림용 템플릿

데이터 스트림을 생성하려면 `data_stream` 객체가 포함된 인덱스 템플릿이 필요합니다.

#### 데이터 스트림용 인덱스 템플릿 예제

```json
PUT _index_template/my-data-stream-template
{
  "index_patterns": ["my-data-stream*"],
  "data_stream": {},
  "composed_of": ["my-mappings", "my-settings"],
  "priority": 500,
  "_meta": {
    "description": "my-data-stream용 템플릿"
  }
}
```

#### data_stream 객체 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `hidden` | boolean | `true`면 데이터 스트림이 숨겨짐 |
| `allow_custom_routing` | boolean | 사용자 정의 라우팅 허용 여부 |

#### @timestamp 필드 요구 사항

데이터 스트림용 템플릿에서는 `@timestamp` 필드에 `date` 또는 `date_nanos` 매핑이 필요합니다. 매핑을 지정하지 않으면 Elasticsearch가 `@timestamp`를 기본값인 `date` 필드로 매핑합니다.

```json
PUT _component_template/my-data-stream-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date",
          "format": "date_optional_time||epoch_millis"
        },
        "message": {
          "type": "text"
        }
      }
    }
  }
}
```

#### 수명 주기 설정

데이터 스트림의 수명 주기를 템플릿에서 설정할 수 있습니다.

```json
PUT _index_template/my-data-stream-template
{
  "index_patterns": ["my-data-stream*"],
  "data_stream": {},
  "priority": 500,
  "template": {
    "lifecycle": {
      "data_retention": "7d"
    }
  }
}
```

#### 시계열 데이터 스트림 (TSDS) 템플릿

```json
PUT _component_template/my-tsds-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword",
              "time_series_dimension": true
            }
          }
        },
        "cpu": {
          "properties": {
            "usage": {
              "type": "double",
              "time_series_metric": "gauge"
            }
          }
        }
      }
    }
  }
}

PUT _index_template/my-tsds-template
{
  "index_patterns": ["metrics-*"],
  "data_stream": {},
  "composed_of": ["my-tsds-mappings"],
  "priority": 500,
  "template": {
    "settings": {
      "index.mode": "time_series"
    }
  }
}
```

#### LogsDB 모드 템플릿

```json
PUT _index_template/my-logsdb-template
{
  "index_patterns": ["logs-*-*"],
  "data_stream": {},
  "priority": 500,
  "template": {
    "settings": {
      "index.mode": "logsdb"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "message": {
          "type": "text"
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword"
            }
          }
        }
      }
    }
  }
}
```

---

### 모범 사례

#### 1. 컴포넌트 템플릿으로 모듈화

매핑과 설정을 별도의 컴포넌트 템플릿으로 분리하여 재사용성을 높입니다.

```json
// 매핑용 컴포넌트 템플릿
PUT _component_template/base-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "message": { "type": "text" }
      }
    }
  }
}

// 설정용 컴포넌트 템플릿
PUT _component_template/base-settings
{
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    }
  }
}

// 인덱스 템플릿에서 조합
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["base-mappings", "base-settings"],
  "priority": 200
}
```

#### 2. 버전 관리

템플릿에 버전 번호를 사용하여 변경 사항을 추적합니다.

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "version": 2,
  "_meta": {
    "description": "v2: 레플리카 수 증가",
    "updated_at": "2024-01-15"
  },
  "template": {
    "settings": {
      "number_of_replicas": 2
    }
  }
}
```

#### 3. 템플릿 테스트

운영 환경에 적용하기 전에 시뮬레이션 API로 테스트합니다.

```json
POST _index_template/_simulate_index/my-index-000001
```

#### 4. 적절한 우선순위 설정

- 내장 템플릿: 우선순위 100
- Fleet 통합 템플릿: 우선순위 200
- 사용자 정의 템플릿 (Fleet 사용): 우선순위 50-99
- 사용자 정의 템플릿 (Fleet 미사용): 우선순위 500+

#### 5. 명확한 이름 지정

템플릿 이름에서 목적을 명확히 합니다.

```json
PUT _component_template/logs-mappings-v1
PUT _component_template/logs-settings-production
PUT _index_template/logs-application-prod
```

#### 6. 누락된 컴포넌트 템플릿 처리

아직 존재하지 않는 컴포넌트 템플릿을 참조하는 인덱스 템플릿을 미리 생성할 수 있습니다.

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["existing-component", "future-component"],
  "ignore_missing_component_templates": ["future-component"],
  "priority": 200
}
```

#### 7. 데이터 스트림 명명 체계

Elastic 데이터 스트림 명명 체계를 따릅니다:

```
<type>-<dataset>-<namespace>
```

예시:
- `logs-nginx-production`
- `metrics-system-monitoring`
- `traces-apm-default`

---

### 문제 해결

#### 템플릿이 적용되지 않을 때

1. 인덱스 패턴 확인: 인덱스 이름이 `index_patterns`에 일치하는지 확인합니다.

```json
GET _index_template/my-template
```

2. 우선순위 충돌 확인: 우선순위가 더 높은 템플릿이 있는지 확인합니다.

```json
POST _index_template/_simulate_index/my-index-000001
```

3. 템플릿 유효성 검사: 템플릿 구문이 올바른지 확인합니다.

#### 컴포넌트 템플릿 삭제 오류

참조 중인 컴포넌트 템플릿은 삭제할 수 없습니다.

```json
// 어떤 인덱스 템플릿이 참조하는지 확인
GET _index_template?filter_path=index_templates.*.index_template.composed_of
```

#### 매핑 충돌

동일한 필드에 대해 호환되지 않는 매핑이 정의된 경우 오류가 발생합니다.

```json
// 올바른 접근: 필드 타입 일관성 유지
PUT _component_template/mappings-v2
{
  "template": {
    "mappings": {
      "properties": {
        "status": {
          "type": "keyword"  // 모든 템플릿에서 동일한 타입 사용
        }
      }
    }
  }
}
```

---

### 참고 자료

- [Templates | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/templates)
- [Create or update an index template API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-put-index-template)
- [Create or update a component template API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-cluster-put-component-template)
- [Simulate an index template API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-simulate-template)
- [Data Streams | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams)
- [Index Settings Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html)
- [Mapping Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html)
