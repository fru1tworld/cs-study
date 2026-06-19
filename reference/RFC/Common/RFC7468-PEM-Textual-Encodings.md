Internet Engineering Task Force (IETF)                      S. Josefsson
Request for Comments: 7468                                        SJD AB
Category: Standards Track                                     S. Leonard
ISSN: 2070-1721                                            Penango, Inc.
                                                              April 2015


             PKIX, PKCS, CMS 구조의 텍스트 인코딩

초록

   이 문서는 공개키 기반구조 X.509(Public-Key Infrastructure X.509,
   PKIX), 공개키 암호화 표준(Public-Key Cryptography Standards, PKCS),
   암호화 메시지 구문(Cryptographic Message Syntax, CMS)의 텍스트 인코딩을
   설명하고 논의한다. 이 텍스트 인코딩은 널리 알려져 있고, 여러 애플리케이션과
   라이브러리에 구현되어 있으며, 광범위하게 배포되어 있다. 이 문서는 기존
   구현체들이 동작하는 사실상의(de facto) 규칙을 명확히 기술하고, 향후
   구현체들이 상호운용될 수 있도록 이를 정의한다.

이 메모의 상태

   이 문서는 인터넷 표준 트랙(Internet Standards Track) 문서이다.

   이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물이다. 이 문서는
   IETF 커뮤니티의 합의를 나타낸다. 이 문서는 공개 검토를 거쳤으며, 인터넷
   엔지니어링 운영 그룹(IESG)에 의해 출판이 승인되었다. 인터넷 표준에 대한
   추가 정보는 RFC 5741의 2절에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는 방법에
   관한 정보는 http://www.rfc-editor.org/info/rfc7468 에서 얻을 수 있다.

