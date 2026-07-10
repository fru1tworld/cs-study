# 소유권 이해하기

# 소유권 이해하기

> **원문:** https://doc.rust-lang.org/book/ch04-00-understanding-ownership.html

## 챕터 개요

**소유권**(Ownership)은 Rust의 가장 독특한 기능이며 언어의 나머지 부분에 깊은 영향을 미칩니다. 가비지 컬렉터 없이도 메모리 안전 보장을 가능하게 하므로 소유권이 어떻게 작동하는지 이해하는 것이 중요합니다.

## 다루는 주제

이 챕터에서 배울 내용:

1. **소유권** - Rust의 핵심 메모리 관리 시스템
2. **빌림(Borrowing)** - 데이터에 대한 참조를 빌려주는 방법
3. **슬라이스** - 연속된 요소 시퀀스를 참조하는 방법
4. **메모리 레이아웃** - Rust가 메모리에 데이터를 어떻게 구성하는지

## 핵심 개념

소유권은 Rust의 기본이 되는 개념입니다:

- 가비지 컬렉터의 필요성 제거
- 컴파일 타임 메모리 안전 보장 제공
- use-after-free와 double-free 오류 같은 일반적인 메모리 버그 방지
- 안전한 동시성 코드 가능

---

# 소유권이란?

> **원문:** https://doc.rust-lang.org/book/ch04-01-what-is-ownership.html

## 개요

**소유권**은 Rust 프로그램이 메모리를 관리하는 방법을 지배하는 규칙 집합입니다. 모든 프로그램은 실행 중에 메모리를 관리해야 합니다. 가비지 컬렉션이 있는 언어나 수동 할당/해제가 필요한 언어와 달리, Rust는 세 번째 접근법을 사용합니다: 컴파일러가 검사하는 소유권 시스템으로 런타임 성능 비용이 없습니다.

## 스택과 힙

둘 다 런타임에 사용할 수 있는 메모리의 일부지만 구조가 다릅니다:

### 스택

- **후입선출(LIFO)** 순서로 값을 저장
- 데이터 추가를 **스택에 푸시**라고 함
- 데이터 제거를 **스택에서 팝**이라고 함
- 모든 데이터는 **컴파일 타임에 알려진 고정 크기**여야 함
- 스택에 푸시하는 것이 **힙 할당보다 빠름**

### 힙

- 덜 조직적; 메모리 할당자가 빈 공간을 찾음
- 할당된 위치에 대한 **포인터**(주소)를 반환
- 이 과정을 **힙에 할당**이라고 함
- 스택 데이터보다 **접근이 느림** (포인터를 따라가야 함)
- **컴파일 타임에 크기를 알 수 없는** 또는 가변 크기의 데이터 저장

## 소유권 규칙

다음 세 가지 기본 규칙을 기억하세요:

1. **Rust의 각 값은 소유자가 있습니다**
2. **한 번에 하나의 소유자만 있을 수 있습니다**
3. **소유자가 스코프를 벗어나면 값이 삭제됩니다**

## 변수 스코프

**스코프**는 항목이 유효한 프로그램 내의 범위입니다:

```rust
fn main() {
    {                      // s는 여기서 유효하지 않음
        let s = "hello";   // s는 이 지점부터 유효함
        // s로 작업 수행
    }                      // 스코프 종료, s는 더 이상 유효하지 않음
}
```

- `s`가 스코프에 **들어오면** 유효합니다
- 스코프를 **벗어날** 때까지 유효합니다

## `String` 타입

소유권 규칙을 설명하기 위해 복잡한 데이터 타입이 필요합니다. `String` 타입은 힙에 저장되며 컴파일 타임에 텍스트 크기를 알 수 없을 때 적합합니다.

```rust
let s = String::from("hello");
```

문자열 리터럴과 달리 `String`은 변경할 수 있습니다:

```rust
fn main() {
    let mut s = String::from("hello");
    s.push_str(", world!"); // push_str()은 리터럴을 추가
    println!("{s}"); // 출력: hello, world!
}
```

## 메모리와 할당

**문자열 리터럴**은 컴파일 타임에 실행 파일에 하드코딩되어 빠르지만 불변입니다.

**String 타입**은 다음이 필요합니다:

- 런타임에 할당자로부터 메모리 할당
- 완료 시 메모리를 반환하는 방법

Rust의 해결책: 변수(소유자)가 스코프를 벗어나면 **`drop` 함수**를 통해 메모리가 자동으로 반환됩니다.

```rust
fn main() {
    {
        let s = String::from("hello");
        // s로 작업 수행
    }  // s가 스코프를 벗어남; drop이 호출됨; 메모리가 해제됨
}
```

이 패턴(수명이 끝날 때 리소스 해제)을 **RAII**(Resource Acquisition Is Initialization)라고 합니다.

## 이동(Move)과 변수 데이터 상호작용

### 스택 데이터(정수)

```rust
fn main() {
    let x = 5;
    let y = x;  // x가 복사됨; 둘 다 유효함
}
```

정수는 알려진 고정 크기를 가지며 간단하게 복사됩니다.

