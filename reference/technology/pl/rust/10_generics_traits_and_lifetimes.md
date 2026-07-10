# 제네릭, 트레이트, 라이프타임

# 제네릭 타입, 트레이트, 그리고 라이프타임

> **원문:** https://doc.rust-lang.org/book/ch10-00-generics.html

## 개요

모든 프로그래밍 언어에는 개념의 중복을 효과적으로 처리하기 위한 도구가 있습니다. Rust에서 그러한 도구 중 하나가 **제네릭**(Generics)입니다: 구체적인 타입이나 다른 속성의 추상적인 대역입니다. 컴파일하고 코드를 실행할 때 그 자리에 무엇이 들어갈지 모르는 상태에서 제네릭의 동작이나 다른 제네릭과의 관계를 표현할 수 있습니다.

함수는 `i32`나 `String` 같은 구체적인 타입 대신 제네릭 타입의 매개변수를 받을 수 있습니다. 이는 알 수 없는 값을 가진 매개변수를 받아 여러 구체적인 값에 대해 동일한 코드를 실행하는 것과 같습니다.

**이미 사용해본 제네릭:**
- 6장의 `Option<T>`
- 8장의 `Vec<T>`와 `HashMap<K, V>`
- 9장의 `Result<T, E>`

이 장에서는 자신만의 타입, 함수, 메서드를 제네릭으로 정의하는 방법을 탐구합니다.

## 다루는 주제

1. **함수 추출로 중복 제거** - 코드 중복을 줄이기 위해 함수를 추출하는 방법 복습
2. **제네릭 함수** - 매개변수 타입만 다른 두 함수에서 제네릭 함수를 만드는 동일한 기법 사용
3. **구조체와 열거형의 제네릭 타입** - 구조체와 열거형 정의에서 제네릭 타입을 사용하는 방법
4. **트레이트(Traits)** - 동작을 제네릭 방식으로 정의하고, 트레이트와 제네릭 타입을 결합하여 제네릭 타입을 제약하는 방법
5. **라이프타임(Lifetimes)** - 참조들이 서로 어떻게 관련되는지 컴파일러에게 정보를 제공하는 제네릭의 일종

## 함수 추출로 중복 제거하기

### 초기 코드 - 가장 큰 숫자 찾기

**Listing 10-1**: 숫자 목록에서 가장 큰 숫자 찾기

```rust
fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");
    assert_eq!(*largest, 100);
}
```

이 코드는 정수 목록을 `number_list` 변수에 저장하고, 목록의 첫 번째 숫자에 대한 참조를 `largest`라는 변수에 넣습니다. 그런 다음 목록의 모든 숫자를 순회합니다. 현재 숫자가 `largest`에 저장된 숫자보다 크면 해당 변수의 참조를 교체합니다. 목록의 모든 숫자를 확인한 후, `largest`는 가장 큰 숫자(이 경우 100)를 참조해야 합니다.

### 중복 코드 문제

**Listing 10-2**: *두 개의* 숫자 목록에서 가장 큰 숫자를 찾는 코드

```rust
fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");

    let number_list = vec![102, 34, 6000, 89, 54, 2, 43, 8];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");
}
```

이 코드는 작동하지만, 코드를 중복하는 것은 지루하고 오류가 발생하기 쉽습니다. 또한 변경하고 싶을 때 여러 곳에서 코드를 업데이트해야 한다는 것을 기억해야 합니다.

### 함수 추출하기

**Listing 10-3**: 두 목록에서 가장 큰 숫자를 찾는 추상화된 코드

```rust
fn largest(list: &[i32]) -> &i32 {
    let mut largest = &list[0];

    for item in list {
        if item > largest {
            largest = item;
        }
    }

    largest
}

fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let result = largest(&number_list);
    println!("The largest number is {result}");
    assert_eq!(*result, 100);

    let number_list = vec![102, 34, 6000, 89, 54, 2, 43, 8];

    let result = largest(&number_list);
    println!("The largest number is {result}");
    assert_eq!(*result, 6000);
}
```

`largest` 함수는 `list`라는 매개변수를 가지며, 이는 함수에 전달할 수 있는 모든 구체적인 `i32` 값의 슬라이스를 나타냅니다. 결과적으로, 함수를 호출할 때 코드는 전달한 특정 값에 대해 실행됩니다.

### 리팩토링 단계

Listing 10-2에서 Listing 10-3으로 코드를 변경하기 위해 취한 단계:

1. **중복 코드 식별** - 반복되는 로직 인식
2. **중복 코드를 함수 본문으로 추출** - 함수 시그니처에서 해당 코드의 입력과 반환 값 지정
3. **중복된 코드 인스턴스 업데이트** - 코드를 반복하는 대신 함수 호출

