# 정렬과 제네릭 컬렉션 헬퍼 (sort/slices/maps/cmp)

Go 1.21 전후로 표준 라이브러리에 제네릭 기반의 `slices`, `maps`, `cmp` 패키지가 추가되면서, 예전 `sort` 패키지만으로 처리하던 정렬·검색·비교 작업을 훨씬 짧고 타입 안전하게 작성할 수 있게 되었다. 이 문서는 네 패키지에서 실무에서 자주 쓰는 함수 위주로 정리한다.

---

# sort — 인터페이스 기반 정렬

> **원문:** https://pkg.go.dev/sort

## 개요

`sort` 패키지는 Go 1.21 이전부터 존재하던 정렬 라이브러리다. 슬라이스가 `sort.Interface`(`Len`, `Less`, `Swap`)를 구현하기만 하면 임의의 타입을 정렬할 수 있다. `Slice`/`SliceStable`이나 `Ints`/`Strings`/`Float64s` 같은 타입별 편의 함수는 Go 1.22부터 내부적으로 `slices` 패키지에 위임하도록 바뀌어 성능이 개선됐지만, `sort.Sort`/`Stable`처럼 `Interface`를 직접 받는 함수는 별도 구현을 유지한다. 어느 쪽이든 API 자체는 그대로다.

## Interface와 Sort/Stable

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

## Slice / SliceStable

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

## 기본 타입용 편의 함수

`Ints`, `Strings`, `Float64s`와 짝을 이루는 `*AreSorted` 함수들이 있다.

| 함수 | 설명 |
|---|---|
| `Ints(x []int)` / `IntsAreSorted(x []int) bool` | int 슬라이스 정렬 / 정렬 여부 확인 |
| `Strings(x []string)` / `StringsAreSorted(...)` | string 슬라이스 정렬 |
| `Float64s(x []float64)` / `Float64sAreSorted(...)` | float64 슬라이스 정렬 (NaN은 맨 앞) |
| `IsSorted(data Interface) bool` | `Interface` 기반 정렬 여부 확인 |

Go 1.22부터 이 함수들은 내부적으로 `slices` 패키지를 호출하도록 바뀌었으므로, 새 코드에서는 `slices.Sort`를 직접 쓰는 편을 권장한다.

## 이진 탐색

정렬된 슬라이스에서 값을 찾을 때 사용한다.

- `Search(n int, f func(int) bool) int`: `f(i)`가 처음으로 `true`가 되는 최소 인덱스를 찾는 범용 이진 탐색
- `SearchInts`, `SearchStrings`, `SearchFloat64s(a, x) int`: 정렬된 슬라이스에서 값을 이진 탐색
- `Find(n int, cmp func(int) int) (i int, found bool)` (Go 1.19+): `cmp`가 0을 반환하는 위치를 찾고 존재 여부도 함께 반환

```go
a := []int{1, 3, 5, 7, 9}
i := sort.SearchInts(a, 5) // 2
```

## Reverse와 편의 슬라이스 타입

- `Reverse(data Interface) Interface`: 정렬 순서를 뒤집은 뷰를 반환 (내부 `Less`를 반전)
- `IntSlice`, `StringSlice`, `Float64Slice`: 각각 `[]int`, `[]string`, `[]float64`에 `Len/Less/Swap`과 `Sort()`, `Search()` 메서드를 얹은 타입. `sort.Reverse`와 조합해 내림차순 정렬을 만들 때 흔히 쓰인다

```go
s := []int{5, 2, 6, 3, 1, 4}
sort.Sort(sort.Reverse(sort.IntSlice(s))) // [6 5 4 3 2 1]
```

---

# cmp — 비교의 최소 공통분모

> **원문:** https://pkg.go.dev/cmp

`slices`와 `maps`의 제네릭 함수들이 공통으로 참조하는 기반 패키지다. 비교 가능한 타입 제약과 3-way 비교 결과(`-1`, `0`, `1`)라는 관례를 여기서 정의한다.

## Ordered 제약

```go
type Ordered interface {
    ~int | ~int8 | ... | ~float32 | ~float64 | ~string
}
```

`<`, `<=`, `>=`, `>` 연산자를 지원하는 정수·부동소수점·문자열 계열 타입을 모두 포함하는 제약이다. `slices.Sort`, `slices.Max` 같은 함수의 타입 매개변수 제약으로 쓰인다.

## Compare / Less

- `Compare[T Ordered](x, y T) int`: x가 작으면 -1, 같으면 0, 크면 1
- `Less[T Ordered](x, y T) bool`: `x < y`와 동일하지만 NaN을 규칙적으로 처리

부동소수점에서는 NaN을 "가장 작은 값"으로 취급해 일관된 전순서를 만든다. 즉 `Compare(NaN, 1.0)`은 -1, `Compare(NaN, NaN)`은 0이다. 이 덕분에 NaN이 섞인 슬라이스도 정렬 시 패닉 없이 한쪽 끝에 몰린다.

