# RFC 7468 - PKIX, PKCS, CMS 구조의 텍스트 인코딩

```
Internet Engineering Task Force (IETF)                      S. Josefsson
Request for Comments: 7468                                        SJD AB
Category: Standards Track                                     S. Leonard
ISSN: 2070-1721                                            Penango, Inc.
                                                              April 2015
```

### Abstract

- 이 문서: 공개키 기반구조 X.509(Public-Key Infrastructure X.509, PKIX)· 공개키 암호화 표준(Public-Key Cryptography Standards, PKCS)· 암호화 메시지 구문(Cryptographic Message Syntax, CMS)의 텍스트 인코딩 설명·논의
- 이 텍스트 인코딩: 널리 알려짐, 여러 애플리케이션·라이브러리에 구현됨, 광범위하게 배포됨
- 이 문서 목적: 기존 구현체들이 동작하는 사실상의(de facto) 규칙 명확화 → 향후 구현체 간 상호운용성 확보

## 이 메모의 상태

- 이 문서: 인터넷 표준 트랙(Internet Standards Track) 문서
- IETF(인터넷 엔지니어링 태스크 포스)의 산출물, IETF 커뮤니티의 합의 반영
- 공개 검토 완료, IESG(인터넷 엔지니어링 운영 그룹)에 의해 출판 승인
- 인터넷 표준 관련 추가 정보: RFC 5741 2절 참고
- 이 문서의 현재 상태·정오표·피드백 방법: http://www.rfc-editor.org/info/rfc7468 에서 확인 가능

## 저작권 고지

Copyright (c) 2015 IETF Trust and the persons identified as the document authors. All rights reserved.

