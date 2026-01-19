# RFC 9457 - Problem Details for HTTP APIs

> HTTP API의 오류 상세 정보를 기계 판독 가능한 형식으로 전달하기 위한 표준

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 9457 |
| **제목** | Problem Details for HTTP APIs |
| **발행일** | 2023년 7월 |
| **대체 문서** | RFC 7807 |
| **상태** | Standards Track |
| **작성자** | M. Nottingham, E. Wilde, S. Dalal |

---

## 1. 개요

### 1.1 배경 및 필요성

HTTP 상태 코드는 항상 오류에 대한 충분한 정보를 전달하지 못합니다. 예를 들어 `403 Forbidden` 응답은 접근이 거부되었음을 알려주지만, **왜** 거부되었는지는 알려주지 않습니다.

이 명세는 HTTP API에서 기계 판독 가능한 오류 상세 정보를 전달하기 위한 표준 형식을 정의합니다. 이를 통해:

- 각 API마다 새로운 오류 응답 형식을 정의할 필요가 없음
- 클라이언트가 오류를 프로그래밍 방식으로 처리할 수 있음
- 사람이 읽을 수 있는 설명도 함께 제공

### 1.2 핵심 개념

```
┌─────────────────────────────────────────────────────────────┐
│                    Problem Details 객체                       │
├─────────────────────────────────────────────────────────────┤
│  type      : 문제 유형을 식별하는 URI                          │
│  status    : HTTP 상태 코드 (자문용)                           │
│  title     : 문제 유형의 간단한 설명                           │
│  detail    : 이 발생에 대한 구체적인 설명                       │
│  instance  : 이 특정 발생을 식별하는 URI                        │
│  + 확장 멤버들...                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 요구사항 언어

이 문서에서 사용된 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL"이라는 키워드는 BCP 14 [RFC 2119] [RFC 8174]에 기술된 대로 해석되어야 합니다.

---

## 3. Problem Details JSON 객체

Problem Details의 표준 모델은 JSON [RFC 8259] 객체이며, 미디어 타입은 `application/problem+json`입니다.

### 3.1 기본 예시

```json
{
  "type": "https://example.com/probs/out-of-credit",
  "title": "잔액이 부족합니다.",
  "detail": "현재 잔액은 30이지만, 해당 작업에는 50이 필요합니다.",
  "instance": "/account/12345/msgs/abc",
  "balance": 30,
  "accounts": ["/account/12345", "/account/67890"]
}
```

이 예시에서:
- `type`: 문제 유형을 식별하는 URI
- `title`: 사람이 읽을 수 있는 간단한 설명
- `detail`: 이 특정 발생에 대한 설명
- `instance`: 이 문제 발생을 식별하는 URI
- `balance`, `accounts`: 이 문제 유형에 특화된 확장 멤버

---

## 4. Problem Details 객체의 멤버

### 4.1 "type" 멤버

```
목적: 문제 유형을 식별하는 URI 참조
형식: JSON 문자열 (URI)
기본값: "about:blank"
```

**특징**:
- 소비자는 이 URI를 **문제 유형의 주요 식별자**로 사용해야 합니다(SHOULD)
- HTTP/HTTPS 스키마를 사용하는 경우, 역참조하면 사람이 읽을 수 있는 문서를 제공해야 합니다(SHOULD)
- **절대 URI 사용을 권장**(RECOMMENDED)합니다
- 존재하지 않을 경우 `about:blank`으로 간주됩니다

**예시**:
```json
{
  "type": "https://example.com/probs/out-of-credit"
}
```

```json
{
  "type": "urn:ietf:params:problem:insufficient-permissions"
}
```

> **주의**: 소비자는 type URI를 자동으로 역참조해서는 안 됩니다(SHOULD NOT). 이는 불필요한 네트워크 트래픽을 발생시킬 수 있습니다.

### 4.2 "status" 멤버

```
목적: 원본 서버가 생성한 HTTP 상태 코드
형식: JSON 숫자 (100-599)
필수: 아니오
```

**특징**:
- 생성자는 실제 HTTP 응답에서 동일한 상태 코드를 사용해야 합니다(SHOULD)
- 이 값은 **자문 정보**(advisory)입니다
- 중개자(프록시, 캐시 등)에 의해 상태 코드가 변경된 경우 유용합니다

**예시**:
```json
{
  "type": "https://example.com/probs/not-found",
  "status": 404,
  "title": "리소스를 찾을 수 없습니다."
}
```

### 4.3 "title" 멤버

```
목적: 문제 유형에 대한 짧은 사람이 읽을 수 있는 요약
형식: JSON 문자열
필수: 아니오
```

**특징**:
- 발생 간에 **변경되지 않아야** 합니다(SHOULD NOT)
- 지역화(localization)는 예외적으로 허용됩니다
- type URI의 의미를 모르는 사용자를 위한 자문 정보입니다

**올바른 사용**:
```json
{
  "type": "https://example.com/probs/validation-error",
  "title": "입력 유효성 검사 실패"
}
```

**잘못된 사용** (발생마다 변경됨):
```json
{
  "title": "필드 'email'에서 입력 유효성 검사 실패"
}
```

### 4.4 "detail" 멤버

```
목적: 이 특정 문제 발생에 대한 사람이 읽을 수 있는 설명
형식: JSON 문자열
필수: 아니오
```

**특징**:
- **클라이언트가 문제를 수정하도록 도와야** 합니다
- 디버깅 정보보다는 **해결책에 초점**을 맞춥니다
- 소비자는 이 멤버를 **파싱해서 정보를 추출하지 않아야** 합니다(SHOULD NOT)

**좋은 예시**:
```json
{
  "detail": "현재 잔액은 30원이지만, 해당 작업에는 50원이 필요합니다. 충전 후 다시 시도해주세요."
}
```

**나쁜 예시** (디버깅 정보):
```json
{
  "detail": "NullPointerException at line 42 in PaymentService.java"
}
```

### 4.5 "instance" 멤버

```
목적: 이 특정 문제 발생을 식별하는 URI 참조
형식: JSON 문자열 (URI)
필수: 아니오
```

**특징**:
- 역참조 가능하면 문제에 대한 추가 정보를 제공할 수 있습니다
- **지원 또는 법의학(forensic) 목적**으로 유용합니다
- 상대 URI도 가능하지만 **절대 URI가 권장**됩니다

**예시**:
```json
{
  "instance": "/errors/12345",
  "detail": "요청 처리 중 오류가 발생했습니다. 지원팀에 문의 시 위 ID를 알려주세요."
}
```

```json
{
  "instance": "urn:uuid:d9e35127-e9b1-4c72-9a3d-1f3d8b6a3d4e"
}
```

---

## 5. 확장 멤버 (Extension Members)

### 5.1 개요

문제 유형 정의는 **해당 문제 유형에 특정한 추가 멤버**로 확장할 수 있습니다.

```json
{
  "type": "https://example.com/probs/out-of-credit",
  "title": "잔액이 부족합니다.",
  "detail": "현재 잔액은 30원입니다.",
  "balance": 30,
  "accounts": ["/account/12345", "/account/67890"]
}
```

위 예시에서 `balance`와 `accounts`는 `out-of-credit` 문제 유형에 특화된 확장 멤버입니다.

### 5.2 확장 멤버 규칙

| 규칙 | 설명 |
|------|------|
| **이름 시작** | 알파벳 문자로 시작해야 함 |
| **허용 문자** | 알파벳, 숫자, 언더스코어(`_`) |
| **최소 길이** | 3자 이상 |
| **대소문자** | 소문자 권장 |

### 5.3 하위 호환성

클라이언트는 **인식하지 못하는 모든 확장을 무시**해야 합니다(MUST). 이를 통해:
- 문제 유형의 미래 진화를 허용
- 이전 버전 클라이언트와의 호환성 유지

```javascript
// 클라이언트 코드 예시
function handleProblem(problem) {
  // 표준 멤버 처리
  console.log(`오류: ${problem.title}`);
  console.log(`상세: ${problem.detail}`);

  // 알려진 확장 멤버 처리 (선택적)
  if (problem.balance !== undefined) {
    console.log(`현재 잔액: ${problem.balance}`);
  }

  // 알 수 없는 확장은 자동으로 무시됨
}
```

---

## 6. 새로운 문제 유형 정의하기

### 6.1 정의 전 고려사항

새로운 문제 유형을 정의하기 전에 다음을 고려해야 합니다:

1. **기존 HTTP 상태 코드로 충분한가?**
   - 일반적인 문제(예: "쓰기 접근 거부")는 표준 상태 코드(`403 Forbidden`)가 더 적절할 수 있습니다

2. **보안 위험 평가**
   - 구현 내부 정보를 노출하여 공격 벡터를 제공할 위험이 있는가?
   - 시스템 손상, 접근 권한 악용, 사용자 개인정보 침해 가능성은?

3. **Problem Details의 목적 이해**
   - 디버깅 도구가 **아님**
   - HTTP 인터페이스 자체에 대한 세부 정보를 노출하는 방식

### 6.2 문제 유형 정의 요소

새로운 문제 유형을 정의할 때 다음을 문서화해야 합니다:

| 요소 | 설명 | 필수 |
|------|------|------|
| **type URI** | 문제를 고유하게 식별하는 URI | 예 |
| **title** | 문제 유형의 적절한 제목 | 예 |
| **HTTP 상태 코드** | 이 문제 유형과 함께 사용할 상태 코드 | 예 |
| **확장 멤버** | 추가적인 멤버와 그 의미 | 아니오 |

### 6.3 예시: 잔액 부족 문제 유형

```
문제 유형 정의: out-of-credit

