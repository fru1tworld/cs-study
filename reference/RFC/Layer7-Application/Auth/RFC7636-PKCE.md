Internet Engineering Task Force (IETF)                  N. Sakimura, Ed.
Request for Comments: 7636                     Nomura Research Institute
Category: Standards Track                                     J. Bradley
ISSN: 2070-1721                                            Ping Identity
                                                              N. Agarwal
                                                                  Google
                                                          September 2015

# OAuth 공개 클라이언트에 의한 코드 교환용 증명 키 (Proof Key for Code Exchange by OAuth Public Clients)

## 초록 (Abstract)

인가 코드 그랜트(Authorization Code Grant)를 활용하는 OAuth 2.0 [RFC6749] 공개 클라이언트는 인가 코드 가로채기 공격에 취약하다. 본 명세는 해당 공격에 대해 기술하며, PKCE(Proof Key for Code Exchange, "픽시(pixy)"로 발음)를 사용하여 이 위협을 완화하는 기법을 설명한다.

## 이 메모의 상태 (Status of This Memo)

이 문서는 인터넷 표준 트랙(Internet Standards Track) 문서이다. 이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물이다. 이 문서는 IETF 커뮤니티의 합의를 대표한다. 이 문서는 공개 검토를 받았으며 인터넷 엔지니어링 운영 그룹(IESG)의 출판 승인을 받았다. 인터넷 표준에 관한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있다. 이 문서의 현재 상태, 오류 정보, 피드백 제공 방법에 대한 정보는 http://www.rfc-editor.org/info/rfc7636 에서 확인할 수 있다.

## 저작권 고지 (Copyright Notice)

Copyright (c) 2015 IETF Trust and the persons identified as the document authors. All rights reserved.

