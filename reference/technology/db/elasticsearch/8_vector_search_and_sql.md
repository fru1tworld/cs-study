# Elasticsearch 벡터 검색과 SQL

## Elasticsearch kNN 검색 및 벡터 검색 가이드

### 목차

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

### 개요

Elasticsearch는 k-최근접 이웃(k-Nearest Neighbor, kNN) 검색을 통해 벡터 유사도 기반 검색을 지원 → 시맨틱 검색·이미지 유사도 검색·추천 시스템 등 다양한 용도로 활용됨.

#### 벡터 검색의 두 가지 방법

Elasticsearch는 kNN 검색을 위한 두 가지 방법을 지원:

1. 근사 kNN (Approximate kNN): `knn` 옵션·`knn` 쿼리·`knn` retriever를 사용하는 빠르고 확장 가능한 유사도 검색. 대부분의 프로덕션 워크로드에 적합.

2. 정확한 무차별 대입 kNN (Exact, brute-force kNN): `script_score` 쿼리와 벡터 함수를 사용. 소규모 데이터셋이나 정확한 스코어링이 필요한 경우에 적합.

---

### kNN 검색이란?

kNN 검색은 쿼리 벡터와 가장 유사한 k개의 벡터를 찾는 검색 방법. 유사도 메트릭(코사인 유사도, 유클리드 거리 등)을 사용해 벡터 간의 거리를 측정.

#### 기본 kNN 검색 예제

먼저, dense_vector 필드가 있는 인덱스를 생성.

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

문서를 인덱싱.

```json
POST image-index/_bulk?refresh=true
{ "index": { "_id": "1" } }
{ "image-vector": [1, 5, -20], "title": "moose family", "file-type": "png" }
{ "index": { "_id": "2" } }
{ "image-vector": [42, 8, -15], "title": "alpine lake", "file-type": "png" }
{ "index": { "_id": "3" } }
{ "image-vector": [15, 11, 23], "title": "full moon", "file-type": "jpg" }
```

kNN 검색을 실행.

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

### 근사 kNN 검색

#### HNSW 알고리즘

Elasticsearch는 효율적인 근사 kNN 검색을 위해 HNSW(Hierarchical Navigable Small World) 알고리즘을 사용. HNSW는 그래프 기반 알고리즘으로, 서로 가까운 요소들 간의 링크를 여러 레이어에서 유지.

- 각 레이어는 연결된 요소들을 포함 → 아래 레이어의 요소들과도 연결됨
- 각 레이어는 더 많은 요소를 포함 → 최하위 레이어는 모든 요소를 포함
- 대부분의 근사 방법과 마찬가지로, HNSW는 정확도를 희생하여 속도를 얻음

#### 근사 kNN의 작동 방식

kNN 검색 API는 먼저 각 샤드에서 `num_candidates` 수만큼 근사 최근접 이웃 후보를 찾음 → 후보 벡터들과 쿼리 벡터 간의 유사도를 계산해 각 샤드에서 가장 유사한 k개 결과를 선택 → 샤드별 결과를 병합해 전역 top-k 최근접 이웃을 반환.

#### num_candidates 파라미터

`num_candidates`는 각 샤드에서 kNN 검색 수행 시 고려할 최근접 이웃 후보의 수. 10,000을 초과 불가.

- num_candidates 증가: 정확도 향상, 검색 속도 저하
- num_candidates 감소: 검색 속도 향상, 정확도 저하 가능

기본값은 `k`가 설정된 경우 `1.5 * k`, 그렇지 않으면 `1.5 * size`.

---

### 정확한 무차별 대입(Brute-force) kNN 검색

정확한 벡터 검색은 전체 벡터 공간에 대해 선형 검색(무차별 대입 검색)을 수행. 쿼리 벡터가 저장된 모든 벡터와 비교되어 가장 가까운 이웃을 찾음.

#### script_score 쿼리 사용

`script_score` 쿼리를 사용하면 각 일치하는 문서를 스캔해 벡터 함수를 계산해야 하므로 검색 속도가 느려질 수 있음 → 쿼리를 사용해 일치하는 문서 수를 제한하면 대기 시간 개선 가능.

#### 사용 가능한 벡터 함수

dense 벡터에 접근하는 권장 방법은 `cosineSimilarity`, `dotProduct`, `l1norm`, `l2norm` 함수 사용.

##### cosineSimilarity 예제

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

> 참고: 코사인 유사도는 음수가 될 수 있음 → 점수가 음수가 되는 것을 방지하기 위해 스크립트에 `+ 1.0`을 추가.

##### dotProduct 예제

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

##### l1norm 및 l2norm 예제

`l1norm`과 `l2norm`은 유사도가 아닌 거리를 나타냄 → 벡터가 유사할수록 점수가 낮아짐 → 출력을 역으로 변환 필요:

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

#### 언제 정확한 kNN을 사용해야 하는가?

- 검색할 문서가 10,000개 미만인 경우
- 데이터를 소규모 하위 집합으로 필터링하는 경우
- 정밀한 스코어링이 필요한 경우

근사 검색은 문서 규모 확장에 유리 → 문서가 많거나 크게 늘어날 것으로 예상되는 경우 근사 검색 권장.

---

### Dense Vector 필드 타입

`dense_vector` 필드 타입은 숫자 값의 밀집 벡터를 저장 → 주로 k-최근접 이웃(kNN) 검색에 사용됨.

> 참고: `dense_vector` 타입은 집계(aggregation)나 정렬(sorting)을 지원하지 않음.

#### 기본 매핑

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

#### 전체 매핑 옵션

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

#### 파라미터 설명

##### dims (차원)

- 벡터 차원의 수를 지정
- `element_type`이 `bit`인 경우 8의 배수여야 함
- 최댓값은 4096

##### element_type (요소 타입)

- `float`: 4바이트 부동소수점 값(기본값) · 크기 4바이트/차원
- `byte`: 1바이트 정수 값(-128 ~ 127) · 크기 1바이트/차원
- `bit`: 단일 비트 · 크기 1비트/차원

##### similarity (유사도 메트릭)

- `l2_norm`: L2 거리(유클리드 거리) 기반
  - 점수 계산: `1 / (1 + l2_norm(query, vector)^2)`
- `dot_product`: 두 단위 벡터의 내적
  - 단위 길이 벡터에만 사용 가능
- `cosine`: 코사인 유사도(기본값)
  - 점수 계산: `(1 + cosine(query, vector)) / 2`
- `max_inner_product`: 최대 내적
  - 정규화되지 않은 벡터에 사용 가능

> 참고: `cosine` 유사도 사용 시 Elasticsearch는 인덱싱 중에 벡터를 자동으로 단위 길이로 정규화함 → 내부적으로 더 효율적인 `dot_product`로 유사도를 계산함.

#### index_options (인덱스 옵션)

kNN 인덱싱 알고리즘을 구성하는 선택적 객체.

##### 인덱스 타입

- `hnsw`: HNSW 알고리즘 사용, 모든 element_type 지원
- `int8_hnsw`: HNSW + 자동 8비트 양자화(기본값, 9.0) · 메모리 절감 4배
- `int4_hnsw`: HNSW + 자동 4비트 양자화 · 메모리 절감 8배
- `bbq_hnsw`: HNSW + 자동 이진 양자화(384차원 이상 기본값) · 메모리 절감 32배
- `flat`: 무차별 대입 검색 알고리즘, 정확한 kNN
- `int8_flat`: 무차별 대입 + 자동 8비트 양자화 · 메모리 절감 4배
- `int4_flat`: 무차별 대입 + 자동 4비트 양자화 · 메모리 절감 8배
- `bbq_flat`: 무차별 대입 + 자동 이진 양자화 · 메모리 절감 32배
- `bbq_disk`: 디스크 기반 이진 양자화, 낮은 메모리 사용 · HNSW보다 낮은 메모리

##### HNSW 파라미터

