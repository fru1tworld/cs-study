# 커맨드 라인 인수 받기

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
