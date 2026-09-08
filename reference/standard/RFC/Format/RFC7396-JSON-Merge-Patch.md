# JSON Merge Patch

```
Internet Engineering Task Force (IETF)                        P. Hoffman
Request for Comments: 7396                                VPN Consortium
Obsoletes: 7386                                                J. Snell
Category: Standards Track                                   October 2014
ISSN: 2070-1721
```

## JSON Merge Patch

#### Abstract

- JSON merge patch 형식과 처리 규칙 정의
- 주 용도: HTTP PATCH 메서드와 함께 대상 리소스 콘텐츠에 대한 일련의 수정 사항 기술

### 이 메모의 상태

- Internet Standards Track 문서, IETF(Internet Engineering Task Force) 산출물
- IETF 커뮤니티 합의 반영, 공개 검토 완료 → IESG 발행 승인
- Internet Standards 관련 추가 정보: RFC 5741 섹션 2 참고
- 현재 상태, 정오표, 피드백 제공 방법: http://www.rfc-editor.org/info/rfc7396

### 저작권 고지

- Copyright (c) 2014 IETF Trust and the persons identified as the document authors. All rights reserved.
- 발행일 기준 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust 법적 조항(http://trustee.ietf.org/license-info) 적용 대상
- 문서에서 추출된 코드 구성요소: Trust Legal Provisions 섹션 4.e에 설명된 Simplified BSD License 텍스트 포함 필요, 보증 없이 제공

### 1. 소개

이 문서는 JSON merge patch의 형식, 처리 규칙, MIME 미디어 타입 식별자를 정의한다. 주로 HTTP PATCH 메서드 [RFC5789]와 함께 리소스의 수정 사항을 나타내는 데 사용한다.

merge patch는 수정할 JSON 문서와 비슷한 구조로 변경 사항을 표현한다. 수신자는 패치와 현재 문서를 비교해 수행할 작업을 결정한다. 대상에 없는 멤버는 추가하고 이미 있는 멤버는 값을 바꾸며, 패치의 null 값은 기존 멤버를 제거하라는 뜻으로 해석한다.

예를 들어, 다음과 같은 원본 JSON 문서가 주어졌을 때:

```json
{
  "a": "b",
  "c": {
    "d": "e",
    "f": "g"
  }
}
```

"a"의 값을 바꾸고 "f"를 제거하려면 다음 요청을 보낸다.

```
PATCH /target HTTP/1.1
Host: example.org
Content-Type: application/merge-patch+json

{
  "a":"z",
  "c": {
    "f": null
  }
}
```

패치를 적용하면 "a"의 값은 "z"로 바뀌고 "f"는 제거되며, 나머지 내용은 유지된다. null을 삭제 표시로 사용하므로, merge patch는 주로 객체로 구성되고 명시적인 null 값을 쓰지 않는 JSON 문서에 적합하다. 모든 JSON 구조의 수정에 적합한 형식은 아니다.

### 2. Merge Patch 문서 처리

수신자는 merge patch와 대상 리소스의 현재 내용을 비교해 구체적인 변경 작업을 결정한다. 처리 결과는 다음 의사 코드의 `MergePatch` 함수와 같아야 한다. 인수는 대상 문서 `Target`과 패치 문서 `Patch` 두 개다. `Target`은 임의의 JSON 값이나 undefined일 수 있고, `Patch`는 임의의 JSON 값일 수 있다.

```
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
```

패치가 객체가 아니면 대상 전체를 패치 값으로 교체한다. 따라서 배열의 일부 값처럼 객체가 아닌 대상의 일부분만 바꿀 수는 없다.

MergePatch는 데이터 항목을 기준으로 동작한다. 공백, 멤버 순서, 구현이 지원하는 범위를 넘는 숫자 정밀도 등 텍스트 표현의 특성은 보존하지 않는다. 구현이 이름이 중복된 멤버를 허용하더라도, 그런 객체에 MergePatch를 적용한 결과는 정의되어 있지 않다.

### 3. 예제

다음 예제 JSON 문서가 주어졌을 때:

```json
{
  "title": "Goodbye!",
  "author" : {
    "givenName" : "John",
    "familyName" : "Doe"
  },
  "tags":[ "example", "sample" ],
  "content": "This will be unchanged"
}
```

사용자 에이전트가 다음을 원할 경우:
- "title" 멤버 값을 "Goodbye!"에서 "Hello!"로 변경
- 새 "phoneNumber" 멤버 추가
- "author" 객체에서 "familyName" 멤버 제거
- "tags" 배열을 "sample"이라는 단어를 포함하지 않도록 교체

→ 다음 요청 전송:

```
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
```

결과 JSON 문서:

```json
{
  "title": "Hello!",
  "author" : {
    "givenName" : "John"
  },
  "tags": [ "example" ],
  "content": "This will be unchanged",
  "phoneNumber": "+01-123-456-7890"
}
```

### 4. IANA 고려사항

이 명세는 다음 추가 MIME 미디어 타입 등록:

- 타입 이름: application
- 서브타입 이름: merge-patch+json
- 필수 매개변수: 없음
- 선택적 매개변수: 없음
- 인코딩 고려사항: "application/merge-patch+json" 미디어 타입 사용 리소스는 "application/json" 미디어 타입 준수 필요 → [RFC7159] 섹션 8에 명시된 것과 동일한 인코딩 고려사항 적용 대상
- 보안 고려사항: 이 명세에 정의된 바와 같음
- 공개된 명세: 이 명세
- 이 미디어 타입을 사용하는 애플리케이션: 현재 알려진 것 없음
- 추가 정보
  - 매직 넘버: 해당 없음
  - 파일 확장자: 해당 없음
  - Macintosh 파일 타입 코드: TEXT
- 추가 정보를 위한 연락처 및 이메일 주소: IESG
- 의도된 용도: COMMON
- 사용 제한: 없음
- 저자: James M. Snell <jasnell@gmail.com>
- 변경 관리자: IESG

### 5. 보안 고려사항

"application/merge-patch+json"을 사용하면 사용자 에이전트는 변경 의도를 표현하고, 서버는 그에 필요한 구체적인 작업을 결정한다. 요청한 변경이 적절한지, 사용자 에이전트에게 권한이 있는지도 서버가 판단해야 한다. 판단 방법은 이 명세의 범위에 포함되지 않는다.

이 미디어 타입으로 HTTP PATCH를 사용할 때는 [RFC5789] 섹션 5의 보안 고려사항이 모두 적용된다.

### 6. 참조

#### 6.1. 규범적 참조

- [RFC7159] Bray, T., "The JavaScript Object Notation (JSON) Data Interchange Format", RFC 7159, March 2014, <http://www.rfc-editor.org/info/rfc7159>.

#### 6.2. 참고 참조

- [RFC5789] Dusseault, L. and J. Snell, "PATCH Method for HTTP", RFC 5789, March 2010, <http://www.rfc-editor.org/info/rfc5789>.

### 부록 A. 예제 테스트 케이스

| ORIGINAL | PATCH | RESULT |
| --- | --- | --- |
| `{"a":"b"}` | `{"a":"c"}` | `{"a":"c"}` |
| `{"a":"b"}` | `{"b":"c"}` | `{"a":"b", "b":"c"}` |
| `{"a":"b"}` | `{"a":null}` | `{}` |
| `{"a":"b", "b":"c"}` | `{"a":null}` | `{"b":"c"}` |
| `{"a":["b"]}` | `{"a":"c"}` | `{"a":"c"}` |
| `{"a":"c"}` | `{"a":["b"]}` | `{"a":["b"]}` |
| `{"a": {"b": "c"}}` | `{"a": {"b": "d", "c": null}}` | `{"a": {"b": "d"}}` |
| `{"a": [{"b":"c"}]}` | `{"a": [1]}` | `{"a": [1]}` |
| `["a","b"]` | `["c","d"]` | `["c","d"]` |
| `{"a":"b"}` | `["c"]` | `["c"]` |
| `{"a":"foo"}` | `null` | `null` |
| `{"a":"foo"}` | `"bar"` | `"bar"` |
| `{"e":null}` | `{"a":1}` | `{"e":null, "a":1}` |
| `[1,2]` | `{"a":"b", "c":null}` | `{"a":"b"}` |
| `{}` | `{"a": {"bb": {"ccc": null}}}` | `{"a": {"bb": {}}}` |
