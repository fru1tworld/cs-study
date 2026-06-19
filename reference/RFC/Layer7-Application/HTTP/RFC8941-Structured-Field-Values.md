Internet Engineering Task Force (IETF)                     M. Nottingham
Request for Comments: 8941                                        Fastly
Category: Standards Track                                      P-H. Kamp
ISSN: 2070-1721                                The Varnish Cache Project
                                                           February 2021


                    HTTP를 위한 구조화된 필드 값(Structured Field Values)

초록

   이 문서는 "구조화된 필드(Structured Fields)", "구조화된 헤더(Structured
   Headers)", 또는 "구조화된 트레일러(Structured Trailers)"로 알려진 HTTP
   헤더 및 트레일러 필드를 보다 쉽고 안전하게 정의하고 처리할 수 있도록 하는
   일련의 데이터 타입과 관련 알고리즘을 기술한다. 이는 전통적인 HTTP 필드 값보다
   더 제한적인 공통 구문을 사용하고자 하는 새로운 HTTP 필드의 명세에서 사용되도록
   의도되었다.

이 메모의 상태

   이 문서는 인터넷 표준화 과정(Internet Standards Track) 문서이다.

   이 문서는 인터넷 엔지니어링 태스크 포스(IETF)의 산출물이다. 이는 IETF
   커뮤니티의 합의를 나타낸다. 이 문서는 공개 검토를 받았으며, 인터넷 엔지니어링
   운영 그룹(IESG)에 의해 출판이 승인되었다. 인터넷 표준에 관한 추가 정보는 RFC
   7841의 2절에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는 방법에 관한
   정보는 https://www.rfc-editor.org/info/rfc8941 에서 얻을 수 있다.

저작권 고지

   Copyright (c) 2021 IETF Trust and the persons identified as the
   document authors. All rights reserved.

   이 문서는 BCP 78 및 이 문서의 출판일에 유효한 IETF 문서에 관한 IETF Trust의
   법적 조항(https://trustee.ietf.org/license-info)의 적용을 받는다. 이 문서에
   대한 귀하의 권리와 제한 사항을 기술하고 있으므로 이 문서들을 주의 깊게 검토하기
   바란다. 이 문서에서 추출한 코드 컴포넌트(Code Components)는 Trust Legal
   Provisions의 4.e절에 기술된 대로 Simplified BSD License 텍스트를 포함해야 하며,
   Simplified BSD License에 기술된 대로 어떠한 보증도 없이 제공된다.

목차

   1.  소개
     1.1.  의도적으로 엄격한 처리
     1.2.  표기 규약
   2.  새로운 구조화된 필드 정의하기
   3.  구조화된 데이터 타입
     3.1.  리스트(Lists)
       3.1.1.  내부 리스트(Inner Lists)
       3.1.2.  파라미터(Parameters)
     3.2.  딕셔너리(Dictionaries)
     3.3.  아이템(Items)
       3.3.1.  정수(Integers)
       3.3.2.  소수(Decimals)
       3.3.3.  문자열(Strings)
       3.3.4.  토큰(Tokens)
       3.3.5.  바이트 시퀀스(Byte Sequences)
       3.3.6.  불리언(Booleans)
   4.  HTTP에서 구조화된 필드 다루기
     4.1.  구조화된 필드 직렬화
       4.1.1.  리스트 직렬화
       4.1.2.  딕셔너리 직렬화
       4.1.3.  아이템 직렬화
       4.1.4.  정수 직렬화
       4.1.5.  소수 직렬화
       4.1.6.  문자열 직렬화
       4.1.7.  토큰 직렬화
       4.1.8.  바이트 시퀀스 직렬화
       4.1.9.  불리언 직렬화
     4.2.  구조화된 필드 파싱
       4.2.1.  리스트 파싱
       4.2.2.  딕셔너리 파싱
       4.2.3.  아이템 파싱
       4.2.4.  정수 또는 소수 파싱
       4.2.5.  문자열 파싱
       4.2.6.  토큰 파싱
       4.2.7.  바이트 시퀀스 파싱
       4.2.8.  불리언 파싱
   5.  IANA 고려사항
   6.  보안 고려사항
   7.  참고문헌
     7.1.  규범적 참고문헌
     7.2.  정보성 참고문헌
   부록 A.  자주 묻는 질문
     A.1.  왜 JSON이 아닌가?
   부록 B.  구현 노트
   감사의 글
   저자 주소

1.  소개

   새로운 HTTP 헤더(및 트레일러) 필드의 구문을 명세하는 것은 부담스러운 작업이다.
   [RFC7231]의 8.3.1절에 있는 지침이 있더라도, 장차 HTTP 필드를 작성하려는
   사람에게는 많은 결정 사항 -- 그리고 함정 -- 이 존재한다.

   필드가 정의되고 나면, 각 필드 값이 공통 구문처럼 보이는 것을 조금씩 다르게
   처리하기 때문에, 맞춤형 파서와 직렬화기를 종종 작성해야 한다.

   이 문서는 이러한 문제들을 해결하기 위해 새로운 HTTP 필드 값의 정의에 사용할 수
   있는 일련의 공통 데이터 구조를 도입한다. 특히, 이를 위한 일반적이고 추상적인
   모델과 함께, HTTP [RFC7230] 헤더 및 트레일러 필드에서 그 모델을 표현하기 위한
   구체적인 직렬화 방식을 정의한다.

   "구조화된 헤더(Structured Header)" 또는 "구조화된 트레일러(Structured
   Trailer)"(둘 중 어느 쪽이든 될 수 있다면 "구조화된 필드(Structured Field)")로
   정의된 HTTP 필드는 이 명세에서 정의한 타입을 사용하여 자신의 구문과 기본 처리
   규칙을 정의하며, 그럼으로써 명세 작성자에 의한 정의와 구현체에 의한 처리를
   모두 단순화한다.

   추가로, HTTP의 향후 버전들은 이러한 구조의 추상 모델에 대한 대안적인 직렬화를
   정의할 수 있어, 그 모델을 사용하는 필드들이 재정의 없이도 더 효율적으로 전송될
   수 있도록 한다.

   이 문서의 목표는 기존 HTTP 필드의 구문을 재정의하는 것이 아님에 유의하라.
   여기서 기술하는 메커니즘은 이를 명시적으로 선택(opt into)한 필드에만 사용되도록
   의도되었다.

   2절은 구조화된 필드를 명세하는 방법을 기술한다.

   3절은 구조화된 필드에 사용될 수 있는 다수의 추상 데이터 타입을 정의한다.

   이러한 추상 타입들은 4절에서 기술하는 알고리즘을 사용하여 HTTP 필드 값으로
   직렬화하고 그로부터 파싱할 수 있다.

1.1.  의도적으로 엄격한 처리

   이 명세는 단계별 알고리즘을 사용하여 엄격한 파싱 및 직렬화 동작을 의도적으로
   정의한다. 정의된 유일한 오류 처리는 연산 전체를 실패시키는 것이다.

   이는 충실한 구현과 우수한 상호운용성을 장려하도록 설계되었다. 따라서 입력에
   대해 더 관대하게 굴어 도움을 주려고 시도하는 구현체는 오히려 상호운용성을
   악화시킬 것인데, 이는 다른 구현체들에게도 유사한(그러나 미묘하게 다를 가능성이
   높은) 우회책을 구현하라는 압력을 만들기 때문이다.

   다시 말해, 엄격한 처리는 이 명세의 의도적인 기능이다. 이는 비순응(non-
   conformant) 입력이 생산자에 의해 일찍 발견되고 수정될 수 있도록 하며, 그렇지
   않으면 발생할 수 있는 상호운용성 및 보안 문제를 모두 방지한다.

   이러한 엄격함의 결과로, 만약 한 필드가 여러 당사자(예: 중개자나 송신자 내부의
   서로 다른 컴포넌트)에 의해 덧붙여진다면, 한 당사자의 값에 있는 오류가 전체 필드
   값의 파싱을 실패하게 만들 가능성이 높음에 유의하라.

1.2.  표기 규약

   이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL"은
   여기에 표시된 것처럼 모두 대문자로 나타나는 경우, 그리고 오직 그 경우에만 BCP
   14 [RFC2119] [RFC8174]에 기술된 대로 해석되어야 한다.

   이 문서는 파싱 및 직렬화 동작을 명세하기 위해 알고리즘을 사용하며, HTTP 헤더
   필드에서 기대되는 구문을 설명하기 위해 [RFC5234]의 확장 배커스-나우어 형식
   (Augmented Backus-Naur Form, ABNF) 표기법을 사용한다. 그렇게 하면서 [RFC5234]의
   VCHAR, SP, DIGIT, ALPHA, DQUOTE 규칙을 사용한다. 또한 [RFC7230]의 tchar 및 OWS
   규칙도 포함한다.

   HTTP 필드로부터 파싱할 때, 구현체는 알고리즘을 따르는 것과 구별할 수 없는
   동작을 해야 한다(MUST). 파싱 알고리즘과 ABNF 사이에 불일치가 있다면, 명세된
   알고리즘이 우선한다.

   HTTP 필드로의 직렬화에 대해서는, ABNF가 그들의 기대되는 와이어(wire) 표현을
   설명하며, 알고리즘은 이를 생성하는 권장 방식을 정의한다. 구현체는 출력이 4.2절에
   기술된 파싱 알고리즘에 의해 여전히 올바르게 처리되는 한 명세된 동작에서 벗어날
   수 있다(MAY).

