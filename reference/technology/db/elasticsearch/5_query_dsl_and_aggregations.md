# Elasticsearch Query DSL과 집계

## Query DSL

> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html

### 목차

1. [Query DSL이란?](#query-dsl이란)
2. [Query DSL로 검색 및 필터링](#query-dsl로-검색-및-필터링)
3. [Query DSL로 분석하기](#query-dsl로-분석하기)
4. [작동 방식](#작동-방식)
5. [쿼리 및 필터 컨텍스트](#쿼리-및-필터-컨텍스트)
6. [복합 쿼리](#복합-쿼리)
7. [전문 검색 쿼리](#전문-검색-쿼리)
8. [용어 수준 쿼리](#용어-수준-쿼리)
9. [지리 공간 쿼리](#지리-공간-쿼리)
10. [Shape 쿼리](#shape-쿼리)
11. [조인 쿼리](#조인-쿼리)
12. [Span 쿼리](#span-쿼리)
13. [특수 쿼리](#특수-쿼리)
14. [시맨틱 검색](#시맨틱-검색)
15. [비용이 많이 드는 쿼리](#비용이-많이-드는-쿼리)

---

### Query DSL이란?

Query DSL(Domain Specific Language)은 복잡한 검색, 필터링, 집계를 지원하는 완전한 기능의 JSON 스타일 쿼리 언어입니다. Elasticsearch의 원래 쿼리 언어이며 현재까지 가장 강력한 쿼리 언어입니다.

`_search` 엔드포인트는 Query DSL 구문으로 작성된 쿼리를 받습니다.

```json
GET /_search
{
  "query": {
    "match": {
      "message": "Elasticsearch"
    }
  }
}
```

---

### Query DSL로 검색 및 필터링

Query DSL은 여러 검색 기법을 지원합니다:

#### 검색 기능

| 검색 유형 | 설명 |
|----------|------|
| 전문 검색 (Full-text search) | 분석되고 인덱싱된 텍스트에서 구문 쿼리, 근접 검색, 퍼지 매칭을 지원합니다 |
| 키워드 검색 (Keyword search) | `keyword` 필드에서 정확한 일치를 수행합니다 |
| 시맨틱 검색 (Semantic search) | Elasticsearch에서 생성된 밀집 또는 희소 벡터 임베딩을 사용하여 `semantic_text` 필드를 검색합니다 |
| 벡터 검색 (Vector search) | 외부에서 생성된 임베딩에 대해 kNN 알고리즘을 사용하여 유사한 밀집 벡터를 검색합니다 |
| 지리 공간 검색 (Geospatial search) | 위치 쿼리 및 공간 관계 계산을 수행합니다 |

#### 필터링

필터링은 특정 필드 기준과 일치하는 문서를 포함하거나 제외할 수 있게 합니다. `filter` 파라미터를 사용하여 필터 컨텍스트를 나타냅니다.

```json
GET /_search
{
  "query": {
    "bool": {
      "must": {
        "match": { "title": "Search" }
      },
      "filter": {
        "term": { "status": "published" }
      }
    }
  }
}
```

---

### Query DSL로 분석하기

집계(Aggregations)는 Query DSL의 주요 분석 도구입니다. 데이터를 복잡하게 요약하여 핵심 메트릭, 패턴, 트렌드에 대한 인사이트를 얻을 수 있습니다.

#### 집계 유형

| 집계 유형 | 설명 |
|----------|------|
| 메트릭 (Metric) | 필드 값에서 합계, 평균 및 유사한 함수를 계산합니다 |
| 버킷 (Bucket) | 필드 값, 범위 또는 기준에 따라 문서를 그룹화합니다 |
| 파이프라인 (Pipeline) | 다른 집계의 결과에 대해 집계를 실행합니다 |

검색 API의 `aggs` 파라미터로 집계를 지정합니다. 집계는 검색 쿼리 컨텍스트 내에서 동작합니다.

```json
GET /_search
{
  "query": {
    "match": { "title": "Elasticsearch" }
  },
  "aggs": {
    "avg_price": {
      "avg": { "field": "price" }
    },
    "by_category": {
      "terms": { "field": "category.keyword" }
    }
  }
}
```

---

### 작동 방식

Query DSL은 두 가지 절 유형으로 구성된 추상 구문 트리(Abstract Syntax Tree) 형태로 동작합니다:

#### 리프 쿼리 절 (Leaf Query Clauses)

리프 쿼리 절은 특정 필드에서 특정 값을 검색합니다. `match`, `term`, `range` 등의 쿼리가 해당하며, 단독으로 사용할 수 있습니다.

```json
GET /_search
{
  "query": {
    "term": {
      "status": "published"
    }
  }
}
```

#### 복합 쿼리 절 (Compound Query Clauses)

복합 쿼리 절은 다른 리프 또는 복합 쿼리를 감싸서 논리적으로 결합하거나(`bool`, `dis_max`) 동작을 변경합니다(`constant_score`).

```json
GET /_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "Search" } }
      ],
      "filter": [
        { "term": { "status": "published" } }
      ]
    }
  }
}
```

절의 동작은 쿼리 컨텍스트인지 필터 컨텍스트인지에 따라 달라집니다.

---

### 쿼리 및 필터 컨텍스트

#### 관련성 점수 (Relevance Scores)

기본적으로 Elasticsearch는 관련성 점수 순으로 결과를 정렬합니다. 관련성 점수는 각 문서가 쿼리와 얼마나 잘 일치하는지를 나타냅니다.

관련성 점수는 검색 API 응답의 `_score` 메타데이터 필드에 양의 부동 소수점 숫자로 반환됩니다. `_score`가 높을수록 더 관련성 있는 문서입니다. 점수 계산 방식은 쿼리 유형과 컨텍스트(쿼리/필터)에 따라 달라집니다.

> 참고: 쿼리 컨텍스트에서 계산된 점수는 단정밀도 부동 소수점 숫자로 표현되며, 유효 숫자의 정밀도는 24비트입니다. 이 정밀도를 초과하는 점수 계산은 정밀도 손실이 발생합니다.

#### 쿼리 컨텍스트 (Query Context)

쿼리 컨텍스트에서 쿼리 절은 "이 문서가 이 쿼리 절과 얼마나 잘 일치하는가?"에 대한 답을 구합니다.

일치 여부를 판단하는 것 외에, 쿼리 절은 `_score` 메타데이터 필드에 관련성 점수를 계산합니다.

쿼리 컨텍스트는 쿼리 절이 검색 API의 `query` 파라미터로 전달될 때 적용됩니다.

#### 필터 컨텍스트 (Filter Context)

필터 컨텍스트에서 쿼리 절은 "이 문서가 이 쿼리 절과 일치하는가?"에 예/아니오로 답합니다.

필터의 이점:

1. 단순한 이진 로직: 점수 계산 없이 예/아니오 판단
2. 성능: 관련성 점수 계산이 없어 쿼리보다 빠른 실행
3. 캐싱: Elasticsearch가 자주 사용되는 필터를 자동으로 캐싱
4. 리소스 효율성: 전문 쿼리에 비해 낮은 CPU 소비
5. 쿼리 조합: 점수가 매겨진 쿼리와 결합하여 결과를 효율적으로 정제

필터 컨텍스트는 다음과 같은 경우에 적용됩니다:
- `bool` 쿼리의 `filter` 또는 `must_not` 파라미터
- `constant_score` 쿼리의 `filter` 파라미터
- `filter` 집계

숫자 필드, 날짜, 타임스탬프, 불리언 값, 키워드 필드, 지오포인트, 지오쉐이프 등 구조화된 데이터에 적합합니다.

일반적인 사용 예:
- 날짜 범위 확인: `timestamp` 필드가 2015년과 2016년 사이인지
- 특정 필드 값 확인: `status`가 "published"인지

> 팁: 문서 점수에 영향을 주어야 하는 조건에는 쿼리 컨텍스트를, 나머지 조건에는 필터 컨텍스트를 사용하세요.

#### 쿼리 및 필터 컨텍스트 예제

다음은 `search` API에서 쿼리 컨텍스트와 필터 컨텍스트를 모두 사용하는 예입니다. 아래 쿼리는 다음 조건을 모두 만족하는 문서를 반환합니다:

- `title` 필드에 "search"라는 단어가 포함됨
- `content` 필드에 "Elasticsearch"라는 단어가 포함됨
- `status` 필드에 "published"라는 정확한 단어가 포함됨
- `publish_date` 필드에 2015년 1월 1일 이후의 날짜가 포함됨

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

설명:

1. `query` 파라미터는 쿼리 컨텍스트를 나타냅니다
2. `bool`과 두 개의 `match` 절은 점수 계산을 위해 쿼리 컨텍스트에서 작동합니다
3. `filter` 파라미터는 필터 컨텍스트를 나타냅니다; `term`과 `range` 절은 점수에 영향을 주지 않고 필터링합니다

---

### 복합 쿼리

복합 쿼리는 결과와 점수를 결합하거나, 동작을 변경하거나, 쿼리/필터 컨텍스트를 전환하기 위해 다른 복합 또는 리프 쿼리를 감싸는 래퍼 쿼리입니다.

#### bool 쿼리

`bool` 쿼리는 여러 쿼리를 논리 연산으로 결합합니다. Lucene의 `BooleanQuery`에 대응하며, 불리언 절을 통해 복잡한 검색 조건을 구성할 수 있습니다.

##### 절 유형

| 절 | 설명 | 점수 기여 |
|----|------|----------|
| `must` | 반환된 모든 문서에서 일치해야 하는 쿼리. 논리적 AND 연산자 역할 | 예 |
| `should` | 일치할 때 관련성을 높이는 선택적 쿼리. 논리적 OR 연산자 역할 | 예 |
| `filter` | 일치해야 하지만 점수에 영향을 주지 않는 쿼리. 필터 컨텍스트에서 실행되며 캐싱 가능 | 아니오 |
| `must_not` | 결과에 나타나지 않아야 하는 쿼리. 필터 컨텍스트에서 실행. 논리적 NOT 연산자 역할 | 아니오 |

```json
GET /_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "Search" } }
      ],
      "should": [
        { "match": { "content": "Elasticsearch" } }
      ],
      "filter": [
        { "term": { "status": "published" } },
        { "range": { "publish_date": { "gte": "2015-01-01" } } }
      ],
      "must_not": [
        { "term": { "deleted": true } }
      ]
    }
  }
}
```

`bool` 쿼리는 "더 많이 일치할수록 좋다" 방식으로, 일치하는 `must`와 `should` 절의 점수를 합산합니다. `filter`와 `must_not` 절은 점수에 기여하지 않고 문서를 효율적으로 필터링합니다.

##### minimum_should_match

`bool` 쿼리에 `should` 절이 하나 이상 있지만 `must`나 `filter` 절이 없는 경우 `minimum_should_match`의 기본값은 1입니다. 그렇지 않으면 기본값은 0입니다.

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "match": { "title": "quick" } },
        { "match": { "title": "brown" } },
        { "match": { "title": "fox" } }
      ],
      "minimum_should_match": 2
    }
  }
}
```

##### 명명된 쿼리

`_name` 파라미터를 사용하면 결과의 `matched_queries` 속성을 통해 어느 절이 특정 문서와 일치했는지 추적할 수 있습니다.

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "match": { "title": { "query": "quick", "_name": "quick_match" } } },
        { "match": { "title": { "query": "brown", "_name": "brown_match" } } }
      ]
    }
  }
}
```

#### boosting 쿼리

`positive` 쿼리에 일치하는 문서를 반환하되, `negative` 쿼리에도 일치하는 문서는 점수를 낮춥니다.

```json
GET /_search
{
  "query": {
    "boosting": {
      "positive": {
        "term": { "text": "apple" }
      },
      "negative": {
        "term": { "text": "pie" }
      },
      "negative_boost": 0.5
    }
  }
}
```

#### constant_score 쿼리

다른 쿼리를 필터 컨텍스트로 감싸고, 일치하는 모든 문서에 동일한 상수 `_score`를 부여합니다.

```json
GET /_search
{
  "query": {
    "constant_score": {
      "filter": {
        "term": { "status": "published" }
      },
      "boost": 1.2
    }
  }
}
```

#### dis_max 쿼리

여러 쿼리를 받아 일치하는 모든 문서를 반환합니다. 일치하는 절의 점수를 모두 합산하는 `bool` 쿼리와 달리, 가장 잘 일치하는 단일 쿼리 절의 점수를 사용합니다.

```json
GET /_search
{
  "query": {
    "dis_max": {
      "queries": [
        { "term": { "title": "Quick pets" } },
        { "term": { "body": "Quick pets" } }
      ],
      "tie_breaker": 0.7
    }
  }
}
```

`tie_breaker` 파라미터(0~1)는 최고 점수 절 외의 다른 일치 절 점수가 최종 점수에 얼마나 기여할지를 조정합니다.

#### function_score 쿼리

인기도, 최신성, 거리, 사용자 정의 스크립트 등의 함수를 사용해 메인 쿼리의 점수를 조정합니다.

```json
GET /_search
{
  "query": {
    "function_score": {
      "query": { "match_all": {} },
      "boost": 5,
      "functions": [
        {
          "filter": { "match": { "test": "bar" } },
          "random_score": {},
          "weight": 23
        },
        {
          "filter": { "match": { "test": "cat" } },
          "weight": 42
        }
      ],
      "max_boost": 42,
      "score_mode": "max",
      "boost_mode": "multiply",
      "min_score": 42
    }
  }
}
```

---

### 전문 검색 쿼리

전문 검색 쿼리(Full text queries)는 이메일 본문과 같은 분석된 텍스트 필드를 검색하기 위해 설계되었습니다. 인덱싱 시 사용된 것과 동일한 분석기를 쿼리 문자열에 적용합니다.

#### match 쿼리

전문 검색을 수행하는 표준 쿼리입니다. 퍼지 매칭, 구문 검색, 근접 쿼리를 지원합니다.

```json
GET /_search
{
  "query": {
    "match": {
      "message": {
        "query": "this is a test",
        "operator": "and"
      }
    }
  }
}
```

##### match 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `query` | (필수) 검색할 텍스트, 숫자, 부울 또는 날짜 값. 텍스트는 검색 전에 분석됩니다 |
| `analyzer` | 쿼리 텍스트를 처리하는 분석기. 기본값은 필드의 매핑된 분석기 |
| `operator` | 부울 로직 모드 - `OR` (기본값) 또는 `AND` |
| `boost` | 관련성 점수 승수 (기본값 1.0) |
| `fuzziness` | 편집 거리 허용 오차로 퍼지 매칭 활성화 (AUTO 또는 숫자 값) |
| `prefix_length` | 퍼지 매칭에서 변경되지 않는 문자 수 (기본값 0) |
| `max_expansions` | 쿼리가 확장되는 최대 용어 수 (기본값 50) |
| `minimum_should_match` | 문서가 일치하기 위해 필요한 최소 절 수 |
| `zero_terms_query` | 분석기가 모든 토큰을 제거할 때의 동작 - `none` (기본값) 또는 `all` |
| `lenient` | true이면 형식 불일치를 무시합니다 (기본값 false) |

##### 퍼지 매칭 예제

```json
GET /_search
{
  "query": {
    "match": {
      "message": {
        "query": "this is a testt",
        "fuzziness": "AUTO"
      }
    }
  }
}
```

#### match_phrase 쿼리

정확한 구문 또는 근접한 단어 순서를 매칭합니다.

```json
GET /_search
{
  "query": {
    "match_phrase": {
      "message": {
        "query": "this is a test",
        "slop": 2
      }
    }
  }
}
```

`slop` 파라미터는 구문 내 용어 사이에 허용되는 위치 수를 지정합니다.

#### match_phrase_prefix 쿼리

`match_phrase`와 유사하지만, 마지막 단어에 대해 접두사 검색을 수행합니다.

```json
GET /_search
{
  "query": {
    "match_phrase_prefix": {
      "message": {
        "query": "quick brown f",
        "max_expansions": 10
      }
    }
  }
}
```

#### match_bool_prefix 쿼리

각 용어를 `term` 쿼리로 매칭하는 `bool` 쿼리를 생성하되, 마지막 용어는 `prefix` 쿼리로 매칭합니다.

```json
GET /_search
{
  "query": {
    "match_bool_prefix": {
      "message": "quick brown f"
    }
  }
}
```

#### multi_match 쿼리

match 쿼리를 여러 필드로 확장하여 실행합니다.

```json
GET /_search
{
  "query": {
    "multi_match": {
      "query": "this is a test",
      "fields": ["subject", "message"]
    }
  }
}
```

##### multi_match 유형

| 유형 | 설명 |
|-----|------|
| `best_fields` | 가장 잘 일치하는 필드의 점수 사용 (기본값) |
| `most_fields` | 일치하는 모든 필드의 점수 결합 |
| `cross_fields` | 필드를 하나의 큰 필드처럼 취급 |
| `phrase` | 각 필드에서 `match_phrase` 쿼리 실행 |
| `phrase_prefix` | 각 필드에서 `match_phrase_prefix` 쿼리 실행 |
| `bool_prefix` | 각 필드에서 `match_bool_prefix` 쿼리 실행 |

```json
GET /_search
{
  "query": {
    "multi_match": {
      "query": "this is a test",
      "fields": ["subject^2", "message"],
      "type": "best_fields"
    }
  }
}
```

`^2`는 `subject` 필드에 2배의 부스트를 적용합니다.

#### combined_fields 쿼리

여러 필드를 하나의 결합된 필드로 인덱싱된 것처럼 취급하여 매칭합니다.

```json
GET /_search
{
  "query": {
    "combined_fields": {
      "query": "database systems",
      "fields": ["title", "abstract", "body"],
      "operator": "and"
    }
  }
}
```

#### query_string 쿼리

간결한 Lucene 쿼리 문자열 구문을 지원합니다. 단일 쿼리 문자열에서 AND/OR/NOT 조건과 다중 필드 검색이 가능합니다.

```json
GET /_search
{
  "query": {
    "query_string": {
      "query": "(new york city) OR (big apple)",
      "default_field": "content"
    }
  }
}
```

> 주의: `query_string` 쿼리는 구문이 유효하지 않으면 오류를 반환합니다. 최종 사용자에게 직접 노출하기에는 적합하지 않습니다.

#### simple_query_string 쿼리

사용자에게 직접 노출하기에 적합한, `query_string`보다 간단하고 견고한 구문입니다.

```json
GET /_search
{
  "query": {
    "simple_query_string": {
      "query": "\"fried eggs\" +(eggplant | potato) -frittata",
      "fields": ["title^5", "body"],
      "default_operator": "and"
    }
  }
}
```

##### simple_query_string 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | AND 연산 |
| `\|` | OR 연산 |
| `-` | 단일 토큰 부정 |
| `"` | 구문 검색을 위해 여러 토큰 래핑 |
| `*` | 용어 끝에서 접두사 쿼리 |
| `(` 및 `)` | 우선순위 |
| `~N` | 퍼지 검색 (용어 뒤) 또는 슬롭 (구문 뒤) |

#### intervals 쿼리

용어 순서와 근접성에 대한 세밀한 제어를 제공합니다.

```json
GET /_search
{
  "query": {
    "intervals": {
      "my_text": {
        "all_of": {
          "ordered": true,
          "intervals": [
            {
              "match": {
                "query": "my favorite food",
                "max_gaps": 0,
                "ordered": true
              }
            },
            {
              "any_of": {
                "intervals": [
                  { "match": { "query": "hot water" } },
                  { "match": { "query": "cold porridge" } }
                ]
              }
            }
          ]
        }
      }
    }
  }
}
```

---

### 용어 수준 쿼리

용어 수준 쿼리(Term-level queries)는 날짜, IP 주소, 제품 ID 등 구조화된 데이터에서 정확한 값으로 문서를 찾습니다. 전문 검색 쿼리와 달리 검색 용어를 분석하지 않고 필드에 저장된 정확한 값을 매칭합니다.

#### term 쿼리

지정된 필드에서 정확한 용어를 포함하는 문서를 반환합니다. 가격, 제품 ID, 사용자 이름 등 정확한 값 매칭에 사용합니다.

```json
GET /_search
{
  "query": {
    "term": {
      "status": {
        "value": "published",
        "boost": 1.0
      }
    }
  }
}
```

##### term 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `value` | (필수) 일치시킬 정확한 용어 |
| `boost` | 관련성 점수 승수 (기본값 1.0) |
| `case_insensitive` | true로 설정하면 대소문자를 구분하지 않는 일치 활성화 (7.10.0에서 추가) |

> 중요: `text` 필드에 `term` 쿼리를 사용하지 마세요. Elasticsearch는 기본적으로 분석 과정에서 `text` 필드 값을 변환합니다(구두점 제거, 토큰화, 소문자 변환 등). 분석되지 않은 정확한 용어를 검색하는 `term` 쿼리는 `text` 필드에서 자주 일치에 실패합니다. `text` 필드 검색에는 `match` 쿼리를 사용하세요.

#### terms 쿼리

지정된 필드에서 하나 이상의 정확한 용어가 포함된 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "terms": {
      "status": ["published", "pending"]
    }
  }
}
```

#### terms_set 쿼리

지정된 필드에서 최소 개수 이상의 정확한 용어가 포함된 문서를 반환합니다. 임계값은 필드 또는 스크립트로 정의합니다.

```json
GET /_search
{
  "query": {
    "terms_set": {
      "programming_languages": {
        "terms": ["c++", "java", "php"],
        "minimum_should_match_field": "required_matches"
      }
    }
  }
}
```

#### range 쿼리

지정된 범위 내의 값을 포함하는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "range": {
      "age": {
        "gte": 10,
        "lte": 20,
        "boost": 2.0
      }
    }
  }
}
```

##### range 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `gt` | 초과 (greater than) |
| `gte` | 이상 (greater than or equal to) |
| `lt` | 미만 (less than) |
| `lte` | 이하 (less than or equal to) |
| `format` | 필드 매핑의 날짜 형식 재정의 |
| `time_zone` | UTC 오프셋 또는 IANA 시간대를 사용한 날짜 변환 |
| `relation` | 범위 필드용: INTERSECTS (기본값), CONTAINS, WITHIN |
| `boost` | 관련성 점수 승수 (기본값 1.0) |

##### 날짜 범위 예제

```json
GET /_search
{
  "query": {
    "range": {
      "timestamp": {
        "gte": "now-1d/d",
        "lte": "now/d"
      }
    }
  }
}
```

##### 시간대 변환 예제

```json
GET /_search
{
  "query": {
    "range": {
      "timestamp": {
        "time_zone": "+01:00",
        "gte": "2020-01-01T00:00:00",
        "lte": "now"
      }
    }
  }
}
```

> 참고: text/keyword 필드에 대한 범위 쿼리는 `search.allow_expensive_queries`가 활성화되어 있어야 합니다.

#### exists 쿼리

해당 필드에 인덱싱된 값이 존재하는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "exists": {
      "field": "user"
    }
  }
}
```

#### prefix 쿼리

지정된 필드에서 특정 접두사로 시작하는 용어를 포함하는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "prefix": {
      "user.id": {
        "value": "ki"
      }
    }
  }
}
```

#### wildcard 쿼리

와일드카드 패턴에 일치하는 용어를 포함하는 문서를 반환합니다. `*`는 0개 이상의 문자, `?`는 단일 문자와 일치합니다.

```json
GET /_search
{
  "query": {
    "wildcard": {
      "user.id": {
        "value": "ki*y",
        "boost": 1.0,
        "rewrite": "constant_score_blended"
      }
    }
  }
}
```

#### regexp 쿼리

정규 표현식에 일치하는 용어를 포함하는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "regexp": {
      "user.id": {
        "value": "k.*y",
        "flags": "ALL",
        "case_insensitive": true,
        "max_determinized_states": 10000,
        "rewrite": "constant_score_blended"
      }
    }
  }
}
```

#### fuzzy 쿼리

검색 용어와 유사한 용어를 포함하는 문서를 반환합니다. Levenshtein 편집 거리로 유사성을 계산하며, 오타 처리에 유용합니다.

```json
GET /_search
{
  "query": {
    "fuzzy": {
      "user.id": {
        "value": "ki",
        "fuzziness": "AUTO",
        "max_expansions": 50,
        "prefix_length": 0,
        "transpositions": true,
        "rewrite": "constant_score_blended"
      }
    }
  }
}
```

#### ids 쿼리

문서 ID를 기준으로 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "ids": {
      "values": ["1", "4", "100"]
    }
  }
}
```

---

### 지리 공간 쿼리

Elasticsearch는 두 가지 필드 유형에 대한 지리 쿼리를 지원합니다. `geo_point` 필드는 위도/경도 쌍을 지원하고, `geo_shape` 필드는 점, 선, 원, 다각형, 다중 다각형 등을 지원합니다.

#### geo_bounding_box 쿼리

지정된 사각형 영역과 교차하는 지오쉐이프 또는 지오포인트를 가진 문서를 찾습니다.

```json
GET /_search
{
  "query": {
    "geo_bounding_box": {
      "pin.location": {
        "top_left": {
          "lat": 40.73,
          "lon": -74.1
        },
        "bottom_right": {
          "lat": 40.01,
          "lon": -71.12
        }
      }
    }
  }
}
```

#### geo_distance 쿼리

중심 좌표에서 지정한 거리 내에 있는 지오쉐이프 또는 지오포인트를 가진 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "geo_distance": {
      "distance": "12km",
      "pin.location": {
        "lat": 40,
        "lon": -70
      }
    }
  }
}
```

#### geo_grid 쿼리

다음과 교차하는 지오쉐이프 또는 지오포인트를 가진 문서를 반환합니다:
- 지정된 geohash
- 지정된 맵 타일
- 지정된 H3 빈 (지오포인트용)

```json
GET /_search
{
  "query": {
    "geo_grid": {
      "pin.location": {
        "geohash": "u0"
      }
    }
  }
}
```

#### geo_polygon 쿼리

지정된 다각형 경계와 교차하는 지오쉐이프 또는 지오포인트를 가진 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "geo_polygon": {
      "person.location": {
        "points": [
          { "lat": 40, "lon": -70 },
          { "lat": 30, "lon": -80 },
          { "lat": 20, "lon": -90 }
        ]
      }
    }
  }
}
```

#### geo_shape 쿼리

참조 지오쉐이프와 특정 공간 관계를 만족하는 지오쉐이프 또는 지오포인트를 가진 문서를 찾습니다.

지원되는 관계:
- `INTERSECTS` - 교차 (기본값)
- `DISJOINT` - 분리
- `WITHIN` - 내부
- `CONTAINS` - 포함

```json
GET /_search
{
  "query": {
    "geo_shape": {
      "location": {
        "shape": {
          "type": "envelope",
          "coordinates": [
            [13.0, 53.0],
            [14.0, 52.0]
          ]
        },
        "relation": "within"
      }
    }
  }
}
```

---

### Shape 쿼리

Shape 쿼리는 가상 세계, 스포츠 경기장, 테마파크, CAD 다이어그램 등에서 임의의 2차원(비지리 공간) 기하학 검색을 지원합니다.

Elasticsearch는 두 가지 데카르트 데이터 필드 유형을 지원합니다:

1. Point 필드: x/y 좌표 쌍 저장
2. Shape 필드: 점, 선, 원, 다각형, 다중 다각형을 포함한 여러 기하학 유형 지원

#### shape 쿼리

제공된 도형과 특정 공간 관계를 충족하는 도형을 포함하는 문서를 찾습니다.

```json
GET /_search
{
  "query": {
    "shape": {
      "geometry": {
        "shape": {
          "type": "envelope",
          "coordinates": [
            [1355.0, 5355.0],
            [1400.0, 5200.0]
          ]
        },
        "relation": "within"
      }
    }
  }
}
```

Shape 쿼리는 데카르트(비지리 공간) 좌표에서 동작하므로 지리 데이터 이외의 2차원 매핑 애플리케이션에 적합합니다.

---

### 조인 쿼리

분산 시스템인 Elasticsearch에서 전체 SQL 스타일 조인은 비용이 매우 높습니다. 이를 대신해 두 가지 주요 조인 접근 방식을 제공합니다.

#### nested 쿼리

`nested` 유형의 필드에서 동작합니다. 각 객체를 독립적인 문서로 쿼리할 수 있는 객체 배열을 인덱싱할 때 사용합니다.

```json
GET /_search
{
  "query": {
    "nested": {
      "path": "obj1",
      "query": {
        "bool": {
          "must": [
            { "match": { "obj1.name": "blue" } },
            { "range": { "obj1.count": { "gt": 5 } } }
          ]
        }
      },
      "score_mode": "avg"
    }
  }
}
```

##### nested 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `path` | (필수) 검색할 중첩 객체의 경로 |
| `query` | (필수) 중첩 객체에서 실행할 쿼리 |
| `score_mode` | 자식 히트의 점수가 부모 히트에 기여하는 방식 (avg, max, min, none, sum) |
| `ignore_unmapped` | 매핑되지 않은 경로를 무시할지 여부 (기본값 false) |

#### has_child 쿼리

자식 문서 조건에 일치하는 부모 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "has_child": {
      "type": "answer",
      "query": {
        "match": {
          "owner.display_name": "Jess"
        }
      }
    }
  }
}
```

#### has_parent 쿼리

부모 문서가 쿼리 조건을 만족하는 자식 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "has_parent": {
      "parent_type": "question",
      "query": {
        "match": {
          "title": "elasticsearch"
        }
      }
    }
  }
}
```

