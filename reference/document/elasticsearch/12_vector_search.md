# Elasticsearch kNN 검색 및 벡터 검색 가이드

## 목차

1. [개요](#개요)
2. [kNN 검색이란?](#knn-검색이란)
3. [근사 kNN 검색](#근사-knn-검색)
4. [정확한 무차별 대입(Brute-force) kNN 검색](#정확한-무차별-대입brute-force-knn-검색)
5. [Dense Vector 필드 타입](#dense-vector-필드-타입)
6. [kNN 쿼리](#knn-쿼리)
7. [필터링을 사용한 kNN 검색](#필터링을-사용한-knn-검색)
8. [하이브리드 검색](#하이브리드-검색)
9. [Retriever를 사용한 검색](#retriever를-사용한-검색)
10. [중첩(Nested) 벡터 검색](#중첩nested-벡터-검색)
11. [시맨틱 검색](#시맨틱-검색)
12. [Sparse Vector와 ELSER](#sparse-vector와-elser)
13. [성능 튜닝](#성능-튜닝)
14. [벡터 양자화(Quantization)](#벡터-양자화quantization)

---

## 개요

Elasticsearch는 k-최근접 이웃(k-Nearest Neighbor, kNN) 검색을 통해 벡터 유사도 기반 검색을 지원합니다. 이 기능은 시맨틱 검색, 이미지 유사도 검색, 추천 시스템 등 다양한 용도로 활용됩니다.

### 벡터 검색의 두 가지 방법

Elasticsearch는 kNN 검색을 위한 두 가지 방법을 지원합니다:

1. 근사 kNN (Approximate kNN): `knn` 옵션, `knn` 쿼리, 또는 `knn` retriever를 사용하는 빠르고 확장 가능한 유사도 검색입니다. 대부분의 프로덕션 워크로드에 적합합니다.

2. 정확한 무차별 대입 kNN (Exact, brute-force kNN): `script_score` 쿼리와 벡터 함수를 사용합니다. 소규모 데이터셋이나 정확한 스코어링이 필요한 경우에 적합합니다.

---

## kNN 검색이란?

kNN 검색은 쿼리 벡터와 가장 유사한 k개의 벡터를 찾는 검색 방법입니다. 유사도 메트릭(코사인 유사도, 유클리드 거리 등)을 사용하여 벡터 간의 거리를 측정합니다.

### 기본 kNN 검색 예제

먼저, dense_vector 필드가 있는 인덱스를 생성합니다:

```json
PUT image-index
{
  "mappings": {
    "properties": {
      "image-vector": {
        "type": "dense_vector",
        "dims": 3,
        "similarity": "l2_norm"
      },
      "title": {
        "type": "text"
      },
      "file-type": {
        "type": "keyword"
      }
    }
  }
}
```

문서를 인덱싱합니다:

```json
POST image-index/_bulk?refresh=true
{ "index": { "_id": "1" } }
{ "image-vector": [1, 5, -20], "title": "moose family", "file-type": "png" }
{ "index": { "_id": "2" } }
{ "image-vector": [42, 8, -15], "title": "alpine lake", "file-type": "png" }
{ "index": { "_id": "3" } }
{ "image-vector": [15, 11, 23], "title": "full moon", "file-type": "jpg" }
```

kNN 검색을 실행합니다:

```json
POST image-index/_search
{
  "knn": {
    "field": "image-vector",
    "query_vector": [54, 10, -2],
    "k": 5,
    "num_candidates": 50
  },
  "fields": ["title"],
  "_source": false
}
```

---

## 근사 kNN 검색

### HNSW 알고리즘

Elasticsearch는 효율적인 근사 kNN 검색을 위해 HNSW(Hierarchical Navigable Small World) 알고리즘을 사용합니다. HNSW는 그래프 기반 알고리즘으로, 서로 가까운 요소들 간의 링크를 여러 레이어에서 유지합니다.

- 각 레이어는 연결된 요소들을 포함하며, 아래 레이어의 요소들과도 연결됩니다.
- 각 레이어는 더 많은 요소를 포함하며, 최하위 레이어는 모든 요소를 포함합니다.
- 대부분의 근사 방법과 마찬가지로, HNSW는 정확도를 희생하여 속도를 얻습니다.

### 근사 kNN의 작동 방식

결과를 수집하기 위해, kNN 검색 API는 먼저 각 샤드에서 `num_candidates` 수만큼의 근사 최근접 이웃 후보를 찾습니다. 그런 다음 이러한 후보 벡터들과 쿼리 벡터 간의 유사도를 계산하고, 각 샤드에서 가장 유사한 k개의 결과를 선택합니다. 마지막으로 각 샤드의 결과를 병합하여 전역 top k 최근접 이웃을 반환합니다.

### num_candidates 파라미터

`num_candidates`는 각 샤드에서 kNN 검색 수행 시 고려할 최근접 이웃 후보의 수입니다. 10,000을 초과할 수 없습니다.

- num_candidates 증가: 정확도가 향상되지만 검색 속도가 느려집니다.
- num_candidates 감소: 검색 속도가 빨라지지만 정확도가 떨어질 수 있습니다.

기본값은 `k`가 설정된 경우 `1.5 * k`, 그렇지 않으면 `1.5 * size`입니다.

---

## 정확한 무차별 대입(Brute-force) kNN 검색

정확한 벡터 검색은 전체 벡터 공간에 대해 선형 검색(무차별 대입 검색)을 수행합니다. 쿼리 벡터가 저장된 모든 벡터와 비교되어 가장 가까운 이웃을 찾습니다.

### script_score 쿼리 사용

`script_score` 쿼리를 사용하면 각 일치하는 문서를 스캔하여 벡터 함수를 계산해야 하므로 검색 속도가 느려질 수 있습니다. 그러나 쿼리를 사용하여 일치하는 문서 수를 제한하면 대기 시간을 개선할 수 있습니다.

### 사용 가능한 벡터 함수

dense 벡터에 접근하는 권장 방법은 `cosineSimilarity`, `dotProduct`, `l1norm`, `l2norm` 함수를 사용하는 것입니다.

#### cosineSimilarity 예제

```json
GET my-index/_search
{
  "query": {
    "script_score": {
      "query": {
        "bool": {
          "filter": {
            "term": {
              "status": "published"
            }
          }
        }
      },
      "script": {
        "source": "cosineSimilarity(params.query_vector, 'my_dense_vector') + 1.0",
        "params": {
          "query_vector": [4, 3.4, -0.2]
        }
      }
    }
  }
}
```

> 참고: 스크립트에서 `+ 1.0`을 추가하는 이유는 코사인 유사도가 음수가 될 수 있으므로 점수가 음수가 되는 것을 방지하기 위함입니다.

#### dotProduct 예제

```json
GET my-index/_search
{
  "query": {
    "script_score": {
      "query": {
        "match_all": {}
      },
      "script": {
        "source": "dotProduct(params.query_vector, 'my_dense_vector')",
        "params": {
          "query_vector": [4, 3.4, -0.2]
        }
      }
    }
  }
}
```

#### l1norm 및 l2norm 예제

`l1norm`과 `l2norm`은 유사도가 아닌 거리를 나타내므로, 벡터가 유사할수록 점수가 낮아집니다. 따라서 출력을 역으로 변환해야 합니다:

```json
GET my-index/_search
{
  "query": {
    "script_score": {
      "query": {
        "match_all": {}
      },
      "script": {
        "source": "1 / (1 + l2norm(params.query_vector, 'my_dense_vector'))",
        "params": {
          "query_vector": [4, 3.4, -0.2]
        }
      }
    }
  }
}
```

### 언제 정확한 kNN을 사용해야 하는가?

- 검색할 문서가 10,000개 미만 인 경우
- 데이터를 소규모 하위 집합으로 필터링하는 경우
- 정밀한 스코어링이 필요한 경우

근사 검색은 문서 수 측면에서 더 잘 확장되므로, 문서가 많거나 크게 증가할 것으로 예상되는 경우 근사 검색을 선호해야 합니다.

---

## Dense Vector 필드 타입

`dense_vector` 필드 타입은 숫자 값의 밀집 벡터를 저장합니다. 주로 k-최근접 이웃(kNN) 검색에 사용됩니다.

> 참고: `dense_vector` 타입은 집계(aggregation)나 정렬(sorting)을 지원하지 않습니다.

### 기본 매핑

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 1024
      }
    }
  }
}
```

### 전체 매핑 옵션

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 1024,
        "element_type": "float",
        "similarity": "cosine",
        "index": true,
        "index_options": {
          "type": "int8_hnsw",
          "m": 16,
          "ef_construction": 100
        }
      }
    }
  }
}
```

### 파라미터 설명

#### dims (차원)
- 벡터 차원의 수를 지정합니다.
- `element_type`이 `bit`인 경우 8의 배수여야 합니다.
- 최대 값은 4096입니다.

#### element_type (요소 타입)

| 값 | 설명 | 크기 |
|---|---|---|
| `float` | 4바이트 부동소수점 값 (기본값) | 4바이트/차원 |
| `byte` | 1바이트 정수 값 (-128 ~ 127) | 1바이트/차원 |
| `bit` | 단일 비트 | 1비트/차원 |

#### similarity (유사도 메트릭)

| 메트릭 | 설명 | 점수 계산 |
|---|---|---|
| `l2_norm` | L2 거리(유클리드 거리) 기반 | `1 / (1 + l2_norm(query, vector)^2)` |
| `dot_product` | 두 단위 벡터의 내적 | 단위 길이 벡터에만 사용 가능 |
| `cosine` | 코사인 유사도 (기본값) | `(1 + cosine(query, vector)) / 2` |
| `max_inner_product` | 최대 내적 | 정규화되지 않은 벡터에 사용 가능 |

> 참고: `cosine` 유사도 사용 시, Elasticsearch는 인덱싱 중에 벡터를 자동으로 단위 길이로 정규화합니다. 이를 통해 내부적으로 더 효율적인 `dot_product`를 사용하여 유사도를 계산합니다.

### index_options (인덱스 옵션)

kNN 인덱싱 알고리즘을 구성하는 선택적 객체입니다.

#### 인덱스 타입

| 타입 | 설명 | 메모리 절감 |
|---|---|---|
| `hnsw` | HNSW 알고리즘 사용, 모든 element_type 지원 | - |
| `int8_hnsw` | HNSW + 자동 8비트 양자화 (기본값, 9.0) | 4배 |
| `int4_hnsw` | HNSW + 자동 4비트 양자화 | 8배 |
| `bbq_hnsw` | HNSW + 자동 이진 양자화 (384차원 이상 기본값) | 32배 |
| `flat` | 무차별 대입 검색 알고리즘, 정확한 kNN | - |
| `int8_flat` | 무차별 대입 + 자동 8비트 양자화 | 4배 |
| `int4_flat` | 무차별 대입 + 자동 4비트 양자화 | 8배 |
| `bbq_flat` | 무차별 대입 + 자동 이진 양자화 | 32배 |
| `bbq_disk` | k-means 클러스터링 + 자동 이진 양자화 | HNSW보다 낮은 메모리 |

#### HNSW 파라미터

| 파라미터 | 설명 | 기본값 |
|---|---|---|
| `m` | 그래프의 각 노드가 가질 수 있는 최대 엣지 수 | 16 |
| `ef_construction` | 노드 추가 시 엣지 후보 큐의 크기 | 100 |

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 1024,
        "index_options": {
          "type": "hnsw",
          "m": 32,
          "ef_construction": 200
        }
      }
    }
  }
}
```

### 인덱스 타입 업그레이드 경로

Update Mapping API를 사용하여 인덱스 타입을 업그레이드할 수 있습니다:

```
flat → int8_flat → int4_flat → bbq_flat → hnsw → int8_hnsw → int4_hnsw → bbq_hnsw
```

> 참고: 타입을 변경해도 이미 인덱싱된 벡터는 다시 인덱싱되지 않습니다. 기존 벡터는 원래 타입을 유지하고, 변경 후 인덱싱된 벡터만 새 타입을 사용합니다.

---

## kNN 쿼리

`knn` 쿼리는 쿼리 벡터에 가장 가까운 k개의 벡터를 찾습니다. 인덱싱된 dense_vector에 대한 근사 검색을 통해 유사도 메트릭으로 측정됩니다.

### 기본 kNN 쿼리

```json
POST my-index/_search
{
  "knn": {
    "field": "my_vector",
    "query_vector": [0.1, 0.2, 0.3, ...],
    "k": 10,
    "num_candidates": 100
  }
}
```

### kNN 쿼리 vs 최상위 kNN 검색

- 최상위 knn 섹션: 근사 kNN 검색을 수행하는 권장 방법
- knn 쿼리: 다른 쿼리와 결합하거나 `semantic_text` 필드에 대해 kNN 검색을 수행해야 하는 전문가 케이스용

`knn` 쿼리는 최상위 knn 검색과 달리 `k` 파라미터가 없습니다. 반환되는 결과(최근접 이웃) 수는 다른 쿼리와 마찬가지로 `size` 파라미터로 정의됩니다.

### kNN 쿼리 파라미터

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `field` | 예 | 검색할 dense_vector 필드 이름 |
| `query_vector` | 예* | 쿼리 벡터 (배열) |
| `query_vector_builder` | 예* | 쿼리 벡터를 빌드하는 설정 객체 |
| `k` | 아니오 | 반환할 최근접 이웃 수 |
| `num_candidates` | 아니오 | 각 샤드에서 고려할 후보 수 (최대 10,000) |
| `filter` | 아니오 | 사전 필터링 쿼리 |
| `boost` | 아니오 | 점수 배수 (기본값: 1.0) |

> 참고: `query_vector` 또는 `query_vector_builder` 중 하나를 제공해야 합니다.

### query_vector_builder 사용

텍스트 임베딩 모델을 사용하여 쿼리 시점에 벡터를 생성할 수 있습니다:

```json
GET my-index/_search
{
  "knn": {
    "field": "my_embeddings",
    "k": 10,
    "num_candidates": 100,
    "query_vector_builder": {
      "text_embedding": {
        "model_id": "sentence-transformers__msmarco-minilm-l-12-v3",
        "model_text": "검색할 텍스트"
      }
    }
  }
}
```

---

## 필터링을 사용한 kNN 검색

kNN 쿼리와 일치하는 문서를 필터링하는 두 가지 방법이 있습니다:

1. 사전 필터링 (Pre-filtering): 근사 kNN 검색 중에 필터가 적용되어 k개의 일치하는 문서가 반환되도록 보장합니다.
2. 사후 필터링 (Post-filtering): 근사 kNN 단계 후에 필터가 적용되어 k개 미만의 결과가 반환될 수 있습니다.

### 사전 필터링 예제

```json
POST image-index/_search
{
  "knn": {
    "field": "image-vector",
    "query_vector": [54, 10, -2],
    "k": 5,
    "num_candidates": 50,
    "filter": {
      "term": {
        "file-type": "png"
      }
    }
  },
  "fields": ["title"],
  "_source": false
}
```

### 복합 필터 예제

```json
POST image-index/_search
{
  "knn": {
    "field": "image-vector",
    "query_vector": [54, 10, -2],
    "k": 5,
    "num_candidates": 50,
    "filter": {
      "bool": {
        "must": [
          { "term": { "file-type": "png" } },
          { "range": { "created_at": { "gte": "2023-01-01" } } }
        ]
      }
    }
  }
}
```

### 필터링 성능 고려사항

HNSW 인덱스를 사용한 근사 kNN 검색에서 필터를 적용하면 성능이 저하될 수 있습니다. 엔진이 필터를 충족하고 `num_candidates`에 도달하는 충분한 후보를 모으기 위해 그래프를 더 많이 탐색해야 하기 때문입니다.

Lucene은 세그먼트별로 다음 전략을 구현합니다:
- 필터링된 문서 수가 `num_candidates` 이하인 경우, HNSW 그래프를 건너뛰고 필터링된 문서에 대해 무차별 대입 검색을 수행합니다.
- HNSW 그래프를 탐색하는 동안, 탐색된 노드 수가 필터를 충족하는 문서 수를 초과하면 그래프 탐색을 중단하고 무차별 대입 검색으로 전환합니다.

---

## 하이브리드 검색

하이브리드 검색은 벡터 유사도 검색과 어휘 검색(BM25)을 결합합니다. Elasticsearch는 RRF(Reciprocal Rank Fusion) 알고리즘을 사용하여 두 검색 결과를 통합합니다.

### 기본 하이브리드 검색

여러 `knn` 절과 `query` 절은 분리 연산(boolean OR)으로 결합됩니다:

```json
POST image-index/_search
{
  "query": {
    "match": {
      "title": {
        "query": "mountain lake",
        "boost": 0.9
      }
    }
  },
  "knn": {
    "field": "image-vector",
    "query_vector": [54, 10, -2],
    "k": 5,
    "num_candidates": 50,
    "boost": 0.1
  },
  "size": 10
}
```

점수 계산: `score = 0.9 * match_score + 0.1 * knn_score`

### Reciprocal Rank Fusion (RRF)

RRF는 원시 점수를 무시하고 각 목록에서 문서의 순위에 초점을 맞춥니다. 목록의 상위에 있는 문서는 강하게 보상받고, 여러 목록에 나타나는 문서는 추가 보너스를 받습니다.

#### RRF의 장점

- 점수 보정이나 정규화가 필요 없습니다.
- 문서를 결과 집합에서의 순위에 따라 간단히 점수를 매깁니다.
- 가중치를 구성하지 않고도 즉시 사용할 수 있습니다.

### RRF를 사용한 하이브리드 검색

```json
POST my-index/_search
{
  "retriever": {
    "rrf": {
      "retrievers": [
        {
          "standard": {
            "query": {
              "match": {
                "title": "mountain lake"
              }
            }
          }
        },
        {
          "knn": {
            "field": "image-vector",
            "query_vector": [54, 10, -2],
            "k": 5,
            "num_candidates": 50
          }
        }
      ],
      "rank_window_size": 50,
      "rank_constant": 20
    }
  }
}
```

### 가중치가 있는 RRF (Weighted RRF)

각 retriever에 서로 다른 중요도 수준을 할당할 수 있습니다:

```json
POST my-index/_search
{
  "retriever": {
    "rrf": {
      "retrievers": [
        {
          "standard": {
            "query": {
              "match": {
                "title": "mountain lake"
              }
            }
          },
          "weight": 0.3
        },
        {
          "knn": {
            "field": "image-vector",
            "query_vector": [54, 10, -2],
            "k": 5,
            "num_candidates": 50
          },
          "weight": 0.7
        }
      ]
    }
  }
}
```

---

## Retriever를 사용한 검색

Retriever는 Elasticsearch 8.16.0에서 정식 출시된 새로운 추상화 레이어입니다. 단일 `_search` API 호출 내에서 다단계 검색 파이프라인을 구성할 수 있습니다.

### Retriever 타입

| Retriever | 설명 |
|---|---|
| `standard` | 기존 Query DSL을 사용하는 표준 검색 |
| `knn` | kNN 검색 결과 반환 |
| `rrf` | RRF 알고리즘으로 여러 retriever 결합 |
| `linear` | 가중치 합으로 여러 retriever 결합 |
| `pinned` | 특정 문서를 항상 상위에 배치 |
| `text_similarity_reranker` | 텍스트 유사도로 재순위 |

### kNN Retriever 예제

```json
POST my-index/_search
{
  "retriever": {
    "knn": {
      "field": "my_vector",
      "query_vector": [0.1, 0.2, 0.3],
      "k": 10,
      "num_candidates": 100
    }
  }
}
```

### 복합 Retriever 예제

여러 retriever를 중첩하여 복잡한 검색 로직을 구현할 수 있습니다:

```json
POST my-index/_search
{
  "retriever": {
    "rrf": {
      "retrievers": [
        {
          "standard": {
            "query": {
              "multi_match": {
                "query": "machine learning",
                "fields": ["title", "content"]
              }
            }
          }
        },
        {
          "knn": {
            "field": "title_vector",
            "query_vector": [0.1, 0.2, ...],
            "k": 10,
            "num_candidates": 50
          }
        },
        {
          "knn": {
            "field": "content_vector",
            "query_vector": [0.3, 0.4, ...],
            "k": 10,
            "num_candidates": 50
          }
        }
      ]
    }
  }
}
```

---

## 중첩(Nested) 벡터 검색

Elasticsearch 8.11부터 중첩 벡터가 지원되어 필드당 여러 벡터를 저장할 수 있습니다. 이는 문서를 청크로 나누어 각 청크에 대한 벡터를 저장하는 데 유용합니다.

### 중첩 벡터 매핑

```json
PUT my-passages-index
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text"
      },
      "passages": {
        "type": "nested",
        "properties": {
          "text": {
            "type": "text"
          },
          "embedding": {
            "type": "dense_vector",
            "dims": 384,
            "similarity": "cosine"
          }
        }
      }
    }
  }
}
```

### 문서 인덱싱

```json
PUT my-passages-index/_doc/1
{
  "title": "Elasticsearch 가이드",
  "passages": [
    {
      "text": "Elasticsearch는 분산 검색 엔진입니다.",
      "embedding": [0.1, 0.2, ...]
    },
    {
      "text": "벡터 검색을 지원합니다.",
      "embedding": [0.3, 0.4, ...]
    }
  ]
}
```

### 중첩 kNN 검색

```json
POST my-passages-index/_search
{
  "knn": {
    "field": "passages.embedding",
    "query_vector": [0.2, 0.3, ...],
    "k": 5,
    "num_candidates": 50,
    "inner_hits": {
      "_source": false,
      "fields": ["passages.text"]
    }
  }
}
```

### inner_hits 사용

`inner_hits`를 사용하면 가장 잘 일치하는 중첩 문서(청크)를 반환할 수 있습니다. 이는 BM25 쿼리의 하이라이팅과 유사한 역할을 합니다.

> 제한사항:
> - 중첩 kNN은 `score_mode=max`만 지원합니다.
> - k개의 최상위 문서가 반환되며, 각 문서의 점수는 가장 가까운 패시지 벡터로 매겨집니다.

---

## 시맨틱 검색

시맨틱 검색은 검색어의 의도와 맥락적 의미를 기반으로 데이터를 찾는 검색 방법입니다. 어휘 검색(lexical search)과 달리 단어의 정확한 일치가 아닌 의미적 유사성을 기반으로 합니다.

### semantic_text 필드

`semantic_text` 필드 타입은 Elasticsearch에서 시맨틱 검색을 가장 쉽게 사용하는 방법입니다. 인덱싱과 쿼리 시점에 inference 엔드포인트를 통해 임베딩 생성을 자동으로 처리합니다.

#### 매핑 설정

```json
PUT my-semantic-index
{
  "mappings": {
    "properties": {
      "content": {
        "type": "semantic_text",
        "inference_id": "my-inference-endpoint"
      }
    }
  }
}
```

#### 특징

- 벡터를 수동으로 생성하거나 저장할 필요가 없습니다.
- 자동 청킹: 긴 문서는 250단어 섹션으로 나뉘며, 100단어가 겹칩니다.
- 이 겹침은 입력 텍스트의 중요한 맥락 정보가 하드 브레이크로 손실되지 않도록 합니다.

### inference 엔드포인트 생성

다양한 임베딩 모델 제공업체를 사용할 수 있습니다:

#### Cohere 예제

```json
PUT _inference/text_embedding/cohere_embeddings
{
  "service": "cohere",
  "service_settings": {
    "api_key": "<your-api-key>",
    "model_id": "embed-english-v3.0"
  }
}
```

#### OpenAI 예제

```json
PUT _inference/text_embedding/openai_embeddings
{
  "service": "openai",
  "service_settings": {
    "api_key": "<your-api-key>",
    "model_id": "text-embedding-3-small"
  }
}
```

### 시맨틱 쿼리

```json
GET my-semantic-index/_search
{
  "query": {
    "semantic": {
      "field": "content",
      "query": "Elasticsearch의 벡터 검색 기능"
    }
  }
}
```

---

## Sparse Vector와 ELSER

### ELSER (Elastic Learned Sparse EncodeR)

ELSER는 Elastic에서 개발한 NLP 모델로, 희소 벡터 표현을 사용하여 시맨틱 검색을 수행할 수 있습니다.

#### ELSER의 특징

- 도메인 외 모델: 특정 도메인에 대한 미세 조정 없이 사용 가능
- 희소 벡터 생성: 약 30,000개 용어의 어휘에서 대부분이 0인 벡터 생성
- 영어 전용: 현재 영어에 대해서만 사용 가능

#### ELSER의 작동 방식

ELSER는 입력 텍스트를 다양한 훈련 데이터 세트에서 자주 함께 발생하는 용어 컬렉션으로 확장합니다. 모델이 확장하는 용어는 검색어의 동의어가 아니라 관련성을 포착하는 학습된 연관성입니다.

### sparse_vector 필드 타입

```json
PUT my-elser-index
{
  "mappings": {
    "properties": {
      "content": {
        "type": "text"
      },
      "content_embedding": {
        "type": "sparse_vector"
      }
    }
  }
}
```

> 중요: ELSER 출력은 반드시 `sparse_vector` 또는 `rank_features` 필드 타입으로 수집해야 합니다. 그렇지 않으면 Elasticsearch가 토큰-가중치 쌍을 대량의 필드로 해석합니다.

### ELSER inference 엔드포인트 생성

```json
PUT _inference/sparse_embedding/elser_embeddings
{
  "service": "elser",
  "service_settings": {
    "num_allocations": 1,
    "num_threads": 1
  }
}
```

### Sparse Vector 쿼리

```json
GET my-elser-index/_search
{
  "query": {
    "sparse_vector": {
      "field": "content_embedding",
      "inference_id": "elser_embeddings",
      "query": "how to perform semantic search"
    }
  }
}
```

### Dense Vector vs Sparse Vector

| 특성 | Dense Vector | Sparse Vector |
|---|---|---|
| 표현 방식 | 모든 차원에 값이 있는 밀집 배열 | 대부분이 0인 희소 배열 |
| 저장 효율 | 상대적으로 큼 | 일반적으로 더 컴팩트 |
| 모델 예시 | OpenAI, Cohere, sentence-transformers | ELSER, SPLADE |
| 검색 방식 | kNN 검색 | sparse_vector 쿼리 |

---

## 성능 튜닝

### 메모리 요구사항

HNSW에 필요한 메모리는 다음과 같이 추정됩니다:

```
메모리 = 1.1 * (4 * 차원 + 8 * m) 바이트/벡터
```

예: 100만 개의 벡터, 256 차원, m=16인 경우
```
메모리 = 1.1 * (4 * 256 + 8 * 16) * 1,000,000 ≈ 1.27GB
```

> 참고: 벡터 인덱스는 기존 텍스트 인덱스보다 훨씬 더 많은 메모리를 소비합니다. 벡터 차원과 HNSW 파라미터에 따라 4-8배의 메모리 오버헤드를 예상해야 합니다.

### HNSW 파라미터 튜닝

#### m (최대 연결 수)

- 증가: 리콜 향상, 메모리 사용량 증가, 인덱싱/검색 지연 시간 증가
- 감소: 메모리 절약, 리콜 감소

#### ef_construction

- 증가: 그래프 품질 향상, 인덱싱 지연 시간 증가
- 감소: 인덱싱 속도 향상, 그래프 품질 저하

#### num_candidates

- 증가: 정확도/리콜 향상, 검색 지연 시간 증가
- 감소: 검색 속도 향상, 정확도/리콜 감소

### 벡터 차원 줄이기

kNN 검색의 속도는 벡터 차원 수에 선형적으로 비례합니다. 각 유사도 계산이 두 벡터의 각 요소를 고려하기 때문입니다.

가능하면 낮은 차원의 벡터를 사용하는 것이 좋습니다:
- 일부 임베딩 모델은 다양한 "크기"로 제공됩니다.
- PCA와 같은 차원 축소 기술을 실험해 볼 수 있습니다.

### 세그먼트 관리

근사 kNN 검색의 경우, Elasticsearch는 각 세그먼트의 벡터 값을 별도의 HNSW 그래프로 저장합니다. 따라서 세그먼트 수가 적을수록 kNN 검색이 더 빠릅니다.

```json
PUT my-index/_settings
{
  "index": {
    "merge.policy.max_merged_segment": "10gb"
  }
}
```

기본값은 5GB이지만, 더 큰 차원의 벡터에는 너무 작을 수 있습니다.

### 양자화로 메모리 절감

기본 `element_type`은 `float`이지만, 인덱스 시점에 자동으로 양자화될 수 있습니다:

| 양자화 | 메모리 절감 | 정확도 손실 |
|---|---|---|
| int8 | 4배 (75%) | 적음 |
| int4 | 8배 (87%) | 중간 |
| bbq | 32배 (96%) | 큼 |

---

## 벡터 양자화(Quantization)

양자화는 벡터를 압축하여 메모리 사용량을 줄이는 기술입니다.

### 양자화 타입

#### Scalar Quantization (int8, int4)

각 차원을 더 작은 정수로 양자화합니다:

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 1024,
        "index_options": {
          "type": "int8_hnsw"
        }
      }
    }
  }
}
```

#### Better Binary Quantization (BBQ)

BBQ는 Elasticsearch 8.16에서 도입된 새로운 접근 방식입니다:

- float32 차원을 비트로 줄여 ~95% 메모리 절감
- Product Quantization(PQ)보다 20-30배 빠른 양자화 시간
- 2-5배 빠른 쿼리 속도
- 추가적인 정확도 손실 없음

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 1024,
        "index_options": {
          "type": "bbq_hnsw"
        }
      }
    }
  }
}
```

### Byte 벡터

Elasticsearch 8.6에서 도입된 8비트 정수 차원의 벡터입니다:

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 256,
        "element_type": "byte"
      }
    }
  }
}
```