- 이 문서: BCP 78 및 이 문서의 출판일에 유효한 IETF Trust의 IETF 문서에 관한 법적 조항(http://trustee.ietf.org/license-info)의 적용 대상 → 이 문서들이 권리·제한 사항을 기술하므로 주의 깊게 검토 필요
- 이 문서에서 추출된 코드 구성요소(Code Components): Trust 법적 조항 4.e절에 기술된 대로 Simplified BSD License 텍스트 포함 필수, Simplified BSD License에 기술된 대로 보증 없이 제공

## 목차

- 1. 서론
- 2. 일반적 고려사항
- 3. ABNF
- 4. 안내
- 5. 인증서의 텍스트 인코딩
  - 5.1. 인코딩
  - 5.2. 설명 텍스트
  - 5.3. 파일 확장자
- 6. 인증서 폐기 목록의 텍스트 인코딩
- 7. PKCS #10 인증 요청 구문의 텍스트 인코딩
- 8. PKCS #7 암호화 메시지 구문의 텍스트 인코딩
- 9. 암호화 메시지 구문의 텍스트 인코딩
- 10. One Asymmetric Key 및 PKCS #8 Private Key Info의 텍스트 인코딩
- 11. PKCS #8 Encrypted Private Key Info의 텍스트 인코딩
- 12. 속성 인증서의 텍스트 인코딩
- 13. Subject Public Key Info의 텍스트 인코딩
- 14. 보안 고려사항
- 15. 참고문헌
  - 15.1. 규범적 참고문헌
  - 15.2. 참고용 참고문헌
- 부록 A. 비준수 예시
- 부록 B. DER에 대한 기대사항
- 감사의 글
- 저자 주소

## 1. 서론

- 인터넷에서 사용되는 여러 보안 관련 표준: ASN.1 데이터 형식 정의 → 기본 인코딩 규칙(Basic Encoding Rules, BER) 또는 구별 인코딩 규칙(Distinguished Encoding Rules, DER) [X.690]으로 인코딩 → 이진(binary)·옥텟 지향(octet-oriented) 인코딩임
- 이 문서가 다루는 텍스트 인코딩 대상:
  1. 인터넷 X.509 공개키 기반구조 인증서 및 인증서 폐기 목록(CRL) 프로파일 [RFC5280]의 인증서(Certificate)· 인증서 폐기 목록(Certificate Revocation List, CRL)· Subject Public Key Info 구조
  2. PKCS #10: 인증 요청 구문(Certification Request Syntax) [RFC2986]
  3. PKCS #7: 암호화 메시지 구문(Cryptographic Message Syntax) [RFC2315]
  4. 암호화 메시지 구문(Cryptographic Message Syntax) [RFC5652]
  5. PKCS #8: Private-Key Information Syntax [RFC5208] → Asymmetric Key Package [RFC5958]에서 One Asymmetric Key로 명칭 변경, 같은 문서에서 Encrypted Private-Key Information Syntax도 정의
  6. An Internet Attribute Certificate Profile for Authorization [RFC5755]의 속성 인증서(Attribute Certificate)

- 이진 데이터 형식의 단점: 이메일·텍스트 문서 같은 텍스트 전송 수단으로 교환 불가
- 텍스트 기반 인코딩의 장점: 일반적인 텍스트 편집기로 손쉽게 수정 가능 → 예: 복사-붙여넣기로 여러 인증서를 이어 붙여 인증서 체인 구성 가능

- RFC 시리즈 내 전통: Marshall Rose가 Message Encapsulation [RFC934]에서 제안한 내용 → Privacy-Enhanced Mail(PEM) [RFC1421]까지 이어짐
- 원래 명칭: "PEM encapsulation mechanism"· "encapsulated PEM message"· (논쟁의 여지가 있지만) "PEM printable encoding" → 오늘날은 "PEM encoding"으로도 통칭
- 그 변형: OpenPGP ASCII armor [RFC4880]와 OpenSSH 키 파일 형식 [RFC4716]

- 배경: 비조정(non-coordination) 또는 부주의로 귀결되는 이유들 → 많은 PKIX, PKCS, CMS 라이브러리가 PEM 인코딩과 유사하지만 동일하지는 않은 텍스트 기반 인코딩 구현
- 이 문서의 목적: _텍스트 인코딩_ 형식 명세, 대부분 구현체가 동작하는 사실상의(de facto) 규칙 명확화, 향후 상호운용성 증진을 위한 권고사항 제공, 사실상 표준 형식의 진화를 반영한 구문 요소 공통 명명법 제공
- Peter Gutmann의 "X.509 Style Guide" [X.509SG]: 형식들을 설명하는 "base64 Encoding" 절에 이 문서와 유사한 제안 포함
- 모든 그림: 실제로 동작하는 기능적 예시, 키 길이·내부 내용은 실용적으로 가능한 한 작게 선택

- 이 문서의 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL": RFC 2119 [RFC2119]에 기술된 대로 해석

## 2. 일반적 고려사항

- 텍스트 인코딩 구조: "-----BEGIN ", 레이블(label), "-----"로 구성된 줄로 시작 → "-----END ", 레이블, "-----"로 구성된 줄로 종료
- 두 줄("캡슐화 경계", encapsulation boundaries) 사이: [RFC4648] 4절에 따라 base64로 인코딩된 데이터 위치 (PEM [RFC1421]은 이를 "캡슐화된 텍스트 부분(encapsulated text portion)"이라 칭함)
- 캡슐화 경계 앞의 데이터: 허용, 파서는 그런 데이터 처리 시 오작동 금지(MUST NOT)
- 파서: 공백(whitespace) 및 기타 비-base64 문자 무시 권장(SHOULD), 서로 다른 개행(newline) 관례 처리 필수(MUST)

- 인코딩된 데이터의 유형: "-----BEGIN " 줄(캡슐화 이전 경계, pre-encapsulation boundary)의 유형 레이블로 표시 (예: "-----BEGIN CERTIFICATE-----" → PKIX 인증서를 나타냄, 아래에서 상세 설명)
- 생성기(generator): "-----END " 줄(캡슐화 이후 경계, post-encapsulation boundary)에 대응하는 "-----BEGIN " 줄과 동일한 레이블 사용 필수(MUST)
- 레이블 규칙: 형식적으로 대소문자 구별, 대문자, 0개 이상의 문자로 구성, 연속된 공백·하이픈-마이너스 불가, 양쪽 끝 공백·하이픈-마이너스 불가
- 파서: 레이블 불일치 시 오류를 알리는 대신 캡슐화 이후 경계의 레이블을 무시 가능(MAY) (일부 기존 구현체는 레이블 일치를 요구, 그렇지 않은 구현체도 있음)

- "BEGIN" 또는 "END"와 레이블 사이: 공백 문자(SP) 정확히 1개
- 캡슐화 경계 양쪽 끝: 하이픈-마이너스(대시(dash)) 문자("-") 정확히 5개, 그 이상도 이하도 아님

- 레이블 유형: 인코딩된 데이터가 명시된 구문을 따른다는 것을 함의
- 파서: 비준수(non-conforming) 데이터를 우아하게(gracefully) 처리 필수(MUST) (다만 이 문서 이전의 모든 파서·생성기가 일관되게 동작하지는 않음)
- 준수하는 파서: 내용을 다른 레이블 유형으로 해석 가능(MAY), 단 보안 고려사항 절에서 논의하는 보안 함의 인식 필요
- 이 문서에서 설명하는 레이블: 특정 암호화 알고리즘에 한정되지 않는 컨테이너 형식 식별 → 알고리즘 민첩성(algorithm agility)과 일관되는 속성
- 이 형식들: [RFC5280] 4.1.1.2절에 기술된 ASN.1 AlgorithmIdentifier 구조 사용

- 레거시 PEM 인코딩 [RFC1421]· OpenPGP ASCII armor· OpenSSH 키 파일 형식과의 차이점: 텍스트 인코딩은 데이터와 함께 헤더가 인코딩되는 것을 정의·허용하지 않음
- 캡슐화 이전 경계와 base64 사이의 빈 공간: 나타날 수 있으나 생성기는 그러한 공백 출력 금지 권장(SHOULD NOT) (이 규정은 "캡슐화된 헤더 부분(encapsulated header portion)"을 정의했던 PEM에서 유래한 과거의 잔재)

- 공백(whitespace) 처리: 기존 파서들이 상당히 다름 → 구현자 인식 필요
- 이 문서의 "공백(whitespace)" 정의: 활자 조판에서 수평·수직 공간을 나타내는 임의의 문자 또는 문자열
  - US-ASCII 기준: HT(0x09)· VT(0x0B)· FF(0x0C)· SP(0x20)· CR(0x0D)· LF(0x0A)
  - "blank": HT와 SP
  - 줄 구분: CRLF, CR, 또는 LF
- ABNF 생성 규칙: 일반 규칙 WSP는 "blank"와 합치, "whitespace"를 위한 새 생성 규칙 W 사용 (3절의 ABNF는 US-ASCII에 한정)
- 이 텍스트 인코딩들: 다양한 시스템뿐 아니라 종이·조각(engraving) 같은 장기 보존용 저장 매체에서도 사용 가능 → 엄밀히 US-ASCII로 한정되지 않는 환경에서는 규칙의 문구보다 그 정신(spirit)을 따라야 함

- 대부분의 기존 파서: 줄 끝의 blank 무시
- 줄 시작 부분·base64 인코딩 데이터 중간의 blank: 호환성 훨씬 저하 (그림 1에 성문화)
- 가장 느슨한 파서 구현: 줄 지향(line-oriented)이 전혀 아님, 캡슐화 경계 바깥의 어떤 공백 조합이든 허용(그림 2 참고) → 애초에 받아들이려 의도하지 않았던 텍스트(예: 단편·샘플)를 받아들일 위험 감수 가능

- 생성기: base64 인코딩된 줄을 줄바꿈하여 각 줄이 정확히 64자로 구성되도록 처리 필수(MUST) (다만 마지막 줄은 64자 줄 경계 내에서 나머지 데이터를 인코딩, 생성기는 불필요한 공백 출력 금지(MUST NOT))
- 파서: 다른 줄 크기 처리 가능(MAY)
- 이 요구사항들: PEM [RFC1421]과 일관

- 파일: 여러 개의 텍스트 인코딩 인스턴스 포함 가능(MAY) (예: 파일이 여러 인증서 포함) → 인스턴스 정렬 여부는 맥락에 따라 다름

## 3. ABNF

텍스트 인코딩의 ABNF [RFC5234]는 다음과 같음.

```
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
```

그림 1: ABNF (Standard)

```
laxtextualmsg    = *W preeb
                   laxbase64text
                   posteb *W

W                = WSP / CR / LF / %x0B / %x0C           ; whitespace

laxbase64text    = *(W / base64char) [base64pad *W [base64pad *W]]
```

그림 2: ABNF (Lax)

```
stricttextualmsg = preeb eol
                   strictbase64text
                   posteb eol

strictbase64finl = *15(4base64char) (4base64char / 3base64char
                     base64pad / 2base64char 2base64pad) eol

base64fullline   = 64base64char eol

strictbase64text = *base64fullline strictbase64finl
```

그림 3: ABNF (Strict)

- 새로운 구현체: 위에 명시된 엄격(strict) 형식(그림 3) 출력 권장(SHOULD)
- 파싱 전략의 선택: 사용 맥락에 따라 다름

## 4. 안내

편의를 위해, 다음 목록은 이후 절들에 나오는 구조·인코딩·참고문헌을 요약함(그림 4: Convenience Guide).

- 5절 CERTIFICATE: ASN.1 타입 Certificate, 참고문헌 [RFC5280], 모듈 id-pkix1-e
- 6절 X509 CRL: ASN.1 타입 CertificateList, 참고문헌 [RFC5280], 모듈 id-pkix1-e
- 7절 CERTIFICATE REQUEST: ASN.1 타입 CertificationRequest, 참고문헌 [RFC2986], 모듈 id-pkcs10
- 8절 PKCS7: ASN.1 타입 ContentInfo, 참고문헌 [RFC2315], 모듈 id-pkcs7*
- 9절 CMS: ASN.1 타입 ContentInfo, 참고문헌 [RFC5652], 모듈 id-cms2004
- 10절 PRIVATE KEY: ASN.1 타입 PrivateKeyInfo ::= OneAsymmetricKey, 참고문헌 [RFC5208]/[RFC5958], 모듈 id-pkcs8/id-aKPV1
- 11절 ENCRYPTED PRIVATE KEY: ASN.1 타입 EncryptedPrivateKeyInfo, 참고문헌 [RFC5958], 모듈 id-aKPV1
- 12절 ATTRIBUTE CERTIFICATE: ASN.1 타입 AttributeCertificate, 참고문헌 [RFC5755], 모듈 id-acv2
- 13절 PUBLIC KEY: ASN.1 타입 SubjectPublicKeyInfo, 참고문헌 [RFC5280], 모듈 id-pkix1-e

```
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
```

- * 표시: 이 OID는 실제로는 PKCS #7 v1.5 [RFC2315]에 나타나지 않음 → PKCS #7 v1.6 [P7v1.6]의 ASN.1 모듈에서 정의됨, PKCS #12 [RFC7292]를 통해 계승됨

그림 5: ASN.1 Module Object Identifier Value Assignments

## 5. 인증서의 텍스트 인코딩

### 5.1. 인코딩

- 공개키 인증서: "CERTIFICATE" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC5280] 4절에 기술된 대로 BER(DER 강력 권장, 부록 B 참고)로 인코딩된 ASN.1 Certificate 구조 필수(MUST)

