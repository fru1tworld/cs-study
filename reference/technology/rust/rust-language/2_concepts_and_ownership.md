# 러스트 일반적인 프로그래밍 개념과 소유권

## 일반적인 프로그래밍 개념

## 일반적인 프로그래밍 개념

> 원문: https://doc.rust-lang.org/book/ch03-00-common-programming-concepts.html

### 챕터 개요

거의 모든 프로그래밍 언어에 나타나는 기본 프로그래밍 개념과 이 개념들이 Rust에서 어떻게 작동하는지 다룸. 이러한 개념 중 Rust에 고유한 것은 없음 → Rust의 맥락에서 관례와 사용법 논의.

### 다루는 주제

이 챕터가 초점을 맞추는 기초 개념:

- 변수
- 기본 타입
- 함수
- 주석
- 제어 흐름

### 핵심 포인트

- 모든 Rust 프로그램의 기초: 이러한 개념은 모든 Rust 프로그램에 나타날 기초 형성 → 일찍 배우면 강력한 핵심 기반 제공
- 언어 간 공통점: 많은 프로그래밍 언어가 기초에서 이러한 핵심 개념 공유, 구체적인 구현은 다를 수 있음
- 키워드: Rust 언어에는 변수나 함수의 이름으로 사용할 수 없는 예약된 키워드 존재
  - 대부분의 키워드는 특별한 의미를 가지며 Rust 프로그램에서 다양한 작업에 사용
  - 일부 키워드는 향후 기능을 위해 예약
  - 키워드 전체 목록은 부록 A 참조

### 탐색

- 이전 챕터: 추리 게임 튜토리얼 (챕터 2)
- 다음 챕터: 변수와 가변성 (챕터 3)

---

## 변수와 가변성

> 원문: https://doc.rust-lang.org/book/ch03-01-variables-and-mutability.html

### 개요

기본적으로 Rust에서 변수는 불변(immutable)임 → 값이 이름에 바인딩되면 그 값 변경 불가. Rust가 제공하는 안전 기능 중 하나 → 필요할 때 변수를 가변으로 만들 수 있는 옵션도 존재.

### 불변 변수

`let`으로 변수를 선언하면 기본적으로 불변:

```rust
fn main() {
    let x = 5;
    println!("The value of x is: {x}");
    x = 6;  // 컴파일 오류 발생
    println!("The value of x is: {x}");
}
```

컴파일 타임 오류 발생:

```text
error[E0384]: cannot assign twice to immutable variable `x`
```

컴파일러가 우발적인 변경을 방지 → 버그를 일찍 잡는 데 도움. 코드의 한 부분이 값이 변경되지 않을 것이라고 가정하고 다른 부분이 그 값을 변경하면 → 컴파일러가 런타임 버그 대신 컴파일 타임에 이 문제를 잡아냄.

### 변수를 가변으로 만들기

변수를 가변으로 만들려면 `mut` 키워드 추가:

```rust
fn main() {
    let mut x = 5;
    println!("The value of x is: {x}");
    x = 6;
    println!("The value of x is: {x}");
}
```

출력:

```text
The value of x is: 5
The value of x is: 6
```

`mut` 사용 → 코드를 읽는 다른 개발자에게 의도 전달도 됨.

### 상수 선언하기

상수는 불변 변수와 유사하지만 중요한 차이점 존재:

```rust
const THREE_HOURS_IN_SECONDS: u32 = 60 * 60 * 3;
```

주요 차이점:

- 상수에는 `mut` 사용 불가 — 항상 불변
- `let` 대신 `const` 키워드로 선언
- 타입 어노테이션 필수
- 전역 스코프를 포함한 모든 스코프에서 선언 가능
- 런타임 값이 아닌 상수 표현식으로만 설정 가능
- 해당 스코프 내에서 프로그램 전체 수명 동안 유효

명명 규칙: 상수는 ALL_UPPERCASE_WITH_UNDERSCORES 사용.

### 섀도잉

이전 변수와 같은 이름으로 새 변수 선언 가능 → 새 변수가 이전 변수를 "섀도잉":

```rust
fn main() {
    let x = 5;

    let x = x + 1;  // x는 이제 6

    {
        let x = x * 2;  // x는 이제 12 (내부 스코프)
        println!("The value of x in the inner scope is: {x}");
    }

    println!("The value of x is: {x}");  // x는 다시 6
}
```

출력:

```text
The value of x in the inner scope is: 12
The value of x is: 6
```

#### 섀도잉 vs 가변성

핵심 차이점: 섀도잉은 새 변수를 생성 → `mut`는 같은 변수의 재할당 허용.

섀도잉의 경우, `let`을 잊으면 컴파일 오류 발생:

```rust
let x = 5;
x = 6;  // 오류: cannot assign twice to immutable variable
```

섀도잉은 타입 변경 허용, `mut`는 그렇지 않음:

```rust
// 작동함 - 섀도잉은 타입 변경 허용
let spaces = "   ";
let spaces = spaces.len();  // 문자열을 숫자로 변환

// 실패함 - mut는 타입 변경을 허용하지 않음
let mut spaces = "   ";
spaces = spaces.len();  // 오류: mismatched types
```

오류:

```text
error[E0308]: mismatched types
expected `&str`, found `usize`
```

섀도잉 → 값을 변환하면서 변환 후에도 불변으로 유지하고 싶을 때 유용.

---

## 데이터 타입

> 원문: https://doc.rust-lang.org/book/ch03-02-data-types.html

Rust의 모든 값은 특정 데이터 타입을 가짐 → Rust에게 어떤 종류의 데이터가 지정되었는지 알려주어 해당 데이터를 어떻게 다룰지 알 수 있게 함. 스칼라와 복합이라는 두 가지 데이터 타입 하위 집합 확인.

Rust는 정적 타입 언어 → 컴파일 타임에 모든 변수의 타입을 알아야 함. 컴파일러는 보통 값과 사용 방법에 따라 사용하고자 하는 타입 추론 가능. `String`을 `parse`로 숫자 타입으로 변환할 때처럼 여러 타입이 가능한 경우 → 타입 어노테이션 추가 필요.

타입 어노테이션이 필요한 예:

```rust
let guess: u32 = "42".parse().expect("Not a number!");
```

`: u32` 어노테이션 없이는 컴파일러가 타입 어노테이션이 필요하다는 오류 표시.

---

### 스칼라 타입

