# 슬라이스 타입

*슬라이스*는 컬렉션에서 연속된 요소 시퀀스를 참조할 수 있게 해줍니다. 슬라이스는 일종의 참조이므로 소유권이 없습니다.

### 문제 제시

공백으로 구분된 단어로 구성된 문자열을 받아 찾은 첫 번째 단어를 반환하는 함수를 작성하세요. 문자열에서 공백을 찾지 못하면 전체 문자열이 하나의 단어이므로 전체 문자열을 반환해야 합니다.

**참고:** 슬라이스를 소개하기 위해 이 섹션에서는 ASCII만 가정합니다. UTF-8 처리에 대한 더 자세한 논의는 8장에 있습니다.

### 슬라이스 없이 초기 접근법

슬라이스 없이 함수 시그니처는 다음과 같을 것입니다:

```rust
fn first_word(s: &String) -> ?
```

무엇을 반환할지 결정하는 것이 과제입니다. 가능한 해결책은 단어 끝의 바이트 인덱스를 반환하는 것입니다:

```rust
fn first_word(s: &String) -> usize {
    let bytes = s.as_bytes();

    for (i, &item) in bytes.iter().enumerate() {
        if item == b' ' {
            return i;
        }
    }

    s.len()
}

fn main() {}
```

**Listing 4-7:** 바이트 인덱스 값을 반환하는 `first_word` 함수

#### 이 접근법의 문제

`String`과 별개로 `usize`를 반환하는 것은 문제가 있습니다. 인덱스는 `String`의 맥락에서만 의미가 있지만, 유효성이 유지된다는 보장이 없습니다:

```rust
fn first_word(s: &String) -> usize {
    let bytes = s.as_bytes();

    for (i, &item) in bytes.iter().enumerate() {
        if item == b' ' {
            return i;
        }
    }

    s.len()
}

fn main() {
    let mut s = String::from("hello world");

    let word = first_word(&s); // word는 값 5를 얻음

    s.clear(); // String을 비워서 ""과 같게 만듦

    // word는 여전히 값 5를 가지지만, s는 더 이상 값 5를 의미 있게
    // 사용할 수 있는 내용이 없으므로 word는 이제 완전히 무효!
}
```

**Listing 4-8:** 결과를 저장한 다음 `String` 내용을 변경

이 프로그램은 오류 없이 컴파일되지만, `s.clear()`가 호출된 후 `word`는 무효화됩니다. 동기화되어야 하는 여러 관련 없는 인덱스를 관리하는 것은 오류가 발생하기 쉽고 취약합니다.

---

## 문자열 슬라이스

*문자열 슬라이스*는 `String`의 연속된 요소 시퀀스에 대한 참조입니다:

```rust
fn main() {
    let s = String::from("hello world");

    let hello = &s[0..5];
    let world = &s[6..11];
}
```

슬라이스는 대괄호 안의 범위를 사용하여 생성됩니다: `[시작_인덱스..끝_인덱스]`, 여기서:

- `시작_인덱스`는 슬라이스의 첫 번째 위치
- `끝_인덱스`는 슬라이스의 마지막 위치보다 하나 큼

내부적으로 슬라이스 데이터 구조는 시작 위치와 슬라이스 길이(끝_인덱스 - 시작_인덱스)를 저장합니다.

### 슬라이스 문법 단축

**인덱스 0에서 시작:**

```rust
fn main() {
    let s = String::from("hello");

    let slice = &s[0..2];
    let slice = &s[..2];  // 동일함
}
```

**마지막 바이트 포함:**

```rust
fn main() {
    let s = String::from("hello");
    let len = s.len();

    let slice = &s[3..len];
    let slice = &s[3..];  // 동일함
}
```

**전체 문자열:**

```rust
fn main() {
    let s = String::from("hello");
    let len = s.len();

    let slice = &s[0..len];
    let slice = &s[..];  // 동일함
}
```

**참고:** 문자열 슬라이스 범위 인덱스는 유효한 UTF-8 문자 경계에서 발생해야 합니다.

### 슬라이스로 `first_word` 다시 작성하기

