# 스마트 포인터

# 스마트 포인터

> **원문:** https://doc.rust-lang.org/book/ch15-00-smart-pointers.html

*포인터*는 메모리의 주소를 포함하는 변수에 대한 일반적인 개념입니다. 이 주소는 다른 데이터를 참조하거나 "가리킵니다". Rust에서 가장 일반적인 종류의 포인터는 4장에서 배운 참조입니다. 참조는 `&` 기호로 표시되고 가리키는 값을 빌립니다. 데이터를 참조하는 것 외에는 특별한 기능이 없으며 오버헤드도 없습니다.

반면에 *스마트 포인터*는 포인터처럼 작동하지만 추가 메타데이터와 기능도 있는 데이터 구조입니다. 스마트 포인터의 개념은 Rust에 고유한 것이 아닙니다: 스마트 포인터는 C++에서 시작되었으며 다른 언어에도 존재합니다. Rust는 표준 라이브러리에 정의된 다양한 스마트 포인터가 있으며, 참조가 제공하는 것 이상의 기능을 제공합니다. 일반적인 개념을 탐구하기 위해 *참조 카운팅* 스마트 포인터 타입을 포함한 몇 가지 다른 예를 살펴보겠습니다. 이 포인터를 사용하면 데이터에 대한 여러 소유자를 허용할 수 있으며, 소유자 수를 추적하고 소유자가 남아 있지 않으면 데이터를 정리합니다.

빌림의 개념이 있는 Rust에서 참조와 스마트 포인터 사이의 추가적인 차이점은 참조가 데이터만 빌리는 반면, 많은 경우 스마트 포인터는 가리키는 데이터를 *소유*한다는 것입니다.

이 책에서 이미 몇 가지 스마트 포인터를 접했지만 그때는 그렇게 부르지 않았습니다. 8장의 `String`과 `Vec<T>`를 포함합니다. 이러한 타입은 둘 다 일부 메모리를 소유하고 조작할 수 있기 때문에 스마트 포인터로 간주됩니다. 또한 메타데이터와 추가 기능 또는 보장이 있습니다. 예를 들어 `String`은 용량을 메타데이터로 저장하고 데이터가 항상 유효한 UTF-8이 되도록 하는 추가 기능이 있습니다.

스마트 포인터는 일반적으로 구조체를 사용하여 구현됩니다. 일반 구조체와 달리 스마트 포인터는 `Deref` 및 `Drop` 트레이트를 구현합니다. `Deref` 트레이트는 스마트 포인터 구조체의 인스턴스가 참조처럼 동작하도록 하여 참조 또는 스마트 포인터와 함께 작동하는 코드를 작성할 수 있습니다. `Drop` 트레이트는 스마트 포인터 인스턴스가 범위를 벗어날 때 실행되는 코드를 사용자 정의할 수 있습니다. 이 장에서는 두 트레이트를 모두 논의하고 스마트 포인터에 중요한 이유를 보여드리겠습니다.

스마트 포인터 패턴이 Rust에서 자주 사용되는 일반적인 디자인 패턴이라는 점을 감안할 때 이 장에서 모든 기존 스마트 포인터를 다루지는 않습니다. 많은 라이브러리에는 자체 스마트 포인터가 있으며 직접 작성할 수도 있습니다. 표준 라이브러리에서 가장 일반적인 스마트 포인터를 다룹니다:

* 힙에 값을 할당하기 위한 `Box<T>`
* 여러 소유권을 가능하게 하는 참조 카운팅 타입인 `Rc<T>`
* `RefCell<T>`를 통해 액세스되고 컴파일 시간이 아닌 런타임에 빌림 규칙을 적용하는 타입인 `Ref<T>` 및 `RefMut<T>`

또한 불변 타입이 내부 값을 변경하기 위한 API를 노출하는 *내부 가변성* 패턴을 다룹니다. 또한 메모리 누수를 유발할 수 있는 *참조 순환*과 이를 방지하는 방법에 대해서도 논의합니다.

시작해 봅시다!

---

# `Box<T>`를 사용하여 힙의 데이터 가리키기

> **원문:** https://doc.rust-lang.org/book/ch15-01-box.html

가장 직접적인 스마트 포인터는 *박스*이며, 타입은 `Box<T>`로 작성됩니다. 박스를 사용하면 스택이 아닌 힙에 데이터를 저장할 수 있습니다. 스택에 남는 것은 힙 데이터에 대한 포인터입니다. 스택과 힙의 차이점을 검토하려면 4장을 참조하세요.

박스는 힙에 데이터를 저장하는 것 외에는 성능 오버헤드가 없습니다. 그러나 많은 추가 기능도 없습니다. 다음 상황에서 가장 자주 사용합니다:

* 컴파일 시간에 크기를 알 수 없는 타입이 있고 정확한 크기를 필요로 하는 컨텍스트에서 해당 타입의 값을 사용하려는 경우
* 큰 양의 데이터가 있고 소유권을 이전하고 싶지만 이전할 때 데이터가 복사되지 않도록 하려는 경우
* 값을 소유하고 특정 타입이 아닌 특정 트레이트를 구현하는 타입이라는 것만 신경 쓰려는 경우

첫 번째 상황은 "박스로 재귀 타입 활성화하기" 섹션에서 보여드리겠습니다. 두 번째 경우에서 큰 양의 데이터 소유권을 이전하면 데이터가 스택에서 복사되기 때문에 오랜 시간이 걸릴 수 있습니다. 이 상황에서 성능을 향상시키기 위해 힙의 박스에 큰 양의 데이터를 저장할 수 있습니다. 그런 다음 소유권이 이전될 때 작은 양의 포인터 데이터만 스택에서 복사되고 데이터가 참조하는 데이터는 힙의 한 곳에 유지됩니다. 세 번째 경우는 *트레이트 객체*로 알려져 있으며 18장에서 전체 섹션을 다룹니다. 따라서 여기서 배운 것은 18장에서 다시 적용될 것입니다!

## `Box<T>`를 사용하여 힙에 데이터 저장하기

`Box<T>`의 힙 저장 사용 사례를 논의하기 전에 구문과 `Box<T>` 내에 저장된 값과 상호 작용하는 방법을 다루겠습니다.

Listing 15-1은 박스를 사용하여 힙에 `i32` 값을 저장하는 방법을 보여줍니다:

```rust
fn main() {
    let b = Box::new(5);
    println!("b = {b}");
}
```

*Listing 15-1: 박스를 사용하여 힙에 `i32` 값 저장하기*

변수 `b`를 힙에 할당된 값 `5`를 가리키는 `Box`의 값을 갖도록 정의합니다. 이 프로그램은 `b = 5`를 출력합니다; 이 경우 데이터가 스택에 있는 것처럼 박스의 데이터에 액세스할 수 있습니다. 소유된 값과 마찬가지로 `main` 끝에서처럼 박스가 범위를 벗어나면 할당 해제됩니다. 할당 해제는 박스(스택에 저장됨)와 박스가 가리키는 데이터(힙에 저장됨) 모두에 대해 발생합니다.

힙에 단일 값을 넣는 것은 그다지 유용하지 않으므로 이런 식으로 박스를 자주 사용하지 않을 것입니다. 기본적으로 스택에 저장되는 단일 `i32`와 같은 값을 갖는 것이 대부분의 상황에서 더 적합합니다. 박스를 정의하지 않으면 정의할 수 없는 타입을 박스가 정의하도록 하는 경우를 살펴보겠습니다.

## 박스로 재귀 타입 활성화하기

*재귀 타입*의 값은 자체 타입의 다른 값을 자신의 일부로 가질 수 있습니다. 재귀 타입은 Rust가 컴파일 시간에 타입이 차지하는 공간을 알아야 하기 때문에 문제가 됩니다. 그러나 재귀 타입의 값 중첩은 이론적으로 무한히 계속될 수 있으므로 Rust는 값에 필요한 공간이 얼마인지 알 수 없습니다. 박스는 알려진 크기를 가지므로 재귀 타입 정의에 박스를 삽입하여 재귀 타입을 가질 수 있습니다.

재귀 타입의 예로 *cons 리스트*를 살펴보겠습니다. 이것은 함수형 프로그래밍 언어에서 흔히 볼 수 있는 데이터 타입입니다. 우리가 정의할 cons 리스트 타입은 재귀를 제외하면 간단합니다; 따라서 우리가 작업할 예제의 개념은 재귀 타입과 관련된 더 복잡한 상황에 처할 때마다 유용할 것입니다.

### Cons 리스트에 대한 자세한 정보

*cons 리스트*는 Lisp 프로그래밍 언어와 그 방언에서 유래한 데이터 구조이며 중첩된 쌍으로 구성됩니다. 그 이름은 Lisp의 `cons` 함수("construct function"의 약자)에서 유래했으며, 이 함수는 두 인수로 새로운 쌍을 구성합니다. 값과 다른 쌍으로 구성된 쌍에서 `cons`를 호출하면 재귀 쌍으로 구성된 cons 리스트를 형성할 수 있습니다.

예를 들어, 다음은 1, 2, 3을 포함하는 리스트의 의사 코드 표현입니다. 각 쌍은 괄호 안에 있습니다:

```text
(1, (2, (3, Nil)))
```

cons 리스트의 각 항목에는 현재 항목의 값과 다음 항목이라는 두 요소가 포함됩니다. 리스트의 마지막 항목에는 다음 항목 없이 `Nil`이라는 값만 포함됩니다. cons 리스트는 `cons` 함수를 재귀적으로 호출하여 생성됩니다. 재귀의 기본 케이스를 나타내는 표준 이름은 `Nil`입니다. 이것은 6장의 "null" 또는 "nil" 개념과 동일하지 않으며, 유효하지 않거나 부재한 값입니다.

cons 리스트는 Rust에서 자주 사용되는 데이터 구조가 아닙니다. Rust에서 항목 리스트가 있는 경우 대부분 `Vec<T>`가 더 나은 선택입니다. 다른 더 복잡한 재귀 데이터 타입은 다양한 상황에서 *유용하지만*, 이 장에서 cons 리스트로 시작하면 박스가 큰 혼란 없이 재귀 데이터 타입을 정의하도록 하는 방법을 탐구할 수 있습니다.

Listing 15-2에는 cons 리스트에 대한 열거형 정의가 포함되어 있습니다. 이 코드는 아직 컴파일되지 않습니다. `List` 타입의 크기가 알려져 있지 않기 때문입니다. 이에 대해서는 나중에 설명하겠습니다.

```rust
enum List {
    Cons(i32, List),
    Nil,
}
```

*Listing 15-2: `i32` 값의 cons 리스트 데이터 구조를 나타내는 열거형을 정의하는 첫 번째 시도*

> 참고: 이 예제의 목적을 위해 `i32` 값만 보유하는 cons 리스트를 구현하고 있습니다. 10장에서 논의한 대로 제네릭을 사용하여 모든 타입의 값을 저장하는 cons 리스트 타입을 정의할 수 있습니다.

`List` 타입을 사용하여 1, 2, 3 리스트를 저장하면 Listing 15-3의 코드처럼 보일 것입니다:

```rust
use crate::List::{Cons, Nil};

fn main() {
    let list = Cons(1, Cons(2, Cons(3, Nil)));
}
```

*Listing 15-3: `List` 열거형을 사용하여 1, 2, 3 리스트 저장하기*

첫 번째 `Cons` 값은 `1`과 다른 `List` 값을 보유합니다. 이 `List` 값은 `2`와 다른 `List` 값을 보유하는 또 다른 `Cons` 값입니다. 이 `List` 값은 `3`과 마지막으로 리스트의 끝을 나타내는 비재귀 변형인 `Nil`인 `List` 값을 보유하는 또 다른 `Cons` 값입니다.

Listing 15-3의 코드를 컴파일하려고 하면 Listing 15-4에 표시된 오류가 발생합니다:

```console
$ cargo run
   Compiling cons-list v0.1.0 (file:///projects/cons-list)
error[E0072]: recursive type `List` has infinite size
 --> src/main.rs:1:1
  |
1 | enum List {
  | ^^^^^^^^^
2 |     Cons(i32, List),
  |               ---- recursive without indirection
  |
help: insert some indirection (e.g., a `Box`, `Rc`, or `&`) to break the cycle
  |
2 |     Cons(i32, Box<List>),
  |               ++++    +
```

*Listing 15-4: 재귀 열거형을 정의하려고 할 때 발생하는 오류*

오류는 이 타입이 "무한 크기"가 있음을 보여줍니다. 그 이유는 재귀 변형을 가진 `List`를 정의했기 때문입니다: 자체 타입의 다른 값을 직접 보유합니다. 결과적으로 Rust는 `List` 값을 저장하는 데 필요한 공간을 파악할 수 없습니다. 이 오류가 발생하는 이유를 분석해 보겠습니다. 먼저 Rust가 비재귀 타입의 값을 저장하는 데 필요한 공간을 결정하는 방법을 살펴보겠습니다.

### 비재귀 타입의 크기 계산하기

6장에서 열거형 정의를 논의할 때 Listing 6-2에서 정의한 `Message` 열거형을 기억해 보세요:

```rust
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}
```

