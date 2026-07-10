# 테스트와 커맨드라인 플래그 (testing/flag)

Go는 별도 프레임워크 없이 표준 라이브러리 `testing` 패키지와 `go test` 명령만으로 단위 테스트·벤치마크·퍼즈 테스트를 실행한다. 커맨드라인 도구를 만들 때는 `flag` 패키지로 옵션을 파싱한다. 이 문서는 두 패키지의 핵심 타입과 함수를 정리한다.

---

# testing 패키지

> **원문:** https://pkg.go.dev/testing

## 개요

`go test`가 실행하는 세 종류의 테스트 함수(`TestXxx`, `BenchmarkXxx`, `FuzzXxx`, `ExampleXxx`)는 각각 `*testing.T`, `*testing.B`, `*testing.F`를 인자로 받는다. 세 타입 모두 `TB` 인터페이스를 만족하며, 실패 보고·로그 출력·정리(cleanup) 같은 공통 동작은 `TB`에 정의되어 있다.

```go
type TB interface {
    Error(args ...any)
    Errorf(format string, args ...any)
    Fatal(args ...any)
    Fatalf(format string, args ...any)
    Fail()
    FailNow()
    Failed() bool
    Log(args ...any)
    Logf(format string, args ...any)
    Helper()
    Cleanup(f func())
    TempDir() string
    Setenv(key, value string)
    Skip(args ...any)
    // ...
}
```

## 실패 보고: Error/Fatal/Fail 계열

- `Error`/`Errorf` — 로그를 남기고 실패로 표시하지만 **함수 실행은 계속**된다 (`Log` + `Fail`)
- `Fatal`/`Fatalf` — 로그를 남기고 실패로 표시한 뒤 `runtime.Goexit()`로 **즉시 종료**한다 (`Log` + `FailNow`). defer는 실행됨
- `Fail`/`FailNow` — 메시지 없이 각각 계속 진행/즉시 종료만 수행
- `Failed() bool` — 지금까지 실패로 표시됐는지 조회

```go
func TestDivide(t *testing.T) {
    got, err := Divide(10, 2)
    if err != nil {
        t.Fatalf("Divide(10, 2) returned error: %v", err) // 더 진행할 의미가 없을 때
    }
    if got != 5 {
        t.Errorf("Divide(10, 2) = %d, want 5", got) // 실패해도 이어서 다른 케이스 확인 가능
    }
}
```

- `Fatal` 계열은 반드시 테스트 자신의 고루틴에서 호출해야 한다 — 별도로 띄운 고루틴 안에서 호출하면 안 됨 (제어용 메서드는 원본 고루틴 전용, 보고용 메서드만 동시 호출 가능)

## Skip: 조건부 건너뛰기

```go
func TestNetwork(t *testing.T) {
    if testing.Short() {
        t.Skip("네트워크 테스트는 -short에서 생략")
    }
    // ...
}
```

- `Skip`/`Skipf` — 로그를 남기고 건너뜀 (`Log` + `SkipNow`)
- `Skipped() bool` — 건너뛰었는지 조회
- `testing.Short() bool` — `go test -short` 플래그 여부. 오래 걸리는 테스트를 조건부로 생략할 때 관용적으로 사용

## Helper와 Cleanup

```go
func mustParse(t *testing.T, s string) int {
    t.Helper() // 실패 위치가 mustParse가 아닌 호출부로 표시되게 함
    n, err := strconv.Atoi(s)
    if err != nil {
        t.Fatalf("parse %q: %v", s, err)
    }
    return n
}

func TestWithTempFile(t *testing.T) {
    dir := t.TempDir() // 테스트 종료 시 자동 삭제
    f, err := os.Create(filepath.Join(dir, "data"))
    if err != nil {
        t.Fatal(err)
    }
    t.Cleanup(func() { f.Close() }) // 등록 역순(LIFO)으로 실행
}
```

- `Helper()` — 이 함수를 테스트 헬퍼로 표시해 실패 로그의 파일/줄 번호가 호출한 테스트 쪽을 가리키게 함
- `Cleanup(f func())` — 테스트(와 하위 서브테스트)가 모두 끝난 뒤 실행할 정리 함수 등록. 여러 번 등록하면 나중에 등록한 것부터 실행
- `TempDir() string` — 테스트 전용 임시 디렉터리를 만들고 종료 시 자동 삭제
- `Setenv(key, value string)` — 환경 변수를 설정하고 테스트 종료 시 원래 값으로 복원. `t.Parallel()`을 쓰는 테스트에서는 사용 불가