- 각 차원의 범위: [-128, 127]
- float 벡터 대비 4배 작음

### Bit 벡터

각 차원이 단일 비트인 벡터입니다:

```json
PUT my-index
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 256,
        "element_type": "bit"
      }
    }
  }
}
```

- float 벡터 대비 32배 크기 절감
- Hamming 거리를 사용하여 유사도 계산
- 일반적인 cosineSimilarity나 dotProduct 사용 불가

### 양자화 선택 가이드

| 시나리오 | 권장 타입 |
|---|---|
| 정확도 우선 | `hnsw` |
| 균형 잡힌 성능 | `int8_hnsw` (기본값) |
| 메모리 제약 | `int4_hnsw` 또는 `bbq_hnsw` |
| 대규모 데이터셋 | `bbq_hnsw` 또는 `bbq_disk` |
| 정확한 결과 필요 | `flat` |

### 오버샘플링과 재순위

양자화된 벡터의 정확도 손실을 완화하기 위해 오버샘플링과 재순위를 사용할 수 있습니다:

```json
POST my-index/_search
{
  "knn": {
    "field": "my_vector",
    "query_vector": [0.1, 0.2, ...],
    "k": 10,
    "num_candidates": 200,
    "rescore_vector": {
      "oversample": 2.0
    }
  }
}
```

