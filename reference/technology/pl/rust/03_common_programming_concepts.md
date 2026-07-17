# 일반적인 프로그래밍 개념

# 일반적인 프로그래밍 개념

> **원문:** https://doc.rust-lang.org/book/ch03-00-common-programming-concepts.html

## 챕터 개요

이 챕터에서는 거의 모든 프로그래밍 언어에 나타나는 기본 프로그래밍 개념과 이 개념들이 Rust에서 어떻게 작동하는지 다룹니다. 이러한 개념 중 Rust에 고유한 것은 없지만, Rust의 맥락에서 관례와 사용법을 논의할 것입니다.

## 다루는 주제

이 챕터는 다음 기초 개념에 초점을 맞춥니다:

- **변수**
- **기본 타입**
- **함수**
- **주석**
- **제어 흐름**

## 핵심 포인트

1. **모든 Rust 프로그램의 기초**: 이러한 개념은 모든 Rust 프로그램에 나타날 기초를 형성합니다. 이것들을 일찍 배우면 강력한 핵심 기반을 제공합니다.

2. **언어 간 공통점**: 많은 프로그래밍 언어가 기초에서 이러한 핵심 개념을 공유하지만, 구체적인 구현은 다를 수 있습니다.

3. **키워드**: Rust 언어에는 변수나 함수의 이름으로 사용할 수 없는 예약된 키워드가 있습니다. 대부분의 키워드는 특별한 의미를 가지며 Rust 프로그램에서 다양한 작업에 사용됩니다. 일부 키워드는 향후 기능을 위해 예약되어 있습니다. 키워드의 전체 목록은 부록 A에서 찾을 수 있습니다.

## 탐색

- **이전 챕터**: 추리 게임 튜토리얼 (챕터 2)
- **다음 챕터**: 변수와 가변성 (챕터 3)

---

# 변수와 가변성

> **원문:** https://doc.rust-lang.org/book/ch03-01-variables-and-mutability.html

## 개요

기본적으로 Rust에서 변수는 **불변**(immutable)입니다. 값이 이름에 바인딩되면 그 값을 변경할 수 없습니다. 이것은 Rust가 제공하는 안전 기능 중 하나이지만, 필요할 때 변수를 가변으로 만들 수 있는 옵션이 있습니다.

## 불변 변수

`let`으로 변수를 선언하면 기본적으로 불변입니다:

```rust
fn main() {
    let x = 5;
    println!("The value of x is: {x}");
    x = 6;  // 컴파일 오류 발생
    println!("The value of x is: {x}");
}
```

이것은 컴파일 타임 오류를 발생시킵니다:

```text
error[E0384]: cannot assign twice to immutable variable `x`
```

컴파일러가 우발적인 변경을 방지하여 버그를 일찍 잡는 데 도움을 줍니다. 코드의 한 부분이 값이 변경되지 않을 것이라고 가정하고 다른 부분이 그 값을 변경하면, 컴파일러가 런타임 버그 대신 컴파일 타임에 이 문제를 잡아냅니다.

## 변수를 가변으로 만들기

변수를 가변으로 만들려면 `mut` 키워드를 추가하세요:

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

`mut`를 사용하면 코드를 읽는 다른 개발자에게 의도를 전달하기도 합니다.

## 상수 선언하기

상수는 불변 변수와 유사하지만 중요한 차이점이 있습니다:

```rust
const THREE_HOURS_IN_SECONDS: u32 = 60 * 60 * 3;
```

### 주요 차이점:

- 상수에는 `mut`를 사용할 수 **없습니다**—항상 불변입니다
- `let` 대신 `const` 키워드로 선언
- **타입 어노테이션 필수**
- 전역 스코프를 포함한 모든 스코프에서 선언 가능
- 런타임 값이 아닌 상수 표현식으로만 설정 가능
- 해당 스코프 내에서 프로그램 전체 수명 동안 유효

**명명 규칙:** 상수는 ALL_UPPERCASE_WITH_UNDERSCORES를 사용

## 섀도잉

