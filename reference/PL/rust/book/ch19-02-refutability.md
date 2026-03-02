# 반박 가능성: 패턴이 매칭에 실패할 수 있는지 여부

## 개요

Rust의 패턴은 두 가지 형태로 나뉩니다:

1. **반박 불가능한 패턴(Irrefutable Patterns)**: **모든 가능한 값**에 대해 매칭되는 패턴입니다. 매칭에 실패할 수 없습니다.
   - 예: `let x = 5;`에서 `x`는 모든 것과 매칭됨

2. **반박 가능한 패턴(Refutable Patterns)**: 일부 가능한 값에 대해 **매칭에 실패할 수 있는** 패턴입니다.
   - 예: `if let Some(x) = a_value`에서 `Some(x)`는 값이 `None`이면 실패함

## 패턴 사용 규칙

**핵심 포인트:**
- **반박 불가능한 패턴만 허용**: `let` 문, 함수 매개변수, `for` 루프
- **반박 가능하거나 반박 불가능한 패턴 모두 허용**: `if let`, `while let`, `let...else` 표현식
- 컴파일러는 `if let`/`while let`/`let...else`에서 반박 불가능한 패턴을 사용하면 경고를 발생시킴 (이들은 실패를 처리하도록 설계되었기 때문)

## 예제: `let`에서 반박 가능한 패턴 사용 시 오류

```rust
fn main() {
    let some_option_value: Option<i32> = None;
    let Some(x) = some_option_value;  // ERROR
}
```

**컴파일러 오류:**
```
error[E0005]: refutable pattern in local binding
  |
3 |     let Some(x) = some_option_value;
  |         ^^^^^^^ pattern `None` not covered
  |
  = note: `let` bindings require an "irrefutable pattern"
```

**핵심 포인트:**
- `let` 바인딩은 반박 불가능한 패턴을 요구함
- `Some(x)` 패턴은 `None` 케이스를 처리하지 않으므로 반박 가능함
- 컴파일러는 커버되지 않은 패턴(`None`)을 알려줌

## 해결책: `let...else` 사용

```rust
fn main() {
    let some_option_value: Option<i32> = None;
    let Some(x) = some_option_value else {
        return;
    };
}
```

**핵심 포인트:**
- `let...else`는 반박 가능한 패턴을 허용함
- `else` 블록은 패턴이 매칭되지 않을 때 실행됨
- `else` 블록은 반드시 분기를 벗어나야 함 (`return`, `break`, `panic!` 등)

## 경고: `let...else`에서 반박 불가능한 패턴 사용

```rust
fn main() {
    let x = 5 else {
        return;
    };
}
```

**컴파일러 경고:**
```
warning: irrefutable `let...else` pattern
  |
2 |     let x = 5 else {
  |     ^^^^^^^^^
  |
  = note: this pattern will always match, so the `else` clause is useless
```

**핵심 포인트:**
- 반박 불가능한 패턴은 항상 매칭되므로 `else` 절이 무의미함
- 이런 경우 일반 `let` 문을 사용해야 함

## `match` 팔 규칙

**핵심 포인트:**
- `match` 팔은 **반박 가능한 패턴**을 사용해야 함
- 단, **마지막 팔**은 남은 모든 값을 매칭하기 위해 반박 불가능한 패턴을 사용해야 함
- 이렇게 해야 모든 가능한 값이 처리됨을 보장함

## 요약

| 구문 | 허용되는 패턴 유형 |
|------|-------------------|
| `let` 문 | 반박 불가능한 패턴만 |
| 함수 매개변수 | 반박 불가능한 패턴만 |
| `for` 루프 | 반박 불가능한 패턴만 |
| `if let` | 반박 가능한 패턴 (권장) |
| `while let` | 반박 가능한 패턴 (권장) |
| `let...else` | 반박 가능한 패턴 (권장) |
| `match` 팔 | 반박 가능한 패턴 (마지막 팔 제외) |
