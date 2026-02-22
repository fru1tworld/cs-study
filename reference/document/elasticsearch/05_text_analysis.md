# Elasticsearch 텍스트 분석 (Text Analysis)

> 이 문서는 Elasticsearch 9.x 공식 문서의 Text Analysis 섹션을 한국어로 번역한 것입니다.

## 목차

1. [개요](#개요)
2. [텍스트 분석의 개념](#텍스트-분석의-개념)
3. [분석기의 구조](#분석기의-구조)
4. [인덱스 및 검색 분석](#인덱스-및-검색-분석)
5. [분석기 테스트](#분석기-테스트)
6. [분석기 지정](#분석기-지정)
7. [내장 분석기](#내장-분석기)
8. [분석기 구성](#분석기-구성)
9. [사용자 정의 분석기 생성](#사용자-정의-분석기-생성)
10. [토크나이저 레퍼런스](#토크나이저-레퍼런스)
11. [토큰 필터 레퍼런스](#토큰-필터-레퍼런스)
12. [캐릭터 필터 레퍼런스](#캐릭터-필터-레퍼런스)
13. [노말라이저](#노말라이저)

---

## 개요

텍스트 분석(Text Analysis)은 Elasticsearch가 전문 검색(full-text search)을 수행할 수 있게 해주는 핵심 기능입니다. 전문 검색에서는 정확히 일치하는 결과만 반환하는 것이 아니라 관련된 모든 결과를 반환합니다.

### 텍스트 분석이란?

텍스트 분석은 토큰화(tokenization) 를 통해 전문 검색을 가능하게 합니다. 토큰화란 텍스트를 토큰(token) 이라고 불리는 작은 조각으로 나누는 과정입니다. 대부분의 경우 이 토큰들은 개별 단어입니다.

예를 들어, "The quick brown fox jumps"라는 문장은 다음과 같은 토큰으로 분리됩니다:
- `the`
- `quick`
- `brown`
- `fox`
- `jumps`

### 텍스트 분석의 수행 시점

Elasticsearch는 다음 두 가지 시점에 텍스트 분석을 수행합니다:

1. 인덱싱 시(Index time): `text` 필드가 인덱싱될 때
2. 검색 시(Search time): `text` 필드에서 전문 검색을 수행할 때 (쿼리 문자열 분석)

### 기본 분석기

기본적으로 Elasticsearch는 모든 텍스트 분석에 standard 분석기 를 사용합니다. standard 분석기는 대부분의 자연어와 사용 사례에 대해 즉시 사용 가능한 지원을 제공합니다.

standard 분석기가 요구사항에 맞지 않는 경우, Elasticsearch의 다른 내장 분석기를 검토하고 테스트해볼 수 있습니다. 내장 분석기는 별도의 구성 없이 사용할 수 있지만, 일부는 동작을 조정하는 데 사용할 수 있는 옵션을 지원합니다.

### Elasticsearch 9.0의 변경사항

Elasticsearch 9.0은 Lucene 10.1.0을 기반으로 구축되어 상당한 성능 향상과 리소스 최적화를 가져왔습니다. 텍스트 분석과 관련된 주요 변경사항은 다음과 같습니다:

- Snowball 스테머 업그레이드: Lucene 10은 Snowball 스테머를 업그레이드했습니다. Snowball 스테머를 사용하는 사용자가 기존 데이터에서 검색 동작의 변화를 경험하는 경우 재인덱싱을 권장합니다. 업그레이드는 일반적으로 향상된 스테밍 결과를 제공합니다.

- german2 별칭 변경: "german2"는 이제 "german"의 더 이상 사용되지 않는(deprecated) 별칭입니다. 이로 인해 움라우트 치환이 있는 용어에 대해 약간 다른 토큰이 생성될 수 있습니다.

- Persian 분석기 스테밍 단계 추가: Lucene 10은 PersianAnalyzer에 최종 스테밍 단계를 추가했으며, Elasticsearch는 이를 `persian` 분석기로 노출합니다. 기존 인덱스는 이전의 비스테밍 동작을 유지하고, 새 인덱스는 추가된 스테밍이 포함된 업데이트된 동작을 볼 수 있습니다.

---

## 텍스트 분석의 개념

텍스트 분석은 비정형 텍스트를 검색에 최적화된 형식으로 변환합니다. 이 과정을 통해 Elasticsearch는 역색인(inverted index)을 구축하고, 이를 통해 빠른 전문 검색이 가능해집니다.

### 역색인(Inverted Index)

역색인은 텍스트 검색의 핵심 데이터 구조입니다. 문서들을 분석하여 각 고유한 용어(term)가 어떤 문서에 나타나는지를 기록합니다.

예를 들어, 두 문서가 있다고 가정합니다:
1. "The quick brown fox"
2. "The quick brown dog"

역색인은 다음과 같이 구축됩니다:

| 용어 | 문서 |
|------|------|
| brown | 1, 2 |
| dog | 2 |
| fox | 1 |
| quick | 1, 2 |
| the | 1, 2 |

### 분석기 파이프라인

문서가 분석기를 통과하는 과정:

1. 원본 문서의 텍스트가 캐릭터 필터 를 통과
2. 토크나이저 가 텍스트를 개별 토큰으로 분리
3. 토큰 필터 가 토큰을 처리 (소문자 변환, 불용어 제거 등)
4. 처리된 토큰(용어)이 역색인에 저장

---

## 분석기의 구조

분석기(analyzer) 는 내장이든 사용자 정의든 세 가지 하위 수준 구성 요소를 포함하는 패키지입니다:

1. 캐릭터 필터(Character filters)
2. 토크나이저(Tokenizer)
3. 토큰 필터(Token filters)

내장 분석기는 이러한 구성 요소들을 다양한 언어와 텍스트 유형에 적합한 분석기로 미리 패키징합니다. Elasticsearch는 개별 구성 요소도 노출하여 새로운 사용자 정의 분석기를 정의하는 데 조합할 수 있습니다.

### 1. 캐릭터 필터 (Character Filters)

캐릭터 필터는 원본 텍스트를 문자 스트림으로 받아 문자를 추가, 제거 또는 변경하여 스트림을 변환할 수 있습니다.

예를 들어, 캐릭터 필터는 다음과 같은 작업에 사용될 수 있습니다:
- 힌두-아라비아 숫자(٠‎١٢٣٤٥٦٧٨‎٩‎)를 아라비아-라틴 숫자(0123456789)로 변환
- `<b>`와 같은 HTML 요소를 스트림에서 제거

분석기는 0개 이상의 캐릭터 필터를 가질 수 있으며, 순서대로 적용됩니다.

Elasticsearch는 다음 세 가지 캐릭터 필터를 기본 제공합니다:
- `html_strip`
- `mapping`
- `pattern_replace`

### 2. 토크나이저 (Tokenizer)

토크나이저는 문자 스트림을 받아 개별 토큰(보통 개별 단어)으로 나누고 토큰 스트림을 출력합니다.

분석기는 정확히 하나의 토크나이저를 가져야 합니다.

토큰은 검색에 사용되는 텍스트 단위입니다. 토크나이저는 연속적인 텍스트 스트림을 받아 토큰으로 나눕니다. 토크나이저는 또한 다음 정보를 추적합니다:
- 텍스트에서 각 용어의 순서와 위치
- 각 용어의 시작 및 끝 문자 오프셋
- 토큰 유형

예를 들어, `whitespace` 토크나이저는 공백 문자를 만날 때마다 텍스트를 나눕니다. "Quick brown fox!"라는 텍스트는 `[Quick, brown, fox!]`로 변환됩니다.

### 3. 토큰 필터 (Token Filters)

토큰 필터는 토큰 스트림을 받아 토큰을 추가, 제거 또는 변경할 수 있습니다.

예시:
- `lowercase` 토큰 필터: 모든 토큰을 소문자로 변환
- `stop` 토큰 필터: "the"와 같은 일반적인 단어(불용어)를 토큰 스트림에서 제거
- `synonym` 토큰 필터: 토큰 스트림에 동의어를 추가

토큰 필터는 각 토큰의 위치나 문자 오프셋을 변경할 수 없습니다.

분석기는 0개 이상의 토큰 필터를 가질 수 있으며, 순서대로 적용됩니다.

### 분석기 구조 예시

다음은 `standard` 분석기의 구조입니다:

```
텍스트 입력
    │
    ▼
[캐릭터 필터] - 없음
    │
    ▼
[토크나이저] - standard
    │
    ▼
[토큰 필터] - lowercase, stop (stop은 기본적으로 비활성화)
    │
    ▼
역색인에 저장
```

---

## 인덱스 및 검색 분석

텍스트 분석은 두 가지 시점에 발생합니다:

### 인덱스 시간 분석 (Index Time Analysis)

문서가 인덱싱될 때 모든 `text` 필드 값이 분석됩니다.

```json
PUT my-index-000001/_doc/1
{
  "title": "The QUICK Brown Fox"
}
```

위 문서의 `title` 필드는 분석되어 다음과 같은 토큰이 역색인에 저장됩니다:
- `the`
- `quick`
- `brown`
- `fox`

### 검색 시간 분석 (Search Time Analysis)

전문 검색을 수행할 때 쿼리 문자열도 분석됩니다.

```json
GET my-index-000001/_search
{
  "query": {
    "match": {
      "title": "Quick FOX"
    }
  }
}
```

위 검색에서 "Quick FOX"는 분석되어 `quick`과 `fox` 토큰이 됩니다. 이 토큰들은 역색인의 토큰과 일치하므로 문서가 반환됩니다.

### 인덱스와 검색에 다른 분석기 사용

드문 경우이지만, 인덱스 시간과 검색 시간에 다른 분석기를 사용하는 것이 합리적일 수 있습니다. 이를 위해 Elasticsearch에서는 별도의 검색 분석기를 지정할 수 있습니다.

일반적으로, 필드 값과 쿼리 문자열에 동일한 형태의 토큰을 사용하면 예상치 못하거나 관련 없는 검색 결과가 발생할 때만 별도의 검색 분석기를 지정해야 합니다.

사용 사례:
- 자동완성을 위해 `edge_ngram` 토크나이저 사용 시
- 검색 시간 동의어 사용 시

---

## 분석기 테스트

`analyze` API는 분석기가 생성하는 용어를 보는 데 매우 유용한 도구입니다. 이 API는 텍스트 문자열에 대해 분석을 수행하고 결과 토큰을 반환합니다.

### 내장 분석기로 테스트

인덱스를 지정하지 않고 텍스트 문자열에 내장 분석기를 적용할 수 있습니다:

```json
POST _analyze
{
  "analyzer": "standard",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

응답:

```json
{
  "tokens": [
    { "token": "the", "start_offset": 0, "end_offset": 3, "type": "<ALPHANUM>", "position": 0 },
    { "token": "2", "start_offset": 4, "end_offset": 5, "type": "<NUM>", "position": 1 },
    { "token": "quick", "start_offset": 6, "end_offset": 11, "type": "<ALPHANUM>", "position": 2 },
    { "token": "brown", "start_offset": 12, "end_offset": 17, "type": "<ALPHANUM>", "position": 3 },
    { "token": "foxes", "start_offset": 18, "end_offset": 23, "type": "<ALPHANUM>", "position": 4 },
    { "token": "jumped", "start_offset": 24, "end_offset": 30, "type": "<ALPHANUM>", "position": 5 },
    { "token": "over", "start_offset": 31, "end_offset": 35, "type": "<ALPHANUM>", "position": 6 },
    { "token": "the", "start_offset": 36, "end_offset": 39, "type": "<ALPHANUM>", "position": 7 },
    { "token": "lazy", "start_offset": 40, "end_offset": 44, "type": "<ALPHANUM>", "position": 8 },
    { "token": "dog's", "start_offset": 45, "end_offset": 50, "type": "<ALPHANUM>", "position": 9 },
    { "token": "bone", "start_offset": 51, "end_offset": 55, "type": "<ALPHANUM>", "position": 10 }
  ]
}
```

### 토크나이저와 필터 조합으로 테스트

토크나이저, 토큰 필터, 캐릭터 필터를 조합하여 임시 사용자 정의 분석기를 테스트할 수 있습니다:

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "lowercase", "asciifolding" ],
  "text": "Is this déjà vu?"
}
```

응답:

```json
{
  "tokens": [
    { "token": "is", "start_offset": 0, "end_offset": 2, "type": "<ALPHANUM>", "position": 0 },
    { "token": "this", "start_offset": 3, "end_offset": 7, "type": "<ALPHANUM>", "position": 1 },
    { "token": "deja", "start_offset": 8, "end_offset": 12, "type": "<ALPHANUM>", "position": 2 },
    { "token": "vu", "start_offset": 13, "end_offset": 15, "type": "<ALPHANUM>", "position": 3 }
  ]
}
```

### 캐릭터 필터와 함께 테스트

```json
POST _analyze
{
  "tokenizer": "keyword",
  "char_filter": [ "html_strip" ],
  "text": "<p>I&apos;m so <b>happy</b>!</p>"
}
```

응답:

```json
{
  "tokens": [
    { "token": "\nI'm so happy!\n", "start_offset": 0, "end_offset": 32, "type": "word", "position": 0 }
  ]
}
```

### 특정 인덱스의 사용자 정의 분석기 테스트

사용자 정의 분석기를 참조하려면 `analyze` API에서 인덱스 이름을 지정해야 합니다:

```json
GET my-index-000001/_analyze
{
  "analyzer": "my_custom_analyzer",
  "text": "Is this déjà vu?"
}
```

### 필드의 분석기로 테스트

필드가 사용하는 분석기로 텍스트를 분석할 수도 있습니다:

```json
GET my-index-000001/_analyze
{
  "field": "title",
  "text": "Is this déjà vu?"
}
```

### 상세 분석 결과 보기

`explain` 매개변수를 `true`로 설정하면 토큰 속성과 추가 세부 정보가 포함된 응답을 받을 수 있습니다:

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "snowball" ],
  "text": "detailed output",
  "explain": true
}
```

### 토큰 수 제한

과도한 양의 토큰을 생성하면 노드의 메모리가 부족해질 수 있습니다. `index.analyze.max_token_count` 설정을 사용하여 생성할 수 있는 토큰 수를 제한할 수 있습니다. 이 제한을 초과하는 토큰이 생성되면 오류가 발생합니다.

---

## 분석기 지정

Elasticsearch는 다양한 방법으로 분석기를 지정할 수 있습니다.

### Elasticsearch가 인덱스 분석기를 결정하는 방법

Elasticsearch는 다음 매개변수를 순서대로 확인하여 사용할 인덱스 분석기를 결정합니다:

1. 필드의 `analyzer` 매핑 매개변수
2. `analysis.analyzer.default` 인덱스 설정
3. 위 매개변수가 지정되지 않은 경우, `standard` 분석기 사용

### Elasticsearch가 검색 분석기를 결정하는 방법

검색 시 Elasticsearch는 다음 매개변수를 순서대로 확인합니다:

1. 검색 쿼리의 `analyzer` 매개변수
2. 필드의 `search_analyzer` 매핑 매개변수
3. `analysis.analyzer.default_search` 인덱스 설정
4. 필드의 `analyzer` 매핑 매개변수
5. 위 매개변수가 지정되지 않은 경우, `standard` 분석기 사용

### 필드에 분석기 지정하기

인덱스를 매핑할 때 `analyzer` 매핑 매개변수를 사용하여 각 `text` 필드에 분석기를 지정할 수 있습니다:

```json
PUT my-index-000001
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "whitespace"
      }
    }
  }
}
```

### 인덱스의 기본 분석기 지정하기

분석기를 지정하지 않은 모든 `text` 필드에 적용될 기본 분석기를 설정할 수 있습니다:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "default": {
          "type": "simple"
        }
      }
    }
  }
}
```

### 검색 분석기 지정하기

대부분의 경우 별도의 검색 분석기를 지정할 필요가 없습니다. 하지만 필요한 경우 `search_analyzer` 매개변수를 사용할 수 있습니다:

```json
PUT my-index-000001
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "whitespace",
        "search_analyzer": "simple"
      }
    }
  }
}
```

### 구문 검색용 분석기 지정하기

`search_quote_analyzer` 설정을 사용하면 구문에 대한 분석기를 지정할 수 있습니다. 이는 구문 쿼리에서 불용어를 비활성화할 때 특히 유용합니다:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [ "lowercase" ]
        },
        "my_stop_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [ "lowercase", "english_stop" ]
        }
      },
      "filter": {
        "english_stop": {
          "type": "stop",
          "stopwords": "_english_"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "my_analyzer",
        "search_analyzer": "my_stop_analyzer",
        "search_quote_analyzer": "my_analyzer"
      }
    }
  }
}
```

위 구성에서:
- `analyzer`: 인덱싱 시 불용어를 포함한 모든 용어를 인덱싱
- `search_analyzer`: 비구문 쿼리에서 불용어를 제거
- `search_quote_analyzer`: 구문 쿼리에서 불용어를 제거하지 않음

---

## 내장 분석기

Elasticsearch는 다양한 내장 분석기를 제공하며, 별도의 구성 없이 모든 인덱스에서 사용할 수 있습니다.

### Standard 분석기

`standard` 분석기는 유니코드 텍스트 분할 알고리즘에 정의된 대로 단어 경계에서 텍스트를 용어로 나눕니다. 대부분의 구두점을 제거하고 용어를 소문자로 변환하며 불용어 제거를 지원합니다.

```json
POST _analyze
{
  "analyzer": "standard",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과 토큰: `[ the, 2, quick, brown, foxes, jumped, over, the, lazy, dog's, bone ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `max_token_length` | 최대 토큰 길이. 초과 시 `max_token_length` 간격으로 분할 | 255 |
| `stopwords` | 사전 정의된 불용어 목록 또는 불용어 배열 | `_none_` |
| `stopwords_path` | 불용어가 포함된 파일 경로 | - |

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_english_analyzer": {
          "type": "standard",
          "max_token_length": 5,
          "stopwords": "_english_"
        }
      }
    }
  }
}
```

### Simple 분석기

`simple` 분석기는 문자가 아닌 문자를 만날 때마다 텍스트를 용어로 나눕니다. 모든 용어를 소문자로 변환합니다.

```json
POST _analyze
{
  "analyzer": "simple",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과 토큰: `[ the, quick, brown, foxes, jumped, over, the, lazy, dog, s, bone ]`

참고: 숫자와 구두점이 토큰에서 제거됩니다.

### Whitespace 분석기

`whitespace` 분석기는 공백 문자를 만날 때마다 텍스트를 용어로 나눕니다. 용어를 소문자로 변환하지 않습니다.

```json
POST _analyze
{
  "analyzer": "whitespace",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과 토큰: `[ The, 2, QUICK, Brown-Foxes, jumped, over, the, lazy, dog's, bone. ]`

참고: 대소문자가 유지되고 구두점도 보존됩니다.

### Stop 분석기

`stop` 분석기는 `simple` 분석기와 비슷하지만 불용어 제거도 지원합니다.

```json
POST _analyze
{
  "analyzer": "stop",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과 토큰: `[ quick, brown, foxes, jumped, lazy, dog, s, bone ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `stopwords` | 사전 정의된 불용어 목록 또는 불용어 배열 | `_english_` |
| `stopwords_path` | 불용어가 포함된 파일 경로 | - |

### Keyword 분석기

`keyword` 분석기는 어떤 텍스트가 주어지든 그대로 단일 용어로 출력하는 "무작업(noop)" 분석기입니다.

```json
POST _analyze
{
  "analyzer": "keyword",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과 토큰: `[ The 2 QUICK Brown-Foxes jumped over the lazy dog's bone. ]`

사용 사례: 우편번호, 제품 ID 등 정확히 일치해야 하는 필드

### Pattern 분석기

`pattern` 분석기는 정규 표현식을 사용하여 텍스트를 용어로 나눕니다. 정규 표현식은 토큰 자체가 아닌 토큰 구분자와 일치해야 합니다.

```json
POST _analyze
{
  "analyzer": "pattern",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과 토큰: `[ the, 2, quick, brown, foxes, jumped, over, the, lazy, dog, s, bone ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `pattern` | Java 정규 표현식 | `\W+` |
| `flags` | Java 정규 표현식 플래그 | - |
| `lowercase` | 용어를 소문자로 변환할지 여부 | `true` |
| `stopwords` | 사전 정의된 불용어 목록 또는 불용어 배열 | `_none_` |
| `stopwords_path` | 불용어가 포함된 파일 경로 | - |

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_email_analyzer": {
          "type": "pattern",
          "pattern": "\\W|_",
          "lowercase": true
        }
      }
    }
  }
}
```

### Fingerprint 분석기

`fingerprint` 분석기는 OpenRefine 프로젝트에서 클러스터링을 지원하는 데 사용되는 핑거프린팅 알고리즘을 구현합니다.

입력 텍스트가 소문자로 변환되고, 확장 문자가 제거되도록 정규화되고, 정렬되고, 중복이 제거되고, 단일 토큰으로 연결됩니다.

```json
POST _analyze
{
  "analyzer": "fingerprint",
  "text": "Yes yes, Gödel said this sentence is consistent and target is consistent"
}
```

결과 토큰: `[ and consistent godel is said sentence target this yes ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `separator` | 용어를 연결하는 데 사용할 문자 | 공백 |
| `max_output_size` | 최대 토큰 크기 | 255 |
| `stopwords` | 사전 정의된 불용어 목록 또는 불용어 배열 | `_none_` |
| `stopwords_path` | 불용어가 포함된 파일 경로 | - |

### Language 분석기 (언어 분석기)

Elasticsearch는 특정 언어 텍스트를 분석하기 위한 분석기 세트를 제공합니다.

지원되는 언어:
- `arabic`, `armenian`, `basque`, `bengali`, `brazilian`, `bulgarian`
- `catalan`, `cjk`, `czech`, `danish`, `dutch`, `english`
- `estonian`, `finnish`, `french`, `galician`, `german`, `greek`
- `hindi`, `hungarian`, `indonesian`, `irish`, `italian`, `latvian`
- `lithuanian`, `norwegian`, `persian`, `portuguese`, `romanian`, `russian`
- `serbian`, `sorani`, `spanish`, `swedish`, `turkish`, `thai`

예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_english": {
          "type": "english",
          "stopwords": ["a", "the"]
        }
      }
    }
  }
}
```

언어 분석기의 공통 구성:

모든 분석기는 커스텀 불용어를 내부적으로 설정하거나 `stopwords_path`로 외부 불용어 파일을 사용하여 설정할 수 있습니다.

stem_exclusion 매개변수:

`stem_exclusion` 매개변수를 사용하여 스테밍되지 않아야 하는 소문자 단어 배열을 지정할 수 있습니다. 내부적으로 이 기능은 `keywords`가 `stem_exclusion` 매개변수 값으로 설정된 `keyword_marker` 토큰 필터를 추가하여 구현됩니다.

stem_exclusion을 지원하는 분석기:
- `arabic`, `armenian`, `basque`, `bengali`, `bulgarian`, `catalan`
- `czech`, `dutch`, `english`, `finnish`, `french`, `galician`
- `german`, `hindi`, `hungarian`, `indonesian`, `irish`, `italian`
- `latvian`, `lithuanian`, `norwegian`, `portuguese`, `romanian`, `russian`
- `serbian`, `sorani`, `spanish`, `swedish`, `turkish`

---

## 분석기 구성

내장 분석기는 별도 구성 없이 바로 사용할 수 있습니다. 하지만 일부 분석기는 동작을 조정하는 데 사용할 수 있는 구성 옵션을 지원합니다.

### Standard 분석기 구성 예시

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "std_english": {
          "type": "standard",
          "stopwords": "_english_"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "my_text": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "english": {
            "type": "text",
            "analyzer": "std_english"
          }
        }
      }
    }
  }
}
```