이전 변수와 같은 이름으로 새 변수를 선언할 수 있습니다. 새 변수가 이전 변수를 "섀도잉"합니다:

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

### 섀도잉 vs 가변성

**핵심 차이점:** 섀도잉은 **새 변수**를 생성하고, `mut`는 같은 변수의 재할당을 허용합니다.

**섀도잉의 경우**, `let`을 잊으면 컴파일 오류가 발생합니다:

```rust
let x = 5;
x = 6;  // 오류: cannot assign twice to immutable variable
```

**섀도잉은 타입 변경을 허용**하지만, `mut`는 그렇지 않습니다:

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

섀도잉은 값을 변환하면서 변환 후에도 불변으로 유지하고 싶을 때 유용합니다.

---

# 데이터 타입

> **원문:** https://doc.rust-lang.org/book/ch03-02-data-types.html

Rust의 모든 값은 특정 *데이터 타입*을 가지며, 이는 Rust에게 어떤 종류의 데이터가 지정되었는지 알려주어 해당 데이터를 어떻게 다룰지 알 수 있게 합니다. 스칼라와 복합이라는 두 가지 데이터 타입 하위 집합을 살펴보겠습니다.

Rust는 *정적 타입* 언어임을 기억하세요. 이는 컴파일 타임에 모든 변수의 타입을 알아야 한다는 것을 의미합니다. 컴파일러는 보통 값과 사용 방법에 따라 사용하고자 하는 타입을 추론할 수 있습니다. `String`을 `parse`를 사용하여 숫자 타입으로 변환할 때처럼 여러 타입이 가능한 경우에는 타입 어노테이션을 추가해야 합니다.

타입 어노테이션이 필요한 예:

```rust
let guess: u32 = "42".parse().expect("Not a number!");
```

`: u32` 어노테이션 없이는 컴파일러가 타입 어노테이션이 필요하다는 오류를 표시합니다.

---

## 스칼라 타입

*스칼라* 타입은 단일 값을 나타냅니다. Rust에는 네 가지 주요 스칼라 타입이 있습니다: 정수, 부동 소수점 숫자, 불리언, 문자.

### 정수 타입

*정수*는 소수 부분이 없는 숫자입니다.

**표 3-1: Rust의 정수 타입**

| 길이 | 부호 있음 | 부호 없음 |
|------|----------|----------|
| 8비트 | `i8` | `u8` |
| 16비트 | `i16` | `u16` |
| 32비트 | `i32` | `u32` |
| 64비트 | `i64` | `u64` |
| 128비트 | `i128` | `u128` |
| 아키텍처 의존 | `isize` | `usize` |

*부호 있음*과 *부호 없음*은 숫자가 음수가 될 수 있는지 여부를 나타냅니다. 부호 있는 숫자는 2의 보수 표현을 사용하여 저장됩니다.

- 부호 있는 각 변형은 -(2^(n-1))에서 2^(n-1) - 1까지의 숫자를 저장할 수 있습니다
- 부호 없는 변형은 0에서 2^n - 1까지의 숫자를 저장할 수 있습니다
- `isize`와 `usize`는 컴퓨터 아키텍처에 따라 달라집니다 (64비트 시스템에서는 64비트, 32비트 시스템에서는 32비트)

**표 3-2: Rust의 정수 리터럴**

| 숫자 리터럴 | 예 |
|------------|-----|
| 십진수 | `98_222` |
| 16진수 | `0xff` |
| 8진수 | `0o77` |
| 이진수 | `0b1111_0000` |
| 바이트 (`u8`만) | `b'A'` |

숫자 리터럴은 `_`를 시각적 구분자로 사용할 수 있습니다. 예: `1_000` (`1000`과 동일).

정수 타입은 기본적으로 `i32`입니다. 컬렉션 인덱싱 시 주로 `isize` 또는 `usize`를 사용합니다.

#### 정수 오버플로

정수 오버플로는 변수가 보유할 수 있는 범위를 초과할 때 발생합니다. 동작은 컴파일 모드에 따라 다릅니다:

- **디버그 모드**: Rust는 런타임에 프로그램을 *패닉*시키는 검사를 포함합니다
- **릴리스 모드** (`--release` 플래그 사용): Rust는 *2의 보수 래핑*을 수행하여 값이 범위의 최소값으로 래핑됩니다

오버플로를 명시적으로 처리하려면 표준 라이브러리가 기본 숫자 타입에 다음 메서드 계열을 제공합니다:

- `wrapping_*` 메서드 (예: `wrapping_add`) - 모든 모드에서 래핑
- `checked_*` 메서드 - 오버플로 발생 시 `None` 반환
- `overflowing_*` 메서드 - 값과 오버플로 여부를 나타내는 불리언 반환
- `saturating_*` 메서드 - 최소값 또는 최대값에서 포화

### 부동 소수점 타입

Rust에는 *부동 소수점 숫자*를 위한 두 가지 기본 타입이 있습니다: `f32`와 `f64`로, 각각 32비트와 64비트 크기입니다. 현대 CPU에서 `f64`가 `f32`와 거의 같은 속도지만 더 정밀하기 때문에 기본 타입은 `f64`입니다. 모든 부동 소수점 타입은 부호가 있습니다.

부동 소수점 숫자는 IEEE-754 표준에 따라 표현됩니다.

예:

```rust
fn main() {
    let x = 2.0; // f64

    let y: f32 = 3.0; // f32
}
```

### 숫자 연산

Rust는 모든 숫자 타입에 기본 수학 연산을 지원합니다: 덧셈, 뺄셈, 곱셈, 나눗셈, 나머지. 정수 나눗셈은 0 방향으로 가장 가까운 정수로 잘립니다.

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

### 불리언 타입

Rust의 불리언 타입은 `true`와 `false` 두 가지 가능한 값을 가집니다. 불리언은 1바이트 크기입니다. 불리언 타입은 `bool`을 사용하여 지정합니다.

```rust
fn main() {
    let t = true;

    let f: bool = false; // 명시적 타입 어노테이션 사용
}
```

불리언 값은 주로 `if` 표현식과 같은 조건문에서 사용됩니다.

### 문자 타입

Rust의 `char` 타입은 언어에서 가장 기본적인 알파벳 타입입니다. `char` 리터럴은 큰따옴표를 사용하는 문자열 리터럴과 달리 작은따옴표로 지정합니다.

```rust
fn main() {
    let c = 'z';
    let z: char = 'ℤ'; // 명시적 타입 어노테이션 사용
    let heart_eyed_cat = '😻';
}
```

Rust의 `char` 타입은 4바이트 크기이며 유니코드 스칼라 값을 나타냅니다. 이것은 다음을 표현할 수 있음을 의미합니다:

- 악센트 문자
- 중국어, 일본어, 한국어 문자
- 이모지
- 제로 폭 공백
- `U+0000`에서 `U+D7FF` 및 `U+E000`에서 `U+10FFFF`까지의 유니코드 스칼라 값

---

## 복합 타입

*복합 타입*은 여러 값을 하나의 타입으로 그룹화할 수 있습니다. Rust에는 두 가지 기본 복합 타입이 있습니다: 튜플과 배열.

### 튜플 타입

*튜플*은 다양한 타입의 여러 값을 하나의 복합 타입으로 그룹화하는 일반적인 방법입니다. 튜플은 고정된 길이를 가지며 한 번 선언되면 늘어나거나 줄어들 수 없습니다.

튜플 생성:

```rust
fn main() {
    let tup: (i32, f64, u8) = (500, 6.4, 1);
}
```

**튜플 구조 분해:**

```rust
fn main() {
    let tup = (500, 6.4, 1);

    let (x, y, z) = tup;

    println!("The value of y is: {y}");
}
```

**인덱스로 튜플 요소 접근:**

```rust
fn main() {
    let x: (i32, f64, u8) = (500, 6.4, 1);

    let five_hundred = x.0;

    let six_point_four = x.1;

    let one = x.2;
}
```

튜플의 첫 번째 인덱스는 0입니다.

**유닛 타입:**