이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF 문서에 관한 IETF Trust의 법적 조항(http://trustee.ietf.org/license-info)의 적용을 받는다. 이 문서들은 본 문서에 대한 귀하의 권리와 제한 사항을 기술하므로 주의 깊게 검토해야 한다. 이 문서에서 추출된 코드 구성 요소는 Trust Legal Provisions의 섹션 4.e에 기술된 Simplified BSD License 텍스트를 포함해야 하며, Simplified BSD License에 기술된 대로 보증 없이 제공된다.

## 목차 (Table of Contents)

   1. 서론 ............................................................ 3
      1.1. 프로토콜 흐름 .............................................. 5
   2. 표기 규약 ....................................................... 6
   3. 용어 ............................................................ 7
      3.1. 약어 ....................................................... 7
   4. 프로토콜 ........................................................ 8
      4.1. 클라이언트가 코드 검증자를 생성함 .......................... 8
      4.2. 클라이언트가 코드 챌린지를 생성함 .......................... 8
      4.3. 클라이언트가 인가 요청과 함께 코드 챌린지를 전송함 ........ 9
      4.4. 서버가 코드를 반환함 ....................................... 9
           4.4.1. 오류 응답 ........................................... 9
      4.5. 클라이언트가 인가 코드와 코드 검증자를 토큰 엔드포인트로
           전송함 .................................................... 10
      4.6. 서버가 토큰 반환 전에 code_verifier를 검증함 .............. 10
   5. 호환성 ......................................................... 11
   6. IANA 고려 사항 .................................................. 11
      6.1. OAuth 매개변수 레지스트리 .................................. 11
      6.2. PKCE 코드 챌린지 방법 레지스트리 .......................... 11
           6.2.1. 등록 템플릿 ........................................ 12
           6.2.2. 초기 레지스트리 내용 ................................ 13
   7. 보안 고려 사항 .................................................. 13
      7.1. code_verifier의 엔트로피 .................................. 13
      7.2. 도청자에 대한 보호 ........................................ 13
      7.3. code_challenge에 대한 솔트(salting) ....................... 14
      7.4. OAuth 보안 고려 사항 ...................................... 14
      7.5. TLS 보안 고려 사항 ........................................ 15
   8. 참조 ........................................................... 15
      8.1. 규범적 참조 ............................................... 15
      8.2. 참고적 참조 ............................................... 16
   부록 A. 패딩 없는 Base64url 인코딩 구현에 관한 참고 사항 .......... 17
   부록 B. S256 code_challenge_method에 대한 예제 .................... 17
   감사의 글 ......................................................... 19
   저자 주소 ......................................................... 20

## 1. 서론 (Introduction)

OAuth 2.0 [RFC6749] 공개 클라이언트는 인가 코드 가로채기 공격에 취약하다.

이 공격에서, 공격자는 클라이언트의 운영 체제 내 애플리케이션 간 통신과 같이 TLS(Transport Layer Security)에 의해 보호되지 않는 통신 경로 내에서 인가 엔드포인트로부터 반환된 인가 코드를 가로챈다.

공격자가 인가 코드에 대한 접근 권한을 얻으면, 이를 사용하여 액세스 토큰을 획득할 수 있다.

그림 1은 이 공격을 도식화하여 보여준다. 단계 (1)에서, 스마트폰과 같은 단말 장치에서 실행 중인 네이티브 애플리케이션이 브라우저/운영 체제를 통해 OAuth 2.0 인가 요청을 발행한다. 이 경우 리다이렉션 엔드포인트 URI는 일반적으로 커스텀 URI 스킴을 사용한다. 단계 (1)은 가로챌 수 없는 보안 API를 통해 발생하지만, 고급 공격 시나리오에서는 관찰될 수 있다. 이후 요청은 단계 (2)에서 OAuth 2.0 인가 서버로 전달된다. OAuth는 TLS 사용을 요구하므로, 이 통신은 TLS에 의해 보호되며 가로챌 수 없다. 인가 서버는 단계 (3)에서 인가 코드를 반환한다. 단계 (4)에서, 인가 코드는 단계 (1)에서 제공된 리다이렉션 엔드포인트 URI를 통해 요청자에게 반환된다.

악성 앱이 합법적인 OAuth 2.0 앱 외에 추가로 해당 커스텀 스킴의 핸들러로 자신을 등록하는 것이 가능하다는 점에 유의하라. 그렇게 하면, 악성 앱은 이제 단계 (4)에서 인가 코드를 가로챌 수 있게 된다. 이를 통해 공격자는 각각 단계 (5)와 (6)에서 액세스 토큰을 요청하고 획득할 수 있다.

```
    +~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~+
    | End Device (e.g., Smartphone)  |
    |                                |
    | +-------------+   +----------+ | (6) Access Token  +----------+
    | |Legitimate   |   | Malicious|<--------------------|          |
    | |OAuth 2.0 App|   | App      |-------------------->|          |
    | +-------------+   +----------+ | (5) Authorization |          |
    |        |    ^          ^       |        Grant      |          |
    |        |     \         |       |                   |          |
    |        |      \   (4)  |       |                   |          |
    |    (1) |       \  Authz|       |                   |          |
    |   Authz|        \ Code |       |                   |  Authz   |
    | Request|         \     |       |                   |  Server  |
    |        |          \    |       |                   |          |
    |        |           \   |       |                   |          |
    |        v            \  |       |                   |          |
    | +----------------------------+ |                   |          |
    | |                            | | (3) Authz Code    |          |
    | |     Operating System/      |<--------------------|          |
    | |         Browser            |-------------------->|          |
    | |                            | | (2) Authz Request |          |
    | +----------------------------+ |                   +----------+
    +~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~+
```

                그림 1: 인가 코드 가로채기 공격

이 공격이 동작하기 위해서는 여러 가지 전제 조건이 충족되어야 한다:

1. 공격자가 클라이언트 장치에 악성 애플리케이션을 등록하는 데 성공하고, 다른 애플리케이션에서도 사용하는 커스텀 URI 스킴을 등록한다. 운영 체제는 커스텀 URI 스킴이 여러 애플리케이션에 의해 등록되는 것을 허용해야 한다.

2. OAuth 2.0 인가 코드 그랜트가 사용된다.

3. 공격자가 OAuth 2.0 [RFC6749] "client_id"와 "client_secret"(프로비저닝된 경우)에 접근할 수 있다. 모든 OAuth 2.0 네이티브 앱 클라이언트 인스턴스는 동일한 "client_id"를 사용한다. 클라이언트 바이너리 애플리케이션에 프로비저닝된 시크릿은 기밀로 간주될 수 없다.

4. 다음 조건 중 하나가 충족된다:

   4a. 공격자(설치된 애플리케이션을 통해)가 인가 엔드포인트로부터의 응답만을 관찰할 수 있다. "code_challenge_method" 값이 "plain"인 경우, 이 공격만 완화된다.

   4b. 더 정교한 공격 시나리오에서는 공격자가 인가 엔드포인트에 대한 (응답 외에) 요청도 관찰할 수 있다. 그러나 공격자는 중간자(man in the middle)로 행동할 수는 없다. 이는 운영 체제에서 http 로그 정보가 유출되어 발생했다. 이를 완화하기 위해, "code_challenge_method" 값은 "S256"이나 암호학적으로 안전한 "code_challenge_method" 확장에 의해 정의된 값으로 설정되어야 한다.

이것은 긴 전제 조건 목록이지만, 기술된 공격은 실제 환경에서 관찰되었으며 OAuth 2.0 배포에서 고려되어야 한다. OAuth 2.0 위협 모델([RFC6819]의 섹션 4.4.1)이 완화 기법을 기술하고 있지만, 이 기법들은 클라이언트 인스턴스별 시크릿이나 클라이언트 인스턴스별 리다이렉트 URI에 의존하기 때문에 불행히도 적용할 수 없다.

이 공격을 완화하기 위해, 이 확장은 "코드 검증자(code verifier)"라고 불리는 동적으로 생성된 암호학적 랜덤 키를 활용한다. 모든 인가 요청에 대해 고유한 코드 검증자가 생성되며, 그 변환된 값인 "코드 챌린지(code challenge)"가 인가 코드를 획득하기 위해 인가 서버로 전송된다. 획득된 인가 코드는 이후 "코드 검증자"와 함께 토큰 엔드포인트로 전송되며, 서버는 이를 이전에 수신한 요청 코드와 비교하여 클라이언트에 의한 "코드 검증자"의 소유 증명을 수행할 수 있다. 이것은 완화 기법으로 작동하는데, 공격자가 이 일회성 키를 알지 못하기 때문이며, 이 키는 TLS를 통해 전송되어 가로챌 수 없다.

### 1.1. 프로토콜 흐름 (Protocol Flow)

```
                                                 +-------------------+
                                                 |   Authz Server    |
       +--------+                                | +---------------+ |
       |        |--(A)- Authorization Request ---->|               | |
       |        |       + t(code_verifier), t_m  | | Authorization | |
       |        |                                | |    Endpoint   | |
       |        |<-(B)---- Authorization Code -----|               | |
       |        |                                | +---------------+ |
       | Client |                                |                   |
       |        |                                | +---------------+ |
       |        |--(C)-- Access Token Request ---->|               | |
       |        |          + code_verifier       | |    Token      | |
       |        |                                | |   Endpoint    | |
       |        |<-(D)------ Access Token ---------|               | |
       +--------+                                | +---------------+ |
                                                 +-------------------+

                     그림 2: 추상 프로토콜 흐름
```

본 명세는 그림 2에 추상적 형태로 표시된 바와 같이, OAuth 2.0 인가 요청 및 액세스 토큰 요청에 추가 매개변수를 추가한다.

A. 클라이언트는 "code_verifier"라는 시크릿을 생성하고 기록하며, 변환된 버전인 "t(code_verifier)"("코드 챌린지"라 지칭)를 도출하여, 변환 방법 "t_m"과 함께 OAuth 2.0 인가 요청에 포함하여 전송한다.

B. 인가 엔드포인트는 평소와 같이 응답하되, "t(code_verifier)"와 변환 방법을 기록한다.

C. 클라이언트는 이후 평소와 같이 액세스 토큰 요청에 인가 코드를 포함하여 전송하되, (A)에서 생성한 "code_verifier" 시크릿을 포함한다.

D. 인가 서버는 "code_verifier"를 변환하고 (B)에서의 "t(code_verifier)"와 비교한다. 두 값이 같지 않으면 접근이 거부된다.

(B)에서 인가 코드를 가로채는 공격자는 "code_verifier" 시크릿을 소유하고 있지 않으므로 이를 액세스 토큰으로 교환할 수 없다.

## 2. 표기 규약 (Notational Conventions)

이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL"은 "Key words for use in RFCs to Indicate Requirement Levels" [RFC2119]에 기술된 대로 해석되어야 한다. 이 단어들이 대문자로 표기되지 않고 사용된 경우, 자연어 의미로 해석되어야 한다.

본 명세는 [RFC5234]의 ABNF(Augmented Backus-Naur Form) 표기법을 사용한다.

STRING은 0개 이상의 ASCII [RFC20] 문자 시퀀스를 나타낸다.

OCTETS는 0개 이상의 옥텟 시퀀스를 나타낸다.

ASCII(STRING)은 STRING의 ASCII [RFC20] 표현의 옥텟을 나타내며, 여기서 STRING은 0개 이상의 ASCII 문자 시퀀스이다.

BASE64URL-ENCODE(OCTETS)는 부록 A에 따른 OCTETS의 base64url 인코딩을 나타내며, STRING을 생성한다.

BASE64URL-DECODE(STRING)은 부록 A에 따른 STRING의 base64url 디코딩을 나타내며, 옥텟 시퀀스를 생성한다.

SHA256(OCTETS)는 OCTETS의 SHA2 256비트 해시 [RFC6234]를 나타낸다.

## 3. 용어 (Terminology)

OAuth 2.0 [RFC6749]에 정의된 용어 외에, 본 명세는 다음 용어를 정의한다:

code verifier (코드 검증자)
인가 요청을 토큰 요청과 연관시키는 데 사용되는 암호학적 랜덤 문자열이다.

code challenge (코드 챌린지)
인가 요청에 전송되는, 코드 검증자로부터 도출된 챌린지로, 나중에 대조 검증된다.

code challenge method (코드 챌린지 방법)
코드 챌린지를 도출하는 데 사용된 방법이다.

Base64url Encoding
[RFC4648]의 섹션 5에 정의된 URL 및 파일명에 안전한 문자 집합을 사용하는 Base64 인코딩으로, 모든 후행 '=' 문자가 생략되고([RFC4648]의 섹션 3.2에서 허용), 줄 바꿈, 공백, 기타 추가 문자를 포함하지 않는다. (패딩 없는 base64url 인코딩 구현에 관한 참고 사항은 부록 A를 참조하라.)

### 3.1. 약어 (Abbreviations)

   ABNF   Augmented Backus-Naur Form

   Authz  Authorization

   PKCE   Proof Key for Code Exchange

   MITM   Man-in-the-middle

   MTI    Mandatory To Implement

## 4. 프로토콜 (Protocol)

### 4.1. 클라이언트가 코드 검증자를 생성함 (Client Creates a Code Verifier)

클라이언트는 먼저 각 OAuth 2.0 [RFC6749] 인가 요청에 대해 다음과 같은 방식으로 코드 검증자 "code_verifier"를 생성한다:

code_verifier = [RFC3986]의 섹션 2.3에서 정의된 비예약 문자 [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"을 사용하는 고엔트로피 암호학적 랜덤 STRING으로, 최소 길이 43자, 최대 길이 128자이다.

"code_verifier"에 대한 ABNF는 다음과 같다.

```
code-verifier = 43*128unreserved
unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
ALPHA = %x41-5A / %x61-7A
DIGIT = %x30-39
```

참고: 코드 검증자는 값을 추측하는 것이 비실용적일 만큼 충분한 엔트로피를 가져야(SHOULD) 한다. 적절한 난수 생성기의 출력을 사용하여 32옥텟 시퀀스를 생성하는 것이 권장된다(RECOMMENDED). 그런 다음 옥텟 시퀀스를 base64url 인코딩하여 코드 검증자로 사용할 43옥텟 URL 안전 문자열을 생성한다.

### 4.2. 클라이언트가 코드 챌린지를 생성함 (Client Creates the Code Challenge)

클라이언트는 다음 변환 중 하나를 코드 검증자에 적용하여 코드 검증자로부터 도출된 코드 챌린지를 생성한다:

plain
   code_challenge = code_verifier

S256
   code_challenge = BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))