- `m`: 그래프의 각 노드가 가질 수 있는 최대 엣지 수 (기본값 16)
- `ef_construction`: 노드 추가 시 엣지 후보 큐의 크기 (기본값 100)

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

#### 인덱스 타입 업그레이드 경로

Update Mapping API로 인덱스 타입 업그레이드 가능:

```
flat → int8_flat → int4_flat → bbq_flat → hnsw → int8_hnsw → int4_hnsw → bbq_hnsw
```

> 참고: 타입을 변경해도 이미 인덱싱된 벡터는 재인덱싱되지 않음 → 기존 벡터는 원래 타입을 유지, 변경 후 인덱싱된 벡터만 새 타입을 사용함.

---

### kNN 쿼리

`knn` 쿼리는 쿼리 벡터에 가장 가까운 k개의 벡터를 찾음 → 인덱싱된 dense_vector에 대한 근사 검색으로 유사도 메트릭을 측정함.

#### 기본 kNN 쿼리

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

#### kNN 쿼리 vs 최상위 kNN 검색

- 최상위 knn 섹션: 근사 kNN 검색을 수행하는 권장 방법
- knn 쿼리: 다른 쿼리와 결합하거나 `semantic_text` 필드에 대해 kNN 검색을 수행해야 하는 전문가 케이스용

`knn` 쿼리는 `k` 파라미터가 선택사항이며, 지정하지 않으면 검색 요청의 `size` 값을 따름.

#### kNN 쿼리 파라미터

- `field` (필수): 검색할 dense_vector 필드 이름
- `query_vector` (필수*): 쿼리 벡터(배열)
- `query_vector_builder` (필수*): 쿼리 벡터를 빌드하는 설정 객체
- `k` (선택): 반환할 최근접 이웃 수
- `num_candidates` (선택): 각 샤드에서 고려할 후보 수 (최대 10,000)
- `filter` (선택): 사전 필터링 쿼리
- `boost` (선택): 점수 배수 (기본값: 1.0)

> 참고: `query_vector` 또는 `query_vector_builder` 중 하나를 제공해야 함.

#### query_vector_builder 사용

텍스트 임베딩 모델로 쿼리 시점에 벡터를 생성할 수 있음:

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

### 필터링을 사용한 kNN 검색

kNN 쿼리와 일치하는 문서를 필터링하는 두 가지 방법:

1. 사전 필터링 (Pre-filtering): 근사 kNN 검색 중에 필터가 적용되어 k개의 일치하는 문서가 반환되도록 보장.
2. 사후 필터링 (Post-filtering): 근사 kNN 단계 후에 필터가 적용되어 k개 미만의 결과가 반환될 수 있음.

#### 사전 필터링 예제

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

#### 복합 필터 예제

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

#### 필터링 성능 고려사항

HNSW 인덱스를 사용한 근사 kNN 검색에 필터를 적용하면 성능이 저하될 수 있음 → 필터를 충족하면서 `num_candidates` 수만큼 후보를 확보하기 위해 그래프를 더 많이 탐색해야 하기 때문.

Lucene은 세그먼트별로 다음 전략을 구현함:
- 필터링된 문서 수가 `num_candidates` 이하인 경우 → HNSW 그래프를 건너뛰고 필터링된 문서에 대해 무차별 대입 검색을 수행
- HNSW 그래프를 탐색하는 동안 탐색된 노드 수가 필터를 충족하는 문서 수를 초과 → 그래프 탐색을 중단하고 무차별 대입 검색으로 전환

---

### 하이브리드 검색

하이브리드 검색은 벡터 유사도 검색과 어휘 검색(BM25)을 결합 → Elasticsearch는 RRF(Reciprocal Rank Fusion) 알고리즘으로 두 검색 결과를 통합함.

#### 기본 하이브리드 검색

여러 `knn` 절과 `query` 절은 논리합(boolean OR)으로 결합됨:

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

#### Reciprocal Rank Fusion (RRF)

RRF는 원시 점수를 무시하고 각 목록에서 문서의 순위에 초점을 맞춤 → 목록 상위에 있는 문서는 강하게 보상받고, 여러 목록에 나타나는 문서는 추가 보너스를 받음.

##### RRF의 장점

- 점수 보정이나 정규화가 불필요
- 문서를 결과 집합에서의 순위에 따라 간단히 점수를 매김
- 가중치를 구성하지 않고도 즉시 사용 가능

#### RRF를 사용한 하이브리드 검색

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

#### 가중치가 있는 RRF (Weighted RRF)

각 retriever에 서로 다른 중요도 수준을 할당 가능:

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

### Retriever를 사용한 검색

Retriever는 Elasticsearch 8.16.0에서 정식 출시된 새로운 추상화 레이어 → 단일 `_search` API 호출 내에서 다단계 검색 파이프라인을 구성 가능.

#### Retriever 타입

- `standard`: 기존 Query DSL을 사용하는 표준 검색
- `knn`: kNN 검색 결과 반환
- `rrf`: RRF 알고리즘으로 여러 retriever 결합
- `linear`: 가중치 합으로 여러 retriever 결합
- `pinned`: 특정 문서를 항상 상위에 배치
- `text_similarity_reranker`: 텍스트 유사도로 재순위

#### kNN Retriever 예제

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

#### 복합 Retriever 예제

여러 retriever를 중첩해 복잡한 검색 로직 구현 가능:

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

### 중첩(Nested) 벡터 검색

Elasticsearch 8.11부터 중첩 벡터가 지원되어 필드당 여러 벡터를 저장 가능 → 문서를 청크로 나누어 각 청크에 대한 벡터를 저장하는 데 유용함.

#### 중첩 벡터 매핑

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

#### 문서 인덱싱

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

#### 중첩 kNN 검색

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

#### inner_hits 사용

`inner_hits`를 사용하면 가장 잘 일치하는 중첩 문서(청크)를 반환 가능 → BM25 쿼리의 하이라이팅과 유사한 역할.

> 제한사항:
> - 중첩 kNN은 `score_mode=max`만 지원
> - k개의 최상위 문서가 반환되며, 각 문서의 점수는 가장 가까운 패시지 벡터로 매겨짐

---

### 시맨틱 검색

시맨틱 검색은 검색어의 의도와 맥락적 의미를 기반으로 데이터를 찾는 검색 방법 → 어휘 검색(lexical search)과 달리 단어의 정확한 일치가 아닌 의미적 유사성을 기반으로 함.

#### semantic_text 필드

`semantic_text` 필드 타입은 Elasticsearch에서 시맨틱 검색을 가장 쉽게 사용하는 방법 → 인덱싱과 쿼리 시점에 inference 엔드포인트를 통해 임베딩 생성을 자동으로 처리함.

##### 매핑 설정

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

##### 특징

- 벡터를 수동으로 생성하거나 저장할 필요 없음
- 자동 청킹: 긴 문서는 250단어 섹션으로 나뉘며, 100단어가 겹침
- 겹침 이유: 청크 경계에서 중요한 맥락 정보가 손실되지 않도록 하기 위함

#### inference 엔드포인트 생성

다양한 임베딩 모델 제공업체를 사용할 수 있음:

##### Cohere 예제

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

##### OpenAI 예제

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

#### 시맨틱 쿼리

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

### Sparse Vector와 ELSER

#### ELSER (Elastic Learned Sparse EncodeR)

ELSER는 Elastic에서 개발한 NLP 모델 → 희소 벡터 표현을 사용하여 시맨틱 검색을 수행 가능.

##### ELSER의 특징

- 도메인 외 모델: 특정 도메인에 대한 미세 조정 없이 사용 가능
- 희소 벡터 생성: 약 30,000개 용어의 어휘에서 대부분이 0인 벡터 생성
- 영어 전용: 현재 영어에 대해서만 사용 가능

##### ELSER의 작동 방식

