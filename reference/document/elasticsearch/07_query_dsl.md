# Query DSL

> 이 문서는 Elasticsearch 9.x 공식 문서의 Query DSL 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html

## 목차

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

## Query DSL이란?

Query DSL(Domain Specific Language)은 복잡한 검색, 필터링 및 집계를 가능하게 하는 완전한 기능을 갖춘 JSON 스타일 쿼리 언어입니다. 이것은 Elasticsearch의 원래 쿼리 언어이며 오늘날 가장 강력한 쿼리 언어입니다.

`_search` 엔드포인트는 Query DSL 구문으로 작성된 쿼리를 받아들입니다.

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

## Query DSL로 검색 및 필터링

Query DSL은 여러 검색 기법을 지원합니다:

### 검색 기능

| 검색 유형 | 설명 |
|----------|------|
| 전문 검색 (Full-text search) | 분석되고 인덱싱된 텍스트에서 구문 쿼리, 근접 검색, 퍼지 매칭을 지원합니다 |
| 키워드 검색 (Keyword search) | `keyword` 필드에서 정확한 일치를 수행합니다 |
| 시맨틱 검색 (Semantic search) | Elasticsearch에서 생성된 밀집 또는 희소 벡터 임베딩을 사용하여 `semantic_text` 필드를 검색합니다 |
| 벡터 검색 (Vector search) | 외부에서 생성된 임베딩에 대해 kNN 알고리즘을 사용하여 유사한 밀집 벡터를 검색합니다 |
| 지리 공간 검색 (Geospatial search) | 위치 쿼리 및 공간 관계 계산을 수행합니다 |

### 필터링

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

## Query DSL로 분석하기

집계(Aggregations)는 Query DSL의 주요 분석 도구입니다. 데이터의 복잡한 요약을 구축하고 핵심 메트릭, 패턴 및 트렌드에 대한 인사이트를 얻을 수 있습니다.

### 집계 유형

| 집계 유형 | 설명 |
|----------|------|
| 메트릭 (Metric) | 필드 값에서 합계, 평균 및 유사한 함수를 계산합니다 |
| 버킷 (Bucket) | 필드 값, 범위 또는 기준에 따라 문서를 그룹화합니다 |
| 파이프라인 (Pipeline) | 다른 집계의 결과에 대해 집계를 실행합니다 |

검색 API의 `aggs` 파라미터를 통해 집계를 지정합니다. 집계는 검색 쿼리 컨텍스트 내에서 작동합니다.

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

## 작동 방식

Query DSL은 두 가지 절 유형으로 구성된 추상 구문 트리(Abstract Syntax Tree)로 기능합니다:

### 리프 쿼리 절 (Leaf Query Clauses)

리프 쿼리 절은 특정 필드의 특정 값을 대상으로 합니다. `match`, `term`, `range`와 같은 쿼리가 있습니다. 독립적으로 사용할 수 있습니다.

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

### 복합 쿼리 절 (Compound Query Clauses)

복합 쿼리 절은 다른 리프 또는 복합 쿼리를 래핑하여 논리적으로 결합하거나(`bool`, `dis_max`) 동작을 변경합니다(`constant_score`).

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

## 쿼리 및 필터 컨텍스트

### 관련성 점수 (Relevance Scores)

기본적으로 Elasticsearch는 관련성 점수에 따라 결과를 정렬합니다. 관련성 점수는 각 문서가 쿼리와 얼마나 잘 일치하는지를 측정합니다.

관련성 점수는 검색 API 응답의 `_score` 메타데이터 필드에 양의 부동 소수점 숫자로 나타납니다. `_score`가 높을수록 문서가 더 관련이 있습니다. 점수 계산은 쿼리 유형에 따라 다르며 쿼리 컨텍스트인지 필터 컨텍스트인지에 따라 달라집니다.

> 참고: 쿼리 컨텍스트에서 쿼리에 대해 계산된 점수는 단정밀도 부동 소수점 숫자로 표현됩니다. 유효 숫자의 정밀도는 24비트만 있습니다. 유효 숫자의 정밀도를 초과하는 점수 계산은 정밀도 손실과 함께 부동 소수점으로 변환됩니다.

