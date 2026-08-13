# RFC 6570 - URI Template

```
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
```

### Abstract

- URI Template: 변수 확장을 통해 URI(Uniform Resource Identifier)의 범위를 기술하는 간결한 문자 시퀀스
- 이 명세: URI Template 구문 + URI Template을 URI 참조로 확장하는 과정 정의
- 인터넷에서 URI Template을 사용하기 위한 지침도 포함

### 이 메모의 상태

- 인터넷 표준 트랙 문서, IETF(Internet Engineering Task Force) 산출물
- IETF 커뮤니티 합의 반영, 공개 검토 완료 → IESG 발행 승인
- 인터넷 표준 관련 추가 정보: RFC 5741 섹션 2 참고
- 현재 상태·정오표·피드백 제공 방법: http://www.rfc-editor.org/info/rfc6570

### 목차

```
1.  소개
    1.1.  개요
    1.2.  레벨 및 표현식 유형
    1.3.  설계 고려 사항
    1.4.  제한 사항
    1.5.  표기 규칙
    1.6.  문자 인코딩과 유니코드 정규화
2.  구문
    2.1.  리터럴
    2.2.  표현식
    2.3.  변수
    2.4.  값 수정자
        2.4.1.  접두사 값
        2.4.2.  복합 값
3.  확장
    3.1.  리터럴 확장
    3.2.  표현식 확장
        3.2.1.  변수 확장
        3.2.2.  단순 문자열 확장: {var}
        3.2.3.  Reserved 확장: {+var}
        3.2.4.  Fragment 확장: {#var}
        3.2.5.  점 접두사 Label 확장: {.var}
        3.2.6.  경로 세그먼트 확장: {/var}
        3.2.7.  경로 스타일 파라미터 확장: {;var}
        3.2.8.  폼 스타일 쿼리 확장: {?var}
        3.2.9.  폼 스타일 쿼리 연속: {&var}
4.  보안 고려 사항
5.  감사의 글
6.  참고 문헌
    6.1.  규범적 참고 문헌
    6.2.  정보적 참고 문헌
부록 A. 구현 힌트
```

### 1. 소개

#### 1.1. 개요

- URI [RFC3986]: 유사한 리소스들의 공통 공간("URI 공간") 내에서 특정 리소스를 식별할 때 자주 사용
- 흔한 패턴 예시
  - 개인 웹 공간: `http://example.com/~fred/` · `http://example.com/~mark/`
  - 사전 항목(용어 첫 글자로 계층화): `http://example.com/dictionary/c/cat` · `http://example.com/dictionary/d/dog`
  - 서비스 인터페이스(사용자 입력 기반 호출): `http://example.com/search?q=cat&lang=en` · `http://example.com/search?q=chien&lang=fr`
- URI Template: 변수 확장을 통해 URI의 범위를 기술하는 간결한 문자 시퀀스
- 용도: 가변 부분을 쉽게 식별·기술하는 리소스 식별자 공간 추상화 메커니즘 제공
  - 가용 서비스 발견, 리소스 매핑 구성, 계산된 링크 정의, 인터페이스 지정, 리소스와의 기타 프로그래밍적 상호작용 등
- 위 리소스들을 URI Template으로 기술한 예
  - `http://example.com/~{username}/`
  - `http://example.com/dictionary/{term:1}/{term}`
  - `http://example.com/search{?q,lang}`
- 용어 정의
  - expression(표현식): 섹션 2에 정의, 둘러싸는 중괄호를 포함한 '{' 와 '}' 사이의 텍스트
  - expansion(확장): 섹션 3에 정의, 표현식 유형·변수 이름 목록·값 수정자에 따라 template expression을 처리한 후 얻어지는 문자열 결과
  - template processor(템플릿 프로세서): URI Template과 값이 있는 변수들의 집합이 주어졌을 때, 표현식을 파싱하고 각 표현식을 해당 확장으로 대체하여 template 문자열을 URI 참조로 변환하는 프로그램 또는 라이브러리
- URI Template의 역할
  - URI 공간의 구조적 설명 제공
  - 변수 값이 제공되면 해당 값에 대응하는 URI를 구성하는 방법에 대한 기계 판독 가능한 명령 제공
  - template 내 각 구분된 표현식을 표현식 유형과 명명된 변수 값으로 정의된 값으로 대체 → URI 참조로 변환
  - 표현식 유형은 단순 문자열 확장부터 여러 개의 name=value 목록까지 다양
  - 확장은 URI 일반 구문에 기반 → 구현체가 결과 URI의 체계별 요구사항을 몰라도 어떤 URI Template이든 처리 가능
- 예시: `http://www.example.com/foo{?query,number}` — 변수 이름 앞 "?" 연산자로 표시되는 폼 스타일 파라미터 표현식 포함
- "?" 연산자로 시작하는 표현식의 확장 과정: World Wide Web의 폼 스타일 인터페이스와 동일한 패턴

  ```
  http://www.example.com/foo{?query,number}
                            \_____________/
                               |
                               |
  ```
  - ['query', 'number'] 내 정의된 각 변수에 대해 → 첫 번째 대체면 "?", 이후에는 "&" 대체 → 변수 이름·'='·값을 뒤에 붙임
- 변수 값이 `query := "mycelium"`, `number := 100`일 때
  - 확장 결과: `http://www.example.com/foo?query=mycelium&number=100`
  - 'query' 미정의: `http://www.example.com/foo?number=100`
  - 두 변수 모두 미정의: `http://www.example.com/foo`
- URI Template은 절대 형식·상대 형식 모두로 제공 가능 → 결과 참조가 상대에서 절대로 해석되기 전에 template이 먼저 확장
- URI 구문이 결과에 사용되지만, template 문자열은 IRI 참조[RFC3987]에서 찾을 수 있는 더 넓은 문자 집합 포함 가능
  - 즉 URI Template은 IRI template이기도 함 → 처리 결과는 [RFC3987] 섹션 3.2에 정의된 과정에 따라 IRI로 변환 가능

#### 1.2. 레벨 및 표현식 유형

- URI Template: 고정된 매크로 정의 집합을 가진 매크로 언어와 유사 → 표현식 유형이 확장 과정을 결정
- 기본 표현식 유형: 단순 문자열 확장
  - 단일 명명된 변수가 unreserved URI 문자 집합(섹션 1.5)에 포함되지 않는 문자를 퍼센트 인코딩한 후 해당 값의 문자열로 대체
- 이 명세 이전 대부분의 template processor는 기본 표현식 유형만 구현 → Level 1 template로 지칭

- Level 1 예시 (`var := "value"`, `hello := "Hello World!"`)
  - 단순 문자열 확장 (3.2.2)
    - `{var}` → `value`
    - `{hello}` → `Hello%20World%21`

- Level 2 template: 추가 연산자
  - "+": reserved URI 문자(섹션 1.5)를 포함할 수 있는 값의 확장
  - "#": fragment 식별자 확장

- Level 2 예시 (`var := "value"`, `hello := "Hello World!"`, `path := "/foo/bar"`)
  - Reserved 문자열 확장 (3.2.3)
    - `{+var}` → `value`
    - `{+hello}` → `Hello%20World!`
    - `{+path}/here` → `/foo/bar/here`
    - `here?ref={+path}` → `here?ref=/foo/bar`
  - Fragment 확장, "#" 접두사 (3.2.4)
    - `X{#var}` → `X#value`
    - `X{#hello}` → `X#Hello%20World!`

