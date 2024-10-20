Internet Engineering Task Force (IETF)                       J. Gregorio
Request for Comments: 6570                                        Google
Category: Standards Track                                    R. Fielding
ISSN: 2070-1721                                                    Adobe
                                                               M. Hadley
                                                                   MITRE
                                                           M. Nottingham
                                                               Rackspace
                                                              D. Orchard
                                                          Salesforce.com
                                                              March 2012


                              URI Template

초록

   URI Template은 변수 확장을 통해 Uniform Resource Identifier의 범위를
   기술하기 위한 간결한 문자 시퀀스이다. 이 명세는 URI Template 구문과
   URI Template을 URI 참조로 확장하는 과정을 정의하며, 인터넷에서
   URI Template을 사용하기 위한 지침도 함께 제공한다.

이 메모의 상태

   이 문서는 인터넷 표준 트랙 문서이다.

   이 문서는 Internet Engineering Task Force(IETF)의 산출물이다. 이것은
   IETF 커뮤니티의 합의를 나타낸다. 이 문서는 공개 검토를 받았으며
   Internet Engineering Steering Group(IESG)의 승인을 받아 출판되었다.
   인터넷 표준에 대한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 피드백 제공 방법에 대한 정보는
   http://www.rfc-editor.org/info/rfc6570 에서 얻을 수 있다.

