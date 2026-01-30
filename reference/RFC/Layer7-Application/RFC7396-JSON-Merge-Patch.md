# RFC 7396: JSON Merge Patch

> HTTP PATCH 메서드와 함께 사용되는 JSON 문서의 부분 수정 형식

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 7396 |
| **분류** | Proposed Standard (제안 표준) |
| **작성자** | P. Hoffman (VPN Consortium), J. Snell |
| **발행일** | 2014년 10월 |
| **폐기한 문서** | RFC 7386 |
| **ISSN** | 2070-1721 |
| **상태** | Internet Standards Track |

---

## 개요 (Abstract)

이 명세는 JSON merge patch 문서 형식과 처리 규칙을 정의합니다. merge patch 형식은 주로 HTTP PATCH 메서드와 함께 사용되어 대상 리소스의 내용에 대한 수정 사항을 기술하는 방법을 설명합니다.

---

## 1. 소개 (Introduction)

이 명세는 JSON merge patch 문서 형식, 처리 규칙, 관련 MIME 미디어 타입 식별자를 정의합니다. merge patch 문서는 수정될 문서와 유사한 구문을 사용하여 변경 사항을 기술하도록 설계되었습니다.

### 핵심 원칙

| 작업 | 동작 |
|------|------|
| **추가** | 대상에 존재하지 않는 멤버는 추가됨 |
| **교체** | 대상에 존재하는 멤버의 값은 교체됨 |
| **삭제** | null 값은 대상에서 기존 값을 제거함을 나타냄 |

### 예시 시나리오

다음과 같은 원본 JSON 문서가 있다고 가정합니다:

```json
{
  "a": "b",
  "c": {
    "d": "e",
    "f": "g"
  }
}
```

"a" 멤버의 값을 "z"로 변경하고, "f" 멤버를 제거하려면 다음과 같은 HTTP PATCH 요청을 보냅니다:

```http
PATCH /target HTTP/1.1
Host: example.org
Content-Type: application/merge-patch+json

{
  "a": "z",
  "c": {
    "f": null
  }
}
```

### 적용 범위

이 형식은 주로 **객체로 구조화된 JSON 문서**에 적합하며, **명시적인 null 값을 사용하지 않는** 경우에 가장 효과적입니다.

**적합한 경우:**
- JSON 문서가 주로 객체로 구성된 경우
- null 값이 실제 데이터로 사용되지 않는 경우

**부적합한 경우:**
- 배열의 특정 요소만 수정해야 하는 경우
- null이 유효한 데이터 값으로 사용되는 경우
- 문서의 최상위가 배열인 경우

---

## 2. Merge Patch 문서 처리

Merge patch 문서를 받으면, 수신자는 현재 대상 리소스의 내용과 merge patch를 비교하여 적용할 구체적인 변경 작업 집합을 결정합니다.

### MergePatch 알고리즘 (의사코드)

```
define MergePatch(Target, Patch):
  if Patch is an Object:
    if Target is not an Object:
      Target = {}  // 빈 객체로 설정
    for each Name/Value pair in Patch:
      if Value is null:
        if Name exists in Target:
          remove the Name/Value pair from Target
      else:
        Target[Name] = MergePatch(Target[Name], Value)
    return Target
  else:
    return Patch
```

### 알고리즘 설명

```
┌─────────────────────────────────────────────────────────────┐
│                    MergePatch 처리 흐름                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Patch가 객체인가?                                        │
│     ├─ 아니오 → Patch를 그대로 반환 (전체 교체)               │
│     └─ 예 ↓                                                 │
│                                                             │
│  2. Target이 객체인가?                                       │
│     ├─ 아니오 → Target을 빈 객체 {}로 초기화                  │
│     └─ 예 ↓                                                 │
│                                                             │
│  3. Patch의 각 Name/Value 쌍에 대해:                         │
│     ├─ Value가 null인 경우:                                  │
│     │   └─ Target에서 해당 Name 제거                         │
│     └─ Value가 null이 아닌 경우:                              │
│         └─ Target[Name] = MergePatch(Target[Name], Value)   │
│           (재귀적 병합)                                      │
│                                                             │
│  4. 수정된 Target 반환                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 중요한 특성

#### 1. 비객체 Patch는 전체 교체

Patch가 객체가 아닌 경우, 대상 전체가 Patch 값으로 교체됩니다.

```json
// Target
{"a": "foo"}

// Patch (문자열)
"bar"

// Result
"bar"
```

#### 2. 배열 수정의 제한

배열의 특정 요소만 수정하는 것은 **불가능**합니다. 배열을 수정하려면 **전체 배열을 교체**해야 합니다.

```json
// Target
{"tags": ["example", "sample", "test"]}

// Patch - "sample"만 제거하고 싶은 경우
{"tags": ["example", "test"]}  // 전체 배열 교체 필요

