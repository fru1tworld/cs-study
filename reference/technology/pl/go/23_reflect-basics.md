# 리플렉션 기초 (reflect)

> **원문:** https://pkg.go.dev/reflect

## 개요

`reflect` 패키지는 프로그램이 실행 중에 임의의 타입의 값을 검사하고 조작할 수 있게 해주는 런타임 리플렉션 기능을 제공합니다. 인터페이스에 담긴 값을 `TypeOf`, `ValueOf`로 꺼내서 그 타입 정보와 실제 데이터에 접근하는 것이 기본 골격입니다.

- **`Type`**: 값의 정적 타입 정보(이름, 종류, 필드, 메서드 등)를 나타내는 인터페이스
- **`Value`**: 값 자체를 감싸서 읽기/쓰기/호출 등을 가능하게 하는 구조체
- 대부분의 코드는 `encoding/json`, ORM, 직렬화 라이브러리처럼 제네릭하게 임의 구조체를 다뤄야 할 때만 리플렉션을 사용합니다. Rob Pike의 격언처럼 "리플렉션은 절대 명확하지 않다(reflection is never clear)" — 필요할 때만 최소한으로 사용하는 것이 좋습니다.

---

## Type과 Kind: 타입 정보 얻기

### TypeOf

```go
func TypeOf(i any) Type
```

- 인터페이스에 담긴 값의 **동적 타입**을 나타내는 `Type`을 반환합니다.
- `i`가 nil 인터페이스면 nil을 반환합니다.

```go
t := reflect.TypeOf(3.14)
fmt.Println(t.Name(), t.Kind()) // float64 float64
```

### Kind vs Type

- `Type`은 "정확히 어떤 타입인가"(예: `MyInt`, `os.File`)를 나타내고, `Kind()`는 "그 타입의 근본 형태가 무엇인가"(예: `Int`, `Struct`, `Pointer`)를 나타냅니다.
- `type MyInt int`로 정의한 타입은 `Type.Name()`이 `"MyInt"`이지만 `Kind()`는 여전히 `reflect.Int`입니다.
- 주요 `Kind` 값: `Bool`, `Int*`, `Uint*`, `Float*`, `Complex*`, `String`, `Array`, `Slice`, `Map`, `Struct`, `Pointer`, `Interface`, `Func`, `Chan`, `Invalid`(잘못되거나 초기화되지 않은 값). 옛 이름 `Ptr`는 `Pointer`의 별칭으로 남아있을 뿐이므로 새 코드에서는 `Pointer`를 씁니다.

```go
switch reflect.ValueOf(v).Kind() {
case reflect.String:
    fmt.Println("문자열")
case reflect.Int, reflect.Int64:
    fmt.Println("정수")
}
```

### Type의 주요 메서드

| 메서드 | 설명 |
|---|---|
| `Name() string` | 패키지 내 타입 이름 (익명 타입은 빈 문자열) |
| `Kind() Kind` | 근본 종류 |
| `NumField()` / `Field(i)` | 구조체 필드 개수 / i번째 필드(`StructField`) |
| `FieldByName(name)` | 이름으로 필드 조회 |
| `NumMethod()` / `Method(i)` | 메서드 개수 / i번째 메서드 |
| `Elem() Type` | 포인터·슬라이스·배열·맵·채널의 요소 타입 (Kind가 Array/Chan/Map/Pointer/Slice가 아니면 panic) |
| `Implements(u Type) bool` | 해당 타입이 인터페이스 `u`를 구현하는지 |
| `Comparable() bool` | `==` 비교가 가능한 타입인지 |
| `Size() uintptr` | 값 하나가 차지하는 바이트 수 |

```go
var w io.Writer
writerType := reflect.TypeOf(&w).Elem() // 인터페이스 타입 얻는 관용 패턴
fileType := reflect.TypeOf((*os.File)(nil)).Elem()
fmt.Println(fileType.Implements(writerType)) // true
```

---

## Value: 값 자체 다루기

### ValueOf / Zero

```go
func ValueOf(i any) Value
func Zero(typ Type) Value
```