### 제네릭으로의 다음 단계

이와 동일한 단계를 제네릭과 함께 사용하여 코드 중복을 더욱 줄일 것입니다. 함수 본문이 특정 값 대신 추상적인 `list`에서 작동할 수 있는 것처럼, 제네릭은 코드가 추상적인 타입에서 작동할 수 있게 합니다.

예를 들어, `i32` 값의 슬라이스에서 가장 큰 항목을 찾는 함수와 `char` 값의 슬라이스에서 가장 큰 항목을 찾는 함수, 두 개의 함수가 있다면 제네릭이 그 중복을 제거할 것입니다.

## 핵심 포인트

**제네릭의 장점:**
- 코드 중복 제거
- 다양한 타입에 대해 동일한 로직 적용
- 타입 안전성 유지하면서 유연성 제공

**함수 추출 과정:**
- 중복 코드 식별
- 공통 로직을 함수로 분리
- 매개변수와 반환 타입 정의
- 원래 코드를 함수 호출로 대체

**이 장에서 배울 내용:**
- 제네릭 함수 정의 방법
- 구조체와 열거형에서 제네릭 사용
- 트레이트로 동작 정의
- 라이프타임으로 참조 관계 표현

---

# 제네릭 데이터 타입

> **원문:** https://doc.rust-lang.org/book/ch10-01-syntax.html

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

Rust는 컴파일 타임에 **단형화**(monomorphization)를 수행합니다. 이는 제네릭 코드를 구체적인 타입으로 채워 특정 코드로 변환하는 과정입니다.

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

---

# 라이프타임으로 참조 유효성 검증하기

> **원문:** https://doc.rust-lang.org/book/ch10-03-lifetime-syntax.html

## 개요

라이프타임(Lifetime)은 참조가 필요한 만큼 오래 유효함을 보장하는 일종의 제네릭입니다. 대부분의 프로그래밍 언어와 달리 Rust는 특정 상황에서 명시적인 라이프타임 어노테이션을 요구합니다. 대부분의 경우 타입처럼 라이프타임도 암묵적으로 추론됩니다.

**핵심 포인트:**
- 라이프타임의 주요 목적은 댕글링 참조(dangling reference) 방지
- 대부분의 경우 라이프타임은 자동으로 추론됨
- 컴파일러가 추론할 수 없는 경우에만 명시적 어노테이션 필요
- 모든 분석은 컴파일 타임에 수행되어 런타임 성능에 영향 없음

## 댕글링 참조 방지

라이프타임의 주요 목적은 **댕글링 참조**를 방지하는 것입니다. 댕글링 참조란 이미 해제된 데이터를 가리키는 참조입니다.

### 잘못된 참조 예제 (Listing 10-16)

```rust
fn main() {
    let r;

    {
        let x = 5;
        r = &x;
    }

    println!("r: {r}");
}
```

이 코드는 컴파일되지 않습니다:

```
error[E0597]: `x` does not live long enough
```

**문제점:** 변수 `x`가 `r`이 사용되기 전에 스코프를 벗어나 댕글링 참조가 발생합니다.

## 빌림 검사기 (Borrow Checker)

Rust 컴파일러의 **빌림 검사기**는 스코프를 비교하여 모든 빌림이 유효한지 확인합니다.

### 라이프타임 시각화 (Listing 10-17)

```rust
fn main() {
    let r;                // ---------+-- 'a
                          //          |
    {                     //          |
        let x = 5;        // -+-- 'b  |
        r = &x;           //  |       |
    }                     // -+       |
                          //          |
    println!("r: {r}");   //          |
}                         // ---------+
```

**분석:**
- `'a`: `r`의 라이프타임
- `'b`: `x`의 라이프타임
- `'b`가 `'a`보다 짧으므로 프로그램이 거부됨

### 수정된 코드 (Listing 10-18)

```rust
fn main() {
    let x = 5;            // ----------+-- 'b
                          //           |
    let r = &x;           // --+-- 'a  |
                          //   |       |
    println!("r: {r}");   //   |       |
                          // --+       |
}                         // ----------+
```

이제 `'a`가 `'b` 내에 포함되므로 코드가 컴파일됩니다.

## 함수에서의 제네릭 라이프타임

### 문제 상황 (Listing 10-20)

```rust
fn longest(x: &str, y: &str) -> &str {
    if x.len() > y.len() { x } else { y }
}
```

오류:

```
error[E0106]: missing lifetime specifier
 --> src/main.rs:9:33
  |
9 | fn longest(x: &str, y: &str) -> &str {
  |               ----     ----     ^ expected named lifetime parameter
  |
  = help: this function's return type contains a borrowed value, but the signature
    does not say whether it is borrowed from `x` or `y`
```

