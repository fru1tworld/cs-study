# 환경 변수로 작업하기

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
4. **유니코드 처리**: `to_lowercase()`는 기본적인 유니코드를 처리하지만 모든 언어에 대해 100% 정확하지 않을 수 있음

`std::env` 모듈은 환경 변수 관리를 위한 추가 유틸리티를 제공합니다. 더 많은 옵션은 해당 문서를 참조하세요.
