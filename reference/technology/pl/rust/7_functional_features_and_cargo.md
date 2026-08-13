# 러스트 함수형 언어의 특징과 Cargo

## 함수형 언어 기능: 반복자와 클로저

> **원문:** https://doc.rust-lang.org/book/ch13-00-functional-features.html

- Rust 설계는 기존 여러 언어와 기술에서 영감을 받음 → 그중 중요한 하나가 *함수형 프로그래밍*
- 함수형 스타일 프로그래밍의 핵심: 함수를 값으로 다룸
  - 함수를 인수로 전달
  - 함수를 다른 함수에서 반환
  - 나중에 실행하기 위해 함수를 변수에 할당
- 이 장의 범위: 함수형 프로그래밍의 정의 논쟁은 다루지 않음 → 다른 언어에서 흔히 함수형이라 불리는 기능과 유사한 Rust 기능만 논의

다루는 내용:

- *클로저*: 변수에 저장할 수 있는 함수와 유사한 구조
- *반복자*: 일련의 요소를 처리하는 방법
- 12장 I/O 프로젝트를 클로저와 반복자로 개선하는 방법
- 클로저와 반복자의 성능 (스포일러: 생각보다 빠름)

- 패턴 매칭·열거형 등 다른 Rust 기능도 함수형 스타일의 영향을 받았고, 이미 다른 장에서 다룸
- 클로저와 반복자를 마스터하는 것 = 관용적이고 빠른 Rust 코드 작성의 핵심 → 이 장 전체를 이 주제에 할애

---

## 클로저: 환경을 캡처하는 익명 함수

> **원문:** https://doc.rust-lang.org/book/ch13-01-closures.html

- Rust 클로저: 변수에 저장하거나 다른 함수에 인수로 전달할 수 있는 익명 함수
- 한 곳에서 만들고 다른 컨텍스트에서 호출·평가 가능
- 일반 함수와 달리 정의된 스코프의 값을 캡처 가능
- 이 절에서 다루는 것: 클로저가 코드 재사용과 동작 사용자 정의를 어떻게 가능하게 하는지

### 클로저로 환경 캡처하기

- 목표: 클로저로 정의된 환경의 값을 캡처해 나중에 사용하는 방법
- 시나리오
  - 티셔츠 회사가 마케팅 프로모션으로 메일링 리스트 회원에게 한정판 셔츠를 무료 증정
  - 회원은 선택적으로 프로필에 좋아하는 색상을 등록 가능
  - 당첨자가 좋아하는 색상을 등록해 두었다면 → 그 색상 셔츠 지급
  - 색상 미지정 → 현재 재고가 가장 많은 색상 지급
- 구현 방식은 여러 가지이지만, 이 예제는 클로저에 초점을 맞추기 위해 이미 배운 개념만 사용
  - `ShirtColor` 열거형: `Red`, `Blue` 두 변형만 사용(단순화)
  - `Inventory` 구조체: `shirts` 필드(`Vec<ShirtColor>`)로 재고 표현
  - `giveaway` 메서드: 당첨자의 선택적 색상 선호(`Option<ShirtColor>`)를 받아 실제 지급 색상 반환
  - Listing 13-1

```rust
#[derive(Debug, PartialEq, Copy, Clone)]
enum ShirtColor {
    Red,
    Blue,
}

struct Inventory {
    shirts: Vec<ShirtColor>,
}

impl Inventory {
    fn giveaway(&self, user_preference: Option<ShirtColor>) -> ShirtColor {
        user_preference.unwrap_or_else(|| self.most_stocked())
    }

    fn most_stocked(&self) -> ShirtColor {
        let mut num_red = 0;
        let mut num_blue = 0;

        for color in &self.shirts {
            match color {
                ShirtColor::Red => num_red += 1,
                ShirtColor::Blue => num_blue += 1,
            }
        }
        if num_red > num_blue {
            ShirtColor::Red
        } else {
            ShirtColor::Blue
        }
    }
}

fn main() {
    let store = Inventory {
        shirts: vec![ShirtColor::Blue, ShirtColor::Red, ShirtColor::Blue],
    };

    let user_pref1 = Some(ShirtColor::Red);
    let giveaway1 = store.giveaway(user_pref1);
    println!(
        "선호도가 {:?}인 사용자가 {:?}를 받습니다",
        user_pref1, giveaway1
    );

    let user_pref2 = None;
    let giveaway2 = store.giveaway(user_pref2);
    println!(
        "선호도가 {:?}인 사용자가 {:?}를 받습니다",
        user_pref2, giveaway2
    );
}
```

*Listing 13-1: 셔츠 회사 경품 상황*

- `main`의 `store`: 파란색 셔츠 두 벌, 빨간색 셔츠 한 벌 재고
- `giveaway` 호출 대상: 빨간색을 선호하는 사용자, 선호도 없는 사용자
- 구현 방식은 다양하지만 이 예제는 클로저에 집중하려고 기존 개념만 사용, 단 `giveaway` 본문은 클로저 사용
  - `giveaway`: `Option<ShirtColor>` 타입 `user_preference`를 받아 `unwrap_or_else` 호출
  - [`unwrap_or_else` 메서드][unwrap-or-else](`Option<T>`, 표준 라이브러리 정의)
    - 인수 하나: 인수 없이 `T`를 반환하는 클로저
    - `Some` → `Some` 내부 값 반환
    - `None` → 클로저 호출 후 그 반환값 사용
  - 인수로 전달한 클로저 표현식: `|| self.most_stocked()`
    - 매개변수 없는 클로저(매개변수가 있으면 두 수직 막대 사이에 표기)
    - 본문은 `self.most_stocked()` 호출
    - 정의 시점과 평가 시점이 분리됨 → `unwrap_or_else` 구현이 결과가 필요할 때 나중에 평가

실행 결과:

```text
선호도가 Some(Red)인 사용자가 Red를 받습니다
선호도가 None인 사용자가 Blue를 받습니다
```

- 핵심 포인트: `Inventory` 인스턴스의 `self.most_stocked()`를 호출하는 클로저를 전달
  - 표준 라이브러리는 `Inventory`나 `ShirtColor` 타입, 이 시나리오의 로직을 전혀 몰라도 됨
  - 클로저가 `self`(`Inventory` 인스턴스)에 대한 불변 참조를 캡처 → 지정한 코드와 함께 `unwrap_or_else`에 전달
  - 함수는 이런 방식으로 환경을 캡처 불가 → 클로저만의 특징

### 클로저 타입 추론과 어노테이션

- 함수와 클로저의 차이
  - 클로저는 `fn` 함수와 달리 매개변수·반환값 타입을 명시적으로 쓸 필요 없음
  - 함수에 타입 어노테이션이 필요한 이유: 타입이 사용자에게 노출되는 명시적 인터페이스의 일부이기 때문 → 인터페이스를 엄격히 정의해야 함수 사용·반환 타입에 모두가 동의 가능
  - 클로저는 이런 노출된 인터페이스로 쓰이지 않음: 변수에 저장되고 이름 없이, 라이브러리 사용자에게 노출되지 않고 사용됨
- 클로저는 보통 짧고 좁은 컨텍스트 안에서만 쓰임 → 이 제한된 컨텍스트 안에서 컴파일러가 매개변수·반환 타입을 대부분 추론 가능(변수 타입 추론과 유사)
  - 예외적으로 컴파일러가 클로저 타입 어노테이션을 요구하는 드문 경우도 있음
- 변수와 마찬가지로 명시성·명확성을 높이려면 타입 어노테이션 추가 가능(단, 필요 이상으로 장황해지는 비용 감수)
  - Listing 13-2: Listing 13-1처럼 인수 전달 위치에서 클로저를 정의하지 않고, 클로저를 정의해 변수에 저장

```rust
use std::thread;
use std::time::Duration;

fn generate_workout(intensity: u32, random_number: u32) {
    let expensive_closure = |num: u32| -> u32 {
        println!("천천히 계산 중...");
        thread::sleep(Duration::from_secs(2));
        num
    };

    if intensity < 25 {
        println!("오늘은 팔굽혀펴기 {}회!", expensive_closure(intensity));
        println!("다음, 윗몸일으키기 {}회!", expensive_closure(intensity));
    } else {
        if random_number == 3 {
            println!("오늘은 휴식! 수분 섭취를 잊지 마세요!");
        } else {
            println!(
                "오늘은 {}분간 달리기!",
                expensive_closure(intensity)
            );
        }
    }
}

fn main() {
    let simulated_user_specified_value = 10;
    let simulated_random_number = 7;

    generate_workout(simulated_user_specified_value, simulated_random_number);
}
```

*Listing 13-2: 클로저에 선택적 매개변수 및 반환 값 타입 어노테이션 추가*

- 타입 어노테이션 추가 → 클로저 구문이 함수 구문과 더 유사해짐
- 매개변수에 1을 더하는 동일 동작을 함수/클로저로 비교(파이프 사용·선택적 구문 양 차이만 존재)

```rust
fn  add_one_v1   (x: u32) -> u32 { x + 1 }
let add_one_v2 = |x: u32| -> u32 { x + 1 };
let add_one_v3 = |x|             { x + 1 };
let add_one_v4 = |x|               x + 1  ;
```

- 1번째 줄: 함수 정의
- 2번째 줄: 완전히 어노테이션된 클로저 정의
- 3번째 줄: 타입 어노테이션 제거
- 4번째 줄: 본문이 단일 표현식이라 선택 사항인 중괄호도 제거
- 넷 다 호출 시 동작 동일한 유효한 정의
- `add_one_v3`, `add_one_v4`는 사용법에서 타입을 추론할 수 있도록 실제로 평가되어야 함