- `ValueOf`는 인터페이스 속 실제 값을 감싼 `Value`를 반환합니다. `ValueOf(nil)`은 제로 값 `Value`(`IsValid() == false`)를 돌려줍니다.
- `Zero(t)`는 타입 `t`의 제로 값을 만들어줍니다. 예: `Zero(TypeOf(0))`은 `Kind() == Int`이고 값이 0인 `Value`.

### 값을 다시 꺼내기: Interface

```go
func (v Value) Interface() any
```

- `Value`에 담긴 값을 다시 `interface{}`로 꺼냅니다. 이후 타입 단언(`.(T)`)으로 원래 타입에 접근합니다.

```go
v := reflect.ValueOf(42)
n := v.Interface().(int)
```

### 검사용 메서드

- `Kind()`, `Type()`: 종류와 타입 조회
- `IsValid() bool`: 유효한 값을 담고 있는지 (제로 `Value`는 false)
- `IsNil() bool`: 채널·함수·인터페이스·맵·포인터·슬라이스에서 nil 여부 (그 외 Kind에 호출하면 panic)
- `IsZero() bool`: 해당 타입의 제로 값과 같은지
- `CanSet()` / `CanAddr()` / `CanInterface()`: 수정 가능 여부, 주소를 딸 수 있는지, `Interface()` 호출이 안전한지

### 기본 타입 값 꺼내기 / 설정하기

읽기용과 쓰기용 메서드가 Kind별로 쌍을 이룹니다.

| 읽기 | 쓰기 | 대상 Kind |
|---|---|---|
| `Bool()` | `SetBool(x bool)` | Bool |
| `Int()` (int64) | `SetInt(x int64)` | Int* |
| `Uint()` (uint64) | `SetUint(x uint64)` | Uint* |
| `Float()` (float64) | `SetFloat(x float64)` | Float* |
| `String()` | `SetString(x string)` | String |

`Set(x Value)`는 임의 타입 간에 값 자체를 통째로 대입할 때 씁니다. `Set` 계열은 반드시 `CanSet() == true`일 때만 호출해야 하며, 그렇지 않으면 panic이 발생합니다.

```go
i := 10
v := reflect.ValueOf(&i).Elem() // 포인터를 역참조해야 CanSet() == true
v.SetInt(20)
fmt.Println(i) // 20
```

- `ValueOf(i)`처럼 값을 그대로 넘기면 원본의 복사본이라 수정할 수 없습니다. **포인터를 넘기고 `Elem()`으로 역참조**해야 원본을 변경할 수 있다는 점이 리플렉션 코드에서 가장 흔히 겪는 함정입니다.

---

## 포인터·인터페이스 다루기: Elem, Addr

```go
func (v Value) Elem() Value
func (v Value) Addr() Value
```

- `Elem()`: 포인터가 가리키는 값, 혹은 인터페이스에 담긴 실제 값을 반환합니다.
- `Addr()`: `v`의 주소를 가리키는 `Value`(포인터)를 반환합니다. `CanAddr()`가 true여야 합니다.

```go
p := &struct{ X int }{X: 1}
ev := reflect.ValueOf(p).Elem() // X int 구조체 값 자체
ev.Field(0).SetInt(42)
fmt.Println(p.X) // 42
```

---

## 구조체 다루기: Field, StructField, StructTag

### 필드 접근

`Type`과 `Value` 양쪽 모두 필드 접근용 메서드를 제공합니다.

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

### StructField와 태그

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

- `StructTag`는 `` `key:"value" key2:"value2"` `` 형식의 원본 태그 문자열을 감싼 타입입니다.
- `Tag.Get(key) string`: 값을 꺼내며, 없으면 빈 문자열
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

`encoding/json` 같은 라이브러리가 구조체 필드마다 붙은 `json:"..."` 태그를 읽어 직렬화 규칙을 정하는 것이 바로 이 메커니즘입니다.

---

## 메서드 호출: Method, Call

```go
func (v Value) NumMethod() int
func (v Value) Method(i int) Value
func (v Value) MethodByName(name string) Value
func (v Value) Call(in []Value) []Value
```

