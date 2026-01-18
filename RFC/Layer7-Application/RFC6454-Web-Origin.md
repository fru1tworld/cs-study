# RFC 6454 - The Web Origin Concept

> **발행일**: 2011년 12월
> **상태**: Standards Track

## 1. 개요

RFC 6454는 웹 보안의 핵심 개념인 **Origin(출처)** 을 정의합니다. Origin은 브라우저의 **Same-Origin Policy(동일 출처 정책)** 와 **CORS(Cross-Origin Resource Sharing)** 의 기반이 되는 개념입니다.

### 핵심 개념

> "Origin은 웹 보안 모델의 근본적인 개념으로, 서로 다른 웹 애플리케이션 간의 격리를 정의합니다."

| 특징 | 설명 |
|------|------|
| **격리 경계** | 서로 다른 Origin 간의 리소스 접근 제한 |
| **보안 컨텍스트** | 동일 Origin은 같은 보안 컨텍스트 공유 |
| **신뢰 경계** | Origin 단위로 권한 부여/제한 |

---

## 2. Origin 정의

### 2.1 Origin의 구성 요소

Origin은 **세 가지 요소의 조합** 입니다:

```
Origin = (scheme, host, port)
```

| 구성 요소 | 설명 | 예시 |
|-----------|------|------|
| **Scheme** | 프로토콜 | `http`, `https` |
| **Host** | 도메인 또는 IP | `example.com`, `192.168.1.1` |
| **Port** | 포트 번호 | `80`, `443`, `8080` |

### 2.2 Origin 예시

```
https://example.com:443
└─┬──┘ └────┬─────┘ └┬─┘
scheme    host     port
```

**동일 Origin 판단**:

| URL A | URL B | 동일 Origin? |
|-------|-------|:------------:|
| `http://example.com/a` | `http://example.com/b` | O |
| `http://example.com` | `http://example.com:80` | O |
| `https://example.com` | `https://example.com:443` | O |
| `http://example.com` | `https://example.com` | X (scheme 다름) |
| `http://example.com` | `http://www.example.com` | X (host 다름) |
| `http://example.com` | `http://example.com:8080` | X (port 다름) |

### 2.3 기본 포트

스킴별 기본 포트:

| Scheme | 기본 Port |
|--------|-----------|
| http | 80 |
| https | 443 |
| ftp | 21 |
| ws | 80 |
| wss | 443 |

---

## 3. Origin 직렬화

### 3.1 ASCII 직렬화

```
origin = scheme "://" host [ ":" port ]
```

**예시**:
```
https://example.com
http://example.com:8080
https://192.168.1.1:443
```

### 3.2 Unicode 직렬화

국제화된 도메인 이름(IDN)의 경우:

```
한글.example.com → xn--bj0bj06e.example.com (Punycode)
```

### 3.3 Opaque Origin vs Tuple Origin

Origin은 두 가지 타입으로 구분됩니다:

#### Tuple Origin (투플 오리진)

일반적인 웹 페이지의 Origin으로, **scheme, host, port** 로 구성된 투플입니다:

```
Tuple Origin = (scheme, host, port)
예: ("https", "example.com", 443)
```

| 특징 | 설명 |
|------|------|
| **직렬화 가능** | `https://example.com:443` 형태로 문자열 표현 |
| **비교 가능** | 두 Origin 간 동일성 비교 가능 |
| **전역 고유** | scheme, host, port 조합으로 고유 식별 |

#### Opaque Origin (불투명 오리진)

**직렬화할 수 없고 전역적으로 고유한** Origin입니다:

| 생성 조건 | 설명 |
|-----------|------|
| `data:` URL | `data:text/html,<h1>Hello</h1>` |
| `file:` URL | `file:///path/to/file.html` |
| `javascript:` URL | `javascript:alert(1)` |
| 샌드박스된 iframe | `<iframe sandbox src="...">` |
| `about:blank` | 일부 조건에서 |

```javascript
// Opaque Origin 예시
const dataUrl = new URL('data:text/html,<h1>Hello</h1>');
console.log(dataUrl.origin); // "null"

// 샌드박스 iframe의 origin
// <iframe sandbox src="https://example.com"></iframe>
// iframe 내부의 origin은 opaque ("null")
```

#### Opaque Origin의 특성