```
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
```

그림 6: Certificate Example

- 역사적 레이블: "X509 CERTIFICATE" 사용됨, (덜 일반적으로) "X.509 CERTIFICATE"도 사용됨
- 이 문서 준수 생성기: "CERTIFICATE" 레이블 생성 필수(MUST), "X509 CERTIFICATE"·"X.509 CERTIFICATE" 레이블 생성 금지(MUST NOT)
- 파서: "X509 CERTIFICATE"·"X.509 CERTIFICATE"를 "CERTIFICATE"와 동등하게 취급 금지 권장(SHOULD NOT) (다만 하위 호환성을 위한 유효한 예외는 있을 수 있음, 경고 병행 가능)

### 5.2. 설명 텍스트

- 다수 도구: PKIX 인증서에 대해 다른 어떤 유형보다도 더 많이, BEGIN 줄 앞·END 줄 뒤에 설명 텍스트(explanatory text) 출력하는 것으로 알려짐
- 그런 텍스트가 출력된다면: 인증서 내 핵심 데이터 요소들의 텍스트 표현을 제공하는 등 인증서와 관련된 내용이어야 함 권장(SHOULD)

```
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
```

그림 7: Certificate Example with Explanatory Text

### 5.3. 파일 확장자

- PKIX 구조의 텍스트 인코딩: 어디에서나 나타날 수 있음, 다수 도구가 PKIX 구조 직렬화 시 이 인코딩 출력 옵션 제공하는 것으로 알려짐
- 상호운용성 증진·DER 인코딩을 텍스트 인코딩과 분리하기 위한 목적 → 인증서의 텍스트 인코딩에는 확장자 ".crt" 사용 권장(SHOULD)
- 주의: 이 권고에도 불구하고 많은 도구들이 여전히 이 텍스트 인코딩으로 인코딩된 인증서를 기본적으로 확장자 ".cer"로 저장