스칼라 타입은 단일 값을 나타냄. Rust의 네 가지 주요 스칼라 타입: 정수, 부동 소수점 숫자, 불리언, 문자.

#### 정수 타입

정수는 소수 부분이 없는 숫자.

Rust의 정수 타입:

- 8비트: 부호 있음 `i8` · 부호 없음 `u8`
- 16비트: 부호 있음 `i16` · 부호 없음 `u16`
- 32비트: 부호 있음 `i32` · 부호 없음 `u32`
- 64비트: 부호 있음 `i64` · 부호 없음 `u64`
- 128비트: 부호 있음 `i128` · 부호 없음 `u128`
- 아키텍처 의존: 부호 있음 `isize` · 부호 없음 `usize`

부호 있음과 부호 없음은 숫자가 음수가 될 수 있는지 여부를 나타냄. 부호 있는 숫자는 2의 보수 표현으로 저장.

- 부호 있는 각 변형은 -(2^(n-1))에서 2^(n-1) - 1까지의 숫자 저장 가능
- 부호 없는 변형은 0에서 2^n - 1까지의 숫자 저장 가능
- `isize`와 `usize`는 컴퓨터 아키텍처에 따라 달라짐 (64비트 시스템에서는 64비트, 32비트 시스템에서는 32비트)

Rust의 정수 리터럴:

- 십진수: `98_222`
- 16진수: `0xff`
- 8진수: `0o77`
- 이진수: `0b1111_0000`
- 바이트 (`u8`만): `b'A'`

숫자 리터럴은 `_`를 시각적 구분자로 사용 가능. 예: `1_000` (`1000`과 동일).

정수 타입은 기본적으로 `i32`임. 컬렉션 인덱싱 시 주로 `isize` 또는 `usize` 사용.

##### 정수 오버플로

정수 오버플로는 변수가 보유할 수 있는 범위를 초과할 때 발생. 동작은 컴파일 모드에 따라 다름:

- 디버그 모드: Rust는 런타임에 프로그램을 패닉시키는 검사 포함
- 릴리스 모드 (`--release` 플래그 사용): Rust는 2의 보수 래핑 수행 → 값이 범위의 최소값으로 래핑

오버플로를 명시적으로 처리하려면 표준 라이브러리가 기본 숫자 타입에 다음 메서드 계열 제공:

- `wrapping_*` 메서드 (예: `wrapping_add`) - 모든 모드에서 래핑
- `checked_*` 메서드 - 오버플로 발생 시 `None` 반환
- `overflowing_*` 메서드 - 값과 오버플로 여부를 나타내는 불리언 반환
- `saturating_*` 메서드 - 최소값 또는 최대값에서 포화

#### 부동 소수점 타입

Rust의 부동 소수점 숫자를 위한 두 가지 기본 타입: `f32`와 `f64`, 각각 32비트와 64비트 크기. 현대 CPU에서 `f64`가 `f32`와 거의 같은 속도지만 더 정밀 → 기본 타입은 `f64`. 모든 부동 소수점 타입은 부호 있음.

부동 소수점 숫자는 IEEE-754 표준에 따라 표현.

예:

```rust
fn main() {
    let x = 2.0; // f64

    let y: f32 = 3.0; // f32
}
```

#### 숫자 연산

Rust는 모든 숫자 타입에 기본 수학 연산 지원: 덧셈, 뺄셈, 곱셈, 나눗셈, 나머지. 정수 나눗셈은 0 방향으로 가장 가까운 정수로 잘림.

```rust
fn main() {
    // 덧셈
    let sum = 5 + 10;

    // 뺄셈
    let difference = 95.5 - 4.3;

    // 곱셈
    let product = 4 * 30;

    // 나눗셈
    let quotient = 56.7 / 32.2;
    let truncated = -5 / 3; // 결과는 -1

    // 나머지
    let remainder = 43 % 5;
}
```

#### 불리언 타입

Rust의 불리언 타입은 `true`와 `false` 두 가지 값을 가짐. 불리언은 1바이트 크기. 불리언 타입은 `bool`로 지정.

```rust
fn main() {
    let t = true;

    let f: bool = false; // 명시적 타입 어노테이션 사용
}
```

불리언 값은 주로 `if` 표현식과 같은 조건문에서 사용됨.

#### 문자 타입

Rust의 `char` 타입은 언어에서 가장 기본적인 알파벳 타입. `char` 리터럴은 큰따옴표를 사용하는 문자열 리터럴과 달리 작은따옴표로 지정.

```rust
fn main() {
    let c = 'z';
    let z: char = 'ℤ'; // 명시적 타입 어노테이션 사용
    let heart_eyed_cat = '😻';
}
```

Rust의 `char` 타입은 4바이트 크기이며 유니코드 스칼라 값을 나타냄. 다음을 표현 가능:

- 악센트 문자
- 중국어, 일본어, 한국어 문자
- 이모지
- 제로 폭 공백
- `U+0000`에서 `U+D7FF` 및 `U+E000`에서 `U+10FFFF`까지의 유니코드 스칼라 값

---

### 복합 타입

복합 타입은 여러 값을 하나의 타입으로 그룹화. Rust의 두 가지 기본 복합 타입: 튜플과 배열.

#### 튜플 타입

튜플은 다양한 타입의 여러 값을 하나의 복합 타입으로 그룹화하는 일반적인 방법. 튜플은 고정된 길이를 가짐 → 한 번 선언되면 늘어나거나 줄어들 수 없음.

튜플 생성:

```rust
fn main() {
    let tup: (i32, f64, u8) = (500, 6.4, 1);
}
```

튜플 구조 분해:

```rust
fn main() {
    let tup = (500, 6.4, 1);

    let (x, y, z) = tup;

    println!("The value of y is: {y}");
}
```

인덱스로 튜플 요소 접근:

```rust
fn main() {
    let x: (i32, f64, u8) = (500, 6.4, 1);

    let five_hundred = x.0;

    let six_point_four = x.1;

    let one = x.2;
}
```

튜플의 첫 번째 인덱스는 0임.

유닛 타입:

값이 없는 튜플은 유닛이라는 특별한 이름을 가짐. 이 값과 해당 타입은 모두 `()`로 작성 → 빈 값 또는 빈 반환 타입 나타냄. 표현식이 다른 값을 반환하지 않으면 암묵적으로 유닛 값 반환.

