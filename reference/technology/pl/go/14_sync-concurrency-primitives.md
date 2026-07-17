# 동시성 기본 도구 (sync/sync-atomic)

# sync 패키지

> **원문:** https://pkg.go.dev/sync

## 개요

`sync` 패키지는 뮤텍스 같은 저수준 동기화 도구를 제공합니다. `Once`와 `WaitGroup`을 제외하면 대부분 저수준 라이브러리 코드에서 쓰라고 만들어진 것이고, 애플리케이션 수준의 동시성 제어는 채널과 통신으로 하는 편이 낫다고 문서는 권합니다.

**공통 규칙**: 이 패키지의 타입은 모두 내부에 상태(락, 카운터 등)를 갖고 있으므로 **첫 사용 이후 값 복사 금지**입니다. 구조체에 담아 함수 인자로 넘길 때도 포인터로 넘겨야 합니다. 대신 제로 값 그대로 바로 쓸 수 있도록 설계되어 있어 별도의 초기화 함수가 필요 없습니다.

---

## Mutex — 상호 배제 락

```go
type Mutex struct{ /* 내부 상태 */ }

func (m *Mutex) Lock()
func (m *Mutex) Unlock()
func (m *Mutex) TryLock() bool // go1.18+
```

- 제로 값이 곧 "잠기지 않은" 뮤텍스라서 `var mu sync.Mutex`만으로 바로 사용 가능합니다.
- `Lock`을 호출한 고루틴과 `Unlock`을 호출하는 고루틴이 달라도 됩니다. 즉 락 소유권이 고루틴에 묶여 있지 않습니다.
- 잠기지 않은 뮤텍스에 `Unlock`을 호출하면 런타임 에러가 납니다.
- `TryLock`은 즉시 성공/실패를 반환하는 비블로킹 버전인데, 문서는 "TryLock을 쓰고 싶어진다는 것 자체가 설계에 더 근본적인 문제가 있다는 신호인 경우가 많다"고 경고합니다. 남발하지 말 것.

```go
var mu sync.Mutex
var counter int

func inc() {
    mu.Lock()
    defer mu.Unlock()
    counter++
}
```

---

## RWMutex — 읽기/쓰기 락

```go
type RWMutex struct{ /* 내부 상태 */ }

func (rw *RWMutex) Lock()
func (rw *RWMutex) Unlock()
func (rw *RWMutex) RLock()
func (rw *RWMutex) RUnlock()
func (rw *RWMutex) TryLock() bool   // go1.18+
func (rw *RWMutex) TryRLock() bool  // go1.18+
func (rw *RWMutex) RLocker() Locker
```

- 읽기 락(`RLock`)은 여러 고루틴이 동시에 가질 수 있지만, 쓰기 락(`Lock`)은 단 하나만 가질 수 있고 읽기 락과도 배타적입니다.
- **쓰기 요청이 대기 중이면 이후의 새 `RLock` 요청은 그 쓰기 락이 처리될 때까지 블록됩니다.** 읽기 요청이 계속 몰려서 쓰기가 영원히 밀리는 상황(writer starvation)을 막기 위한 장치입니다.
- 같은 고루틴이 `RLock`을 재귀적으로 두 번 거는 것은 위험합니다 — 그사이 다른 고루틴이 `Lock`을 요청하면 데드락이 날 수 있습니다.
- `RLock`을 `Lock`으로 업그레이드하거나 그 반대로 다운그레이드하는 기능은 없습니다.
- 읽기가 압도적으로 많고 쓰기가 드문 캐시성 데이터에 적합합니다.

```go
var mu sync.RWMutex
var cache = map[string]string{}

func get(k string) string {
    mu.RLock()
    defer mu.RUnlock()
    return cache[k]
}

func set(k, v string) {
    mu.Lock()
    defer mu.Unlock()
    cache[k] = v
}
```

---

## WaitGroup — 고루틴 대기

```go
type WaitGroup struct{ /* 내부 상태 */ }

func (wg *WaitGroup) Add(delta int)
func (wg *WaitGroup) Done()
func (wg *WaitGroup) Wait()
func (wg *WaitGroup) Go(f func()) // go1.25+
```

