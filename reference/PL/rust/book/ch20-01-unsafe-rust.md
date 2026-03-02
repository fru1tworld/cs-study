# 안전하지 않은 Rust (Unsafe Rust)

## 개요

Unsafe Rust는 Rust 내부에 숨겨진 두 번째 언어로, 컴파일 타임에 메모리 안전성 보장을 강제하지 않습니다. 이것이 존재하는 이유:

1. **정적 분석은 보수적임** - 컴파일러는 유효하지 않은 프로그램을 수용하지 않기 위해 일부 유효한 프로그램도 거부함
2. **하드웨어는 본질적으로 안전하지 않음** - 저수준 시스템 프로그래밍에는 Rust의 안전성 규칙이 방지하는 작업이 필요함

## 다섯 가지 Unsafe 슈퍼파워

`unsafe` 키워드를 사용하면 안전한 Rust에서 허용되지 않는 다섯 가지 작업을 수행할 수 있습니다:

1. 원시 포인터(raw pointer) 역참조
2. 안전하지 않은 함수 또는 메서드 호출
3. 가변 정적 변수 접근 또는 수정
4. 안전하지 않은 트레이트 구현
5. 유니온(union)의 필드 접근

**중요**: `unsafe`는 빌림 검사기나 다른 안전성 검사를 비활성화하지 않습니다. 메모리 안전성 검증 없이 이 다섯 가지 기능에만 접근을 허용합니다.

---

## 1. 원시 포인터 역참조

### 원시 포인터 생성

원시 포인터(`*const T`와 `*mut T`)는 참조와 유사하지만 빌림 규칙을 우회할 수 있습니다:

```rust
fn main() {
    let mut num = 5;

    let r1 = &raw const num;  // *const i32
    let r2 = &raw mut num;    // *mut i32
}
```

원시 포인터는 안전한 코드에서 생성할 수 있지만, 역참조는 `unsafe`가 필요합니다:

```rust
fn main() {
    let mut num = 5;

    let r1 = &raw const num;
    let r2 = &raw mut num;

    unsafe {
        println!("r1 is: {}", *r1);
        println!("r2 is: {}", *r2);
    }
}
```

### 참조와의 주요 차이점

**원시 포인터의 특징:**
- 빌림 규칙을 무시할 수 있음 (같은 위치에 불변과 가변 포인터 모두 가능)
- 유효한 메모리를 가리킨다는 보장이 없음
- null일 수 있음
- 자동 정리(cleanup)가 구현되어 있지 않음

### 임의의 메모리에서 생성

```rust
fn main() {
    let address = 0x012345usize;
    let r = address as *const i32;  // 위험: 정의되지 않은 동작
}
```

---

## 2. 안전하지 않은 함수 또는 메서드 호출

안전하지 않은 함수는 호출하기 위해 `unsafe` 블록이 필요합니다:

```rust
fn main() {
    unsafe fn dangerous() {}

    unsafe {
        dangerous();
    }
}
```

블록 없이 호출하면 컴파일러 오류가 발생합니다:
```
error[E0133]: call to unsafe function `dangerous` is unsafe and requires unsafe block
```

### 안전하지 않은 코드 위에 안전한 추상화 만들기

unsafe 코드를 안전한 함수로 감싸서 `unsafe`가 코드베이스 전체로 퍼지는 것을 방지합니다:

```rust
use std::slice;

fn split_at_mut(values: &mut [i32], mid: usize) -> (&mut [i32], &mut [i32]) {
    let len = values.len();
    let ptr = values.as_mut_ptr();

    assert!(mid <= len);

    unsafe {
        (
            slice::from_raw_parts_mut(ptr, mid),
            slice::from_raw_parts_mut(ptr.add(mid), len - mid),
        )
    }
}

fn main() {
    let mut vector = vec![1, 2, 3, 4, 5, 6];
    let (left, right) = split_at_mut(&mut vector, 3);
    assert_eq!(left, &mut [1, 2, 3]);
    assert_eq!(right, &mut [4, 5, 6]);
}
```

**핵심 포인트:** 함수 자체는 검증된 전제 조건으로 안전한 인터페이스를 제공하므로 `unsafe`로 표시할 필요가 없습니다.

### `extern` 함수를 사용하여 외부 코드 호출

외부 함수 인터페이스(FFI)를 통해 C 함수를 호출합니다:

```rust
unsafe extern "C" {
    fn abs(input: i32) -> i32;
}

fn main() {
    unsafe {
        println!("Absolute value of -3 according to C: {}", abs(-3));
    }
}
```

#### 함수를 안전하게 표시하기

