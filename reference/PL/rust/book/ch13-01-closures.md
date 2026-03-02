# 클로저: 환경을 캡처하는 익명 함수

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