## 서브테스트: Run과 테이블 기반 테스트

```go
func (t *T) Run(name string, f func(t *T)) bool
```

- 이름이 `name`인 서브테스트로 `f`를 실행. 부모 테스트 이름 뒤에 `/`로 이어 붙어 `TestFoo/case1`처럼 표시됨
- 반환값은 서브테스트 성공 여부 — 이어지는 로직을 조건부로 건너뛸 때 활용 가능

```go
func TestAdd(t *testing.T) {
    cases := []struct {
        name     string
        a, b     int
        expected int
    }{
        {"양수", 1, 1, 2},
        {"음수", -1, -1, -2},
        {"영", 0, 5, 5},
    }
    for _, c := range cases {
        t.Run(c.name, func(t *testing.T) {
            if got := Add(c.a, c.b); got != c.expected {
                t.Errorf("Add(%d, %d) = %d, want %d", c.a, c.b, got, c.expected)
            }
        })
    }
}
```

- `go test -run TestAdd/음수`처럼 `/`로 구분된 경로 패턴으로 특정 서브테스트만 골라 실행할 수 있다

## Parallel: 병렬 실행

```go
func TestSlow(t *testing.T) {
    t.Parallel() // 다른 Parallel 테스트들과 함께 병렬 실행되도록 표시
    // ...
}
```

- `Parallel()`을 호출한 테스트는 같은 부모 안의 다른 병렬 테스트들과 동시에 실행되며, 병렬이 아닌 테스트와는 함께 실행되지 않음
- 서브테스트 루프에서 병렬 테스트를 만들 때는 루프 변수를 별도로 캡처할 필요가 없다(Go 1.22+ 루프 변수 스코프 변경 이후)

## TestMain: 커스텀 진입점

```go
func TestMain(m *testing.M) {
    setup()
    code := m.Run() // 모든 테스트를 실행하고 종료 코드를 받음
    teardown()
    os.Exit(code)
}
```

- 패키지에 `TestMain`이 정의되어 있으면 `go test`는 이 함수를 호출하고, 개별 테스트 실행은 `m.Run()`에 위임한다
- 전역 자원 초기화/정리, 커스텀 플래그 처리(`flag.Parse()`) 등에 사용

## 벤치마크: B

```go
func BenchmarkFib10(b *testing.B) {
    for i := 0; i < b.N; i++ {
        Fib(10)
    }
}
```

- `b.N` — 벤치마크 프레임워크가 안정적인 시간 측정을 위해 자동으로 조정하는 반복 횟수
- Go 1.24+에서는 `for b.Loop() { ... }` 형태를 권장 — 첫 호출 시 타이머를 리셋하고, `false`를 반환하면 자동으로 정지하며 루프 변수가 컴파일러 최적화로 사라지는 것도 방지해 준다

```go
func BenchmarkSortLarge(b *testing.B) {
    data := generateData() // 측정에서 제외하고 싶은 준비 작업
    b.ResetTimer()
    for b.Loop() {
        sort.Ints(data)
    }
}
```

- `ResetTimer()` — 경과 시간과 메모리 카운터를 0으로 초기화 (준비 작업을 측정에서 빼고 싶을 때)
- `StartTimer()`/`StopTimer()` — 타이머를 재개/일시 정지 (측정하고 싶지 않은 구간을 감쌀 때)
- `ReportAllocs()` — `-benchmem`처럼 할당 통계를 함께 출력
- `SetBytes(n int64)` — 반복당 처리 바이트 수를 기록해 MB/s를 함께 리포트
- `RunParallel(body func(*PB))` — GOMAXPROCS만큼 고루틴을 띄워 병렬로 반복 수행 (`pb.Next()`로 반복 여부 확인)

```bash
$ go test -bench=. -benchmem ./...
BenchmarkFib10-8    5000000    250 ns/op    0 B/op    0 allocs/op
```

## 퍼즈 테스트: F

```go
func FuzzReverse(f *testing.F) {
    f.Add("hello") // 시드 코퍼스 등록
    f.Fuzz(func(t *testing.T, s string) {
        r := Reverse(s)
        if Reverse(r) != s {
            t.Errorf("Reverse(Reverse(%q)) != %q", s, s)
        }
    })
}
```

