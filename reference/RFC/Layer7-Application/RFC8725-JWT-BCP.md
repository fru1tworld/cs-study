# RFC 8725: JSON Web Token Best Current Practices

> JSON 웹 토큰(JWT) 구현 및 배포를 위한 현행 모범 사례

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 8725 |
| **분류** | BCP 225 (Best Current Practice) |
| **작성자** | Y. Sheffer (Intuit), D. Hardt, M. B. Jones (Microsoft) |
| **발행일** | 2020년 2월 |
| **상태** | 현행 표준 |
| **업데이트** | RFC 7519 |

## 개요

JSON 웹 토큰(JWT)은 두 당사자 간에 전송될 클레임(claims)을 나타내는 URL 안전(URL-safe) JSON 기반 보안 토큰입니다. JWT는 선택적으로 서명 및/또는 암호화될 수 있습니다.

이 문서는 JWT 메커니즘의 불완전한 명세와 구현으로 인해 발생한 **여러 보안 공격들에 대응** 하기 위한 모범 사례를 제공합니다. 실제 배포 환경에서 발견된 취약점들을 분석하고, 이를 방지하기 위한 구체적인 가이드라인을 제시합니다.

### 대상 독자

이 문서는 다음 세 가지 대상을 위해 작성되었습니다:

1. **JWT 라이브러리 구현자**: 안전한 JWT 라이브러리를 개발하기 위한 기술적 요구사항
2. **애플리케이션 개발자**: JWT 라이브러리를 안전하게 사용하기 위한 가이드라인
3. **명세 개발자**: JWT에 의존하는 프로토콜을 설계할 때 고려해야 할 사항

---

## 위협 및 취약점 (Threats and Vulnerabilities)

### 2.1 취약한 서명 및 불충분한 서명 검증

JWT 서명 검증과 관련하여 여러 알려진 공격이 존재합니다. 공격자는 `alg` 헤더 매개변수를 조작하여 검증 로직을 우회할 수 있습니다.

**주요 공격 유형:**

| 공격 방식 | 설명 | 영향 |
|-----------|------|------|
| **`alg: none` 공격** | `alg` 값을 `none`으로 변경하여 서명 검증 우회 | 임의의 JWT가 유효한 것으로 처리됨 |
| **RS256 to HS256 공격** | 비대칭 알고리즘(RS256)을 대칭 알고리즘(HS256)으로 변경 | 공개키가 HMAC 비밀키로 사용됨 |

```
공격 시나리오 (RS256 → HS256):

원본 JWT:
{
  "alg": "RS256",    ← RSA 공개키/개인키 쌍 사용
  "typ": "JWT"
}

공격 JWT:
{
  "alg": "HS256",    ← HMAC-SHA256으로 변경
  "typ": "JWT"
}

→ 라이브러리가 공개키를 HMAC 비밀키로 사용하여 서명을 검증
→ 공격자는 공개키(공개 정보)만으로 유효한 서명 생성 가능
```

---

### 2.2 취약한 대칭키

`HS256`과 같은 keyed-MAC 알고리즘을 사용할 때, **엔트로피가 낮은 대칭키** 를 사용하면 심각한 보안 위험이 발생합니다.

**위험 요소:**

- 인간이 기억할 수 있는 비밀번호를 직접 키로 사용
- 사전에 있는 단어나 짧은 문자열 사용
- 예측 가능한 패턴을 가진 키 사용

**공격 방식:**

```
공격자가 JWT를 획득한 후:

1. 오프라인 무차별 대입 공격 (Brute-force Attack)
   - 모든 가능한 키 조합을 시도
   - 서버와의 통신 없이 로컬에서 수행

2. 사전 공격 (Dictionary Attack)
   - 일반적으로 사용되는 비밀번호 목록을 사용
   - 레인보우 테이블 활용 가능

→ 취약한 키 사용 시 수 분 내에 키 복구 가능
```

---

### 2.3 암호화와 서명의 잘못된 조합

JWT가 서명된 후 암호화되는 경우(Nested JWT), 일부 라이브러리에서 **내부 서명 검증을 생략** 하는 문제가 발생합니다.

