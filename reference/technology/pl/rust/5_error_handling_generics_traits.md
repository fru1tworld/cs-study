# 러스트 오류 처리, 제네릭, 트레이트, 라이프타임

## 오류 처리 (Error Handling)

> **원문:** https://doc.rust-lang.org/book/ch09-00-error-handling.html

### 개요

- 오류는 소프트웨어에서 피할 수 없는 현실
- Rust는 오류 상황 처리를 위한 다양한 기능 제공
- 많은 경우 오류 가능성을 인지하고 컴파일 전 조치를 요구 → 배포 전 오류 발견·처리 보장 → 프로그램 견고성 향상

### 오류의 분류

- Rust는 오류를 두 주요 범주로 분류

#### 복구 가능한 오류 (Recoverable Errors)
- 예시: 파일을 찾을 수 없는 오류
- 조치: 사용자에게 문제를 보고하고 작업을 재시도
- 결과: 프로그램 실행을 계속할 수 있음

#### 복구 불가능한 오류 (Unrecoverable Errors)
- 예시: 배열 끝을 넘어서 접근 시도
- 조치: 프로그램을 즉시 중단
- 항상 코드의 버그 증상임

### Rust의 오류 처리 방식

- 대부분의 언어는 두 오류 종류를 구분하지 않고 예외(exception) 메커니즘으로 동일하게 처리
- Rust는 예외가 없음 → 대신 다음을 제공

1. **`Result<T, E>`** - 복구 가능한 오류를 처리하기 위한 타입
2. **`panic!` 매크로** - 복구 불가능한 오류를 만났을 때 실행을 중단

---

## `panic!`을 이용한 복구 불가능한 오류

### 개요

- 때때로 코드에서 나쁜 일이 발생하고 이에 대해 손쓸 방법이 없음 → Rust는 이런 경우를 위해 `panic!` 매크로 제공
- 패닉을 유발하는 두 방법
  1. 코드가 패닉을 유발하는 동작을 수행 (예: 배열 끝을 넘어서 접근)
  2. 명시적으로 `panic!` 매크로 호출
- 기본 동작: 실패 메시지 출력 → 스택을 되감고(unwind) 정리 후 종료
- 환경 변수로 패닉 발생 시 호출 스택 표시 설정 가능

### 스택 되감기 vs 중단 (Unwinding vs Aborting)

- 기본 동작은 **되감기**(unwinding): Rust가 스택을 거슬러 올라가며 각 함수의 데이터를 정리 → 작업량이 많음
- 대안은 **중단**(aborting): 정리 없이 프로그램 즉시 종료 → 메모리 정리는 운영체제가 담당
- 바이너리 크기를 최소화해야 한다면 `Cargo.toml`에서 중단 모드로 전환 가능:

```toml
[profile.release]
panic = 'abort'
```

### 예제 1: 명시적 `panic!` 호출

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
- `panic!` 호출이 오류 메시지를 생성
- 첫 번째 줄은 패닉 메시지와 소스 코드 위치를 표시
- `src/main.rs:2:5`는 두 번째 줄, 다섯 번째 문자를 의미

### 예제 2: 배열 범위를 벗어난 접근

```rust
fn main() {
    let v = vec![1, 2, 3];

    v[99];
}
```

- 벡터에 요소 3개만 있는데 100번째 요소(인덱스 99)에 접근 시도
- C 같은 언어에서 발생하는 버퍼 오버리드(buffer overread) 취약점 방지 위해 Rust는 패닉

출력:
```
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

### 백트레이스 사용하기

- `RUST_BACKTRACE` 환경 변수를 설정하면 백트레이스를 얻을 수 있음

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
- **백트레이스**(backtrace)는 이 지점까지 호출된 모든 함수의 목록
- 백트레이스를 읽는 핵심: 위에서부터 시작하여 작성한 파일이 보일 때까지 읽기
- 디버그 심볼은 `--release` 플래그 없이 빌드할 때 기본적으로 활성화됨

---

## `Result`를 이용한 복구 가능한 오류

### 개요

- 대부분의 오류는 프로그램을 완전히 중단해야 할 만큼 심각하지 않음
- 예: 파일을 열려고 했는데 파일이 없어 실패한 경우 → 프로세스 종료 대신 파일 생성을 원할 수 있음

### Result 열거형

- `Result` 열거형은 변형(variant) 두 개를 가짐:

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

- `T`: 성공 시 `Ok` 변형 내에서 반환되는 값의 타입
- `E`: 실패 시 `Err` 변형 내에서 반환되는 오류의 타입

### 파일 열기

```rust
use std::fs::File;

fn main() {
    let greeting_file_result = File::open("hello.txt");
}
```

- `File::open`의 반환 타입: `Result<T, E>`
- `T`: `std::fs::File` (성공 시 파일 핸들)
- `E`: `std::io::Error` (실패 시 오류 타입)

### `match`로 Result 처리하기

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

### 다양한 오류에 대한 매칭

- 중첩된 `match` 표현식으로 서로 다른 실패 이유를 다르게 처리 가능

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

#### 클로저를 사용한 대안

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

### 오류 시 패닉을 위한 단축 메서드

#### `unwrap` 메서드

- `Ok`이면 내부 값을 반환, `Err`이면 `panic!` 호출:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap();
}
```

#### `expect` 메서드

- `unwrap`과 유사하지만 사용자 정의 오류 메시지 지정 가능:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")
        .expect("hello.txt should be included in this project");
}
```

**핵심 포인트:**
- 프로덕션 코드에서는 `unwrap`보다 `expect`를 사용하여 더 나은 디버깅 컨텍스트를 제공
- `expect` 메시지는 오류의 의도와 원인을 명확하게 설명해야 함

### 오류 전파하기 (Propagating Errors)

- 함수 내에서 오류를 처리하는 대신, 호출 코드에 오류 반환 가능:

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

#### `?` 연산자를 이용한 단축 문법

- `?` 연산자는 오류 전파를 단순화:

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
- `Result`가 `Err`이면 함수에서 즉시 오류를 반환
- 오류 값은 필요시 `From` 트레이트를 통해 오류 타입 간 변환

#### 메서드 체이닝과 `?`

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username = String::new();

    File::open("hello.txt")?.read_to_string(&mut username)?;

    Ok(username)
}
```

