# VRL (Vector Remap Language) 완벽 가이드

## 목차

1. [개요](#개요)
2. [VRL의 특징](#vrl의-특징)
3. [기본 문법](#기본-문법)
4. [데이터 타입과 리터럴](#데이터-타입과-리터럴)
5. [변수와 경로](#변수와-경로)
6. [연산자](#연산자)
7. [표현식](#표현식)
8. [함수](#함수)
9. [에러 처리](#에러-처리)
10. [Remap Transform 설정](#remap-transform-설정)
11. [실전 예제](#실전-예제)
12. [VRL 실행 및 테스트](#vrl-실행-및-테스트)

---

## 개요

VRL(Vector Remap Language)은 관측성(Observability) 데이터(로그, 메트릭, 트레이스)를 안전하고 효율적으로 변환하기 위해 설계된 표현식 지향(expression-oriented) 언어입니다. VRL은 원래 Vector에서 사용하기 위해 만들어졌지만, 다양한 컨텍스트에서 재사용 가능하도록 범용적으로 설계되었습니다.

### VRL을 사용하는 이유

- 안전성: 컴파일 타임에 오류를 감지하여 런타임 오류를 방지
- 성능: Rust 네이티브 코드로 컴파일되어 매우 빠른 실행 속도
- 간결함: 관측성 데이터 처리에 최적화된 간단한 문법
- 타입 안전성: 컴파일 타임에 타입 검사 수행

### VRL의 디자인 원칙

VRL은 jq에서 영감을 받아 시작되었으며(`.` 기반 경로 참조), 간단한 멀티라인 언어로 발전했습니다. VRL은 "자유 형식(free-form)" 언어로, 모든 형태의 공백은 토큰을 구분하는 역할만 하며 의미론적 중요성은 없습니다.

---

## VRL의 특징

### 1. Fail Safety (실패 안전성)

VRL의 가장 중요한 특징 중 하나는 실패 안전성(fail safety)입니다. VRL 프로그램은 모든 잠재적 오류가 처리되지 않으면 컴파일되지 않습니다.

```vrl
# 잘못된 예시 - 컴파일 오류 발생
.parsed = parse_json(.message)

# 올바른 예시 - 오류 처리 필수
.parsed = parse_json!(.message)
# 또는
.parsed = parse_json(.message) ?? {}
```

### 2. 네이티브 성능

VRL 프로그램은 Rust 네이티브 코드로 컴파일되어 실행됩니다. 이는 다음을 의미합니다:

- 런타임 없음: 가비지 컬렉션(GC) 일시 중지나 메모리 누적 없음
- FFI 비용 없음: 이벤트마다 외부 함수 인터페이스 변환 비용 없음
- Rust에 가까운 성능: 매우 빠르고 효율적인 실행

### 3. 타입 안전성

VRL은 컴파일 타임에 타입 안전성 검사를 수행하여 타입 불일치로 인한 런타임 오류를 방지합니다.

```vrl
# parse_syslog는 문자열만 받을 수 있음
# 정수를 전달하면 컴파일 오류 발생
.parsed = parse_syslog(.message)  # .message가 문자열이 아니면 오류
```

---

## 기본 문법

### 프로그램 구조

VRL 프로그램은 전적으로 표현식으로 구성되며, 모든 표현식은 값을 반환합니다.

```vrl
# 기본 할당
.field = "value"

# 여러 표현식
.timestamp = now()
.level = "info"
.processed = true
```

### 주석

```vrl
# 한 줄 주석
.field = "value"  # 라인 끝 주석

/*
  여러 줄 주석
  블록 형태로 작성
*/
```

### 세미콜론

표현식은 세미콜론으로 구분되거나 줄바꿈으로 구분될 수 있습니다.

```vrl
# 세미콜론 사용
.field1 = "value1"; .field2 = "value2"

# 줄바꿈 사용
.field1 = "value1"
.field2 = "value2"
```

---

## 데이터 타입과 리터럴

VRL의 리터럴은 다른 대부분의 언어와 마찬가지로 해석되는 대로 정확하게 작성된 값입니다.

### Boolean (불리언)

이진 값을 나타내며 `true` 또는 `false`만 가질 수 있습니다.

```vrl
.is_active = true
.is_deleted = false
```

### Integer (정수)

64비트 부호 있는 정수입니다.

```vrl
.count = 42
.negative = -100
.hex = 0x2A      # 16진수
.octal = 0o52    # 8진수
.binary = 0b101010  # 2진수
```

### Float (부동소수점)

64비트 부동소수점 숫자입니다.

```vrl
.price = 19.99
.scientific = 1.5e10
.negative_exp = 2.5e-3
```

### String (문자열)

UTF-8 인코딩된 문자열입니다.

```vrl
.message = "Hello, World!"
.escaped = "Line1\nLine2\tTabbed"
.raw = s'원시 문자열: \n은 이스케이프되지 않음'

# 문자열 보간 (템플릿 문자열)
.greeting = "Hello, {{.name}}!"
```

#### 이스케이프 시퀀스

| 시퀀스 | 의미 |
|--------|------|
| `\\` | 백슬래시 |
| `\n` | 줄바꿈 |
| `\r` | 캐리지 리턴 |
| `\t` | 탭 |
| `\"` | 큰따옴표 |
| `\0` | 널 문자 |

### Null

값이 없음을 나타냅니다.

```vrl
.optional_field = null
```

### Array (배열)

연속적인 값의 집합입니다. 배열 인덱스는 0부터 시작합니다.

```vrl
.tags = ["error", "critical", "production"]
.mixed = [1, "two", true, null]
.nested = [[1, 2], [3, 4]]

# 배열 접근
.first = .tags[0]       # "error"
.last = .tags[-1]       # "production"
```

### Object (객체)

키-값 쌍의 집합입니다. 올바른 형식의 JSON 문서는 유효한 VRL 객체입니다.

```vrl
.user = {
    "name": "John",
    "age": 30,
    "active": true
}

# 객체 필드 접근
.username = .user.name
```

참고: 객체 필드는 키를 기준으로 알파벳 오름차순으로 정렬됩니다. 따라서 JSON으로 인코딩할 때 키가 알파벳순으로 정렬됩니다.

### Timestamp (타임스탬프)

RFC 3339 형식의 나노초 정밀도 타임스탬프입니다. `t` 시길(sigil)을 사용합니다.

```vrl
.created_at = t'2021-02-11T10:32:50.553955473Z'
.local_time = t'2021-02-11T10:32:50+09:00'
```

### Regular Expression (정규 표현식)

문자열 매칭 및 파싱에 사용되는 정규 표현식입니다. `r` 시길을 사용합니다.

```vrl
.pattern = r'^\d{4}-\d{2}-\d{2}$'
.ip_pattern = r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}'

# 플래그 사용
.case_insensitive = r'(?i)error'     # 대소문자 무시
.multiline = r'(?m)^start'           # 멀티라인 모드
.extended = r'(?x) \d+ \s+ \w+'      # 확장 모드 (공백 무시)
```

#### 정규 표현식 플래그

| 플래그 | 설명 |
|--------|------|
| `i` | 대소문자 무시 |
| `m` | 멀티라인 모드 (^, $가 각 줄의 시작/끝에 매칭) |
| `x` | 확장 모드 (공백 무시, 주석 허용) |

플래그는 조합하여 사용할 수 있습니다: `r'(?ixm)pattern'`

---

## 변수와 경로

### 변수

변수는 문자로 시작하고 문자와 숫자로 구성된 시퀀스입니다.

```vrl
# 변수 선언 및 할당
message = "Hello"
count = 42
is_valid = true

# 변수 사용
.output = message
.total = count + 10
```

### 경로 (Paths)

경로 표현식은 점(.)으로 구분된 세그먼트 시퀀스로, 객체 내의 값 위치를 나타냅니다.

#### 이벤트 경로

선행 `.`는 현재 이벤트를 가리킵니다.

```vrl
# 현재 이벤트 전체
.

# 이벤트의 필드
.message
.user.name
.tags[0]

# 중첩 필드
.request.headers."Content-Type"
```

#### 메타데이터 경로

선행 `%`는 이벤트 메타데이터를 가리킵니다.

```vrl
# 메타데이터 접근
%metadata.source_type
%metadata.host
```

#### 동적 경로

변수를 사용한 동적 경로 접근:

```vrl
field_name = "status"
.result = get(.event, [field_name]) ?? "unknown"
```

#### 경로 예시

```vrl
# 기본 경로
.message                    # 최상위 필드
.user.email                 # 중첩 필드
.items[0]                   # 배열 첫 번째 요소
.items[-1]                  # 배열 마지막 요소
.items[0].name              # 배열 요소의 필드

# 특수 문자가 포함된 필드명
."field-name"               # 하이픈 포함
."field.name"               # 점 포함
."Content-Type"             # 대시 포함
```

---

## 연산자

### 산술 연산자

숫자뿐만 아니라 문자열 등 다른 타입에도 적용할 수 있습니다.

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `+` | 덧셈 / 문자열 연결 | `1 + 2`, `"a" + "b"` |
| `-` | 뺄셈 | `5 - 3` |
| `*` | 곱셈 | `4 * 3` |
| `/` | 나눗셈 | `10 / 2` |
| `%` | 나머지 | `10 % 3` |

```vrl
# 숫자 연산
.sum = 10 + 5          # 15
.diff = 10 - 3         # 7
.product = 4 * 3       # 12
.quotient = 10 / 3     # 3.333...
.remainder = 10 % 3    # 1

# 문자열 연결
.full_name = .first_name + " " + .last_name
```

### 비교 연산자

두 표현식을 비교하여 불리언 값을 생성합니다.

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `==` | 같음 | `.status == 200` |
| `!=` | 같지 않음 | `.status != 404` |
| `<` | 작음 | `.count < 100` |
| `<=` | 작거나 같음 | `.count <= 100` |
| `>` | 큼 | `.count > 0` |
| `>=` | 크거나 같음 | `.count >= 1` |

```vrl
.is_success = .status == 200
.is_error = .status >= 400
.is_empty = .count == 0
```

참고: 정규 표현식 매칭에는 `match` 함수를 사용하세요.

### 논리 연산자

두 표현식을 비교하고 단락 평가(short-circuit)를 수행합니다.

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `&&` | 논리 AND | `.a && .b` |
| `\|\|` | 논리 OR | `.a \|\| .b` |
| `!` | 논리 NOT | `!.flag` |

```vrl
# AND 연산
.is_valid = .status == 200 && .body != null

# OR 연산
.should_alert = .level == "error" || .level == "critical"

# NOT 연산
.is_inactive = !.is_active
```

### 병합 연산자 (Coalesce)

첫 번째 null이 아닌 값을 반환하거나 오류를 처리합니다.

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `??` | Null 병합 | `.value ?? "default"` |
| `\|\|` | 오류/null 병합 | `parse_json(.msg) \|\| {}` |

```vrl
# null 병합
.name = .username ?? .email ?? "anonymous"

# 오류 처리와 함께 사용
.data = parse_json(.message) ?? {}
```

### 할당 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `=` | 할당 | `.field = "value"` |
| `\|=` | 병합 할당 | `. \|= parse_json!(.msg)` |

```vrl
# 기본 할당
.status = "active"

# 병합 할당 (객체 병합)
. |= parse_json!(.message)
. |= {"new_field": "value"}
```

---

## 표현식

VRL은 표현식 지향 언어입니다. VRL 프로그램은 전적으로 표현식으로 구성되며, 모든 표현식은 값을 반환합니다.

### 할당 표현식

오른쪽 표현식의 결과를 왼쪽 대상(경로 또는 변수)에 할당합니다.

```vrl
# 변수 할당
message = "Hello"

# 경로 할당
.status = 200
.user.name = "John"
.tags[0] = "important"

# 다중 할당
.a = .b = .c = "same value"
```

### 블록 표현식

중괄호 내의 하나 이상의 표현식 시퀀스입니다. 블록은 비어 있을 수 없습니다(빈 블록 `{}`는 빈 객체로 처리됨).

```vrl
result = {
    temp = .value * 2
    temp + 10
}

# 조건부 블록
if .condition {
    .a = 1
    .b = 2
}
```

### 조건문 (If 표현식)

불리언 표현식의 값에 따라 두 분기 중 하나를 실행합니다.

```vrl
# 기본 if 문
if .status >= 400 {
    .level = "error"
}

# if-else 문
if .status >= 500 {
    .level = "critical"
} else if .status >= 400 {
    .level = "error"
} else {
    .level = "info"
}

# 표현식으로서의 if (값 반환)
.level = if .status >= 400 { "error" } else { "info" }
```

### 중단 표현식 (Abort)

VRL 프로그램을 종료하고 이벤트에 대한 모든 수정을 취소합니다.

```vrl
# 조건부 중단
if .should_drop {
    abort
}

# 유효성 검사 실패 시 중단
if !exists(.required_field) {
    abort
}
```

### 함수 호출 표현식

내장 VRL 함수를 호출합니다.

```vrl
# 기본 함수 호출
.timestamp = now()
.upper = upcase(.message)

# 명명된 인자 사용
.parsed = parse_timestamp(.time, format: "%Y-%m-%d %H:%M:%S")

# 오류 처리와 함께 (!를 사용하여 실패 시 중단)
. = parse_json!(.message)
```

### 인덱스 표현식

배열의 요소를 참조합니다. VRL의 배열 인덱스는 0부터 시작합니다.

```vrl
.first = .items[0]
.second = .items[1]
.last = .items[-1]        # 마지막 요소
.second_last = .items[-2] # 뒤에서 두 번째
```

---

## 함수

VRL은 관측성 데이터 처리에 특화된 풍부한 내장 함수를 제공합니다. 함수는 목적에 따라 분류됩니다.

### 파싱 함수 (Parsing Functions)

다양한 형식의 데이터를 구조화된 형태로 파싱합니다.

#### parse_json

JSON 문자열을 VRL 값으로 파싱합니다.

```vrl
# 기본 사용법
.data = parse_json!(.message)

# 오류 처리와 함께
.data = parse_json(.message) ?? {}

# 이벤트에 병합
. = parse_json!(.message)
```

#### parse_syslog

Syslog 메시지를 구조화된 데이터로 파싱합니다 (RFC 5424, RFC 3164 지원).

```vrl
# Syslog 파싱
. = parse_syslog!(.message)

# 결과 필드: appname, facility, hostname, message, msgid,
#           procid, severity, timestamp, version
```

#### parse_regex

정규 표현식을 사용하여 문자열에서 데이터를 추출합니다.

```vrl
# 정규 표현식으로 파싱
.parsed = parse_regex!(.message, r'^(?P<ip>\d+\.\d+\.\d+\.\d+) - (?P<user>\S+)')

# 결과 접근
.client_ip = .parsed.ip
.username = .parsed.user
```

#### parse_apache_log

Apache 로그를 파싱합니다.

```vrl
# Common 형식
.log = parse_apache_log!(.message, format: "common")

# Combined 형식
.log = parse_apache_log!(.message, format: "combined")
```

#### parse_nginx_log

Nginx 로그를 파싱합니다.

```vrl
# Combined 형식
.log = parse_nginx_log!(.message, format: "combined")

# Error 형식
.log = parse_nginx_log!(.message, format: "error")
```

#### parse_key_value

키-값 쌍을 파싱합니다.

```vrl
# 기본 사용법 (key=value 형식)
.data = parse_key_value!(.message)

# 커스텀 구분자
.data = parse_key_value!(.message, key_value_delimiter: ":", field_delimiter: ",")
```

#### parse_csv

CSV 형식의 행을 파싱합니다.

```vrl
# 기본 CSV 파싱
.fields = parse_csv!(.message)

# 커스텀 구분자
.fields = parse_csv!(.message, delimiter: ";")
```

#### parse_timestamp

문자열을 타임스탬프로 파싱합니다.

```vrl
# 형식 지정
.timestamp = parse_timestamp!(.time_str, format: "%Y-%m-%d %H:%M:%S")

# ISO 8601 형식
.timestamp = parse_timestamp!(.time_str, format: "%+")
```

#### parse_duration

기간 문자열을 초 단위로 파싱합니다.

```vrl
# 기간 파싱
.seconds = parse_duration!(.duration, "s")

# 예: "5m" -> 300, "1h30m" -> 5400
```

#### parse_url

URL을 구성 요소로 파싱합니다.

```vrl
.url = parse_url!("https://example.com:8080/path?query=1#fragment")

# 결과: {
#   "scheme": "https",
#   "host": "example.com",
#   "port": 8080,
#   "path": "/path",
#   "query": {"query": "1"},
#   "fragment": "fragment"
# }
```

#### parse_xml

XML 문서를 VRL 값으로 파싱합니다.

```vrl
.data = parse_xml!(.xml_content)
```

#### parse_grok

Grok 패턴을 사용하여 파싱합니다.

```vrl
.parsed = parse_grok!(.message, "%{IP:client} %{WORD:method} %{URIPATHPARAM:request}")
```

### 인코딩 함수 (Encoding Functions)

VRL 값을 다양한 형식으로 인코딩합니다.

#### encode_json

VRL 값을 JSON 문자열로 인코딩합니다.

```vrl
.json_string = encode_json(.)
.pretty_json = encode_json(., pretty: true)
```

#### encode_base64

문자열을 Base64로 인코딩합니다.

```vrl
.encoded = encode_base64(.data)
.decoded = decode_base64!(.encoded)
```

#### encode_gzip / decode_gzip

Gzip 압축/해제를 수행합니다.

```vrl
.compressed = encode_gzip(.data)
.decompressed = decode_gzip!(.compressed)
```

#### encode_zlib / decode_zlib

Zlib 압축/해제를 수행합니다.

```vrl
.compressed = encode_zlib(.data)
.decompressed = decode_zlib!(.compressed)
```

### 문자열 함수 (String Functions)

#### upcase / downcase

대소문자 변환을 수행합니다.

```vrl
.upper = upcase(.message)      # 대문자로
.lower = downcase(.message)    # 소문자로
```

#### contains

문자열에 하위 문자열이 포함되어 있는지 확인합니다.

```vrl
if contains(.message, "error") {
    .level = "error"
}

# 대소문자 무시
if contains(.message, "ERROR", case_sensitive: false) {
    .level = "error"
}
```

#### starts_with / ends_with

문자열의 시작/끝을 확인합니다.

```vrl
if starts_with(.path, "/api/") {
    .type = "api"
}

if ends_with(.file, ".log") {
    .is_log = true
}
```

#### replace

문자열 치환을 수행합니다.

```vrl
# 첫 번째 일치 항목 치환
.sanitized = replace(.message, "password", "[REDACTED]")

# 모든 일치 항목 치환
.sanitized = replace(.message, r'\d{4}-\d{4}-\d{4}-\d{4}', "[CARD]", count: -1)
```

#### split

문자열을 구분자로 분할합니다.

```vrl
.parts = split(.path, "/")
.words = split(.sentence, " ", limit: 3)
```

#### join

배열을 문자열로 결합합니다.

```vrl
.path = join(.segments, "/")
.csv = join(.values, ",")
```

#### strip_whitespace

앞뒤 공백을 제거합니다.

```vrl
.trimmed = strip_whitespace(.input)
```

#### slice

문자열의 일부를 추출합니다.

```vrl
.first_10 = slice!(.message, 0, 10)
.last_5 = slice!(.message, -5)
```

#### length

문자열(또는 배열/객체)의 길이를 반환합니다.

```vrl
.msg_length = length(.message)
.item_count = length(.items)
```

#### match

정규 표현식 매칭을 수행합니다.

```vrl
if match(.message, r'error|fail|exception') {
    .level = "error"
}
```

#### find

문자열에서 패턴의 위치를 찾습니다.

```vrl
.position = find(.message, "error") ?? -1
```

### 타입 변환 함수 (Type Functions)

#### to_string

값을 문자열로 변환합니다.

```vrl
.status_str = to_string(.status)
```

#### to_int

값을 정수로 변환합니다.

```vrl
.count = to_int!(.count_str)
```

#### to_float

값을 부동소수점으로 변환합니다.

```vrl
.price = to_float!(.price_str)
```

#### to_bool

값을 불리언으로 변환합니다.

```vrl
.flag = to_bool!(.flag_str)
```

#### to_timestamp

값을 타임스탬프로 변환합니다.

```vrl
.time = to_timestamp!(.time_value)
```

#### to_regex

문자열을 정규 표현식으로 변환합니다.

```vrl
pattern_str = "error|warning"
.pattern = to_regex!(pattern_str)
```

### 배열 함수 (Array Functions)

#### push

배열 끝에 요소를 추가합니다.

```vrl
.tags = push(.tags, "new_tag")
```

#### append

두 배열을 연결합니다.

```vrl
.all_tags = append(.tags1, .tags2)
```

#### flatten

중첩 배열을 평탄화합니다.

```vrl
.flat = flatten([[1, 2], [3, 4]])  # [1, 2, 3, 4]
```

#### unique

배열에서 중복을 제거합니다 (첫 번째 발생 유지).

```vrl
.unique_tags = unique(.tags)
```

#### filter

조건에 맞는 요소만 필터링합니다.

```vrl
.errors = filter(.logs) -> |_index, value| {
    value.level == "error"
}
```

#### map

배열의 각 요소를 변환합니다.

```vrl
.doubled = map(.numbers) -> |_index, value| {
    value * 2
}
```

#### reduce

배열을 단일 값으로 축소합니다.

```vrl
.sum = reduce(.numbers, 0) -> |acc, _index, value| {
    acc + value
}
```

#### zip

여러 배열을 병렬로 순회하여 새 배열을 생성합니다.

```vrl
.pairs = zip([1, 2, 3], ["a", "b", "c"])
# 결과: [[1, "a"], [2, "b"], [3, "c"]]
```

### 객체 함수 (Object Functions)

#### keys

객체의 모든 키를 배열로 반환합니다.

```vrl
.field_names = keys(.)
```

#### values

객체의 모든 값을 배열로 반환합니다.

```vrl
.field_values = values(.)
```

#### merge

두 객체를 병합합니다.

```vrl
.combined = merge(.defaults, .overrides)

# 깊은 병합
.combined = merge(.defaults, .overrides, deep: true)
```

#### del

필드를 삭제하고 삭제된 값을 반환합니다.

```vrl
deleted_value = del(.sensitive_field)
del(.password)
del(.user.ssn)
```

#### exists

경로가 존재하는지 확인합니다 (null 값과 누락된 경로를 구분).

```vrl
if exists(.optional_field) {
    .has_optional = true
}
```

#### get

동적으로 경로의 값을 가져옵니다.

```vrl
field_name = "status"
.value = get(., [field_name]) ?? "unknown"
```

#### set

동적으로 경로에 값을 설정합니다.

```vrl
field_name = "new_field"
. = set!(., [field_name], "value")
```

### 시간 함수 (Time Functions)

#### now

현재 타임스탬프를 반환합니다.

```vrl
.processed_at = now()
```

#### format_timestamp

타임스탬프를 문자열로 형식화합니다.

```vrl
.time_str = format_timestamp!(.timestamp, format: "%Y-%m-%d %H:%M:%S")
.iso = format_timestamp!(.timestamp, format: "%+")
```

#### from_unix_timestamp

Unix 타임스탬프를 VRL 타임스탬프로 변환합니다.

```vrl
# 초 단위 (기본값)
.time = from_unix_timestamp!(.unix_time)

# 밀리초 단위
.time = from_unix_timestamp!(.unix_time_ms, unit: "milliseconds")

# 나노초 단위
.time = from_unix_timestamp!(.unix_time_ns, unit: "nanoseconds")

# 마이크로초 단위
.time = from_unix_timestamp!(.unix_time_us, unit: "microseconds")
```

#### to_unix_timestamp

VRL 타임스탬프를 Unix 타임스탬프로 변환합니다.

```vrl
.unix_time = to_unix_timestamp(.timestamp)
.unix_time_ms = to_unix_timestamp(.timestamp, unit: "milliseconds")
```

### IP 함수 (IP Functions)

#### ip_aton

IP 주소를 정수로 변환합니다.

```vrl
.ip_int = ip_aton!(.ip_address)
```

#### ip_ntoa

정수를 IP 주소로 변환합니다.

```vrl
.ip_address = ip_ntoa!(.ip_int)
```

#### ip_cidr_contains

IP 주소가 CIDR 범위에 포함되는지 확인합니다.

```vrl
if ip_cidr_contains!("10.0.0.0/8", .client_ip) {
    .is_internal = true
}
```

#### ip_to_ipv6

IPv4 주소를 IPv6 형식으로 변환합니다.

```vrl
.ipv6 = ip_to_ipv6!(.ipv4_address)
```

### 해시 함수 (Hash Functions)

#### md5

MD5 해시를 계산합니다.

```vrl
.hash = md5(.data)
```

#### sha1

SHA-1 해시를 계산합니다.

```vrl
.hash = sha1(.data)
```

#### sha2

SHA-2 해시를 계산합니다.

```vrl
.hash = sha2(.data)                    # SHA-256 (기본값)
.hash = sha2(.data, variant: "SHA-512")
```

#### sha3

SHA-3 해시를 계산합니다.

```vrl
.hash = sha3(.data)
```

### 암호화 함수 (Cryptography Functions)

#### hmac

HMAC을 계산합니다.

```vrl
.hmac = hmac(.data, key: "secret_key", algorithm: "SHA-256")
```

### 기타 유용한 함수

#### uuid_v4

무작위 UUID v4를 생성합니다.

```vrl
.id = uuid_v4()
```

#### random_bytes

무작위 바이트를 생성합니다.

```vrl
.token = encode_base64(random_bytes(32))
```

#### assert

조건이 참인지 확인하고, 거짓이면 오류를 발생시킵니다.

```vrl
assert!(.status >= 100 && .status < 600, message: "Invalid HTTP status")
```

#### log

디버깅을 위해 메시지를 로깅합니다.

```vrl
log("Processing event: " + to_string(.event_id), level: "debug")
```

#### type_def

값의 타입을 문자열로 반환합니다.

```vrl
.type = type_def(.value)  # "string", "integer", "boolean", etc.
```

#### compact

null 값을 제거합니다.

```vrl
.cleaned = compact(.)
.cleaned_array = compact(.array)
```

#### shannon_entropy

문자열의 섀넌 엔트로피를 계산합니다.

```vrl
.entropy = shannon_entropy(.password)
```

---

## 에러 처리

VRL은 실패 안전(fail-safe) 언어로, 모든 잠재적 오류가 처리되지 않으면 컴파일되지 않습니다.

### 오류의 종류

#### 컴파일 타임 오류

컴파일 시점에 발생하는 오류입니다:

1. 유효하지 않은 토큰: VRL 파서가 인식하지 못하는 문자
2. 유효하지 않은 인자: 함수에 잘못된 인자 전달
3. 정적 표현식 필요: 동적 표현식 대신 정적 표현식 필요
4. 유효하지 않은 타임스탬프: RFC 3339 형식에 맞지 않는 타임스탬프
5. 유효하지 않은 정규 표현식: 잘못된 정규 표현식 문법
6. 타입 불일치: 예상 타입과 실제 타입의 불일치

```vrl
# 컴파일 오류 예시
.result = "10" + 5  # 오류: 문자열과 정수를 더할 수 없음
```

#### 런타임 오류

실행 시점에 발생할 수 있는 오류입니다:

1. 0으로 나누기: 정수 또는 부동소수점을 0으로 나누기
2. 파싱 실패: 잘못된 형식의 데이터 파싱
3. 타입 변환 실패: 호환되지 않는 타입으로 변환

```vrl
# 런타임 오류 가능성
.result = .a / .b  # .b가 0이면 오류
```

### 오류 처리 방법

#### 1. ! 연산자 (Abort on Error)

오류 발생 시 프로그램을 중단합니다.

```vrl
# 파싱 실패 시 프로그램 중단
. = parse_json!(.message)

# 타입 변환 실패 시 중단
.count = to_int!(.count_str)
```

#### 2. ?? 연산자 (Coalesce / Default Value)

오류 발생 시 기본값을 사용합니다.

```vrl
# 파싱 실패 시 빈 객체 사용
.data = parse_json(.message) ?? {}

# null이면 기본값 사용
.name = .username ?? "anonymous"

# 체인으로 여러 대안 제공
.value = .primary ?? .secondary ?? .fallback ?? "default"
```

#### 3. 결과 변수 할당

오류를 변수에 캡처하여 처리합니다.

```vrl
result, err = parse_json(.message)

if err != null {
    log("Parse error: " + to_string(err), level: "error")
    .parse_failed = true
} else {
    . = result
}
```

### Fallible vs Infallible 함수

#### Fallible 함수

런타임에 실패할 수 있는 함수입니다. 오류 처리가 필수입니다.

```vrl
# fallible 함수들
parse_json(.msg)      # 유효하지 않은 JSON
parse_syslog(.msg)    # 유효하지 않은 Syslog
to_int(.str)          # 숫자가 아닌 문자열
```

#### Infallible 함수

올바른 인자가 주어지면 절대 실패하지 않는 함수입니다.

```vrl
# infallible 함수들 (인자 타입이 보장된 경우)
upcase("hello")       # 항상 성공
now()                 # 항상 성공
length([1, 2, 3])     # 항상 성공
```

참고: 함수 자체가 infallible이라도 인자가 런타임에 실패할 수 있으면 전체 표현식은 fallible로 간주됩니다.

### 오류 메시지 예시

VRL은 도움이 되는 오류 메시지를 제공합니다:

```
error[E110]: invalid argument type
  ┌─ :1:21
  │
1 │ parse_syslog(42)
  │              ^^
  │              │
  │              this expression resolves to type integer
  │              but the parameter requires type string
  │
  = try: coercing to a string using the `to_string` function
  = see: https://vector.dev/docs/reference/vrl/errors/#E110
```

---

## Remap Transform 설정

VRL은 주로 Vector의 `remap` transform과 함께 사용됩니다.

### 기본 설정

```toml
[transforms.parse_logs]
type = "remap"
inputs = ["source"]
source = '''
. = parse_json!(.message)
.timestamp = now()
.processed = true
'''
```

### 주요 설정 옵션

#### source

VRL 프로그램을 지정합니다.

```toml
source = '''
.level = downcase!(.level)
.timestamp = parse_timestamp!(.time, format: "%Y-%m-%d %H:%M:%S")
'''
```

#### file

외부 VRL 파일을 사용합니다.

```toml
file = "/etc/vector/transforms/parse.vrl"
```

#### drop_on_error

VRL 프로그램에서 오류 발생 시 이벤트를 드롭할지 결정합니다.

```toml
drop_on_error = true  # 오류 시 이벤트 드롭 (기본값: false)
```

- `false` (기본값): 오류 발생 시 수정되지 않은 원본 이벤트가 다운스트림으로 전송
- `true`: 오류 발생 시 이벤트 드롭

#### drop_on_abort

`abort` 표현식 실행 시 이벤트를 드롭할지 결정합니다.

```toml
drop_on_abort = true  # abort 시 이벤트 드롭 (기본값: true)
```

#### reroute_dropped

드롭된 이벤트를 별도 출력으로 라우팅합니다.

```toml
reroute_dropped = true
```

이 옵션이 활성화되면 드롭된 이벤트는 `<transform_name>.dropped` 출력으로 전송됩니다.

### 완전한 설정 예시

```toml
[sources.logs]
type = "file"
include = ["/var/log/*.log"]

[transforms.parse_and_enrich]
type = "remap"
inputs = ["logs"]
drop_on_error = true
drop_on_abort = true
reroute_dropped = true
source = '''
# JSON 파싱
. = parse_json!(.message)

# 타임스탬프 처리
.timestamp = parse_timestamp!(.time, format: "%Y-%m-%d %H:%M:%S") ?? now()

# 레벨 정규화
.level = downcase(.level) ?? "info"

# 민감 정보 삭제
del(.password)
del(.token)

# 필터링
if .level == "debug" && !exists(.force_log) {
    abort
}
'''

[transforms.handle_dropped]
type = "remap"
inputs = ["parse_and_enrich.dropped"]
source = '''
.error_type = "parse_failure"
.original_message = .message
'''

[sinks.main_output]
type = "console"
inputs = ["parse_and_enrich"]
encoding.codec = "json"

[sinks.error_output]
type = "file"
inputs = ["handle_dropped"]
path = "/var/log/vector/errors.log"
encoding.codec = "json"
```

---

## 실전 예제

### 예제 1: Syslog 파싱

```vrl
# Syslog 메시지 파싱
. = parse_syslog!(.message)

# 결과 예시:
# {
#   "appname": "myapp",
#   "facility": "local0",
#   "hostname": "server1",
#   "message": "Connection established",
#   "msgid": "12345",
#   "procid": 1234,
#   "severity": "info",
#   "timestamp": "2021-02-11T10:32:50Z",
#   "version": 1
# }
```

### 예제 2: Apache 로그 파싱

```vrl
# Apache Common Log 형식 파싱
log = parse_apache_log!(.message, format: "common")

# 상태 코드 기반 필터링
if to_int!(log.status) >= 400 {
    .level = "error"
} else {
    .level = "info"
}

# 필드 추출
.client_ip = log.host
.method = log.method
.path = log.path
.status = to_int!(log.status)
.bytes = to_int(log.size) ?? 0
```

### 예제 3: Nginx 로그 파싱

```vrl
# Nginx Combined 형식 파싱
. = parse_nginx_log!(.message, format: "combined")

# 또는 커스텀 정규 표현식 사용
. |= parse_regex!(
    .message,
    r'^(?P<ip>\d+\.\d+\.\d+\.\d+) - (?P<date>\d+\-\d+\-\d+)T(?P<time>\d+:\d+:\d+).+?"(?P<url>.+?)" (?P<status>\d+) (?P<size>\d+) "(?P<agent>.+?)"$'
)
```

### 예제 4: JSON 로그 변환

```vrl
# JSON 파싱
. = parse_json!(.message)

# 새 필드 추가
.new_field = "new value"

# 타입 변환
.status = to_int!(.status)
.duration = parse_duration!(.duration, "s")

# 타임스탬프 처리
.time = from_unix_timestamp!(.time)
.time = format_timestamp!(.time, format: "%+")
```

### 예제 5: 민감 정보 마스킹

```vrl
# 이메일 마스킹
.email = replace(.email, r'(\w{2})\w*(@\w+\.\w+)', "$1*$2")

# 카드 번호 마스킹
.card = replace(.card, r'\d{4}-\d{4}-\d{4}-(\d{4})', "---$1")

# 전화번호 마스킹
.phone = replace(.phone, r'(\d{3})\d{4}(\d{4})', "$1--$2")

# 민감 필드 삭제
del(.password)
del(.ssn)
del(.secret_key)
```

### 예제 6: 조건부 처리

```vrl
# 로그 레벨에 따른 처리
if .level == "error" || .level == "fatal" {
    .priority = "high"
    .alert = true
} else if .level == "warning" {
    .priority = "medium"
} else {
    .priority = "low"
}

# 필드 존재 여부 확인
if exists(.user_id) {
    .authenticated = true
} else {
    .authenticated = false
    .user_id = "anonymous"
}

# 조건부 드롭
if .level == "debug" {
    abort
}
```

### 예제 7: 데이터 보강

```vrl
# 타임스탬프 추가
.processed_at = now()
.process_id = uuid_v4()

# 환경 정보 추가
.environment = "production"
.service = "api-gateway"
.version = "1.2.3"

# IP 기반 처리
if ip_cidr_contains!("10.0.0.0/8", .client_ip) {
    .network = "internal"
} else if ip_cidr_contains!("192.168.0.0/16", .client_ip) {
    .network = "private"
} else {
    .network = "external"
}

# 객체 병합
.metadata = merge(.metadata ?? {}, {
    "source": "vector",
    "processed": true
})
```

### 예제 8: 배열 처리

```vrl
# 태그 정규화
.tags = map(.tags) -> |_index, tag| {
    downcase(tag)
}

# 중복 제거
.tags = unique(.tags)

# 특정 태그 필터링
.error_tags = filter(.tags) -> |_index, tag| {
    starts_with(tag, "error-")
}

# 태그 문자열 생성
.tag_string = join(.tags, ",")
```

### 예제 9: 숫자 계산

```vrl
# 응답 시간 계산 (밀리초를 초로)
.response_time_sec = .response_time_ms / 1000.0

# 백분율 계산
.success_rate = (.success_count / .total_count) * 100

# 반올림
.rounded_rate = round(.success_rate, precision: 2)

# 범위 확인
if .response_time_sec > 5.0 {
    .slow_request = true
}
```

### 예제 10: 복잡한 로그 파싱

```vrl
# 여러 형식 시도
parsed, err = parse_json(.message)

if err == null {
    . = parsed
} else {
    parsed, err = parse_syslog(.message)
    if err == null {
        . = parsed
    } else {
        # 키-값 형식 시도
        parsed, err = parse_key_value(.message)
        if err == null {
            . = parsed
        } else {
            # 원본 유지, 파싱 실패 표시
            .parse_failed = true
            .raw_message = .message
        }
    }
}

# 공통 필드 정규화
.timestamp = .timestamp ?? .time ?? .ts ?? now()
.level = downcase(.level ?? .severity ?? .log_level ?? "info")
```

---

## VRL 실행 및 테스트

### REPL (대화형 모드)

Vector가 설치되어 있다면 REPL을 사용하여 VRL을 대화형으로 테스트할 수 있습니다.

```bash
# REPL 시작
vector vrl

# 예시 세션
$ vector vrl
> .message = "Hello, World!"
"Hello, World!"
> upcase(.message)
"HELLO, WORLD!"
```

### 파일 기반 실행

```bash
# VRL 프로그램 파일 실행
vector vrl --input input.json --program program.vrl --print-object
```

input.json:
```json
{"message": "test message", "level": "INFO"}
```

program.vrl:
```vrl
.level = downcase!(.level)
.timestamp = now()
```

### VRL Playground

온라인에서 VRL을 테스트할 수 있는 플레이그라운드도 제공됩니다:

- URL: https://playground.vrl.dev/

### 단위 테스트

Vector 설정 파일에서 VRL 프로그램을 단위 테스트할 수 있습니다.

```toml
[[tests]]
name = "Test JSON parsing"

[[tests.inputs]]
insert_at = "parse_logs"
type = "log"
log_fields.message = '{"level": "ERROR", "msg": "test"}'

[[tests.outputs]]
extract_from = "parse_logs"

[[tests.outputs.conditions]]
type = "vrl"
source = '''
assert_eq!(.level, "error")
assert!(exists(.msg))
'''
```

---

## 참고 자료

- [VRL Reference](https://vector.dev/docs/reference/vrl/) - 공식 VRL 문서
- [VRL Expression Reference](https://vector.dev/docs/reference/vrl/expressions/) - 표현식 참조
- [VRL Function Reference](https://vector.dev/docs/reference/vrl/functions/) - 함수 참조
- [VRL Example Reference](https://vector.dev/docs/reference/vrl/examples/) - 예제 참조
- [VRL Error Reference](https://vector.dev/docs/reference/vrl/errors/) - 오류 참조
- [Remap Transform](https://vector.dev/docs/reference/configuration/transforms/remap/) - Remap Transform 문서
- [VRL Playground](https://playground.vrl.dev/) - 온라인 VRL 테스트
- [VRL GitHub Repository](https://github.com/vectordotdev/vrl) - VRL 소스 코드
