# 에러 처리와 로깅 (errors/log/log-slog)

Go는 예외 대신 `error` 값을 반환값으로 다루고, 표준 라이브러리만으로 에러 래핑·검사·로깅을 처리할 수 있다. 이 문서는 `errors`, `log`, `log/slog` 세 패키지의 핵심 사용법을 정리한다.

---

# errors 패키지

> **원문:** https://pkg.go.dev/errors

## 개요

`errors` 패키지는 에러 값을 만들고(`New`), 감싸진 에러를 검사하고(`Is`, `As`, `Unwrap`), 여러 에러를 하나로 합치는(`Join`) 함수들을 제공한다. 실제 래핑은 `fmt.Errorf`의 `%w` 포맷 동사로 이루어지고, `errors` 패키지는 그렇게 만들어진 에러 트리를 순회하는 도구를 제공하는 구조다. `Join`으로 여러 에러를 묶으면 한 에러가 여러 자식을 갖는 구조가 되므로, `Is`/`As`는 단순 체인이 아니라 트리를 훑는다.

## 에러 생성: New

```go
var ErrNotFound = errors.New("record not found")
```

- `errors.New(text string) error` — 주어진 문자열을 담은 에러 값을 만든다
- 호출할 때마다 별개의 값이 생성되므로, 나중에 비교하려면 반드시 패키지 레벨 변수(센티널 에러)로 선언해 재사용해야 한다
- 같은 문자열로 `New`를 두 번 호출한 값은 `==`로 비교하면 다르다

## 에러 래핑: %w와 Unwrap

- `fmt.Errorf("... %w ...", err)`처럼 `%w` 동사를 쓰면 원본 에러를 감싸면서 `Unwrap() error` 메서드를 가진 새 에러가 만들어진다
- `errors.Unwrap(err error) error` — 감싸인 에러 하나를 꺼낸다. `Unwrap` 메서드가 없으면 `nil`을 반환한다

```go
baseErr := errors.New("connection refused")
wrapped := fmt.Errorf("dial tcp: %w", baseErr)

fmt.Println(errors.Unwrap(wrapped) == baseErr) // true
```

## 여러 에러 검사: Is / As

두 함수 모두 에러 트리(err 자신과 `Unwrap`으로 얻어지는 하위 에러들)를 선행 깊이 우선(pre-order depth-first) 순서로 순회하며, 직접 비교 대신 반드시 이 함수들을 사용하는 것이 관용적이다.

- `errors.Is(err, target error) bool` — 트리에서 `target`과 같은 값을 찾는다. 값이 같거나, 그 에러가 `Is(error) bool` 메서드를 구현하고 `true`를 돌려주면 매칭
- `errors.As(err error, target any) bool` — 트리에서 `target`이 가리키는 타입과 일치하는 첫 에러를 찾아 `target`에 대입한다. `target`은 반드시 에러 타입이나 인터페이스를 가리키는 0이 아닌 포인터여야 하며, 조건을 어기면 패닉이 난다

```go
if _, err := os.Open("missing.txt"); err != nil {
    if errors.Is(err, fs.ErrNotExist) {
        fmt.Println("파일이 없습니다")
    }

    var pathErr *fs.PathError
    if errors.As(err, &pathErr) {
        fmt.Println("문제 경로:", pathErr.Path)
    }
}
```

- `errors.Is`는 `err == target` 대신, `errors.As`는 `err.(*SomeType)` 타입 단언 대신 쓰는 관용구다 — 래핑된 트리 안쪽까지 훑어주기 때문

## 다중 에러 결합: Join

- `errors.Join(errs ...error) error` — 여러 에러를 하나로 묶는다. `nil` 값은 무시되고, 전부 `nil`이면 결과도 `nil`
- 반환된 에러는 `Unwrap() []error` 메서드를 구현하므로 `Is`/`As`가 묶인 에러들 각각을 검사할 수 있다
- 문자열 표현은 각 에러 메시지를 줄바꿈으로 이어붙인 형태

```go
err1 := errors.New("설정 파일 없음")
err2 := errors.New("네트워크 연결 실패")

if err := errors.Join(err1, err2); err != nil {
    fmt.Println(err)                     // 두 줄로 출력
    fmt.Println(errors.Is(err, err1))    // true
}
```

## ErrUnsupported

- 패키지 레벨 변수 `errors.ErrUnsupported` — "지원하지 않는 연산"을 나타내는 센티널 에러 (예: 하드 링크를 지원하지 않는 파일시스템에서의 `os.Link`)
- 이 값을 직접 반환하기보다, 이 값을 감싸거나 `Is` 메서드로 매칭되는 에러를 반환하는 것이 관례

```go
if errors.Is(err, errors.ErrUnsupported) {
    // 대체 로직 수행
}
```

---

# log 패키지

