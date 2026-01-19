# RFC 7636: Proof Key for Code Exchange (PKCE)

> OAuth 2.0 공개 클라이언트를 위한 코드 교환용 증명 키

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 7636 |
| **분류** | Standards Track (표준 트랙) |
| **작성자** | N. Sakimura (NRI), J. Bradley (Ping Identity), N. Agarwal (Google) |
| **발행일** | 2015년 9월 |
| **상태** | 현행 표준 (Proposed Standard) |
| **관련 RFC** | RFC 6749 (OAuth 2.0), RFC 6819 (OAuth 위협 모델) |

## 개요

RFC 7636은 **PKCE(Proof Key for Code Exchange)**, 발음 "픽시(pixy)"를 정의합니다. 이 명세는 OAuth 2.0 공개 클라이언트(public client)가 인가 코드 그랜트(Authorization Code Grant)를 사용할 때 발생할 수 있는 **인가 코드 가로채기 공격(Authorization Code Interception Attack)**을 방지하기 위한 보안 확장 기능입니다.

### 핵심 문제

OAuth 2.0 인가 코드 그랜트 방식에서 공개 클라이언트(특히 네이티브 앱)는 다음과 같은 취약점에 노출됩니다:

```
┌─────────────────────────────────────────────────────────────────┐
│                    인가 코드 가로채기 공격                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 정상 앱과 악성 앱이 동일한 커스텀 URI 스킴 등록               │
│     예: myapp://callback                                        │
│                                                                 │
│  2. 사용자가 정상 앱에서 OAuth 인증 시작                         │
│     → 브라우저로 인가 서버 접속                                  │
│                                                                 │
│  3. 사용자 인증 완료 후 리다이렉트 발생                          │
│     → myapp://callback?code=AUTH_CODE                           │
│                                                                 │
│  4. OS가 악성 앱을 URI 핸들러로 선택할 수 있음                   │
│     → 악성 앱이 인가 코드 탈취!                                  │
│                                                                 │
│  5. 악성 앱이 탈취한 코드로 액세스 토큰 획득                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

이 공격이 가능한 이유:
- 공개 클라이언트는 클라이언트 시크릿을 안전하게 저장할 수 없음
- 모바일 OS에서 여러 앱이 동일한 커스텀 URI 스킴을 등록 가능
- 인가 코드만으로 토큰을 요청할 수 있음

---

## 키워드 정의

이 문서에서 사용된 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL"이라는 키워드는 RFC 2119에 기술된 대로 해석되어야 합니다.

---

## 용어 정의

### code_verifier (코드 검증자)

클라이언트가 생성하는 **암호학적으로 안전한 무작위 문자열**입니다. 인가 요청과 토큰 요청을 상호 연결하는 역할을 합니다.

```
특성:
- 길이: 43자 ~ 128자
- 허용 문자: [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
- ABNF: code-verifier = 43*128unreserved
- unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
```

**엔트로피 요구사항**: 최소 256비트의 엔트로피를 가져야 합니다(SHOULD). 일반적으로 32옥텟(256비트)의 무작위 데이터를 base64url 인코딩하여 43자의 문자열을 생성합니다.

### code_challenge (코드 챌린지)

code_verifier에서 **변환 메서드를 통해 파생된 값**입니다. 인가 요청 시 서버에 전송됩니다.

```
특성:
- 길이: 43자 ~ 128자
- 허용 문자: [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
- ABNF: code-challenge = 43*128unreserved
```

### code_challenge_method (코드 챌린지 메서드)

code_verifier를 code_challenge로 변환하는 **알고리즘**을 지정합니다.

| 메서드 | 설명 | 보안 수준 |
|--------|------|----------|
| `plain` | 변환 없이 그대로 사용 | 낮음 (TLS 의존) |
| `S256` | SHA-256 해시 후 base64url 인코딩 | 높음 (필수 구현) |

---

## 코드 챌린지 메서드

### plain 메서드

변환 없이 code_verifier를 그대로 code_challenge로 사용합니다.

```
code_challenge = code_verifier
```

**특징**:
- 하위 호환성을 위해 존재
- TLS 및 OS 보호에 전적으로 의존
- 요청 경로가 이미 보호된 경우에만 사용해야 함(SHOULD)
- 도청자가 인가 요청을 볼 수 있다면 code_verifier 노출

### S256 메서드 (필수 구현)

code_verifier를 SHA-256으로 해시하고 base64url 인코딩합니다.

```
code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))
```

**변환 과정**:

```
1. code_verifier를 ASCII 옥텟 시퀀스로 변환
2. SHA-256 해시 함수 적용 (32바이트 출력)
3. 해시 결과를 base64url 인코딩 (패딩 없음)
4. 결과: 43자 문자열
```

**특징**:
- 모든 클라이언트와 서버가 반드시 지원해야 함(MUST)
- 도청자가 code_challenge를 보더라도 code_verifier 역산 불가
- 브루트포스 공격으로부터 보호

**구현 요구사항**:
- 클라이언트: 기술적으로 불가능하지 않는 한 S256을 사용해야 함(SHOULD)
- 서버: S256을 반드시 지원해야 함(MUST)

---

## 프로토콜 흐름

### 전체 흐름도

```
                                                 +-------------------+
                                                 |   인가 서버       |
       +--------+                                | +---------------+ |
       |        |--(A)- 인가 요청 + ------------>| |               | |
       |        |       code_challenge           | |     인가      | |
       |        |                                | |     엔드포인트 | |
       |        |<-(B)- 인가 코드 ---------------| |               | |
       |        |                                | +---------------+ |
       | 클라   |                                |                   |
       | 이언트 |                                | +---------------+ |
       |        |--(C)- 인가 코드 + ------------>| |               | |
       |        |       code_verifier            | |     토큰      | |
       |        |                                | |     엔드포인트 | |
       |        |<-(D)- 액세스 토큰 -------------| |               | |
       +--------+                                | +---------------+ |
                                                 +-------------------+

                     그림 1: 프로토콜 흐름 개요
