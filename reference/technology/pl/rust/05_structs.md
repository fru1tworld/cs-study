# 구조체

# 구조체를 사용하여 관련 데이터 구조화하기

> **원문:** https://doc.rust-lang.org/book/ch05-00-structs.html

**구조체(struct)** 또는 **structure**는 의미 있는 그룹을 구성하는 여러 관련 값을 함께 패키징하고 이름을 지정할 수 있는 사용자 정의 데이터 타입입니다. 객체 지향 언어에 익숙하다면, 구조체는 객체의 데이터 속성과 같습니다.

## 챕터 내용

이 챕터에서는 다음 내용을 다룹니다:

- **구조체 정의 및 인스턴스화** - 구조체 타입을 생성하고 사용하는 방법
- **연관 함수** - 구조체 타입과 연관된 함수
- **메서드** - 구조체 타입과 연관된 동작을 지정하는 특별한 종류의 연관 함수
- **구조체와 열거형을 빌딩 블록으로 사용** - 구조체(6장의 열거형과 함께)가 프로그램 도메인에서 새로운 타입을 만드는 기본 요소가 되어 Rust의 컴파일 타임 타입 검사를 최대한 활용하는 방법

## 핵심 개념

### 구조체 vs 튜플
이 챕터에서는 구조체와 튜플을 비교하고 대조하여, 튜플보다 구조체가 데이터를 그룹화하는 더 나은 방법인 경우를 보여줍니다.

### 타입 안전성
구조체는 프로그램 도메인에서 새로운 타입을 만들기 위한 빌딩 블록 중 하나로, Rust의 컴파일 타임 타입 검사 기능을 활용할 수 있게 해줍니다.

---

# 구조체 정의 및 인스턴스화

> **원문:** https://doc.rust-lang.org/book/ch05-01-defining-structs.html

## 개요

구조체는 튜플과 유사하지만 각 데이터에 이름을 지정할 수 있어 더 유연하고 명확합니다. 튜플과 달리 값에 접근하기 위해 데이터의 순서에 의존할 필요가 없습니다.

## 구조체 정의

구조체를 정의하려면 `struct` 키워드를 사용하고, 구조체의 이름을 지정한 다음, 중괄호 안에 데이터 조각들(_필드_라고 함)의 이름과 타입을 정의합니다.

```rust
struct User {
    active: bool,
    username: String,
    email: String,
    sign_in_count: u64,
}

fn main() {}
```

## 인스턴스 생성

구조체 이름을 지정하고 `key: value` 쌍을 사용하여 각 필드에 구체적인 값을 제공하여 인스턴스를 생성합니다. 필드는 정의와 동일한 순서로 지정할 필요가 없습니다.

```rust
struct User {
    active: bool,
    username: String,
    email: String,
    sign_in_count: u64,
}

fn main() {
    let user1 = User {
        active: true,
        username: String::from("someusername123"),
        email: String::from("someone@example.com"),
        sign_in_count: 1,
    };
}
```

## 필드 접근 및 수정

점 표기법을 사용하여 값에 접근합니다: `user1.email`

필드를 수정하려면 전체 인스턴스가 가변(mutable)이어야 합니다:

```rust
struct User {
    active: bool,
    username: String,
    email: String,
    sign_in_count: u64,
}

fn main() {
    let mut user1 = User {
        active: true,
        username: String::from("someusername123"),
        email: String::from("someone@example.com"),
        sign_in_count: 1,
    };

    user1.email = String::from("anotheremail@example.com");
}
```

**참고:** Rust는 특정 필드만 가변으로 표시하는 것을 허용하지 않습니다. 전체 인스턴스가 가변이어야 합니다.

## 함수에서 구조체 인스턴스 반환

함수 본문의 마지막 표현식으로 구조체의 새 인스턴스를 생성하여 암시적으로 반환할 수 있습니다:

```rust
struct User {
    active: bool,
    username: String,
    email: String,
    sign_in_count: u64,
}

fn build_user(email: String, username: String) -> User {
    User {
        active: true,
        username: username,
        email: email,
        sign_in_count: 1,
    }
}

fn main() {
    let user1 = build_user(
        String::from("someone@example.com"),
        String::from("someusername123"),
    );
}
```

