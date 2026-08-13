# 러스트 시작하기와 숫자 맞추기 게임

## 시작하기

## 시작하기

> 원문: https://doc.rust-lang.org/book/ch01-00-getting-started.html

이 챕터에서 다루는 내용:

- Linux·macOS·Windows에서 Rust 설치
- "Hello, world!" 프로그램 작성
- Rust의 패키지 관리자이자 빌드 시스템인 `cargo` 사용

Rust를 시작하는 첫 단계 → 설치 방법부터 알아보고 간단한 프로그램 작성.

---

## 설치

> 원문: https://doc.rust-lang.org/book/ch01-01-installation.html

첫 번째 단계는 Rust 설치. Rust 버전과 관련 도구를 관리하는 커맨드 라인 도구인 `rustup`을 통해 다운로드 → 인터넷 연결 필요.

참고: `rustup`을 사용하고 싶지 않다면 [다른 Rust 설치 방법 페이지](https://forge.rust-lang.org/infra/other-installation-methods.html)에서 다른 옵션 확인.

다음 단계는 Rust 컴파일러의 최신 안정 버전을 설치.
- Rust의 안정성 보장 → 이 책의 모든 예제가 최신 Rust 버전에서도 계속 컴파일됨
- Rust는 오류 메시지와 경고를 자주 개선 → 출력이 버전마다 약간 다를 수 있음
- 즉, 이 단계로 설치한 최신 안정 버전 Rust는 이 책의 내용과 예상대로 작동해야 함

#### 커맨드 라인 표기법

- 터미널에 입력해야 하는 줄은 모두 `$`로 시작 → `$` 문자 자체는 입력하지 않음, 각 명령의 시작을 나타내는 프롬프트
- `$`로 시작하지 않는 줄 → 일반적으로 이전 명령의 출력
- PowerShell 전용 예제는 `$` 대신 `>` 사용

#### Linux 또는 macOS에서 `rustup` 설치하기

Linux 또는 macOS에서 터미널을 열고 다음 명령 입력:

```console
$ curl --proto '=https' --tlsv1.2 https://sh.rustup.rs -sSf | sh
```

- 이 명령은 스크립트를 다운로드하고 `rustup` 도구 설치를 시작 → Rust의 최신 안정 버전을 설치
- 비밀번호 입력 메시지가 나타날 수 있음
- 설치 성공 시 다음 줄 표시:

```text
Rust is installed now. Great!
```

또한 *링커*가 필요.
- 링커: Rust가 컴파일된 출력을 하나의 파일로 결합하는 데 사용하는 프로그램
- 이미 설치되어 있을 가능성이 높음
- 링커 오류 발생 시 → 일반적으로 링커를 포함하는 C 컴파일러 설치 필요
- 일부 일반적인 Rust 패키지가 C 코드에 의존 → C 컴파일러가 필요하므로 유용함

macOS에서는 다음을 실행하여 C 컴파일러 획득:

```console
$ xcode-select --install
```

Linux 사용자는 일반적으로 배포판 문서에 따라 GCC 또는 Clang 설치 필요. 예: Ubuntu 사용 시 `build-essential` 패키지 설치.

#### Windows에서 `rustup` 설치하기

- Windows에서는 [https://www.rust-lang.org/tools/install](https://www.rust-lang.org/tools/install)로 이동하여 Rust 설치 지침 따름
- 설치 과정 중 Visual Studio 설치 메시지 표시 → 프로그램 컴파일에 필요한 링커와 네이티브 라이브러리 제공
- 이 단계에서 더 많은 도움 필요 시 [https://rust-lang.github.io/rustup/installation/windows-msvc.html](https://rust-lang.github.io/rustup/installation/windows-msvc.html) 참조
- 이 책의 나머지 부분은 *cmd.exe*와 PowerShell 모두에서 작동하는 명령 사용 → 특정 차이점이 있으면 별도로 설명

#### 문제 해결

Rust가 올바르게 설치되었는지 확인 → 셸을 열고 다음 줄 입력:

```console
$ rustc --version
```

출시된 최신 안정 버전의 버전 번호·커밋 해시·커밋 날짜가 다음 형식으로 표시되어야 함:

```text
rustc x.y.z (abcabcabc yyyy-mm-dd)
```

- 이 정보가 표시되면 Rust 설치 성공
- 표시되지 않으면 → Rust가 `%PATH%` 시스템 변수에 있는지 확인

Windows CMD에서:

```console
> echo %PATH%
```

PowerShell에서:

```console
> echo $env:Path
```

Linux와 macOS에서:

```console
$ echo $PATH
```

모든 것이 올바른데도 Rust가 작동하지 않는 경우 → [커뮤니티 페이지](https://www.rust-lang.org/community)에서 다른 Rustacean(자칭 별명)과 연락하는 방법 확인.

#### 업데이트 및 제거

`rustup`을 통해 Rust가 설치된 경우 새 버전으로 업데이트 용이. 셸에서 다음 업데이트 스크립트 실행:

```console
$ rustup update
```

Rust와 `rustup`을 제거하려면 셸에서 다음 제거 스크립트 실행:

```console
$ rustup self uninstall
```

#### 로컬 문서 읽기

- Rust 설치에는 오프라인 읽기용 로컬 문서 사본 포함
- `rustup doc` 실행 → 브라우저에서 로컬 문서 열림
- 표준 라이브러리가 제공하는 타입·함수의 동작이나 사용법이 불확실할 때마다 API(Application Programming Interface) 문서로 확인

#### 텍스트 편집기와 IDE 사용하기

- 이 책은 Rust 코드 작성 도구를 특정하지 않음 → 거의 모든 텍스트 편집기로 작업 가능
- 많은 텍스트 편집기와 IDE에 Rust 내장 지원 존재
- Rust 웹사이트의 [도구 페이지](https://www.rust-lang.org/tools)에서 최신 편집기·IDE 목록 확인 가능

#### 이 책으로 오프라인 작업하기

- 여러 예제가 표준 라이브러리 외의 Rust 패키지를 사용 → 인터넷 연결 필요하거나 의존성을 미리 다운로드해야 함
- 의존성 미리 다운로드 명령(`cargo`와 각 명령의 의미는 이후 설명):

```console
$ cargo new get-dependencies
$ cd get-dependencies
$ cargo add rand@0.8.5 trpl@0.2.0
```

- 이렇게 하면 패키지 다운로드가 캐시됨 → 나중에 다시 다운로드할 필요 없음
- 명령 실행 후 `get-dependencies` 폴더를 유지할 필요 없음
- 이후 책 전체에서 모든 `cargo` 명령에 `--offline` 플래그 사용 → 네트워크 없이 캐시된 버전 사용 가능

---

## Hello, World!

> 원문: https://doc.rust-lang.org/book/ch01-02-hello-world.html

Rust를 설치했으니 첫 번째 Rust 프로그램 작성. 새로운 언어를 배울 때 `Hello, world!`를 화면에 출력하는 작은 프로그램을 작성하는 것이 전통 → 동일하게 진행.

참고: 이 책은 커맨드 라인에 대한 기본적인 친숙함을 가정.
- Rust는 편집·도구·코드 위치에 특정한 요구 없음 → 커맨드 라인 대신 IDE 사용 가능
- 많은 IDE가 어느 정도 Rust 지원 제공 → 자세한 내용은 IDE 문서 확인
- Rust 팀은 `rust-analyzer`를 통한 IDE 지원에 집중 → 자세한 내용은 부록 D 참조

### 프로젝트 디렉토리 설정

- Rust 코드를 저장할 디렉토리 생성부터 시작
- Rust는 코드 위치를 신경 쓰지 않음 → 이 책의 연습·프로젝트를 위해 홈 디렉토리에 *projects* 디렉토리를 만들고 모든 프로젝트를 그 안에 보관하는 것을 권장

터미널을 열고 다음 명령으로 *projects* 디렉토리와 "Hello, world!" 프로젝트용 디렉토리 생성.

Linux, macOS, Windows PowerShell의 경우:

```console
$ mkdir ~/projects
$ cd ~/projects
$ mkdir hello_world
$ cd hello_world
```

Windows CMD의 경우:

```cmd
> mkdir "%USERPROFILE%\projects"
> cd /d "%USERPROFILE%\projects"
> mkdir hello_world
> cd hello_world
```

### Rust 프로그램 기초

- 새 소스 파일을 만들고 *main.rs*로 명명
- Rust 파일은 항상 *.rs* 확장자로 끝남
- 파일 이름에 둘 이상의 단어를 사용하는 경우 → 밑줄로 단어 구분하는 것이 규칙 (예: *helloworld.rs* 대신 *hello_world.rs*)

*main.rs* 파일을 열고 다음 코드 입력.

파일명: main.rs

```rust
fn main() {
    println!("Hello, world!");
}
```

Listing 1-1: `Hello, world!`를 출력하는 프로그램

파일을 저장하고 *~/projects/hello_world* 디렉토리의 터미널 창으로 돌아감.

Linux 또는 macOS에서:

```console
$ rustc main.rs
$ ./main
Hello, world!
```

Windows에서:

```cmd
> rustc main.rs
> .\main
Hello, world!
```

- 운영체제와 관계없이 `Hello, world!` 문자열이 터미널에 출력되어야 함
- 출력이 보이지 않으면 → 설치 섹션의 "문제 해결" 부분 참조
- `Hello, world!`가 출력됐다면 공식적으로 Rust 프로그램 작성 완료 → Rust 프로그래머가 된 것

### Rust 프로그램 해부하기

"Hello, world!" 프로그램의 첫 번째 조각:

```rust
fn main() {

}
```

- 이 줄들은 `main`이라는 함수를 정의
- `main` 함수는 특별함 → 모든 실행 가능한 Rust 프로그램에서 항상 가장 먼저 실행되는 코드
- 첫 번째 줄은 매개변수가 없고 아무것도 반환하지 않는 `main` 함수를 선언 → 매개변수가 있다면 괄호 `()` 안에 위치

- 함수 본문은 `{}`로 감쌈 → Rust는 모든 함수 본문 주위에 중괄호 필요
- 여는 중괄호를 함수 선언과 같은 줄에 배치하고 사이에 공백 하나를 두는 것이 좋은 스타일

참고: Rust 프로젝트 전체에서 표준 스타일을 유지하려면 `rustfmt`라는 자동 포맷터 도구로 코드를 포맷 가능(자세한 내용은 부록 D 참조). Rust 팀이 `rustc`와 마찬가지로 표준 Rust 배포판에 포함 → 이미 컴퓨터에 설치되어 있어야 함.

`main` 함수 본문의 코드:

```rust
    println!("Hello, world!");
```

이 줄이 프로그램의 모든 작업을 수행 → 텍스트를 화면에 출력. 주목할 세 가지 세부 사항:

- `println!`은 Rust 매크로 호출
  - 함수를 호출했다면 `!` 없이 `println`으로 입력
  - Rust 매크로: Rust 구문을 확장하기 위해 코드를 생성하는 코드 작성 방법, 20장에서 자세히 논의
  - 지금은 `!`를 사용하면 일반 함수 대신 매크로를 호출한다는 것, 매크로가 항상 함수와 같은 규칙을 따르지는 않는다는 것만 알면 됨
- `"Hello, world!"` 문자열
  - 이 문자열을 `println!`의 인수로 전달 → 문자열이 화면에 출력됨
- 줄 끝의 세미콜론(`;`)
  - 이 표현식이 끝났고 다음 표현식이 시작할 준비가 되었음을 나타냄
  - 대부분의 Rust 코드 줄은 세미콜론으로 끝남

### 컴파일과 실행

새로 만든 프로그램을 실행했으므로 프로세스의 각 단계 확인.

Rust 프로그램을 실행하기 전에 Rust 컴파일러로 컴파일 필요 → `rustc` 명령에 소스 파일 이름을 전달:

```console
$ rustc main.rs
```

- C나 C++ 배경이 있다면 `gcc`나 `clang`과 유사함을 알 수 있음
- 성공적으로 컴파일한 후 Rust는 바이너리 실행 파일을 출력

Linux, macOS, Windows PowerShell에서 셸에 `ls` 명령을 입력하면 실행 파일 확인 가능:

```console
$ ls
main  main.rs
```

- Linux와 macOS에서는 두 개의 파일이 보임
- Windows PowerShell에서도 CMD를 사용할 때와 같은 세 개의 파일이 보임

Windows CMD에서는 다음을 입력:

```cmd
> dir /B
main.exe
main.pdb
main.rs
```

- *.rs* 확장자를 가진 소스 코드 파일
- 실행 파일 (Windows에서는 *main.exe*, 다른 모든 플랫폼에서는 *main*)
- Windows에서는 디버깅 정보를 담은 *.pdb* 확장자 파일

*main* 또는 *main.exe* 파일을 다음과 같이 실행:

```console
$ ./main # Windows에서는 .\main
```

*main.rs*가 "Hello, world!" 프로그램이라면 이 줄은 터미널에 `Hello, world!`를 출력.

- Ruby, Python, JavaScript와 같은 동적 언어에 더 익숙하다면 프로그램을 별도의 단계로 컴파일하고 실행하는 것이 낯설 수 있음
- Rust는 *사전 컴파일(ahead-of-time compiled)* 언어
  - 프로그램을 컴파일하고 실행 파일을 다른 사람에게 줄 수 있음 → 그 사람은 Rust가 설치되어 있지 않아도 실행 가능
  - *.rb*, *.py*, *.js* 파일을 주면 → 그 사람은 각각 Ruby, Python, JavaScript 구현이 설치되어 있어야 함
  - 다만 그런 언어는 컴파일과 실행에 하나의 명령만 필요 → 모든 것은 언어 설계의 트레이드오프

단순한 프로그램에서는 `rustc`로 컴파일하는 것만으로 충분 → 프로젝트가 성장하면 모든 옵션을 관리하고 코드를 쉽게 공유하고 싶어짐. 다음으로 실제 Rust 프로그램 작성에 도움이 되는 Cargo 도구 소개.

---

## Hello, Cargo!

> 원문: https://doc.rust-lang.org/book/ch01-03-hello-cargo.html

- Cargo: Rust의 빌드 시스템이자 패키지 관리자
- 대부분의 Rustacean이 이 도구로 Rust 프로젝트 관리 → Cargo가 다음 작업을 처리하기 때문:
  - 코드 빌드
  - 코드가 의존하는 라이브러리 다운로드
  - 해당 라이브러리(*의존성*) 빌드

가장 단순한 Rust 프로그램은 의존성이 없음 → 더 복잡한 프로그램을 작성하면서 의존성을 추가하게 되는데, Cargo로 프로젝트를 시작하면 의존성 추가가 훨씬 쉬워짐.

공식 설치 프로그램을 사용했다면 Cargo는 Rust와 함께 설치됨. 설치 여부 확인:

```console
$ cargo --version
```

### Cargo로 프로젝트 생성하기

projects 디렉토리로 이동하고 다음 실행:

```console
$ cargo new hello_cargo
$ cd hello_cargo
```

다음을 포함하는 새 디렉토리 생성:

- `Cargo.toml` 파일
- `main.rs` 파일이 있는 `src` 디렉토리
- 새 Git 저장소와 `.gitignore` 파일 (이미 Git 저장소 안에 있지 않은 경우)

#### Cargo.toml 설정

텍스트 편집기에서 `Cargo.toml` 열기:

```toml
[package]
name = "hello_cargo"
version = "0.1.0"
edition = "2024"

[dependencies]
```

이 파일은 *TOML*(*Tom's Obvious, Minimal Language*) 형식:

- `[package]` - 패키지 설정을 위한 섹션 헤딩
- `name`, `version`, `edition` - 프로그램을 컴파일하는 데 필요한 설정 정보
- `[dependencies]` - 프로젝트의 의존성을 나열하는 섹션 (코드 패키지를 *크레이트*라고 함)

#### 생성된 소스 코드

`src/main.rs`:

```rust
fn main() {
    println!("Hello, world!");
}
```

- Cargo가 "Hello, world!" 프로그램을 생성
- Cargo는 소스 파일이 `src` 디렉토리 안에 있기를 기대 → 최상위 디렉토리는 README 파일, 라이선스, 설정 파일용

### Cargo 프로젝트 빌드 및 실행하기

#### `cargo build`로 빌드하기

`hello_cargo` 디렉토리에서:

```console
$ cargo build
   Compiling hello_cargo v0.1.0 (file:///projects/hello_cargo)
    Finished dev [unoptimized + debuginfo] target(s) in 2.85 secs
```

이것은 `target/debug/hello_cargo` (Windows에서는 `target\debug\hello_cargo.exe`)에 실행 파일 생성.

실행 파일 실행:

```console
$ ./target/debug/hello_cargo
Hello, world!
```

- 이 명령은 최상위 레벨에 의존성의 정확한 버전을 추적하는 `Cargo.lock` 파일도 생성

#### `cargo run`으로 실행하기

컴파일과 실행을 하나의 명령으로:

```console
$ cargo run
    Finished dev [unoptimized + debuginfo] target(s) in 0.0 secs
     Running `target/debug/hello_cargo`
Hello, world!
```

- Cargo는 파일이 변경되지 않았으면 다시 빌드하지 않음
- 소스 코드를 수정하면:

```console
$ cargo run
   Compiling hello_cargo v0.1.0 (file:///projects/hello_cargo)
    Finished dev [unoptimized + debuginfo] target(s) in 0.33 secs
     Running `target/debug/hello_cargo`
Hello, world!
```

#### `cargo check`로 검사하기

실행 파일을 생성하지 않고 코드 컴파일 여부만 빠르게 확인:

```console
$ cargo check
   Checking hello_cargo v0.1.0 (file:///projects/hello_cargo)
    Finished dev [unoptimized + debuginfo] target(s) in 0.32 secs
```

- `cargo check`는 실행 파일 생성을 건너뛰기 때문에 `cargo build`보다 훨씬 빠름
- 많은 Rustacean이 코드 작성 중 주기적으로 이것을 실행하여 컴파일 여부 확인 → 실행 파일이 필요할 때 `cargo build` 실행

### Cargo 빠른 참조

- `cargo new` - 새 프로젝트 생성
- `cargo build` - 프로젝트 빌드
- `cargo run` - 프로젝트 빌드 및 실행을 한 단계로
- `cargo check` - 바이너리 생성 없이 코드 컴파일 확인
- 빌드 출력 위치 - Cargo는 결과를 `target/debug` 디렉토리에 저장 (코드와 같은 디렉토리가 아님)

명령은 모든 운영체제에서 동일.

### 릴리스용 빌드

프로젝트가 릴리스 준비가 되면:

```console
$ cargo build --release
```

- 이것은 최적화와 함께 컴파일하여 `target/debug` 대신 `target/release`에 실행 파일 생성
- 최적화는 코드를 더 빠르게 실행하지만 컴파일 시간이 늘어남 → 사용자에게 제공할 최종 프로그램에 사용

코드의 실행 시간을 벤치마킹할 때는 항상 `cargo build --release` 사용.

### Cargo의 규칙 활용하기

단순한 프로젝트에서는 Cargo가 `rustc`보다 큰 가치를 제공하지 않음 → 프로그램이 여러 파일과 의존성으로 복잡해지면 필수적이 됨.

기존 Rust 프로젝트에서 작업하려면:

```console
$ git clone example.org/someproject
$ cd someproject
$ cargo build
```

자세한 정보는 [Cargo 문서](https://doc.rust-lang.org/cargo/) 참조.

### 요약

이 챕터에서 배운 내용:

- `rustup`을 사용하여 Rust 설치
- 최신 버전으로 Rust 업데이트
- 로컬에 설치된 문서 열기
- `rustc`를 직접 사용하여 "Hello, world!" 프로그램 작성 및 실행
- Cargo의 규칙을 사용하여 새 프로젝트 생성 및 실행

---

## 숫자 맞추기 게임 튜토리얼

## 추리 게임 프로그래밍

> 원문: https://doc.rust-lang.org/book/ch02-00-guessing-game-tutorial.html

### 개요

이 챕터는 완전한 추리 게임 프로그램을 만들면서 기본적인 Rust 개념을 소개. 게임은 1과 100 사이의 난수를 생성 → 플레이어에게 추측을 입력받아 각 추측이 너무 작은지·너무 큰지·정확한지 피드백 제공.

### 새 프로젝트 설정

```console
$ cargo new guessing_game
$ cd guessing_game
```

이것은 `Cargo.toml`과 `src/main.rs`를 포함하는 초기 프로젝트 구조를 생성.

Cargo.toml:

```toml
[package]
name = "guessing_game"
version = "0.1.0"
edition = "2024"

[dependencies]
```

### 추측 처리하기

첫 번째 단계는 키보드에서 사용자 입력을 받는 것.

src/main.rs (초기 버전):

```rust
use std::io;

fn main() {
    println!("Guess the number!");

    println!("Please input your guess.");

    let mut guess = String::new();

    io::stdin()
        .read_line(&mut guess)
        .expect("Failed to read line");

    println!("You guessed: {guess}");
}
```

#### 핵심 개념

- `use std::io;` - 입출력 라이브러리를 스코프로 가져옴
- `let mut guess = String::new();` - 사용자 입력을 저장할 가변 변수 생성
  - Rust에서 변수는 기본적으로 불변
  - `mut` 키워드로 가변으로 만듦
- `io::stdin().read_line(&mut guess)` - 표준 입력에서 한 줄을 읽음
  - `&mut guess`는 문자열에 대한 가변 참조를 전달
  - `expect()`는 `Result` 타입을 처리 → 읽기 실패 시 크래시
- `{guess}` - 변수 출력을 위한 `println!` 매크로의 플레이스홀더 문법

### 비밀 번호 생성하기

난수를 생성하기 위해 `rand` 크레이트 추가.

Cargo.toml 업데이트:

```toml
[dependencies]
rand = "0.8.5"
```

src/main.rs 업데이트:

```rust
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    println!("The secret number is: {secret_number}");

    println!("Please input your guess.");

    let mut guess = String::new();

    io::stdin()
        .read_line(&mut guess)
        .expect("Failed to read line");

    println!("You guessed: {guess}");
}
```

#### Cargo 의존성 관리

- `cargo build` - 의존성 다운로드 및 컴파일
- `cargo run` - 프로그램 컴파일 및 실행
- Cargo.lock - 의존성 버전을 잠금으로써 재현 가능한 빌드 보장
- `cargo update` - 의존성을 최신 호환 버전으로 업데이트
- `cargo doc --open` - 의존성에 대한 로컬 문서 열기

### 추측과 비밀 번호 비교하기

문자열 입력을 숫자로 변환하고 비교:

```rust
use std::cmp::Ordering;
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    println!("The secret number is: {secret_number}");

    println!("Please input your guess.");

    let mut guess = String::new();

    io::stdin()
        .read_line(&mut guess)
        .expect("Failed to read line");

    let guess: u32 = guess.trim().parse().expect("Please type a number!");

    println!("You guessed: {guess}");

    match guess.cmp(&secret_number) {
        Ordering::Less => println!("Too small!"),
        Ordering::Greater => println!("Too big!"),
        Ordering::Equal => println!("You win!"),
    }
}
```

#### 타입 변환

- 섀도잉(Shadowing) - 타입을 변환할 때 변수 이름 재사용
- `guess.trim()` - 공백과 줄바꿈 문자 제거
- `.parse()` - 문자열을 다른 타입으로 변환, `Result` 반환
- 타입 어노테이션 - `let guess: u32`는 타입을 명시적으로 지정

#### match 표현식

`match` 문은 비교의 다른 결과를 처리:

- 갈래(Arms) - 각 패턴과 관련 코드
- 패턴 - 매칭할 값들 (`Ordering::Less` 등)
- 첫 번째로 성공한 패턴을 매칭하고 그 코드를 실행

### 반복으로 여러 추측 허용하기

반복적인 추측을 위해 `loop` 추가:

```rust
use std::cmp::Ordering;
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    loop {
        println!("Please input your guess.");

        let mut guess = String::new();

        io::stdin()
            .read_line(&mut guess)
            .expect("Failed to read line");

        let guess: u32 = match guess.trim().parse() {
            Ok(num) => num,
            Err(_) => continue,
        };

        println!("You guessed: {guess}");

        match guess.cmp(&secret_number) {
            Ordering::Less => println!("Too small!"),
            Ordering::Greater => println!("Too big!"),
            Ordering::Equal => {
                println!("You win!");
                break;
            }
        }
    }
}
```

#### 반복 제어

- `loop` - 무한 반복 생성
- `break` - 정확한 숫자를 맞추면 반복 종료
- `continue` - 다음 반복으로 건너뜀

#### 잘못된 입력 처리

`expect()` 대신 `parse()` 결과와 함께 `match` 사용:

- `Ok(num)` - 성공적으로 파싱됨, 숫자 사용
- `Err(_)` - 파싱 실패, `_`는 오류 세부 사항 무시, `continue`는 다시 반복

### 최종 코드 (완전한 추리 게임)

```rust
use std::cmp::Ordering;
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    loop {
        println!("Please input your guess.");

        let mut guess = String::new();

        io::stdin()
            .read_line(&mut guess)
            .expect("Failed to read line");

        let guess: u32 = match guess.trim().parse() {
            Ok(num) => num,
            Err(_) => continue,
        };

        println!("You guessed: {guess}");

        match guess.cmp(&secret_number) {
            Ordering::Less => println!("Too small!"),
            Ordering::Greater => println!("Too big!"),
            Ordering::Equal => {
                println!("You win!");
                break;
            }
        }
    }
}
```

### 요약

이 프로젝트에서 소개한 내용:

- `let` 문 - 변수 선언
- `match` 표현식 - 패턴 매칭과 제어 흐름
- 메서드 - 타입에 대한 연산 (`.trim()`, `.parse()`, `.cmp()`)
- 연관 함수 - `String::new()` 및 `rand::thread_rng()`와 같은 타입 수준 함수
- 외부 크레이트 - Cargo를 통해 `rand` 크레이트 사용
- 오류 처리 - `Result`와 `expect()` 또는 `match` 사용
- 타입 변환 - 문자열과 숫자 간 변환
- 반복 - `loop`, `break`, `continue` 사용

이러한 개념들은 Rust 책의 이후 챕터에서 더 자세히 탐구됨.
