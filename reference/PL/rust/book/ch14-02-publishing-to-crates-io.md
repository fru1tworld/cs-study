# Crates.io에 크레이트 게시하기

프로젝트의 의존성으로 [crates.io](https://crates.io/)의 패키지를 사용해 왔지만, 자신의 패키지를 게시하여 다른 사람들과 코드를 공유할 수도 있습니다. [crates.io](https://crates.io/)의 크레이트 레지스트리는 패키지의 소스 코드를 배포하므로 주로 오픈 소스인 코드를 호스팅합니다.

Rust와 Cargo에는 게시된 패키지를 사람들이 더 쉽게 찾고 사용할 수 있도록 하는 기능이 있습니다. 이러한 기능 중 일부에 대해 다음에 이야기하고 그 다음에 패키지를 게시하는 방법을 설명하겠습니다.

## 유용한 문서 주석 작성하기

패키지를 정확하게 문서화하면 다른 사용자가 패키지를 어떻게 그리고 언제 사용해야 하는지 알 수 있으므로 문서 작성에 시간을 투자할 가치가 있습니다. 3장에서 두 개의 슬래시 `//`를 사용하여 Rust 코드에 주석을 다는 것에 대해 논의했습니다. Rust에는 *문서 주석*으로 알려진 문서화를 위한 특별한 종류의 주석도 있으며, 이는 HTML 문서를 생성합니다. HTML은 크레이트를 *구현하는* 방법이 아니라 크레이트를 *사용하는* 방법을 알고 싶어하는 프로그래머를 위한 공개 API 항목에 대한 문서 주석의 내용을 표시합니다.

문서 주석은 두 개 대신 세 개의 슬래시 `///`를 사용하고 텍스트 서식 지정을 위한 Markdown 표기법을 지원합니다. 문서 주석을 문서화하는 항목 바로 앞에 배치합니다. Listing 14-1은 `my_crate`라는 크레이트의 `add_one` 함수에 대한 문서 주석을 보여줍니다.

```rust
/// 주어진 숫자에 1을 더합니다.
///
/// # 예제
///
/// ```
/// let arg = 5;
/// let answer = my_crate::add_one(arg);
///
/// assert_eq!(6, answer);
/// ```
pub fn add_one(x: i32) -> i32 {
    x + 1
}
```

*Listing 14-1: 함수에 대한 문서 주석*

여기서 `add_one` 함수가 수행하는 작업에 대한 설명을 제공하고 `예제`라는 제목의 섹션을 시작한 다음 `add_one` 함수를 사용하는 방법을 보여주는 코드를 제공합니다. `cargo doc`을 실행하여 이 문서 주석에서 HTML 문서를 생성할 수 있습니다. 이 명령은 Rust와 함께 배포되는 `rustdoc` 도구를 실행하고 생성된 HTML 문서를 *target/doc* 디렉토리에 배치합니다.

편의를 위해 `cargo doc --open`을 실행하면 현재 크레이트의 문서(및 크레이트의 모든 의존성에 대한 문서)에 대한 HTML을 빌드하고 결과를 웹 브라우저에서 엽니다. `add_one` 함수로 이동하면 문서 주석의 텍스트가 어떻게 렌더링되는지 볼 수 있습니다.

### 자주 사용되는 섹션

Listing 14-1에서 `# 예제` Markdown 제목을 사용하여 HTML에서 "예제"라는 제목의 섹션을 만들었습니다. 다음은 크레이트 작성자가 문서에서 자주 사용하는 몇 가지 다른 섹션입니다:

* **Panics**: 문서화되는 함수가 `panic!`할 수 있는 시나리오. 프로그램을 패닉시키고 싶지 않은 함수 호출자는 이러한 상황에서 함수를 호출하지 않도록 해야 합니다.
* **Errors**: 함수가 `Result`를 반환하는 경우 발생할 수 있는 오류의 종류와 해당 오류가 반환될 수 있는 조건을 설명하면 호출자가 다른 종류의 오류를 다른 방식으로 처리하는 코드를 작성하는 데 도움이 될 수 있습니다.
* **Safety**: 함수가 호출하기에 `unsafe`한 경우(19장에서 안전하지 않음에 대해 논의합니다), 함수가 안전하지 않은 이유와 함수가 호출자가 지켜야 할 불변성을 설명하는 섹션이 있어야 합니다.

대부분의 문서 주석에는 이러한 섹션이 모두 필요하지는 않지만, 코드를 호출하는 사람들이 알고 싶어할 측면을 상기시키기 위한 좋은 체크리스트입니다.

### 테스트로서의 문서 주석

문서 주석에 예제 코드 블록을 추가하면 라이브러리 사용 방법을 보여주는 데 도움이 될 수 있으며, 그렇게 하면 추가 보너스가 있습니다: `cargo test`를 실행하면 문서의 코드 예제가 테스트로 실행됩니다! 예제가 있는 문서보다 더 좋은 것은 없습니다. 그러나 문서가 작성된 이후 코드가 변경되었기 때문에 작동하지 않는 예제보다 더 나쁜 것도 없습니다. Listing 14-1의 `add_one` 함수에 대한 문서와 함께 `cargo test`를 실행하면 다음과 같은 테스트 결과 섹션이 표시됩니다:

```text
   Doc-tests my_crate

running 1 test
test src/lib.rs - add_one (line 5) ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.27s
```

이제 함수나 예제를 변경하여 예제의 `assert_eq!`가 패닉하고 `cargo test`를 다시 실행하면 문서 테스트가 예제와 코드가 서로 동기화되지 않았음을 감지하는 것을 볼 수 있습니다!

### 포함된 항목에 주석 달기

문서 주석 스타일 `//!`은 주석 뒤에 오는 항목이 아닌 주석을 포함하는 항목에 문서를 추가합니다. 일반적으로 이러한 문서 주석은 크레이트 루트 파일(관례상 *src/lib.rs*) 내부 또는 모듈 내부에서 전체 크레이트나 모듈을 문서화하는 데 사용합니다.

예를 들어 `add_one` 함수를 포함하는 `my_crate` 크레이트의 목적을 설명하는 문서를 추가하려면 *src/lib.rs* 파일 시작 부분에 `//!`로 시작하는 문서 주석을 추가합니다. Listing 14-2에 나와 있습니다:

```rust
//! # My Crate
//!
//! `my_crate`는 특정 계산을 더 편리하게 수행하기 위한
//! 유틸리티 모음입니다.

/// 주어진 숫자에 1을 더합니다.
// --snip--
```

*Listing 14-2: `my_crate` 크레이트 전체에 대한 문서*

`//!`로 시작하는 마지막 줄 뒤에 코드가 없다는 점에 유의하세요. `///` 대신 `//!`로 주석을 시작했기 때문에 이 주석 뒤에 오는 항목이 아닌 이 주석을 포함하는 항목을 문서화합니다. 이 경우 해당 항목은 크레이트 루트인 *src/lib.rs* 파일입니다.

이제 `cargo doc --open`을 실행하면 이러한 주석이 `my_crate`의 문서 첫 페이지에 크레이트의 공개 항목 목록 위에 표시됩니다.

항목 내부의 문서 주석은 특히 크레이트와 모듈을 설명하는 데 유용합니다. 이를 사용하여 컨테이너의 전체 목적을 설명하고 사용자가 크레이트의 구성을 이해하도록 도와주세요.

## `pub use`로 편리한 공개 API 내보내기

공개 API의 구조는 크레이트를 게시할 때 주요 고려 사항입니다. 크레이트를 사용하는 사람들은 여러분보다 구조에 덜 익숙하며 크레이트에 큰 모듈 계층 구조가 있는 경우 사용하려는 부분을 찾는 데 어려움을 겪을 수 있습니다.

7장에서 `pub` 키워드를 사용하여 항목을 공개하고 `use` 키워드를 사용하여 항목을 범위에 가져오는 방법을 다뤘습니다. 그러나 크레이트를 개발하는 동안 여러분에게 합리적인 구조가 사용자에게는 그다지 편리하지 않을 수 있습니다. 여러 레벨의 계층 구조로 구조체를 구성하고 싶을 수 있지만, 계층 구조 깊이 정의한 타입을 사용하려는 사람들은 해당 타입이 존재하는지 찾는 데 어려움을 겪을 수 있습니다. 그들은 또한 `use my_crate::UsefulType;` 대신 `use my_crate::some_module::another_module::UsefulType;`을 입력해야 하는 것에 짜증을 낼 수 있습니다.

좋은 소식은 다른 사람들이 다른 라이브러리에서 사용하기에 편리하지 *않은* 구조라면 내부 구성을 재배열할 필요가 없다는 것입니다: 대신 `pub use`를 사용하여 항목을 다시 내보내 비공개 구조와 다른 공개 구조를 만들 수 있습니다. 재내보내기는 한 위치에서 공개 항목을 가져와 마치 다른 위치에서 정의된 것처럼 다른 위치에서 공개합니다.

예를 들어 예술적 개념을 모델링하기 위해 `art`라는 라이브러리를 만들었다고 가정합니다. 이 라이브러리 내에는 두 개의 모듈이 있습니다: Listing 14-3에 표시된 대로 `PrimaryColor`와 `SecondaryColor`라는 두 열거형을 포함하는 `kinds` 모듈과 `mix`라는 함수를 포함하는 `utils` 모듈입니다:

```rust
//! # Art
//!
//! 예술적 개념을 모델링하기 위한 라이브러리.

pub mod kinds {
    /// RYB 색상 모델에 따른 원색.
    pub enum PrimaryColor {
        Red,
        Yellow,
        Blue,
    }

    /// RYB 색상 모델에 따른 이차색.
    pub enum SecondaryColor {
        Orange,
        Green,
        Purple,
    }
}

pub mod utils {
    use crate::kinds::*;

    /// 두 원색을 동일한 양으로 혼합하여
    /// 이차색을 만듭니다.
    pub fn mix(c1: PrimaryColor, c2: PrimaryColor) -> SecondaryColor {
        // --snip--
    }
}
```

*Listing 14-3: `kinds` 및 `utils` 모듈로 구성된 항목이 있는 `art` 라이브러리*

`cargo doc`에 의해 생성된 이 크레이트의 문서 첫 페이지는 `kinds`와 `utils` 모듈을 나열하고 `PrimaryColor`와 `SecondaryColor` 타입이나 `mix` 함수를 나열하지 않습니다. 이를 보려면 모듈을 클릭해야 합니다.

이 라이브러리에 의존하는 다른 크레이트는 현재 정의된 모듈 구조를 지정하여 `art`의 항목을 범위로 가져오는 `use` 문이 필요합니다. Listing 14-4는 `art` 크레이트의 `PrimaryColor` 및 `mix` 항목을 사용하는 크레이트의 예를 보여줍니다:

```rust
use art::kinds::PrimaryColor;
use art::utils::mix;

fn main() {
    let red = PrimaryColor::Red;
    let yellow = PrimaryColor::Yellow;
    mix(red, yellow);
}
```

*Listing 14-4: 내부 구조가 내보낸 `art` 크레이트의 항목을 사용하는 크레이트*

Listing 14-4의 코드 작성자, `art` 크레이트를 사용하는 사람은 `PrimaryColor`가 `kinds` 모듈에 있고 `mix`가 `utils` 모듈에 있다는 것을 알아내야 했습니다. `art` 크레이트의 모듈 구조는 `art` 크레이트를 사용하는 개발자보다 `art` 크레이트를 작업하는 개발자에게 더 관련이 있습니다. 크레이트의 부분을 `kinds` 모듈과 `utils` 모듈로 구성하는 내부 구조는 `art` 크레이트 사용 방법을 이해하려는 사람에게 유용한 정보를 포함하지 않습니다. 대신 `art` 크레이트의 모듈 구조는 혼란을 야기합니다. 사용자가 어디를 봐야 할지 알아내야 하고 `use` 문에서 모듈 이름을 지정해야 하기 때문입니다.

공개 API에서 내부 구성을 제거하려면 Listing 14-3의 `art` 크레이트 코드를 수정하여 `pub use` 문을 추가하여 최상위 레벨에서 항목을 재내보낼 수 있습니다. Listing 14-5에 나와 있습니다:

```rust
//! # Art
//!
//! 예술적 개념을 모델링하기 위한 라이브러리.

pub use self::kinds::PrimaryColor;
pub use self::kinds::SecondaryColor;
pub use self::utils::mix;

pub mod kinds {
    // --snip--
}

pub mod utils {
    // --snip--
}
```

*Listing 14-5: 항목을 재내보내기 위해 `pub use` 문 추가*

`cargo doc`이 이 크레이트에 대해 생성하는 API 문서는 이제 첫 페이지에 재내보내기를 나열하고 링크하여 `PrimaryColor` 및 `SecondaryColor` 타입과 `mix` 함수를 더 쉽게 찾을 수 있습니다.

`art` 크레이트 사용자는 Listing 14-4에 표시된 대로 Listing 14-3의 내부 구조를 보고 사용할 수 있거나 Listing 14-6에 표시된 대로 Listing 14-5의 더 편리한 구조를 사용할 수 있습니다:

```rust
use art::mix;
use art::PrimaryColor;

fn main() {
    // --snip--
}
```

*Listing 14-6: `art` 크레이트의 재내보낸 항목을 사용하는 프로그램*

중첩된 모듈이 많은 경우 `pub use`로 최상위 레벨에서 타입을 재내보내면 크레이트를 사용하는 사람들의 경험에 큰 차이를 만들 수 있습니다. `pub use`의 또 다른 일반적인 용도는 현재 크레이트의 의존성 정의를 재내보내 해당 크레이트의 정의를 크레이트의 공개 API의 일부로 만드는 것입니다.

유용한 공개 API 구조를 만드는 것은 과학보다 예술에 가깝고 반복하여 사용자에게 가장 적합한 API를 찾을 수 있습니다. `pub use`를 선택하면 크레이트를 내부적으로 구조화하는 방법에 유연성을 제공하고 해당 내부 구조를 사용자에게 제시하는 것에서 분리합니다. 설치한 일부 크레이트의 코드를 살펴보고 내부 구조가 공개 API와 다른지 확인하세요.

## Crates.io 계정 설정하기

패키지를 게시하려면 먼저 [crates.io](https://crates.io/)에서 계정을 만들고 API 토큰을 받아야 합니다. 그렇게 하려면 [crates.io](https://crates.io/)의 홈페이지를 방문하여 GitHub 계정을 통해 로그인하세요. (현재 GitHub 계정이 필요하지만 사이트에서 향후 다른 계정 생성 방법을 지원할 수 있습니다.) 로그인한 후 [https://crates.io/me/](https://crates.io/me/)의 계정 설정을 방문하여 API 키를 검색하세요. 그런 다음 API 키로 `cargo login` 명령을 실행합니다:

```console
$ cargo login abcdefghijklmnopqrstuvwxyz012345
```

이 명령은 Cargo에 API 토큰을 알리고 로컬에 *~/.cargo/credentials.toml*에 저장합니다. 이 토큰은 *비밀*입니다: 다른 사람과 공유하지 마세요. 어떤 이유로든 다른 사람과 공유한 경우 철회하고 [crates.io](https://crates.io/)에서 새 토큰을 생성해야 합니다.

## 새 크레이트에 메타데이터 추가하기

계정이 있다고 가정하고 게시하려는 크레이트가 있다고 합시다. 게시하기 전에 *Cargo.toml* 파일의 `[package]` 섹션에 추가하여 크레이트에 일부 메타데이터를 추가해야 합니다.

크레이트는 고유한 이름이 필요합니다. 로컬에서 크레이트를 작업하는 동안 크레이트 이름을 원하는 대로 지정할 수 있습니다. 그러나 [crates.io](https://crates.io/)의 크레이트 이름은 선착순으로 할당됩니다. 크레이트 이름이 사용되면 다른 사람은 해당 이름으로 크레이트를 게시할 수 없습니다. 사이트에서 사용하려는 이름을 검색하여 사용되었는지 확인하세요. 그렇지 않은 경우 *Cargo.toml*의 `[package]` 아래의 이름을 편집하여 게시에 사용합니다:

```toml
[package]
name = "guessing_game"
```

고유한 이름을 선택했더라도 이 시점에서 `cargo publish`를 실행하여 크레이트를 게시하면 경고와 오류가 표시됩니다:

```console
$ cargo publish
    Updating crates.io index
warning: manifest has no description, license, license-file, documentation, homepage or repository.
See https://doc.rust-lang.org/cargo/reference/manifest.html#package-metadata for more info.
--snip--
error: failed to publish to registry at https://crates.io

Caused by:
  the remote server responded with an error (status 400 Bad Request): missing or empty metadata fields: description, license. Please see https://doc.rust-lang.org/cargo/reference/manifest.html for more information on configuring these fields
```

이 오류는 일부 중요한 정보가 누락되었기 때문입니다: 설명과 라이선스가 필요합니다. 이는 사람들이 크레이트가 무엇을 하고 어떤 조건으로 사용할 수 있는지 알 수 있도록 하기 위해서입니다. *Cargo.toml*에서 크레이트를 검색 결과에 표시할 한두 문장의 설명을 추가합니다. `license` 필드의 경우 *라이선스 식별자 값*을 제공해야 합니다. [Linux Foundation의 소프트웨어 패키지 데이터 교환(SPDX)][spdx]는 이 값에 사용할 수 있는 식별자를 나열합니다. 예를 들어 MIT 라이선스로 크레이트에 라이선스를 부여했음을 지정하려면 `MIT` 식별자를 추가합니다:

```toml
[package]
name = "guessing_game"
license = "MIT"
```

SPDX에 나타나지 않는 라이선스를 사용하려면 해당 라이선스의 텍스트를 파일에 넣고 프로젝트에 파일을 포함한 다음 `license` 키 대신 `license-file`을 사용하여 해당 파일의 이름을 지정해야 합니다.

어떤 라이선스가 프로젝트에 적합한지에 대한 지침은 이 책의 범위를 벗어납니다. Rust 커뮤니티의 많은 사람들은 `MIT OR Apache-2.0`의 이중 라이선스를 사용하여 Rust와 동일한 방식으로 프로젝트에 라이선스를 부여합니다. 이 관행은 `OR`로 구분된 여러 라이선스 식별자를 지정하여 프로젝트에 여러 라이선스를 가질 수 있음을 보여줍니다.

고유한 이름, 버전, 설명, 라이선스가 추가되면 게시할 준비가 된 프로젝트의 *Cargo.toml* 파일은 다음과 같을 수 있습니다:

```toml
[package]
name = "guessing_game"
version = "0.1.0"
edition = "2024"
description = "컴퓨터가 선택한 숫자를 추측하는 재미있는 게임."
license = "MIT OR Apache-2.0"

[dependencies]
```

[Cargo 문서](https://doc.rust-lang.org/cargo/)는 다른 사람들이 크레이트를 더 쉽게 검색하고 사용할 수 있도록 지정할 수 있는 다른 메타데이터를 설명합니다.

## Crates.io에 게시하기

계정을 만들고, API 토큰을 저장하고, 크레이트 이름을 선택하고, 필요한 메타데이터를 지정했으므로 이제 게시할 준비가 되었습니다! 크레이트를 게시하면 다른 사람들이 사용할 수 있도록 [crates.io](https://crates.io/)에 특정 버전이 업로드됩니다.

게시는 *영구적*이므로 주의하세요. 버전은 절대 덮어쓸 수 없으며 코드를 삭제할 수 없습니다. [crates.io](https://crates.io/)의 주요 목표 중 하나는 코드의 영구 아카이브 역할을 하여 [crates.io](https://crates.io/)의 크레이트에 의존하는 모든 프로젝트의 빌드가 계속 작동하도록 하는 것입니다. 버전 삭제를 허용하면 해당 목표를 달성하는 것이 불가능해집니다. 그러나 게시할 수 있는 크레이트 버전 수에는 제한이 없습니다.

`cargo publish` 명령을 다시 실행하세요. 이제 성공해야 합니다:

```console
$ cargo publish
    Updating crates.io index
   Packaging guessing_game v0.1.0 (file:///projects/guessing_game)
   Verifying guessing_game v0.1.0 (file:///projects/guessing_game)
   Compiling guessing_game v0.1.0
(file:///projects/guessing_game/target/package/guessing_game-0.1.0)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.19s
   Uploading guessing_game v0.1.0 (file:///projects/guessing_game)
    Uploaded guessing_game v0.1.0 to registry `crates-io`
note: waiting for `guessing_game v0.1.0` to be available at registry `crates-io`.
You may press ctrl-c to skip waiting; the crate should be available shortly.
   Published guessing_game v0.1.0 at registry `crates-io`
```

축하합니다! 이제 Rust 커뮤니티와 코드를 공유했으며 누구나 크레이트를 프로젝트의 의존성으로 쉽게 추가할 수 있습니다.

## 기존 크레이트의 새 버전 게시하기

크레이트를 변경하고 새 버전을 릴리스할 준비가 되면 *Cargo.toml*에 지정된 `version` 값을 변경하고 다시 게시합니다. [시맨틱 버전 관리 규칙][semver]을 사용하여 변경 유형에 따라 적절한 다음 버전 번호를 결정하세요. 그런 다음 `cargo publish`를 실행하여 새 버전을 업로드합니다.

## `cargo yank`로 Crates.io에서 버전 폐기하기

이전 버전의 크레이트를 제거할 수는 없지만, 향후 프로젝트가 해당 버전을 새 의존성으로 추가하는 것을 방지할 수 있습니다. 이는 크레이트 버전이 어떤 이유로 손상된 경우에 유용합니다. 이러한 상황에서 Cargo는 버전 *얀크*(폐기)를 지원합니다.

버전을 얀크하면 새 프로젝트가 해당 버전에 의존하는 것을 방지하면서 해당 버전에 의존하는 모든 기존 프로젝트가 계속 작동하도록 합니다. 본질적으로 얀크는 *Cargo.lock*이 있는 모든 프로젝트가 중단되지 않고 향후 생성되는 *Cargo.lock* 파일이 얀크된 버전을 사용하지 않음을 의미합니다.

버전을 얀크하려면 이전에 게시한 크레이트의 디렉토리에서 `cargo yank`를 실행하고 얀크하려는 버전을 지정합니다. 예를 들어 `guessing_game`이라는 크레이트의 버전 1.0.1을 게시했고 얀크하려는 경우 `guessing_game`의 프로젝트 디렉토리에서 다음을 실행합니다:

```console
$ cargo yank --vers 1.0.1
    Updating crates.io index
        Yank guessing_game@1.0.1
```

명령에 `--undo`를 추가하면 얀크를 취소하고 프로젝트가 버전에 다시 의존할 수 있도록 허용할 수도 있습니다:

```console
$ cargo yank --vers 1.0.1 --undo
    Updating crates.io index
      Unyank guessing_game@1.0.1
```

얀크는 코드를 삭제하지 *않습니다*. 예를 들어, 실수로 업로드된 비밀을 삭제할 수 없습니다. 그런 일이 발생하면 해당 비밀을 즉시 재설정해야 합니다.

[spdx]: https://spdx.org/licenses/
[semver]: https://semver.org/
