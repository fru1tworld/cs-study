# Cargo 워크스페이스

12장에서 바이너리 크레이트와 라이브러리 크레이트를 포함하는 패키지를 빌드했습니다. 프로젝트가 발전함에 따라 라이브러리 크레이트가 계속 커지면서 패키지를 여러 라이브러리 크레이트로 더 분할하고 싶을 수 있습니다. Cargo는 *워크스페이스*라는 기능을 제공하여 함께 개발되는 여러 관련 패키지를 관리하는 데 도움을 줍니다.

## 워크스페이스 생성하기

*워크스페이스*는 동일한 *Cargo.lock* 및 출력 디렉토리를 공유하는 패키지 집합입니다. 워크스페이스를 사용하여 프로젝트를 만들어 보겠습니다—워크스페이스의 구조에 집중할 수 있도록 간단한 코드를 사용합니다. 워크스페이스를 구조화하는 여러 방법이 있으므로 일반적인 방법 하나만 보여드리겠습니다. 바이너리 하나와 라이브러리 두 개를 포함하는 워크스페이스가 있습니다. 주요 기능을 제공하는 바이너리는 두 라이브러리에 의존합니다. 한 라이브러리는 `add_one` 함수를 제공하고 두 번째 라이브러리는 `add_two` 함수를 제공합니다. 이 세 크레이트는 동일한 워크스페이스의 일부가 됩니다. 먼저 워크스페이스용 새 디렉토리를 만듭니다:

```console
$ mkdir add
$ cd add
```

다음으로 *add* 디렉토리에 전체 워크스페이스를 구성하는 *Cargo.toml* 파일을 만듭니다. 이 파일에는 다른 *Cargo.toml* 파일에서 볼 수 있는 `[package]` 섹션이 없습니다. 대신 `[workspace]` 섹션으로 시작합니다:

```toml
[workspace]
resolver = "3"
```

다음으로 *add* 디렉토리 내에서 `cargo new`를 실행하여 `adder` 바이너리 크레이트를 만듭니다:

```console
$ cargo new adder
     Created binary (application) `adder` package
      Adding `adder` as member of workspace at `/projects/add`
```

이 시점에서 `cargo build`를 실행하여 워크스페이스를 빌드할 수 있습니다. *add* 디렉토리의 파일은 다음과 같아야 합니다:

```text
├── Cargo.lock
├── Cargo.toml
├── adder
│   ├── Cargo.toml
│   └── src
│       └── main.rs
└── target
```

워크스페이스에는 컴파일된 아티팩트가 배치될 최상위 레벨에 하나의 *target* 디렉토리가 있습니다; `adder` 패키지에는 자체 *target* 디렉토리가 없습니다. *adder* 디렉토리 내부에서 `cargo build`를 실행하더라도 컴파일된 아티팩트는 *add/adder/target* 대신 *add/target*에 저장됩니다. Cargo는 워크스페이스에서 *target* 디렉토리를 이와 같이 구조화합니다. 워크스페이스의 크레이트가 서로 의존하기 때문입니다. 각 크레이트가 자체 *target* 디렉토리를 가지고 있다면 각 크레이트는 자체 *target* 디렉토리에 아티팩트를 배치하기 위해 워크스페이스의 다른 각 크레이트를 다시 컴파일해야 합니다. 하나의 *target* 디렉토리를 공유하면 크레이트가 불필요한 재빌드를 피할 수 있습니다.

## 워크스페이스에 두 번째 패키지 생성하기

다음으로 워크스페이스에 `add_one`이라는 또 다른 멤버 패키지를 만들겠습니다. 최상위 *Cargo.toml*을 변경하여 `members` 목록에 *add_one* 경로를 지정합니다:

```toml
[workspace]
resolver = "3"
members = ["adder", "add_one"]
```

그런 다음 `add_one`이라는 새 라이브러리 크레이트를 생성합니다:

```console
$ cargo new add_one --lib
     Created library `add_one` package
      Adding `add_one` as member of workspace at `/projects/add`
```

*add* 디렉토리는 이제 다음 디렉토리와 파일을 포함해야 합니다:

```text
├── Cargo.lock
├── Cargo.toml
├── add_one
│   ├── Cargo.toml
│   └── src
│       └── lib.rs
├── adder
│   ├── Cargo.toml
│   └── src
│       └── main.rs
└── target
```

*add_one/src/lib.rs* 파일에 `add_one` 함수를 추가해 보겠습니다:

```rust
pub fn add_one(x: i32) -> i32 {
    x + 1
}
```

이제 바이너리가 있는 `adder` 패키지가 라이브러리를 포함하는 `add_one` 패키지에 의존하도록 할 수 있습니다. 먼저 *adder/Cargo.toml*에 `add_one`에 대한 경로 의존성을 추가해야 합니다.

```toml
[dependencies]
add_one = { path = "../add_one" }
```

Cargo는 워크스페이스의 크레이트가 서로 의존한다고 가정하지 않으므로 의존성 관계를 명시적으로 지정해야 합니다.

다음으로 `adder` 크레이트에서 (`add_one` 크레이트의) `add_one` 함수를 사용해 보겠습니다. *adder/src/main.rs* 파일을 열고 `add_one` 라이브러리 크레이트를 범위로 가져오는 `use` 줄을 맨 위에 추가합니다. 그런 다음 `main` 함수를 변경하여 `add_one` 함수를 호출합니다:

```rust
use add_one;

fn main() {
    let num = 10;
    println!("Hello, world! {num} 더하기 1은 {}!", add_one::add_one(num));
}
```

최상위 *add* 디렉토리에서 `cargo build`를 실행하여 워크스페이스를 빌드해 보겠습니다!

```console
$ cargo build
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.22s
```

*add* 디렉토리에서 바이너리 크레이트를 실행하려면 `-p` 인수와 패키지 이름을 `cargo run`과 함께 사용하여 실행하려는 워크스페이스의 패키지를 지정할 수 있습니다:

```console
$ cargo run -p adder
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
     Running `target/debug/adder`
Hello, world! 10 더하기 1은 11!
```

이렇게 하면 *adder/src/main.rs*의 코드가 실행되고 `add_one` 크레이트에 의존합니다.

### 워크스페이스에서 외부 패키지에 의존하기

워크스페이스에는 각 크레이트의 디렉토리에 있는 개별 *Cargo.toml* 파일이 아닌 최상위 레벨에 *Cargo.lock* 파일이 하나만 있습니다. 이렇게 하면 워크스페이스의 모든 크레이트가 모든 의존성의 동일한 버전을 사용합니다. `rand` 패키지를 *adder/Cargo.toml* 및 *add_one/Cargo.toml* 파일에 추가하면 Cargo는 둘 다 하나의 `rand` 버전으로 해결하고 하나의 *Cargo.lock*에 기록합니다. 워크스페이스의 모든 크레이트가 동일한 의존성을 사용하도록 하면 워크스페이스의 크레이트가 항상 서로 호환됩니다. *add_one/Cargo.toml* 파일의 `[dependencies]` 섹션에 `rand` 크레이트를 추가하여 `add_one` 크레이트에서 `rand` 크레이트를 사용할 수 있습니다:

```toml
[dependencies]
rand = "0.8.5"
```

이제 *add_one/src/lib.rs* 파일에 `use rand;`를 추가할 수 있으며 *add* 디렉토리에서 `cargo build`를 실행하여 전체 워크스페이스를 빌드하면 `rand` 크레이트를 가져와서 컴파일합니다. 범위로 가져온 `rand`를 참조하지 않기 때문에 경고가 하나 발생합니다:

```console
$ cargo build
    Updating crates.io index
  Downloaded rand v0.8.5
   --snip--
   Compiling rand v0.8.5
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
warning: unused import: `rand`
 --> add_one/src/lib.rs:1:5
  |
1 | use rand;
  |     ^^^^
  |
  = note: `#[warn(unused_imports)]` on by default