위 예시에서:
- `my_text` 필드는 기본 `standard` 분석기를 사용
- `my_text.english` 서브필드는 영어 불용어를 제거하는 구성된 `standard` 분석기를 사용

### 테스트

```json
POST my-index-000001/_analyze
{
  "field": "my_text",
  "text": "The old brown cow"
}
```

결과: `[ the, old, brown, cow ]`

```json
POST my-index-000001/_analyze
{
  "field": "my_text.english",
  "text": "The old brown cow"
}
```

결과: `[ old, brown, cow ]` (불용어 "the"가 제거됨)

---

## 사용자 정의 분석기 생성

내장 분석기가 요구사항에 맞지 않을 때 단일 토크나이저, 0개 이상의 토큰 필터, 0개 이상의 캐릭터 필터를 결합하여 `custom` 분석기를 만들 수 있습니다.

### 기본 구성

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_custom_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "char_filter": [
            "html_strip"
          ],
          "filter": [
            "lowercase",
            "asciifolding"
          ]
        }
      }
    }
  }
}
```

구성 매개변수:

| 매개변수 | 설명 |
|---------|------|
| `type` | 분석기 유형. 사용자 정의 분석기의 경우 `custom`을 사용하거나 생략 |
| `tokenizer` | 내장 또는 사용자 정의 토크나이저 (필수) |
| `char_filter` | 내장 또는 사용자 정의 캐릭터 필터 배열 (선택) |
| `filter` | 내장 또는 사용자 정의 토큰 필터 배열 (선택) |
| `position_increment_gap` | 텍스트 값 배열을 인덱싱할 때 사이에 추가되는 가짜 용어 위치 수. 기본값은 100 |

### 테스트

```json
POST my-index-000001/_analyze
{
  "analyzer": "my_custom_analyzer",
  "text": "Is this <b>déjà vu</b>?"
}
```

결과 토큰: `[ is, this, deja, vu ]`

설명:
- `html_strip` 캐릭터 필터가 HTML 태그 제거
- `standard` 토크나이저가 단어 경계에서 텍스트 분리
- `lowercase` 필터가 소문자로 변환
- `asciifolding` 필터가 악센트 문자를 ASCII로 변환 (`déjà` → `deja`)

### 사용자 정의 구성요소를 포함한 고급 예시

기본 구성으로 토크나이저, 토큰 필터, 캐릭터 필터를 사용할 수 있지만, 각각의 구성된 버전을 만들어 사용자 정의 분석기에서 사용할 수 있습니다.

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_custom_analyzer": {
          "char_filter": [
            "emoticons"
          ],
          "tokenizer": "punctuation",
          "filter": [
            "lowercase",
            "english_stop"
          ]
        }
      },
      "tokenizer": {
        "punctuation": {
          "type": "pattern",
          "pattern": "[ .,!?]"
        }
      },
      "char_filter": {
        "emoticons": {
          "type": "mapping",
          "mappings": [
            ":) => _happy_",
            ":( => _sad_"
          ]
        }
      },
      "filter": {
        "english_stop": {
          "type": "stop",
          "stopwords": "_english_"
        }
      }
    }
  }
}
```

