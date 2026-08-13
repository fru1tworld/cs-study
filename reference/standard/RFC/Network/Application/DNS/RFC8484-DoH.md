# RFC 8484 - HTTPS를 통한 DNS 쿼리 (DoH)

```
Internet Engineering Task Force (IETF)                        P. Hoffman
Request for Comments: 8484                                         ICANN
Category: Standards Track                                     P. McManus
ISSN: 2070-1721                                                  Mozilla
                                                            October 2018
```

### Abstract

- HTTPS를 통해 DNS 쿼리를 전송하고 DNS 응답을 수신하는 방법 수립
- 각 DNS 쿼리-응답 교환은 하나의 HTTP 트랜잭션에 매핑

## 이 메모의 상태

- Internet Standards Track 문서
- IETF 커뮤니티의 합의를 대표
- 공개 검토를 거쳐 IESG가 출판 승인
- Internet Standards에 대한 추가 정보 → RFC 7841 섹션 2 참고

이 문서의 현재 상태, 정오표, 피드백 제공 방법: https://www.rfc-editor.org/info/rfc8484

## 저작권 고지

- Copyright (c) 2018 IETF Trust 및 문서 저자로 식별된 사람들. 모든 권리 보유.
- 이 문서는 발행일 기준 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust의 법적 조항(https://trustee.ietf.org/license-info) 적용 대상 → 권리와 제한 사항 확인을 위해 검토 필요
- 이 문서에서 추출된 코드 구성요소는 Trust 법적 조항 섹션 4.e에 설명된 대로 Simplified BSD License 텍스트 포함 필요, Simplified BSD License에 설명된 대로 보증 없이 제공

## 목차

```
1.  서론
2.  용어
3.  DoH 서버 선택
4.  HTTP 교환
    4.1.  HTTP 요청
          4.1.1.  HTTP 요청 예시
    4.2.  HTTP 응답
          4.2.1.  DNS 및 HTTP 오류 처리
          4.2.2.  HTTP 응답 예시
5.  HTTP 통합
    5.1.  캐시 상호작용
    5.2.  HTTP/2
    5.3.  서버 푸시
    5.4.  콘텐츠 협상
6.  "application/dns-message" 미디어 유형 정의
7.  IANA 고려사항
    7.1.  "application/dns-message" 미디어 유형 등록
8.  프라이버시 고려사항
    8.1.  와이어상
    8.2.  서버 내
9.  보안 고려사항
10. 운영 고려사항
11. 참조
    11.1. 규범적 참조
    11.2. 정보적 참조
부록 A. 프로토콜 개발
부록 B. HTTP를 통한 DNS 또는 기타 형식에 관한 이전 작업
감사의 글
저자 주소
```

## 1. 서론

- HTTPS를 통한 DNS(DoH): https [RFC2818] URI(TLS [RFC8446] 기반 무결성·기밀성)를 사용해 HTTP [RFC7540]를 통해 DNS [RFC1035] 쿼리를 전송하고 응답을 수신하는 프로토콜
- 각 DNS 쿼리-응답 쌍은 하나의 HTTP 교환에 매핑
- DNS 와이어 형식 [RFC1035]을 HTTPS 위에 단순 터널링하는 방식과는 다름
  - 요청·응답의 기본 미디어 형식을 설정하면서도 대체 형식을 위한 HTTP 콘텐츠 협상 활용
  - → 캐싱·리디렉션·프록시·인증·압축을 포함한 HTTP 기능과의 통합 가능
- 이 통합의 활용 대상
  - 기존 DNS 클라이언트
  - 교차 출처 리소스 공유(CORS, [FETCH] 참조)로 제어되는 브라우저 API를 통해 DNS 접근이 필요한 웹 애플리케이션
- 이 프로토콜 설계의 주요 사용 사례 2가지(서로 활용 가능)
  - 경로상 장치에 의한 DNS 작업 간섭 방지
  - 웹 애플리케이션이 기존 브라우저 API를 통해 DNS 정보에 접근
- 이 사용 사례에서 DoH 프로토콜의 중점: DNS 클라이언트(운영 체제 스텁 리졸버 등)와 재귀 리졸버 간 통신

## 2. 용어

- "DoH 서버": 이 프로토콜을 구현하는 서버, 다른 표준화된 전송 프로토콜을 통해 DNS 서비스를 제공하는 "DNS 서버"와 구별
- "DoH 클라이언트": 이 프로토콜을 구현하는 클라이언트
- 핵심 단어 "MUST"·"MUST NOT"·"REQUIRED"·"SHALL"·"SHALL NOT"·"SHOULD"·"SHOULD NOT"·"RECOMMENDED"·"NOT RECOMMENDED"·"MAY"·"OPTIONAL"은 모두 대문자로 나타날 때 BCP 14 [RFC2119] [RFC8174]에 설명된 대로 해석

## 3. DoH 서버 선택

- DoH 클라이언트: 해석 URL 구성 방법을 설명하는 URI 템플릿 [RFC6570]으로 구성
  - 구성·탐색·URI 템플릿 업데이트는 모두 이 프로토콜 외부에서 발생
  - 구성 방식: 수동(예: 사용자 인터페이스에 URI 템플릿 입력)·자동(예: DHCP 또는 유사 프로토콜을 통한 발견) 모두 가능
  - DoH 서버는 하나 이상의 URI 템플릿 지원 가능 → 인증 요구 사항·서비스 수준 보장 등 다른 속성을 가진 엔드포인트 구현 가능
- DoH 클라이언트는 해석 처리할 DoH 서버 결정에 구성에서 부여된 URI 사용
  - [RFC2818]: HTTPS에서의 URI 기반 서버 식별 방법 기술
- DoH 클라이언트는 클라이언트 구성 외부에서 발견되었거나(예: HTTP/2 서버 푸시를 통해) 서버가 비요청 응답을 제공한다는 이유로 다른 URI를 사용해서는 안 됨(MUST NOT)
  - → 해석 권한이 구성에 없는 URI로 확장되는 운영·추적·보안 위험 방지
  - 이러한 시나리오를 다루는 미래 명세는 적절한 문맥·프레임워크 포함 기대

## 4. HTTP 교환

### 4.1. HTTP 요청

- DoH 클라이언트: 개별 DNS 쿼리를 이 섹션의 요구 사항에 따라 GET 또는 POST 메서드로 HTTP 요청 인코딩
- DoH 서버는 URI 템플릿으로 요청 URI 정의
- URI 템플릿 처리
  - POST 요청: 변수 없이 처리
  - GET 요청: 단일 변수 "dns"가 섹션 6에 설명된 DNS 요청 내용으로 정의, base64url [RFC4648]로 인코딩
- 미래의 DoH 관련 미디어 유형 명세는 이 프로토콜에서 사용할 URI 템플릿 변수 정의 가능
- DoH 서버는 POST·GET 메서드 모두 구현해야 함(MUST)
- POST 메서드 사용 시
  - DNS 쿼리는 HTTP 메시지 본문에 포함
  - Content-Type 요청 헤더 필드가 메시지의 미디어 유형 나타냄
  - 동일한 DNS 쿼리에 대한 GET 요청보다 일반적으로 작을 것으로 예상
- GET 메서드: 많은 수의 기존 HTTP 캐시 구현과 더 나은 호환성
- DoH 클라이언트는 응답에서 이해할 수 있는 콘텐츠 유형을 나타내기 위해 HTTP Accept 요청 헤더 필드를 포함해야 함(SHOULD)
  - 클라이언트는 섹션 6에 설명된 "application/dns-message" 응답을 처리할 수 있어야 함(MUST)
  - 다른 DNS 관련 미디어 유형도 처리 가능(MAY)
- HTTP 캐싱 최적화를 위해, DNS 메시지 ID 필드를 포함하는 미디어 형식("application/dns-message" 등)을 사용하는 DoH 클라이언트는 모든 DNS 요청에서 DNS ID를 0으로 사용해야 함(SHOULD)
  - HTTP 수준 요청-응답 상관관계가 "application/dns-message" 같은 형식에서 ID 필요성 제거
  - DNS ID를 다양하게 변경 → 의미적으로 동일한 쿼리를 가진 DNS 요청이 다른 항목으로 캐시될 수 있음
- DoH 클라이언트는 [RFC7540]에 따라 HTTP/2 패딩과 압축 사용 가능(MAY)

### 4.1.1. HTTP 요청 예시

- 이 예시들: [RFC7540]의 HTTP/2 스타일 형식 사용
- URI 템플릿 "https://dnsserver.example.net/dns-query{?dns}"를 가진 DoH 서비스를 사용하여 "www.example.com"에 대한 IN A 레코드 조회, 요청은 "application/dns-message" 미디어 유형 사용

첫 번째 예시 - "www.example.com"에 대한 GET 요청:

```
:method = GET
:scheme = https
:authority = dnsserver.example.net
:path = /dns-query?dns=AAABAAABAAAAAAAAA3d3dwdleGFtcGxlA2NvbQAAAQAB
accept = application/dns-message
```

두 번째 예시 - "www.example.com"에 대한 POST 요청:

```
:method = POST
:scheme = https
:authority = dnsserver.example.net
:path = /dns-query
accept = application/dns-message
content-type = application/dns-message
content-length = 33

<33 bytes represented as hex>:
00 00 01 00 00 01 00 00  00 00 00 00 03 77 77 77
07 65 78 61 6d 70 6c 65  03 63 6f 6d 00 00 01 00
01
```

- 이 33바이트: [RFC1035] 섹션 4.1에 정의된 DNS 와이어 형식의 DNS 메시지, DNS 헤더에서 시작

세 번째 예시 - 확장 도메인을 사용하는 GET 요청:

- "a.62characterlabel-makes-base64url-distinct-from-standard-base64.example.com"에 대한 GET 쿼리 → base64url 인코딩 알파벳이 표준 base64와 다르다는 점, 패딩이 생략된다는 점을 보여줌

```
DNS 쿼리 (16진수 94바이트):
00 00 01 00 00 01 00 00  00 00 00 00 01 61 3e 36
32 63 68 61 72 61 63 74  65 72 6c 61 62 65 6c 2d
6d 61 6b 65 73 2d 62 61  73 65 36 34 75 72 6c 2d
64 69 73 74 69 6e 63 74  2d 66 72 6f 6d 2d 73 74
61 6e 64 61 72 64 2d 62  61 73 65 36 34 07 65 78
61 6d 70 6c 65 03 63 6f  6d 00 00 01 00 01

:method = GET
:scheme = https
:authority = dnsserver.example.net
:path = /dns-query?dns=AAABAAABAAAAAAAAAWE-NjJjaGFyYWN0ZXJsYWJl
           bC1tYWtlcy1iYXNlNjR1cmwtZGlzdGluY3QtZnJvbS1z
           dGFuZGFyZC1iYXNlNjQHZXhhbXBsZQNjb20AAAEAAQ
accept = application/dns-message
```

### 4.2. HTTP 응답

- 여기서 정의하는 단일 응답 형식: "application/dns-message"(다른 형식은 차후 정의 가능)
- DoH 서버는 "application/dns-message" 요청 메시지를 처리할 수 있어야 함(MUST)
- 다른 응답 미디어 유형은 다양한 양의 DNS 응답 정보 제공
  - 한 형식은 DNS 헤더 바이트를 포함, 다른 형식은 생략 가능
  - 정보 범위는 미디어 유형 명세가 결정
- 각 DNS 요청-응답 쌍은 하나의 HTTP 교환에 매핑
  - HTTP의 멀티스트리밍 기능([RFC7540] 섹션 5 참조)으로 여러 DNS 요청-응답을 순서 무관하게 처리·전송 가능
- DNS 및 HTTP 응답 캐싱의 관계 → 섹션 5.1 참고

### 4.2.1. DNS 및 HTTP 오류 처리

- DNS 응답 코드: DNS 쿼리의 성공·실패 나타냄
- 성공적인 2xx HTTP 상태 코드([RFC7231] 섹션 6.3): DNS 응답 코드와 관계없이 모든 유효한 DNS 응답에 적용
  - 예: SERVFAIL·NXDOMAIN 같은 실패 DNS 응답 코드를 가진 DNS 메시지에도 성공적인 2xx HTTP 상태 코드 수반
- 비성공 HTTP 상태 코드는 원래 DNS 질문에 대한 응답을 생성하지 않음
  - DoH 클라이언트는 다른 HTTP 클라이언트와 동일한 비성공 HTTP 상태 코드의 의미론적 처리 사용 필요
  - → 동일한 DoH 서버에서 쿼리 재시도(예: HTTP 401 권한 부여 실패 [RFC7235] 섹션 3.1) 또는 다른 서버에서 시도(예: HTTP 415 미지원 미디어 유형 [RFC7231] 섹션 6.5.13, HTTP 406 부적합한 표현 [RFC7231] 섹션 6.5.6) 포함 가능

### 4.2.2. HTTP 응답 예시

- 이 예시: "www.example.com"에 대한 IN AAAA 레코드 쿼리 응답, 재귀 활성화 상태
- 응답에는 주소 2001:db8:abcd:12:1:2:3:4·TTL 3709초를 가진 응답 레코드 1개 포함

```
:status = 200
content-type = application/dns-message
content-length = 61
cache-control = max-age=3709

<61 bytes represented as hex>:
00 00 81 80 00 01 00 01  00 00 00 00 03 77 77 77
07 65 78 61 6d 70 6c 65  03 63 6f 6d 00 00 1c 00
01 c0 0c 00 1c 00 01 00  00 0e 7d 00 10 20 01 0d
b8 ab cd 00 12 00 01 00  02 00 03 00 04
```

## 5. HTTP 통합

- 이 프로토콜은 [RFC7230]의 https URI 스킴과 함께 사용되어야 함(MUST)
- HTTP 통합에 대한 추가 고려사항 → 섹션 8·9 참고

### 5.1. 캐시 상호작용

- DoH 교환은 클라이언트-서버 사이 또는 클라이언트 자체에서 HTTP 캐시와 DNS 전용 캐시를 모두 포함하는 계층 통과 가능
  - HTTP 캐시는 범용적으로 설계 → DoH 프로토콜에 대한 이해 없음
  - DoH 인식하도록 수정된 클라이언트 구현이라도 업스트림 캐시(프록시·게이트웨이·CDN)가 DoH를 인식한다고 보장 불가
- → DoH 서버는 GET 응답의 HTTP 캐싱 메타데이터를 신중하게 선택해야 함
  - POST 응답 캐싱은 특정 헤더가 필요하며 널리 구현되지 않아 DoH에 부적합
- DoH 서버는 명시적인 HTTP 신선도 수명([RFC7234] 섹션 4.2 참조)을 할당해 DoH 클라이언트가 최신 DNS 데이터를 사용할 가능성을 높여야 함(SHOULD)
  - → HTTP 캐시가 휴리스틱 신선도([RFC7234] 섹션 4.2.2)를 적용해 DoH 서버의 캐시 제어를 제거하는 것을 방지
- DoH HTTP 응답에 할당된 신선도 수명은 DNS 응답의 Answer 섹션에 있는 가장 작은 TTL 이하여야 함(MUST)
  - Answer 섹션의 가장 작은 TTL과 동일한 신선도 수명 권장(RECOMMENDED)
  - 예: TTL이 30초·600초·300초인 RRset이 있는 경우 → HTTP 신선도 수명은 30초("Cache-Control: max-age=30")여야 함
  - → 만료된 RRset이 의도치 않게 HTTP 캐시에서 제공되는 것 방지
- Answer 섹션에 레코드가 없지만 Authority 섹션에 SOA 레코드를 포함하는 DNS 응답의 경우, 응답의 신선도 수명은 해당 SOA 레코드의 MINIMUM 필드보다 크면 안 됨(MUST NOT) [RFC2308]
- stale-while-revalidate·stale-if-error Cache-Control 지시문([RFC5861]): 서버 정책이 허용하는 경우 DoH 구현에 적합
  - → 클라이언트는 서버의 재량에 따라 더 이상 신선하지 않은 캐시 항목(완전한 캐시 항목 또는 캐시 항목 없음)을 재사용 가능
- DoH 서버는 전역적으로 유효하지 않은 응답에 대한 HTTP 캐싱을 처리해야 함
  - 클라이언트 ID에 맞춤화된 응답의 경우, 서버는 Cache-Control max-age=0이나 Vary 응답 헤더([RFC7231] 섹션 7.1.4)를 통해 보조 캐시 키([RFC7234] 섹션 4.1)를 설정해 전역 재사용을 방지해야 함
- DoH 클라이언트는 응답의 DNS TTL을 계산할 때 Age 응답 헤더 필드 값 [RFC7234]을 고려해야 함(MUST)
  - 예: DNS TTL 600초의 RRset을 받았지만 Age 헤더가 250초의 캐시 기간을 나타내는 경우 → 남은 수명은 350초
  - DoH 클라이언트의 HTTP 캐시·DNS 캐시 모두에 적용
- DoH 클라이언트는 "no-cache" 요청 Cache-Control 지시문([RFC7234] 섹션 5.2.1.4)과 유사한 제어로 캐시되지 않은 HTTP 응답 사본을 요청 가능(MAY)
  - 일부 캐시는 구성·전통적 DNS 캐시 상호작용에 이러한 메커니즘이 없어 해당 지시문을 준수하지 않을 수 있음
- HTTP 조건부 요청([RFC7232])은 DoH에서 제한된 가치 제공 → DNS 트랜잭션이 여전히 지연시간에 제한되는 동안 대역폭 이점만 제공
  - HTTP 재검증 헤더(Last-Modified·Etag)는 종종 전체 DNS 응답 크기를 초과, 가변적 특성으로 HTTP/2 압축 사전([RFC7541])에 부담
  - 영역 전송과 같은 대규모 DNS 데이터는 재검증에서 더 많은 이점 획득 가능

### 5.2. HTTP/2

- HTTP/2 [RFC7540]: DoH와 함께 사용하기 위한 최소 권장 HTTP 버전(RECOMMENDED)
- 기존 UDP 기반 DNS 메시지는 본질적으로 비순서적·낮은 오버헤드
  - 경쟁력 있는 HTTP 전송은 유사한 성능을 위해 재정렬·병렬 처리·우선순위·헤더 압축 지원 필요
  - HTTP/2는 이러한 기능 도입
  - 이전 버전의 HTTP는 DoH의 의미론적 요구 사항을 지원하지만 성능 저하

### 5.3. 서버 푸시

- DoH 응답 데이터를 DNS 해석에 적용하기 전, 클라이언트는 HTTP 요청 URI가 DoH 쿼리에 적합한지 확인해야 함
  - DoH 클라이언트가 시작한 요청의 경우: URI 선택으로 인해 암묵적으로 확인됨
  - HTTP 서버 푸시([RFC7540] 섹션 8.2)의 경우: 표준 서버 푸시 보안 검사와 함께, 푸시된 URI가 클라이언트의 의도된 쿼리 대상과 일치하는지 추가 주의 필요

### 5.4. 콘텐츠 협상

- 상호운용성 극대화를 위해, DoH 클라이언트와 DoH 서버는 "application/dns-message" 미디어 유형을 지원해야 함(MUST)
- HTTP 콘텐츠 협상([RFC7231] 섹션 3.4 참조)으로 정의된 다른 미디어 유형도 사용 가능(MAY)
- 대체 미디어 유형은 모든 표준 UDP DNS 쿼리(확장 포함, 다중 응답 쿼리는 제외)를 표현할 수 있어야 함

## 6. "application/dns-message" 미디어 유형 정의

- "application/dns-message" 미디어 유형의 페이로드: [RFC1035] 섹션 4.2.1에 정의된 단일 DNS 온더와이어 메시지, [RFC1035] 섹션 4.1의 전체 형식 참조
- [RFC1035]는 UDP로 전송되는 DNS 메시지를 512바이트로 제한했으나 [RFC6891]이 이를 업데이트
  - 이 미디어 유형은 최대 DNS 메시지 크기를 65535바이트로 제한
- 이 와이어 형식은 [RFC7858]의 형식(2바이트 길이를 포함하는 [RFC1035] 섹션 4.2.2 사용)과 다름
- 이 미디어 유형을 사용하는 DoH 클라이언트는 하나 이상의 EDNS(DNS용 확장 메커니즘, [RFC6891]) 옵션을 포함 가능(MAY)
- 이 미디어 유형을 사용하는 DoH 서버는 DNS 요청에서 EDNS UDP 페이로드 크기에 지정된 값을 무시해야 함(MUST)
- GET 요청의 경우, 이 미디어 유형의 데이터 페이로드는 base64url [RFC4648]로 인코딩된 다음 "dns"라는 변수로 URI 템플릿 확장에 제공되어야 함(MUST)
  - base64url의 패딩 문자는 포함되어서는 안 됨(MUST NOT)
- POST 요청의 경우, 이 미디어 유형의 데이터 페이로드는 인코딩되어서는 안 되며(MUST NOT) HTTP 메시지 본문으로 직접 사용

## 7. IANA 고려사항

### 7.1. "application/dns-message" 미디어 유형 등록

- Type name: application
- Subtype name: dns-message
- Required parameters: N/A
- Optional parameters: N/A
- Encoding considerations: 바이너리 형식
  - 내용은 [RFC1035]에 정의된 DNS 메시지
  - 여기서 사용되는 형식은 DNS over UDP에 사용되는 형식, [RFC1035]의 다이어그램에 해당
- Security considerations: RFC 8484 참조
  - 내용은 DNS 메시지이며 실행 가능한 코드가 아님
- Interoperability considerations: 없음
- Published specification: RFC 8484
- Applications that use this media type: 전체 DNS 메시지를 교환하는 시스템
- Additional information
  - Deprecated alias names for this type: N/A
  - Magic number(s): N/A
  - File extension(s): N/A
  - Macintosh file type code(s): N/A
- Person & email address to contact for further information: Paul Hoffman, paul.hoffman@icann.org
- Intended usage: COMMON
- Restrictions on usage: N/A
- Author: Paul Hoffman, paul.hoffman@icann.org
- Change controller: IESG

## 8. 프라이버시 고려사항

- [RFC7626]: "와이어상"(섹션 2.4)·"서버 내"(섹션 2.5) 맥락에서 DNS 프라이버시 고려사항 검토
  - 이 프레이밍은 DoH 프라이버시 고려사항에도 유용하게 적용

### 8.1. 와이어상

- DoH: DNS 트래픽을 암호화하고 서버 인증을 요구 → 수동 감시([RFC7258])와 DNS 트래픽을 악성 서버로 전환하는 능동 공격([RFC7626] 섹션 2.5.1) 완화
  - DNS over TLS([RFC7858])도 유사한 보호 제공, 직접적인 UDP·TCP 전송은 여전히 취약
  - 실험적 패딩 길이 지침은 [RFC8467]에 나타남
- HTTPS의 기본 포트 443 사용과 동일한 연결에서 DoH 트래픽이 다른 HTTPS 트래픽과 혼합 → 권한 없는 경로상 장치의 DNS 작업 간섭 억제, DNS 트래픽 분석을 어렵게 만듦

### 8.2. 서버 내

- DNS 와이어 형식에는 클라이언트 식별자 미포함, 전송 메커니즘은 상관관계 데이터 제공
  - HTTPS는 명시적 HTTP 쿠키·HTTP 요청 헤더 필드 세트 및 순서에서 오는 암묵적 핑거프린팅을 포함해 새로운 상관관계 고려사항 도입
- DoH 구현은 IP·TCP·TLS·HTTP 계층 위에 위치
  - 각 계층은 상관관계 기능 포함
  - DNS 전송은 일반적으로 구현 계층의 프라이버시 속성 상속 → 예: DNS over TLS 전송은 IP·TCP·TLS의 속성을 가짐
- HTTPS 계층과 관련된 DoH의 프라이버시 고려사항은 DNS over TLS에 비해 점진적
  - DoH는 HTTPS와 관련된 것 이상의 알려진 우려를 도입하지 않음
- IP 수준의 클라이언트 주소는 명백한 상관관계 정보 제공
  - NAT·프록시·VPN·주소 순환이 이를 완화 가능
  - 동일한 엔티티가 DNS 서버와 DHCP 서버를 동시에 운영하면 악화 가능
- 여러 요청에 단일 TCP 연결을 사용하는 DNS 구현은 해당 요청들을 직접 그룹화
  - 장기 연결은 성능이 우수하지만 더 많은 요청을 그룹화 → 잠재적으로 상관관계·통합 정보 노출
  - TCP 기반 솔루션은 TCP Fast Open([RFC7413]) 사용 가능 → TCP Fast Open 쿠키는 서버 측에서 TCP 세션 상관관계를 가능하게 함
- TLS 기반 구현은 종종 [RFC8446] 섹션 2.2와 같은 세션 재개 메커니즘으로 핸드셰이크 성능 향상
  - 세션 재개는 TLS 세션을 연결하기 위한 사소한 상관관계 메커니즘을 만듦
- HTTP의 기능 세트는 여러 메커니즘으로 식별·추적을 가능하게 함
  - 인증 헤더는 프로필을 명시적으로 식별
  - HTTP 쿠키는 명시적 상태 추적·인증 메커니즘으로 작용
- User-Agent·Accept-Language 헤더는 종종 클라이언트 버전·로캘 정보를 전달 → 콘텐츠 협상·운영 버그 해결 용이
  - 캐시 제어 헤더는 클라이언트 히스토리의 부분 집합 정보를 노출 가능
  - 동일한 연결에서 DoH 요청이 다른 HTTP 요청과 혼합되면 더 풍부한 데이터 상관관계 지원
- DoH 프로토콜 설계는 애플리케이션이 여기에 열거되지 않은 기능을 포함해 HTTP 생태계를 완전히 활용할 수 있도록 함
  - HTTP 기능의 전체 세트를 활용하면 DoH가 HTTP 터널 이상이 될 수 있지만, 구현을 HTTP의 프라이버시 고려사항 전체에 노출시키는 대가가 있음
- DoH 클라이언트·서버의 구현은 이러한 기능의 이점·프라이버시 영향·배포 맥락을 고려해 활성화 여부를 결정해야 함
  - 구현은 원하는 기능 세트를 달성하는 데 필요한 최소한의 데이터 세트를 노출하도록 권고
- DoH 구현에 HTTP 쿠키 [RFC6265] 지원이 필요한지 결정하는 것이 특히 중요
  - HTTP 쿠키가 HTTP의 주요 상태 추적 메커니즘이기 때문
  - HTTP 쿠키는 사용 사례에서 명시적으로 요구되지 않는 한 DoH 클라이언트에 의해 수락되어서는 안 됨(SHOULD NOT)

## 9. 보안 고려사항

- HTTPS를 통한 DNS는 기본 HTTP 전송 보안에 의존 → 기존 UDP 기반 DNS 증폭 공격 완화
  - HTTP/2 구현은 [RFC7540] 섹션 9.2에 정의된 TLS 프로필에서 이점을 얻음
- 세션 수준 암호화는 인정된 트래픽 분석 취약점 존재 → DNS 쿼리에서 특히 심각할 수 있음
  - HTTP/2는 압축([RFC7540] 섹션 10.6)·패딩([RFC7540] 섹션 10.7) 지침 제공
  - DoH 서버는 DoH 클라이언트가 요청하면 DNS 패딩([RFC7830]) 적용 가능
  - 실험적 패딩 길이 지침은 [RFC8467]에 나타남
- HTTPS 연결은 DoH 서버-클라이언트 간 전송 보안을 제공하지만, DNSSEC이 제공하는 DNS 데이터 응답 무결성은 제공하지 않음
  - DNSSEC과 DoH는 독립적이고 완전히 호환되는 프로토콜로서 서로 다른 문제를 해결 → 하나의 사용이 다른 하나의 필요성·유용성을 감소시키지 않음
  - 클라이언트는 전체 DNSSEC 검증을 수행하거나 DoH 서버의 검증을 신뢰해 AD(Authentic Data) 비트를 검사함으로써 진본 여부 판단 가능
  - 다른 응답 미디어 유형은 다양한 양의 DNS 응답 정보를 제공해 이 선택에 잠재적으로 영향 가능
- 섹션 5.1에서 이 프로토콜의 HTTP 캐싱 상호작용을 설명
  - 클라이언트가 사용하는 캐시를 제어하는 공격자는 클라이언트의 DNS 관점에 영향 가능
  - 다른 프로토콜에 대한 HTTP 캐싱의 보안 영향과 일치
- DNSSEC 정보가 없는 경우, DoH 서버는 잘못된 응답 데이터를 제공할 수 있음
  - 섹션 3에서는 DoH DNS 응답 사용을 구성된 서버로 제한
  - 잘못된 데이터 보호를 보장하지는 않지만 위험을 줄임

## 10. 운영 고려사항

- 로컬 정책·유사 요인으로 인해 다른 DNS 서버가 동일한 쿼리에 대해 다른 결과를 반환할 수 있음(분할 DNS 구성 [RFC6950]에서와 같음)
  - → 쿼리되는 서버의 선택이 최종 결과에 영향
  - DNS64([RFC6147])의 경우, 선택에 따라 IPv6/IPv4 변환 기능 결정 가능
- 이 명세의 HTTPS 채널은 DoH 클라이언트-서버 간 안전한 양자간 통신 수립
  - 보안되지 않은 DNS 전송에 의존하는 필터링·검사 시스템은 TLS가 제공하는 기밀성·무결성 보호로 인해 DNS over HTTPS 환경에서 작동 중단
- 일부 HTTPS 클라이언트 구현은 실시간 제3자 인증서 해지 상태 확인 수행
  - 이것이 DoH 서버 연결 중 실행되고, 해당 확인이 제3자 연결을 위해 DNS 해석을 필요로 하면 교착 상태 발생
  - Online Certificate Status Protocol(OCSP) 서버([RFC6960]) 또는 인증서 해지 목록(CRL) 가져오기를 위한 Authority Information Access(AIA)([RFC5280] 섹션 4.2.2.1)가 교착 상태 메커니즘의 예
  - 완화 방법: DoH 서버의 인증은 TLS 핸드셰이크에서 외부 리소스에 대한 DNS 기반 참조에 의존해서는 안 됨(SHOULD NOT)
  - OCSP는 TLS 1.3용 [RFC8446] 섹션 4.4.2.1과 같은 메커니즘으로 인증서 상태 번들링 허용
  - AIA 교착 상태는 중간 인증서 제공으로 방지
  - 이러한 교착 상태는 리디렉트 대상 서버에 대해서도 고려 필요
- DoH 클라이언트는 HTTP 요청이 DoH 서버 호스트명 해석을 필요로 할 때 유사한 부트스트래핑 과제에 직면
  - 전통적인 DNS 네임서버 주소는 동일한 서버에서 초기 결정 불가능, 마찬가지로 DoH 클라이언트는 초기 호스트명-주소 해석에 자체 DoH 서버를 사용 불가
  - 대안적 클라이언트 전략
    - 초기 해석이 구성에 통합
    - IP 기반 URI와 해당하는 IP 기반 HTTPS 인증서 사용
    - HTTPS 연결 인증을 유지하면서 전통적 DNS 또는 다른 DoH 서버를 통한 호스트명 해석
- HTTP는 무상태 애플리케이션 수준 프로토콜 → DoH 구현은 상태 유지 요청 순서 보장 없음
  - DoH는 엄격한 순서를 요구하는 프로토콜을 전송할 수 없음
- DoH 서버는 유효한 모든 DNS 응답으로 응답
  - 예: 유효한 응답에는 서버가 검색한 응답이 불완전함을 나타내는 TC(truncation) 비트가 포함되어 최선 노력의 응답 제공 가능
  - DoH 서버는 이행 불가능한 쿼리에 대해 HTTP 오류 사용 가능
  - 동일한 예시에서 DoH 서버는 비오류 TC 비트 응답 대신 HTTP 오류를 사용할 수도 있음
- [RFC6891]을 통한 DNS 확장이 확산됨
  - [RFC7828]을 포함한 전송 특정 확장은 DoH에 적용되지 않음

## 11. 참조

### 11.1. 규범적 참조

```
[RFC1035]  Mockapetris, P., "Domain names - implementation and
           specification", STD 13, RFC 1035,
           DOI 10.17487/RFC1035, November 1987,
           <https://www.rfc-editor.org/info/rfc1035>.

[RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
           Requirement Levels", BCP 14, RFC 2119,
           DOI 10.17487/RFC2119, March 1997,
           <https://www.rfc-editor.org/info/rfc2119>.

[RFC2308]  Andrews, M., "Negative Caching of DNS Queries (DNS
           NCACHE)", RFC 2308, DOI 10.17487/RFC2308, March 1998,
           <https://www.rfc-editor.org/info/rfc2308>.

[RFC4648]  Josefsson, S., "The Base16, Base32, and Base64 Data
           Encodings", RFC 4648, DOI 10.17487/RFC4648, October 2006,
           <https://www.rfc-editor.org/info/rfc4648>.

[RFC6265]  Barth, A., "HTTP State Management Mechanism", RFC 6265,
           DOI 10.17487/RFC6265, April 2011,
           <https://www.rfc-editor.org/info/rfc6265>.

[RFC6570]  Gregorio, J., Fielding, R., Hadley, M., Nottingham, M.,
           and D. Orchard, "URI Template", RFC 6570,
           DOI 10.17487/RFC6570, March 2012,
           <https://www.rfc-editor.org/info/rfc6570>.

[RFC7230]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext
           Transfer Protocol (HTTP/1.1): Message Syntax and Routing",
           RFC 7230, DOI 10.17487/RFC7230, June 2014,
           <https://www.rfc-editor.org/info/rfc7230>.

[RFC7231]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext
           Transfer Protocol (HTTP/1.1): Semantics and Content",
           RFC 7231, DOI 10.17487/RFC7231, June 2014,
           <https://www.rfc-editor.org/info/rfc7231>.

[RFC7232]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext
           Transfer Protocol (HTTP/1.1): Conditional Requests",
           RFC 7232, DOI 10.17487/RFC7232, June 2014,
           <https://www.rfc-editor.org/info/rfc7232>.

[RFC7234]  Fielding, R., Ed., Nottingham, M., Ed., and J. Reschke,
           Ed., "Hypertext Transfer Protocol (HTTP/1.1): Caching",
           RFC 7234, DOI 10.17487/RFC7234, June 2014,
           <https://www.rfc-editor.org/info/rfc7234>.

[RFC7235]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext
           Transfer Protocol (HTTP/1.1): Authentication", RFC 7235,
           DOI 10.17487/RFC7235, June 2014,
           <https://www.rfc-editor.org/info/rfc7235>.

[RFC7540]  Belshe, M., Peon, R., and M. Thomson, Ed., "Hypertext
           Transfer Protocol Version 2 (HTTP/2)", RFC 7540,
           DOI 10.17487/RFC7540, May 2015,
           <https://www.rfc-editor.org/info/rfc7540>.

[RFC7541]  Peon, R. and H. Ruellan, "HPACK: Header Compression for
           HTTP/2", RFC 7541, DOI 10.17487/RFC7541, May 2015,
           <https://www.rfc-editor.org/info/rfc7541>.

[RFC7626]  Bortzmeyer, S., "DNS Privacy Considerations", RFC 7626,
           DOI 10.17487/RFC7626, August 2015,
           <https://www.rfc-editor.org/info/rfc7626>.

[RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
           2119 Key Words", BCP 14, RFC 8174,
           DOI 10.17487/RFC8174, May 2017,
           <https://www.rfc-editor.org/info/rfc8174>.

[RFC8446]  Rescorla, E., "The Transport Layer Security (TLS)
           Protocol Version 1.3", RFC 8446,
           DOI 10.17487/RFC8446, August 2018,
           <https://www.rfc-editor.org/info/rfc8446>.
```

### 11.2. 정보적 참조

```
[FETCH]    "Fetch Living Standard", August 2018,
           <https://fetch.spec.whatwg.org/>.

[RFC2818]  Rescorla, E., "HTTP Over TLS", RFC 2818,
           DOI 10.17487/RFC2818, May 2000,
           <https://www.rfc-editor.org/info/rfc2818>.

[RFC5280]  Cooper, D., Santesson, S., Farrell, S., Boeyen, S.,
           Housley, R., and W. Polk, "Internet X.509 Public Key
           Infrastructure Certificate and Certificate Revocation List
           (CRL) Profile", RFC 5280, DOI 10.17487/RFC5280, May 2008,
           <https://www.rfc-editor.org/info/rfc5280>.

[RFC5861]  Nottingham, M., "HTTP Cache-Control Extensions for Stale
           Content", RFC 5861, DOI 10.17487/RFC5861, May 2010,
           <https://www.rfc-editor.org/info/rfc5861>.

[RFC6147]  Bagnulo, M., Sullivan, A., Matthews, P., and I. van
           Beijnum, "DNS64: DNS Extensions for Network Address
           Translation from IPv6 Clients to IPv4 Servers", RFC 6147,
           DOI 10.17487/RFC6147, April 2011,
           <https://www.rfc-editor.org/info/rfc6147>.

[RFC6891]  Damas, J., Graff, M., and P. Vixie, "Extension Mechanisms
           for DNS (EDNS(0))", STD 75, RFC 6891,
           DOI 10.17487/RFC6891, April 2013,
           <https://www.rfc-editor.org/info/rfc6891>.

[RFC6950]  Peterson, J., Kolkman, O., Tschofenig, H., and B. Aboba,
           "Architectural Considerations on Application Features in
           the DNS", RFC 6950, DOI 10.17487/RFC6950, October 2013,
           <https://www.rfc-editor.org/info/rfc6950>.

[RFC6960]  Santesson, S., Myers, M., Ankney, R., Malpani, A.,
           Galperin, S., and C. Adams, "X.509 Internet Public Key
           Infrastructure Online Certificate Status Protocol - OCSP",
           RFC 6960, DOI 10.17487/RFC6960, June 2013,
           <https://www.rfc-editor.org/info/rfc6960>.

[RFC7258]  Farrell, S. and H. Tschofenig, "Pervasive Monitoring Is
           an Attack", BCP 188, RFC 7258,
           DOI 10.17487/RFC7258, May 2014,
           <https://www.rfc-editor.org/info/rfc7258>.

[RFC7413]  Cheng, Y., Chu, J., Radhakrishnan, S., and A. Jain, "TCP
           Fast Open", RFC 7413, DOI 10.17487/RFC7413, December 2014,
           <https://www.rfc-editor.org/info/rfc7413>.

[RFC7828]  Wouters, P., Abley, J., Dickinson, S., and R. Bellis,
           "The edns-tcp-keepalive EDNS0 Option", RFC 7828,
           DOI 10.17487/RFC7828, April 2016,
           <https://www.rfc-editor.org/info/rfc7828>.

[RFC7830]  Mayrhofer, A., "The EDNS(0) Padding Option", RFC 7830,
           DOI 10.17487/RFC7830, May 2016,
           <https://www.rfc-editor.org/info/rfc7830>.

[RFC7858]  Hu, Z., Zhu, L., Heidemann, J., Mankin, A., Wessels, D.,
           and P. Hoffman, "Specification for DNS over Transport
           Layer Security (TLS)", RFC 7858, DOI 10.17487/RFC7858,
           May 2016, <https://www.rfc-editor.org/info/rfc7858>.

[RFC8467]  Mayrhofer, A., "Padding Policies for Extension Mechanisms
           for DNS (EDNS(0))", RFC 8467, DOI 10.17487/RFC8467,
           October 2018, <https://www.rfc-editor.org/info/rfc8467>.
```

## 부록 A. 프로토콜 개발

- 이 부록은 DoH의 설계 요구 사항을 설명
  - 독자가 현재 프로토콜을 이해하는 데 도움을 주기 위해 나열, 미래 개발을 제한하기 위한 것이 아님
  - 이 부록은 비규범적

설명된 프로토콜 설계가 기반한 프로토콜 요구 사항:

- 프로토콜은 일반적인 HTTP 의미론을 사용해야 함
- 쿼리와 응답은 확장을 포함하되 다중 응답을 제외한 모든 표준 UDP 기반 DNS 쿼리를 유연하게 표현해야 함
- 프로토콜은 새로운 DNS 쿼리 및 응답 형식을 허용해야 함
- 프로토콜은 필수적인 단일 형식 명세를 통해 상호운용성을 보장해야 함
  - 해당 형식은 하나 이상의 EDNS 옵션(정의되지 않은 것 포함)을 포함해 미래의 DNS 프로토콜 수정을 지원해야 함
- 프로토콜은 HTTPS를 준수하는 보안 전송을 사용해야 함

비요구 사항으로 간주된 항목:

- 네트워크 특정 DNS64([RFC6147]) 지원
- 다른 네트워크 특정 평문 DNS 쿼리 추론 지원
- 비보안 HTTP 지원

## 부록 B. HTTP를 통한 DNS 또는 기타 형식에 관한 이전 작업

- 다음은 HTTP/1을 통한 DNS 또는 다른 형식으로의 DNS 데이터 표현에 관한 이전 작업의 불완전한 목록
  - tools.ietf.org 링크(문서는 모두 만료됨)와 소프트웨어 웹사이트 포함

- <https://tools.ietf.org/html/draft-mohan-dns-query-xml>
- <https://tools.ietf.org/html/draft-daley-dnsxml>
- <https://tools.ietf.org/html/draft-dulaunoy-dnsop-passive-dns-cof>
- <https://tools.ietf.org/html/draft-bortzmeyer-dns-json>
- <https://www.nlnetlabs.nl/projects/dnssec-trigger/>

## 감사의 글

- 이 작업은 다양한 기술 전문가들 간의 높은 수준의 협력이 필요
- 감사 대상: Ray Bellis, Stephane Bortzmeyer, Manu Bretelle, Sara Dickinson, Massimiliano Fantuzzi, Tony Finch, Daniel Kahn Gilmor, Olafur Gudmundsson, Wes Hardaker, Rory Hewitt, Joe Hildebrand, David Lawrence, Eliot Lear, John Mattsson, Alex Mayrhofer, Mark Nottingham, Jim Reid, Adam Roach, Ben Schwartz, Davey Song, Daniel Stenberg, Andrew Sullivan, Martin Thomson, Sam Weiler

## 저자 주소

```
Paul Hoffman
ICANN

Email: paul.hoffman@icann.org


Patrick McManus
Mozilla

Email: mcmanus@ducksong.com
```
