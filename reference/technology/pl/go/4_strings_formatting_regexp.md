# Go 문자열 처리, 포맷 입출력, 정규표현식

## fmt: 포맷 입출력

## fmt 패키지

> **원문:** https://pkg.go.dev/fmt

### 개요

`fmt` 패키지는 C의 `printf`/`scanf`와 유사한 방식으로 값을 포맷팅하여 출력하거나, 텍스트를 파싱하여 값을 읽어들이는 기능을 제공합니다. 크게 세 부류로 나뉩니다.

- **출력 함수**: `Print` 계열 — 표준 출력, 문자열, `io.Writer`, 바이트 슬라이스로 값을 씁니다
- **입력 함수**: `Scan` 계열 — 표준 입력, 문자열, `io.Reader`에서 값을 읽습니다
- **포맷 검증(verb)**: `%v`, `%d`, `%s` 등 타입별 서식 지정자

---

### 출력 함수

#### 목적지별 분류

| 목적지 | 기본 | 개행 추가 | 포맷 문자열 사용 |
|---|---|---|---|
| `os.Stdout` | `Print` | `Println` | `Printf` |
| `string` 반환 | `Sprint` | `Sprintln` | `Sprintf` |
| `io.Writer` | `Fprint` | `Fprintln` | `Fprintf` |
| `[]byte` 추가(Go 1.19+) | `Append` | `Appendln` | `Appendf` |

- **`Print(a ...any)`**: 인수를 기본 형식으로 출력합니다. 피연산자 사이에는 둘 다 문자열이 아닐 때만 공백을 넣습니다.
- **`Println(a ...any)`**: 항상 인수 사이에 공백을 넣고, 끝에 개행을 붙입니다.
- **`Printf(format string, a ...any)`**: 포맷 문자열의 verb(`%v`, `%d` 등)에 맞춰 인수를 대입해 출력합니다.
- 나머지(`Sprint*`, `Fprint*`, `Append*`)는 출력 대상만 다를 뿐 동작 규칙은 동일합니다.

```go
fmt.Print("a", "b", 1, 2)      // ab1 2 (문자열끼리는 공백 없음)
fmt.Println("a", "b")          // a b\n
s := fmt.Sprintf("%d-%s", 1, "go") // "1-go"

var buf bytes.Buffer
fmt.Fprintf(&buf, "%5.2f", 3.14159) // buf에 " 3.14" 기록
```

#### 에러 생성: Errorf

- **`Errorf(format string, a ...any) error`**: `Sprintf`처럼 문자열을 만들되 `error` 값으로 반환합니다.
- `%w` verb를 쓰면 인수로 넘긴 에러를 감싸서(wrap) `errors.Is`/`errors.As`/`Unwrap()`이 원본 에러를 찾을 수 있게 합니다.
- Go 1.20부터는 `%w`를 여러 번 써서 에러를 여러 개 감쌀 수 있으며, 이 경우 `Unwrap() []error`를 구현한 값이 반환됩니다.

```go
baseErr := errors.New("not found")
wrapped := fmt.Errorf("user %q 조회 실패: %w", "alice", baseErr)
errors.Is(wrapped, baseErr) // true
```

---

### 입력 함수

#### 소스별 분류

| 소스 | 공백 구분 | 줄 단위 | 포맷 문자열 사용 |
|---|---|---|---|
| `os.Stdin` | `Scan` | `Scanln` | `Scanf` |
| `string` | `Sscan` | `Sscanln` | `Sscanf` |
| `io.Reader` | `Fscan` | `Fscanln` | `Fscanf` |

- **`Scan(a ...any)`**: 공백으로 구분된 값을 읽어 각 인수(포인터)에 채웁니다. 개행도 공백으로 취급합니다.
- **`Scanln(a ...any)`**: `Scan`과 비슷하지만 개행에서 멈추며, 마지막 항목 뒤에 개행이나 EOF가 와야 합니다.
- **`Scanf(format string, a ...any)`**: 포맷 문자열의 verb에 맞춰 입력을 파싱합니다.
- 모든 함수는 성공적으로 읽어들인 항목 수 `n`과 `error`를 반환합니다.

```go
var name string
var age int
n, err := fmt.Sscanf("Alice 30", "%s %d", &name, &age)
// n == 2, name == "Alice", age == 30
```

---

### 포맷 verb (지정자)

`%[플래그][너비][.정밀도]verb` 형태로 구성됩니다.

#### 범용 verb

| verb | 의미 |
|---|---|
| `%v` | 값의 기본 형식 |
| `%+v` | 구조체일 때 필드 이름을 함께 표시 |
| `%#v` | Go 소스 코드 형태의 표현 |
| `%T` | 값의 Go 타입 |
| `%%` | 리터럴 `%` 문자 |

#### 불리언 / 정수

| verb | 의미 |
|---|---|
| `%t` | `true`/`false` |
| `%d` | 10진수 |
| `%b` | 2진수 |
| `%o` | 8진수 |
| `%x` / `%X` | 16진수(소문자/대문자) |
| `%c` | 코드 포인트에 대응하는 문자 |
| `%q` | 안전하게 이스케이프된 작은따옴표 문자 리터럴 |
| `%U` | 유니코드 형식(`U+1234`) |

#### 부동소수점 / 복소수

| verb | 의미 |
|---|---|
| `%f` | 소수점 표기, 지수 없음 |
| `%e` / `%E` | 과학적 표기법 |
| `%g` / `%G` | 지수가 크면 `%e`, 아니면 `%f` 형태로 자동 선택 |

#### 문자열 / 바이트

