Internet Engineering Task Force (IETF)                        C. Huitema
Request for Comments: 9250                          Private Octopus Inc.
Category: Standards Track                                   S. Dickinson
ISSN: 2070-1721                                               Sinodun IT
                                                               A. Mankin
                                                              Salesforce
                                                                May 2022


                  DNS over Dedicated QUIC Connections

초록

   이 문서는 DNS에 대한 전송 기밀성을 제공하기 위한 QUIC의 사용을
   기술한다. QUIC이 제공하는 암호화는 TLS가 제공하는 것과 유사한
   속성을 가지며, QUIC 전송은 TCP에 내재하는 head-of-line blocking
   문제를 제거하고 UDP보다 더 효율적인 패킷 손실 복구를 제공한다.
   DNS over QUIC(DoQ)은 RFC 7858에 명시된 DNS over TLS(DoT)와 유사한
   프라이버시 속성을 가지며, 기존 DNS over UDP와 유사한 지연 특성을
   가진다. 이 명세는 DNS를 위한 범용 전송으로서의 DoQ 사용을
   기술하며, stub에서 recursive로, recursive에서 authoritative로,
   그리고 영역 전송 시나리오에서의 DoQ 사용을 포함한다.

이 메모의 상태

   이것은 Internet Standards Track 문서이다.

   이 문서는 Internet Engineering Task Force(IETF)의 산출물이다.
   이것은 IETF 커뮤니티의 합의를 대표한다. 이것은 공개 검토를
   받았으며 Internet Engineering Steering Group(IESG)에 의해
   발행이 승인되었다. Internet Standards에 대한 추가 정보는
   RFC 7841의 Section 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는
   방법에 대한 정보는 https://www.rfc-editor.org/info/rfc9250 에서
   얻을 수 있다.