Type URI: https://example.com/probs/out-of-credit
Title: 잔액이 부족합니다
Recommended HTTP Status: 403 Forbidden

설명:
사용자의 계정 잔액이 요청된 작업을 수행하기에 충분하지 않을 때 발생합니다.

확장 멤버:
- balance (integer): 현재 계정 잔액
- accounts (array of strings): 관련 계정 URI 목록
- cost (integer, optional): 요청된 작업의 비용
```

**사용 예시**:
```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "https://example.com/probs/out-of-credit",
  "title": "잔액이 부족합니다.",
  "status": 403,
  "detail": "현재 잔액은 30원이지만, 해당 작업에는 50원이 필요합니다.",
  "instance": "/account/12345/transactions/abc",
  "balance": 30,
  "cost": 50,
  "accounts": ["/account/12345", "/account/67890"]
}
```

---

## 7. 등록된 문제 유형

### 7.1 HTTP Problem Types 레지스트리

이 명세는 "HTTP Problem Types" 레지스트리를 정의합니다. 이 레지스트리는:

- 공통적으로 널리 사용되는 문제 유형 URI의 재사용을 촉진
- IANA가 관리
- 등록 정책: RFC 8126의 "Specification Required"

### 7.2 about:blank

| 항목 | 값 |
|------|-----|
| **Type URI** | `about:blank` |
| **Title** | HTTP 상태 코드 참조 |
| **권장 HTTP 상태 코드** | 해당 없음 |
| **참조** | RFC 9457 |

**의미**:
- HTTP 상태 코드 이상의 **추가적인 의미가 없음**을 나타냄
- `title`은 권장 HTTP 상태 구문과 동일해야 함 (지역화 가능)
- `type` 멤버가 없는 모든 Problem Details는 암묵적으로 이 URI를 사용

**예시**:
```json
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "요청한 리소스 '/users/999'를 찾을 수 없습니다."
}
```

또는 `type`을 생략:
```json
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "title": "Not Found",
  "status": 404,
  "detail": "요청한 리소스를 찾을 수 없습니다."
}
```

---

## 8. 유효성 검사 오류 예시

여러 오류를 한 번에 전달해야 하는 경우의 예시입니다.

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://example.net/validation-error",
  "title": "요청이 유효하지 않습니다.",
  "status": 400,
  "errors": [
    {
      "detail": "양의 정수여야 합니다.",
      "pointer": "#/age"
    },
    {
      "detail": "'green', 'red', 'blue' 중 하나여야 합니다.",
      "pointer": "#/profile/color"
    }
  ]
}
```