### 테스트

```json
POST my-index-000001/_analyze
{
  "analyzer": "my_custom_analyzer",
  "text": "I'm a :) person, and you?"
}
```

결과 토큰: `[ i'm, _happy_, person, you ]`

설명:
- `emoticons` 캐릭터 필터가 `:)`를 `_happy_`로 변환
- `punctuation` 토크나이저가 구두점에서 텍스트 분리
- `lowercase` 필터가 소문자로 변환
- `english_stop` 필터가 불용어 "a", "and" 제거

### 기존 인덱스에 분석기 추가

기존 인덱스에 분석기를 추가하려면 먼저 인덱스를 닫고, 분석기를 정의한 다음, 인덱스를 다시 열어야 합니다.

```json
POST my-index-000001/_close

PUT my-index-000001/_settings
{
  "analysis": {
    "analyzer": {
      "my_new_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": ["lowercase"]
      }
    }
  }
}

POST my-index-000001/_open
```

---

## 토크나이저 레퍼런스

토크나이저는 문자 스트림을 받아 개별 토큰으로 나누고 토큰 스트림을 출력합니다.

토크나이저는 또한 다음 정보를 기록합니다:
- 각 용어의 순서 또는 위치 (구문 및 단어 근접 쿼리에 사용)
- 용어가 나타내는 원본 단어의 시작 및 끝 문자 오프셋 (검색 스니펫 하이라이팅에 사용)