저작권 공지

   Copyright (c) 2012 IETF Trust and the persons identified as the
   document authors.  All rights reserved.

   이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF Trust의 IETF 문서
   관련 법적 조항(http://trustee.ietf.org/license-info)의 적용을 받는다.
   이 문서들은 이 문서에 대한 여러분의 권리와 제한 사항을 기술하고 있으므로
   주의 깊게 검토하기 바란다. 이 문서에서 추출된 코드 구성 요소는 Trust
   Legal Provisions의 섹션 4.e에 설명된 대로 Simplified BSD License 텍스트를
   포함해야 하며, Simplified BSD License에 설명된 대로 보증 없이 제공된다.

목차

   1. 소개 ............................................................3
      1.1. 개요 .......................................................3
      1.2. 레벨 및 표현식 유형 ........................................5
      1.3. 설계 고려 사항 .............................................9
      1.4. 제한 사항 .................................................10
      1.5. 표기 규칙 .................................................11
      1.6. 문자 인코딩과 유니코드 정규화 .............................12
   2. 구문 ...........................................................13
      2.1. 리터럴 ....................................................13
      2.2. 표현식 ....................................................13
      2.3. 변수 ......................................................14
      2.4. 값 수정자 .................................................15
           2.4.1. 접두사 값 ..........................................15
           2.4.2. 복합 값 ............................................16
   3. 확장 ...........................................................18
      3.1. 리터럴 확장 ...............................................18
      3.2. 표현식 확장 ...............................................18
           3.2.1. 변수 확장 ..........................................19
           3.2.2. 단순 문자열 확장: {var} ............................21
           3.2.3. Reserved 확장: {+var} ..............................22
           3.2.4. Fragment 확장: {#var} ..............................23
           3.2.5. 점 접두사 Label 확장: {.var} .......................24
           3.2.6. 경로 세그먼트 확장: {/var} .........................24
           3.2.7. 경로 스타일 파라미터 확장: {;var} ...................25
           3.2.8. 폼 스타일 쿼리 확장: {?var} ........................26
           3.2.9. 폼 스타일 쿼리 연속: {&var} ........................27
   4. 보안 고려 사항 .................................................27
   5. 감사의 글 ......................................................28
   6. 참고 문헌 ......................................................28
      6.1. 규범적 참고 문헌 ..........................................28
      6.2. 정보적 참고 문헌 ..........................................29
   부록 A. 구현 힌트 .................................................30


1.  소개

1.1.  개요

   Uniform Resource Identifier(URI) [RFC3986]는 유사한 리소스들의 공통
   공간(비공식적으로 "URI 공간") 내에서 특정 리소스를 식별하는 데
   자주 사용된다. 예를 들어, 개인 웹 공간은 흔히 다음과 같은 공통 패턴을
   사용하여 위임된다:

     http://example.com/~fred/
     http://example.com/~mark/

   또는 사전 항목들의 집합이 용어의 첫 글자에 의한 계층 구조로 그룹화될
   수 있다:

     http://example.com/dictionary/c/cat
     http://example.com/dictionary/d/dog

   또는 서비스 인터페이스가 다양한 사용자 입력으로 공통 패턴에 따라
   호출될 수 있다:

     http://example.com/search?q=cat&lang=en
     http://example.com/search?q=chien&lang=fr

   URI Template은 변수 확장을 통해 Uniform Resource Identifier의 범위를
   기술하기 위한 간결한 문자 시퀀스이다.

   URI Template은 가변적인 부분을 쉽게 식별하고 기술할 수 있도록 리소스
   식별자 공간을 추상화하는 메커니즘을 제공한다. URI Template은 가용
   서비스의 발견, 리소스 매핑 구성, 계산된 링크 정의, 인터페이스 지정, 및
   리소스와의 기타 프로그래밍적 상호작용 등 다양한 용도로 사용될 수 있다.
   예를 들어, 위의 리소스들은 다음 URI Template으로 기술될 수 있다:

     http://example.com/~{username}/
     http://example.com/dictionary/{term:1}/{term}
     http://example.com/search{?q,lang}

   다음 용어들을 정의한다:

   expression(표현식):  섹션 2에 정의된 대로, 둘러싸는 중괄호를 포함하여
      '{' 와 '}' 사이의 텍스트.

   expansion(확장):  섹션 3에 정의된 대로, 표현식 유형, 변수 이름 목록,
      및 값 수정자에 따라 template expression을 처리한 후 얻어지는 문자열
      결과.

   template processor(템플릿 프로세서):  URI Template과 값이 있는 변수들의
      집합이 주어졌을 때, 표현식을 파싱하고 각 표현식을 해당 확장으로
      대체하여 template 문자열을 URI 참조로 변환하는 프로그램 또는 라이브러리.

   URI Template은 URI 공간의 구조적 설명을 제공하며, 변수 값이 제공되면
   해당 값에 대응하는 URI를 구성하는 방법에 대한 기계 판독 가능한 명령도
   제공한다. URI Template은 template 내 각 구분된 표현식을 해당 표현식
   유형과 표현식 내에 명명된 변수의 값에 의해 정의된 값으로 대체함으로써
   URI 참조로 변환된다. 표현식 유형은 단순 문자열 확장에서 여러 개의
   name=value 목록까지 다양하다. 확장은 URI 일반 구문에 기반하므로,
   구현체가 가능한 모든 결과 URI의 체계별 요구 사항을 알지 못하더라도
   어떤 URI Template이든 처리할 수 있다.

   예를 들어, 다음 URI Template에는 변수 이름 앞에 "?" 연산자가 나타나는
   것으로 표시되는 폼 스타일 파라미터 표현식이 포함되어 있다.

     http://www.example.com/foo{?query,number}

   물음표("?") 연산자로 시작하는 표현식의 확장 과정은 World Wide Web의
   폼 스타일 인터페이스와 동일한 패턴을 따른다:

     http://www.example.com/foo{?query,number}
                               \_____________/
                                  |
                                  |
             [ 'query', 'number' ] 내의 정의된 각 변수에 대해,
             첫 번째 대체이면 "?"를 그 이후에는 "&"를 대체하고,
             변수 이름, '=', 그리고 변수의 값을 뒤에 붙인다.

   변수의 값이 다음과 같다면

     query  := "mycelium"
     number := 100

   위 URI Template의 확장 결과는 다음과 같다

     http://www.example.com/foo?query=mycelium&number=100

   또는 'query'가 정의되지 않은 경우, 확장 결과는 다음과 같다

     http://www.example.com/foo?number=100

   또는 두 변수 모두 정의되지 않은 경우, 다음과 같다

     http://www.example.com/foo

   URI Template은 위의 예시들처럼 절대 형식으로 제공될 수도 있고,
   상대 형식으로 제공될 수도 있다. 결과 참조가 상대에서 절대 형식으로
   해석되기 전에 template이 먼저 확장된다.

   URI 구문이 결과에 사용되지만, template 문자열은 Internationalized
   Resource Identifier(IRI) 참조 [RFC3987]에서 찾을 수 있는 더 넓은 범위의
   문자 집합을 포함할 수 있다. 따라서 URI Template은 IRI template이기도
   하며, template 처리 결과는 [RFC3987]의 섹션 3.2에 정의된 과정을 따라
   IRI로 변환될 수 있다.

1.2.  레벨 및 표현식 유형

   URI Template은 고정된 매크로 정의 집합을 가진 매크로 언어와 유사하다:
   표현식 유형이 확장 과정을 결정한다. 기본 표현식 유형은 단순 문자열
   확장으로, 단일 명명된 변수가 unreserved URI 문자 집합(섹션 1.5)에
   포함되지 않는 문자를 퍼센트 인코딩한 후 해당 값의 문자열로
   대체된다.

   이 명세 이전에 구현된 대부분의 template processor는 기본 표현식 유형만
   구현했으므로, 이를 Level 1 template이라 한다.

   .-----------------------------------------------------------------.
   | Level 1 예시, 변수 값이 다음과 같을 때                          |
   |                                                                 |
   |             var   := "value"                                    |
   |             hello := "Hello World!"                             |
   |                                                                 |
   |-----------------------------------------------------------------|
   | Op       Expression            Expansion                        |
   |-----------------------------------------------------------------|
   |     | 단순 문자열 확장                              (Sec 3.2.2) |
   |     |                                                           |
   |     |    {var}                 value                            |
   |     |    {hello}               Hello%20World%21                 |
   `-----------------------------------------------------------------'

   Level 2 template은 reserved URI 문자(섹션 1.5)를 포함할 수 있는 값의
   확장을 위한 더하기("+") 연산자와 fragment 식별자 확장을 위한
   크로스해치("#") 연산자를 추가한다.

   .-----------------------------------------------------------------.
   | Level 2 예시, 변수 값이 다음과 같을 때                          |
   |                                                                 |
   |             var   := "value"                                    |
   |             hello := "Hello World!"                             |
   |             path  := "/foo/bar"                                 |
   |                                                                 |
   |-----------------------------------------------------------------|
   | Op       Expression            Expansion                        |
   |-----------------------------------------------------------------|
   |  +  | Reserved 문자열 확장                          (Sec 3.2.3) |
   |     |                                                           |
   |     |    {+var}                value                            |
   |     |    {+hello}              Hello%20World!                   |
   |     |    {+path}/here          /foo/bar/here                    |
   |     |    here?ref={+path}      here?ref=/foo/bar                |
   |-----+-----------------------------------------------------------|
   |  #  | Fragment 확장, 크로스해치 접두사              (Sec 3.2.4) |
   |     |                                                           |
   |     |    X{#var}               X#value                          |
   |     |    X{#hello}             X#Hello%20World!                 |
   `-----------------------------------------------------------------'

   Level 3 template은 각각 쉼표로 구분되는 표현식당 복수 변수를 허용하며,
   점 접두사 label, 슬래시 접두사 경로 세그먼트, 세미콜론 접두사 경로
   파라미터, 그리고 앰퍼샌드 문자로 구분되는 name=value 쌍으로 구성된
   쿼리 구문의 폼 스타일 구성을 위한 더 복잡한 연산자들을 추가한다.

   .-----------------------------------------------------------------.
   | Level 3 예시, 변수 값이 다음과 같을 때                          |
   |                                                                 |
   |             var   := "value"                                    |
   |             hello := "Hello World!"                             |
   |             empty := ""                                         |
   |             path  := "/foo/bar"                                 |
   |             x     := "1024"                                     |
   |             y     := "768"                                      |
   |                                                                 |
   |-----------------------------------------------------------------|
   | Op       Expression            Expansion                        |
   |-----------------------------------------------------------------|
   |     | 복수 변수를 사용한 문자열 확장                (Sec 3.2.2) |
   |     |                                                           |
   |     |    map?{x,y}             map?1024,768                     |
   |     |    {x,hello,y}           1024,Hello%20World%21,768        |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  +  | 복수 변수를 사용한 Reserved 확장              (Sec 3.2.3) |
   |     |                                                           |
   |     |    {+x,hello,y}          1024,Hello%20World!,768          |
   |     |    {+path,x}/here        /foo/bar,1024/here               |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  #  | 복수 변수를 사용한 Fragment 확장              (Sec 3.2.4) |
   |     |                                                           |
   |     |    {#x,hello,y}          #1024,Hello%20World!,768         |
   |     |    {#path,x}/here        #/foo/bar,1024/here              |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  .  | Label 확장, 점 접두사                         (Sec 3.2.5) |
   |     |                                                           |
   |     |    X{.var}               X.value                          |
   |     |    X{.x,y}               X.1024.768                       |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  /  | 경로 세그먼트, 슬래시 접두사                  (Sec 3.2.6) |
   |     |                                                           |
   |     |    {/var}                /value                           |
   |     |    {/var,x}/here         /value/1024/here                 |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  ;  | 경로 스타일 파라미터, 세미콜론 접두사         (Sec 3.2.7) |
   |     |                                                           |
   |     |    {;x,y}                ;x=1024;y=768                    |
   |     |    {;x,y,empty}          ;x=1024;y=768;empty              |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  ?  | 폼 스타일 쿼리, 앰퍼샌드 구분                (Sec 3.2.8) |
   |     |                                                           |
   |     |    {?x,y}                ?x=1024&y=768                    |
   |     |    {?x,y,empty}          ?x=1024&y=768&empty=             |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  &  | 폼 스타일 쿼리 연속                          (Sec 3.2.9) |
   |     |                                                           |
   |     |    ?fixed=yes{&x}        ?fixed=yes&x=1024                |
   |     |    {&x,y,empty}          &x=1024&y=768&empty=             |
   |     |                                                           |
   `-----------------------------------------------------------------'

   마지막으로 Level 4 template은 각 변수 이름에 선택적 접미사로서 값
   수정자를 추가한다. 접두사 수정자(":")는 확장 시 값의 시작 부분에서
   제한된 수의 문자만 사용됨을 나타낸다(섹션 2.4.1). 전개("*") 수정자는
   해당 변수가 이름 목록 또는 (name, value) 쌍의 연관 배열로 구성된
   복합 값으로 취급되어야 하며, 각 구성원이 별도의 변수인 것처럼 확장됨을
   나타낸다(섹션 2.4.2).

   .-----------------------------------------------------------------.
   | Level 4 예시, 변수 값이 다음과 같을 때                          |
   |                                                                 |
   |             var   := "value"                                    |
   |             hello := "Hello World!"                             |
   |             path  := "/foo/bar"                                 |
   |             list  := ("red", "green", "blue")                   |
   |             keys  := [("semi",";"),("dot","."),("comma",",")]   |
   |                                                                 |
   | Op       Expression            Expansion                        |
   |-----------------------------------------------------------------|
   |     | 값 수정자를 사용한 문자열 확장                (Sec 3.2.2) |
   |     |                                                           |
   |     |    {var:3}               val                              |
   |     |    {var:30}              value                            |
   |     |    {list}                red,green,blue                   |
   |     |    {list*}               red,green,blue                   |
   |     |    {keys}                semi,%3B,dot,.,comma,%2C         |
   |     |    {keys*}               semi=%3B,dot=.,comma=%2C         |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  +  | 값 수정자를 사용한 Reserved 확장              (Sec 3.2.3) |
   |     |                                                           |
   |     |    {+path:6}/here        /foo/b/here                      |
   |     |    {+list}               red,green,blue                   |
   |     |    {+list*}              red,green,blue                   |
   |     |    {+keys}               semi,;,dot,.,comma,,             |
   |     |    {+keys*}              semi=;,dot=.,comma=,             |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  #  | 값 수정자를 사용한 Fragment 확장              (Sec 3.2.4) |
   |     |                                                           |
   |     |    {#path:6}/here        #/foo/b/here                     |
   |     |    {#list}               #red,green,blue                  |
   |     |    {#list*}              #red,green,blue                  |
   |     |    {#keys}               #semi,;,dot,.,comma,,            |
   |     |    {#keys*}              #semi=;,dot=.,comma=,            |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  .  | Label 확장, 점 접두사                         (Sec 3.2.5) |
   |     |                                                           |
   |     |    X{.var:3}             X.val                            |
   |     |    X{.list}              X.red,green,blue                 |
   |     |    X{.list*}             X.red.green.blue                 |
   |     |    X{.keys}              X.semi,%3B,dot,.,comma,%2C       |
   |     |    X{.keys*}             X.semi=%3B.dot=..comma=%2C       |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  /  | 경로 세그먼트, 슬래시 접두사                  (Sec 3.2.6) |
   |     |                                                           |
   |     |    {/var:1,var}          /v/value                         |
   |     |    {/list}               /red,green,blue                  |
   |     |    {/list*}              /red/green/blue                  |
   |     |    {/list*,path:4}       /red/green/blue/%2Ffoo           |
   |     |    {/keys}               /semi,%3B,dot,.,comma,%2C        |
   |     |    {/keys*}              /semi=%3B/dot=./comma=%2C        |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  ;  | 경로 스타일 파라미터, 세미콜론 접두사         (Sec 3.2.7) |
   |     |                                                           |
   |     |    {;hello:5}            ;hello=Hello                     |
   |     |    {;list}               ;list=red,green,blue             |
   |     |    {;list*}              ;list=red;list=green;list=blue   |
   |     |    {;keys}               ;keys=semi,%3B,dot,.,comma,%2C   |
   |     |    {;keys*}              ;semi=%3B;dot=.;comma=%2C        |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  ?  | 폼 스타일 쿼리, 앰퍼샌드 구분                (Sec 3.2.8) |
   |     |                                                           |
   |     |    {?var:3}              ?var=val                         |
   |     |    {?list}               ?list=red,green,blue             |
   |     |    {?list*}              ?list=red&list=green&list=blue   |
   |     |    {?keys}               ?keys=semi,%3B,dot,.,comma,%2C   |
   |     |    {?keys*}              ?semi=%3B&dot=.&comma=%2C        |
   |     |                                                           |
   |-----+-----------------------------------------------------------|
   |  &  | 폼 스타일 쿼리 연속                          (Sec 3.2.9) |
   |     |                                                           |
   |     |    {&var:3}              &var=val                         |
   |     |    {&list}               &list=red,green,blue             |
   |     |    {&list*}              &list=red&list=green&list=blue   |
   |     |    {&keys}               &keys=semi,%3B,dot,.,comma,%2C   |
   |     |    {&keys*}              &semi=%3B&dot=.&comma=%2C        |
   |     |                                                           |
   `-----------------------------------------------------------------'

1.3.  설계 고려 사항

   URI Template과 유사한 메커니즘은 WSDL [WSDL], WADL [WADL], OpenSearch
   [OpenSearch]를 포함한 여러 명세에서 정의되어 왔다. 이 명세는 URI
   Template이 여러 인터넷 애플리케이션과 인터넷 메시지 필드에서 일관되게
   사용될 수 있도록 구문을 확장하고 공식적으로 정의하며, 동시에 이전
   정의와의 호환성을 유지한다.

   URI Template 구문은 강력한 확장 메커니즘의 필요성과 구현 용이성의
   필요성 사이의 균형을 신중히 맞추도록 설계되었다. 이 구문은 파싱이
   극히 간단하면서도 많은 일반적인 template 시나리오를 표현하기에 충분한
   유연성을 제공하도록 설계되었다. 구현체는 template을 파싱하고 단일
   패스로 확장을 수행할 수 있다.

   단일 문자 연산자가 URI 일반 구문 구분자와 일치하므로, 일반적인
   예시에서 사용될 때 template은 단순하고 가독성이 좋다. 나열된 변수 중
   어느 것도 정의되지 않은 경우 연산자의 관련 구분자(".", ";", "/", "?",
   "&", "#")는 생략된다. 마찬가지로, ";"(경로 스타일 파라미터) 확장 과정은
   변수 값이 비어 있을 때 "="를 생략하는 반면, "?"(폼 스타일 파라미터)
   과정은 값이 비어 있어도 "="를 생략하지 않는다. 복수 변수와 목록 값은
   연산자에 대해 미리 정의된 결합 메커니즘이 없는 경우 해당 값이 ","로
   결합된다. "+" 및 "#" 연산자는 변수 값 내에 있는 인코딩되지 않은
   reserved 문자를 그대로 대체하며; 다른 연산자들은 확장 전에 변수 값에
   있는 reserved 문자를 퍼센트 인코딩한다.

   URI 공간의 가장 일반적인 경우는 Level 1 template 표현식으로 기술될
   수 있다. URI 생성에만 관심이 있다면, 더 복잡한 형태는 변수 값을
   변경하여 생성할 수 있으므로 template 구문을 단순 변수 확장으로
   제한할 수 있다. 그러나 URI Template은 기존 데이터 값의 관점에서
   식별자의 레이아웃을 기술하는 추가적인 목표를 가진다. 따라서 template
   구문에는 리소스 식별자가 일반적으로 할당되는 방식을 반영하는 연산자가
   포함되어 있다. 마찬가지로, 접두사 부분 문자열이 대규모 리소스 공간을
   분할하는 데 자주 사용되므로, 변수 값에 대한 수정자는 단일 변수 이름으로
   부분 문자열과 전체 값 문자열 모두를 지정하는 방법을 제공한다.

1.4.  제한 사항

   URI Template은 식별자의 상위 집합을 기술하므로, 각 구분된 변수
   표현식에 대한 모든 가능한 확장이 기존 리소스의 URI에 대응한다는 것을
   암시하지는 않는다. 우리의 기대는 template에 따라 URI를 구성하는
   애플리케이션에 대체될 변수에 대한 적절한 값 집합이 제공되거나, 최소한
   해당 값에 대한 사용자 데이터 입력을 검증하는 수단이 제공될 것이라는
   것이다.

   URI Template은 URI가 아니다: 추상적이거나 물리적인 리소스를 식별하지
   않으며, URI로 파싱되지 않으며, template 표현식이 사용 전에 template
   processor에 의해 확장되지 않는 한 URI가 기대되는 곳에서 사용되어서는
   안 된다. URI Template을 전달하는 프로토콜 요소와 URI 참조를 기대하는
   프로토콜 요소를 구별하기 위해 별도의 필드, 요소 또는 속성 이름이
   사용되어야 한다.

   일부 URI Template은 변수 매칭의 목적으로 역방향으로 사용될 수 있다:
   template을 완전히 형성된 URI와 비교하여 해당 URI에서 변수 부분을
   추출하고 명명된 변수에 할당하는 것이다. 변수 매칭은 template 표현식이
   URI의 시작 또는 끝, 또는 확장의 일부가 될 수 없는 문자(예: 단순
   문자열 표현식을 둘러싸는 reserved 문자)에 의해 구분되는 경우에만
   잘 작동한다. 일반적으로 정규 표현식 언어가 변수 매칭에 더 적합하다.

1.5.  표기 규칙

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL" 키워드는
   [RFC2119]에 설명된 대로 해석되어야 한다.

   이 명세는 [RFC5234]의 Augmented Backus-Naur Form(ABNF) 표기법을
   사용한다. 다음 ABNF 규칙은 규범적 참고 문헌 [RFC5234], [RFC3986],
   [RFC3987]에서 가져온 것이다.

     ALPHA          =  %x41-5A / %x61-7A   ; A-Z / a-z
     DIGIT          =  %x30-39             ; 0-9
     HEXDIG         =  DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
                       ; 대소문자 무관

     pct-encoded    =  "%" HEXDIG HEXDIG
     unreserved     =  ALPHA / DIGIT / "-" / "." / "_" / "~"
     reserved       =  gen-delims / sub-delims
     gen-delims     =  ":" / "/" / "?" / "#" / "[" / "]" / "@"
     sub-delims     =  "!" / "$" / "&" / "'" / "(" / ")"
                    /  "*" / "+" / "," / ";" / "="

     ucschar        =  %xA0-D7FF / %xF900-FDCF / %xFDF0-FFEF
                    /  %x10000-1FFFD / %x20000-2FFFD / %x30000-3FFFD
                    /  %x40000-4FFFD / %x50000-5FFFD / %x60000-6FFFD
                    /  %x70000-7FFFD / %x80000-8FFFD / %x90000-9FFFD
                    /  %xA0000-AFFFD / %xB0000-BFFFD / %xC0000-CFFFD
                    /  %xD0000-DFFFD / %xE1000-EFFFD

     iprivate       =  %xE000-F8FF / %xF0000-FFFFD / %x100000-10FFFD

1.6.  문자 인코딩과 유니코드 정규화

   이 명세는 [RFC6365]에 정의된 대로 "character", "character encoding
   scheme", "code point", "coded character set", "glyph", "non-ASCII",
   "normalization", "protocol element", "regular expression"이라는 용어를
   사용한다.

   ABNF 표기법은 터미널 값을 US-ASCII 코드 문자 집합 [ASCII]의 상위
   집합인 비음수 정수(코드 포인트)로 정의한다. 이 명세는 터미널 값을
   Unicode 코드 문자 집합 [UNIV6] 내의 코드 포인트로 정의한다.

   구문과 template 확장 과정이 Unicode 코드 포인트의 관점에서 정의되어
   있지만, 실제로 template은 네트워크 프로토콜 요소에 내장된 옥텟이든
   버스 측면에 그려진 글리프이든, 발생하는 맥락에 적합한 어떤 형태나
   인코딩의 문자 시퀀스로 존재한다는 것을 이해해야 한다. 이 명세는
   URI Template 문자와 해당 문자를 저장하거나 전송하는 데 사용되는 옥텟
   사이의 매핑을 위한 특정 문자 인코딩 체계를 의무화하지 않는다. URI
   Template이 프로토콜 요소에 나타나는 경우, 문자 인코딩 체계는 해당
   프로토콜에 의해 정의된다; 그러한 정의가 없는 경우, URI Template은
   주변 텍스트와 동일한 문자 인코딩 체계에 있다고 가정한다. template
   확장 과정 중에만 URI Template의 문자열이 Unicode 코드 포인트의
   시퀀스로 처리되는 것이 REQUIRED이다.

   Unicode Standard [UNIV6]는 다양한 목적을 위해 문자 시퀀스 간의 다양한
   동치를 정의한다. Unicode Standard Annex #15 [UTR15]는 이러한 동치에
   대한 다양한 Normalization Form을 정의한다. 정규화 형태는 동치인
   문자열을 일관되게 인코딩하는 방법을 결정한다. 이론적으로, template
   processor를 포함한 모든 URI 처리 구현체는 URI 참조를 생성할 때 동일한
   정규화 형태를 사용해야 한다. 실제로는 그렇지 않다. 값이 리소스와
   동일한 서버에 의해 제공된 경우, 해당 문자열이 이미 그 서버가 기대하는
   형태라고 가정할 수 있다. 데이터 입력 대화 상자와 같이 사용자에 의해
   값이 제공되는 경우, template processor에 의한 확장에 사용되기 전에
   해당 문자열은 Normalization Form C(NFC: Canonical Decomposition 후
   Canonical Composition)로 정규화되어야 한다(SHOULD).

   마찬가지로, 읽을 수 있는 문자열을 나타내는 non-ASCII 데이터가 URI
   참조에서 사용하기 위해 퍼센트 인코딩될 때, template processor는
   먼저 해당 문자열을 UTF-8 [RFC3629]로 인코딩한 다음 URI 참조에서
   허용되지 않는 옥텟을 퍼센트 인코딩해야 한다(MUST).

2.  구문

   URI Template은 0개 이상의 내장된 변수 표현식을 포함하는 인쇄 가능한
   Unicode 문자의 문자열이며, 각 표현식은 일치하는 중괄호 쌍('{', '}')으로
   구분된다.

     URI-Template  = *( literals / expression )

   위에서 template(및 template processor 구현)은 네 가지 점진적 레벨의
   관점에서 설명되었지만, URI-Template 구문은 Level 4의 ABNF로 정의한다.
   하위 레벨 template으로 제한된 template processor는 상위 레벨에만
   적용되는 ABNF 규칙을 제외할 수 있다(MAY). 그러나 지원되지 않는 레벨이
   최종 사용자에게 적절히 식별될 수 있도록 모든 파서가 전체 구문을
   구현하는 것이 권장된다(RECOMMENDED).

2.1.  리터럴

   URI Template 문자열에서 표현식 외부의 문자는 해당 문자가 URI에서
   허용되는 경우(reserved / unreserved / pct-encoded) URI 참조에 문자
   그대로 복사되거나, 허용되지 않는 경우 해당 문자의 UTF-8 [RFC3629]
   인코딩에 해당하는 퍼센트 인코딩 삼중항의 시퀀스로 URI 참조에 복사되도록
   의도되었다.

     literals      =  %x21 / %x23-24 / %x26 / %x28-3B / %x3D / %x3F-5B
                   /  %x5D / %x5F / %x61-7A / %x7E / ucschar / iprivate
                   /  pct-encoded
                        ; CTL, SP, DQUOTE, "'", "%"(pct-encoded 제외),
                        ;  "<", ">", "\", "^", "`", "{", "|", "}"를
                        ;  제외한 모든 Unicode 문자

2.2.  표현식

   Template expression은 URI Template의 매개변수화된 부분이다. 각 표현식은
   표현식 유형과 해당 확장 과정을 정의하는 선택적 연산자와 그 뒤에
   쉼표로 구분된 변수 지정자(변수 이름과 선택적 값 수정자) 목록을
   포함한다. 연산자가 제공되지 않으면, 표현식은 unreserved 값의 단순
   변수 확장으로 기본 설정된다.

     expression    =  "{" [ operator ] variable-list "}"
     operator      =  op-level2 / op-level3 / op-reserve
     op-level2     =  "+" / "#"
     op-level3     =  "." / "/" / ";" / "?" / "&"
     op-reserve    =  "=" / "," / "!" / "@" / "|"

   연산자 문자는 URI 일반 구문에서 reserved 문자로서의 각 역할을 반영하도록
   선택되었다. 이 명세의 섹션 3에 정의된 연산자는 다음을 포함한다:

      +   Reserved 문자 문자열;

      #   "#"가 접두사인 fragment 식별자;

      .   "."가 접두사인 이름 label 또는 확장자;

      /   "/"가 접두사인 경로 세그먼트;

      ;   ";"가 접두사인 경로 파라미터 이름 또는 name=value 쌍;

      ?   "?"로 시작하고 "&"로 구분된 name=value 쌍으로 구성된 쿼리 구성
          요소; 그리고

      &   리터럴 쿼리 구성 요소 내에서 &name=value 쌍의 연속.

   등호("="), 쉼표(","), 느낌표("!"), 골뱅이("@"), 파이프("|") 연산자
   문자는 향후 확장을 위해 예약되어 있다.

   표현식 구문은 달러("$") 및 괄호["(" 와 ")"] 문자의 사용을 명시적으로
   제외하여 이 명세의 범위 밖에서 사용 가능하도록 한다. 예를 들어, 매크로
   언어가 문자열을 URI Template으로 처리하기 전에 매크로 대체를 적용하기
   위해 이러한 문자를 사용할 수 있다.

2.3.  변수

   연산자(있는 경우) 뒤에, 각 표현식은 하나 이상의 쉼표로 구분된 변수
   지정자(varspec)의 목록을 포함한다. 변수 이름은 기대되는 값의 종류에 대한
   문서화, template processor 내에서 값을 연관시키기 위한 식별자, 그리고
   name=value 확장에서 이름에 사용되는 리터럴 문자열(연관 배열을 전개하는
   경우 제외)이라는 다중 목적을 수행한다. 변수 이름은 대소문자를 구분하는데,
   이름이 대소문자를 구분하는 URI 구성 요소 내에서 확장될 수 있기
   때문이다.

     variable-list =  varspec *( "," varspec )
     varspec       =  varname [ modifier-level4 ]
     varname       =  varchar *( ["."] varchar )
     varchar       =  ALPHA / DIGIT / "_" / pct-encoded

   varname은 하나 이상의 퍼센트 인코딩 삼중항을 포함할 수 있다(MAY). 이
   삼중항은 변수 이름의 필수적인 부분으로 간주되며 처리 중에 디코딩되지
   않는다. 퍼센트 인코딩된 문자를 포함하는 varname은 동일한 문자가
   디코딩된 varname과 같은 변수가 아니다. URI Template을 제공하는
   애플리케이션은 변수 이름 내에서 퍼센트 인코딩의 사용에 일관성을
   유지할 것으로 기대된다.

   표현식은 template processor에 알려지지 않거나 undef 또는 null과 같은
   특별한 "정의되지 않은" 값으로 설정된 변수를 참조할 수 있다(MAY).
   이러한 정의되지 않은 변수는 확장 과정에서 특별한 처리를 받는다
   (섹션 3.2.1).

   길이가 0인 문자열인 변수 값은 정의되지 않은 것으로 간주되지 않는다;
   빈 문자열의 정의된 값을 가진다.

   Level 4 template에서 변수는 값의 목록 또는 (name, value) 쌍의 연관
   배열 형태의 복합 값을 가질 수 있다. 이러한 값 유형은 template 구문에
   의해 직접 표시되지 않지만, 확장 과정에 영향을 미친다(섹션 3.2.1).

   목록 값으로 정의된 변수는 목록이 구성원을 0개 포함하는 경우 정의되지
   않은 것으로 간주된다. (name, value) 쌍의 연관 배열로 정의된 변수는
   배열이 구성원을 0개 포함하거나 배열의 모든 구성원 이름이 정의되지 않은
   값과 연관된 경우 정의되지 않은 것으로 간주된다.

2.4.  값 수정자

   Level 4 template 표현식의 각 변수는 확장이 변수 값 문자열의 접두사로
   제한되거나 확장이 값 목록 또는 (name, value) 쌍의 연관 배열 형태의
   복합 값으로 전개됨을 나타내는 수정자를 가질 수 있다.

     modifier-level4 =  prefix / explode

2.4.1.  접두사 값

   접두사 수정자는 변수 확장이 변수 값 문자열의 접두사로 제한됨을
   나타낸다. 접두사 수정자는 참조 인덱스와 해시 기반 저장소에서 흔히
   볼 수 있듯이 식별자 공간을 계층적으로 분할하는 데 자주 사용된다.
   또한 확장된 값을 최대 문자 수로 제한하는 역할도 한다. 접두사 수정자는
   복합 값을 가진 변수에는 적용되지 않는다.

     prefix        =  ":" max-length
     max-length    =  %x31-39 0*3DIGIT   ; 10000 미만의 양의 정수

   max-length는 Unicode 문자열로서의 변수 값의 시작 부분에서의 최대 문자
   수를 나타내는 양의 정수이다. 이 번호 매기기는 다중 옥텟 인코딩 문자의
   옥텟 사이 또는 퍼센트 인코딩 삼중항 내에서의 분할을 방지하기 위해
   옥텟이 아닌 문자 단위임에 유의하라. max-length가 변수 값의 길이보다
   큰 경우, 전체 값 문자열이 사용된다.

   예를 들어,

     변수 할당이 다음과 같을 때

       var   := "value"
       semi  := ";"

     예시 Template          확장 결과

       {var}              value
       {var:20}           value
       {var:3}            val
       {semi}             %3B
       {semi:2}           %3B

2.4.2.  복합 값

   전개("*") 수정자는 해당 변수가 값의 목록 또는 (name, value) 쌍의 연관
   배열로 구성된 복합 값으로 취급되어야 함을 나타낸다. 따라서 확장 과정은
   복합체의 각 구성원이 별도의 변수로 나열된 것처럼 적용된다. 이러한
   종류의 변수 지정은 변수 이름과 확장 후 URI 참조가 나타나는 방식 사이의
   대응이 적기 때문에 비전개 변수보다 자기 문서화 정도가 상당히 낮다.

     explode       =  "*"

   URI Template은 유형이나 스키마의 표시를 포함하지 않으므로, 전개된
   변수의 유형은 맥락에 의해 결정되는 것으로 가정한다. 예를 들어,
   processor에 문자열, 목록 또는 연관 배열로 값을 구별하는 형태로 값이
   제공될 수 있다. 마찬가지로, template이 사용되는 맥락(스크립트, 마크업
   언어, Interface Definition Language 등)이 변수 이름을 유형, 구조 또는
   스키마와 연관시키는 규칙을 정의할 수 있다.

   전개 수정자는 URI Template 구문의 간결성을 향상시킨다. 예를 들어,
   주어진 도로 주소에 대한 지리적 지도를 제공하는 리소스가 부분 주소
   (예: 도시 또는 우편 번호만)를 포함하여 주소 입력을 위한 수백 가지
   순열의 필드를 허용할 수 있다. 이러한 리소스는 각각의 모든 주소
   구성 요소가 순서대로 나열된 template으로 기술되거나, 전개 수정자를
   활용하는 훨씬 더 단순한 template으로 기술될 수 있다:

      /mapper{?address*}

   "address"라는 이름의 변수가 무엇을 포함할 수 있는지를 정의하는
   어떤 맥락과 함께, 예를 들어 다른 주소 표준(예: [UPU-S42])을 참조하여
   정의한다. 스키마를 인식하는 수신자는 다음과 같은 적절한 확장을 제공할
   수 있다:

      /mapper?city=Newport%20Beach&state=CA

   전개된 변수의 확장 과정은 사용 중인 연산자와 복합 값이 값 목록으로
   취급되는지 또는 (name, value) 쌍의 연관 배열로 취급되는지 모두에
   의존한다. 구조체는 구조체 정의의 필드에 대응하는 이름과 하위 구조체의
   이름 계층을 나타내기 위해 "." 구분자를 사용하는 연관 배열인 것처럼
   처리된다.

   변수가 복합 구조를 가지고 해당 구조의 일부 필드만 정의된 값을 가지는
   경우, 정의된 쌍만 확장에 존재한다. 이것은 많은 수의 잠재적 쿼리 용어로
   구성된 template에 유용할 수 있다.

   목록 변수에 적용된 전개 수정자는 확장이 목록의 구성원 값에 대해
   반복하도록 한다. 경로 및 쿼리 파라미터 확장의 경우, 각 구성원 값은
   변수의 이름과 (varname, value) 쌍으로 짝지어진다. 이를 통해 경로 및
   쿼리 파라미터가 다음과 같이 복수 값에 대해 반복될 수 있다:

     변수 할당이 다음과 같을 때

       year  := ("1965", "2000", "2012")
       dom   := ("example", "com")

     예시 Template          확장 결과

       find{?year*}       find?year=1965&year=2000&year=2012
       www{.dom*}         www.example.com

3.  확장

   URI Template 확장의 과정은 template 문자열을 처음부터 끝까지 스캔하면서
   리터럴 문자를 복사하고 각 표현식을 표현식 내에 명명된 각 변수의 값에
   표현식의 연산자를 적용한 결과로 대체하는 것이다. 각 변수의 값은
   template 확장 전에 형성되어야 한다(MUST).

   URI Template 문법의 각 측면에 대한 확장 요구 사항은 이 섹션에서
   정의된다. 확장 과정 전체에 대한 비규범적 알고리즘은 부록 A에 제공된다.

   template processor가 표현식 외부에서 <URI-Template> 문법과 일치하지
   않는 문자 시퀀스를 만나는 경우, template 처리는 중단되어야 하고(SHOULD),
   URI 참조 결과는 template의 확장된 부분과 그 뒤의 확장되지 않은
   나머지를 포함해야 하며(SHOULD), 오류의 위치와 유형이 호출 애플리케이션에
   표시되어야 한다(SHOULD).

   표현식에서 오류가 발생하는 경우, 예를 들어 template processor가
   인식하지 못하거나 아직 지원하지 않는 연산자 또는 값 수정자이거나,
   <expression> 문법에서 허용되지 않는 문자가 발견된 경우, 표현식의
   처리되지 않은 부분은 확장되지 않은 채 결과에 복사되어야 하고(SHOULD),
   template의 나머지 처리는 계속되어야 하며(SHOULD), 오류의 위치와 유형이
   호출 애플리케이션에 표시되어야 한다(SHOULD).

   오류가 발생하면, 반환되는 결과는 유효한 URI 참조가 아닐 수 있다;
   이것은 진단 목적으로만 사용되는 불완전하게 확장된 template 문자열이
   될 것이다.

3.1.  리터럴 확장

   리터럴 문자가 URI 구문 어디에서나 허용되는 경우(unreserved / reserved /
   pct-encoded), 결과 문자열에 직접 복사된다. 그렇지 않으면, 리터럴 문자의
   퍼센트 인코딩 등가물이 해당 문자를 먼저 UTF-8의 옥텟 시퀀스로
   인코딩한 다음 각 옥텟을 퍼센트 인코딩 삼중항으로 인코딩하여 결과
   문자열에 복사된다.

3.2.  표현식 확장

   각 표현식은 여는 중괄호("{") 문자로 표시되며 다음 닫는 중괄호("}")까지
   계속된다. 표현식은 중첩될 수 없다.

   표현식은 표현식 유형을 결정한 다음 표현식 내의 각 쉼표로 구분된
   varspec에 대해 해당 유형의 확장 과정을 따름으로써 확장된다. Level 1
   template은 기본 연산자(단순 문자열 값 확장)와 표현식당 단일 변수로
   제한된다. Level 2 template은 표현식당 단일 varspec으로 제한된다.

   표현식 유형은 여는 중괄호 뒤의 첫 번째 문자를 확인하여 결정된다.
   문자가 연산자이면, 나중의 확장 결정을 위해 해당 연산자와 관련된 표현식
   유형을 기억하고 variable-list를 위한 다음 문자로 건너뛴다. 첫 번째
   문자가 연산자가 아니면, 표현식 유형은 단순 문자열 확장이며 첫 번째
   문자가 variable-list의 시작이다.

   아래 하위 섹션의 예시들은 변수 값에 대해 다음 정의를 사용한다:

         count := ("one", "two", "three")
         dom   := ("example", "com")
         dub   := "me/too"
         hello := "Hello World!"
         half  := "50%"
         var   := "value"
         who   := "fred"
         base  := "http://example.com/home/"
         path  := "/foo/bar"
         list  := ("red", "green", "blue")
         keys  := [("semi",";"),("dot","."),("comma",",")]
         v     := "6"
         x     := "1024"
         y     := "768"
         empty := ""
         empty_keys  := []
         undef := null

3.2.1.  변수 확장

   정의되지 않은(섹션 2.3) 변수는 값이 없으며 확장 과정에서 무시된다.
   표현식의 모든 변수가 정의되지 않은 경우, 표현식의 확장은 빈 문자열이다.

   정의된 비어 있지 않은 값의 변수 확장은 허용된 URI 문자의 부분 문자열을
   생성한다. 섹션 1.6에서 설명한 대로, 확장 과정은 non-ASCII 문자가 결과
   URI 참조에서 일관되게 퍼센트 인코딩되도록 보장하기 위해 Unicode 코드
   포인트의 관점에서 정의된다. template processor가 일관된 확장을 얻는
   한 가지 방법은 값 문자열을 UTF-8로 변환(아직 UTF-8이 아닌 경우)한 다음
   허용된 집합에 포함되지 않는 각 옥텟을 해당 퍼센트 인코딩 삼중항으로
   변환하는 것이다. 또 다른 방법은 값의 네이티브 문자 인코딩에서 허용된
   URI 문자 집합으로 직접 매핑하고, 나머지 허용되지 않는 문자를 UTF-8
   [RFC3629]로 인코딩될 때 해당 문자의 옥텟에 해당하는 퍼센트 인코딩
   삼중항의 시퀀스로 매핑하는 것이다.

   주어진 확장에 대한 허용 집합은 표현식 유형에 따라 달라진다:
   reserved("+") 및 fragment("#") 확장은 ( unreserved / reserved /
   pct-encoded )의 합집합에 있는 문자 집합이 퍼센트 인코딩 없이 통과되도록
   허용하는 반면, 다른 모든 표현식 유형은 unreserved 문자만 퍼센트 인코딩
   없이 통과되도록 허용한다. 퍼센트 문자("%")는 퍼센트 인코딩 삼중항의
   일부로서만 그리고 reserved/fragment 확장에 대해서만 허용됨에 유의하라:
   다른 모든 경우에, "%"의 값 문자는 변수 확장에 의해 "%25"로 퍼센트
   인코딩되어야 한다(MUST).

   변수가 표현식에서 또는 URI Template의 복수 표현식 내에서 두 번 이상
   나타나는 경우, 해당 변수의 값은 확장 과정 전체에서 정적으로 유지되어야
   한다(MUST)(즉, 변수는 각 확장을 계산하는 목적으로 동일한 값을 가져야
   한다). 그러나 reserved 문자 또는 퍼센트 인코딩 삼중항이 값에 포함된
   경우, 일부 표현식 유형에서는 퍼센트 인코딩되고 다른 유형에서는 그렇지
   않을 것이다.

   단순 문자열 값인 변수의 경우, 확장은 인코딩된 값을 결과 문자열에
   추가하는 것으로 구성된다. 전개 수정자는 효과가 없다. 접두사 수정자는
   확장을 디코딩된 값의 처음 max-length 문자로 제한한다. 값에 다중 옥텟
   또는 퍼센트 인코딩 문자가 포함된 경우, 문자 중간에서 값이 분할되지
   않도록 주의해야 한다: 각 Unicode 코드 포인트를 하나의 문자로 계산하라.

   연관 배열인 변수의 경우, 확장은 표현식 유형과 전개 수정자의 존재 여부
   모두에 의존한다. 전개 수정자가 없으면, 확장은 정의된 값을 가진 각
   (name, value) 쌍의 쉼표로 구분된 연결을 추가하는 것으로 구성된다.
   전개 수정자가 있으면, 확장은 정의된 값을 가진 각 쌍을 "name=value"
   형태로, 또는 값이 빈 문자열이고 표현식 유형이 폼 스타일 파라미터를
   나타내지 않는 경우(즉, "?" 또는 "&" 유형이 아닌 경우) 단순히 "name"
   형태로 추가하는 것으로 구성된다. name과 value 문자열 모두 단순 문자열
   값과 동일한 방식으로 인코딩된다. 다음 표에 따라 표현식 유형에 의해
   정의된 구분자 문자열이 정의된 쌍 사이에 추가된다:

      유형    구분자
                 ","     (기본)
        +        ","
        #        ","
        .        "."
        /        "/"
        ;        ";"
        ?        "&"
        &        "&"

   값의 목록인 변수의 경우, 확장은 표현식 유형과 전개 수정자의 존재 여부
   모두에 의존한다. 전개 수정자가 없으면, 확장은 정의된 구성원 문자열 값의
   쉼표로 구분된 연결로 구성된다. 전개 수정자가 있고 표현식 유형이 명명된
   파라미터를 확장하는 경우(";", "?", 또는 "&"), 목록은 각 구성원 값이
   목록의 varname과 짝지어진 연관 배열인 것처럼 확장된다. 그렇지 않으면,
   값은 각 값이 위 표에서 정의된 표현식 유형의 관련 구분자로 구분되는
   별도의 변수 값의 목록인 것처럼 확장될 것이다.

     예시 Template          확장 결과

       {count}            one,two,three
       {count*}           one,two,three
       {/count}           /one,two,three
       {/count*}          /one/two/three
       {;count}           ;count=one,two,three
       {;count*}          ;count=one;count=two;count=three
       {?count}           ?count=one,two,three
       {?count*}          ?count=one&count=two&count=three
       {&count*}          &count=one&count=two&count=three

3.2.2.  단순 문자열 확장: {var}

   단순 문자열 확장은 연산자가 주어지지 않을 때의 기본 표현식 유형이다.

   variable-list의 각 정의된 변수에 대해, unreserved 집합의 문자를 허용
   문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장을 수행한다. 둘 이상의
   변수가 정의된 값을 가지면, 변수 확장 사이의 구분자로 결과 문자열에
   쉼표(",")를 추가한다.

     예시 Template          확장 결과

       {var}              value
       {hello}            Hello%20World%21
       {half}             50%25
       O{empty}X          OX
       O{undef}X          OX
       {x,y}              1024,768
       {x,hello,y}        1024,Hello%20World%21,768
       ?{x,empty}         ?1024,
       ?{x,undef}         ?1024
       ?{undef,y}         ?768
       {var:3}            val
       {var:30}           value
       {list}             red,green,blue
       {list*}            red,green,blue
       {keys}             semi,%3B,dot,.,comma,%2C
       {keys*}            semi=%3B,dot=.,comma=%2C

3.2.3.  Reserved 확장: {+var}

   Level 2 이상 template을 위한 더하기("+") 연산자로 표시되는 Reserved
   확장은 대체된 값이 퍼센트 인코딩 삼중항과 reserved 집합의 문자도
   포함할 수 있다는 점을 제외하면 단순 문자열 확장과 동일하다.

   variable-list의 각 정의된 변수에 대해, (unreserved / reserved /
   pct-encoded) 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로
   변수 확장을 수행한다. 둘 이상의 변수가 정의된 값을 가지면, 변수 확장
   사이의 구분자로 결과 문자열에 쉼표(",")를 추가한다.

     예시 Template             확장 결과

       {+var}                value
       {+hello}              Hello%20World!
       {+half}               50%25

       {base}index           http%3A%2F%2Fexample.com%2Fhome%2Findex
       {+base}index          http://example.com/home/index
       O{+empty}X            OX
       O{+undef}X            OX

       {+path}/here          /foo/bar/here
       here?ref={+path}      here?ref=/foo/bar
       up{+path}{var}/here   up/foo/barvalue/here
       {+x,hello,y}          1024,Hello%20World!,768
       {+path,x}/here        /foo/bar,1024/here

       {+path:6}/here        /foo/b/here
       {+list}               red,green,blue
       {+list*}              red,green,blue
       {+keys}               semi,;,dot,.,comma,,
       {+keys*}              semi=;,dot=.,comma=,

3.2.4.  Fragment 확장: {#var}

   Level 2 이상 template을 위한 크로스해치("#") 연산자로 표시되는 Fragment
   확장은 변수 중 하나라도 정의된 경우 크로스해치 문자(fragment 구분자)가
   먼저 결과 문자열에 추가된다는 점을 제외하면 Reserved 확장과 동일하다.

     예시 Template          확장 결과

       {#var}             #value
       {#hello}           #Hello%20World!
       {#half}            #50%25
       foo{#empty}        foo#
       foo{#undef}        foo
       {#x,hello,y}       #1024,Hello%20World!,768
       {#path,x}/here     #/foo/bar,1024/here
       {#path:6}/here     #/foo/b/here
       {#list}            #red,green,blue
       {#list*}           #red,green,blue
       {#keys}            #semi,;,dot,.,comma,,
       {#keys*}           #semi=;,dot=.,comma=,

3.2.5.  점 접두사 Label 확장: {.var}

   Level 3 이상 template을 위한 점(".") 연산자로 표시되는 Label 확장은
   다양한 도메인 이름이나 경로 선택자(예: 파일 확장자)를 가진 URI 공간을
   기술하는 데 유용하다.

   variable-list의 각 정의된 변수에 대해, 결과 문자열에 "."를 추가한 다음
   unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로
   변수 확장을 수행한다.

   "."는 unreserved 집합에 포함되므로, "."를 포함하는 값은 복수 label을
   추가하는 효과가 있다.

     예시 Template          확장 결과

       {.who}             .fred
       {.who,who}         .fred.fred
       {.half,who}        .50%25.fred
       www{.dom*}         www.example.com
       X{.var}            X.value
       X{.empty}          X.
       X{.undef}          X
       X{.var:3}          X.val
       X{.list}           X.red,green,blue
       X{.list*}          X.red.green.blue
       X{.keys}           X.semi,%3B,dot,.,comma,%2C
       X{.keys*}          X.semi=%3B.dot=..comma=%2C
       X{.empty_keys}     X
       X{.empty_keys*}    X

3.2.6.  경로 세그먼트 확장: {/var}

   Level 3 이상 template의 슬래시("/") 연산자로 표시되는 경로 세그먼트
   확장은 URI 경로 계층 구조를 기술하는 데 유용하다.

   variable-list의 각 정의된 변수에 대해, 결과 문자열에 "/"를 추가한 다음
   unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로
   변수 확장을 수행한다.

   경로 세그먼트 확장의 확장 과정은 "." 대신 "/"가 대체되는 것 외에는
   Label 확장의 과정과 동일함에 유의하라. 그러나 "."와 달리 "/"는
   reserved 문자이므로 값에서 발견되면 퍼센트 인코딩된다.

     예시 Template          확장 결과

       {/who}             /fred
       {/who,who}         /fred/fred
       {/half,who}        /50%25/fred
       {/who,dub}         /fred/me%2Ftoo
       {/var}             /value
       {/var,empty}       /value/
       {/var,undef}       /value
       {/var,x}/here      /value/1024/here
       {/var:1,var}       /v/value
       {/list}            /red,green,blue
       {/list*}           /red/green/blue
       {/list*,path:4}    /red/green/blue/%2Ffoo
       {/keys}            /semi,%3B,dot,.,comma,%2C
       {/keys*}           /semi=%3B/dot=./comma=%2C

3.2.7.  경로 스타일 파라미터 확장: {;var}

   Level 3 이상 template의 세미콜론(";") 연산자로 표시되는 경로 스타일
   파라미터 확장은 "path;property" 또는 "path;name=value"와 같은 URI 경로
   파라미터를 기술하는 데 유용하다.

   variable-list의 각 정의된 변수에 대해:

   o  결과 문자열에 ";"를 추가한다;

   o  변수가 단순 문자열 값을 가지거나 전개 수정자가 주어지지 않은 경우:

      *  변수 이름을 (리터럴 문자열인 것처럼 인코딩하여) 결과 문자열에
         추가한다;

      *  변수의 값이 비어 있지 않으면, 결과 문자열에 "="를 추가한다;

   o  unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로
      변수 확장을 수행한다.

     예시 Template          확장 결과

       {;who}             ;who=fred
       {;half}            ;half=50%25
       {;empty}           ;empty
       {;v,empty,who}     ;v=6;empty;who=fred
       {;v,bar,who}       ;v=6;who=fred
       {;x,y}             ;x=1024;y=768
       {;x,y,empty}       ;x=1024;y=768;empty
       {;x,y,undef}       ;x=1024;y=768
       {;hello:5}         ;hello=Hello
       {;list}            ;list=red,green,blue
       {;list*}           ;list=red;list=green;list=blue
       {;keys}            ;keys=semi,%3B,dot,.,comma,%2C
       {;keys*}           ;semi=%3B;dot=.;comma=%2C

3.2.8.  폼 스타일 쿼리 확장: {?var}

   Level 3 이상 template의 물음표("?") 연산자로 표시되는 폼 스타일 쿼리
   확장은 전체 선택적 쿼리 구성 요소를 기술하는 데 유용하다.

   variable-list의 각 정의된 변수에 대해:

   o  첫 번째 정의된 값이면 결과 문자열에 "?"를 추가하고, 그 이후에는
      "&"를 추가한다;

   o  변수가 단순 문자열 값을 가지거나 전개 수정자가 주어지지 않은 경우,
      변수 이름(리터럴 문자열인 것처럼 인코딩)과 등호 문자("=")를 결과
      문자열에 추가한다; 그리고

   o  unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로
      변수 확장을 수행한다.

     예시 Template          확장 결과

       {?who}             ?who=fred
       {?half}            ?half=50%25
       {?x,y}             ?x=1024&y=768
       {?x,y,empty}       ?x=1024&y=768&empty=
       {?x,y,undef}       ?x=1024&y=768
       {?var:3}           ?var=val
       {?list}            ?list=red,green,blue
       {?list*}           ?list=red&list=green&list=blue
       {?keys}            ?keys=semi,%3B,dot,.,comma,%2C
       {?keys*}           ?semi=%3B&dot=.&comma=%2C

3.2.9.  폼 스타일 쿼리 연속: {&var}

   Level 3 이상 template의 앰퍼샌드("&") 연산자로 표시되는 폼 스타일 쿼리
   연속은 고정 파라미터를 가진 리터럴 쿼리 구성 요소가 이미 포함된
   template에서 선택적 &name=value 쌍을 기술하는 데 유용하다.

   variable-list의 각 정의된 변수에 대해:

   o  결과 문자열에 "&"를 추가한다;

   o  변수가 단순 문자열 값을 가지거나 전개 수정자가 주어지지 않은 경우,
      변수 이름(리터럴 문자열인 것처럼 인코딩)과 등호 문자("=")를 결과
      문자열에 추가한다; 그리고

   o  unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로
      변수 확장을 수행한다.

     예시 Template          확장 결과

       {&who}             &who=fred
       {&half}            &half=50%25
       ?fixed=yes{&x}     ?fixed=yes&x=1024
       {&x,y,empty}       &x=1024&y=768&empty=
       {&x,y,undef}       &x=1024&y=768

       {&var:3}           &var=val
       {&list}            &list=red,green,blue
       {&list*}           &list=red&list=green&list=blue
       {&keys}            &keys=semi,%3B,dot,.,comma,%2C
       {&keys*}           &semi=%3B&dot=.&comma=%2C

4.  보안 고려 사항

   URI Template은 능동적이거나 실행 가능한 콘텐츠를 포함하지 않는다.
   그러나 공격자가 template에 대한 통제권을 부여받거나 확장에서 reserved
   문자를 허용하는 표현식 내의 변수 값에 대한 통제권을 부여받는 경우
   예상치 못한 URI를 만들어낼 수 있다. 어느 경우든, 보안 고려 사항은
   누가 template을 제공하는지, 누가 template 내의 변수에 사용할 값을
   제공하는지, 확장이 어떤 실행 컨텍스트(클라이언트 또는 서버)에서
   발생하는지, 그리고 결과 URI가 어디에서 사용되는지에 의해 크게
   결정된다.

   이 명세는 URI Template이 사용될 수 있는 장소를 제한하지 않는다.
   현재 구현은 서버 측 개발 프레임워크와 계산된 링크 또는 폼을 위한
   클라이언트 측 javascript에 존재한다.

   프레임워크 내에서 template은 일반적으로 나중의(요청 시) 클라이언트
   요청의 URI에서 데이터가 발생할 수 있는 위치에 대한 안내 역할을 한다.
   따라서 보안 우려는 template 자체에 있는 것이 아니라, 서버가 일반적인
   웹 요청 내에서 사용자 제공 데이터를 추출하고 처리하는 방법에 있다.

   클라이언트 측 구현에서 URI Template은 HTML 폼과 많은 동일한 속성을
   가지지만, URI 문자로 제한되고 메시지 본문 콘텐츠뿐만 아니라 HTTP 헤더
   필드 값에 포함될 수 있다. "javascript:"로 시작하는 것과 같은 잠재적으로
   위험한 URI 참조 문자열이 template과 값 모두 신뢰할 수 있는 소스에
   의해 제공되지 않는 한 확장에 나타나지 않도록 주의해야 한다.

   기타 보안 고려 사항은 [RFC3986]의 섹션 7에 설명된 URI에 대한 것과
   동일하다.

5.  감사의 글

   다음 분들이 이 명세에 기여하였다: Mike Burrows, Michaeljohn Clement,
   DeWitt Clinton, John Cowan, Stephen Farrell, Robbie Gates,
   Vijay K. Gurbani, Peter Johanson, Murray S. Kucherawy,
   James H. Manger, Tom Petch, Marc Portier, Pete Resnick,
   James Snell, Jiankang Yao.

6.  참고 문헌

6.1.  규범적 참고 문헌

   [ASCII]       American National Standards Institute, "Coded Character
                 Set - 7-bit American Standard Code for Information
                 Interchange", ANSI X3.4, 1986.

   [RFC2119]     Bradner, S., "Key words for use in RFCs to Indicate
                 Requirement Levels", BCP 14, RFC 2119, March 1997.

   [RFC3629]     Yergeau, F., "UTF-8, a transformation format of ISO
                 10646", STD 63, RFC 3629, November 2003.

   [RFC3986]     Berners-Lee, T., Fielding, R., and L. Masinter,
                 "Uniform Resource Identifier (URI): Generic Syntax",
                 STD 66, RFC 3986, January 2005.

   [RFC3987]     Duerst, M. and M. Suignard, "Internationalized Resource
                 Identifiers (IRIs)", RFC 3987, January 2005.

   [RFC5234]     Crocker, D. and P. Overell, "Augmented BNF for Syntax
                 Specifications: ABNF", STD 68, RFC 5234, January 2008.

   [RFC6365]     Hoffman, P. and J. Klensin, "Terminology Used in
                 Internationalization in the IETF", BCP 166, RFC 6365,
                 September 2011.

   [UNIV6]       The Unicode Consortium, "The Unicode Standard, Version
                 6.0.0", (Mountain View, CA: The Unicode Consortium,
                 2011.  ISBN 978-1-936213-01-6),
                 <http://www.unicode.org/versions/Unicode6.0.0/>.

   [UTR15]       Davis, M. and M. Duerst, "Unicode Normalization Forms",
                 Unicode Standard Annex # 15, April 2003,
                 <http://www.unicode.org/unicode/reports/tr15/
                 tr15-23.html>.

6.2.  정보적 참고 문헌

   [OpenSearch]  Clinton, D., "OpenSearch 1.1", Draft 5, December 2011,
                 <http://www.opensearch.org/Specifications/OpenSearch>.

   [UPU-S42]     Universal Postal Union, "International Postal Address
                 Components and Templates", UPU S42-1, November 2002,
                 <http://www.upu.int/en/activities/addressing/
                 standards.html>.

   [WADL]        Hadley, M., "Web Application Description Language",
                 World Wide Web Consortium Member Submission
                 SUBM-wadl-20090831, August 2009,
                 <http://www.w3.org/Submission/2009/
                 SUBM-wadl-20090831/>.

   [WSDL]        Weerawarana, S., Moreau, J., Ryman, A., and R.
                 Chinnici, "Web Services Description Language (WSDL)
                 Version 2.0 Part 1: Core Language", World Wide Web
                 Consortium Recommendation REC-wsdl20-20070626,
                 June 2007, <http://www.w3.org/TR/2007/
                 REC-wsdl20-20070626>.

부록 A.  구현 힌트

   확장에 대한 규범적 섹션은 서술적 명확성을 위해 각 연산자를 별도의
   확장 과정으로 기술한다. 실제 구현에서는 표현식이 왼쪽에서 오른쪽으로
   연산자별로 약간의 과정 변형만 있는 공통 알고리즘을 사용하여 처리될
   것으로 기대한다. 이 비규범적 부록은 그러한 알고리즘 중 하나를
   기술한다.

   빈 결과 문자열과 비오류 상태를 초기화한다.

   template 문자열을 스캔하고 리터럴을 결과 문자열에 복사한다(섹션 3.1에
   따라). "{"에 의해 표현식이 표시되거나, "{" 이외의 비리터럴 문자의
   존재에 의해 오류가 표시되거나, template이 끝날 때까지 계속한다.
   template이 끝나면, 결과 문자열과 현재 오류 또는 비오류 상태를 반환한다.

   o  표현식이 발견되면, 다음 "}"까지 template을 스캔하고 중괄호 사이의
      문자를 추출한다.

   o  "}" 전에 template이 끝나면, "{"와 추출된 문자를 결과 문자열에
      추가하고 표현식이 잘못되었음을 나타내는 오류 상태와 함께 반환한다.

   추출된 표현식의 첫 번째 문자에서 연산자를 확인한다.

   o  표현식이 끝났거나(즉, "{}"인 경우), 알려지지 않거나 구현되지 않은
      연산자가 발견되거나, 문자가 varchar 집합(섹션 2.3)에 포함되지 않는
      경우, "{", 추출된 표현식, "}"를 결과 문자열에 추가하고, 결과가 오류
      상태임을 기억한 다음, template의 나머지를 스캔하러 돌아간다.

   o  알려지고 구현된 연산자가 발견되면, 연산자를 저장하고 varspec-list를
      시작하기 위해 다음 문자로 건너뛴다.

   o  그렇지 않으면, 연산자를 NUL(단순 문자열 확장)로 저장한다.

   다음 값 표를 사용하여 표현식 유형 연산자별 처리 동작을 결정한다.
   "first" 항목은 표현식의 변수 중 하나라도 정의된 경우 결과에 먼저
   추가할 문자열이다. "sep" 항목은 두 번째(또는 이후의) 정의된 변수 확장
   전에 결과에 추가할 구분자이다. "named" 항목은 전개 수정자가 주어지지
   않을 때 확장이 변수 또는 키 이름을 포함하는지 여부에 대한 불리언이다.
   "ifemp" 항목은 해당 값이 비어 있을 때 이름에 추가할 문자열이다.
   "allow" 항목은 값 확장 내에서 인코딩되지 않은 채 허용할 문자를
   나타낸다: (U)는 unreserved 집합에 없는 모든 문자가 인코딩됨을 의미한다;
   (U+R)은 (unreserved / reserved / pct-encoding)의 합집합에 없는 모든
   문자가 인코딩됨을 의미한다; 그리고 두 경우 모두, 허용되지 않는 각
   문자는 먼저 UTF-8의 옥텟 시퀀스로 인코딩된 다음 각 옥텟이 퍼센트 인코딩
   삼중항으로 인코딩된다.

   .------------------------------------------------------------------.
   |          NUL     +      .       /       ;      ?      &      #   |
   |------------------------------------------------------------------|
   | first |  ""     ""     "."     "/"     ";"    "?"    "&"    "#"  |
   | sep   |  ","    ","    "."     "/"     ";"    "&"    "&"    ","  |
   | named | false  false  false   false   true   true   true   false |
   | ifemp |  ""     ""     ""      ""      ""     "="    "="    ""   |
   | allow |   U     U+R     U       U       U      U      U     U+R  |
   `------------------------------------------------------------------'

   위의 표를 염두에 두고, variable-list를 다음과 같이 처리한다:

   각 varspec에 대해, 표현식에서 varchar 집합에 포함되지 않는 문자가
   발견되거나 표현식의 끝에 도달할 때까지 variable-list를 스캔하여
   변수 이름과 선택적 수정자를 추출한다.

   o  표현식의 끝이고 varname이 비어 있으면, template의 나머지를 스캔하러
      돌아간다.

   o  표현식의 끝이 아니고 마지막으로 발견된 문자가 수정자("*" 또는 ":")를
      나타내면, 해당 수정자를 기억한다. 전개("*")이면 다음 문자를
      스캔한다. 접두사(":")이면 10진 정수로 표현된 max-length를 위해
      다음 1~4개의 문자를 계속 스캔한 다음, 아직 표현식의 끝이 아니면
      다음 문자를 스캔한다.

   o  표현식의 끝이 아니고 마지막으로 발견된 문자가 쉼표(",")가 아니면,
      "{", 저장된 연산자(있는 경우), 스캔된 varname과 수정자, 나머지
      표현식, "}"를 결과 문자열에 추가하고, 결과가 오류 상태임을 기억한
      다음, template의 나머지를 스캔하러 돌아간다.

   스캔된 변수 이름에 대한 값을 조회한 다음

   o  varname이 알려지지 않았거나 정의되지 않은 값(섹션 2.3)을 가진
      변수에 해당하면, 다음 varspec으로 건너뛴다.

   o  이것이 이 표현식의 첫 번째 정의된 변수이면, 이 표현식 유형의
      first 문자열을 결과 문자열에 추가하고 완료되었음을 기억한다.
      그렇지 않으면, sep 문자열을 결과 문자열에 추가한다.

   o  이 변수의 값이 문자열이면:

      *  named가 true이면, 리터럴과 동일한 인코딩 과정을 사용하여
         varname을 결과 문자열에 추가하고,

         +  값이 비어 있으면, ifemp 문자열을 결과 문자열에 추가하고
            다음 varspec으로 건너뛴다;

         +  그렇지 않으면, 결과 문자열에 "="를 추가한다.

      *  접두사 수정자가 있고 접두사 길이가 Unicode 문자 수 기준으로
         값 문자열 길이보다 작으면, 값 문자열의 시작 부분에서 해당
         수의 문자를 allow 집합에 없는 문자를 퍼센트 인코딩한 후 결과
         문자열에 추가하되, 단일 Unicode 코드 포인트를 나타내는 다중
         옥텟 또는 퍼센트 인코딩 삼중항 문자를 분할하지 않도록 주의한다;

      *  그렇지 않으면, allow 집합에 없는 문자를 퍼센트 인코딩한 후
         값을 결과 문자열에 추가한다.

   o  전개 수정자가 주어지지 않은 경우:

      *  named가 true이면, 리터럴과 동일한 인코딩 과정을 사용하여
         varname을 결과 문자열에 추가하고,

         +  값이 비어 있으면, ifemp 문자열을 결과 문자열에 추가하고
            다음 varspec으로 건너뛴다;

         +  그렇지 않으면, 결과 문자열에 "="를 추가한다; 그리고

      *  이 변수의 값이 목록이면, 정의된 각 목록 구성원을 allow 집합에
         없는 문자를 퍼센트 인코딩한 후 결과 문자열에 추가하되, 정의된
         각 목록 구성원 사이에 쉼표(",")를 결과에 추가한다;

      *  이 변수의 값이 연관 배열 또는 기타 형태의 쌍(name, value) 구조이면,
         정의된 값을 가진 각 쌍을 allow 집합에 없는 문자를 퍼센트
         인코딩한 후 "name,value"로 결과 문자열에 추가하되, 정의된 각
         쌍 사이에 쉼표(",")를 결과에 추가한다.

   o  전개 수정자가 주어진 경우:

      *  named가 true이면, 정의된 각 목록 구성원 또는 정의된 값을 가진
         배열(name, value) 쌍에 대해 다음을 수행한다:

         +  첫 번째 정의된 구성원/값이 아니면, sep 문자열을 결과 문자열에
            추가한다;

         +  목록이면, 리터럴과 동일한 인코딩 과정을 사용하여 varname을
            결과 문자열에 추가한다;

         +  쌍이면, 리터럴과 동일한 인코딩 과정을 사용하여 name을 결과
            문자열에 추가한다;

         +  구성원/값이 비어 있으면, ifemp 문자열을 결과 문자열에 추가한다;
            그렇지 않으면, "="와 구성원/값을 allow 집합에 없는 구성원/값
            문자를 퍼센트 인코딩한 후 결과 문자열에 추가한다.

      *  named가 false이면:

         +  목록이면, 정의된 각 목록 구성원을 allow 집합에 없는 문자를
            퍼센트 인코딩한 후 결과 문자열에 추가하되, 정의된 각 목록
            구성원 사이에 sep 문자열을 결과에 추가한다.

         +  (name, value) 쌍의 배열이면, 정의된 값을 가진 각 쌍을
            allow 집합에 없는 문자를 퍼센트 인코딩한 후 "name=value"로
            결과 문자열에 추가하되, 정의된 각 쌍 사이에 sep 문자열을
            결과에 추가한다.

   이 표현식의 variable-list가 소진되면, template의 나머지를 스캔하러
   돌아간다.

저자 주소

   Joe Gregorio
   Google

   EMail: joe@bitworking.org
   URI:   http://bitworking.org/

   Roy T. Fielding
   Adobe Systems Incorporated

   EMail: fielding@gbiv.com
   URI:   http://roy.gbiv.com/

   Marc Hadley
   The MITRE Corporation

   EMail: mhadley@mitre.org
   URI:   http://mitre.org/

   Mark Nottingham
   Rackspace

   EMail: mnot@mnot.net
   URI:   http://www.mnot.net/

   David Orchard
   Salesforce.com

   EMail: orchard@pacificspirit.com
   URI:   http://www.pacificspirit.com/