**설명**:
- `errors` 배열은 확장 멤버로, 개별 유효성 검사 오류를 포함
- `pointer`는 JSON Pointer [RFC 6901] 형식으로 오류 위치를 지정

---

## 9. 보안 고려사항

### 9.1 정보 노출 위험

새로운 문제 유형을 정의할 때 포함되는 정보를 **신중하게 검토**해야 합니다:

| 위험 유형 | 설명 | 예시 |
|----------|------|------|
| **시스템 손상** | 내부 시스템 구조 노출 | 데이터베이스 스키마, 내부 IP |
| **접근 권한 악용** | 공격 벡터 제공 | 유효한 사용자 ID 목록 |
| **개인정보 침해** | 사용자 정보 유출 | 다른 사용자의 이메일 |

### 9.2 피해야 할 정보

```json
// 잘못된 예시 - 구현 세부사항 노출
{
  "type": "https://example.com/probs/database-error",
  "title": "데이터베이스 오류",
  "detail": "PostgreSQL error at /var/lib/postgresql/14/main: connection refused to db-server-01.internal.example.com:5432",
  "stackTrace": "at com.example.db.ConnectionPool.getConnection(ConnectionPool.java:142)..."
}
```

```json
// 올바른 예시 - 필요한 정보만 제공
{
  "type": "https://example.com/probs/service-unavailable",
  "title": "서비스를 일시적으로 사용할 수 없습니다.",
  "status": 503,
  "detail": "잠시 후 다시 시도해주세요.",
  "instance": "/errors/abc123"
}
```