- `F.Add(args ...any)` — 퍼즈 타겟 시그니처와 같은 타입의 시드 값을 등록. `go test`로 실행할 때는 이 시드들만 순회함
- `F.Fuzz(ff any)` — 퍼즈 타겟을 등록. `ff`는 `func(t *testing.T, args...)` 형태여야 하며, 지원 타입은 `string`, `[]byte`, `bool`, 각종 정수/부동소수점 등으로 제한됨
- `go test -fuzz=FuzzReverse` 로 실행하면 무작위 입력을 생성해 실패 케이스를 찾고, 발견되면 `testdata/fuzz/FuzzReverse/` 아래에 회귀 테스트용으로 저장됨
- 퍼즈 타겟 함수 안에서는 `*F`의 다른 메서드를 호출할 수 없음 (등록 단계와 실행 단계가 분리되어 있기 때문)

## 그 외 전역 함수

| 함수 | 설명 |
|------|------|
| `testing.Short() bool` | `-short` 플래그 여부 |
| `testing.Verbose() bool` | `-v` 플래그 여부 |
| `testing.Coverage() float64` | 현재까지 커버리지 비율 |

## Example 함수: 실행 가능한 문서

```go
func ExampleReverse() {
    fmt.Println(Reverse("hello"))
    // Output: olleh
}
```

- 이름이 `Example`, `ExampleF`(함수 F), `ExampleT_M`(타입 T의 메서드 M) 형태인 함수는 `go test`가 실제로 실행하고, 마지막 `// Output: ...` 주석과 표준 출력을 비교해 검증한다
- 순서가 상관없으면 `// Unordered output:`을 쓴다
- `// Output:` 주석이 없으면 컴파일만 되고 실행은 되지 않는다 — godoc 상의 사용 예시로만 쓰임

---

# flag 패키지

> **원문:** https://pkg.go.dev/flag

## 개요

`flag` 패키지는 `os.Args`를 파싱해 `-name value` 형태의 커맨드라인 플래그를 Go 변수에 채워 넣는다. 패키지 레벨 함수는 내부적으로 전역 `FlagSet`인 `flag.CommandLine`을 사용하며, 여러 개의 독립된 플래그 집합이 필요하면 `flag.NewFlagSet`으로 직접 만들 수도 있다.

## 플래그 정의: 포인터 반환 vs Var 계열

```go
var (
    name  = flag.String("name", "world", "인사할 대상 이름")
    count = flag.Int("count", 1, "반복 횟수")
    debug = flag.Bool("debug", false, "디버그 로그 활성화")
)
```

- `flag.String/Int/Int64/Uint/Uint64/Float64/Bool/Duration(name, defaultValue, usage string) *T` — 새 변수를 할당하고 그 포인터를 반환
- `flag.StringVar/IntVar/...(p *T, name string, value T, usage string)` — 이미 있는 변수에 값을 채움

```go
var port int
flag.IntVar(&port, "port", 8080, "리슨할 포트")
```

- 두 계열 모두 `*FlagSet`의 메서드로도 동일하게 존재한다 (`fs.String(...)`, `fs.IntVar(...)` 등)

## Parse: 실제 파싱 실행

```go
func main() {
    flag.Parse() // os.Args[1:]를 파싱해 위 변수들을 채움
    fmt.Println(*name, *count, *debug)
}
```

- `flag.Parse()` — 정의된 모든 플래그를 읽어 들이고, 첫 번째 플래그가 아닌 인자(또는 `--`)를 만나면 멈춘다
- 반드시 모든 플래그를 정의한 뒤, 값을 읽기 전에 호출해야 한다
- `Parsed() bool` — 이미 `Parse`를 호출했는지 조회

## 커맨드라인 문법 규칙

- `-flag`, `--flag` (대시 1개/2개는 동일하게 취급)
- `-flag=x`, `-flag x` (단, bool 플래그는 `-flag x` 형태를 지원하지 않음 — `-flag=false`로 명시해야 함)
- 정수 플래그는 `1234`, `0664`(8진수), `0x1234`(16진수), 음수를 허용
- bool 플래그는 `1, 0, t, f, true, false` 등을 인식
- duration 플래그는 `time.ParseDuration`이 받아들이는 형식(`1h30m` 등)을 그대로 사용

## 남은 인자: Args/Arg/NArg

```go
flag.Parse()
for i := 0; i < flag.NArg(); i++ {
    fmt.Println(flag.Arg(i))
}
files := flag.Args() // 플래그를 제외한 나머지 인자 슬라이스
```

- `Args() []string` — 플래그 파싱 후 남은 인자들
- `NArg() int` — 남은 인자 개수
- `Arg(i int) string` — i번째 남은 인자 (범위 밖이면 빈 문자열)
- `NFlag() int` — 실제로 설정된(기본값이 아닌) 플래그 개수