ELSER는 입력 텍스트를 다양한 훈련 데이터에서 함께 등장하는 경향이 있는 용어들로 확장함 → 이때 확장되는 용어는 단순한 동의어가 아니라, 관련성을 포착하도록 학습된 연관 용어들.

#### sparse_vector 필드 타입

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

> 중요: ELSER 출력은 반드시 `sparse_vector` 또는 `rank_features` 필드 타입으로 수집해야 함 → 그렇지 않으면 Elasticsearch가 토큰-가중치 쌍을 대량의 필드로 해석함.

#### ELSER inference 엔드포인트 생성

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

#### Sparse Vector 쿼리

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

#### Dense Vector vs Sparse Vector

- Dense Vector
  - 표현 방식: 모든 차원에 값이 있는 밀집 배열
  - 저장 효율: 상대적으로 큼
  - 모델 예시: OpenAI, Cohere, sentence-transformers
  - 검색 방식: kNN 검색
- Sparse Vector
  - 표현 방식: 대부분이 0인 희소 배열
  - 저장 효율: 일반적으로 더 컴팩트
  - 모델 예시: ELSER, SPLADE
  - 검색 방식: `sparse_vector` 쿼리

---

### 성능 튜닝

#### 메모리 요구사항

HNSW에 필요한 메모리는 다음과 같이 추정됨:

```
메모리 = 1.1 * (4 * 차원 + 8 * m) 바이트/벡터
```

예: 100만 개의 벡터, 256 차원, m=16인 경우
```
메모리 = 1.1 * (4 * 256 + 8 * 16) * 1,000,000 ≈ 1.27GB
```

> 참고: 벡터 인덱스는 기존 텍스트 인덱스보다 훨씬 더 많은 메모리를 소비함 → 벡터 차원과 HNSW 파라미터에 따라 4~8배의 메모리 오버헤드를 예상해야 함.

#### HNSW 파라미터 튜닝

##### m (최대 연결 수)

- 증가: 리콜 향상, 메모리 사용량 증가, 인덱싱/검색 지연 시간 증가
- 감소: 메모리 절약, 리콜 감소

##### ef_construction

- 증가: 그래프 품질 향상, 인덱싱 지연 시간 증가
- 감소: 인덱싱 속도 향상, 그래프 품질 저하

##### num_candidates

- 증가: 정확도/리콜 향상, 검색 지연 시간 증가
- 감소: 검색 속도 향상, 정확도/리콜 감소

#### 벡터 차원 줄이기

kNN 검색 속도는 벡터 차원 수에 선형적으로 비례함 → 유사도를 계산할 때 두 벡터의 모든 요소를 비교해야 하기 때문.

가능하면 낮은 차원의 벡터 사용 권장:
- 일부 임베딩 모델은 다양한 "크기"로 제공됨
- PCA와 같은 차원 축소 기술을 실험 가능

#### 세그먼트 관리

근사 kNN 검색의 경우, Elasticsearch는 각 세그먼트의 벡터 값을 별도의 HNSW 그래프로 저장함 → 세그먼트 수가 적을수록 kNN 검색이 더 빠름.

```json
PUT my-index/_settings
{
  "index": {
    "merge.policy.max_merged_segment": "10gb"
  }
}
```

기본값은 5GB → 더 큰 차원의 벡터에는 너무 작을 수 있음.

#### 양자화로 메모리 절감

기본 `element_type`은 `float` → 인덱스 시점에 자동으로 양자화될 수 있음:

- int8: 메모리 절감 4배(75%) · 정확도 손실 적음
- int4: 메모리 절감 8배(87%) · 정확도 손실 중간
- bbq: 메모리 절감 32배(96%) · 정확도 손실 큼

---

### 벡터 양자화(Quantization)

양자화는 벡터를 압축하여 메모리 사용량을 줄이는 기술.

#### 양자화 타입

##### Scalar Quantization (int8, int4)

각 차원을 더 작은 정수로 양자화:

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

##### Better Binary Quantization (BBQ)

BBQ는 Elasticsearch 8.16에서 도입된 새로운 접근 방식:

- float32 차원을 비트로 줄여 약 95% 메모리 절감
- Product Quantization(PQ) 대비 20~30배 빠른 양자화 시간
- 2~5배 빠른 쿼리 속도
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

#### Byte 벡터

Elasticsearch 8.6에서 도입된 8비트 정수 차원의 벡터:

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

#### Bit 벡터

각 차원이 단일 비트인 벡터:

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

#### 양자화 선택 가이드

- 정확도 우선: `hnsw`
- 균형 잡힌 성능: `int8_hnsw` (기본값)
- 메모리 제약: `int4_hnsw` 또는 `bbq_hnsw`
- 대규모 데이터셋: `bbq_hnsw` 또는 `bbq_disk`
- 정확한 결과 필요: `flat`

#### 오버샘플링과 재순위

양자화된 벡터의 정확도 손실을 완화하기 위해 오버샘플링과 재순위 사용 가능:

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

### 참고 자료

#### 공식 문서

