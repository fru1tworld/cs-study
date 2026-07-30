# HTTP 메서드와 상태 코드

Internet Engineering Task Force (IETF)                     L. Dusseault
Request for Comments: 5789                          Linden Lab
Category: Standards Track                                     J. Snell
ISSN: 2070-1721                                           2010년 3월


                       HTTP를 위한 PATCH 메서드

#### Abstract

   여러 애플리케이션에서 리소스의 부분 수정을 수행하기 위한 기능이 필요하며,
   기존 HTTP PUT 메서드는 문서의 완전한 교체만 허용합니다.
 이 제안은 기존
   HTTP 리소스를 수정하기 위한 새로운 HTTP 메서드인 PATCH를 추가합니다.

이 메모의 상태

   이 문서는 인터넷 표준 트랙 문서입니다.

   이 문서는 Internet Engineering Task Force (IETF)의 산출물입니다.
 이 문서는
   IETF 커뮤니티의 합의를 나타냅니다.
 이 문서는 공개 검토를 받았으며 Internet
   Engineering Steering Group (IESG)에 의해 출판이 승인되었습니다.
 인터넷
   표준에 대한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있습니다.

   이 문서의 현재 상태, 정오표, 그리고 피드백을 제공하는 방법에 대한 정보는
   http://www.rfc-editor.org/info/rfc5789 에서 확인할 수 있습니다.

