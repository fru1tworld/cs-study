# 수학 연산과 난수 생성 (math / math/rand / math/bits)

# math 패키지

> **원문:** https://pkg.go.dev/math

## 개요

`math`는 부동소수점 수학 함수와 상수를 제공하는 표준 라이브러리 패키지다. 대부분의 함수는 `float64`를 받고 `float64`를 반환하며, 특수 케이스(NaN, ±Inf, ±0)에 대한 동작은 IEEE 754 표준을 따른다.

## 상수

```go
const (
    Pi = 3.14159265358979323846
    E  = 2.71828182845904523536
)

const (
    MaxInt64   = 1<<63 - 1
    MaxFloat64 = 1.797693134862315708145274237317043567981e+308
)
```

- 수학 상수: `Pi`, `E`, `Phi`(황금비), `Sqrt2`, `SqrtE`, `Ln2`, `Log2E` 등
- 타입별 최댓값/최솟값: `MaxInt`, `MinInt`, `MaxInt8`~`MaxInt64`, `MaxUint`, `MaxUint8`~`MaxUint64`
- 부동소수점 한계: `MaxFloat32`, `MaxFloat64`, `SmallestNonzeroFloat32/64`
- 정수 타입의 min/max는 상수 표현식으로 정의되어 있어 `int8(math.MaxInt8)`처럼 변환 없이 바로 쓸 수 있다.

## 반올림 계열

| 함수 | 동작 |
|---|---|
| `Floor(x float64) float64` | x보다 작거나 같은 최대 정수 |
| `Ceil(x float64) float64` | x보다 크거나 같은 최소 정수 |
| `Trunc(x float64) float64` | 소수부를 버림(0 방향으로) |
| `Round(x float64) float64` | 가장 가까운 정수로 반올림(0에서 먼 방향) |
| `RoundToEven(x float64) float64` | 가장 가까운 정수로, 동률이면 짝수로 |

`Round`와 `RoundToEven`의 차이는 `.5` 케이스에서 드러난다: `Round(2.5) == 3`이지만 `RoundToEven(2.5) == 2`.

```go
fmt.Println(math.Floor(3.7), math.Ceil(3.2), math.Round(2.5), math.RoundToEven(2.5))
// 3 4 3 2
```

## 거듭제곱·지수·로그

```go
func Pow(x, y float64) float64   // x**y
func Sqrt(x float64) float64
func Cbrt(x float64) float64     // 세제곱근
func Exp(x float64) float64      // e**x
func Log(x float64) float64      // 자연로그
func Log2(x float64) float64
func Log10(x float64) float64
```

- 정수 거듭제곱만 필요하면 `Pow` 대신 반복 곱셈이 더 빠르다. `Pow`는 임의의 실수 지수를 다루기 위한 범용 함수다.
- `0`에 가까운 값에서 정밀도가 중요하면 `Expm1(x)`(= `e^x - 1`), `Log1p(x)`(= `log(1+x)`)를 쓴다.

```go
r := math.Sqrt(2)             // 1.4142135623730951
p := math.Pow(2, 10)          // 1024
```

## 삼각/쌍곡 함수

- 기본 삼각함수: `Sin`, `Cos`, `Tan` (라디안 단위 입력)
- 역삼각함수: `Asin`, `Acos`, `Atan`, 사분면까지 고려하는 `Atan2(y, x float64) float64`
- 쌍곡함수: `Sinh`, `Cosh`, `Tanh`와 그 역함수 `Asinh`, `Acosh`, `Atanh`
- `Sincos(x float64) (sin, cos float64)`로 sin/cos을 한 번에 계산하면 각각 호출하는 것보다 효율적이다

## 절댓값·최대/최소·나머지

```go
func Abs(x float64) float64
func Max(x, y float64) float64
func Min(x, y float64) float64
func Mod(x, y float64) float64        // 부호는 x를 따름, |결과| < |y|
func Remainder(x, y float64) float64  // IEEE 754 나머지
```

