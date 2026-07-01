Internet Engineering Task Force (IETF)                        P. Hoffman
Request for Comments: 7396                                VPN Consortium
Obsoletes: 7386                                                J. Snell
Category: Standards Track                                   October 2014
ISSN: 2070-1721


                            JSON Merge Patch

초록

   이 명세는 JSON merge patch 형식과 처리 규칙을 정의한다. merge patch
   형식은 주로 HTTP PATCH 메서드와 함께 대상 리소스의 콘텐츠에 대한
   일련의 수정 사항을 기술하는 수단으로 사용하기 위해 고안되었다.

이 메모의 상태

   이 문서는 Internet Standards Track 문서이다.

   이 문서는 Internet Engineering Task Force (IETF)의 산출물이다.
   이것은 IETF 커뮤니티의 합의를 대표한다. 이 문서는 공개 검토를
   받았으며 Internet Engineering Steering Group (IESG)에 의해 발행이
   승인되었다. Internet Standards에 대한 추가 정보는 RFC 5741의
   섹션 2에서 확인할 수 있다.

   이 문서의 현재 상태, 정오표, 그리고 이에 대한 피드백을 제공하는
   방법에 관한 정보는 http://www.rfc-editor.org/info/rfc7396 에서
   얻을 수 있다.

저작권 고지

   Copyright (c) 2014 IETF Trust 및 문서 저자로 식별된 사람들.
   모든 권리 보유.

   이 문서는 이 문서의 발행일에 유효한 BCP 78 및 IETF Trust의
   IETF 문서에 관한 법적 조항
   (http://trustee.ietf.org/license-info)의 적용을 받는다.
   이 문서들은 이 문서에 관한 귀하의 권리와 제한 사항을 기술하고
   있으므로 주의 깊게 검토하기 바란다. 이 문서에서 추출된
   코드 구성요소는 Trust Legal Provisions의 섹션 4.e에 기술된
   Simplified BSD License 텍스트를 포함해야 하며, Simplified BSD
   License에 기술된 대로 보증 없이 제공된다.

Hoffman & Snell              Standards Track                    [Page 1]

RFC 7396                    JSON Merge Patch                October 2014


목차

   1.  소개  . . . . . . . . . . . . . . . . . . . . . . . . . . . .   2
   2.  Merge Patch 문서 처리  . . . . . . . . . . . . . . . . . . .   3
   3.  예제  . . . . . . . . . . . . . . . . . . . . . . . . . . . .   4
   4.  IANA 고려사항  . . . . . . . . . . . . . . . . . . . . . . .   5
   5.  보안 고려사항  . . . . . . . . . . . . . . . . . . . . . . .   6
   6.  참조  . . . . . . . . . . . . . . . . . . . . . . . . . . . .   7
     6.1.  규범적 참조  . . . . . . . . . . . . . . . . . . . . . .   7
     6.2.  참고 참조  . . . . . . . . . . . . . . . . . . . . . . .   7
   부록 A.  예제 테스트 케이스  . . . . . . . . . . . . . . . . . .   8
   감사의 글  . . . . . . . . . . . . . . . . . . . . . . . . . . .   9
   저자 주소  . . . . . . . . . . . . . . . . . . . . . . . . . . .   9

1.  소개

   이 명세는 JSON merge patch 문서 형식, 처리 규칙, 및 관련 MIME 미디어
   타입 식별자를 정의한다. merge patch 형식은 주로 HTTP PATCH 메서드
   [RFC5789]와 함께 대상 리소스의 콘텐츠에 대한 일련의 수정 사항을
   기술하는 수단으로 사용하기 위해 고안되었다.

   JSON merge patch 문서는 수정되는 문서를 밀접하게 모방하는 구문을
   사용하여 대상 JSON 문서에 적용할 변경 사항을 기술한다. merge patch
   문서의 수신자는 제공된 패치의 내용을 대상 문서의 현재 내용과
   비교하여 요청되는 정확한 변경 사항 집합을 결정한다. 제공된
   merge patch에 대상 내에 존재하지 않는 멤버가 포함되어 있으면
   해당 멤버가 추가된다. 대상에 해당 멤버가 이미 포함되어 있으면
   값이 대체된다. merge patch의 null 값은 대상에서 기존 값의 제거를
   나타내는 특별한 의미가 부여된다.

   예를 들어, 다음과 같은 원본 JSON 문서가 주어졌을 때:

   {
     "a": "b",
     "c": {
       "d": "e",
       "f": "g"
     }
   }

Hoffman & Snell              Standards Track                    [Page 2]

RFC 7396                    JSON Merge Patch                October 2014


   "a"의 값을 변경하고 "f"를 제거하는 것은 다음을 전송하여 달성할
   수 있다:

   PATCH /target HTTP/1.1
   Host: example.org
   Content-Type: application/merge-patch+json

   {
     "a":"z",
     "c": {
       "f": null
     }
   }

   대상 리소스에 적용되면, "a" 멤버의 값은 "z"로 대체되고 "f"는
   제거되며, 나머지 콘텐츠는 그대로 유지된다.

   이 설계는 merge patch 문서가 주로 구조에 객체를 사용하고 명시적
   null 값을 사용하지 않는 JSON 문서에 대한 수정을 기술하는 데
   적합하다는 것을 의미한다. merge patch 형식은 모든 JSON 구문에
   적합한 것은 아니다.

2.  Merge Patch 문서 처리

   JSON merge patch 문서는 대상 리소스에 적용할 일련의 변경 사항을
   예시를 통해 기술한다. merge patch 문서의 수신자는 대상에 적용할
   구체적인 변경 작업 집합을 결정하기 위해 merge patch를 대상 리소스의
   현재 콘텐츠와 비교할 책임이 있다.

   merge patch 문서를 대상 리소스에 적용하기 위해, 시스템은 의사
   코드로 기술된 다음 함수의 효과를 실현한다. 이 설명에서 함수는
   MergePatch라 불리며, 두 개의 인수를 취한다: 대상 리소스 문서와
   merge patch 문서. Target 인수는 임의의 JSON 값이거나 undefined일
   수 있다. Patch 인수는 임의의 JSON 값일 수 있다.

Hoffman & Snell              Standards Track                    [Page 3]

RFC 7396                    JSON Merge Patch                October 2014


   define MergePatch(Target, Patch):
     if Patch is an Object:
       if Target is not an Object:
         Target = {} # Ignore the contents and set it to an empty Object
       for each Name/Value pair in Patch:
         if Value is null:
           if Name exists in Target:
             remove the Name/Value pair from Target
         else:
           Target[Name] = MergePatch(Target[Name], Value)
       return Target
     else:
       return Patch

   이 함수에 대해 몇 가지 주목할 점이 있다. 패치가 객체가 아닌 다른
   것이면, 결과는 항상 전체 대상을 전체 패치로 대체하는 것이 된다.
   또한, 객체가 아닌 대상의 일부를 패치하는 것은 불가능한데, 예를 들어
   배열의 일부 값만 교체하는 것과 같은 경우이다.

   MergePatch 작업은 텍스트 표현 수준이 아닌 데이터 항목 수준에서
   동작하도록 정의된다. MergePatch 작업이 공백, 멤버 순서, 대상
   구현에서 사용 가능한 것 이상의 숫자 정밀도 등과 같은 텍스트 표현
   수준의 특성을 보존할 것이라는 기대는 없다. 또한, 대상 구현이
   동일한 이름을 가진 여러 이름/값 쌍을 허용하더라도, 그러한 객체에
   대한 MergePatch 작업의 결과는 정의되지 않는다.

3.  예제

   다음 예제 JSON 문서가 주어졌을 때:

   {
     "title": "Goodbye!",
     "author" : {
       "givenName" : "John",
       "familyName" : "Doe"
     },
     "tags":[ "example", "sample" ],
     "content": "This will be unchanged"
   }

Hoffman & Snell              Standards Track                    [Page 4]

RFC 7396                    JSON Merge Patch                October 2014


   사용자 에이전트가 "title" 멤버의 값을 "Goodbye!"에서 "Hello!"로
   변경하고, 새로운 "phoneNumber" 멤버를 추가하고, "author" 객체에서
   "familyName" 멤버를 제거하고, "tags" 배열을 "sample"이라는 단어를
   포함하지 않도록 교체하려면 다음 요청을 전송할 것이다:

   PATCH /my/resource HTTP/1.1
   Host: example.org
   Content-Type: application/merge-patch+json

   {
     "title": "Hello!",
     "phoneNumber": "+01-123-456-7890",
     "author": {
       "familyName": null
     },
     "tags": [ "example" ]
   }

   결과 JSON 문서는 다음과 같을 것이다:

   {
     "title": "Hello!",
     "author" : {
       "givenName" : "John"
     },
     "tags": [ "example" ],
     "content": "This will be unchanged",
     "phoneNumber": "+01-123-456-7890"
   }

4.  IANA 고려사항

   이 명세는 다음의 추가 MIME 미디어 타입을 등록한다:

      타입 이름: application

      서브타입 이름: merge-patch+json

      필수 매개변수: 없음

      선택적 매개변수: 없음

      인코딩 고려사항: "application/merge-patch+json" 미디어 타입을
      사용하는 리소스는 "application/json" 미디어 타입을 준수해야
      하며 따라서 [RFC7159]의 섹션 8에 명시된 것과 동일한 인코딩
      고려사항의 적용을 받는다.

Hoffman & Snell              Standards Track                    [Page 5]

RFC 7396                    JSON Merge Patch                October 2014


      보안 고려사항: 이 명세에 정의된 바와 같음

      공개된 명세: 이 명세.

      이 미디어 타입을 사용하는 애플리케이션: 현재 알려진 것 없음.

      추가 정보:

         매직 넘버: 해당 없음

         파일 확장자: 해당 없음

         Macintosh 파일 타입 코드: TEXT

      추가 정보를 위한 연락처 및 이메일 주소: IESG

      의도된 용도: COMMON

      사용 제한: 없음

      저자: James M. Snell <jasnell@gmail.com>

      변경 관리자: IESG

5.  보안 고려사항

   "application/merge-patch+json" 미디어 타입은 사용자 에이전트가
   서버에게 대상 리소스에 적용할 구체적인 변경 작업 집합을 결정하도록
   하려는 의도를 나타낼 수 있게 한다. 따라서 주어진 변경의 적절성과
   그러한 변경을 요청하는 사용자 에이전트의 권한을 결정하는 것은
   서버의 책임이다. 그러한 결정이 어떻게 이루어지는지는 이 명세의
   범위 밖으로 간주된다.

   [RFC5789]의 섹션 5에서 논의된 모든 보안 고려사항은
   "application/merge-patch+json" 미디어 타입을 사용하는 HTTP PATCH
   메서드의 모든 사용에 적용된다.

Hoffman & Snell              Standards Track                    [Page 6]

RFC 7396                    JSON Merge Patch                October 2014


6.  참조

6.1.  규범적 참조

   [RFC7159]  Bray, T., "The JavaScript Object Notation (JSON) Data
              Interchange Format", RFC 7159, March 2014,
              <http://www.rfc-editor.org/info/rfc7159>.

6.2.  참고 참조

   [RFC5789]  Dusseault, L. and J. Snell, "PATCH Method for HTTP", RFC
              5789, March 2010,
              <http://www.rfc-editor.org/info/rfc5789>.

Hoffman & Snell              Standards Track                    [Page 7]

RFC 7396                    JSON Merge Patch                October 2014


부록 A.  예제 테스트 케이스

   ORIGINAL        PATCH            RESULT
   ------------------------------------------
   {"a":"b"}       {"a":"c"}       {"a":"c"}

   {"a":"b"}       {"b":"c"}       {"a":"b",
                                    "b":"c"}

   {"a":"b"}       {"a":null}      {}

   {"a":"b",       {"a":null}      {"b":"c"}
    "b":"c"}

   {"a":["b"]}     {"a":"c"}       {"a":"c"}

   {"a":"c"}       {"a":["b"]}     {"a":["b"]}

   {"a": {         {"a": {         {"a": {
     "b": "c"}       "b": "d",       "b": "d"
   }                 "c": null}      }
                   }               }

   {"a": [         {"a": [1]}      {"a": [1]}
     {"b":"c"}
    ]
   }

   ["a","b"]       ["c","d"]       ["c","d"]

   {"a":"b"}       ["c"]           ["c"]

   {"a":"foo"}     null            null

   {"a":"foo"}     "bar"           "bar"

   {"e":null}      {"a":1}         {"e":null,
                                    "a":1}

   [1,2]           {"a":"b",       {"a":"b"}
                    "c":null}

   {}              {"a":            {"a":
                    {"bb":           {"bb":
                     {"ccc":          {}}}
                      null}}}

Hoffman & Snell              Standards Track                    [Page 8]

RFC 7396                    JSON Merge Patch                October 2014


감사의 글

   많은 사람들이 이 문서에 중요한 아이디어를 기여하였다. 이 사람들에는
   James Manger, Matt Miller, Carsten Bormann, Bjoern Hoehrmann,
   Pete Resnick, Richard Barnes가 포함되지만 이에 국한되지 않는다.

저자 주소

   Paul Hoffman
   VPN Consortium

   EMail: paul.hoffman@vpnc.org


   James M. Snell

   EMail: jasnell@gmail.com

Hoffman & Snell              Standards Track                    [Page 9]

RFC 7396                    JSON Merge Patch                October 2014
