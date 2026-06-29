# 데이터 검색 (Search your data)

> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-your-data.html

## 목차

1. [개요](#1-개요)
2. [검색 쿼리 작성하기](#2-검색-쿼리-작성하기)
3. [Query DSL](#3-query-dsl)
4. [Search API](#4-search-api)
5. [Retrievers](#5-retrievers)
6. [검색 결과 정렬](#6-검색-결과-정렬)
7. [검색 결과 페이지네이션](#7-검색-결과-페이지네이션)
8. [선택된 필드 검색](#8-선택된-필드-검색)
9. [하이라이팅](#9-하이라이팅)
10. [검색 결과 접기](#10-검색-결과-접기)
11. [검색 결과 필터링](#11-검색-결과-필터링)
12. [집계 (Aggregations)](#12-집계-aggregations)
13. [Near Real-Time 검색](#13-near-real-time-검색)
14. [비동기 검색](#14-비동기-검색)
15. [검색 샤드 라우팅](#15-검색-샤드-라우팅)
16. [Inner Hits](#16-inner-hits)

---

## 1. 개요

Elasticsearch는 다양한 쿼리 인터페이스를 통해 데이터를 검색할 수 있습니다. 세 가지 주요 쿼리 언어가 있으며, 각각 특정 사용 사례에 최적화되어 있습니다:

### 1.1 Query DSL

Query DSL(Domain Specific Language)은 Elasticsearch의 원래 JSON 기반 쿼리 언어입니다. `_search` 엔드포인트를 통해 작동하며, 전체 텍스트 검색과 시맨틱 검색에 가장 적합합니다. 복잡한 구문을 가지고 있지만 매우 강력한 검색 기능을 제공합니다.

### 1.2 ES|QL

ES|QL(Elasticsearch Query Language)은 파이프 구문을 가진 빠른 SQL 유사 언어로, 새로운 컴퓨팅 아키텍처 위에 구축되었습니다. `_query` 엔드포인트를 통해 작동하며, 필터링, 분석 및 집계에 탁월합니다.

### 1.3 Retrievers

Retrievers는 구성 가능성에 초점을 맞춘 현대적인 `_search` API 구문입니다. 시맨틱 검색 기능을 통합하고 시맨틱 리랭킹이 필요한 복잡한 검색 파이프라인 구축에 적합합니다.

중요: 이 쿼리 언어들은 서로 경쟁하는 옵션이 아니라 보완적인 도구입니다. 요구 사항에 따라 애플리케이션의 각 부분에서 서로 다른 언어를 사용할 수 있습니다.

Elasticsearch에는 HTTP 클라이언트, 클라이언트 라이브러리 또는 Console 인터페이스를 통해 REST API로 접근하는 것이 권장됩니다.

---

## 2. 검색 쿼리 작성하기

Elasticsearch에서 검색 쿼리를 작성하는 방법은 사용하는 쿼리 언어에 따라 다릅니다.

### 2.1 기본 검색 요청

가장 기본적인 검색 요청은 `_search` 엔드포인트를 사용합니다:

```json
GET /my-index/_search
{
  "query": {
    "match_all": {}
  }
}
```

### 2.2 검색 기능 개요

Elasticsearch는 다양한 검색 방법론을 제공합니다:

- 전체 텍스트 검색: 텍스트를 분석하고 인덱싱하여 구문 쿼리, 근접 검색, 퍼지 매칭을 지원
- 키워드 검색: keyword 필드에서 정확한 일치를 찾음
- 시맨틱 검색: 벡터 임베딩을 사용하여 semantic_text 필드를 쿼리
- 벡터 검색: kNN 알고리즘을 사용하여 유사한 벡터를 찾음
- 지리 공간 검색: 위치 기반 쿼리와 공간 관계를 처리

`filter` 매개변수를 사용하면 필드 수준 기준에 따라 문서를 포함하거나 제외할 수 있습니다.

---

## 3. Query DSL

Query DSL은 복잡한 검색, 필터링, 집계를 지원하는 완전한 기능의 JSON 스타일 쿼리 언어입니다. Elasticsearch의 원래 쿼리 시스템이며 `_search` 엔드포인트를 통해 동작합니다.

### 3.1 쿼리 구조

Query DSL은 두 가지 유형의 절로 구성된 추상 구문 트리(AST) 구조로 동작합니다:

#### 리프 쿼리 절 (Leaf Query Clauses)

리프 쿼리 절은 `match`, `term`, `range` 같은 쿼리를 사용해 특정 필드 값을 대상으로 하며, 단독으로 사용할 수 있습니다.

```json
GET /_search
{
  "query": {
    "match": {
      "message": "elasticsearch"
    }
  }
}
```

#### 복합 쿼리 절 (Compound Query Clauses)

복합 쿼리 절은 `bool`, `dis_max` 같은 연산자를 통해 여러 쿼리를 논리적으로 결합하거나, `constant_score`를 통해 동작을 변경합니다.

```json
GET /_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "Search" } },
        { "match": { "content": "Elasticsearch" } }
      ],
      "filter": [
        { "term": { "status": "published" } },
        { "range": { "publish_date": { "gte": "2015-01-01" } } }
      ]
    }
  }
}
```

### 3.2 쿼리 컨텍스트 vs 필터 컨텍스트

#### 쿼리 컨텍스트 (Query Context)

쿼리 컨텍스트는 "이 문서가 쿼리와 얼마나 잘 일치하는가?"라는 질문에 답합니다. 관련성 점수가 `_score` 필드에 계산되며, 절이 `query` 매개변수에 전달될 때 적용됩니다.

#### 필터 컨텍스트 (Filter Context)

필터 컨텍스트는 점수 계산 없이 이진(예/아니오) 매칭을 제공합니다. 필터 컨텍스트의 이점:

- 점수가 매겨진 쿼리보다 빠른 실행
- 자주 사용되는 필터의 자동 캐싱
- 낮은 리소스 소비
- 구조화된 데이터(숫자, 날짜, 불리언, 키워드)에 이상적

필터 컨텍스트는 다음에 적용됩니다:
- `bool` 쿼리의 `filter` 또는 `must_not` 매개변수
- `constant_score` 쿼리의 `filter` 매개변수
- 필터 집계

### 3.3 주요 쿼리 유형

#### match 쿼리

전체 텍스트 검색을 위한 표준 쿼리입니다:

```json
GET /books/_search
{
  "query": {
    "match": {
      "name": "brave new world"
    }
  }
}
```

#### match_phrase 쿼리

정확한 구문을 검색합니다:

```json
GET /books/_search
{
  "query": {
    "match_phrase": {
      "content": "quick brown fox"
    }
  }
}
```

#### term 쿼리

정확한 값을 검색할 때 사용합니다:

```json
GET /books/_search
{
  "query": {
    "term": {
      "author.keyword": "Neal Stephenson"
    }
  }
}
```

#### range 쿼리

범위 검색에 사용합니다:

```json
GET /books/_search
{
  "query": {
    "range": {
      "release_date": {
        "gte": "1980-01-01",
        "lte": "1999-12-31"
      }
    }
  }
}
```

범위 연산자:
- `gte`: 크거나 같음 (greater than or equal)
- `gt`: 큼 (greater than)
- `lte`: 작거나 같음 (less than or equal)
- `lt`: 작음 (less than)

#### bool 쿼리

불리언 논리를 사용하여 여러 쿼리를 결합합니다:

```json
GET /books/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "world" } }
      ],
      "filter": [
        { "range": { "page_count": { "gte": 200 } } }
      ],
      "should": [
        { "match": { "author": "Huxley" } }
      ],
      "must_not": [
        { "term": { "author.keyword": "George Orwell" } }
      ]
    }
  }
}
```

`bool` 쿼리 절:
- `must`: 반드시 일치해야 함. 점수에 기여함
- `filter`: 반드시 일치해야 함. 점수에 기여하지 않음 (필터 컨텍스트)
- `should`: 일치하면 좋음. 점수를 높임
- `must_not`: 일치하면 안 됨

#### multi_match 쿼리

여러 필드에서 검색합니다:

```json
GET /books/_search
{
  "query": {
    "multi_match": {
      "query": "science fiction",
      "fields": ["name", "author", "description"]
    }
  }
}
```

---

## 4. Search API

Search API는 Query DSL 또는 쿼리 문자열 매개변수를 사용해 인덱싱된 데이터를 쿼리합니다.

### 4.1 엔드포인트

```
GET /_search
POST /_search
GET /{index}/_search
POST /{index}/_search
```

### 4.2 인증

API 키, 기본(Basic) 또는 Bearer 인증이 필요합니다. 대상 데이터 스트림, 인덱스 또는 별칭에 대한 `read` 인덱스 권한이 있어야 합니다.

### 4.3 주요 쿼리 매개변수

#### 페이지네이션

- `from`: 시작 문서 오프셋 (기본값: 0)
- `size`: 반환할 결과 수 (기본값: 10)
- `search_after`: 정렬 값을 사용하여 후속 페이지 검색

#### 필터링 및 분석

- `q`: Lucene 쿼리 문자열 구문
- `analyzer`: 쿼리 문자열 분석용
- `default_operator`: AND 또는 OR 논리 (기본값: OR)
- `df`: 지정되지 않은 경우 기본 필드

#### 성능

- `timeout`: 샤드당 응답 대기 시간
- `terminate_after`: 샤드당 수집할 최대 문서 수
- `track_total_hits`: 정확한 히트 수 집계 활성화 (성능 비용 발생)
- `request_cache`: size=0일 때 결과를 캐시

#### 고급 옵션

- `track_scores`: 정렬에 사용되지 않더라도 점수 계산
- `explain`: 자세한 점수 정보 반환
- `version`: 문서 버전 번호 포함
- `seq_no_primary_term`: 시퀀스 번호와 주 용어 반환

### 4.4 요청 본문 매개변수

#### 쿼리 구조

- `query`: Query DSL을 사용하여 검색 기준 정의
- `filter`: 점수에 영향 없는 추가 필터링
- `aggs`: 집계 정의

#### 결과 사용자 정의

- `_source`: 반환되는 필드 제어
- `fields`: 형식화된 필드 값 검색
- `highlight`: 관련 텍스트 스니펫 추출
- `script_fields`: 스크립트를 통한 계산된 필드

#### 정렬 및 순서

- `sort`: 필드, 점수 또는 거리로 결과 정렬
- `search_after`: 정렬된 결과로 페이지네이션

#### 고급 기능

- `knn`: 근사 k-최근접 이웃 벡터 검색
- `pit`: 일관된 페이지네이션을 위한 Point-in-Time
- `slice`: 독립적 소비를 위해 결과 분할
- `collapse`: 중복 필드 값 감소

### 4.5 응답 구조

API는 다음을 반환합니다:

- `took`: 쿼리 실행 시간 (밀리초)
- `timed_out`: 요청 시간 초과 여부
- `_shards`: 샤드 전체의 성공/실패 수
- `hits`: `_id`, `_score`, `_source` 등 메타데이터를 포함한 일치 문서 목록
- `aggregations`: 집계 계산 결과
- `suggest`: 맞춤법 제안 결과

### 4.6 코드 예제

```json
GET /my-index-000001/_search?from=40&size=20
{
  "query": {
    "term": {
      "user.id": "kimchy"
    }
  }
}
```

이 요청은 오프셋 40에서 시작해 지정된 term 쿼리와 일치하는 문서를 20개 반환합니다.

### 4.7 전체 검색 예제

```json
GET /my-index/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "elasticsearch" } }
      ],
      "filter": [
        { "term": { "status": "published" } },
        { "range": { "date": { "gte": "2024-01-01" } } }
      ]
    }
  },
  "from": 0,
  "size": 10,
  "sort": [
    { "date": { "order": "desc" } },
    "_score"
  ],
  "_source": ["title", "author", "date"],
  "highlight": {
    "fields": {
      "title": {}
    }
  },
  "aggs": {
    "authors": {
      "terms": {
        "field": "author.keyword"
      }
    }
  }
}
```

---

## 5. Retrievers

Retrievers는 검색에서 반환되는 상위 문서를 기술하는 사양입니다. 기존 query 및 kNN 요소를 대체해 더 정교한 결과 랭킹을 구현할 수 있습니다.

### 5.1 핵심 Retriever 유형

#### Standard Retriever

Standard Retriever는 전통적인 쿼리 기능을 대체합니다:

- 일치 가능한 문서를 제한하는 필터 쿼리
- 결과 포함을 위한 최소 점수 임계값
- 효율적인 결과 탐색을 위한 search_after 페이지네이션
- 조기 쿼리 종료를 위한 terminate_after 제한
- 결과 정렬을 위한 정렬 사양
- 필드 값에 의한 결과 통합을 위한 접기 기능

```json
GET /my-index/_search
{
  "retriever": {
    "standard": {
      "query": {
        "match": {
          "content": "elasticsearch"
        }
      },
      "filter": {
        "term": {
          "status": "published"
        }
      },
      "min_score": 0.5
    }
  }
}
```

#### KNN Retriever

벡터 유사도 검색을 위해 설계된 Retriever입니다:

- 유사도 매칭을 위한 벡터 필드 지정
- 쿼리 벡터 (정적) 또는 쿼리 벡터 빌더 (동적)
- `num_candidates`를 통한 샤드당 후보 고려 구성
- 일치 요구 사항을 결정하는 유사도 임계값
- 오버샘플 팩터를 사용한 양자화 벡터 최적화를 위한 rescore 벡터 지원
- k-최근접 이웃 검색을 통한 필드 기반 벡터 검색

```json
GET /my-index/_search
{
  "retriever": {
    "knn": {
      "field": "content_embedding",
      "query_vector": [0.1, 0.2, 0.3, ...],
      "k": 10,
      "num_candidates": 100
    }
  }
}
```

#### RRF (Reciprocal Rank Fusion) Retriever

수학적 랭킹 방식으로 여러 검색 결과 집합을 결합합니다:

- 랭킹된 문서 세트를 생성하는 여러 자식 Retriever
- 결과 세트 영향을 제어하는 `rank_constant` 매개변수
- 개별 결과 세트 크기를 결정하는 `rank_window_size`
- 차등 Retriever 영향을 위한 가중치 컴포넌트 지원

```json
GET /my-index/_search
{
  "retriever": {
    "rrf": {
      "retrievers": [
        {
          "standard": {
            "query": {
              "match": {
                "content": "elasticsearch"
              }
            }
          }
        },
        {
          "knn": {
            "field": "content_embedding",
            "query_vector": [0.1, 0.2, 0.3, ...],
            "k": 10,
            "num_candidates": 100
          }
        }
      ],
      "rank_constant": 60,
      "rank_window_size": 100
    }
  }
}
```

#### Text Similarity Reranker

Inference API를 활용하여 관련성 기반 리랭킹을 수행합니다:

- 초기 결과를 생성하는 중첩 Retriever
- 리랭킹 계산을 위한 Inference API 통합
- 유사도 비교를 위한 문서 필드 선택
- 고려 대상 문서를 제한하는 `rank_window_size`
- 선택적 청크 수준 리스코어링 기능

```json
GET /my-index/_search
{
  "retriever": {
    "text_similarity_reranker": {
      "retriever": {
        "standard": {
          "query": {
            "match": {
              "content": "machine learning"
            }
          }
        }
      },
      "field": "content",
      "inference_id": "my-rerank-model",
      "inference_text": "What is machine learning?",
      "rank_window_size": 100
    }
  }
}
```

### 5.2 고급 Retriever 패턴

Rule Retriever: 일치 기준에 따라 지정된 규칙 세트를 기반으로 사전 정의된 비즈니스 로직 규칙을 기본 Retriever 결과에 적용합니다.

Rescorer Retriever: 초기 결과 세트를 변경하지 않고 특수 랭킹 알고리즘으로 자식 Retriever 결과를 재점수화합니다.

Linear Retriever: 구성 가능한 정규화 전략을 사용한 가중 선형 조합으로 여러 Retriever를 결합합니다.

Pinned Retriever: 기본 Retriever 순서를 유지하면서 특정 문서가 결과 상단에 표시되도록 보장합니다.

Diversify Retriever: 람다 기반 관련성/다양성 트레이드오프 제어로 최대 한계 관련성(MMR) 다양화를 구현합니다.

### 5.3 공통 구성 요소

모든 Retriever는 다음을 지원합니다:

- Query DSL 구문을 사용하는 필터 매개변수
- 최소 점수 임계값 (`min_score`)
- `_name`을 통한 명명된 Retriever 식별
- 구성을 위한 Retriever 중첩

Retrievers는 집계, 하이라이팅 등 다른 검색 기능과 원활하게 통합되며, 결과 랭킹 및 필터링 로직에 대한 세밀한 제어를 제공합니다.

---

## 6. 검색 결과 정렬

Elasticsearch의 sort 매개변수를 사용하면 하나 이상의 필드로 결과를 정렬할 수 있습니다. 특정 필드, 관련성 점수, 문서 순서로 정렬 가능하며, 각 정렬 기준은 오름차순 또는 내림차순을 지원합니다.

### 6.1 기본 정렬 구문

정렬은 순서가 중요한 배열로 지정합니다. Elasticsearch는 첫 번째 필드 기준으로 정렬하고, 동점인 경우에만 다음 필드를 사용합니다:

```json
{
  "sort": [
    { "post_date": { "order": "asc" } },
    { "name": "desc" },
    "_score"
  ]
}
```

### 6.2 정렬 순서 옵션

- `asc`: 오름차순
- `desc`: 내림차순 (`_score`의 기본값)

숫자 필드는 기본적으로 내림차순으로, 문자열 필드는 명시적으로 지정하지 않으면 오름차순으로 정렬됩니다.

### 6.3 다중 값 필드의 정렬 모드

필드에 여러 값이 포함된 경우, 모드가 사용할 값을 결정합니다:

- `min`: 최저 값 (오름차순의 기본값)
- `max`: 최고 값 (내림차순의 기본값)
- `sum`: 모든 값의 합계 (숫자만)
- `avg`: 값의 평균 (숫자만)
- `median`: 값의 중앙값 (숫자만)

### 6.4 숫자 필드 캐스팅

여러 인덱스를 쿼리할 때 서로 다른 유형 매핑 간에 값을 캐스팅하려면 `numeric_type`을 사용합니다:

```json
{
  "sort": [
    {
      "field": {
        "numeric_type": "double"
      }
    }
  ]
}
```

지원 값: "double", "long", "date", "date_nanos"

### 6.5 중첩 객체 정렬

중첩 객체 내의 필드에 대해 중첩 경로를 지정합니다:

```json
{
  "sort": [
    {
      "offer.price": {
        "mode": "avg",
        "order": "asc",
        "nested": {
          "path": "offer",
          "filter": {
            "term": { "offer.color": "blue" }
          }
        }
      }
    }
  ]
}
```

### 6.6 지리적 거리 정렬

`_geo_distance`를 사용하여 지리적 거리로 정렬합니다:

```json
{
  "sort": [
    {
      "_geo_distance": {
        "pin.location": [-70, 40],
        "order": "asc",
        "unit": "km",
        "distance_type": "arc"
      }
    }
  ]
}
```

옵션:
- `distance_type`: "arc" (기본값, 정확) 또는 "plane" (빠름, 덜 정확)
- `unit`: 거리 단위 (기본값 "m" 미터)
- `mode`: "min", "max", "median" 또는 "avg"

### 6.7 누락된 값 처리

`missing` 매개변수는 정렬 필드가 없는 문서의 동작을 지정합니다:

```json
{
  "sort": [
    { "price": { "missing": "_last" } }
  ]
}
```

`_last` (기본값), `_first` 또는 사용자 지정 값을 사용합니다.

### 6.8 매핑되지 않은 필드 처리

인덱스 간에 특정 필드의 매핑이 없을 때 오류를 방지하려면 `unmapped_type`을 사용합니다:

```json
{
  "sort": [
    { "price": { "unmapped_type": "long" } }
  ]
}
```

### 6.9 스크립트 기반 정렬

Painless 스크립트를 통한 사용자 정의 정렬 로직:

```json
{
  "sort": {
    "_script": {
      "type": "number",
      "script": {
        "source": "doc['field_name'].value * params.factor",
        "params": { "factor": 1.1 }
      },
      "order": "asc"
    }
  }
}
```

### 6.10 성능 최적화

- 분석된 텍스트 필드 정렬을 피하고 keyword 또는 숫자 유형을 사용합니다
- 인덱스 설정으로 인덱스 시간 정렬을 활성화하면 쿼리 시간 정렬을 빠르게 할 수 있지만, 인덱싱 성능과 메모리 사용량에 영향을 줍니다
- 결과 순서가 중요하지 않을 때는 `_doc` 정렬이 가장 효율적입니다. 특히 스크롤 작업에 유용합니다

---

## 7. 검색 결과 페이지네이션

Elasticsearch는 검색 결과를 페이지네이션하기 위한 여러 전략을 제공하며, 각각 다른 사용 사례에 적합한 방식입니다.

### 7.1 From/Size 페이지네이션

기본 접근 방식은 `from`과 `size` 매개변수를 사용합니다:

```json
GET /_search
{
  "from": 5,
  "size": 20,
  "query": {
    "match": {
      "user.id": "kimchy"
    }
  }
}
```

`from`은 건너뛸 결과 수를 지정하고 (기본값: 0), `size`는 페이지당 반환할 히트 수를 제한합니다.

제한 사항: 이 방법으로 너무 깊이 페이지를 넘기거나 한 번에 너무 많은 결과를 요청하는 것은 피해야 합니다. 각 샤드가 이전 페이지를 메모리에 로드해야 하므로 성능이 크게 저하될 수 있습니다.

기본적으로 `index.max_result_window` 설정으로 인해 이 방법으로는 10,000개를 초과하는 히트를 페이지할 수 없습니다.

### 7.2 Point in Time (PIT)과 Search After

깊은 페이지네이션에는 `search_after`를 Point in Time과 결합하는 방식이 권장됩니다. 이 방법은 인덱스 상태 일관성을 유지합니다.

1단계: PIT 생성

```json
POST /my-index-000001/_pit?keep_alive=1m
```

2단계: 초기 검색 실행

```json
GET /_search
{
  "size": 10000,
  "query": {
    "match": { "user.id": "elkbee" }
  },
  "pit": {
    "id": "46ToAwMDaWR5BXV1aWQyKwZub2RlXzM...",
    "keep_alive": "1m"
  },
  "sort": [
    { "@timestamp": { "order": "asc" } }
  ]
}
```

3단계: 정렬 값을 사용하여 페이지네이션

```json
GET /_search
{
  "pit": {
    "id": "46ToAwMDaWR5BXV1aWQyKwZub2RlXzM...",
    "keep_alive": "1m"
  },
  "sort": [ { "@timestamp": { "order": "asc" } } ],
  "search_after": ["2021-05-20T05:30:04.832Z", 4294967298],
  "track_total_hits": false
}
```

이 방법은 요청 간 일관된 순서를 보장하기 위해 암시적 `_shard_doc` 타이브레이커 필드를 자동으로 추가합니다. 정렬 순서가 `_shard_doc`이고 전체 히트를 추적하지 않을 경우 search_after 요청에 추가적인 성능 최적화가 적용됩니다.

### 7.3 Scroll API

스크롤 메커니즘은 검색 컨텍스트를 유지하면서 대규모 결과 세트를 검색합니다:

```json
POST /my-index-000001/_search?scroll=1m
{
  "size": 100,
  "query": { "match": { "message": "foo" } }
}
```

후속 페이지네이션 요청에는 반환된 `_scroll_id`를 사용합니다:

```json
POST /_search/scroll
{
  "scroll": "1m",
  "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAD4W..."
}
```

중요: 스크롤은 실시간 사용자 요청이 아닌 대량 데이터 처리용입니다. 스크롤 컨텍스트를 열어두면 리소스를 소비하므로, 작업이 완료되면 clear-scroll API로 명시적으로 정리해야 합니다.

### 7.4 슬라이스 스크롤

대규모 데이터셋을 병렬로 처리할 때는 슬라이스 스크롤을 사용해 결과를 여러 독립적인 커서로 분할합니다:

```json
GET /my-index-000001/_search?scroll=1m
{
  "slice": {
    "id": 0,
    "max": 2
  },
  "query": { "match": { "message": "foo" } }
}
```

각 슬라이스를 독립적으로 처리하면 대량 작업의 처리량을 향상시킬 수 있습니다.

### 7.5 주요 권장 사항

10,000개 이상의 히트를 페이지하면서 인덱스 상태를 보존해야 한다면, 더 이상 권장되지 않는 scroll 방식 대신 `search_after` 매개변수를 Point in Time (PIT)과 함께 사용하세요. 이 방법이 더 나은 성능과 리소스 효율성을 제공합니다.

---

## 8. 선택된 필드 검색

Elasticsearch는 전체 문서를 반환하는 대신 검색 결과에서 특정 필드만 가져오는 여러 방법을 제공합니다. 주요 방법은 `fields` 매개변수와 `_source` 필터링이며, 특수 사용 사례를 위한 추가 옵션도 있습니다.

### 8.1 Fields 매개변수 (권장)

`fields` 옵션은 필드 검색에 권장되는 방법입니다. 각 값을 매핑 유형에 맞는 표준화된 방식으로 반환하며, 다중 필드와 필드 별칭을 지원합니다. 주요 이점은 다음과 같습니다:

- 날짜와 공간 데이터 유형을 적절하게 형식화
- 런타임 필드 값 검색
- 인덱스 시간에 스크립트로 계산된 필드 반환
- `ignore_above` 및 `null_value`와 같은 매핑 옵션 준수
- 단일 값에도 항상 배열로 값 반환

예제 요청:

```json
POST my-index-000001/_search
{
  "query": { "match": { "user.id": "kimchy" } },
  "fields": [
    "user.id",
    "http.response.*",
    { "field": "@timestamp", "format": "epoch_millis" }
  ],
  "_source": false
}
```

응답은 `fields` 섹션 아래에 각 필드가 값 배열을 담은 평탄한 구조로 반환됩니다.

### 8.2 Source 필터링

`_source` 매개변수를 사용하면 원본 문서 소스에서 특정 필드를 포함하거나 제외할 수 있습니다. 세 가지 형식을 지원합니다:

단순 비활성화:

```json
"_source": false
```

전체 소스를 제외합니다.

와일드카드 패턴:

```json
"_source": "obj.*"
```

포함/제외 로직:

```json
"_source": {
  "includes": ["obj1.*", "obj2.*"],
  "excludes": ["*.description"]
}
```

이 방법은 인덱싱된 그대로 데이터를 반환하지만, 특정 필드를 요청할 때도 전체 `_source` 객체를 파싱해야 합니다.

### 8.3 대체 방법

특수 시나리오를 위한 추가 필드 검색 옵션도 있습니다:

Doc value 필드: `docvalue_fields` 매개변수는 정렬 및 집계에 최적화된 열 기반 필드 값을 가져오며, 전체 `_source` 문서 로드를 피합니다.

저장된 필드: `stored_fields` 매개변수는 매핑에서 `store: true`로 명시적으로 지정된 필드를 반환하지만, 일반적으로 권장되지 않는 방법입니다.

스크립트 필드: `script_fields`를 통해 스크립트로 계산된 값을 검색 결과와 함께 반환할 수 있습니다.

### 8.4 주요 고려 사항

Elasticsearch에는 전용 배열 유형이 없기 때문에 `fields` 매개변수는 항상 배열로 결과를 반환합니다. 중첩 필드의 경우 값을 평탄화하지 않고 중첩 구조를 유지합니다. 매핑되지 않았지만 `_source`에 존재하는 필드는 `include_unmapped: true`로 검색할 수 있습니다.

성능과 유연성 면에서 `fields` 옵션을 사용하는 것이 일반적으로 더 나은 선택입니다.

---

## 9. 하이라이팅

검색 요청에 `highlight` 매개변수를 지정하면 하이라이팅을 사용할 수 있습니다. 하이라이팅 기능은 쿼리 일치가 발생한 위치를 보여주는 관련 텍스트 조각을 추출합니다.

### 9.1 하이라이터 유형

#### Unified 하이라이터 (기본값)

Unified 하이라이터는 `text` 및 `keyword` 필드의 기본 하이라이터입니다. Lucene Unified Highlighter를 사용하며, 텍스트를 문장으로 분리한 후 BM25 알고리즘으로 개별 문장에 점수를 매깁니다. 정확한 구문 매칭과 다중 용어 하이라이팅을 지원하며, 여러 필드의 일치를 병합하는 기능도 제공합니다.

#### Semantic 하이라이터

`semantic_text` 필드를 위해 특별히 설계된 하이라이터로, 쿼리와 각 조각 간의 시맨틱 유사도를 기반으로 가장 관련성 높은 조각을 식별하고 추출합니다.

#### Plain 하이라이터

표준 Lucene 하이라이터를 사용합니다. 단어 중요도와 구문 쿼리의 단어 위치 기준 측면에서 쿼리 매칭 로직을 반영하려 하지만, 단일 필드에서 단순한 쿼리 일치를 하이라이팅하는 데 가장 적합합니다. 각 문서에 대해 메모리 내 인덱스를 다시 생성합니다.

#### Fast Vector 하이라이터 (FVH)

Lucene Fast Vector Highlighter를 사용하며, 매핑에서 `term_vector`가 `with_positions_offsets`로 설정된 필드에서 사용할 수 있습니다. 경계 스캐닝 커스터마이징을 지원하며, 위치에 따라 다른 가중치를 부여할 수 있습니다.

### 9.2 기본 사용 예제

```json
GET /_search
{
  "query": {
    "match": { "content": "kimchy" }
  },
  "highlight": {
    "fields": {
      "content": {}
    }
  }
}
```

### 9.3 오프셋 전략

스니펫 생성에 필요한 문자 오프셋은 세 가지 소스에서 가져옵니다:

1. Postings 리스트 - `index_options`가 `offsets`로 설정된 경우, 텍스트 재분석 없이 인덱스에서 직접 오프셋을 가져옵니다
2. Term 벡터 - `term_vector`가 `with_positions_offsets`로 설정된 경우 사용할 수 있으며, 특히 대용량 필드에 효율적입니다
3. Plain 하이라이팅 - 대안이 없을 때 메모리 내 인덱스를 생성합니다. 기본적으로 1,000,000자로 제한됩니다

### 9.4 중요 제한 사항

하이라이터는 복잡한 불리언 쿼리 로직을 항상 일관되게 반영하지는 않습니다. 또한 대용량 텍스트에 대한 plain 하이라이팅은 상당한 시간과 메모리가 필요할 수 있습니다.

### 9.5 고급 하이라이팅 옵션

```json
GET /_search
{
  "query": {
    "match": { "content": "elasticsearch guide" }
  },
  "highlight": {
    "pre_tags": ["<em>"],
    "post_tags": ["</em>"],
    "fields": {
      "content": {
        "fragment_size": 150,
        "number_of_fragments": 3,
        "order": "score"
      }
    }
  }
}
```

옵션:
- `pre_tags` / `post_tags`: 하이라이트 태그 사용자 정의
- `fragment_size`: 각 조각의 문자 크기
- `number_of_fragments`: 반환할 조각 수
- `order`: 점수순으로 조각 정렬

---

## 10. 검색 결과 접기

Elasticsearch의 접기(collapse) 기능을 사용하면 필드 값을 기반으로 검색 결과를 그룹화하여, 그룹당 상위 랭킹 문서만 선택할 수 있습니다. 결과 중복 제거나 유사한 문서 그룹화에 특히 유용합니다.

### 10.1 기본 접기

collapse 매개변수에는 doc_values가 활성화된 단일 값 keyword 또는 숫자 필드가 필요합니다.

예를 들어 `http.response.bytes`로 정렬하면서 `user.id`로 접으면 사용자당 가장 높은 랭킹의 문서만 반환됩니다:

```json
GET my-index-000001/_search
{
  "query": {
    "match": { "message": "GET /search" }
  },
  "collapse": {
    "field": "user.id"
  },
  "sort": [
    { "http.response.bytes": { "order": "desc" } }
  ]
}
```

### 10.2 Inner Hits로 접힌 결과 확장

inner_hits를 사용하면 접기 그룹당 여러 문서를 가져올 수 있습니다:

```json
{
  "collapse": {
    "field": "user.id",
    "inner_hits": {
      "name": "most_recent",
      "size": 5,
      "sort": [ { "@timestamp": "desc" } ]
    }
  }
}
```

### 10.3 다중 Inner Hits 표현

동일한 접힌 데이터를 다른 관점으로 볼 수 있습니다:

```json
{
  "inner_hits": [
    {
      "name": "largest_responses",
      "sort": [ { "http.response.bytes": { "order": "desc" } } ]
    },
    {
      "name": "most_recent",
      "sort": [ { "@timestamp": { "order": "desc" } } ]
    }
  ]
}
```

### 10.4 고급 기능

Search After: 페이지네이션을 위해 접기와 `search_after`를 함께 사용합니다:

```json
{
  "collapse": { "field": "user.id" },
  "sort": [ "user.id" ],
  "search_after": ["dd5ce1ad"]
}
```

Rescore: 그룹당 상위 문서에 대해 정밀한 랭킹을 적용하려면 접기와 리스코어링을 결합합니다.

2차 접기: 여러 필드로 계층적 그룹화를 하려면 inner_hits 내에서 접기를 적용합니다.

### 10.5 중요 제한 사항

- 접기는 scroll과 함께 사용할 수 없습니다
- 접기는 상위 히트에만 영향을 미치며, 집계에는 영향을 주지 않습니다
- 응답의 전체 히트 수는 접기 전에 일치한 문서 수를 반영합니다

---

## 11. 검색 결과 필터링

### 11.1 Post Filter 개요

`post_filter` 매개변수는 집계가 계산된 후 필터링을 적용합니다. 집계 결과는 변경하지 않고 검색 히트에만 영향을 주므로, 필터링된 결과와 함께 패싯 탐색이 필요할 때 유용합니다.

### 11.2 불리언 필터와의 주요 차이점

일반 불리언 필터는 검색 히트와 집계 모두에 적용되는 반면, post 필터는 검색 히트에만 적용됩니다. 이를 통해 표시 결과를 좁히면서도 더 넓은 범위의 집계 옵션을 함께 보여줄 수 있습니다.

### 11.3 실용적인 예제

브랜드, 색상, 모델 필드가 있는 셔츠 인벤토리 전자상거래 시나리오를 예로 들겠습니다. 사용자가 `color:red`와 `brand:gucci`로 필터링하되, 해당 색상의 사용 가능한 모델과 함께 *모든 색상*의 Gucci 셔츠 집계 수를 표시하고 싶은 경우가 있습니다.

해결책은 세 부분 구조를 사용하는 것입니다:

1. 메인 쿼리: 브랜드로만 필터링 (집계에서 모든 색상 허용)
2. 집계: 모든 Gucci 셔츠의 색상 빈도 계산
3. Post 필터: 검색 결과를 빨간색 항목으로만 제한

이 방식은 post 필터가 집계 범위를 줄이지 않도록 하여, 사용자 정제 워크플로우에서 패싯 수의 통계적 정확성을 유지합니다.

### 11.4 구현 패턴

```json
GET /shirts/_search
{
  "query": {
    "bool": {
      "filter": {
        "term": { "brand": "gucci" }
      }
    }
  },
  "aggs": {
    "colors": {
      "terms": { "field": "color" }
    },
    "color_red": {
      "filter": { "term": { "color": "red" } },
      "aggs": {
        "models": {
          "terms": { "field": "model" }
        }
      }
    }
  },
  "post_filter": {
    "term": { "color": "red" }
  }
}
```

post 필터는 일반 필터와 동일한 Query DSL 구문을 사용하며, 집계 계산이 완료된 후 적용됩니다.

---

## 12. 집계 (Aggregations)

집계는 메트릭, 통계, 분석을 통해 데이터를 요약합니다. 평균 응답 시간, 고객 가치 순위, 파일 크기 분포, 제품 카테고리 수 등 다양한 분석 질문에 답할 수 있습니다.

### 12.1 집계 유형

Elasticsearch는 집계를 세 가지 유형으로 분류합니다:

#### 메트릭 집계 (Metric Aggregations)

필드 값에서 합계, 평균 등의 메트릭을 계산합니다.

```json
GET /my-index-000001/_search
{
  "size": 0,
  "aggs": {
    "avg_price": {
      "avg": {
        "field": "price"
      }
    }
  }
}
```

#### 버킷 집계 (Bucket Aggregations)

필드 값, 범위 또는 기타 기준에 따라 문서를 버킷으로 그룹화합니다.

```json
GET /my-index-000001/_search
{
  "size": 0,
  "aggs": {
    "genres": {
      "terms": {
        "field": "genre"
      }
    }
  }
}
```

#### 파이프라인 집계 (Pipeline Aggregations)

문서나 필드 값 대신 다른 집계의 결과를 입력으로 받습니다.

### 12.2 집계 실행

기본 집계 구문은 Search API의 `aggs` 매개변수를 사용합니다:

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

결과는 응답의 `aggregations` 객체에 포함됩니다.

### 12.3 주요 집계 기법

범위 제한: `query` 매개변수로 집계 대상 문서를 제한합니다.

결과만 반환: 검색 히트 없이 집계 데이터만 반환하려면 `size`를 `0`으로 설정합니다.

다중 집계: 이름이 다른 여러 집계 객체를 지정하면 하나의 요청에서 여러 집계를 실행할 수 있습니다.

하위 집계: 계층적 분석을 위해 버킷 집계 내에 집계를 중첩합니다. 중첩 깊이 제한은 없습니다.

사용자 정의 메타데이터: `meta` 객체를 사용해 참조용 메타데이터를 집계에 첨부할 수 있습니다.

런타임 필드: 정확한 필드 일치가 없을 때 런타임 필드 스크립트를 사용해 계산된 값에 대해 집계합니다.

### 12.4 전체 집계 예제

```json
GET /sales/_search
{
  "size": 0,
  "query": {
    "range": {
      "date": {
        "gte": "2024-01-01",
        "lte": "2024-12-31"
      }
    }
  },
  "aggs": {
    "sales_per_month": {
      "date_histogram": {
        "field": "date",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_sales": {
          "sum": { "field": "amount" }
        },
        "avg_sale": {
          "avg": { "field": "amount" }
        }
      }
    },
    "sales_by_category": {
      "terms": {
        "field": "category.keyword",
        "size": 10
      },
      "aggs": {
        "category_total": {
          "sum": { "field": "amount" }
        }
      }
    }
  }
}
```

### 12.5 성능 고려 사항

샤드 요청 캐시는 자주 실행되는 집계 결과를 저장합니다. 일관된 `preference` 문자열을 사용하면 캐시 히트율을 높일 수 있습니다. 런타임 필드 계산은 오버헤드를 추가하며, 영향도는 집계 유형에 따라 다릅니다. 2^53을 초과하는 long 값은 내부 double 표현으로 인해 근사 결과가 반환됩니다.

---

## 13. Near Real-Time 검색

### 13.1 핵심 개념

Elasticsearch는 refresh 프로세스를 통해 거의 실시간 검색을 구현합니다. 문서는 저장 직후가 아니라 약 1초 후에 검색 가능해집니다.

### 13.2 작동 방식

이 메커니즘은 Lucene의 세그먼트 기반 아키텍처를 기반으로 합니다. 문서가 인덱싱되면 먼저 메모리 내 버퍼에 저장됩니다. refresh 작업 중에 버퍼의 문서는 새 세그먼트에 기록되고, 전체 디스크 커밋 없이 검색 가능한 상태가 됩니다.

Lucene은 새 세그먼트를 작성하고 열어서 포함된 문서를 전체 커밋 없이 검색에 노출시킵니다.

### 13.3 Refresh 간격

기본적으로 Elasticsearch는 매초 인덱스를 refresh합니다. 단, 중요한 조건이 있습니다: 지난 30초 동안 하나 이상의 검색 요청을 받은 인덱스에서만 refresh가 발생합니다.

refresh 시점을 제어하는 방법은 세 가지입니다:
1. 자동 refresh 간격 대기
2. 요청에 refresh 매개변수 사용
3. `POST _refresh`로 Refresh API 직접 호출

### 13.4 트레이드오프

이 방식은 성능과 신선도 사이의 균형을 맞춥니다. 파일시스템 캐시 덕분에 비용이 큰 디스크 플러시 없이도 세그먼트가 작성 직후 검색 가능해집니다. Elasticsearch가 스스로를 "거의" 실시간 검색이라고 설명하는 이유가 바로 이것입니다. 문서는 인덱싱 직후가 아니라 짧은 시간 후에 검색에 노출됩니다.

### 13.5 Refresh 설정

```json
PUT /my-index/_settings
{
  "index": {
    "refresh_interval": "30s"
  }
}
```

대량 인덱싱 중에는 refresh를 비활성화할 수 있습니다:

```json
PUT /my-index/_settings
{
  "index": {
    "refresh_interval": "-1"
  }
}
```

---

## 14. 비동기 검색

Elasticsearch 비동기 검색 API는 결과를 점진적으로 반환하는 논블로킹 검색 요청을 지원합니다.

### 14.1 핵심 기능

비동기 검색 엔드포인트는 `/_async_search` 또는 `/{index}/_async_search`에 대한 POST 요청을 처리합니다. Search API와 동일한 매개변수와 요청 본문을 사용하므로, 기존 검색 구문을 그대로 활용할 수 있습니다.

### 14.2 주요 매개변수

타이밍 제어:
- `wait_for_completion_timeout`: 지정한 시간 동안 검색 완료를 기다리며 차단
- `keep_alive`: 자동 삭제 전까지 비동기 검색 결과를 보존하는 기간 지정

응답 처리:
- `keep_on_completion`: true이면, 시간 초과 내에 검색이 완료되어도 나중에 결과를 조회할 수 있도록 유지
- `allow_partial_search_results`: 부분 실패 결과를 반환할지 여부 제어

### 14.3 기본 사용법

비동기 검색 제출:

```json
POST /my-index/_async_search
{
  "query": {
    "match": {
      "message": "elasticsearch"
    }
  },
  "size": 100
}
```

응답 구조:

응답에는 다음 필드가 포함됩니다:
- `id`: 이후 결과 조회에 사용하는 고유 식별자
- `is_partial`: 결과가 불완전한지 여부
- `is_running`: 검색이 진행 중인지 여부
- `response`: 집계, 히트, 샤드 정보가 포함된 부분 결과

부분 결과는 요청된 정렬 기준에 따라 순차적으로 제공되므로, 점진적인 결과 소비가 가능합니다.

결과 검색:

```json
GET /_async_search/FmRldE8zREVEUzA2ZVpUeGs2ejJFUFEaMkZ5QTVrSTZSaVN3WlNFVmtlWHJsdzoxMDc=
```

검색 삭제:

```json
DELETE /_async_search/FmRldE8zREVEUzA2ZVpUeGs2ejJFUFEaMkZ5QTVrSTZSaVN3WlNFVmtlWHJsdzoxMDc=
```

### 14.4 중요 제한 사항

API는 scroll 요청 또는 제안(suggest)만 포함된 요청을 지원하지 않습니다. 또한 Elasticsearch는 10MB를 초과하는 비동기 검색 응답을 저장할 수 없으며, `search.max_async_search_response_size` 클러스터 설정으로 이 임계값을 조정할 수 있습니다.

---

## 15. 검색 샤드 라우팅

Elasticsearch는 검색 샤드 라우팅 프로세스를 통해 여러 샤드와 노드에 걸쳐 검색 요청을 관리합니다. 이 메커니즘은 복제된 데이터로 내결함성을 유지하면서 검색 작업을 효율적으로 분산합니다.

### 15.1 적응형 복제본 선택

기본적으로 Elasticsearch는 다음 기준을 바탕으로 요청을 지능적으로 라우팅하는 적응형 복제본 선택(adaptive replica selection)을 사용합니다:

- 조정 노드와 후보 노드 간 이전 요청의 응답 시간
- 후보 노드의 과거 검색 성능
- 각 노드의 검색 스레드풀 현재 큐 깊이

이 방식은 가장 빠르게 응답할 수 있는 샤드 복사본을 선택하여 지연을 최소화합니다. 라운드 로빈 방식을 원한다면 `cluster.routing.use_adaptive_replica_selection` 설정으로 이 기능을 비활성화할 수 있습니다.

### 15.2 Preference 매개변수 사용

`preference` 쿼리 매개변수는 요청을 처리할 노드와 샤드를 제한합니다. 주요 값:

- `_local`: 조정 노드의 샤드를 우선 사용
- 사용자 정의 문자열: 반복 검색을 동일한 샤드로 라우팅하여 캐시 활용도를 높임

```
GET /my-index-000001/_search?preference=my-custom-shard-string
```

이 방법은 이전 인덱스가 비교적 정적인 시계열 데이터에 유용합니다.

### 15.3 라우팅 값

명시적 라우팅 값으로 인덱싱된 문서는 일치하는 라우팅 매개변수로 검색할 수 있습니다:

```
GET /my-index-000001/_search?routing=my-routing-value
```

여러 라우팅 값은 쉼표로 구분하여 지정할 수 있습니다.

### 15.4 동시성 제어

`max_concurrent_shard_requests` 매개변수(기본값: 5)는 노드당 병렬 샤드 요청 수를 제한합니다. 또한 `action.search.shard_count.limit` 클러스터 설정은 샤드 임계값을 초과하는 요청을 거부하여 쿼리가 클러스터를 과부하시키는 것을 방지합니다.

---

## 16. Inner Hits

Inner Hits를 사용하면 검색 결과를 일치시킨 정확한 중첩 또는 부모/자식 문서를 가져올 수 있습니다. 이 기능 없이는 어떤 하위 문서가 일치를 발생시켰는지 알 수 없으므로, 중첩 또는 부모/자식 구조에서 inner hits는 필수적입니다.

### 16.1 핵심 구문

Inner hits는 `nested`, `has_child`, `has_parent` 쿼리 내에서 다음 구조로 정의합니다:

```json
"<query>": {
  "inner_hits": {
    <inner_hits_options>
  }
}
```

응답에는 일치하는 하위 문서를 담은 `inner_hits` 객체가 포함됩니다.

### 16.2 구성 옵션

다음 옵션을 설정할 수 있습니다:

- `from`: inner hits 페이지네이션 오프셋
- `size`: 반환할 최대 히트 수 (기본값: 3)
- `sort`: inner hits 정렬 방식 (기본값: 점수순)
- `name`: 응답에서 inner hits를 식별하는 사용자 정의 이름

Inner hits는 하이라이팅, 소스 필터링, 스크립트 필드, doc value 필드, 버전 정보도 지원합니다.

### 16.3 Nested Inner Hits

계층적 중첩 필드의 경우, 쿼리와 일치하는 내부 객체를 가져올 수 있습니다. 응답에는 히트가 어떤 필드에서 왔는지와 오프셋 위치를 나타내는 `_nested` 메타데이터가 포함됩니다.

주요 이점: `inner_hits` 내 히트에서 반환된 `_source`는 `_nested` 메타데이터 기준 상대 경로이므로, 전체 부모 문서가 아닌 해당 중첩 객체의 데이터만 얻을 수 있습니다.

성능 최적화를 위해 소스 추출을 비활성화하고 doc value 필드를 활용하는 것이 좋습니다.

### 16.4 다중 수준 Nested 필드

깊이 중첩된 구조는 점 표기법(예: `comments.votes`)으로 접근할 수 있으며, 중첩 메타데이터는 계층적 경로를 반영합니다.

### 16.5 Parent/Child Inner Hits

중첩 히트와 유사하게, 부모/자식 관계에서도 일치를 발생시킨 문서를 노출할 수 있습니다. 응답의 이름은 조인 관계 유형에 해당하며, 일치하는 자식 또는 부모 문서가 inner hits 섹션에 나타납니다.

### 16.6 예제

```json
GET /my-index/_search
{
  "query": {
    "nested": {
      "path": "comments",
      "query": {
        "match": {
          "comments.text": "elasticsearch"
        }
      },
      "inner_hits": {
        "size": 3,
        "highlight": {
          "fields": {
            "comments.text": {}
          }
        }
      }
    }
  }
}
```

---

## 참고 자료

- [Elasticsearch 검색 개요](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-your-data.html)
- [Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
- [Search API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html)
- [Retrievers](https://www.elastic.co/guide/en/elasticsearch/reference/current/retriever.html)
- [집계](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html)
- [하이라이팅](https://www.elastic.co/guide/en/elasticsearch/reference/current/highlighting.html)
- [페이지네이션](https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html)
- [정렬](https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html)
- [필드 검색](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-fields.html)
- [비동기 검색](https://www.elastic.co/guide/en/elasticsearch/reference/current/async-search.html)
- [Near Real-Time 검색](https://www.elastic.co/guide/en/elasticsearch/reference/current/near-real-time.html)