```
올바른 Nested JWT 처리:

┌─────────────────────────────────────┐
│           JWE (암호화 계층)          │
│  ┌─────────────────────────────┐    │
│  │       JWS (서명 계층)        │    │  ← 복호화 후 서명도 반드시 검증해야 함
│  │  ┌─────────────────────┐    │    │
│  │  │   JWT Claims        │    │    │
│  │  │   (실제 페이로드)    │    │    │
│  │  └─────────────────────┘    │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘

잘못된 구현:
1. JWE 복호화 성공 ✓
2. 내부 JWS 서명 검증 건너뜀 ✗
→ 조작된 클레임이 유효한 것으로 처리됨
```

---

### 2.4 암호문 길이 분석을 통한 평문 유출

암호화 전에 **압축을 적용** 하면, 암호문의 길이를 통해 평문에 대한 정보가 유출될 수 있습니다.

**CRIME/BREACH 스타일 공격:**

```
압축 + 암호화의 위험성:

1. 압축 알고리즘은 반복되는 패턴을 제거
2. 공격자가 제어하는 데이터와 비밀 데이터가 같은 압축 공간에 있을 때

공격 시나리오:
┌────────────────────────────────┐
│  비밀 데이터: "secret=ABC123"  │
│  공격자 데이터: "secret=A"     │
└────────────────────────────────┘
                ↓ 압축
        "secret=" 패턴이 반복되어 압축됨
                ↓
        더 짧은 암호문 생성
                ↓
공격자는 암호문 길이 변화를 관찰하여 한 글자씩 비밀 추측
```

**특히 위험한 상황:**

- 공격자가 JWT에 포함될 데이터의 일부를 제어할 수 있는 경우
- 동일한 압축 컨텍스트에서 비밀 데이터와 공격자 데이터가 처리되는 경우

---

### 2.5 안전하지 않은 타원 곡선 암호화

`ECDH-ES` 알고리즘을 사용하는 구현에서 **유효하지 않은 곡선 점(Invalid Curve Point)** 을 허용하면, 수신자의 개인키가 노출될 수 있습니다.

**Invalid Curve Attack:**

```
정상적인 ECDH-ES:

발신자 → 임시 공개키(epk) → 수신자
       ← 수신자의 공개키 ←

       공유 비밀 계산 → 암호화 키 유도

공격 시나리오:

공격자 → 잘못된 곡선 위의 점(invalid epk) → 수신자
                                              ↓
                              수신자가 검증 없이 연산 수행
                                              ↓
                              개인키 정보가 부분적으로 유출
                                              ↓
                              여러 번의 공격으로 개인키 복구
```

---

### 2.6 JSON 인코딩의 다양성

이전 JSON 표준에서는 UTF-8, UTF-16, UTF-32 인코딩을 모두 허용했습니다. 현대 표준(RFC 8259)은 **UTF-8만 사용** 하도록 규정하지만, 레거시 구현과의 **해석 불일치** 가 발생할 수 있습니다.

**잠재적 문제:**

| 인코딩 | 지원 여부 | 문제점 |
|--------|-----------|--------|
| UTF-8 | RFC 8259 필수 | 표준 |
| UTF-16 | 레거시 지원 | 해석 불일치 가능 |
| UTF-32 | 레거시 지원 | 해석 불일치 가능 |

```
인코딩 해석 차이 예시:

발신자 (UTF-16):  "admin\u0000user"
수신자 (UTF-8):   "admin" (null 이후 무시)

→ 권한 우회 공격 가능
```

---

### 2.7 대체 공격 (Substitution Attacks)

한 수신자를 위해 발행된 JWT가 **다른 수신자에게 제출** 되어 오용될 수 있습니다.

**공격 시나리오:**

```
OAuth 2.0 환경에서의 대체 공격:

1. 사용자가 Resource Server A에서 액세스 토큰 획득
2. 공격자가 이 토큰을 Resource Server B에 제출
3. Resource Server B가 토큰을 유효한 것으로 수락

┌─────────────┐     정상 토큰     ┌───────────────────┐
│  사용자      │ ───────────────→ │ Resource Server A │ ✓
└─────────────┘                   └───────────────────┘
       │
       │ 토큰 탈취
       ↓
┌─────────────┐   동일한 토큰    ┌───────────────────┐
│   공격자     │ ───────────────→ │ Resource Server B │ ✗ 허용되면 안됨
└─────────────┘                   └───────────────────┘
```

---

### 2.8 JWT 간 혼동 (Cross-JWT Confusion)

서로 다른 프로토콜이나 용도로 발행된 JWT가 **다른 컨텍스트에서 오용** 될 수 있습니다.

