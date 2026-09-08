# 모듈성과 에러 처리 개선을 위한 리팩토링

## 개요

- 이 장: 커맨드라인 프로그램의 네 가지 구조적 문제를 다룸

- 1\. `main` 함수가 여러 작업을 수행함 (인수 파싱과 파일 읽기)
- 2\. 설정 변수들이 그룹화되어 있지 않음
- 3\. `expect`의 에러 메시지가 불친절하고 일반적임
- 4\. 에러 처리 코드가 분산됨 → 유지보수 어려움

## 네 가지 문제와 해결책

### 문제 1 & 2: 관심사 분리와 설정 그룹화

현재 `main`은 인수 파싱과 파일 로직을 모두 처리한다. 두 역할을 분리하기 위해 인수 파싱을 별도 함수로 옮기고, 파싱한 설정을 구조체로 묶는다.

#### 1단계: 인수 파서 추출

```rust
fn main() {
    let args: Vec<String> = env::args().collect();
    let (query, file_path) = parse_config(&args);

    println!("Searching for {query}");
    println!("In file {file_path}");

    let contents = fs::read_to_string(file_path)
        .expect("Should have been able to read the file");

    println!("With text:\n{contents}");
}

fn parse_config(args: &[String]) -> (&str, &str) {
    let query = &args[1];
    let file_path = &args[2];
    (query, file_path)
}
```

#### 2단계: 설정을 구조체로 그룹화

```rust
struct Config {
    query: String,
    file_path: String,
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let config = parse_config(&args);

    println!("Searching for {}", config.query);
    println!("In file {}", config.file_path);

    let contents = fs::read_to_string(config.file_path)
        .expect("Should have been able to read the file");

    println!("With text:\n{contents}");
}

fn parse_config(args: &[String]) -> Config {
    let query = args[1].clone();
    let file_path = args[2].clone();
    Config { query, file_path }
}
```

`clone()`은 데이터를 복사하므로 약간의 비용이 든다. 다만 여기서는 작은 문자열을 한 번 복사하므로 이 비용을 허용하고, 우선 작동하는 코드를 만드는 데 집중한다. 더 효율적인 대안은 13장에서 다룬다.

#### 3단계: `Config`에 생성자 만들기

- `parse_config`를 `Config::new` 연관 함수로 변환:

```rust
struct Config {
    query: String,
    file_path: String,
}

impl Config {
    fn new(args: &[String]) -> Config {
        let query = args[1].clone();
        let file_path = args[2].clone();
        Config { query, file_path }
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();
    let config = Config::new(&args);

    println!("Searching for {}", config.query);
    println!("In file {}", config.file_path);

    let contents = fs::read_to_string(config.file_path)
        .expect("Should have been able to read the file");

    println!("With text:\n{contents}");
}
```

### 문제 3 & 4: 에러 처리 개선

#### 개선 전 에러 메시지

```
$ cargo run
thread 'main' panicked at src/main.rs:27:21:
index out of bounds: the len is 1 but the index is 1
```

- 이 에러 메시지 → 최종 사용자에게 도움 안 됨

#### 해결책 1: 더 나은 패닉 메시지

```rust
impl Config {
    fn new(args: &[String]) -> Config {
        if args.len() < 3 {
            panic!("not enough arguments");
        }

        let query = args[1].clone();
        let file_path = args[2].clone();
        Config { query, file_path }
    }
}
```

- 출력:

```
thread 'main' panicked at src/main.rs:26:13:
not enough arguments
```

인수가 부족하다는 원인은 이제 알 수 있지만, 사용자의 입력 오류에도 여전히 `panic!`으로 종료한다. 호출자가 오류를 처리할 수 있도록 반환 타입을 바꿔 보자.

#### 해결책 2: 패닉 대신 `Result` 반환

- `new`를 `build`로 이름 변경하고 `Result` 반환:

```rust
impl Config {
    fn build(args: &[String]) -> Result<Config, &'static str> {
        if args.len() < 3 {
            return Err("not enough arguments");
        }

        let query = args[1].clone();
        let file_path = args[2].clone();

        Ok(Config { query, file_path })
    }
}
```

#### 해결책 3: `main`에서 에러 처리

