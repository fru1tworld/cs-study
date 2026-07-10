# 문자열과 텍스트 처리 (strings/strconv/bytes/unicode)

# strings 패키지

> **원문:** https://pkg.go.dev/strings

## 개요

`strings` 패키지는 UTF-8로 인코딩된 문자열을 다루는 함수 모음이다. 문자열은 불변(immutable)이므로 대부분의 함수는 원본을 수정하지 않고 새 문자열을 반환한다. 많이 쓰는 것 위주로만 정리한다.

## 포함 여부 확인

- `Contains(s, substr string) bool` - substr이 s 안에 있는지
- `ContainsAny(s, chars string) bool` - chars에 속한 문자 중 하나라도 s에 있는지
- `ContainsRune(s string, r rune) bool` - 특정 rune 포함 여부
- `HasPrefix(s, prefix string) bool` / `HasSuffix(s, suffix string) bool`
- `EqualFold(s, t string) bool` - 유니코드 케이스 폴딩 기반 대소문자 무시 비교

```go
strings.HasPrefix("go.mod", "go.")   // true
strings.EqualFold("Go", "GO")        // true
```

## 검색과 인덱스

- `Index(s, substr string) int` / `LastIndex` - 찾으면 시작 위치, 없으면 -1
- `IndexByte`, `IndexRune`, `IndexAny`, `IndexFunc` - 조건별 인덱스 탐색 변형
- `Count(s, substr string) int` - 겹치지 않는 출현 횟수

```go
idx := strings.Index("hello world", "world") // 6
n := strings.Count("banana", "a")             // 3
```

## 자르기와 분할

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

## 치환과 변환

- `Replace(s, old, new string, n int) string` - n번만 치환 (n<0이면 전체)
- `ReplaceAll(s, old, new string) string` - `Replace(s, old, new, -1)`과 동일
- `Map(mapping func(rune) rune, s string) string` - rune 단위로 변환 함수 적용. 함수가 음수를 반환하면 해당 문자는 결과에서 제거됨
- `Repeat(s string, count int) string` - count번 반복 연결

## 대소문자 변환

- `ToUpper`, `ToLower` - 대문자/소문자로 변환
- `ToTitle` - 단어별이 아니라 문자열 전체의 모든 글자를 유니코드 타이틀 케이스로 변환 (대부분의 라틴 문자는 대문자와 동일)
- `ToUpperSpecial`/`ToLowerSpecial`/`ToTitleSpecial` - 튀르키예어 등 언어별 특수 규칙(`unicode.SpecialCase`) 적용
- 단어 첫 글자만 대문자로 바꾸는 `Title`은 단어 경계 판정이 부정확해 **deprecated** 상태이며, 대신 `golang.org/x/text/cases` 패키지를 권장

## 트리밍

- `Trim(s, cutset string) string` - 양쪽에서 cutset에 속한 문자를 모두 제거
- `TrimLeft` / `TrimRight` - 한쪽만 제거
- `TrimFunc` / `TrimLeftFunc` / `TrimRightFunc` - 조건 함수로 제거
- `TrimPrefix` / `TrimSuffix` - 접두/접미사가 있으면 제거, 없으면 그대로 반환
- `TrimSpace(s string) string` - 앞뒤 공백 제거 (가장 흔히 쓰임)

```go
strings.Trim("¡¡¡Hello!!!", "!¡") // "Hello"
strings.TrimSpace("  hi  ")       // "hi"
```

## 기타 유틸리티

- `Join(elems []string, sep string) string` - 슬라이스를 구분자로 연결
- `Compare(a, b string) int` - 사전식 비교 (0/-1/+1); 대부분 `==`, `<`가 더 간단하므로 정렬 등 특수한 경우에만 사용
- `Clone(s string) string` - 원본과 메모리를 공유하지 않는 새 복사본 생성 (부분 문자열이 큰 원본을 붙잡고 있는 걸 막을 때 유용)
- `ToValidUTF8(s, replacement string) string` - 깨진 UTF-8 시퀀스를 치환 문자열로 대체

## 반복 조립: strings.Builder

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

## strings.Reader / strings.Replacer

- `NewReader(s string) *Reader` - 문자열을 `io.Reader`, `io.Seeker` 등으로 감싸 스트림처럼 읽을 수 있게 함
- `NewReplacer(oldnew ...string) *Replacer` - 여러 쌍의 old→new 치환을 한 번에 등록. 동시성 안전하며 여러 치환을 한 번에 순회하므로 `ReplaceAll`을 여러 번 호출하는 것보다 효율적

```go
r := strings.NewReplacer("<", "&lt;", ">", "&gt;")
r.Replace("This is <b>HTML</b>!")
// This is &lt;b&gt;HTML&lt;/b&gt;!
```

---

# strconv 패키지

> **원문:** https://pkg.go.dev/strconv

## 개요

기본 자료형과 문자열 표현 사이의 변환을 담당한다. 숫자 파싱/포매팅, 문자열 인용(quote)/해제(unquote) 두 축으로 나뉜다.

## 문자열 → 숫자 (Parse)

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

## 숫자 → 문자열 (Format)

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

## 오류 처리: NumError

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

## 문자열 리터럴 처리: Quote / Unquote

- `Quote(s string) string` - Go 문자열 리터럴 형태로 이스케이프 처리해서 감쌈
- `QuoteToASCII(s string) string` - 비-ASCII 문자까지 `\uXXXX`로 이스케이프
- `Unquote(s string) (string, error)` - 따옴표로 감싼 리터럴을 원래 문자열로 복원

