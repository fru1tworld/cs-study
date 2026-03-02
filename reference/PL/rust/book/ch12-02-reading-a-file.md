# 파일 읽기

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
- `.expect()`를 사용하여 파일을 읽을 수 없는 경우의 에러를 처리

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
- `main` 함수가 여러 책임을 가지고 있음
- 에러 처리를 개선할 수 있음

이러한 문제점들은 개발 초기에 리팩토링하는 것이 좋으며, 다음 섹션에서 에러 처리와 모듈성 개선에 대해 다룹니다.

## 요약

- **표준 라이브러리 임포트**: 파일 작업을 위해 `std::fs` 모듈 사용
- **`fs::read_to_string()` 함수**: 파일 경로를 받아 파일 내용을 `String`으로 반환
- **에러 처리**: `expect()`를 사용하여 파일 읽기 실패 시 패닉 발생
- **다음 단계**: 코드 리팩토링을 통해 에러 처리와 모듈성 개선 필요