`Message` 값에 할당할 공간이 얼마나 필요한지 결정하기 위해 Rust는 각 변형을 살펴보고 가장 많은 공간이 필요한 변형을 확인합니다. Rust는 `Message::Quit`에 공간이 필요하지 않고, `Message::Move`에 두 `i32` 값을 저장할 충분한 공간이 필요하다는 것을 알 수 있습니다. 하나의 변형만 사용되므로 `Message` 값에 필요한 가장 많은 공간은 가장 큰 변형 중 하나를 저장하는 데 필요한 공간입니다.

Rust가 Listing 15-2의 `List` 열거형과 같은 재귀 타입에 필요한 공간을 결정하려고 할 때 어떤 일이 발생하는지 대조해 보세요. 컴파일러는 `Cons` 변형을 살펴보기 시작합니다. 이 변형은 `i32` 타입의 값과 `List` 타입의 값을 보유합니다. 따라서 `Cons`는 `i32`의 크기에 `List`의 크기를 더한 공간이 필요합니다. `List` 타입에 필요한 메모리 양을 파악하기 위해 컴파일러는 `Cons` 변형부터 시작하여 변형을 살펴봅니다. `Cons` 변형은 `i32` 타입의 값과 `List` 타입의 값을 보유하며 이 프로세스는 무한히 계속됩니다.

### 알려진 크기의 재귀 타입을 얻기 위해 `Box<T>` 사용하기

Rust는 재귀적으로 정의된 타입에 할당할 공간을 파악할 수 없으므로 컴파일러는 다음과 같은 유용한 제안과 함께 오류를 제공합니다:

```text
help: insert some indirection (e.g., a `Box`, `Rc`, or `&`) to break the cycle
  |
2 |     Cons(i32, Box<List>),
  |               ++++    +
```

이 제안에서 "간접"은 값을 직접 저장하는 대신 값에 대한 포인터를 저장하여 데이터 구조를 변경해야 함을 의미합니다.

`Box<T>`는 포인터이기 때문에 Rust는 항상 `Box<T>`에 필요한 공간을 알고 있습니다: 포인터의 크기는 가리키는 데이터의 양에 따라 변경되지 않습니다. 이것은 `Cons` 변형에 직접 다른 `List` 값 대신 `Box<T>`를 넣을 수 있음을 의미합니다. `Box<T>`는 `Cons` 변형 안이 아닌 힙에 있을 다음 `List` 값을 가리킵니다. 개념적으로 여전히 다른 리스트를 보유하는 리스트로 만들어진 리스트가 있습니다. 그러나 이 구현은 이제 항목이 서로 안에 있기보다 서로 옆에 배치되는 것과 더 비슷합니다.

Listing 15-2의 `List` 열거형 정의와 Listing 15-3의 `List` 사용을 Listing 15-5의 코드로 변경할 수 있으며 컴파일됩니다:

```rust
enum List {
    Cons(i32, Box<List>),
    Nil,
}

use crate::List::{Cons, Nil};

fn main() {
    let list = Cons(1, Box::new(Cons(2, Box::new(Cons(3, Box::new(Nil))))));
}
```

*Listing 15-5: 알려진 크기를 갖기 위해 `Box<T>`를 사용하는 `List` 정의*

`Cons` 변형에는 `i32`의 크기에 박스의 포인터 데이터를 저장할 공간이 필요합니다. `Nil` 변형은 값을 저장하지 않으므로 `Cons` 변형보다 공간이 덜 필요합니다. 이제 모든 `List` 값이 `i32`의 크기에 박스의 포인터 데이터 크기를 더한 것임을 알 수 있습니다. 박스를 사용하여 무한 재귀 체인을 끊었으므로 컴파일러는 `List` 값을 저장하는 데 필요한 크기를 파악할 수 있습니다.

박스는 간접과 힙 할당만 제공합니다; 다른 스마트 포인터 타입에서 볼 수 있는 것과 같은 다른 특별한 기능은 없습니다. 또한 이러한 특별한 기능으로 인해 발생하는 성능 오버헤드도 없으므로 cons 리스트와 같이 간접만 필요한 기능인 경우에 유용할 수 있습니다. 17장에서 박스의 더 많은 사용 사례를 살펴보겠습니다.

`Box<T>` 타입은 `Deref` 트레이트를 구현하기 때문에 스마트 포인터입니다. 이를 통해 `Box<T>` 값을 참조처럼 처리할 수 있습니다. `Box<T>` 값이 범위를 벗어나면 박스가 가리키는 힙 데이터도 `Drop` 트레이트 구현으로 인해 정리됩니다. 이 두 트레이트는 이 장의 나머지 부분에서 논의할 다른 스마트 포인터 타입이 제공하는 기능에 더욱 중요할 것입니다. 이 두 트레이트를 더 자세히 살펴보겠습니다.

---

# `Deref` 트레이트로 스마트 포인터를 일반 참조처럼 취급하기

> **원문:** https://doc.rust-lang.org/book/ch15-02-deref.html

`Deref` 트레이트를 구현하면 *역참조 연산자* `*`의 동작을 사용자 정의할 수 있습니다(곱셈이나 글롭 연산자와 혼동하지 마세요). 스마트 포인터가 일반 참조처럼 취급될 수 있도록 `Deref`를 구현하면 참조에서 작동하는 코드를 작성하고 해당 코드를 스마트 포인터와 함께 사용할 수도 있습니다.

먼저 역참조 연산자가 일반 참조와 어떻게 작동하는지 살펴보겠습니다. 그런 다음 `Box<T>`처럼 동작하는 사용자 정의 타입을 정의하고 새로 정의한 타입에서 역참조 연산자가 작동하지 않는 이유를 살펴보겠습니다. `Deref` 트레이트를 구현하면 스마트 포인터가 참조와 유사한 방식으로 작동할 수 있는 방법을 살펴볼 것입니다. 그런 다음 Rust의 *역참조 강제 변환* 기능과 참조 또는 스마트 포인터와 함께 작동하는 방법을 살펴보겠습니다.

> 참고: 빌드할 `MyBox<T>` 타입과 실제 `Box<T>` 사이에는 큰 차이가 있습니다: 우리 버전은 데이터를 힙에 저장하지 않습니다. 이 예제를 `Deref`에 집중하고 있으므로 데이터가 실제로 어디에 저장되는지는 포인터와 같은 동작보다 덜 중요합니다.

## 포인터를 따라가서 값 얻기

일반 참조는 포인터의 한 유형이며 포인터를 생각하는 한 가지 방법은 다른 곳에 저장된 값을 가리키는 화살표로 생각하는 것입니다. Listing 15-6에서 `i32` 값에 대한 참조를 만든 다음 역참조 연산자를 사용하여 참조를 따라 값으로 이동합니다:

```rust
fn main() {
    let x = 5;
    let y = &x;

    assert_eq!(5, x);
    assert_eq!(5, *y);
}
```

*Listing 15-6: 역참조 연산자를 사용하여 `i32` 값에 대한 참조 따라가기*

변수 `x`는 `i32` 값 `5`를 보유합니다. `y`를 `x`에 대한 참조와 같게 설정합니다. `x`가 `5`와 같다고 assert할 수 있습니다. 그러나 `y`의 값에 대해 assertion을 하려면 `*y`를 사용하여 참조가 가리키는 값으로 따라가야 합니다(따라서 *역참조*). `y`를 역참조하면 `5`와 비교할 수 있는 `y`가 가리키는 정수 값에 액세스할 수 있습니다.

대신 `assert_eq!(5, y);`를 작성하려고 하면 이 컴파일 오류가 발생합니다:

```console
$ cargo run
   Compiling deref-example v0.1.0 (file:///projects/deref-example)
error[E0277]: can't compare `{integer}` with `&{integer}`
 --> src/main.rs:6:5
  |
6 |     assert_eq!(5, y);
  |     ^^^^^^^^^^^^^^^^ no implementation for `{integer} == &{integer}`
```

숫자와 숫자에 대한 참조를 비교하는 것은 다른 타입이기 때문에 허용되지 않습니다. 역참조 연산자를 사용하여 참조가 가리키는 값으로 따라가야 합니다.

## `Box<T>`를 참조처럼 사용하기

Listing 15-6의 코드를 참조 대신 `Box<T>`를 사용하도록 다시 작성할 수 있습니다; Listing 15-7에서 `Box<T>`에 사용된 역참조 연산자는 Listing 15-6의 참조에 사용된 역참조 연산자와 동일한 방식으로 작동합니다:

```rust
fn main() {
    let x = 5;
    let y = Box::new(x);

    assert_eq!(5, x);
    assert_eq!(5, *y);
}
```

*Listing 15-7: `Box<i32>`에 역참조 연산자 사용하기*

Listing 15-7과 Listing 15-6 사이의 주요 차이점은 여기서 `y`를 `x`의 값을 가리키는 `Box<T>`의 인스턴스로 설정한다는 것입니다. 이는 `x`의 복사된 값을 가리키는 것입니다. 마지막 assertion에서 박스의 포인터가 가리키는 값을 따라가기 위해 `y`가 참조일 때와 동일한 방식으로 역참조 연산자를 사용할 수 있습니다. 다음으로, 자체 타입을 정의하여 `Box<T>`가 역참조 연산자를 사용할 수 있게 하는 특별한 점이 무엇인지 살펴보겠습니다.

## 자체 스마트 포인터 정의하기

표준 라이브러리에서 제공하는 `Box<T>` 타입과 유사한 스마트 포인터를 빌드하여 스마트 포인터가 기본적으로 참조와 다르게 동작하는 방식을 경험해 봅시다. 그런 다음 역참조 연산자를 사용하는 기능을 추가하는 방법을 살펴보겠습니다.

`Box<T>` 타입은 궁극적으로 하나의 요소가 있는 튜플 구조체로 정의되므로 Listing 15-8은 `MyBox<T>` 타입을 동일한 방식으로 정의합니다. 또한 `Box<T>`에 정의된 `new` 함수와 일치하는 `new` 함수를 정의합니다.

```rust
struct MyBox<T>(T);

impl<T> MyBox<T> {
    fn new(x: T) -> MyBox<T> {
        MyBox(x)
    }
}
```

*Listing 15-8: `MyBox<T>` 타입 정의하기*

`MyBox`라는 구조체를 정의하고 제네릭 매개변수 `T`를 선언합니다. 타입이 모든 타입의 값을 보유할 수 있도록 하기 때문입니다. `MyBox` 타입은 `T` 타입의 하나의 요소를 가진 튜플 구조체입니다. `MyBox::new` 함수는 `T` 타입의 매개변수 하나를 받아 해당 값을 보유하는 `MyBox` 인스턴스를 반환합니다.

Listing 15-7의 `main` 함수를 Listing 15-8에 추가하고 `Box<T>` 대신 우리가 정의한 `MyBox<T>` 타입을 사용하도록 변경해 봅시다. Listing 15-9의 코드는 Rust가 `MyBox`를 역참조하는 방법을 모르기 때문에 컴파일되지 않습니다.

```rust
fn main() {
    let x = 5;
    let y = MyBox::new(x);

    assert_eq!(5, x);
    assert_eq!(5, *y);
}
```

*Listing 15-9: 참조와 `Box<T>`를 사용한 것과 같은 방식으로 `MyBox<T>` 사용 시도하기*

다음은 결과 컴파일 오류입니다:

```console
$ cargo run
   Compiling deref-example v0.1.0 (file:///projects/deref-example)
error[E0614]: type `MyBox<{integer}>` cannot be dereferenced
  --> src/main.rs:14:19
   |
14 |     assert_eq!(5, *y);
   |                   ^^
```

`MyBox<T>` 타입은 역참조할 수 없습니다. 해당 기능을 타입에 구현하지 않았기 때문입니다. `*` 연산자로 역참조를 활성화하려면 `Deref` 트레이트를 구현합니다.

## `Deref` 트레이트를 구현하여 타입을 참조처럼 취급하기

10장의 "타입에 트레이트 구현하기" 섹션에서 논의한 것처럼 트레이트를 구현하려면 트레이트의 필수 메서드에 대한 구현을 제공해야 합니다. 표준 라이브러리에서 제공하는 `Deref` 트레이트는 `self`를 빌리고 내부 데이터에 대한 참조를 반환하는 `deref`라는 메서드 하나를 구현해야 합니다. Listing 15-10에는 `MyBox`의 정의에 추가할 `Deref`의 구현이 포함되어 있습니다:

```rust
use std::ops::Deref;

impl<T> Deref for MyBox<T> {
    type Target = T;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}
```

*Listing 15-10: `MyBox<T>`에 `Deref` 구현하기*

`type Target = T;` 구문은 `Deref` 트레이트가 사용할 연관 타입을 정의합니다. 연관 타입은 제네릭 매개변수를 선언하는 약간 다른 방법이지만 지금은 걱정할 필요가 없습니다; 20장에서 더 자세히 다룰 것입니다.

`deref` 메서드 본문을 `&self.0`으로 채워 `deref`가 `*` 연산자로 액세스하려는 값에 대한 참조를 반환하도록 합니다. 5장의 "이름 없는 필드가 있는 튜플 구조체를 사용하여 다른 타입 만들기" 섹션에서 `.0`이 튜플 구조체의 첫 번째 값에 액세스한다는 것을 기억하세요. Listing 15-9의 `MyBox<T>` 값에서 `*`를 호출하는 `main` 함수는 이제 컴파일되고 assertions가 통과합니다!