| verb | 의미 |
|---|---|
| `%s` | 문자열 그대로 출력 |
| `%q` | 안전하게 이스케이프된 큰따옴표 문자열 |
| `%x` / `%X` | 바이트마다 16진수 두 글자로 변환 |

#### 포인터

| verb | 의미 |
|---|---|
| `%p` | `0x` 접두사가 붙은 16진수 주소 |

```go
type Point struct{ X, Y int }
p := Point{1, 2}
fmt.Printf("%v\n", p)   // {1 2}
fmt.Printf("%+v\n", p)  // {X:1 Y:2}
fmt.Printf("%#v\n", p)  // main.Point{X:1, Y:2}
fmt.Printf("%T\n", p)   // main.Point

fmt.Printf("%d %b %x\n", 255, 255, 255) // 255 11111111 ff
fmt.Printf("%q\n", "hi\n")              // "hi\n"
```

#### 너비와 정밀도

- **너비**: 최소 출력 폭. 부족하면 공백(또는 `0` 플래그면 0)으로 채웁니다.
- **정밀도**: `.` 뒤에 지정. 부동소수점은 소수 자릿수, 문자열은 최대 출력 룬 수를 의미합니다.
- `*`를 쓰면 다음 인수(`int`)를 너비/정밀도 값으로 사용합니다.

```go
fmt.Printf("%9.2f\n", 12.3)       // "    12.30"
fmt.Printf("%-9.2f|\n", 12.3)     // "12.30    |"
fmt.Printf("%*.*f\n", 9, 2, 12.3) // 위와 동일, 인수로 너비·정밀도 전달
```

#### 플래그

| 플래그 | 효과 |
|---|---|
| `-` | 왼쪽 정렬(오른쪽에 공백 채움) |
| `+` | 숫자 부호를 항상 표시 |
| `#` | 대체 형식: `0b`/`0`/`0x` 접두사 추가, `%p`의 `0x` 제거 등 |
| `공백` | 부호 자리에 빈 공백 확보 |
| `0` | 부호 뒤부터 0으로 채움 |

---

### 커스텀 포맷팅을 위한 인터페이스

#### Stringer

```go
type Stringer interface {
    String() string
}
```

`String()`을 구현하면 `%v`, `Print` 계열 등 서식이 없는 verb에서 이 값을 사용합니다.

```go
type Status int

func (s Status) String() string {
    switch s {
    case 0:
        return "대기"
    case 1:
        return "완료"
    default:
        return "알수없음"
    }
}

fmt.Println(Status(1)) // 완료
```

#### GoStringer

```go
type GoStringer interface {
    GoString() string
}
```

`%#v`에서 사용할 Go 소스 코드 형태 표현을 직접 정의할 때 구현합니다.

#### Formatter

```go
type Formatter interface {
    Format(f State, verb rune)
}
```

verb, 플래그, 너비, 정밀도까지 전부 직접 제어하고 싶을 때 구현합니다. `f State`를 통해 `f.Width()`, `f.Precision()`, `f.Flag('-')` 등을 조회할 수 있습니다.

#### Scanner

```go
type Scanner interface {
    Scan(state ScanState, verb rune) error
}
```

`Scan`/`Scanf` 계열이 값을 읽을 때 커스텀 파싱 로직을 쓰고 싶을 때 구현합니다. `ScanState`는 `ReadRune`, `SkipSpace`, `Token` 등을 제공합니다.

#### 재귀 주의

`String()` 안에서 자기 자신을 다시 `%s`나 `%v`로 포맷하면 무한 재귀에 빠집니다. 기반 타입으로 변환해서 사용해야 합니다.

```go
type Celsius float64

func (c Celsius) String() string {
    return fmt.Sprintf("%.1f°C", float64(c)) // float64로 변환 후 포맷
}
```

---

### 참고 사항

- **패닉 복구**: `String()`, `Error()`, `GoString()`이 패닉을 일으키면 `fmt`는 이를 잡아 `%!s(PANIC=메시지)` 형태로 출력에 남깁니다.
- **인터페이스 언랩핑**: 인터페이스 값을 출력할 때는 (`%T`, `%p` 제외) 내부의 구체 타입 값을 기준으로 포맷합니다.
- **SQL 문 조립 금지**: `database/sql` 사용 시 `Sprintf`로 쿼리 문자열을 직접 조립하면 SQL 인젝션 위험이 있으므로 플레이스홀더(`?`, `$1` 등)를 사용해야 합니다.

---

## 문자열과 텍스트 처리 (strings/strconv/bytes/unicode)

## strings 패키지

> **원문:** https://pkg.go.dev/strings

### 개요

`strings` 패키지는 UTF-8로 인코딩된 문자열을 다루는 함수 모음이다. 문자열은 불변(immutable)이므로 대부분의 함수는 원본을 수정하지 않고 새 문자열을 반환한다. 많이 쓰는 것 위주로만 정리한다.

### 포함 여부 확인

- `Contains(s, substr string) bool` - substr이 s 안에 있는지
- `ContainsAny(s, chars string) bool` - chars에 속한 문자 중 하나라도 s에 있는지
- `ContainsRune(s string, r rune) bool` - 특정 rune 포함 여부
- `HasPrefix(s, prefix string) bool` / `HasSuffix(s, suffix string) bool`
- `EqualFold(s, t string) bool` - 유니코드 케이스 폴딩 기반 대소문자 무시 비교

```go
strings.HasPrefix("go.mod", "go.")   // true
strings.EqualFold("Go", "GO")        // true
```

### 검색과 인덱스