### 단어 지향 토크나이저 (Word Oriented Tokenizers)

#### Standard 토크나이저

`standard` 토크나이저는 유니코드 텍스트 분할 알고리즘에 정의된 대로 단어 경계에서 텍스트를 용어로 나눕니다. 대부분의 구두점 기호를 제거합니다. 대부분의 언어에서 최선의 선택입니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과: `[ The, 2, QUICK, Brown, Foxes, jumped, over, the, lazy, dog's, bone ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `max_token_length` | 최대 토큰 길이 | 255 |

#### Letter 토크나이저

`letter` 토크나이저는 문자가 아닌 문자를 만날 때마다 텍스트를 용어로 나눕니다.

```json
POST _analyze
{
  "tokenizer": "letter",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과: `[ The, QUICK, Brown, Foxes, jumped, over, the, lazy, dog, s, bone ]`

#### Lowercase 토크나이저

`lowercase` 토크나이저는 `letter` 토크나이저와 마찬가지로 문자가 아닌 문자를 만날 때마다 텍스트를 용어로 나누지만, 모든 용어를 소문자로 변환합니다.

```json
POST _analyze
{
  "tokenizer": "lowercase",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과: `[ the, quick, brown, foxes, jumped, over, the, lazy, dog, s, bone ]`

#### Whitespace 토크나이저

`whitespace` 토크나이저는 공백 문자를 만날 때마다 텍스트를 용어로 나눕니다.

```json
POST _analyze
{
  "tokenizer": "whitespace",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과: `[ The, 2, QUICK, Brown-Foxes, jumped, over, the, lazy, dog's, bone. ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `max_token_length` | 최대 토큰 길이 | 255 |

#### UAX URL Email 토크나이저

`uax_url_email` 토크나이저는 `standard` 토크나이저와 비슷하지만 URL과 이메일 주소를 단일 토큰으로 인식합니다.

```json
POST _analyze
{
  "tokenizer": "uax_url_email",
  "text": "Email me at john.smith@global-international.com"
}
```

결과: `[ Email, me, at, john.smith@global-international.com ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `max_token_length` | 최대 토큰 길이 | 255 |

#### Classic 토크나이저

`classic` 토크나이저는 영어 문서에 적합한 문법 기반 토크나이저입니다. 두문자어, 회사명, 이메일 주소, 인터넷 호스트명에 대한 특별 처리를 위한 휴리스틱을 가지고 있습니다.

```json
POST _analyze
{
  "tokenizer": "classic",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

결과: `[ The, 2, QUICK, Brown, Foxes, jumped, over, the, lazy, dog's, bone ]`

참고: 이 토크나이저는 대부분의 비영어권 언어에서는 잘 작동하지 않습니다.

#### Thai 토크나이저

`thai` 토크나이저는 태국어 텍스트를 단어로 분할합니다. 태국어 사전을 사용합니다.

```json
POST _analyze
{
  "tokenizer": "thai",
  "text": "การทดสอบ"
}
```

### 부분 단어 토크나이저 (Partial Word Tokenizers)

#### N-gram 토크나이저

`ngram` 토크나이저는 지정된 문자 목록(예: 공백 또는 구두점)을 만날 때마다 텍스트를 단어로 나눈 다음, 각 단어의 n-gram을 반환합니다.

예: `quick` → `[qu, ui, ic, ck]`

```json
POST _analyze
{
  "tokenizer": "ngram",
  "text": "Quick Fox"
}
```

결과: `[ Q, Qu, u, ui, i, ic, c, ck, k, " ", " F", F, Fo, o, ox, x ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `min_gram` | gram의 최소 문자 길이 | 1 |
| `max_gram` | gram의 최대 문자 길이 | 2 |
| `token_chars` | 토큰에 포함되어야 하는 문자 클래스. 이 클래스에 속하지 않는 문자에서 분할 | `[]` (모든 문자 유지) |
| `custom_token_chars` | 토큰의 일부로 처리해야 하는 사용자 정의 문자 | - |

token_chars 문자 클래스:
- `letter` - 예: a, b, ï, 京
- `digit` - 예: 3, 7
- `whitespace` - 예: " ", "\n"
- `punctuation` - 예: !, "
- `symbol` - 예: $, √
- `custom` - `custom_token_chars` 설정을 사용하여 정의

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "my_tokenizer"
        }
      },
      "tokenizer": {
        "my_tokenizer": {
          "type": "ngram",
          "min_gram": 3,
          "max_gram": 3,
          "token_chars": [
            "letter",
            "digit"
          ]
        }
      }
    }
  }
}
```

#### Edge N-gram 토크나이저

`edge_ngram` 토크나이저는 먼저 지정된 문자 목록(예: 공백 또는 구두점)을 만날 때마다 텍스트를 단어로 나눈 다음, 시작이 단어의 시작에 고정된 각 단어의 N-gram을 출력합니다.

예: `quick` → `[q, qu, qui, quic, quick]`

Edge N-gram은 검색 시 자동완성(search-as-you-type) 쿼리에 유용합니다.

```json
POST _analyze
{
  "tokenizer": "edge_ngram",
  "text": "Quick Fox"
}
```

결과: `[ Q, Qu ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `min_gram` | gram의 최소 문자 길이 | 1 |
| `max_gram` | gram의 최대 문자 길이 | 2 |
| `token_chars` | 토큰에 포함되어야 하는 문자 클래스 | `[]` (모든 문자 유지) |
| `custom_token_chars` | 토큰의 일부로 처리해야 하는 사용자 정의 문자 | - |

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "my_tokenizer"
        }
      },
      "tokenizer": {
        "my_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 10,
          "token_chars": [
            "letter",
            "digit"
          ]
        }
      }
    }
  }
}
```

권장 사항: `edge_ngram` 토크나이저는 인덱스 시간에만 사용하여 부분 단어가 인덱스에서 매칭되도록 하는 것이 합리적입니다. 검색 시에는 사용자가 입력한 용어만 검색하면 됩니다.

### 구조화된 텍스트 토크나이저 (Structured Text Tokenizers)

#### Keyword 토크나이저

`keyword` 토크나이저는 어떤 텍스트가 주어지든 그대로 단일 용어로 출력하는 "무작업(noop)" 토크나이저입니다.

```json
POST _analyze
{
  "tokenizer": "keyword",
  "text": "New York"
}
```

결과: `[ New York ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `buffer_size` | 버퍼 크기 | 256 |

#### Pattern 토크나이저

`pattern` 토크나이저는 정규 표현식을 사용하여 텍스트를 용어로 나눕니다. 정규 표현식은 토큰 자체가 아닌 단어 구분자와 일치해야 합니다. 기본 패턴은 `\W+`입니다.

```json
POST _analyze
{
  "tokenizer": "pattern",
  "text": "The foo_bar_size's default is 5."
}
```

결과: `[ The, foo_bar_size, s, default, is, 5 ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `pattern` | Java 정규 표현식 | `\W+` |
| `flags` | Java 정규 표현식 플래그 | - |
| `group` | 토큰으로 추출할 캡처 그룹 | -1 (패턴에서 분할) |

예시: 이메일 분리

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "my_tokenizer"
        }
      },
      "tokenizer": {
        "my_tokenizer": {
          "type": "pattern",
          "pattern": "([^@]+)@([^.]+)",
          "group": 1
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "john.smith@example.com"
}
```

결과: `[ john.smith ]`

#### Simple Pattern 토크나이저

`simple_pattern` 토크나이저는 정규 표현식을 사용하여 일치하는 텍스트를 용어로 캡처합니다. 제한된 정규 표현식 하위 집합을 지원하며, 일반적으로 `pattern` 토크나이저보다 빠릅니다.

#### Simple Pattern Split 토크나이저

`simple_pattern_split` 토크나이저는 정규 표현식 패턴 일치를 단어 구분자로 사용합니다.

#### Char Group 토크나이저

`char_group` 토크나이저는 정의된 문자 집합에서 분할하도록 구성할 수 있습니다. `pattern` 토크나이저보다 빠른 경우가 많습니다.

```json
POST _analyze
{
  "tokenizer": {
    "type": "char_group",
    "tokenize_on_chars": [
      "whitespace",
      "-",
      "\n"
    ]
  },
  "text": "The QUICK brown-fox"
}
```

결과: `[ The, QUICK, brown, fox ]`

#### Path Hierarchy 토크나이저

`path_hierarchy` 토크나이저는 제공된 입력 경로에 있는 모든 가능한 경로를 생성합니다.

```json
POST _analyze
{
  "tokenizer": "path_hierarchy",
  "text": "/one/two/three"
}
```

결과: `[ /one, /one/two, /one/two/three ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `delimiter` | 경로 구분 문자 | `/` |
| `replacement` | 대체 문자 | `delimiter` |
| `buffer_size` | 버퍼 크기 | 1024 |
| `reverse` | 토큰을 역순으로 생성 | `false` |
| `skip` | 건너뛸 초기 토큰 수 | `0` |

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "my_tokenizer"
        }
      },
      "tokenizer": {
        "my_tokenizer": {
          "type": "path_hierarchy",
          "delimiter": "-",
          "replacement": "/",
          "skip": 2
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "one-two-three-four-five"
}
```

결과: `[ /three, /three/four, /three/four/five ]`

---

## 토큰 필터 레퍼런스

토큰 필터는 토크나이저로부터 토큰 스트림을 받아 토큰을 추가, 제거 또는 변경할 수 있습니다. Elasticsearch는 거의 50개의 토큰 필터를 제공합니다.

### 주요 토큰 필터

#### Lowercase 필터

`lowercase` 필터는 모든 토큰을 소문자로 변환합니다. 이렇게 하면 문자열의 대소문자가 쿼리의 토큰이 문서의 토큰과 일치하는 능력에 영향을 주지 않습니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "lowercase" ],
  "text": "THE Quick FoX JUMPs"
}
```

결과: `[ the, quick, fox, jumps ]`

#### Uppercase 필터

`uppercase` 필터는 모든 토큰을 대문자로 변환합니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "uppercase" ],
  "text": "the Quick FoX JUMPs"
}
```

결과: `[ THE, QUICK, FOX, JUMPS ]`

#### Stop 필터

`stop` 필터는 소위 불용어(stop words)를 인덱싱 전에 제거합니다. 언어별로 제거할 단어 목록을 선택하거나 사용자 정의 목록을 전달할 수 있습니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "stop" ],
  "text": "a]quick fox jumps over the lazy dog"
}
```

결과: `[ quick, fox, jumps, lazy, dog ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `stopwords` | 사전 정의된 불용어 목록 또는 불용어 배열 | `_english_` |
| `stopwords_path` | 불용어가 포함된 파일 경로 | - |
| `ignore_case` | true이면 불용어 일치가 대소문자를 구분하지 않음 | `false` |
| `remove_trailing` | true이면 스트림의 마지막 토큰이 불용어이면 제거 | `true` |

사전 정의된 불용어 목록:
- `_arabic_`, `_armenian_`, `_basque_`, `_bengali_`, `_brazilian_`
- `_bulgarian_`, `_catalan_`, `_cjk_`, `_czech_`, `_danish_`
- `_dutch_`, `_english_`, `_estonian_`, `_finnish_`, `_french_`
- `_galician_`, `_german_`, `_greek_`, `_hindi_`, `_hungarian_`
- `_indonesian_`, `_irish_`, `_italian_`, `_latvian_`, `_lithuanian_`
- `_norwegian_`, `_persian_`, `_portuguese_`, `_romanian_`, `_russian_`
- `_sorani_`, `_spanish_`, `_swedish_`, `_thai_`, `_turkish_`
- `_none_` (빈 불용어 목록)

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "whitespace",
          "filter": [ "my_custom_stop_words_filter" ]
        }
      },
      "filter": {
        "my_custom_stop_words_filter": {
          "type": "stop",
          "ignore_case": true,
          "stopwords": [ "and", "is", "the" ]
        }
      }
    }
  }
}
```

#### Stemmer 필터

`stemmer` 필터는 여러 언어에 대한 알고리즘 스테밍을 제공합니다. 스테밍은 용어를 어간으로 축소합니다.

예: "collected", "collect", "collecting" → "collect"

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "stemmer" ],
  "text": "foxes jumping quickly"
}
```

결과: `[ fox, jump, quickli ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `language` 또는 `name` | 스테머 언어 | `english` |

지원되는 언어:

- 아라비아어: `arabic`
- 아르메니아어: `armenian`
- 바스크어: `basque`
- 벵골어: `bengali`, `light_bengali`
- 불가리아어: `bulgarian`
- 카탈루냐어: `catalan`
- 체코어: `czech`
- 덴마크어: `danish`
- 네덜란드어: `dutch`, `dutch_kp`
- 영어: `english`, `light_english`, `minimal_english`, `porter2`, `possessive_english`
- 에스토니아어: `estonian`
- 핀란드어: `finnish`, `light_finnish`
- 프랑스어: `french`, `light_french`, `minimal_french`
- 갈리시아어: `galician`, `minimal_galician`
- 독일어: `german`, `german2`, `light_german`, `minimal_german`
- 그리스어: `greek`
- 힌디어: `hindi`
- 헝가리어: `hungarian`, `light_hungarian`
- 인도네시아어: `indonesian`
- 아일랜드어: `irish`
- 이탈리아어: `italian`, `light_italian`
- 쿠르드어(소라니): `sorani`
- 라트비아어: `latvian`
- 리투아니아어: `lithuanian`
- 노르웨이어: `norwegian`, `light_norwegian`, `minimal_norwegian`
- 페르시아어: `persian` (9.0에서 스테밍 추가됨)
- 포르투갈어: `portuguese`, `light_portuguese`, `minimal_portuguese`, `portuguese_rslp`
- 루마니아어: `romanian`
- 러시아어: `russian`, `light_russian`
- 세르비아어: `serbian`
- 스페인어: `spanish`, `light_spanish`
- 스웨덴어: `swedish`, `light_swedish`
- 터키어: `turkish`

참고: 8.16.0에서 `lovins` 영어 스테머가 더 이상 사용되지 않습니다.

#### Porter Stem 필터

`porter_stem` 필터는 Porter 스테밍 알고리즘을 기반으로 영어에 대한 알고리즘 스테밍을 제공합니다. 이 필터는 `kstem` 필터와 같은 다른 영어 스테머 필터보다 더 공격적으로 스테밍하는 경향이 있습니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "porter_stem" ],
  "text": "the foxes jumping quickly"
}
```

결과: `[ the, fox, jump, quickli ]`

#### Snowball 필터

`snowball` 필터는 Snowball 생성 스테머를 사용하여 단어를 스테밍합니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "snowball" ],
  "text": "the foxes jumping quickly"
}
```

