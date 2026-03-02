# 함수

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

함수가 *매개변수*를 갖도록 정의할 수 있습니다. 매개변수는 함수 시그니처의 일부인 특수 변수입니다. 함수에 매개변수가 있으면 해당 매개변수에 구체적인 값을 제공할 수 있습니다. 기술적으로 구체적인 값을 *인수*라고 하지만, 일상적인 대화에서는 함수 정의의 변수나 호출 시 전달되는 구체적인 값 모두에 대해 *매개변수*와 *인수*라는 단어를 혼용하는 경향이 있습니다.

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

은 이 경우 `4`로 평가되는 블록입니다. 그 값은 `let` 구문의 일부로 `y`에 바인딩됩니다. 끝에 세미콜론이 없는 `x + 1` 줄에 주목하세요. 지금까지 본 대부분의 줄과 다릅니다. 표현식은 끝에 세미콜론을 포함하지 않습니다. 표현식 끝에 세미콜론을 추가하면 그것을 구문으로 바꾸고, 그러면 값을 반환하지 않습니다. 다음에 함수 반환 값과 표현식을 탐구할 때 이것을 명심하세요.

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
