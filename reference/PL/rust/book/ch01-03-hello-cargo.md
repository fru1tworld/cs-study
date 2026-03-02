# Hello, Cargo!

Cargo는 Rust의 빌드 시스템이자 패키지 관리자입니다. 대부분의 Rustacean은 이 도구를 사용하여 Rust 프로젝트를 관리합니다. Cargo가 다음과 같은 많은 작업을 처리해 주기 때문입니다:

- 코드 빌드
- 코드가 의존하는 라이브러리 다운로드
- 해당 라이브러리(*의존성*이라고 함) 빌드

가장 단순한 Rust 프로그램은 의존성이 없습니다. 더 복잡한 Rust 프로그램을 작성하면서 의존성을 추가하게 될 것이고, Cargo를 사용하여 프로젝트를 시작하면 의존성 추가가 훨씬 쉬워집니다.

공식 설치 프로그램을 사용했다면 Cargo는 Rust와 함께 설치됩니다. 설치 여부를 확인하려면:

```console
$ cargo --version
```

## Cargo로 프로젝트 생성하기

projects 디렉토리로 이동하고 다음을 실행하세요:

```console
$ cargo new hello_cargo
$ cd hello_cargo
```

이것은 다음을 포함하는 새 디렉토리를 생성합니다:

- `Cargo.toml` 파일
- `main.rs` 파일이 있는 `src` 디렉토리
- 새 Git 저장소와 `.gitignore` 파일 (이미 Git 저장소 안에 있지 않은 경우)

### Cargo.toml 설정

텍스트 편집기에서 `Cargo.toml`을 여세요:

```toml
[package]
name = "hello_cargo"
version = "0.1.0"
edition = "2024"

[dependencies]
```

이 파일은 *TOML*(*Tom's Obvious, Minimal Language*) 형식입니다:

- `[package]` - 패키지 설정을 위한 섹션 헤딩
- `name`, `version`, `edition` - 프로그램을 컴파일하는 데 필요한 설정 정보
- `[dependencies]` - 프로젝트의 의존성을 나열하는 섹션 (코드 패키지를 *크레이트*라고 함)

### 생성된 소스 코드

`src/main.rs`:

```rust
fn main() {
    println!("Hello, world!");
}
```

Cargo가 "Hello, world!" 프로그램을 생성해 주었습니다. Cargo는 소스 파일이 `src` 디렉토리 안에 있기를 기대하며, 최상위 디렉토리는 README 파일, 라이선스, 설정 파일을 위한 것입니다.

## Cargo 프로젝트 빌드 및 실행하기

### `cargo build`로 빌드하기

`hello_cargo` 디렉토리에서:

```console
$ cargo build
   Compiling hello_cargo v0.1.0 (file:///projects/hello_cargo)
    Finished dev [unoptimized + debuginfo] target(s) in 2.85 secs
```

이것은 `target/debug/hello_cargo` (Windows에서는 `target\debug\hello_cargo.exe`)에 실행 파일을 생성합니다.

실행 파일 실행:

```console
$ ./target/debug/hello_cargo
Hello, world!
```

이것은 또한 최상위 레벨에 의존성의 정확한 버전을 추적하는 `Cargo.lock` 파일을 생성합니다.

### `cargo run`으로 실행하기

컴파일과 실행을 하나의 명령으로:

```console
$ cargo run
    Finished dev [unoptimized + debuginfo] target(s) in 0.0 secs
     Running `target/debug/hello_cargo`
Hello, world!
```

Cargo는 똑똑합니다—파일이 변경되지 않았으면 다시 빌드하지 않습니다. 소스 코드를 수정하면:

```console
$ cargo run
   Compiling hello_cargo v0.1.0 (file:///projects/hello_cargo)
    Finished dev [unoptimized + debuginfo] target(s) in 0.33 secs
     Running `target/debug/hello_cargo`
Hello, world!
```

### `cargo check`로 검사하기

실행 파일을 생성하지 않고 코드가 컴파일되는지 빠르게 확인:

```console
$ cargo check
   Checking hello_cargo v0.1.0 (file:///projects/hello_cargo)
    Finished dev [unoptimized + debuginfo] target(s) in 0.32 secs
```

`cargo check`는 실행 파일 생성을 건너뛰기 때문에 `cargo build`보다 훨씬 빠릅니다. 많은 Rustacean이 코드를 작성하는 동안 주기적으로 이것을 실행하여 컴파일되는지 확인한 다음, 실행 파일을 사용할 준비가 되면 `cargo build`를 실행합니다.

## Cargo 빠른 참조

- **`cargo new`** - 새 프로젝트 생성
- **`cargo build`** - 프로젝트 빌드
- **`cargo run`** - 프로젝트 빌드 및 실행을 한 단계로
- **`cargo check`** - 바이너리 생성 없이 코드 컴파일 확인
- **빌드 출력 위치** - Cargo는 결과를 `target/debug` 디렉토리에 저장 (코드와 같은 디렉토리가 아님)

명령은 모든 운영 체제에서 동일합니다.

## 릴리스용 빌드

프로젝트가 릴리스 준비가 되면:

```console
$ cargo build --release
```

이것은 최적화와 함께 컴파일하여 `target/debug` 대신 `target/release`에 실행 파일을 생성합니다. 최적화는 코드를 더 빠르게 실행하지만 컴파일 시간이 늘어납니다. 사용자에게 제공할 최종 프로그램에 이것을 사용하세요.

코드의 실행 시간을 벤치마킹할 때는 항상 `cargo build --release`를 사용하세요.

## Cargo의 규칙 활용하기

단순한 프로젝트에서는 Cargo가 `rustc`보다 큰 가치를 제공하지 않지만, 프로그램이 여러 파일과 의존성으로 복잡해지면 필수적이 됩니다.

기존 Rust 프로젝트에서 작업하려면:

```console
$ git clone example.org/someproject
$ cd someproject
$ cargo build
```

자세한 정보는 [Cargo 문서](https://doc.rust-lang.org/cargo/)를 참조하세요.

## 요약

이 챕터에서 배운 내용:

- `rustup`을 사용하여 Rust 설치
- 최신 버전으로 Rust 업데이트
- 로컬에 설치된 문서 열기
- `rustc`를 직접 사용하여 "Hello, world!" 프로그램 작성 및 실행
- Cargo의 규칙을 사용하여 새 프로젝트 생성 및 실행