값이 없는 튜플은 *유닛*이라는 특별한 이름을 가집니다. 이 값과 해당 타입은 모두 `()`로 작성되며 빈 값 또는 빈 반환 타입을 나타냅니다. 표현식이 다른 값을 반환하지 않으면 암묵적으로 유닛 값을 반환합니다.

### 배열 타입

*배열*은 여러 값의 컬렉션을 가지는 또 다른 방법입니다. 튜플과 달리:

- 배열의 모든 요소는 같은 타입이어야 합니다
- Rust의 배열은 고정된 길이를 가집니다

```rust
fn main() {
    let a = [1, 2, 3, 4, 5];
}
```

배열은 다음과 같은 경우에 유용합니다:

- 스택에 데이터를 할당하고 싶을 때
- 항상 고정된 수의 요소를 갖도록 하고 싶을 때

늘어나거나 줄어들 수 있는 유연한 컬렉션이 필요하면 대신 **벡터**를 사용하세요 (8장에서 논의).

**배열 타입 어노테이션:**

```rust
let a: [i32; 5] = [1, 2, 3, 4, 5];
```

여기서 `i32`는 요소 타입이고 `5`는 요소 수입니다.

**같은 값으로 배열 초기화:**

```rust
let a = [3; 5];
```

이것은 5개의 요소를 가진 배열을 생성하며, 모두 `3`으로 초기화됩니다 (`[3, 3, 3, 3, 3]`과 동일).

### 배열 요소 접근

인덱싱을 사용하여 배열 요소에 접근:

```rust
fn main() {
    let a = [1, 2, 3, 4, 5];

    let first = a[0];
    let second = a[1];
}
```

### 잘못된 배열 요소 접근

배열의 끝을 넘어 요소에 접근하려고 하면 Rust는 패닉합니다:

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

Rust는 런타임에 인덱스가 배열 길이보다 작은지 확인하고, 같거나 크면 패닉합니다. 이것은 많은 저수준 언어에서 발생하는 잘못된 메모리 접근을 방지하는 메모리 안전 원칙입니다.

---

# 함수

> **원문:** https://doc.rust-lang.org/book/ch03-03-how-functions-work.html

함수는 Rust 코드에서 광범위하게 사용됩니다. 이미 언어에서 가장 중요한 함수 중 하나인 `main` 함수를 보았습니다. 이 함수는 많은 프로그램의 진입점입니다. 또한 새로운 함수를 선언할 수 있게 해주는 `fn` 키워드도 보았습니다.

Rust 코드는 함수와 변수 이름에 *스네이크 케이스*를 관례적인 스타일로 사용합니다. 모든 문자가 소문자이고 밑줄이 단어를 구분합니다. 다음은 함수 정의 예제가 포함된 프로그램입니다:

**파일명: src/main.rs**

```rust
fn main() {
    println!("Hello, world!");

    another_function();
}

fn another_function() {
    println!("Another function.");
}
```

Rust에서 함수를 정의하려면 `fn` 다음에 함수 이름과 괄호 세트를 입력합니다. 중괄호는 컴파일러에게 함수 본문이 시작하고 끝나는 위치를 알려줍니다.

정의한 함수는 이름 뒤에 괄호를 입력하여 호출할 수 있습니다. `another_function`이 프로그램에 정의되어 있으므로 `main` 함수 내부에서 호출할 수 있습니다. 소스 코드에서 `main` 함수 *뒤에* `another_function`을 정의했지만, 앞에 정의할 수도 있습니다. Rust는 함수가 어디에 정의되어 있는지 상관하지 않으며, 호출자가 볼 수 있는 스코프 어딘가에 정의되어 있기만 하면 됩니다.

함수를 더 탐구하기 위해 *functions*라는 새 바이너리 프로젝트를 시작합시다. `another_function` 예제를 *src/main.rs*에 넣고 실행하세요. 다음과 같은 출력이 표시되어야 합니다:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.28s
     Running `target/debug/functions`
