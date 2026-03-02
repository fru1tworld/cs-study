# 에러 처리 (Error Handling)

## 개요

에러는 소프트웨어에서 피할 수 없는 현실입니다. Rust는 무언가 잘못될 수 있는 상황을 처리하기 위한 다양한 기능을 제공합니다. 많은 경우 Rust는 에러 가능성을 인지하고 코드가 컴파일되기 전에 조치를 취하도록 요구합니다. 이 요구사항은 프로덕션에 배포하기 전에 에러를 발견하고 적절하게 처리하도록 보장하여 프로그램을 더욱 견고하게 만듭니다.

## 에러의 분류

Rust는 에러를 두 가지 주요 범주로 분류합니다:

### 복구 가능한 에러 (Recoverable Errors)
- 예시: 파일을 찾을 수 없는 에러
- 조치: 사용자에게 문제를 보고하고 작업을 재시도
- 결과: 프로그램 실행을 계속할 수 있음

### 복구 불가능한 에러 (Unrecoverable Errors)
- 예시: 배열 끝을 넘어서 접근 시도
- 조치: 프로그램을 즉시 중단
- 항상 코드의 버그 증상임

## Rust의 에러 처리 방식

대부분의 언어는 이 두 종류의 에러를 구분하지 않고 예외(exception)와 같은 메커니즘으로 동일하게 처리합니다. **Rust는 예외가 없습니다.** 대신 다음을 제공합니다:

1. **`Result<T, E>`** - 복구 가능한 에러를 처리하기 위한 타입
2. **`panic!` 매크로** - 복구 불가능한 에러를 만났을 때 실행을 중단

---

# `panic!`을 이용한 복구 불가능한 에러

## 개요

때때로 코드에서 나쁜 일이 발생하고, 이에 대해 할 수 있는 것이 없습니다. 이런 경우 Rust는 `panic!` 매크로를 제공합니다. 실제로 패닉을 유발하는 두 가지 방법이 있습니다:

1. 코드가 패닉을 유발하는 동작을 수행 (예: 배열 끝을 넘어서 접근)
2. 명시적으로 `panic!` 매크로 호출

기본적으로 이러한 패닉은 실패 메시지를 출력하고, 스택을 되감고(unwind), 정리한 후 종료합니다. 환경 변수를 통해 패닉 발생 시 호출 스택을 표시하도록 설정할 수 있습니다.

## 스택 되감기 vs 중단 (Unwinding vs Aborting)

패닉 발생 시 기본 동작은 **되감기(unwinding)**입니다. Rust가 스택을 거슬러 올라가며 각 함수의 데이터를 정리합니다. 하지만 이 과정은 많은 작업을 필요로 합니다.

대안으로 **중단(aborting)**을 선택할 수 있습니다. 이는 정리 없이 프로그램을 즉시 종료합니다. 운영체제가 메모리를 정리해야 합니다.

바이너리 크기를 최소화해야 한다면 `Cargo.toml`에서 중단 모드로 전환할 수 있습니다:

```toml
[profile.release]
panic = 'abort'
```

## 예제 1: 명시적 `panic!` 호출

```rust
fn main() {
    panic!("crash and burn");
}
```

출력:
```
thread 'main' panicked at src/main.rs:2:5:
crash and burn
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

**핵심 포인트:**
- `panic!` 호출이 에러 메시지를 생성
- 첫 번째 줄은 패닉 메시지와 소스 코드 위치를 표시
- `src/main.rs:2:5`는 두 번째 줄, 다섯 번째 문자를 의미

## 예제 2: 배열 범위를 벗어난 접근

```rust
fn main() {
    let v = vec![1, 2, 3];

    v[99];
}
```

벡터에 세 개의 요소만 있는데 100번째 요소(인덱스 99)에 접근하려고 합니다. Rust는 C와 같은 언어에서 발생하는 **버퍼 오버리드(buffer overread)** 취약점을 방지하기 위해 패닉합니다.

출력:
```
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

## 백트레이스 사용하기

`RUST_BACKTRACE` 환경 변수를 설정하여 백트레이스를 얻을 수 있습니다:

```
$ RUST_BACKTRACE=1 cargo run
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
stack backtrace:
   0: rust_begin_unwind
   ...
   6: panic::main
             at ./src/main.rs:4:6
   ...
```

**핵심 포인트:**
- **백트레이스(backtrace)**는 이 지점까지 호출된 모든 함수의 목록
- 백트레이스를 읽는 핵심: 위에서부터 시작하여 작성한 파일이 보일 때까지 읽기
- 디버그 심볼은 `--release` 플래그 없이 빌드할 때 기본적으로 활성화됨

---

# `Result`를 이용한 복구 가능한 에러

## 개요