// Result
{"tags": ["example", "test"]}
```

#### 3. 텍스트 표현이 아닌 데이터 수준의 작업

Merge patch 연산은 JSON 데이터 항목 수준에서 작동하며, 텍스트 표현 수준에서는 작동하지 않습니다.

**보장되지 않는 것들:**
- 공백(whitespace) 유지
- 멤버 순서 유지
- 숫자의 정밀도 유지

```json
// 원본에서의 순서
{"b": 1, "a": 2}

// Patch 적용 후 순서가 변경될 수 있음
{"a": 2, "b": 1}  // 또는 다른 순서
```

---

## 3. 예시

### 원본 문서

```json
{
  "title": "Goodbye!",
  "author": {
    "givenName": "John",
    "familyName": "Doe"
  },
  "tags": ["example", "sample"],
  "content": "This will be unchanged"
}
```

### PATCH 요청

```http
PATCH /my/resource HTTP/1.1
Host: example.org
Content-Type: application/merge-patch+json

{
  "title": "Hello!",
  "phoneNumber": "+01-123-456-7890",
  "author": {
    "familyName": null
  },
  "tags": ["example"]
}
```

### 변경 사항 분석

| 필드 | 원본 값 | Patch 값 | 결과 | 설명 |
|------|---------|----------|------|------|
| `title` | "Goodbye!" | "Hello!" | "Hello!" | 값 교체 |
| `phoneNumber` | (없음) | "+01-123-456-7890" | "+01-123-456-7890" | 새 필드 추가 |
| `author.givenName` | "John" | (없음) | "John" | 변경 없음 |
| `author.familyName` | "Doe" | null | (삭제됨) | 필드 삭제 |
| `tags` | ["example", "sample"] | ["example"] | ["example"] | 배열 전체 교체 |
| `content` | "This will be unchanged" | (없음) | "This will be unchanged" | 변경 없음 |

### 결과 문서

```json
{
  "title": "Hello!",
  "author": {
    "givenName": "John"
  },
  "tags": ["example"],
  "content": "This will be unchanged",
  "phoneNumber": "+01-123-456-7890"
}
```

---

## 4. IANA 고려사항 (IANA Considerations)

이 명세는 다음 MIME 미디어 타입을 등록합니다:

### application/merge-patch+json

| 항목 | 값 |
|------|-----|
| **타입 이름** | application |
| **서브타입 이름** | merge-patch+json |
| **필수 매개변수** | 없음 |
| **선택 매개변수** | 없음 |
| **인코딩 고려사항** | "application/json" 타입에서 정의된 것과 동일 |
| **보안 고려사항** | 이 문서의 섹션 5 참조 |
| **상호운용성 고려사항** | 없음 |
| **의도된 사용** | COMMON (일반적 사용) |
| **사용 제한** | 없음 |
| **작성자** | James M. Snell <jasnell@gmail.com> |
| **변경 관리자** | IESG |

---

## 5. 보안 고려사항 (Security Considerations)

merge patch 문서 처리자가 수행하는 모든 작업은 변경이 **적절한지**, 그리고 사용자 에이전트가 해당 변경을 **요청할 권한이 있는지** 여부를 서버가 판단해야 합니다. 그러한 판단을 내리는 데 사용되는 메커니즘은 이 명세의 범위를 벗어납니다.

RFC 5789 [RFC5789]에서 정의된 HTTP PATCH 메서드의 모든 보안 고려사항이 JSON merge patch와 함께 PATCH 사용에 적용됩니다.

### 주요 보안 고려사항

| 항목 | 설명 |
|------|------|
| **권한 검증** | 서버는 요청자가 해당 변경을 수행할 권한이 있는지 확인해야 함 |
| **변경 유효성** | 서버는 요청된 변경이 리소스에 적합한지 검증해야 함 |
| **부분 적용 방지** | Patch 연산은 원자적(atomic)으로 적용되어야 함 |
| **민감 데이터** | null로 삭제되는 필드에 민감 정보가 있을 수 있음에 유의 |

---

## 6. 참고 문헌 (References)

### 6.1 규범적 참고 문헌 (Normative References)

| 문서 | 설명 |
|------|------|
| **[RFC7159]** | Bray, T., Ed., "The JavaScript Object Notation (JSON) Data Interchange Format", RFC 7159, March 2014 |

### 6.2 정보 참고 문헌 (Informative References)

| 문서 | 설명 |
|------|------|
| **[RFC5789]** | Dusseault, L. and J. Snell, "PATCH Method for HTTP", RFC 5789, March 2010 |

---

## 부록 A. 예시 테스트 케이스 (Example Test Cases)

다음 테스트 케이스들은 merge patch 작업의 예상 결과를 보여줍니다.

### 기본 객체 연산

| # | 원본 (ORIGINAL) | Patch | 결과 (RESULT) | 설명 |
|---|-----------------|-------|---------------|------|
| 1 | `{"a":"b"}` | `{"a":"c"}` | `{"a":"c"}` | 값 교체 |
| 2 | `{"a":"b"}` | `{"b":"c"}` | `{"a":"b","b":"c"}` | 새 필드 추가 |
| 3 | `{"a":"b"}` | `{"a":null}` | `{}` | 필드 삭제 |
| 4 | `{"a":"b","b":"c"}` | `{"a":null}` | `{"b":"c"}` | 특정 필드만 삭제 |

### 타입 변환

| # | 원본 (ORIGINAL) | Patch | 결과 (RESULT) | 설명 |
|---|-----------------|-------|---------------|------|
| 5 | `{"a":["b"]}` | `{"a":"c"}` | `{"a":"c"}` | 배열을 문자열로 교체 |
| 6 | `{"a":"c"}` | `{"a":["b"]}` | `{"a":["b"]}` | 문자열을 배열로 교체 |

### 중첩 객체

| # | 원본 (ORIGINAL) | Patch | 결과 (RESULT) | 설명 |
|---|-----------------|-------|---------------|------|
| 7 | `{"a":{"b":"c"}}` | `{"a":{"b":"d","c":null}}` | `{"a":{"b":"d"}}` | 중첩 객체 수정 |

### 배열 전체 교체

| # | 원본 (ORIGINAL) | Patch | 결과 (RESULT) | 설명 |
|---|-----------------|-------|---------------|------|
| 8 | `{"a":[{"b":"c"}]}` | `{"a":[1]}` | `{"a":[1]}` | 배열 전체 교체 |
| 9 | `["a","b"]` | `["c","d"]` | `["c","d"]` | 최상위 배열 교체 |

### 타입 완전 교체

| # | 원본 (ORIGINAL) | Patch | 결과 (RESULT) | 설명 |
|---|-----------------|-------|---------------|------|
| 10 | `{"a":"b"}` | `["c"]` | `["c"]` | 객체를 배열로 교체 |
| 11 | `{"a":"foo"}` | `null` | `null` | 객체를 null로 교체 |
| 12 | `{"a":"foo"}` | `"bar"` | `"bar"` | 객체를 문자열로 교체 |

### 특수 케이스

| # | 원본 (ORIGINAL) | Patch | 결과 (RESULT) | 설명 |
|---|-----------------|-------|---------------|------|
| 13 | `{"e":null}` | `{"a":1}` | `{"e":null,"a":1}` | 기존 null 값 유지 |
| 14 | `[1,2]` | `{"a":"b","c":null}` | `{"a":"b"}` | 배열을 객체로 교체 (null 멤버 무시) |
| 15 | `{}` | `{"a":{"bb":{"ccc":null}}}` | `{"a":{"bb":{}}}` | 깊은 중첩에서 null 처리 |

---

## RFC 6902 (JSON Patch)와의 비교

RFC 7396 (JSON Merge Patch)과 RFC 6902 (JSON Patch)는 모두 JSON 문서를 수정하는 방법을 정의하지만, 접근 방식이 다릅니다.

### 비교표

| 특성 | RFC 7396 (Merge Patch) | RFC 6902 (JSON Patch) |
|------|------------------------|----------------------|
| **형식** | 변경된 상태를 나타내는 JSON 객체 | 작업(operation) 배열 |
| **복잡성** | 단순함 | 더 복잡함 |
| **MIME 타입** | `application/merge-patch+json` | `application/json-patch+json` |
| **배열 수정** | 전체 교체만 가능 | 인덱스 기반 개별 수정 가능 |
| **null 값 설정** | 불가능 (null = 삭제) | 가능 |
| **테스트 연산** | 없음 | test 연산 지원 |
| **이동/복사** | 없음 | move, copy 연산 지원 |

### JSON Patch (RFC 6902) 예시

```json
[
  { "op": "replace", "path": "/title", "value": "Hello!" },
  { "op": "add", "path": "/phoneNumber", "value": "+01-123-456-7890" },
  { "op": "remove", "path": "/author/familyName" },
  { "op": "replace", "path": "/tags", "value": ["example"] }
]
```

### JSON Merge Patch (RFC 7396) 예시

```json
{
  "title": "Hello!",
  "phoneNumber": "+01-123-456-7890",
  "author": {
    "familyName": null
  },
  "tags": ["example"]
}
```

### 언제 어떤 것을 사용해야 하는가?

#### JSON Merge Patch (RFC 7396) 권장

- 단순한 객체 수정이 필요한 경우
- 배열 전체 교체가 허용되는 경우
- null 값이 데이터로 사용되지 않는 경우
- 직관적이고 읽기 쉬운 형식이 필요한 경우

#### JSON Patch (RFC 6902) 권장

- 배열의 특정 요소만 수정해야 하는 경우
- null을 실제 값으로 설정해야 하는 경우
- 테스트 연산(조건부 수정)이 필요한 경우
- 복잡한 이동/복사 작업이 필요한 경우

### 기능 비교 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                      JSON 문서 수정 방법 비교                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  JSON Merge Patch (RFC 7396)       JSON Patch (RFC 6902)           │
│  ════════════════════════════      ══════════════════════          │
│                                                                     │
│  ✓ 단순하고 직관적                  ✓ 풍부한 기능                     │
│  ✓ 문서 구조와 유사                 ✓ 정밀한 배열 조작                 │
│  ✓ 학습 곡선 낮음                   ✓ null 값 설정 가능                │
│  ✓ 적은 페이로드 크기               ✓ 조건부 적용 (test)              │
│                                    ✓ move, copy 연산                │
│                                                                     │
│  ✗ 배열 부분 수정 불가              ✗ 더 복잡한 형식                   │
│  ✗ null 값 설정 불가                ✗ 학습 곡선 높음                   │
│  ✗ 조건부 적용 불가                 ✗ 더 큰 페이로드 크기              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 구현 예시

### JavaScript 구현

```javascript
function mergePatch(target, patch) {
  // Patch가 객체가 아닌 경우 전체 교체
  if (typeof patch !== 'object' || patch === null || Array.isArray(patch)) {
    return patch;
  }

  // Target이 객체가 아닌 경우 빈 객체로 초기화
  if (typeof target !== 'object' || target === null || Array.isArray(target)) {
    target = {};
  }

  // 각 Patch 멤버 처리
  for (const key of Object.keys(patch)) {
    if (patch[key] === null) {
      // null 값은 삭제를 의미
      delete target[key];
    } else {
      // 재귀적 병합
      target[key] = mergePatch(target[key], patch[key]);
    }
  }

  return target;
}

