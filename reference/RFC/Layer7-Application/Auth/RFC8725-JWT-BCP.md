Internet Engineering Task Force (IETF)                        Y. Sheffer
Request for Comments: 8725                                        Intuit
BCP: 225                                                        D. Hardt
Updates: 7519
Category: Best Current Practice                                 M. Jones
ISSN: 2070-1721                                                Microsoft
                                                           February 2020


                 JSON Web Token 현행 모범 사례

초록

   JSON 웹 토큰(JWT)은 서명 및/또는 암호화할 수 있는 클레임 집합을
   포함하는 URL 안전 JSON 기반 보안 토큰입니다. JWT는 디지털 신원
   영역과 기타 애플리케이션 영역 모두에서 다수의 프로토콜과 애플리케이션에서
   간단한 보안 토큰 형식으로 널리 사용되고 배포되고 있습니다. 이 현행
   모범 사례 문서는 RFC 7519를 업데이트하여 JWT의 안전한 구현과 배포를
   이끄는 실행 가능한 지침을 제공합니다.

이 메모의 상태

   이 메모는 인터넷 현행 모범 사례를 문서화합니다.

   이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물입니다. 이것은
   IETF 커뮤니티의 합의를 나타냅니다. 공개 검토를 받았으며 인터넷
   엔지니어링 운영 그룹(IESG)에 의해 출판이 승인되었습니다. BCP에 대한
   추가 정보는 RFC 7841의 섹션 2에서 확인할 수 있습니다.

   이 문서의 현재 상태, 정오표 및 피드백 제공 방법에 관한 정보는
   https://www.rfc-editor.org/info/rfc8725 에서 얻을 수 있습니다.