2.  새로운 구조화된 필드 정의하기

   HTTP 필드를 구조화된 필드로 명세하기 위해, 그 저자는 다음을 해야 한다:

   *  이 명세를 규범적으로 참조한다. 필드의 수신자와 생성자는 이 문서의 요구사항이
      적용되고 있음을 알아야 한다.

   *  필드가 구조화된 헤더인지(즉, 헤더 섹션에서만 사용 가능 -- 일반적인 경우),
      구조화된 트레일러인지(트레일러 섹션에서만), 또는 구조화된 필드인지(둘 다)를
      식별한다.

   *  필드 값의 타입을 명세한다. 리스트(3.1절), 딕셔너리(3.2절), 또는 아이템
      (3.3절) 중 하나이다.

   *  필드 값의 의미론(semantics)을 정의한다.

   *  필드 값에 대한 추가적인 제약 조건과, 그러한 제약 조건이 위반될 때의 결과를
      명세한다.

   일반적으로 이는 필드 정의가 최상위 타입 -- 리스트, 딕셔너리, 또는 아이템 -- 을
   명세하고, 그 다음 허용되는 타입들과 그에 대한 제약 조건을 정의함을 의미한다.
   예를 들어, 리스트로 정의된 헤더는 모든 멤버가 정수일 수도 있고 타입의 혼합일
   수도 있다. 아이템으로 정의된 헤더는 오직 문자열만 허용할 수도 있으며, 추가로
   "Q" 문자로 시작하는 문자열, 또는 소문자 문자열만 허용할 수도 있다. 마찬가지로,
   내부 리스트(3.1.1절)는 필드 정의가 명시적으로 그것을 허용할 때에만 유효하다.

   파싱이 실패하면, 전체 필드가 무시된다(4.2절 참조). 대부분의 상황에서, 필드별
   제약 조건을 위반하는 것도 동일한 효과를 가져야 한다. 따라서 어떤 헤더가
   아이템이며 정수일 것을 요구하도록 정의되었으나 문자열을 수신한 경우, 그 필드는
   기본적으로 무시된다. 필드가 다른 오류 처리를 요구한다면, 이는 명시적으로
   명세되어야 한다.

   아이템과 내부 리스트는 모두 확장성 메커니즘으로 파라미터를 허용한다. 이는 필요할
   경우 나중에 값이 더 많은 정보를 수용하도록 확장될 수 있음을 의미한다. 전방
   호환성(forward compatibility)을 보존하기 위해, 필드 명세가 인식되지 않은
   파라미터의 존재를 오류 조건으로 정의하는 것은 권장되지 않는다.

   이 확장성을 향후에도 확실히 이용 가능하게 하고, 소비자가 완전한 파서 구현을
   사용하도록 장려하기 위해, 필드 정의는 송신자가 "그리스(grease)" 파라미터를
   추가하도록 명세할 수 있다. 명세는 정의된 패턴에 맞는 모든 파라미터가 이 용도를
   위해 예약되어 있다고 규정한 다음, 일부 비율의 요청에 그것들을 전송하도록 장려할
   수 있다. 이는 수신자가 파라미터를 고려하지 않는 파서를 작성하는 것을 막는 데에
   도움이 된다.

   딕셔너리를 사용하는 명세 또한 알려지지 않은 멤버의 존재 -- 그리고 그와 연관된
   값 및 타입 -- 가 무시되도록 요구함으로써 전방 호환성을 확보할 수 있다. 후속
   명세들은 그러면 추가 멤버를 추가하고, 적절히 그에 대한 제약 조건을 명세할 수
   있다.

   그러면 구조화된 필드에 대한 확장은, 확장을 이해하는 수신자가 그 확장이 정의하는
   값에 대한 제약 조건이 충족되지 않으면 전체 필드 값을 무시하도록 요구할 수 있다.

   필드 정의는 이 명세의 요구사항을 완화할 수 없는데, 그렇게 하면 일반 소프트웨어에
   의한 처리가 불가능해지기 때문이다. 필드 정의는 오직 추가적인 제약 조건만을
   더할 수 있다(예: 정수와 소수의 수치 범위, 문자열과 토큰의 형식, 딕셔너리 값에서
   허용되는 타입, 또는 리스트의 아이템 수에 대한 제약). 마찬가지로, 필드 정의는 이
   명세를 전체 필드 값에 대해서만 사용할 수 있으며, 그 일부에 대해서는 사용할 수
   없다.

   이 명세는 구현체가 지원하는 다양한 구조의 길이 또는 개수에 대한 최소값을
   정의한다. 대부분의 경우 최대 크기는 명세하지 않으나, 저자들은 HTTP 구현체가
   개별 필드의 크기, 필드의 총 개수, 그리고/또는 전체 헤더 또는 트레일러 섹션의
   크기에 다양한 제한을 부과한다는 점을 인지해야 한다.

   명세는 필드 이름을 적절히 "구조화된 헤더 이름(structured header name)",
   "구조화된 트레일러 이름(structured trailer name)", 또는 "구조화된 필드 이름
   (structured field name)"으로 지칭할 수 있다. 마찬가지로, 필요에 따라 필드 값을
   "구조화된 헤더 값(structured header value)", "구조화된 트레일러 값(structured
   trailer value)", 또는 "구조화된 필드 값(structured field value)"으로 지칭할 수
   있다. 필드 정의는 이 명세에서 정의한 "sf-"로 시작하는 ABNF 규칙을 사용하도록
   장려된다. 이 명세의 다른 규칙들은 필드 정의에서 사용되도록 의도되지 않았다.

   예를 들어, 가상의 Foo-Example 헤더 필드는 다음과 같이 명세될 수 있다:

   |  42.  Foo-Example 헤더
   |  
   |  Foo-Example HTTP 헤더 필드는 메시지가 얼마나 많은 Foo를 가지고 있는지에
   |  관한 정보를 전달한다.
   |  
   |  Foo-Example은 아이템 구조화된 헤더 [RFC8941]이다. 그 값은 정수(Integer,
   |  [RFC8941]의 3.3.1절)여야 한다(MUST). 그 ABNF는 다음과 같다:
   |  
   |        Foo-Example = sf-integer
   |  
   |  그 값은 메시지 내의 Foo의 양을 나타내며, 0과 10 사이(양 끝값 포함)여야
   |  한다(MUST). 다른 값들은 전체 헤더 필드가 무시되도록 해야 한다(MUST).
   |  
   |  다음 파라미터가 정의된다:
   |  *  키가 "foourl"이고 값이 문자열(String, [RFC8941]의 3.3.3절)인 파라미터로,
   |     메시지에 대한 Foo URL을 전달한다. 처리 요구사항은 아래를 참조하라.
   |  
   |  "foourl"은 URI 참조(URI-reference, [RFC3986]의 4.1절)를 포함한다. 그 값이
   |  유효한 URI 참조가 아니면, 전체 헤더 필드가 무시되어야 한다(MUST). 그 값이
   |  상대 참조(relative reference, [RFC3986]의 4.2절)이면, 사용되기 전에 해석
   |  ([RFC3986]의 5절)되어야 한다(MUST).
   |  
   |  예를 들면:
   |  
   |        Foo-Example: 2; foourl="https://foo.example.com/"

3.  구조화된 데이터 타입

   이 절은 구조화된 필드를 위한 추상 타입들을 정의한다. 제공된 ABNF는 HTTP 필드
   값에서의 와이어 상의 형식을 나타낸다.

   요약하면:

   *  HTTP 필드가 정의될 수 있는 세 가지 최상위 타입이 있다: 리스트, 딕셔너리,
      아이템.

   *  리스트와 딕셔너리는 컨테이너이다. 그들의 멤버는 아이템이거나 내부 리스트
      (이는 그 자체로 아이템의 배열)일 수 있다.

   *  아이템과 내부 리스트는 모두 키/값 쌍으로 파라미터화(Parameterized)될 수 있다.

3.1.  리스트(Lists)

   리스트는 0개 이상의 멤버로 이루어진 배열이며, 각 멤버는 아이템(3.3절) 또는 내부
   리스트(3.1.1절)일 수 있고, 둘 다 파라미터화(3.1.2절)될 수 있다.

   HTTP 필드에서 리스트의 ABNF는 다음과 같다:

   sf-list       = list-member *( OWS "," OWS list-member )
   list-member   = sf-item / inner-list

   각 멤버는 쉼표와 선택적 공백(whitespace)으로 구분된다. 예를 들어, 값이 토큰의
   리스트로 정의된 필드는 다음과 같이 보일 수 있다:

   Example-List: sugar, tea, rum

   빈 리스트는 필드를 전혀 직렬화하지 않음으로써 표시된다. 이는 리스트로 정의된
   필드가 기본 빈 값(default empty value)을 가짐을 함의한다.

   리스트는 [RFC7230]의 3.2.2절에 따라 동일한 헤더 또는 트레일러 섹션의 여러 줄에
   걸쳐 멤버를 분할할 수 있음에 유의하라. 예를 들어, 다음은 동등하다:

   Example-List: sugar, tea, rum

   그리고

   Example-List: sugar, tea
   Example-List: rum

   그러나, 리스트의 개별 멤버는 여러 줄에 걸쳐 안전하게 분할될 수 없다. 자세한
   내용은 4.2절을 참조하라.

   파서는 적어도 1024개의 멤버를 포함하는 리스트를 지원해야 한다(MUST). 필드
   명세는 필요에 따라 개별 리스트 값의 타입과 개수(cardinality)를 제약할 수 있다.