- `Index(s, substr string) int` / `LastIndex` - 찾으면 시작 위치, 없으면 -1
- `IndexByte`, `IndexRune`, `IndexAny`, `IndexFunc` - 조건별 인덱스 탐색 변형
- `Count(s, substr string) int` - 겹치지 않는 출현 횟수

```go
idx := strings.Index("hello world", "world") // 6
n := strings.Count("banana", "a")             // 3
```

### 자르기와 분할

- `Cut(s, sep string) (before, after string, found bool)` - sep 기준으로 딱 한 번만 자름. `found`로 존재 여부 확인 후 바로 분기 처리 가능해 `Index` + 슬라이싱 조합보다 간결함
- `CutPrefix` / `CutSuffix` - 접두/접미사를 제거하면서 동시에 있었는지 여부도 반환
- `Split(s, sep string) []string` / `SplitN` (개수 제한) / `SplitAfter` (구분자를 결과에 남김)
- `Fields(s string) []string` - 공백 기준 분리, 빈 문자열은 제외 (연속 공백도 하나로 처리)
- `FieldsFunc(s string, f func(rune) bool) []string` - 임의 조건으로 분리

```go
key, value, ok := strings.Cut("name=gopher", "=")
// key="name", value="gopher", ok=true

fields := strings.Fields("  a   b  c ") // ["a" "b" "c"]
```

Go 1.24부터는 `SplitSeq`, `FieldsSeq` 등 `iter.Seq[string]`을 반환하는 이터레이터 버전도 추가되어 `for range`로 직접 순회할 수 있다.

### 치환과 변환

- `Replace(s, old, new string, n int) string` - n번만 치환 (n<0이면 전체)
- `ReplaceAll(s, old, new string) string` - `Replace(s, old, new, -1)`과 동일
- `Map(mapping func(rune) rune, s string) string` - rune 단위로 변환 함수 적용. 함수가 음수를 반환하면 해당 문자는 결과에서 제거됨
- `Repeat(s string, count int) string` - count번 반복 연결

### 대소문자 변환

- `ToUpper`, `ToLower` - 대문자/소문자로 변환
- `ToTitle` - 단어별이 아니라 문자열 전체의 모든 글자를 유니코드 타이틀 케이스로 변환 (대부분의 라틴 문자는 대문자와 동일)
- `ToUpperSpecial`/`ToLowerSpecial`/`ToTitleSpecial` - 튀르키예어 등 언어별 특수 규칙(`unicode.SpecialCase`) 적용
- 단어 첫 글자만 대문자로 바꾸는 `Title`은 단어 경계 판정이 부정확해 **deprecated** 상태이며, 대신 `golang.org/x/text/cases` 패키지를 권장

### 트리밍

- `Trim(s, cutset string) string` - 양쪽에서 cutset에 속한 문자를 모두 제거
- `TrimLeft` / `TrimRight` - 한쪽만 제거
- `TrimFunc` / `TrimLeftFunc` / `TrimRightFunc` - 조건 함수로 제거
- `TrimPrefix` / `TrimSuffix` - 접두/접미사가 있으면 제거, 없으면 그대로 반환
- `TrimSpace(s string) string` - 앞뒤 공백 제거 (가장 흔히 쓰임)

```go
strings.Trim("¡¡¡Hello!!!", "!¡") // "Hello"
strings.TrimSpace("  hi  ")       // "hi"
```

### 기타 유틸리티

- `Join(elems []string, sep string) string` - 슬라이스를 구분자로 연결
- `Compare(a, b string) int` - 사전식 비교 (0/-1/+1); 대부분 `==`, `<`가 더 간단하므로 정렬 등 특수한 경우에만 사용
- `Clone(s string) string` - 원본과 메모리를 공유하지 않는 새 복사본 생성 (부분 문자열이 큰 원본을 붙잡고 있는 걸 막을 때 유용)
- `ToValidUTF8(s, replacement string) string` - 깨진 UTF-8 시퀀스를 치환 문자열로 대체

### 반복 조립: strings.Builder

문자열을 `+`로 계속 이어 붙이면 매번 새 메모리를 할당하므로 느리다. 반복적으로 문자열을 조립할 때는 `Builder`를 쓴다.

```go
var b strings.Builder
b.WriteString("Hello")
b.WriteByte(' ')
b.WriteString("World")
fmt.Println(b.String()) // Hello World
```

- `Write`, `WriteByte`, `WriteRune`, `WriteString` - 내용 추가
- `String()` - 누적된 문자열 반환
- `Len()`, `Cap()`, `Grow(n)`, `Reset()` - 크기 조회/사전 할당/초기화
- 값 복사 시 패닉을 일으키도록 설계되어 있어 실수로 복사해 쓰는 것을 방지함

### strings.Reader / strings.Replacer

- `NewReader(s string) *Reader` - 문자열을 `io.Reader`, `io.Seeker` 등으로 감싸 스트림처럼 읽을 수 있게 함
- `NewReplacer(oldnew ...string) *Replacer` - 여러 쌍의 old→new 치환을 한 번에 등록. 동시성 안전하며 여러 치환을 한 번에 순회하므로 `ReplaceAll`을 여러 번 호출하는 것보다 효율적

```go
r := strings.NewReplacer("<", "&lt;", ">", "&gt;")
r.Replace("This is <b>HTML</b>!")
// This is &lt;b&gt;HTML&lt;/b&gt;!
```

---

## strconv 패키지

> **원문:** https://pkg.go.dev/strconv

### 개요