```rust
fn first_word(s: &String) -> &str {
    let bytes = s.as_bytes();

    for (i, &item) in bytes.iter().enumerate() {
        if item == b' ' {
            return &s[0..i];
        }
    }

    &s[..]
}

fn main() {}
```

이제 `first_word`는 기본 데이터와 연결된 `&str`(문자열 슬라이스)을 반환합니다.

### 컴파일 타임 오류 방지

슬라이스를 사용하면 Listing 4-8의 버그를 컴파일 타임에 방지합니다:

```rust
fn first_word(s: &String) -> &str {
    let bytes = s.as_bytes();

    for (i, &item) in bytes.iter().enumerate() {
        if item == b' ' {
            return &s[0..i];
        }
    }

    &s[..]
}

fn main() {
    let mut s = String::from("hello world");

    let word = first_word(&s);

    s.clear(); // 오류!

    println!("the first word is: {word}");
}
```

**컴파일러 오류:**

```text
error[E0502]: cannot borrow `s` as mutable because it is also borrowed as immutable
  --> src/main.rs:18:5
   |
16 |     let word = first_word(&s);
   |                           -- immutable borrow occurs here
18 |     s.clear(); // error!
   |     ^^^^^^^^^ mutable borrow occurs here
20 |     println!("the first word is: {word}");
   |                                   ---- immutable borrow later used here
```

컴파일러는 `word`의 불변 빌림과 동시에 `clear()`의 가변 빌림이 존재하는 것을 방지합니다.

---

## 슬라이스로서의 문자열 리터럴

문자열 리터럴은 바이너리 안에 저장되며 `&str` 타입을 가집니다:

```rust
fn main() {
    let s = "Hello, world!";
}
```

`s`의 타입은 `&str`입니다: 바이너리의 특정 지점을 가리키는 슬라이스. 이것이 문자열 리터럴이 불변인 이유입니다.

---

## 매개변수로서의 문자열 슬라이스

더 관용적인 접근법은 `&String` 대신 `&str`을 매개변수 타입으로 사용하는 것입니다:

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

fn main() {
    let my_string = String::from("hello world");

    // `first_word`는 `String`의 슬라이스에 작동, 부분적이든 전체든
    let word = first_word(&my_string[0..6]);
    let word = first_word(&my_string[..]);
    // `first_word`는 `String`의 참조에도 작동, 이는 `String`의
    // 전체 슬라이스와 동등함
    let word = first_word(&my_string);

    let my_string_literal = "hello world";

    // `first_word`는 문자열 리터럴의 슬라이스에 작동, 부분적이든
    // 전체든
    let word = first_word(&my_string_literal[0..6]);
    let word = first_word(&my_string_literal[..]);

    // 문자열 리터럴은 *이미* 문자열 슬라이스이므로,
    // 슬라이스 문법 없이도 작동!
    let word = first_word(my_string_literal);
}
```

**Listing 4-9:** 문자열 슬라이스를 사용하여 `first_word` 함수 개선

`&str`을 사용하면 역참조 강제 변환(15장에서 다룸)을 통해 모든 기능을 유지하면서 API를 더 일반적이고 유용하게 만듭니다.

---

## 다른 슬라이스

슬라이스는 배열에서도 작동합니다:

```rust
fn main() {
    let a = [1, 2, 3, 4, 5];

    let slice = &a[1..3];

    assert_eq!(slice, &[2, 3]);
}
```

이 슬라이스는 `&[i32]` 타입을 가지며 문자열 슬라이스와 같은 방식으로 작동합니다—첫 번째 요소에 대한 참조와 길이를 저장합니다.

---

## 요약

소유권, 빌림, 슬라이스의 개념은 Rust 프로그램에서 컴파일 타임에 메모리 안전을 보장합니다. Rust는 다른 시스템 프로그래밍 언어처럼 메모리 사용에 대한 제어를 제공하지만, 데이터 소유자가 스코프를 벗어나면 자동으로 정리하여 추가 정리 코드가 필요 없습니다. 이러한 개념은 Rust의 많은 다른 부분에 영향을 미치며 책 전체에서 논의될 것입니다.
