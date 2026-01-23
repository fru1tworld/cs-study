# RFC 6585: Additional HTTP Status Codes

> 추가 HTTP 상태 코드

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 6585 |
| **분류** | Standards Track (표준) |
| **작성자** | M. Nottingham (Rackspace), R. Fielding (Adobe) |
| **발행일** | 2012년 4월 |
| **상태** | Proposed Standard |
| **업데이트** | RFC 2616 |

---

## 개요

이 문서는 HTTP에 대한 추가 상태 코드를 명시하여 상호운용성을 개선하고, 오류 조건을 표현하기 위한 공통 의미를 확립합니다. 클라이언트가 인식하지 못하는 상태 코드의 처리를 RFC 2616에서 정의하고 있으므로, 새로운 상태 코드를 안전하게 배포할 수 있습니다.

이 문서에서 사용된 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"이라는 키워드는 RFC 2119에 기술된 대로 해석되어야 합니다.

---

## 상태 코드 정의

### 1. 428 Precondition Required (전제조건 필수)

```
상태 코드: 428
분류: 4xx (클라이언트 오류)
캐시 가능: 아니오
```

#### 정의

428 상태 코드는 원본 서버가 요청을 **조건부로** 제출하도록 요구함을 나타냅니다.

이 상태 코드의 일반적인 용도는 **"분실된 업데이트(lost update)" 문제** 를 방지하는 것입니다. 이 문제는 클라이언트가 리소스의 상태를 GET으로 가져온 후, 수정하여 PUT으로 서버에 다시 전송할 때 발생합니다. 이 사이에 제3자가 서버의 상태를 수정한 경우 충돌이 발생하게 됩니다.

#### 문제 상황 예시

```
시간순서:
┌─────────────────────────────────────────────────────────────┐
│ 1. 클라이언트 A: GET /resource → 버전 1 획득                    │
│ 2. 클라이언트 B: GET /resource → 버전 1 획득                    │
│ 3. 클라이언트 B: PUT /resource → 버전 2로 업데이트              │
│ 4. 클라이언트 A: PUT /resource → 버전 1 기반으로 수정           │
│    ↳ 클라이언트 B의 변경사항이 덮어씌워짐! (분실된 업데이트)     │
└─────────────────────────────────────────────────────────────┘
```

#### 해결 방법

서버가 조건부 요청을 요구하면 이 문제를 회피할 수 있습니다:

```
해결된 시나리오:
┌─────────────────────────────────────────────────────────────┐
│ 1. 클라이언트 A: GET /resource → 버전 1, ETag: "v1"            │
│ 2. 클라이언트 B: GET /resource → 버전 1, ETag: "v1"            │
│ 3. 클라이언트 B: PUT /resource (If-Match: "v1") → 성공         │
│    ↳ 서버: ETag를 "v2"로 업데이트                              │
│ 4. 클라이언트 A: PUT /resource (If-Match: "v1") → 412 실패     │
│    ↳ 서버: ETag가 "v2"이므로 조건 불충족                       │
│ 5. 클라이언트 A: 최신 버전 다시 가져와서 병합 후 재시도          │
└─────────────────────────────────────────────────────────────┘
```

#### 응답 요구사항

428 응답은 **캐시되어서는 안 됩니다(MUST NOT be cached)**.

이 상태 코드를 생성하는 응답은 사용자에게 요청을 성공적으로 (재)제출하는 방법을 설명해야 합니다(SHOULD).

#### 조건부 헤더

| 헤더 | 용도 |
|------|------|
| `If-Match` | 특정 ETag 값과 일치할 때만 요청 수행 |
| `If-None-Match` | 특정 ETag 값과 일치하지 않을 때만 요청 수행 |
| `If-Modified-Since` | 지정된 날짜 이후 수정된 경우에만 요청 수행 |
| `If-Unmodified-Since` | 지정된 날짜 이후 수정되지 않은 경우에만 요청 수행 |

#### 예시 응답

```http
HTTP/1.1 428 Precondition Required
Content-Type: text/html
Date: Fri, 13 Apr 2012 12:00:00 GMT

<!DOCTYPE html>
<html>
<head>
    <title>Precondition Required</title>
</head>
<body>
    <h1>Precondition Required</h1>
    <p>이 리소스를 업데이트하려면 조건부 요청이 필요합니다.</p>
    <p>If-Match 또는 If-Unmodified-Since 헤더를 포함하여
       요청을 다시 제출해주세요.</p>
</body>
</html>
```

#### 주의사항

이 상태 코드의 사용은 **선택적(optional)** 입니다. 클라이언트는 모든 원본 서버가 이를 사용할 것이라고 기대할 수 없으므로, 이 상태 코드가 충돌을 방지해줄 것이라고 의존해서는 안 됩니다.