#### `fs::read_to_string` 사용

이 일반적인 작업에 대한 가장 간결한 접근 방식:

```rust
use std::fs;
use std::io;

fn read_username_from_file() -> Result<String, io::Error> {
    fs::read_to_string("hello.txt")
}
```

### `?` 연산자 사용 위치

- `?` 연산자는 반환 타입이 `?`와 함께 사용되는 값과 호환되는 함수에서만 사용 가능

#### `main`에서 `Result` 반환

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
- `Box<dyn Error>`는 모든 종류의 오류를 반환 가능

### `Option`에서 `?` 사용

- `?` 연산자는 `Option<T>`에서도 유사하게 동작:

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

- 참고: `Result`와 `Option`은 `?`와 혼합 불가 → `Result`의 `ok()` 메서드나 `Option`의 `ok_or()` 메서드로 명시적 변환 필요

---

## `panic!`할 것인가, 말 것인가

### 개요

- 코드가 패닉하면 복구 불가 → 핵심 결정: `panic!` 호출 vs `Result` 반환
- 핵심 원칙: 실패할 수 있는 함수를 정의할 때 `Result` 반환이 좋은 기본 선택 → 호출 코드에 오류 처리 옵션을 제공하기 때문

### 예제, 프로토타입 코드, 테스트에서 사용

- 다음 상황에서는 `panic!` (또는 `unwrap`, `expect`) 사용:

- **예제:** 강력한 오류 처리 코드를 포함하면 예제가 덜 명확해짐. `unwrap`은 실제 오류 처리를 위한 플레이스홀더 역할
- **프로토타입 코드:** 오류 처리 방법을 아직 결정하지 않았을 때 `unwrap`과 `expect`가 유용. 프로그램을 더 견고하게 만들 때를 위한 명확한 마커
- **테스트:** 테스트에서 메서드 호출이 실패하면 전체 테스트가 실패해야 함. `unwrap`이나 `expect` 호출이 적절한 동작

### 컴파일러보다 더 많은 정보를 가진 경우

- `Result`가 `Ok` 값을 가질 것이라는 로직이 있지만 컴파일러가 이해할 수 없을 때 `expect` 호출이 적절:

```rust
fn main() {
    use std::net::IpAddr;

    let home: IpAddr = "127.0.0.1"
        .parse()
        .expect("Hardcoded IP address should be valid");
}
```

- `127.0.0.1`은 하드코딩되어 있어 유효함을 이미 알고 있는 경우 → `expect` 메시지에 이유를 문서화

### 오류 처리 가이드라인

#### 패닉해야 할 때

- 코드가 나쁜 상태(bad state, 가정·보장·계약·불변성이 깨진 상태)에 빠질 수 있고 다음 조건에 해당하면 패닉이 적절:

- 나쁜 상태가 **예상치 못한 것**일 때 (잘못된 사용자 입력처럼 가끔 발생하는 것이 아님)
- 이 지점 이후의 코드가 매 단계마다 검사하는 대신 **이 나쁜 상태가 아님을 가정**할 때
- 이 정보를 **타입으로 인코딩할 좋은 방법이 없을 때**

**패닉이 적절한 경우:**
- 계속 진행하면 안전하지 않거나 해로울 때
- 호출 코드가 유효하지 않은 값을 전달할 때 - 개발 중 프로그래머에게 버그 수정을 알림
- 외부 코드 의존성이 수정할 수 없는 유효하지 않은 상태를 반환할 때
- 계약 위반이 발생할 때 (계약 위반은 호출자 측 버그를 나타냄)
- 범위를 벗어난 메모리 접근이 시도될 때 (보안 문제)

#### Result를 반환해야 할 때

- 실패가 예상되는 경우 `Result` 반환:
  - 파서가 잘못된 형식의 데이터를 받을 때
  - HTTP 요청이 속도 제한 상태를 반환할 때
  - 사용자 입력이 유효하지 않을 때
- `Result` 반환 → 실패가 호출 코드가 반드시 처리해야 하는 예상된 가능성임을 나타냄

### 유효성 검사를 위한 사용자 정의 타입

- Rust의 타입 시스템으로 컴파일 타임에 유효한 값을 강제
- 모든 함수에서 런타임 검사를 하는 대신, 생성 시 유효성을 검사하는 사용자 정의 타입을 만드는 방식

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

- `Guess`를 받는 함수는 값이 1~100 사이임을 확신하고 진행 가능 → 컴파일러가 이를 보장

### Rust의 타입 시스템 활용

- 타입을 사용해 컴파일 타임에 유효하지 않은 상태를 방지:
- 음수 값이 불가능해야 하면 `i32` 대신 `u32` 사용
- 무언가가 항상 존재해야 하면 `Option` 대신 전용 타입 사용
- 컴파일러가 런타임 검사를 요구하는 대신 유효하지 않은 코드가 컴파일되지 않도록 방지

---

### 요약

- Rust의 오류 처리 기능은 견고한 코드 작성에 도움
  - `panic!`: 용도는 복구 불가능한 상태 → 실행을 중단하고 프로그램 종료
  - `Result`: 용도는 복구 가능한 오류 → 호출 코드가 오류 처리 방법 결정

**핵심 포인트:**
- `panic!`은 버그나 계속 진행할 수 없는 상태에 사용
- `Result`는 예상 가능한 실패에 사용하여 호출자에게 선택권 제공
- `?` 연산자로 오류 전파를 간결하게 처리
- 사용자 정의 타입으로 유효성을 타입 시스템에 인코딩
- `expect`를 사용하여 의도를 문서화하고 디버깅 용이하게

