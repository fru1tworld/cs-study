# Go 정렬·제네릭 컬렉션, 수학 연산, 컨테이너 자료구조

## 정렬과 제네릭 컬렉션 헬퍼 (sort/slices/maps/cmp)

Go 1.21 전후로 표준 라이브러리에 제네릭 기반의 `slices`, `maps`, `cmp` 패키지가 추가되면서, 예전 `sort` 패키지만으로 처리하던 정렬·검색·비교 작업을 훨씬 짧고 타입 안전하게 작성할 수 있게 되었다. 이 문서는 네 패키지에서 실무에서 자주 쓰는 함수 위주로 정리한다.

---

## sort — 인터페이스 기반 정렬

> **원문:** https://pkg.go.dev/sort

### 개요

`sort` 패키지는 Go 1.21 이전부터 존재하던 정렬 라이브러리다. 슬라이스가 `sort.Interface`(`Len`, `Less`, `Swap`)를 구현하기만 하면 임의의 타입을 정렬할 수 있다. `Slice`/`SliceStable`이나 `Ints`/`Strings`/`Float64s` 같은 타입별 편의 함수는 Go 1.22부터 내부적으로 `slices` 패키지에 위임하도록 바뀌어 성능이 개선됐지만, `sort.Sort`/`Stable`처럼 `Interface`를 직접 받는 함수는 별도 구현을 유지한다. 어느 쪽이든 API 자체는 그대로다.

### Interface와 Sort/Stable

```go
type Interface interface {
    Len() int
    Less(i, j int) bool
    Swap(i, j int)
}
```

- `Sort(data Interface)`: `Less`에 따라 오름차순 정렬. 정렬 안정성은 보장하지 않음
- `Stable(data Interface)`: 동일한 값의 상대적 순서를 보존하는 안정 정렬. `Sort`보다 느림

```go
type byLength []string

func (s byLength) Len() int           { return len(s) }
func (s byLength) Less(i, j int) bool { return len(s[i]) < len(s[j]) }
func (s byLength) Swap(i, j int)      { s[i], s[j] = s[j], s[i] }

words := byLength{"banana", "kiwi", "fig"}
sort.Sort(words) // [fig kiwi banana]
```

### Slice / SliceStable

구조체마다 `Interface`를 구현하기 번거로울 때는 `less` 함수만 넘기는 편이 간편하다.

- `Slice(x any, less func(i, j int) bool)`
- `SliceStable(x any, less func(i, j int) bool)`
- `SliceIsSorted(x any, less func(i, j int) bool) bool`

```go
type Person struct {
    Name string
    Age  int
}

people := []Person{{"Bob", 31}, {"Alice", 25}}
sort.Slice(people, func(i, j int) bool {
    return people[i].Age < people[j].Age
})
```

`x`가 `any`이고 리플렉션 기반으로 동작하기 때문에, 타입이 고정되어 있다면 뒤에서 설명할 제네릭 `slices.SortFunc`가 더 빠르고 타입 안전하다.

### 기본 타입용 편의 함수

`Ints`, `Strings`, `Float64s`와 짝을 이루는 `*AreSorted` 함수들이 있다.

| 함수 | 설명 |
|---|---|
| `Ints(x []int)` / `IntsAreSorted(x []int) bool` | int 슬라이스 정렬 / 정렬 여부 확인 |
| `Strings(x []string)` / `StringsAreSorted(...)` | string 슬라이스 정렬 |
| `Float64s(x []float64)` / `Float64sAreSorted(...)` | float64 슬라이스 정렬 (NaN은 맨 앞) |
| `IsSorted(data Interface) bool` | `Interface` 기반 정렬 여부 확인 |

Go 1.22부터 이 함수들은 내부적으로 `slices` 패키지를 호출하도록 바뀌었으므로, 새 코드에서는 `slices.Sort`를 직접 쓰는 편을 권장한다.

### 이진 탐색

정렬된 슬라이스에서 값을 찾을 때 사용한다.

- `Search(n int, f func(int) bool) int`: `f(i)`가 처음으로 `true`가 되는 최소 인덱스를 찾는 범용 이진 탐색
- `SearchInts`, `SearchStrings`, `SearchFloat64s(a, x) int`: 정렬된 슬라이스에서 값을 이진 탐색
- `Find(n int, cmp func(int) int) (i int, found bool)` (Go 1.19+): `cmp`가 0을 반환하는 위치를 찾고 존재 여부도 함께 반환

