# 정규표현식 (regexp)

# regexp — 정규표현식 검색

> **원문:** https://pkg.go.dev/regexp

## 개요

`regexp` 패키지는 RE2 문법(Perl 호환 정규식이 아님)으로 정규표현식 검색을 구현한다.

- **선형 시간 보장**: 백트래킹 기반 엔진과 달리 입력 길이에 비례하는 시간 안에 매칭이 끝난다.
- **매칭 방식**: 기본은 Perl/Python 스타일의 **leftmost-first**(가장 왼쪽에서 시작하되, 우선순위가 높은 대안을 먼저 선택). POSIX ERE 스타일인 **leftmost-longest**로 바꾸려면 `CompilePOSIX`를 쓰거나 `Longest()`를 호출한다.
- UTF-8 문자열을 다루며, 잘못된 바이트 시퀀스는 `utf8.RuneError`로 취급한다.
- `*Regexp` 값은 컴파일 후에는 여러 고루틴에서 동시에 읽기 전용으로 안전하게 쓸 수 있다. 단 `Longest()`처럼 설정을 바꾸는 메서드는 동시 호출에 안전하지 않다.

## 컴파일: Compile / MustCompile

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

## 단순 매칭 여부 확인

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

## 매칭 결과 찾기: Find 계열

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

## 치환: Replace 계열

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

## 분리: Split

```go
func (re *Regexp) Split(s string, n int) []string
```

매칭되는 부분을 구분자 삼아 문자열을 나눈다. `n < 0`이면 제한 없이 모두 분리한다.

```go
regexp.MustCompile(`\s+`).Split("one  two\tthree", -1)
// []string{"one", "two", "three"}
```

## 기타 유틸리티

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

## 빈 매칭과 순서에 대한 주의

- `FindAll*` 계열은 앞선 매칭 바로 뒤에 붙어 발생하는 빈 매칭은 건너뛴다. 그렇지 않으면 무한히 빈 매칭만 쌓일 수 있기 때문이다.
- 캡처 그룹은 여는 괄호 `(` 의 등장 순서대로 왼쪽부터 번호가 매겨진다. `(?:...)` 처럼 캡처하지 않는 그룹은 번호에 포함되지 않는다.

---

# regexp/syntax — 정규식 파서와 컴파일러

> **원문:** https://pkg.go.dev/regexp/syntax

## 개요

`regexp/syntax`는 `regexp` 패키지가 내부적으로 쓰는 저수준 패키지로, 정규식 문자열을 파싱해 구문 트리(AST)로 만들고 그 트리를 실행 가능한 프로그램으로 컴파일한다. 일반적인 정규식 사용에는 필요 없고, 정규식을 분석·변형하는 도구를 만들 때 쓴다.

## 구문 트리: Regexp 타입

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

## Op — 노드 연산자 상수

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

## Flags — 파싱 동작 제어

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

## 컴파일된 프로그램: Prog / Inst

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

## 오류 타입

```go
type Error struct {
    Code ErrorCode
    Expr string
}
```

패턴이 잘못됐을 때 `Parse`/`Compile`이 반환하는 오류로, `Error()`가 어떤 부분(`Expr`)이 왜(`Code`) 문제인지 알려준다. `ErrorCode`의 대표 값: `ErrMissingParen`(괄호 안 닫힘), `ErrInvalidCharClass`(잘못된 `[...]`), `ErrTrailingBackslash`(패턴이 `\`로 끝남), `ErrNestingDepth`(중첩이 너무 깊음).

## 언제 이 패키지를 직접 쓰나

대부분의 코드는 `regexp.Compile`/`MustCompile`만으로 충분하다. `regexp/syntax`는 다음과 같은 상황에서만 필요하다.

- 정규식 패턴 자체를 정적 분석하는 린터·포매터를 만들 때
- 여러 정규식을 조합·최적화하는 커스텀 매칭 엔진을 만들 때
- 정규식이 요구하는 리터럴 접두사를 뽑아내 사전 필터링에 활용할 때