- Level 3 template: 표현식당 쉼표로 구분되는 복수 변수 허용 + 다음 연산자 추가
  - 점 접두사 label · 슬래시 접두사 경로 세그먼트 · 세미콜론 접두사 경로 파라미터 · 앰퍼샌드 구분 name=value 쌍 쿼리 구문

- Level 3 예시 (`var := "value"`, `hello := "Hello World!"`, `empty := ""`, `path := "/foo/bar"`, `x := "1024"`, `y := "768"`)
  - 복수 변수 문자열 확장 (3.2.2)
    - `map?{x,y}` → `map?1024,768`
    - `{x,hello,y}` → `1024,Hello%20World%21,768`
  - 복수 변수 Reserved 확장 (3.2.3)
    - `{+x,hello,y}` → `1024,Hello%20World!,768`
    - `{+path,x}/here` → `/foo/bar,1024/here`
  - 복수 변수 Fragment 확장 (3.2.4)
    - `{#x,hello,y}` → `#1024,Hello%20World!,768`
    - `{#path,x}/here` → `#/foo/bar,1024/here`
  - Label 확장, 점 접두사 (3.2.5)
    - `X{.var}` → `X.value`
    - `X{.x,y}` → `X.1024.768`
  - 경로 세그먼트, 슬래시 접두사 (3.2.6)
    - `{/var}` → `/value`
    - `{/var,x}/here` → `/value/1024/here`
  - 경로 스타일 파라미터, 세미콜론 접두사 (3.2.7)
    - `{;x,y}` → `;x=1024;y=768`
    - `{;x,y,empty}` → `;x=1024;y=768;empty`
  - 폼 스타일 쿼리, 앰퍼샌드 구분 (3.2.8)
    - `{?x,y}` → `?x=1024&y=768`
    - `{?x,y,empty}` → `?x=1024&y=768&empty=`
  - 폼 스타일 쿼리 연속 (3.2.9)
    - `?fixed=yes{&x}` → `?fixed=yes&x=1024`
    - `{&x,y,empty}` → `&x=1024&y=768&empty=`

- Level 4 template: 각 변수 이름에 선택적 접미사 값 수정자 추가
  - 접두사 수정자(":"): 확장 시 값 문자열의 시작 부분에서 제한된 수의 문자만 사용(섹션 2.4.1)
  - 전개("*") 수정자: 변수를 이름 목록 또는 (name, value) 쌍의 연관 배열로 구성된 복합 값으로 취급 → 각 구성원을 별도 변수처럼 확장(섹션 2.4.2)

- Level 4 예시 (`var := "value"`, `hello := "Hello World!"`, `path := "/foo/bar"`, `list := ("red", "green", "blue")`, `keys := [("semi",";"),("dot","."),("comma",",")]`)
  - 값 수정자를 사용한 문자열 확장 (3.2.2)
    - `{var:3}` → `val`
    - `{var:30}` → `value`
    - `{list}` → `red,green,blue`
    - `{list*}` → `red,green,blue`
    - `{keys}` → `semi,%3B,dot,.,comma,%2C`
    - `{keys*}` → `semi=%3B,dot=.,comma=%2C`
  - 값 수정자를 사용한 Reserved 확장 (3.2.3)
    - `{+path:6}/here` → `/foo/b/here`
    - `{+list}` → `red,green,blue`
    - `{+list*}` → `red,green,blue`
    - `{+keys}` → `semi,;,dot,.,comma,,`
    - `{+keys*}` → `semi=;,dot=.,comma=,`
  - 값 수정자를 사용한 Fragment 확장 (3.2.4)
    - `{#path:6}/here` → `#/foo/b/here`
    - `{#list}` → `#red,green,blue`
    - `{#list*}` → `#red,green,blue`
    - `{#keys}` → `#semi,;,dot,.,comma,,`
    - `{#keys*}` → `#semi=;,dot=.,comma=,`
  - Label 확장, 점 접두사 (3.2.5)
    - `X{.var:3}` → `X.val`
    - `X{.list}` → `X.red,green,blue`
    - `X{.list*}` → `X.red.green.blue`
    - `X{.keys}` → `X.semi,%3B,dot,.,comma,%2C`
    - `X{.keys*}` → `X.semi=%3B.dot=..comma=%2C`
  - 경로 세그먼트, 슬래시 접두사 (3.2.6)
    - `{/var:1,var}` → `/v/value`
    - `{/list}` → `/red,green,blue`
    - `{/list*}` → `/red/green/blue`
    - `{/list*,path:4}` → `/red/green/blue/%2Ffoo`
    - `{/keys}` → `/semi,%3B,dot,.,comma,%2C`
    - `{/keys*}` → `/semi=%3B/dot=./comma=%2C`
  - 경로 스타일 파라미터, 세미콜론 접두사 (3.2.7)
    - `{;hello:5}` → `;hello=Hello`
    - `{;list}` → `;list=red,green,blue`
    - `{;list*}` → `;list=red;list=green;list=blue`
    - `{;keys}` → `;keys=semi,%3B,dot,.,comma,%2C`
    - `{;keys*}` → `;semi=%3B;dot=.;comma=%2C`
  - 폼 스타일 쿼리, 앰퍼샌드 구분 (3.2.8)
    - `{?var:3}` → `?var=val`
    - `{?list}` → `?list=red,green,blue`
    - `{?list*}` → `?list=red&list=green&list=blue`
    - `{?keys}` → `?keys=semi,%3B,dot,.,comma,%2C`
    - `{?keys*}` → `?semi=%3B&dot=.&comma=%2C`
  - 폼 스타일 쿼리 연속 (3.2.9)
    - `{&var:3}` → `&var=val`
    - `{&list}` → `&list=red,green,blue`
    - `{&list*}` → `&list=red&list=green&list=blue`
    - `{&keys}` → `&keys=semi,%3B,dot,.,comma,%2C`
    - `{&keys*}` → `&semi=%3B&dot=.&comma=%2C`

#### 1.3. 설계 고려 사항

- URI Template과 유사한 메커니즘: WSDL[WSDL] · WADL[WADL] · OpenSearch[OpenSearch] 등 여러 명세에서 정의
- 이 명세의 목표: 여러 인터넷 애플리케이션·인터넷 메시지 필드에서 URI Template이 일관되게 사용되도록 구문을 확장·공식 정의 + 이전 정의와의 호환성 유지
- 구문 설계의 균형: 강력한 확장 메커니즘의 필요성 ↔ 구현 용이성의 필요성
  - 파싱은 극히 간단하면서도 많은 일반적 template 시나리오를 표현하기에 충분한 유연성
  - 구현체는 template을 파싱하고 단일 패스로 확장 수행 가능
- 단일 문자 연산자가 URI 일반 구문 구분자와 일치 → 일반적 예시에서 template이 단순하고 가독성 좋음
  - 나열된 변수 중 어느 것도 미정의면 연산자의 관련 구분자(".", ";", "/", "?", "&", "#") 생략
  - ";"(경로 스타일 파라미터) 확장: 값이 비어 있을 때 "=" 생략 / "?"(폼 스타일 파라미터) 확장: 값이 비어 있어도 "=" 유지
  - 복수 변수·목록 값: 연산자에 미리 정의된 결합 메커니즘이 없으면 ","로 결합
  - "+"·"#" 연산자: 변수 값 내 인코딩되지 않은 reserved 문자를 그대로 대체 / 다른 연산자: 확장 전 값의 reserved 문자를 퍼센트 인코딩