#### parent_id 쿼리

특정 부모 문서에 조인된 자식 문서만 반환합니다.

```json
GET /_search
{
  "query": {
    "parent_id": {
      "type": "answer",
      "id": "1"
    }
  }
}
```

> 참고: `search.allow_expensive_queries`가 `false`로 설정되면 조인 쿼리는 실행되지 않습니다. 조인 쿼리는 계산 비용이 높은 작업으로 분류됩니다.

---

### Span 쿼리

Span 쿼리는 용어 순서와 근접성을 정밀하게 제어할 수 있는 특수 위치 쿼리입니다. 법률 문서나 특허 같은 특정 도메인에서 주로 사용됩니다.

주요 제약사항:
- 외부 span 쿼리에서만 boost 설정 가능
- Span 쿼리는 비span 쿼리와 혼합할 수 없음 (`span_multi` 쿼리 제외)

#### span_term 쿼리

다른 span 쿼리와 함께 사용하기 위한 `term` 쿼리의 span 버전입니다.

```json
GET /_search
{
  "query": {
    "span_term": {
      "user.id": "kimchy"
    }
  }
}
```

#### span_multi 쿼리

span 쿼리 내에서 `term`, `range`, `prefix`, `wildcard`, `regexp`, `fuzzy` 쿼리를 사용할 수 있도록 감쌉니다.