```
┌─────────────────────────────────────────────────────────────┐
│                    Opaque Origin 특성                        │
├─────────────────────────────────────────────────────────────┤
│ 1. 직렬화 시 "null" 문자열로 표현                            │
│ 2. 자기 자신과만 동일 (다른 "null" origin과도 다름)          │
│ 3. 매번 새로운 고유 식별자 생성                              │
│ 4. 다른 어떤 origin과도 same-origin이 아님                   │
└─────────────────────────────────────────────────────────────┘
```

**중요**: 두 개의 Opaque Origin은 둘 다 `null`로 직렬화되더라도 **서로 다른 Origin** 입니다:

```javascript
// 두 개의 data: URL
const iframe1 = 'data:text/html,<h1>A</h1>'; // origin: opaque1
const iframe2 = 'data:text/html,<h1>B</h1>'; // origin: opaque2

// opaque1 !== opaque2 (둘 다 "null"로 직렬화되지만 다른 origin)
```

---

## 4. Origin 비교 알고리즘

### 4.1 Same-Origin 비교

두 Origin이 동일한지 판단하는 알고리즘:

```
Algorithm: Same Origin Check
Input: Origin A, Origin B
Output: Boolean (same-origin 여부)

1. If A is opaque origin:
   a. If A === B (동일한 opaque origin 인스턴스)
      Return true
   b. Else
      Return false

2. If B is opaque origin:
   Return false

3. If A.scheme ≠ B.scheme (대소문자 무시):
   Return false

4. If A.host ≠ B.host (대소문자 무시):
   Return false

5. If A.port ≠ B.port:
   Return false

6. Return true
```

### 4.2 Same-Origin-Domain 비교

`document.domain`을 고려한 확장된 비교:

```
Algorithm: Same Origin-Domain Check
Input: Origin A, Origin B
Output: Boolean

1. If A and B are same origin:
   Return true

2. If A.domain is set AND B.domain is set:
   a. If A.domain === B.domain
   b. AND A.scheme === B.scheme
      Return true

3. Return false
```

### 4.3 비교 예시

```javascript
// Same-Origin 비교 구현 예시
function isSameOrigin(urlA, urlB) {
  const a = new URL(urlA);
  const b = new URL(urlB);

  // Opaque origin 체크 (data:, file: 등)
  if (a.origin === 'null' || b.origin === 'null') {
    return false; // opaque origin은 자기 자신만 same-origin
  }

  return (
    a.protocol === b.protocol &&
    a.hostname === b.hostname &&
    a.port === b.port
  );
}

// 테스트
console.log(isSameOrigin(
  'https://example.com/a',
  'https://example.com/b'
)); // true

console.log(isSameOrigin(
  'https://example.com',
  'https://example.com:443'
)); // true (기본 포트)

console.log(isSameOrigin(
  'http://example.com',
  'https://example.com'
)); // false (scheme 다름)
```

### 4.4 비교 결과 요약

| Origin A | Origin B | Same Origin? | 이유 |
|----------|----------|:------------:|------|
| `https://a.com` | `https://a.com` | O | 모두 일치 |
| `https://a.com:443` | `https://a.com` | O | 기본 포트 일치 |
| `http://a.com:80` | `http://a.com` | O | 기본 포트 일치 |
| `https://a.com` | `http://a.com` | X | scheme 불일치 |
| `https://a.com` | `https://b.a.com` | X | host 불일치 |
| `https://a.com:443` | `https://a.com:8443` | X | port 불일치 |
| `null (data:)` | `null (data:)` | X | 서로 다른 opaque |
| `null (same data:)` | `null (same data:)` | O | 동일 opaque 인스턴스 |

---

## 5. Same-Origin Policy

### 5.1 기본 원칙

> "다른 Origin의 리소스에 대한 읽기/쓰기 접근을 기본적으로 차단"