- 이 절: 공식 application/pkix-cert 등록 [RFC2585](이는 "각 '.cer' 파일은 DER 형식으로 인코딩된 정확히 하나의 인증서를 포함한다"고 명시)을 어떤 식으로든 침해하지 않음 → 단지 널리 퍼진 사실상의 대안을 명확히 기술할 뿐

## 6. 인증서 폐기 목록의 텍스트 인코딩

- 인증서 폐기 목록(Certificate Revocation List, CRL): "X509 CRL" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC5280] 5절에 기술된 대로 BER(DER 강력 권장, 부록 B 참고)로 인코딩된 ASN.1 CertificateList 구조 필수(MUST)

```
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
```

그림 8: CRL Example

- 역사적으로 레이블 "CRL": 거의 사용되지 않음, 오늘날도 흔하지 않음, 다수 인기 도구가 그 레이블 이해 못함
- 이 문서: 상호운용성·하위 호환성 증진 목적으로 "X509 CRL" 표준화
- 이 문서 준수 생성기: "X509 CRL" 레이블 생성 필수(MUST), "CRL" 레이블 생성 금지(MUST NOT)
- 파서: "CRL"을 "X509 CRL"과 동등하게 취급 금지 권장(SHOULD NOT)

## 7. PKCS #10 인증 요청 구문의 텍스트 인코딩

- PKCS #10 인증 요청(Certification Request): "CERTIFICATE REQUEST" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC2986]에 기술된 대로 BER(DER 강력 권장, 부록 B 참고)로 인코딩된 ASN.1 CertificationRequest 구조 필수(MUST)

```
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
```

그림 9: PKCS #10 Example

- 레이블 "NEW CERTIFICATE REQUEST": 또한 널리 사용됨
- 이 문서 준수 생성기: "CERTIFICATE REQUEST" 레이블 생성 필수(MUST)
- 파서: "NEW CERTIFICATE REQUEST"를 "CERTIFICATE REQUEST"와 동등하게 취급 가능(MAY)