3.1.1.  내부 리스트(Inner Lists)

   내부 리스트는 0개 이상의 아이템(3.3절)으로 이루어진 배열이다. 개별 아이템과 내부
   리스트 자체 모두 파라미터화(3.1.2절)될 수 있다.

   내부 리스트의 ABNF는 다음과 같다:

   inner-list    = "(" *SP [ sf-item *( 1*SP sf-item ) *SP ] ")"
                   parameters

   내부 리스트는 둘러싸는 괄호로 표시되며, 그 값들은 하나 이상의 공백으로
   구분된다. 값이 문자열의 내부 리스트의 리스트로 정의된 필드는 다음과 같이 보일
   수 있다:

   Example-List: ("foo" "bar"), ("baz"), ("bat" "one"), ()

   이 예시에서 마지막 멤버는 빈 내부 리스트임에 유의하라.

   값이 두 수준 모두에서 파라미터를 갖는 내부 리스트의 리스트로 정의된 헤더 필드는
   다음과 같이 보일 수 있다:

   Example-List: ("foo"; a=1;b=2);lvl=5, ("bar" "baz");lvl=1

   파서는 적어도 256개의 멤버를 포함하는 내부 리스트를 지원해야 한다(MUST). 필드
   명세는 필요에 따라 개별 내부 리스트 멤버의 타입과 개수를 제약할 수 있다.

3.1.2.  파라미터(Parameters)

   파라미터는 아이템(3.3절) 또는 내부 리스트(3.1.1절)와 연관된, 키-값 쌍의 순서
   있는 맵(ordered map)이다. 키는 그것이 나타나는 파라미터의 범위 내에서 고유하며,
   값은 베어 아이템(bare item)이다(즉, 그것들 자체는 파라미터화될 수 없다. 3.3절
   참조).

   구현체는 인덱스로도 키로도 파라미터에 대한 접근을 제공해야 한다(MUST). 명세는
   그들에 접근하는 두 수단 중 어느 쪽이든 사용할 수 있다(MAY).

   파라미터의 ABNF는 다음과 같다:

   parameters    = *( ";" *SP parameter )
   parameter     = param-key [ "=" param-value ]
   param-key     = key
   key           = ( lcalpha / "*" )
                   *( lcalpha / DIGIT / "_" / "-" / "." / "*" )
   lcalpha       = %x61-7A ; a-z
   param-value   = bare-item

   파라미터는 직렬화된 순서대로 정렬되며, 파라미터 키는 대문자를 포함할 수 없음에
   유의하라. 파라미터는 그것의 아이템 또는 내부 리스트, 그리고 다른 파라미터들과
   세미콜론으로 구분된다. 예를 들면:

   Example-List: abc;a=1;b=2; cde_456, (ghi;jk=4 l);q="9";r=w

   값이 불리언(3.3.6절 참조) true인 파라미터는 직렬화될 때 그 값을 생략해야 한다
   (MUST). 예를 들어, 여기서 "a" 파라미터는 true이고, "b" 파라미터는 false이다:

   Example-Integer: 1; a; b=?0

   이 요구사항은 직렬화에만 적용됨에 유의하라. 파서는 여전히 파라미터에서 true 값이
   나타날 때 그것을 올바르게 처리하도록 요구된다.

   파서는 한 아이템 또는 내부 리스트에 적어도 256개의 파라미터를 지원해야 하며
   (MUST), 적어도 64개 문자의 파라미터 키를 지원해야 한다(MUST). 필드 명세는
   필요에 따라 개별 파라미터의 순서뿐만 아니라 그 값들의 타입도 제약할 수 있다.

3.2.  딕셔너리(Dictionaries)

   딕셔너리는 키-값 쌍의 순서 있는 맵으로, 키는 짧은 텍스트 문자열이고 값은
   아이템(3.3절) 또는 아이템의 배열이며, 둘 다 파라미터화(3.1.2절)될 수 있다.
   0개 이상의 멤버가 있을 수 있으며, 그들의 키는 그것들이 나타나는 딕셔너리의
   범위 내에서 고유하다.

   구현체는 인덱스로도 키로도 딕셔너리에 대한 접근을 제공해야 한다(MUST). 명세는
   멤버에 접근하는 두 수단 중 어느 쪽이든 사용할 수 있다(MAY).

   딕셔너리의 ABNF는 다음과 같다:

   sf-dictionary  = dict-member *( OWS "," OWS dict-member )
   dict-member    = member-key ( parameters / ( "=" member-value ))
   member-key     = key
   member-value   = sf-item / inner-list

   멤버는 직렬화된 순서대로 정렬되며 쉼표와 선택적 공백으로 구분된다. 멤버 키는
   대문자를 포함할 수 없다. 키와 값은 "="로 구분된다(공백 없이). 예를 들면:

   Example-Dict: en="Applepie", da=:w4ZibGV0w6ZydGU=:

   이 예시에서 마지막 "="는 바이트 시퀀스의 포함으로 인한 것임에 유의하라. 3.3.5절을
   참조하라.

   값이 불리언(3.3.6절 참조) true인 멤버는 직렬화될 때 그 값을 생략해야 한다(MUST).
   예를 들어, 여기서 "b"와 "c"는 모두 true이다:

   Example-Dict: a=?0, b, c; foo=bar

   이 요구사항은 직렬화에만 적용됨에 유의하라. 파서는 여전히 딕셔너리 값에서 true
   불리언 값이 나타날 때 그것을 올바르게 처리하도록 요구된다.

   값이 토큰의 내부 리스트인 멤버를 갖는 딕셔너리:

   Example-Dict: rating=1.5, feelings=(joy sadness)

   아이템과 내부 리스트가 혼합되어 있고, 일부는 파라미터를 갖는 딕셔너리:

   Example-Dict: a=(1 2), b=3, c=4;aa=bb, d=(5 6);valid

   리스트와 마찬가지로, 빈 딕셔너리는 전체 필드를 생략함으로써 표현된다. 이는
   딕셔너리로 정의된 필드가 기본 빈 값을 가짐을 함의한다.

   일반적으로, 필드 명세는 키별로 개별 멤버에 허용되는 타입(들)뿐만 아니라 그들의
   존재가 필수인지 선택인지를 명세함으로써 딕셔너리의 의미론을 정의한다. 수신자는
   필드의 명세가 명시적으로 그것들을 금지하지 않는 한, 정의되지 않았거나 알려지지
   않은 키를 갖는 멤버를 무시해야 한다(MUST).

   딕셔너리는 동일한 헤더 또는 트레일러 섹션의 여러 줄에 걸쳐 멤버를 분할할 수
   있음에 유의하라. 예를 들어, 다음은 동등하다:

   Example-Dict: foo=1, bar=2

   그리고

   Example-Dict: foo=1
   Example-Dict: bar=2

   그러나, 딕셔너리의 개별 멤버는 여러 줄에 걸쳐 안전하게 분할될 수 없다. 자세한
   내용은 4.2절을 참조하라.

   파서는 적어도 1024개의 키/값 쌍을 포함하는 딕셔너리와 적어도 64개 문자의 키를
   지원해야 한다(MUST). 필드 명세는 필요에 따라 개별 딕셔너리 멤버의 순서뿐만
   아니라 그 값들의 타입도 제약할 수 있다.

3.3.  아이템(Items)

   아이템은 정수(3.3.1절), 소수(3.3.2절), 문자열(3.3.3절), 토큰(3.3.4절), 바이트
   시퀀스(3.3.5절), 또는 불리언(3.3.6절)일 수 있다. 아이템은 연관된 파라미터
   (3.1.2절)를 가질 수 있다.

   아이템의 ABNF는 다음과 같다:

   sf-item   = bare-item parameters
   bare-item = sf-integer / sf-decimal / sf-string / sf-token
               / sf-binary / sf-boolean

   예를 들어, 정수인 아이템으로 정의된 헤더 필드는 다음과 같이 보일 수 있다:

   Example-Integer: 5

   또는 파라미터를 가지면:

   Example-Integer: 5; foo=bar

3.3.1.  정수(Integers)

   정수는 IEEE 754 호환성 [IEEE754]을 위해 -999,999,999,999,999에서
   999,999,999,999,999까지(양 끝값 포함, 즉 부호 있는 최대 15자리)의 범위를 가진다.

   정수의 ABNF는 다음과 같다:

   sf-integer = ["-"] 1*15DIGIT

   예를 들면:

   Example-Integer: 42

   15자리보다 큰 정수는 다양한 방식으로 지원될 수 있다. 예를 들어, 문자열(3.3.3절),
   바이트 시퀀스(3.3.5절)를 사용하거나, 스케일링 인자(scaling factor)로 작동하는
   정수에 대한 파라미터를 사용할 수 있다.

   선행 0(예: "0002", "-01")과 부호 있는 0("-0")으로 정수를 직렬화하는 것이
   가능하지만, 이러한 구별은 구현체에 의해 보존되지 않을 수 있다.

   정수에서의 쉼표는 이 절의 산문에서 가독성을 위해서만 사용된다는 점에 유의하라.
   그것들은 와이어 형식에서 유효하지 않다.