```go
cmp.Compare(1, 2)            // -1
cmp.Compare(math.NaN(), 1.0) // -1 (NaN을 가장 작게 취급)
```

## Or — 제로 값 폴백 체이닝

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

# slices — 제네릭 슬라이스 유틸리티

> **원문:** https://pkg.go.dev/slices

Go 1.21에 도입된 패키지로, `sort`의 기능을 대체하면서 검색·삽입·삭제·비교까지 아우르는 슬라이스 전용 제네릭 함수 모음을 제공한다. 대부분의 함수가 타입 매개변수 `S ~[]E`를 받아 슬라이스 기반 타입(named slice type 포함)에도 그대로 적용된다.

## 정렬

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

## 검색

- `BinarySearch[E cmp.Ordered](x []E, target E) (int, bool)`: 정렬된 슬라이스에서 위치와 존재 여부 반환
- `BinarySearchFunc[E, T any](x []E, target T, cmp func(E, T) int) (int, bool)`: 커스텀 비교 함수 버전
- `Contains[E comparable](s []E, v E) bool` / `ContainsFunc[E any](s []E, f func(E) bool) bool`
- `Index[E comparable](s []E, v E) int` / `IndexFunc[E any](s []E, f func(E) bool) int`: 없으면 -1

```go
idx, found := slices.BinarySearch(nums, 3) // 2, true
has := slices.Contains(nums, 10)           // false
```

## 슬라이스 변형 (삽입/삭제/치환)

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

## 복제와 용량 관리

- `Clone[E any](s []E) []E`: 얕은 복사본 생성
- `Clip[E any](s []E) []E`: 여유 용량을 제거해 `len == cap`으로 만듦
- `Grow[E any](s []E, n int) []E`: 최소 n개를 더 담을 수 있도록 용량 확보
- `Concat[E any](slices ...[]E) []E`: 여러 슬라이스를 이어 붙인 새 슬라이스 반환
- `Repeat[E any](x []E, count int) []E`: 슬라이스를 count번 반복

## 비교

- `Equal[E comparable](s1, s2 []E) bool`: 길이와 원소가 모두 같은지 확인
- `EqualFunc[E1, E2 any](s1 []E1, s2 []E2, eq func(E1, E2) bool) bool`
- `Compare[E cmp.Ordered](s1, s2 []E) int`: 사전식 비교 (-1/0/1)
- `CompareFunc[E1, E2 any](s1 []E1, s2 []E2, cmp func(E1, E2) int) int`

## Max/Min, Reverse

- `Max[E cmp.Ordered](x []E) E` / `Min[E cmp.Ordered](x []E) E`: 빈 슬라이스면 패닉
- `MaxFunc`, `MinFunc`: 커스텀 비교 함수 버전
- `Reverse[E any](s []E)`: 슬라이스를 제자리에서 뒤집음

```go
slices.Reverse(nums)   // in-place
max := slices.Max(nums)
```

## 반복자(iterator) 연동 함수 (Go 1.23+)

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

# maps — 제네릭 맵 유틸리티

> **원문:** https://pkg.go.dev/maps

`slices`의 맵 버전으로, Go 1.21에 함께 도입되었다. 맵은 순서가 없으므로 `Sort` 계열 함수는 없고, 대신 키/값 열거, 복사, 비교, 조건부 삭제에 집중한다.

## 반복자 함수 (Go 1.23+)

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

## 비교와 복사

- `Equal(m1, m2 M) bool`: 키/값 쌍이 모두 같은지 확인 (`==` 비교)
- `EqualFunc(m1, m2, eq func(V1, V2) bool) bool`: 값 비교 함수를 직접 지정
- `Clone(m M) M`: 얕은 복사본 생성
- `Copy(dst M1, src M2)`: `src`의 키/값을 `dst`에 덮어쓰며 복사

## 조건부 삭제

- `DeleteFunc(m M, del func(K, V) bool)`: `del`이 `true`를 반환하는 키를 맵에서 제거

```go
m := map[string]int{"a": 1, "b": -1, "c": 2}
maps.DeleteFunc(m, func(k string, v int) bool {
    return v < 0
})
// m == map[string]int{"a": 1, "c": 2}
```

---

## 정리

- 기존 코드나 `sort.Interface`를 구현한 커스텀 타입을 다룰 때는 여전히 `sort` 패키지가 유효하지만, 새 코드에서 일반 슬라이스를 다룰 때는 `slices.Sort` / `slices.SortFunc`가 더 빠르고 타입 안전하다
- 여러 필드를 기준으로 정렬할 때는 `cmp.Compare`와 `cmp.Or`를 조합해 우선순위 체인을 구성하는 패턴이 관용적이다
- 맵은 순서가 없으므로, 정렬된 순회가 필요하면 `maps.Keys` + `slices.Sort`(또는 `slices.Sorted`) 조합을 사용한다
- Go 1.23의 `iter.Seq`/`iter.Seq2` 기반 함수(`All`, `Values`, `Collect`, `Sorted` 등)는 `slices`와 `maps` 사이를 자연스럽게 넘나들 수 있게 해준다
