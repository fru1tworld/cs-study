# 벡터로 값 목록 저장하기

## 개요

벡터라고도 불리는 `Vec<T>`는 모든 값이 메모리에 연속적으로 저장되는 단일 데이터 구조에 여러 값을 저장할 수 있는 컬렉션 타입입니다. 벡터는 같은 타입의 값만 저장할 수 있으며 파일의 텍스트 줄이나 장바구니의 가격과 같은 항목 목록에 유용합니다.

## 새 벡터 만들기

### `Vec::new` 사용

새로운 빈 벡터를 만들려면 `Vec::new` 함수를 호출합니다:

```rust
fn main() {
    let v: Vec<i32> = Vec::new();
}
```

**참고:** Rust가 초기 값 없이는 요소 타입을 추론할 수 없으므로 빈 벡터를 만들 때 타입 주석이 필요합니다.

### `vec!` 매크로 사용

더 일반적으로, Rust가 타입을 추론할 수 있게 해주는 `vec!` 매크로를 사용하여 초기 값이 있는 벡터를 만듭니다:

```rust
fn main() {
    let v = vec![1, 2, 3];
}
```

이 경우 Rust는 정수 타입(기본 정수 타입)을 기반으로 타입을 `Vec<i32>`로 추론합니다.

## 벡터 업데이트

벡터를 만들고 요소를 추가하려면 `push` 메서드를 사용합니다:

```rust
fn main() {
    let mut v = Vec::new();

    v.push(5);
    v.push(6);
    v.push(7);
    v.push(8);
}
```

**중요:** 벡터를 수정하려면 `mut` 키워드를 사용하여 가변으로 선언해야 합니다. Rust는 push된 값에서 `Vec<i32>` 타입을 추론합니다.

## 벡터 요소 읽기

벡터 요소를 참조하는 두 가지 방법이 있습니다:

### 1. 인덱싱 문법

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let third: &i32 = &v[2];
    println!("The third element is {third}");
}
```

벡터는 0부터 인덱싱됩니다. `&`와 `[]`를 사용하면 요소에 대한 참조를 반환합니다.

### 2. `get` 메서드 사용

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let third: Option<&i32> = v.get(2);
    match third {
        Some(third) => println!("The third element is {third}"),
        None => println!("There is no third element."),
    }
}
```

`get` 메서드는 매치할 수 있는 `Option<&T>`를 반환합니다.

### 범위 밖 접근 시 동작 차이

벡터 범위 밖의 요소에 접근할 때:

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let does_not_exist = &v[100];  // 패닉!
    let does_not_exist = v.get(100);  // None 반환
}
```

- **인덱싱 `[]`**: 인덱스가 범위를 벗어나면 패닉합니다. 잘못된 접근 시 프로그램이 충돌하길 원할 때 사용합니다.
- **`get` 메서드**: 범위를 벗어나면 `None`을 반환합니다. 정상적인 작업 중에 범위 밖 접근이 발생할 수 있을 때 사용합니다.

### 벡터 참조와 빌림 규칙

같은 스코프에서 불변 참조를 보유하고 벡터를 가변적으로 수정할 수 없습니다:

```rust
fn main() {
    let mut v = vec![1, 2, 3, 4, 5];

    let first = &v[0];

    v.push(6);  // 오류: 가변으로 빌릴 수 없음

    println!("The first element is: {first}");
}
```

**오류:**
```
error[E0502]: cannot borrow `v` as mutable because it is also borrowed as immutable
```

이 오류는 벡터가 요소를 메모리에 연속적으로 저장하기 때문에 발생합니다. 새 요소를 추가하면 메모리를 재할당하고 이전 요소를 복사해야 할 수 있으며, 이는 기존 참조를 무효화합니다.

## 벡터 값 반복

### 불변 반복

```rust
fn main() {
    let v = vec![100, 32, 57];
    for i in &v {
        println!("{i}");
    }
}
```

### 가변 반복

```rust
fn main() {
    let mut v = vec![100, 32, 57];
    for i in &mut v {
        *i += 50;
    }
}
```

값에 접근하고 수정하려면 역참조 연산자 `*`를 사용합니다. 빌림 검사기는 반복 루프 내에서 벡터의 동시 수정을 방지합니다.

## 열거형을 사용하여 여러 타입 저장

벡터는 하나의 타입만 저장할 수 있지만, 열거형을 사용하여 감싸면 여러 타입을 저장할 수 있습니다:

```rust
fn main() {
    enum SpreadsheetCell {
        Int(i32),
        Float(f64),
        Text(String),
    }

    let row = vec![
        SpreadsheetCell::Int(3),
        SpreadsheetCell::Text(String::from("blue")),
        SpreadsheetCell::Float(10.12),
    ];
}
```

Rust는 컴파일 시점에 모든 가능한 타입을 알아야 합니다. 런타임에 가능한 타입의 전체 집합을 모른다면 대신 트레이트 객체(18장)를 사용하세요.

## 벡터 드롭

다른 구조체처럼 벡터는 스코프를 벗어나면 해제되고 포함된 모든 요소도 드롭됩니다:

```rust
fn main() {
    {
        let v = vec![1, 2, 3, 4];

        // v로 작업 수행
    } // <- v는 여기서 스코프를 벗어나고 해제됨
}
```

빌림 검사기는 벡터 내용에 대한 참조가 벡터가 유효한 동안에만 사용되도록 보장합니다.

## 추가 메서드

`push` 외에도 표준 라이브러리는 `Vec<T>`에 많은 유용한 메서드를 제공합니다. 예를 들어:
- `pop` - 마지막 요소를 제거하고 반환

사용 가능한 모든 메서드는 [API 문서](../std/vec/struct.Vec.html)를 참조하세요.
