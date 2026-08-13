# Go 에러 처리·로깅, 리플렉션, 테스트, 암호화

## 에러 처리와 로깅 (errors/log/log-slog)

Go는 예외 대신 `error` 값을 반환값으로 다룸 → 표준 라이브러리만으로 에러 래핑·검사·로깅 처리 가능. 이 문서는 `errors`, `log`, `log/slog` 세 패키지의 핵심 사용법 정리.

---

## errors 패키지

> 원문: https://pkg.go.dev/errors

### 개요

- `errors` 패키지 제공 기능
  - 에러 값 생성: `New`
  - 감싸진 에러 검사: `Is`, `As`, `Unwrap`
  - 여러 에러를 하나로 결합: `Join`
- 실제 래핑은 `fmt.Errorf`의 `%w` 포맷 동사로 수행 → `errors` 패키지는 그렇게 만들어진 에러 트리를 순회하는 도구 제공
- `Join`으로 여러 에러 묶음 → 한 에러가 여러 자식을 갖는 구조 → `Is`/`As`는 단순 체인이 아니라 트리를 훑음

### 에러 생성: New

```go
var ErrNotFound = errors.New("record not found")
```

- `errors.New(text string) error` — 주어진 문자열을 담은 에러 값 생성
- 호출할 때마다 별개의 값 생성 → 나중에 비교하려면 패키지 레벨 변수(센티널 에러)로 선언해 재사용 필요
- 같은 문자열로 `New`를 두 번 호출한 값은 `==`로 비교 시 다름

### 에러 래핑: %w와 Unwrap

- `fmt.Errorf("... %w ...", err)`처럼 `%w` 동사 사용 → 원본 에러를 감싸면서 `Unwrap() error` 메서드를 가진 새 에러 생성
- `errors.Unwrap(err error) error` — 감싸인 에러 하나를 꺼냄. `Unwrap` 메서드가 없으면 `nil` 반환

```go
baseErr := errors.New("connection refused")
wrapped := fmt.Errorf("dial tcp: %w", baseErr)

fmt.Println(errors.Unwrap(wrapped) == baseErr) // true
```

### 여러 에러 검사: Is / As

두 함수 모두 에러 트리(err 자신과 `Unwrap`으로 얻어지는 하위 에러들)를 선행 깊이 우선(pre-order depth-first) 순서로 순회 → 직접 비교 대신 이 함수들 사용이 관용적.

- `errors.Is(err, target error) bool` — 트리에서 `target`과 같은 값을 찾음. 값이 같거나, 그 에러가 `Is(error) bool` 메서드를 구현하고 `true`를 돌려주면 매칭
- `errors.As(err error, target any) bool` — 트리에서 `target`이 가리키는 타입과 일치하는 첫 에러를 찾아 `target`에 대입. `target`은 반드시 에러 타입이나 인터페이스를 가리키는 0이 아닌 포인터 필요, 조건 위반 시 패닉

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

- `errors.Is`는 `err == target` 대신, `errors.As`는 `err.(*SomeType)` 타입 단언 대신 쓰는 관용구 → 래핑된 트리 안쪽까지 훑어줌

### 다중 에러 결합: Join

- `errors.Join(errs ...error) error` — 여러 에러를 하나로 묶음. `nil` 값은 무시, 전부 `nil`이면 결과도 `nil`
- 반환된 에러는 `Unwrap() []error` 메서드 구현 → `Is`/`As`가 묶인 에러들 각각을 검사 가능
- 문자열 표현은 각 에러 메시지를 줄바꿈으로 이어붙인 형태

```go
err1 := errors.New("설정 파일 없음")
err2 := errors.New("네트워크 연결 실패")

if err := errors.Join(err1, err2); err != nil {
    fmt.Println(err)                     // 두 줄로 출력
    fmt.Println(errors.Is(err, err1))    // true
}
```

### ErrUnsupported

- 패키지 레벨 변수 `errors.ErrUnsupported` — "지원하지 않는 연산"을 나타내는 센티널 에러 (예: 하드 링크를 지원하지 않는 파일시스템에서의 `os.Link`)
- 이 값을 직접 반환하기보다, 이 값을 감싸거나 `Is` 메서드로 매칭되는 에러를 반환하는 것이 관례

```go
if errors.Is(err, errors.ErrUnsupported) {
    // 대체 로직 수행
}
```

---

## log 패키지

> 원문: https://pkg.go.dev/log

### 개요

- `log` 패키지 제공 요소
  - 단순 텍스트 로그용 `Logger` 타입
  - 별도 로거 생성 없이 바로 쓸 수 있는 표준 로거(패키지 레벨 함수들)
- 구조화된 로그가 필요하면 `log/slog`가 낫음 → 단, 간단한 CLI 도구나 서버 초기화 로그에는 `log`만으로 충분한 경우 많음

### Logger 타입과 생성

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

### 플래그 상수

- `Ldate`: 로컬 날짜 (`2009/01/23`)
- `Ltime`: 로컬 시각 (`01:23:23`)
- `Lmicroseconds`: 마이크로초까지의 시각 (`Ltime`을 포함하는 것으로 간주)
- `Llongfile`: 전체 경로와 줄 번호 (`/a/b/c/d.go:23`)
- `Lshortfile`: 파일명과 줄 번호만 (`d.go:23`), `Llongfile`보다 우선
- `LUTC`: 날짜/시각을 로컬 대신 UTC로 표시
- `Lmsgprefix`: prefix를 줄 맨 앞이 아니라 메시지 직전으로 이동
- `LstdFlags`: `Ldate | Ltime`, 표준 로거의 기본값

### 출력 함수 3계열