```json
GET /_search
{
  "query": {
    "span_multi": {
      "match": {
        "prefix": {
          "user.id": {
            "value": "ki"
          }
        }
      }
    }
  }
}
```

#### span_first 쿼리

일치 항목이 필드의 처음 N개 위치 안에 나타나도록 제한합니다.

```json
GET /_search
{
  "query": {
    "span_first": {
      "match": {
        "span_term": { "user.id": "kimchy" }
      },
      "end": 3
    }
  }
}
```

#### span_near 쿼리

일치 항목이 서로 지정된 거리 내에 있어야 하며, 필요에 따라 동일한 순서를 요구할 수 있는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "span_near": {
      "clauses": [
        { "span_term": { "field": "quick" } },
        { "span_term": { "field": "brown" } },
        { "span_term": { "field": "fox" } }
      ],
      "slop": 12,
      "in_order": false
    }
  }
}
```

#### span_or 쿼리

여러 span 쿼리를 결합하여 그 중 하나 이상에 일치하는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "span_or": {
      "clauses": [
        { "span_term": { "field": "quick" } },
        { "span_term": { "field": "brown" } }
      ]
    }
  }
}
```

#### span_not 쿼리

`include` span에 일치하지만 `exclude` span에는 일치하지 않는 문서를 반환합니다.