클라이언트가 "S256"을 사용할 수 있는 경우, "S256"을 사용해야(MUST) 한다. "S256"은 서버에서 구현 필수(MTI: Mandatory To Implement)이기 때문이다. 클라이언트는 일부 기술적 이유로 "S256"을 지원할 수 없고 서버가 "plain"을 지원한다는 것을 대역 외(out-of-band) 구성을 통해 알고 있는 경우에만 "plain"을 사용하는 것이 허용된다.

plain 변환은 기존 배포와의 호환성 및 S256 변환을 사용할 수 없는 제한된 환경을 위한 것이다.

"code_challenge"에 대한 ABNF는 다음과 같다.

```
code-challenge = 43*128unreserved
unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
ALPHA = %x41-5A / %x61-7A
DIGIT = %x30-39
```

### 4.3. 클라이언트가 인가 요청과 함께 코드 챌린지를 전송함 (Client Sends the Code Challenge with the Authorization Request)

클라이언트는 다음 추가 매개변수를 사용하여 OAuth 2.0 인가 요청([RFC6749]의 섹션 4.1.1)의 일부로 코드 챌린지를 전송한다:

code_challenge
   필수(REQUIRED). 코드 챌린지.

code_challenge_method
   선택(OPTIONAL), 요청에 존재하지 않는 경우 "plain"이 기본값이다. 코드 검증자 변환 방법은 "S256" 또는 "plain"이다.