// 사용 예시
const original = {
  title: "Goodbye!",
  author: { givenName: "John", familyName: "Doe" },
  tags: ["example", "sample"]
};

const patch = {
  title: "Hello!",
  author: { familyName: null },
  tags: ["example"]
};

const result = mergePatch(original, patch);
console.log(JSON.stringify(result, null, 2));
```

### Python 구현

```python
def merge_patch(target, patch):
    """RFC 7396 JSON Merge Patch 구현"""

    # Patch가 dict가 아닌 경우 전체 교체
    if not isinstance(patch, dict):
        return patch

    # Target이 dict가 아닌 경우 빈 dict로 초기화
    if not isinstance(target, dict):
        target = {}
    else:
        # 원본을 수정하지 않기 위해 복사
        target = target.copy()

    # 각 Patch 멤버 처리
    for key, value in patch.items():
        if value is None:
            # None 값은 삭제를 의미
            target.pop(key, None)
        else:
            # 재귀적 병합
            target[key] = merge_patch(target.get(key), value)

    return target

# 사용 예시
original = {
    "title": "Goodbye!",
    "author": {"givenName": "John", "familyName": "Doe"},
    "tags": ["example", "sample"]
}

patch = {
    "title": "Hello!",
    "author": {"familyName": None},
    "tags": ["example"]
}