- URI 공간의 가장 일반적인 경우는 Level 1 template 표현식으로 기술 가능
  - URI 생성에만 관심 있으면 template 구문을 단순 변수 확장으로 제한 가능(더 복잡한 형태는 변수 값 변경으로 생성)
  - 단, URI Template은 기존 데이터 값 관점에서 식별자 레이아웃을 기술하는 추가 목표도 가짐 → 리소스 식별자의 일반적 할당 방식을 반영하는 연산자 포함
  - 접두사 부분 문자열은 대규모 리소스 공간 분할에 자주 사용 → 값 수정자로 단일 변수 이름에서 부분 문자열과 전체 값 문자열 모두 지정 가능

#### 1.4. 제한 사항

- URI Template은 식별자의 상위 집합을 기술 → 각 구분된 변수 표현식의 모든 가능한 확장이 기존 리소스의 URI에 대응함을 암시하지 않음
- 기대 사항: template에 따라 URI를 구성하는 애플리케이션에 적절한 값 집합이 제공되거나, 최소한 해당 값에 대한 사용자 데이터 입력 검증 수단이 제공되어야 함
- URI Template은 URI가 아님
  - 추상적/물리적 리소스를 식별하지 않음
  - URI로 파싱되지 않음
  - template processor가 사용 전에 확장하지 않는 한, URI가 기대되는 곳에서 사용 금지
  - URI Template을 전달하는 프로토콜 요소와 URI 참조를 기대하는 프로토콜 요소는 별도의 필드·요소·속성 이름으로 구별 필요
- 변수 매칭(역방향 사용): template을 완전히 형성된 URI와 비교하여 변수 부분을 추출·할당
  - 잘 작동하는 조건: template 표현식이 URI의 시작/끝, 또는 확장의 일부가 될 수 없는 문자(예: 단순 문자열 표현식을 둘러싸는 reserved 문자)로 구분되는 경우
  - 일반적으로는 정규 표현식 언어가 변수 매칭에 더 적합

#### 1.5. 표기 규칙

- "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL" 키워드: [RFC2119]에 설명된 대로 해석
- ABNF(Augmented Backus-Naur Form) [RFC5234] 표기법 사용, 다음 규칙은 규범적 참고 문헌 [RFC5234], [RFC3986], [RFC3987]에서 인용

```
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
```

#### 1.6. 문자 인코딩과 유니코드 정규화

- 이 명세는 [RFC6365]에 정의된 대로 "character", "character encoding scheme", "code point", "coded character set", "glyph", "non-ASCII", "normalization", "protocol element", "regular expression" 용어를 사용
- ABNF 표기법: 터미널 값을 US-ASCII 코드 문자 집합[ASCII]의 상위 집합인 비음수 정수(코드 포인트)로 정의
  - 이 명세는 터미널 값을 Unicode 코드 문자 집합[UNIV6] 내 코드 포인트로 정의
- 구문·template 확장 과정은 Unicode 코드 포인트 관점에서 정의되지만, 실제 template은 네트워크 프로토콜 요소에 내장된 옥텟이든 버스 측면에 그려진 글리프이든 맥락에 적합한 어떤 형태·인코딩의 문자 시퀀스로도 존재
  - 이 명세는 URI Template 문자와 저장·전송 옥텟 간 매핑을 위한 특정 문자 인코딩 체계를 의무화하지 않음
  - URI Template이 프로토콜 요소에 나타나는 경우 → 문자 인코딩 체계는 해당 프로토콜이 정의 / 정의가 없으면 주변 텍스트와 동일한 인코딩으로 가정
  - template 확장 과정 중에만 URI Template 문자열이 Unicode 코드 포인트 시퀀스로 처리되어야 함(REQUIRED)
- Unicode Standard[UNIV6]: 다양한 목적을 위해 문자 시퀀스 간 다양한 동치 정의 / Unicode Standard Annex #15[UTR15]: 해당 동치에 대한 Normalization Form 정의
  - 정규화 형태는 동치인 문자열을 일관되게 인코딩하는 방법을 결정
  - 이론상 template processor를 포함한 모든 URI 처리 구현체는 URI 참조 생성 시 동일한 정규화 형태를 사용해야 하나, 실제로는 그렇지 않음
  - 값이 리소스와 동일한 서버에 의해 제공된 경우 → 해당 문자열이 이미 그 서버가 기대하는 형태라고 가정 가능
  - 데이터 입력 대화 상자처럼 사용자가 값을 제공하는 경우 → template processor의 확장에 사용되기 전에 Normalization Form C(NFC: Canonical Decomposition 후 Canonical Composition)로 정규화 필요(SHOULD)
- 읽을 수 있는 문자열을 나타내는 non-ASCII 데이터가 URI 참조에서 사용되기 위해 퍼센트 인코딩될 때 → template processor는 먼저 해당 문자열을 UTF-8[RFC3629]로 인코딩한 다음 허용되지 않는 옥텟을 퍼센트 인코딩해야 함(MUST)

### 2. 구문

- URI Template: 0개 이상의 내장된 변수 표현식을 포함하는 인쇄 가능한 Unicode 문자열, 각 표현식은 중괄호 쌍('{', '}')으로 구분

```
URI-Template  = *( literals / expression )
```

- template(및 template processor 구현)은 위에서 네 가지 점진적 레벨의 관점으로 설명되었으나, URI-Template 구문은 Level 4의 ABNF로 정의
- 하위 레벨 template으로 제한된 template processor는 상위 레벨에만 적용되는 ABNF 규칙을 제외 가능(MAY)
- 단, 지원되지 않는 레벨이 최종 사용자에게 적절히 식별될 수 있도록 모든 파서가 전체 구문을 구현하는 것이 권장(RECOMMENDED)

#### 2.1. 리터럴

- URI Template 문자열에서 표현식 외부의 문자 처리
  - 해당 문자가 URI에서 허용되는 경우(reserved / unreserved / pct-encoded) → URI 참조에 문자 그대로 복사
  - 허용되지 않는 경우 → 해당 문자의 UTF-8[RFC3629] 인코딩에 해당하는 퍼센트 인코딩 삼중항 시퀀스로 복사

```
literals      =  %x21 / %x23-24 / %x26 / %x28-3B / %x3D / %x3F-5B
              /  %x5D / %x5F / %x61-7A / %x7E / ucschar / iprivate
              /  pct-encoded
                   ; CTL, SP, DQUOTE, "'", "%"(pct-encoded 제외),
                   ;  "<", ">", "\", "^", "`", "{", "|", "}"를
                   ;  제외한 모든 Unicode 문자
```

#### 2.2. 표현식

- Template expression: URI Template의 매개변수화된 부분
  - 표현식 유형과 해당 확장 과정을 정의하는 선택적 연산자 + 쉼표로 구분된 변수 지정자(변수 이름과 선택적 값 수정자) 목록으로 구성
  - 연산자가 제공되지 않으면 → 표현식은 unreserved 값의 단순 변수 확장으로 기본 설정

```
expression    =  "{" [ operator ] variable-list "}"
operator      =  op-level2 / op-level3 / op-reserve
op-level2     =  "+" / "#"
op-level3     =  "." / "/" / ";" / "?" / "&"
op-reserve    =  "=" / "," / "!" / "@" / "|"
```

- 연산자 문자: URI 일반 구문에서 reserved 문자로서의 각 역할을 반영하도록 선택
- 이 명세 섹션 3에 정의된 연산자
  - `+`: reserved 문자 문자열
  - `#`: "#"가 접두사인 fragment 식별자
  - `.`: "."가 접두사인 이름 label 또는 확장자
  - `/`: "/"가 접두사인 경로 세그먼트
  - `;`: ";"가 접두사인 경로 파라미터 이름 또는 name=value 쌍
  - `?`: "?"로 시작하고 "&"로 구분된 name=value 쌍으로 구성된 쿼리 구성 요소
  - `&`: 리터럴 쿼리 구성 요소 내에서 &name=value 쌍의 연속
