# RFC 6902 - JavaScript Object Notation (JSON) Patch

```
Internet Engineering Task Force (IETF)                     P. Bryan, Ed.
Request for Comments: 6902                                Salesforce.com
Category: Standards Track                             M. Nottingham, Ed.
ISSN: 2070-1721                                                   Akamai
                                                              April 2013
```

### Abstract

- JSON Patch: JavaScript Object Notation(JSON) 문서에 적용할 연산의 시퀀스를 표현하는 JSON 문서 구조
- HTTP PATCH 메서드와 함께 사용하기 적합
- "application/json-patch+json" 미디어 타입으로 패치 문서 식별

## 이 메모의 상태

- 인터넷 표준 트랙 문서
- IETF(인터넷 엔지니어링 태스크 포스)의 산출물 → IETF 커뮤니티의 합의 반영
- 공개 검토를 거쳐 IESG(인터넷 엔지니어링 운영 그룹)의 발행 승인 완료
- 인터넷 표준 관련 추가 정보 → RFC 5741 섹션 2 참고

이 문서의 현재 상태, 정오표, 피드백 제공 방법: http://www.rfc-editor.org/info/rfc6902

## 저작권 고지

- Copyright (c) 2013 IETF Trust 및 문서 저자로 식별된 자. 모든 권리 보유
- 발행일에 유효한 BCP 78 및 IETF 문서에 관한 IETF Trust의 법적 조항(http://trustee.ietf.org/license-info) 적용 대상 → 권리·제한사항 확인을 위해 검토 필요
- 이 문서에서 추출된 코드 구성요소 → Trust 법적 조항 섹션 4.e에 기술된 간소화된 BSD 라이선스 텍스트 포함 필요, 보증 없이 제공

## 목차

```
1.  소개
2.  규약
3.  문서 구조
4.  연산
  4.1.  add
  4.2.  remove
  4.3.  replace
  4.4.  move
  4.5.  copy
  4.6.  test
5.  오류 처리
6.  IANA 고려사항
7.  보안 고려사항
8.  감사의 글
9.  참고 문헌
  9.1.  규범적 참고 문헌
  9.2.  참고적 참고 문헌
부록 A.  예제
  A.1.  객체 멤버 추가
  A.2.  배열 요소 추가
  A.3.  객체 멤버 제거
  A.4.  배열 요소 제거
  A.5.  값 교체
  A.6.  값 이동
  A.7.  배열 요소 이동
  A.8.  값 테스트: 성공
  A.9.  값 테스트: 오류
  A.10. 중첩된 멤버 객체 추가
  A.11. 인식되지 않는 요소 무시
  A.12. 존재하지 않는 대상에 추가
  A.13. 유효하지 않은 JSON Patch 문서
  A.14. ~ 이스케이프 순서
  A.15. 문자열과 숫자 비교
  A.16. 배열 값 추가
```

## 1. 소개

- JSON [RFC4627]: 구조화된 데이터의 교환·저장을 위한 일반적인 형식
- HTTP PATCH [RFC5789]: HTTP [RFC2616]를 리소스에 대한 부분적 수정 메서드로 확장
- JSON Patch: 대상 JSON 문서에 적용할 연산의 시퀀스를 표현하는 형식(미디어 타입 "application/json-patch+json"으로 식별) → HTTP PATCH 메서드와 함께 사용하기 적합
- 활용 범위: JSON 문서, 또는 JSON 문법으로 객체·배열 직렬화가 가능한 유사 제약 조건의 데이터 구조에 대한 부분적 업데이트 전반

## 2. 규약

- "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL" → RFC 2119 [RFC2119]에 기술된 대로 해석

## 3. 문서 구조

- JSON Patch 문서: 객체의 배열을 나타내는 JSON [RFC4627] 문서
- 각 객체 = 대상 JSON 문서에 적용할 단일 연산

HTTP PATCH 요청에서 전송되는 JSON Patch 문서 예시:

```
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
```

- JSON Patch 문서의 평가 → 대상 JSON 문서에 대해 시작
- 연산 → 배열에 나타나는 순서대로 순차적으로 적용
- 시퀀스의 각 연산 → 대상 문서에 적용 → 결과 문서가 다음 연산의 대상이 됨
- 평가 종료 조건: 모든 연산 성공 적용 또는 오류 조건 발생

## 4. 연산

- 연산 객체 → 수행할 연산을 나타내는 값을 가진 "op" 멤버 정확히 하나 필수(MUST)
  - 값: "add"·"remove"·"replace"·"move"·"copy"·"test" 중 하나 필수(MUST) — 다른 값은 오류
  - 각 연산의 의미는 아래 하위 섹션에 정의
- 연산 객체 → "path" 멤버 정확히 하나 필수(MUST)
  - 값: 연산이 수행되는 대상 문서 내 위치("대상 위치")를 참조하는 JSON-Pointer 값 [RFC6901]을 포함하는 문자열
- 그 외 연산 객체 멤버의 의미 → 연산별로 정의(아래 하위 섹션 참조)
  - 해당 연산에 명시적으로 정의되지 않은 멤버 → 무시 필수(MUST), 즉 정의되지 않은 멤버가 없는 것처럼 연산 완료
- JSON 객체에서 멤버 순서는 무의미 → 다음 연산 객체들은 동등:

```
{ "op": "add", "path": "/a/b/c", "value": "foo" }
{ "path": "/a/b/c", "op": "add", "value": "foo" }
{ "value": "foo", "path": "/a/b/c", "op": "add" }
```

- 연산 적용 대상: JSON 문서로 표현되는 데이터 구조 → 모든 이스케이프 해제([RFC4627] 섹션 2.5 참조) 완료 후 적용

### 4.1. add

- "add" 연산 → 대상 위치가 무엇을 참조하는지에 따라 다음 중 하나 수행:
  - 대상 위치가 배열 인덱스 지정 → 지정된 인덱스에 배열 새 값 삽입
  - 대상 위치가 아직 존재하지 않는 객체 멤버 지정 → 객체에 새 멤버 추가
  - 대상 위치가 이미 존재하는 객체 멤버 지정 → 해당 멤버 값 교체
- 연산 객체 → 추가할 값을 지정하는 "value" 멤버 포함 필수(MUST)

예시:

```
{ "op": "add", "path": "/a/b/c", "value": [ "foo", "bar" ] }
```

- 연산 적용 시 대상 위치가 참조해야 하는 대상(MUST) 중 하나:
  - 대상 문서의 루트 → 지정된 값이 대상 문서의 전체 내용이 됨
  - 기존 객체에 추가할 멤버 → 제공된 값이 해당 객체의 표시된 위치에 추가, 멤버가 이미 존재하면 지정된 값으로 교체
  - 기존 배열에 추가할 요소 → 제공된 값이 배열의 표시된 위치에 추가, 지정된 인덱스 이상의 모든 요소는 오른쪽으로 한 위치 이동
    - 지정된 인덱스는 배열의 요소 수보다 클 수 없음(MUST NOT)
    - "-" 문자로 배열의 끝을 인덱싱([RFC6901] 참조) → 배열에 값을 추가(append)하는 효과
- 이 연산은 기존 객체·배열에 추가하도록 설계 → 대상 위치가 종종 존재하지 않음
  - 포인터의 오류 처리 알고리즘이 호출되지만, 이 명세는 "add" 포인터에 대한 오류 처리 동작을 해당 오류를 무시하고 지정된 대로 값을 추가하도록 정의
- 단, 객체 자체 또는 이를 포함하는 배열은 존재 필수 → 존재하지 않으면 오류

예를 들어 다음 문서로 시작하여 대상 위치가 "/a/b"인 "add":

```
{ "a": { "foo": 1 } }
```

- 오류 아님 → "a"가 존재 → "b"가 그 값에 추가됨

다음 문서에서는 오류:

```
{ "q": { "bar": 2 } }
```

- 오류 이유: "a"가 존재하지 않음

### 4.2. remove

- "remove" 연산: 대상 위치의 값 제거
- 연산 성공 조건: 대상 위치 존재 필수(MUST)

예시:

```
{ "op": "remove", "path": "/a/b/c" }
```

- 배열에서 요소 제거 시 → 지정된 인덱스 위의 모든 요소는 왼쪽으로 한 위치 이동

### 4.3. replace

- "replace" 연산: 대상 위치의 값을 새 값으로 교체
- 연산 객체 → 교체 값을 지정하는 "value" 멤버 포함 필수(MUST)
- 연산 성공 조건: 대상 위치 존재 필수(MUST)

예시:

```
{ "op": "replace", "path": "/a/b/c", "value": 42 }
```

- 기능적으로 동일: 값에 대한 "remove" 연산 후 즉시 동일한 위치에 교체 값을 사용하는 "add" 연산

### 4.4. move

- "move" 연산: 지정된 위치의 값을 제거하고 대상 위치에 추가
- 연산 객체 → 값을 이동할 위치를 참조하는 JSON Pointer 값을 포함하는 "from" 멤버 포함 필수(MUST)
- 연산 성공 조건: "from" 위치 존재 필수(MUST)

예시:

```
{ "op": "move", "from": "/a/b/c", "path": "/a/b/d" }
```

- 기능적으로 동일: "from" 위치에 대한 "remove" 연산 후 즉시 방금 제거된 값을 사용하여 대상 위치에 "add" 연산
- "from" 위치는 "path" 위치의 적절한 접두사가 될 수 없음(MUST NOT) → 위치를 자신의 하위 항목으로 이동 불가

### 4.5. copy

- "copy" 연산: 지정된 위치의 값을 대상 위치에 복사
- 연산 객체 → 값을 복사할 위치를 참조하는 JSON Pointer 값을 포함하는 "from" 멤버 포함 필수(MUST)
- 연산 성공 조건: "from" 위치 존재 필수(MUST)

예시:

```
{ "op": "copy", "from": "/a/b/c", "path": "/a/b/e" }
```

- 기능적으로 동일: "from" 멤버에 지정된 값을 사용하여 대상 위치에 "add" 연산

### 4.6. test

- "test" 연산: 대상 위치의 값이 지정된 값과 같은지 테스트
- 연산 객체 → 대상 위치의 값과 비교할 값을 전달하는 "value" 멤버 포함 필수(MUST)
- 연산 성공 조건: 대상 위치가 "value" 값과 같아야 함(MUST)
- "같다"의 의미: 대상 위치의 값과 "value"가 동일한 JSON 타입 → 해당 타입에 대한 다음 규칙으로 판정
  - 문자열: 동일한 수의 유니코드 문자를 포함하고 코드 포인트가 바이트 단위로 같으면 같다고 간주
  - 숫자: 값이 수치적으로 같으면 같다고 간주
  - 배열: 동일한 수의 값을 포함하고, 각 값이 이 타입별 규칙으로 다른 배열의 해당 위치 값과 같다고 간주되면 같다고 간주
  - 객체: 동일한 수의 멤버를 포함하고, 각 멤버가 키(문자열)와 값(이 타입별 규칙)을 비교하여 다른 객체의 멤버와 같다고 간주되면 같다고 간주
  - 리터럴(false, true, null): 같으면 같다고 간주
- 비교는 논리적 비교 → 배열 멤버 값 사이의 공백은 무의미
- 객체 멤버의 직렬화 순서도 무의미

예시:

```
{ "op": "test", "path": "/a/b/c", "value": "foo" }
```

## 5. 오류 처리

- JSON Patch 문서가 규범적 요구사항을 위반하거나 연산이 성공하지 못한 경우 → 평가 종료 필요(SHOULD), 전체 패치 문서의 적용 성공으로 간주 불가(SHALL NOT)
- JSON Patch를 HTTP PATCH 메서드와 함께 사용할 때의 오류 처리 고려사항, 조건별 제안 상태 코드 → [RFC5789] 섹션 2.2 참고
- HTTP PATCH 메서드는 [RFC5789]에 따라 원자적 → 다음 패치는 문서에 아무런 변경도 없는 결과("test" 연산이 오류를 발생시키므로):

```
[
  { "op": "replace", "path": "/a/b/c", "value": 42 },
  { "op": "test", "path": "/a/b/c", "value": "C" }
]
```

## 6. IANA 고려사항

- JSON Patch 문서에 대한 인터넷 미디어 타입: application/json-patch+json
- 타입 이름: application
- 하위 타입 이름: json-patch+json
- 필수 매개변수: 없음
- 선택적 매개변수: 없음
- 인코딩 고려사항: binary
- 보안 고려사항: 섹션 7 참고
- 상호 운용성 고려사항: 해당 없음
- 발행된 명세: RFC 6902
- 이 미디어 타입을 사용하는 애플리케이션: JSON 문서를 조작하는 애플리케이션
- 추가 정보:
  - 매직 넘버: 해당 없음
  - 파일 확장자: .json-patch
  - 매킨토시 파일 타입 코드: TEXT
- 추가 정보를 위한 연락처 및 이메일 주소: Paul C. Bryan <pbryan@anode.ca>
- 의도된 용도: COMMON
- 사용 제한: 없음
- 저자: Paul C. Bryan <pbryan@anode.ca>
- 변경 관리자: IETF

## 7. 보안 고려사항

- 이 명세: JSON [RFC4627] 및 JSON-Pointer [RFC6901]과 동일한 보안 고려사항 적용
- 일부 오래된 웹 브라우저 → 루트가 배열인 임의의 JSON 문서를 로드하도록 강제 가능 → 민감한 정보를 포함하는 JSON Patch 문서가 접근 인증 여부와 무관하게 공격자에게 노출될 위험 → 교차 사이트 요청 위조(CSRF) 공격 [CSRF]으로 알려짐
- 단, 이러한 브라우저는 널리 사용되지 않음(작성 시점 기준 시장의 1% 미만으로 추정) → 그럼에도 이 공격이 우려되는 발행자는 해당 문서를 HTTP GET으로 제공하지 않도록 권고

## 8. 감사의 글

- 다음 개인들이 이 명세에 아이디어·피드백·문구를 기여: Mike Acar, Mike Amundsen, Cyrus Daboo, Paul Davis, Stefan Koegl, Murray S. Kucherawy, Dean Landolt, Randall Leeds, James Manger, Julian Reschke, James Snell, Eli Stevens, Henry S. Thompson
- JSON Patch 문서의 구조는 XML Patch 문서 명세 [RFC5261]의 영향을 받음

## 9. 참고 문헌

### 9.1. 규범적 참고 문헌

```
[RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
           Requirement Levels", BCP 14, RFC 2119, March 1997.

[RFC4627]  Crockford, D., "The application/json Media Type for
           JavaScript Object Notation (JSON)", RFC 4627, July 2006.

[RFC6901]  Bryan, P., Ed., Zyp, K., and M. Nottingham, Ed.,
           "JavaScript Object Notation (JSON) Pointer", RFC 6901,
           April 2013.
```

### 9.2. 참고적 참고 문헌

```
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
```

## 부록 A. 예제

### A.1. 객체 멤버 추가

예시 대상 JSON 문서:

```
{ "foo": "bar"}
```

JSON Patch 문서:

```
[
  { "op": "add", "path": "/baz", "value": "qux" }
]
```

결과 JSON 문서:

```
{
  "baz": "qux",
  "foo": "bar"
}
```

### A.2. 배열 요소 추가

예시 대상 JSON 문서:

```
{ "foo": [ "bar", "baz" ] }
```

JSON Patch 문서:

```
[
  { "op": "add", "path": "/foo/1", "value": "qux" }
]
```

결과 JSON 문서:

```
{ "foo": [ "bar", "qux", "baz" ] }
```

### A.3. 객체 멤버 제거

예시 대상 JSON 문서:

```
{
  "baz": "qux",
  "foo": "bar"
}
```

JSON Patch 문서:

```
[
  { "op": "remove", "path": "/baz" }
]
```

결과 JSON 문서:

```
{ "foo": "bar" }
```

### A.4. 배열 요소 제거

예시 대상 JSON 문서:

```
{ "foo": [ "bar", "qux", "baz" ] }
```

JSON Patch 문서:

```
[
  { "op": "remove", "path": "/foo/1" }
]
```

결과 JSON 문서:

```
{ "foo": [ "bar", "baz" ] }
```

### A.5. 값 교체

예시 대상 JSON 문서:

```
{
  "baz": "qux",
  "foo": "bar"
}
```

JSON Patch 문서:

```
[
  { "op": "replace", "path": "/baz", "value": "boo" }
]
```

결과 JSON 문서:

```
{
  "baz": "boo",
  "foo": "bar"
}
```

### A.6. 값 이동

예시 대상 JSON 문서:

```
{
  "foo": {
    "bar": "baz",
    "waldo": "fred"
  },
  "qux": {
    "corge": "grault"
  }
}
```

JSON Patch 문서:

```
[
  { "op": "move", "from": "/foo/waldo", "path": "/qux/thud" }
]
```

결과 JSON 문서:

```
{
  "foo": {
    "bar": "baz"
  },
  "qux": {
    "corge": "grault",
    "thud": "fred"
  }
}
```

### A.7. 배열 요소 이동

예시 대상 JSON 문서:

```
{ "foo": [ "all", "grass", "cows", "eat" ] }
```

JSON Patch 문서:

```
[
  { "op": "move", "from": "/foo/1", "path": "/foo/3" }
]
```

결과 JSON 문서:

```
{ "foo": [ "all", "cows", "eat", "grass" ] }
```

### A.8. 값 테스트: 성공

예시 대상 JSON 문서:

```
{
  "baz": "qux",
  "foo": [ "a", 2, "c" ]
}
```

성공적인 평가를 초래할 JSON Patch 문서:

```
[
  { "op": "test", "path": "/baz", "value": "qux" },
  { "op": "test", "path": "/foo/1", "value": 2 }
]
```

### A.9. 값 테스트: 오류

예시 대상 JSON 문서:

```
{ "baz": "qux" }
```

오류 조건을 초래할 JSON Patch 문서:

```
[
  { "op": "test", "path": "/baz", "value": "bar" }
]
```

### A.10. 중첩된 멤버 객체 추가

예시 대상 JSON 문서:

```
{ "foo": "bar" }
```

JSON Patch 문서:

```
[
  { "op": "add", "path": "/child", "value": { "grandchild": { } } }
]
```

결과 JSON 문서:

```
{
  "foo": "bar",
  "child": {
    "grandchild": {
    }
  }
}
```

### A.11. 인식되지 않는 요소 무시

예시 대상 JSON 문서:

```
{ "foo": "bar" }
```

JSON Patch 문서:

```
[
  { "op": "add", "path": "/baz", "value": "qux", "xyz": 123 }
]
```

결과 JSON 문서:

```
{
  "foo": "bar",
  "baz": "qux"
}
```

### A.12. 존재하지 않는 대상에 추가

예시 대상 JSON 문서:

```
{ "foo": "bar" }
```

JSON Patch 문서:

```
[
  { "op": "add", "path": "/baz/bat", "value": "qux" }
]
```

- 이 JSON Patch 문서를 위 대상 JSON 문서에 적용하면 오류 발생(적용되지 않음) → 이유: "add" 연산의 대상 위치가 문서의 루트도, 기존 객체의 멤버도, 기존 배열의 멤버도 참조하지 않음

### A.13. 유효하지 않은 JSON Patch 문서

JSON Patch 문서:

```
[
  { "op": "add", "path": "/baz", "value": "qux", "op": "remove" }
]
```

- 이 JSON Patch 문서는 "add" 연산으로 처리 불가 → 이후에 "op":"remove" 요소를 포함
- JSON은 객체 멤버 이름의 고유성을 "SHOULD" 요구사항으로 요구 → 중복에 대한 표준 오류 처리는 없음

### A.14. ~ 이스케이프 순서

예시 대상 JSON 문서:

```
{
  "/": 9,
  "~1": 10
}
```

JSON Patch 문서:

```
[
  {"op": "test", "path": "/~01", "value": 10}
]
```

결과 JSON 문서:

```
{
  "/": 9,
  "~1": 10
}
```

### A.15. 문자열과 숫자 비교

예시 대상 JSON 문서:

```
{
  "/": 9,
  "~1": 10
}
```

JSON Patch 문서:

```
[
  {"op": "test", "path": "/~01", "value": "10"}
]
```

- 오류 발생 → 이유: 테스트 실패(문서 값은 숫자, 테스트하는 값은 문자열)

### A.16. 배열 값 추가

예시 대상 JSON 문서:

```
{ "foo": ["bar"] }
```

JSON Patch 문서:

```
[
  { "op": "add", "path": "/foo/-", "value": ["abc", "def"] }
]
```

결과 JSON 문서:

```
{ "foo": ["bar", ["abc", "def"]] }
```

## 저자 주소

```
Paul C. Bryan (editor)
Salesforce.com

Phone: +1 604 783 1481
EMail: pbryan@anode.ca


Mark Nottingham (editor)
Akamai

EMail: mnot@mnot.net
```