---

### 2. 429 Too Many Requests (요청 과다)

```
상태 코드: 429
분류: 4xx (클라이언트 오류)
캐시 가능: 아니오
```

#### 정의

429 상태 코드는 사용자가 주어진 시간 동안 **너무 많은 요청을 전송** 했음을 나타냅니다 ("속도 제한" 또는 "rate limiting").

#### 응답 요구사항

429 응답은 **캐시되어서는 안 됩니다(MUST NOT be cached)**.

응답 표현(response representation)은 조건에 대한 세부 정보를 포함해야 합니다(SHOULD). 선택적으로 **Retry-After** 헤더를 포함하여 새 요청을 할 수 있는 시점을 나타낼 수 있습니다(MAY).

#### Retry-After 헤더

| 형식 | 예시 | 설명 |
|------|------|------|
| 초 단위 | `Retry-After: 3600` | 3600초(1시간) 후에 재시도 |
| 날짜 | `Retry-After: Fri, 13 Apr 2012 13:00:00 GMT` | 지정된 시간 이후에 재시도 |

#### 예시 응답

```http
HTTP/1.1 429 Too Many Requests
Content-Type: text/html
Retry-After: 3600
Date: Fri, 13 Apr 2012 12:00:00 GMT

<!DOCTYPE html>
<html>
<head>
    <title>Too Many Requests</title>
</head>
<body>
    <h1>Too Many Requests</h1>
    <p>시간당 허용된 요청 수를 초과했습니다.</p>
    <p>1시간 후에 다시 시도해주세요.</p>
</body>
</html>
```

#### 속도 제한 정책 예시

```
일반적인 API 속도 제한 정책:

┌────────────────────────────────────────────────┐
│ 계층        │ 제한                              │
├────────────────────────────────────────────────┤
│ 분당        │ 60 요청/분                        │
│ 시간당      │ 1,000 요청/시간                   │
│ 일일        │ 10,000 요청/일                    │
│ 사용자별    │ IP 또는 API 키 기준               │
└────────────────────────────────────────────────┘
```

#### 주의사항

이 명세는 원본 서버가 속도 제한을 식별하는 방식을 정의하지 않습니다. 예를 들어:
- 요청 자격 증명을 공유하는 요청들이 동일한 클라이언트로 간주될 수 있음
- 동일한 서버 주소에 대한 요청으로 제한될 수 있음
- 일반적으로 서버 전체 요청에 적용될 수 있음

---

### 3. 431 Request Header Fields Too Large (요청 헤더 필드 과대)

```
상태 코드: 431
분류: 4xx (클라이언트 오류)
캐시 가능: 아니오
```

#### 정의

431 상태 코드는 서버가 **헤더 필드가 너무 크기 때문에** 요청을 처리하지 않겠다는 것을 나타냅니다.

이 상태 코드는 다음 두 가지 경우에 사용될 수 있습니다:
1. **전체 헤더 필드 집합** 이 너무 큰 경우
2. **단일 헤더 필드** 가 너무 큰 경우

#### 응답 요구사항

431 응답은 **캐시되어서는 안 됩니다(MUST NOT be cached)**.

후자의 경우(단일 헤더가 너무 큰 경우), 응답 표현은 어떤 헤더 필드가 너무 컸는지 나타내야 합니다(SHOULD). 이를 통해 사용자가 문제를 해결할 수 있습니다.

#### 예시 응답 (전체 헤더 과대)

```http
HTTP/1.1 431 Request Header Fields Too Large
Content-Type: text/html
Date: Fri, 13 Apr 2012 12:00:00 GMT

<!DOCTYPE html>
<html>
<head>
    <title>Request Header Fields Too Large</title>
</head>
<body>
    <h1>Request Header Fields Too Large</h1>
    <p>요청 헤더의 전체 크기가 서버 제한을 초과했습니다.</p>
    <p>헤더 크기를 줄인 후 요청을 다시 제출해주세요.</p>
</body>
</html>
```

#### 예시 응답 (특정 헤더 과대)

```http
HTTP/1.1 431 Request Header Fields Too Large
Content-Type: text/html
Date: Fri, 13 Apr 2012 12:00:00 GMT

<!DOCTYPE html>
<html>
<head>
    <title>Request Header Fields Too Large</title>
</head>
<body>
    <h1>Request Header Fields Too Large</h1>
    <p>"Cookie" 헤더 필드가 너무 큽니다.</p>
    <p>쿠키를 정리한 후 다시 시도해주세요.</p>
</body>
</html>
```

