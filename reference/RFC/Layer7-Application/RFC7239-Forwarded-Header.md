Internet Engineering Task Force (IETF)                      A. Petersson
Request for Comments: 7239                                   M. Nilsson
Category: Standards Track                                 Opera Software
ISSN: 2070-1721                                             2014년 6월


                     Forwarded HTTP 확장

초록

   이 문서는 프록시 구성 요소가 프록싱 과정에서 변경되거나 손실되는
   정보를 공개할 수 있게 하는 HTTP 확장 헤더 필드를 정의한다.
   이를 통해 프록시의 사용자 에이전트 측 인터페이스의 IP 주소,
   예를 들어, 또는 HTTP 요청의 원본 클라이언트 IP 주소를 공개할 수
   있다. 이 확장은 모든 프록시 체인 구성 요소가 완전한 IP 주소
   정보에 접근할 수 있게 한다.

   이 문서는 또한 요청의 출처를 익명화하기 위한 지침을 명시한다.

이 메모의 상태

   이 문서는 인터넷 표준 트랙 문서이다.

   이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물이다. 이
   문서는 IETF 커뮤니티의 합의를 나타낸다. 이 문서는 공개 검토를
   받았으며 인터넷 엔지니어링 운영 그룹(IESG)에 의해 출판이
   승인되었다. 인터넷 표준에 대한 추가 정보는 RFC 5741의 섹션 2에서
   확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는
   방법에 대한 정보는 http://www.rfc-editor.org/info/rfc7239 에서
   얻을 수 있다.

