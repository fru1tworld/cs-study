# RFC 7519 - JSON Web Token (JWT)

> **발행일**: 2015년 5월
> **상태**: Standards Track

## 1. 개요

JWT(JSON Web Token)는 두 당사자 간에 안전하게 **클레임(claims)** 을 전달하기 위한 **컴팩트하고 URL-safe한 토큰 형식** 입니다. OAuth 2.0 및 OpenID Connect에서 널리 사용됩니다.

### 핵심 특징

| 특징 | 설명 |
|------|------|
| **자체 포함(Self-contained)** | 필요한 모든 정보를 토큰 자체에 포함 |
| **컴팩트(Compact)** | URL, 헤더, 쿠키에 쉽게 전달 가능 |
| **서명/암호화** | JWS(서명) 또는 JWE(암호화) 적용 가능 |
| **JSON 기반** | 페이로드가 JSON 형식 |

---

## 2. JWT 구조

### 2.1 기본 형식

```
Header.Payload.Signature
```

**예시**:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### 2.2 세 부분 구성

```
┌─────────────────────────────────────────────────────────────┐
│                        JWT Token                            │
├────────────────┬────────────────────┬───────────────────────┤
│     Header     │      Payload       │      Signature        │
│   (헤더)       │    (페이로드)       │       (서명)          │
├────────────────┼────────────────────┼───────────────────────┤
│  Base64Url     │     Base64Url      │      Base64Url        │
│  인코딩        │     인코딩          │       인코딩          │
└────────────────┴────────────────────┴───────────────────────┘
         │                  │                    │
         └──────────────────┼────────────────────┘
                            │
                     "." 으로 연결
```

---

## 3. Header (헤더)

### 3.1 구조

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### 3.2 헤더 파라미터

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| **alg** | 필수 | 서명 알고리즘 |
| **typ** | 선택 | 토큰 타입 (일반적으로 "JWT") |
| **kid** | 선택 | 키 식별자 |
| **cty** | 선택 | 콘텐츠 타입 (중첩 JWT 시) |

### 3.3 서명 알고리즘 (alg)

| 알고리즘 | 설명 | 키 유형 |
|----------|------|---------|
| **HS256** | HMAC with SHA-256 | 대칭키 |
| **HS384** | HMAC with SHA-384 | 대칭키 |
| **HS512** | HMAC with SHA-512 | 대칭키 |
| **RS256** | RSA with SHA-256 | 비대칭키 (RSA) |
| **RS384** | RSA with SHA-384 | 비대칭키 (RSA) |
| **RS512** | RSA with SHA-512 | 비대칭키 (RSA) |
| **ES256** | ECDSA with SHA-256 | 비대칭키 (타원곡선) |
| **ES384** | ECDSA with SHA-384 | 비대칭키 (타원곡선) |
| **ES512** | ECDSA with SHA-512 | 비대칭키 (타원곡선) |
| **PS256** | RSA-PSS with SHA-256 | 비대칭키 (RSA-PSS) |
| **none** | 서명 없음 | 없음 (위험!) |

---

## 4. Payload (페이로드)

### 4.1 클레임 (Claims)

페이로드는 **클레임(claims)** 의 집합으로, 엔티티와 추가 데이터에 대한 정보를 담습니다.

```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "admin": true,
  "iat": 1516239022
}
```

### 4.2 등록된 클레임 (Registered Claims)

| 클레임 | 이름 | 설명 |
|--------|------|------|
| **iss** | Issuer | 토큰 발급자 |
| **sub** | Subject | 토큰 주체 (사용자 ID 등) |
| **aud** | Audience | 토큰 수신자 |
| **exp** | Expiration Time | 만료 시간 (Unix timestamp) |
| **nbf** | Not Before | 토큰 활성화 시간 |
| **iat** | Issued At | 토큰 발급 시간 |
| **jti** | JWT ID | 토큰 고유 식별자 |

### 4.3 클레임 유형

```
┌───────────────────────────────────────────────────────┐
│                      Claims                           │
├─────────────────┬─────────────────┬───────────────────┤
│   Registered    │     Public      │     Private       │
│   (등록된)      │    (공개)       │     (비공개)       │
├─────────────────┼─────────────────┼───────────────────┤
│ iss, sub, aud   │  IANA 등록 또는 │   발급자와 수신자  │
│ exp, nbf, iat   │  충돌 방지 URI  │   간의 합의       │
│ jti             │                 │                   │
└─────────────────┴─────────────────┴───────────────────┘
```

### 4.4 예시: 액세스 토큰

