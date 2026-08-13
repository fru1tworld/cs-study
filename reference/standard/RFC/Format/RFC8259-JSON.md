# RFC 8259 - JavaScript Object Notation (JSON) 데이터 교환 형식

```
Internet Engineering Task Force (IETF)                      T. Bray, Ed.
Request for Comments: 8259                                    Textuality
Obsoletes: 7159                                            December 2017
Category: Standards Track
ISSN: 2070-1721
```

### Abstract

- JavaScript Object Notation (JSON): 경량 텍스트 기반·언어 독립적 데이터 교환 형식
- ECMAScript 프로그래밍 언어 표준에서 파생
- 구조화된 데이터의 이식 가능한 표현을 위한 소규모 서식 규칙 집합 정의
- 이 문서의 목표: JSON의 다른 명세들과의 불일치 제거 + 명세 오류 수정 + 경험 기반 상호운용성 지침 제공

## 이 메모의 상태

- Internet Standards Track 문서
- Internet Engineering Task Force (IETF)의 산출물, IETF 커뮤니티의 합의를 대표
- 공개 검토를 거쳐 Internet Engineering Steering Group (IESG)이 출판 승인
- Internet Standards에 대한 추가 정보 → RFC 7841 섹션 2 참고

이 문서의 현재 상태, 정오표, 피드백 제공 방법: https://www.rfc-editor.org/info/rfc8259

## 저작권 고지