- 정수 `int`에는 `math.Abs`를 직접 쓸 수 없다(파라미터가 `float64`). 정수 최대/최소는 Go 1.21부터 추가된 내장 함수 `min`/`max`(제네릭)를 쓰는 것이 일반적이고, `math.Max`/`math.Min`은 float 특유의 NaN 처리가 필요할 때 쓴다.
- `Mod`는 나눗셈의 나머지, `Remainder`는 반올림 나눗셈 기준의 나머지로 서로 다르다.

## 특수값 판별

```go
func NaN() float64
func Inf(sign int) float64          // sign > 0 → +Inf, sign < 0 → -Inf
func IsNaN(f float64) bool
func IsInf(f float64, sign int) bool
```

```go
if math.IsNaN(result) {
    return errors.New("계산 결과가 NaN입니다")
}
```

## 기타 유용한 함수

- `Hypot(p, q float64) float64` — `sqrt(p²+q²)`를 오버플로 없이 계산 (직각삼각형의 빗변)
- `Copysign(f, sign float64) float64` — f의 크기에 sign의 부호를 적용
- `Dim(x, y float64) float64` — `max(x-y, 0)`
- `Modf(f float64) (int, frac float64)` — 정수부와 소수부 분리
- `Float64bits` / `Float64frombits` — IEEE 754 비트 표현과 상호 변환(해시, 직렬화에 사용)

Bessel 함수(`J0`, `J1`, `Jn`, `Y0`...), 감마·에러 함수(`Gamma`, `Erf`, `Lgamma`)도 제공되지만 일반적인 애플리케이션 코드에서는 거의 쓰이지 않는다.

---

# math/rand — 레거시 의사난수 생성

> **원문:** https://pkg.go.dev/math/rand

## 성격

- **암호학적으로 안전하지 않다.** 보안이 필요한 곳(토큰, 키 등)에는 반드시 `crypto/rand`를 사용해야 한다.
- 시뮬레이션, 테스트 데이터 생성, 게임 로직 등 예측 가능해도 무방한 용도에 적합하다.

## 핵심 타입

```go
type Source interface {
    Int63() int64
    Seed(seed int64)
}

type Rand struct { /* ... */ }
```

- `Source`는 난수의 원천을 추상화한 인터페이스, `Rand`는 그 위에 다양한 분포(정수/실수/순열)를 얹은 래퍼다.
- `Rand`는 여러 고루틴에서 동시에 안전하게 쓸 수 없다. 반면 패키지 최상위 함수들(`rand.Intn` 등)은 내부적으로 잠금 처리된 전역 `Rand`를 사용해 동시성에 안전하다.

## 생성과 시딩

```go
func New(src Source) *Rand
func NewSource(seed int64) Source
```

```go
r := rand.New(rand.NewSource(99))
fmt.Println(r.Intn(100))
```

- Go 1.20부터는 `Seed`를 호출하지 않아도 프로그램 시작 시 자동으로 무작위 시드가 설정된다. 그래서 재현 가능한 시퀀스가 필요할 때만 명시적으로 `New(NewSource(seed))`를 쓰면 된다.
- 전역 `rand.Seed(seed)`는 deprecated이며 더 이상 권장되지 않는다. 독립된 생성기가 필요하면 `rand.New`로 별도 인스턴스를 만드는 편이 낫다.

## 정수/실수/순열 생성

```go
func Intn(n int) int          // [0, n)
func Int63n(n int64) int64
func Float64() float64        // [0.0, 1.0)
func Perm(n int) []int        // [0, n)의 무작위 순열
func Shuffle(n int, swap func(i, j int))
```

`Rand` 값에도 동일한 이름의 메서드가 있다(`r.Intn`, `r.Float64`, `r.Perm`, `r.Shuffle` ...).