저작권 고지

   Copyright (c) 2010 IETF Trust and the persons identified as the
   document authors. All rights reserved.

   이 문서는 이 문서의 발행일에 유효한 BCP 78 및 IETF 문서에 관한 IETF
   Trust의 법적 조항 (http://trustee.ietf.org/license-info)의 적용을 받습니다.
   이 문서에 관한 귀하의 권리와 제한사항이 기술되어 있으므로 이 문서들을
   주의 깊게 검토하시기 바랍니다.
 이 문서에서 추출된 코드 구성요소는
   Trust Legal Provisions의 섹션 4.e에 기술된 대로 Simplified BSD License
   텍스트를 포함해야 하며, Simplified BSD License에 기술된 대로 보증 없이
   제공됩니다.

목차

   1. 소개 ............................................................. 2
   2. PATCH 메서드 ..................................................... 2
      2.1. 간단한 PATCH 예시 ........................................... 4
      2.2. 오류 처리 ................................................... 5
   3. OPTIONS에서 지원 알림 ............................................ 7
      3.1. Accept-Patch 헤더 ........................................... 7
      3.2. OPTIONS 요청 및 응답 예시 ................................... 7
   4. IANA 고려사항 .................................................... 7
      4.1. Accept-Patch 응답 헤더 등록 ................................. 7
   5. 보안 고려사항 .................................................... 8
   6. 참조 ............................................................ 9
      6.1. 규범적 참조 ................................................ 9
      6.2. 정보적 참조 ................................................ 9
   부록 A. 감사의 말 .................................................. 10

1. 소개

   이 명세는 기존 리소스에 부분 수정을 적용하기 위한 새로운 HTTP/1.1
   ([RFC2616]) 메서드인 PATCH를 정의합니다.

   새로운 메서드는 기존 HTTP 인프라의 상호운용성을 개선하고
   리소스를 부분적으로 수정하기 위한 다른 HTTP 메서드의 사용으로 인한
   혼란을 방지하기 위해 필요합니다.
 기존 HTTP PUT 메서드는 문서의 완전한
   교체만 허용하는 등의 다양한 이유가 있습니다.
 PATCH라는 메서드는
   이전에 HTTP에 사용된 적이 있지만 완전하게 정의되지 않았습니다.

   부분 PUT이 아닌 이 메커니즘의 정의는 정확한 의미가 재정의하는 것이
   어렵고 기존의 파이프라인된 PUT과의 상호운용이 어려운 경우를 피하기
   위해 필요했습니다.

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL" 키워드는
   [RFC2119]에 기술된 대로 해석되어야 합니다.

   또한, 이 문서는 [RFC2616] 섹션 2.1의 ABNF 구문 표기법을 사용합니다.

2. PATCH 메서드

   PATCH 메서드는 요청 엔티티에 기술된 변경 사항 집합을 Request-URI로
   식별되는 리소스에 적용하도록 요청합니다.
 변경 사항 집합은 미디어
   타입으로 식별되는 "패치 문서"라는 형식으로 표현됩니다.
 Request-URI가
   기존 리소스를 가리키지 않는 경우, 서버는 패치 문서 유형(논리적으로
   존재하지 않는 리소스를 수정할 수 있는지 여부)과 권한 등에 따라 새
   리소스를 생성할 수 있습니다(MAY).

   PUT 요청과 PATCH 요청의 차이점은 서버가 동봉된 엔티티를 처리하여
   Request-URI로 식별되는 리소스를 수정하는 방식에 반영됩니다.
 PUT
   요청에서 동봉된 엔티티는 원본 서버에 저장된 리소스의 수정된 버전으로
   간주되며, 클라이언트는 저장된 버전의 교체를 요청합니다.
 그러나
   PATCH에서 동봉된 엔티티는 원본 서버에 현재 존재하는 리소스가 새
   버전을 생성하기 위해 어떻게 수정되어야 하는지를 설명하는 명령어
   집합을 포함합니다.
 PATCH 메서드는 Request-URI로 식별되는 리소스에
   영향을 미치며, 다른 리소스에도 부수적인 영향을 미칠 수 있습니다(MAY).
   즉, PATCH의 적용에 의해 새로운 리소스가 생성되거나 기존 리소스가
   수정될 수 있습니다.

   PATCH는 [RFC2616] 섹션 9.1에 정의된 대로 안전하지도 않고 멱등하지도
   않습니다.

   PATCH 요청은 그것이 적용되는 리소스에 영향을 미치는 방식으로 발행될
   수 있지만, 일반적으로 멱등한 방식은 아닙니다.
 그러나 멱등한 패치
   요청을 발행하는 것이 가능하며, 이는 또한 유사한 시간대에 동일
   리소스에 대한 두 PATCH 요청 간의 충돌로 인한 나쁜 결과를 방지하는
   데 도움이 됩니다.

   여러 PATCH 요청의 충돌은 PUT 충돌보다 위험할 수 있습니다.
 왜냐하면
   일부 패치 형식은 알려진 기준점에서 작동해야 하기 때문입니다.
 그렇지
   않으면 리소스가 손상될 수 있습니다.
 이 유형의 패치 애플리케이션을
   사용하는 클라이언트는 클라이언트가 마지막으로 리소스에 접근한 이후
   리소스가 업데이트된 경우 요청이 실패하도록 조건부 요청을 사용해야
   합니다(SHOULD).
 예를 들어, 클라이언트는 패치 요청의 If-Match 헤더에
   ETag를 사용할 수 있습니다.

   PATCH를 사용한 리소스 생성 또는 수정에 대한 기본적인 제한은 없습니다.
   다만 서버는 전체 변경 사항 집합을 원자적으로 적용해야 하며(MUST), 이
   작업 도중(예: GET에 대한 응답으로) 일부만 적용된 표현을 절대로
   제공해서는 안 됩니다(MUST NOT).

   PATCH에 대한 "성공적" 응답은 적절한 것만을 의미합니다 -- 이전 또는
   이후에 적용되는 다른 (패치가 아닌) 작업의 결과가 아닌, 전체 패치가
   적용되었음을 의미합니다.
 어떤 서버 상태를 확인하기 위해 패치를
   적용한 후 클라이언트의 GET 요청은 중간에 다른 요청을 발행한 다른
   사용자가 있을 수 있으므 보장되지 않습니다.

   서버는 수신된 패치 문서가 Request-URI로 식별된 리소스 유형에 적합한지
   확인해야 합니다(MUST).
 관련성이 없는 패치 문서를 잘못 적용하는 것은
   리소스를 손상시키거나 보안 취약점을 도입할 수 있으므로 서버가 이를
   방지해야 합니다.

   적용 가능한 PATCH의 대체 형식이 있는지 여부의 결정은 전적으로 서버에
   의해 이루어집니다.
 서버가 단일 유형의 패치 문서만 수락하도록 선택
   하더라도 이는 완전히 수용 가능합니다.
 유사하게, 패치 문서 크기와 전체
   교체될 문서의 크기를 비교하는 PATCH를 사용할지 PUT을 사용할지에 대한
   결정을 하는 사람은 궁극적으로 클라이언트이지만, 리소스가 어떤 패치
   문서 형식을 지원할지에 대한 결정을 내리는 것은 서버입니다.

   이 문서에서 정의되지 않은 부분에서 PATCH에 대한 서버의 처리는
   PUT과 유사해야 합니다.
 여기에는 성공 및 오류에 대한 응답 의미론,
   인가, 그리고 요청 헤더의 상호작용이 포함됩니다.
 특히, 특정 패치
   문서 형식에서 명시적으로 허용하지 않는 한 PATCH 요청의 엔티티
   헤더는 PATCH 문서에만 적용되어야 하며(SHOULD) 생성 또는 수정되는
   리소스에 적용되어서는 안 됩니다.

   단일 기본 패치 문서 형식은 존재하지 않습니다.
 그러나
   application/json-patch+json [RFC6902]와 같은 새로운 패치 문서 형식이
   등록되어야 합니다(SHOULD).

   PATCH 요청에 대한 응답은 Expires 헤더나 Cache-Control: max-age
   지시자 같은 명시적 신선도 정보를 포함하면서, 동시에 Content-Location
   헤더 값이 Request-URI와 일치하는 경우에만 캐시할 수 있습니다.

2.1. 간단한 PATCH 예시

   PATCH /file.txt HTTP/1.1
   Host: www.example.com
   Content-Type: application/example
   If-Match: "e0023aa4e"
   Content-Length: 100

   [변경 사항 설명]

   성공적인 PATCH에 대한 응답:

   HTTP/1.1 204 No Content
   Content-Location: /file.txt
   ETag: "e0023aa4f"

   [RFC2616] 섹션 10.2.1에 정의된 대로 200 응답 코드도 사용될 수
   있습니다.

   100 바이트의 패치 문서가 파일 하나에 적용되었습니다.
 가상적인
   "application/example" 패치 형식은 패치가 완전히 적용되거나 전혀
   적용되지 않도록 의미가 정의되어 서버의 원자성 요구사항을 강제합니다.

   응답에서 ETag를 확인하여 리소스의 현재 내용 식별이 가능하며 이를
   통해 리소스의 상태를 추적하고 향후 PATCH에 사용할 수 있습니다.
 ETag를
   사용하는 경우 PATCH 응답에서 수신된 ETag 값이 리소스의 현재 상태에
   대한 유일한 신뢰할 수 있는 정보가 됩니다.

2.2. 오류 처리

   PATCH에서 발생할 수 있는 여러 알려진 조건이 있습니다:

   잘못된 형식의 패치 문서: 서버가 패치 문서가 적절하게 형식화되지
   않았다고 판단하면, 서버는 400 (Bad Request) 응답을 반환해야 합니다
   (SHOULD). "적절한 형식"의 정의는 패치 문서가 선택한 미디어 유형에
   따라 다릅니다.

   지원되지 않는 패치 문서: 클라이언트가 서버가 지원하지 않는 형식의
   패치 문서를 보낸 경우 415 (Unsupported Media Type) 응답으로
   지정할 수 있습니다.
 이러한 응답에는 [RFC2616] 섹션 3.7에 따라
   Accept-Patch 응답 헤더를 포함하여 서버가 지원하는 패치 문서 미디어
   유형을 알려야 합니다(SHOULD).

   처리할 수 없는 요청: 패치 문서의 형식은 유효하지만, 서버가 요청을
   처리할 수 없는 경우 422 (Unprocessable Entity) 응답을 사용할 수
   있습니다 ([RFC4918], 섹션 11.2).
 예를 들어, 수정이 리소스를 유효하지
   않은 상태로 만드는 경우가 있을 수 있습니다.

   리소스를 찾을 수 없음: 패치 문서를 존재하지 않는 리소스에 적용할 수
   없는 경우(예: 새 리소스 생성을 지원하지 않는 패치 문서 유형에서),
   서버는 404 (Not Found) 상태 코드로 응답해야 합니다(SHOULD).

   충돌 상태: 리소스의 현재 상태로 인해 요청이 적용될 수 없는 특정
   상황(예: 삭제하라는 부분이 이미 리소스에 없는 경우처럼 패치에 의해
   가정된 구조가 존재하지 않음)에서 서버는 409 (Conflict) 상태 코드로
   표시해야 합니다(SHOULD).

   동시 수정 충돌: 서버가 동일한 리소스에 대해 동시에 두 개의 PATCH
   요청(또는 PATCH 요청과 PUT/POST)을 수신하고 큐에 넣을 수 없는 경우
   409 (Conflict) 오류 응답을 사용해야 합니다(SHOULD).

   전제조건 실패: 조건부 요청의 전제조건이 실패한 경우 412
   (Precondition Failed) 응답을 통해 표시해야 합니다(SHOULD).

   위 오류 목록은 완전하지 않습니다 -- 서버는 적절한 경우 다른 상태
   코드를 사용해야 합니다(MUST).
 HTTP 오류 응답 본문은 클라이언트가
   실패한 요청에 대한 후속 조치를 취하기에 충분한 정보를 포함해야
   합니다(SHOULD).

   클라이언트는 실패한 요청을 처리하기 위해 GET 요청을 발행하여
   리소스의 현재 상태를 가져올 수 있습니다.
 예를 들어, 위의 409 Conflict
   응답에 대해 클라이언트는 리소스의 현재 사본을 가져와서 오류가
   포함된 차이점 보고서를 사용하여 새 패치를 구성하거나 리소스의 현재
   사본에 대해 변환하여 패치를 재구성할 수 있습니다.

3. OPTIONS에서 지원 알림

   서버는 특정 리소스에 대한 OPTIONS 응답의 Allow 헤더에 PATCH를
   포함하여 PATCH를 지원함을 알릴 수 있습니다.
 OPTIONS의 Accept-Patch
   헤더가 있으면 PATCH가 허용됨을 나타내므로 Allow 헤더를 사실상
   불필요하게 만들지만, 하위 호환성을 위해 PATCH를 지원할 때 Allow
   헤더에 포함되어야 합니다(SHOULD).

3.1. Accept-Patch 헤더

   이 명세는 서버가 수락하는 패치 문서 미디어 유형을 지정하기 위한 새로운
   응답 헤더 Accept-Patch를 도입합니다.
 Accept-Patch는 반드시 PATCH를
   지원하는 모든 리소스에 대한 OPTIONS 응답에 나타나야 합니다(MUST).
   OPTIONS 응답에 이 헤더가 있으면 해당 리소스에 PATCH가 허용됨을
   암시합니다.
 이 헤더가 없다고 해서 PATCH가 허용되지 않음을 의미하지는
   않습니다.
 왜냐하면 Accept-Patch에 대한 지원이 필수가 아닐 수
   있기 때문입니다.

   Accept-Patch의 구문은 다음과 같습니다:

     Accept-Patch = "Accept-Patch" ":" 1#media-type

   media-type의 구문은 [RFC2616] 섹션 3.7에 정의되어 있습니다.

3.2. OPTIONS 요청 및 응답 예시

   OPTIONS /example/buddies.xml HTTP/1.1
   Host: www.example.com

   위에 대한 예시 응답에서 PATCH가 지원됨을 나타냅니다:

   HTTP/1.1 200 OK
   Allow: GET, PUT, POST, OPTIONS, HEAD, DELETE, PATCH
   Accept-Patch: application/example, text/example

   실제 패치 문서 형식은 RFC에 의해 정의되지 않으며, 두 개의 가상
   패치 문서 형식이 예시를 위해 사용되었습니다.

4. IANA 고려사항

4.1. Accept-Patch 응답 헤더 등록

   Accept-Patch 응답 헤더가 [RFC3864]에 정의된 영구 등록소에
   추가되었습니다.

      헤더 필드 이름: Accept-Patch

      적용 프로토콜: HTTP

      저자/변경 관리자: IETF

      명세 문서: 이 명세

5. 보안 고려사항

   PATCH 요청에 대한 보안 고려사항은 PUT([RFC2616], 섹션 9.6)에 대한
   것과 거의 동일합니다.
 여기에는 접근 제어 및 인증을 통한 요청 인가,
   우발적인 덮어쓰기에 대한 보호, 그리고 전송 오류에 대한 데이터
   무결성 보호가 포함됩니다.

   PUT을 사용하여 완전히 교체된 문서와 비교할 때, 패치 문서를 사용하여
   패치된 문서가 손상될 수 있다는 우려가 있을 수 있습니다.
 이것이
   위험이라면 ETag와 If-Match 요청 헤더를 사용한 조건부 요청을 사용하여
   완화할 수 있습니다.
 PATCH가 실패한 경우, 클라이언트는 리소스가
   올바른 상태에 있는지 확인하기 위해 GET 요청을 발행할 수 있습니다.
   어떤 경우에는, PATCH 응답이 수신되기 전에 전송 연결이 실패하면
   클라이언트는 캐시를 우회하는 GET 요청을 발행하여 애플리케이션
   상태를 확인할 수 있습니다.

   악성 콘텐츠를 검사하는 중개자 (예: 바이러스를 검사하는 HTTP 중개자)는
   PATCH 작업에 특별한 어려움이 있습니다.
 소스 문서도, 패치 문서도
   개별적으로는 악성이 아닐 수 있지만, 서버가 패치를 적용한 결과
   악성인 리소스를 생성할 수 있습니다.
 바이트 범위 다운로드, 패치 문서,
   압축 파일 업로드 및 유사한 메커니즘에서 이 우려는 기존의 위험과
   유사합니다.
 이러한 유형의 콘텐츠 포함을 탐지하는 중개자는 이 특정
   우려에 대해 개별 데이터 조각보다는 조립된 콘텐츠를 확인하는 방식
   외에는 할 수 있는 것이 없습니다.

   개별 패치 문서는 수정될 리소스의 유형에 따라 고유한 보안 고려사항을
   가질 수 있습니다.
 서버는 악의적인 클라이언트가 PATCH 작업을
   통해 과도한 서버 리소스(예: CPU, 메모리, 디스크 I/O)를 소비하지
   않도록 적절한 예방 조치를 취해야 합니다(MUST).

6. 참조

6.1. 규범적 참조

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119, March 1997.

   [RFC2616]  Fielding, R., Gettys, J., Mogul, J., Frystyk, H.,
              Masinter, L., Leach, P., and T. Berners-Lee, "Hypertext
              Transfer Protocol -- HTTP/1.1", RFC 2616, June 1999.

   [RFC3864]  Klyne, G., Nottingham, M., and J. Mogul, "Registration
              Procedures for Message Header Fields", BCP 90, RFC 3864,
              September 2004.

6.2. 정보적 참조

   [RFC4918]  Dusseault, L., "HTTP Extensions for Web Distributed
              Authoring and Versioning (WebDAV)", RFC 4918, June 2007.

부록 A. 감사의 말

   PATCH는 Roy Fielding과 Henrik Frystyk이 작성한 HTTP/1.1 초안의
   이전 버전에 포함되었습니다; 이를 인용한 문서는 RFC 2068입니다.

   이 문서에 대해 검토하고 제안해 주신 분들께 감사드립니다: Adam Roach,
   Chris Sharp, Julian Reschke, Geoff Clemm, Scott Lawrence, Jeffrey
   Mogul, Roy Fielding, Greg Stein, Jim Luther, Alex Rousskov, Jamie
   Lokier, Joe Hildebrand, Mark Nottingham, Michael Balloni, Cyrus
   Daboo, Brian Carpenter, John Klensin, Eliot Lear, SM, 그리고 Bernie
   Hoeneisen.

   특히, Julian Reschke는 반복적인 검토, 논의, 그리고 출판에 대한
   조언으로 크게 기여했습니다.

저자 주소

   Lisa Dusseault
   Linden Lab

   EMail: lisa.dusseault@gmail.com


   James M. Snell

   EMail: jasnell@gmail.com
   URI:   http://www.snellspace.com

---

Internet Engineering Task Force (IETF)                     M. Nottingham
Request for Comments: 6585                                     Rackspace
Updates: 2616                                               R. Fielding
Category: Standards Track                                          Adobe
ISSN: 2070-1721                                            2012년 4월


                     추가 HTTP 상태 코드

요약

   이 문서는 다양한 일반적인 상황에 대한 추가 HyperText Transfer
   Protocol (HTTP) 상태 코드를 명시한다.

이 메모의 상태

   이 문서는 Internet Standards Track 문서이다.
 이 문서는 Internet
   Engineering Task Force (IETF)의 산출물이다.
 이 문서는 IETF 커뮤니티의
   합의를 나타낸다.
 이 문서는 공개 검토를 받았으며 Internet Engineering
   Steering Group (IESG)에 의해 발행이 승인되었다.
 Internet Standards에
   대한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는 방법에
   대한 정보는 http://www.rfc-editor.org/info/rfc6585 에서 확인할 수 있다.

저작권 고지

   Copyright (c) 2012 IETF Trust 및 문서 작성자로 식별된 사람들.
 모든
   권리 보유.

   이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF 문서에 관한 IETF
   Trust의 법적 조항(http://trustee.ietf.org/license-info)의 적용을
   받는다.
 이 문서에 대한 귀하의 권리와 제한 사항이 기술되어 있으므로
   이러한 문서를 주의 깊게 검토하시기 바란다.
 이 문서에서 추출된 코드
   구성요소는 Trust 법적 조항의 섹션 4.e에 기술된 대로 Simplified BSD
   License 텍스트를 포함해야 하며, Simplified BSD License에 기술된 대로
   보증 없이 제공된다.

목차

   1. 소개
   2. 요구사항
   3. 428 Precondition Required
   4. 429 Too Many Requests
   5. 431 Request Header Fields Too Large
   6. 511 Network Authentication Required
      6.1. 511 상태 코드와 Captive Portal
   7. 보안 고려사항
      7.1. 428 Precondition Required
      7.2. 429 Too Many Requests
      7.3. 431 Request Header Fields Too Large
      7.4. 511 Network Authentication Required
   8. IANA 고려사항
   9. 참고 문헌
      9.1. 규범적 참고 문헌
      9.2. 정보적 참고 문헌
   부록 A. 감사의 글
   부록 B. Captive Portal에 의해 제기되는 문제점

1. 소개

   이 문서는 다양한 일반적인 상황에 대한 추가 HTTP [RFC2616] 상태 코드를
   명시하며, 상호운용성을 개선하고 다른 덜 정확한 상태 코드가 사용될 때
   발생하는 혼란을 방지하기 위한 것이다.

   이 상태 코드는 선택적이다; 서버가 이를 지원해야 할 요구사항은 없다.
   그러나, 이러한 상태 코드는 클라이언트가 인식하지 못하는 상태 코드를
   해당 클래스의 일반적인 상태 코드로 처리하도록 되어 있으므로(예: 499를
   400으로 처리) 안전하게 배포할 수 있다.

2. 요구사항

   이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL",
   "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY",
   그리고 "OPTIONAL"은 [RFC2119]에 기술된 대로 해석되어야 한다.

3. 428 Precondition Required

   428 상태 코드는 origin 서버가 요청에 조건부 처리를 요구함을 나타낸다.

   이 코드의 일반적인 사용 목적은 클라이언트가 리소스의 상태를 GET하고,
   이를 수정한 후 서버에 PUT으로 다시 보내는 사이에 제3자가 서버의 상태를
   수정하여 충돌이 발생하는 "lost update" 문제를 방지하기 위한 것이다.
   요청을 조건부로 만들도록 요구함으로써 서버는 클라이언트가 리소스의
   현재 사본을 기반으로 작업하고 있음을 보장할 수 있다.

   이 상태 코드를 생성하는 응답은 요청을 성공적으로 재제출하는 방법을
   설명해야 한다(SHOULD).

   응답에는 다음과 같은 것을 포함할 수 있다:

   HTTP/1.1 428 Precondition Required
   Content-Type: text/html

   <html>
      <head>
         <title>Precondition Required</title>
      </head>
      <body>
         <h1>Precondition Required</h1>
         <p>This request is required to be conditional;
         try using "If-Match".</p>
      </body>
   </html>

   428 상태 코드를 가진 응답은 캐시에 저장되어서는 안 된다(MUST NOT).

4. 429 Too Many Requests

   429 상태 코드는 사용자가 주어진 시간 동안 너무 많은 요청을 보냈음을
   나타낸다("rate limiting").

   응답 표현은 조건을 설명하는 세부 사항을 포함해야 하며(SHOULD), 새
   요청을 하기 전에 얼마나 기다려야 하는지를 나타내는 Retry-After 헤더를
   포함할 수 있다(MAY).

   예를 들어:

   HTTP/1.1 429 Too Many Requests
   Content-Type: text/html
   Retry-After: 3600

   <html>
      <head>
         <title>Too Many Requests</title>
      </head>
      <body>
         <h1>Too Many Requests</h1>
         <p>I only allow 50 requests per hour to this Web site per
            logged in user.  Try again soon.</p>
      </body>
   </html>

   이 명세는 사용자가 어떻게 식별되는지 또는 요청이 어떻게 계산되는지에
   대한 메커니즘을 정의하지 않는다는 점에 유의하라.
 예를 들어, origin
   서버가 리소스별, 전체 서버에 대해, 또는 서버 집합에 대해 요청을 제한하는
   것은 이 명세의 범위 밖이다.
 마찬가지로, 이 목적을 위해 사용자를
   인증 자격 증명으로 식별하든 상태 기반 쿠키로 식별하든 이 명세의 범위
   밖이다.

   429 상태 코드를 가진 응답은 캐시에 저장되어서는 안 된다(MUST NOT).

5. 431 Request Header Fields Too Large

   431 상태 코드는 서버가 요청의 헤더 필드가 너무 크기 때문에 요청을
   처리할 의사가 없음을 나타낸다.
 요청 헤더 필드의 크기를 줄인 후 요청을
   다시 제출할 수 있다(MAY).

   이 코드는 전체 요청 헤더 필드 집합이 너무 클 때와 단일 헤더 필드에
   문제가 있을 때 모두 사용할 수 있다.
 후자의 경우, 응답 표현은 어떤
   헤더 필드가 너무 컸는지를 명시해야 한다(SHOULD).

   예를 들어:

   HTTP/1.1 431 Request Header Fields Too Large
   Content-Type: text/html

   <html>
      <head>
         <title>Request Header Fields Too Large</title>
      </head>
      <body>
         <h1>Request Header Fields Too Large</h1>
         <p>The "Example" header was too large.</p>
      </body>
   </html>

   431 상태 코드를 가진 응답은 캐시에 저장되어서는 안 된다(MUST NOT).

6. 511 Network Authentication Required

   511 상태 코드는 클라이언트가 네트워크 접근 권한을 얻기 위해 인증해야
   함을 나타낸다.

   응답 표현은 사용자가 자격 증명을 제출할 수 있는 리소스에 대한 링크를
   포함해야 한다(SHOULD) (예: HTML 양식).

   511 응답에는 challenge나 로그인 인터페이스 자체를 포함해서는 안 된다
   (SHOULD NOT).
 브라우저가 로그인 인터페이스를 원래 요청한 URL과 연관된
   것으로 표시하여 혼란을 야기할 수 있기 때문이다.

   511 상태는 origin 서버에 의해 생성되어서는 안 된다(SHOULD NOT); 이
   코드는 네트워크 접근을 제어하는 수단으로 중간에 개입하는 가로채기
   프록시(intercepting proxy)가 사용하도록 의도된 것이다.

   511 상태 코드를 가진 응답은 캐시에 저장되어서는 안 된다(MUST NOT).

6.1. 511 상태 코드와 Captive Portal

   511 상태 코드는 요청이 전달된 서버가 아닌 중간 네트워크 인프라로부터의
   응답을 기대하는 소프트웨어(특히 비브라우저 에이전트)에 "captive portal"
   이 야기하는 문제를 완화하기 위해 설계되었다.
 이 코드는 captive portal의
   배포를 장려하기 위한 것이 아니라 -- 이로 인한 피해를 제한하기 위한
   것이다.

   인증, 이용약관 동의, 또는 기타 사용자 상호작용을 요구한 후 접근 권한을
   부여하고자 하는 네트워크 운영자는 일반적으로 이를 수행하지 않은
   클라이언트("unknown client")를 해당 Media Access Control (MAC) 주소를
   사용하여 식별한다.

   unknown client의 모든 트래픽은 차단되며, TCP 포트 80의 트래픽만 예외로
   "unknown client"의 "로그인"을 전담하는 HTTP 서버("login server")로
   전송되고, 물론 login server 자체로의 트래픽도 허용된다.

   예를 들어, 사용자 에이전트가 네트워크에 연결하여 TCP 포트 80에서
   다음과 같은 HTTP 요청을 할 수 있다:

   GET /index.htm HTTP/1.1
   Host: www.example.com

   이러한 요청을 수신하면, login server는 511 응답을 생성한다:

   HTTP/1.1 511 Network Authentication Required
   Content-Type: text/html

   <html>
      <head>
         <title>Network Authentication Required</title>
         <meta http-equiv="refresh"
               content="0; url=https://login.example.net/">
      </head>
      <body>
         <p>You need to <a href="https://login.example.net/">
         authenticate with the local network</a> in order to gain
         access.</p>
      </body>
   </html>

   여기서 511 상태 코드는 비브라우저 클라이언트가 이 응답을 origin 서버의
   응답으로 해석하지 않도록 보장하며, META HTML 요소는 사용자 에이전트를
   login server로 리다이렉트한다.

7. 보안 고려사항

7.1. 428 Precondition Required

   428 상태 코드는 선택적이다; 클라이언트는 "lost update" 충돌을 방지하기
   위해 이 코드의 사용에 의존할 수 없다.

7.2. 429 Too Many Requests

   서버가 공격을 받고 있거나 단일 당사자로부터 매우 많은 수의 요청을 받고
   있을 때, 각 요청에 429 상태 코드로 응답하는 것은 리소스를 소비한다.

   따라서 서버는 429 상태 코드를 사용할 필요가 없다; 리소스 사용을 제한할
   때 연결을 끊거나 다른 조치를 취하는 것이 더 적절할 수 있다.

7.3. 431 Request Header Fields Too Large

   서버는 431 상태 코드를 사용할 필요가 없다; 공격을 받고 있을 때 연결을
   끊거나 다른 조치를 취하는 것이 더 적절할 수 있다.

7.4. 511 Network Authentication Required

   일반적인 사용에서, 511 상태 코드를 전달하는 응답은 요청 URL에 표시된
   origin 서버에서 오지 않을 것이다.
 이는 많은 보안 문제를 야기한다;
   예를 들어, 공격하는 중개자가 원래 도메인의 이름 공간에 쿠키를 삽입하고
   있을 수 있고, 사용자 에이전트에서 보낸 쿠키나 HTTP 인증 자격 증명을
   관찰하고 있을 수 있는 등의 문제가 있다.

   그러나 이러한 위험은 511 상태 코드에 고유한 것이 아니다; 다시 말해,
   이 상태 코드를 사용하지 않는 captive portal도 동일한 문제를 야기한다.

   또한, Secure Socket Layer (SSL) 또는 Transport Layer Security (TLS)
   연결(일반적으로 포트 443)에서 이 상태 코드를 사용하는 captive portal은
   클라이언트에서 인증서 오류를 생성할 것이라는 점에 유의하라.

8. IANA 고려사항

   HTTP 상태 코드 레지스트리가 다음 항목으로 업데이트되었다:

      값:  428
      설명:  Precondition Required
      참조:  [RFC6585]

      값:  429
      설명:  Too Many Requests
      참조:  [RFC6585]

      값:  431
      설명:  Request Header Fields Too Large
      참조:  [RFC6585]

      값:  511
      설명:  Network Authentication Required
      참조:  [RFC6585]

9. 참고 문헌

9.1. 규범적 참고 문헌

   [RFC2119]    Bradner, S., "Key words for use in RFCs to Indicate
                Requirement Levels", BCP 14, RFC 2119, March 1997.

   [RFC2616]    Fielding, R., Gettys, J., Mogul, J., Frystyk, H.,
                Masinter, L., Leach, P., and T. Berners-Lee,
                "Hypertext Transfer Protocol -- HTTP/1.1", RFC 2616,
                June 1999.

9.2. 정보적 참고 문헌

   [CORS]       van Kesteren, A., Ed., "Cross-Origin Resource Sharing",
                W3C Working Draft WD-cors-20100727, July 2010,
                <http://www.w3.org/TR/cors/>.

   [Favicon]    Wikipedia, "Favicon", March 2012,
                <http://en.wikipedia.org/w/
                index.php?title=Favicon&oldid=484627550>.

   [OAuth2.0]   Hammer-Lahav, E., Ed., Recordon, D., and D. Hardt,
                "The OAuth 2.0 Authorization Protocol", Work
                in Progress, March 2012.

   [P3P]        Marchiori, M., Ed., "The Platform for Privacy
                Preferences 1.0 (P3P1.0) Specification", W3C
                Recommendation REC-P3P-20020416, April 2002,
                <http://www.w3.org/TR/P3P/>.

   [RFC4791]    Daboo, C., Desruisseaux, B., and L. Dusseault,
                "Calendaring Extensions to WebDAV (CalDAV)", RFC 4791,
                March 2007.

   [RFC4918]    Dusseault, L., Ed., "HTTP Extensions for Web
                Distributed Authoring and Versioning (WebDAV)",
                RFC 4918, June 2007.

   [WIDGETS]    Caceres, M., Ed., "Widget Packaging and XML
                Configuration", W3C Recommendation
                REC-widgets-20110927, September 2011,
                <http://www.w3.org/TR/widgets/>.

   [WebFinger]  WebFinger Project, "WebFingerProtocol (Draft)",
                January 2010, <http://code.google.com/p/webfinger/wiki/
                WebFingerProtocol>.

부록 A. 감사의 글

   제안과 피드백을 주신 Jan Algermissen과 Julian Reschke에게 감사드린다.

부록 B. Captive Portal에 의해 제기되는 문제점

   클라이언트가 portal의 응답과 통신하려고 의도했던 HTTP 서버의 응답을
   구별할 수 없기 때문에 여러 가지 문제가 발생한다. 511 상태 코드는
   이러한 문제 중 일부를 완화하기 위한 것이다.

   한 가지 예는 브라우저가 접근 중인 사이트를 식별하기 위해 일반적으로
   사용하는 "favicon.ico" [Favicon]이다.
 주어진 사이트의 favicon이 의도된
   사이트가 아닌 captive portal에서 가져오면(예: 사용자가 인증되지
   않았기 때문에), 이 favicon은 종종 portal 세션 이후에도 브라우저의
   캐시에 "고착"되어(대부분의 구현은 favicon을 적극적으로 캐시함) portal의
   favicon이 정당한 사이트를 "접수"한 것처럼 보이게 된다.

   또 다른 브라우저 기반 문제는 Platform for Privacy Preferences [P3P]가
   지원될 때 발생한다.
 구현 방식에 따라, 브라우저가 p3p.xml 파일에 대한
   portal의 응답을 서버의 응답으로 해석하여, portal이 광고하는 개인정보
   보호 정책(또는 그 부재)이 의도된 사이트에 적용되는 것으로 해석될 수
   있다.
 WebFinger [WebFinger], Cross-Origin Resource Sharing [CORS],
   그리고 Open Authorization [OAuth2.0]과 같은 다른 웹 기반 프로토콜도
   이러한 문제에 취약할 수 있다.

   HTTP가 웹 브라우저에서 가장 널리 사용되지만, 점점 더 많은 비브라우저
   애플리케이션이 HTTP를 기반 프로토콜로 사용하고 있다.
 예를 들어, Web
   Distributed Authoring and Versioning (WebDAV) [RFC4918]과 Calendaring
   Extensions to WebDAV (CalDAV) [RFC4791]은 모두 HTTP를 기반으로
   사용한다(각각 원격 저작 및 일정 관리 용도). captive portal 뒤에서
   이러한 애플리케이션을 사용하면 사용자에게 가짜 오류가 표시될 수 있으며,
   극단적인 경우 콘텐츠 손상이 발생할 수 있다.

   마찬가지로 HTTP를 사용하는 다른 비브라우저 애플리케이션도 영향을 받을
   수 있다.
 예를 들어, 위젯 [WIDGETS], 소프트웨어 업데이트, 그리고
   Twitter 클라이언트 및 iTunes Music Store와 같은 기타 전문화된
   소프트웨어가 해당된다.

   HTTP 리다이렉션을 사용하여 트래픽을 portal로 보내면 이러한 문제를
   해결한다고 믿어지는 경우가 있다는 점에 유의해야 한다.
 그러나 이러한
   용도의 대부분이 리다이렉트를 "따르기" 때문에 이는 좋은 해결책이
   아니다.

저자 주소

   Mark Nottingham
   Rackspace

   EMail: mnot@mnot.net
   URI:   http://www.mnot.net/


   Roy T. Fielding
   Adobe Systems Incorporated
   345 Park Ave.
   San Jose, CA  95110
   USA

   EMail: fielding@gbiv.com
   URI:   http://roy.gbiv.com/

---

Internet Engineering Task Force (IETF)                    M. Nottingham
Request for Comments: 9457                                      E. Wilde
Obsoletes: 7807                                                 S. Dalal
Category: Standards Track                                      July 2023
ISSN: 2070-1721


                   HTTP API를 위한 Problem Details

#### Abstract

   이 문서는 HTTP API를 위한 새로운 오류 응답 형식을 정의할 필요 없이,
   HTTP 응답 콘텐츠에 기계 판독 가능한 오류 상세 정보를 전달하기 위한
   "problem detail"을 정의한다.

   이 문서는 RFC 7807을 대체한다.

이 메모의 상태

   이 문서는 Internet Standards Track 문서이다.

   이 문서는 Internet Engineering Task Force (IETF)의 산출물이다.
   이 문서는 IETF 커뮤니티의 합의를 나타낸다.
 이 문서는 공개 검토를
   받았으며 Internet Engineering Steering Group (IESG)에 의해 출판이
   승인되었다.
 Internet Standards에 대한 추가 정보는 RFC 7841의
   섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 피드백을 제공하는 방법에 대한
   정보는 https://www.rfc-editor.org/info/rfc9457 에서 확인할 수 있다.

저작권 고지

   Copyright (c) 2023 IETF Trust and the persons identified as the
   document authors. All rights reserved.

   이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF 문서와 관련된
   IETF Trust의 법적 조항(https://trustee.ietf.org/license-info)의
   적용을 받는다.
 이 문서에 대한 귀하의 권리와 제한 사항이 설명되어
   있으므로 이 문서들을 주의 깊게 검토하기 바란다.
 이 문서에서 추출된
   코드 구성 요소는 Trust Legal Provisions의 섹션 4.e에 기술된 대로
   Revised BSD License 텍스트를 포함해야 하며, Revised BSD License에
   기술된 대로 보증 없이 제공된다.

목차

   1.  소개
   2.  요구사항 언어
   3.  Problem Details JSON 객체
       3.1.  Problem Details 객체의 멤버
           3.1.1.  "type"
           3.1.2.  "status"
           3.1.3.  "title"
           3.1.4.  "detail"
           3.1.5.  "instance"
       3.2.  확장 멤버
   4.  새로운 Problem Type 정의
       4.1.  예시
       4.2.  등록된 Problem Type
           4.2.1.  about:blank
   5.  보안 고려사항
   6.  IANA 고려사항
   7.  참조
       7.1.  규범적 참조
       7.2.  참고적 참조
   부록 A.  HTTP Problem을 위한 JSON Schema
   부록 B.  HTTP Problem과 XML
   부록 C.  다른 형식과 함께 Problem Details 사용
   부록 D.  RFC 7807로부터의 변경사항
   감사의 글
   저자 주소

1.  소개

   RFC 9110의 섹션 15에 정의된 HTTP 상태 코드만으로는 항상 오류에 대한
   충분한 정보를 전달하여 도움이 되기 어렵다.
 웹 브라우저를 사용하는
   사람은 HTML 응답 콘텐츠를 이해할 수 있는 경우가 많지만, HTTP API의
   비인간 소비자는 그렇게 하기 어렵다.

   이 결함을 해결하기 위해, 이 명세는 발생한 문제의 구체적인 내용을
   설명하는 간단한 JSON 및 XML 문서 형식을 정의한다 -- "problem
   details".

   예를 들어, 클라이언트의 계정에 충분한 크레딧이 없다는 응답을
   생각해 보자.
 API 설계자는 403 Forbidden 상태 코드를 사용하여 일반
   HTTP 소프트웨어(예: 클라이언트 라이브러리, 캐시, 프록시)에게 응답의
   일반적인 의미를 알릴 수 있다.
 API 특정 problem details(예: 서버가
   요청을 거부한 이유 및 해당 계정 잔액)는 응답 콘텐츠에 포함되어
   클라이언트가 적절하게 행동할 수 있도록 한다(예를 들어, 계정에 추가
   크레딧 이체를 트리거).

   이 명세는 특정 "problem type"(예: "크레딧 부족")을 URI로 식별한다.
   HTTP API는 자체 제어 하의 URI를 사용하여 자신에게 특정한 문제를
   식별하거나, 상호 운용성을 촉진하고 공통 의미를 활용하기 위해 기존
   URI를 재사용할 수 있다(섹션 4.2 참조).

   Problem details는 문제의 특정 발생을 식별하는 URI(사실상 "지난
   목요일에 Joe에게 크레딧이 부족했던 그 때"라는 개념에 식별자를
   부여하는 것)와 같은 기타 정보를 포함할 수 있으며, 이는 지원 또는
   포렌식 목적에 유용할 수 있다.

   Problem details의 데이터 모델은 JSON 객체이다; JSON 문서로 직렬화될
   때, "application/problem+json" 미디어 타입을 사용한다.
 부록 B는
   동등한 XML 형식을 정의하며, "application/problem+xml" 미디어 타입을
   사용한다.

   HTTP 응답으로 전달될 때, problem details의 콘텐츠는 사전 협상
   (proactive negotiation)을 사용하여 협상될 수 있다; RFC 9110의
   섹션 12.1을 참조하라.
 특히, 사람이 읽을 수 있는 문자열(예: title
   및 description에 포함된 것)에 사용되는 언어는 Accept-Language 요청
   헤더 필드(RFC 9110의 섹션 12.5.4)를 사용하여 협상될 수 있지만,
   그 협상의 결과가 여전히 선호되지 않는 기본 표현이 반환될 수 있다.

   Problem details는 모든 HTTP 상태 코드와 함께 사용될 수 있지만,
   4xx 및 5xx 응답의 의미에 가장 자연스럽게 부합한다.
 Problem details는
   (당연히) HTTP에서 문제의 상세 정보를 전달하는 유일한 방법이 아님에
   유의하라.
 예를 들어, 응답이 여전히 리소스의 표현인 경우, 해당
   애플리케이션의 형식으로 관련 상세 정보를 설명하는 것이 더 바람직한
   경우가 많다.
 마찬가지로, 정의된 HTTP 상태 코드는 추가 상세 정보를
   전달할 필요 없이 많은 상황을 다룬다.

   이 명세의 목적은 오류 형식이 필요한 애플리케이션을 위한 공통 오류
   형식을 정의하여, 자체적으로 정의하지 않아도 되고, 더 나아가 기존
   HTTP 상태 코드의 의미를 재정의하려는 유혹을 피하도록 하는 것이다.
   애플리케이션이 오류를 전달하기 위해 이를 사용하지 않기로 선택하더라도,
   이 설계를 검토하면 기존 형식으로 오류를 전달할 때 직면하는 설계 결정에
   도움이 될 수 있다.

   RFC 7807로부터의 변경사항 목록은 부록 D를 참조하라.

2.  요구사항 언어

   이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL",
   "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED",
   "NOT RECOMMENDED", "MAY", "OPTIONAL"은 여기에 표시된 것처럼
   모두 대문자로 나타날 때, 그리고 그 경우에만, BCP 14 [RFC2119]
   [RFC8174]에 기술된 대로 해석되어야 한다.

3.  Problem Details JSON 객체

   Problem details의 표준 모델은 JSON 객체이다.
 JSON 문서로 직렬화될
   때, 해당 형식은 "application/problem+json" 미디어 타입으로
   식별된다.

   예를 들어:

   POST /purchase HTTP/1.1
   Host: store.example.com
   Content-Type: application/json
   Accept: application/json, application/problem+json

   {
     "item": 123456,
     "quantity": 2
   }

   HTTP/1.1 403 Forbidden
   Content-Type: application/problem+json
   Content-Language: en

   {
    "type": "https://example.com/probs/out-of-credit",
    "title": "You do not have enough credit.",
    "detail": "Your current balance is 30, but that costs 50.",
    "instance": "/account/12345/msgs/abc",
    "balance": 30,
    "accounts": ["/account/12345",
                 "/account/67890"]
   }

   여기서, 크레딧 부족 문제(type으로 식별됨)는 "title"에서 403의
   이유를 나타내고, "instance"로 특정 문제 발생을 식별하며, "detail"에
   발생별 상세 정보를 제공하고, 두 개의 확장을 추가한다: "balance"는
   계정의 잔액을 전달하고, "accounts"는 계정을 충전할 수 있는 링크를
   나열한다.

   이를 수용하도록 설계된 경우, 문제별 확장은 동일한 problem type의
   둘 이상의 인스턴스를 전달할 수 있다.
 예를 들어:

   POST /details HTTP/1.1
   Host: account.example.com
   Accept: application/json

   {
     "age": 42.3,
     "profile": {
       "color": "yellow"
     }
   }

   HTTP/1.1 422 Unprocessable Content
   Content-Type: application/problem+json
   Content-Language: en

   {
    "type": "https://example.net/validation-error",
    "title": "Your request is not valid.",
    "errors": [
                {
                  "detail": "must be a positive integer",
                  "pointer": "#/age"
                },
                {
                  "detail": "must be 'green', 'red' or 'blue'",
                  "pointer": "#/profile/color"
                }
             ]
   }

   여기서의 가상 problem type은 "errors" 확장을 정의하는데, 이는 각
   유효성 검사 오류의 상세 정보를 설명하는 배열이다.
 각 멤버는 문제를
   설명하는 "detail"과 JSON Pointer를 사용하여 요청 콘텐츠 내의 문제
   위치를 가리키는 "pointer"를 포함하는 객체이다.

   API가 동일한 type을 공유하지 않는 여러 문제를 만나는 경우, 가장
   관련성이 높거나 긴급한 문제를 응답에 표현하는 것이 권장된다
   (RECOMMENDED).
 여러 이질적인 type을 전달하는 일반적인 "배치"
   problem type을 만드는 것이 가능하지만, 이는 HTTP 의미론에 잘
   매핑되지 않는다.

   또한 API가 클라이언트가 Accept에 나열하지 않았음에도
   "application/problem+json" 타입으로 응답했음에 유의하라.
   이는 HTTP에서 허용된다(RFC 9110의 섹션 12.5.1 참조).

3.1.  Problem Details 객체의 멤버

   Problem detail 객체는 다음 멤버를 가질 수 있다.
 멤버의 값 타입이
   지정된 타입과 일치하지 않는 경우, 해당 멤버는 무시되어야 한다
   (MUST) -- 즉, 마치 해당 멤버가 존재하지 않는 것처럼 처리가
   계속된다.

3.1.1.  "type"

   "type" 멤버는 problem type을 식별하는 URI 참조를 포함하는 JSON
   문자열이다.
 소비자는 "type" URI(필요한 경우 해석 후)를 problem
   type의 주요 식별자로 사용해야 한다(MUST).

   이 멤버가 존재하지 않을 때, 그 값은 "about:blank"으로 간주된다.

   type URI가 로케이터(예: "http" 또는 "https" 스키마를 가진 것)인
   경우, 이를 역참조하면 해당 problem type에 대한 사람이 읽을 수 있는
   문서를 제공해야 한다(SHOULD)(예: HTML 사용).
 그러나, 소비자는
   개발자에게 정보를 제공할 때(예: 디버깅 도구가 사용 중인 경우)를
   제외하고는 type URI를 자동으로 역참조해서는 안 된다(SHOULD NOT).

   "type"에 상대 URI가 포함된 경우, RFC 3986의 섹션 5에 따라 문서의
   기본 URI를 기준으로 해석된다.
 그러나, 상대 URI를 사용하면 혼란을
   야기할 수 있으며, 모든 구현에서 올바르게 처리되지 않을 수 있다.

   예를 들어, 두 리소스 "https://api.example.org/foo/bar/123"과
   "https://api.example.org/widget/456"이 모두 상대 URI 참조
   "example-problem"과 동일한 "type"으로 응답하는 경우, 해석 시
   서로 다른 리소스를 식별하게 된다
   ("https://api.example.org/foo/bar/example-problem" 및
   "https://api.example.org/widget/example-problem").
 그 결과,
   가능한 경우 "type"에 절대 URI를 사용하는 것이 권장되며
   (RECOMMENDED), 상대 URI를 사용하는 경우 전체 경로를 포함하는
   것이 좋다(예: "/types/123").

   type URI는 해석 불가능한 URI일 수 있다.
 예를 들어, tag URI
   스키마를 사용하여 problem type을 고유하게 식별할 수 있다:

   tag:example@example.org,2021-09-17:OutOfLuck

   그러나, 이 명세는 해석 가능한 type URI를 권장한다.
 왜냐하면
   향후 URI를 해석하는 것이 바람직해질 수 있기 때문이다.
 예를 들어,
   API 설계자가 위의 URI를 사용하고 나중에 type URI를 해석하여
   오류에 대한 정보를 발견하는 도구를 도입한 경우, 해당 기능을
   활용하려면 해석 가능한 URI로 전환해야 하며, 이는 problem type에
   대한 새로운 신원을 생성하여 하위 호환성을 깨뜨리는 변경을
   초래한다.

3.1.2.  "status"

   "status" 멤버는 이 문제 발생에 대해 원본 서버가 생성한 HTTP 상태
   코드(RFC 9110, 섹션 15)를 나타내는 JSON 숫자이다.

   "status" 멤버가 존재하는 경우, 이는 자문 정보일 뿐이다; 이는
   소비자의 편의를 위해 사용된 HTTP 상태 코드를 전달한다.
 생성자는
   이 형식을 이해하지 못하는 일반 HTTP 소프트웨어도 올바르게
   동작하도록 보장하기 위해 실제 HTTP 응답에서 동일한 상태 코드를
   사용해야 한다(MUST).
 사용에 관한 추가 주의사항은 섹션 5를
   참조하라.

   소비자는 status 멤버를 사용하여 생성자가 사용한 원래 상태 코드가
   변경(예: 중개자 또는 캐시에 의해)되었을 때와 메시지 콘텐츠가 HTTP
   정보 없이 지속되었을 때 이를 판별할 수 있다.
 일반 HTTP 소프트웨어는
   여전히 HTTP 상태 코드를 사용할 것이다.

3.1.3.  "title"

   "title" 멤버는 problem type에 대한 짧은, 사람이 읽을 수 있는
   요약을 포함하는 JSON 문자열이다.

   이는 지역화(예: RFC 9110의 섹션 12.1의 사전 콘텐츠 협상 사용)를
   제외하고는 문제의 발생 간에 변경되어서는 안 된다(SHOULD NOT).

   "title" 문자열은 자문 정보이며, type URI의 의미를 알지 못하고
   발견할 수 없는 사용자(예: 오프라인 로그 분석 중)를 위해서만
   포함된다.

3.1.4.  "detail"

   "detail" 멤버는 이 문제의 특정 발생에 대한 사람이 읽을 수 있는
   설명을 포함하는 JSON 문자열이다.

   "detail" 문자열이 존재하는 경우, 디버깅 정보를 제공하는 것보다
   클라이언트가 문제를 수정하도록 돕는 데 초점을 맞추어야 한다.

   소비자는 정보를 얻기 위해 "detail" 멤버를 파싱해서는 안 된다
   (SHOULD NOT); 확장이 그러한 정보를 얻기 위한 더 적합하고 오류가
   적은 방법이다.

3.1.5.  "instance"

   "instance" 멤버는 문제의 특정 발생을 식별하는 URI 참조를 포함하는
   JSON 문자열이다.

   "instance" URI가 역참조 가능한 경우, problem details 객체를
   그로부터 가져올 수 있다.
 사전 콘텐츠 협상(RFC 9110의
   섹션 12.5.1 참조)을 사용하여 다른 형식으로 문제 발생에 대한
   정보를 반환할 수도 있다.

   "instance" URI가 역참조 불가능한 경우, 이는 서버에 의미 있을 수
   있지만 클라이언트에게는 불투명한 문제 발생의 고유 식별자로
   기능한다.

   "instance"에 상대 URI가 포함된 경우, RFC 3986의 섹션 5에 따라
   문서의 기본 URI를 기준으로 해석된다.
 그러나, 상대 URI를 사용하면
   혼란을 야기할 수 있으며, 모든 구현에서 올바르게 처리되지 않을 수
   있다.

   예를 들어, 두 리소스 "https://api.example.org/foo/bar/123"과
   "https://api.example.org/widget/456"이 모두 상대 URI 참조
   "example-instance"와 동일한 "instance"로 응답하는 경우, 해석 시
   서로 다른 리소스를 식별하게 된다
   ("https://api.example.org/foo/bar/example-instance" 및
   "https://api.example.org/widget/example-instance").
 그 결과,
   가능한 경우 "instance"에 절대 URI를 사용하는 것이 권장되며
   (RECOMMENDED), 상대 URI를 사용하는 경우 전체 경로를 포함하는
   것이 좋다(예: "/instances/123").

3.2.  확장 멤버

   Problem type 정의는 해당 problem type에 특정한 추가 멤버로 problem
   details 객체를 확장할 수 있다(MAY).

   예를 들어, 위의 크레딧 부족 문제는 두 가지 확장을 정의한다 --
   추가적인 문제별 정보를 전달하기 위한 "balance"와 "accounts".

   마찬가지로, "유효성 검사 오류" 예시는 발견된 개별 오류 발생의
   목록을 포함하는 "errors" 확장을 정의하며, 각각의 상세 정보와
   위치 포인터를 포함한다.

   Problem details를 소비하는 클라이언트는 인식하지 못하는 모든
   확장을 무시해야 한다(MUST); 이를 통해 problem type이 진화하고
   향후 추가 정보를 포함할 수 있게 된다.

   확장을 생성할 때, problem type 작성자는 이름을 신중하게 선택해야
   한다.
 XML 형식(부록 B 참조)에서 사용되려면, XML 명세의 섹션 2.3의
   Name 규칙을 준수해야 한다.

4.  새로운 Problem Type 정의

   HTTP API가 오류 조건을 나타내는 응답을 정의해야 할 때, 새로운
   problem type을 정의하는 것이 적절할 수 있다.

   그렇게 하기 전에, problem type이 무엇에 적합하고 무엇이 다른
   메커니즘에 맡기는 것이 더 나은지 이해하는 것이 중요하다.

   Problem details는 기본 구현을 위한 디버깅 도구가 아니다; 오히려,
   HTTP 인터페이스 자체에 대한 더 큰 세부 정보를 노출하는 방법이다.
   새로운 problem type의 설계자는 보안 고려사항(섹션 5)을 신중하게
   고려해야 하며, 특히 오류 메시지를 통해 구현 내부를 노출하여
   공격 벡터를 노출하는 위험을 주의해야 한다.

   마찬가지로, 진정으로 일반적인 문제 -- 즉, 웹의 모든 리소스에
   적용될 수 있는 조건 -- 는 일반적으로 일반 상태 코드로 표현하는
   것이 더 낫다.
 예를 들어, "쓰기 접근 불허" 문제는 PUT 요청에 대한
   403 Forbidden 상태 코드가 자명하므로 아마 불필요할 것이다.

   마지막으로, 애플리케이션은 이미 정의한 형식으로 오류를 전달하는
   더 적절한 방법을 가지고 있을 수 있다.
 Problem details는 새로운
   "fault" 또는 "error" 문서 형식을 수립할 필요성을 피하기 위한
   것이지, 기존 도메인별 형식을 대체하기 위한 것이 아니다.

   그렇긴 하지만, HTTP 콘텐츠 협상을 사용하여 기존 HTTP API에
   problem details에 대한 지원을 추가하는 것은 가능하다(예: Accept
   요청 헤더를 사용하여 이 형식에 대한 선호를 나타냄; HTTP,
   섹션 12.5.1 참조).

   새로운 problem type 정의는 다음을 문서화해야 한다(MUST):

   1.  type URI (일반적으로, "http" 또는 "https" 스키마)

   2.  이를 적절하게 설명하는 title (짧게 작성)

   3.  함께 사용할 HTTP 상태 코드

   Problem type 정의는 적절한 상황에서 Retry-After 응답 헤더(HTTP,
   섹션 10.2.3)의 사용을 명시할 수 있다(MAY).

   Problem type URI는 문제를 해결하는 방법을 설명하는 HTML 문서로
   해석되어야 한다(SHOULD).

   Problem type 정의는 problem details 객체에 추가 멤버를 명시할 수
   있다(MAY).
 예를 들어, 확장이 기계가 문제를 해결하는 데 사용할 수
   있는 다른 리소스에 대한 타입이 지정된 링크를 사용할 수 있다.

   그러한 추가 멤버가 정의된 경우, 그 이름은 문자(ABNF의 부록 B.1에
   따른 ALPHA)로 시작해야 하며(SHOULD), ALPHA, DIGIT(ABNF,
   부록 B.1), 그리고 "_"의 문자로 구성되어야 하고(SHOULD)(JSON 이외의
   형식으로도 직렬화될 수 있도록), 3자 이상이어야 한다(SHOULD).

4.1.  예시

   예를 들어, 온라인 쇼핑 카트에 HTTP API를 게시하는 경우, 사용자의
   크레딧이 부족하여(위의 예시) 구매를 할 수 없음을 나타내야 할 수
   있다.

   이 정보를 수용할 수 있는 애플리케이션별 형식이 이미 있다면, 그것을
   사용하는 것이 아마 가장 좋을 것이다.
 그러나, 없다면, problem
   detail 형식 중 하나를 사용할 수 있다 -- API가 JSON 기반이면 JSON,
   해당 형식을 사용한다면 XML.

   그렇게 하기 위해, 레지스트리(섹션 4.2)에서 목적에 맞는 이미
   정의된 type URI를 찾아볼 수 있다.
 사용 가능한 것이 있으면, 해당
   URI를 재사용할 수 있다.

   사용 가능한 것이 없다면, 새로운 type URI(귀하의 관리 하에 있고
   시간이 지나도 안정적이어야 하는)를 발행하고 문서화할 수 있으며,
   적절한 title과 함께 사용할 HTTP 상태 코드, 그리고 그 의미와
   처리 방법을 문서화한다.

4.2.  등록된 Problem Type

   이 명세는 재사용을 촉진하기 위해 공통적으로 널리 사용되는 problem
   type URI를 위한 "HTTP Problem Types" 레지스트리를 정의한다.

   이 레지스트리의 정책은 RFC 8126의 섹션 4.6에 따른 Specification
   Required이다.

   요청을 평가할 때, 지정된 전문가는 커뮤니티 피드백, problem type이
   얼마나 잘 정의되어 있는지, 그리고 이 명세의 요구사항을 고려해야
   한다.
 벤더별, 애플리케이션별, 배포별 값은 등록할 수 없다.
 명세
   문서는 안정적이고 자유롭게 이용 가능한 방식으로(이상적으로는 URL에
   위치하여) 게시되어야 하지만, 표준일 필요는 없다.

   등록은 type URI에 접두사 "https://iana.org/assignments/
   http-problem-types#"를 사용할 수 있다(MAY).
 이러한 URI는 해석이
   불가능할 수 있음에 유의하라.

   등록 요청에는 다음 템플릿을 사용해야 한다:

   Type URI:

      [problem type을 위한 URI]

   Title:

      [problem type에 대한 간단한 설명]

   Recommended HTTP status code:

      [해당 type과 함께 사용하기에 가장 적합한 상태 코드]

   Reference:

      [type을 정의하는 명세에 대한 참조]

   등록 요청을 보낼 곳에 대한 상세 정보는
   https://iana.org/assignments/http-problem-types 의 레지스트리를
   참조하라.

4.2.1.  about:blank

   이 명세는 하나의 Problem Type, "about:blank"을 다음과 같이
   등록한다.

   Type URI:

      about:blank

   Title:

      See HTTP Status Code

   Recommended HTTP status code:

      N/A

   Reference:

      RFC 9457

   "about:blank" URI가 problem type으로 사용될 때, 이는 문제에 HTTP
   상태 코드 이상의 추가적인 의미가 없음을 나타낸다.

   "about:blank"이 사용될 때, title은 해당 코드에 대한 권장 HTTP
   상태 구문과 동일해야 하며(SHOULD)(예: 404에 대한 "Not Found"
   등), 클라이언트 선호(Accept-Language 요청 헤더로 표현됨)에 맞게
   지역화될 수 있다(MAY).

   "type" 멤버가 정의되는 방식(섹션 3.1.1)에 따라, "about:blank"
   URI는 해당 멤버의 기본값임을 유의하라.
 따라서, 명시적 "type"
   멤버를 가지지 않는 모든 problem details 객체는 암묵적으로 이
   URI를 사용한다.

5.  보안 고려사항

   새로운 problem type을 정의할 때, 포함되는 정보를 신중하게
   검토해야 한다.
 마찬가지로, 실제로 문제를 생성할 때 -- 어떻게
   직렬화되든 -- 제공되는 상세 정보도 면밀히 검토해야 한다.

   위험에는 시스템을 손상시키거나, 시스템에 접근하거나, 시스템
   사용자의 개인정보를 침해하는 데 악용될 수 있는 정보 유출이
   포함된다.

   발생 정보에 대한 링크를 제공하는 생성자는 스택 덤프와 같은
   구현 세부사항을 HTTP 인터페이스를 통해 제공하는 것을 피하도록
   권장된다.
 이는 서버 구현, 데이터 등의 민감한 세부사항을 노출할
   수 있기 때문이다.

   "status" 멤버는 HTTP 상태 코드 자체에서 사용 가능한 정보를
   복제하여, 둘 사이의 불일치 가능성을 가져온다.
 불일치가 (예를
   들어) 중개자가 전송 중에 HTTP 상태 코드를 변경한 것(예: 프록시
   또는 캐시에 의해)을 나타낼 수 있으므로 이들의 상대적 우선순위는
   명확하지 않다.
 일반 HTTP 소프트웨어(예: 프록시, 로드 밸런서,
   방화벽, 바이러스 스캐너)는 이 멤버에 전달된 상태 코드를 알거나
   준수할 가능성이 낮다.

6.  IANA 고려사항

   "Media Types" 레지스트리 아래의 "application" 레지스트리에서,
   IANA는 이 문서를 참조하도록 "application/problem+json" 및
   "application/problem+xml" 등록을 업데이트하였다.

   IANA는 섹션 4.2에 명시된 대로 "HTTP Problem Types" 레지스트리를
   생성하였으며, 섹션 4.2.1에 따라 "about:blank"으로 채웠다.

7.  참조

7.1.  규범적 참조

   [ABNF]     Crocker, D., Ed. and P. Overell, "Augmented BNF for
              Syntax Specifications: ABNF", STD 68, RFC 5234,
              DOI 10.17487/RFC5234, January 2008,
              <https://www.rfc-editor.org/info/rfc5234>.

   [HTTP]     Fielding, R., Ed., Nottingham, M., Ed., and J. Reschke,
              Ed., "HTTP Semantics", STD 97, RFC 9110,
              DOI 10.17487/RFC9110, June 2022,
              <https://www.rfc-editor.org/info/rfc9110>.

   [JSON]     Bray, T., Ed., "The JavaScript Object Notation (JSON)
              Data Interchange Format", STD 90, RFC 8259,
              DOI 10.17487/RFC8259, December 2017,
              <https://www.rfc-editor.org/info/rfc8259>.

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119,
              DOI 10.17487/RFC2119, March 1997,
              <https://www.rfc-editor.org/info/rfc2119>.

   [RFC8126]  Cotton, M., Leiba, B., and T. Narten, "Guidelines for
              Writing an IANA Considerations Section in RFCs", BCP 26,
              RFC 8126, DOI 10.17487/RFC8126, June 2017,
              <https://www.rfc-editor.org/info/rfc8126>.

   [RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
              2119 Key Words", BCP 14, RFC 8174,
              DOI 10.17487/RFC8174, May 2017,
              <https://www.rfc-editor.org/info/rfc8174>.

   [URI]      Berners-Lee, T., Fielding, R., and L. Masinter, "Uniform
              Resource Identifier (URI): Generic Syntax", STD 66,
              RFC 3986, DOI 10.17487/RFC3986, January 2005,
              <https://www.rfc-editor.org/info/rfc3986>.

   [XML]      Bray, T., Paoli, J., Sperberg-McQueen, C. M., Maler, E.,
              and F. Yergeau, "Extensible Markup Language (XML) 1.0
              (Fifth Edition)", W3C Recommendation
              REC-xml-20081126, November 2008,
              <https://www.w3.org/TR/2008/REC-xml-20081126/>.

7.2.  참고적 참조

   [ABOUT]    Moonesamy, S., Ed., "The 'about' URI Scheme", RFC 6694,
              DOI 10.17487/RFC6694, August 2012,
              <https://www.rfc-editor.org/info/rfc6694>.

   [HTML5]    WHATWG, "HTML: Living Standard",
              <https://html.spec.whatwg.org>.

   [ISO-19757-2]
              ISO, "Information technology -- Document Schema Definition
              Language (DSDL) -- Part 2: Regular-grammar-based
              validation -- RELAX NG", ISO/IEC 19757-2:2008,
              December 2008,
              <https://www.iso.org/standard/52348.html>.

   [JSON-POINTER]
              Bryan, P., Ed., Zyp, K., and M. Nottingham, Ed.,
              "JavaScript Object Notation (JSON) Pointer", RFC 6901,
              DOI 10.17487/RFC6901, April 2013,
              <https://www.rfc-editor.org/info/rfc6901>.

   [JSON-SCHEMA]
              Wright, A., Ed., Andrews, H., Ed., Hutton, B., Ed., and
              G. Dennis, "JSON Schema: A Media Type for Describing JSON
              Documents", Work in Progress, Internet-Draft,
              draft-bhutton-json-schema-01, 10 June 2022,
              <https://datatracker.ietf.org/doc/html/
              draft-bhutton-json-schema-01>.

   [RDFA]     Adida, B., Birbeck, M., McCarron, S., and I. Herman,
              "RDFa Core 1.1 - Third Edition", W3C Recommendation,
              March 2015,
              <https://www.w3.org/TR/2015/REC-rdfa-core-20150317/>.

   [RFC7807]  Nottingham, M. and E. Wilde, "Problem Details for HTTP
              APIs", RFC 7807, DOI 10.17487/RFC7807, March 2016,
              <https://www.rfc-editor.org/info/rfc7807>.

   [TAG]      Kindberg, T. and S. Hawke, "The 'tag' URI Scheme",
              RFC 4151, DOI 10.17487/RFC4151, October 2005,
              <https://www.rfc-editor.org/info/rfc4151>.

   [WEB-LINKING]
              Nottingham, M., "Web Linking", RFC 8288,
              DOI 10.17487/RFC8288, October 2017,
              <https://www.rfc-editor.org/info/rfc8288>.

   [XSLT]     Clark, J., Pieters, S., and H. Thompson, "Associating
              Style Sheets with XML documents 1.0 (Second Edition)",
              W3C Recommendation, October 2010,
              <https://www.w3.org/TR/2010/REC-xml-stylesheet-20101028/>.

부록 A.  HTTP Problem을 위한 JSON Schema

   이 섹션은 HTTP problem details를 위한 비규범적 JSON Schema를
   제시한다.
 이 스키마와 명세 텍스트 사이에 불일치가 있는 경우,
   후자가 우선한다.

   {
     "$schema": "https://json-schema.org/draft/2020-12/schema",
     "title": "An RFC 7807 problem object",
     "type": "object",
     "properties": {
       "type": {
         "type": "string",
         "format": "uri-reference",
         "description": "A URI reference that identifies the
   problem type."
       },
       "title": {
         "type": "string",
         "description": "A short, human-readable summary of the
   problem type."
       },
       "status": {
         "type": "integer",
         "description": "The HTTP status code
   generated by the origin server for this occurrence of the problem.",
         "minimum": 100,
         "maximum": 599
       },
       "detail": {
         "type": "string",
         "description": "A human-readable explanation specific to
   this occurrence of the problem."
       },
       "instance": {
         "type": "string",
         "format": "uri-reference",
         "description": "A URI reference that identifies the
   specific occurrence of the problem. It may or may not yield
   further information if dereferenced."
       }
     }
   }

부록 B.  HTTP Problem과 XML

   XML을 사용하는 HTTP 기반 API는 이 부록에서 정의된 형식을 사용하여
   problem details를 표현할 수 있다.

   XML 형식을 위한 RELAX NG 스키마는 다음과 같다:

      default namespace ns = "urn:ietf:rfc:7807"

      start = problem

      problem =
        element problem {
          (  element  type            { xsd:anyURI }?
           & element  title           { xsd:string }?
           & element  detail          { xsd:string }?
           & element  status          { xsd:positiveInteger }?
           & element  instance        { xsd:anyURI }? ),
          anyNsElement
        }

      anyNsElement =
        (  element    ns:*  { anyNsElement | text }
         | attribute  *     { text })*

   이 스키마는 문서화 목적으로만 의도되었으며 XML 형식의 모든 제약
   조건을 캡처하는 규범적 스키마가 아님에 유의하라.
 선택한 스키마
   언어의 기능에 따라 다른 XML 스키마 언어를 사용하여 유사한 제약
   조건 집합을 정의할 수 있다.

   이 형식의 미디어 타입은 "application/problem+xml"이다.

   확장 배열과 객체는 자식 요소를 포함하는 요소를 객체로 간주하여
   XML 형식으로 직렬화되며, "i"라는 이름의 하나 이상의 자식 요소만
   포함하는 요소는 배열로 간주된다.
 예를 들어, 위의 예시는 XML에서
   다음과 같이 나타난다:

   HTTP/1.1 403 Forbidden
   Content-Type: application/problem+xml
   Content-Language: en

   <?xml version="1.0" encoding="UTF-8"?>
   <problem xmlns="urn:ietf:rfc:7807">
     <type>https://example.com/probs/out-of-credit</type>
     <title>You do not have enough credit.</title>
     <detail>Your current balance is 30, but that costs 50.</detail>
     <instance>https://example.net/account/12345/msgs/abc</instance>
     <balance>30</balance>
     <accounts>
       <i>https://example.net/account/12345</i>
       <i>https://example.net/account/67890</i>
     </accounts>
   </problem>

   이 형식은 XML 네임스페이스를 사용하는데, 이는 주로 다른 XML 기반
   형식에 임베딩할 수 있도록 하기 위함이다; 이것이 다른 네임스페이스의
   요소나 속성으로 확장할 수 있거나 확장해야 함을 의미하지는 않는다.
   RELAX NG 스키마는 XML 형식에서 사용되는 하나의 네임스페이스의
   요소만을 명시적으로 허용한다.
 모든 확장 배열과 객체는 해당
   네임스페이스만을 사용하여 XML 마크업으로 직렬화되어야 한다(MUST).

   XML 형식을 사용할 때, 참조된 XSL Transformations (XSLT) 코드를
   사용하여 XML을 변환하도록 클라이언트에 지시하는 XML 처리 명령을
   XML에 임베딩할 수 있다.
 이 코드가 XML을 (X)HTML로 변환하는 경우,
   XML 형식을 제공하면서도 변환을 수행할 수 있는 클라이언트가
   클라이언트에서 렌더링되고 표시되는 사람 친화적인 (X)HTML을
   표시하도록 할 수 있다.
 이 방법을 사용할 때, XSLT 코드를 실행할 수
   있는 클라이언트의 수를 최대화하기 위해 XSLT 1.0을 사용하는 것이
   바람직하다.

부록 C.  다른 형식과 함께 Problem Details 사용

   일부 상황에서는, 여기에 설명된 것 이외의 형식에 problem details를
   임베딩하는 것이 유리할 수 있다.
 예를 들어, HTML을 사용하는 API가
   problem details를 표현하기 위해 HTML도 사용하고 싶을 수 있다.

   Problem details는 기존 직렬화(JSON 또는 XML) 중 하나를 해당 형식에
   캡슐화하거나 problem detail의 모델(섹션 3에 명시된 대로)을 해당
   형식의 규약으로 변환하여 다른 형식에 임베딩할 수 있다.

   예를 들어, HTML에서는 script 태그 안에 JSON을 캡슐화하여 문제를
   임베딩할 수 있다:

   <script type="application/problem+json">
     {
      "type": "https://example.com/probs/out-of-credit",
      "title": "You do not have enough credit.",
      "detail": "Your current balance is 30, but that costs 50.",
      "instance": "/account/12345/msgs/abc",
      "balance": 30,
      "accounts": ["/account/12345",
                   "/account/67890"]
     }
   </script>

   또는 Resource Description Framework in Attributes (RDFa)로의
   매핑을 정의하여 임베딩할 수 있다.

   이 명세는 다른 형식에 problem details를 임베딩하는 것에 대한 특정
   권장사항을 제시하지 않는다; 이를 임베딩하는 적절한 방법은 사용
   중인 형식과 해당 형식의 적용 방식 모두에 달려 있다.

부록 D.  RFC 7807로부터의 변경사항

   이 개정판은 다음과 같은 변경을 하였다:

   *  섹션 4.2는 공통 problem type URI의 레지스트리를 도입한다

   *  섹션 3은 여러 문제가 어떻게 처리되어야 하는지를 명확히 한다

   *  섹션 3.1.1은 역참조할 수 없는 type URI의 사용에 대한 지침을
      제공한다

감사의 글

   저자들은 Jan Algermissen, Subbu Allamaraju, Mike Amundsen,
   Roy Fielding, Eran Hammer, Sam Johnston, Mike McCall,
   Julian Reschke, James Snell의 의견과 제안에 감사를 표한다.

저자 주소

   Mark Nottingham
   Prahran
   Australia
   Email: mnot@mnot.net
   URI:   https://www.mnot.net/

   Erik Wilde
   Email: erik.wilde@dret.net
   URI:   http://dret.net/netdret/

   Sanjay Dalal
   United States of America
   Email: sanjay.dalal@cal.berkeley.edu
   URI:   https://github.com/sdatspun2