```json
{
  "iss": "https://auth.example.com",
  "sub": "user123",
  "aud": "https://api.example.com",
  "exp": 1735689600,
  "iat": 1735686000,
  "scope": "read write",
  "roles": ["user", "admin"]
}
```

---

## 5. Signature (서명)

### 5.1 서명 생성

```
HMAC-SHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### 5.2 대칭키 (HS256) 예시

```javascript
// Node.js 예시
const crypto = require('crypto');

const header = base64UrlEncode(JSON.stringify({ alg: "HS256", typ: "JWT" }));
const payload = base64UrlEncode(JSON.stringify({ sub: "1234567890" }));
const signature = crypto
  .createHmac('sha256', secret)
  .update(`${header}.${payload}`)
  .digest('base64url');

const jwt = `${header}.${payload}.${signature}`;
```

### 5.3 비대칭키 (RS256) 예시

```javascript
// 서명 생성 (개인키 사용)
const sign = crypto.createSign('RSA-SHA256');
sign.update(`${header}.${payload}`);
const signature = sign.sign(privateKey, 'base64url');

// 서명 검증 (공개키 사용)
const verify = crypto.createVerify('RSA-SHA256');
verify.update(`${header}.${payload}`);
const isValid = verify.verify(publicKey, signature, 'base64url');
```

---

## 6. JWT 생성 및 검증

### 6.1 생성 과정

```
1. 헤더 JSON 생성
   {"alg": "HS256", "typ": "JWT"}

2. 페이로드 JSON 생성
   {"sub": "1234567890", "name": "John Doe", "iat": 1516239022}

3. Base64Url 인코딩
   Header: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
   Payload: eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ

4. 서명 생성
   HMAC-SHA256(header + "." + payload, secret)
   → SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

5. 토큰 조합
   header.payload.signature
```

### 6.2 검증 과정

```
1. 토큰을 "."으로 분리
   [header, payload, signature]

2. 헤더 디코딩하여 알고리즘 확인

3. 서명 검증
   - 동일한 알고리즘과 키로 서명 재계산
   - 전달받은 서명과 비교

4. 클레임 검증
   - exp: 현재 시간보다 미래인지
   - nbf: 현재 시간보다 과거인지
   - iss: 예상 발급자인지
   - aud: 올바른 수신자인지

5. 검증 성공 시 페이로드 반환
```

---

## 7. 프로그래밍 언어별 예시

### 7.1 Node.js (jsonwebtoken)

```javascript
const jwt = require('jsonwebtoken');

// 토큰 생성
const token = jwt.sign(
  { userId: '12345', role: 'admin' },
  'secret-key',
  { expiresIn: '1h', issuer: 'my-app' }
);

// 토큰 검증
try {
  const decoded = jwt.verify(token, 'secret-key');
  console.log(decoded);
} catch (err) {
  console.error('Invalid token:', err.message);
}
```

### 7.2 Python (PyJWT)

```python
import jwt
from datetime import datetime, timedelta

# 토큰 생성
payload = {
    'user_id': '12345',
    'exp': datetime.utcnow() + timedelta(hours=1),
    'iat': datetime.utcnow()
}
token = jwt.encode(payload, 'secret-key', algorithm='HS256')

# 토큰 검증
try:
    decoded = jwt.decode(token, 'secret-key', algorithms=['HS256'])
    print(decoded)
except jwt.ExpiredSignatureError:
    print('Token expired')
except jwt.InvalidTokenError:
    print('Invalid token')
```

### 7.3 Java (jjwt)

```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// 토큰 생성
String token = Jwts.builder()
    .setSubject("12345")
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 3600000))
    .signWith(SignatureAlgorithm.HS256, "secret-key")
    .compact();

// 토큰 검증
Claims claims = Jwts.parser()
    .setSigningKey("secret-key")
    .parseClaimsJws(token)
    .getBody();
```

### 7.4 Go (golang-jwt)

```go
import "github.com/golang-jwt/jwt/v5"

// 토큰 생성
claims := jwt.MapClaims{
    "user_id": "12345",
    "exp":     time.Now().Add(time.Hour).Unix(),
}
token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
tokenString, _ := token.SignedString([]byte("secret-key"))

// 토큰 검증
parsed, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
    return []byte("secret-key"), nil
})
if claims, ok := parsed.Claims.(jwt.MapClaims); ok && parsed.Valid {
    fmt.Println(claims["user_id"])
}
```

---

## 8. 사용 사례

### 8.1 인증 (Authentication)

```http
# 로그인
POST /login HTTP/1.1
Content-Type: application/json