기본 자료형과 문자열 표현 사이의 변환을 담당한다. 숫자 파싱/포매팅, 문자열 인용(quote)/해제(unquote) 두 축으로 나뉜다.

### 문자열 → 숫자 (Parse)

| 함수 | 설명 |
|------|------|
| `Atoi(s string) (int, error)` | 10진 정수 문자열을 `int`로. `ParseInt(s, 10, 0)`의 축약형 |
| `ParseInt(s string, base, bitSize int) (int64, error)` | base(0, 2~36)와 비트 크기(0~64) 지정 파싱 |
| `ParseUint(s string, base, bitSize int) (uint64, error)` | 부호 없는 정수, 부호 기호는 허용하지 않음 |
| `ParseFloat(s string, bitSize int) (float64, error)` | 10진수, 16진수, NaN, Inf 표현 지원 |
| `ParseBool(str string) (bool, error)` | "1", "t", "T", "true" 등과 "0", "f", "F", "false" 등을 인식 |

```go
n, err := strconv.Atoi("-42")
f, err := strconv.ParseFloat("3.14", 64)
```

`base`를 0으로 주면 `ParseInt`/`ParseUint`가 접두사(`0x`, `0o`, `0b` 등)를 보고 진법을 자동으로 판별한다.

### 숫자 → 문자열 (Format)

| 함수 | 설명 |
|------|------|
| `Itoa(i int) string` | `int`를 10진 문자열로. `Atoi`의 반대 |
| `FormatInt(i int64, base int) string` / `FormatUint` | 임의 진법(2~36) 문자열로 변환 |
| `FormatFloat(f float64, fmt byte, prec, bitSize int) string` | `fmt`에 `'f'`, `'e'`, `'g'` 등 지정, `prec=-1`이면 필요한 최소 자릿수 사용 |
| `FormatBool(b bool) string` | "true"/"false" |

```go
strconv.Itoa(255)                  // "255"
strconv.FormatInt(255, 16)         // "ff"
strconv.FormatFloat(3.14, 'f', 2, 64) // "3.14"
```

반복문 안에서 바이트 슬라이스에 계속 이어붙여야 한다면 문자열을 새로 할당하는 `Format*` 대신 `AppendInt`, `AppendFloat`, `AppendBool`처럼 기존 슬라이스에 추가하는 `Append*` 계열을 쓰면 할당을 줄일 수 있다.

### 오류 처리: NumError

Parse 계열 함수가 실패하면 `*strconv.NumError`를 반환한다.

```go
type NumError struct {
    Func string // 실패한 함수 이름
    Num  string // 입력값
    Err  error  // ErrSyntax 또는 ErrRange
}
```

```go
_, err := strconv.Atoi("abc")
var ne *strconv.NumError
if errors.As(err, &ne) {
    fmt.Println(ne.Func, ne.Num, ne.Err) // Atoi abc invalid syntax
}
```

`ErrSyntax`(형식이 잘못됨)와 `ErrRange`(값이 타입 범위를 벗어남)로 원인을 구분할 수 있다.

### 문자열 리터럴 처리: Quote / Unquote

- `Quote(s string) string` - Go 문자열 리터럴 형태로 이스케이프 처리해서 감쌈
- `QuoteToASCII(s string) string` - 비-ASCII 문자까지 `\uXXXX`로 이스케이프
- `Unquote(s string) (string, error)` - 따옴표로 감싼 리터럴을 원래 문자열로 복원

```go
strconv.Quote("hi\t안녕")      // "\"hi\\t안녕\""
s, _ := strconv.Unquote(`"a\nb"`) // "a\nb" (개행 포함 2줄)
```

로그 출력이나 디버깅 시 문자열에 포함된 제어 문자를 눈에 보이게 표시할 때 유용하다.

---

## bytes 패키지

> **원문:** https://pkg.go.dev/bytes

### 개요

`bytes` 패키지는 `strings`와 거의 동일한 API를 `[]byte`에 대해 제공한다. 문자열 대신 바이트 슬라이스를 다뤄야 할 때(예: 네트워크 I/O, 파일 처리, 불필요한 문자열 변환을 피하고 싶을 때) 사용한다. 함수 이름과 동작이 `strings`와 대응되므로 겹치는 부분은 생략하고 `bytes`만의 특징을 중심으로 정리한다.

### strings와 대응되는 함수들

`Contains`, `Index`, `Split`, `Join`, `Trim*`, `Replace(All)`, `HasPrefix/Suffix`, `Cut`, `Fields`, `Count`, `Equal`, `Compare`, `ToUpper/Lower` 등이 동일한 이름과 시맨틱으로 존재한다. 차이는 인자와 반환값이 `string` 대신 `[]byte`라는 점뿐이다.

```go
data := []byte("apple,banana,cherry")
parts := bytes.Split(data, []byte(","))
joined := bytes.Join(parts, []byte("-"))
fmt.Println(string(joined)) // apple-banana-cherry
```

`strings.EqualFold`에 대응하는 `bytes.EqualFold(s, t []byte) bool`도 있다.

### bytes.Buffer

`strings.Builder`의 상위 호환에 가까운, 읽기와 쓰기를 모두 지원하는 가변 크기 버퍼다. 제로 값 그대로 바로 사용할 수 있다.

- 쓰기: `Write`, `WriteString`, `WriteByte`, `WriteRune`
- 읽기: `Read`, `ReadByte`, `ReadRune`, `ReadBytes(delim)`, `ReadString(delim)` - delim을 만날 때까지 읽음
- 상태 조회: `Bytes()`, `String()`, `Len()`, `Cap()`
- 제어: `Grow(n)`, `Reset()`, `Truncate(n)`, `Next(n)` (다음 n바이트를 반환하며 커서 이동)

