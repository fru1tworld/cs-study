# Result로 복구 가능한 에러 처리하기

## 개요

대부분의 에러는 프로그램을 완전히 중단시킬 만큼 심각하지 않습니다. `Result` 열거형(enum)은 함수가 성공 또는 실패를 반환하면서 호출 코드가 어떻게 응답할지 결정하게 합니다.

## Result 열거형 정의

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

**핵심 포인트:**
- `T`: 성공 시 반환되는 값의 타입 (`Ok` 변형)
- `E`: 실패 시 반환되는 에러의 타입 (`Err` 변형)

## match를 사용한 기본 에러 처리

### 예제: 파일 열기

```rust
use std::fs::File;

fn main() {
    let greeting_file_result = File::open("hello.txt");

    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => panic!("Problem opening the file: {error:?}"),
    };
}
```

## 다른 에러에 따라 다르게 처리하기

에러 종류에 따라 다른 방식으로 처리할 수 있습니다:

```rust
use std::fs::File;
use std::io::ErrorKind;

fn main() {
    let greeting_file_result = File::open("hello.txt");

    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => match error.kind() {
            ErrorKind::NotFound => match File::create("hello.txt") {
                Ok(fc) => fc,
                Err(e) => panic!("Problem creating the file: {e:?}"),
            },
            _ => panic!("Problem opening the file: {error:?}"),
        },
    };
}
```

## match의 대안

### 클로저와 함께 `unwrap_or_else` 사용

```rust
use std::fs::File;
use std::io::ErrorKind;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap_or_else(|error| {
        if error.kind() == ErrorKind::NotFound {
            File::create("hello.txt").unwrap_or_else(|error| {
                panic!("Problem creating the file: {error:?}");
            })
        } else {
            panic!("Problem opening the file: {error:?}");
        }
    });
}
```

## 에러 발생 시 패닉을 위한 단축 메서드

### `unwrap()` 메서드

`Ok` 안의 값을 반환하거나 `Err`이면 `panic!`을 호출합니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap();
}
```

에러 출력:
```
thread 'main' panicked at src/main.rs:4:49:
called `Result::unwrap()` on an `Err` value: Os { code: 2, kind: NotFound, message: "No such file or directory" }
```

### `expect()` 메서드

`unwrap()`과 유사하지만 사용자 정의 에러 메시지를 지정할 수 있습니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")
        .expect("hello.txt should be included in this project");
}
```

에러 출력:
```
thread 'main' panicked at src/main.rs:5:10:
hello.txt should be included in this project: Os { code: 2, kind: NotFound, message: "No such file or directory" }
```

**핵심 포인트:**
- 프로덕션 코드에서는 더 나은 디버깅 컨텍스트를 위해 `unwrap()`보다 `expect()`를 선호해야 함
- `expect()`는 에러 발생 위치와 이유를 명확하게 전달

## 에러 전파하기

에러를 직접 처리하지 않고 호출 코드로 반환할 수 있습니다:

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let username_file_result = File::open("hello.txt");

    let mut username_file = match username_file_result {
        Ok(file) => file,
        Err(e) => return Err(e),
    };

    let mut username = String::new();

    match username_file.read_to_string(&mut username) {
        Ok(_) => Ok(username),
        Err(e) => Err(e),
    }
}
```

**핵심 포인트:**
- 함수가 실패할 수 있는 작업을 호출할 때, 에러를 상위로 전파 가능
- 호출자가 에러 처리 방법을 결정하도록 위임

## `?` 연산자

에러 전파를 크게 단순화합니다:

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username_file = File::open("hello.txt")?;
    let mut username = String::new();
    username_file.read_to_string(&mut username)?;
    Ok(username)
}
```

### `?` 연산자의 동작 방식

**핵심 포인트:**
- `Result`가 `Ok`이면 내부 값을 반환하고 계속 진행
- `Result`가 `Err`이면 해당 에러를 함수 전체에서 반환
- 에러 타입은 `From` 트레이트를 통해 자동 변환됨

### `?`를 사용한 메서드 체이닝

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username = String::new();
    File::open("hello.txt")?.read_to_string(&mut username)?;
    Ok(username)
}
```

### 표준 라이브러리의 편의 함수 사용

```rust
use std::fs;
use std::io;

fn read_username_from_file() -> Result<String, io::Error> {
    fs::read_to_string("hello.txt")
}
```

## `?` 연산자를 사용할 수 있는 곳

`?` 연산자는 호환 가능한 반환 타입을 가진 함수에서만 사용할 수 있습니다: `Result`, `Option`, 또는 `FromResidual`을 구현하는 타입.

### 에러: `()`를 반환하는 `main()`에서 `?` 사용

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")?;  // 컴파일 에러
}
```

### 해결책: `main` 반환 타입 변경

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;
    Ok(())
}
```

## Option과 함께 `?` 사용하기

`?` 연산자는 `Option`을 반환하는 함수에서 `Option<T>`와 함께 작동합니다:

```rust
fn last_char_of_first_line(text: &str) -> Option<char> {
    text.lines().next()?.chars().last()
}

fn main() {
    assert_eq!(
        last_char_of_first_line("Hello, world\nHow are you today?"),
        Some('d')
    );

    assert_eq!(last_char_of_first_line(""), None);
    assert_eq!(last_char_of_first_line("\nhi"), None);
}
```

## 반환 타입 호환성

**핵심 포인트:**
- `Result`를 반환하는 함수에서만 `Result`에 `?` 사용 가능
- `Option`을 반환하는 함수에서만 `Option`에 `?` 사용 가능
- `?`로 `Result`와 `Option`을 혼합할 수 없음
- 필요한 경우 `ok()` 또는 `ok_or()` 같은 메서드로 명시적 변환

## main 함수의 반환 타입

Rust에서 `main`은 `Result<(), E>`를 반환할 수 있습니다:

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;
    Ok(())
}
```

**핵심 포인트:**
- `Ok(())`: 실행 파일이 종료 코드 `0`으로 종료
- `Err`: 실행 파일이 0이 아닌 종료 코드로 종료
- 성공한 프로그램은 `0`을 반환하고 에러가 발생한 프로그램은 0이 아닌 값을 반환하는 C 규칙을 따름

## 요약

**Result와 에러 처리 핵심 개념:**
- `Result<T, E>`는 성공(`Ok`)과 실패(`Err`)를 표현하는 열거형
- `match`를 사용하여 각 경우를 명시적으로 처리
- `unwrap()`과 `expect()`는 빠른 프로토타이핑에 유용하지만, 프로덕션에서는 `expect()` 선호
- `?` 연산자는 에러 전파를 간결하게 만듦
- `?`는 `Result`와 `Option`에서 사용 가능하나 혼합 불가
- `main()`도 `Result`를 반환하여 `?` 연산자 사용 가능