```json
GET /_search
{
  "query": {
    "span_not": {
      "include": {
        "span_term": { "field": "hoya" }
      },
      "exclude": {
        "span_near": {
          "clauses": [
            { "span_term": { "field": "la" } },
            { "span_term": { "field": "hoya" } }
          ],
          "slop": 0,
          "in_order": true
        }
      }
    }
  }
}
```

#### span_containing 쿼리

`big` span 내에 `little` span이 포함된 결과만 반환합니다.

```json
GET /_search
{
  "query": {
    "span_containing": {
      "little": {
        "span_term": { "field": "foo" }
      },
      "big": {
        "span_near": {
          "clauses": [
            { "span_term": { "field": "bar" } },
            { "span_term": { "field": "baz" } }
          ],
          "slop": 5,
          "in_order": true
        }
      }
    }
  }
}
```

#### span_within 쿼리

`little` span이 `big` span 목록 안에 포함되어 있는 결과를 반환합니다.

```json
GET /_search
{
  "query": {
    "span_within": {
      "little": {
        "span_term": { "field": "foo" }
      },
      "big": {
        "span_near": {
          "clauses": [
            { "span_term": { "field": "bar" } },
            { "span_term": { "field": "baz" } }
          ],
          "slop": 5,
          "in_order": true
        }
      }
    }
  }
}
```