warning: `add_one` (lib) generated 1 warning
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 10.18s
```

최상위 *Cargo.lock*에는 이제 `add_one`의 `rand` 의존성에 대한 정보가 포함됩니다. 그러나 `rand`가 워크스페이스 어딘가에서 사용되더라도 자체 *Cargo.toml* 파일에도 `rand`를 추가하지 않으면 워크스페이스의 다른 크레이트에서 사용할 수 없습니다. 예를 들어 `adder` 패키지의 *adder/src/main.rs* 파일에 `use rand;`를 추가하면 오류가 발생합니다:

```console
$ cargo build
  --snip--
   Compiling adder v0.1.0 (file:///projects/add/adder)
error[E0432]: unresolved import `rand`
 --> adder/src/main.rs:2:5
  |
2 | use rand;
  |     ^^^^ no external crate `rand`
```

이를 수정하려면 `adder` 패키지의 *Cargo.toml* 파일을 편집하여 `rand`도 의존성임을 표시합니다. `adder` 패키지를 빌드하면 *Cargo.lock*의 `adder` 의존성 목록에 `rand`가 추가되지만 `rand`의 추가 복사본은 다운로드되지 않습니다. Cargo는 `rand` 패키지를 사용하는 워크스페이스의 모든 패키지가 동일한 버전을 사용하도록 보장하여 공간을 절약하고 워크스페이스의 크레이트가 서로 호환되도록 합니다.

### 워크스페이스에 테스트 추가하기

또 다른 개선 사항으로 `add_one` 크레이트 내에 `add_one::add_one` 함수의 테스트를 추가해 보겠습니다:

```rust
pub fn add_one(x: i32) -> i32 {
    x + 1
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        assert_eq!(3, add_one(2));
    }
}
```

이제 최상위 *add* 디렉토리에서 `cargo test`를 실행합니다. 이와 같이 구조화된 워크스페이스에서 `cargo test`를 실행하면 워크스페이스의 모든 크레이트에 대한 테스트가 실행됩니다:

```console
$ cargo test
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `test` profile [unoptimized + debuginfo] target(s) in 0.27s
     Running unittests src/lib.rs (target/debug/deps/add_one-f0253159197f7841)

running 1 test
test tests::it_works ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

     Running unittests src/main.rs (target/debug/deps/adder-49979ff40686fa8e)

running 0 tests

test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

   Doc-tests add_one

running 0 tests

test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
```

출력의 첫 번째 섹션은 `add_one` 크레이트의 `it_works` 테스트가 통과했음을 보여줍니다. 다음 섹션은 `adder` 크레이트에서 테스트가 0개 발견되었음을 보여주고 마지막 섹션은 `add_one` 크레이트에서 문서 테스트가 0개 발견되었음을 보여줍니다.

`-p` 플래그를 사용하고 테스트하려는 크레이트 이름을 지정하여 최상위 디렉토리에서 워크스페이스의 특정 크레이트에 대한 테스트를 실행할 수도 있습니다:

```console
$ cargo test -p add_one
    Finished `test` profile [unoptimized + debuginfo] target(s) in 0.00s
     Running unittests src/lib.rs (target/debug/deps/add_one-b3235fea9a156f74)

running 1 test
test tests::it_works ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

   Doc-tests add_one

running 0 tests

test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
```

이 출력은 `cargo test`가 `add_one` 크레이트에 대한 테스트만 실행하고 `adder` 크레이트 테스트는 실행하지 않았음을 보여줍니다.

워크스페이스의 크레이트를 [crates.io](https://crates.io/)에 게시하는 경우 워크스페이스의 각 크레이트는 별도로 게시해야 합니다. `cargo test`와 마찬가지로 `-p` 플래그를 사용하고 게시하려는 크레이트 이름을 지정하여 워크스페이스의 특정 크레이트를 게시할 수 있습니다.

추가 연습을 위해 `add_one` 크레이트와 유사한 방식으로 이 워크스페이스에 `add_two` 크레이트를 추가하세요!

프로젝트가 성장함에 따라 워크스페이스 사용을 고려하세요: 하나의 큰 코드 덩어리보다 더 작은 개별 구성 요소를 이해하기가 더 쉽습니다. 또한 워크스페이스에 크레이트를 유지하면 자주 동시에 변경되는 경우 크레이트 간의 조정이 더 쉬워질 수 있습니다.