결과: `[ the, fox, jump, quick ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `language` | 스노우볼 스테머 언어 | `English` |

사용 가능한 언어:
- `Arabic`, `Armenian`, `Basque`, `Catalan`, `Danish`
- `Dutch`, `English`, `Estonian`, `Finnish`, `French`
- `German`, `German2`, `Hungarian`, `Italian`, `Irish`
- `Kp` (8.16.0에서 더 이상 사용되지 않음)
- `Lithuanian`, `Lovins` (8.16.0에서 더 이상 사용되지 않음)
- `Norwegian`, `Porter`, `Portuguese`, `Romanian`
- `Russian`, `Serbian`, `Spanish`, `Swedish`, `Turkish`

#### Synonym 필터

`synonym` 필터를 사용하면 분석 과정에서 동의어를 쉽게 처리할 수 있습니다.

구성 매개변수:

| 매개변수 | 설명 |
|---------|------|
| `synonyms` | 인라인 동의어 정의 |
| `synonyms_path` | 동의어 파일 경로 (config 디렉토리 기준) |
| `synonyms_set` | Synonyms Management API로 생성된 동의어 세트 |
| `expand` | 동등 동의어에 대해 모든 용어로 확장할지 여부 |
| `lenient` | true이면 동의어 구성을 구문 분석하는 동안 예외를 무시 |

동의어 형식:

1. 명시적 매핑 (Explicit mappings):
   ```
   i-pod, i pod => ipod
   ```
   `=>`를 사용하여 정확한 대체를 지정합니다.

2. 동등 동의어 (Equivalent synonyms):
   ```
   computer, pc, laptop
   ```
   쉼표로 구분된 동의어들이 모두 동등하게 취급됩니다.

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "whitespace",
          "filter": [ "my_synonym_filter" ]
        }
      },
      "filter": {
        "my_synonym_filter": {
          "type": "synonym",
          "synonyms": [
            "quick,fast,speedy",
            "big => large"
          ]
        }
      }
    }
  }
}
```