#### span_field_masking 쿼리

`span_near`, `span_or` 등의 쿼리가 서로 다른 필드에서도 동작할 수 있도록 합니다.

```json
GET /_search
{
  "query": {
    "span_near": {
      "clauses": [
        {
          "span_term": { "text": "quick brown" }
        },
        {
          "span_field_masking": {
            "query": { "span_term": { "text.stems": "fox" } },
            "field": "text"
          }
        }
      ],
      "slop": 5,
      "in_order": false
    }
  }
}
```

---

### 특수 쿼리

일반적인 쿼리 유형으로 처리하기 어려운 특수 검색 시나리오를 위한 쿼리들입니다.

#### distance_feature 쿼리

원점과 문서의 `date`, `date_nanos`, `geo_point` 필드 사이의 거리를 동적으로 계산하여 점수에 반영합니다. 경쟁력 없는 결과는 효율적으로 건너뜁니다.

```json
GET /_search
{
  "query": {
    "distance_feature": {
      "field": "production_date",
      "pivot": "7d",
      "origin": "now"
    }
  }
}
```

#### more_like_this 쿼리

제공된 텍스트, 특정 문서, 또는 여러 문서와 유사한 내용을 공유하는 문서를 찾습니다.

```json
GET /_search
{
  "query": {
    "more_like_this": {
      "fields": ["title", "description"],
      "like": "Once upon a time",
      "min_term_freq": 1,
      "max_query_terms": 12
    }
  }
}
```