### 쿼리 컨텍스트 (Query Context)

쿼리 컨텍스트에서 쿼리 절은 "이 문서가 이 쿼리 절과 얼마나 잘 일치하는가?"라는 질문에 답합니다.

문서가 일치하는지 여부를 결정하는 것 외에도, 쿼리 절은 `_score` 메타데이터 필드에 관련성 점수를 계산합니다.

쿼리 컨텍스트는 검색 API의 `query` 파라미터와 같이 쿼리 절이 `query` 파라미터에 전달될 때마다 적용됩니다.

### 필터 컨텍스트 (Filter Context)

필터 컨텍스트에서 쿼리 절은 "이 문서가 이 쿼리 절과 일치하는가?"라는 질문에 답합니다. 대답은 간단한 예 또는 아니오입니다.

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

구조화된 데이터에 이상적: 숫자 필드, 날짜, 타임스탬프, 부울 값, 키워드 필드, 지오포인트 및 지오쉐이프.

일반적인 사용 사례:
- 날짜 범위 확인: 예를 들어 `timestamp` 필드가 2015년과 2016년 사이인지
- 특정 필드 값 확인: 예를 들어 상태가 "published"인지

> 팁: 매칭 문서의 점수에 영향을 미쳐야 하는 조건(즉, 문서가 얼마나 잘 일치하는지)에는 쿼리 컨텍스트에서 쿼리 절을 사용하고, 다른 모든 쿼리 절에는 필터 컨텍스트를 사용하세요.

### 쿼리 및 필터 컨텍스트 예제

다음은 `search` API에서 쿼리 및 필터 컨텍스트 모두에서 사용되는 쿼리 절의 예입니다. 이 쿼리는 다음 조건을 모두 만족하는 문서를 매칭합니다:

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

## 복합 쿼리

복합 쿼리는 결과와 점수를 결합하거나, 동작을 변경하거나, 쿼리와 필터 컨텍스트 간에 전환하기 위해 다른 복합 또는 리프 쿼리를 래핑하는 래퍼 쿼리입니다.

### bool 쿼리

`bool` 쿼리는 여러 쿼리를 논리적 연산을 사용하여 결합합니다. Lucene의 `BooleanQuery`에 매핑되며 부울 절을 통해 복잡한 검색 조건을 구축할 수 있습니다.

#### 절 유형

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

`bool` 쿼리는 "더 많은 일치가 더 좋다" 접근 방식을 사용하여 일치하는 `must`와 `should` 절의 점수를 합산합니다. `filter`와 `must_not` 절은 점수에 기여하지 않지만 문서를 효율적으로 제외합니다.

#### minimum_should_match

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

#### 명명된 쿼리

쿼리 내에서 `_name` 파라미터를 사용하면 결과의 `matched_queries` 속성을 통해 어떤 절이 특정 문서와 일치했는지 추적할 수 있습니다.

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

### boosting 쿼리

긍정적 쿼리와 일치하는 문서를 반환하면서 부정적 쿼리와도 일치하는 문서의 점수를 줄입니다.

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

### constant_score 쿼리

다른 쿼리를 필터 컨텍스트로 래핑하고 모든 일치하는 문서에 동일한 상수 `_score` 값을 부여합니다.

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

### dis_max 쿼리

여러 쿼리를 받아들이고 일치하는 모든 문서를 반환합니다. 모든 일치하는 절의 점수를 결합하는 `bool` 쿼리와 달리, 이 쿼리는 가장 잘 일치하는 단일 쿼리 절의 점수를 사용합니다.

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

`tie_breaker` 파라미터는 다른 일치하는 쿼리 절의 점수가 최종 점수에 얼마나 기여하는지 조정합니다 (0에서 1 사이).

### function_score 쿼리

인기도, 최신성, 거리 또는 사용자 정의 스크립팅 알고리즘과 같은 요소를 고려하는 함수를 사용하여 메인 쿼리의 점수를 수정합니다.

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

## 전문 검색 쿼리