- 내부 카운터를 두고 `Add`로 늘리고 `Done`(=`Add(-1)`)으로 줄이며, `Wait`는 카운터가 0이 될 때까지 블록합니다.
- **호출 순서 규칙**: 카운터가 0인 상태에서 양수 delta로 `Add`를 부르는 호출은 반드시 그 이후의 `Wait` 호출보다 먼저 일어나야(happens-before) 합니다. 실무적으로는 고루틴을 띄우기 *전에* `Add`를 호출하라는 뜻입니다. 고루틴 안에서 `Add`를 부르면 `Wait`가 먼저 실행되어 카운터를 0으로 오인하고 즉시 리턴해버리는 경쟁 상태가 생길 수 있습니다.
- 카운터가 음수가 되면 패닉이 납니다.
- 같은 `WaitGroup`을 재사용해 여러 차례의 독립적인 대기에 쓰려면, 새로운 `Add` 호출은 이전 `Wait`가 모두 리턴한 뒤에 해야 합니다.
- Go 1.25부터는 `Add`+고루틴 실행+`Done`을 한 번에 묶어주는 `Go(f)`가 추가됐습니다.

```go
var wg sync.WaitGroup
for _, url := range urls {
    wg.Add(1)
    go func(u string) {
        defer wg.Done()
        fetch(u)
    }(url)
}
wg.Wait()

// go1.25+: Add/Done을 대신 처리해주는 Go 메서드
var wg2 sync.WaitGroup
for _, url := range urls {
    wg2.Go(func() { fetch(url) })
}
wg2.Wait()
```

---

## Once, OnceFunc, OnceValue, OnceValues — 단 한 번만 실행

```go
type Once struct{ /* 내부 상태 */ }

func (o *Once) Do(f func())

func OnceFunc(f func()) func()                          // go1.21+
func OnceValue[T any](f func() T) func() T               // go1.21+
func OnceValues[T1, T2 any](f func() (T1, T2)) func() (T1, T2) // go1.21+
```

- `Once.Do(f)`는 같은 `Once` 인스턴스에 대해 여러 번 호출되어도 `f`를 딱 한 번만 실행합니다. 두 번째 호출부터는 `f`가 다른 함수여도 무시됩니다.
- 초기화 로직을 한 번만 실행하고 싶을 때(싱글턴, 설정 로딩 등) 쓰는 전형적인 패턴입니다.
- **주의**: `f` 내부에서 같은 `Once`의 `Do`를 다시 호출하면 데드락입니다.
- `f`가 패닉을 던지면 `Do`는 "실행 완료"로 간주하고, 이후 호출은 `f`를 다시 실행하지 않고 그냥 리턴합니다.
- Go 1.21부터는 `Once`를 감싸는 함수형 헬퍼가 추가됐습니다. `OnceFunc`는 부작용만 있는 함수를, `OnceValue`/`OnceValues`는 반환값을 캐싱하는 함수를 만들어줍니다. 이들이 감싼 함수가 패닉을 내면, 반환된 함수는 호출될 때마다 같은 패닉을 다시 던집니다.

```go
var once sync.Once
var config *Config

func loadConfig() *Config {
    once.Do(func() {
        config = readConfigFile()
    })
    return config
}

// go1.21+: 캐싱까지 한 번에
var getConfig = sync.OnceValue(func() *Config {
    return readConfigFile()
})
```

---

## Cond — 조건 변수

```go
type Cond struct {
    L Locker
    // 내부 상태
}

func NewCond(l Locker) *Cond
func (c *Cond) Wait()
func (c *Cond) Signal()
func (c *Cond) Broadcast()
```

- 락(`L`)과 결합해 "조건이 참이 될 때까지 대기"하는 패턴을 구현합니다.
- 사용 관용구는 항상 다음 형태여야 합니다.

```go
c.L.Lock()
for !condition() {
    c.Wait()
}
// condition()이 참인 상태에서 로직 수행
c.L.Unlock()
```