`Deref` 트레이트가 없으면 컴파일러는 `&` 참조만 역참조할 수 있습니다. `deref` 메서드는 컴파일러에게 `Deref`를 구현하는 모든 타입의 값을 받아 `deref` 메서드를 호출하여 역참조 방법을 아는 `&` 참조를 얻을 수 있는 기능을 제공합니다.

Listing 15-9에서 `*y`를 입력했을 때 Rust는 실제로 다음 코드를 실행했습니다:

```rust
*(y.deref())
```

Rust는 `*` 연산자를 `deref` 메서드 호출과 일반 역참조로 대체하므로 `deref` 메서드를 호출해야 하는지 여부를 생각할 필요가 없습니다. 이 Rust 기능을 사용하면 일반 참조가 있든 `Deref`를 구현하는 타입이 있든 동일하게 작동하는 코드를 작성할 수 있습니다.

`deref` 메서드가 값에 대한 참조를 반환하고 `*(y.deref())` 괄호 밖의 일반 역참조가 여전히 필요한 이유는 소유권 시스템 때문입니다. `deref` 메서드가 값에 대한 참조 대신 값을 직접 반환하면 값이 `self` 밖으로 이동됩니다. 이 경우나 역참조 연산자를 사용하는 대부분의 경우 `MyBox<T>` 내부 값의 소유권을 갖고 싶지 않습니다.

코드에서 `*`를 입력할 때마다 `*` 연산자가 `deref` 메서드 호출로 대체된 다음 `*`로 한 번 대체된다는 점에 유의하세요. `*` 연산자의 대체는 무한히 재귀하지 않으므로 `i32` 타입의 데이터가 됩니다. 이 데이터는 Listing 15-9의 `assert_eq!`에서 `5`와 일치합니다.

## 함수와 메서드에서 암묵적 역참조 강제 변환

*역참조 강제 변환*은 `Deref` 트레이트를 구현하는 타입에 대한 참조를 다른 타입에 대한 참조로 변환합니다. 예를 들어 역참조 강제 변환은 `&String`을 `&str`로 변환할 수 있습니다. `String`이 `&str`을 반환하도록 `Deref` 트레이트를 구현하기 때문입니다. 역참조 강제 변환은 Rust가 함수와 메서드에 대한 인수에 대해 수행하는 편의 기능이며 `Deref` 트레이트를 구현하는 타입에서만 작동합니다. 특정 타입의 값에 대한 참조를 함수 또는 메서드 정의의 매개변수 타입과 일치하지 않는 함수나 메서드에 인수로 전달할 때 자동으로 발생합니다. `deref` 메서드에 대한 일련의 호출은 제공한 타입을 매개변수에 필요한 타입으로 변환합니다.

역참조 강제 변환이 Rust에 추가되어 함수와 메서드 호출을 작성하는 프로그래머가 `&`와 `*`로 많은 명시적 참조와 역참조를 추가할 필요가 없습니다. 역참조 강제 변환 기능을 사용하면 참조 또는 스마트 포인터 모두에 대해 작동하는 더 많은 코드를 작성할 수 있습니다.

역참조 강제 변환이 실제로 작동하는 것을 보려면 Listing 15-8에서 정의한 `MyBox<T>` 타입과 Listing 15-10에서 추가한 `Deref`의 구현을 사용해 봅시다. Listing 15-11은 문자열 슬라이스 매개변수가 있는 함수의 정의를 보여줍니다:

```rust
fn hello(name: &str) {
    println!("안녕하세요, {name}님!");
}
```

*Listing 15-11: `&str` 타입의 매개변수 `name`이 있는 `hello` 함수*

예를 들어 `hello("Rust");`처럼 문자열 슬라이스를 인수로 `hello` 함수를 호출할 수 있습니다. 역참조 강제 변환을 사용하면 Listing 15-12에 표시된 대로 `MyBox<String>` 타입의 값에 대한 참조로 `hello`를 호출할 수 있습니다:

```rust
fn main() {
    let m = MyBox::new(String::from("Rust"));
    hello(&m);
}
```

*Listing 15-12: 역참조 강제 변환 때문에 작동하는 `MyBox<String>` 값에 대한 참조로 `hello` 호출하기*

여기서 `&m` 인수로 `hello` 함수를 호출하며, 이것은 `MyBox<String>` 값에 대한 참조입니다. Listing 15-10에서 `MyBox<T>`에 `Deref` 트레이트를 구현했기 때문에 Rust는 `deref`를 호출하여 `&MyBox<String>`을 `&String`으로 변환할 수 있습니다. 표준 라이브러리는 문자열 슬라이스를 반환하는 `String`에 `Deref` 구현을 제공하며 이것은 `Deref`의 API 문서에 있습니다. Rust는 `deref`를 다시 호출하여 `&String`을 `&str`로 변환하며, 이것은 `hello` 함수의 정의와 일치합니다.

Rust가 역참조 강제 변환을 구현하지 않았다면 Listing 15-12의 코드 대신 Listing 15-13의 코드를 작성하여 `&MyBox<String>` 타입의 값으로 `hello`를 호출해야 합니다.

```rust
fn main() {
    let m = MyBox::new(String::from("Rust"));
    hello(&(*m)[..]);
}
```

*Listing 15-13: Rust에 역참조 강제 변환이 없었다면 작성해야 했을 코드*

`(*m)`은 `MyBox<String>`을 `String`으로 역참조합니다. 그런 다음 `&`와 `[..]`는 전체 문자열과 같은 `String`의 문자열 슬라이스를 취하여 `hello`의 시그니처와 일치시킵니다. 역참조 강제 변환이 없는 이 코드는 관련된 모든 기호가 있어 읽고 쓰고 이해하기가 더 어렵습니다. 역참조 강제 변환을 사용하면 Rust가 이러한 변환을 자동으로 처리할 수 있습니다.

관련된 타입에 대해 `Deref` 트레이트가 정의되면 Rust는 타입을 분석하고 `Deref::deref`를 필요한 만큼 사용하여 매개변수 타입과 일치하는 참조를 얻습니다. `Deref::deref`가 삽입되어야 하는 횟수는 컴파일 시간에 해결되므로 역참조 강제 변환을 활용하는 데 런타임 페널티가 없습니다!

## 역참조 강제 변환이 가변성과 상호 작용하는 방식

불변 참조에서 `*` 연산자를 오버라이드하기 위해 `Deref` 트레이트를 사용하는 방법과 유사하게, `DerefMut` 트레이트를 사용하여 가변 참조에서 `*` 연산자를 오버라이드할 수 있습니다.

Rust는 다음 세 가지 경우에 타입과 트레이트 구현을 발견하면 역참조 강제 변환을 수행합니다:

* `T: Deref<Target=U>`일 때 `&T`에서 `&U`로
* `T: DerefMut<Target=U>`일 때 `&mut T`에서 `&mut U`로
* `T: Deref<Target=U>`일 때 `&mut T`에서 `&U`로

처음 두 경우는 가변성을 제외하면 동일합니다. 첫 번째 경우는 `&T`가 있고 `T`가 어떤 타입 `U`에 대해 `Deref`를 구현하면 `&U`를 투명하게 얻을 수 있다고 말합니다. 두 번째 경우는 가변 참조에 대해 동일한 역참조 강제 변환이 발생한다고 말합니다.

세 번째 경우는 더 까다롭습니다: Rust는 가변 참조를 불변 참조로도 강제 변환합니다. 그러나 그 반대는 *불가능*합니다: 불변 참조는 절대로 가변 참조로 강제 변환되지 않습니다. 빌림 규칙 때문에 가변 참조가 있으면 해당 가변 참조가 해당 데이터에 대한 유일한 참조여야 합니다(그렇지 않으면 프로그램이 컴파일되지 않습니다). 하나의 가변 참조를 하나의 불변 참조로 변환해도 빌림 규칙을 위반하지 않습니다. 불변 참조를 가변 참조로 변환하려면 해당 데이터에 대한 초기 불변 참조가 해당 불변 참조 하나만 있어야 하지만 빌림 규칙은 이를 보장하지 않습니다. 따라서 Rust는 불변 참조를 가변 참조로 변환하는 것이 가능하다고 가정할 수 없습니다.

---

# `Drop` 트레이트로 정리 시 코드 실행하기

> **원문:** https://doc.rust-lang.org/book/ch15-03-drop.html

스마트 포인터 패턴에서 중요한 두 번째 트레이트는 `Drop`입니다. 이를 통해 값이 범위를 벗어나려 할 때 무슨 일이 발생하는지 사용자 정의할 수 있습니다. 모든 타입에 `Drop` 트레이트의 구현을 제공할 수 있으며, 해당 코드를 파일이나 네트워크 연결과 같은 리소스를 해제하는 데 사용할 수 있습니다.

스마트 포인터의 맥락에서 `Drop`을 소개합니다. 왜냐하면 `Drop` 트레이트의 기능은 스마트 포인터를 구현할 때 거의 항상 사용되기 때문입니다. 예를 들어 `Box<T>`가 드롭되면 박스가 가리키는 힙의 공간을 할당 해제합니다.

일부 언어에서는 해당 언어의 스마트 포인터 인스턴스를 사용할 때마다 프로그래머가 메모리나 리소스를 해제하는 코드를 호출해야 합니다. 예로는 파일 핸들, 소켓, 또는 잠금이 있습니다. 잊어버리면 시스템이 과부하되어 충돌할 수 있습니다. Rust에서는 값이 범위를 벗어날 때마다 특정 코드 조각이 실행되도록 지정할 수 있으며, 컴파일러가 이 코드를 자동으로 삽입합니다. 결과적으로 특정 타입의 인스턴스가 사용을 마친 프로그램의 모든 곳에 정리 코드를 배치하는 것에 대해 주의를 기울일 필요가 없습니다—여전히 리소스가 누수되지 않습니다!

`Drop` 트레이트를 구현하여 값이 범위를 벗어날 때 실행할 코드를 지정합니다. `Drop` 트레이트는 `self`에 대한 가변 참조를 받는 `drop`이라는 메서드 하나를 구현해야 합니다. Rust가 `drop`을 호출하는 시점을 보려면 지금은 `println!` 문으로 `drop`을 구현해 봅시다.

Listing 15-14는 유일한 사용자 정의 기능이 해당 인스턴스가 범위를 벗어날 때 `CustomSmartPointer 드롭!`을 출력하여 Rust가 `drop` 함수를 실행하는 시점을 보여주는 `CustomSmartPointer` 구조체를 보여줍니다.

```rust
struct CustomSmartPointer {
    data: String,
}

impl Drop for CustomSmartPointer {
    fn drop(&mut self) {
        println!("데이터 `{}`로 CustomSmartPointer 드롭!", self.data);
    }
}

fn main() {
    let c = CustomSmartPointer {
        data: String::from("내 것"),
    };
    let d = CustomSmartPointer {
        data: String::from("다른 것"),
    };
    println!("CustomSmartPointers가 생성되었습니다.");
}
```

*Listing 15-14: 정리 코드를 넣을 `Drop` 트레이트를 구현하는 `CustomSmartPointer` 구조체*

`Drop` 트레이트는 프렐루드에 포함되어 있으므로 범위로 가져올 필요가 없습니다. `CustomSmartPointer`에 `Drop` 트레이트를 구현하고 `println!`을 호출하는 `drop` 메서드의 구현을 제공합니다. `drop` 함수의 본문은 타입의 인스턴스가 범위를 벗어날 때 실행하려는 모든 로직을 배치할 곳입니다. 여기서는 Rust가 `drop`을 호출하는 시점을 시각적으로 보여주기 위해 일부 텍스트를 출력합니다.

`main`에서 `CustomSmartPointer`의 두 인스턴스를 만든 다음 `CustomSmartPointers가 생성되었습니다.`를 출력합니다. `main` 끝에서 `CustomSmartPointer`의 인스턴스가 범위를 벗어나고 Rust는 `drop` 메서드에 넣은 코드를 호출하여 최종 메시지를 출력합니다. `drop` 메서드를 명시적으로 호출할 필요가 없다는 점에 유의하세요.

이 프로그램을 실행하면 다음 출력이 표시됩니다:

```console
$ cargo run
   Compiling drop-example v0.1.0 (file:///projects/drop-example)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.60s
     Running `target/debug/drop-example`
CustomSmartPointers가 생성되었습니다.
데이터 `다른 것`로 CustomSmartPointer 드롭!
데이터 `내 것`로 CustomSmartPointer 드롭!
```

Rust는 인스턴스가 범위를 벗어날 때 자동으로 `drop`을 호출하여 지정한 코드를 호출했습니다. 변수는 생성된 역순으로 드롭되므로 `d`가 `c`보다 먼저 드롭되었습니다. 이 예제의 목적은 `drop` 메서드가 어떻게 작동하는지 시각적으로 안내하는 것입니다; 일반적으로 출력 메시지가 아닌 타입이 실행해야 하는 정리 코드를 지정합니다.

