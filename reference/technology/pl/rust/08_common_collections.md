# 일반적인 컬렉션

# 일반적인 컬렉션

> **원문:** https://doc.rust-lang.org/book/ch08-00-common-collections.html

## 개요

Rust의 표준 라이브러리에는 **컬렉션**이라고 불리는 매우 유용한 데이터 구조가 여러 개 포함되어 있습니다. 대부분의 다른 데이터 타입은 하나의 특정 값을 나타내지만, 컬렉션은 여러 값을 포함할 수 있습니다.

### 주요 특징

- 내장 배열 및 튜플 타입과 달리, 이러한 컬렉션이 가리키는 데이터는 **힙**에 저장됩니다
- 데이터의 양을 컴파일 시점에 알 필요가 없습니다
- 프로그램이 실행되는 동안 컬렉션이 커지거나 줄어들 수 있습니다
- 각 종류의 컬렉션은 서로 다른 기능과 비용을 가집니다
- 현재 상황에 적합한 것을 선택하는 것은 시간이 지나면서 개발하게 될 기술입니다

## 세 가지 주요 컬렉션

이 챕터에서는 Rust 프로그램에서 매우 자주 사용되는 세 가지 컬렉션을 다룹니다:

1. **벡터** - 여러 값을 나란히 저장할 수 있게 해줍니다

2. **문자열** - 문자들의 컬렉션입니다. `String` 타입은 이전에 언급되었지만, 이 챕터에서 자세히 다룹니다

3. **해시 맵** - 값을 특정 키와 연관시킬 수 있게 해줍니다. **맵**이라고 불리는 더 일반적인 데이터 구조의 특정 구현입니다

## 다루는 주제

이 챕터에서는 다음을 논의합니다:
- 벡터, 문자열, 해시 맵을 생성하고 업데이트하는 방법
- 각 컬렉션을 특별하게 만드는 것

## 추가 자료

표준 라이브러리가 제공하는 다른 종류의 컬렉션에 대한 정보는 [표준 라이브러리 문서](../std/collections/index.html)를 참조하세요.

---

# 벡터로 값 목록 저장하기

> **원문:** https://doc.rust-lang.org/book/ch08-01-vectors.html

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

---

# UTF-8로 인코딩된 텍스트를 문자열로 저장하기

> **원문:** https://doc.rust-lang.org/book/ch08-02-strings.html

## 개요

Rust는 핵심 언어에서 문자열 슬라이스 `str`이라는 문자열 타입 하나만 있으며, 일반적으로 빌린 형태인 `&str`로 볼 수 있습니다. `String` 타입은 Rust의 표준 라이브러리에서 증가 가능하고, 가변이며, 소유되고, UTF-8로 인코딩된 문자열 타입으로 제공됩니다.

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

---

# 해시 맵에 연관된 값과 키 저장하기

> **원문:** https://doc.rust-lang.org/book/ch08-03-hash-maps.html

## 개요

`HashMap<K, V>` 타입은 해싱 함수를 사용하여 타입 `K`의 키를 타입 `V`의 값에 매핑하여 저장합니다. 해시 맵은 벡터처럼 인덱스가 아닌 모든 타입의 키로 데이터를 조회하는 데 유용합니다.

많은 프로그래밍 언어가 이 데이터 구조를 다른 이름으로 지원합니다: 해시, 맵, 객체, 해시 테이블, 딕셔너리, 연관 배열 등.

## 새 해시 맵 만들기

`new`를 사용하여 빈 해시 맵을 만들고 `insert`로 요소를 추가할 수 있습니다:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);
}
```

**핵심 포인트:**
- `HashMap`은 `std::collections`에서 명시적으로 가져와야 함
- 벡터와 문자열과 달리 프렐루드에 자동으로 포함되지 않음
- 해시 맵은 데이터를 힙에 저장
- 벡터처럼 동종성을 가짐: 모든 키는 같은 타입이어야 하고, 모든 값도 같은 타입이어야 함

## 해시 맵의 값 접근

`get` 메서드를 사용하여 값을 검색합니다:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);

    let team_name = String::from("Blue");
    let score = scores.get(&team_name).copied().unwrap_or(0);
}
```