대부분의 에러는 프로그램을 완전히 중단해야 할 만큼 심각하지 않습니다. 예를 들어, 파일을 열려고 했는데 파일이 존재하지 않아 실패하면, 프로세스를 종료하는 대신 파일을 생성하고 싶을 수 있습니다.

## Result 열거형

`Result` 열거형은 두 개의 변형(variant)을 가집니다:

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

- `T`: 성공 시 `Ok` 변형 내에서 반환되는 값의 타입
- `E`: 실패 시 `Err` 변형 내에서 반환되는 에러의 타입

## 파일 열기

```rust
use std::fs::File;

fn main() {
    let greeting_file_result = File::open("hello.txt");
}
```

`File::open`의 반환 타입은 `Result<T, E>`입니다:
- `T`: `std::fs::File` (성공 시 파일 핸들)
- `E`: `std::io::Error` (실패 시 에러 타입)

## `match`로 Result 처리하기

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

## 다양한 에러에 대한 매칭

중첩된 `match` 표현식을 사용하여 서로 다른 실패 이유를 다르게 처리할 수 있습니다:

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
            _ => {
                panic!("Problem opening the file: {error:?}");
            }
        },
    };
}
```

### 클로저를 사용한 대안

`unwrap_or_else`와 클로저를 사용:

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

## 에러 시 패닉을 위한 단축 메서드

### `unwrap` 메서드

`Ok`인 경우 내부 값을 반환하고, `Err`인 경우 `panic!`을 호출합니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap();
}
```

### `expect` 메서드

`unwrap`과 유사하지만 사용자 정의 에러 메시지를 지정할 수 있습니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")
        .expect("hello.txt should be included in this project");
}
```

**핵심 포인트:**
- 프로덕션 코드에서는 `unwrap`보다 `expect`를 사용하여 더 나은 디버깅 컨텍스트를 제공
- `expect` 메시지는 에러의 의도와 원인을 명확하게 설명해야 함

## 에러 전파하기 (Propagating Errors)

함수 내에서 에러를 처리하는 대신, 호출 코드에 에러를 반환할 수 있습니다:

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

### `?` 연산자를 이용한 단축 문법

`?` 연산자는 에러 전파를 단순화합니다:

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

**`?` 연산자 동작 방식:**
- `Result`가 `Ok`이면 내부 값을 추출하고 프로그램 계속 진행
- `Result`가 `Err`이면 함수에서 즉시 에러를 반환
- 에러 값은 필요시 `From` 트레이트를 통해 에러 타입 간 변환

### 메서드 체이닝과 `?`

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username = String::new();

    File::open("hello.txt")?.read_to_string(&mut username)?;

    Ok(username)
}
```

### `fs::read_to_string` 사용

이 일반적인 작업에 대한 가장 간결한 접근 방식:

```rust
use std::fs;
use std::io;

fn read_username_from_file() -> Result<String, io::Error> {
    fs::read_to_string("hello.txt")
}
```

## `?` 연산자 사용 위치

`?` 연산자는 반환 타입이 `?`와 함께 사용되는 값과 호환되는 함수에서만 사용할 수 있습니다.