외부 함수가 증명 가능하게 안전하다면 `safe` 키워드를 사용합니다:

```rust
unsafe extern "C" {
    safe fn abs(input: i32) -> i32;
}

fn main() {
    println!("Absolute value of -3 according to C: {}", abs(-3));
}
```

**주의:** 함수를 `safe`로 표시하는 것은 Rust에게 하는 약속입니다. 자동으로 안전하게 만들어주지 않습니다.

### 다른 언어에서 Rust 함수 호출

이름 맹글링(name mangling)을 비활성화하려면 `#[unsafe(no_mangle)]`을 사용합니다:

```rust
#[unsafe(no_mangle)]
pub extern "C" fn call_from_c() {
    println!("Just called a Rust function from C!");
}
```

---

## 3. 가변 정적 변수 접근 또는 수정

### 불변 정적 변수

접근이 안전합니다:

```rust
static HELLO_WORLD: &str = "Hello, world!";

fn main() {
    println!("value is: {HELLO_WORLD}");
}
```

### 가변 정적 변수

접근과 수정에 `unsafe`가 필요합니다:

```rust
static mut COUNTER: u32 = 0;

/// SAFETY: 한 번에 하나의 스레드에서만 호출해야 합니다.
/// 여러 스레드에서 동시에 호출하면 정의되지 않은 동작이 발생합니다.
unsafe fn add_to_count(inc: u32) {
    unsafe {
        COUNTER += inc;
    }
}

fn main() {
    unsafe {
        // SAFETY: 이것은 `main`의 단일 스레드에서만 호출됩니다.
        add_to_count(3);
        println!("COUNTER: {}", *(&raw const COUNTER));
    }
}
```

**모범 사례:** 가능하면 16장의 동시성 기법을 대신 사용하세요.

---

## 4. 안전하지 않은 트레이트 구현

안전하지 않은 트레이트를 선언하고 구현합니다:

```rust
unsafe trait Foo {
    // 메서드가 여기에 옵니다
}

unsafe impl Foo for i32 {
    // 메서드 구현이 여기에 옵니다
}
```

### 예시: Send와 Sync

원시 포인터를 포함하는 타입에 대해 `Send` 또는 `Sync`를 수동으로 구현합니다:

```rust
unsafe impl Send for MyType {}
unsafe impl Sync for MyType {}
```

---

## 5. 유니온의 필드 접근

유니온 필드에 접근합니다 (주로 C 상호 운용을 위해):

```rust
union MyUnion {
    i: i32,
    f: f32,
}
```

이것은 Rust가 저장된 데이터의 타입을 보장할 수 없기 때문에 안전하지 않습니다. 자세한 내용은 [Rust 레퍼런스](https://doc.rust-lang.org/reference/items/unions.html)를 참조하세요.

---

## Miri로 Unsafe 코드 검사하기

Miri는 코드를 동적으로 실행하여 정의되지 않은 동작을 감지하는 공식 도구입니다:

```bash
# Miri 설치
rustup +nightly component add miri

# 프로젝트에서 실행
cargo +nightly miri run
cargo +nightly miri test
```

### 예시

문제가 있는 코드에 Miri를 실행:

```rust
fn main() {
    use std::slice;
    let address = 0x01234usize;
    let r = address as *mut i32;
    let values: &[i32] = unsafe { slice::from_raw_parts_mut(r, 10000) };
}
```

Miri 출력:
```
error: Undefined Behavior: pointer not dereferenceable: pointer must be dereferenceable
for 40000 bytes, but got 0x1234[noalloc] which is a dangling pointer
```

**참고:** Miri는 동적입니다. 실행된 코드의 버그를 잡지만 모든 문제를 찾는 것을 보장하지는 않습니다.

---

## 모범 사례

**핵심 포인트:**
- **unsafe 블록을 작게 유지** - 잠재적 문제의 범위를 최소화
- **SAFETY 주석 작성** - 코드가 안전한 이유를 설명:
  ```rust
  // SAFETY: 이 포인터는 ... 때문에 유효함이 보장됩니다.
  unsafe { /* 코드 */ }
  ```
- **Miri 사용** - unsafe 코드를 검증
- **안전한 추상화 선호** - unsafe 코드를 안전한 API로 감싸기
- **요구 사항 문서화** - 호출자가 보장해야 할 것을 명확히 명시

---

## 추가 자료

- [The Rustonomicon](https://doc.rust-lang.org/nomicon/) - Unsafe Rust 공식 가이드
- [Miri GitHub 저장소](https://github.com/rust-lang/miri)
- [Rust Reference - FFI](https://doc.rust-lang.org/reference/items/external-blocks.html)
