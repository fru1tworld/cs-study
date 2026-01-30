# RFC 6750: The OAuth 2.0 Authorization Framework: Bearer Token Usage

> OAuth 2.0 보호 리소스에 접근하기 위한 Bearer 토큰 사용법

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 6750 |
| **제목** | The OAuth 2.0 Authorization Framework: Bearer Token Usage |
| **분류** | Standards Track (Proposed Standard) |
| **작성자** | Michael B. Jones (Microsoft), Dick Hardt (Independent) |
| **발행일** | 2012년 10월 |
| **업데이트** | RFC 8996, RFC 9700에 의해 업데이트됨 |
| **관련 RFC** | RFC 6749 (OAuth 2.0 Authorization Framework) |

---

## 1. 소개 (Introduction)

### 1.1 개요

OAuth 2.0은 리소스 소유자를 대신하여 보호된 리소스에 접근하기 위한 인가(Authorization) 프레임워크입니다. 클라이언트는 리소스 소유자의 인가를 받아 액세스 토큰을 획득하고, 이 토큰을 사용하여 리소스 서버의 보호된 리소스에 접근합니다.

이 명세서는 **Bearer 토큰**을 HTTP 요청에서 사용하여 OAuth 2.0 보호 리소스에 접근하는 방법을 설명합니다.

### 1.2 Bearer 토큰이란?

**Bearer 토큰(무기명 토큰)**은 다음과 같은 특성을 가진 보안 토큰입니다:

```
Bearer 토큰의 핵심 특성:
┌─────────────────────────────────────────────────────────────┐
│  Bearer 토큰을 소유한 모든 당사자("bearer")는              │
│  암호화 키의 소유를 증명하지 않고도                        │
│  연관된 리소스에 접근할 수 있습니다.                       │
└─────────────────────────────────────────────────────────────┘
```

**중요**: Bearer 토큰은 소유만으로 리소스 접근 권한을 부여하므로, 저장 및 전송 시 반드시 보호되어야 합니다.

### 1.3 표기 규칙

이 문서에서 사용되는 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"은 RFC 2119에 정의된 대로 해석됩니다.

### 1.4 용어

| 용어 | 정의 |
|------|------|
| **Bearer Token** | 토큰을 소유한 모든 당사자가 암호화 키 소유 증명 없이 토큰을 사용할 수 있는 보안 토큰 |
| **Resource Server** | 보호된 리소스를 호스팅하는 서버로, 액세스 토큰을 사용한 보호 리소스 요청을 수락하고 응답 |
| **Access Token** | 리소스 소유자가 부여하고 리소스 서버와 인가 서버가 적용하는 접근 제한을 나타내는 문자열 |

---

## 2. 인증된 요청 (Authenticated Requests)

이 섹션에서는 Bearer 액세스 토큰을 리소스 요청 시 리소스 서버로 전송하는 **세 가지 방법**을 정의합니다.

**중요 규칙**:
- 클라이언트는 단일 요청에서 **하나의 방법만** 사용하여 토큰을 전송해야 합니다(MUST NOT use more than one method)
- 여러 방법을 동시에 사용하면 요청이 거부됩니다

### 2.1 Authorization Request Header Field (권장)

**Authorization HTTP 요청 헤더**를 사용하여 Bearer 토큰을 전송하는 방법입니다.

#### 문법

```
Authorization: Bearer <b64token>
```

- `Bearer`: 인증 스킴 이름 (대소문자 구분 없음)
- `b64token`: Base64로 인코딩된 토큰 값

#### ABNF 문법

```abnf
credentials = "Bearer" 1*SP b64token
b64token    = 1*( ALPHA / DIGIT /
                  "-" / "." / "_" / "~" / "+" / "/" ) *"="
```

#### 예제

```http
GET /resource/1 HTTP/1.1
Host: example.com
Authorization: Bearer mF_9.B5f-4.1JqM
```

#### 요구사항

| 대상 | 요구사항 |
|------|----------|
| **리소스 서버** | 이 방법을 반드시 지원해야 합니다(MUST) |
| **클라이언트** | 이 방법을 사용하여 인증된 요청을 보내는 것이 좋습니다(SHOULD) |