---

## `panic!`으로 복구 불가능한 오류 처리하기

> **원문:** https://doc.rust-lang.org/book/ch09-01-unrecoverable-errors-with-panic.html

### 개요

- 코드에서 처리할 수 없는 나쁜 일이 발생할 때가 있음 → 이런 경우를 위해 Rust는 `panic!` 매크로 제공
- 패닉을 발생시키는 방법 두 가지:

1. 코드가 패닉을 발생시키는 동작 수행 (예: 배열 끝을 넘어 접근)
2. `panic!` 매크로를 명시적으로 호출

기본적으로 패닉이 발생하면:
- 실패 메시지 출력
- 스택을 되감고(unwind) 정리
- 프로그램 종료

- `RUST_BACKTRACE` 환경 변수 설정 시 패닉 발생 시 호출 스택 표시 가능

### 스택 되감기(Unwinding) vs 중단(Abort)

#### 기본 동작: 되감기(Unwinding)

- 패닉 발생 시 프로그램은 되감기(unwinding) 시작 → Rust가 스택을 거슬러 올라가며 만나는 각 함수의 데이터를 정리

#### 대안적 동작: 중단(Abort)

- 스택을 거슬러 올라가며 정리하는 작업은 비용이 큼 → Rust는 정리 없이 즉시 중단(abort)하여 프로그램을 종료하는 선택지도 제공
- 메모리 정리는 운영 체제가 담당

#### 설정 방법

- 결과 바이너리를 최대한 작게 만들려면, *Cargo.toml*의 `[profile]` 섹션에 `panic = 'abort'`를 추가해 되감기에서 중단으로 전환 가능:

```toml
[profile.release]
panic = 'abort'
```

**핵심 포인트:**
- 되감기(Unwinding): 스택을 거슬러 올라가며 데이터 정리 (기본값)
- 중단(Abort): 정리 없이 즉시 종료, OS가 메모리 정리
- 바이너리 크기를 줄이려면 `panic = 'abort'` 사용

### 예제 1: 간단한 패닉

**파일명: src/main.rs**

```rust
fn main() {
    panic!("crash and burn");
}
```

**출력:**

```
$ cargo run
   Compiling panic v0.1.0 (file:///projects/panic)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.25s
     Running `target/debug/panic`

thread 'main' panicked at src/main.rs:2:5:
crash and burn
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

**오류 메시지 구성:**
- 패닉 메시지: "crash and burn"
- 위치: *src/main.rs:2:5* (2번째 줄, 5번째 문자)

### 예제 2: 잘못된 벡터 접근으로 인한 패닉

**파일명: src/main.rs**

```rust
fn main() {
    let v = vec![1, 2, 3];

    v[99];
}
```

- 요소 3개만 있는 벡터에서 100번째 요소(인덱스 99)에 접근하려 하여 패닉 발생

**출력:**

```
$ cargo run
   Compiling panic v0.1.0 (file:///projects/panic)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.27s
     Running `target/debug/panic`

thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

#### Rust가 패닉을 발생시키는 이유

- C 언어는 데이터 구조의 끝을 넘어 읽는 것이 정의되지 않은 동작(undefined behavior) → 해당 메모리 위치의 값을 얻게 됨 → 이를 버퍼 오버리드(buffer overread)라 하며 보안 취약점으로 이어질 수 있음
- Rust는 이를 보호: 잘못된 인덱스로 읽으려 하면 실행을 멈추고 진행하지 않음

**핵심 포인트:**
- C: 범위 밖 접근 시 정의되지 않은 동작 (보안 취약점 가능)
- Rust: 범위 밖 접근 시 즉시 패닉 (안전성 보장)
- 버퍼 오버리드 공격 방지

### 백트레이스(Backtrace) 사용하기

- 백트레이스를 얻으려면 `RUST_BACKTRACE` 환경 변수를 설정:

```bash
$ RUST_BACKTRACE=1 cargo run
```

**백트레이스 출력 예시:**

```
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
stack backtrace:
   0: rust_begin_unwind
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/std/src/panicking.rs:692:5
   1: core::panicking::panic_fmt
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/core/src/panicking.rs:75:14
   2: core::panicking::panic_bounds_check
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/core/src/panicking.rs:273:5
   3: <usize as core::slice::index::SliceIndex<[T]>>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/slice/index.rs:274:10
   4: core::slice::index::<impl core::ops::index::Index<I> for [T]>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/slice/index.rs:16:9
   5: <alloc::vec::Vec<T,A> as core::ops::index::Index<I>>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/alloc/src/vec/mod.rs:3361:9
   6: panic::main
             at ./src/main.rs:4:6
   7: core::ops::function::FnOnce::call_once
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/ops/function.rs:250:5

note: Some details are omitted, run with `RUST_BACKTRACE=full` for a verbose backtrace.
```

#### 백트레이스 읽는 방법

1. 위에서부터 시작해 작성한 파일이 나올 때까지 읽기
2. 그곳이 문제가 발생한 지점
3. 위의 줄들 = 내 코드가 호출한 코드
4. 아래의 줄들 = 내 코드를 호출한 코드

위 예시에서 **6번 줄**이 사용자 코드의 실제 문제 지점을 가리킵니다: *src/main.rs:4:6*

**핵심 포인트:**
- 백트레이스는 문제 발생 시점까지의 함수 호출 경로를 보여줌
- 직접 작성한 코드 파일을 찾아서 문제 지점 파악
- `RUST_BACKTRACE=full`로 더 자세한 정보 확인 가능

### 디버깅 팁

- `cargo build` 또는 `cargo run`(--release 없이)을 사용하면 디버그 심볼이 기본으로 활성화됨
- 더 자세한 출력은 `RUST_BACKTRACE=full` 사용
- 코드가 패닉을 발생시키면, 어떤 동작과 값이 패닉을 유발했는지, 코드가 대신 무엇을 해야 하는지 파악해야 함

**핵심 포인트:**
- 디버그 빌드에서 디버그 심볼 자동 활성화
- 패닉 발생 시: 원인 파악 -> 올바른 동작 결정 -> 코드 수정
- 릴리스 빌드(`--release`)에서는 디버그 정보가 제거됨

### 요약

- `panic!` 매크로: 복구 불가능한 오류 발생 시 프로그램 종료
- 되감기(Unwinding): 스택을 거슬러 올라가며 정리 (기본값)
- 중단(Abort): 정리 없이 즉시 종료, 바이너리 크기 감소
- 백트레이스: `RUST_BACKTRACE=1`로 호출 스택 확인
- 안전성: 잘못된 메모리 접근 시 패닉으로 보안 보장

---

## Result로 복구 가능한 오류 처리하기

> **원문:** https://doc.rust-lang.org/book/ch09-02-recoverable-errors-with-result.html

### 개요

- 대부분의 오류는 프로그램을 완전히 중단시킬 만큼 심각하지 않음
- `Result` 열거형(enum)은 함수가 성공 또는 실패를 반환 → 호출 코드가 응답 방식을 결정

### Result 열거형 정의

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

**핵심 포인트:**
- `T`: 성공 시 반환되는 값의 타입 (`Ok` 변형)
- `E`: 실패 시 반환되는 오류의 타입 (`Err` 변형)

### match를 사용한 기본 오류 처리

#### 예제: 파일 열기

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

### 다른 오류에 따라 다르게 처리하기

- 오류 종류에 따라 다른 방식으로 처리 가능:

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

### match의 대안

#### 클로저와 함께 `unwrap_or_else` 사용

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

### 오류 발생 시 패닉을 위한 단축 메서드

#### `unwrap()` 메서드

- `Ok` 안의 값을 반환하거나 `Err`이면 `panic!` 호출:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap();
}
```

오류 출력:
```
thread 'main' panicked at src/main.rs:4:49:
called `Result::unwrap()` on an `Err` value: Os { code: 2, kind: NotFound, message: "No such file or directory" }
```

#### `expect()` 메서드

- `unwrap()`과 유사하지만 사용자 정의 오류 메시지 지정 가능:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")
        .expect("hello.txt should be included in this project");
}
```

