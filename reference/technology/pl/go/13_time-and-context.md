# 시간과 컨텍스트 (time / context)

# time 패키지

> **원문:** https://pkg.go.dev/time

## 개요

`time` 패키지는 시각(instant)을 나타내는 `Time`, 시간 간격을 나타내는 `Duration`, 타임존 정보를 담는 `Location`을 중심으로 시간 계산, 포맷팅, 파싱, 타이머/티커 기능을 제공합니다.

---

## Time: 시각을 표현하는 값 타입

- 나노초 정밀도로 특정 순간을 표현하며, 항상 값(value)으로 전달하고 포인터로 다루지 않는 것이 관례입니다.
- 제로 값은 `0001-01-01 00:00:00 UTC`이며 `IsZero()`로 확인할 수 있습니다.
- 내부적으로 벽시계(wall clock) 값과 모노토닉 클록(monotonic clock) 값을 함께 들고 있습니다(뒤에서 설명).

### 생성 함수

```go
now := time.Now()                                   // 현재 시각(로컬 타임존)
t1  := time.Date(2024, time.March, 15, 9, 0, 0, 0, time.UTC)
t2, err := time.Parse(time.RFC3339, "2024-03-15T09:00:00Z")
t3  := time.Unix(1710489600, 0)                      // 유닉스 초 → Time
t4  := time.UnixMilli(1710489600000)                 // Go 1.17+
```

- `Date(year, month, day, hour, min, sec, nsec, loc)`는 필드 값이 범위를 벗어나도(예: 13월, 32일) 자동으로 정규화합니다.
- `Parse(layout, value)`는 시간대 정보가 없으면 UTC로 해석하고, `ParseInLocation(layout, value, loc)`은 지정한 위치를 기본 타임존으로 사용합니다.

### 필드 추출

```go
y, m, d := now.Date()
h, mi, s := now.Clock()
now.Year(); now.Month(); now.Day()
now.Weekday()   // time.Sunday ~ time.Saturday
now.YearDay()   // 1~366
```

### 타임존 관련

```go
now.Location()          // *time.Location
name, offset := now.Zone()   // 예: "KST", 32400
utcTime := now.UTC()
localTime := now.Local()
seoul, _ := time.LoadLocation("Asia/Seoul")
inSeoul := now.In(seoul)
```

- `time.Local`, `time.UTC`는 미리 정의된 `*Location` 변수입니다.
- `time.LoadLocation(name)`은 IANA 타임존 데이터베이스에서 이름으로 위치를 불러옵니다(예: `"Asia/Seoul"`).
- `time.FixedZone(name, offsetSec)`은 DST 없이 고정된 오프셋을 갖는 위치를 만듭니다.

### 유닉스 타임 변환

```go
now.Unix()       // 초
now.UnixMilli()  // 밀리초 (Go 1.17+)
now.UnixMicro()  // 마이크로초 (Go 1.17+)
now.UnixNano()   // 나노초
```

---

## Duration: 시간 간격

`Duration`은 나노초 단위 `int64`로 정의된 타입입니다.

```go
const (
    Nanosecond  Duration = 1
    Microsecond          = 1000 * Nanosecond
    Millisecond          = 1000 * Microsecond
    Second               = 1000 * Millisecond
    Minute               = 60 * Second
    Hour                 = 60 * Minute
)

d1 := 500 * time.Millisecond
d2, err := time.ParseDuration("2h45m")   // "300ms", "-1.5h" 같은 문자열도 파싱 가능
```

### 단위 변환 및 가공

```go
d.Seconds()       // float64
d.Milliseconds()  // int64, Go 1.13+
d.Abs()           // 절댓값, Go 1.19+
d.Round(time.Second)     // 가장 가까운 배수로 반올림
d.Truncate(time.Minute)  // 배수로 내림
d.String()        // "72h3m0.5s" 형태
```

---

## 시간 산술과 비교