#### 일반적인 원인

| 원인 | 설명 |
|------|------|
| 과도한 쿠키 | 너무 많거나 큰 쿠키가 설정된 경우 |
| 긴 Authorization 헤더 | 복잡한 인증 토큰 사용 시 |
| 많은 사용자 정의 헤더 | X-Custom-* 헤더가 많은 경우 |
| Referer 헤더 | 매우 긴 URL에서 참조된 경우 |

#### 일반적인 서버 제한

```
서버별 헤더 크기 제한 예시:

┌────────────────────────────────────────────────┐
│ 서버          │ 기본 제한                       │
├────────────────────────────────────────────────┤
│ Apache        │ 8KB (LimitRequestFieldSize)    │
│ Nginx         │ 8KB (large_client_header_buffers) │
│ IIS           │ 16KB                            │
│ Node.js       │ 80KB (--max-http-header-size)   │
└────────────────────────────────────────────────┘
```

---

### 4. 511 Network Authentication Required (네트워크 인증 필요)

```
상태 코드: 511
분류: 5xx (서버 오류)
캐시 가능: 아니오
```

#### 정의

511 상태 코드는 **클라이언트가 네트워크 접근을 얻기 위해 인증해야 함** 을 나타냅니다.

#### 사용 목적

이 응답 표현은 사용자에게 네트워크 접근을 허용할 리소스(예: 자격 증명 제출을 허용하는 HTML 양식이 있는 페이지)에 대한 링크를 포함해야 합니다(SHOULD).

511 응답은 **원본 서버에 의해 생성되어서는 안 됩니다(SHOULD NOT)**. 이 상태 코드는 네트워크 접근을 제어하는 **중간 프록시(intercepting proxies)** 에 의해 사용되도록 의도되었습니다.

#### 캡티브 포털 (Captive Portal)

511 상태 코드는 주로 "캡티브 포털" 또는 "로그인 네트워크"에서 사용됩니다. 캡티브 포털은 다음과 같은 상황에서 자주 볼 수 있습니다:

```
캡티브 포털 시나리오:

┌─────────────────────────────────────────────────────────────┐
│ 📶 사용자가 공용 WiFi에 연결                                  │
│                                                             │
│ 1. 사용자: 브라우저에서 www.example.com 요청                  │
│ 2. 캡티브 포털: 요청을 가로챔                                 │
│ 3. 캡티브 포털 → 사용자: 511 Network Authentication Required  │
│    - 로그인 페이지 링크 제공                                  │
│ 4. 사용자: 로그인 페이지에서 인증 완료                         │
│ 5. 캡티브 포털: 이후 요청을 정상적으로 전달                    │
└─────────────────────────────────────────────────────────────┘
```

#### 일반적인 캡티브 포털 환경

| 환경 | 예시 |
|------|------|
| 공항/호텔 WiFi | 인터넷 접속 전 약관 동의 또는 결제 필요 |
| 기업 네트워크 | 사내 네트워크 접속 시 인증 필요 |
| 공공 도서관 | 이용자 카드 인증 필요 |
| 카페/레스토랑 | 비밀번호 입력 또는 소셜 로그인 필요 |

#### 예시 응답

```http
HTTP/1.1 511 Network Authentication Required
Content-Type: text/html
Date: Fri, 13 Apr 2012 12:00:00 GMT

<!DOCTYPE html>
<html>
<head>
    <title>Network Authentication Required</title>
</head>
<body>
    <h1>Network Authentication Required</h1>
    <p>이 네트워크에 접근하려면 인증이 필요합니다.</p>
    <p><a href="http://network-login.example.com/">
       여기를 클릭하여 로그인하세요
    </a></p>
</body>
</html>
```

#### 응답 요구사항

511 응답은 **캐시되어서는 안 됩니다(MUST NOT be cached)**.

511 상태 코드는 **원본 서버로부터 응답이 온 것처럼 표시되어서는 안 됩니다(SHOULD NOT)**. 이는 클라이언트가 원본 서버와의 연결 상태를 잘못 인식하는 것을 방지하기 위함입니다.

#### 중요한 구현 세부사항

`non-authoritative` 응답임을 나타내기 위해 몇 가지 기술을 사용할 수 있습니다:

1. **비활성 URL 사용**: 응답의 콘텐츠 위치를 나타내는 URL이 원본 서버의 URL과 다르게 설정
2. **명확한 안내 문구**: 이 응답이 네트워크 장비에서 온 것임을 명시

---

## 보안 고려사항 (Security Considerations)

### 428 Precondition Required