전문 검색 쿼리(Full text queries)는 이메일 본문과 같은 분석된 텍스트 필드를 검색하도록 설계되었습니다. 이 쿼리들은 인덱싱 중에 사용된 것과 동일한 분석기를 쿼리 문자열에 적용합니다.

### match 쿼리

전문 검색을 수행하기 위한 표준 쿼리입니다. 퍼지 매칭과 구문 또는 근접 쿼리를 지원합니다.

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

#### match 쿼리 파라미터

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

#### 퍼지 매칭 예제

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

### match_phrase 쿼리

정확한 구문 또는 단어 근접 일치를 매칭합니다.

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

`slop` 파라미터는 구문의 용어 사이에 허용되는 위치의 수를 지정합니다.

### match_phrase_prefix 쿼리

`match_phrase`와 유사하지만 마지막 단어에 대해 와일드카드 검색을 수행합니다.

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

### match_bool_prefix 쿼리

각 용어를 `term` 쿼리로 매칭하는 `bool` 쿼리를 생성하지만, 마지막 용어는 `prefix` 쿼리로 매칭됩니다.

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

### multi_match 쿼리

여러 필드에서 match 쿼리 기능을 확장합니다.

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

#### multi_match 유형

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

### combined_fields 쿼리

여러 필드가 하나의 결합된 필드로 인덱싱된 것처럼 일치시킵니다.

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

### query_string 쿼리

컴팩트한 Lucene 쿼리 문자열 구문을 지원합니다. 단일 쿼리 문자열에서 AND/OR/NOT 조건과 다중 필드 검색을 활성화합니다.

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

> 주의: `query_string` 쿼리는 유효하지 않은 구문에 대해 오류를 반환할 수 있습니다. 최종 사용자에게 노출하기에는 적합하지 않습니다.

### simple_query_string 쿼리

사용자에게 직접 노출하기에 적합한 더 간단하고 견고한 `query_string` 구문 버전입니다.

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

#### simple_query_string 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | AND 연산 |
| `\|` | OR 연산 |
| `-` | 단일 토큰 부정 |
| `"` | 구문 검색을 위해 여러 토큰 래핑 |
| `*` | 용어 끝에서 접두사 쿼리 |
| `(` 및 `)` | 우선순위 |
| `~N` | 퍼지 검색 (용어 뒤) 또는 슬롭 (구문 뒤) |

### intervals 쿼리

용어 순서 및 근접성에 대한 세밀한 제어를 제공합니다.

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

## 용어 수준 쿼리

용어 수준 쿼리(Term-level queries)는 날짜, IP 주소, 제품 ID와 같은 구조화된 데이터에서 정확한 값을 사용하여 문서를 찾습니다. 전문 쿼리와 달리 이 쿼리는 검색 용어를 분석하지 않고 필드에 저장된 정확한 값을 일치시킵니다.

### term 쿼리

지정된 필드에서 정확한 용어를 포함하는 문서를 반환합니다. 가격, 제품 ID 또는 사용자 이름과 같은 정확한 값 일치에 사용합니다.

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

#### term 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `value` | (필수) 일치시킬 정확한 용어 |
| `boost` | 관련성 점수 승수 (기본값 1.0) |
| `case_insensitive` | true로 설정하면 대소문자를 구분하지 않는 일치 활성화 (7.10.0에서 추가) |

> 중요: text 필드에 term 쿼리를 사용하지 마세요. Elasticsearch는 기본적으로 분석의 일부로 text 필드 값을 수정합니다 (구두점 제거, 토큰화, 소문자 변환 등). 이로 인해 분석되지 않은 정확한 용어를 검색하는 term 쿼리는 text 필드에서 자주 실패합니다. text 필드를 검색하려면 match 쿼리를 사용하세요.

### terms 쿼리

지정된 필드에서 하나 이상의 정확한 용어를 포함하는 문서를 반환합니다.

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

### terms_set 쿼리

지정된 필드에서 최소 개수의 정확한 용어를 포함하는 문서를 반환합니다. 임계값은 필드 또는 스크립트를 통해 정의합니다.

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

### range 쿼리

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

#### range 쿼리 파라미터

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