#### Synonym Graph 필터

`synonym_graph` 필터를 사용하면 다중 단어 동의어를 포함한 동의어를 분석 과정에서 올바르게 처리할 수 있습니다. 다중 단어 동의어를 적절히 처리하기 위해 이 토큰 필터는 처리 중에 그래프 토큰 스트림을 생성합니다.

중요: 이 토큰 필터는 검색 분석기의 일부로만 사용하도록 설계되었습니다. 인덱싱 중에 동의어를 적용하려면 표준 `synonym` 토큰 필터를 사용하세요.

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `synonyms` | 인라인 동의어 정의 | - |
| `synonyms_path` | 동의어 파일 경로 | - |
| `synonyms_set` | Synonyms API로 생성된 동의어 세트 | - |
| `expand` | 동등 동의어 확장 여부 | `true` |
| `format` | 동의어 형식 (`solr` 또는 `wordnet`) | `solr` |
| `updateable` | true이면 검색 분석기를 다시 로드하여 동의어 파일 변경 사항 적용 가능 | `false` |

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "search_synonyms": {
          "tokenizer": "whitespace",
          "filter": [ "graph_synonyms" ]
        }
      },
      "filter": {
        "graph_synonyms": {
          "type": "synonym_graph",
          "synonyms_path": "analysis/synonym.txt"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "standard",
        "search_analyzer": "search_synonyms"
      }
    }
  }
}
```

#### ASCII Folding 필터

`asciifolding` 필터는 기본 라틴 유니코드 블록(처음 127개 ASCII 문자)에 없는 알파벳, 숫자, 기호 문자를 동등한 ASCII 문자로 변환합니다(존재하는 경우).

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "asciifolding" ],
  "text": "açaí à la carte"
}
```