## 8. PKCS #7 암호화 메시지 구문의 텍스트 인코딩

- PKCS #7 암호화 메시지 구문(Cryptographic Message Syntax) 구조: "PKCS7" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC2315]에 기술된 대로 BER로 인코딩된 ASN.1 ContentInfo 구조 필수(MUST)

```
-----BEGIN PKCS7-----
MIHjBgsqhkiG9w0BCRABF6CB0zCB0AIBADFho18CAQCgGwYJKoZIhvcNAQUMMA4E
CLfrI6dr0gUWAgITiDAjBgsqhkiG9w0BCRADCTAUBggqhkiG9w0DBwQIZpECRWtz
u5kEGDCjerXY8odQ7EEEromZJvAurk/j81IrozBSBgkqhkiG9w0BBwEwMwYLKoZI
hvcNAQkQAw8wJDAUBggqhkiG9w0DBwQI0tCBcU09nxEwDAYIKwYBBQUIAQIFAIAQ
OsYGYUFdAH0RNc1p4VbKEAQUM2Xo8PMHBoYdqEcsbTodlCFAZH4=
-----END PKCS7-----
```

그림 10: PKCS #7 Example

- 레이블 "CERTIFICATE CHAIN": 인증서 목록만을 포함하는 퇴화된(degenerate) PKCS #7 구조 표기 용도로 사용됨 ([RFC2315] 9절 참고)
- 다수 현대 도구: 이 레이블 미지원
- 생성기: "CERTIFICATE CHAIN" 레이블 생성 금지(MUST NOT)
- 파서: "CERTIFICATE CHAIN"을 "PKCS7"과 동등하게 취급 금지 권장(SHOULD NOT)

- PKCS #7: CMS [RFC5652]로 오래전에 대체된 구 명세
- 구현체: CMS가 대안으로 존재할 때 PKCS #7 생성 금지 권장(SHOULD NOT)

## 9. 암호화 메시지 구문의 텍스트 인코딩

- 암호화 메시지 구문(Cryptographic Message Syntax) 구조: "CMS" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC5652]에 기술된 대로 BER로 인코딩된 ASN.1 ContentInfo 구조 필수(MUST)

```
-----BEGIN CMS-----
MIGDBgsqhkiG9w0BCRABCaB0MHICAQAwDQYLKoZIhvcNAQkQAwgwXgYJKoZIhvcN
AQcBoFEET3icc87PK0nNK9ENqSxItVIoSa0o0S/ISczMs1ZIzkgsKk4tsQ0N1nUM
dvb05OXi5XLPLEtViMwvLVLwSE0sKlFIVHAqSk3MBkkBAJv0Fx0=
-----END CMS-----
```

그림 11: CMS Example

- CMS: PKCS #7의 IETF 후속(successor) ([RFC5652] 1.1.1절에 PKCS #7 v1.5 이후 변경 사항 설명)
- 구현체: CMS가 대안으로 존재할 때 CMS 생성 권장(SHOULD) → 상호운용성·상위 호환성(forwards-compatibility) 증진

## 10. One Asymmetric Key 및 PKCS #8 Private Key Info의 텍스트 인코딩

- 암호화되지 않은 PKCS #8 Private Key Information Syntax 구조(PrivateKeyInfo): Asymmetric Key Packages(OneAsymmetricKey)로 명칭 변경됨, "PRIVATE KEY" 레이블 사용 인코딩
- 인코딩된 데이터: PKCS #8 [RFC5208]에 기술된 대로 BER(DER 권장, 부록 B 참고)로 인코딩된 ASN.1 PrivateKeyInfo 구조, 또는 [RFC5958]에 기술된 대로 OneAsymmetricKey 구조 중 하나 필수(MUST) (둘은 의미상 동일, 버전 번호로 구별 가능)

```
-----BEGIN PRIVATE KEY-----
MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgVcB/UNPxalR9zDYAjQIf
jojUDiQuGnSJrFEEzZPT/92hRANCAASc7UJtgnF/abqWM60T3XNJEzBv5ez9TdwK
H0M6xpM2q+53wmsN/eYLdgtjgBd3DBmHtPilCkiFICXyaA8z9LkJ
-----END PRIVATE KEY-----
```

그림 12: PKCS #8 PrivateKeyInfo (OneAsymmetricKey) Example

## 11. PKCS #8 Encrypted Private Key Info의 텍스트 인코딩

- 암호화된 PKCS #8 Private Key Information Syntax 구조(EncryptedPrivateKeyInfo): [RFC5958]에서도 동일 명칭 사용, "ENCRYPTED PRIVATE KEY" 레이블 사용 인코딩
- 인코딩된 데이터: PKCS #8 [RFC5208]과 [RFC5958]에 기술된 대로 BER(DER 권장, 부록 B 참고)로 인코딩된 ASN.1 EncryptedPrivateKeyInfo 구조 필수(MUST)

