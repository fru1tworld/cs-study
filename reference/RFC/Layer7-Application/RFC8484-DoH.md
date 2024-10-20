Internet Engineering Task Force (IETF)                        P. Hoffman
Request for Comments: 8484                                         ICANN
Category: Standards Track                                     P. McManus
ISSN: 2070-1721                                                  Mozilla
                                                            October 2018


                      HTTPS를 통한 DNS 쿼리 (DoH)

초록

   이 명세는 HTTPS를 통해 DNS 쿼리를 전송하고 DNS 응답을 수신하는 방법을
   수립한다. 각 DNS 쿼리-응답 교환은 하나의 HTTP 트랜잭션에 매핑된다.

이 메모의 상태

   이 문서는 인터넷 표준 추적(Internet Standards Track) 문서이다. 이 문서는
   IETF 커뮤니티의 합의를 반영한다. 이 문서는 공개 검토를 거쳤으며 인터넷
   엔지니어링 운영 그룹(IESG)에 의해 발행이 승인되었다. 인터넷 표준에 대한
   추가 정보는 RFC 7841의 섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 피드백 제공 방법에 대한 정보는
   https://www.rfc-editor.org/info/rfc8484 에서 확인할 수 있다.

저작권 고지

   Copyright (c) 2018 IETF Trust와 문서 저자로 식별된 사람들. 모든 권리
   보유.

   이 문서는 BCP 78과 이 문서의 발행일에 유효한 IETF 문서에 관한 IETF
   Trust의 법적 조항(https://trustee.ietf.org/license-info)의 적용을 받는다.
   이 문서에 대한 귀하의 권리와 제한 사항이 설명되어 있으므로 이 문서들을
   주의 깊게 검토하기 바란다. 이 문서에서 추출된 코드 구성 요소는 Trust 법적
   조항의 섹션 4.e에 설명된 대로 단순화된 BSD 라이선스 텍스트를 포함해야
   하며, 단순화된 BSD 라이선스에 설명된 대로 보증 없이 제공된다.

목차

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

1. 서론

   이 문서는 HTTPS를 통한 DNS(DoH)라는 특정 프로토콜을 정의하며, https
   [RFC2818] URI(따라서 무결성과 기밀성을 위한 TLS [RFC8446] 보안)를
   사용하여 HTTP [RFC7540]를 통해 DNS [RFC1035] 쿼리를 전송하고 DNS 응답을
   수신하는 프로토콜이다. 각 DNS 쿼리-응답 쌍은 하나의 HTTP 교환에
   매핑된다.

   여기서 설명하는 접근 방식은 DNS 와이어 형식 [RFC1035]을 HTTPS 위에
   단순히 터널링하는 것과는 다르다. 요청과 응답에 대한 기본 미디어 형식을
   설정하면서도 대체 형식을 위한 HTTP 콘텐츠 협상의 이점을 활용한다. 이를
   통해 이 프로토콜은 캐싱, 리디렉션, 프록시, 인증, 압축을 포함한 HTTP
   기능과의 통합이 가능하다.

   이러한 통합은 기존 DNS 클라이언트에 유용하며, 교차 출처 리소스 공유
   (CORS, [FETCH] 참조)를 통해 제어된 브라우저 API를 사용하여 DNS 접근이
   필요한 웹 애플리케이션에도 유용하다.

   이 프로토콜 설계의 주요 사용 사례는 두 가지이다. 첫 번째는 경로상 장치에
   의한 DNS 작업 간섭을 방지하는 것이고, 두 번째는 웹 애플리케이션이 기존
   브라우저 API를 통해 DNS 정보에 접근할 수 있도록 하는 것이다. 이 설계의
   각 사용 사례는 다른 사용 사례에서도 활용될 수 있다.

   이 사용 사례에서 DoH 프로토콜의 중점은 DNS 클라이언트(운영 체제 스텁
   리졸버 등)와 재귀 리졸버 간의 통신이다.

2. 용어

   이 프로토콜을 구현하는 서버는 다른 표준화된 전송 프로토콜을 통해 DNS
   서비스를 제공하는 "DNS 서버"와 구별하기 위해 "DoH 서버"라 한다. 마찬가지로
   이 프로토콜을 구현하는 클라이언트는 "DoH 클라이언트"라 한다.

   이 문서에서 핵심 단어 "MUST", "MUST NOT", "REQUIRED", "SHALL",
   "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED",
   "MAY", "OPTIONAL"은 여기에 표시된 것처럼 모두 대문자로 나타날 때
   BCP 14 [RFC2119] [RFC8174]에 설명된 대로 해석되어야 한다.

3. DoH 서버 선택

   DoH 클라이언트는 해석 URL을 구성하는 방법을 설명하는 URI 템플릿
   [RFC6570]을 사용하여 구성된다. 구성 및 탐색은 이 프로토콜 외부에서
   발생하며, URI 템플릿의 업데이트도 마찬가지이다. 구성은 수동(예: 사용자
   인터페이스에 URI 템플릿 입력)이거나 자동(예: DHCP 또는 유사 프로토콜을
   통한 발견)일 수 있다. DoH 서버는 하나 이상의 URI 템플릿을 지원할 수
   있다. 이를 통해 인증 요구 사항이나 서비스 수준 보장과 같은 다른 속성을
   가진 서로 다른 엔드포인트를 구현할 수 있다.

   DoH 클라이언트는 해석을 처리할 DoH 서버를 결정하기 위해 구성에 의해
   부여된 URI를 사용한다. [RFC2818]은 HTTPS에서의 URI 기반 서버 식별 방법을
   기술한다.

   DoH 클라이언트는 클라이언트의 구성 외부에서 발견되었거나(예: HTTP/2
   서버 푸시를 통해) 서버가 비요청 응답을 제공한다고 해서 다른 URI를
   사용해서는 안 된다(MUST NOT). 이는 해석 권한이 구성에 없는 URI로
   확장되는 것을 방지하여 운영, 추적, 보안 위험을 피하기 위함이다. 이러한
   시나리오를 다루는 미래 명세는 적절한 문맥과 프레임워크를 포함하도록
   기대된다.

4. HTTP 교환

4.1. HTTP 요청

   DoH 클라이언트는 개별 DNS 쿼리를 이 섹션의 요구 사항에 따라 GET 또는
   POST 메서드를 사용하여 HTTP 요청으로 인코딩한다. DoH 서버는 URI 템플릿을
   사용하여 요청 URI를 정의한다.

   URI 템플릿은 POST 요청의 경우 변수 없이 처리된다. GET 요청의 경우, 단일
   변수 "dns"가 섹션 6에 설명된 대로 DNS 요청의 내용으로 정의되며,
   base64url [RFC4648]을 사용하여 인코딩된다.

   미래의 DoH 관련 미디어 유형 명세는 이 프로토콜에서 사용할 URI 템플릿
   변수를 정의할 수 있다.

   DoH 서버는 POST 메서드와 GET 메서드를 모두 구현해야 한다(MUST).

   POST 메서드를 사용할 때, DNS 쿼리는 HTTP 메시지 본문에 포함되며,
   Content-Type 요청 헤더 필드는 메시지의 미디어 유형을 나타낸다. POST
   요청은 일반적으로 동일한 DNS 쿼리에 대한 GET 요청보다 작은 것으로
   예상된다.

   GET 메서드는 많은 수의 기존 HTTP 캐시 구현과 더 나은 호환성을 가진다.

   DoH 클라이언트는 응답에서 이해할 수 있는 콘텐츠 유형을 나타내기 위해
   HTTP Accept 요청 헤더 필드를 포함해야 한다(SHOULD). 클라이언트는 섹션
   6에 설명된 "application/dns-message" 응답을 처리할 수 있어야 한다(MUST).
   그러나 다른 DNS 관련 미디어 유형도 처리할 수 있다(MAY).

   HTTP 캐싱을 최적화하기 위해, DNS 메시지 ID 필드를 포함하는 미디어
   형식("application/dns-message" 등)을 사용하는 DoH 클라이언트는 모든 DNS
   요청에서 DNS ID를 0으로 사용해야 한다(SHOULD). HTTP 수준의
   요청-응답 상관관계가 "application/dns-message"와 같은 형식에서 ID의
   필요성을 제거한다. DNS ID를 다양하게 변경하면 의미적으로 동일한 쿼리를
   가진 DNS 요청이 다른 항목으로 캐시될 수 있다.

   DoH 클라이언트는 [RFC7540]에 따라 HTTP/2 패딩과 압축을 사용할 수 있다
   (MAY).

4.1.1. HTTP 요청 예시

   이 예시들은 [RFC7540]의 HTTP/2 스타일 형식을 사용한다.

   이 예시들은 URI 템플릿
   "https://dnsserver.example.net/dns-query{?dns}"를 가진 DoH 서비스를
   사용하여 "www.example.com"에 대한 IN A 레코드를 조회한다. 요청은
   "application/dns-message" 미디어 유형을 사용한다.

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

   이 33바이트는 [RFC1035]의 섹션 4.1에 정의된 DNS 와이어 형식의 DNS
   메시지이며, DNS 헤더에서 시작한다.

   세 번째 예시 - 확장 도메인을 사용하는 GET 요청:

   "a.62characterlabel-makes-base64url-distinct-from-standard-base64.example.com"에
   대한 GET 쿼리는 base64url 인코딩 알파벳이 표준 base64와 다르다는 점과
   패딩이 생략된다는 점을 보여준다:

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

4.2. HTTP 응답

   여기서 정의하는 단일 응답 형식은 "application/dns-message"이지만, 다른
   형식이 차후에 정의될 수 있다. DoH 서버는 "application/dns-message" 요청
   메시지를 처리할 수 있어야 한다(MUST).

   다른 응답 미디어 유형은 다양한 양의 DNS 응답 정보를 제공한다. 한 형식은
   DNS 헤더 바이트를 포함할 수 있고, 다른 형식은 이를 생략할 수 있다. 미디어
   유형 명세는 정보 범위를 결정한다.

   각 DNS 요청-응답 쌍은 하나의 HTTP 교환에 매핑된다. HTTP의
   멀티스트리밍 기능([RFC7540] 섹션 5 참조)을 통해 여러 DNS 요청-응답을
   순서에 상관없이 처리하고 전송할 수 있다.

   섹션 5.1에서 DNS 및 HTTP 응답 캐싱의 관계를 설명한다.

4.2.1. DNS 및 HTTP 오류 처리

   DNS 응답 코드는 DNS 쿼리의 성공 또는 실패를 나타낸다. 성공적인 2xx HTTP
   상태 코드([RFC7231] 섹션 6.3)는 DNS 응답 코드에 관계없이 모든 유효한 DNS
   응답에 적용된다. 예를 들어, SERVFAIL이나 NXDOMAIN과 같은 실패 DNS 응답
   코드를 가진 DNS 메시지에도 성공적인 2xx HTTP 상태 코드가 수반된다.

   비성공 HTTP 상태 코드는 원래의 DNS 질문에 대한 응답을 생성하지 않는다.
   DoH 클라이언트는 다른 HTTP 클라이언트와 동일한 비성공 HTTP 상태 코드의
   의미론적 처리를 사용해야 한다. 이는 동일한 DoH 서버에서 쿼리를 재시도
   하거나(예: HTTP 401 권한 부여 실패 [RFC7235] 섹션 3.1) 다른 서버에서
   시도하는 것(예: HTTP 415 미지원 미디어 유형 [RFC7231] 섹션 6.5.13, HTTP
   406 부적합한 표현 [RFC7231] 섹션 6.5.6)을 포함할 수 있다.

4.2.2. HTTP 응답 예시

   이 예시는 "www.example.com"에 대한 IN AAAA 레코드 쿼리에 대한 응답으로,
   재귀가 활성화되어 있다. 응답에는 주소 2001:db8:abcd:12:1:2:3:4와 TTL
   3709초를 가진 하나의 응답 레코드가 포함된다:

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

5. HTTP 통합

   이 프로토콜은 [RFC7230]의 https URI 스킴과 함께 사용되어야 한다(MUST).

   HTTP 통합에 대한 추가 고려사항은 섹션 8과 9에서 논의된다.

5.1. 캐시 상호작용

   DoH 교환은 클라이언트와 서버 사이, 또는 클라이언트 자체에 HTTP 캐시와
   DNS 전용 캐시를 모두 포함하는 계층을 통과할 수 있다. HTTP 캐시는
   범용적으로 설계되었으며 DoH 프로토콜에 대한 이해가 없다. DoH를 인식하도록
   수정된 클라이언트 구현조차도 업스트림 캐시(프록시, 게이트웨이, CDN)가
   DoH를 인식한다고 보장할 수 없다.

   따라서 DoH 서버는 GET 응답의 HTTP 캐싱 메타데이터를 신중하게 선택해야
   한다(POST 응답 캐싱은 특정 헤더가 필요하며 널리 구현되지 않아 DoH에는
   적합하지 않다).

   특히, DoH 서버는 명시적인 HTTP 신선도 수명([RFC7234] 섹션 4.2 참조)을
   할당하여 DoH 클라이언트가 최신 DNS 데이터를 사용할 가능성을 높여야
   한다(SHOULD). 이 요구 사항은 HTTP 캐시가 휴리스틱 신선도([RFC7234]
   섹션 4.2.2)를 적용하여 DoH 서버의 캐시 제어를 제거하는 것에서
   비롯된다.

   DoH HTTP 응답에 할당된 신선도 수명은 DNS 응답의 Answer 섹션에 있는 가장
   작은 TTL 이하여야 한다(MUST). Answer 섹션의 가장 작은 TTL과 동일한
   신선도 수명이 권장된다(RECOMMENDED). 예를 들어, TTL이 30초, 600초,
   300초인 RRset이 있는 경우, HTTP 신선도 수명은 30초("Cache-Control:
   max-age=30"으로 지정)여야 한다. 이는 만료된 RRset이 의도치 않게 HTTP
   캐시에서 제공되는 것을 방지한다.

   Answer 섹션에 레코드가 없지만 Authority 섹션에 SOA 레코드를 포함하는
   DNS 응답의 경우, 응답의 신선도 수명은 해당 SOA 레코드의 MINIMUM 필드보다
   크면 안 된다(MUST NOT) [RFC2308].

   stale-while-revalidate 및 stale-if-error Cache-Control 지시문([RFC5861])은
   서버 정책이 허용하는 경우 DoH 구현에 적합하다. 이러한 메커니즘을 통해
   클라이언트는 서버의 재량에 따라 더 이상 신선하지 않은 캐시 항목을 재사용할
   수 있으며, 완전한 캐시 항목 또는 캐시 항목 없음을 재사용한다.

   DoH 서버는 전역적으로 유효하지 않은 응답에 대한 HTTP 캐싱을 처리해야
   한다. 클라이언트 ID에 맞춤화된 응답의 경우, 서버는 Cache-Control
   max-age=0이나 Vary 응답 헤더([RFC7231] 섹션 7.1.4)를 통해 보조 캐시
   키([RFC7234] 섹션 4.1)를 설정하여 전역 재사용을 방지해야 한다.

   DoH 클라이언트는 응답의 DNS TTL을 계산할 때 Age 응답 헤더 필드의 값
   [RFC7234]을 고려해야 한다(MUST). 예를 들어, DNS TTL 600초의 RRset을
   받았지만 Age 헤더가 250초의 캐시 기간을 나타내는 경우, 남은 수명은
   350초이다. 이는 DoH 클라이언트의 HTTP 캐시와 DNS 캐시 모두에 적용된다.

   DoH 클라이언트는 "no-cache" 요청 Cache-Control 지시문([RFC7234]
   섹션 5.2.1.4)과 유사한 제어를 사용하여 캐시되지 않은 HTTP 응답 사본을
   요청할 수 있다(MAY). 일부 캐시는 구성이나 전통적 DNS 캐시 상호작용에
   이러한 메커니즘이 없기 때문에 이러한 지시문을 준수하지 않을 수 있다.

   HTTP 조건부 요청([RFC7232])은 DoH에서 제한된 가치를 제공하며, DNS
   트랜잭션이 여전히 지연시간에 의해 제한되는 동안 대역폭 이점만 제공한다.
   HTTP 재검증 헤더(Last-Modified, Etag)는 종종 전체 DNS 응답 크기를 초과하며
   가변적인 특성으로 인해 HTTP/2 압축 사전([RFC7541])에 부담을 준다. 영역
   전송과 같은 대규모 DNS 데이터는 재검증에서 더 많은 이점을 얻을 수 있다.

5.2. HTTP/2

   HTTP/2 [RFC7540]는 DoH와 함께 사용하기 위한 최소 권장 HTTP 버전이다
   (RECOMMENDED).

   기존의 UDP 기반 DNS 메시지는 본질적으로 비순서적이며 낮은 오버헤드를
   가진다. 경쟁력 있는 HTTP 전송은 유사한 성능을 위해 재정렬, 병렬 처리,
   우선순위, 헤더 압축을 지원해야 한다. HTTP/2는 이러한 기능을 도입했다.
   이전 버전의 HTTP는 DoH의 의미론적 요구 사항을 지원하지만 성능이 저하된다.

5.3. 서버 푸시

   DoH 응답 데이터를 DNS 해석에 적용하기 전에, 클라이언트는 HTTP 요청 URI가
   DoH 쿼리에 적합한지 확인해야 한다. DoH 클라이언트가 시작한 요청의 경우,
   URI 선택으로 인해 이는 암묵적이다. HTTP 서버 푸시([RFC7540] 섹션 8.2)의
   경우, 표준 서버 푸시 보안 검사와 함께 푸시된 URI가 클라이언트의 의도된
   쿼리 대상과 일치하는지 추가 주의가 필요하다.

5.4. 콘텐츠 협상

   상호운용성을 극대화하기 위해, DoH 클라이언트와 DoH 서버는
   "application/dns-message" 미디어 유형을 지원해야 한다(MUST). HTTP 콘텐츠
   협상([RFC7231] 섹션 3.4 참조)에 의해 정의된 다른 미디어 유형도 사용할
   수 있다(MAY). 대체 미디어 유형은 모든 표준 UDP DNS 쿼리(확장을 포함하되,
   다중 응답 쿼리는 제외)를 표현할 수 있어야 한다.

6. "application/dns-message" 미디어 유형 정의

   "application/dns-message" 미디어 유형의 페이로드는 [RFC1035]
   섹션 4.2.1에 정의된 단일 DNS 온더와이어 메시지이며, [RFC1035]
   섹션 4.1의 전체 형식을 참조한다.

   [RFC1035]는 UDP로 전송되는 DNS 메시지를 512바이트로 제한했으나,
   [RFC6891]이 이를 업데이트했다. 이 미디어 유형은 최대 DNS 메시지
   크기를 65535바이트로 제한한다.

   이 와이어 형식은 [RFC7858]의 형식(2바이트 길이를 포함하는 [RFC1035]
   섹션 4.2.2를 사용)과 다르다.

   이 미디어 유형을 사용하는 DoH 클라이언트는 하나 이상의 EDNS(DNS용 확장
   메커니즘, [RFC6891]) 옵션을 포함할 수 있다(MAY). 이 미디어 유형을
   사용하는 DoH 서버는 DNS 요청에서 EDNS UDP 페이로드 크기에 지정된 값을
   무시해야 한다(MUST).

   GET 요청의 경우, 이 미디어 유형의 데이터 페이로드는 base64url
   [RFC4648]로 인코딩된 다음 "dns"라는 변수로 URI 템플릿 확장에 제공되어야
   한다(MUST). base64url의 패딩 문자는 포함되어서는 안 된다(MUST NOT).

   POST 요청의 경우, 이 미디어 유형의 데이터 페이로드는 인코딩되어서는
   안 되며(MUST NOT) HTTP 메시지 본문으로 직접 사용된다.

7. IANA 고려사항

7.1. "application/dns-message" 미디어 유형 등록

   Type name: application

   Subtype name: dns-message

   Required parameters: N/A

   Optional parameters: N/A

   Encoding considerations: 이것은 바이너리 형식이다. 내용은 [RFC1035]에
      정의된 DNS 메시지이다. 여기서 사용되는 형식은 DNS over UDP에 사용되는
      형식이며, [RFC1035]의 다이어그램에 해당한다.

   Security considerations: RFC 8484를 참조한다. 내용은 DNS 메시지이며
      실행 가능한 코드가 아니다.

   Interoperability considerations: 없음.

   Published specification: RFC 8484.

   Applications that use this media type: 전체 DNS 메시지를 교환하는 시스템.

   Additional information:

      Deprecated alias names for this type: N/A

      Magic number(s): N/A

      File extension(s): N/A

      Macintosh file type code(s): N/A

   Person & email address to contact for further information:
      Paul Hoffman, paul.hoffman@icann.org

   Intended usage: COMMON

   Restrictions on usage: N/A

   Author: Paul Hoffman, paul.hoffman@icann.org

   Change controller: IESG

8. 프라이버시 고려사항

   [RFC7626]은 "와이어상"(섹션 2.4) 및 "서버 내"(섹션 2.5) 맥락에서 DNS
   프라이버시 고려사항을 검토한다. 이 프레이밍은 DoH 프라이버시 고려사항에도
   유용하게 적용된다.

8.1. 와이어상

   DoH는 DNS 트래픽을 암호화하고 서버 인증을 요구하여, 수동 감시([RFC7258])와
   DNS 트래픽을 악성 서버로 전환하는 능동 공격([RFC7626] 섹션 2.5.1)을
   완화한다. DNS over TLS([RFC7858])도 유사한 보호를 제공하나, 직접적인
   UDP 및 TCP 전송은 여전히 취약하다. 실험적 패딩 길이 지침은 [RFC8467]에
   나타난다.

   또한, HTTPS의 기본 포트 443 사용과 동일한 연결에서 DoH 트래픽이 다른
   HTTPS 트래픽과 혼합되는 점은 권한 없는 경로상 장치의 DNS 작업 간섭을
   억제하고 DNS 트래픽 분석을 어렵게 만든다.

8.2. 서버 내

   DNS 와이어 형식에는 클라이언트 식별자가 포함되지 않지만, 전송 메커니즘은
   상관관계 데이터를 제공한다. HTTPS는 명시적 HTTP 쿠키와 HTTP 요청 헤더
   필드 세트 및 순서에서 오는 암묵적 핑거프린팅을 포함하여 새로운 상관관계
   고려사항을 도입한다.

   DoH 구현은 IP, TCP, TLS, HTTP 계층 위에 놓인다. 각 계층은 상관관계
   기능을 포함한다. DNS 전송은 일반적으로 구현 계층의 프라이버시 속성을
   상속한다. 예를 들어, DNS over TLS 전송은 IP, TCP, TLS의 속성을 가진다.

   HTTPS 계층과 관련된 DoH의 프라이버시 고려사항은 DNS over TLS에 비해
   점진적이다. DoH는 HTTPS와 관련된 것 이상의 알려진 우려를 도입하지 않는다.

   IP 수준의 클라이언트 주소는 명백한 상관관계 정보를 제공한다. NAT,
   프록시, VPN, 주소 순환이 이를 완화할 수 있다. 동일한 엔티티가 DNS
   서버와 DHCP 서버를 동시에 운영하면 이를 악화시킬 수 있다.

   여러 요청에 단일 TCP 연결을 사용하는 DNS 구현은 해당 요청들을 직접
   그룹화한다. 장기 연결은 우수한 성능을 보이지만 더 많은 요청을
   그룹화하여 잠재적으로 상관관계 및 통합 정보를 노출시킨다. TCP 기반
   솔루션은 TCP Fast Open([RFC7413])을 사용할 수 있다. TCP Fast Open
   쿠키는 서버 측에서 TCP 세션 상관관계를 가능하게 한다.

   TLS 기반 구현은 종종 [RFC8446] 섹션 2.2와 같은 세션 재개 메커니즘을 통해
   핸드셰이크 성능을 향상시킨다. 세션 재개는 TLS 세션을 연결하기 위한
   사소한 상관관계 메커니즘을 만든다.

   HTTP의 기능 세트는 여러 메커니즘을 통해 식별과 추적을 가능하게 한다.
   인증 헤더는 프로필을 명시적으로 식별하며, HTTP 쿠키는 명시적 상태 추적
   및 인증 메커니즘으로 작용한다.

   User-Agent와 Accept-Language 헤더는 종종 클라이언트 버전이나 로캘 정보를
   전달하여 콘텐츠 협상과 운영 버그 해결을 용이하게 한다. 캐시 제어 헤더는
   클라이언트 히스토리의 부분 집합 정보를 노출할 수 있다. 동일한 연결에서
   DoH 요청이 다른 HTTP 요청과 혼합되면 더 풍부한 데이터 상관관계를
   지원한다.

   DoH 프로토콜 설계는 애플리케이션이 여기에 열거되지 않은 기능을 포함하여
   HTTP 생태계를 완전히 활용할 수 있도록 한다. HTTP 기능의 전체 세트를
   활용하면 DoH가 HTTP 터널 이상이 될 수 있지만, 이는 구현을 HTTP의
   프라이버시 고려사항 전체에 노출시키는 대가가 있다.

   DoH 클라이언트와 서버의 구현은 이러한 기능의 이점과 프라이버시 영향,
   배포 맥락을 고려하여 활성화 여부를 결정해야 한다. 구현은 원하는 기능
   세트를 달성하는 데 필요한 최소한의 데이터 세트를 노출하도록 권고된다.

   DoH 구현에 HTTP 쿠키 [RFC6265] 지원이 필요한지 결정하는 것이 특히
   중요한데, HTTP 쿠키가 HTTP의 주요 상태 추적 메커니즘이기 때문이다. HTTP
   쿠키는 사용 사례에서 명시적으로 요구되지 않는 한 DoH 클라이언트에 의해
   수락되어서는 안 된다(SHOULD NOT).

9. 보안 고려사항

   HTTPS를 통한 DNS는 기본 HTTP 전송 보안에 의존한다. 이는 기존 UDP 기반
   DNS 증폭 공격을 완화한다. HTTP/2 구현은 [RFC7540] 섹션 9.2에 정의된
   TLS 프로필에서 이점을 얻는다.

   세션 수준 암호화는 인정된 트래픽 분석 취약점이 있으며, 이는 DNS 쿼리에서
   특히 심각할 수 있다. HTTP/2는 압축([RFC7540] 섹션 10.6) 및 패딩([RFC7540]
   섹션 10.7) 지침을 제공한다. DoH 서버는 DoH 클라이언트가 요청하면 DNS
   패딩([RFC7830])을 적용할 수 있다. 실험적 패딩 길이 지침은 [RFC8467]에
   나타난다.

   HTTPS 연결은 DoH 서버와 클라이언트 간의 전송 보안을 제공하지만,
   DNSSEC에서 제공하는 DNS 데이터 응답 무결성은 제공하지 않는다. DNSSEC과
   DoH는 독립적이고 완전히 호환되는 프로토콜로서 서로 다른 문제를 해결한다.
   하나의 사용이 다른 하나의 필요성이나 유용성을 감소시키지 않는다.
   클라이언트는 전체 DNSSEC 검증을 수행하거나 DoH 서버의 검증을 신뢰하여
   AD(Authentic Data) 비트를 검사하여 진본 여부를 판단할 수 있다. 다른 응답
   미디어 유형은 다양한 양의 DNS 응답 정보를 제공하여 이 선택에 잠재적으로
   영향을 미칠 수 있다.

   섹션 5.1에서는 이 프로토콜의 HTTP 캐싱 상호작용을 설명한다. 클라이언트가
   사용하는 캐시를 제어하는 공격자는 클라이언트의 DNS 관점에 영향을 미칠
   수 있다. 이는 다른 프로토콜에 대한 HTTP 캐싱의 보안 영향과 일치한다.

   DNSSEC 정보가 없는 경우, DoH 서버는 잘못된 응답 데이터를 제공할 수
   있다. 섹션 3에서는 DoH DNS 응답 사용을 구성된 서버로 제한한다. 이것이
   잘못된 데이터 보호를 보장하지는 않지만 위험을 줄인다.

10. 운영 고려사항

   로컬 정책 및 유사 요인으로 인해 다른 DNS 서버가 동일한 쿼리에 대해
   다른 결과를 반환할 수 있으며, 이는 분할 DNS 구성([RFC6950])에서와
   같다. 따라서 쿼리되는 서버의 선택이 최종 결과에 영향을 미칠 수 있다.
   DNS64([RFC6147])의 경우, 선택에 따라 IPv6/IPv4 변환 기능이 결정될 수
   있다.

   이 명세의 HTTPS 채널은 DoH 클라이언트와 서버 간의 안전한 양자간 통신을
   수립한다. 보안되지 않은 DNS 전송에 의존하는 필터링 또는 검사 시스템은
   TLS가 제공하는 기밀성과 무결성 보호로 인해 DNS over HTTPS 환경에서
   작동을 중단한다.

   일부 HTTPS 클라이언트 구현은 실시간 제3자 인증서 해지 상태 확인을
   수행한다. 이것이 DoH 서버 연결 중에 실행되고 해당 확인이 제3자 연결을
   위해 DNS 해석을 필요로 하면 교착 상태가 발생한다. Online Certificate
   Status Protocol(OCSP) 서버([RFC6960]) 또는 인증서 해지 목록(CRL)
   가져오기를 위한 Authority Information Access(AIA)([RFC5280] 섹션
   4.2.2.1)가 교착 상태 메커니즘의 예이다. 완화를 위해, DoH 서버의 인증은
   TLS 핸드셰이크에서 외부 리소스에 대한 DNS 기반 참조에 의존해서는 안
   된다(SHOULD NOT). OCSP는 TLS 1.3용 [RFC8446] 섹션 4.4.2.1과 같은
   메커니즘을 통해 인증서 상태 번들링을 허용한다. AIA 교착 상태는 중간
   인증서 제공으로 방지한다. 이러한 교착 상태는 리디렉트 대상 서버에 대해서도
   고려해야 한다.

   DoH 클라이언트는 HTTP 요청이 DoH 서버 호스트명 해석을 필요로 할 때
   유사한 부트스트래핑 과제에 직면한다. 전통적인 DNS 네임서버 주소는 동일한
   서버에서 초기 결정이 불가능하며; 마찬가지로 DoH 클라이언트는 초기
   호스트명-주소 해석에 자체 DoH 서버를 사용할 수 없다. 대안적 클라이언트
   전략에는 다음이 포함된다: 1) 초기 해석이 구성에 통합됨, 2) IP 기반
   URI와 해당하는 IP 기반 HTTPS 인증서 사용, 또는 3) HTTPS 연결 인증을
   유지하면서 전통적 DNS 또는 다른 DoH 서버를 통한 호스트명 해석.

   HTTP는 무상태 애플리케이션 수준 프로토콜이므로, DoH 구현은 상태 유지
   요청 순서 보장이 없다. DoH는 엄격한 순서를 요구하는 프로토콜을 전송할
   수 없다.

   DoH 서버는 유효한 모든 DNS 응답으로 응답한다. 예를 들어, 유효한 응답에는
   서버가 검색한 응답이 불완전함을 나타내는 TC(truncation) 비트가 포함되어
   최선 노력의 응답을 제공할 수 있다. DoH 서버는 이행 불가능한 쿼리에
   대해 HTTP 오류를 사용할 수 있다. 이 동일한 예시에서 DoH 서버는 비오류
   TC 비트 응답 대신 HTTP 오류를 사용할 수도 있다.

   [RFC6891]을 통한 DNS 확장이 확산되었다. [RFC7828]을 포함한 전송 특정
   확장은 DoH에 적용되지 않는다.

