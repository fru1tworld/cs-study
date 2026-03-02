# 구조체를 사용한 예제 프로그램

## 개요
이 챕터에서는 직사각형의 면적을 계산하는 프로그램을 작성하면서 구조체를 언제, 어떻게 사용하는지 보여줍니다. 이 섹션에서는 별도의 변수 사용, 튜플 사용, 마지막으로 구조체 사용의 세 가지 접근 방식을 통해 진행합니다.

## 초기 접근 방식: 별도의 변수

**파일명: src/main.rs**

```rust
fn main() {
    let width1 = 30;
    let height1 = 50;

    println!(
        "The area of the rectangle is {} square pixels.",
        area(width1, height1)
    );
}

fn area(width: u32, height: u32) -> u32 {
    width * height
}
```

**Listing 5-8**: 별도의 너비와 높이 변수로 지정된 직사각형의 면적 계산

`cargo run`으로 실행:
```
$ cargo run
   Compiling rectangles v0.1.0 (file:///projects/rectangles)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.42s
     Running `target/debug/rectangles`
The area of the rectangle is 1500 square pixels.
```

**문제점**: `area` 함수 시그니처가 매개변수들이 서로 관련되어 있다는 것을 명확하게 보여주지 않습니다.

---

## 튜플로 리팩토링

**파일명: src/main.rs**

```rust
fn main() {
    let rect1 = (30, 50);

    println!(
        "The area of the rectangle is {} square pixels.",
        area(rect1)
    );
}

fn area(dimensions: (u32, u32)) -> u32 {
    dimensions.0 * dimensions.1
}
```

**Listing 5-9**: 튜플로 직사각형의 너비와 높이 지정

**장점**: 구조를 추가하고 단일 인수를 전달합니다.

**단점**:
- 튜플은 요소에 이름을 붙이지 않습니다
- 튜플 부분에 인덱스로 접근해야 해서(`.0`과 `.1`) 계산이 덜 명확합니다
- 너비와 높이를 혼동하여 오류를 발생시키기 쉽습니다
- 코드를 사용하는 다른 개발자에게 덜 명확합니다

---

## 구조체로 리팩토링

**파일명: src/main.rs**

```rust
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!(
        "The area of the rectangle is {} square pixels.",
        area(&rect1)
    );
}

fn area(rectangle: &Rectangle) -> u32 {
    rectangle.width * rectangle.height
}
```

**Listing 5-10**: `Rectangle` 구조체 정의

**이점**:
- 데이터에 의미 있는 레이블을 추가합니다
- 함수 시그니처가 Rectangle의 면적을 계산한다는 것을 명확히 나타냅니다
- 너비와 높이가 관련되어 있음을 보여줍니다
- 소유권을 가져가지 않기 위해 불변 빌림(`&Rectangle`)을 사용합니다
- `main`이 `rect1`의 소유권을 유지하고 계속 사용할 수 있습니다

---

## 파생 트레이트로 기능 추가

### 출력 시도

**파일명: src/main.rs**

```rust
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!("rect1 is {rect1}");
}
```

**Listing 5-11**: `Rectangle` 인스턴스 출력 시도

**오류**:
```
error[E0277]: `Rectangle` doesn't implement `std::fmt::Display`
```

구조체는 포맷팅 방법이 여러 가지이기 때문에 `Display`의 기본 구현을 제공하지 않습니다.

### Debug 포맷팅 사용

`Debug` 트레이트는 개발자에게 유용한 출력을 제공합니다. `:?` 지정자를 사용합니다:

```rust
println!("rect1 is {rect1:?}");
```

그러나 먼저 `Debug` 트레이트를 파생해야 합니다.

### Debug 트레이트 파생

**파일명: src/main.rs**

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let rect1 = Rectangle {
        width: 30,
        height: 50,
    };

    println!("rect1 is {rect1:?}");
}
```

**Listing 5-12**: `Debug` 트레이트 파생을 위한 속성 추가

**출력**:
```
$ cargo run
   Compiling rectangles v0.1.0 (file:///projects/rectangles)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.48s
     Running `target/debug/rectangles`
rect1 is Rectangle { width: 30, height: 50 }
```

### 예쁜 출력

더 큰 구조체의 경우 더 읽기 쉬운 출력을 위해 `{:#?}`를 사용합니다:

```rust
println!("rect1 is {rect1:#?}");
```

**출력**:
```
rect1 is Rectangle {
    width: 30,
    height: 50,
}
```

### dbg! 매크로 사용

`dbg!` 매크로는 표현식의 소유권을 가져가고, 파일과 줄 번호를 출력하고, 값을 출력한 다음 소유권을 반환합니다:

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let scale = 2;
    let rect1 = Rectangle {
        width: dbg!(30 * scale),
        height: 50,
    };

    dbg!(&rect1);
}
```

**출력**:
```
$ cargo run
   Compiling rectangles v0.1.0 (file:///projects/rectangles)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.61s
     Running `target/debug/rectangles`
[src/main.rs:10:16] 30 * scale = 60
[src/main.rs:14:5] &rect1 = Rectangle {
    width: 60,
    height: 50,
}
```

**참고**: `dbg!` 매크로는 `stderr`로 출력하고, `println!`은 `stdout`으로 출력합니다.

---

## 요약

이 챕터에서는 코드 명확성을 점진적으로 개선하는 방법을 보여줍니다:

1. **별도의 변수**: 관련 데이터에 대한 명확성 부족
2. **튜플**: 데이터를 그룹화하지만 의미론적 의미 부족
3. **구조체**: 명명된 필드와 명확한 의미론 제공
4. **파생 트레이트**: 유용한 디버깅 기능 활성화

`#[derive(Debug)]` 속성은 구조체를 디버깅 목적으로 출력 가능하게 만듭니다. 추가 파생 가능한 트레이트는 부록 C에서 확인할 수 있습니다. 다음 섹션에서는 `area` 함수를 `Rectangle` 구조체의 메서드로 변환하는 방법을 다룹니다.
