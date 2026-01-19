# RFC 6902: JavaScript Object Notation (JSON) Patch

> JSON 문서에 부분 수정을 적용하기 위한 형식 정의

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 6902 |
| **분류** | Proposed Standard (표준 제안) |
| **작성자** | P. Bryan (Salesforce.com), M. Nottingham (Akamai) |
| **발행일** | 2013년 4월 |
| **상태** | 현행 표준 |
| **미디어 타입** | application/json-patch+json |

## 개요

RFC 6902는 JSON 문서에 부분적인 수정을 표현하기 위한 형식인 **JSON Patch**를 정의합니다. JSON Patch는 HTTP PATCH 메서드(RFC 5789)와 함께 사용하도록 설계되었으며, `application/json-patch+json` 미디어 타입으로 식별됩니다.

JSON Patch를 사용하면 전체 문서를 교체하지 않고도 JSON 문서의 특정 부분만 수정할 수 있어, 네트워크 대역폭을 절약하고 동시 수정 충돌을 줄일 수 있습니다.

---

## 규약

이 문서에서 사용된 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL" 키워드는 RFC 2119에 기술된 대로 해석되어야 합니다.

---

## JSON Patch 문서 구조

### 기본 구조

JSON Patch 문서는 **JSON 객체들의 배열**입니다. 각 객체는 대상 JSON 문서에 적용할 **단일 연산(operation)**을 나타냅니다.

```json
[
  { "op": "test", "path": "/a/b/c", "value": "foo" },
  { "op": "remove", "path": "/a/b/c" },
  { "op": "add", "path": "/a/b/c", "value": ["foo", "bar"] },
  { "op": "replace", "path": "/a/b/c", "value": 42 },
  { "op": "move", "from": "/a/b/c", "path": "/a/b/d" },
  { "op": "copy", "from": "/a/b/d", "path": "/a/b/e" }
]
```

### 연산 적용 순서

**연산은 배열에 나타난 순서대로 순차적으로 적용됩니다.** 각 연산의 결과가 다음 연산의 입력이 됩니다.

```
원본 문서 → [연산 1 적용] → 중간 문서 1 → [연산 2 적용] → 중간 문서 2 → ... → 최종 문서
```

### 연산 객체 구조

모든 연산 객체는 다음을 포함해야 합니다(MUST):

| 멤버 | 설명 |
|------|------|
| **op** | 수행할 연산의 이름을 나타내는 문자열 |
| **path** | 대상 위치를 나타내는 JSON Pointer (RFC 6901) 문자열 |

**참고사항:**
- 멤버의 순서는 중요하지 않습니다
- 정의되지 않은 연산 멤버는 무시해야 합니다(SHOULD)
- 연산 객체에서 멤버 순서는 중요하지 않습니다

---

## JSON Pointer (RFC 6901)

JSON Patch의 모든 연산은 **JSON Pointer**를 사용하여 대상 위치를 참조합니다.

### JSON Pointer 형식

JSON Pointer는 `/` 문자로 구분된 **참조 토큰(reference token)**들의 문자열입니다.

```
/foo/bar/0/baz
 │   │   │  └─ 객체의 "baz" 멤버
 │   │   └──── 배열의 첫 번째 요소 (인덱스 0)
 │   └──────── 객체의 "bar" 멤버
 └──────────── 객체의 "foo" 멤버
```

### 특수 문자 이스케이프

JSON Pointer에서 특수 문자는 다음과 같이 이스케이프됩니다:

| 문자 | 이스케이프 |
|------|----------|
| `~` | `~0` |
| `/` | `~1` |

**예시:**

```json
{
  "foo/bar": "value1",
  "a~b": "value2"
}
```

- `"foo/bar"` 접근: `/foo~1bar`
- `"a~b"` 접근: `/a~0b`

### 배열 인덱스

배열 요소는 0부터 시작하는 숫자 인덱스로 참조됩니다:

```json
{
  "items": ["apple", "banana", "cherry"]
}
```

- `/items/0` → `"apple"`
- `/items/1` → `"banana"`
- `/items/2` → `"cherry"`

**특수 인덱스 `-`:**
- 배열의 끝을 나타내며, `add` 연산에서 배열 끝에 요소를 추가할 때 사용됩니다
- 예: `/items/-`는 배열의 마지막 위치 다음(새 요소 추가 위치)을 가리킵니다