**예시:**

```
JWT 용도별 구분 실패:

┌────────────────────────────────────────┐
│ ID Token (OpenID Connect)              │
│ - 사용자 인증용                         │
│ - sub: "user123"                       │
│ - aud: "client_app"                    │
└────────────────────────────────────────┘
                    ↓ 혼동
┌────────────────────────────────────────┐
│ Access Token (OAuth 2.0)               │
│ - 리소스 접근용                         │
│ - sub: "user123"                       │
│ - aud: "resource_server"               │
└────────────────────────────────────────┘

→ ID Token이 Access Token으로 오용되거나 그 반대의 경우
```

---

### 2.9 서버에 대한 간접 공격

JWT 클레임이 **데이터베이스 쿼리, LDAP 검색** 등에 직접 사용되면 주입 공격(Injection Attack)이 가능합니다.

**공격 유형:**

| 공격 | 설명 | 예시 |
|------|------|------|
| **SQL Injection** | 클레임 값이 SQL 쿼리에 삽입 | `"sub": "'; DROP TABLE users;--"` |
| **LDAP Injection** | 클레임 값이 LDAP 필터에 삽입 | `"sub": "*)(uid=*))(|(uid=*"` |
| **SSRF** | `jku`/`x5u` URL을 통한 서버측 요청 위조 | `"jku": "http://internal-server/"` |

```
SSRF 공격 시나리오:

공격자가 조작한 JWT:
{
  "alg": "RS256",
  "jku": "http://internal-server:8080/keys"  ← 내부 서버 접근
}

서버가 jku URL을 맹목적으로 따라가면:
1. 내부 네트워크 스캔
2. 내부 서비스 정보 유출
3. 내부 시스템 공격
```

---

## 모범 사례 (Best Practices)

### 3.1 알고리즘 검증 수행

**요구사항:**

```
라이브러리는 호출자가 지원하는 알고리즘 집합을 지정할 수 있도록
해야 하며(MUST), 암호화 작업 수행 시 다른 알고리즘을 사용해서는
안 됩니다(MUST NOT).
```

**구현 가이드:**

```javascript
// ❌ 잘못된 구현: JWT 헤더의 alg 값을 그대로 신뢰
function verifyJWT(token) {
    const header = decodeHeader(token);
    const algorithm = header.alg;  // 공격자가 조작 가능
    return verify(token, algorithm);
}

// ✅ 올바른 구현: 허용된 알고리즘 목록으로 제한
function verifyJWT(token, allowedAlgorithms = ['RS256', 'ES256']) {
    const header = decodeHeader(token);
    const algorithm = header.alg;

    if (!allowedAlgorithms.includes(algorithm)) {
        throw new Error('Algorithm not allowed');
    }

    return verify(token, algorithm);
}
```

**키와 알고리즘 매칭:**

```
각 키는 정확히 하나의 알고리즘과 연결되어야 합니다:

키 ID: "key-001"  →  알고리즘: RS256
키 ID: "key-002"  →  알고리즘: ES256
키 ID: "key-003"  →  알고리즘: HS256

→ 공격자가 알고리즘을 변경해도 키-알고리즘 불일치로 거부됨
```

---

### 3.2 적절한 알고리즘 사용

**핵심 원칙:**

애플리케이션은 **암호학적으로 현재(current)인 알고리즘만** 허용해야 하며, 애플리케이션의 보안 요구사항을 충족해야 합니다.

**알고리즘별 권장사항:**

| 알고리즘 | 상태 | 권장사항 |
|----------|------|----------|
| `none` | 조건부 허용 | TLS 등 다른 수단으로 보호되는 경우에만 사용 |
| `HS256`, `HS384`, `HS512` | 권장 | 충분한 엔트로피의 키 사용 필수 |
| `RS256`, `RS384`, `RS512` | 권장 | 최소 2048비트 키 사용 |
| `ES256`, `ES384`, `ES512` | 권장 | RFC 6979에 따른 결정적 서명 구현 권장 |
| `RSA1_5` (RSAES-PKCS1-v1_5) | **비권장** | RSAES-OAEP 사용 권장 |

**`none` 알고리즘 사용 조건:**