저작권 고지

   Copyright (c) 2022 IETF Trust and the persons identified as the
   document authors. All rights reserved.

   이 문서는 BCP 78과 이 문서의 발행일에 유효한 IETF Trust의 IETF
   문서에 관한 법적 조항(https://trustee.ietf.org/license-info)의
   적용을 받는다. 이 문서들은 이 문서에 대한 귀하의 권리와 제한을
   기술하므로 주의 깊게 검토하기 바란다. 이 문서에서 추출된 코드
   구성 요소는 Trust Legal Provisions의 Section 4.e에 기술된 대로
   Revised BSD License 텍스트를 포함해야 하며, Revised BSD License에
   기술된 대로 보증 없이 제공된다.

목차

   1.  서론
   2.  핵심 용어
   3.  설계 고려사항
     3.1.  DNS 프라이버시 제공
     3.2.  최소 지연을 위한 설계
     3.3.  중간 장치 고려사항
     3.4.  서버 시작 트랜잭션 없음
   4.  명세
     4.1.  연결 설정
       4.1.1.  포트 선택
     4.2.  스트림 매핑 및 사용
       4.2.1.  DNS Message ID
     4.3.  DoQ 오류 코드
       4.3.1.  트랜잭션 취소
       4.3.2.  트랜잭션 오류
       4.3.3.  프로토콜 오류
       4.3.4.  대체 오류 코드
     4.4.  연결 관리
     4.5.  세션 재개 및 0-RTT
     4.6.  메시지 크기
   5.  구현 요구사항
     5.1.  인증
     5.2.  연결 실패 시 다른 프로토콜로의 폴백
     5.3.  주소 검증
     5.4.  패딩
     5.5.  연결 처리
       5.5.1.  연결 재사용
       5.5.2.  리소스 관리
       5.5.3.  0-RTT 및 세션 재개 사용
       5.5.4.  프라이버시를 위한 연결 마이그레이션 제어
     5.6.  쿼리 병렬 처리
     5.7.  영역 전송
     5.8.  흐름 제어 메커니즘
   6.  보안 고려사항
   7.  프라이버시 고려사항
     7.1.  0-RTT 데이터의 프라이버시 문제
     7.2.  세션 재개의 프라이버시 문제
     7.3.  주소 검증 토큰의 프라이버시 문제
     7.4.  장기 세션의 프라이버시 문제
     7.5.  트래픽 분석
   8.  IANA 고려사항
     8.1.  DoQ 식별 문자열 등록
     8.2.  전용 포트 예약
     8.3.  Extended DNS Error Code: Too Early 예약
     8.4.  DNS-over-QUIC Error Codes 레지스트리
   9.  참고문헌
     9.1.  규범적 참고문헌
     9.2.  참고적 참고문헌
   부록 A.  NOTIFY 서비스
   감사의 글
   저자 주소

1.  서론

   Domain Name System(DNS) 개념은 "Domain names - concepts and
   facilities" [RFC1034]에 명시되어 있다. UDP 및 TCP를 통한 DNS 쿼리
   및 응답의 전송은 "Domain names - implementation and specification"
   [RFC1035]에 명시되어 있다.

   이 문서는 QUIC 전송 [RFC9000] [RFC9001]을 통한 DNS 프로토콜의
   매핑을 제시한다. DNS over QUIC은 "DNS Terminology" [DNS-TERMS]에
   따라 여기서 DoQ로 지칭된다.

   DoQ 매핑의 목표는 다음과 같다:

   1.  DoT [RFC7858]와 동일한 DNS 프라이버시 보호를 제공한다. 이는
       "Usage Profiles for DNS over TLS and DNS over DTLS"
       [RFC8310]에 명시된 대로 인증 도메인 이름을 통해 클라이언트가
       서버를 인증하는 옵션을 포함한다.

   2.  기존 DNS over UDP에 비해 DNS 서버에 대한 향상된 수준의 출발지
       주소 검증을 제공한다.

   3.  전송할 수 있는 DNS 응답의 크기에 경로 MTU 제한을 부과하지
       않는 전송을 제공한다.

   이러한 목표를 달성하고 DNS 암호화에 대한 진행 중인 작업을
   지원하기 위해, 이 문서의 범위에는 다음이 포함된다:

   *  "stub에서 recursive resolver로"의 시나리오 (이 문서에서는
      "stub에서 recursive로"의 시나리오라고도 한다)

   *  "recursive resolver에서 authoritative nameserver로"의 시나리오
      (이 문서에서는 "recursive에서 authoritative로"의 시나리오라고도
      한다), 그리고

   *  "nameserver에서 nameserver로"의 시나리오 (주로 영역 전송(XFR)
      [RFC1995] [RFC5936]에 사용된다).

   달리 말하면, 이 문서는 DNS를 위한 범용 전송으로서 QUIC을
   명시한다.

   이 문서의 구체적인 비목표는 다음과 같다:

   1.  중간 장치에 의한 DoQ 트래픽의 잠재적 차단을 회피하려는 시도는
       하지 않는다.

   2.  DNS Stateful Operations(DSO) [RFC8490]에서만 사용되는 서버
       시작 트랜잭션을 지원하려는 시도는 하지 않는다.

   QUIC을 통한 애플리케이션의 전송을 명시하려면 애플리케이션의
   메시지가 QUIC 스트림에 어떻게 매핑되는지, 그리고 일반적으로
   애플리케이션이 QUIC을 어떻게 사용할 것인지를 명시해야 한다.
   이것은 HTTP의 경우 "Hypertext Transfer Protocol Version 3
   (HTTP/3)" [HTTP/3]에서 수행된다. 이 문서의 목적은 DNS 메시지가
   QUIC을 통해 전송될 수 있는 방법을 정의하는 것이다.

   DNS over HTTPS(DoH) [RFC8484]는 HTTP/3과 함께 사용하여 QUIC의
   일부 이점을 얻을 수 있다. 그러나 DoQ를 위한 경량의 직접 매핑은
   중개자가 거의 관여하지 않는 recursive에서 authoritative로의
   시나리오와 영역 전송 시나리오 모두에 더 자연스러운 적합으로 간주될
   수 있다. 이러한 시나리오에서는 HTTP의 추가 오버헤드가 예를 들어
   HTTP 프록싱 및 캐싱 동작의 이점으로 상쇄되지 않는다.

   이 문서에서, Section 3은 제안된 설계를 안내한 추론을 제시한다.
   Section 4는 DoQ의 실제 매핑을 명시한다. Section 5는 DoQ의 구현,
   사용 및 배포에 대한 지침을 제시한다.

2.  핵심 용어

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY",
   그리고 "OPTIONAL"이라는 핵심 용어는 여기에 표시된 것처럼 모두
   대문자로 나타나는 경우에만 BCP 14 [RFC2119] [RFC8174]에 기술된
   대로 해석되어야 한다.

3.  설계 고려사항

   이 섹션과 그 하위 섹션은 DoQ에 사용된 설계 지침을 제시한다.
   이 문서의 다른 모든 섹션은 규범적이지만, 이 섹션은 본질적으로
   정보 제공적이다.

3.1.  DNS 프라이버시 제공

   DoT [RFC7858]는 TLS를 통한 DNS 전송을 명시함으로써 "DNS Privacy
   Considerations" [RFC9076]에 기술된 문제를 완화하는 방법을
   정의한다. "Usage Profiles for DNS over TLS and DNS over DTLS"
   [RFC8310]은 stub resolver 인증 메커니즘을 포함하여 DoT의 사용
   프로필을 명시한다.

   QUIC 연결 설정은 "Using TLS to Secure QUIC" [RFC9001]에 명시된
   대로 TLS를 사용한 보안 매개변수의 협상을 포함하며, QUIC 전송의
   암호화를 가능하게 한다. QUIC을 통한 DNS 메시지 전송은 사용
   프로필 [RFC8310]을 포함하여 DoT [RFC7858]와 본질적으로 동일한
   프라이버시 보호를 제공할 것이다. 이에 대한 추가 논의는 Section 7
   에 제공된다.

3.2.  최소 지연을 위한 설계

   QUIC은 다음과 같은 기능을 통해 프로토콜로 인한 지연을 줄이도록
   특별히 설계되었다:

   1.  세션 재개 중 0-RTT 데이터 지원.

   2.  "QUIC Loss Detection and Congestion Control" [RFC9002]에
       명시된 대로 고급 패킷 손실 복구 절차 지원.

   3.  여러 스트림에서 데이터의 병렬 전달을 허용함으로써 head-of-line
       blocking의 완화.

   DNS의 QUIC으로의 이 매핑은 세 가지 방법으로 이러한 기능을
   활용할 것이다:

   1.  세션 재개 중 0-RTT 데이터 전송에 대한 선택적 지원 (이에
       대한 보안 및 프라이버시 영향은 이후 섹션에서 논의된다).

   2.  여러 DNS 트랜잭션이 수행되는 장기 QUIC 연결로, 고급 복구
       기능의 이점을 얻는 데 필요한 지속적 트래픽을 생성한다.

   3.  head-of-line blocking을 완화하기 위해 각 DNS 쿼리/응답
       트랜잭션을 별도의 스트림에 매핑한다. 이를 통해 서버는 쿼리에
       "순서와 관계없이" 응답할 수 있다. 또한 클라이언트는 서버가
       이전에 게시한 응답의 순서대로 전달을 기다리지 않고 응답이
       도착하는 대로 처리할 수 있다.

   이러한 고려사항은 Section 4.2에서의 DNS 트래픽의 QUIC 스트림으로의
   매핑에 반영되어 있다.

3.3.  중간 장치 고려사항

   QUIC을 사용하면 암호화 및 패딩, 트래픽 페이싱, 트래픽 셰이핑과
   같은 트래픽 분석 저항 기법을 사용하여 프로토콜이 네트워크 경로상의
   장치로부터 그 목적을 위장할 수 있을 것이다. 이 명세는 그러한
   분류를 회피하도록 설계된 조치를 포함하지 않는다; Section 5.4에
   정의된 패딩 메커니즘은 DNS 쿼리 및 응답에 포함된 특정 레코드를
   난독화하기 위한 것이지만, 이것이 DNS 트래픽이라는 사실을
   난독화하기 위한 것은 아니다. 따라서 방화벽 및 기타 중간 장치는
   DoQ를 HTTP와 같이 QUIC을 사용하는 다른 프로토콜과 구별하고 다른
   처리를 적용할 수 있을 것이다.

   이 명세에서 프로토콜 분류를 회피하기 위한 조치의 부재는 그러한
   관행의 지지를 의미하지 않는다.

3.4.  서버 시작 트랜잭션 없음

   Section 1에 명시된 바와 같이, 이 문서는 설정된 DoQ 연결 내에서의
   서버 시작 트랜잭션에 대한 지원을 명시하지 않는다. 즉, DoQ 연결의
   개시자만이 해당 연결을 통해 쿼리를 보낼 수 있다.

   DSO는 기존 연결 내에서 서버 시작 트랜잭션을 지원한다. 그러나
   여기서 정의된 DoQ는 메시지의 순서대로 전달을 보장하지 않기
   때문에 DSO의 적용 가능한 전송 기준을 충족하지 않는다;
   [RFC8490]의 Section 4.2를 참조하라.

4.  명세

   DoQ 연결은 QUIC 전송 명세 [RFC9000]에 기술된 대로 설정된다.
   연결 설정 중, DoQ 지원은 암호화 핸드셰이크에서 Application-Layer
   Protocol Negotiation(ALPN) 토큰 "doq"를 선택함으로써 표시된다.

4.1.  연결 설정

   DoQ 연결은 QUIC 전송 명세 [RFC9000]에 기술된 대로 설정된다.
   연결 설정 중, DoQ 지원은 암호화 핸드셰이크에서 Application-Layer
   Protocol Negotiation(ALPN) 토큰 "doq"를 선택함으로써 표시된다.

4.1.1.  포트 선택

   기본적으로, DoQ를 지원하는 DNS 서버는 다른 포트를 사용하기로 하는
   상호 합의가 없는 한 전용 UDP 포트 853(Section 8)에서 QUIC 연결을
   수신 대기하고 수락해야 한다(MUST).

   기본적으로, 특정 서버와 DoQ를 사용하고자 하는 DNS 클라이언트는
   다른 포트를 사용하기로 하는 상호 합의가 없는 한 서버의 UDP 포트
   853에 QUIC 연결을 설정해야 한다(MUST).

   DoQ 연결은 UDP 포트 53을 사용해서는 안 된다(MUST NOT). DoQ에
   대한 포트 53 사용에 대한 이 권고는 DoQ와 DNS over UDP [RFC1035]의
   사용 사이의 혼동을 피하기 위한 것이다. 두 당사자가 포트 53에
   동의하더라도 그 합의를 모르는 다른 당사자가 여전히 해당 포트를
   사용하려고 시도할 수 있기 때문에 혼동의 위험이 존재한다.

   stub에서 recursive 시나리오에서, 상호 합의된 대체 포트로서 포트
   443의 사용은 운영상 유익할 수 있다. 왜냐하면 포트 443은 QUIC 및
   HTTP-3을 사용하는 많은 서비스에 사용되므로 다른 포트보다 차단될
   가능성이 적기 때문이다. stub이 사용자 정의 포트의 사용을 포함하여
   암호화된 전송을 제공하는 recursive를 발견하기 위한 여러 메커니즘은
   진행 중인 작업의 주제이다.

4.2.  스트림 매핑 및 사용

   QUIC 스트림을 통한 DNS 트래픽의 매핑은 QUIC 전송 명세인
   [RFC9000]의 Section 2에 상세히 기술된 QUIC 스트림 기능을
   활용한다.

   DNS 쿼리/응답 트래픽 [RFC1034] [RFC1035]은 클라이언트가 쿼리를
   보내고 서버가 하나 이상의 응답을 제공하는(여러 응답은 영역
   전송에서 발생할 수 있다) 단순한 패턴을 따른다.

   여기서 명시된 매핑은 클라이언트가 각 쿼리에 대해 별도의 QUIC
   스트림을 선택하도록 요구한다. 그런 다음 서버는 해당 쿼리에 대한
   모든 응답 메시지를 제공하기 위해 동일한 스트림을 사용한다. 여러
   응답이 파싱될 수 있도록, DNS over TCP [RFC1035]에 정의된 2옥텟
   길이 필드와 정확히 동일한 방식으로 2옥텟 길이 필드가 사용된다.
   이것의 실질적 결과는 각 QUIC 스트림의 내용이 정확히 하나의
   쿼리를 관리하는 TCP 연결의 내용과 정확히 동일하다는 것이다.

   DoQ 연결을 통해 전송되는 모든 DNS 메시지(쿼리 및 응답)는
   [RFC1035]에 명시된 대로 2옥텟 길이 필드 다음에 메시지 내용으로
   인코딩되어야 한다(MUST).

   클라이언트는 QUIC 전송 명세 [RFC9000]에 부합하여 QUIC 연결에서
   각 후속 쿼리에 대해 다음으로 사용 가능한 클라이언트 시작 양방향
   스트림을 선택해야 한다(MUST). 패킷 손실 및 기타 네트워크 이벤트로
   인해 쿼리가 다른 순서로 도착할 수 있다. 서버는 쿼리가 도착하는
   대로 처리해야 한다(SHOULD). 그렇지 않으면 불필요한 지연이
   발생할 것이기 때문이다.

   클라이언트는 선택한 스트림을 통해 DNS 쿼리를 보내야 하며(MUST),
   해당 스트림에서 더 이상 데이터가 전송되지 않을 것임을 STREAM FIN
   메커니즘을 통해 표시해야 한다(MUST).

   서버는 동일한 스트림에서 응답(들)을 보내야 하며(MUST), 마지막
   응답 후에 해당 스트림에서 더 이상 데이터가 전송되지 않을 것임을
   STREAM FIN 메커니즘을 통해 표시해야 한다(MUST).

   따라서 단일 DNS 트랜잭션은 단일 양방향 클라이언트 시작 스트림을
   소비한다. 이는 클라이언트의 첫 번째 쿼리가 QUIC 스트림 0에서
   발생하고, 두 번째는 4에서, 이런 식으로 계속됨을 의미한다
   ([RFC9000]의 Section 2.1 참조).

   서버는 클라이언트가 선택한 스트림에서 STREAM FIN이 표시될 때까지
   쿼리 처리를 연기할 수 있다(MAY).

   서버와 클라이언트는 "매달려 있는(dangling)" 스트림의 수를
   모니터링할 수 있다(MAY). 이들은 구현에서 정의된 타임아웃 후에
   다음 이벤트가 발생하지 않은 열린 스트림이다:

   *  예상되는 쿼리 또는 응답이 수신되지 않은 경우, 또는

   *  예상되는 쿼리 또는 응답이 수신되었지만 STREAM FIN이 수신되지
      않은 경우

   구현은 그러한 매달려 있는 스트림의 수에 제한을 부과할 수 있다
   (MAY). 제한에 도달하면 구현은 연결을 닫을 수 있다(MAY).

4.2.1.  DNS Message ID

   QUIC 연결을 통해 쿼리를 보낼 때, DNS Message ID는 0으로
   설정되어야 한다(MUST). DoQ의 스트림 매핑은 쿼리와 응답의
   모호하지 않은 상관관계를 허용하므로 Message ID 필드는 필요하지
   않다.

   이것은 DoQ 메시지를 다른 전송으로 또는 다른 전송에서 프록싱하는
   데 영향을 미친다. 예를 들어, 프록시는 DoQ가 예를 들어 DNS over
   TCP보다 단일 연결에서 더 많은 수의 미완료 쿼리를 지원할 수
   있다는 사실을 관리해야 할 수 있다. 왜냐하면 DoQ는 Message ID
   공간에 의해 제한되지 않기 때문이다. 이 문제는 Message ID를 0으로
   설정하는 것이 권장되는 DoH에 이미 존재한다.

   DoQ에서 다른 전송을 통해 DNS 메시지를 전달할 때, 사용 중인
   프로토콜의 규칙에 따라 DNS Message ID가 생성되어야 한다(MUST).
   다른 전송에서 DoQ를 통해 DNS 메시지를 전달할 때, Message ID는
   0으로 설정되어야 한다(MUST).

4.3.  DoQ 오류 코드

   다음 오류 코드는 스트림을 급작스럽게 종료할 때, 스트림 읽기를
   중단할 때 애플리케이션 프로토콜 오류 코드로 사용하거나, 연결을
   즉시 닫을 때 사용하기 위해 정의된다:

   DOQ_NO_ERROR (0x0): 오류 없음. 이것은 연결이나 스트림을 닫아야
   하지만 알릴 오류가 없을 때 사용된다.

   DOQ_INTERNAL_ERROR (0x1): DoQ 구현이 내부 오류를 만났으며
   트랜잭션이나 연결을 계속할 수 없다.

   DOQ_PROTOCOL_ERROR (0x2): DoQ 구현이 프로토콜 오류를 만났으며
   연결을 강제로 중단한다.

   DOQ_REQUEST_CANCELLED (0x3): DoQ 클라이언트가 미완료 트랜잭션을
   취소하고자 할 때 이것을 사용한다.

   DOQ_EXCESSIVE_LOAD (0x4): DoQ 구현이 과도한 부하로 인해 연결을
   닫을 때 이것을 사용한다.

   DOQ_UNSPECIFIED_ERROR (0x5): DoQ 구현이 더 구체적인 오류 코드가
   없을 때 이것을 사용한다.

   DOQ_ERROR_RESERVED (0xd098ea5e): 테스트에 사용되는 대체 오류
   코드.

   새로운 오류 코드를 등록하는 것에 대한 세부사항은 Section 8.4를
   참조하라.

4.3.1.  트랜잭션 취소

   QUIC에서 STOP_SENDING을 보내면 피어가 스트림에서의 전송을 중단할
   것을 요청한다. DoQ 클라이언트가 미완료 요청을 취소하고자 하는
   경우, QUIC STOP_SENDING을 발행해야 하며(MUST), 오류 코드
   DOQ_REQUEST_CANCELLED를 사용해야 한다(SHOULD). Section 8.4에
   따라 등록된 더 구체적인 오류 코드를 사용할 수 있다(MAY).
   STOP_SENDING 요청은 언제든지 보낼 수 있지만 서버 응답이 이미
   전송된 경우에는 효과가 없으며, 이 경우 클라이언트는 수신되는
   응답을 단순히 폐기할 것이다. 해당 DNS 트랜잭션은 포기되어야
   한다(MUST).

   STOP_SENDING을 수신한 서버는 [RFC9000]의 Section 3.5에 따라
   행동한다. 서버는 STOP_SENDING을 수신한 경우 DNS 트랜잭션 처리를
   계속해서는 안 된다(SHOULD NOT).

   서버는 취소 요청의 총 수 또는 비율에 대한 구현 제한을 부과할 수
   있다(MAY). 제한에 도달하면 서버는 연결을 닫을 수 있다(MAY). 이
   경우 클라이언트 디버깅을 돕고자 하는 서버는 오류 코드
   DOQ_EXCESSIVE_LOAD를 사용할 수 있다(MAY). 선의의 클라이언트가
   문제를 디버깅하도록 돕는 것과 서비스 거부 공격자가 서버 방어를
   테스트하도록 허용하는 것 사이에는 항상 절충이 있다; 상황에 따라
   서버는 다른 오류 코드를 보내는 것을 선택할 수 있다.

   이 메커니즘은 보조 서버가 QUIC 연결을 닫지 않고도 주어진
   스트림에서 발생하는 단일 영역 전송을 취소하는 방법을 제공한다는
   점에 주목하라.

   서버는 클라이언트가 STREAM FIN을 표시하기 전에 클라이언트로부터
   RESET_STREAM 요청을 수신한 경우 DNS 트랜잭션 처리를
   계속해서는 안 된다(MUST NOT). 서버는 다음의 경우를 제외하고
   트랜잭션이 포기되었음을 표시하기 위해 RESET_STREAM을 발행해야
   한다(MUST):

   *  다른 이유로 이미 그렇게 한 경우, 또는

   *  이미 응답을 보내고 STREAM FIN을 표시한 경우.

4.3.2.  트랜잭션 오류

   서버는 일반적으로 DNS 응답이 DNS 오류를 나타내는 경우를 포함하여
   트랜잭션의 스트림에서 DNS 응답(또는 응답들)을 보냄으로써
   트랜잭션을 완료한다. 예를 들어, 클라이언트는 Response Code가
   SERVFAIL로 설정된 응답을 통해 Server Failure(SERVFAIL, [RFC1035])
   에 대해 알림을 받아야 한다(SHOULD).

   서버가 내부 오류로 인해 DNS 응답을 보낼 수 없는 경우, QUIC
   RESET_STREAM 프레임을 발행해야 한다(SHOULD). 오류 코드는
   DOQ_INTERNAL_ERROR로 설정되어야 한다(SHOULD). 해당 DNS
   트랜잭션은 포기되어야 한다(MUST). 클라이언트는 연결을 닫기로
   선택하기 전에 연결에서 수신되는 요청되지 않은 QUIC RESET_STREAM
   프레임의 수를 제한할 수 있다(MAY).

   이 메커니즘은 주 서버가 QUIC 연결을 닫지 않고도 주어진 스트림에서
   발생하는 단일 영역 전송을 중단하는 방법을 제공한다는 점에
   주목하라.

4.3.3.  프로토콜 오류

   트랜잭션 중에 잘못된 형식의, 불완전한, 또는 예상치 못한 메시지로
   인해 다른 오류 시나리오가 발생할 수 있다. 이에는 다음이 포함되지만
   이에 국한되지 않는다:

   *  클라이언트 또는 서버가 0이 아닌 Message ID를 가진 메시지를
      수신한 경우

   *  클라이언트 또는 서버가 2옥텟 길이 필드에 표시된 메시지의 모든
      바이트를 수신하기 전에 STREAM FIN을 수신한 경우

   *  클라이언트가 예상되는 모든 응답을 수신하기 전에 STREAM FIN을
      수신한 경우

   *  서버가 하나의 스트림에서 둘 이상의 쿼리를 수신한 경우

   *  클라이언트가 스트림에서 예상과 다른 수의 응답을 수신한 경우
      (예: A 레코드에 대한 쿼리에 대해 여러 응답)

   *  클라이언트가 STOP_SENDING 요청을 수신한 경우

   *  클라이언트 또는 서버가 요청 또는 응답을 보낸 후 예상되는
      STREAM FIN을 표시하지 않은 경우 (Section 4.2 참조)

   *  구현이 edns-tcp-keepalive EDNS(0) Option [RFC7828]을 포함하는
      메시지를 수신한 경우 (Section 5.5.2 참조)

   *  클라이언트 또는 서버가 단방향 QUIC 스트림을 열려고 시도한 경우

   *  서버가 서버 시작 양방향 QUIC 스트림을 열려고 시도한 경우

   *  서버가 0-RTT 데이터에서 "재생 가능한" 트랜잭션을 수신한 경우
      (이 경우를 처리하지 않으려는 서버의 경우, Section 4.5 참조)

   피어가 그러한 오류 조건을 만나면 치명적 오류로 간주된다. QUIC의
   CONNECTION_CLOSE 메커니즘을 사용하여 연결을 강제로 중단해야 하며
   (SHOULD), DoQ 오류 코드 DOQ_PROTOCOL_ERROR를 사용해야 한다
   (SHOULD). 일부 경우에는 대신 연결을 조용히 포기할 수 있으며
   (MAY), 이는 더 적은 로컬 리소스를 사용하지만 문제가 있는
   노드에서의 디버깅을 더 어렵게 만든다.

   위의 EDNS(0) 옵션 사용에 대한 제한은 TCP/DoT/DoH를 통한 메시지를
   DoQ를 통해 프록싱하는 데 영향을 미친다는 점에 주목한다.

4.3.4.  대체 오류 코드

   이 명세는 Section 4.3.1, 4.3.2, 4.3.3에서 구체적인 오류 코드를
   기술한다. 이러한 오류 코드는 실패 및 기타 사고의 조사를 용이하게
   하기 위한 것이다. 새로운 오류 코드는 DoQ의 향후 버전에서
   정의되거나 Section 8.4에 명시된 대로 등록될 수 있다.

   새로운 오류 코드는 협상 없이 정의될 수 있기 때문에, 예상치 못한
   컨텍스트에서 오류 코드를 사용하거나 알 수 없는 오류 코드를
   수신하는 것은 DOQ_UNSPECIFIED_ERROR와 동등하게 처리되어야 한다
   (MUST).

   구현은 이 문서에 나열되지 않은 오류 코드를 사용하여 오류 코드
   확장 메커니즘에 대한 지원을 테스트하고자 할 수 있으며(MAY),
   DOQ_ERROR_RESERVED를 사용할 수 있다(MAY).

4.4.  연결 관리

   QUIC 전송 명세인 [RFC9000]의 Section 10은 연결이 세 가지 방법으로
   닫힐 수 있음을 명시한다:

   *  유휴 타임아웃

   *  즉시 닫기

   *  상태 비저장 리셋

   DoQ를 구현하는 클라이언트와 서버는 유휴 타임아웃의 사용을
   협상해야 한다(SHOULD). 유휴 타임아웃에 의한 닫기는 패킷 교환
   없이 수행되며, 이는 프로토콜 오버헤드를 최소화한다. QUIC 전송
   명세인 [RFC9000]의 Section 10.1에 따라, 유휴 타임아웃의 유효
   값은 두 엔드포인트가 광고한 값의 최솟값으로 계산된다. 유휴
   타임아웃 설정에 대한 실제적 고려사항은 Section 5.5.2에서
   논의된다.

   클라이언트는 서버로부터 마지막 패킷이 수신된 이후 경과한 시간으로
   정의되는 서버와의 연결에서 발생한 유휴 시간을 모니터링해야 한다
   (SHOULD). 클라이언트가 서버에 새로운 DNS 쿼리를 보내려고 준비할
   때, 유휴 시간이 유휴 타이머보다 충분히 낮은지 확인해야 한다
   (SHOULD). 그렇다면, 클라이언트는 기존 연결을 통해 DNS 쿼리를
   보내야 한다(SHOULD). 그렇지 않다면, 클라이언트는 새로운 연결을
   설정하고 그 연결을 통해 쿼리를 보내야 한다(SHOULD).

   클라이언트는 유휴 타임아웃이 만료되기 전에 서버와의 연결을
   폐기할 수 있다(MAY). 미완료 쿼리가 있는 클라이언트는 QUIC의
   CONNECTION_CLOSE 메커니즘과 DoQ 오류 코드 DOQ_NO_ERROR를
   사용하여 명시적으로 연결을 닫아야 한다(SHOULD).

   클라이언트와 서버는 QUIC의 CONNECTION_CLOSE를 사용하여 표시되는
   다양한 다른 이유로 연결을 닫을 수 있다(MAY). 피어가 폐기한
   연결을 통해 패킷을 보내는 클라이언트와 서버는 상태 비저장 리셋
   표시를 받을 수 있다. 연결이 실패하면 해당 연결에서 진행 중인
   모든 트랜잭션은 포기되어야 한다(MUST).

4.5.  세션 재개 및 0-RTT

   서버가 지원하는 경우 클라이언트는 QUIC 전송 [RFC9000] 및 QUIC
   TLS [RFC9001]에 의해 지원되는 세션 재개 및 0-RTT 메커니즘을
   활용할 수 있다(MAY). 클라이언트는 이 메커니즘을 사용하기로
   결정하기 전에 세션 재개와 관련된 잠재적 프라이버시 문제를
   고려하고, 특히 이 문서의 다양한 섹션에 제시된 절충을 평가해야
   한다(SHOULD). 프라이버시 문제는 Section 7.1과 7.2에 자세히
   기술되어 있으며, 구현 고려사항은 Section 5.5.3에서 논의된다.

   0-RTT 메커니즘은 "재생 가능한" 트랜잭션이 아닌 DNS 요청을
   보내는 데 사용되어서는 안 된다(MUST NOT). 이 명세에서는
   OPCODE가 QUERY 또는 NOTIFY인 트랜잭션만 재생 가능한 것으로
   간주된다; 따라서 다른 OPCODE는 0-RTT 데이터로 전송되어서는
   안 된다(MUST NOT). NOTIFY가 여기에 포함된 이유에 대한 자세한
   논의는 부록 A를 참조하라.

   서버는 세션 재개를 지원할 수 있으며(MAY), [RFC9001]의
   Section 4.6.1에 기술된 메커니즘을 사용하여 0-RTT를 지원하거나
   지원하지 않으면서 그렇게 할 수 있다(MAY). 0-RTT를 지원하는
   서버는 0-RTT 데이터에서 수신된 재생 불가능한 트랜잭션을 즉시
   처리해서는 안 되며(MUST NOT), 대신 다음 동작 중 하나를
   채택해야 한다(MUST):

   *  문제가 되는 트랜잭션을 대기열에 넣고 [RFC9001]의
      Section 4.1.1에 정의된 대로 QUIC 핸드셰이크가 완료된 후에만
      처리한다.

   *  문제가 되는 트랜잭션에 [RFC6891]에 정의된 확장 RCODE
      메커니즘과 [RFC8914]에 정의된 확장 DNS 오류를 사용하여 응답
      코드 REFUSED와 Extended DNS Error Code(EDE) "Too Early"로
      응답한다; Section 8.3을 참조하라.

   *  오류 코드 DOQ_PROTOCOL_ERROR로 연결을 닫는다.

4.6.  메시지 크기

   DoQ 쿼리 및 응답은 이론적으로 최대 2^62 바이트를 운반할 수 있는
   QUIC 스트림에서 전송된다. 그러나 DNS 메시지는 실제로 최대 65535
   바이트로 제한된다. 이 최대 크기는 DNS over TCP [RFC1035] 및 DoT
   [RFC7858]에서 2옥텟 메시지 길이 필드의 사용에 의해, 그리고 DoH
   [RFC8484]를 위한 "application/dns-message"의 정의에 의해
   적용된다. DoQ는 동일한 제한을 적용한다.

   Extension Mechanisms for DNS(EDNS(0)) [RFC6891]은 피어가 UDP
   메시지 크기를 지정할 수 있도록 한다. 이 매개변수는 DoQ에 의해
   무시된다. DoQ 구현은 항상 최대 메시지 크기가 65535 바이트라고
   가정한다.

5.  구현 요구사항

5.1.  인증

   stub에서 recursive 시나리오의 경우, 인증 요구사항은 DoT
   [RFC7858] 및 "Usage Profiles for DNS over TLS and DNS over DTLS"
   [RFC8310]에 기술된 것과 동일하다. [RFC8932]는 DNS 프라이버시
   서비스가 클라이언트가 서버를 인증하는 데 사용할 수 있는 자격
   증명을 제공해야 한다(SHOULD)고 명시한다. 이를 감안하여, DoH의
   인증 모델과 일치시키기 위해, DoQ stub은 Strict 사용 프로필을
   사용해야 한다(SHOULD). 암호화된 stub에서 recursive 시나리오에
   대한 클라이언트 인증은 어떤 DNS RFC에도 기술되어 있지 않다.

   영역 전송의 경우, 인증 요구사항은 [RFC9103]에 기술된 것과
   동일하다.

   recursive에서 authoritative 시나리오의 경우, 인증 요구사항은
   작성 시점에 명시되지 않았으며 DPRIVE WG에서 진행 중인 작업의
   주제이다.

5.2.  연결 실패 시 다른 프로토콜로의 폴백

   DoQ 연결의 설정이 실패하면, 클라이언트는 DoT [RFC7858] 및
   "Usage Profiles for DNS over TLS and DNS over DTLS" [RFC8310]에
   명시된 대로, 사용 프로필에 따라 DoT로 폴백하고 잠재적으로
   평문으로 폴백하려고 시도할 수 있다(MAY).

   DNS 클라이언트는 DoQ를 지원하지 않는 서버 IP 주소를 기억해야
   한다(SHOULD). 모바일 클라이언트는 또한 주어진 IP 주소에 의한
   DoQ 지원 부재를 컨텍스트별로(예: 네트워크 또는 프로비저닝
   도메인별로) 기억할 수 있다.

   타임아웃, 연결 거부, 그리고 QUIC 핸드셰이크 실패는 서버가 DoQ를
   지원하지 않음을 나타내는 지표이다. 클라이언트는 합리적인 기간
   (예: 서버당 1시간) 동안 DoQ를 지원하지 않는 서버에 DoQ 쿼리를
   시도해서는 안 된다(SHOULD NOT). 대역 외 키 고정 사용 프로필
   [RFC7858]을 따르는 DNS 클라이언트는 DoQ 연결 실패 후 재시도에
   대해 더 공격적일 수 있다(MAY).

5.3.  주소 검증

   QUIC 전송 명세인 [RFC9000]의 Section 8은 서버가 주소 증폭
   공격에 사용되는 것을 방지하기 위한 주소 검증 절차를 정의한다.
   DoQ 구현은 이 명세를 준수해야 하며(MUST), 이는 최악의 경우
   증폭을 3배로 제한한다.

   DoQ 구현은 QUIC 전송 명세인 [RFC9000]의 Section 8.1.2에 정의된
   Retry Packets를 사용한 주소 검증 절차를 사용하도록 서버를
   구성하는 것을 고려해야 한다(SHOULD). 이 절차는 DNS Cookies
   메커니즘 [RFC7873]과 유사하게 클라이언트의 출발지 주소의 반환
   경로 확인을 위해 1-RTT 지연을 부과한다.

   Retry Packets를 사용한 주소 검증을 구성하는 DoQ 구현은 QUIC
   전송 명세인 [RFC9000]의 Section 8.1.3에 정의된 향후 연결을 위한
   주소 검증 절차를 구현해야 한다(SHOULD). 이것은 동일한 주소에서의
   클라이언트에 의한 후속 연결 중 1-RTT 패널티를 피하기 위해
   클라이언트 주소가 검증된 후 서버가 클라이언트에게 NEW_TOKEN
   프레임을 보낼 수 있는 방법을 정의한다.

5.4.  패딩

   구현은 패딩의 신중한 삽입을 통해 Section 7.5에 기술된 트래픽
   분석 공격으로부터 보호해야 한다(MUST). 이것은 EDNS(0) Padding
   Option [RFC7830]을 사용하여 개별 DNS 메시지를 패딩하거나
   QUIC 패킷을 패딩([RFC9000]의 Section 19.1 참조)하여 수행될 수
   있다.

   이론적으로, QUIC 패킷 수준에서의 패딩은 동등한 보호에 대해 더
   나은 성능을 가져올 수 있다. 왜냐하면 패딩의 양이 확인 응답이나
   흐름 제어 업데이트와 같은 비DNS 프레임을 고려할 수 있고, 또한
   QUIC 패킷이 여러 DNS 메시지를 운반할 수 있기 때문이다. 그러나
   애플리케이션은 QUIC의 구현이 적절한 API를 노출하는 경우에만
   QUIC 패킷의 패딩 양을 제어할 수 있다. 이는 다음과 같은 권고로
   이어진다:

   *  QUIC의 구현이 패딩 정책을 설정하는 API를 노출하는 경우,
      DoQ는 패킷 길이를 소수의 고정 크기 집합에 맞추기 위해
      해당 API를 사용해야 한다(SHOULD).

   *  QUIC 패킷 수준에서의 패딩이 불가능하거나 사용되지 않는 경우,
      DoQ는 [RFC7830]에 명시된 EDNS(0) padding 확장을 사용하여
      모든 DNS 쿼리 및 응답이 소수의 고정 크기 집합으로
      패딩되도록 보장해야 한다(MUST).

   구현은 다른 암호화된 전송에 적용되는 기존 DNS 메시지 패딩 로직을
   재사용하는 것이 상당히 더 간단한 경우 패딩을 위한 QUIC API를
   사용하지 않기로 선택할 수 있다.

   패딩 크기에 대한 표준 정책이 없는 경우, 구현은 Experimental
   상태의 "Padding Policies for Extension Mechanisms for DNS
   (EDNS(0))" [RFC8467]의 권고를 따라야 한다(SHOULD). Experimental
   이지만, 이러한 권고는 DoT에 대해 구현되고 배포되어 있으며 구현이
   이 명세를 완전히 준수하는 방법을 제공하기 때문에 참조된다.

5.5.  연결 처리

   "DNS Transport over TCP - Implementation Requirements" [RFC7766]은
   DNS over TCP에 대한 업데이트된 지침을 제공하며, 그 중 일부는
   DoQ에 적용 가능하다. 이 섹션은 DoQ에 대한 연결 처리에 대해
   유사한 조언을 제공한다.

5.5.1.  연결 재사용

   DNS 클라이언트의 역사적 구현은 각 DNS 쿼리마다 TCP 연결을 열고
   닫는 것으로 알려져 있다. 연결 설정 비용을 분산시키기 위해,
   클라이언트와 서버 모두 단일 지속 QUIC 연결을 통해 여러 쿼리와
   응답을 보냄으로써 연결 재사용을 지원해야 한다(SHOULD).

   UDP와 동등한 성능을 달성하기 위해, DNS 클라이언트는 QUIC 연결의
   QUIC 스트림을 통해 쿼리를 동시에 보내야 한다(SHOULD). 즉, DNS
   클라이언트가 QUIC 연결을 통해 서버에 여러 쿼리를 보낼 때,
   다음 쿼리를 보내기 전에 미완료 응답을 기다려서는 안 된다
   (SHOULD NOT).

5.5.2.  리소스 관리

   설정된 연결과 유휴 연결의 적절한 관리는 DNS 서버의 건전한 운영에
   중요하다.

   DoQ의 구현은 DNS over TCP [RFC7766]에 명시된 것과 유사한 모범
   사례를 따라야 하며(SHOULD), 특히 다음과 관련하여:

   *  동시 연결 ([RFC7766]의 Section 6.2.2, [RFC9103]의
      Section 6.4에 의해 업데이트됨)

   *  보안 고려사항 ([RFC7766]의 Section 10)

   그렇게 하지 않으면 리소스 고갈과 서비스 거부로 이어질 수 있다.

   장기 DoQ 연결을 유지하고자 하는 클라이언트는 QUIC 전송 명세인
   [RFC9000]의 Section 10.1에 정의된 유휴 타임아웃 메커니즘을
   사용해야 한다(SHOULD). 클라이언트와 서버는 DoQ 연결에서 전송되는
   어떤 메시지에서도 edns-tcp-keepalive EDNS(0) Option [RFC7828]을
   보내서는 안 된다(MUST NOT) (이것은 전송으로서 TCP/TLS의 사용에
   특화되어 있기 때문이다).

   이 문서는 유휴 연결에 대한 타임아웃 값에 대해 구체적인 권고를
   하지 않는다. 클라이언트와 서버는 사용 가능한 리소스의 수준에
   따라 연결을 재사용하거나 닫아야 한다. 타임아웃은 활동이 적은
   기간에는 더 길고 활동이 많은 기간에는 더 짧을 수 있다.

5.5.3.  0-RTT 및 세션 재개 사용

   DoQ에 0-RTT를 사용하는 것은 많은 매력적인 이점이 있다.
   클라이언트는 연결 지연 없이 연결을 설정하고 쿼리를 보낼 수
   있다. 따라서 서버는 연결 타이머의 낮은 값을 협상할 수 있으며,
   이는 관리해야 하는 총 연결 수를 줄인다. 0-RTT를 사용하는
   클라이언트는 쿼리에 새 연결이 필요한 경우 지연 패널티를 받지
   않기 때문에 서버가 그렇게 할 수 있다.

   세션 재개 및 0-RTT 데이터 전송은 Section 7.1과 7.2에 자세히
   기술된 프라이버시 위험을 생성한다. 다음 권고는 Section 4.5에
   명시된 제한에 따라 0-RTT 데이터의 성능 이점을 누리면서
   프라이버시 위험을 줄이기 위한 것이다.

   클라이언트는 [RFC8446]의 Appendix C.4에 명시된 대로 재개 티켓을
   한 번만 사용해야 한다(SHOULD). 기본적으로, 클라이언트의 연결
   상태가 변경된 경우 클라이언트는 세션 재개를 사용해서는 안 된다
   (SHOULD NOT).

   클라이언트는 NEW_TOKEN 메커니즘을 사용하여 서버로부터 주소 검증
   토큰을 받을 수 있다; [RFC9000]의 Section 8을 참조하라. 관련된
   추적 위험은 Section 7.3에 언급되어 있다. 클라이언트는 추가적인
   추적 위험을 피하기 위해 세션 재개도 사용할 때만 주소 검증 토큰을
   사용해야 한다(SHOULD).

   서버는 클라이언트가 연결을 살려두거나 세션 재개 티켓을 갱신하기
   위해 서버를 자주 폴링하려는 유혹을 받지 않도록 충분히 긴 수명
   (예: 6시간)의 세션 재개 티켓을 발행해야 한다(SHOULD). 서버는
   [RFC8446]의 Section 8에 명시된 안티 리플레이 메커니즘을 구현해야
   한다(SHOULD).

5.5.4.  프라이버시를 위한 연결 마이그레이션 제어

   DoQ 구현은 [RFC9000]의 Section 9에 정의된 연결 마이그레이션
   기능의 사용을 고려할 수 있다. 이러한 기능은 클라이언트의 연결
   상태가 변경되어도 연결이 계속 작동할 수 있도록 한다. Section 7.4
   에 자세히 기술된 바와 같이, 이러한 기능은 프라이버시를 지연과
   교환한다. 기본적으로, 클라이언트는 연결 상태가 변경되면
   프라이버시를 우선시하고 새 세션을 시작하도록 구성되어야 한다
   (SHOULD).

5.6.  쿼리 병렬 처리

   "DNS Transport over TCP - Implementation Requirements" [RFC7766]의
   Section 7에 명시된 바와 같이, resolver는 응답의 준비를 병렬로
   지원하고 순서와 관계없이 보내는 것이 권장된다(RECOMMENDED). DoQ
   에서는 이전에 열린 스트림에 대한 응답의 가용성을 기다리지 않고
   가능한 한 빨리 해당 특정 스트림에서 응답을 보냄으로써 이를
   수행한다.

5.7.  영역 전송

   [RFC9103]은 TLS를 통한 영역 전송(XoT)을 명시하며 [RFC1995]
   (IXFR), [RFC5936](AXFR), 그리고 [RFC7766]에 대한 업데이트를
   포함한다. 거기서 기술된 XoT 연결의 재사용에 관한 고려사항은
   DoQ 연결을 사용하여 수행되는 영역 전송에 유사하게 적용된다.
   그러한 구체적인 지침을 재진술하는 한 가지 이유는 오늘날 기존
   TCP/TLS 영역 전송 구현에서 효과적인 연결 재사용이 부족하기
   때문이다. 다음 권고가 적용된다:

   *  DoQ 서버는 단일 QUIC 연결에서 여러 동시 IXFR 요청을 처리할
      수 있어야 한다(MUST).

   *  DoQ 서버는 단일 QUIC 연결에서 여러 동시 AXFR 요청을 처리할
      수 있어야 한다(MUST).

   *  DoQ 구현은 다음을 수행해야 한다(SHOULD):

      -  동일한 주 서버에 대한 AXFR 및 IXFR 요청 모두에 동일한
         QUIC 연결을 사용한다

      -  대기열에 들어오는 대로 해당 요청을 병렬로 보낸다, 즉
         연결에서 다음 쿼리를 보내기 전에 응답을 기다리지 않는다
         (이것은 TCP/TLS 연결에서 요청을 파이프라이닝하는 것과
         유사하다)

      -  각 요청에 대한 응답(들)을 사용 가능해지는 대로 보낸다,
         즉 응답 스트림이 서로 섞여서 전송될 수 있다(MAY)

5.8.  흐름 제어 메커니즘

   서버와 클라이언트는 [RFC9000]의 Section 4에 정의된 메커니즘을
   사용하여 흐름 제어를 관리한다. 이러한 메커니즘은 클라이언트와
   서버가 생성할 수 있는 스트림의 수, 스트림에서 보낼 수 있는
   데이터의 양, 그리고 모든 스트림의 합집합에서 보낼 수 있는
   데이터의 양을 지정할 수 있도록 한다. DoQ의 경우, 생성되는
   스트림의 수를 제어하면 서버가 클라이언트가 주어진 연결에서 보낼
   수 있는 새로운 요청의 수를 제어할 수 있다.

   흐름 제어는 엔드포인트 리소스를 보호하기 위해 존재한다. 서버의
   경우, 전역 및 스트림별 흐름 제어 한계는 클라이언트가 보낼 수
   있는 데이터의 양을 제어한다. 동일한 메커니즘은 클라이언트가
   서버가 보낼 수 있는 데이터의 양을 제어할 수 있도록 한다. 너무
   작은 값은 불필요하게 성능을 제한할 것이다. 너무 큰 값은
   엔드포인트를 과부하나 메모리 고갈에 노출시킬 수 있다. 구현 또는
   배포는 이러한 우려의 균형을 맞추기 위해 흐름 제어 한계를 조정해야
   할 것이다. 특히 영역 전송 구현은 대규모 및 동시 영역 전송이 잘
   관리되도록 이러한 한계를 주의 깊게 제어해야 할 것이다.

   매개변수의 초기 값은 연결 시작 시 클라이언트와 서버가 보낼 수
   있는 요청의 수와 데이터의 양을 제어한다. 이러한 값은 연결
   핸드셰이크 중에 교환되는 전송 매개변수에 명시된다. 초기 연결에서
   수신된 매개변수 값은 또한 재개된 연결에서 클라이언트가 0-RTT
   데이터를 사용하여 보낼 수 있는 요청의 수와 데이터의 양을
   제어한다. 이러한 초기 매개변수의 너무 작은 값을 사용하면 0-RTT
   데이터 허용의 유용성이 제한될 것이다.

6.  보안 고려사항

   Domain Name System의 위협 분석은 [RFC3833]에 있다. 이 분석은
   DoT, DoH, DoQ의 개발 이전에 작성되었으며 아마도 업데이트가
   필요할 것이다.

   DoQ의 보안 고려사항은 DoT [RFC7858]의 것과 비교할 만해야 한다.
   [RFC7858]에 명시된 DoT는 stub에서 recursive 시나리오만 다루지만,
   중간자 공격, 중간 장치, 그리고 평문 연결의 데이터 캐싱에 대한
   고려사항은 resolver에서 authoritative server 시나리오에서도 DoQ에
   적용된다. Section 5.1에 명시된 바와 같이, DoQ를 사용한 영역
   전송 보안을 위한 인증 요구사항은 DoT를 통한 영역 전송에 대한
   것과 동일하다; 따라서 일반적인 보안 고려사항은 [RFC9103]에
   기술된 것과 전적으로 유사하다.

   DoQ는 그 자체가 TLS 1.3에 의존하는 QUIC에 의존하며, 따라서
   [BCP195]에 기술된 다운그레이드 공격에 대한 보호를 기본적으로
   지원한다. QUIC 특유의 문제와 그 완화는 [RFC9000]의 Section 21에
   기술되어 있다.

7.  프라이버시 고려사항

   "DNS Privacy Considerations" [RFC9076]에 제공된 암호화된 전송에
   대한 일반적 고려사항은 DoQ에 적용된다. 거기서 제공된 구체적
   고려사항은 DoT와 DoQ 사이에 차이가 없으며, 여기서 더 이상
   논의되지 않는다. 마찬가지로, "Recommendations for DNS Privacy
   Service Operators" [RFC8932] (DNS 프라이버시 서비스에 대한 운영,
   정책, 그리고 보안 고려사항을 다루는)도 DoQ 서비스에 적용 가능하다.

   QUIC은 TLS 1.3 [RFC8446]의 메커니즘을 통합하며, 이는 QUIC의
   "0-RTT" 데이터 전송을 가능하게 한다. 이는 흥미로운 지연 이득을
   제공할 수 있지만, 두 가지 우려를 제기한다:

   1.  공격자가 0-RTT 데이터를 재생하고 수신 서버의 동작에서 그
       내용을 추론할 수 있다.

   2.  0-RTT 메커니즘은 연속적인 클라이언트 세션 간의 연결 가능성을
       제공할 수 있는 TLS 세션 재개에 의존한다.

   이러한 문제는 Section 7.1과 7.2에서 전개된다.

