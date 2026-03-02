# 해시 맵에 연관된 값과 키 저장하기

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
