# 러스트 함수형 언어의 특징과 Cargo

## 함수형 언어의 특징: 이터레이터와 클로저

## 함수형 언어 기능: 반복자와 클로저

> **원문:** https://doc.rust-lang.org/book/ch13-00-functional-features.html

Rust의 설계는 기존의 많은 언어와 기술로부터 영감을 받았으며, 그중 중요한 영향 중 하나는 *함수형 프로그래밍*입니다. 함수형 스타일의 프로그래밍은 종종 함수를 값으로 사용하는 것을 포함하며, 이는 함수를 인수로 전달하거나 다른 함수에서 반환하거나 나중에 실행하기 위해 변수에 할당하는 것을 의미합니다.

이 장에서는 함수형 프로그래밍이 무엇인지 또는 무엇이 아닌지에 대한 논쟁을 다루지 않고, 대신 많은 언어에서 종종 함수형이라고 불리는 기능들과 유사한 Rust의 일부 기능에 대해 논의할 것입니다.

더 구체적으로, 다음 내용을 다룹니다:

- *클로저*: 변수에 저장할 수 있는 함수와 유사한 구조
- *반복자*: 일련의 요소를 처리하는 방법
- 12장의 I/O 프로젝트를 클로저와 반복자를 사용하여 개선하는 방법
- 클로저와 반복자의 성능 (스포일러 경고: 생각보다 빠릅니다!)

패턴 매칭과 열거형과 같은 다른 Rust 기능들도 함수형 스타일의 영향을 받았으며, 이미 다른 장에서 다루었습니다. 클로저와 반복자를 마스터하는 것은 관용적이고 빠른 Rust 코드를 작성하는 데 중요한 부분이므로, 이 장 전체를 이 주제에 할애할 것입니다.

---

## 클로저: 환경을 캡처하는 익명 함수

> **원문:** https://doc.rust-lang.org/book/ch13-01-closures.html

Rust의 클로저는 변수에 저장하거나 다른 함수에 인수로 전달할 수 있는 익명 함수입니다. 한 곳에서 클로저를 만들고 다른 컨텍스트에서 호출하여 평가할 수 있습니다. 일반 함수와 달리, 클로저는 정의된 스코프의 값을 캡처할 수 있습니다. 이러한 클로저 기능이 어떻게 코드 재사용과 동작 사용자 정의를 가능하게 하는지 보여드리겠습니다.

### 클로저로 환경 캡처하기

먼저 클로저를 사용하여 정의된 환경의 값을 나중에 사용하기 위해 캡처하는 방법을 살펴보겠습니다. 다음은 시나리오입니다: 때때로 우리 티셔츠 회사는 마케팅 프로모션의 일환으로 메일링 리스트의 누군가에게 독점 한정판 셔츠를 무료로 증정합니다. 메일링 리스트의 사람들은 선택적으로 프로필에 좋아하는 색상을 추가할 수 있습니다. 무료 셔츠를 받도록 선택된 사람이 좋아하는 색상을 설정해 두었다면 그 색상의 셔츠를 받습니다. 좋아하는 색상을 지정하지 않은 경우 회사에 현재 가장 많이 있는 색상을 받습니다.

이를 구현하는 방법은 여러 가지가 있습니다. 이 예제에서는 `Red`와 `Blue` 변형을 가진 `ShirtColor`라는 열거형을 사용하겠습니다(단순화를 위해 사용 가능한 색상 수를 제한합니다). 회사의 재고는 `shirts`라는 필드가 있는 `Inventory` 구조체로 표현하며, 이 필드는 현재 재고에 있는 셔츠 색상을 나타내는 `Vec<ShirtColor>`를 포함합니다. `Inventory`에 정의된 `giveaway` 메서드는 무료 셔츠 당첨자의 선택적 셔츠 색상 선호도를 가져와 그 사람이 받게 될 셔츠 색상을 반환합니다. 이 설정은 Listing 13-1에 나와 있습니다:

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

`main`에 정의된 `store`에는 이 한정판 프로모션용으로 파란색 셔츠 두 벌과 빨간색 셔츠 한 벌이 남아 있습니다. 빨간색 셔츠를 선호하는 사용자와 선호도가 없는 사용자에 대해 `giveaway` 메서드를 호출합니다.

다시 말하지만, 이 코드는 여러 가지 방법으로 구현할 수 있으며, 여기서는 클로저에 초점을 맞추기 위해 이미 배운 개념만 사용했습니다. 단, `giveaway` 메서드의 본문은 클로저를 사용합니다. `giveaway` 메서드에서 `Option<ShirtColor>` 타입의 매개변수 `user_preference`로 사용자 선호도를 받아 `user_preference`에 대해 `unwrap_or_else` 메서드를 호출합니다. `Option<T>`의 [`unwrap_or_else` 메서드][unwrap-or-else]는 표준 라이브러리에서 정의됩니다. 이 메서드는 하나의 인수를 받습니다: 인수가 없고 `T` 값을 반환하는 클로저입니다(`Option<T>`의 `Some` 변형에 저장된 것과 동일한 타입, 이 경우 `ShirtColor`). `Option<T>`가 `Some` 변형이면 `unwrap_or_else`는 `Some` 내부의 값을 반환합니다. `Option<T>`가 `None` 변형이면 `unwrap_or_else`는 클로저를 호출하고 클로저가 반환한 값을 반환합니다.

`unwrap_or_else`에 대한 인수로 클로저 표현식 `|| self.most_stocked()`를 지정합니다. 이것은 매개변수가 없는 클로저입니다(클로저에 매개변수가 있다면 두 수직 막대 사이에 나타납니다). 클로저의 본문은 `self.most_stocked()`를 호출합니다. 여기서 클로저를 정의하고, `unwrap_or_else`의 구현이 결과가 필요할 때 나중에 클로저를 평가합니다.

이 코드를 실행하면 다음이 출력됩니다:

```text
선호도가 Some(Red)인 사용자가 Red를 받습니다
선호도가 None인 사용자가 Blue를 받습니다
```

여기서 흥미로운 측면은 현재 `Inventory` 인스턴스에서 `self.most_stocked()`를 호출하는 클로저를 전달했다는 것입니다. 표준 라이브러리는 우리가 정의하는 `Inventory`나 `ShirtColor` 타입, 또는 이 시나리오에서 사용하려는 로직에 대해 아무것도 알 필요가 없었습니다. 클로저는 `self` `Inventory` 인스턴스에 대한 불변 참조를 캡처하고 우리가 지정한 코드와 함께 `unwrap_or_else` 메서드에 전달합니다. 반면에 함수는 이런 방식으로 환경을 캡처할 수 없습니다.

### 클로저 타입 추론과 어노테이션

함수와 클로저 사이에는 더 많은 차이점이 있습니다. 클로저는 일반적으로 `fn` 함수처럼 매개변수나 반환 값의 타입을 명시적으로 작성할 필요가 없습니다. 함수에 타입 어노테이션이 필요한 이유는 타입이 사용자에게 노출되는 명시적 인터페이스의 일부이기 때문입니다. 이 인터페이스를 엄격하게 정의하는 것은 함수가 사용하고 반환하는 값의 타입에 대해 모두가 동의하도록 하는 데 중요합니다. 반면에 클로저는 이렇게 노출된 인터페이스에서 사용되지 않습니다: 변수에 저장되고 이름을 지정하지 않고 라이브러리 사용자에게 노출하지 않고 사용됩니다.

클로저는 일반적으로 짧으며 임의의 시나리오가 아닌 좁은 컨텍스트 내에서만 관련이 있습니다. 이러한 제한된 컨텍스트 내에서 컴파일러는 대부분의 변수 타입을 추론할 수 있는 방식과 유사하게 매개변수와 반환 타입의 타입을 추론할 수 있습니다(컴파일러가 클로저 타입 어노테이션도 필요로 하는 드문 경우도 있습니다).

변수와 마찬가지로, 엄격히 필요한 것보다 더 장황해지는 비용을 감수하더라도 명시성과 명확성을 높이고 싶다면 타입 어노테이션을 추가할 수 있습니다. 클로저에 타입을 어노테이션하는 것은 Listing 13-2에 표시된 정의처럼 보일 것입니다. 이 예제에서는 Listing 13-1에서처럼 인수로 전달하는 곳에서 클로저를 정의하는 대신 클로저를 정의하고 변수에 저장합니다.

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

타입 어노테이션을 추가하면 클로저의 구문이 함수의 구문과 더 유사해집니다. 여기서는 매개변수에 1을 더하는 함수와 동일한 동작을 하는 클로저의 구문을 비교합니다. 관련 부분을 정렬하기 위해 약간의 공백을 추가했습니다. 이것은 파이프 사용과 선택적 구문의 양을 제외하고 클로저 구문이 함수 구문과 얼마나 유사한지 보여줍니다:

```rust
fn  add_one_v1   (x: u32) -> u32 { x + 1 }
let add_one_v2 = |x: u32| -> u32 { x + 1 };
let add_one_v3 = |x|             { x + 1 };
let add_one_v4 = |x|               x + 1  ;
```

첫 번째 줄은 함수 정의를 보여주고, 두 번째 줄은 완전히 어노테이션된 클로저 정의를 보여줍니다. 세 번째 줄에서는 클로저 정의에서 타입 어노테이션을 제거합니다. 네 번째 줄에서는 클로저 본문이 단일 표현식이기 때문에 선택 사항인 중괄호를 제거합니다. 이들은 모두 호출될 때 동일한 동작을 생성하는 유효한 정의입니다. `add_one_v3`와 `add_one_v4` 줄은 컴파일러가 사용법에서 타입을 추론할 수 있도록 클로저가 평가되어야 합니다.

클로저 정의는 각 매개변수와 반환 값에 대해 하나의 구체적인 타입이 추론됩니다. 예를 들어, Listing 13-3은 매개변수로 받은 값을 그대로 반환하는 짧은 클로저의 정의를 보여줍니다. 이 클로저는 이 예제의 목적 외에는 그다지 유용하지 않습니다. 정의에 타입 어노테이션을 추가하지 않았다는 점에 유의하세요. 클로저에 타입 어노테이션이 없기 때문에 먼저 `String`으로 호출하고 그 다음 `i32`로 호출한 것처럼 모든 타입으로 클로저를 호출할 수 있습니다. 그러나 이 코드는 컴파일되지 않습니다.

```rust
fn main() {
    let example_closure = |x| x;

    let s = example_closure(String::from("hello"));
    let n = example_closure(5);
}
```

*Listing 13-3: 두 가지 다른 타입으로 타입이 추론된 클로저를 호출하려고 시도*

컴파일러가 다음 오류를 표시합니다:

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

`String` 값으로 `example_closure`를 처음 호출하면 컴파일러는 `x`의 타입과 클로저의 반환 타입을 `String`으로 추론합니다. 그런 다음 이러한 타입은 `example_closure`의 클로저에 고정되고, 동일한 클로저에서 다른 타입을 사용하려고 하면 타입 오류가 발생합니다.

