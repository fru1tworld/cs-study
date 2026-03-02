# `panic!`으로 복구 불가능한 에러 처리하기

## 개요

코드에서 처리할 수 없는 나쁜 일이 발생할 때가 있습니다. 이런 경우 Rust에는 `panic!` 매크로가 있습니다. 패닉을 발생시키는 방법은 두 가지가 있습니다:

1. 코드가 패닉을 발생시키는 동작 수행 (예: 배열 끝을 넘어 접근)
2. `panic!` 매크로를 명시적으로 호출

기본적으로 패닉이 발생하면:
- 실패 메시지 출력
- 스택을 되감고(unwind) 정리
- 프로그램 종료

`RUST_BACKTRACE` 환경 변수를 설정하여 패닉 발생 시 호출 스택을 표시할 수 있습니다.

## 스택 되감기(Unwinding) vs 중단(Abort)

### 기본 동작: 되감기(Unwinding)

패닉이 발생하면 프로그램은 **되감기(unwinding)**를 시작합니다. 이는 Rust가 스택을 거슬러 올라가며 만나는 각 함수의 데이터를 정리하는 것을 의미합니다.

### 대안적 동작: 중단(Abort)

스택을 거슬러 올라가며 정리하는 것은 작업이 많이 필요합니다. Rust는 정리 없이 즉시 **중단(abort)**하여 프로그램을 종료하는 것을 선택할 수 있게 합니다.

메모리는 대신 운영 체제가 정리합니다.

### 설정 방법

결과 바이너리를 최대한 작게 만들려면, *Cargo.toml*의 `[profile]` 섹션에 `panic = 'abort'`를 추가하여 되감기에서 중단으로 전환할 수 있습니다:

```toml
[profile.release]
panic = 'abort'
```

**핵심 포인트:**
- 되감기(Unwinding): 스택을 거슬러 올라가며 데이터 정리 (기본값)
- 중단(Abort): 정리 없이 즉시 종료, OS가 메모리 정리
- 바이너리 크기를 줄이려면 `panic = 'abort'` 사용

## 예제 1: 간단한 패닉

**파일명: src/main.rs**

```rust
fn main() {
    panic!("crash and burn");
}
```

**출력:**

```
$ cargo run
   Compiling panic v0.1.0 (file:///projects/panic)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.25s
     Running `target/debug/panic`

thread 'main' panicked at src/main.rs:2:5:
crash and burn
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

**에러 메시지 구성:**
- 패닉 메시지: "crash and burn"
- 위치: *src/main.rs:2:5* (2번째 줄, 5번째 문자)

## 예제 2: 잘못된 벡터 접근으로 인한 패닉

**파일명: src/main.rs**

```rust
fn main() {
    let v = vec![1, 2, 3];

    v[99];
}
```

이 코드는 3개의 요소만 있는 벡터에서 100번째 요소(인덱스 99)에 접근하려 하여 패닉을 발생시킵니다.

**출력:**

```
$ cargo run
   Compiling panic v0.1.0 (file:///projects/panic)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.27s
     Running `target/debug/panic`

thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

### Rust가 패닉을 발생시키는 이유

C 언어에서는 데이터 구조의 끝을 넘어 읽는 것이 정의되지 않은 동작(undefined behavior)입니다. 해당 메모리 위치에 있는 값을 얻을 수 있으며, 이를 **버퍼 오버리드(buffer overread)**라고 합니다. 이는 보안 취약점으로 이어질 수 있습니다.

**Rust는 이를 보호합니다:** 잘못된 인덱스로 읽으려 하면 실행을 멈추고 계속 진행하지 않습니다.

**핵심 포인트:**
- C: 범위 밖 접근 시 정의되지 않은 동작 (보안 취약점 가능)
- Rust: 범위 밖 접근 시 즉시 패닉 (안전성 보장)
- 버퍼 오버리드 공격 방지

## 백트레이스(Backtrace) 사용하기

백트레이스를 얻으려면 `RUST_BACKTRACE` 환경 변수를 설정합니다:

```bash
$ RUST_BACKTRACE=1 cargo run
```

**백트레이스 출력 예시:**

```
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
stack backtrace:
   0: rust_begin_unwind
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/std/src/panicking.rs:692:5
   1: core::panicking::panic_fmt
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/core/src/panicking.rs:75:14
   2: core::panicking::panic_bounds_check
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/core/src/panicking.rs:273:5
   3: <usize as core::slice::index::SliceIndex<[T]>>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/slice/index.rs:274:10
   4: core::slice::index::<impl core::ops::index::Index<I> for [T]>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/slice/index.rs:16:9
   5: <alloc::vec::Vec<T,A> as core::ops::index::Index<I>>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/alloc/src/vec/mod.rs:3361:9
   6: panic::main
             at ./src/main.rs:4:6
   7: core::ops::function::FnOnce::call_once
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/ops/function.rs:250:5

note: Some details are omitted, run with `RUST_BACKTRACE=full` for a verbose backtrace.
```

### 백트레이스 읽는 방법

1. **위에서부터 시작**하여 작성한 파일이 나올 때까지 읽습니다
2. 그곳이 문제가 발생한 지점입니다
3. 위의 줄들 = 내 코드가 호출한 코드
4. 아래의 줄들 = 내 코드를 호출한 코드

위 예시에서 **6번 줄**이 사용자 코드의 실제 문제 지점을 가리킵니다: *src/main.rs:4:6*

**핵심 포인트:**
- 백트레이스는 문제 발생 시점까지의 함수 호출 경로를 보여줌
- 직접 작성한 코드 파일을 찾아서 문제 지점 파악
- `RUST_BACKTRACE=full`로 더 자세한 정보 확인 가능

## 디버깅 팁

- `cargo build` 또는 `cargo run`(--release 없이)을 사용하면 디버그 심볼이 기본으로 활성화됨
- 더 자세한 출력은 `RUST_BACKTRACE=full` 사용
- 코드가 패닉을 발생시키면, 어떤 동작과 값이 패닉을 유발했는지, 코드가 대신 무엇을 해야 하는지 파악해야 함

**핵심 포인트:**
- 디버그 빌드에서 디버그 심볼 자동 활성화
- 패닉 발생 시: 원인 파악 -> 올바른 동작 결정 -> 코드 수정
- 릴리스 빌드(`--release`)에서는 디버그 정보가 제거됨

## 요약

| 항목 | 설명 |
|------|------|
| `panic!` 매크로 | 복구 불가능한 에러 발생 시 프로그램 종료 |
| 되감기(Unwinding) | 스택을 거슬러 올라가며 정리 (기본값) |
| 중단(Abort) | 정리 없이 즉시 종료, 바이너리 크기 감소 |
| 백트레이스 | `RUST_BACKTRACE=1`로 호출 스택 확인 |
| 안전성 | 잘못된 메모리 접근 시 패닉으로 보안 보장 |
