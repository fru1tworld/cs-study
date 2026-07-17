# 함수형 언어의 특징: 이터레이터와 클로저

# 함수형 언어 기능: 반복자와 클로저

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

# 클로저: 환경을 캡처하는 익명 함수

> **원문:** https://doc.rust-lang.org/book/ch13-01-closures.html

Rust의 클로저는 변수에 저장하거나 다른 함수에 인수로 전달할 수 있는 익명 함수입니다. 한 곳에서 클로저를 만들고 다른 컨텍스트에서 호출하여 평가할 수 있습니다. 일반 함수와 달리, 클로저는 정의된 스코프의 값을 캡처할 수 있습니다. 이러한 클로저 기능이 어떻게 코드 재사용과 동작 사용자 정의를 가능하게 하는지 보여드리겠습니다.

## 클로저로 환경 캡처하기

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

## 클로저 타입 추론과 어노테이션

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

## 참조 캡처 또는 소유권 이동

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

## 캡처된 값을 클로저 밖으로 이동하기와 `Fn` 트레이트

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

# 반복자로 일련의 항목 처리하기

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

## `Iterator` 트레이트와 `next` 메서드

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

## 반복자를 소비하는 메서드

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

## 다른 반복자를 생성하는 메서드

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

## 환경을 캡처하는 클로저 사용하기

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

# 반복자를 사용하여 I/O 프로젝트 개선하기

> **원문:** https://doc.rust-lang.org/book/ch13-03-improving-our-io-project.html

반복자에 대한 새로운 지식을 바탕으로 12장의 I/O 프로젝트를 반복자를 사용하여 코드의 여러 부분을 더 명확하고 간결하게 만들어 개선할 수 있습니다. `Config::build` 함수와 `search` 함수의 구현을 반복자가 어떻게 개선할 수 있는지 살펴보겠습니다.

## 반복자를 사용하여 `clone` 제거하기

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

### 반환된 반복자 직접 사용하기

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

### 인덱싱 대신 `Iterator` 트레이트 메서드 사용하기

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

## 반복자 어댑터로 코드 더 명확하게 만들기

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

## 루프와 반복자 중 선택하기

다음 논리적 질문은 자신의 코드에서 어떤 스타일을 선택해야 하고 그 이유가 무엇인지입니다: Listing 13-21의 원래 구현 또는 Listing 13-22의 반복자를 사용하는 버전. 대부분의 Rust 프로그래머는 반복자 스타일을 선호합니다. 처음에는 익숙해지기가 좀 더 어렵지만, 다양한 반복자 어댑터와 이들이 수행하는 작업에 대한 감각을 얻으면 반복자가 이해하기 더 쉬울 수 있습니다. 루핑과 새 벡터 생성의 다양한 부분을 만지작거리는 대신, 코드는 루프의 고수준 목적에 집중합니다. 이것은 일부 일반적인 코드를 추상화하여 이 코드에 고유한 개념, 즉 반복자의 각 요소가 통과해야 하는 필터링 조건을 더 쉽게 볼 수 있게 합니다.

그러나 두 구현이 정말 동등할까요? 직관적인 가정은 더 저수준 루프가 더 빠를 것이라는 것입니다. 성능에 대해 이야기해 봅시다.

---

# 성능 비교: 루프 vs. 반복자

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

## 요약

클로저와 반복자는 함수형 프로그래밍 언어 아이디어에서 영감을 받은 Rust 기능입니다. 이들은 저수준 성능으로 고수준 아이디어를 명확하게 표현하는 Rust의 능력에 기여합니다. 클로저와 반복자의 구현은 런타임 성능이 영향을 받지 않도록 합니다. 이것은 제로 비용 추상화를 제공하기 위해 노력하는 Rust의 목표의 일부입니다.

이제 I/O 프로젝트의 표현력을 개선했으니 프로젝트를 세상과 공유하는 데 도움이 될 `cargo`의 몇 가지 기능을 더 살펴보겠습니다.
