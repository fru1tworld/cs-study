# Cargo와 Crates.io 더 알아보기

# Cargo와 Crates.io에 대해 더 알아보기

> **원문:** https://doc.rust-lang.org/book/ch14-00-more-about-cargo.html

지금까지 우리는 Cargo의 가장 기본적인 기능만 사용하여 코드를 빌드, 실행, 테스트했지만, Cargo는 훨씬 더 많은 것을 할 수 있습니다. 이 장에서는 다음과 같은 작업을 수행하는 방법을 보여주는 더 고급 기능 중 일부를 논의합니다:

* 릴리스 프로필을 통해 빌드 사용자 정의하기
* [crates.io](https://crates.io/)에 라이브러리 게시하기
* 워크스페이스로 대규모 프로젝트 구성하기
* [crates.io](https://crates.io/)에서 바이너리 설치하기
* 사용자 정의 명령을 사용하여 Cargo 확장하기

Cargo는 이 장에서 다루는 것보다 훨씬 더 많은 것을 할 수 있으므로 모든 기능에 대한 전체 설명은 [해당 문서](https://doc.rust-lang.org/cargo/)를 참조하세요.

---

# 릴리스 프로필로 빌드 사용자 정의하기

> **원문:** https://doc.rust-lang.org/book/ch14-01-release-profiles.html

Rust에서 *릴리스 프로필*은 프로그래머가 코드 컴파일을 위한 다양한 옵션을 더 많이 제어할 수 있도록 하는 다양한 구성을 가진 미리 정의된 사용자 정의 가능한 프로필입니다. 각 프로필은 다른 프로필과 독립적으로 구성됩니다.

Cargo에는 두 가지 주요 프로필이 있습니다: `cargo build`를 실행할 때 Cargo가 사용하는 `dev` 프로필과 `cargo build --release`를 실행할 때 Cargo가 사용하는 `release` 프로필입니다. `dev` 프로필은 개발에 적합한 기본값으로 정의되고 `release` 프로필은 릴리스 빌드에 적합한 기본값이 있습니다.

이러한 프로필 이름은 빌드 출력에서 익숙할 수 있습니다:

```console
$ cargo build
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
$ cargo build --release
    Finished `release` profile [optimized] target(s) in 0.32s
```

`dev`와 `release`는 컴파일러가 사용하는 이러한 다른 프로필입니다.

Cargo는 프로젝트의 *Cargo.toml* 파일에 명시적으로 추가된 `[profile.*]` 섹션이 없을 때 적용되는 각 프로필에 대한 기본 설정이 있습니다. 사용자 정의하려는 프로필에 대해 `[profile.*]` 섹션을 추가하면 기본 설정의 모든 하위 집합을 재정의합니다. 예를 들어, `dev` 및 `release` 프로필의 `opt-level` 설정에 대한 기본값은 다음과 같습니다:

```toml
[profile.dev]
opt-level = 0

[profile.release]
opt-level = 3
```

`opt-level` 설정은 Rust가 코드에 적용할 최적화 수를 제어하며, 0에서 3 사이의 범위입니다. 더 많은 최적화를 적용하면 컴파일 시간이 늘어나므로 개발 중이고 자주 코드를 컴파일하는 경우 결과 코드가 느리게 실행되더라도 더 빠른 컴파일을 원합니다. 따라서 `dev`의 기본 `opt-level`은 `0`입니다. 코드를 릴리스할 준비가 되면 컴파일하는 데 더 많은 시간을 투자하는 것이 가장 좋습니다. 릴리스 모드에서는 한 번만 컴파일하지만 컴파일된 프로그램은 여러 번 실행하므로 릴리스 모드는 더 긴 컴파일 시간을 더 빠르게 실행되는 코드와 교환합니다. 그래서 `release` 프로필의 기본 `opt-level`은 `3`입니다.

*Cargo.toml*에 다른 값을 추가하여 기본 설정을 재정의할 수 있습니다. 예를 들어, 개발 프로필에서 최적화 레벨 1을 사용하려면 프로젝트의 *Cargo.toml* 파일에 다음 두 줄을 추가할 수 있습니다:

```toml
[profile.dev]
opt-level = 1
```

이 코드는 기본 설정 `0`을 재정의합니다. 이제 `cargo build`를 실행하면 Cargo는 `dev` 프로필의 기본값과 `opt-level`에 대한 사용자 정의를 사용합니다. `opt-level`을 `1`로 설정했기 때문에 Cargo는 기본값보다 더 많은 최적화를 적용하지만 릴리스 빌드만큼은 많지 않습니다.

각 프로필의 구성 옵션 및 기본값의 전체 목록은 [Cargo 문서](https://doc.rust-lang.org/cargo/reference/profiles.html)를 참조하세요.

---

# Crates.io에 크레이트 게시하기

> **원문:** https://doc.rust-lang.org/book/ch14-02-publishing-to-crates-io.html

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

7장에서 `pub` 키워드를 사용하여 항목을 공개하고 `use` 키워드를 사용하여 항목을 범위에 가져오는 방법을 다뤘습니다. 그러나 크레이트를 개발하는 동안 여러분에게 합리적인 구조가 사용자에게는 그다지 편리하지 않을 수 있습니다. 여러 레벨의 계층 구조로 구조체를 구성하고 싶을 수 있지만, 계층 구조 깊이 정의한 타입을 사용하려는 사람들은 해당 타입이 존재하는지 찾는 데 어려움을 겪을 수 있습니다. 사용자들은 또한 `use my_crate::UsefulType;` 대신 `use my_crate::some_module::another_module::UsefulType;`을 입력해야 하는 것에 짜증을 낼 수 있습니다.

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

`cargo doc`이 생성한 이 크레이트의 문서 첫 페이지는 `kinds`와 `utils` 모듈을 나열하고 `PrimaryColor`와 `SecondaryColor` 타입이나 `mix` 함수를 나열하지 않습니다. 이를 보려면 모듈을 클릭해야 합니다.

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

---

# Cargo 워크스페이스

> **원문:** https://doc.rust-lang.org/book/ch14-03-cargo-workspaces.html

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

워크스페이스에는 컴파일된 아티팩트가 배치될 최상위 레벨에 하나의 *target* 디렉토리가 있습니다; `adder` 패키지에는 자체 *target* 디렉토리가 없습니다. *adder* 디렉토리 내부에서 `cargo build`를 실행하더라도 컴파일된 아티팩트는 *add/adder/target* 대신 *add/target*에 저장됩니다. Cargo는 워크스페이스에서 *target* 디렉토리를 이와 같이 구조화합니다. 워크스페이스의 크레이트가 서로 의존하기 때문입니다. 각 크레이트가 자체 *target* 디렉토리가 있다면 각 크레이트는 자체 *target* 디렉토리에 아티팩트를 배치하기 위해 워크스페이스의 다른 각 크레이트를 다시 컴파일해야 합니다. 하나의 *target* 디렉토리를 공유하면 크레이트가 불필요한 재빌드를 피할 수 있습니다.

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

---

# `cargo install`로 Crates.io에서 바이너리 설치하기

> **원문:** https://doc.rust-lang.org/book/ch14-04-installing-binaries.html

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

---

# 사용자 정의 명령으로 Cargo 확장하기

> **원문:** https://doc.rust-lang.org/book/ch14-05-extending-cargo.html

Cargo는 수정 없이 새 하위 명령으로 확장할 수 있도록 설계되었습니다. `$PATH`에 있는 바이너리 이름이 `cargo-something`이면 `cargo something`을 실행하여 마치 Cargo 하위 명령인 것처럼 실행할 수 있습니다. 이와 같은 사용자 정의 명령은 `cargo --list`를 실행할 때도 나열됩니다. `cargo install`을 사용하여 확장을 설치한 다음 내장 Cargo 도구처럼 실행할 수 있는 것은 Cargo 디자인의 매우 편리한 이점입니다!

## 요약

Cargo와 [crates.io](https://crates.io/)로 코드를 공유하는 것은 Rust 생태계를 다양한 작업에 유용하게 만드는 부분입니다. Rust의 표준 라이브러리는 작고 안정적이지만, 크레이트는 언어와 다른 타임라인으로 공유, 사용 및 개선하기 쉽습니다. [crates.io](https://crates.io/)에서 유용한 코드를 공유하는 것을 부끄러워하지 마세요; 다른 누군가에게도 유용할 가능성이 높습니다!
