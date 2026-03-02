# `cargo install`로 Crates.io에서 바이너리 설치하기

`cargo install` 명령을 사용하면 바이너리 크레이트를 로컬에 설치하고 사용할 수 있습니다. 이것은 시스템 패키지를 대체하려는 것이 아니라, Rust 개발자가 [crates.io](https://crates.io/)에서 다른 사람들이 공유한 도구를 편리하게 설치할 수 있는 방법입니다. 바이너리 대상이 있는 패키지만 설치할 수 있다는 점에 유의하세요. *바이너리 대상*은 크레이트에 *src/main.rs* 파일이 있거나 바이너리로 지정된 다른 파일이 있는 경우 생성되는 실행 가능한 프로그램입니다. 이는 자체적으로 실행할 수 없지만 다른 프로그램에 포함하기에 적합한 라이브러리 대상과 대조됩니다. 일반적으로 크레이트에는 *README* 파일에 크레이트가 라이브러리인지, 바이너리 대상이 있는지 또는 둘 다인지에 대한 정보가 있습니다.

`cargo install`로 설치된 모든 바이너리는 설치 루트의 *bin* 폴더에 저장됩니다. *rustup.rs*를 사용하여 Rust를 설치했고 사용자 정의 구성이 없는 경우 이 디렉토리는 *$HOME/.cargo/bin*입니다. `cargo install`로 설치한 프로그램을 실행하려면 해당 디렉토리가 `$PATH`에 있는지 확인하세요.

예를 들어 12장에서 파일 검색을 위한 `grep` 도구의 Rust 구현인 `ripgrep`이 있다고 언급했습니다. `ripgrep`을 설치하려면 다음을 실행할 수 있습니다:

```console
$ cargo install ripgrep
    Updating crates.io index
  Downloaded ripgrep v14.1.1
  Downloaded 1 crate (213.6 KB) in 0.40s
  Installing ripgrep v14.1.1
--snip--
   Compiling ripgrep v14.1.1
    Finished `release` profile [optimized + debuginfo] target(s) in 6.73s
  Installing ~/.cargo/bin/rg
   Installed package `ripgrep v14.1.1` (executable `rg`)
```

출력의 마지막에서 두 번째 줄은 설치된 바이너리의 위치와 이름을 보여줍니다. `ripgrep`의 경우 `rg`입니다. 앞서 언급한 대로 설치 디렉토리가 `$PATH`에 있으면 `rg --help`를 실행하고 더 빠르고 Rust 스러운 파일 검색 도구를 사용할 수 있습니다!
