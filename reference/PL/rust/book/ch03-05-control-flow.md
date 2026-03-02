# 제어 흐름

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