### 참조 캡처 또는 소유권 이동

클로저는 세 가지 방법으로 환경에서 값을 캡처할 수 있으며, 이는 함수가 매개변수를 받는 세 가지 방법에 직접적으로 매핑됩니다: 불변으로 빌리기, 가변으로 빌리기, 소유권 가져오기. 클로저는 캡처된 값으로 함수 본문이 수행하는 작업에 따라 이들 중 어떤 것을 사용할지 결정합니다.

Listing 13-4에서 `list`라는 벡터에 대한 불변 참조를 캡처하는 클로저를 정의합니다. 값을 출력하는 데 불변 참조만 필요하기 때문입니다:

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

이 예제는 또한 변수가 클로저 정의에 바인딩될 수 있고, 나중에 변수 이름과 괄호를 마치 변수 이름이 함수 이름인 것처럼 사용하여 클로저를 호출할 수 있음을 보여줍니다.

동시에 `list`에 대한 여러 불변 참조를 가질 수 있기 때문에, `list`는 클로저 정의 전, 클로저 정의 후 클로저 호출 전, 클로저 호출 후의 코드에서 여전히 접근 가능합니다. 이 코드는 컴파일되고 실행되며 다음을 출력합니다:

```text
클로저 정의 전: [1, 2, 3]
클로저 호출 전: [1, 2, 3]
클로저에서: [1, 2, 3]
클로저 호출 후: [1, 2, 3]
```

다음으로, Listing 13-5에서 클로저 본문을 변경하여 `list` 벡터에 요소를 추가합니다. 클로저는 이제 가변 참조를 캡처합니다:

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

이 코드는 컴파일되고 실행되며 다음을 출력합니다:

```text
클로저 정의 전: [1, 2, 3]
클로저 호출 후: [1, 2, 3, 7]
```

`borrows_mutably` 클로저가 정의되었을 때 `list`에 대한 가변 빌림이 끝나는 정의와 호출 사이에 `println!`이 없다는 점에 유의하세요. 클로저 정의와 클로저 호출 사이에는 출력을 위한 불변 빌림이 허용되지 않습니다. 가변 빌림이 있을 때는 다른 빌림이 허용되지 않기 때문입니다. 거기에 `println!`을 추가해 보고 어떤 오류 메시지가 나오는지 확인해 보세요!

클로저 본문이 엄격하게 소유권을 필요로 하지 않더라도 클로저가 환경에서 사용하는 값의 소유권을 갖도록 강제하려면 매개변수 목록 앞에 `move` 키워드를 사용할 수 있습니다.

이 기술은 새 스레드에 데이터를 이동시켜 새 스레드가 데이터를 소유하도록 클로저를 새 스레드에 전달할 때 주로 유용합니다. 스레드와 왜 사용하고 싶은지에 대해서는 16장에서 동시성에 대해 이야기할 때 자세히 논의하겠지만, 지금은 `move` 키워드가 필요한 클로저를 사용하여 새 스레드를 생성하는 것을 간단히 살펴보겠습니다. Listing 13-6은 메인 스레드가 아닌 새 스레드에서 벡터를 출력하도록 Listing 13-4를 수정한 것입니다:

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

새 스레드를 생성하고 스레드에 클로저를 인수로 전달하여 실행합니다. 클로저 본문은 목록을 출력합니다. Listing 13-4에서 클로저는 불변 참조만으로 `list`를 캡처했는데, 이것이 `list`를 출력하는 데 필요한 최소한의 접근이기 때문입니다. 이 예제에서는 클로저 본문이 여전히 불변 참조만 필요하더라도 클로저 정의 시작 부분에 `move` 키워드를 넣어 `list`가 이동되어야 함을 지정해야 합니다. 새 스레드는 나머지 메인 스레드가 완료되기 전에 끝나거나 메인 스레드가 먼저 끝날 수 있습니다. 메인 스레드가 `list`의 소유권을 유지하지만 새 스레드가 끝나기 전에 끝나서 `list`를 드롭하면 스레드의 불변 참조가 유효하지 않게 됩니다. 따라서 컴파일러는 참조가 유효하도록 `list`가 새 스레드에 주어진 클로저로 이동되어야 한다고 요구합니다. `move` 키워드를 제거하거나 클로저가 정의된 후 메인 스레드에서 `list`를 사용해 보고 어떤 컴파일러 오류가 발생하는지 확인해 보세요!

### 캡처된 값을 클로저 밖으로 이동하기와 `Fn` 트레이트

클로저가 자신이 정의된 환경에서 값에 대한 참조나 소유권을 캡처하면(따라서 클로저 *안*으로 이동되는 것에 영향을 미침), 클로저 본문의 코드는 클로저가 나중에 평가될 때 참조나 값에 어떤 일이 발생하는지 정의합니다(따라서 클로저 *밖*으로 이동되는 것에 영향을 미침). 클로저 본문은 다음 중 하나를 수행할 수 있습니다: 캡처된 값을 클로저 밖으로 이동, 캡처된 값 변형, 값을 이동하지도 변형하지도 않음, 또는 처음부터 환경에서 아무것도 캡처하지 않음.

클로저가 환경에서 값을 캡처하고 처리하는 방식은 클로저가 구현하는 트레이트에 영향을 미치며, 트레이트는 함수와 구조체가 사용할 수 있는 클로저의 종류를 지정하는 방법입니다. 클로저는 클로저 본문이 값을 처리하는 방식에 따라 덧붙여지는 방식으로 이러한 `Fn` 트레이트 중 하나, 둘 또는 세 가지 모두를 자동으로 구현합니다:

1. `FnOnce`는 한 번 호출할 수 있는 클로저에 적용됩니다. 모든 클로저는 최소한 이 트레이트를 구현합니다. 왜냐하면 모든 클로저를 호출할 수 있기 때문입니다. 캡처된 값을 본문 밖으로 이동하는 클로저는 `FnOnce`만 구현하고 다른 `Fn` 트레이트는 구현하지 않습니다. 왜냐하면 한 번만 호출할 수 있기 때문입니다.
2. `FnMut`는 캡처된 값을 본문 밖으로 이동하지 않지만 캡처된 값을 변형할 수 있는 클로저에 적용됩니다. 이러한 클로저는 두 번 이상 호출할 수 있습니다.
3. `Fn`은 캡처된 값을 본문 밖으로 이동하지 않고 캡처된 값을 변형하지 않는 클로저와 환경에서 아무것도 캡처하지 않는 클로저에 적용됩니다. 이러한 클로저는 환경을 변형하지 않고 두 번 이상 호출할 수 있으며, 이는 클로저를 동시에 여러 번 호출하는 경우와 같은 케이스에서 중요합니다.

`Option<T>`에 대한 `unwrap_or_else` 메서드의 정의를 살펴보겠습니다:

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

`T`는 `Option`의 `Some` 변형에 있는 값의 타입을 나타내는 제네릭 타입임을 기억하세요. 그 타입 `T`는 `unwrap_or_else` 함수의 반환 타입이기도 합니다: 예를 들어 `Option<String>`에 대해 `unwrap_or_else`를 호출하면 `String`을 얻습니다.

다음으로, `unwrap_or_else` 함수에는 추가 제네릭 타입 매개변수 `F`가 있습니다. `F` 타입은 `f`라는 매개변수의 타입인데, 이것이 `unwrap_or_else`를 호출할 때 제공하는 클로저입니다.

제네릭 타입 `F`에 지정된 트레이트 바운드는 `FnOnce() -> T`입니다. 이는 `F`가 한 번 호출되고, 인수를 받지 않으며, `T`를 반환해야 함을 의미합니다. 트레이트 바운드에서 `FnOnce`를 사용하면 `unwrap_or_else`가 `f`를 최대 한 번만 호출할 것이라는 제약을 표현합니다. `unwrap_or_else`의 본문에서 `Option`이 `Some`이면 `f`가 호출되지 않는 것을 볼 수 있습니다. `Option`이 `None`이면 `f`가 한 번 호출됩니다. 모든 클로저가 `FnOnce`를 구현하기 때문에 `unwrap_or_else`는 가장 다양한 종류의 클로저를 받아들이고 가능한 한 유연합니다.

> 참고: 함수도 세 가지 `Fn` 트레이트를 모두 구현할 수 있습니다. 우리가 하려는 일이 환경에서 값을 캡처할 필요가 없다면, `Fn` 트레이트 중 하나를 구현하는 것이 필요한 곳에서 클로저 대신 함수 이름을 사용할 수 있습니다. 예를 들어, `Option<Vec<T>>` 값에 대해 `unwrap_or_else(Vec::new)`를 호출하여 값이 `None`이면 새 빈 벡터를 얻을 수 있습니다.

이제 슬라이스에 정의된 표준 라이브러리 메서드 `sort_by_key`를 살펴보고 `unwrap_or_else`와 어떻게 다른지, 그리고 왜 `sort_by_key`가 트레이트 바운드에 `FnOnce` 대신 `FnMut`를 사용하는지 알아보겠습니다. 클로저는 처리 중인 슬라이스의 현재 항목에 대한 참조 형태로 하나의 인수를 받고, 정렬할 수 있는 `K` 타입의 값을 반환합니다. 이 함수는 각 항목의 특정 속성으로 슬라이스를 정렬하려는 경우에 유용합니다. Listing 13-7에서 `Rectangle` 인스턴스 목록이 있고 `sort_by_key`를 사용하여 `width` 속성을 오름차순으로 정렬합니다:

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

이 코드는 다음을 출력합니다:

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

`sort_by_key`가 `FnMut` 클로저를 받도록 정의된 이유는 클로저를 여러 번 호출하기 때문입니다: 슬라이스의 각 항목에 대해 한 번씩. 클로저 `|r| r.width`는 환경에서 아무것도 캡처하지 않고, 변형하지 않고, 이동하지 않으므로 트레이트 바운드 요구 사항을 충족합니다.

반대로, Listing 13-8은 `FnOnce` 트레이트만 구현하는 클로저의 예를 보여줍니다. 환경에서 값을 이동하기 때문입니다. 컴파일러는 이 클로저를 `sort_by_key`와 함께 사용하도록 허용하지 않습니다:

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

이것은 `list`를 정렬할 때 `sort_by_key`가 클로저를 호출하는 횟수를 세려는 인위적이고 복잡한 방법입니다(작동하지 않음). 이 코드는 클로저의 환경에서 `String`인 `value`를 `sort_operations` 벡터에 푸시하여 이 카운팅을 시도합니다. 클로저는 `value`를 캡처한 다음 `value`의 소유권을 `sort_operations` 벡터로 이전하여 `value`를 클로저 밖으로 이동합니다. 이 클로저는 한 번만 호출할 수 있습니다; 두 번째 호출하려고 하면 `value`가 더 이상 환경에 없어 다시 `sort_operations`에 푸시할 수 없기 때문에 작동하지 않습니다! 따라서 이 클로저는 `FnOnce`만 구현합니다. 이 코드를 컴파일하려고 하면 `value`를 클로저 밖으로 이동할 수 없다는 오류가 발생합니다. 클로저가 `FnMut`를 구현해야 하기 때문입니다:

```text
error[E0507]: cannot move out of `value`, a captured variable in an `FnMut` closure
```

오류는 `value`를 환경 밖으로 이동하는 클로저의 줄을 가리킵니다. 이를 수정하려면 클로저 본문을 변경하여 환경 밖으로 값을 이동하지 않아야 합니다. `sort_by_key`가 호출되는 횟수를 세려면 환경에 카운터를 유지하고 클로저 본문에서 그 값을 증가시키는 것이 더 직접적인 방법입니다. Listing 13-9의 클로저는 `num_sort_operations` 카운터에 대한 가변 참조만 캡처하므로 두 번 이상 호출할 수 있어 `sort_by_key`와 함께 작동합니다:

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

`Fn` 트레이트는 클로저를 사용하는 함수나 타입을 정의하거나 사용할 때 중요합니다. 다음 섹션에서는 반복자에 대해 논의할 것입니다. 많은 반복자 메서드가 클로저 인수를 받으므로 계속 진행하면서 이러한 클로저 세부 사항을 기억해 두세요!

[unwrap-or-else]: https://doc.rust-lang.org/std/option/enum.Option.html#method.unwrap_or_else

---

## 반복자로 일련의 항목 처리하기

> **원문:** https://doc.rust-lang.org/book/ch13-02-iterators.html

반복자 패턴을 사용하면 일련의 항목에 대해 순서대로 어떤 작업을 수행할 수 있습니다. 반복자는 각 항목을 반복하고 시퀀스가 언제 끝났는지 결정하는 로직을 담당합니다. 반복자를 사용하면 이 로직을 직접 다시 구현할 필요가 없습니다.

Rust에서 반복자는 *지연적(lazy)*입니다. 즉, 반복자를 소비하는 메서드를 호출하여 사용하기 전까지는 아무런 효과가 없습니다. 예를 들어, Listing 13-10의 코드는 `Vec<T>`에 정의된 `iter` 메서드를 호출하여 벡터 `v1`의 항목에 대한 반복자를 생성합니다. 이 코드 자체로는 유용한 일을 수행하지 않습니다.

```rust
fn main() {
    let v1 = vec![1, 2, 3];

    let v1_iter = v1.iter();
}
```

*Listing 13-10: 반복자 생성하기*

반복자는 `v1_iter` 변수에 저장됩니다. 반복자를 생성하면 다양한 방법으로 사용할 수 있습니다. 3장의 Listing 3-5에서 `for` 루프를 사용하여 배열을 반복하며 각 항목에 대해 일부 코드를 실행했습니다. 내부적으로 이것은 암묵적으로 반복자를 생성한 다음 소비했지만, 지금까지 이 과정이 정확히 어떻게 작동하는지 설명하지 않았습니다.

Listing 13-11의 예제에서는 반복자 생성을 `for` 루프에서의 반복자 사용과 분리합니다. `v1_iter`의 반복자를 사용하여 `for` 루프가 호출되면, 반복자의 각 요소가 루프의 한 반복에서 사용되어 각 값을 출력합니다.

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

표준 라이브러리에서 제공하는 반복자가 없는 언어에서는 변수를 인덱스 0에서 시작하고, 그 변수를 사용하여 벡터에 인덱싱하여 값을 얻고, 벡터의 총 항목 수에 도달할 때까지 루프에서 변수 값을 증가시켜 동일한 기능을 작성할 수 있습니다.

반복자는 이 모든 로직을 처리하여 잠재적으로 엉망으로 만들 수 있는 반복적인 코드를 줄여줍니다. 반복자는 벡터와 같이 인덱싱할 수 있는 데이터 구조뿐만 아니라 많은 다른 종류의 시퀀스에서 동일한 로직을 사용할 수 있는 유연성을 더 많이 제공합니다. 반복자가 어떻게 그렇게 하는지 살펴보겠습니다.

### `Iterator` 트레이트와 `next` 메서드

모든 반복자는 표준 라이브러리에 정의된 `Iterator`라는 트레이트를 구현합니다. 트레이트의 정의는 다음과 같습니다:

```rust
pub trait Iterator {
    type Item;

    fn next(&mut self) -> Option<Self::Item>;

    // 기본 구현이 있는 메서드는 생략
}
```

이 정의는 새로운 구문을 사용하는 것에 주목하세요: `type Item`과 `Self::Item`, 이것은 이 트레이트와 함께 *연관 타입*을 정의합니다. 연관 타입에 대해서는 20장에서 자세히 설명하겠습니다. 지금은 이 코드가 `Iterator` 트레이트를 구현하려면 `Item` 타입도 정의해야 하며, 이 `Item` 타입이 `next` 메서드의 반환 타입에 사용된다는 것만 알면 됩니다. 즉, `Item` 타입은 반복자에서 반환되는 타입입니다.

`Iterator` 트레이트는 구현자가 `next` 메서드 하나만 정의하면 됩니다. 이 메서드는 반복자의 항목을 한 번에 하나씩 `Some`으로 감싸서 반환하고, 반복이 끝나면 `None`을 반환합니다.

반복자에서 직접 `next` 메서드를 호출할 수 있습니다; Listing 13-12는 벡터에서 생성된 반복자에서 `next`를 반복 호출했을 때 어떤 값이 반환되는지 보여줍니다.

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

`v1_iter`를 가변으로 만들어야 했다는 점에 유의하세요: 반복자에서 `next` 메서드를 호출하면 반복자가 시퀀스에서 현재 위치를 추적하는 데 사용하는 내부 상태가 변경됩니다. 다시 말해, 이 코드는 반복자를 *소비*하거나 사용합니다. `next`에 대한 각 호출은 반복자에서 항목을 소비합니다. `for` 루프를 사용할 때는 `v1_iter`를 가변으로 만들 필요가 없었습니다. 루프가 `v1_iter`의 소유권을 가져가고 내부적으로 가변으로 만들었기 때문입니다.

또한 `next` 호출에서 얻은 값이 벡터의 값에 대한 불변 참조라는 점에 유의하세요. `iter` 메서드는 불변 참조에 대한 반복자를 생성합니다. `v1`의 소유권을 가져가고 소유된 값을 반환하는 반복자를 생성하려면 `iter` 대신 `into_iter`를 호출할 수 있습니다. 마찬가지로, 가변 참조를 반복하려면 `iter` 대신 `iter_mut`를 호출할 수 있습니다.

### 반복자를 소비하는 메서드

`Iterator` 트레이트에는 표준 라이브러리에서 제공하는 기본 구현이 있는 여러 다른 메서드가 있습니다; 표준 라이브러리 API 문서에서 `Iterator` 트레이트를 살펴보면 이러한 메서드에 대해 알아볼 수 있습니다. 이러한 메서드 중 일부는 정의에서 `next` 메서드를 호출하므로 `Iterator` 트레이트를 구현할 때 `next` 메서드를 구현해야 합니다.

`next`를 호출하는 메서드는 *소비 어댑터*라고 합니다. 호출하면 반복자를 사용하기 때문입니다. 한 가지 예는 `sum` 메서드로, 반복자의 소유권을 가져가고 반복적으로 `next`를 호출하여 항목을 반복하여 반복자를 소비합니다. 반복하면서 누적 합계에 각 항목을 더하고 반복이 완료되면 합계를 반환합니다. Listing 13-13에는 `sum` 메서드 사용을 보여주는 테스트가 있습니다:

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

`sum`이 호출된 반복자의 소유권을 가져가기 때문에 `sum` 호출 후에는 `v1_iter`를 사용할 수 없습니다.

### 다른 반복자를 생성하는 메서드

*반복자 어댑터*는 `Iterator` 트레이트에 정의된 메서드로, 반복자를 소비하지 않습니다. 대신 원래 반복자의 일부 측면을 변경하여 다른 반복자를 생성합니다.

Listing 13-14는 반복자 어댑터 메서드 `map`을 호출하는 예를 보여줍니다. 이 메서드는 항목이 반복될 때 각 항목에 대해 호출할 클로저를 받습니다. `map` 메서드는 수정된 항목을 생성하는 새 반복자를 반환합니다. 여기서 클로저는 벡터의 각 항목이 1씩 증가된 새 반복자를 생성합니다:

```rust
fn main() {
    let v1: Vec<i32> = vec![1, 2, 3];

    v1.iter().map(|x| x + 1);
}
```

*Listing 13-14: 반복자 어댑터 `map`을 호출하여 새 반복자 생성하기*

그러나 이 코드는 경고를 생성합니다:

```text
warning: unused `Map` that must be used
 --> src/main.rs:4:5
  |
4 |     v1.iter().map(|x| x + 1);
  |     ^^^^^^^^^^^^^^^^^^^^^^^^
  |
  = note: iterators are lazy and do nothing unless consumed
```

Listing 13-14의 코드는 아무것도 하지 않습니다; 지정한 클로저가 호출되지 않습니다. 경고는 그 이유를 상기시켜 줍니다: 반복자 어댑터는 지연적이며, 여기서 반복자를 소비해야 합니다.

이를 수정하고 반복자를 소비하려면 `collect` 메서드를 사용합니다. 이 메서드는 13장에서 `env::args`와 함께 12장에서 사용했습니다. 이 메서드는 반복자를 소비하고 결과 값을 컬렉션 데이터 타입으로 수집합니다.

Listing 13-15에서 `map` 호출에서 반환된 반복자를 반복한 결과를 벡터에 수집합니다. 이 벡터는 원래 벡터의 각 항목이 1씩 증가된 값을 포함합니다.

```rust
fn main() {
    let v1: Vec<i32> = vec![1, 2, 3];

    let v2: Vec<_> = v1.iter().map(|x| x + 1).collect();

    assert_eq!(v2, vec![2, 3, 4]);
}
```

*Listing 13-15: `map` 메서드를 호출하여 새 반복자를 생성한 다음 `collect` 메서드를 호출하여 새 반복자를 소비하고 벡터 생성하기*

`map`은 클로저를 받기 때문에 각 항목에 수행하려는 모든 작업을 지정할 수 있습니다. 이것은 클로저가 `Iterator` 트레이트가 제공하는 반복 동작을 재사용하면서 일부 동작을 사용자 정의하는 방법의 좋은 예입니다.

반복자 어댑터에 대한 여러 호출을 연결하여 복잡한 작업을 읽기 쉬운 방식으로 수행할 수 있습니다. 그러나 모든 반복자는 지연적이므로 반복자 어댑터 호출에서 결과를 얻으려면 소비 어댑터 메서드 중 하나를 호출해야 합니다.

