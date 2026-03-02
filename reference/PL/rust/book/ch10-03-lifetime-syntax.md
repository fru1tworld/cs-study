# 라이프타임으로 참조 유효성 검증하기

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