```go
later := now.Add(2 * time.Hour)
diff  := later.Sub(now)          // Duration
nextMonth := now.AddDate(0, 1, 0) // 연/월/일 단위 가산, 오버플로우 자동 보정

now.Before(later)  // bool
now.After(later)   // bool
now.Equal(later)   // 실제 시각만 비교(Location, 모노토닉 값은 무시)
now.Compare(later) // -1, 0, 1 반환 (Go 1.20+)
```

- **주의**: `==` 연산자로 `Time` 값을 비교하지 마세요. `Location`이나 모노토닉 클록 값이 다르면 실제로 같은 순간이어도 `false`가 나올 수 있습니다. 항상 `Equal`을 사용합니다.
- `AddDate`, `Round`, `Truncate`, `In`, `Local`, `UTC`는 결과에서 모노토닉 클록 정보를 제거합니다.

### 반올림/내림

```go
now.Round(15 * time.Minute)     // 절대 시각 기준 반올림
now.Truncate(24 * time.Hour)    // 절대 시각 기준 내림
```

---

## 포맷팅과 파싱

Go는 `strftime` 같은 지시자 대신 **기준 시각**(reference time) `Mon Jan 2 15:04:05 MST 2006`을 예시 삼아 레이아웃 문자열을 작성합니다. 이 값(월=1, 일=2, 시=15, 분=4, 초=5, 년=06, 타임존=MST/-0700)이 그대로 각 자리의 자릿수를 나타냅니다.

```go
t.Format("2006-01-02 15:04:05")     // "2024-03-15 09:00:00"
t.Format(time.RFC3339)              // "2024-03-15T09:00:00Z"
t.Format("2006년 01월 02일")

parsed, err := time.Parse(time.DateOnly, "2024-03-15")
```

### 자주 쓰는 미리 정의된 레이아웃

| 상수 | 형태 |
|---|---|
| `time.RFC3339` | `2006-01-02T15:04:05Z07:00` |
| `time.RFC3339Nano` | 소수점 이하 나노초 포함 |
| `time.DateOnly` | `2006-01-02` |
| `time.TimeOnly` | `15:04:05` |
| `time.DateTime` | `2006-01-02 15:04:05` |
| `time.Kitchen` | `3:04PM` |

- 파싱에 실패하면 어느 레이아웃 요소가 문제인지 알려주는 `*time.ParseError`가 반환됩니다.
- DB 드라이버나 DBMS별 타임스탬프 파싱과 마찬가지로, 로케일에 따라 요일/월 이름이 아닌 숫자 기반 레이아웃을 사용하는 것이 안전합니다.

---

## 경과 시간 측정과 지연

```go
start := time.Now()
doWork()
elapsed := time.Since(start)   // time.Now().Sub(start)의 축약형

deadline := start.Add(3 * time.Second)
remaining := time.Until(deadline)  // deadline.Sub(time.Now())의 축약형

time.Sleep(200 * time.Millisecond) // 최소 지정한 시간만큼 고루틴을 정지
```

---

## Timer / Ticker: 채널 기반 타이머

`Timer`는 지정된 시간이 지나면 채널로 한 번 값을 보내고, `Ticker`는 일정 간격마다 반복해서 값을 보냅니다.

```go
// 일회성 타이머
timer := time.NewTimer(500 * time.Millisecond)
defer timer.Stop()
<-timer.C

// select와 함께 쓰는 타임아웃 패턴
select {
case result := <-resultCh:
    fmt.Println(result)
case <-time.After(3 * time.Second):
    fmt.Println("timeout")
}

// 주기적 실행
ticker := time.NewTicker(1 * time.Second)
defer ticker.Stop()
for range ticker.C {
    fmt.Println("tick")
}
```