저작권 고지

   Copyright (c) 2020 IETF Trust and the persons identified as the
   document authors.  All rights reserved.

   이 문서는 BCP 78과 이 문서의 발행일에 유효한 IETF 문서에 관한
   IETF Trust의 법적 조항(https://trustee.ietf.org/license-info)의
   적용을 받습니다. 이 문서들은 이 문서에 관한 귀하의 권리와 제한을
   설명하므로 주의 깊게 검토하시기 바랍니다. 이 문서에서 추출된 코드
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

## 1. 소개

   JSON 웹 토큰, 즉 JWT [RFC7519]는 서명 및/또는 암호화할 수 있는
   클레임 집합을 포함하는 URL 안전 JSON 기반 보안 토큰입니다. JWT
   명세는 보안 관련 정보를 보호하기 쉬운 하나의 위치에 캡슐화하고,
   널리 사용 가능한 도구를 사용하여 쉽게 구현할 수 있기 때문에 빠른
   채택을 이루었습니다. JWT가 일반적으로 사용되는 하나의 애플리케이션
   영역은 디지털 신원 정보를 표현하는 것으로, OpenID Connect ID
   Token [OpenID.Core]과 OAuth 2.0 [RFC6749] 액세스 토큰 및 리프레시
   토큰이 이에 해당하며, 세부 사항은 배포에 따라 다릅니다.

   JWT 명세가 발행된 이후로 구현과 배포에 대한 여러 널리 알려진 공격이
   있었습니다. 이러한 공격은 불충분하게 명세된 보안 메커니즘과 불완전한
   구현 및 애플리케이션의 잘못된 사용의 결과입니다.

   이 문서의 목표는 JWT의 안전한 구현과 배포를 촉진하는 것입니다. 이
   문서의 많은 권장사항은 JSON Web Signature(JWS) [RFC7515], JSON Web
   Encryption(JWE) [RFC7516], JSON Web Algorithms(JWA) [RFC7518]에
   의해 정의된 JWT의 기반이 되는 암호화 메커니즘의 구현 및 사용에
   관한 것입니다. 나머지는 JWT 클레임 자체의 사용에 관한 것입니다.

   이것은 대다수의 구현 및 배포 시나리오에서 JWT 사용에 대한 최소한의
   권장사항으로 의도되었습니다. 이 문서를 참조하는 다른 명세는 특정
   상황에 따라 형식의 하나 이상의 측면에 관하여 더 엄격한 요구사항을
   가질 수 있습니다; 그러한 경우 구현자는 해당 더 엄격한 요구사항을
   준수하는 것이 좋습니다. 또한 이 문서는 하한선을 제공하는 것이지
   상한선을 제공하는 것이 아니므로, 더 강력한 옵션은 항상 허용됩니다
   (예: 암호화 강도 대 계산 부하의 중요성에 대한 서로 다른 평가에
   따라).

   다양한 알고리즘의 강도와 실행 가능한 공격에 대한 커뮤니티 지식은
   빠르게 변할 수 있으며, 경험에 따르면 보안에 관한 현행 모범 사례(BCP)
   문서는 특정 시점의 진술입니다. 독자는 이 문서에 적용되는 정오표나
   업데이트를 찾아보는 것이 좋습니다.

### 1.1. 대상 독자

   이 문서의 의도된 독자는 다음과 같습니다:

   *  JWT 라이브러리(그리고 해당 라이브러리가 사용하는 JWS 및 JWE
      라이브러리)의 구현자,

   *  그러한 라이브러리를 사용하는 코드의 구현자(일부 메커니즘이
      라이브러리에서 제공되지 않을 수 있거나, 제공될 때까지의 범위에서),
      그리고

   *  IETF 내외부에서 JWT에 의존하는 명세의 개발자.

### 1.2. 이 문서에서 사용된 규약

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY",
   "OPTIONAL"이라는 키워드는 여기에 표시된 것처럼 모두 대문자로
   나타날 때, 그리고 오직 그때만 BCP 14 [RFC2119] [RFC8174]에 설명된
   대로 해석되어야 합니다.

## 2. 위협 및 취약점

   이 섹션은 JWT 구현 및 배포에서 알려진 그리고 가능한 몇 가지 문제를
   나열합니다. 각 문제 설명 뒤에는 해당 문제에 대한 하나 이상의 완화
   방안에 대한 참조가 따릅니다.

### 2.1. 취약한 서명 및 불충분한 서명 검증

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

### 2.2. 취약한 대칭키

   또한, 일부 애플리케이션은 토큰에 서명하기 위해 "HS256"과 같은
   키 기반 메시지 인증 코드(MAC) 알고리즘을 사용하지만, 불충분한
   엔트로피를 가진 약한 대칭키(예: 인간이 기억할 수 있는 비밀번호)를
   제공합니다. 이러한 키는 공격자가 그러한 토큰을 확보하면 오프라인
   무차별 대입 또는 사전 공격에 취약합니다 [Langkemper].

   완화 방안에 대해서는 섹션 3.5를 참조하십시오.

### 2.3. 암호화와 서명의 잘못된 조합

   JWE 암호화된 JWT를 복호화하여 JWS 서명된 객체를 얻는 일부 라이브러리는
   내부 서명을 항상 검증하지는 않습니다.

   완화 방안에 대해서는 섹션 3.3을 참조하십시오.

### 2.4. 암호문 길이 분석을 통한 평문 유출

   많은 암호화 알고리즘은 평문의 길이에 대한 정보를 유출하며, 알고리즘과
   운영 모드에 따라 유출량이 다릅니다. 이 문제는 평문이 초기에 압축될
   때 악화되는데, 압축된 평문의 길이와 따라서 암호문의 길이가 원래
   평문의 길이뿐만 아니라 그 내용에도 의존하기 때문입니다. 압축 공격은
   공격자가 제어하는 데이터가 비밀 데이터와 동일한 압축 공간에 있을 때
   특히 강력하며, 이는 HTTPS에 대한 일부 공격의 경우에 해당합니다.

   압축과 암호화에 대한 일반적인 배경은 [Kelsey]를, HTTP 쿠키에 대한
   공격의 구체적인 예는 [Alawatugoda]를 참조하십시오.

   완화 방안에 대해서는 섹션 3.6을 참조하십시오.

### 2.5. 안전하지 않은 타원 곡선 암호화 사용

   [Sanso]에 따르면, 여러 Javascript Object Signing and Encryption
   (JOSE) 라이브러리가 타원 곡선 키 합의("ECDH-ES" 알고리즘)를 수행할
   때 입력을 올바르게 검증하지 못합니다. 유효하지 않은 곡선 점을
   사용하는 자신이 선택한 JWE를 보내고 유효하지 않은 곡선 점으로
   복호화한 결과인 평문 출력을 관찰할 수 있는 공격자는 이 취약점을
   사용하여 수신자의 개인키를 복구할 수 있습니다.

   완화 방안에 대해서는 섹션 3.4를 참조하십시오.

### 2.6. JSON 인코딩의 다양성

   폐기된 [RFC7159]와 같은 이전 버전의 JSON 형식은 UTF-8, UTF-16,
   UTF-32의 여러 다른 문자 인코딩을 허용했습니다. 최신 표준 [RFC8259]은
   "폐쇄 생태계" 내의 내부 사용을 제외하고 UTF-8만 허용하므로 이제는
   그렇지 않습니다. 오래된 구현체와 폐쇄 환경 내에서 사용되는 구현체가
   비표준 인코딩을 생성할 수 있는 이러한 모호성은 JWT가 수신자에 의해
   잘못 해석되는 결과를 초래할 수 있습니다. 이는 차례로 악의적인
   발신자가 수신자의 검증 확인을 우회하는 데 사용될 수 있습니다.

   완화 방안에 대해서는 섹션 3.7을 참조하십시오.

### 2.7. 대체 공격

   하나의 수신자에게 의도된 JWT가 해당 수신자에게 주어지고, 그 수신자가
   해당 JWT가 의도되지 않은 다른 수신자에게 이를 사용하려고 시도하는
   공격이 있습니다. 예를 들어, OAuth 2.0 [RFC6749] 액세스 토큰이
   의도된 OAuth 2.0 보호 리소스에 합법적으로 제시된 경우, 해당 보호
   리소스가 액세스 토큰이 의도되지 않은 다른 보호 리소스에 동일한
   액세스 토큰을 제시하여 접근을 얻으려고 시도할 수 있습니다. 이러한
   상황이 포착되지 않으면, 공격자가 접근 권한이 없는 리소스에 접근하게
   되는 결과를 초래할 수 있습니다.

   완화 방안에 대해서는 섹션 3.8 및 3.9를 참조하십시오.

### 2.8. 교차 JWT 혼동

   JWT가 다양한 애플리케이션 영역에서 더 많은 서로 다른 프로토콜에
   의해 사용됨에 따라, 하나의 목적으로 발행된 JWT 토큰이 전용되어
   다른 목적으로 사용되는 경우를 방지하는 것이 점점 더 중요해집니다.
   이것은 특정 유형의 대체 공격임에 유의하십시오. JWT가 다른 종류의
   JWT와 혼동될 수 있는 애플리케이션 컨텍스트에서 사용될 수 있다면,
   이러한 대체 공격을 방지하기 위한 완화 방안이 사용되어야 합니다(MUST).

   완화 방안에 대해서는 섹션 3.8, 3.9, 3.11, 3.12를 참조하십시오.

### 2.9. 서버에 대한 간접 공격

   다양한 JWT 클레임이 수신자에 의해 데이터베이스 및 경량 디렉터리
   접근 프로토콜(LDAP) 검색과 같은 조회 작업을 수행하는 데 사용됩니다.
   그 외에도 서버에 의해 유사하게 조회되는 URL을 포함하는 것들이
   있습니다. 이러한 클레임 중 어느 것이든 공격자가 주입 공격이나
   서버 측 요청 위조(SSRF) 공격을 위한 벡터로 사용할 수 있습니다.

   완화 방안에 대해서는 섹션 3.10을 참조하십시오.

## 3. 모범 사례

   아래에 나열된 모범 사례는 실무자가 앞 섹션에 나열된 위협을 완화하기
   위해 적용해야 합니다.

### 3.1. 알고리즘 검증 수행

   라이브러리는 호출자가 지원하는 알고리즘 집합을 지정할 수 있도록
   해야 하며(MUST), 암호화 작업을 수행할 때 다른 알고리즘을 사용해서는
   안 됩니다(MUST NOT). 라이브러리는 "alg" 또는 "enc" 헤더가 암호화
   작업에 사용되는 것과 동일한 알고리즘을 지정하는지 확인해야
   합니다(MUST). 또한 각 키는 정확히 하나의 알고리즘과 함께
   사용되어야 하며(MUST), 이것은 암호화 작업이 수행될 때
   확인되어야 합니다(MUST).

### 3.2. 적절한 알고리즘 사용

   [RFC7515]의 섹션 5.2에서 말하듯이, "주어진 컨텍스트에서 어떤
   알고리즘을 사용할 수 있는지는 애플리케이션의 결정입니다. JWS가
   성공적으로 검증될 수 있더라도, JWS에 사용된 알고리즘이 애플리케이션에
   수용 가능하지 않다면, 해당 JWS를 유효하지 않은 것으로 간주해야
   합니다(SHOULD)."

   따라서 애플리케이션은 애플리케이션의 보안 요구사항을 충족하는
   암호학적으로 현재인 알고리즘만 사용을 허용해야 합니다(MUST). 이
   집합은 새로운 알고리즘이 도입되고 발견된 암호학적 약점으로 인해
   기존 알고리즘이 더 이상 사용되지 않게 됨에 따라 시간이 지남에 따라
   변할 것입니다. 따라서 애플리케이션은 암호화 민첩성을 가능하게 하도록
   설계되어야 합니다(MUST).

   그렇긴 하지만, JWT가 암호학적으로 현재인 알고리즘을 사용하는 TLS와
   같은 전송 계층에 의해 종단간 암호학적으로 보호되는 경우, JWT에 다른
   암호학적 보호 계층을 적용할 필요가 없을 수 있습니다. 이러한 경우
   "none" 알고리즘의 사용은 완전히 수용 가능할 수 있습니다. "none"
   알고리즘은 JWT가 다른 수단에 의해 암호학적으로 보호되는 경우에만
   사용해야 합니다. "none"을 사용하는 JWT는 콘텐츠가 선택적으로
   서명되는 애플리케이션 컨텍스트에서 종종 사용됩니다; 그러면 URL 안전
   클레임 표현과 처리가 서명된 경우와 서명되지 않은 경우 모두에서
   동일할 수 있습니다. JWT 라이브러리는 호출자가 명시적으로 요청하지
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
      서명 체계의 보안이 손상될 수 있습니다. 최악의 경우, 공격자가
      개인키를 복구할 수 있습니다. 이러한 공격에 대응하기 위해 JWT
      라이브러리는 [RFC6979]에 정의된 결정적 접근 방식을 사용하여
      ECDSA를 구현해야 합니다(SHOULD). 이 접근 방식은 기존 ECDSA
      검증자와 완전히 호환되므로 새로운 알고리즘 식별자를 요구하지
      않고 구현할 수 있습니다.

### 3.3. 모든 암호화 작업 검증

   JWT에 사용된 모든 암호화 작업은 검증되어야 하며(MUST), 검증에
   실패하면 전체 JWT가 거부되어야 합니다(MUST). 이것은 단일 헤더
   매개변수 집합을 가진 JWT에만 해당되는 것이 아니라, 외부와 내부
   작업 모두가 애플리케이션에서 제공한 키와 알고리즘을 사용하여
   검증되어야 하는(MUST) 중첩된 JWT에도 해당됩니다.

### 3.4. 암호화 입력 검증

   타원 곡선 Diffie-Hellman 키 합의("ECDH-ES")와 같은 일부 암호화
   작업은 유효하지 않은 값을 포함할 수 있는 입력을 받습니다. 이것은
   지정된 타원 곡선 위에 있지 않은 점이나 기타 유효하지 않은 점(예:
   [Valenta], 섹션 7.1)을 포함합니다. JWS/JWE 라이브러리 자체가 이러한
   입력을 사용하기 전에 검증해야 하거나, 검증을 수행하는 기반 암호화
   라이브러리를 사용해야 합니다(또는 둘 다!).

   타원 곡선 Diffie-Hellman Ephemeral Static(ECDH-ES) 임시 공개키(epk)
   입력은 수신자가 선택한 타원 곡선에 따라 검증되어야 합니다. NIST
   소수 차수 곡선 P-256, P-384, P-521의 경우, 검증은 "이산 로그
   암호학을 사용한 쌍별 키 설정 체계에 대한 권장사항"
   [nist-sp-800-56a-r3]의 섹션 5.6.2.3.4(ECC 부분 공개키 검증
   루틴)에 따라 수행되어야 합니다(MUST). "X25519" 또는 "X448"
   [RFC8037] 알고리즘이 사용되는 경우, [RFC8037]의 보안 고려사항이
   적용됩니다.

### 3.5. 암호화 키의 충분한 엔트로피 보장

   [RFC7515]의 섹션 10.1에 있는 키 엔트로피 및 랜덤 값 조언과
   [RFC7518]의 섹션 8.8에 있는 비밀번호 고려사항이 따라야
   합니다(MUST). 특히 인간이 기억할 수 있는 비밀번호는 "HS256"과 같은
   키 기반 MAC 알고리즘의 키로 직접 사용해서는 안 됩니다(MUST NOT).
   또한 비밀번호는 [RFC7518]의 섹션 4.8에 설명된 대로 콘텐츠 암호화가
   아닌 키 암호화를 수행하는 데에만 사용해야 합니다. 키 암호화에
   사용되는 경우에도 비밀번호 기반 암호화는 여전히 무차별 대입 공격의
   대상이 됩니다.

### 3.6. 암호화 입력의 압축 회피

   데이터의 압축은 암호화 전에 수행해서는 안 됩니다(SHOULD NOT).
   그러한 압축된 데이터는 종종 평문에 대한 정보를 드러내기 때문입니다.

### 3.7. UTF-8 사용

   [RFC7515], [RFC7516], [RFC7519] 모두 헤더 매개변수와 JWT 클레임
   세트에서 사용되는 JSON의 인코딩 및 디코딩에 UTF-8을 사용하도록
   지정합니다. 이것은 최신 JSON 명세 [RFC8259]와도 일치합니다. 구현체와
   애플리케이션은 이를 수행해야 하며(MUST), 이러한 목적으로 다른
   유니코드 인코딩을 사용하거나 사용을 허용해서는 안 됩니다(MUST NOT).

### 3.8. 발행자 및 주체 검증

   JWT에 "iss"(발행자) 클레임이 포함된 경우, 애플리케이션은 JWT의
   암호화 작업에 사용된 암호화 키가 발행자에 속하는지 검증해야
   합니다(MUST). 그렇지 않은 경우, 애플리케이션은 JWT를 거부해야
   합니다(MUST).

   발행자가 소유한 키를 결정하는 수단은 애플리케이션에 따라
   다릅니다. 하나의 예로, OpenID Connect [OpenID.Core] 발행자 값은
   "jwks_uri" 값을 포함하는 JSON 메타데이터 문서를 참조하는 "https"
   URL이며, 이 "jwks_uri"는 발행자의 키가 JWK Set [RFC7517]으로
   검색되는 "https" URL입니다. 동일한 메커니즘이 [RFC8414]에서
   사용됩니다. 다른 애플리케이션은 키를 발행자에 바인딩하는 다른
   수단을 사용할 수 있습니다.

   마찬가지로, JWT에 "sub"(주체) 클레임이 포함된 경우, 애플리케이션은
   주체 값이 애플리케이션에서 유효한 주체 및/또는 발행자-주체 쌍에
   해당하는지 검증해야 합니다(MUST). 이것은 발행자가 애플리케이션에
   의해 신뢰되는지 확인하는 것을 포함할 수 있습니다. 발행자, 주체
   또는 해당 쌍이 유효하지 않은 경우, 애플리케이션은 JWT를 거부해야
   합니다(MUST).

### 3.9. 대상(Audience) 사용 및 검증

   동일한 발행자가 둘 이상의 신뢰 당사자 또는 애플리케이션에서
   사용하도록 의도된 JWT를 발행할 수 있는 경우, JWT에는 JWT가 의도된
   당사자에 의해 사용되고 있는지 또는 공격자가 의도되지 않은 당사자에게
   대체한 것인지를 결정하는 데 사용할 수 있는 "aud"(대상) 클레임이
   포함되어야 합니다(MUST).

   이러한 경우, 신뢰 당사자 또는 애플리케이션은 대상 값을 검증해야
   하며(MUST), 대상 값이 존재하지 않거나 수신자와 연관되지 않은 경우
   JWT를 거부해야 합니다(MUST).

### 3.10. 수신된 클레임을 신뢰하지 않음

   "kid"(키 ID) 헤더는 신뢰 당사자 애플리케이션이 키 조회를 수행하는
   데 사용됩니다. 애플리케이션은 수신된 값을 검증 및/또는 정제하여
   이것이 SQL 또는 LDAP 주입 취약점을 생성하지 않도록 해야 합니다.

   마찬가지로, 임의의 URL을 포함할 수 있는 "jku"(JWK 세트 URL) 또는
   "x5u"(X.509 URL) 헤더를 맹목적으로 따르면 서버 측 요청 위조(SSRF)
   공격이 발생할 수 있습니다. 애플리케이션은 이러한 공격으로부터
   보호해야 합니다(SHOULD). 예를 들어, URL을 허용된 위치의 화이트리스트와
   일치시키고 GET 요청에 쿠키가 전송되지 않도록 하는 방법이 있습니다.

### 3.11. 명시적 타이핑 사용

   때때로 한 종류의 JWT가 다른 종류로 혼동될 수 있습니다. 특정 종류의
   JWT가 이러한 혼동의 대상이 되는 경우, 해당 JWT에 명시적 JWT 유형
   값을 포함할 수 있으며, 검증 규칙에서 유형 확인을 지정할 수 있습니다.
   이 메커니즘은 이러한 혼동을 방지할 수 있습니다. 명시적 JWT 타이핑은
   "typ" 헤더 매개변수를 사용하여 수행됩니다. 예를 들어, [RFC8417]
   명세는 Security Event Token(SET)의 명시적 타이핑을 수행하기 위해
   "application/secevent+jwt" 미디어 유형을 사용합니다.

   [RFC7515]의 섹션 4.1.9에서 "typ"의 정의에 따라, "typ" 값에서
   "application/" 접두사를 생략하는 것이 권장됩니다(RECOMMENDED).
   따라서 예를 들어, SET에 대한 유형을 명시적으로 포함하기 위해
   사용되는 "typ" 값은 "secevent+jwt"여야 합니다(SHOULD). 명시적
   타이핑이 JWT에 사용될 때, "application/example+jwt" 형식의 미디어
   유형 이름을 사용하는 것이 권장됩니다(RECOMMENDED). 여기서 "example"은
   특정 종류의 JWT에 대한 식별자로 대체됩니다.

   중첩된 JWT에 명시적 타이핑을 적용할 때, 명시적 유형 값을 포함하는
   "typ" 헤더 매개변수는 중첩된 JWT의 내부 JWT(페이로드가 JWT 클레임
   세트인 JWT)에 존재해야 합니다(MUST). 일부 경우에는 전체 중첩된 JWT를
   명시적으로 타이핑하기 위해 외부 JWT에도 동일한 "typ" 헤더 매개변수
   값이 존재할 수 있습니다.

   명시적 타이핑의 사용이 기존 종류의 JWT와의 명확한 구분을 달성하지
   못할 수 있음에 유의하십시오. 기존 종류의 JWT에 대한 검증 규칙이
   "typ" 헤더 매개변수 값을 사용하지 않는 경우가 종종 있기 때문입니다.
   명시적 타이핑은 JWT의 새로운 사용에 권장됩니다(RECOMMENDED).

### 3.12. 서로 다른 종류의 JWT에 대해 상호 배타적 검증 규칙 사용

   JWT의 각 애플리케이션은 필수 및 선택적 JWT 클레임과 이에 관련된
   검증 규칙을 지정하는 프로필을 정의합니다. 동일한 발행자가 둘 이상의
   종류의 JWT를 발행할 수 있는 경우, 해당 JWT에 대한 검증 규칙은
   잘못된 종류의 JWT를 거부하도록 상호 배타적으로 작성되어야
   합니다(MUST). 한 컨텍스트에서 다른 컨텍스트로의 JWT 대체를 방지하기
   위해, 애플리케이션 개발자는 여러 전략을 사용할 수 있습니다:

   *  서로 다른 종류의 JWT에 명시적 타이핑을 사용합니다. 그러면
      고유한 "typ" 값을 사용하여 서로 다른 종류의 JWT를 구별할 수
      있습니다.

   *  서로 다른 필수 클레임 집합 또는 서로 다른 필수 클레임 값을
      사용합니다. 그러면 한 종류의 JWT에 대한 검증 규칙이 다른 클레임
      또는 값을 가진 것을 거부합니다.

   *  서로 다른 필수 헤더 매개변수 집합 또는 서로 다른 필수 헤더
      매개변수 값을 사용합니다. 그러면 한 종류의 JWT에 대한 검증 규칙이
      다른 헤더 매개변수 또는 값을 가진 것을 거부합니다.

   *  서로 다른 종류의 JWT에 서로 다른 키를 사용합니다. 그러면 한
      종류의 JWT를 검증하는 데 사용되는 키가 다른 종류의 JWT를 검증하는
      데 실패합니다.

   *  동일한 발행자의 서로 다른 JWT 사용에 서로 다른 "aud" 값을
      사용합니다. 그러면 대상 검증이 부적절한 컨텍스트에 대체된 JWT를
      거부합니다.

   *  서로 다른 종류의 JWT에 서로 다른 발행자를 사용합니다. 그러면
      고유한 "iss" 값을 사용하여 서로 다른 종류의 JWT를 분리할 수
      있습니다.

   JWT 사용과 애플리케이션의 광범위한 다양성을 고려할 때, 서로 다른
   종류의 JWT를 구별하기 위한 유형, 필수 클레임, 값, 헤더 매개변수,
   키 사용 및 발행자의 최적 조합은 일반적으로 애플리케이션에 따라
   다릅니다. 섹션 3.11에서 논의된 바와 같이, 새로운 JWT 애플리케이션의
   경우 명시적 타이핑의 사용이 권장됩니다(RECOMMENDED).

## 4. 보안 고려사항

   이 문서 전체가 JSON 웹 토큰을 구현하고 배포할 때의 보안 고려사항에
   관한 것입니다.

## 5. IANA 고려사항

   이 문서에는 IANA 관련 작업이 없습니다.

## 6. 참고 문헌

### 6.1. 규범적 참고 문헌

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

### 6.2. 참고적 참고 문헌

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

## 감사의 말

   "ECDH-ES" 유효하지 않은 점 공격을 JWE 및 JWT 구현자의 주의로
   가져온 Antonio Sanso에게 감사드립니다. Tim McLean은 RSA/HMAC 혼동
   공격을 발표했습니다. 명시적 타이핑의 사용을 옹호한 Nat Sakimura에게
   감사드립니다. 다수의 의견을 준 Neil Madden에게, 그리고 검토를
   해 준 Carsten Bormann, Brian Campbell, Brian Carpenter, Alissa
   Cooper, Roman Danyliw, Ben Kaduk, Mirja Kuhlewind, Barry Leiba,
   Eric Rescorla, Adam Roach, Martin Vigoureux, Eric Vyncke에게
   감사드립니다.

## 저자 주소

   Yaron Sheffer
   Intuit

   Email: yaronf.ietf@gmail.com


   Dick Hardt

   Email: dick.hardt@gmail.com


   Michael B. Jones
   Microsoft

   Email: mbj@microsoft.com
   URI:   https://self-issued.info/