- 등호("="), 쉼표(","), 느낌표("!"), 골뱅이("@"), 파이프("|") 연산자 문자: 향후 확장을 위해 예약
- 표현식 구문은 달러("$") 및 괄호["(" 와 ")"] 문자의 사용을 명시적으로 제외 → 이 명세 범위 밖에서 사용 가능
  - 예: 매크로 언어가 문자열을 URI Template으로 처리하기 전에 매크로 대체를 적용하기 위해 이러한 문자 사용 가능

#### 2.3. 변수

- 연산자(있는 경우) 뒤에, 각 표현식은 하나 이상의 쉼표로 구분된 변수 지정자(varspec) 목록 포함
- 변수 이름의 다중 목적: 기대되는 값의 종류에 대한 문서화 · template processor 내에서 값을 연관시키기 위한 식별자 · name=value 확장에서 이름에 사용되는 리터럴 문자열(연관 배열을 전개하는 경우 제외)
- 변수 이름은 대소문자를 구분 (이름이 대소문자를 구분하는 URI 구성 요소 내에서 확장될 수 있기 때문)

```
variable-list =  varspec *( "," varspec )
varspec       =  varname [ modifier-level4 ]
varname       =  varchar *( ["."] varchar )
varchar       =  ALPHA / DIGIT / "_" / pct-encoded
```

- varname은 하나 이상의 퍼센트 인코딩 삼중항을 포함 가능(MAY)
  - 이 삼중항은 변수 이름의 필수적인 부분으로 간주되며 처리 중 디코딩되지 않음
  - 퍼센트 인코딩된 문자를 포함하는 varname은 동일한 문자가 디코딩된 varname과 같은 변수가 아님
  - URI Template을 제공하는 애플리케이션은 변수 이름 내 퍼센트 인코딩 사용에 일관성을 유지할 것으로 기대
- 표현식은 template processor에 알려지지 않거나 undef 또는 null과 같은 특별한 "정의되지 않은" 값으로 설정된 변수를 참조 가능(MAY) → 확장 과정에서 특별한 처리를 받음(섹션 3.2.1)
- 길이가 0인 문자열인 변수 값은 정의되지 않은 것으로 간주되지 않음 → 빈 문자열의 정의된 값을 가짐
- Level 4 template에서 변수는 값의 목록 또는 (name, value) 쌍의 연관 배열 형태의 복합 값을 가질 수 있음
  - 이러한 값 유형은 template 구문에 의해 직접 표시되지 않지만 확장 과정에 영향(섹션 3.2.1)
- 목록 값으로 정의된 변수: 목록이 구성원을 0개 포함하는 경우 정의되지 않은 것으로 간주
- (name, value) 쌍의 연관 배열로 정의된 변수: 배열이 구성원을 0개 포함하거나 배열의 모든 구성원 이름이 정의되지 않은 값과 연관된 경우 정의되지 않은 것으로 간주

#### 2.4. 값 수정자

- Level 4 template 표현식의 각 변수는 다음을 나타내는 수정자를 가질 수 있음
  - 확장이 변수 값 문자열의 접두사로 제한됨
  - 확장이 값 목록 또는 (name, value) 쌍의 연관 배열 형태의 복합 값으로 전개됨

```
modifier-level4 =  prefix / explode
```

##### 2.4.1. 접두사 값

- 접두사 수정자: 변수 확장이 변수 값 문자열의 접두사로 제한됨을 나타냄
  - 용도: 참조 인덱스·해시 기반 저장소에서 식별자 공간을 계층적으로 분할 + 확장된 값을 최대 문자 수로 제한
  - 복합 값을 가진 변수에는 적용되지 않음

```
prefix        =  ":" max-length
max-length    =  %x31-39 0*3DIGIT   ; 10000 미만의 양의 정수
```

- max-length: Unicode 문자열로서의 변수 값 시작 부분에서의 최대 문자 수를 나타내는 양의 정수
  - 다중 옥텟 인코딩 문자의 옥텟 사이 또는 퍼센트 인코딩 삼중항 내에서의 분할 방지를 위해 옥텟이 아닌 문자 단위로 번호 매김
  - max-length가 변수 값의 길이보다 크면 → 전체 값 문자열 사용
- 예시 (`var := "value"`, `semi := ";"`)
  - `{var}` → `value`
  - `{var:20}` → `value`
  - `{var:3}` → `val`
  - `{semi}` → `%3B`
  - `{semi:2}` → `%3B`

##### 2.4.2. 복합 값

- 전개("*") 수정자: 해당 변수가 값의 목록 또는 (name, value) 쌍의 연관 배열로 구성된 복합 값으로 취급되어야 함을 나타냄 → 확장 과정이 복합체의 각 구성원을 별도 변수로 나열한 것처럼 적용
  - 이러한 종류의 변수 지정은 변수 이름과 확장 후 URI 참조가 나타나는 방식 사이의 대응이 적어 비전개 변수보다 자기 문서화 정도가 상당히 낮음

```
explode       =  "*"
```

- URI Template은 유형이나 스키마의 표시를 포함하지 않음 → 전개된 변수의 유형은 맥락에 의해 결정되는 것으로 가정
  - processor에 문자열, 목록, 연관 배열로 값을 구별하는 형태로 값이 제공 가능
  - template이 사용되는 맥락(스크립트, 마크업 언어, Interface Definition Language 등)이 변수 이름을 유형·구조·스키마와 연관시키는 규칙 정의 가능
- 전개 수정자는 URI Template 구문의 간결성 향상
  - 예: 주어진 도로 주소에 대한 지리적 지도를 제공하는 리소스가 부분 주소(도시 또는 우편번호만 등) 입력을 위한 수백 가지 순열의 필드를 허용 가능
  - 각 주소 구성 요소를 순서대로 나열한 template 또는 전개 수정자를 활용한 더 단순한 template으로 기술 가능: `/mapper{?address*}`
  - "address" 변수가 무엇을 포함할 수 있는지는 어떤 맥락(예: [UPU-S42] 등 다른 주소 표준 참조)과 함께 정의
  - 스키마를 인식하는 수신자의 적절한 확장 예: `/mapper?city=Newport%20Beach&state=CA`
- 전개된 변수의 확장 과정: 사용 중인 연산자 + 복합 값이 값 목록으로 취급되는지 또는 (name, value) 쌍의 연관 배열로 취급되는지 모두에 의존
  - 구조체는 구조체 정의의 필드에 대응하는 이름과 하위 구조체의 이름 계층을 나타내기 위해 "." 구분자를 사용하는 연관 배열인 것처럼 처리
- 변수가 복합 구조를 가지고 해당 구조의 일부 필드만 정의된 값을 가지는 경우 → 정의된 쌍만 확장에 존재 (많은 수의 잠재적 쿼리 용어로 구성된 template에 유용)
- 목록 변수 + 전개 수정자: 확장이 목록의 구성원 값에 대해 반복
  - 경로·쿼리 파라미터 확장 시 각 구성원 값은 변수의 이름과 (varname, value) 쌍으로 짝지어짐 → 경로·쿼리 파라미터가 복수 값에 대해 반복 가능
  - 예시 (`year := ("1965", "2000", "2012")`, `dom := ("example", "com")`)
    - `find{?year*}` → `find?year=1965&year=2000&year=2012`
    - `www{.dom*}` → `www.example.com`