- `Method(i)` / `MethodByName(name)`은 리시버가 이미 바인딩된 함수 형태의 `Value`를 반환합니다.
- `Call(in)`은 인자 목록을 `[]Value`로 넘겨 함수/메서드를 실제로 호출하고 결과를 `[]Value`로 돌려받습니다. 인자 개수·타입이 시그니처와 맞지 않으면 panic이 발생하므로 사전에 `Type()`을 확인하는 것이 안전합니다.

```go
type Greeter struct{ Name string }
func (g Greeter) Hello(suffix string) string { return "Hello, " + g.Name + suffix }

g := Greeter{"World"}
m := reflect.ValueOf(g).MethodByName("Hello")
out := m.Call([]reflect.Value{reflect.ValueOf("!")})
fmt.Println(out[0].String()) // Hello, World!
```

---

## 컬렉션 다루기: Slice, Map, Array

| 메서드 | 대상 | 설명 |
|---|---|---|
| `Len()` | Array/Slice/String/Map/Chan | 길이 |
| `Cap()` | Array/Slice/Chan | 용량 |
| `Index(i)` | Array/Slice/String | i번째 요소 |
| `Slice(i, j)` | Array/Slice/String | 부분 슬라이스 |
| `MapIndex(key)` | Map | 키에 대응하는 값(없으면 제로 `Value`) |
| `MapKeys()` | Map | 모든 키의 `[]Value` |
| `MapRange() *MapIter` | Map | `for iter.Next() { iter.Key(); iter.Value() }` 형태의 순회자 |
| `SetMapIndex(k, v)` | Map | 키-값 설정 (v가 제로 `Value`면 키 삭제) |

```go
m := map[string]int{"a": 1, "b": 2}
v := reflect.ValueOf(m)
iter := v.MapRange()
for iter.Next() {
    fmt.Println(iter.Key(), iter.Value())
}
```

---

## 값 생성하기: New, Make 계열

리플렉션만으로 새로운 값을 만들어야 할 때(제네릭 팩토리, 디코더 구현 등) 쓰는 함수들입니다.

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

`MakeFunc`은 함수 시그니처(`Type`)만 있고 구현은 클로저로 주는 경우에 사용합니다. 어댑터나 미들웨어를 리플렉션으로 감쌀 때 쓰입니다.

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

관련해서 `ArrayOf`, `SliceOf`, `MapOf`, `ChanOf`, `PointerTo`, `FuncOf`, `StructOf` 같은 `*Of` 함수들은 기존 `Type`을 조합해 새로운 `Type`을 동적으로 정의할 때 사용합니다.

---

## 비교와 기타 유틸리티

```go
func DeepEqual(x, y any) bool
func Copy(dst, src Value) int
func Swapper(slice any) func(i, j int)
```

- `DeepEqual`은 `==`로 비교할 수 없는 슬라이스·맵·포인터가 가리키는 내용까지 재귀적으로 비교합니다. 테스트 코드의 `assertEqual` 류 헬퍼에서 자주 쓰입니다. 함수 값은 서로 nil인 경우만 같다고 판정합니다.
- `Copy(dst, src)`는 슬라이스나 배열 사이에서 요소를 복사하고, 실제로 복사된 개수를 반환합니다(`copy` 내장 함수의 리플렉션 버전).
- `Swapper(slice)`는 `sort.Interface`를 구현할 때 쓸 수 있는 `func(i, j int)` 스왑 함수를 반환합니다.

---

## 실전 패턴: 임의 구조체를 map으로 변환

각 개념을 조합하면 아래처럼 리플렉션으로 임의 구조체를 순회하는 코드를 짤 수 있습니다.

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

## 주의할 점

- 리플렉션 코드는 컴파일 타임 타입 검사를 우회하므로 잘못된 `Kind`에 메서드를 호출하면 **런타임 panic**으로 이어집니다. 호출 전에 `Kind()`를 확인하는 방어 코드가 필수입니다.
- 값을 수정하려면 반드시 포인터를 거쳐 `Elem()`으로 얻은 addressable한 `Value`여야 합니다.
- 리플렉션은 일반 코드보다 느립니다. 성능이 중요한 경로에서는 코드 생성이나 제네릭 등 대안을 먼저 고려하는 것이 좋습니다.