Hello, world!
Another function.
```

줄은 `main` 함수에 나타나는 순서대로 실행됩니다. 먼저 "Hello, world!" 메시지가 출력되고, 그 다음 `another_function`이 호출되어 그 메시지가 출력됩니다.

### 매개변수

함수가 *매개변수*를 갖도록 정의할 수 있습니다. 매개변수는 함수 시그니처의 일부인 특수 변수입니다. 함수에 매개변수가 있으면 해당 매개변수에 구체적인 값을 제공할 수 있습니다. 기술적으로 구체적인 값을 *인수*라고 하지만, 일상적인 대화에서는 함수 정의의 변수나 호출 시 전달되는 구체적인 값 모두에 *매개변수*와 *인수*라는 단어를 혼용하는 경향이 있습니다.

이 버전의 `another_function`에서는 매개변수를 추가합니다:

**파일명: src/main.rs**

```rust
fn main() {
    another_function(5);
}

fn another_function(x: i32) {
    println!("The value of x is: {x}");
}
```

이 프로그램을 실행해 보세요. 다음과 같은 출력이 표시되어야 합니다:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 1.21s
     Running `target/debug/functions`
The value of x is: 5
```

`another_function` 선언에는 `x`라는 매개변수가 하나 있습니다. `x`의 타입은 `i32`로 지정되어 있습니다. `another_function`에 `5`를 전달하면 `println!` 매크로가 포맷 문자열에서 `x`를 포함하는 중괄호 쌍이 있던 자리에 `5`를 넣습니다.

함수 시그니처에서 각 매개변수의 타입을 *반드시* 선언해야 합니다. 이것은 Rust 설계에서 의도적인 결정입니다: 함수 정의에서 타입 어노테이션을 요구하면 컴파일러가 코드의 다른 곳에서 어떤 타입을 의미하는지 파악하기 위해 타입 어노테이션을 사용할 필요가 거의 없습니다. 또한 컴파일러가 함수가 기대하는 타입을 알면 더 유용한 오류 메시지를 제공할 수 있습니다.

여러 매개변수를 정의할 때는 다음과 같이 매개변수 선언을 쉼표로 구분합니다:

**파일명: src/main.rs**

```rust
fn main() {
    print_labeled_measurement(5, 'h');
}

fn print_labeled_measurement(value: i32, unit_label: char) {
    println!("The measurement is: {value}{unit_label}");
}
```

이 예제는 두 개의 매개변수를 가진 `print_labeled_measurement`라는 함수를 생성합니다. 첫 번째 매개변수는 `value`이고 `i32`입니다. 두 번째는 `unit_label`이고 `char` 타입입니다. 함수는 `value`와 `unit_label` 둘 다 포함하는 텍스트를 출력합니다.

이 코드를 실행해 봅시다. 현재 *functions* 프로젝트의 *src/main.rs* 파일에 있는 프로그램을 위의 예제로 교체하고 `cargo run`으로 실행하세요:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.31s
     Running `target/debug/functions`