오류 출력:
```
thread 'main' panicked at src/main.rs:5:10:
hello.txt should be included in this project: Os { code: 2, kind: NotFound, message: "No such file or directory" }
```

**핵심 포인트:**
- 프로덕션 코드에서는 더 나은 디버깅 컨텍스트를 위해 `unwrap()`보다 `expect()`를 선호해야 함
- `expect()`는 오류 발생 위치와 이유를 명확하게 전달

### 오류 전파하기

- 오류를 직접 처리하지 않고 호출 코드로 반환 가능:

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
- 함수가 실패할 수 있는 작업을 호출할 때, 오류를 상위로 전파 가능
- 호출자가 오류 처리 방법을 결정하도록 위임

### `?` 연산자

- 오류 전파를 크게 단순화:

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

#### `?` 연산자의 동작 방식

**핵심 포인트:**
- `Result`가 `Ok`이면 내부 값을 반환하고 계속 진행
- `Result`가 `Err`이면 해당 오류를 함수 전체에서 반환
- 오류 타입은 `From` 트레이트를 통해 자동 변환됨

#### `?`를 사용한 메서드 체이닝

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username = String::new();
    File::open("hello.txt")?.read_to_string(&mut username)?;
    Ok(username)
}
```

#### 표준 라이브러리의 편의 함수 사용

```rust
use std::fs;
use std::io;

fn read_username_from_file() -> Result<String, io::Error> {
    fs::read_to_string("hello.txt")
}
```

### `?` 연산자를 사용할 수 있는 곳

- `?` 연산자는 호환 가능한 반환 타입을 가진 함수에서만 사용 가능: `Result`, `Option`, 또는 `FromResidual`을 구현하는 타입

#### 오류: `()`를 반환하는 `main()`에서 `?` 사용

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")?;  // 컴파일 오류
}
```