저작권 고지

   Copyright (c) 2014 IETF Trust and the persons identified as the
   document authors. All rights reserved.

   이 문서는 BCP 78과 이 문서의 발행일에 유효한 IETF 트러스트의 IETF
   문서 관련 법적 조항(http://trustee.ietf.org/license-info)의
   적용을 받는다. 이 문서들은 이 문서에 대한 귀하의 권리와 제한 사항을
   설명하므로 주의 깊게 검토하기 바란다. 이 문서에서 추출된 코드
   구성 요소는 트러스트 법적 조항의 섹션 4.e에 기술된 대로 Simplified
   BSD License 텍스트를 포함해야 하며, Simplified BSD License에
   기술된 대로 보증 없이 제공된다.

목차

   1. 서론
   2. 표기법 규칙
   3. 구문 표기법
   4. Forwarded HTTP 헤더 필드
   5. 파라미터
      5.1. Forwarded By
      5.2. Forwarded For
      5.3. Forwarded Host
      5.4. Forwarded Proto
      5.5. 확장
   6. 노드 식별자
      6.1. IPv4 및 IPv6 식별자
      6.2. "unknown" 식별자
      6.3. 난독화된 식별자
   7. 구현 고려사항
      7.1. HTTP 리스트
      7.2. 헤더 필드 보존
      7.3. Via와의 관계
      7.4. 전환
      7.5. 사용 예시
   8. 보안 고려사항
      8.1. 헤더 유효성 및 무결성
      8.2. 정보 유출
      8.3. 프라이버시 고려사항
   9. IANA 고려사항
   10. 참조
      10.1. 규범적 참조
      10.2. 참고 참조
   부록 A. 감사의 글
   저자 주소

1. 서론

   오늘날의 HTTP 환경에서는 매우 다양한 수의 애플리케이션이
   프록시 역할을 수행한다. 이러한 프록시 중 상당수는 최종 사용자에게
   투명하게 동작한다. 이러한 프록시가 수행하는 기능의 예로는
   사용자 그룹을 위한 요청 로드 밸런싱, 암호화 오프로드, 자주
   접근하는 리소스 캐싱 등이 있다.

   프록시 환경에서의 큰 한계 중 하나는 프록시가 요청을 전달할 때
   마치 프록시의 IP 주소에서 시작된 것처럼 보이게 만드는 경향이
   있어, 원본 클라이언트에 대한 정보가 손실된다는 것이다.

   이 정보 손실은 요청하는 클라이언트의 IP 주소 정보를 필요로 하는
   서버의 수많은 기능에 부정적인 영향을 미칠 수 있다. 정보가 필요한
   기능의 범주로는 진단(diagnostics), 접근 제어(access control),
   남용 관리(abuse management)가 있다. 진단의 예로는 이벤트 로깅,
   문제 해결 및 통계 수집이 있다. 접근 제어의 예로는 클라이언트
   주소의 화이트리스팅이 있는데, 이는 신뢰할 수 있는 프록시 설정 없이
   작동하지 않는다. 남용 관리의 예로는 주로 진단 기능에 의존하는
   것들이 있다.

   대부분의 경우 이 정보 손실은 프록시의 목적이 아니라 의도하지
   않은 부작용이다. 프록시의 명시적 목적이 클라이언트 정보를
   숨기는 것인 경우의 예로 익명화 프록시(anonymizing proxy)가
   있다. 이러한 프록시는 이 확장을 구현하지 않을 것이다.

   리버스 프록시(reverse proxy)는 사용자와 원본 서버 사이에서
   정보를 숨기는 또 다른 유형의 프록시이다. 이 경우, 정보 숨김이
   의도하지 않은 부작용일 때, 이 정보를 HTTP 메시지 자체에
   인코딩하는 것이 다운스트림 소비자에게 도움이 된다.

   X-Forwarded-For, X-Forwarded-By, X-Forwarded-Proto와 같은 비표준
   헤더 필드들이 이 문제를 부분적으로 해결하기 위해 존재한다.
   이를 표준화하는 것은 상호운용성 관점에서 이점이 있다. 이
   문서에서 정의하는 "Forwarded" 헤더 필드는 모든 정보를 단일
   헤더 필드에 통합하므로 다른 관련 정보와의 상관관계를 가능하게
   한다 -- X-Forwarded-For와 X-Forwarded-Proto와 같은 서로 다른
   유형의 X-Forwarded-* 변형들로는 불가능했던 것이다. 이 헤더
   필드는 선택적이므로, 프록시가 프라이버시 보호를 위해 구현하지
   않기로 선택한 경우 영향을 받지 않는다. 네트워크 주소 변환(NAT)
   시스템에서도 유사한 문제가 발생하며, 이는 [RFC6269]에서 추가로
   논의된다.

2. 표기법 규칙

   이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL",
   "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY",
   "OPTIONAL"은 [RFC2119]에 기술된 대로 해석되어야 한다.

3. 구문 표기법

   이 명세는 [RFC5234]의 ABNF(Augmented Backus-Naur Form) 표기법을
   사용하며, [RFC7230] 섹션 7의 리스트 규칙 확장을 포함한다.

4. Forwarded HTTP 헤더 필드

   "Forwarded" HTTP 헤더 필드는 프록시가 프록싱 과정에서 변경되거나
   손실될 수 있는 정보를 공개할 수 있는 선택적 헤더 필드이다.
   공개 가능한 정보는 파라미터-식별자 쌍의 형태를 취하며, 또한
   "섹션 5.5"에 기술된 확장을 통해 추가될 수 있다. 이 헤더 필드에
   담긴 정보는 민감할 수 있으므로("섹션 8" 참조), 이 헤더 필드의
   배포 시 기본값은 비활성화되어야 하며(SHOULD), 개별 파라미터는
   개별적으로 활성화할 수 있다. 이 헤더 필드는 HTTP 응답이 아닌
   HTTP 요청에서만 사용해야 한다(MUST). 이 헤더 필드는 포워딩
   프록시와 리버스 프록시 모두에 적용 가능하다.

   전달되는 정보의 예로는 요청하는 클라이언트의 소스 IP 주소,
   프록시의 수신 인터페이스의 IP 주소, 또는 클라이언트가 사용한
   프로토콜이 있다. 체인의 여러 프록시가 각각 파라미터 세트를
   추가할 수 있다; 프록시는 또한 이전에 추가된 "Forwarded" 헤더
   필드를 삭제하도록 선택할 수 있다.

   최상위 리스트는 [RFC7230] 섹션 3.2에 정의된 HTTP 헤더 필드-값
   목록이다. 리스트의 첫 번째 요소는 이 확장을 구현하는 첫 번째
   프록시에 의해 추가된 정보를 포함하며, 리스트의 이후 요소는
   각 후속 프록시에 의해 추가된 정보를 포함한다. 이 헤더 필드는
   선택적이므로, 이 확장을 지원하는 프록시라도 선택에 따라 업데이트
   하지 않을 수 있다.

   리스트의 각 필드-값은 세미콜론으로 구분된 파라미터-식별자 쌍의
   목록이다. 파라미터-식별자 쌍은 파라미터가 앞에 오고, 그 뒤에
   등호와 값이 오는 형태로 그룹화된다. 각 파라미터는 필드-값당
   최대 한 번만 나타나야 한다(MUST). 파라미터 이름은 대소문자를
   구분하지 않는다. 파라미터 값은 [RFC7230] 섹션 3.2.6에 기술된
   대로 토큰이나 따옴표로 묶인 문자열일 수 있다. 토큰 파라미터
   값은 대소문자를 구분하지 않을 수 있다(MAY).

   ABNF 구문:

     Forwarded   = 1#forwarded-element

     forwarded-element =
         [ forwarded-pair ] *( ";" [ forwarded-pair ] )

     forwarded-pair = token "=" value
     value          = token / quoted-string

     token = <[RFC7230] 섹션 3.2.6에 정의됨>
     quoted-string = <[RFC7230] 섹션 3.2.6에 정의됨>

   예시:

     Forwarded: for="_gazonk"
     Forwarded: For="[2001:db8:cafe::17]:4711"
     Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
     Forwarded: for=192.0.2.43, for=198.51.100.17

   IPv6 주소는 콜론과 대괄호가 유효한 토큰 문자가 아니기 때문에
   따옴표로 묶이고 대괄호로 감싸진다는 점에 유의한다.

   프록시 서버가 이미 "Forwarded" 헤더 필드를 포함하는 요청에 새
   "Forwarded" 헤더 필드 값을 추가할 때, 프록시 서버는 기존의
   마지막 "Forwarded" 헤더 필드의 끝에 쉼표 다음에 새 필드 값을
   추가하거나, 헤더 블록의 끝에 새 필드를 추가하는 방식으로 추가할
   수 있다. 프록시는 기존의 모든 "Forwarded" 헤더 필드를 제거할
   수도 있다(MAY). 프록시는 여러 "Forwarded" 헤더 필드가 존재할 때
   올바른 필드가 업데이트되도록 해야 한다(MUST).

5. 파라미터

   이 문서는 "Forwarded" 헤더 필드와 함께 사용할 파라미터를 다음과
   같이 명시한다: "by", "for", "host", "proto". 이 파라미터의 값은
   각각 "by"는 이 요청이 들어온 프록시의 사용자 에이전트 측
   인터페이스를, "for"는 프록시에 요청하는 노드를, "host"는 프록시가
   받은 host 요청 헤더 필드를, "proto"는 수신 요청에 사용된
   프로토콜을 식별한다.

5.1. Forwarded By

   "by" 파라미터는 프록시 서버로의 수신 요청의 사용자 에이전트 측
   인터페이스를 공개하는 데 사용된다. 이것은 일반적으로 IP 주소와
   선택적으로 포트 번호이다. "by" 파라미터의 기본 설정은 섹션 6.3에
   기술된 대로 난독화된 식별자를 포함해야 한다(SHOULD). 서버가
   주소 기반 기능을 필요로 하는 경우, 설정은 IP 주소와 선택적으로
   포트 번호를 대신 사용하도록 변경될 수 있다. 세 번째 옵션은
   섹션 6.2에 기술된 "unknown" 식별자이다.

   "by" 값의 구문은, 잠재적인 따옴표 문자열 이스케이프 해제 후,
   섹션 6의 "node" ABNF를 따라야 한다(MUST).

   이 파라미터의 주된 사용 사례는 리버스 프록시가 백엔드 서버에
   정보를 전달하려는 경우이다. 또한 멀티홈 환경에서 요청이 어디서
   들어왔는지 신호를 보내는 데에도 유용할 수 있다.

5.2. Forwarded For

   "for" 파라미터는 요청을 시작한 클라이언트와 프록시 체인의 후속
   프록시를 공개하는 데 사용된다. 이것은 일반적으로 IP 주소와
   선택적으로 포트 번호이다. "for" 파라미터의 기본 설정은 섹션 6.3에
   기술된 대로 난독화된 식별자를 포함해야 한다(SHOULD). 서버가
   주소 기반 기능을 필요로 하는 경우, 설정은 IP 주소와 선택적으로
   포트 번호를 대신 사용하도록 변경될 수 있다. 세 번째 옵션은
   섹션 6.2에 기술된 "unknown" 식별자이다.

   "for" 값의 구문은, 잠재적인 따옴표 문자열 이스케이프 해제 후,
   섹션 6의 "node" ABNF를 따라야 한다(MUST).

   이 확장을 완전히 활용하는 프록시 체인에서는, "for" 파라미터의
   첫 번째 값이 최초 요청하는 클라이언트를 나타내고, 이후의 각
   "for" 값은 이전 프록시의 식별자를 나타낸다. 체인의 마지막
   프록시는 "for" 파라미터 목록에 나타나지 않는다. 마지막 프록시의
   IP 주소와 선택적으로 포트 번호는 네트워크 전송 계층의 원격 IP
   주소로 쉽게 확인할 수 있다. 그러나, 이전 "Forwarded" 헤더 필드의
   "by" 파라미터가 마지막 프록시를 식별하는 데 더 적절한 정보를
   제공할 수 있다.

5.3. Forwarded Host

   "host" 파라미터는 원본 "Host" 요청 헤더 필드를 전달하는 데
   사용된다. 이는 리버스 프록시가 들어오는 "Host" HTTP 요청 헤더
   필드를 내부 호스트 이름으로 재작성하는 경우에 유용하다.

   "host" 값의 구문은, 잠재적인 따옴표 문자열 이스케이프 해제 후,
   [RFC7230] 섹션 5.4의 Host ABNF를 따라야 한다(MUST).

5.4. Forwarded Proto

   "proto" 파라미터는 사용된 프로토콜 유형 값을 포함한다. "proto"
   값의 구문은, 잠재적인 따옴표 문자열 이스케이프 해제 후,
   [RFC3986] 섹션 3.1에 정의되고 [RFC4395]에 따라 IANA에 등록된
   URI 스킴 이름을 따라야 한다(MUST). 일반적인 값은 "http" 또는
   "https"이다.

   예를 들어, 리버스 프록시가 암호화 오프로드를 제공하는 환경에서,
   원본 서버에 대한 연결은 암호화되지 않은 HTTP를 사용할 수 있지만,
   이 파라미터를 통해 원본 서버가 사용자 에이전트가 요청한 연결
   유형에 맞게 문서의 URL을 올바르게 재작성할 수 있다.

5.5. 확장

   확장은 추가 파라미터와 값을 허용하는 데 사용된다. 확장은
   리버스 프록시 환경에서 특히 유용할 수 있다. 모든 확장
   파라미터는 "HTTP Forwarded Parameter" 레지스트리에 등록되어야
   한다(SHOULD). 광범위한 배포가 예상되는 확장은 섹션 9에서 추가로
   논의되는 바와 같이 표준화되어야 한다(SHOULD).

6. 노드 식별자

   "Forwarded" 헤더 필드의 "by"와 "for" 파라미터의 각 값은 다음
   중 하나로 구성된 식별자를 포함한다:

   o  선택적으로 포트 번호를 포함하는 네트워크 프로토콜에서의
      클라이언트 IP 주소

   o  요청하는 클라이언트의 IP 주소를 알 수 없는 경우 이를 나타내는
      토큰

   o  내부 구조 또는 민감한 정보를 공개하지 않으면서 추적 및
      디버깅을 가능하게 하는 생성된 토큰

   ABNF 구문:

     node     = nodename [ ":" node-port ]
     nodename = IPv4address / "[" IPv6address "]" /
                 "unknown" / obfnode

     IPv4address = <[RFC3986] 섹션 3.2.2에 정의됨>
     IPv6address = <[RFC3986] 섹션 3.2.2에 정의됨>
     obfnode = "_" 1*( ALPHA / DIGIT / "." / "_" / "-")

     node-port     = port / obfport
     port          = 1*5DIGIT
     obfport       = "_" 1*(ALPHA / DIGIT / "." / "_" / "-")

     DIGIT = <[RFC5234] 섹션 3.4에 정의됨>
     ALPHA = <[RFC5234] 섹션 B.1에 정의됨>

   각 식별자는 선택적으로 포트 식별과 함께 사용될 수 있다. 이는
   NAT(네트워크 주소 변환) 환경에서 특히 유용하다. "node-port"는
   실제 포트 번호이거나 실제 포트 번호를 숨기는 난독화된 토큰이다.
   이를 통해 내부 정보를 드러내지 않으면서도 디버깅이 가능하다.

   ABNF은 포트 번호가 "unknown" 식별자에도 추가될 수 있도록
   허용하지만, 이러한 값의 해석은 이를 소유한 프록시에 달려 있다.
   "obfport"를 실제 포트 번호와 구분하기 위해, "obfport"는 선행
   밑줄이 필요하며(MUST), "ALPHA", "DIGIT", 그리고 문자 ".", "_",
   "-"로만 구성되어야 한다.

   IPv6 주소와 node-port가 있는 모든 nodename은 콜론이 토큰에서
   허용되지 않으므로 따옴표로 묶어야 한다(MUST).

   예시:

     "192.0.2.43:47011"
     "[2001:db8:cafe::17]:47011"

6.1. IPv4 및 IPv6 식별자

   "IPv6address"와 "IPv4address"에 대한 ABNF 규칙은 [RFC3986]에
   명시되어 있다. "IPv6address"는 [RFC5952]의 텍스트 표현
   권장사항(소문자, 제로 압축)을 따라야 한다(SHOULD).

   IP 주소는 [RFC1918] 및 [RFC4193]에 따라 내부 네트워크에서 올
   수 있다. IPv6 주소는 항상 대괄호로 감싸야 한다.

6.2. "unknown" 식별자

   "unknown" 식별자는 이전 엔티티의 신원을 알 수 없지만 프록시가
   여전히 요청 전달이 이루어졌음을 알리고자 할 때 사용된다.
   이의 한 가지 예는 수신 요청의 TCP 소켓에 직접 접근하지 않고
   발신 요청을 생성하는 프록시 서버 프로세스이다.

6.3. 난독화된 식별자

   생성된 식별자는 내부 IP 주소를 숨기는 동시에 "Forwarded" 헤더
   필드가 추적 및 디버깅에 사용될 수 있게 한다. 이는 프록시가
   IP 주소가 아닌 전송이 필요한 인터페이스 레이블을 사용하는
   경우에도 유용할 수 있다.

   정적 식별자 할당이 필요한 경우가 아니라면, 난독화된 식별자는
   요청마다 무작위로 생성되어야 한다(SHOULD). 요청 간에 지속되는
   식별자가 필요한 경우, 식별자의 수명은 해당 구현에서 클라이언트
   IP 주소를 유지하는 기간과 같거나 짧아야 한다(SHOULD). 난독화된
   식별자는 선행 밑줄 "_"을 포함해야 하며(MUST), "ALPHA", "DIGIT",
   그리고 문자 ".", "_", "-"로만 구성되어야 한다(MUST).

   예시:

     Forwarded: for=_hidden, for=_SEVKISEK

7. 구현 고려사항

7.1. HTTP 리스트

   HTTP 리스트는 식별자 사이에 공백을 허용하며, 여러 헤더 필드에
   걸쳐 있을 수 있다. 예를 들어, 다음은 모두 동일하다:

     Forwarded: for=192.0.2.43,for="[2001:db8:cafe::17]",for=unknown
     Forwarded: for=192.0.2.43, for="[2001:db8:cafe::17]", for=unknown

   또한:

     Forwarded: for=192.0.2.43
     Forwarded: for="[2001:db8:cafe::17]", for=unknown

7.2. 헤더 필드 보존

   일부 경우에는 "Forwarded" 헤더 필드가 보존되어야 하고, 다른
   경우에는 그렇지 않다. 직접 전달되는 요청에서는 헤더를 보존하고
   잠재적으로 확장해야 한다. 하나의 수신 요청이 여러 발신 요청을
   생성하는 경우, 특별한 주의를 기울여 헤더 필드 보존이 필요한지
   판단해야 한다. 여러 발신 요청이 있는 경우, 일반적으로 헤더
   필드를 보존해야 하지만, 수신 요청의 직접적인 결과가 아닌 발신
   요청에는 보존하지 않아야 한다. [RFC7232] 섹션 4.1을 참고하면,
   프록시가 304 응답의 콘텐츠가 캐시된 엔티티와 다르다고 감지한
   경우 조건 없이 요청을 반복해야 하는 경우가 있다. 이러한 반복된
   요청은 일반적으로 수신 요청의 직접적인 결과로 간주되므로 헤더
   보존이 적절하다.

7.3. Via와의 관계

   "Via" 헤더 필드([RFC7230] 섹션 5.7.1에 기술)는 "Forwarded"
   헤더 필드와 유사한 목적으로 사용된다. Via 헤더 필드는 클라이언트
   측 정보를 제공하지 않는다. Via는 프록시 자체에 대한 정보를
   제공하는 반면, "Forwarded"는 프록시가 릴레이하는 클라이언트 측
   정보에 초점을 맞춘다. "Via"는 이미 널리 배포되었으므로,
   "Forwarded"가 해결하는 문제를 다루기 위해 Via의 형식을 변경하는
   것은 불가능하다.

   이 헤더 필드의 정보를 "Via" 헤더 필드의 정보와 결합하여 해석하는
   것은 불가능할 수 있다는 점에 유의한다. 일부 프록시는 "Forwarded"를
   업데이트하고, 다른 프록시는 Via를 업데이트하며, 또 다른 프록시는
   둘 다 업데이트한다.

7.4. 전환

   X-Forwarded-For와 같은 X-Forwarded-* 헤더 필드를 수신하는
   프록시가 이를 "Forwarded" 형식으로 합리적으로 변환하는 것이
   바람직할 수 있다. 단일 유형의 헤더가 있는 경우, 예를 들어
   X-Forwarded-For의 경우, 이는 각 요소 앞에 "for="를 추가하여
   수행할 수 있다. X-Forwarded-For의 IPv6 주소는 따옴표로 묶이지
   않고 대괄호로 감싸지지 않을 수 있지만, "Forwarded" 헤더 필드에서는
   따옴표와 대괄호 모두가 필요하다는 점에 유의한다.

   예시:

     X-Forwarded-For: 192.0.2.43, 2001:db8:cafe::17

     이 헤더는 다음과 같이 변환된다:

     Forwarded: for=192.0.2.43, for="[2001:db8:cafe::17]"

   그러나 여러 유형의 X-Forwarded-* 헤더가 있는 경우 특별한 주의가
   필요하다. 이 경우, 추가 순서를 모르면 변환이 불가능할 수 있다.
   또한, X-Forwarded-For 헤더를 제거하면 아직 "Forwarded"를
   지원하지 않는 당사자들에게 문제를 일으킬 수 있다.

7.5. 사용 예시

   IP 주소가 192.0.2.43인 클라이언트가 198.51.100.17의 프록시를
   사용하여 203.0.113.60의 또 다른 프록시를 거쳐 원본 서버에
   도달하는 경우를 가정한다. 이는 예를 들어 회사의 악성코드 필터링
   프록시 뒤에 있는 사무실 클라이언트가 그 사이에 리버스 프록시가
   있는 서버에 접근하는 경우일 수 있다.

   o  클라이언트에서 첫 번째 프록시로 전달되는 HTTP 요청:

      클라이언트에서 직접 전달되므로 "Forwarded" 헤더 필드가 없다.

   o  첫 번째 프록시에서 두 번째 프록시로의 요청:

      Forwarded: for=192.0.2.43

   o  두 번째 프록시에서 원본 서버로의 요청:

      Forwarded: for=192.0.2.43,
                 for=198.51.100.17;by=203.0.113.60;proto=http;
                 host=example.com

   연결 체인의 특정 지점에서 확장 지원이 없거나 네트워크 구성
   요소에 대한 정보를 공개하지 않기로 한 정책 결정으로 인해 정보가
   업데이트되지 않을 수 있다는 점에 유의한다.

8. 보안 고려사항

8.1. 헤더 유효성 및 무결성

   "Forwarded" HTTP 헤더 필드는 올바르다고 신뢰할 수 없다. 이
   헤더는 요청하는 클라이언트를 포함하여 서버까지의 경로에 있는 모든
   노드에 의해 실수로 또는 악의적으로 수정될 수 있기 때문이다.

   이러한 헤더 필드의 정확성을 검증하는 한 가지 방법은 기존의
   신뢰할 수 있는 프록시를 화이트리스트에 추가하고, 해당 프록시에서
   제공하는 헤더 필드를 기반으로 결정하는 것이다. 이 접근 방식에는
   두 가지 약점이 있다. 첫째, 프록시에 대한 요청 이전에 나열된
   IP 주소 체인은 신뢰할 수 없다. 둘째, 프록시와 엔드포인트 사이의
   네트워크 통신이 보호되지 않는 한, 네트워크에 접근할 수 있는
   공격자가 데이터를 수정할 수 있다.

8.2. 정보 유출

   "Forwarded" HTTP 헤더 필드는 NAT 또는 프록시 설정 뒤의 내부
   네트워크 구조를 잠재적으로 원치 않게 노출할 수 있다. 이 문제를
   해결하는 방법으로는 난독화된 요소 사용, 내부 노드에 의한 헤더
   비업데이트, 또는 출구 프록시에서의 항목 제거 등이 있다.

   이 헤더 필드는 원본 서버나 중간자의 응답 메시지에 절대 나타나서는
   안 된다. 이 경우 전체 프록시 체인이 클라이언트에게 노출될 수
   있기 때문이다. 결과적으로, "Forwarded" 필드가 사용되는 호스팅
   환경에서는 "Forwarded" 필드가 응답 본문에 나타나는 TRACE 요청에
   주의해야 한다.

8.3. 프라이버시 고려사항

   프라이버시에 대한 관심이 높아지는 현대 사회에서, 사용자
   프라이버시와 유용한 디버깅/통계/위치 기반 콘텐츠 정보 공개
   사이에는 트레이드오프가 존재한다. 설계상, "Forwarded"는 많은
   사람들이 프라이버시에 민감하다고 여기는 정보를 노출한다.

   모든 프록시에 대해, HTTP 요청이 프라이버시 의미를 요청하는
   헤더 필드를 포함하는 경우, 프록시는 "Forwarded" 헤더 필드를
   사용해서는 안 되며(SHOULD NOT), IP 주소와 같은 개인 정보를
   어떤 다른 방식으로도 다음 홉으로 전달해서는 안 된다(SHOULD NOT).

   "for" 파라미터에서 전달되는 클라이언트 IP 주소는 많은 사람들에
   의해 프라이버시에 민감하다고 여겨진다. IP 주소를 통해 개별
   클라이언트를 식별할 수 있고, 사용자가 사용하는 인터넷 서비스
   사업자를 식별할 수 있으며, 클라이언트의 대략적인 지리적 위치를
   추정할 수 있기 때문이다.

   직접 연결에서 사용 가능한 정보를 보존하는 프록시는 사용자나
   배포자가 이를 인식하고 있는지 또는 기대하고 있는지에 관계없이
   최종 사용자의 프라이버시에 영향을 미친다.

   구현자와 배포자는 이 확장의 배포가 사용자 프라이버시에 미치는
   영향을 고려해야 한다.

   "by"와 "for" 파라미터의 기본 설정은 요청마다 무작위로 생성되는
   난독화된 식별자를 포함해야 한다(SHOULD). 요청 간에 지속되는
   식별자가 필요한 경우, 식별자의 수명은 제한되어야 하며 해당
   구현에서의 클라이언트 IP 주소 유지 기간을 초과해서는 안 된다
   (SHOULD). 난독화된 식별자 생성 시 잠재적으로 민감한 정보를
   포함하지 않도록 주의를 기울여야 한다.

   사용자의 IP 주소는 이미 X-Forwarded-For를 통해 전달하는
   프록시에 의해 전달될 수 있으며, 이는 널리 배포되어 있다.
   사용자가 프록시 중개 없이 직접 연결하는 경우, 클라이언트의
   IP 주소는 어차피 웹 서버로 전송된다. 비익명화 프록시를 선택하여
   연결하는 사용자는 IP 주소 보호를 기대할 수 없다. 추적 위험을
   최소화하기 위해, 브라우저의 헤더 필드 핑거프린팅을 포함하여 다른
   정보 유출 방법도 유의해야 한다. 고유한 클라이언트 식별자가
   포함되지 않더라도, "Forwarded" 헤더 필드 자체가 통과한 프록시
   체인을 드러냄으로써 핑거프린팅을 더 용이하게 할 수 있다.

9. IANA 고려사항

   이 문서는 아래에 나열된 HTTP 헤더 필드를 명시하며, 이는
   [RFC3864]의 "영구 메시지 헤더 필드 이름(Permanent Message
   Header Field Names)" 레지스트리에 추가되었다.

   헤더 필드:  Forwarded
   적용 프로토콜:  http
   상태:  standard
   저자/변경 관리자:  IETF (iesg@ietf.org)
      Internet Engineering Task Force
   명세 문서:  이 명세 (섹션 4)
   관련 정보:  없음

   "Forwarded" 헤더 필드는 IANA가 "HTTP Forwarded Parameters"
   레지스트리를 설립하고 유지관리하는 파라미터를 포함한다. 초기
   등록은 아래에 있다. 추가적인 할당은 [RFC5226]에 따른 IETF Review
   절차에 의해 이루어진다. 모든 새 파라미터의 보안 및 프라이버시
   영향은 철저히 문서화되어야 한다. 새 파라미터와 값은 섹션 4의
   forwarded-pair ABNF 정의를 따라야 한다(MUST). 또한 등록 시
   간단한 설명을 포함해야 한다.

   +-------------+-----------------------------------------------+-------------+
   | 파라미터명  | 설명                                          | 참조        |
   +-------------+-----------------------------------------------+-------------+
   | by          | 프록시의 수신 인터페이스 IP 주소              | 섹션 5.1    |
   | for         | 프록시를 통해 요청하는 클라이언트의 IP 주소   | 섹션 5.2    |
   | host        | 수신 요청의 Host 헤더 필드                    | 섹션 5.3    |
   | proto       | 수신 요청에 사용된 애플리케이션 프로토콜      | 섹션 5.4    |
   +-------------+-----------------------------------------------+-------------+