7.1.  0-RTT 데이터의 프라이버시 문제

   0-RTT 데이터는 공격자에 의해 재생될 수 있다. 해당 데이터는
   recursive resolver에 의한 authoritative resolver로의 쿼리를
   트리거할 수 있다. 공격자는 recursive resolver의 발신 트래픽이
   관찰 가능한 시간을 선택하여 0-RTT 데이터에서 어떤 이름이
   쿼리되었는지 알아낼 수 있다.

   이 위험은 사실 "DNS Privacy Considerations" [RFC9076]에서 논의된
   recursive resolver의 동작을 관찰하는 일반적 문제의 부분집합이다.
   공격은 이 트래픽의 관찰 가능성을 줄임으로써 부분적으로
   완화된다. TLS 1.3 [RFC8446]의 필수적 리플레이 보호 메커니즘은
   리플레이의 위험을 제한하지만 제거하지는 않는다. 0-RTT 패킷은
   클럭 스큐와 네트워크 전송의 변동을 고려할 만큼 충분히 넓은 좁은
   창 내에서만 재생될 수 있다.

   TLS 1.3 [RFC8446]에 대한 권고는 0-RTT 데이터를 사용하는 기능이
   기본적으로 꺼져 있어야 하고 사용자가 관련된 위험을 명확히 이해하는
   경우에만 활성화되어야 한다는 것이다. DoQ의 경우, 0-RTT 데이터를
   허용하는 것은 상당한 성능 향상을 제공하며, 이를 사용하지 말라는
   권고는 단순히 무시될 것이라는 우려가 있다. 대신, Section 4.5와
   5.5.3에서 일련의 실용적 권고가 제공된다.

   Section 4.5의 명세는 서버의 장기 상태를 변경하지 않을 트랜잭션만
   허용함으로써 리플레이 공격의 가장 명백한 위험을 차단한다.

   위에서 기술된 공격은 stub resolver에서 recursive resolver
   시나리오에 적용되지만, recursive resolver에서 authoritative
   resolver 시나리오에서도 유사한 공격이 구상될 수 있으며, 동일한
   완화가 적용된다.