```
"none" 알고리즘은 JWT가 다른 수단으로 암호학적 보호를 받는
경우에만 허용됩니다.

허용 가능한 시나리오:
┌────────────────────────────────────────┐
│            TLS 1.3 연결               │
│  ┌────────────────────────────────┐   │
│  │    JWT (alg: "none")           │   │
│  │    - 서명 없음                  │   │
│  │    - TLS가 무결성/기밀성 보장   │   │
│  └────────────────────────────────┘   │
└────────────────────────────────────────┘

허용되지 않는 시나리오:
- HTTP 평문 통신
- 신뢰할 수 없는 중간자 존재
- 저장 후 나중에 검증하는 경우
```

**ECDSA 결정적 서명:**

```
ECDSA 서명에서 난수(k) 생성 문제:

비결정적 ECDSA:
- 서명마다 새로운 난수 k 필요
- 난수 생성기 품질이 낮으면 개인키 유출 가능
- 동일 메시지에 동일 k 사용 시 개인키 복구 가능

결정적 ECDSA (RFC 6979):
- 메시지와 개인키에서 k를 결정적으로 유도
- 난수 생성기 품질에 의존하지 않음
- 동일 메시지에는 항상 동일 서명 생성
```

---

### 3.3 모든 암호화 작업 검증

**요구사항:**

```
JWT에 사용된 모든 암호화 작업은 검증되어야 하며(MUST),
검증에 실패하면 전체 JWT가 거부되어야 합니다(MUST).
중첩된 JWT의 경우에도 내부 JWT의 모든 암호화 작업이 검증되어야 합니다.
```

**검증 체크리스트:**

```
JWT 검증 시 확인해야 할 항목:

□ 서명 알고리즘이 허용 목록에 있는가?
□ 서명이 유효한가?
□ 서명에 사용된 키가 발행자의 것인가?
□ 토큰이 만료되지 않았는가? (exp)
□ 토큰이 아직 유효하지 않은 것은 아닌가? (nbf)
□ 발행자가 신뢰할 수 있는가? (iss)
□ 대상이 올바른가? (aud)
□ 중첩된 JWT가 있다면 내부도 모두 검증했는가?
```

---

### 3.4 암호화 입력 검증

**요구사항:**

```
라이브러리는 암호화 입력을 사용하기 전에 검증해야 합니다(MUST).
특히 타원 곡선 연산에서 이 검증이 중요합니다.
```

**타원 곡선 점 검증:**

```
ECDH-ES에서 임시 공개키(epk) 검증:

1. 점이 무한점이 아닌지 확인
2. 점의 좌표가 올바른 범위 내에 있는지 확인
3. 점이 지정된 곡선 위에 있는지 확인
4. 점의 차수(order)가 올바른지 확인

검증 실패 시:
→ 전체 JWT 거부
→ Invalid Curve Attack 방지
```

**NIST 가이드라인 참조:**

- SP 800-56A: 키 합의 체계에 대한 권장사항
- 타원 곡선 Diffie-Hellman 구현 요구사항

---

### 3.5 암호화 키의 충분한 엔트로피 보장

**요구사항:**

```
인간이 기억할 수 있는 비밀번호는 HS256과 같은 keyed-MAC
알고리즘의 키로 직접 사용해서는 안 됩니다(MUST NOT).
```

**키 사용 구분:**

| 용도 | 비밀번호 직접 사용 | 권장 방식 |
|------|-------------------|-----------|
| 콘텐츠 암호화 | **금지** | 충분한 엔트로피의 랜덤 키 |
| 키 암호화 | 허용 (PBES2 등) | 비밀번호 기반 키 유도 함수 사용 |
| MAC 서명 | **금지** | 최소 256비트 랜덤 키 |

**안전한 키 생성:**

```python
# ❌ 잘못된 방식: 비밀번호 직접 사용
secret_key = "mysecretpassword"

# ❌ 잘못된 방식: 짧거나 예측 가능한 키
secret_key = "123456"

# ✅ 올바른 방식: 충분한 엔트로피의 랜덤 키
import secrets
secret_key = secrets.token_bytes(32)  # 256비트

# ✅ 비밀번호 기반이 필요한 경우: PBKDF2 등 사용
import hashlib
derived_key = hashlib.pbkdf2_hmac(
    'sha256',
    password.encode(),
    salt,
    iterations=100000,
    dklen=32
)
```

---

### 3.6 암호화 전 압축 회피

**권장사항:**

```
암호화 전에 데이터를 압축해서는 안 됩니다(SHOULD NOT).
압축된 데이터는 종종 평문에 대한 정보를 노출합니다.
```

