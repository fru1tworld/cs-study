# 메서드 문법

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

종종 메서드에 필드와 같은 이름을 부여할 때 필드의 값만 반환하고 다른 작업은 수행하지 않게 합니다. 이러한 메서드를 *getter*라고 하며, Rust는 일부 다른 언어처럼 구조체 필드에 대해 자동으로 구현하지 않습니다. getter는 필드를 비공개로 만들고 메서드를 공개로 만들어 타입의 공개 API의 일부로 해당 필드에 대한 읽기 전용 액세스를 활성화할 수 있어 유용합니다.

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

첫 번째가 훨씬 깔끔해 보입니다. 이 자동 참조 동작은 메서드가 명확한 수신자(`self`의 타입)를 가지고 있기 때문에 작동합니다. 수신자와 메서드 이름이 주어지면 Rust는 메서드가 읽기(`&self`), 변경(`&mut self`), 또는 소비(`self`)하는지 확실히 파악할 수 있습니다. Rust가 메서드 수신자에 대해 빌림을 암시적으로 만드는 것은 실제로 소유권을 인체공학적으로 만드는 큰 부분입니다.

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

`impl` 블록 내에 정의된 모든 함수는 `impl` 뒤에 명명된 타입과 연관되어 있으므로 *연관 함수*라고 합니다. 첫 번째 매개변수로 `self`를 가지지 않는 연관 함수(따라서 메서드가 아닌)를 정의할 수 있습니다. 이들은 작업할 타입의 인스턴스가 필요하지 않기 때문입니다. 우리는 이미 이와 같은 함수를 사용했습니다: `String` 타입에 정의된 `String::from` 함수가 그것입니다.

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

이 연관 함수를 호출하려면 구조체 이름과 함께 `::` 문법을 사용합니다. `let sq = Rectangle::square(3);`이 그 예입니다. 이 함수는 구조체에 의해 네임스페이스가 지정됩니다: `::` 문법은 연관 함수와 모듈이 생성하는 네임스페이스 모두에 사용됩니다.

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