#### 배열 타입

배열은 여러 값의 컬렉션을 가지는 또 다른 방법. 튜플과 달리:

- 배열의 모든 요소는 같은 타입이어야 함
- Rust의 배열은 고정된 길이를 가짐

```rust
fn main() {
    let a = [1, 2, 3, 4, 5];
}
```

배열이 유용한 경우:

- 스택에 데이터를 할당하고 싶을 때
- 항상 고정된 수의 요소를 갖도록 하고 싶을 때

늘어나거나 줄어들 수 있는 유연한 컬렉션이 필요하면 대신 벡터 사용 (8장에서 논의).

배열 타입 어노테이션:

```rust
let a: [i32; 5] = [1, 2, 3, 4, 5];
```

여기서 `i32`는 요소 타입, `5`는 요소 수.

같은 값으로 배열 초기화:

```rust
let a = [3; 5];
```

5개의 요소를 가진 배열 생성, 모두 `3`으로 초기화 (`[3, 3, 3, 3, 3]`과 동일).

#### 배열 요소 접근

인덱싱으로 배열 요소 접근:

```rust
fn main() {
    let a = [1, 2, 3, 4, 5];

    let first = a[0];
    let second = a[1];
}
```

#### 잘못된 배열 요소 접근

배열의 끝을 넘어 요소에 접근하려고 하면 Rust는 패닉:

```rust
use std::io;

fn main() {
    let a = [1, 2, 3, 4, 5];

    println!("Please enter an array index.");

    let mut index = String::new();

    io::stdin()
        .read_line(&mut index)
        .expect("Failed to read line");

    let index: usize = index
        .trim()
        .parse()
        .expect("Index entered was not a number");

    let element = a[index];

    println!("The value of the element at index {index} is: {element}");
}
```

5개 요소 배열에서 인덱스 10에 접근할 때 패닉 출력 예:

```text
thread 'main' panicked at src/main.rs:19:19:
index out of bounds: the len is 5 but the index is 10
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

Rust는 런타임에 인덱스가 배열 길이보다 작은지 확인 → 같거나 크면 패닉. 많은 저수준 언어에서 발생하는 잘못된 메모리 접근을 방지하는 메모리 안전 원칙임.

---

## 함수

> 원문: https://doc.rust-lang.org/book/ch03-03-how-functions-work.html

함수는 Rust 코드에서 광범위하게 사용됨. 언어에서 가장 중요한 함수 중 하나인 `main` 함수 → 많은 프로그램의 진입점. 새로운 함수를 선언할 수 있게 해주는 `fn` 키워드도 확인.

Rust 코드는 함수와 변수 이름에 스네이크 케이스를 관례적인 스타일로 사용 → 모든 문자가 소문자이고 밑줄이 단어를 구분. 함수 정의 예제가 포함된 프로그램:

파일명: src/main.rs

```rust
fn main() {
    println!("Hello, world!");

    another_function();
}

fn another_function() {
    println!("Another function.");
}
```

Rust에서 함수를 정의하려면 `fn` 다음에 함수 이름과 괄호 세트 입력. 중괄호는 컴파일러에게 함수 본문이 시작하고 끝나는 위치를 알려줌.

정의한 함수는 이름 뒤에 괄호를 입력하여 호출 가능. `another_function`이 프로그램에 정의되어 있으므로 `main` 함수 내부에서 호출 가능. 소스 코드에서 `main` 함수 뒤에 `another_function`을 정의했지만, 앞에 정의해도 됨 → Rust는 함수가 어디에 정의되어 있는지 상관하지 않음, 호출자가 볼 수 있는 스코프 어딘가에 정의되어 있기만 하면 됨.

함수를 더 탐구하기 위해 functions라는 새 바이너리 프로젝트 시작. `another_function` 예제를 src/main.rs에 넣고 실행하면 다음과 같은 출력 표시:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.28s
     Running `target/debug/functions`
Hello, world!
Another function.
```

줄은 `main` 함수에 나타나는 순서대로 실행. 먼저 "Hello, world!" 메시지가 출력되고, 그 다음 `another_function`이 호출되어 그 메시지가 출력됨.

#### 매개변수

함수가 매개변수를 갖도록 정의 가능. 매개변수는 함수 시그니처의 일부인 특수 변수. 함수에 매개변수가 있으면 해당 매개변수에 구체적인 값 제공 가능. 기술적으로 구체적인 값을 인수라고 하지만, 일상적인 대화에서는 함수 정의의 변수나 호출 시 전달되는 구체적인 값 모두에 매개변수와 인수라는 단어를 혼용하는 경향 있음.

이 버전의 `another_function`에서는 매개변수 추가:

파일명: src/main.rs

```rust
fn main() {
    another_function(5);
}

fn another_function(x: i32) {
    println!("The value of x is: {x}");
}
```

이 프로그램을 실행하면 다음과 같은 출력 표시:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 1.21s
     Running `target/debug/functions`
The value of x is: 5
```

`another_function` 선언에는 `x`라는 매개변수 하나 존재. `x`의 타입은 `i32`로 지정. `another_function`에 `5`를 전달하면 `println!` 매크로가 포맷 문자열에서 `x`를 포함하는 중괄호 쌍이 있던 자리에 `5`를 넣음.

함수 시그니처에서 각 매개변수의 타입을 반드시 선언해야 함 → Rust 설계에서 의도적인 결정. 함수 정의에서 타입 어노테이션을 요구하면 컴파일러가 코드의 다른 곳에서 어떤 타입을 의미하는지 파악하기 위해 타입 어노테이션을 사용할 필요가 거의 없음. 또한 컴파일러가 함수가 기대하는 타입을 알면 더 유용한 오류 메시지 제공 가능.

여러 매개변수를 정의할 때는 다음과 같이 매개변수 선언을 쉼표로 구분:

파일명: src/main.rs

```rust
fn main() {
    print_labeled_measurement(5, 'h');
}

fn print_labeled_measurement(value: i32, unit_label: char) {
    println!("The measurement is: {value}{unit_label}");
}
```

이 예제는 두 개의 매개변수를 가진 `print_labeled_measurement`라는 함수 생성. 첫 번째 매개변수는 `value`이고 `i32`. 두 번째는 `unit_label`이고 `char` 타입. 함수는 `value`와 `unit_label` 둘 다 포함하는 텍스트 출력.

이 코드를 실행. 현재 functions 프로젝트의 src/main.rs 파일에 있는 프로그램을 위의 예제로 교체하고 `cargo run`으로 실행:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.31s
     Running `target/debug/functions`
The measurement is: 5h
```