### 4.4. 서버가 코드를 반환함 (Server Returns the Code)

서버가 인가 응답에서 인가 코드를 발급할 때, 나중에 검증할 수 있도록 "code_challenge"와 "code_challenge_method" 값을 인가 코드와 연관시켜야(MUST) 한다.

일반적으로 "code_challenge"와 "code_challenge_method" 값은 "code" 자체에 암호화된 형태로 저장되지만, 대안적으로 코드와 연관되어 서버에 저장될 수도 있다. 서버는 다른 엔터티가 추출할 수 있는 형태로 클라이언트 요청에 "code_challenge" 값을 포함해서는 안 된다(MUST NOT).

서버가 "code_challenge"를 발급된 "code"와 연관시키는 정확한 방법은 본 명세의 범위 밖이다.

#### 4.4.1. 오류 응답 (Error Response)

서버가 OAuth 공개 클라이언트에 의한 PKCE(Proof Key for Code Exchange)를 요구하고 클라이언트가 요청에 "code_challenge"를 전송하지 않는 경우, 인가 엔드포인트는 "error" 값이 "invalid_request"로 설정된 인가 오류 응답을 반환해야(MUST) 한다. "error_description" 또는 "error_uri"의 응답은 오류의 성격을 설명해야(SHOULD) 한다(예: code challenge required).

서버가 PKCE를 지원하지만 요청된 변환을 지원하지 않는 경우, 인가 엔드포인트는 "error" 값이 "invalid_request"로 설정된 인가 오류 응답을 반환해야(MUST) 한다. "error_description" 또는 "error_uri"의 응답은 오류의 성격을 설명해야(SHOULD) 한다(예: transform algorithm not supported).

### 4.5. 클라이언트가 인가 코드와 코드 검증자를 토큰 엔드포인트로 전송함 (Client Sends the Authorization Code and the Code Verifier to the Token Endpoint)

인가 코드를 수신하면, 클라이언트는 토큰 엔드포인트로 액세스 토큰 요청을 전송한다. OAuth 2.0 액세스 토큰 요청([RFC6749]의 섹션 4.1.3)에 정의된 매개변수 외에, 다음 매개변수를 전송한다:

code_verifier
   필수(REQUIRED). 코드 검증자

"code_challenge_method"는 인가 코드가 발급될 때 인가 코드에 바인딩된다. 이것이 토큰 엔드포인트가 "code_verifier"를 검증하는 데 사용해야(MUST) 하는 방법이다.

### 4.6. 서버가 토큰 반환 전에 code_verifier를 검증함 (Server Verifies code_verifier before Returning the Tokens)

토큰 엔드포인트에서 요청을 수신하면, 서버는 수신된 "code_verifier"에서 코드 챌린지를 계산하고, 클라이언트가 지정한 "code_challenge_method" 방법에 따라 먼저 변환한 후, 이전에 연관된 "code_challenge"와 비교하여 이를 검증한다.

섹션 4.3의 "code_challenge_method"가 "S256"인 경우, 수신된 "code_verifier"는 SHA-256으로 해시되고, base64url 인코딩된 후, "code_challenge"와 비교된다. 즉:

   BASE64URL-ENCODE(SHA256(ASCII(code_verifier))) == code_challenge

섹션 4.3의 "code_challenge_method"가 "plain"인 경우, 직접 비교된다. 즉:

   code_verifier == code_challenge.

값이 같으면, 토큰 엔드포인트는 정상적으로(OAuth 2.0 [RFC6749]에 정의된 대로) 처리를 계속해야(MUST) 한다. 값이 같지 않으면, [RFC6749]의 섹션 5.2에 기술된 대로 "invalid_grant"를 나타내는 오류 응답이 반환되어야(MUST) 한다.

## 5. 호환성 (Compatibility)

본 명세의 서버 구현은 이 확장을 구현하지 않는 OAuth 2.0 클라이언트를 수용할 수 있다(MAY). 인가 요청에서 클라이언트로부터 "code_verifier"가 수신되지 않는 경우, 하위 호환성을 지원하는 서버는 이 확장 없이 OAuth 2.0 [RFC6749] 프로토콜로 되돌아간다.

OAuth 2.0 [RFC6749] 서버 응답이 본 명세에 의해 변경되지 않으므로, 본 명세의 클라이언트 구현은 서버가 본 명세를 구현했는지 여부를 알 필요가 없으며, 섹션 4에 정의된 추가 매개변수를 모든 서버에 전송해야(SHOULD) 한다.

## 6. IANA 고려 사항 (IANA Considerations)

IANA는 이 문서에 따라 다음 등록을 수행하였다.

### 6.1. OAuth 매개변수 레지스트리 (OAuth Parameters Registry)

본 명세는 OAuth 2.0 [RFC6749]에 정의된 IANA "OAuth Parameters" 레지스트리에 다음 매개변수를 등록한다.

   o  매개변수 이름: code_verifier
   o  매개변수 사용 위치: token request
   o  변경 관리자: IESG
   o  명세 문서: RFC 7636 (이 문서)

   o  매개변수 이름: code_challenge
   o  매개변수 사용 위치: authorization request
   o  변경 관리자: IESG
   o  명세 문서: RFC 7636 (이 문서)

   o  매개변수 이름: code_challenge_method
   o  매개변수 사용 위치: authorization request
   o  변경 관리자: IESG
   o  명세 문서: RFC 7636 (이 문서)

### 6.2. PKCE 코드 챌린지 방법 레지스트리 (PKCE Code Challenge Method Registry)

본 명세는 "PKCE Code Challenge Methods" 레지스트리를 신설한다. 새 레지스트리는 "OAuth Parameters" 레지스트리의 하위 레지스트리여야 한다.

인가 엔드포인트에서 사용하기 위한 추가 "code_challenge_method" 유형은 Specification Required 정책 [RFC5226]을 사용하여 등록되며, 이는 하나 이상의 지정 전문가(Designated Experts, DEs)에 의한 요청 검토를 포함한다. 지정 전문가는 oauth-ext-review@ietf.org 메일링 리스트에서 최소 2주간의 요청 검토가 이루어지고 해당 리스트에서의 모든 논의가 수렴된 후에 요청에 응답해야 한다. 발행 전 값 할당을 허용하기 위해, 지정 전문가는 수용 가능한 명세가 발행될 것이라고 확신하면 등록을 승인할 수 있다.

oauth-ext-review@ietf.org 메일링 리스트에서의 등록 요청 및 논의는 적절한 제목을 사용해야 한다(예: "Request for PKCE code_challenge_method: example").

지정 전문가는 등록 요청을 평가할 때 메일링 리스트에서의 논의와 챌린지 방법의 전반적인 보안 속성을 고려해야(SHOULD) 한다. 새로운 방법은 인가 엔드포인트로의 요청에서 code_verifier의 값을 공개해서는 안 된다. 거부에는 설명과, 해당되는 경우, 요청을 성공적으로 만드는 방법에 대한 제안이 포함되어야 한다.

#### 6.2.1. 등록 템플릿 (Registration Template)