- [Elasticsearch kNN 검색](https://www.elastic.co/docs/solutions/search/vector/knn)
- [kNN 쿼리 레퍼런스](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-knn-query)
- [Dense Vector 필드 타입](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/dense-vector)
- [근사 kNN 검색 튜닝](https://www.elastic.co/docs/deploy-manage/production-guidance/optimize-performance/approximate-knn-search)
- [Retriever 개요](https://www.elastic.co/docs/solutions/search/retrievers-overview)
- [하이브리드 검색](https://www.elastic.co/docs/solutions/search/hybrid-search)
- [ELSER를 사용한 시맨틱 검색](https://www.elastic.co/docs/solutions/search/semantic-search/semantic-search-elser-ingest-pipelines)
- [semantic_text를 사용한 시맨틱 검색](https://www.elastic.co/docs/solutions/search/semantic-search/semantic-search-semantic-text)
- [RRF (Reciprocal Rank Fusion)](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/reciprocal-rank-fusion)

#### Elasticsearch Labs

- [벡터 검색 설정 방법](https://www.elastic.co/search-labs/blog/vector-search-set-up-elasticsearch)
- [정확한 kNN vs 근사 kNN 선택](https://www.elastic.co/search-labs/blog/knn-exact-vs-approximate-search)
- [HNSW 그래프로 성능 향상](https://www.elastic.co/search-labs/blog/hnsw-graph)
- [임베딩을 Elasticsearch 필드 타입에 매핑하기](https://www.elastic.co/search-labs/blog/mapping-embeddings-to-elasticsearch-field-types)
- [Elasticsearch Retriever 사용법](https://www.elastic.co/search-labs/blog/elasticsearch-retrievers)
- [BBQ (Better Binary Quantization)](https://www.elastic.co/search-labs/blog/better-binary-quantization-lucene-elasticsearch)
- [Bit 벡터](https://www.elastic.co/search-labs/blog/bit-vectors-in-elasticsearch)

---

## SQL

> 원문: https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-sql.html

---

### 목차

1. [개요](#1-개요)
2. [시작하기](#2-시작하기)
3. [개념 및 용어](#3-개념-및-용어)
4. [보안](#4-보안)
5. [SQL REST API](#5-sql-rest-api)
6. [SQL Translate API](#6-sql-translate-api)
7. [SQL CLI](#7-sql-cli)
8. [SQL JDBC](#8-sql-jdbc)
9. [SQL ODBC](#9-sql-odbc)
10. [클라이언트 애플리케이션](#10-클라이언트-애플리케이션)
11. [SQL 언어](#11-sql-언어)
12. [함수 및 연산자](#12-함수-및-연산자)
13. [제한 사항](#13-제한-사항)

---

### 1. 개요

#### 1.1 Elasticsearch SQL이란?

Elasticsearch SQL은 Elasticsearch에서 SQL과 유사한 쿼리를 실시간으로 실행할 수 있게 해주는 X-Pack 구성 요소 → SQL과 Elasticsearch 양쪽에 익숙한 사용자가 두 환경에서 기본적으로 데이터를 검색하고 집계 가능.

Elasticsearch SQL은 SQL 구문과 Elasticsearch의 네이티브 쿼리 기능 간의 번역기 역할 → 익숙한 SQL 명령으로 Elasticsearch 데이터 검색 가능.

#### 1.2 주요 장점

Elasticsearch SQL은 다음과 같은 핵심 장점을 제공함:

- 네이티브 통합: Elasticsearch를 위해 처음부터 구축 → 각 쿼리는 기본 저장 아키텍처에 따라 관련 노드에서 효율적으로 실행됨
- 자체 포함(Self-contained): Elasticsearch를 쿼리하는 데 추가 하드웨어·프로세스·런타임·라이브러리 불필요 → Elasticsearch SQL은 Elasticsearch 클러스터 내부에서 실행되어 별도 구성 요소 불필요
- 경량 및 효율성: Elasticsearch SQL은 검색을 추상화하지 않고 SQL을 그대로 수용 → 실시간으로 적절한 전문 검색(full-text search) 수행

#### 1.3 인터페이스

Elasticsearch SQL은 다양한 인터페이스를 통해 액세스 가능:

- REST API: JSON 형식의 SQL 쿼리 실행
- CLI (Command Line Interface): 명령줄 도구
- JDBC 드라이버: Java 데이터베이스 연결
- ODBC 드라이버: 비즈니스 인텔리전스 도구 연결
- 클라이언트 애플리케이션: Tableau, Power BI, Excel 등

---

### 2. 시작하기

#### 2.1 샘플 데이터 준비

Elasticsearch SQL 사용 전 먼저 쿼리할 데이터 필요 → 다음 예제에서는 "library"라는 인덱스에 도서 데이터를 색인함:

```json
PUT /library/_bulk?refresh
{"index":{"_id": "Leviathan Wakes"}}
{"name": "Leviathan Wakes", "author": "James S.A. Corey", "release_date": "2011-06-02", "page_count": 561}
{"index":{"_id": "Hyperion"}}
{"name": "Hyperion", "author": "Dan Simmons", "release_date": "1989-05-26", "page_count": 482}
{"index":{"_id": "Dune"}}
{"name": "Dune", "author": "Frank Herbert", "release_date": "1965-06-01", "page_count": 604}
```

#### 2.2 SQL REST API로 첫 쿼리 실행

SQL 검색 API로 쿼리 실행 가능 → `format` 매개변수로 응답 형식을 지정함:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library WHERE release_date < '2000-01-01'"
}
```

응답:

```
    author     |     name      |  page_count   | release_date
---------------+---------------+---------------+------------------------
Dan Simmons    |Hyperion       |482            |1989-05-26T00:00:00.000Z
Frank Herbert  |Dune           |604            |1965-06-01T00:00:00.000Z
```

#### 2.3 SQL CLI로 쿼리 실행

Elasticsearch의 `bin` 디렉토리에 있는 대화형 SQL CLI 도구도 사용 가능:

```bash
./bin/elasticsearch-sql-cli
```

CLI에서 동일한 쿼리 실행 가능:

```sql
sql> SELECT * FROM library WHERE release_date < '2000-01-01';
```

---

### 3. 개념 및 용어

#### 3.1 SQL과 Elasticsearch 간 매핑

Elasticsearch SQL은 SQL 의미론을 가능한 한 따르면서 두 가지 서로 다른 데이터 관리 패러다임을 연결함 → 다음은 SQL과 Elasticsearch 간의 용어 매핑:

- column → field: 하나의 값을 포함하는 명명된 항목. SQL에서 열은 정확히 하나의 값을 포함하지만, Elasticsearch 필드는 동일한 타입의 여러 값을 포함 가능
- row → document: 열/필드로 구성된 데이터 항목. SQL 행은 "엄격한" 구조를 가지며, Elasticsearch 문서는 "유연한" 구조를 가짐
- table → index: 행/문서의 모음
- schema → implicit: SQL에서 스키마는 테이블의 네임스페이스. Elasticsearch는 스키마 수준이 없으며, 각 인덱스에는 매핑이 암시적으로 적용됨
- catalog(database) → cluster: SQL에서 카탈로그 또는 데이터베이스는 스키마 집합을 나타냄. Elasticsearch에서는 클러스터가 배포된 여러 Elasticsearch 인스턴스를 나타냄

#### 3.2 핵심 원칙

Elasticsearch SQL은 "최소 놀라움의 원칙"을 따르며 SQL 의미론을 유지하면서 Elasticsearch의 네이티브 기능을 수용함 → 덕분에 많은 개념이 Elasticsearch 전반에 걸쳐 자연스럽게 적용됨.

---

### 4. 보안

#### 4.1 SSL/TLS 구성

클러스터가 암호화된 전송을 사용하는 경우 Elasticsearch SQL에 SSL/TLS 설정이 필요함 → `ssl` 속성을 `true`로 설정하거나 연결 URL에서 `https` 접두사를 사용하여 활성화함.

인증서 설정에 따라 구성이 달라짐:

- Keystore: 개인 키와 인증서를 저장 (PKI 인증에 필요)
- Truststore: CA 인증서를 저장하여 검증

#### 4.2 인증 방법

##### 사용자 이름/비밀번호

연결 설정에서 `user`와 `password` 속성을 통해 인증을 구성함.

##### PKI/X.509 인증서

인증서 기반 인증을 위해 다음을 설정:

- `ssl.keystore.location` - 키스토어 경로
- `ssl.keystore.pass` - 키스토어 비밀번호
- `ssl.truststore.location` - 트러스트스토어 경로
- `ssl.truststore.pass` - 트러스트스토어 비밀번호

#### 4.3 서버 측 권한

SQL 쿼리를 실행하는 사용자에게는 다음과 같은 최소 권한이 필요함:

- 대상 인덱스에 대한 `read` 액세스
- `indices:admin/get` 권한
- 특정 API 작업을 위한 `cluster:monitor/main` 권한

권한은 역할을 통해 할당됨 → 다음을 통해 생성 가능:

- Kibana UI
- 역할 관리 API
- `roles.yml` 구성 파일

---

### 5. SQL REST API

#### 5.1 개요

SQL 검색 API를 사용하면 JSON 문서 형식으로 SQL을 제출하고 결과를 받을 수 있음 → 기본 엔드포인트는 `/_sql`.

#### 5.2 기본 요청

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library ORDER BY page_count DESC LIMIT 5"
}
```

Kibana Console에서 쿼리를 작성할 때는 삼중 따옴표(`"""`)를 사용하면 내부 인용 부호를 자동으로 이스케이프하고 여러 줄 쿼리 형식을 지원함.

#### 5.3 응답 형식

`format` URL 매개변수 또는 `Accept` HTTP 헤더를 통해 응답 형식을 지정 가능 → URL 매개변수가 우선함.

##### 지원되는 형식

- csv (`text/csv`): 쉼표로 구분된 값
- json (`application/json`): 열 메타데이터와 행이 포함된 구조화된 형식
- tsv (`text/tab-separated-values`): 탭으로 구분된 값
- txt (`text/plain`): CLI 스타일 테이블 표현
- yaml (`application/yaml`): 사람이 읽을 수 있는 구조화된 형식
- cbor (`application/cbor`): 간결한 바이너리 객체 표현
- smile (`application/smile`): CBOR과 유사한 바이너리 형식

##### JSON 형식 예제

```json
POST /_sql?format=json
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 5
}
```

응답:

```json
{
  "columns": [
    {"name": "author", "type": "text"},
    {"name": "name", "type": "text"},
    {"name": "page_count", "type": "short"},
    {"name": "release_date", "type": "datetime"}
  ],
  "rows": [
    ["Frank Herbert", "Dune", 604, "1965-06-01T00:00:00.000Z"],
    ["James S.A. Corey", "Leviathan Wakes", 561, "2011-06-02T00:00:00.000Z"],
    ["Dan Simmons", "Hyperion", 482, "1989-05-26T00:00:00.000Z"]
  ]
}
```

##### CSV 형식 예제

```
POST /_sql?format=csv
{
  "query": "SELECT * FROM library ORDER BY page_count DESC"
}
```

선택적으로 `delimiter` 매개변수로 구분자 사용자 정의 가능 (기본값: 쉼표).

#### 5.4 페이지네이션

대용량 결과 집합의 경우 커서 기반 페이지네이션을 사용 → 초기 쿼리에서 `fetch_size`를 지정하면 응답에 `cursor`가 포함됨:

```json
POST /_sql?format=json
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 2
}
```

다음 페이지를 가져오려면 커서를 사용:

```json
POST /_sql?format=json
{
  "cursor": "sDXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAAE..."
}
```

#### 5.5 Query DSL 필터링

`filter` 매개변수를 통해 SQL 쿼리가 실행되기 전에 Elasticsearch Query DSL을 사용하여 결과를 필터링 가능:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "filter": {
    "range": {
      "page_count": {
        "gte": 100,
        "lte": 200
      }
    }
  },
  "fetch_size": 5
}
```

라우팅 기반 필터링도 가능:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library",
  "filter": {
    "terms": {
      "_routing": ["abc"]
    }
  }
}
```

#### 5.6 컬럼형 결과

`columnar` 매개변수를 `true`로 설정하면 행 지향 형식 대신 컬럼형 형식으로 결과를 반환함 → 이 기능은 `json`, `yaml`, `cbor`, `smile` 형식에서 사용 가능:

```json
POST /_sql?format=json
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 5,
  "columnar": true
}
```

응답:

```json
{
  "columns": [
    {"name": "author", "type": "text"},
    {"name": "name", "type": "text"},
    {"name": "page_count", "type": "short"},
    {"name": "release_date", "type": "datetime"}
  ],
  "values": [
    ["Frank Herbert", "James S.A. Corey", "Dan Simmons"],
    ["Dune", "Leviathan Wakes", "Hyperion"],
    [604, 561, 482],
    ["1965-06-01T00:00:00.000Z", "2011-06-02T00:00:00.000Z", "1989-05-26T00:00:00.000Z"]
  ]
}
```

#### 5.7 매개변수화된 쿼리

SQL 주입을 방지하기 위해 매개변수화된 쿼리 사용 권장 → 쿼리 문자열에서 물음표 플레이스홀더(`?`)를 사용하고 값을 별도의 매개변수 배열에 추출함:

```json
POST /_sql?format=txt
{
  "query": "SELECT YEAR(release_date) AS year FROM library WHERE page_count > ? AND author = ? GROUP BY year HAVING COUNT(*) > ?",
  "params": [300, "Frank Herbert", 0]
}
```

#### 5.8 런타임 필드

런타임 필드를 사용하면 기본 매핑을 수정하지 않고도 기존 데이터에서 새 열을 추출하고 생성 가능 → `runtime_mappings` 매개변수를 사용함:

```json
POST /_sql?format=txt
{
  "query": "SELECT * FROM library",
  "runtime_mappings": {
    "release_day_of_week": {
      "type": "keyword",
      "script": "emit(doc['release_date'].value.dayOfWeekEnum.toString())"
    }
  }
}
```

#### 5.9 비동기 검색

대용량 데이터셋 또는 동결된 인덱스를 쿼리할 때 시간이 오래 걸리는 작업의 경우 비동기 실행 활성화 가능:

```json
POST /_sql?format=json
{
  "wait_for_completion_timeout": "2s",
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 5
}
```

지정된 시간 내에 결과가 반환되지 않으면 요청이 비동기 모드로 전환되며 다음을 반환함:

- 고유 검색 식별자
- `is_partial: true` (불완전한 결과 표시)
- `is_running: true` (백그라운드 실행 표시)

검색 진행 상황을 확인하려면:

```json
GET _sql/async/status/<search_id>
```

검색 유지 기간을 사용자 정의하려면 `keep_alive`를 사용:

```json
POST /_sql?format=json
{
  "keep_alive": "2d",
  "wait_for_completion_timeout": "2s",
  "query": "SELECT * FROM library ORDER BY page_count DESC"
}
```

---

### 6. SQL Translate API

#### 6.1 개요

SQL Translate API는 SQL 쿼리를 Elasticsearch Query DSL 요청으로 변환함 → SQL 문이 쿼리 수준에서 어떻게 처리되는지 파악 가능.

#### 6.2 사용법

```json
POST /_sql/translate
{
  "query": "SELECT * FROM library ORDER BY page_count DESC",
  "fetch_size": 10
}
```

응답:

```json
{
  "size": 10,
  "_source": false,
  "fields": [
    {"field": "author"},
    {"field": "name"},
    {"field": "page_count"},
    {"field": "release_date", "format": "strict_date_optional_time_nanos"}
  ],
  "sort": [
    {
      "page_count": {
        "order": "desc",
        "missing": "_first",
        "unmapped_type": "short"
      }
    }
  ],
  "track_total_hits": -1
}
```

#### 6.3 활용

다음 용도로 활용 가능:

- SQL 쿼리가 어떤 Elasticsearch API 메서드를 사용하는지 확인
- SQL 쿼리 최적화 파악
- 디버깅

요청 본문은 `cursor`를 제외하고 SQL 검색 API와 동일한 파라미터를 지원함.

---

### 7. SQL CLI

#### 7.1 개요

Elasticsearch는 Elasticsearch 인스턴스에서 SQL 쿼리를 실행하기 위한 SQL CLI 스크립트를 `bin` 디렉토리에 제공함.

#### 7.2 시작하기

기본 시작:

```bash
./bin/elasticsearch-sql-cli
```

특정 서버에 연결:

```bash
./bin/elasticsearch-sql-cli https://some.server:9200
```

인증 포함:

```bash
./bin/elasticsearch-sql-cli https://username:password@host:port
```

독립 실행형 JAR:

Elasticsearch 설치 없이 실행하려면:

```bash
java -jar elasticsearch-sql-cli-VERSION.jar https://server:9200
```

#### 7.3 CLI 명령

##### 구성 명령

- `allow_partial_search_results` (기본값 false): 샤드 타임아웃 또는 실패 시 부분 결과 반환 여부
- `fetch_size` (기본값 1000): 페치 작업당 검색되는 레코드 수
- `fetch_separator` (기본값 빈 문자열): 연속 페치 작업 간의 구분자 문자열
- `lenient` (기본값 false): 활성화하면 배열 필드에서 오류 대신 첫 번째 값 반환

##### 유틸리티 명령

- `info`: 서버 및 클러스터 정보 표시
- `exit`: CLI 애플리케이션 종료
- `cls`: 터미널 화면 지우기
- `logo`: Elastic 로고 및 버전 출력

#### 7.4 쿼리 예제

```sql
sql> SELECT * FROM library WHERE page_count > 500 ORDER BY page_count DESC;
```

---

### 8. SQL JDBC

#### 8.1 개요

Elasticsearch SQL JDBC 드라이버는 JDBC 호출을 Elasticsearch SQL로 변환하는 Type 4 드라이버 → 플랫폼에 독립적인 순수 Java 솔루션.

#### 8.2 설치

##### Maven

프로젝트에 다음 의존성을 추가:

```xml
<dependency>
  <groupId>org.elasticsearch.plugin</groupId>
  <artifactId>x-pack-sql-jdbc</artifactId>
  <version>9.2.4</version>
</dependency>
```

리포지토리도 포함:

```xml
<repository>
  <id>elastic.co</id>
  <url>https://artifacts.elastic.co/maven</url>
</repository>
```

##### 직접 다운로드

수동 다운로드는 [elastic.co의 JDBC 페이지](https://www.elastic.co/downloads/jdbc-client) 참고.

#### 8.3 버전 호환성

드라이버 버전은 Elasticsearch 버전보다 높아서는 안 됨 → 예를 들어, 9.2.4 드라이버는 Elasticsearch 7.10.0에서 작동하지 않음.

#### 8.4 드라이버 등록

메인 클래스는 `org.elasticsearch.xpack.sql.jdbc.EsDriver` → 드라이버는 클래스패스에 있을 때 JDBC 4.0의 서비스 프로바이더 메커니즘을 통해 자동 등록됨.

#### 8.5 연결 URL 형식

```
jdbc:[es|elasticsearch]://[[http|https]://]?[host[:port]]?/[prefix]?[\?[option=value]&]*
```

예제:

```
jdbc:es://http://server:3456/?timezone=UTC&page.size=250
```

#### 8.6 필수 구성

- `timezone` (기본값 JVM 시간대): 연결별 시간대 (UTC 권장)
- `connect.timeout` (기본값 30000ms): 최대 연결 수립 시간
- `network.timeout` (기본값 60000ms): 최대 네트워크 작업 시간
- `query.timeout` (기본값 90000ms): 최대 쿼리 실행 대기 시간
- `page.size` (기본값 1000): 페이지당 반환되는 결과 수
- `page.timeout` (기본값 45000ms): 스크롤 커서 유지 기간

#### 8.7 인증

기본 인증을 위해 다음 속성을 사용:

- `user`: 사용자 이름
- `password`: 비밀번호

#### 8.8 SSL 구성

보안 연결을 활성화하려면 다음 옵션을 사용:

- `ssl`: `true`로 설정하여 활성화
- `ssl.keystore.location`: 키 스토어 파일 경로
- `ssl.keystore.pass`: 키 스토어 비밀번호
- `ssl.keystore.type`: 형식 (기본 JKS; PKCS12 사용 가능)
- `ssl.truststore.location`: 트러스트 스토어 경로
- `ssl.truststore.pass`: 트러스트 스토어 비밀번호
- `ssl.protocol`: SSL 버전 (기본 TLS)

#### 8.9 고급 옵션

프록시 지원:

- `proxy.http`: HTTP 프록시 호스트 이름
- `proxy.socks`: SOCKS 프록시 호스트 이름

쿼리 동작:

- `field.multi.value.leniency`: 다중값 필드의 첫 번째 값 반환 (기본 true)
- `allow.partial.results`: 샤드 실패 시 부분 결과 활성화 (기본 false)
- `index.include.frozen`: 동결된 인덱스 포함 (기본 false)

디버깅:

- `debug`: 디버그 로깅 활성화 (기본 false)
- `debug.output`: 로그 대상 - `err` (기본), `out`, 또는 파일 경로

---

### 9. SQL ODBC

#### 9.1 개요

Elasticsearch SQL ODBC 드라이버는 Elasticsearch를 위한 3.80 호환 ODBC 드라이버 → ODBC 함수 호출을 Elasticsearch SQL 작업으로 변환하는 코어 레벨 인터페이스로 동작함.

#### 9.2 요구 사항

이 드라이버를 사용하려면 시스템에 다음이 필요함:

- Elasticsearch SQL 설치 및 운영
- 서버의 유효한 라이선스

#### 9.3 설치 및 구성

문서는 두 가지 주요 구성 영역을 다룸:

1. 드라이버 설치: 운영 체제에서 ODBC 드라이버를 얻고 설정하는 방법
2. 구성: 연결 매개변수 및 드라이버 설정 구성 지침

#### 9.4 목적

드라이버는 ODBC 호환 애플리케이션과 Elasticsearch SQL 기능 사이의 브리지 역할 → API를 직접 다루지 않아도 Elasticsearch 데이터에 SQL 쿼리 실행 가능.

---

### 10. 클라이언트 애플리케이션

#### 10.1 지원되는 애플리케이션

Elasticsearch SQL 기능은 JDBC 및 ODBC 인터페이스를 통해 다양한 서드파티 애플리케이션에서 활용 가능:

데이터베이스 도구:

- DBeaver
- DbVisualizer
- SQL Workbench/J
- SQuirreL SQL

비즈니스 인텔리전스:

- Tableau Desktop
- Tableau Server
- Qlik Sense Desktop
- MicroStrategy Desktop
- Microsoft Power BI Desktop

스프레드시트 및 스크립팅:

- Microsoft Excel
- Microsoft PowerShell

#### 10.2 주요 고려 사항

면책 조항: Elastic은 나열된 애플리케이션을 보증·홍보·지원하지 않음 → 네이티브 Elasticsearch 통합을 원하는 조직은 공급업체에 직접 문의 필요.

기술적 제한 사항: ODBC 2.x 표준 이하를 구현하는 애플리케이션에 대한 지원은 현재 제한적.

범위: 각 애플리케이션은 고유한 요구 사항과 라이선스 조건을 가짐 → 구성 지원은 Elasticsearch SQL 연결에 한함.

---

### 11. SQL 언어

#### 11.1 어휘 구조

##### 키워드

SQL 키워드는 `SELECT`, `FROM`, `WHERE`와 같은 고정 의미 단어 → 대소문자를 구분하지 않으므로 `select`와 `SELECT`는 동일함 → 가독성을 높이기 위해 키워드를 대문자로 작성하는 것이 관례.

##### 식별자

식별자는 테이블 및 열과 같은 엔티티의 이름을 지정함 → 인용되지 않은 식별자(예: `ip_address`)와 인용된 식별자(예: `"hosts-*"`)가 있음 → 인용된 식별자는 명확성과 모호성 제거에 유용하므로, 특히 사용자 입력을 처리할 때 사용 권장 → 키워드와 달리 식별자는 대소문자를 구분함.

##### 리터럴

- 문자열 리터럴: 작은따옴표로 묶이며, 이스케이프된 따옴표는 반복됨 (`'Captain EO''s Voyage'`)
- 숫자 리터럴: 십진수 또는 과학적 표기법으로 허용됨. 소수점이 있는 숫자는 `double` 타입이고, 소수점이 없는 정수는 크기에 따라 `integer` 또는 `long`
- 일반 리터럴: 캐스트 연산자나 변환 함수를 통해 생성됨

##### 작은따옴표 vs 큰따옴표

작은따옴표(`'`)와 큰따옴표(`"`)는 다른 의미를 가지며 상호 교환 불가 → 작은따옴표는 문자열 리터럴을 정의하고, 큰따옴표는 식별자를 정의함.

##### 특수 문자

별표(`*`), 쉼표(`,`), 마침표(`.`), 괄호(`()`)는 SQL 구문에서 전용 의미를 가짐.

##### 연산자 및 주석

연산자는 왼쪽 결합성을 가진 표준 우선순위 규칙을 따름 → 주석은 한 줄의 경우 `--`를, 여러 줄의 경우 `/* */` 형식을 사용함.

#### 11.2 SQL 명령

##### SELECT

SELECT 문은 이 일반적인 형식으로 테이블에서 행을 검색함:

```sql
SELECT [TOP [count]] select_expr [, ...]
[FROM table_name]
[WHERE condition]
[GROUP BY grouping_element [, ...]]
[HAVING condition]
[ORDER BY expression [ASC | DESC] [, ...]]
[LIMIT [count]]
[PIVOT (aggregation_expr FOR column IN (value...))]
```

SELECT 목록: 출력 열은 `AS` 키워드를 사용하여 명시적으로 이름을 지정하거나 열 참조에서 암시적으로 파생 가능.

와일드카드 선택: `*` 연산자는 다중 필드 및 하위 필드를 제외한 모든 최상위 열을 선택함.

TOP 절: 결과를 최대 행 수로 제한:

```sql
SELECT TOP 2 first_name, last_name FROM emp
```

동일한 쿼리에서 LIMIT와 결합 불가.

FROM 절: 단일 테이블 또는 인덱스 패턴을 지정함. 지원 기능:

- 큰따옴표를 사용한 특수 문자가 있는 이스케이프된 테이블 이름
- 여러 인덱스 쿼리를 위한 인덱스 패턴
- `<remote_cluster>:<target>` 구문을 사용한 크로스 클러스터 검색
- 간결함을 위한 선택적 테이블 별칭

WHERE 절: 부울 값으로 평가되는 조건에 따라 행을 필터링함.

GROUP BY: 지정된 열의 일치하는 값에 따라 결과를 그룹으로 나눔 → 표현식은 열 이름, 별칭, 서수 위치 또는 계산된 표현식을 참조 가능 → 모든 출력 표현식은 집계 함수이거나 그룹화 표현식이어야 함.

암시적 그룹화: GROUP BY 없이 집계 함수가 나타나면 단일 기본 그룹이 생성되어 집계된 결과가 있는 하나의 행을 반환함.

HAVING: GROUP BY로 생성된 그룹을 필터링함 → 개별 행이 아닌 집계된 값에 대해 작동하며, 그룹화가 발생한 후에 평가됨.

ORDER BY: 표현식, 열, 출력 서수 위치 또는 관련성 점수별로 결과를 정렬함 → 기본 방향은 오름차순이며 null 값은 마지막에 나타남.

LIMIT: `LIMIT count` 또는 제한 없이 `LIMIT ALL`을 사용하여 결과를 제한함.

PIVOT: 고유한 열 값을 결과 헤더로 회전하여 교차 집계를 수행함:

```sql
SELECT * FROM test_emp PIVOT (SUM(salary) FOR languages IN (1, 2))
```

##### DESCRIBE TABLE

DESCRIBE TABLE 명령은 테이블, 인덱스 또는 데이터 스트림에서 열 정보를 검색함.

```sql
DESCRIBE table_name
```

또는

```sql
DESC table_name
```

출력 열:

- column: 필드 이름
- type: SQL 데이터 타입 (VARCHAR, INTEGER, TIMESTAMP 등)
- mapping: 기본 Elasticsearch 매핑 타입 (keyword, text, nested 등)

##### SHOW TABLES

SHOW TABLES 명령은 사용 가능한 테이블(인덱스 및 데이터 스트림)을 타입 및 종류와 함께 나열함:

```sql
SHOW TABLES [CATALOG identifier] [INCLUDE FROZEN] [table_identifier | LIKE pattern]
```

구성 요소:

- CATALOG: 와일드카드를 지원하는 선택적 클러스터 식별자
- INCLUDE FROZEN: 동결된 인덱스를 표시하는 선택적 플래그
- table_identifier: 단일 테이블 이름 또는 다중 대상 패턴
- LIKE 절: SQL 패턴 매칭을 사용하여 결과 제한

기본 사용법:

```sql
SHOW TABLES;
```

패턴 매칭:

```sql
SHOW TABLES LIKE 'emp%';
```

다중 인덱스 선택:

```sql
SHOW TABLES "*,-l*";
```

##### SHOW COLUMNS

SHOW COLUMNS 명령은 테이블의 열과 데이터 타입을 나열함:

```sql
SHOW COLUMNS [CATALOG identifier] [INCLUDE FROZEN] FROM table_identifier
```

또는

```sql
SHOW COLUMNS [CATALOG identifier] [INCLUDE FROZEN] IN table_identifier
```

##### SHOW FUNCTIONS

SHOW FUNCTIONS 명령은 사용 가능한 모든 SQL 함수와 타입을 나열함:

```sql
SHOW FUNCTIONS [LIKE pattern]
```

함수 타입:

- AGGREGATE: AVG, COUNT, SUM, MAX, MIN 등
- GROUPING: HISTOGRAM 등
- CONDITIONAL: CASE, COALESCE, IFNULL, NULLIF 포함
- SCALAR: 수학, 문자열, 날짜/시간 및 타입 변환 함수
- SCORE: 점수 함수

패턴 매칭:

```sql
SHOW FUNCTIONS LIKE 'A%';
```

#### 11.3 데이터 타입

##### SQL과 Elasticsearch 타입 매핑

숫자 타입:

- byte → TINYINT (정밀도 3)
- short → SMALLINT (정밀도 5)
- integer → INTEGER (정밀도 10)
- long → BIGINT (정밀도 19)
- unsigned_long → BIGINT (정밀도 20)
- float → REAL (정밀도 7)
- double → DOUBLE (정밀도 15)
- half_float → FLOAT (정밀도 3)
- scaled_float → DOUBLE (정밀도 15)

문자열 및 텍스트 타입:

- keyword 계열 → VARCHAR (정밀도 32,766)
- text → VARCHAR (정밀도 2,147,483,647)
- binary → VARBINARY (정밀도 2,147,483,647)

시간 타입:

- date → datetime (정밀도 29)
- ip → VARCHAR (정밀도 39)

복합 타입:

- object → STRUCT
- nested → STRUCT

##### SQL 전용 런타임 타입

이러한 타입은 Elasticsearch에 해당하는 것 없이 SQL 쿼리에만 존재함:

- DATE 및 TIME (datetime과 별도)
- INTERVAL 타입 (year, month, day, hour, minute, second 및 조합)
- GEO_POINT (정밀도 52)
- GEO_SHAPE 및 SHAPE (정밀도 2,147,483,647)

##### 다중 필드 처리

SQL이 정확한 일치가 필요한 `text` 필드를 만나면, 정규화되지 않은 첫 번째 `keyword` 서브필드를 찾아 원래 필드의 정확한 값으로 사용함.

#### 11.4 인덱스 패턴

Elasticsearch SQL은 여러 인덱스 또는 테이블을 일치시키기 위한 두 가지 접근 방식을 지원함:

##### Elasticsearch 다중 대상 구문

큰따옴표를 사용하며 Elasticsearch의 표준 다중 대상 표기법을 따름 → 와일드카드로 인덱스 포함, 제외, 열거를 지원함.

주요 특성:

- 인용에 `"`를 사용
- 다중 문자 일치에 `*` 지원
- `-` 접두사로 제외
- 특정 대상 열거 가능

예제 사용법:

```sql
SHOW TABLES "*,-l*";
SELECT emp_no FROM "e*p" LIMIT 1;
```

크로스 클러스터: 원격 클러스터는 `"<remote_cluster>:<target>"` 구문으로 쿼리 가능하며, 양쪽 모두 와일드카드를 지원함.

##### SQL LIKE 표기법

작은따옴표를 사용하며 표준 SQL 패턴 매칭 규칙을 따름.

주요 특성:

- 인용에 `'`를 사용
- 다중 문자 일치에 `%` 사용
- 단일 문자 일치에 `_` 사용
- 특수 문자 처리를 위한 `ESCAPE` 키워드 지원

예제 사용법:

```sql
SHOW TABLES LIKE 'emp%';
SHOW TABLES LIKE 'emp!%' ESCAPE '!';
```

##### 비교 요약

- 인용 타입: 다중 인덱스 = `"` · SQL LIKE = `'`
- 다중 문자 패턴: 다중 인덱스 = `*` · SQL LIKE = `%`
- 단일 문자 패턴: 다중 인덱스 = 없음 · SQL LIKE = `_`
- 제외 지원: 다중 인덱스 = 지원 · SQL LIKE = 미지원
- 열거 지원: 다중 인덱스 = 지원 · SQL LIKE = 미지원
- 이스케이프 지원: 다중 인덱스 = 미지원 · SQL LIKE = `ESCAPE`

중요 제약: 다중 인덱스 쿼리에서 대상으로 지정된 모든 구체적인 테이블은 동일한 매핑을 공유해야 함.

---

### 12. 함수 및 연산자

#### 12.1 연산자

##### 비교 연산자

- `=`: 같음
- `<>`, `!=`: 같지 않음
- `<`: 보다 작음
- `<=`: 보다 작거나 같음
- `>`: 보다 큼
- `>=`: 보다 크거나 같음
- `BETWEEN`: 범위 확인
- `IS NULL`: null 여부
- `IN`: 목록에 포함 여부

##### 논리 연산자

- `AND`: 논리 AND
- `OR`: 논리 OR
- `NOT`: 논리 NOT

##### 산술 연산자

- `+`: 덧셈
- `-`: 뺄셈
- `*`: 곱셈
- `/`: 나눗셈
- `%`: 나머지 (모듈로)

##### 패턴 매칭

- `LIKE`: SQL 표준 패턴 매칭
- `RLIKE`: 정규식 기반 패턴 검색

##### 타입 캐스팅

값 간 타입 변환을 위해 `::` 연산자를 사용함.

#### 12.2 집계 함수

- `COUNT`: 행 개수
- `SUM`: 합계
- `AVG`: 평균
- `MIN`: 최솟값
- `MAX`: 최댓값
- `STDDEV_POP`: 모집단 표준 편차
- `STDDEV_SAMP`: 표본 표준 편차
- `VAR_POP`: 모집단 분산
- `VAR_SAMP`: 표본 분산
- `PERCENTILE`: 백분위수
- `KURTOSIS`: 첨도
- `SKEWNESS`: 왜도
- `MAD`: 중앙값 절대 편차

#### 12.3 그룹화 함수

- `HISTOGRAM`: 숫자 값을 버킷으로 분류

#### 12.4 날짜-시간 함수

- `CURRENT_DATE`: 현재 날짜
- `CURRENT_TIME`: 현재 시간
- `CURRENT_TIMESTAMP`: 현재 타임스탬프
- `DATE_ADD`: 날짜 더하기
- `DATE_DIFF`: 날짜 차이
- `DATE_TRUNC`: 날짜 잘라내기
- `EXTRACT`: 날짜 부분 추출
- `DATE_PART`: 날짜 부분 추출
- `DAY_OF_MONTH`: 월의 날짜
- `MONTH_OF_YEAR`: 연의 월

#### 12.5 문자열 함수

- `CONCAT`: 문자열 연결
- `SUBSTRING`: 부분 문자열 추출
- `REPLACE`: 문자열 대체
- `TRIM`: 공백 제거
- `UPPER`: 대문자로 변환
- `LOWER`: 소문자로 변환
- `LOCATE`: 위치 찾기
- `POSITION`: 위치 찾기
- `LENGTH`: 길이

#### 12.6 수학 함수

- `ABS`: 절대값
- `ROUND`: 반올림
- `FLOOR`: 내림
- `CEIL`: 올림
- `SQRT`: 제곱근
- `POWER`: 거듭제곱
- `LOG`: 로그
- `EXP`: 지수
- `SIN`: 사인
- `COS`: 코사인
- `TAN`: 탄젠트
- `ASIN`: 아크사인
- `ACOS`: 아크코사인
- `ATAN`: 아크탄젠트

#### 12.7 조건 함수

- `CASE`: 조건부 표현식
- `COALESCE`: 첫 번째 non-null 값
- `IFNULL`: null인 경우 대체 값
- `IIF`: 인라인 조건
- `GREATEST`: 최대값
- `LEAST`: 최소값
- `NULLIF`: 같으면 null

#### 12.8 전문 검색 함수

- `MATCH`: 매치 쿼리 수행
- `QUERY`: 쿼리 문자열 쿼리 수행
- `SCORE`: 관련성 점수

#### 12.9 지리 함수

- `ST_Distance`: 두 지점 간 거리
- `ST_AsWKT`: WKT 형식으로 변환
- `ST_X`: X 좌표 (경도)
- `ST_Y`: Y 좌표 (위도)
- `ST_Z`: Z 좌표 (고도)

#### 12.10 타입 변환

- `CAST`: 명시적 타입 변환
- `CONVERT`: 타입 변환

---

### 13. 제한 사항

#### 13.1 파싱 제한

매우 큰 쿼리는 파싱 단계에서 메모리를 과도하게 소비하여 `ParsingException`을 발생시킬 수 있음 → 쿼리를 단순화하거나 더 작은 단위로 분할하는 것이 권장됨.

#### 13.2 중첩 필드 제한

중첩 필드는 상위 형식으로 직접 쿼리할 수 없음 → 사용자는 `[nested_field_name].[sub_field_name]`과 같은 점 표기법을 사용하여 내부 하위 필드를 참조해야 함 → 또한 스칼라 함수는 `WHERE` 및 `ORDER BY` 절 내의 중첩 필드에 적용할 수 없지만 비교 및 논리 연산자는 허용됨.

다중 중첩 문서에는 추가 제약이 있음 → 쿼리는 서로 다른 수준이나 동일 수준의 여러 중첩 필드를 동시에 참조할 수 없음.

#### 13.3 페이지네이션 문제

중첩 필드를 선택할 때 페이지네이션은 정확히 페이지 크기만큼이 아닌, 최소 페이지 크기 이상의 레코드를 반환함 → Elasticsearch가 중첩 쿼리를 내부 히트가 아닌 루트 문서 단위로 처리하기 때문.

#### 13.4 집계 제약

집계 함수로 정렬하는 경우 최대 65,535행으로 제한됨 → `ORDER BY` 절은 일반 집계 함수만 참조할 수 있으며, 스칼라 함수나 연산자 조합은 허용되지 않음 → 또한 `FIRST` 및 `LAST` 집계 함수는 `HAVING` 절에서 사용할 수 없음.

#### 13.5 데이터 타입 제한

- `TIME` 데이터 타입은 `GROUP BY` 또는 `HISTOGRAM` 함수에서 사용할 수 없음
- 정규화기가 있는 `keyword` 필드는 지원되지 않음
- 배열 필드는 `field_multi_value_leniency` 매개변수를 통한 특수 처리가 필요함

#### 13.6 쿼리 구조 제한

서브쿼리는 단일 `SELECT` 문으로 평탄화되어야 함 → `GROUP BY`, `HAVING` 또는 복잡한 조건이 포함된 서브쿼리는 지원되지 않음 → `PIVOT` 집계는 단일 표현식만 허용하며, `PIVOT`의 `IN` 절에서 서브쿼리는 지원되지 않음.

#### 13.7 지리적 제약

`geo_shape` 필드는 doc values가 없어 필터링, 그룹화, 정렬에 사용할 수 없음 → `geo_point` 필드의 고도(altitude) 값은 인덱싱되거나 저장되지 않음.

---

### 실전 예제

#### 기본 SELECT 쿼리

```sql
SELECT author, name, page_count
FROM library
WHERE page_count > 300
ORDER BY page_count DESC
LIMIT 10;
```

#### 집계 및 그룹화

```sql
SELECT author, COUNT(*) AS book_count, AVG(page_count) AS avg_pages
FROM library
GROUP BY author
HAVING COUNT(*) > 1
ORDER BY book_count DESC;
```

#### 날짜 함수 사용

```sql
SELECT name, release_date, YEAR(release_date) AS release_year
FROM library
WHERE release_date BETWEEN '1960-01-01' AND '2000-12-31'
ORDER BY release_date;
```

#### PIVOT 사용

```sql
SELECT *
FROM library
PIVOT (COUNT(*) FOR author IN ('Frank Herbert', 'Dan Simmons', 'James S.A. Corey'));
```

#### 전문 검색

```sql
SELECT name, author, SCORE()
FROM library
WHERE MATCH(name, 'dune')
ORDER BY SCORE() DESC;
```

#### 인덱스 패턴 사용

```sql
SELECT *
FROM "lib*"
WHERE page_count > 400;
```

---

### 참고 자료

- [Elasticsearch SQL 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-sql.html)
- [SQL 개요](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-overview.html)
- [SQL 시작하기](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-getting-started.html)
- [SQL 개념 및 용어](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-concepts.html)
- [SQL REST API](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-rest.html)
- [SQL 언어](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-spec.html)
- [SQL 함수 및 연산자](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-functions.html)
- [SQL 제한 사항](https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-limitations.html)