## 필드 초기화 축약법

매개변수 이름이 구조체 필드 이름과 일치하는 경우, 필드 초기화 축약 문법을 사용하여 반복을 제거할 수 있습니다:

```rust
struct User {
    active: bool,
    username: String,
    email: String,
    sign_in_count: u64,
}

fn build_user(email: String, username: String) -> User {
    User {
        active: true,
        username,
        email,
        sign_in_count: 1,
    }
}

fn main() {
    let user1 = build_user(
        String::from("someone@example.com"),
        String::from("someusername123"),
    );
}
```

## 구조체 업데이트 문법

`..` 문법을 사용하여 다른 인스턴스의 대부분의 값을 포함하는 새 인스턴스를 생성합니다:

```rust
struct User {
    active: bool,
    username: String,
    email: String,
    sign_in_count: u64,
}

fn main() {
    let user1 = User {
        email: String::from("someone@example.com"),
        username: String::from("someusername123"),
        active: true,
        sign_in_count: 1,
    };

    let user2 = User {
        email: String::from("another@example.com"),
        ..user1
    };
}
```

**중요:** `..` 문법은 데이터를 이동시킵니다. 이 코드 이후에 이동된 필드(예: `String` 같은 `Copy`가 아닌 타입)가 포함된 경우 `user1`을 사용할 수 없습니다. `active`와 `sign_in_count`는 `Copy` 트레이트를 구현하므로 여전히 사용할 수 있습니다. `user1.email`은 이동되지 않았으므로 여전히 사용할 수 있습니다.

## 튜플 구조체

튜플 구조체는 튜플과 유사하게 보이지만 개별 필드에 이름을 지정하지 않고도 구조체 이름으로 의미를 부여합니다. 튜플에 이름을 부여하고 별개의 타입으로 만드는 데 유용합니다.

```rust
struct Color(i32, i32, i32);
struct Point(i32, i32, i32);

fn main() {
    let black = Color(0, 0, 0);
    let origin = Point(0, 0, 0);
}
```

각 구조체는 자체 타입이므로, `Color`를 받는 함수는 두 구조체가 동일한 필드 타입을 가지더라도 `Point`를 인수로 받을 수 없습니다.

인덱스와 함께 점 표기법을 사용하여 개별 값에 접근합니다: `origin.0`

구조체 이름을 사용하여 구조 분해할 수 있습니다:
```rust
let Point(x, y, z) = origin;
```

## 유닛과 유사한 구조체

`struct` 키워드와 세미콜론만 사용하여 필드가 없는 구조체를 정의합니다:

```rust
struct AlwaysEqual;

fn main() {
    let subject = AlwaysEqual;
}
```

이들은 타입에 트레이트를 구현해야 하지만 데이터를 저장할 필요가 없을 때 유용합니다. 트레이트는 10장에서 다룹니다.

## 구조체 데이터의 소유권

`User` 구조체는 `&str` 문자열 슬라이스가 아닌 소유된 `String` 타입을 사용하여 각 인스턴스가 모든 데이터를 소유하고 구조체가 유효한 한 데이터가 유효하도록 합니다.

구조체는 다른 곳에서 소유한 데이터에 대한 참조를 저장할 수 있지만, 이를 위해서는 _라이프타임_(10장 주제)을 사용하여 참조된 데이터가 구조체가 유효한 동안 유효하도록 보장해야 합니다. 라이프타임 지정자 없이는 컴파일러가 오류를 발생시킵니다:

```rust
struct User {
    active: bool,
    username: &str,
    email: &str,
    sign_in_count: u64,
}

fn main() {
    let user1 = User {
        active: true,
        username: "someusername123",
        email: "someone@example.com",
        sign_in_count: 1,
    };
}
```

이 코드는 라이프타임 지정자가 필요하다는 컴파일러 오류를 생성합니다. 지금은 `&str` 같은 참조 대신 `String` 같은 소유 타입을 사용하세요.

---

# 구조체를 사용한 예제 프로그램

> **원문:** https://doc.rust-lang.org/book/ch05-02-example-structs.html