Code Challenge Method 매개변수 이름:
   요청된 이름(예: "example"). 본 명세의 핵심 목표 중 하나가 결과 표현이 간결한 것이므로, 불가피한 이유가 없는 한 이름이 8자를 초과하지 않는 것이 권장된다(RECOMMENDED). 이 이름은 대소문자를 구분한다. 지정 전문가가 이 특정 경우에 예외를 허용해야 할 불가피한 이유가 있다고 명시하지 않는 한, 이름은 대소문자를 구분하지 않는 방식으로 다른 등록된 이름과 일치해서는 안 된다.

변경 관리자(Change Controller):
   Standards Track RFC의 경우, "IESG"로 명시한다. 그 외의 경우, 책임 당사자의 이름을 기재한다. 기타 세부 사항(예: 우편 주소, 이메일 주소, 홈페이지 URI)도 포함될 수 있다.

명세 문서(Specification Document(s)):
   매개변수를 명세하는 문서에 대한 참조로, 해당 문서의 사본을 검색하는 데 사용할 수 있는 URI를 포함하는 것이 바람직하다. 관련 섹션의 표시도 포함될 수 있지만 필수는 아니다.

#### 6.2.2. 초기 레지스트리 내용 (Initial Registry Contents)

이 문서에 따라, IANA는 이 레지스트리에 섹션 4.2에 정의된 Code Challenge Method 매개변수 이름을 등록하였다.

   o  Code Challenge Method 매개변수 이름: plain
   o  변경 관리자: IESG
   o  명세 문서: RFC 7636의 섹션 4.2 (이 문서)

   o  Code Challenge Method 매개변수 이름: S256
   o  변경 관리자: IESG
   o  명세 문서: RFC 7636의 섹션 4.2 (이 문서)

## 7. 보안 고려 사항 (Security Considerations)

### 7.1. code_verifier의 엔트로피 (Entropy of the code_verifier)

보안 모델은 코드 검증자가 공격자에 의해 학습되거나 추측되지 않는다는 사실에 의존한다. 이 원칙을 준수하는 것이 극히 중요하다. 따라서, 코드 검증자는 공격자가 추측하는 것이 비실용적일 만큼 암호학적으로 랜덤하고 높은 엔트로피를 가지도록 생성되어야 한다.

클라이언트는 최소 256비트의 엔트로피를 가진 "code_verifier"를 생성해야(SHOULD) 한다. 이는 적절한 난수 생성기가 32옥텟 시퀀스를 생성하도록 하여 수행할 수 있다. 그런 다음 옥텟 시퀀스를 base64url 인코딩하여 필요한 엔트로피를 가진 "code_challenge"로 사용할 43옥텟 URL 안전 문자열을 생성할 수 있다.

### 7.2. 도청자에 대한 보호 (Protection against Eavesdroppers)

클라이언트는 "S256" 방법을 시도한 후 "plain"으로 다운그레이드해서는 안 된다(MUST NOT). PKCE를 지원하는 서버는 "S256"을 지원해야 하며, PKCE를 지원하지 않는 서버는 알 수 없는 "code_verifier"를 단순히 무시한다. 이 때문에, "S256"이 제시되었을 때의 오류는 서버에 결함이 있거나 MITM 공격자가 다운그레이드 공격을 시도하고 있음을 의미할 수 있을 뿐이다.

"S256" 방법은 "code_challenge"를 관찰하거나 가로채는 도청자로부터 보호한다. 챌린지는 검증자 없이 사용될 수 없기 때문이다. "plain" 방법에서는 장치나 http 요청에서 "code_challenge"가 공격자에 의해 관찰될 가능성이 있다. 이 경우 코드 챌린지가 코드 검증자와 동일하므로, "plain" 방법은 초기 요청의 도청으로부터 보호하지 않는다.

"S256"의 사용은 공격자에게 "code_verifier" 값이 공개되는 것을 방지한다.

이 때문에, "plain"은 사용해서는 안 되며(SHOULD NOT) 요청 경로가 이미 보호된 배포된 구현과의 호환성을 위해서만 존재한다. "plain" 방법은 일부 기술적 이유로 "S256"을 지원할 수 없는 경우가 아니라면 새로운 구현에서 사용해서는 안 된다(SHOULD NOT).

"S256" 코드 챌린지 방법 또는 기타 암호학적으로 안전한 코드 챌린지 방법 확장이 사용되어야(SHOULD) 한다. "plain" 코드 챌린지 방법은 운영 체제와 전송 보안이 공격자에게 요청을 공개하지 않는 것에 의존한다.

코드 챌린지 방법이 "plain"이고 코드 챌린지가 상태 없는(stateless) 서버를 달성하기 위해 인가 "code" 내부에 반환되어야 하는 경우, 서버만이 복호화하고 추출할 수 있는 방식으로 암호화되어야(MUST) 한다.

### 7.3. code_challenge에 대한 솔트(salting) (Salting the code_challenge)

