# 설치

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

Windows에서는 [https://www.rust-lang.org/tools/install](https://www.rust-lang.org/tools/install)로 이동하여 Rust 설치 지침을 따르세요. 설치 과정 중 어느 시점에서 Visual Studio를 설치하라는 메시지가 표시됩니다. 이는 프로그램을 컴파일하는 데 필요한 링커와 네이티브 라이브러리를 제공합니다. 이 단계에 대해 더 많은 도움이 필요하면 [https://rust-lang.github.io/rustup/installation/windows-msvc.html](https://rust-lang.github.io/rustup/installation/windows-msvc.html)을 참조하세요.

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

이 책은 Rust 코드를 작성하는 데 어떤 도구를 사용하는지에 대해 가정하지 않습니다. 거의 모든 텍스트 편집기로 작업을 수행할 수 있습니다! 그러나 많은 텍스트 편집기와 통합 개발 환경(IDE)에는 Rust에 대한 내장 지원이 있습니다. Rust 웹사이트의 [도구 페이지](https://www.rust-lang.org/tools)에서 많은 편집기와 IDE의 최신 목록을 항상 찾을 수 있습니다.

### 이 책으로 오프라인 작업하기

여러 예제에서 표준 라이브러리 외의 Rust 패키지를 사용합니다. 이러한 예제를 작업하려면 인터넷 연결이 필요하거나 해당 의존성을 미리 다운로드해야 합니다. 의존성을 미리 다운로드하려면 다음 명령을 실행하세요. (`cargo`가 무엇이고 각 명령이 무엇을 하는지는 나중에 자세히 설명합니다.)

```console
$ cargo new get-dependencies
$ cd get-dependencies
$ cargo add rand@0.8.5 trpl@0.2.0
```

이렇게 하면 이러한 패키지의 다운로드가 캐시되어 나중에 다운로드할 필요가 없습니다. 이 명령을 실행한 후에는 `get-dependencies` 폴더를 유지할 필요가 없습니다. 이 명령을 실행했다면, 책의 나머지 부분에서 모든 `cargo` 명령과 함께 `--offline` 플래그를 사용하여 네트워크를 사용하지 않고 이러한 캐시된 버전을 사용할 수 있습니다.