```go
a := []int{1, 3, 5, 7, 9}
i := sort.SearchInts(a, 5) // 2
```

### Reverse와 편의 슬라이스 타입

- `Reverse(data Interface) Interface`: 정렬 순서를 뒤집은 뷰를 반환 (내부 `Less`를 반전)
- `IntSlice`, `StringSlice`, `Float64Slice`: 각각 `[]int`, `[]string`, `[]float64`에 `Len/Less/Swap`과 `Sort()`, `Search()` 메서드를 얹은 타입. `sort.Reverse`와 조합해 내림차순 정렬을 만들 때 흔히 쓰인다

```go
s := []int{5, 2, 6, 3, 1, 4}
sort.Sort(sort.Reverse(sort.IntSlice(s))) // [6 5 4 3 2 1]
```

---

## cmp — 비교의 최소 공통분모

> **원문:** https://pkg.go.dev/cmp

`slices`와 `maps`의 제네릭 함수들이 공통으로 참조하는 기반 패키지다. 비교 가능한 타입 제약과 3-way 비교 결과(`-1`, `0`, `1`)라는 관례를 여기서 정의한다.

### Ordered 제약

```go
type Ordered interface {
    ~int | ~int8 | ... | ~float32 | ~float64 | ~string
}
```

`<`, `<=`, `>=`, `>` 연산자를 지원하는 정수·부동소수점·문자열 계열 타입을 모두 포함하는 제약이다. `slices.Sort`, `slices.Max` 같은 함수의 타입 매개변수 제약으로 쓰인다.

### Compare / Less

- `Compare[T Ordered](x, y T) int`: x가 작으면 -1, 같으면 0, 크면 1
- `Less[T Ordered](x, y T) bool`: `x < y`와 동일하지만 NaN을 규칙적으로 처리

부동소수점에서는 NaN을 "가장 작은 값"으로 취급해 일관된 전순서를 만든다. 즉 `Compare(NaN, 1.0)`은 -1, `Compare(NaN, NaN)`은 0이다. 이 덕분에 NaN이 섞인 슬라이스도 정렬 시 패닉 없이 한쪽 끝에 몰린다.

```go
cmp.Compare(1, 2)            // -1
cmp.Compare(math.NaN(), 1.0) // -1 (NaN을 가장 작게 취급)
```

### Or — 제로 값 폴백 체이닝

`Or[T comparable](vals ...T) T` (Go 1.22+)는 인자를 순서대로 검사해 제로 값이 아닌 첫 값을 반환한다. 여러 정렬 기준을 우선순위대로 이어 붙일 때 유용하다.

```go
name := cmp.Or(nickname, realName, "익명")

// 정렬 기준을 이어 붙이는 전형적인 패턴
slices.SortFunc(people, func(a, b Person) int {
    return cmp.Or(
        cmp.Compare(a.LastName, b.LastName),
        cmp.Compare(a.FirstName, b.FirstName),
    )
})
```

---

## slices — 제네릭 슬라이스 유틸리티

> **원문:** https://pkg.go.dev/slices

Go 1.21에 도입된 패키지로, `sort`의 기능을 대체하면서 검색·삽입·삭제·비교까지 아우르는 슬라이스 전용 제네릭 함수 모음을 제공한다. 대부분의 함수가 타입 매개변수 `S ~[]E`를 받아 슬라이스 기반 타입(named slice type 포함)에도 그대로 적용된다.

### 정렬

- `Sort[E cmp.Ordered](x []E)`: 오름차순 정렬. `sort.Slice`보다 빠르고 리플렉션이 없음
- `SortFunc[E any](x []E, cmp func(a, b E) int)`: 임의 타입을 3-way 비교 함수로 정렬 (불안정)
- `SortStableFunc[E any](x []E, cmp func(a, b E) int)`: 동일 값의 상대 순서를 보존
- `IsSorted[E cmp.Ordered](x []E) bool` / `IsSortedFunc[E any](x []E, cmp ...) bool`

```go
nums := []int{5, 2, 4, 1, 3}
slices.Sort(nums) // [1 2 3 4 5]

type Task struct {
    Priority int
    Name     string
}
tasks := []Task{{2, "b"}, {1, "a"}}
slices.SortFunc(tasks, func(a, b Task) int {
    return cmp.Compare(a.Priority, b.Priority)
})
```