```
-----BEGIN ENCRYPTED PRIVATE KEY-----
MIHNMEAGCSqGSIb3DQEFDTAzMBsGCSqGSIb3DQEFDDAOBAghhICA6T/51QICCAAw
FAYIKoZIhvcNAwcECBCxDgvI59i9BIGIY3CAqlMNBgaSI5QiiWVNJ3IpfLnEiEsW
Z0JIoHyRmKK/+cr9QPLnzxImm0TR9s4JrG3CilzTWvb0jIvbG3hu0zyFPraoMkap
8eRzWsIvC5SVel+CSjoS2mVS87cyjlD+txrmrXOVYDE+eTgMLbrLmsWh3QkCTRtF
QC7k0NNzUHTV9yGDwfqMbw==
-----END ENCRYPTED PRIVATE KEY-----
```

그림 13: PKCS #8 EncryptedPrivateKeyInfo Example

## 12. 속성 인증서의 텍스트 인코딩

- 속성 인증서(Attribute Certificate): "ATTRIBUTE CERTIFICATE" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC5755]에 기술된 대로 BER(DER 강력 권장, 부록 B 참고)로 인코딩된 ASN.1 AttributeCertificate 구조 필수(MUST)

```
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
```

그림 14: Attribute Certificate Example

## 13. Subject Public Key Info의 텍스트 인코딩

- 공개키: "PUBLIC KEY" 레이블 사용 인코딩
- 인코딩된 데이터: [RFC5280] 4.1.2.7절에 기술된 대로 BER(DER 권장, 부록 B 참고)로 인코딩된 ASN.1 SubjectPublicKeyInfo 구조 필수(MUST)

```
-----BEGIN PUBLIC KEY-----
MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEn1LlwLN/KBYQRVH6HfIMTzfEqJOVztLe
kLchp2hi78cCaMY81FBlYs8J9l7krc+M4aBeCGYFjba+hiXttJWPL7ydlE+5UG4U
Nkn3Eos8EiZByi9DVsyfy9eejh+8AXgp
-----END PUBLIC KEY-----
```

그림 15: Subject Public Key Info Example

## 14. 보안 고려사항

- 이 형식의 데이터: 신뢰할 수 없는 출처에서 비롯되는 경우가 많음 → 파서는 보안 취약점을 유발하지 않고 예기치 않은 데이터를 처리할 준비 필요
- 정규(canonical) 표현이나 특정 데이터 객체의 지문(fingerprint) 생성 능력에 의존하는 구현체 설계 시 주의: 이 문서는 정규 인코딩을 정의하지 않음
  - 첫 번째 모호성: 이진 BER·DER 인코딩 대신 텍스트 인코딩 표현을 허용함으로써 발생
  - 추가 모호성: 여러 레이블이 유사하게 취급될 때 발생
  - 공백·비-base64 알파벳 문자의 변형: 추가적인 모호성 유발 가능
  - 데이터 인코딩 모호성: 측면 채널(side channel) 공격 기회 생성 가능
- 정규 인코딩 필요 시: 인코딩된 구조를 디코딩하여 정규 형식(즉, DER 인코딩)으로 처리 필요

## 15. 참고문헌

### 15.1. 규범적 참고문헌