#### 해결책: `main` 반환 타입 변경

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;
    Ok(())
}
```

### Option과 함께 `?` 사용하기

- `?` 연산자는 `Option`을 반환하는 함수에서 `Option<T>`와 함께 작동:

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

### 반환 타입 호환성

**핵심 포인트:**
- `Result`를 반환하는 함수에서만 `Result`에 `?` 사용 가능
- `Option`을 반환하는 함수에서만 `Option`에 `?` 사용 가능
- `?`로 `Result`와 `Option`을 혼합할 수 없음
- 필요한 경우 `ok()` 또는 `ok_or()` 같은 메서드로 명시적 변환

### main 함수의 반환 타입

- Rust에서 `main`은 `Result<(), E>` 반환 가능:

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
- 성공한 프로그램은 `0`을 반환하고 오류가 발생한 프로그램은 0이 아닌 값을 반환하는 C 규칙을 따름

### 요약

**Result와 오류 처리 핵심 개념:**
- `Result<T, E>`는 성공(`Ok`)과 실패(`Err`)를 표현하는 열거형
- `match`를 사용하여 각 경우를 명시적으로 처리
- `unwrap()`과 `expect()`는 빠른 프로토타이핑에 유용하지만, 프로덕션에서는 `expect()` 선호
- `?` 연산자는 오류 전파를 간결하게 만듦
- `?`는 `Result`와 `Option`에서 사용 가능하나 혼합 불가
- `main()`도 `Result`를 반환하여 `?` 연산자 사용 가능

---

## `panic!`을 사용할지 말지 결정하기

> **원문:** https://doc.rust-lang.org/book/ch09-03-to-panic-or-not-to-panic.html

### 개요

- 이 장은 Rust 오류 처리에서 `panic!`을 사용할지 `Result`를 반환할지 결정하는 방법을 다룸
- 핵심 원칙: 코드가 패닉을 일으키면 복구 불가 → `Result` 반환 시 호출 코드에 오류를 적절히 처리할 옵션을 제공

> 실패할 수 있는 함수를 정의할 때는 `Result`를 반환하는 것이 좋은 기본 선택

---

### `panic!` vs `Result` 사용 시점

#### 예제, 프로토타입 코드, 테스트

- 다음 상황에서는 `panic!` 사용:
  - 예제: 강력한 오류 처리 코드를 포함하면 예제가 덜 명확해질 수 있음 → `unwrap` 같은 메서드는 오류 처리가 플레이스홀더임을 나타냄
  - 프로토타이핑: `unwrap`과 `expect`는 오류 처리 방법을 아직 결정하지 않았을 때 유용
  - 테스트: 메서드 호출이 실패하면 전체 테스트가 실패해야 함 → `unwrap`이나 `expect` 호출이 적절 (`panic!`이 테스트 실패를 표시하기 때문)

#### 컴파일러보다 더 많은 정보가 있을 때

- `Result`가 `Ok`가 될 것이라는 로직이 있지만 컴파일러가 이해할 수 없는 경우 `expect` 호출이 적절

```rust
fn main() {
    use std::net::IpAddr;

    let home: IpAddr = "127.0.0.1"
        .parse()
        .expect("Hardcoded IP address should be valid");
}
```

**핵심 포인트:**
- 컴파일러는 `"127.0.0.1"`이 항상 유효한지 판단할 수 없지만, 개발자는 수동으로 검증할 수 있음
- `expect` 메시지에 이유를 문서화하는 것이 좋음

---

### 오류 처리 가이드라인

#### 패닉을 사용해야 하는 경우

- 코드가 잘못된 상태(bad state, 가정·보장·계약·불변성(invariant)이 위반된 상태)에 놓일 수 있을 때 패닉 사용:

1. 잘못된 상태가 **예상치 못한 것**일 때 (잘못된 사용자 입력처럼 가끔 발생하는 것이 아닌 경우)
2. 이 시점 이후의 코드가 **이 잘못된 상태가 아님에 의존**해야 할 때
3. 이 정보를 **타입 시스템에 인코딩할 좋은 방법이 없을** 때

**패닉 시나리오:**
- 코드에 잘못된 값이 전달된 경우
- 계속 진행하면 보안에 문제가 되거나 해로울 경우
- 외부 코드가 고칠 수 없는 잘못된 상태를 반환하는 경우
- 사용자에게 해를 끼칠 수 있는 작업(예: 메모리 접근) 전에 값을 검증할 때

> 계약(contract) 위반 시 패닉을 일으키는 것이 합리적 — 계약 위반은 항상 호출자 측 버그를 나타내기 때문

#### `Result`를 반환해야 하는 경우

- 실패가 예상되는 경우 `Result` 반환:
  - 파서가 잘못된 형식의 데이터를 받는 경우
  - HTTP 요청이 속도 제한 상태를 반환하는 경우
- `Result` 반환 → 실패가 예상 가능한 상황임을 나타냄

---

### 유효성 검증을 위한 커스텀 타입

- 모든 함수에서 런타임 검사 대신, 생성 시 유효성을 검사하는 커스텀 타입을 만들어 Rust의 타입 시스템 활용 가능

#### 예제: Guess 타입

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

**핵심 이점:**
- `value` 필드는 **private**이므로 `Guess::new`를 통해서만 생성 가능
- 유효성 검사가 한 곳에서 이루어지며 코드 전체에 분산되지 않음
- 함수가 시그니처에서 `Guess`를 받을 수 있어 런타임 검사가 불필요
- 타입 시스템이 유효한 값만 존재함을 보장

#### 이 타입을 사용하는 함수

```rust
fn some_function(guess: Guess) {
    // guess.value()가 1과 100 사이임을 안전하게 사용할 수 있음
}
```

---

### 요약

- 정말로 복구할 수 없는 오류와 계약 위반에는 `panic!`을 사용
- 잠재적으로 복구 가능한 오류에는 기본적으로 `Result`를 반환
- Rust의 타입 시스템을 활용하여 컴파일 타임에 잘못된 상태를 방지
- 유효성 검사가 포함된 커스텀 타입을 만들어 반복적인 런타임 검사를 제거

---

## 제네릭, 트레이트, 라이프타임

## 제네릭 타입, 트레이트, 그리고 라이프타임

> **원문:** https://doc.rust-lang.org/book/ch10-00-generics.html

### 개요

- 모든 프로그래밍 언어에는 개념의 중복을 효과적으로 처리하기 위한 도구가 있음
- Rust의 도구 중 하나가 제네릭(Generics): 구체적인 타입이나 다른 속성의 추상적인 대역
  - 컴파일·실행 시 그 자리에 무엇이 들어갈지 모르는 상태에서도 제네릭의 동작이나 다른 제네릭과의 관계 표현 가능
- 함수는 `i32`나 `String` 같은 구체적인 타입 대신 제네릭 타입의 매개변수를 받을 수 있음 → 알 수 없는 값을 가진 매개변수를 받아 여러 구체적인 값에 대해 동일한 코드를 실행하는 것과 같음

**이미 사용해본 제네릭:**
- 6장의 `Option<T>`
- 8장의 `Vec<T>`와 `HashMap<K, V>`
- 9장의 `Result<T, E>`

- 이 장에서는 자신만의 타입, 함수, 메서드를 제네릭으로 정의하는 방법을 다룸

### 다루는 주제

1. **함수 추출로 중복 제거** - 코드 중복을 줄이기 위해 함수를 추출하는 방법 복습
2. **제네릭 함수** - 매개변수 타입만 다른 두 함수에서 제네릭 함수를 만드는 동일한 기법 사용
3. **구조체와 열거형의 제네릭 타입** - 구조체와 열거형 정의에서 제네릭 타입을 사용하는 방법
4. **트레이트(Traits)** - 동작을 제네릭 방식으로 정의하고, 트레이트와 제네릭 타입을 결합하여 제네릭 타입을 제약하는 방법
5. **라이프타임(Lifetimes)** - 참조들이 서로 어떻게 관련되는지 컴파일러에게 정보를 제공하는 제네릭의 일종

### 함수 추출로 중복 제거하기

#### 초기 코드 - 가장 큰 숫자 찾기

**Listing 10-1**: 숫자 목록에서 가장 큰 숫자 찾기

```rust
fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");
    assert_eq!(*largest, 100);
}
```

- 정수 목록을 `number_list` 변수에 저장 → 목록의 첫 번째 숫자에 대한 참조를 `largest` 변수에 저장
- 목록의 모든 숫자를 순회 → 현재 숫자가 `largest`보다 크면 참조 교체
- 모든 숫자를 확인한 후 `largest`는 가장 큰 숫자(이 경우 100)를 참조

#### 중복 코드 문제

**Listing 10-2**: *두 개의* 숫자 목록에서 가장 큰 숫자를 찾는 코드

```rust
fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");

    let number_list = vec![102, 34, 6000, 89, 54, 2, 43, 8];

    let mut largest = &number_list[0];

    for number in &number_list {
        if number > largest {
            largest = number;
        }
    }

    println!("The largest number is {largest}");
}
```

- 이 코드는 작동하지만, 코드 중복은 지루하고 오류가 발생하기 쉬움
- 변경 시 여러 곳에서 코드를 업데이트해야 한다는 점을 기억해야 함

#### 함수 추출하기

**Listing 10-3**: 두 목록에서 가장 큰 숫자를 찾는 추상화된 코드

```rust
fn largest(list: &[i32]) -> &i32 {
    let mut largest = &list[0];

    for item in list {
        if item > largest {
            largest = item;
        }
    }

    largest
}