`Logger`의 메서드이자, 표준 로거를 대상으로 하는 패키지 레벨 함수로도 동일하게 존재.

- Print 계열 — `Print`/`Printf`/`Println`: `fmt.Print`/`Printf`/`Println`과 동일한 인자 규칙으로 그냥 출력
- Fatal 계열 — `Fatal`/`Fatalf`/`Fatalln`: 출력 후 `os.Exit(1)` 호출 (defer 실행되지 않음에 유의)
- Panic 계열 — `Panic`/`Panicf`/`Panicln`: 출력 후 `panic()` 호출

```go
if err := run(); err != nil {
    log.Fatalf("실행 실패: %v", err) // 메시지 출력 후 프로세스 종료
}
```

### 표준 로거 설정 함수

패키지 레벨에서 표준 로거의 상태를 바꾸는 함수들:

- `log.SetOutput(w io.Writer)` — 출력 대상 변경
- `log.SetFlags(flag int)` — 플래그 변경
- `log.SetPrefix(prefix string)` — 접두어 변경
- `log.Default() *Logger` — 표준 로거 자체를 가져옴 (다른 함수에 넘기거나 `slog`와 연동할 때 유용)

각 `*Logger` 값에도 동일한 이름의 메서드(`SetOutput`, `SetFlags`, `SetPrefix`)와 조회용 메서드(`Flags`, `Prefix`, `Writer`) 존재.

---

## log/slog 패키지

> 원문: https://pkg.go.dev/log/slog

### 개요

- `log/slog`(Go 1.21+) — 키-값 쌍(Attr) 형태의 구조화된 로깅을 표준 라이브러리 수준에서 지원
- `Logger`가 메시지와 속성을 `Handler`에 전달 → Handler가 텍스트나 JSON 등 실제 출력 형식으로 변환
- `log` 패키지보다 필터링·포맷·컨텍스트 전파에 유리

### Logger 생성

```go
func New(h Handler) *Logger
func Default() *Logger
func SetDefault(l *Logger)
```

- `New`는 지정한 `Handler`로 새 로거 생성
- `Default`는 패키지 레벨 함수(`slog.Info` 등)가 사용하는 기본 로거 반환
- `SetDefault`는 기본 로거 교체, 동시에 `log` 패키지의 표준 로거 출력도 이 핸들러를 거치도록 맞춤

```go
logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
slog.SetDefault(logger)
```

### 레벨별 로그 함수

`Logger` 메서드와 패키지 레벨 함수가 쌍으로 존재.

```go
slog.Debug("캐시 미스", "key", "user:42")
slog.Info("요청 처리", "method", "GET", "status", 200)
slog.Warn("재시도 중", "attempt", 3)
slog.Error("저장 실패", "err", err)
```

- 각각 `DebugContext`/`InfoContext`/`WarnContext`/`ErrorContext` 형태로 `context.Context`를 받는 버전도 존재 — 컨텍스트에 값(예: trace id)을 담아 핸들러가 꺼내 쓰게 할 때 사용
- 메시지 뒤의 인자는 `key1, value1, key2, value2, ...` 순서로 나열하거나, `Attr` 값을 직접 섞어 넣을 수 있음
- `Log(ctx, level, msg, args...)` / `LogAttrs(ctx, level, msg, attrs...)` — 임의의 레벨로 로그를 남기고 싶을 때 쓰는 저수준 진입점

### With / WithGroup으로 로거 파생

```go
reqLogger := slog.With("request_id", reqID)
reqLogger.Info("처리 시작")
reqLogger.Info("처리 완료", "duration_ms", 42)
```

- `With(args ...any) *Logger` — 주어진 속성을 항상 포함하는 새 로거 반환 (원본은 그대로)
- `WithGroup(name string) *Logger` — 이후에 추가되는 속성들을 `name.`으로 묶어서 표시하는 새 로거 반환

### 속성(Attr) 만들기

```go
type Attr struct {
    Key   string
    Value Value
}
```

타입별 생성 함수로 값 변환 비용과 오류 감소:

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

### Handler와 내장 구현체

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

### HandlerOptions로 동작 조정

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

### 레벨 타입과 동적 조정

```go
type Level int

const (
    LevelDebug Level = -4
    LevelInfo  Level = 0
    LevelWarn  Level = 4
    LevelError Level = 8
)
```

- 값이 클수록 심각한 레벨 → 이 상수 외의 임의 정수도 레벨로 사용 가능
- `LevelVar`는 실행 중에 레벨을 안전하게 바꿀 수 있는 컨테이너 — 운영 중인 서비스의 로그 레벨을 재시작 없이 높이고 싶을 때 사용

```go
var level = new(slog.LevelVar) // 기본 LevelInfo
h := slog.NewJSONHandler(os.Stderr, &slog.HandlerOptions{Level: level})
slog.SetDefault(slog.New(h))

// 문제 상황 발생 시 디버그 로그로 전환
level.Set(slog.LevelDebug)
```

---

## 리플렉션 기초 (reflect)

> 원문: https://pkg.go.dev/reflect

### 개요

- `reflect` 패키지 — 프로그램이 실행 중에 임의의 타입의 값을 검사하고 조작할 수 있게 해주는 런타임 리플렉션 기능 제공
- 기본 골격: 인터페이스에 담긴 값을 `TypeOf`, `ValueOf`로 꺼내서 그 타입 정보와 실제 데이터에 접근
- `Type`: 값의 정적 타입 정보(이름, 종류, 필드, 메서드 등)를 나타내는 인터페이스
- `Value`: 값 자체를 감싸서 읽기/쓰기/호출 등을 가능하게 하는 구조체
- 대부분의 코드는 `encoding/json`, ORM, 직렬화 라이브러리처럼 제네릭하게 임의 구조체를 다뤄야 할 때만 리플렉션 사용
- Rob Pike의 격언: "리플렉션은 절대 명확하지 않다(reflection is never clear)" → 필요할 때만 최소한으로 사용 권장