- `Wait()`는 `L`을 원자적으로 언락하고 대기 상태로 들어갔다가, 깨어나면 다시 `L`을 락한 뒤 리턴합니다. 그래서 `Wait` 호출 전후로 락이 걸려 있어야 합니다.
- `Signal()`은 대기 중인 고루틴 중 하나만, `Broadcast()`는 전부를 깨웁니다.
- **`Wait`에서 깨어났다고 condition이 참이라는 보장은 없습니다.** 다른 고루틴이 먼저 낚아채 갔을 수도 있으므로 반드시 `for` 루프로 조건을 재확인해야 합니다(`if`가 아니라 `for`).
- 문서는 채널로 대체 가능한 경우가 많다고 언급합니다 — `Broadcast`는 채널을 닫는 것으로, `Signal`은 채널에 값을 보내는 것으로 대응시킬 수 있습니다.

---

## Locker 인터페이스

```go
type Locker interface {
    Lock()
    Unlock()
}
```

`Mutex`, `RWMutex.RLocker()`가 반환하는 값, `Cond.L`이 이 인터페이스를 만족합니다. 락처럼 동작하는 아무 객체나 추상화해서 다루고 싶을 때 씁니다.

---

## Map — 동시성 맵

```go
type Map struct{ /* 내부 상태 */ }

func (m *Map) Load(key any) (value any, ok bool)
func (m *Map) Store(key, value any)
func (m *Map) LoadOrStore(key, value any) (actual any, loaded bool)
func (m *Map) LoadAndDelete(key any) (value any, loaded bool)
func (m *Map) Delete(key any)
func (m *Map) Swap(key, value any) (previous any, loaded bool)          // go1.20+
func (m *Map) CompareAndSwap(key, old, new any) (swapped bool)          // go1.20+
func (m *Map) CompareAndDelete(key, old any) (deleted bool)             // go1.20+
func (m *Map) Range(f func(key, value any) bool)
func (m *Map) Clear()                                                   // go1.23+
```

- **일반적인 `map` + `Mutex`의 대체재가 아닙니다.** 문서는 명시적으로 "대부분의 코드는 그냥 map에 별도의 락을 쓰는 편이 타입 안전성이나 유지보수 측면에서 낫다"고 말합니다.
- `sync.Map`이 유리한 상황은 딱 두 가지입니다.
  1. 특정 키의 항목이 한 번 쓰이고 그 뒤로는 계속 읽히기만 하는 캐시성 데이터
  2. 여러 고루틴이 서로 겹치지 않는 키 집합을 각자 읽고 쓰는 경우
- `Range`가 순회 중인 맵의 어떤 일관된 스냅샷을 보장하지는 않습니다. 같은 키를 두 번 방문하진 않지만, 순회 도중 값이 바뀌면 어느 시점의 값을 볼지는 불확정입니다. `Range` 실행 중에 `f` 자신을 포함해 다른 메서드를 호출해도 안전합니다.

```go
var m sync.Map

m.Store("a", 1)
if v, ok := m.Load("a"); ok {
    fmt.Println(v)
}
m.Range(func(k, v any) bool {
    fmt.Println(k, v)
    return true // false를 반환하면 순회 중단
})
```

---

## Pool — 재사용 가능한 객체 풀

```go
type Pool struct {
    New func() any
}

func (p *Pool) Get() any
func (p *Pool) Put(x any)
```

- GC 압박을 줄이기 위해 다 쓴 임시 객체를 버리지 않고 재사용하는 용도입니다. `fmt` 패키지가 출력 버퍼를 이런 식으로 재사용합니다.
- `Get()`은 풀에서 아무 항목이나 꺼내오고, 비어 있으면 `New()`를 호출해 새로 만듭니다.
- **경고**: 풀에 넣은 항목은 통보 없이 아무 때나 자동으로 제거될 수 있습니다(주로 GC 타이밍에). 풀만이 유일한 참조를 쥐고 있었다면 그 항목은 그대로 회수됩니다. 따라서 Pool을 "반드시 유지되는 저장소"로 오해하면 안 됩니다.
- 짧게 살고 죽는 객체의 프리 리스트 용도로는 적합하지 않습니다 — 오버헤드가 상각되지 않습니다. 여러 독립적인 클라이언트가 공유하는 일시적 객체(버퍼 등)에 적합합니다.
- `New`는 보통 포인터 타입을 반환하도록 작성합니다(값 타입을 `any`에 담을 때 생기는 추가 할당을 피하기 위함).