#### percolate 쿼리

주어진 문서와 일치하는 저장된 쿼리를 찾는 역방향(reverse) 쿼리 방식입니다.

```json
GET /my-index/_search
{
  "query": {
    "percolate": {
      "field": "query",
      "document": {
        "message": "A new bonsai tree in the office"
      }
    }
  }
}
```

#### rank_feature 쿼리

숫자 피처 값을 기반으로 점수를 계산하고, 경쟁력 없는 히트를 효율적으로 건너뜁니다.

```json
GET /_search
{
  "query": {
    "rank_feature": {
      "field": "pagerank"
    }
  }
}
```

#### script 쿼리

스크립트를 필터링 수단으로 활용합니다. 복잡한 점수 로직에는 `function_score` 쿼리를 함께 사용하세요.

```json
GET /_search
{
  "query": {
    "script": {
      "script": {
        "source": "doc['num1'].value > 1",
        "lang": "painless"
      }
    }
  }
}
```

#### script_score 쿼리

사용자 정의 스크립트로 하위 쿼리의 점수를 수정하여 세밀한 점수 조정이 가능합니다.

```json
GET /_search
{
  "query": {
    "script_score": {
      "query": {
        "match": { "message": "elasticsearch" }
      },
      "script": {
        "source": "_score * doc['my-int'].value"
      }
    }
  }
}
```

#### wrapper 쿼리

다른 쿼리를 JSON 또는 YAML 문자열로 받아 쿼리를 저장하거나 동적으로 구성할 수 있게 합니다.

```json
GET /_search
{
  "query": {
    "wrapper": {
      "query": "eyJ0ZXJtIiA6IHsgInVzZXIuaWQiIDogImtpbWNoeSIgfX0="
    }
  }
}
```

위의 `query` 값은 Base64로 인코딩된 `{"term" : { "user.id" : "kimchy" }}`입니다.

#### pinned 쿼리

특정 문서를 기본 쿼리 결과의 나머지 문서보다 상위에 배치합니다.