## `std::mem::drop`으로 값을 일찍 드롭하기

불행히도 자동 `drop` 기능을 비활성화하는 것은 간단하지 않습니다. `drop`을 비활성화하는 것은 일반적으로 필요하지 않습니다; `Drop` 트레이트의 요점은 자동으로 처리된다는 것입니다. 그러나 때때로 값을 일찍 정리하고 싶을 수 있습니다. 한 가지 예는 잠금을 관리하는 스마트 포인터를 사용할 때입니다: 같은 범위의 다른 코드가 잠금을 획득할 수 있도록 잠금을 해제하는 `drop` 메서드를 강제로 실행하고 싶을 수 있습니다. Rust는 `Drop` 트레이트의 `drop` 메서드를 수동으로 호출하는 것을 허용하지 않습니다; 대신 범위가 끝나기 전에 값을 강제로 드롭하려면 표준 라이브러리에서 제공하는 `std::mem::drop` 함수를 호출해야 합니다.

Listing 15-14의 `main` 함수를 수정하여 Listing 15-15에 표시된 대로 `Drop` 트레이트의 `drop` 메서드를 수동으로 호출하려고 하면 컴파일러 오류가 발생합니다:

```rust
fn main() {
    let c = CustomSmartPointer {
        data: String::from("내 것"),
    };
    println!("CustomSmartPointer가 생성되었습니다.");
    c.drop();
    println!("main 끝 전에 CustomSmartPointer가 드롭되었습니다.");
}
```

*Listing 15-15: 조기 정리를 위해 `Drop` 트레이트에서 `drop` 메서드를 수동으로 호출하려고 시도*

이 코드를 컴파일하려고 하면 이 오류가 발생합니다:

```console
$ cargo run
   Compiling drop-example v0.1.0 (file:///projects/drop-example)
error[E0040]: explicit use of destructor method
  --> src/main.rs:16:7
   |
16 |     c.drop();
   |       ^^^^ explicit destructor calls not allowed
```

이 오류 메시지는 `drop`을 명시적으로 호출할 수 없다고 말합니다. 오류 메시지는 인스턴스가 정리될 때 호출되는 함수에 대한 일반적인 프로그래밍 용어인 *소멸자*라는 용어를 사용합니다. *소멸자*는 인스턴스를 생성하는 *생성자*와 유사합니다. Rust의 `drop` 함수는 특정 소멸자입니다.

Rust는 `drop`을 명시적으로 호출하는 것을 허용하지 않습니다. 왜냐하면 Rust는 여전히 `main` 끝에서 값에 대해 자동으로 `drop`을 호출하기 때문입니다. 이것은 Rust가 같은 값을 두 번 정리하려고 하기 때문에 *이중 해제* 오류가 됩니다.

값이 범위를 벗어날 때 `drop`의 자동 삽입을 비활성화할 수 없으며 `drop` 메서드를 명시적으로 호출할 수 없습니다. 따라서 값을 강제로 일찍 정리해야 하는 경우 `std::mem::drop` 함수를 사용합니다.

`std::mem::drop` 함수는 `Drop` 트레이트의 `drop` 메서드와 다릅니다. 조기에 드롭하려는 값을 인수로 전달하여 호출합니다. 함수는 프렐루드에 있으므로 Listing 15-16에 표시된 대로 Listing 15-15의 `main`을 수정하여 `drop` 함수를 호출할 수 있습니다:

```rust
fn main() {
    let c = CustomSmartPointer {
        data: String::from("내 것"),
    };
    println!("CustomSmartPointer가 생성되었습니다.");
    drop(c);
    println!("main 끝 전에 CustomSmartPointer가 드롭되었습니다.");
}
```

*Listing 15-16: `std::mem::drop`을 호출하여 값이 범위를 벗어나기 전에 명시적으로 드롭하기*

이 코드를 실행하면 다음이 출력됩니다:

```console
$ cargo run
   Compiling drop-example v0.1.0 (file:///projects/drop-example)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.73s
     Running `target/debug/drop-example`
CustomSmartPointer가 생성되었습니다.
데이터 `내 것`로 CustomSmartPointer 드롭!
main 끝 전에 CustomSmartPointer가 드롭되었습니다.
```

``데이터 `내 것`로 CustomSmartPointer 드롭!`` 텍스트가 `CustomSmartPointer가 생성되었습니다.`와 `main 끝 전에 CustomSmartPointer가 드롭되었습니다.` 텍스트 사이에 출력되어 `drop` 메서드 코드가 그 시점에서 `c`를 드롭하기 위해 호출되었음을 보여줍니다.

`Drop` 트레이트 구현에서 지정된 코드를 다양한 방법으로 사용하여 정리를 편리하고 안전하게 만들 수 있습니다: 예를 들어 자체 메모리 할당자를 만드는 데 사용할 수 있습니다! `Drop` 트레이트와 Rust의 소유권 시스템을 사용하면 Rust가 자동으로 정리하기 때문에 정리를 기억할 필요가 없습니다.

또한 여전히 사용 중인 값이 실수로 정리되어 발생하는 문제에 대해 걱정할 필요가 없습니다: 참조가 항상 유효하도록 보장하는 소유권 시스템은 또한 값이 더 이상 사용되지 않을 때 `drop`이 한 번만 호출되도록 보장합니다.

이제 `Box<T>`와 스마트 포인터의 일부 특성을 살펴보았으므로 표준 라이브러리에 정의된 몇 가지 다른 스마트 포인터를 살펴보겠습니다.

---

# `Rc<T>`, 참조 카운팅 스마트 포인터

> **원문:** https://doc.rust-lang.org/book/ch15-04-rc.html

대부분의 경우 소유권은 명확합니다: 주어진 값을 어떤 변수가 소유하는지 정확히 알 수 있습니다. 그러나 단일 값이 여러 소유자를 가질 수 있는 경우가 있습니다. 예를 들어 그래프 데이터 구조에서 여러 에지가 같은 노드를 가리킬 수 있으며, 해당 노드는 개념적으로 자신을 가리키는 모든 에지가 소유합니다. 노드는 에지가 가리키지 않아 소유자가 없을 때까지 정리되어서는 안 됩니다.

`Rc<T>` 타입을 사용하면 *참조 카운팅*을 통해 여러 소유권을 명시적으로 활성화해야 합니다. `Rc<T>`는 "reference counting"의 약자입니다. `Rc<T>` 타입은 값에 대한 참조 수를 추적하여 값이 여전히 사용 중인지 확인합니다. 값에 대한 참조가 0개이면 참조가 유효하지 않게 되지 않고 값을 정리할 수 있습니다.

`Rc<T>`를 거실의 TV로 상상해 보세요. 한 사람이 들어와서 TV를 볼 때 켭니다. 다른 사람들이 방에 들어와서 TV를 볼 수 있습니다. 마지막 사람이 거실을 떠날 때 더 이상 사용되지 않으므로 TV를 끕니다. 다른 사람들이 여전히 보고 있을 때 누군가가 TV를 끄면 나머지 TV 시청자들의 항의가 있을 것입니다!

프로그램의 여러 부분이 읽을 수 있도록 힙에 일부 데이터를 할당하고 컴파일 시간에 어느 부분이 데이터 사용을 마지막으로 끝낼지 결정할 수 없을 때 `Rc<T>` 타입을 사용합니다. 어느 부분이 마지막으로 끝낼지 알았다면 해당 부분을 데이터의 소유자로 만들 수 있으며, 컴파일 시간에 적용되는 일반 소유권 규칙이 적용됩니다.

`Rc<T>`는 단일 스레드 시나리오에서만 사용할 수 있습니다. 16장에서 동시성에 대해 논의할 때 다중 스레드 프로그램에서 참조 카운팅을 수행하는 방법을 다룰 것입니다.

## `Rc<T>`를 사용하여 데이터 공유하기

Listing 15-5의 cons 리스트 예제로 돌아가 봅시다. `Box<T>`를 사용하여 정의했음을 기억하세요. 이번에는 두 리스트가 모두 세 번째 리스트의 소유권을 공유하도록 만들 것입니다. 개념적으로 이것은 다음과 유사합니다:

```text
b (3) ─┐
       ├─► a (5) ─► (10) ─► Nil
c (4) ─┘
```

리스트 `a`는 5와 10을 포함합니다. 그런 다음 3으로 시작하는 리스트 `b`와 4로 시작하는 리스트 `c` 두 개를 더 만들 것입니다. `b`와 `c` 리스트 모두 5와 10을 포함하는 첫 번째 `a` 리스트로 계속됩니다. 다시 말해, 두 리스트는 5와 10을 포함하는 첫 번째 리스트를 공유합니다.

`Box<T>`를 사용한 `List` 정의로 이 시나리오를 구현하려고 하면 작동하지 않습니다. Listing 15-17에 표시된 대로:

```rust
enum List {
    Cons(i32, Box<List>),
    Nil,
}

use crate::List::{Cons, Nil};

fn main() {
    let a = Cons(5, Box::new(Cons(10, Box::new(Nil))));
    let b = Cons(3, Box::new(a));
    let c = Cons(4, Box::new(a));
}
```

*Listing 15-17: `Box<T>`를 사용하여 두 리스트가 세 번째 리스트의 소유권을 공유하려고 시도하는 것이 허용되지 않음을 보여주기*

이 코드를 컴파일하면 이 오류가 발생합니다:

```console
$ cargo run
   Compiling cons-list v0.1.0 (file:///projects/cons-list)
error[E0382]: use of moved value: `a`
  --> src/main.rs:11:30
   |
9  |     let a = Cons(5, Box::new(Cons(10, Box::new(Nil))));
   |         - move occurs because `a` has type `List`, which does not implement the `Copy` trait
10 |     let b = Cons(3, Box::new(a));
   |                              - value moved here
11 |     let c = Cons(4, Box::new(a));
   |                              ^ value used here after move
```

`Cons` 변형은 보유한 데이터를 소유하므로 `b` 리스트를 만들 때 `a`가 `b`로 이동되고 `b`가 `a`를 소유합니다. 그런 다음 `c`를 만들 때 `a`를 다시 사용하려고 하면 `a`가 이동되었기 때문에 허용되지 않습니다.

`Cons`의 정의를 변경하여 참조를 대신 보유하도록 할 수 있지만, 그러면 라이프타임 매개변수를 지정해야 합니다. 라이프타임 매개변수를 지정하면 리스트의 모든 요소가 적어도 전체 리스트만큼 오래 살아야 한다고 지정하게 됩니다. 이것은 Listing 15-17의 요소와 리스트의 경우이지만 모든 시나리오에서는 그렇지 않습니다.

대신 `List`의 정의를 `Box<T>` 대신 `Rc<T>`를 사용하도록 변경합니다. Listing 15-18에 표시된 대로. 각 `Cons` 변형은 이제 값과 `List`를 가리키는 `Rc<T>`를 보유합니다. `b`를 만들 때 `a`의 소유권을 가져가는 대신 `a`가 보유한 `Rc<List>`를 복제하여 참조 수를 1에서 2로 늘리고 `a`와 `b`가 해당 `Rc<List>`의 데이터 소유권을 공유하도록 합니다. 또한 `c`를 만들 때 `a`를 복제하여 참조 수를 2에서 3으로 늘립니다. `Rc::clone`을 호출할 때마다 `Rc<List>` 내부 데이터에 대한 참조 카운트가 증가하고 참조가 0이 될 때까지 데이터가 정리되지 않습니다.

```rust
enum List {
    Cons(i32, Rc<List>),
    Nil,
}

use crate::List::{Cons, Nil};
use std::rc::Rc;

fn main() {
    let a = Rc::new(Cons(5, Rc::new(Cons(10, Rc::new(Nil)))));
    let b = Cons(3, Rc::clone(&a));
    let c = Cons(4, Rc::clone(&a));
}
```

*Listing 15-18: `Rc<T>`를 사용하는 `List` 정의*

`Rc<T>`는 프렐루드에 없으므로 범위로 가져오기 위해 `use` 문을 추가해야 합니다. `main`에서 5와 10을 보유하는 리스트를 만들고 `a`의 새 `Rc<List>`에 저장합니다. 그런 다음 `b`와 `c`를 만들 때 `Rc::clone` 함수를 호출하고 `a`의 `Rc<List>`에 대한 참조를 인수로 전달합니다.

`Rc::clone(&a)` 대신 `a.clone()`을 호출할 수 있지만 Rust의 관례는 이 경우 `Rc::clone`을 사용하는 것입니다. `Rc::clone`의 구현은 대부분의 타입의 `clone` 구현처럼 모든 데이터의 딥 카피를 만들지 않습니다. `Rc::clone` 호출은 참조 카운트만 증가시키며 이는 많은 시간이 걸리지 않습니다. 데이터의 딥 카피는 많은 시간이 걸릴 수 있습니다. 참조 카운팅에 `Rc::clone`을 사용하면 딥 카피 종류의 복제와 참조 카운트를 증가시키는 종류의 복제를 시각적으로 구분할 수 있습니다. 코드에서 성능 문제를 찾을 때 딥 카피 복제만 고려하면 되고 `Rc::clone` 호출은 무시할 수 있습니다.