7.2.  세션 재개의 프라이버시 문제

   QUIC 세션 재개 메커니즘은 세션 재설정 비용을 줄이고 0-RTT
   데이터를 가능하게 한다. 동일한 재개 토큰이 여러 번 사용되는 경우
   세션 재개와 관련된 연결 가능성 문제가 있다. 클라이언트와 서버
   사이의 경로에 있는 공격자는 토큰의 반복적 사용을 관찰하고 이를
   사용하여 시간에 걸쳐 또는 여러 위치에 걸쳐 클라이언트를 추적할
   수 있다.

   세션 재개 메커니즘은 서버가 재개된 세션을 초기 세션과 상관시킬
   수 있도록 하여 클라이언트를 추적할 수 있도록 한다. 이것은 가상의
   장기 세션을 생성한다. 해당 세션의 일련의 쿼리는 서버가
   클라이언트를 식별하는 데 사용될 수 있다. 클라이언트 주소가
   일정하게 유지되는 경우 서버는 이미 그렇게 할 수 있지만, 세션
   재개 티켓은 클라이언트의 주소 변경 후에도 추적을 가능하게 한다.

   Section 5.5.3의 권고는 이러한 위험을 완화하도록 설계되었다. 세션
   티켓을 한 번만 사용하면 제3자에 의한 추적 위험이 완화된다. 주소가
   변경되면 세션 재개를 거부하면 서버에 의한 추적의 증분적 위험이
   완화된다 (그러나 IP 주소에 의한 추적 위험은 남아 있다).

   여기서의 프라이버시 절충은 컨텍스트에 따라 다를 수 있다. Stub
   resolver는 종종 위치를 변경하기 때문에 지연보다 프라이버시를
   선호하는 강한 동기를 가질 것이다. 그러나 소수의 정적 IP 주소
   집합을 사용하는 recursive resolver는 세션 재개가 제공하는 감소된
   지연을 선호할 가능성이 더 높으며, 세션 간에 IP 주소가 변경되더라도
   재개 티켓을 사용하는 유효한 이유로 이를 고려할 수 있다.

   암호화된 영역 전송([RFC9103])은 전송에 관련된 당사자의 신원을
   숨기려고 명시적으로 시도하지 않는다; 동시에, 그러한 전송은
   특별히 지연에 민감하지 않다. 이는 영역 전송을 지원하는
   애플리케이션이 stub에서 recursive 애플리케이션과 동일한 보호를
   적용하기로 결정할 수 있음을 의미한다.