코드 검증자가 무차별 대입 공격을 방지하기에 충분한 엔트로피를 포함하므로, 구현 복잡성을 줄이기 위해 코드 챌린지 생성에 솔트를 사용하지 않는다. 공개적으로 알려진 값을 (256비트의 엔트로피를 포함하는) 코드 검증자에 연결한 후 SHA256으로 해시하여 코드 챌린지를 생성하더라도, code_verifier의 유효한 값을 무차별 대입하는 데 필요한 시도 횟수를 증가시키지 않을 것이다.

"S256" 변환은 비밀번호 해싱과 유사하지만, 중요한 차이가 있다. 비밀번호는 오프라인에서 해시되어 사전에서 해시를 조회할 수 있는 비교적 낮은 엔트로피의 단어인 경향이 있다. 해싱 전에 각 비밀번호에 고유하지만 공개된 값을 연결함으로써, 공격자가 검색해야 하는 사전 공간이 크게 확장된다.

현대의 그래픽 프로세서는 이제 공격자가 디스크에서 조회할 수 있는 것보다 더 빠르게 실시간으로 해시를 계산할 수 있게 한다. 이는 낮은 엔트로피의 비밀번호에 대해서도 무차별 대입 공격의 복잡성을 증가시키는 데 있어 솔트의 가치를 제거한다.

### 7.4. OAuth 보안 고려 사항 (OAuth Security Considerations)

[RFC6819]에 제시된 모든 OAuth 보안 분석이 적용되므로, 독자는 이를 주의 깊게 따라야(SHOULD) 한다.

### 7.5. TLS 보안 고려 사항 (TLS Security Considerations)

현재의 보안 고려 사항은 "Recommendations for Secure Use of Transport Layer Security (TLS) and Datagram Transport Layer Security (DTLS)" [BCP195]에서 확인할 수 있다. 이것은 OAuth 2.0 [RFC6749]의 TLS 버전 권장 사항을 대체한다.

## 8. 참조 (References)

### 8.1. 규범적 참조 (Normative References)

   [BCP195]   Sheffer, Y., Holz, R., and P. Saint-Andre,
              "Recommendations for Secure Use of Transport Layer
              Security (TLS) and Datagram Transport Layer Security
              (DTLS)", BCP 195, RFC 7525, May 2015,
              <http://www.rfc-editor.org/info/bcp195>.

   [RFC20]    Cerf, V., "ASCII format for network interchange", STD 80,
              RFC 20, DOI 10.17487/RFC0020, October 1969,
              <http://www.rfc-editor.org/info/rfc20>.

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119,
              DOI 10.17487/RFC2119, March 1997,
              <http://www.rfc-editor.org/info/rfc2119>.

   [RFC3986]  Berners-Lee, T., Fielding, R., and L. Masinter, "Uniform
              Resource Identifier (URI): Generic Syntax", STD 66,
              RFC 3986, DOI 10.17487/RFC3986, January 2005,
              <http://www.rfc-editor.org/info/rfc3986>.

   [RFC4648]  Josefsson, S., "The Base16, Base32, and Base64 Data
              Encodings", RFC 4648, DOI 10.17487/RFC4648, October 2006,
              <http://www.rfc-editor.org/info/rfc4648>.

   [RFC5226]  Narten, T. and H. Alvestrand, "Guidelines for Writing an
              IANA Considerations Section in RFCs", BCP 26, RFC 5226,
              DOI 10.17487/RFC5226, May 2008,
              <http://www.rfc-editor.org/info/rfc5226>.

   [RFC5234]  Crocker, D., Ed. and P. Overell, "Augmented BNF for Syntax
              Specifications: ABNF", STD 68, RFC 5234,
              DOI 10.17487/RFC5234, January 2008,
              <http://www.rfc-editor.org/info/rfc5234>.

   [RFC6234]  Eastlake 3rd, D. and T. Hansen, "US Secure Hash Algorithms
              (SHA and SHA-based HMAC and HKDF)", RFC 6234,
              DOI 10.17487/RFC6234, May 2011,
              <http://www.rfc-editor.org/info/rfc6234>.

   [RFC6749]  Hardt, D., Ed., "The OAuth 2.0 Authorization Framework",
              RFC 6749, DOI 10.17487/RFC6749, October 2012,
              <http://www.rfc-editor.org/info/rfc6749>.

### 8.2. 참고적 참조 (Informative References)

   [RFC6819]  Lodderstedt, T., Ed., McGloin, M., and P. Hunt, "OAuth 2.0
              Threat Model and Security Considerations", RFC 6819,
              DOI 10.17487/RFC6819, January 2013,
              <http://www.rfc-editor.org/info/rfc6819>.

## 부록 A. 패딩 없는 Base64url 인코딩 구현에 관한 참고 사항 (Notes on Implementing Base64url Encoding without Padding)

이 부록은 패딩을 사용하는 표준 base64 인코딩 함수를 기반으로, 패딩 없는 base64url 인코딩 함수를 구현하는 방법을 설명한다.

구체적으로, 이 함수를 구현하는 예제 C# 코드가 아래에 나와 있다. 다른 언어에서도 유사한 코드를 사용할 수 있다.