> **원문:** https://pkg.go.dev/log

## 개요

`log` 패키지는 단순 텍스트 로그를 위한 `Logger` 타입과, 별도의 로거를 만들지 않고 바로 쓸 수 있는 표준 로거(패키지 레벨 함수들)를 제공한다. 구조화된 로그가 필요하면 `log/slog`를 쓰는 게 낫지만, 간단한 CLI 도구나 서버 초기화 로그에는 `log`만으로 충분한 경우가 많다.

## Logger 타입과 생성

```go
func New(out io.Writer, prefix string, flag int) *Logger
```

- `out` — 로그를 기록할 대상 (`os.Stderr`, 파일, `bytes.Buffer` 등)
- `prefix` — 각 줄 앞에 붙는 문자열 (단, `Lmsgprefix` 플래그가 있으면 헤더 뒤·메시지 앞으로 이동)
- `flag` — 날짜/시간/파일 위치 등 어떤 정보를 자동으로 붙일지 결정하는 비트 플래그

```go
var buf bytes.Buffer
logger := log.New(&buf, "worker: ", log.LstdFlags|log.Lshortfile)
logger.Print("작업 시작")
// worker: 2024/01/02 15:04:05 main.go:10: 작업 시작
```

## 플래그 상수

| 상수 | 의미 |
|------|------|
| `Ldate` | 로컬 날짜 (`2009/01/23`) |
| `Ltime` | 로컬 시각 (`01:23:23`) |
| `Lmicroseconds` | 마이크로초까지의 시각 (`Ltime`을 포함하는 것으로 간주) |
| `Llongfile` | 전체 경로와 줄 번호 (`/a/b/c/d.go:23`) |
| `Lshortfile` | 파일명과 줄 번호만 (`d.go:23`), `Llongfile`보다 우선 |
| `LUTC` | 날짜/시각을 로컬 대신 UTC로 표시 |
| `Lmsgprefix` | prefix를 줄 맨 앞이 아니라 메시지 직전으로 이동 |
| `LstdFlags` | `Ldate \| Ltime`, 표준 로거의 기본값 |

## 출력 함수 3계열

`Logger`의 메서드이자, 표준 로거를 대상으로 하는 패키지 레벨 함수로도 동일하게 존재한다.

- **Print 계열** — `Print`/`Printf`/`Println`: `fmt.Print`/`Printf`/`Println`과 동일한 인자 규칙으로 그냥 출력
- **Fatal 계열** — `Fatal`/`Fatalf`/`Fatalln`: 출력 후 `os.Exit(1)` 호출 (defer 실행되지 않음에 유의)
- **Panic 계열** — `Panic`/`Panicf`/`Panicln`: 출력 후 `panic()` 호출

```go
if err := run(); err != nil {
    log.Fatalf("실행 실패: %v", err) // 메시지 출력 후 프로세스 종료
}
```

## 표준 로거 설정 함수

패키지 레벨에서 표준 로거의 상태를 바꾸는 함수들:

- `log.SetOutput(w io.Writer)` — 출력 대상 변경
- `log.SetFlags(flag int)` — 플래그 변경
- `log.SetPrefix(prefix string)` — 접두어 변경
- `log.Default() *Logger` — 표준 로거 자체를 가져옴 (다른 함수에 넘기거나 `slog`와 연동할 때 유용)

각 `*Logger` 값에도 동일한 이름의 메서드(`SetOutput`, `SetFlags`, `SetPrefix`)와 조회용 메서드(`Flags`, `Prefix`, `Writer`)가 있다.

---

# log/slog 패키지

> **원문:** https://pkg.go.dev/log/slog

## 개요

`log/slog`(Go 1.21+)는 키-값 쌍(Attr) 형태의 구조화된 로깅을 표준 라이브러리 수준에서 지원한다. `Logger`가 메시지와 속성을 `Handler`에 전달하면, Handler가 텍스트나 JSON 등 실제 출력 형식으로 변환한다. `log` 패키지보다 필터링·포맷·컨텍스트 전파에 유리하다.

## Logger 생성

```go
func New(h Handler) *Logger
func Default() *Logger
func SetDefault(l *Logger)
```

- `New`는 지정한 `Handler`로 새 로거를 만든다
- `Default`는 패키지 레벨 함수(`slog.Info` 등)가 사용하는 기본 로거를 반환한다
- `SetDefault`는 기본 로거를 교체하며, 동시에 `log` 패키지의 표준 로거 출력도 이 핸들러를 거치도록 맞춰준다

```go
logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
slog.SetDefault(logger)
```

## 레벨별 로그 함수

`Logger` 메서드와 패키지 레벨 함수가 쌍으로 존재한다.

```go
slog.Debug("캐시 미스", "key", "user:42")
slog.Info("요청 처리", "method", "GET", "status", 200)
slog.Warn("재시도 중", "attempt", 3)
slog.Error("저장 실패", "err", err)
```