- `time.After(d)`는 `NewTimer(d).C`의 축약형이며, `time.Tick(d)`는 `NewTicker(d).C`의 축약형입니다(단, `Tick`으로 만든 티커는 멈출 방법이 없으므로 반복 호출 시 누수가 생길 수 있어 주의가 필요합니다).
- `Timer.Stop()`, `Ticker.Stop()`으로 리소스를 해제하며, 특히 `Ticker`는 더 이상 필요 없을 때 반드시 `Stop()`을 호출해야 합니다.
- `AfterFunc(d, f)`는 `d`가 지난 뒤 별도 고루틴에서 `f`를 실행하는 `*Timer`를 만듭니다.
- `Timer.Reset(d)` / `Ticker.Reset(d)`로 기존 타이머를 재사용할 수 있습니다.

---

## 모노토닉 클록

- `time.Now()`가 반환하는 `Time` 값은 벽시계 값과 모노토닉 클록 값을 함께 담습니다.
- 벽시계는 NTP 동기화 등으로 앞뒤로 튈 수 있지만, 모노토닉 클록은 항상 단조 증가하므로 경과 시간 측정에 더 안전합니다.
- `Sub`, `Before`, `After`, `Equal`, `Compare`는 양쪽 `Time`이 모두 모노토닉 값을 가지고 있으면 그것을 우선 사용합니다.
- `Format`, `Marshal*` 계열 함수와 `AddDate`, `Round`, `Truncate`, `In`, `Local`, `UTC`는 모노토닉 값을 결과에서 제거합니다.
- 요점: **경과 시간 측정에는 `time.Since`/`Sub`를 쓰고, 벽시계 값 자체가 필요할 때만 `Unix()` 등을 사용**하세요.

---

# context 패키지

> **원문:** https://pkg.go.dev/context

## 개요

`context` 패키지는 여러 고루틴과 API 경계를 넘나드는 요청 범위의 **취소 신호, 마감 시각, 요청 스코프 값**을 전달하기 위한 표준 방법입니다. HTTP 서버, RPC 호출, DB 쿼리 등 취소 가능해야 하는 작업 전반에서 사용됩니다.

## Context 인터페이스

```go
type Context interface {
    Deadline() (deadline time.Time, ok bool)
    Done() <-chan struct{}
    Err() error
    Value(key any) any
}
```

- `Deadline()`: 마감 시각이 설정되어 있으면 그 값과 `ok=true`를 반환합니다.
- `Done()`: 취소되거나 마감 시각이 지나면 닫히는 채널을 반환합니다. `select`문에서 취소 감지 용도로 사용합니다.
- `Err()`: `Done()`이 닫히기 전에는 `nil`, 닫힌 후에는 `context.Canceled` 또는 `context.DeadlineExceeded`를 반환합니다.
- `Value(key)`: 해당 키에 연결된 값을 반환하며, 없으면 `nil`입니다.

## 루트 컨텍스트

```go
ctx := context.Background()  // 어디에도 속하지 않는 최상위 컨텍스트: main, 초기화, 테스트에서 사용
ctx2 := context.TODO()       // 어떤 컨텍스트를 써야 할지 아직 정해지지 않았을 때의 자리표시자
```

두 함수 모두 절대 취소되지 않고 마감 시각도 없는 빈 컨텍스트를 반환합니다. 차이는 의도 표현뿐입니다.

## 취소 가능한 컨텍스트 만들기

```go
ctx, cancel := context.WithCancel(context.Background())
defer cancel()

go func() {
    <-ctx.Done()
    fmt.Println("취소됨:", ctx.Err())
}()

cancel() // Done() 채널을 닫고 Err()가 context.Canceled를 반환하게 함
```

- 반환된 `cancel`(타입 `context.CancelFunc`)은 여러 번, 여러 고루틴에서 호출해도 안전하며 두 번째 호출부터는 아무 일도 하지 않습니다.
- 부모 컨텍스트가 취소되면 자식 컨텍스트도 함께 취소됩니다.
- **`cancel`을 반드시 `defer`로 호출**해서 관련 리소스(내부 고루틴 등)를 해제해야 합니다.