```go
var buf bytes.Buffer
buf.WriteString("Hello, ")
buf.WriteString("World!")
fmt.Println(buf.String()) // Hello, World!
```

`io.Writer`를 요구하는 함수(`fmt.Fprintf` 등)에 그대로 넘길 수 있어서 문자열 조립 후 I/O로 넘기는 흔한 패턴에 잘 맞는다.

### bytes.Reader

읽기 전용으로 `[]byte`를 감싸며 `io.Reader`, `io.ReaderAt`, `io.WriterTo`, `io.Seeker`, `io.ByteScanner`, `io.RuneScanner`를 모두 구현한다. `Buffer`와 달리 쓰기 메서드가 없고, 대신 `Seek`으로 임의 위치 이동이 가능하며 `UnreadByte`/`UnreadRune`으로 직전에 읽은 바이트·rune을 되돌릴 수 있다.

```go
r := bytes.NewReader([]byte("hello"))
buf := make([]byte, 2)
r.Read(buf) // buf = "he"
```

### 그 밖의 유틸리티

- `Runes(s []byte) []rune` - UTF-8 바이트를 rune 슬라이스로 디코딩
- `Clone(b []byte) []byte` - 원본과 메모리를 공유하지 않는 복사본
- `ToValidUTF8(s, replacement []byte) []byte` - 깨진 UTF-8 시퀀스 치환

---

## unicode 패키지

> **원문:** https://pkg.go.dev/unicode

### 개요

개별 `rune`(유니코드 코드 포인트) 하나의 성질을 판정하고 대소문자를 변환하는 저수준 함수들을 제공한다. `strings`/`bytes`의 `*Func` 계열 함수(`TrimFunc`, `IndexFunc` 등)에 조건 함수로 자주 넘겨진다.

### 문자 분류 함수

| 함수 | 판정 대상 |
|------|-----------|
| `IsLetter(r) bool` | 문자(카테고리 L) |
| `IsDigit(r) bool` | 10진 숫자 |
| `IsNumber(r) bool` | 숫자류 전반(카테고리 N, 로마 숫자 등 포함) |
| `IsSpace(r) bool` | 공백류(스페이스, 탭, 개행 등) |
| `IsUpper(r)` / `IsLower(r)` / `IsTitle(r)` | 대문자/소문자/타이틀 케이스 여부 |
| `IsPunct(r)` / `IsSymbol(r)` | 구두점 / 기호 |
| `IsControl(r)` | 제어 문자 |
| `IsPrint(r)` / `IsGraphic(r)` | 출력 가능 여부 (`IsPrint`는 공백을 ASCII 스페이스로 한정) |

```go
isAlnum := func(r rune) bool {
    return unicode.IsLetter(r) || unicode.IsDigit(r)
}
strings.IndexFunc("go1.16!", func(r rune) bool { return !isAlnum(r) }) // 4 (".")
```

### 케이스 변환

- `ToUpper(r rune) rune` / `ToLower(r rune) rune` / `ToTitle(r rune) rune` - rune 하나를 변환 (`strings.ToUpper`는 문자열 전체를 순회하며 내부적으로 이 함수를 사용)
- `SpecialCase` 타입 - 튀르키예어(`unicode.TurkishCase`)처럼 언어별로 다른 대소문자 규칙을 표현

### RangeTable과 사용자 정의 카테고리

`unicode.Is(rangeTab *RangeTable, r rune) bool`로 임의의 유니코드 범위 테이블에 대해 소속 여부를 검사할 수 있다. `unicode.Letter`, `unicode.Han`처럼 카테고리/스크립트별로 미리 정의된 `*RangeTable` 값들이 패키지에 노출되어 있어 특정 문자 집합(한글, 한자, 이모지 등)만 걸러낼 때 활용할 수 있다.

```go
unicode.Is(unicode.Han, '漢') // true
```

---

## unicode/utf8 패키지

> **원문:** https://pkg.go.dev/unicode/utf8

### 개요

`rune`과 UTF-8 바이트 인코딩 사이의 저수준 인코딩/디코딩을 담당한다. Go 문자열은 내부적으로 UTF-8 바이트 시퀀스이므로, 바이트 단위가 아니라 문자(rune) 단위로 정확히 다뤄야 할 때 이 패키지가 필요하다.

### 핵심 상수

```go
const (
    RuneError = '�'     // 잘못된 인코딩을 나타내는 대체 문자
    RuneSelf  = 0x80         // 이 값 미만은 항상 1바이트 rune (ASCII)
    MaxRune   = '\U0010FFFF' // 최대 유효 코드 포인트
    UTFMax    = 4            // UTF-8 인코딩의 최대 바이트 수
)
```

### 디코딩

- `DecodeRune(p []byte) (r rune, size int)` - 첫 번째 rune과 그 바이트 길이를 반환
- `DecodeRuneInString(s string) (r rune, size int)` - 문자열 버전
- `DecodeLastRune(p []byte) (r rune, size int)` - 마지막 rune (뒤에서부터 읽어야 할 때)

빈 슬라이스면 `(RuneError, 0)`, 잘못된 인코딩이면 `(RuneError, 1)`을 반환하므로 `size`로 오류 여부를 판별할 수 있다.

```go
s := "Hello, 世界"
for i := 0; i < len(s); {
    r, size := utf8.DecodeRuneInString(s[i:])
    fmt.Printf("%c ", r)
    i += size
}
```

for-range로 문자열을 순회하면 Go 런타임이 내부적으로 이와 동일한 디코딩을 자동으로 해준다.

