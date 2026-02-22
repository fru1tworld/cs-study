# 집계 (Aggregations)

> 이 문서는 Elasticsearch 9.x 공식 문서의 Aggregations 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html

## 목차

1. [집계 개요](#집계-개요)
2. [집계 실행하기](#집계-실행하기)
3. [집계 범위 변경](#집계-범위-변경)
4. [집계 결과만 반환하기](#집계-결과만-반환하기)
5. [여러 집계 실행하기](#여러-집계-실행하기)
6. [하위 집계](#하위-집계)
7. [메타데이터 추가](#메타데이터-추가)
8. [집계 타입 반환](#집계-타입-반환)
9. [스크립트를 사용한 집계](#스크립트를-사용한-집계)
10. [집계 캐싱](#집계-캐싱)
11. [숫자 제한 사항](#숫자-제한-사항)
12. [집계 유형](#집계-유형)
    - [버킷 집계](#버킷-집계)
    - [메트릭 집계](#메트릭-집계)
    - [파이프라인 집계](#파이프라인-집계)

---

## 집계 개요

집계(Aggregations)는 데이터를 메트릭, 통계 또는 기타 분석 정보로 요약합니다. 집계를 통해 다음과 같은 질문에 답할 수 있습니다:

- 내 웹사이트의 평균 로드 시간은 얼마인가?
- 거래 금액을 기준으로 가장 가치 있는 고객은 누구인가?
- 네트워크에서 대용량 파일로 간주되는 크기는 얼마인가?
- 각 제품 카테고리에는 몇 개의 제품이 있는가?

Elasticsearch는 집계를 세 가지 카테고리로 구분합니다:

| 집계 유형 | 설명 |
|----------|------|
| 메트릭 집계 | 필드 값에서 합계나 평균과 같은 메트릭을 계산하는 집계 |
| 버킷 집계 | 필드 값, 범위 또는 기타 기준에 따라 문서를 버킷(그룹)으로 분류하는 집계 |
| 파이프라인 집계 | 문서나 필드 대신 다른 집계의 출력을 입력으로 사용하는 집계 |

---

## 집계 실행하기

집계는 검색의 일부로 실행할 수 있습니다. 검색 API의 `aggs` 파라미터를 사용하여 집계를 지정합니다. 다음 검색은 `my-field`에 대한 terms 집계를 실행합니다:

```json
GET /my-index-000001/_search
{
  "aggs": {
    "my-agg-name": {
      "terms": {
        "field": "my-field"
      }
    }
  }
}
```

집계 결과는 응답의 `aggregations` 객체에 포함됩니다:

```json
{
  "took": 78,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 5,
      "relation": "eq"
    },
    "max_score": 1.0,
    "hits": [...]
  },
  "aggregations": {
    "my-agg-name": {
      "doc_count_error_upper_bound": 0,
      "sum_other_doc_count": 0,
      "buckets": []
    }
  }
}
```

---

## 집계 범위 변경

`query` 파라미터를 사용하여 집계가 실행되는 문서의 범위를 제한할 수 있습니다. 다음 검색은 `query` 파라미터를 사용하여 지난 하루 동안의 문서에 대해서만 terms 집계를 실행합니다:

```json
GET /my-index-000001/_search
{
  "query": {
    "range": {
      "@timestamp": {
        "gte": "now-1d/d",
        "lt": "now/d"
      }
    }
  },
  "aggs": {
    "my-agg-name": {
      "terms": {
        "field": "my-field"
      }
    }
  }
}
```

---

## 집계 결과만 반환하기

기본적으로 집계가 포함된 검색은 검색 히트와 집계 결과를 모두 반환합니다. 집계 결과만 얻으려면 `size`를 `0`으로 설정합니다:

```json
GET /my-index-000001/_search
{
  "size": 0,
  "aggs": {
    "my-agg-name": {
      "terms": {
        "field": "my-field"
      }
    }
  }
}
```

`size`를 `0`으로 설정하면 검색 히트가 반환되지 않아 불필요한 캐시 사용을 피할 수 있습니다.

---

## 여러 집계 실행하기

동일한 요청에서 여러 집계를 지정할 수 있습니다:

```json
GET /my-index-000001/_search
{
  "aggs": {
    "my-first-agg-name": {
      "terms": {
        "field": "my-field"
      }
    },
    "my-second-agg-name": {
      "avg": {
        "field": "my-other-field"
      }
    }
  }
}
```

---

## 하위 집계

버킷 집계는 버킷 또는 메트릭 하위 집계를 지원합니다. 예를 들어, 평균 하위 집계가 있는 terms 집계는 각 버킷의 문서에 대한 평균값을 계산합니다. 하위 집계의 깊이에는 제한이 없습니다:

```json
GET /my-index-000001/_search
{
  "aggs": {
    "my-agg-name": {
      "terms": {
        "field": "my-field"
      },
      "aggs": {
        "my-sub-agg-name": {
          "avg": {
            "field": "my-other-field"
          }
        }
      }
    }
  }
}
```

응답은 하위 집계 결과를 부모 집계의 버킷 아래에 중첩합니다:

```json
{
  ...
  "aggregations": {
    "my-agg-name": {
      "doc_count_error_upper_bound": 0,
      "sum_other_doc_count": 0,
      "buckets": [
        {
          "key": "foo",
          "doc_count": 5,
          "my-sub-agg-name": {
            "value": 75.0
          }
        }
      ]
    }
  }
}
```

---

## 메타데이터 추가

`meta` 객체를 사용하여 집계에 사용자 정의 메타데이터를 연결할 수 있습니다:

```json
GET /my-index-000001/_search
{
  "aggs": {
    "my-agg-name": {
      "terms": {
        "field": "my-field"
      },
      "meta": {
        "my-metadata-field": "foo"
      }
    }
  }
}
```

응답은 집계 결과에 `meta` 객체를 포함합니다:

```json
{
  ...
  "aggregations": {
    "my-agg-name": {
      "meta": {
        "my-metadata-field": "foo"
      },
      "doc_count_error_upper_bound": 0,
      "sum_other_doc_count": 0,
      "buckets": []
    }
  }
}
```

---

## 집계 타입 반환

기본적으로 집계 결과에는 집계 이름만 포함되고 타입은 포함되지 않습니다. 집계 타입을 반환하려면 `typed_keys` 쿼리 파라미터를 사용합니다:

```json
GET /my-index-000001/_search?typed_keys
{
  "aggs": {
    "my-agg-name": {
      "histogram": {
        "field": "my-field",
        "interval": 1000
      }
    }
  }
}
```

응답은 집계 이름 앞에 집계 타입을 접두사로 반환합니다:

```json
{
  ...
  "aggregations": {
    "histogram#my-agg-name": {
      "buckets": []
    }
  }
}
```

`typed_keys` 파라미터는 terms, significant terms, 백분위수 집계와 같이 필드 데이터 타입에 따라 다른 타입을 반환할 수 있는 집계에서 특히 유용합니다.

---

## 스크립트를 사용한 집계

런타임 필드를 사용하여 집계 중에 필드 값을 동적으로 계산할 수 있습니다. 예를 들어, `message` 필드의 문자열 길이를 기반으로 히스토그램 집계를 실행할 수 있습니다:

```json
GET /my-index-000001/_search?size=0
{
  "runtime_mappings": {
    "message.length": {
      "type": "long",
      "script": "emit(doc['message.keyword'].value.length())"
    }
  },
  "aggs": {
    "message_length": {
      "histogram": {
        "interval": 10,
        "field": "message.length"
      }
    }
  }
}
```

스크립트는 문서의 원시 값에 접근할 수 있지만, 필드에 직접 집계하는 것보다 성능이 느립니다. 자주 검색하고 필터링하는 필드는 인덱싱하여 검색 성능과 유연성의 균형을 맞추는 것이 좋습니다.

---

## 집계 캐싱

자주 실행되는 집계의 결과는 샤드 요청 캐시에 캐시됩니다. 캐시된 결과를 얻으려면 검색에 대해 동일한 `preference` 문자열을 사용합니다. 검색 히트가 필요하지 않으면 `size`를 `0`으로 설정하여 캐시 채우기를 피할 수 있습니다.

Elasticsearch는 동일한 `preference` 문자열을 가진 검색을 동일한 샤드로 라우팅합니다. 샤드의 데이터가 검색 간에 변경되지 않으면 샤드는 캐시된 집계 결과를 반환합니다.

---

## 숫자 제한 사항

Elasticsearch는 `long` 정수에 대한 집계를 실행할 때 내부적으로 `double` 값을 사용합니다. 결과적으로 2^53보다 큰 `long` 값에 대한 집계는 근사치입니다.

---

## 집계 유형

### 버킷 집계

버킷 집계는 메트릭 집계처럼 필드에 대한 메트릭을 계산하지 않습니다. 대신 문서의 버킷을 생성합니다. 각 버킷은 기준(집계에 따라 다름)과 연결되어 현재 컨텍스트의 문서가 "해당 버킷에 속하는지" 여부를 결정합니다. 집계는 버킷에 속하는 문서 수도 계산합니다. 즉, 버킷은 효과적으로 문서 집합을 정의합니다.

메트릭 집계와 달리 버킷 집계는 하위 집계를 가질 수 있습니다. 이 하위 집계는 부모 버킷 집계가 생성한 버킷에 대해 집계됩니다.

버킷 집계 유형마다 버킷을 정의하는 "버킷팅" 전략이 다릅니다:
- 단일 버킷을 정의하는 버킷 집계
- 고정된 수의 여러 버킷을 정의하는 버킷 집계
- 집계 과정에서 버킷을 동적으로 생성하는 버킷 집계

> 중요: `search.max_buckets` 클러스터 설정은 단일 응답에서 허용되는 버킷 수를 제한합니다.

#### 버킷 집계 종류

| 집계 | 설명 |
|------|------|
| Adjacency matrix | 인접 행렬 집계 |
| Auto-interval date histogram | 자동 간격 날짜 히스토그램 집계 |
| Categorize text | 텍스트 분류 집계 |
| Children | 자식 문서 집계 |
| Composite | 복합 집계 |
| Date histogram | 날짜 히스토그램 집계 |
| Date range | 날짜 범위 집계 |
| Diversified sampler | 다양화된 샘플러 집계 |
| Filter | 필터 집계 |
| Filters | 필터들 집계 |
| Frequent item sets | 빈발 항목 집합 집계 |
| Geo-distance | 지리적 거리 집계 |
| Geohash grid | Geohash 그리드 집계 |
| Geohex grid | Geohex 그리드 집계 |
| Geotile grid | Geotile 그리드 집계 |
| Global | 전역 집계 |
| Histogram | 히스토그램 집계 |
| IP prefix | IP 접두사 집계 |
| IP range | IP 범위 집계 |
| Missing | 누락 값 집계 |
| Multi Terms | 다중 용어 집계 |
| Nested | 중첩 집계 |
| Parent | 부모 문서 집계 |
| Random sampler | 무작위 샘플러 집계 |
| Range | 범위 집계 |
| Rare terms | 희귀 용어 집계 |
| Reverse nested | 역중첩 집계 |
| Sampler | 샘플러 집계 |
| Significant terms | 유의미한 용어 집계 |
| Significant text | 유의미한 텍스트 집계 |
| Terms | 용어 집계 |
| Time series | 시계열 집계 |
| Variable width histogram | 가변 너비 히스토그램 집계 |

---

### 메트릭 집계

메트릭 집계는 집계되는 문서에서 추출된 값을 기반으로 메트릭을 계산합니다. 값은 일반적으로 문서의 필드에서 추출되지만 스크립트를 사용하여 생성할 수도 있습니다.

메트릭 집계는 두 가지 범주로 나뉩니다:

| 범주 | 설명 |
|------|------|
| 단일 값 숫자 메트릭 | 단일 메트릭 값을 출력하는 집계 (예: 평균) |
| 다중 값 숫자 메트릭 | 여러 메트릭을 생성하는 집계 (예: 통계) |

이 구분은 버킷 집계의 하위 집계로 사용될 때 버킷 정렬 기능에서 중요합니다.

#### 메트릭 집계 종류

| 집계 | 설명 |
|------|------|
| Avg | 평균값 계산 |
| Boxplot | 분포 요약 생성 |
| Cardinality | 고유 값 수 계산 |
| Extended stats | 확장 통계 분석 |
| Geo-bounds | 지리적 경계 결정 |
| Geo-centroid | 지리적 중심점 계산 |
| Geo-line | 지리적 경로 추적 |
| Matrix stats | 값 관계 분석 |
| Max | 최댓값 식별 |
| Min | 최솟값 식별 |
| Median absolute deviation | 중앙값 절대 편차 계산 |
| Percentile ranks | 값 분포 결정 |
| Percentiles | 분포 지점 계산 |
| Rate | 변화율 측정 |
| Scripted metric | 사용자 정의 계산 |
| Stats | 요약 통계 |
| String stats | 텍스트 분석 메트릭 |
| Sum | 합계 집계 |
| T-test | 통계적 검정 |
| Top hits | 상위 문서 검색 |
| Top metrics | 상위 메트릭 검색 |
| Value count | 값 개수 계산 |
| Weighted avg | 가중 평균 계산 |

---

### 파이프라인 집계

파이프라인 집계는 문서 집합이 아닌 다른 집계의 출력에서 작동하며, 출력 트리에 정보를 추가합니다. 파이프라인 집계에는 두 가지 유형이 있습니다:

| 유형 | 설명 |
|------|------|
| 부모 파이프라인 집계 | 부모 집계의 출력을 받아 새 버킷을 계산하거나 기존 버킷에 새 집계를 추가할 수 있음 |
| 형제 파이프라인 집계 | 동일한 계층 수준에서 형제 집계의 출력으로 작업하여 그들과 함께 새 집계를 계산할 수 있음 |

#### buckets_path 파라미터

대부분의 파이프라인 집계는 `buckets_path` 파라미터가 필요합니다. 이 파라미터는 집계 이름과 메트릭 이름을 지정하는 경로 구문을 사용합니다:

- 집계 이름은 `>`로 구분됩니다
- 메트릭 이름은 `.`으로 구분됩니다

예: `"my_bucket>my_stats.avg"`는 `my_bucket` 집계 내의 `my_stats` 집계의 평균값을 참조합니다.

#### 특수 경로 옵션

| 경로 | 설명 |
|------|------|
| `_count` | 특정 메트릭 대신 문서 수를 참조 |
| `_bucket_count` | 다중 버킷 집계가 반환한 버킷 수를 사용 |
| 대괄호 표기법 | 점이 포함된 집계 이름 처리 (예: `"my_percentile[99.9]"`) |

#### 갭 정책

데이터에 갭(누락된 값)이 있을 때 세 가지 정책으로 처리합니다:

| 정책 | 설명 |
|------|------|
| skip | 누락된 데이터를 존재하지 않는 것으로 취급하고 사용 가능한 값으로 계속 진행 |
| insert_zeros | 누락된 값을 0으로 대체 |
| keep_values | 사용 가능한 경우 null이 아닌 값을 사용하고 빈 버킷은 건너뜀 |

파이프라인 집계는 하위 집계를 가질 수 없지만 다른 파이프라인을 참조할 수 있어, 2차 미분과 같은 복잡한 계산을 위한 집계 체이닝이 가능합니다.

#### 파이프라인 집계 종류

| 집계 | 설명 |
|------|------|
| Average bucket | 버킷 평균 |
| Bucket script | 버킷 스크립트 |
| Bucket count K-S test | 버킷 카운트 K-S 테스트 |
| Bucket correlation | 버킷 상관관계 |
| Bucket selector | 버킷 선택기 |
| Bucket sort | 버킷 정렬 |
| Change point | 변경점 감지 |
| Cumulative cardinality | 누적 카디널리티 |
| Cumulative sum | 누적 합계 |
| Derivative | 미분 |
| Extended stats bucket | 확장 통계 버킷 |
| Inference | 추론 |
| Max bucket | 최대 버킷 |
| Min bucket | 최소 버킷 |
| Moving average | 이동 평균 |
| Moving function | 이동 함수 |
| Moving percentiles | 이동 백분위수 |
| Normalize | 정규화 |
| Percentiles bucket | 백분위수 버킷 |
| Serial differencing | 직렬 차분 |
| Stats bucket | 통계 버킷 |
| Sum bucket | 합계 버킷 |

---

## 요약

Elasticsearch 집계는 데이터 분석을 위한 강력한 도구입니다. 주요 포인트는 다음과 같습니다:

1. 세 가지 집계 유형: 메트릭, 버킷, 파이프라인 집계가 있으며 각각 다른 목적으로 사용됩니다
2. 중첩 가능: 버킷 집계 내에 하위 집계를 무제한으로 중첩할 수 있습니다
3. 범위 제어: `query` 파라미터로 집계 대상 문서를 제한할 수 있습니다
4. 성능 최적화: `size: 0`으로 집계 결과만 반환하고, 캐싱을 활용할 수 있습니다
5. 런타임 필드: 스크립트를 사용하여 동적으로 필드 값을 계산할 수 있지만 성능 오버헤드가 있습니다
6. 숫자 정밀도: 2^53보다 큰 `long` 값은 근사치로 처리됩니다

---

## 참고 자료

- [Elasticsearch Aggregations 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html)
- [Bucket Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket.html)
- [Metrics Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics.html)
- [Pipeline Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-pipeline.html)