### 3. 확장

- URI Template 확장 과정: template 문자열을 처음부터 끝까지 스캔하면서 리터럴 문자를 복사하고 각 표현식을 표현식 내 명명된 각 변수의 값에 표현식의 연산자를 적용한 결과로 대체
  - 각 변수의 값은 template 확장 전에 형성되어야 함(MUST)
- URI Template 문법의 각 측면에 대한 확장 요구사항: 이 섹션에서 정의 / 확장 과정 전체에 대한 비규범적 알고리즘: 부록 A에서 제공
- template processor가 표현식 외부에서 <URI-Template> 문법과 일치하지 않는 문자 시퀀스를 만나는 경우
  - template 처리는 중단되어야 함(SHOULD)
  - URI 참조 결과는 template의 확장된 부분과 그 뒤의 확장되지 않은 나머지를 포함해야 함(SHOULD)
  - 오류의 위치와 유형이 호출 애플리케이션에 표시되어야 함(SHOULD)
- 표현식에서 오류가 발생하는 경우(예: 인식하지 못하거나 아직 지원하지 않는 연산자·값 수정자, <expression> 문법에서 허용되지 않는 문자)
  - 표현식의 처리되지 않은 부분은 확장되지 않은 채 결과에 복사되어야 함(SHOULD)
  - template의 나머지 처리는 계속되어야 함(SHOULD)
  - 오류의 위치와 유형이 호출 애플리케이션에 표시되어야 함(SHOULD)
- 오류가 발생하면 반환되는 결과는 유효한 URI 참조가 아닐 수 있음 → 진단 목적으로만 사용되는 불완전하게 확장된 template 문자열

#### 3.1. 리터럴 확장

- 리터럴 문자가 URI 구문 어디에서나 허용되는 경우(unreserved / reserved / pct-encoded) → 결과 문자열에 직접 복사
- 그렇지 않으면 → 리터럴 문자의 퍼센트 인코딩 등가물이 해당 문자를 먼저 UTF-8의 옥텟 시퀀스로 인코딩한 다음 각 옥텟을 퍼센트 인코딩 삼중항으로 인코딩하여 결과 문자열에 복사

#### 3.2. 표현식 확장

- 각 표현식은 여는 중괄호("{") 문자로 표시되며 다음 닫는 중괄호("}")까지 계속 → 표현식은 중첩될 수 없음
- 확장 방식: 표현식 유형을 결정한 다음 표현식 내 각 쉼표로 구분된 varspec에 대해 해당 유형의 확장 과정을 따름
  - Level 1 template: 기본 연산자(단순 문자열 값 확장)와 표현식당 단일 변수로 제한
  - Level 2 template: 표현식당 단일 varspec으로 제한
- 표현식 유형 결정: 여는 중괄호 뒤의 첫 번째 문자를 확인
  - 문자가 연산자이면 → 나중의 확장 결정을 위해 해당 연산자와 관련된 표현식 유형을 기억, variable-list를 위한 다음 문자로 건너뜀
  - 첫 번째 문자가 연산자가 아니면 → 표현식 유형은 단순 문자열 확장이며 첫 번째 문자가 variable-list의 시작

- 아래 하위 섹션의 예시들은 변수 값에 대해 다음 정의를 사용

```
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
```

##### 3.2.1. 변수 확장

- 정의되지 않은(섹션 2.3) 변수: 값이 없으며 확장 과정에서 무시
  - 표현식의 모든 변수가 정의되지 않은 경우 → 표현식의 확장은 빈 문자열
- 정의된 비어 있지 않은 값의 변수 확장은 허용된 URI 문자의 부분 문자열을 생성
  - 섹션 1.6에서 설명한 대로, 확장 과정은 non-ASCII 문자가 결과 URI 참조에서 일관되게 퍼센트 인코딩되도록 Unicode 코드 포인트 관점에서 정의
  - 방법 1: 값 문자열을 UTF-8로 변환(아직 UTF-8이 아닌 경우)한 다음 허용된 집합에 포함되지 않는 각 옥텟을 해당 퍼센트 인코딩 삼중항으로 변환
  - 방법 2: 값의 네이티브 문자 인코딩에서 허용된 URI 문자 집합으로 직접 매핑하고, 나머지 허용되지 않는 문자를 UTF-8[RFC3629]로 인코딩될 때의 옥텟에 해당하는 퍼센트 인코딩 삼중항 시퀀스로 매핑
- 허용 집합은 표현식 유형에 따라 다름
  - reserved("+") 및 fragment("#") 확장: (unreserved / reserved / pct-encoded)의 합집합에 있는 문자 집합이 퍼센트 인코딩 없이 통과되도록 허용
  - 다른 모든 표현식 유형: unreserved 문자만 퍼센트 인코딩 없이 통과되도록 허용
  - 퍼센트 문자("%")는 퍼센트 인코딩 삼중항의 일부로서만 그리고 reserved/fragment 확장에 대해서만 허용 → 다른 모든 경우, "%"의 값 문자는 변수 확장에 의해 "%25"로 퍼센트 인코딩되어야 함(MUST)
- 변수가 표현식에서 또는 URI Template의 복수 표현식 내에서 두 번 이상 나타나는 경우 → 해당 변수의 값은 확장 과정 전체에서 정적으로 유지되어야 함(MUST)(변수는 각 확장을 계산하는 목적으로 동일한 값을 가져야 함)
  - 단, reserved 문자 또는 퍼센트 인코딩 삼중항이 값에 포함된 경우 일부 표현식 유형에서는 퍼센트 인코딩되고 다른 유형에서는 그렇지 않을 수 있음
- 단순 문자열 값인 변수: 확장은 인코딩된 값을 결과 문자열에 추가하는 것으로 구성
  - 전개 수정자는 효과가 없음
  - 접두사 수정자는 확장을 디코딩된 값의 처음 max-length 문자로 제한 (값에 다중 옥텟 또는 퍼센트 인코딩 문자가 포함된 경우 문자 중간에서 값이 분할되지 않도록 주의, 각 Unicode 코드 포인트를 하나의 문자로 계산)
- 연관 배열인 변수: 확장은 표현식 유형과 전개 수정자의 존재 여부 모두에 의존
  - 전개 수정자 없음: 정의된 값을 가진 각 (name, value) 쌍의 쉼표로 구분된 연결을 추가
  - 전개 수정자 있음: 정의된 각 쌍을 "name=value" 형태로 추가 (값이 빈 문자열이고 표현식 유형이 폼 스타일 파라미터를 나타내지 않는 경우, 즉 "?" 또는 "&" 유형이 아닌 경우 단순히 "name" 형태로 추가) — name과 value 문자열 모두 단순 문자열 값과 동일한 방식으로 인코딩
  - 표현식 유형별로 정의된 값을 가진 쌍 사이에 추가되는 구분자
    - (기본): `,`
    - `+`: `,`
    - `#`: `,`
    - `.`: `.`
    - `/`: `/`
    - `;`: `;`
    - `?`: `&`
    - `&`: `&`