fn main() {
    let number_list = vec![34, 50, 25, 100, 65];

    let result = largest(&number_list);
    println!("The largest number is {result}");
    assert_eq!(*result, 100);

    let number_list = vec![102, 34, 6000, 89, 54, 2, 43, 8];

    let result = largest(&number_list);
    println!("The largest number is {result}");
    assert_eq!(*result, 6000);
}
```

- `largest` 함수는 `list`라는 매개변수를 가짐 → 함수에 전달할 수 있는 모든 구체적인 `i32` 값의 슬라이스를 나타냄
- 함수 호출 시 코드는 전달한 특정 값에 대해 실행됨

#### 리팩토링 단계

Listing 10-2에서 Listing 10-3으로 코드를 변경하기 위해 취한 단계:

1. **중복 코드 식별** - 반복되는 로직 인식
2. **중복 코드를 함수 본문으로 추출** - 함수 시그니처에서 해당 코드의 입력과 반환 값 지정
3. **중복된 코드 인스턴스 업데이트** - 코드를 반복하는 대신 함수 호출

#### 제네릭으로의 다음 단계

- 동일한 단계를 제네릭과 함께 사용하면 코드 중복을 더욱 줄일 수 있음
- 함수 본문이 특정 값 대신 추상적인 `list`에서 작동할 수 있는 것처럼, 제네릭은 코드가 추상적인 타입에서 작동할 수 있게 함
- 예: `i32` 슬라이스에서 가장 큰 항목을 찾는 함수와 `char` 슬라이스에서 가장 큰 항목을 찾는 함수가 있다면 제네릭으로 중복 제거 가능

### 핵심 포인트

**제네릭의 장점:**
- 코드 중복 제거
- 다양한 타입에 대해 동일한 로직 적용
- 타입 안전성 유지하면서 유연성 제공

**함수 추출 과정:**
- 중복 코드 식별
- 공통 로직을 함수로 분리
- 매개변수와 반환 타입 정의
- 원래 코드를 함수 호출로 대체

**이 장에서 배울 내용:**
- 제네릭 함수 정의 방법
- 구조체와 열거형에서 제네릭 사용
- 트레이트로 동작 정의
- 라이프타임으로 참조 관계 표현

---

## 제네릭 데이터 타입

> **원문:** https://doc.rust-lang.org/book/ch10-01-syntax.html

### 개요

- 제네릭(Generic)을 사용하면 함수 시그니처나 구조체 같은 항목에 대해 다양한 구체적인 데이터 타입에서 작동할 수 있는 정의를 만들 수 있음
- 타입 안전성을 유지하면서 코드 중복 감소 가능

**핵심 포인트:**
- 제네릭은 코드 중복을 줄이고 재사용성을 높임
- 다양한 타입에 대해 동일한 로직을 적용 가능
- 컴파일 타임에 타입 검사가 이루어져 타입 안전성 보장

### 함수 정의에서의 제네릭

#### 기본 개념

- 함수 시그니처에서 매개변수 목록 앞에 꺾쇠 괄호 `<>`를 사용해 제네릭을 배치:

```rust
fn largest<T>(list: &[T]) -> &T {
    let mut largest = &list[0];

    for item in list {
        if item > largest {
            largest = item;
        }
    }

    largest
}
```

#### 명명 규칙

**핵심 포인트:**
- 타입 매개변수는 **UpperCamelCase**를 사용
- `T`는 "type"의 줄임말로 관례적으로 사용됨
- 여러 제네릭 타입이 필요하면 `T`, `U`, `V` 등을 사용

#### 트레이트 바운드 (Trait Bounds)

- 제네릭 함수는 제약 조건이 필요할 수 있음 → 비교 연산에는 `PartialOrd` 트레이트 필요:

```rust
fn largest<T: std::cmp::PartialOrd>(list: &[T]) -> &T {
    // 구현
}
```

### 구조체 정의에서의 제네릭

#### 단일 제네릭 타입

```rust
struct Point<T> {
    x: T,
    y: T,
}