- 클로저 정의는 매개변수·반환값마다 구체적 타입 하나로 추론됨
- Listing 13-3: 받은 값을 그대로 반환하는 짧은 클로저(예제 목적 외 유용성 낮음), 타입 어노테이션 없음
  - 어노테이션이 없으니 `String`으로도, `i32`로도 호출 가능해 보이지만 → 실제로는 컴파일 실패

```rust
fn main() {
    let example_closure = |x| x;

    let s = example_closure(String::from("hello"));
    let n = example_closure(5);
}
```

*Listing 13-3: 두 가지 다른 타입으로 타입이 추론된 클로저를 호출하려고 시도*

컴파일러 오류:

```text
error[E0308]: mismatched types
 --> src/main.rs:5:29
  |
5 |     let n = example_closure(5);
  |             --------------- ^- help: try using a conversion method: `.to_string()`
  |             |               |
  |             |               expected `String`, found integer
  |             arguments to this function are incorrect
```

- 원인: `String` 값으로 처음 호출 → 컴파일러가 `x`와 반환 타입을 `String`으로 추론 → 이후 같은 클로저에 다른 타입 사용 시 타입 오류

### 참조 캡처 또는 소유권 이동

- 클로저가 환경 값을 캡처하는 세 가지 방법(함수 매개변수 방식과 1대1 대응)
  - 불변으로 빌리기
  - 가변으로 빌리기
  - 소유권 가져오기
- 어떤 방식을 쓸지는 클로저 본문이 캡처된 값으로 하는 작업에 따라 결정됨
- Listing 13-4: `list` 벡터에 대한 불변 참조만 캡처(출력에는 불변 참조로 충분)

```rust
fn main() {
    let list = vec![1, 2, 3];
    println!("클로저 정의 전: {list:?}");

    let only_borrows = || println!("클로저에서: {list:?}");

    println!("클로저 호출 전: {list:?}");
    only_borrows();
    println!("클로저 호출 후: {list:?}");
}
```

*Listing 13-4: 불변 참조를 캡처하는 클로저 정의 및 호출*

- 변수가 클로저 정의에 바인딩되고, 이후 변수 이름 + 괄호로 함수처럼 호출 가능함을 보여주는 예제이기도 함
- `list`에 대한 불변 참조는 동시에 여럿 존재 가능 → 클로저 정의 전/후, 호출 전/후 어디서든 `list` 접근 가능

```text
클로저 정의 전: [1, 2, 3]
클로저 호출 전: [1, 2, 3]
클로저에서: [1, 2, 3]
클로저 호출 후: [1, 2, 3]
```

- Listing 13-5: 클로저 본문에서 `list`에 요소 추가 → 이제 가변 참조를 캡처

```rust
fn main() {
    let mut list = vec![1, 2, 3];
    println!("클로저 정의 전: {list:?}");

    let mut borrows_mutably = || list.push(7);

    borrows_mutably();
    println!("클로저 호출 후: {list:?}");
}
```

*Listing 13-5: 가변 참조를 캡처하는 클로저 정의 및 호출*

실행 결과:

```text
클로저 정의 전: [1, 2, 3]
클로저 호출 후: [1, 2, 3, 7]
```

- 주의: `borrows_mutably` 정의 시점부터 `list`에 대한 가변 빌림이 끝나는 호출 시점 사이에 `println!`이 없음
  - 가변 빌림 중에는 다른 빌림(불변 포함) 허용 안 됨 → 그 사이에 `println!`을 넣어 보면 오류 확인 가능
- `move` 키워드
  - 클로저 본문이 소유권을 엄밀히 필요로 하지 않아도, 매개변수 목록 앞에 `move`를 붙이면 캡처한 값의 소유권을 강제로 갖게 함
  - 주 용도: 새 스레드에 데이터를 이동시켜 그 스레드가 소유하게 할 때(스레드·동시성은 16장에서 상세 설명)
  - Listing 13-6: Listing 13-4를 수정해 메인 스레드가 아닌 새 스레드에서 벡터 출력

```rust
use std::thread;

fn main() {
    let list = vec![1, 2, 3];
    println!("클로저 정의 전: {list:?}");

    thread::spawn(move || println!("스레드에서: {list:?}"))
        .join()
        .unwrap();
}
```

*Listing 13-6: 클로저가 스레드에 대해 `list`의 소유권을 갖도록 `move` 사용*

- 새 스레드를 생성해 클로저를 인수로 전달·실행, 본문은 목록 출력
- Listing 13-4의 클로저는 불변 참조만으로 충분했지만, 이 예제는 본문이 여전히 불변 참조만 필요해도 `move`로 `list` 이동을 명시해야 함
  - 이유: 새 스레드가 메인 스레드보다 먼저 끝날지 나중에 끝날지 알 수 없음 → 메인 스레드가 `list` 소유권을 유지한 채 먼저 끝나 `list`를 드롭하면 새 스레드의 참조가 무효화됨
  - 컴파일러는 참조 유효성을 보장하기 위해 `list`가 클로저로 이동되도록 요구
  - `move`를 제거하거나 클로저 정의 후 메인 스레드에서 `list`를 사용해 보면 어떤 오류가 나는지 직접 확인 가능

### 캡처된 값을 클로저 밖으로 이동하기와 `Fn` 트레이트

- 클로저가 정의된 환경에서 값의 참조·소유권을 캡처(클로저 *안*으로 이동) → 이후 클로저 본문 코드가 평가될 때 그 값에 어떤 일이 일어나는지 결정(클로저 *밖*으로 이동에 영향)
- 클로저 본문이 할 수 있는 것 (택1)
  - 캡처된 값을 클로저 밖으로 이동
  - 캡처된 값을 변형
  - 값을 이동도 변형도 하지 않음
  - 처음부터 환경에서 아무것도 캡처하지 않음
- 값 처리 방식 → 구현하는 `Fn` 트레이트에 영향(트레이트 = 함수·구조체가 받을 수 있는 클로저 종류를 지정하는 방법)
  - `FnOnce`: 한 번 호출 가능한 클로저 → 모든 클로저가 호출 가능하므로 최소한 이 트레이트는 구현
    - 캡처된 값을 본문 밖으로 이동하는 클로저는 `FnOnce`만 구현(한 번만 호출 가능하기 때문)
  - `FnMut`: 캡처된 값을 밖으로 이동하지 않지만 변형은 가능한 클로저 → 두 번 이상 호출 가능
  - `Fn`: 캡처된 값을 밖으로 이동하지도 변형하지도 않는 클로저, 또는 환경에서 아무것도 캡처하지 않는 클로저
    - 환경을 변형하지 않고 두 번 이상 호출 가능 → 동시에 여러 번 호출하는 케이스에서 중요

- `Option<T>`의 `unwrap_or_else` 정의:

```rust
impl<T> Option<T> {
    pub fn unwrap_or_else<F>(self, f: F) -> T
    where
        F: FnOnce() -> T
    {
        match self {
            Some(x) => x,
            None => f(),
        }
    }
}
```

- `T`: `Option`의 `Some` 값 타입을 나타내는 제네릭 타입 = `unwrap_or_else`의 반환 타입도 동일(예: `Option<String>`에서 호출 시 `String` 반환)
- `F`: 추가 제네릭 타입 매개변수, 매개변수 `f`(호출 시 넘기는 클로저)의 타입
- `F`의 트레이트 바운드: `FnOnce() -> T` → `F`는 한 번 호출, 인수 없음, `T` 반환
  - `unwrap_or_else`가 `f`를 최대 한 번만 호출한다는 제약을 표현
  - 본문 확인: `Some`이면 `f` 미호출, `None`이면 `f` 한 번 호출
  - 모든 클로저가 `FnOnce`를 구현하므로 `unwrap_or_else`는 가장 다양한 클로저를 받아들여 유연함

> 참고: 함수도 세 가지 `Fn` 트레이트를 모두 구현할 수 있습니다. 우리가 하려는 일이 환경에서 값을 캡처할 필요가 없다면, `Fn` 트레이트 중 하나를 구현하는 것이 필요한 곳에서 클로저 대신 함수 이름을 사용할 수 있습니다. 예를 들어, `Option<Vec<T>>` 값에 대해 `unwrap_or_else(Vec::new)`를 호출하여 값이 `None`이면 새 빈 벡터를 얻을 수 있습니다.

- 이어서 살펴볼 것: 슬라이스의 표준 라이브러리 메서드 `sort_by_key`
  - `unwrap_or_else`와의 차이, 왜 `FnOnce` 대신 `FnMut` 바운드를 쓰는지
  - 클로저 인수: 처리 중인 슬라이스 현재 항목에 대한 참조 하나 → 정렬 가능한 `K` 타입 값 반환
  - 용도: 각 항목의 특정 속성으로 슬라이스를 정렬하고 싶을 때
- Listing 13-7: `Rectangle` 목록을 `width` 기준 오름차순 정렬

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let mut list = [
        Rectangle { width: 10, height: 1 },
        Rectangle { width: 3, height: 5 },
        Rectangle { width: 7, height: 12 },
    ];

    list.sort_by_key(|r| r.width);
    println!("{list:#?}");
}
```

*Listing 13-7: `sort_by_key`를 사용하여 너비로 사각형 정렬*

출력:

```text
[
    Rectangle {
        width: 3,
        height: 5,
    },
    Rectangle {
        width: 7,
        height: 12,
    },
    Rectangle {
        width: 10,
        height: 1,
    },
]
```

- `sort_by_key`가 `FnMut` 클로저를 받도록 정의된 이유: 슬라이스 각 항목마다 한 번씩, 즉 여러 번 호출하기 때문
  - `|r| r.width`는 환경에서 캡처·변형·이동이 전혀 없음 → 바운드 요구 사항 충족

- 반대 사례: Listing 13-8, 환경에서 값을 이동해 `FnOnce`만 구현 → 컴파일러가 `sort_by_key`와의 사용을 허용 안 함

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let mut list = [
        Rectangle { width: 10, height: 1 },
        Rectangle { width: 3, height: 5 },
        Rectangle { width: 7, height: 12 },
    ];

    let mut sort_operations = vec![];
    let value = String::from("closure called");

    list.sort_by_key(|r| {
        sort_operations.push(value);
        r.width
    });
    println!("{list:#?}");
}
```