---

### Type과 Kind: 타입 정보 얻기

#### TypeOf

```go
func TypeOf(i any) Type
```

- 인터페이스에 담긴 값의 동적 타입을 나타내는 `Type` 반환
- `i`가 nil 인터페이스면 nil 반환

```go
t := reflect.TypeOf(3.14)
fmt.Println(t.Name(), t.Kind()) // float64 float64
```

#### Kind vs Type

- `Type`은 "정확히 어떤 타입인가"(예: `MyInt`, `os.File`)를 나타냄, `Kind()`는 "그 타입의 근본 형태가 무엇인가"(예: `Int`, `Struct`, `Pointer`)를 나타냄
- `type MyInt int`로 정의한 타입은 `Type.Name()`이 `"MyInt"`이지만 `Kind()`는 여전히 `reflect.Int`
- 주요 `Kind` 값: `Bool`, `Int*`, `Uint*`, `Float*`, `Complex*`, `String`, `Array`, `Slice`, `Map`, `Struct`, `Pointer`, `Interface`, `Func`, `Chan`, `Invalid`(잘못되거나 초기화되지 않은 값)
- 옛 이름 `Ptr`는 `Pointer`의 별칭으로 남아있을 뿐 → 새 코드에서는 `Pointer` 사용

```go
switch reflect.ValueOf(v).Kind() {
case reflect.String:
    fmt.Println("문자열")
case reflect.Int, reflect.Int64:
    fmt.Println("정수")
}
```

#### Type의 주요 메서드

- `Name() string`: 패키지 내 타입 이름 (익명 타입은 빈 문자열)
- `Kind() Kind`: 근본 종류
- `NumField()` / `Field(i)`: 구조체 필드 개수 / i번째 필드(`StructField`)
- `FieldByName(name)`: 이름으로 필드 조회
- `NumMethod()` / `Method(i)`: 메서드 개수 / i번째 메서드
- `Elem() Type`: 포인터·슬라이스·배열·맵·채널의 요소 타입 (Kind가 Array/Chan/Map/Pointer/Slice가 아니면 panic)
- `Implements(u Type) bool`: 해당 타입이 인터페이스 `u`를 구현하는지
- `Comparable() bool`: `==` 비교가 가능한 타입인지
- `Size() uintptr`: 값 하나가 차지하는 바이트 수

```go
var w io.Writer
writerType := reflect.TypeOf(&w).Elem() // 인터페이스 타입 얻는 관용 패턴
fileType := reflect.TypeOf((*os.File)(nil)).Elem()
fmt.Println(fileType.Implements(writerType)) // true
```

---

### Value: 값 자체 다루기

#### ValueOf / Zero

```go
func ValueOf(i any) Value
func Zero(typ Type) Value
```

- `ValueOf`는 인터페이스 속 실제 값을 감싼 `Value` 반환. `ValueOf(nil)`은 제로 값 `Value`(`IsValid() == false`)를 돌려줌
- `Zero(t)`는 타입 `t`의 제로 값을 만듦. 예: `Zero(TypeOf(0))`은 `Kind() == Int`이고 값이 0인 `Value`

#### 값을 다시 꺼내기: Interface

```go
func (v Value) Interface() any
```

- `Value`에 담긴 값을 다시 `interface{}`로 꺼냄. 이후 타입 단언(`.(T)`)으로 원래 타입에 접근

```go
v := reflect.ValueOf(42)
n := v.Interface().(int)
```

#### 검사용 메서드

- `Kind()`, `Type()`: 종류와 타입 조회
- `IsValid() bool`: 유효한 값을 담고 있는지 (제로 `Value`는 false)
- `IsNil() bool`: 채널·함수·인터페이스·맵·포인터·슬라이스에서 nil 여부 (그 외 Kind에 호출하면 panic)
- `IsZero() bool`: 해당 타입의 제로 값과 같은지
- `CanSet()` / `CanAddr()` / `CanInterface()`: 수정 가능 여부, 주소를 딸 수 있는지, `Interface()` 호출이 안전한지

#### 기본 타입 값 꺼내기 / 설정하기

읽기용과 쓰기용 메서드가 Kind별로 쌍을 이룸.

- Bool: 읽기 `Bool()` / 쓰기 `SetBool(x bool)`
- Int*: 읽기 `Int()` (int64) / 쓰기 `SetInt(x int64)`
- Uint*: 읽기 `Uint()` (uint64) / 쓰기 `SetUint(x uint64)`
- Float*: 읽기 `Float()` (float64) / 쓰기 `SetFloat(x float64)`
- String: 읽기 `String()` / 쓰기 `SetString(x string)`

`Set(x Value)`는 임의 타입 간에 값 자체를 통째로 대입할 때 사용. `Set` 계열은 반드시 `CanSet() == true`일 때만 호출 → 그렇지 않으면 panic 발생.

```go
i := 10
v := reflect.ValueOf(&i).Elem() // 포인터를 역참조해야 CanSet() == true
v.SetInt(20)
fmt.Println(i) // 20
```

- `ValueOf(i)`처럼 값을 그대로 넘기면 원본의 복사본이라 수정 불가능. 포인터를 넘기고 `Elem()`으로 역참조해야 원본을 변경 가능 — 리플렉션 코드에서 가장 흔히 겪는 함정

---

### 포인터·인터페이스 다루기: Elem, Addr

```go
func (v Value) Elem() Value
func (v Value) Addr() Value
```

