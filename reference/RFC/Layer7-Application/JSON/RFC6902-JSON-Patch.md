Internet Engineering Task Force (IETF)                     P. Bryan, Ed.
Request for Comments: 6902                                Salesforce.com
Category: Standards Track                             M. Nottingham, Ed.
ISSN: 2070-1721                                                   Akamai
                                                              April 2013


                JavaScript Object Notation (JSON) Patch

초록

   JSON Patch는 JavaScript Object Notation(JSON) 문서에 적용할 연산의
   시퀀스를 표현하기 위한 JSON 문서 구조를 정의한다. 이는 HTTP PATCH
   메서드와 함께 사용하기에 적합하다. "application/json-patch+json"
   미디어 타입은 이러한 패치 문서를 식별하는 데 사용된다.

이 메모의 상태

   이 문서는 인터넷 표준 트랙 문서이다.

   이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물이다. 이는
   IETF 커뮤니티의 합의를 나타낸다. 이 문서는 공개 검토를 받았으며
   인터넷 엔지니어링 운영 그룹(IESG)에 의해 출판이 승인되었다. 인터넷
   표준에 대한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는
   방법에 대한 정보는 http://www.rfc-editor.org/info/rfc6902 에서
   얻을 수 있다.

저작권 고지

   Copyright (c) 2013 IETF Trust 및 문서 저자로 식별된 자. 모든 권리
   보유.

   이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF 문서에 관한
   IETF Trust의 법적 조항(http://trustee.ietf.org/license-info)의
   적용을 받는다. 이 문서들은 이 문서에 관한 귀하의 권리와 제한사항을
   기술하므로 주의 깊게 검토하기 바란다. 이 문서에서 추출된 코드
   구성요소는 Trust 법적 조항의 섹션 4.e에 기술된 바와 같이 간소화된
   BSD 라이선스 텍스트를 포함해야 하며, 간소화된 BSD 라이선스에 기술된
   바와 같이 보증 없이 제공된다.

목차

   1.  소개  . . . . . . . . . . . . . . . . . . . . . . . . . . . .  3
   2.  규약  . . . . . . . . . . . . . . . . . . . . . . . . . . . .  3
   3.  문서 구조 . . . . . . . . . . . . . . . . . . . . . . . . . .  3
   4.  연산  . . . . . . . . . . . . . . . . . . . . . . . . . . . .  4
     4.1.  add . . . . . . . . . . . . . . . . . . . . . . . . . . .  4
     4.2.  remove  . . . . . . . . . . . . . . . . . . . . . . . . .  6
     4.3.  replace . . . . . . . . . . . . . . . . . . . . . . . . .  6
     4.4.  move  . . . . . . . . . . . . . . . . . . . . . . . . . .  6
     4.5.  copy  . . . . . . . . . . . . . . . . . . . . . . . . . .  7
     4.6.  test  . . . . . . . . . . . . . . . . . . . . . . . . . .  7
   5.  오류 처리 . . . . . . . . . . . . . . . . . . . . . . . . . .  8
   6.  IANA 고려사항 . . . . . . . . . . . . . . . . . . . . . . . .  9
   7.  보안 고려사항 . . . . . . . . . . . . . . . . . . . . . . . . 10
   8.  감사의 글 . . . . . . . . . . . . . . . . . . . . . . . . . . 10
   9.  참고 문헌 . . . . . . . . . . . . . . . . . . . . . . . . . . 10
     9.1.  규범적 참고 문헌 . . . . . . . . . . . . . . . . . . . . . 10
     9.2.  참고적 참고 문헌 . . . . . . . . . . . . . . . . . . . . . 10
   부록 A.  예제 . . . . . . . . . . . . . . . . . . . . . . . . . . 12
     A.1.  객체 멤버 추가  . . . . . . . . . . . . . . . . . . . . . 12
     A.2.  배열 요소 추가  . . . . . . . . . . . . . . . . . . . . . 12
     A.3.  객체 멤버 제거  . . . . . . . . . . . . . . . . . . . . . 12
     A.4.  배열 요소 제거  . . . . . . . . . . . . . . . . . . . . . 13
     A.5.  값 교체 . . . . . . . . . . . . . . . . . . . . . . . . . 13
     A.6.  값 이동 . . . . . . . . . . . . . . . . . . . . . . . . . 14
     A.7.  배열 요소 이동  . . . . . . . . . . . . . . . . . . . . . 14
     A.8.  값 테스트: 성공 . . . . . . . . . . . . . . . . . . . . . 15
     A.9.  값 테스트: 오류 . . . . . . . . . . . . . . . . . . . . . 15
     A.10. 중첩된 멤버 객체 추가 . . . . . . . . . . . . . . . . . . 15
     A.11. 인식되지 않는 요소 무시 . . . . . . . . . . . . . . . . . 16
     A.12. 존재하지 않는 대상에 추가 . . . . . . . . . . . . . . . . 16
     A.13. 유효하지 않은 JSON Patch 문서 . . . . . . . . . . . . . . 17
     A.14. ~ 이스케이프 순서 . . . . . . . . . . . . . . . . . . . . 17
     A.15. 문자열과 숫자 비교 . . . . . . . . . . . . . . . . . . . . 17
     A.16. 배열 값 추가  . . . . . . . . . . . . . . . . . . . . . . 18

1.  소개

   JavaScript Object Notation(JSON) [RFC4627]은 구조화된 데이터의
   교환 및 저장을 위한 일반적인 형식이다. HTTP PATCH [RFC5789]는
   하이퍼텍스트 전송 프로토콜(HTTP) [RFC2616]을 리소스에 대한 부분적
   수정을 수행하기 위한 메서드로 확장한다.

   JSON Patch는 대상 JSON 문서에 적용할 연산의 시퀀스를 표현하기 위한
   형식(미디어 타입 "application/json-patch+json"으로 식별됨)이다. 이는
   HTTP PATCH 메서드와 함께 사용하기에 적합하다.

   이 형식은 JSON 문서 또는 유사한 제약 조건을 가진 데이터 구조(즉,
   JSON 문법을 사용하여 객체 또는 배열로 직렬화할 수 있는 데이터
   구조)에 대해 부분적 업데이트가 필요한 다른 경우에도 잠재적으로
   유용하다.

2.  규약

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"이라는
   키워드는 RFC 2119 [RFC2119]에 기술된 대로 해석되어야 한다.

3.  문서 구조

   JSON Patch 문서는 객체의 배열을 나타내는 JSON [RFC4627] 문서이다.
   각 객체는 대상 JSON 문서에 적용할 단일 연산을 나타낸다.

   다음은 HTTP PATCH 요청에서 전송되는 JSON Patch 문서의 예시이다:

   PATCH /my/data HTTP/1.1
   Host: example.org
   Content-Length: 326
   Content-Type: application/json-patch+json
   If-Match: "abc123"

   [
     { "op": "test", "path": "/a/b/c", "value": "foo" },
     { "op": "remove", "path": "/a/b/c" },
     { "op": "add", "path": "/a/b/c", "value": [ "foo", "bar" ] },
     { "op": "replace", "path": "/a/b/c", "value": 42 },
     { "op": "move", "from": "/a/b/c", "path": "/a/b/d" },
     { "op": "copy", "from": "/a/b/d", "path": "/a/b/e" }
   ]

   JSON Patch 문서의 평가는 대상 JSON 문서에 대해 시작된다. 연산은
   배열에 나타나는 순서대로 순차적으로 적용된다. 시퀀스의 각 연산은
   대상 문서에 적용되며, 결과 문서는 다음 연산의 대상이 된다. 평가는
   모든 연산이 성공적으로 적용되거나 오류 조건이 발생할 때까지
   계속된다.

4.  연산

   연산 객체는 수행할 연산을 나타내는 값을 가진 정확히 하나의 "op"
   멤버를 가져야 한다(MUST). 그 값은 "add", "remove", "replace",
   "move", "copy", 또는 "test" 중 하나여야 한다(MUST). 다른 값은
   오류이다. 각 객체의 의미는 아래에 정의되어 있다.

   추가로, 연산 객체는 정확히 하나의 "path" 멤버를 가져야 한다(MUST).
   해당 멤버의 값은 연산이 수행되는 대상 문서 내의 위치("대상 위치")를
   참조하는 JSON-Pointer 값 [RFC6901]을 포함하는 문자열이다.

   다른 연산 객체 멤버의 의미는 연산에 의해 정의된다(아래 하위 섹션
   참조). 해당 연산에 대해 명시적으로 정의되지 않은 멤버는 무시되어야
   한다(MUST)(즉, 정의되지 않은 멤버가 객체에 나타나지 않은 것처럼
   연산이 완료된다).

   JSON 객체에서 멤버의 순서는 중요하지 않다는 점에 유의하라. 따라서
   다음 연산 객체들은 동등하다:

   { "op": "add", "path": "/a/b/c", "value": "foo" }
   { "path": "/a/b/c", "op": "add", "value": "foo" }
   { "value": "foo", "path": "/a/b/c", "op": "add" }

   연산은 JSON 문서로 표현되는 데이터 구조에 적용된다. 즉, 모든
   이스케이프 해제([RFC4627], 섹션 2.5 참조)가 이루어진 후에 적용된다.

4.1.  add

   "add" 연산은 대상 위치가 무엇을 참조하는지에 따라 다음 기능 중
   하나를 수행한다:

   o  대상 위치가 배열 인덱스를 지정하는 경우, 지정된 인덱스에서
      배열에 새 값이 삽입된다.

   o  대상 위치가 아직 존재하지 않는 객체 멤버를 지정하는 경우,
      객체에 새 멤버가 추가된다.

   o  대상 위치가 이미 존재하는 객체 멤버를 지정하는 경우, 해당
      멤버의 값이 교체된다.

   연산 객체는 추가할 값을 지정하는 내용을 가진 "value" 멤버를
   포함해야 한다(MUST).

   예시:

   { "op": "add", "path": "/a/b/c", "value": [ "foo", "bar" ] }

   연산이 적용될 때, 대상 위치는 다음 중 하나를 참조해야 한다(MUST):

   o  대상 문서의 루트 - 이 경우 지정된 값이 대상 문서의 전체 내용이
      된다.

   o  기존 객체에 추가할 멤버 - 이 경우 제공된 값이 해당 객체의
      표시된 위치에 추가된다. 멤버가 이미 존재하는 경우, 지정된 값으로
      교체된다.

   o  기존 배열에 추가할 요소 - 이 경우 제공된 값이 배열의 표시된
      위치에 추가된다. 지정된 인덱스 이상의 모든 요소는 오른쪽으로
      한 위치 이동된다. 지정된 인덱스는 배열의 요소 수보다 크지
      않아야 한다(MUST NOT). "-" 문자가 배열의 끝을 인덱싱하는 데
      사용되는 경우([RFC6901] 참조), 이는 배열에 값을 추가(append)하는
      효과를 가진다.

   이 연산은 기존 객체와 배열에 추가하도록 설계되었으므로, 대상 위치가
   종종 존재하지 않을 것이다. 따라서 포인터의 오류 처리 알고리즘이
   호출되지만, 이 명세는 "add" 포인터에 대한 오류 처리 동작을 해당
   오류를 무시하고 지정된 대로 값을 추가하도록 정의한다.

   그러나 객체 자체 또는 그것을 포함하는 배열은 존재해야 하며, 그렇지
   않은 경우는 여전히 오류이다. 예를 들어, 다음 문서로 시작하여
   대상 위치가 "/a/b"인 "add"는:

   { "a": { "foo": 1 } }

   오류가 아니다. 왜냐하면 "a"가 존재하고, "b"가 그 값에 추가될
   것이기 때문이다. 다음 문서에서는 오류이다:

   { "q": { "bar": 2 } }

   왜냐하면 "a"가 존재하지 않기 때문이다.

4.2.  remove

   "remove" 연산은 대상 위치의 값을 제거한다.

   연산이 성공하려면 대상 위치가 존재해야 한다(MUST).

   예시:

   { "op": "remove", "path": "/a/b/c" }

   배열에서 요소를 제거하는 경우, 지정된 인덱스 위의 모든 요소는
   왼쪽으로 한 위치 이동된다.

4.3.  replace

   "replace" 연산은 대상 위치의 값을 새 값으로 교체한다. 연산 객체는
   교체 값을 지정하는 내용을 가진 "value" 멤버를 포함해야 한다(MUST).

   연산이 성공하려면 대상 위치가 존재해야 한다(MUST).

   예시:

   { "op": "replace", "path": "/a/b/c", "value": 42 }

   이 연산은 값에 대한 "remove" 연산 후 즉시 동일한 위치에 교체 값을
   사용하는 "add" 연산이 뒤따르는 것과 기능적으로 동일하다.

4.4.  move

   "move" 연산은 지정된 위치의 값을 제거하고 대상 위치에 추가한다.

   연산 객체는 대상 문서에서 값을 이동할 위치를 참조하는 JSON Pointer
   값을 포함하는 문자열인 "from" 멤버를 포함해야 한다(MUST).

   연산이 성공하려면 "from" 위치가 존재해야 한다(MUST).

   예시:

   { "op": "move", "from": "/a/b/c", "path": "/a/b/d" }

   이 연산은 "from" 위치에 대한 "remove" 연산 후 즉시 방금 제거된
   값을 사용하여 대상 위치에 "add" 연산이 뒤따르는 것과 기능적으로
   동일하다.

   "from" 위치는 "path" 위치의 적절한 접두사가 아니어야 한다(MUST NOT).
   즉, 위치를 자신의 하위 항목 중 하나로 이동할 수 없다.

4.5.  copy

   "copy" 연산은 지정된 위치의 값을 대상 위치에 복사한다.

   연산 객체는 대상 문서에서 값을 복사할 위치를 참조하는 JSON Pointer
   값을 포함하는 문자열인 "from" 멤버를 포함해야 한다(MUST).

   연산이 성공하려면 "from" 위치가 존재해야 한다(MUST).

   예시:

   { "op": "copy", "from": "/a/b/c", "path": "/a/b/e" }

   이 연산은 "from" 멤버에 지정된 값을 사용하여 대상 위치에 "add"
   연산을 수행하는 것과 기능적으로 동일하다.

4.6.  test

   "test" 연산은 대상 위치의 값이 지정된 값과 같은지 테스트한다.

   연산 객체는 대상 위치의 값과 비교할 값을 전달하는 "value" 멤버를
   포함해야 한다(MUST).

   연산이 성공적인 것으로 간주되려면 대상 위치가 "value" 값과
   같아야 한다(MUST).

   여기서 "같다"는 것은 대상 위치의 값과 "value"에 의해 전달되는 값이
   동일한 JSON 타입이며, 해당 타입에 대한 다음 규칙에 의해 같다고
   간주됨을 의미한다:

   o  문자열: 동일한 수의 유니코드 문자를 포함하고 코드 포인트가
      바이트 단위로 같으면 같다고 간주된다.

   o  숫자: 값이 수치적으로 같으면 같다고 간주된다.

   o  배열: 동일한 수의 값을 포함하고, 각 값이 이 타입별 규칙 목록을
      사용하여 다른 배열의 해당 위치에 있는 값과 같다고 간주될 수
      있으면 같다고 간주된다.

   o  객체: 동일한 수의 멤버를 포함하고, 각 멤버가 키(문자열로서)와
      값(이 타입별 규칙 목록을 사용하여)을 비교하여 다른 객체의
      멤버와 같다고 간주될 수 있으면 같다고 간주된다.

   o  리터럴(false, true, null): 같으면 같다고 간주된다.

   수행되는 비교는 논리적 비교라는 점에 유의하라. 예를 들어, 배열의
   멤버 값 사이의 공백은 중요하지 않다.

   또한 객체 멤버의 직렬화 순서가 중요하지 않다는 점에 유의하라.

   예시:

   { "op": "test", "path": "/a/b/c", "value": "foo" }

5.  오류 처리

   JSON Patch 문서가 규범적 요구사항을 위반하거나 연산이 성공하지
   못한 경우, JSON Patch 문서의 평가는 종료되어야 하며(SHOULD) 전체
   패치 문서의 적용이 성공한 것으로 간주되어서는 안 된다(SHALL NOT).

   JSON Patch가 HTTP PATCH 메서드와 함께 사용될 때의 오류 처리에 관한
   고려사항, 다양한 조건을 나타내기 위해 사용할 제안된 상태 코드를
   포함하여, [RFC5789] 섹션 2.2를 참조하라.

   HTTP PATCH 메서드는 [RFC5789]에 따라 원자적이라는 점에 유의하라.
   따라서 다음 패치는 문서에 아무런 변경도 이루어지지 않는 결과를
   초래할 것이다("test" 연산이 오류를 발생시키기 때문이다):

   [
     { "op": "replace", "path": "/a/b/c", "value": 42 },
     { "op": "test", "path": "/a/b/c", "value": "C" }
   ]

6.  IANA 고려사항

   JSON Patch 문서에 대한 인터넷 미디어 타입은 application/
   json-patch+json이다.

   타입 이름:  application

   하위 타입 이름:  json-patch+json

   필수 매개변수:  없음

   선택적 매개변수:  없음

   인코딩 고려사항:  binary

   보안 고려사항:
      섹션 7의 보안 고려사항을 참조하라.

   상호 운용성 고려사항:  해당 없음

   발행된 명세:
      RFC 6902

   이 미디어 타입을 사용하는 애플리케이션:
      JSON 문서를 조작하는 애플리케이션.

   추가 정보:

      매직 넘버:  해당 없음

      파일 확장자:  .json-patch

      매킨토시 파일 타입 코드:  TEXT

   추가 정보를 위한 연락처 및 이메일 주소:
      Paul C. Bryan <pbryan@anode.ca>

   의도된 용도:  COMMON

   사용 제한:  없음

   저자:  Paul C. Bryan <pbryan@anode.ca>

   변경 관리자:  IETF

7.  보안 고려사항

   이 명세는 JSON [RFC4627] 및 JSON-Pointer [RFC6901]과 동일한 보안
   고려사항을 가진다.

   일부 오래된 웹 브라우저는 루트가 배열인 임의의 JSON 문서를
   로드하도록 강제될 수 있으며, 이는 민감한 정보를 포함하는 JSON Patch
   문서가 접근이 인증되더라도 공격자에게 노출될 수 있는 상황을
   초래한다. 이는 교차 사이트 요청 위조(CSRF) 공격 [CSRF]으로 알려져
   있다.

   그러나 이러한 브라우저는 널리 사용되지 않는다(이 글을 쓰는 시점에서
   시장의 1% 미만에서 사용되는 것으로 추정된다). 그럼에도 불구하고
   이 공격이 우려되는 발행자는 이러한 문서를 HTTP GET으로 제공하지
   않도록 권고한다.

8.  감사의 글

   다음 개인들이 이 명세에 아이디어, 피드백, 문구를 기여하였다:

      Mike Acar, Mike Amundsen, Cyrus Daboo, Paul Davis, Stefan Koegl,
      Murray S. Kucherawy, Dean Landolt, Randall Leeds, James Manger,
      Julian Reschke, James Snell, Eli Stevens, Henry S. Thompson.

   JSON Patch 문서의 구조는 XML Patch 문서 명세 [RFC5261]의 영향을
   받았다.

9.  참고 문헌

9.1.  규범적 참고 문헌

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119, March 1997.

   [RFC4627]  Crockford, D., "The application/json Media Type for
              JavaScript Object Notation (JSON)", RFC 4627, July 2006.

   [RFC6901]  Bryan, P., Ed., Zyp, K., and M. Nottingham, Ed.,
              "JavaScript Object Notation (JSON) Pointer", RFC 6901,
              April 2013.

9.2.  참고적 참고 문헌

   [CSRF]     Barth, A., Jackson, C., and J. Mitchell, "Robust Defenses
              for Cross-Site Request Forgery", ACM Conference
              on Computer and Communications Security, October 2008,
              <http://seclab.stanford.edu/websec/csrf/csrf.pdf>.

   [RFC2616]  Fielding, R., Gettys, J., Mogul, J., Frystyk, H.,
              Masinter, L., Leach, P., and T. Berners-Lee, "Hypertext
              Transfer Protocol -- HTTP/1.1", RFC 2616, June 1999.

   [RFC5261]  Urpalainen, J., "An Extensible Markup Language (XML) Patch
              Operations Framework Utilizing XML Path Language (XPath)
              Selectors", RFC 5261, September 2008.

   [RFC5789]  Dusseault, L. and J. Snell, "PATCH Method for HTTP",
              RFC 5789, March 2010.

부록 A.  예제

A.1.  객체 멤버 추가

   예시 대상 JSON 문서:

   { "foo": "bar"}

   JSON Patch 문서:

   [
     { "op": "add", "path": "/baz", "value": "qux" }
   ]

   결과 JSON 문서:

   {
     "baz": "qux",
     "foo": "bar"
   }

A.2.  배열 요소 추가

   예시 대상 JSON 문서:

   { "foo": [ "bar", "baz" ] }

   JSON Patch 문서:

   [
     { "op": "add", "path": "/foo/1", "value": "qux" }
   ]

   결과 JSON 문서:

   { "foo": [ "bar", "qux", "baz" ] }

A.3.  객체 멤버 제거

   예시 대상 JSON 문서:

   {
     "baz": "qux",
     "foo": "bar"
   }

   JSON Patch 문서:

   [
     { "op": "remove", "path": "/baz" }
   ]

   결과 JSON 문서:

   { "foo": "bar" }

A.4.  배열 요소 제거

   예시 대상 JSON 문서:

   { "foo": [ "bar", "qux", "baz" ] }

   JSON Patch 문서:

   [
     { "op": "remove", "path": "/foo/1" }
   ]

   결과 JSON 문서:

   { "foo": [ "bar", "baz" ] }

A.5.  값 교체

   예시 대상 JSON 문서:

   {
     "baz": "qux",
     "foo": "bar"
   }

   JSON Patch 문서:

   [
     { "op": "replace", "path": "/baz", "value": "boo" }
   ]

   결과 JSON 문서:

   {
     "baz": "boo",
     "foo": "bar"
   }

A.6.  값 이동

   예시 대상 JSON 문서:

   {
     "foo": {
       "bar": "baz",
       "waldo": "fred"
     },
     "qux": {
       "corge": "grault"
     }
   }

   JSON Patch 문서:

   [
     { "op": "move", "from": "/foo/waldo", "path": "/qux/thud" }
   ]

   결과 JSON 문서:

   {
     "foo": {
       "bar": "baz"
     },
     "qux": {
       "corge": "grault",
       "thud": "fred"
     }
   }

A.7.  배열 요소 이동

   예시 대상 JSON 문서:

   { "foo": [ "all", "grass", "cows", "eat" ] }

   JSON Patch 문서:

   [
     { "op": "move", "from": "/foo/1", "path": "/foo/3" }
   ]

   결과 JSON 문서:

   { "foo": [ "all", "cows", "eat", "grass" ] }

A.8.  값 테스트: 성공

   예시 대상 JSON 문서:

   {
     "baz": "qux",
     "foo": [ "a", 2, "c" ]
   }

   성공적인 평가를 초래할 JSON Patch 문서:

   [
     { "op": "test", "path": "/baz", "value": "qux" },
     { "op": "test", "path": "/foo/1", "value": 2 }
   ]

A.9.  값 테스트: 오류

   예시 대상 JSON 문서:

   { "baz": "qux" }

   오류 조건을 초래할 JSON Patch 문서:

   [
     { "op": "test", "path": "/baz", "value": "bar" }
   ]

A.10.  중첩된 멤버 객체 추가

   예시 대상 JSON 문서:

   { "foo": "bar" }

   JSON Patch 문서:

   [
     { "op": "add", "path": "/child", "value": { "grandchild": { } } }
   ]

   결과 JSON 문서:

   {
     "foo": "bar",
     "child": {
       "grandchild": {
       }
     }
   }

A.11.  인식되지 않는 요소 무시

   예시 대상 JSON 문서:

   { "foo": "bar" }

   JSON Patch 문서:

   [
     { "op": "add", "path": "/baz", "value": "qux", "xyz": 123 }
   ]

   결과 JSON 문서:

   {
     "foo": "bar",
     "baz": "qux"
   }

A.12.  존재하지 않는 대상에 추가

   예시 대상 JSON 문서:

   { "foo": "bar" }

   JSON Patch 문서:

   [
     { "op": "add", "path": "/baz/bat", "value": "qux" }
   ]

   이 JSON Patch 문서를 위의 대상 JSON 문서에 적용하면 오류가
   발생한다(따라서 적용되지 않는다). "add" 연산의 대상 위치가 문서의
   루트도, 기존 객체의 멤버도, 기존 배열의 멤버도 참조하지 않기
   때문이다.

A.13.  유효하지 않은 JSON Patch 문서

   JSON Patch 문서:

   [
     { "op": "add", "path": "/baz", "value": "qux", "op": "remove" }
   ]

   이 JSON Patch 문서는 "add" 연산으로 처리될 수 없다. 이후에
   "op":"remove" 요소를 포함하고 있기 때문이다. JSON은 객체 멤버
   이름이 고유할 것을 "SHOULD" 요구사항으로 요구하며, 중복에 대한
   표준 오류 처리는 없다.

A.14.  ~ 이스케이프 순서

   예시 대상 JSON 문서:

   {
     "/": 9,
     "~1": 10
   }

   JSON Patch 문서:

   [
     {"op": "test", "path": "/~01", "value": 10}
   ]

   결과 JSON 문서:

   {
     "/": 9,
     "~1": 10
   }

A.15.  문자열과 숫자 비교

   예시 대상 JSON 문서:

   {
     "/": 9,
     "~1": 10
   }

   JSON Patch 문서:

   [
     {"op": "test", "path": "/~01", "value": "10"}
   ]

   이는 테스트가 실패하기 때문에 오류를 초래한다. 문서 값은 숫자인
   반면, 테스트하는 값은 문자열이다.

A.16.  배열 값 추가

   예시 대상 JSON 문서:

   { "foo": ["bar"] }

   JSON Patch 문서:

   [
     { "op": "add", "path": "/foo/-", "value": ["abc", "def"] }
   ]

   결과 JSON 문서:

   { "foo": ["bar", ["abc", "def"]] }

저자 주소

   Paul C. Bryan (editor)
   Salesforce.com

   Phone: +1 604 783 1481
   EMail: pbryan@anode.ca


   Mark Nottingham (editor)
   Akamai

   EMail: mnot@mnot.net