#### 장점

```
✅ HTTP 헤더는 일반적으로 로그에 기록되지 않음
✅ 브라우저 기록에 남지 않음
✅ 표준 HTTP 인증 프레임워크와 호환
✅ 가장 안전한 전송 방법
```

---

### 2.2 Form-Encoded Body Parameter

HTTP 요청 본문(body)에 액세스 토큰을 포함하여 전송하는 방법입니다.

#### 예제

```http
POST /resource HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded

access_token=mF_9.B5f-4.1JqM
```

#### 사용 조건 (모두 충족해야 함)

이 방법은 다음 조건이 **모두** 충족될 때만 사용해야 합니다(MUST NOT be used unless):

| 조건 | 설명 |
|------|------|
| **Content-Type** | `application/x-www-form-urlencoded`이어야 함 |
| **HTTP 메서드** | 요청 본문을 가질 수 있는 메서드만 사용 (GET 불가) |
| **본문 규격** | HTTP 요청 엔티티 본문이 단일 파트여야 함 |
| **인코딩** | 본문의 콘텐츠가 ASCII 문자로만 구성되어야 함 |

#### 요구사항

| 대상 | 요구사항 |
|------|----------|
| **리소스 서버** | 이 방법을 지원할 수 있습니다(MAY) |
| **클라이언트** | Authorization 헤더를 사용할 수 없는 경우에만 이 방법 사용 |

#### 제한사항

```
⚠️ Authorization 헤더 필드를 사용할 수 없는 경우를 제외하고는
   사용하지 않는 것이 좋습니다(SHOULD NOT)

⚠️ GET 요청에서는 사용할 수 없습니다
```

---

### 2.3 URI Query Parameter (비권장)

URI 쿼리 파라미터로 액세스 토큰을 전송하는 방법입니다.

#### 예제

```http
GET /resource?access_token=mF_9.B5f-4.1JqM HTTP/1.1
Host: server.example.com
```

#### 응답 예제

이 방법 사용 시 클라이언트는 `Cache-Control` 헤더를 포함하여 응답해야 합니다:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: private

{
  "id": "12345",
  "name": "Example Resource"
}
```

#### 보안 경고

```
⛔ 이 방법은 보안 취약점으로 인해 사용을 권장하지 않습니다(SHOULD NOT be used)
```

| 보안 문제 | 설명 |
|----------|------|
| **로깅** | URI는 웹 서버 로그에 기록될 가능성이 높음 |
| **브라우저 기록** | 브라우저 히스토리에 토큰이 저장됨 |
| **Referer 헤더** | 다른 사이트로 이동 시 Referer 헤더에 토큰 노출 가능 |
| **캐싱** | 프록시나 중간 캐시에 토큰이 저장될 수 있음 |

#### 사용이 불가피한 경우

다른 방법으로 토큰을 전송할 수 없는 경우에만 사용합니다:

```
예: Authorization 헤더 필드에 접근할 수 없는 애플리케이션 컨텍스트
```

#### 추가 보안 조치

이 방법 사용 시 다음 보안 조치를 적용해야 합니다:

1. 클라이언트는 `Cache-Control: no-store` 옵션으로 요청을 보내야 합니다(SHOULD)
2. 서버는 `Cache-Control: private` 응답 헤더를 포함해야 합니다(MUST)

---

### 2.4 세 가지 방법 비교

| 방법 | 권장 여부 | 리소스 서버 지원 | 보안 수준 |
|------|----------|-----------------|----------|
| **Authorization Header** | 권장(RECOMMENDED) | 필수(MUST) | 높음 |
| **Form-Encoded Body** | 조건부 허용 | 선택(MAY) | 중간 |
| **URI Query Parameter** | 비권장(NOT RECOMMENDED) | 선택(MAY) | 낮음 |

```
우선순위:
┌───────────────────────────────────────────────────────────┐
│  1순위: Authorization Header (가장 안전)                  │
│  2순위: Form-Encoded Body (헤더 사용 불가 시)            │
│  3순위: URI Query Parameter (다른 방법 불가능 시만)      │
└───────────────────────────────────────────────────────────┘
```

---

## 3. WWW-Authenticate 응답 헤더 필드

보호된 리소스 요청이 **인증 자격 증명을 포함하지 않거나** **유효하지 않은 액세스 토큰을 포함**한 경우, 리소스 서버는 `WWW-Authenticate` HTTP 응답 헤더 필드를 포함해야 합니다(MUST).

### 3.1 기본 구문

```http
WWW-Authenticate: Bearer realm="example"
```

### 3.2 속성 (Attributes)

| 속성 | 설명 | 필수 여부 |
|------|------|----------|
| `realm` | 보호 영역을 나타내는 문자열 | 선택 |
| `scope` | 리소스 접근에 필요한 범위 | 선택 |
| `error` | 에러 코드 | 조건부 |
| `error_description` | 사람이 읽을 수 있는 에러 설명 | 선택 |
| `error_uri` | 에러 정보 페이지 URI | 선택 |

### 3.3 에러 응답 예제

#### 인증 정보 없는 요청

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="example"
```

