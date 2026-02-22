# Elasticsearch 매핑 (Mapping)

> 이 문서는 Elasticsearch 공식 문서의 Mapping 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html

## 목차

1. [매핑 개요](#매핑-개요)
2. [동적 매핑](#동적-매핑)
3. [명시적 매핑](#명시적-매핑)
4. [런타임 필드](#런타임-필드)
5. [필드 데이터 타입](#필드-데이터-타입)
6. [메타데이터 필드](#메타데이터-필드)
7. [매핑 파라미터](#매핑-파라미터)
8. [동적 템플릿](#동적-템플릿)
9. [매핑 제한 설정](#매핑-제한-설정)
10. [매핑 관리](#매핑-관리)

---

## 매핑 개요

매핑(Mapping)은 문서와 그 문서에 포함된 필드가 어떻게 저장되고 인덱싱되는지 정의하는 과정입니다. 각 문서는 필드들의 컬렉션이며, 각 필드는 고유한 데이터 타입을 가집니다.

데이터를 매핑할 때, 문서와 관련된 필드 목록을 포함하는 매핑 정의를 생성합니다. 매핑 정의에는 다음이 포함됩니다:

- 필드 정의: 각 필드의 이름, 데이터 타입, 그리고 매핑 파라미터
- 메타데이터 필드: `_source` 필드와 같이 문서의 관련 메타데이터가 어떻게 처리되는지 커스터마이징하는 필드
- 런타임 필드: 쿼리 시점에 평가되는 필드

### 매핑 접근 방식

Elasticsearch는 두 가지 매핑 접근 방식을 제공합니다:

1. 동적 매핑 (Dynamic Mapping): Elasticsearch가 문서의 필드 데이터 타입을 자동으로 감지하고 매핑을 생성합니다. 새로운 필드가 있는 추가 문서를 인덱싱하면 Elasticsearch가 자동으로 이 필드들을 추가합니다.

2. 명시적 매핑 (Explicit Mapping): 각 필드의 데이터 타입을 직접 지정하여 매핑을 정의합니다. 프로덕션 환경에서는 데이터가 인덱싱되는 방식을 완전히 제어할 수 있으므로 명시적 매핑을 권장합니다.

### 기본 매핑 예제

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "age":    { "type": "integer" },
      "email":  { "type": "keyword" },
      "name":   { "type": "text" }
    }
  }
}
```

---

## 동적 매핑

동적 매핑을 사용하면 Elasticsearch가 문서의 필드 데이터 타입을 자동으로 감지하고 매핑을 생성합니다. 새로운 필드의 자동 감지 및 추가를 동적 매핑 이라고 합니다.

### 동적 필드 매핑

동적 필드 매핑이 활성화되면 Elasticsearch는 특정 규칙을 사용하여 각 필드의 데이터 타입을 매핑하는 방법을 결정합니다. 동적으로 감지할 수 있는 필드 데이터 타입은 제한적이며, 다른 모든 데이터 타입은 명시적으로 매핑해야 합니다.

### JSON 데이터 타입 감지

| JSON 데이터 타입 | Elasticsearch 데이터 타입 |
|-----------------|-------------------------|
| `null` | 필드가 추가되지 않음 |
| `true` 또는 `false` | `boolean` |
| 부동 소수점 숫자 | `float` |
| 정수 | `long` |
| 객체 | `object` |
| 배열 | 첫 번째 null이 아닌 값에 따라 결정 |
| 문자열 | 날짜 감지를 통과하면 `date`, 숫자 감지를 통과하면 `float` 또는 `long`, 그 외에는 `text`와 `keyword` 하위 필드 |

> 참고: JSON은 long과 integer, double과 float을 구분하지 않으므로, 파싱된 모든 부동 소수점 숫자는 `double` JSON 데이터 타입으로 간주되고, 모든 파싱된 정수 숫자는 `long`으로 간주됩니다.

### dynamic 파라미터

`dynamic` 파라미터는 새로운 필드가 동적으로 추가되는지 여부를 제어합니다:

| 값 | 설명 |
|----|------|
| `true` | 새로운 필드가 매핑에 추가됩니다 (기본값) |
| `runtime` | 새로운 필드가 런타임 필드로 매핑에 추가됩니다. 이 필드들은 인덱싱되지 않으며 쿼리 시점에 `_source`에서 로드됩니다 |
| `false` | 새로운 필드는 무시됩니다. 이 필드들은 인덱싱되거나 검색 가능하지 않지만, 반환된 히트의 `_source` 필드에는 여전히 나타납니다 |
| `strict` | 새로운 필드가 감지되면 예외가 발생하고 문서가 거부됩니다. 새 필드는 매핑에 명시적으로 추가해야 합니다 |

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "user": {
        "properties": {
          "name": { "type": "text" },
          "social_networks": {
            "dynamic": true,
            "properties": {}
          }
        }
      }
    }
  }
}
```

### 날짜 감지

`date_detection`이 활성화되어 있으면 (기본값), 새로운 문자열 필드의 내용이 `dynamic_date_formats`에 지정된 날짜 패턴과 일치하는지 확인합니다. 일치하면 해당 형식의 새 `date` 필드가 추가됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "date_detection": true,
    "dynamic_date_formats": ["yyyy-MM-dd", "yyyy/MM/dd"]
  }
}
```

날짜 감지를 비활성화하려면:

```json
PUT /my-index-000001
{
  "mappings": {
    "date_detection": false
  }
}
```

### 숫자 감지

JSON은 네이티브 부동 소수점과 정수 데이터 타입을 지원하지만, 일부 애플리케이션이나 언어에서는 때때로 숫자를 문자열로 렌더링할 수 있습니다. 일반적으로 올바른 해결책은 이러한 필드를 명시적으로 매핑하는 것이지만, 숫자 감지(기본적으로 비활성화됨)를 활성화하여 자동으로 이를 수행할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "numeric_detection": true
  }
}

PUT /my-index-000001/_doc/1
{
  "my_float":   "1.0",
  "my_integer": "1"
}
```

---

## 명시적 매핑

당신은 Elasticsearch가 추측할 수 있는 것보다 데이터에 대해 더 잘 알고 있습니다. 동적 매핑이 시작하는 데 유용할 수 있지만, 어느 시점에서는 자신만의 명시적 매핑을 지정하고 싶을 것입니다. 인덱스를 생성할 때 필드 매핑을 만들고 기존 인덱스에 필드를 추가할 수 있습니다.

### 인덱스 생성 시 명시적 매핑 정의

인덱스 생성 API를 사용하여 명시적 매핑으로 새 인덱스를 생성할 수 있습니다:

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "age":    { "type": "integer" },
      "email":  { "type": "keyword" },
      "name":   { "type": "text" }
    }
  }
}
```

### 기존 인덱스에 필드 추가

업데이트 매핑 API를 사용하여 기존 인덱스에 하나 이상의 새 필드를 추가할 수 있습니다:

```json
PUT /my-index-000001/_mapping
{
  "properties": {
    "employee-id": {
      "type": "keyword",
      "index": false
    }
  }
}
```

위 예제에서 `employee-id`는 `keyword` 필드이며 `index` 매핑 파라미터 값이 `false`입니다. 이는 `employee-id` 필드의 값은 저장되지만 인덱싱되거나 검색에 사용할 수 없음을 의미합니다.

### 동적 매핑을 비활성화한 명시적 매핑

```json
PUT /my-explicit-mappings-books
{
  "mappings": {
    "dynamic": false,
    "properties": {
      "name": { "type": "text" },
      "author": { "type": "text" },
      "release_date": { "type": "date", "format": "yyyy-MM-dd" },
      "page_count": { "type": "integer" }
    }
  }
}
```

### 매핑 조회

Get Mapping API를 사용하여 인덱스의 매핑을 조회할 수 있습니다:

```json
GET /my-index-000001/_mapping
```

여러 인덱스의 매핑을 동시에 조회:

```json
GET /my-index-000001,my-index-000002/_mapping
```

클러스터의 모든 인덱스 매핑 조회:

```json
GET /_mapping
```

또는

```json
GET /*/_mapping
```

### 특정 필드 매핑 조회

특정 필드의 매핑만 조회하려면:

```json
GET /my-index-000001/_mapping/field/user
```

### 매핑 업데이트 제한 사항

지원되는 매핑 파라미터를 제외하고, 기존 필드의 매핑이나 필드 타입을 변경할 수 없습니다. 기존 필드를 변경하면 이미 인덱싱된 데이터가 무효화될 수 있습니다.

다른 인덱스에서 필드의 매핑을 변경해야 하는 경우, 올바른 매핑으로 새 인덱스를 만들고 데이터를 해당 인덱스로 리인덱싱해야 합니다.

### 여러 인덱스 동시 업데이트

업데이트 매핑 API는 단일 요청으로 여러 데이터 스트림 또는 인덱스에 적용할 수 있습니다:

```json
PUT /my-index-000001,my-index-000002/_mapping
{
  "properties": {
    "user": {
      "properties": {
        "name": { "type": "keyword" }
      }
    }
  }
}
```

---

## 런타임 필드

런타임 필드는 쿼리 시점에 평가되는 필드입니다. 런타임 필드를 사용하면:

- 데이터를 리인덱싱하지 않고 기존 문서에 필드를 추가할 수 있습니다
- 데이터 구조를 이해하지 않고 데이터 작업을 시작할 수 있습니다
- 쿼리 시점에 인덱싱된 필드에서 반환된 값을 재정의할 수 있습니다
- 기본 스키마를 수정하지 않고 특정 용도의 필드를 정의할 수 있습니다

### 런타임 필드 정의

매핑에서 `runtime` 섹션을 추가하고 Painless 스크립트를 정의하여 런타임 필드를 매핑합니다:

```json
PUT /my-index-000001
{
  "mappings": {
    "runtime": {
      "day_of_week": {
        "type": "keyword",
        "script": {
          "source": "emit(doc['@timestamp'].value.dayOfWeekEnum.getDisplayName(TextStyle.FULL, Locale.ENGLISH))"
        }
      }
    },
    "properties": {
      "@timestamp": { "type": "date" }
    }
  }
}
```

### 스크립트 없이 런타임 필드 정의

스크립트 없이 런타임 필드를 정의할 수도 있습니다. 예를 들어, `_source`에서 변경 없이 단일 필드를 검색하려는 경우 스크립트가 필요 없습니다:

```json
PUT /my-index-000001
{
  "mappings": {
    "runtime": {
      "day_of_week": {
        "type": "keyword"
      }
    }
  }
}
```

스크립트가 제공되지 않으면, Elasticsearch는 쿼리 시점에 런타임 필드와 동일한 이름의 필드를 `_source`에서 암묵적으로 찾아 값이 존재하면 반환합니다.

### 검색 요청에서 런타임 필드 정의

검색 요청에서 `runtime_mappings` 섹션을 지정하여 쿼리의 일부로만 존재하는 런타임 필드를 생성할 수 있습니다:

```json
GET /my-index-000001/_search
{
  "runtime_mappings": {
    "day_of_week": {
      "type": "keyword",
      "script": {
        "source": "emit(doc['@timestamp'].value.dayOfWeekEnum.getDisplayName(TextStyle.FULL, Locale.ENGLISH))"
      }
    }
  },
  "aggs": {
    "day_of_week": {
      "terms": {
        "field": "day_of_week"
      }
    }
  }
}
```

런타임 필드는 인덱스 매핑에서 동일한 이름으로 정의된 필드보다 우선합니다. 이 유연성으로 인해 필드 자체를 수정하지 않고도 기존 필드를 섀도잉하고 다른 값을 계산할 수 있습니다.

### 동적 런타임 필드

`"dynamic": "runtime"` 설정을 포함하면 Elasticsearch가 이 인덱스에서 추가 필드를 런타임 필드로 동적 생성하도록 지시합니다:

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic": "runtime",
    "properties": {
      "@timestamp": { "type": "date" }
    }
  }
}
```

### 런타임 필드의 장점

- 저장 비용 절감: 인덱스 매핑에서 직접 런타임 필드를 정의하여 수집 속도를 높이고 저장 비용을 절약합니다
- 즉시 사용 가능: 런타임 필드를 정의하면 검색 요청, 집계, 필터링 및 정렬에 즉시 사용할 수 있습니다
- 인덱스 크기 증가 없음: 런타임 필드는 인덱싱되지 않으므로 인덱스 크기가 증가하지 않습니다

### 런타임 필드 업데이트 및 제거

언제든지 런타임 필드를 업데이트하거나 제거할 수 있습니다. 기존 런타임 필드를 교체하려면 동일한 이름의 새 런타임 필드를 매핑에 추가합니다.

### 성능 고려 사항

런타임 필드는 디스크 공간을 적게 사용하고 데이터 접근 방식에 유연성을 제공하지만, 런타임 스크립트에 정의된 계산에 따라 검색 성능에 영향을 미칠 수 있습니다. 타임스탬프와 같이 자주 검색하고 필터링하는 필드는 인덱싱하여 검색 성능과 유연성의 균형을 맞추세요.

런타임 필드에 대한 쿼리는 비용이 많이 드는 것으로 간주됩니다. `search.allow_expensive_queries`가 `false`로 설정되면 비용이 많이 드는 쿼리가 허용되지 않으며 Elasticsearch는 런타임 필드에 대한 모든 쿼리를 거부합니다.

---

## 필드 데이터 타입

각 필드에는 필드 데이터 타입 또는 필드 타입이 있습니다. 이 타입은 필드가 포함하는 데이터의 종류(예: 문자열 또는 부울 값)와 의도된 용도를 나타냅니다. 예를 들어, 문자열을 `text`와 `keyword` 필드 모두에 인덱싱할 수 있습니다.

### 타입 패밀리

일부 필드 타입은 동일한 패밀리를 공유합니다. 동일한 패밀리의 타입은 정확히 동일한 검색 동작을 지원하지만 공간 사용이나 성능 특성이 다를 수 있습니다.

현재 두 가지 타입 패밀리가 있습니다:

1. keyword 패밀리: `keyword`, `constant_keyword`, `wildcard`
2. text 패밀리: `text`, `match_only_text`

다른 타입 패밀리는 단일 필드 타입만 가집니다. 예를 들어, `boolean` 타입 패밀리는 `boolean`이라는 하나의 필드 타입으로 구성됩니다.

### 일반적인 타입

#### 바이너리 (binary)
Base64로 인코딩된 이진 값입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "blob": { "type": "binary" }
    }
  }
}
```

#### 부울 (boolean)
`true`와 `false` 값입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "is_published": { "type": "boolean" }
    }
  }
}
```

Boolean 필드는 JSON `true` 및 `false` 값을 허용하지만, true 또는 false로 해석되는 문자열도 허용합니다.

#### 키워드 (Keywords)

키워드 패밀리에는 다음 필드 타입이 포함됩니다:

- keyword: ID, 이메일 주소, 호스트 이름, 상태 코드, 우편 번호 또는 태그와 같은 구조화된 콘텐츠용
- constant_keyword: 항상 동일한 값을 포함하는 키워드 필드용
- wildcard: 와일드카드 패턴으로 검색되는 비구조화된 기계 생성 콘텐츠용

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "tags": { "type": "keyword" },
      "constant_field": {
        "type": "constant_keyword",
        "value": "constant_value"
      }
    }
  }
}
```

> 팁: Elasticsearch는 `integer`나 `long`과 같은 숫자 필드를 범위 쿼리에 최적화합니다. 그러나 `keyword` 필드는 `term` 및 기타 용어 수준 쿼리에 더 적합합니다.

#### 숫자 (Numbers)

다음 숫자 데이터 타입이 지원됩니다:

| 타입 | 설명 |
|------|------|
| `long` | 최소값 -2^63, 최대값 2^63-1의 부호 있는 64비트 정수 |
| `integer` | 최소값 -2^31, 최대값 2^31-1의 부호 있는 32비트 정수 |
| `short` | 최소값 -32,768, 최대값 32,767의 부호 있는 16비트 정수 |
| `byte` | 최소값 -128, 최대값 127의 부호 있는 8비트 정수 |
| `double` | 유한값으로 제한된 배정밀도 64비트 IEEE 754 부동 소수점 숫자 |
| `float` | 유한값으로 제한된 단정밀도 32비트 IEEE 754 부동 소수점 숫자 |
| `half_float` | 유한값으로 제한된 반정밀도 16비트 IEEE 754 부동 소수점 숫자 |
| `scaled_float` | 고정 `double` 스케일링 팩터로 스케일링된 `long`으로 지원되는 부동 소수점 숫자 |
| `unsigned_long` | 최소값 0, 최대값 2^64-1의 부호 없는 64비트 정수 |

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "number_of_bytes": { "type": "integer" },
      "time_in_seconds": { "type": "float" },
      "price": {
        "type": "scaled_float",
        "scaling_factor": 100
      }
    }
  }
}
```

> 팁: 정수 타입(`byte`, `short`, `integer`, `long`)의 경우 사용 사례에 충분한 가장 작은 타입을 선택해야 합니다. 이는 인덱싱과 검색을 더 효율적으로 만듭니다.

#### 날짜 (Dates)

JSON에는 날짜 데이터 타입이 없으므로 Elasticsearch의 날짜는 다음 중 하나일 수 있습니다:

- 형식화된 날짜가 포함된 문자열 (예: `"2015-01-01"` 또는 `"2015/01/01 12:10:30"`)
- 에포크 이후 밀리초를 나타내는 숫자
- 에포크 이후 초를 나타내는 숫자

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "date": {
        "type": "date"
      }
    }
  }
}

PUT /my-index-000001/_doc/1
{ "date": "2015-01-01" }

PUT /my-index-000001/_doc/2
{ "date": "2015-01-01T12:10:30Z" }

PUT /my-index-000001/_doc/3
{ "date": 1420070400001 }
```

date_nanos: 나노초 해상도의 날짜

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "date": {
        "type": "date_nanos"
      }
    }
  }
}
```

#### 별칭 (alias)

기존 필드에 대한 대체 이름을 정의합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "distance": { "type": "long" },
      "route_length_miles": {
        "type": "alias",
        "path": "distance"
      }
    }
  }
}
```

### 객체 및 관계형 타입

#### 객체 (object)

JSON 객체입니다. Elasticsearch는 내부 객체 개념이 없으므로 객체 계층 구조를 필드 이름과 값의 단순 목록으로 평탄화합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "region": { "type": "keyword" },
      "manager": {
        "properties": {
          "age": { "type": "integer" },
          "name": {
            "properties": {
              "first": { "type": "text" },
              "last": { "type": "text" }
            }
          }
        }
      }
    }
  }
}
```

#### 플랫튼 (flattened)

전체 JSON 객체를 단일 필드로 매핑합니다. 임의의 키 집합이 있는 키-값 쌍을 수집할 때 유용합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "labels": { "type": "flattened" }
    }
  }
}
```

`flattened` 타입의 최대 허용 깊이는 `depth_limit` 파라미터로 제어하며 기본값은 20입니다. 이 값은 업데이트 매핑 API를 통해 동적으로 업데이트할 수 있습니다.

#### 중첩 (nested)

하위 필드 간의 관계를 보존하는 JSON 객체입니다. `nested` 타입은 객체 배열을 서로 독립적으로 쿼리해야 하는 특수한 경우에만 사용해야 합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "user": {
        "type": "nested",
        "properties": {
          "first": { "type": "text" },
          "last": { "type": "text" }
        }
      }
    }
  }
}
```

> 참고: 각 중첩 객체는 별도의 Lucene 문서로 인덱싱됩니다. 100개의 user 객체가 포함된 단일 문서를 인덱싱하면 101개의 Lucene 문서가 생성됩니다: 부모 문서 1개와 각 중첩 객체당 1개.

> 주의: 중첩 문서와 쿼리는 일반적으로 비용이 많이 드므로, 대규모 임의 키 집합의 경우 `flattened` 데이터 타입을 사용하는 것이 더 나은 옵션입니다.

#### 조인 (join)

동일 인덱스 내 문서에 대한 부모/자식 관계를 정의합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "my_id": { "type": "keyword" },
      "my_join_field": {
        "type": "join",
        "relations": {
          "question": "answer"
        }
      }
    }
  }
}
```

### 구조화된 데이터 타입

#### 범위 (Range)

범위 타입에는 다음이 포함됩니다:

| 타입 | 설명 |
|------|------|
| `integer_range` | 부호 있는 32비트 정수 범위 |
| `float_range` | 단정밀도 32비트 IEEE 754 부동 소수점 값 범위 |
| `long_range` | 부호 있는 64비트 정수 범위 |
| `double_range` | 배정밀도 64비트 IEEE 754 부동 소수점 값 범위 |
| `date_range` | 날짜 값 범위 |
| `ip_range` | IPv4 또는 IPv6 (또는 혼합) 주소를 지원하는 IP 값 범위 |

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "expected_attendees": { "type": "integer_range" },
      "time_frame": {
        "type": "date_range",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      }
    }
  }
}

PUT /my-index-000001/_doc/1
{
  "expected_attendees": {
    "gte": 10,
    "lt": 20
  },
  "time_frame": {
    "gte": "2015-10-31 12:00:00",
    "lte": "2015-11-01"
  }
}
```

#### IP

IPv4 및 IPv6 주소입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "ip_addr": { "type": "ip" }
    }
  }
}
```

#### 완성 (completion)

자동 완성 제안을 제공하기 위해 사용됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "suggest": { "type": "completion" }
    }
  }
}
```

#### 토큰 카운트 (token_count)

문자열의 토큰 수를 계산합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "fields": {
          "length": {
            "type": "token_count",
            "analyzer": "standard"
          }
        }
      }
    }
  }
}
```

### 집계 데이터 타입

#### 집계 메트릭 (aggregate_metric_double)

사전 집계된 메트릭 값입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "my-agg-metric-field": {
        "type": "aggregate_metric_double",
        "metrics": ["min", "max", "sum", "value_count"],
        "default_metric": "max"
      }
    }
  }
}
```

#### 히스토그램 (histogram)

히스토그램 형태의 사전 집계된 수치 값입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "my_histogram": { "type": "histogram" }
    }
  }
}
```

### 텍스트 검색 타입

#### 텍스트 (text)

전체 텍스트 검색을 위한 분석되고 비구조화된 텍스트입니다. `text` 필드는 정렬에 사용되지 않으며 집계에 거의 사용되지 않습니다. 텍스트 필드는 사람이 읽을 수 있는 비구조화된 콘텐츠에 가장 적합합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "full_name": { "type": "text" }
    }
  }
}
```

분석 프로세스를 통해 Elasticsearch는 각 전체 텍스트 필드 내에서 개별 단어를 검색할 수 있습니다.

#### 매치 온리 텍스트 (match_only_text)

점수 계산과 위치가 비활성화된 공간 최적화 `text` 변형입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "logs": { "type": "match_only_text" }
    }
  }
}
```

#### 주석 텍스트 (annotated_text)

특수 마크업이 포함된 텍스트입니다. 명명된 엔티티를 식별하는 데 사용됩니다.

#### 타입 어헤드 검색 (search_as_you_type)

타입 어헤드 완성을 위한 `text` 유사 타입입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "my_field": { "type": "search_as_you_type" }
    }
  }
}
```

### 문서 순위 타입

#### 밀집 벡터 (dense_vector)

부동 소수점 값의 밀집 벡터를 기록합니다. k-최근접 이웃(kNN) 검색에 주로 사용됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "my_vector": {
        "type": "dense_vector",
        "dims": 3
      }
    }
  }
}

PUT /my-index-000001/_doc/1
{
  "my_vector": [0.5, 10, 6]
}
```

> 참고: `dense_vector` 타입은 집계나 정렬을 지원하지 않습니다. 또한 밀집 벡터는 항상 단일 값입니다 - 하나의 `dense_vector` 필드에 여러 값을 저장할 수 없습니다.

Elasticsearch는 효율적인 kNN 검색을 지원하기 위해 HNSW 알고리즘을 사용합니다. 대부분의 kNN 알고리즘과 마찬가지로 HNSW는 향상된 속도를 위해 결과 정확도를 희생하는 근사 방법입니다.

#### 희소 벡터 (sparse_vector)

부동 소수점 값의 희소 벡터를 기록합니다. 대부분의 값이 0이고 몇 개의 용어만 상당한 가중치를 갖는 수치 표현에 사용됩니다. SPLADE 또는 ELSER와 같은 용어 기반 모델에서 일반적입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text_embedding": { "type": "sparse_vector" }
    }
  }
}
```

> 주의: `sparse_vector` 필드는 8.0에서 8.10 사이에 생성된 인덱스에 포함될 수 없습니다. 엄격하게 양수 값만 지원하며, 음수 값은 거부됩니다. 분석기, 쿼리, 정렬 또는 집계를 지원하지 않습니다.

#### 순위 특성 (rank_feature)

숫자 특성을 기록하여 쿼리 시 점수를 높입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "pagerank": { "type": "rank_feature" },
      "url_length": {
        "type": "rank_feature",
        "positive_score_impact": false
      }
    }
  }
}
```

#### 순위 특성들 (rank_features)

숫자 특성 벡터를 기록하여 쿼리 시 점수를 높입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "topics": { "type": "rank_features" }
    }
  }
}
```

#### 시맨틱 텍스트 (semantic_text)

`semantic_text` 필드 타입은 합리적인 기본값을 제공하여 시맨틱 검색을 단순화합니다. 이 타입을 사용하면 수동으로 매핑을 구성하거나, 수집 파이프라인을 설정하거나, 청킹을 처리할 필요가 없습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "inference_field": {
        "type": "semantic_text",
        "inference_id": "my-elser-endpoint"
      }
    }
  }
}
```

필드 타입은 자동으로:
- 인덱스 매핑을 구성합니다 (`sparse_vector` 또는 `dense_vector`와 같은 올바른 필드 타입 선택)
- 수집 파이프라인 없이 인덱싱 중에 임베딩을 생성합니다
- 인덱싱 중에 긴 텍스트 문서를 자동으로 청킹합니다

> 제한 사항:
> - `semantic_text` 필드는 현재 `nested` 필드의 요소로 지원되지 않습니다.
> - 동적 템플릿의 일부로 설정할 수 없습니다.
> - 8.11.0 이전에 생성된 인덱스에서는 지원되지 않습니다.

### 공간 데이터 타입

#### 지오 포인트 (geo_point)

위도/경도 포인트입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "location": { "type": "geo_point" }
    }
  }
}

PUT /my-index-000001/_doc/1
{
  "text": "Geopoint as an object using GeoJSON format",
  "location": {
    "type": "Point",
    "coordinates": [-71.34, 41.12]
  }
}

PUT /my-index-000001/_doc/2
{
  "text": "Geopoint as a WKT POINT primitive",
  "location": "POINT (-71.34 41.12)"
}

PUT /my-index-000001/_doc/3
{
  "text": "Geopoint as an object with 'lat' and 'lon' keys",
  "location": {
    "lat": 41.12,
    "lon": -71.34
  }
}

PUT /my-index-000001/_doc/4
{
  "text": "Geopoint as an array",
  "location": [-71.34, 41.12]
}

PUT /my-index-000001/_doc/5
{
  "text": "Geopoint as a string",
  "location": "41.12,-71.34"
}

PUT /my-index-000001/_doc/6
{
  "text": "Geopoint as a geohash",
  "location": "drm3btev3e86"
}
```

> 참고: Geopoint는 Well-Known Text POINT, lat과 lon 키가 있는 객체, `[lon, lat]` 형식의 배열, `"lat,lon"` 형식의 문자열 또는 geohash로 표현할 수 있습니다.

#### 지오 쉐이프 (geo_shape)

다각형과 같은 복잡한 도형입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "location": { "type": "geo_shape" }
    }
  }
}
```

#### 포인트 (point)

임의의 데카르트 포인트입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "location": { "type": "point" }
    }
  }
}
```

#### 쉐이프 (shape)

임의의 데카르트 기하학입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "geometry": { "type": "shape" }
    }
  }
}
```

### 기타 타입

#### 퍼콜레이터 (percolator)

인덱스 문서에 저장된 쿼리입니다. JSON 객체를 포함하는 모든 필드는 퍼콜레이터 필드로 구성할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "query": { "type": "percolator" }
    }
  }
}
```

> 팁: 리인덱스가 필요한 경우를 대비하여 인덱스에 별칭을 정의하는 것이 항상 권장됩니다. 이렇게 하면 시스템/애플리케이션이 퍼콜레이터 쿼리가 이제 다른 인덱스에 있음을 알 필요가 없습니다.

#### 버전 (version)

소프트웨어 버전입니다. 시맨틱 버전 관리 우선순위 규칙을 지원합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "version": { "type": "version" }
    }
  }
}
```

#### murmur3

인덱스 시점에 값의 해시를 계산하고 저장합니다.

### 배열

Elasticsearch에서 배열은 전용 필드 데이터 타입이 필요하지 않습니다. 기본적으로 모든 필드는 0개 이상의 값을 포함할 수 있지만, 배열의 모든 값은 동일한 필드 타입이어야 합니다.

```json
PUT /my-index-000001/_doc/1
{
  "message": "some arrays in this document...",
  "tags": ["elasticsearch", "wow"],
  "lists": [
    {
      "name": "prog_list",
      "description": "programming list"
    },
    {
      "name": "cool_list",
      "description": "cool stuff list"
    }
  ]
}
```

### 멀티 필드

하나의 필드를 다른 목적으로 다른 방식으로 인덱싱하는 것이 종종 유용합니다. 예를 들어, 문자열 필드를 전체 텍스트 검색을 위한 `text` 필드로, 정렬이나 집계를 위한 `keyword` 필드로 매핑할 수 있습니다. 또는 `text` 필드를 표준 분석기, 영어 분석기, 프랑스어 분석기로 인덱싱할 수 있습니다. 이것이 멀티 필드의 목적입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "city": {
        "type": "text",
        "fields": {
          "raw": {
            "type": "keyword"
          }
        }
      }
    }
  }
}
```

대부분의 필드 타입은 `fields` 파라미터를 통해 멀티 필드를 지원합니다.

---

## 메타데이터 필드

매핑 정의에는 `_source` 필드와 같이 문서의 관련 메타데이터가 어떻게 처리되는지 커스터마이징하는 메타데이터 필드도 포함됩니다.

### 문서 ID 메타데이터

#### _id

문서의 ID입니다. 각 문서에는 고유하게 식별하는 `_id`가 있습니다. 이 `_id`는 인덱싱되어 GET API 또는 `ids` 쿼리를 사용하여 문서를 조회할 수 있습니다.

#### _index

문서가 속한 인덱스입니다. 기본적으로 `_id`나 `_index`와 같은 문서 메타데이터 필드는 `*`와 같은 와일드카드 패턴으로 요청된 필드 옵션을 사용할 때 반환되지 않습니다. 그러나 필드 이름을 명시적으로 요청하면 `_id`, `_routing`, `_ignored`, `_index` 및 `_version` 메타데이터 필드를 검색할 수 있습니다.

### _source 필드

`_source` 필드는 인덱스 시점에 전달된 원본 JSON 문서 본문을 포함합니다. `_source` 필드 자체는 인덱싱되지 않으므로 검색할 수 없지만, get 또는 search와 같은 가져오기 요청을 실행할 때 반환될 수 있도록 저장됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "_source": {
      "enabled": true
    }
  }
}
```

#### synthetic _source

디스크 사용량이 중요한 경우, 검색 시점에 소스 콘텐츠를 재구성하는 synthetic `_source` 사용을 고려하세요. 이는 디스크 사용량을 줄이지만 Get 및 Search 쿼리에서 `_source` 접근 속도가 느려집니다.

#### _source 비활성화

`_source` 필드를 비활성화할 수 있지만, 이렇게 하면 다음 기능이 지원되지 않습니다:
- update, update_by_query, reindex API
- 하이라이팅
- 한 Elasticsearch 인덱스에서 다른 인덱스로 리인덱싱
- 원본 문서를 보며 매핑 또는 분석을 디버깅하는 기능
- 향후 매핑을 자동으로 복구하는 기능

### _routing 필드

`_routing` 필드는 문서를 특정 샤드로 라우팅하는 사용자 정의 라우팅 값입니다. 기본 `_routing` 값은 문서의 `_id`입니다.

```json
PUT /my-index-000001/_doc/1?routing=user1
{
  "title": "This is a document"
}

GET /my-index-000001/_doc/1?routing=user1
```

> 중요: 문서를 가져오거나, 삭제하거나, 업데이트할 때 동일한 라우팅 값을 제공해야 합니다.

### _meta 필드

매핑 타입에는 사용자 정의 메타 데이터가 연결될 수 있습니다. 이는 Elasticsearch에서 전혀 사용되지 않지만, 문서가 속한 클래스와 같은 애플리케이션별 메타데이터를 저장하는 데 사용할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "_meta": {
      "class": "MyApp::User",
      "version": {
        "min": "1.0",
        "max": "1.3"
      }
    }
  }
}
```

### _ignored 필드

`ignore_malformed`로 인해 인덱스 시점에 무시된 문서의 모든 필드입니다.

### _field_names 필드

null이 아닌 값을 포함하는 문서의 모든 필드입니다.

### _size 필드

`_source` 필드의 바이트 크기입니다. mapper-size 플러그인에서 제공됩니다.

---

## 매핑 파라미터

매핑 파라미터는 일부 또는 모든 필드 데이터 타입에 공통적입니다.

### analyzer

텍스트 필드의 분석기를 지정합니다. 인덱싱 시와 검색 시 모두 사용됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "standard"
      }
    }
  }
}
```

Elasticsearch는 다음 순서로 파라미터를 확인하여 인덱스 분석기를 결정합니다:
1. 필드의 `analyzer` 매핑 파라미터
2. `analysis.analyzer.default` 인덱스 설정
3. 위 파라미터가 지정되지 않으면 `standard` 분석기 사용

### search_analyzer

검색 시 사용할 분석기를 지정합니다. 기본적으로 `analyzer` 파라미터에 지정된 분석기를 사용합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "standard",
        "search_analyzer": "simple"
      }
    }
  }
}
```

### coerce

coercion은 더티 값을 필드의 데이터 타입에 맞게 정리하려고 시도합니다. 예를 들어:
- 문자열은 숫자로 강제 변환됩니다
- 부동 소수점은 정수 값으로 잘립니다

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "number_one": {
        "type": "integer"
      },
      "number_two": {
        "type": "integer",
        "coerce": false
      }
    }
  }
}

PUT /my-index-000001/_doc/1
{
  "number_one": "10"
}

PUT /my-index-000001/_doc/2
{
  "number_two": "10"
}
```

첫 번째 문서에서 `"10"`은 정수 `10`으로 강제 변환됩니다. 두 번째 문서는 강제 변환이 비활성화되어 있어 거부됩니다.

`coerce` 설정 값은 업데이트 매핑 API를 사용하여 기존 필드에서 업데이트할 수 있습니다.

### copy_to

`copy_to` 파라미터를 사용하면 여러 필드의 값을 그룹 필드로 복사한 다음 단일 필드로 쿼리할 수 있습니다. 여러 필드를 자주 검색하는 경우 `copy_to`를 사용하여 더 적은 필드를 검색하면 검색 속도를 향상시킬 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "first_name": {
        "type": "text",
        "copy_to": "full_name"
      },
      "last_name": {
        "type": "text",
        "copy_to": "full_name"
      },
      "full_name": {
        "type": "text"
      }
    }
  }
}

PUT /my-index-000001/_doc/1
{
  "first_name": "John",
  "last_name": "Smith"
}

GET /my-index-000001/_search
{
  "query": {
    "match": {
      "full_name": {
        "query": "John Smith",
        "operator": "and"
      }
    }
  }
}
```

> 중요 사항:
> - 복사되는 것은 필드 값이지, 분석 프로세스의 결과인 용어가 아닙니다
> - 원본 `_source` 필드는 복사된 값을 표시하도록 수정되지 않습니다
> - 동일한 값을 여러 필드에 복사할 수 있습니다: `"copy_to": ["field_1", "field_2"]`
> - 중간 필드를 사용하여 재귀적으로 복사할 수 없습니다

### doc_values

정렬, 집계 및 스크립트에서 필드 값에 대한 접근은 다른 데이터 접근 패턴이 필요합니다. 용어를 찾아 문서를 찾는 대신, 문서를 찾아 해당 필드에 있는 용어를 찾아야 합니다.

`doc_values` 필드는 문서 인덱스 시점에 빌드되는 온디스크 데이터 구조로, 이 데이터 접근 패턴을 가능하게 합니다. `_source`와 동일한 값을 저장하지만 정렬 및 집계에 더 효율적인 열 기반 형식으로 저장합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "status_code": {
        "type": "keyword"
      },
      "session_id": {
        "type": "keyword",
        "doc_values": false
      }
    }
  }
}
```

Doc values는 `text` 및 `annotated_text` 필드를 제외한 대부분의 필드 타입에서 지원됩니다. 기본적으로 필드에는 doc_values가 활성화되어 있습니다. doc_values가 비활성화된 필드는 여전히 쿼리할 수 있습니다.

### dynamic

`dynamic` 설정은 새 필드가 기존 객체에 동적으로 추가될 수 있는지 여부를 제어합니다. 이 설정은 문서 수준과 객체 수준 모두에서 비활성화할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic": false,
    "properties": {
      "user": {
        "dynamic": true,
        "properties": {}
      }
    }
  }
}
```

### eager_global_ordinals

글로벌 서수는 용어 집계의 성능을 최적화하는 데이터 구조입니다. 기본적으로 지연 로드되지만, 자주 집계되는 필드의 경우 `eager_global_ordinals`를 활성화할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "tags": {
        "type": "keyword",
        "eager_global_ordinals": true
      }
    }
  }
}
```

### enabled

`enabled` 설정은 특정 필드를 완전히 비활성화할 수 있습니다. 비활성화된 필드는 인덱싱되거나 검색되지 않지만 `_source`에는 저장됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "user_id": {
        "type": "keyword"
      },
      "last_updated": {
        "type": "date"
      },
      "session_data": {
        "type": "object",
        "enabled": false
      }
    }
  }
}
```

### fielddata

`fielddata`는 `text` 필드에서 정렬, 집계 또는 스크립팅을 수행하기 위해 필요한 인메모리 데이터 구조입니다. 기본적으로 비활성화되어 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "my_field": {
        "type": "text",
        "fielddata": true
      }
    }
  }
}
```

> 주의: `fielddata`는 상당한 메모리를 소비할 수 있습니다. 텍스트 필드에서 집계가 필요한 경우 멀티 필드를 사용하는 것이 더 좋습니다.

### fields

멀티 필드를 정의하는 데 사용됩니다. 동일한 필드를 다른 목적으로 다른 방식으로 인덱싱할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "city": {
        "type": "text",
        "fields": {
          "raw": {
            "type": "keyword"
          }
        }
      }
    }
  }
}
```

### format

날짜 필드의 날짜 형식을 지정합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "date": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      }
    }
  }
}
```

### ignore_above

`ignore_above`보다 긴 문자열은 인덱싱되거나 저장되지 않습니다. 문자열 배열의 경우 각 배열 요소에 대해 별도로 `ignore_above`가 적용됩니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "message": {
        "type": "keyword",
        "ignore_above": 256
      }
    }
  }
}
```

### ignore_malformed

잘못된 형식의 데이터를 무시할지 여부를 지정합니다. 기본적으로 잘못된 데이터 타입의 값을 인덱싱하려고 하면 예외가 발생합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "number_one": {
        "type": "integer",
        "ignore_malformed": true
      },
      "number_two": {
        "type": "integer"
      }
    }
  }
}

PUT /my-index-000001/_doc/1
{
  "text": "Some text value",
  "number_one": "foo"
}
```

### index

필드가 검색 가능해야 하는지 여부를 지정합니다. `index`가 `false`로 설정된 필드는 검색할 수 없지만 집계나 스크립팅에는 여전히 사용할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "index": true
      },
      "age": {
        "type": "integer",
        "index": false
      }
    }
  }
}
```

대부분의 필드는 기본적으로 인덱싱되어 검색 가능합니다. 역색인을 통해 쿼리는 고유하게 정렬된 용어 목록에서 검색 용어를 찾고, 그로부터 해당 용어를 포함하는 문서 목록에 즉시 접근할 수 있습니다.

### index_options

인덱스에 저장될 정보를 지정합니다. `text` 필드에만 적용됩니다.

| 값 | 설명 |
|----|------|
| `docs` | 문서 번호만 인덱싱 |
| `freqs` | 문서 번호와 용어 빈도 인덱싱 |
| `positions` | 문서 번호, 용어 빈도, 용어 위치 인덱싱 (기본값) |
| `offsets` | 문서 번호, 용어 빈도, 위치, 시작 및 끝 문자 오프셋 인덱싱 |

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "index_options": "offsets"
      }
    }
  }
}
```

### index_phrases

2-단어 조합(shingles)이 별도의 필드에 인덱싱되어야 하는지 여부입니다. 이를 통해 구문 쿼리가 더 효율적으로 실행될 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "index_phrases": true
      }
    }
  }
}
```

### index_prefixes

용어 접두사를 인덱싱하여 접두사 검색을 가속화합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "index_prefixes": {
          "min_chars": 1,
          "max_chars": 10
        }
      }
    }
  }
}
```

### meta

필드에 대한 메타데이터를 저장합니다. 이 정보는 Elasticsearch에서 사용되지 않지만 애플리케이션에서 필드에 대한 추가 정보를 저장하는 데 사용할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "latency": {
        "type": "long",
        "meta": {
          "unit": "ms"
        }
      }
    }
  }
}
```

### normalizer

`keyword` 필드에 대한 정규화기를 지정합니다. 분석기와 유사하지만 토큰화 없이 단일 토큰만 생성합니다.

```json
PUT /my-index-000001
{
  "settings": {
    "analysis": {
      "normalizer": {
        "my_normalizer": {
          "type": "custom",
          "char_filter": [],
          "filter": ["lowercase", "asciifolding"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "foo": {
        "type": "keyword",
        "normalizer": "my_normalizer"
      }
    }
  }
}
```

### norms

점수 계산에 사용되는 정규화 팩터입니다. 기본적으로 `text` 필드에 활성화되어 있습니다. 필터링이나 집계에만 사용되는 필드의 경우 비활성화할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "norms": false
      }
    }
  }
}
```

### null_value

null 값은 인덱싱되거나 검색할 수 없습니다. `null_value` 파라미터를 사용하면 명시적 null 값을 지정된 값으로 대체할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "status_code": {
        "type": "keyword",
        "null_value": "NULL"
      }
    }
  }
}
```

### position_increment_gap

분석된 텍스트 필드에서 구문 및 구문 접두사 쿼리를 위한 위치 증가 간격입니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "names": {
        "type": "text",
        "position_increment_gap": 100
      }
    }
  }
}
```

### properties

객체 필드 또는 중첩 필드 내의 하위 필드를 정의합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "manager": {
        "properties": {
          "age": { "type": "integer" },
          "name": { "type": "text" }
        }
      }
    }
  }
}
```

### similarity

사용할 유사도 알고리즘을 지정합니다. 기본값은 `BM25`입니다.

| 값 | 설명 |
|----|------|
| `BM25` | Elasticsearch와 Lucene의 기본 유사도 알고리즘 |
| `boolean` | 전체 텍스트 순위가 필요하지 않고 쿼리 용어가 일치하는지 여부만 중요한 경우 사용 |

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "default_field": {
        "type": "text"
      },
      "boolean_sim_field": {
        "type": "text",
        "similarity": "boolean"
      }
    }
  }
}
```

### store

기본적으로 필드 값은 인덱싱되어 검색 가능하지만 저장되지 않습니다. 즉, 필드를 쿼리할 수 있지만 원래 필드 값은 검색할 수 없습니다. `store`를 `true`로 설정하면 필드 값을 별도로 저장할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "store": true
      }
    }
  }
}
```

### subobjects

객체 내에서 하위 객체를 허용할지 여부를 지정합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "metrics": {
        "type": "object",
        "subobjects": false
      }
    }
  }
}
```

### term_vector

용어 벡터를 저장할지 여부와 어떤 정보를 저장할지 지정합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "term_vector": "with_positions_offsets"
      }
    }
  }
}
```

---

## 동적 템플릿

동적 템플릿을 사용하면 기본 동적 필드 매핑 규칙 이상으로 Elasticsearch가 데이터를 매핑하는 방법을 더 세밀하게 제어할 수 있습니다. `dynamic` 파라미터를 `true` 또는 `runtime`으로 설정하여 동적 매핑을 활성화합니다.

동적 템플릿은 다음과 같은 조건으로 필드를 매칭합니다:

- `match_mapping_type`과 `unmatch_mapping_type`은 Elasticsearch가 감지하는 데이터 타입에서 작동합니다
- `match`와 `unmatch`는 필드 이름에서 패턴을 사용하여 매칭합니다
- `path_match`와 `path_unmatch`는 필드의 전체 점 구분 경로에서 작동합니다

동적 템플릿이 `match_mapping_type`, `match` 또는 `path_match`를 정의하지 않으면 어떤 필드도 매칭하지 않습니다.

### 기본 동적 템플릿 예제

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "strings_as_keywords": {
          "match_mapping_type": "string",
          "mapping": {
            "type": "keyword"
          }
        }
      }
    ]
  }
}
```

### match_mapping_type과 unmatch_mapping_type

`match_mapping_type` 파라미터는 JSON 파서가 감지한 데이터 타입으로 필드를 매칭합니다. `unmatch_mapping_type`은 데이터 타입에 따라 필드를 제외합니다.

JSON은 long과 integer, double과 float을 구분하지 않으므로, 모든 파싱된 부동 소수점 숫자는 `double` JSON 데이터 타입으로 간주되고, 모든 파싱된 정수 숫자는 `long`으로 간주됩니다.

단일 데이터 타입 또는 데이터 타입 목록을 지정할 수 있습니다. `match_mapping_type` 파라미터에 와일드카드(`*`)를 사용하여 모든 데이터 타입을 매칭할 수도 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "integers": {
          "match_mapping_type": "long",
          "mapping": {
            "type": "integer"
          }
        }
      },
      {
        "strings": {
          "match_mapping_type": "string",
          "mapping": {
            "type": "text",
            "fields": {
              "raw": {
                "type": "keyword",
                "ignore_above": 256
              }
            }
          }
        }
      }
    ]
  }
}
```

### match와 unmatch

`match`와 `unmatch` 파라미터는 필드 이름에 패턴을 사용하여 매칭합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "longs_as_strings": {
          "match_mapping_type": "string",
          "match": "long_*",
          "unmatch": "*_text",
          "mapping": {
            "type": "long"
          }
        }
      }
    ]
  }
}
```

### match_pattern

`match_pattern` 파라미터를 사용하면 `match` 파라미터에 정규식을 사용할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "longs_as_strings": {
          "match_mapping_type": "string",
          "match_pattern": "regex",
          "match": "^profit_\\d+$",
          "mapping": {
            "type": "long"
          }
        }
      }
    ]
  }
}
```

### path_match와 path_unmatch

`path_match`와 `path_unmatch` 파라미터는 `match` 및 `unmatch`와 동일한 방식으로 작동하지만, 최종 이름뿐만 아니라 필드의 전체 점 구분 경로에서 작동합니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "full_name": {
          "path_match": "name.*",
          "path_unmatch": "*.middle",
          "mapping": {
            "type": "text",
            "copy_to": "full_name"
          }
        }
      }
    ]
  }
}

PUT /my-index-000001/_doc/1
{
  "name": {
    "first": "John",
    "middle": "Winston",
    "last": "Lennon"
  }
}
```

### 템플릿 변수

매핑 사양에서 `{name}` 및 `{dynamic_type}` 템플릿 변수를 플레이스홀더로 사용할 수 있습니다.

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "named_analyzers": {
          "match_mapping_type": "string",
          "match": "*",
          "mapping": {
            "type": "text",
            "analyzer": "{name}"
          }
        }
      },
      {
        "no_doc_values": {
          "match_mapping_type": "*",
          "mapping": {
            "type": "{dynamic_type}",
            "doc_values": false
          }
        }
      }
    ]
  }
}
```

### 런타임 필드로 매핑

문자열 필드를 `keyword` 필드로 런타임 섹션에 매핑하는 동적 템플릿을 만들 수 있습니다:

```json
PUT /my-index-000001
{
  "mappings": {
    "dynamic_templates": [
      {
        "strings_as_keywords": {
          "match_mapping_type": "string",
          "runtime": {}
        }
      }
    ]
  }
}
```

### 동적 필드 매핑 참고 사항

동적 필드 매핑은 필드에 구체적인 값이 포함된 경우에만 추가됩니다. Elasticsearch는 필드에 `null`이나 빈 배열이 포함된 경우 동적 필드 매핑을 추가하지 않습니다. `dynamic_template`에 `null_value` 옵션이 사용되면, 해당 필드에 대한 구체적인 값이 있는 첫 번째 문서가 인덱싱된 후에만 적용됩니다.

---

## 매핑 제한 설정

인덱스의 필드 수나 깊이 등을 제한하기 위한 설정들이 있습니다. 이러한 제한은 매핑과 검색이 너무 커지는 것을 방지하기 위해 존재합니다.

### index.mapping.total_fields.limit

인덱스의 최대 필드 수입니다. 필드 및 객체 매핑과 필드 별칭이 이 제한에 포함됩니다. 매핑된 런타임 필드도 이 제한에 포함됩니다. 기본값은 1000 입니다.

이 제한은 매핑과 검색이 너무 커지는 것을 방지하기 위해 존재합니다. 높은 값은 성능 저하와 메모리 문제를 일으킬 수 있으며, 특히 부하가 높거나 리소스가 적은 클러스터에서 그렇습니다. 이 설정을 늘리면 `indices.query.bool.max_clause_count` 설정도 함께 늘리는 것이 좋습니다. 이 설정은 쿼리의 최대 절 수를 제한합니다.

#### 제한에 포함되는 항목

`index.mapping.total_fields.limit` 설정은 리프 필드뿐만 아니라 모든 매퍼를 계산합니다:
- 객체 매퍼 (점 구분 필드 경로의 각 레벨이 별도로 계산됨)
- 필드 매퍼 (`keyword`, `text`, `long` 등의 리프 필드)
- 멀티 필드 (`.keyword`, `.raw`와 같은 각 하위 필드가 개별적으로 계산됨)

메타데이터 필드(`_id`, `_source`, `_routing` 등)는 이 제한에 포함되지 않습니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.mapping.total_fields.limit": 2000
  }
}
```

### index.mapping.total_fields.ignore_dynamic_beyond_limit

동적으로 매핑된 필드가 총 필드 제한을 초과할 때 발생하는 동작을 결정합니다.

- `false` (기본값): 동적 필드를 매핑에 추가하려는 문서의 인덱스 요청이 "인덱스의 총 필드 제한 [X]을 초과했습니다" 메시지와 함께 실패합니다.
- `true`: 인덱스 요청이 실패하지 않습니다. 대신 제한을 초과하는 필드는 `dynamic: false`와 유사하게 매핑에 추가되지 않습니다. 매핑에 추가되지 않은 필드는 `_ignored` 필드에 추가됩니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.mapping.total_fields.ignore_dynamic_beyond_limit": true
  }
}
```

### index.mapping.depth.limit

필드의 최대 깊이이며, 내부 객체 수로 측정됩니다. 예를 들어, 모든 필드가 루트 객체 수준에 정의되면 깊이는 1입니다. 객체 매핑이 하나 있으면 깊이는 2입니다. 기본값은 20 입니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.mapping.depth.limit": 5
  }
}
```

### index.mapping.nested_fields.limit

잘못 설계된 매핑으로부터 보호하기 위해 인덱스당 고유 중첩 타입의 수를 제한합니다. 기본값은 50 입니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.mapping.nested_fields.limit": 100
  }
}
```

### index.mapping.nested_objects.limit

단일 문서가 모든 중첩 타입에 걸쳐 포함할 수 있는 중첩 JSON 객체의 최대 수입니다. 이 제한은 문서에 너무 많은 중첩 객체가 포함될 때 메모리 부족 오류를 방지하는 데 도움이 됩니다. 기본값은 10000 입니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.mapping.nested_objects.limit": 5000
  }
}
```

### index.mapping.nested_parents.limit

다른 중첩 필드의 부모 역할을 하는 중첩 필드의 최대 수입니다. 각 중첩 부모는 자체 인메모리 부모 비트셋이 필요합니다. 루트 레벨 중첩 필드는 부모 비트셋을 공유하지만, 다른 중첩 필드 아래의 중첩 필드는 추가 비트셋이 필요합니다. 이 설정은 과도한 메모리 사용을 방지하기 위해 고유 중첩 부모 수를 제한합니다. 기본값은 50 입니다.

### index.mapping.field_name_length.limit

필드 이름의 최대 길이 설정입니다.

```json
PUT /my-index-000001
{
  "settings": {
    "index.mapping.field_name_length.limit": 100
  }
}
```

### index.mapping.dimension_fields.limit

시계열 데이터 스트림의 차원 필드 최대 수입니다.

### 매핑 폭발 방지

인덱스에 너무 많은 필드를 정의하면 매핑 폭발이 발생하여 메모리 부족 오류와 복구하기 어려운 상황이 발생할 수 있습니다.

동적 매핑과 같이 삽입되는 모든 새 문서가 새 필드를 도입하는 상황을 고려하세요. 각 새 필드가 인덱스 매핑에 추가되며, 매핑이 커지면 문제가 될 수 있습니다.

매핑 제한 설정을 사용하여 필드 매핑(수동 또는 동적으로 생성된) 수를 제한하고 매핑 폭발을 일으키는 문서를 방지하세요.

---

## 매핑 관리

### 기존 필드 매핑 변경

지원되는 매핑 파라미터를 제외하고, 기존 필드의 매핑이나 필드 타입을 변경할 수 없습니다. 기존 필드를 변경하면 이미 인덱싱된 데이터가 무효화될 수 있습니다.

필드의 매핑을 변경해야 하는 경우:
1. 올바른 매핑으로 새 인덱스를 생성합니다
2. 데이터를 새 인덱스로 리인덱싱합니다

### 리인덱싱 없이 스키마 변경

런타임 필드를 사용하면 리인덱싱 없이 스키마를 변경할 수 있습니다. 인덱싱된 필드와 함께 런타임 필드를 사용하여 리소스 사용량과 성능의 균형을 맞출 수 있습니다. 인덱스는 작아지지만 검색 성능은 느려집니다.

### 다운타임 없이 매핑 변경

인덱스에 별칭을 정의하면 리인덱스가 필요할 때 시스템/애플리케이션이 쿼리가 이제 다른 인덱스에 있음을 알 필요 없이 백그라운드에서 모든 데이터를 리인덱싱할 수 있습니다.

### 필드 이름 변경

필드 별칭을 사용하여 기존 필드의 대체 이름을 만들 수 있습니다:

```json
PUT /my-index-000001/_mapping
{
  "properties": {
    "user_identifier": {
      "type": "alias",
      "path": "user_id"
    }
  }
}
```

### 멀티 필드 추가

기존 필드에 멀티 필드를 활성화할 수 있습니다:

```json
PUT /my-index-000001/_mapping
{
  "properties": {
    "city": {
      "type": "text",
      "fields": {
        "raw": {
          "type": "keyword"
        }
      }
    }
  }
}
```

> 참고: 매핑 업데이트 전에 인덱싱된 문서는 업데이트되거나 리인덱싱될 때까지 새 멀티 필드에 대한 값을 갖지 않습니다. 매핑 변경 후 인덱싱된 문서는 자동으로 새 멀티 필드에 대한 값을 갖습니다.

---

## 요약

Elasticsearch 매핑은 데이터가 인덱싱되고 검색되는 방식을 정의하는 핵심 개념입니다. 주요 포인트는 다음과 같습니다:

1. 동적 매핑 은 시작하기 쉽지만, 프로덕션에서는 명시적 매핑 을 권장합니다
2. 다양한 필드 데이터 타입 이 있으며, 사용 사례에 맞는 타입을 선택해야 합니다
3. 런타임 필드 를 사용하면 리인덱싱 없이 유연하게 필드를 추가할 수 있습니다
4. 동적 템플릿 을 사용하여 새 필드가 매핑되는 방식을 제어할 수 있습니다
5. 매핑 제한 설정 을 통해 매핑 폭발을 방지해야 합니다
6. 기존 필드의 타입은 변경할 수 없으며, 변경하려면 새 인덱스를 생성하고 리인덱싱해야 합니다

---

## 참고 자료

- [Elasticsearch Mapping 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html)
- [Dynamic Mapping](https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-mapping.html)
- [Explicit Mapping](https://www.elastic.co/guide/en/elasticsearch/reference/current/explicit-mapping.html)
- [Field Data Types](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html)
- [Mapping Parameters](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-params.html)
- [Dynamic Templates](https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html)
- [Runtime Fields](https://www.elastic.co/docs/manage-data/data-store/mapping/runtime-fields)
- [Mapping Limit Settings](https://www.elastic.co/docs/reference/elasticsearch/index-settings/mapping-limit)