### 환경을 캡처하는 클로저 사용하기

많은 반복자 어댑터가 클로저를 인수로 받으며, 일반적으로 반복자 어댑터에 인수로 지정하는 클로저는 환경을 캡처하는 클로저입니다.

이 예제에서는 클로저를 받는 `filter` 메서드를 사용합니다. 클로저는 반복자에서 항목을 가져와 `bool`을 반환합니다. 클로저가 `true`를 반환하면 값이 `filter`가 생성한 반복에 포함됩니다. 클로저가 `false`를 반환하면 값이 포함되지 않습니다.

Listing 13-16에서 `filter`를 사용하여 환경에서 `shoe_size` 변수를 캡처하는 클로저와 함께 `Shoe` 구조체 인스턴스 컬렉션을 반복합니다. 지정된 크기의 신발만 반환합니다.

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

`shoes_in_size` 함수는 매개변수로 신발 벡터의 소유권과 신발 크기를 받습니다. 지정된 크기의 신발만 포함하는 벡터를 반환합니다.

`shoes_in_size`의 본문에서 `into_iter`를 호출하여 벡터의 소유권을 갖는 반복자를 생성합니다. 그런 다음 `filter`를 호출하여 해당 반복자를 클로저가 `true`를 반환하는 요소만 포함하는 새 반복자로 변환합니다.

클로저는 환경에서 `shoe_size` 매개변수를 캡처하고 해당 값을 각 신발의 크기와 비교하여 지정된 크기의 신발만 유지합니다. 마지막으로 `collect`를 호출하면 적용된 반복자가 반환한 값을 함수가 반환하는 벡터에 수집합니다.

테스트는 `shoes_in_size`를 호출하면 지정한 것과 같은 크기의 신발만 반환된다는 것을 보여줍니다.

---

## 반복자를 사용하여 I/O 프로젝트 개선하기

> **원문:** https://doc.rust-lang.org/book/ch13-03-improving-our-io-project.html

반복자에 대한 새로운 지식을 바탕으로 12장의 I/O 프로젝트를 반복자를 사용하여 코드의 여러 부분을 더 명확하고 간결하게 만들어 개선할 수 있습니다. `Config::build` 함수와 `search` 함수의 구현을 반복자가 어떻게 개선할 수 있는지 살펴보겠습니다.

### 반복자를 사용하여 `clone` 제거하기

Listing 12-6에서 `String` 값의 슬라이스를 받아 슬라이스에 인덱싱하고 값을 복제하여 `Config` 구조체가 해당 값을 소유하도록 `Config` 구조체의 인스턴스를 만드는 코드를 추가했습니다. Listing 13-17에서 Listing 12-23에서와 같이 `Config::build` 함수의 구현을 다시 보여줍니다:

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

당시에 우리는 비효율적인 `clone` 호출에 대해 걱정하지 말고 나중에 제거하겠다고 말했습니다. 이제 그때가 되었습니다!

여기서 `clone`이 필요했던 이유는 매개변수 `args`에 `String` 요소가 있는 슬라이스가 있지만 `build` 함수는 `args`를 소유하지 않기 때문입니다. `Config` 인스턴스의 소유권을 반환하려면 `Config`의 `query` 및 `file_path` 필드에서 값을 복제해야 `Config` 인스턴스가 해당 값을 소유할 수 있습니다.

반복자에 대한 새로운 지식을 통해 슬라이스를 빌리는 대신 반복자의 소유권을 인수로 받도록 `build` 함수를 변경할 수 있습니다. 슬라이스의 길이를 확인하고 특정 위치에 인덱싱하는 코드 대신 반복자 기능을 사용합니다. 이렇게 하면 반복자가 값에 액세스하기 때문에 `Config::build` 함수가 수행하는 작업이 명확해집니다.

`Config::build`가 반복자의 소유권을 가져가고 빌린 인덱싱 작업을 사용하지 않으면 `clone`을 호출하고 새 할당을 만드는 대신 반복자의 `String` 값을 `Config`로 이동할 수 있습니다.

#### 반환된 반복자 직접 사용하기

I/O 프로젝트의 *src/main.rs* 파일을 열면 다음과 같이 보일 것입니다:

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

먼저 Listing 12-24에 있던 `main` 함수의 시작 부분을 Listing 13-18의 코드로 변경합니다. 이번에는 반복자를 사용합니다. `Config::build`도 업데이트할 때까지 컴파일되지 않습니다.

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

`env::args` 함수는 반복자를 반환합니다! 반복자 값을 벡터로 수집한 다음 슬라이스를 `Config::build`에 전달하는 대신, 이제 `env::args`에서 반환된 반복자의 소유권을 `Config::build`에 직접 전달합니다.

다음으로 `Config::build`의 정의를 업데이트해야 합니다. I/O 프로젝트의 *src/lib.rs* 파일에서 `Config::build`의 시그니처를 Listing 13-19처럼 변경해 보겠습니다. 함수 본문을 업데이트해야 하므로 아직 컴파일되지 않습니다.

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

`env::args` 함수의 표준 라이브러리 문서는 반환하는 반복자의 타입이 `std::env::Args`이고, 해당 타입이 `Iterator` 트레이트를 구현하고 `String` 값을 반환함을 보여줍니다.

`Config::build` 함수의 시그니처를 업데이트하여 `args` 매개변수가 `&[String]` 대신 트레이트 바운드 `impl Iterator<Item = String>`이 있는 제네릭 타입을 갖도록 했습니다. 11장의 "매개변수로서의 트레이트" 섹션에서 논의한 `impl Trait` 구문의 사용은 `args`가 `Iterator` 타입을 구현하고 `String` 항목을 반환하는 모든 타입이 될 수 있음을 의미합니다.

`args`의 소유권을 가져가고 반복하여 `args`를 변형할 것이기 때문에 `args` 매개변수의 지정에 `mut` 키워드를 추가하여 가변으로 만들 수 있습니다.

#### 인덱싱 대신 `Iterator` 트레이트 메서드 사용하기

다음으로 `Config::build`의 본문을 수정합니다. `args`가 `Iterator` 트레이트를 구현하기 때문에 `next` 메서드를 호출할 수 있다는 것을 알고 있습니다! Listing 13-20은 Listing 12-23의 코드를 `next` 메서드를 사용하도록 업데이트합니다:

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

`env::args` 반환 값의 첫 번째 값은 프로그램 이름입니다. 이 값을 무시하고 다음 값을 얻고 싶으므로 먼저 `next`를 호출하고 반환 값으로 아무것도 하지 않습니다. 그런 다음 `next`를 호출하여 `Config`의 `query` 필드에 넣을 값을 얻습니다. `next`가 `Some`을 반환하면 `match`를 사용하여 값을 추출합니다. `None`을 반환하면 인수가 충분하지 않다는 의미이므로 `Err` 값으로 일찍 반환합니다. `file_path` 값에 대해서도 동일하게 수행합니다.

### 반복자 어댑터로 코드 더 명확하게 만들기

I/O 프로젝트의 `search` 함수에서도 반복자를 활용할 수 있습니다. Listing 12-19에 있는 그대로 Listing 13-21에 재현되어 있습니다:

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

반복자 어댑터 메서드를 사용하여 이 코드를 더 간결하게 작성할 수 있습니다. 이렇게 하면 가변 중간 `results` 벡터도 피할 수 있습니다. 함수형 프로그래밍 스타일은 코드를 더 명확하게 하기 위해 가변 상태의 양을 최소화하는 것을 선호합니다. 가변 상태를 제거하면 검색이 병렬로 수행되도록 하는 향후 개선이 가능할 수 있습니다. `results` 벡터에 대한 동시 액세스를 관리할 필요가 없기 때문입니다. Listing 13-22는 이 변경 사항을 보여줍니다:

```rust
pub fn search<'a>(query: &str, contents: &'a str) -> Vec<&'a str> {
    contents
        .lines()
        .filter(|line| line.contains(query))
        .collect()
}
```

*Listing 13-22: `search` 함수 구현에서 반복자 어댑터 메서드 사용하기*

`search` 함수의 목적은 `query`를 포함하는 `contents`의 모든 줄을 반환하는 것임을 기억하세요. Listing 13-16의 `filter` 예제와 유사하게, 이 코드는 `filter` 어댑터를 사용하여 `line.contains(query)`가 `true`를 반환하는 줄만 유지합니다. 그런 다음 일치하는 줄을 `collect`로 다른 벡터에 수집합니다. 훨씬 더 간단합니다! `search_case_insensitive` 함수에서도 반복자 메서드를 사용하도록 동일한 변경을 자유롭게 해보세요.

### 루프와 반복자 중 선택하기

다음 논리적 질문은 자신의 코드에서 어떤 스타일을 선택해야 하고 그 이유가 무엇인지입니다: Listing 13-21의 원래 구현 또는 Listing 13-22의 반복자를 사용하는 버전. 대부분의 Rust 프로그래머는 반복자 스타일을 선호합니다. 처음에는 익숙해지기가 좀 더 어렵지만, 다양한 반복자 어댑터와 이들이 수행하는 작업에 대한 감각을 얻으면 반복자가 이해하기 더 쉬울 수 있습니다. 루핑과 새 벡터 생성의 다양한 부분을 만지작거리는 대신, 코드는 루프의 고수준 목적에 집중합니다. 이것은 일부 일반적인 코드를 추상화하여 이 코드에 고유한 개념, 즉 반복자의 각 요소가 통과해야 하는 필터링 조건을 더 쉽게 볼 수 있게 합니다.

그러나 두 구현이 정말 동등할까요? 직관적인 가정은 더 저수준 루프가 더 빠를 것이라는 것입니다. 성능에 대해 이야기해 봅시다.

---

## 성능 비교: 루프 vs. 반복자

> **원문:** https://doc.rust-lang.org/book/ch13-04-performance.html

루프를 사용할지 반복자를 사용할지 결정하려면 어떤 구현이 더 빠른지 알아야 합니다: 명시적 `for` 루프가 있는 `search` 함수 버전 또는 반복자가 있는 버전.

우리는 아서 코난 도일 경의 *셜록 홈즈의 모험* 전체 내용을 `String`에 로드하고 내용에서 *the*라는 단어를 찾는 벤치마크를 실행했습니다. 다음은 `for` 루프를 사용하는 `search` 버전과 반복자를 사용하는 버전의 벤치마크 결과입니다:

```text
test bench_search_for  ... bench:  19,620,300 ns/iter (+/- 915,700)
test bench_search_iter ... bench:  19,234,900 ns/iter (+/- 657,200)
```

반복자 버전이 약간 더 빨랐습니다! 여기서 벤치마크 코드를 설명하지는 않겠습니다. 요점은 두 버전이 동등하다는 것을 증명하는 것이 아니라 이 두 구현이 성능 측면에서 어떻게 비교되는지에 대한 일반적인 감각을 얻는 것이기 때문입니다.

