# JWT와 JWT 최선의 관행

Internet Engineering Task Force (IETF)                          M. Jones
Request for Comments: 7519                                     Microsoft
Category: Standards Track                                     J. Bradley
ISSN: 2070-1721                                            Ping Identity
                                                             N. Sakimura
                                                                     NRI
                                                                May 2015

## JSON Web Token (JWT)

#### Abstract

   JSON Web Token (JWT)은 두 당사자 간에 전송될 클레임을 표현하기 위한
   컴팩트하고 URL-safe한 수단이다.
 JWT의 클레임은 JSON Web Signature (JWS)
   구조의 페이로드 또는 JSON Web Encryption (JWE) 구조의 평문으로 사용되는
   JSON 객체로 인코딩되어, 클레임이 디지털 서명되거나 Message Authentication
   Code (MAC)로 무결성 보호되고/되거나 암호화될 수 있다.

### 이 메모의 상태

   이 문서는 Internet Standards Track 문서이다.
 이 문서는 Internet
   Engineering Task Force (IETF)의 산출물이다.
 이 문서는 IETF 커뮤니티의
   합의를 나타낸다.
 이 문서는 공개 검토를 받았으며 Internet Engineering
   Steering Group (IESG)에 의해 출판이 승인되었다.
 Internet Standards에 대한
   추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 피드백 제공 방법에 관한 정보는
   http://www.rfc-editor.org/info/rfc7519 에서 얻을 수 있다.