- [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, RFC 2119, March 1997, <http://www.rfc-editor.org/info/rfc2119>.
- [RFC2315]  Kaliski, B., "PKCS #7: Cryptographic Message Syntax Version 1.5", RFC 2315, March 1998, <http://www.rfc-editor.org/info/rfc2315>.
- [RFC2986]  Nystrom, M. and B. Kaliski, "PKCS #10: Certification Request Syntax Specification Version 1.7", RFC 2986, November 2000, <http://www.rfc-editor.org/info/rfc2986>.
- [RFC4648]  Josefsson, S., "The Base16, Base32, and Base64 Data Encodings", RFC 4648, October 2006, <http://www.rfc-editor.org/info/rfc4648>.
- [RFC5280]  Cooper, D., Santesson, S., Farrell, S., Boeyen, S., Housley, R., and W. Polk, "Internet X.509 Public Key Infrastructure Certificate and Certificate Revocation List (CRL) Profile", RFC 5280, May 2008, <http://www.rfc-editor.org/info/rfc5280>.
- [RFC5234]  Crocker, D., Ed. and P. Overell, "Augmented BNF for Syntax Specifications: ABNF", STD 68, RFC 5234, January 2008, <http://www.rfc-editor.org/info/rfc5234>.
- [RFC5652]  Housley, R., "Cryptographic Message Syntax (CMS)", STD 70, RFC 5652, September 2009, <http://www.rfc-editor.org/info/rfc5652>.
- [RFC5755]  Farrell, S., Housley, R., and S. Turner, "An Internet Attribute Certificate Profile for Authorization", RFC 5755, January 2010, <http://www.rfc-editor.org/info/rfc5755>.
- [RFC5958]  Turner, S., "Asymmetric Key Packages", RFC 5958, August 2010, <http://www.rfc-editor.org/info/rfc5958>.
- [X.690]    International Telecommunications Union, "Information Technology - ASN.1 encoding rules: Specification of Basic Encoding Rules (BER), Canonical Encoding Rules (CER) and Distinguished Encoding Rules (DER)", ITU-T Recommendation X.690, ISO/IEC 8825-1:2008, November 2008.

### 15.2. 참고용 참고문헌

- [RFC934]   Rose, M. and E. Stefferud, "Proposed standard for message encapsulation", RFC 934, January 1985, <http://www.rfc-editor.org/info/rfc934>.
- [RFC1421]  Linn, J., "Privacy Enhancement for Internet Electronic Mail: Part I: Message Encryption and Authentication Procedures", RFC 1421, February 1993, <http://www.rfc-editor.org/info/rfc1421>.
- [RFC2585]  Housley, R. and P. Hoffman, "Internet X.509 Public Key Infrastructure Operational Protocols: FTP and HTTP", RFC 2585, May 1999, <http://www.rfc-editor.org/info/rfc2585>.
- [RFC4716]  Galbraith, J. and R. Thayer, "The Secure Shell (SSH) Public Key File Format", RFC 4716, November 2006, <http://www.rfc-editor.org/info/rfc4716>.
- [RFC4880]  Callas, J., Donnerhacke, L., Finney, H., Shaw, D., and R. Thayer, "OpenPGP Message Format", RFC 4880, November 2007, <http://www.rfc-editor.org/info/rfc4880>.
- [RFC5208]  Kaliski, B., "Public-Key Cryptography Standards (PKCS) #8: Private-Key Information Syntax Specification Version 1.2", RFC 5208, May 2008, <http://www.rfc-editor.org/info/rfc5208>.
- [RFC7292]  Moriarty, K., Ed., Nystrom, M., Parkinson, S., Rusch, A., and M. Scott, "PKCS #12: Personal Information Exchange Syntax v1.1", RFC 7292, July 2014, <http://www.rfc-editor.org/info/rfc7292>.
- [P7v1.6]   Kaliski, B. and K. Kingdon, "Extensions and Revisions to PKCS #7 (Version 1.6 Bulletin)", May 1997, <http://www.emc.com/emc-plus/rsa-labs/standards-initiatives/pkcs-7-cryptographic-message-syntax-standar.htm>.
- [X.509SG]  Gutmann, P., "X.509 Style Guide", October 2000, <http://www.cs.auckland.ac.nz/~pgut001/pubs/x509guide.txt>.

## 부록 A. 비준수 예시

- 이 부록: 이 문서 앞부분에서 설명한, 권장되지 않는 레이블 변형들의 예시 포함
- 앞서 논의한 바와 같이 지원 필수 아님, 때로는 권장되지 않음
- 다만 상호운용성 테스트·손쉬운 참조 용도로 유용 가능

```
-----BEGIN X509 CERTIFICATE-----
MIIBHDCBxaADAgECAgIcxzAJBgcqhkjOPQQBMBAxDjAMBgNVBAMUBVBLSVghMB4X
DTE0MDkxNDA2MTU1MFoXDTI0MDkxNDA2MTU1MFowEDEOMAwGA1UEAxQFUEtJWCEw
WTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATwoQSr863QrR0PoRIYQ96H7WykDePH
Wa0eVAE24bth43wCNc+U5aZ761dhGhSSJkVWRgVH5+prLIr+nzfIq+X4oxAwDjAM
BgNVHRMBAf8EAjAAMAkGByqGSM49BAEDRwAwRAIfMdKS5F63lMnWVhi7uaKJzKCs
NnY/OKgBex6MIEAv2AIhAI2GdvfL+mGvhyPZE+JxRxWChmggb5/9eHdUcmW/jkOH
-----END X509 CERTIFICATE-----
```

그림 16: Non-standard 'X509' Certificate Example

```
-----BEGIN X.509 CERTIFICATE-----
MIIBHDCBxaADAgECAgIcxzAJBgcqhkjOPQQBMBAxDjAMBgNVBAMUBVBLSVghMB4X
DTE0MDkxNDA2MTU1MFoXDTI0MDkxNDA2MTU1MFowEDEOMAwGA1UEAxQFUEtJWCEw
WTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATwoQSr863QrR0PoRIYQ96H7WykDePH
Wa0eVAE24bth43wCNc+U5aZ761dhGhSSJkVWRgVH5+prLIr+nzfIq+X4oxAwDjAM
BgNVHRMBAf8EAjAAMAkGByqGSM49BAEDRwAwRAIfMdKS5F63lMnWVhi7uaKJzKCs
NnY/OKgBex6MIEAv2AIhAI2GdvfL+mGvhyPZE+JxRxWChmggb5/9eHdUcmW/jkOH
-----END X.509 CERTIFICATE-----
```

그림 17: Non-standard 'X.509' Certificate Example

```
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
```

그림 18: Non-standard 'NEW' PKCS #10 Example

```
-----BEGIN CERTIFICATE CHAIN-----
MIHjBgsqhkiG9w0BCRABF6CB0zCB0AIBADFho18CAQCgGwYJKoZIhvcNAQUMMA4E
CLfrI6dr0gUWAgITiDAjBgsqhkiG9w0BCRADCTAUBggqhkiG9w0DBwQIZpECRWtz
u5kEGDCjerXY8odQ7EEEromZJvAurk/j81IrozBSBgkqhkiG9w0BBwEwMwYLKoZI
hvcNAQkQAw8wJDAUBggqhkiG9w0DBwQI0tCBcU09nxEwDAYIKwYBBQUIAQIFAIAQ
OsYGYUFdAH0RNc1p4VbKEAQUM2Xo8PMHBoYdqEcsbTodlCFAZH4=
-----END CERTIFICATE CHAIN-----
```

그림 19: Non-standard 'CERTIFICATE CHAIN' Example

## 부록 B. DER에 대한 기대사항

- 이 부록: 참고용(informative) → 규범적 규칙은 각각의 해당 표준 참고

- DER: BER [X.690]의 제한된 프로파일 → 데이터 값의 모든 DER 인코딩은 BER 인코딩이지만, 어떤 데이터 값에 대해서는 BER 인코딩 중 단 하나만 DER 인코딩
- 정규(canonical) 인코딩: 암호화 연산 수행 시 중요, 파서에 특정한 효율성 이점 제공
- DER로 인코딩하는 3가지 주요 이유:
  1. 디지털 서명은 의미적 내용(semantic content)의 DER 인코딩에 대해 계산되도록 되어 있어야 함 → DER 인코딩이 아닌 다른 것을 제공하는 것은 무의미 (실제로 구현자는 데이터를 있는 그대로 파싱·다이제스트하도록 선택할 수도 있으나, 이런 관행은 추측성에 불과)
  2. 실제로 암호화 해시는 식별을 위해 DER 인코딩에 대해 계산됨
  3. 실제로 내용이 작음 → DER은 데이터 값을 항상 확정 길이(definite-length) 형식(길이가 인코딩의 시작 부분에 명시되는 형식)으로 인코딩 → 파서는 메모리·자원 사용량을 미리 예측 가능

그림 20은 이 문서의 구조들을 DER 인코딩의 특정한 이유와 대응시킴.

- 5절 CERTIFICATE: 이유 1, 2, ~3
- 6절 X509 CRL: 이유 1
- 7절 CERTIFICATE REQUEST: 이유 1, ~3
- 8절 PKCS7: 이유 *
- 9절 CMS: 이유 *
- 10절 PRIVATE KEY: 이유 3
- 11절 ENCRYPTED PRIVATE KEY: 이유 3
- 12절 ATTRIBUTE CERTIFICATE: 이유 1, ~3
- 13절 PUBLIC KEY: 이유 2, 3

그림 20: Guide for DER Encoding

- * 표시: 암호화 메시지 구문(Cryptographic Message Syntax)은 임의 길이의 내용을 위해 설계됨 → 부정 길이(indefinite-length) 인코딩은 인코딩 생성 시 단일 패스 처리(원패스 처리, 스트리밍) 가능 → 특정 부분(서명된(signed) 속성·인증된(authenticated) 속성)만 DER로 인코딩 필수(MUST)
- ~ 표시: 항상 "작지는" 않으나 이 인코딩된 구조들은 특별히 "크지"(예: 16킬로바이트 이상) 않아야 함 → 파서는 큰 것들에 대해 미리 통지받아야 함 → 이는 애초에 이것들을 DER로 인코딩해야 하는 또 다른 이유

## 감사의 글

- Peter Gutmann: 속성 인증서(Attribute Certificate)·PKCS #7 메시지에 대한 레이블 문서화, 비표준 변형들에 대한 예시 추가 제안
- Dr. Stephen Henson: 언제 BER 대 DER이 적절하거나 필요한지를 구별할 것을 제안

## 저자 주소

```
Simon Josefsson
SJD AB
Johan Olof Wallins Vaeg 13
Solna  171 64
Sweden

Email: simon@josefsson.org
URI:   http://josefsson.org/


Sean Leonard
Penango, Inc.
5900 Wilshire Boulevard
21st Floor
Los Angeles, CA  90036
United States

Email: dev+ietf@seantek.com
URI:   http://www.penango.com/
```