7.3.  주소 검증 토큰의 프라이버시 문제

   QUIC은 [RFC9000]의 Section 8에서 주소 검증 메커니즘을 명시한다.
   주소 검증 토큰의 사용은 QUIC 서버가 새 연결에 대해 추가 RTT를
   피할 수 있도록 한다. 주소 검증 토큰은 일반적으로 IP 주소에
   연결되어 있다. QUIC 클라이언트는 일반적으로 이전에 사용한
   주소에서 새 연결을 설정할 때만 이러한 토큰을 사용한다. 그러나
   클라이언트가 새 주소를 사용하고 있다는 것을 항상 인식하는 것은
   아니다. 이것은 NAT 때문이거나, IP 주소가 변경되었는지 확인하는
   데 사용할 수 있는 API가 클라이언트에 없기 때문일 수 있다 (IPv6의
   경우 매우 빈번할 수 있다). 클라이언트가 모르는 사이에 새 위치로
   이동한 후 주소 검증 토큰을 잘못 사용하면 연결 가능성 위험이 있다.

   Section 5.5.3의 권고는 NEW_TOKEN의 사용을 세션 재개의 사용에
   연결함으로써 이 위험을 완화한다. 다만 이 권고는 클라이언트가
   주소 변경을 인식하지 못하는 경우를 다루지 않는다.