**JWE 압축 옵션:**

```
JWE 헤더의 "zip" 매개변수:

{
  "alg": "RSA-OAEP",
  "enc": "A256GCM",
  "zip": "DEF"        ← DEFLATE 압축 사용 (위험)
}

권장 설정:
{
  "alg": "RSA-OAEP",
  "enc": "A256GCM"
  // "zip" 매개변수 생략 → 압축 없음
}
```

**압축 사용이 특히 위험한 경우:**

- 공격자가 JWT 페이로드의 일부를 제어할 수 있을 때
- 비밀 데이터와 공격자 데이터가 같은 JWT에 있을 때
- 암호문 길이를 관찰할 수 있을 때

---

### 3.7 UTF-8 사용

**요구사항:**

```
모든 명세는 헤더 매개변수와 JWT 클레임 세트의 JSON 인코딩 및
디코딩에 UTF-8을 사용하도록 지정해야 합니다.
구현체는 이를 준수해야 하며(MUST), 다른 유니코드 인코딩을
사용하거나 허용해서는 안 됩니다(MUST NOT).
```

**인코딩 검증:**

```python
# JWT 처리 시 UTF-8 검증
def validate_encoding(data):
    try:
        # 반드시 UTF-8로 디코딩
        decoded = data.decode('utf-8')

        # BOM 확인 및 제거
        if decoded.startswith('\ufeff'):
            raise ValueError('BOM not allowed')

        return decoded
    except UnicodeDecodeError:
        raise ValueError('Invalid UTF-8 encoding')
```

---

### 3.8 발행자 및 주체 검증

**요구사항:**

```
애플리케이션은 JWT의 암호화 작업에 사용된 암호화 키가
발행자(issuer)에 속하는지 검증해야 합니다(MUST).
마찬가지로 주체(subject) 값이 유효한 애플리케이션 주체인지
검증해야 합니다.
```

**발행자 검증 프로세스:**

```
발행자-키 바인딩 검증:

1. JWT의 "iss" 클레임 확인
2. 해당 발행자에 대해 신뢰하는 키 집합 조회
3. JWT 서명이 해당 키 집합의 키로 검증되는지 확인

예시:
iss: "https://auth.example.com"
     ↓
신뢰하는 키: auth.example.com의 JWKS에서 가져온 키들
     ↓
서명 검증: 해당 키로만 검증 수행
```

**주체 검증:**

```python
def validate_subject(claims, allowed_subjects):
    subject = claims.get('sub')

    if subject is None:
        raise ValueError('Subject claim required')

    # 주체가 애플리케이션에서 유효한지 확인
    if not is_valid_subject(subject, allowed_subjects):
        raise ValueError('Invalid subject')

    # SQL Injection 등 방지를 위한 정제
    if contains_dangerous_characters(subject):
        raise ValueError('Subject contains dangerous characters')

    return subject
```

---

### 3.9 대상(Audience) 사용 및 검증

**요구사항:**

```
발행자가 여러 수신자를 위한 JWT를 생성할 때, 토큰에는 반드시
"aud" (audience) 클레임이 포함되어야 합니다(MUST).
```

**대체 공격 방지:**

```
aud 클레임을 통한 수신자 제한:

JWT 발행:
{
  "iss": "https://auth.example.com",
  "sub": "user123",
  "aud": "https://api-a.example.com",  ← 특정 수신자 지정
  "exp": 1735689600
}

검증 (API Server A):
aud == "https://api-a.example.com" ? ✓ 허용

검증 (API Server B):
aud == "https://api-a.example.com" ? ✗ 거부
→ 대체 공격 방지됨
```

**다중 Audience:**

```json
{
  "aud": ["https://api-a.example.com", "https://api-b.example.com"]
}
```

```
다중 aud 검증:
- 수신자는 자신의 식별자가 aud 배열에 포함되어 있는지 확인
- 모든 aud 값을 만족할 필요 없음, 자신만 포함되어 있으면 됨
```

---

### 3.10 수신된 클레임을 신뢰하지 않음

**요구사항:**

```
애플리케이션은 수신된 클레임 값을 정제(sanitize)해야 합니다.
"kid" 헤더 값은 SQL/LDAP 주입을 방지하도록 정제해야 합니다.
"jku"나 "x5u" URL을 맹목적으로 따라가면 SSRF 공격 위험이 있습니다.
```