## 개요
이 챕터에서는 직사각형의 면적을 계산하는 프로그램을 작성하면서 구조체를 언제, 어떻게 사용하는지 보여줍니다. 이 섹션에서는 별도의 변수 사용, 튜플 사용, 마지막으로 구조체 사용의 세 가지 접근 방식을 통해 진행합니다.

## 초기 접근 방식: 별도의 변수

**파일명: src/main.rs**

```rust
fn main() {
    let width1 = 30;
    let height1 = 50;

    println!(
        "The area of the rectangle is {} square pixels.",
        area(width1, height1)
    );
}

fn area(width: u32, height: u32) -> u32 {
    width * height
}
```

**Listing 5-8**: 별도의 너비와 높이 변수로 지정된 직사각형의 면적 계산

`cargo run`으로 실행:
```
$ cargo run
   Compiling rectangles v0.1.0 (file:///projects/rectangles)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.42s
     Running `target/debug/rectangles`
The area of the rectangle is 1500 square pixels.
```

**문제점**: `area` 함수 시그니처가 매개변수들이 서로 관련되어 있다는 것을 명확하게 보여주지 않습니다.

---

## 튜플로 리팩토링

**파일명: src/main.rs**

```rust
fn main() {
    let rect1 = (30, 50);

    println!(
        "The area of the rectangle is {} square pixels.",
        area(rect1)
    );
}

fn area(dimensions: (u32, u32)) -> u32 {
    dimensions.0 * dimensions.1
}
```

**Listing 5-9**: 튜플로 직사각형의 너비와 높이 지정

**장점**: 구조를 추가하고 단일 인수를 전달합니다.

**단점**:
- 튜플은 요소에 이름을 붙이지 않습니다
- 튜플 부분에 인덱스로 접근해야 해서(`.0`과 `.1`) 계산이 덜 명확합니다
- 너비와 높이를 혼동하여 오류를 발생시키기 쉽습니다
- 코드를 사용하는 다른 개발자에게 덜 명확합니다

---

## 구조체로 리팩토링

**파일명: src/main.rs**

```rust
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!(
        "The area of the rectangle is {} square pixels.",
        area(&rect1)
    );
}

fn area(rectangle: &Rectangle) -> u32 {
    rectangle.width * rectangle.height
}
```

**Listing 5-10**: `Rectangle` 구조체 정의

**이점**:
- 데이터에 의미 있는 레이블을 추가합니다
- 함수 시그니처가 Rectangle의 면적을 계산한다는 것을 명확히 나타냅니다
- 너비와 높이가 관련되어 있음을 보여줍니다
- 소유권을 가져가지 않기 위해 불변 빌림(`&Rectangle`)을 사용합니다
- `main`이 `rect1`의 소유권을 유지하고 계속 사용할 수 있습니다

---

## 파생 트레이트로 기능 추가

### 출력 시도

**파일명: src/main.rs**

```rust
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!("rect1 is {rect1}");
}
```

**Listing 5-11**: `Rectangle` 인스턴스 출력 시도

**오류**:
```
error[E0277]: `Rectangle` doesn't implement `std::fmt::Display`
```

구조체는 포맷팅 방법이 여러 가지이기 때문에 `Display`의 기본 구현을 제공하지 않습니다.

### Debug 포맷팅 사용

`Debug` 트레이트는 개발자에게 유용한 출력을 제공합니다. `:?` 지정자를 사용합니다:

```rust
println!("rect1 is {rect1:?}");
```

그러나 먼저 `Debug` 트레이트를 파생해야 합니다.

### Debug 트레이트 파생

