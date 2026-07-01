Internet Engineering Task Force (IETF)                      T. Bray, Ed.
Request for Comments: 8259                                    Textuality
Obsoletes: 7159                                            December 2017
Category: Standards Track
ISSN: 2070-1721


     JavaScript Object Notation (JSON) 데이터 교환 형식

초록

   JavaScript Object Notation (JSON)은 경량의 텍스트 기반이며
   언어 독립적인 데이터 교환 형식이다. 이 형식은 ECMAScript 프로그래밍
   언어 표준에서 파생되었다. JSON은 구조화된 데이터의 이식 가능한 표현을
   위한 소규모 서식 규칙 집합을 정의한다.

   이 문서는 JSON의 다른 명세들과의 불일치를 제거하고, 명세 오류를
   수정하며, 경험에 기반한 상호운용성 지침을 제공한다.

이 메모의 상태

   이 문서는 Internet Standards Track 문서이다.

   이 문서는 Internet Engineering Task Force (IETF)의 산출물이다.
   이 문서는 IETF 커뮤니티의 합의를 대표한다. 이 문서는 공개 검토를
   받았으며 Internet Engineering Steering Group (IESG)에 의해 출판이
   승인되었다. Internet Standards에 대한 추가 정보는 RFC 7841의
   섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이 문서에 대한 피드백을 제공하는
   방법에 대한 정보는 https://www.rfc-editor.org/info/rfc8259 에서
   확인할 수 있다.

