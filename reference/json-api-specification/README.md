# JSON:API Specification 상세 정리

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [핵심 개념 및 구조](#3-핵심-개념-및-구조)
4. [버전별 차이](#4-버전별-차이)
5. [주요 키워드 및 필드 설명](#5-주요-키워드-및-필드-설명)
6. [사용 사례 및 활용 도구](#6-사용-사례-및-활용-도구)
7. [장점과 단점](#7-장점과-단점)
8. [실제 예시](#8-실제-예시)
9. [관련 생태계 및 커뮤니티](#9-관련-생태계-및-커뮤니티)

---

## 1. 개요

### JSON:API란

JSON:API는 클라이언트와 서버 간에 JSON 기반으로 리소스를 주고받는 방식을 표준화한 명세(specification) 이다. REST API를 설계할 때 응답 본문의 구조, 관계 표현 방식, 페이지네이션, 정렬, 필터링, 에러 형식 등을 어떻게 구성해야 하는지에 대한 구체적인 규약을 제공한다. 공식 미디어 타입은 `application/vnd.api+json`이며, IANA에 등록된 벤더 특화 미디어 타입이다.

JSON:API는 단순한 가이드라인이 아니라 엄격한 명세이다. 명세를 준수하는 서버와 클라이언트는 사전 합의 없이도 상호 운용이 가능하며, 응답 구조를 예측할 수 있기 때문에 범용 클라이언트 라이브러리를 구축할 수 있다. 이는 REST API 생태계에서 오랫동안 부재했던 구조적 계약(structural contract) 을 제공한다는 점에서 의의가 크다.

### 왜 만들어졌는가

REST 아키텍처 스타일은 HTTP 프로토콜 위에서 리소스를 다루는 원칙을 제시하지만, 응답 본문의 구체적인 형식에 대해서는 아무런 규정을 두지 않는다. 이로 인해 각 조직, 각 프로젝트마다 고유한 JSON 구조를 채택하게 되었으며, 다음과 같은 문제가 발생했다.

- 응답 형식의 파편화(fragmentation): 동일한 "게시글 목록 조회" API라 하더라도, 어떤 서비스는 `{ "posts": [...] }` 형태를 사용하고, 다른 서비스는 `{ "data": [...] }` 형태를 사용하며, 또 다른 서비스는 `{ "results": [...], "count": 10 }` 형태를 사용했다. 응답 구조가 서비스마다 달라 클라이언트 코드를 매번 새로 작성해야 했다.
- 관계(relationship) 표현의 비일관성: 관련 리소스를 임베딩(embedding)할지, 링크(linking)할지, ID 참조만 포함할지에 대한 통일된 관례가 없었다. 같은 서비스 내에서도 엔드포인트마다 방식이 달라지는 경우가 빈번했다.
- 에러 형식의 혼란: 에러 응답의 구조도 제각각이었다. HTTP 상태 코드만 사용하는 서비스, `{ "error": "message" }` 형태의 서비스, `{ "errors": [...] }` 배열 형태의 서비스 등 에러 처리 방식이 통일되지 않았다.
- 개발 시간 낭비(bikeshedding): 새 프로젝트를 시작할 때마다 "응답 JSON을 어떤 구조로 할 것인가"에 대한 논의가 반복되었다. 이는 전형적인 바이크쉐딩(bikeshedding) 문제로, 본질적이지 않은 결정에 과도한 시간을 소비하는 현상이었다.

### Anti-Bikeshedding 철학

JSON:API의 핵심 철학은 "anti-bikeshedding" 이다. 이 용어는 Parkinson의 사소함의 법칙(Law of Triviality)에서 유래한 것으로, 중요하지 않은 사안에 불필요하게 많은 시간을 투자하는 현상을 지칭한다. JSON:API 공식 사이트의 첫 문장도 이 철학을 명확히 드러낸다.

> "If you've ever argued with your team about the way your JSON responses should be formatted, JSON:API can be your anti-bikeshedding tool."

JSON:API는 응답 형식에 대한 의사결정을 명세에 위임함으로써, 개발 팀이 비즈니스 로직과 도메인 설계에 집중할 수 있도록 한다. 사전에 검증된 구조를 채택하면 형식 논쟁이 불필요해지고, 결과적으로 개발 생산성이 향상된다.

### 핵심 철학

| 원칙 | 설명 |
|------|------|
| Anti-Bikeshedding | 응답 형식에 대한 논의를 명세에 위임하여 불필요한 의사결정을 제거한다 |
| 일관성(Consistency) | 모든 리소스, 모든 엔드포인트에서 동일한 구조를 사용한다 |
| 발견 가능성(Discoverability) | 링크 기반으로 관련 리소스를 탐색할 수 있도록 HATEOAS 원칙을 반영한다 |
| 효율성(Efficiency) | Compound Documents, Sparse Fieldsets 등으로 네트워크 왕복을 최소화한다 |
| 관심사의 분리(Separation of Concerns) | 리소스의 속성(attributes)과 관계(relationships)를 명확히 분리한다 |
| 호환성(Compatibility) | 기존 HTTP 의미론과 REST 원칙을 존중하면서 그 위에 구조를 부여한다 |

---

## 2. 역사

### 시작: Yehuda Katz와 Ember.js 커뮤니티 (2013)

JSON:API의 역사는 Yehuda Katz로부터 시작된다. Yehuda Katz는 jQuery, Ruby on Rails, Ember.js 등 다수의 유명 오픈소스 프로젝트에 기여한 개발자로, 웹 프론트엔드와 백엔드 양쪽 모두에 깊은 이해를 가지고 있었다.

2013년, Yehuda Katz는 Ember.js 프레임워크의 데이터 레이어인 Ember Data를 개발하면서 REST API 응답 형식의 표준화 필요성을 절감했다. Ember Data는 서버에서 전달받은 JSON 데이터를 클라이언트 측 모델로 매핑하는 역할을 했는데, 모든 서버가 제각기 다른 응답 형식을 사용하기 때문에 범용 어댑터를 만들기가 극도로 어려웠다.

이 문제를 해결하기 위해 Yehuda Katz는 서버와 클라이언트가 공유할 수 있는 범용적인 JSON 응답 규약을 설계하기 시작했다. 초기 명칭은 단순히 "JSON API"였으며, Ember.js 커뮤니티 내에서 논의되고 발전했다. 초기 드래프트는 Ember Data의 직렬화(serialization) 규약에 크게 영향을 받았으나, 점차 Ember.js에 종속되지 않는 범용 명세로 발전해 나갔다.

### 초기 드래프트와 커뮤니티 형성 (2013-2014)

2013년 5월, JSON API의 첫 번째 드래프트가 공개되었다. 이 시기의 주요 특징은 다음과 같다.

- 리소스 객체 구조의 기본 틀이 제안되었다
- `type`과 `id`를 통한 리소스 식별 방식이 도입되었다
- 관계(relationships)의 표현 방식에 대한 논의가 활발히 진행되었다
- GitHub 저장소를 통해 오픈 소스 형태로 명세 개발이 이루어졌다

초기 드래프트는 현재 버전과 상당한 차이가 있었다. 예를 들어, 리소스의 속성과 관계가 동일한 레벨에 혼재되어 있었고, `links` 객체의 구조도 현재와 달랐다. 커뮤니티의 피드백을 통해 점진적으로 현재의 형태에 가까워졌다.

### 1.0 릴리스 (2015년 5월 29일)

약 2년간의 논의와 RC(Release Candidate) 단계를 거쳐 2015년 5월 29일에 JSON:API 1.0이 정식 릴리스되었다. 이 버전은 다음과 같은 주요 결정사항을 확정했다.

- `application/vnd.api+json` 미디어 타입이 IANA에 등록되었다
- Top-level document 구조(`data`, `errors`, `meta`)가 확정되었다
- Resource Object의 `attributes`와 `relationships` 분리가 확정되었다
- Compound Documents를 통한 사이드로딩(sideloading) 메커니즘이 명세화되었다
- Sparse Fieldsets, Sorting, Pagination, Filtering에 대한 규약이 포함되었다
- 에러 객체의 상세 구조가 정의되었다

1.0 릴리스 이후 JSON:API는 Ember.js 생태계를 넘어 다양한 언어와 프레임워크에서 채택되기 시작했다.

### 1.0에서 1.1로의 발전 (2015-2022)

1.0 이후 명세의 안정성이 확보되면서, 커뮤니티는 기존 명세의 제한사항을 보완하는 작업에 착수했다. 몇 가지 핵심 확장 요구사항이 있었다.

- 클라이언트 생성 ID(client-generated IDs) 에 대한 보다 유연한 지원
- 원자적 오퍼레이션(Atomic Operations) 을 통한 배치 처리
- 프로필(Profiles) 을 통한 명세 확장 메커니즘

### 1.1 릴리스 (2022)

JSON:API 1.1은 1.0과의 하위 호환성을 유지하면서 다음과 같은 주요 기능을 추가했다.

- Atomic Operations 확장: 여러 리소스에 대한 생성, 수정, 삭제를 단일 요청으로 처리할 수 있는 공식 확장이다
- Profiles: 명세를 확장하는 공식 메커니즘으로, 특정 도메인에 맞는 추가 규약을 정의할 수 있다
- lid(Local ID): 클라이언트가 아직 서버에 존재하지 않는 리소스를 참조할 수 있도록 로컬 식별자를 도입했다
- 링크 객체 확장: `describedby`, `hreflang`, `type` 등 추가 메타데이터를 링크에 포함할 수 있다

### 타임라인 요약

```
2013.05   Yehuda Katz, JSON API 첫 드래프트 공개 (Ember.js 커뮤니티)
2013-2014 커뮤니티 피드백 기반으로 명세 발전
2015.05   JSON:API 1.0 정식 릴리스
2015      application/vnd.api+json IANA 등록
2015-2022 1.1을 위한 확장 논의 (Atomic Operations, Profiles 등)
2022      JSON:API 1.1 릴리스 (lid, Profiles, Atomic Operations)
```

---

## 3. 핵심 개념 및 구조

### 3.1 Content-Type

JSON:API는 고유한 미디어 타입을 사용한다.

```
Content-Type: application/vnd.api+json
```

이 미디어 타입은 IANA(Internet Assigned Numbers Authority) 에 공식 등록되어 있다. `vnd`는 "vendor"의 약자로, 벤더 특화 미디어 타입임을 나타낸다.

JSON:API 명세를 준수하는 클라이언트와 서버는 반드시 이 미디어 타입을 사용해야 한다. 구체적으로:

- 서버는 `Content-Type: application/vnd.api+json` 헤더와 함께 응답을 전송해야 한다
- 클라이언트는 `Accept: application/vnd.api+json` 헤더를 포함하여 요청해야 한다
- 클라이언트가 JSON:API 본문을 전송할 때는 `Content-Type: application/vnd.api+json` 헤더를 사용해야 한다

JSON:API 1.1에서는 미디어 타입에 프로필(profile) 파라미터를 추가할 수 있게 되었다.

```
Content-Type: application/vnd.api+json; profile="https://example.com/profiles/atomic-operations"
```

주의사항: JSON:API 미디어 타입에 프로필 외의 미디어 타입 파라미터를 포함해서는 안 된다. 예를 들어, `application/vnd.api+json; charset=utf-8`과 같은 형태는 명세 위반이다(JSON은 기본적으로 UTF-8이므로 불필요하다).

### 3.2 Top-Level Document 구조

모든 JSON:API 응답은 Top-Level Document라 불리는 최상위 JSON 객체를 포함한다. 이 객체에는 다음 멤버가 포함될 수 있다.

```json
{
  "data": "...",
  "errors": "...",
  "meta": "...",
  "jsonapi": "...",
  "links": "...",
  "included": "..."
}
```

각 멤버의 역할은 다음과 같다.

| 멤버 | 필수 여부 | 설명 |
|------|-----------|------|
| data | 조건부 필수 | 문서의 "주 데이터(primary data)". 단일 리소스 객체, 리소스 객체 배열, 또는 `null` |
| errors | 조건부 필수 | 에러 객체의 배열. `data`와 동시에 존재할 수 없다 |
| meta | 선택 | 표준 규격에 포함되지 않는 추가 메타 정보 |
| jsonapi | 선택 | 서버가 구현한 JSON:API 버전 및 메타 정보 |
| links | 선택 | 문서 자체와 관련된 링크 (`self`, `related`, `next`, `prev` 등) |
| included | 선택 | `data`에 포함된 리소스와 관련된 사이드로딩 리소스 배열 |

핵심 규칙: `data`와 `errors`는 동일 문서에 동시에 존재할 수 없다. 성공 응답에는 `data`가, 에러 응답에는 `errors`가 포함된다. `meta`만 단독으로 존재하는 것은 허용된다.

최소 요구사항: Top-Level Document는 최소한 `data`, `errors`, `meta` 중 하나를 반드시 포함해야 한다.

성공적인 단일 리소스 조회 응답의 예시는 다음과 같다.

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "data": {
    "type": "articles",
    "id": "1",
    "attributes": {
      "title": "JSON:API 완벽 가이드"
    },
    "relationships": {
      "author": {
        "data": { "type": "people", "id": "42" }
      }
    },
    "links": {
      "self": "/articles/1"
    }
  },
  "links": {
    "self": "/articles/1"
  }
}
```

### 3.3 Resource Objects

Resource Object는 JSON:API의 핵심 단위이다. 모든 리소스는 반드시 `type`과 `id`를 가져야 하며, 선택적으로 `attributes`, `relationships`, `links`, `meta`를 포함할 수 있다.

```json
{
  "type": "articles",
  "id": "1",
  "attributes": {
    "title": "JSON:API paints my bikeshed!",
    "body": "The shortest article. Ever.",
    "created": "2015-05-22T14:56:29.000Z",
    "updated": "2015-05-22T14:56:28.000Z"
  },
  "relationships": {
    "author": {
      "links": {
        "self": "/articles/1/relationships/author",
        "related": "/articles/1/author"
      },
      "data": { "type": "people", "id": "42" }
    },
    "comments": {
      "links": {
        "self": "/articles/1/relationships/comments",
        "related": "/articles/1/comments"
      },
      "data": [
        { "type": "comments", "id": "5" },
        { "type": "comments", "id": "12" }
      ]
    }
  },
  "links": {
    "self": "/articles/1"
  },
  "meta": {
    "copyright": "Copyright 2015 Example Corp.",
    "revision": 3
  }
}
```

Resource Object의 멤버 규칙:

| 멤버 | 필수 여부 | 설명 |
|------|-----------|------|
| type | 필수 | 리소스의 유형을 나타내는 문자열 |
| id | 필수* | 리소스의 고유 식별자 문자열 (*서버 생성 시 POST 요청에서 생략 가능) |
| lid | 선택 (1.1) | 클라이언트가 부여하는 로컬 식별자 |
| attributes | 선택 | 리소스의 속성을 담는 객체 |
| relationships | 선택 | 다른 리소스와의 관계를 담는 객체 |
| links | 선택 | 리소스와 관련된 링크 |
| meta | 선택 | 리소스와 관련된 메타 정보 |

중요한 제약사항:
- `type`과 `id`의 조합은 전체 API 내에서 고유(unique) 해야 한다
- `id`는 반드시 문자열(string) 이어야 한다. 숫자 타입이 아님에 주의해야 한다
- `attributes`와 `relationships` 내의 키(key)는 서로 겹쳐서는 안 된다
- `type`, `id`, `lid`라는 이름은 `attributes`나 `relationships`의 키로 사용할 수 없다

### 3.4 Relationships

관계(relationship)는 리소스 간의 연결을 표현한다. JSON:API는 두 가지 관계 유형을 지원한다.

#### To-One Relationship

대상 리소스가 0개 또는 1개인 관계이다. `data` 멤버는 단일 Resource Identifier Object 또는 `null`이다.

```json
{
  "author": {
    "links": {
      "self": "/articles/1/relationships/author",
      "related": "/articles/1/author"
    },
    "data": { "type": "people", "id": "42" }
  }
}
```

관계가 비어있는 경우:

```json
{
  "author": {
    "links": {
      "self": "/articles/1/relationships/author",
      "related": "/articles/1/author"
    },
    "data": null
  }
}
```

#### To-Many Relationship

대상 리소스가 0개 이상인 관계이다. `data` 멤버는 Resource Identifier Object의 배열이다.

```json
{
  "comments": {
    "links": {
      "self": "/articles/1/relationships/comments",
      "related": "/articles/1/comments"
    },
    "data": [
      { "type": "comments", "id": "5" },
      { "type": "comments", "id": "12" }
    ]
  }
}
```

빈 to-many 관계:

```json
{
  "tags": {
    "data": []
  }
}
```

#### Resource Identifier Object

관계의 `data` 멤버에 사용되는 객체로, 리소스의 전체 데이터가 아닌 식별 정보만 포함한다.

```json
{ "type": "people", "id": "42" }
```

JSON:API 1.1에서는 `lid`를 사용할 수 있다.

```json
{ "type": "people", "lid": "temp-author-1" }
```

#### Relationship Links

각 관계 객체는 다음 두 종류의 링크를 가질 수 있다.

| 링크 | 설명 |
|------|------|
| self | 관계 자체를 나타내는 URL. 이 URL로 관계를 조회, 수정, 삭제할 수 있다 |
| related | 관련 리소스(들)의 전체 데이터를 조회할 수 있는 URL |

`self` 링크(`/articles/1/relationships/author`)로 요청하면 Resource Identifier Object(들)가 반환되고, `related` 링크(`/articles/1/author`)로 요청하면 전체 Resource Object(들)가 반환된다.

### 3.5 Compound Documents

Compound Document는 관련 리소스를 별도 요청 없이 단일 응답에 포함하는 메커니즘이다. 이를 사이드로딩(sideloading) 이라고도 한다. N+1 요청 문제를 해결하는 핵심 기능이다.

클라이언트는 `include` 쿼리 파라미터를 사용하여 사이드로딩할 관계를 지정한다.

```http
GET /articles/1?include=author,comments HTTP/1.1
Accept: application/vnd.api+json
```

서버 응답:

```json
{
  "data": {
    "type": "articles",
    "id": "1",
    "attributes": {
      "title": "JSON:API paints my bikeshed!"
    },
    "relationships": {
      "author": {
        "data": { "type": "people", "id": "42" }
      },
      "comments": {
        "data": [
          { "type": "comments", "id": "5" },
          { "type": "comments", "id": "12" }
        ]
      }
    }
  },
  "included": [
    {
      "type": "people",
      "id": "42",
      "attributes": {
        "name": "Yehuda Katz"
      }
    },
    {
      "type": "comments",
      "id": "5",
      "attributes": {
        "body": "First!"
      }
    },
    {
      "type": "comments",
      "id": "12",
      "attributes": {
        "body": "Great article!"
      }
    }
  ]
}
```

중첩 관계(nested relationships) 도 지원된다. 점(`.`) 표기법을 사용한다.

```http
GET /articles/1?include=comments.author HTTP/1.1
```

이 요청은 게시글의 댓글과, 각 댓글의 작성자를 모두 사이드로딩한다.

핵심 규칙:
- `included` 배열의 각 리소스는 `data`의 관계에서 참조된 것이어야 한다 (Full Linkage 규칙)
- `included` 배열 내에서 동일한 `type`/`id` 조합의 리소스가 중복되어서는 안 된다
- `included`는 `data`가 존재할 때만 사용할 수 있다

### 3.6 Sparse Fieldsets

Sparse Fieldsets는 클라이언트가 각 리소스 타입에서 필요한 필드만 선택적으로 요청하는 기능이다. 불필요한 데이터 전송을 줄여 네트워크 효율성을 높인다.

```http
GET /articles?fields[articles]=title,body&fields[people]=name HTTP/1.1
Accept: application/vnd.api+json
```

위 요청은 다음을 의미한다:
- `articles` 타입에서는 `title`과 `body` 필드만 반환한다
- `people` 타입에서는 `name` 필드만 반환한다 (사이드로딩된 경우)

쿼리 파라미터 형식은 `fields[TYPE]=field1,field2`이다.

주의사항:
- Sparse Fieldsets를 지정하지 않으면 서버는 해당 리소스의 모든 필드를 반환해야 한다
- `type`과 `id`는 항상 포함된다 (Sparse Fieldsets의 대상이 아니다)
- Sparse Fieldsets에 나열되지 않은 필드는 응답에서 제외되어야 한다
- 이 기능은 서버가 지원할 수도 있고(MAY) 지원하지 않을 수도 있다

### 3.7 Sorting

정렬은 `sort` 쿼리 파라미터를 통해 수행된다.

```http
GET /articles?sort=created,-updated HTTP/1.1
Accept: application/vnd.api+json
```

정렬 규칙:
- 필드명 앞에 하이픈(`-`) 을 붙이면 내림차순(descending) 이다
- 하이픈이 없으면 오름차순(ascending) 이다
- 쉼표로 구분하여 다중 정렬 기준을 지정할 수 있다
- 앞에 명시된 기준이 우선 적용된다

위 예시는 "created 오름차순으로 정렬하되, created가 동일한 경우 updated 내림차순으로 정렬"을 의미한다.

관계의 필드로 정렬하는 방법은 명세에서 정의하지 않으며, 구현에 따라 점 표기법(`sort=author.name`)이 사용되기도 한다.

### 3.8 Pagination

JSON:API는 페이지네이션의 구체적인 전략을 강제하지 않지만, `page` 쿼리 파라미터 계열을 사용하도록 규정한다. 구체적인 파라미터 이름은 구현에 따라 다를 수 있으나, 일반적으로 다음 두 가지 방식이 널리 사용된다.

#### 페이지 번호 기반 (Page-based)

```http
GET /articles?page[number]=3&page[size]=10 HTTP/1.1
```

#### 커서 기반 (Cursor-based)

```http
GET /articles?page[cursor]=eyJpZCI6MTB9&page[size]=10 HTTP/1.1
```

#### 오프셋 기반 (Offset-based)

```http
GET /articles?page[offset]=20&page[limit]=10 HTTP/1.1
```

페이지네이션 응답에서 가장 중요한 것은 Top-Level `links` 객체이다.

```json
{
  "data": [ "..." ],
  "links": {
    "self": "/articles?page[number]=3&page[size]=10",
    "first": "/articles?page[number]=1&page[size]=10",
    "prev": "/articles?page[number]=2&page[size]=10",
    "next": "/articles?page[number]=4&page[size]=10",
    "last": "/articles?page[number]=13&page[size]=10"
  },
  "meta": {
    "totalPages": 13,
    "totalCount": 125
  }
}
```

페이지네이션 링크의 종류:

| 링크 | 설명 |
|------|------|
| first | 첫 번째 페이지 |
| last | 마지막 페이지 |
| prev | 이전 페이지 (첫 페이지에서는 `null`) |
| next | 다음 페이지 (마지막 페이지에서는 `null`) |

### 3.9 Filtering

필터링은 `filter` 쿼리 파라미터를 통해 수행된다. JSON:API 명세는 필터링의 구체적인 전략을 의도적으로 정의하지 않으며, 서버 구현에 위임한다. 다만 `filter` 키워드는 예약되어 있다.

일반적으로 사용되는 패턴:

```http
GET /articles?filter[author]=42 HTTP/1.1
GET /articles?filter[status]=published&filter[category]=tech HTTP/1.1
GET /articles?filter[created][gte]=2024-01-01 HTTP/1.1
```

일부 구현에서는 보다 정교한 필터 표현식을 지원하기도 한다.

```http
GET /articles?filter[title][contains]=JSON HTTP/1.1
GET /articles?filter[views][gt]=1000 HTTP/1.1
```

필터, 정렬, 페이지네이션, Sparse Fieldsets를 결합하여 사용할 수 있다.

```http
GET /articles?filter[status]=published&sort=-created&page[number]=1&page[size]=5&fields[articles]=title,created&include=author&fields[people]=name HTTP/1.1
Accept: application/vnd.api+json
```

이 요청은 "발행 상태인 게시글을 최신순으로 정렬하여 첫 페이지의 5개를 가져오되, 게시글에서는 제목과 생성일만, 작성자에서는 이름만 포함하며, 작성자 데이터를 사이드로딩"하라는 의미이다.

---

## 4. 버전별 차이

### 초기 드래프트 vs 1.0 vs 1.1 비교

| 특성 | 초기 드래프트 (2013) | 1.0 (2015) | 1.1 (2022) |
|------|---------------------|------------|------------|
| 미디어 타입 | 미정 | `application/vnd.api+json` | `application/vnd.api+json` (profile 파라미터 추가) |
| 리소스 구조 | `type`, `id`와 속성이 혼재 | `attributes`, `relationships` 명확 분리 | 1.0과 동일 |
| 관계 표현 | 단순 ID 배열 또는 링크 | Resource Identifier Object + links | 1.0과 동일 + `lid` 지원 |
| Compound Documents | 기본 개념 존재 | `included` 배열, Full Linkage 규칙 확정 | 1.0과 동일 |
| 에러 객체 | 비정형 | `id`, `status`, `title`, `detail`, `source`, `code` 등 확정 | 1.0과 동일 |
| Sparse Fieldsets | 미지원 | `fields[type]` 파라미터 | 1.0과 동일 |
| 정렬 | 미정 | `sort` 파라미터 확정 | 1.0과 동일 |
| Local ID (lid) | 미지원 | 미지원 | `lid` 도입 |
| Atomic Operations | 미지원 | 미지원 | 공식 확장으로 추가 |
| Profiles | 미지원 | 미지원 | 명세 확장 메커니즘 도입 |
| 링크 객체 확장 | 단순 URL 문자열 | URL 문자열 또는 링크 객체(`href`, `meta`) | `describedby`, `hreflang`, `type` 등 추가 |

### 1.0에서 1.1로의 주요 변경 상세

#### Atomic Operations

1.0에서는 여러 리소스를 동시에 생성하거나, 생성과 관계 설정을 하나의 요청으로 처리하는 것이 불가능했다. 1.1의 Atomic Operations 확장은 이 문제를 해결한다.

```json
{
  "atomic:operations": [
    {
      "op": "add",
      "data": {
        "type": "articles",
        "lid": "temp-article-1",
        "attributes": {
          "title": "새 게시글"
        }
      }
    },
    {
      "op": "update",
      "data": {
        "type": "articles",
        "id": "10",
        "attributes": {
          "title": "수정된 제목"
        }
      }
    },
    {
      "op": "remove",
      "ref": {
        "type": "articles",
        "id": "5"
      }
    }
  ]
}
```

Atomic Operations의 핵심 특성:
- 원자성(Atomicity): 모든 오퍼레이션이 성공하거나, 모두 실패한다. 부분 적용은 없다
- 순차 실행: 오퍼레이션은 배열 순서대로 실행된다
- lid 참조: `lid`를 통해 앞선 오퍼레이션에서 생성된 리소스를 참조할 수 있다

#### Profiles

Profile은 JSON:API 명세를 확장하는 공식 메커니즘이다. URI로 식별되며, 미디어 타입 파라미터로 전달된다.

```http
Content-Type: application/vnd.api+json; profile="https://jsonapi.org/ext/atomic https://example.com/profiles/custom"
```

Profile의 역할:
- 기존 명세의 의미를 구체화하거나 제한할 수 있다
- 새로운 쿼리 파라미터를 정의할 수 있다
- 새로운 문서 멤버를 추가할 수 있다
- 기존 멤버의 의미를 재정의할 수는 없다

#### lid (Local ID)

`lid`는 클라이언트가 아직 서버에 존재하지 않는 리소스에 임시 식별자를 부여하는 메커니즘이다. 주로 Atomic Operations에서 사용된다.

```json
{
  "atomic:operations": [
    {
      "op": "add",
      "data": {
        "type": "people",
        "lid": "temp-author",
        "attributes": { "name": "새 작성자" }
      }
    },
    {
      "op": "add",
      "data": {
        "type": "articles",
        "lid": "temp-article",
        "attributes": { "title": "새 게시글" },
        "relationships": {
          "author": {
            "data": { "type": "people", "lid": "temp-author" }
          }
        }
      }
    }
  ]
}
```

위 예시에서 두 번째 오퍼레이션은 첫 번째 오퍼레이션에서 생성될 `people` 리소스를 `lid`를 통해 참조한다. 서버가 실제 `id`를 부여하기 전에 클라이언트 측에서 관계를 설정할 수 있게 해준다.

---

## 5. 주요 키워드 및 필드 설명

### 5.1 type

`type` 은 리소스의 유형을 나타내는 문자열이다. API 전체에서 동일한 구조를 공유하는 리소스들의 집합을 식별한다.

```json
{ "type": "articles", "id": "1" }
```

- 반드시 문자열(string) 이어야 한다
- 일반적으로 복수형 명사를 사용한다 (`articles`, `people`, `comments`)
- 동일 `type`의 리소스는 동일한 `attributes`와 `relationships` 구조를 공유해야 한다
- `type`은 사실상 REST API의 리소스 컬렉션 이름에 대응한다

명세에서는 `type`의 명명 규칙을 강제하지 않지만, 다음 관례가 널리 사용된다:
- 케밥 케이스: `blog-posts`, `user-profiles`
- 카멜 케이스: `blogPosts`, `userProfiles`
- 단순 복수형: `articles`, `comments`

### 5.2 id

`id` 는 주어진 `type` 내에서 리소스를 고유하게 식별하는 문자열이다.

```json
{ "type": "articles", "id": "550e8400-e29b-41d4-a716-446655440000" }
```

- 반드시 문자열(string) 이어야 한다 (숫자 `1`이 아닌 문자열 `"1"`)
- `type`과 `id`의 조합은 API 전체에서 유일(unique) 해야 한다
- 서버가 생성하는 것이 기본이다
- POST 요청에서 클라이언트가 `id`를 지정할 수도 있다 (서버가 허용하는 경우)
- UUID, 자동 증가 정수의 문자열 표현, slug 등이 사용된다

### 5.3 lid (Local ID) - JSON:API 1.1

`lid` 는 클라이언트가 부여하는 로컬 식별자이다.

```json
{ "type": "articles", "lid": "my-new-article" }
```

- 서버에 전송되는 요청 문서 내에서만 유효하다
- 서버의 응답에는 포함되지 않는다 (서버가 실제 `id`를 부여한다)
- `id`와 `lid`가 동시에 존재할 수 없다
- 주로 Atomic Operations에서 아직 생성되지 않은 리소스를 참조할 때 사용한다
- 동일 요청 내에서 동일 `type`/`lid` 조합은 동일한 리소스를 가리킨다

### 5.4 attributes

`attributes` 는 리소스의 속성 데이터를 담는 객체이다.

```json
{
  "attributes": {
    "title": "JSON:API 완벽 가이드",
    "body": "본문 내용...",
    "created-at": "2024-01-15T09:30:00Z",
    "word-count": 5000,
    "is-published": true,
    "tags": ["api", "json", "rest"]
  }
}
```

- `attributes` 내의 값은 어떤 JSON 타입이든 가능하다 (문자열, 숫자, 불리언, 배열, 중첩 객체, null)
- 관계(relationship)를 표현하는 외래 키(foreign key)는 `attributes`에 포함해서는 안 된다. 관계는 반드시 `relationships`에서 표현해야 한다
- `type`, `id`, `lid`라는 이름은 `attributes`의 키로 사용할 수 없다
- `relationships`의 키와 중복되어서는 안 된다

올바른 예시 vs 잘못된 예시:

```json
// 올바른 예시: 외래 키 대신 relationship 사용
{
  "type": "articles",
  "id": "1",
  "attributes": {
    "title": "제목"
  },
  "relationships": {
    "author": {
      "data": { "type": "people", "id": "42" }
    }
  }
}
```

```json
// 잘못된 예시: 외래 키를 attributes에 포함
{
  "type": "articles",
  "id": "1",
  "attributes": {
    "title": "제목",
    "author-id": 42
  }
}
```

### 5.5 relationships

`relationships` 는 리소스와 다른 리소스 간의 관계를 표현하는 객체이다.

```json
{
  "relationships": {
    "author": {
      "links": {
        "self": "/articles/1/relationships/author",
        "related": "/articles/1/author"
      },
      "data": { "type": "people", "id": "42" },
      "meta": {
        "assignedAt": "2024-01-15T09:30:00Z"
      }
    }
  }
}
```

각 관계 객체는 다음 멤버를 포함할 수 있다:

| 멤버 | 설명 |
|------|------|
| links | `self`(관계 자체의 URL)와 `related`(관련 리소스의 URL) |
| data | 관계의 연결 정보(linkage). Resource Identifier Object 또는 그 배열 |
| meta | 관계와 관련된 메타 정보 |

관계 객체는 최소한 `links`, `data`, `meta` 중 하나를 반드시 포함해야 한다.

### 5.6 links

`links` 는 리소스, 관계, 또는 문서 수준에서 관련 URL을 제공하는 객체이다.

```json
{
  "links": {
    "self": "https://api.example.com/articles/1",
    "related": {
      "href": "https://api.example.com/articles/1/comments",
      "meta": {
        "count": 42
      }
    }
  }
}
```

링크 값은 다음 두 형태 중 하나일 수 있다:
- 문자열: URL을 직접 나타낸다
- 링크 객체: `href`(URL)와 선택적 `meta`를 포함하는 객체

#### self 링크

리소스 자체를 가리키는 URL이다. 이 URL로 GET 요청을 보내면 해당 리소스를 조회할 수 있다.

```json
{ "self": "/articles/1" }
```

#### related 링크

관계에서 사용되며, 관련 리소스의 전체 데이터를 조회할 수 있는 URL이다.

```json
{ "related": "/articles/1/author" }
```

`/articles/1/relationships/author`(self)는 Resource Identifier만 반환하지만, `/articles/1/author`(related)는 전체 Resource Object를 반환한다.

#### JSON:API 1.1에서 추가된 링크 멤버

| 멤버 | 설명 |
|------|------|
| describedby | 리소스 또는 관계를 기술하는 문서(JSON Schema 등)의 URL |
| hreflang | 링크 대상의 언어 |
| type | 링크 대상의 미디어 타입 |
| title | 링크에 대한 사람이 읽을 수 있는 레이블 |

```json
{
  "links": {
    "self": {
      "href": "/articles/1",
      "title": "JSON:API 완벽 가이드",
      "type": "application/vnd.api+json",
      "hreflang": "ko",
      "describedby": "/schemas/articles"
    }
  }
}
```

### 5.7 meta

`meta` 는 JSON:API 명세에서 정의하지 않는 추가 정보를 담는 자유 형식 객체이다. 리소스, 관계, 링크, 에러, Top-Level Document 등 거의 모든 곳에서 사용할 수 있다.

```json
{
  "meta": {
    "copyright": "Copyright 2024 Example Corp.",
    "authors": ["Yehuda Katz"],
    "totalCount": 125,
    "generatedAt": "2024-01-15T09:30:00Z",
    "requestId": "abc-123-def-456"
  }
}
```

`meta`의 일반적인 활용 사례:
- 총 레코드 수 (`totalCount`)
- 저작권 정보
- 요청 추적 ID
- 생성 시각
- 서버 버전 정보
- 비즈니스 로직 관련 추가 데이터

### 5.8 included

`included` 는 Compound Document에서 사이드로딩된 리소스의 배열이다.

```json
{
  "data": {
    "type": "articles",
    "id": "1",
    "relationships": {
      "author": { "data": { "type": "people", "id": "42" } }
    }
  },
  "included": [
    {
      "type": "people",
      "id": "42",
      "attributes": {
        "name": "Yehuda Katz"
      }
    }
  ]
}
```

핵심 규칙:
- `included` 배열의 모든 리소스는 `data`의 관계에서 직접적 또는 간접적으로 참조되어야 한다 (Full Linkage)
- 동일 `type`/`id` 조합의 리소스는 `included` 배열에 한 번만 포함되어야 한다
- `data`가 없으면 `included`도 존재할 수 없다

### 5.9 jsonapi 객체

`jsonapi` 는 서버가 구현한 JSON:API 버전 및 관련 메타 정보를 나타내는 객체이다.

```json
{
  "jsonapi": {
    "version": "1.1",
    "ext": [
      "https://jsonapi.org/ext/atomic"
    ],
    "profile": [
      "https://example.com/profiles/custom"
    ],
    "meta": {
      "implementation": "jsonapi-resources 6.0"
    }
  }
}
```

| 멤버 | 설명 |
|------|------|
| version | 서버가 지원하는 JSON:API 최상위 버전 (예: `"1.1"`) |
| ext | 적용된 확장(extensions)의 URI 배열 (1.1) |
| profile | 적용된 프로필(profiles)의 URI 배열 (1.1) |
| meta | 추가 메타 정보 |

### 5.10 에러 객체 (Error Objects)

에러 응답은 `errors` 배열에 하나 이상의 에러 객체를 포함한다.

```json
{
  "errors": [
    {
      "id": "err-001",
      "status": "422",
      "code": "VALIDATION_ERROR",
      "title": "유효성 검사 실패",
      "detail": "'title' 필드는 비어 있을 수 없습니다.",
      "source": {
        "pointer": "/data/attributes/title"
      },
      "links": {
        "about": "https://api.example.com/docs/errors/VALIDATION_ERROR"
      },
      "meta": {
        "field": "title",
        "constraint": "not_blank"
      }
    }
  ]
}
```

에러 객체의 멤버:

| 멤버 | 설명 |
|------|------|
| id | 이 특정 에러 발생의 고유 식별자 |
| links | `about`(에러에 대한 추가 정보 URL), `type`(에러 유형 URL) |
| status | 이 에러에 적용되는 HTTP 상태 코드 (문자열) |
| code | 애플리케이션 고유의 에러 코드 (문자열) |
| title | 에러의 일반적인 요약. 동일 문제에 대해 변하지 않아야 한다 |
| detail | 이 특정 에러 발생에 대한 구체적인 설명 |
| source | 에러의 원인을 나타내는 객체 |
| meta | 에러와 관련된 추가 메타 정보 |

`source` 객체의 멤버:

| 멤버 | 설명 |
|------|------|
| pointer | 요청 문서에서 에러를 유발한 위치를 가리키는 JSON Pointer (RFC 6901) |
| parameter | 에러를 유발한 쿼리 파라미터의 이름 |
| header | 에러를 유발한 HTTP 헤더의 이름 |

---

## 6. 사용 사례 및 활용 도구

### 6.1 OpenAPI Specification(OAS)과 함께 사용

JSON:API는 API 응답의 형식(format) 을 규정하는 명세이고, OpenAPI Specification(OAS)은 API의 인터페이스(interface) 를 기술하는 명세이다. 두 명세는 서로 다른 계층의 문제를 해결하므로 상호 보완적으로 함께 사용할 수 있다.

OAS에서 JSON:API 응답 구조를 기술하는 예시:

```json
{
  "paths": {
    "/articles": {
      "get": {
        "summary": "게시글 목록 조회",
        "parameters": [
          {
            "name": "page[number]",
            "in": "query",
            "schema": { "type": "integer", "default": 1 }
          },
          {
            "name": "page[size]",
            "in": "query",
            "schema": { "type": "integer", "default": 10 }
          },
          {
            "name": "include",
            "in": "query",
            "schema": { "type": "string" }
          },
          {
            "name": "sort",
            "in": "query",
            "schema": { "type": "string" }
          }
        ],
        "responses": {
          "200": {
            "description": "성공",
            "content": {
              "application/vnd.api+json": {
                "schema": {
                  "$ref": "#/components/schemas/ArticleListResponse"
                }
              }
            }
          }
        }
      }
    }
  }
}
```

### 6.2 서버 라이브러리

#### jsonapi-resources (Ruby)

jsonapi-resources는 Ruby on Rails 애플리케이션에서 JSON:API 명세를 구현하는 가장 대표적인 라이브러리이다. Rails의 관례(convention)를 최대한 활용하며, 모델 정의만으로 JSON:API 호환 엔드포인트를 자동 생성한다.

```ruby
# app/resources/article_resource.rb
class ArticleResource < JSONAPI::Resource
  attributes :title, :body, :created_at
  has_one :author
  has_many :comments

  filter :status
  sort :title, :created_at
end
```

주요 기능:
- 자동 직렬화/역직렬화
- 관계 자동 처리
- Sparse Fieldsets, Sorting, Filtering, Pagination 자동 지원
- include 파라미터 자동 처리

#### jsonapi-serializer (Ruby)

jsonapi-serializer(구 fast_jsonapi, Netflix에서 개발)는 JSON:API 형식의 직렬화에 특화된 경량 라이브러리이다. 컨트롤러 로직은 개발자가 직접 작성하되, 직렬화만 JSON:API 형식으로 수행한다.

```ruby
class ArticleSerializer
  include JSONAPI::Serializer

  attributes :title, :body, :created_at
  belongs_to :author
  has_many :comments

  link :self do |article|
    "/articles/#{article.id}"
  end
end
```

#### Django REST Framework JSON:API (Python)

djangorestframework-jsonapi는 Django REST Framework(DRF) 위에서 JSON:API 명세를 구현하는 라이브러리이다. 기존 DRF의 Serializer, ViewSet, Router를 그대로 활용하면서 응답 형식만 JSON:API로 변환한다.

```python
# settings.py
REST_FRAMEWORK = {
    'DEFAULT_RENDERER_CLASSES': (
        'rest_framework_json_api.renderers.JSONRenderer',
    ),
    'DEFAULT_PARSER_CLASSES': (
        'rest_framework_json_api.parsers.JSONParser',
    ),
    'DEFAULT_METADATA_CLASS': 'rest_framework_json_api.metadata.JSONAPIMetadata',
    'DEFAULT_FILTER_BACKENDS': (
        'rest_framework_json_api.filters.QueryParameterValidationFilter',
        'rest_framework_json_api.filters.OrderingFilter',
        'rest_framework_json_api.django_filters.DjangoFilterBackend',
        'rest_framework.filters.SearchFilter',
    ),
    'DEFAULT_PAGINATION_CLASS': 'rest_framework_json_api.pagination.JsonApiPageNumberPagination',
    'PAGE_SIZE': 10,
}
```

```python
# serializers.py
from rest_framework_json_api import serializers

class ArticleSerializer(serializers.ModelSerializer):
    included_serializers = {
        'author': 'myapp.serializers.PersonSerializer',
        'comments': 'myapp.serializers.CommentSerializer',
    }

    class Meta:
        model = Article
        fields = ['title', 'body', 'created_at', 'author', 'comments']
```

#### Spring Boot JSON:API (Java)

Java/Spring Boot 생태계에서는 여러 라이브러리가 JSON:API를 지원한다.

crnk-framework는 JSON:API를 위한 포괄적인 Java 프레임워크이다.

```java
@JsonApiResource(type = "articles")
public class Article {
    @JsonApiId
    private Long id;

    private String title;
    private String body;
    private LocalDateTime createdAt;

    @JsonApiRelation
    private Person author;

    @JsonApiRelation
    private List<Comment> comments;
}
```

spring-hateoas-jsonapi는 Spring HATEOAS 위에서 JSON:API 형식을 지원하는 라이브러리이다.

```java
@GetMapping("/articles/{id}")
public ResponseEntity<EntityModel<Article>> getArticle(@PathVariable Long id) {
    Article article = articleService.findById(id);
    EntityModel<Article> model = EntityModel.of(article);
    model.add(linkTo(methodOn(ArticleController.class).getArticle(id)).withSelfRel());
    return ResponseEntity.ok(model);
}
```

### 6.3 클라이언트 라이브러리

#### Spraypaint.js

Spraypaint.js는 JavaScript/TypeScript에서 JSON:API 서버와 상호작용하기 위한 ORM 스타일 클라이언트 라이브러리이다. ActiveRecord 패턴에 영감을 받아 설계되었다.

```javascript
import { SpraypaintBase, Model, Attr, HasMany, BelongsTo } from "spraypaint"

@Model()
class Article extends SpraypaintBase {
  static jsonapiType = "articles"

  @Attr() title: string
  @Attr() body: string
  @Attr() createdAt: string

  @BelongsTo() author: Person
  @HasMany() comments: Comment[]
}

// 조회
const articles = await Article
  .where({ status: "published" })
  .order({ created_at: "desc" })
  .per(10)
  .page(1)
  .includes(["author", "comments"])
  .select({ articles: ["title", "created_at"], people: ["name"] })
  .all()

// 생성
const article = new Article({ title: "새 게시글" })
article.author = existingAuthor
await article.save()
```

#### jsonapi-typescript

jsonapi-typescript는 JSON:API 문서의 TypeScript 타입 정의를 제공하는 라이브러리이다. 런타임 코드는 없으며, 순수 타입 정의만 제공한다.

```typescript
import {
  ResourceObject,
  DocWithData,
  RelationshipsWithData,
  ResourceIdentifierObject
} from "jsonapi-typescript"

type ArticleResource = ResourceObject<
  "articles",
  {
    title: string
    body: string
    createdAt: string
  }
>

type ArticleDocument = DocWithData<ArticleResource>
```

#### Ember Data

Ember Data는 JSON:API의 탄생 배경이 된 라이브러리이다. Ember.js 프레임워크의 공식 데이터 레이어로, JSON:API를 기본 직렬화 형식으로 사용한다.

```javascript
// app/models/article.js
import Model, { attr, belongsTo, hasMany } from '@ember-data/model';

export default class ArticleModel extends Model {
  @attr('string') title;
  @attr('string') body;
  @attr('date') createdAt;

  @belongsTo('person') author;
  @hasMany('comment') comments;
}

// 사용 예시
let articles = await this.store.query('article', {
  filter: { status: 'published' },
  sort: '-created-at',
  include: 'author,comments',
  page: { number: 1, size: 10 },
  fields: { articles: 'title,created-at', people: 'name' }
});
```

### 6.4 기타 도구

| 도구 | 언어/플랫폼 | 설명 |
|------|-------------|------|
| jsonapi-rb | Ruby | 경량 JSON:API 직렬화/역직렬화 라이브러리 |
| Elide | Java | JPA 기반 JSON:API + GraphQL 서버 프레임워크 |
| json-api-dotnet | .NET | ASP.NET Core용 JSON:API 구현체 |
| Bumi/jsonapi | Go | Go 언어용 JSON:API 직렬화/역직렬화 |
| jsonapi-fractal | Node.js | JSON:API 직렬화 라이브러리 |
| jsonapi-datastore | JavaScript | JSON:API 응답을 로컬 데이터 저장소로 관리 |
| jsonapi-validator | Node.js | JSON:API 문서 유효성 검증 도구 |

---

## 7. 장점과 단점

### 장점

| 장점 | 설명 |
|------|------|
| 일관된 응답 형식 | 모든 엔드포인트에서 동일한 구조를 사용하므로, 클라이언트 측에서 범용 파싱 로직을 구축할 수 있다. 새 리소스가 추가되어도 기존 로직을 재사용할 수 있다 |
| N+1 요청 문제 해결 | Compound Documents(`include`)를 통해 관련 리소스를 단일 요청으로 가져올 수 있어, REST API의 고질적인 N+1 요청 문제를 해결한다 |
| 효율적인 데이터 전송 | Sparse Fieldsets를 통해 필요한 필드만 요청할 수 있어 over-fetching을 방지한다. GraphQL의 필드 선택 기능과 유사한 효과를 REST에서 달성할 수 있다 |
| 표준화된 에러 형식 | 에러 객체의 구조가 명확히 정의되어 있어, 에러 처리 로직을 일관되게 구현할 수 있다. `source.pointer`를 통해 에러 발생 위치까지 정확히 식별할 수 있다 |
| 관계의 명확한 표현 | `relationships`를 통해 리소스 간의 관계를 명시적으로 표현한다. 링크 기반의 관계 탐색이 가능하여 HATEOAS 원칙을 자연스럽게 구현한다 |
| 발견 가능성 | `links` 객체를 통해 클라이언트가 사전 URL 구성 없이도 관련 리소스를 탐색할 수 있다. API 변경 시 클라이언트의 하드코딩된 URL이 깨질 위험이 줄어든다 |
| 풍부한 생태계 | 다양한 언어와 프레임워크에서 JSON:API 구현 라이브러리가 제공된다. 서버와 클라이언트 양쪽 모두 도구 지원이 풍부하다 |
| 바이크쉐딩 방지 | 응답 형식에 대한 논의를 명세에 위임함으로써, 팀의 의사결정 비용을 절감한다 |

### 단점

| 단점 | 설명 |
|------|------|
| 응답 크기 증가 | `type`, `id`, `attributes`, `relationships` 등의 메타 구조로 인해 단순 JSON 응답 대비 페이로드가 커진다. 단순한 리소스를 반환할 때에도 최소 구조가 필요하다 |
| 학습 곡선 | Resource Object, Resource Identifier Object, Compound Documents, Full Linkage 규칙 등 이해해야 할 개념이 많다. 특히 관계와 사이드로딩의 동작 방식은 초보자에게 직관적이지 않을 수 있다 |
| 유연성 제한 | 명세의 엄격한 규칙으로 인해, 특수한 비즈니스 요구사항을 구현하기 어려운 경우가 있다. 예를 들어, 비정형 응답이나 중첩 리소스 생성은 명세의 범위를 벗어날 수 있다 |
| 중첩 리소스 생성의 어려움 | 1.0에서는 부모와 자식 리소스를 한 번에 생성하는 것이 불가능했다. 1.1의 Atomic Operations로 개선되었으나, 구현이 복잡하다 |
| 필터링 규약 부재 | 필터링의 구체적인 문법이 명세에 정의되지 않아, 서버마다 필터 파라미터 형식이 다를 수 있다. 이는 클라이언트 라이브러리의 범용성을 저해한다 |
| 부분 채택의 어려움 | JSON:API는 "전부 아니면 전무(all or nothing)"에 가까운 명세이다. 일부만 채택하면 명세 준수 도구와의 호환성이 깨진다 |
| 과잉 엔지니어링 가능성 | 단순한 API(리소스 간 관계가 적고, 클라이언트가 한정된 경우)에서는 JSON:API의 구조가 불필요하게 복잡할 수 있다 |
| 클라이언트 측 정규화 필요 | Compound Documents를 사용할 경우, 클라이언트에서 `included` 배열의 리소스를 `data`의 관계와 매핑하는 정규화(normalization) 로직이 필요하다 |

---

## 8. 실제 예시

이 절에서는 블로그 API를 JSON:API 명세에 따라 설계하고, 주요 CRUD 오퍼레이션의 완전한 요청/응답 예시를 제시한다.

### 8.1 리소스 모델

블로그 API의 리소스 구조는 다음과 같다.

- articles: 블로그 게시글 (`title`, `body`, `status`, `created-at`, `updated-at`)
- people: 작성자 (`first-name`, `last-name`, `email`, `twitter`)
- comments: 댓글 (`body`, `created-at`)
- tags: 태그 (`name`, `slug`)

관계:
- article -> author (to-one, people)
- article -> comments (to-many)
- article -> tags (to-many)
- comment -> author (to-one, people)

### 8.2 GET - 게시글 목록 조회 (페이지네이션 포함)

```http
GET /articles?filter[status]=published&sort=-created-at&page[number]=1&page[size]=3&fields[articles]=title,status,created-at&include=author&fields[people]=first-name,last-name HTTP/1.1
Host: api.example.com
Accept: application/vnd.api+json
```

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "data": [
    {
      "type": "articles",
      "id": "15",
      "attributes": {
        "title": "JSON:API로 REST API 설계하기",
        "status": "published",
        "created-at": "2024-03-15T09:00:00Z"
      },
      "relationships": {
        "author": {
          "links": {
            "self": "/articles/15/relationships/author",
            "related": "/articles/15/author"
          },
          "data": { "type": "people", "id": "42" }
        }
      },
      "links": {
        "self": "/articles/15"
      }
    },
    {
      "type": "articles",
      "id": "14",
      "attributes": {
        "title": "RESTful API 모범 사례",
        "status": "published",
        "created-at": "2024-03-10T14:30:00Z"
      },
      "relationships": {
        "author": {
          "links": {
            "self": "/articles/14/relationships/author",
            "related": "/articles/14/author"
          },
          "data": { "type": "people", "id": "42" }
        }
      },
      "links": {
        "self": "/articles/14"
      }
    },
    {
      "type": "articles",
      "id": "12",
      "attributes": {
        "title": "OpenAPI 3.1과 JSON Schema",
        "status": "published",
        "created-at": "2024-03-05T08:15:00Z"
      },
      "relationships": {
        "author": {
          "links": {
            "self": "/articles/12/relationships/author",
            "related": "/articles/12/author"
          },
          "data": { "type": "people", "id": "7" }
        }
      },
      "links": {
        "self": "/articles/12"
      }
    }
  ],
  "included": [
    {
      "type": "people",
      "id": "42",
      "attributes": {
        "first-name": "Yehuda",
        "last-name": "Katz"
      },
      "links": {
        "self": "/people/42"
      }
    },
    {
      "type": "people",
      "id": "7",
      "attributes": {
        "first-name": "Dan",
        "last-name": "Gebhardt"
      },
      "links": {
        "self": "/people/7"
      }
    }
  ],
  "links": {
    "self": "/articles?filter[status]=published&sort=-created-at&page[number]=1&page[size]=3",
    "first": "/articles?filter[status]=published&sort=-created-at&page[number]=1&page[size]=3",
    "next": "/articles?filter[status]=published&sort=-created-at&page[number]=2&page[size]=3",
    "last": "/articles?filter[status]=published&sort=-created-at&page[number]=5&page[size]=3"
  },
  "meta": {
    "total-count": 15,
    "total-pages": 5
  }
}
```

### 8.3 GET - 단일 게시글 조회 (include 사용)

```http
GET /articles/15?include=author,comments.author,tags HTTP/1.1
Host: api.example.com
Accept: application/vnd.api+json
```

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "data": {
    "type": "articles",
    "id": "15",
    "attributes": {
      "title": "JSON:API로 REST API 설계하기",
      "body": "JSON:API는 클라이언트와 서버 간에 JSON 기반으로 리소스를 주고받는 방식을 표준화한 명세이다...",
      "status": "published",
      "created-at": "2024-03-15T09:00:00Z",
      "updated-at": "2024-03-15T10:30:00Z"
    },
    "relationships": {
      "author": {
        "links": {
          "self": "/articles/15/relationships/author",
          "related": "/articles/15/author"
        },
        "data": { "type": "people", "id": "42" }
      },
      "comments": {
        "links": {
          "self": "/articles/15/relationships/comments",
          "related": "/articles/15/comments"
        },
        "data": [
          { "type": "comments", "id": "101" },
          { "type": "comments", "id": "102" },
          { "type": "comments", "id": "103" }
        ]
      },
      "tags": {
        "links": {
          "self": "/articles/15/relationships/tags",
          "related": "/articles/15/tags"
        },
        "data": [
          { "type": "tags", "id": "1" },
          { "type": "tags", "id": "5" }
        ]
      }
    },
    "links": {
      "self": "/articles/15"
    },
    "meta": {
      "revision": 3,
      "word-count": 2500
    }
  },
  "included": [
    {
      "type": "people",
      "id": "42",
      "attributes": {
        "first-name": "Yehuda",
        "last-name": "Katz",
        "email": "yehuda@example.com",
        "twitter": "@wycats"
      },
      "links": {
        "self": "/people/42"
      }
    },
    {
      "type": "comments",
      "id": "101",
      "attributes": {
        "body": "정말 유용한 글입니다. 감사합니다!",
        "created-at": "2024-03-15T11:00:00Z"
      },
      "relationships": {
        "author": {
          "data": { "type": "people", "id": "7" }
        }
      },
      "links": {
        "self": "/comments/101"
      }
    },
    {
      "type": "comments",
      "id": "102",
      "attributes": {
        "body": "Compound Documents 부분이 특히 도움이 되었습니다.",
        "created-at": "2024-03-15T12:30:00Z"
      },
      "relationships": {
        "author": {
          "data": { "type": "people", "id": "18" }
        }
      },
      "links": {
        "self": "/comments/102"
      }
    },
    {
      "type": "comments",
      "id": "103",
      "attributes": {
        "body": "GraphQL과의 비교도 추가해주시면 좋겠습니다.",
        "created-at": "2024-03-16T08:00:00Z"
      },
      "relationships": {
        "author": {
          "data": { "type": "people", "id": "42" }
        }
      },
      "links": {
        "self": "/comments/103"
      }
    },
    {
      "type": "people",
      "id": "7",
      "attributes": {
        "first-name": "Dan",
        "last-name": "Gebhardt",
        "email": "dan@example.com",
        "twitter": "@dgeb"
      },
      "links": {
        "self": "/people/7"
      }
    },
    {
      "type": "people",
      "id": "18",
      "attributes": {
        "first-name": "민수",
        "last-name": "김",
        "email": "minsu@example.com",
        "twitter": null
      },
      "links": {
        "self": "/people/18"
      }
    },
    {
      "type": "tags",
      "id": "1",
      "attributes": {
        "name": "API 설계",
        "slug": "api-design"
      },
      "links": {
        "self": "/tags/1"
      }
    },
    {
      "type": "tags",
      "id": "5",
      "attributes": {
        "name": "JSON:API",
        "slug": "json-api"
      },
      "links": {
        "self": "/tags/5"
      }
    }
  ]
}
```

### 8.4 POST - 게시글 생성

```http
POST /articles HTTP/1.1
Host: api.example.com
Content-Type: application/vnd.api+json
Accept: application/vnd.api+json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

요청 본문:

```json
{
  "data": {
    "type": "articles",
    "attributes": {
      "title": "새로운 블로그 게시글",
      "body": "이것은 JSON:API를 활용하여 작성된 새 게시글입니다. Compound Documents를 통해 관련 리소스를 효율적으로 로드할 수 있습니다.",
      "status": "draft"
    },
    "relationships": {
      "author": {
        "data": { "type": "people", "id": "42" }
      },
      "tags": {
        "data": [
          { "type": "tags", "id": "1" },
          { "type": "tags", "id": "5" }
        ]
      }
    }
  }
}
```

응답 (201 Created):

```http
HTTP/1.1 201 Created
Location: /articles/16
Content-Type: application/vnd.api+json
```

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "data": {
    "type": "articles",
    "id": "16",
    "attributes": {
      "title": "새로운 블로그 게시글",
      "body": "이것은 JSON:API를 활용하여 작성된 새 게시글입니다. Compound Documents를 통해 관련 리소스를 효율적으로 로드할 수 있습니다.",
      "status": "draft",
      "created-at": "2024-03-20T14:00:00Z",
      "updated-at": "2024-03-20T14:00:00Z"
    },
    "relationships": {
      "author": {
        "links": {
          "self": "/articles/16/relationships/author",
          "related": "/articles/16/author"
        },
        "data": { "type": "people", "id": "42" }
      },
      "comments": {
        "links": {
          "self": "/articles/16/relationships/comments",
          "related": "/articles/16/comments"
        },
        "data": []
      },
      "tags": {
        "links": {
          "self": "/articles/16/relationships/tags",
          "related": "/articles/16/tags"
        },
        "data": [
          { "type": "tags", "id": "1" },
          { "type": "tags", "id": "5" }
        ]
      }
    },
    "links": {
      "self": "/articles/16"
    }
  }
}
```

### 8.5 PATCH - 게시글 수정

```http
PATCH /articles/16 HTTP/1.1
Host: api.example.com
Content-Type: application/vnd.api+json
Accept: application/vnd.api+json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

요청 본문 (변경할 필드만 포함):

```json
{
  "data": {
    "type": "articles",
    "id": "16",
    "attributes": {
      "title": "수정된 블로그 게시글 제목",
      "status": "published"
    }
  }
}
```

핵심 규칙:
- `type`과 `id`는 반드시 포함해야 한다
- URL의 `id`와 본문의 `id`가 일치해야 한다
- 본문에 포함되지 않은 `attributes`는 변경되지 않는다
- `relationships`를 포함하면 해당 관계가 전체 교체된다

응답 (200 OK):

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "data": {
    "type": "articles",
    "id": "16",
    "attributes": {
      "title": "수정된 블로그 게시글 제목",
      "body": "이것은 JSON:API를 활용하여 작성된 새 게시글입니다. Compound Documents를 통해 관련 리소스를 효율적으로 로드할 수 있습니다.",
      "status": "published",
      "created-at": "2024-03-20T14:00:00Z",
      "updated-at": "2024-03-20T15:30:00Z"
    },
    "relationships": {
      "author": {
        "links": {
          "self": "/articles/16/relationships/author",
          "related": "/articles/16/author"
        },
        "data": { "type": "people", "id": "42" }
      },
      "comments": {
        "links": {
          "self": "/articles/16/relationships/comments",
          "related": "/articles/16/comments"
        },
        "data": []
      },
      "tags": {
        "links": {
          "self": "/articles/16/relationships/tags",
          "related": "/articles/16/tags"
        },
        "data": [
          { "type": "tags", "id": "1" },
          { "type": "tags", "id": "5" }
        ]
      }
    },
    "links": {
      "self": "/articles/16"
    }
  }
}
```

### 8.6 DELETE - 게시글 삭제

```http
DELETE /articles/16 HTTP/1.1
Host: api.example.com
Accept: application/vnd.api+json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

응답 (204 No Content):

```http
HTTP/1.1 204 No Content
```

삭제가 성공하면 본문 없이 204를 반환한다. 일부 구현에서는 삭제된 리소스의 `meta` 정보를 포함하여 200을 반환하기도 한다.

```json
{
  "meta": {
    "deleted-at": "2024-03-20T16:00:00Z",
    "message": "게시글이 성공적으로 삭제되었습니다."
  }
}
```

### 8.7 Relationships 엔드포인트

관계 엔드포인트는 리소스의 관계를 독립적으로 조회, 수정할 수 있는 기능을 제공한다.

#### To-One 관계 조회

```http
GET /articles/15/relationships/author HTTP/1.1
Accept: application/vnd.api+json
```

```json
{
  "links": {
    "self": "/articles/15/relationships/author",
    "related": "/articles/15/author"
  },
  "data": { "type": "people", "id": "42" }
}
```

#### To-One 관계 수정

```http
PATCH /articles/15/relationships/author HTTP/1.1
Content-Type: application/vnd.api+json
```

```json
{
  "data": { "type": "people", "id": "7" }
}
```

관계를 제거하려면:

```json
{
  "data": null
}
```

#### To-Many 관계 조회

```http
GET /articles/15/relationships/tags HTTP/1.1
Accept: application/vnd.api+json
```

```json
{
  "links": {
    "self": "/articles/15/relationships/tags",
    "related": "/articles/15/tags"
  },
  "data": [
    { "type": "tags", "id": "1" },
    { "type": "tags", "id": "5" }
  ]
}
```

#### To-Many 관계에 추가 (POST)

```http
POST /articles/15/relationships/tags HTTP/1.1
Content-Type: application/vnd.api+json
```

```json
{
  "data": [
    { "type": "tags", "id": "8" },
    { "type": "tags", "id": "12" }
  ]
}
```

이 요청은 기존 태그를 유지하면서 새 태그를 추가한다.

#### To-Many 관계 전체 교체 (PATCH)

```http
PATCH /articles/15/relationships/tags HTTP/1.1
Content-Type: application/vnd.api+json
```

```json
{
  "data": [
    { "type": "tags", "id": "1" },
    { "type": "tags", "id": "8" }
  ]
}
```

이 요청은 기존 태그를 모두 제거하고 지정된 태그로 교체한다.

#### To-Many 관계에서 제거 (DELETE)

```http
DELETE /articles/15/relationships/tags HTTP/1.1
Content-Type: application/vnd.api+json
```

```json
{
  "data": [
    { "type": "tags", "id": "5" }
  ]
}
```

이 요청은 지정된 태그만 관계에서 제거한다.

### 8.8 에러 응답 예시

#### 단일 에러 - 리소스 미발견

```http
GET /articles/9999 HTTP/1.1
Accept: application/vnd.api+json
```

```http
HTTP/1.1 404 Not Found
Content-Type: application/vnd.api+json
```

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "errors": [
    {
      "id": "err-20240320-001",
      "status": "404",
      "code": "RESOURCE_NOT_FOUND",
      "title": "리소스를 찾을 수 없음",
      "detail": "ID가 '9999'인 'articles' 리소스가 존재하지 않습니다.",
      "links": {
        "about": "https://api.example.com/docs/errors#RESOURCE_NOT_FOUND"
      }
    }
  ]
}
```

#### 다중 에러 - 유효성 검사 실패

```http
POST /articles HTTP/1.1
Content-Type: application/vnd.api+json
```

```json
{
  "data": {
    "type": "articles",
    "attributes": {
      "title": "",
      "body": null,
      "status": "invalid_status"
    },
    "relationships": {
      "author": {
        "data": { "type": "people", "id": "99999" }
      }
    }
  }
}
```

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/vnd.api+json
```

```json
{
  "jsonapi": {
    "version": "1.1"
  },
  "errors": [
    {
      "id": "err-20240320-010",
      "status": "422",
      "code": "VALIDATION_BLANK",
      "title": "필수 필드 누락",
      "detail": "'title' 필드는 비어 있을 수 없습니다. 최소 1자 이상의 문자열을 입력해 주세요.",
      "source": {
        "pointer": "/data/attributes/title"
      }
    },
    {
      "id": "err-20240320-011",
      "status": "422",
      "code": "VALIDATION_NULL",
      "title": "null 허용되지 않음",
      "detail": "'body' 필드는 null일 수 없습니다.",
      "source": {
        "pointer": "/data/attributes/body"
      }
    },
    {
      "id": "err-20240320-012",
      "status": "422",
      "code": "VALIDATION_INCLUSION",
      "title": "유효하지 않은 값",
      "detail": "'status' 필드의 값 'invalid_status'는 허용되지 않습니다. 허용 값: draft, published, archived",
      "source": {
        "pointer": "/data/attributes/status"
      },
      "meta": {
        "allowed-values": ["draft", "published", "archived"]
      }
    },
    {
      "id": "err-20240320-013",
      "status": "422",
      "code": "RELATION_NOT_FOUND",
      "title": "관련 리소스를 찾을 수 없음",
      "detail": "ID가 '99999'인 'people' 리소스가 존재하지 않아 'author' 관계를 설정할 수 없습니다.",
      "source": {
        "pointer": "/data/relationships/author"
      }
    }
  ]
}
```

#### 인증 에러

```http
GET /articles HTTP/1.1
Accept: application/vnd.api+json
```

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/vnd.api+json
WWW-Authenticate: Bearer realm="api"
```

```json
{
  "errors": [
    {
      "status": "401",
      "code": "AUTHENTICATION_REQUIRED",
      "title": "인증이 필요합니다",
      "detail": "이 리소스에 접근하려면 유효한 Bearer 토큰이 필요합니다. Authorization 헤더에 토큰을 포함하여 요청해 주세요."
    }
  ]
}
```

#### 권한 에러

```http
DELETE /articles/15 HTTP/1.1
Accept: application/vnd.api+json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

```http
HTTP/1.1 403 Forbidden
Content-Type: application/vnd.api+json
```

```json
{
  "errors": [
    {
      "status": "403",
      "code": "INSUFFICIENT_PERMISSIONS",
      "title": "권한 부족",
      "detail": "현재 사용자는 이 게시글을 삭제할 권한이 없습니다. 게시글 삭제는 작성자 또는 관리자만 수행할 수 있습니다.",
      "meta": {
        "required-role": "admin",
        "current-role": "viewer"
      }
    }
  ]
}
```

#### 미디어 타입 에러

```http
POST /articles HTTP/1.1
Content-Type: application/json
Accept: application/vnd.api+json
```

```http
HTTP/1.1 415 Unsupported Media Type
Content-Type: application/vnd.api+json
```

```json
{
  "errors": [
    {
      "status": "415",
      "code": "UNSUPPORTED_MEDIA_TYPE",
      "title": "지원되지 않는 미디어 타입",
      "detail": "이 서버는 'application/vnd.api+json' 미디어 타입만 허용합니다. 'application/json' 미디어 타입은 지원되지 않습니다.",
      "source": {
        "header": "Content-Type"
      }
    }
  ]
}
```

### 8.9 Atomic Operations 예시 (JSON:API 1.1)

```http
POST /operations HTTP/1.1
Host: api.example.com
Content-Type: application/vnd.api+json; ext="https://jsonapi.org/ext/atomic"
Accept: application/vnd.api+json; ext="https://jsonapi.org/ext/atomic"
```

```json
{
  "atomic:operations": [
    {
      "op": "add",
      "data": {
        "type": "people",
        "lid": "new-author",
        "attributes": {
          "first-name": "새로운",
          "last-name": "작성자",
          "email": "new-author@example.com"
        }
      }
    },
    {
      "op": "add",
      "data": {
        "type": "tags",
        "lid": "new-tag",
        "attributes": {
          "name": "신규 태그",
          "slug": "new-tag"
        }
      }
    },
    {
      "op": "add",
      "data": {
        "type": "articles",
        "lid": "new-article",
        "attributes": {
          "title": "Atomic Operations로 생성된 게시글",
          "body": "이 게시글은 작성자, 태그와 함께 원자적으로 생성되었습니다.",
          "status": "draft"
        },
        "relationships": {
          "author": {
            "data": { "type": "people", "lid": "new-author" }
          },
          "tags": {
            "data": [
              { "type": "tags", "lid": "new-tag" },
              { "type": "tags", "id": "1" }
            ]
          }
        }
      }
    },
    {
      "op": "update",
      "data": {
        "type": "articles",
        "id": "15",
        "attributes": {
          "status": "archived"
        }
      }
    }
  ]
}
```

응답 (200 OK):

```json
{
  "atomic:results": [
    {
      "data": {
        "type": "people",
        "id": "50",
        "lid": "new-author",
        "attributes": {
          "first-name": "새로운",
          "last-name": "작성자",
          "email": "new-author@example.com"
        },
        "links": { "self": "/people/50" }
      }
    },
    {
      "data": {
        "type": "tags",
        "id": "15",
        "lid": "new-tag",
        "attributes": {
          "name": "신규 태그",
          "slug": "new-tag"
        },
        "links": { "self": "/tags/15" }
      }
    },
    {
      "data": {
        "type": "articles",
        "id": "17",
        "lid": "new-article",
        "attributes": {
          "title": "Atomic Operations로 생성된 게시글",
          "body": "이 게시글은 작성자, 태그와 함께 원자적으로 생성되었습니다.",
          "status": "draft",
          "created-at": "2024-03-20T17:00:00Z",
          "updated-at": "2024-03-20T17:00:00Z"
        },
        "relationships": {
          "author": {
            "data": { "type": "people", "id": "50" }
          },
          "tags": {
            "data": [
              { "type": "tags", "id": "15" },
              { "type": "tags", "id": "1" }
            ]
          }
        },
        "links": { "self": "/articles/17" }
      }
    },
    {
      "data": {
        "type": "articles",
        "id": "15",
        "attributes": {
          "title": "JSON:API로 REST API 설계하기",
          "body": "JSON:API는 클라이언트와 서버 간에 JSON 기반으로 리소스를 주고받는 방식을 표준화한 명세이다...",
          "status": "archived",
          "created-at": "2024-03-15T09:00:00Z",
          "updated-at": "2024-03-20T17:00:00Z"
        },
        "links": { "self": "/articles/15" }
      }
    }
  ]
}
```

---

## 9. 관련 생태계 및 커뮤니티

### 9.1 OpenAPI와의 관계

JSON:API와 OpenAPI Specification(OAS)은 REST API 생태계에서 서로 다른 계층의 문제를 해결한다.

| 측면 | JSON:API | OpenAPI Specification |
|------|----------|----------------------|
| 관심사 | 응답 본문의 구조와 규약 | API 인터페이스의 기술(description) |
| 정의 대상 | 데이터가 "어떤 형태"로 표현되는가 | API가 "어떤 엔드포인트"를 제공하는가 |
| 런타임 영향 | 실제 HTTP 요청/응답에 직접 반영 | 문서/코드 생성 도구가 소비 |
| 상호 관계 | OAS로 JSON:API 응답 스키마를 기술 가능 | JSON:API 규약을 따르는 API를 OAS로 문서화 가능 |

두 명세는 상호 배타적이 아니며, 함께 사용할 때 시너지를 발휘한다. OAS로 엔드포인트, 파라미터, 인증 방식을 기술하고, JSON:API로 응답 형식을 표준화하는 것이 이상적인 조합이다.

### 9.2 하이퍼미디어 형식 비교

JSON:API 외에도 REST API의 응답 형식을 표준화하려는 여러 시도가 존재한다.

#### HAL (Hypertext Application Language)

HAL은 Mike Kelly가 설계한 하이퍼미디어 형식이다. `_links`와 `_embedded` 두 가지 예약 키워드를 통해 링크와 내장 리소스를 표현한다. 미디어 타입은 `application/hal+json`이다.

```json
{
  "_links": {
    "self": { "href": "/articles/1" },
    "author": { "href": "/people/42" }
  },
  "title": "HAL 예시 게시글",
  "body": "본문 내용...",
  "_embedded": {
    "author": {
      "_links": { "self": { "href": "/people/42" } },
      "name": "Yehuda Katz"
    }
  }
}
```

#### Siren

Siren은 Kevin Swiber가 설계한 하이퍼미디어 형식이다. 엔티티(entities), 링크(links), 액션(actions)을 통해 보다 풍부한 하이퍼미디어 제어를 제공한다. 특히 actions를 통해 폼(form) 기반의 상태 전이를 기술할 수 있다는 것이 독특한 특징이다.

```json
{
  "class": ["article"],
  "properties": {
    "title": "Siren 예시 게시글"
  },
  "entities": [
    {
      "class": ["author"],
      "rel": ["author"],
      "href": "/people/42"
    }
  ],
  "actions": [
    {
      "name": "update-article",
      "title": "게시글 수정",
      "method": "PATCH",
      "href": "/articles/1",
      "type": "application/json",
      "fields": [
        { "name": "title", "type": "text" },
        { "name": "body", "type": "text" }
      ]
    }
  ],
  "links": [
    { "rel": ["self"], "href": "/articles/1" }
  ]
}
```

#### JSON-LD + Hydra

JSON-LD(JSON for Linked Data) 는 W3C 표준으로, JSON 데이터에 시맨틱 의미를 부여하는 형식이다. Hydra는 JSON-LD 위에서 웹 API를 기술하기 위한 어휘(vocabulary)이다. 시맨틱 웹 기술에 기반하며, `@context`를 통해 데이터의 의미를 명확히 한다.

```json
{
  "@context": "https://schema.org",
  "@type": "BlogPosting",
  "@id": "/articles/1",
  "headline": "JSON-LD 예시 게시글",
  "articleBody": "본문 내용...",
  "author": {
    "@type": "Person",
    "@id": "/people/42",
    "name": "Yehuda Katz"
  }
}
```

### 9.3 하이퍼미디어 형식 비교 테이블

| 특성 | JSON:API | HAL | Siren | JSON-LD/Hydra |
|------|----------|-----|-------|---------------|
| 설계 철학 | Anti-bikeshedding, 실용주의 | 단순함, 최소 규약 | 풍부한 하이퍼미디어 제어 | 시맨틱 웹, Linked Data |
| 미디어 타입 | `application/vnd.api+json` | `application/hal+json` | `application/vnd.siren+json` | `application/ld+json` |
| IANA 등록 | 등록됨 | 등록됨 | 등록됨 | 등록됨 |
| 리소스 식별 | `type` + `id` | URL (self 링크) | `class` + `href` | `@type` + `@id` |
| 관계 표현 | `relationships` 객체 | `_links` | `entities` (sub/linked) | JSON-LD `@context` |
| 내장 리소스 | `included` 배열 | `_embedded` 객체 | `entities` 배열 | 직접 중첩 |
| 액션/폼 | 미지원 | 미지원 | `actions` 배열 | Hydra `operation` |
| 페이지네이션 | `page` 파라미터 + `links` | `_links` (next, prev) | `links` | Hydra `PartialCollectionView` |
| Sparse Fieldsets | `fields[type]` 지원 | 미지원 | 미지원 | 미지원 |
| 정렬 | `sort` 파라미터 | 미지원 | 미지원 | Hydra 가능 |
| 필터링 | `filter` 파라미터 | 미지원 | 미지원 | Hydra 가능 |
| 에러 형식 | 명세에 포함 | 미지원 (HTTP Problem 등 별도 사용) | 미지원 | Hydra `Error` |
| 복잡도 | 중간 | 낮음 | 높음 | 매우 높음 |
| 학습 곡선 | 중간 | 낮음 | 중간-높음 | 높음 |
| 생태계 크기 | 큼 | 큼 | 작음 | 중간 |
| W3C/IETF 표준 | 독립 명세 | IETF Internet-Draft | 독립 명세 | W3C 표준 |

### 9.4 GraphQL과의 비교

JSON:API와 GraphQL은 모두 "REST API의 한계를 보완한다"는 목표를 가지지만, 접근 방식이 근본적으로 다르다.

| 비교 항목 | JSON:API | GraphQL |
|-----------|----------|---------|
| 패러다임 | REST 기반 (HTTP 메서드 + URL) | 쿼리 언어 기반 (단일 엔드포인트) |
| 엔드포인트 | 리소스별 URL (`/articles`, `/people`) | 단일 URL (`/graphql`) |
| 데이터 형태 결정 | 서버가 결정하되, Sparse Fieldsets로 제한 가능 | 클라이언트가 쿼리로 정확히 지정 |
| Over-fetching 방지 | `fields[type]` 파라미터 | 쿼리에서 필드 선택 |
| Under-fetching 방지 | `include` 파라미터 (Compound Documents) | 중첩 쿼리 |
| 타입 시스템 | `type` 문자열 기반 (약한 타입) | SDL 기반 강타입 스키마 |
| HTTP 캐싱 | URL 기반으로 자연스럽게 활용 가능 | POST 요청이므로 HTTP 캐싱 불가 (별도 구현 필요) |
| 실시간 | 미지원 (별도 WebSocket 등) | Subscription으로 스펙에 내장 |
| 파일 업로드 | 표준 multipart/form-data 활용 | 미지원 (별도 구현 필요) |
| 에러 처리 | HTTP 상태 코드 + 에러 객체 | 항상 200, `errors` 배열 |
| 인트로스펙션 | 미지원 | 내장 인트로스펙션 쿼리 |
| 배치 처리 | Atomic Operations (1.1) | 단일 요청에 여러 쿼리 가능 |
| 버전 관리 | URL 버저닝 또는 미디어 타입 | 스키마 진화 (deprecation) |
| 학습 곡선 | REST 경험이 있으면 낮음 | 새로운 쿼리 언어 학습 필요 |
| 도구 지원 | REST 도구(Postman, curl 등) 그대로 사용 | 전용 도구(GraphiQL, Apollo DevTools 등) 필요 |

#### 선택 기준

JSON:API가 적합한 경우:
- 기존 REST API 인프라와 도구를 활용하고자 할 때
- HTTP 캐싱이 중요한 시스템일 때
- 리소스 간 관계가 비교적 단순하고 예측 가능할 때
- 팀이 REST에 익숙하고, 새로운 쿼리 언어 도입에 부담이 있을 때
- 다양한 클라이언트가 유사한 데이터를 소비할 때

GraphQL이 적합한 경우:
- 클라이언트마다 필요한 데이터가 크게 다를 때 (모바일 vs 웹 vs 데스크톱)
- 리소스 간 관계가 깊고 복잡하여 다양한 탐색 패턴이 필요할 때
- 프론트엔드 팀이 백엔드 API 변경 없이 독립적으로 데이터 요구사항을 변경하고 싶을 때
- 실시간 기능(Subscription)이 필요할 때
- 강타입 스키마를 통한 개발 생산성 향상이 중요할 때

### 9.5 커뮤니티 및 거버넌스

JSON:API 명세는 GitHub에서 오픈 소스로 관리된다. 주요 관리자(maintainer)는 다음과 같다.

- Yehuda Katz - 명세 창시자
- Dan Gebhardt - 현재 주요 관리자
- Ethan Resnick - 명세 기여자
- Steve Klabnik - 초기 기여자 (Rust 커뮤니티에서도 활동)

커뮤니티 활동:
- 공식 사이트: [https://jsonapi.org](https://jsonapi.org)에서 명세 전문, 구현체 목록, FAQ 등을 제공한다
- GitHub 저장소: 명세 변경 사항은 이슈와 풀 리퀘스트를 통해 논의되고 반영된다
- 구현체 목록: 공식 사이트에서 30개 이상 언어/프레임워크의 서버 및 클라이언트 구현체 목록을 관리한다
- Discussion Forum: GitHub Discussions를 통해 명세 해석, 구현 질문, 확장 제안 등이 논의된다

### 9.6 관련 표준 및 RFC

JSON:API 명세는 여러 기존 표준과 RFC를 참조하고 활용한다.

| 표준/RFC | JSON:API에서의 활용 |
|----------|---------------------|
| RFC 7159 (JSON) | 기본 데이터 형식 |
| RFC 7231 (HTTP/1.1 Semantics) | HTTP 메서드, 상태 코드의 의미론 |
| RFC 6570 (URI Template) | URL 템플릿 표현 |
| RFC 6901 (JSON Pointer) | 에러 `source.pointer`에서 사용 |
| RFC 5988 (Web Linking) | 링크 관계 유형 참조 |
| RFC 6838 (Media Type) | `application/vnd.api+json` 등록 근거 |
| RFC 7807 (Problem Details) | 에러 형식 설계 시 참고 (JSON:API는 자체 에러 형식 사용) |

---

## 부록: JSON:API 문서 구조 전체 요약

```json
{
  "jsonapi": {
    "version": "1.1",
    "ext": ["URI"],
    "profile": ["URI"],
    "meta": {}
  },
  "data": {
    "type": "string (필수)",
    "id": "string (필수, POST 시 선택)",
    "lid": "string (1.1, 선택)",
    "attributes": {
      "key": "any JSON value"
    },
    "relationships": {
      "relationship-name": {
        "links": {
          "self": "URL (관계 자체)",
          "related": "URL (관련 리소스)"
        },
        "data": "Resource Identifier | [Resource Identifiers] | null",
        "meta": {}
      }
    },
    "links": {
      "self": "URL"
    },
    "meta": {}
  },
  "included": [
    {
      "type": "string",
      "id": "string",
      "attributes": {},
      "relationships": {},
      "links": {},
      "meta": {}
    }
  ],
  "links": {
    "self": "URL",
    "first": "URL | null",
    "prev": "URL | null",
    "next": "URL | null",
    "last": "URL | null",
    "related": "URL",
    "describedby": "URL"
  },
  "meta": {},
  "errors": [
    {
      "id": "string",
      "links": {
        "about": "URL",
        "type": "URL"
      },
      "status": "HTTP status code string",
      "code": "application-specific code string",
      "title": "general summary (stable across occurrences)",
      "detail": "specific explanation (varies per occurrence)",
      "source": {
        "pointer": "JSON Pointer (RFC 6901)",
        "parameter": "query parameter name",
        "header": "HTTP header name"
      },
      "meta": {}
    }
  ]
}
```