```go
var bufPool = sync.Pool{
    New: func() any { return new(bytes.Buffer) },
}

func render(w io.Writer) {
    buf := bufPool.Get().(*bytes.Buffer)
    buf.Reset()
    buf.WriteString("hello")
    w.Write(buf.Bytes())
    bufPool.Put(buf)
}
```

---

# sync/atomic 패키지

> **원문:** https://pkg.go.dev/sync/atomic

## 개요

`sync/atomic`은 저수준 원자적 메모리 연산을 제공합니다. Go의 오래된 격언대로 "메모리를 공유해서 통신하지 말고, 통신해서 메모리를 공유하라"가 원칙이므로, 대부분의 상황에서는 채널이나 `sync` 패키지가 우선입니다. 다만 카운터, 플래그, 설정 스냅샷처럼 단일 값을 락 없이 다뤄야 할 때 이 패키지가 쓰입니다.

## 함수형 API (레거시) vs 타입 기반 API (권장)

Go 1.19 이전에는 `AddInt32`, `LoadInt64`, `StoreUint32`, `CompareAndSwapPointer`처럼 타입별 자유 함수만 있었습니다. 지금도 남아 있지만, Go 1.19부터 도입된 **타입 기반 API**(`atomic.Int32`, `atomic.Bool` 등)를 쓰는 편이 타입 오류를 줄이고 가독성도 좋아 권장됩니다.

```go
// 레거시 함수형 API
var counter int64
atomic.AddInt64(&counter, 1)
n := atomic.LoadInt64(&counter)

// 권장: 타입 기반 API
var counter2 atomic.Int64
counter2.Add(1)
n2 := counter2.Load()
```

레거시 함수는 `Int32/Int64/Uint32/Uint64/Uintptr/Pointer` 각각에 대해 `CompareAndSwap`, `Load`, `Store`, `Swap` 계열이 있습니다. `Add`와 (Go 1.23부터 추가된) `And`/`Or` 비트 연산은 정수 타입(`Int32/Int64/Uint32/Uint64/Uintptr`)에만 있고 `Pointer`에는 없습니다.

## 타입 기반 API

```go
type Bool struct{ /* ... */ }      // go1.19+
type Int32 struct{ /* ... */ }     // go1.19+
type Int64 struct{ /* ... */ }     // go1.19+
type Uint32 struct{ /* ... */ }    // go1.19+
type Uint64 struct{ /* ... */ }    // go1.19+
type Uintptr struct{ /* ... */ }   // go1.19+
type Pointer[T any] struct{ /* ... */ } // go1.19+, 제네릭

func (x *Int32) Load() int32
func (x *Int32) Store(val int32)
func (x *Int32) Swap(new int32) (old int32)
func (x *Int32) CompareAndSwap(old, new int32) (swapped bool)
func (x *Int32) Add(delta int32) (new int32)
func (x *Int32) And(mask int32) (old int32) // go1.23+
func (x *Int32) Or(mask int32) (old int32)  // go1.23+
```

- `Bool`, `Int32/64`, `Uint32/64`, `Uintptr`은 모두 위와 같은 형태의 메서드 집합을 갖습니다(`Bool`은 `Add`/`And`/`Or`가 없음).
- `Pointer[T]`는 제네릭으로 구현되어 있어 `unsafe.Pointer`를 직접 다루지 않고도 타입 안전하게 포인터를 원자적으로 교체할 수 있습니다.

```go
var flag atomic.Bool
flag.Store(true)
if flag.Load() {
    // ...
}

var seq atomic.Int64
next := seq.Add(1)

var current atomic.Pointer[Config]
current.Store(&Config{Timeout: 5})
cfg := current.Load()
```

