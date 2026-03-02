# 소유권이란?

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

이 패턴(수명이 끝날 때 리소스 해제)을 **RAII(Resource Acquisition Is Initialization)**라고 합니다.

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

이 연산을 **이동(move)**이라고 합니다 (첫 번째 변수가 무효화되므로 얕은 복사가 아님).

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

이 소유권 패턴은 작동하지만 번거롭습니다—모든 함수에 대해 소유권을 반환하는 것은 지루합니다. Rust는 해결책을 제공합니다: **참조** (다음 챕터에서 다룸).

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
