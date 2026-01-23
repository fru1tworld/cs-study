# RFC 6570: URI Template

> URI 템플릿: 변수 확장을 통해 URI 범위를 기술하기 위한 문법

## 문서 정보

| 항목 | 내용 |
|------|------|
| **RFC 번호** | 6570 |
| **분류** | Standards Track (표준 제안) |
| **작성자** | J. Gregorio (Google), R. Fielding (Adobe), M. Hadley (MITRE), M. Nottingham (Rackspace), D. Orchard (Salesforce.com) |
| **발행일** | 2012년 3월 |
| **상태** | 현행 표준 |

---

## 개요

**URI 템플릿**은 변수 확장을 통해 Uniform Resource Identifier(URI)의 범위를 기술하기 위한 간결한 문자 시퀀스입니다. URI 템플릿은 리소스 식별자 공간을 추상화하여 가변적인 부분을 쉽게 식별하고 동적으로 채울 수 있게 합니다.

### 핵심 개념

| 용어 | 정의 |
|------|------|
| **표현식 (Expression)** | 중괄호('{' 와 '}') 사이의 텍스트 (구분자 포함) |
| **확장 (Expansion)** | 표현식을 처리한 후 생성되는 결과 문자열 |
| **템플릿 프로세서 (Template Processor)** | 표현식을 계산된 값으로 치환하여 템플릿을 URI 참조로 변환하는 소프트웨어 |

### 사용 사례

- **서비스 발견**: 서비스 엔드포인트의 동적 구성
- **리소스 매핑 구성**: API 라우팅 정의
- **프로그래밍적 리소스 상호작용**: 클라이언트 라이브러리에서의 URL 생성

---

## 1. 소개

### 1.1 개요

URI 템플릿은 표현식을 해당 유형과 변수 값에 따라 확장하여 URI로 변환됩니다.

**기본 예시:**
```
템플릿: http://example.com/~{username}/
변수: username = "fred"
결과: http://example.com/~fred/
```

**폼 스타일 쿼리 예시:**
```
템플릿: http://www.example.com/foo{?query,number}
변수: query = "mycelium", number = 100
결과: http://www.example.com/foo?query=mycelium&number=100
```

### 1.2 레벨과 표현식 유형

URI 템플릿은 4개의 복잡도 레벨로 구분됩니다:

#### 레벨 1: 단순 문자열 확장

기본적인 변수 치환으로, 예약되지 않은 문자에 대해 퍼센트 인코딩을 수행합니다.

```
예시 변수:
  var   := "value"
  hello := "Hello World!"

{var}    → value
{hello}  → Hello%20World%21
```

#### 레벨 2: 예약 문자 및 프래그먼트 확장