### 9.3 instance 링크 주의사항

`instance` 멤버에서 상세 정보 링크를 제공할 때:
- **스택 덤프**와 같은 구현 세부사항을 HTTP 인터페이스를 통해 공개하지 않도록 권장
- 링크된 리소스에 대한 적절한 접근 제어 구현
- 민감한 정보가 포함된 경우 인증 요구

### 9.4 상태 불일치

`status` 멤버와 실제 HTTP 상태 코드가 불일치할 수 있습니다:

```
시나리오: 프록시가 상태 코드를 변경

원본 서버 → 프록시 → 클라이언트
    503        200 (캐시됨)

Problem Details:
{
  "status": 503,  // 원본 서버의 상태
  "title": "서비스 불가"
}

실제 HTTP 응답: 200 OK
```

클라이언트는 이러한 불일치를 적절히 처리해야 합니다.

---

## 10. IANA 고려사항

### 10.1 미디어 타입 등록

#### application/problem+json

| 항목 | 값 |
|------|-----|
| **타입 이름** | application |
| **서브타입 이름** | problem+json |
| **필수 파라미터** | 없음 |
| **선택적 파라미터** | 없음 |
| **인코딩** | RFC 8259, Section 8.1 |
| **보안 고려사항** | RFC 9457, Section 5 |

#### application/problem+xml

| 항목 | 값 |
|------|-----|
| **타입 이름** | application |
| **서브타입 이름** | problem+xml |
| **필수 파라미터** | 없음 |
| **선택적 파라미터** | 없음 |
| **인코딩** | UTF-8 |
| **보안 고려사항** | RFC 9457, Section 5 |

### 10.2 HTTP Problem Types 레지스트리

| 등록 항목 | 값 |
|----------|-----|
| **Type URI** | about:blank |
| **Title** | See HTTP Status Code |
| **권장 HTTP 상태 코드** | N/A |
| **참조** | RFC 9457, Section 4.2.1 |

---

## 11. XML 형식 (부록 B)

XML 기반 API를 위한 대체 표현입니다.

### 11.1 미디어 타입

```
application/problem+xml
```

### 11.2 네임스페이스

```
urn:ietf:rfc:9457
```

### 11.3 예시