- Copyright (c) 2017 IETF Trust 및 문서 저자로 식별된 사람들. 모든 권리 보유.
- 이 문서는 발행일 기준 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust의 법적 조항(https://trustee.ietf.org/license-info) 적용 대상 → 권리와 제한 사항 확인을 위해 검토 필요
- 이 문서에서 추출된 코드 구성요소는 Trust Legal Provisions 섹션 4.e에 설명된 대로 Simplified BSD License 텍스트 포함 필요, Simplified BSD License에 설명된 대로 보증 없이 제공
- 2008년 11월 10일 이전에 출판되거나 공개적으로 제공된 IETF 문서·기여물의 자료 포함 가능
  - 해당 자료의 저작권 관리자가 IETF Standards Process 외부에서의 수정을 IETF Trust에 허용하지 않았을 수 있음
  - 적절한 라이선스 취득 없이는 IETF Standards Process 외부에서 이 문서를 수정 불가
  - 파생 저작물 작성도 IETF Standards Process 외부에서는 RFC 출판용 서식 변환·비영어 번역을 제외하고 불가

## 목차

```
1.  소개
  1.1.  이 문서에서 사용하는 규약
  1.2.  JSON의 명세들
  1.3.  이 개정판에 대한 소개
2.  JSON 문법
3.  값
4.  객체
5.  배열
6.  숫자
7.  문자열
8.  문자열 및 문자 관련 사항
  8.1.  문자 인코딩
  8.2.  유니코드 문자
  8.3.  문자열 비교
9.  파서
10. 생성기
11. IANA 고려사항
12. 보안 고려사항
13. 예제
14. 참고문헌
  14.1.  규범적 참고문헌
  14.2.  참고적 참고문헌
부록 A.  RFC 7159로부터의 변경사항
기여자
저자 주소
```

## 1. 소개

- JavaScript Object Notation (JSON): 구조화된 데이터의 직렬화를 위한 텍스트 형식
- ECMAScript Programming Language Standard, Third Edition [ECMA-262]에 정의된 JavaScript의 객체 리터럴에서 파생
- 표현 가능 타입
  - 원시 타입 4종: 문자열·숫자·불리언·null
  - 구조화된 타입 2종: 객체·배열
- 문자열: 0개 이상의 유니코드 문자 [UNICODE]의 시퀀스
  - 이 인용은 특정 릴리스가 아닌 유니코드의 최신 버전 참조
  - 유니코드 명세의 향후 변경이 JSON 구문에 영향을 미칠 것으로 예상되지 않음
- 객체: 0개 이상의 이름/값 쌍의 순서 없는 컬렉션
  - 이름은 문자열, 값은 문자열·숫자·불리언·null·객체·배열 중 하나
- 배열: 0개 이상의 값의 순서 있는 시퀀스
- "object"와 "array" 용어는 JavaScript의 관례에서 유래
- JSON의 설계 목표: 최소성·이식성·텍스트 기반이면서 JavaScript의 하위 집합이 되는 것

### 1.1. 이 문서에서 사용하는 규약

- "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL" 키워드는 모두 대문자로 나타날 때만 BCP 14 [RFC2119] [RFC8174]에 설명된 대로 해석
- 이 문서의 문법 규칙은 [RFC5234]에 설명된 대로 해석

### 1.2. JSON의 명세들

- 이 문서는 [RFC7159]를 대체
  - [RFC7159]는 원래 JSON을 설명하고 미디어 타입 "application/json"을 등록한 [RFC4627]을 폐기(obsolete)
- JSON은 [ECMA-404]에도 설명됨
  - ECMA-404 참조는 규범적(normative) → 구현자가 반드시 참조해야 한다는 의미가 아니라, "JSON text" 용어 정의에 명세 간 불일치가 없음을 강조하기 위함
  - 다만 ECMA-404는 이 명세가 최대한의 상호운용성을 위해 피할 것을 권장하는 몇 가지 관행을 허용
- 의도: 두 문서 사이의 문법은 동일(설명 방식은 서로 다를 수 있음)
  - 차이 발견 시 → ECMA와 IETF가 두 문서 모두 업데이트하도록 협력
  - 한쪽 문서에서 오류 발견 시 → 다른 쪽 문서도 유사 오류 검토 필요, 있으면 가능한 한 수정
  - 향후 한쪽 문서 변경 시 → ECMA와 IETF가 두 문서의 정렬 상태 유지를 위해 협력

### 1.3. 이 개정판에 대한 소개

- RFC 4627 발행 이후 JSON이 매우 폭넓게 사용됨 → 명세상 허용되지만 상호운용성 문제를 야기하는 특정 패턴들이 드러남
- RFC 4627 정오표: RFC Errata IDs 607 [Err607], 3607 [Err3607]
- RFC 7159 정오표: RFC Errata IDs 3915 [Err3915], 4264 [Err4264], 4336 [Err4336], 4388 [Err4388]
- 이 문서의 목표: 정오표 적용 + JSON의 다른 명세들과의 불일치 제거 + 상호운용성 문제를 초래할 수 있는 관행 강조

## 2. JSON 문법

- JSON 텍스트: 토큰의 시퀀스
  - 토큰 집합: 구조 문자 6개·문자열·숫자·리터럴 이름 3개
- JSON 텍스트는 직렬화된 값
  - JSON의 이전 명세 중 일부는 JSON 텍스트를 객체 또는 배열로 제한
  - JSON 텍스트가 요구되는 곳에서 객체·배열만 생성하는 구현체 → 모든 구현체가 이를 적합한 JSON 텍스트로 받아들인다는 의미에서 상호운용 가능

```abnf
JSON-text = ws value ws
```

구조 문자 6개:

```abnf
begin-array     = ws %x5B ws  ; [ 왼쪽 대괄호
begin-object    = ws %x7B ws  ; { 왼쪽 중괄호
end-array       = ws %x5D ws  ; ] 오른쪽 대괄호
end-object      = ws %x7D ws  ; } 오른쪽 중괄호
name-separator  = ws %x3A ws  ; : 콜론
value-separator = ws %x2C ws  ; , 쉼표
```

- 구조 문자 6개 앞뒤에 의미 없는 공백 허용

```abnf
ws = *(
        %x20 /              ; Space
        %x09 /              ; Horizontal tab
        %x0A /              ; Line feed or New line
        %x0D )              ; Carriage return
```

## 3. 값

- JSON 값은 다음 중 하나여야 함(MUST): 객체·배열·숫자·문자열·리터럴 이름 3종(false, null, true)
- 리터럴 이름은 반드시 소문자여야 함(MUST) → 다른 리터럴 이름 불허

```abnf
value = false / null / true / object / array / number / string

false = %x66.61.6c.73.65   ; false
null  = %x6e.75.6c.6c      ; null
true  = %x74.72.75.65      ; true
```

## 4. 객체

- 객체 구조: 0개 이상의 이름/값 쌍(멤버)을 둘러싸는 한 쌍의 중괄호로 표현
  - 이름은 문자열
  - 각 이름 뒤에 단일 콜론 → 이름과 값 구분
  - 단일 쉼표가 값과 뒤따르는 이름을 구분
  - 객체 내의 이름은 고유해야 함(SHOULD)

```abnf
object = begin-object [ member *( value-separator member ) ]
         end-object

member = string name-separator value
```

- 이름이 모두 고유한 객체 → 해당 객체를 수신하는 모든 소프트웨어 구현체가 이름-값 매핑에 동의한다는 의미에서 상호운용 가능
- 객체 내의 이름이 고유하지 않은 경우 → 수신 소프트웨어 동작 예측 불가
  - 많은 구현체: 마지막 이름/값 쌍만 보고
  - 일부 구현체: 오류 보고 또는 파싱 실패
  - 일부 구현체: 중복 포함 모든 이름/값 쌍 보고
- JSON 파싱 라이브러리들은 객체 멤버 순서를 호출 소프트웨어에 표시하는지 여부에서 차이 관찰됨
  - 멤버 순서에 동작이 의존하지 않는 구현체 → 이러한 차이에 영향받지 않는다는 의미에서 상호운용 가능

## 5. 배열

- 배열 구조: 0개 이상의 값(요소)을 둘러싸는 대괄호로 표현, 요소들은 쉼표로 구분

```abnf
array = begin-array [ value *( value-separator value ) ] end-array
```

- 배열 내의 값이 동일한 타입이어야 한다는 요구사항 없음

## 6. 숫자

- 숫자 표현은 대부분의 프로그래밍 언어에서 사용되는 것과 유사
  - 10진 숫자를 사용한 10진수 표현
  - 선택적 마이너스 부호 + 정수 부분, 소수 부분·지수 부분이 뒤따를 수 있음
  - 선행 0 불허
- 소수 부분: 소수점 뒤에 하나 이상의 숫자
- 지수 부분: 대문자·소문자 E로 시작 → 뒤에 더하기·빼기 부호 가능 → E와 선택적 부호 뒤에 하나 이상의 숫자
- 아래 문법으로 표현할 수 없는 숫자 값(예: Infinity, NaN) 불허

```abnf
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
```

- 이 명세는 구현체가 수용하는 숫자의 범위·정밀도에 제한 설정 허용
- IEEE 754 binary64 (배정밀도) 숫자 [IEEE754] 구현 소프트웨어가 일반적으로 사용 가능하고 널리 사용됨 → 구현체가 이것이 제공하는 것 이상의 정밀도·범위를 기대하지 않는다면 좋은 상호운용성 달성 가능
  - 이는 구현체가 예상되는 정밀도 내에서 JSON 숫자를 근사화한다는 의미
  - 1E400, 3.141592653589793238462643383279 같은 JSON 숫자는 잠재적 상호운용성 문제를 시사 → 해당 숫자를 만든 소프트웨어가 수신 소프트웨어에 널리 이용 가능한 것보다 더 큰 숫자 크기·정밀도 역량을 기대함을 의미
- 정수이면서 [-(2^53)+1, (2^53)-1] 범위에 있는 숫자 → 구현체가 그 값에 대해 정확히 동의한다는 의미에서 상호운용 가능

## 7. 문자열

- 문자열 표현은 C 계열 프로그래밍 언어의 관례와 유사
  - 따옴표로 시작·종료
  - 반드시 이스케이프해야 하는(MUST) 문자: 따옴표·역슬래시·제어 문자(U+0000~U+001F)
  - 그 외 모든 유니코드 문자는 따옴표 안에 놓일 수 있음
- 모든 문자는 이스케이프 가능
  - 기본 다국어 평면(U+0000~U+FFFF)에 있는 문자 → 6문자 시퀀스로 표현 가능: 역슬래시 + 소문자 u + 코드 포인트를 인코딩하는 4자리 16진수
    - 16진수 문자 A~F는 대문자·소문자 모두 가능
    - 예: 단일 역슬래시 문자만 포함하는 문자열 → `"\u005C"`로 표현 가능
  - 대안: 자주 사용되는 일부 문자에 대한 2문자 시퀀스 이스케이프 표현
    - 예: 단일 역슬래시 문자만 포함하는 문자열 → 더 간결하게 `"\\"`로 표현 가능
  - 기본 다국어 평면에 없는 확장 문자 → UTF-16 서로게이트 쌍을 인코딩하는 12문자 시퀀스로 표현
    - 예: G 음자리표 문자(U+1D11E)만 포함하는 문자열 → `"\uD834\uDD1E"`로 표현 가능

```abnf
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
```

## 8. 문자열 및 문자 관련 사항

### 8.1. 문자 인코딩

- 폐쇄된 에코시스템의 일부가 아닌 시스템 간에 교환되는 JSON 텍스트 → UTF-8 [RFC3629] 인코딩 필수(MUST)
- JSON의 이전 명세들은 JSON 텍스트 전송 시 UTF-8 사용을 요구하지 않았음
  - 다만 JSON 기반 소프트웨어 구현의 대다수가 UTF-8 인코딩을 선택 → 상호운용성 달성의 유일한 인코딩이 될 정도
- 구현체는 네트워크로 전송되는 JSON 텍스트 시작에 바이트 순서 표시(U+FEFF)를 추가해서는 안 됨(MUST NOT)
  - 상호운용성을 위해, 파싱하는 구현체는 바이트 순서 표시의 존재를 오류 처리하지 않고 무시 가능(MAY)

### 8.2. 유니코드 문자

- JSON 텍스트에 표현된 모든 문자열이(이스케이프 여부 무관) 전적으로 유니코드 문자 [UNICODE]로 구성 → 파싱하는 모든 소프트웨어 구현체가 객체·배열 내 이름과 문자열 값의 내용에 동의한다는 의미에서 상호운용 가능
- 그러나 이 명세의 ABNF는 멤버 이름·문자열 값에 유니코드 문자로 인코딩할 수 없는 비트 시퀀스 포함을 허용
  - 예: "\uDEAD" (짝이 없는 단일 UTF-16 서로게이트)
  - 관찰 사례: 라이브러리가 절단 지점이 서로게이트 쌍을 분리하는지 확인하지 않고 UTF-16 문자열을 절단
  - 이러한 값을 포함하는 JSON 텍스트를 수신하는 소프트웨어의 동작은 예측 불가
    - 예: 구현체가 문자열 값의 길이에 대해 서로 다른 값을 반환하거나 치명적인 런타임 예외 발생 가능

### 8.3. 문자열 비교

- 소프트웨어 구현체는 일반적으로 객체 멤버 이름에 대한 동등성 테스트 수행 필요
- 텍스트 표현을 유니코드 코드 유닛 시퀀스로 변환 후 코드 유닛별로 수치 비교하는 구현체 → 두 문자열의 동등·비동등에 대해 모든 경우에 동의한다는 의미에서 상호운용 가능
- 예: 이스케이프된 문자를 변환하지 않고 문자열을 비교하는 구현체 → `"a\\b"`와 `"a\u005Cb"`가 같지 않다고 잘못 판단할 수 있음

## 9. 파서

- JSON 파서: JSON 텍스트를 다른 표현으로 변환
- JSON 문법에 적합한 모든 텍스트를 수용해야 함(MUST)
- JSON이 아닌 형식·확장을 수용할 수 있음(MAY)
- 구현체가 설정 가능한 제한
  - 수용하는 텍스트의 크기
  - 최대 중첩 깊이
  - 숫자의 범위와 정밀도
  - 문자열의 길이와 문자 내용

## 10. 생성기

- JSON 생성기: JSON 텍스트를 생산
- 생성된 텍스트는 반드시 JSON 문법에 엄격히 적합해야 함(MUST)

## 11. IANA 고려사항

- JSON 텍스트의 미디어 타입: application/json
- 등록 정보
  - Type name: application
  - Subtype name: json
  - Required parameters: n/a
  - Optional parameters: n/a
  - Encoding considerations: binary
  - Security considerations: RFC 8259, 섹션 12 참조
  - Interoperability considerations: RFC 8259에 설명됨
  - Published specification: RFC 8259
  - Applications that use this media type: JSON은 ActionScript·C·C#·Clojure·ColdFusion·Common Lisp·E·Erlang·Go·Java·JavaScript·Lua·Objective CAML·Perl·PHP·Python·Rebol·Ruby·Scala·Scheme 등 다양한 프로그래밍 언어로 작성된 애플리케이션 간 데이터 교환에 사용됨
  - Additional information
    - Magic number(s): n/a
    - File extension(s): .json
    - Macintosh file type code(s): TEXT
  - Person & email address to contact for further information: IESG, iesg@ietf.org
  - Intended usage: COMMON
  - Restrictions on usage: none
  - Author: Douglas Crockford, douglas@crockford.com
  - Change controller: IESG, iesg@ietf.org
  - Note: 이 등록에 대해 "charset" 파라미터는 정의되지 않음 → 파라미터 추가해도 적합한 수신자에게는 실질적인 효과 없음

## 12. 보안 고려사항

- 일반적으로 스크립팅 언어에는 보안 문제 존재
- JSON은 JavaScript의 하위 집합이지만 대입과 호출은 제외
- JSON 구문이 JavaScript에서 차용됨 → 해당 언어의 "eval()" 함수로 대부분의 JSON 텍스트 파싱 가능(전부는 아님; U+2028 LINE SEPARATOR, U+2029 PARAGRAPH SEPARATOR는 JSON에서는 합법적이지만 JavaScript에서는 그렇지 않음)
  - 텍스트에 데이터 선언과 함께 실행 가능한 코드가 포함될 수 있음 → 일반적으로 허용할 수 없는 보안 위험
  - JSON 텍스트가 해당 언어의 구문에 적합한 다른 프로그래밍 언어에서 eval()과 유사한 함수를 사용하는 경우에도 동일 고려사항 적용

## 13. 예제

다음은 JSON 객체:

```json
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
```

- 이 객체의 Image 멤버: Thumbnail 멤버가 객체이고 IDs 멤버가 숫자 배열인 객체

다음은 두 개의 객체를 포함하는 JSON 배열:

```json
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
```

다음은 값만 포함하는 세 개의 작은 JSON 텍스트:

```json
"Hello world!"
```

```json
42
```

```json
true
```

## 14. 참고문헌

### 14.1. 규범적 참고문헌

```
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
```

### 14.2. 참고적 참고문헌

```
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
```

## 부록 A. RFC 7159로부터의 변경사항

이 섹션은 이 문서와 RFC 7159의 텍스트 사이의 변경사항 나열:

- 섹션 1.2: ECMA-262에서 JSON 명세 제거 반영, ECMA-404를 규범적 참고문헌으로 전환, "규범적(normative)"의 특정한 의미 설명하도록 업데이트
- 섹션 1.3: RFC 4627이 아닌 RFC 7159에 대해 제출된 정오표를 반영하도록 업데이트
- 섹션 8.1: 네트워크 전송 시 UTF-8 사용을 요구하도록 변경
- 섹션 12: ECMAScript "eval()" 함수 사용으로 인한 보안 위험 설명의 정확도를 높이도록 업데이트
- 섹션 14.1: ECMA-404를 규범적 참고문헌으로 포함하도록 업데이트
- 섹션 14.2: ECMA-404 제거, ECMA-262의 버전 업데이트, 정오표 목록 갱신

## 기여자

- RFC 4627은 Douglas Crockford가 작성
- 이 문서는 해당 문서에 비교적 적은 수의 변경을 가하여 구성 → 텍스트 대부분은 그의 것

## 저자 주소

```
Tim Bray (editor)
Textuality

Email: tbray@textuality.com
```