7.4.  장기 세션의 프라이버시 문제

   세션 재개의 잠재적 대안은 장기 세션의 사용이다: 세션이 오랜
   시간 동안 열려 있으면 연결 설정 지연 없이 새 쿼리를 보낼 수
   있다. 두 솔루션이 유사한 프라이버시 특성을 가진다는 점을 지적할
   가치가 있다. 세션 재개는 서버가 클라이언트의 IP 주소를 추적할
   수 있도록 할 수 있지만, 장기 세션도 동일한 효과를 가진다.

   특히, DoQ 구현은 클라이언트의 연결 상태가 변경되어도 세션을
   유지하기 위해 QUIC의 연결 마이그레이션 기능을 활용할 수 있다.
   예를 들어 클라이언트가 Wi-Fi 연결에서 셀룰러 네트워크 연결로,
   그리고 다른 Wi-Fi 연결로 마이그레이션하는 경우이다. 서버는
   장기 연결이 사용하는 IP 주소의 변천을 모니터링하여 클라이언트
   위치를 추적할 수 있을 것이다.

   Section 5.5.4의 권고는 여러 클라이언트 주소를 사용하는 장기
   세션과 관련된 프라이버시 우려를 완화한다.

7.5.  트래픽 분석

   QUIC 패킷이 암호화되어 있더라도, 공격자는 쿼리와 응답 모두에서
   패킷 길이를 관찰하고, 패킷 타이밍을 관찰함으로써 정보를 얻을 수
   있다. 많은 DNS 요청은 웹 브라우저에 의해 발생한다. 특정 웹
   페이지를 로드하려면 수십 개의 DNS 이름을 해석해야 할 수 있다.
   애플리케이션이 패킷당 하나의 쿼리 또는 응답, 또는 "패킷당 하나의
   QUIC STREAM 프레임"의 단순한 매핑을 채택하면, 패킷 길이의 연속은
   요청된 사이트를 식별하기에 충분한 정보를 제공할 수 있다.

   구현은 이 공격을 완화하기 위해 Section 5.4에 정의된 메커니즘을
   사용해야 한다(SHOULD).