*Listing 13-8: `FnOnce`만 구현하는 클로저를 `sort_by_key`와 함께 사용하려고 시도*

- 의도: `list` 정렬 시 `sort_by_key`가 클로저를 호출하는 횟수를 세려는 인위적 시도(실제로는 작동 안 함)
- 시도 방식: 클로저 환경의 `String` `value`를 `sort_operations` 벡터에 push → `value`를 클로저 밖으로 이동
- 문제: 이 클로저는 한 번만 호출 가능 → 두 번째 호출 시 `value`가 이미 환경에서 사라져 다시 push 불가 → `FnOnce`만 구현
- 컴파일 시도 시 오류: `value`를 클로저 밖으로 이동할 수 없음(클로저가 `FnMut`를 구현해야 하기 때문)

```text
error[E0507]: cannot move out of `value`, a captured variable in an `FnMut` closure
```

- 오류 위치: `value`를 환경 밖으로 이동하는 줄
- 수정 방향: 본문을 바꿔 값을 환경 밖으로 이동하지 않게 함 → 호출 횟수를 세려면 환경에 카운터를 두고 본문에서 증가시키는 편이 더 직접적
- Listing 13-9: `num_sort_operations` 카운터에 대한 가변 참조만 캡처 → 여러 번 호출 가능 → `sort_by_key`와 정상 작동

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

fn main() {
    let mut list = [
        Rectangle { width: 10, height: 1 },
        Rectangle { width: 3, height: 5 },
        Rectangle { width: 7, height: 12 },
    ];

    let mut num_sort_operations = 0;
    list.sort_by_key(|r| {
        num_sort_operations += 1;
        r.width
    });
    println!("{list:#?}, {num_sort_operations}번 연산으로 정렬됨");
}
```

*Listing 13-9: `FnMut` 클로저를 `sort_by_key`와 함께 사용하는 것이 허용됨*

- `Fn` 트레이트는 클로저를 사용하는 함수·타입을 정의·사용할 때 중요
- 다음 절에서 반복자를 다룸: 많은 반복자 메서드가 클로저 인수를 받으므로 위 내용을 기억해 둘 것

[unwrap-or-else]: https://doc.rust-lang.org/std/option/enum.Option.html#method.unwrap_or_else

---

## 반복자로 일련의 항목 처리하기

> **원문:** https://doc.rust-lang.org/book/ch13-02-iterators.html

- 반복자 패턴: 일련의 항목에 순서대로 어떤 작업을 수행
- 반복자가 담당하는 것: 각 항목 반복 + 시퀀스 종료 판단 로직 → 사용자는 이 로직을 직접 재구현할 필요 없음
- Rust 반복자는 *지연적(lazy)* → 소비하는 메서드를 호출하기 전까지는 아무 효과 없음
  - Listing 13-10: `Vec<T>`의 `iter` 메서드로 `v1`의 항목에 대한 반복자 생성 → 이 코드 자체는 아무 일도 하지 않음

```rust
fn main() {
    let v1 = vec![1, 2, 3];

    let v1_iter = v1.iter();
}
```

*Listing 13-10: 반복자 생성하기*

- 반복자는 `v1_iter`에 저장됨 → 이후 다양한 방식으로 사용 가능
  - 3장 Listing 3-5: `for` 루프로 배열 반복(내부적으로 반복자를 암묵적으로 생성·소비했지만 상세 동작은 설명하지 않았음)
- Listing 13-11: 반복자 생성과 `for` 루프에서의 사용을 분리
  - `v1_iter`의 반복자를 `for` 루프에 넘기면 각 요소가 한 반복마다 사용되어 값 출력

```rust
fn main() {
    let v1 = vec![1, 2, 3];

    let v1_iter = v1.iter();

    for val in v1_iter {
        println!("받은 값: {val}");
    }
}
```

*Listing 13-11: `for` 루프에서 반복자 사용하기*

- 표준 라이브러리 반복자가 없는 언어라면: 인덱스 변수를 0에서 시작 → 벡터 인덱싱으로 값 획득 → 총 항목 수까지 인덱스 증가, 이런 식으로 동일 기능을 직접 작성해야 함
- 반복자의 이점: 이 모든 로직을 처리해 반복적이고 실수하기 쉬운 코드를 줄임 → 벡터 같은 인덱싱 가능 구조뿐 아니라 다양한 시퀀스에 동일 로직 재사용 가능

### `Iterator` 트레이트와 `next` 메서드

- 모든 반복자는 표준 라이브러리의 `Iterator` 트레이트를 구현

트레이트 정의:

```rust
pub trait Iterator {
    type Item;

    fn next(&mut self) -> Option<Self::Item>;

    // 기본 구현이 있는 메서드는 생략
}
```

- 새 구문 주목: `type Item`, `Self::Item` → 이 트레이트가 정의하는 *연관 타입*(20장에서 상세)
  - 지금 필요한 것: `Iterator` 구현 시 `Item` 타입도 함께 정의해야 함 + 이 `Item` 타입이 `next`의 반환 타입에 쓰임 → 즉 `Item` = 반복자가 반환하는 타입
- `Iterator` 구현자가 정의해야 할 것은 `next` 메서드 하나뿐
  - 반복자의 항목을 한 번에 하나씩 `Some`으로 감싸 반환, 반복이 끝나면 `None` 반환

- 반복자에서 직접 `next` 호출 가능 — Listing 13-12: 벡터에서 생성한 반복자에 `next`를 반복 호출한 결과

```rust
#[test]
fn iterator_demonstration() {
    let v1 = vec![1, 2, 3];

    let mut v1_iter = v1.iter();

    assert_eq!(v1_iter.next(), Some(&1));
    assert_eq!(v1_iter.next(), Some(&2));
    assert_eq!(v1_iter.next(), Some(&3));
    assert_eq!(v1_iter.next(), None);
}
```

*Listing 13-12: 반복자에서 `next` 메서드 호출하기*

- `v1_iter`를 가변으로 만들어야 하는 이유: `next` 호출이 반복자 내부의 현재 위치 추적 상태를 변경하기 때문
  - 즉 이 코드는 반복자를 *소비*함 → `next` 호출마다 항목 하나 소비
  - `for` 루프에서는 `v1_iter`를 가변으로 만들 필요 없었음: 루프가 소유권을 가져가 내부적으로 가변으로 만들기 때문
- `next`가 반환하는 값 = 벡터 값에 대한 불변 참조
  - `iter` 메서드 → 불변 참조에 대한 반복자 생성
  - 소유권을 가져가 소유된 값을 반환하는 반복자가 필요하면 `into_iter` 사용
  - 가변 참조를 반복하려면 `iter_mut` 사용

### 반복자를 소비하는 메서드

- `Iterator` 트레이트에는 표준 라이브러리 기본 구현이 있는 다른 메서드도 다수 존재(표준 라이브러리 API 문서 참고)
  - 이 중 일부는 정의 내부에서 `next`를 호출 → 그래서 `Iterator` 구현 시 `next` 구현이 필수
- `next`를 호출하는 메서드 = *소비 어댑터*(호출하면 반복자를 사용하게 되므로)
  - 예: `sum` 메서드 — 반복자의 소유권을 가져가 반복적으로 `next`를 호출하며 소비, 누적 합계에 각 항목을 더하고 반복 완료 시 합계 반환
  - Listing 13-13

```rust
#[test]
fn iterator_sum() {
    let v1 = vec![1, 2, 3];

    let v1_iter = v1.iter();

    let total: i32 = v1_iter.sum();

    assert_eq!(total, 6);
}
```

*Listing 13-13: `sum` 메서드를 호출하여 반복자의 모든 항목 합계 얻기*

- `sum`이 반복자 소유권을 가져가므로 호출 이후 `v1_iter` 사용 불가

### 다른 반복자를 생성하는 메서드

- *반복자 어댑터*: `Iterator` 트레이트에 정의된 메서드, 반복자를 소비하지 않음 → 원래 반복자의 일부 측면을 바꿔 다른 반복자를 생성
- Listing 13-14: 반복자 어댑터 `map` 호출 예
  - `map`은 항목마다 호출할 클로저를 받아, 수정된 항목을 만드는 새 반복자를 반환
  - 예제 클로저: 각 항목을 1씩 증가

```rust
fn main() {
    let v1: Vec<i32> = vec![1, 2, 3];

    v1.iter().map(|x| x + 1);
}
```

*Listing 13-14: 반복자 어댑터 `map`을 호출하여 새 반복자 생성하기*

경고 발생:

```text
warning: unused `Map` that must be used
 --> src/main.rs:4:5
  |
4 |     v1.iter().map(|x| x + 1);
  |     ^^^^^^^^^^^^^^^^^^^^^^^^
  |
  = note: iterators are lazy and do nothing unless consumed