```
┌─────────────────────────────────────────────────────────────┐
│                      브라우저                                │
│  ┌───────────────────┐    ┌───────────────────┐            │
│  │ Origin A          │    │ Origin B          │            │
│  │ (example.com)     │    │ (other.com)       │            │
│  │                   │    │                   │            │
│  │ ┌───────────┐     │    │ ┌───────────┐     │            │
│  │ │  DOM      │     │ X  │ │  DOM      │     │            │
│  │ │  Cookie   │◄────┼────┼►│  Cookie   │     │            │
│  │ │  Storage  │     │    │ │  Storage  │     │            │
│  │ └───────────┘     │    │ └───────────┘     │            │
│  └───────────────────┘    └───────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 제한되는 작업

| 작업 | 동일 Origin | 다른 Origin |
|------|:-----------:|:-----------:|
| DOM 접근 | O | X |
| Cookie 읽기 | O | X |
| LocalStorage 접근 | O | X |
| AJAX 요청 (읽기) | O | X (CORS 필요) |
| iframe 내용 접근 | O | X |
| Canvas 이미지 추출 | O | X |

### 5.3 허용되는 작업

일부 크로스 Origin 작업은 **기본적으로 허용** 됩니다:

| 작업 | 설명 |
|------|------|
| **링크** | `<a href="...">` 다른 Origin으로 네비게이션 |
| **폼 제출** | `<form action="...">` 다른 Origin으로 전송 |
| **이미지 로드** | `<img src="...">` |
| **스크립트 로드** | `<script src="...">` |
| **스타일시트** | `<link rel="stylesheet">` |
| **iframe 삽입** | `<iframe src="...">` (내용 접근은 불가) |

---

## 5. CORS (Cross-Origin Resource Sharing)

### 5.1 개요

CORS는 **서버가 명시적으로 허용한 경우** 크로스 Origin 요청을 가능하게 합니다.

### 5.2 Simple Request

**조건** 을 만족하면 Preflight 없이 바로 요청:

```
메서드: GET, HEAD, POST
헤더: Accept, Accept-Language, Content-Language, Content-Type
Content-Type: application/x-www-form-urlencoded, multipart/form-data, text/plain
```

```http
# 요청
GET /api/data HTTP/1.1
Host: api.other.com
Origin: https://example.com

# 응답
HTTP/1.1 200 OK
Access-Control-Allow-Origin: https://example.com
```

### 5.3 Preflight Request

조건을 만족하지 않으면 **OPTIONS 사전 요청**:

```http
# Preflight 요청
OPTIONS /api/data HTTP/1.1
Host: api.other.com
Origin: https://example.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: Content-Type, X-Custom-Header

# Preflight 응답
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, X-Custom-Header
Access-Control-Max-Age: 86400

# 실제 요청
PUT /api/data HTTP/1.1
Host: api.other.com
Origin: https://example.com
Content-Type: application/json

{"data": "value"}
```

### 5.4 CORS 헤더

**요청 헤더** (브라우저가 자동 추가):

| 헤더 | 설명 |
|------|------|
| `Origin` | 요청 출처 |
| `Access-Control-Request-Method` | 실제 요청에서 사용할 메서드 |
| `Access-Control-Request-Headers` | 실제 요청에서 사용할 헤더 |

**응답 헤더** (서버가 설정):

| 헤더 | 설명 |
|------|------|
| `Access-Control-Allow-Origin` | 허용할 Origin (`*` 또는 특정 Origin) |
| `Access-Control-Allow-Methods` | 허용할 메서드 |
| `Access-Control-Allow-Headers` | 허용할 헤더 |
| `Access-Control-Allow-Credentials` | 인증 정보 포함 허용 |
| `Access-Control-Expose-Headers` | 클라이언트에 노출할 헤더 |
| `Access-Control-Max-Age` | Preflight 캐시 시간 |

### 5.5 자격 증명 (Credentials)

쿠키나 인증 정보를 포함하려면:

```javascript
// 클라이언트
fetch('https://api.other.com/data', {
  credentials: 'include'
});

// 서버 응답 (반드시 특정 Origin 지정)
Access-Control-Allow-Origin: https://example.com
Access-Control-Allow-Credentials: true
```

> **주의**: `credentials: include` 시 `Access-Control-Allow-Origin: *` 불가

---

## 6. Origin 헤더

### 6.1 Origin 헤더 전송 시점

| 상황 | Origin 헤더 |
|------|-------------|
| Cross-Origin 요청 | 항상 전송 |
| Same-Origin POST 요청 | 전송 |
| Same-Origin GET 요청 | 전송 안 함 |
| Preflight 요청 | 항상 전송 |

### 6.2 Origin 헤더 값

```http
# 일반적인 경우
Origin: https://example.com

# Opaque Origin (직렬화 불가)
Origin: null
```

### 6.3 Origin vs Referer

| 헤더 | 내용 | 개인정보 |
|------|------|----------|
| **Origin** | scheme + host + port만 | 낮음 |
| **Referer** | 전체 URL (경로 포함) | 높음 |

```http
# Origin
Origin: https://example.com