8.  IANA 고려사항

8.1.  DoQ 식별 문자열 등록

   이 문서는 "TLS Application-Layer Protocol Negotiation (ALPN)
   Protocol IDs" 레지스트리 [RFC7301]에서 DoQ의 식별을 위한 새로운
   등록을 생성한다.

   "doq" 문자열은 DoQ를 식별한다:

   Protocol:  DoQ

   Identification Sequence:  0x64 0x6F 0x71 ("doq")

   Specification:  이 문서

8.2.  전용 포트 예약

   TCP와 UDP 모두에서 포트 853은 현재 "DNS query-response protocol
   run over TLS/DTLS" [RFC7858]를 위해 예약되어 있다.

   그러나 DNS over DTLS(DoD) [RFC8094]의 명세는 실험적이고, stub에서
   resolver로 제한되며, 저자들이 아는 한 현재 어떤 구현이나 배포도
   존재하지 않는다 (명세가 발행된 후 몇 년이 지났음에도 불구하고).

   이 명세는 추가적으로 DoQ를 위한 UDP 포트 853의 사용을 예약한다.
   QUIC 버전 1은 DTLS를 포함하여 동일한 포트에서 다른 프로토콜과
   공존할 수 있도록 설계되었다; [RFC9000]의 Section 17.2를
   참조하라. 이는 동일한 포트에서 DoD와 DoQ(QUIC 버전 1)를 제공하는
   배포가 각 UDP 페이로드의 두 번째로 가장 유의미한 비트로 인해 둘을
   역다중화할 수 있음을 의미한다. 그러한 배포는 동일한 포트에서
   DNS를 제공하기 위해 배포하기 전에 QUIC 및 DTLS의 향후 버전 또는
   확장(예: [GREASING-QUIC])의 서명을 확인해야 한다.

   IANA는 System 범위의 "Service Name and Transport Protocol Port
   Number Registry"에서 다음 값을 업데이트했다. 해당 범위의
   레지스트리는 IETF Review 또는 IESG Approval [RFC6335]을
   요구한다.

   Service Name:  domain-s

   Port Number:  853

   Transport Protocol(s):  UDP

   Assignee:  IESG

   Contact:  IETF Chair

   Description:  DNS query-response protocol run over DTLS or QUIC

   Reference:  [RFC7858][RFC8094] 이 문서

   추가적으로, IANA는 일관성과 명확성을 위해 해당 TCP 포트 853
   할당의 Description 필드를 "DNS query-response protocol run over
   TLS"로 업데이트하고 TCP 할당의 Reference 필드에서 [RFC8094]를
   제거했다.

8.3.  Extended DNS Error Code: Too Early 예약

   IANA는 "Extended DNS Error Codes" 레지스트리 [RFC8914]에 다음
   값을 등록했다:

   INFO-CODE:  26

   Purpose:  Too Early

   Reference:  이 문서

8.4.  DNS-over-QUIC Error Codes 레지스트리

   IANA는 "Domain Name System (DNS) Parameters" 웹 페이지에
   "DNS-over-QUIC Error Codes"에 대한 레지스트리를 추가했다.

   "DNS-over-QUIC Error Codes" 레지스트리는 62비트 공간을 관리한다.
   이 공간은 서로 다른 정책에 의해 관리되는 세 영역으로 분할된다:

   *  0x00에서 0x3f 사이의 값(16진수; 포함)에 대한 영구 등록,
      [RFC8126]의 Section 4.9 및 4.10에 정의된 Standards Action 또는
      IESG Approval을 사용하여 할당된다

   *  0x3f보다 큰 값에 대한 영구 등록, Specification Required 정책
      ([RFC8126])을 사용하여 할당된다

   *  0x3f보다 큰 값에 대한 임시 등록, [RFC8126]의 Section 4.5에
      정의된 Expert Review를 요구한다.

   임시 예약은 일부 영구 등록과 0x3f보다 큰 값의 범위를 공유한다.
   이것은 배포된 시스템의 변경을 요구하지 않고 임시 등록을 영구
   등록으로 전환할 수 있도록 의도적으로 설계된 것이다. (이 설계는
   [RFC9000]의 Section 22에 설정된 원칙에 부합한다.)

   이 레지스트리의 등록은 다음 필드를 포함해야 한다(MUST):

   Value:  할당된 코드포인트

   Status:  "Permanent" 또는 "Provisional"

   Contact:  등록자에 대한 연락처 정보

   또한, 영구 등록은 다음을 포함해야 한다(MUST):

   Error:  매개변수에 대한 짧은 니모닉

   Specification:  해당 값에 대한 공개적으로 이용 가능한 명세에 대한
   참조 (임시 등록의 경우 선택사항)

   Description:  오류 코드 의미론에 대한 간략한 설명, 명세 참조가
   제공되는 경우 요약일 수 있다(MAY)

   코드포인트의 임시 등록은 DoQ에 대한 확장의 개인적 사용 및
   실험을 허용하기 위한 것이다. 그러나 임시 등록은 다른 목적을 위해
   회수되고 재할당될 수 있다. 위에 나열된 매개변수 외에도, 임시
   등록은 다음을 포함해야 한다(MUST):

   Date:  등록의 마지막 업데이트 날짜

   임시 등록의 날짜를 업데이트하는 요청은 지정된 전문가(들)의
   검토 없이 이루어질 수 있다.

   이 레지스트리의 초기 내용은 Table 1에 표시되며 모든 항목은 다음
   필드를 공유한다:

   Status:  Permanent

   Contact:  DPRIVE WG

   Specification:  Section 4.3

+============+=======================+=============================+
| Value      | Error                 | Description                 |
+============+=======================+=============================+
| 0x0        | DOQ_NO_ERROR          | No error                    |
+------------+-----------------------+-----------------------------+
| 0x1        | DOQ_INTERNAL_ERROR    | Implementation error        |
+------------+-----------------------+-----------------------------+
| 0x2        | DOQ_PROTOCOL_ERROR    | Generic protocol violation  |
+------------+-----------------------+-----------------------------+
| 0x3        | DOQ_REQUEST_CANCELLED | Request cancelled by client |
+------------+-----------------------+-----------------------------+
| 0x4        | DOQ_EXCESSIVE_LOAD    | Closing a connection for    |
|            |                       | excessive load              |
+------------+-----------------------+-----------------------------+
| 0x5        | DOQ_UNSPECIFIED_ERROR | No error reason specified   |
+------------+-----------------------+-----------------------------+
| 0xd098ea5e | DOQ_ERROR_RESERVED    | Alternative error code used |
|            |                       | for tests                   |
+------------+-----------------------+-----------------------------+

            Table 1: 초기 DNS-over-QUIC Error Codes 항목

9.  참고문헌