결과: `[ acai, a, la, carte ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `preserve_original` | true이면 원래 토큰과 폴딩된 토큰 모두 출력 | `false` |

#### Length 필터

`length` 필터는 지정된 문자 길이 범위를 벗어나는 토큰을 제거합니다.

```json
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": [
    {
      "type": "length",
      "min": 0,
      "max": 4
    }
  ],
  "text": "the quick brown fox jumps over the lazy dog"
}
```

결과: `[ the, fox, over, the, lazy, dog ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `min` | 최소 문자 길이 | `0` |
| `max` | 최대 문자 길이 | `Integer.MAX_VALUE` |

#### Truncate 필터

`truncate` 필터는 지정된 문자 제한을 초과하는 토큰을 자릅니다. 이 제한은 기본적으로 10이지만 `length` 매개변수를 사용하여 사용자 정의할 수 있습니다.

```json
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": [
    {
      "type": "truncate",
      "length": 3
    }
  ],
  "text": "the quick brown fox jumps over the lazy dog"
}
```

결과: `[ the, qui, bro, fox, jum, ove, the, laz, dog ]`

#### Trim 필터

`trim` 필터는 스트림의 각 토큰에서 앞뒤 공백을 제거합니다.

```json
POST _analyze
{
  "tokenizer": "keyword",
  "filter": [ "trim" ],
  "text": "  foo bar  "
}
```

결과: `[ foo bar ]`

#### Unique 필터

`unique` 필터는 스트림에서 중복 토큰을 제거합니다.

```json
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": [ "unique" ],
  "text": "the lazy lazy dog"
}
```

결과: `[ the, lazy, dog ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `only_on_same_position` | true이면 동일한 위치의 중복 토큰만 제거 | `false` |

#### Reverse 필터

`reverse` 필터는 스트림의 각 토큰을 뒤집습니다. 뒤집힌 토큰은 접미사 기반 검색에 유용합니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "reverse" ],
  "text": "quick fox jumps"
}
```

결과: `[ kciuq, xof, spmuj ]`

#### Elision 필터

`elision` 필터는 토큰 시작 부분에서 지정된 생략(elision)을 제거합니다. 예를 들어, 이 필터를 사용하여 프랑스어에서 `l'avion`을 `avion`으로 변환할 수 있습니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "elision" ],
  "text": "l'avion"
}
```

결과: `[ avion ]`

#### N-gram 필터

`ngram` 필터는 지정된 길이의 토큰에서 n-gram을 형성합니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "ngram" ],
  "text": "Quick fox"
}
```

결과: `[ Q, Qu, u, ui, i, ic, c, ck, k, f, fo, o, ox, x ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `min_gram` | gram의 최소 문자 길이 | `1` |
| `max_gram` | gram의 최대 문자 길이 | `2` |
| `preserve_original` | true이면 원래 토큰도 출력 | `false` |

#### Edge N-gram 필터

`edge_ngram` 필터는 토큰의 시작 부분에 고정된 지정 길이의 n-gram을 형성합니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "filter": [ "edge_ngram" ],
  "text": "Quick fox"
}
```

결과: `[ Q, Qu, f, fo ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `min_gram` | gram의 최소 문자 길이 | `1` |
| `max_gram` | gram의 최대 문자 길이 | `2` |
| `preserve_original` | true이면 원래 토큰도 출력 | `false` |
| `side` | 시작 또는 끝에서 n-gram을 잘라낼 방향. 더 이상 사용되지 않음 | `front` |

#### Shingle 필터

`shingle` 필터는 토큰 스트림에서 shingle(단어 n-gram)을 추가합니다. 주로 구문 쿼리 성능을 향상시키는 데 사용됩니다.

```json
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": [ "shingle" ],
  "text": "quick brown fox jumps"
}
```

결과: `[ quick, quick brown, brown, brown fox, fox, fox jumps, jumps ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `min_shingle_size` | shingle의 최소 단어 수 | `2` |
| `max_shingle_size` | shingle의 최대 단어 수 | `2` |
| `output_unigrams` | true이면 개별 토큰도 출력 | `true` |
| `output_unigrams_if_no_shingles` | true이면 shingle이 없을 때만 개별 토큰 출력 | `false` |
| `token_separator` | shingle의 인접 토큰 사이에 사용할 구분자 | `" "` |
| `filler_token` | 위치 증가로 인해 비어 있는 위치를 대체할 문자열 | `"_"` |

#### Keyword Marker 필터

`keyword_marker` 필터는 지정된 토큰을 키워드로 표시하여 후속 스테밍 토큰 필터가 해당 단어를 건드리지 않도록 합니다.

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "whitespace",
          "filter": [ "my_keyword_marker", "stemmer" ]
        }
      },
      "filter": {
        "my_keyword_marker": {
          "type": "keyword_marker",
          "keywords": [ "jumping" ]
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "foxes jumping quickly"
}
```

결과: `[ fox, jumping, quickli ]` ("jumping"은 스테밍되지 않음)

#### Conditional 필터

`conditional` 필터는 제공된 조건 스크립트의 조건을 충족하는 토큰에 일련의 토큰 필터를 적용합니다.

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "standard",
          "filter": [ "my_condition" ]
        }
      },
      "filter": {
        "my_condition": {
          "type": "condition",
          "filter": [ "lowercase" ],
          "script": {
            "source": "token.getTerm().length() < 5"
          }
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "THE QUICK BROWN FOX"
}
```

결과: `[ the, QUICK, BROWN, fox ]` (5자 미만인 토큰만 소문자로 변환)

#### Fingerprint 필터

`fingerprint` 필터는 토큰을 정렬하고 중복을 제거한 다음 단일 출력 토큰으로 연결합니다.

```json
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": [ "fingerprint" ],
  "text": "zebra jumps over resting resting dog"
}
```

결과: `[ dog jumps over resting zebra ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `max_output_size` | 연결된 출력 토큰의 최대 문자 크기 | `255` |
| `separator` | 출력 토큰을 연결할 때 사용할 문자 | `" "` |

---

## 캐릭터 필터 레퍼런스

캐릭터 필터는 토크나이저에 전달되기 전에 문자 스트림을 전처리(문자 추가, 제거 또는 변경)합니다.

Elasticsearch는 사용자 정의 분석기를 구축하는 데 사용할 수 있는 여러 내장 캐릭터 필터를 제공합니다.

### HTML Strip 캐릭터 필터

`html_strip` 캐릭터 필터는 `<b>`와 같은 HTML 요소를 제거하고 `&amp;`와 같은 HTML 엔티티를 디코딩합니다.

```json
POST _analyze
{
  "tokenizer": "keyword",
  "char_filter": [ "html_strip" ],
  "text": "<p>I&apos;m so <b>happy</b>!</p>"
}
```

결과: `[ \nI'm so happy!\n ]`

구성 매개변수:

| 매개변수 | 설명 |
|---------|------|
| `escaped_tags` | 제거하지 않을 HTML 요소의 배열 |

구성 예시:

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "keyword",
          "char_filter": [ "my_custom_html_strip_char_filter" ]
        }
      },
      "char_filter": {
        "my_custom_html_strip_char_filter": {
          "type": "html_strip",
          "escaped_tags": [ "b" ]
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "<p>I&apos;m so <b>happy</b>!</p>"
}
```

결과: `[ \nI'm so <b>happy</b>!\n ]` (`<b>` 태그는 유지됨)

### Mapping 캐릭터 필터

`mapping` 캐릭터 필터는 지정된 문자열의 모든 발생을 지정된 대체 문자열로 바꿉니다.

```json
POST _analyze
{
  "tokenizer": "keyword",
  "char_filter": [
    {
      "type": "mapping",
      "mappings": [
        "٠ => 0",
        "١ => 1",
        "٢ => 2",
        "٣ => 3",
        "٤ => 4",
        "٥ => 5",
        "٦ => 6",
        "٧ => 7",
        "٨ => 8",
        "٩ => 9"
      ]
    }
  ],
  "text": "My license plate is ٢٥٠١٥"
}
```

결과: `[ My license plate is 25015 ]`

구성 매개변수:

| 매개변수 | 설명 |
|---------|------|
| `mappings` | `key => value` 형식의 매핑 배열 |
| `mappings_path` | 매핑이 포함된 파일 경로 (config 디렉토리 기준 상대 경로 또는 절대 경로) |

참고:
- 가장 긴 패턴이 우선합니다 (예: 원본 문자열이 "javasampleapproach"이면 "javasample" 키가 "java" 키보다 우선)
- 대체 문자열은 빈 문자열일 수 있습니다

구성 예시 (이모티콘 매핑):

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "standard",
          "char_filter": [ "my_mappings_char_filter" ]
        }
      },
      "char_filter": {
        "my_mappings_char_filter": {
          "type": "mapping",
          "mappings": [
            ":) => _happy_",
            ":( => _sad_"
          ]
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "I'm feeling :) today"
}
```

결과: `[ I'm, feeling, _happy_, today ]`

### Pattern Replace 캐릭터 필터

`pattern_replace` 캐릭터 필터는 정규 표현식과 일치하는 모든 문자를 지정된 대체 문자열로 바꿉니다.

```json
POST _analyze
{
  "tokenizer": "standard",
  "char_filter": [
    {
      "type": "pattern_replace",
      "pattern": "(\\d+)-(?=\\d)",
      "replacement": "$1_"
    }
  ],
  "text": "My credit card is 123-456-789"
}
```

결과: `[ My, credit, card, is, 123_456_789 ]`

구성 매개변수:

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `pattern` | Java 정규 표현식 | (필수) |
| `replacement` | 대체 문자열. `$1`..`$9` 구문을 사용하여 캡처 그룹 참조 가능 | `""` |
| `flags` | Java 정규 표현식 플래그. 플래그는 파이프로 구분 (예: `"CASE_INSENSITIVE\|COMMENTS"`) | - |

구성 예시 (숫자 정리):

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "standard",
          "char_filter": [ "my_char_filter" ]
        }
      },
      "char_filter": {
        "my_char_filter": {
          "type": "pattern_replace",
          "pattern": "(\\d+\\.\\d+)",
          "replacement": "[$1]"
        }
      }
    }
  }
}