fn main() {
    let integer = Point { x: 5, y: 10 };
    let float = Point { x: 1.0, y: 4.0 };
}
```

**핵심 포인트:**
- 단일 제네릭 매개변수를 사용할 때 두 필드는 같은 타입이어야 함
- `Point { x: 5, y: 4.0 }`처럼 다른 타입을 사용하면 컴파일 오류 발생

#### 다중 제네릭 타입

```rust
struct Point<T, U> {
    x: T,
    y: U,
}

fn main() {
    let both_integer = Point { x: 5, y: 10 };
    let both_float = Point { x: 1.0, y: 4.0 };
    let integer_and_float = Point { x: 5, y: 4.0 };
}
```

**핵심 포인트:**
- 여러 제네릭 타입 매개변수를 사용하면 필드마다 다른 타입 가능
- 너무 많은 제네릭 매개변수는 코드를 읽기 어렵게 만들 수 있음

### 열거형 정의에서의 제네릭

#### Option<T>

```rust
enum Option<T> {
    Some(T),
    None,
}
```

**핵심 포인트:**
- `Option<T>`는 값이 있거나 없을 수 있는 상황을 표현
- `Some(T)`는 타입 `T`의 값을 포함
- `None`은 값이 없음을 나타냄

#### Result<T, E>

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

**핵심 포인트:**
- `Result<T, E>`는 연산이 성공하거나 실패할 수 있는 상황을 표현
- `Ok(T)`는 성공 시 타입 `T`의 값을 반환
- `Err(E)`는 실패 시 타입 `E`의 에러를 반환
- 파일 열기 등 실패 가능성이 있는 연산에 자주 사용됨

### 메서드 정의에서의 제네릭

#### 제네릭 타입에 대한 제네릭 메서드

```rust
struct Point<T> {
    x: T,
    y: T,
}

impl<T> Point<T> {
    fn x(&self) -> &T {
        &self.x
    }
}
```

**핵심 포인트:**
- `impl` 뒤에 `T`를 선언하여 `Point<T>`에 메서드를 구현함을 명시
- `impl<T>`는 "모든 타입 T에 대해"라는 의미

#### 특정 타입에 대한 메서드

- 특정 타입에 대해서만 메서드를 구현할 수도 있음:

```rust
impl Point<f32> {
    fn distance_from_origin(&self) -> f32 {
        (self.x.powi(2) + self.y.powi(2)).sqrt()
    }
}
```

**핵심 포인트:**
- `Point<f32>`에만 `distance_from_origin` 메서드가 존재
- 다른 타입의 `Point<T>` 인스턴스에서는 이 메서드 사용 불가
- 타입별로 특화된 기능을 제공할 때 유용

#### 다른 제네릭 매개변수를 가진 메서드

```rust
struct Point<X1, Y1> {
    x: X1,
    y: Y1,
}

impl<X1, Y1> Point<X1, Y1> {
    fn mixup<X2, Y2>(self, other: Point<X2, Y2>) -> Point<X1, Y2> {
        Point {
            x: self.x,
            y: other.y,
        }
    }
}
```

**핵심 포인트:**
- `impl`에 선언된 제네릭 매개변수(`X1`, `Y1`)와 메서드 시그니처에 선언된 제네릭 매개변수(`X2`, `Y2`)는 다름
- 이 예제에서 `mixup`은 두 `Point`를 혼합하여 새로운 `Point`를 생성
- `self.x`(타입 `X1`)와 `other.y`(타입 `Y2`)를 조합

### 제네릭 코드의 성능

#### 런타임 비용 없음

- 제네릭 타입 사용은 구체적인 타입 사용에 비해 런타임 성능 패널티 없음

#### 단형화 (Monomorphization)

- Rust는 컴파일 타임에 단형화(monomorphization) 수행: 제네릭 코드를 구체적인 타입으로 채워 특정 코드로 변환하는 과정

**제네릭 코드:**

```rust
let integer = Some(5);
let float = Some(5.0);
```

**단형화된 결과 (컴파일러가 생성):**

```rust
enum Option_i32 {
    Some(i32),
    None,
}

enum Option_f64 {
    Some(f64),
    None,
}

fn main() {
    let integer = Option_i32::Some(5);
    let float = Option_f64::Some(5.0);
}
```

**핵심 포인트:**
- 컴파일러가 사용된 각 구체적인 타입에 대해 특화된 버전을 생성
- 런타임 성능이 수동으로 중복 작성한 코드와 동일
- 제네릭의 유연성과 구체 코드의 성능을 모두 얻을 수 있음
- 컴파일 시간이 약간 증가할 수 있으나 런타임에는 영향 없음

---

## 라이프타임으로 참조 유효성 검증하기

> **원문:** https://doc.rust-lang.org/book/ch10-03-lifetime-syntax.html

### 개요

- 라이프타임(Lifetime): 참조가 필요한 만큼 오래 유효함을 보장하는 일종의 제네릭
- 대부분의 언어와 달리 Rust는 특정 상황에서 명시적인 라이프타임 어노테이션 요구
- 대부분의 경우 타입처럼 라이프타임도 암묵적으로 추론됨

**핵심 포인트:**
- 라이프타임의 주요 목적은 댕글링 참조(dangling reference) 방지
- 대부분의 경우 라이프타임은 자동으로 추론됨
- 컴파일러가 추론할 수 없는 경우에만 명시적 어노테이션 필요
- 모든 분석은 컴파일 타임에 수행되어 런타임 성능에 영향 없음

### 댕글링 참조 방지

- 라이프타임의 주요 목적: 댕글링 참조 방지 (댕글링 참조 = 이미 해제된 데이터를 가리키는 참조)

#### 잘못된 참조 예제 (Listing 10-16)

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

- 이 코드는 컴파일되지 않음:

```
error[E0597]: `x` does not live long enough
```

- 문제점: 변수 `x`가 `r`이 사용되기 전에 스코프를 벗어나 댕글링 참조 발생

### 빌림 검사기 (Borrow Checker)

- Rust 컴파일러의 빌림 검사기는 스코프를 비교해 모든 빌림이 유효한지 확인

#### 라이프타임 시각화 (Listing 10-17)

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

#### 수정된 코드 (Listing 10-18)

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

- 이제 `'a`가 `'b` 내에 포함되므로 코드가 컴파일됨

### 함수에서의 제네릭 라이프타임

#### 문제 상황 (Listing 10-20)

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

- 문제점: 컴파일러가 반환되는 참조가 `x`에서 오는지 `y`에서 오는지 결정 불가

### 라이프타임 어노테이션 문법

- 라이프타임 어노테이션은 참조가 실제로 얼마나 오래 사는지를 변경하지 않고, 여러 참조의 라이프타임 간 관계를 설명

#### 기본 문법

라이프타임 매개변수 이름:
- 아포스트로피(`'`)로 시작
- 보통 소문자이고 짧음 (예: `'a`)
- `&` 뒤에 공백과 함께 배치