**kid 클레임 정제:**

```python
def get_key_by_kid(kid):
    # ❌ 위험: SQL 주입 가능
    query = f"SELECT key FROM keys WHERE kid = '{kid}'"

    # ✅ 안전: 매개변수화된 쿼리 사용
    query = "SELECT key FROM keys WHERE kid = ?"
    cursor.execute(query, (kid,))

    # ✅ 추가 검증: 허용된 문자만 포함하는지 확인
    if not re.match(r'^[a-zA-Z0-9_-]+$', kid):
        raise ValueError('Invalid kid format')
```

**jku/x5u URL 검증:**

```python
def fetch_jwks(jku_url):
    # ✅ 허용된 도메인 화이트리스트
    allowed_domains = [
        'auth.example.com',
        'keys.example.com'
    ]

    parsed = urlparse(jku_url)

    # 스키마 검증 (HTTPS만 허용)
    if parsed.scheme != 'https':
        raise ValueError('HTTPS required')

    # 도메인 화이트리스트 검증
    if parsed.netloc not in allowed_domains:
        raise ValueError('Domain not allowed')

    # 내부 IP 차단
    if is_internal_ip(parsed.hostname):
        raise ValueError('Internal IPs not allowed')

    return fetch(jku_url)
```

---

### 3.11 명시적 타이핑 사용

**권장사항:**

```
"typ" 헤더 매개변수를 사용하여 JWT 유형을 구분합니다.
"application/" 접두사는 생략하는 것이 권장됩니다(RECOMMENDED).
```

**타입 구분 예시:**

```
서로 다른 용도의 JWT에 typ 사용:

ID Token:
{
  "alg": "RS256",
  "typ": "JWT"           ← 일반 JWT
}

Security Event Token:
{
  "alg": "RS256",
  "typ": "secevent+jwt"  ← 보안 이벤트 토큰
}

DPoP Proof:
{
  "alg": "ES256",
  "typ": "dpop+jwt"      ← DPoP 증명 토큰
}
```

**타입 검증:**

```python
def validate_jwt_type(header, expected_type):
    typ = header.get('typ')

    if typ is None:
        raise ValueError('typ header required')

    # 대소문자 무시 비교 (RFC 7231)
    if typ.lower() != expected_type.lower():
        raise ValueError(f'Expected type {expected_type}, got {typ}')
```

---

### 3.12 상호 배타적 검증 규칙 사용

**요구사항:**

```
여러 종류의 JWT가 존재할 때, 검증 규칙은 상호 배타적이어야 하며(MUST),
잘못된 종류의 JWT는 거부해야 합니다.
```

**JWT 유형 구분 전략:**

| 전략 | 설명 | 예시 |
|------|------|------|
| **다른 typ 값** | 헤더의 typ으로 구분 | `"typ": "at+jwt"` vs `"typ": "id_token"` |
| **다른 필수 클레임** | 특정 클레임 유무로 구분 | ID Token은 `nonce` 필수 |
| **다른 필수 헤더** | 헤더 매개변수로 구분 | DPoP는 `htm`, `htu` 필수 |
| **다른 키** | 용도별 다른 키 사용 | 서명용 키와 암호화용 키 분리 |
| **다른 aud 값** | 대상 클레임으로 구분 | 서비스별 다른 audience |
| **다른 발행자** | iss 클레임으로 구분 | 용도별 다른 발행자 |

**구현 예시:**

```python
def validate_token(token, expected_type):
    header = decode_header(token)
    claims = decode_claims(token)

    # 1. typ 검증
    if header.get('typ') != expected_type:
        raise ValueError('Invalid token type')

    # 2. 필수 클레임 검증 (타입별)
    required_claims = get_required_claims(expected_type)
    for claim in required_claims:
        if claim not in claims:
            raise ValueError(f'Missing required claim: {claim}')

    # 3. 발행자 검증 (타입별)
    allowed_issuers = get_allowed_issuers(expected_type)
    if claims.get('iss') not in allowed_issuers:
        raise ValueError('Invalid issuer for token type')

    # 4. Audience 검증
    if expected_type == 'access_token':
        if claims.get('aud') != 'https://api.example.com':
            raise ValueError('Invalid audience')

    return claims
```

**상호 배타성 보장:**