저작권 고지

   Copyright (c) 2015 IETF Trust and the persons identified as the
   document authors.  All rights reserved.

   이 문서는 BCP 78 및 이 문서의 출판일에 유효한 IETF Trust의 IETF 문서에
   관한 법적 조항(http://trustee.ietf.org/license-info)의 적용을 받는다.
   이 문서들은 이 문서에 관한 귀하의 권리와 제한 사항을 기술하므로 주의 깊게
   검토하기 바란다. 이 문서에서 추출된 코드 구성요소(Code Components)는
   Trust 법적 조항의 4.e절에 기술된 대로 Simplified BSD License 텍스트를
   반드시 포함해야 하며, Simplified BSD License에 기술된 대로 보증 없이
   제공된다.



Josefsson & Leonard          Standards Track                    [Page 1]

RFC 7468                 PKIX Textual Encodings               April 2015


목차

   1.  서론  . . . . . . . . . . . . . . . . . . . . . . . . . . . .   2
   2.  일반적 고려사항  . . . . . . . . . . . . . . . . . . . . . . .   3
   3.  ABNF  . . . . . . . . . . . . . . . . . . . . . . . . . . . .   5
   4.  안내 . . . . . . . . . . . . . . . . . . . . . . . . . . . . .   7
   5.  인증서의 텍스트 인코딩  . . . . . . . . . . . . . . . . . . . .   8
     5.1.  인코딩  . . . . . . . . . . . . . . . . . . . . . . . . .   8
     5.2.  설명 텍스트  . . . . . . . . . . . . . . . . . . . . . . .   9
     5.3.  파일 확장자  . . . . . . . . . . . . . . . . . . . . . . .   9
   6.  인증서 폐기 목록의 텍스트 인코딩  . . . . . . . . . . . . . . .  10
   7.  PKCS #10 인증 요청 구문의 텍스트 인코딩 . . . . . . . . . . . .  11
   8.  PKCS #7 암호화 메시지 구문의 텍스트 인코딩  . . . . . . . . . .  11
   9.  암호화 메시지 구문의 텍스트 인코딩  . . . . . . . . . . . . . .  12
   10. One Asymmetric Key 및 PKCS #8 Private Key Info의
       텍스트 인코딩  . . . . . . . . . . . . . . . . . . . . . . . .  12
   11. PKCS #8 Encrypted Private Key Info의 텍스트 인코딩  . . . . . .  13
   12. 속성 인증서의 텍스트 인코딩 . . . . . . . . . . . . . . . . . .  13
   13. Subject Public Key Info의 텍스트 인코딩 . . . . . . . . . . . .  14
   14. 보안 고려사항 . . . . . . . . . . . . . . . . . . . . . . . . .  14
   15. 참고문헌  . . . . . . . . . . . . . . . . . . . . . . . . . . .  14
     15.1.  규범적 참고문헌 . . . . . . . . . . . . . . . . . . . . .  14
     15.2.  참고용 참고문헌 . . . . . . . . . . . . . . . . . . . . .  15
   부록 A.  비준수 예시  . . . . . . . . . . . . . . . . . . . . . . .  17
   부록 B.  DER에 대한 기대사항 . . . . . . . . . . . . . . . . . . .  18
   감사의 글  . . . . . . . . . . . . . . . . . . . . . . . . . . . .  19
   저자 주소  . . . . . . . . . . . . . . . . . . . . . . . . . . . .  20

1.  서론

   인터넷에서 사용되는 여러 보안 관련 표준들은 보통 기본 인코딩 규칙(Basic
   Encoding Rules, BER) 또는 구별 인코딩 규칙(Distinguished Encoding
   Rules, DER) [X.690]을 사용하여 인코딩되는 ASN.1 데이터 형식을 정의하는데,
   이들은 이진(binary), 옥텟 지향(octet-oriented) 인코딩이다. 이 문서는 다음
   형식들의 텍스트 인코딩에 관한 것이다:

   1.  인터넷 X.509 공개키 기반구조 인증서 및 인증서 폐기 목록(CRL) 프로파일
       [RFC5280]의 인증서(Certificate), 인증서 폐기 목록(Certificate
       Revocation List, CRL), Subject Public Key Info 구조.

   2.  PKCS #10: 인증 요청 구문(Certification Request Syntax) [RFC2986].

   3.  PKCS #7: 암호화 메시지 구문(Cryptographic Message Syntax)
       [RFC2315].

   4.  암호화 메시지 구문(Cryptographic Message Syntax) [RFC5652].




Josefsson & Leonard          Standards Track                    [Page 2]

RFC 7468                 PKIX Textual Encodings               April 2015


   5.  PKCS #8: Private-Key Information Syntax [RFC5208]. 이는
       Asymmetric Key Package [RFC5958]에서 One Asymmetric Key로
       명칭이 변경되었으며, 같은 문서들에서 Encrypted Private-Key
       Information Syntax도 정의된다.

   6.  An Internet Attribute Certificate Profile for Authorization
       [RFC5755]의 속성 인증서(Attribute Certificate).

   이진 데이터 형식의 단점은 이메일이나 텍스트 문서와 같은 텍스트 전송
   수단으로 교환될 수 없다는 점이다. 텍스트 기반 인코딩의 한 가지 장점은
   일반적인 텍스트 편집기를 사용하여 손쉽게 수정할 수 있다는 것이다. 예를 들어,
   사용자는 복사-붙여넣기 작업으로 여러 인증서를 이어 붙여 인증서 체인을 구성할
   수 있다.

   RFC 시리즈 내의 전통은 Marshall Rose가 Message Encapsulation [RFC934]에서
   제안한 내용에 기반한 Privacy-Enhanced Mail(PEM) [RFC1421]까지 거슬러
   올라갈 수 있다. 원래 "PEM encapsulation mechanism", "encapsulated PEM
   message", 또는 (논쟁의 여지가 있지만) "PEM printable encoding"이라
   불렸으며, 오늘날 이 형식은 때때로 "PEM encoding"이라고 불린다. 그 변형으로는
   OpenPGP ASCII armor [RFC4880]와 OpenSSH 키 파일 형식 [RFC4716]이 있다.

   기본적으로 비조정(non-coordination) 또는 부주의로 귀결되는 이유들로 인해,
   많은 PKIX, PKCS, CMS 라이브러리들은 PEM 인코딩과 유사하지만 동일하지는
   않은 텍스트 기반 인코딩을 구현한다. 이 문서는 _텍스트 인코딩_ 형식을
   명세하고, 대부분의 구현체가 동작하는 사실상의(de facto) 규칙을 명확히
   기술하며, 앞으로의 상호운용성을 증진할 권고사항을 제공한다. 또한 이 문서는
   이 사실상 표준 형식의 진화를 반영하여 구문 요소들에 대한 공통 명명법을
   제공한다. Peter Gutmann의 "X.509 Style Guide" [X.509SG]에는 형식들을
   설명하고 이 문서와 유사한 제안들을 담고 있는 "base64 Encoding"이라는 절이
   있다. 모든 그림은 실제로 동작하는 기능적 예시이며, 키 길이와 내부 내용은
   실용적으로 가능한 한 작게 선택되었다.

   이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL
   NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED",
   "MAY", "OPTIONAL"은 RFC 2119 [RFC2119]에 기술된 대로 해석되어야 한다.

2.  일반적 고려사항

   텍스트 인코딩은 "-----BEGIN ", 레이블(label), "-----"로 구성된 줄로
   시작하고, "-----END ", 레이블, "-----"로 구성된 줄로 끝난다. 이 줄들,
   즉 "캡슐화 경계(encapsulation boundaries)" 사이에는 [RFC4648]의 4절에
   따라 base64로 인코딩된 데이터가 위치한다. (PEM [RFC1421]은 이 데이터를



Josefsson & Leonard          Standards Track                    [Page 3]

RFC 7468                 PKIX Textual Encodings               April 2015


   "캡슐화된 텍스트 부분(encapsulated text portion)"이라고 불렀다.) 캡슐화
   경계 앞의 데이터는 허용되며, 파서(parser)는 그러한 데이터를 처리할 때
   오작동해서는 안 된다(MUST NOT). 더 나아가, 파서는 공백(whitespace) 및
   기타 비-base64 문자를 무시해야 하며(SHOULD), 서로 다른 개행(newline)
   관례를 처리해야 한다(MUST).

   인코딩된 데이터의 유형은 "-----BEGIN " 줄(캡슐화 이전 경계,
   pre-encapsulation boundary)의 유형 레이블에 따라 표시된다. 예를 들어,
   해당 줄이 "-----BEGIN CERTIFICATE-----"라면 그 내용이 PKIX 인증서임을
   나타낸다(아래에서 더 자세히 설명한다). 생성기(generator)는 "-----END "
   줄(캡슐화 이후 경계, post-encapsulation boundary)에 대응하는
   "-----BEGIN " 줄과 동일한 레이블을 넣어야 한다(MUST). 레이블은 형식적으로
   대소문자를 구별하며, 대문자이고, 0개 이상의 문자로 구성된다. 레이블은 연속된
   공백이나 하이픈-마이너스(hyphen-minus)를 포함하지 않으며, 양쪽 끝에 공백이나
   하이픈-마이너스를 포함하지도 않는다. 파서는 레이블 불일치가 있을 경우 오류를
   알리는 대신 캡슐화 이후 경계의 레이블을 무시해도 된다(MAY). 일부 기존
   구현체는 레이블이 일치할 것을 요구하지만, 그렇지 않은 구현체도 있다.

   "BEGIN" 또는 "END"와 레이블 사이에는 정확히 하나의 공백 문자(SP)가 있다.
   캡슐화 경계의 양쪽 끝에는 정확히 다섯 개의 하이픈-마이너스(대시(dash)라고도
   함) 문자("-")가 있으며, 그 이상도 그 이하도 아니다.

   레이블 유형은 인코딩된 데이터가 명시된 구문을 따른다는 것을 함의한다.
   파서는 비준수(non-conforming) 데이터를 우아하게(gracefully) 처리해야
   한다(MUST). 그러나 이 문서 이전의 모든 파서나 생성기가 일관되게 동작하지는
   않는다. 준수하는 파서는 내용을 다른 레이블 유형으로 해석해도 되지만(MAY),
   보안 고려사항 절에서 논의하는 보안 함의를 인식하고 있어야 한다. 이 문서에서
   설명하는 레이블들은 특정 암호화 알고리즘에 한정되지 않는 컨테이너 형식을
   식별하며, 이는 알고리즘 민첩성(algorithm agility)과 일관되는 속성이다. 이
   형식들은 [RFC5280]의 4.1.1.2절에 기술된 ASN.1 AlgorithmIdentifier 구조를
   사용한다.

   레거시 PEM 인코딩 [RFC1421], OpenPGP ASCII armor, OpenSSH 키 파일 형식과
   달리, 텍스트 인코딩은 데이터와 함께 헤더가 인코딩되는 것을 정의하거나
   허용하지 *않는다*. 캡슐화 이전 경계와 base64 사이에 빈 공간이 나타날 수
   있지만, 생성기는 그러한 공백을 출력해서는 안 된다(SHOULD NOT). (이 빈 영역에
   대한 규정은 "캡슐화된 헤더 부분(encapsulated header portion)"을 정의했던
   PEM에서 유래한 과거의 잔재이다.)

   구현자는 기존 파서들이 공백 처리에 있어 상당히 다르다는 점을 인식해야 한다.
   이 문서에서 "공백(whitespace)"은 활자 조판에서 수평 또는 수직 공간을
   나타내는 임의의 문자 또는 일련의 문자를 의미한다. US-ASCII에서 공백은
   HT(0x09), VT(0x0B), FF(0x0C), SP(0x20), CR



Josefsson & Leonard          Standards Track                    [Page 4]

RFC 7468                 PKIX Textual Encodings               April 2015


   (0x0D), LF(0x0A)를 의미한다. "blank"는 HT와 SP를 의미한다. 줄은 CRLF, CR,
   또는 LF로 나뉜다. 일반적인 ABNF 생성 규칙 WSP는 "blank"와 합치하며,
   "whitespace"를 위해 새로운 생성 규칙 W가 사용된다. 3절의 ABNF는
   US-ASCII에 한정된다. 이 텍스트 인코딩들은 여러 다양한 시스템뿐만 아니라
   종이나 조각(engraving)과 같은 장기 보존용 저장 매체에서도 사용될 수 있으므로,
   구현자는 엄밀히 US-ASCII로 한정되지 않는 환경에서 이 형식들을 생성하거나
   파싱할 때 규칙의 문구보다는 그 정신(spirit)을 따라야 한다.

   대부분의 기존 파서는 줄 끝의 blank를 무시한다. 줄 시작 부분이나 base64
   인코딩된 데이터 중간의 blank는 호환성이 훨씬 떨어진다. 이러한 관찰은
   그림 1에 성문화되어 있다. 가장 느슨한 파서 구현은 줄 지향(line-oriented)이
   전혀 아니며 캡슐화 경계 바깥의 어떤 공백 조합이든 받아들인다(그림 2 참조).
   그러한 느슨한 파싱은 애초에 받아들이려고 의도하지 않았던 텍스트(예: 그것이
   일부 단편이거나 샘플이었기 때문)를 받아들일 위험을 감수할 수 있다.

   생성기는 base64 인코딩된 줄을 줄바꿈하여 각 줄이 정확히 64자로 구성되도록
   해야 한다(MUST). 다만 마지막 줄은 (64자 줄 경계 내에서) 데이터의 나머지를
   인코딩하며, 생성기는 불필요한 공백을 출력해서는 안 된다(MUST NOT). 파서는
   다른 줄 크기를 처리해도 된다(MAY). 이 요구사항들은 PEM [RFC1421]과
   일관된다.

   파일은 여러 개의 텍스트 인코딩 인스턴스를 포함해도 된다(MAY). 이는 예를 들어
   파일이 여러 인증서를 포함할 때 사용된다. 인스턴스들이 정렬되어 있는지
   여부는 맥락에 따라 다르다.

3.  ABNF

   텍스트 인코딩의 ABNF [RFC5234]는 다음과 같다:

  textualmsg = preeb *WSP eol
               *eolWSP
               base64text
               posteb *WSP [eol]

  preeb      = "-----BEGIN " label "-----" ; unlike [RFC1421] (A)BNF,
                                           ; eol is not required (but
  posteb     = "-----END " label "-----"   ; see [RFC1421], Section 4.4)

  base64char = ALPHA / DIGIT / "+" / "/"

  base64pad  = "="

  base64line = 1*base64char *WSP eol



Josefsson & Leonard          Standards Track                    [Page 5]

RFC 7468                 PKIX Textual Encodings               April 2015


  base64finl = *base64char (base64pad *WSP eol base64pad /
                            *2base64pad) *WSP eol
                       ; ...AB= <EOL> = <EOL> is not good, but is valid

  base64text = *base64line base64finl
         ; we could also use <encbinbody> from RFC 1421, which requires
         ; 16 groups of 4 chars, which means exactly 64 chars per
         ; line, except the final line, but this is more accurate

  labelchar  = %x21-2C / %x2E-7E    ; any printable character,
                                    ; except hyphen-minus

  label      = [ labelchar *( ["-" / SP] labelchar ) ]       ; empty ok

  eol        = CRLF / CR / LF

  eolWSP     = WSP / CR / LF                        ; compare with LWSP

                         Figure 1: ABNF (Standard)

   laxtextualmsg    = *W preeb
                      laxbase64text
                      posteb *W

   W                = WSP / CR / LF / %x0B / %x0C           ; whitespace

   laxbase64text    = *(W / base64char) [base64pad *W [base64pad *W]]

                           Figure 2: ABNF (Lax)

   stricttextualmsg = preeb eol
                      strictbase64text
                      posteb eol

   strictbase64finl = *15(4base64char) (4base64char / 3base64char
                        base64pad / 2base64char 2base64pad) eol

   base64fullline   = 64base64char eol

   strictbase64text = *base64fullline strictbase64finl

                          Figure 3: ABNF (Strict)

   새로운 구현체는 위에 명시된 엄격(strict) 형식(그림 3)을 출력해야 한다
   (SHOULD). 파싱 전략의 선택은 사용 맥락에 따라 다르다.




Josefsson & Leonard          Standards Track                    [Page 6]

RFC 7468                 PKIX Textual Encodings               April 2015


4.  안내

   편의를 위해, 다음 그림들은 이후 절들에 나오는 구조, 인코딩, 참고문헌을
   요약한다:

Sec. Label                  ASN.1 Type              Reference Module
----+----------------------+-----------------------+---------+----------
  5  CERTIFICATE            Certificate             [RFC5280] id-pkix1-e
  6  X509 CRL               CertificateList         [RFC5280] id-pkix1-e
  7  CERTIFICATE REQUEST    CertificationRequest    [RFC2986] id-pkcs10
  8  PKCS7                  ContentInfo             [RFC2315] id-pkcs7*
  9  CMS                    ContentInfo             [RFC5652] id-cms2004
 10  PRIVATE KEY            PrivateKeyInfo ::=      [RFC5208] id-pkcs8
                            OneAsymmetricKey        [RFC5958] id-aKPV1
 11  ENCRYPTED PRIVATE KEY  EncryptedPrivateKeyInfo [RFC5958] id-aKPV1
 12  ATTRIBUTE CERTIFICATE  AttributeCertificate    [RFC5755] id-acv2
 13  PUBLIC KEY             SubjectPublicKeyInfo    [RFC5280] id-pkix1-e

                        Figure 4: Convenience Guide

 -----------------------------------------------------------------------
 id-pkixmod OBJECT IDENTIFIER ::= {iso(1) identified-organization(3)
            dod(6) internet(1) security(5) mechanisms(5) pkix(7) mod(0)}
 id-pkix1-e OBJECT IDENTIFIER ::= {id-pkixmod pkix1-explicit(18)}
 id-acv2    OBJECT IDENTIFIER ::= {id-pkixmod mod-attribute-cert-v2(61)}
 id-pkcs    OBJECT IDENTIFIER ::= {iso(1) member-body(2) us(840)
                                   rsadsi(113549) pkcs(1)}
 id-pkcs10  OBJECT IDENTIFIER ::= {id-pkcs 10 modules(1) pkcs-10(1)}
 id-pkcs7   OBJECT IDENTIFIER ::= {id-pkcs 7 modules(0) pkcs-7(1)}
 id-pkcs8   OBJECT IDENTIFIER ::= {id-pkcs 8 modules(1) pkcs-8(1)}
 id-sm-mod  OBJECT IDENTIFIER ::= {id-pkcs 9 smime(16) modules(0)}
 id-aKPV1   OBJECT IDENTIFIER ::= {id-sm-mod mod-asymmetricKeyPkgV1(50)}
 id-cms2004 OBJECT IDENTIFIER ::= {id-sm-mod cms-2004(24)}

   * 이 OID는 실제로는 PKCS #7 v1.5 [RFC2315]에 나타나지 않는다. 이 OID는
   PKCS #7 v1.6 [P7v1.6]의 ASN.1 모듈에서 정의되었으며, PKCS #12 [RFC7292]를
   통해 계승되어 왔다.

        Figure 5: ASN.1 Module Object Identifier Value Assignments








Josefsson & Leonard          Standards Track                    [Page 7]

RFC 7468                 PKIX Textual Encodings               April 2015


5.  인증서의 텍스트 인코딩

5.1.  인코딩

   공개키 인증서는 "CERTIFICATE" 레이블을 사용하여 인코딩된다. 인코딩된
   데이터는 [RFC5280]의 4절에 기술된 대로 BER(DER 강력 권장; 부록 B 참조)로
   인코딩된 ASN.1 Certificate 구조여야 한다(MUST).

-----BEGIN CERTIFICATE-----
MIICLDCCAdKgAwIBAgIBADAKBggqhkjOPQQDAjB9MQswCQYDVQQGEwJCRTEPMA0G
A1UEChMGR251VExTMSUwIwYDVQQLExxHbnVUTFMgY2VydGlmaWNhdGUgYXV0aG9y
aXR5MQ8wDQYDVQQIEwZMZXV2ZW4xJTAjBgNVBAMTHEdudVRMUyBjZXJ0aWZpY2F0
ZSBhdXRob3JpdHkwHhcNMTEwNTIzMjAzODIxWhcNMTIxMjIyMDc0MTUxWjB9MQsw
CQYDVQQGEwJCRTEPMA0GA1UEChMGR251VExTMSUwIwYDVQQLExxHbnVUTFMgY2Vy
dGlmaWNhdGUgYXV0aG9yaXR5MQ8wDQYDVQQIEwZMZXV2ZW4xJTAjBgNVBAMTHEdu
dVRMUyBjZXJ0aWZpY2F0ZSBhdXRob3JpdHkwWTATBgcqhkjOPQIBBggqhkjOPQMB
BwNCAARS2I0jiuNn14Y2sSALCX3IybqiIJUvxUpj+oNfzngvj/Niyv2394BWnW4X
uQ4RTEiywK87WRcWMGgJB5kX/t2no0MwQTAPBgNVHRMBAf8EBTADAQH/MA8GA1Ud
DwEB/wQFAwMHBgAwHQYDVR0OBBYEFPC0gf6YEr+1KLlkQAPLzB9mTigDMAoGCCqG
SM49BAMCA0gAMEUCIDGuwD1KPyG+hRf88MeyMQcqOFZD0TbVleF+UsAGQ4enAiEA
l4wOuDwKQa+upc8GftXE2C//4mKANBC6It01gUaTIpo=
-----END CERTIFICATE-----

                       Figure 6: Certificate Example

   역사적으로 레이블 "X509 CERTIFICATE"가 사용되었고, 또한 덜 일반적으로
   "X.509 CERTIFICATE"도 사용되었다. 이 문서를 준수하는 생성기는
   "CERTIFICATE" 레이블을 생성해야 하며(MUST), "X509 CERTIFICATE"나
   "X.509 CERTIFICATE" 레이블을 생성해서는 안 된다(MUST NOT). 파서는
   "X509 CERTIFICATE"나 "X.509 CERTIFICATE"를 "CERTIFICATE"와 동등하게
   취급해서는 안 되지만(SHOULD NOT), 하위 호환성을 위한 (잠재적으로 경고와
   함께 하는) 유효한 예외는 있을 수 있다.

















Josefsson & Leonard          Standards Track                    [Page 8]

RFC 7468                 PKIX Textual Encodings               April 2015


5.2.  설명 텍스트

   많은 도구들이 PKIX 인증서에 대해 다른 어떤 유형보다도 더 많이, BEGIN 줄
   앞과 END 줄 뒤에 설명 텍스트(explanatory text)를 출력하는 것으로 알려져
   있다. 그러한 텍스트가 출력된다면, 인증서 내 핵심 데이터 요소들의 텍스트
   표현을 제공하는 등 인증서와 관련된 것이어야 한다(SHOULD).

Subject: CN=Atlantis
Issuer: CN=Atlantis
Validity: from 7/9/2012 3:10:38 AM UTC to 7/9/2013 3:10:37 AM UTC
-----BEGIN CERTIFICATE-----
MIIBmTCCAUegAwIBAgIBKjAJBgUrDgMCHQUAMBMxETAPBgNVBAMTCEF0bGFudGlz
MB4XDTEyMDcwOTAzMTAzOFoXDTEzMDcwOTAzMTAzN1owEzERMA8GA1UEAxMIQXRs
YW50aXMwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAu+BXo+miabDIHHx+yquqzqNh
Ryn/XtkJIIHVcYtHvIX+S1x5ErgMoHehycpoxbErZmVR4GCq1S2diNmRFZCRtQID
AQABo4GJMIGGMAwGA1UdEwEB/wQCMAAwIAYDVR0EAQH/BBYwFDAOMAwGCisGAQQB
gjcCARUDAgeAMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDAzA1BgNVHQEE
LjAsgBA0jOnSSuIHYmnVryHAdywMoRUwEzERMA8GA1UEAxMIQXRsYW50aXOCASow
CQYFKw4DAh0FAANBAKi6HRBaNEL5R0n56nvfclQNaXiDT174uf+lojzA4lhVInc0
ILwpnZ1izL4MlI9eCSHhVQBHEp2uQdXJB+d5Byg=
-----END CERTIFICATE-----

            Figure 7: Certificate Example with Explanatory Text

5.3.  파일 확장자

   PKIX 구조의 텍스트 인코딩은 어디에서나 나타날 수 있지만, 많은 도구들이
   PKIX 구조를 직렬화할 때 이 인코딩을 출력하는 옵션을 제공하는 것으로 알려져
   있다. 상호운용성을 증진하고 DER 인코딩을 텍스트 인코딩과 분리하기 위해,
   인증서의 텍스트 인코딩에는 확장자 ".crt"를 사용해야 한다(SHOULD). 구현자는
   이 권고에도 불구하고 많은 도구들이 여전히 이 텍스트 인코딩으로 인코딩된
   인증서를 기본적으로 확장자 ".cer"로 저장한다는 점을 인식해야 한다.

   이 절은 공식적인 application/pkix-cert 등록 [RFC2585](이는 "각 '.cer'
   파일은 DER 형식으로 인코딩된 정확히 하나의 인증서를 포함한다"고 명시한다)을
   어떤 식으로든 침해하지 않으며, 단지 널리 퍼진 사실상의 대안을 명확히
   기술할 뿐이다.










Josefsson & Leonard          Standards Track                    [Page 9]

RFC 7468                 PKIX Textual Encodings               April 2015


6.  인증서 폐기 목록의 텍스트 인코딩

   인증서 폐기 목록(Certificate Revocation List, CRL)은 "X509 CRL"
   레이블을 사용하여 인코딩된다. 인코딩된 데이터는 [RFC5280]의 5절에 기술된
   대로 BER(DER 강력 권장; 부록 B 참조)로 인코딩된 ASN.1 CertificateList
   구조여야 한다(MUST).

-----BEGIN X509 CRL-----
MIIB9DCCAV8CAQEwCwYJKoZIhvcNAQEFMIIBCDEXMBUGA1UEChMOVmVyaVNpZ24s
IEluYy4xHzAdBgNVBAsTFlZlcmlTaWduIFRydXN0IE5ldHdvcmsxRjBEBgNVBAsT
PXd3dy52ZXJpc2lnbi5jb20vcmVwb3NpdG9yeS9SUEEgSW5jb3JwLiBieSBSZWYu
LExJQUIuTFREKGMpOTgxHjAcBgNVBAsTFVBlcnNvbmEgTm90IFZhbGlkYXRlZDEm
MCQGA1UECxMdRGlnaXRhbCBJRCBDbGFzcyAxIC0gTmV0c2NhcGUxGDAWBgNVBAMU
D1NpbW9uIEpvc2Vmc3NvbjEiMCAGCSqGSIb3DQEJARYTc2ltb25Aam9zZWZzc29u
Lm9yZxcNMDYxMjI3MDgwMjM0WhcNMDcwMjA3MDgwMjM1WjAjMCECEC4QNwPfRoWd
elUNpllhhTgXDTA2MTIyNzA4MDIzNFowCwYJKoZIhvcNAQEFA4GBAD0zX+J2hkcc
Nbrq1Dn5IKL8nXLgPGcHv1I/le1MNo9t1ohGQxB5HnFUkRPAY82fR6Epor4aHgVy
b+5y+neKN9Kn2mPF4iiun+a4o26CjJ0pArojCL1p8T0yyi9Xxvyc/ezaZ98HiIyP
c3DGMNR+oUmSjKZ0jIhAYmeLxaPHfQwR
-----END X509 CRL-----

                           Figure 8: CRL Example

   역사적으로 레이블 "CRL"은 거의 사용되지 않았다. 오늘날 그것은 흔하지 않으며
   많은 인기 도구들이 그 레이블을 이해하지 못한다. 따라서 이 문서는
   상호운용성과 하위 호환성을 증진하기 위해 "X509 CRL"을 표준화한다. 이 문서를
   준수하는 생성기는 "X509 CRL" 레이블을 생성해야 하며(MUST), "CRL" 레이블을
   생성해서는 안 된다(MUST NOT). 파서는 "CRL"을 "X509 CRL"과 동등하게
   취급해서는 안 된다(SHOULD NOT).

















Josefsson & Leonard          Standards Track                   [Page 10]

RFC 7468                 PKIX Textual Encodings               April 2015


7.  PKCS #10 인증 요청 구문의 텍스트 인코딩

   PKCS #10 인증 요청(Certification Request)은 "CERTIFICATE REQUEST"
   레이블을 사용하여 인코딩된다. 인코딩된 데이터는 [RFC2986]에 기술된 대로
   BER(DER 강력 권장; 부록 B 참조)로 인코딩된 ASN.1 CertificationRequest
   구조여야 한다(MUST).

-----BEGIN CERTIFICATE REQUEST-----
MIIBWDCCAQcCAQAwTjELMAkGA1UEBhMCU0UxJzAlBgNVBAoTHlNpbW9uIEpvc2Vm
c3NvbiBEYXRha29uc3VsdCBBQjEWMBQGA1UEAxMNam9zZWZzc29uLm9yZzBOMBAG
ByqGSM49AgEGBSuBBAAhAzoABLLPSkuXY0l66MbxVJ3Mot5FCFuqQfn6dTs+9/CM
EOlSwVej77tj56kj9R/j9Q+LfysX8FO9I5p3oGIwYAYJKoZIhvcNAQkOMVMwUTAY
BgNVHREEETAPgg1qb3NlZnNzb24ub3JnMAwGA1UdEwEB/wQCMAAwDwYDVR0PAQH/
BAUDAwegADAWBgNVHSUBAf8EDDAKBggrBgEFBQcDATAKBggqhkjOPQQDAgM/ADA8
AhxBvfhxPFfbBbsE1NoFmCUczOFApEuQVUw3ZP69AhwWXk3dgSUsKnuwL5g/ftAY
dEQc8B8jAcnuOrfU
-----END CERTIFICATE REQUEST-----

                        Figure 9: PKCS #10 Example

   레이블 "NEW CERTIFICATE REQUEST" 또한 널리 사용된다. 이 문서를 준수하는
   생성기는 "CERTIFICATE REQUEST" 레이블을 생성해야 한다(MUST). 파서는
   "NEW CERTIFICATE REQUEST"를 "CERTIFICATE REQUEST"와 동등하게 취급해도
   된다(MAY).

8.  PKCS #7 암호화 메시지 구문의 텍스트 인코딩

   PKCS #7 암호화 메시지 구문(Cryptographic Message Syntax) 구조는 "PKCS7"
   레이블을 사용하여 인코딩된다. 인코딩된 데이터는 [RFC2315]에 기술된 대로
   BER로 인코딩된 ASN.1 ContentInfo 구조여야 한다(MUST).

-----BEGIN PKCS7-----
MIHjBgsqhkiG9w0BCRABF6CB0zCB0AIBADFho18CAQCgGwYJKoZIhvcNAQUMMA4E
CLfrI6dr0gUWAgITiDAjBgsqhkiG9w0BCRADCTAUBggqhkiG9w0DBwQIZpECRWtz
u5kEGDCjerXY8odQ7EEEromZJvAurk/j81IrozBSBgkqhkiG9w0BBwEwMwYLKoZI
hvcNAQkQAw8wJDAUBggqhkiG9w0DBwQI0tCBcU09nxEwDAYIKwYBBQUIAQIFAIAQ
OsYGYUFdAH0RNc1p4VbKEAQUM2Xo8PMHBoYdqEcsbTodlCFAZH4=
-----END PKCS7-----

                        Figure 10: PKCS #7 Example

   레이블 "CERTIFICATE CHAIN"은 인증서 목록만을 포함하는 퇴화된(degenerate)
   PKCS #7 구조를 나타내기 위해 사용되어 왔다([RFC2315]의 9절 참조). 여러
   현대 도구들은 이 레이블을 지원하지 않는다. 생성기는 "CERTIFICATE CHAIN"
   레이블을 생성해서는 안 된다(MUST NOT). 파서는 "CERTIFICATE CHAIN"을
   "PKCS7"과 동등하게 취급해서는 안 된다(SHOULD NOT).




Josefsson & Leonard          Standards Track                   [Page 11]

RFC 7468                 PKIX Textual Encodings               April 2015


   PKCS #7은 CMS [RFC5652]로 오래전에 대체된 오래된 명세이다. 구현체는 CMS가
   대안으로 존재할 때 PKCS #7을 생성해서는 안 된다(SHOULD NOT).

9.  암호화 메시지 구문의 텍스트 인코딩

   암호화 메시지 구문(Cryptographic Message Syntax) 구조는 "CMS" 레이블을
   사용하여 인코딩된다. 인코딩된 데이터는 [RFC5652]에 기술된 대로 BER로
   인코딩된 ASN.1 ContentInfo 구조여야 한다(MUST).

-----BEGIN CMS-----
MIGDBgsqhkiG9w0BCRABCaB0MHICAQAwDQYLKoZIhvcNAQkQAwgwXgYJKoZIhvcN
AQcBoFEET3icc87PK0nNK9ENqSxItVIoSa0o0S/ISczMs1ZIzkgsKk4tsQ0N1nUM
dvb05OXi5XLPLEtViMwvLVLwSE0sKlFIVHAqSk3MBkkBAJv0Fx0=
-----END CMS-----

                          Figure 11: CMS Example

   CMS는 PKCS #7의 IETF 후속(successor)이다. [RFC5652]의 1.1.1절은 PKCS #7
   v1.5 이후의 변경 사항을 설명한다. 구현체는 CMS가 대안으로 존재할 때 CMS를
   생성하여 상호운용성과 상위 호환성(forwards-compatibility)을 증진해야
   한다(SHOULD).

10.  One Asymmetric Key 및 PKCS #8 Private Key Info의 텍스트 인코딩

   암호화되지 않은 PKCS #8 Private Key Information Syntax 구조
   (PrivateKeyInfo)는 Asymmetric Key Packages(OneAsymmetricKey)로 명칭이
   변경되었으며, "PRIVATE KEY" 레이블을 사용하여 인코딩된다. 인코딩된 데이터는
   PKCS #8 [RFC5208]에 기술된 대로 BER(DER 권장; 부록 B 참조)로 인코딩된
   ASN.1 PrivateKeyInfo 구조이거나, [RFC5958]에 기술된 대로
   OneAsymmetricKey 구조여야 한다(MUST). 이 둘은 의미상 동일하며 버전 번호로
   구별할 수 있다.

-----BEGIN PRIVATE KEY-----
MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgVcB/UNPxalR9zDYAjQIf
jojUDiQuGnSJrFEEzZPT/92hRANCAASc7UJtgnF/abqWM60T3XNJEzBv5ez9TdwK
H0M6xpM2q+53wmsN/eYLdgtjgBd3DBmHtPilCkiFICXyaA8z9LkJ
-----END PRIVATE KEY-----

       Figure 12: PKCS #8 PrivateKeyInfo (OneAsymmetricKey) Example








Josefsson & Leonard          Standards Track                   [Page 12]

RFC 7468                 PKIX Textual Encodings               April 2015


11.  PKCS #8 Encrypted Private Key Info의 텍스트 인코딩

   암호화된 PKCS #8 Private Key Information Syntax 구조
   (EncryptedPrivateKeyInfo)는 [RFC5958]에서도 동일하게 불리며,
   "ENCRYPTED PRIVATE KEY" 레이블을 사용하여 인코딩된다. 인코딩된 데이터는
   PKCS #8 [RFC5208]과 [RFC5958]에 기술된 대로 BER(DER 권장; 부록 B 참조)로
   인코딩된 ASN.1 EncryptedPrivateKeyInfo 구조여야 한다(MUST).

-----BEGIN ENCRYPTED PRIVATE KEY-----
MIHNMEAGCSqGSIb3DQEFDTAzMBsGCSqGSIb3DQEFDDAOBAghhICA6T/51QICCAAw
FAYIKoZIhvcNAwcECBCxDgvI59i9BIGIY3CAqlMNBgaSI5QiiWVNJ3IpfLnEiEsW
Z0JIoHyRmKK/+cr9QPLnzxImm0TR9s4JrG3CilzTWvb0jIvbG3hu0zyFPraoMkap
8eRzWsIvC5SVel+CSjoS2mVS87cyjlD+txrmrXOVYDE+eTgMLbrLmsWh3QkCTRtF
QC7k0NNzUHTV9yGDwfqMbw==
-----END ENCRYPTED PRIVATE KEY-----

            Figure 13: PKCS #8 EncryptedPrivateKeyInfo Example

12.  속성 인증서의 텍스트 인코딩

   속성 인증서(Attribute Certificate)는 "ATTRIBUTE CERTIFICATE" 레이블을
   사용하여 인코딩된다. 인코딩된 데이터는 [RFC5755]에 기술된 대로 BER(DER
   강력 권장; 부록 B 참조)로 인코딩된 ASN.1 AttributeCertificate 구조여야
   한다(MUST).

-----BEGIN ATTRIBUTE CERTIFICATE-----
MIICKzCCAZQCAQEwgZeggZQwgYmkgYYwgYMxCzAJBgNVBAYTAlVTMREwDwYDVQQI
DAhOZXcgWW9yazEUMBIGA1UEBwwLU3RvbnkgQnJvb2sxDzANBgNVBAoMBkNTRTU5
MjE6MDgGA1UEAwwxU2NvdHQgU3RhbGxlci9lbWFpbEFkZHJlc3M9c3N0YWxsZXJA
aWMuc3VueXNiLmVkdQIGARWrgUUSoIGMMIGJpIGGMIGDMQswCQYDVQQGEwJVUzER
MA8GA1UECAwITmV3IFlvcmsxFDASBgNVBAcMC1N0b255IEJyb29rMQ8wDQYDVQQK
DAZDU0U1OTIxOjA4BgNVBAMMMVNjb3R0IFN0YWxsZXIvZW1haWxBZGRyZXNzPXNz
dGFsbGVyQGljLnN1bnlzYi5lZHUwDQYJKoZIhvcNAQEFBQACBgEVq4FFSjAiGA8z
OTA3MDIwMTA1MDAwMFoYDzM5MTEwMTMxMDUwMDAwWjArMCkGA1UYSDEiMCCGHmh0
dHA6Ly9pZGVyYXNobi5vcmcvaW5kZXguaHRtbDANBgkqhkiG9w0BAQUFAAOBgQAV
M9axFPXXozEFcer06bj9MCBBCQLtAM7ZXcZjcxyva7xCBDmtZXPYUluHf5OcWPJz
5XPus/xS9wBgtlM3fldIKNyNO8RsMp6Ocx+PGlICc7zpZiGmCYLl64lAEGPO/bsw
Smluak1aZIttePeTAHeJJs8izNJ5aR3Wcd3A5gLztQ==
-----END ATTRIBUTE CERTIFICATE-----

                 Figure 14: Attribute Certificate Example









Josefsson & Leonard          Standards Track                   [Page 13]

RFC 7468                 PKIX Textual Encodings               April 2015


13.  Subject Public Key Info의 텍스트 인코딩

   공개키는 "PUBLIC KEY" 레이블을 사용하여 인코딩된다. 인코딩된 데이터는
   [RFC5280]의 4.1.2.7절에 기술된 대로 BER(DER 권장; 부록 B 참조)로
   인코딩된 ASN.1 SubjectPublicKeyInfo 구조여야 한다(MUST).

-----BEGIN PUBLIC KEY-----
MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEn1LlwLN/KBYQRVH6HfIMTzfEqJOVztLe
kLchp2hi78cCaMY81FBlYs8J9l7krc+M4aBeCGYFjba+hiXttJWPL7ydlE+5UG4U
Nkn3Eos8EiZByi9DVsyfy9eejh+8AXgp
-----END PUBLIC KEY-----

                Figure 15: Subject Public Key Info Example

14.  보안 고려사항

   이 형식의 데이터는 종종 신뢰할 수 없는 출처에서 비롯되므로, 파서는 보안
   취약점을 유발하지 않고 예기치 않은 데이터를 처리할 준비가 되어 있어야 한다.

   정규(canonical) 표현이나 특정 데이터 객체의 지문(fingerprint)을 생성하는
   능력에 의존하는 구현체를 만드는 구현자는, 이 문서가 정규 인코딩을 정의하지
   않는다는 점을 이해해야 한다. 첫 번째 모호성은 이진 BER 또는 DER 인코딩 대신
   텍스트 인코딩 표현을 허용함으로써 발생하지만, 여러 레이블이 유사하게
   취급될 때 추가적인 모호성이 발생한다. 공백 및 비-base64 알파벳 문자의
   변형은 추가적인 모호성을 만들 수 있다. 데이터 인코딩 모호성은 또한 측면
   채널(side channel)의 기회를 만든다. 정규 인코딩이 필요하다면, 인코딩된
   구조를 디코딩하여 정규 형식(즉, DER 인코딩)으로 처리해야 한다.

15.  참고문헌

15.1.  규범적 참고문헌

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119, March 1997,
              <http://www.rfc-editor.org/info/rfc2119>.

   [RFC2315]  Kaliski, B., "PKCS #7: Cryptographic Message Syntax
              Version 1.5", RFC 2315, March 1998,
              <http://www.rfc-editor.org/info/rfc2315>.

   [RFC2986]  Nystrom, M. and B. Kaliski, "PKCS #10: Certification
              Request Syntax Specification Version 1.7", RFC 2986,
              November 2000, <http://www.rfc-editor.org/info/rfc2986>.



Josefsson & Leonard          Standards Track                   [Page 14]

RFC 7468                 PKIX Textual Encodings               April 2015


   [RFC4648]  Josefsson, S., "The Base16, Base32, and Base64 Data
              Encodings", RFC 4648, October 2006,
              <http://www.rfc-editor.org/info/rfc4648>.

   [RFC5280]  Cooper, D., Santesson, S., Farrell, S., Boeyen, S.,
              Housley, R., and W. Polk, "Internet X.509 Public Key
              Infrastructure Certificate and Certificate Revocation List
              (CRL) Profile", RFC 5280, May 2008,
              <http://www.rfc-editor.org/info/rfc5280>.

   [RFC5234]  Crocker, D., Ed. and P. Overell, "Augmented BNF for Syntax
              Specifications: ABNF", STD 68, RFC 5234, January 2008,
              <http://www.rfc-editor.org/info/rfc5234>.

   [RFC5652]  Housley, R., "Cryptographic Message Syntax (CMS)", STD 70,
              RFC 5652, September 2009,
              <http://www.rfc-editor.org/info/rfc5652>.

   [RFC5755]  Farrell, S., Housley, R., and S. Turner, "An Internet
              Attribute Certificate Profile for Authorization", RFC
              5755, January 2010,
              <http://www.rfc-editor.org/info/rfc5755>.

   [RFC5958]  Turner, S., "Asymmetric Key Packages", RFC 5958, August
              2010, <http://www.rfc-editor.org/info/rfc5958>.

   [X.690]    International Telecommunications Union, "Information
              Technology - ASN.1 encoding rules: Specification of Basic
              Encoding Rules (BER), Canonical Encoding Rules (CER) and
              Distinguished Encoding Rules (DER)", ITU-T Recommendation
              X.690, ISO/IEC 8825-1:2008, November 2008.

15.2.  참고용 참고문헌

   [RFC934]   Rose, M. and E. Stefferud, "Proposed standard for message
              encapsulation", RFC 934, January 1985,
              <http://www.rfc-editor.org/info/rfc934>.

   [RFC1421]  Linn, J., "Privacy Enhancement for Internet Electronic
              Mail: Part I: Message Encryption and Authentication
              Procedures", RFC 1421, February 1993,
              <http://www.rfc-editor.org/info/rfc1421>.

   [RFC2585]  Housley, R. and P. Hoffman, "Internet X.509 Public Key
              Infrastructure Operational Protocols: FTP and HTTP", RFC
              2585, May 1999, <http://www.rfc-editor.org/info/rfc2585>.




Josefsson & Leonard          Standards Track                   [Page 15]

RFC 7468                 PKIX Textual Encodings               April 2015


   [RFC4716]  Galbraith, J. and R. Thayer, "The Secure Shell (SSH)
              Public Key File Format", RFC 4716, November 2006,
              <http://www.rfc-editor.org/info/rfc4716>.

   [RFC4880]  Callas, J., Donnerhacke, L., Finney, H., Shaw, D., and R.
              Thayer, "OpenPGP Message Format", RFC 4880, November 2007,
              <http://www.rfc-editor.org/info/rfc4880>.

   [RFC5208]  Kaliski, B., "Public-Key Cryptography Standards (PKCS) #8:
              Private-Key Information Syntax Specification Version 1.2",
              RFC 5208, May 2008,
              <http://www.rfc-editor.org/info/rfc5208>.

   [RFC7292]  Moriarty, K., Ed., Nystrom, M., Parkinson, S., Rusch, A.,
              and M. Scott, "PKCS #12: Personal Information Exchange
              Syntax v1.1", RFC 7292, July 2014,
              <http://www.rfc-editor.org/info/rfc7292>.

   [P7v1.6]   Kaliski, B. and K. Kingdon, "Extensions and Revisions to
              PKCS #7 (Version 1.6 Bulletin)", May 1997,
              <http://www.emc.com/emc-plus/rsa-labs/
              standards-initiatives/pkcs-7-cryptographic-message-
              syntax-standar.htm>.

   [X.509SG]  Gutmann, P., "X.509 Style Guide", October 2000,
              <http://www.cs.auckland.ac.nz/~pgut001/pubs/
              x509guide.txt>.

















Josefsson & Leonard          Standards Track                   [Page 16]

RFC 7468                 PKIX Textual Encodings               April 2015


부록 A.  비준수 예시

   이 부록은 이 문서 앞부분에서 설명한, 권장되지 않는 레이블 변형들에 대한
   예시를 담고 있다. 앞서 논의한 바와 같이, 이들을 지원하는 것은 요구되지
   않으며 때로는 권장되지 않는다. 그럼에도 불구하고 이들은 상호운용성 테스트와
   손쉬운 참조를 위해 유용할 수 있다.

-----BEGIN X509 CERTIFICATE-----
MIIBHDCBxaADAgECAgIcxzAJBgcqhkjOPQQBMBAxDjAMBgNVBAMUBVBLSVghMB4X
DTE0MDkxNDA2MTU1MFoXDTI0MDkxNDA2MTU1MFowEDEOMAwGA1UEAxQFUEtJWCEw
WTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATwoQSr863QrR0PoRIYQ96H7WykDePH
Wa0eVAE24bth43wCNc+U5aZ761dhGhSSJkVWRgVH5+prLIr+nzfIq+X4oxAwDjAM
BgNVHRMBAf8EAjAAMAkGByqGSM49BAEDRwAwRAIfMdKS5F63lMnWVhi7uaKJzKCs
NnY/OKgBex6MIEAv2AIhAI2GdvfL+mGvhyPZE+JxRxWChmggb5/9eHdUcmW/jkOH
-----END X509 CERTIFICATE-----

            Figure 16: Non-standard 'X509' Certificate Example

-----BEGIN X.509 CERTIFICATE-----
MIIBHDCBxaADAgECAgIcxzAJBgcqhkjOPQQBMBAxDjAMBgNVBAMUBVBLSVghMB4X
DTE0MDkxNDA2MTU1MFoXDTI0MDkxNDA2MTU1MFowEDEOMAwGA1UEAxQFUEtJWCEw
WTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATwoQSr863QrR0PoRIYQ96H7WykDePH
Wa0eVAE24bth43wCNc+U5aZ761dhGhSSJkVWRgVH5+prLIr+nzfIq+X4oxAwDjAM
BgNVHRMBAf8EAjAAMAkGByqGSM49BAEDRwAwRAIfMdKS5F63lMnWVhi7uaKJzKCs
NnY/OKgBex6MIEAv2AIhAI2GdvfL+mGvhyPZE+JxRxWChmggb5/9eHdUcmW/jkOH
-----END X.509 CERTIFICATE-----

            Figure 17: Non-standard 'X.509' Certificate Example

-----BEGIN NEW CERTIFICATE REQUEST-----
MIIBWDCCAQcCAQAwTjELMAkGA1UEBhMCU0UxJzAlBgNVBAoTHlNpbW9uIEpvc2Vm
c3NvbiBEYXRha29uc3VsdCBBQjEWMBQGA1UEAxMNam9zZWZzc29uLm9yZzBOMBAG
ByqGSM49AgEGBSuBBAAhAzoABLLPSkuXY0l66MbxVJ3Mot5FCFuqQfn6dTs+9/CM
EOlSwVej77tj56kj9R/j9Q+LfysX8FO9I5p3oGIwYAYJKoZIhvcNAQkOMVMwUTAY
BgNVHREEETAPgg1qb3NlZnNzb24ub3JnMAwGA1UdEwEB/wQCMAAwDwYDVR0PAQH/
BAUDAwegADAWBgNVHSUBAf8EDDAKBggrBgEFBQcDATAKBggqhkjOPQQDAgM/ADA8
AhxBvfhxPFfbBbsE1NoFmCUczOFApEuQVUw3ZP69AhwWXk3dgSUsKnuwL5g/ftAY
dEQc8B8jAcnuOrfU
-----END NEW CERTIFICATE REQUEST-----

              Figure 18: Non-standard 'NEW' PKCS #10 Example









Josefsson & Leonard          Standards Track                   [Page 17]

RFC 7468                 PKIX Textual Encodings               April 2015


-----BEGIN CERTIFICATE CHAIN-----
MIHjBgsqhkiG9w0BCRABF6CB0zCB0AIBADFho18CAQCgGwYJKoZIhvcNAQUMMA4E
CLfrI6dr0gUWAgITiDAjBgsqhkiG9w0BCRADCTAUBggqhkiG9w0DBwQIZpECRWtz
u5kEGDCjerXY8odQ7EEEromZJvAurk/j81IrozBSBgkqhkiG9w0BBwEwMwYLKoZI
hvcNAQkQAw8wJDAUBggqhkiG9w0DBwQI0tCBcU09nxEwDAYIKwYBBQUIAQIFAIAQ
OsYGYUFdAH0RNc1p4VbKEAQUM2Xo8PMHBoYdqEcsbTodlCFAZH4=
-----END CERTIFICATE CHAIN-----

            Figure 19: Non-standard 'CERTIFICATE CHAIN' Example

부록 B.  DER에 대한 기대사항

   이 부록은 참고용(informative)이다. 규범적 규칙에 대해서는 각각의 해당
   표준을 참조하라.

   DER은 BER [X.690]의 제한된 프로파일이다. 따라서 데이터 값의 모든 DER
   인코딩은 BER 인코딩이지만, BER 인코딩 중 단 하나만이 어떤 데이터 값에 대한
   DER 인코딩이다. 정규(canonical) 인코딩은 암호화 연산을 수행할 때 중요하다.
   추가로, 정규 인코딩은 파서에게 특정한 효율성 이점을 제공한다. DER로
   인코딩하는 데에는 세 가지 주요 이유가 있다:

   1.  디지털 서명은 의미적 내용(semantic content)의 DER 인코딩에 대해
       계산되도록 (되어 있어야) 하므로, DER 인코딩이 아닌 다른 것을 제공하는
       것은 무의미하다. (실제로 구현자는 구현체가 데이터를 있는 그대로 파싱하고
       다이제스트하도록 선택할 수도 있지만, 이러한 관행은 추측에 불과하다.)

   2.  실제로, 암호화 해시는 식별을 위해 DER 인코딩에 대해 계산된다.

   3.  실제로, 내용은 작다. DER은 데이터 값을 항상 확정 길이
       (definite-length) 형식(길이가 인코딩의 시작 부분에 명시되는 형식)으로
       인코딩한다. 따라서 파서는 메모리나 자원 사용량을 미리 예측할 수 있다.













Josefsson & Leonard          Standards Track                   [Page 18]

RFC 7468                 PKIX Textual Encodings               April 2015


   그림 20은 이 문서의 구조들을 DER 인코딩의 특정한 이유와 대응시킨다:

                    Sec. Label                  Reasons
                    ----+----------------------+-------
                      5  CERTIFICATE            1  2 ~3
                      6  X509 CRL               1
                      7  CERTIFICATE REQUEST    1    ~3
                      8  PKCS7                  *
                      9  CMS                    *
                     10  PRIVATE KEY                  3
                     11  ENCRYPTED PRIVATE KEY        3
                     12  ATTRIBUTE CERTIFICATE  1    ~3
                     13  PUBLIC KEY                2  3

                     Figure 20: Guide for DER Encoding

   * 암호화 메시지 구문(Cryptographic Message Syntax)은 임의 길이의 내용을
     위해 설계되었다. 부정 길이(indefinite-length) 인코딩은 인코딩을 생성할 때
     단일 패스 처리(원패스 처리, 스트리밍)를 가능하게 한다. 특정 부분, 즉
     서명된(signed) 속성과 인증된(authenticated) 속성만이 DER로 인코딩되어야
     한다.

   ~ 항상 "작지는" 않지만, 이 인코딩된 구조들은 특별히 "크지"(예: 16킬로바이트
     이상) 않아야 한다. 어떤 경우든 파서는 큰 것들에 대해 미리 통지받아야 한다.
     이것은 애초에 이것들을 DER로 인코딩해야 하는 또 다른 이유이다.

                     Figure 20: Guide for DER Encoding

감사의 글

   Peter Gutmann은 속성 인증서(Attribute Certificate)와 PKCS #7 메시지에
   대한 레이블을 문서화하고, 비표준 변형들에 대한 예시를 추가할 것을 제안했다.
   Dr. Stephen Henson은 언제 BER 대 DER이 적절하거나 필요한지를 구별할 것을
   제안했다.













Josefsson & Leonard          Standards Track                   [Page 19]

RFC 7468                 PKIX Textual Encodings               April 2015


저자 주소

   Simon Josefsson
   SJD AB
   Johan Olof Wallins Vaeg 13
   Solna  171 64
   Sweden

   EMail: simon@josefsson.org
   URI:   http://josefsson.org/


   Sean Leonard
   Penango, Inc.
   5900 Wilshire Boulevard
   21st Floor
   Los Angeles, CA  90036
   United States

   EMail: dev+ietf@seantek.com
   URI:   http://www.penango.com/

















Josefsson & Leonard          Standards Track                   [Page 20]