### 검색

- `BinarySearch[E cmp.Ordered](x []E, target E) (int, bool)`: 정렬된 슬라이스에서 위치와 존재 여부 반환
- `BinarySearchFunc[E, T any](x []E, target T, cmp func(E, T) int) (int, bool)`: 커스텀 비교 함수 버전
- `Contains[E comparable](s []E, v E) bool` / `ContainsFunc[E any](s []E, f func(E) bool) bool`
- `Index[E comparable](s []E, v E) int` / `IndexFunc[E any](s []E, f func(E) bool) int`: 없으면 -1

```go
idx, found := slices.BinarySearch(nums, 3) // 2, true
has := slices.Contains(nums, 10)           // false
```

### 슬라이스 변형 (삽입/삭제/치환)

- `Insert[E any](s []E, i int, v ...E) []E`: 인덱스 `i`에 값 삽입
- `Delete[E any](s []E, i, j int) []E`: `s[i:j]` 구간 제거
- `DeleteFunc[E any](s []E, del func(E) bool) []E`: 조건을 만족하는 원소 제거
- `Replace[E any](s []E, i, j int, v ...E) []E`: `s[i:j]`를 `v`로 치환
- `Compact[E comparable](s []E) []E` / `CompactFunc[E any](s []E, eq func(E, E) bool) []E`: 연속된 중복 원소 제거 (정렬된 슬라이스에서 유용)

```go
s := []int{1, 2, 2, 3, 3, 3}
s = slices.Compact(s) // [1 2 3]

s = slices.Insert(s, 1, 99) // [1 99 2 3]
s = slices.Delete(s, 0, 1)  // [99 2 3]
```

### 복제와 용량 관리

- `Clone[E any](s []E) []E`: 얕은 복사본 생성
- `Clip[E any](s []E) []E`: 여유 용량을 제거해 `len == cap`으로 만듦
- `Grow[E any](s []E, n int) []E`: 최소 n개를 더 담을 수 있도록 용량 확보
- `Concat[E any](slices ...[]E) []E`: 여러 슬라이스를 이어 붙인 새 슬라이스 반환
- `Repeat[E any](x []E, count int) []E`: 슬라이스를 count번 반복

### 비교

- `Equal[E comparable](s1, s2 []E) bool`: 길이와 원소가 모두 같은지 확인
- `EqualFunc[E1, E2 any](s1 []E1, s2 []E2, eq func(E1, E2) bool) bool`
- `Compare[E cmp.Ordered](s1, s2 []E) int`: 사전식 비교 (-1/0/1)
- `CompareFunc[E1, E2 any](s1 []E1, s2 []E2, cmp func(E1, E2) int) int`

### Max/Min, Reverse

- `Max[E cmp.Ordered](x []E) E` / `Min[E cmp.Ordered](x []E) E`: 빈 슬라이스면 패닉
- `MaxFunc`, `MinFunc`: 커스텀 비교 함수 버전
- `Reverse[E any](s []E)`: 슬라이스를 제자리에서 뒤집음

```go
slices.Reverse(nums)   // in-place
max := slices.Max(nums)
```

### 반복자(iterator) 연동 함수 (Go 1.23+)

`range-over-func`(`iter.Seq`)가 도입되면서 슬라이스와 이터레이터를 오가는 함수들이 추가되었다.

| 함수 | 설명 |
|---|---|
| `All(s []E) iter.Seq2[int, E]` | (인덱스, 값) 쌍을 순서대로 순회 |
| `Values(s []E) iter.Seq[E]` | 값만 순서대로 순회 |
| `Backward(s []E) iter.Seq2[int, E]` | 역순으로 순회 |
| `Collect(seq iter.Seq[E]) []E` | 이터레이터를 슬라이스로 수집 |
| `AppendSeq(s []E, seq iter.Seq[E]) []E` | 이터레이터 값을 기존 슬라이스에 추가 |
| `Sorted(seq iter.Seq[E]) []E` | 수집 후 정렬 |
| `SortedFunc` / `SortedStableFunc` | 커스텀 비교자로 수집 후 정렬 |
| `Chunk(s []E, n int) iter.Seq[[]E]` | 크기 n짜리 연속 부분 슬라이스로 분할 |