- `Elem()`: 포인터가 가리키는 값, 혹은 인터페이스에 담긴 실제 값을 반환
- `Addr()`: `v`의 주소를 가리키는 `Value`(포인터)를 반환. `CanAddr()`가 true여야 함

```go
p := &struct{ X int }{X: 1}
ev := reflect.ValueOf(p).Elem() // X int 구조체 값 자체
ev.Field(0).SetInt(42)
fmt.Println(p.X) // 42
```

---

### 구조체 다루기: Field, StructField, StructTag

#### 필드 접근

`Type`과 `Value` 양쪽 모두 필드 접근용 메서드 제공.

- `Type.NumField()`, `Type.Field(i) StructField`, `Type.FieldByName(name)`
- `Value.NumField()`, `Value.Field(i) Value`, `Value.FieldByName(name) Value`

```go
type User struct {
    Name string
    Age  int
}

u := User{"Gopher", 10}
t := reflect.TypeOf(u)
v := reflect.ValueOf(u)
for i := 0; i < t.NumField(); i++ {
    fmt.Printf("%s = %v\n", t.Field(i).Name, v.Field(i))
}
// Name = Gopher
// Age = 10
```

#### StructField와 태그

```go
type StructField struct {
    Name      string
    PkgPath   string    // 비공개 필드일 때만 채워짐
    Type      Type
    Tag       StructTag
    Offset    uintptr   // 구조체 내 바이트 오프셋
    Index     []int     // Type.FieldByIndex용 인덱스 경로
    Anonymous bool      // 임베딩된 필드인지
}
```

- `StructTag`는 `` `key:"value" key2:"value2"` `` 형식의 원본 태그 문자열을 감싼 타입
- `Tag.Get(key) string`: 값을 꺼냄, 없으면 빈 문자열
- `Tag.Lookup(key) (string, bool)`: 태그가 아예 없는지 vs 값이 빈 문자열인지를 구분해야 할 때 사용

```go
type Person struct {
    Name string `json:"name" validate:"required"`
}

f := reflect.TypeOf(Person{}).Field(0)
fmt.Println(f.Tag.Get("json"))               // name
if v, ok := f.Tag.Lookup("validate"); ok {
    fmt.Println(v)                            // required
}
```

`encoding/json` 같은 라이브러리가 구조체 필드마다 붙은 `json:"..."` 태그를 읽어 직렬화 규칙을 정하는 것이 이 메커니즘.

---

### 메서드 호출: Method, Call

```go
func (v Value) NumMethod() int
func (v Value) Method(i int) Value
func (v Value) MethodByName(name string) Value
func (v Value) Call(in []Value) []Value
```

- `Method(i)` / `MethodByName(name)`은 리시버가 이미 바인딩된 함수 형태의 `Value` 반환
- `Call(in)`은 인자 목록을 `[]Value`로 넘겨 함수/메서드를 실제로 호출하고 결과를 `[]Value`로 돌려받음. 인자 개수·타입이 시그니처와 맞지 않으면 panic 발생 → 사전에 `Type()` 확인이 안전

```go
type Greeter struct{ Name string }
func (g Greeter) Hello(suffix string) string { return "Hello, " + g.Name + suffix }

g := Greeter{"World"}
m := reflect.ValueOf(g).MethodByName("Hello")
out := m.Call([]reflect.Value{reflect.ValueOf("!")})
fmt.Println(out[0].String()) // Hello, World!
```

---

### 컬렉션 다루기: Slice, Map, Array

- `Len()` — 대상: Array/Slice/String/Map/Chan — 길이
- `Cap()` — 대상: Array/Slice/Chan — 용량
- `Index(i)` — 대상: Array/Slice/String — i번째 요소
- `Slice(i, j)` — 대상: Array/Slice/String — 부분 슬라이스
- `MapIndex(key)` — 대상: Map — 키에 대응하는 값(없으면 제로 `Value`)
- `MapKeys()` — 대상: Map — 모든 키의 `[]Value`
- `MapRange() *MapIter` — 대상: Map — `for iter.Next() { iter.Key(); iter.Value() }` 형태의 순회자
- `SetMapIndex(k, v)` — 대상: Map — 키-값 설정 (v가 제로 `Value`면 키 삭제)

```go
m := map[string]int{"a": 1, "b": 2}
v := reflect.ValueOf(m)
iter := v.MapRange()
for iter.Next() {
    fmt.Println(iter.Key(), iter.Value())
}
```

---

### 값 생성하기: New, Make 계열

리플렉션만으로 새로운 값을 만들어야 할 때(제네릭 팩토리, 디코더 구현 등) 쓰는 함수들.

```go
func New(typ Type) Value                        // 새 제로 값을 가리키는 포인터
func MakeSlice(typ Type, len, cap int) Value     // 슬라이스 생성
func MakeMap(typ Type) Value                     // 맵 생성
func MakeChan(typ Type, buffer int) Value        // 채널 생성
func MakeFunc(typ Type, fn func([]Value) []Value) Value // 함수 값 생성
```

```go
t := reflect.TypeOf(User{})
nv := reflect.New(t) // *User를 가리키는 Value
nv.Elem().FieldByName("Name").SetString("New Gopher")
u := nv.Interface().(*User)
fmt.Println(u.Name) // New Gopher
```

`MakeFunc`은 함수 시그니처(`Type`)만 있고 구현은 클로저로 주는 경우에 사용 → 어댑터나 미들웨어를 리플렉션으로 감쌀 때 쓰임.

```go
swap := func(in []reflect.Value) []reflect.Value {
    return []reflect.Value{in[1], in[0]}
}
var intSwap func(int, int) (int, int)
fn := reflect.ValueOf(&intSwap).Elem()
fn.Set(reflect.MakeFunc(fn.Type(), swap))
a, b := intSwap(1, 2)
fmt.Println(a, b) // 2 1
```