# Referer
Referer: https://example.com/page/subpage?query=value
```

---

## 7. 보안 고려사항

### 7.1 Origin 스푸핑

> "서버는 Origin 헤더를 신뢰할 수 있는 클라이언트(브라우저)에서만 신뢰해야 합니다."

- 브라우저는 Origin 헤더를 위조할 수 없음
- curl, Postman 등은 임의로 설정 가능
- **CORS는 브라우저 보안 메커니즘**

### 7.2 Null Origin 주의

```http
Access-Control-Allow-Origin: null
```

**위험**: `data:`, `file:`, 샌드박스 iframe 등 다양한 소스가 `null` Origin을 가짐

### 7.3 와일드카드 주의

```http
# 모든 Origin 허용 (주의 필요)
Access-Control-Allow-Origin: *
```

| 상황 | `*` 허용 가능? |
|------|:--------------:|
| 공개 API (인증 없음) | O |
| 인증 필요한 API | X |
| 사용자 데이터 반환 | X |

### 7.4 CORS 설정 예시

```javascript
// Express.js 예시
const corsOptions = {
  origin: function(origin, callback) {
    const allowedOrigins = [
      'https://example.com',
      'https://app.example.com'
    ];
    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
};

app.use(cors(corsOptions));
```

---

## 8. 관련 보안 정책

### 8.1 Content Security Policy (CSP)

Origin 기반으로 리소스 로드 제한:

```http
Content-Security-Policy:
  default-src 'self';
  script-src 'self' https://trusted.cdn.com;
  img-src *;
  connect-src https://api.example.com
```

### 8.2 SameSite Cookie

쿠키의 크로스 사이트 전송 제어:

```http
Set-Cookie: session=abc123; SameSite=Strict
Set-Cookie: session=abc123; SameSite=Lax
Set-Cookie: session=abc123; SameSite=None; Secure
```

| SameSite 값 | 동작 |
|-------------|------|
| **Strict** | 동일 사이트만 전송 |
| **Lax** | 탑레벨 네비게이션 시 전송 |
| **None** | 항상 전송 (Secure 필수) |

### 8.3 X-Frame-Options / frame-ancestors

iframe 삽입 제어:

```http
X-Frame-Options: DENY
X-Frame-Options: SAMEORIGIN

# CSP 대체
Content-Security-Policy: frame-ancestors 'self' https://trusted.com
```

---

## 9. JavaScript에서 Origin 확인

### 9.1 document.domain

```javascript
// 현재 문서의 도메인 (수정 가능했으나 더 이상 권장하지 않음)
console.log(document.domain); // "example.com"
```

### 9.2 location.origin

```javascript
// 현재 페이지의 origin
console.log(location.origin); // "https://example.com"
```

### 9.3 URL API

```javascript
const url = new URL('https://example.com:8080/path');
console.log(url.origin);   // "https://example.com:8080"
console.log(url.protocol); // "https:"
console.log(url.host);     // "example.com:8080"
console.log(url.hostname); // "example.com"
console.log(url.port);     // "8080"
```

### 9.4 postMessage Origin 검증

```javascript
// 수신 측
window.addEventListener('message', (event) => {
  // Origin 검증 필수!
  if (event.origin !== 'https://trusted.com') {
    return;
  }
  console.log('Received:', event.data);
});

// 송신 측
otherWindow.postMessage('Hello', 'https://target.com');
```

---

## 10. 요약

RFC 6454는 웹 보안의 핵심인 Origin 개념을 정의합니다:

- **Origin = (scheme, host, port)**: 세 요소가 모두 같아야 동일 Origin
- **Same-Origin Policy**: 기본적으로 다른 Origin 리소스 접근 차단
- **CORS**: 서버가 명시적으로 허용한 크로스 Origin 요청
- **Opaque Origin**: 직렬화할 수 없는 Origin (`null`)
- **보안**: Origin 헤더는 브라우저만 신뢰, 서버 측 검증 필수

Origin은 **XSS, CSRF** 등 웹 공격을 방어하는 **브라우저 보안 모델의 기반** 입니다.

---

## 참고 자료

- [RFC 6454 원문](https://www.rfc-editor.org/rfc/rfc6454)
- [MDN - Same-origin policy](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy)
- [MDN - CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Fetch Standard - CORS Protocol](https://fetch.spec.whatwg.org/#http-cors-protocol)