```

- Listing 13-14 코드는 아무 일도 하지 않음(클로저 미호출) → 경고가 이유를 알려줌: 반복자 어댑터는 지연적이라 소비 과정이 필요
- 해결: `collect` 메서드로 반복자를 소비(12장에서 `env::args`와 함께 이미 사용) → 반복자를 소비해 컬렉션 데이터 타입으로 수집
- Listing 13-15: `map` 결과를 반복해 벡터로 수집, 각 항목이 1씩 증가된 값 포함

```rust
fn main() {
    let v1: Vec<i32> = vec![1, 2, 3];

    let v2: Vec<_> = v1.iter().map(|x| x + 1).collect();

    assert_eq!(v2, vec![2, 3, 4]);
}
```

*Listing 13-15: `map` 메서드를 호출하여 새 반복자를 생성한 다음 `collect` 메서드를 호출하여 새 반복자를 소비하고 벡터 생성하기*

- `map`이 클로저를 받으므로 각 항목에 원하는 모든 작업 지정 가능 → 클로저로 `Iterator`의 반복 동작을 재사용하면서 일부 동작만 사용자 정의하는 좋은 예
- 반복자 어댑터를 여러 번 연결해 복잡한 작업을 읽기 쉽게 표현 가능
- 단, 모든 반복자는 지연적 → 결과를 얻으려면 소비 어댑터 메서드 중 하나를 반드시 호출해야 함

### 환경을 캡처하는 클로저 사용하기

- 많은 반복자 어댑터가 클로저를 인수로 받으며, 보통 그 클로저는 환경을 캡처하는 클로저
- 예제: `filter` 메서드
  - 클로저는 반복자 항목을 받아 `bool` 반환
  - `true` → 값 포함, `false` → 값 제외
- Listing 13-16: `filter` + 환경의 `shoe_size` 변수를 캡처하는 클로저로 `Shoe` 인스턴스 컬렉션 반복 → 지정 크기 신발만 반환

```rust
#[derive(PartialEq, Debug)]
struct Shoe {
    size: u32,
    style: String,
}

fn shoes_in_size(shoes: Vec<Shoe>, shoe_size: u32) -> Vec<Shoe> {
    shoes.into_iter().filter(|s| s.size == shoe_size).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn filters_by_size() {
        let shoes = vec![
            Shoe {
                size: 10,
                style: String::from("스니커즈"),
            },
            Shoe {
                size: 13,
                style: String::from("샌들"),
            },
            Shoe {
                size: 10,
                style: String::from("부츠"),
            },
        ];

        let in_my_size = shoes_in_size(shoes, 10);

        assert_eq!(
            in_my_size,
            vec![
                Shoe {
                    size: 10,
                    style: String::from("스니커즈")
                },
                Shoe {
                    size: 10,
                    style: String::from("부츠")
                },
            ]
        );
    }
}
```

*Listing 13-16: `shoe_size`를 캡처하는 클로저와 함께 `filter` 메서드 사용하기*

- `shoes_in_size`: 신발 벡터 소유권 + 신발 크기를 매개변수로 받아, 지정 크기만 포함하는 벡터 반환
- 본문 동작
  - `into_iter` 호출 → 벡터 소유권을 갖는 반복자 생성
  - `filter` 호출 → 클로저가 `true`를 반환하는 요소만 남은 새 반복자로 변환
  - 클로저는 `shoe_size`를 캡처해 각 신발 크기와 비교
  - `collect` 호출 → 결과를 함수가 반환할 벡터로 수집
- 테스트 결과: `shoes_in_size` 호출 시 지정한 크기와 같은 신발만 반환됨을 확인

---

## 반복자를 사용하여 I/O 프로젝트 개선하기

> **원문:** https://doc.rust-lang.org/book/ch13-03-improving-our-io-project.html

- 반복자 지식을 활용해 12장 I/O 프로젝트의 코드를 더 명확·간결하게 개선 가능
- 살펴볼 대상: `Config::build` 함수, `search` 함수

### 반복자를 사용하여 `clone` 제거하기

- Listing 12-6에서 한 일: `String` 슬라이스를 받아 인덱싱·복제하여 `Config` 구조체가 값을 소유하게 함
- Listing 13-17: Listing 12-23의 `Config::build` 구현 재현

```rust
impl Config {
    pub fn build(args: &[String]) -> Result<Config, &'static str> {
        if args.len() < 3 {
            return Err("인수가 충분하지 않습니다");
        }

        let query = args[1].clone();
        let file_path = args[2].clone();

        let ignore_case = env::var("IGNORE_CASE").is_ok();

        Ok(Config {
            query,
            file_path,
            ignore_case,
        })
    }
}
```

*Listing 13-17: Listing 12-23의 `Config::build` 함수 재현*

- 당시 비효율적인 `clone` 호출을 나중에 제거하기로 미뤄둠 → 이제 제거할 차례
- `clone`이 필요했던 이유: 매개변수 `args`가 `String` 요소를 가진 슬라이스이지만 `build`가 `args`를 소유하지 않기 때문
  - `Config` 인스턴스가 값을 소유하려면 `query`·`file_path` 필드 값을 복제해야 했음
- 개선 방향: 슬라이스를 빌리는 대신 반복자의 소유권을 인수로 받도록 `build`를 변경
  - 슬라이스 길이 확인·인덱싱 코드 대신 반복자 기능 사용
  - 결과적으로 반복자가 값에 접근하므로 `Config::build`가 하는 일이 더 명확해짐
- `Config::build`가 반복자 소유권을 가져가고 빌린 인덱싱 작업을 쓰지 않으면 → `clone`·새 할당 없이 반복자의 `String` 값을 `Config`로 바로 이동 가능

#### 반환된 반복자 직접 사용하기

- I/O 프로젝트 *src/main.rs* 현재 상태:

```rust
fn main() {
    let args: Vec<String> = env::args().collect();

    let config = Config::build(&args).unwrap_or_else(|err| {
        eprintln!("인수 파싱 문제: {err}");
        process::exit(1);
    });

    // --snip--
}
```

- 먼저 Listing 12-24의 `main` 시작 부분을 Listing 13-18로 변경(반복자 사용) → `Config::build`도 업데이트할 때까지는 컴파일 안 됨

```rust
fn main() {
    let config = Config::build(env::args()).unwrap_or_else(|err| {
        eprintln!("인수 파싱 문제: {err}");
        process::exit(1);
    });

    // --snip--
}
```

*Listing 13-18: `Config::build`에 `env::args`의 반환 값 전달하기*

- `env::args` 함수는 반복자를 반환 → 벡터로 수집 후 슬라이스를 넘기던 방식 대신, 반복자 소유권을 `Config::build`에 직접 전달
- 다음 단계: `Config::build` 정의 업데이트(*src/lib.rs*) → Listing 13-19처럼 시그니처 변경(본문도 고쳐야 하므로 아직 컴파일 안 됨)

```rust
impl Config {
    pub fn build(
        mut args: impl Iterator<Item = String>,
    ) -> Result<Config, &'static str> {
        // --snip--
    }
}
```

*Listing 13-19: 반복자를 기대하도록 `Config::build`의 시그니처 업데이트하기*

- `env::args`의 표준 라이브러리 문서: 반환 타입은 `std::env::Args`, 이 타입이 `Iterator`를 구현하고 `String` 값을 반환
- `args` 매개변수 타입을 `&[String]`에서 트레이트 바운드 `impl Iterator<Item = String>`의 제네릭 타입으로 변경
  - `impl Trait` 구문(11장 "매개변수로서의 트레이트" 참고): `args`는 `Iterator`를 구현하고 `String` 항목을 반환하는 어떤 타입도 될 수 있음
- 소유권을 가져가 반복하며 `args`를 변형하므로 `mut` 키워드로 가변 매개변수 선언

#### 인덱싱 대신 `Iterator` 트레이트 메서드 사용하기

- 다음 단계: `Config::build` 본문 수정 → `args`가 `Iterator`를 구현하므로 `next` 호출 가능
- Listing 13-20: Listing 12-23 코드를 `next` 사용으로 업데이트

```rust
impl Config {
    pub fn build(
        mut args: impl Iterator<Item = String>,
    ) -> Result<Config, &'static str> {
        args.next();

        let query = match args.next() {
            Some(arg) => arg,
            None => return Err("쿼리 문자열을 받지 못했습니다"),
        };

        let file_path = match args.next() {
            Some(arg) => arg,
            None => return Err("파일 경로를 받지 못했습니다"),
        };

        let ignore_case = env::var("IGNORE_CASE").is_ok();