#### 유효하지 않은 토큰

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="example",
                  error="invalid_token",
                  error_description="The access token expired"
```

#### 권한 부족

```http
HTTP/1.1 403 Forbidden
WWW-Authenticate: Bearer realm="example",
                  error="insufficient_scope",
                  scope="read write"
```

### 3.4 에러 코드 (Error Codes)

#### invalid_request

| 항목 | 내용 |
|------|------|
| **HTTP 상태 코드** | 400 (Bad Request) |
| **의미** | 요청에 필수 파라미터가 누락되었거나, 지원되지 않는 파라미터나 값이 포함되었거나, 동일한 파라미터가 반복되었거나, 액세스 토큰 전송에 여러 방법이 사용되었거나, 요청 형식이 잘못됨 |

```http
HTTP/1.1 400 Bad Request
WWW-Authenticate: Bearer error="invalid_request",
                  error_description="Multiple access tokens were used"
```

#### invalid_token

| 항목 | 내용 |
|------|------|
| **HTTP 상태 코드** | 401 (Unauthorized) |
| **의미** | 제공된 액세스 토큰이 만료되었거나, 취소되었거나, 형식이 잘못되었거나, 다른 이유로 유효하지 않음 |
| **클라이언트 조치** | 새로운 액세스 토큰을 요청하고 보호된 리소스 요청을 재시도할 수 있음 |

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="example",
                  error="invalid_token",
                  error_description="The access token has been revoked"
```

#### insufficient_scope

| 항목 | 내용 |
|------|------|
| **HTTP 상태 코드** | 403 (Forbidden) |
| **의미** | 요청에 유효한 액세스 토큰이 제공되었으나, 리소스 서버가 요구하는 범위(scope)보다 낮은 권한을 가짐 |
| **추가 정보** | 필요한 scope 값을 `scope` 속성에 포함하여 응답할 수 있음 |

```http
HTTP/1.1 403 Forbidden
WWW-Authenticate: Bearer realm="example",
                  error="insufficient_scope",
                  scope="admin"
```

### 3.5 에러 코드 요약

```
에러 코드와 HTTP 상태 코드 매핑:
┌─────────────────────┬─────────────────┬────────────────────────────┐
│ 에러 코드           │ HTTP 상태       │ 설명                       │
├─────────────────────┼─────────────────┼────────────────────────────┤
│ invalid_request     │ 400 Bad Request │ 잘못된 요청 형식           │
│ invalid_token       │ 401 Unauthorized│ 유효하지 않은/만료된 토큰  │
│ insufficient_scope  │ 403 Forbidden   │ 권한 부족                  │
└─────────────────────┴─────────────────┴────────────────────────────┘
```

---

## 4. 액세스 토큰 응답 예제 (Example Access Token Response)

성공적인 토큰 응답의 예제입니다. 이 응답은 OAuth 2.0 인가 서버(Authorization Server)가 액세스 토큰을 발급할 때 반환합니다.

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Cache-Control: no-store
Pragma: no-cache

