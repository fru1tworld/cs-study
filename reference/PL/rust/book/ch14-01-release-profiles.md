# 릴리스 프로필로 빌드 사용자 정의하기

Rust에서 *릴리스 프로필*은 프로그래머가 코드 컴파일을 위한 다양한 옵션을 더 많이 제어할 수 있도록 하는 다양한 구성을 가진 미리 정의된 사용자 정의 가능한 프로필입니다. 각 프로필은 다른 프로필과 독립적으로 구성됩니다.

Cargo에는 두 가지 주요 프로필이 있습니다: `cargo build`를 실행할 때 Cargo가 사용하는 `dev` 프로필과 `cargo build --release`를 실행할 때 Cargo가 사용하는 `release` 프로필입니다. `dev` 프로필은 개발에 적합한 기본값으로 정의되고 `release` 프로필은 릴리스 빌드에 적합한 기본값을 가지고 있습니다.

이러한 프로필 이름은 빌드 출력에서 익숙할 수 있습니다:

```console
$ cargo build
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
$ cargo build --release
    Finished `release` profile [optimized] target(s) in 0.32s
```

`dev`와 `release`는 컴파일러가 사용하는 이러한 다른 프로필입니다.

Cargo는 프로젝트의 *Cargo.toml* 파일에 명시적으로 추가된 `[profile.*]` 섹션이 없을 때 적용되는 각 프로필에 대한 기본 설정을 가지고 있습니다. 사용자 정의하려는 프로필에 대해 `[profile.*]` 섹션을 추가하면 기본 설정의 모든 하위 집합을 재정의합니다. 예를 들어, `dev` 및 `release` 프로필의 `opt-level` 설정에 대한 기본값은 다음과 같습니다:

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