관련해서 `ArrayOf`, `SliceOf`, `MapOf`, `ChanOf`, `PointerTo`, `FuncOf`, `StructOf` 같은 `*Of` 함수들은 기존 `Type`을 조합해 새로운 `Type`을 동적으로 정의할 때 사용.

---

### 비교와 기타 유틸리티

```go
func DeepEqual(x, y any) bool
func Copy(dst, src Value) int
func Swapper(slice any) func(i, j int)
```

- `DeepEqual`은 `==`로 비교할 수 없는 슬라이스·맵·포인터가 가리키는 내용까지 재귀적으로 비교. 테스트 코드의 `assertEqual` 류 헬퍼에서 자주 쓰임. 함수 값은 서로 nil인 경우만 같다고 판정
- `Copy(dst, src)`는 슬라이스나 배열 사이에서 요소를 복사하고, 실제로 복사된 개수를 반환(`copy` 내장 함수의 리플렉션 버전)
- `Swapper(slice)`는 `sort.Interface`를 구현할 때 쓸 수 있는 `func(i, j int)` 스왑 함수를 반환

---

### 실전 패턴: 임의 구조체를 map으로 변환

각 개념을 조합하면 아래처럼 리플렉션으로 임의 구조체를 순회하는 코드를 짤 수 있음.

```go
func toMap(v any) map[string]any {
    rv := reflect.ValueOf(v)
    rt := rv.Type()
    out := make(map[string]any, rt.NumField())
    for i := 0; i < rt.NumField(); i++ {
        name := rt.Field(i).Tag.Get("json")
        if name == "" {
            name = rt.Field(i).Name
        }
        out[name] = rv.Field(i).Interface()
    }
    return out
}
```

### 주의할 점

- 리플렉션 코드는 컴파일 타임 타입 검사를 우회 → 잘못된 `Kind`에 메서드를 호출하면 런타임 panic으로 이어짐. 호출 전에 `Kind()`를 확인하는 방어 코드 필수
- 값을 수정하려면 반드시 포인터를 거쳐 `Elem()`으로 얻은 addressable한 `Value`여야 함
- 리플렉션은 일반 코드보다 느림 → 성능이 중요한 경로에서는 코드 생성이나 제네릭 등 대안을 먼저 고려 권장

---

## 테스트와 커맨드라인 플래그 (testing/flag)

- Go는 별도 프레임워크 없이 표준 라이브러리 `testing` 패키지와 `go test` 명령만으로 단위 테스트·벤치마크·퍼즈 테스트 실행
- 커맨드라인 도구를 만들 때는 `flag` 패키지로 옵션 파싱
- 이 문서는 두 패키지의 핵심 타입과 함수 정리

---

## testing 패키지

> 원문: https://pkg.go.dev/testing

### 개요

- `go test`가 실행하는 세 종류의 테스트 함수(`TestXxx`, `BenchmarkXxx`, `FuzzXxx`, `ExampleXxx`)는 각각 `*testing.T`, `*testing.B`, `*testing.F`를 인자로 받음
- 세 타입 모두 `TB` 인터페이스를 만족 → 실패 보고·로그 출력·정리(cleanup) 같은 공통 동작은 `TB`에 정의

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

### 실패 보고: Error/Fatal/Fail 계열

- `Error`/`Errorf` — 로그를 남기고 실패로 표시하지만 함수 실행은 계속됨 (`Log` + `Fail`)
- `Fatal`/`Fatalf` — 로그를 남기고 실패로 표시한 뒤 `runtime.Goexit()`로 즉시 종료 (`Log` + `FailNow`). defer는 실행됨
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

- `Fatal` 계열은 반드시 테스트 자신의 고루틴에서 호출 필요 — 별도로 띄운 고루틴 안에서 호출 금지 (제어용 메서드는 원본 고루틴 전용, 보고용 메서드만 동시 호출 가능)

### Skip: 조건부 건너뛰기

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

### Helper와 Cleanup

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

- `Helper()` — 이 함수를 테스트 헬퍼로 표시 → 실패 로그의 파일/줄 번호가 호출한 테스트 쪽을 가리키게 함
- `Cleanup(f func())` — 테스트(와 하위 서브테스트)가 모두 끝난 뒤 실행할 정리 함수 등록. 여러 번 등록하면 나중에 등록한 것부터 실행
- `TempDir() string` — 테스트 전용 임시 디렉터리를 만들고 종료 시 자동 삭제
- `Setenv(key, value string)` — 환경 변수를 설정하고 테스트 종료 시 원래 값으로 복원. `t.Parallel()`을 쓰는 테스트에서는 사용 불가

### 서브테스트: Run과 테이블 기반 테스트

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

- `go test -run TestAdd/음수`처럼 `/`로 구분된 경로 패턴으로 특정 서브테스트만 골라 실행 가능

### Parallel: 병렬 실행

```go
func TestSlow(t *testing.T) {
    t.Parallel() // 다른 Parallel 테스트들과 함께 병렬 실행되도록 표시
    // ...
}
```

- `Parallel()`을 호출한 테스트는 같은 부모 안의 다른 병렬 테스트들과 동시에 실행 → 병렬이 아닌 테스트와는 함께 실행되지 않음
- 서브테스트 루프에서 병렬 테스트를 만들 때는 루프 변수를 별도로 캡처할 필요 없음(Go 1.22+ 루프 변수 스코프 변경 이후)

### TestMain: 커스텀 진입점

```go
func TestMain(m *testing.M) {
    setup()
    code := m.Run() // 모든 테스트를 실행하고 종료 코드를 받음
    teardown()
    os.Exit(code)
}
```

