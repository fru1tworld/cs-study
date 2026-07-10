# 시작하기

# 시작하기

> **원문:** https://doc.rust-lang.org/book/ch01-00-getting-started.html

이 챕터에서는 다음 내용을 다룹니다:

- Linux, macOS, Windows에서 **Rust 설치하기**
- **"Hello, world!" 프로그램** 작성하기
- Rust의 패키지 관리자이자 빌드 시스템인 **`cargo`** 사용하기

Rust를 시작하는 첫 단계로, 설치 방법부터 알아보고 간단한 프로그램을 작성해 보겠습니다.

---

# 설치

> **원문:** https://doc.rust-lang.org/book/ch01-01-installation.html

첫 번째 단계는 Rust를 설치하는 것입니다. Rust 버전과 관련 도구를 관리하는 커맨드 라인 도구인 `rustup`을 통해 Rust를 다운로드합니다. 다운로드를 위해 인터넷 연결이 필요합니다.

> **참고:** 어떤 이유로 `rustup`을 사용하고 싶지 않다면, [다른 Rust 설치 방법 페이지](https://forge.rust-lang.org/infra/other-installation-methods.html)에서 더 많은 옵션을 확인하세요.

다음 단계들은 Rust 컴파일러의 최신 안정 버전을 설치합니다. Rust의 안정성 보장은 이 책의 모든 예제가 최신 Rust 버전에서도 계속 컴파일된다는 것을 의미합니다. Rust는 오류 메시지와 경고를 자주 개선하기 때문에 출력이 버전마다 약간 다를 수 있습니다. 다시 말해, 이 단계들을 사용하여 설치한 최신 안정 버전의 Rust는 이 책의 내용과 예상대로 작동해야 합니다.

### 커맨드 라인 표기법

이 챕터와 책 전체에서 터미널에서 사용되는 몇 가지 명령을 보여줍니다. 터미널에 입력해야 하는 줄은 모두 `$`로 시작합니다. `$` 문자를 입력할 필요는 없습니다. 이것은 각 명령의 시작을 나타내는 커맨드 라인 프롬프트입니다. `$`로 시작하지 않는 줄은 일반적으로 이전 명령의 출력을 보여줍니다. 또한, PowerShell 전용 예제는 `$` 대신 `>`를 사용합니다.

### Linux 또는 macOS에서 `rustup` 설치하기

Linux 또는 macOS를 사용하는 경우, 터미널을 열고 다음 명령을 입력하세요:

```console
$ curl --proto '=https' --tlsv1.2 https://sh.rustup.rs -sSf | sh
```

이 명령은 스크립트를 다운로드하고 `rustup` 도구 설치를 시작합니다. 이 도구는 Rust의 최신 안정 버전을 설치합니다. 비밀번호를 입력하라는 메시지가 나타날 수 있습니다. 설치가 성공하면 다음 줄이 표시됩니다:

```text
Rust is installed now. Great!
```

또한 *링커*가 필요합니다. 링커는 Rust가 컴파일된 출력을 하나의 파일로 결합하는 데 사용하는 프로그램입니다. 이미 설치되어 있을 가능성이 높습니다. 링커 오류가 발생하면 일반적으로 링커를 포함하는 C 컴파일러를 설치해야 합니다. 일부 일반적인 Rust 패키지가 C 코드에 의존하고 C 컴파일러가 필요하기 때문에 C 컴파일러도 유용합니다.

macOS에서는 다음을 실행하여 C 컴파일러를 얻을 수 있습니다:

```console
$ xcode-select --install
```

Linux 사용자는 일반적으로 배포판 문서에 따라 GCC 또는 Clang을 설치해야 합니다. 예를 들어, Ubuntu를 사용하는 경우 `build-essential` 패키지를 설치할 수 있습니다.

### Windows에서 `rustup` 설치하기

Windows에서는 [https://www.rust-lang.org/tools/install](https://www.rust-lang.org/tools/install)로 이동하여 Rust 설치 지침을 따르세요. 설치 과정 중 어느 시점에서 Visual Studio를 설치하라는 메시지가 표시됩니다. 이는 프로그램을 컴파일하는 데 필요한 링커와 네이티브 라이브러리를 제공합니다. 이 단계에서 더 많은 도움이 필요하면 [https://rust-lang.github.io/rustup/installation/windows-msvc.html](https://rust-lang.github.io/rustup/installation/windows-msvc.html)을 참조하세요.

이 책의 나머지 부분에서는 *cmd.exe*와 PowerShell 모두에서 작동하는 명령을 사용합니다. 특정 차이점이 있으면 어떤 것을 사용해야 하는지 설명합니다.

### 문제 해결

Rust가 올바르게 설치되었는지 확인하려면 셸을 열고 다음 줄을 입력하세요:

```console
$ rustc --version
```

출시된 최신 안정 버전의 버전 번호, 커밋 해시, 커밋 날짜가 다음 형식으로 표시되어야 합니다:

```text
rustc x.y.z (abcabcabc yyyy-mm-dd)
```

이 정보가 표시되면 Rust를 성공적으로 설치한 것입니다! 이 정보가 표시되지 않으면 Rust가 `%PATH%` 시스템 변수에 있는지 다음과 같이 확인하세요.

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

모든 것이 올바르지만 Rust가 여전히 작동하지 않는 경우, 도움을 받을 수 있는 여러 곳이 있습니다. [커뮤니티 페이지](https://www.rust-lang.org/community)에서 다른 Rustacean(우리가 스스로를 부르는 재미있는 별명)과 연락하는 방법을 찾아보세요.

### 업데이트 및 제거

`rustup`을 통해 Rust가 설치되면, 새로 출시된 버전으로 업데이트하는 것은 쉽습니다. 셸에서 다음 업데이트 스크립트를 실행하세요:

```console
$ rustup update
```

Rust와 `rustup`을 제거하려면 셸에서 다음 제거 스크립트를 실행하세요:

```console
$ rustup self uninstall
```

### 로컬 문서 읽기

Rust 설치에는 오프라인에서 읽을 수 있도록 로컬 문서 사본도 포함됩니다. `rustup doc`을 실행하여 브라우저에서 로컬 문서를 열 수 있습니다.

표준 라이브러리에서 제공하는 타입이나 함수가 무엇을 하는지 또는 어떻게 사용하는지 확실하지 않을 때마다 API(Application Programming Interface) 문서를 사용하여 알아보세요!

### 텍스트 편집기와 IDE 사용하기

이 책은 Rust 코드를 작성하는 데 어떤 도구를 사용하는지 가정하지 않습니다. 거의 모든 텍스트 편집기로 작업을 수행할 수 있습니다! 그러나 많은 텍스트 편집기와 통합 개발 환경(IDE)에는 Rust에 대한 내장 지원이 있습니다. Rust 웹사이트의 [도구 페이지](https://www.rust-lang.org/tools)에서 많은 편집기와 IDE의 최신 목록을 항상 찾을 수 있습니다.

### 이 책으로 오프라인 작업하기

여러 예제에서 표준 라이브러리 외의 Rust 패키지를 사용합니다. 이러한 예제를 작업하려면 인터넷 연결이 필요하거나 해당 의존성을 미리 다운로드해야 합니다. 의존성을 미리 다운로드하려면 다음 명령을 실행하세요. (`cargo`가 무엇이고 각 명령이 무엇을 하는지는 나중에 자세히 설명합니다.)

```console
$ cargo new get-dependencies
$ cd get-dependencies
$ cargo add rand@0.8.5 trpl@0.2.0
```

이렇게 하면 이러한 패키지의 다운로드가 캐시되어 나중에 다운로드할 필요가 없습니다. 이 명령을 실행한 후에는 `get-dependencies` 폴더를 유지할 필요가 없습니다. 이 명령을 실행했다면, 책의 나머지 부분에서 모든 `cargo` 명령과 함께 `--offline` 플래그를 사용하여 네트워크를 사용하지 않고 이러한 캐시된 버전을 사용할 수 있습니다.

---

# Hello, World!

> **원문:** https://doc.rust-lang.org/book/ch01-02-hello-world.html

이제 Rust를 설치했으니, 첫 번째 Rust 프로그램을 작성할 시간입니다. 새로운 언어를 배울 때 `Hello, world!` 텍스트를 화면에 출력하는 작은 프로그램을 작성하는 것이 전통이므로, 여기서도 똑같이 하겠습니다!

> **참고:** 이 책은 커맨드 라인에 대한 기본적인 친숙함을 가정합니다. Rust는 편집이나 도구, 코드가 어디에 있는지에 특정한 요구를 하지 않으므로, 커맨드 라인 대신 IDE를 사용하고 싶다면 자유롭게 좋아하는 IDE를 사용하세요. 많은 IDE가 이제 어느 정도 Rust 지원을 제공합니다. 자세한 내용은 IDE 문서를 확인하세요. Rust 팀은 `rust-analyzer`를 통해 훌륭한 IDE 지원을 가능하게 하는 데 집중해 왔습니다. 자세한 내용은 부록 D를 참조하세요.

## 프로젝트 디렉토리 설정

Rust 코드를 저장할 디렉토리를 만드는 것부터 시작합니다. Rust에게는 코드가 어디에 있는지 중요하지 않지만, 이 책의 연습과 프로젝트를 위해 홈 디렉토리에 *projects* 디렉토리를 만들고 모든 프로젝트를 그 안에 보관하는 것을 권장합니다.

터미널을 열고 다음 명령을 입력하여 *projects* 디렉토리와 *projects* 디렉토리 내에 "Hello, world!" 프로젝트를 위한 디렉토리를 만드세요.

**Linux, macOS, Windows PowerShell의 경우:**

```console
$ mkdir ~/projects
$ cd ~/projects
$ mkdir hello_world
$ cd hello_world
```

**Windows CMD의 경우:**

```cmd
> mkdir "%USERPROFILE%\projects"
> cd /d "%USERPROFILE%\projects"
> mkdir hello_world
> cd hello_world
```

## Rust 프로그램 기초

다음으로, 새 소스 파일을 만들고 *main.rs*라고 이름 지으세요. Rust 파일은 항상 *.rs* 확장자로 끝납니다. 파일 이름에 둘 이상의 단어를 사용하는 경우, 규칙은 밑줄을 사용하여 단어를 구분하는 것입니다. 예를 들어, *helloworld.rs* 대신 *hello_world.rs*를 사용하세요.

이제 방금 만든 *main.rs* 파일을 열고 다음 코드를 입력하세요.

**파일명: main.rs**

```rust
fn main() {
    println!("Hello, world!");
}
```

**Listing 1-1:** `Hello, world!`를 출력하는 프로그램

파일을 저장하고 *~/projects/hello_world* 디렉토리의 터미널 창으로 돌아갑니다.

**Linux 또는 macOS에서:**

```console
$ rustc main.rs
$ ./main
Hello, world!
```

**Windows에서:**

```cmd
> rustc main.rs
> .\main
Hello, world!
```

운영 체제에 관계없이 `Hello, world!` 문자열이 터미널에 출력되어야 합니다. 이 출력이 보이지 않으면 설치 섹션의 "문제 해결" 부분을 다시 참조하여 도움을 받으세요.

`Hello, world!`가 출력되었다면, 축하합니다! 공식적으로 Rust 프로그램을 작성했습니다. 이것은 당신을 Rust 프로그래머로 만듭니다—환영합니다!

## Rust 프로그램 해부하기

이 "Hello, world!" 프로그램을 자세히 살펴봅시다. 퍼즐의 첫 번째 조각은 다음과 같습니다:

```rust
fn main() {

}
```

이 줄들은 `main`이라는 함수를 정의합니다. `main` 함수는 특별합니다: 모든 실행 가능한 Rust 프로그램에서 항상 가장 먼저 실행되는 코드입니다. 여기서 첫 번째 줄은 매개변수가 없고 아무것도 반환하지 않는 `main`이라는 함수를 선언합니다. 매개변수가 있다면 괄호 `()` 안에 들어갑니다.

함수 본문은 `{}`로 감싸져 있습니다. Rust는 모든 함수 본문 주위에 중괄호가 필요합니다. 여는 중괄호를 함수 선언과 같은 줄에 배치하고 그 사이에 공백 하나를 추가하는 것이 좋은 스타일입니다.

> **참고:** Rust 프로젝트 전체에서 표준 스타일을 유지하려면 `rustfmt`라는 자동 포맷터 도구를 사용하여 특정 스타일로 코드를 포맷할 수 있습니다(`rustfmt`에 대한 자세한 내용은 부록 D 참조). Rust 팀은 `rustc`와 마찬가지로 이 도구를 표준 Rust 배포판에 포함시켰으므로 이미 컴퓨터에 설치되어 있어야 합니다!

`main` 함수의 본문에는 다음 코드가 있습니다:

```rust
    println!("Hello, world!");
```

이 줄이 이 작은 프로그램의 모든 작업을 수행합니다: 텍스트를 화면에 출력합니다. 여기서 주목할 세 가지 중요한 세부 사항이 있습니다.

**첫째**, `println!`은 Rust 매크로를 호출합니다. 함수를 호출했다면 `!` 없이 `println`으로 입력되었을 것입니다. Rust 매크로는 Rust 구문을 확장하기 위해 코드를 생성하는 코드를 작성하는 방법이며, 20장에서 더 자세히 논의합니다. 지금은 `!`를 사용하면 일반 함수 대신 매크로를 호출한다는 것과 매크로가 항상 함수와 같은 규칙을 따르지는 않는다는 것만 알면 됩니다.

**둘째**, `"Hello, world!"` 문자열을 볼 수 있습니다. 이 문자열을 `println!`의 인수로 전달하면 문자열이 화면에 출력됩니다.

**셋째**, 줄 끝에 세미콜론(`;`)으로 끝납니다. 이것은 이 표현식이 끝났고 다음 표현식이 시작할 준비가 되었음을 나타냅니다. 대부분의 Rust 코드 줄은 세미콜론으로 끝납니다.

## 컴파일과 실행

방금 새로 만든 프로그램을 실행했으므로, 프로세스의 각 단계를 살펴봅시다.

Rust 프로그램을 실행하기 전에 Rust 컴파일러를 사용하여 컴파일해야 합니다. `rustc` 명령을 입력하고 소스 파일 이름을 전달하면 됩니다:

```console
$ rustc main.rs
```

C나 C++ 배경이 있다면, 이것이 `gcc`나 `clang`과 유사하다는 것을 알 수 있습니다. 성공적으로 컴파일한 후 Rust는 바이너리 실행 파일을 출력합니다.

**Linux, macOS, Windows PowerShell에서** 셸에 `ls` 명령을 입력하여 실행 파일을 볼 수 있습니다:

```console
$ ls
main  main.rs
```

Linux와 macOS에서는 두 개의 파일이 보입니다. Windows PowerShell에서도 CMD를 사용할 때와 같은 세 개의 파일이 보입니다.

**Windows CMD에서는** 다음을 입력합니다:

```cmd
> dir /B
main.exe
main.pdb
main.rs
```

이것은 *.rs* 확장자를 가진 소스 코드 파일, 실행 파일(Windows에서는 *main.exe*, 다른 모든 플랫폼에서는 *main*), 그리고 Windows를 사용할 때 *.pdb* 확장자를 가진 디버깅 정보를 포함하는 파일을 보여줍니다. 여기서 *main* 또는 *main.exe* 파일을 다음과 같이 실행합니다:

```console
$ ./main # Windows에서는 .\main
```

*main.rs*가 "Hello, world!" 프로그램이라면 이 줄은 터미널에 `Hello, world!`를 출력합니다.

Ruby, Python, JavaScript와 같은 동적 언어에 더 익숙하다면, 프로그램을 별도의 단계로 컴파일하고 실행하는 것에 익숙하지 않을 수 있습니다. Rust는 *사전 컴파일(ahead-of-time compiled)* 언어입니다. 즉, 프로그램을 컴파일하고 실행 파일을 다른 사람에게 줄 수 있으며, 그 사람은 Rust가 설치되어 있지 않아도 실행할 수 있습니다. *.rb*, *.py*, *.js* 파일을 누군가에게 주면 그 사람은 각각 Ruby, Python, JavaScript 구현이 설치되어 있어야 합니다. 그러나 그러한 언어에서는 프로그램을 컴파일하고 실행하는 데 하나의 명령만 필요합니다. 모든 것은 언어 설계에서의 트레이드오프입니다.

단순한 프로그램에서는 `rustc`로 컴파일하는 것만으로도 충분하지만, 프로젝트가 성장함에 따라 모든 옵션을 관리하고 코드를 쉽게 공유하고 싶을 것입니다. 다음으로, 실제 Rust 프로그램을 작성하는 데 도움이 되는 Cargo 도구를 소개합니다.

---

# Hello, Cargo!

> **원문:** https://doc.rust-lang.org/book/ch01-03-hello-cargo.html

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
