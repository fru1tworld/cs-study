# 참조와 빌림

## 개요

참조는 소유권을 가져가지 않고 값을 참조할 수 있게 해줍니다. 참조는 해당 참조의 수명 동안 특정 타입의 유효한 값을 가리키도록 보장되는 포인터와 같습니다.

## 기본 참조

소유권을 이전하는 대신 값에 대한 참조를 전달할 수 있습니다:

```rust
fn main() {
    let s1 = String::from("hello");

    let len = calculate_length(&s1);

    println!("The length of '{s1}' is {len}.");
}

fn calculate_length(s: &String) -> usize {
    s.len()
}
```

`&s1` 문법은 `s1`의 값을 소유하지 않고 참조하는 참조를 생성합니다. 참조가 스코프를 벗어나면 참조가 소유권을 가지지 않기 때문에 값이 삭제되지 않습니다.

**핵심 포인트:** 이 과정을 *빌림(borrowing)*이라고 합니다. 빌린 것을 다 쓰면 소유하지 않고 돌려줍니다.

## 빌린 값 수정 시도

기본적으로 참조는 불변입니다:

```rust
fn main() {
    let s = String::from("hello");

    change(&s);
}

fn change(some_string: &String) {
    some_string.push_str(", world"); // 오류!
}
```

실패 메시지: `error[E0596]: cannot borrow '*some_string' as mutable, as it is behind a '&' reference`

## 가변 참조

빌린 값을 수정하려면 가변 참조를 사용하세요:

```rust
fn main() {
    let mut s = String::from("hello");

    change(&mut s);
}

fn change(some_string: &mut String) {
    some_string.push_str(", world");
}
```

### 가변 참조 제한

**한 번에 값에 대한 가변 참조는 하나만 가질 수 있습니다:**

```rust
fn main() {
    let mut s = String::from("hello");

    let r1 = &mut s;
    let r2 = &mut s; // 오류!

    println!("{r1}, {r2}");
}
```

오류: `error[E0499]: cannot borrow 's' as mutable more than once at a time`

다른 스코프를 사용하여 여러 가변 참조를 생성할 수 있습니다:

```rust
fn main() {
    let mut s = String::from("hello");

    {
        let r1 = &mut s;
    } // r1이 여기서 스코프를 벗어남

    let r2 = &mut s; // OK
}
```

## 가변 참조와 불변 참조 혼합

불변 참조가 존재하는 동안 가변 참조를 가질 수 없습니다:

```rust
fn main() {
    let mut s = String::from("hello");

    let r1 = &s;  // OK
    let r2 = &s;  // OK
    let r3 = &mut s; // 오류!

    println!("{r1}, {r2}, and {r3}");
}
```

오류: `error[E0502]: cannot borrow 's' as mutable because it is also borrowed as immutable`

그러나 불변 참조가 더 이상 사용되지 않으면 작동합니다:

```rust
fn main() {
    let mut s = String::from("hello");

    let r1 = &s;
    let r2 = &s;
    println!("{r1} and {r2}");
    // r1과 r2는 더 이상 사용되지 않음

    let r3 = &mut s; // OK
    println!("{r3}");
}
```

**참조 스코프**는 도입된 곳에서 시작하여 마지막 사용 시점에서 끝납니다.

## 댕글링 참조

Rust는 컴파일 타임에 댕글링 참조(잘못된 메모리에 대한 참조)를 방지합니다:

```rust
fn main() {
    let reference_to_nothing = dangle();
}

fn dangle() -> &String { // 오류!
    let s = String::from("hello");

    &s // s가 여기서 삭제되어 참조가 무효화됨
}
```

오류: `error[E0106]: missing lifetime specifier` 및 메시지: `this function's return type contains a borrowed value, but there is no value for it to be borrowed from`

**해결책:** 소유된 값을 직접 반환:

```rust
fn main() {
    let string = no_dangle();
}

fn no_dangle() -> String {
    let s = String::from("hello");

    s // 소유권이 밖으로 이동
}
```

## 참조의 규칙

1. **주어진 시점에 다음 중 하나만 가질 수 있습니다:**
   - 하나의 가변 참조, 또는
   - 임의 개수의 불변 참조

2. **참조는 항상 유효해야 합니다**

이러한 규칙은 컴파일 타임에 데이터 경쟁을 방지하고 메모리 안전을 보장합니다.