```rust
&i32        // 참조
&'a i32     // 명시적 라이프타임을 가진 참조
&'a mut i32 // 명시적 라이프타임을 가진 가변 참조
```

### 함수 시그니처에서의 라이프타임

- 라이프타임 어노테이션을 사용하려면 함수 이름과 매개변수 목록 사이의 꺾쇠 괄호 안에 제네릭 라이프타임 매개변수를 선언

#### 해결책 (Listing 10-21)

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

#### 유효한 예제 (Listing 10-22)

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

- `result`가 `string2`가 유효한 동안 사용되므로 컴파일됨

#### 유효하지 않은 예제 (Listing 10-23)

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

- 문제점: 반환된 참조가 `println!`까지 유효하려면 `string2`가 유효해야 하지만, 그 전에 드롭됨

### 라이프타임 관계

- 라이프타임 매개변수는 함수가 수행하는 작업에 따라 달라짐

#### 하나의 매개변수만 관련되는 경우

```rust
fn longest<'a>(x: &'a str, y: &str) -> &'a str {
    x
}
```

- 반환 타입에 `x`의 라이프타임만 관련되므로 `y`에는 라이프타임 어노테이션 불필요

#### 댕글링 참조 예제

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

- 해결책: 함수 내에서 생성된 데이터에 대한 참조는 반환 불가 → 소유된 데이터를 대신 반환 필요

### 구조체 정의에서의 라이프타임

- 참조를 보유하는 구조체는 라이프타임 어노테이션 필요

#### 예제 (Listing 10-24)

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

### 라이프타임 생략 (Lifetime Elision)

- 라이프타임 생략 규칙: 컴파일러가 라이프타임을 추론할 수 있는 패턴

#### 역사적 배경

- 초기 Rust(1.0 이전)에서는 모든 참조에 명시적 라이프타임 어노테이션이 필요했음
- 프로그래머들이 동일한 패턴을 반복적으로 사용한다는 점을 발견 → 이 패턴을 컴파일러에 내장

#### 세 가지 생략 규칙

- 규칙 1 (입력 라이프타임): 각 참조 매개변수는 고유한 라이프타임 매개변수를 받음

```rust
fn foo<'a>(x: &'a i32)                           // 매개변수 하나
fn foo<'a, 'b>(x: &'a i32, y: &'b i32)          // 매개변수 둘
```

- 규칙 2 (출력 라이프타임): 입력 라이프타임이 정확히 하나이면, 모든 출력 라이프타임에 할당됨

```rust
fn foo<'a>(x: &'a i32) -> &'a i32
```

- 규칙 3 (메서드 라이프타임): 매개변수 중 하나가 `&self` 또는 `&mut self`이면, 그 라이프타임이 모든 출력 라이프타임에 할당됨

#### 예제: `first_word` (Listing 10-25)

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

- 명시적 어노테이션 없이 컴파일됨

#### 예제: `longest` (Listing 10-20에서)

```rust
fn longest(x: &str, y: &str) -> &str {
```

**규칙 적용:**
1. 규칙 1: `fn longest<'a, 'b>(x: &'a str, y: &'b str) -> &str`
2. 규칙 2 적용 불가 (여러 입력 라이프타임)
3. 규칙 3 적용 불가 (메서드가 아님)

**결과:** 컴파일 오류 - 라이프타임이 여전히 모호함

### 메서드 정의에서의 라이프타임

- 메서드 라이프타임은 제네릭 타입 매개변수와 동일한 문법 사용

#### 기본 예제

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

#### 여러 매개변수가 있는 메서드

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

- 규칙 3 적용: 반환 타입이 `&self`의 라이프타임을 받으므로 반환값에 명시적 어노테이션 불필요

### 정적 라이프타임 ('static)

- `'static` 라이프타임: 참조가 프로그램 전체 기간 동안 유효할 수 있음을 나타냄

#### 예제

```rust
let s: &'static str = "I have a static lifetime.";
```

- 모든 문자열 리터럴은 프로그램 바이너리에 저장되므로 `'static` 라이프타임을 가짐

#### 주의사항

- 경고: 신중한 고려 없이 `'static`을 지정하는 것은 금지
  - `'static`을 제안하는 오류 메시지는 보통 댕글링 참조나 라이프타임 불일치를 나타냄
  - `'static`으로 기본 설정하지 말고 해당 문제를 해결 필요

### 제네릭 타입 매개변수, 트레이트 바운드, 라이프타임 결합

- 세 가지 모두 하나의 함수에서 결합 가능:

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

### 요약

- 제네릭 타입 매개변수: 코드가 다양한 타입과 작동할 수 있게 함
- 트레이트와 트레이트 바운드: 제네릭 타입이 필요한 동작을 가지도록 보장
- 라이프타임 어노테이션: 유연한 코드가 댕글링 참조를 갖지 않도록 보장

- 모든 분석은 컴파일 타임에 수행되어 런타임 성능에 영향 없음