```go
for i, v := range slices.All(nums) {
    fmt.Println(i, v)
}

top3 := slices.Collect(slices.Values(nums))
```

---

## maps — 제네릭 맵 유틸리티

> **원문:** https://pkg.go.dev/maps

`slices`의 맵 버전으로, Go 1.21에 함께 도입되었다. 맵은 순서가 없으므로 `Sort` 계열 함수는 없고, 대신 키/값 열거, 복사, 비교, 조건부 삭제에 집중한다.

### 반복자 함수 (Go 1.23+)

- `Keys(m M) iter.Seq[K]`: 키 이터레이터 (순서 미보장)
- `Values(m M) iter.Seq[V]`: 값 이터레이터
- `All(m M) iter.Seq2[K, V]`: (키, 값) 쌍 이터레이터
- `Collect(seq iter.Seq2[K, V]) map[K]V`: 이터레이터를 새 맵으로 수집
- `Insert(m M, seq iter.Seq2[K, V])`: 이터레이터의 키/값을 기존 맵에 덮어쓰며 삽입

```go
m := map[string]int{"a": 1, "b": 2}

keys := slices.Collect(maps.Keys(m))   // slices.Collect와 함께 자주 씀
slices.Sort(keys)                      // 맵 자체엔 순서가 없으므로 정렬은 별도로

for k, v := range maps.All(m) {
    fmt.Println(k, v)
}
```

맵은 순서가 없기 때문에, 정렬된 순서로 순회하고 싶다면 `maps.Keys`로 키를 뽑은 뒤 `slices.Sort`(또는 `slices.Sorted(maps.Keys(m))`)로 정렬하는 조합이 관용구처럼 쓰인다.

### 비교와 복사

- `Equal(m1, m2 M) bool`: 키/값 쌍이 모두 같은지 확인 (`==` 비교)
- `EqualFunc(m1, m2, eq func(V1, V2) bool) bool`: 값 비교 함수를 직접 지정
- `Clone(m M) M`: 얕은 복사본 생성
- `Copy(dst M1, src M2)`: `src`의 키/값을 `dst`에 덮어쓰며 복사

### 조건부 삭제

- `DeleteFunc(m M, del func(K, V) bool)`: `del`이 `true`를 반환하는 키를 맵에서 제거

```go
m := map[string]int{"a": 1, "b": -1, "c": 2}
maps.DeleteFunc(m, func(k string, v int) bool {
    return v < 0
})
// m == map[string]int{"a": 1, "c": 2}
```

---

### 정리

- 기존 코드나 `sort.Interface`를 구현한 커스텀 타입을 다룰 때는 여전히 `sort` 패키지가 유효하지만, 새 코드에서 일반 슬라이스를 다룰 때는 `slices.Sort` / `slices.SortFunc`가 더 빠르고 타입 안전하다
- 여러 필드를 기준으로 정렬할 때는 `cmp.Compare`와 `cmp.Or`를 조합해 우선순위 체인을 구성하는 패턴이 관용적이다
- 맵은 순서가 없으므로, 정렬된 순회가 필요하면 `maps.Keys` + `slices.Sort`(또는 `slices.Sorted`) 조합을 사용한다
- Go 1.23의 `iter.Seq`/`iter.Seq2` 기반 함수(`All`, `Values`, `Collect`, `Sorted` 등)는 `slices`와 `maps` 사이를 자연스럽게 넘나들 수 있게 해준다

---

## 수학 연산과 난수 생성 (math / math/rand / math/bits)

## math 패키지

> **원문:** https://pkg.go.dev/math

### 개요

`math`는 부동소수점 수학 함수와 상수를 제공하는 표준 라이브러리 패키지다. 대부분의 함수는 `float64`를 받고 `float64`를 반환하며, 특수 케이스(NaN, ±Inf, ±0)에 대한 동작은 IEEE 754 표준을 따른다.

### 상수

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

### 반올림 계열

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

### 거듭제곱·지수·로그

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

### 삼각/쌍곡 함수

- 기본 삼각함수: `Sin`, `Cos`, `Tan` (라디안 단위 입력)
- 역삼각함수: `Asin`, `Acos`, `Atan`, 사분면까지 고려하는 `Atan2(y, x float64) float64`
- 쌍곡함수: `Sinh`, `Cosh`, `Tanh`와 그 역함수 `Asinh`, `Acosh`, `Atanh`
- `Sincos(x float64) (sin, cos float64)`로 sin/cos을 한 번에 계산하면 각각 호출하는 것보다 효율적이다