The measurement is: 5h
```

`value`에 `5`를, `unit_label`에 `'h'`를 넣어 함수를 호출했기 때문에 프로그램 출력에 해당 값들이 포함됩니다.

### 구문과 표현식

함수 본문은 선택적으로 표현식으로 끝나는 일련의 구문으로 구성됩니다. 지금까지 다룬 함수에는 끝나는 표현식이 포함되지 않았지만, 구문의 일부로서 표현식을 보았습니다. Rust는 표현식 기반 언어이므로 이것은 이해해야 할 중요한 구분입니다. 다른 언어에는 같은 구분이 없으므로 구문과 표현식이 무엇인지, 그리고 그 차이가 함수 본문에 어떤 영향을 미치는지 살펴보겠습니다.

- *구문*은 어떤 동작을 수행하고 값을 반환하지 않는 명령입니다.
- *표현식*은 결과 값으로 평가됩니다.

몇 가지 예를 살펴봅시다.

실제로 이미 구문과 표현식을 사용했습니다. `let` 키워드로 변수를 만들고 값을 할당하는 것은 구문입니다. Listing 3-1에서 `let y = 6;`은 구문입니다.

**파일명: src/main.rs**

```rust
fn main() {
    let y = 6;
}
```

**Listing 3-1:** 하나의 구문을 포함하는 `main` 함수 선언

함수 정의도 구문입니다. 앞의 전체 예제는 그 자체로 구문입니다. (곧 보겠지만, 함수를 *호출하는 것*은 구문이 아닙니다.)

구문은 값을 반환하지 않습니다. 따라서 다음 코드가 시도하는 것처럼 `let` 구문을 다른 변수에 할당할 수 없습니다. 오류가 발생합니다:

**파일명: src/main.rs**

```rust
fn main() {
    let x = (let y = 6);
}
```

이 프로그램을 실행하면 다음과 같은 오류가 발생합니다:

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

`let y = 6` 구문은 값을 반환하지 않으므로 `x`에 바인딩할 것이 없습니다. 이것은 C와 Ruby 같은 다른 언어에서 일어나는 일과 다릅니다. 그 언어들에서는 할당이 할당 값을 반환합니다. 그러한 언어에서는 `x = y = 6`을 작성하여 `x`와 `y` 모두 `6` 값을 가지게 할 수 있지만, Rust에서는 그렇지 않습니다.

표현식은 값으로 평가되며 Rust에서 작성하게 될 나머지 대부분의 코드를 구성합니다. `5 + 6`과 같은 수학 연산을 생각해 보세요. 이것은 `11` 값으로 평가되는 표현식입니다. 표현식은 구문의 일부일 수 있습니다: Listing 3-1에서 `let y = 6;` 구문의 `6`은 `6` 값으로 평가되는 표현식입니다. 함수를 호출하는 것은 표현식입니다. 매크로를 호출하는 것은 표현식입니다. 중괄호로 생성된 새 스코프 블록은 표현식입니다. 예를 들어:

**파일명: src/main.rs**

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

은 이 경우 `4`로 평가되는 블록입니다. 그 값은 `let` 구문의 일부로 `y`에 바인딩됩니다. 끝에 세미콜론이 없는 `x + 1` 줄에 주목하세요. 지금까지 본 대부분의 줄과 다릅니다. 표현식은 끝에 세미콜론을 포함하지 않습니다. 표현식 끝에 세미콜론을 추가하면 표현식을 구문으로 바꾸고, 그러면 값을 반환하지 않습니다. 다음에 함수 반환 값과 표현식을 탐구할 때 이것을 명심하세요.

### 반환 값이 있는 함수

함수는 호출하는 코드에 값을 반환할 수 있습니다. 반환 값에 이름을 붙이지 않지만 화살표(`->`) 뒤에 타입을 선언해야 합니다. Rust에서 함수의 반환 값은 함수 본문 블록의 마지막 표현식 값과 동의어입니다. `return` 키워드를 사용하고 값을 지정하여 함수에서 일찍 반환할 수 있지만, 대부분의 함수는 암묵적으로 마지막 표현식을 반환합니다. 다음은 값을 반환하는 함수의 예입니다:

**파일명: src/main.rs**

```rust
fn five() -> i32 {
    5
}