`get` 메서드는 `Option<&V>`를 반환합니다:
- 키가 존재하면 `Some(&value)` 반환
- 키가 존재하지 않으면 `None` 반환

`copied()`를 사용하여 `Option<&i32>`를 `Option<i32>`로 변환하고, `unwrap_or()`를 사용하여 기본값을 제공합니다.

### 키-값 쌍 반복

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);

    for (key, value) in &scores {
        println!("{key}: {value}");
    }
}
```

출력 (임의 순서):
```
Yellow: 50
Blue: 10
```

## 해시 맵에서 소유권 관리

### `Copy` 타입의 경우
`Copy`를 구현하는 값(`i32` 같은)은 해시 맵에 복사됩니다:

```rust
scores.insert(String::from("Blue"), 10);  // 10이 복사됨
```

### 소유된 타입의 경우
`String` 같은 소유된 값은 해시 맵으로 이동되고, 해시 맵이 소유자가 됩니다:

```rust
fn main() {
    use std::collections::HashMap;

    let field_name = String::from("Favorite color");
    let field_value = String::from("Blue");

    let mut map = HashMap::new();
    map.insert(field_name, field_value);
    // field_name과 field_value는 이제 유효하지 않음
}
```

### 참조
참조를 삽입하면 값이 이동되지 않습니다. 참조된 값은 해시 맵이 존재하는 동안 유효해야 합니다.

## 해시 맵 업데이트

### 값 덮어쓰기

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Blue"), 25);

    println!("{scores:?}");
}
```

출력: `{"Blue": 25}`

### 키가 없을 때만 추가

`entry` API와 `or_insert`를 사용합니다:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();
    scores.insert(String::from("Blue"), 10);

    scores.entry(String::from("Yellow")).or_insert(50);
    scores.entry(String::from("Blue")).or_insert(50);

    println!("{scores:?}");
}
```

출력: `{"Yellow": 50, "Blue": 10}`

`or_insert` 메서드:
- 키가 존재하면 값에 대한 가변 참조를 반환
- 키가 존재하지 않으면 매개변수를 삽입하고 가변 참조를 반환

### 이전 값을 기반으로 업데이트

```rust
fn main() {
    use std::collections::HashMap;

    let text = "hello world wonderful world";

    let mut map = HashMap::new();

    for word in text.split_whitespace() {
        let count = map.entry(word).or_insert(0);
        *count += 1;
    }

    println!("{map:?}");
}
```

출력: `{"world": 2, "hello": 1, "wonderful": 1}`

`or_insert` 메서드는 가변 참조(`&mut V`)를 반환하며, `*`로 역참조하여 값을 수정할 수 있습니다.

## 해싱 함수

기본적으로 `HashMap`은 서비스 거부(DoS) 공격에 대한 저항성을 제공하는 **SipHash**를 사용합니다. 가장 빠른 알고리즘은 아니지만 보안 트레이드오프는 가치가 있습니다.

다른 해싱 알고리즘을 사용하려면:
- `BuildHasher` 트레이트를 구현 (10장에서 논의)
- [crates.io](https://crates.io/)의 서드파티 해셔를 사용

## 연습 문제

1. **중앙값과 최빈값**: 정수 목록이 주어지면, 벡터를 사용하고 중앙값과 최빈값을 반환합니다 (해시 맵이 여기서 도움됨)

2. **피그 라틴**: 문자열을 피그 라틴으로 변환합니다:
   - 첫 번째 자음을 끝으로 이동하고 "ay" 추가
   - 예: "first" -> "irst-fay"
   - 모음으로 시작하는 단어는 "hay" 추가
   - 예: "apple" -> "apple-hay"

3. **직원 관리**: 해시 맵과 벡터를 사용하여 텍스트 인터페이스를 만듭니다:
   - 직원 이름을 부서에 추가
   - 부서별 목록 검색
   - 모든 사람을 알파벳순으로 정렬하여 표시
