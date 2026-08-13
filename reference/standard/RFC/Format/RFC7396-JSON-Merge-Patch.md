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
- 현재 상태·정오표·피드백 제공 방법: http://www.rfc-editor.org/info/rfc7396

### 저작권 고지

- Copyright (c) 2014 IETF Trust and the persons identified as the document authors. All rights reserved.
- 발행일 기준 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust 법적 조항(http://trustee.ietf.org/license-info) 적용 대상
- 문서에서 추출된 코드 구성요소: Trust Legal Provisions 섹션 4.e에 설명된 Simplified BSD License 텍스트 포함 필요, 보증 없이 제공

### 목차

```
1.  소개
2.  Merge Patch 문서 처리
3.  예제
4.  IANA 고려사항
5.  보안 고려사항
6.  참조
    6.1.  규범적 참조
    6.2.  참고 참조
부록 A.  예제 테스트 케이스
```

### 1. 소개

- JSON merge patch 문서 형식·처리 규칙·관련 MIME 미디어 타입 식별자 정의
- 주 용도: HTTP PATCH 메서드 [RFC5789]와 함께 대상 리소스 콘텐츠에 대한 일련의 수정 사항 기술
- JSON merge patch 문서: 수정되는 문서를 밀접하게 모방하는 구문으로 대상 JSON 문서에 적용할 변경 사항 기술
- 수신자는 제공된 패치 내용을 대상 문서의 현재 내용과 비교 → 요청되는 정확한 변경 사항 집합 결정
  - 패치에 대상 내 존재하지 않는 멤버 포함 → 해당 멤버 추가
  - 대상에 해당 멤버 이미 존재 → 값 대체
  - merge patch의 null 값 → 대상에서 기존 값 제거를 나타내는 특별한 의미

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

"a"의 값 변경 및 "f" 제거 → 다음 전송으로 달성 가능:

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

- 대상 리소스에 적용 시 → "a" 멤버 값이 "z"로 대체, "f" 제거, 나머지 콘텐츠는 그대로 유지
- 이 설계 의미: merge patch 문서는 주로 구조에 객체를 사용하고 명시적 null 값을 사용하지 않는 JSON 문서에 대한 수정 기술에 적합
- merge patch 형식은 모든 JSON 구문에 적합하지는 않음

### 2. Merge Patch 문서 처리

- JSON merge patch 문서: 대상 리소스에 적용할 일련의 변경 사항을 예시를 통해 기술
- 수신자 책임: merge patch를 대상 리소스의 현재 콘텐츠와 비교 → 대상에 적용할 구체적인 변경 작업 집합 결정
- merge patch 문서를 대상 리소스에 적용하려면 시스템이 의사 코드로 기술된 다음 함수의 효과를 실현
  - 함수 이름: MergePatch, 인수 2개 — 대상 리소스 문서·merge patch 문서
  - Target 인수: 임의의 JSON 값 또는 undefined 가능
  - Patch 인수: 임의의 JSON 값 가능

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

이 함수에 대한 주목할 점:

- 패치가 객체가 아닌 다른 것이면 → 결과는 항상 전체 대상을 전체 패치로 대체
- 객체가 아닌 대상의 일부 패치는 불가 (예: 배열의 일부 값만 교체하는 경우)
- MergePatch 작업은 텍스트 표현 수준이 아닌 데이터 항목 수준에서 동작하도록 정의
  - 공백·멤버 순서·대상 구현에서 사용 가능한 것 이상의 숫자 정밀도 등 텍스트 표현 수준 특성 보존 기대 없음
  - 대상 구현이 동일한 이름을 가진 여러 이름/값 쌍을 허용하더라도 → 그러한 객체에 대한 MergePatch 작업 결과는 정의되지 않음

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

- "application/merge-patch+json" 미디어 타입: 사용자 에이전트가 서버에게 대상 리소스에 적용할 구체적인 변경 작업 집합을 결정하도록 하려는 의도 표현 가능
- 따라서 주어진 변경의 적절성·그러한 변경을 요청하는 사용자 에이전트의 권한 결정은 서버 책임
- 그러한 결정 방식은 이 명세의 범위 밖
- [RFC5789] 섹션 5에서 논의된 모든 보안 고려사항은 "application/merge-patch+json" 미디어 타입을 사용하는 HTTP PATCH 메서드의 모든 사용에 적용

### 6. 참조

#### 6.1. 규범적 참조

- [RFC7159] Bray, T., "The JavaScript Object Notation (JSON) Data Interchange Format", RFC 7159, March 2014, <http://www.rfc-editor.org/info/rfc7159>.

#### 6.2. 참고 참조

- [RFC5789] Dusseault, L. and J. Snell, "PATCH Method for HTTP", RFC 5789, March 2010, <http://www.rfc-editor.org/info/rfc5789>.

### 부록 A. 예제 테스트 케이스

- ORIGINAL: `{"a":"b"}` · PATCH: `{"a":"c"}` · RESULT: `{"a":"c"}`
- ORIGINAL: `{"a":"b"}` · PATCH: `{"b":"c"}` · RESULT: `{"a":"b", "b":"c"}`
- ORIGINAL: `{"a":"b"}` · PATCH: `{"a":null}` · RESULT: `{}`
- ORIGINAL: `{"a":"b", "b":"c"}` · PATCH: `{"a":null}` · RESULT: `{"b":"c"}`
- ORIGINAL: `{"a":["b"]}` · PATCH: `{"a":"c"}` · RESULT: `{"a":"c"}`
- ORIGINAL: `{"a":"c"}` · PATCH: `{"a":["b"]}` · RESULT: `{"a":["b"]}`
- ORIGINAL: `{"a": {"b": "c"}}` · PATCH: `{"a": {"b": "d", "c": null}}` · RESULT: `{"a": {"b": "d"}}`
- ORIGINAL: `{"a": [{"b":"c"}]}` · PATCH: `{"a": [1]}` · RESULT: `{"a": [1]}`
- ORIGINAL: `["a","b"]` · PATCH: `["c","d"]` · RESULT: `["c","d"]`
- ORIGINAL: `{"a":"b"}` · PATCH: `["c"]` · RESULT: `["c"]`
- ORIGINAL: `{"a":"foo"}` · PATCH: `null` · RESULT: `null`
- ORIGINAL: `{"a":"foo"}` · PATCH: `"bar"` · RESULT: `"bar"`
- ORIGINAL: `{"e":null}` · PATCH: `{"a":1}` · RESULT: `{"e":null, "a":1}`
- ORIGINAL: `[1,2]` · PATCH: `{"a":"b", "c":null}` · RESULT: `{"a":"b"}`
- ORIGINAL: `{}` · PATCH: `{"a": {"bb": {"ccc": null}}}` · RESULT: `{"a": {"bb": {}}}`
