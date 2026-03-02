# 구조체 정의 및 인스턴스화

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
