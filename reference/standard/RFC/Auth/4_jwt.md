# JWT와 JWT 모범 사례

```
Internet Engineering Task Force (IETF)                          M. Jones
Request for Comments: 7519                                     Microsoft
Category: Standards Track                                     J. Bradley
ISSN: 2070-1721                                            Ping Identity
                                                             N. Sakimura
                                                                     NRI
                                                                May 2015
```

## JSON Web Token (JWT)

#### Abstract

- JWT: 두 당사자 간 전송되는 클레임 표현을 위한 컴팩트·URL-safe 수단
- 클레임은 JSON Web Signature(JWS) 구조의 페이로드 또는 JSON Web Encryption(JWE) 구조의 평문으로 사용되는 JSON 객체로 인코딩 → 디지털 서명·MAC 무결성 보호·암호화 가능

### 이 메모의 상태

- Internet Standards Track 문서, IETF(Internet Engineering Task Force) 산출물
- IETF 커뮤니티 합의 반영, 공개 검토 완료 → IESG 발행 승인
- Internet Standards 관련 추가 정보: RFC 5741 섹션 2 참고
- 현재 상태·정오표·피드백 제공 방법: http://www.rfc-editor.org/info/rfc7519

### 저작권 고지

- Copyright (c) 2015 IETF Trust and the persons identified as the document authors. All rights reserved.
- 발행일 기준 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust 법적 조항(http://trustee.ietf.org/license-info) 적용 대상
- 문서에서 추출된 코드 구성요소: Trust Legal Provisions 섹션 4.e에 설명된 Simplified BSD License 텍스트 포함 필요, 보증 없이 제공

### 목차

```
1.  서론
    1.1.  표기 규약
2.  용어
3.  JSON Web Token (JWT) 개요
    3.1.  JWT 예시
4.  JWT 클레임
    4.1.  등록된 클레임 이름
        4.1.1.  "iss" (Issuer) 클레임
        4.1.2.  "sub" (Subject) 클레임
        4.1.3.  "aud" (Audience) 클레임
        4.1.4.  "exp" (Expiration Time) 클레임
        4.1.5.  "nbf" (Not Before) 클레임
        4.1.6.  "iat" (Issued At) 클레임
        4.1.7.  "jti" (JWT ID) 클레임
    4.2.  공개 클레임 이름
    4.3.  비공개 클레임 이름
5.  JOSE 헤더
    5.1.  "typ" (Type) 헤더 파라미터
    5.2.  "cty" (Content Type) 헤더 파라미터
    5.3.  헤더 파라미터로서의 클레임 복제
6.  비보안 JWT
    6.1.  비보안 JWT 예시
7.  JWT 생성 및 검증
    7.1.  JWT 생성
    7.2.  JWT 검증
    7.3.  문자열 비교 규칙
8.  구현 요구사항
9.  콘텐츠가 JWT임을 선언하기 위한 URI
10. IANA 고려사항
    10.1. JSON Web Token Claims 레지스트리
    10.2. urn:ietf:params:oauth:token-type:jwt 서브 네임스페이스 등록
    10.3. 미디어 타입 등록
    10.4. 헤더 파라미터 이름 등록
11. 보안 고려사항
    11.1. 신뢰 결정
    11.2. 서명 및 암호화 순서
12. 프라이버시 고려사항
13. 참조
부록 A.  JWT 예시
부록 B.  JWT와 SAML Assertion의 관계
부록 C.  JWT와 Simple Web Token (SWT)의 관계
```

### 1. 서론

- JWT: HTTP Authorization 헤더·URI 쿼리 파라미터처럼 공간 제약이 있는 환경을 위한 컴팩트 클레임 표현 형식
- JWS [JWS] 구조의 페이로드 또는 JWE [JWE] 구조의 평문으로 사용되는 JSON [RFC7159] 객체로 클레임 인코딩 → 디지털 서명·MAC 무결성 보호·암호화 가능
- 항상 JWS Compact Serialization 또는 JWE Compact Serialization으로 표현
- 발음은 영어 단어 "jot"과 동일

#### 1.1. 표기 규약

- "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL" 키워드는 RFC 2119에 설명된 대로 해석
- 해당 해석은 키워드가 모두 대문자로 표시될 때만 적용

### 2. 용어

JSON Web Signature (JWS) [JWS] 명세에 의해 정의:

- JSON Web Signature (JWS)
- Base64url Encoding
- Header Parameter
- JOSE Header
- JWS Compact Serialization
- JWS Payload
- JWS Signature
- Unsecured JWS

JSON Web Encryption (JWE) [JWE] 명세에 의해 정의:

- JSON Web Encryption (JWE)
- Content Encryption Key (CEK)
- JWE Compact Serialization
- JWE Encrypted Key
- JWE Initialization Vector

"Internet Security Glossary, Version 2" [RFC4949]에 의해 정의:

- Ciphertext
- Digital Signature
- Message Authentication Code (MAC)
- Plaintext

이 명세에 의해 정의:

- JSON Web Token (JWT): JWS 또는 JWE로 인코딩되어 클레임이 디지털 서명·MAC 처리·암호화될 수 있는 JSON 객체로서 클레임 집합을 나타내는 문자열
- JWT Claims Set: JWT가 전달하는 클레임을 포함하는 JSON 객체
- Claim: 주체에 대해 주장되는 정보 조각. 클레임 이름과 클레임 값으로 구성된 이름/값 쌍
- Claim Name: 클레임 표현의 이름 부분, 항상 문자열
- Claim Value: 클레임 표현의 값 부분, 어떤 JSON 값이든 가능
- Nested JWT: 중첩 서명 및/또는 암호화가 적용된 JWT. JWT가 둘러싸는 JWS 또는 JWE 구조의 페이로드/평문 값으로 사용됨
- Unsecured JWT: 클레임이 무결성 보호되거나 암호화되지 않은 JWT
- Collision-Resistant Name: 다른 이름들과 충돌할 가능성이 매우 낮게 이름을 할당할 수 있는 네임스페이스 내 이름
- StringOrURI: 임의의 문자열 값 사용 가능, 단 ':' 문자를 포함하는 값은 URI [RFC3986]여야 하는 JSON 문자열 값
- NumericDate: 지정 UTC 날짜/시간까지 1970-01-01T00:00:00Z UTC로부터의 초 수를 나타내는 JSON 숫자 값(윤초 무시)
  - IEEE Std 1003.1, 2013 Edition [POSIX.1] 정의와 동등, 각 날짜는 정확히 86400초로 표현
  - 정수·비정수 숫자 값 모두 표현 가능
  - 구현 참고사항: RFC 3339 [RFC3339] 섹션 2 참고

### 3. JSON Web Token (JWT) 개요

- JWT: JWS 및/또는 JWE 구조로 인코딩된 JSON 객체로서 클레임 집합을 나타냄 → 이 JSON 객체가 JWT Claims Set
- RFC 7159 [RFC7159] 섹션 4에 따라, JSON 객체는 0개 이상의 이름/값 쌍(멤버)으로 구성 → 이름은 문자열, 값은 임의의 JSON 값 → 이 멤버들이 JWT가 나타내는 클레임
- JSON 값·구조 문자 앞뒤에 공백 및/또는 줄 바꿈 포함 가능(RFC 7159 섹션 2)
- JWT Claims Set 내 멤버 이름 = 클레임 이름, 해당 값 = 클레임 값
- JOSE 헤더 내용은 JWT Claims Set에 적용되는 암호화 연산을 기술
  - JOSE 헤더가 JWS용 → JWT는 JWS로 표현, 클레임은 디지털 서명·MAC 처리, JWT Claims Set이 JWS Payload
  - JOSE 헤더가 JWE용 → JWT는 JWE로 표현, 클레임은 암호화, JWT Claims Set이 JWE에 의해 암호화되는 평문
  - JWT는 Nested JWT 생성을 위해 다른 JWE/JWS 구조 안에 둘러싸일 수 있음 → 중첩 서명 및 암호화 수행 가능
- JWT는 마침표('.') 문자로 구분된 URL-safe 파트들의 시퀀스로 표현 → 각 파트는 base64url 인코딩된 값 포함
- JWT의 파트 수는 JWS/JWE Compact Serialization 사용 결과 표현에 의존

#### 3.1. JWT 예시

다음 JOSE 헤더 예시는 인코딩된 객체가 JWT임을 선언, HMAC SHA-256 알고리즘으로 MAC 처리된 JWS:

```
{"typ":"JWT",
 "alg":"HS256"}
```

- 표현 모호성 제거를 위해 UTF-8 표현의 옥텟 시퀀스도 아래에 포함
  - 모호성 발생 원인: 줄 바꿈의 플랫폼 간 상이한 표현(CRLF 대 LF), 줄의 시작·끝 간격 차이, 마지막 줄 종료 줄 바꿈 여부 등
  - 이 예시의 표현: 첫 줄은 선행/후행 공백 없음, CRLF 줄 바꿈(13, 10)이 첫째-둘째 줄 사이에 위치, 둘째 줄은 선행 공백 1개(32)·후행 공백 없음, 마지막 줄에 종료 줄 바꿈 없음
- JOSE 헤더의 UTF-8 표현 옥텟(JSON 배열 표기법):

```
[123, 34, 116, 121, 112, 34, 58, 34, 74, 87, 84, 34, 44, 13, 10, 32,
34, 97, 108, 103, 34, 58, 34, 72, 83, 50, 53, 54, 34, 125]
```

JOSE 헤더의 UTF-8 표현 옥텟을 Base64url 인코딩하면 다음의 인코딩된 JOSE 헤더 값 생성:

```
eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9
```

JWT Claims Set 예시:

```
{"iss":"joe",
 "exp":1300819380,
 "http://example.com/is_root":true}
```

위 JWT Claims Set의 UTF-8 표현 옥텟 시퀀스 = JWS Payload:

```
[123, 34, 105, 115, 115, 34, 58, 34, 106, 111, 101, 34, 44, 13, 10,
32, 34, 101, 120, 112, 34, 58, 49, 51, 48, 48, 56, 49, 57, 51, 56,
48, 44, 13, 10, 32, 34, 104, 116, 116, 112, 58, 47, 47, 101, 120, 97,
109, 112, 108, 101, 46, 99, 111, 109, 47, 105, 115, 95, 114, 111, 111,
116, 34, 58, 116, 114, 117, 101, 125]
```

JWS Payload를 Base64url 인코딩하면 다음의 인코딩된 JWS Payload 생성(표시 목적으로만 줄 바꿈 포함):

```
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
```

인코딩된 JOSE 헤더와 인코딩된 JWS Payload의 MAC을 HMAC SHA-256 알고리즘으로 계산 → base64url 인코딩하면 다음의 인코딩된 JWS Signature 생성:

```
dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

인코딩된 파트들을 순서대로 마침표('.') 문자로 연결하면 완전한 JWT 생성(표시 목적으로만 줄 바꿈 포함):

```
eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9
.
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
.
dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

- 계산 상세: [JWS] 부록 A.1 참고
- 암호화된 JWT 예시: 부록 A.1 참고

### 4. JWT 클레임

- JWT Claims Set: 멤버가 JWT가 전달하는 클레임인 JSON 객체
- JWT Claims Set 내 클레임 이름은 고유해야 함(MUST)
- JWT 파서는 중복 클레임 이름을 가진 JWT를 거부하거나, ECMAScript 5.1 [ECMAScript] 섹션 15.12에 지정된 대로 마지막 중복 멤버 이름만 반환하는 JSON 파서를 사용해야 함(MUST)
- JWT가 유효하려면 반드시 포함해야 하는 클레임 집합은 컨텍스트에 의존 → 이 명세의 범위 밖
- JWT의 특정 응용프로그램은 일부 클레임을 특정 방식으로 이해·처리하도록 요구 가능 → 그런 요구사항이 없으면, 이해하지 못하는 클레임은 무시해야 함(MUST)
- JWT 클레임 이름 3부류: 등록된 클레임 이름 · 공개 클레임 이름 · 비공개 클레임 이름

#### 4.1. 등록된 클레임 이름

- 섹션 10.1에 의해 설정된 IANA "JSON Web Token Claims" 레지스트리에 등록된 클레임 이름들
- 아래 정의된 클레임 중 모든 경우에 사용·구현이 필수인 것은 없음 → 유용하고 상호 운용 가능한 클레임 집합의 출발점 제공
- JWT를 사용하는 응용프로그램은 어떤 클레임을 사용할지, 필수인지 선택인지 정의해야 함
- 모든 이름은 JWT의 핵심 목표(컴팩트한 표현) 때문에 짧음

##### 4.1.1. "iss" (Issuer) 클레임

- JWT를 발급한 주체 식별
- 처리는 일반적으로 응용프로그램에 따라 다름
- 값은 StringOrURI 값을 포함하는 대소문자 구분 문자열
- 사용은 선택적(OPTIONAL)

##### 4.1.2. "sub" (Subject) 클레임

- JWT의 주체인 주체 식별 → JWT의 클레임은 일반적으로 주체에 대한 진술
- 주체 값은 발급자 컨텍스트에서 로컬하게 고유하도록 범위 지정되거나, 전역적으로 고유해야 함(MUST)
- 처리는 일반적으로 응용프로그램에 따라 다름
- 값은 StringOrURI 값을 포함하는 대소문자 구분 문자열
- 사용은 선택적(OPTIONAL)

##### 4.1.3. "aud" (Audience) 클레임

- JWT가 의도하는 수신자 식별
- JWT를 처리하려는 각 주체는 audience 클레임 값으로 자신을 식별해야 함(MUST)
- 클레임이 존재할 때, 처리 주체가 "aud" 값으로 자신을 식별하지 않으면 JWT는 거부되어야 함(MUST)
- 일반적으로 "aud" 값은 StringOrURI 값을 포함하는 대소문자 구분 문자열의 배열
- audience가 하나인 특수한 경우, "aud" 값은 StringOrURI 값을 포함하는 단일 문자열일 수 있음(MAY)
- audience 값의 해석은 일반적으로 응용프로그램에 따라 다름
- 사용은 선택적(OPTIONAL)

##### 4.1.4. "exp" (Expiration Time) 클레임

- JWT가 처리를 위해 수락되어서는 안 되는 만료 시간 이후를 식별
- 처리 시 현재 날짜/시간이 "exp" 클레임의 만료 날짜/시간 이전이어야 함(MUST)
- 구현자는 시계 오차 고려를 위해 몇 분 이내의 여유를 둘 수 있음(MAY)
- 값은 NumericDate여야 함(MUST)
- 사용은 선택적(OPTIONAL)

##### 4.1.5. "nbf" (Not Before) 클레임

- JWT가 처리를 위해 수락되어서는 안 되는 이전 시간을 식별
- 처리 시 현재 날짜/시간이 "nbf" 클레임의 not-before 날짜/시간 이후이거나 같아야 함(MUST)
- 구현자는 시계 오차 고려를 위해 몇 분 이내의 여유를 둘 수 있음(MAY)
- 값은 NumericDate여야 함(MUST)
- 사용은 선택적(OPTIONAL)

##### 4.1.6. "iat" (Issued At) 클레임

- JWT가 발급된 시간을 식별 → JWT의 연령 판단에 사용 가능
- 값은 NumericDate여야 함(MUST)
- 사용은 선택적(OPTIONAL)

##### 4.1.7. "jti" (JWT ID) 클레임

- JWT에 대한 고유 식별자 제공
- 동일한 값이 우연히 다른 데이터 객체에 할당될 확률이 무시할 수 있을 정도로 낮게 할당되어야 함(MUST)
- 여러 발급자를 사용하는 응용프로그램은 발급자 간 값 충돌도 방지해야 함(MUST)
- JWT 재전송 방지 용도로 사용 가능
- 값은 대소문자 구분 문자열
- 사용은 선택적(OPTIONAL)

#### 4.2. 공개 클레임 이름

- 클레임 이름은 JWT 사용자가 자유롭게 정의 가능
- 충돌 방지를 위해, 새 클레임 이름은 IANA "JSON Web Token Claims" 레지스트리(섹션 10.1)에 등록되거나 Collision-Resistant Name을 포함하는 공개 이름(Public Name)이어야 함
- 정의자는 클레임 이름 네임스페이스의 통제권을 확인하기 위한 합리적 예방 조치 필요

#### 4.3. 비공개 클레임 이름

- JWT의 생산자·소비자는 등록된 클레임 이름(4.1)·공개 클레임 이름(4.2)이 아닌 비공개 이름(Private Name)을 사용하기로 합의 가능(MAY)
- 공개 클레임 이름과 달리, 비공개 클레임 이름은 충돌 가능성이 있으므로 주의 필요

### 5. JOSE 헤더

- JWT 객체의 경우, JOSE 헤더 JSON 객체의 멤버는 JWT에 적용되는 암호화 연산과 선택적으로 JWT의 추가 속성을 기술
- JWT가 JWS인지 JWE인지에 따라 해당 규칙 적용
- 이 명세는 JWT가 JWS·JWE인 경우 모두에서 다음 헤더 파라미터의 사용을 추가 지정

#### 5.1. "typ" (Type) 헤더 파라미터

- [JWS], [JWE]에 의해 정의된 "typ" 헤더 파라미터 → JWT 응용프로그램이 완전한 JWT의 미디어 타입 [IANA.MediaTypes]을 선언하는 데 사용
- 용도: JWT를 포함할 수 있는 응용프로그램 데이터 구조에 JWT가 아닌 값도 존재할 수 있는 경우, 다양한 종류의 객체를 구별
- 객체가 JWT임이 이미 알려진 경우 일반적으로 사용되지 않음
- JWT 구현에 의해 무시됨 → 처리는 JWT 응용프로그램이 수행
- 존재하는 경우 값은 "JWT"일 것을 권장(RECOMMENDED)
- 미디어 타입 이름은 대소문자 구분 없음, 레거시 구현 호환을 위해 "JWT"는 대문자 표기 권장(RECOMMENDED)
- 사용은 선택적(OPTIONAL)

#### 5.2. "cty" (Content Type) 헤더 파라미터

- [JWS], [JWE]에 의해 정의된 "cty" 헤더 파라미터 → 이 명세에서 JWT에 대한 구조적 정보 전달에 사용
- 중첩 서명·암호화 연산이 없는 일반적인 경우 사용은 권장되지 않음(NOT RECOMMENDED)
- 중첩 서명·암호화가 사용되는 경우 반드시 존재해야 함(MUST), 값은 반드시 "JWT"여야 함(MUST) → Nested JWT 포함을 나타냄
- 미디어 타입 이름은 대소문자 구분 없음, 레거시 구현 호환을 위해 "JWT"는 대문자 표기 권장(RECOMMENDED)
- Nested JWT 예시: 부록 A.2 참고

#### 5.3. 헤더 파라미터로서의 클레임 복제

- 암호화된 JWT를 사용하는 일부 응용프로그램은 일부 클레임의 암호화되지 않은 표현이 필요
  - 예: JWT 복호화 전 처리 여부·방법 결정을 위한 응용프로그램 처리 규칙
- 이 명세는 JWT Claims Set에 존재하는 클레임을 JWE인 JWT의 헤더 파라미터로 복제하는 것을 허용
- 복제된 클레임이 존재하면, 응용프로그램이 별도 처리 규칙을 정의하지 않는 한 값이 동일한지 검증해야 함(SHOULD)
- 암호화되지 않은 방식으로 전송해도 안전한 클레임만 복제되도록 보장하는 것은 응용프로그램의 책임
- 이 명세 섹션 10.4.1은 "iss"(issuer)·"sub"(subject)·"aud"(audience) 헤더 파라미터 이름을 등록 → 암호화된 JWT에서 이들의 암호화되지 않은 복제본 제공 목적
- 다른 명세도 필요에 따라 등록된 클레임 이름을 헤더 파라미터 이름으로 등록 가능(MAY)

### 6. 비보안 JWT

- 서명·암호화 이외의 수단(예: JWT를 포함하는 데이터 구조에 대한 서명)으로 JWT 콘텐츠가 보안되는 사용 사례 지원을 위해, JWT는 서명·암호화 없이 생성 가능(MAY)
- 비보안 JWT: "alg" 헤더 파라미터 값 "none" 사용, JWS Signature 값은 빈 문자열
- JWA 명세 [JWA]에 정의된 대로 JWT Claims Set을 JWS Payload로 하는 Unsecured JWS

#### 6.1. 비보안 JWT 예시

다음 JOSE 헤더 예시는 인코딩된 객체가 비보안 JWT임을 선언:

```
{"alg":"none"}
```

JOSE 헤더의 UTF-8 표현 옥텟을 Base64url 인코딩하면 다음의 인코딩된 JOSE 헤더 값 생성:

```
eyJhbGciOiJub25lIn0
```

섹션 3.1과 동일한 JWT Claims Set:

```
{"iss":"joe",
 "exp":1300819380,
 "http://example.com/is_root":true}
```

JWS Payload의 Base64url 인코딩된 표현(표시 목적으로만 줄 바꿈 포함):

```
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
```

- 인코딩된 JWS Signature는 빈 문자열

인코딩된 파트들을 순서대로 마침표('.') 문자로 연결하면 완전한 JWT 생성(표시 목적으로만 줄 바꿈 포함):

```
eyJhbGciOiJub25lIn0
.
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
.
```

### 7. JWT 생성 및 검증

#### 7.1. JWT 생성

JWT 생성 시 다음 단계 수행 (단계의 입력·출력 간 의존성이 없으면 순서 무관):

1. 원하는 클레임을 포함하는 JWT Claims Set 생성
   - 공백은 표현에서 명시적으로 허용, 인코딩 전 정규화 불필요
2. Message = JWT Claims Set의 UTF-8 표현 옥텟
3. 원하는 헤더 파라미터 집합을 포함하는 JOSE 헤더 생성
   - JWT는 [JWS] 또는 [JWE] 명세 중 하나를 준수해야 함(MUST)
   - 공백은 표현에서 명시적으로 허용, 인코딩 전 정규화 불필요
4. JWT가 JWS인지 JWE인지에 따라 두 경우:
   - JWT가 JWS인 경우: Message를 JWS Payload로 사용하여 JWS 생성 → [JWS]에 지정된 모든 단계를 따라야 함(MUST)
   - JWT가 JWE인 경우: Message를 JWE의 평문으로 사용하여 JWE 생성 → [JWE]에 지정된 모든 단계를 따라야 함(MUST)
5. 중첩 서명·암호화 연산 수행 시: Message = JWS 또는 JWE로 하고, 단계 3으로 복귀 → 새 JOSE 헤더에 "cty" 값 "JWT" 사용
6. 그렇지 않으면 결과 JWT = JWS 또는 JWE

#### 7.2. JWT 검증

JWT 검증 시 다음 단계 수행 (단계의 입력·출력 간 의존성이 없으면 순서 무관, 어느 단계라도 실패하면 JWT는 거부되어야 함(MUST) — 즉 응용프로그램에 의해 유효하지 않은 입력으로 처리):

1. JWT가 최소한 하나의 마침표('.') 문자를 포함하는지 검증
2. 인코딩된 JOSE 헤더 = JWT에서 첫 번째 마침표('.') 이전 부분
3. 줄 바꿈·공백·기타 추가 문자 미사용 제한에 따라 인코딩된 JOSE 헤더를 Base64url 디코딩
4. 결과 옥텟 시퀀스가 RFC 7159 [RFC7159]에 부합하는 완전히 유효한 JSON 객체의 UTF-8 인코딩된 표현인지 검증 → JOSE 헤더 = 이 JSON 객체
5. 결과 JOSE 헤더가 구문·의미 모두 이해·지원되거나, 이해되지 않을 때 무시되도록 지정된 파라미터·값만 포함하는지 검증
6. JWT가 JWS인지 JWE인지를 [JWE] 섹션 9에 설명된 방법 중 하나로 결정
7. JWT가 JWS인지 JWE인지에 따라 두 경우:
   - JWT가 JWS인 경우: [JWS]에 지정된 단계에 따라 JWS 검증 → Message = JWS Payload의 base64url 디코딩 결과
   - JWT가 JWE인 경우: [JWE]에 지정된 단계에 따라 JWE 검증 → Message = 결과 평문
8. JOSE 헤더가 값 "JWT"인 "cty"를 포함하면 → Message는 중첩 서명·암호화 연산 대상이었던 JWT → Message를 JWT로 사용해 단계 1로 복귀
9. 그렇지 않으면, 줄 바꿈·공백·기타 추가 문자 미사용 제한에 따라 Message를 Base64url 디코딩
10. 결과 옥텟 시퀀스가 RFC 7159 [RFC7159]에 부합하는 완전히 유효한 JSON 객체의 UTF-8 인코딩된 표현인지 검증 → JWT Claims Set = 이 JSON 객체

- JWT가 성공적으로 검증되더라도, 사용된 알고리즘이 응용프로그램에 수용 가능하지 않으면 응용프로그램은 JWT를 거부해야 함(SHOULD)
- 어떤 알고리즘이 주어진 컨텍스트에서 사용될 수 있는지는 응용프로그램의 결정 사항

#### 7.3. 문자열 비교 규칙

- JWT 처리는 필연적으로 알려진 문자열을 JSON 객체의 멤버·값과 비교해야 함
  - 예: 알고리즘 확인 시, 유니코드 [UNICODE] 문자열 인코딩 "alg"가 일치하는 헤더 파라미터 이름이 있는지 JOSE 헤더 멤버 이름과 비교
- 멤버 이름 비교를 위한 JSON 규칙: RFC 7159 [RFC7159] 섹션 8.3 참고
- 수행되는 유일한 문자열 비교 연산이 동등·부등이므로, 멤버 이름과 멤버 값을 알려진 문자열과 비교하는 데 동일 규칙 사용 가능
- 이 비교 규칙은 멤버의 정의가 다른 비교 규칙 사용을 명시적으로 지정하지 않는 한, 모든 JSON 문자열 비교에 사용되어야 함(MUST)
  - 이 명세에서는 "typ"·"cty" 멤버 값만 이 비교 규칙을 사용하지 않음
- 일부 응용프로그램은 대소문자 구분 값에 대소문자 구분 없는 정보를 포함할 수 있음
  - 예: DNS 이름을 "iss" 클레임 값의 일부로 포함
  - 둘 이상의 당사자가 비교를 위해 동일한 값을 생산해야 하는 경우, 응용프로그램은 대소문자 구분 없는 부분을 소문자로 변환하는 등 표현에 사용할 정규 대소문자 규약을 정의해야 할 수 있음
  - 다른 모든 당사자가 생산 당사자가 발행한 값을 독립 생산 값과 비교하지 않고 그대로 소비하는 경우, 생산자가 사용한 대소문자는 중요하지 않음

### 8. 구현 요구사항

- 이 섹션은 이 명세의 어떤 알고리즘·기능이 구현 필수인지 정의
- 이 명세를 사용하는 응용프로그램은 자신이 사용하는 구현에 추가 요구사항 부과 가능
  - 예: 한 응용프로그램은 암호화된 JWT·Nested JWT 지원을 요구, 다른 응용프로그램은 P-256 곡선·SHA-256 해시를 사용하는 ECDSA("ES256") 서명 지원을 요구
- JSON Web Algorithms [JWA]에 지정된 서명·MAC 알고리즘 중, 준수하는 JWT 구현은 HMAC SHA-256("HS256")과 "none"만 반드시 구현해야 함(MUST)
- SHA-256을 사용하는 RSASSA-PKCS1-v1_5("RS256")와 P-256 곡선·SHA-256을 사용하는 ECDSA("ES256")도 지원 권장(RECOMMENDED)
- 다른 알고리즘·키 크기 지원은 선택적(OPTIONAL)
- 암호화된 JWT 지원은 선택적(OPTIONAL)
  - 암호화 기능을 제공하는 구현은 [JWA]에 지정된 알고리즘 중, 2048비트 키의 RSAES-PKCS1-v1_5("RSA1_5"), 128/256비트 키의 AES Key Wrap("A128KW"·"A256KW"), AES-CBC와 HMAC SHA-2를 사용하는 복합 인증 암호화("A128CBC-HS256"·"A256CBC-HS512")를 반드시 구현해야 함(MUST)
  - ECDH-ES("ECDH-ES+A128KW"·"ECDH-ES+A256KW")와 128/256비트 키의 AES-GCM("A128GCM"·"A256GCM")도 지원 권장(RECOMMENDED)
  - 다른 알고리즘·키 크기 지원은 선택적(OPTIONAL)
- Nested JWT 지원은 선택적(OPTIONAL)

### 9. 콘텐츠가 JWT임을 선언하기 위한 URI

- 이 명세는 참조되는 콘텐츠가 JWT임을 나타내기 위해, 미디어 타입이 아닌 URI로 콘텐츠 타입을 선언하는 응용프로그램이 사용할 수 있는 URN "urn:ietf:params:oauth:token-type:jwt" 등록

### 10. IANA 고려사항

#### 10.1. JSON Web Token Claims 레지스트리

- IANA "JSON Web Token Claims" 레지스트리 설정 → 클레임 이름과 이를 정의하는 명세 참조 기록
- 섹션 4.1에 정의된 클레임 이름들 등록
- 값은 jwt-reg-review@ietf.org 메일링 리스트에서 1명 이상의 Designated Expert 조언에 따라 3주간 검토 후 Specification Required [RFC5226] 기준으로 등록
  - 값 할당 전 출판이 요구되지는 않음 → 할당 후 사용될 명세가 출판될 것이라는 확신이 있으면 Designated Expert(s)가 등록 승인 가능(MAY)
- 검토를 위한 메일링 리스트 발송 시 적절한 제목 사용 필요(예: "Request to register claim: example")
- 검토 기간 내 Designated Expert(s)는 요청을 승인·거부하고 검토 리스트·IANA에 통보
  - 거부 시 설명·가능하면 제안사항 포함
  - 21일 넘게 미결정된 요청은 iesg@ietf.org 메일링 리스트를 통해 IESG의 주의 환기 가능
- Designated Expert(s)의 판단 기준: 기존 기능 중복 여부, 일반 적용 가능성 vs 단일 응용프로그램 전용 여부, 등록 설명의 명확성
- IANA는 Designated Expert(s)로부터의 등록 업데이트만 수용, 모든 등록 요청은 검토 메일링 리스트로 전달
- 폭넓게 정보에 기반한 검토를 위해 여러 명의 Designated Expert 임명 권장
  - 특정 Expert에게 이해 충돌이 인식되면 해당 Expert는 다른 Expert(s)의 판단에 따름

##### 10.1.1. 등록 템플릿

- Claim Name: 요청된 클레임 이름(예: "example"). 8자 이하 권장, 대소문자 구분. 1자 이름("e")·긴 이름("example") 모두 가능
- Claim Description: 클레임에 대한 간단한 설명(예: "Example description")
- Change Controller: 표준 트랙 RFC는 "IESG"로 기술, 그 외는 책임 당사자 이름 기재. 우편 주소·이메일·홈페이지 URI 등도 포함 가능
- Specification Document(s): 클레임 의미론을 지정하는 문서 참조 (구문·의미론의 정밀한 정의 포함)

##### 10.1.2. 초기 레지스트리 내용

- Claim Name "iss" · Claim Description Issuer · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.1
- Claim Name "sub" · Claim Description Subject · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.2
- Claim Name "aud" · Claim Description Audience · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.3
- Claim Name "exp" · Claim Description Expiration Time · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.4
- Claim Name "nbf" · Claim Description Not Before · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.5
- Claim Name "iat" · Claim Description Issued At · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.6
- Claim Name "jti" · Claim Description JWT ID · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.7

#### 10.2. urn:ietf:params:oauth:token-type:jwt 서브 네임스페이스 등록

- 콘텐츠가 JWT임을 나타내는 데 사용 가능한 값 "token-type:jwt"를, "An IETF URN Sub-Namespace for OAuth" [RFC6755]에 의해 설정된 IANA "OAuth URI" 레지스트리에 등록

##### 10.2.1. 레지스트리 내용

- URN urn:ietf:params:oauth:token-type:jwt · Common Name JSON Web Token (JWT) Token Type · Change Controller IESG · Specification Document(s) RFC 7519

#### 10.3. 미디어 타입 등록

- 콘텐츠가 JWT임을 나타내는 데 사용 가능한 "application/jwt" 미디어 타입 [RFC2046]을, RFC 6838 [RFC6838]에 설명된 방식으로 "Media Types" 레지스트리 [IANA.MediaTypes]에 등록

##### 10.3.1. 레지스트리 내용

- Type name: application
- Subtype name: jwt
- Required parameters: n/a
- Optional parameters: n/a
- Encoding considerations: 8bit. JWT 값은 마침표('.') 문자로 구분된 일련의 base64url 인코딩 값(일부는 빈 문자열 가능)으로 인코딩
- Security considerations: RFC 7519의 보안 고려사항 섹션 참고
- Interoperability considerations: n/a
- Published specification: RFC 7519
- Applications that use this media type: OpenID Connect · Mozilla Persona · Salesforce · Google · Android · Windows Azure · Amazon Web Services 및 기타 다수
- Fragment identifier considerations: n/a
- Additional information: Magic number(s) n/a · File extension(s) n/a · Macintosh file type code(s) n/a
- Person & email address to contact for further information: Michael B. Jones, mbj@microsoft.com
- Intended usage: COMMON
- Restrictions on usage: none
- Author: Michael B. Jones, mbj@microsoft.com
- Change controller: IESG
- Provisional registration? No

#### 10.4. 헤더 파라미터 이름 등록

- 섹션 5.3에 따라 JWE에서 헤더 파라미터로 복제되는 클레임에 사용하기 위해, [JWS]에 의해 설정된 IANA "JSON Web Signature and Encryption Header Parameters" 레지스트리에 섹션 4.1에 정의된 특정 클레임 이름들을 등록

##### 10.4.1. 레지스트리 내용

- Header Parameter Name "iss" · Description Issuer · Usage Location(s) JWE · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.1
- Header Parameter Name "sub" · Description Subject · Usage Location(s) JWE · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.2
- Header Parameter Name "aud" · Description Audience · Usage Location(s) JWE · Change Controller IESG · Specification Document(s) RFC 7519 섹션 4.1.3

### 11. 보안 고려사항

- 모든 암호화 응용프로그램에 관련된 모든 보안 문제는 JWT/JWS/JWE/JWK 에이전트가 해결해야 함
- 여기에는 사용자의 비대칭 개인키·대칭 비밀키 보호, 다양한 공격에 대한 대응책 적용이 포함

#### 11.1. 신뢰 결정

- JWT의 내용이 암호학적으로 보안되고 신뢰 결정에 필요한 컨텍스트에 바인딩되지 않는 한, 신뢰 결정에서 의존 불가
- 특히 JWT 서명·암호화에 사용되는 키는 JWT 발급자로 식별된 당사자의 통제 하에 있음을 검증 가능해야 함

#### 11.2. 서명 및 암호화 순서

- 구문적으로 Nested JWT의 서명·암호화 연산은 어떤 순서로든 적용 가능
- 서명·암호화가 모두 필요한 경우, 일반적으로 생산자는 메시지에 서명한 후 결과를 암호화해야 함(서명을 암호화)
  - 이유: 서명이 제거되어 암호화된 메시지만 남는 공격 방지 → 서명자에 대한 프라이버시 제공
  - 추가로, 암호화된 텍스트에 대한 서명은 많은 관할권에서 유효하지 않음
- 서명·암호화 순서 관련 보안 문제는 기저의 JWS·JWE 명세에 의해 이미 해결됨
  - JWE는 인증된 암호화 알고리즘만 지원 → 암호화 후 서명이 필요할 수 있다는 암호학적 우려는 이 명세에 적용되지 않음

### 12. 프라이버시 고려사항

- JWT는 프라이버시에 민감한 정보를 포함할 수 있음 → 의도하지 않은 당사자에게 공개되지 않도록 조치 필요(MUST)
- 방법 1: 암호화된 JWT 사용 + 수신자 인증
- 방법 2: 암호화되지 않은 민감 정보를 포함하는 JWT를 엔드포인트 인증을 지원하는 암호화 프로토콜(예: TLS)로만 전송
- 가장 간단한 방법: JWT에서 프라이버시 민감 정보 자체를 생략

### 13. 참조

#### 13.1. 규범적 참조

```
[ECMAScript]  Ecma International, "ECMAScript Language Specification,
              5.1 Edition", ECMA Standard 262, June 2011,
              <http://www.ecma-international.org/ecma-262/5.1/
              ECMA-262.pdf>.

[IANA.MediaTypes]
              IANA, "Media Types",
              <http://www.iana.org/assignments/media-types>.

[JWA]         Jones, M., "JSON Web Algorithms (JWA)", RFC 7518,
              DOI 10.17487/RFC7518, May 2015,
              <http://www.rfc-editor.org/info/rfc7518>.

[JWE]         Jones, M. and J. Hildebrand, "JSON Web Encryption
              (JWE)", RFC 7516, DOI 10.17487/RFC7516, May 2015,
              <http://www.rfc-editor.org/info/rfc7516>.

[JWS]         Jones, M., Bradley, J., and N. Sakimura, "JSON Web
              Signature (JWS)", RFC 7515, DOI 10.17487/RFC7515,
              May 2015, <http://www.rfc-editor.org/info/rfc7515>.

[RFC20]       Cerf, V., "ASCII format for Network Interchange",
              STD 80, RFC 20, DOI 10.17487/RFC0020, October 1969,
              <http://www.rfc-editor.org/info/rfc20>.

[RFC2046]     Freed, N. and N. Borenstein, "Multipurpose Internet
              Mail Extensions (MIME) Part Two: Media Types",
              RFC 2046, DOI 10.17487/RFC2046, November 1996,
              <http://www.rfc-editor.org/info/rfc2046>.

[RFC2119]     Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119,
              DOI 10.17487/RFC2119, March 1997,
              <http://www.rfc-editor.org/info/rfc2119>.

[RFC3986]     Berners-Lee, T., Fielding, R., and L. Masinter,
              "Uniform Resource Identifier (URI): Generic Syntax",
              STD 66, RFC 3986, DOI 10.17487/RFC3986, January 2005,
              <http://www.rfc-editor.org/info/rfc3986>.

[RFC4949]     Shirey, R., "Internet Security Glossary, Version 2",
              FYI 36, RFC 4949, DOI 10.17487/RFC4949, August 2007,
              <http://www.rfc-editor.org/info/rfc4949>.

[RFC7159]     Bray, T., Ed., "The JavaScript Object Notation (JSON)
              Data Interchange Format", RFC 7159,
              DOI 10.17487/RFC7159, March 2014,
              <http://www.rfc-editor.org/info/rfc7159>.

[UNICODE]     The Unicode Consortium, "The Unicode Standard",
              <http://www.unicode.org/versions/latest/>.
```

#### 13.2. 정보적 참조

```
[CanvasApp]   Facebook, "Canvas Applications", 2010,
              <http://developers.facebook.com/docs/authentication/
              canvas>.

[JSS]         Bradley, J. and N. Sakimura (editor), "JSON Simple
              Sign", September 2010, <http://jsonenc.info/jss/1.0/>.

[MagicSignatures]
              Panzer, J., Ed., Laurie, B., and D. Balfanz, "Magic
              Signatures", January 2011,
              <http://salmon-protocol.googlecode.com/svn/trunk/
              draft-panzer-magicsig-01.html>.

[OASIS.saml-core-2.0-os]
              Cantor, S., Kemp, J., Philpott, R., and E. Maler,
              "Assertions and Protocols for the OASIS Security
              Assertion Markup Language (SAML) V2.0", OASIS
              Standard saml-core-2.0-os, March 2005,
              <http://docs.oasis-open.org/security/saml/v2.0/
              saml-core-2.0-os.pdf>.

[POSIX.1]     IEEE, "The Open Group Base Specifications Issue 7",
              IEEE Std 1003.1, 2013 Edition, 2013,
              <http://pubs.opengroup.org/onlinepubs/9699919799/
              basedefs/V1_chap04.html#tag_04_15>.

[RFC3275]     Eastlake 3rd, D., Reagle, J., and D. Solo,
              "(Extensible Markup Language) XML-Signature Syntax and
              Processing", RFC 3275, DOI 10.17487/RFC3275,
              March 2002, <http://www.rfc-editor.org/info/rfc3275>.

[RFC3339]     Klyne, G. and C. Newman, "Date and Time on the
              Internet: Timestamps", RFC 3339,
              DOI 10.17487/RFC3339, July 2002,
              <http://www.rfc-editor.org/info/rfc3339>.

[RFC4122]     Leach, P., Mealling, M., and R. Salz, "A Universally
              Unique IDentifier (UUID) URN Namespace", RFC 4122,
              DOI 10.17487/RFC4122, July 2005,
              <http://www.rfc-editor.org/info/rfc4122>.

[RFC5226]     Narten, T. and H. Alvestrand, "Guidelines for Writing
              an IANA Considerations Section in RFCs", BCP 26,
              RFC 5226, DOI 10.17487/RFC5226, May 2008,
              <http://www.rfc-editor.org/info/rfc5226>.

[RFC6755]     Campbell, B. and H. Tschofenig, "An IETF URN
              Sub-Namespace for OAuth", RFC 6755,
              DOI 10.17487/RFC6755, October 2012,
              <http://www.rfc-editor.org/info/rfc6755>.

[RFC6838]     Freed, N., Klensin, J., and T. Hansen, "Media Type
              Specifications and Registration Procedures", BCP 13,
              RFC 6838, DOI 10.17487/RFC6838, January 2013,
              <http://www.rfc-editor.org/info/rfc6838>.

[SWT]         Hardt, D. and Y. Goland, "Simple Web Token (SWT)",
              Version 0.9.5.1, November 2009,
              <http://msdn.microsoft.com/en-us/library/windowsazure/
              hh781551.aspx>.

[W3C.CR-xml11-20060816]
              Cowan, J., "Extensible Markup Language (XML) 1.1
              (Second Edition)", World Wide Web Consortium
              Recommendation REC-xml11-20060816, August 2006,
              <http://www.w3.org/TR/2006/REC-xml11-20060816>.

[W3C.REC-xml-c14n-20010315]
              Boyer, J., "Canonical XML Version 1.0", World Wide Web
              Consortium Recommendation REC-xml-c14n-20010315,
              March 2001,
              <http://www.w3.org/TR/2001/REC-xml-c14n-20010315>.
```

### 부록 A. JWT 예시

#### A.1. 암호화된 JWT 예시

- 섹션 3.1에서 사용된 클레임을 RSAES-PKCS1-v1_5와 AES_128_CBC_HMAC_SHA_256으로 수신자에게 암호화한 예시
- 다음 JOSE 헤더가 선언하는 내용:
  - Content Encryption Key가 RSAES-PKCS1-v1_5 알고리즘으로 수신자에게 암호화 → JWE Encrypted Key 생성
  - AES_128_CBC_HMAC_SHA_256 알고리즘으로 평문에 인증된 암호화 수행 → JWE Ciphertext·JWE Authentication Tag 생성

```
{"alg":"RSA1_5","enc":"A128CBC-HS256"}
```

- 섹션 3.1의 JWT Claims Set UTF-8 표현 옥텟을 평문 값으로 사용하는 점 외에, 이 JWT 계산은 [JWE] 부록 A.2의 JWE 계산과 사용 키를 포함해 동일

완전한 JWT 결과(표시 목적으로만 줄 바꿈 포함):

```
eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.
QR1Owv2ug2WyPBnbQrRARTeEk9kDO2w8qDcjiHnSJflSdv1iNqhWXaKH4MqAkQtM
oNfABIPJaZm0HaA415sv3aeuBWnD8J-Ui7Ah6cWafs3ZwwFKDFUUsWHSK-IPKxLG
TkND09XyjORj_CHAgOPJ-Sd8ONQRnJvWn_hXV1BNMHzUjPyYwEsRhDhzjAD26ima
sOTsgruobpYGoQcXUwFDn7moXPRfDE8-NoQX7N7ZYMmpUDkR-Cx9obNGwJQ3nM52
YCitxoQVPzjbl7WBuB7AohdBoZOdZ24WlN1lVIeh8v1K4krB8xgKvRU8kgFrEn_a
1rZgN5TiysnmzTROF869lQ.
AxY8DCtDaGlsbGljb3RoZQ.
MKOle7UQrG6nSxTLX6Mqwt0orbHvAKeWnDYvpIAeZ72deHxz3roJDXQyhxx0wKaM
HDjUEOKIwrtkHthpqEanSBNYHZgmNOV7sln1Eu9g3J8.
fiK51VwhsxJ-siBMR-YFiA
```

#### A.2. 중첩 JWT 예시

- JWT가 JWE 또는 JWS의 페이로드로 사용되어 Nested JWT를 생성하는 방법을 보여주는 예시
- 이 경우 JWT Claims Set은 먼저 서명된 다음 암호화됨
- 내부 서명된 JWT는 [JWS] 부록 A.2의 예시와 동일 → 계산은 여기서 반복하지 않음
- 이 내부 JWT를 RSAES-PKCS1-v1_5와 AES_128_CBC_HMAC_SHA_256으로 수신자에게 암호화
- 다음 JOSE 헤더가 선언하는 내용:
  - Content Encryption Key가 RSAES-PKCS1-v1_5 알고리즘으로 수신자에게 암호화 → JWE Encrypted Key 생성
  - AES_128_CBC_HMAC_SHA_256 알고리즘으로 평문에 인증된 암호화 수행 → JWE Ciphertext·JWE Authentication Tag 생성
  - 평문 자체가 JWT

```
{"alg":"RSA1_5","enc":"A128CBC-HS256","cty":"JWT"}
```

JOSE 헤더의 UTF-8 표현 옥텟을 Base64url 인코딩하면 다음의 인코딩된 JOSE 헤더 값 생성:

```
eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY3R5IjoiSldU
In0
```

- 이 JWT의 계산은, 다른 JOSE 헤더·평문·JWE Initialization Vector·Content Encryption Key 값을 사용한다는 점을 제외하면 [JWE] 부록 A.2의 JWE 계산과 동일 (사용되는 RSA 키는 동일)
- 평문은 [JWS] 부록 A.2.1 끝에 있는 JWT의 ASCII 표현 옥텟(모든 공백·줄 바꿈 제거), 458 옥텟 시퀀스

사용되는 JWE Initialization Vector 값(JSON 배열 표기법):

```
[82, 101, 100, 109, 111, 110, 100, 32, 87, 65, 32, 57, 56, 48, 53, 50]
```

사용되는 Content Encryption Key 값(base64url 인코딩):

```
GawgguFyGrWKav7AX4VKUg
```

완전한 Nested JWT 결과(표시 목적으로만 줄 바꿈 포함):

```
eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY3R5IjoiSldU
In0.
g_hEwksO1Ax8Qn7HoN-BVeBoa8FXe0kpyk_XdcSmxvcM5_P296JXXtoHISr_DD_M
qewaQSH4dZOQHoUgKLeFly-9RI11TG-_Ge1bZFazBPwKC5lJ6OLANLMd0QSL4fYE
b9ERe-epKYE3xb2jfY1AltHqBO-PM6j23Guj2yDKnFv6WO72tteVzm_2n17SBFvh
DuR9a2nHTE67pe0XGBUS_TK7ecA-iVq5COeVdJR4U4VZGGlxRGPLRHvolVLEHx6D
YyLpw30Ay9R6d68YCLi9FYTq3hIXPK_-dmPlOUlKvPr1GgJzRoeC9G5qCvdcHWsq
JGTO_z3Wfo5zsqwkxruxwA.
UmVkbW9uZCBXQSA5ODA1Mg.
VwHERHPvCNcHHpTjkoigx3_ExK0Qc71RMEParpatm0X_qpg-w8kozSjfNIPPXiTB
BLXR65CIPkFqz4l1Ae9w_uowKiwyi9acgVztAi-pSL8GQSXnaamh9kX1mdh3M_TT
-FZGQFQsFhu0Z72gJKGdfGE-OE7hS1zuBD5oEUfk0Dmb0VzWEzpxxiSSBbBAzP10
l56pPfAtrjEYw-7ygeMkwBl6Z_mLS6w6xUgKlvW6ULmkV-uLC4FUiyKECK4e3WZY
Kw1bpgIqGYsw2v_grHjszJZ-_I5uM-9RA8ycX9KqPRp9gc6pXmoU_-27ATs9XCvr
ZXUtK2902AUzqpeEUJYjWWxSNsS-r1TJ1I-FMJ4XyAiGrfmo9hQPcNBYxPz3GQb2
8Y5CLSQfNgKSGt0A4isp1hBUXBHAndgtcslt7ZoQJaKe_nNJgNliWtWpJ_ebuOpE
l8jdhehdccnRMIwAmU1n7SPkmhIl1HlSOpvcvDfhUN5wuqU955vOBvfkBOh5A11U
zBuo2WlgZ6hYi9-e3w29bR0C2-pp3jbqxEDw3iWaf2dc5b-LnR0FEYXvI_tYk5rd
_J9N0mg0tQ6RbpxNEMNoA9QWk5lgdPvbh9BaO195abQ.
AVO9iT5AV4CzvDJCdhSFlQ
```

### 부록 B. JWT와 SAML Assertion의 관계

- SAML 2.0 [OASIS.saml-core-2.0-os]: JWT보다 더 큰 표현력·더 많은 보안 옵션을 가진 보안 토큰 표준 제공
  - 대신 크기·복잡성 비용 발생: SAML의 XML [W3C.CR-xml11-20060816] 및 XML Digital Signature(DSIG) [RFC3275] 사용 → 크기 증가, XML·XML Canonicalization [W3C.REC-xml-c14n-20010315] 사용 → 복잡성 증가
- JWT: HTTP 헤더·URI 쿼리 인수에 들어갈 수 있을 만큼 작은 보안 토큰 형식을 목표
  - SAML보다 훨씬 간단한 토큰 모델 + JSON [RFC7159] 객체 인코딩 구문 사용으로 달성
  - XML DSIG보다 작고 덜 유연한 형식으로 MAC·디지털 서명 토큰 보안 지원
- 결론: JWT는 SAML Assertion의 완전한 대체물이 아니며, 구현 용이성·컴팩트함이 중요한 경우에 사용할 토큰 형식
- SAML Assertion은 항상 엔티티가 주체에 대해 하는 진술
  - JWT도 같은 방식으로 자주 사용 → 진술 엔티티는 "iss"(issuer) 클레임, 주체는 "sub"(subject) 클레임으로 표현
  - 단, 이 클레임들이 선택적이므로 JWT 형식의 다른 사용도 허용됨

### 부록 C. JWT와 Simple Web Token (SWT)의 관계

- JWT와 SWT [SWT] 모두 핵심적으로 응용프로그램 간 클레임 집합 통신을 지원
- SWT: 클레임 이름·클레임 값 모두 문자열
- JWT: 클레임 이름은 문자열, 클레임 값은 어떤 JSON 타입이든 가능
- 두 토큰 타입 모두 콘텐츠의 암호화 보호 제공: SWT는 HMAC SHA-256, JWT는 서명·MAC·암호화 알고리즘을 포함하는 알고리즘 선택으로 보호

### 감사의 글

- JWT의 설계는 SWT [SWT]의 설계·단순성, Dick Hardt가 OpenID 커뮤니티 내에서 논의한 JSON 토큰 아이디어의 영향을 받음
- JSON 콘텐츠 서명 솔루션은 이전에 Magic Signatures [MagicSignatures], JSON Simple Sign [JSS], Canvas Applications [CanvasApp]에 의해 탐구됨 → 이 문서에 영향
- 이 명세는 OAuth 워킹 그룹의 작업. 다음 참여자들이 아이디어·피드백·문구를 기여: Dirk Balfanz, Richard Barnes, Brian Campbell, Alissa Cooper, Breno de Medeiros, Stephen Farrell, Yaron Y. Goland, Dick Hardt, Joe Hildebrand, Jeff Hodges, Edmund Jay, Warren Kumari, Ben Laurie, Barry Leiba, Ted Lemon, James Manger, Prateek Mishra, Kathleen Moriarty, Tony Nadalin, Axel Nennker, John Panzer, Emmanuel Raviart, David Recordon, Eric Rescorla, Jim Schaad, Paul Tarjan, Hannes Tschofenig, Sean Turner, Tom Yu
- Hannes Tschofenig, Derek Atkins: OAuth 워킹 그룹 의장. Sean Turner, Stephen Farrell, Kathleen Moriarty: 명세 작성 기간 Security Area Director

### 저자 주소

```
Michael B. Jones
Microsoft

EMail: mbj@microsoft.com
URI:   http://self-issued.info/


John Bradley
Ping Identity

EMail: ve7jtb@ve7jtb.com
URI:   http://www.thread-safe.com/


Nat Sakimura
Nomura Research Institute

EMail: n-sakimura@nri.co.jp
URI:   http://nat.sakimura.org/
```

---

# JSON Web Token 현행 모범 사례

```
Internet Engineering Task Force (IETF)                        Y. Sheffer
Request for Comments: 8725                                        Intuit
BCP: 225                                                        D. Hardt
Updates: 7519
Category: Best Current Practice                                 M. Jones
ISSN: 2070-1721                                                Microsoft
                                                           February 2020
```

#### Abstract

- JWT: 서명·암호화 가능한 클레임 집합을 포함하는 URL-safe JSON 기반 보안 토큰
- 디지털 신원 영역·기타 애플리케이션 영역에서 다수의 프로토콜·애플리케이션에 널리 사용
- 이 현행 모범 사례 문서는 RFC 7519를 업데이트 → JWT의 안전한 구현·배포를 위한 실행 가능한 지침 제공

### 이 메모의 상태

- 인터넷 현행 모범 사례를 문서화하는 메모
- IETF 산출물, IETF 커뮤니티 합의 반영
- 공개 검토 완료 → IESG 발행 승인
- BCP 관련 추가 정보: RFC 7841 섹션 2 참고
- 현재 상태·정오표·피드백 제공 방법: https://www.rfc-editor.org/info/rfc8725

### 저작권 고지

- Copyright (c) 2020 IETF Trust and the persons identified as the document authors. All rights reserved.
- BCP 78 및 발행일 기준 유효한 IETF 문서에 관한 IETF Trust 법적 조항(https://trustee.ietf.org/license-info) 적용 대상
- 문서에서 추출된 코드 구성요소: Trust Legal Provisions 섹션 4.e에 설명된 Simplified BSD License 텍스트 포함 필요, 보증 없이 제공

### 목차

```
1.  소개
    1.1.  대상 독자
    1.2.  이 문서에서 사용된 규약
2.  위협 및 취약점
    2.1.  취약한 서명 및 불충분한 서명 검증
    2.2.  취약한 대칭키
    2.3.  암호화와 서명의 잘못된 조합
    2.4.  암호문 길이 분석을 통한 평문 유출
    2.5.  안전하지 않은 타원 곡선 암호화 사용
    2.6.  JSON 인코딩의 다양성
    2.7.  대체 공격
    2.8.  교차 JWT 혼동
    2.9.  서버에 대한 간접 공격
3.  모범 사례
    3.1.  알고리즘 검증 수행
    3.2.  적절한 알고리즘 사용
    3.3.  모든 암호화 작업 검증
    3.4.  암호화 입력 검증
    3.5.  암호화 키의 충분한 엔트로피 보장
    3.6.  암호화 입력의 압축 회피
    3.7.  UTF-8 사용
    3.8.  발행자 및 주체 검증
    3.9.  대상(Audience) 사용 및 검증
    3.10. 수신된 클레임을 신뢰하지 않음
    3.11. 명시적 타이핑 사용
    3.12. 서로 다른 종류의 JWT에 대해 상호 배타적 검증 규칙 사용
4.  보안 고려사항
5.  IANA 고려사항
6.  참고 문헌
    6.1.  규범적 참고 문헌
    6.2.  참고적 참고 문헌
```

### 1. 소개

- JWT [RFC7519]: 서명·암호화 가능한 클레임 집합을 포함하는 URL-safe JSON 기반 보안 토큰
- 보안 관련 정보를 캡슐화하기 쉽고, 널리 사용 가능한 도구로 쉽게 구현 가능 → 빠른 채택
- 대표 응용 영역: 디지털 신원 정보 표현 → OpenID Connect ID Token [OpenID.Core], OAuth 2.0 [RFC6749] 액세스 토큰·리프레시 토큰 (세부사항은 배포에 따라 다름)
- JWT 명세 발행 이후 구현·배포에 대한 여러 널리 알려진 공격 발생 → 불충분하게 명세된 보안 메커니즘, 불완전한 구현, 애플리케이션의 잘못된 사용의 결과
- 이 문서의 목표: JWT의 안전한 구현·배포 촉진
  - 많은 권장사항은 JWS [RFC7515]·JWE [RFC7516]·JWA [RFC7518]에 의해 정의된 JWT 기반 암호화 메커니즘의 구현·사용에 관한 것
  - 나머지는 JWT 클레임 자체의 사용에 관한 것
- 대다수 구현·배포 시나리오에서 JWT 사용에 대한 최소 권장사항으로 의도됨
  - 이 문서를 참조하는 다른 명세는 특정 상황에 따라 더 엄격한 요구사항을 가질 수 있음 → 그 경우 구현자는 더 엄격한 요구사항 준수 권장
  - 이 문서는 하한선 제공이며 상한선이 아님 → 더 강력한 옵션은 항상 허용(예: 암호화 강도 대 계산 부하 평가 차이에 따라)
- 알고리즘 강도·공격에 대한 커뮤니티 지식은 빠르게 변화 → BCP 문서는 특정 시점의 진술 → 독자는 정오표·업데이트 확인 권장

#### 1.1. 대상 독자

- JWT 라이브러리(및 해당 라이브러리가 사용하는 JWS·JWE 라이브러리)의 구현자
- 그러한 라이브러리를 사용하는 코드의 구현자(일부 메커니즘이 라이브러리에서 제공되지 않는 범위에서)
- IETF 내외부에서 JWT에 의존하는 명세의 개발자

#### 1.2. 이 문서에서 사용된 규약

- "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL" 키워드는 모두 대문자로 나타날 때만 BCP 14 [RFC2119] [RFC8174]에 설명된 대로 해석

### 2. 위협 및 취약점

- 이 섹션은 JWT 구현·배포에서 알려진·가능한 문제 나열 → 각 문제 뒤에 완화 방안 참조 포함

#### 2.1. 취약한 서명 및 불충분한 서명 검증

- 서명된 JWT는 암호화 민첩성을 위해 "alg" 헤더 파라미터로 서명 알고리즘을 명시적으로 표시 → 일부 라이브러리·애플리케이션의 설계 결함과 결합되어 여러 공격 발생:
  - 공격자가 알고리즘을 "none"으로 변경 → 일부 라이브러리는 이 값을 신뢰, 서명 확인 없이 JWT를 "검증"
  - "RS256"(RSA, 2048비트) 값을 "HS256"(HMAC, SHA-256)으로 변경 → 일부 라이브러리는 HMAC-SHA256으로 RSA 공개키를 HMAC 공유 비밀처럼 사용해 검증 시도([McLean], [CVE-2015-9235] 참고)
- 완화 방안: 섹션 3.1, 3.2 참고

#### 2.2. 취약한 대칭키

- 일부 애플리케이션은 토큰 서명에 "HS256" 같은 키 기반 MAC 알고리즘을 사용하지만, 불충분한 엔트로피의 약한 대칭키(예: 인간이 기억할 수 있는 비밀번호)를 제공
- 이런 키는 공격자가 토큰을 확보하면 오프라인 무차별 대입·사전 공격에 취약 [Langkemper]
- 완화 방안: 섹션 3.5 참고

#### 2.3. 암호화와 서명의 잘못된 조합

- JWE 암호화된 JWT를 복호화하여 JWS 서명된 객체를 얻는 일부 라이브러리는 내부 서명을 항상 검증하지 않음
- 완화 방안: 섹션 3.3 참고

#### 2.4. 암호문 길이 분석을 통한 평문 유출

- 많은 암호화 알고리즘은 평문 길이 정보를 유출 (알고리즘·운영 모드에 따라 유출량 다름)
- 평문이 초기에 압축될 때 문제 악화 → 압축된 평문 길이(따라서 암호문 길이)가 원래 길이뿐 아니라 내용에도 의존
- 압축 공격은 공격자가 제어하는 데이터가 비밀 데이터와 동일한 압축 공간에 있을 때 특히 강력 (HTTPS에 대한 일부 공격 사례)
- 참고: 압축·암호화 일반 배경 [Kelsey], HTTP 쿠키 공격 사례 [Alawatugoda]
- 완화 방안: 섹션 3.6 참고

#### 2.5. 안전하지 않은 타원 곡선 암호화 사용

- [Sanso]에 따르면, 여러 JOSE 라이브러리가 타원 곡선 키 합의("ECDH-ES" 알고리즘) 시 입력을 올바르게 검증하지 못함
- 유효하지 않은 곡선 점을 사용한 JWE를 보내고, 복호화 결과 평문 출력을 관찰할 수 있는 공격자 → 수신자의 개인키 복구 가능
- 완화 방안: 섹션 3.4 참고

#### 2.6. JSON 인코딩의 다양성

- 폐기된 [RFC7159] 등 이전 JSON 형식은 UTF-8·UTF-16·UTF-32 등 여러 문자 인코딩 허용
- 최신 표준 [RFC8259]은 "폐쇄 생태계" 내부 사용을 제외하고 UTF-8만 허용 → 이제는 해당 모호성 없음
- 단, 오래된 구현체·폐쇄 환경 구현체는 비표준 인코딩을 생성할 수 있음 → JWT가 수신자에 의해 잘못 해석될 수 있음 → 악의적 발신자가 수신자의 검증 확인을 우회하는 데 사용 가능
- 완화 방안: 섹션 3.7 참고

#### 2.7. 대체 공격

- 한 수신자용 JWT를 그 수신자가 받아, 의도되지 않은 다른 수신자에게 사용하려는 공격
- 예: OAuth 2.0 [RFC6749] 액세스 토큰이 의도된 보호 리소스에 정당하게 제시된 후, 해당 리소스가 동일한 토큰을 의도되지 않은 다른 보호 리소스에 제시해 접근을 시도
- 이 상황이 포착되지 않으면 공격자가 접근 권한 없는 리소스에 접근하게 됨
- 완화 방안: 섹션 3.8, 3.9 참고

#### 2.8. 교차 JWT 혼동

- JWT가 다양한 애플리케이션 영역·프로토콜에 의해 사용됨에 따라, 한 목적으로 발행된 JWT가 전용되어 다른 목적으로 사용되는 것을 방지하는 일이 점점 중요
- 이는 특정 유형의 대체 공격
- JWT가 다른 종류의 JWT와 혼동될 수 있는 컨텍스트에서 사용된다면, 대체 공격 방지를 위한 완화 방안을 사용해야 함(MUST)
- 완화 방안: 섹션 3.8, 3.9, 3.11, 3.12 참고

#### 2.9. 서버에 대한 간접 공격

- 다양한 JWT 클레임이 데이터베이스·LDAP 검색 같은 조회 작업에 사용됨. 서버가 유사하게 조회하는 URL을 포함하는 클레임도 존재
- 이런 클레임은 공격자의 주입 공격·SSRF(서버 측 요청 위조) 공격 벡터로 사용 가능
- 완화 방안: 섹션 3.10 참고

### 3. 모범 사례

- 아래 모범 사례는 실무자가 앞 섹션에 나열된 위협을 완화하기 위해 적용해야 함

#### 3.1. 알고리즘 검증 수행

- 라이브러리는 호출자가 지원 알고리즘 집합을 지정할 수 있게 해야 하며(MUST), 암호화 작업 시 다른 알고리즘을 사용해서는 안 됨(MUST NOT)
- 라이브러리는 "alg"·"enc" 헤더가 실제 암호화 작업에 사용되는 알고리즘과 일치하는지 확인해야 함(MUST)
- 각 키는 정확히 하나의 알고리즘과만 사용되어야 하며(MUST), 암호화 작업 시 이를 확인해야 함(MUST)

#### 3.2. 적절한 알고리즘 사용

- [RFC7515] 섹션 5.2: "주어진 컨텍스트에서 어떤 알고리즘을 사용할 수 있는지는 애플리케이션의 결정. JWS가 성공적으로 검증되더라도, 사용된 알고리즘이 애플리케이션에 수용 가능하지 않다면 해당 JWS를 유효하지 않은 것으로 간주해야 함(SHOULD)"
- 애플리케이션은 보안 요구사항을 충족하는 암호학적으로 현재인 알고리즘만 허용해야 함(MUST)
  - 이 집합은 새 알고리즘 도입, 기존 알고리즘의 암호학적 약점 발견에 따라 시간이 지나며 변화
  - 따라서 애플리케이션은 암호화 민첩성을 갖도록 설계되어야 함(MUST)
- JWT가 TLS 같은 전송 계층에서 암호학적으로 현재인 알고리즘으로 종단간 보호되는 경우, 추가 암호학적 보호 계층이 불필요할 수 있음
  - 이 경우 "none" 알고리즘 사용도 완전히 수용 가능할 수 있음
  - "none"은 JWT가 다른 수단으로 암호학적으로 보호되는 경우에만 사용해야 함
  - "none" 사용 JWT는 콘텐츠가 선택적으로 서명되는 컨텍스트에서 자주 사용 → 서명 여부와 무관하게 URL-safe 클레임 표현·처리가 동일해질 수 있음
  - JWT 라이브러리는 호출자가 명시적으로 요청하지 않는 한 "none"을 사용하는 JWT를 생성·소비해서는 안 됨(SHOULD NOT)
- 알고리즘별 권장사항(SHOULD):
  - 모든 RSA-PKCS1 v1.5 암호화 알고리즘([RFC8017] 섹션 7.2) 회피, RSAES-OAEP([RFC8017] 섹션 7.1) 선호
  - ECDSA 서명 [ANSI-X962-2005]은 서명되는 모든 메시지에 고유한 랜덤 값 필요 → 랜덤 값의 몇 비트만 예측 가능해도 서명 체계 보안 손상 가능, 최악의 경우 개인키 복구 가능 → JWT 라이브러리는 [RFC6979]에 정의된 결정적 접근 방식으로 ECDSA를 구현해야 함(SHOULD)
    - 이 접근 방식은 기존 ECDSA 검증자와 완전히 호환 → 새 알고리즘 식별자 불필요

#### 3.3. 모든 암호화 작업 검증

- JWT에 사용된 모든 암호화 작업은 검증되어야 하며(MUST), 검증 실패 시 전체 JWT를 거부해야 함(MUST)
- 이는 단일 헤더 파라미터 집합을 가진 JWT뿐 아니라, 외부·내부 작업 모두를 애플리케이션 제공 키·알고리즘으로 검증해야 하는(MUST) 중첩된 JWT에도 적용

#### 3.4. 암호화 입력 검증

- ECDH-ES 등 일부 암호화 작업은 유효하지 않은 값을 포함할 수 있는 입력을 받음 (지정된 타원 곡선 위에 있지 않은 점, [Valenta] 섹션 7.1 등의 기타 유효하지 않은 점 포함)
- JWS/JWE 라이브러리 자체가 이런 입력을 사용 전에 검증해야 하거나, 검증을 수행하는 기반 암호화 라이브러리를 사용해야 함(또는 둘 다)
- ECDH-ES 임시 공개키(epk) 입력은 수신자가 선택한 타원 곡선에 따라 검증되어야 함
  - NIST 소수 차수 곡선 P-256, P-384, P-521: [nist-sp-800-56a-r3] 섹션 5.6.2.3.4(ECC 부분 공개키 검증 루틴)에 따라 검증해야 함(MUST)
  - "X25519"·"X448" [RFC8037] 알고리즘 사용 시 [RFC8037]의 보안 고려사항 적용

#### 3.5. 암호화 키의 충분한 엔트로피 보장

- [RFC7515] 섹션 10.1의 키 엔트로피·랜덤 값 조언, [RFC7518] 섹션 8.8의 비밀번호 고려사항을 따라야 함(MUST)
- 인간이 기억할 수 있는 비밀번호는 "HS256" 같은 키 기반 MAC 알고리즘의 키로 직접 사용해서는 안 됨(MUST NOT)
- 비밀번호는 [RFC7518] 섹션 4.8에 설명된 대로 콘텐츠 암호화가 아닌 키 암호화에만 사용해야 함
- 키 암호화에 사용되더라도 비밀번호 기반 암호화는 여전히 무차별 대입 공격 대상

#### 3.6. 암호화 입력의 압축 회피

- 데이터 압축은 암호화 전에 수행해서는 안 됨(SHOULD NOT) — 압축된 데이터는 종종 평문 정보를 드러냄

#### 3.7. UTF-8 사용

- [RFC7515], [RFC7516], [RFC7519] 모두 헤더 파라미터·JWT 클레임 세트의 JSON 인코딩·디코딩에 UTF-8 사용을 지정 → 최신 JSON 명세 [RFC8259]와도 일치
- 구현체·애플리케이션은 UTF-8을 사용해야 하며(MUST), 다른 유니코드 인코딩을 사용하거나 허용해서는 안 됨(MUST NOT)

#### 3.8. 발행자 및 주체 검증

- JWT에 "iss"(발행자) 클레임이 포함된 경우, 애플리케이션은 JWT의 암호화 작업에 사용된 키가 발행자에 속하는지 검증해야 함(MUST) → 그렇지 않으면 JWT를 거부해야 함(MUST)
- 발행자가 소유한 키를 결정하는 수단은 애플리케이션에 따라 다름
  - 예: OpenID Connect [OpenID.Core] 발행자 값은 "jwks_uri" 값을 포함하는 JSON 메타데이터 문서를 참조하는 "https" URL, 이 "jwks_uri"는 발행자 키를 JWK Set [RFC7517]으로 검색하는 "https" URL. 동일한 메커니즘이 [RFC8414]에서도 사용됨
- JWT에 "sub"(주체) 클레임이 포함된 경우, 애플리케이션은 주체 값이 유효한 주체 및/또는 발행자-주체 쌍에 해당하는지 검증해야 함(MUST) (발행자가 애플리케이션에 신뢰되는지 확인 포함)
  - 발행자·주체·해당 쌍이 유효하지 않으면 JWT를 거부해야 함(MUST)

#### 3.9. 대상(Audience) 사용 및 검증

- 동일한 발행자가 둘 이상의 신뢰 당사자·애플리케이션용 JWT를 발행할 수 있는 경우, JWT는 의도된 사용 여부·대체 여부 판단에 쓰이는 "aud"(대상) 클레임을 포함해야 함(MUST)
- 이런 경우, 신뢰 당사자·애플리케이션은 대상 값을 검증해야 하며(MUST), 값이 없거나 수신자와 연관되지 않으면 JWT를 거부해야 함(MUST)

#### 3.10. 수신된 클레임을 신뢰하지 않음

- "kid"(키 ID) 헤더는 신뢰 당사자 애플리케이션의 키 조회에 사용 → 애플리케이션은 수신 값을 검증·정제하여 SQL·LDAP 주입 취약점이 생기지 않도록 해야 함
- 마찬가지로, 임의 URL을 포함할 수 있는 "jku"(JWK 세트 URL)·"x5u"(X.509 URL) 헤더를 맹목적으로 따르면 SSRF 공격 발생 가능 → 애플리케이션은 이런 공격으로부터 보호해야 함(SHOULD)
  - 예: URL을 허용된 위치 화이트리스트와 대조, GET 요청에 쿠키 미전송

#### 3.11. 명시적 타이핑 사용

- 한 종류의 JWT가 다른 종류로 혼동될 수 있는 경우, 해당 JWT에 명시적 JWT 유형 값을 포함하고 검증 규칙에서 유형 확인을 지정 가능 → 이 메커니즘으로 혼동 방지
- 명시적 JWT 타이핑은 "typ" 헤더 파라미터로 수행. 예: [RFC8417]은 Security Event Token(SET)의 명시적 타이핑에 "application/secevent+jwt" 미디어 유형 사용
- [RFC7515] 섹션 4.1.9의 "typ" 정의에 따라, "typ" 값에서 "application/" 접두사 생략이 권장됨(RECOMMENDED)
  - 예: SET용 "typ" 값은 "secevent+jwt"여야 함(SHOULD)
- 명시적 타이핑 사용 시, "application/example+jwt" 형식의 미디어 유형 이름 사용이 권장됨(RECOMMENDED) ("example"은 특정 JWT 종류의 식별자로 대체)
- 중첩된 JWT에 명시적 타이핑 적용 시, 명시적 유형 값을 포함하는 "typ" 헤더 파라미터는 내부 JWT(페이로드가 JWT Claims Set인 JWT)에 존재해야 함(MUST)
  - 일부 경우, 전체 중첩 JWT를 명시적으로 타이핑하기 위해 외부 JWT에도 동일한 "typ" 값이 존재할 수 있음
- 명시적 타이핑이 기존 종류의 JWT와의 명확한 구분을 항상 달성하지는 못함 (기존 검증 규칙이 "typ" 값을 사용하지 않는 경우가 종종 있음)
- 명시적 타이핑은 JWT의 새로운 사용에 권장됨(RECOMMENDED)

#### 3.12. 서로 다른 종류의 JWT에 대해 상호 배타적 검증 규칙 사용

- JWT의 각 애플리케이션은 필수·선택적 클레임과 관련 검증 규칙을 지정하는 프로필을 정의
- 동일한 발행자가 둘 이상의 JWT 종류를 발행할 수 있는 경우, 검증 규칙은 잘못된 종류의 JWT를 거부하도록 상호 배타적으로 작성되어야 함(MUST)
- 한 컨텍스트에서 다른 컨텍스트로의 JWT 대체를 방지하기 위한 전략:
  - 서로 다른 종류의 JWT에 명시적 타이핑 사용 → 고유한 "typ" 값으로 구별
  - 서로 다른 필수 클레임 집합·값 사용 → 다른 클레임·값을 가진 것을 거부
  - 서로 다른 필수 헤더 파라미터 집합·값 사용 → 다른 헤더 파라미터·값을 가진 것을 거부
  - 서로 다른 종류의 JWT에 서로 다른 키 사용 → 한 종류 검증용 키가 다른 종류 검증에는 실패
  - 동일 발행자의 서로 다른 JWT 사용에 서로 다른 "aud" 값 사용 → 대상 검증으로 부적절한 컨텍스트의 대체 JWT 거부
  - 서로 다른 종류의 JWT에 서로 다른 발행자 사용 → 고유한 "iss" 값으로 분리
- 애플리케이션에 따라 유형·필수 클레임·값·헤더 파라미터·키 사용·발행자의 최적 조합은 다름
- 새로운 JWT 애플리케이션에는 명시적 타이핑 사용이 권장됨(RECOMMENDED, 섹션 3.11 참고)

### 4. 보안 고려사항

- 이 문서 전체가 JWT 구현·배포 시의 보안 고려사항에 관한 것

### 5. IANA 고려사항

- 이 문서에는 IANA 관련 작업 없음

### 6. 참고 문헌

#### 6.1. 규범적 참고 문헌

```
[nist-sp-800-56a-r3]
           Barker, E., Chen, L., Roginsky, A., Vassilev, A., and
           R. Davis, "Recommendation for Pair-Wise Key-Establishment
           Schemes Using Discrete Logarithm Cryptography", NIST
           Special Publication 800-56A Revision 3,
           DOI 10.6028/NIST.SP.800-56Ar3, April 2018,
           <https://doi.org/10.6028/NIST.SP.800-56Ar3>.

[RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
           Requirement Levels", BCP 14, RFC 2119,
           DOI 10.17487/RFC2119, March 1997,
           <https://www.rfc-editor.org/info/rfc2119>.

[RFC6979]  Pornin, T., "Deterministic Usage of the Digital Signature
           Algorithm (DSA) and Elliptic Curve Digital Signature
           Algorithm (ECDSA)", RFC 6979, DOI 10.17487/RFC6979,
           August 2013, <https://www.rfc-editor.org/info/rfc6979>.

[RFC7515]  Jones, M., Bradley, J., and N. Sakimura, "JSON Web
           Signature (JWS)", RFC 7515, DOI 10.17487/RFC7515,
           May 2015, <https://www.rfc-editor.org/info/rfc7515>.

[RFC7516]  Jones, M. and J. Hildebrand, "JSON Web Encryption (JWE)",
           RFC 7516, DOI 10.17487/RFC7516, May 2015,
           <https://www.rfc-editor.org/info/rfc7516>.

[RFC7518]  Jones, M., "JSON Web Algorithms (JWA)", RFC 7518,
           DOI 10.17487/RFC7518, May 2015,
           <https://www.rfc-editor.org/info/rfc7518>.

[RFC7519]  Jones, M., Bradley, J., and N. Sakimura, "JSON Web Token
           (JWT)", RFC 7519, DOI 10.17487/RFC7519, May 2015,
           <https://www.rfc-editor.org/info/rfc7519>.

[RFC8017]  Moriarty, K., Ed., Kaliski, B., Jonsson, J., and
           A. Rusch, "PKCS #1: RSA Cryptography Specifications
           Version 2.2", RFC 8017, DOI 10.17487/RFC8017,
           November 2016,
           <https://www.rfc-editor.org/info/rfc8017>.

[RFC8037]  Liusvaara, I., "CFRG Elliptic Curve Diffie-Hellman (ECDH)
           and Signatures in JSON Object Signing and Encryption
           (JOSE)", RFC 8037, DOI 10.17487/RFC8037, January 2017,
           <https://www.rfc-editor.org/info/rfc8037>.

[RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in
           RFC 2119 Key Words", BCP 14, RFC 8174,
           DOI 10.17487/RFC8174, May 2017,
           <https://www.rfc-editor.org/info/rfc8174>.

[RFC8259]  Bray, T., Ed., "The JavaScript Object Notation (JSON)
           Data Interchange Format", STD 90, RFC 8259,
           DOI 10.17487/RFC8259, December 2017,
           <https://www.rfc-editor.org/info/rfc8259>.
```

#### 6.2. 참고적 참고 문헌

```
[Alawatugoda]
           Alawatugoda, J., Stebila, D., and C. Boyd, "Protecting
           Encrypted Cookies from Compression Side-Channel Attacks",
           Financial Cryptography and Data Security, pp. 86-106,
           DOI 10.1007/978-3-662-47854-7_6, July 2015,
           <https://doi.org/10.1007/978-3-662-47854-7_6>.

[ANSI-X962-2005]
           American National Standards Institute, "Public Key
           Cryptography for the Financial Services Industry: the
           Elliptic Curve Digital Signature Algorithm (ECDSA)",
           ANSI X9.62-2005, November 2005.

[CVE-2015-9235]
           NIST, "CVE-2015-9235 Detail", National Vulnerability
           Database, May 2018,
           <https://nvd.nist.gov/vuln/detail/CVE-2015-9235>.

[Kelsey]   Kelsey, J., "Compression and Information Leakage of
           Plaintext", Fast Software Encryption, pp. 263-276,
           DOI 10.1007/3-540-45661-9_21, July 2002,
           <https://doi.org/10.1007/3-540-45661-9_21>.

[Langkemper]
           Langkemper, S., "Attacking JWT authentication",
           September 2016,
           <https://www.sjoerdlangkemper.nl/2016/09/28/attacking-jwt-authentication/>.

[McLean]   McLean, T., "Critical vulnerabilities in JSON Web Token
           libraries", March 2015,
           <https://auth0.com/blog/critical-vulnerabilities-in-json-web-token-libraries/>.

[OpenID.Core]
           Sakimura, N., Bradley, J., Jones, M., de Medeiros, B.,
           and C. Mortimore, "OpenID Connect Core 1.0 incorporating
           errata set 1", November 2014,
           <https://openid.net/specs/openid-connect-core-1_0.html>.

[RFC6749]  Hardt, D., Ed., "The OAuth 2.0 Authorization Framework",
           RFC 6749, DOI 10.17487/RFC6749, October 2012,
           <https://www.rfc-editor.org/info/rfc6749>.

[RFC7159]  Bray, T., Ed., "The JavaScript Object Notation (JSON)
           Data Interchange Format", RFC 7159,
           DOI 10.17487/RFC7159, March 2014,
           <https://www.rfc-editor.org/info/rfc7159>.

[RFC7517]  Jones, M., "JSON Web Key (JWK)", RFC 7517,
           DOI 10.17487/RFC7517, May 2015,
           <https://www.rfc-editor.org/info/rfc7517>.

[RFC8414]  Jones, M., Sakimura, N., and J. Bradley, "OAuth 2.0
           Authorization Server Metadata", RFC 8414,
           DOI 10.17487/RFC8414, June 2018,
           <https://www.rfc-editor.org/info/rfc8414>.

[RFC8417]  Hunt, P., Ed., Jones, M., Denniss, W., and M. Ansari,
           "Security Event Token (SET)", RFC 8417,
           DOI 10.17487/RFC8417, July 2018,
           <https://www.rfc-editor.org/info/rfc8417>.

[Sanso]    Sanso, A., "Critical Vulnerability Uncovered in JSON
           Encryption", March 2017,
           <https://blogs.adobe.com/security/2017/03/critical-vulnerability-uncovered-in-json-encryption.html>.

[Valenta]  Valenta, L., Sullivan, N., Sanso, A., and N. Heninger,
           "In search of CurveSwap: Measuring elliptic curve
           implementations in the wild", March 2018,
           <https://ia.cr/2018/298>.
```

### 감사의 말

- "ECDH-ES" 유효하지 않은 점 공격을 JWE·JWT 구현자의 주의로 가져온 Antonio Sanso에게 감사
- Tim McLean은 RSA/HMAC 혼동 공격을 발표
- 명시적 타이핑 사용을 옹호한 Nat Sakimura에게 감사
- 다수의 의견을 준 Neil Madden, 검토를 해 준 Carsten Bormann, Brian Campbell, Brian Carpenter, Alissa Cooper, Roman Danyliw, Ben Kaduk, Mirja Kuhlewind, Barry Leiba, Eric Rescorla, Adam Roach, Martin Vigoureux, Eric Vyncke에게 감사

### 저자 주소

```
Yaron Sheffer
Intuit

Email: yaronf.ietf@gmail.com


Dick Hardt

Email: dick.hardt@gmail.com


Michael B. Jones
Microsoft

Email: mbj@microsoft.com
URI:   https://self-issued.info/
```