```xml
<?xml version="1.0" encoding="UTF-8"?>
<problem xmlns="urn:ietf:rfc:9457">
  <type>https://example.com/probs/out-of-credit</type>
  <title>잔액이 부족합니다.</title>
  <detail>현재 잔액은 30원이지만, 해당 작업에는 50원이 필요합니다.</detail>
  <instance>/account/12345/msgs/abc</instance>
  <balance>30</balance>
  <accounts>
    <i>/account/12345</i>
    <i>/account/67890</i>
  </accounts>
</problem>
```

**XML 형식 규칙**:
- 배열은 여러 `<i>` 요소를 포함하는 요소로 표현
- 확장 멤버는 동일한 네임스페이스에 포함
- XSLT를 통한 클라이언트 측 HTML 변환 지원

---

## 12. JSON Schema (부록 A)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "urn:ietf:rfc:9457",
  "title": "Problem Details",
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "format": "uri-reference",
      "description": "문제 유형을 식별하는 URI 참조",
      "default": "about:blank"
    },
    "status": {
      "type": "integer",
      "description": "원본 서버가 생성한 HTTP 상태 코드",
      "minimum": 100,
      "maximum": 599
    },
    "title": {
      "type": "string",
      "description": "문제 유형에 대한 짧은 사람이 읽을 수 있는 요약"
    },
    "detail": {
      "type": "string",
      "description": "이 특정 문제 발생에 대한 사람이 읽을 수 있는 설명"
    },
    "instance": {
      "type": "string",
      "format": "uri-reference",
      "description": "이 특정 문제 발생을 식별하는 URI 참조"
    }
  },
  "additionalProperties": true
}
```

---

## 13. RFC 7807과의 변경사항 (부록 D)

RFC 9457은 RFC 7807을 대체하며, 다음과 같은 주요 개선사항이 있습니다:

| 변경 사항 | 설명 |
|----------|------|
| **레지스트리 도입** | 일반적인 문제 유형 URI를 위한 IANA 레지스트리 생성 |
| **여러 문제 처리** | 여러 문제를 한 응답에서 처리하는 방식 명확화 |
| **역참조 불가 URI** | `urn:` 등 역참조 불가능한 type URI 사용 지침 제공 |
| **네임스페이스 업데이트** | XML 네임스페이스가 `urn:ietf:rfc:9457`로 변경 |

---

## 14. 실제 사용 예시

### 14.1 인증 오류

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json
WWW-Authenticate: Bearer realm="api"

{
  "type": "https://api.example.com/probs/authentication-required",
  "title": "인증이 필요합니다.",
  "status": 401,
  "detail": "유효한 액세스 토큰을 제공해주세요.",
  "instance": "/login"
}
```

### 14.2 권한 부족

```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "https://api.example.com/probs/insufficient-permissions",
  "title": "권한이 부족합니다.",
  "status": 403,
  "detail": "이 리소스에 접근하려면 'admin' 역할이 필요합니다.",
  "required_role": "admin",
  "current_roles": ["user", "viewer"]
}
```

### 14.3 요청 속도 제한

```json
HTTP/1.1 429 Too Many Requests
Content-Type: application/problem+json
Retry-After: 60

{
  "type": "https://api.example.com/probs/rate-limit-exceeded",
  "title": "요청 속도 제한 초과",
  "status": 429,
  "detail": "1분에 100개의 요청만 허용됩니다. 60초 후에 다시 시도해주세요.",
  "limit": 100,
  "remaining": 0,
  "reset": "2023-07-15T10:30:00Z"
}
```

### 14.4 리소스 충돌

```json
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://api.example.com/probs/resource-conflict",
  "title": "리소스 충돌",
  "status": 409,
  "detail": "이미 동일한 이메일 주소로 등록된 사용자가 있습니다.",
  "conflicting_field": "email",
  "conflicting_value": "user@example.com"
}
```

### 14.5 내부 서버 오류 (보안 고려)

```json
HTTP/1.1 500 Internal Server Error
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "요청을 처리하는 중 예기치 않은 오류가 발생했습니다. 문제가 지속되면 지원팀에 문의해주세요.",
  "instance": "/errors/err-20230715-abc123"
}
```