3.3.2.  소수(Decimals)

   소수는 정수 부분과 소수 부분을 가진 숫자이다. 정수 부분은 최대 12자리이고, 소수
   부분은 최대 3자리이다.

   소수의 ABNF는 다음과 같다:

   sf-decimal  = ["-"] 1*12DIGIT "." 1*3DIGIT

   예를 들어, 값이 소수로 정의된 헤더는 다음과 같이 보일 수 있다:

   Example-Decimal: 4.5

   선행 0(예: "0002.5", "-01.334"), 후행 0(예: "5.230", "-0.40"), 그리고 부호 있는
   0(예: "-0.0")으로 소수를 직렬화하는 것이 가능하지만, 이러한 구별은 구현체에 의해
   보존되지 않을 수 있다.

   직렬화 알고리즘(4.1.5절)은 소수 부분에 3자리보다 많은 정밀도를 가진 입력을
   반올림함에 유의하라. 대안적인 반올림 전략이 요구된다면, 이는 직렬화 전에
   일어나도록 헤더 정의에 의해 명세되어야 한다.

3.3.3.  문자열(Strings)

   문자열은 0개 이상의 출력 가능한 ASCII [RFC0020] 문자(즉, %x20에서 %x7E 범위)이다.
   이는 탭, 개행(newline), 캐리지 리턴(carriage return) 등을 제외함에 유의하라.

   문자열의 ABNF는 다음과 같다:

   sf-string = DQUOTE *chr DQUOTE
   chr       = unescaped / escaped
   unescaped = %x20-21 / %x23-5B / %x5D-7E
   escaped   = "\" ( DQUOTE / "\" )

   문자열은 큰따옴표로 구분되며, 큰따옴표와 백슬래시를 이스케이프하기 위해
   백슬래시("\")를 사용한다. 예를 들면:

   Example-String: "hello world"

   문자열은 구분자로 DQUOTE만 사용함에 유의하라. 작은따옴표는 문자열을 구분하지
   않는다. 또한, 오직 DQUOTE와 "\"만 이스케이프될 수 있다. "\" 다음에 오는 다른
   문자들은 파싱이 실패하도록 해야 한다(MUST).

   유니코드는 문자열에서 직접 지원되지 않는데, 이는 다수의 상호운용성 문제를
   야기하며, 또한 -- 소수의 예외를 제외하고 -- 필드 값이 그것을 요구하지 않기
   때문이다.

   필드 값이 비-ASCII 콘텐츠를 전달하는 것이 필요한 경우, 문자 인코딩(가급적 UTF-8
   [STD63])과 함께 바이트 시퀀스(3.3.5절)를 명세할 수 있다.

   파서는 (어떠한 디코딩 이후에) 적어도 1024개 문자의 문자열을 지원해야 한다(MUST).

3.3.4.  토큰(Tokens)

   토큰은 짧은 텍스트 단어이다. 그들의 추상 모델은 HTTP 필드 값 직렬화에서의 표현과
   동일하다.

   토큰의 ABNF는 다음과 같다:

   sf-token = ( ALPHA / "*" ) *( tchar / ":" / "/" )

   예를 들면:

   Example-Token: foo123/456

   파서는 적어도 512개 문자의 토큰을 지원해야 한다(MUST).

   토큰은 [RFC7230]에 정의된 "token" ABNF 규칙과 동일한 문자를 허용하되, 첫 번째
   문자가 ALPHA 또는 "*"여야 한다는 점과, 이후 문자에서 ":"와 "/"도 허용된다는
   예외가 있음에 유의하라.

3.3.5.  바이트 시퀀스(Byte Sequences)

   바이트 시퀀스는 구조화된 필드에서 전달될 수 있다.

   바이트 시퀀스의 ABNF는 다음과 같다:

   sf-binary = ":" *(base64) ":"
   base64    = ALPHA / DIGIT / "+" / "/" / "="

   바이트 시퀀스는 콜론으로 구분되며 base64([RFC4648]의 4절)를 사용하여
   인코딩된다. 예를 들면:

   Example-ByteSequence: :cHJldGVuZCB0aGlzIGlzIGJpbmFyeSBjb250ZW50Lg==:

   파서는 디코딩 후 적어도 16384 옥텟의 바이트 시퀀스를 지원해야 한다(MUST).

3.3.6.  불리언(Booleans)

   불리언 값은 구조화된 필드에서 전달될 수 있다.

   불리언의 ABNF는 다음과 같다:

   sf-boolean = "?" boolean
   boolean    = "0" / "1"

   불리언은 선행 "?" 문자 뒤에 true 값을 위한 "1" 또는 false를 위한 "0"이 따라오는
   것으로 나타낸다. 예를 들면:

   Example-Boolean: ?1

   딕셔너리(3.2절)와 파라미터(3.1.2절) 값에서는, 불리언 true가 값을 생략함으로써
   나타내어진다는 점에 유의하라.

4.  HTTP에서 구조화된 필드 다루기

   이 절은 텍스트 형식의 HTTP 필드 값 및 그와 호환되는 다른 인코딩(예: HPACK
   [RFC7541] 압축 이전의 HTTP/2 [RFC7540])에서 구조화된 필드를 직렬화하고 파싱하는
   방법을 정의한다.

4.1.  구조화된 필드 직렬화

   이 명세에 정의된 구조가 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.  구조가 딕셔너리 또는 리스트이고 그 값이 비어 있으면(즉, 멤버가 없으면),
       필드를 전혀 직렬화하지 않는다(즉, field-name과 field-value를 모두 생략한다).

   2.  구조가 리스트이면, output_string을 그 구조에 대해 리스트 직렬화(4.1.1절)를
       실행한 결과로 둔다.

   3.  그렇지 않고, 구조가 딕셔너리이면, output_string을 그 구조에 대해 딕셔너리
       직렬화(4.1.2절)를 실행한 결과로 둔다.

   4.  그렇지 않고, 구조가 아이템이면, output_string을 그 구조에 대해 아이템
       직렬화(4.1.3절)를 실행한 결과로 둔다.

   5.  그렇지 않으면, 직렬화를 실패시킨다.

   6.  ASCII 인코딩 [RFC0020]을 사용하여 output_string을 바이트 배열로 변환한 것을
       반환한다.

4.1.1.  리스트 직렬화

   (member_value, parameters) 튜플의 배열을 input_list로 주어졌을 때, HTTP 필드
   값에 사용하기 적합한 ASCII 문자열을 반환한다.

   1.  output을 빈 문자열로 둔다.

   2.  input_list의 각 (member_value, parameters)에 대해:

       1.  member_value가 배열이면, (member_value, parameters)에 대해 내부 리스트
           직렬화(4.1.1.1절)를 실행한 결과를 output에 덧붙인다.

       2.  그렇지 않으면, (member_value, parameters)에 대해 아이템 직렬화(4.1.3절)를
           실행한 결과를 output에 덧붙인다.

       3.  input_list에 더 많은 member_value가 남아 있으면:

           1.  output에 ","를 덧붙인다.

           2.  output에 단일 SP를 덧붙인다.

   3.  output을 반환한다.

4.1.1.1.  내부 리스트 직렬화

   (member_value, parameters) 튜플의 배열을 inner_list로, 파라미터를
   list_parameters로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII 문자열을
   반환한다.

   1.  output을 문자열 "("로 둔다.

   2.  inner_list의 각 (member_value, parameters)에 대해:

       1.  (member_value, parameters)에 대해 아이템 직렬화(4.1.3절)를 실행한
           결과를 output에 덧붙인다.

       2.  inner_list에 더 많은 값이 남아 있으면, output에 단일 SP를 덧붙인다.

   3.  output에 ")"를 덧붙인다.

   4.  list_parameters에 대해 파라미터 직렬화(4.1.1.2절)를 실행한 결과를 output에
       덧붙인다.

   5.  output을 반환한다.

4.1.1.2.  파라미터 직렬화

   순서 있는 딕셔너리를 input_parameters로 주어졌을 때(각 멤버는 param_key와
   param_value를 가짐), HTTP 필드 값에 사용하기 적합한 ASCII 문자열을 반환한다.

   1.  output을 빈 문자열로 둔다.

   2.  input_parameters에서 값이 param_value인 각 param_key에 대해:

       1.  output에 ";"를 덧붙인다.

       2.  param_key에 대해 키 직렬화(4.1.1.3절)를 실행한 결과를 output에
           덧붙인다.

       3.  param_value가 불리언 true가 아니면:

           1.  output에 "="를 덧붙인다.

           2.  param_value에 대해 베어 아이템 직렬화(4.1.3.1절)를 실행한 결과를
               output에 덧붙인다.

   3.  output을 반환한다.

4.1.1.3.  키 직렬화

   키를 input_key로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII 문자열을
   반환한다.

   1.  input_key를 ASCII 문자의 시퀀스로 변환한다. 변환이 실패하면, 직렬화를
       실패시킨다.

   2.  input_key가 lcalpha, DIGIT, "_", "-", ".", "*"에 없는 문자를 포함하면,
       직렬화를 실패시킨다.

   3.  input_key의 첫 번째 문자가 lcalpha 또는 "*"가 아니면, 직렬화를 실패시킨다.

   4.  output을 빈 문자열로 둔다.

   5.  output에 input_key를 덧붙인다.

   6.  output을 반환한다.

4.1.2.  딕셔너리 직렬화

   순서 있는 딕셔너리를 input_dictionary로 주어졌을 때(각 멤버는 member_key와
   튜플 값 (member_value, parameters)를 가짐), HTTP 필드 값에 사용하기 적합한
   ASCII 문자열을 반환한다.

   1.  output을 빈 문자열로 둔다.

   2.  input_dictionary에서 값이 (member_value, parameters)인 각 member_key에 대해:

       1.  멤버의 member_key에 대해 키 직렬화(4.1.1.3절)를 실행한 결과를 output에
           덧붙인다.

       2.  member_value가 불리언 true이면:

           1.  parameters에 대해 파라미터 직렬화(4.1.1.2절)를 실행한 결과를
               output에 덧붙인다.

       3.  그렇지 않으면:

           1.  output에 "="를 덧붙인다.

           2.  member_value가 배열이면, (member_value, parameters)에 대해 내부
               리스트 직렬화(4.1.1.1절)를 실행한 결과를 output에 덧붙인다.

           3.  그렇지 않으면, (member_value, parameters)에 대해 아이템 직렬화
               (4.1.3절)를 실행한 결과를 output에 덧붙인다.

       4.  input_dictionary에 더 많은 멤버가 남아 있으면:

           1.  output에 ","를 덧붙인다.

           2.  output에 단일 SP를 덧붙인다.

   3.  output을 반환한다.

4.1.3.  아이템 직렬화

   아이템을 bare_item으로, 파라미터를 item_parameters로 주어졌을 때, HTTP 필드
   값에 사용하기 적합한 ASCII 문자열을 반환한다.

   1.  output을 빈 문자열로 둔다.

   2.  bare_item에 대해 베어 아이템 직렬화(4.1.3.1절)를 실행한 결과를 output에
       덧붙인다.

   3.  item_parameters에 대해 파라미터 직렬화(4.1.1.2절)를 실행한 결과를 output에
       덧붙인다.

   4.  output을 반환한다.

4.1.3.1.  베어 아이템 직렬화

   아이템을 input_item으로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.  input_item이 정수이면, input_item에 대해 정수 직렬화(4.1.4절)를 실행한
       결과를 반환한다.

   2.  input_item이 소수이면, input_item에 대해 소수 직렬화(4.1.5절)를 실행한
       결과를 반환한다.

   3.  input_item이 문자열이면, input_item에 대해 문자열 직렬화(4.1.6절)를 실행한
       결과를 반환한다.

   4.  input_item이 토큰이면, input_item에 대해 토큰 직렬화(4.1.7절)를 실행한
       결과를 반환한다.

   5.  input_item이 바이트 시퀀스이면, input_item에 대해 바이트 시퀀스 직렬화
       (4.1.8절)를 실행한 결과를 반환한다.

   6.  input_item이 불리언이면, input_item에 대해 불리언 직렬화(4.1.9절)를 실행한
       결과를 반환한다.

   7.  그렇지 않으면, 직렬화를 실패시킨다.

4.1.4.  정수 직렬화

   정수를 input_integer로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.  input_integer가 -999,999,999,999,999에서 999,999,999,999,999까지(양 끝값
       포함)의 범위에 있는 정수가 아니면, 직렬화를 실패시킨다.

   2.  output을 빈 문자열로 둔다.

   3.  input_integer가 0보다 작으면(같지는 않고), output에 "-"를 덧붙인다.

   4.  input_integer의 수치 값을 십진수 숫자만 사용하여 10진법으로 표현한 것을
       output에 덧붙인다.

   5.  output을 반환한다.

4.1.5.  소수 직렬화

   소수를 input_decimal로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.   input_decimal이 소수가 아니면, 직렬화를 실패시킨다.

   2.   input_decimal이 소수점 오른쪽에 유효 숫자가 3자리보다 많으면, 세 자리로
        반올림하되, 마지막 자릿수를 가장 가까운 값으로 반올림하며, 등거리에 있으면
        짝수 값으로 반올림한다.

   3.   반올림 후 input_decimal이 소수점 왼쪽에 유효 숫자가 12자리보다 많으면,
        직렬화를 실패시킨다.

   4.   output을 빈 문자열로 둔다.

   5.   input_decimal이 0보다 작으면(같지는 않고), output에 "-"를 덧붙인다.

   6.   input_decimal의 정수 부분을 10진법(십진수 숫자만 사용)으로 표현한 것을
        output에 덧붙인다. 0이면 "0"을 덧붙인다.

   7.   output에 "."를 덧붙인다.

   8.   input_decimal의 소수 부분이 0이면, output에 "0"을 덧붙인다.

   9.   그렇지 않으면, input_decimal의 소수 부분의 유효 숫자를 10진법(십진수 숫자만
        사용)으로 표현한 것을 output에 덧붙인다.

   10.  output을 반환한다.

4.1.6.  문자열 직렬화

   문자열을 input_string으로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.  input_string을 ASCII 문자의 시퀀스로 변환한다. 변환이 실패하면, 직렬화를
       실패시킨다.

   2.  input_string이 %x00-1f 또는 %x7f-ff 범위(즉, VCHAR 또는 SP에 없는)의 문자를
       포함하면, 직렬화를 실패시킨다.

   3.  output을 문자열 DQUOTE로 둔다.

   4.  input_string의 각 문자 char에 대해:

       1.  char가 "\" 또는 DQUOTE이면:

           1.  output에 "\"를 덧붙인다.

       2.  output에 char를 덧붙인다.

   5.  output에 DQUOTE를 덧붙인다.

   6.  output을 반환한다.

4.1.7.  토큰 직렬화

   토큰을 input_token으로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.  input_token을 ASCII 문자의 시퀀스로 변환한다. 변환이 실패하면, 직렬화를
       실패시킨다.

   2.  input_token의 첫 번째 문자가 ALPHA 또는 "*"가 아니거나, 나머지 부분이
       tchar, ":", "/"에 없는 문자를 포함하면, 직렬화를 실패시킨다.

   3.  output을 빈 문자열로 둔다.

   4.  output에 input_token을 덧붙인다.

   5.  output을 반환한다.

4.1.8.  바이트 시퀀스 직렬화

   바이트 시퀀스를 input_bytes로 주어졌을 때, HTTP 필드 값에 사용하기 적합한
   ASCII 문자열을 반환한다.

   1.  input_bytes가 바이트의 시퀀스가 아니면, 직렬화를 실패시킨다.

   2.  output을 빈 문자열로 둔다.

   3.  output에 ":"를 덧붙인다.

   4.  아래의 요구사항을 고려하여, [RFC4648]의 4절에 따라 input_bytes를 base64
       인코딩한 결과를 덧붙인다.

   5.  output에 ":"를 덧붙인다.

   6.  output을 반환한다.

   인코딩된 데이터는 [RFC4648]의 3.2절에 따라 "="로 패딩되어야 한다(required).

   마찬가지로, 인코딩된 데이터는 구현상의 제약으로 인해 그렇게 하는 것이 불가능한
   경우가 아니라면, [RFC4648]의 3.5절에 따라 패딩 비트가 0으로 설정되어야 한다
   (SHOULD).

4.1.9.  불리언 직렬화

   불리언을 input_boolean으로 주어졌을 때, HTTP 필드 값에 사용하기 적합한 ASCII
   문자열을 반환한다.

   1.  input_boolean이 불리언이 아니면, 직렬화를 실패시킨다.

   2.  output을 빈 문자열로 둔다.

   3.  output에 "?"를 덧붙인다.

   4.  input_boolean이 true이면, output에 "1"을 덧붙인다.

   5.  input_boolean이 false이면, output에 "0"을 덧붙인다.

   6.  output을 반환한다.

4.2.  구조화된 필드 파싱

   수신 측 구현체가 구조화된 필드임이 알려진 HTTP 필드를 파싱할 때, 상호운용성
   또는 심지어 보안 문제를 야기할 수 있는 다수의 경계 사례(edge cases)가 존재하므로
   주의를 기울이는 것이 중요하다. 이 절은 그렇게 하기 위한 알고리즘을 명세한다.

   선택된 필드의 field-value를 나타내는 바이트 배열을 input_bytes로(그 필드가
   존재하지 않으면 비어 있음), 그리고 field_type을("dictionary", "list", "item"
   중 하나) 주어졌을 때, 파싱된 헤더 값을 반환한다.

   1.  input_bytes를 ASCII 문자열 input_string으로 변환한다. 변환이 실패하면,
       파싱을 실패시킨다.

   2.  input_string에서 선행 SP 문자들을 버린다.

   3.  field_type이 "list"이면, output을 input_string에 대해 리스트 파싱(4.2.1절)을
       실행한 결과로 둔다.

   4.  field_type이 "dictionary"이면, output을 input_string에 대해 딕셔너리 파싱
       (4.2.2절)을 실행한 결과로 둔다.

   5.  field_type이 "item"이면, output을 input_string에 대해 아이템 파싱(4.2.3절)을
       실행한 결과로 둔다.

   6.  input_string에서 선행 SP 문자들을 버린다.

   7.  input_string이 비어 있지 않으면, 파싱을 실패시킨다.

   8.  그렇지 않으면, output을 반환한다.

   input_bytes를 생성할 때, 파서는 [RFC7230]의 3.2.2절에 따라 필드 이름이 대소문자
   구분 없이 일치하는, 동일한 섹션(헤더 또는 트레일러)의 모든 필드 라인을 하나의
   쉼표로 구분된 field-value로 결합해야 한다(MUST). 이는 전체 필드 값이 올바르게
   처리되도록 보장한다.

   리스트와 딕셔너리의 경우, 최상위 데이터 구조의 개별 멤버가 여러 헤더 인스턴스에
   걸쳐 분할되지 않는 한, 이는 필드의 모든 라인을 올바르게 연결하는 효과를 가진다.
   두 타입 모두에 대한 파싱 알고리즘은 탭 문자를 허용하는데, 이는 일부 구현체에서
   필드 라인을 결합하는 데 사용될 수 있기 때문이다.

   여러 필드 라인에 걸쳐 분할된 문자열은 예측 불가능한 결과를 가질 것인데, 이는
   하나 이상의 쉼표(선택적 공백과 함께)가 파서가 출력하는 문자열의 일부가 되기
   때문이다. 연결은 상류(upstream)의 중개자에 의해 수행될 수 있으므로, 직렬화기와
   파서가 모두 동일한 당사자의 통제 하에 있더라도 그 결과는 직렬화기나 파서의 통제
   하에 있지 않다.

   토큰, 정수, 소수, 바이트 시퀀스는 여러 필드 라인에 걸쳐 분할될 수 없는데, 삽입된
   쉼표가 파싱을 실패하게 만들기 때문이다.

   파서는 여러 필드 라인에 걸친 필드 값을 처리할 때, 그 라인들 중 하나가 해당
   필드로 파싱되지 않으면 실패할 수 있다(MAY). 예를 들어, sf-string으로 정의된
   Example-String 필드를 처리하는 파서는 다음 필드 섹션을 처리할 때 실패하는 것이
   허용된다:

   Example-String: "foo
   Example-String: bar"

   파싱이 실패하면 -- 다른 알고리즘을 호출할 때 실패하는 경우를 포함하여 -- 전체
   필드 값이 무시되어야 한다(MUST)(즉, 그 필드가 섹션에 존재하지 않는 것처럼
   취급된다). 이는 상호운용성과 안전성을 개선하기 위해 의도적으로 엄격하며, 이
   문서를 참조하는 명세들은 이 요구사항을 완화하는 것이 허용되지 않는다.

   이 요구사항은 필드를 파싱하지 않는 구현체에는 적용되지 않음에 유의하라. 예를
   들어, 중개자는 메시지를 전달하기 전에 실패하는 필드를 제거할 것이 요구되지
   않는다.

4.2.1.  리스트 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, (item_or_inner_list, parameters)
   튜플의 배열을 반환한다. input_string은 파싱된 값을 제거하도록 수정된다.

   1.  members를 빈 배열로 둔다.

   2.  input_string이 비어 있지 않은 동안:

       1.  input_string에 대해 아이템 또는 내부 리스트 파싱(4.2.1.1절)을 실행한
           결과를 members에 덧붙인다.

       2.  input_string에서 선행 OWS 문자들을 버린다.

       3.  input_string이 비어 있으면, members를 반환한다.

       4.  input_string의 첫 번째 문자를 소비한다. 그것이 ","가 아니면, 파싱을
           실패시킨다.

       5.  input_string에서 선행 OWS 문자들을 버린다.

       6.  input_string이 비어 있으면, 후행 쉼표가 있는 것이다. 파싱을 실패시킨다.

   3.  구조화된 데이터가 발견되지 않았다. members(비어 있음)를 반환한다.

4.2.1.1.  아이템 또는 내부 리스트 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, (item_or_inner_list, parameters)
   튜플을 반환한다. 여기서 item_or_inner_list는 단일 베어 아이템이거나
   (bare_item, parameters) 튜플의 배열일 수 있다. input_string은 파싱된 값을
   제거하도록 수정된다.

   1.  input_string의 첫 번째 문자가 "("이면, input_string에 대해 내부 리스트 파싱
       (4.2.1.2절)을 실행한 결과를 반환한다.

   2.  input_string에 대해 아이템 파싱(4.2.3절)을 실행한 결과를 반환한다.

4.2.1.2.  내부 리스트 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, (inner_list, parameters) 튜플을
   반환한다. 여기서 inner_list는 (bare_item, parameters) 튜플의 배열이다.
   input_string은 파싱된 값을 제거하도록 수정된다.

   1.  input_string의 첫 번째 문자를 소비한다. 그것이 "("가 아니면, 파싱을
       실패시킨다.

   2.  inner_list를 빈 배열로 둔다.

   3.  input_string이 비어 있지 않은 동안:

       1.  input_string에서 선행 SP 문자들을 버린다.

       2.  input_string의 첫 번째 문자가 ")"이면:

           1.  input_string의 첫 번째 문자를 소비한다.

           2.  parameters를 input_string에 대해 파라미터 파싱(4.2.3.2절)을 실행한
               결과로 둔다.

           3.  (inner_list, parameters) 튜플을 반환한다.

       3.  item을 input_string에 대해 아이템 파싱(4.2.3절)을 실행한 결과로 둔다.

       4.  item을 inner_list에 덧붙인다.

       5.  input_string의 첫 번째 문자가 SP 또는 ")"가 아니면, 파싱을 실패시킨다.

   4.  내부 리스트의 끝이 발견되지 않았다. 파싱을 실패시킨다.

4.2.2.  딕셔너리 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 값이 (item_or_inner_list,
   parameters) 튜플인 순서 있는 맵을 반환한다. input_string은 파싱된 값을 제거하도록
   수정된다.

   1.  dictionary를 비어 있는 순서 있는 맵으로 둔다.

   2.  input_string이 비어 있지 않은 동안:

       1.   this_key를 input_string에 대해 키 파싱(4.2.3.3절)을 실행한 결과로 둔다.

       2.   input_string의 첫 번째 문자가 "="이면:

            1.  input_string의 첫 번째 문자를 소비한다.

            2.  member를 input_string에 대해 아이템 또는 내부 리스트 파싱(4.2.1.1절)을
                실행한 결과로 둔다.

       3.   그렇지 않으면:

            1.  value를 불리언 true로 둔다.

            2.  parameters를 input_string에 대해 파라미터 파싱(4.2.3.2절)을 실행한
                결과로 둔다.

            3.  member를 (value, parameters) 튜플로 둔다.

       4.   dictionary가 이미 this_key 키를 포함하면(문자 대 문자로 비교), 그 값을
            member로 덮어쓴다.

       5.   그렇지 않으면, this_key 키와 member 값을 dictionary에 덧붙인다.

       6.   input_string에서 선행 OWS 문자들을 버린다.

       7.   input_string이 비어 있으면, dictionary를 반환한다.

       8.   input_string의 첫 번째 문자를 소비한다. 그것이 ","가 아니면, 파싱을
            실패시킨다.

       9.   input_string에서 선행 OWS 문자들을 버린다.

       10.  input_string이 비어 있으면, 후행 쉼표가 있는 것이다. 파싱을 실패시킨다.

   3.  구조화된 데이터가 발견되지 않았다. dictionary(비어 있음)를 반환한다.

   중복된 딕셔너리 키가 발견되면, 마지막 인스턴스를 제외한 모두가 무시됨에 유의하라.

4.2.3.  아이템 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, (bare_item, parameters) 튜플을
   반환한다. input_string은 파싱된 값을 제거하도록 수정된다.

   1.  bare_item을 input_string에 대해 베어 아이템 파싱(4.2.3.1절)을 실행한
       결과로 둔다.

   2.  parameters를 input_string에 대해 파라미터 파싱(4.2.3.2절)을 실행한 결과로
       둔다.

   3.  (bare_item, parameters) 튜플을 반환한다.

4.2.3.1.  베어 아이템 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 베어 아이템을 반환한다.
   input_string은 파싱된 값을 제거하도록 수정된다.

   1.  input_string의 첫 번째 문자가 "-" 또는 DIGIT이면, input_string에 대해 정수
       또는 소수 파싱(4.2.4절)을 실행한 결과를 반환한다.

   2.  input_string의 첫 번째 문자가 DQUOTE이면, input_string에 대해 문자열 파싱
       (4.2.5절)을 실행한 결과를 반환한다.

   3.  input_string의 첫 번째 문자가 ALPHA 또는 "*"이면, input_string에 대해 토큰
       파싱(4.2.6절)을 실행한 결과를 반환한다.

   4.  input_string의 첫 번째 문자가 ":"이면, input_string에 대해 바이트 시퀀스
       파싱(4.2.7절)을 실행한 결과를 반환한다.

   5.  input_string의 첫 번째 문자가 "?"이면, input_string에 대해 불리언 파싱
       (4.2.8절)을 실행한 결과를 반환한다.

   6.  그렇지 않으면, 아이템 타입이 인식되지 않은 것이다. 파싱을 실패시킨다.

4.2.3.2.  파라미터 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 값이 베어 아이템인 순서 있는 맵을
   반환한다. input_string은 파싱된 값을 제거하도록 수정된다.

   1.  parameters를 비어 있는 순서 있는 맵으로 둔다.

   2.  input_string이 비어 있지 않은 동안:

       1.  input_string의 첫 번째 문자가 ";"가 아니면, 루프를 빠져나간다.

       2.  input_string의 시작에서 ";" 문자를 소비한다.

       3.  input_string에서 선행 SP 문자들을 버린다.

       4.  param_key를 input_string에 대해 키 파싱(4.2.3.3절)을 실행한 결과로 둔다.

       5.  param_value를 불리언 true로 둔다.

       6.  input_string의 첫 번째 문자가 "="이면:

           1.  input_string의 시작에서 "=" 문자를 소비한다.

           2.  param_value를 input_string에 대해 베어 아이템 파싱(4.2.3.1절)을
               실행한 결과로 둔다.

       7.  parameters가 이미 param_key 키를 포함하면(문자 대 문자로 비교), 그 값을
           param_value로 덮어쓴다.

       8.  그렇지 않으면, param_key 키와 param_value 값을 parameters에 덧붙인다.

   3.  parameters를 반환한다.

   중복된 파라미터 키가 발견되면, 마지막 인스턴스를 제외한 모두가 무시됨에
   유의하라.

4.2.3.3.  키 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 키를 반환한다. input_string은
   파싱된 값을 제거하도록 수정된다.

   1.  input_string의 첫 번째 문자가 lcalpha 또는 "*"가 아니면, 파싱을 실패시킨다.

   2.  output_string을 빈 문자열로 둔다.

   3.  input_string이 비어 있지 않은 동안:

       1.  input_string의 첫 번째 문자가 lcalpha, DIGIT, "_", "-", ".", "*" 중
           하나가 아니면, output_string을 반환한다.

       2.  char를 input_string의 첫 번째 문자를 소비한 결과로 둔다.

       3.  output_string에 char를 덧붙인다.

   4.  output_string을 반환한다.

4.2.4.  정수 또는 소수 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 정수 또는 소수를 반환한다.
   input_string은 파싱된 값을 제거하도록 수정된다.

   NOTE: 이 알고리즘은 정수(3.3.1절)와 소수(3.3.2절)를 모두 파싱하며, 해당하는
   구조를 반환한다.

   1.   type을 "integer"로 둔다.

   2.   sign을 1로 둔다.

   3.   input_number를 빈 문자열로 둔다.

   4.   input_string의 첫 번째 문자가 "-"이면, 그것을 소비하고 sign을 -1로 설정한다.

   5.   input_string이 비어 있으면, 빈 정수가 있는 것이다. 파싱을 실패시킨다.

   6.   input_string의 첫 번째 문자가 DIGIT이 아니면, 파싱을 실패시킨다.

   7.   input_string이 비어 있지 않은 동안:

        1.  char를 input_string의 첫 번째 문자를 소비한 결과로 둔다.

        2.  char가 DIGIT이면, 그것을 input_number에 덧붙인다.

        3.  그렇지 않고, type이 "integer"이고 char가 "."이면:

            1.  input_number가 12개보다 많은 문자를 포함하면, 파싱을 실패시킨다.

            2.  그렇지 않으면, char를 input_number에 덧붙이고 type을 "decimal"로
                설정한다.

        4.  그렇지 않으면, char를 input_string 앞에 붙이고, 루프를 빠져나간다.

        5.  type이 "integer"이고 input_number가 15개보다 많은 문자를 포함하면,
            파싱을 실패시킨다.

        6.  type이 "decimal"이고 input_number가 16개보다 많은 문자를 포함하면,
            파싱을 실패시킨다.

   8.   type이 "integer"이면:

        1.  input_number를 정수로 파싱하고, output_number를 그 결과와 sign의 곱으로
            둔다.

   9.   그렇지 않으면:

        1.  input_number의 마지막 문자가 "."이면, 파싱을 실패시킨다.

        2.  input_number에서 "." 이후의 문자 수가 3개보다 많으면, 파싱을
            실패시킨다.

        3.  input_number를 소수로 파싱하고, output_number를 그 결과와 sign의 곱으로
            둔다.

   10.  output_number를 반환한다.

4.2.5.  문자열 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 따옴표가 제거된(unquoted) 문자열을
   반환한다. input_string은 파싱된 값을 제거하도록 수정된다.

   1.  output_string을 빈 문자열로 둔다.

   2.  input_string의 첫 번째 문자가 DQUOTE가 아니면, 파싱을 실패시킨다.

   3.  input_string의 첫 번째 문자를 버린다.

   4.  input_string이 비어 있지 않은 동안:

       1.  char를 input_string의 첫 번째 문자를 소비한 결과로 둔다.

       2.  char가 백슬래시("\")이면:

           1.  input_string이 이제 비어 있으면, 파싱을 실패시킨다.

           2.  next_char를 input_string의 첫 번째 문자를 소비한 결과로 둔다.

           3.  next_char가 DQUOTE 또는 "\"가 아니면, 파싱을 실패시킨다.

           4.  output_string에 next_char를 덧붙인다.

       3.  그렇지 않고, char가 DQUOTE이면, output_string을 반환한다.

       4.  그렇지 않고, char가 %x00-1f 또는 %x7f-ff 범위에 있으면(즉, VCHAR 또는
           SP에 없으면), 파싱을 실패시킨다.

       5.  그렇지 않으면, output_string에 char를 덧붙인다.

   5.  닫는 DQUOTE를 찾지 못한 채 input_string의 끝에 도달했다. 파싱을 실패시킨다.

4.2.6.  토큰 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 토큰을 반환한다. input_string은
   파싱된 값을 제거하도록 수정된다.

   1.  input_string의 첫 번째 문자가 ALPHA 또는 "*"가 아니면, 파싱을 실패시킨다.

   2.  output_string을 빈 문자열로 둔다.

   3.  input_string이 비어 있지 않은 동안:

       1.  input_string의 첫 번째 문자가 tchar, ":", "/"에 없으면, output_string을
           반환한다.

       2.  char를 input_string의 첫 번째 문자를 소비한 결과로 둔다.

       3.  output_string에 char를 덧붙인다.

   4.  output_string을 반환한다.

4.2.7.  바이트 시퀀스 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 바이트 시퀀스를 반환한다.
   input_string은 파싱된 값을 제거하도록 수정된다.

   1.  input_string의 첫 번째 문자가 ":"가 아니면, 파싱을 실패시킨다.

   2.  input_string의 첫 번째 문자를 버린다.

   3.  input_string의 끝 이전에 ":" 문자가 없으면, 파싱을 실패시킨다.

   4.  b64_content를 input_string의 내용을 첫 번째 ":" 문자 인스턴스 직전까지(그것을
       포함하지 않고) 소비한 결과로 둔다.

   5.  input_string의 시작에서 ":" 문자를 소비한다.

   6.  b64_content가 ALPHA, DIGIT, "+", "/", "="에 포함되지 않은 문자를 포함하면,
       파싱을 실패시킨다.

   7.  binary_content를 b64_content를 base64 디코딩 [RFC4648]한 결과로 두며,
       필요하면 패딩을 합성한다(아래의 수신자 동작에 관한 요구사항에 유의하라).
       base64 디코딩이 실패하면, 파싱이 실패한다.

   8.  binary_content를 반환한다.

   일부 base64 구현은 적절하게 "=" 패딩되지 않은 인코딩된 데이터의 거부를 허용하지
   않으므로([RFC4648]의 3.2절 참조), 파서는 그렇게 하도록 구성될 수 없는 경우가
   아니라면 "=" 패딩이 존재하지 않을 때 실패해서는 안 된다(SHOULD NOT).

   일부 base64 구현은 0이 아닌 패딩 비트를 가진 인코딩된 데이터의 거부를 허용하지
   않으므로([RFC4648]의 3.5절 참조), 파서는 그렇게 하도록 구성될 수 없는 경우가
   아니라면 0이 아닌 패딩 비트가 존재할 때 실패해서는 안 된다(SHOULD NOT).

   이 명세는 [RFC4648]의 3.1절과 3.3절의 요구사항을 완화하지 않는다. 따라서 파서는
   base64 알파벳 밖의 문자와 인코딩된 데이터 내의 개행(line feed)에 대해 실패해야
   한다(MUST).

4.2.8.  불리언 파싱

   ASCII 문자열을 input_string으로 주어졌을 때, 불리언을 반환한다. input_string은
   파싱된 값을 제거하도록 수정된다.

   1.  input_string의 첫 번째 문자가 "?"가 아니면, 파싱을 실패시킨다.

   2.  input_string의 첫 번째 문자를 버린다.

   3.  input_string의 첫 번째 문자가 "1"과 일치하면, 첫 번째 문자를 버리고 true를
       반환한다.

   4.  input_string의 첫 번째 문자가 "0"과 일치하면, 첫 번째 문자를 버리고 false를
       반환한다.

   5.  일치하는 값이 없다. 파싱을 실패시킨다.

5.  IANA 고려사항

   이 문서에는 IANA 조치가 없다.

6.  보안 고려사항

   구조화된 필드가 정의하는 대부분의 타입의 크기는 제한되지 않는다. 그 결과, 극도로
   큰 필드는 공격 벡터(예: 자원 소모)가 될 수 있다. 대부분의 HTTP 구현체는 그러한
   공격을 완화하기 위해 개별 필드의 크기뿐만 아니라 전체 헤더 또는 트레일러 섹션의
   크기를 제한한다.

   새로운 HTTP 필드를 주입할 수 있는 능력을 가진 당사자가 구조화된 필드의 의미를
   변경하는 것이 가능하다. 일부 상황에서는 이것이 파싱을 실패하게 하지만, 그러한
   모든 상황에서 신뢰성 있게 실패하는 것은 불가능하다.

7.  참고문헌

7.1.  규범적 참고문헌

   [RFC0020]  Cerf, V., "ASCII format for network interchange", STD 80,
              RFC 20, DOI 10.17487/RFC0020, October 1969,
              <https://www.rfc-editor.org/info/rfc20>.

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119,
              DOI 10.17487/RFC2119, March 1997,
              <https://www.rfc-editor.org/info/rfc2119>.

   [RFC4648]  Josefsson, S., "The Base16, Base32, and Base64 Data
              Encodings", RFC 4648, DOI 10.17487/RFC4648, October 2006,
              <https://www.rfc-editor.org/info/rfc4648>.

   [RFC5234]  Crocker, D., Ed. and P. Overell, "Augmented BNF for Syntax
              Specifications: ABNF", STD 68, RFC 5234,
              DOI 10.17487/RFC5234, January 2008,
              <https://www.rfc-editor.org/info/rfc5234>.

   [RFC7230]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext Transfer
              Protocol (HTTP/1.1): Message Syntax and Routing",
              RFC 7230, DOI 10.17487/RFC7230, June 2014,
              <https://www.rfc-editor.org/info/rfc7230>.

   [RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
              2119 Key Words", BCP 14, RFC 8174, DOI 10.17487/RFC8174,
              May 2017, <https://www.rfc-editor.org/info/rfc8174>.

7.2.  정보성 참고문헌

   [IEEE754]  IEEE, "IEEE Standard for Floating-Point Arithmetic",
              DOI 10.1109/IEEESTD.2019.8766229, IEEE 754-2019, July
              2019, <https://ieeexplore.ieee.org/document/8766229>.

   [RFC7231]  Fielding, R., Ed. and J. Reschke, Ed., "Hypertext Transfer
              Protocol (HTTP/1.1): Semantics and Content", RFC 7231,
              DOI 10.17487/RFC7231, June 2014,
              <https://www.rfc-editor.org/info/rfc7231>.

   [RFC7493]  Bray, T., Ed., "The I-JSON Message Format", RFC 7493,
              DOI 10.17487/RFC7493, March 2015,
              <https://www.rfc-editor.org/info/rfc7493>.

   [RFC7540]  Belshe, M., Peon, R., and M. Thomson, Ed., "Hypertext
              Transfer Protocol Version 2 (HTTP/2)", RFC 7540,
              DOI 10.17487/RFC7540, May 2015,
              <https://www.rfc-editor.org/info/rfc7540>.

   [RFC7541]  Peon, R. and H. Ruellan, "HPACK: Header Compression for
              HTTP/2", RFC 7541, DOI 10.17487/RFC7541, May 2015,
              <https://www.rfc-editor.org/info/rfc7541>.

   [RFC8259]  Bray, T., Ed., "The JavaScript Object Notation (JSON) Data
              Interchange Format", STD 90, RFC 8259,
              DOI 10.17487/RFC8259, December 2017,
              <https://www.rfc-editor.org/info/rfc8259>.

   [STD63]    Yergeau, F., "UTF-8, a transformation format of ISO
              10646", STD 63, RFC 3629, November 2003,
              <https://www.rfc-editor.org/info/std63>.

부록 A.  자주 묻는 질문

A.1.  왜 JSON이 아닌가?

   구조화된 필드에 대한 초기 제안들은 JSON [RFC8259]에 기반했다. 그러나, HTTP 헤더
   필드에 적합하도록 그 사용을 제약하는 것은 송신자와 수신자가 특정한 추가 처리를
   구현하도록 요구했다.

   예를 들어, JSON은 큰 숫자와 중복 멤버를 가진 객체에 관한 명세상의 문제를 가지고
   있다. 이러한 문제들을 회피하기 위한 조언(예: [RFC7493])이 이용 가능하지만, 그것에
   의존할 수는 없다.

   마찬가지로, JSON 문자열은 기본적으로 유니코드 문자열이며, 이는 다수의 잠재적
   상호운용성 문제(예: 비교에서)를 가진다. 구현자가 불필요한 경우 비-ASCII 콘텐츠를
   회피하도록 권고할 수 있지만, 이를 강제하기는 어렵다.

   또 다른 예는 임의의 깊이로 콘텐츠를 중첩할 수 있는 JSON의 능력이다. 그로 인한
   메모리 부담이 부적합할 수 있으므로(예: 임베디드 및 기타 제한적인 서버 배포에서),
   어떤 방식으로든 그것을 제한하는 것이 필요하다. 그러나, 기존의 JSON 구현체들은
   그러한 제한을 가지고 있지 않으며, 제한이 명세되더라도 일부 필드 정의가 그것을
   위반할 필요를 찾게 될 가능성이 높다.

   JSON의 광범위한 채택과 구현으로 인해, 모든 구현체에 걸쳐 그러한 추가 제약을
   부과하기는 어렵다. 일부 배포는 그것들을 강제하는 데 실패할 것이며, 그럼으로써
   상호운용성을 해칠 것이다. 요컨대, JSON처럼 보인다면, 사람들은 필드 값에 JSON
   파서/직렬화기를 사용하고 싶은 유혹을 받을 것이다.

   구조화된 필드의 주요 목표가 상호운용성을 개선하고 구현을 단순화하는 것이므로,
   이러한 우려들이 전용 파서와 직렬화기를 요구하는 형식으로 이어졌다.

   추가로, JSON이 HTTP 필드에서 "제대로 보이지 않는다"는 광범위하게 공유된 느낌이
   있었다.

부록 B.  구현 노트

   이 명세의 일반적인 구현은 최상위 직렬화(4.1절) 및 파싱(4.2절) 함수를 노출해야
   한다. 그것들이 함수일 필요는 없다. 예를 들어, 각 서로 다른 최상위 타입에 대한
   메서드를 가진 객체로 구현될 수 있다.

   상호운용성을 위해, 일반 구현이 완전하고 알고리즘을 면밀히 따르는 것이 중요하다.
   1.1절을 참조하라. 이를 돕기 위해, 공통 테스트 스위트가 커뮤니티에 의해
   <https://github.com/httpwg/structured-field-tests>에서 유지되고 있다.

   구현자는 딕셔너리와 파라미터가 순서를 보존하는 맵(order-preserving maps)임에
   유의해야 한다. 일부 필드는 이러한 데이터 타입의 순서에 의미를 전달하지 않을 수
   있지만, 그 순서를 사용해야 하는 애플리케이션이 이용할 수 있도록 여전히 노출되어야
   한다.

   마찬가지로, 구현체는 토큰과 문자열의 구별을 보존하는 것이 중요함에 유의해야
   한다. 대부분의 프로그래밍 언어는 다른 타입들에 잘 매핑되는 네이티브 타입을
   가지고 있지만, 이러한 타입들이 분리된 상태를 유지하도록 보장하기 위해 래퍼
   "token" 객체를 만들거나 함수에 파라미터를 사용하는 것이 필요할 수 있다.

   직렬화 알고리즘은 모든 경우에 3절에 정의된 데이터 타입에 엄격히 한정되지 않는
   방식으로 정의되어 있다. 예를 들어, 소수는 더 넓은 입력을 받아 허용된 값으로
   반올림하도록 설계되었다.

   구현체는 각 타입에 대해 정의된 최소값을 따르는 한도 내에서, 서로 다른 구조의
   크기를 제한하는 것이 허용된다. 어떤 구조가 구현 제한을 초과하면, 그 구조는 파싱
   또는 직렬화를 실패한다.

감사의 글

   이 명세의 개발 과정에서 상세한 피드백과 신중한 고려를 해준 Matthew Kerwin에게
   깊이 감사한다.

   또한 그들의 기여에 대해 Ian Clelland, Roy Fielding, Anne van Kesteren, Kazuho
   Oku, Evert Pot, Julian Reschke, Martin Thomson, Mike West, Jeffrey Yasskin에게도
   감사한다.

저자 주소

   Mark Nottingham
   Fastly
   Prahran VIC
   Australia

   Email: mnot@mnot.net
   URI:   https://www.mnot.net/


   Poul-Henning Kamp
   The Varnish Cache Project

   Email: phk@varnish-cache.org