### 저작권 고지

   Copyright (c) 2015 IETF Trust and the persons identified as the
   document authors. All rights reserved.

   이 문서는 이 문서의 발행일에 유효한 BCP 78 및 IETF Trust의 IETF 문서에
   관한 법적 조항(http://trustee.ietf.org/license-info)의 적용을 받는다.
   이 문서에 관한 귀하의 권리와 제한사항을 설명하고 있으므로 이 문서들을
   주의 깊게 검토하시기 바란다.
 이 문서에서 추출된 코드 구성요소는
   Trust Legal Provisions의 섹션 4.e에 설명된 대로 Simplified BSD License
   텍스트를 포함해야 하며 Simplified BSD License에 설명된 대로 보증 없이
   제공된다.

### 목차

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
           10.1.1. 등록 템플릿
           10.1.2. 초기 레지스트리 내용
       10.2. urn:ietf:params:oauth:token-type:jwt 서브 네임스페이스 등록
           10.2.1. 레지스트리 내용
       10.3. 미디어 타입 등록
           10.3.1. 레지스트리 내용
       10.4. 헤더 파라미터 이름 등록
           10.4.1. 레지스트리 내용
   11. 보안 고려사항
       11.1. 신뢰 결정
       11.2. 서명 및 암호화 순서
   12. 프라이버시 고려사항
   13. 참조
       13.1. 규범적 참조
       13.2. 정보적 참조
   부록 A.  JWT 예시
       A.1.  암호화된 JWT 예시
       A.2.  중첩 JWT 예시
   부록 B.  JWT와 SAML Assertion의 관계
   부록 C.  JWT와 Simple Web Token (SWT)의 관계
   감사의 글
   저자 주소

### 1. 서론

   JSON Web Token (JWT)은 HTTP Authorization 헤더 및 URI 쿼리 파라미터와
   같은 공간 제약이 있는 환경을 위한 컴팩트 클레임 표현 형식이다.
 JWT는
   JSON Web Signature (JWS) [JWS] 구조의 페이로드 또는 JSON Web Encryption
   (JWE) [JWE] 구조의 평문으로 사용되는 JSON [RFC7159] 객체로 클레임을
   인코딩하여, 클레임이 디지털 서명되거나, Message Authentication Code
   (MAC)로 무결성 보호되고/되거나, 암호화될 수 있게 한다.
 JWT는 항상
   JWS Compact Serialization 또는 JWE Compact Serialization을 사용하여
   표현된다.

   JWT의 발음은 영어 단어 "jot"과 동일하다.

#### 1.1. 표기 규약

   이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY",
   "OPTIONAL"은 "Key words for use in RFCs to Indicate Requirement
   Levels" [RFC2119]에 설명된 대로 해석되어야 한다.
 해당 해석은 이러한
   키워드가 여기에서와 같이 모두 대문자로 표시될 때만 적용된다.

### 2. 용어

   이 용어들은 JSON Web Signature (JWS) [JWS] 명세에 의해 정의된다:

   - JSON Web Signature (JWS)
   - Base64url Encoding
   - Header Parameter
   - JOSE Header
   - JWS Compact Serialization
   - JWS Payload
   - JWS Signature
   - Unsecured JWS

   이 용어들은 JSON Web Encryption (JWE) [JWE] 명세에 의해 정의된다:

   - JSON Web Encryption (JWE)
   - Content Encryption Key (CEK)
   - JWE Compact Serialization
   - JWE Encrypted Key
   - JWE Initialization Vector

   이 용어들은 "Internet Security Glossary, Version 2" [RFC4949]에 의해
   정의된다:

   - Ciphertext
   - Digital Signature
   - Message Authentication Code (MAC)
   - Plaintext

   이 용어들은 이 명세에 의해 정의된다:

   JSON Web Token (JWT)
      JWS 또는 JWE로 인코딩되어, 클레임이 디지털 서명되거나 MAC 처리되고
      /되거나 암호화될 수 있도록 하는 JSON 객체로서 클레임 집합을 나타내는
      문자열.

   JWT Claims Set
      JWT에 의해 전달되는 클레임을 포함하는 JSON 객체.

   Claim
      주체에 대해 주장되는 정보의 조각.
 클레임은 클레임 이름과 클레임 값으로
      구성된 이름/값 쌍으로 표현된다.

   Claim Name
      클레임 표현의 이름 부분.
 클레임 이름은 항상 문자열이다.

   Claim Value
      클레임 표현의 값 부분.
 클레임 값은 어떤 JSON 값이든 될 수 있다.

   Nested JWT
      중첩 서명 및/또는 암호화가 적용된 JWT. Nested JWT에서는 JWT가 각각
      둘러싸는 JWS 또는 JWE 구조의 페이로드 또는 평문 값으로 사용된다.

   Unsecured JWT
      클레임이 무결성 보호되거나 암호화되지 않은 JWT.

   Collision-Resistant Name
      다른 이름들과 충돌할 가능성이 매우 낮은 방식으로 이름을 할당할 수
      있는 네임스페이스 내의 이름.

   StringOrURI
      임의의 문자열 값이 사용될 수 있지만, ':' 문자를 포함하는 모든 값은
      URI [RFC3986]여야 한다는 추가 요구사항을 가진 JSON 문자열 값.

   NumericDate
      지정된 UTC 날짜/시간까지 1970-01-01T00:00:00Z UTC로부터의 초 수를
      나타내는 JSON 숫자 값으로, 윤초를 무시한다.
 이는 IEEE Std 1003.1,
      2013 Edition [POSIX.1] 정의 "1970년 1월 1일 (목요일) 협정 세계시
      00:00:00과 현재 날짜 및 시간 사이에 경과된 초를 표현하는 값"과
      동등하며, 각 날짜는 정확히 86400초로 표현된다.
 정수 또는 비정수
      숫자 값 모두 표현될 수 있다.
 구현에 대한 참고사항은 RFC 3339
      [RFC3339], 특히 섹션 2를 참조하라.

### 3. JSON Web Token (JWT) 개요

   JWT는 JWS 및/또는 JWE 구조로 인코딩된 JSON 객체로서 클레임 집합을
   나타낸다.
 이 JSON 객체가 JWT Claims Set이다.
 RFC 7159 [RFC7159]의 섹션
   4에 따라, JSON 객체는 0개 이상의 이름/값 쌍(또는 멤버)으로 구성되며,
   이름은 문자열이고 값은 임의의 JSON 값이다.
 이러한 멤버들이 JWT가
   나타내는 클레임이다.
 이 JSON 객체는 RFC 7159 [RFC7159]의 섹션 2에 따라
   JSON 값 또는 구조 문자 앞뒤에 공백 및/또는 줄 바꿈을 포함할 수 있다.

   JWT Claims Set 내의 멤버 이름은 클레임 이름이라 한다.
 해당 값은 클레임
   값이라 한다.

   JOSE 헤더의 내용은 JWT Claims Set에 적용되는 암호화 연산을 기술한다.
   JOSE 헤더가 JWS를 위한 것이면, JWT는 JWS로 표현되고 클레임은 디지털
   서명되거나 MAC 처리되며, JWT Claims Set이 JWS Payload가 된다.
 JOSE
   헤더가 JWE를 위한 것이면, JWT는 JWE로 표현되고 클레임은 암호화되며,
   JWT Claims Set이 JWE에 의해 암호화되는 평문이 된다.
 JWT는 Nested JWT를
   생성하기 위해 다른 JWE 또는 JWS 구조 안에 둘러싸일 수 있으며, 이를
   통해 중첩 서명 및 암호화가 수행될 수 있다.

   JWT는 마침표('.') 문자로 구분된 URL-safe 파트들의 시퀀스로 표현된다.
   각 파트는 base64url 인코딩된 값을 포함한다.
 JWT의 파트 수는 JWS Compact
   Serialization 또는 JWE Compact Serialization을 사용한 결과 표현에
   의존한다.

#### 3.1. JWT 예시

   다음 JOSE 헤더 예시는 인코딩된 객체가 JWT임을 선언하며, 이 JWT는 HMAC
   SHA-256 알고리즘을 사용하여 MAC 처리된 JWS이다:

```
{"typ":"JWT",
 "alg":"HS256"}
```

   위 JSON 객체의 표현에서 잠재적 모호성을 제거하기 위해, 이 예시에서
   위의 JOSE 헤더에 실제로 사용된 UTF-8 표현의 옥텟 시퀀스도 아래에
   포함되어 있다. (모호성은 줄 바꿈의 플랫폼 간 상이한 표현(CRLF 대 LF),
   줄의 시작과 끝에서의 상이한 간격, 마지막 줄에 종료 줄 바꿈이 있는지
   여부, 기타 원인으로 인해 발생할 수 있음을 유의하라.
 이 예시에서 사용된
   표현에서, 첫 번째 줄은 선행 또는 후행 공백이 없고, CRLF 줄 바꿈(13,
   10)이 첫 번째와 두 번째 줄 사이에 발생하며, 두 번째 줄은 하나의 선행
   공백(32)과 후행 공백이 없고, 마지막 줄에는 종료 줄 바꿈이 없다.) 이
   예시에서 JOSE 헤더의 UTF-8 표현을 나타내는 옥텟(JSON 배열 표기법
   사용)은:

```
[123, 34, 116, 121, 112, 34, 58, 34, 74, 87, 84, 34, 44, 13, 10, 32,
34, 97, 108, 103, 34, 58, 34, 72, 83, 50, 53, 54, 34, 125]
```

   JOSE 헤더의 UTF-8 표현의 옥텟을 Base64url 인코딩하면 다음의 인코딩된
   JOSE 헤더 값이 생성된다:

```
eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9
```

   다음은 JWT Claims Set의 예시이다:

```
{"iss":"joe",
 "exp":1300819380,
 "http://example.com/is_root":true}
```

   다음 옥텟 시퀀스는 위의 JWT Claims Set에 이 예시에서 사용된 UTF-8
   표현이며, JWS Payload이다:

```
[123, 34, 105, 115, 115, 34, 58, 34, 106, 111, 101, 34, 44, 13, 10,
32, 34, 101, 120, 112, 34, 58, 49, 51, 48, 48, 56, 49, 57, 51, 56,
48, 44, 13, 10, 32, 34, 104, 116, 116, 112, 58, 47, 47, 101, 120, 97,
109, 112, 108, 101, 46, 99, 111, 109, 47, 105, 115, 95, 114, 111, 111,
116, 34, 58, 116, 114, 117, 101, 125]
```

   JWS Payload를 Base64url 인코딩하면 다음의 인코딩된 JWS Payload가
   생성된다 (표시 목적으로만 줄 바꿈 포함):

```
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
```

   인코딩된 JOSE 헤더와 인코딩된 JWS Payload의 MAC을 HMAC SHA-256
   알고리즘으로 계산하고 [JWS]에 지정된 방식으로 HMAC 값을 base64url
   인코딩하면 다음의 인코딩된 JWS Signature가 생성된다:

```
dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

   이 인코딩된 파트들을 이 순서대로 파트 사이에 마침표('.') 문자를 넣어
   연결하면 이 완전한 JWT가 생성된다 (표시 목적으로만 줄 바꿈 포함):

```
eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9
.
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
.
dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

   이 계산은 [JWS]의 부록 A.1에서 더 자세히 설명되어 있다.
 암호화된 JWT의
   예시는 부록 A.1을 참조하라.

### 4. JWT 클레임

   JWT Claims Set은 멤버가 JWT에 의해 전달되는 클레임인 JSON 객체를
   나타낸다.
 JWT Claims Set 내의 클레임 이름은 고유해야 한다(MUST).
 JWT
   파서는 중복 클레임 이름을 가진 JWT를 거부하거나, ECMAScript 5.1
   [ECMAScript]의 섹션 15.12 ("The JSON Object")에 지정된 대로 마지막
   중복 멤버 이름만 반환하는 JSON 파서를 사용해야 한다(MUST).

   JWT가 유효한 것으로 간주되기 위해 반드시 포함해야 하는 클레임 집합은
   컨텍스트에 의존하며 이 명세의 범위 밖이다.
 JWT의 특정 응용프로그램은
   구현이 특정 방식으로 일부 클레임을 이해하고 처리하도록 요구할 것이다.
   그러나 그러한 요구사항이 없는 경우, 구현이 이해하지 못하는 모든
   클레임은 무시되어야 한다(MUST).

   JWT 클레임 이름에는 세 가지 부류가 있다: 등록된 클레임 이름, 공개
   클레임 이름, 비공개 클레임 이름.

#### 4.1. 등록된 클레임 이름

   다음 클레임 이름들은 섹션 10.1에 의해 설정된 IANA "JSON Web Token
   Claims" 레지스트리에 등록되어 있다.
 아래에 정의된 클레임 중 어느 것도
   모든 경우에 사용하거나 구현하는 것이 필수인 것은 아니며, 오히려
   유용하고 상호 운용 가능한 클레임 집합의 출발점을 제공한다.
 JWT를
   사용하는 응용프로그램은 어떤 특정 클레임을 사용할지와 그것이 필수인지
   선택인지를 정의해야 한다.
 모든 이름은 JWT의 핵심 목표가 컴팩트한
   표현이기 때문에 짧다.

##### 4.1.1. "iss" (Issuer) 클레임

   "iss" (issuer) 클레임은 JWT를 발급한 주체를 식별한다.
 이 클레임의
   처리는 일반적으로 응용프로그램에 따라 다르다. "iss" 값은 StringOrURI
   값을 포함하는 대소문자를 구분하는 문자열이다.
 이 클레임의 사용은
   선택적(OPTIONAL)이다.

##### 4.1.2. "sub" (Subject) 클레임

   "sub" (subject) 클레임은 JWT의 주체인 주체를 식별한다.
 JWT의 클레임은
   일반적으로 주체에 대한 진술이다.
 주체 값은 발급자의 컨텍스트에서
   로컬하게 고유하도록 범위가 지정되거나, 전역적으로 고유해야 한다(MUST).
   이 클레임의 처리는 일반적으로 응용프로그램에 따라 다르다. "sub" 값은
   StringOrURI 값을 포함하는 대소문자를 구분하는 문자열이다.
 이 클레임의
   사용은 선택적(OPTIONAL)이다.

##### 4.1.3. "aud" (Audience) 클레임

   "aud" (audience) 클레임은 JWT가 의도하는 수신자를 식별한다.
 JWT를
   처리하려는 각 주체는 audience 클레임의 값으로 자신을 식별해야 한다
   (MUST).
 이 클레임이 존재할 때, 클레임을 처리하는 주체가 "aud" 클레임의
   값으로 자신을 식별하지 않으면, JWT는 거부되어야 한다(MUST).
 일반적인
   경우, "aud" 값은 각각 StringOrURI 값을 포함하는 대소문자를 구분하는
   문자열의 배열이다.
 JWT가 하나의 audience를 가지는 특수한 경우, "aud"
   값은 StringOrURI 값을 포함하는 단일 대소문자 구분 문자열일 수 있다
   (MAY). audience 값의 해석은 일반적으로 응용프로그램에 따라 다르다.
 이
   클레임의 사용은 선택적(OPTIONAL)이다.

##### 4.1.4. "exp" (Expiration Time) 클레임

   "exp" (expiration time) 클레임은 JWT가 처리를 위해 수락되어서는 안 되는
   만료 시간 이후를 식별한다. "exp" 클레임의 처리는 현재 날짜/시간이 "exp"
   클레임에 나열된 만료 날짜/시간 이전이어야 한다(MUST).
 구현자는 시계
   오차를 고려하기 위해 일반적으로 몇 분을 넘지 않는 약간의 여유를 제공할
   수 있다(MAY).
 이 값은 NumericDate 값을 포함하는 숫자여야 한다(MUST).
   이 클레임의 사용은 선택적(OPTIONAL)이다.

##### 4.1.5. "nbf" (Not Before) 클레임

   "nbf" (not before) 클레임은 JWT가 처리를 위해 수락되어서는 안 되는
   이전 시간을 식별한다. "nbf" 클레임의 처리는 현재 날짜/시간이 "nbf"
   클레임에 나열된 not-before 날짜/시간 이후이거나 같아야 한다(MUST).
   구현자는 시계 오차를 고려하기 위해 일반적으로 몇 분을 넘지 않는 약간의
   여유를 제공할 수 있다(MAY).
 이 값은 NumericDate 값을 포함하는 숫자여야
   한다(MUST).
 이 클레임의 사용은 선택적(OPTIONAL)이다.

##### 4.1.6. "iat" (Issued At) 클레임

   "iat" (issued at) 클레임은 JWT가 발급된 시간을 식별한다.
 이 클레임은
   JWT의 연령을 결정하는 데 사용될 수 있다.
 이 값은 NumericDate 값을
   포함하는 숫자여야 한다(MUST).
 이 클레임의 사용은 선택적(OPTIONAL)이다.

##### 4.1.7. "jti" (JWT ID) 클레임

   "jti" (JWT ID) 클레임은 JWT에 대한 고유 식별자를 제공한다.
 식별자 값은
   동일한 값이 우연히 다른 데이터 객체에 할당될 확률이 무시할 수 있을
   정도로 보장되는 방식으로 할당되어야 한다(MUST).
 응용프로그램이 여러
   발급자를 사용하는 경우, 다른 발급자에 의해 생산된 값 간의 충돌도
   방지되어야 한다(MUST). "jti" 클레임은 JWT가 재전송되는 것을 방지하는
   데 사용될 수 있다. "jti" 값은 대소문자를 구분하는 문자열이다.
 이
   클레임의 사용은 선택적(OPTIONAL)이다.

#### 4.2. 공개 클레임 이름

   클레임 이름은 JWT를 사용하는 사람들이 자유롭게 정의할 수 있다.
 그러나
   충돌을 방지하기 위해, 새로운 클레임 이름은 섹션 10.1에 의해 설정된
   IANA "JSON Web Token Claims" 레지스트리에 등록되거나 공개 이름
   (Public Name), 즉 Collision-Resistant Name을 포함하는 값이어야 한다.
   어느 경우든, 이름이나 값의 정의자는 클레임 이름을 정의하는 데 사용하는
   네임스페이스의 부분을 자신이 통제하고 있는지 확인하기 위해 합리적인
   예방 조치를 취해야 한다.

#### 4.3. 비공개 클레임 이름

   JWT의 생산자와 소비자는 등록된 클레임 이름(섹션 4.1)이나 공개 클레임
   이름(섹션 4.2)이 아닌 비공개 이름(Private Name)인 클레임 이름을 사용하기로
   합의할 수 있다(MAY).
 공개 클레임 이름과 달리, 비공개 클레임 이름은
   충돌 가능성이 있으므로 주의하여 사용해야 한다.

### 5. JOSE 헤더

   JWT 객체의 경우, JOSE 헤더에 의해 표현되는 JSON 객체의 멤버는 JWT에
   적용되는 암호화 연산과 선택적으로 JWT의 추가 속성을 기술한다.
 JWT가
   JWS인지 JWE인지에 따라, JOSE 헤더 값에 대한 해당 규칙이 적용된다.
 이
   명세는 JWT가 JWS인 경우와 JWE인 경우 모두에서 다음 헤더 파라미터의
   사용을 추가로 지정한다.

#### 5.1. "typ" (Type) 헤더 파라미터

   [JWS] 및 [JWE]에 의해 정의된 "typ" (type) 헤더 파라미터는 JWT
   응용프로그램이 이 완전한 JWT의 미디어 타입 [IANA.MediaTypes]을 선언하는
   데 사용된다.
 이는 JWT 객체를 포함할 수 있는 응용프로그램 데이터 구조에
   JWT가 아닌 값도 존재할 수 있는 경우, JWT 응용프로그램이 존재할 수 있는
   다양한 종류의 객체를 구별하는 데 이 값을 사용할 수 있도록 의도된
   것이다.
 객체가 JWT인 것이 이미 알려진 경우에는 일반적으로
   응용프로그램에서 사용되지 않을 것이다.
 이 파라미터는 JWT 구현에 의해
   무시된다.
 이 파라미터에 대한 모든 처리는 JWT 응용프로그램에 의해
   수행된다.
 존재하는 경우, 이 객체가 JWT임을 나타내기 위해 값이 "JWT"일
   것을 권장한다(RECOMMENDED).
 미디어 타입 이름은 대소문자를 구분하지
   않지만, 레거시 구현과의 호환성을 위해 "JWT"는 항상 대문자로 표기할
   것을 권장한다(RECOMMENDED).
 이 헤더 파라미터의 사용은 선택적
   (OPTIONAL)이다.

#### 5.2. "cty" (Content Type) 헤더 파라미터

   [JWS] 및 [JWE]에 의해 정의된 "cty" (content type) 헤더 파라미터는 이
   명세에서 JWT에 대한 구조적 정보를 전달하는 데 사용된다.
 중첩 서명 또는
   암호화 연산이 사용되지 않는 일반적인 경우에는, 이 헤더 파라미터의 사용은
   권장되지 않는다(NOT RECOMMENDED).
 중첩 서명 또는 암호화가 사용되는
   경우, 이 헤더 파라미터는 반드시 존재해야 한다(MUST).
 이 경우, 이 JWT
   안에 Nested JWT가 들어 있음을 나타내기 위해 값은 반드시 "JWT"여야 한다
   (MUST).
 미디어 타입 이름은 대소문자를 구분하지 않지만, 레거시 구현과의
   호환성을 위해 "JWT"는 항상 대문자로 표기할 것을 권장한다(RECOMMENDED).
   Nested JWT의 예시는 부록 A.2를 참조하라.

#### 5.3. 헤더 파라미터로서의 클레임 복제

   암호화된 JWT를 사용하는 일부 응용프로그램에서는 일부 클레임의 암호화되지
   않은 표현을 갖는 것이 유용하다.
 이것은 예를 들어, JWT가 복호화되기
   전에 JWT를 처리할지 여부와 방법을 결정하기 위한 응용프로그램 처리
   규칙에 사용될 수 있다.
 이 명세는 응용프로그램이 필요로 하는 대로,
   JWT Claims Set에 존재하는 클레임이 JWE인 JWT의 헤더 파라미터로 복제되는
   것을 허용한다.
 그러한 복제된 클레임이 존재하는 경우, 이를 수신하는
   응용프로그램은 응용프로그램이 이러한 클레임에 대해 다른 특정 처리 규칙을
   정의하지 않는 한, 그 값이 동일한지 검증해야 한다(SHOULD).
 암호화되지
   않은 방식으로 전송해도 안전한 클레임만 JWT의 헤더 파라미터 값으로
   복제되도록 보장하는 것은 응용프로그램의 책임이다.
 이 명세의 섹션
   10.4.1은 이를 필요로 하는 응용프로그램을 위해, 암호화된 JWT에서 이러한
   클레임의 암호화되지 않은 복제본을 제공하기 위한 목적으로 "iss"
   (issuer), "sub" (subject), "aud" (audience) 헤더 파라미터 이름을
   등록한다.
 다른 명세들도 마찬가지로 필요에 따라 등록된 클레임 이름인
   다른 이름을 헤더 파라미터 이름으로 등록할 수 있다(MAY).

### 6. 비보안 JWT

   JWT 내에 포함된 서명 및/또는 암호화 이외의 수단(예: JWT를 포함하는
   데이터 구조에 대한 서명)에 의해 JWT 콘텐츠가 보안되는 사용 사례를
   지원하기 위해, JWT는 서명이나 암호화 없이도 생성될 수 있다(MAY).
   비보안 JWT는 "alg" 헤더 파라미터 값 "none"을 사용하고 JWS Signature
   값으로 빈 문자열을 사용하는 JWS이며, JWA 명세 [JWA]에 정의된 대로
   JWT Claims Set을 JWS Payload로 하는 Unsecured JWS이다.

#### 6.1. 비보안 JWT 예시

   다음 JOSE 헤더 예시는, 인코딩된 객체가 비보안 JWT임을 선언한다:

```
{"alg":"none"}
```

   JOSE 헤더의 UTF-8 표현의 옥텟을 Base64url 인코딩하면 다음의 인코딩된
   JOSE 헤더 값이 생성된다:

```
eyJhbGciOiJub25lIn0
```

   섹션 3.1과 동일한 JWT Claims Set을 사용하여 다음의 JWT Claims Set이 된다:

```
{"iss":"joe",
 "exp":1300819380,
 "http://example.com/is_root":true}
```

   JWS Payload의 Base64url 인코딩된 표현은 다음과 같다 (표시 목적으로만
   줄 바꿈 포함):

```
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
```

   인코딩된 JWS Signature는 빈 문자열이다.

   이 인코딩된 파트들을 이 순서대로 파트 사이에 마침표('.') 문자를 넣어
   연결하면 이 완전한 JWT가 생성된다 (표시 목적으로만 줄 바꿈 포함):

```
eyJhbGciOiJub25lIn0
.
eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt
cGxlLmNvbS9pc19yb290Ijp0cnVlfQ
.
```

### 7. JWT 생성 및 검증

#### 7.1. JWT 생성

   JWT를 생성하기 위해, 다음 단계들이 수행된다.
 단계의 입력과 출력 사이에
   의존성이 없는 경우 단계의 순서는 중요하지 않다.

   1. 원하는 클레임을 포함하는 JWT Claims Set을 생성한다.
 공백은 표현에서
      명시적으로 허용되며 인코딩 전에 정규화를 수행할 필요가 없음을
      유의하라.

   2. Message를 JWT Claims Set의 UTF-8 표현의 옥텟으로 한다.

   3. 원하는 헤더 파라미터 집합을 포함하는 JOSE 헤더를 생성한다.
 JWT는
      [JWS] 또는 [JWE] 명세 중 하나를 준수해야 한다(MUST).
 공백은
      표현에서 명시적으로 허용되며 인코딩 전에 정규화를 수행할 필요가
      없음을 유의하라.

   4. JWT가 JWS인지 JWE인지에 따라, 두 가지 경우가 있다:

      - JWT가 JWS인 경우, Message를 JWS Payload로 사용하여 JWS를
        생성한다.
 JWS를 생성하기 위해 [JWS]에 지정된 모든 단계를 따라야
        한다(MUST).

      - 그렇지 않으면, JWT가 JWE인 경우, Message를 JWE의 평문으로
        사용하여 JWE를 생성한다.
 JWE를 생성하기 위해 [JWE]에 지정된 모든
        단계를 따라야 한다(MUST).

   5. 중첩 서명 또는 암호화 연산이 수행될 경우, Message를 JWS 또는 JWE로
      하고, 단계 3으로 돌아가서, 해당 단계에서 생성된 새로운 JOSE 헤더에
      "cty" (content type) 값 "JWT"를 사용한다.

   6. 그렇지 않으면, 결과 JWT를 JWS 또는 JWE로 한다.

#### 7.2. JWT 검증

   JWT를 검증할 때, 다음 단계들이 수행된다.
 단계의 입력과 출력 사이에
   의존성이 없는 경우 단계의 순서는 중요하지 않다.
 나열된 단계 중 어느
   것이라도 실패하면, JWT는 거부되어야 한다(MUST) -- 즉, 응용프로그램에
   의해 유효하지 않은 입력으로 처리되어야 한다.

   1.  JWT가 최소한 하나의 마침표('.') 문자를 포함하는지 검증한다.

   2.  인코딩된 JOSE 헤더를 JWT에서 첫 번째 마침표('.') 문자 이전의
       부분으로 한다.

   3.  줄 바꿈, 공백 또는 기타 추가 문자가 사용되지 않았다는 제한에 따라
       인코딩된 JOSE 헤더를 Base64url 디코딩한다.

   4.  결과 옥텟 시퀀스가 RFC 7159 [RFC7159]에 부합하는 완전히 유효한 JSON
       객체의 UTF-8 인코딩된 표현인지 검증한다.
 JOSE 헤더를 이 JSON
       객체로 한다.

   5.  결과 JOSE 헤더가 구문과 의미가 모두 이해되고 지원되거나, 이해되지
       않을 때 무시되도록 지정된 파라미터와 값만 포함하는지 검증한다.

   6.  JWT가 JWS인지 JWE인지를 [JWE]의 섹션 9에 설명된 방법 중 하나를
       사용하여 결정한다.

   7.  JWT가 JWS인지 JWE인지에 따라, 두 가지 경우가 있다:

       - JWT가 JWS인 경우, [JWS]에 지정된 단계에 따라 JWS를 검증한다.
         Message를 JWS Payload의 base64url 디코딩 결과로 한다.

       - 그렇지 않으면, JWT가 JWE인 경우, [JWE]에 지정된 단계에 따라
         JWE를 검증한다.
 Message를 결과 평문으로 한다.

   8.  JOSE 헤더가 값이 "JWT"인 "cty" (content type)을 포함하는 경우,
       Message는 중첩 서명 또는 암호화 연산의 대상이었던 JWT이다.
 이
       경우, Message를 JWT로 사용하여 단계 1로 돌아간다.

   9.  그렇지 않으면, 줄 바꿈, 공백 또는 기타 추가 문자가 사용되지
       않았다는 제한에 따라 Message를 Base64url 디코딩한다.

   10. 결과 옥텟 시퀀스가 RFC 7159 [RFC7159]에 부합하는 완전히 유효한
       JSON 객체의 UTF-8 인코딩된 표현인지 검증한다.
 JWT Claims Set을 이
       JSON 객체로 한다.

   마지막으로, JWT가 성공적으로 검증될 수 있더라도, JWT에 사용된 알고리즘이
   응용프로그램에 수용 가능하지 않으면, 응용프로그램은 JWT를 거부해야
   한다(SHOULD).
 어떤 알고리즘이 주어진 컨텍스트에서 사용될 수 있는지는
   응용프로그램의 결정 사항임을 유의하라.

#### 7.3. 문자열 비교 규칙

   JWT를 처리하려면 필연적으로 알려진 문자열을 JSON 객체의 멤버 및 값과
   비교해야 한다.
 예를 들어, 알고리즘이 무엇인지 확인할 때, 유니코드
   [UNICODE] 문자열 인코딩 "alg"가 일치하는 헤더 파라미터 이름이 있는지
   JOSE 헤더의 멤버 이름과 비교된다.

   멤버 이름 비교를 위한 JSON 규칙은 RFC 7159 [RFC7159]의 섹션 8.3에
   설명되어 있다.
 수행되는 유일한 문자열 비교 연산이 동등과 부등이므로,
   멤버 이름과 멤버 값을 알려진 문자열과 비교하는 데 동일한 규칙을 사용할
   수 있다.

   이러한 비교 규칙은 멤버의 정의가 해당 멤버 값에 다른 비교 규칙이
   사용됨을 명시적으로 호출하는 경우를 제외하고, 모든 JSON 문자열 비교에
   사용되어야 한다(MUST).
 이 명세에서, "typ"과 "cty" 멤버 값만이 이
   비교 규칙을 사용하지 않는다.

   일부 응용프로그램은 대소문자를 구분하는 값에 대소문자를 구분하지 않는
   정보를 포함할 수 있다.
 예를 들어, DNS 이름을 "iss" (issuer) 클레임
   값의 일부로 포함하는 경우이다.
 그러한 경우에, 둘 이상의 당사자가
   비교를 위해 동일한 값을 생산해야 할 수 있다면, 응용프로그램은 대소문자를
   구분하지 않는 부분을 소문자로 변환하는 것과 같이, 표현에 사용할 정규
   대소문자에 대한 규약을 정의해야 할 수 있다. (그러나, 다른 모든
   당사자가 생산 당사자가 발행한 값을 독립적으로 생산된 값과 비교하려
   시도하지 않고 그대로 소비하는 경우, 생산자가 사용한 대소문자는 중요하지
   않다.)

### 8. 구현 요구사항

   이 섹션은 이 명세의 어떤 알고리즘과 기능이 구현 필수인지를 정의한다.
   이 명세를 사용하는 응용프로그램은 자신이 사용하는 구현에 대해 추가
   요구사항을 부과할 수 있다.
 예를 들어, 한 응용프로그램은 암호화된 JWT와
   Nested JWT에 대한 지원을 요구할 수 있고, 다른 응용프로그램은 P-256
   곡선과 SHA-256 해시 알고리즘을 사용하는 Elliptic Curve Digital
   Signature Algorithm (ECDSA)("ES256")으로 JWT에 서명하는 것에 대한
   지원을 요구할 수 있다.

   JSON Web Algorithms [JWA]에 지정된 서명 및 MAC 알고리즘 중, 준수하는
   JWT 구현은 HMAC SHA-256 ("HS256")과 "none"만 반드시 구현해야 한다
   (MUST).
 구현이 SHA-256 해시 알고리즘을 사용하는 RSASSA-PKCS1-v1_5
   ("RS256")와 P-256 곡선 및 SHA-256 해시 알고리즘을 사용하는 ECDSA
   ("ES256")도 지원할 것을 권장한다(RECOMMENDED).
 다른 알고리즘과 키
   크기에 대한 지원은 선택적(OPTIONAL)이다.

   암호화된 JWT에 대한 지원은 선택적(OPTIONAL)이다.
 구현이 암호화 기능을
   제공하는 경우, [JWA]에 지정된 암호화 알고리즘 중, 2048비트 키를 사용하는
   RSAES-PKCS1-v1_5 ("RSA1_5"), 128비트 및 256비트 키를 사용하는 AES Key
   Wrap ("A128KW" 및 "A256KW"), AES-CBC와 HMAC SHA-2를 사용하는 복합 인증
   암호화 알고리즘 ("A128CBC-HS256" 및 "A256CBC-HS512")만 준수하는 구현에
   의해 반드시 구현되어야 한다(MUST).
 구현이 Content Encryption Key를
   래핑하는 데 사용되는 키에 합의하기 위한 Elliptic Curve Diffie-Hellman
   Ephemeral Static (ECDH-ES) ("ECDH-ES+A128KW" 및 "ECDH-ES+A256KW")와
   128비트 및 256비트 키를 사용하는 Galois/Counter Mode (GCM)의 AES
   ("A128GCM" 및 "A256GCM")도 지원할 것을 권장한다(RECOMMENDED).
 다른
   알고리즘과 키 크기에 대한 지원은 선택적(OPTIONAL)이다.

   Nested JWT에 대한 지원은 선택적(OPTIONAL)이다.

### 9. 콘텐츠가 JWT임을 선언하기 위한 URI

   이 명세는 참조되는 콘텐츠가 JWT임을 나타내기 위해, 미디어 타입이 아닌
   URI를 사용하여 콘텐츠 타입을 선언하는 응용프로그램이 사용할 수 있는
   URN "urn:ietf:params:oauth:token-type:jwt"를 등록한다.

### 10. IANA 고려사항

#### 10.1. JSON Web Token Claims 레지스트리

   이 섹션은 JWT 클레임 이름을 위한 IANA "JSON Web Token Claims"
   레지스트리를 설정한다.
 레지스트리는 클레임 이름과 이를 정의하는 명세에
   대한 참조를 기록한다.
 이 섹션은 섹션 4.1에 정의된 클레임 이름들을
   등록한다.

   값은 jwt-reg-review@ietf.org 메일링 리스트에서 1명 이상의 Designated
   Expert의 조언에 따라 3주간의 검토 기간 후 Specification Required
   [RFC5226] 기준으로 등록된다.
 그러나, 값 할당 전에 출판이 요구되지
   않도록, Designated Expert(s)는 값이 할당된 후에 사용될 명세가 출판될
   것이라는 확신이 있으면 등록을 승인할 수 있다(MAY).

   검토를 위해 메일링 리스트로 보내는 등록 요청에는 적절한 제목을
   사용해야 한다 (예: "Request to register claim: example").

   검토 기간 내에, Designated Expert(s)는 등록 요청을 승인 또는 거부하고,
   이 결정을 검토 리스트와 IANA에 통보한다.
 거부에는 설명과 가능한 경우
   제안사항이 포함되어야 한다. 21일이 넘도록 결정되지 않은 등록 요청은
   해결을 위해 IESG의 주의를 환기시킬 수 있다 (iesg@ietf.org 메일링
   리스트 사용).

   Designated Expert(s)의 기준에는 제안된 등록이 기존 기능을 중복하는지
   여부, 일반적으로 적용 가능한지 또는 단일 응용프로그램에만 유용한지
   여부의 결정, 등록 설명의 명확성에 대한 평가가 포함되어야 한다.

   IANA는 Designated Expert(s)로부터의 등록 업데이트만 수용해야 하며, 모든
   등록 요청을 검토 메일링 리스트로 보내야 한다.

   다양한 응용프로그램 관점을 대표하여 폭넓게 정보에 기반한 검토를 할 수
   있도록, 여러 명의 Designated Expert를 임명할 것을 권장한다.
 등록
   결정이 특정 Expert에게 이해 충돌을 야기하는 것으로 인식될 수 있는
   경우에는, 해당 Expert는 다른 Expert(s)의 판단에 따라야 한다.

##### 10.1.1. 등록 템플릿

   Claim Name:
      요청된 클레임 이름 (예: "example").
 클레임 이름은 8자 이하가
      바람직하기 때문에, 클레임에 대한 짧은 이름을 요청할 것을 권장한다.
      이 이름은 대소문자를 구분한다. "e"와 같은 1자 이름과 "example"과
      같은 긴 이름 모두 인정 가능하다.

   Claim Description:
      클레임에 대한 간단한 설명 (예: "Example description").

   Change Controller:
      표준 트랙 RFC의 경우, "IESG"로 기술한다.
 그 외의 경우, 책임 있는
      당사자의 이름을 기재한다.
 그 밖의 세부사항 (예: 우편 주소, 이메일
      주소, 홈페이지 URI)도 포함될 수 있다.

   Specification Document(s):
      클레임 의미론을 지정하는 문서에 대한 참조로, 클레임 값의 구문과
      의미론의 정밀한 정의에 대한 참조를 포함한다.

##### 10.1.2. 초기 레지스트리 내용

   o  Claim Name: "iss"
   o  Claim Description: Issuer
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.1

   o  Claim Name: "sub"
   o  Claim Description: Subject
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.2

   o  Claim Name: "aud"
   o  Claim Description: Audience
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.3

   o  Claim Name: "exp"
   o  Claim Description: Expiration Time
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.4

   o  Claim Name: "nbf"
   o  Claim Description: Not Before
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.5

   o  Claim Name: "iat"
   o  Claim Description: Issued At
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.6

   o  Claim Name: "jti"
   o  Claim Description: JWT ID
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.7

#### 10.2. urn:ietf:params:oauth:token-type:jwt 서브 네임스페이스 등록

   이 섹션은 콘텐츠가 JWT임을 나타내는 데 사용될 수 있는, "An IETF URN
   Sub-Namespace for OAuth" [RFC6755]에 의해 설정된 IANA "OAuth URI"
   레지스트리에 값 "token-type:jwt"를 등록한다.

##### 10.2.1. 레지스트리 내용

   o  URN: urn:ietf:params:oauth:token-type:jwt
   o  Common Name: JSON Web Token (JWT) Token Type
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519

#### 10.3. 미디어 타입 등록

   이 섹션은 콘텐츠가 JWT임을 나타내는 데 사용될 수 있는, RFC 6838
   [RFC6838]에 설명된 방식으로 "Media Types" 레지스트리
   [IANA.MediaTypes]에 "application/jwt" 미디어 타입 [RFC2046]을
   등록한다.

##### 10.3.1. 레지스트리 내용

   o  Type name: application
   o  Subtype name: jwt
   o  Required parameters: n/a
   o  Optional parameters: n/a
   o  Encoding considerations: 8bit; JWT 값은 마침표('.') 문자로 구분된
      일련의 base64url 인코딩된 값(일부는 빈 문자열일 수 있음)으로
      인코딩된다.
   o  Security considerations: RFC 7519의 보안 고려사항 섹션 참조
   o  Interoperability considerations: n/a
   o  Published specification: RFC 7519
   o  Applications that use this media type: OpenID Connect, Mozilla
      Persona, Salesforce, Google, Android, Windows Azure, Amazon Web
      Services 및 기타 다수
   o  Fragment identifier considerations: n/a
   o  Additional information:

        Magic number(s): n/a
        File extension(s): n/a
        Macintosh file type code(s): n/a

   o  Person & email address to contact for further information:
      Michael B. Jones, mbj@microsoft.com
   o  Intended usage: COMMON
   o  Restrictions on usage: none
   o  Author: Michael B. Jones, mbj@microsoft.com
   o  Change controller: IESG
   o  Provisional registration? No

#### 10.4. 헤더 파라미터 이름 등록

   이 섹션은 섹션 5.3에 따라 JWE에서 헤더 파라미터로 복제되는 클레임에
   사용하기 위해, [JWS]에 의해 설정된 IANA "JSON Web Signature and
   Encryption Header Parameters" 레지스트리에 섹션 4.1에 정의된 특정
   클레임 이름들을 등록한다.

##### 10.4.1. 레지스트리 내용

   o  Header Parameter Name: "iss"
   o  Header Parameter Description: Issuer
   o  Header Parameter Usage Location(s): JWE
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.1

   o  Header Parameter Name: "sub"
   o  Header Parameter Description: Subject
   o  Header Parameter Usage Location(s): JWE
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.2

   o  Header Parameter Name: "aud"
   o  Header Parameter Description: Audience
   o  Header Parameter Usage Location(s): JWE
   o  Change Controller: IESG
   o  Specification Document(s): RFC 7519의 섹션 4.1.3

### 11. 보안 고려사항

   모든 암호화 응용프로그램에 관련된 모든 보안 문제는 JWT/JWS/JWE/JWK
   에이전트에 의해 해결되어야 한다.
 이러한 문제 중에는 사용자의 비대칭
   개인키 및 대칭 비밀키의 보호와 다양한 공격에 대한 대응책 적용이
   포함된다.

#### 11.1. 신뢰 결정

   JWT의 내용이 암호학적으로 보안되고 신뢰 결정에 필요한 컨텍스트에
   바인딩되지 않는 한, JWT의 내용은 신뢰 결정에서 의존할 수 없다.
   특히, JWT에 서명 및/또는 암호화하는 데 사용되는 키는 일반적으로
   JWT의 발급자로 식별된 당사자의 통제 하에 있음이 검증 가능해야 한다.

#### 11.2. 서명 및 암호화 순서

   구문적으로 Nested JWT에 대한 서명 및 암호화 연산은 어떤 순서로든
   적용될 수 있지만, 서명과 암호화가 모두 필요한 경우, 일반적으로
   생산자는 메시지에 서명한 다음 그 결과를 암호화해야 한다(서명을
   암호화하는 것).
 이는 서명이 제거되어 단지 암호화된 메시지만 남는
   공격을 방지할 뿐만 아니라, 서명자에 대한 프라이버시를 제공한다.
   더욱이, 암호화된 텍스트에 대한 서명은 많은 관할권에서 유효한 것으로
   간주되지 않는다.
 서명 및 암호화 연산의 순서와 관련된 보안 문제에 대한
   잠재적 우려는 기저의 JWS 및 JWE 명세에 의해 이미 해결되어 있음을
   유의하라.
 특히, JWE는 인증된 암호화 알고리즘의 사용만 지원하기 때문에,
   많은 컨텍스트에서 적용되는 암호화 후 서명의 잠재적 필요성에 대한 암호학적
   우려는 이 명세에는 적용되지 않는다.

### 12. 프라이버시 고려사항

   JWT는 프라이버시에 민감한 정보를 포함할 수 있다.
 이 경우, 이 정보가
   의도하지 않은 당사자에게 공개되는 것을 방지하기 위한 조치가 취해져야
   한다(MUST).
 이를 달성하는 한 가지 방법은 암호화된 JWT를 사용하고
   수신자를 인증하는 것이다.
 다른 방법은 암호화되지 않은 프라이버시에
   민감한 정보를 포함하는 JWT가 엔드포인트 인증을 지원하는 암호화를
   활용하는 프로토콜(예: Transport Layer Security (TLS))을 통해서만
   전송되도록 보장하는 것이다.
 JWT에서 프라이버시에 민감한 정보를 생략하는
   것이 프라이버시 문제를 최소화하는 가장 간단한 방법이다.

### 13. 참조

#### 13.1. 규범적 참조

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

#### 13.2. 정보적 참조

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

### 부록 A. JWT 예시

#### A.1. 암호화된 JWT 예시

   이 예시는 섹션 3.1에서 사용된 것과 동일한 클레임을 RSAES-PKCS1-v1_5와
   AES_128_CBC_HMAC_SHA_256을 사용하여 수신자에게 암호화한다.

   다음 JOSE 헤더 예시는 다음을 선언한다:

   o  Content Encryption Key가 RSAES-PKCS1-v1_5 알고리즘을 사용하여
      수신자에게 암호화되어 JWE Encrypted Key를 생성한다.
   o  인증된 암호화가 AES_128_CBC_HMAC_SHA_256 알고리즘을 사용하여
      평문에 대해 수행되어 JWE Ciphertext와 JWE Authentication Tag를
      생성한다.

```
{"alg":"RSA1_5","enc":"A128CBC-HS256"}
```

   섹션 3.1의 JWT Claims Set의 UTF-8 표현의 옥텟을 평문 값으로 사용하는
   것 외에, 이 JWT의 계산은 [JWE]의 부록 A.2에서의 JWE 계산과 사용되는
   키를 포함하여 동일하다.

   다음은 완전한 JWT의 결과이다 (표시 목적으로만 줄 바꿈 포함):

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

   이 예시는 JWT가 JWE 또는 JWS의 페이로드로 사용되어 Nested JWT를 생성하는
   방법을 보여준다.
 이 경우, JWT Claims Set은 먼저 서명된 다음 암호화된다.

   내부 서명된 JWT는 [JWS]의 부록 A.2의 예시와 동일하다.
 따라서 그 계산은
   여기서 반복하지 않는다.
 이 예시는 그 후 이 내부 JWT를 RSAES-PKCS1-v1_5와
   AES_128_CBC_HMAC_SHA_256을 사용하여 수신자에게 암호화한다.

   다음 JOSE 헤더 예시는 다음을 선언한다:

   o  Content Encryption Key가 RSAES-PKCS1-v1_5 알고리즘을 사용하여
      수신자에게 암호화되어 JWE Encrypted Key를 생성한다.
   o  인증된 암호화가 AES_128_CBC_HMAC_SHA_256 알고리즘을 사용하여
      평문에 대해 수행되어 JWE Ciphertext와 JWE Authentication Tag를
      생성한다.
   o  평문 자체가 JWT이다.

```
{"alg":"RSA1_5","enc":"A128CBC-HS256","cty":"JWT"}
```

   JOSE 헤더의 UTF-8 표현의 옥텟을 Base64url 인코딩하면 다음의 인코딩된
   JOSE 헤더 값이 생성된다:

```
eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY3R5IjoiSldU
In0
```

   이 JWT의 계산은 다른 JOSE 헤더, 평문, JWE Initialization Vector 및
   Content Encryption Key 값이 사용된다는 것을 제외하면, [JWE]의 부록
   A.2에서의 JWE 계산과 동일하다. (사용되는 RSA 키는 동일하다.)

   평문은 [JWS]의 부록 A.2.1 끝에 있는 JWT의 ASCII 표현의 옥텟이며
   (모든 공백과 줄 바꿈이 제거됨), 458 옥텟의 시퀀스이다.

   사용되는 JWE Initialization Vector 값은 (JSON 배열 표기법 사용):

```
[82, 101, 100, 109, 111, 110, 100, 32, 87, 65, 32, 57, 56, 48, 53, 50]
```

   사용되는 Content Encryption Key 값 (base64url 인코딩)은:

```
GawgguFyGrWKav7AX4VKUg
```

   다음은 완전한 Nested JWT의 결과이다 (표시 목적으로만 줄 바꿈 포함):

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

   Security Assertion Markup Language (SAML) 2.0 [OASIS.saml-core-2.0-os]은
   JWT가 지원하는 것보다 더 큰 표현력과 더 많은 보안 옵션을 가진 보안
   토큰을 생성하기 위한 표준을 제공한다.
 그러나, 이 유연성과 표현력의
   비용은 크기와 복잡성 모두에 있다.
 SAML의 XML [W3C.CR-xml11-20060816]
   및 XML Digital Signature (DSIG) [RFC3275] 사용은 SAML Assertion의 크기에
   기여하며, XML 및 특히 XML Canonicalization [W3C.REC-xml-c14n-20010315]의
   사용은 복잡성에 기여한다.

   JWT는 HTTP 헤더와 URI의 쿼리 인수에 들어갈 수 있을 만큼 작은 간단한
   보안 토큰 형식을 제공하도록 의도되었다.
 SAML보다 훨씬 간단한 토큰
   모델을 지원하고 JSON [RFC7159] 객체 인코딩 구문을 사용함으로써 이를
   달성한다.
 또한 XML DSIG보다 더 작은 (그리고 덜 유연한) 형식을 사용하여
   Message Authentication Code (MAC) 및 디지털 서명으로 토큰 보안을
   지원한다.

   따라서, JWT가 SAML Assertion이 수행하는 일부 작업을 할 수 있지만, JWT는
   SAML Assertion의 완전한 대체물로 의도된 것이 아니라, 구현의 용이성이나
   컴팩트함이 고려 사항인 경우에 사용할 토큰 형식으로 의도된 것이다.

   SAML Assertion은 항상 엔티티가 주체에 대해 하는 진술이다.
 JWT도 종종
   같은 방식으로 사용되며, 진술을 하는 엔티티는 "iss" (issuer) 클레임으로,
   주체는 "sub" (subject) 클레임으로 표현된다.
 그러나, 이러한 클레임이
   선택적이므로, JWT 형식의 다른 사용도 허용된다.

### 부록 C. JWT와 Simple Web Token (SWT)의 관계

   JWT와 SWT [SWT] 모두, 핵심적으로 응용프로그램 간에 클레임 집합을
   통신할 수 있게 한다.
 SWT의 경우, 클레임 이름과 클레임 값이 모두
   문자열이다.
 JWT의 경우, 클레임 이름은 문자열이지만, 클레임 값은 어떤
   JSON 타입이든 될 수 있다.
 두 토큰 타입 모두 콘텐츠의 암호화 보호를
   제공한다: SWT는 HMAC SHA-256으로, JWT는 서명, MAC 및 암호화 알고리즘을
   포함하는 알고리즘 선택으로 보호한다.

### 감사의 글

   저자들은 JWT의 설계가 SWT [SWT]의 설계와 단순성, 그리고 Dick Hardt가
   OpenID 커뮤니티 내에서 논의한 JSON 토큰에 대한 아이디어에 의해 의도적으로
   영향을 받았음을 인정한다.

   JSON 콘텐츠에 서명하기 위한 솔루션은 이전에 Magic Signatures
   [MagicSignatures], JSON Simple Sign [JSS], Canvas Applications
   [CanvasApp]에 의해 탐구되었으며, 이 모두가 이 문서에 영향을 미쳤다.

   이 명세는 수십 명의 활동적이고 헌신적인 참여자를 포함하는 OAuth 워킹
   그룹의 작업이다.
 특히, 다음 개인들이 이 명세에 영향을 미친 아이디어,
   피드백, 문구를 기여했다:

   Dirk Balfanz, Richard Barnes, Brian Campbell, Alissa Cooper, Breno
   de Medeiros, Stephen Farrell, Yaron Y. Goland, Dick Hardt, Joe
   Hildebrand, Jeff Hodges, Edmund Jay, Warren Kumari, Ben Laurie,
   Barry Leiba, Ted Lemon, James Manger, Prateek Mishra, Kathleen
   Moriarty, Tony Nadalin, Axel Nennker, John Panzer, Emmanuel Raviart,
   David Recordon, Eric Rescorla, Jim Schaad, Paul Tarjan, Hannes
   Tschofenig, Sean Turner, Tom Yu.

   Hannes Tschofenig과 Derek Atkins는 OAuth 워킹 그룹의 의장을 맡았으며,
   Sean Turner, Stephen Farrell, Kathleen Moriarty는 이 명세 작성 기간
   동안 Security Area Director로 활동했다.

### 저자 주소

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

---

Internet Engineering Task Force (IETF)                        Y. Sheffer
Request for Comments: 8725                                        Intuit
BCP: 225                                                        D. Hardt
Updates: 7519
Category: Best Current Practice                                 M. Jones
ISSN: 2070-1721                                                Microsoft
                                                           February 2020


                 JSON Web Token 현행 모범 사례

#### Abstract

   JSON 웹 토큰(JWT)은 서명 및/또는 암호화할 수 있는 클레임 집합을
   포함하는 URL 안전 JSON 기반 보안 토큰입니다.
 JWT는 디지털 신원
   영역과 기타 애플리케이션 영역 모두에서 다수의 프로토콜과 애플리케이션에서
   간단한 보안 토큰 형식으로 널리 사용되고 배포되고 있습니다.
 이 현행
   모범 사례 문서는 RFC 7519를 업데이트하여 JWT의 안전한 구현과 배포를
   이끄는 실행 가능한 지침을 제공합니다.

이 메모의 상태

   이 메모는 인터넷 현행 모범 사례를 문서화합니다.

   이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물입니다.
 이것은
   IETF 커뮤니티의 합의를 나타냅니다.
 공개 검토를 받았으며 인터넷
   엔지니어링 운영 그룹(IESG)에 의해 출판이 승인되었습니다.
 BCP에 대한
   추가 정보는 RFC 7841의 섹션 2에서 확인할 수 있습니다.

   이 문서의 현재 상태, 정오표 및 피드백 제공 방법에 관한 정보는
   https://www.rfc-editor.org/info/rfc8725 에서 얻을 수 있습니다.

저작권 고지

   Copyright (c) 2020 IETF Trust and the persons identified as the
   document authors.  All rights reserved.

   이 문서는 BCP 78과 이 문서의 발행일에 유효한 IETF 문서에 관한
   IETF Trust의 법적 조항(https://trustee.ietf.org/license-info)의
   적용을 받습니다.
 이 문서들은 이 문서에 관한 귀하의 권리와 제한을
   설명하므로 주의 깊게 검토하시기 바랍니다.
 이 문서에서 추출된 코드
   구성 요소는 Trust Legal Provisions의 섹션 4.e에 설명된 대로
   Simplified BSD License 텍스트를 포함해야 하며, Simplified BSD
   License에 설명된 대로 보증 없이 제공됩니다.

목차

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
   감사의 말
   저자 주소

### 1. 소개

   JSON 웹 토큰, 즉 JWT [RFC7519]는 서명 및/또는 암호화할 수 있는
   클레임 집합을 포함하는 URL 안전 JSON 기반 보안 토큰입니다.
 JWT
   명세는 보안 관련 정보를 보호하기 쉬운 하나의 위치에 캡슐화하고,
   널리 사용 가능한 도구를 사용하여 쉽게 구현할 수 있기 때문에 빠른
   채택을 이루었습니다.
 JWT가 일반적으로 사용되는 하나의 애플리케이션
   영역은 디지털 신원 정보를 표현하는 것으로, OpenID Connect ID
   Token [OpenID.Core]과 OAuth 2.0 [RFC6749] 액세스 토큰 및 리프레시
   토큰이 이에 해당하며, 세부 사항은 배포에 따라 다릅니다.

   JWT 명세가 발행된 이후로 구현과 배포에 대한 여러 널리 알려진 공격이
   있었습니다.
 이러한 공격은 불충분하게 명세된 보안 메커니즘과 불완전한
   구현 및 애플리케이션의 잘못된 사용의 결과입니다.

   이 문서의 목표는 JWT의 안전한 구현과 배포를 촉진하는 것입니다.
 이
   문서의 많은 권장사항은 JSON Web Signature(JWS) [RFC7515], JSON Web
   Encryption(JWE) [RFC7516], JSON Web Algorithms(JWA) [RFC7518]에
   의해 정의된 JWT의 기반이 되는 암호화 메커니즘의 구현 및 사용에
   관한 것입니다.
 나머지는 JWT 클레임 자체의 사용에 관한 것입니다.

   이것은 대다수의 구현 및 배포 시나리오에서 JWT 사용에 대한 최소한의
   권장사항으로 의도되었습니다.
 이 문서를 참조하는 다른 명세는 특정
   상황에 따라 형식의 하나 이상의 측면에 관하여 더 엄격한 요구사항을
   가질 수 있습니다; 그러한 경우 구현자는 해당 더 엄격한 요구사항을
   준수하는 것이 좋습니다.
 또한 이 문서는 하한선을 제공하는 것이지
   상한선을 제공하는 것이 아니므로, 더 강력한 옵션은 항상 허용됩니다
   (예: 암호화 강도 대 계산 부하의 중요성에 대한 서로 다른 평가에
   따라).

   다양한 알고리즘의 강도와 실행 가능한 공격에 대한 커뮤니티 지식은
   빠르게 변할 수 있으며, 경험에 따르면 보안에 관한 현행 모범 사례(BCP)
   문서는 특정 시점의 진술입니다.
 독자는 이 문서에 적용되는 정오표나
   업데이트를 찾아보는 것이 좋습니다.

#### 1.1. 대상 독자

   이 문서의 의도된 독자는 다음과 같습니다:

   *  JWT 라이브러리(그리고 해당 라이브러리가 사용하는 JWS 및 JWE
      라이브러리)의 구현자,

   *  그러한 라이브러리를 사용하는 코드의 구현자(일부 메커니즘이
      라이브러리에서 제공되지 않을 수 있거나, 제공될 때까지의 범위에서),
      그리고

   *  IETF 내외부에서 JWT에 의존하는 명세의 개발자.

#### 1.2. 이 문서에서 사용된 규약

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY",
   "OPTIONAL"이라는 키워드는 여기에 표시된 것처럼 모두 대문자로
   나타날 때, 그리고 오직 그때만 BCP 14 [RFC2119] [RFC8174]에 설명된
   대로 해석되어야 합니다.

### 2. 위협 및 취약점

   이 섹션은 JWT 구현 및 배포에서 알려진 그리고 가능한 몇 가지 문제를
   나열합니다.
 각 문제 설명 뒤에는 해당 문제에 대한 하나 이상의 완화
   방안에 대한 참조가 따릅니다.

#### 2.1. 취약한 서명 및 불충분한 서명 검증

   서명된 JSON 웹 토큰은 암호화 민첩성을 용이하게 하기 위해 "alg"
   헤더 매개변수 형태로 서명 알고리즘의 명시적 표시를 전달합니다.
   이것은 일부 라이브러리와 애플리케이션의 설계 결함과 결합되어 여러
   공격을 야기했습니다:

   *  공격자가 알고리즘을 "none"으로 변경할 수 있으며, 일부 라이브러리는
      이 값을 신뢰하고 어떠한 서명도 확인하지 않고 JWT를 "검증"합니다.

   *  "RS256"(RSA, 2048비트) 매개변수 값이 "HS256"(HMAC, SHA-256)으로
      변경될 수 있으며, 일부 라이브러리는 HMAC-SHA256을 사용하고 RSA
      공개키를 HMAC 공유 비밀로 사용하여 서명을 검증하려고 합니다
      ([McLean] 및 [CVE-2015-9235] 참조).

   완화 방안에 대해서는 섹션 3.1 및 3.2를 참조하십시오.

#### 2.2. 취약한 대칭키

   또한, 일부 애플리케이션은 토큰에 서명하기 위해 "HS256"과 같은
   키 기반 메시지 인증 코드(MAC) 알고리즘을 사용하지만, 불충분한
   엔트로피를 가진 약한 대칭키(예: 인간이 기억할 수 있는 비밀번호)를
   제공합니다.
 이러한 키는 공격자가 그러한 토큰을 확보하면 오프라인
   무차별 대입 또는 사전 공격에 취약합니다 [Langkemper].

   완화 방안에 대해서는 섹션 3.5를 참조하십시오.

#### 2.3. 암호화와 서명의 잘못된 조합

   JWE 암호화된 JWT를 복호화하여 JWS 서명된 객체를 얻는 일부 라이브러리는
   내부 서명을 항상 검증하지는 않습니다.

   완화 방안에 대해서는 섹션 3.3을 참조하십시오.

#### 2.4. 암호문 길이 분석을 통한 평문 유출

   많은 암호화 알고리즘은 평문의 길이에 대한 정보를 유출하며, 알고리즘과
   운영 모드에 따라 유출량이 다릅니다.
 이 문제는 평문이 초기에 압축될
   때 악화되는데, 압축된 평문의 길이와 따라서 암호문의 길이가 원래
   평문의 길이뿐만 아니라 그 내용에도 의존하기 때문입니다.
 압축 공격은
   공격자가 제어하는 데이터가 비밀 데이터와 동일한 압축 공간에 있을 때
   특히 강력하며, 이는 HTTPS에 대한 일부 공격의 경우에 해당합니다.

   압축과 암호화에 대한 일반적인 배경은 [Kelsey]를, HTTP 쿠키에 대한
   공격의 구체적인 예는 [Alawatugoda]를 참조하십시오.

   완화 방안에 대해서는 섹션 3.6을 참조하십시오.

#### 2.5. 안전하지 않은 타원 곡선 암호화 사용

   [Sanso]에 따르면, 여러 Javascript Object Signing and Encryption
   (JOSE) 라이브러리가 타원 곡선 키 합의("ECDH-ES" 알고리즘)를 수행할
   때 입력을 올바르게 검증하지 못합니다.
 유효하지 않은 곡선 점을
   사용하는 자신이 선택한 JWE를 보내고 유효하지 않은 곡선 점으로
   복호화한 결과인 평문 출력을 관찰할 수 있는 공격자는 이 취약점을
   사용하여 수신자의 개인키를 복구할 수 있습니다.

   완화 방안에 대해서는 섹션 3.4를 참조하십시오.

#### 2.6. JSON 인코딩의 다양성

   폐기된 [RFC7159]와 같은 이전 버전의 JSON 형식은 UTF-8, UTF-16,
   UTF-32의 여러 다른 문자 인코딩을 허용했습니다.
 최신 표준 [RFC8259]은
   "폐쇄 생태계" 내의 내부 사용을 제외하고 UTF-8만 허용하므로 이제는
   그렇지 않습니다.
 오래된 구현체와 폐쇄 환경 내에서 사용되는 구현체가
   비표준 인코딩을 생성할 수 있는 이러한 모호성은 JWT가 수신자에 의해
   잘못 해석되는 결과를 초래할 수 있습니다.
 이는 차례로 악의적인
   발신자가 수신자의 검증 확인을 우회하는 데 사용될 수 있습니다.

   완화 방안에 대해서는 섹션 3.7을 참조하십시오.

#### 2.7. 대체 공격

   하나의 수신자에게 의도된 JWT가 해당 수신자에게 주어지고, 그 수신자가
   해당 JWT가 의도되지 않은 다른 수신자에게 이를 사용하려고 시도하는
   공격이 있습니다.
 예를 들어, OAuth 2.0 [RFC6749] 액세스 토큰이
   의도된 OAuth 2.0 보호 리소스에 합법적으로 제시된 경우, 해당 보호
   리소스가 액세스 토큰이 의도되지 않은 다른 보호 리소스에 동일한
   액세스 토큰을 제시하여 접근을 얻으려고 시도할 수 있습니다.
 이러한
   상황이 포착되지 않으면, 공격자가 접근 권한이 없는 리소스에 접근하게
   되는 결과를 초래할 수 있습니다.

   완화 방안에 대해서는 섹션 3.8 및 3.9를 참조하십시오.

#### 2.8. 교차 JWT 혼동

   JWT가 다양한 애플리케이션 영역에서 더 많은 서로 다른 프로토콜에
   의해 사용됨에 따라, 하나의 목적으로 발행된 JWT 토큰이 전용되어
   다른 목적으로 사용되는 경우를 방지하는 것이 점점 더 중요해집니다.
   이것은 특정 유형의 대체 공격임에 유의하십시오.
 JWT가 다른 종류의
   JWT와 혼동될 수 있는 애플리케이션 컨텍스트에서 사용될 수 있다면,
   이러한 대체 공격을 방지하기 위한 완화 방안이 사용되어야 합니다(MUST).

   완화 방안에 대해서는 섹션 3.8, 3.9, 3.11, 3.12를 참조하십시오.

#### 2.9. 서버에 대한 간접 공격

   다양한 JWT 클레임이 수신자에 의해 데이터베이스 및 경량 디렉터리
   접근 프로토콜(LDAP) 검색과 같은 조회 작업을 수행하는 데 사용됩니다.
   그 외에도 서버에 의해 유사하게 조회되는 URL을 포함하는 것들이
   있습니다.
 이러한 클레임 중 어느 것이든 공격자가 주입 공격이나
   서버 측 요청 위조(SSRF) 공격을 위한 벡터로 사용할 수 있습니다.

   완화 방안에 대해서는 섹션 3.10을 참조하십시오.

### 3. 모범 사례

   아래에 나열된 모범 사례는 실무자가 앞 섹션에 나열된 위협을 완화하기
   위해 적용해야 합니다.

#### 3.1. 알고리즘 검증 수행

   라이브러리는 호출자가 지원하는 알고리즘 집합을 지정할 수 있도록
   해야 하며(MUST), 암호화 작업을 수행할 때 다른 알고리즘을 사용해서는
   안 됩니다(MUST NOT).
 라이브러리는 "alg" 또는 "enc" 헤더가 암호화
   작업에 사용되는 것과 동일한 알고리즘을 지정하는지 확인해야
   합니다(MUST).
 또한 각 키는 정확히 하나의 알고리즘과 함께
   사용되어야 하며(MUST), 이것은 암호화 작업이 수행될 때
   확인되어야 합니다(MUST).

#### 3.2. 적절한 알고리즘 사용

   [RFC7515]의 섹션 5.2에서 말하듯이, "주어진 컨텍스트에서 어떤
   알고리즘을 사용할 수 있는지는 애플리케이션의 결정입니다.
 JWS가
   성공적으로 검증될 수 있더라도, JWS에 사용된 알고리즘이 애플리케이션에
   수용 가능하지 않다면, 해당 JWS를 유효하지 않은 것으로 간주해야
   합니다(SHOULD)."

   따라서 애플리케이션은 애플리케이션의 보안 요구사항을 충족하는
   암호학적으로 현재인 알고리즘만 사용을 허용해야 합니다(MUST).
 이
   집합은 새로운 알고리즘이 도입되고 발견된 암호학적 약점으로 인해
   기존 알고리즘이 더 이상 사용되지 않게 됨에 따라 시간이 지남에 따라
   변할 것입니다.
 따라서 애플리케이션은 암호화 민첩성을 가능하게 하도록
   설계되어야 합니다(MUST).

   그렇긴 하지만, JWT가 암호학적으로 현재인 알고리즘을 사용하는 TLS와
   같은 전송 계층에 의해 종단간 암호학적으로 보호되는 경우, JWT에 다른
   암호학적 보호 계층을 적용할 필요가 없을 수 있습니다.
 이러한 경우
   "none" 알고리즘의 사용은 완전히 수용 가능할 수 있습니다. "none"
   알고리즘은 JWT가 다른 수단에 의해 암호학적으로 보호되는 경우에만
   사용해야 합니다. "none"을 사용하는 JWT는 콘텐츠가 선택적으로
   서명되는 애플리케이션 컨텍스트에서 종종 사용됩니다; 그러면 URL 안전
   클레임 표현과 처리가 서명된 경우와 서명되지 않은 경우 모두에서
   동일할 수 있습니다.
 JWT 라이브러리는 호출자가 명시적으로 요청하지
   않는 한 "none"을 사용하는 JWT를 생성해서는 안 됩니다(SHOULD NOT).
   마찬가지로 JWT 라이브러리는 호출자가 명시적으로 요청하지 않는 한
   "none"을 사용하는 JWT를 소비해서는 안 됩니다(SHOULD NOT).

   애플리케이션은 다음과 같은 알고리즘별 권장사항을 따라야
   합니다(SHOULD):

   *  모든 RSA-PKCS1 v1.5 암호화 알고리즘([RFC8017], 섹션 7.2)을
      피하고, RSAES-OAEP([RFC8017], 섹션 7.1)를 선호하십시오.

   *  타원 곡선 디지털 서명 알고리즘(ECDSA) 서명 [ANSI-X962-2005]은
      서명되는 모든 메시지에 대해 고유한 랜덤 값을 필요로 합니다.
      랜덤 값의 몇 비트만이라도 여러 메시지에 걸쳐 예측 가능하다면,
      서명 체계의 보안이 손상될 수 있습니다.
 최악의 경우, 공격자가
      개인키를 복구할 수 있습니다.
 이러한 공격에 대응하기 위해 JWT
      라이브러리는 [RFC6979]에 정의된 결정적 접근 방식을 사용하여
      ECDSA를 구현해야 합니다(SHOULD).
 이 접근 방식은 기존 ECDSA
      검증자와 완전히 호환되므로 새로운 알고리즘 식별자를 요구하지
      않고 구현할 수 있습니다.

#### 3.3. 모든 암호화 작업 검증

   JWT에 사용된 모든 암호화 작업은 검증되어야 하며(MUST), 검증에
   실패하면 전체 JWT가 거부되어야 합니다(MUST).
 이것은 단일 헤더
   매개변수 집합을 가진 JWT에만 해당되는 것이 아니라, 외부와 내부
   작업 모두가 애플리케이션에서 제공한 키와 알고리즘을 사용하여
   검증되어야 하는(MUST) 중첩된 JWT에도 해당됩니다.

#### 3.4. 암호화 입력 검증

   타원 곡선 Diffie-Hellman 키 합의("ECDH-ES")와 같은 일부 암호화
   작업은 유효하지 않은 값을 포함할 수 있는 입력을 받습니다.
 이것은
   지정된 타원 곡선 위에 있지 않은 점이나 기타 유효하지 않은 점(예:
   [Valenta], 섹션 7.1)을 포함합니다.
 JWS/JWE 라이브러리 자체가 이러한
   입력을 사용하기 전에 검증해야 하거나, 검증을 수행하는 기반 암호화
   라이브러리를 사용해야 합니다(또는 둘 다!).

   타원 곡선 Diffie-Hellman Ephemeral Static(ECDH-ES) 임시 공개키(epk)
   입력은 수신자가 선택한 타원 곡선에 따라 검증되어야 합니다.
 NIST
   소수 차수 곡선 P-256, P-384, P-521의 경우, 검증은 "이산 로그
   암호학을 사용한 쌍별 키 설정 체계에 대한 권장사항"
   [nist-sp-800-56a-r3]의 섹션 5.6.2.3.4(ECC 부분 공개키 검증
   루틴)에 따라 수행되어야 합니다(MUST). "X25519" 또는 "X448"
   [RFC8037] 알고리즘이 사용되는 경우, [RFC8037]의 보안 고려사항이
   적용됩니다.

#### 3.5. 암호화 키의 충분한 엔트로피 보장

   [RFC7515]의 섹션 10.1에 있는 키 엔트로피 및 랜덤 값 조언과
   [RFC7518]의 섹션 8.8에 있는 비밀번호 고려사항이 따라야
   합니다(MUST).
 특히 인간이 기억할 수 있는 비밀번호는 "HS256"과 같은
   키 기반 MAC 알고리즘의 키로 직접 사용해서는 안 됩니다(MUST NOT).
   또한 비밀번호는 [RFC7518]의 섹션 4.8에 설명된 대로 콘텐츠 암호화가
   아닌 키 암호화를 수행하는 데에만 사용해야 합니다.
 키 암호화에
   사용되는 경우에도 비밀번호 기반 암호화는 여전히 무차별 대입 공격의
   대상이 됩니다.

#### 3.6. 암호화 입력의 압축 회피

   데이터의 압축은 암호화 전에 수행해서는 안 됩니다(SHOULD NOT).
   그러한 압축된 데이터는 종종 평문에 대한 정보를 드러내기 때문입니다.

#### 3.7. UTF-8 사용

   [RFC7515], [RFC7516], [RFC7519] 모두 헤더 매개변수와 JWT 클레임
   세트에서 사용되는 JSON의 인코딩 및 디코딩에 UTF-8을 사용하도록
   지정합니다.
 이것은 최신 JSON 명세 [RFC8259]와도 일치합니다.
 구현체와
   애플리케이션은 이를 수행해야 하며(MUST), 이러한 목적으로 다른
   유니코드 인코딩을 사용하거나 사용을 허용해서는 안 됩니다(MUST NOT).

#### 3.8. 발행자 및 주체 검증

   JWT에 "iss"(발행자) 클레임이 포함된 경우, 애플리케이션은 JWT의
   암호화 작업에 사용된 암호화 키가 발행자에 속하는지 검증해야
   합니다(MUST).
 그렇지 않은 경우, 애플리케이션은 JWT를 거부해야
   합니다(MUST).

   발행자가 소유한 키를 결정하는 수단은 애플리케이션에 따라
   다릅니다.
 하나의 예로, OpenID Connect [OpenID.Core] 발행자 값은
   "jwks_uri" 값을 포함하는 JSON 메타데이터 문서를 참조하는 "https"
   URL이며, 이 "jwks_uri"는 발행자의 키가 JWK Set [RFC7517]으로
   검색되는 "https" URL입니다.
 동일한 메커니즘이 [RFC8414]에서
   사용됩니다.
 다른 애플리케이션은 키를 발행자에 바인딩하는 다른
   수단을 사용할 수 있습니다.

   마찬가지로, JWT에 "sub"(주체) 클레임이 포함된 경우, 애플리케이션은
   주체 값이 애플리케이션에서 유효한 주체 및/또는 발행자-주체 쌍에
   해당하는지 검증해야 합니다(MUST).
 이것은 발행자가 애플리케이션에
   의해 신뢰되는지 확인하는 것을 포함할 수 있습니다.
 발행자, 주체
   또는 해당 쌍이 유효하지 않은 경우, 애플리케이션은 JWT를 거부해야
   합니다(MUST).

#### 3.9. 대상(Audience) 사용 및 검증

   동일한 발행자가 둘 이상의 신뢰 당사자 또는 애플리케이션에서
   사용하도록 의도된 JWT를 발행할 수 있는 경우, JWT에는 JWT가 의도된
   당사자에 의해 사용되고 있는지 또는 공격자가 의도되지 않은 당사자에게
   대체한 것인지를 결정하는 데 사용할 수 있는 "aud"(대상) 클레임이
   포함되어야 합니다(MUST).

   이러한 경우, 신뢰 당사자 또는 애플리케이션은 대상 값을 검증해야
   하며(MUST), 대상 값이 존재하지 않거나 수신자와 연관되지 않은 경우
   JWT를 거부해야 합니다(MUST).

#### 3.10. 수신된 클레임을 신뢰하지 않음

   "kid"(키 ID) 헤더는 신뢰 당사자 애플리케이션이 키 조회를 수행하는
   데 사용됩니다.
 애플리케이션은 수신된 값을 검증 및/또는 정제하여
   이것이 SQL 또는 LDAP 주입 취약점을 생성하지 않도록 해야 합니다.

   마찬가지로, 임의의 URL을 포함할 수 있는 "jku"(JWK 세트 URL) 또는
   "x5u"(X.509 URL) 헤더를 맹목적으로 따르면 서버 측 요청 위조(SSRF)
   공격이 발생할 수 있습니다.
 애플리케이션은 이러한 공격으로부터
   보호해야 합니다(SHOULD).
 예를 들어, URL을 허용된 위치의 화이트리스트와
   일치시키고 GET 요청에 쿠키가 전송되지 않도록 하는 방법이 있습니다.

#### 3.11. 명시적 타이핑 사용

   때때로 한 종류의 JWT가 다른 종류로 혼동될 수 있습니다.
 특정 종류의
   JWT가 이러한 혼동의 대상이 되는 경우, 해당 JWT에 명시적 JWT 유형
   값을 포함할 수 있으며, 검증 규칙에서 유형 확인을 지정할 수 있습니다.
   이 메커니즘은 이러한 혼동을 방지할 수 있습니다.
 명시적 JWT 타이핑은
   "typ" 헤더 매개변수를 사용하여 수행됩니다.
 예를 들어, [RFC8417]
   명세는 Security Event Token(SET)의 명시적 타이핑을 수행하기 위해
   "application/secevent+jwt" 미디어 유형을 사용합니다.

   [RFC7515]의 섹션 4.1.9에서 "typ"의 정의에 따라, "typ" 값에서
   "application/" 접두사를 생략하는 것이 권장됩니다(RECOMMENDED).
   따라서 예를 들어, SET에 대한 유형을 명시적으로 포함하기 위해
   사용되는 "typ" 값은 "secevent+jwt"여야 합니다(SHOULD).
 명시적
   타이핑이 JWT에 사용될 때, "application/example+jwt" 형식의 미디어
   유형 이름을 사용하는 것이 권장됩니다(RECOMMENDED).
 여기서 "example"은
   특정 종류의 JWT에 대한 식별자로 대체됩니다.

   중첩된 JWT에 명시적 타이핑을 적용할 때, 명시적 유형 값을 포함하는
   "typ" 헤더 매개변수는 중첩된 JWT의 내부 JWT(페이로드가 JWT 클레임
   세트인 JWT)에 존재해야 합니다(MUST).
 일부 경우에는 전체 중첩된 JWT를
   명시적으로 타이핑하기 위해 외부 JWT에도 동일한 "typ" 헤더 매개변수
   값이 존재할 수 있습니다.

   명시적 타이핑의 사용이 기존 종류의 JWT와의 명확한 구분을 달성하지
   못할 수 있음에 유의하십시오.
 기존 종류의 JWT에 대한 검증 규칙이
   "typ" 헤더 매개변수 값을 사용하지 않는 경우가 종종 있기 때문입니다.
   명시적 타이핑은 JWT의 새로운 사용에 권장됩니다(RECOMMENDED).

#### 3.12. 서로 다른 종류의 JWT에 대해 상호 배타적 검증 규칙 사용

   JWT의 각 애플리케이션은 필수 및 선택적 JWT 클레임과 이에 관련된
   검증 규칙을 지정하는 프로필을 정의합니다.
 동일한 발행자가 둘 이상의
   종류의 JWT를 발행할 수 있는 경우, 해당 JWT에 대한 검증 규칙은
   잘못된 종류의 JWT를 거부하도록 상호 배타적으로 작성되어야
   합니다(MUST).
 한 컨텍스트에서 다른 컨텍스트로의 JWT 대체를 방지하기
   위해, 애플리케이션 개발자는 여러 전략을 사용할 수 있습니다:

   *  서로 다른 종류의 JWT에 명시적 타이핑을 사용합니다.
 그러면
      고유한 "typ" 값을 사용하여 서로 다른 종류의 JWT를 구별할 수
      있습니다.

   *  서로 다른 필수 클레임 집합 또는 서로 다른 필수 클레임 값을
      사용합니다.
 그러면 한 종류의 JWT에 대한 검증 규칙이 다른 클레임
      또는 값을 가진 것을 거부합니다.

   *  서로 다른 필수 헤더 매개변수 집합 또는 서로 다른 필수 헤더
      매개변수 값을 사용합니다.
 그러면 한 종류의 JWT에 대한 검증 규칙이
      다른 헤더 매개변수 또는 값을 가진 것을 거부합니다.

   *  서로 다른 종류의 JWT에 서로 다른 키를 사용합니다.
 그러면 한
      종류의 JWT를 검증하는 데 사용되는 키가 다른 종류의 JWT를 검증하는
      데 실패합니다.

   *  동일한 발행자의 서로 다른 JWT 사용에 서로 다른 "aud" 값을
      사용합니다.
 그러면 대상 검증이 부적절한 컨텍스트에 대체된 JWT를
      거부합니다.

   *  서로 다른 종류의 JWT에 서로 다른 발행자를 사용합니다.
 그러면
      고유한 "iss" 값을 사용하여 서로 다른 종류의 JWT를 분리할 수
      있습니다.

   JWT 사용과 애플리케이션의 광범위한 다양성을 고려할 때, 서로 다른
   종류의 JWT를 구별하기 위한 유형, 필수 클레임, 값, 헤더 매개변수,
   키 사용 및 발행자의 최적 조합은 일반적으로 애플리케이션에 따라
   다릅니다.
 섹션 3.11에서 논의된 바와 같이, 새로운 JWT 애플리케이션의
   경우 명시적 타이핑의 사용이 권장됩니다(RECOMMENDED).

### 4. 보안 고려사항

   이 문서 전체가 JSON 웹 토큰을 구현하고 배포할 때의 보안 고려사항에
   관한 것입니다.

### 5. IANA 고려사항

   이 문서에는 IANA 관련 작업이 없습니다.

### 6. 참고 문헌

#### 6.1. 규범적 참고 문헌

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

#### 6.2. 참고적 참고 문헌

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

### 감사의 말

   "ECDH-ES" 유효하지 않은 점 공격을 JWE 및 JWT 구현자의 주의로
   가져온 Antonio Sanso에게 감사드립니다.
 Tim McLean은 RSA/HMAC 혼동
   공격을 발표했습니다.
 명시적 타이핑의 사용을 옹호한 Nat Sakimura에게
   감사드립니다.
 다수의 의견을 준 Neil Madden에게, 그리고 검토를
   해 준 Carsten Bormann, Brian Campbell, Brian Carpenter, Alissa
   Cooper, Roman Danyliw, Ben Kaduk, Mirja Kuhlewind, Barry Leiba,
   Eric Rescorla, Adam Roach, Martin Vigoureux, Eric Vyncke에게
   감사드립니다.

### 저자 주소

   Yaron Sheffer
   Intuit

   Email: yaronf.ietf@gmail.com


   Dick Hardt

   Email: dick.hardt@gmail.com


   Michael B. Jones
   Microsoft

   Email: mbj@microsoft.com
   URI:   https://self-issued.info/