{
  "access_token": "mF_9.B5f-4.1JqM",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "tGzv3JOkF0XG5Qx2TlKWIA"
}
```

### 응답 필드 설명

| 필드 | 설명 |
|------|------|
| `access_token` | 발급된 액세스 토큰 값 |
| `token_type` | 토큰 유형, 이 명세서에서는 "Bearer" |
| `expires_in` | 토큰 만료까지 남은 시간(초) |
| `refresh_token` | 액세스 토큰 갱신에 사용되는 리프레시 토큰 (선택) |

### 응답 헤더 요구사항

| 헤더 | 값 | 이유 |
|------|-----|------|
| `Cache-Control` | `no-store` | 토큰이 캐시되지 않도록 방지 |
| `Pragma` | `no-cache` | HTTP/1.0 호환성을 위한 캐시 방지 |

---

## 5. 보안 고려사항 (Security Considerations)

이 섹션에서는 Bearer 토큰 사용 시 관련된 보안 위협과 이를 완화하기 위한 방법을 설명합니다.

### 5.1 보안 위협 (Security Threats)

#### Token Manufacture/Modification (토큰 위조/변조)

공격자가 자체적으로 토큰을 생성하거나 기존 토큰의 내용을 수정하여 리소스 서버가 클라이언트에게 부적절한 접근 권한을 부여하도록 하는 위협입니다.

```
위협 시나리오:
┌─────────────────────────────────────────────────────────────┐
│  1. 공격자가 유효한 토큰 형식을 분석                       │
│  2. 자체적으로 토큰 생성 또는 기존 토큰의 권한 수정        │
│  3. 위조된 토큰으로 보호된 리소스에 접근 시도              │
└─────────────────────────────────────────────────────────────┘
```

#### Token Disclosure (토큰 노출)

토큰이 인가되지 않은 당사자에게 노출되는 위협입니다.

```
토큰 노출 경로:
├── 안전하지 않은 통신 채널 (HTTP 사용)
├── 서버 로그 파일
├── 브라우저 히스토리
├── 공유 시스템의 저장소
└── Referer 헤더를 통한 누출
```

#### Token Redirect (토큰 리다이렉트)

한 리소스 서버에서 발급받은 토큰을 공격자가 다른 리소스 서버에서 사용하는 위협입니다.

```
공격 시나리오:
┌─────────────────────────────────────────────────────────────┐
│  클라이언트 → 악의적 서버 A (토큰 수집)                    │
│      │                                                      │
│      └──→ 정상 서버 B (수집된 토큰으로 불법 접근)          │
└─────────────────────────────────────────────────────────────┘
```

#### Token Replay (토큰 재사용)

공격자가 이미 사용된 액세스 토큰을 가로채어 보호된 리소스에 접근하기 위해 재사용하는 위협입니다.

```
재사용 공격 흐름:
┌─────────────────────────────────────────────────────────────┐
│  정상 사용자 ──토큰──→ 리소스 서버                         │
│       │                                                     │
│       └──(토큰 가로채기)──→ 공격자 ──토큰──→ 리소스 서버   │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 위협 완화 (Threat Mitigation)

#### TLS 사용 (필수)

Bearer 토큰을 전송할 때 **TLS(Transport Layer Security)**를 사용하는 것은 **필수**입니다.

```
요구사항:
┌─────────────────────────────────────────────────────────────┐
│  ✓ 항상 TLS(HTTPS)를 사용해야 합니다(MUST)                │
│  ✓ TLS 버전 1.2 이상을 권장합니다(RECOMMENDED)            │
│  ✓ TLS 인증서 체인을 검증해야 합니다(MUST)                │
└─────────────────────────────────────────────────────────────┘
```

#### 토큰 무결성 보호

토큰이 수정되지 않았음을 보장하기 위해 다음 방법을 사용할 수 있습니다:

| 방법 | 설명 |
|------|------|
| **디지털 서명** | 인가 서버가 토큰에 서명하고 리소스 서버가 검증 |
| **MAC (Message Authentication Code)** | 공유 비밀키를 사용한 토큰 인증 |
| **암호화** | 토큰 내용을 암호화하여 위조 방지 |

#### 짧은 토큰 수명

토큰의 유효 기간을 **1시간 이하**로 설정하는 것이 권장됩니다:

```
권장 토큰 수명:
┌─────────────────────────────────────────────────────────────┐
│  액세스 토큰: 1시간 이하 (≤ 3600초)                        │
│  리프레시 토큰: 더 긴 수명 허용, 별도 보안 조치 필요       │
└─────────────────────────────────────────────────────────────┘
```

#### 토큰 범위 제한 (Audience Restriction)

토큰이 특정 리소스 서버에서만 사용될 수 있도록 대상(audience)을 제한합니다:

```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "aud": ["https://api.example.com"]
}
```

### 5.3 권장사항 요약 (Summary of Recommendations)

#### 필수 사항 (MUST)

| 번호 | 권장사항 |
|------|----------|
| 1 | Bearer 토큰은 TLS를 사용하여 전송해야 합니다 |
| 2 | TLS 인증서 체인을 검증해야 합니다 |
| 3 | Bearer 토큰을 평문으로 전송 가능한 쿠키에 저장해서는 안 됩니다 |
| 4 | 클라이언트는 Bearer 토큰이 의도하지 않은 당사자에게 누출되지 않도록 해야 합니다 |

#### 권장 사항 (SHOULD/RECOMMENDED)

| 번호 | 권장사항 |
|------|----------|
| 1 | 짧은 수명(1시간 이하)의 Bearer 토큰을 발급해야 합니다 |
| 2 | 범위가 제한된(scoped) Bearer 토큰을 발급해야 합니다 |
| 3 | Bearer 토큰을 페이지 URL에 전달하지 않아야 합니다 |
| 4 | Authorization 헤더 필드를 사용하여 토큰을 전송해야 합니다 |
| 5 | URI 쿼리 파라미터 방법은 사용하지 않아야 합니다 |

#### 금지 사항 (MUST NOT)

| 번호 | 금지사항 |
|------|----------|
| 1 | 평문으로 전송될 수 있는 쿠키에 Bearer 토큰을 저장해서는 안 됩니다 |
| 2 | 단일 요청에서 여러 방법으로 토큰을 전송해서는 안 됩니다 |

---

## 6. 구현 가이드

### 6.1 클라이언트 구현

#### Authorization Header 사용 (권장)

```javascript
// JavaScript/Node.js 예제
const response = await fetch('https://api.example.com/resource', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer mF_9.B5f-4.1JqM',
    'Content-Type': 'application/json'
  }
});
```

```python
# Python 예제
import requests

headers = {
    'Authorization': 'Bearer mF_9.B5f-4.1JqM',
    'Content-Type': 'application/json'
}

response = requests.get('https://api.example.com/resource', headers=headers)
```

```java
// Java 예제
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/resource"))
    .header("Authorization", "Bearer mF_9.B5f-4.1JqM")
    .header("Content-Type", "application/json")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
```

#### 에러 처리

```javascript
// 에러 응답 처리 예제
async function fetchProtectedResource(accessToken) {
  const response = await fetch('https://api.example.com/resource', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  if (response.status === 401) {
    const wwwAuthenticate = response.headers.get('WWW-Authenticate');
    if (wwwAuthenticate?.includes('invalid_token')) {
      // 토큰이 만료됨 - 리프레시 토큰으로 새 토큰 요청
      return await refreshAndRetry();
    }
  }

  if (response.status === 403) {
    // 권한 부족 - 추가 권한 요청 필요
    throw new Error('Insufficient scope');
  }

  return response.json();
}
```

### 6.2 리소스 서버 구현

#### 토큰 검증

```python
# Python/Flask 예제
from flask import Flask, request, jsonify
from functools import wraps

app = Flask(__name__)

def require_bearer_token(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')

        if not auth_header:
            return jsonify({
                'error': 'invalid_request',
                'error_description': 'Missing Authorization header'
            }), 401, {
                'WWW-Authenticate': 'Bearer realm="example"'
            }

        try:
            scheme, token = auth_header.split(' ', 1)
            if scheme.lower() != 'bearer':
                raise ValueError('Invalid scheme')
        except ValueError:
            return jsonify({
                'error': 'invalid_request',
                'error_description': 'Invalid Authorization header format'
            }), 400, {
                'WWW-Authenticate': 'Bearer error="invalid_request"'
            }

        # 토큰 검증 로직
        if not validate_token(token):
            return jsonify({
                'error': 'invalid_token',
                'error_description': 'The access token is invalid'
            }), 401, {
                'WWW-Authenticate': 'Bearer error="invalid_token"'
            }

        return f(*args, **kwargs)
    return decorated_function

@app.route('/resource')
@require_bearer_token
def protected_resource():
    return jsonify({'data': 'Protected resource data'})
```

