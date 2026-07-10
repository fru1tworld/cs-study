# fmt: 포맷 입출력

# fmt 패키지

> **원문:** https://pkg.go.dev/fmt

## 개요

`fmt` 패키지는 C의 `printf`/`scanf`와 유사한 방식으로 값을 포맷팅하여 출력하거나, 텍스트를 파싱하여 값을 읽어들이는 기능을 제공합니다. 크게 세 부류로 나뉩니다.

- **출력 함수**: `Print` 계열 — 표준 출력, 문자열, `io.Writer`, 바이트 슬라이스로 값을 씁니다
- **입력 함수**: `Scan` 계열 — 표준 입력, 문자열, `io.Reader`에서 값을 읽습니다
- **포맷 검증(verb)**: `%v`, `%d`, `%s` 등 타입별 서식 지정자

---

## 출력 함수

### 목적지별 분류

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

### 에러 생성: Errorf

- **`Errorf(format string, a ...any) error`**: `Sprintf`처럼 문자열을 만들되 `error` 값으로 반환합니다.
- `%w` verb를 쓰면 인수로 넘긴 에러를 감싸서(wrap) `errors.Is`/`errors.As`/`Unwrap()`이 원본 에러를 찾을 수 있게 합니다.
- Go 1.20부터는 `%w`를 여러 번 써서 에러를 여러 개 감쌀 수 있으며, 이 경우 `Unwrap() []error`를 구현한 값이 반환됩니다.

```go
baseErr := errors.New("not found")
wrapped := fmt.Errorf("user %q 조회 실패: %w", "alice", baseErr)
errors.Is(wrapped, baseErr) // true
```

---

## 입력 함수

### 소스별 분류

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

## 포맷 verb (지정자)

`%[플래그][너비][.정밀도]verb` 형태로 구성됩니다.

### 범용 verb

| verb | 의미 |
|---|---|
| `%v` | 값의 기본 형식 |
| `%+v` | 구조체일 때 필드 이름을 함께 표시 |
| `%#v` | Go 소스 코드 형태의 표현 |
| `%T` | 값의 Go 타입 |
| `%%` | 리터럴 `%` 문자 |

### 불리언 / 정수

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

### 부동소수점 / 복소수

| verb | 의미 |
|---|---|
| `%f` | 소수점 표기, 지수 없음 |
| `%e` / `%E` | 과학적 표기법 |
| `%g` / `%G` | 지수가 크면 `%e`, 아니면 `%f` 형태로 자동 선택 |

### 문자열 / 바이트

| verb | 의미 |
|---|---|
| `%s` | 문자열 그대로 출력 |
| `%q` | 안전하게 이스케이프된 큰따옴표 문자열 |
| `%x` / `%X` | 바이트마다 16진수 두 글자로 변환 |

### 포인터

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

### 너비와 정밀도

- **너비**: 최소 출력 폭. 부족하면 공백(또는 `0` 플래그면 0)으로 채웁니다.
- **정밀도**: `.` 뒤에 지정. 부동소수점은 소수 자릿수, 문자열은 최대 출력 룬 수를 의미합니다.
- `*`를 쓰면 다음 인수(`int`)를 너비/정밀도 값으로 사용합니다.

```go
fmt.Printf("%9.2f\n", 12.3)       // "    12.30"
fmt.Printf("%-9.2f|\n", 12.3)     // "12.30    |"
fmt.Printf("%*.*f\n", 9, 2, 12.3) // 위와 동일, 인수로 너비·정밀도 전달
```

### 플래그

| 플래그 | 효과 |
|---|---|
| `-` | 왼쪽 정렬(오른쪽에 공백 채움) |
| `+` | 숫자 부호를 항상 표시 |
| `#` | 대체 형식: `0b`/`0`/`0x` 접두사 추가, `%p`의 `0x` 제거 등 |
| `공백` | 부호 자리에 빈 공백 확보 |
| `0` | 부호 뒤부터 0으로 채움 |

---

## 커스텀 포맷팅을 위한 인터페이스

### Stringer

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

### GoStringer

```go
type GoStringer interface {
    GoString() string
}
```

`%#v`에서 사용할 Go 소스 코드 형태 표현을 직접 정의할 때 구현합니다.

### Formatter

```go
type Formatter interface {
    Format(f State, verb rune)
}
```

verb, 플래그, 너비, 정밀도까지 전부 직접 제어하고 싶을 때 구현합니다. `f State`를 통해 `f.Width()`, `f.Precision()`, `f.Flag('-')` 등을 조회할 수 있습니다.

### Scanner

```go
type Scanner interface {
    Scan(state ScanState, verb rune) error
}
```

`Scan`/`Scanf` 계열이 값을 읽을 때 커스텀 파싱 로직을 쓰고 싶을 때 구현합니다. `ScanState`는 `ReadRune`, `SkipSpace`, `Token` 등을 제공합니다.

### 재귀 주의

`String()` 안에서 자기 자신을 다시 `%s`나 `%v`로 포맷하면 무한 재귀에 빠집니다. 기반 타입으로 변환해서 사용해야 합니다.

```go
type Celsius float64

func (c Celsius) String() string {
    return fmt.Sprintf("%.1f°C", float64(c)) // float64로 변환 후 포맷
}
```

---

## 참고 사항

- **패닉 복구**: `String()`, `Error()`, `GoString()`이 패닉을 일으키면 `fmt`는 이를 잡아 `%!s(PANIC=메시지)` 형태로 출력에 남깁니다.
- **인터페이스 언랩핑**: 인터페이스 값을 출력할 때는 (`%T`, `%p` 제외) 내부의 구체 타입 값을 기준으로 포맷합니다.
- **SQL 문 조립 금지**: `database/sql` 사용 시 `Sprintf`로 쿼리 문자열을 직접 조립하면 SQL 인젝션 위험이 있으므로 플레이스홀더(`?`, `$1` 등)를 사용해야 합니다.