### 인코딩

- `EncodeRune(p []byte, r rune) int` - rune을 UTF-8로 인코딩해 `p`에 쓰고 바이트 수 반환
- `AppendRune(p []byte, r rune) []byte` - 기존 슬라이스 뒤에 인코딩 결과를 추가

### 유효성 검사와 길이 계산

- `Valid(p []byte) bool` / `ValidString(s string) bool` - 전체가 올바른 UTF-8인지
- `ValidRune(r rune) bool` - 개별 rune이 UTF-8로 인코딩 가능한 값인지 (서로게이트 쌍 등은 false)
- `RuneLen(r rune) int` - 해당 rune을 인코딩했을 때 바이트 수 (유효하지 않으면 -1)
- `RuneCountInString(s string) int` - **바이트 길이(`len(s)`)가 아니라** 실제 문자(rune) 개수

```go
s := "Hello, 世界"
len(s)                       // 13 (바이트 수)
utf8.RuneCountInString(s)    // 9  (문자 수)
```

문자열의 "길이"가 필요할 때 `len()`과 `utf8.RuneCountInString()`을 혼동하면 멀티바이트 문자(한글, 한자, 이모지 등)에서 버그가 생기기 쉬우므로 용도에 맞게 구분해서 써야 한다.

### RuneStart

`RuneStart(b byte) bool` - 어떤 바이트가 rune 인코딩의 시작 바이트일 수 있는지 판정한다. UTF-8 바이트 스트림을 임의 위치에서 잘랐을 때, 문자 경계를 찾아 안전하게 자르는 위치를 재조정하는 데 사용한다.

---

## 정규표현식 (regexp)

## regexp — 정규표현식 검색

> **원문:** https://pkg.go.dev/regexp

### 개요

`regexp` 패키지는 RE2 문법(Perl 호환 정규식이 아님)으로 정규표현식 검색을 구현한다.

- **선형 시간 보장**: 백트래킹 기반 엔진과 달리 입력 길이에 비례하는 시간 안에 매칭이 끝난다.
- **매칭 방식**: 기본은 Perl/Python 스타일의 **leftmost-first**(가장 왼쪽에서 시작하되, 우선순위가 높은 대안을 먼저 선택). POSIX ERE 스타일인 **leftmost-longest**로 바꾸려면 `CompilePOSIX`를 쓰거나 `Longest()`를 호출한다.
- UTF-8 문자열을 다루며, 잘못된 바이트 시퀀스는 `utf8.RuneError`로 취급한다.
- `*Regexp` 값은 컴파일 후에는 여러 고루틴에서 동시에 읽기 전용으로 안전하게 쓸 수 있다. 단 `Longest()`처럼 설정을 바꾸는 메서드는 동시 호출에 안전하지 않다.

### 컴파일: Compile / MustCompile

| 함수 | 매칭 방식 | 실패 시 동작 |
|---|---|---|
| `Compile(expr string) (*Regexp, error)` | leftmost-first | 오류 반환 |
| `CompilePOSIX(expr string) (*Regexp, error)` | leftmost-longest | 오류 반환 |
| `MustCompile(str string) *Regexp` | leftmost-first | panic |
| `MustCompilePOSIX(str string) *Regexp` | leftmost-longest | panic |

패턴이 상수 리터럴이라 컴파일에 실패할 일이 없다면(패키지 전역 변수 초기화 등) `MustCompile`을 쓰는 것이 관례다. 사용자 입력처럼 실패 가능성이 있는 패턴은 `Compile`로 오류를 처리한다.

```go
var wordRe = regexp.MustCompile(`(?P<first>\w+)\s+(?P<last>\w+)`)

func compileUserPattern(p string) (*regexp.Regexp, error) {
    return regexp.Compile(p)
}
```

### 단순 매칭 여부 확인

컴파일 없이 바로 확인하고 싶다면 패키지 레벨 함수를 쓴다. 같은 패턴을 반복 사용한다면 매번 파싱 비용이 드므로 `Compile` 후 메서드를 쓰는 편이 낫다.

```go
func Match(pattern string, b []byte) (matched bool, err error)
func MatchString(pattern string, s string) (matched bool, err error)
func MatchReader(pattern string, r io.RuneReader) (matched bool, err error)
```

컴파일된 `*Regexp`의 동일한 메서드도 있다: `Match(b []byte) bool`, `MatchString(s string) bool`, `MatchReader(r io.RuneReader) bool`.

```go
ok, _ := regexp.MatchString(`^\d{3}-\d{4}$`, "010-1234")

re := regexp.MustCompile(`^\d{3}-\d{4}$`)
ok2 := re.MatchString("010-1234")
```

### 매칭 결과 찾기: Find 계열

메서드 이름은 다음 조합으로 결정된다.

```
Find [All] [String] [Submatch] [Index]
```

- **All 없음**: 첫 번째 매칭 하나만 반환
- **All 있음**: 겹치지 않는 모든 매칭을 반환, `n` 인자로 최대 개수 제한(`n < 0`이면 전부)
- **String 없음**: `[]byte` 입출력, **String 있음**: `string` 입출력
- **Submatch 없음**: 전체 매칭 문자열만, **Submatch 있음**: 괄호로 캡처한 하위 그룹까지 함께 반환
- **Index 없음**: 매칭된 텍스트 자체, **Index 있음**: `[start, end]` 위치(byte offset) 반환