#### 날짜 범위 예제

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

#### 시간대 변환 예제

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

### exists 쿼리

필드에 대해 인덱싱된 값이 있는 문서를 반환합니다.

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

### prefix 쿼리

지정된 필드에서 특정 접두사를 포함하는 문서를 반환합니다.

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

### wildcard 쿼리

와일드카드 패턴과 일치하는 용어를 포함하는 문서를 반환합니다. `*`는 0개 이상의 문자와 일치하고, `?`는 단일 문자와 일치합니다.

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

### regexp 쿼리

정규 표현식과 일치하는 용어를 포함하는 문서를 반환합니다.

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

### fuzzy 쿼리

검색 용어와 유사한 용어를 포함하는 문서를 반환합니다. 시스템은 Levenshtein 편집 거리를 사용하여 유사성을 계산하며, 오타 처리에 유용합니다.

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

### ids 쿼리

문서 ID를 기반으로 문서를 반환합니다.

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

## 지리 공간 쿼리

Elasticsearch는 두 가지 필드 유형에 대한 지리 쿼리 기능을 제공합니다: `geo_point` 필드(위도/경도 쌍 지원)와 `geo_shape` 필드(점, 선, 원, 다각형 및 다중 다각형 지원).

### geo_bounding_box 쿼리

지정된 사각형 영역과 교차하는 지오쉐이프 또는 지오포인트를 포함하는 문서를 찾습니다.

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

### geo_distance 쿼리

중심 좌표점에서 정의된 거리 내에 위치한 지오쉐이프 또는 지오포인트가 있는 문서를 식별합니다.

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

### geo_grid 쿼리

다음과 교차하는 지오쉐이프 또는 지오포인트가 있는 문서를 발견합니다:
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

### geo_polygon 쿼리

지정된 다각형 경계와 교차하는 지오쉐이프 또는 지오포인트가 있는 문서를 검색합니다.

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

### geo_shape 쿼리

참조 지오쉐이프와 특정 공간 관계를 보여주는 지오쉐이프 또는 지오포인트가 있는 문서를 찾습니다.

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

## Shape 쿼리

Shape 쿼리는 가상 세계, 스포츠 경기장, 테마파크 및 CAD 다이어그램과 같은 사용 사례를 지원하는 임의의 2차원(비지리 공간) 기하학 검색을 가능하게 합니다.

Elasticsearch는 두 가지 데카르트 데이터 필드 유형을 지원합니다:

1. Point 필드: x/y 좌표 쌍 저장
2. Shape 필드: 점, 선, 원, 다각형, 다중 다각형을 포함한 여러 기하학 유형 지원

### shape 쿼리

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

Shape 쿼리는 데카르트(비지리 공간) 좌표에서 작동하여 지리 데이터 이외의 2차원 매핑 애플리케이션에 적합합니다.

---

## 조인 쿼리

Elasticsearch와 같은 분산 시스템에서 전체 SQL 스타일 조인은 비용이 매우 높기 때문에, Elasticsearch는 조인을 수행하기 위한 두 가지 주요 접근 방식을 제공합니다.

### nested 쿼리

`nested` 유형의 필드와 함께 작동합니다. 각 객체가 독립적인 문서로 쿼리될 수 있는 객체 배열을 인덱싱하는 데 사용됩니다.

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

#### nested 쿼리 파라미터

| 파라미터 | 설명 |
|---------|------|
| `path` | (필수) 검색할 중첩 객체의 경로 |
| `query` | (필수) 중첩 객체에서 실행할 쿼리 |
| `score_mode` | 자식 히트의 점수가 부모 히트에 기여하는 방식 (avg, max, min, none, sum) |
| `ignore_unmapped` | 매핑되지 않은 경로를 무시할지 여부 (기본값 false) |

### has_child 쿼리

자식 문서 기준과 일치하는 부모 문서를 반환합니다.

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

### has_parent 쿼리

부모 문서가 쿼리를 만족하는 자식 문서를 반환합니다.

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

### parent_id 쿼리

특정 부모 문서에 조인된 자식 문서를 반환합니다.

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