**문제점:** 컴파일러가 반환되는 참조가 `x`에서 오는지 `y`에서 오는지 결정할 수 없습니다.

## 라이프타임 어노테이션 문법

라이프타임 어노테이션은 참조가 실제로 얼마나 오래 사는지를 변경하지 않고, 여러 참조의 라이프타임 간 **관계**를 설명합니다.

### 기본 문법

라이프타임 매개변수 이름:
- 아포스트로피(`'`)로 시작
- 보통 소문자이고 짧음 (예: `'a`)
- `&` 뒤에 공백과 함께 배치

```rust
&i32        // 참조
&'a i32     // 명시적 라이프타임을 가진 참조
&'a mut i32 // 명시적 라이프타임을 가진 가변 참조
```

## 함수 시그니처에서의 라이프타임

라이프타임 어노테이션을 사용하려면 함수 이름과 매개변수 목록 사이의 꺾쇠 괄호 안에 제네릭 라이프타임 매개변수를 선언합니다.

### 해결책 (Listing 10-21)

```rust
fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() { x } else { y }
}

fn main() {
    let string1 = String::from("abcd");
    let string2 = "xyz";

    let result = longest(string1.as_str(), string2);
    println!("The longest string is {result}");
}
```

**이 시그니처가 Rust에게 알려주는 것:**
- 어떤 라이프타임 `'a`에 대해, 함수는 최소 `'a`만큼 사는 두 문자열 슬라이스를 받음
- 반환되는 문자열 슬라이스도 최소 `'a`만큼 삶
- 실제 반환값의 라이프타임은 인자들의 라이프타임 중 더 작은 것

### 유효한 예제 (Listing 10-22)

```rust
fn main() {
    let string1 = String::from("long string is long");

    {
        let string2 = String::from("xyz");
        let result = longest(string1.as_str(), string2.as_str());
        println!("The longest string is {result}");
    }
}

fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() { x } else { y }
}
```

`result`가 `string2`가 유효한 동안 사용되므로 컴파일됩니다.

### 유효하지 않은 예제 (Listing 10-23)

```rust
fn main() {
    let string1 = String::from("long string is long");
    let result;
    {
        let string2 = String::from("xyz");
        result = longest(string1.as_str(), string2.as_str());
    }
    println!("The longest string is {result}");
}

fn longest<'a>(x: &'a str, y: &'a str) -> &'a str {
    if x.len() > y.len() { x } else { y }
}
```

오류:

```
error[E0597]: `string2` does not live long enough
```

**문제점:** 반환된 참조가 `println!`까지 유효하려면 `string2`가 유효해야 하지만, 그 전에 드롭됩니다.

## 라이프타임 관계

라이프타임 매개변수는 함수가 수행하는 작업에 따라 달라집니다.

### 하나의 매개변수만 관련되는 경우

```rust
fn longest<'a>(x: &'a str, y: &str) -> &'a str {
    x
}
```

반환 타입에 `x`의 라이프타임만 관련되므로 `y`에는 라이프타임 어노테이션이 필요하지 않습니다.

### 댕글링 참조 예제

```rust
fn longest<'a>(x: &str, y: &str) -> &'a str {
    let result = String::from("really long string");
    result.as_str()
}
```

오류:

```
error[E0515]: cannot return value referencing local variable `result`
```

**해결책:** 함수 내에서 생성된 데이터에 대한 참조는 반환할 수 없습니다. 소유된 데이터를 대신 반환해야 합니다.

## 구조체 정의에서의 라이프타임

참조를 보유하는 구조체는 라이프타임 어노테이션이 필요합니다.

### 예제 (Listing 10-24)

```rust
struct ImportantExcerpt<'a> {
    part: &'a str,
}

fn main() {
    let novel = String::from("Call me Ishmael. Some years ago...");
    let first_sentence = novel.split('.').next().unwrap();
    let i = ImportantExcerpt {
        part: first_sentence,
    };
}
```

**핵심 포인트:**
- `ImportantExcerpt` 인스턴스는 `part` 필드의 참조보다 오래 살 수 없음
- 구조체와 참조된 데이터의 라이프타임이 연결됨

## 라이프타임 생략 (Lifetime Elision)

**라이프타임 생략 규칙**은 컴파일러가 라이프타임을 추론할 수 있는 패턴입니다.

### 역사적 배경

초기 Rust(1.0 이전)에서는 모든 참조에 명시적 라이프타임 어노테이션이 필요했습니다. 프로그래머들이 동일한 패턴을 반복적으로 사용한다는 것을 발견하고, 이러한 패턴을 컴파일러에 프로그래밍했습니다.

### 세 가지 생략 규칙

**규칙 1 (입력 라이프타임):** 각 참조 매개변수는 고유한 라이프타임 매개변수를 받습니다.