## 마감 시각과 타임아웃

```go
deadline := time.Now().Add(3 * time.Second)
ctx, cancel := context.WithDeadline(context.Background(), deadline)
defer cancel()

// WithTimeout은 WithDeadline(parent, time.Now().Add(timeout))의 축약형
ctx2, cancel2 := context.WithTimeout(context.Background(), 3*time.Second)
defer cancel2()

select {
case <-longRunningOp(ctx2):
    // 정상 완료
case <-ctx2.Done():
    fmt.Println(ctx2.Err()) // "context deadline exceeded"
}
```

- 부모의 마감 시각이 자식보다 이르면, 자식의 마감 시각은 자동으로 부모 것과 같아집니다.
- 마감 시각이 지나면 `Done()`이 닫히고 `Err()`가 `context.DeadlineExceeded`를 반환합니다.

## 취소 사유(cause) 지정 (Go 1.20+)

```go
ctx, cancel := context.WithCancelCause(context.Background())
cancel(errors.New("사용자가 요청을 중단함"))
fmt.Println(context.Cause(ctx))  // "사용자가 요청을 중단함"

ctx2, cancel2 := context.WithTimeoutCause(context.Background(), time.Second, errors.New("업스트림 타임아웃"))
defer cancel2()
```

- `context.Cause(ctx)`는 `CancelCauseFunc`로 설정한 원인이 있으면 그것을, 없으면 일반적인 `Canceled`/`DeadlineExceeded`를 반환합니다.
- 부모와 자식이 서로 다른 원인으로 취소되면, 먼저 취소를 발생시킨 쪽의 원인이 우선합니다.

## 값 전달: WithValue

```go
type ctxKey int

const userIDKey ctxKey = 0

ctx := context.WithValue(context.Background(), userIDKey, "u-123")

if v, ok := ctx.Value(userIDKey).(string); ok {
    fmt.Println("userID:", v)
}
```

- `WithValue`는 요청 하나를 관통하는 **요청 스코프 데이터**(요청 ID, 인증 정보 등) 전달용이며, 함수의 일반적인 매개변수를 대신하는 용도로 쓰면 안 됩니다.
- 키 충돌을 막기 위해 문자열 대신 **비공개(unexported) 커스텀 타입**을 키로 사용하는 것이 관례입니다.
- `context.WithoutCancel(parent)`(Go 1.21+)는 부모의 값은 유지하되 취소·마감 시각 전파는 끊는 컨텍스트를 만듭니다. 백그라운드로 넘겨야 하지만 요청 데이터는 유지하고 싶은 작업에 사용합니다.

## AfterFunc (Go 1.21+)

```go
stop := context.AfterFunc(ctx, func() {
    fmt.Println("컨텍스트가 취소됨, 정리 작업 실행")
})
defer stop()
```

`ctx`가 취소되면 별도 고루틴에서 `f`를 실행합니다. 반환된 `stop` 함수를 호출하면 아직 실행되지 않은 `f`의 실행을 막을 수 있습니다(이미 시작됐거나 컨텍스트가 이미 취소된 상태였다면 `false`를 반환).

## 실전 규칙

- `Context`는 구조체 필드로 저장하지 말고, 함수의 **첫 번째 인수**로 명시적으로 전달하며 관례상 이름은 `ctx`로 씁니다.

  ```go
  func FetchUser(ctx context.Context, id string) (*User, error)
  ```

- `nil` 컨텍스트를 넘기지 말고, 애매하면 `context.TODO()`를 사용합니다.
- `Context`는 여러 고루틴에서 동시에 안전하게 사용할 수 있습니다.
- DB 접근처럼 취소 가능해야 하는 작업에서는 `Context`를 받는 버전(`QueryContext`, `ExecContext` 등)을 사용해, 클라이언트 연결이 끊기거나 타임아웃이 발생하면 하위 작업까지 취소를 전파합니다.