### 힙 데이터(String)

```rust
fn main() {
    let s1 = String::from("hello");
    let s2 = s1;  // s1이 s2로 이동됨
}
```

`String`은 다음으로 구성됩니다:

- 힙 메모리에 대한 **포인터**
- **길이** (현재 사용 중인 바이트)
- **용량** (할당된 총 바이트)

`s2 = s1`이 실행될 때:

- 스택 데이터만 복사됨 (포인터, 길이, 용량)
- 힙 데이터는 복사되지 **않음**
- 이중 해제 오류를 방지하기 위해 `s1`은 **무효화**됨

이동 후 `s1`을 사용하려고 하면 컴파일러 오류가 발생합니다:

```rust
fn main() {
    let s1 = String::from("hello");
    let s2 = s1;
    println!("{s1}, world!");  // 오류: borrow of moved value
}
```

오류 메시지: `error[E0382]: borrow of moved value: s1`

이 연산을 **이동**(move)이라고 합니다 (첫 번째 변수가 무효화되므로 얕은 복사가 아님).

## 스코프와 할당

새 값이 기존 변수에 할당되면 원래 값의 메모리가 즉시 해제됩니다:

```rust
fn main() {
    let mut s = String::from("hello");
    s = String::from("ahoy");  // "hello"가 삭제됨; 메모리 해제
    println!("{s}, world!");   // 출력: ahoy, world!
}
```

## 클론(Clone)과 변수 데이터 상호작용

힙 데이터를 명시적으로 **깊은 복사**하려면 (스택 데이터만이 아닌):

```rust
fn main() {
    let s1 = String::from("hello");
    let s2 = s1.clone();  // 힙 데이터의 깊은 복사
    println!("s1 = {s1}, s2 = {s2}");  // 둘 다 유효
}
```

`clone()`을 보면 임의의 코드가 실행되고 있으며 성능에 비용이 들 수 있음을 알 수 있습니다.

## 스택 전용 데이터: Copy

`Copy` 트레이트는 스택에 전적으로 저장된 타입이 원본을 무효화하지 않고 간단하게 복사되도록 합니다:

```rust
fn main() {
    let x = 5;
    let y = x;
    println!("x = {x}, y = {y}");  // 둘 다 유효; 이동 없음
}
```

**`Copy`를 구현하는 타입:**

- 모든 정수 타입 (`u32`, `i64` 등)
- 불리언 타입 (`bool`)
- 모든 부동 소수점 타입 (`f64`, `f32` 등)
- 문자 타입 (`char`)
- `Copy` 타입만 포함하는 튜플: `(i32, i32)` O, `(i32, String)` X

**참고:** 타입 또는 그 일부가 `Drop`을 구현하면 `Copy`를 구현할 수 없습니다.

## 소유권과 함수

함수에 값을 전달하는 것은 할당과 같은 이동/복사 의미를 따릅니다:

```rust
fn main() {
    let s = String::from("hello");
    takes_ownership(s);  // s의 값이 이동함; 이후 s 무효

    let x = 5;
    makes_copy(x);       // x가 복사됨; x 여전히 유효 (Copy 구현)
}

fn takes_ownership(some_string: String) {
    println!("{some_string}");
}  // drop 호출됨; 메모리 해제

fn makes_copy(some_integer: i32) {
    println!("{some_integer}");
}  // 특별한 일 없음
```

## 반환 값과 스코프

값을 반환하는 것도 소유권을 이전합니다:

```rust
fn main() {
    let s1 = gives_ownership();
    let s2 = String::from("hello");
    let s3 = takes_and_gives_back(s2);
}  // s3 삭제, s2는 이동됨(아무 일 없음), s1 삭제

fn gives_ownership() -> String {
    let some_string = String::from("yours");
    some_string  // 호출자에게 이동
}

fn takes_and_gives_back(a_string: String) -> String {
    a_string  // 호출자에게 이동
}
```

패턴은 다음과 같습니다: **다른 변수에 값을 할당하면 이동합니다.** 힙 변수가 스코프를 벗어나고 소유권이 이동되지 않았으면 `drop`이 메모리를 정리합니다.

### 번거로움 문제

이 소유권 패턴은 작동하지만 번거롭습니다—모든 함수마다 소유권을 반환하는 것은 지루합니다. Rust는 해결책을 제공합니다: **참조** (다음 챕터에서 다룸).

```rust
fn main() {
    let s1 = String::from("hello");
    let (s2, len) = calculate_length(s1);
    println!("The length of '{s2}' is {len}.");
}

fn calculate_length(s: String) -> (String, usize) {
    let length = s.len();
    (s, length)
}
```

그러나 이 목적을 위해 튜플을 반환하는 것은 의례적입니다. **참조**는 소유권을 이전하지 않고 값을 사용할 수 있게 해줍니다—더 우아한 해결책입니다.

---

# 참조와 빌림

> **원문:** https://doc.rust-lang.org/book/ch04-02-references-and-borrowing.html

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

---

# 슬라이스 타입

> **원문:** https://doc.rust-lang.org/book/ch04-03-slices.html

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