```go
words := []string{"a", "b", "c", "d"}
rand.Shuffle(len(words), func(i, j int) {
    words[i], words[j] = words[j], words[i]
})
```

`Intn`, `Int31n`, `Int63n`은 `n <= 0`이면 panic한다는 점을 주의해야 한다.

## Zipf 분포

```go
func NewZipf(r *Rand, s, v float64, imax uint64) *Zipf
func (z *Zipf) Uint64() uint64
```

Zipf-Mandelbrot 분포를 따르는 값을 생성한다. 순위 기반 빈도 분포(예: 인기도 시뮬레이션)를 모사할 때 쓴다.

---

# math/rand/v2 — 재설계된 난수 패키지

> **원문:** https://pkg.go.dev/math/rand/v2

## 왜 v2인가

- 기존 `math/rand`의 알고리즘보다 통계적으로 더 우수한 PCG, ChaCha8 생성기를 채택
- `Intn(n)`처럼 이름에 접미사를 붙이던 관례 대신 `IntN`, `Int64N`처럼 대문자 `N`으로 통일하고 제네릭 `N[Int](n Int) Int`를 추가
- 모듈 버전(v2)이므로 하위 호환 부담 없이 API를 정리할 수 있었다

## 소스: PCG와 ChaCha8

```go
func NewPCG(seed1, seed2 uint64) *PCG
func NewChaCha8(seed [32]byte) *ChaCha8

type Source interface {
    Uint64() uint64
}
```

- `PCG`는 128비트 내부 상태를 가진 빠르고 품질 좋은 범용 생성기. 대부분의 용도에 기본으로 적합하다.
- `ChaCha8`은 스트림 암호 기반이라 통계적으로 더 강력하지만, 패키지 문서는 여전히 "보안이 필요한 용도에는 `crypto/rand`를 쓰라"고 명시한다.
- 새 `Rand` 타입은 이 `Source`를 받아 생성한다: `rand.New(rand.NewPCG(1, 2))`.

## 제네릭 N과 경계 있는 정수 생성

```go
func N[Int intType](n Int) Int      // [0, n)
func IntN(n int) int
func Int64N(n int64) int64
func Uint64N(n uint64) uint64
```

```go
v := rand.N(int64(100))              // [0, 100)
d := rand.N(100 * time.Millisecond)  // 시간 값에도 그대로 적용 가능
time.Sleep(d)
```

`N`은 타입 매개변수 덕분에 `int`, `int64`, `time.Duration` 등 어떤 정수 계열 타입에도 그대로 쓸 수 있어, v1에서 타입마다 다른 함수(`Intn`, `Int63n` ...)를 골라 쓰던 번거로움을 없앤다. 모든 `*N` 계열 함수는 `n <= 0`일 때 panic한다는 점은 v1과 동일하다.

## 실수·순열 함수

```go
func Float64() float64        // [0.0, 1.0)
func Perm(n int) []int
func Shuffle(n int, swap func(i, j int))
```

시그니처와 사용법은 v1과 동일하다. 최상위 함수는 전역 소스를 사용하며 동시성에 안전하고, `*Rand` 값의 메서드는 v1과 마찬가지로 동시 사용 시 별도 동기화가 필요하다.

## 마이그레이션 감

기존 코드의 `rand.Intn(n)` → `rand.IntN(n)`, `rand.Int63n(n)` → `rand.Int64N(n)`처럼 이름만 바뀐 경우가 많아 치환이 비교적 단순하다. `import "math/rand/v2"`로 바꾸고 컴파일 에러를 따라가며 이름을 맞추는 방식으로 마이그레이션하면 된다.

---

# math/bits — 저수준 비트 연산

> **원문:** https://pkg.go.dev/math/bits

## 개요