---

## 연산 (Operations)

JSON Patch는 6가지 연산을 정의합니다:

### 1. add - 값 추가

**형식:**
```json
{ "op": "add", "path": "<JSON Pointer>", "value": <추가할 값> }
```

**동작:**

`add` 연산은 대상 위치가 참조하는 것에 따라 다음 기능 중 하나를 수행합니다:

| 대상 위치 | 동작 |
|----------|------|
| 배열 인덱스 | 해당 인덱스에 새 값을 삽입, 기존 요소들은 오른쪽으로 이동 |
| 객체 멤버 | 해당 멤버에 값을 추가하거나 기존 값을 교체 |
| 루트 (`""`) | 전체 문서 내용을 대체 |

**필수 조건:**
- `value` 멤버를 반드시 포함해야 합니다(MUST)
- 대상 위치의 상위 객체/배열이 존재해야 합니다(MUST)
- 배열 인덱스는 기존 요소 또는 배열 끝(`-`)을 참조해야 합니다(MUST)

**예시:**

```json
// 원본 문서
{ "foo": "bar" }

// 연산: 객체에 새 멤버 추가
{ "op": "add", "path": "/baz", "value": "qux" }

// 결과
{ "foo": "bar", "baz": "qux" }
```

```json
// 원본 문서
{ "foo": ["bar", "baz"] }

// 연산: 배열에 요소 삽입 (인덱스 1에)
{ "op": "add", "path": "/foo/1", "value": "qux" }

// 결과
{ "foo": ["bar", "qux", "baz"] }
```

```json
// 원본 문서
{ "foo": ["bar", "baz"] }

// 연산: 배열 끝에 요소 추가
{ "op": "add", "path": "/foo/-", "value": "qux" }

// 결과
{ "foo": ["bar", "baz", "qux"] }
```

---

### 2. remove - 값 제거

**형식:**
```json
{ "op": "remove", "path": "<JSON Pointer>" }
```

**동작:**

대상 위치의 값을 제거합니다.

| 대상 위치 | 동작 |
|----------|------|
| 배열 요소 | 해당 요소 제거, 이후 요소들이 왼쪽으로 이동 |
| 객체 멤버 | 해당 멤버 제거 |

**필수 조건:**
- 대상 위치가 반드시 존재해야 합니다(MUST)
- 연산이 성공하려면 대상이 있어야 합니다

**예시:**

```json
// 원본 문서
{ "baz": "qux", "foo": "bar" }

// 연산: 객체 멤버 제거
{ "op": "remove", "path": "/baz" }

// 결과
{ "foo": "bar" }
```

```json
// 원본 문서
{ "foo": ["bar", "qux", "baz"] }

// 연산: 배열 요소 제거 (인덱스 1)
{ "op": "remove", "path": "/foo/1" }

// 결과
{ "foo": ["bar", "baz"] }
```

---

### 3. replace - 값 교체

**형식:**
```json
{ "op": "replace", "path": "<JSON Pointer>", "value": <새 값> }
```

**동작:**

대상 위치의 값을 새 값으로 교체합니다. 이 연산은 기능적으로 대상 위치에 대한 `remove` 연산 후 동일한 위치에 `add` 연산을 수행하는 것과 동일합니다.

**필수 조건:**
- `value` 멤버를 반드시 포함해야 합니다(MUST)
- 대상 위치가 반드시 존재해야 합니다(MUST)

**예시:**

```json
// 원본 문서
{ "baz": "qux", "foo": "bar" }

// 연산: 값 교체
{ "op": "replace", "path": "/baz", "value": "boo" }

// 결과
{ "baz": "boo", "foo": "bar" }
```

---

### 4. move - 값 이동

**형식:**
```json
{ "op": "move", "from": "<원본 JSON Pointer>", "path": "<대상 JSON Pointer>" }
```

**동작:**

지정된 위치(`from`)에서 값을 제거하고 대상 위치(`path`)에 추가합니다. 이 연산은 기능적으로 `from` 위치에 대한 `remove` 연산 후 `path` 위치에 제거된 값으로 `add` 연산을 수행하는 것과 동일합니다.