### `main`에서 `Result` 반환

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;

    Ok(())
}
```

**핵심 포인트:**
- `main`이 `Ok(())`를 반환하면 실행 파일은 `0`으로 종료
- `main`이 `Err`를 반환하면 0이 아닌 값으로 종료
- `Box<dyn Error>`는 모든 종류의 에러를 반환 가능

## `Option`에서 `?` 사용

`?` 연산자는 `Option<T>`에서도 유사하게 동작합니다:

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

**참고:** `Result`와 `Option`을 `?`와 혼합할 수 없습니다. `Result`의 `ok()` 메서드나 `Option`의 `ok_or()` 메서드를 사용하여 명시적으로 변환해야 합니다.

---

# `panic!`할 것인가, 말 것인가

## 개요

코드가 패닉하면 복구할 방법이 없습니다. 핵심 결정 포인트: `panic!`을 호출해야 할까요, 아니면 `Result`를 반환해야 할까요?

**핵심 원칙:** 실패할 수 있는 함수를 정의할 때 `Result`를 반환하는 것이 좋은 기본 선택입니다. 호출 코드에 에러를 적절히 처리할 옵션을 제공하기 때문입니다.

## 예제, 프로토타입 코드, 테스트에서 사용

다음 상황에서 `panic!` (또는 `unwrap`, `expect`)을 사용합니다:

- **예제:** 강력한 에러 처리 코드를 포함하면 예제가 덜 명확해짐. `unwrap`은 실제 에러 처리를 위한 플레이스홀더 역할
- **프로토타입 코드:** 에러 처리 방법을 아직 결정하지 않았을 때 `unwrap`과 `expect`가 유용. 프로그램을 더 견고하게 만들 때를 위한 명확한 마커
- **테스트:** 테스트에서 메서드 호출이 실패하면 전체 테스트가 실패해야 함. `unwrap`이나 `expect` 호출이 적절한 동작

## 컴파일러보다 더 많은 정보를 가진 경우

`Result`가 `Ok` 값을 가질 것이라는 로직이 있지만 컴파일러가 이해할 수 없을 때 `expect`를 호출하는 것이 적절합니다:

```rust
fn main() {
    use std::net::IpAddr;

    let home: IpAddr = "127.0.0.1"
        .parse()
        .expect("Hardcoded IP address should be valid");
}
```

`127.0.0.1`은 하드코딩되어 있으므로 유효하다는 것을 알고 있습니다. `expect` 메시지에 이유를 문서화하세요.

## 에러 처리 가이드라인

### 패닉해야 할 때

코드가 **나쁜 상태(bad state)**에 빠질 수 있고 (가정, 보장, 계약, 불변성이 깨질 때) 다음 조건에 해당하면 패닉하는 것이 좋습니다:

- 나쁜 상태가 **예상치 못한 것**일 때 (잘못된 사용자 입력처럼 가끔 발생하는 것이 아님)
- 이 지점 이후의 코드가 매 단계마다 검사하는 대신 **이 나쁜 상태가 아님을 가정**할 때
- 이 정보를 **타입으로 인코딩할 좋은 방법이 없을 때**

**패닉이 적절한 경우:**
- 계속 진행하면 안전하지 않거나 해로울 때
- 호출 코드가 유효하지 않은 값을 전달할 때 - 개발 중 프로그래머에게 버그 수정을 알림
- 외부 코드 의존성이 수정할 수 없는 유효하지 않은 상태를 반환할 때
- 계약 위반이 발생할 때 (계약 위반은 호출자 측 버그를 나타냄)
- 범위를 벗어난 메모리 접근이 시도될 때 (보안 문제)

### Result를 반환해야 할 때

**실패가 예상되는 경우** `Result`를 반환합니다:
- 파서가 잘못된 형식의 데이터를 받을 때
- HTTP 요청이 속도 제한 상태를 반환할 때
- 사용자 입력이 유효하지 않을 때

`Result`를 반환하면 실패가 호출 코드가 반드시 처리해야 하는 예상된 가능성임을 나타냅니다.

## 유효성 검사를 위한 사용자 정의 타입

Rust의 타입 시스템을 사용하여 컴파일 타임에 유효한 값을 강제합니다. 모든 함수에서 런타임 검사를 하는 대신, 생성 시 유효성을 검사하는 사용자 정의 타입을 만듭니다.

**예제: Guess 타입**

```rust
pub struct Guess {
    value: i32,
}

impl Guess {
    pub fn new(value: i32) -> Guess {
        if value < 1 || value > 100 {
            panic!("Guess value must be between 1 and 100, got {value}.");
        }

        Guess { value }
    }

    pub fn value(&self) -> i32 {
        self.value
    }
}
```

**주요 이점:**
- `value` 필드가 비공개이므로 `Guess::new()` 사용을 강제
- 유효성 검사는 생성 시 한 번만 수행
- 함수는 추가 검사 없이 시그니처에 `Guess`를 사용 가능
- 타입 시스템이 유효성을 보장

`Guess`를 받는 함수는 값이 1에서 100 사이임을 확신하고 진행할 수 있습니다 - 컴파일러가 이를 보장합니다.

## Rust의 타입 시스템 활용

타입을 사용하여 컴파일 타임에 유효하지 않은 상태를 방지합니다:
- 음수 값이 불가능해야 하면 `i32` 대신 `u32` 사용
- 무언가가 항상 존재해야 하면 `Option` 대신 전용 타입 사용
- 컴파일러가 런타임 검사를 요구하는 대신 유효하지 않은 코드가 컴파일되지 않도록 방지

---

## 요약

Rust의 에러 처리 기능은 견고한 코드를 작성하는 데 도움을 줍니다:

| 기능 | 용도 | 설명 |
|------|------|------|
| `panic!` | 복구 불가능한 상태 | 실행을 중단하고 프로그램 종료 |
| `Result` | 복구 가능한 에러 | 호출 코드가 에러 처리 방법 결정 |

**핵심 포인트:**
- `panic!`은 버그나 계속 진행할 수 없는 상태에 사용
- `Result`는 예상 가능한 실패에 사용하여 호출자에게 선택권 제공
- `?` 연산자로 에러 전파를 간결하게 처리
- 사용자 정의 타입으로 유효성을 타입 시스템에 인코딩
- `expect`를 사용하여 의도를 문서화하고 디버깅 용이하게
