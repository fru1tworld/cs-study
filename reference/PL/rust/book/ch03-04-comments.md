# 주석

모든 프로그래머는 코드를 이해하기 쉽게 만들려고 노력하지만, 때로는 추가 설명이 필요합니다. 이러한 경우 프로그래머는 소스 코드에 *주석*을 남깁니다. 컴파일러는 이를 무시하지만 소스 코드를 읽는 사람들은 유용하게 볼 수 있습니다.

### 간단한 주석

다음은 간단한 주석입니다:

```rust
// hello, world
```

### 관용적인 주석 스타일

Rust에서 관용적인 주석 스타일은 두 개의 슬래시로 주석을 시작하며, 주석은 줄 끝까지 계속됩니다. 한 줄을 넘어 확장되는 주석의 경우 각 줄에 `//`를 포함해야 합니다:

```rust
// So we're doing something complicated here, long enough that we need
// multiple lines of comments to do it! Whew! Hopefully, this comment will
// explain what's going on.
```

### 줄 끝 주석

주석은 코드가 포함된 줄의 끝에도 배치할 수 있습니다:

**파일명: src/main.rs**

```rust
fn main() {
    let lucky_number = 7; // I'm feeling lucky today
}
```

### 선호되는 형식

하지만 더 자주 보게 될 형식은 주석이 주석을 달고 있는 코드 위의 별도 줄에 있는 것입니다:

**파일명: src/main.rs**

```rust
fn main() {
    // I'm feeling lucky today
    let lucky_number = 7;
}
```

### 문서 주석

Rust에는 또 다른 종류의 주석인 **문서 주석**이 있습니다. 이것은 14장의 ["Crates.io에 크레이트 게시하기"](ch14-02-publishing-to-crates-io.html) 섹션에서 논의됩니다.