```
ID Token과 Access Token의 상호 배타적 검증:

ID Token 검증 규칙:
- typ: "id_token" 또는 없음 (OpenID Connect)
- 필수 클레임: iss, sub, aud, exp, iat, nonce
- aud: 클라이언트 ID
- 발행자: ID Provider

Access Token 검증 규칙:
- typ: "at+jwt"
- 필수 클레임: iss, sub, aud, exp, iat, scope
- aud: Resource Server
- 발행자: Authorization Server

→ 동일 토큰이 두 검증을 모두 통과할 수 없음
```

---

## 보안 고려사항 (Security Considerations)

```
이 문서 전체가 JSON 웹 토큰 구현 및 배포 시의 보안 고려사항에 관한 것입니다.
```

### 보안 체크리스트

**라이브러리 구현자용:**

```
□ 알고리즘 화이트리스트 지원
□ "none" 알고리즘 기본 비활성화
□ 충분한 키 엔트로피 검증
□ 타원 곡선 점 검증
□ UTF-8 인코딩 강제
□ 중첩 JWT 완전 검증
□ 결정적 ECDSA 지원 (권장)
```

**애플리케이션 개발자용:**

```
□ 허용 알고리즘 명시적 지정
□ 발행자(iss) 검증
□ 대상(aud) 검증
□ 만료(exp) 검증
□ 클레임 값 정제
□ jku/x5u URL 화이트리스트
□ 적절한 키 관리
□ 압축 비활성화
```

---

## IANA 고려사항 (IANA Considerations)

```
이 문서에는 IANA 관련 작업이 없습니다.
```

---

## 요약

### 핵심 모범 사례

| 번호 | 모범 사례 | 중요도 |
|------|----------|--------|
| 3.1 | 알고리즘 검증 수행 | **필수** |
| 3.2 | 적절한 알고리즘 사용 | **필수** |
| 3.3 | 모든 암호화 작업 검증 | **필수** |
| 3.4 | 암호화 입력 검증 | **필수** |
| 3.5 | 충분한 키 엔트로피 | **필수** |
| 3.6 | 압축 회피 | 권장 |
| 3.7 | UTF-8 사용 | **필수** |
| 3.8 | 발행자/주체 검증 | **필수** |
| 3.9 | Audience 사용 및 검증 | **필수** |
| 3.10 | 클레임 정제 | **필수** |
| 3.11 | 명시적 타이핑 | 권장 |
| 3.12 | 상호 배타적 검증 | **필수** |

### 위협 대응 매핑

| 위협 | 대응 모범 사례 |
|------|---------------|
| alg 헤더 조작 | 3.1, 3.2 |
| 약한 키 | 3.5 |
| 중첩 JWT 취약점 | 3.3 |
| 압축 기반 공격 | 3.6 |
| Invalid Curve Attack | 3.4 |
| 인코딩 불일치 | 3.7 |
| 대체 공격 | 3.8, 3.9 |
| JWT 혼동 | 3.11, 3.12 |
| 주입 공격 | 3.10 |

---

## 참고 자료

- [RFC 8725 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc8725)
- [RFC 7519 - JSON Web Token (JWT)](https://datatracker.ietf.org/doc/html/rfc7519)
- [RFC 7515 - JSON Web Signature (JWS)](https://datatracker.ietf.org/doc/html/rfc7515)
- [RFC 7516 - JSON Web Encryption (JWE)](https://datatracker.ietf.org/doc/html/rfc7516)
- [RFC 7517 - JSON Web Key (JWK)](https://datatracker.ietf.org/doc/html/rfc7517)
- [RFC 7518 - JSON Web Algorithms (JWA)](https://datatracker.ietf.org/doc/html/rfc7518)
- [RFC 6979 - Deterministic DSA and ECDSA](https://datatracker.ietf.org/doc/html/rfc6979)
- [RFC 8259 - JSON (UTF-8)](https://datatracker.ietf.org/doc/html/rfc8259)

---

## 관련 RFC

- **RFC 7519**: JSON Web Token (JWT) - JWT 기본 명세
- **RFC 7515**: JSON Web Signature (JWS) - 서명 명세
- **RFC 7516**: JSON Web Encryption (JWE) - 암호화 명세
- **RFC 7517**: JSON Web Key (JWK) - 키 표현 명세
- **RFC 7518**: JSON Web Algorithms (JWA) - 알고리즘 명세
- **RFC 2119**: RFC 키워드 정의

---

*이 문서는 RFC 8725의 한국어 번역 및 정리본입니다.*