```go
re := regexp.MustCompile(`\d+`)

re.FindString("a1b22c333")                 // "1"
re.FindAllString("a1b22c333", -1)           // ["1" "22" "333"]
re.FindStringIndex("a1b22c333")             // [1 2]
re.FindAllStringIndex("a1b22c333", 2)       // [[1 2] [3 5]]
```

캡처 그룹이 있는 패턴은 `Submatch` 계열로 그룹별 값을 얻는다. 반환 슬라이스의 인덱스 0은 항상 전체 매칭이고, 1부터가 첫 번째, 두 번째 … 캡처 그룹이다. 매칭에 실패한 옵셔널 그룹은 빈 문자열(또는 Index 계열에서는 `-1`)이 된다.

```go
re := regexp.MustCompile(`(?P<first>\w+)\s+(?P<last>\w+)`)
m := re.FindStringSubmatch("Alan Turing")
// m == []string{"Alan Turing", "Alan", "Turing"}
```

이름 붙인 캡처 그룹은 `(?P<이름>...)` 문법으로 선언하며, 아래 메서드로 이름과 인덱스를 매핑한다.

- `NumSubexp() int` — 캡처 그룹 개수
- `SubexpNames() []string` — 인덱스 순서대로 그룹 이름(0번은 항상 `""`, 이름 없는 그룹도 `""`)
- `SubexpIndex(name string) int` — 이름으로 인덱스 조회, 없으면 `-1`

```go
idx := re.SubexpIndex("last")
fmt.Println(m[idx]) // "Turing"
```

### 치환: Replace 계열

| 메서드 | `$` 확장 | repl 종류 |
|---|---|---|
| `ReplaceAllString(src, repl string) string` | O | 문자열 템플릿 |
| `ReplaceAllLiteralString(src, repl string) string` | X | 문자열 그대로 |
| `ReplaceAllStringFunc(src string, repl func(string) string) string` | X | 함수(매칭된 전체 문자열을 받아 대체값 반환) |

`[]byte` 버전(`ReplaceAll`, `ReplaceAllLiteral`, `ReplaceAllFunc`)도 동일한 규칙으로 존재한다.

`$` 확장 템플릿에서는 `$1`, `$name`, `${name}`으로 캡처 그룹을 참조하고 `$$`는 리터럴 `$` 하나를 뜻한다.

```go
re := regexp.MustCompile(`(?P<first>\w+)\s+(?P<last>\w+)`)
re.ReplaceAllString("Alan Turing", "${last}, ${first}")   // "Turing, Alan"

digits := regexp.MustCompile(`\d+`)
digits.ReplaceAllStringFunc("a1b22", func(s string) string {
    n, _ := strconv.Atoi(s)
    return strconv.Itoa(n * 2)
})
```

직접 인덱스 슬라이스를 다루고 싶으면 `Expand` / `ExpandString`으로 `FindSubmatchIndex` 결과에 템플릿을 적용할 수 있다.

### 분리: Split

```go
func (re *Regexp) Split(s string, n int) []string
```

매칭되는 부분을 구분자 삼아 문자열을 나눈다. `n < 0`이면 제한 없이 모두 분리한다.

```go
regexp.MustCompile(`\s+`).Split("one  two\tthree", -1)
// []string{"one", "two", "three"}
```

### 기타 유틸리티

- `QuoteMeta(s string) string` — 문자열에 포함된 정규식 메타문자를 이스케이프해, `s`를 리터럴 그대로 매칭하는 패턴으로 바꾼다(사용자 입력을 패턴 일부로 끼워 넣을 때 유용).
- `(re *Regexp) String() string` — 컴파일에 사용한 원본 패턴 문자열 반환.
- `(re *Regexp) LiteralPrefix() (prefix string, complete bool)` — 패턴이 반드시 가져야 하는 리터럴 접두사와, 그 접두사가 패턴 전체와 같은지 여부.
- `(re *Regexp) Longest()` — 이 인스턴스를 leftmost-longest 매칭으로 전환한다(POSIX와 동일한 방식). 호출 자체가 동시성 안전하지 않으므로 다른 고루틴이 아직 쓰기 전에 호출해야 한다.
- Go 1.21부터 `MarshalText` / `UnmarshalText`, Go 1.24부터 `AppendText`를 지원해 `encoding.TextMarshaler` 등으로 직렬화할 수 있다.

```go
safe := regexp.QuoteMeta("a.b*c")   // "a\.b\*c"
re := regexp.MustCompile(safe)
re.MatchString("a.b*c")             // true
```

### 빈 매칭과 순서에 대한 주의

- `FindAll*` 계열은 앞선 매칭 바로 뒤에 붙어 발생하는 빈 매칭은 건너뛴다. 그렇지 않으면 무한히 빈 매칭만 쌓일 수 있기 때문이다.
- 캡처 그룹은 여는 괄호 `(` 의 등장 순서대로 왼쪽부터 번호가 매겨진다. `(?:...)` 처럼 캡처하지 않는 그룹은 번호에 포함되지 않는다.

---

## regexp/syntax — 정규식 파서와 컴파일러

> **원문:** https://pkg.go.dev/regexp/syntax

### 개요

`regexp/syntax`는 `regexp` 패키지가 내부적으로 쓰는 저수준 패키지로, 정규식 문자열을 파싱해 구문 트리(AST)로 만들고 그 트리를 실행 가능한 프로그램으로 컴파일한다. 일반적인 정규식 사용에는 필요 없고, 정규식을 분석·변형하는 도구를 만들 때 쓴다.

### 구문 트리: Regexp 타입