- 패키지에 `TestMain`이 정의되어 있으면 `go test`는 이 함수를 호출 → 개별 테스트 실행은 `m.Run()`에 위임
- 전역 자원 초기화/정리, 커스텀 플래그 처리(`flag.Parse()`) 등에 사용

### 벤치마크: B

```go
func BenchmarkFib10(b *testing.B) {
    for i := 0; i < b.N; i++ {
        Fib(10)
    }
}
```

- `b.N` — 벤치마크 프레임워크가 안정적인 시간 측정을 위해 자동으로 조정하는 반복 횟수
- Go 1.24+에서는 `for b.Loop() { ... }` 형태를 권장 — 첫 호출 시 타이머를 리셋, `false`를 반환하면 자동으로 정지, 루프 변수가 컴파일러 최적화로 사라지는 것도 방지

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

### 퍼즈 테스트: F

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

- `F.Add(args ...any)` — 퍼즈 타겟 시그니처와 같은 타입의 시드 값을 등록. `go test`로 실행할 때는 이 시드들만 순회
- `F.Fuzz(ff any)` — 퍼즈 타겟을 등록. `ff`는 `func(t *testing.T, args...)` 형태 필요, 지원 타입은 `string`, `[]byte`, `bool`, 각종 정수/부동소수점 등으로 제한
- `go test -fuzz=FuzzReverse`로 실행하면 무작위 입력을 생성해 실패 케이스를 찾음 → 발견되면 `testdata/fuzz/FuzzReverse/` 아래에 회귀 테스트용으로 저장
- 퍼즈 타겟 함수 안에서는 `*F`의 다른 메서드 호출 불가 (등록 단계와 실행 단계가 분리되어 있기 때문)

### 그 외 전역 함수

- `testing.Short() bool` — `-short` 플래그 여부
- `testing.Verbose() bool` — `-v` 플래그 여부
- `testing.Coverage() float64` — 현재까지 커버리지 비율

### Example 함수: 실행 가능한 문서

```go
func ExampleReverse() {
    fmt.Println(Reverse("hello"))
    // Output: olleh
}
```

- 이름이 `Example`, `ExampleF`(함수 F), `ExampleT_M`(타입 T의 메서드 M) 형태인 함수는 `go test`가 실제로 실행 → 마지막 `// Output: ...` 주석과 표준 출력을 비교해 검증
- 순서가 상관없으면 `// Unordered output:` 사용
- `// Output:` 주석이 없으면 컴파일만 되고 실행은 되지 않음 — godoc 상의 사용 예시로만 쓰임

---

## flag 패키지

> 원문: https://pkg.go.dev/flag

### 개요

- `flag` 패키지는 `os.Args`를 파싱해 `-name value` 형태의 커맨드라인 플래그를 Go 변수에 채워 넣음
- 패키지 레벨 함수는 내부적으로 전역 `FlagSet`인 `flag.CommandLine`을 사용 → 여러 개의 독립된 플래그 집합이 필요하면 `flag.NewFlagSet`으로 직접 생성 가능

### 플래그 정의: 포인터 반환 vs Var 계열

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

- 두 계열 모두 `*FlagSet`의 메서드로도 동일하게 존재 (`fs.String(...)`, `fs.IntVar(...)` 등)

### Parse: 실제 파싱 실행

```go
func main() {
    flag.Parse() // os.Args[1:]를 파싱해 위 변수들을 채움
    fmt.Println(*name, *count, *debug)
}
```

- `flag.Parse()` — 정의된 모든 플래그를 읽어 들이고, 첫 번째 플래그가 아닌 인자(또는 `--`)를 만나면 멈춤
- 반드시 모든 플래그를 정의한 뒤, 값을 읽기 전에 호출 필요
- `Parsed() bool` — 이미 `Parse`를 호출했는지 조회

### 커맨드라인 문법 규칙

- `-flag`, `--flag` (대시 1개/2개는 동일하게 취급)
- `-flag=x`, `-flag x` (단, bool 플래그는 `-flag x` 형태를 지원하지 않음 — `-flag=false`로 명시 필요)
- 정수 플래그는 `1234`, `0664`(8진수), `0x1234`(16진수), 음수 허용
- bool 플래그는 `1, 0, t, f, true, false` 등을 인식
- duration 플래그는 `time.ParseDuration`이 받아들이는 형식(`1h30m` 등)을 그대로 사용

### 남은 인자: Args/Arg/NArg

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

### 서브커맨드 패턴: NewFlagSet

`git commit`, `go build`처럼 서브커맨드마다 다른 플래그 집합이 필요하면 별도의 `FlagSet` 생성.

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

#### ErrorHandling 옵션

- `ContinueOnError`: 에러를 값으로 반환 (기본값, 직접 처리 필요)
- `ExitOnError`: `os.Exit(2)` 호출 (`-h`/`-help`는 `os.Exit(0)`)
- `PanicOnError`: `panic` 호출

전역 `flag.Parse()`가 사용하는 `flag.CommandLine`은 `ExitOnError`로 설정 → 잘못된 플래그를 주면 사용법을 출력하고 프로세스가 즉시 종료.

### 커스텀 타입: Value 인터페이스와 Var/Func

기본 제공 타입 외의 값(쉼표로 구분된 목록, 열거형 등)을 플래그로 받고 싶으면 `Value` 인터페이스 구현.

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

### 조회와 Usage 메시지

```go
flag.VisitAll(func(f *flag.Flag) {
    fmt.Printf("%s=%s (기본값 %s)\n", f.Name, f.Value.String(), f.DefValue)
})
```