- 값의 목록인 변수: 확장은 표현식 유형과 전개 수정자의 존재 여부 모두에 의존
  - 전개 수정자 없음: 정의된 구성원 문자열 값의 쉼표로 구분된 연결로 구성
  - 전개 수정자 있음 + 표현식 유형이 명명된 파라미터를 확장(";", "?", 또는 "&"): 목록은 각 구성원 값이 목록의 varname과 짝지어진 연관 배열인 것처럼 확장
  - 그렇지 않으면: 각 값이 위 구분자 정의에 따른 표현식 유형의 관련 구분자로 구분되는 별도의 변수 값 목록인 것처럼 확장
- 예시 (`count := ("one", "two", "three")`)
  - `{count}` → `one,two,three`
  - `{count*}` → `one,two,three`
  - `{/count}` → `/one,two,three`
  - `{/count*}` → `/one/two/three`
  - `{;count}` → `;count=one,two,three`
  - `{;count*}` → `;count=one;count=two;count=three`
  - `{?count}` → `?count=one,two,three`
  - `{?count*}` → `?count=one&count=two&count=three`
  - `{&count*}` → `&count=one&count=two&count=three`

##### 3.2.2. 단순 문자열 확장: {var}

- 단순 문자열 확장: 연산자가 주어지지 않을 때의 기본 표현식 유형
- variable-list의 각 정의된 변수에 대해, unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
  - 둘 이상의 변수가 정의된 값을 가지면 → 변수 확장 사이의 구분자로 결과 문자열에 쉼표(",") 추가
- 예시
  - `{var}` → `value`
  - `{hello}` → `Hello%20World%21`
  - `{half}` → `50%25`
  - `O{empty}X` → `OX`
  - `O{undef}X` → `OX`
  - `{x,y}` → `1024,768`
  - `{x,hello,y}` → `1024,Hello%20World%21,768`
  - `?{x,empty}` → `?1024,`
  - `?{x,undef}` → `?1024`
  - `?{undef,y}` → `?768`
  - `{var:3}` → `val`
  - `{var:30}` → `value`
  - `{list}` → `red,green,blue`
  - `{list*}` → `red,green,blue`
  - `{keys}` → `semi,%3B,dot,.,comma,%2C`
  - `{keys*}` → `semi=%3B,dot=.,comma=%2C`

##### 3.2.3. Reserved 확장: {+var}

- Level 2 이상 template을 위한 더하기("+") 연산자로 표시되는 Reserved 확장: 대체된 값이 퍼센트 인코딩 삼중항과 reserved 집합의 문자도 포함할 수 있다는 점을 제외하면 단순 문자열 확장과 동일
- variable-list의 각 정의된 변수에 대해, (unreserved / reserved / pct-encoded) 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
  - 둘 이상의 변수가 정의된 값을 가지면 → 변수 확장 사이의 구분자로 결과 문자열에 쉼표(",") 추가
- 예시
  - `{+var}` → `value`
  - `{+hello}` → `Hello%20World!`
  - `{+half}` → `50%25`
  - `{base}index` → `http%3A%2F%2Fexample.com%2Fhome%2Findex`
  - `{+base}index` → `http://example.com/home/index`
  - `O{+empty}X` → `OX`
  - `O{+undef}X` → `OX`
  - `{+path}/here` → `/foo/bar/here`
  - `here?ref={+path}` → `here?ref=/foo/bar`
  - `up{+path}{var}/here` → `up/foo/barvalue/here`
  - `{+x,hello,y}` → `1024,Hello%20World!,768`
  - `{+path,x}/here` → `/foo/bar,1024/here`
  - `{+path:6}/here` → `/foo/b/here`
  - `{+list}` → `red,green,blue`
  - `{+list*}` → `red,green,blue`
  - `{+keys}` → `semi,;,dot,.,comma,,`
  - `{+keys*}` → `semi=;,dot=.,comma=,`

##### 3.2.4. Fragment 확장: {#var}

- Level 2 이상 template을 위한 크로스해치("#") 연산자로 표시되는 Fragment 확장: 변수 중 하나라도 정의된 경우 크로스해치 문자(fragment 구분자)가 먼저 결과 문자열에 추가된다는 점을 제외하면 Reserved 확장과 동일
- 예시
  - `{#var}` → `#value`
  - `{#hello}` → `#Hello%20World!`
  - `{#half}` → `#50%25`
  - `foo{#empty}` → `foo#`
  - `foo{#undef}` → `foo`
  - `{#x,hello,y}` → `#1024,Hello%20World!,768`
  - `{#path,x}/here` → `#/foo/bar,1024/here`
  - `{#path:6}/here` → `#/foo/b/here`
  - `{#list}` → `#red,green,blue`
  - `{#list*}` → `#red,green,blue`
  - `{#keys}` → `#semi,;,dot,.,comma,,`
  - `{#keys*}` → `#semi=;,dot=.,comma=,`

##### 3.2.5. 점 접두사 Label 확장: {.var}

- Level 3 이상 template을 위한 점(".") 연산자로 표시되는 Label 확장: 다양한 도메인 이름이나 경로 선택자(예: 파일 확장자)를 가진 URI 공간을 기술하는 데 유용
- variable-list의 각 정의된 변수에 대해, 결과 문자열에 "."를 추가한 다음 unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
- "."는 unreserved 집합에 포함 → "."를 포함하는 값은 복수 label을 추가하는 효과
- 예시
  - `{.who}` → `.fred`
  - `{.who,who}` → `.fred.fred`
  - `{.half,who}` → `.50%25.fred`
  - `www{.dom*}` → `www.example.com`
  - `X{.var}` → `X.value`
  - `X{.empty}` → `X.`
  - `X{.undef}` → `X`
  - `X{.var:3}` → `X.val`
  - `X{.list}` → `X.red,green,blue`
  - `X{.list*}` → `X.red.green.blue`
  - `X{.keys}` → `X.semi,%3B,dot,.,comma,%2C`
  - `X{.keys*}` → `X.semi=%3B.dot=..comma=%2C`
  - `X{.empty_keys}` → `X`
  - `X{.empty_keys*}` → `X`

##### 3.2.6. 경로 세그먼트 확장: {/var}

- Level 3 이상 template의 슬래시("/") 연산자로 표시되는 경로 세그먼트 확장: URI 경로 계층 구조를 기술하는 데 유용
- variable-list의 각 정의된 변수에 대해, 결과 문자열에 "/"를 추가한 다음 unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
- "." 대신 "/"가 대체되는 것 외에는 Label 확장의 과정과 동일 → 단, "."와 달리 "/"는 reserved 문자이므로 값에서 발견되면 퍼센트 인코딩됨
- 예시
  - `{/who}` → `/fred`
  - `{/who,who}` → `/fred/fred`
  - `{/half,who}` → `/50%25/fred`
  - `{/who,dub}` → `/fred/me%2Ftoo`
  - `{/var}` → `/value`
  - `{/var,empty}` → `/value/`
  - `{/var,undef}` → `/value`
  - `{/var,x}/here` → `/value/1024/here`
  - `{/var:1,var}` → `/v/value`
  - `{/list}` → `/red,green,blue`
  - `{/list*}` → `/red/green/blue`
  - `{/list*,path:4}` → `/red/green/blue/%2Ffoo`
  - `{/keys}` → `/semi,%3B,dot,.,comma,%2C`
  - `{/keys*}` → `/semi=%3B/dot=./comma=%2C`

##### 3.2.7. 경로 스타일 파라미터 확장: {;var}