`math/bits`는 부호 없는 정수에 대한 비트 카운팅·회전·반전, 그리고 캐리를 포함한 저수준 산술 연산을 제공한다. 대부분의 함수는 컴파일러가 대상 아키텍처의 전용 명령어(예: `POPCNT`, `BSR`)로 치환해 최적화하므로, 직접 루프를 짜는 것보다 빠르다.

각 함수군은 플랫폼 기본 `uint`용과 `8`/`16`/`32`/`64` 비트 폭이 고정된 변형(`XxxN`)을 함께 제공한다.

```go
const UintSize = 32 << (^uint(0) >> 63)  // 플랫폼의 uint 비트 수 (32 또는 64)
```

## 0의 개수·1의 개수·비트 길이

```go
func LeadingZeros8(x uint8) int
func TrailingZeros8(x uint8) int
func OnesCount8(x uint8) int
func Len8(x uint8) int
```

```go
bits.LeadingZeros8(1)   // 7  (00000001)
bits.TrailingZeros8(14) // 1  (00001110)
bits.OnesCount8(14)     // 3  (1110 → 1이 세 개)
bits.Len8(8)            // 4  (00001000을 표현하는 데 4비트 필요)
```

`OnesCount`(popcount)는 집합 크기 계산이나 해밍 거리 계산에, `Len`은 값 하나를 저장하는 데 필요한 최소 비트 수를 구할 때 쓰인다.

## 회전과 반전

```go
func RotateLeft8(x uint8, k int) uint8   // k가 음수면 오른쪽 회전
func Reverse8(x uint8) uint8              // 비트 순서 전체 반전
func ReverseBytes16(x uint16) uint16      // 바이트 단위로만 순서 반전
```

```go
bits.RotateLeft8(0b00001111, 2) // 0b00111100
bits.Reverse8(0b00010011)       // 0b11001000
```

`ReverseBytes`는 네트워크 바이트 순서(빅엔디언)와 리틀엔디언 사이 변환에 자주 쓰인다.

## 캐리를 포함한 산술

64비트를 넘는 범위의 덧셈·뺄셈·곱셈을 다룰 때, 자리올림/빌림을 명시적으로 주고받는 함수들이다. 빅넘버 연산이나 암호 구현에서 쓰인다.

```go
func Add64(x, y, carry uint64) (sum, carryOut uint64)
func Sub64(x, y, borrow uint64) (diff, borrowOut uint64)
func Mul64(x, y uint64) (hi, lo uint64)   // 128비트 곱셈 결과를 상/하위로 분리
func Div64(hi, lo, y uint64) (quo, rem uint64)
```

```go
// 두 개의 uint64 쌍으로 표현된 128비트 수끼리 더하기
sum1, carry := bits.Add64(lo1, lo2, 0)
sum0, _ := bits.Add64(hi1, hi2, carry)

// 64비트 곱셈에서 오버플로된 상위 비트까지 얻기
hi, lo := bits.Mul64(1<<40, 1<<40) // hi=256, lo=0
```

`Div64`는 `y == 0`이거나 몫이 64비트를 넘치면 panic한다. 오버플로 여부와 무관하게 나머지만 필요하면 panic하지 않는 `Rem64`를 쓴다.

## 요약

- `math`: 부동소수점 수치 계산의 기본 도구 상자. 반올림, 거듭제곱/로그, 삼각함수, 특수값 판별이 핵심.
- `math/rand`: 레거시 의사난수. 자동 시딩(Go 1.20+)과 `rand.New(rand.NewSource(seed))` 조합으로 충분하며, 보안 용도에는 쓰지 않는다.
- `math/rand/v2`: 더 나은 알고리즘(PCG/ChaCha8)과 제네릭 `N` 기반의 정리된 API. 새 코드에서는 v2를 우선 검토한다.
- `math/bits`: popcount, 비트 길이, 회전/반전, 캐리 산술 등 컴파일러 최적화 혜택을 받는 저수준 연산 모음. 비트 조작이 잦은 알고리즘, 빅넘버, 해시 구현에 사용한다.