## 서브커맨드 패턴: NewFlagSet

`git commit`, `go build`처럼 서브커맨드마다 다른 플래그 집합이 필요하면 별도의 `FlagSet`을 만든다.

```go
func NewFlagSet(name string, errorHandling ErrorHandling) *FlagSet
```

```go
serveCmd := flag.NewFlagSet("serve", flag.ExitOnError)
addr := serveCmd.String("addr", ":8080", "리슨 주소")

migrateCmd := flag.NewFlagSet("migrate", flag.ExitOnError)
dryRun := migrateCmd.Bool("dry-run", false, "실제 반영 없이 확인만")

switch os.Args[1] {
case "serve":
    serveCmd.Parse(os.Args[2:])
    startServer(*addr)
case "migrate":
    migrateCmd.Parse(os.Args[2:])
    runMigration(*dryRun)
default:
    fmt.Fprintf(os.Stderr, "unknown subcommand %q\n", os.Args[1])
    os.Exit(1)
}
```

### ErrorHandling 옵션

| 상수 | 파싱 실패 시 동작 |
|------|------|
| `ContinueOnError` | 에러를 값으로 반환 (기본값, 직접 처리해야 함) |
| `ExitOnError` | `os.Exit(2)` 호출 (`-h`/`-help`는 `os.Exit(0)`) |
| `PanicOnError` | `panic` 호출 |

전역 `flag.Parse()`가 사용하는 `flag.CommandLine`은 `ExitOnError`로 설정되어 있어, 잘못된 플래그를 주면 사용법을 출력하고 프로세스가 즉시 종료된다.

## 커스텀 타입: Value 인터페이스와 Var/Func

기본 제공 타입 외의 값(쉼표로 구분된 목록, 열거형 등)을 플래그로 받고 싶으면 `Value` 인터페이스를 구현한다.

```go
type Value interface {
    String() string
    Set(string) error
}
```

```go
type stringList []string

func (l *stringList) String() string { return strings.Join(*l, ",") }
func (l *stringList) Set(s string) error {
    *l = append(*l, strings.Split(s, ",")...)
    return nil
}

var tags stringList
flag.Var(&tags, "tag", "쉼표로 구분된 태그 목록 (반복 지정 가능)")
```

- `flag.Var(value Value, name, usage string)` — 커스텀 `Value` 구현을 플래그로 등록
- `flag.Func(name, usage string, fn func(string) error)` (Go 1.16+) — 값을 저장할 구조체 없이, 문자열을 받아 검증/처리만 하고 싶을 때 쓰는 축약형
- `flag.BoolFunc(name, usage string, fn func(string) error)` (Go 1.21+) — `Func`의 bool 플래그 버전. `-flag`만 써도 호출됨
- `flag.TextVar(p encoding.TextUnmarshaler, name string, value encoding.TextMarshaler, usage string)` (Go 1.19+) — `encoding.TextUnmarshaler`를 구현하는 타입(예: `net.IP`, 커스텀 enum)을 바로 플래그로 사용

```go
flag.Func("level", "로그 레벨 (debug|info|warn|error)", func(s string) error {
    switch s {
    case "debug", "info", "warn", "error":
        logLevel = s
        return nil
    default:
        return fmt.Errorf("invalid level %q", s)
    }
})
```

## 조회와 Usage 메시지

```go
flag.VisitAll(func(f *flag.Flag) {
    fmt.Printf("%s=%s (기본값 %s)\n", f.Name, f.Value.String(), f.DefValue)
})
```

- `Lookup(name string) *Flag` — 이름으로 등록된 플래그 정보 조회 (미등록이면 `nil`)
- `Visit(fn func(*Flag))` — **설정된** 플래그만 방문 (기본값 그대로인 것은 제외)
- `VisitAll(fn func(*Flag))` — 등록된 **모든** 플래그를 방문
- `Set(name, value string) error` — 코드에서 직접 플래그 값을 설정 (테스트에서 특히 유용)
- `PrintDefaults()` — `-help` 출력 형식으로 모든 플래그와 기본값을 출력
- `var Usage func()` — `-h`/`-help` 또는 파싱 에러 시 호출되는 사용법 출력 함수. 재정의해서 커스텀 메시지를 만들 수 있음

```go
flag.Usage = func() {
    fmt.Fprintf(os.Stderr, "사용법: %s [옵션] <파일...>\n", os.Args[0])
    flag.PrintDefaults()
}
```