**필수 조건:**
- `from` 멤버를 반드시 포함해야 합니다(MUST)
- `from` 위치가 반드시 존재해야 합니다(MUST)
- `from` 위치는 `path` 위치의 상위 경로가 될 수 없습니다(MUST NOT)
  - 즉, 위치를 자신의 자식으로 이동할 수 없습니다

**예시:**

```json
// 원본 문서
{ "foo": { "bar": "baz", "waldo": "fred" }, "qux": { "corge": "grault" } }

// 연산: 값 이동
{ "op": "move", "from": "/foo/waldo", "path": "/qux/thud" }

// 결과
{ "foo": { "bar": "baz" }, "qux": { "corge": "grault", "thud": "fred" } }
```

```json
// 원본 문서
{ "foo": ["all", "grass", "cows", "eat"] }

// 연산: 배열 요소 이동
{ "op": "move", "from": "/foo/1", "path": "/foo/3" }

// 결과
{ "foo": ["all", "cows", "eat", "grass"] }
```

---

### 5. copy - 값 복사

**형식:**
```json
{ "op": "copy", "from": "<원본 JSON Pointer>", "path": "<대상 JSON Pointer>" }
```

**동작:**

지정된 위치(`from`)의 값을 대상 위치(`path`)로 복사합니다. 이 연산은 기능적으로 `from` 위치의 값을 사용하여 `path` 위치에 `add` 연산을 수행하는 것과 동일합니다.

**필수 조건:**
- `from` 멤버를 반드시 포함해야 합니다(MUST)
- `from` 위치가 반드시 존재해야 합니다(MUST)

**예시:**

```json
// 원본 문서
{ "foo": { "bar": "baz", "waldo": "fred" }, "qux": { "corge": "grault" } }

// 연산: 값 복사
{ "op": "copy", "from": "/foo/waldo", "path": "/qux/thud" }

// 결과
{ "foo": { "bar": "baz", "waldo": "fred" }, "qux": { "corge": "grault", "thud": "fred" } }
```

---

### 6. test - 값 검증

**형식:**
```json
{ "op": "test", "path": "<JSON Pointer>", "value": <기대 값> }
```

**동작:**

대상 위치의 값이 지정된 값과 동일한지 테스트합니다. 테스트가 실패하면 연산이 실패한 것으로 간주됩니다.

**필수 조건:**
- `value` 멤버를 반드시 포함해야 합니다(MUST)
- 대상 위치가 반드시 존재해야 합니다(MUST)

**용도:**
- 조건부 패치 적용
- 동시 수정 충돌 감지
- 패치 적용 전 상태 확인

**예시:**

```json
// 원본 문서
{ "baz": "qux", "foo": ["a", 2, "c"] }

// 연산: 문자열 값 테스트 (성공)
{ "op": "test", "path": "/baz", "value": "qux" }

// 연산: 배열 값 테스트 (성공)
{ "op": "test", "path": "/foo", "value": ["a", 2, "c"] }
```

---

## 값 비교 규칙 (test 연산)

`test` 연산에서 값의 동등성은 다음 규칙에 따라 판단됩니다:

### 타입별 비교 규칙

| JSON 타입 | 동등 조건 |
|----------|----------|
| **문자열** | 동일한 코드포인트 수를 포함하고, 바이트 단위로 정확히 일치 |
| **숫자** | 수치적으로 동일 (예: `1.0`과 `1`은 동일) |
| **배열** | 동일한 개수의 값을 포함하고, 각 위치의 값이 동일 |
| **객체** | 동일한 개수의 멤버를 포함하고, 각 멤버가 동일한 키-값 쌍 |
| **리터럴** | `false`, `true`, `null`은 동일한 리터럴일 때만 동일 |

### 비교 예시

```json
// 동일한 값들
1 == 1.0        // 숫자 동등
"abc" == "abc"  // 문자열 동등
[1, 2] == [1, 2]  // 배열 동등 (순서 중요)
{"a": 1, "b": 2} == {"b": 2, "a": 1}  // 객체 동등 (순서 무관)

// 다른 값들
1 != "1"        // 타입이 다름
[1, 2] != [2, 1]  // 배열 순서가 다름
null != false   // 다른 리터럴
```

---

## 에러 처리

### 에러 발생 조건

다음 상황에서 연산이 실패합니다:

| 상황 | 설명 |
|------|------|
| 규범적 요구사항 위반 | MUST/MUST NOT 요구사항을 위반한 경우 |
| 대상 위치 미존재 | `remove`, `replace`, `test`에서 대상이 없는 경우 |
| 원본 위치 미존재 | `move`, `copy`에서 `from` 위치가 없는 경우 |
| `test` 실패 | 지정된 값과 실제 값이 일치하지 않는 경우 |
| 유효하지 않은 JSON Pointer | 올바르지 않은 경로 형식 |

### 원자성 (Atomicity)

**JSON Patch 문서의 평가는 원자적이어야 합니다:**

- 모든 연산이 성공하거나
- 하나라도 실패하면 전체 패치가 적용되지 않습니다

```
규범적 요구사항 위반 또는 연산 실패 시:
JSON Patch 문서의 평가가 중단되어야 하며(SHOULD),
전체 패치 문서 적용이 성공하지 않아야 합니다(SHOULD NOT).
```

### HTTP PATCH와의 관계

RFC 5789 (HTTP PATCH)는 PATCH 메서드가 원자적이어야 한다고 명시합니다:

> 서버는 PATCH 요청의 모든 변경을 원자적으로 적용해야 하며(MUST), 일부만 적용된 패치 문서의 결과가 제공되어서는 안 됩니다(MUST NEVER).

---

## 연산 요약표

| 연산 | 필수 멤버 | 설명 | 대상 존재 필요 |
|------|----------|------|--------------|
| **add** | `path`, `value` | 값 추가 또는 삽입 | 상위 경로만 |
| **remove** | `path` | 값 제거 | 예 |
| **replace** | `path`, `value` | 값 교체 | 예 |
| **move** | `from`, `path` | 값 이동 | `from` 위치 |
| **copy** | `from`, `path` | 값 복사 | `from` 위치 |
| **test** | `path`, `value` | 값 검증 | 예 |

---

## 실용적인 예시

### 예시 1: 사용자 정보 업데이트

```json
// 원본 문서
{
  "user": {
    "name": "John",
    "email": "john@old.com",
    "roles": ["user"]
  }
}

// JSON Patch
[
  { "op": "replace", "path": "/user/email", "value": "john@new.com" },
  { "op": "add", "path": "/user/roles/-", "value": "admin" },
  { "op": "add", "path": "/user/phone", "value": "123-456-7890" }
]

// 결과
{
  "user": {
    "name": "John",
    "email": "john@new.com",
    "roles": ["user", "admin"],
    "phone": "123-456-7890"
  }
}
```

### 예시 2: 조건부 업데이트 (test 사용)

```json
// 원본 문서
{
  "version": "1.0",
  "data": { "count": 5 }
}

// JSON Patch (version이 "1.0"일 때만 업데이트)
[
  { "op": "test", "path": "/version", "value": "1.0" },
  { "op": "replace", "path": "/version", "value": "1.1" },
  { "op": "replace", "path": "/data/count", "value": 10 }
]

// 결과 (test 성공 시)
{
  "version": "1.1",
  "data": { "count": 10 }
}

// test 실패 시: 전체 패치가 적용되지 않음
```

### 예시 3: 복잡한 구조 조작

```json
// 원본 문서
{
  "store": {
    "books": [
      { "title": "Book A", "price": 10 },
      { "title": "Book B", "price": 20 }
    ],
    "location": "Seoul"
  }
}

// JSON Patch
[
  { "op": "copy", "from": "/store/location", "path": "/store/books/0/location" },
  { "op": "remove", "path": "/store/books/1" },
  { "op": "move", "from": "/store/location", "path": "/store/city" }
]

// 결과
{
  "store": {
    "books": [
      { "title": "Book A", "price": 10, "location": "Seoul" }
    ],
    "city": "Seoul"
  }
}
```

---

## 보안 고려사항

### JSON 관련 보안

JSON Patch는 JSON(RFC 4627)과 JSON Pointer(RFC 6901)와 동일한 보안 고려사항을 가집니다.

### 신뢰할 수 없는 패치 문서

**신뢰할 수 없는 출처의 패치 문서를 처리할 때 주의해야 합니다:**

- 악의적인 패치가 예상치 못한 데이터 수정을 유발할 수 있습니다
- 서버는 패치 연산의 범위와 영향을 제한하는 것이 좋습니다
- 민감한 필드에 대한 수정을 제한하는 정책을 구현해야 합니다

