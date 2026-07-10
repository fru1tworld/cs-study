# 컨테이너 자료구조 (container/list, container/heap, container/ring)

Go 표준 라이브러리는 범용 컨테이너 자료구조로 `container` 하위에 세 패키지를 제공합니다: 이중 연결 리스트(`list`), 힙 인터페이스(`heap`), 원형 리스트(`ring`). 세 패키지 모두 제네릭이 아니라 `any` 타입과 인터페이스를 이용해 동작하며, 실무에서는 슬라이스로 충분한 경우가 많아 사용 빈도는 낮지만 우선순위 큐(heap)나 LRU 캐시·이벤트 루프(list) 구현에 종종 쓰입니다.

---

# container/list — 이중 연결 리스트

> **원문:** https://pkg.go.dev/container/list

## 개요

- `List`는 이중 연결 리스트(doubly linked list)이며, 각 노드는 `Element` 타입입니다.
- `List`의 제로 값은 곧바로 사용 가능한 빈 리스트입니다. 다만 `list.New()`로 생성하는 것이 관용적입니다.
- 슬라이스와 달리 중간 삽입/삭제가 O(1)이라는 점이 핵심 장점이고, 대신 인덱스 접근이 없고 순회는 포인터를 따라가야 합니다.

## Element

```go
type Element struct {
    Value any
    // 그 외 비공개 필드
}

func (e *Element) Next() *Element // 다음 요소, 없으면 nil
func (e *Element) Prev() *Element // 이전 요소, 없으면 nil
```

`Value`에 원하는 값을 자유롭게 담아 쓰고, `Next`/`Prev`로 앞뒤 요소를 따라갑니다.

## List 생성과 기본 정보

```go
func New() *List
func (l *List) Init() *List // 리스트를 초기화(또는 비움)하고 자신을 반환
func (l *List) Len() int    // 요소 개수, O(1)
func (l *List) Front() *Element // 첫 요소, 없으면 nil
func (l *List) Back() *Element       // 마지막 요소, 없으면 nil
```

## 삽입 계열 함수

| 함수 | 동작 |
|---|---|
| `PushFront(v any) *Element` | 맨 앞에 삽입 |
| `PushBack(v any) *Element` | 맨 뒤에 삽입 |
| `InsertBefore(v any, mark *Element) *Element` | mark 앞에 삽입 |
| `InsertAfter(v any, mark *Element) *Element` | mark 뒤에 삽입 |
| `PushFrontList(other *List)` | 다른 리스트를 통째로 앞에 복사 |
| `PushBackList(other *List)` | 다른 리스트를 통째로 뒤에 복사 |

모두 새로 생긴 `*Element`(또는 void)를 반환하며, `mark`는 반드시 같은 리스트에 속한 요소여야 합니다.

## 삭제·이동 계열 함수

```go
func (l *List) Remove(e *Element) any // 제거하고 저장된 값을 반환

func (l *List) MoveToFront(e *Element)
func (l *List) MoveToBack(e *Element)
func (l *List) MoveBefore(e, mark *Element)
func (l *List) MoveAfter(e, mark *Element)
```

`Move*` 계열은 요소를 새로 만들지 않고 위치만 재배치하므로, LRU 캐시에서 "최근 사용한 항목을 맨 앞으로 이동"하는 용도로 적합합니다.

## 순회 관용구

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

# container/heap — 힙 인터페이스

> **원문:** https://pkg.go.dev/container/heap

## 개요

- `heap` 패키지는 자료구조를 직접 제공하지 않고, 임의의 타입이 `heap.Interface`를 구현하면 그 타입을 **최소 힙**(min-heap)으로 다룰 수 있게 해주는 알고리즘 모음입니다.
- 루트(인덱스 0)가 항상 최솟값이라는 힙 불변식을 유지하며, 내부적으로 `sort.Interface`(`Len`, `Less`, `Swap`)에 `Push`, `Pop`을 더한 인터페이스를 요구합니다.
- 최댓값 우선 큐가 필요하면 `Less`의 부등호 방향만 뒤집으면 됩니다.