## `Rc<T>` 복제는 참조 카운트를 증가시킵니다

Listing 15-18의 작동 예제를 변경하여 `a`의 `Rc<List>`에 대한 참조를 만들고 드롭할 때 참조 카운트가 변경되는 것을 볼 수 있습니다.

Listing 15-19에서 `main`을 변경하여 리스트 `c` 주위에 내부 범위를 갖도록 합니다; 그런 다음 `c`가 범위를 벗어날 때 참조 카운트가 어떻게 변경되는지 볼 수 있습니다.

```rust
enum List {
    Cons(i32, Rc<List>),
    Nil,
}

use crate::List::{Cons, Nil};
use std::rc::Rc;

fn main() {
    let a = Rc::new(Cons(5, Rc::new(Cons(10, Rc::new(Nil)))));
    println!("a 생성 후 카운트 = {}", Rc::strong_count(&a));
    let b = Cons(3, Rc::clone(&a));
    println!("b 생성 후 카운트 = {}", Rc::strong_count(&a));
    {
        let c = Cons(4, Rc::clone(&a));
        println!("c 생성 후 카운트 = {}", Rc::strong_count(&a));
    }
    println!("c가 범위를 벗어난 후 카운트 = {}", Rc::strong_count(&a));
}
```

*Listing 15-19: 참조 카운트 출력하기*

프로그램에서 참조 카운트가 변경되는 각 지점에서 `Rc::strong_count` 함수를 호출하여 얻은 참조 카운트를 출력합니다. 이 함수는 `count` 대신 `strong_count`라고 명명됩니다. 왜냐하면 `Rc<T>` 타입에는 `weak_count`도 있기 때문입니다; "참조 순환 방지하기: `Rc<T>`를 `Weak<T>`로 변환하기" 섹션에서 `weak_count`가 무엇에 사용되는지 볼 것입니다.

이 코드는 다음을 출력합니다:

```console
$ cargo run
   Compiling cons-list v0.1.0 (file:///projects/cons-list)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.45s
     Running `target/debug/cons-list`
a 생성 후 카운트 = 1
b 생성 후 카운트 = 2
c 생성 후 카운트 = 3
c가 범위를 벗어난 후 카운트 = 2
```

`a`의 `Rc<List>`가 초기 참조 카운트 1이 있는 것을 볼 수 있습니다; 그런 다음 `clone`을 호출할 때마다 카운트가 1씩 증가합니다. `c`가 범위를 벗어나면 카운트가 1 감소합니다. 참조 카운트를 증가시키기 위해 `Rc::clone`을 호출해야 하는 것처럼 참조 카운트를 감소시키기 위해 함수를 호출할 필요가 없습니다: `Rc<T>` 값이 범위를 벗어날 때 `Drop` 트레이트의 구현이 자동으로 참조 카운트를 감소시킵니다.

이 예제에서 볼 수 없는 것은 `main` 끝에서 `b` 다음에 `a`가 범위를 벗어나면 카운트가 0이 되고 `Rc<List>`가 완전히 정리된다는 것입니다. `Rc<T>`를 사용하면 단일 값이 여러 소유자를 가질 수 있으며, 카운트는 소유자 중 어느 것이든 여전히 존재하는 한 값이 유효하게 유지되도록 보장합니다.

불변 참조를 통해 `Rc<T>`를 사용하면 프로그램의 여러 부분 간에 읽기 전용으로 데이터를 공유할 수 있습니다. `Rc<T>`가 여러 가변 참조를 갖도록 허용하면 4장에서 논의한 빌림 규칙 중 하나를 위반할 수 있습니다: 같은 장소에 대한 여러 가변 빌림은 데이터 레이스와 불일치를 유발할 수 있습니다. 그러나 데이터를 변형할 수 있는 것은 매우 유용합니다! 다음 섹션에서는 내부 가변성 패턴과 이 불변성 제한을 해결하기 위해 `Rc<T>`와 함께 사용할 수 있는 `RefCell<T>` 타입에 대해 논의합니다.

---

# `RefCell<T>`와 내부 가변성 패턴

> **원문:** https://doc.rust-lang.org/book/ch15-05-interior-mutability.html

*내부 가변성*은 해당 데이터에 대한 불변 참조가 있을 때도 데이터를 변형할 수 있게 하는 Rust의 디자인 패턴입니다; 일반적으로 이 행동은 빌림 규칙에 의해 허용되지 않습니다. 데이터를 변형하기 위해 패턴은 데이터 구조 내에서 `unsafe` 코드를 사용하여 변형과 빌림을 관리하는 Rust의 일반적인 규칙을 우회합니다. 안전하지 않은 코드는 컴파일러에게 규칙을 확인하는 대신 직접 규칙을 확인하고 있음을 나타냅니다; 안전하지 않은 코드는 20장에서 자세히 논의합니다.

내부 가변성 패턴을 사용하는 타입은 컴파일러가 보장할 수 없더라도 런타임에 빌림 규칙을 따를 것이 보장될 때만 사용할 수 있습니다. 관련된 `unsafe` 코드는 안전한 API로 래핑되고 외부 타입은 여전히 불변입니다.

내부 가변성 패턴을 따르는 `RefCell<T>` 타입을 살펴보며 이 개념을 탐구해 봅시다.

## `RefCell<T>`로 런타임에 빌림 규칙 적용하기

`Rc<T>`와 달리 `RefCell<T>` 타입은 보유한 데이터에 대한 단일 소유권을 나타냅니다. 그렇다면 `RefCell<T>`를 `Box<T>`와 같은 타입과 다르게 만드는 것은 무엇일까요? 4장에서 배운 빌림 규칙을 기억하세요:

* 주어진 시간에 하나의 가변 참조 *또는* 임의의 수의 불변 참조 중 하나를 가질 수 있습니다(둘 다는 아닙니다).
* 참조는 항상 유효해야 합니다.

참조와 `Box<T>`를 사용하면 빌림 규칙의 불변성이 컴파일 시간에 적용됩니다. `RefCell<T>`를 사용하면 이러한 불변성이 *런타임*에 적용됩니다. 참조를 사용하면 이러한 규칙을 위반하면 컴파일러 오류가 발생합니다. `RefCell<T>`를 사용하면 이러한 규칙을 위반하면 프로그램이 패닉하고 종료됩니다.

컴파일 시간에 빌림 규칙을 확인하는 장점은 개발 프로세스 초기에 오류가 잡히고 모든 분석이 미리 완료되므로 런타임 성능에 영향이 없다는 것입니다. 이러한 이유로 컴파일 시간에 빌림 규칙을 확인하는 것이 대부분의 경우 최선의 선택이며, 이것이 Rust의 기본값인 이유입니다.

런타임에 빌림 규칙을 확인하는 장점은 컴파일 시간 검사에서 허용되지 않았을 특정 메모리 안전 시나리오가 허용된다는 것입니다. Rust 컴파일러와 같은 정적 분석은 본질적으로 보수적입니다. 코드의 일부 속성은 코드를 분석하여 감지할 수 없습니다: 가장 유명한 예는 정지 문제이며, 이 책의 범위를 벗어나지만 연구하기에 흥미로운 주제입니다.

일부 분석이 불가능하기 때문에 Rust 컴파일러가 코드가 소유권 규칙을 준수하는지 확신할 수 없으면 올바른 프로그램을 거부할 수 있습니다; 이런 방식으로 보수적입니다. Rust가 올바르지 않은 프로그램을 수락하면 사용자는 Rust가 제공하는 보장을 신뢰할 수 없습니다. 그러나 Rust가 올바른 프로그램을 거부하면 프로그래머가 불편할 것이지만 재앙적인 일은 일어나지 않습니다. `RefCell<T>` 타입은 코드가 빌림 규칙을 따르는 것을 확신하지만 컴파일러가 이를 이해하고 보장할 수 없을 때 유용합니다.

`Rc<T>`와 유사하게 `RefCell<T>`는 단일 스레드 시나리오에서만 사용하기 위한 것이며 다중 스레드 컨텍스트에서 사용하려고 하면 컴파일 시간 오류가 발생합니다. 다중 스레드 프로그램에서 `RefCell<T>`의 기능을 얻는 방법은 16장에서 다룰 것입니다.

다음은 `Box<T>`, `Rc<T>` 또는 `RefCell<T>`를 선택하는 이유의 요약입니다:

* `Rc<T>`는 같은 데이터의 여러 소유자를 가능하게 합니다; `Box<T>`와 `RefCell<T>`는 단일 소유자를 가집니다.
* `Box<T>`는 컴파일 시간에 확인되는 불변 또는 가변 빌림을 허용합니다; `Rc<T>`는 컴파일 시간에 확인되는 불변 빌림만 허용합니다; `RefCell<T>`는 런타임에 확인되는 불변 또는 가변 빌림을 허용합니다.
* `RefCell<T>`는 런타임에 확인되는 가변 빌림을 허용하기 때문에, `RefCell<T>`가 불변이더라도 `RefCell<T>` 내부의 값을 변형할 수 있습니다.

불변 값 내에서 값을 변형하는 것은 *내부 가변성* 패턴입니다. 내부 가변성이 유용한 상황과 내부 가변성이 어떻게 가능한지 살펴보겠습니다.

## 내부 가변성: 불변 값에 대한 가변 빌림

빌림 규칙의 결과는 불변 값이 있을 때 그 값을 가변으로 빌릴 수 없다는 것입니다. 예를 들어, 이 코드는 컴파일되지 않습니다:

```rust
fn main() {
    let x = 5;
    let y = &mut x;
}
```

이 코드를 컴파일하려고 하면 다음 오류가 발생합니다:

```console
$ cargo run
   Compiling borrowing v0.1.0 (file:///projects/borrowing)
error[E0596]: cannot borrow `x` as mutable, as it is not declared as mutable
 --> src/main.rs:3:13
  |
3 |     let y = &mut x;
  |             ^^^^^^ cannot borrow as mutable
  |
help: consider changing this to be mutable
  |
2 |     let mut x = 5;
  |         +++
```

그러나 값이 자신의 메서드에서 자신을 변형하지만 다른 코드에는 불변으로 나타나는 것이 유용한 상황이 있습니다. 값의 메서드 외부의 코드는 값을 변형할 수 없습니다. `RefCell<T>`를 사용하는 것은 내부 가변성의 기능을 얻는 한 가지 방법입니다. 그러나 `RefCell<T>`가 빌림 규칙을 완전히 우회하는 것은 아닙니다: 컴파일러의 빌림 검사기는 내부 가변성을 허용하고 빌림 규칙은 대신 런타임에 확인됩니다. 규칙을 위반하면 컴파일러 오류 대신 `panic!`이 발생합니다.

`RefCell<T>`를 사용하여 불변 값을 변형하는 것이 왜 유용한지 보여주는 실제 예제를 살펴보겠습니다.

### 내부 가변성 사용 사례: 모의 객체

때때로 테스트 중에 프로그래머는 타입을 다른 타입 대신 사용하여 특정 동작을 관찰하고 올바르게 구현되었는지 assert합니다. 이 자리 표시자 타입을 *테스트 더블*이라고 합니다. 영화 제작에서 스턴트맨이 배우를 대신하여 특히 까다로운 장면을 촬영하는 "스턴트 더블"처럼 생각하세요. 테스트 더블은 테스트를 실행할 때 다른 타입을 대신합니다. *모의 객체*는 테스트 중에 무슨 일이 발생했는지 기록하여 올바른 작업이 수행되었는지 assert할 수 있는 특정 유형의 테스트 더블입니다.

Rust는 다른 언어에서의 객체와 같은 의미로 객체가 있지 않으며, Rust는 일부 다른 언어처럼 표준 라이브러리에 내장된 모의 객체 기능이 없습니다. 그러나 모의 객체와 같은 목적을 수행하는 구조체를 확실히 만들 수 있습니다.

다음은 테스트할 시나리오입니다: 최대값에 대해 값을 추적하고 현재 값이 최대값에 얼마나 가까운지에 따라 메시지를 보내는 라이브러리를 만들 것입니다. 예를 들어, 이 라이브러리는 사용자가 허용된 API 호출 수에 대한 할당량을 추적하는 데 사용할 수 있습니다.

우리 라이브러리는 최대값에 얼마나 가까운지와 메시지가 무엇이어야 하는지를 추적하는 기능만 제공합니다. 라이브러리를 사용하는 애플리케이션은 메시지 전송 메커니즘을 제공해야 합니다: 애플리케이션은 애플리케이션에 메시지를 넣거나, 이메일을 보내거나, 문자 메시지를 보내거나, 다른 것을 할 수 있습니다. 라이브러리는 그 세부 사항을 알 필요가 없습니다. 우리가 제공할 `Messenger`라는 트레이트를 구현하는 것만 필요합니다. Listing 15-20은 라이브러리 코드를 보여줍니다:

```rust
pub trait Messenger {
    fn send(&self, msg: &str);
}

pub struct LimitTracker<'a, T: Messenger> {
    messenger: &'a T,
    value: usize,
    max: usize,
}

impl<'a, T> LimitTracker<'a, T>
where
    T: Messenger,
{
    pub fn new(messenger: &'a T, max: usize) -> LimitTracker<'a, T> {
        LimitTracker {
            messenger,
            value: 0,
            max,
        }
    }

    pub fn set_value(&mut self, value: usize) {
        self.value = value;

        let percentage_of_max = self.value as f64 / self.max as f64;

        if percentage_of_max >= 1.0 {
            self.messenger.send("오류: 할당량을 초과했습니다!");
        } else if percentage_of_max >= 0.9 {
            self.messenger
                .send("긴급 경고: 할당량의 90% 이상을 사용했습니다!");
        } else if percentage_of_max >= 0.75 {
            self.messenger
                .send("경고: 할당량의 75% 이상을 사용했습니다!");
        }
    }
}
```

*Listing 15-20: 최대값에 얼마나 가까운지를 추적하고 값이 특정 레벨에 있을 때 경고하는 라이브러리*

이 코드의 중요한 부분 하나는 `Messenger` 트레이트가 `self`에 대한 불변 참조와 메시지 텍스트를 받는 `send`라는 메서드 하나가 있다는 것입니다. 이 트레이트는 모의 객체가 구현해야 하는 인터페이스이므로 실제 객체처럼 같은 방식으로 사용할 수 있습니다. 중요한 다른 부분은 `LimitTracker`의 `set_value` 메서드의 동작을 테스트하고 싶다는 것입니다. `value` 매개변수로 전달하는 것을 변경할 수 있지만 `set_value`는 우리가 assertion을 만들 수 있는 아무것도 반환하지 않습니다. `Messenger` 트레이트를 구현하는 것과 `max`에 대한 특정 값을 가진 `LimitTracker`를 만들 때 `value`에 다른 숫자를 전달하면 메신저가 적절한 메시지를 보내도록 지시받았다고 말할 수 있기를 원합니다.

`send`를 호출할 때 이메일이나 문자 메시지를 보내는 대신 보내도록 지시받은 메시지만 추적하는 모의 객체가 필요합니다. 모의 객체의 새 인스턴스를 만들고, 모의 객체를 사용하는 `LimitTracker`를 만들고, `LimitTracker`에서 `set_value` 메서드를 호출한 다음, 모의 객체에 예상되는 메시지가 있는지 확인할 수 있습니다. Listing 15-21은 그렇게 하려는 모의 객체를 구현하려는 시도를 보여주지만 빌림 검사기가 허용하지 않습니다:

```rust
#[cfg(test)]
mod tests {
    use super::*;

    struct MockMessenger {
        sent_messages: Vec<String>,
    }

    impl MockMessenger {
        fn new() -> MockMessenger {
            MockMessenger {
                sent_messages: vec![],
            }
        }
    }

    impl Messenger for MockMessenger {
        fn send(&self, message: &str) {
            self.sent_messages.push(String::from(message));
        }
    }

    #[test]
    fn it_sends_an_over_75_percent_warning_message() {
        let mock_messenger = MockMessenger::new();
        let mut limit_tracker = LimitTracker::new(&mock_messenger, 100);

        limit_tracker.set_value(80);

        assert_eq!(mock_messenger.sent_messages.len(), 1);
    }
}
```

*Listing 15-21: 빌림 검사기가 허용하지 않는 `MockMessenger` 구현 시도*

이 테스트 코드는 추적하기 위해 전송된 메시지의 `Vec<String>`으로 시작하는 `sent_messages` 필드가 있는 `MockMessenger` 구조체를 정의합니다. 또한 빈 메시지 목록으로 시작하는 새 `MockMessenger` 값을 편리하게 만드는 연관 함수 `new`를 정의합니다. 그런 다음 `MockMessenger`에 `Messenger` 트레이트를 구현하여 `MockMessenger`를 `LimitTracker`에 전달할 수 있습니다. `send` 메서드의 정의에서 매개변수로 전달된 메시지를 `MockMessenger`의 `sent_messages` 목록에 저장합니다.

테스트에서 `max` 값의 75% 이상인 `value`를 설정하도록 지시받았을 때 `LimitTracker`에 무슨 일이 발생하는지 테스트합니다. 먼저 빈 메시지 목록으로 시작하는 새 `MockMessenger`를 만듭니다. 그런 다음 새 `LimitTracker`를 만들고 새 `MockMessenger`에 대한 참조와 `max` 값 100을 전달합니다. `LimitTracker`에서 `set_value` 메서드를 값 80으로 호출합니다. 이는 100의 75%보다 큽니다. 그런 다음 `MockMessenger`가 추적하는 메시지 목록에 이제 하나의 메시지가 있어야 한다고 assert합니다.

그러나 이 테스트에는 여기 표시된 문제가 있습니다:

```console
$ cargo test
   Compiling limit-tracker v0.1.0 (file:///projects/limit-tracker)
error[E0596]: cannot borrow `self.sent_messages` as mutable, as it is behind a `&` reference
  --> src/lib.rs:58:13
   |
58 |             self.sent_messages.push(String::from(message));
   |             ^^^^^^^^^^^^^^^^^^ `self` is a `&` reference, so the data it refers to cannot be borrowed as mutable
```

`send` 메서드가 `self`에 대한 불변 참조를 받기 때문에 `MockMessenger`를 수정하여 메시지를 추적할 수 없습니다. 또한 트레이트 정의와 일치해야 하기 때문에 오류 텍스트의 제안을 받아 `&mut self`를 대신 사용할 수 없습니다(시도해서 어떤 오류 메시지를 받는지 확인해 보세요).