fn main() {
    let x = five();

    println!("The value of x is: {x}");
}
```

`five` 함수에는 함수 호출, 매크로, 심지어 `let` 구문도 없습니다—숫자 `5` 하나뿐입니다. 이것은 Rust에서 완벽하게 유효한 함수입니다. 함수의 반환 타입도 `-> i32`로 지정되어 있습니다. 이 코드를 실행해 보세요. 출력은 다음과 같아야 합니다:

```console
$ cargo run
   Compiling functions v0.1.0 (file:///projects/functions)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.30s
     Running `target/debug/functions`
The value of x is: 5
```

`five`의 `5`가 함수의 반환 값이므로 반환 타입이 `i32`입니다. 이것을 더 자세히 살펴봅시다. 두 가지 중요한 점이 있습니다: 첫째, `let x = five();` 줄은 함수의 반환 값을 사용하여 변수를 초기화한다는 것을 보여줍니다. `five` 함수가 `5`를 반환하기 때문에 그 줄은 다음과 같습니다:

```rust
let x = 5;
```

둘째, `five` 함수는 매개변수가 없고 반환 값의 타입을 정의하지만, 함수 본문은 반환하고자 하는 값이기 때문에 세미콜론이 없는 외로운 `5`입니다.

다른 예를 살펴봅시다:

**파일명: src/main.rs**

```rust
fn main() {
    let x = plus_one(5);

    println!("The value of x is: {x}");
}

fn plus_one(x: i32) -> i32 {
    x + 1
}
```

이 코드를 실행하면 `The value of x is: 6`이 출력됩니다. 하지만 `x + 1`을 포함하는 줄 끝에 세미콜론을 배치하여 표현식을 구문으로 바꾸면 어떻게 될까요?

**파일명: src/main.rs**

```rust
fn main() {
    let x = plus_one(5);

    println!("The value of x is: {x}");
}

fn plus_one(x: i32) -> i32 {
    x + 1;
}
```

이 코드를 컴파일하면 다음과 같은 오류가 발생합니다:

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

주요 오류 메시지 `mismatched types`가 이 코드의 핵심 문제를 드러냅니다. `plus_one` 함수의 정의는 `i32`를 반환할 것이라고 말하지만, 구문은 값으로 평가되지 않으며 이는 유닛 타입인 `()`로 표현됩니다. 따라서 아무것도 반환되지 않아 함수 정의와 모순되어 오류가 발생합니다. 이 출력에서 Rust는 이 문제를 수정하는 데 도움이 될 수 있는 메시지를 제공합니다: 세미콜론을 제거하면 오류가 수정됩니다.

---

# 주석

> **원문:** https://doc.rust-lang.org/book/ch03-04-comments.html

모든 프로그래머는 코드를 이해하기 쉽게 만들려고 노력하지만, 때로는 추가 설명이 필요합니다. 이러한 경우 프로그래머는 소스 코드에 *주석*을 남깁니다. 컴파일러는 이를 무시하지만 소스 코드를 읽는 사람들은 유용하게 볼 수 있습니다.

### 간단한 주석

다음은 간단한 주석입니다:

```rust
// hello, world
```

### 관용적인 주석 스타일

Rust에서 관용적인 주석 스타일은 두 개의 슬래시로 주석을 시작하며, 주석은 줄 끝까지 계속됩니다. 한 줄을 넘어 확장되는 주석의 경우 각 줄에 `//`를 포함해야 합니다:

```rust
// So we're doing something complicated here, long enough that we need
// multiple lines of comments to do it! Whew! Hopefully, this comment will
// explain what's going on.
```

### 줄 끝 주석

주석은 코드가 포함된 줄의 끝에도 배치할 수 있습니다:

**파일명: src/main.rs**

```rust
fn main() {
    let lucky_number = 7; // I'm feeling lucky today
}
```

### 선호되는 형식

하지만 더 자주 보게 될 형식은 주석이 주석을 달고 있는 코드 위의 별도 줄에 있는 것입니다:

**파일명: src/main.rs**

```rust
fn main() {
    // I'm feeling lucky today
    let lucky_number = 7;
}
```

### 문서 주석

Rust에는 또 다른 종류의 주석인 **문서 주석**이 있습니다. 이것은 14장의 ["Crates.io에 크레이트 게시하기"](ch14-02-publishing-to-crates-io.html) 섹션에서 논의됩니다.

---

# 제어 흐름

> **원문:** https://doc.rust-lang.org/book/ch03-05-control-flow.html

조건이 `true`인지에 따라 코드를 실행하는 능력과 조건이 `true`인 동안 반복적으로 코드를 실행하는 능력은 대부분의 프로그래밍 언어에서 기본 빌딩 블록입니다. Rust 코드의 실행 흐름을 제어할 수 있게 해주는 가장 일반적인 구조는 `if` 표현식과 루프입니다.

## `if` 표현식

`if` 표현식은 조건에 따라 코드를 분기할 수 있게 해줍니다. 조건을 제공한 다음, "이 조건이 충족되면 이 코드 블록을 실행하고, 조건이 충족되지 않으면 이 코드 블록을 실행하지 마세요"라고 명시합니다.

### 기본 `if` 표현식

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

모든 `if` 표현식은 `if` 키워드로 시작하고 그 뒤에 조건이 옵니다. 이 경우 조건은 변수 `number`가 5보다 작은 값을 가지는지 확인합니다. `if` 표현식의 조건과 연관된 코드 블록을 때때로 *갈래(arms)*라고 합니다.

**중요:** `if` 표현식의 조건은 **반드시** `bool`이어야 합니다. Rust는 Ruby나 JavaScript 같은 언어와 달리 불리언이 아닌 타입을 자동으로 불리언으로 변환하지 않습니다.

비교 연산자를 사용하는 예:

```rust
fn main() {
    let number = 3;

    if number != 0 {
        println!("number was something other than zero");
    }
}
```

### `else if`로 여러 조건 처리하기

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

Rust는 첫 번째 `true` 조건에 대한 블록만 실행하고, 하나를 찾으면 나머지는 확인하지 않습니다.

### `let` 문에서 `if` 사용하기

`if`는 표현식이므로 `let` 문의 오른쪽에 사용하여 결과를 변수에 할당할 수 있습니다:

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

**중요:** `if`의 각 갈래에서 결과가 될 수 있는 값은 같은 타입이어야 합니다:

```rust
fn main() {
    let condition = true;

    let number = if condition { 5 } else { "six" };  // 오류!

    println!("The value of number is: {number}");
}
```

이것은 `if` 갈래가 정수로 평가되고 `else` 갈래가 문자열로 평가되기 때문에 컴파일 오류를 발생시킵니다. 변수는 단일 타입을 가져야 합니다.

## 루프로 반복하기

Rust에는 세 가지 종류의 루프가 있습니다: `loop`, `while`, `for`.

### `loop`로 코드 반복하기

`loop` 키워드는 Rust에게 코드 블록을 영원히 또는 명시적으로 멈추라고 할 때까지 계속 실행하라고 말합니다.

```rust
fn main() {
    loop {
        println!("again!");
    }
}
```

`ctrl-C`를 사용하여 계속되는 루프에 갇힌 프로그램을 중단할 수 있습니다.

#### 루프에서 벗어나기

`break` 키워드를 사용하여 루프 실행을 중지합니다:

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

`break` 뒤에 값을 추가하여 해당 값을 루프 밖으로 반환할 수 있습니다. 또한 `continue`를 사용하여 루프의 다음 반복으로 건너뛸 수 있습니다.

#### 루프 레이블

루프 안에 루프가 있는 경우, `break`와 `continue`는 가장 안쪽 루프에 적용됩니다. *루프 레이블*을 사용하여 어떤 루프에서 벗어나거나 계속할지 지정할 수 있습니다:

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

### `while`로 조건부 루프

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

`while` 구조는 중첩을 제거하고 `loop`, `if`, `else`, `break`를 사용하는 것보다 더 명확합니다.

### `for`로 컬렉션 순회하기

`while` 루프를 사용하여 배열 요소를 순회할 수 있지만, `for` 루프가 더 안전하고 간결합니다:

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

`for` 루프의 안전성과 간결성은 Rust에서 가장 일반적으로 사용되는 루프 구조로 만듭니다.

#### `for`와 범위 사용하기

표준 라이브러리의 `Range`를 사용하여 코드를 특정 횟수 실행할 수 있습니다:

```rust
fn main() {
    for number in (1..4).rev() {
        println!("{number}!");
    }
    println!("LIFTOFF!!!");
}
```

이것은 `(1..4).rev()`를 사용하여 3에서 1까지 카운트다운합니다.

## 요약

이 챕터에서 다룬 내용:

- 변수, 스칼라 및 복합 데이터 타입
- 함수
- 주석
- `if` 표현식
- 루프 (`loop`, `while`, `for`)

다음 챕터에서는 다른 프로그래밍 언어에 일반적으로 존재하지 않는 Rust의 개념인 소유권에 대해 논의합니다.