        Ok(Config {
            query,
            file_path,
            ignore_case,
        })
    }
}
```

*Listing 13-20: 반복자 메서드를 사용하도록 `Config::build`의 본문 변경하기*

- `env::args` 반환값의 첫 값 = 프로그램 이름 → 무시하려고 먼저 `next` 호출 후 반환값 사용 안 함
- 이어서 `next` 호출로 `query` 필드 값을 얻음
  - `Some` → `match`로 값 추출
  - `None` → 인수 부족을 의미하므로 `Err`로 조기 반환
- `file_path` 값도 동일한 방식으로 처리

### 반복자 어댑터로 코드 더 명확하게 만들기

- I/O 프로젝트의 `search` 함수도 반복자로 개선 가능
- Listing 13-21: Listing 12-19의 원본 구현 재현

```rust
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    let mut results = Vec::new();

    for line in contents.lines() {
        if line.contains(query) {
            results.push(line);
        }
    }

    results
}
```

*Listing 13-21: Listing 12-19의 `search` 함수 구현*

- 반복자 어댑터 메서드로 더 간결하게 작성 가능 → 가변 중간 `results` 벡터도 제거 가능
- 함수형 프로그래밍 스타일: 가변 상태를 최소화해 코드를 더 명확하게 함
  - 가변 상태 제거 → 향후 검색을 병렬로 수행하는 개선이 쉬워짐(`results`에 대한 동시 접근 관리가 불필요해지므로)
- Listing 13-22: 변경된 구현

```rust
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    contents
        .lines()
        .filter(|line| line.contains(query))
        .collect()
}
```

*Listing 13-22: `search` 함수 구현에서 반복자 어댑터 메서드 사용하기*

- `search`의 목적: `query`를 포함하는 `contents`의 모든 줄 반환
- Listing 13-16의 `filter` 예제와 유사하게, `line.contains(query)`가 `true`인 줄만 유지 → `collect`로 벡터에 수집(훨씬 간단해짐)
- `search_case_insensitive` 함수에도 동일한 방식 적용 가능

### 루프와 반복자 중 선택하기

- 다음 질문: Listing 13-21(원래 구현)과 Listing 13-22(반복자 버전) 중 어떤 스타일을 선택할지
- 대다수 Rust 프로그래머는 반복자 스타일 선호
  - 처음에는 익숙해지기 조금 어렵지만, 다양한 반복자 어댑터와 동작에 익숙해지면 더 이해하기 쉬워짐
  - 루핑·새 벡터 생성의 세부 사항을 만지는 대신, 코드가 루프의 고수준 목적에 집중
  - 공통적인 코드를 추상화한 결과 → 이 코드에 고유한 개념(반복자 각 요소가 통과해야 하는 필터링 조건)을 더 쉽게 파악 가능
- 남은 질문: 두 구현이 성능도 정말 동등한가 → 직관적으로는 저수준 루프가 더 빠를 것 같음 → 다음 절에서 성능 비교

---

## 성능 비교: 루프 vs. 반복자

> **원문:** https://doc.rust-lang.org/book/ch13-04-performance.html

- 루프 vs 반복자 선택 기준: 어떤 구현이 더 빠른가(`for` 루프 버전 vs 반복자 버전의 `search`)
- 벤치마크: 아서 코난 도일의 *셜록 홈즈의 모험* 전체를 `String`에 로드 → *the* 단어 검색

```text
test bench_search_for  ... bench:  19,620,300 ns/iter (+/- 915,700)
test bench_search_iter ... bench:  19,234,900 ns/iter (+/- 657,200)
```

- 결과: 반복자 버전이 약간 더 빠름
  - 벤치마크 코드 자체의 설명은 생략 — 목적은 "두 버전이 동등함을 증명"이 아니라 "성능 측면에서 어떻게 비교되는지에 대한 감각을 얻는 것"
- 더 포괄적인 벤치마크를 위해서는 다양한 크기·내용의 `contents`, 다른 단어·길이의 `query` 등 여러 변형 확인 필요
- 핵심: 반복자는 고수준 추상화이지만, 직접 작성한 저수준 코드와 거의 동일한 코드로 컴파일됨
  - 반복자 = Rust의 *제로 비용 추상화* 중 하나 → 추상화를 써도 추가 런타임 오버헤드가 없다는 의미
  - C++ 원설계자 비야네 스트롭스트룹이 "Foundations of C++"(2012)에서 정의한 *제로 오버헤드*와 유사

> 일반적으로 C++ 구현은 제로 오버헤드 원칙을 따릅니다: 사용하지 않는 것에 대해 비용을 지불하지 않습니다. 그리고 더 나아가: 사용하는 것에 대해 직접 더 잘 작성할 수 없습니다.

- 또 다른 예: 오디오 디코더에서 가져온 코드
  - 디코딩 알고리즘: 선형 예측 수학 연산으로 이전 샘플의 선형 함수를 기반으로 미래 값을 추정
  - 코드는 반복자 체인으로 범위 내 세 변수(`buffer` 데이터 슬라이스 · `coefficients` 12개 배열 · `qlp_shift` 시프트 양)를 연산
  - 예제 내 변수는 선언만 하고 값은 부여하지 않음(컨텍스트 밖에서 큰 의미는 없지만, Rust가 고수준 아이디어를 저수준 코드로 바꾸는 방법을 보여주는 간결한 예)

```rust
let buffer: &mut [i32];
let coefficients: [i64; 12];
let qlp_shift: i16;

for i in 12..buffer.len() {
    let prediction = coefficients.iter()
                                 .zip(&buffer[i - 12..i])
                                 .map(|(&c, &s)| c * s as i64)
                                 .sum::<i64>() >> qlp_shift;
    let delta = buffer[i];
    buffer[i] = prediction as i32 + delta;
}
```

- `prediction` 계산 흐름: `coefficients` 12개 값을 순회 → `zip`으로 `buffer`의 이전 12개 값과 쌍을 만듦 → 각 쌍을 곱함 → 결과를 모두 합산 → 합계를 `qlp_shift` 비트만큼 오른쪽 시프트
- 오디오 디코더 같은 애플리케이션은 성능을 최우선시하는 경우가 많음
  - 이 코드는 어댑터 두 개로 반복자를 만들고 값을 소비하는 구조
  - 실제 컴파일 결과: 직접 손으로 작성한 것과 동일한 어셈블리로 컴파일됨
  - `coefficients` 반복에 해당하는 루프 자체가 없음 → Rust가 반복 횟수(12)를 알고 있어 루프를 "언롤링"
    - *언롤링*: 루프 제어 코드 오버헤드를 없애고 각 반복에 대한 코드를 반복적으로 생성하는 최적화
- 추가 최적화 결과
  - 모든 coefficients가 레지스터에 저장 → 매우 빠른 접근
  - 런타임 배열 접근에 대한 경계 검사 없음
  - 결론: Rust가 적용 가능한 이런 최적화들이 결과 코드를 매우 효율적으로 만듦 → 반복자·클로저는 고수준으로 보이지만 런타임 성능 페널티 없이 두려움 없이 사용 가능

### 요약

- 클로저·반복자: 함수형 프로그래밍 아이디어에서 영감을 받은 Rust 기능
- 기여점: 저수준 성능으로 고수준 아이디어를 명확히 표현하는 Rust의 능력
- 구현 방식: 런타임 성능에 영향 없음 → 제로 비용 추상화를 지향하는 Rust 목표의 일부
- 다음: I/O 프로젝트 표현력을 개선했으니, 프로젝트를 세상과 공유하는 데 도움이 될 `cargo` 기능 몇 가지를 더 살펴봄

---

## Cargo와 Crates.io 더 알아보기

## Cargo와 Crates.io에 대해 더 알아보기

> **원문:** https://doc.rust-lang.org/book/ch14-00-more-about-cargo.html

- 지금까지는 Cargo의 가장 기본적인 기능(빌드·실행·테스트)만 사용 → Cargo는 훨씬 더 많은 것을 할 수 있음

다루는 내용:

- 릴리스 프로필을 통해 빌드 사용자 정의하기
- [crates.io](https://crates.io/)에 라이브러리 게시하기
- 워크스페이스로 대규모 프로젝트 구성하기
- [crates.io](https://crates.io/)에서 바이너리 설치하기
- 사용자 정의 명령을 사용하여 Cargo 확장하기

- Cargo는 이 장에서 다루는 것보다 훨씬 많은 기능을 제공 → 전체 설명은 [해당 문서](https://doc.rust-lang.org/cargo/) 참고

---

## 릴리스 프로필로 빌드 사용자 정의하기

> **원문:** https://doc.rust-lang.org/book/ch14-01-release-profiles.html

- *릴리스 프로필*: 코드 컴파일 옵션을 제어할 수 있도록 미리 정의된 사용자 정의 가능 프로필
  - 각 프로필은 다른 프로필과 독립적으로 구성됨
- Cargo의 두 가지 주요 프로필
  - `dev` 프로필: `cargo build` 실행 시 사용, 개발에 적합한 기본값
  - `release` 프로필: `cargo build --release` 실행 시 사용, 릴리스 빌드에 적합한 기본값

빌드 출력 예:

```console
$ cargo build
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
$ cargo build --release
    Finished `release` profile [optimized] target(s) in 0.32s
```

- `dev`·`release` = 컴파일러가 사용하는 서로 다른 프로필
- Cargo는 *Cargo.toml*에 `[profile.*]` 섹션이 명시되지 않으면 각 프로필의 기본 설정을 적용
  - `[profile.*]` 섹션을 추가하면 기본 설정의 일부(또는 전부)를 재정의
  - 예: `dev`·`release` 프로필의 `opt-level` 기본값

```toml
[profile.dev]
opt-level = 0

[profile.release]
opt-level = 3
```

- `opt-level`: Rust가 코드에 적용할 최적화 수(0~3)
  - 최적화를 많이 적용 → 컴파일 시간 증가
  - 개발 중 자주 컴파일하는 상황 → 결과 코드가 느려도 빠른 컴파일이 우선 → `dev` 기본값 `0`
  - 릴리스 준비 상황 → 컴파일에 시간을 더 투자하는 편이 유리(한 번 컴파일, 여러 번 실행하므로 더 긴 컴파일 시간을 더 빠른 실행 코드와 교환) → `release` 기본값 `3`
- *Cargo.toml*에 값을 추가해 기본 설정 재정의 가능 — 예: 개발 프로필에서 최적화 레벨 1 사용

```toml
[profile.dev]
opt-level = 1
```

- 위 코드는 기본값 `0`을 재정의 → `cargo build` 실행 시 `dev` 프로필 기본값 + `opt-level` 사용자 정의가 함께 적용
  - `opt-level = 1` → 기본값보다 최적화는 더 하지만 릴리스 빌드만큼은 아님
- 각 프로필의 구성 옵션·기본값 전체 목록은 [Cargo 문서](https://doc.rust-lang.org/cargo/reference/profiles.html) 참고

---

## Crates.io에 크레이트 게시하기

> **원문:** https://doc.rust-lang.org/book/ch14-02-publishing-to-crates-io.html

- 지금까지는 [crates.io](https://crates.io/)의 패키지를 의존성으로 사용 → 자신의 패키지도 게시해 다른 사람과 공유 가능
  - [crates.io](https://crates.io/) 크레이트 레지스트리는 패키지 소스 코드를 배포 → 주로 오픈 소스 코드 호스팅
- Rust·Cargo는 게시된 패키지를 더 쉽게 찾고 쓸 수 있게 해주는 기능 제공 → 몇 가지를 먼저 살펴본 뒤 게시 방법 설명

### 유용한 문서 주석 작성하기

- 패키지를 정확히 문서화하면 다른 사용자가 사용법·사용 시점을 파악 가능 → 문서 작성에 투자할 가치 있음
- 3장에서 다룬 두 슬래시 `//` 주석과 별개로, Rust에는 *문서 주석*이라는 특별한 주석이 존재
  - HTML 문서를 생성
  - 대상: 크레이트를 *구현*하는 방법이 아니라 *사용*하는 방법을 알고 싶은 프로그래머, 공개 API 항목 문서화용