```rust
use std::env;
use std::fs;
use std::process;

fn main() {
    let args: Vec<String> = env::args().collect();

    let config = Config::build(&args).unwrap_or_else(|err| {
        println!("Problem parsing arguments: {err}");
        process::exit(1);
    });

    println!("Searching for {}", config.query);
    println!("In file {}", config.file_path);

    let contents = fs::read_to_string(config.file_path)
        .expect("Should have been able to read the file");

    println!("With text:\n{contents}");
}
```

`unwrap_or_else()`는 `Result`가 `Ok`이면 내부 값을 반환하고, `Err`이면 에러 값을 전달해 클로저를 호출한다. 위 클로저는 사용자가 이해하기 쉬운 메시지를 출력한 뒤 상태 코드 1로 프로그램을 종료한다.

- 사용자 친화적인 출력:

```
$ cargo run
Problem parsing arguments: not enough arguments
```

## `main`에서 로직 추출

### 1단계: `run` 함수 생성

- 핵심 로직을 별도 함수로 추출:

```rust
fn run(config: Config) {
    let contents = fs::read_to_string(config.file_path)
        .expect("Should have been able to read the file");

    println!("With text:\n{contents}");
}

fn main() {
    let args: Vec<String> = env::args().collect();

    let config = Config::build(&args).unwrap_or_else(|err| {
        println!("Problem parsing arguments: {err}");
        process::exit(1);
    });

    println!("Searching for {}", config.query);
    println!("In file {}", config.file_path);

    run(config);
}
```

### 2단계: `run`에서 `Result` 반환

- 패닉 대신 에러를 반환하도록 `run` 수정:

```rust
use std::error::Error;

fn run(config: Config) -> Result<(), Box<dyn Error>> {
    let contents = fs::read_to_string(config.file_path)?;

    println!("With text:\n{contents}");

    Ok(())
}
```

`run`은 성공했을 때 돌려줄 값이 없으므로 `Ok(())`를 반환한다. 파일 읽기에 실패하면 `?`가 에러를 호출자에게 전달한다. 반환 타입의 `Box<dyn Error>`는 `Error` 트레이트를 구현하는 여러 에러 타입을 담을 수 있어, 이후 다른 종류의 실패가 추가되어도 같은 반환 타입을 사용할 수 있다.

### 3단계: `main`에서 `run`의 에러 처리

```rust
fn main() {
    let args: Vec<String> = env::args().collect();

    let config = Config::build(&args).unwrap_or_else(|err| {
        println!("Problem parsing arguments: {err}");
        process::exit(1);
    });

    println!("Searching for {}", config.query);
    println!("In file {}", config.file_path);

    if let Err(e) = run(config) {
        println!("Application error: {e}");
        process::exit(1);
    }
}
```

- 에러 감지만 필요, 값 언래핑 불필요 → `unwrap_or_else` 대신 `if let` 사용

## 코드를 라이브러리 크레이트로 분리

### `src/lib.rs` 생성

- 라이브러리에 검색 가능한 함수 정의:

```rust
// src/lib.rs
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    unimplemented!();
}
```

- 시그니처 참고:
- `pub` 키워드는 공개 API의 일부로 만듦
- 라이프타임 `'a`는 반환된 참조가 `contents` 매개변수만큼 유효함을 나타냄
- `Vec<&'a str>` 반환: `contents`와 같은 라이프타임을 가진 문자열 슬라이스 벡터

### `src/main.rs` 업데이트

```rust
use std::env;
use std::error::Error;
use std::fs;
use std::process;

use minigrep::search;

struct Config {
    query: String,
    file_path: String,
}

impl Config {
    fn build(args: &[String]) -> Result<Config, &'static str> {
        if args.len() < 3 {
            return Err("not enough arguments");
        }

        let query = args[1].clone();
        let file_path = args[2].clone();

        Ok(Config { query, file_path })
    }
}

fn run(config: Config) -> Result<(), Box<dyn Error>> {
    let contents = fs::read_to_string(config.file_path)?;

    for line in search(&config.query, &contents) {
        println!("{line}");
    }

    Ok(())
}

fn main() {
    let args: Vec<String> = env::args().collect();

    let config = Config::build(&args).unwrap_or_else(|err| {
        println!("Problem parsing arguments: {err}");
        process::exit(1);
    });

    if let Err(e) = run(config) {
        println!("Application error: {e}");
        process::exit(1);
    }
}
```

`search`를 공개된 라이브러리 함수로 분리했으므로 다른 코드에서 재사용하거나 직접 테스트할 수 있다. 이제 이 함수에 검색 기능을 구현하면 된다.
