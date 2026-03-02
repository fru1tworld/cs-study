# UTF-8로 인코딩된 텍스트를 문자열로 저장하기

## 개요

Rust는 핵심 언어에서 문자열 슬라이스 `str`이라는 하나의 문자열 타입만 가지고 있으며, 일반적으로 빌린 형태인 `&str`로 볼 수 있습니다. `String` 타입은 Rust의 표준 라이브러리에서 증가 가능하고, 가변이며, 소유되고, UTF-8로 인코딩된 문자열 타입으로 제공됩니다.

## 문자열 정의

- **`&str`**: 문자열 슬라이스 - 다른 곳에 저장된 UTF-8로 인코딩된 문자열 데이터에 대한 참조 (예: 프로그램 바이너리의 문자열 리터럴)
- **`String`**: 추가 보장, 제한 및 기능과 함께 바이트 벡터(`Vec<u8>`)를 감싸는 래퍼로 구현됨

두 타입 모두 UTF-8로 인코딩되며 Rust의 표준 라이브러리에서 많이 사용됩니다.

## 새 문자열 만들기

### `String::new()` 사용
```rust
fn main() {
    let mut s = String::new();
}
```

### `to_string()` 사용
```rust
fn main() {
    let data = "initial contents";
    let s = data.to_string();

    // 리터럴에 직접 작동
    let s = "initial contents".to_string();
}
```

### `String::from()` 사용
```rust
fn main() {
    let s = String::from("initial contents");
}
```

### UTF-8 지원
```rust
fn main() {
    let hello = String::from("السلام عليكم");
    let hello = String::from("Dobrý den");
    let hello = String::from("Hello");
    let hello = String::from("שלום");
    let hello = String::from("नमस्ते");
    let hello = String::from("こんにちは");
    let hello = String::from("안녕하세요");
    let hello = String::from("你好");
    let hello = String::from("Olá");
    let hello = String::from("Здравствуйте");
    let hello = String::from("Hola");
}
```

## 문자열 업데이트

### `push_str()`로 추가
```rust
fn main() {
    let mut s = String::from("foo");
    s.push_str("bar");
}
// 결과: "foobar"
```

`push_str()`은 문자열 슬라이스를 받고 소유권을 가져가지 않아 매개변수를 계속 사용할 수 있습니다:

```rust
fn main() {
    let mut s1 = String::from("foo");
    let s2 = "bar";
    s1.push_str(s2);
    println!("s2 is {s2}"); // s2는 여전히 유효
}
```

### `push()`로 추가
```rust
fn main() {
    let mut s = String::from("lo");
    s.push('l');
}
// 결과: "lol"
```

### `+`로 연결
```rust
fn main() {
    let s1 = String::from("Hello, ");
    let s2 = String::from("world!");
    let s3 = s1 + &s2; // s1은 이동됨, 더 이상 사용할 수 없음
}
// 결과: "Hello, world!"
```

**중요**: `+` 연산자는 다음 시그니처의 `add` 메서드를 사용합니다:
```rust
fn add(self, s: &str) -> String
```

- `self`의 소유권을 가져감 (s1이 이동됨)
- 두 번째 문자열의 참조를 받음 (`&s2`)
- 컴파일러가 `&String`을 `&str`로 강제 변환

### `format!`으로 여러 문자열 연결
```rust
fn main() {
    let s1 = String::from("tic");
    let s2 = String::from("tac");
    let s3 = String::from("toe");

    let s = format!("{s1}-{s2}-{s3}");
}
// 결과: "tic-tac-toe"
```

`format!` 매크로:
- 출력하는 대신 `String`을 반환
- 참조를 사용하므로 매개변수의 소유권을 가져가지 않음
- 여러 `+` 작업보다 더 읽기 쉬움

## 문자열 인덱싱

**Rust는 문자열에 대한 직접 인덱싱을 지원하지 않습니다:**

