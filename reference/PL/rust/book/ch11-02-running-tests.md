# 테스트 실행 방법 제어하기

## 개요

`cargo test`는 코드를 테스트 모드로 컴파일하고 결과 테스트 바이너리를 실행합니다. 기본적으로 테스트는 **병렬**로 실행되고 **출력을 캡처**하여 결과를 읽기 쉽게 만듭니다. 명령줄 옵션으로 이 동작을 수정할 수 있습니다.

### 명령줄 인수

인수는 두 가지 범주로 구분됩니다:
- **`cargo test`에 대한 인수**: `--` 앞에 나열
- **테스트 바이너리에 대한 인수**: `--` 뒤에 나열

```bash
$ cargo test -- --option
$ cargo test --help              # cargo test 옵션
$ cargo test -- --help           # 테스트 바이너리 옵션
```

**핵심 포인트:**
- `cargo test --help`는 `cargo test`와 함께 사용할 수 있는 옵션을 표시
- `cargo test -- --help`는 구분자 뒤에 사용할 수 있는 옵션을 표시
- `--` 구분자로 두 종류의 인수를 분리

---

## 테스트를 병렬 또는 순차적으로 실행하기

기본적으로 테스트는 **스레드를 사용하여 병렬**로 실행되어 더 빠른 피드백을 제공합니다. 그러나 이를 위해서는 테스트가 독립적이고 상태를 공유하지 않아야 합니다.

### 문제 예시

여러 테스트가 같은 파일을 읽고/쓰면 서로 간섭할 수 있습니다:

```rust
// 각 테스트가 test-output.txt에 쓰기
// 한 테스트가 병렬 실행 중 다른 테스트의 데이터를 덮어쓸 수 있음
```

**핵심 포인트:**
- 테스트가 현재 작업 디렉토리나 환경 변수 같은 공유 환경에 의존하면 문제 발생
- 테스트가 같은 파일에 동시에 쓰면 데이터 충돌 발생
- 테스트끼리 서로 독립적이어야 안전하게 병렬 실행 가능

### 해결책: 단일 스레드 실행

```bash
$ cargo test -- --test-threads=1
```

이렇게 하면 테스트가 순차적으로 실행되어 시간이 더 오래 걸리지만 간섭을 방지합니다.

**핵심 포인트:**
- `--test-threads` 플래그로 사용할 스레드 수 지정
- 값을 `1`로 설정하면 병렬 처리를 사용하지 않음
- 공유 상태가 있는 테스트에서 유용

---

## 함수 출력 표시하기

기본적으로 Rust의 테스트 라이브러리는 통과하는 테스트의 **출력을 캡처**합니다. 실패한 테스트만 출력을 표시합니다.

### 예제 코드 (Listing 11-10)

```rust
fn prints_and_returns_10(a: i32) -> i32 {
    println!("I got the value {a}");
    10
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn this_test_will_pass() {
        let value = prints_and_returns_10(4);
        assert_eq!(value, 10);
    }

    #[test]
    fn this_test_will_fail() {
        let value = prints_and_returns_10(8);
        assert_eq!(value, 5);
    }
}
```

**기본 동작**: 통과하는 테스트의 `println!` 출력은 숨겨집니다.

**핵심 포인트:**
- 테스트가 통과하면 `println!` 출력이 표시되지 않음
- 테스트가 실패하면 표준 출력에 출력된 모든 내용이 표시됨
- 실패 메시지와 함께 실패 원인을 파악하는 데 도움

### 모든 출력 보기

```bash
$ cargo test -- --show-output
```

이 명령은 통과하는 테스트와 실패하는 테스트 모두의 출력을 표시합니다.

**핵심 포인트:**
- `--show-output` 플래그로 성공한 테스트의 출력도 확인 가능
- 디버깅 시 유용함
- 출력은 각 테스트별로 구분되어 표시됨

---

## 이름으로 테스트의 하위 집합 실행하기

### 단일 테스트 실행

```bash
$ cargo test one_hundred
```

`one_hundred`라는 이름의 테스트 함수만 실행합니다.

### 예제 코드 (Listing 11-11)

```rust
pub fn add_two(a: u64) -> u64 {
    a + 2
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn add_two_and_two() {
        let result = add_two(2);
        assert_eq!(result, 4);
    }

    #[test]
    fn add_three_and_two() {
        let result = add_two(3);
        assert_eq!(result, 5);
    }

    #[test]
    fn one_hundred() {
        let result = add_two(100);
        assert_eq!(result, 102);
    }
}
```

**핵심 포인트:**
- 테스트 이름을 `cargo test`에 인수로 전달하여 특정 테스트만 실행
- 이 방법으로는 한 번에 하나의 테스트만 지정 가능
- 나머지 테스트는 "filtered out"으로 표시됨

### 여러 테스트 필터링

```bash
$ cargo test add
```

이름에 "add"가 포함된 모든 테스트를 실행합니다 (예: `add_two_and_two`, `add_three_and_two`). 모듈 이름도 필터와 일치합니다.

**핵심 포인트:**
- 테스트 이름의 일부를 지정하여 해당 값과 일치하는 모든 테스트 실행
- 모듈 이름도 필터에 포함되므로 모듈의 모든 테스트를 실행할 수 있음
- 관련된 테스트 그룹을 함께 실행할 때 유용

---

## 특별히 요청하지 않으면 일부 테스트 무시하기

시간이 많이 걸리는 테스트의 경우 `#[ignore]` 속성을 사용합니다:

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }

    #[test]
    #[ignore]
    fn expensive_test() {
        // 실행하는 데 한 시간이 걸리는 코드
    }
}
```

**핵심 포인트:**
- `#[ignore]` 속성을 추가하면 기본 `cargo test`에서 제외됨
- 시간이 오래 걸리는 테스트에 유용
- 무시된 테스트는 별도 명령으로 실행 가능

### 무시된 테스트만 실행

```bash
$ cargo test -- --ignored
```

### 모든 테스트 실행 (무시된 테스트 포함)

```bash
$ cargo test -- --include-ignored
```

**핵심 포인트:**
- `--ignored` 플래그는 무시된 테스트만 실행
- `--include-ignored` 플래그는 무시 여부와 관계없이 모든 테스트 실행
- CI/CD 환경에서 전체 테스트 실행 시 유용

---

## 주요 명령어 요약

| 명령어 | 용도 |
|--------|------|
| `cargo test` | 모든 테스트를 병렬로 실행 (출력 캡처) |
| `cargo test -- --test-threads=1` | 테스트를 순차적으로 실행 |
| `cargo test -- --show-output` | 통과한 테스트의 출력도 표시 |
| `cargo test <name>` | 특정 이름의 테스트 실행 |
| `cargo test <partial_name>` | 필터와 일치하는 모든 테스트 실행 |
| `cargo test -- --ignored` | 무시된 테스트만 실행 |
| `cargo test -- --include-ignored` | 모든 테스트 실행 (무시된 테스트 포함) |