10. 참조

10.1. 규범적 참조

   [RFC1918]  Rekhter, Y., Moskowitz, R., Karrenberg, D., Groot, G.,
              and E. Lear, "Address Allocation for Private Internets",
              BCP 5, RFC 1918, February 1996.

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119, March 1997.

   [RFC3864]  Klyne, G., Nottingham, M., and J. Mogul, "Registration
              Procedures for Message Header Fields", BCP 90, RFC 3864,
              September 2004.

   [RFC3986]  Berners-Lee, T., Fielding, R., and L. Masinter, "Uniform
              Resource Identifier (URI): Generic Syntax", STD 66,
              RFC 3986, January 2005.

   [RFC4193]  Hinden, R. and B. Haberman, "Unique Local IPv6 Unicast
              Addresses", RFC 4193, October 2005.

   [RFC4395]  Hansen, T., Hardie, T., and L. Masinter, "Guidelines and
              Registration Procedures for New URI Schemes", BCP 35,
              RFC 4395, February 2006.

   [RFC5226]  Narten, T. and H. Alvestrand, "Guidelines for Writing an
              IANA Considerations Section in RFCs", BCP 26, RFC 5226,
              May 2008.

   [RFC5234]  Crocker, D. and P. Overell, "Augmented BNF for Syntax
              Specifications: ABNF", STD 68, RFC 5234, January 2008.

   [RFC5952]  Kawamura, S. and M. Kawashima, "A Recommendation for IPv6
              Address Text Representation", RFC 5952, August 2010.

   [RFC7230]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext
              Transfer Protocol (HTTP/1.1): Message Syntax and Routing",
              RFC 7230, June 2014.

   [RFC7232]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext
              Transfer Protocol (HTTP/1.1): Conditional Requests",
              RFC 7232, June 2014.

10.2. 참고 참조

   [RFC6269]  Ford, M., Boucadair, M., Durand, A., Levis, P., and
              P. Roberts, "Issues with IP Address Sharing", RFC 6269,
              June 2011.

부록 A. 감사의 글

   이 문서의 기여자로는 Per Cederqvist, Alissa Cooper, Adrian Farrel,
   Stephen Farrell, Ned Freed, Per Hedbor, Amos Jeffries, Poul-Henning
   Kamp, Murray S. Kucherawy, Barry Leiba, Salvatore Loreto, Alexey
   Melnikov, S. Moonesamy, Susan Nichols, Mark Nottingham, Julian
   Reschke, John Sullivan, Willy Tarreau, Dan Wing이 있다.

저자 주소

   Andreas Petersson
   Opera Software

   EMail: andreas@sbin.se


   Martin Nilsson
   Opera Software
   S:t Larsgatan 12
   Linkoping  SE-582 24

   EMail: nilsson@opera.com