```rust
fn main() {
    let s1 = String::from("hi");
    let h = s1[0]; // 오류!
}
```

오류:
```
error[E0277]: the type `str` cannot be indexed by `{integer}`
```

### 인덱싱이 없는 이유

`String`은 `Vec<u8>`을 감싸는 래퍼입니다. 다른 UTF-8 문자는 다른 바이트 수를 차지합니다:

- `"Hola"`: 4바이트 (문자당 1바이트)
- `"Здравствуйте"`: 24바이트 (키릴 문자당 2바이트)

바이트에 대한 인덱스가 항상 유효한 유니코드 스칼라 값과 일치하지 않습니다:

```rust
let hello = "Здравствуйте";
let answer = &hello[0]; // 'З'가 아니라 바이트 208을 반환할 것
```

인덱스 0의 바이트는 `208`이지만, 이는 2바이트 문자의 첫 번째 바이트입니다. `208`만 반환하면 유효한 문자가 아니며 버그를 일으킬 것입니다.

또한, 인덱싱은 상수 시간 O(1)이 예상되지만, Rust는 유효한 문자 경계를 결정하기 위해 문자열의 처음부터 순회해야 합니다.

## 내부 표현: 바이트, 스칼라 값, 자소 클러스터

예: 데바나가리 문자로 된 힌디어 단어 "नमस्ते"

**바이트로** (18바이트):
```
[224, 164, 168, 224, 164, 174, 224, 164, 184, 224, 165, 141, 224, 164, 164, 224, 165, 135]
```

**유니코드 스칼라 값으로** (6개의 `char` 값):
```
['न', 'म', 'स', '्', 'त', 'े']
```
(4번째와 6번째는 분음 부호이며 독립적인 글자가 아님)

**자소 클러스터로** (4개의 글자):
```
["न", "म", "स्", "ते"]
```

Rust는 다른 프로그램이 다른 해석을 필요로 하므로 문자열 데이터를 해석하는 다양한 방법을 제공합니다.

## 문자열 슬라이싱

범위 문법을 사용하여 문자열 슬라이스를 만듭니다:

```rust
fn main() {
    let hello = "Здравствуйте";
    let s = &hello[0..4];
}
// s = "Зд" (처음 4바이트 = 2문자)
```

**주의**: 멀티바이트 문자 중간에서 슬라이싱하면 패닉이 발생합니다:

```rust
let s = &hello[0..1]; // 패닉! 바이트 인덱스 1에서 패닉
```

오류:
```
thread 'main' panicked at src/main.rs:4:19:
byte index 1 is not a char boundary; it is inside 'З' (bytes 0..2) of `Здравствуйте`
```

## 문자열 반복

### 유니코드 스칼라 값에 `chars()` 사용

```rust
fn main() {
    for c in "Зд".chars() {
        println!("{c}");
    }
}
```

출력:
```
З
д
```

### 원시 바이트에 `bytes()` 사용

```rust
fn main() {
    for b in "Зд".bytes() {
        println!("{b}");
    }
}
```

출력:
```
208
151
208
180
```

### 자소 클러스터 가져오기

자소 클러스터 기능은 표준 라이브러리에서 제공되지 않습니다. 필요하다면 [crates.io](https://crates.io/)의 크레이트를 사용하세요.

## 문자열의 복잡성 처리

Rust는 `String` 데이터의 올바른 처리를 기본 동작으로 만들어, 프로그래머가 처음부터 UTF-8 데이터에 대해 생각해야 합니다. 이는 다른 언어보다 더 많은 복잡성을 노출하지만, 개발 후반에 비ASCII 문자와 관련된 버그를 방지합니다.

### 유용한 표준 라이브러리 메서드

- `contains()` - 문자열에서 검색
- `replace()` - 문자열의 일부를 대체

표준 라이브러리는 이러한 복잡성을 올바르게 처리하기 위해 `String`과 `&str` 타입을 기반으로 구축된 상당한 기능을 제공합니다.