- Level 3 이상 template의 세미콜론(";") 연산자로 표시되는 경로 스타일 파라미터 확장: "path;property" 또는 "path;name=value"와 같은 URI 경로 파라미터를 기술하는 데 유용
- variable-list의 각 정의된 변수에 대해
  - 결과 문자열에 ";"를 추가
  - 변수가 단순 문자열 값을 가지거나 전개 수정자가 주어지지 않은 경우
    - 변수 이름을(리터럴 문자열인 것처럼 인코딩하여) 결과 문자열에 추가
    - 변수의 값이 비어 있지 않으면 결과 문자열에 "=" 추가
  - unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
- 예시
  - `{;who}` → `;who=fred`
  - `{;half}` → `;half=50%25`
  - `{;empty}` → `;empty`
  - `{;v,empty,who}` → `;v=6;empty;who=fred`
  - `{;v,bar,who}` → `;v=6;who=fred`
  - `{;x,y}` → `;x=1024;y=768`
  - `{;x,y,empty}` → `;x=1024;y=768;empty`
  - `{;x,y,undef}` → `;x=1024;y=768`
  - `{;hello:5}` → `;hello=Hello`
  - `{;list}` → `;list=red,green,blue`
  - `{;list*}` → `;list=red;list=green;list=blue`
  - `{;keys}` → `;keys=semi,%3B,dot,.,comma,%2C`
  - `{;keys*}` → `;semi=%3B;dot=.;comma=%2C`

##### 3.2.8. 폼 스타일 쿼리 확장: {?var}

- Level 3 이상 template의 물음표("?") 연산자로 표시되는 폼 스타일 쿼리 확장: 전체 선택적 쿼리 구성 요소를 기술하는 데 유용
- variable-list의 각 정의된 변수에 대해
  - 첫 번째 정의된 값이면 결과 문자열에 "?"를 추가, 그 이후에는 "&" 추가
  - 변수가 단순 문자열 값을 가지거나 전개 수정자가 주어지지 않은 경우, 변수 이름(리터럴 문자열인 것처럼 인코딩)과 등호 문자("=")를 결과 문자열에 추가
  - unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
- 예시
  - `{?who}` → `?who=fred`
  - `{?half}` → `?half=50%25`
  - `{?x,y}` → `?x=1024&y=768`
  - `{?x,y,empty}` → `?x=1024&y=768&empty=`
  - `{?x,y,undef}` → `?x=1024&y=768`
  - `{?var:3}` → `?var=val`
  - `{?list}` → `?list=red,green,blue`
  - `{?list*}` → `?list=red&list=green&list=blue`
  - `{?keys}` → `?keys=semi,%3B,dot,.,comma,%2C`
  - `{?keys*}` → `?semi=%3B&dot=.&comma=%2C`

##### 3.2.9. 폼 스타일 쿼리 연속: {&var}

- Level 3 이상 template의 앰퍼샌드("&") 연산자로 표시되는 폼 스타일 쿼리 연속: 고정 파라미터를 가진 리터럴 쿼리 구성 요소가 이미 포함된 template에서 선택적 &name=value 쌍을 기술하는 데 유용
- variable-list의 각 정의된 변수에 대해
  - 결과 문자열에 "&"를 추가
  - 변수가 단순 문자열 값을 가지거나 전개 수정자가 주어지지 않은 경우, 변수 이름(리터럴 문자열인 것처럼 인코딩)과 등호 문자("=")를 결과 문자열에 추가
  - unreserved 집합의 문자를 허용 문자로 하여 섹션 3.2.1에 정의된 대로 변수 확장 수행
- 예시
  - `{&who}` → `&who=fred`
  - `{&half}` → `&half=50%25`
  - `?fixed=yes{&x}` → `?fixed=yes&x=1024`
  - `{&x,y,empty}` → `&x=1024&y=768&empty=`
  - `{&x,y,undef}` → `&x=1024&y=768`
  - `{&var:3}` → `&var=val`
  - `{&list}` → `&list=red,green,blue`
  - `{&list*}` → `&list=red&list=green&list=blue`
  - `{&keys}` → `&keys=semi,%3B,dot,.,comma,%2C`
  - `{&keys*}` → `&semi=%3B&dot=.&comma=%2C`

### 4. 보안 고려 사항

- URI Template은 능동적이거나 실행 가능한 콘텐츠를 포함하지 않음
- 단, 공격자가 template 자체 또는 확장에서 reserved 문자를 허용하는 표현식 내 변수 값에 대한 통제권을 부여받는 경우 예상치 못한 URI를 만들어낼 수 있음
  - 어느 경우든 보안 고려사항은 누가 template을 제공하는지 · 누가 변수 값을 제공하는지 · 확장이 어떤 실행 컨텍스트(클라이언트 또는 서버)에서 발생하는지 · 결과 URI가 어디에서 사용되는지에 의해 크게 결정
- 이 명세는 URI Template이 사용될 수 있는 장소를 제한하지 않음
  - 현재 구현: 서버 측 개발 프레임워크 · 계산된 링크·폼을 위한 클라이언트 측 JavaScript
- 프레임워크 내에서 template은 일반적으로 나중의(요청 시) 클라이언트 요청 URI에서 데이터가 발생할 수 있는 위치에 대한 안내 역할
  - 보안 우려는 template 자체가 아니라 서버가 일반적인 웹 요청 내에서 사용자 제공 데이터를 추출·처리하는 방법에 있음
- 클라이언트 측 구현에서 URI Template은 HTML 폼과 많은 동일 속성을 가지지만, URI 문자로 제한되고 메시지 본문 콘텐츠뿐만 아니라 HTTP 헤더 필드 값에도 포함 가능
  - "javascript:"로 시작하는 것과 같은 잠재적으로 위험한 URI 참조 문자열이, template과 값 모두 신뢰할 수 있는 소스에서 제공되지 않는 한 확장에 나타나지 않도록 주의 필요
- 기타 보안 고려사항: [RFC3986] 섹션 7에 설명된 URI에 대한 것과 동일

### 5. 감사의 글

- 다음 분들이 이 명세에 기여: Mike Burrows, Michaeljohn Clement, DeWitt Clinton, John Cowan, Stephen Farrell, Robbie Gates, Vijay K. Gurbani, Peter Johanson, Murray S. Kucherawy, James H. Manger, Tom Petch, Marc Portier, Pete Resnick, James Snell, Jiankang Yao

### 6. 참고 문헌

#### 6.1. 규범적 참고 문헌

```
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
```

#### 6.2. 정보적 참고 문헌

```
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
```

### 부록 A. 구현 힌트

- 확장에 대한 규범적 섹션은 서술적 명확성을 위해 각 연산자를 별도의 확장 과정으로 기술
- 실제 구현에서는 표현식이 왼쪽에서 오른쪽으로, 연산자별로 약간의 과정 변형만 있는 공통 알고리즘을 사용해 처리될 것으로 기대
- 이 비규범적 부록은 그러한 알고리즘 중 하나를 기술

- 빈 결과 문자열과 비오류 상태를 초기화
- template 문자열을 스캔하고 리터럴을 결과 문자열에 복사(섹션 3.1에 따라)
  - "{"에 의해 표현식이 표시되거나, "{" 이외의 비리터럴 문자의 존재에 의해 오류가 표시되거나, template이 끝날 때까지 계속
  - template이 끝나면 → 결과 문자열과 현재 오류/비오류 상태를 반환
  - 표현식이 발견되면 → 다음 "}"까지 template을 스캔하고 중괄호 사이의 문자를 추출
    - "}" 전에 template이 끝나면 → "{"와 추출된 문자를 결과 문자열에 추가하고 오류 상태(표현식 잘못됨)와 함께 반환