### 절댓값·최대/최소·나머지

```go
func Abs(x float64) float64
func Max(x, y float64) float64
func Min(x, y float64) float64
func Mod(x, y float64) float64        // 부호는 x를 따름, |결과| < |y|
func Remainder(x, y float64) float64  // IEEE 754 나머지
```

- 정수 `int`에는 `math.Abs`를 직접 쓸 수 없다(파라미터가 `float64`). 정수 최대/최소는 Go 1.21부터 추가된 내장 함수 `min`/`max`(제네릭)를 쓰는 것이 일반적이고, `math.Max`/`math.Min`은 float 특유의 NaN 처리가 필요할 때 쓴다.
- `Mod`는 나눗셈의 나머지, `Remainder`는 반올림 나눗셈 기준의 나머지로 서로 다르다.

### 특수값 판별

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

### 기타 유용한 함수

- `Hypot(p, q float64) float64` — `sqrt(p²+q²)`를 오버플로 없이 계산 (직각삼각형의 빗변)
- `Copysign(f, sign float64) float64` — f의 크기에 sign의 부호를 적용
- `Dim(x, y float64) float64` — `max(x-y, 0)`
- `Modf(f float64) (int, frac float64)` — 정수부와 소수부 분리
- `Float64bits` / `Float64frombits` — IEEE 754 비트 표현과 상호 변환(해시, 직렬화에 사용)

Bessel 함수(`J0`, `J1`, `Jn`, `Y0`...), 감마·에러 함수(`Gamma`, `Erf`, `Lgamma`)도 제공되지만 일반적인 애플리케이션 코드에서는 거의 쓰이지 않는다.

---

## math/rand — 레거시 의사난수 생성

> **원문:** https://pkg.go.dev/math/rand

### 성격

- **암호학적으로 안전하지 않다.** 보안이 필요한 곳(토큰, 키 등)에는 반드시 `crypto/rand`를 사용해야 한다.
- 시뮬레이션, 테스트 데이터 생성, 게임 로직 등 예측 가능해도 무방한 용도에 적합하다.

### 핵심 타입

```go
type Source interface {
    Int63() int64
    Seed(seed int64)
}

type Rand struct { /* ... */ }
```

- `Source`는 난수의 원천을 추상화한 인터페이스, `Rand`는 그 위에 다양한 분포(정수/실수/순열)를 얹은 래퍼다.
- `Rand`는 여러 고루틴에서 동시에 안전하게 쓸 수 없다. 반면 패키지 최상위 함수들(`rand.Intn` 등)은 내부적으로 잠금 처리된 전역 `Rand`를 사용해 동시성에 안전하다.

### 생성과 시딩

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

### 정수/실수/순열 생성

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

### Zipf 분포

```go
func NewZipf(r *Rand, s, v float64, imax uint64) *Zipf
func (z *Zipf) Uint64() uint64
```

Zipf-Mandelbrot 분포를 따르는 값을 생성한다. 순위 기반 빈도 분포(예: 인기도 시뮬레이션)를 모사할 때 쓴다.

---

## math/rand/v2 — 재설계된 난수 패키지

> **원문:** https://pkg.go.dev/math/rand/v2

### 왜 v2인가

- 기존 `math/rand`의 알고리즘보다 통계적으로 더 우수한 PCG, ChaCha8 생성기를 채택
- `Intn(n)`처럼 이름에 접미사를 붙이던 관례 대신 `IntN`, `Int64N`처럼 대문자 `N`으로 통일하고 제네릭 `N[Int](n Int) Int`를 추가
- 모듈 버전(v2)이므로 하위 호환 부담 없이 API를 정리할 수 있었다

### 소스: PCG와 ChaCha8

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

### 제네릭 N과 경계 있는 정수 생성

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

### 실수·순열 함수

```go
func Float64() float64        // [0.0, 1.0)
func Perm(n int) []int
func Shuffle(n int, swap func(i, j int))
```

시그니처와 사용법은 v1과 동일하다. 최상위 함수는 전역 소스를 사용하며 동시성에 안전하고, `*Rand` 값의 메서드는 v1과 마찬가지로 동시 사용 시 별도 동기화가 필요하다.

### 마이그레이션 감

