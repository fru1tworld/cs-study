# RFC 5789: HTTP PATCH 메서드

> HTTP 리소스의 부분 수정을 위한 PATCH 메서드 명세

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 5789 |
| **제목** | PATCH Method for HTTP |
| **분류** | Proposed Standard (IETF 표준 트랙) |
| **작성자** | Lisa M. Dusseault (Linden Lab), James M. Snell |
| **발행일** | 2010년 3월 |
| **상태** | 인터넷 표준 트랙 문서 (IETF 커뮤니티 합의) |

---

## 개요 (Abstract)

이 명세는 리소스의 **부분 수정(partial modifications)** 을 위한 새로운 HTTP 메서드인 PATCH를 도입합니다. 리소스를 완전히 대체하는 PUT과 달리, PATCH는 대상을 지정한 업데이트를 가능하게 합니다. 이는 문서의 점진적인 변경이 필요한 애플리케이션에서의 상호운용성 문제를 해결합니다.

---

## 1. 소개 (Introduction)

이 문서는 리소스에 부분 수정을 적용하기 위한 HTTP/1.1 PATCH 메서드를 정의합니다.

### PATCH가 필요한 이유

| 기존 메서드 | 한계점 |
|------------|--------|
| **PUT** | 리소스 전체를 덮어쓰기 때문에 부분 수정에 비효율적 |
| **POST** | 표준화된 패치 형식 발견 메커니즘이 없음 |

이전 HTTP 명세에서도 PATCH를 언급했지만 완전한 정의가 없었습니다. RFC 5789는 이 공백을 채웁니다.

### 용어

이 문서에서 사용하는 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL" 키워드는 RFC 2119에 기술된 대로 해석되어야 합니다.

### ABNF 문법

RFC 2616 섹션 2.1의 규약을 따릅니다.

---

## 2. PATCH 메서드

### 2.1 핵심 기능

PATCH 메서드는 식별된 리소스에 대해 요청 엔티티에 기술된 변경 사항을 처리합니다. 변경 사항은 특정 미디어 타입으로 식별되는 **"패치 문서(patch document)"** 형태로 나타납니다.

- Request-URI가 기존 리소스를 참조하지 않는 경우, 서버는 패치 문서 유형과 권한에 따라 새 리소스를 생성할 수 있습니다(MAY).

### 2.2 PUT과 PATCH의 차이

| 메서드 | 동작 방식 |
|--------|----------|
| **PUT** | 동봉된 엔티티가 **수정된 완전한 리소스를 표현**; 클라이언트는 저장된 버전의 교체를 요청 |
| **PATCH** | 동봉된 엔티티가 **기존 리소스를 수정하는 방법을 설명하는 명령어 집합을 포함**; 새 버전을 생성하기 위한 지침 |

```
┌─────────────────────────────────────────────────────────────────┐
│                        PUT vs PATCH                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PUT: 완전한 리소스 교체                                         │
│  ┌──────────────┐     PUT      ┌──────────────┐                │
│  │ 원본 리소스   │ ──────────→ │ 새 리소스     │                │
│  │ {            │   (전체교체)  │ {            │                │
│  │   "a": 1,    │             │   "a": 2,    │                │
│  │   "b": 2     │             │   "c": 3     │                │
│  │ }            │             │ }            │                │
│  └──────────────┘             └──────────────┘                │
│                                                                 │
│  PATCH: 부분적 수정                                             │
│  ┌──────────────┐    PATCH     ┌──────────────┐                │
│  │ 원본 리소스   │ ──────────→ │ 수정된 리소스  │                │
│  │ {            │ (변경 지침만) │ {            │                │
│  │   "a": 1,    │             │   "a": 2,    │                │
│  │   "b": 2     │             │   "b": 2     │  ← b 유지       │
│  │ }            │             │ }            │                │
│  └──────────────┘             └──────────────┘                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 주요 특성

#### 안전성과 멱등성 (Safety & Idempotency)

RFC 2616 섹션 9.1에 따라 PATCH는 **안전하지도 않고 멱등하지도 않습니다**.

| 특성 | PATCH | PUT | GET |
|------|-------|-----|-----|
| 안전(Safe) | X | X | O |
| 멱등(Idempotent) | X | O | O |

```
멱등성 비교:

PUT /user/1 {"name": "Kim"}  → 항상 동일한 결과
PUT /user/1 {"name": "Kim"}  → 항상 동일한 결과
PUT /user/1 {"name": "Kim"}  → 항상 동일한 결과