- 문서 주석 형식
  - 슬래시 세 개 `///` 사용
  - Markdown 표기법으로 텍스트 서식 지정 가능
  - 문서화할 항목 바로 앞에 배치
- Listing 14-1: `my_crate` 크레이트의 `add_one` 함수 문서 주석

```rust
/// 주어진 숫자에 1을 더합니다.
///
/// # 예제
///
/// ```
/// let arg = 5;
/// let answer = my_crate::add_one(arg);
///
/// assert_eq!(6, answer);
/// ```
pub fn add_one(x: i32) -> i32 {
    x + 1
}
```

*Listing 14-1: 함수에 대한 문서 주석*

- 구성: `add_one`이 하는 일을 설명 → `예제` 제목 섹션 시작 → 사용법을 보여주는 코드 제공
- `cargo doc` 실행 → 문서 주석에서 HTML 문서 생성(`rustdoc` 도구가 실행되어 *target/doc*에 배치)
- `cargo doc --open` 실행 → 현재 크레이트(+의존성)의 HTML 문서를 빌드해 브라우저에서 바로 열어줌
  - `add_one` 함수로 이동하면 문서 주석 텍스트가 렌더링된 모습을 확인 가능

#### 자주 사용되는 섹션

- Listing 14-1의 `# 예제` Markdown 제목 → HTML에서 "예제" 섹션 생성
- 크레이트 작성자가 자주 쓰는 다른 섹션들
  - Panics: 문서화 대상 함수가 `panic!`할 수 있는 시나리오 설명 → 패닉을 원치 않는 호출자가 해당 상황을 피할 수 있게 함
  - Errors: 함수가 `Result`를 반환할 때 발생 가능한 오류 종류·조건 설명 → 호출자가 오류별로 다른 처리 코드를 작성하는 데 도움
  - Safety: 함수 호출이 `unsafe`한 경우(19장에서 상세) → 안전하지 않은 이유와 호출자가 지켜야 할 불변성 설명
- 대부분의 문서 주석에 이 섹션이 모두 필요하진 않지만, 호출자가 알아야 할 측면을 상기시키는 좋은 체크리스트 역할

#### 테스트로서의 문서 주석

- 문서 주석에 예제 코드 블록을 추가하면 라이브러리 사용법 설명에 도움 + 추가 보너스 존재
  - `cargo test` 실행 시 문서의 코드 예제가 테스트로 실행됨
  - 예제가 있는 문서는 유용하지만, 코드 변경 후 갱신되지 않아 작동하지 않는 예제는 오히려 해로움 → 이 자동 테스트가 그 문제를 방지
- Listing 14-1의 `add_one` 문서와 함께 `cargo test` 실행 결과:

```text
   Doc-tests my_crate

running 1 test
test src/lib.rs - add_one (line 5) ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.27s
```

- 함수나 예제를 바꿔 `assert_eq!`가 패닉하도록 만들고 `cargo test`를 다시 실행하면 → 문서 테스트가 예제와 코드의 불일치를 즉시 감지

#### 포함된 항목에 주석 달기

- `//!` 문서 주석 스타일: 주석 뒤에 오는 항목이 아니라 주석을 *포함하는* 항목에 문서를 붙임
  - 보통 크레이트 루트 파일(*src/lib.rs*) 또는 모듈 내부에서 전체 크레이트·모듈을 문서화할 때 사용
- 예: `add_one` 함수를 포함하는 `my_crate` 크레이트 목적 설명 → *src/lib.rs* 시작 부분에 `//!` 주석 추가(Listing 14-2)

```rust
//! # My Crate
//!
//! `my_crate`는 특정 계산을 더 편리하게 수행하기 위한
//! 유틸리티 모음입니다.

/// 주어진 숫자에 1을 더합니다.
// --snip--
```

*Listing 14-2: `my_crate` 크레이트 전체에 대한 문서*

- 주의: `//!` 마지막 줄 뒤에는 코드가 없음 → `///`가 아닌 `//!`로 시작했기 때문에 이 주석을 *포함하는* 항목(여기서는 크레이트 루트 *src/lib.rs* 파일)을 문서화
- `cargo doc --open` 실행 시 이 주석이 `my_crate` 문서 첫 페이지, 공개 항목 목록 위에 표시됨
- 항목 내부 문서 주석은 크레이트·모듈 설명에 특히 유용 → 컨테이너 전체 목적을 설명하고 사용자가 구성을 이해하도록 도움

### `pub use`로 편리한 공개 API 내보내기

- 공개 API 구조 = 크레이트 게시 시 주요 고려 사항
  - 크레이트 사용자는 작성자보다 구조에 덜 익숙함 → 모듈 계층이 크면 원하는 부분을 찾기 어려움
- 7장에서 다룬 것: `pub`로 항목 공개, `use`로 항목을 범위에 가져오기
  - 개발 중에는 합리적이던 구조가 사용자에게는 불편할 수 있음
  - 여러 레벨의 계층으로 구조체를 구성할 수 있지만, 깊은 위치의 타입을 찾기 어려워질 수 있음
  - 사용자가 `use my_crate::UsefulType;` 대신 `use my_crate::some_module::another_module::UsefulType;`를 입력해야 하는 불편도 발생
- 해결책: 내부 구성을 바꿀 필요 없이 `pub use`로 항목을 재내보내 비공개 구조와 다른 공개 구조를 만들 수 있음
  - 재내보내기: 한 위치의 공개 항목을 가져와 다른 위치에서 정의된 것처럼 공개
- 예: 예술적 개념을 모델링하는 `art` 라이브러리
  - `kinds` 모듈: `PrimaryColor`, `SecondaryColor` 열거형
  - `utils` 모듈: `mix` 함수
  - Listing 14-3

```rust
//! # Art
//!
//! 예술적 개념을 모델링하기 위한 라이브러리.

pub mod kinds {
    /// RYB 색상 모델에 따른 원색.
    pub enum PrimaryColor {
        Red,
        Yellow,
        Blue,
    }

    /// RYB 색상 모델에 따른 이차색.
    pub enum SecondaryColor {
        Orange,
        Green,
        Purple,
    }
}

pub mod utils {
    use crate::kinds::*;

    /// 두 원색을 동일한 양으로 혼합하여
    /// 이차색을 만듭니다.
    pub fn mix(c1: PrimaryColor, c2: PrimaryColor) -> SecondaryColor {
        // --snip--
    }
}
```

*Listing 14-3: `kinds` 및 `utils` 모듈로 구성된 항목이 있는 `art` 라이브러리*

- `cargo doc` 생성 문서 첫 페이지: `kinds`·`utils` 모듈만 나열, `PrimaryColor`·`SecondaryColor`·`mix`는 안 보임 → 모듈을 클릭해야 확인 가능
- 이 라이브러리에 의존하는 크레이트는 현재 모듈 구조대로 `use` 문을 작성해야 함(Listing 14-4)

```rust
use art::kinds::PrimaryColor;
use art::utils::mix;

fn main() {
    let red = PrimaryColor::Red;
    let yellow = PrimaryColor::Yellow;
    mix(red, yellow);
}
```

*Listing 14-4: 내부 구조가 내보낸 `art` 크레이트의 항목을 사용하는 크레이트*

- `art` 사용자는 `PrimaryColor`가 `kinds`에, `mix`가 `utils`에 있다는 것을 직접 알아내야 함
  - 이런 모듈 구조는 `art`를 작업하는 개발자에게는 의미 있지만, 사용하는 개발자에게는 유용한 정보를 담지 않음
  - 오히려 어디를 봐야 할지 알아내고 `use` 문에 모듈 이름까지 지정해야 하는 혼란만 유발
- 해결: Listing 14-3 코드에 `pub use`를 추가해 최상위 레벨에서 항목 재내보내기(Listing 14-5)

```rust
//! # Art
//!
//! 예술적 개념을 모델링하기 위한 라이브러리.

pub use self::kinds::PrimaryColor;
pub use self::kinds::SecondaryColor;
pub use self::utils::mix;

pub mod kinds {
    // --snip--
}

pub mod utils {
    // --snip--
}
```

*Listing 14-5: 항목을 재내보내기 위해 `pub use` 문 추가*

- `cargo doc`이 생성하는 API 문서: 첫 페이지에 재내보내기가 나열·링크됨 → `PrimaryColor`·`SecondaryColor`·`mix`를 더 쉽게 찾을 수 있음
- `art` 사용자는 Listing 14-4의 내부 구조 방식과 Listing 14-6의 더 편리한 방식을 모두 사용 가능

```rust
use art::mix;
use art::PrimaryColor;

fn main() {
    // --snip--
}
```

*Listing 14-6: `art` 크레이트의 재내보낸 항목을 사용하는 프로그램*

- 중첩 모듈이 많을 때 `pub use`로 최상위 레벨 재내보내기를 하면 사용자 경험이 크게 개선됨
- `pub use`의 또 다른 흔한 용도: 현재 크레이트가 의존하는 크레이트의 정의를 재내보내 자기 크레이트 공개 API의 일부로 만드는 것
- 좋은 공개 API 구조 만들기는 과학보다 예술에 가까움 → 반복 시행으로 사용자에게 가장 적합한 API를 찾아가야 함
  - `pub use`를 쓰면 내부 구조화 방식과 사용자에게 보여줄 구조를 분리할 수 있어 유연함
  - 참고로 설치한 크레이트들의 코드를 살펴보면 내부 구조와 공개 API가 다른 경우가 흔함

### Crates.io 계정 설정하기