{"username": "user", "password": "pass"}

# 응답
HTTP/1.1 200 OK
Content-Type: application/json

{"token": "eyJhbGciOiJIUzI1NiIs..."}

# API 요청
GET /api/profile HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### 8.2 정보 교환

```json
{
  "iss": "service-a",
  "aud": "service-b",
  "data": {
    "orderId": "ORD-123",
    "amount": 50000
  },
  "exp": 1735689600
}
```

### 8.3 싱글 사인온 (SSO)

```
1. 사용자가 ID 제공자에서 로그인
2. ID 제공자가 JWT 발급 (여러 서비스의 aud 포함)
3. 사용자가 JWT로 여러 서비스에 접근
4. 각 서비스는 JWT 검증만으로 인증 완료
```

---

## 9. 보안 고려사항

### 9.1 알고리즘 혼동 공격 방지

```javascript
// 위험! alg 클레임을 신뢰함
const decoded = jwt.decode(token);
const verified = jwt.verify(token, key, { algorithms: [decoded.alg] });

// 안전! 서버가 알고리즘 지정
const verified = jwt.verify(token, key, { algorithms: ['RS256'] });
```

### 9.2 none 알고리즘 비활성화

```javascript
// 위험! none 허용
jwt.verify(token, null, { algorithms: ['none'] });

// 안전! none 제외
jwt.verify(token, key, { algorithms: ['HS256', 'RS256'] });
```

### 9.3 키 관리

| 권장사항 | 설명 |
|----------|------|
| 충분한 키 길이 | HS256: 최소 256비트, RS256: 최소 2048비트 |
| 키 로테이션 | 정기적으로 키 교체 |
| 안전한 저장 | 환경 변수, 키 관리 서비스 사용 |
| 키 분리 | 용도별 다른 키 사용 |

### 9.4 토큰 저장

```
브라우저 저장소 비교:

localStorage:
  - XSS 공격에 취약
  - 쉬운 접근

sessionStorage:
  - XSS 공격에 취약
  - 탭 간 공유 안 됨

HttpOnly Cookie:
  - XSS로부터 보호
  - CSRF 방어 필요
  - SameSite 속성 설정 권장
```

### 9.5 클레임 검증

```javascript
// 필수 검증 항목
const options = {
  issuer: 'https://auth.example.com',    // iss 검증
  audience: 'https://api.example.com',   // aud 검증
  clockTolerance: 30,                     // 시계 오차 허용
  algorithms: ['RS256']                   // 알고리즘 제한
};

jwt.verify(token, publicKey, options);
```

### 9.6 민감 정보 제외

```json
// 위험! 페이로드는 암호화되지 않음
{
  "password": "secret123",
  "creditCard": "1234-5678-9012-3456"
}

// 안전! 민감하지 않은 정보만
{
  "userId": "12345",
  "role": "user"
}
```

---

## 10. JWT vs 세션

| 특성 | JWT | 세션 |
|------|-----|------|
| 상태 | Stateless | Stateful |
| 저장 위치 | 클라이언트 | 서버 |
| 확장성 | 높음 (서버 메모리 불필요) | 낮음 (세션 공유 필요) |
| 무효화 | 어려움 | 쉬움 (삭제하면 됨) |
| 크기 | 상대적으로 큼 | 세션 ID만 전송 |
| 네트워크 | 매 요청마다 전체 토큰 | 세션 ID만 |

---

## 11. 요약

RFC 7519 JWT는 안전한 클레임 전달을 위한 토큰 형식입니다:

- **구조**: Header.Payload.Signature (Base64Url 인코딩)
- **클레임**: 등록된 클레임(iss, sub, exp 등) + 사용자 정의 클레임
- **서명**: HMAC(대칭), RSA/ECDSA(비대칭)
- **용도**: 인증, 정보 교환, SSO
- **주의**: 암호화가 아닌 서명만 제공 (민감 정보 포함 금지)

JWT는 **마이크로서비스, API 인증, OAuth 2.0** 에서 핵심적으로 사용되는 표준입니다.

---

## 참고 자료

- [RFC 7519 원문](https://www.rfc-editor.org/rfc/rfc7519)
- [RFC 7515 JWS](https://www.rfc-editor.org/rfc/rfc7515) - JSON Web Signature
- [RFC 7516 JWE](https://www.rfc-editor.org/rfc/rfc7516) - JSON Web Encryption
- [RFC 7517 JWK](https://www.rfc-editor.org/rfc/rfc7517) - JSON Web Key
- [jwt.io](https://jwt.io/) - JWT 디버거 및 라이브러리 목록