```go
strconv.Quote("hi\t안녕")      // "\"hi\\t안녕\""
s, _ := strconv.Unquote(`"a\nb"`) // "a\nb" (개행 포함 2줄)
```

로그 출력이나 디버깅 시 문자열에 포함된 제어 문자를 눈에 보이게 표시할 때 유용하다.

---

# bytes 패키지

> **원문:** https://pkg.go.dev/bytes

## 개요

`bytes` 패키지는 `strings`와 거의 동일한 API를 `[]byte`에 대해 제공한다. 문자열 대신 바이트 슬라이스를 다뤄야 할 때(예: 네트워크 I/O, 파일 처리, 불필요한 문자열 변환을 피하고 싶을 때) 사용한다. 함수 이름과 동작이 `strings`와 대응되므로 겹치는 부분은 생략하고 `bytes`만의 특징을 중심으로 정리한다.

## strings와 대응되는 함수들

`Contains`, `Index`, `Split`, `Join`, `Trim*`, `Replace(All)`, `HasPrefix/Suffix`, `Cut`, `Fields`, `Count`, `Equal`, `Compare`, `ToUpper/Lower` 등이 동일한 이름과 시맨틱으로 존재한다. 차이는 인자와 반환값이 `string` 대신 `[]byte`라는 점뿐이다.

```go
data := []byte("apple,banana,cherry")
parts := bytes.Split(data, []byte(","))
joined := bytes.Join(parts, []byte("-"))
fmt.Println(string(joined)) // apple-banana-cherry
```

`strings.EqualFold`에 대응하는 `bytes.EqualFold(s, t []byte) bool`도 있다.

## bytes.Buffer

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

## bytes.Reader

읽기 전용으로 `[]byte`를 감싸며 `io.Reader`, `io.ReaderAt`, `io.WriterTo`, `io.Seeker`, `io.ByteScanner`, `io.RuneScanner`를 모두 구현한다. `Buffer`와 달리 쓰기 메서드가 없고, 대신 `Seek`으로 임의 위치 이동이 가능하며 `UnreadByte`/`UnreadRune`으로 직전에 읽은 바이트·rune을 되돌릴 수 있다.

```go
r := bytes.NewReader([]byte("hello"))
buf := make([]byte, 2)
r.Read(buf) // buf = "he"
```

## 그 밖의 유틸리티

- `Runes(s []byte) []rune` - UTF-8 바이트를 rune 슬라이스로 디코딩
- `Clone(b []byte) []byte` - 원본과 메모리를 공유하지 않는 복사본
- `ToValidUTF8(s, replacement []byte) []byte` - 깨진 UTF-8 시퀀스 치환

---

# unicode 패키지

> **원문:** https://pkg.go.dev/unicode

## 개요

개별 `rune`(유니코드 코드 포인트) 하나의 성질을 판정하고 대소문자를 변환하는 저수준 함수들을 제공한다. `strings`/`bytes`의 `*Func` 계열 함수(`TrimFunc`, `IndexFunc` 등)에 조건 함수로 자주 넘겨진다.

## 문자 분류 함수

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

## 케이스 변환

- `ToUpper(r rune) rune` / `ToLower(r rune) rune` / `ToTitle(r rune) rune` - rune 하나를 변환 (`strings.ToUpper`는 문자열 전체를 순회하며 내부적으로 이 함수를 사용)
- `SpecialCase` 타입 - 튀르키예어(`unicode.TurkishCase`)처럼 언어별로 다른 대소문자 규칙을 표현

## RangeTable과 사용자 정의 카테고리

`unicode.Is(rangeTab *RangeTable, r rune) bool`로 임의의 유니코드 범위 테이블에 대해 소속 여부를 검사할 수 있다. `unicode.Letter`, `unicode.Han`처럼 카테고리/스크립트별로 미리 정의된 `*RangeTable` 값들이 패키지에 노출되어 있어 특정 문자 집합(한글, 한자, 이모지 등)만 걸러낼 때 활용할 수 있다.

```go
unicode.Is(unicode.Han, '漢') // true
```

---

# unicode/utf8 패키지

> **원문:** https://pkg.go.dev/unicode/utf8

## 개요

`rune`과 UTF-8 바이트 인코딩 사이의 저수준 인코딩/디코딩을 담당한다. Go 문자열은 내부적으로 UTF-8 바이트 시퀀스이므로, 바이트 단위가 아니라 문자(rune) 단위로 정확히 다뤄야 할 때 이 패키지가 필요하다.

## 핵심 상수

```go
const (
    RuneError = '�'     // 잘못된 인코딩을 나타내는 대체 문자
    RuneSelf  = 0x80         // 이 값 미만은 항상 1바이트 rune (ASCII)
    MaxRune   = '\U0010FFFF' // 최대 유효 코드 포인트
    UTFMax    = 4            // UTF-8 인코딩의 최대 바이트 수
)
```

## 디코딩

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

## 인코딩

- `EncodeRune(p []byte, r rune) int` - rune을 UTF-8로 인코딩해 `p`에 쓰고 바이트 수 반환
- `AppendRune(p []byte, r rune) []byte` - 기존 슬라이스 뒤에 인코딩 결과를 추가

## 유효성 검사와 길이 계산

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

## RuneStart

`RuneStart(b byte) bool` - 어떤 바이트가 rune 인코딩의 시작 바이트일 수 있는지 판정한다. UTF-8 바이트 스트림을 임의 위치에서 잘랐을 때, 문자 경계를 찾아 안전하게 자르는 위치를 재조정하는 데 사용한다.