더 포괄적인 벤치마크를 위해서는 다양한 크기의 다양한 텍스트를 `contents`로 사용하고, 다른 단어와 다른 길이의 단어를 `query`로 사용하고, 기타 모든 종류의 변형을 확인해야 합니다. 요점은 이것입니다: 반복자는 고수준 추상화이지만 직접 저수준 코드를 작성한 것과 거의 동일한 코드로 컴파일됩니다. 반복자는 Rust의 *제로 비용 추상화* 중 하나입니다. 이는 추상화를 사용해도 추가 런타임 오버헤드가 발생하지 않음을 의미합니다. 이것은 C++의 원래 설계자이자 구현자인 비야네 스트롭스트룹이 "Foundations of C++"(2012)에서 *제로 오버헤드*를 정의하는 방식과 유사합니다:

> 일반적으로 C++ 구현은 제로 오버헤드 원칙을 따릅니다: 사용하지 않는 것에 대해 비용을 지불하지 않습니다. 그리고 더 나아가: 사용하는 것에 대해 직접 더 잘 작성할 수 없습니다.

또 다른 예로, 다음 코드는 오디오 디코더에서 가져온 것입니다. 디코딩 알고리즘은 선형 예측 수학 연산을 사용하여 이전 샘플의 선형 함수를 기반으로 미래 값을 추정합니다. 이 코드는 반복자 체인을 사용하여 범위 내의 세 변수에 대해 일부 수학을 수행합니다: `buffer` 데이터 슬라이스, 12개의 `coefficients` 배열, `qlp_shift`로 데이터를 시프트할 양. 이 예제 내에서 변수를 선언했지만 값은 부여하지 않았습니다; 이 코드가 컨텍스트 외부에서 큰 의미가 없더라도 Rust가 고수준 아이디어를 저수준 코드로 변환하는 방법의 간결하고 실제적인 예입니다.

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

`prediction` 값을 계산하기 위해 이 코드는 `coefficients`의 12개 값 각각을 반복하고 `zip` 메서드를 사용하여 coefficient 값을 `buffer`의 이전 12개 값과 쌍으로 만듭니다. 그런 다음 각 쌍에 대해 값을 함께 곱하고 모든 결과를 합산한 다음 합계의 비트를 `qlp_shift` 비트만큼 오른쪽으로 시프트합니다.

오디오 디코더와 같은 애플리케이션의 계산은 종종 성능을 가장 높이 우선시합니다. 여기서 우리는 두 개의 어댑터를 사용하여 반복자를 만든 다음 값을 소비합니다. 이 Rust 코드는 어떤 어셈블리 코드로 컴파일될까요? 글쎄, 이 글을 쓰는 현재 직접 손으로 작성했을 것과 동일한 어셈블리로 컴파일됩니다. `coefficients`의 값에 대한 반복에 해당하는 루프가 전혀 없습니다: Rust는 12번의 반복이 있다는 것을 알기 때문에 루프를 "언롤링"합니다. *언롤링*은 루프 제어 코드의 오버헤드를 제거하고 대신 루프의 각 반복에 대해 반복적인 코드를 생성하는 최적화입니다.

모든 coefficients는 레지스터에 저장되어 값에 대한 매우 빠른 액세스를 의미합니다. 런타임에 배열 액세스에 대한 경계 검사가 없습니다. Rust가 적용할 수 있는 이러한 모든 최적화는 결과 코드를 매우 효율적으로 만듭니다. 이제 이것을 알았으니 반복자와 클로저를 두려움 없이 사용할 수 있습니다! 반복자와 클로저는 코드를 더 고수준으로 보이게 하지만 그렇게 하기 위해 런타임 성능 페널티를 부과하지 않습니다.

### 요약

클로저와 반복자는 함수형 프로그래밍 언어 아이디어에서 영감을 받은 Rust 기능입니다. 이들은 저수준 성능으로 고수준 아이디어를 명확하게 표현하는 Rust의 능력에 기여합니다. 클로저와 반복자의 구현은 런타임 성능이 영향을 받지 않도록 합니다. 이것은 제로 비용 추상화를 제공하기 위해 노력하는 Rust의 목표의 일부입니다.

이제 I/O 프로젝트의 표현력을 개선했으니 프로젝트를 세상과 공유하는 데 도움이 될 `cargo`의 몇 가지 기능을 더 살펴보겠습니다.

---

## Cargo와 Crates.io 더 알아보기

## Cargo와 Crates.io에 대해 더 알아보기

> **원문:** https://doc.rust-lang.org/book/ch14-00-more-about-cargo.html

지금까지 우리는 Cargo의 가장 기본적인 기능만 사용하여 코드를 빌드, 실행, 테스트했지만, Cargo는 훨씬 더 많은 것을 할 수 있습니다. 이 장에서는 다음과 같은 작업을 수행하는 방법을 보여주는 더 고급 기능 중 일부를 논의합니다:

* 릴리스 프로필을 통해 빌드 사용자 정의하기
* [crates.io](https://crates.io/)에 라이브러리 게시하기
* 워크스페이스로 대규모 프로젝트 구성하기
* [crates.io](https://crates.io/)에서 바이너리 설치하기
* 사용자 정의 명령을 사용하여 Cargo 확장하기

Cargo는 이 장에서 다루는 것보다 훨씬 더 많은 것을 할 수 있으므로 모든 기능에 대한 전체 설명은 [해당 문서](https://doc.rust-lang.org/cargo/)를 참조하세요.

---

## 릴리스 프로필로 빌드 사용자 정의하기

> **원문:** https://doc.rust-lang.org/book/ch14-01-release-profiles.html

Rust에서 *릴리스 프로필*은 프로그래머가 코드 컴파일을 위한 다양한 옵션을 더 많이 제어할 수 있도록 하는 다양한 구성을 가진 미리 정의된 사용자 정의 가능한 프로필입니다. 각 프로필은 다른 프로필과 독립적으로 구성됩니다.

Cargo에는 두 가지 주요 프로필이 있습니다: `cargo build`를 실행할 때 Cargo가 사용하는 `dev` 프로필과 `cargo build --release`를 실행할 때 Cargo가 사용하는 `release` 프로필입니다. `dev` 프로필은 개발에 적합한 기본값으로 정의되고 `release` 프로필은 릴리스 빌드에 적합한 기본값이 있습니다.

이러한 프로필 이름은 빌드 출력에서 익숙할 수 있습니다:

```console
$ cargo build
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
$ cargo build --release
    Finished `release` profile [optimized] target(s) in 0.32s
```

`dev`와 `release`는 컴파일러가 사용하는 이러한 다른 프로필입니다.

Cargo는 프로젝트의 *Cargo.toml* 파일에 명시적으로 추가된 `[profile.*]` 섹션이 없을 때 적용되는 각 프로필에 대한 기본 설정이 있습니다. 사용자 정의하려는 프로필에 대해 `[profile.*]` 섹션을 추가하면 기본 설정의 모든 하위 집합을 재정의합니다. 예를 들어, `dev` 및 `release` 프로필의 `opt-level` 설정에 대한 기본값은 다음과 같습니다:

```toml
[profile.dev]
opt-level = 0

[profile.release]
opt-level = 3
```

`opt-level` 설정은 Rust가 코드에 적용할 최적화 수를 제어하며, 0에서 3 사이의 범위입니다. 더 많은 최적화를 적용하면 컴파일 시간이 늘어나므로 개발 중이고 자주 코드를 컴파일하는 경우 결과 코드가 느리게 실행되더라도 더 빠른 컴파일을 원합니다. 따라서 `dev`의 기본 `opt-level`은 `0`입니다. 코드를 릴리스할 준비가 되면 컴파일하는 데 더 많은 시간을 투자하는 것이 가장 좋습니다. 릴리스 모드에서는 한 번만 컴파일하지만 컴파일된 프로그램은 여러 번 실행하므로 릴리스 모드는 더 긴 컴파일 시간을 더 빠르게 실행되는 코드와 교환합니다. 그래서 `release` 프로필의 기본 `opt-level`은 `3`입니다.

*Cargo.toml*에 다른 값을 추가하여 기본 설정을 재정의할 수 있습니다. 예를 들어, 개발 프로필에서 최적화 레벨 1을 사용하려면 프로젝트의 *Cargo.toml* 파일에 다음 두 줄을 추가할 수 있습니다:

```toml
[profile.dev]
opt-level = 1
```

이 코드는 기본 설정 `0`을 재정의합니다. 이제 `cargo build`를 실행하면 Cargo는 `dev` 프로필의 기본값과 `opt-level`에 대한 사용자 정의를 사용합니다. `opt-level`을 `1`로 설정했기 때문에 Cargo는 기본값보다 더 많은 최적화를 적용하지만 릴리스 빌드만큼은 많지 않습니다.

각 프로필의 구성 옵션 및 기본값의 전체 목록은 [Cargo 문서](https://doc.rust-lang.org/cargo/reference/profiles.html)를 참조하세요.

---

## Crates.io에 크레이트 게시하기

> **원문:** https://doc.rust-lang.org/book/ch14-02-publishing-to-crates-io.html

프로젝트의 의존성으로 [crates.io](https://crates.io/)의 패키지를 사용해 왔지만, 자신의 패키지를 게시하여 다른 사람들과 코드를 공유할 수도 있습니다. [crates.io](https://crates.io/)의 크레이트 레지스트리는 패키지의 소스 코드를 배포하므로 주로 오픈 소스인 코드를 호스팅합니다.

Rust와 Cargo에는 게시된 패키지를 사람들이 더 쉽게 찾고 사용할 수 있도록 하는 기능이 있습니다. 이러한 기능 중 일부에 대해 다음에 이야기하고 그 다음에 패키지를 게시하는 방법을 설명하겠습니다.

### 유용한 문서 주석 작성하기

패키지를 정확하게 문서화하면 다른 사용자가 패키지를 어떻게 그리고 언제 사용해야 하는지 알 수 있으므로 문서 작성에 시간을 투자할 가치가 있습니다. 3장에서 두 개의 슬래시 `//`를 사용하여 Rust 코드에 주석을 다는 것에 대해 논의했습니다. Rust에는 *문서 주석*으로 알려진 문서화를 위한 특별한 종류의 주석도 있으며, 이는 HTML 문서를 생성합니다. HTML은 크레이트를 *구현하는* 방법이 아니라 크레이트를 *사용하는* 방법을 알고 싶어하는 프로그래머를 위한 공개 API 항목에 대한 문서 주석의 내용을 표시합니다.

문서 주석은 두 개 대신 세 개의 슬래시 `///`를 사용하고 텍스트 서식 지정을 위한 Markdown 표기법을 지원합니다. 문서 주석을 문서화하는 항목 바로 앞에 배치합니다. Listing 14-1은 `my_crate`라는 크레이트의 `add_one` 함수에 대한 문서 주석을 보여줍니다.

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

여기서 `add_one` 함수가 수행하는 작업에 대한 설명을 제공하고 `예제`라는 제목의 섹션을 시작한 다음 `add_one` 함수를 사용하는 방법을 보여주는 코드를 제공합니다. `cargo doc`을 실행하여 이 문서 주석에서 HTML 문서를 생성할 수 있습니다. 이 명령은 Rust와 함께 배포되는 `rustdoc` 도구를 실행하고 생성된 HTML 문서를 *target/doc* 디렉토리에 배치합니다.

편의를 위해 `cargo doc --open`을 실행하면 현재 크레이트의 문서(및 크레이트의 모든 의존성에 대한 문서)에 대한 HTML을 빌드하고 결과를 웹 브라우저에서 엽니다. `add_one` 함수로 이동하면 문서 주석의 텍스트가 어떻게 렌더링되는지 볼 수 있습니다.

#### 자주 사용되는 섹션

Listing 14-1에서 `# 예제` Markdown 제목을 사용하여 HTML에서 "예제"라는 제목의 섹션을 만들었습니다. 다음은 크레이트 작성자가 문서에서 자주 사용하는 몇 가지 다른 섹션입니다:

* **Panics**: 문서화되는 함수가 `panic!`할 수 있는 시나리오. 프로그램을 패닉시키고 싶지 않은 함수 호출자는 이러한 상황에서 함수를 호출하지 않도록 해야 합니다.
* **Errors**: 함수가 `Result`를 반환하는 경우 발생할 수 있는 오류의 종류와 해당 오류가 반환될 수 있는 조건을 설명하면 호출자가 다른 종류의 오류를 다른 방식으로 처리하는 코드를 작성하는 데 도움이 될 수 있습니다.
* **Safety**: 함수가 호출하기에 `unsafe`한 경우(19장에서 안전하지 않음에 대해 논의합니다), 함수가 안전하지 않은 이유와 함수가 호출자가 지켜야 할 불변성을 설명하는 섹션이 있어야 합니다.

대부분의 문서 주석에는 이러한 섹션이 모두 필요하지는 않지만, 코드를 호출하는 사람들이 알고 싶어할 측면을 상기시키기 위한 좋은 체크리스트입니다.

#### 테스트로서의 문서 주석

문서 주석에 예제 코드 블록을 추가하면 라이브러리 사용 방법을 보여주는 데 도움이 될 수 있으며, 그렇게 하면 추가 보너스가 있습니다: `cargo test`를 실행하면 문서의 코드 예제가 테스트로 실행됩니다! 예제가 있는 문서보다 더 좋은 것은 없습니다. 그러나 문서가 작성된 이후 코드가 변경되었기 때문에 작동하지 않는 예제보다 더 나쁜 것도 없습니다. Listing 14-1의 `add_one` 함수에 대한 문서와 함께 `cargo test`를 실행하면 다음과 같은 테스트 결과 섹션이 표시됩니다:

```text
   Doc-tests my_crate

running 1 test
test src/lib.rs - add_one (line 5) ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out; finished in 0.27s
```

이제 함수나 예제를 변경하여 예제의 `assert_eq!`가 패닉하고 `cargo test`를 다시 실행하면 문서 테스트가 예제와 코드가 서로 동기화되지 않았음을 감지하는 것을 볼 수 있습니다!

#### 포함된 항목에 주석 달기

문서 주석 스타일 `//!`은 주석 뒤에 오는 항목이 아닌 주석을 포함하는 항목에 문서를 추가합니다. 일반적으로 이러한 문서 주석은 크레이트 루트 파일(관례상 *src/lib.rs*) 내부 또는 모듈 내부에서 전체 크레이트나 모듈을 문서화하는 데 사용합니다.

예를 들어 `add_one` 함수를 포함하는 `my_crate` 크레이트의 목적을 설명하는 문서를 추가하려면 *src/lib.rs* 파일 시작 부분에 `//!`로 시작하는 문서 주석을 추가합니다. Listing 14-2에 나와 있습니다:

```rust
//! # My Crate
//!
//! `my_crate`는 특정 계산을 더 편리하게 수행하기 위한
//! 유틸리티 모음입니다.

/// 주어진 숫자에 1을 더합니다.
// --snip--
```

*Listing 14-2: `my_crate` 크레이트 전체에 대한 문서*

`//!`로 시작하는 마지막 줄 뒤에 코드가 없다는 점에 유의하세요. `///` 대신 `//!`로 주석을 시작했기 때문에 이 주석 뒤에 오는 항목이 아닌 이 주석을 포함하는 항목을 문서화합니다. 이 경우 해당 항목은 크레이트 루트인 *src/lib.rs* 파일입니다.

이제 `cargo doc --open`을 실행하면 이러한 주석이 `my_crate`의 문서 첫 페이지에 크레이트의 공개 항목 목록 위에 표시됩니다.

항목 내부의 문서 주석은 특히 크레이트와 모듈을 설명하는 데 유용합니다. 이를 사용하여 컨테이너의 전체 목적을 설명하고 사용자가 크레이트의 구성을 이해하도록 도와주세요.

### `pub use`로 편리한 공개 API 내보내기

공개 API의 구조는 크레이트를 게시할 때 주요 고려 사항입니다. 크레이트를 사용하는 사람들은 여러분보다 구조에 덜 익숙하며 크레이트에 큰 모듈 계층 구조가 있는 경우 사용하려는 부분을 찾는 데 어려움을 겪을 수 있습니다.

7장에서 `pub` 키워드를 사용하여 항목을 공개하고 `use` 키워드를 사용하여 항목을 범위에 가져오는 방법을 다뤘습니다. 그러나 크레이트를 개발하는 동안 여러분에게 합리적인 구조가 사용자에게는 그다지 편리하지 않을 수 있습니다. 여러 레벨의 계층 구조로 구조체를 구성하고 싶을 수 있지만, 계층 구조 깊이 정의한 타입을 사용하려는 사람들은 해당 타입이 존재하는지 찾는 데 어려움을 겪을 수 있습니다. 사용자들은 또한 `use my_crate::UsefulType;` 대신 `use my_crate::some_module::another_module::UsefulType;`을 입력해야 하는 것에 짜증을 낼 수 있습니다.

좋은 소식은 다른 사람들이 다른 라이브러리에서 사용하기에 편리하지 *않은* 구조라면 내부 구성을 재배열할 필요가 없다는 것입니다: 대신 `pub use`를 사용하여 항목을 다시 내보내 비공개 구조와 다른 공개 구조를 만들 수 있습니다. 재내보내기는 한 위치에서 공개 항목을 가져와 마치 다른 위치에서 정의된 것처럼 다른 위치에서 공개합니다.

예를 들어 예술적 개념을 모델링하기 위해 `art`라는 라이브러리를 만들었다고 가정합니다. 이 라이브러리 내에는 두 개의 모듈이 있습니다: Listing 14-3에 표시된 대로 `PrimaryColor`와 `SecondaryColor`라는 두 열거형을 포함하는 `kinds` 모듈과 `mix`라는 함수를 포함하는 `utils` 모듈입니다:

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

`cargo doc`이 생성한 이 크레이트의 문서 첫 페이지는 `kinds`와 `utils` 모듈을 나열하고 `PrimaryColor`와 `SecondaryColor` 타입이나 `mix` 함수를 나열하지 않습니다. 이를 보려면 모듈을 클릭해야 합니다.

이 라이브러리에 의존하는 다른 크레이트는 현재 정의된 모듈 구조를 지정하여 `art`의 항목을 범위로 가져오는 `use` 문이 필요합니다. Listing 14-4는 `art` 크레이트의 `PrimaryColor` 및 `mix` 항목을 사용하는 크레이트의 예를 보여줍니다:

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

Listing 14-4의 코드 작성자, `art` 크레이트를 사용하는 사람은 `PrimaryColor`가 `kinds` 모듈에 있고 `mix`가 `utils` 모듈에 있다는 것을 알아내야 했습니다. `art` 크레이트의 모듈 구조는 `art` 크레이트를 사용하는 개발자보다 `art` 크레이트를 작업하는 개발자에게 더 관련이 있습니다. 크레이트의 부분을 `kinds` 모듈과 `utils` 모듈로 구성하는 내부 구조는 `art` 크레이트 사용 방법을 이해하려는 사람에게 유용한 정보를 포함하지 않습니다. 대신 `art` 크레이트의 모듈 구조는 혼란을 야기합니다. 사용자가 어디를 봐야 할지 알아내야 하고 `use` 문에서 모듈 이름을 지정해야 하기 때문입니다.

공개 API에서 내부 구성을 제거하려면 Listing 14-3의 `art` 크레이트 코드를 수정하여 `pub use` 문을 추가하여 최상위 레벨에서 항목을 재내보낼 수 있습니다. Listing 14-5에 나와 있습니다:

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

`cargo doc`이 이 크레이트에 대해 생성하는 API 문서는 이제 첫 페이지에 재내보내기를 나열하고 링크하여 `PrimaryColor` 및 `SecondaryColor` 타입과 `mix` 함수를 더 쉽게 찾을 수 있습니다.

`art` 크레이트 사용자는 Listing 14-4에 표시된 대로 Listing 14-3의 내부 구조를 보고 사용할 수 있거나 Listing 14-6에 표시된 대로 Listing 14-5의 더 편리한 구조를 사용할 수 있습니다:

```rust
use art::mix;
use art::PrimaryColor;

fn main() {
    // --snip--
}
```

*Listing 14-6: `art` 크레이트의 재내보낸 항목을 사용하는 프로그램*

중첩된 모듈이 많은 경우 `pub use`로 최상위 레벨에서 타입을 재내보내면 크레이트를 사용하는 사람들의 경험에 큰 차이를 만들 수 있습니다. `pub use`의 또 다른 일반적인 용도는 현재 크레이트의 의존성 정의를 재내보내 해당 크레이트의 정의를 크레이트의 공개 API의 일부로 만드는 것입니다.

유용한 공개 API 구조를 만드는 것은 과학보다 예술에 가깝고 반복하여 사용자에게 가장 적합한 API를 찾을 수 있습니다. `pub use`를 선택하면 크레이트를 내부적으로 구조화하는 방법에 유연성을 제공하고 해당 내부 구조를 사용자에게 제시하는 것에서 분리합니다. 설치한 일부 크레이트의 코드를 살펴보고 내부 구조가 공개 API와 다른지 확인하세요.

### Crates.io 계정 설정하기

패키지를 게시하려면 먼저 [crates.io](https://crates.io/)에서 계정을 만들고 API 토큰을 받아야 합니다. 그렇게 하려면 [crates.io](https://crates.io/)의 홈페이지를 방문하여 GitHub 계정을 통해 로그인하세요. (현재 GitHub 계정이 필요하지만 사이트에서 향후 다른 계정 생성 방법을 지원할 수 있습니다.) 로그인한 후 [https://crates.io/me/](https://crates.io/me/)의 계정 설정을 방문하여 API 키를 검색하세요. 그런 다음 API 키로 `cargo login` 명령을 실행합니다:

```console
$ cargo login abcdefghijklmnopqrstuvwxyz012345
```

이 명령은 Cargo에 API 토큰을 알리고 로컬에 *~/.cargo/credentials.toml*에 저장합니다. 이 토큰은 *비밀*입니다: 다른 사람과 공유하지 마세요. 어떤 이유로든 다른 사람과 공유한 경우 철회하고 [crates.io](https://crates.io/)에서 새 토큰을 생성해야 합니다.

### 새 크레이트에 메타데이터 추가하기

계정이 있다고 가정하고 게시하려는 크레이트가 있다고 합시다. 게시하기 전에 *Cargo.toml* 파일의 `[package]` 섹션에 추가하여 크레이트에 일부 메타데이터를 추가해야 합니다.

크레이트는 고유한 이름이 필요합니다. 로컬에서 크레이트를 작업하는 동안 크레이트 이름을 원하는 대로 지정할 수 있습니다. 그러나 [crates.io](https://crates.io/)의 크레이트 이름은 선착순으로 할당됩니다. 크레이트 이름이 사용되면 다른 사람은 해당 이름으로 크레이트를 게시할 수 없습니다. 사이트에서 사용하려는 이름을 검색하여 사용되었는지 확인하세요. 그렇지 않은 경우 *Cargo.toml*의 `[package]` 아래의 이름을 편집하여 게시에 사용합니다:

```toml
[package]
name = "guessing_game"
```

고유한 이름을 선택했더라도 이 시점에서 `cargo publish`를 실행하여 크레이트를 게시하면 경고와 오류가 표시됩니다:

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

이 오류는 일부 중요한 정보가 누락되었기 때문입니다: 설명과 라이선스가 필요합니다. 이는 사람들이 크레이트가 무엇을 하고 어떤 조건으로 사용할 수 있는지 알 수 있도록 하기 위해서입니다. *Cargo.toml*에서 크레이트를 검색 결과에 표시할 한두 문장의 설명을 추가합니다. `license` 필드의 경우 *라이선스 식별자 값*을 제공해야 합니다. [Linux Foundation의 소프트웨어 패키지 데이터 교환(SPDX)][spdx]는 이 값에 사용할 수 있는 식별자를 나열합니다. 예를 들어 MIT 라이선스로 크레이트에 라이선스를 부여했음을 지정하려면 `MIT` 식별자를 추가합니다:

```toml
[package]
name = "guessing_game"
license = "MIT"
```

SPDX에 나타나지 않는 라이선스를 사용하려면 해당 라이선스의 텍스트를 파일에 넣고 프로젝트에 파일을 포함한 다음 `license` 키 대신 `license-file`을 사용하여 해당 파일의 이름을 지정해야 합니다.

어떤 라이선스가 프로젝트에 적합한지에 대한 지침은 이 책의 범위를 벗어납니다. Rust 커뮤니티의 많은 사람들은 `MIT OR Apache-2.0`의 이중 라이선스를 사용하여 Rust와 동일한 방식으로 프로젝트에 라이선스를 부여합니다. 이 관행은 `OR`로 구분된 여러 라이선스 식별자를 지정하여 프로젝트에 여러 라이선스를 가질 수 있음을 보여줍니다.

고유한 이름, 버전, 설명, 라이선스가 추가되면 게시할 준비가 된 프로젝트의 *Cargo.toml* 파일은 다음과 같을 수 있습니다:

```toml
[package]
name = "guessing_game"
version = "0.1.0"
edition = "2024"
description = "컴퓨터가 선택한 숫자를 추측하는 재미있는 게임."
license = "MIT OR Apache-2.0"

[dependencies]
```

[Cargo 문서](https://doc.rust-lang.org/cargo/)는 다른 사람들이 크레이트를 더 쉽게 검색하고 사용할 수 있도록 지정할 수 있는 다른 메타데이터를 설명합니다.

### Crates.io에 게시하기

계정을 만들고, API 토큰을 저장하고, 크레이트 이름을 선택하고, 필요한 메타데이터를 지정했으므로 이제 게시할 준비가 되었습니다! 크레이트를 게시하면 다른 사람들이 사용할 수 있도록 [crates.io](https://crates.io/)에 특정 버전이 업로드됩니다.

게시는 *영구적*이므로 주의하세요. 버전은 절대 덮어쓸 수 없으며 코드를 삭제할 수 없습니다. [crates.io](https://crates.io/)의 주요 목표 중 하나는 코드의 영구 아카이브 역할을 하여 [crates.io](https://crates.io/)의 크레이트에 의존하는 모든 프로젝트의 빌드가 계속 작동하도록 하는 것입니다. 버전 삭제를 허용하면 해당 목표를 달성하는 것이 불가능해집니다. 그러나 게시할 수 있는 크레이트 버전 수에는 제한이 없습니다.

`cargo publish` 명령을 다시 실행하세요. 이제 성공해야 합니다:

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

축하합니다! 이제 Rust 커뮤니티와 코드를 공유했으며 누구나 크레이트를 프로젝트의 의존성으로 쉽게 추가할 수 있습니다.

### 기존 크레이트의 새 버전 게시하기

크레이트를 변경하고 새 버전을 릴리스할 준비가 되면 *Cargo.toml*에 지정된 `version` 값을 변경하고 다시 게시합니다. [시맨틱 버전 관리 규칙][semver]을 사용하여 변경 유형에 따라 적절한 다음 버전 번호를 결정하세요. 그런 다음 `cargo publish`를 실행하여 새 버전을 업로드합니다.

### `cargo yank`로 Crates.io에서 버전 폐기하기

이전 버전의 크레이트를 제거할 수는 없지만, 향후 프로젝트가 해당 버전을 새 의존성으로 추가하는 것을 방지할 수 있습니다. 이는 크레이트 버전이 어떤 이유로 손상된 경우에 유용합니다. 이러한 상황에서 Cargo는 버전 *얀크*(폐기)를 지원합니다.

버전을 얀크하면 새 프로젝트가 해당 버전에 의존하는 것을 방지하면서 해당 버전에 의존하는 모든 기존 프로젝트가 계속 작동하도록 합니다. 본질적으로 얀크는 *Cargo.lock*이 있는 모든 프로젝트가 중단되지 않고 향후 생성되는 *Cargo.lock* 파일이 얀크된 버전을 사용하지 않음을 의미합니다.

버전을 얀크하려면 이전에 게시한 크레이트의 디렉토리에서 `cargo yank`를 실행하고 얀크하려는 버전을 지정합니다. 예를 들어 `guessing_game`이라는 크레이트의 버전 1.0.1을 게시했고 얀크하려는 경우 `guessing_game`의 프로젝트 디렉토리에서 다음을 실행합니다:

```console
$ cargo yank --vers 1.0.1
    Updating crates.io index
        Yank guessing_game@1.0.1
```

명령에 `--undo`를 추가하면 얀크를 취소하고 프로젝트가 버전에 다시 의존할 수 있도록 허용할 수도 있습니다:

```console
$ cargo yank --vers 1.0.1 --undo
    Updating crates.io index
      Unyank guessing_game@1.0.1
```

얀크는 코드를 삭제하지 *않습니다*. 예를 들어, 실수로 업로드된 비밀을 삭제할 수 없습니다. 그런 일이 발생하면 해당 비밀을 즉시 재설정해야 합니다.

[spdx]: https://spdx.org/licenses/
[semver]: https://semver.org/

---

## Cargo 워크스페이스

> **원문:** https://doc.rust-lang.org/book/ch14-03-cargo-workspaces.html

12장에서 바이너리 크레이트와 라이브러리 크레이트를 포함하는 패키지를 빌드했습니다. 프로젝트가 발전함에 따라 라이브러리 크레이트가 계속 커지면서 패키지를 여러 라이브러리 크레이트로 더 분할하고 싶을 수 있습니다. Cargo는 *워크스페이스*라는 기능을 제공하여 함께 개발되는 여러 관련 패키지를 관리하는 데 도움을 줍니다.

### 워크스페이스 생성하기

*워크스페이스*는 동일한 *Cargo.lock* 및 출력 디렉토리를 공유하는 패키지 집합입니다. 워크스페이스를 사용하여 프로젝트를 만들어 보겠습니다—워크스페이스의 구조에 집중할 수 있도록 간단한 코드를 사용합니다. 워크스페이스를 구조화하는 여러 방법이 있으므로 일반적인 방법 하나만 보여드리겠습니다. 바이너리 하나와 라이브러리 두 개를 포함하는 워크스페이스가 있습니다. 주요 기능을 제공하는 바이너리는 두 라이브러리에 의존합니다. 한 라이브러리는 `add_one` 함수를 제공하고 두 번째 라이브러리는 `add_two` 함수를 제공합니다. 이 세 크레이트는 동일한 워크스페이스의 일부가 됩니다. 먼저 워크스페이스용 새 디렉토리를 만듭니다:

```console
$ mkdir add
$ cd add
```

다음으로 *add* 디렉토리에 전체 워크스페이스를 구성하는 *Cargo.toml* 파일을 만듭니다. 이 파일에는 다른 *Cargo.toml* 파일에서 볼 수 있는 `[package]` 섹션이 없습니다. 대신 `[workspace]` 섹션으로 시작합니다:

```toml
[workspace]
resolver = "3"
```

다음으로 *add* 디렉토리 내에서 `cargo new`를 실행하여 `adder` 바이너리 크레이트를 만듭니다:

```console
$ cargo new adder
     Created binary (application) `adder` package
      Adding `adder` as member of workspace at `/projects/add`
```

이 시점에서 `cargo build`를 실행하여 워크스페이스를 빌드할 수 있습니다. *add* 디렉토리의 파일은 다음과 같아야 합니다:

```text
├── Cargo.lock
├── Cargo.toml
├── adder
│   ├── Cargo.toml
│   └── src
│       └── main.rs
└── target
```

워크스페이스에는 컴파일된 아티팩트가 배치될 최상위 레벨에 하나의 *target* 디렉토리가 있습니다; `adder` 패키지에는 자체 *target* 디렉토리가 없습니다. *adder* 디렉토리 내부에서 `cargo build`를 실행하더라도 컴파일된 아티팩트는 *add/adder/target* 대신 *add/target*에 저장됩니다. Cargo는 워크스페이스에서 *target* 디렉토리를 이와 같이 구조화합니다. 워크스페이스의 크레이트가 서로 의존하기 때문입니다. 각 크레이트가 자체 *target* 디렉토리가 있다면 각 크레이트는 자체 *target* 디렉토리에 아티팩트를 배치하기 위해 워크스페이스의 다른 각 크레이트를 다시 컴파일해야 합니다. 하나의 *target* 디렉토리를 공유하면 크레이트가 불필요한 재빌드를 피할 수 있습니다.

### 워크스페이스에 두 번째 패키지 생성하기

다음으로 워크스페이스에 `add_one`이라는 또 다른 멤버 패키지를 만들겠습니다. 최상위 *Cargo.toml*을 변경하여 `members` 목록에 *add_one* 경로를 지정합니다:

```toml
[workspace]
resolver = "3"
members = ["adder", "add_one"]
```

그런 다음 `add_one`이라는 새 라이브러리 크레이트를 생성합니다:

```console
$ cargo new add_one --lib
     Created library `add_one` package
      Adding `add_one` as member of workspace at `/projects/add`
```

*add* 디렉토리는 이제 다음 디렉토리와 파일을 포함해야 합니다:

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

*add_one/src/lib.rs* 파일에 `add_one` 함수를 추가해 보겠습니다:

```rust
pub fn add_one(x: i32) -> i32 {
    x + 1
}
```

이제 바이너리가 있는 `adder` 패키지가 라이브러리를 포함하는 `add_one` 패키지에 의존하도록 할 수 있습니다. 먼저 *adder/Cargo.toml*에 `add_one`에 대한 경로 의존성을 추가해야 합니다.

```toml
[dependencies]
add_one = { path = "../add_one" }
```

Cargo는 워크스페이스의 크레이트가 서로 의존한다고 가정하지 않으므로 의존성 관계를 명시적으로 지정해야 합니다.

다음으로 `adder` 크레이트에서 (`add_one` 크레이트의) `add_one` 함수를 사용해 보겠습니다. *adder/src/main.rs* 파일을 열고 `add_one` 라이브러리 크레이트를 범위로 가져오는 `use` 줄을 맨 위에 추가합니다. 그런 다음 `main` 함수를 변경하여 `add_one` 함수를 호출합니다:

```rust
use add_one;

fn main() {
    let num = 10;
    println!("Hello, world! {num} 더하기 1은 {}!", add_one::add_one(num));
}
```

최상위 *add* 디렉토리에서 `cargo build`를 실행하여 워크스페이스를 빌드해 보겠습니다!

```console
$ cargo build
   Compiling add_one v0.1.0 (file:///projects/add/add_one)
   Compiling adder v0.1.0 (file:///projects/add/adder)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.22s
```

*add* 디렉토리에서 바이너리 크레이트를 실행하려면 `-p` 인수와 패키지 이름을 `cargo run`과 함께 사용하여 실행하려는 워크스페이스의 패키지를 지정할 수 있습니다:

```console
$ cargo run -p adder
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.00s
     Running `target/debug/adder`
Hello, world! 10 더하기 1은 11!
```

이렇게 하면 *adder/src/main.rs*의 코드가 실행되고 `add_one` 크레이트에 의존합니다.

#### 워크스페이스에서 외부 패키지에 의존하기

워크스페이스에는 각 크레이트의 디렉토리에 있는 개별 *Cargo.toml* 파일이 아닌 최상위 레벨에 *Cargo.lock* 파일이 하나만 있습니다. 이렇게 하면 워크스페이스의 모든 크레이트가 모든 의존성의 동일한 버전을 사용합니다. `rand` 패키지를 *adder/Cargo.toml* 및 *add_one/Cargo.toml* 파일에 추가하면 Cargo는 둘 다 하나의 `rand` 버전으로 해결하고 하나의 *Cargo.lock*에 기록합니다. 워크스페이스의 모든 크레이트가 동일한 의존성을 사용하도록 하면 워크스페이스의 크레이트가 항상 서로 호환됩니다. *add_one/Cargo.toml* 파일의 `[dependencies]` 섹션에 `rand` 크레이트를 추가하여 `add_one` 크레이트에서 `rand` 크레이트를 사용할 수 있습니다:

```toml
[dependencies]
rand = "0.8.5"
```

이제 *add_one/src/lib.rs* 파일에 `use rand;`를 추가할 수 있으며 *add* 디렉토리에서 `cargo build`를 실행하여 전체 워크스페이스를 빌드하면 `rand` 크레이트를 가져와서 컴파일합니다. 범위로 가져온 `rand`를 참조하지 않기 때문에 경고가 하나 발생합니다:

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

최상위 *Cargo.lock*에는 이제 `add_one`의 `rand` 의존성에 대한 정보가 포함됩니다. 그러나 `rand`가 워크스페이스 어딘가에서 사용되더라도 자체 *Cargo.toml* 파일에도 `rand`를 추가하지 않으면 워크스페이스의 다른 크레이트에서 사용할 수 없습니다. 예를 들어 `adder` 패키지의 *adder/src/main.rs* 파일에 `use rand;`를 추가하면 오류가 발생합니다:

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

이를 수정하려면 `adder` 패키지의 *Cargo.toml* 파일을 편집하여 `rand`도 의존성임을 표시합니다. `adder` 패키지를 빌드하면 *Cargo.lock*의 `adder` 의존성 목록에 `rand`가 추가되지만 `rand`의 추가 복사본은 다운로드되지 않습니다. Cargo는 `rand` 패키지를 사용하는 워크스페이스의 모든 패키지가 동일한 버전을 사용하도록 보장하여 공간을 절약하고 워크스페이스의 크레이트가 서로 호환되도록 합니다.

#### 워크스페이스에 테스트 추가하기

또 다른 개선 사항으로 `add_one` 크레이트 내에 `add_one::add_one` 함수의 테스트를 추가해 보겠습니다:

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

이제 최상위 *add* 디렉토리에서 `cargo test`를 실행합니다. 이와 같이 구조화된 워크스페이스에서 `cargo test`를 실행하면 워크스페이스의 모든 크레이트에 대한 테스트가 실행됩니다:

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

출력의 첫 번째 섹션은 `add_one` 크레이트의 `it_works` 테스트가 통과했음을 보여줍니다. 다음 섹션은 `adder` 크레이트에서 테스트가 0개 발견되었음을 보여주고 마지막 섹션은 `add_one` 크레이트에서 문서 테스트가 0개 발견되었음을 보여줍니다.

`-p` 플래그를 사용하고 테스트하려는 크레이트 이름을 지정하여 최상위 디렉토리에서 워크스페이스의 특정 크레이트에 대한 테스트를 실행할 수도 있습니다:

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

이 출력은 `cargo test`가 `add_one` 크레이트에 대한 테스트만 실행하고 `adder` 크레이트 테스트는 실행하지 않았음을 보여줍니다.

워크스페이스의 크레이트를 [crates.io](https://crates.io/)에 게시하는 경우 워크스페이스의 각 크레이트는 별도로 게시해야 합니다. `cargo test`와 마찬가지로 `-p` 플래그를 사용하고 게시하려는 크레이트 이름을 지정하여 워크스페이스의 특정 크레이트를 게시할 수 있습니다.

추가 연습을 위해 `add_one` 크레이트와 유사한 방식으로 이 워크스페이스에 `add_two` 크레이트를 추가하세요!

프로젝트가 성장함에 따라 워크스페이스 사용을 고려하세요: 하나의 큰 코드 덩어리보다 더 작은 개별 구성 요소를 이해하기가 더 쉽습니다. 또한 워크스페이스에 크레이트를 유지하면 자주 동시에 변경되는 경우 크레이트 간의 조정이 더 쉬워질 수 있습니다.

---

## `cargo install`로 Crates.io에서 바이너리 설치하기

> **원문:** https://doc.rust-lang.org/book/ch14-04-installing-binaries.html

`cargo install` 명령을 사용하면 바이너리 크레이트를 로컬에 설치하고 사용할 수 있습니다. 이것은 시스템 패키지를 대체하려는 것이 아니라, Rust 개발자가 [crates.io](https://crates.io/)에서 다른 사람들이 공유한 도구를 편리하게 설치할 수 있는 방법입니다. 바이너리 대상이 있는 패키지만 설치할 수 있다는 점에 유의하세요. *바이너리 대상*은 크레이트에 *src/main.rs* 파일이 있거나 바이너리로 지정된 다른 파일이 있는 경우 생성되는 실행 가능한 프로그램입니다. 이는 자체적으로 실행할 수 없지만 다른 프로그램에 포함하기에 적합한 라이브러리 대상과 대조됩니다. 일반적으로 크레이트에는 *README* 파일에 크레이트가 라이브러리인지, 바이너리 대상이 있는지 또는 둘 다인지에 대한 정보가 있습니다.

`cargo install`로 설치된 모든 바이너리는 설치 루트의 *bin* 폴더에 저장됩니다. *rustup.rs*를 사용하여 Rust를 설치했고 사용자 정의 구성이 없는 경우 이 디렉토리는 `$HOME/.cargo/bin`입니다. `cargo install`로 설치한 프로그램을 실행하려면 해당 디렉토리가 `$PATH`에 있는지 확인하세요.

예를 들어 12장에서 파일 검색을 위한 `grep` 도구의 Rust 구현인 `ripgrep`이 있다고 언급했습니다. `ripgrep`을 설치하려면 다음을 실행할 수 있습니다:

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

출력의 마지막에서 두 번째 줄은 설치된 바이너리의 위치와 이름을 보여줍니다. `ripgrep`의 경우 `rg`입니다. 앞서 언급한 대로 설치 디렉토리가 `$PATH`에 있으면 `rg --help`를 실행하고 더 빠르고 Rust 스러운 파일 검색 도구를 사용할 수 있습니다!

---

## 사용자 정의 명령으로 Cargo 확장하기

> **원문:** https://doc.rust-lang.org/book/ch14-05-extending-cargo.html

Cargo는 수정 없이 새 하위 명령으로 확장할 수 있도록 설계되었습니다. `$PATH`에 있는 바이너리 이름이 `cargo-something`이면 `cargo something`을 실행하여 마치 Cargo 하위 명령인 것처럼 실행할 수 있습니다. 이와 같은 사용자 정의 명령은 `cargo --list`를 실행할 때도 나열됩니다. `cargo install`을 사용하여 확장을 설치한 다음 내장 Cargo 도구처럼 실행할 수 있는 것은 Cargo 디자인의 매우 편리한 이점입니다!

### 요약

Cargo와 [crates.io](https://crates.io/)로 코드를 공유하는 것은 Rust 생태계를 다양한 작업에 유용하게 만드는 부분입니다. Rust의 표준 라이브러리는 작고 안정적이지만, 크레이트는 언어와 다른 타임라인으로 공유, 사용 및 개선하기 쉽습니다. [crates.io](https://crates.io/)에서 유용한 코드를 공유하는 것을 부끄러워하지 마세요; 다른 누군가에게도 유용할 가능성이 높습니다!