- 각각 `DebugContext`/`InfoContext`/`WarnContext`/`ErrorContext` 형태로 `context.Context`를 받는 버전도 있다 — 컨텍스트에 값(예: trace id)을 담아 핸들러가 꺼내 쓰게 할 때 사용
- 메시지 뒤의 인자는 `key1, value1, key2, value2, ...` 순서로 나열하거나, `Attr` 값을 직접 섞어 넣을 수 있다
- `Log(ctx, level, msg, args...)` / `LogAttrs(ctx, level, msg, attrs...)` — 임의의 레벨로 로그를 남기고 싶을 때 쓰는 저수준 진입점

## With / WithGroup으로 로거 파생

```go
reqLogger := slog.With("request_id", reqID)
reqLogger.Info("처리 시작")
reqLogger.Info("처리 완료", "duration_ms", 42)
```

- `With(args ...any) *Logger` — 주어진 속성을 항상 포함하는 새 로거를 반환 (원본은 그대로)
- `WithGroup(name string) *Logger` — 이후에 추가되는 속성들을 `name.`으로 묶어서 표시하는 새 로거를 반환

## 속성(Attr) 만들기

```go
type Attr struct {
    Key   string
    Value Value
}
```

타입별 생성 함수로 값 변환 비용과 오류를 줄인다:

```go
slog.String("user", "alice")
slog.Int("count", 3)
slog.Bool("active", true)
slog.Duration("elapsed", time.Second)
slog.Any("payload", req)          // 임의 타입
slog.Group("http",                // 하위 그룹으로 묶기
    slog.String("method", "GET"),
    slog.Int("status", 200),
)
```

- `String`, `Int`, `Int64`, `Uint64`, `Float64`, `Bool`, `Duration`, `Time`, `Any` — 각 타입 전용 Attr 생성자
- `Group(key string, args ...any) Attr` — 여러 속성을 한 키 아래로 묶음. JSON 핸들러는 중첩 객체로, 텍스트 핸들러는 `key.subkey=value` 형태로 출력

## Handler와 내장 구현체

```go
type Handler interface {
    Enabled(ctx context.Context, level Level) bool
    Handle(ctx context.Context, r Record) error
    WithAttrs(attrs []Attr) Handler
    WithGroup(name string) Handler
}
```

- `NewTextHandler(w io.Writer, opts *HandlerOptions) *TextHandler` — `key=value` 형태를 공백으로 나열 (사람이 읽기 좋음, 로컬 개발용)
- `NewJSONHandler(w io.Writer, opts *HandlerOptions) *JSONHandler` — 한 줄짜리 JSON (로그 수집기 연동용)

```go
h := slog.NewTextHandler(os.Stderr, nil)
slog.New(h).Info("서버 시작", "port", 8080)
// time=2024-01-02T15:04:05.000+09:00 level=INFO msg="서버 시작" port=8080
```

## HandlerOptions로 동작 조정

```go
type HandlerOptions struct {
    AddSource   bool
    Level       Leveler
    ReplaceAttr func(groups []string, a Attr) Attr
}
```

- `AddSource: true` — 로그를 호출한 파일명·줄 번호를 함께 기록
- `Level` — 이 레벨 미만은 버림 (기본값 `LevelInfo`). 정적인 `Level` 값이나 아래의 `LevelVar`를 넣을 수 있음
- `ReplaceAttr` — 속성을 가로채 이름을 바꾸거나 제거할 때 사용 (예: 민감 정보 마스킹, 타임스탬프 키 제거)

```go
opts := &slog.HandlerOptions{
    AddSource: true,
    ReplaceAttr: func(groups []string, a slog.Attr) slog.Attr {
        if a.Key == "password" {
            a.Value = slog.StringValue("[REDACTED]")
        }
        return a
    },
}
slog.New(slog.NewJSONHandler(os.Stdout, opts))
```

## 레벨 타입과 동적 조정

```go
type Level int

const (
    LevelDebug Level = -4
    LevelInfo  Level = 0
    LevelWarn  Level = 4
    LevelError Level = 8
)
```

- 값이 클수록 심각한 레벨이며, 이 상수 외의 임의 정수도 레벨로 쓸 수 있다
- `LevelVar`는 실행 중에 레벨을 안전하게 바꿀 수 있는 컨테이너다 — 운영 중인 서비스의 로그 레벨을 재시작 없이 높이고 싶을 때 사용

```go
var level = new(slog.LevelVar) // 기본 LevelInfo
h := slog.NewJSONHandler(os.Stderr, &slog.HandlerOptions{Level: level})
slog.SetDefault(slog.New(h))

// 문제 상황 발생 시 디버그 로그로 전환
level.Set(slog.LevelDebug)
```