> 참고: `search.allow_expensive_queries` 설정이 false로 구성된 경우 조인 쿼리는 실행되지 않습니다. 이러한 작업은 계산 비용이 많이 드는 것으로 간주됩니다.

---

## Span 쿼리

Span 쿼리는 용어 순서와 근접성에 대한 정확한 제어를 제공하는 특수한 위치 쿼리입니다. 법률 문서나 특허와 같은 특정 애플리케이션에 주로 사용됩니다.

주요 제약사항:
- 외부 span 쿼리에서만 boost 설정 가능
- Span 쿼리는 비span 쿼리와 혼합할 수 없음 (`span_multi` 쿼리 제외)

### span_term 쿼리

다른 span 쿼리와 함께 사용하기 위한 표준 term 쿼리의 span 쿼리 버전입니다.

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

### span_multi 쿼리

span 작업을 위해 term, range, prefix, wildcard, regexp 또는 fuzzy 쿼리를 래핑합니다.

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

### span_first 쿼리

일치 항목이 필드의 처음 N개 위치 내에 나타나도록 제한합니다.

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

### span_near 쿼리

일치 항목이 서로 지정된 거리 내에 있어야 하며, 가능하면 동일한 순서로 있어야 하는 문서를 반환합니다.

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

### span_or 쿼리

여러 span 쿼리를 결합하여 지정된 쿼리 중 하나와 일치하는 문서를 반환합니다.

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

### span_not 쿼리

해당 쿼리와 일치하는 문서를 제외합니다.

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

### span_containing 쿼리

보조 span 쿼리 기준과 일치하는 span 결과만 필터링합니다.

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

### span_within 쿼리

span이 다른 span 쿼리 목록이 반환한 span 내에 있는 한 결과를 반환합니다.

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

### span_field_masking 쿼리

span-near 또는 span-or와 같은 쿼리가 다른 필드에서 작동할 수 있게 합니다.

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

## 특수 쿼리

고유한 검색 시나리오를 처리하는 특수 쿼리 유형들입니다.

### distance_feature 쿼리

원점과 문서의 `date`, `date_nanos`, `geo_point` 필드 간의 동적으로 계산된 거리를 기반으로 점수를 계산합니다. 비경쟁적 결과를 효율적으로 건너뜁니다.

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

### more_like_this 쿼리

제공된 텍스트, 특정 문서 또는 여러 문서와 유사점을 공유하는 문서를 찾습니다.

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

### percolate 쿼리

주어진 문서와 일치하는 저장된 쿼리를 찾는 역방향 쿼리 접근 방식입니다.

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

### rank_feature 쿼리

숫자 특성의 값을 기반으로 점수를 계산하고 비경쟁적 히트를 효율적으로 건너뜁니다.

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

### script 쿼리

스크립트를 필터링 메커니즘으로 활용합니다. 복잡한 점수 로직을 위해 function_score 쿼리를 보완합니다.

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

### script_score 쿼리

사용자 정의 스크립트 로직을 통해 하위 쿼리의 점수를 수정하여 세밀한 점수 조작을 가능하게 합니다.

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

### wrapper 쿼리

다른 쿼리를 json 또는 yaml 문자열로 받아들여 쿼리 저장 및 동적 구성을 가능하게 합니다.

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

### pinned 쿼리

기본 쿼리와 일치하는 다른 문서보다 특정 문서를 상위에 배치합니다.

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

### rule 쿼리

Query Rules API를 사용하여 정의된 쿼리 기반 컨텍스트 규칙을 주어진 쿼리에 적용합니다.

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

## 시맨틱 검색

Elasticsearch는 자연어 처리(NLP) 및 벡터 검색 기술을 사용하여 시맨틱 검색 기능을 제공합니다. 플랫폼은 시맨틱 검색을 구현하기 위한 세 가지 별도의 워크플로를 제공하며, 각각 다양한 복잡성 수준을 가집니다.

### 옵션 1: semantic_text (권장)

`semantic_text` 워크플로는 시맨틱 검색을 구현하기 위한 가장 간단한 접근 방식입니다. 이 방법은 많은 수동 작업을 추상화합니다.

