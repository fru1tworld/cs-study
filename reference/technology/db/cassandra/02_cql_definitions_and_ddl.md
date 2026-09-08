# CQL 정의, 데이터 타입, DDL

## CQL 정의 및 데이터 타입

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/

<a id="정의definitions"></a>
### 정의(Definitions)

<a id="표기-규칙conventions"></a>

#### 표기 규칙(Conventions)

이 문서는 다음 표기 규칙으로 CQL 문법을 설명한다.

- 언어 규칙은 비공식적인 [BNF 변형(BNF variant)](http://en.wikipedia.org/wiki/Backus%E2%80%93Naur_Form#Variants) 표기법으로 제공됨
  - 특히 선택적(optional) 항목은 대괄호(`[ item ]`)로 표기하고, 반복(repeated) 항목은 별표(`*`)와 더하기 기호(`+`)로 표기함
  - 별표는 0개 이상과 일치(matches zero or more)하며, 더하기 기호는 1개 이상과 일치(matches one or more)함
- 문법은 편의를 위해 다음 규칙도 따름
  - 비종단(non-terminal) 항은 소문자로 표기하고(그리고 해당 정의로 링크되며), 종단 키워드(terminal keyword)는 "모두 대문자(all caps)"로 제공됨
  - 다만 키워드는 `식별자(identifier)`이므로 실제로는 대소문자를 구분하지 않음
  - 또한 일부 기초적인 구성은 정규식(regexp)으로 정의하며, 이는 `re(<some regular expression>)`로 표기함
- 문법은 문서화 목적으로 제공되며 일부 사소한 세부 사항은 생략함
  - 예를 들어 `CREATE TABLE` 구문에서 마지막 컬럼 정의 뒤의 쉼표는 선택 사항이지만 존재해도 지원됨
  - 이는 이 문서의 문법이 시사하는 바와 다름
  - 또한 문법이 허용한다고 해서 모든 것이 반드시 유효한 CQL인 것은 아님
- 본문 중에 나오는 키워드나 CQL 코드 조각은 `고정폭 글꼴(fixed-width font)`로 표시됨

<a id="식별자와-키워드identifiers-and-keywords"></a>

#### 식별자와 키워드(Identifiers and keywords)

테이블이나 컬럼 같은 객체는 식별자(identifier), 즉 이름으로 가리킨다. 따옴표 없는 식별자는 정규식 `[a-zA-Z][a-zA-Z0-9_]*`와 일치하는 토큰이다.

이 형태를 따르는 이름 중 `SELECT`나 `WITH`처럼 언어에서 정해진 의미로 쓰이는 것이 키워드(keyword)다. 대부분 예약되어 있으며, 전체 목록은 [부록 A(Appendix A)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/appendices.html)에서 확인할 수 있다.

- 식별자와 (따옴표로 묶이지 않은) 키워드는 대소문자를 구분하지 않음
  - 따라서 `SELECT` 는 `select` 나 `sElEcT` 와 동일하며, `myId` 는 `myid` 나 `MYID` 와 동일함
  - 흔히 사용되는 관례(특히 이 문서의 예제에서 사용하는 관례)는 키워드에는 대문자를, 그 외 식별자에는 소문자를 사용하는 것임

- 따옴표로 묶인 식별자(quoted identifier) 라고 불리는 두 번째 종류의 식별자도 있음
  - 이는 비어 있지 않은 임의의 문자 시퀀스를 큰따옴표(`"`)로 감싸서 정의함
  - 따옴표로 묶인 식별자는 결코 키워드가 아님
  - 따라서 `"select"` 는 예약 키워드가 아니며 컬럼을 가리키는 데 사용할 수 있음(다만 이런 사용은 매우 권장되지 않음). 반면 `select` 는 파싱 오류를 일으킴
  - 또한 따옴표로 묶이지 않은 식별자나 키워드와 달리, 따옴표로 묶인 식별자는 대소문자를 구분함(`"My Quoted Id"` 는 `"my quoted id"` 와 다름 ). 그러나 `[a-zA-Z][a-zA-Z0-9_]*` 와 일치하는 완전히 소문자인 따옴표 식별자는 큰따옴표를 제거하여 얻은 따옴표 없는 식별자와 동등(equivalent) 함(따라서 `"myid"` 는 `myid` 및 `myId` 와 동등하지만 `"myId"` 와는 다름). 따옴표로 묶인 식별자 내부에서는 큰따옴표 문자를 두 번 반복하여 이스케이프할 수 있으므로 `"foo "" bar"` 는 유효한 식별자임

> 참고(NOTE)

>

> _따옴표로 묶인 식별자_ 를 사용하면 임의의 이름으로 컬럼을 선언할 수 있는데, 이러한 이름이 서버가 사용하는 특정 이름과 충돌할 수 있음. 예를 들어 조건부 업데이트(conditional update)를 사용할 때 서버는 `"[applied]"` 라는 특수한 이름이 포함된 결과 집합(result set)으로 응답함. 만약 이러한 이름으로 컬럼을 선언했다면 일부 도구를 혼란스럽게 만들 수 있으므로 피해야 함. 일반적으로 따옴표 없는 식별자가 선호되지만, 따옴표로 묶인 식별자를 사용한다면 대괄호로 감싼 이름(예: `"[applied]"`)이나 함수 호출처럼 보이는 이름(예: `"f(x)"`)은 피하는 것이 강력히 권장됨.

보다 형식적으로는 다음과 같다.

```bnf
identifier::= unquoted_identifier | quoted_identifier
unquoted_identifier::= re('[a-zA-Z][a-zA-Z0-9_]*')
quoted_identifier::= '"' (any character where " can appear if doubled)+ '"'
```

<a id="상수constants"></a>

#### 상수(Constants)

- CQL은 다음과 같은 상수(constant) 를 정의함

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

다시 말해 다음과 같다.

- 문자열(string) 상수는 작은따옴표(`'`)로 감싼 임의의 문자 시퀀스임
  - 작은따옴표를 포함하려면 두 번 반복하면 됨
  - 예: `'It''s raining today'`. 이것은 큰따옴표를 사용하는 따옴표로 묶인 `식별자(identifier)` 와 혼동해서는 안 됨
  - 또는 임의의 문자 시퀀스를 두 개의 달러 문자(`$$`)로 감싸서 문자열을 정의할 수도 있으며, 이 경우 작은따옴표를 이스케이프하지 않고 사용할 수 있음(`$$It's raining today$$`). 후자의 형식은 함수 본문에서 작은따옴표를 이스케이프하지 않기 위해 [사용자 정의 함수(user-defined functions)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/functions.html#udfs)를 정의할 때 자주 사용됨(함수 본문에서는 작은따옴표가 `$$` 보다 더 자주 나타날 가능성이 높기 때문임).
- 정수(integer), 부동소수점(float), 불리언(boolean) 상수는 예상되는 대로 정의됨
  - 다만 float은 특수한 `NaN` 및 `Infinity` 상수를 허용함
- CQL은 [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier) 상수를 지원함
- blob의 내용은 16진수(hexadecimal)로 제공되며 `0x` 로 시작함
- 특수한 `NULL` 상수는 값의 부재(absence of value)를 나타냄

- 이러한 상수가 어떻게 타입이 지정되는지에 대해서는 [데이터 타입(Data types)](#데이터-타입data-types) 섹션 참고

<a id="항terms"></a>

#### 항(Terms)

- CQL에는 항(term) 이라는 개념이 있으며, 이는 CQL이 지원하는 값의 종류를 나타냄
  - 항은 다음과 같이 정의됨

```bnf
term::= constant | literal | function_call | arithmetic_operation | type_hint | bind_marker
literal::= collection_literal | vector_literal | udt_literal | tuple_literal
function_call::= identifier '(' [ term (',' term)* ] ')'
arithmetic_operation::= '-' term | term ('+' | '-' | '*' | '/' | '%') term
type_hint::= '(' cql_type ')' term
bind_marker::= '?' | ':' identifier
```

- 따라서 항은 다음 중 하나임

- [상수(constant)](#상수constants)
- [컬렉션(collection)](#컬렉션collections), 벡터(vector), [사용자 정의 타입(user-defined type)](#사용자-정의-타입udt) 또는 [튜플(tuple)](#튜플tuples)에 대한 리터럴(literal)
- [네이티브 함수(native function)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/functions.html) 또는 [사용자 정의 함수(user-defined function)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/functions.html) 중 하나인 [함수(function)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/functions.html) 호출
- 항 사이의 [산술 연산(arithmetic operation)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/operators.html)
- 타입 힌트(type hint)
- 실행 시점에 바인딩될 변수를 나타내는 바인드 마커(bind marker). 자세한 내용은 [준비된 구문(prepared-statements)](#준비된-구문prepared-statements) 섹션 참고
  - 바인드 마커는 익명(anonymous, `?`)이거나 이름이 지정된(named, `:some_name`) 형태일 수 있음
  - 후자의 형식은 바인딩 시 변수를 참조하는 더 편리한 방법을 제공하므로 일반적으로 선호돼야 함

<a id="주석comments"></a>

#### 주석(Comments)

- CQL의 주석은 이중 대시(`--`) 또는 이중 슬래시(`//`)로 시작하는 한 줄임

- 여러 줄 주석(multi-line comment)도 `/*` 와 `*/` 로 감싸서 지원됨(단, 중첩은 지원되지 않음).

```cql
-- This is a comment
// This is a comment too
/* This is
   a multi-line comment */
```

<a id="구문statements"></a>

#### 구문(Statements)

- CQL은 다음과 같은 범주로 나눌 수 있는 구문(statement)들로 구성됨

- `데이터 정의(data-definition)` 구문: 데이터가 저장되는 방식을 정의하고 변경함(키스페이스 및 테이블).
- `데이터 조작(data-manipulation)` 구문: 데이터를 선택(select), 삽입(insert), 삭제(delete)함
- `보조 인덱스(secondary-indexes)` 구문
- `구체화된 뷰(materialized-views)` 구문
- `역할(cql-roles)` 구문
- `권한(cql-permissions)` 구문
- `사용자 정의 함수(User-Defined Functions, UDFs)` 구문
- `사용자 정의 타입(udts)` 구문
- `트리거(cql-triggers)` 구문

- 각 구문은 이 문서의 나머지 부분에서 설명됨(위 링크 참고).

<a id="준비된-구문prepared-statements"></a>

#### 준비된 구문(Prepared Statements)

같은 쿼리를 값만 바꾸어 반복 실행할 때는 준비된 구문(prepared statement)을 사용한다. 쿼리를 한 번 파싱한 뒤 바인드 마커(`bind_marker` 참고)에 구체적인 값을 넣어 여러 번 실행하는 최적화다.

바인드 마커가 하나라도 있는 구문은 먼저 준비(prepared)해야 한다. 준비와 실행을 호출하는 방법은 CQL 드라이버마다 다르므로, 사용하는 드라이버의 문서에서 해당 API를 확인해야 한다.

<a id="데이터-타입data-types"></a>

### 데이터 타입(Data Types)

- CQL은 타입이 지정된(typed) 언어이며, [네이티브 타입(native types)](#네이티브-타입native-types), [컬렉션 타입(collection types)](#컬렉션collections), [사용자 정의 타입(user-defined types)](#사용자-정의-타입udt), [튜플 타입(tuple types)](#튜플tuples), [커스텀 타입(custom types)](#커스텀-타입custom-types) 등 다양한 데이터 타입을 지원함

```bnf
cql_type::= native_type | collection_type | user_defined_type | tuple_type | custom_type
```

<a id="네이티브-타입native-types"></a>

#### 네이티브 타입(Native types)

CQL이 지원하는 네이티브 타입은 다음과 같다.

```bnf
native_type::= ASCII | BIGINT | BLOB | BOOLEAN | COUNTER | DATE
| DECIMAL | DOUBLE | DURATION | FLOAT | INET | INT |
SMALLINT | TEXT | TIME | TIMESTAMP | TIMEUUID | TINYINT |
UUID | VARCHAR | VARINT | VECTOR
```

- 다음 목록은 네이티브 데이터 타입에 대한 추가 정보와, 각 타입이 어떤 종류의 [상수(constants)](#상수constants)를 지원하는지를 보여줌

- `ascii`: 지원 상수 `string`: ASCII 문자열
- `bigint`: 지원 상수 `integer`: 64비트 부호 있는 long
- `blob`: 지원 상수 `blob`: 임의의 바이트(검증 없음)
- `boolean`: 지원 상수 `boolean`: `true` 또는 `false`
- `counter`: 지원 상수 `integer`: 카운터 컬럼(64비트 부호 있는 값). 자세한 내용은 `counters` 참고
- `date`: 지원 상수 `integer`, `string`: 날짜(대응하는 시간 값 없음). 자세한 내용은 아래 `dates` 참고
- `decimal`: 지원 상수 `integer`, `float`: 가변 정밀도 십진수
- `double`: 지원 상수 `integer`, `float`: 64비트 IEEE-754 부동소수점
- `duration`: 지원 상수 `duration`: 나노초 정밀도의 기간(duration). 자세한 내용은 아래 `durations` 참고
- `float`: 지원 상수 `integer`, `float`: 32비트 IEEE-754 부동소수점
- `inet`: 지원 상수 `string`: IPv4(4바이트) 또는 IPv6(16바이트) IP 주소
  - `inet` 상수는 존재하지 않으므로 IP 주소는 문자열로 입력해야 함
- `int`: 지원 상수 `integer`: 32비트 부호 있는 int
- `smallint`: 지원 상수 `integer`: 16비트 부호 있는 int
- `text`: 지원 상수 `string`: UTF8 인코딩 문자열
- `time`: 지원 상수 `integer`, `string`: 나노초 정밀도의 시간(대응하는 날짜 값 없음). 자세한 내용은 아래 `times` 참고
- `timestamp`: 지원 상수 `integer`, `string`: 밀리초 정밀도의 타임스탬프(날짜 및 시간). 자세한 내용은 아래 `timestamps` 참고
- `timeuuid`: 지원 상수 `uuid`: 버전 1 [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier). 일반적으로 "충돌 없는(conflict-free)" 타임스탬프로 사용됨
  - `timeuuid-functions` 도 참고
- `tinyint`: 지원 상수 `integer`: 8비트 부호 있는 int
- `uuid`: 지원 상수 `uuid`: [UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier)(모든 버전)
- `varchar`: 지원 상수 `string`: UTF8 인코딩 문자열
- `varint`: 지원 상수 `integer`: 임의 정밀도 정수
- `vector`: 지원 상수 `float`: null이 아닌 고정 길이의 평탄화된(flattened) float 값 배열
  - [CASSANDRA-18504](https://issues.apache.org/jira/browse/CASSANDRA-18504)에서 이 데이터 타입을 Cassandra 5.0에 추가했음

<a id="카운터counters"></a>

#### 카운터(Counters)

`counter`는 64비트 부호 있는 정수를 증가하거나 감소시키는 컬럼이다. 값을 직접 설정할 수는 없으며, 갱신 문법은 [UPDATE](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html#update-statement) 구문을 따른다. 처음 증가하거나 감소시키기 전에는 카운터가 존재하지 않고, 첫 연산은 이전 값이 0인 것처럼 처리된다.

- 카운터에는 다음과 같은 중요한 제약이 있음

- 테이블의 `PRIMARY KEY` 에 속하는 컬럼에는 사용할 수 없음
- 카운터를 포함하는 테이블은 카운터만 포함할 수 있음
  - 다시 말해, `PRIMARY KEY` 외부의 모든 컬럼이 `counter` 타입을 가지거나, 아니면 어느 컬럼도 `counter` 타입을 갖지 않아야 함
- 카운터는 [만료(expiration)](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html#writetime-and-ttl-function)를 지원하지 않음
- 카운터의 삭제는 지원되지만, 처음 카운터를 삭제할 때만 동작이 보장됨
  - 다시 말해 삭제한 카운터를 다시 업데이트해서는 안 됨(그렇게 하면 올바른 동작이 보장되지 않음).
- 카운터 업데이트는 본질적으로 [멱등(idempotent)](https://en.wikipedia.org/wiki/Idempotence)이지 않음
  - 그 중요한 결과로, 카운터 업데이트가 예기치 않게 실패하면(타임아웃이나 코디네이터 노드와의 연결 손실 등) 클라이언트는 업데이트가 적용되었는지 확인할 방법이 없음
  - 특히 업데이트를 재실행(replay)하면 과다 집계(over count)로 이어질 수도, 아닐 수도 있음

<a id="타임스탬프-다루기working-with-timestamps"></a>

#### 타임스탬프 다루기(Working with timestamps)

- `timestamp` 타입의 값은 [에포크(the epoch)](https://en.wikipedia.org/wiki/Unix_time)라고 알려진 표준 기준 시각(1970년 1월 1일 00:00:00 GMT) 이후의 밀리초 수를 나타내는 64비트 부호 있는 정수로 인코딩됨

- 타임스탬프는 CQL에서 `integer` 값으로 입력하거나, [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) 날짜를 나타내는 `string` 으로 입력할 수 있음
  - 예를 들어 아래의 모든 값은 GMT 기준 2011년 3월 2일 오전 04:05:00 에 대한 유효한 `timestamp` 값임

- `1299038700000`
- `'2011-02-03 04:05+0000'`
- `'2011-02-03 04:05:00+0000'`
- `'2011-02-03 04:05:00.000+0000'`
- `'2011-02-03T04:05+0000'`
- `'2011-02-03T04:05:00+0000'`
- `'2011-02-03T04:05:00.000+0000'`

- 위의 `+0000` 은 RFC 822 형식의 4자리 시간대 지정자임
  - `+0000` 은 GMT를 가리킴
  - 미국 태평양 표준시(US Pacific Standard Time)는 `-0800` 임
  - 원한다면 시간대를 생략할 수 있으며(`'2011-02-03 04:05:00'`), 이 경우 날짜는 코디네이팅 Cassandra 노드에 설정된 시간대에 있는 것으로 해석됨
  - 그러나 서버의 시간대 설정에 의존하는 것은 본질적으로 위험하므로, 가능하다면 타임스탬프에는 항상 시간대를 명시하는 것이 권장됨

- 하루 중 시각도 생략할 수 있으며(`'2011-02-03'` 또는 `'2011-02-03+0000'`), 이 경우 시각은 지정되거나 기본값인 시간대에서 00:00:00 으로 기본 설정됨
  - 다만 날짜 부분만 관련이 있다면 [date](#date-타입) 타입 사용을 고려

<a id="date-타입"></a>

#### date 타입

- `date` 타입의 값은 범위의 중앙(2^31)에 "에포크"를 두고, 날짜 수를 나타내는 32비트 부호 없는 정수로 인코딩됨
  - 에포크는 1970년 1월 1일임

- [타임스탬프](#타임스탬프-다루기working-with-timestamps)와 마찬가지로 날짜는 `integer` 로 입력하거나 날짜 `string` 으로 입력할 수 있음
  - 후자의 경우 형식은 `yyyy-mm-dd` 이어야 함(예: `'2011-02-03'`).

<a id="time-타입"></a>

#### time 타입

- `time` 타입의 값은 자정 이후의 나노초 수를 나타내는 64비트 부호 있는 정수로 인코딩됨

- [타임스탬프](#타임스탬프-다루기working-with-timestamps)와 마찬가지로 시간은 `integer` 로 입력하거나 시간을 나타내는 `string` 으로 입력할 수 있음
  - 후자의 경우 형식은 `hh:mm:ss[.fffffffff]` 이어야 함(초 미만 정밀도는 선택 사항이며, 제공하는 경우 나노초보다 작을 수 있음). 예를 들어 다음은 time에 대한 유효한 입력임

- `'08:12:54'`
- `'08:12:54.123'`
- `'08:12:54.123456'`
- `'08:12:54.123456789'`

<a id="duration-타입"></a>

#### duration 타입

한 달의 일수는 달마다 다르고, 일광 절약 시간제(daylight saving)에 따라 하루가 23시간이나 25시간이 될 수도 있다. 그래서 `duration`은 기간을 하나의 시간 단위로 환산하지 않고 개월 수, 일 수, 나노초 수를 나누어 저장한다.

이 세 값은 가변 길이의 부호 있는 정수 3개로 인코딩된다. 첫 번째와 두 번째인 개월 수와 일 수는 내부적으로 32비트 정수, 세 번째인 나노초 수는 64비트 정수를 사용한다.

- 기간은 다음과 같이 입력할 수 있음

- `(quantity unit)+` 형식(예: `12h30m`)이며, 여기서 unit은 다음 중 하나임
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

예를 들면 다음과 같다.

```cql
INSERT INTO RiderResults (rider, race, result)
   VALUES ('Christopher Froome', 'Tour de France', 89h4m48s);
INSERT INTO RiderResults (rider, race, result)
   VALUES ('BARDET Romain', 'Tour de France', PT89H8M53S);
INSERT INTO RiderResults (rider, race, result)
   VALUES ('QUINTANA Nairo', 'Tour de France', P0000-00-00T89:09:09);
```

- duration 컬럼은 테이블의 `PRIMARY KEY` 에 사용할 수 없음
  - 이 제약은 기간이 순서를 매길 수 없기 때문임
  - 날짜 맥락(date context) 없이는 `1mo` 가 `29d` 보다 큰지 알 수 없음

- `1d` 기간은 `24h` 기간과 같지 않음
  - duration 타입은 일광 절약 시간제를 지원할 수 있도록 만들어졌기 때문임

<a id="컬렉션collections"></a>

#### 컬렉션(Collections)

- CQL은 세 가지 종류의 컬렉션을 지원함: `maps`, `sets`, `lists`. 이러한 컬렉션의 타입은 다음과 같이 정의됨

```bnf
collection_type::= MAP '<' cql_type ',' cql_type '>'
	| SET '<' cql_type '>'
	| LIST '<' cql_type '>'
```

- 그리고 그 값은 컬렉션 리터럴(collection literal)을 사용하여 입력할 수 있음

```bnf
collection_literal::= map_literal | set_literal | list_literal
map_literal::= '{' [ term ':' term (',' term ':' term)* ] '}'
set_literal::= '{' [ term (',' term)* ] '}'
list_literal::= '[' [ term (',' term)* ] ']'
```

- 다만 컬렉션 리터럴 내부에서는 `bind_marker` 와 `NULL` 모두 지원되지 않는다는 점에 유의

- 주목할 만한 특성(Noteworthy characteristics)

  - 컬렉션은 비교적 적은 양의 데이터를 저장/비정규화하기 위한 것임
    - "특정 사용자의 전화번호", "이메일에 적용된 라벨" 등에는 잘 동작함
    - 그러나 항목이 무한히 증가할 것으로 예상되는 경우("사용자가 보낸 모든 메시지", "센서에 등록된 이벤트" 등)에는 컬렉션이 적합하지 않으며, (클러스터링 컬럼이 있는) 별도의 테이블을 사용해야 함
    - 구체적으로 (frozen이 아닌) 컬렉션에는 다음과 같은 주목할 만한 특성과 제약이 있음

  - 개별 컬렉션은 내부적으로 인덱싱되지 않음
    - 즉, 컬렉션의 단일 요소에 접근하더라도 전체 컬렉션을 읽어야 함(그리고 컬렉션 읽기는 내부적으로 페이징되지 않음).
  - set과 map에 대한 삽입 연산은 내부적으로 쓰기 전 읽기(read-before-write)를 결코 유발하지 않지만, list에 대한 일부 연산은 이를 유발함
    - 또한 list의 일부 연산은 본질적으로 멱등이 아니므로(자세한 내용은 아래 [lists](#lists) 섹션 참고), 타임아웃 발생 시 재시도가 문제가 됨
    - 따라서 가능하면 list보다 set을 선호하는 것이 권장됨

  - 이러한 제약 중 일부는 향후 제거되거나 개선될 수도, 아닐 수도 있지만, (단일) 컬렉션을 사용하여 대량의 데이터를 저장하는 것은 안티 패턴(anti-pattern)임

- <a id="maps"></a> Maps

  - `map` 은 키-값 쌍의 (정렬된) 집합으로, 키는 고유하며 map은 키를 기준으로 정렬됨
    - 다음과 같이 map을 정의하고 삽입할 수 있음

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

  - 또한 map은 다음을 지원함

  - 하나 이상의 요소 갱신 또는 삽입:

  ```cql
  UPDATE users SET favs['author'] = 'Ed Poe' WHERE id = 'jsmith';
  UPDATE users SET favs = favs + { 'movie' : 'Cassablanca', 'band' : 'ZZ Top' } WHERE id = 'jsmith';
  ```

  - 하나 이상의 요소 제거(요소가 존재하지 않으면 제거는 아무 작업도 하지 않으며 오류도 발생하지 않음):

  ```cql
  DELETE favs['author'] FROM users WHERE id = 'jsmith';
  UPDATE users SET favs = favs - { 'movie', 'band'} WHERE id = 'jsmith';
  ```

  - `map` 에서 여러 요소를 제거할 때는 키의 `set` 을 map에서 제거한다는 점에 유의

  - 마지막으로, `INSERT` 와 `UPDATE` 모두에 TTL을 사용할 수 있지만, 두 경우 모두 설정된 TTL은 새로 삽입/갱신된 요소에만 적용됨
    - 다시 말해 다음 구문은

  ```cql
  UPDATE users USING TTL 10 SET favs['color'] = 'green' WHERE id = 'jsmith';
  ```

  - `{ 'color' : 'green' }` 레코드에만 TTL을 적용하며, map의 나머지 부분은 영향을 받지 않음

- <a id="sets"></a> Sets

  - `set` 은 고유한 값들의 (정렬된) 컬렉션임
    - 다음과 같이 set을 정의하고 삽입할 수 있음

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

  - 또한 set은 다음을 지원함

  - 하나 이상의 요소 추가(set이므로 이미 존재하는 요소를 삽입하는 것은 아무 작업도 하지 않음):

  ```cql
  UPDATE images SET tags = tags + { 'gray', 'cuddly' } WHERE name = 'cat.jpg';
  ```

  - 하나 이상의 요소 제거(요소가 존재하지 않으면 제거는 아무 작업도 하지 않으며 오류도 발생하지 않음):

  ```cql
  UPDATE images SET tags = tags - { 'cat' } WHERE name = 'cat.jpg';
  ```

  - 마지막으로 [set](#sets)의 경우 TTL은 새로 삽입된 값에만 적용됨

- <a id="lists"></a> Lists

  > 참고(NOTE)

  >

  > 위에서 언급했고 이 섹션 끝에서 더 자세히 다루듯이, list에는 제약과 특정 성능 고려 사항이 있으므로 사용하기 전에 이를 고려해야 함. 일반적으로 list 대신 [set](#sets)을 사용할 수 있다면 항상 set을 선호.

  - `list` 는 고유하지 않은 값들의 (정렬된) 컬렉션으로, 요소는 list 내 위치(position)에 따라 순서가 매겨짐
    - 다음과 같이 list를 정의하고 삽입할 수 있음

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

  - 또한 list는 다음을 지원함

  - list에 값을 뒤에 추가(append)하거나 앞에 추가(prepend):

  ```cql
  UPDATE plays SET players = 5, scores = scores + [ 14, 21 ] WHERE id = '123-afde';
  UPDATE plays SET players = 6, scores = [ 3 ] + scores WHERE id = '123-afde';
  ```

  > 경고(WARNING)

  >

  > append와 prepend 연산은 본질적으로 멱등이 아님. 특히 이러한 연산 중 하나가 타임아웃되면 재시도하는 것은 안전하지 않으며, 값을 두 번 추가하게 될 수도(또는 아닐 수도) 있음.

  - list의 특정 위치에 값을 설정(해당 위치에 기존 요소가 있어야 함). 해당 위치가 없으면 오류가 발생함

  ```cql
  UPDATE plays SET scores[1] = 7 WHERE id = '123-afde';
  ```

  - list에서 특정 위치의 요소를 제거(해당 위치에 기존 요소가 있어야 함). 해당 위치가 없으면 오류가 발생함
    - 이 연산은 list 크기를 한 요소만큼 줄이며, 이후의 모든 요소 위치가 하나씩 앞당겨짐

  ```cql
  DELETE scores[1] FROM plays WHERE id = '123-afde';
  ```

  - list에서 특정 값의 모든 출현을 삭제(특정 요소가 list에 전혀 나타나지 않으면 단순히 무시되며 오류가 발생하지 않음):

  ```cql
  UPDATE plays SET scores = scores - [ 12, 21 ] WHERE id = '123-afde';
  ```

  > 경고(WARNING)

  >

  > 위치로 요소를 설정 및 제거하는 연산과 특정 값의 출현을 제거하는 연산은 내부적으로 _쓰기 전 읽기(read-before-write)_ 를 유발함. 이러한 연산은 일반적인 갱신보다 느리게 실행되며 더 많은 리소스를 사용함(자체적인 비용이 있는 조건부 쓰기는 제외).

  - 마지막으로 [list](#lists)의 경우 TTL은 새로 삽입된 값에만 적용됨

- 벡터 다루기(Working with vectors)

  - 벡터(vector)는 특정 데이터 타입의 null이 아닌 값들로 이루어진 고정 크기 시퀀스임
    - list와 동일한 리터럴을 사용함

  - 다음과 같이 벡터를 정의, 삽입, 갱신할 수 있음

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

  - 벡터의 개별 값을 변경하는 것은 불가능하며, 벡터의 개별 요소를 선택하는 것도 불가능하다는 점에 유의

<a id="사용자-정의-타입udt"></a>

#### 사용자 정의 타입(UDT)

- CQL은 사용자 정의 타입(user-defined types, UDT)의 정의를 지원함
  - 이러한 타입은 아래에 설명된 `create_type_statement`, `alter_type_statement`, `drop_type_statement` 를 사용하여 생성, 수정, 제거할 수 있음
  - 그러나 일단 생성된 UDT는 단순히 그 이름으로 참조됨

```bnf
user_defined_type::= udt_name
udt_name::= [ keyspace_name '.' ] identifier
```

- UDT 생성

  - 새로운 사용자 정의 타입은 다음과 같이 정의되는 `CREATE TYPE` 구문으로 생성함

  ```bnf
  create_type_statement::= CREATE TYPE [ IF NOT EXISTS ] udt_name
          '(' field_definition ( ',' field_definition)* ')'
  field_definition::= identifier cql_type
  ```

  - UDT는 (해당 타입의 컬럼을 선언하는 데 사용되는) 이름을 가지며, 이름과 타입이 지정된 필드(field)들의 집합임
    - 필드 이름은 컬렉션이나 다른 UDT를 포함하여 어떤 타입이든 될 수 있음
    - 예를 들면 다음과 같음

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

  - UDT에 관해 유념해야 할 사항은 다음과 같음

  - 이미 존재하는 타입을 생성하려고 하면 `IF NOT EXISTS` 옵션을 사용하지 않는 한 오류가 발생함
    - 이 옵션을 사용하면 타입이 이미 존재할 경우 구문은 아무 작업도 하지 않음
  - 타입은 본질적으로 생성된 키스페이스에 묶여 있으며 해당 키스페이스에서만 사용할 수 있음
    - 생성 시 타입 이름 앞에 키스페이스 이름이 붙으면 해당 키스페이스에 생성됨
    - 그렇지 않으면 현재 키스페이스에 생성됨
  - Cassandra에서는 대부분의 경우 UDT를 frozen으로 만들어야 하므로, 위 테이블 정의에서 `frozen<address>` 를 사용했음

- UDT 리터럴

  - 사용자 정의 타입이 생성된 후에는 UDT 리터럴을 사용하여 값을 입력할 수 있음

  ```bnf
  udt_literal::= '{' identifier ':' term ( ',' identifier ':' term)* '}'
  ```

  - 다시 말해 UDT 리터럴은 [map](#maps) 리터럴과 비슷하지만, 그 키가 타입의 필드 이름이라는 점이 다름
    - 예를 들어 이전 섹션에서 정의한 테이블에 다음과 같이 삽입할 수 있음

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

  - 유효하려면 UDT 리터럴은 해당 타입이 정의한 필드만 포함할 수 있지만, 일부 필드는 생략할 수 있음(생략된 필드는 `NULL` 로 설정됨).

- UDT 변경(Altering)

  - 기존 사용자 정의 타입은 `ALTER TYPE` 구문으로 수정할 수 있음

  ```bnf
  alter_type_statement::= ALTER TYPE [ IF EXISTS ] udt_name alter_type_modification
  alter_type_modification::= ADD [ IF NOT EXISTS ] field_definition
          | RENAME [ IF EXISTS ] identifier TO identifier (AND identifier TO identifier )*
  ```

  - 타입이 존재하지 않으면 구문은 오류를 반환하지만, `IF EXISTS` 를 사용하면 연산은 아무 작업도 하지 않음
    - 다음을 할 수 있음

  - 타입에 새 필드 추가(`ALTER TYPE address ADD country text`). 추가 이전에 생성된 타입의 모든 값에서 그 새 필드는 `NULL` 이 됨
    - 새 필드가 이미 존재하면 구문은 오류를 반환하지만, `IF NOT EXISTS` 를 사용하면 연산은 아무 작업도 하지 않음
  - 타입의 필드 이름 변경
    - 필드가 존재하지 않으면 구문은 오류를 반환하지만, `IF EXISTS` 를 사용하면 연산은 아무 작업도 하지 않음

  ```cql
  ALTER TYPE address RENAME zip TO zipcode;
  ```

- UDT 삭제(Dropping)

  - 기존 사용자 정의 타입은 `DROP TYPE` 구문으로 삭제할 수 있음

  ```bnf
  drop_type_statement::= DROP TYPE [ IF EXISTS ] udt_name
  ```

  - 타입을 삭제하면 해당 타입이 즉시, 되돌릴 수 없게 제거됨
    - 다만 다른 타입, 테이블, 함수에서 여전히 사용 중인 타입을 삭제하려고 하면 오류가 발생함

  - 삭제하려는 타입이 존재하지 않으면 오류가 반환되지만, `IF EXISTS` 를 사용하면 연산은 아무 작업도 하지 않음

<a id="튜플tuples"></a>

#### 튜플(Tuples)

- CQL은 튜플(tuple)과 튜플 타입도 지원함(요소들의 타입이 서로 다를 수 있음). 기능적으로 튜플은 익명 필드를 가진 익명 UDT로 생각할 수 있음
  - 튜플 타입과 튜플 리터럴은 다음과 같이 정의됨

```bnf
tuple_type::= TUPLE '<' cql_type( ',' cql_type)* '>'
tuple_literal::= '(' term( ',' term )* ')'
```

- 그리고 다음과 같이 생성할 수 있음

```cql
CREATE TABLE durations (
  event text,
  duration tuple<int, text>,
);

INSERT INTO durations (event, duration) VALUES ('ev1', (3, 'hours'));
```

- 컬렉션이나 UDT 같은 다른 합성 타입과 달리, 튜플은 (`frozen` 키워드 없이도) 항상 `frozen` 이며, 튜플의 일부 요소만 갱신하는 것(전체 튜플을 갱신하지 않고)은 불가능함
  - 또한 튜플 리터럴은 항상 해당 튜플 타입에 선언된 것과 동일한 수의 값을 가져야 함(이 값들 중 일부는 null일 수 있지만, 명시적으로 null로 선언해야 함).

<a id="커스텀-타입custom-types"></a>

#### 커스텀 타입(Custom Types)

> 참고(NOTE)

>

> 커스텀 타입(custom type)은 주로 하위 호환성(backward compatibility) 목적으로 존재하며, 사용은 권장되지 않음. 사용법이 복잡하고 사용자 친화적이지 않으며, 제공되는 다른 타입들, 특히 [사용자 정의 타입(user-defined types)](#사용자-정의-타입udt)으로 거의 항상 충분함.

- 커스텀 타입은 다음과 같이 정의됨

```bnf
custom_type::= string
```

- 커스텀 타입은 서버 측 `AbstractType` 클래스를 확장하고 Cassandra가 로드할 수 있는 Java 클래스의 이름을 담은 `string` 임(따라서 Cassandra를 실행하는 모든 노드의 `CLASSPATH` 에 있어야 함). 그 클래스는 해당 타입에 어떤 값이 유효한지, 그리고 클러스터링 컬럼으로 사용될 때 어떻게 정렬되는지를 정의함
  - 그 외의 모든 목적에서, 커스텀 타입의 값은 `blob` 의 값과 동일하며, 특히 `blob` 리터럴 문법을 사용하여 입력할 수 있음

<a id="참고-자료"></a>

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL 레퍼런스](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/)

## CQL 데이터 정의 (DDL)

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/cql/ddl.html

<a id="개요"></a>
### 개요

데이터 타입을 정했다면 데이터를 담을 키스페이스(keyspace)와 테이블(table)을 정의해야 한다. 데이터 정의(Data Definition, DDL) 구문은 이 저장 구조를 만들고 변경하는 데 사용한다.

- 키스페이스(keyspace): 테이블의 묶음을 위한 네임스페이스(namespace)로, 복제 전략(replication strategy)과 몇 가지 옵션을 정의함
- 테이블(table): 행(row)들의 집합이며, 각 행은 미리 정의된 컬럼(column)들로 구성됨

- 권장되는 모범 사례(best practice)는 "애플리케이션당 하나의 키스페이스(one keyspace per application)"를 두는 것임

#### 명명 규칙(Naming Rules)

- 키스페이스와 테이블의 이름은 영숫자(alphanumeric) 문자와 밑줄(`_`)만 사용할 수 있음

- 키스페이스 이름: 최대 48자(characters)
- 테이블 이름: 최대 222자(characters)

- 기본적으로 이름은 대소문자를 구분하지 않음(case-insensitive). 대소문자를 구분하려면 큰따옴표(`"`)로 감싸야 함

<a id="키스페이스keyspace"></a>

### 키스페이스(Keyspace)

키스페이스는 테이블을 담는 최상위 네임스페이스이면서 복제(replication) 설정을 정하는 단위다. 데이터를 몇 개 노드에 둘지, 어떤 전략으로 복제할지를 여기서 지정하면 그 안의 테이블에 적용된다.

<a id="create-keyspace"></a>

### CREATE KEYSPACE

- 키스페이스를 생성하는 구문임

- 문법(Grammar):

```
create_keyspace_statement::= CREATE KEYSPACE [ IF NOT EXISTS ] keyspace_name
	WITH options
```

- 예제:

```cql
CREATE KEYSPACE excelsior
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

CREATE KEYSPACE excalibur
   WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 1, 'DC2' : 3}
   AND durable_writes = false;
```

- `IF NOT EXISTS`를 사용하면 동일한 이름의 키스페이스가 이미 존재할 때 오류가 발생하지 않음(이 경우 아무 작업도 수행되지 않음).

- `CREATE KEYSPACE`에서 `replication` 속성은 필수이며, 사용할 복제 전략 클래스를 정의하는 `'class'` 하위 옵션(sub-option)을 최소한 반드시 포함해야 함

<a id="복제-전략replication-strategy"></a>

#### 복제 전략(Replication Strategy)

- Cassandra는 다음 두 가지 주요 복제 전략을 지원함

<a id="simplestrategy"></a>

#### SimpleStrategy

- `SimpleStrategy`는 클러스터 전체에 대해 단순한 복제 계수(replication factor)를 정의하는 간단한 전략임
  - 필수 하위 옵션으로 `'replication_factor'`를 지정함

```cql
CREATE KEYSPACE excelsior
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};
```

- `SimpleStrategy`는 데이터센터(datacenter) 레이아웃을 고려하지 않으므로, 일반적으로 프로덕션(production) 환경에서는 권장되지 않음

<a id="networktopologystrategy"></a>

#### NetworkTopologyStrategy

- `NetworkTopologyStrategy`는 프로덕션 환경에 적합한 전략으로, 데이터센터별(per-datacenter)로 복제 계수를 개별 지정할 수 있음

```cql
CREATE KEYSPACE excalibur
   WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 1, 'DC2' : 3};
```

- 위 예제는 `DC1` 데이터센터에 1개의 복제본을, `DC2` 데이터센터에 3개의 복제본을 둠

- `NetworkTopologyStrategy`는 새로운 데이터센터가 추가될 때 복제 계수를 자동으로 확장(auto-expansion)하는 기능을 제공함
  - 또한 일시적 복제(transient replication)를 지원하여 `'DC1': '3/1'`과 같은 형식으로 지정할 수 있음
  - 여기서 `3`은 전체 복제본 수이고, `1`은 그 중 일시적 복제본(transient replica) 수를 의미함

<a id="durable_writes"></a>

#### durable_writes

- `durable_writes`는 불리언(boolean) 옵션으로 기본값은 `true`임
  - 이 옵션은 커밋 로그(commit log) 사용 여부를 제어함
  - `false`로 설정하면 해당 키스페이스의 쓰기에 커밋 로그를 사용하지 않음

> 경고: `durable_writes`를 `false`로 설정하면 데이터 손실(data loss)의 위험이 있으므로 주의해서 사용해야 함.

```cql
CREATE KEYSPACE excalibur
   WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1' : 1, 'DC2' : 3}
   AND durable_writes = false;
```

<a id="use"></a>

### USE

- 현재 세션(session)에서 사용할 키스페이스를 지정함
  - 이후 키스페이스를 명시하지 않은 모든 구문은 이 키스페이스를 기준으로 동작함

- 문법(Grammar):

```
use_statement::= USE keyspace_name
```

- 예제:

```cql
USE excelsior;
```

<a id="alter-keyspace"></a>

### ALTER KEYSPACE

- 기존 키스페이스의 옵션을 변경함
  - 문법은 `CREATE KEYSPACE`와 동일하게 `WITH` 절을 사용함

- 문법(Grammar):

```
alter_keyspace_statement::= ALTER KEYSPACE [ IF EXISTS ] keyspace_name
	WITH options
```

- 예제:

```cql
ALTER KEYSPACE excelsior
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 4};
```

- 위 예제는 복제 계수를 4로 변경함
  - `IF EXISTS`를 사용하면 해당 키스페이스가 존재하지 않을 때 오류가 발생하지 않음

<a id="drop-keyspace"></a>

### DROP KEYSPACE

- 키스페이스를 삭제함
  - 키스페이스에 속한 모든 테이블과 그 안의 데이터, 인덱스(index) 등도 함께 영구적으로 삭제됨

- 문법(Grammar):

```
drop_keyspace_statement::= DROP KEYSPACE [ IF EXISTS ] keyspace_name
```

- 예제:

```cql
DROP KEYSPACE excelsior;
```

- `IF EXISTS`를 사용하면 해당 키스페이스가 존재하지 않을 때 오류가 발생하지 않음

<a id="create-table"></a>

### CREATE TABLE

- 테이블을 생성함
  - 테이블은 행(row)들의 집합이며, 각 행은 이름과 타입(type)으로 정의된 컬럼들로 구성됨

- 문법(Grammar):

```
create_table_statement::= CREATE TABLE [ IF NOT EXISTS ] table_name '('
	column_definition  ( ',' column_definition )*
	[ ',' PRIMARY KEY '(' primary_key ')' ]
	 ')' [ WITH table_options ]
column_definition::= column_name cql_type [ STATIC ] [ column_mask ] [ PRIMARY KEY]
column_mask::= MASKED WITH ( DEFAULT | function_name '(' term ( ',' term )* ')' )
primary_key::= partition_key [ ',' clustering_columns ]
partition_key::= column_name  | '(' column_name ( ',' column_name )* ')'
clustering_columns::= column_name ( ',' column_name )*
table_options::= COMPACT STORAGE [ AND table_options ]
	| CLUSTERING ORDER BY '(' clustering_order ')'
	[ AND table_options ]  | options
clustering_order::= column_name (ASC | DESC) ( ',' column_name (ASC | DESC) )*
```

- 주요 예제:

```cql
CREATE TABLE monkey_species (
    species text PRIMARY KEY,
    common_name text,
    population varint,
    average_size int
) WITH comment='Important biological records';

CREATE TABLE timeline (
    userid uuid,
    posted_month int,
    posted_time uuid,
    body text,
    posted_by text,
    PRIMARY KEY (userid, posted_month, posted_time)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };

CREATE TABLE loads (
    machine inet,
    cpu int,
    mtime timeuuid,
    load float,
    PRIMARY KEY ((machine, cpu), mtime)
) WITH CLUSTERING ORDER BY (mtime DESC);
```

- `IF NOT EXISTS`를 사용하면 동일한 이름의 테이블이 이미 존재할 때 오류가 발생하지 않음

> 참고: `COMPACT STORAGE` 옵션은 과거 Thrift 호환성을 위해 존재하던 레거시(legacy) 옵션으로, 현재는 더 이상 사용이 권장되지 않음(deprecated).

<a id="컬럼-정의column-definition"></a>

#### 컬럼 정의(Column Definition)

- 각 컬럼은 이름(name)과 CQL 데이터 타입(data type)으로 정의됨
  - 컬럼 정의에는 다음과 같은 수식어(modifier)를 붙일 수 있음

- `STATIC`: 같은 파티션(partition)의 모든 행이 공유하는 정적 컬럼으로 만듦
- `PRIMARY KEY`: 단일 컬럼을 기본 키로 지정함(인라인 형태).
- `MASKED WITH`: 동적 데이터 마스킹(dynamic data masking)을 적용함

<a id="기본-키primary-key"></a>

#### 기본 키(Primary Key)

- 모든 테이블 정의는 반드시 하나의 기본 키(PRIMARY KEY)를 정의해야 하며, 단 하나만 가질 수 있음
  - 기본 키는 두 부분으로 구성됨

```
primary_key::= partition_key [ ',' clustering_columns ]
```

- 파티션 키(partition key): 기본 키의 첫 번째 구성 요소
- 클러스터링 컬럼(clustering columns): 그 뒤를 따르는 선택적 구성 요소

<a id="파티션-키partition-key와-클러스터링-컬럼clustering-columns"></a>

#### 파티션 키(Partition Key)와 클러스터링 컬럼(Clustering Columns)

파티션 키(Partition Key)는 행을 저장할 노드를 결정한다. 같은 파티션 키 값을 가진 행들은 같은 파티션에 저장되며, 키는 단일 컬럼이나 여러 컬럼을 괄호로 묶은 복합 파티션 키(composite partition key)로 정의한다.

```
partition_key::= column_name  | '(' column_name ( ',' column_name )* ')'
```

파티션 안에서 행을 구분하고 정렬하는 것은 파티션 키 뒤에 놓이는 클러스터링 컬럼(Clustering Columns)이다. 행들이 이 순서대로 저장되므로 파티션 내부의 범위 쿼리를 효율적으로 처리할 수 있다.

- 예를 들어 `PRIMARY KEY ((a, b), c)`는 `(a, b)`를 복합 파티션 키로, `c`를 클러스터링 컬럼으로 만듦

```cql
CREATE TABLE loads (
    machine inet,
    cpu int,
    mtime timeuuid,
    load float,
    PRIMARY KEY ((machine, cpu), mtime)
) WITH CLUSTERING ORDER BY (mtime DESC);
```

- 위 예제에서 `(machine, cpu)`는 복합 파티션 키이고, `mtime`은 클러스터링 컬럼이며, 각 파티션 내에서 `mtime` 기준 내림차순(DESC)으로 정렬됨

<a id="정적-컬럼static-column"></a>

#### 정적 컬럼(Static Column)

- `STATIC` 수식어로 선언된 컬럼은 같은 파티션에 속한 모든 행이 "공유(shared)"하는 값을 가짐
  - 즉, 같은 파티션 키를 가진 행들은 정적 컬럼에 대해 동일한 값을 보게 됨
  - 이는 파티션 수준의 메타데이터(partition-level metadata)를 저장하는 데 유용함

```cql
CREATE TABLE t (
    pk int,
    t int,
    v text,
    s text static,
    PRIMARY KEY (pk, t)
);
INSERT INTO t (pk, t, v, s) VALUES (0, 0, 'val0', 'static0');
INSERT INTO t (pk, t, v, s) VALUES (0, 1, 'val1', 'static1');
SELECT * FROM t;
```

두 `INSERT`는 `t` 값이 달라도 같은 파티션 키(`pk = 0`)를 사용한다. 따라서 정적 컬럼 `s`를 공유하며, 두 번째 `INSERT`에서 `static1`을 쓰면 첫 번째 행을 조회할 때도 `s`가 `static1`로 보인다.

<a id="테이블-옵션table-options"></a>

### 테이블 옵션(Table Options)

- `CREATE TABLE` 또는 `ALTER TABLE` 구문의 `WITH` 절을 통해 테이블의 다양한 속성(property)을 지정할 수 있음
  - 여러 옵션은 `AND`로 연결함

<a id="clustering-order-by"></a>

#### CLUSTERING ORDER BY

- 파티션 내 행들의 기본 정렬 순서를 지정함

```
clustering_order::= column_name (ASC | DESC) ( ',' column_name (ASC | DESC) )*
```

```cql
CREATE TABLE loads (
    machine inet,
    cpu int,
    mtime timeuuid,
    load float,
    PRIMARY KEY ((machine, cpu), mtime)
) WITH CLUSTERING ORDER BY (mtime DESC);
```

> 중요: 쿼리 결과는 원래의 클러스터링 순서(original clustering order) 또는 그 역순(reverse clustering order)으로만 정렬할 수 있음.

>

> 예를 들어 두 개의 클러스터링 컬럼 `a`와 `b`를 가진 테이블을 `WITH CLUSTERING ORDER BY (a DESC, b ASC)`로 정의했다면, 이 테이블에 대한 쿼리는 `ORDER BY (a DESC, b ASC)` 또는 `ORDER BY (a ASC, b DESC)`만 사용할 수 있음.

<a id="compaction"></a>

#### compaction

- 컴팩션(compaction)은 SSTable들을 병합하여 디스크 공간을 회수하고 읽기 성능을 유지하는 과정임
  - `compaction` 옵션은 사용할 컴팩션 전략 클래스(`'class'`)와 전략별 하위 옵션을 지정함

- 지원되는 컴팩션 전략 클래스:

- `SizeTieredCompactionStrategy` (STCS)
- `LeveledCompactionStrategy` (LCS)
- `TimeWindowCompactionStrategy` (TWCS)

```cql
CREATE TABLE timeline (
    userid uuid,
    posted_month int,
    posted_time uuid,
    body text,
    posted_by text,
    PRIMARY KEY (userid, posted_month, posted_time)
) WITH compaction = { 'class' : 'LeveledCompactionStrategy' };
```

- 각 전략은 `enabled`, `tombstone_threshold`, `tombstone_compaction_interval`, `min_threshold`, `max_threshold` 등의 공통 하위 옵션과 함께 전략 고유의 하위 옵션을 가짐
  - 자세한 내용은 공식 문서의 [Compaction](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/compaction/index.html) 섹션을 참고함

<a id="compression"></a>

#### compression

- `compression` 옵션은 SSTable 압축(compression)을 구성함

- `class`: 사용할 압축 알고리즘 (기본값 `LZ4Compressor`)
  - 기본 제공 압축기: `LZ4Compressor`, `SnappyCompressor`, `DeflateCompressor`, `ZstdCompressor`. 압축을 비활성화하려면 `'enabled' : false`를 사용함
- `enabled`: SSTable 압축 활성화/비활성화 (기본값 `true`)
  - `enabled`가 `false`이면 다른 옵션을 지정해서는 안 됨
- `chunk_length_in_kb`: SSTable은 랜덤 읽기(random read)를 허용하기 위해 블록 단위로 압축됨 (기본값 `64`)
  - 이 옵션은 해당 블록의 크기(KB 단위)를 정의함
- `compression_level`: 압축 레벨 (기본값 `3`)
  - `ZstdCompressor`에만 적용됨
  - `-131072`에서 `22` 사이의 값을 허용함

```cql
CREATE TABLE simple (
   id int,
   key text,
   value text,
   PRIMARY KEY (key, value)
) WITH compression = {'class': 'LZ4Compressor', 'chunk_length_in_kb': 4};
```

<a id="caching"></a>

#### caching

- `caching` 옵션은 키 캐시(key cache)와 행 캐시(row cache) 동작을 제어함

- `keys`: 이 테이블의 키 캐싱 여부 (기본값 `ALL`)
  - 유효한 값은 `ALL`과 `NONE`임
- `rows_per_partition`: 파티션당 캐싱할 행의 수(행 캐시). 정수 `n`을 지정하면 파티션에서 처음 조회된 `n`개의 행이 캐싱됨 (기본값 `NONE`)
  - 유효한 값: 파티션의 모든 행을 캐싱하는 `ALL`, 행 캐싱을 비활성화하는 `NONE`, 또는 정수

```cql
CREATE TABLE simple (
id int,
key text,
value text,
PRIMARY KEY (key, value)
) WITH caching = {'keys': 'ALL', 'rows_per_partition': 10};
```

<a id="speculative_retry--additional_write_policy"></a>

#### speculative_retry / additional_write_policy

- 읽기 코디네이터(read coordinator)는 일관성 수준(consistency level)을 만족하는 데 필요한 만큼의 복제본만 조회함(예: `ONE`은 1개, `QUORUM`은 정족수). `speculative_retry`는 코디네이터가 추가 복제본을 조회할 시점을 결정하며, 복제본이 느리거나 응답하지 않을 때 유용함

- `additional_write_policy`는 `speculative_retry`와 동일하지만 쓰기 경로(write path)에 적용됨
  - 두 옵션의 기본값은 모두 `99PERCENTILE`임

- 4.0 이전(pre-4.0) 값:

- `NONE`
- `ALWAYS`
- `99PERCENTILE` (PERCENTILE)
- `50MS` (CUSTOM)

- Cassandra 4.0+ 확장 값:

- `XPERCENTILE`(예: `90.5PERCENTILE`): 코디네이터는 모든 복제본에 대해 테이블별 평균 응답 시간을 기록함 → 어떤 복제본이 이 테이블 평균 응답 시간의 `X`퍼센트보다 오래 걸리면 코디네이터가 추가 복제본을 조회함
  - `X`는 0~100 사이여야 함
- `XP`(예: `90.5P`): `XPERCENTILE`와 동일함
- `Yms`(예: `25ms`): 복제본이 응답하는 데 `Y` 밀리초(milliseconds)를 초과하면 코디네이터가 추가 복제본을 조회함
- `MIN(XPERCENTILE,YMS)`(예: `MIN(99PERCENTILE,35MS)`): 계산 시점에 더 낮은(lower) 값을 사용하는 하이브리드(hybrid) 정책임
- `MAX(XPERCENTILE,YMS)`(예: `MAX(90.5P,25ms)`): 계산 시점에 더 높은(higher) 값을 사용하는 하이브리드(hybrid) 정책임

- Cassandra 4.0은 대소문자 구분 없는(case-insensitive) 표기와 백분위(percentile), 밀리초를 조합하는 `MIN()`/`MAX()` 하이브리드 정책을 추가했음

```cql
ALTER TABLE users WITH speculative_retry = '10ms';
ALTER TABLE users WITH speculative_retry = '99PERCENTILE';
```

<a id="read_repair"></a>

#### read_repair

- `read_repair` 옵션은 읽기 복구(read repair) 동작을 설정함
  - 기본값은 `BLOCKING`임

- `BLOCKING`: 읽기 복구가 트리거되면, 다른 복제본으로 보낸 쓰기가 일관성 수준에 도달할 때까지 읽기가 차단(block)됨
- `NONE`: 코디네이터가 복제본 간의 차이를 조정(reconcile)하기는 하지만, 이를 복구(repair)하려고 시도하지는 않음

- 일관성에 미치는 영향:

- 단조 정족수 읽기(Monotonic quorum reads): 읽기가 시간상 과거로 돌아간 것처럼 보이는 현상을 방지함
  - `BLOCKING`이 이 동작을 제공함
- 쓰기 원자성(Write atomicity): 부분적으로 적용된 쓰기(partially-applied write)가 반환되는 것을 방지함
  - `NONE`이 이 동작을 제공함

<a id="그-외-옵션-목록"></a>

#### 그 외 옵션 목록

- 테이블 생성 또는 변경 시 설정할 수 있는 전체 옵션 목록임

- `comment`: 자유 형식의 사람이 읽을 수 있는 주석(comment) (종류 simple, 기본값 none)
- `speculative_retry`: 추측성 재시도(speculative retry) 옵션 (종류 simple, 기본값 `99PERCENTILE`)
- `additional_write_policy`: `speculative_retry`와 동일 (종류 simple, 기본값 `99PERCENTILE`)
- `cdc`: 테이블에 변경 데이터 캡처(Change Data Capture, CDC) 로그를 생성함 (종류 boolean, 기본값 `false`)
- `gc_grace_seconds`: 툼스톤(tombstone, 삭제 표식)을 가비지 컬렉션하기 전 대기 시간(초). 기본값 864000초는 10일임 (종류 simple, 기본값 `864000`)
- `bloom_filter_fp_chance`: SSTable 블룸 필터(bloom filter)의 목표 거짓 양성(false positive) 확률 (종류 simple, 기본값 `0.00075`)
- `default_time_to_live`: 테이블의 기본 만료 시간(TTL, 초). `0`은 만료 없음을 의미함 (종류 simple, 기본값 `0`)
- `compaction`: 컴팩션 옵션 (종류 map, 기본값 (위 참고))
- `compression`: 압축 옵션 (종류 map, 기본값 (위 참고))
- `caching`: 캐싱 옵션 (종류 map, 기본값 (위 참고))
- `memtable_flush_period_in_ms`: Cassandra가 멤테이블(memtable)을 디스크로 플러시(flush)하기까지의 시간(ms) (종류 simple, 기본값 `0`)
- `read_repair`: 읽기 복구 동작 설정(위 참고) (종류 simple, 기본값 `BLOCKING`)

```cql
CREATE TABLE monkey_species (
    species text PRIMARY KEY,
    common_name text,
    population varint,
    average_size int
) WITH comment='Important biological records';
```

<a id="alter-table"></a>

### ALTER TABLE

- 기존 테이블을 변경함
  - 컬럼 추가/삭제/이름 변경, 마스킹 적용/해제, 테이블 옵션 변경이 가능함

- 문법(Grammar):

```
alter_table_statement::= ALTER TABLE [ IF EXISTS ] table_name alter_table_instruction
alter_table_instruction::= ADD [ IF NOT EXISTS ] column_definition ( ',' column_definition)*
	| DROP [ IF EXISTS ] column_name ( ',' column_name )*
	| RENAME [ IF EXISTS ] column_name to column_name (AND column_name to column_name)*
	| ALTER [ IF EXISTS ] column_name ( column_mask | DROP MASKED )
	| WITH options
column_definition::= column_name cql_type [ column_mask]
column_mask::= MASKED WITH ( DEFAULT | function_name '(' term ( ',' term )* ')' )
```

- 예제:

```cql
ALTER TABLE addamsFamily ADD gravesite varchar;

ALTER TABLE addamsFamily
   WITH comment = 'A most excellent and useful table';
```

- 주요 제약 사항:

- 기본 키(primary key)에 속한 컬럼은 변경할 수 없음
- 컬럼을 추가하는 작업(`ADD`)은 상수 시간 연산(constant-time operation)임
- `ADD`, `DROP`, `RENAME`, `ALTER`(마스킹), `WITH`(옵션 변경) 중 하나의 명령을 지정함

<a id="drop-table"></a>

### DROP TABLE

- 테이블과 그 안의 모든 데이터를 영구적으로 삭제함

- 문법(Grammar):

```
drop_table_statement::= DROP TABLE [ IF EXISTS ] table_name
```

- `IF EXISTS`를 사용하면 해당 테이블이 존재하지 않을 때 오류가 발생하지 않음

```cql
DROP TABLE monkey_species;
```

<a id="truncate"></a>

### TRUNCATE

- 테이블의 구조(schema)는 유지한 채, 테이블에 담긴 모든 데이터를 삭제함

- 문법(Grammar):

```
truncate_statement::= TRUNCATE [ TABLE ] table_name
```

```cql
TRUNCATE monkey_species;
TRUNCATE TABLE monkey_species;
```

> 참고: `TRUNCATE`는 모든 노드에서 데이터를 제거하는 작업으로, 클러스터 전체에 영향을 주는 무거운(heavy) 연산임. `DROP TABLE`이 테이블 정의 자체를 제거하는 것과 달리, `TRUNCATE`는 정의를 보존하고 데이터만 비움.

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL DDL](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/ddl.html)