```
     static string base64urlencode(byte [] arg)
     {
       string s = Convert.ToBase64String(arg); // Regular base64 encoder
       s = s.Split('=')[0]; // Remove any trailing '='s
       s = s.Replace('+', '-'); // 62nd char of encoding
       s = s.Replace('/', '_'); // 63rd char of encoding
       return s;
     }
```

인코딩되지 않은 값과 인코딩된 값 사이의 대응 예는 다음과 같다. 아래의 옥텟 시퀀스는 아래의 문자열로 인코딩되며, 디코딩하면 옥텟 시퀀스를 재생성한다.

```
   3 236 255 224 193

   A-z_4ME
```

## 부록 B. S256 code_challenge_method에 대한 예제 (Example for the S256 code_challenge_method)

클라이언트는 적절한 난수 생성기의 출력을 사용하여 32옥텟 시퀀스를 생성한다. 이 예제에서 값을 나타내는 옥텟(JSON 배열 표기법 사용)은 다음과 같다:

```
      [116, 24, 223, 180, 151, 153, 224, 37, 79, 250, 96, 125, 216, 173,
      187, 186, 22, 212, 37, 77, 105, 214, 191, 240, 91, 88, 5, 88, 83,
      132, 141, 121]
```

이 옥텟 시퀀스를 base64url로 인코딩하면 code_verifier의 값이 제공된다:

```
       dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

그런 다음 code_verifier는 SHA256 해시 함수를 통해 해시되어 다음을 생성한다:

```
     [19, 211, 30, 150, 26, 26, 216, 236, 47, 22, 177, 12, 76, 152, 46,
      8, 118, 168, 120, 173, 109, 241, 68, 86, 110, 225, 137, 74, 203,
      112, 249, 195]
```

이 옥텟 시퀀스를 base64url로 인코딩하면 code_challenge의 값이 제공된다:

```
       E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
```

인가 요청은 다음을 포함한다:

```
       code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
       &code_challenge_method=S256
```

인가 서버는 클라이언트에게 부여된 코드와 함께 code_challenge 및 code_challenge_method를 기록한다.

token_endpoint로의 요청에서, 클라이언트는 인가 응답에서 수신한 코드와 함께 추가 매개변수를 포함한다:

```
       code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

인가 서버는 코드 부여에 대한 정보를 검색한다. 기록된 code_challenge_method가 S256인 것에 기반하여, code_verifier의 값을 해시하고 base64url 인코딩한다:

```
   BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))
```

계산된 값은 "code_challenge"의 값과 비교된다:

```
   BASE64URL-ENCODE(SHA256(ASCII(code_verifier))) == code_challenge
```

두 값이 같으면, 요청에 다른 오류가 없는 한 인가 서버는 토큰을 제공할 수 있다. 값이 같지 않으면, 요청이 거부되어야 하며 오류가 반환되어야 한다.

## 감사의 글 (Acknowledgements)

본 명세의 초기 초안 버전은 OpenID Foundation의 OpenID AB/Connect Working Group에 의해 작성되었다.

본 명세는 수십 명의 활발하고 헌신적인 참가자를 포함하는 OAuth Working Group의 산출물이다. 특히, 다음 개인들이 최종 명세를 형성하고 구체화하는 아이디어, 피드백 및 문구를 기여하였다:

   Anthony Nadalin, Microsoft
   Axel Nenker, Deutsche Telekom
   Breno de Medeiros, Google
   Brian Campbell, Ping Identity
   Chuck Mortimore, Salesforce
   Dirk Balfanz, Google
   Eduardo Gueiros, Jive Communications
   Hannes Tschonfenig, ARM
   James Manger, Telstra
   Justin Richer, MIT Kerberos
   Josh Mandel, Boston Children's Hospital
   Lewis Adam, Motorola Solutions
   Madjid Nakhjiri, Samsung
   Michael B. Jones, Microsoft
   Paul Madsen, Ping Identity
   Phil Hunt, Oracle
   Prateek Mishra, Oracle
   Ryo Ito, mixi
   Scott Tomilson, Ping Identity
   Sergey Beryozkin
   Takamichi Saito
   Torsten Lodderstedt, Deutsche Telekom
   William Denniss, Google

## 저자 주소 (Authors' Addresses)

   Nat Sakimura (editor)
   Nomura Research Institute
   1-6-5 Marunouchi, Marunouchi Kitaguchi Bldg.
   Chiyoda-ku, Tokyo  100-0005
   Japan

   Phone: +81-3-5533-2111
   Email: n-sakimura@nri.co.jp
   URI:   http://nat.sakimura.org/

   John Bradley
   Ping Identity
   Casilla 177, Sucursal Talagante
   Talagante, RM
   Chile

   Phone: +44 20 8133 3718
   Email: ve7jtb@ve7jtb.com
   URI:   http://www.thread-safe.com/

   Naveen Agarwal
   Google
   1600 Amphitheatre Parkway
   Mountain View, CA  94043
   United States

   Phone: +1 650-253-0000
   Email: naa@google.com
   URI:   http://google.com/