```

### 단계별 상세 설명

#### (A) 인가 요청

클라이언트가 code_verifier를 생성하고, 이로부터 code_challenge를 도출하여 인가 요청에 포함합니다.

```http
GET /authorize?
    response_type=code&
    client_id=s6BhdRkqt3&
    state=xyz&
    redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb&
    code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&
    code_challenge_method=S256
HTTP/1.1
Host: server.example.com
```

**추가 파라미터**:

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `code_challenge` | REQUIRED | 변환된 code_verifier |
| `code_challenge_method` | OPTIONAL | 변환 메서드 (기본값: "plain") |

#### (B) 인가 응답

인가 서버는 code_challenge와 code_challenge_method를 저장하고, 평소와 같이 인가 코드를 발급합니다.

```http
HTTP/1.1 302 Found
Location: https://client.example.com/cb?
    code=SplxlOBeZQQYbYS6WxSbIA&
    state=xyz
```

**서버 동작**:
- code_challenge를 인가 코드와 연결하여 저장
- code_challenge_method도 함께 저장
- 저장 방식: 서버 측 저장 또는 인가 코드 자체에 암호화하여 포함

#### (C) 토큰 요청

클라이언트가 인가 코드와 함께 원본 code_verifier를 전송합니다.

```http
POST /token HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
code=SplxlOBeZQQYbYS6WxSbIA&
redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb&
code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk&
client_id=s6BhdRkqt3
```

**추가 파라미터**:

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `code_verifier` | REQUIRED* | 인가 요청에서 사용한 원본 검증자 |

*PKCE를 사용하는 경우 필수

#### (D) 검증 및 토큰 발급

서버가 code_verifier를 검증하고, 성공 시 토큰을 발급합니다.

```
검증 과정:

1. 저장된 code_challenge_method 확인
2. 수신한 code_verifier에 해당 메서드 적용
   - plain: code_verifier 그대로 사용
   - S256: BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))
3. 결과와 저장된 code_challenge 비교
4. 일치하면 토큰 발급, 불일치하면 오류 반환
```

**검증 성공 시**:
```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8

{
    "access_token": "2YotnFZFEjr1zCsicMWpAA",
    "token_type": "Bearer",
    "expires_in": 3600,
    "refresh_token": "tGzv3JOkF0XG5Qx2TlKWIA"
}
```

**검증 실패 시**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json;charset=UTF-8

{
    "error": "invalid_grant",
    "error_description": "Code verifier does not match"
}
```

---

## 클라이언트 구현 요구사항

### code_verifier 생성

```
필수 요구사항:
├── 길이: 43 ~ 128자
├── 문자셋: unreserved characters only
├── 엔트로피: 최소 256비트 (SHOULD)
└── 생성 방법: 암호학적으로 안전한 난수 생성기 사용 (MUST)
```

**권장 생성 방법**:
```
1. 32옥텟(256비트)의 암호학적 무작위 바이트 생성
2. base64url 인코딩 (패딩 제거)
3. 결과: 43자의 code_verifier
```