11. 참조

11.1. 규범적 참조

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

11.2. 정보적 참조

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

부록 A. 프로토콜 개발

   이 부록은 DoH의 설계 요구 사항을 설명한다. 독자가 현재 프로토콜을
   이해하는 데 도움을 주기 위해 나열되었으며, 미래 개발을 제한하기 위한
   것이 아니다. 이 부록은 비규범적이다.

   설명된 프로토콜 설계는 다음의 프로토콜 요구 사항에 기반한다:

   o 프로토콜은 일반적인 HTTP 의미론을 사용해야 한다.

   o 쿼리와 응답은 확장을 포함하되 다중 응답을 제외한 모든 표준 UDP 기반
     DNS 쿼리를 유연하게 표현해야 한다.

   o 프로토콜은 새로운 DNS 쿼리 및 응답 형식을 허용해야 한다.

   o 프로토콜은 필수적인 단일 형식 명세를 통해 상호운용성을 보장해야
     한다. 해당 형식은 하나 이상의 EDNS 옵션(정의되지 않은 것 포함)을
     포함하여 미래의 DNS 프로토콜 수정을 지원해야 한다.

   o 프로토콜은 HTTPS를 준수하는 보안 전송을 사용해야 한다.

   다음은 비요구 사항으로 간주되었다:

   o 네트워크 특정 DNS64([RFC6147]) 지원

   o 다른 네트워크 특정 평문 DNS 쿼리 추론 지원

   o 비보안 HTTP 지원