428 상태 코드는 **선택적(optional)** 입니다. 클라이언트는 모든 서버가 이 상태 코드를 반환할 것이라고 기대할 수 없습니다. 따라서 **클라이언트는 서버가 항상 충돌을 방지해줄 것이라고 의존해서는 안 됩니다**.

애플리케이션 설계 시 클라이언트 측에서도 동시성 제어 메커니즘을 구현하는 것이 권장됩니다.

### 429 Too Many Requests

429 상태 코드를 전송하면 **공격 상황에서 서버 리소스를 소모** 할 수 있습니다.

- 대규모 DDoS 공격 시 모든 요청에 429를 반환하면 상당한 리소스가 소모됨
- 운영 환경에 따라 연결을 조용히 끊는 것이 더 적절할 수 있음

```
공격 상황에서의 고려사항:

┌─────────────────────────────────────────────────────────────┐
│ 접근 방식          │ 장점           │ 단점                   │
├─────────────────────────────────────────────────────────────┤
│ 429 응답 반환      │ 정상 클라이언트 │ 공격 시 리소스 소모    │
│                   │ 에게 유용한     │                       │
│                   │ 피드백 제공     │                       │
├─────────────────────────────────────────────────────────────┤
│ 연결 무시/차단     │ 리소스 절약    │ 정상 클라이언트도       │
│                   │               │ 피드백 받지 못함        │
├─────────────────────────────────────────────────────────────┤
│ 하이브리드 접근    │ 균형 잡힌     │ 구현 복잡도 증가        │
│ (임계값 기반)      │ 대응 가능     │                        │
└─────────────────────────────────────────────────────────────┘
```

### 431 Request Header Fields Too Large

과도하게 큰 헤더를 사용하는 공격의 경우, 431 상태 코드를 반환하는 것이 **적절하지 않을 수 있습니다**.

- 공격자에게 서버 상태 정보를 노출할 수 있음
- 응답 생성 자체가 리소스를 소모함
- 방화벽 수준에서 차단하는 것이 더 효과적일 수 있음

### 511 Network Authentication Required

511 상태 코드는 특별히 주의해야 할 보안 고려사항이 있습니다:

#### 1. 비신뢰 출처

511 응답은 **원본 서버에서 오지 않습니다**. 이 응답은 클라이언트에 대한 신뢰를 설정하지 않은 네트워크 인프라에서 옵니다.

#### 2. 쿠키 주입 위험

```
쿠키 주입 공격 시나리오:

┌─────────────────────────────────────────────────────────────┐
│ 1. 악성 네트워크가 요청을 가로챔                              │
│ 2. 악성 511 응답에 Set-Cookie 헤더 포함                      │
│    Set-Cookie: tracking=malicious_value; Domain=.example.com │
│ 3. 클라이언트가 이 쿠키를 저장                                │
│ 4. 실제 example.com에 접속 시 악성 쿠키가 전송됨              │
│ 5. 세션 고정 공격 등에 악용 가능                              │
└─────────────────────────────────────────────────────────────┘
```

#### 3. 자격 증명 가로채기

```
자격 증명 도용 위험:

┌─────────────────────────────────────────────────────────────┐
│ 위험: 악성 캡티브 포털이 로그인 양식을 제공하여               │
│       사용자의 자격 증명을 수집할 수 있음                     │
│                                                             │
│ 완화 방법:                                                   │
│ - HTTPS 사이트에서만 자격 증명 입력                          │
│ - 로그인 페이지 URL 확인                                     │
│ - 신뢰할 수 없는 네트워크에서 민감한 정보 입력 자제           │
└─────────────────────────────────────────────────────────────┘
```

#### 4. 클라이언트 구현 권장사항

클라이언트는 511 응답을 처리할 때 다음 사항을 고려해야 합니다:

- 511 응답에 포함된 쿠키를 신뢰하지 않아야 함
- 사용자에게 응답이 캡티브 포털에서 왔음을 명확히 표시해야 함
- 민감한 정보를 입력하기 전에 사용자에게 경고해야 함

---

## IANA 고려사항 (IANA Considerations)

이 문서는 다음 HTTP 상태 코드들을 "HTTP Status Codes" 레지스트리에 등록합니다:

| 코드 | 설명 | 참조 |
|------|------|------|
| 428 | Precondition Required | RFC 6585, Section 3 |
| 429 | Too Many Requests | RFC 6585, Section 4 |
| 431 | Request Header Fields Too Large | RFC 6585, Section 5 |
| 511 | Network Authentication Required | RFC 6585, Section 6 |

---

## 상태 코드 요약

### 비교표

