
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

   이 문서는 Internet Standards Track 문서이다. 이 문서는 Internet
   Engineering Task Force (IETF)의 산출물이다. 이 문서는 IETF 커뮤니티의
   합의를 나타낸다. 이 문서는 공개 검토를 받았으며 Internet Engineering
   Steering Group (IESG)에 의해 발행이 승인되었다. Internet Standards에
   대한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는 방법에
   대한 정보는 http://www.rfc-editor.org/info/rfc6585 에서 확인할 수 있다.

저작권 고지

   Copyright (c) 2012 IETF Trust 및 문서 작성자로 식별된 사람들. 모든
   권리 보유.

   이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF 문서에 관한 IETF
   Trust의 법적 조항(http://trustee.ietf.org/license-info)의 적용을
   받는다. 이 문서에 대한 귀하의 권리와 제한 사항이 기술되어 있으므로
   이러한 문서를 주의 깊게 검토하시기 바란다. 이 문서에서 추출된 코드
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
   대한 메커니즘을 정의하지 않는다는 점에 유의하라. 예를 들어, origin
   서버가 리소스별, 전체 서버에 대해, 또는 서버 집합에 대해 요청을 제한하는
   것은 이 명세의 범위 밖이다. 마찬가지로, 이 목적을 위해 사용자를
   인증 자격 증명으로 식별하든 상태 기반 쿠키로 식별하든 이 명세의 범위
   밖이다.

   429 상태 코드를 가진 응답은 캐시에 저장되어서는 안 된다(MUST NOT).

5. 431 Request Header Fields Too Large

   431 상태 코드는 서버가 요청의 헤더 필드가 너무 크기 때문에 요청을
   처리할 의사가 없음을 나타낸다. 요청 헤더 필드의 크기를 줄인 후 요청을
   다시 제출할 수 있다(MAY).

   이 코드는 전체 요청 헤더 필드 집합이 너무 클 때와 단일 헤더 필드에
   문제가 있을 때 모두 사용할 수 있다. 후자의 경우, 응답 표현은 어떤
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
   (SHOULD NOT). 브라우저가 로그인 인터페이스를 원래 요청한 URL과 연관된
   것으로 표시하여 혼란을 야기할 수 있기 때문이다.

   511 상태는 origin 서버에 의해 생성되어서는 안 된다(SHOULD NOT); 이
   코드는 네트워크 접근을 제어하는 수단으로 중간에 개입하는 가로채기
   프록시(intercepting proxy)가 사용하도록 의도된 것이다.

   511 상태 코드를 가진 응답은 캐시에 저장되어서는 안 된다(MUST NOT).

6.1. 511 상태 코드와 Captive Portal

   511 상태 코드는 요청이 전달된 서버가 아닌 중간 네트워크 인프라로부터의
   응답을 기대하는 소프트웨어(특히 비브라우저 에이전트)에 "captive portal"
   이 야기하는 문제를 완화하기 위해 설계되었다. 이 코드는 captive portal의
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
   origin 서버에서 오지 않을 것이다. 이는 많은 보안 문제를 야기한다;
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
   사용하는 "favicon.ico" [Favicon]이다. 주어진 사이트의 favicon이 의도된
   사이트가 아닌 captive portal에서 가져오면(예: 사용자가 인증되지
   않았기 때문에), 이 favicon은 종종 portal 세션 이후에도 브라우저의
   캐시에 "고착"되어(대부분의 구현은 favicon을 적극적으로 캐시함) portal의
   favicon이 정당한 사이트를 "접수"한 것처럼 보이게 된다.

   또 다른 브라우저 기반 문제는 Platform for Privacy Preferences [P3P]가
   지원될 때 발생한다. 구현 방식에 따라, 브라우저가 p3p.xml 파일에 대한
   portal의 응답을 서버의 응답으로 해석하여, portal이 광고하는 개인정보
   보호 정책(또는 그 부재)이 의도된 사이트에 적용되는 것으로 해석될 수
   있다. WebFinger [WebFinger], Cross-Origin Resource Sharing [CORS],
   그리고 Open Authorization [OAuth2.0]과 같은 다른 웹 기반 프로토콜도
   이러한 문제에 취약할 수 있다.

   HTTP가 웹 브라우저에서 가장 널리 사용되지만, 점점 더 많은 비브라우저
   애플리케이션이 HTTP를 기반 프로토콜로 사용하고 있다. 예를 들어, Web
   Distributed Authoring and Versioning (WebDAV) [RFC4918]과 Calendaring
   Extensions to WebDAV (CalDAV) [RFC4791]은 모두 HTTP를 기반으로
   사용한다(각각 원격 저작 및 일정 관리 용도). captive portal 뒤에서
   이러한 애플리케이션을 사용하면 사용자에게 가짜 오류가 표시될 수 있으며,
   극단적인 경우 콘텐츠 손상이 발생할 수 있다.

   마찬가지로 HTTP를 사용하는 다른 비브라우저 애플리케이션도 영향을 받을
   수 있다. 예를 들어, 위젯 [WIDGETS], 소프트웨어 업데이트, 그리고
   Twitter 클라이언트 및 iTunes Music Store와 같은 기타 전문화된
   소프트웨어가 해당된다.

   HTTP 리다이렉션을 사용하여 트래픽을 portal로 보내면 이러한 문제를
   해결한다고 믿어지는 경우가 있다는 점에 유의해야 한다. 그러나 이러한
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