이것은 내부 가변성이 도움이 될 수 있는 상황입니다! `sent_messages`를 `RefCell<T>`에 저장하면 `send` 메서드가 우리가 본 메시지를 저장하기 위해 `sent_messages`를 수정할 수 있습니다. Listing 15-22는 그 모습을 보여줍니다:

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use std::cell::RefCell;

    struct MockMessenger {
        sent_messages: RefCell<Vec<String>>,
    }

    impl MockMessenger {
        fn new() -> MockMessenger {
            MockMessenger {
                sent_messages: RefCell::new(vec![]),
            }
        }
    }

    impl Messenger for MockMessenger {
        fn send(&self, message: &str) {
            self.sent_messages.borrow_mut().push(String::from(message));
        }
    }

    #[test]
    fn it_sends_an_over_75_percent_warning_message() {
        // --snip--
        let mock_messenger = MockMessenger::new();
        let mut limit_tracker = LimitTracker::new(&mock_messenger, 100);

        limit_tracker.set_value(80);

        assert_eq!(mock_messenger.sent_messages.borrow().len(), 1);
    }
}
```

*Listing 15-22: 외부 값이 불변으로 간주되는 동안 내부 값을 변형하기 위해 `RefCell<T>` 사용하기*

`sent_messages` 필드는 이제 `Vec<String>` 대신 `RefCell<Vec<String>>` 타입입니다. `new` 함수에서 빈 벡터 주위에 새 `RefCell<Vec<String>>` 인스턴스를 만듭니다.

`send` 메서드의 구현에서 첫 번째 매개변수는 여전히 트레이트 정의와 일치하는 `self`의 불변 빌림입니다. `self.sent_messages`에서 `RefCell<Vec<String>>`에 `borrow_mut`를 호출하여 벡터에 대한 가변 참조인 `RefCell<Vec<String>>` 내부의 값에 대한 가변 참조를 얻습니다. 그런 다음 벡터에 대한 가변 참조에서 `push`를 호출하여 테스트 중에 전송된 메시지를 추적할 수 있습니다.

마지막으로 해야 할 변경 사항은 assertion에 있습니다: 내부 벡터에 얼마나 많은 항목이 있는지 보기 위해 `RefCell<Vec<String>>`에 `borrow`를 호출하여 벡터에 대한 불변 참조를 얻습니다.

이제 `RefCell<T>`를 사용하는 방법을 보았으니 어떻게 작동하는지 자세히 알아보겠습니다!

### `RefCell<T>`로 런타임에 빌림 추적하기

불변 및 가변 참조를 만들 때 각각 `&`와 `&mut` 구문을 사용합니다. `RefCell<T>`에서는 `RefCell<T>`에 속하는 안전한 API의 일부인 `borrow`와 `borrow_mut` 메서드를 사용합니다. `borrow` 메서드는 스마트 포인터 타입 `Ref<T>`를 반환하고 `borrow_mut`는 스마트 포인터 타입 `RefMut<T>`를 반환합니다. 두 타입 모두 `Deref`를 구현하므로 일반 참조처럼 취급할 수 있습니다.

`RefCell<T>`는 현재 활성 상태인 `Ref<T>`와 `RefMut<T>` 스마트 포인터 수를 추적합니다. `borrow`를 호출할 때마다 `RefCell<T>`는 활성 상태인 불변 빌림 수를 증가시킵니다. `Ref<T>` 값이 범위를 벗어나면 불변 빌림 수가 1 감소합니다. 컴파일 시간 빌림 규칙과 마찬가지로 `RefCell<T>`는 주어진 시간에 많은 불변 빌림 또는 하나의 가변 빌림을 가질 수 있습니다.

이러한 규칙을 위반하면 참조에서 발생하는 컴파일러 오류 대신 `RefCell<T>`의 구현이 런타임에 패닉합니다. Listing 15-23은 Listing 15-22의 `send` 구현 수정 사항을 보여줍니다. 우리는 동일한 범위에서 두 개의 가변 빌림을 만들어 `RefCell<T>`가 런타임에 이것을 막는다는 것을 보여주려고 의도적으로 시도합니다.

```rust
impl Messenger for MockMessenger {
    fn send(&self, message: &str) {
        let mut one_borrow = self.sent_messages.borrow_mut();
        let mut two_borrow = self.sent_messages.borrow_mut();

        one_borrow.push(String::from(message));
        two_borrow.push(String::from(message));
    }
}
```

*Listing 15-23: 같은 범위에서 두 개의 가변 참조를 만들어 `RefCell<T>`가 패닉할 것임을 확인*

`borrow_mut`에서 반환된 `RefMut<T>` 스마트 포인터에 대해 변수 `one_borrow`를 만듭니다. 그런 다음 같은 방식으로 변수 `two_borrow`에 다른 가변 빌림을 만듭니다. 이것은 같은 범위에서 같은 데이터에 대한 두 개의 가변 참조를 만들며, 이는 허용되지 않습니다. 라이브러리에 대한 테스트를 실행하면 Listing 15-23의 코드는 오류 없이 컴파일되지만 테스트는 실패합니다:

```console
$ cargo test
   Compiling limit-tracker v0.1.0 (file:///projects/limit-tracker)
    Finished `test` profile [unoptimized + debuginfo] target(s) in 0.91s
     Running unittests src/lib.rs (target/debug/deps/limit_tracker-e599811fa246dbde)

running 1 test
test tests::it_sends_an_over_75_percent_warning_message ... FAILED

failures:

---- tests::it_sends_an_over_75_percent_warning_message stdout ----
thread 'tests::it_sends_an_over_75_percent_warning_message' panicked at src/lib.rs:60:53:
already borrowed: BorrowMutError
```

코드가 `already borrowed: BorrowMutError` 메시지와 함께 패닉했다는 것에 주목하세요. 이것이 `RefCell<T>`가 런타임에 빌림 규칙 위반을 처리하는 방식입니다.

컴파일 시간이 아닌 런타임에 빌림 오류를 잡기로 선택하면 개발 프로세스 후반에 코드에서 실수를 발견할 수 있으며 코드가 프로덕션에 배포될 때까지 발견하지 못할 수도 있습니다. 또한 컴파일 시간이 아닌 런타임에 빌림을 추적하는 결과로 코드에 약간의 런타임 성능 페널티가 발생합니다. 그러나 `RefCell<T>`를 사용하면 불변 값만 허용되는 컨텍스트에서 사용되는 동안 자신을 수정하여 본 메시지를 추적하는 모의 객체를 작성할 수 있습니다. 일반 참조가 제공하는 것보다 더 많은 기능을 얻기 위해 트레이드오프가 있더라도 `RefCell<T>`를 사용할 수 있습니다.

## `Rc<T>`와 `RefCell<T>` 결합하여 가변 데이터의 여러 소유자 갖기

`RefCell<T>`를 사용하는 일반적인 방법은 `Rc<T>`와 결합하는 것입니다. `Rc<T>`가 일부 데이터의 여러 소유자를 가질 수 있게 하지만 해당 데이터에 대한 불변 액세스만 제공한다는 것을 기억하세요. `RefCell<T>`를 보유하는 `Rc<T>`가 있으면 여러 소유자를 *가지고* 변형할 수 있는 값을 얻을 수 있습니다!

예를 들어 Listing 15-18의 cons 리스트 예제를 기억하세요. `Rc<T>`를 사용하여 여러 리스트가 다른 리스트의 소유권을 공유하도록 했습니다. `Rc<T>`는 불변 값만 보유하므로 리스트를 만든 후에는 리스트의 값을 변경할 수 없습니다. 리스트의 값을 변경할 수 있도록 `RefCell<T>`를 추가해 봅시다. Listing 15-24는 `Cons` 정의에서 `RefCell<T>`를 사용하면 모든 리스트에 저장된 값을 수정할 수 있음을 보여줍니다:

```rust
#[derive(Debug)]
enum List {
    Cons(Rc<RefCell<i32>>, Rc<List>),
    Nil,
}

use crate::List::{Cons, Nil};
use std::cell::RefCell;
use std::rc::Rc;

fn main() {
    let value = Rc::new(RefCell::new(5));

    let a = Rc::new(Cons(Rc::clone(&value), Rc::new(Nil)));

    let b = Cons(Rc::new(RefCell::new(3)), Rc::clone(&a));
    let c = Cons(Rc::new(RefCell::new(4)), Rc::clone(&a));

    *value.borrow_mut() += 10;

    println!("a after = {a:?}");
    println!("b after = {b:?}");
    println!("c after = {c:?}");
}
```

*Listing 15-24: `Rc<RefCell<i32>>`를 사용하여 변형할 수 있는 `List` 생성하기*

값 `Rc<RefCell<i32>>`의 인스턴스를 만들고 나중에 직접 액세스할 수 있도록 `value`라는 변수에 저장합니다. 그런 다음 `value`를 보유하는 `Cons` 변형으로 `a`에 `List`를 만듭니다. `value`를 복제하여 `a`와 `value` 모두 외부 값 `5`의 소유권을 갖도록 해야 합니다. `value`에서 `a`로 소유권을 이전하거나 `a`가 `value`에서 빌리도록 하지 않습니다.

리스트 `a`를 `Rc<List>`로 래핑하므로 `b`와 `c` 리스트를 만들 때 둘 다 `a`를 참조할 수 있습니다. 이것이 Listing 15-18에서 한 것입니다.

`a`, `b`, `c` 리스트를 만든 후 `value`의 값에 10을 더하고 싶습니다. `value`에서 `borrow_mut`를 호출하여 이를 수행합니다. 이는 5장에서 논의한 자동 역참조 기능을 사용하여 `Rc<T>`를 내부 `RefCell<T>` 값으로 역참조합니다. `borrow_mut` 메서드는 `RefMut<T>` 스마트 포인터를 반환하고 역참조 연산자를 사용하여 내부 값을 변경합니다.

`a`, `b`, `c`를 출력하면 모두 5 대신 수정된 값 15가 있는 것을 볼 수 있습니다:

```console
$ cargo run
   Compiling cons-list v0.1.0 (file:///projects/cons-list)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.63s
     Running `target/debug/cons-list`
a after = Cons(RefCell { value: 15 }, Nil)
b after = Cons(RefCell { value: 3 }, Cons(RefCell { value: 15 }, Nil))
c after = Cons(RefCell { value: 4 }, Cons(RefCell { value: 15 }, Nil))
```

이 기술은 꽤 깔끔합니다! `RefCell<T>`를 사용하여 외부적으로 불변인 `List` 값을 가집니다. 그러나 내부 값에 액세스하고 필요할 때 데이터를 수정할 수 있도록 내부 가변성에 대한 액세스를 제공하는 `RefCell<T>`의 메서드를 사용할 수 있습니다. 빌림 규칙의 런타임 검사는 데이터 레이스로부터 우리를 보호하며, 데이터 구조의 이 유연성을 위해 약간의 속도를 트레이드하는 것이 때때로 가치가 있습니다. `RefCell<T>`는 다중 스레드 코드에서는 작동하지 않는다는 점에 유의하세요! `Mutex<T>`는 `RefCell<T>`의 스레드 안전 버전이며 16장에서 `Mutex<T>`에 대해 논의할 것입니다.

---

# 참조 순환은 메모리를 누수시킬 수 있습니다

> **원문:** https://doc.rust-lang.org/book/ch15-06-reference-cycles.html

Rust의 메모리 안전 보장은 절대 정리되지 않는 메모리(*메모리 누수*라고 함)를 실수로 만드는 것을 어렵게 하지만 불가능하게 하지는 않습니다. 메모리 누수를 완전히 방지하는 것은 컴파일 시간에 데이터 레이스를 허용하지 않는 것과 같은 방식으로 Rust의 보장 중 하나가 아닙니다. 즉, 메모리 누수는 Rust에서 메모리 안전합니다. `Rc<T>`와 `RefCell<T>`를 사용하여 Rust가 메모리 누수를 허용하는 것을 볼 수 있습니다: 항목이 순환에서 서로를 참조하는 참조를 만들 수 있습니다. 이것은 순환의 각 항목의 참조 카운트가 절대 0이 되지 않고 값이 절대 드롭되지 않기 때문에 메모리 누수를 생성합니다.

## 참조 순환 만들기

Listing 15-25의 `List` 열거형과 `tail` 메서드 정의로 시작하여 참조 순환이 어떻게 발생할 수 있는지, 그리고 어떻게 방지하는지 살펴보겠습니다:

```rust
use crate::List::{Cons, Nil};
use std::cell::RefCell;
use std::rc::Rc;

#[derive(Debug)]
enum List {
    Cons(i32, RefCell<Rc<List>>),
    Nil,
}

impl List {
    fn tail(&self) -> Option<&RefCell<Rc<List>>> {
        match self {
            Cons(_, item) => Some(item),
            Nil => None,
        }
    }
}

fn main() {}
```

*Listing 15-25: `Cons` 변형이 참조하는 것을 수정할 수 있도록 `RefCell<T>`를 보유하는 cons 리스트 정의*

Listing 15-5의 `List` 정의의 또 다른 변형을 사용하고 있습니다. `Cons` 변형의 두 번째 요소는 이제 `RefCell<Rc<List>>`입니다. 이것은 Listing 15-24에서와 같이 `i32` 값을 수정하는 기능 대신 `Cons` 변형이 가리키는 `List` 값을 수정하고 싶다는 것을 의미합니다. 또한 `Cons` 변형이 있으면 두 번째 항목에 액세스할 수 있도록 편리하게 하기 위해 `tail` 메서드를 추가하고 있습니다.

Listing 15-26에서 Listing 15-25의 정의를 사용하는 `main` 함수를 추가합니다. 이 코드는 `a`에 리스트를 만들고 `a`의 리스트를 가리키는 `b`에 리스트를 만듭니다. 그런 다음 `a`의 리스트를 수정하여 `b`를 가리키게 하여 참조 순환을 만듭니다. 프로세스의 다양한 지점에서 참조 카운트가 무엇인지 보여주기 위해 `println!` 문이 있습니다.

```rust
fn main() {
    let a = Rc::new(Cons(5, RefCell::new(Rc::new(Nil))));

    println!("a 초기 rc 카운트 = {}", Rc::strong_count(&a));
    println!("a 다음 항목 = {:?}", a.tail());

    let b = Rc::new(Cons(10, RefCell::new(Rc::clone(&a))));

    println!("b 생성 후 a rc 카운트 = {}", Rc::strong_count(&a));
    println!("b 초기 rc 카운트 = {}", Rc::strong_count(&b));
    println!("b 다음 항목 = {:?}", b.tail());

    if let Some(link) = a.tail() {
        *link.borrow_mut() = Rc::clone(&b);
    }

    println!("a 변경 후 b rc 카운트 = {}", Rc::strong_count(&b));
    println!("a 변경 후 a rc 카운트 = {}", Rc::strong_count(&a));

    // 다음 줄의 주석을 해제하면 순환을 확인할 수 있습니다; 스택 오버플로가 발생합니다
    // println!("a 다음 항목 = {:?}", a.tail());
}
```

*Listing 15-26: 서로를 가리키는 두 `List` 값의 참조 순환 만들기*

초기 리스트 `5, Nil`을 보유하는 `Rc<List>` 인스턴스를 `a` 변수에 만듭니다. 그런 다음 값 10을 보유하고 `a`의 리스트를 가리키는 또 다른 `Rc<List>` 인스턴스를 변수 `b`에 만듭니다.

`a`가 `Nil` 대신 `b`를 가리키도록 수정하여 순환을 만듭니다. `tail` 메서드를 사용하여 `a`의 `RefCell<Rc<List>>`에 대한 참조를 얻고 변수 `link`에 넣습니다. 그런 다음 `RefCell<Rc<List>>`에서 `borrow_mut` 메서드를 사용하여 `Nil` 값을 보유하는 `Rc<List>` 내부의 값을 `b`의 `Rc<List>`로 변경합니다.

이 코드를 실행하면(지금은 마지막 `println!`을 주석 처리한 상태로) 다음 출력이 표시됩니다:

```console
$ cargo run
   Compiling cons-list v0.1.0 (file:///projects/cons-list)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.53s
     Running `target/debug/cons-list`
a 초기 rc 카운트 = 1
a 다음 항목 = Some(RefCell { value: Nil })
b 생성 후 a rc 카운트 = 2
b 초기 rc 카운트 = 1
b 다음 항목 = Some(RefCell { value: Cons(5, RefCell { value: Nil }) })
a 변경 후 b rc 카운트 = 2
a 변경 후 a rc 카운트 = 2
```

`a`의 리스트를 `b`를 가리키도록 변경한 후 `a`와 `b`의 `Rc<List>` 인스턴스에 대한 참조 카운트는 모두 2입니다. `main` 끝에서 Rust는 `b` 변수를 드롭하여 `b` `Rc<List>` 인스턴스의 참조 카운트를 2에서 1로 줄입니다. 이 시점에서 `Rc<List>`가 힙에 있는 메모리는 드롭되지 않습니다. 카운트가 0이 아니라 1이기 때문입니다. 그런 다음 Rust는 `a`를 드롭하여 `a` `Rc<List>` 인스턴스의 참조 카운트도 2에서 1로 줄입니다. 이 인스턴스의 메모리도 드롭될 수 없습니다. 다른 `Rc<List>` 인스턴스가 여전히 참조하기 때문입니다. 리스트에 할당된 메모리는 영원히 수집되지 않은 채 유지됩니다. 이 참조 순환을 시각화하기 위해 다이어그램을 만들었습니다:

```text
┌─────────────────┐     ┌─────────────────┐
│  a: Rc<List>    │     │  b: Rc<List>    │
│  count: 2       │     │  count: 2       │
└───────┬─────────┘     └───────┬─────────┘
        │                       │
        ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│  Cons(5, ...)   │────►│  Cons(10, ...)  │
│                 │◄────│                 │
└─────────────────┘     └─────────────────┘
```

마지막 `println!`의 주석을 해제하고 프로그램을 실행하면 Rust가 `a`가 `b`를 가리키고 `b`가 `a`를 가리키는 이 순환을 출력하려고 시도하고 스택이 오버플로될 때까지 계속됩니다.

실제 프로그램과 비교하면 이 예제에서 참조 순환을 만드는 결과는 그다지 심각하지 않습니다: 참조 순환을 만든 직후 프로그램이 종료됩니다. 그러나 더 복잡한 프로그램이 순환에서 많은 메모리를 할당하고 오랜 시간 동안 보유하면 프로그램이 필요한 것보다 더 많은 메모리를 사용하고 시스템을 압도하여 사용 가능한 메모리가 부족해질 수 있습니다.

참조 순환을 만드는 것은 쉽게 할 수 있는 일이 아니지만 불가능한 것도 아닙니다. `Rc<T>` 값을 포함하는 `RefCell<T>` 값이 있거나 내부 가변성과 참조 카운팅이 있는 유사한 중첩 타입 조합이 있는 경우 순환을 만들지 않도록 해야 합니다; Rust가 순환을 잡아줄 것이라고 의존할 수 없습니다. 참조 순환을 만드는 것은 프로그램의 로직 버그이며 자동화된 테스트, 코드 리뷰 및 기타 소프트웨어 개발 관행을 사용하여 최소화해야 합니다.

참조 순환을 피하는 또 다른 해결책은 일부 참조가 소유권을 표현하고 일부 참조는 그렇지 않도록 데이터 구조를 재구성하는 것입니다. 결과적으로 일부 강한 참조와 일부 약한 참조로 구성된 순환을 가질 수 있으며, 강한 참조만 값이 드롭되는지 여부에 영향을 미칩니다. Listing 15-25에서는 항상 `Cons` 변형이 리스트를 소유하기를 원하므로 데이터 구조를 재구성하는 것이 불가능합니다. 부모 노드와 자식 노드로 구성된 그래프를 사용하는 예를 살펴보고 비소유 관계가 참조 순환을 방지하는 적절한 방법인 경우를 살펴보겠습니다.

## 참조 순환 방지하기: `Rc<T>`를 `Weak<T>`로 변환하기

지금까지 `Rc::clone`을 호출하면 `Rc<T>` 인스턴스의 `strong_count`가 증가하고 `Rc<T>` 인스턴스는 `strong_count`가 0일 때만 정리된다는 것을 보여드렸습니다. `Rc::downgrade`를 호출하고 `Rc<T>`에 대한 참조를 전달하여 `Rc<T>` 인스턴스 내의 값에 대한 *약한 참조*를 만들 수도 있습니다. 강한 참조는 `Rc<T>` 인스턴스의 소유권을 공유하는 방법입니다. 약한 참조는 소유권 관계를 표현하지 않으며 카운트는 `Rc<T>` 인스턴스가 정리되는 시점에 영향을 미치지 않습니다. 약한 참조가 포함된 모든 순환은 강한 참조 카운트가 0이 되면 끊어지기 때문에 참조 순환을 유발하지 않습니다.

`Rc::downgrade`를 호출하면 `Weak<T>` 타입의 스마트 포인터를 얻습니다. `Rc<T>` 인스턴스의 `strong_count`를 1 증가시키는 대신 `Rc::downgrade`를 호출하면 `weak_count`가 1 증가합니다. `Rc<T>` 타입은 `strong_count`와 유사하게 얼마나 많은 `Weak<T>` 참조가 존재하는지 추적하기 위해 `weak_count`를 사용합니다. 차이점은 `Rc<T>` 인스턴스가 정리되기 위해 `weak_count`가 0일 필요가 없다는 것입니다.

`Weak<T>`가 참조하는 값이 드롭되었을 수 있으므로 `Weak<T>`가 가리키는 값으로 무언가를 하려면 값이 여전히 존재하는지 확인해야 합니다. `Weak<T>`에서 `upgrade` 메서드를 호출하여 이 작업을 수행합니다. 이 메서드는 `Option<Rc<T>>`를 반환합니다. `Rc<T>` 값이 아직 드롭되지 않았으면 `Some` 결과를 얻고 `Rc<T>` 값이 드롭되었으면 `None` 결과를 얻습니다. `upgrade`가 `Option<T>`를 반환하기 때문에 Rust는 `Some` 경우와 `None` 경우가 처리되도록 보장하고 유효하지 않은 포인터가 없습니다.

예를 들어 항목이 다음 항목에 대해서만 아는 리스트를 사용하는 대신 항목이 자식 항목 *및* 부모 항목에 대해 아는 트리를 만들 것입니다.

### 트리 데이터 구조 만들기: 자식 노드가 있는 `Node`

시작하기 위해 자식 노드에 대해 아는 노드가 있는 트리를 빌드할 것입니다. 자체 `i32` 값과 자식 `Node` 값에 대한 참조를 보유하는 `Node`라는 구조체를 만들 것입니다:

```rust
use std::cell::RefCell;
use std::rc::Rc;

#[derive(Debug)]
struct Node {
    value: i32,
    children: RefCell<Vec<Rc<Node>>>,
}
```

`Node`가 자식을 소유하기를 원하고 해당 소유권을 변수와 공유하여 트리의 각 `Node`에 직접 액세스할 수 있기를 원합니다. 이를 위해 `Vec<T>` 항목이 `Rc<Node>` 타입의 값이 되도록 정의합니다. 또한 어떤 노드가 다른 노드의 자식인지 수정할 수 있기를 원하므로 `Vec<Rc<Node>>` 주위에 `RefCell<T>`가 있는 `children`이 있습니다.

다음으로 구조체 정의를 사용하여 값 3을 가진 `leaf`라는 `Node` 인스턴스를 만들고 자식이 없으며, `leaf`를 자식 중 하나로 가진 값 5를 가진 `branch`라는 또 다른 인스턴스를 만듭니다:

```rust
fn main() {
    let leaf = Rc::new(Node {
        value: 3,
        children: RefCell::new(vec![]),
    });

    let branch = Rc::new(Node {
        value: 5,
        children: RefCell::new(vec![Rc::clone(&leaf)]),
    });
}
```

`leaf`의 `Rc<Node>`를 복제하여 `branch`에 저장하면 `leaf`의 `Node`가 이제 두 소유자: `leaf`와 `branch`를 가집니다. `branch.children`을 통해 `branch`에서 `leaf`로 갈 수 있지만 `leaf`에서 `branch`로 갈 방법이 없습니다. 그 이유는 `leaf`가 `branch`에 대한 참조가 없고 관련이 있다는 것을 모르기 때문입니다. `leaf`가 `branch`가 부모임을 알기를 원합니다. 다음에 그렇게 할 것입니다.

### 자식에서 부모로 참조 추가하기

자식 노드가 부모를 인식하도록 하려면 `Node` 구조체 정의에 `parent` 필드를 추가해야 합니다. 문제는 `parent` 타입이 무엇이어야 하는지 결정하는 것입니다. `Rc<T>`를 포함할 수 없다는 것을 알고 있습니다. 그러면 `branch`를 가리키는 `leaf.parent`와 `leaf`를 가리키는 `branch.children`으로 참조 순환이 생성되어 `strong_count` 값이 절대 0이 되지 않기 때문입니다.

관계를 다른 방식으로 생각해 보면 부모 노드가 자식을 소유해야 합니다: 부모 노드가 드롭되면 자식 노드도 드롭되어야 합니다. 그러나 자식은 부모를 소유해서는 안 됩니다: 자식 노드를 드롭하면 부모가 여전히 존재해야 합니다. 이것이 약한 참조의 경우입니다!

따라서 `Rc<T>` 대신 `parent`의 타입이 `Weak<T>`, 특히 `RefCell<Weak<Node>>`를 사용하도록 합니다. 이제 `Node` 구조체 정의는 다음과 같습니다:

```rust
use std::cell::RefCell;
use std::rc::{Rc, Weak};

#[derive(Debug)]
struct Node {
    value: i32,
    parent: RefCell<Weak<Node>>,
    children: RefCell<Vec<Rc<Node>>>,
}
```

노드가 부모 노드를 참조할 수 있지만 부모를 소유하지는 않습니다. Listing 15-28에서 `leaf`가 부모 `branch`에 대한 경로를 갖도록 이 새 정의를 사용하도록 `main`을 업데이트합니다:

```rust
fn main() {
    let leaf = Rc::new(Node {
        value: 3,
        parent: RefCell::new(Weak::new()),
        children: RefCell::new(vec![]),
    });

    println!("leaf 부모 = {:?}", leaf.parent.borrow().upgrade());

    let branch = Rc::new(Node {
        value: 5,
        parent: RefCell::new(Weak::new()),
        children: RefCell::new(vec![Rc::clone(&leaf)]),
    });

    *leaf.parent.borrow_mut() = Rc::downgrade(&branch);

    println!("leaf 부모 = {:?}", leaf.parent.borrow().upgrade());
}
```

*Listing 15-28: 부모 노드 `branch`에 대한 약한 참조가 있는 `leaf` 노드*

`leaf` 노드 생성은 `parent` 필드를 제외하고 Listing 15-27과 유사합니다: `leaf`는 부모 없이 시작하므로 빈 `Weak<Node>` 참조 인스턴스를 만듭니다.

이 시점에서 `upgrade` 메서드를 사용하여 `leaf`의 부모에 대한 참조를 얻으려고 하면 `None` 값을 얻습니다. 첫 번째 `println!` 문의 출력에서 이것을 볼 수 있습니다:

```text
leaf 부모 = None
```

`branch` 노드를 만들 때도 `parent` 필드에 새 `Weak<Node>` 참조가 있습니다. `branch`에는 부모 노드가 없기 때문입니다. 여전히 `leaf`가 `branch`의 자식 중 하나입니다. `branch`에 `Node` 인스턴스가 있으면 `leaf`를 수정하여 부모에 대한 `Weak<Node>` 참조를 제공할 수 있습니다. `leaf`의 `parent` 필드에서 `RefCell<Weak<Node>>`의 `borrow_mut` 메서드를 사용한 다음 `Rc::downgrade` 함수를 사용하여 `branch`의 `Rc<Node>`에서 `branch`에 대한 `Weak<Node>` 참조를 만듭니다.

`leaf`의 부모를 다시 출력하면 이번에는 `branch`를 보유하는 `Some` 변형을 얻습니다: 이제 `leaf`가 부모에 액세스할 수 있습니다! `leaf`를 출력할 때 Listing 15-26에서 발생한 것과 같이 결국 스택 오버플로로 끝나는 순환도 피합니다; `Weak<Node>` 참조는 `(Weak)`로 출력됩니다:

```text
leaf 부모 = Some(Node { value: 5, parent: RefCell { value: (Weak) },
children: RefCell { value: [Node { value: 3, parent: RefCell { value: (Weak) },
children: RefCell { value: [] } }] } })
```

무한 출력이 없다는 것은 이 코드가 참조 순환을 만들지 않았음을 나타냅니다. `Rc::strong_count`와 `Rc::weak_count` 호출에서 얻은 값을 살펴보면 이것을 알 수 있습니다.

### `strong_count`와 `weak_count` 변경 사항 시각화하기

내부 범위를 만들고 `branch` 생성을 해당 범위로 이동하여 `Rc<Node>` 인스턴스의 `strong_count`와 `weak_count` 값이 어떻게 변경되는지 살펴보겠습니다. 그렇게 하면 `branch`가 생성된 다음 범위를 벗어날 때 드롭될 때 무슨 일이 발생하는지 볼 수 있습니다. 수정 사항은 Listing 15-29에 나와 있습니다:

```rust
fn main() {
    let leaf = Rc::new(Node {
        value: 3,
        parent: RefCell::new(Weak::new()),
        children: RefCell::new(vec![]),
    });

    println!(
        "leaf strong = {}, weak = {}",
        Rc::strong_count(&leaf),
        Rc::weak_count(&leaf),
    );

    {
        let branch = Rc::new(Node {
            value: 5,
            parent: RefCell::new(Weak::new()),
            children: RefCell::new(vec![Rc::clone(&leaf)]),
        });

        *leaf.parent.borrow_mut() = Rc::downgrade(&branch);

        println!(
            "branch strong = {}, weak = {}",
            Rc::strong_count(&branch),
            Rc::weak_count(&branch),
        );

        println!(
            "leaf strong = {}, weak = {}",
            Rc::strong_count(&leaf),
            Rc::weak_count(&leaf),
        );
    }

    println!("leaf 부모 = {:?}", leaf.parent.borrow().upgrade());
    println!(
        "leaf strong = {}, weak = {}",
        Rc::strong_count(&leaf),
        Rc::weak_count(&leaf),
    );
}
```

*Listing 15-29: 내부 범위에서 `branch`를 만들고 강한 및 약한 참조 카운트 검사*

`leaf`가 생성된 후 `Rc<Node>`는 강한 카운트 1과 약한 카운트 0을 가집니다. 내부 범위에서 `branch`를 만들고 `leaf`와 연결합니다. 이 시점에서 카운트를 출력하면 `branch`의 `Rc<Node>`는 강한 카운트 1과 약한 카운트 1을 가집니다(`leaf.parent`가 `Weak<Node>`로 `branch`를 가리키기 때문). `leaf`에 대한 카운트를 출력하면 강한 카운트 2를 가지는 것을 볼 수 있습니다. `branch`가 이제 `branch.children`에 저장된 `leaf`의 `Rc<Node>` 복제본이 있지만 여전히 약한 카운트 0을 가지기 때문입니다.

내부 범위가 끝나면 `branch`가 범위를 벗어나고 `Rc<Node>`의 강한 카운트가 0으로 감소하므로 `Node`가 드롭됩니다. `leaf.parent`의 약한 카운트 1은 `Node`가 드롭되는지 여부에 영향을 미치지 않으므로 메모리 누수가 없습니다!

범위 끝 이후에 `leaf`의 부모에 액세스하려고 하면 다시 `None`을 얻습니다. 프로그램 끝에서 `leaf`의 `Rc<Node>`는 `leaf` 변수가 이제 `Rc<Node>`에 대한 유일한 참조이기 때문에 강한 카운트 1과 약한 카운트 0을 가집니다.

카운트와 값 드롭을 관리하는 모든 로직은 `Rc<T>`와 `Weak<T>` 및 `Drop` 트레이트의 구현에 내장되어 있습니다. 노드 정의에서 자식에서 부모로의 관계가 `Weak<T>` 참조여야 함을 지정함으로써 메모리 누수와 참조 순환을 만들지 않고 부모 노드가 자식 노드를 가리키고 그 반대로도 할 수 있습니다.

## 요약

이 장에서는 스마트 포인터를 사용하여 Rust가 기본적으로 일반 참조로 만드는 것과 다른 보장과 트레이드오프를 만드는 방법을 다루었습니다. `Box<T>` 타입은 알려진 크기를 가지고 힙에 할당된 데이터를 가리킵니다. `Rc<T>` 타입은 힙의 데이터에 대한 참조 수를 추적하여 해당 데이터가 여러 소유자를 가질 수 있도록 합니다. `RefCell<T>` 타입은 내부 가변성을 통해 불변 타입이 필요하지만 해당 타입의 내부 값을 변경해야 할 때 사용할 수 있는 타입을 제공합니다; 또한 컴파일 시간이 아닌 런타임에 빌림 규칙을 적용합니다.

또한 스마트 포인터 타입의 많은 기능을 가능하게 하는 `Deref`와 `Drop` 트레이트에 대해서도 논의했습니다. 메모리 누수를 유발할 수 있는 참조 순환과 `Weak<T>`를 사용하여 방지하는 방법을 탐구했습니다.

이 장이 관심을 끌었고 직접 스마트 포인터를 구현하고 싶다면 ["The Rustonomicon"](https://doc.rust-lang.org/nomicon/index.html)에서 더 유용한 정보를 확인하세요.

다음으로 Rust의 동시성에 대해 이야기하겠습니다. 몇 가지 새로운 스마트 포인터에 대해서도 배우게 됩니다.