---

## 7. IANA 고려사항

### 7.1 OAuth Access Token Types Registry

이 명세서는 "Bearer" 토큰 타입을 OAuth Access Token Types 레지스트리에 등록합니다.

| 항목 | 값 |
|------|-----|
| **Type name** | Bearer |
| **Additional Token Endpoint Response Parameters** | (없음) |
| **HTTP Authentication Scheme(s)** | Bearer |
| **Change controller** | IETF |
| **Specification document(s)** | RFC 6750 |

### 7.2 OAuth Extensions Error Registry

다음 에러 코드가 OAuth Extensions Error Registry에 등록됩니다:

| 에러 코드 | 사용 위치 | 명세 문서 |
|----------|----------|----------|
| invalid_request | Resource access error response | RFC 6750 |
| invalid_token | Resource access error response | RFC 6750 |
| insufficient_scope | Resource access error response | RFC 6750 |

---

## 8. 관련 RFC 문서

| RFC | 제목 | 관계 |
|-----|------|------|
| **RFC 6749** | The OAuth 2.0 Authorization Framework | 기반 프레임워크 |
| **RFC 2119** | Key words for use in RFCs | 요구사항 키워드 정의 |
| **RFC 2617** | HTTP Authentication | HTTP 인증 스킴 |
| **RFC 5234** | ABNF (Augmented BNF) | 문법 표기법 |
| **RFC 5246** | TLS Protocol Version 1.2 | 전송 보안 |
| **RFC 8996** | Deprecating TLS 1.0 and TLS 1.1 | RFC 6750 업데이트 |
| **RFC 9700** | OAuth 2.0 Security Best Current Practice | RFC 6750 업데이트 |

---

## 9. 자주 묻는 질문 (FAQ)

### Q1: Bearer 토큰은 왜 "Bearer"라고 부르나요?

**A**: "Bearer"는 "소유자"를 의미합니다. 이 토큰은 소유하고 있는 것만으로 리소스에 접근할 수 있으며, 추가적인 신원 증명(예: 암호화 키 소유 증명)이 필요하지 않기 때문입니다.

### Q2: Bearer 토큰과 MAC 토큰의 차이점은?

| 특성 | Bearer Token | MAC Token |
|------|-------------|-----------|
| 소유 증명 | 불필요 | 필요 |
| 구현 복잡도 | 낮음 | 높음 |
| TLS 필요 | 필수 | 권장 |
| 토큰 재사용 방지 | TLS 의존 | 암호화 서명 |

### Q3: 왜 Authorization 헤더 방법이 권장되나요?

**A**:
1. HTTP 헤더는 일반적으로 서버 로그에 기록되지 않음
2. 브라우저 히스토리에 저장되지 않음
3. Referer 헤더를 통해 누출되지 않음
4. 표준 HTTP 인증 프레임워크와 호환됨

### Q4: 토큰이 만료되면 어떻게 해야 하나요?

**A**: 리프레시 토큰을 사용하여 새로운 액세스 토큰을 요청합니다:

```http
POST /token HTTP/1.1
Host: authorization-server.example.com
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=tGzv3JOkF0XG5Qx2TlKWIA
```

---

## 10. 참고 자료

- [RFC 6750 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc6750)
- [RFC 6750 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc6750.html)
- [OAuth 2.0 Bearer Tokens (oauth.net)](https://oauth.net/2/bearer-tokens/)
- [RFC 6749 - OAuth 2.0 Framework](https://datatracker.ietf.org/doc/html/rfc6749)

---

*이 문서는 RFC 6750의 한국어 번역 및 정리본입니다.*