### CSRF (Cross-Site Request Forgery)

일부 오래된 웹 브라우저에서 CSRF 공격 위험이 있습니다. 그러나 이러한 브라우저의 시장 점유율은 1% 미만으로 추정됩니다.

**권장 보안 조치:**
- 적절한 CSRF 토큰 사용
- Content-Type 헤더 검증
- 요청 출처(Origin) 검증

---

## IANA 고려사항

### 미디어 타입 등록

RFC 6902는 다음 미디어 타입을 IANA에 등록합니다:

| 항목 | 값 |
|------|-----|
| **타입 이름** | application |
| **하위 타입 이름** | json-patch+json |
| **필수 매개변수** | 없음 |
| **선택적 매개변수** | 없음 |
| **인코딩 고려사항** | binary (RFC 4627 Section 6에 따름) |
| **보안 고려사항** | RFC 6902 Section 5 참조 |
| **상호운용성 고려사항** | 해당 없음 |
| **발행된 명세** | RFC 6902 |
| **파일 확장자** | .json-patch |
| **Macintosh 파일 타입 코드** | TEXT |
| **연락처** | json@ietf.org |

---

## HTTP에서의 사용

### PATCH 요청 예시

```http
PATCH /users/123 HTTP/1.1
Host: api.example.com
Content-Type: application/json-patch+json
If-Match: "etag-value"

[
  { "op": "replace", "path": "/email", "value": "new@email.com" },
  { "op": "add", "path": "/verified", "value": true }
]
```

### 응답 예시

**성공 시 (200 OK):**
```http
HTTP/1.1 200 OK
Content-Type: application/json
ETag: "new-etag-value"

{
  "id": 123,
  "email": "new@email.com",
  "verified": true
}
```

**실패 시 (409 Conflict):**
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "error": "Patch operation failed",
  "detail": "test operation at /version failed"
}
```

---

## 구현 고려사항

### 트랜잭션 처리

구현체는 원자성을 보장하기 위해 다음 전략을 사용할 수 있습니다:

1. **복사 후 수정**: 원본을 복사한 후 복사본에 연산을 적용하고, 모든 연산이 성공하면 원본을 복사본으로 교체
2. **롤백 지원**: 각 연산의 역연산을 기록하고, 실패 시 역순으로 롤백
3. **사전 검증**: 모든 연산을 먼저 검증한 후 적용

### 성능 최적화

- 여러 연산을 배치로 처리하여 I/O 최소화
- `test` 연산을 먼저 배치하여 조기 실패 감지
- JSON Pointer 파싱 결과 캐싱

---

## 참고 자료

### 규범적 참조 (Normative References)

- **RFC 2119**: Key words for use in RFCs to Indicate Requirement Levels
- **RFC 4627**: The application/json Media Type for JavaScript Object Notation (JSON)
- **RFC 6901**: JavaScript Object Notation (JSON) Pointer

### 정보적 참조 (Informative References)

- **RFC 5789**: PATCH Method for HTTP

---

## 관련 RFC

| RFC | 제목 | 관계 |
|-----|------|------|
| **RFC 6901** | JSON Pointer | JSON Patch에서 경로 참조에 사용 |
| **RFC 5789** | HTTP PATCH | JSON Patch의 주요 사용 컨텍스트 |
| **RFC 7396** | JSON Merge Patch | JSON Patch의 대안적 접근 방식 |
| **RFC 8259** | JSON (RFC 4627 대체) | 최신 JSON 명세 |

---

## 부록: JSON Patch vs JSON Merge Patch

| 특성 | JSON Patch (RFC 6902) | JSON Merge Patch (RFC 7396) |
|------|----------------------|----------------------------|
| **형식** | 연산 배열 | 병합할 JSON 객체 |
| **명시성** | 명시적 연산 | 암시적 병합 |
| **배열 처리** | 개별 요소 조작 가능 | 전체 배열 교체만 가능 |
| **null 처리** | 값으로 사용 가능 | 삭제를 의미 |
| **복잡성** | 더 복잡하지만 강력 | 단순하지만 제한적 |
| **미디어 타입** | application/json-patch+json | application/merge-patch+json |

---

*이 문서는 RFC 6902의 한국어 번역 및 정리본입니다.*