- 추출된 표현식의 첫 번째 문자에서 연산자를 확인
  - 표현식이 끝났거나(즉 "{}"인 경우), 알려지지 않거나 구현되지 않은 연산자가 발견되거나, 문자가 varchar 집합(섹션 2.3)에 포함되지 않는 경우
    - "{", 추출된 표현식, "}"를 결과 문자열에 추가
    - 결과가 오류 상태임을 기억
    - template의 나머지를 스캔하러 돌아감
  - 알려지고 구현된 연산자가 발견되면 → 연산자를 저장하고 varspec-list를 시작하기 위해 다음 문자로 건너뜀
  - 그렇지 않으면 → 연산자를 NUL(단순 문자열 확장)로 저장

- 다음 값 표를 사용하여 표현식 유형 연산자별 처리 동작을 결정
  - "first": 표현식의 변수 중 하나라도 정의된 경우 결과에 먼저 추가할 문자열
  - "sep": 두 번째(또는 이후의) 정의된 변수 확장 전에 결과에 추가할 구분자
  - "named": 전개 수정자가 주어지지 않을 때 확장이 변수 또는 키 이름을 포함하는지 여부(불리언)
  - "ifemp": 해당 값이 비어 있을 때 이름에 추가할 문자열
  - "allow": 값 확장 내에서 인코딩되지 않은 채 허용할 문자
    - `U`: unreserved 집합에 없는 모든 문자가 인코딩됨
    - `U+R`: (unreserved / reserved / pct-encoding)의 합집합에 없는 모든 문자가 인코딩됨
    - 두 경우 모두, 허용되지 않는 각 문자는 먼저 UTF-8의 옥텟 시퀀스로 인코딩된 다음 각 옥텟이 퍼센트 인코딩 삼중항으로 인코딩됨

  - NUL(단순 문자열 확장)
    - first: `""` · sep: `,` · named: false · ifemp: `""` · allow: U
  - `+`
    - first: `""` · sep: `,` · named: false · ifemp: `""` · allow: U+R
  - `.`
    - first: `.` · sep: `.` · named: false · ifemp: `""` · allow: U
  - `/`
    - first: `/` · sep: `/` · named: false · ifemp: `""` · allow: U
  - `;`
    - first: `;` · sep: `;` · named: true · ifemp: `""` · allow: U
  - `?`
    - first: `?` · sep: `&` · named: true · ifemp: `=` · allow: U
  - `&`
    - first: `&` · sep: `&` · named: true · ifemp: `=` · allow: U
  - `#`
    - first: `#` · sep: `,` · named: false · ifemp: `""` · allow: U+R

- 위 표를 염두에 두고, variable-list를 다음과 같이 처리

- 각 varspec에 대해, 표현식에서 varchar 집합에 포함되지 않는 문자가 발견되거나 표현식의 끝에 도달할 때까지 variable-list를 스캔하여 변수 이름과 선택적 수정자를 추출
  - 표현식의 끝이고 varname이 비어 있으면 → template의 나머지를 스캔하러 돌아감
  - 표현식의 끝이 아니고 마지막으로 발견된 문자가 수정자("*" 또는 ":")를 나타내면
    - 해당 수정자를 기억
    - 전개("*")이면 → 다음 문자를 스캔
    - 접두사(":")이면 → 10진 정수로 표현된 max-length를 위해 다음 1~4개의 문자를 계속 스캔한 다음, 표현식의 끝이 아니면 다음 문자를 스캔
  - 표현식의 끝이 아니고 마지막으로 발견된 문자가 쉼표(",")가 아니면
    - "{", 저장된 연산자(있는 경우), 스캔된 varname과 수정자, 나머지 표현식, "}"를 결과 문자열에 추가
    - 결과가 오류 상태임을 기억
    - template의 나머지를 스캔하러 돌아감

- 스캔된 변수 이름에 대한 값을 조회한 다음
  - varname이 알려지지 않았거나 정의되지 않은 값(섹션 2.3)을 가진 변수에 해당하면 → 다음 varspec으로 건너뜀
  - 이것이 이 표현식의 첫 번째 정의된 변수이면 → 이 표현식 유형의 first 문자열을 결과 문자열에 추가하고 완료되었음을 기억, 그렇지 않으면 → sep 문자열을 결과 문자열에 추가
  - 이 변수의 값이 문자열이면
    - named가 true이면 → 리터럴과 동일한 인코딩 과정으로 varname을 결과 문자열에 추가하고
      - 값이 비어 있으면 → ifemp 문자열을 결과 문자열에 추가하고 다음 varspec으로 건너뜀
      - 그렇지 않으면 → 결과 문자열에 "=" 추가
    - 접두사 수정자가 있고 접두사 길이가 Unicode 문자 수 기준으로 값 문자열 길이보다 작으면 → 값 문자열의 시작 부분에서 해당 수의 문자를 allow 집합에 없는 문자를 퍼센트 인코딩한 후 결과 문자열에 추가 (단일 Unicode 코드 포인트를 나타내는 다중 옥텟 또는 퍼센트 인코딩 삼중항 문자를 분할하지 않도록 주의)
    - 그렇지 않으면 → allow 집합에 없는 문자를 퍼센트 인코딩한 후 값을 결과 문자열에 추가
  - 전개 수정자가 주어지지 않은 경우
    - named가 true이면 → 리터럴과 동일한 인코딩 과정으로 varname을 결과 문자열에 추가하고
      - 값이 비어 있으면 → ifemp 문자열을 결과 문자열에 추가하고 다음 varspec으로 건너뜀
      - 그렇지 않으면 → 결과 문자열에 "=" 추가
    - 이 변수의 값이 목록이면 → 정의된 각 목록 구성원을 allow 집합에 없는 문자를 퍼센트 인코딩한 후 결과 문자열에 추가 (정의된 각 목록 구성원 사이에 쉼표(",") 추가)
    - 이 변수의 값이 연관 배열 또는 기타 형태의 (name, value) 쌍 구조이면 → 정의된 값을 가진 각 쌍을 allow 집합에 없는 문자를 퍼센트 인코딩한 후 "name,value"로 결과 문자열에 추가 (정의된 각 쌍 사이에 쉼표(",") 추가)
  - 전개 수정자가 주어진 경우
    - named가 true이면 → 정의된 각 목록 구성원 또는 정의된 값을 가진 배열(name, value) 쌍에 대해
      - 첫 번째 정의된 구성원/값이 아니면 → sep 문자열을 결과 문자열에 추가
      - 목록이면 → 리터럴과 동일한 인코딩 과정으로 varname을 결과 문자열에 추가
      - 쌍이면 → 리터럴과 동일한 인코딩 과정으로 name을 결과 문자열에 추가
      - 구성원/값이 비어 있으면 → ifemp 문자열을 결과 문자열에 추가, 그렇지 않으면 → "="와 구성원/값을 allow 집합에 없는 구성원/값 문자를 퍼센트 인코딩한 후 결과 문자열에 추가
    - named가 false이면
      - 목록이면 → 정의된 각 목록 구성원을 allow 집합에 없는 문자를 퍼센트 인코딩한 후 결과 문자열에 추가 (정의된 각 목록 구성원 사이에 sep 문자열 추가)
      - (name, value) 쌍의 배열이면 → 정의된 값을 가진 각 쌍을 allow 집합에 없는 문자를 퍼센트 인코딩한 후 "name=value"로 결과 문자열에 추가 (정의된 각 쌍 사이에 sep 문자열 추가)

- 이 표현식의 variable-list가 소진되면 → template의 나머지를 스캔하러 돌아감

### 저자 주소

```
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
```