기존 코드의 `rand.Intn(n)` → `rand.IntN(n)`, `rand.Int63n(n)` → `rand.Int64N(n)`처럼 이름만 바뀐 경우가 많아 치환이 비교적 단순하다. `import "math/rand/v2"`로 바꾸고 컴파일 에러를 따라가며 이름을 맞추는 방식으로 마이그레이션하면 된다.

---

## math/bits — 저수준 비트 연산

> **원문:** https://pkg.go.dev/math/bits

### 개요

`math/bits`는 부호 없는 정수에 대한 비트 카운팅·회전·반전, 그리고 캐리를 포함한 저수준 산술 연산을 제공한다. 대부분의 함수는 컴파일러가 대상 아키텍처의 전용 명령어(예: `POPCNT`, `BSR`)로 치환해 최적화하므로, 직접 루프를 짜는 것보다 빠르다.

각 함수군은 플랫폼 기본 `uint`용과 `8`/`16`/`32`/`64` 비트 폭이 고정된 변형(`XxxN`)을 함께 제공한다.

```go
const UintSize = 32 << (^uint(0) >> 63)  // 플랫폼의 uint 비트 수 (32 또는 64)
```

### 0의 개수·1의 개수·비트 길이

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

### 회전과 반전

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

### 캐리를 포함한 산술

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

### 요약

- `math`: 부동소수점 수치 계산의 기본 도구 상자. 반올림, 거듭제곱/로그, 삼각함수, 특수값 판별이 핵심.
- `math/rand`: 레거시 의사난수. 자동 시딩(Go 1.20+)과 `rand.New(rand.NewSource(seed))` 조합으로 충분하며, 보안 용도에는 쓰지 않는다.
- `math/rand/v2`: 더 나은 알고리즘(PCG/ChaCha8)과 제네릭 `N` 기반의 정리된 API. 새 코드에서는 v2를 우선 검토한다.
- `math/bits`: popcount, 비트 길이, 회전/반전, 캐리 산술 등 컴파일러 최적화 혜택을 받는 저수준 연산 모음. 비트 조작이 잦은 알고리즘, 빅넘버, 해시 구현에 사용한다.

---

## 컨테이너 자료구조 (container/list, container/heap, container/ring)

Go 표준 라이브러리는 범용 컨테이너 자료구조로 `container` 하위에 세 패키지를 제공합니다: 이중 연결 리스트(`list`), 힙 인터페이스(`heap`), 원형 리스트(`ring`). 세 패키지 모두 제네릭이 아니라 `any` 타입과 인터페이스를 이용해 동작하며, 실무에서는 슬라이스로 충분한 경우가 많아 사용 빈도는 낮지만 우선순위 큐(heap)나 LRU 캐시·이벤트 루프(list) 구현에 종종 쓰입니다.

---

## container/list — 이중 연결 리스트

> **원문:** https://pkg.go.dev/container/list

### 개요

- `List`는 이중 연결 리스트(doubly linked list)이며, 각 노드는 `Element` 타입입니다.
- `List`의 제로 값은 곧바로 사용 가능한 빈 리스트입니다. 다만 `list.New()`로 생성하는 것이 관용적입니다.
- 슬라이스와 달리 중간 삽입/삭제가 O(1)이라는 점이 핵심 장점이고, 대신 인덱스 접근이 없고 순회는 포인터를 따라가야 합니다.

### Element

```go
type Element struct {
    Value any
    // 그 외 비공개 필드
}

func (e *Element) Next() *Element // 다음 요소, 없으면 nil
func (e *Element) Prev() *Element // 이전 요소, 없으면 nil
```

`Value`에 원하는 값을 자유롭게 담아 쓰고, `Next`/`Prev`로 앞뒤 요소를 따라갑니다.

### List 생성과 기본 정보

```go
func New() *List
func (l *List) Init() *List // 리스트를 초기화(또는 비움)하고 자신을 반환
func (l *List) Len() int    // 요소 개수, O(1)
func (l *List) Front() *Element // 첫 요소, 없으면 nil
func (l *List) Back() *Element       // 마지막 요소, 없으면 nil
```

### 삽입 계열 함수