```go
type Regexp struct {
    Op    Op        // 연산자 종류 (OpLiteral, OpCharClass 등)
    Flags Flags
    Sub   []*Regexp // 하위 표현식들
    Rune  []rune    // OpLiteral/OpCharClass가 다루는 룬
    Min, Max int     // OpRepeat의 반복 횟수 범위
    Cap   int        // OpCapture의 캡처 그룹 번호
    Name  string      // OpCapture의 캡처 그룹 이름
}
```

`regexp.Regexp`(컴파일된 정규식 핸들)과 이름은 같지만 별개 타입이다. 이쪽은 순수한 파스 트리 노드다.

주요 함수/메서드:

- `Parse(s string, flags Flags) (*Regexp, error)` — 패턴 문자열을 AST로 파싱
- `(re *Regexp) Simplify() *Regexp` — `{n,m}` 같은 반복을 더 단순한 노드 조합으로 정규화
- `(re *Regexp) String() string` — AST를 다시 패턴 문자열로 직렬화
- `(re *Regexp) MaxCap() int` / `CapNames() []string` — 캡처 그룹 최대 번호 / 이름 목록 조회
- `(x *Regexp) Equal(y *Regexp) bool` — 두 AST의 구조적 동등성 비교

```go
re, err := syntax.Parse(`(?P<num>\d+)-(\w+)`, syntax.Perl)
if err != nil {
    log.Fatal(err)
}
fmt.Println(re.Op, re.CapNames())
```

### Op — 노드 연산자 상수

트리 노드가 무엇을 매칭하는지 나타낸다. 주요 값:

- `OpLiteral` — 고정 문자열(룬 나열)
- `OpCharClass` — `[...]` 문자 클래스
- `OpAnyChar` / `OpAnyCharNotNL` — `.` (줄바꿈 포함/제외)
- `OpBeginLine` / `OpEndLine` — `^` / `$`(멀티라인 모드)
- `OpBeginText` / `OpEndText` — `\A` / `\z`
- `OpWordBoundary` / `OpNoWordBoundary` — `\b` / `\B`
- `OpCapture` — 캡처 그룹 `(...)`
- `OpStar` / `OpPlus` / `OpQuest` / `OpRepeat` — `*` `+` `?` `{n,m}`
- `OpConcat` / `OpAlternate` — 이어붙이기 / `|` 선택

### Flags — 파싱 동작 제어

`Parse`에 넘기는 비트플래그로, 어떤 문법 방언으로 해석할지 결정한다.

| 플래그 | 의미 |
|---|---|
| `FoldCase` | 대소문자 구분 안 함 |
| `Literal` | 패턴을 정규식이 아닌 리터럴 문자열로 취급 |
| `ClassNL` | `[\s]` 등의 클래스가 줄바꿈도 매칭 |
| `DotNL` | `.`이 줄바꿈도 매칭 |
| `OneLine` | `^`/`$`이 텍스트 시작·끝에서만 매칭(멀티라인 아님) |
| `PerlX` | Perl 확장 문법(`\d`, `(?:)`, non-greedy `*?` 등) 허용 |
| `UnicodeGroups` | `\p{Han}`, `\P{Han}` 같은 유니코드 스크립트 클래스 허용 |

미리 조합해둔 상수:

- `syntax.Perl = ClassNL | OneLine | PerlX | UnicodeGroups` — `regexp.Compile`이 실제로 사용하는 조합(가장 널리 쓰임)
- `syntax.POSIX = 0` — 엄격한 POSIX ERE 문법, `regexp.CompilePOSIX`가 사용

### 컴파일된 프로그램: Prog / Inst

파싱된 AST는 `Compile`로 실행 가능한 명령어 시퀀스(`Prog`)로 바뀐다.

```go
func Compile(re *Regexp) (*Prog, error)

type Prog struct {
    Inst   []Inst
    Start  int
    NumCap int
}
```

- `(p *Prog) Prefix() (string, bool)` — 매칭이 반드시 시작해야 하는 리터럴 접두사와, 그것이 패턴 전체를 대신하는지 여부(빠른 사전 필터링에 쓰임)
- `(p *Prog) StartCond() EmptyOp` — 매칭 시작 지점에서 요구되는 빈 너비 단언(예: 줄 시작) 조회

`Inst`는 개별 명령(문자 매칭, 분기, 캡처 기록 등)을 나타내며 `InstOp` 상수(`InstRune`, `InstCapture`, `InstMatch` 등)로 종류를 구분한다. 이 계층은 정규식 엔진 내부 구현에 해당해 일반 애플리케이션 코드에서 직접 다룰 일은 거의 없다.

### 오류 타입

```go
type Error struct {
    Code ErrorCode
    Expr string
}
```

패턴이 잘못됐을 때 `Parse`/`Compile`이 반환하는 오류로, `Error()`가 어떤 부분(`Expr`)이 왜(`Code`) 문제인지 알려준다. `ErrorCode`의 대표 값: `ErrMissingParen`(괄호 안 닫힘), `ErrInvalidCharClass`(잘못된 `[...]`), `ErrTrailingBackslash`(패턴이 `\`로 끝남), `ErrNestingDepth`(중첩이 너무 깊음).

### 언제 이 패키지를 직접 쓰나

대부분의 코드는 `regexp.Compile`/`MustCompile`만으로 충분하다. `regexp/syntax`는 다음과 같은 상황에서만 필요하다.

- 정규식 패턴 자체를 정적 분석하는 린터·포매터를 만들 때
- 여러 정규식을 조합·최적화하는 커스텀 매칭 엔진을 만들 때
- 정규식이 요구하는 리터럴 접두사를 뽑아내 사전 필터링에 활용할 때