**파일명: src/main.rs**

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!("rect1 is {rect1:?}");
}
```

**Listing 5-12**: `Debug` 트레이트 파생을 위한 속성 추가

**출력**:
```
$ cargo run
   Compiling rectangles v0.1.0 (file:///projects/rectangles)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.48s
     Running `target/debug/rectangles`
rect1 is Rectangle { width: 30, height: 50 }
```

### 예쁜 출력

더 큰 구조체의 경우 더 읽기 쉬운 출력을 위해 `{:#?}`를 사용합니다:

```rust
println!("rect1 is {rect1:#?}");
```

**출력**:
```
rect1 is Rectangle {
    width: 30,
    height: 50,
}
```

### dbg! 매크로 사용

`dbg!` 매크로는 표현식의 소유권을 가져가고, 파일과 줄 번호를 출력하고, 값을 출력한 다음 소유권을 반환합니다:

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let scale = 2;
    let rect1 = Rectangle {
        width: dbg!(30 * scale),
        height: 50,
    };

    dbg!(&rect1);
}
```

**출력**:
```
$ cargo run
   Compiling rectangles v0.1.0 (file:///projects/rectangles)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.61s
     Running `target/debug/rectangles`
[src/main.rs:10:16] 30 * scale = 60
[src/main.rs:14:5] &rect1 = Rectangle {
    width: 60,
    height: 50,
}
```

**참고**: `dbg!` 매크로는 `stderr`로 출력하고, `println!`은 `stdout`으로 출력합니다.

---

## 요약

이 챕터에서는 코드 명확성을 점진적으로 개선하는 방법을 보여줍니다:

1. **별도의 변수**: 관련 데이터에 대한 명확성 부족
2. **튜플**: 데이터를 그룹화하지만 의미론적 의미 부족
3. **구조체**: 명명된 필드와 명확한 의미론 제공
4. **파생 트레이트**: 유용한 디버깅 기능 활성화

`#[derive(Debug)]` 속성은 구조체를 디버깅 목적으로 출력 가능하게 만듭니다. 추가 파생 가능한 트레이트는 부록 C에서 확인할 수 있습니다. 다음 섹션에서는 `area` 함수를 `Rectangle` 구조체의 메서드로 변환하는 방법을 다룹니다.

---

# 메서드 문법

> **원문:** https://doc.rust-lang.org/book/ch05-03-method-syntax.html

## 메서드

메서드는 함수와 유사합니다: `fn` 키워드와 이름으로 선언하고, 매개변수와 반환 값을 가질 수 있으며, 다른 곳에서 메서드가 호출될 때 실행되는 코드를 포함합니다. 함수와 달리 메서드는 구조체(또는 열거형이나 트레이트 객체)의 컨텍스트 내에서 정의되며, 첫 번째 매개변수는 항상 `self`로, 메서드가 호출되는 구조체의 인스턴스를 나타냅니다.

## 메서드 문법

`Rectangle` 인스턴스를 매개변수로 받는 `area` 함수를 `Rectangle` 구조체에 정의된 `area` 메서드로 변경해 봅시다.

**파일명: src/main.rs**

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

impl Rectangle {
    fn area(&self) -> u32 {
        self.width * self.height
    }
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!(
        "The area of the rectangle is {} square pixels.",
        rect1.area()
    );
}
```

`Rectangle`의 컨텍스트 내에서 함수를 정의하기 위해 `Rectangle`에 대한 `impl`(구현) 블록을 시작합니다. 이 `impl` 블록 내의 모든 것은 `Rectangle` 타입과 연관됩니다. 그런 다음 `area` 함수를 `impl` 중괄호 내로 이동하고 시그니처와 본문 전체에서 첫 번째 매개변수를 `self`로 변경합니다.

`area`의 시그니처에서 `rectangle: &Rectangle` 대신 `&self`를 사용합니다. `&self`는 실제로 `self: &Self`의 축약형입니다. `impl` 블록 내에서 `Self` 타입은 `impl` 블록이 대상으로 하는 타입의 별칭입니다. 메서드는 첫 번째 매개변수로 `Self` 타입의 `self`라는 이름의 매개변수를 가져야 하므로, Rust는 첫 번째 매개변수 위치에서 `self`라는 이름만으로 이를 축약할 수 있게 합니다.

소유권을 가져가지 않고 구조체의 데이터를 읽기만 하고 쓰지 않기 때문에 `&self`를 선택했습니다. 메서드가 수행하는 작업의 일부로 메서드가 호출된 인스턴스를 변경하려면 첫 번째 매개변수로 `&mut self`를 사용합니다. `self`만 사용하여 인스턴스의 소유권을 가져가는 메서드는 드뭅니다. 이 기법은 메서드가 `self`를 다른 것으로 변환하고 변환 후 호출자가 원래 인스턴스를 사용하지 못하게 하려는 경우에 주로 사용됩니다.

### 메서드는 필드와 같은 이름을 가질 수 있음

구조체의 필드 중 하나와 같은 이름을 메서드에 부여할 수 있습니다.

**파일명: src/main.rs**

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

impl Rectangle {
    fn width(&self) -> bool {
        self.width > 0
    }
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    if rect1.width() {
        println!("The rectangle has a nonzero width; it is {}", rect1.width);
    }
}
```

`rect1.width` 뒤에 괄호를 붙이면 Rust는 `width` 메서드를 의미한다는 것을 압니다. 괄호를 사용하지 않으면 Rust는 `width` 필드를 의미한다는 것을 압니다.

종종 메서드에 필드와 같은 이름을 부여할 때 필드의 값만 반환하고 다른 작업은 수행하지 않게 합니다. 이러한 메서드를 *getter*라고 하며, Rust는 일부 다른 언어처럼 구조체 필드에 자동으로 구현하지 않습니다. getter는 필드를 비공개로 만들고 메서드를 공개로 만들어 타입의 공개 API의 일부로 해당 필드에 대한 읽기 전용 액세스를 활성화할 수 있어 유용합니다.

## `->` 연산자는 어디에?

C와 C++에서는 메서드 호출에 두 가지 다른 연산자가 사용됩니다: 객체에서 직접 메서드를 호출하는 경우 `.`를 사용하고, 객체에 대한 포인터에서 메서드를 호출하고 먼저 포인터를 역참조해야 하는 경우 `->`를 사용합니다.

Rust에는 `->` 연산자에 해당하는 것이 없습니다. 대신 Rust에는 *자동 참조 및 역참조*라는 기능이 있습니다. 메서드 호출은 Rust에서 이 동작이 있는 몇 안 되는 곳 중 하나입니다.

작동 방식은 다음과 같습니다: `object.something()`으로 메서드를 호출하면 Rust가 `&`, `&mut` 또는 `*`를 자동으로 추가하여 `object`가 메서드의 시그니처와 일치하게 합니다. 다시 말해 다음은 동일합니다:

```rust
#[derive(Debug,Copy,Clone)]
struct Point {
    x: f64,
    y: f64,
}

impl Point {
   fn distance(&self, other: &Point) -> f64 {
       let x_squared = f64::powi(other.x - self.x, 2);
       let y_squared = f64::powi(other.y - self.y, 2);

       f64::sqrt(x_squared + y_squared)
   }
}

let p1 = Point { x: 0.0, y: 0.0 };
let p2 = Point { x: 5.0, y: 6.5 };
p1.distance(&p2);
(&p1).distance(&p2);
```

첫 번째가 훨씬 깔끔해 보입니다. 이 자동 참조 동작은 메서드가 명확한 수신자(`self`의 타입)가 있기 때문에 작동합니다. 수신자와 메서드 이름이 주어지면 Rust는 메서드가 읽기(`&self`), 변경(`&mut self`), 또는 소비(`self`)하는지 확실히 파악할 수 있습니다. Rust가 메서드 수신자의 빌림을 암시적으로 만드는 것은 실제로 소유권을 인체공학적으로 만드는 큰 부분입니다.

## 더 많은 매개변수를 가진 메서드

`Rectangle` 구조체에 두 번째 메서드를 구현하여 메서드 사용을 연습해 봅시다. 이번에는 `Rectangle` 인스턴스가 다른 `Rectangle` 인스턴스를 받아 두 번째 `Rectangle`이 `self` 내에 완전히 들어갈 수 있으면 `true`를 반환하고, 그렇지 않으면 `false`를 반환하게 합니다.

**파일명: src/main.rs**

```rust
fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };
    let rect2 = Rectangle {
        width: 10,
        height: 40,
    };
    let rect3 = Rectangle {
        width: 60,
        height: 45,
    };

    println!("Can rect1 hold rect2? {}", rect1.can_hold(&rect2));
    println!("Can rect1 hold rect3? {}", rect1.can_hold(&rect3));
}
```

`rect2`의 두 치수가 모두 `rect1`의 치수보다 작지만 `rect3`은 `rect1`보다 넓기 때문에 예상 출력은 다음과 같습니다:

```
Can rect1 hold rect2? true
Can rect1 hold rect3? false
```

메서드 이름은 `can_hold`이고, 다른 `Rectangle`의 불변 빌림을 매개변수로 받습니다. `can_hold`의 반환 값은 Boolean입니다.

**파일명: src/main.rs**

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

impl Rectangle {
    fn area(&self) -> u32 {
        self.width * self.height
    }

    fn can_hold(&self, other: &Rectangle) -> bool {
        self.width > other.width && self.height > other.height
    }
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };
    let rect2 = Rectangle {
        width: 10,
        height: 40,
    };
    let rect3 = Rectangle {
        width: 60,
        height: 45,
    };

    println!("Can rect1 hold rect2? {}", rect1.can_hold(&rect2));
    println!("Can rect1 hold rect3? {}", rect1.can_hold(&rect3));
}
```

메서드는 `self` 매개변수 뒤에 시그니처에 추가하는 여러 매개변수를 받을 수 있으며, 이러한 매개변수는 함수의 매개변수와 동일하게 작동합니다.

## 연관 함수

`impl` 블록 내에 정의된 모든 함수는 `impl` 뒤에 명명된 타입과 연관되어 있으므로 *연관 함수*라고 합니다. 첫 번째 매개변수로 `self`를 가지지 않는 연관 함수(따라서 메서드가 아닌)를 정의할 수 있습니다. 이들은 작업할 타입의 인스턴스가 필요하지 않기 때문입니다. 우리는 이미 이와 같은 함수를 사용했습니다: `String` 타입에 정의된 `String::from` 함수입니다.

메서드가 아닌 연관 함수는 구조체의 새 인스턴스를 반환하는 생성자로 자주 사용됩니다. 이들은 종종 `new`라고 불리지만, `new`는 특별한 이름이 아니며 언어에 내장되어 있지 않습니다. 예를 들어, `square`라는 이름의 연관 함수를 제공할 수 있습니다:

**파일명: src/main.rs**

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

impl Rectangle {
    fn square(size: u32) -> Self {
        Self {
            width: size,
            height: size,
        }
    }
}

fn main() {
    let sq = Rectangle::square(3);
}
```

반환 타입과 함수 본문의 `Self` 키워드는 `impl` 키워드 뒤에 나오는 타입의 별칭으로, 이 경우 `Rectangle`입니다.

이 연관 함수를 호출하려면 구조체 이름과 함께 `::` 문법을 사용합니다. `let sq = Rectangle::square(3);`이 그 예입니다. 구조체가 이 함수의 네임스페이스를 지정합니다: `::` 문법은 연관 함수와 모듈이 생성하는 네임스페이스 모두에 사용됩니다.

## 여러 `impl` 블록

각 구조체는 여러 `impl` 블록을 가질 수 있습니다. 예를 들어, 다음 코드는 각 메서드를 자체 `impl` 블록에 포함합니다:

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

impl Rectangle {
    fn area(&self) -> u32 {
        self.width * self.height
    }
}

impl Rectangle {
    fn can_hold(&self, other: &Rectangle) -> bool {
        self.width > other.width && self.height > other.height
    }
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };
    let rect2 = Rectangle {
        width: 10,
        height: 40,
    };
    let rect3 = Rectangle {
        width: 60,
        height: 45,
    };

    println!("Can rect1 hold rect2? {}", rect1.can_hold(&rect2));
    println!("Can rect1 hold rect3? {}", rect1.can_hold(&rect3));
}
```

여기서 이러한 메서드를 여러 `impl` 블록으로 분리할 이유는 없지만, 이것은 유효한 문법입니다. 여러 `impl` 블록은 제네릭 타입과 트레이트를 다룰 때 유용합니다.

## 요약

구조체를 사용하면 도메인에 의미 있는 사용자 정의 타입을 만들 수 있습니다. 구조체를 사용하면 관련된 데이터 조각을 서로 연결하고 각 조각에 이름을 지정하여 코드를 명확하게 만들 수 있습니다. `impl` 블록에서 타입과 연관된 함수를 정의할 수 있으며, 메서드는 구조체 인스턴스가 가지는 동작을 지정할 수 있는 연관 함수의 한 종류입니다.