| 함수 | 동작 |
|---|---|
| `PushFront(v any) *Element` | 맨 앞에 삽입 |
| `PushBack(v any) *Element` | 맨 뒤에 삽입 |
| `InsertBefore(v any, mark *Element) *Element` | mark 앞에 삽입 |
| `InsertAfter(v any, mark *Element) *Element` | mark 뒤에 삽입 |
| `PushFrontList(other *List)` | 다른 리스트를 통째로 앞에 복사 |
| `PushBackList(other *List)` | 다른 리스트를 통째로 뒤에 복사 |

모두 새로 생긴 `*Element`(또는 void)를 반환하며, `mark`는 반드시 같은 리스트에 속한 요소여야 합니다.

### 삭제·이동 계열 함수

```go
func (l *List) Remove(e *Element) any // 제거하고 저장된 값을 반환

func (l *List) MoveToFront(e *Element)
func (l *List) MoveToBack(e *Element)
func (l *List) MoveBefore(e, mark *Element)
func (l *List) MoveAfter(e, mark *Element)
```

`Move*` 계열은 요소를 새로 만들지 않고 위치만 재배치하므로, LRU 캐시에서 "최근 사용한 항목을 맨 앞으로 이동"하는 용도로 적합합니다.

### 순회 관용구

```go
l := list.New()
l.PushBack(4)
e1 := l.PushFront(1)
l.InsertAfter(2, e1)

for e := l.Front(); e != nil; e = e.Next() {
    fmt.Println(e.Value)
}
```

`for e := l.Front(); e != nil; e = e.Next()` 형태가 표준 순회 패턴이며, 역순 순회는 `l.Back()`과 `e.Prev()`를 사용합니다.

---

## container/heap — 힙 인터페이스

> **원문:** https://pkg.go.dev/container/heap

### 개요

- `heap` 패키지는 자료구조를 직접 제공하지 않고, 임의의 타입이 `heap.Interface`를 구현하면 그 타입을 **최소 힙**(min-heap)으로 다룰 수 있게 해주는 알고리즘 모음입니다.
- 루트(인덱스 0)가 항상 최솟값이라는 힙 불변식을 유지하며, 내부적으로 `sort.Interface`(`Len`, `Less`, `Swap`)에 `Push`, `Pop`을 더한 인터페이스를 요구합니다.
- 최댓값 우선 큐가 필요하면 `Less`의 부등호 방향만 뒤집으면 됩니다.

### Interface

```go
type Interface interface {
    sort.Interface
    Push(x any) // 길이가 Len()인 위치에 x를 추가
    Pop() any   // 길이가 Len()-1인 위치의 요소를 제거하고 반환
}
```

구현 시 주의할 점:
- `Less(i, j)`가 "i가 j보다 우선순위가 높은가"를 정의하며, 이 순서가 힙의 루트를 결정합니다.
- `Push`/`Pop`은 슬라이스의 맨 끝을 다루도록 구현하는 것이 관례입니다(내부적으로 heap 알고리즘이 앞부분 재배치를 담당).
- 리시버는 슬라이스 자체를 교체해야 하므로 포인터 리시버(`*IntHeap`)로 구현해야 합니다.

### 힙 조작 함수

| 함수 | 시그니처 | 설명 | 복잡도 |
|---|---|---|---|
| `Init` | `func Init(h Interface)` | 임의 순서의 데이터를 힙 불변식에 맞게 재배치 | O(n) |
| `Push` | `func Push(h Interface, x any)` | 힙에 x를 추가 후 재정렬 | O(log n) |
| `Pop` | `func Pop(h Interface) any` | 최솟값 제거 후 반환 (`Remove(h, 0)`과 동일) | O(log n) |
| `Remove` | `func Remove(h Interface, i int) any` | 인덱스 i의 요소를 제거 후 반환 | O(log n) |
| `Fix` | `func Fix(h Interface, i int)` | 인덱스 i의 값이 바뀐 뒤 힙 불변식을 재확립 | O(log n) |

`Fix`는 우선순위 큐에서 항목의 우선순위를 바꿀 때, `Remove` 후 `Push`하는 것보다 저렴하게 재정렬하는 용도로 씁니다.

### 최소 힙 예시