저작권 고지

   Copyright (c) 2017 IETF Trust 및 문서 저자로 식별된 사람들.
   모든 권리 보유.

   이 문서는 이 문서의 발행일에 유효한 BCP 78 및 IETF Trust의 IETF
   문서 관련 법적 조항 (https://trustee.ietf.org/license-info)의
   적용을 받는다. 이 문서들은 이 문서에 대한 귀하의 권리와 제한을
   설명하므로 주의 깊게 검토하기 바란다. 이 문서에서 추출된 코드
   구성요소는 Trust Legal Provisions의 섹션 4.e에 설명된 바와 같이
   Simplified BSD License 텍스트를 포함해야 하며, Simplified BSD
   License에 설명된 대로 보증 없이 제공된다.

   이 문서에는 2008년 11월 10일 이전에 출판되거나 공개적으로 제공된
   IETF 문서 또는 IETF 기여물의 자료가 포함되어 있을 수 있다. 이
   자료의 일부에 대한 저작권을 관리하는 사람(들)이 IETF Standards
   Process 외부에서 해당 자료의 수정을 허용할 권리를 IETF Trust에
   부여하지 않았을 수 있다. 해당 자료의 저작권을 관리하는 사람(들)로부터
   적절한 라이선스를 취득하지 않는 한, 이 문서는 IETF Standards Process
   외부에서 수정될 수 없으며, 이 문서의 파생 저작물은 RFC로 출판하기
   위한 서식 변환이나 영어 이외의 언어로 번역하는 경우를 제외하고 IETF
   Standards Process 외부에서 작성될 수 없다.

목차

   1.  소개  . . . . . . . . . . . . . . . . . . . . . . . . . . .   3
     1.1.  이 문서에서 사용하는 규약 . . . . . . . . . . . . . . .   4
     1.2.  JSON의 명세들  . . . . . . . . . . . . . . . . . . . . .   4
     1.3.  이 개정판에 대한 소개  . . . . . . . . . . . . . . . . .   5
   2.  JSON 문법  . . . . . . . . . . . . . . . . . . . . . . . . .   5
   3.  값  . . . . . . . . . . . . . . . . . . . . . . . . . . . .   6
   4.  객체  . . . . . . . . . . . . . . . . . . . . . . . . . . .   6
   5.  배열  . . . . . . . . . . . . . . . . . . . . . . . . . . .   7
   6.  숫자  . . . . . . . . . . . . . . . . . . . . . . . . . . .   7
   7.  문자열  . . . . . . . . . . . . . . . . . . . . . . . . . .   8
   8.  문자열 및 문자 관련 사항  . . . . . . . . . . . . . . . . .   9
     8.1.  문자 인코딩  . . . . . . . . . . . . . . . . . . . . . .   9
     8.2.  유니코드 문자  . . . . . . . . . . . . . . . . . . . . .  10
     8.3.  문자열 비교  . . . . . . . . . . . . . . . . . . . . . .  10
   9.  파서  . . . . . . . . . . . . . . . . . . . . . . . . . . .  10
   10. 생성기  . . . . . . . . . . . . . . . . . . . . . . . . . .  10
   11. IANA 고려사항  . . . . . . . . . . . . . . . . . . . . . . .  11
   12. 보안 고려사항  . . . . . . . . . . . . . . . . . . . . . . .  12
   13. 예제  . . . . . . . . . . . . . . . . . . . . . . . . . . .  12
   14. 참고문헌  . . . . . . . . . . . . . . . . . . . . . . . . .  14
     14.1.  규범적 참고문헌  . . . . . . . . . . . . . . . . . . .  14
     14.2.  참고적 참고문헌  . . . . . . . . . . . . . . . . . . .  14
   부록 A.  RFC 7159로부터의 변경사항  . . . . . . . . . . . . . .  16
   기여자  . . . . . . . . . . . . . . . . . . . . . . . . . . . .  16
   저자 주소  . . . . . . . . . . . . . . . . . . . . . . . . . . .  16

1.  소개

   JavaScript Object Notation (JSON)은 구조화된 데이터의 직렬화를
   위한 텍스트 형식이다. 이 형식은 ECMAScript Programming Language
   Standard, Third Edition [ECMA-262]에 정의된 JavaScript의 객체
   리터럴에서 파생되었다.

   JSON은 네 가지 원시 타입(문자열, 숫자, 불리언, null)과 두 가지
   구조화된 타입(객체와 배열)을 표현할 수 있다.

   문자열은 0개 이상의 유니코드 문자 [UNICODE]의 시퀀스이다. 이 인용은
   특정 릴리스가 아닌 유니코드의 최신 버전을 참조한다는 점에 유의한다.
   유니코드 명세의 향후 변경이 JSON의 구문에 영향을 미칠 것으로
   예상되지 않는다.

   객체는 0개 이상의 이름/값 쌍의 순서 없는 컬렉션이며, 여기서 이름은
   문자열이고 값은 문자열, 숫자, 불리언, null, 객체 또는 배열이다.

   배열은 0개 이상의 값의 순서 있는 시퀀스이다.

   "object"와 "array"라는 용어는 JavaScript의 관례에서 유래한다.

   JSON의 설계 목표는 최소성, 이식성, 텍스트 기반이면서 JavaScript의
   하위 집합이 되는 것이었다.

1.1.  이 문서에서 사용하는 규약

   이 문서에서 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
   "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY",
   "OPTIONAL"이라는 키워드는 여기에 표시된 것처럼 모두 대문자로
   나타나는 경우에 한하여 BCP 14 [RFC2119] [RFC8174]에 설명된 대로
   해석되어야 한다.

   이 문서의 문법 규칙은 [RFC5234]에 설명된 대로 해석되어야 한다.

1.2.  JSON의 명세들

   이 문서는 [RFC7159]를 대체한다. [RFC7159]는 원래 JSON을 설명하고
   미디어 타입 "application/json"을 등록한 [RFC4627]을 폐기(obsolete)
   시켰다.

   JSON은 또한 [ECMA-404]에 설명되어 있다.

   앞 문장에서의 ECMA-404 참조는 규범적(normative)이며, 이는 구현자가
   이 문서를 이해하기 위해 참조해야 한다는 일반적인 의미가 아니라,
   "JSON text"라는 용어의 정의에 있어 어떤 명세에서도 불일치가 없음을
   강조하기 위한 것이다. 다만, ECMA-404는 이 명세가 최대한의
   상호운용성을 위해 피할 것을 권장하는 몇 가지 관행을 허용한다는
   점에 유의한다.

   의도는 두 문서 사이의 문법이 동일하다는 것이며, 비록 서로 다른
   설명이 사용되지만 그러하다. 두 문서 사이에 차이가 발견되면, ECMA와
   IETF는 두 문서를 모두 업데이트하기 위해 협력할 것이다.

   어느 한쪽 문서에서 오류가 발견되면, 다른 쪽 문서에도 유사한 오류가
   있는지 검토해야 하며, 있다면 가능한 한 수정되어야 한다.

   향후 어느 한쪽 문서가 변경되면, ECMA와 IETF는 변경을 통해 두 문서가
   정렬된 상태를 유지하도록 협력할 것이다.

1.3.  이 개정판에 대한 소개

   RFC 4627이 발행된 이후 수년간, JSON은 매우 폭넓게 사용되었다. 이
   경험을 통해 명세에 의해 허용되지만 상호운용성 문제를 야기하는 특정
   패턴들이 드러났다.

   또한, RFC 4627에 대해 소수의 정오표가 보고되었으며 (RFC Errata IDs
   607 [Err607] 및 3607 [Err3607] 참조), RFC 7159에 대해서도 마찬가지
   이다 (RFC Errata IDs 3915 [Err3915], 4264 [Err4264], 4336
   [Err4336], 및 4388 [Err4388] 참조).

   이 문서의 목표는 정오표를 적용하고, JSON의 다른 명세들과의 불일치를
   제거하며, 상호운용성 문제를 초래할 수 있는 관행을 강조하는 것이다.

2.  JSON 문법

   JSON 텍스트는 토큰의 시퀀스이다. 토큰 집합에는 6개의 구조 문자,
   문자열, 숫자, 그리고 3개의 리터럴 이름이 포함된다.

   JSON 텍스트는 직렬화된 값이다. JSON의 이전 명세 중 일부는 JSON
   텍스트를 객체 또는 배열로 제한했었다는 점에 유의한다. JSON 텍스트가
   요구되는 곳에서 객체 또는 배열만 생성하는 구현체는, 모든 구현체가
   이를 적합한 JSON 텍스트로 받아들일 것이라는 의미에서 상호운용
   가능할 것이다.

      JSON-text = ws value ws

   다음은 6개의 구조 문자이다:

      begin-array     = ws %x5B ws  ; [ 왼쪽 대괄호

      begin-object    = ws %x7B ws  ; { 왼쪽 중괄호

      end-array       = ws %x5D ws  ; ] 오른쪽 대괄호

      end-object      = ws %x7D ws  ; } 오른쪽 중괄호

      name-separator  = ws %x3A ws  ; : 콜론

      value-separator = ws %x2C ws  ; , 쉼표

   6개의 구조 문자 앞이나 뒤에 의미 없는 공백이 허용된다.

      ws = *(
              %x20 /              ; Space
              %x09 /              ; Horizontal tab
              %x0A /              ; Line feed or New line
              %x0D )              ; Carriage return

3.  값

   JSON 값은 객체, 배열, 숫자, 문자열, 또는 다음 세 가지 리터럴 이름
   중 하나여야 한다(MUST):

      false
      null
      true

   리터럴 이름은 반드시 소문자여야 한다(MUST). 다른 리터럴 이름은
   허용되지 않는다.

      value = false / null / true / object / array / number / string

      false = %x66.61.6c.73.65   ; false

      null  = %x6e.75.6c.6c      ; null

      true  = %x74.72.75.65      ; true

4.  객체

   객체 구조는 0개 이상의 이름/값 쌍(또는 멤버)을 둘러싸는 한 쌍의
   중괄호로 표현된다. 이름은 문자열이다. 각 이름 뒤에 단일 콜론이
   오며, 이름과 값을 구분한다. 단일 쉼표가 값과 뒤따르는 이름을
   구분한다. 객체 내의 이름은 고유해야 한다(SHOULD).

      object = begin-object [ member *( value-separator member ) ]
               end-object

      member = string name-separator value

   이름이 모두 고유한 객체는, 해당 객체를 수신하는 모든 소프트웨어
   구현체가 이름-값 매핑에 동의할 것이라는 의미에서 상호운용 가능하다.
   객체 내의 이름이 고유하지 않은 경우, 그러한 객체를 수신하는
   소프트웨어의 동작은 예측할 수 없다. 많은 구현체는 마지막 이름/값
   쌍만을 보고한다. 다른 구현체는 오류를 보고하거나 객체를 파싱하지
   못하며, 일부 구현체는 중복을 포함한 모든 이름/값 쌍을 보고한다.

   JSON 파싱 라이브러리들은 객체 멤버의 순서를 호출 소프트웨어에
   표시하는지 여부에 있어 차이가 있는 것으로 관찰되었다. 멤버 순서에
   동작이 의존하지 않는 구현체는, 이러한 차이에 영향을 받지 않을
   것이라는 의미에서 상호운용 가능할 것이다.

5.  배열

   배열 구조는 0개 이상의 값(또는 요소)을 둘러싸는 대괄호로 표현된다.
   요소들은 쉼표로 구분된다.

   array = begin-array [ value *( value-separator value ) ] end-array

   배열 내의 값이 동일한 타입이어야 한다는 요구사항은 없다.

6.  숫자

   숫자의 표현은 대부분의 프로그래밍 언어에서 사용되는 것과 유사하다.
   숫자는 10진 숫자를 사용하여 10진수로 표현된다. 숫자는 선택적
   마이너스 부호가 앞에 올 수 있는 정수 부분을 포함하며, 소수 부분
   및/또는 지수 부분이 뒤따를 수 있다. 선행 0은 허용되지 않는다.

   소수 부분은 소수점 뒤에 하나 이상의 숫자가 오는 것이다.

   지수 부분은 대문자 또는 소문자 E로 시작하며, 그 뒤에 더하기 또는
   빼기 부호가 올 수 있다. E와 선택적 부호 뒤에 하나 이상의 숫자가
   따라온다.

   아래 문법으로 표현할 수 없는 숫자 값(예: Infinity와 NaN)은
   허용되지 않는다.

      number = [ minus ] int [ frac ] [ exp ]

      decimal-point = %x2E       ; .

      digit1-9 = %x31-39         ; 1-9

      e = %x65 / %x45            ; e E

      exp = e [ minus / plus ] 1*DIGIT

      frac = decimal-point 1*DIGIT

      int = zero / ( digit1-9 *DIGIT )

      minus = %x2D               ; -

      plus = %x2B                ; +

      zero = %x30                ; 0

   이 명세는 구현체가 수용하는 숫자의 범위와 정밀도에 대한 제한을
   설정하는 것을 허용한다. IEEE 754 binary64 (배정밀도) 숫자
   [IEEE754]를 구현하는 소프트웨어가 일반적으로 사용 가능하고 널리
   사용되므로, 구현체가 이것이 제공하는 것 이상의 정밀도나 범위를
   기대하지 않는다면 좋은 상호운용성을 달성할 수 있으며, 이는 구현체가
   예상되는 정밀도 내에서 JSON 숫자를 근사화할 것이라는 의미에서
   그러하다. 1E400이나 3.141592653589793238462643383279와 같은 JSON
   숫자는 잠재적인 상호운용성 문제를 나타낼 수 있는데, 이는 해당
   숫자를 만든 소프트웨어가 수신 소프트웨어가 널리 이용 가능한 것보다
   더 큰 숫자 크기 및 정밀도 역량을 가지고 있을 것으로 기대함을
   시사하기 때문이다.

   이러한 소프트웨어가 사용될 때, 정수이면서 [-(253)+1, (253)-1]
   범위에 있는 숫자는, 구현체가 그 숫자 값에 대해 정확히 동의할
   것이라는 의미에서 상호운용 가능하다는 점에 유의한다.

7.  문자열

   문자열의 표현은 C 계열 프로그래밍 언어에서 사용되는 관례와 유사하다.
   문자열은 따옴표로 시작하고 끝난다. 반드시 이스케이프해야 하는(MUST)
   문자들인 따옴표, 역슬래시, 그리고 제어 문자(U+0000에서 U+001F까지)를
   제외한 모든 유니코드 문자가 따옴표 안에 놓일 수 있다.

   모든 문자는 이스케이프될 수 있다. 해당 문자가 기본 다국어 평면
   (U+0000에서 U+FFFF까지)에 있다면, 6문자 시퀀스로 표현될 수 있다:
   역슬래시 뒤에 소문자 u가 오고, 그 뒤에 해당 문자의 코드 포인트를
   인코딩하는 4자리 16진수가 따라온다. 16진수 문자 A부터 F는 대문자
   또는 소문자일 수 있다. 예를 들어, 단일 역슬래시 문자만 포함하는
   문자열은 "\u005C"로 표현될 수 있다.

   대안으로, 일부 자주 사용되는 문자에 대한 2문자 시퀀스 이스케이프
   표현이 있다. 예를 들어, 단일 역슬래시 문자만 포함하는 문자열은
   더 간결하게 "\\"로 표현될 수 있다.

   기본 다국어 평면에 없는 확장 문자를 이스케이프하기 위해, 해당
   문자는 UTF-16 서로게이트 쌍을 인코딩하는 12문자 시퀀스로 표현된다.
   예를 들어, G 음자리표 문자(U+1D11E)만 포함하는 문자열은
   "\uD834\uDD1E"로 표현될 수 있다.

      string = quotation-mark *char quotation-mark

      char = unescaped /
          escape (
              %x22 /          ; "    quotation mark  U+0022
              %x5C /          ; \    reverse solidus U+005C
              %x2F /          ; /    solidus         U+002F
              %x62 /          ; b    backspace       U+0008
              %x66 /          ; f    form feed       U+000C
              %x6E /          ; n    line feed       U+000A
              %x72 /          ; r    carriage return U+000D
              %x74 /          ; t    tab             U+0009
              %x75 4HEXDIG )  ; uXXXX                U+XXXX

      escape = %x5C              ; \

      quotation-mark = %x22      ; "

      unescaped = %x20-21 / %x23-5B / %x5D-10FFFF

8.  문자열 및 문자 관련 사항

8.1.  문자 인코딩

   폐쇄된 에코시스템의 일부가 아닌 시스템 간에 교환되는 JSON 텍스트는
   UTF-8 [RFC3629]을 사용하여 인코딩되어야 한다(MUST).

   JSON의 이전 명세들은 JSON 텍스트를 전송할 때 UTF-8의 사용을
   요구하지 않았다. 그러나 JSON 기반 소프트웨어 구현의 대다수가
   UTF-8 인코딩을 사용하기로 선택했으며, 이는 상호운용성을 달성하는
   유일한 인코딩이 될 정도이다.

   구현체는 네트워크로 전송되는 JSON 텍스트의 시작에 바이트 순서 표시
   (U+FEFF)를 추가해서는 안 된다(MUST NOT). 상호운용성을 위해, JSON
   텍스트를 파싱하는 구현체는 바이트 순서 표시의 존재를 오류로
   처리하기보다 무시할 수 있다(MAY).

8.2.  유니코드 문자

   JSON 텍스트에 표현된 모든 문자열이 (이스케이프되었든 아니든) 전적으로
   유니코드 문자 [UNICODE]로 구성되어 있다면, 그 JSON 텍스트는
   이를 파싱하는 모든 소프트웨어 구현체가 객체와 배열 내의 이름과
   문자열 값의 내용에 동의할 것이라는 의미에서 상호운용 가능하다.

   그러나 이 명세의 ABNF는 멤버 이름과 문자열 값에 유니코드 문자를
   인코딩할 수 없는 비트 시퀀스를 포함하는 것을 허용한다; 예를 들어,
   "\uDEAD" (짝이 없는 단일 UTF-16 서로게이트). 이러한 사례는 예를
   들어, 라이브러리가 절단이 서로게이트 쌍을 분리하는지 확인하지
   않고 UTF-16 문자열을 절단할 때 관찰되었다. 이러한 값을 포함하는
   JSON 텍스트를 수신하는 소프트웨어의 동작은 예측할 수 없다; 예를
   들어, 구현체가 문자열 값의 길이에 대해 서로 다른 값을 반환하거나
   치명적인 런타임 예외를 겪을 수 있다.

8.3.  문자열 비교

   소프트웨어 구현체는 일반적으로 객체 멤버의 이름에 대한 동등성
   테스트를 수행해야 한다. 텍스트 표현을 유니코드 코드 유닛의
   시퀀스로 변환한 다음 코드 유닛별로 수치적으로 비교를 수행하는
   구현체는, 구현체가 두 문자열의 동등 또는 비동등에 대해 모든
   경우에 동의할 것이라는 의미에서 상호운용 가능하다. 예를 들어,
   이스케이프된 문자를 변환하지 않고 문자열을 비교하는 구현체는
   "a\\b"와 "a\u005Cb"가 같지 않다고 잘못 판단할 수 있다.

9.  파서

   JSON 파서는 JSON 텍스트를 다른 표현으로 변환한다. JSON 파서는
   JSON 문법에 적합한 모든 텍스트를 수용해야 한다(MUST). JSON
   파서는 JSON이 아닌 형식이나 확장을 수용할 수 있다(MAY).

   구현체는 수용하는 텍스트의 크기에 제한을 설정할 수 있다. 구현체는
   최대 중첩 깊이에 제한을 설정할 수 있다. 구현체는 숫자의 범위와
   정밀도에 제한을 설정할 수 있다. 구현체는 문자열의 길이와 문자
   내용에 제한을 설정할 수 있다.

10.  생성기

   JSON 생성기는 JSON 텍스트를 생산한다. 생성된 텍스트는 반드시
   JSON 문법에 엄격히 적합해야 한다(MUST).

11.  IANA 고려사항

   JSON 텍스트의 미디어 타입은 application/json이다.

   Type name:  application

   Subtype name:  json

   Required parameters:  n/a

   Optional parameters:  n/a

   Encoding considerations:  binary

   Security considerations:  RFC 8259, 섹션 12 참조

   Interoperability considerations:  RFC 8259에 설명됨

   Published specification:  RFC 8259

   Applications that use this media type:
      JSON은 다음의 모든 프로그래밍 언어로 작성된 애플리케이션 간에
      데이터를 교환하는 데 사용되어 왔다: ActionScript, C, C#,
      Clojure, ColdFusion, Common Lisp, E, Erlang, Go, Java,
      JavaScript, Lua, Objective CAML, Perl, PHP, Python, Rebol,
      Ruby, Scala, Scheme.

   Additional information:
      Magic number(s): n/a
      File extension(s): .json
      Macintosh file type code(s): TEXT

   Person & email address to contact for further information:
      IESG
      <iesg@ietf.org>

   Intended usage:  COMMON

   Restrictions on usage:  none

   Author:
      Douglas Crockford
      <douglas@crockford.com>

   Change controller:
      IESG
      <iesg@ietf.org>

   Note:  이 등록에 대해 "charset" 파라미터는 정의되지 않는다.
      파라미터를 추가하더라도 적합한 수신자에게는 실질적인 효과가 없다.

12.  보안 고려사항

   일반적으로 스크립팅 언어에는 보안 문제가 있다. JSON은 JavaScript의
   하위 집합이지만 대입과 호출을 제외한다.

   JSON의 구문이 JavaScript에서 차용되었으므로, 해당 언어의 "eval()"
   함수를 사용하여 대부분의 JSON 텍스트를 파싱할 수 있다 (그러나
   전부는 아니다; U+2028 LINE SEPARATOR 및 U+2029 PARAGRAPH
   SEPARATOR와 같은 특정 문자는 JSON에서는 합법적이지만 JavaScript
   에서는 그렇지 않다). 텍스트에 데이터 선언과 함께 실행 가능한
   코드가 포함될 수 있으므로, 이는 일반적으로 허용할 수 없는 보안
   위험을 구성한다. JSON 텍스트가 해당 언어의 구문에 적합한 다른
   프로그래밍 언어에서 eval()과 유사한 함수를 사용하는 경우에도
   동일한 고려사항이 적용된다.

13.  예제

   다음은 JSON 객체이다:

      {
        "Image": {
            "Width":  800,
            "Height": 600,
            "Title":  "View from 15th Floor",
            "Thumbnail": {
                "Url":    "http://www.example.com/image/481989943",
                "Height": 125,
                "Width":  100
            },
            "Animated" : false,
            "IDs": [116, 943, 234, 38793]
          }
      }

   이 객체의 Image 멤버는 Thumbnail 멤버가 객체이고 IDs 멤버가 숫자
   배열인 객체이다.

   다음은 두 개의 객체를 포함하는 JSON 배열이다:

      [
        {
           "precision": "zip",
           "Latitude":  37.7668,
           "Longitude": -122.3959,
           "Address":   "",
           "City":      "SAN FRANCISCO",
           "State":     "CA",
           "Zip":       "94107",
           "Country":   "US"
        },
        {
           "precision": "zip",
           "Latitude":  37.371991,
           "Longitude": -122.026020,
           "Address":   "",
           "City":      "SUNNYVALE",
           "State":     "CA",
           "Zip":       "94085",
           "Country":   "US"
        }
      ]

   다음은 값만 포함하는 세 개의 작은 JSON 텍스트이다:

   "Hello world!"

   42

   true

14.  참고문헌

14.1.  규범적 참고문헌

   [ECMA-404] Ecma International, "The JSON Data Interchange Format",
              Standard ECMA-404,
              <http://www.ecma-international.org/publications/
              standards/Ecma-404.htm>.

   [IEEE754]  IEEE, "IEEE Standard for Floating-Point Arithmetic",
              IEEE 754.

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119,
              DOI 10.17487/RFC2119, March 1997,
              <https://www.rfc-editor.org/info/rfc2119>.

   [RFC3629]  Yergeau, F., "UTF-8, a transformation format of ISO
              10646", STD 63, RFC 3629, DOI 10.17487/RFC3629, November
              2003, <https://www.rfc-editor.org/info/rfc3629>.

   [RFC5234]  Crocker, D., Ed. and P. Overell, "Augmented BNF for Syntax
              Specifications: ABNF", STD 68, RFC 5234,
              DOI 10.17487/RFC5234, January 2008,
              <https://www.rfc-editor.org/info/rfc5234>.

   [RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
              2119 Key Words", BCP 14, RFC 8174, DOI 10.17487/RFC8174,
              May 2017, <https://www.rfc-editor.org/info/rfc8174>.

   [UNICODE]  The Unicode Consortium, "The Unicode Standard",
              <http://www.unicode.org/versions/latest/>.

14.2.  참고적 참고문헌

   [ECMA-262] Ecma International, "ECMAScript Language Specification",
              Standard ECMA-262, Third Edition, December 1999,
              <http://www.ecma-international.org/publications/files/
              ECMA-ST-ARCH/
              ECMA-262,%203rd%20edition,%20December%201999.pdf>.

   [Err3607]  RFC Errata, Erratum ID 3607, RFC 4627,
              <https://www.rfc-editor.org/errata/eid3607>.

   [Err3915]  RFC Errata, Erratum ID 3915, RFC 7159,
              <https://www.rfc-editor.org/errata/eid3915>.

   [Err4264]  RFC Errata, Erratum ID 4264, RFC 7159,
              <https://www.rfc-editor.org/errata/eid4264>.

   [Err4336]  RFC Errata, Erratum ID 4336, RFC 7159,
              <https://www.rfc-editor.org/errata/eid4336>.

   [Err4388]  RFC Errata, Erratum ID 4388, RFC 7159,
              <https://www.rfc-editor.org/errata/eid4388>.

   [Err607]   RFC Errata, Erratum ID 607, RFC 4627,
              <https://www.rfc-editor.org/errata/eid607>.

   [RFC4627]  Crockford, D., "The application/json Media Type for
              JavaScript Object Notation (JSON)", RFC 4627,
              DOI 10.17487/RFC4627, July 2006,
              <https://www.rfc-editor.org/info/rfc4627>.

   [RFC7159]  Bray, T., Ed., "The JavaScript Object Notation (JSON) Data
              Interchange Format", RFC 7159, DOI 10.17487/RFC7159, March
              2014, <https://www.rfc-editor.org/info/rfc7159>.

부록 A.  RFC 7159로부터의 변경사항

   이 섹션은 이 문서와 RFC 7159의 텍스트 사이의 변경사항을 나열한다.

   o  섹션 1.2가 ECMA-262에서 JSON 명세의 제거를 반영하고, ECMA-404를
      규범적 참고문헌으로 만들며, "규범적(normative)"의 특정한 의미를
      설명하도록 업데이트되었다.

   o  섹션 1.3이 RFC 4627이 아닌 RFC 7159에 대해 제출된 정오표를
      반영하도록 업데이트되었다.

   o  섹션 8.1이 네트워크를 통해 전송될 때 UTF-8의 사용을 요구하도록
      변경되었다.

   o  섹션 12가 ECMAScript "eval()" 함수 사용으로 인한 보안 위험에
      대한 설명의 정확도를 높이도록 업데이트되었다.

   o  섹션 14.1이 ECMA-404를 규범적 참고문헌으로 포함하도록
      업데이트되었다.

   o  섹션 14.2가 ECMA-404를 제거하고, ECMA-262의 버전을 업데이트하며,
      정오표 목록을 갱신하도록 업데이트되었다.

기여자

   RFC 4627은 Douglas Crockford에 의해 작성되었다. 이 문서는 해당
   문서에 비교적 적은 수의 변경을 가하여 구성되었다; 따라서 여기
   텍스트의 대부분은 그의 것이다.

저자 주소

   Tim Bray (editor)
   Textuality

   Email: tbray@textuality.com