PATCH /user/1 [add 10 to balance]  → balance: 10
PATCH /user/1 [add 10 to balance]  → balance: 20  ← 다른 결과!
PATCH /user/1 [add 10 to balance]  → balance: 30  ← 다른 결과!
```

#### 조건부 요청 (Conditional Requests)

알려진 기준점(base-point)을 요구하는 패치 형식을 사용하는 클라이언트는 충돌을 방지하기 위해 **조건부 요청**을 사용해야 합니다(SHOULD).

- **If-Match**: ETag를 사용한 조건부 요청
- **If-Unmodified-Since**: 타임스탬프 기반 조건부 요청

```http
PATCH /resource HTTP/1.1
If-Match: "abc123"
Content-Type: application/json-patch+json

[패치 문서]
```

#### 원자성 요구사항 (Atomicity Requirement)

서버는 전체 변경 집합을 **원자적으로** 적용해야 합니다(MUST):

- 부분적 수정이 노출되어서는 안 됨
- 완전한 실패 시 어떤 변경도 적용되지 않아야 함

```
원자성 보장:

성공 케이스:                      실패 케이스:
┌─────────────┐                 ┌─────────────┐
│ 변경 1 적용  │                 │ 변경 1 적용  │
│ 변경 2 적용  │                 │ 변경 2 실패! │ ← 에러 발생
│ 변경 3 적용  │                 │ 롤백        │ ← 변경 1도 취소
│ 커밋        │                 │ 원상복구    │
└─────────────┘                 └─────────────┘
```

#### 캐싱 (Caching)

PATCH 응답은 다음 조건을 **모두** 만족할 때만 캐시 가능합니다:

1. 명시적 신선도 정보 포함 (Expires 또는 Cache-Control 헤더)
2. Content-Location 헤더가 Request-URI와 일치

### 2.4 엔티티 헤더

요청의 엔티티 헤더는 **패치 문서 자체에만** 적용됩니다. 수정되는 리소스에는 적용되지 않습니다.

서버는 PUT 요청에서처럼 이러한 헤더를 저장하거나 사용하지 않아야 합니다(SHOULD NOT).

### 2.5 형식 유연성

- 단일 기본 패치 문서 형식은 존재하지 않습니다.
- 서버는 수신된 문서가 Request-URI로 식별된 리소스 유형과 일치하는지 확인해야 합니다(MUST).

### 2.6 PATCH vs PUT 선택 기준

클라이언트는 패치 문서 크기와 대체할 리소스 크기를 비교해야 합니다:

| 상황 | 권장 메서드 |
|------|------------|
| 패치가 전체 리소스보다 작음 | PATCH |
| 패치가 전체 리소스와 비슷하거나 큼 | PUT |

---

## 3. 간단한 PATCH 예시

### 요청 예시

```http
PATCH /file.txt HTTP/1.1
Host: www.example.com
Content-Type: application/example
If-Match: "e0023aa4e"
Content-Length: 100

[변경 사항 설명]
```

### 성공 응답 예시

```http
HTTP/1.1 204 No Content
Content-Location: /file.txt
ETag: "e0023aa4f"
```

**설명**:
- **204 상태 코드**: 응답 본문이 없음을 나타냄
- **ETag**: PATCH 적용 후의 리소스 상태를 반영
- **Content-Location**: 리소스 위치를 표시

### JSON Patch 예시 (실무 활용)

```http
PATCH /users/123 HTTP/1.1
Host: api.example.com
Content-Type: application/json-patch+json
If-Match: "version-42"

[
  { "op": "replace", "path": "/email", "value": "new@example.com" },
  { "op": "add", "path": "/phone", "value": "010-1234-5678" },
  { "op": "remove", "path": "/fax" }
]
```

---

## 4. 오류 처리 (Error Handling)

RFC 5789는 다양한 실패 시나리오에 대한 적절한 상태 코드를 명시합니다:

| 조건 | HTTP 상태 코드 | 설명 |
|------|---------------|------|
| **잘못된 형식의 패치** | 400 Bad Request | 부적절하게 형식화된 패치 문서 |
| **지원하지 않는 형식** | 415 Unsupported Media Type | 서버가 패치 문서 형식을 지원하지 않음; Accept-Patch 헤더를 포함해야 함(SHOULD) |
| **처리 불가능한 요청** | 422 Unprocessable Entity | 서버가 구문은 이해하지만 처리할 수 없음; 유효하지 않은 리소스를 생성하는 수정 포함 |
| **리소스를 찾을 수 없음** | 404 Not Found | 존재하지 않는 리소스에 부적합한 형식으로 패치 시도 |
| **상태 충돌** | 409 Conflict | 리소스 상태로 인해 요청을 적용할 수 없음; 가정된 구조가 존재하지 않음 |
| **수정 충돌** | 412 Precondition Failed | If-Match 또는 If-Unmodified-Since 전제조건 실패 |
| **동시 수정** | 409 Conflict | 서버가 동일 리소스에 대한 동시 요청을 수신했지만 큐에 넣을 수 없음 |

### 오류 응답 형식

```
상태 코드별 응답 흐름:

클라이언트 요청                    서버 응답
     │                              │
     │  PATCH /resource             │
     │  Content-Type: text/invalid  │
     │ ──────────────────────────→  │
     │                              │ 형식 검사 실패
     │  ←────────────────────────── │
     │  415 Unsupported Media Type  │
     │  Accept-Patch: app/json      │
     │                              │

```

**409 Conflict 응답**은 일관된 정보를 제공하여 클라이언트가:
- 요청을 재발행하거나
- 패치를 재계산하거나
- 작업을 실패로 처리할 수 있게 합니다.

오류 응답 엔티티는 클라이언트에게 오류의 성격을 설명하는 **충분한 정보를 포함해야 합니다(SHOULD)**.

---

## 5. OPTIONS에서 지원 알림 (Advertising Support in OPTIONS)

### 5.1 Accept-Patch 헤더

서버가 수락하는 패치 문서 형식을 지정하는 새로운 응답 헤더입니다.

**특성**:
- PATCH를 지원하는 리소스의 OPTIONS 응답에 나타나야 합니다(SHOULD)
- 헤더의 존재는 PATCH가 허용됨을 나타냄
- 헤더의 특정 형식은 지원되는 형식을 나타냄

**문법**:
```
Accept-Patch = "Accept-Patch" ":" 1#media-type
```

**예시**:
```http
Accept-Patch: text/example;charset=utf-8
```

### 5.2 OPTIONS 요청 및 응답 예시

**요청**:
```http
OPTIONS /example/buddies.xml HTTP/1.1
Host: www.example.com
```

**응답**:
```http
HTTP/1.1 200 OK
Allow: GET, PUT, POST, OPTIONS, HEAD, DELETE, PATCH
Accept-Patch: application/example, text/example
```

이 예시는 서버가 두 가지 가상의 패치 문서 형식으로 PATCH를 지원함을 보여줍니다.

---

## 6. IANA 고려사항 (IANA Considerations)

### Accept-Patch 응답 헤더 등록

Accept-Patch 헤더가 IANA 영구 레지스트리에 다음 세부사항으로 추가되었습니다:

| 항목 | 값 |
|------|-----|
| **헤더 필드 이름** | Accept-Patch |
| **적용 프로토콜** | HTTP |
| **저자/변경 관리자** | IETF |
| **명세 문서** | RFC 5789 |

---

## 7. 보안 고려사항 (Security Considerations)

보안 고려사항은 PUT 메서드 보안(RFC 2616, 섹션 9.6)과 거의 동일합니다:

### 기본 보안 요구사항

| 요구사항 | 설명 |
|----------|------|
| **요청 인가** | 접근 제어 및 인증을 통한 요청 인가 |
| **데이터 무결성** | 전송 오류 및 우발적 덮어쓰기에 대한 데이터 무결성 보호 |

### PATCH 특정 우려사항

#### 손상 위험 (Corruption Risk)

패치된 문서는 완전히 교체된 문서보다 **손상에 더 취약**할 수 있습니다.

**완화 방법**: ETag와 If-Match 헤더를 사용한 조건부 요청

```http
PATCH /document HTTP/1.1
If-Match: "current-etag-value"    ← 손상 방지를 위한 조건부 요청
Content-Type: application/json-patch+json

[패치 내용]
```

#### 검증 (Verification)

| 상황 | 대응 방법 |
|------|----------|
| PATCH 실패 시 | GET 요청을 발행하여 리소스 상태 검증 |
| PATCH 응답 수신 전 전송 실패 시 | 캐시를 우회하는 GET 요청을 발행하여 애플리케이션 상태 확인 |

#### 바이러스 탐지 문제 (Virus Detection Challenges)

바이러스를 검사하는 HTTP 중개자는 PATCH에서 복잡한 상황에 직면합니다:

- 소스 문서도, 패치 문서도 개별적으로는 악의적일 필요가 없음
- 그러나 결합 시 악의적인 결과를 생성할 수 있음

이 우려는 다음과 같은 기존 위험과 유사합니다:
- 바이트 범위 다운로드
- 패치 문서
- 압축 파일 업로드
- 유사한 메커니즘

#### 패치 특정 보안

개별 패치 문서는 리소스 유형에 따라 **고유한 보안 고려사항**을 수반합니다:

- 바이너리 리소스 패칭 vs XML 문서 패칭
- 서버는 악의적인 클라이언트가 PATCH 작업을 통해 **과도한 리소스를 소비하지 않도록** 적절한 예방 조치를 취해야 합니다(MUST)

```
리소스 소진 공격 예시:

악의적 클라이언트                   서버
     │                              │
     │  PATCH (매우 큰 패치 문서)     │
     │ ──────────────────────────→  │
     │                              │ 메모리/CPU 과부하
     │  PATCH (매우 큰 패치 문서)     │
     │ ──────────────────────────→  │
     │                              │ 서비스 거부

예방 조치:
- 패치 문서 크기 제한
- 요청 속도 제한 (Rate Limiting)
- 타임아웃 설정
```

---

## 8. 참조 (References)

### 8.1 규범적 참조 (Normative References)

| RFC | 제목 | 저자 | 날짜 |
|-----|------|------|------|
| **RFC 2119** | Key words for use in RFCs to Indicate Requirement Levels | Bradner, S. | 1997년 3월 |
| **RFC 2616** | Hypertext Transfer Protocol -- HTTP/1.1 | Fielding, et al. | 1999년 6월 |
| **RFC 3864** | Registration Procedures for Message Header Fields | Klyne, G., Nottingham, M., Mogul, J. | 2004년 9월 |

### 8.2 정보적 참조 (Informative References)

| RFC | 제목 | 저자 | 날짜 |
|-----|------|------|------|
| **RFC 4918** | HTTP Extensions for Web Distributed Authoring and Versioning (WebDAV) | Dusseault, L. | 2007년 6월 |

---

## 부록 A: 감사의 말 (Acknowledgements)

PATCH는 Roy Fielding과 Henrik Frystyk의 HTTP/1.1 초안에서 시작되었으며, RFC 2068 섹션 19.6.1.1에 등장했습니다.

검토와 조언에 기여한 분들:
- Adam Roach, Chris Sharp, Julian Reschke, Geoff Clemm, Scott Lawrence
- Jeffrey Mogul, Roy Fielding, Greg Stein, Jim Luther, Alex Rousskov
- Jamie Lokier, Joe Hildebrand, Mark Nottingham, Michael Balloni
- Cyrus Daboo, Brian Carpenter, John Klensin, Eliot Lear, Bernie Hoeneisen

특히 Julian Reschke는 반복적인 검토와 출판에 대한 중요한 기여로 특별한 인정을 받았습니다.

---

## 실무 활용 가이드

### 일반적인 PATCH 형식

| 형식 | Content-Type | 설명 |
|------|-------------|------|
| **JSON Patch** | application/json-patch+json | JSON 문서 수정을 위한 표준 형식 (RFC 6902) |
| **JSON Merge Patch** | application/merge-patch+json | 간단한 JSON 병합 (RFC 7386) |
| **XML Patch** | application/xml-patch+xml | XML 문서 수정 (RFC 5261) |

### JSON Patch 작업 유형

```json
[
  { "op": "add", "path": "/newField", "value": "newValue" },
  { "op": "remove", "path": "/oldField" },
  { "op": "replace", "path": "/field", "value": "updatedValue" },
  { "op": "move", "from": "/oldPath", "path": "/newPath" },
  { "op": "copy", "from": "/source", "path": "/destination" },
  { "op": "test", "path": "/field", "value": "expectedValue" }
]
```

### JSON Merge Patch 예시

```http
PATCH /users/123 HTTP/1.1
Content-Type: application/merge-patch+json

{
  "email": "newemail@example.com",
  "phone": null,
  "address": {
    "city": "Seoul"
  }
}
```

**동작**:
- `email`: 값 업데이트
- `phone`: null로 설정하면 필드 삭제
- `address.city`: 중첩된 필드 업데이트

---

## 관련 RFC

| RFC | 제목 | 관계 |
|-----|------|------|
| **RFC 2616** | HTTP/1.1 | 기본 HTTP 명세 |
| **RFC 7231** | HTTP/1.1 Semantics and Content | HTTP 메서드 의미론 |
| **RFC 6902** | JavaScript Object Notation (JSON) Patch | JSON Patch 형식 |
| **RFC 7386** | JSON Merge Patch | JSON Merge Patch 형식 |
| **RFC 5261** | XML Patch Operations Framework | XML Patch 형식 |

---

*이 문서는 RFC 5789의 한국어 번역 및 정리본입니다.*