**예시 (의사 코드)**:
```python
import secrets
import base64

# 32바이트 무작위 데이터 생성
random_bytes = secrets.token_bytes(32)

# base64url 인코딩 (패딩 없음)
code_verifier = base64.urlsafe_b64encode(random_bytes).rstrip(b'=').decode('ascii')
```

### code_challenge 생성

```python
import hashlib
import base64

# S256 메서드
def create_code_challenge(code_verifier):
    # ASCII 인코딩
    verifier_bytes = code_verifier.encode('ascii')

    # SHA-256 해시
    digest = hashlib.sha256(verifier_bytes).digest()

    # base64url 인코딩 (패딩 없음)
    code_challenge = base64.urlsafe_b64encode(digest).rstrip(b'=').decode('ascii')

    return code_challenge
```

### 클라이언트 동작 규칙

| 상황 | 동작 |
|------|------|
| 새 인가 요청마다 | 새로운 code_verifier 생성 (MUST) |
| 메서드 선택 | S256 우선 사용 (SHOULD) |
| S256 지원 불가 시 | plain 사용 가능 |
| PKCE 미지원 서버 | 추가 파라미터 무시됨, 정상 동작 |
| S256 지원하는 서버에서 plain만 요청 | 다운그레이드 공격 가능성, S256 사용 (SHOULD NOT) |

---

## 서버 구현 요구사항

### 인가 엔드포인트

```
필수 구현:
├── code_challenge 파라미터 수신 처리
├── code_challenge_method 파라미터 수신 처리
├── S256 메서드 지원 (MUST)
├── plain 메서드 지원 (MAY)
├── code_challenge를 인가 코드와 연결 저장
└── 지원하지 않는 메서드 요청 시 오류 반환
```

**오류 응답 (PKCE 필수인데 code_challenge 누락 시)**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
    "error": "invalid_request",
    "error_description": "code_challenge required"
}
```

**오류 응답 (지원하지 않는 메서드 요청 시)**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
    "error": "invalid_request",
    "error_description": "Unsupported code_challenge_method"
}
```

### 토큰 엔드포인트

```
검증 절차:
1. 인가 코드와 연결된 code_challenge 조회
2. code_verifier 수신 확인
3. code_challenge_method에 따라 변환 수행
4. 변환 결과와 저장된 code_challenge 비교
5. 일치: 토큰 발급, 불일치: invalid_grant 오류
```

**서버 정책 옵션**:

| 정책 | 설명 |
|------|------|
| PKCE 필수 | code_challenge 없는 인가 요청 거부 |
| PKCE 선택 | code_challenge 없어도 허용 (하위 호환성) |
| S256 전용 | plain 메서드 거부 |

---

## 보안 고려사항

### 엔트로피 요구사항

code_verifier는 **최소 256비트의 엔트로피**를 가져야 합니다. 이는 브루트포스 공격을 방지하기 위함입니다.

```
계산 예시:
- 256비트 엔트로피
- 2^256 ≈ 1.16 × 10^77 가능한 조합
- 초당 10억 회 시도해도 약 3.7 × 10^60 년 소요
→ 브루트포스 공격 실질적으로 불가능
```

### 도청 보호

| 메서드 | 도청 보호 |
|--------|----------|
| plain | 보호 안 됨 - TLS에 의존 |
| S256 | 보호됨 - code_challenge만으로는 code_verifier 역산 불가 |

**S256의 보호 원리**:
```
공격자가 알 수 있는 정보:
├── code_challenge (인가 요청에서 획득)
├── code_challenge_method = "S256"
└── 인가 코드 (가로챌 경우)

공격자가 알 수 없는 정보:
└── code_verifier (클라이언트만 보유)

SHA-256의 단방향성으로 인해:
code_challenge → code_verifier 역산 불가능
```

### 다운그레이드 공격 방지

S256을 지원하는 서버에서 S256 요청에 오류가 발생하면 다음 중 하나입니다:

1. 서버 구현 오류
2. 중간자(MITM) 공격으로 인한 다운그레이드 시도

**권장 대응**:
- 클라이언트는 S256 → plain으로 다운그레이드하지 말아야 함(SHOULD NOT)
- 서버는 S256을 반드시 지원해야 함(MUST)

### 솔팅(Salting) 불필요

code_verifier의 높은 엔트로피(256비트)로 인해 솔팅이 불필요합니다:

```
솔팅의 목적: 레인보우 테이블 공격 방지

PKCE에서 불필요한 이유:
├── code_verifier가 이미 충분히 무작위
├── 매 요청마다 새로운 code_verifier 생성
├── 256비트 엔트로피로 레인보우 테이블 구축 비현실적
└── 추가 복잡성 없이 동일한 보안 수준 달성
```

### TLS 필수

모든 PKCE 교환은 TLS를 통해 이루어져야 합니다:

```
보호되어야 하는 통신:
├── 클라이언트 ↔ 인가 엔드포인트 (code_challenge 전송)
├── 클라이언트 ↔ 토큰 엔드포인트 (code_verifier 전송)
└── 리다이렉트 URI (인가 코드 전달)
```

### OAuth 2.0 위협 모델과의 관계

RFC 6819(OAuth 2.0 위협 모델)에서 권장하는 대책과의 관계:

| 권장 대책 | PKCE 적용 |
|----------|----------|
| 인스턴스별 클라이언트 시크릿 | 공개 클라이언트에서 불가능 |
| 인스턴스별 리다이렉트 URI | 플랫폼 제약으로 어려움 |
| **코드 바인딩 메커니즘** | **PKCE가 해결** |

---

## IANA 고려사항

### OAuth 파라미터 등록

**인가 엔드포인트 파라미터**:

| 파라미터 명 | 용도 | 변경 관리자 | 참조 |
|-------------|------|------------|------|
| code_challenge | 인가 요청 | IESG | RFC 7636 |
| code_challenge_method | 인가 요청 | IESG | RFC 7636 |

**토큰 엔드포인트 파라미터**:

| 파라미터 명 | 용도 | 변경 관리자 | 참조 |
|-------------|------|------------|------|
| code_verifier | 토큰 요청 | IESG | RFC 7636 |

### PKCE 코드 챌린지 메서드 레지스트리

새로운 레지스트리 "PKCE Code Challenge Methods"가 설립되었습니다.

**등록 정책**: Specification Required

**초기 등록 내용**:

| 메서드 명 | 변환 방식 | 참조 |
|----------|----------|------|
| plain | code_challenge = code_verifier | RFC 7636 |
| S256 | code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier))) | RFC 7636 |

---

## 구현 예제 (부록 B)

### S256 변환 예제

**1단계: 무작위 옥텟 시퀀스 생성**

```
32옥텟 (256비트) 무작위 데이터:
[116, 24, 223, 180, 151, 153, 224, 37, 79, 250, 96, 125, 216, 173,
 187, 186, 22, 212, 37, 77, 105, 214, 191, 240, 91, 88, 5, 88, 83,
 132, 141, 121]
```

**2단계: base64url 인코딩 (code_verifier)**

