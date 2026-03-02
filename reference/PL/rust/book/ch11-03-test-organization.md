# 테스트 조직화

## 개요

Rust에서 테스트는 두 가지 주요 카테고리로 구성됩니다:

- **단위 테스트(Unit Tests)**: 작고 집중된 테스트로, 하나의 모듈을 격리하여 테스트합니다. 비공개 인터페이스도 테스트할 수 있습니다.
- **통합 테스트(Integration Tests)**: 라이브러리의 공개 API만 사용하는 외부 테스트로, 여러 모듈을 함께 테스트할 수 있습니다.

## 단위 테스트

### `tests` 모듈과 `#[cfg(test)]`

단위 테스트는 테스트하려는 코드와 함께 `src` 디렉토리에 배치됩니다. 관례적으로 `#[cfg(test)]` 속성이 붙은 `tests` 모듈을 생성합니다.

`#[cfg(test)]` 어노테이션은 Rust에게 `cargo test` 실행 시에만 테스트 코드를 컴파일하고 실행하도록 지시합니다. `cargo build` 시에는 컴파일되지 않습니다.

**핵심 포인트:**
- 컴파일 시간 절약
- 결과물(artifact) 크기 감소
- 프로덕션 코드와 테스트 코드의 명확한 분리

**예제:**

```rust
pub fn add(left: u64, right: u64) -> u64 {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
}
```

### 비공개 함수 테스트

Rust의 프라이버시 규칙은 비공개 함수를 직접 테스트하는 것을 허용합니다. 자식 모듈은 부모 모듈의 항목에 접근할 수 있습니다.

```rust
pub fn add_two(a: u64) -> u64 {
    internal_adder(a, 2)
}

fn internal_adder(left: u64, right: u64) -> u64 {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn internal() {
        let result = internal_adder(2, 2);
        assert_eq!(result, 4);
    }
}
```

**핵심 포인트:**
- `use super::*`를 통해 부모 모듈의 모든 항목(비공개 포함)에 접근
- 비공개 함수 테스트 여부는 프로그래밍 철학에 따라 다름
- Rust는 비공개 함수 테스트를 기술적으로 허용

## 통합 테스트

### `tests` 디렉토리

통합 테스트는 프로젝트 루트 레벨의 `tests` 디렉토리에 배치됩니다(`src` 옆). `tests` 디렉토리의 각 파일은 별도의 크레이트로 컴파일됩니다.

**프로젝트 구조:**

```
adder
├── Cargo.lock
├── Cargo.toml
├── src
│   └── lib.rs
└── tests
    └── integration_test.rs
```

### 통합 테스트 예제

```rust
use adder::add_two;

#[test]
fn it_adds_two() {
    let result = add_two(2);
    assert_eq!(result, 4);
}
```

**단위 테스트와의 주요 차이점:**
- `#[cfg(test)]` 어노테이션이 필요 없음 (Cargo가 `tests` 디렉토리를 특별히 처리)
- 라이브러리의 공개 API를 가져오기 위해 `use` 문 필수
- 비공개 함수에 접근 불가

### 통합 테스트 실행

모든 테스트 실행:
```bash
$ cargo test
```

특정 통합 테스트 파일 실행:
```bash
$ cargo test --test integration_test
```

## 통합 테스트의 서브모듈

### 헬퍼 모듈 문제

`tests` 디렉토리의 각 파일은 별도의 크레이트로 컴파일됩니다. `tests/common.rs` 파일을 만들면 테스트 출력에 `running 0 tests`로 표시되는데, 이는 헬퍼 함수에는 바람직하지 않습니다.

### 해결책: `tests/common/mod.rs` 사용

헬퍼 모듈이 테스트 출력에 나타나지 않도록 하려면 이전 명명 규칙을 사용합니다:

```
tests
├── common
│   └── mod.rs
└── integration_test.rs
```

`tests`의 하위 디렉토리는 별도의 크레이트로 컴파일되지 않습니다.

**`tests/common/mod.rs` 예제:**

```rust
pub fn setup() {
    // 라이브러리 테스트에 필요한 설정 코드
}
```

**`tests/integration_test.rs`에서 사용:**

```rust
use adder::add_two;

mod common;

#[test]
fn it_adds_two() {
    common::setup();

    let result = add_two(2);
    assert_eq!(result, 4);
}
```

**핵심 포인트:**
- `tests/common.rs` 대신 `tests/common/mod.rs` 사용
- 하위 디렉토리는 별도 크레이트로 취급되지 않음
- 여러 통합 테스트 파일에서 공유 설정 코드 재사용 가능

## 바이너리 크레이트의 통합 테스트

바이너리 크레이트(`src/lib.rs` 없이 `src/main.rs`만 포함)는 통합 테스트를 가질 수 없습니다. 통합 테스트는 가져올 라이브러리 크레이트가 필요하기 때문입니다.

**모범 사례:**
- `src/lib.rs`: 테스트 가능한 재사용 로직 포함
- `src/main.rs`: 라이브러리의 로직 호출 (최소한의 코드, 테스트 불필요)

이 구조를 통해 통합 테스트가 중요한 기능을 검증하면서 `main.rs`는 단순하게 유지할 수 있습니다.

## 테스트 출력

`cargo test` 실행 시 세 가지 섹션이 출력됩니다:

1. **단위 테스트** - `src/lib.rs`에서
2. **통합 테스트** - `tests/` 디렉토리에서
3. **문서 테스트** - 문서화 예제에서

어떤 섹션이 실패하면 이후 섹션은 실행되지 않습니다.

## 요약

Rust는 포괄적인 테스트 기능을 제공합니다:

- **단위 테스트**: 개별 컴포넌트와 비공개 세부사항 검증
- **통합 테스트**: 공개 API 동작과 컴포넌트 상호작용 검증
- Rust의 타입 시스템 및 소유권 규칙과 결합하여 로직 버그를 잡고 코드 변경 시 예상 동작 보장

**핵심 포인트:**
- `#[cfg(test)]`로 테스트 코드를 프로덕션에서 제외
- `tests/` 디렉토리로 통합 테스트 분리
- 헬퍼 함수는 `tests/common/mod.rs` 패턴 사용
- 바이너리 크레이트는 로직을 라이브러리로 분리하여 테스트 가능하게 구성
