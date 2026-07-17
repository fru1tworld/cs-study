# I/O 프로젝트: 커맨드 라인 프로그램 만들기

# I/O 프로젝트: 커맨드 라인 프로그램 만들기

> **원문:** https://doc.rust-lang.org/book/ch12-00-an-io-project.html

## 개요

이 장은 지금까지 배운 많은 기술을 복습하고 몇 가지 추가적인 표준 라이브러리 기능을 탐구하는 프로젝트입니다. 지금까지 익힌 Rust 개념들을 실습하기 위해 파일 및 커맨드 라인 입출력(I/O)과 상호작용하는 커맨드 라인 도구를 만들어 봅니다.

**핵심 포인트:**
- Rust의 속도, 안전성, 단일 바이너리 출력, 크로스 플랫폼 지원을 활용한 CLI 도구 제작
- 클래식 커맨드 라인 검색 도구 `grep`의 자체 버전 구현
- 여러 장에서 배운 개념들의 종합적 활용

---

## 프로젝트 소개

Rust의 속도, 안전성, 단일 바이너리 출력, 그리고 크로스 플랫폼 지원은 커맨드 라인 도구를 만들기에 이상적인 언어로 만들어 줍니다. 이 프로젝트에서는 클래식 커맨드 라인 검색 도구인 `grep`(**g**lobally search a **r**egular **e**xpression and **p**rint)의 자체 버전을 만들어 보겠습니다.

---

## `grep`의 작동 방식

가장 단순한 사용 사례에서, `grep`은 지정된 파일에서 지정된 문자열을 검색합니다. 이를 위해 `grep`은 다음과 같은 인수를 받습니다:

- 파일 경로(file path)
- 검색할 문자열(string)

그런 다음, 파일을 읽고, 해당 파일에서 문자열 인수를 포함하는 줄을 찾아 해당 줄들을 출력합니다.

---

## 구현할 주요 기능

이 과정에서 많은 다른 커맨드 라인 도구들이 사용하는 터미널 기능을 우리의 커맨드 라인 도구에 적용하는 방법을 보여줄 것입니다:

- **환경 변수(environment variables)**: 환경 변수의 값을 읽어 사용자가 도구의 동작을 설정할 수 있도록 함
- **표준 오류 출력(stderr)**: 오류 메시지를 표준 출력(`stdout`) 대신 표준 오류 콘솔 스트림(`stderr`)에 출력하여, 예를 들어 사용자가 성공적인 출력을 파일로 리다이렉트하면서도 오류 메시지는 화면에서 계속 볼 수 있도록 함

---

## 다루는 개념

이 `grep` 프로젝트는 지금까지 배운 여러 개념들을 결합합니다:

- 코드 구성하기 (7장)
- 벡터(vector)와 문자열(string) 사용하기 (8장)
- 오류 처리하기 (9장)
- 적절한 곳에서 트레이트(trait)와 라이프타임(lifetime) 사용하기 (10장)
- 테스트 작성하기 (11장)

또한 클로저(closure), 반복자(iterator), 트레이트 객체(trait object)도 간략히 소개할 예정이며, 이는 13장과 18장에서 자세히 다룹니다.

---

## ripgrep에 대한 참고

Rust 커뮤니티 멤버인 Andrew Gallant는 이미 `grep`의 완전한 기능을 갖춘 매우 빠른 버전인 `ripgrep`을 만들었습니다. 이에 비해 우리의 버전은 상당히 단순하지만, 이 장에서는 `ripgrep`과 같은 실제 프로젝트를 이해하는 데 필요한 배경 지식을 제공할 것입니다.

---

# 커맨드 라인 인수 받기

> **원문:** https://doc.rust-lang.org/book/ch12-01-accepting-command-line-arguments.html

## 개요

이 섹션에서는 커맨드 라인 인수를 받아들이는 `minigrep` 프로젝트를 만드는 방법을 배웁니다. 목표는 다음과 같이 실행할 수 있는 프로그램을 만드는 것입니다:

```
$ cargo run -- searchstring example-filename.txt
```

## 인수 값 읽기

Rust에서 커맨드 라인 인수를 읽으려면 표준 라이브러리의 `std::env::args` 함수를 사용합니다. 이 함수는 커맨드 라인 인수의 반복자(iterator)를 반환합니다.

### 코드 예제 (Listing 12-1)

```rust
use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();
    dbg!(args);
}
```

**핵심 포인트:**
- `env::args()`는 커맨드 라인 인수의 반복자를 반환
- `.collect()`는 반복자를 `Vec<String>`으로 변환
- Rust는 컬렉션 타입을 추론할 수 없으므로 타입 명시가 필요

### 프로그램 실행

**인수 없이 실행:**
```
$ cargo run
[src/main.rs:5:5] args = [
    "target/debug/minigrep",
]
```