주요 기능:
- 인덱스 매핑 생성만으로 최소한의 설정 필요
- 모델 설정이나 파라미터를 수동으로 구성할 필요 없음
- 수집, 임베딩 및 쿼리의 자동 처리
- 별도의 추론 수집 파이프라인 생성 불필요

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

### 옵션 2: 추론 API 워크플로

이 중간 접근 방식은 구성에 대한 더 큰 제어를 제공합니다:

- 추론 엔드포인트 생성
- 모델 관련 설정 및 파라미터 구성
- 인덱스 매핑 정의
- 선택적으로 자동 임베딩을 위한 추론 수집 파이프라인 설정

### 옵션 3: 수동 모델 배포

가장 복잡하고 노동 집약적인 워크플로입니다. 단계:

- Elasticsearch의 지원 목록에서 NLP 모델 선택
- Eland 클라이언트를 사용하여 모델 배포
- 적절한 인덱스 매핑 생성
- 데이터 수집을 위한 수집 파이프라인 구축

### semantic_text 필드 쿼리

`semantic_text` 필드에서 `match` 쿼리를 사용하면 자동으로 시맨틱 검색을 수행합니다:

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

또는 전용 `semantic` 쿼리를 사용할 수 있습니다:

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

## 비용이 많이 드는 쿼리

특정 쿼리는 느리게 실행되며 클러스터 안정성에 영향을 미칠 수 있습니다. 이러한 쿼리에는 다음이 포함됩니다:

### 선형 스캔 쿼리

| 쿼리 유형 | 설명 |
|----------|------|
| `script` 쿼리 | 각 문서에서 스크립트를 실행해야 함 |
| 인덱싱 없이 doc values가 활성화된 필드에 대한 쿼리 | numeric, date, boolean, ip, geo_point, keyword 필드 |

### 높은 선행 비용 쿼리

| 쿼리 유형 | 설명 |
|----------|------|
| `fuzzy` 쿼리 | `wildcard` 필드 제외 |
| `regexp` 쿼리 | `wildcard` 필드 제외 |
| `prefix` 쿼리 | `wildcard` 필드 또는 `index_prefixes`가 있는 필드 제외 |
| `wildcard` 쿼리 | `wildcard` 필드 제외 |
| `text` 및 `keyword` 필드에 대한 `range` 쿼리 | - |

### 기타 비용이 많이 드는 쿼리

| 쿼리 유형 | 설명 |
|----------|------|
| 조인 쿼리 | `nested`, `has_child`, `has_parent`, `parent_id` |
| `script_score` 쿼리 | - |
| `percolate` 쿼리 | - |

### 비용이 많이 드는 쿼리 방지

`search.allow_expensive_queries` 설정을 `false`로 설정하면 비용이 많이 드는 쿼리를 방지할 수 있습니다 (기본값은 `true`).

```json
PUT /_cluster/settings
{
  "persistent": {
    "search.allow_expensive_queries": false
  }
}
```

---

## 요약

Query DSL은 Elasticsearch의 핵심 쿼리 언어로, 복잡한 검색, 필터링 및 집계를 위한 포괄적인 기능을 제공합니다. 주요 포인트는 다음과 같습니다:

1. 리프 쿼리와 복합 쿼리 를 사용하여 검색 조건을 구축합니다
2. 쿼리 컨텍스트 는 관련성 점수를 계산하고, 필터 컨텍스트 는 이진 일치를 수행합니다
3. 복합 쿼리 (특히 `bool`)를 사용하여 여러 조건을 논리적으로 결합합니다
4. 전문 검색 쿼리 는 분석된 텍스트에, 용어 수준 쿼리 는 정확한 값에 사용합니다
5. 지리 공간 쿼리 와 조인 쿼리 로 특수한 검색 요구 사항을 처리합니다
6. 시맨틱 검색 은 `semantic_text` 필드를 통해 쉽게 구현할 수 있습니다
7. 비용이 많이 드는 쿼리에 주의하고 필요시 `search.allow_expensive_queries` 설정을 사용합니다

---

## 참고 자료

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