9.1.  규범적 참고문헌

   [RFC1034]  Mockapetris, P., "Domain names - concepts and
              facilities", STD 13, RFC 1034, DOI 10.17487/RFC1034,
              November 1987,
              <https://www.rfc-editor.org/info/rfc1034>.

   [RFC1035]  Mockapetris, P., "Domain names - implementation and
              specification", STD 13, RFC 1035,
              DOI 10.17487/RFC1035, November 1987,
              <https://www.rfc-editor.org/info/rfc1035>.

   [RFC1995]  Ohta, M., "Incremental Zone Transfer in DNS", RFC 1995,
              DOI 10.17487/RFC1995, August 1996,
              <https://www.rfc-editor.org/info/rfc1995>.

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119,
              DOI 10.17487/RFC2119, March 1997,
              <https://www.rfc-editor.org/info/rfc2119>.

   [RFC5936]  Lewis, E. and A. Hoenes, Ed., "DNS Zone Transfer
              Protocol (AXFR)", RFC 5936, DOI 10.17487/RFC5936,
              June 2010,
              <https://www.rfc-editor.org/info/rfc5936>.

   [RFC6891]  Damas, J., Graff, M., and P. Vixie, "Extension
              Mechanisms for DNS (EDNS(0))", STD 75, RFC 6891,
              DOI 10.17487/RFC6891, April 2013,
              <https://www.rfc-editor.org/info/rfc6891>.

   [RFC7301]  Friedl, S., Popov, A., Langley, A., and E. Stephan,
              "Transport Layer Security (TLS) Application-Layer
              Protocol Negotiation Extension", RFC 7301,
              DOI 10.17487/RFC7301, July 2014,
              <https://www.rfc-editor.org/info/rfc7301>.

   [RFC7766]  Dickinson, J., Dickinson, S., Bellis, R., Mankin, A.,
              and D. Wessels, "DNS Transport over TCP -
              Implementation Requirements", RFC 7766,
              DOI 10.17487/RFC7766, March 2016,
              <https://www.rfc-editor.org/info/rfc7766>.

   [RFC7830]  Mayrhofer, A., "The EDNS(0) Padding Option", RFC 7830,
              DOI 10.17487/RFC7830, May 2016,
              <https://www.rfc-editor.org/info/rfc7830>.

   [RFC7858]  Hu, Z., Zhu, L., Heidemann, J., Mankin, A., Wessels,
              D., and P. Hoffman, "Specification for DNS over
              Transport Layer Security (TLS)", RFC 7858,
              DOI 10.17487/RFC7858, May 2016,
              <https://www.rfc-editor.org/info/rfc7858>.

   [RFC8126]  Cotton, M., Leiba, B., and T. Narten, "Guidelines for
              Writing an IANA Considerations Section in RFCs", BCP 26,
              RFC 8126, DOI 10.17487/RFC8126, June 2017,
              <https://www.rfc-editor.org/info/rfc8126>.

   [RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
              2119 Key Words", BCP 14, RFC 8174,
              DOI 10.17487/RFC8174, May 2017,
              <https://www.rfc-editor.org/info/rfc8174>.

   [RFC8310]  Dickinson, S., Gillmor, D., and T. Reddy, "Usage
              Profiles for DNS over TLS and DNS over DTLS", RFC 8310,
              DOI 10.17487/RFC8310, March 2018,
              <https://www.rfc-editor.org/info/rfc8310>.

   [RFC8446]  Rescorla, E., "The Transport Layer Security (TLS)
              Protocol Version 1.3", RFC 8446,
              DOI 10.17487/RFC8446, August 2018,
              <https://www.rfc-editor.org/info/rfc8446>.

   [RFC8467]  Mayrhofer, A., "Padding Policies for Extension
              Mechanisms for DNS (EDNS(0))", RFC 8467,
              DOI 10.17487/RFC8467, October 2018,
              <https://www.rfc-editor.org/info/rfc8467>.

   [RFC8914]  Kumari, W., Hunt, E., Arends, R., Hardaker, W., and D.
              Lawrence, "Extended DNS Errors", RFC 8914,
              DOI 10.17487/RFC8914, October 2020,
              <https://www.rfc-editor.org/info/rfc8914>.

   [RFC9000]  Iyengar, J., Ed. and M. Thomson, Ed., "QUIC: A
              UDP-Based Multiplexed and Secure Transport", RFC 9000,
              DOI 10.17487/RFC9000, May 2021,
              <https://www.rfc-editor.org/info/rfc9000>.

   [RFC9001]  Thomson, M., Ed. and S. Turner, Ed., "Using TLS to
              Secure QUIC", RFC 9001, DOI 10.17487/RFC9001, May 2021,
              <https://www.rfc-editor.org/info/rfc9001>.

   [RFC9103]  Toorop, W., Dickinson, S., Sahib, S., Aras, P., and A.
              Mankin, "DNS Zone Transfer over TLS", RFC 9103,
              DOI 10.17487/RFC9103, August 2021,
              <https://www.rfc-editor.org/info/rfc9103>.

9.2.  참고적 참고문헌

   [BCP195]   Sheffer, Y., Holz, R., and P. Saint-Andre,
              "Recommendations for Secure Use of Transport Layer
              Security (TLS) and Datagram Transport Layer Security
              (DTLS)", BCP 195, RFC 7525, May 2015.

              Moriarty, K. and S. Farrell, "Deprecating TLS 1.0 and
              TLS 1.1", BCP 195, RFC 8996, March 2021.

              <https://www.rfc-editor.org/info/bcp195>

   [DNS-TERMS]
              Hoffman, P. and K. Fujiwara, "DNS Terminology", Work in
              Progress, Internet-Draft,
              draft-ietf-dnsop-rfc8499bis-03, 28 September 2021,
              <https://datatracker.ietf.org/doc/html/
              draft-ietf-dnsop-rfc8499bis-03>.

   [DNS0RTT]  Kahn Gillmor, D., "DNS + 0-RTT", Message to DNS-Privacy
              WG mailing list, 6 April 2016,
              <https://www.ietf.org/mail-archive/web/dns-privacy/
              current/msg01276.html>.

   [GREASING-QUIC]
              Thomson, M., "Greasing the QUIC Bit", Work in Progress,
              Internet-Draft, draft-ietf-quic-bit-grease-02,
              10 November 2021,
              <https://datatracker.ietf.org/doc/html/draft-ietf-
              quic-bit-grease-02>.

   [HTTP/3]   Bishop, M., Ed., "Hypertext Transfer Protocol Version 3
              (HTTP/3)", Work in Progress, Internet-Draft, draft-ietf-
              quic-http-34, 2 February 2021,
              <https://datatracker.ietf.org/doc/html/draft-ietf-quic-
              http-34>.

   [RFC1996]  Vixie, P., "A Mechanism for Prompt Notification of Zone
              Changes (DNS NOTIFY)", RFC 1996, DOI 10.17487/RFC1996,
              August 1996,
              <https://www.rfc-editor.org/info/rfc1996>.

   [RFC3833]  Atkins, D. and R. Austein, "Threat Analysis of the
              Domain Name System (DNS)", RFC 3833,
              DOI 10.17487/RFC3833, August 2004,
              <https://www.rfc-editor.org/info/rfc3833>.

   [RFC6335]  Cotton, M., Eggert, L., Touch, J., Westerlund, M., and
              S. Cheshire, "Internet Assigned Numbers Authority (IANA)
              Procedures for the Management of the Service Name and
              Transport Protocol Port Number Registry", BCP 165,
              RFC 6335, DOI 10.17487/RFC6335, August 2011,
              <https://www.rfc-editor.org/info/rfc6335>.

   [RFC7828]  Wouters, P., Abley, J., Dickinson, S., and R. Bellis,
              "The edns-tcp-keepalive EDNS0 Option", RFC 7828,
              DOI 10.17487/RFC7828, April 2016,
              <https://www.rfc-editor.org/info/rfc7828>.

   [RFC7873]  Eastlake 3rd, D. and M. Andrews, "Domain Name System
              (DNS) Cookies", RFC 7873, DOI 10.17487/RFC7873, May
              2016, <https://www.rfc-editor.org/info/rfc7873>.

   [RFC8094]  Reddy, T., Wing, D., and P. Patil, "DNS over Datagram
              Transport Layer Security (DTLS)", RFC 8094,
              DOI 10.17487/RFC8094, February 2017,
              <https://www.rfc-editor.org/info/rfc8094>.

   [RFC8484]  Hoffman, P. and P. McManus, "DNS Queries over HTTPS
              (DoH)", RFC 8484, DOI 10.17487/RFC8484, October 2018,
              <https://www.rfc-editor.org/info/rfc8484>.

   [RFC8490]  Bellis, R., Cheshire, S., Dickinson, J., Dickinson, S.,
              Lemon, T., and T. Pusateri, "DNS Stateful Operations",
              RFC 8490, DOI 10.17487/RFC8490, March 2019,
              <https://www.rfc-editor.org/info/rfc8490>.

   [RFC8932]  Dickinson, S., Overeinder, B., van Rijswijk-Deij, R.,
              and A. Mankin, "Recommendations for DNS Privacy Service
              Operators", BCP 232, RFC 8932, DOI 10.17487/RFC8932,
              October 2020,
              <https://www.rfc-editor.org/info/rfc8932>.

   [RFC9002]  Iyengar, J., Ed. and I. Swett, Ed., "QUIC Loss
              Detection and Congestion Control", RFC 9002,
              DOI 10.17487/RFC9002, May 2021,
              <https://www.rfc-editor.org/info/rfc9002>.

   [RFC9076]  Wicinski, T., Ed., "DNS Privacy Considerations",
              RFC 9076, DOI 10.17487/RFC9076, July 2021,
              <https://www.rfc-editor.org/info/rfc9076>.

부록 A.  NOTIFY 서비스

   이 부록은 0-RTT 데이터로 NOTIFY([RFC1996] 참조)를 보내는 것이
   수용 가능하다고 간주되는 이유를 논의한다.

   Section 4.5는 "0-RTT 메커니즘은 '재생 가능한' 트랜잭션이 아닌
   DNS 요청을 보내는 데 사용되어서는 안 된다(MUST NOT)"고 말한다.
   이 명세는 NOTIFY가 기술적으로 수신 서버의 상태를 변경하지만,
   NOTIFY를 재생하는 효과가 실제로는 무시할 만한 영향을 미치기
   때문에 0-RTT 데이터로 NOTIFY를 보내는 것을 지원한다.

   NOTIFY 메시지는 보조 서버에게 더 새로운 버전의 영역이 사용
   가능하다는 근거로 SOA 쿼리 또는 XFR 요청을 주 서버에 보내도록
   촉구한다. NOTIFY가 위조될 수 있으며, 이론적으로 보조 서버가
   주 서버에 반복적으로 불필요한 요청을 보내도록 하는 데 사용될 수
   있다는 것은 오래전부터 인정되어 왔다. 이러한 이유로, 대부분의
   구현은 하나 이상의 NOTIFY 수신에 의해 트리거되는 SOA/XFR 쿼리에
   대한 어떤 형태의 제한(throttling)을 가지고 있다.

   [RFC9103]은 NOTIFY 및 SOA 쿼리와 관련된 프라이버시 위험을
   기술하며 영역 전송의 암호화 범위 내에서 이러한 위험을 해결하는
   것을 포함하지 않는다. 이를 감안하면, NOTIFY에 DoQ를 사용하는
   프라이버시 이점은 명확하지 않지만, 같은 이유로, 0-RTT 데이터로
   NOTIFY를 보내는 것은 평문 DNS를 사용하여 보내는 것 이상의
   프라이버시 위험을 가지지 않는다.

감사의 글

   이 문서는 Mike Bishop이 편집한 HTTP/3 명세 [HTTP/3]와 Zi Hu,
   Liang Zhu, John Heidemann, Allison Mankin, Duane Wessels, Paul
   Hoffman이 저술한 DoT 명세 [RFC7858]의 텍스트를 자유롭게
   차용한다.

   0-RTT 데이터 및 세션 재개의 프라이버시 문제는 Daniel Kahn
   Gillmor(DKG)가 IETF DPRIVE Working Group에 보낸 메시지 [DNS0RTT]
   에서 분석되었다.

   이 문서의 초기 초안 버전에 대한 광범위한 검토를 해 준 Tony Finch
   에게, 그리고 0-RTT 프라이버시 문제에 대한 논의를 해 준 Robert
   Evans에게 감사한다. Paul Hoffman과 Martin Thomson의 초기 검토와
   Stephane Bortzmeyer가 수행한 상호운용성 테스트는 프로토콜의
   정의를 개선하는 데 도움이 되었다.

   또한 저수준 QUIC 세부사항에 초점을 맞춘 이후의 검토로 DoQ의 여러
   측면을 명확히 하는 데 도움을 준 Martin Thomson과 Martin Duke에게도
   감사한다. 검토와 기여를 해 준 Andrey Meshkov, Loganaden
   Velvindron, Lucas Pardue, Matt Joras, Mirja Kuelewind, Brian
   Trammell, Phillip Hallam-Baker에게도 감사한다.

저자 주소

   Christian Huitema
   Private Octopus Inc.
   427 Golfcourse Rd
   Friday Harbor, WA 98250
   United States of America
   Email: huitema@huitema.net

   Sara Dickinson
   Sinodun IT
   Oxford Science Park
   Oxford
   OX4 4GA
   United Kingdom
   Email: sara@sinodun.com

   Allison Mankin
   Salesforce
   Email: allison.mankin@gmail.com