**인수와 함께 실행:**
```
$ cargo run -- needle haystack
[src/main.rs:5:5] args = [
    "target/debug/minigrep",
    "needle",
    "haystack",
]
```

참고: 첫 번째 값은 항상 바이너리 이름(`target/debug/minigrep`)입니다.

## `args` 함수와 유효하지 않은 유니코드

- `std::env::args`는 인수에 유효하지 않은 유니코드가 포함되면 **패닉**(panic)을 발생시킴
- 유효하지 않은 유니코드를 지원하려면 `std::env::args_os`를 대신 사용 (`OsString` 값을 반환)

## 인수 값을 변수에 저장하기

### 코드 예제 (Listing 12-2)

```rust
use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();

    let query = &args[1];
    let file_path = &args[2];

    println!("Searching for {query}");
    println!("In file {file_path}");
}
```

**설명:**
- `args[0]` = 프로그램 이름 (건너뜀)
- `args[1]` = 검색 문자열 (`query`)
- `args[2]` = 파일 경로 (`file_path`)

### 출력 예제

```
$ cargo run -- test sample.txt
Searching for test
In file sample.txt
```

## 요약

- `std::env::args`를 사용하여 커맨드 라인 인수에 접근
- `.collect()`를 사용하여 반복자를 벡터로 변환
- 첫 번째 인수(`args[0]`)는 항상 프로그램 이름
- 유효하지 않은 유니코드를 처리해야 할 경우 `args_os` 사용

---

# 파일 읽기

> **원문:** https://doc.rust-lang.org/book/ch12-02-reading-a-file.html

## 개요

이 장에서는 커맨드라인 인수로 지정된 파일을 읽는 기능을 구현합니다. `minigrep` 검색 유틸리티 구축의 일부로, 파일 내용을 읽어 처리하는 방법을 배웁니다.

## 테스트 파일 설정

프로젝트 루트 디렉토리에 `poem.txt` 파일을 생성합니다:

```
I'm nobody! Who are you?
Are you nobody, too?
Then there's a pair of us - don't tell!
They'd banish us, you know.

How dreary to be somebody!
How public, like a frog
To tell your name the livelong day
To an admiring bog!
```

Emily Dickinson의 시로, 여러 줄과 반복되는 단어가 있어 테스트하기에 적합합니다.

## 코드 구현

`src/main.rs`를 업데이트하여 파일을 읽습니다:

```rust
use std::env;
use std::fs;

fn main() {
    // --snip--
    let args: Vec<String> = env::args().collect();

    let query = &args[1];
    let file_path = &args[2];

    println!("Searching for {query}");
    println!("In file {file_path}");

    let contents = fs::read_to_string(file_path)
        .expect("Should have been able to read the file");

    println!("With text:\n{contents}");
}
```

**핵심 포인트:**
- `std::fs` 모듈을 사용하여 파일 작업을 처리
- `fs::read_to_string()` 함수는 파일 경로를 받아 파일을 열고 `std::io::Result<String>`을 반환
- `.expect()`를 사용하여 파일을 읽을 수 없는 경우의 오류를 처리

## 프로그램 실행

다음 명령어로 실행합니다:

```bash
$ cargo run -- the poem.txt
```

출력:
```
Searching for the
In file poem.txt
With text:
I'm nobody! Who are you?
Are you nobody, too?
Then there's a pair of us - don't tell!
They'd banish us, you know.

How dreary to be somebody!
How public, like a frog
To tell your name the livelong day
To an admiring bog!
```

## 코드 품질에 대한 참고 사항

현재 구현에는 몇 가지 문제점이 있습니다:
- `main` 함수가 여러 책임이 있음
- 오류 처리를 개선할 수 있음

이러한 문제점들은 개발 초기에 리팩토링하는 것이 좋으며, 다음 섹션에서 오류 처리와 모듈성 개선을 다룹니다.

## 요약

- **표준 라이브러리 임포트**: 파일 작업을 위해 `std::fs` 모듈 사용
- **`fs::read_to_string()` 함수**: 파일 경로를 받아 파일 내용을 `String`으로 반환
- **오류 처리**: `expect()`를 사용하여 파일 읽기 실패 시 패닉 발생
- **다음 단계**: 코드 리팩토링을 통해 오류 처리와 모듈성 개선 필요

---

# 환경 변수로 작업하기

> **원문:** https://doc.rust-lang.org/book/ch12-05-working-with-environment-variables.html

## 개요

이 섹션에서는 환경 변수를 사용하여 `minigrep` 바이너리에 대소문자 구분 없는 검색 기능을 추가하는 방법을 설명합니다. 테스트 주도 개발(TDD) 원칙을 따라 진행합니다.

**핵심 포인트:**
- 환경 변수를 사용하여 프로그램 동작을 설정
- `std::env::var()` 함수로 환경 변수 읽기
- TDD 방식으로 새 기능 구현