- `Lookup(name string) *Flag` — 이름으로 등록된 플래그 정보 조회 (미등록이면 `nil`)
- `Visit(fn func(*Flag))` — 설정된 플래그만 방문 (기본값 그대로인 것은 제외)
- `VisitAll(fn func(*Flag))` — 등록된 모든 플래그를 방문
- `Set(name, value string) error` — 코드에서 직접 플래그 값을 설정 (테스트에서 특히 유용)
- `PrintDefaults()` — `-help` 출력 형식으로 모든 플래그와 기본값을 출력
- `var Usage func()` — `-h`/`-help` 또는 파싱 에러 시 호출되는 사용법 출력 함수. 재정의해서 커스텀 메시지를 만들 수 있음

```go
flag.Usage = func() {
    fmt.Fprintf(os.Stderr, "사용법: %s [옵션] <파일...>\n", os.Args[0])
    flag.PrintDefaults()
}
```

---

## 해시와 암호화 기초 (crypto/hash)

- Go 표준 라이브러리 제공 요소
  - 해시 함수를 위한 공통 인터페이스(`hash`)
  - 그 인터페이스를 구현하는 여러 구현체(`crypto/sha256`, `crypto/md5` 등)
  - 암호학적으로 안전한 난수를 생성하는 `crypto/rand`
- 이 문서는 이 네 패키지의 핵심 사용법을 다룸

---

## hash — 해시 함수 공통 인터페이스

> 원문: https://pkg.go.dev/hash

### 개요

- `hash` 패키지는 구체적인 알고리즘을 담고 있지 않음. 대신 `sha256`, `md5`, `crc32` 등 모든 해시 구현체가 공통으로 따르는 인터페이스만 정의
- 알고리즘을 바꾸더라도 `Write` / `Sum` 호출 코드는 그대로 유지 가능

### Hash 인터페이스

```go
type Hash interface {
    io.Writer
    Sum(b []byte) []byte
    Reset()
    Size() int
    BlockSize() int
}
```

- `io.Writer` 임베딩 — `Write(p []byte) (n int, err error)`로 데이터를 스트리밍 방식으로 계속 넣을 수 있음. 한 번에 다 넣지 않아도 되고, 파일이나 네트워크 스트림에서 읽은 조각을 순서대로 넣어도 결과는 동일
- `Sum(b []byte) []byte` — 지금까지 누적된 해시 값을 `b` 뒤에 append해서 반환. 내부 상태는 변경되지 않으므로 `Sum` 이후에도 `Write`를 이어갈 수 있음. 보통 `nil`을 넘겨서 새 슬라이스로 결과만 받음
- `Reset()` — 내부 상태를 초기값으로 되돌려 같은 `Hash` 값을 재사용할 수 있게 함
- `Size()` / `BlockSize()` — 각각 `Sum`이 반환할 바이트 수, 내부적으로 처리하는 블록 크기를 알려줌

```go
func printSum(h hash.Hash, data []byte) {
    h.Reset()
    h.Write(data)
    fmt.Printf("%x\n", h.Sum(nil))
}
```

### Hash32 / Hash64

체크섬류(CRC32, FNV 등)처럼 결과를 정수로 바로 쓰고 싶을 때를 위한 확장 인터페이스.

```go
type Hash32 interface {
    Hash
    Sum32() uint32
}

type Hash64 interface {
    Hash
    Sum64() uint64
}
```

`[]byte` 형태의 `Sum` 대신 고정폭 정수를 바로 얻고 싶을 때 사용.

### Cloner / XOF (최신 버전 추가)

- `Cloner` — `Clone() (Cloner, error)` 메서드로 현재까지 누적된 해시 상태를 복제. 같은 접두사를 공유하는 여러 데이터에 대해 반복 계산을 피하고 싶을 때 유용(예: 같은 헤더 뒤에 여러 바디를 이어 붙여 해시할 때)
- `XOF`(extendable-output function) — 출력 길이가 고정되지 않은 해시(SHA-3의 SHAKE 계열 등)를 위한 인터페이스, `Sum` 대신 `Read`로 원하는 만큼 출력을 뽑아낼 수 있음

이 두 인터페이스는 표준 라이브러리의 대부분 해시 구현체가 이미 만족.

---

## crypto/sha256 — SHA-256 / SHA-224 해시

> 원문: https://pkg.go.dev/crypto/sha256

### 상수

- `Size`: 32 — SHA-256 체크섬의 바이트 길이
- `Size224`: 28 — SHA-224 체크섬의 바이트 길이
- `BlockSize`: 64 — 내부 처리 블록 크기

### 한 번에 계산: Sum256 / Sum224

데이터 전체가 이미 메모리에 있을 때 가장 간단한 방법.

```go
sum := sha256.Sum256([]byte("hello world"))
fmt.Printf("%x\n", sum) // [32]byte 고정 배열 반환
```

`Sum224`도 시그니처는 동일하되 `[Size224]byte`를 반환. 반환 타입이 슬라이스가 아니라 고정 크기 배열이라는 점이 특징 → 필요하면 `sum[:]`로 슬라이스화해서 사용.

### 스트리밍 계산: New / New224

큰 파일이나 네트워크 스트림처럼 한 번에 메모리에 올리기 어려운 데이터를 처리할 때는 `hash.Hash`를 반환하는 `New` 사용.

```go
h := sha256.New()
f, _ := os.Open("bigfile.bin")
defer f.Close()
io.Copy(h, f)          // 파일을 조금씩 읽어 h.Write로 흘려보냄
fmt.Printf("%x\n", h.Sum(nil))
```