| 코드 | 이름 | 분류 | 용도 | 캐시 가능 |
|------|------|------|------|----------|
| 428 | Precondition Required | 4xx | 조건부 요청 강제 | 아니오 |
| 429 | Too Many Requests | 4xx | 속도 제한 | 아니오 |
| 431 | Request Header Fields Too Large | 4xx | 헤더 크기 제한 | 아니오 |
| 511 | Network Authentication Required | 5xx | 네트워크 인증 | 아니오 |

### 사용 시나리오

```
┌───────────────────────────────────────────────────────────────────┐
│ 상태 코드 선택 가이드                                              │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│ "리소스 수정 시 동시성 충돌 방지가 필요한가?"                       │
│     └─ 예 → 428 Precondition Required                            │
│                                                                   │
│ "클라이언트가 너무 자주 요청하고 있는가?"                           │
│     └─ 예 → 429 Too Many Requests                                │
│                                                                   │
│ "요청 헤더가 서버 제한을 초과했는가?"                               │
│     └─ 예 → 431 Request Header Fields Too Large                  │
│                                                                   │
│ "네트워크 접근에 인증이 필요한가? (캡티브 포털)"                     │
│     └─ 예 → 511 Network Authentication Required                  │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## 인식되지 않는 상태 코드 처리

RFC 2616에 따르면, 클라이언트가 인식하지 못하는 상태 코드는 해당 클래스의 일반 코드(x00)로 처리됩니다:

| 인식되지 않는 코드 | 처리 방식 |
|-------------------|----------|
| 428, 429, 431 | 400 Bad Request로 처리 |
| 511 | 500 Internal Server Error로 처리 |

이러한 처리 방식 덕분에 새로운 상태 코드를 기존 인프라에 안전하게 배포할 수 있습니다. 오래된 클라이언트도 적어도 오류가 발생했음을 인식할 수 있습니다.

---

## 참조 문서 (References)

### 규범적 참조 (Normative References)

| RFC | 제목 | 설명 |
|-----|------|------|
| RFC 2119 | Key words for use in RFCs | 요구사항 수준을 나타내는 키워드 정의 |
| RFC 2616 | Hypertext Transfer Protocol -- HTTP/1.1 | HTTP/1.1 명세 (이 문서에 의해 업데이트됨) |

### 참고 참조 (Informative References)

| RFC | 제목 | 설명 |
|-----|------|------|
| RFC 4918 | HTTP Extensions for WebDAV | WebDAV 확장 (412 상태 코드 사용 예시) |

---

## 관련 RFC 문서

- **RFC 7231**: HTTP/1.1 Semantics and Content (HTTP 상태 코드 정의)
- **RFC 7232**: HTTP/1.1 Conditional Requests (조건부 요청)
- **RFC 7233**: HTTP/1.1 Range Requests (범위 요청)
- **RFC 7234**: HTTP/1.1 Caching (캐싱)
- **RFC 7235**: HTTP/1.1 Authentication (인증)

---

## 실제 활용 예시

### 429 Too Many Requests - API 속도 제한

```python
# Python Flask 예시
from flask import Flask, jsonify
from flask_limiter import Limiter

app = Flask(__name__)
limiter = Limiter(app)

@app.route("/api/data")
@limiter.limit("100/hour")  # 시간당 100회 제한
def get_data():
    return jsonify({"data": "value"})

# 제한 초과 시 자동으로 429 응답 반환
```

### 428 Precondition Required - ETag 기반 동시성 제어

```javascript
// JavaScript 클라이언트 예시
async function updateResource(id, data, etag) {
    const response = await fetch(`/api/resource/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'If-Match': etag  // 조건부 요청
        },
        body: JSON.stringify(data)
    });

    if (response.status === 428) {
        console.log('서버가 조건부 요청을 요구합니다. ETag를 포함해주세요.');
    } else if (response.status === 412) {
        console.log('리소스가 수정되었습니다. 최신 버전을 가져오세요.');
    }
}
```

### 431 Request Header Fields Too Large - Nginx 설정

```nginx
# Nginx 설정 예시
http {
    # 클라이언트 헤더 버퍼 크기 설정
    large_client_header_buffers 4 16k;

    # 헤더 크기 초과 시 431 반환
    # (기본적으로 Nginx는 400을 반환하지만 커스텀 설정 가능)
}
```

---

## 참고 자료

- [RFC 6585 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc6585)
- [RFC 6585 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc6585)
- [HTTP 상태 코드 - MDN Web Docs](https://developer.mozilla.org/ko/docs/Web/HTTP/Status)
- [IANA HTTP Status Code Registry](https://www.iana.org/assignments/http-status-codes)

---

*이 문서는 RFC 6585의 한국어 번역 및 해설입니다.*