## 대소문자 구분 없는 검색을 위한 실패하는 테스트 작성

먼저, 새로운 `search_case_insensitive` 함수를 추가하고 대소문자 구분 검색과 대소문자 구분 없는 검색을 검증하는 테스트를 만듭니다.

**파일명: src/lib.rs**

```rust
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    let mut results = Vec::new();

    for line in contents.lines() {
        if line.contains(query) {
            results.push(line);
        }
    }

    results
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn case_sensitive() {
        let query = "duct";
        let contents = "\
Rust:
safe, fast, productive.
Pick three.
Duct tape.";

        assert_eq!(vec!["safe, fast, productive."], search(query, contents));
    }

    #[test]
    fn case_insensitive() {
        let query = "rUsT";
        let contents = "\
Rust:
safe, fast, productive.
Pick three.
Trust me.";

        assert_eq!(
            vec!["Rust:", "Trust me."],
            search_case_insensitive(query, contents)
        );
    }
}
```

## `search_case_insensitive` 함수 구현

구현에서는 비교 전에 쿼리와 각 줄을 모두 소문자로 변환합니다.

**파일명: src/lib.rs**

```rust
pub fn search_case_insensitive<'a>(
    query: &str,
    contents: &'a str,
) -> Vec<&'a str> {
    let query = query.to_lowercase();
    let mut results = Vec::new();

    for line in contents.lines() {
        if line.to_lowercase().contains(&query) {
            results.push(line);
        }
    }

    results
}
```

**핵심 포인트:**
- `query.to_lowercase()`는 새로운 `String`을 생성 (문자열 슬라이스가 아님)
- `contains()`는 문자열 슬라이스를 기대하므로 `&query`를 전달
- 각 `line`은 비교 시 소문자로 변환됨
- 결과에는 원래 대소문자가 유지된 원본 줄이 반환됨

## Config 구조체와 run 함수 업데이트

**파일명: src/main.rs**

```rust
use std::env;
use std::error::Error;
use std::fs;
use std::process;

use minigrep::{search, search_case_insensitive};

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

pub struct Config {
    pub query: String,
    pub file_path: String,
    pub ignore_case: bool,
}

impl Config {
    fn build(args: &[String]) -> Result<Config, &'static str> {
        if args.len() < 3 {
            return Err("not enough arguments");
        }

        let query = args[1].clone();
        let file_path = args[2].clone();

        let ignore_case = env::var("IGNORE_CASE").is_ok();

        Ok(Config {
            query,
            file_path,
            ignore_case,
        })
    }
}

fn run(config: Config) -> Result<(), Box<dyn Error>> {
    let contents = fs::read_to_string(config.file_path)?;

    let results = if config.ignore_case {
        search_case_insensitive(&config.query, &contents)
    } else {
        search(&config.query, &contents)
    };

    for line in results {
        println!("{line}");
    }

    Ok(())
}
```

## 환경 변수 사용하기

### IGNORE_CASE 변수 확인

```rust
let ignore_case = env::var("IGNORE_CASE").is_ok();
```

`env::var()` 함수는 `Result`를 반환합니다:
- **Ok 변형**: 환경 변수가 설정됨 (어떤 값이든 대소문자 구분 없는 검색을 트리거)
- **Err 변형**: 환경 변수가 설정되지 않음
- `is_ok()`는 변수가 설정된 경우에만 `true`를 반환 (값에 관계없이)

### 프로그램 실행

**대소문자 구분 검색 (기본값):**
```bash
$ cargo run -- to poem.txt
Are you nobody, too?
How dreary to be somebody!
```

**대소문자 구분 없는 검색 (Linux/Mac):**
```bash
$ IGNORE_CASE=1 cargo run -- to poem.txt
Are you nobody, too?
How dreary to be somebody!
To tell your name the livelong day
To an admiring bog!
```

**대소문자 구분 없는 검색 (PowerShell):**
```powershell
PS> $Env:IGNORE_CASE=1; cargo run -- to poem.txt
```

**변수 해제 (PowerShell):**
```powershell
PS> Remove-Item Env:IGNORE_CASE
```

## 핵심 요약

1. **`std::env::var()`를 통한 환경 변수**: 특정 값에 관계없이 환경 변수가 설정되었는지 확인
2. **우선순위 결정**: 프로그램은 명령줄 인수를 환경 변수보다 우선시하거나 (또는 그 반대로) 설정 가능
3. **TDD 접근 방식**: 먼저 실패하는 테스트를 작성한 후 기능 구현
4. **유니코드 처리**: `to_lowercase()`는 기본적인 유니코드를 처리하지만 모든 언어에서 100% 정확하지 않을 수 있음

`std::env` 모듈은 환경 변수 관리를 위한 추가 유틸리티를 제공합니다. 더 많은 옵션은 해당 문서를 참조하세요.
