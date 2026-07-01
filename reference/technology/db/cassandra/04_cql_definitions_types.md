# CQL 정의 및 데이터 타입

> 원본: https://cassandra.apache.org/doc/latest/cassandra/cql/

---

## 목차

1. [정의(Definitions)](#정의definitions)
   1. [표기 규칙(Conventions)](#표기-규칙conventions)
   2. [식별자와 키워드(Identifiers and keywords)](#식별자와-키워드identifiers-and-keywords)
   3. [상수(Constants)](#상수constants)
   4. [항(Terms)](#항terms)
   5. [주석(Comments)](#주석comments)
   6. [구문(Statements)](#구문statements)
   7. [준비된 구문(Prepared Statements)](#준비된-구문prepared-statements)
2. [데이터 타입(Data Types)](#데이터-타입data-types)
   1. [네이티브 타입(Native types)](#네이티브-타입native-types)
   2. [카운터(Counters)](#카운터counters)
   3. [타임스탬프 다루기(Working with timestamps)](#타임스탬프-다루기working-with-timestamps)
   4. [date 타입](#date-타입)
   5. [time 타입](#time-타입)
   6. [duration 타입](#duration-타입)
   7. [컬렉션(Collections)](#컬렉션collections)
   8. [사용자 정의 타입(UDT)](#사용자-정의-타입udt)
   9. [튜플(Tuples)](#튜플tuples)
   10. [커스텀 타입(Custom Types)](#커스텀-타입custom-types)
3. [참고 자료](#참고-자료)

---

## 정의(Definitions)

### 표기 규칙(Conventions)

CQL 문법을 명세하는 데 도움을 주기 위해, 이 문서에서는 다음과 같은 표기 규칙을 사용합니다.

- 언어 규칙은 비공식적인 [BNF 변형(BNF variant)](http://en.wikipedia.org/wiki/Backus%E2%80%93Naur_Form#Variants) 표기법으로 제공됩니다. 특히 선택적(optional) 항목은 대괄호(`[ item ]`)로 표기하고, 반복(repeated) 항목은 별표(`*`)와 더하기 기호(`+`)로 표기합니다. 별표는 0개 이상과 일치(matches zero or more)하며, 더하기 기호는 1개 이상과 일치(matches one or more)합니다.
- 문법은 편의를 위해 다음 규칙도 따릅니다. 비종단(non-terminal) 항은 소문자로 표기하고(그리고 해당 정의로 링크되며), 종단 키워드(terminal keyword)는 "모두 대문자(all caps)"로 제공됩니다. 다만 키워드는 `식별자(identifier)`이므로 실제로는 대소문자를 구분하지 않습니다. 또한 일부 기초적인 구성은 정규식(regexp)으로 정의하며, 이는 `re(<some regular expression>)`로 표기합니다.
- 문법은 문서화 목적으로 제공되며 일부 사소한 세부 사항은 생략합니다. 예를 들어 `CREATE TABLE` 구문에서 마지막 컬럼 정의 뒤의 쉼표는 선택 사항이지만 존재해도 지원됩니다. 이는 이 문서의 문법이 시사하는 바와 다릅니다. 또한 문법이 허용한다고 해서 모든 것이 반드시 유효한 CQL인 것은 아닙니다.
- 본문 중에 나오는 키워드나 CQL 코드 조각은 `고정폭 글꼴(fixed-width font)`로 표시됩니다.

---

### 식별자와 키워드(Identifiers and keywords)

CQL 언어는 테이블, 컬럼 및 기타 객체를 식별하기 위해 _식별자(identifier)_ (또는 _이름(name)_ )를 사용합니다. 식별자는 정규식 `[a-zA-Z][a-zA-Z0-9_]*` 와 일치하는 토큰입니다.

`SELECT` 나 `WITH` 와 같은 다수의 식별자는 _키워드(keyword)_ 입니다. 이들은 언어에서 고정된 의미를 가지며 대부분은 예약(reserved)되어 있습니다. 이러한 키워드의 목록은 [부록 A(Appendix A)](https://cassandra.apache.org/doc/latest/cassandra/cql/appendices.html)에서 확인할 수 있습니다.

식별자와 (따옴표로 묶이지 않은) 키워드는 대소문자를 구분하지 않습니다. 따라서 `SELECT` 는 `select` 나 `sElEcT` 와 동일하며, `myId` 는 `myid` 나 `MYID` 와 동일합니다. 흔히 사용되는 관례(특히 이 문서의 예제에서 사용하는 관례)는 키워드에는 대문자를, 그 외 식별자에는 소문자를 사용하는 것입니다.

_따옴표로 묶인 식별자(quoted identifier)_ 라고 불리는 두 번째 종류의 식별자도 있습니다. 이는 비어 있지 않은 임의의 문자 시퀀스를 큰따옴표(`"`)로 감싸서 정의합니다. 따옴표로 묶인 식별자는 결코 키워드가 아닙니다. 따라서 `"select"` 는 예약 키워드가 아니며 컬럼을 가리키는 데 사용할 수 있습니다(다만 이런 사용은 매우 권장되지 않습니다). 반면 `select` 는 파싱 오류를 일으킵니다. 또한 따옴표로 묶이지 않은 식별자나 키워드와 달리, 따옴표로 묶인 식별자는 대소문자를 구분합니다(`"My Quoted Id"` 는 `"my quoted id"` 와 _다릅니다_ ). 그러나 `[a-zA-Z][a-zA-Z0-9_]*` 와 일치하는 완전히 소문자인 따옴표 식별자는 큰따옴표를 제거하여 얻은 따옴표 없는 식별자와 _동등(equivalent)_ 합니다(따라서 `"myid"` 는 `myid` 및 `myId` 와 동등하지만 `"myId"` 와는 다릅니다). 따옴표로 묶인 식별자 내부에서는 큰따옴표 문자를 두 번 반복하여 이스케이프할 수 있으므로 `"foo "" bar"` 는 유효한 식별자입니다.

> **참고(NOTE)**
>
> _따옴표로 묶인 식별자_ 를 사용하면 임의의 이름으로 컬럼을 선언할 수 있는데, 이러한 이름이 서버가 사용하는 특정 이름과 충돌할 수 있습니다. 예를 들어 조건부 업데이트(conditional update)를 사용할 때 서버는 `"[applied]"` 라는 특수한 이름이 포함된 결과 집합(result set)으로 응답합니다. 만약 이러한 이름으로 컬럼을 선언했다면 일부 도구를 혼란스럽게 만들 수 있으므로 피해야 합니다. 일반적으로 따옴표 없는 식별자가 선호되지만, 따옴표로 묶인 식별자를 사용한다면 대괄호로 감싼 이름(예: `"[applied]"`)이나 함수 호출처럼 보이는 이름(예: `"f(x)"`)은 피하는 것이 강력히 권장됩니다.

보다 형식적으로는 다음과 같습니다.

```bnf
identifier::= unquoted_identifier | quoted_identifier
unquoted_identifier::= re('[a-zA-Z][a-zA-Z0-9_]*')
quoted_identifier::= '"' (any character where " can appear if doubled)+ '"'
```

---

### 상수(Constants)

CQL은 다음과 같은 _상수(constant)_ 를 정의합니다.

```bnf
constant::= string | integer | float | boolean | uuid | blob | NULL
string::= ''' (any character where ' can appear if doubled)+ ''' : '$$' (any character other than '$$') '$$'
integer::= re('-?[0-9]+')
float::= re('-?[0-9]+(.[0-9]*)?([eE][+-]?[0-9+])?') | NAN | INFINITY
boolean::= TRUE | FALSE
uuid::= hex{8}-hex{4}-hex{4}-hex{4}-hex{12}
hex::= re("[0-9a-fA-F]")
blob::= '0' ('x' | 'X') hex+
```

다시 말해 다음과 같습니다.

- 문자열(string) 상수는 작은따옴표(`'`)로 감싼 임의의 문자 시퀀스입니다. 작은따옴표를 포함하려면 두 번 반복하면 됩니다. 예: `'It''s raining today'`. 이것은 큰따옴표를 사용하는 따옴표로 묶인 `식별자(identifier)` 와 혼동해서는 안 됩니다. 또는 임의의 문자 시퀀스를 두 개의 달러 문자(`$$`)로 감싸서 문자열을 정의할 수도 있으며, 이 경우 작은따옴표를 이스케이프하지 않고 사용할 수 있습니다(`$$It's raining today$$`). 후자의 형식은 함수 본문에서 작은따옴표를 이스케이프하지 않기 위해 [사용자 정의 함수(user-defined functions)](https://cassandra.apache.org/doc/latest/cassandra/cql/functions.html#udfs)를 정의할 때 자주 사용됩니다(함수 본문에서는 작은따옴표가 `$$` 보다 더 자주 나타날 가능성이 높기 때문입니다).
- 정수(integer), 부동소수점(float), 불리언(boolean) 상수는 예상되는 대로 정의됩니다. 다만 float은 특수한 `NaN` 및 `Infinity` 상수를 허용합니다.
- CQL은 [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier) 상수를 지원합니다.
- blob의 내용은 16진수(hexadecimal)로 제공되며 `0x` 로 시작합니다.
- 특수한 `NULL` 상수는 값의 부재(absence of value)를 나타냅니다.

이러한 상수가 어떻게 타입이 지정되는지에 대해서는 [데이터 타입(Data types)](#데이터-타입data-types) 섹션을 참고하세요.

---

### 항(Terms)

CQL에는 _항(term)_ 이라는 개념이 있으며, 이는 CQL이 지원하는 값의 종류를 나타냅니다. 항은 다음과 같이 정의됩니다.

```bnf
term::= constant | literal | function_call | arithmetic_operation | type_hint | bind_marker
literal::= collection_literal | vector_literal | udt_literal | tuple_literal
function_call::= identifier '(' [ term (',' term)* ] ')'
arithmetic_operation::= '-' term | term ('+' | '-' | '*' | '/' | '%') term
type_hint::= '(' cql_type ')' term
bind_marker::= '?' | ':' identifier
```

따라서 항은 다음 중 하나입니다.

- [상수(constant)](#상수constants)
- [컬렉션(collection)](#컬렉션collections), 벡터(vector), [사용자 정의 타입(user-defined type)](#사용자-정의-타입udt) 또는 [튜플(tuple)](#튜플tuples)에 대한 리터럴(literal)
- [네이티브 함수(native function)](https://cassandra.apache.org/doc/latest/cassandra/cql/functions.html) 또는 [사용자 정의 함수(user-defined function)](https://cassandra.apache.org/doc/latest/cassandra/cql/functions.html) 중 하나인 [함수(function)](https://cassandra.apache.org/doc/latest/cassandra/cql/functions.html) 호출
- 항 사이의 [산술 연산(arithmetic operation)](https://cassandra.apache.org/doc/latest/cassandra/cql/operators.html)
- 타입 힌트(type hint)
- 실행 시점에 바인딩될 변수를 나타내는 바인드 마커(bind marker). 자세한 내용은 [준비된 구문(prepared-statements)](#준비된-구문prepared-statements) 섹션을 참고하세요. 바인드 마커는 익명(anonymous, `?`)이거나 이름이 지정된(named, `:some_name`) 형태일 수 있습니다. 후자의 형식은 바인딩 시 변수를 참조하는 더 편리한 방법을 제공하므로 일반적으로 선호되어야 합니다.

---

### 주석(Comments)

CQL의 주석은 이중 대시(`--`) 또는 이중 슬래시(`//`)로 시작하는 한 줄입니다.

여러 줄 주석(multi-line comment)도 `/*` 와 `*/` 로 감싸서 지원됩니다(단, 중첩은 지원되지 않습니다).

```cql
-- This is a comment
// This is a comment too
/* This is
   a multi-line comment */
```

---

### 구문(Statements)

CQL은 다음과 같은 범주로 나눌 수 있는 구문(statement)들로 구성됩니다.

- `데이터 정의(data-definition)` 구문: 데이터가 저장되는 방식을 정의하고 변경합니다(키스페이스 및 테이블).
- `데이터 조작(data-manipulation)` 구문: 데이터를 선택(select), 삽입(insert), 삭제(delete)합니다.
- `보조 인덱스(secondary-indexes)` 구문.
- `구체화된 뷰(materialized-views)` 구문.
- `역할(cql-roles)` 구문.
- `권한(cql-permissions)` 구문.
- `사용자 정의 함수(User-Defined Functions, UDFs)` 구문.
- `사용자 정의 타입(udts)` 구문.
- `트리거(cql-triggers)` 구문.

각 구문은 이 문서의 나머지 부분에서 설명됩니다(위 링크 참고).

---

### 준비된 구문(Prepared Statements)

CQL은 _준비된 구문(prepared statement)_ 을 지원합니다. 준비된 구문은 쿼리를 한 번만 파싱하되 서로 다른 구체적인 값으로 여러 번 실행할 수 있게 해 주는 최적화입니다.

최소 하나의 바인드 마커(`bind_marker` 참고)를 사용하는 모든 구문은 _준비(prepared)_ 되어야 합니다. 준비된 후에는 각 마커에 구체적인 값을 제공하여 구문을 _실행(executed)_ 할 수 있습니다. 구문을 준비하고 실행하는 과정은 사용하는 CQL 드라이버에 따라 다릅니다. 사용 중인 특정 드라이버의 문서를 반드시 참고하세요.

---

## 데이터 타입(Data Types)

CQL은 타입이 지정된(typed) 언어이며, [네이티브 타입(native types)](#네이티브-타입native-types), [컬렉션 타입(collection types)](#컬렉션collections), [사용자 정의 타입(user-defined types)](#사용자-정의-타입udt), [튜플 타입(tuple types)](#튜플tuples), [커스텀 타입(custom types)](#커스텀-타입custom-types) 등 풍부한 데이터 타입을 지원합니다.

```bnf
cql_type::= native_type | collection_type | user_defined_type | tuple_type | custom_type
```

---

### 네이티브 타입(Native types)

CQL이 지원하는 네이티브 타입은 다음과 같습니다.

```bnf
native_type::= ASCII | BIGINT | BLOB | BOOLEAN | COUNTER | DATE
| DECIMAL | DOUBLE | DURATION | FLOAT | INET | INT |
SMALLINT | TEXT | TIME | TIMESTAMP | TIMEUUID | TINYINT |
UUID | VARCHAR | VARINT | VECTOR
```

다음 표는 네이티브 데이터 타입에 대한 추가 정보와, 각 타입이 어떤 종류의 [상수(constants)](#상수constants)를 지원하는지를 보여 줍니다.

| 타입 | 지원하는 상수 | 설명 |
| --- | --- | --- |
| `ascii` | `string` | ASCII 문자열 |
| `bigint` | `integer` | 64비트 부호 있는 long |
| `blob` | `blob` | 임의의 바이트(검증 없음) |
| `boolean` | `boolean` | `true` 또는 `false` |
| `counter` | `integer` | 카운터 컬럼(64비트 부호 있는 값). 자세한 내용은 `counters` 참고. |
| `date` | `integer`, `string` | 날짜(대응하는 시간 값 없음). 자세한 내용은 아래 `dates` 참고. |
| `decimal` | `integer`, `float` | 가변 정밀도 십진수 |
| `double` | `integer`, `float` | 64비트 IEEE-754 부동소수점 |
| `duration` | `duration` | 나노초 정밀도의 기간(duration). 자세한 내용은 아래 `durations` 참고. |
| `float` | `integer`, `float` | 32비트 IEEE-754 부동소수점 |
| `inet` | `string` | IPv4(4바이트) 또는 IPv6(16바이트) IP 주소. `inet` 상수는 존재하지 않으므로 IP 주소는 문자열로 입력해야 합니다. |
| `int` | `integer` | 32비트 부호 있는 int |
| `smallint` | `integer` | 16비트 부호 있는 int |
| `text` | `string` | UTF8 인코딩 문자열 |
| `time` | `integer`, `string` | 나노초 정밀도의 시간(대응하는 날짜 값 없음). 자세한 내용은 아래 `times` 참고. |
| `timestamp` | `integer`, `string` | 밀리초 정밀도의 타임스탬프(날짜 및 시간). 자세한 내용은 아래 `timestamps` 참고. |
| `timeuuid` | `uuid` | 버전 1 [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier). 일반적으로 "충돌 없는(conflict-free)" 타임스탬프로 사용됩니다. `timeuuid-functions` 도 참고하세요. |
| `tinyint` | `integer` | 8비트 부호 있는 int |
| `uuid` | `uuid` | [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier)(모든 버전) |
| `varchar` | `string` | UTF8 인코딩 문자열 |
| `varint` | `integer` | 임의 정밀도 정수 |
| `vector` | `float` | null이 아닌 고정 길이의 평탄화된(flattened) float 값 배열. [CASSANDRA-18504](https://issues.apache.org/jira/browse/CASSANDRA-18504)에서 이 데이터 타입을 Cassandra 5.0에 추가했습니다. |

---

### 카운터(Counters)

`counter` 타입은 _카운터 컬럼(counter column)_ 을 정의하는 데 사용됩니다. 카운터 컬럼은 값이 64비트 부호 있는 정수인 컬럼으로, 증가(incrementing)와 감소(decrementing)라는 두 가지 연산을 지원합니다(문법은 [UPDATE](https://cassandra.apache.org/doc/latest/cassandra/cql/dml.html#update-statement) 구문 참고). 카운터의 값은 설정(set)할 수 없습니다. 카운터는 처음 증가/감소되기 전까지는 존재하지 않으며, 그 첫 증가/감소는 이전 값이 0이었던 것처럼 수행됩니다.

카운터에는 다음과 같은 중요한 제약이 있습니다.

- 테이블의 `PRIMARY KEY` 에 속하는 컬럼에는 사용할 수 없습니다.
- 카운터를 포함하는 테이블은 카운터만 포함할 수 있습니다. 다시 말해, `PRIMARY KEY` 외부의 모든 컬럼이 `counter` 타입을 가지거나, 아니면 어느 컬럼도 `counter` 타입을 갖지 않아야 합니다.
- 카운터는 [만료(expiration)](https://cassandra.apache.org/doc/latest/cassandra/cql/dml.html#writetime-and-ttl-function)를 지원하지 않습니다.
- 카운터의 삭제는 지원되지만, 처음 카운터를 삭제할 때만 동작이 보장됩니다. 다시 말해 삭제한 카운터를 다시 업데이트해서는 안 됩니다(그렇게 하면 올바른 동작이 보장되지 않습니다).
- 카운터 업데이트는 본질적으로 [멱등(idempotent)](https://en.wikipedia.org/wiki/Idempotence)이지 않습니다. 그 중요한 결과로, 카운터 업데이트가 예기치 않게 실패하면(타임아웃이나 코디네이터 노드와의 연결 손실 등) 클라이언트는 업데이트가 적용되었는지 확인할 방법이 없습니다. 특히 업데이트를 재실행(replay)하면 과다 집계(over count)로 이어질 수도, 아닐 수도 있습니다.

---

### 타임스탬프 다루기(Working with timestamps)

`timestamp` 타입의 값은 [에포크(the epoch)](https://en.wikipedia.org/wiki/Unix_time)라고 알려진 표준 기준 시각(1970년 1월 1일 00:00:00 GMT) 이후의 밀리초 수를 나타내는 64비트 부호 있는 정수로 인코딩됩니다.

타임스탬프는 CQL에서 `integer` 값으로 입력하거나, [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) 날짜를 나타내는 `string` 으로 입력할 수 있습니다. 예를 들어 아래의 모든 값은 GMT 기준 2011년 3월 2일 오전 04:05:00 에 대한 유효한 `timestamp` 값입니다.

- `1299038700000`
- `'2011-02-03 04:05+0000'`
- `'2011-02-03 04:05:00+0000'`
- `'2011-02-03 04:05:00.000+0000'`
- `'2011-02-03T04:05+0000'`
- `'2011-02-03T04:05:00+0000'`
- `'2011-02-03T04:05:00.000+0000'`

위의 `+0000` 은 RFC 822 형식의 4자리 시간대 지정자입니다. `+0000` 은 GMT를 가리킵니다. 미국 태평양 표준시(US Pacific Standard Time)는 `-0800` 입니다. 원한다면 시간대를 생략할 수 있으며(`'2011-02-03 04:05:00'`), 이 경우 날짜는 코디네이팅 Cassandra 노드에 설정된 시간대에 있는 것으로 해석됩니다. 그러나 서버의 시간대 설정에 의존하는 것은 본질적으로 위험하므로, 가능하다면 타임스탬프에는 항상 시간대를 명시하는 것이 권장됩니다.

하루 중 시각도 생략할 수 있으며(`'2011-02-03'` 또는 `'2011-02-03+0000'`), 이 경우 시각은 지정되거나 기본값인 시간대에서 00:00:00 으로 기본 설정됩니다. 다만 날짜 부분만 관련이 있다면 [date](#date-타입) 타입 사용을 고려하세요.

---

### date 타입

`date` 타입의 값은 범위의 중앙(2^31)에 "에포크"를 두고, 날짜 수를 나타내는 32비트 부호 없는 정수로 인코딩됩니다. 에포크는 1970년 1월 1일입니다.

[타임스탬프](#타임스탬프-다루기working-with-timestamps)와 마찬가지로 날짜는 `integer` 로 입력하거나 날짜 `string` 으로 입력할 수 있습니다. 후자의 경우 형식은 `yyyy-mm-dd` 이어야 합니다(예: `'2011-02-03'`).

---

### time 타입

`time` 타입의 값은 자정 이후의 나노초 수를 나타내는 64비트 부호 있는 정수로 인코딩됩니다.

[타임스탬프](#타임스탬프-다루기working-with-timestamps)와 마찬가지로 시간은 `integer` 로 입력하거나 시간을 나타내는 `string` 으로 입력할 수 있습니다. 후자의 경우 형식은 `hh:mm:ss[.fffffffff]` 이어야 합니다(초 미만 정밀도는 선택 사항이며, 제공하는 경우 나노초보다 작을 수 있습니다). 예를 들어 다음은 time에 대한 유효한 입력입니다.

- `'08:12:54'`
- `'08:12:54.123'`
- `'08:12:54.123456'`
- `'08:12:54.123456789'`

---

### duration 타입

`duration` 타입의 값은 가변 길이의 부호 있는 정수 3개로 인코딩됩니다. 첫 번째 정수는 개월 수를, 두 번째는 일 수를, 세 번째는 나노초 수를 나타냅니다. 이는 한 달의 일수가 달에 따라 다르고, 일광 절약 시간제(daylight saving)에 따라 하루가 23시간 또는 25시간이 될 수 있기 때문입니다. 내부적으로 개월 수와 일 수는 32비트 정수로, 나노초 수는 64비트 정수로 인코딩됩니다.

기간은 다음과 같이 입력할 수 있습니다.

- `(quantity unit)+` 형식(예: `12h30m`)이며, 여기서 unit은 다음 중 하나입니다.
  - `y`: 년(12개월)
  - `mo`: 월(1개월)
  - `w`: 주(7일)
  - `d`: 일(1일)
  - `h`: 시간(3,600,000,000,000 나노초)
  - `m`: 분(60,000,000,000 나노초)
  - `s`: 초(1,000,000,000 나노초)
  - `ms`: 밀리초(1,000,000 나노초)
  - `us` 또는 `µs`: 마이크로초(1000 나노초)
  - `ns`: 나노초(1 나노초)
- ISO 8601 형식: `P[n]Y[n]M[n]DT[n]H[n]M[n]S or P[n]W`
- ISO 8601 대체 형식: `P[YYYY]-[MM]-[DD]T[hh]:[mm]:[ss]`

예를 들면 다음과 같습니다.

```cql
INSERT INTO RiderResults (rider, race, result)
   VALUES ('Christopher Froome', 'Tour de France', 89h4m48s);
INSERT INTO RiderResults (rider, race, result)
   VALUES ('BARDET Romain', 'Tour de France', PT89H8M53S);
INSERT INTO RiderResults (rider, race, result)
   VALUES ('QUINTANA Nairo', 'Tour de France', P0000-00-00T89:09:09);
```

duration 컬럼은 테이블의 `PRIMARY KEY` 에 사용할 수 없습니다. 이 제약은 기간이 순서를 매길 수 없기 때문입니다. 날짜 맥락(date context) 없이는 `1mo` 가 `29d` 보다 큰지 알 수 없습니다.

`1d` 기간은 `24h` 기간과 같지 않습니다. duration 타입은 일광 절약 시간제를 지원할 수 있도록 만들어졌기 때문입니다.

---

### 컬렉션(Collections)

CQL은 세 가지 종류의 컬렉션을 지원합니다: `maps`, `sets`, `lists`. 이러한 컬렉션의 타입은 다음과 같이 정의됩니다.

```bnf
collection_type::= MAP '<' cql_type ',' cql_type '>'
	| SET '<' cql_type '>'
	| LIST '<' cql_type '>'
```

그리고 그 값은 컬렉션 리터럴(collection literal)을 사용하여 입력할 수 있습니다.

```bnf
collection_literal::= map_literal | set_literal | list_literal
map_literal::= '{' [ term ':' term (',' term ':' term)* ] '}'
set_literal::= '{' [ term (',' term)* ] '}'
list_literal::= '[' [ term (',' term)* ] ']'
```

다만 컬렉션 리터럴 내부에서는 `bind_marker` 와 `NULL` 모두 지원되지 않는다는 점에 유의하세요.

#### 주목할 만한 특성(Noteworthy characteristics)

컬렉션은 비교적 적은 양의 데이터를 저장/비정규화하기 위한 것입니다. "특정 사용자의 전화번호", "이메일에 적용된 라벨" 등에는 잘 동작합니다. 그러나 항목이 무한히 증가할 것으로 예상되는 경우("사용자가 보낸 모든 메시지", "센서에 등록된 이벤트" 등)에는 컬렉션이 적합하지 않으며, (클러스터링 컬럼이 있는) 별도의 테이블을 사용해야 합니다. 구체적으로 (frozen이 아닌) 컬렉션에는 다음과 같은 주목할 만한 특성과 제약이 있습니다.

- 개별 컬렉션은 내부적으로 인덱싱되지 않습니다. 즉, 컬렉션의 단일 요소에 접근하더라도 전체 컬렉션을 읽어야 합니다(그리고 컬렉션 읽기는 내부적으로 페이징되지 않습니다).
- set과 map에 대한 삽입 연산은 내부적으로 쓰기 전 읽기(read-before-write)를 결코 유발하지 않지만, list에 대한 일부 연산은 이를 유발합니다. 또한 list의 일부 연산은 본질적으로 멱등이 아니므로(자세한 내용은 아래 [lists](#lists) 섹션 참고), 타임아웃 발생 시 재시도가 문제가 됩니다. 따라서 가능하면 list보다 set을 선호하는 것이 권장됩니다.

이러한 제약 중 일부는 향후 제거되거나 개선될 수도, 아닐 수도 있지만, (단일) 컬렉션을 사용하여 대량의 데이터를 저장하는 것은 안티 패턴(anti-pattern)입니다.

#### Maps

`map` 은 키-값 쌍의 (정렬된) 집합으로, 키는 고유하며 map은 키를 기준으로 정렬됩니다. 다음과 같이 map을 정의하고 삽입할 수 있습니다.

```cql
CREATE TABLE users (
   id text PRIMARY KEY,
   name text,
   favs map<text, text> // A map of text keys, and text values
);

INSERT INTO users (id, name, favs)
   VALUES ('jsmith', 'John Smith', { 'fruit' : 'Apple', 'band' : 'Beatles' });

// Replace the existing map entirely.
UPDATE users SET favs = { 'fruit' : 'Banana' } WHERE id = 'jsmith';
```

또한 map은 다음을 지원합니다.

- 하나 이상의 요소 갱신 또는 삽입:

```cql
UPDATE users SET favs['author'] = 'Ed Poe' WHERE id = 'jsmith';
UPDATE users SET favs = favs + { 'movie' : 'Cassablanca', 'band' : 'ZZ Top' } WHERE id = 'jsmith';
```

- 하나 이상의 요소 제거(요소가 존재하지 않으면 제거는 아무 작업도 하지 않으며 오류도 발생하지 않습니다):

```cql
DELETE favs['author'] FROM users WHERE id = 'jsmith';
UPDATE users SET favs = favs - { 'movie', 'band'} WHERE id = 'jsmith';
```

`map` 에서 여러 요소를 제거할 때는 키의 `set` 을 map에서 제거한다는 점에 유의하세요.

마지막으로, `INSERT` 와 `UPDATE` 모두에 TTL을 사용할 수 있지만, 두 경우 모두 설정된 TTL은 새로 삽입/갱신된 요소에만 적용됩니다. 다시 말해 다음 구문은

```cql
UPDATE users USING TTL 10 SET favs['color'] = 'green' WHERE id = 'jsmith';
```

`{ 'color' : 'green' }` 레코드에만 TTL을 적용하며, map의 나머지 부분은 영향을 받지 않습니다.

#### Sets

`set` 은 고유한 값들의 (정렬된) 컬렉션입니다. 다음과 같이 set을 정의하고 삽입할 수 있습니다.

```cql
CREATE TABLE images (
   name text PRIMARY KEY,
   owner text,
   tags set<text> // A set of text values
);

INSERT INTO images (name, owner, tags)
   VALUES ('cat.jpg', 'jsmith', { 'pet', 'cute' });

// Replace the existing set entirely
UPDATE images SET tags = { 'kitten', 'cat', 'lol' } WHERE name = 'cat.jpg';
```

또한 set은 다음을 지원합니다.

- 하나 이상의 요소 추가(set이므로 이미 존재하는 요소를 삽입하는 것은 아무 작업도 하지 않습니다):

```cql
UPDATE images SET tags = tags + { 'gray', 'cuddly' } WHERE name = 'cat.jpg';
```

- 하나 이상의 요소 제거(요소가 존재하지 않으면 제거는 아무 작업도 하지 않으며 오류도 발생하지 않습니다):

```cql
UPDATE images SET tags = tags - { 'cat' } WHERE name = 'cat.jpg';
```

마지막으로 [set](#sets)의 경우 TTL은 새로 삽입된 값에만 적용됩니다.

#### Lists

> **참고(NOTE)**
>
> 위에서 언급했고 이 섹션 끝에서 더 자세히 다루듯이, list에는 제약과 특정 성능 고려 사항이 있으므로 사용하기 전에 이를 고려해야 합니다. 일반적으로 list 대신 [set](#sets)을 사용할 수 있다면 항상 set을 선호하세요.

`list` 는 고유하지 않은 값들의 (정렬된) 컬렉션으로, 요소는 list 내 위치(position)에 따라 순서가 매겨집니다. 다음과 같이 list를 정의하고 삽입할 수 있습니다.

```cql
CREATE TABLE plays (
    id text PRIMARY KEY,
    game text,
    players int,
    scores list<int> // A list of integers
)

INSERT INTO plays (id, game, players, scores)
           VALUES ('123-afde', 'quake', 3, [17, 4, 2]);

// Replace the existing list entirely
UPDATE plays SET scores = [ 3, 9, 4] WHERE id = '123-afde';
```

또한 list는 다음을 지원합니다.

- list에 값을 뒤에 추가(append)하거나 앞에 추가(prepend):

```cql
UPDATE plays SET players = 5, scores = scores + [ 14, 21 ] WHERE id = '123-afde';
UPDATE plays SET players = 6, scores = [ 3 ] + scores WHERE id = '123-afde';
```

> **경고(WARNING)**
>
> append와 prepend 연산은 본질적으로 멱등이 아닙니다. 특히 이러한 연산 중 하나가 타임아웃되면 재시도하는 것은 안전하지 않으며, 값을 두 번 추가하게 될 수도(또는 아닐 수도) 있습니다.

- list의 특정 위치에 값을 설정(해당 위치에 기존 요소가 있어야 함). 해당 위치가 없으면 오류가 발생합니다.

```cql
UPDATE plays SET scores[1] = 7 WHERE id = '123-afde';
```

- list에서 특정 위치의 요소를 제거(해당 위치에 기존 요소가 있어야 함). 해당 위치가 없으면 오류가 발생합니다. 이 연산은 list 크기를 한 요소만큼 줄이며, 이후의 모든 요소 위치가 하나씩 앞당겨집니다.

```cql
DELETE scores[1] FROM plays WHERE id = '123-afde';
```

- list에서 특정 값의 _모든_ 출현을 삭제(특정 요소가 list에 전혀 나타나지 않으면 단순히 무시되며 오류가 발생하지 않습니다):

```cql
UPDATE plays SET scores = scores - [ 12, 21 ] WHERE id = '123-afde';
```

> **경고(WARNING)**
>
> 위치로 요소를 설정 및 제거하는 연산과 특정 값의 출현을 제거하는 연산은 내부적으로 _쓰기 전 읽기(read-before-write)_ 를 유발합니다. 이러한 연산은 일반적인 갱신보다 느리게 실행되며 더 많은 리소스를 사용합니다(자체적인 비용이 있는 조건부 쓰기는 제외).

마지막으로 [list](#lists)의 경우 TTL은 새로 삽입된 값에만 적용됩니다.

#### 벡터 다루기(Working with vectors)

벡터(vector)는 특정 데이터 타입의 null이 아닌 값들로 이루어진 고정 크기 시퀀스입니다. list와 동일한 리터럴을 사용합니다.

다음과 같이 벡터를 정의, 삽입, 갱신할 수 있습니다.

```cql
CREATE TABLE plays (
    id text PRIMARY KEY,
    game text,
    players int,
    scores vector<int, 3> // A vector of 3 integers
)

INSERT INTO plays (id, game, players, scores)
           VALUES ('123-afde', 'quake', 3, [17, 4, 2]);

// Replace the existing vector entirely
UPDATE plays SET scores = [ 3, 9, 4] WHERE id = '123-afde';
```

벡터의 개별 값을 변경하는 것은 불가능하며, 벡터의 개별 요소를 선택하는 것도 불가능하다는 점에 유의하세요.

---

### 사용자 정의 타입(UDT)

CQL은 사용자 정의 타입(user-defined types, UDT)의 정의를 지원합니다. 이러한 타입은 아래에 설명된 `create_type_statement`, `alter_type_statement`, `drop_type_statement` 를 사용하여 생성, 수정, 제거할 수 있습니다. 그러나 일단 생성된 UDT는 단순히 그 이름으로 참조됩니다.

```bnf
user_defined_type::= udt_name
udt_name::= [ keyspace_name '.' ] identifier
```

#### UDT 생성

새로운 사용자 정의 타입은 다음과 같이 정의되는 `CREATE TYPE` 구문으로 생성합니다.

```bnf
create_type_statement::= CREATE TYPE [ IF NOT EXISTS ] udt_name
        '(' field_definition ( ',' field_definition)* ')'
field_definition::= identifier cql_type
```

UDT는 (해당 타입의 컬럼을 선언하는 데 사용되는) 이름을 가지며, 이름과 타입이 지정된 필드(field)들의 집합입니다. 필드 이름은 컬렉션이나 다른 UDT를 포함하여 어떤 타입이든 될 수 있습니다. 예를 들면 다음과 같습니다.

```cql
CREATE TYPE phone (
    country_code int,
    number text,
);

CREATE TYPE address (
    street text,
    city text,
    zip text,
    phones map<text, phone>
);

CREATE TABLE user (
    name text PRIMARY KEY,
    addresses map<text, frozen<address>>
);
```

UDT에 관해 유념해야 할 사항은 다음과 같습니다.

- 이미 존재하는 타입을 생성하려고 하면 `IF NOT EXISTS` 옵션을 사용하지 않는 한 오류가 발생합니다. 이 옵션을 사용하면 타입이 이미 존재할 경우 구문은 아무 작업도 하지 않습니다.
- 타입은 본질적으로 생성된 키스페이스에 묶여 있으며 해당 키스페이스에서만 사용할 수 있습니다. 생성 시 타입 이름 앞에 키스페이스 이름이 붙으면 해당 키스페이스에 생성됩니다. 그렇지 않으면 현재 키스페이스에 생성됩니다.
- Cassandra에서는 대부분의 경우 UDT를 frozen으로 만들어야 하므로, 위 테이블 정의에서 `frozen<address>` 를 사용했습니다.

#### UDT 리터럴

사용자 정의 타입이 생성된 후에는 UDT 리터럴을 사용하여 값을 입력할 수 있습니다.

```bnf
udt_literal::= '{' identifier ':' term ( ',' identifier ':' term)* '}'
```

다시 말해 UDT 리터럴은 [map](#maps) 리터럴과 비슷하지만, 그 키가 타입의 필드 이름이라는 점이 다릅니다. 예를 들어 이전 섹션에서 정의한 테이블에 다음과 같이 삽입할 수 있습니다.

```cql
INSERT INTO user (name, addresses)
   VALUES ('z3 Pr3z1den7', {
     'home' : {
        street: '1600 Pennsylvania Ave NW',
        city: 'Washington',
        zip: '20500',
        phones: { 'cell' : { country_code: 1, number: '202 456-1111' },
                  'landline' : { country_code: 1, number: '...' } }
     },
     'work' : {
        street: '1600 Pennsylvania Ave NW',
        city: 'Washington',
        zip: '20500',
        phones: { 'fax' : { country_code: 1, number: '...' } }
     }
  }
);
```

유효하려면 UDT 리터럴은 해당 타입이 정의한 필드만 포함할 수 있지만, 일부 필드는 생략할 수 있습니다(생략된 필드는 `NULL` 로 설정됩니다).

#### UDT 변경(Altering)

기존 사용자 정의 타입은 `ALTER TYPE` 구문으로 수정할 수 있습니다.

```bnf
alter_type_statement::= ALTER TYPE [ IF EXISTS ] udt_name alter_type_modification
alter_type_modification::= ADD [ IF NOT EXISTS ] field_definition
        | RENAME [ IF EXISTS ] identifier TO identifier (AND identifier TO identifier )*
```

타입이 존재하지 않으면 구문은 오류를 반환하지만, `IF EXISTS` 를 사용하면 연산은 아무 작업도 하지 않습니다. 다음을 할 수 있습니다.

- 타입에 새 필드 추가(`ALTER TYPE address ADD country text`). 추가 이전에 생성된 타입의 모든 값에서 그 새 필드는 `NULL` 이 됩니다. 새 필드가 이미 존재하면 구문은 오류를 반환하지만, `IF NOT EXISTS` 를 사용하면 연산은 아무 작업도 하지 않습니다.
- 타입의 필드 이름 변경. 필드가 존재하지 않으면 구문은 오류를 반환하지만, `IF EXISTS` 를 사용하면 연산은 아무 작업도 하지 않습니다.

```cql
ALTER TYPE address RENAME zip TO zipcode;
```

#### UDT 삭제(Dropping)

기존 사용자 정의 타입은 `DROP TYPE` 구문으로 삭제할 수 있습니다.

```bnf
drop_type_statement::= DROP TYPE [ IF EXISTS ] udt_name
```

타입을 삭제하면 해당 타입이 즉시, 되돌릴 수 없게 제거됩니다. 다만 다른 타입, 테이블, 함수에서 여전히 사용 중인 타입을 삭제하려고 하면 오류가 발생합니다.

삭제하려는 타입이 존재하지 않으면 오류가 반환되지만, `IF EXISTS` 를 사용하면 연산은 아무 작업도 하지 않습니다.

---

### 튜플(Tuples)

CQL은 튜플(tuple)과 튜플 타입도 지원합니다(요소들의 타입이 서로 다를 수 있습니다). 기능적으로 튜플은 익명 필드를 가진 익명 UDT로 생각할 수 있습니다. 튜플 타입과 튜플 리터럴은 다음과 같이 정의됩니다.

```bnf
tuple_type::= TUPLE '<' cql_type( ',' cql_type)* '>'
tuple_literal::= '(' term( ',' term )* ')'
```

그리고 다음과 같이 생성할 수 있습니다.

```cql
CREATE TABLE durations (
  event text,
  duration tuple<int, text>,
);

INSERT INTO durations (event, duration) VALUES ('ev1', (3, 'hours'));
```

컬렉션이나 UDT 같은 다른 합성 타입과 달리, 튜플은 (`frozen` 키워드 없이도) 항상 `frozen` 이며, 튜플의 일부 요소만 갱신하는 것(전체 튜플을 갱신하지 않고)은 불가능합니다. 또한 튜플 리터럴은 항상 해당 튜플 타입에 선언된 것과 동일한 수의 값을 가져야 합니다(이 값들 중 일부는 null일 수 있지만, 명시적으로 null로 선언해야 합니다).

---

### 커스텀 타입(Custom Types)

> **참고(NOTE)**
>
> 커스텀 타입(custom type)은 주로 하위 호환성(backward compatibility) 목적으로 존재하며, 사용은 권장되지 않습니다. 사용법이 복잡하고 사용자 친화적이지 않으며, 제공되는 다른 타입들, 특히 [사용자 정의 타입(user-defined types)](#사용자-정의-타입udt)으로 거의 항상 충분합니다.

커스텀 타입은 다음과 같이 정의됩니다.

```bnf
custom_type::= string
```

커스텀 타입은 서버 측 `AbstractType` 클래스를 확장하고 Cassandra가 로드할 수 있는 Java 클래스의 이름을 담은 `string` 입니다(따라서 Cassandra를 실행하는 모든 노드의 `CLASSPATH` 에 있어야 합니다). 그 클래스는 해당 타입에 어떤 값이 유효한지, 그리고 클러스터링 컬럼으로 사용될 때 어떻게 정렬되는지를 정의합니다. 그 외의 모든 목적에서, 커스텀 타입의 값은 `blob` 의 값과 동일하며, 특히 `blob` 리터럴 문법을 사용하여 입력할 수 있습니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL 레퍼런스](https://cassandra.apache.org/doc/latest/cassandra/cql/)