예약 문자를 위한 연산자(+)와 프래그먼트를 위한 연산자(#)를 도입합니다.

```
예시 변수:
  var   := "value"
  hello := "Hello World!"
  path  := "/foo/bar"

{+var}       → value
{+hello}     → Hello%20World!
{+path}/here → /foo/bar/here
{#var}       → #value
{#hello}     → #Hello%20World!
```

#### 레벨 3: 복합 연산자

표현식당 여러 변수를 허용하고 특화된 연산자들을 도입합니다.

```
예시 변수:
  var   := "value"
  x     := "1024"
  y     := "768"
  empty := ""
  path  := "/foo/bar"
  who   := "fred"
  dom   := ["example", "com"]

문자열 확장 (다중 변수):
{x,y}            → 1024,768
{x,hello,y}      → 1024,Hello%20World%21,768

예약 확장 (다중 변수):
{+x,hello,y}     → 1024,Hello%20World!,768
{+path,x}/here   → /foo/bar,1024/here

프래그먼트 확장 (다중 변수):
{#x,hello,y}     → #1024,Hello%20World!,768
{#path,x}/here   → #/foo/bar,1024/here

레이블 확장 (점-접두사):
X{.var}          → X.value
X{.x,y}          → X.1024.768

경로 세그먼트:
{/var}           → /value
{/var,x}/here    → /value/1024/here

경로 스타일 파라미터:
{;x,y}           → ;x=1024;y=768
{;x,y,empty}     → ;x=1024;y=768;empty

폼 스타일 쿼리:
{?x,y}           → ?x=1024&y=768
{?x,y,empty}     → ?x=1024&y=768&empty=

폼 스타일 쿼리 연속:
?fixed=yes{&x}   → ?fixed=yes&x=1024
{&x,y,empty}     → &x=1024&y=768&empty=
```

#### 레벨 4: 값 수정자

두 가지 수정자 유형을 추가합니다:

- **접두사 수정자 (`:length`)**: 확장을 지정된 문자 수로 제한
- **폭발 수정자 (`*`)**: 복합 값(리스트/배열)을 개별 변수로 처리

```
예시 변수:
  var   := "value"
  hello := "Hello World!"
  path  := "/foo/bar"
  list  := ["red", "green", "blue"]
  keys  := [("semi", ";"), ("dot", "."), ("comma", ",")]

접두사 수정자:
{var:3}          → val
{var:30}         → value
{+path:6}/here   → /foo/b/here
{#path:6}/here   → #/foo/b/here

폭발 수정자 (리스트):
{list}           → red,green,blue
{list*}          → red,green,blue
{+list}          → red,green,blue
{+list*}         → red,green,blue
{#list}          → #red,green,blue
{#list*}         → #red,green,blue
{.list}          → .red,green,blue
{.list*}         → .red.green.blue
{/list}          → /red,green,blue
{/list*}         → /red/green/blue
{;list}          → ;list=red,green,blue
{;list*}         → ;list=red;list=green;list=blue
{?list}          → ?list=red,green,blue
{?list*}         → ?list=red&list=green&list=blue
{&list*}         → &list=red&list=green&list=blue

폭발 수정자 (연관 배열):
{keys}           → semi,%3B,dot,.,comma,%2C
{keys*}          → semi=%3B,dot=.,comma=%2C
{+keys}          → semi,;,dot,.,comma,,
{+keys*}         → semi=;,dot=.,comma=,
{#keys}          → #semi,;,dot,.,comma,,
{#keys*}         → #semi=;,dot=.,comma=,
{.keys}          → .semi,%3B,dot,.,comma,%2C
{.keys*}         → .semi=%3B.dot=..comma=%2C
{/keys}          → /semi,%3B,dot,.,comma,%2C
{/keys*}         → /semi=%3B/dot=./comma=%2C
{;keys}          → ;keys=semi,%3B,dot,.,comma,%2C
{;keys*}         → ;semi=%3B;dot=.;comma=%2C
{?keys}          → ?keys=semi,%3B,dot,.,comma,%2C
{?keys*}         → ?semi=%3B&dot=.&comma=%2C
{&keys*}         → &semi=%3B&dot=.&comma=%2C
```

### 1.3 설계 고려사항

URI 템플릿 명세는 강력한 확장 메커니즘과 구현의 단순성 사이에서 균형을 맞춥니다:

- 템플릿은 **파싱이 매우 쉬움**
- 단일 문자 연산자는 **URI 구문 구분자와 일치**
- 변수가 정의되지 않은 경우 **구분자 생략**
- 쉼표로 여러 값 구분
- "+" 및 "#" 연산자는 **인코딩되지 않은 예약 문자 보존**
- 접두사 하위 문자열로 **큰 식별자 공간 분할** 지원

### 1.4 제한사항

| 제한사항 | 설명 |
|----------|------|
| 상위집합 기술 | URI 템플릿은 식별자의 상위집합을 기술함 - 모든 확장이 기존 리소스에 대응하지는 않음 |
| URI 아님 | 템플릿 자체는 URI가 아니며, 프로세서가 확장할 것이 아니라면 실제 URI가 예상되는 곳에 나타나서는 안 됨 |
| 역매칭 제한 | 기존 URI에서 변수를 추출하는 역매칭은 구분된 표현식이나 예약 문자가 단순 표현식을 구분하는 경우에만 작동 |

### 1.5 표기법 및 규칙

이 명세는 RFC 2119의 키워드(MUST, SHOULD 등)와 RFC 5234의 ABNF 표기법을 사용합니다.

**문자 집합 정의:**

| 집합 | 문자 |
|------|------|
| **예약되지 않은 문자 (unreserved)** | 문자, 숫자, `-`, `.`, `_`, `~` |
| **예약 문자 (reserved)** | 일반 구분자와 하위 구분자 |
| **일반 구분자 (gen-delims)** | `:`, `/`, `?`, `#`, `[`, `]`, `@` |
| **하위 구분자 (sub-delims)** | `!`, `$`, `&`, `'`, `(`, `)`, `*`, `+`, `,`, `;`, `=` |

### 1.6 문자 인코딩과 유니코드 정규화

| 요구사항 | 설명 |
|----------|------|
| **처리 시** | 템플릿은 확장 중 유니코드 코드 포인트 시퀀스로 처리됨 |
| **NFC 정규화** | 사용자 제공 값은 NFC 형식으로 정규화되어야 함(SHOULD) |
| **UTF-8 인코딩** | 비 ASCII 데이터는 퍼센트 인코딩 전에 먼저 UTF-8로 인코딩되어야 함(MUST) |

---

## 2. 구문

### 2.1 리터럴

표현식 외부의 문자는 URI 구문에서 허용되는 경우 출력 URI에 그대로 복사됩니다. 허용되지 않는 문자는 UTF-8 옥텟 시퀀스로 인코딩된 후 퍼센트 인코딩 트리플렛으로 변환됩니다.

**허용되는 리터럴 문자:**
- 영숫자
- 구두점 (중괄호와 같은 특수 템플릿 문자 제외)
- 제어 문자 범위 외의 유니코드 문자

**ABNF:**
```
literals = %x21 / %x23-24 / %x26 / %x28-3B / %x3D / %x3F-5B
         / %x5D / %x5F / %x61-7A / %x7E / ucschar / iprivate
         / pct-encoded
```

"CTL, SP, DQUOTE, 아포스트로피, 퍼센트(pct-encoded 외부), 꺾쇠괄호, 백슬래시, 캐럿, 백틱, 중괄호, 파이프를 제외한 모든 유니코드 문자"가 허용됩니다.

### 2.2 표현식

표현식은 `{[연산자] 변수-목록}` 형식을 사용합니다.

**ABNF:**
```
expression    = "{" [ operator ] variable-list "}"
operator      = op-level2 / op-level3 / op-reserve
op-level2     = "+" / "#"
op-level3     = "." / "/" / ";" / "?" / "&"
op-reserve    = "=" / "," / "!" / "@" / "|"
```

| 연산자 유형 | 연산자 | 용도 |
|------------|--------|------|
| 레벨 2 | `+` | 예약 문자 확장 |
| 레벨 2 | `#` | 프래그먼트 확장 |
| 레벨 3 | `.` | 레이블 (점-접두사) |
| 레벨 3 | `/` | 경로 세그먼트 |
| 레벨 3 | `;` | 경로 파라미터 |
| 레벨 3 | `?` | 폼 스타일 쿼리 |
| 레벨 3 | `&` | 쿼리 연속 |
| 예약됨 | `=`, `,`, `!`, `@`, `\|` | 향후 사용을 위해 예약 |

**참고:** 달러 기호와 괄호는 매크로 언어와의 호환성을 위해 의도적으로 제외되었습니다.

### 2.3 변수

**ABNF:**
```
variable-list = varspec *( "," varspec )
varspec       = varname [ modifier-level4 ]
varname       = varchar *( ["."] varchar )
varchar       = ALPHA / DIGIT / "_" / pct-encoded
```

**변수 규칙:**

| 규칙 | 설명 |
|------|------|
| **대소문자 구분** | 변수 이름은 대소문자를 구분함 |
| **허용 문자** | 문자, 숫자, 밑줄, 퍼센트 인코딩 트리플렛 |
| **정의되지 않은 변수** | null/undef 값은 확장 시 특별 처리 |
| **빈 문자열** | 빈 문자열은 정의되지 않은 것이 아니라 정의된 것임 |
| **복합 값** | 리스트나 연관 배열은 확장 동작에 영향을 미침 |
| **빈 리스트/배열** | 모든 값이 정의되지 않은 빈 리스트나 배열은 정의되지 않은 것으로 간주 |

### 2.4 값 수정자

**ABNF:**
```
modifier-level4 = prefix / explode
prefix          = ":" max-length
max-length      = %x31-39 0*3DIGIT
explode         = "*"
```

#### 2.4.1 접두사 수정자

**접두사 값 (:max-length):**
- 확장을 값 시작부터 지정된 문자 수로 제한
- max-length는 10000 미만의 양의 정수여야 함
- 복합 값에는 적용되지 않음
- 멀티바이트 문자 분할을 피하기 위해 옥텟이 아닌 유니코드 문자를 계산

```
예시:
  var := "value"

{var:3}  → val
{var:30} → value
```

#### 2.4.2 폭발 수정자

**폭발 값 (*):**
- 변수를 리스트나 연관 배열로 처리
- 폭발된 변수는 멤버를 개별적으로 확장
- 복잡한 데이터 구조를 처리할 때 템플릿 간결성 향상

```
예시:
  list := ["red", "green", "blue"]

폭발 없이:
{list}   → red,green,blue

폭발 사용:
{list*}  → red,green,blue
{/list*} → /red/green/blue
{?list*} → ?list=red&list=green&list=blue
```

---

## 3. 확장

### 3.1 리터럴 확장

URI 템플릿의 표현식 외부 리터럴 문자는 신중하게 처리되어야 합니다:

- 문자가 URI 구문의 어디에서나 허용되는 경우 (unreserved / reserved / pct-encoded), 결과 문자열에 직접 복사됩니다.
- URI에서 허용되지 않는 문자의 경우, 템플릿 프로세서는 이를 인코딩해야 합니다. 이는 문자를 UTF-8 옥텟으로 변환한 후 각 옥텟을 퍼센트 인코딩 트리플렛으로 표현하는 것을 포함합니다.

### 3.2 표현식 확장

확장 프로세스는 템플릿 문자열을 처음부터 끝까지 스캔하여 리터럴을 복사하고 표현식을 계산된 값으로 대체합니다.

각 표현식은 여는 중괄호 ("{") 문자로 표시되며 다음 닫는 중괄호 ("}")까지 계속됩니다.

표현식 유형은 여는 중괄호 다음의 첫 번째 문자를 확인하여 결정됩니다. 연산자인 경우 확장 유형을 정의하고, 그렇지 않으면 단순 문자열 확장이 적용됩니다.

#### 연산자 동작 참조표

| 연산자 | 용도 | 첫 문자열 | 구분자 | named | ifemp | 허용 |
|--------|------|----------|--------|-------|-------|------|
| (없음) | 단순 확장 | "" | "," | false | "" | U |
| + | 예약 확장 | "" | "," | false | "" | U+R |
| # | 프래그먼트 | "#" | "," | false | "" | U+R |
| . | 레이블 | "." | "." | false | "" | U |
| / | 경로 세그먼트 | "/" | "/" | false | "" | U |
| ; | 파라미터 | ";" | ";" | true | "" | U |
| ? | 쿼리 | "?" | "&" | true | "=" | U |
| & | 쿼리 연속 | "&" | "&" | true | "=" | U |

*참고: U = 예약되지 않은 문자만; U+R = 예약되지 않은 문자 + 예약 문자 + pct-encoded*

#### 3.2.1 변수 확장

**정의되지 않은 변수:**
- 정의되지 않은 변수는 특별 처리를 받음 - 완전히 무시됨
- 표현식의 모든 변수가 정의되지 않은 경우, 결과는 빈 문자열

**인코딩 전략:**
- **예약 및 프래그먼트 확장**: unreserved, reserved, pct-encoded 문자를 인코딩하지 않고 허용
- **기타 유형**: unreserved 문자만 인코딩하지 않고 허용

**리스트 확장 예시:**
```
예시 변수:
  count := ["one", "two", "three"]

{count}    → one,two,three
{count*}   → one,two,three
{/count*}  → /one/two/three
{?count*}  → ?count=one&count=two&count=three
```

#### 3.2.2 단순 문자열 확장: {var}

연산자가 지정되지 않은 경우의 기본값입니다. 변수 목록의 각 정의된 변수에 대해 unreserved 집합의 문자를 허용하여 변수 확장을 수행합니다.

여러 변수는 쉼표로 결합됩니다.

```
예시 변수:
  var   := "value"
  hello := "Hello World!"
  x     := "1024"
  y     := "768"
  list  := ["red", "green", "blue"]
  keys  := [("semi", ";"), ("dot", "."), ("comma", ",")]

{var}    → value
{hello}  → Hello%20World%21
{x,y}    → 1024,768
{list}   → red,green,blue
{list*}  → red,green,blue
{keys*}  → semi=%3B,dot=.,comma=%2C
```

#### 3.2.3 예약 확장: {+var}

플러스 연산자는 확장에서 예약 문자를 허용합니다. 예약 확장은 대체된 값이 pct-encoded 트리플렛과 reserved 집합의 문자도 포함할 수 있다는 점을 제외하면 단순 문자열 확장과 동일합니다.

```
예시 변수:
  var   := "value"
  hello := "Hello World!"
  path  := "/foo/bar"
  list  := ["red", "green", "blue"]
  keys  := [("semi", ";"), ("dot", "."), ("comma", ",")]

{+var}       → value
{+hello}     → Hello%20World!
{+path}/here → /foo/bar/here
{+list}      → red,green,blue
{+keys*}     → semi=;,dot=.,comma=,
```

#### 3.2.4 프래그먼트 확장: {#var}

프래그먼트 확장은 예약 확장과 유사하지만 변수가 정의된 경우 "#" 문자를 앞에 붙입니다.

```
예시 변수:
  var   := "value"
  hello := "Hello World!"
  x     := "1024"
  y     := "768"
  list  := ["red", "green", "blue"]

{#var}       → #value
{#hello}     → #Hello%20World!
{#x,hello,y} → #1024,Hello%20World!,768
{#list*}     → #red,green,blue
```

#### 3.2.5 레이블 확장 (점-접두사): {.var}

이 연산자는 점 구분자를 추가하며, 도메인 이름과 파일 확장자에 유용합니다. 변수 목록의 각 정의된 변수에 대해 결과 문자열에 "."를 추가한 다음 unreserved 집합의 문자를 허용하여 변수 확장을 수행합니다.

```
예시 변수:
  who  := "fred"
  dom  := ["example", "com"]
  list := ["red", "green", "blue"]
  keys := [("semi", ";"), ("dot", "."), ("comma", ",")]

{.who}       → .fred
www{.dom*}   → www.example.com
X{.list*}    → X.red.green.blue
X{.keys*}    → X.semi=%3B.dot=..comma=%2C
```

#### 3.2.6 경로 세그먼트 확장: {/var}

슬래시 연산자는 경로 계층을 생성합니다. 변수 목록의 각 정의된 변수에 대해 결과 문자열에 "/"를 추가한 다음 unreserved 집합의 문자를 허용하여 변수 확장을 수행합니다.

**참고:** 값 내의 슬래시는 레이블 확장의 점과 달리 퍼센트 인코딩됩니다.

```
예시 변수:
  who   := "fred"
  var   := "value"
  empty := ""
  list  := ["red", "green", "blue"]
  keys  := [("semi", ";"), ("dot", "."), ("comma", ",")]

{/who}          → /fred
{/who,who}      → /fred/fred
{/var,empty}    → /value/
{/list*}        → /red/green/blue
{/keys*}        → /semi=%3B/dot=./comma=%2C
```

#### 3.2.7 경로 스타일 파라미터 확장: {;var}

세미콜론 접두사 파라미터는 특정 패턴을 따릅니다. 프로세스는 세미콜론, 변수 이름, 그리고 선택적으로 값의 존재 여부에 따라 등호를 추가하는 것을 포함합니다.

변수 목록의 각 정의된 변수에 대해:
- 결과 문자열에 ";"를 추가
- 변수가 단순 문자열 값을 가지거나 폭발 수정자가 없는 경우:
  - 변수 이름(리터럴 문자열인 것처럼 인코딩)을 결과 문자열에 추가
  - 값이 비어있지 않으면 "="와 인코딩된 값을 추가

```
예시 변수:
  who   := "fred"
  empty := ""
  v     := "6"
  list  := ["red", "green", "blue"]
  keys  := [("semi", ";"), ("dot", "."), ("comma", ",")]

{;who}         → ;who=fred
{;empty}       → ;empty
{;v,empty,who} → ;v=6;empty;who=fred
{;list*}       → ;list=red;list=green;list=blue
{;keys*}       → ;semi=%3B;dot=.;comma=%2C
```

#### 3.2.8 폼 스타일 쿼리 확장: {?var}

물음표는 앰퍼샌드로 구분된 name=value 쌍으로 쿼리 구성요소를 시작합니다.

변수 목록의 각 정의된 변수에 대해:
- 첫 번째 정의된 값이면 결과 문자열에 "?"를 추가하고, 이후에는 "&"를 추가
- 변수가 단순 문자열 값을 가지거나 폭발 수정자가 없는 경우, 변수 이름(리터럴 문자열인 것처럼 인코딩)과 등호("=")를 결과 문자열에 추가

```
예시 변수:
  who  := "fred"
  x    := "1024"
  y    := "768"
  list := ["red", "green", "blue"]
  keys := [("semi", ";"), ("dot", "."), ("comma", ",")]

{?who}    → ?who=fred
{?x,y}    → ?x=1024&y=768
{?list*}  → ?list=red&list=green&list=blue
{?keys*}  → ?semi=%3B&dot=.&comma=%2C
```

#### 3.2.9 폼 스타일 쿼리 연속: {&var}

앰퍼샌드 연산자는 기존 쿼리 구성요소를 확장합니다. 폼 스타일 쿼리 연속은 이미 고정된 파라미터가 있는 리터럴 쿼리 구성요소를 포함하는 템플릿에서 선택적 &name=value 쌍을 기술하는 데 유용합니다.

```
예시 변수:
  who   := "fred"
  x     := "1024"
  y     := "768"
  empty := ""
  list  := ["red", "green", "blue"]

{&who}          → &who=fred
?fixed=yes{&x}  → ?fixed=yes&x=1024
{&x,y,empty}    → &x=1024&y=768&empty=
{&list*}        → &list=red&list=green&list=blue
```

---

## 4. 보안 고려사항

URI 템플릿 자체는 실행 가능한 코드를 포함하지 않으므로 최소한의 위험을 제기합니다. 그러나 공격자가 템플릿 구조나 변수 값(특히 예약 문자를 지원하는 값)을 제어할 때 취약점이 발생합니다.

### 주요 보안 고려사항

| 요소 | 설명 |
|------|------|
| **템플릿 소스** | 템플릿 정의를 누가 제공하는가 |
| **값 소스** | 변수 데이터를 누가 제공하는가 |
| **실행 컨텍스트** | 클라이언트 측 또는 서버 측 처리 |
| **URI 사용** | 결과 URI가 어떻게 배포되는가 |

### 구현 컨텍스트

**서버 측 프레임워크:**
- 템플릿을 예상되는 데이터 위치에 대한 구조적 가이드로 처리
- 보안 위험은 템플릿 자체보다 서버가 사용자 제공 요청 데이터를 추출하고 처리하는 방법에 집중됨

**클라이언트 측 구현 (예: JavaScript):**
- HTML 폼과 속성을 공유
- "템플릿과 값이 모두 신뢰할 수 있는 소스에서 제공되지 않는 한, 'javascript:'로 시작하는 것과 같은 잠재적으로 위험한 URI 참조 문자열이 확장에 나타나지 않도록 주의해야 합니다."

**일반 URI 보안:**
- RFC 3986 Section 7의 일반 URI 보안 고려사항도 적용됩니다.

---

## 5. IANA 고려사항

이 문서는 IANA에 새로운 등록을 요청하지 않습니다.

---

## 6. 감사의 글

이 명세에 기여한 분들: Mike Burrows, Michaeljohn Clement, DeWitt Clinton, John Cowan, Stephen Farrell, Robbie Gates, Vijay K. Gurbani, Peter Johanson, Murray S. Kucherawy, James H. Manger, Tom Petch, Marc Portier, Pete Resnick, James Snell, Jiankang Yao.

---

## 7. 참조

### 7.1 규범적 참조

| RFC | 제목 |
|-----|------|
| RFC 2119 | RFC에서 요구사항 수준을 나타내기 위해 사용되는 핵심 단어 |
| RFC 3629 | UTF-8, ISO 10646의 변환 형식 |
| RFC 3986 | Uniform Resource Identifier (URI): 일반 구문 |
| RFC 3987 | Internationalized Resource Identifiers (IRIs) |
| RFC 5234 | 구문 명세를 위한 확장 BNF: ABNF |

### 7.2 참고 참조

| RFC | 제목 |
|-----|------|
| RFC 2616 | Hypertext Transfer Protocol -- HTTP/1.1 |
| RFC 4627 | JSON용 application/json 미디어 타입 |

---

## 부록 A: 구현 힌트

이 명세는 실제 구현을 위한 비규범적 알고리즘을 제공합니다. 각 연산자를 개별적으로 처리하는 대신 개발자는 연산자별 변형이 있는 통합된 왼쪽에서 오른쪽 스캐닝 접근 방식을 사용할 수 있습니다.

### 핵심 알고리즘 단계

1. **빈 결과 문자열을 초기화**하고 리터럴이나 표현식을 스캔합니다.
2. **리터럴 문자를 복사**하다가 "{"를 만나거나, 오류가 발생하거나, 템플릿이 끝날 때까지 계속합니다.
3. 표현식이 시작되면, **중괄호 사이의 문자를 추출**합니다. 닫는 중괄호가 없으면 오류 상태로 잘못된 표현식을 추가합니다.
4. **추출된 첫 번째 문자에서 연산자를 확인**합니다. 알 수 없는 연산자, 빈 표현식, 또는 유효하지 않은 문자는 오류 처리를 트리거합니다.
5. 각 변수에 대해:
   - **이름과 수정자(폭발 "*" 또는 접두사 ":")를 추출**
   - **정의되지 않은 변수는 다음 변수로 건너뛰어 처리**
   - 첫 번째 정의된 변수에서 **연산자의 "first" 문자열을 추가**
   - 이후 변수에서는 **구분자를 추가**
6. "named" 플래그가 설정된 문자열 값의 경우, **변수 이름을 추가**합니다.
7. 비어있고 "named"가 true이면 **"ifemp" 문자열을 추가**합니다.
8. 그렇지 않으면 **"="와 인코딩된 값을 추가**합니다.

### 접두사 수정자

접두사 수정자는 지정된 유니코드 문자 수로 확장을 제한하여 멀티바이트 시퀀스에서 문자 중간 분할을 피합니다.

### 복합 값

- **폭발 수정자 없이**: 쉼표로 구분된 연결을 추가
- **리스트나 배열에 폭발 수정자 사용**: 연산자별 구분자를 사용하여 멤버를 개별적으로 반복

---

## 부록 B: 전체 예시 테이블

### 테스트 변수 집합

```
var   := "value"
hello := "Hello World!"
empty := ""
path  := "/foo/bar"
x     := "1024"
y     := "768"
list  := ["red", "green", "blue"]
keys  := [("semi", ";"), ("dot", "."), ("comma", ",")]
```

### 레벨 1 예시

| 템플릿 | 확장 결과 |
|--------|----------|
| `{var}` | `value` |
| `{hello}` | `Hello%20World%21` |

### 레벨 2 예시

| 템플릿 | 확장 결과 |
|--------|----------|
| `{+var}` | `value` |
| `{+hello}` | `Hello%20World!` |
| `{+path}/here` | `/foo/bar/here` |
| `{#var}` | `#value` |
| `{#hello}` | `#Hello%20World!` |

### 레벨 3 예시

| 템플릿 | 확장 결과 |
|--------|----------|
| `{x,y}` | `1024,768` |
| `{+x,hello,y}` | `1024,Hello%20World!,768` |
| `{#x,hello,y}` | `#1024,Hello%20World!,768` |
| `{.var}` | `.value` |
| `{.x,y}` | `.1024.768` |
| `{/var}` | `/value` |
| `{/var,x}/here` | `/value/1024/here` |
| `{;x,y}` | `;x=1024;y=768` |
| `{;x,y,empty}` | `;x=1024;y=768;empty` |
| `{?x,y}` | `?x=1024&y=768` |
| `{?x,y,empty}` | `?x=1024&y=768&empty=` |
| `{&x,y,empty}` | `&x=1024&y=768&empty=` |

### 레벨 4 예시 - 접두사 수정자

| 템플릿 | 확장 결과 |
|--------|----------|
| `{var:3}` | `val` |
| `{var:30}` | `value` |
| `{+path:6}/here` | `/foo/b/here` |
| `{#path:6}/here` | `#/foo/b/here` |

### 레벨 4 예시 - 폭발 수정자

| 템플릿 | 확장 결과 |
|--------|----------|
| `{list}` | `red,green,blue` |
| `{list*}` | `red,green,blue` |
| `{+list}` | `red,green,blue` |
| `{+list*}` | `red,green,blue` |
| `{.list}` | `.red,green,blue` |
| `{.list*}` | `.red.green.blue` |
| `{/list}` | `/red,green,blue` |
| `{/list*}` | `/red/green/blue` |
| `{;list}` | `;list=red,green,blue` |
| `{;list*}` | `;list=red;list=green;list=blue` |
| `{?list}` | `?list=red,green,blue` |
| `{?list*}` | `?list=red&list=green&list=blue` |
| `{keys}` | `semi,%3B,dot,.,comma,%2C` |
| `{keys*}` | `semi=%3B,dot=.,comma=%2C` |
| `{+keys}` | `semi,;,dot,.,comma,,` |
| `{+keys*}` | `semi=;,dot=.,comma=,` |
| `{;keys}` | `;keys=semi,%3B,dot,.,comma,%2C` |
| `{;keys*}` | `;semi=%3B;dot=.;comma=%2C` |
| `{?keys}` | `?keys=semi,%3B,dot,.,comma,%2C` |
| `{?keys*}` | `?semi=%3B&dot=.&comma=%2C` |

---

## 참고 자료

- [RFC 6570 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc6570)
- [RFC 6570 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc6570)
- [RFC 3986 - URI 일반 구문](https://datatracker.ietf.org/doc/html/rfc3986)

---

## 관련 RFC

- **RFC 3986**: Uniform Resource Identifier (URI): 일반 구문
- **RFC 3987**: Internationalized Resource Identifiers (IRIs)
- **RFC 2119**: RFC에서 요구사항 수준을 나타내기 위해 사용되는 핵심 단어

---

*이 문서는 RFC 6570의 한국어 번역 및 정리본입니다.*