POST my-index-000001/_analyze
{
  "analyzer": "my_analyzer",
  "text": "The price is 12.99 dollars"
}
```

결과: `[ The, price, is, [12.99], dollars ]`

---

## 노말라이저

노말라이저(Normalizer)는 분석기와 유사하지만 단일 토큰만 출력할 수 있다는 점에서 다릅니다. 결과적으로 토크나이저가 없으며 문자별로 작동하는 캐릭터 필터와 토큰 필터의 하위 집합만 허용합니다.

노말라이저는 전체 텍스트 필드 대신 `keyword` 필드에서 작동합니다. 텍스트를 토큰으로 분해하지는 않지만 내용을 조정하기 위해 캐릭터 필터와 토큰 필터를 적용합니다.

### 사용 사례

노말라이저는 다음과 같은 경우에 특히 유용합니다:
- `keyword` 필드에서 정렬 또는 집계를 수행하면서 소문자 변환이나 악센트 제거와 같은 텍스트 처리가 필요한 경우
- 대소문자를 구분하지 않는 검색이 필요한 `keyword` 필드

### 내장 노말라이저

Elasticsearch는 `lowercase` 내장 노말라이저를 제공합니다. 다른 형태의 정규화를 위해서는 사용자 정의 구성이 필요합니다.

### 노말라이저에서 사용 가능한 필터

다음은 노말라이저 정의에서 사용할 수 있는 필터 목록입니다:

캐릭터 필터:
- `pattern_replace`

토큰 필터:
- `arabic_normalization`
- `asciifolding`
- `bengali_normalization`
- `cjk_width`
- `decimal_digit`
- `elision`
- `german_normalization`
- `hindi_normalization`
- `indic_normalization`
- `lowercase`
- `pattern_replace`
- `persian_normalization`
- `scandinavian_folding`
- `serbian_normalization`
- `sorani_normalization`
- `trim`
- `uppercase`

### 사용자 정의 노말라이저 생성

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "normalizer": {
        "my_normalizer": {
          "type": "custom",
          "char_filter": [],
          "filter": [ "lowercase", "asciifolding" ]
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

### 테스트

```json
POST my-index-000001/_analyze
{
  "normalizer": "my_normalizer",
  "text": "BÀR"
}
```

결과: `[ bar ]`

### 사용 예시

```json
PUT my-index-000001/_doc/1
{
  "foo": "BÀR"
}

PUT my-index-000001/_doc/2
{
  "foo": "bar"
}

PUT my-index-000001/_doc/3
{
  "foo": "baz"
}

POST my-index-000001/_refresh

GET my-index-000001/_search
{
  "query": {
    "term": {
      "foo": "BAR"
    }
  }
}
```

위 검색은 문서 1과 2를 모두 반환합니다. `BÀR`이 인덱스와 쿼리 시간 모두에서 `bar`로 변환되기 때문입니다.

### 서브필드와 함께 사용

노말라이저를 사용하면 인덱스의 값이 변경됩니다. 원래 값(예: 대문자 "A"가 있는 "Apple")을 유지하려면 서브필드를 사용할 수 있습니다. 이렇게 하면 원래 필드 값과 정규화된 필드 값을 모두 유지할 수 있습니다.

```json
PUT my-index-000001
{
  "settings": {
    "analysis": {
      "normalizer": {
        "my_normalizer": {
          "type": "custom",
          "filter": [ "lowercase" ]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "foo": {
        "type": "keyword",
        "fields": {
          "normalized": {
            "type": "keyword",
            "normalizer": "my_normalizer"
          }
        }
      }
    }
  }
}
```

위 구성에서:
- `foo` 필드: 원래 값을 유지 (예: "Apple")
- `foo.normalized` 필드: 정규화된 값을 저장 (예: "apple")

---

## 참고 자료

### 공식 문서 링크

- [Text Analysis Overview](https://www.elastic.co/docs/manage-data/data-store/text-analysis)
- [Anatomy of an Analyzer](https://www.elastic.co/docs/manage-data/data-store/text-analysis/anatomy-of-an-analyzer)
- [Configure Text Analysis](https://www.elastic.co/docs/manage-data/data-store/text-analysis/configure-text-analysis)
- [Create a Custom Analyzer](https://www.elastic.co/docs/manage-data/data-store/text-analysis/create-custom-analyzer)
- [Specify an Analyzer](https://www.elastic.co/docs/manage-data/data-store/text-analysis/specify-an-analyzer)
- [Test an Analyzer](https://www.elastic.co/docs/manage-data/data-store/text-analysis/test-an-analyzer)
- [Index and Search Analysis](https://www.elastic.co/docs/manage-data/data-store/text-analysis/index-search-analysis)
- [Built-in Analyzers Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html)
- [Tokenizer Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenizers.html)
- [Token Filter Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenfilters.html)
- [Character Filter Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-charfilters.html)
- [Normalizers](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-normalizers.html)
- [Language Analyzers](https://www.elastic.co/docs/reference/text-analysis/analysis-lang-analyzer)
- [Analyze API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-analyze)
- [Elasticsearch 9.0 Release Notes](https://www.elastic.co/guide/en/elastic-stack/9.0/release-notes-elasticsearch-9.0.0.html)