```json
GET /_search
{
  "query": {
    "pinned": {
      "ids": ["1", "4", "100"],
      "organic": {
        "match": {
          "description": "iphone"
        }
      }
    }
  }
}
```

#### rule 쿼리

Query Rules API로 정의된 컨텍스트 기반 규칙을 쿼리에 적용합니다.

```json
GET /_search
{
  "query": {
    "rule": {
      "organic": {
        "match": {
          "description": "iphone"
        }
      },
      "ruleset_ids": ["my-ruleset"],
      "match_criteria": {
        "query_string": "iphone"
      }
    }
  }
}
```

---

### 시맨틱 검색

Elasticsearch는 자연어 처리(NLP)와 벡터 검색 기술을 활용하여 시맨틱 검색 기능을 제공합니다. 복잡성 수준이 다른 세 가지 구현 워크플로를 지원합니다.

#### 옵션 1: semantic_text (권장)

`semantic_text` 워크플로는 시맨틱 검색을 구현하는 가장 간단한 방법으로, 많은 수동 작업을 자동화합니다.

주요 특징:
- 인덱스 매핑 생성만으로 설정 완료
- 모델 설정과 파라미터를 수동 구성할 필요 없음
- 수집, 임베딩, 쿼리를 자동으로 처리
- 별도의 추론 수집 파이프라인 불필요