`value`에 `5`를, `unit_label`에 `'h'`를 넣어 함수를 호출 → 프로그램 출력에 해당 값들이 포함됨.

#### 구문과 표현식

함수 본문은 선택적으로 표현식으로 끝나는 일련의 구문으로 구성. 지금까지 다룬 함수에는 끝나는 표현식이 포함되지 않았지만, 구문의 일부로서 표현식은 확인. Rust는 표현식 기반 언어 → 이해해야 할 중요한 구분. 다른 언어에는 같은 구분이 없음 → 구문과 표현식이 무엇인지, 그리고 그 차이가 함수 본문에 어떤 영향을 미치는지 확인.

- 구문은 어떤 동작을 수행하고 값을 반환하지 않는 명령
- 표현식은 결과 값으로 평가됨

몇 가지 예 확인.

실제로 이미 구문과 표현식을 사용함. `let` 키워드로 변수를 만들고 값을 할당하는 것은 구문. Listing 3-1에서 `let y = 6;`은 구문.

파일명: src/main.rs

```rust
fn main() {
    let y = 6;
}
```

Listing 3-1: 하나의 구문을 포함하는 `main` 함수 선언.

함수 정의도 구문임. 앞의 전체 예제는 그 자체로 구문. (곧 확인하겠지만, 함수를 호출하는 것은 구문이 아님.)

구문은 값을 반환하지 않음 → 다음 코드가 시도하는 것처럼 `let` 구문을 다른 변수에 할당할 수 없음, 오류 발생:

파일명: src/main.rs

```rust
fn main() {
    let x = (let y = 6);
}
```

이 프로그램을 실행하면 다음과 같은 오류 발생:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
error: expected expression, found `let` statement
 --> src/main.rs:2:14
  |
2 |     let x = (let y = 6);
  |              ^^^
  |
  = note: only supported directly in conditions of `if` and `while` expressions

warning: unnecessary parentheses around assigned value
 --> src/main.rs:2:13
  |
2 |     let x = (let y = 6);
  |             ^         ^
  |
  = note: `#[warn(unused_parens)]` on by default
help: remove these parentheses
  |
2 -     let x = (let y = 6);
2 +     let x = let y = 6;
  |

warning: `functions` (bin "functions") generated 1 warning
error: could not compile `functions` (bin "functions") due to 1 previous error, 1 warning emitted
```

`let y = 6` 구문은 값을 반환하지 않음 → `x`에 바인딩할 것이 없음. C와 Ruby 같은 다른 언어에서 일어나는 일과 다름 → 그 언어들에서는 할당이 할당 값을 반환. 그러한 언어에서는 `x = y = 6`을 작성하여 `x`와 `y` 모두 `6` 값을 가지게 할 수 있지만, Rust에서는 그렇지 않음.

표현식은 값으로 평가되며 Rust에서 작성하게 될 나머지 대부분의 코드를 구성. `5 + 6`과 같은 수학 연산 → `11` 값으로 평가되는 표현식. 표현식은 구문의 일부일 수 있음 → Listing 3-1에서 `let y = 6;` 구문의 `6`은 `6` 값으로 평가되는 표현식. 함수를 호출하는 것은 표현식. 매크로를 호출하는 것은 표현식. 중괄호로 생성된 새 스코프 블록은 표현식. 예:

파일명: src/main.rs

```rust
fn main() {
    let y = {
        let x = 3;
        x + 1
    };

    println!("The value of y is: {y}");
}
```

이 표현식:

```rust
{
    let x = 3;
    x + 1
}
```

은 이 경우 `4`로 평가되는 블록. 그 값은 `let` 구문의 일부로 `y`에 바인딩. 끝에 세미콜론이 없는 `x + 1` 줄에 주목 → 지금까지 본 대부분의 줄과 다름. 표현식은 끝에 세미콜론을 포함하지 않음. 표현식 끝에 세미콜론을 추가하면 표현식을 구문으로 바꿈 → 값을 반환하지 않게 됨. 다음에 함수 반환 값과 표현식을 탐구할 때 이것을 염두에 둘 것.

#### 반환 값이 있는 함수

함수는 호출하는 코드에 값을 반환 가능. 반환 값에 이름을 붙이지 않지만 화살표(`->`) 뒤에 타입을 선언해야 함. Rust에서 함수의 반환 값은 함수 본문 블록의 마지막 표현식 값과 동의어. `return` 키워드를 사용하고 값을 지정하여 함수에서 일찍 반환 가능하지만, 대부분의 함수는 암묵적으로 마지막 표현식을 반환. 값을 반환하는 함수의 예:

파일명: src/main.rs

```rust
fn five() -> i32 {
    5
}

fn main() {
    let x = five();

    println!("The value of x is: {x}");
}
```

`five` 함수에는 함수 호출, 매크로, 심지어 `let` 구문도 없음 — 숫자 `5` 하나뿐. Rust에서 완벽하게 유효한 함수. 함수의 반환 타입도 `-> i32`로 지정. 이 코드를 실행하면 출력:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.30s
     Running `target/debug/functions`
The value of x is: 5
```

`five`의 `5`가 함수의 반환 값 → 반환 타입이 `i32`. 이것을 더 자세히 확인. 두 가지 중요한 점:

- `let x = five();` 줄은 함수의 반환 값을 사용하여 변수를 초기화한다는 것을 보여줌. `five` 함수가 `5`를 반환하기 때문에 그 줄은 다음과 같음:

```rust
let x = 5;
```

- `five` 함수는 매개변수가 없고 반환 값의 타입을 정의하지만, 함수 본문은 반환하고자 하는 값이기 때문에 세미콜론이 없는 외로운 `5`임

다른 예:

파일명: src/main.rs

```rust
fn main() {
    let x = plus_one(5);

    println!("The value of x is: {x}");
}

fn plus_one(x: i32) -> i32 {
    x + 1
}
```

이 코드를 실행하면 `The value of x is: 6`이 출력됨. 하지만 `x + 1`을 포함하는 줄 끝에 세미콜론을 배치하여 표현식을 구문으로 바꾸면:

파일명: src/main.rs

