# 열거형 정의하기

## 개요

열거형을 사용하면 가능한 변형을 열거하여 타입을 정의할 수 있습니다. 구조체가 관련 필드를 함께 그룹화하는 반면, 열거형은 값이 가능한 값들의 집합 중 하나임을 표현할 수 있게 합니다.

## 기본 열거형 정의

열거형은 값이 여러 개의 개별적인 가능성 중 하나일 수 있을 때 유용합니다. 예를 들어, IP 주소의 경우:

```rust
enum IpAddrKind {
    V4,
    V6,
}

fn main() {
    let four = IpAddrKind::V4;
    let six = IpAddrKind::V6;

    route(IpAddrKind::V4);
    route(IpAddrKind::V6);
}

fn route(ip_kind: IpAddrKind) {}
```

열거형 변형은 열거형의 식별자 아래에 네임스페이스가 지정되며 `::`를 사용하여 접근합니다. `IpAddrKind::V4`와 `IpAddrKind::V6`은 모두 같은 타입인 `IpAddrKind`입니다.

## 연관 데이터가 있는 열거형 값

열거형 변형과 연관 데이터를 보관하기 위해 구조체를 사용하는 대신:

```rust
fn main() {
    enum IpAddrKind {
        V4,
        V6,
    }

    struct IpAddr {
        kind: IpAddrKind,
        address: String,
    }

    let home = IpAddr {
        kind: IpAddrKind::V4,
        address: String::from("127.0.0.1"),
    };

    let loopback = IpAddr {
        kind: IpAddrKind::V6,
        address: String::from("::1"),
    };
}
```

데이터를 열거형 변형에 직접 첨부할 수 있습니다:

```rust
fn main() {
    enum IpAddr {
        V4(String),
        V6(String),
    }

    let home = IpAddr::V4(String::from("127.0.0.1"));
    let loopback = IpAddr::V6(String::from("::1"));
}
```

열거형 변형 이름은 자동으로 생성자 함수가 됩니다.

## 변형에서 다른 타입

각 열거형 변형은 서로 다른 타입과 양의 연관 데이터를 가질 수 있습니다:

```rust
fn main() {
    enum IpAddr {
        V4(u8, u8, u8, u8),
        V6(String),
    }

    let home = IpAddr::V4(127, 0, 0, 1);
    let loopback = IpAddr::V6(String::from("::1"));
}
```

## 복잡한 열거형 예제

열거형은 구조체를 포함한 다양한 데이터 타입을 보관할 수 있습니다:

```rust
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}

fn main() {}
```

이 열거형은 네 가지 변형을 가집니다:
- `Quit`: 연관 데이터 없음
- `Move`: 구조체처럼 명명된 필드
- `Write`: 단일 `String`
- `ChangeColor`: 세 개의 `i32` 값

## 열거형의 메서드

구조체처럼 `impl`을 사용하여 열거형에 메서드를 정의할 수 있습니다:

```rust
fn main() {
    enum Message {
        Quit,
        Move { x: i32, y: i32 },
        Write(String),
        ChangeColor(i32, i32, i32),
    }

    impl Message {
        fn call(&self) {
            // 메서드 본문은 여기에 정의됩니다
        }
    }

    let m = Message::Write(String::from("hello"));
    m.call();
}
```

## Option 열거형

표준 라이브러리의 `Option<T>` 열거형은 무언가이거나 아무것도 아닌 값을 나타냅니다:

```rust
#![allow(unused)]
fn main() {
enum Option<T> {
    None,
    Some(T),
}
}
```

`Option<T>`는 매우 유용해서 프렐루드에 포함되어 있어 `Option::` 접두사 없이 `Some`과 `None`을 직접 사용할 수 있습니다.

### Option 사용하기

```rust
fn main() {
    let some_number = Some(5);
    let some_char = Some('e');
    let absent_number: Option<i32> = None;
}
```

- `some_number`의 타입은 `Option<i32>`입니다
- `some_char`의 타입은 `Option<char>`입니다
- `absent_number`의 경우, 컴파일러가 `None`만으로는 타입을 추론할 수 없으므로 명시적인 타입 주석이 필요합니다

### 안전성 이점

다른 언어의 null과 달리, Rust는 `Option<T>`와 `T`를 다른 타입으로 취급합니다. 이는 null 관련 버그를 방지합니다:

```rust
fn main() {
    let x: i8 = 5;
    let y: Option<i8> = Some(5);

    let sum = x + y;  // 컴파일 오류!
}
```

이 코드는 컴파일에 실패합니다:
```
error[E0277]: cannot add `Option<i8>` to `i8`
```

**핵심 이점**: 작업을 수행하기 전에 `Option<T>`를 `T`로 명시적으로 변환해야 합니다. 이는 값이 `None`일 수 있을 때 유효하다고 가정하는 위험을 제거합니다. 컴파일러가 `None` 케이스를 명시적으로 처리하도록 강제합니다.

### 이것이 중요한 이유

- 변수가 `Option<T>`가 아니면 유효한 값이 있다고 안전하게 가정할 수 있습니다
- `Option<T>` 타입만 `None`일 수 있습니다
- 값을 사용하기 전에 `Some(T)`와 `None` 케이스를 모두 명시적으로 처리해야 합니다

`Option<T>`의 값을 추출하고 작업하려면 `match` 표현식을 사용합니다. 이는 가지고 있는 열거형 변형에 따라 다른 코드를 실행합니다.