```json
PUT /my-index
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

#### 옵션 2: 추론 API 워크플로

옵션 1보다 세밀한 제어가 필요할 때 사용하는 중간 수준의 접근 방식입니다:

- 추론 엔드포인트 생성
- 모델 관련 설정과 파라미터 구성
- 인덱스 매핑 정의
- 필요 시 자동 임베딩을 위한 추론 수집 파이프라인 설정

#### 옵션 3: 수동 모델 배포

가장 복잡하고 수동 작업이 많은 워크플로입니다:

- Elasticsearch 지원 목록에서 NLP 모델 선택
- Eland 클라이언트로 모델 배포
- 인덱스 매핑 생성
- 데이터 수집을 위한 수집 파이프라인 구축

#### semantic_text 필드 쿼리

`semantic_text` 필드에서 `match` 쿼리를 사용하면 자동으로 시맨틱 검색이 수행됩니다:

```json
GET /my-index/_search
{
  "query": {
    "match": {
      "content": "how to implement search"
    }
  }
}
```

전용 `semantic` 쿼리를 사용할 수도 있습니다:

```json
GET /my-index/_search
{
  "query": {
    "semantic": {
      "field": "content",
      "query": "how to implement search"
    }
  }
}
```

---

### 비용이 많이 드는 쿼리

일부 쿼리는 실행 속도가 느리고 클러스터 안정성에 영향을 줄 수 있습니다. 해당 쿼리는 다음과 같습니다:

#### 선형 스캔 쿼리

| 쿼리 유형 | 설명 |
|----------|------|
| `script` 쿼리 | 각 문서에서 스크립트를 실행해야 함 |
| 인덱싱 없이 doc values가 활성화된 필드에 대한 쿼리 | numeric, date, boolean, ip, geo_point, keyword 필드 |

#### 높은 선행 비용 쿼리

| 쿼리 유형 | 설명 |
|----------|------|
| `fuzzy` 쿼리 | `wildcard` 필드 제외 |
| `regexp` 쿼리 | `wildcard` 필드 제외 |
| `prefix` 쿼리 | `wildcard` 필드 또는 `index_prefixes`가 있는 필드 제외 |
| `wildcard` 쿼리 | `wildcard` 필드 제외 |
| `text` 및 `keyword` 필드에 대한 `range` 쿼리 | - |

#### 기타 비용이 많이 드는 쿼리

| 쿼리 유형 | 설명 |
|----------|------|
| 조인 쿼리 | `nested`, `has_child`, `has_parent`, `parent_id` |
| `script_score` 쿼리 | - |
| `percolate` 쿼리 | - |

#### 비용이 많이 드는 쿼리 방지

`search.allow_expensive_queries`를 `false`로 설정하면 비용이 많이 드는 쿼리 실행을 차단할 수 있습니다(기본값은 `true`).

```json
PUT /_cluster/settings
{
  "persistent": {
    "search.allow_expensive_queries": false
  }
}
```

---

### 요약

Query DSL은 Elasticsearch의 핵심 쿼리 언어로, 복잡한 검색, 필터링, 집계를 위한 포괄적인 기능을 제공합니다. 주요 요점은 다음과 같습니다:

1. 리프 쿼리와 복합 쿼리로 검색 조건을 구성합니다
2. 쿼리 컨텍스트는 관련성 점수를 계산하고, 필터 컨텍스트는 이진 일치를 수행합니다
3. 복합 쿼리(특히 `bool`)로 여러 조건을 논리적으로 결합합니다
4. 전문 검색 쿼리는 분석된 텍스트에, 용어 수준 쿼리는 정확한 값에 사용합니다
5. 지리 공간 쿼리와 조인 쿼리로 특수한 검색 요구사항을 처리합니다
6. 시맨틱 검색은 `semantic_text` 필드를 통해 간단히 구현할 수 있습니다
7. 비용이 많이 드는 쿼리에 주의하고 필요 시 `search.allow_expensive_queries` 설정을 활용합니다

---

### 참고 자료

- [Query DSL 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
- [Query and filter context](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-filter-context.html)
- [Compound queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/compound-queries.html)
- [Full text queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/full-text-queries.html)
- [Term-level queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/term-level-queries.html)
- [Geo queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-queries.html)
- [Shape queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/shape-queries.html)
- [Joining queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/joining-queries.html)
- [Span queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/span-queries.html)
- [Specialized queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/specialized-queries.html)
- [Semantic search](https://www.elastic.co/guide/en/elasticsearch/reference/current/semantic-search.html)

---

## 집계 (Aggregations)

> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html

### 목차

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

### 집계 개요

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

### 집계 실행하기

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

### 집계 범위 변경

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

### 집계 결과만 반환하기

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

`size`를 `0`으로 설정하면 검색 히트가 반환되지 않아 불필요한 처리 비용을 줄일 수 있습니다.

---

### 여러 집계 실행하기

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

### 하위 집계

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

### 메타데이터 추가

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

### 집계 타입 반환

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

### 스크립트를 사용한 집계

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

스크립트는 문서의 원시 값에 접근할 수 있지만, 필드를 직접 집계하는 것보다 성능이 떨어집니다. 자주 검색하고 필터링하는 필드는 인덱싱하여 성능과 유연성의 균형을 맞추는 것이 좋습니다.

---

### 집계 캐싱

자주 실행되는 집계의 결과는 샤드 요청 캐시에 저장됩니다. 캐시된 결과를 얻으려면 동일한 `preference` 문자열을 사용해야 합니다. 검색 히트가 필요하지 않다면 `size`를 `0`으로 설정해 불필요한 데이터 반환을 생략할 수 있습니다.

Elasticsearch는 동일한 `preference` 문자열을 가진 검색을 같은 샤드로 라우팅합니다. 샤드 데이터가 변경되지 않았으면 해당 샤드는 캐시된 집계 결과를 반환합니다.

---

### 숫자 제한 사항

Elasticsearch는 `long` 정수에 대한 집계를 실행할 때 내부적으로 `double` 값을 사용합니다. 결과적으로 2^53보다 큰 `long` 값에 대한 집계는 근사치입니다.

---

### 집계 유형

#### 버킷 집계

버킷 집계는 메트릭 집계처럼 필드에 대한 메트릭을 계산하지 않습니다. 대신 문서의 버킷을 생성합니다. 각 버킷은 기준(집계에 따라 다름)과 연결되어 현재 컨텍스트의 문서가 "해당 버킷에 속하는지" 여부를 결정합니다. 집계는 버킷에 속하는 문서 수도 계산합니다. 즉, 버킷은 효과적으로 문서 집합을 정의합니다.

메트릭 집계와 달리 버킷 집계는 하위 집계를 가질 수 있습니다. 이 하위 집계는 부모 버킷 집계가 생성한 버킷에 대해 집계됩니다.

버킷 집계 유형마다 버킷을 정의하는 "버킷팅" 전략이 다릅니다:
- 단일 버킷을 정의하는 버킷 집계
- 고정된 수의 여러 버킷을 정의하는 버킷 집계
- 집계 과정에서 버킷을 동적으로 생성하는 버킷 집계

> 중요: `search.max_buckets` 클러스터 설정은 단일 응답에서 허용되는 버킷 수를 제한합니다.

##### 버킷 집계 종류

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

#### 메트릭 집계

메트릭 집계는 집계되는 문서에서 추출된 값을 기반으로 메트릭을 계산합니다. 값은 일반적으로 문서의 필드에서 추출되지만 스크립트를 사용하여 생성할 수도 있습니다.

메트릭 집계는 두 가지 범주로 나뉩니다:

| 범주 | 설명 |
|------|------|
| 단일 값 숫자 메트릭 | 단일 메트릭 값을 출력하는 집계 (예: 평균) |
| 다중 값 숫자 메트릭 | 여러 메트릭을 생성하는 집계 (예: 통계) |

이 구분은 버킷 집계의 하위 집계로 사용할 때 버킷 정렬 기능과 관련하여 중요합니다.

##### 메트릭 집계 종류

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

#### 파이프라인 집계

파이프라인 집계는 문서 집합이 아닌 다른 집계의 출력에서 작동하며, 출력 트리에 정보를 추가합니다. 파이프라인 집계에는 두 가지 유형이 있습니다:

| 유형 | 설명 |
|------|------|
| 부모 파이프라인 집계 | 부모 집계의 출력을 받아 새 버킷을 계산하거나 기존 버킷에 새 집계를 추가할 수 있음 |
| 형제 파이프라인 집계 | 동일한 계층 수준에서 형제 집계의 출력으로 작업하여 그들과 함께 새 집계를 계산할 수 있음 |

##### buckets_path 파라미터

대부분의 파이프라인 집계는 `buckets_path` 파라미터가 필요합니다. 이 파라미터는 집계 이름과 메트릭 이름을 지정하는 경로 구문을 사용합니다:

- 집계 이름은 `>`로 구분됩니다
- 메트릭 이름은 `.`으로 구분됩니다

예: `"my_bucket>my_stats.avg"`는 `my_bucket` 집계 내의 `my_stats` 집계의 평균값을 참조합니다.

##### 특수 경로 옵션

| 경로 | 설명 |
|------|------|
| `_count` | 특정 메트릭 대신 문서 수를 참조 |
| `_bucket_count` | 다중 버킷 집계가 반환한 버킷 수를 사용 |
| 대괄호 표기법 | 점이 포함된 집계 이름 처리 (예: `"my_percentile[99.9]"`) |

##### 갭 정책

데이터에 갭(누락된 값)이 있을 때 세 가지 정책으로 처리합니다:

| 정책 | 설명 |
|------|------|
| skip | 누락된 데이터를 존재하지 않는 것으로 취급하고 사용 가능한 값으로 계속 진행 |
| insert_zeros | 누락된 값을 0으로 대체 |
| keep_values | 사용 가능한 경우 null이 아닌 값을 사용하고 빈 버킷은 건너뜀 |

파이프라인 집계는 하위 집계를 가질 수 없지만 다른 파이프라인을 참조할 수 있어, 2차 미분과 같은 복잡한 계산을 위한 집계 체이닝이 가능합니다.

##### 파이프라인 집계 종류

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

### 요약

Elasticsearch 집계는 데이터 분석을 위한 강력한 도구입니다. 주요 포인트는 다음과 같습니다:

1. 세 가지 집계 유형: 메트릭, 버킷, 파이프라인 집계가 있으며 각각 다른 목적으로 사용됩니다
2. 중첩 가능: 버킷 집계 내에 하위 집계를 무제한으로 중첩할 수 있습니다
3. 범위 제어: `query` 파라미터로 집계 대상 문서를 제한할 수 있습니다
4. 성능 최적화: `size: 0`으로 집계 결과만 반환하고, 캐싱을 활용할 수 있습니다
5. 런타임 필드: 스크립트를 사용하여 동적으로 필드 값을 계산할 수 있지만 성능 오버헤드가 있습니다
6. 숫자 정밀도: 2^53보다 큰 `long` 값은 근사치로 처리됩니다

---

### 참고 자료

- [Elasticsearch Aggregations 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html)
- [Bucket Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket.html)
- [Metrics Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics.html)
- [Pipeline Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-pipeline.html)