`New224()`는 SHA-224 버전의 `hash.Hash`를 만듦. 두 함수가 반환하는 `Hash`는 `encoding.BinaryMarshaler` / `BinaryUnmarshaler`도 구현 → 진행 중인 해시 상태를 직렬화해 저장했다가 나중에 이어서 계산 가능.

### 언제 Sum을, 언제 New를 쓰나

- 데이터가 작고 이미 `[]byte`로 존재 → `Sum256`/`Sum224`가 더 간단
- 파일·스트림처럼 크거나 조각 단위로 들어오는 데이터 → `New()`로 `hash.Hash`를 만들어 `io.Copy` 등으로 흘려보냄

---

## crypto/md5 — MD5 해시

> 원문: https://pkg.go.dev/crypto/md5

### 경고

- MD5는 충돌 공격에 취약한 것으로 알려져 이미 암호학적으로 깨진 알고리즘
- 비밀번호 해싱, 서명, 무결성 검증 등 보안이 중요한 용도로는 사용 금지
- 레거시 포맷 호환이나 비보안 용도의 체크섬(예: 캐시 키, 중복 파일 탐지)에서만 사용

### 상수

- `Size`: 16
- `BlockSize`: 64

### API

`sha256`과 API 형태가 동일.

```go
// 한 번에 계산
sum := md5.Sum([]byte("data"))       // [16]byte 반환

// 스트리밍 계산
h := md5.New()                        // hash.Hash 반환
io.WriteString(h, "chunk1")
io.WriteString(h, "chunk2")
fmt.Printf("%x\n", h.Sum(nil))
```

- `Sum(data []byte) [Size]byte` — 즉시 계산
- `New() hash.Hash` — 스트리밍 계산, `Write`/`Sum`/`Reset` 등 `hash.Hash` 전체 인터페이스 사용 가능

`sha256`, `md5`뿐 아니라 `crypto/sha1`, `crypto/sha512` 등도 전부 같은 `Sum*` / `New*` 패턴을 따름 → 한 패키지의 사용법을 익히면 나머지는 이름만 바꿔서 그대로 적용 가능.

---

## crypto/rand — 암호학적으로 안전한 난수

> 원문: https://pkg.go.dev/crypto/rand

`math/rand`는 예측 가능한 의사난수를 만듦 → 토큰, 세션 키, 솔트(salt), 암호화 키 등 보안이 필요한 값에는 절대 사용 금지. 이런 용도에는 반드시 `crypto/rand` 사용.

### Reader — 전역 난수 소스

```go
var Reader io.Reader
```

- 운영체제가 제공하는 안전한 난수 소스(리눅스 `getrandom`, macOS `arc4random_buf`, 윈도우 `ProcessPrng` 등)를 감싼 `io.Reader`
- 동시성 환경에서도 안전하게 공유해서 쓸 수 있음
- 아래 `Read`, `Int`, `Prime`의 내부 구현이자, 다른 API에 `io.Reader`가 필요할 때 그대로 넘기는 용도로도 쓰임

### Read — 임의 바이트 채우기

```go
func Read(b []byte) (n int, err error)
```

- 슬라이스 `b`를 안전한 난수 바이트로 완전히 채움
- 정상적인 환경에서는 오류를 반환하지 않음(리턴값은 항상 `len(b), nil`) → 내부적으로 문제가 생기면 에러를 반환하는 대신 프로세스를 크래시
- AES 키 같은 대칭키를 만들 때 가장 많이 쓰는 함수

```go
key := make([]byte, 32) // AES-256 키
if _, err := rand.Read(key); err != nil {
    log.Fatal(err)
}
```

### Text — 랜덤 토큰 문자열

```go
func Text() string
```

RFC 4648 base32 알파벳으로 인코딩된, 128비트 이상의 엔트로피를 가진 랜덤 문자열을 바로 반환. API 키, 임시 비밀번호, CSRF 토큰처럼 사람이 다루는 랜덤 문자열이 필요할 때 `Read` + 인코딩을 직접 조합하지 않아도 되는 편의 함수.

```go
token := rand.Text()
fmt.Println(token) // 예: "JBSWY3DPEHPK3PXP..."
```

### Int / Prime — 큰 정수 난수

`math/big`과 함께 사용하는 함수로, 임의 정밀도 정수 범위의 난수나 소수를 구할 때 사용.

```go
func Int(rand io.Reader, max *big.Int) (n *big.Int, err error)
func Prime(r io.Reader, bits int) (*big.Int, error)
```

- `Int(rand.Reader, max)` — `[0, max)` 구간에서 균등 분포로 정수를 뽑음. `max`가 0 이하면 패닉
- `Prime(rand.Reader, bits)` — 지정한 비트 길이를 가지며 소수일 확률이 매우 높은 수를 반환. RSA 키 생성처럼 소수가 필요한 자리에 사용

```go
n, _ := rand.Int(rand.Reader, big.NewInt(100))   // [0,100) 사이 정수
p, _ := rand.Prime(rand.Reader, 128)             // 128비트 소수 후보
```

### 핵심 요점

- 해시가 필요하면 `hash.Hash` 인터페이스(`Write`/`Sum`/`Reset`)를 기준으로 코드를 짜고, 구체적인 알고리즘은 `sha256.New()`처럼 생성자만 바꿔 끼움
- 데이터가 이미 다 있으면 `Sum256`/`Sum` 같은 원샷 함수, 스트림이면 `New()` + `io.Copy`
- MD5, SHA-1은 보안 목적으로 사용 금지, 체크섬 용도로만 사용. 보안이 필요하면 SHA-256 이상 사용
- 키, 토큰, 솔트 등 보안이 걸린 난수는 `math/rand`가 아니라 반드시 `crypto/rand`(`Read`, `Text`, `Int`, `Prime`) 사용
</content>
