# 제네릭 데이터 타입

## 개요

제네릭(Generic)을 사용하면 함수 시그니처나 구조체 같은 항목에 대해 다양한 구체적인 데이터 타입에서 작동할 수 있는 정의를 만들 수 있습니다. 이를 통해 타입 안전성을 유지하면서 코드 중복을 줄일 수 있습니다.

**핵심 포인트:**
- 제네릭은 코드 중복을 줄이고 재사용성을 높임
- 다양한 타입에 대해 동일한 로직을 적용 가능
- 컴파일 타임에 타입 검사가 이루어져 타입 안전성 보장

## 함수 정의에서의 제네릭

### 기본 개념

함수 시그니처에서 매개변수 목록 앞에 꺾쇠 괄호 `<>`를 사용하여 제네릭을 배치합니다:

```rust
fn largest<T>(list: &[T]) -> &T {
    let mut largest = &list[0];

    for item in list {
        if item > largest {
            largest = item;
        }
    }

    largest
}
```

### 명명 규칙

**핵심 포인트:**
- 타입 매개변수는 **UpperCamelCase**를 사용
- `T`는 "type"의 줄임말로 관례적으로 사용됨
- 여러 제네릭 타입이 필요하면 `T`, `U`, `V` 등을 사용

### 트레이트 바운드 (Trait Bounds)

제네릭 함수는 제약 조건이 필요할 수 있습니다. 비교 연산을 위해서는 `PartialOrd` 트레이트가 필요합니다:

```rust
fn largest<T: std::cmp::PartialOrd>(list: &[T]) -> &T {
    // 구현
}
```

## 구조체 정의에서의 제네릭

### 단일 제네릭 타입

```rust
struct Point<T> {
    x: T,
    y: T,
}

fn main() {
    let integer = Point { x: 5, y: 10 };
    let float = Point { x: 1.0, y: 4.0 };
}
```

**핵심 포인트:**
- 단일 제네릭 매개변수를 사용할 때 두 필드는 같은 타입이어야 함
- `Point { x: 5, y: 4.0 }`처럼 다른 타입을 사용하면 컴파일 오류 발생

### 다중 제네릭 타입

```rust
struct Point<T, U> {
    x: T,
    y: U,
}

fn main() {
    let both_integer = Point { x: 5, y: 10 };
    let both_float = Point { x: 1.0, y: 4.0 };
    let integer_and_float = Point { x: 5, y: 4.0 };
}
```

**핵심 포인트:**
- 여러 제네릭 타입 매개변수를 사용하면 필드마다 다른 타입 가능
- 너무 많은 제네릭 매개변수는 코드를 읽기 어렵게 만들 수 있음

## 열거형 정의에서의 제네릭

### Option<T>

```rust
enum Option<T> {
    Some(T),
    None,
}
```

**핵심 포인트:**
- `Option<T>`는 값이 있거나 없을 수 있는 상황을 표현
- `Some(T)`는 타입 `T`의 값을 포함
- `None`은 값이 없음을 나타냄

### Result<T, E>

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

**핵심 포인트:**
- `Result<T, E>`는 연산이 성공하거나 실패할 수 있는 상황을 표현
- `Ok(T)`는 성공 시 타입 `T`의 값을 반환
- `Err(E)`는 실패 시 타입 `E`의 에러를 반환
- 파일 열기 등 실패 가능성이 있는 연산에 자주 사용됨

## 메서드 정의에서의 제네릭

### 제네릭 타입에 대한 제네릭 메서드

```rust
struct Point<T> {
    x: T,
    y: T,
}

impl<T> Point<T> {
    fn x(&self) -> &T {
        &self.x
    }
}
```

**핵심 포인트:**
- `impl` 뒤에 `T`를 선언하여 `Point<T>`에 메서드를 구현함을 명시
- `impl<T>`는 "모든 타입 T에 대해"라는 의미

### 특정 타입에 대한 메서드

특정 타입에 대해서만 메서드를 구현할 수 있습니다:

```rust
impl Point<f32> {
    fn distance_from_origin(&self) -> f32 {
        (self.x.powi(2) + self.y.powi(2)).sqrt()
    }
}
```

**핵심 포인트:**
- `Point<f32>`에만 `distance_from_origin` 메서드가 존재
- 다른 타입의 `Point<T>` 인스턴스에서는 이 메서드 사용 불가
- 타입별로 특화된 기능을 제공할 때 유용

### 다른 제네릭 매개변수를 가진 메서드

```rust
struct Point<X1, Y1> {
    x: X1,
    y: Y1,
}

impl<X1, Y1> Point<X1, Y1> {
    fn mixup<X2, Y2>(self, other: Point<X2, Y2>) -> Point<X1, Y2> {
        Point {
            x: self.x,
            y: other.y,
        }
    }
}
```

**핵심 포인트:**
- `impl`에 선언된 제네릭 매개변수(`X1`, `Y1`)와 메서드 시그니처에 선언된 제네릭 매개변수(`X2`, `Y2`)는 다름
- 이 예제에서 `mixup`은 두 `Point`를 혼합하여 새로운 `Point`를 생성
- `self.x`(타입 `X1`)와 `other.y`(타입 `Y2`)를 조합

## 제네릭 코드의 성능

### 런타임 비용 없음

제네릭 타입 사용은 구체적인 타입 사용에 비해 **런타임 성능 패널티가 없습니다**.

### 단형화 (Monomorphization)

Rust는 컴파일 타임에 **단형화(monomorphization)**를 수행합니다. 이는 제네릭 코드를 구체적인 타입으로 채워 특정 코드로 변환하는 과정입니다.

**제네릭 코드:**

```rust
let integer = Some(5);
let float = Some(5.0);
```

**단형화된 결과 (컴파일러가 생성):**

```rust
enum Option_i32 {
    Some(i32),
    None,
}

enum Option_f64 {
    Some(f64),
    None,
}

fn main() {
    let integer = Option_i32::Some(5);
    let float = Option_f64::Some(5.0);
}
```

**핵심 포인트:**
- 컴파일러가 사용된 각 구체적인 타입에 대해 특화된 버전을 생성
- 런타임 성능이 수동으로 중복 작성한 코드와 동일
- 제네릭의 유연성과 구체 코드의 성능을 모두 얻을 수 있음
- 컴파일 시간이 약간 증가할 수 있으나 런타임에는 영향 없음