```rust
fn foo<'a>(x: &'a i32)                           // 매개변수 하나
fn foo<'a, 'b>(x: &'a i32, y: &'b i32)          // 매개변수 둘
```

**규칙 2 (출력 라이프타임):** 입력 라이프타임이 정확히 하나이면, 모든 출력 라이프타임에 할당됩니다.

```rust
fn foo<'a>(x: &'a i32) -> &'a i32
```

**규칙 3 (메서드 라이프타임):** 매개변수 중 하나가 `&self` 또는 `&mut self`이면, 그 라이프타임이 모든 출력 라이프타임에 할당됩니다.

### 예제: `first_word` (Listing 10-25)

```rust
fn first_word(s: &str) -> &str {
    let bytes = s.as_bytes();

    for (i, &item) in bytes.iter().enumerate() {
        if item == b' ' {
            return &s[0..i];
        }
    }

    &s[..]
}
```

**규칙 적용:**
1. 규칙 1: `fn first_word<'a>(s: &'a str) -> &str`
2. 규칙 2 (입력 라이프타임이 하나): `fn first_word<'a>(s: &'a str) -> &'a str`

명시적 어노테이션 없이 컴파일됩니다.

### 예제: `longest` (Listing 10-20에서)

```rust
fn longest(x: &str, y: &str) -> &str {
```

**규칙 적용:**
1. 규칙 1: `fn longest<'a, 'b>(x: &'a str, y: &'b str) -> &str`
2. 규칙 2 적용 불가 (여러 입력 라이프타임)
3. 규칙 3 적용 불가 (메서드가 아님)

**결과:** 컴파일 오류 - 라이프타임이 여전히 모호함

## 메서드 정의에서의 라이프타임

메서드 라이프타임은 제네릭 타입 매개변수와 동일한 문법을 사용합니다.

### 기본 예제

```rust
struct ImportantExcerpt<'a> {
    part: &'a str,
}

impl<'a> ImportantExcerpt<'a> {
    fn level(&self) -> i32 {
        3
    }
}
```

**핵심 포인트:**
- 라이프타임 매개변수는 `impl` 뒤에 선언
- 구조체 이름 뒤에 사용

### 여러 매개변수가 있는 메서드

```rust
struct ImportantExcerpt<'a> {
    part: &'a str,
}

impl<'a> ImportantExcerpt<'a> {
    fn announce_and_return_part(&self, announcement: &str) -> &str {
        println!("Attention please: {announcement}");
        self.part
    }
}
```

**규칙 3 적용:** 반환 타입이 `&self`의 라이프타임을 받으므로 반환값에 명시적 어노테이션이 필요하지 않습니다.

## 정적 라이프타임 ('static)

`'static` 라이프타임은 참조가 **프로그램 전체 기간** 동안 유효할 수 있음을 나타냅니다.

### 예제

```rust
let s: &'static str = "I have a static lifetime.";
```

모든 문자열 리터럴은 프로그램 바이너리에 저장되므로 `'static` 라이프타임을 가집니다.

### 주의사항

**경고:** 신중하게 고려하지 않고 `'static`을 지정하지 마세요. `'static`을 제안하는 오류 메시지는 보통 댕글링 참조나 라이프타임 불일치를 나타냅니다. `'static`으로 기본 설정하지 말고 해당 문제를 해결하세요.

## 제네릭 타입 매개변수, 트레이트 바운드, 라이프타임 결합

세 가지 모두 하나의 함수에서 결합할 수 있습니다:

```rust
use std::fmt::Display;

fn longest_with_an_announcement<'a, T>(
    x: &'a str,
    y: &'a str,
    ann: T,
) -> &'a str
where
    T: Display,
{
    println!("Announcement! {ann}");
    if x.len() > y.len() { x } else { y }
}

fn main() {
    let string1 = String::from("abcd");
    let string2 = "xyz";

    let result = longest_with_an_announcement(
        string1.as_str(),
        string2,
        "Today is someone's birthday!",
    );
    println!("The longest string is {result}");
}
```

**핵심 포인트:**
- 라이프타임 매개변수와 제네릭 타입 매개변수는 꺾쇠 괄호 안에 함께 선언
- `where` 절을 사용하여 트레이트 바운드 지정

## 요약

| 개념 | 설명 |
|------|------|
| **제네릭 타입 매개변수** | 코드가 다양한 타입과 작동할 수 있게 함 |
| **트레이트와 트레이트 바운드** | 제네릭 타입이 필요한 동작을 가지도록 보장 |
| **라이프타임 어노테이션** | 유연한 코드가 댕글링 참조를 갖지 않도록 보장 |

모든 분석은 컴파일 타임에 수행되어 런타임 성능에 영향을 주지 않습니다.