> **참고**: 내부 오류의 경우 스택 트레이스나 시스템 정보를 노출하지 않고, `instance`로 내부 로그를 참조할 수 있는 ID만 제공합니다.

---

## 15. 클라이언트 구현 가이드

### 15.1 Problem Details 처리 흐름

```
┌─────────────────┐
│   HTTP 응답     │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ Content-Type이 application/problem+json인가? │
└────────┬────────────────────────────┘
         │
    예 ──┼── 아니오 → 일반 오류 처리
         │
         ▼
┌─────────────────────────────────────┐
│     Problem Details 파싱            │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│     type 멤버 확인                  │
└────────┬────────────────────────────┘
         │
    알려진 타입 ──┼── 알 수 없는 타입
         │              │
         ▼              ▼
┌───────────────┐ ┌───────────────────┐
│ 특화된 처리   │ │ 일반적인 처리     │
│ (확장 멤버   │ │ (title, detail    │
│  활용)       │ │  표시)            │
└───────────────┘ └───────────────────┘
```

### 15.2 JavaScript 예시

```javascript
async function handleApiError(response) {
  const contentType = response.headers.get('content-type');

  if (contentType && contentType.includes('application/problem+json')) {
    const problem = await response.json();

    // 알려진 문제 유형 처리
    switch (problem.type) {
      case 'https://api.example.com/probs/out-of-credit':
        return handleOutOfCredit(problem);

      case 'https://api.example.com/probs/validation-error':
        return handleValidationError(problem);

      default:
        // 알 수 없는 문제 유형 - 일반 처리
        return handleGenericProblem(problem);
    }
  }

  // Problem Details가 아닌 경우
  throw new Error(`HTTP ${response.status}: ${response.statusText}`);
}

function handleOutOfCredit(problem) {
  console.log(`잔액 부족: 현재 ${problem.balance}원`);
  // 충전 페이지로 안내
}

function handleValidationError(problem) {
  if (problem.errors) {
    problem.errors.forEach(error => {
      console.log(`${error.pointer}: ${error.detail}`);
    });
  }
}

function handleGenericProblem(problem) {
  console.log(problem.title || 'Unknown Error');
  console.log(problem.detail || 'An error occurred');
}
```

---

## 16. 요약

RFC 9457은 HTTP API의 오류 응답을 위한 표준 형식을 정의합니다:

| 항목 | 설명 |
|------|------|
| **목적** | 기계 판독 가능한 오류 상세 정보 전달 |
| **미디어 타입** | `application/problem+json`, `application/problem+xml` |
| **핵심 멤버** | type, status, title, detail, instance |
| **확장성** | 문제 유형별 확장 멤버 정의 가능 |
| **하위 호환성** | 알 수 없는 확장은 무시해야 함 |
| **보안** | 민감한 정보 노출 금지 |

이 표준을 사용하면:
- API 간 일관된 오류 응답 형식 제공
- 클라이언트의 프로그래밍 방식 오류 처리 용이
- 사람이 읽을 수 있는 설명 함께 제공
- 확장 가능하고 미래 호환성 있는 설계

---

## 참고 자료

- [RFC 9457 원문](https://www.rfc-editor.org/rfc/rfc9457)
- [RFC 7807 (이전 버전)](https://www.rfc-editor.org/rfc/rfc7807)
- [RFC 8259 - JSON](https://www.rfc-editor.org/rfc/rfc8259)
- [RFC 6901 - JSON Pointer](https://www.rfc-editor.org/rfc/rfc6901)
- [IANA HTTP Problem Types Registry](https://www.iana.org/assignments/http-problem-types/)

---

## 관련 RFC

- **RFC 9110**: HTTP Semantics
- **RFC 8259**: JSON Data Interchange Format
- **RFC 7807**: Problem Details for HTTP APIs (이전 버전)
- **RFC 6901**: JavaScript Object Notation (JSON) Pointer
- **RFC 2119**: Key words for use in RFCs

---

*이 문서는 RFC 9457의 한국어 번역 및 정리본입니다.*