---

## 참고 자료

### 공식 문서

- [Elasticsearch kNN 검색](https://www.elastic.co/docs/solutions/search/vector/knn)
- [kNN 쿼리 레퍼런스](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-knn-query)
- [Dense Vector 필드 타입](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/dense-vector)
- [근사 kNN 검색 튜닝](https://www.elastic.co/docs/deploy-manage/production-guidance/optimize-performance/approximate-knn-search)
- [Retriever 개요](https://www.elastic.co/docs/solutions/search/retrievers-overview)
- [하이브리드 검색](https://www.elastic.co/docs/solutions/search/hybrid-search)
- [ELSER를 사용한 시맨틱 검색](https://www.elastic.co/docs/solutions/search/semantic-search/semantic-search-elser-ingest-pipelines)
- [semantic_text를 사용한 시맨틱 검색](https://www.elastic.co/docs/solutions/search/semantic-search/semantic-search-semantic-text)
- [RRF (Reciprocal Rank Fusion)](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/reciprocal-rank-fusion)

### Elasticsearch Labs

- [벡터 검색 설정 방법](https://www.elastic.co/search-labs/blog/vector-search-set-up-elasticsearch)
- [정확한 kNN vs 근사 kNN 선택](https://www.elastic.co/search-labs/blog/knn-exact-vs-approximate-search)
- [HNSW 그래프로 성능 향상](https://www.elastic.co/search-labs/blog/hnsw-graph)
- [임베딩을 Elasticsearch 필드 타입에 매핑하기](https://www.elastic.co/search-labs/blog/mapping-embeddings-to-elasticsearch-field-types)
- [Elasticsearch Retriever 사용법](https://www.elastic.co/search-labs/blog/elasticsearch-retrievers)
- [BBQ (Better Binary Quantization)](https://www.elastic.co/search-labs/blog/better-binary-quantization-lucene-elasticsearch)
- [Bit 벡터](https://www.elastic.co/search-labs/blog/bit-vectors-in-elasticsearch)