## Interface

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

## 힙 조작 함수

| 함수 | 시그니처 | 설명 | 복잡도 |
|---|---|---|---|
| `Init` | `func Init(h Interface)` | 임의 순서의 데이터를 힙 불변식에 맞게 재배치 | O(n) |
| `Push` | `func Push(h Interface, x any)` | 힙에 x를 추가 후 재정렬 | O(log n) |
| `Pop` | `func Pop(h Interface) any` | 최솟값 제거 후 반환 (`Remove(h, 0)`과 동일) | O(log n) |
| `Remove` | `func Remove(h Interface, i int) any` | 인덱스 i의 요소를 제거 후 반환 | O(log n) |
| `Fix` | `func Fix(h Interface, i int)` | 인덱스 i의 값이 바뀐 뒤 힙 불변식을 재확립 | O(log n) |

`Fix`는 우선순위 큐에서 항목의 우선순위를 바꿀 때, `Remove` 후 `Push`하는 것보다 저렴하게 재정렬하는 용도로 씁니다.

## 최소 힙 예시

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

## 우선순위 큐 패턴

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

# container/ring — 원형 리스트

> **원문:** https://pkg.go.dev/container/ring

## 개요

- `Ring`은 순환 연결 리스트로, 리스트 자체에 시작이나 끝이 없어 임의의 한 요소에 대한 포인터가 곧 전체 링을 가리키는 참조 역할을 합니다.
- `Ring`의 제로 값은 `Value`가 nil인 요소 1개짜리 링입니다. 빈 링은 nil 포인터로 표현합니다.
- 라운드로빈 스케줄링, 순환 버퍼처럼 "끝에 도달하면 처음으로 돌아가는" 순회에 적합합니다.

## Ring 타입과 생성

```go
type Ring struct {
    Value any
    // 비공개 필드
}

func New(n int) *Ring // n개 요소로 이루어진 링 생성
```

## 이동 계열 메서드

```go
func (r *Ring) Next() *Ring     // 다음 요소
func (r *Ring) Prev() *Ring     // 이전 요소
func (r *Ring) Move(n int) *Ring // n%Len()만큼 이동 (n<0이면 역방향)
func (r *Ring) Len() int         // 요소 개수, 링을 한 바퀴 돌아 계산 (O(n))
```

`r`은 빈 링(nil)이 아니어야 하며, `Len()`은 캐시되지 않으므로 반복 호출을 피하는 것이 좋습니다.

## 링 조작: Link / Unlink

```go
func (r *Ring) Link(s *Ring) *Ring
func (r *Ring) Unlink(n int) *Ring
```

- `Link(s)`: `r.Next()`가 `s`가 되도록 연결하고, 연결 전 `r.Next()`였던 값을 반환합니다.
  - `r`과 `s`가 같은 링에 속하면 그 사이 요소들을 링에서 잘라내는 효과를 냅니다.
  - 서로 다른 링이면 `s`의 모든 요소를 `r` 뒤에 끼워 넣어 하나의 링으로 합칩니다.
- `Unlink(n)`: `r.Next()`부터 `n % Len()`개 요소를 링에서 떼어내고, 떼어낸 부분 링을 반환합니다. `n % Len() == 0`이면 아무 변화가 없습니다.

## 순회: Do

```go
func (r *Ring) Do(f func(any))
```

링의 모든 요소에 대해 정방향으로 `f(value)`를 호출합니다. `f` 안에서 링 구조를 변경하면 동작이 정의되지 않습니다.

## 사용 예시

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

## 요약: 세 패키지 언제 쓰나

| 패키지 | 자료구조 | 주요 용도 |
|---|---|---|
| `container/list` | 이중 연결 리스트 | 중간 삽입/삭제가 잦은 시퀀스, LRU 캐시 |
| `container/heap` | 힙(우선순위 큐) 알고리즘 | 최솟값/최댓값 우선 처리, 이벤트 스케줄링 |
| `container/ring` | 원형 리스트 | 라운드로빈, 순환 버퍼 |