## Value — 임의 타입 저장

```go
type Value struct{ /* 내부 상태 */ }

func (v *Value) Load() any
func (v *Value) Store(val any)
func (v *Value) Swap(new any) (old any)                  // go1.17+
func (v *Value) CompareAndSwap(old, new any) (swapped bool) // go1.17+
```

- `Pointer[T]`가 나오기 전부터 있던, 임의 타입 하나를 원자적으로 담는 상자입니다. 제네릭이 필요 없는 경우나 여러 타입을 시기별로 유연하게 다뤄야 할 때 여전히 쓰입니다.
- **제약 1**: 한 `Value`에 대해 `Store`/`Swap`/`CompareAndSwap`을 부를 때는 항상 같은 구체 타입의 값만 넣어야 합니다. 타입이 섞이면 패닉이 납니다.
- **제약 2**: `nil`을 저장할 수 없습니다(`Store(nil)`, `CompareAndSwap(old, nil)` 모두 패닉).
- 설정값을 통째로 교체하는 용도(설정 핫 리로드), 또는 읽기 전용 스냅샷을 락 없이 배포하는 copy-on-write 패턴에 흔히 쓰입니다.

```go
type Config struct{ Timeout int }

var config atomic.Value
config.Store(&Config{Timeout: 5})

go func() {
    for {
        time.Sleep(10 * time.Second)
        config.Store(loadLatestConfig()) // 항상 *Config 타입만 저장
    }
}()

func handle() {
    cfg := config.Load().(*Config) // 락 없이 최신 설정 읽기
    _ = cfg
}
```

## 32비트 플랫폼에서의 정렬(alignment) 주의사항

ARM, 386, 32비트 MIPS 같은 32비트 아키텍처에서는 64비트 값(`Int64`, `Uint64` 및 이에 대응하는 함수)에 접근할 때 **8바이트 정렬**이 되어 있어야 합니다. 정렬이 보장되는 위치는 다음과 같습니다.

- 할당된 구조체(struct)의 첫 번째 필드
- 배열(array)의 첫 번째 원소
- 슬라이스(slice)의 첫 번째 원소
- 전역 변수
- (32비트 아키텍처에서 힙으로 이스케이프하는) 지역 변수

타입 기반 API인 `atomic.Int64`, `atomic.Uint64`는 내부적으로 이 정렬을 스스로 맞춰주므로, 레거시 함수형 API보다 이 문제에서도 더 안전합니다. 구조체에 직접 `int64` 필드를 두고 레거시 함수로 다룬다면 필드 순서에 신경 써야 합니다.

## 메모리 모델과의 관계

Go 메모리 모델 기준으로, 한 원자적 연산 A의 결과가 다른 원자적 연산 B에 의해 관측되면 A는 B보다 먼저 일어난 것으로(happens-before) 취급됩니다. 모든 원자적 연산은 순차적 일관성(sequential consistency)을 갖도록 실행되는 것처럼 보장되며, 이는 C++의 순차적 일관성 원자 연산이나 Java의 `volatile` 변수와 동등한 의미론입니다.

## 정리: 언제 무엇을 쓸까

| 상황 | 선택 |
|------|------|
| 여러 필드를 아우르는 복합적인 임계 구역 보호 | `sync.Mutex` / `sync.RWMutex` |
| 단일 정수·불리언 카운터/플래그 | `atomic.Int64`, `atomic.Bool` 등 |
| 설정값 통째 교체, 락 없는 스냅샷 배포 | `atomic.Value` 또는 `atomic.Pointer[T]` |
| 초기화를 한 번만 수행 | `sync.Once` / `sync.OnceValue` |
| 고루틴 집합이 끝날 때까지 대기 | `sync.WaitGroup` |
| 조건이 충족될 때까지 대기 | `sync.Cond` (대개는 채널이 더 나은 대안) |
| 키가 겹치지 않거나 write-once-read-many인 동시성 맵 | `sync.Map` (아니면 그냥 `map` + `Mutex`) |
| GC 부담을 줄이는 임시 객체 재사용 | `sync.Pool` |