부록 B. HTTP를 통한 DNS 또는 기타 형식에 관한 이전 작업

   다음은 HTTP/1을 통한 DNS 또는 다른 형식으로의 DNS 데이터 표현에 관한
   이전 작업의 불완전한 목록이다. 이 목록에는 tools.ietf.org 링크(문서는
   모두 만료됨)와 소프트웨어 웹사이트가 포함된다.

   o <https://tools.ietf.org/html/draft-mohan-dns-query-xml>

   o <https://tools.ietf.org/html/draft-daley-dnsxml>

   o <https://tools.ietf.org/html/draft-dulaunoy-dnsop-passive-dns-cof>

   o <https://tools.ietf.org/html/draft-bortzmeyer-dns-json>

   o <https://www.nlnetlabs.nl/projects/dnssec-trigger/>

감사의 글

   이 작업은 다양한 기술 전문가들 간의 높은 수준의 협력을 필요로 했다.
   Ray Bellis, Stephane Bortzmeyer, Manu Bretelle, Sara Dickinson,
   Massimiliano Fantuzzi, Tony Finch, Daniel Kahn Gilmor, Olafur
   Gudmundsson, Wes Hardaker, Rory Hewitt, Joe Hildebrand, David
   Lawrence, Eliot Lear, John Mattsson, Alex Mayrhofer, Mark
   Nottingham, Jim Reid, Adam Roach, Ben Schwartz, Davey Song, Daniel
   Stenberg, Andrew Sullivan, Martin Thomson, Sam Weiler에게 진심으로
   감사드린다.

저자 주소

   Paul Hoffman
   ICANN

   Email: paul.hoffman@icann.org


   Patrick McManus
   Mozilla

   Email: mcmanus@ducksong.com