```go
type IntHeap []int

func (h IntHeap) Len() int            { return len(h) }
func (h IntHeap) Less(i, j int) bool  { return h[i] < h[j] }
func (h IntHeap) Swap(i, j int)       { h[i], h[j] = h[j], h[i] }

func (h *IntHeap) Push(x any) {
    *h = append(*h, x.(int))
}

func (h *IntHeap) Pop() any {
    old := *h
    n := len(old)
    v := old[n-1]
    *h = old[:n-1]
    return v
}

func main() {
    h := &IntHeap{5, 2, 8}
    heap.Init(h)
    heap.Push(h, 1)
    for h.Len() > 0 {
        fmt.Println(heap.Pop(h)) // 1 2 5 8
    }
}
```

### 우선순위 큐 패턴

우선순위가 바뀌는 항목을 다룰 때는 각 항목이 힙 내부 인덱스를 스스로 들고 있게 하고, `Swap`에서 그 인덱스를 갱신합니다.

```go
type Item struct {
    value    string
    priority int
    index    int // heap.Swap에서 유지 관리
}

type PQ []*Item

func (pq PQ) Less(i, j int) bool { return pq[i].priority > pq[j].priority } // 값이 클수록 우선
func (pq PQ) Swap(i, j int) {
    pq[i], pq[j] = pq[j], pq[i]
    pq[i].index, pq[j].index = i, j
}

func (pq *PQ) update(item *Item, priority int) {
    item.priority = priority
    heap.Fix(pq, item.index) // Remove+Push보다 저렴
}
```

---

## container/ring — 원형 리스트

> **원문:** https://pkg.go.dev/container/ring

### 개요

- `Ring`은 순환 연결 리스트로, 리스트 자체에 시작이나 끝이 없어 임의의 한 요소에 대한 포인터가 곧 전체 링을 가리키는 참조 역할을 합니다.
- `Ring`의 제로 값은 `Value`가 nil인 요소 1개짜리 링입니다. 빈 링은 nil 포인터로 표현합니다.
- 라운드로빈 스케줄링, 순환 버퍼처럼 "끝에 도달하면 처음으로 돌아가는" 순회에 적합합니다.

### Ring 타입과 생성

```go
type Ring struct {
    Value any
    // 비공개 필드
}

func New(n int) *Ring // n개 요소로 이루어진 링 생성
```

### 이동 계열 메서드

```go
func (r *Ring) Next() *Ring     // 다음 요소
func (r *Ring) Prev() *Ring     // 이전 요소
func (r *Ring) Move(n int) *Ring // n%Len()만큼 이동 (n<0이면 역방향)
func (r *Ring) Len() int         // 요소 개수, 링을 한 바퀴 돌아 계산 (O(n))
```

`r`은 빈 링(nil)이 아니어야 하며, `Len()`은 캐시되지 않으므로 반복 호출을 피하는 것이 좋습니다.

### 링 조작: Link / Unlink

```go
func (r *Ring) Link(s *Ring) *Ring
func (r *Ring) Unlink(n int) *Ring
```

- `Link(s)`: `r.Next()`가 `s`가 되도록 연결하고, 연결 전 `r.Next()`였던 값을 반환합니다.
  - `r`과 `s`가 같은 링에 속하면 그 사이 요소들을 링에서 잘라내는 효과를 냅니다.
  - 서로 다른 링이면 `s`의 모든 요소를 `r` 뒤에 끼워 넣어 하나의 링으로 합칩니다.
- `Unlink(n)`: `r.Next()`부터 `n % Len()`개 요소를 링에서 떼어내고, 떼어낸 부분 링을 반환합니다. `n % Len() == 0`이면 아무 변화가 없습니다.

### 순회: Do

```go
func (r *Ring) Do(f func(any))
```

링의 모든 요소에 대해 정방향으로 `f(value)`를 호출합니다. `f` 안에서 링 구조를 변경하면 동작이 정의되지 않습니다.

### 사용 예시

```go
r := ring.New(5)
n := r.Len()
for i := 0; i < n; i++ {
    r.Value = i
    r = r.Next()
}

r.Do(func(v any) {
    fmt.Println(v) // 0 1 2 3 4
})
```

### 요약: 세 패키지 언제 쓰나

| 패키지 | 자료구조 | 주요 용도 |
|---|---|---|
| `container/list` | 이중 연결 리스트 | 중간 삽입/삭제가 잦은 시퀀스, LRU 캐시 |
| `container/heap` | 힙(우선순위 큐) 알고리즘 | 최솟값/최댓값 우선 처리, 이벤트 스케줄링 |
| `container/ring` | 원형 리스트 | 라운드로빈, 순환 버퍼 |