```
dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

**3단계: SHA-256 해시**

```
해시 입력: ASCII("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
해시 출력: [19, 211, 30, 150, 26, 26, 216, 236, 47, 22, 177, 12, 76,
           152, 46, 8, 118, 168, 120, 173, 109, 241, 68, 86, 110,
           225, 137, 74, 203, 112, 249, 195]
```

**4단계: base64url 인코딩 (code_challenge)**

```
E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
```

### 전체 흐름 예제

```
┌─────────────────────────────────────────────────────────────────┐
│                        예제 흐름                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [클라이언트]                                                   │
│  1. code_verifier 생성:                                        │
│     dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk                │
│                                                                 │
│  2. code_challenge 계산 (S256):                                │
│     E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM                │
│                                                                 │
│  ──────────────────────────────────────────────────────────    │
│                                                                 │
│  [인가 요청]                                                    │
│  GET /authorize?                                                │
│      response_type=code&                                        │
│      client_id=s6BhdRkqt3&                                     │
│      redirect_uri=https://client.example.com/cb&               │
│      code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&│
│      code_challenge_method=S256                                 │
│                                                                 │
│  ──────────────────────────────────────────────────────────    │
│                                                                 │
│  [인가 응답]                                                    │
│  302 Found                                                      │
│  Location: https://client.example.com/cb?code=SplxlOBeZQQ...   │
│                                                                 │
│  ──────────────────────────────────────────────────────────    │
│                                                                 │
│  [토큰 요청]                                                    │
│  POST /token                                                    │
│  grant_type=authorization_code&                                 │
│  code=SplxlOBeZQQ...&                                          │
│  redirect_uri=https://client.example.com/cb&                   │
│  code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk&    │
│  client_id=s6BhdRkqt3                                          │
│                                                                 │
│  ──────────────────────────────────────────────────────────    │
│                                                                 │
│  [서버 검증]                                                    │
│  1. 저장된 code_challenge 조회:                                │
│     E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM                │
│  2. 수신한 code_verifier에 S256 적용:                          │
│     SHA256("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")      │
│     = E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM              │
│  3. 비교: 일치! ✓                                               │
│  4. 토큰 발급                                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 하위 호환성

### PKCE 미지원 서버와의 호환

PKCE를 구현하는 클라이언트가 PKCE를 지원하지 않는 서버와 통신하는 경우:

```
동작 방식:
├── 클라이언트: code_challenge, code_challenge_method 전송
├── 서버: 알 수 없는 파라미터 무시 (OAuth 2.0 표준 동작)
├── 서버: 일반 인가 코드 발급
├── 클라이언트: code_verifier와 함께 토큰 요청
├── 서버: code_verifier 무시
└── 결과: 정상적인 OAuth 2.0 흐름 완료
```

**권장 사항**: 클라이언트는 서버의 PKCE 지원 여부와 관계없이 항상 PKCE 파라미터를 전송해야 합니다(SHOULD).

### PKCE 미지원 클라이언트와의 호환

PKCE를 지원하는 서버가 PKCE를 사용하지 않는 클라이언트를 처리하는 경우:

```
서버 정책 옵션:
├── 허용 (하위 호환성): code_verifier 없으면 검증 생략
└── 거부 (보안 강화): code_challenge 없는 요청 거부
```

---

## 참고 자료

### 규범적 참조 (Normative References)

| RFC | 제목 | 설명 |
|-----|------|------|
| RFC 2119 | Key words for use in RFCs | 요구사항 수준 키워드 정의 |
| RFC 4648 | Base16, Base32, Base64 Encodings | base64url 인코딩 정의 |
| RFC 6234 | US Secure Hash Algorithms | SHA-256 정의 |
| RFC 6749 | OAuth 2.0 Authorization Framework | OAuth 2.0 핵심 명세 |

### 정보적 참조 (Informative References)

| RFC | 제목 | 설명 |
|-----|------|------|
| RFC 6819 | OAuth 2.0 Threat Model | OAuth 2.0 보안 위협 분석 |
| BCP 195 | TLS Recommendations | TLS 사용 권장사항 |

---

## 관련 RFC

- **RFC 6749**: OAuth 2.0 인가 프레임워크
- **RFC 6819**: OAuth 2.0 위협 모델 및 보안 고려사항
- **RFC 7591**: OAuth 2.0 동적 클라이언트 등록
- **RFC 8252**: OAuth 2.0 네이티브 앱을 위한 모범 사례

---

## 요약

### PKCE의 핵심 가치

```
┌─────────────────────────────────────────────────────────────────┐
│                    PKCE가 제공하는 보호                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✓ 인가 코드 가로채기 공격 방지                                 │
│    - 공격자가 인가 코드를 가로채도 토큰 획득 불가               │
│    - code_verifier 없이는 토큰 요청 실패                        │
│                                                                 │
│  ✓ 클라이언트 시크릿 없이도 보안 유지                           │
│    - 공개 클라이언트(네이티브 앱)에서도 안전                    │
│    - 동적 비밀(code_verifier)로 요청 간 바인딩                  │
│                                                                 │
│  ✓ 간단한 구현                                                  │
│    - 클라이언트: 무작위 문자열 생성 + SHA-256 해시              │
│    - 서버: 추가 파라미터 저장 + 해시 비교                       │
│                                                                 │
│  ✓ 하위 호환성                                                  │
│    - 기존 OAuth 2.0과 완벽하게 호환                             │
│    - 점진적 도입 가능                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 구현 체크리스트

**클라이언트**:
- [ ] 암호학적으로 안전한 난수 생성기 사용
- [ ] 최소 256비트 엔트로피의 code_verifier 생성
- [ ] S256 메서드 우선 사용
- [ ] 매 인가 요청마다 새로운 code_verifier 생성
- [ ] code_verifier를 토큰 요청 시까지 안전하게 보관

**서버**:
- [ ] S256 메서드 지원
- [ ] code_challenge와 인가 코드 연결 저장
- [ ] 토큰 요청 시 code_verifier 검증
- [ ] 검증 실패 시 invalid_grant 오류 반환
- [ ] (선택) PKCE 필수 정책 적용

---

*이 문서는 RFC 7636 (Proof Key for Code Exchange by OAuth Public Clients)의 한국어 번역 및 정리본입니다.*