- 패키지 게시 전 준비: [crates.io](https://crates.io/) 계정 생성 + API 토큰 발급
  - [crates.io](https://crates.io/) 홈페이지에서 GitHub 계정으로 로그인(현재는 GitHub 계정 필요, 향후 다른 방식도 지원될 수 있음)
  - 로그인 후 [https://crates.io/me/](https://crates.io/me/) 계정 설정에서 API 키 확인
  - API 키로 `cargo login` 명령 실행

```console
$ cargo login abcdefghijklmnopqrstuvwxyz012345
```

- 이 명령은 Cargo에 API 토큰을 알리고 로컬 *~/.cargo/credentials.toml*에 저장
- 토큰은 *비밀* → 다른 사람과 공유 금지
  - 실수로 공유했다면 즉시 철회하고 [crates.io](https://crates.io/)에서 새 토큰 발급 필요

### 새 크레이트에 메타데이터 추가하기

- 게시 전 준비: *Cargo.toml*의 `[package]` 섹션에 메타데이터 추가 필요
- 크레이트 이름
  - 고유해야 함
  - 로컬 작업 중에는 원하는 대로 지정 가능하지만, [crates.io](https://crates.io/) 이름은 선착순
  - 이미 사용된 이름은 다른 사람이 게시 불가 → 사이트에서 이름 검색으로 사용 여부 확인
  - *Cargo.toml* `[package]` 아래 이름 편집

```toml
[package]
name = "guessing_game"
```

- 고유한 이름을 선택했더라도 이 시점에서 `cargo publish` 실행 시 경고·오류 발생

```console
$ cargo publish
    Updating crates.io index
warning: manifest has no description, license, license-file, documentation, homepage or repository.
See https://doc.rust-lang.org/cargo/reference/manifest.html#package-metadata for more info.
--snip--
error: failed to publish to registry at https://crates.io

Caused by:
  the remote server responded with an error (status 400 Bad Request): missing or empty metadata fields: description, license. Please see https://doc.rust-lang.org/cargo/reference/manifest.html for more information on configuring these fields
```

- 오류 원인: 설명·라이선스 정보 누락 → 사용자가 크레이트 용도와 사용 조건을 알 수 있게 하기 위해 필수
  - *Cargo.toml*에 검색 결과에 표시될 한두 문장 설명 추가
  - `license` 필드: *라이선스 식별자 값* 필요([Linux Foundation SPDX][spdx]가 사용 가능한 식별자를 나열)
  - 예: MIT 라이선스 지정 시 `MIT` 식별자 사용

```toml
[package]
name = "guessing_game"
license = "MIT"
```

- SPDX에 없는 라이선스를 쓰려면: 라이선스 텍스트 파일을 프로젝트에 포함 → `license` 대신 `license-file`로 파일명 지정
- 어떤 라이선스가 적합한지는 이 문서 범위 밖
  - Rust 커뮤니티 다수가 `MIT OR Apache-2.0` 이중 라이선스를 Rust와 동일한 방식으로 사용
  - `OR`로 구분해 여러 라이선스 식별자를 지정하면 프로젝트가 다중 라이선스를 가질 수 있음을 보여주는 관행
- 이름·버전·설명·라이선스를 갖춘 게시 준비 완료 *Cargo.toml* 예:

```toml
[package]
name = "guessing_game"
version = "0.1.0"
edition = "2024"
description = "컴퓨터가 선택한 숫자를 추측하는 재미있는 게임."
license = "MIT OR Apache-2.0"

[dependencies]
```

- 그 외 지정 가능한 메타데이터는 [Cargo 문서](https://doc.rust-lang.org/cargo/) 참고

### Crates.io에 게시하기

- 준비 완료 상태(계정 생성, API 토큰 저장, 크레이트 이름 선택, 필수 메타데이터 지정) → 이제 게시 가능
  - 게시하면 특정 버전이 [crates.io](https://crates.io/)에 업로드되어 다른 사람이 사용 가능
- 주의: 게시는 *영구적*
  - 버전은 절대 덮어쓸 수 없고, 코드 삭제도 불가
  - [crates.io](https://crates.io/)의 주요 목표 중 하나: 코드의 영구 아카이브 역할 → 의존하는 모든 프로젝트의 빌드가 계속 작동하도록 보장
  - 버전 삭제를 허용하면 이 목표 달성이 불가능해짐
  - 단, 게시 가능한 버전 수 자체에는 제한 없음
- `cargo publish` 재실행 → 이번엔 성공

```console
$ cargo publish
    Updating crates.io index
   Packaging guessing_game v0.1.0 (file:///projects/guessing_game)
   Verifying guessing_game v0.1.0 (file:///projects/guessing_game)
   Compiling guessing_game v0.1.0
(file:///projects/guessing_game/target/package/guessing_game-0.1.0)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.19s
   Uploading guessing_game v0.1.0 (file:///projects/guessing_game)
    Uploaded guessing_game v0.1.0 to registry `crates-io`
note: waiting for `guessing_game v0.1.0` to be available at registry `crates-io`.
You may press ctrl-c to skip waiting; the crate should be available shortly.
   Published guessing_game v0.1.0 at registry `crates-io`
```

- 결과: Rust 커뮤니티와 코드 공유 완료 → 누구나 이 크레이트를 프로젝트 의존성으로 추가 가능

### 기존 크레이트의 새 버전 게시하기

- 크레이트 변경 후 새 버전 릴리스 절차
  - *Cargo.toml*의 `version` 값 변경
  - [시맨틱 버전 관리 규칙][semver]으로 변경 유형에 맞는 다음 버전 번호 결정
  - `cargo publish`로 새 버전 업로드

### `cargo yank`로 Crates.io에서 버전 폐기하기

- 이전 버전 크레이트는 삭제 불가하지만, 향후 프로젝트가 새 의존성으로 추가하는 것은 막을 수 있음
  - 크레이트 버전이 어떤 이유로 손상된 경우에 유용
  - Cargo는 이 상황을 위해 버전 *얀크*(폐기)를 지원
- 얀크의 효과
  - 새 프로젝트가 해당 버전에 의존하는 것은 방지
  - 이미 의존하는 기존 프로젝트는 계속 작동
  - 즉 *Cargo.lock*이 있는 기존 프로젝트는 중단되지 않고, 이후 생성되는 *Cargo.lock*은 얀크된 버전을 쓰지 않음
- 얀크 방법: 게시했던 크레이트 디렉토리에서 `cargo yank` 실행 + 버전 지정
  - 예: `guessing_game` 버전 1.0.1을 얀크

```console
$ cargo yank --vers 1.0.1
    Updating crates.io index
        Yank guessing_game@1.0.1
```

- `--undo` 플래그로 얀크 취소 → 프로젝트가 다시 그 버전에 의존 가능

```console
$ cargo yank --vers 1.0.1 --undo
    Updating crates.io index
      Unyank guessing_game@1.0.1
```

- 얀크는 코드를 삭제하지 *않음* → 실수로 업로드한 비밀은 얀크로 지울 수 없음
  - 그런 일이 발생했다면 해당 비밀을 즉시 재설정 필요

[spdx]: https://spdx.org/licenses/
[semver]: https://semver.org/

---

## Cargo 워크스페이스

> **원문:** https://doc.rust-lang.org/book/ch14-03-cargo-workspaces.html

- 12장에서 바이너리 크레이트·라이브러리 크레이트를 포함한 패키지를 빌드
  - 프로젝트가 커지면 라이브러리 크레이트를 여러 개로 분할하고 싶어질 수 있음
  - Cargo *워크스페이스*: 함께 개발되는 여러 관련 패키지를 관리하는 기능

### 워크스페이스 생성하기

- *워크스페이스*: 동일한 *Cargo.lock*·출력 디렉토리를 공유하는 패키지 집합
- 예제 구성(워크스페이스 구조에 집중하기 위해 코드는 단순화)
  - 바이너리 하나 + 라이브러리 두 개
  - 바이너리는 주요 기능 제공, 두 라이브러리에 의존
  - 라이브러리 1: `add_one` 함수 제공
  - 라이브러리 2: `add_two` 함수 제공
  - 세 크레이트 모두 동일한 워크스페이스에 속함
- 워크스페이스용 디렉토리 생성

```console
$ mkdir add
$ cd add
```

- *add* 디렉토리에 전체 워크스페이스를 구성하는 *Cargo.toml* 생성
  - 다른 *Cargo.toml*과 달리 `[package]` 섹션 없음 → `[workspace]` 섹션으로 시작

```toml
[workspace]
resolver = "3"
```

- *add* 디렉토리 안에서 `cargo new`로 `adder` 바이너리 크레이트 생성

```console
$ cargo new adder
     Created binary (application) `adder` package
      Adding `adder` as member of workspace at `/projects/add`
```

- 이 시점에서 `cargo build`로 워크스페이스 빌드 가능 → *add* 디렉토리 구조:

```text
├── Cargo.lock
├── Cargo.toml
├── adder
│   ├── Cargo.toml
│   └── src
│       └── main.rs
└── target
```

- 워크스페이스에는 컴파일 아티팩트가 놓일 최상위 *target* 디렉토리가 하나만 존재
  - `adder` 패키지는 자체 *target* 디렉토리를 갖지 않음
  - *adder* 디렉토리 내부에서 `cargo build`를 실행해도 아티팩트는 *add/adder/target*이 아닌 *add/target*에 저장됨
  - 이유: 워크스페이스의 크레이트들이 서로 의존하기 때문 → 각 크레이트가 자체 *target*을 가지면 다른 크레이트를 매번 다시 컴파일해야 함
  - 하나의 *target* 디렉토리 공유 → 불필요한 재빌드 방지

### 워크스페이스에 두 번째 패키지 생성하기

- 워크스페이스에 `add_one` 멤버 패키지 추가
  - 최상위 *Cargo.toml*의 `members` 목록에 *add_one* 경로 지정

```toml
[workspace]
resolver = "3"
members = ["adder", "add_one"]
```

- `add_one` 라이브러리 크레이트 생성

```console
$ cargo new add_one --lib
     Created library `add_one` package
      Adding `add_one` as member of workspace at `/projects/add`
```

- *add* 디렉토리 구조:

```text
├── Cargo.lock
├── Cargo.toml
├── add_one
│   ├── Cargo.toml
│   └── src
│       └── lib.rs
├── adder
│   ├── Cargo.toml
│   └── src
│       └── main.rs
└── target
```

- *add_one/src/lib.rs*에 `add_one` 함수 추가

```rust
pub fn add_one(x: i32) -> i32 {
    x + 1
}
```

- `adder` 패키지(바이너리)가 `add_one` 패키지(라이브러리)에 의존하도록 설정
  - *adder/Cargo.toml*에 `add_one` 경로 의존성 추가

```toml
[dependencies]
add_one = { path = "../add_one" }
```

- 참고: Cargo는 워크스페이스 크레이트가 서로 의존한다고 자동으로 가정하지 않음 → 의존성 관계를 명시적으로 지정해야 함
- `adder` 크레이트에서 `add_one` 함수 사용
  - *adder/src/main.rs* 맨 위에 `use` 추가
  - `main` 함수에서 `add_one` 호출

```rust
use add_one;

fn main() {
    let num = 10;
    println!("Hello, world! {num} 더하기 1은 {}!", add_one::add_one(num));
}
```

- 최상위 *add* 디렉토리에서 `cargo build`로 워크스페이스 빌드

```console
$ cargo build
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.22s
```

- *add* 디렉토리에서 바이너리 크레이트 실행: `-p` 인수 + 패키지 이름을 `cargo run`과 함께 사용

```console
$ cargo run -p adder
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
     Running `target/debug/adder`
Hello, world! 10 더하기 1은 11!
```

- 결과: *adder/src/main.rs* 코드가 실행되고 `add_one` 크레이트에 의존

#### 워크스페이스에서 외부 패키지에 의존하기

- 워크스페이스에는 각 크레이트 디렉토리의 개별 *Cargo.toml*과 별개로, 최상위 레벨에 *Cargo.lock*이 하나만 존재
  - 이로써 워크스페이스의 모든 크레이트가 모든 의존성의 동일 버전 사용
  - 예: `rand`를 *adder/Cargo.toml*·*add_one/Cargo.toml* 둘 다에 추가 → Cargo가 하나의 `rand` 버전으로 해결해 하나의 *Cargo.lock*에 기록
  - 효과: 워크스페이스 크레이트들이 항상 서로 호환됨
- `add_one`에서 `rand` 사용을 위해 `[dependencies]`에 추가

```toml
[dependencies]
rand = "0.8.5"
```

- *add_one/src/lib.rs*에 `use rand;` 추가 → *add* 디렉토리에서 `cargo build` 실행 시 `rand`를 가져와 컴파일
  - 단, 범위로 가져온 `rand`를 참조하지 않으면 경고 발생

```console
$ cargo build
    Updating crates.io index
  Downloaded rand v0.8.5
   --snip--
   Compiling rand v0.8.5
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
warning: unused import: `rand`
 --> add_one/src/lib.rs:1:5
  |
1 | use rand;
  |     ^^^^
  |
  = note: `#[warn(unused_imports)]` on by default

warning: `add_one` (lib) generated 1 warning
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 10.18s
```

- 최상위 *Cargo.lock*에는 `add_one`의 `rand` 의존성 정보가 포함됨
- 주의: `rand`가 워크스페이스 어딘가에서 쓰이더라도, 자체 *Cargo.toml*에 추가하지 않은 다른 크레이트에서는 사용 불가
  - 예: `adder` 패키지의 *adder/src/main.rs*에 `use rand;`를 추가하면 오류 발생

```console
$ cargo build
  --snip--
   Compiling adder v0.1.0 (file:///projects/add/adder)
error[E0432]: unresolved import `rand`
 --> adder/src/main.rs:2:5
  |
2 | use rand;
  |     ^^^^ no external crate `rand`
```

- 해결: `adder` 패키지의 *Cargo.toml*에도 `rand`를 의존성으로 명시
  - 빌드 시 *Cargo.lock*의 `adder` 의존성 목록에 `rand` 추가, 단 `rand`의 추가 복사본은 다운로드되지 않음
  - Cargo는 워크스페이스 내 모든 패키지가 동일 버전의 `rand`를 쓰도록 보장 → 공간 절약 + 상호 호환성 유지

#### 워크스페이스에 테스트 추가하기

- `add_one` 크레이트 내부에 `add_one::add_one` 함수 테스트 추가

```rust
pub fn add_one(x: i32) -> i32 {
    x + 1
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        assert_eq!(3, add_one(2));
    }
}
```

- 최상위 *add* 디렉토리에서 `cargo test` 실행 → 워크스페이스의 모든 크레이트 테스트가 함께 실행됨

```console
$ cargo test
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `test` profile [unoptimized + debuginfo] target(s) in 0.27s
     Running unittests src/lib.rs (target/debug/deps/add_one-f0253159197f7841)

running 1 test
test tests::it_works ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

     Running unittests src/main.rs (target/debug/deps/adder-49979ff40686fa8e)

running 0 tests

test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

   Doc-tests add_one

running 0 tests

test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
```

- 출력 해석
  - 첫 섹션: `add_one` 크레이트의 `it_works` 테스트 통과
  - 다음 섹션: `adder` 크레이트에는 테스트 0개
  - 마지막 섹션: `add_one` 크레이트의 문서 테스트 0개
- `-p` 플래그 + 크레이트 이름 지정으로 특정 크레이트 테스트만 실행 가능

```console
$ cargo test -p add_one
    Finished `test` profile [unoptimized + debuginfo] target(s) in 0.00s
     Running unittests src/lib.rs (target/debug/deps/add_one-b3235fea9a156f74)

running 1 test
test tests::it_works ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s

   Doc-tests add_one

running 0 tests

test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.00s
```

- 결과: `add_one` 크레이트 테스트만 실행, `adder` 테스트는 실행되지 않음
- 워크스페이스 크레이트를 [crates.io](https://crates.io/)에 게시할 때도 각 크레이트를 별도로 게시해야 함
  - `cargo test`처럼 `-p` 플래그 + 크레이트 이름으로 특정 크레이트만 게시 가능
- 추가 연습: `add_one` 크레이트와 유사한 방식으로 `add_two` 크레이트를 워크스페이스에 추가해 볼 것
- 정리: 프로젝트가 성장하면 워크스페이스 고려 필요
  - 하나의 큰 코드 덩어리보다 작은 개별 구성 요소가 이해하기 쉬움
  - 자주 함께 변경되는 크레이트를 워크스페이스로 유지하면 조정도 쉬워짐

---

## `cargo install`로 Crates.io에서 바이너리 설치하기

> **원문:** https://doc.rust-lang.org/book/ch14-04-installing-binaries.html

- `cargo install`: 바이너리 크레이트를 로컬에 설치·사용하는 명령
  - 시스템 패키지 대체 목적이 아님 → Rust 개발자가 [crates.io](https://crates.io/)에서 공유된 도구를 편리하게 설치하는 방법
  - 바이너리 대상이 있는 패키지만 설치 가능
  - *바이너리 대상*: 크레이트에 *src/main.rs* 또는 바이너리로 지정된 파일이 있을 때 생성되는 실행 가능한 프로그램(자체 실행이 불가한 라이브러리 대상과 대조됨)
  - 보통 *README*에 라이브러리/바이너리 대상/둘 다 여부 정보가 있음
- 설치 위치: `cargo install`로 설치한 모든 바이너리는 설치 루트의 *bin* 폴더에 저장
  - *rustup.rs*로 설치했고 사용자 정의 구성이 없다면 `$HOME/.cargo/bin`
  - 실행하려면 이 디렉토리가 `$PATH`에 있어야 함
- 예: 12장에서 언급한 `grep`의 Rust 구현 `ripgrep` 설치

```console
$ cargo install ripgrep
    Updating crates.io index
  Downloaded ripgrep v14.1.1
  Downloaded 1 crate (213.6 KB) in 0.40s
  Installing ripgrep v14.1.1
--snip--
   Compiling ripgrep v14.1.1
    Finished `release` profile [optimized + debuginfo] target(s) in 6.73s
  Installing ~/.cargo/bin/rg
   Installed package `ripgrep v14.1.1` (executable `rg`)
```

- 출력 마지막에서 두 번째 줄: 설치된 바이너리 위치·이름(`ripgrep`의 경우 `rg`)
  - 설치 디렉토리가 `$PATH`에 있으면 `rg --help` 실행으로 더 빠른 Rust 파일 검색 도구 사용 가능

---

## 사용자 정의 명령으로 Cargo 확장하기

> **원문:** https://doc.rust-lang.org/book/ch14-05-extending-cargo.html

- Cargo는 수정 없이 새 하위 명령으로 확장 가능하도록 설계됨
  - `$PATH`에 `cargo-something`이라는 이름의 바이너리가 있으면 `cargo something`으로 실행 가능 → 마치 내장 Cargo 하위 명령처럼 동작
  - 이런 사용자 정의 명령은 `cargo --list` 실행 시에도 함께 나열됨
  - `cargo install`로 확장을 설치한 뒤 내장 도구처럼 실행 가능 → Cargo 설계의 편리한 이점

### 요약

- Cargo·[crates.io](https://crates.io/)로 코드를 공유하는 것 = Rust 생태계를 다양한 작업에 유용하게 만드는 핵심 요소
- Rust 표준 라이브러리는 작고 안정적이지만, 크레이트는 언어와 다른 타임라인으로 공유·사용·개선하기 쉬움
- 결론: [crates.io](https://crates.io/)에서 유용한 코드를 공유하는 데 부끄러워할 필요 없음 → 다른 누군가에게도 유용할 가능성이 높음