```rust
fn main() {
    let x = plus_one(5);

    println!("The value of x is: {x}");
}

fn plus_one(x: i32) -> i32 {
    x + 1;
}
```

이 코드를 컴파일하면 다음과 같은 오류 발생:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
error[E0308]: mismatched types
 --> src/main.rs:7:24
  |
7 | fn plus_one(x: i32) -> i32 {
  |    --------            ^^^ expected `i32`, found `()`
  |    |
  |    implicitly returns `()` as its body has no tail or `return` expression
8 |     x + 1;
  |          - help: remove this semicolon to return this value
  |
For more information about this error, try `rustc --explain E0308`.
error: could not compile `functions` (bin "functions") due to 1 previous error
```

주요 오류 메시지 `mismatched types`가 이 코드의 핵심 문제를 드러냄. `plus_one` 함수의 정의는 `i32`를 반환할 것이라고 말하지만, 구문은 값으로 평가되지 않으며 이는 유닛 타입인 `()`로 표현됨 → 아무것도 반환되지 않아 함수 정의와 모순되어 오류 발생. 이 출력에서 Rust는 이 문제를 수정하는 데 도움이 될 수 있는 메시지 제공: 세미콜론을 제거하면 오류가 수정됨.

---

## 주석

> 원문: https://doc.rust-lang.org/book/ch03-04-comments.html

모든 프로그래머는 코드를 이해하기 쉽게 만들려고 노력하지만, 때로는 추가 설명이 필요함 → 이러한 경우 프로그래머는 소스 코드에 주석을 남김. 컴파일러는 이를 무시하지만 소스 코드를 읽는 사람들은 유용하게 볼 수 있음.

#### 간단한 주석

다음은 간단한 주석:

```rust
// hello, world
```

#### 관용적인 주석 스타일

Rust에서 관용적인 주석 스타일은 두 개의 슬래시로 주석을 시작하며, 주석은 줄 끝까지 계속됨. 한 줄을 넘어 확장되는 주석의 경우 각 줄에 `//`를 포함해야 함:

```rust
// So we're doing something complicated here, long enough that we need
// multiple lines of comments to do it! Whew! Hopefully, this comment will
// explain what's going on.
```

#### 줄 끝 주석

주석은 코드가 포함된 줄의 끝에도 배치 가능:

파일명: src/main.rs

```rust
fn main() {
    let lucky_number = 7; // I'm feeling lucky today
}
```

#### 선호되는 형식

더 자주 보게 될 형식은 주석이 주석을 달고 있는 코드 위의 별도 줄에 있는 것:

파일명: src/main.rs

```rust
fn main() {
    // I'm feeling lucky today
    let lucky_number = 7;
}
```

#### 문서 주석

Rust에는 또 다른 종류의 주석인 문서 주석이 존재. [Crates.io에 크레이트 게시하기](7_functional_features_and_cargo.md#cratesio에-크레이트-게시하기)에서 논의됨.

---

## 제어 흐름

> 원문: https://doc.rust-lang.org/book/ch03-05-control-flow.html

조건이 `true`인지에 따라 코드를 실행하는 능력과 조건이 `true`인 동안 반복적으로 코드를 실행하는 능력은 대부분의 프로그래밍 언어에서 기본 빌딩 블록. Rust 코드의 실행 흐름을 제어할 수 있게 해주는 가장 일반적인 구조는 `if` 표현식과 루프.

### `if` 표현식

`if` 표현식은 조건에 따라 코드를 분기할 수 있게 해줌. 조건을 제공한 다음, "이 조건이 충족되면 이 코드 블록을 실행하고, 조건이 충족되지 않으면 이 코드 블록을 실행하지 마세요"라고 명시하는 구조.

#### 기본 `if` 표현식

```rust
fn main() {
    let number = 3;

    if number < 5 {
        println!("condition was true");
    } else {
        println!("condition was false");
    }
}
```

모든 `if` 표현식은 `if` 키워드로 시작하고 그 뒤에 조건이 옴. 이 경우 조건은 변수 `number`가 5보다 작은 값을 가지는지 확인. `if` 표현식의 조건과 연관된 코드 블록을 때때로 갈래(arms)라고 함.

`if` 표현식의 조건은 반드시 `bool`이어야 함. Rust는 Ruby나 JavaScript 같은 언어와 달리 불리언이 아닌 타입을 자동으로 불리언으로 변환하지 않음.

비교 연산자를 사용하는 예:

```rust
fn main() {
    let number = 3;

    if number != 0 {
        println!("number was something other than zero");
    }
}
```

#### `else if`로 여러 조건 처리하기

```rust
fn main() {
    let number = 6;

    if number % 4 == 0 {
        println!("number is divisible by 4");
    } else if number % 3 == 0 {
        println!("number is divisible by 3");
    } else if number % 2 == 0 {
        println!("number is divisible by 2");
    } else {
        println!("number is not divisible by 4, 3, or 2");
    }
}
```

출력:

```text
number is divisible by 3
```

Rust는 첫 번째 `true` 조건에 대한 블록만 실행 → 하나를 찾으면 나머지는 확인하지 않음.

#### `let` 문에서 `if` 사용하기

`if`는 표현식이므로 `let` 문의 오른쪽에 사용하여 결과를 변수에 할당 가능:

```rust
fn main() {
    let condition = true;
    let number = if condition { 5 } else { 6 };

    println!("The value of number is: {number}");
}
```

출력:

```text
The value of number is: 5
```

`if`의 각 갈래에서 결과가 될 수 있는 값은 같은 타입이어야 함:

```rust
fn main() {
    let condition = true;

    let number = if condition { 5 } else { "six" };  // 오류!

    println!("The value of number is: {number}");
}
```

이것은 `if` 갈래가 정수로 평가되고 `else` 갈래가 문자열로 평가되기 때문에 컴파일 오류 발생. 변수는 단일 타입을 가져야 함.

### 루프로 반복하기

Rust의 세 가지 종류의 루프: `loop`, `while`, `for`.

#### `loop`로 코드 반복하기

`loop` 키워드는 Rust에게 코드 블록을 영원히 또는 명시적으로 멈추라고 할 때까지 계속 실행하라고 지시:

```rust
fn main() {
    loop {
        println!("again!");
    }
}
```

`ctrl-C`를 사용하여 계속되는 루프에 갇힌 프로그램 중단 가능.

##### 루프에서 벗어나기

`break` 키워드로 루프 실행 중지:

```rust
fn main() {
    let mut counter = 0;

    let result = loop {
        counter += 1;

        if counter == 10 {
            break counter * 2;
        }
    };

    println!("The result is {result}");
}
```

출력:

```text
The result is 20
```

`break` 뒤에 값을 추가하여 해당 값을 루프 밖으로 반환 가능. `continue`를 사용하여 루프의 다음 반복으로 건너뛰기도 가능.

##### 루프 레이블

루프 안에 루프가 있는 경우, `break`와 `continue`는 가장 안쪽 루프에 적용됨. 루프 레이블을 사용하여 어떤 루프에서 벗어나거나 계속할지 지정 가능:

```rust
fn main() {
    let mut count = 0;
    'counting_up: loop {
        println!("count = {count}");
        let mut remaining = 10;

        loop {
            println!("remaining = {remaining}");
            if remaining == 9 {
                break;
            }
            if count == 2 {
                break 'counting_up;
            }
            remaining -= 1;
        }

        count += 1;
    }
    println!("End count = {count}");
}
```

출력:

```text
count = 0
remaining = 10
remaining = 9
count = 1
remaining = 10
remaining = 9
count = 2
remaining = 10
End count = 2
```

#### `while`로 조건부 루프

```rust
fn main() {
    let mut number = 3;

    while number != 0 {
        println!("{number}!");

        number -= 1;
    }

    println!("LIFTOFF!!!");
}
```

`while` 구조는 중첩을 제거하고 `loop`, `if`, `else`, `break`를 사용하는 것보다 더 명확함.

#### `for`로 컬렉션 순회하기

`while` 루프를 사용하여 배열 요소를 순회할 수 있지만, `for` 루프가 더 안전하고 간결:

```rust
fn main() {
    let a = [10, 20, 30, 40, 50];

    for element in a {
        println!("the value is: {element}");
    }
}
```

출력:

```text
the value is: 10
the value is: 20
the value is: 30
the value is: 40
the value is: 50
```

`for` 루프의 안전성과 간결성 → Rust에서 가장 일반적으로 사용되는 루프 구조가 되는 이유.

##### `for`와 범위 사용하기

표준 라이브러리의 `Range`를 사용하여 코드를 특정 횟수 실행 가능:

```rust
fn main() {
    for number in (1..4).rev() {
        println!("{number}!");
    }
    println!("LIFTOFF!!!");
}
```

`(1..4).rev()`를 사용하여 3에서 1까지 카운트다운.

### 요약

이 챕터에서 다룬 내용:

- 변수, 스칼라 및 복합 데이터 타입
- 함수
- 주석
- `if` 표현식
- 루프 (`loop`, `while`, `for`)

다음 챕터는 다른 프로그래밍 언어에 일반적으로 존재하지 않는 Rust의 개념인 소유권 논의.

---

## 소유권 이해하기

## 소유권 이해하기

> 원문: https://doc.rust-lang.org/book/ch04-00-understanding-ownership.html

### 챕터 개요

소유권(Ownership)은 Rust의 가장 독특한 기능이며 언어의 나머지 부분에 깊은 영향을 미침. 가비지 컬렉터 없이도 메모리 안전 보장을 가능하게 함 → 소유권이 어떻게 작동하는지 이해하는 것이 중요.

### 다루는 주제

이 챕터에서 배울 내용:

- 소유권 - Rust의 핵심 메모리 관리 시스템
- 빌림(Borrowing) - 데이터에 대한 참조를 빌려주는 방법
- 슬라이스 - 연속된 요소 시퀀스를 참조하는 방법
- 메모리 레이아웃 - Rust가 메모리에 데이터를 어떻게 구성하는지

### 핵심 개념

소유권은 Rust의 기본이 되는 개념:

- 가비지 컬렉터의 필요성 제거
- 컴파일 타임 메모리 안전 보장 제공
- use-after-free와 double-free 오류 같은 일반적인 메모리 버그 방지
- 안전한 동시성 코드 가능

---

## 소유권이란?

> 원문: https://doc.rust-lang.org/book/ch04-01-what-is-ownership.html

### 개요

소유권은 Rust 프로그램이 메모리를 관리하는 방법을 지배하는 규칙 집합. 모든 프로그램은 실행 중에 메모리를 관리해야 함. 가비지 컬렉션이 있는 언어나 수동 할당/해제가 필요한 언어와 달리, Rust는 세 번째 접근법 사용: 컴파일러가 검사하는 소유권 시스템, 런타임 성능 비용 없음.

### 스택과 힙

둘 다 런타임에 사용할 수 있는 메모리의 일부지만 구조가 다름:

#### 스택

- 후입선출(LIFO) 순서로 값을 저장
- 데이터 추가를 스택에 푸시라고 함
- 데이터 제거를 스택에서 팝이라고 함
- 모든 데이터는 컴파일 타임에 알려진 고정 크기여야 함
- 스택에 푸시하는 것이 힙 할당보다 빠름

#### 힙

- 덜 조직적, 메모리 할당자가 빈 공간을 찾음
- 할당된 위치에 대한 포인터(주소)를 반환
- 이 과정을 힙에 할당이라고 함
- 스택 데이터보다 접근이 느림 (포인터를 따라가야 함)
- 컴파일 타임에 크기를 알 수 없는 또는 가변 크기의 데이터 저장

### 소유권 규칙

다음 세 가지 기본 규칙:

- Rust의 각 값은 소유자가 있음
- 한 번에 하나의 소유자만 있을 수 있음
- 소유자가 스코프를 벗어나면 값이 삭제됨

### 변수 스코프

스코프는 항목이 유효한 프로그램 내의 범위:

```rust
fn main() {
    {                      // s는 여기서 유효하지 않음
        let s = "hello";   // s는 이 지점부터 유효함
        // s로 작업 수행
    }                      // 스코프 종료, s는 더 이상 유효하지 않음
}
```

- `s`가 스코프에 들어오면 유효함
- 스코프를 벗어날 때까지 유효함

### `String` 타입

소유권 규칙을 설명하기 위해 복잡한 데이터 타입이 필요함. `String` 타입은 힙에 저장되며 컴파일 타임에 텍스트 크기를 알 수 없을 때 적합.

```rust
let s = String::from("hello");
```

문자열 리터럴과 달리 `String`은 변경 가능:

```rust
fn main() {
    let mut s = String::from("hello");
    s.push_str(", world!"); // push_str()은 리터럴을 추가
    println!("{s}"); // 출력: hello, world!
}
```

### 메모리와 할당

문자열 리터럴은 컴파일 타임에 실행 파일에 하드코딩되어 빠르지만 불변.

String 타입은 다음이 필요:

- 런타임에 할당자로부터 메모리 할당
- 완료 시 메모리를 반환하는 방법

Rust의 해결책: 변수(소유자)가 스코프를 벗어나면 `drop` 함수를 통해 메모리가 자동으로 반환됨.

```rust
fn main() {
    {
        let s = String::from("hello");
        // s로 작업 수행
    }  // s가 스코프를 벗어남; drop이 호출됨; 메모리가 해제됨
}
```

이 패턴(수명이 끝날 때 리소스 해제)을 RAII(Resource Acquisition Is Initialization)라고 함.

### 이동(Move)과 변수 데이터 상호작용

#### 스택 데이터(정수)

```rust
fn main() {
    let x = 5;
    let y = x;  // x가 복사됨; 둘 다 유효함
}
```

정수는 알려진 고정 크기를 가지며 간단하게 복사됨.

#### 힙 데이터(String)

```rust
fn main() {
    let s1 = String::from("hello");
    let s2 = s1;  // s1이 s2로 이동됨
}
```

`String`은 다음으로 구성:

- 힙 메모리에 대한 포인터
- 길이 (현재 사용 중인 바이트)
- 용량 (할당된 총 바이트)

`s2 = s1`이 실행될 때:

- 스택 데이터만 복사됨 (포인터, 길이, 용량)
- 힙 데이터는 복사되지 않음
- 이중 해제 오류를 방지하기 위해 `s1`은 무효화됨

이동 후 `s1`을 사용하려고 하면 컴파일러 오류 발생:

```rust
fn main() {
    let s1 = String::from("hello");
    let s2 = s1;
    println!("{s1}, world!");  // 오류: borrow of moved value
}
```

오류 메시지: `error[E0382]: borrow of moved value: s1`

이 연산을 이동(move)이라고 함 (첫 번째 변수가 무효화되므로 얕은 복사가 아님).

### 스코프와 할당

새 값이 기존 변수에 할당되면 원래 값의 메모리가 즉시 해제됨:

```rust
fn main() {
    let mut s = String::from("hello");
    s = String::from("ahoy");  // "hello"가 삭제됨; 메모리 해제
    println!("{s}, world!");   // 출력: ahoy, world!
}
```

### 클론(Clone)과 변수 데이터 상호작용

힙 데이터를 명시적으로 깊은 복사하려면 (스택 데이터만이 아닌):

```rust
fn main() {
    let s1 = String::from("hello");
    let s2 = s1.clone();  // 힙 데이터의 깊은 복사
    println!("s1 = {s1}, s2 = {s2}");  // 둘 다 유효
}
```

`clone()`을 보면 임의의 코드가 실행되고 있으며 성능에 비용이 들 수 있음을 알 수 있음.

### 스택 전용 데이터: Copy

`Copy` 트레이트는 스택에 전적으로 저장된 타입이 원본을 무효화하지 않고 간단하게 복사되도록 함:

```rust
fn main() {
    let x = 5;
    let y = x;
    println!("x = {x}, y = {y}");  // 둘 다 유효; 이동 없음
}
```

`Copy`를 구현하는 타입:

- 모든 정수 타입 (`u32`, `i64` 등)
- 불리언 타입 (`bool`)
- 모든 부동 소수점 타입 (`f64`, `f32` 등)
- 문자 타입 (`char`)
- `Copy` 타입만 포함하는 튜플: `(i32, i32)` 가능 · `(i32, String)` 불가능

참고: 타입 또는 그 일부가 `Drop`을 구현하면 `Copy`를 구현할 수 없음.

### 소유권과 함수

함수에 값을 전달하는 것은 할당과 같은 이동/복사 의미를 따름:

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

### 반환 값과 스코프

값을 반환하는 것도 소유권을 이전함:

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

패턴은 다음과 같음: 다른 변수에 값을 할당하면 이동함. 힙 변수가 스코프를 벗어나고 소유권이 이동되지 않았으면 `drop`이 메모리를 정리함.

#### 번거로움 문제

이 소유권 패턴은 작동하지만 번거로움 — 모든 함수마다 소유권을 반환하는 것은 지루함. Rust는 해결책 제공: 참조 (다음 챕터에서 다룸).

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

그러나 이 목적을 위해 튜플을 반환하는 것은 의례적임. 참조는 소유권을 이전하지 않고 값을 사용할 수 있게 해줌 — 더 우아한 해결책.

---

## 참조와 빌림

> 원문: https://doc.rust-lang.org/book/ch04-02-references-and-borrowing.html

### 개요

참조는 소유권을 가져가지 않고 값을 참조할 수 있게 해줌. 참조는 해당 참조의 수명 동안 특정 타입의 유효한 값을 가리키도록 보장되는 포인터와 같음.

### 기본 참조

소유권을 이전하는 대신 값에 대한 참조를 전달 가능:

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

`&s1` 문법은 `s1`의 값을 소유하지 않고 참조하는 참조를 생성함. 참조가 스코프를 벗어나면 참조가 소유권을 가지지 않기 때문에 값이 삭제되지 않음.

핵심 포인트: 이 과정을 빌림(borrowing)이라고 함. 빌린 것을 다 쓰면 소유하지 않고 돌려줌.

### 빌린 값 수정 시도

기본적으로 참조는 불변임:

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

### 가변 참조

빌린 값을 수정하려면 가변 참조 사용:

```rust
fn main() {
    let mut s = String::from("hello");

    change(&mut s);
}

fn change(some_string: &mut String) {
    some_string.push_str(", world");
}
```

#### 가변 참조 제한

한 번에 값에 대한 가변 참조는 하나만 가질 수 있음:

```rust
fn main() {
    let mut s = String::from("hello");

    let r1 = &mut s;
    let r2 = &mut s; // 오류!

    println!("{r1}, {r2}");
}
```

오류: `error[E0499]: cannot borrow 's' as mutable more than once at a time`

다른 스코프를 사용하여 여러 가변 참조 생성 가능:

```rust
fn main() {
    let mut s = String::from("hello");

    {
        let r1 = &mut s;
    } // r1이 여기서 스코프를 벗어남

    let r2 = &mut s; // OK
}
```

### 가변 참조와 불변 참조 혼합

불변 참조가 존재하는 동안 가변 참조를 가질 수 없음:

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

그러나 불변 참조가 더 이상 사용되지 않으면 작동함:

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

참조 스코프는 도입된 곳에서 시작하여 마지막 사용 시점에서 끝남.

### 댕글링 참조

Rust는 컴파일 타임에 댕글링 참조(잘못된 메모리에 대한 참조)를 방지함:

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

해결책: 소유된 값을 직접 반환:

```rust
fn main() {
    let string = no_dangle();
}

fn no_dangle() -> String {
    let s = String::from("hello");

    s // 소유권이 밖으로 이동
}
```

### 참조의 규칙

- 주어진 시점에 다음 중 하나만 가질 수 있음
  - 하나의 가변 참조, 또는
  - 임의 개수의 불변 참조
- 참조는 항상 유효해야 함

이러한 규칙은 컴파일 타임에 데이터 경쟁을 방지하고 메모리 안전을 보장함.

---

## 슬라이스 타입

> 원문: https://doc.rust-lang.org/book/ch04-03-slices.html

슬라이스는 컬렉션에서 연속된 요소 시퀀스를 참조할 수 있게 해줌. 슬라이스는 일종의 참조이므로 소유권이 없음.

#### 문제 제시

공백으로 구분된 단어로 구성된 문자열을 받아 찾은 첫 번째 단어를 반환하는 함수 작성. 문자열에서 공백을 찾지 못하면 전체 문자열이 하나의 단어이므로 전체 문자열을 반환해야 함.

참고: 슬라이스를 소개하기 위해 이 섹션에서는 ASCII만 가정. UTF-8 처리에 대한 더 자세한 논의는 8장 참조.

#### 슬라이스 없이 초기 접근법

슬라이스 없이 함수 시그니처는 다음과 같음:

```rust
fn first_word(s: &String) -> ?
```

무엇을 반환할지 결정하는 것이 과제. 가능한 해결책은 단어 끝의 바이트 인덱스를 반환하는 것:

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

Listing 4-7: 바이트 인덱스 값을 반환하는 `first_word` 함수.

##### 이 접근법의 문제

`String`과 별개로 `usize`를 반환하는 것은 문제가 있음. 인덱스는 `String`의 맥락에서만 의미가 있지만, 유효성이 유지된다는 보장이 없음:

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

Listing 4-8: 결과를 저장한 다음 `String` 내용을 변경.

이 프로그램은 오류 없이 컴파일되지만, `s.clear()`가 호출된 후 `word`는 무효화됨. 동기화되어야 하는 여러 관련 없는 인덱스를 관리하는 것은 오류가 발생하기 쉽고 취약함.

---

### 문자열 슬라이스

문자열 슬라이스는 `String`의 연속된 요소 시퀀스에 대한 참조:

```rust
fn main() {
    let s = String::from("hello world");

    let hello = &s[0..5];
    let world = &s[6..11];
}
```

슬라이스는 대괄호 안의 범위를 사용하여 생성됨: `[시작_인덱스..끝_인덱스]`, 여기서:

- 시작_인덱스는 슬라이스의 첫 번째 위치
- 끝_인덱스는 슬라이스의 마지막 위치보다 하나 큼

내부적으로 슬라이스 데이터 구조는 시작 위치와 슬라이스 길이(끝_인덱스 - 시작_인덱스)를 저장함.

#### 슬라이스 문법 단축

인덱스 0에서 시작:

```rust
fn main() {
    let s = String::from("hello");

    let slice = &s[0..2];
    let slice = &s[..2];  // 동일함
}
```

마지막 바이트 포함:

```rust
fn main() {
    let s = String::from("hello");
    let len = s.len();

    let slice = &s[3..len];
    let slice = &s[3..];  // 동일함
}
```

전체 문자열:

```rust
fn main() {
    let s = String::from("hello");
    let len = s.len();

    let slice = &s[0..len];
    let slice = &s[..];  // 동일함
}
```

참고: 문자열 슬라이스 범위 인덱스는 유효한 UTF-8 문자 경계에서 발생해야 함.

#### 슬라이스로 `first_word` 다시 작성하기

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

이제 `first_word`는 기본 데이터와 연결된 `&str`(문자열 슬라이스)을 반환함.

#### 컴파일 타임 오류 방지

슬라이스를 사용하면 Listing 4-8의 버그를 컴파일 타임에 방지:

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

컴파일러 오류:

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

컴파일러는 `word`의 불변 빌림과 동시에 `clear()`의 가변 빌림이 존재하는 것을 방지함.

---

### 슬라이스로서의 문자열 리터럴

문자열 리터럴은 바이너리 안에 저장되며 `&str` 타입을 가짐:

```rust
fn main() {
    let s = "Hello, world!";
}
```

`s`의 타입은 `&str`임: 바이너리의 특정 지점을 가리키는 슬라이스. 이것이 문자열 리터럴이 불변인 이유임.

---

### 매개변수로서의 문자열 슬라이스

더 관용적인 접근법은 `&String` 대신 `&str`을 매개변수 타입으로 사용하는 것:

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

Listing 4-9: 문자열 슬라이스를 사용하여 `first_word` 함수 개선.

`&str`을 사용하면 역참조 강제 변환(15장에서 다룸)을 통해 모든 기능을 유지하면서 API를 더 일반적이고 유용하게 만듦.

---

### 다른 슬라이스

슬라이스는 배열에서도 작동함:

```rust
fn main() {
    let a = [1, 2, 3, 4, 5];

    let slice = &a[1..3];

    assert_eq!(slice, &[2, 3]);
}
```

이 슬라이스는 `&[i32]` 타입을 가지며 문자열 슬라이스와 같은 방식으로 작동함 — 첫 번째 요소에 대한 참조와 길이를 저장함.

---

### 요약

소유권, 빌림, 슬라이스의 개념은 Rust 프로그램에서 컴파일 타임에 메모리 안전을 보장함. Rust는 다른 시스템 프로그래밍 언어처럼 메모리 사용에 대한 제어를 제공하지만, 데이터 소유자가 스코프를 벗어나면 자동으로 정리되어 추가 정리 코드가 필요 없음. 이러한 개념은 Rust의 많은 다른 부분에 영향을 미치며 책 전체에서 논의됨.