result = merge_patch(original, patch)
print(result)
```

---

## 감사의 말 (Acknowledgments)

이 문서에 기여해주신 분들: James Manger, Matt Miller, Carsten Bormann, Bjoern Hoehrmann, Pete Resnick, Richard Barnes

---

## 저자 주소 (Authors' Addresses)

**Paul Hoffman**
- 소속: VPN Consortium
- 이메일: paul.hoffman@vpnc.org

**James M. Snell**
- 이메일: jasnell@gmail.com

---

## 참고 자료

- [RFC 7396 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc7396)
- [RFC 7396 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc7396)
- [RFC 6902 - JSON Patch](https://datatracker.ietf.org/doc/html/rfc6902)
- [RFC 5789 - PATCH Method for HTTP](https://datatracker.ietf.org/doc/html/rfc5789)
- [RFC 7159 - The JavaScript Object Notation (JSON) Data Interchange Format](https://datatracker.ietf.org/doc/html/rfc7159)

---

## 관련 RFC

| RFC | 제목 | 관계 |
|-----|------|------|
| **RFC 5789** | PATCH Method for HTTP | HTTP PATCH 메서드 정의 |
| **RFC 6902** | JavaScript Object Notation (JSON) Patch | 대안적 JSON 패치 형식 |
| **RFC 7159** | The JSON Data Interchange Format | JSON 형식 명세 |
| **RFC 7386** | JSON Merge Patch (폐기됨) | RFC 7396으로 대체됨 |

---

*이 문서는 RFC 7396의 한국어 번역 및 해설본입니다.*
