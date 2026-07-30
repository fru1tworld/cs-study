# 러스트 스마트 포인터, 동시성, Async/Await

## 스마트 포인터

## 스마트 포인터

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

## `Box<T>`를 사용하여 힙의 데이터 가리키기

> **원문:** https://doc.rust-lang.org/book/ch15-01-box.html

가장 직접적인 스마트 포인터는 *박스*이며, 타입은 `Box<T>`로 작성됩니다. 박스를 사용하면 스택이 아닌 힙에 데이터를 저장할 수 있습니다. 스택에 남는 것은 힙 데이터에 대한 포인터입니다. 스택과 힙의 차이점을 검토하려면 4장을 참조하세요.

박스는 힙에 데이터를 저장하는 것 외에는 성능 오버헤드가 없습니다. 그러나 많은 추가 기능도 없습니다. 다음 상황에서 가장 자주 사용합니다:

* 컴파일 시간에 크기를 알 수 없는 타입이 있고 정확한 크기를 필요로 하는 컨텍스트에서 해당 타입의 값을 사용하려는 경우
* 큰 양의 데이터가 있고 소유권을 이전하고 싶지만 이전할 때 데이터가 복사되지 않도록 하려는 경우
* 값을 소유하고 특정 타입이 아닌 특정 트레이트를 구현하는 타입이라는 것만 신경 쓰려는 경우

첫 번째 상황은 "박스로 재귀 타입 활성화하기" 섹션에서 보여드리겠습니다. 두 번째 경우에서 큰 양의 데이터 소유권을 이전하면 데이터가 스택에서 복사되기 때문에 오랜 시간이 걸릴 수 있습니다. 이 상황에서 성능을 향상시키기 위해 힙의 박스에 큰 양의 데이터를 저장할 수 있습니다. 그런 다음 소유권이 이전될 때 작은 양의 포인터 데이터만 스택에서 복사되고 데이터가 참조하는 데이터는 힙의 한 곳에 유지됩니다. 세 번째 경우는 *트레이트 객체*로 알려져 있으며 18장에서 전체 섹션을 다룹니다. 따라서 여기서 배운 것은 18장에서 다시 적용될 것입니다!

### `Box<T>`를 사용하여 힙에 데이터 저장하기

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

### 박스로 재귀 타입 활성화하기

*재귀 타입*의 값은 자체 타입의 다른 값을 자신의 일부로 가질 수 있습니다. 재귀 타입은 Rust가 컴파일 시간에 타입이 차지하는 공간을 알아야 하기 때문에 문제가 됩니다. 그러나 재귀 타입의 값 중첩은 이론적으로 무한히 계속될 수 있으므로 Rust는 값에 필요한 공간이 얼마인지 알 수 없습니다. 박스는 알려진 크기를 가지므로 재귀 타입 정의에 박스를 삽입하여 재귀 타입을 가질 수 있습니다.

재귀 타입의 예로 *cons 리스트*를 살펴보겠습니다. 이것은 함수형 프로그래밍 언어에서 흔히 볼 수 있는 데이터 타입입니다. 우리가 정의할 cons 리스트 타입은 재귀를 제외하면 간단합니다; 따라서 우리가 작업할 예제의 개념은 재귀 타입과 관련된 더 복잡한 상황에 처할 때마다 유용할 것입니다.

#### Cons 리스트에 대한 자세한 정보

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

#### 비재귀 타입의 크기 계산하기

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

#### 알려진 크기의 재귀 타입을 얻기 위해 `Box<T>` 사용하기

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

## `Deref` 트레이트로 스마트 포인터를 일반 참조처럼 취급하기

> **원문:** https://doc.rust-lang.org/book/ch15-02-deref.html

`Deref` 트레이트를 구현하면 *역참조 연산자* `*`의 동작을 사용자 정의할 수 있습니다(곱셈이나 글롭 연산자와 혼동하지 마세요). 스마트 포인터가 일반 참조처럼 취급될 수 있도록 `Deref`를 구현하면 참조에서 작동하는 코드를 작성하고 해당 코드를 스마트 포인터와 함께 사용할 수도 있습니다.

먼저 역참조 연산자가 일반 참조와 어떻게 작동하는지 살펴보겠습니다. 그런 다음 `Box<T>`처럼 동작하는 사용자 정의 타입을 정의하고 새로 정의한 타입에서 역참조 연산자가 작동하지 않는 이유를 살펴보겠습니다. `Deref` 트레이트를 구현하면 스마트 포인터가 참조와 유사한 방식으로 작동할 수 있는 방법을 살펴볼 것입니다. 그런 다음 Rust의 *역참조 강제 변환* 기능과 참조 또는 스마트 포인터와 함께 작동하는 방법을 살펴보겠습니다.

> 참고: 빌드할 `MyBox<T>` 타입과 실제 `Box<T>` 사이에는 큰 차이가 있습니다: 우리 버전은 데이터를 힙에 저장하지 않습니다. 이 예제를 `Deref`에 집중하고 있으므로 데이터가 실제로 어디에 저장되는지는 포인터와 같은 동작보다 덜 중요합니다.

### 포인터를 따라가서 값 얻기

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

### `Box<T>`를 참조처럼 사용하기

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

### 자체 스마트 포인터 정의하기

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

### `Deref` 트레이트를 구현하여 타입을 참조처럼 취급하기

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

### 함수와 메서드에서 암묵적 역참조 강제 변환

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

### 역참조 강제 변환이 가변성과 상호 작용하는 방식

불변 참조에서 `*` 연산자를 오버라이드하기 위해 `Deref` 트레이트를 사용하는 방법과 유사하게, `DerefMut` 트레이트를 사용하여 가변 참조에서 `*` 연산자를 오버라이드할 수 있습니다.

Rust는 다음 세 가지 경우에 타입과 트레이트 구현을 발견하면 역참조 강제 변환을 수행합니다:

* `T: Deref<Target=U>`일 때 `&T`에서 `&U`로
* `T: DerefMut<Target=U>`일 때 `&mut T`에서 `&mut U`로
* `T: Deref<Target=U>`일 때 `&mut T`에서 `&U`로

처음 두 경우는 가변성을 제외하면 동일합니다. 첫 번째 경우는 `&T`가 있고 `T`가 어떤 타입 `U`에 대해 `Deref`를 구현하면 `&U`를 투명하게 얻을 수 있다고 말합니다. 두 번째 경우는 가변 참조에 대해 동일한 역참조 강제 변환이 발생한다고 말합니다.

세 번째 경우는 더 까다롭습니다: Rust는 가변 참조를 불변 참조로도 강제 변환합니다. 그러나 그 반대는 *불가능*합니다: 불변 참조는 절대로 가변 참조로 강제 변환되지 않습니다. 빌림 규칙 때문에 가변 참조가 있으면 해당 가변 참조가 해당 데이터에 대한 유일한 참조여야 합니다(그렇지 않으면 프로그램이 컴파일되지 않습니다). 하나의 가변 참조를 하나의 불변 참조로 변환해도 빌림 규칙을 위반하지 않습니다. 불변 참조를 가변 참조로 변환하려면 해당 데이터에 대한 초기 불변 참조가 해당 불변 참조 하나만 있어야 하지만 빌림 규칙은 이를 보장하지 않습니다. 따라서 Rust는 불변 참조를 가변 참조로 변환하는 것이 가능하다고 가정할 수 없습니다.

---

## `Drop` 트레이트로 정리 시 코드 실행하기

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

### `std::mem::drop`으로 값을 일찍 드롭하기

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

## `Rc<T>`, 참조 카운팅 스마트 포인터

> **원문:** https://doc.rust-lang.org/book/ch15-04-rc.html

대부분의 경우 소유권은 명확합니다: 주어진 값을 어떤 변수가 소유하는지 정확히 알 수 있습니다. 그러나 단일 값이 여러 소유자를 가질 수 있는 경우가 있습니다. 예를 들어 그래프 데이터 구조에서 여러 에지가 같은 노드를 가리킬 수 있으며, 해당 노드는 개념적으로 자신을 가리키는 모든 에지가 소유합니다. 노드는 에지가 가리키지 않아 소유자가 없을 때까지 정리되어서는 안 됩니다.

`Rc<T>` 타입을 사용하면 *참조 카운팅*을 통해 여러 소유권을 명시적으로 활성화해야 합니다. `Rc<T>`는 "reference counting"의 약자입니다. `Rc<T>` 타입은 값에 대한 참조 수를 추적하여 값이 여전히 사용 중인지 확인합니다. 값에 대한 참조가 0개이면 참조가 유효하지 않게 되지 않고 값을 정리할 수 있습니다.

`Rc<T>`를 거실의 TV로 상상해 보세요. 한 사람이 들어와서 TV를 볼 때 켭니다. 다른 사람들이 방에 들어와서 TV를 볼 수 있습니다. 마지막 사람이 거실을 떠날 때 더 이상 사용되지 않으므로 TV를 끕니다. 다른 사람들이 여전히 보고 있을 때 누군가가 TV를 끄면 나머지 TV 시청자들의 항의가 있을 것입니다!

프로그램의 여러 부분이 읽을 수 있도록 힙에 일부 데이터를 할당하고 컴파일 시간에 어느 부분이 데이터 사용을 마지막으로 끝낼지 결정할 수 없을 때 `Rc<T>` 타입을 사용합니다. 어느 부분이 마지막으로 끝낼지 알았다면 해당 부분을 데이터의 소유자로 만들 수 있으며, 컴파일 시간에 적용되는 일반 소유권 규칙이 적용됩니다.

`Rc<T>`는 단일 스레드 시나리오에서만 사용할 수 있습니다. 16장에서 동시성에 대해 논의할 때 다중 스레드 프로그램에서 참조 카운팅을 수행하는 방법을 다룰 것입니다.

### `Rc<T>`를 사용하여 데이터 공유하기

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

### `Rc<T>` 복제는 참조 카운트를 증가시킵니다

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

## `RefCell<T>`와 내부 가변성 패턴

> **원문:** https://doc.rust-lang.org/book/ch15-05-interior-mutability.html

*내부 가변성*은 해당 데이터에 대한 불변 참조가 있을 때도 데이터를 변형할 수 있게 하는 Rust의 디자인 패턴입니다; 일반적으로 이 행동은 빌림 규칙에 의해 허용되지 않습니다. 데이터를 변형하기 위해 패턴은 데이터 구조 내에서 `unsafe` 코드를 사용하여 변형과 빌림을 관리하는 Rust의 일반적인 규칙을 우회합니다. 안전하지 않은 코드는 컴파일러에게 규칙을 확인하는 대신 직접 규칙을 확인하고 있음을 나타냅니다; 안전하지 않은 코드는 20장에서 자세히 논의합니다.

내부 가변성 패턴을 사용하는 타입은 컴파일러가 보장할 수 없더라도 런타임에 빌림 규칙을 따를 것이 보장될 때만 사용할 수 있습니다. 관련된 `unsafe` 코드는 안전한 API로 래핑되고 외부 타입은 여전히 불변입니다.

내부 가변성 패턴을 따르는 `RefCell<T>` 타입을 살펴보며 이 개념을 탐구해 봅시다.

### `RefCell<T>`로 런타임에 빌림 규칙 적용하기

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

### 내부 가변성: 불변 값에 대한 가변 빌림

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

#### 내부 가변성 사용 사례: 모의 객체

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

#### `RefCell<T>`로 런타임에 빌림 추적하기

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

### `Rc<T>`와 `RefCell<T>` 결합하여 가변 데이터의 여러 소유자 갖기

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

## 참조 순환은 메모리를 누수시킬 수 있습니다

> **원문:** https://doc.rust-lang.org/book/ch15-06-reference-cycles.html

Rust의 메모리 안전 보장은 절대 정리되지 않는 메모리(*메모리 누수*라고 함)를 실수로 만드는 것을 어렵게 하지만 불가능하게 하지는 않습니다. 메모리 누수를 완전히 방지하는 것은 컴파일 시간에 데이터 레이스를 허용하지 않는 것과 같은 방식으로 Rust의 보장 중 하나가 아닙니다. 즉, 메모리 누수는 Rust에서 메모리 안전합니다. `Rc<T>`와 `RefCell<T>`를 사용하여 Rust가 메모리 누수를 허용하는 것을 볼 수 있습니다: 항목이 순환에서 서로를 참조하는 참조를 만들 수 있습니다. 이것은 순환의 각 항목의 참조 카운트가 절대 0이 되지 않고 값이 절대 드롭되지 않기 때문에 메모리 누수를 생성합니다.

### 참조 순환 만들기

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

### 참조 순환 방지하기: `Rc<T>`를 `Weak<T>`로 변환하기

지금까지 `Rc::clone`을 호출하면 `Rc<T>` 인스턴스의 `strong_count`가 증가하고 `Rc<T>` 인스턴스는 `strong_count`가 0일 때만 정리된다는 것을 보여드렸습니다. `Rc::downgrade`를 호출하고 `Rc<T>`에 대한 참조를 전달하여 `Rc<T>` 인스턴스 내의 값에 대한 *약한 참조*를 만들 수도 있습니다. 강한 참조는 `Rc<T>` 인스턴스의 소유권을 공유하는 방법입니다. 약한 참조는 소유권 관계를 표현하지 않으며 카운트는 `Rc<T>` 인스턴스가 정리되는 시점에 영향을 미치지 않습니다. 약한 참조가 포함된 모든 순환은 강한 참조 카운트가 0이 되면 끊어지기 때문에 참조 순환을 유발하지 않습니다.

`Rc::downgrade`를 호출하면 `Weak<T>` 타입의 스마트 포인터를 얻습니다. `Rc<T>` 인스턴스의 `strong_count`를 1 증가시키는 대신 `Rc::downgrade`를 호출하면 `weak_count`가 1 증가합니다. `Rc<T>` 타입은 `strong_count`와 유사하게 얼마나 많은 `Weak<T>` 참조가 존재하는지 추적하기 위해 `weak_count`를 사용합니다. 차이점은 `Rc<T>` 인스턴스가 정리되기 위해 `weak_count`가 0일 필요가 없다는 것입니다.

`Weak<T>`가 참조하는 값이 드롭되었을 수 있으므로 `Weak<T>`가 가리키는 값으로 무언가를 하려면 값이 여전히 존재하는지 확인해야 합니다. `Weak<T>`에서 `upgrade` 메서드를 호출하여 이 작업을 수행합니다. 이 메서드는 `Option<Rc<T>>`를 반환합니다. `Rc<T>` 값이 아직 드롭되지 않았으면 `Some` 결과를 얻고 `Rc<T>` 값이 드롭되었으면 `None` 결과를 얻습니다. `upgrade`가 `Option<T>`를 반환하기 때문에 Rust는 `Some` 경우와 `None` 경우가 처리되도록 보장하고 유효하지 않은 포인터가 없습니다.

예를 들어 항목이 다음 항목에 대해서만 아는 리스트를 사용하는 대신 항목이 자식 항목 *및* 부모 항목에 대해 아는 트리를 만들 것입니다.

#### 트리 데이터 구조 만들기: 자식 노드가 있는 `Node`

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

#### 자식에서 부모로 참조 추가하기

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

#### `strong_count`와 `weak_count` 변경 사항 시각화하기

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

### 요약

이 장에서는 스마트 포인터를 사용하여 Rust가 기본적으로 일반 참조로 만드는 것과 다른 보장과 트레이드오프를 만드는 방법을 다루었습니다. `Box<T>` 타입은 알려진 크기를 가지고 힙에 할당된 데이터를 가리킵니다. `Rc<T>` 타입은 힙의 데이터에 대한 참조 수를 추적하여 해당 데이터가 여러 소유자를 가질 수 있도록 합니다. `RefCell<T>` 타입은 내부 가변성을 통해 불변 타입이 필요하지만 해당 타입의 내부 값을 변경해야 할 때 사용할 수 있는 타입을 제공합니다; 또한 컴파일 시간이 아닌 런타임에 빌림 규칙을 적용합니다.

또한 스마트 포인터 타입의 많은 기능을 가능하게 하는 `Deref`와 `Drop` 트레이트에 대해서도 논의했습니다. 메모리 누수를 유발할 수 있는 참조 순환과 `Weak<T>`를 사용하여 방지하는 방법을 탐구했습니다.

이 장이 관심을 끌었고 직접 스마트 포인터를 구현하고 싶다면 ["The Rustonomicon"](https://doc.rust-lang.org/nomicon/index.html)에서 더 유용한 정보를 확인하세요.

다음으로 Rust의 동시성에 대해 이야기하겠습니다. 몇 가지 새로운 스마트 포인터에 대해서도 배우게 됩니다.

---

## 동시성 프로그래밍

## 겁 없는 동시성

> **원문:** https://doc.rust-lang.org/book/ch16-00-concurrency.html

동시 프로그래밍을 안전하고 효율적으로 처리하는 것은 Rust의 또 다른 주요 목표 중 하나입니다. *동시 프로그래밍*은 프로그램의 다른 부분이 독립적으로 실행되는 것이고, *병렬 프로그래밍*은 프로그램의 다른 부분이 동시에 실행되는 것입니다. 이 두 개념은 역사적으로 별도로 처리하기 어려웠고 안전하게 프로그래밍하기 어려웠습니다. 컴퓨터가 여러 프로세서를 활용함에 따라 점점 더 중요해지고 있습니다. Rust의 소유권 시스템과 타입 시스템은 이 문제를 관리하는 데 도움이 되는 강력한 도구 세트입니다.

많은 언어들은 동시성 문제에 제공할 수 있는 잠재적 해결책에 대해 교조적입니다. 예를 들어 Erlang은 메시지 전달 동시성에 대한 우아한 기능이 있지만 스레드 간에 상태를 공유하는 모호한 방법만 있습니다. 가능한 해결책의 하위 집합만 지원하는 것은 높은 수준의 언어에 대한 합리적인 전략입니다. 높은 수준의 언어는 추상화를 얻기 위해 일부 제어를 포기하기로 약속하기 때문입니다. 그러나 낮은 수준의 언어는 주어진 상황에서 최고의 성능으로 어떤 솔루션이든 제공해야 하고 하드웨어에 대한 추상화가 적어야 합니다. 따라서 Rust는 상황과 요구 사항에 적합한 방식으로 문제를 모델링하기 위한 다양한 도구를 제공합니다.

다음은 이 장에서 다룰 주제입니다:

* 여러 코드 조각을 동시에 실행하기 위해 스레드를 만드는 방법
* 채널을 사용하여 스레드 간에 메시지를 보내는 *메시지 전달* 동시성
* 여러 스레드가 일부 데이터에 액세스할 수 있는 *공유 상태* 동시성
* 표준 라이브러리 타입뿐만 아니라 사용자 정의 타입에도 Rust의 동시성 보장을 확장하는 `Sync` 및 `Send` 트레이트

다음 장에서는 Rust에서 동시성에 대한 또 다른 접근 방식인 async/await을 다룰 것입니다.

---

## 스레드를 사용하여 코드를 동시에 실행하기

> **원문:** https://doc.rust-lang.org/book/ch16-01-threads.html

대부분의 현재 운영 체제에서 실행된 프로그램의 코드는 *프로세스*에서 실행되고 운영 체제는 여러 프로세스를 동시에 관리합니다. 프로그램 내에서 동시에 실행되는 독립적인 부분도 가질 수 있습니다. 이러한 독립적인 부분을 실행하는 기능을 *스레드*라고 합니다. 예를 들어 웹 서버는 여러 스레드를 가져 동시에 둘 이상의 요청에 응답할 수 있습니다.

프로그램의 계산을 여러 스레드로 분할하여 동시에 여러 작업을 실행하면 성능을 향상시킬 수 있지만 복잡성도 추가됩니다. 스레드는 동시에 실행될 수 있으므로 다른 스레드에서 코드 부분이 실행되는 순서에 대한 본질적인 보장이 없습니다. 이것은 다음과 같은 문제를 일으킬 수 있습니다:

* 스레드가 일관되지 않은 순서로 데이터나 리소스에 액세스하는 레이스 조건
* 두 스레드가 서로를 기다리며 둘 다 계속 진행하지 못하는 교착 상태
* 특정 상황에서만 발생하여 재현 및 신뢰성 있게 수정하기 어려운 버그

Rust는 스레드 사용의 부정적인 효과를 완화하려고 시도하지만 다중 스레드 컨텍스트에서 프로그래밍하는 것은 여전히 신중한 생각이 필요하고 단일 스레드에서 실행되는 프로그램과 다른 코드 구조가 필요합니다.

프로그래밍 언어는 스레드를 몇 가지 다른 방식으로 구현하며 많은 운영 체제는 새 스레드를 생성하기 위해 언어가 호출할 수 있는 API를 제공합니다. Rust 표준 라이브러리는 스레드 구현의 *1:1* 모델을 사용합니다. 이 모델에서 프로그램은 언어 스레드당 하나의 운영 체제 스레드를 사용합니다. 다른 트레이드오프를 만드는 다른 스레딩 모델을 구현하는 크레이트가 있습니다.

### `spawn`으로 새 스레드 만들기

새 스레드를 만들려면 `thread::spawn` 함수를 호출하고 새 스레드에서 실행하려는 코드를 포함하는 클로저(13장에서 클로저에 대해 이야기했습니다)를 전달합니다. Listing 16-1의 예제는 메인 스레드에서 일부 텍스트를 출력하고 새 스레드에서 다른 텍스트를 출력합니다:

```rust
use std::thread;
use std::time::Duration;

fn main() {
    thread::spawn(|| {
        for i in 1..10 {
            println!("hi number {i} from the spawned thread!");
            thread::sleep(Duration::from_millis(1));
        }
    });

    for i in 1..5 {
        println!("hi number {i} from the main thread!");
        thread::sleep(Duration::from_millis(1));
    }
}
```

*Listing 16-1: 메인 스레드가 다른 것을 출력하는 동안 무언가를 출력하는 새 스레드 생성*

메인 스레드가 완료되면 생성된 스레드가 실행을 마쳤는지 여부에 관계없이 모든 생성된 스레드가 종료됩니다. 이 프로그램의 출력은 매번 약간 다를 수 있지만 다음과 유사하게 보일 것입니다:

```text
hi number 1 from the main thread!
hi number 1 from the spawned thread!
hi number 2 from the main thread!
hi number 2 from the spawned thread!
hi number 3 from the main thread!
hi number 3 from the spawned thread!
hi number 4 from the main thread!
hi number 4 from the spawned thread!
hi number 5 from the spawned thread!
```

`thread::sleep` 호출은 스레드가 짧은 시간 동안 실행을 중지하도록 강제하여 다른 스레드가 실행될 수 있게 합니다. 스레드는 아마도 교대로 실행되겠지만 보장되지는 않습니다: 운영 체제가 스레드를 스케줄링하는 방식에 따라 다릅니다. 이 실행에서 메인 스레드가 먼저 출력했지만 생성된 스레드의 print 문이 코드에서 먼저 나타납니다. 그리고 생성된 스레드가 `i`가 9일 때까지 출력하도록 지시했지만 메인 스레드가 종료되기 전에 5까지만 도달했습니다.

이 코드를 실행하고 메인 스레드의 출력만 표시되거나 겹침이 표시되지 않으면 범위의 숫자를 늘려 운영 체제가 스레드 간에 전환할 기회를 더 많이 만들어 보세요.

### `join` 핸들을 사용하여 모든 스레드가 완료될 때까지 대기하기

Listing 16-1의 코드는 메인 스레드가 끝나기 때문에 생성된 스레드를 대부분 조기에 중지할 뿐만 아니라 스레드가 실행되는 순서에 대한 보장이 없기 때문에 생성된 스레드가 전혀 실행될지 보장할 수도 없습니다!

`thread::spawn`의 반환 값을 변수에 저장하여 생성된 스레드가 실행되지 않거나 조기에 종료되는 문제를 해결할 수 있습니다. `thread::spawn`의 반환 타입은 `JoinHandle<T>`입니다. `JoinHandle<T>`은 `join` 메서드를 호출할 때 스레드가 완료될 때까지 대기하는 소유된 값입니다. Listing 16-2는 Listing 16-1에서 만든 스레드의 `JoinHandle<T>`를 사용하고 `join`을 호출하여 `main`이 종료되기 전에 생성된 스레드가 완료되도록 하는 방법을 보여줍니다:

```rust
use std::thread;
use std::time::Duration;

fn main() {
    let handle = thread::spawn(|| {
        for i in 1..10 {
            println!("hi number {i} from the spawned thread!");
            thread::sleep(Duration::from_millis(1));
        }
    });

    for i in 1..5 {
        println!("hi number {i} from the main thread!");
        thread::sleep(Duration::from_millis(1));
    }

    handle.join().unwrap();
}
```

*Listing 16-2: `thread::spawn`에서 `JoinHandle`을 저장하여 스레드가 완료될 때까지 실행되도록 보장*

핸들에서 `join`을 호출하면 핸들이 나타내는 스레드가 종료될 때까지 현재 실행 중인 스레드가 차단됩니다. 스레드를 *차단*한다는 것은 해당 스레드가 작업을 수행하거나 종료하는 것이 방지됨을 의미합니다. 메인 스레드의 `for` 루프 뒤에 `join` 호출을 넣었기 때문에 Listing 16-2를 실행하면 다음과 유사한 출력이 생성됩니다:

```text
hi number 1 from the main thread!
hi number 2 from the main thread!
hi number 1 from the spawned thread!
hi number 3 from the main thread!
hi number 2 from the spawned thread!
hi number 4 from the main thread!
hi number 3 from the spawned thread!
hi number 4 from the spawned thread!
hi number 5 from the spawned thread!
hi number 6 from the spawned thread!
hi number 7 from the spawned thread!
hi number 8 from the spawned thread!
hi number 9 from the spawned thread!
```

두 스레드는 계속 교대로 실행되지만 `handle.join()` 호출로 인해 메인 스레드가 대기하고 생성된 스레드가 완료될 때까지 종료되지 않습니다.

그러나 대신 `handle.join()`을 `main`의 `for` 루프 앞으로 이동하면 무슨 일이 발생하는지 봅시다:

```rust
use std::thread;
use std::time::Duration;

fn main() {
    let handle = thread::spawn(|| {
        for i in 1..10 {
            println!("hi number {i} from the spawned thread!");
            thread::sleep(Duration::from_millis(1));
        }
    });

    handle.join().unwrap();

    for i in 1..5 {
        println!("hi number {i} from the main thread!");
        thread::sleep(Duration::from_millis(1));
    }
}
```

메인 스레드는 생성된 스레드가 완료될 때까지 대기한 다음 `for` 루프를 실행하므로 출력이 더 이상 인터리브되지 않습니다:

```text
hi number 1 from the spawned thread!
hi number 2 from the spawned thread!
hi number 3 from the spawned thread!
hi number 4 from the spawned thread!
hi number 5 from the spawned thread!
hi number 6 from the spawned thread!
hi number 7 from the spawned thread!
hi number 8 from the spawned thread!
hi number 9 from the spawned thread!
hi number 1 from the main thread!
hi number 2 from the main thread!
hi number 3 from the main thread!
hi number 4 from the main thread!
```

`join`이 호출되는 위치와 같은 작은 세부 사항이 스레드가 동시에 실행되는지 여부에 영향을 줄 수 있습니다.

### 스레드에서 `move` 클로저 사용하기

`thread::spawn`에 전달되는 클로저와 함께 `move` 키워드를 자주 사용합니다. 왜냐하면 클로저가 환경에서 사용하는 값의 소유권을 가져가서 해당 값의 소유권을 한 스레드에서 다른 스레드로 이전하기 때문입니다. 13장의 "참조 캡처 또는 소유권 이동" 섹션에서 클로저 컨텍스트에서 `move`에 대해 논의했습니다. 이제 `move`와 `thread::spawn` 사이의 상호 작용에 더 집중할 것입니다.

Listing 16-1에서 `thread::spawn`에 전달하는 클로저는 인수를 받지 않습니다: 생성된 스레드의 코드에서 메인 스레드의 데이터를 사용하지 않습니다. 생성된 스레드에서 메인 스레드의 데이터를 사용하려면 생성된 스레드의 클로저가 필요한 값을 캡처해야 합니다. Listing 16-3은 메인 스레드에서 벡터를 만들고 생성된 스레드에서 사용하려는 시도를 보여줍니다. 그러나 이것은 잠시 후에 볼 수 있듯이 아직 작동하지 않습니다.

```rust
use std::thread;

fn main() {
    let v = vec![1, 2, 3];

    let handle = thread::spawn(|| {
        println!("Here's a vector: {v:?}");
    });

    handle.join().unwrap();
}
```

*Listing 16-3: 메인 스레드에서 만든 벡터를 다른 스레드에서 사용하려는 시도*

클로저는 `v`를 사용하므로 `v`를 캡처하여 클로저 환경의 일부로 만듭니다. `thread::spawn`이 이 클로저를 새 스레드에서 실행하므로 새 스레드 내에서 `v`에 액세스할 수 있어야 합니다. 그러나 이 예제를 컴파일하면 다음 오류가 발생합니다:

```console
$ cargo run
   Compiling threads v0.1.0 (file:///projects/threads)
error[E0373]: closure may outlive the current function, but it borrows `v`, which is owned by the current function
 --> src/main.rs:6:32
  |
6 |     let handle = thread::spawn(|| {
  |                                ^^ may outlive borrowed value `v`
7 |         println!("Here's a vector: {v:?}");
  |                                     - `v` is borrowed here
  |
note: function requires argument type to outlive `'static`
 --> src/main.rs:6:18
  |
6 |       let handle = thread::spawn(|| {
  |  __________________^
7 | |         println!("Here's a vector: {v:?}");
8 | |     });
  | |______^
help: to force the closure to take ownership of `v` (and any other referenced variables), use the `move` keyword
  |
6 |     let handle = thread::spawn(move || {
  |                                ++++
```

Rust는 `v`를 캡처하는 방법을 *추론*하고 `println!`이 `v`에 대한 참조만 필요하기 때문에 클로저는 `v`를 빌리려고 시도합니다. 그러나 문제가 있습니다: Rust는 생성된 스레드가 얼마나 오래 실행될지 알 수 없으므로 `v`에 대한 참조가 항상 유효한지 알 수 없습니다.

Listing 16-4는 `v`에 대한 참조가 유효하지 않을 가능성이 더 높은 시나리오를 제공합니다:

```rust
use std::thread;

fn main() {
    let v = vec![1, 2, 3];

    let handle = thread::spawn(|| {
        println!("Here's a vector: {v:?}");
    });

    drop(v); // 오 이런!

    handle.join().unwrap();
}
```

*Listing 16-4: `v`를 드롭하는 메인 스레드의 클로저가 `v`를 캡처하려고 시도하는 스레드*

이 코드가 실행되도록 허용되면 생성된 스레드가 전혀 실행되지 않고 즉시 백그라운드에 배치될 가능성이 있습니다. 생성된 스레드는 내부에 `v`에 대한 참조가 있지만 메인 스레드는 15장에서 논의한 `drop` 함수를 사용하여 `v`를 즉시 드롭합니다. 그런 다음 생성된 스레드가 실행을 시작하면 `v`가 더 이상 유효하지 않으므로 그에 대한 참조도 유효하지 않습니다. 오 이런!

Listing 16-3의 컴파일러 오류를 수정하려면 오류 메시지의 조언을 사용할 수 있습니다:

```text
help: to force the closure to take ownership of `v` (and any other referenced variables), use the `move` keyword
  |
6 |     let handle = thread::spawn(move || {
  |                                ++++
```

클로저 앞에 `move` 키워드를 추가하면 Rust가 값을 빌려야 한다고 추론하도록 허용하는 대신 클로저가 사용하는 값의 소유권을 가져가도록 강제합니다. Listing 16-5에 표시된 Listing 16-3의 수정 사항은 의도대로 컴파일되고 실행됩니다:

```rust
use std::thread;

fn main() {
    let v = vec![1, 2, 3];

    let handle = thread::spawn(move || {
        println!("Here's a vector: {v:?}");
    });

    handle.join().unwrap();
}
```

*Listing 16-5: `move` 키워드를 사용하여 클로저가 사용하는 값의 소유권을 가져가도록 강제*

`move` 클로저를 사용하여 메인 스레드가 `drop`을 호출하는 Listing 16-4의 코드를 수정하려고 할 수 있습니다. 그러나 이 수정은 작동하지 않습니다. Listing 16-4가 시도하는 것은 다른 이유로 허용되지 않기 때문입니다. 클로저에 `move`를 추가하면 `v`를 클로저의 환경으로 이동하고 메인 스레드에서 더 이상 `drop`을 호출할 수 없습니다. 대신 이 컴파일러 오류가 발생합니다:

```console
$ cargo run
   Compiling threads v0.1.0 (file:///projects/threads)
error[E0382]: use of moved value: `v`
  --> src/main.rs:10:10
   |
4  |     let v = vec![1, 2, 3];
   |         - move occurs because `v` has type `Vec<i32>`, which does not implement the `Copy` trait
5  |
6  |     let handle = thread::spawn(move || {
   |                                ------- value moved into closure here
7  |         println!("Here's a vector: {v:?}");
   |                                     - variable moved due to use in closure
...
10 |     drop(v); // oh no!
   |          ^ value used here after move
```

Rust의 소유권 규칙이 다시 우리를 구했습니다! Listing 16-3의 코드에서 오류가 발생한 이유는 Rust가 보수적이어서 스레드가 `v`만 빌렸기 때문입니다. 이것은 메인 스레드가 이론적으로 생성된 스레드의 참조를 무효화할 수 있다는 것을 의미했습니다. Rust에게 `v`의 소유권을 생성된 스레드로 이동하도록 지시함으로써 메인 스레드가 더 이상 `v`를 사용하지 않을 것이라고 Rust에게 보장합니다. Listing 16-4를 같은 방식으로 변경하면 메인 스레드에서 `v`를 사용하려고 할 때 소유권 규칙을 위반합니다. `move` 키워드는 Rust의 보수적인 빌림 기본값을 무시합니다; 소유권 규칙을 위반하도록 허용하지 않습니다.

스레드와 스레드 API에 대한 기본 이해를 바탕으로 스레드로 *할 수 있는* 것을 살펴보겠습니다.

---

## 메시지 전달을 사용하여 스레드 간 데이터 전송하기

> **원문:** https://doc.rust-lang.org/book/ch16-02-message-passing.html

안전한 동시성을 보장하는 점점 인기 있는 접근 방식 중 하나는 *메시지 전달*입니다. 스레드나 액터가 서로 데이터를 포함하는 메시지를 보내 통신합니다. 다음은 [Go 언어 문서](https://golang.org/doc/effective_go.html#concurrency)의 슬로건의 아이디어입니다: "메모리를 공유하여 통신하지 말고; 대신 통신하여 메모리를 공유하세요."

메시지 전송 동시성을 달성하기 위해 Rust의 표준 라이브러리는 *채널*의 구현을 제공합니다. 채널은 한 스레드에서 다른 스레드로 데이터를 보내는 일반적인 프로그래밍 개념입니다.

프로그래밍에서 채널을 물의 방향성 채널, 예를 들어 개울이나 강과 같다고 상상할 수 있습니다. 강에 고무 오리를 넣으면 수로의 끝까지 하류로 이동합니다.

채널에는 두 부분이 있습니다: 송신자와 수신자. 송신자 절반은 고무 오리를 강에 넣는 상류 위치이고, 수신자 절반은 고무 오리가 하류에 도착하는 곳입니다. 코드의 한 부분이 보내려는 데이터와 함께 송신자의 메서드를 호출하고 다른 부분이 도착하는 메시지를 위해 수신 끝을 확인합니다. 송신자 또는 수신자 절반이 드롭되면 채널이 *닫혔다*고 합니다.

여기서 한 스레드가 값을 생성하고 채널로 보내고 다른 스레드가 값을 받아 출력하는 프로그램을 작업할 것입니다. 기능을 설명하기 위해 채널을 사용하여 스레드 간에 간단한 값을 보낼 것입니다. 기술에 익숙해지면 서로 통신해야 하는 모든 스레드에 대해 채널을 사용할 수 있습니다. 예를 들어 채팅 시스템이나 많은 스레드가 계산의 일부를 수행하고 결과를 집계하는 하나의 스레드로 해당 부분을 보내는 시스템 등이 있습니다.

먼저 Listing 16-6에서 채널을 만들지만 아무것도 하지 않습니다. Rust가 채널을 통해 보내려는 값의 타입을 알 수 없기 때문에 아직 컴파일되지 않습니다.

```rust
use std::sync::mpsc;

fn main() {
    let (tx, rx) = mpsc::channel();
}
```

*Listing 16-6: 채널을 만들고 두 부분을 `tx`와 `rx`에 할당*

`mpsc::channel` 함수를 사용하여 새 채널을 만듭니다; `mpsc`는 *여러 생산자, 단일 소비자*를 의미합니다. 간단히 말해서 Rust의 표준 라이브러리가 채널을 구현하는 방식은 채널이 값을 생성하는 여러 *송신* 끝을 가질 수 있지만 해당 값을 소비하는 *수신* 끝은 하나만 가질 수 있다는 것을 의미합니다. 모두 같은 강으로 흘러가는 여러 개울이 합쳐지는 것을 상상해 보세요: 개울 중 어느 것에서든 보낸 모든 것은 결국 하나의 강에 도착합니다. 지금은 단일 생산자로 시작하지만 이 예제가 작동하면 여러 생산자를 추가할 것입니다.

`mpsc::channel` 함수는 튜플을 반환합니다. 첫 번째 요소는 송신 끝(송신자)이고 두 번째 요소는 수신 끝(수신자)입니다. 약어 `tx`와 `rx`는 많은 분야에서 전통적으로 각각 *송신자*와 *수신자*에 사용되므로 각 끝을 나타내기 위해 해당 변수 이름을 지정합니다. 튜플을 분해하는 패턴과 함께 `let` 문을 사용하고 있습니다; 18장에서 `let` 문에서 패턴 사용과 분해에 대해 논의할 것입니다. 지금은 이런 식으로 `let` 문을 사용하는 것이 `mpsc::channel`이 반환하는 튜플의 조각을 추출하는 편리한 접근 방식이라는 것만 알면 됩니다.

송신 끝을 생성된 스레드로 이동하고 메인 스레드의 수신 끝과 통신할 수 있도록 한 문자열을 보내도록 해 봅시다. Listing 16-7에 나와 있습니다. 이것은 상류에 강에 고무 오리를 넣거나 한 스레드에서 다른 스레드로 채팅 메시지를 보내는 것과 같습니다.

```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (tx, rx) = mpsc::channel();

    thread::spawn(move || {
        let val = String::from("hi");
        tx.send(val).unwrap();
    });

    let received = rx.recv().unwrap();
    println!("받음: {received}");
}
```

*Listing 16-7: `tx`를 생성된 스레드로 이동하고 "hi" 전송*

다시 `thread::spawn`을 사용하여 새 스레드를 만든 다음 `move`를 사용하여 `tx`를 클로저로 이동하여 생성된 스레드가 `tx`를 소유하도록 합니다. 생성된 스레드는 채널을 통해 메시지를 보낼 수 있으려면 송신자를 소유해야 합니다. 송신자에는 보내려는 값을 받는 `send` 메서드가 있습니다. `send` 메서드는 `Result<T, E>` 타입을 반환하므로 수신자가 이미 드롭되어 값을 보낼 곳이 없으면 전송 작업이 오류를 반환합니다. 이 예제에서는 오류 발생 시 패닉하기 위해 `unwrap`을 호출합니다. 그러나 실제 애플리케이션에서는 적절하게 처리해야 합니다: 적절한 오류 처리 전략을 검토하려면 9장으로 돌아가세요.

Listing 16-8에서는 메인 스레드의 수신자로부터 값을 가져옵니다. 이것은 강 끝의 물에서 고무 오리를 꺼내거나 채팅 메시지를 받는 것과 같습니다.

```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (tx, rx) = mpsc::channel();

    thread::spawn(move || {
        let val = String::from("hi");
        tx.send(val).unwrap();
    });

    let received = rx.recv().unwrap();
    println!("받음: {received}");
}
```

*Listing 16-8: 메인 스레드에서 "hi" 값 수신 및 출력*

수신자에는 `recv`와 `try_recv`라는 두 가지 유용한 메서드가 있습니다. 우리는 `recv`를 사용하고 있으며, 이는 *receive*의 약자로 메인 스레드의 실행을 차단하고 값이 채널로 전송될 때까지 대기합니다. 값이 전송되면 `recv`는 `Result<T, E>`로 반환합니다. 송신자가 닫히면 `recv`는 더 이상 값이 오지 않음을 알리기 위해 오류를 반환합니다.

`try_recv` 메서드는 차단하지 않고 대신 즉시 `Result<T, E>`를 반환합니다: 사용 가능한 메시지가 있으면 메시지를 보유하는 `Ok` 값, 이번에 메시지가 없으면 `Err` 값. `try_recv` 사용은 이 스레드가 메시지를 기다리는 동안 수행할 다른 작업이 있는 경우 유용합니다: `try_recv`를 주기적으로 호출하고 사용 가능한 메시지가 있으면 처리하고 그렇지 않으면 다시 확인할 때까지 잠시 다른 작업을 수행하는 루프를 작성할 수 있습니다.

이 예제에서는 단순성을 위해 `recv`를 사용했습니다; 메시지를 기다리는 것 외에 메인 스레드가 수행할 다른 작업이 없으므로 메인 스레드를 차단하는 것이 적절합니다.

Listing 16-7을 실행하면 메인 스레드에서 출력되는 값을 볼 수 있습니다:

```text
받음: hi
```

### 채널과 소유권 이전

소유권 규칙은 안전하고 동시적인 코드를 작성하는 데 도움이 되므로 메시지 전송에서 중요한 역할을 합니다. 동시 프로그래밍에서 오류를 방지하는 것은 Rust 프로그램 전체에서 소유권에 대해 생각하는 것의 이점입니다. 채널과 소유권이 함께 작동하여 문제를 방지하는 방법을 보여주는 실험을 해 봅시다: 채널로 보낸 후 생성된 스레드에서 `val` 값을 사용하려고 시도합니다. Listing 16-9의 코드를 컴파일하여 이 코드가 허용되지 않는 이유를 확인해 보세요:

```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (tx, rx) = mpsc::channel();

    thread::spawn(move || {
        let val = String::from("hi");
        tx.send(val).unwrap();
        println!("val is {val}");
    });

    let received = rx.recv().unwrap();
    println!("받음: {received}");
}
```

*Listing 16-9: 채널로 보낸 후 `val` 사용 시도*

여기서 `tx.send`를 통해 채널로 보낸 후 `val`을 출력하려고 시도합니다. 이것을 허용하는 것은 나쁜 생각입니다: 값이 다른 스레드로 전송되면 해당 스레드가 다시 사용하기 전에 값을 수정하거나 드롭할 수 있습니다. 잠재적으로 다른 스레드의 수정은 일관되지 않거나 존재하지 않는 데이터로 인해 오류나 예상치 못한 결과를 일으킬 수 있습니다. 그러나 Rust는 Listing 16-9의 코드를 컴파일하려고 하면 오류를 표시합니다:

```console
$ cargo run
   Compiling message-passing v0.1.0 (file:///projects/message-passing)
error[E0382]: borrow of moved value: `val`
  --> src/main.rs:10:35
   |
8  |         let val = String::from("hi");
   |             --- move occurs because `val` has type `String`, which does not implement the `Copy` trait
9  |         tx.send(val).unwrap();
   |                 --- value moved here
10 |         println!("val is {val}");
   |                           ^^^ value borrowed here after move
```

동시성 실수로 인해 컴파일 시간 오류가 발생했습니다. `send` 함수는 매개변수의 소유권을 가져가고 값이 이동되면 수신자가 소유권을 갖습니다. 이것은 전송 후 실수로 값을 다시 사용하는 것을 방지합니다; 소유권 시스템은 모든 것이 괜찮은지 확인합니다.

### 여러 값 보내기 및 수신자가 대기하는 것 확인하기

Listing 16-7의 코드는 컴파일되고 실행되었지만 두 개의 별도 스레드가 채널을 통해 서로 대화하고 있음을 명확하게 보여주지 않았습니다. Listing 16-10에서 Listing 16-7의 코드가 동시에 실행되고 있음을 증명할 몇 가지 수정 사항을 만들었습니다: 생성된 스레드가 이제 여러 메시지를 보내고 각 메시지 사이에 1초 동안 일시 중지합니다.

```rust
use std::sync::mpsc;
use std::thread;
use std::time::Duration;

fn main() {
    let (tx, rx) = mpsc::channel();

    thread::spawn(move || {
        let vals = vec![
            String::from("hi"),
            String::from("from"),
            String::from("the"),
            String::from("thread"),
        ];

        for val in vals {
            tx.send(val).unwrap();
            thread::sleep(Duration::from_secs(1));
        }
    });

    for received in rx {
        println!("받음: {received}");
    }
}
```

*Listing 16-10: 여러 메시지 보내기 및 각각 사이에 일시 중지*

이번에는 생성된 스레드에 메인 스레드로 보내려는 문자열 벡터가 있습니다. 이를 반복하고 각각을 개별적으로 보내고 `Duration` 값 1초와 함께 `thread::sleep` 함수를 호출하여 각각 사이에 일시 중지합니다.

메인 스레드에서 더 이상 `recv` 함수를 명시적으로 호출하지 않습니다: 대신 `rx`를 반복자로 취급합니다. 수신된 각 값을 출력합니다. 채널이 닫히면 반복이 종료됩니다.

Listing 16-10의 코드를 실행하면 각 줄 사이에 1초 일시 중지가 있는 다음 출력이 표시됩니다:

```text
받음: hi
받음: from
받음: the
받음: thread
```

메인 스레드의 `for` 루프에 일시 중지하거나 지연시키는 코드가 없으므로 메인 스레드가 생성된 스레드에서 값을 받기 위해 대기하고 있음을 알 수 있습니다.

### 송신자를 복제하여 여러 생산자 만들기

앞서 `mpsc`가 *여러 생산자, 단일 소비자*의 약자라고 언급했습니다. `mpsc`를 사용하고 Listing 16-10의 코드를 확장하여 모두 같은 수신자에게 값을 보내는 여러 스레드를 만들어 봅시다. Listing 16-11에서처럼 송신자를 복제하여 그렇게 할 수 있습니다:

```rust
use std::sync::mpsc;
use std::thread;
use std::time::Duration;

fn main() {
    let (tx, rx) = mpsc::channel();

    let tx1 = tx.clone();
    thread::spawn(move || {
        let vals = vec![
            String::from("hi"),
            String::from("from"),
            String::from("the"),
            String::from("thread"),
        ];

        for val in vals {
            tx1.send(val).unwrap();
            thread::sleep(Duration::from_secs(1));
        }
    });

    thread::spawn(move || {
        let vals = vec![
            String::from("more"),
            String::from("messages"),
            String::from("for"),
            String::from("you"),
        ];

        for val in vals {
            tx.send(val).unwrap();
            thread::sleep(Duration::from_secs(1));
        }
    });

    for received in rx {
        println!("받음: {received}");
    }
}
```

*Listing 16-11: 여러 생산자에서 여러 메시지 보내기*

이번에는 첫 번째 생성된 스레드를 만들기 전에 송신자에서 `clone`을 호출합니다. 이렇게 하면 첫 번째 생성된 스레드에 전달할 수 있는 새 송신자를 얻습니다. 원래 송신자를 두 번째 생성된 스레드에 전달합니다. 이렇게 하면 각각 다른 메시지를 하나의 수신자에게 보내는 두 개의 스레드를 얻습니다.

코드를 실행하면 출력은 다음과 같아야 합니다:

```text
받음: hi
받음: more
받음: from
받음: messages
받음: the
받음: for
받음: thread
받음: you
```

시스템에 따라 값이 다른 순서로 표시될 수 있습니다. 이것이 동시성을 흥미롭게 하면서도 어렵게 만드는 것입니다. 다른 스레드에 주어진 다른 값으로 `thread::sleep`을 실험하면 각 실행이 더 비결정적이고 매번 다른 출력을 생성합니다.

이제 채널이 어떻게 작동하는지 살펴보았으니 동시성의 다른 방법을 살펴보겠습니다.

---

## 공유 상태 동시성

> **원문:** https://doc.rust-lang.org/book/ch16-03-shared-state.html

메시지 전달은 동시성을 처리하는 좋은 방법이지만 유일한 방법은 아닙니다. 또 다른 방법은 여러 스레드가 같은 공유 데이터에 액세스하는 것입니다. Go 언어 문서의 슬로건의 이 부분을 다시 고려해 보세요: "메모리를 공유하여 통신하지 마세요."

메모리를 공유하여 통신하는 것은 어떤 모습일까요? 또한 메시지 전달 지지자들이 왜 메모리 공유를 사용하지 말라고 주의할까요?

어떤 면에서 모든 프로그래밍 언어의 채널은 단일 소유권과 유사합니다. 채널로 값을 전송하면 해당 값을 더 이상 사용해서는 안 되기 때문입니다. 공유 메모리 동시성은 다중 소유권과 같습니다: 여러 스레드가 동시에 같은 메모리 위치에 액세스할 수 있습니다. 15장에서 보았듯이 스마트 포인터가 다중 소유권을 가능하게 했으므로 다중 소유권은 복잡성을 추가할 수 있습니다. 이러한 다른 소유자가 관리되어야 하기 때문입니다. Rust의 타입 시스템과 소유권 규칙은 이 관리를 올바르게 하는 데 큰 도움이 됩니다. 예를 들어 공유 메모리에 대한 가장 일반적인 동시성 기본 요소 중 하나인 뮤텍스를 살펴보겠습니다.

### 뮤텍스를 사용하여 한 번에 하나의 스레드에서 데이터에 액세스 허용하기

*뮤텍스*는 *상호 배제(mutual exclusion)*의 약자로, 뮤텍스는 주어진 시간에 하나의 스레드만 일부 데이터에 액세스할 수 있도록 합니다. 뮤텍스의 데이터에 액세스하려면 스레드는 먼저 뮤텍스의 *잠금*을 획득하기를 요청하여 액세스를 원한다는 신호를 보내야 합니다. 잠금은 현재 누가 데이터에 대한 배타적 액세스 권한이 있는지 추적하는 뮤텍스의 일부인 데이터 구조입니다. 따라서 뮤텍스는 잠금 시스템을 통해 보유한 데이터를 *보호*하는 것으로 설명됩니다.

뮤텍스는 사용하기 어려운 것으로 유명합니다. 두 가지 규칙을 기억해야 하기 때문입니다:

* 데이터를 사용하기 전에 잠금을 획득해야 합니다.
* 뮤텍스가 보호하는 데이터 사용이 완료되면 다른 스레드가 잠금을 획득할 수 있도록 데이터 잠금을 해제해야 합니다.

뮤텍스의 실제 비유로 마이크가 하나뿐인 회의 패널 토론을 상상해 보세요. 패널리스트가 발언하기 전에 마이크 사용을 원한다고 요청하거나 신호를 보내야 합니다. 마이크를 받으면 원하는 만큼 이야기한 다음 발언을 원하는 다음 패널리스트에게 마이크를 넘깁니다. 패널리스트가 마이크 사용이 끝났을 때 마이크를 넘기는 것을 잊으면 아무도 발언할 수 없습니다. 공유 마이크 관리가 잘못되면 패널이 계획대로 작동하지 않습니다!

뮤텍스 관리는 올바르게 하기가 매우 까다로울 수 있으며 이것이 많은 사람들이 채널에 열광하는 이유입니다. 그러나 Rust의 타입 시스템과 소유권 규칙 덕분에 잠금과 잠금 해제를 잘못하는 것이 불가능합니다.

### `Mutex<T>`의 API

뮤텍스 사용 방법의 예로 Listing 16-12에 표시된 것처럼 단일 스레드 컨텍스트에서 뮤텍스를 사용하여 시작해 보겠습니다:

```rust
use std::sync::Mutex;

fn main() {
    let m = Mutex::new(5);

    {
        let mut num = m.lock().unwrap();
        *num = 6;
    }

    println!("m = {m:?}");
}
```

*Listing 16-12: 단일 스레드 컨텍스트에서 단순성을 위해 `Mutex<T>`의 API 탐색*

많은 타입과 마찬가지로 연관 함수 `new`를 사용하여 `Mutex<T>`를 만듭니다. 뮤텍스 내부의 데이터에 액세스하려면 `lock` 메서드를 사용하여 잠금을 획득합니다. 이 호출은 현재 스레드를 차단하여 잠금을 얻을 차례가 될 때까지 아무 작업도 수행할 수 없습니다.

잠금을 보유한 다른 스레드가 패닉하면 `lock` 호출이 실패합니다. 이 경우 아무도 잠금을 얻을 수 없으므로 `unwrap`을 선택했고 그 상황이 발생하면 이 스레드가 패닉하도록 합니다.

잠금을 획득한 후 이 경우 `num`이라는 반환 값을 내부의 데이터에 대한 가변 참조로 취급할 수 있습니다. 타입 시스템은 `m`의 값을 사용하기 전에 잠금을 획득하도록 합니다. `Mutex<i32>`는 `i32`가 아니므로 `i32` 값을 사용하려면 *반드시* 잠금을 획득해야 합니다. 잊을 수 없습니다; 그렇지 않으면 타입 시스템이 내부 `i32`에 액세스하도록 허용하지 않습니다.

짐작할 수 있듯이 `Mutex<T>`는 스마트 포인터입니다. 더 정확하게는 `lock` 호출은 `LockResult`로 래핑된 `MutexGuard`라는 스마트 포인터를 *반환*하며, 이것이 `unwrap` 호출로 처리했습니다. `MutexGuard` 스마트 포인터는 내부 데이터를 가리키도록 `Deref`를 구현합니다; 스마트 포인터는 또한 `MutexGuard`가 범위를 벗어날 때 잠금을 자동으로 해제하는 `Drop` 구현이 있으며, 이것은 내부 범위 끝에서 발생합니다. 결과적으로 잠금을 해제하는 것을 잊을 위험이 없습니다. 잠금 해제는 자동으로 발생하기 때문에 잠금으로 인해 다른 스레드가 뮤텍스를 사용하는 것을 차단합니다.

잠금을 드롭한 후 뮤텍스 값을 출력하고 내부 `i32`를 6으로 변경할 수 있었음을 확인할 수 있습니다.

### 여러 스레드 간에 `Mutex<T>` 공유하기

이제 `Mutex<T>`를 사용하여 여러 스레드 간에 값을 공유해 보겠습니다. 10개의 스레드를 생성하고 각각 카운터 값을 1씩 증가시켜 카운터가 0에서 10으로 증가하도록 합니다. Listing 16-13의 다음 예제는 컴파일러 오류가 발생하며 해당 오류를 사용하여 `Mutex<T>` 사용 방법과 Rust가 올바르게 사용하도록 돕는 방법에 대해 자세히 알아보겠습니다.

```rust
use std::sync::Mutex;
use std::thread;

fn main() {
    let counter = Mutex::new(0);
    let mut handles = vec![];

    for _ in 0..10 {
        let handle = thread::spawn(move || {
            let mut num = counter.lock().unwrap();

            *num += 1;
        });
        handles.push(handle);
    }

    for handle in handles {
        handle.join().unwrap();
    }

    println!("결과: {}", *counter.lock().unwrap());
}
```

*Listing 16-13: `Mutex<T>`로 보호되는 카운터를 10개 스레드 각각이 증가시키기*

Listing 16-12에서처럼 `Mutex<T>` 내부에 `i32`를 보유하는 `counter` 변수를 만듭니다. 다음으로 숫자 범위를 반복하여 10개의 스레드를 만듭니다. `thread::spawn`을 사용하고 모든 스레드에 동일한 클로저를 제공합니다: 카운터를 스레드로 이동하고 `lock` 메서드를 호출하여 `Mutex<T>`에 대한 잠금을 획득한 다음 뮤텍스의 값에 1을 더합니다. 스레드가 클로저 실행을 마치면 `num`이 범위를 벗어나고 잠금을 해제하여 다른 스레드가 획득할 수 있습니다.

메인 스레드에서 모든 조인 핸들을 수집합니다. 그런 다음 Listing 16-2에서처럼 각 핸들에서 `join`을 호출하여 모든 스레드가 완료되도록 합니다. 그 시점에서 메인 스레드가 잠금을 획득하고 이 프로그램의 결과를 출력합니다.

이 예제가 컴파일되지 않을 것이라고 암시했습니다. 이제 그 이유를 알아봅시다!

```console
$ cargo run
   Compiling shared-state v0.1.0 (file:///projects/shared-state)
error[E0382]: borrow of moved value: `counter`
  --> src/main.rs:21:29
   |
5  |     let counter = Mutex::new(0);
   |         ------- move occurs because `counter` has type `Mutex<i32>`, which does not implement the `Copy` trait
...
9  |         let handle = thread::spawn(move || {
   |                                    ------- value moved into closure here, in previous iteration of loop
...
21 |     println!("결과: {}", *counter.lock().unwrap());
   |                             ^^^^^^^ value borrowed here after move
```

오류 메시지는 `counter` 값이 루프의 이전 반복에서 이동되었음을 나타냅니다. Rust는 `counter`의 소유권을 여러 스레드로 이동할 수 없다고 말하고 있습니다.

### 여러 스레드와 다중 소유권

15장에서 스마트 포인터 `Rc<T>`를 사용하여 참조 카운팅 값을 만들어 값에 여러 소유자를 부여했습니다. 여기서 같은 작업을 수행하고 결과를 확인해 봅시다. Listing 16-14에서 `Mutex<T>`를 `Rc<T>`로 래핑하고 소유권을 스레드로 이동하기 전에 `Rc<T>`를 복제합니다.

```rust
use std::rc::Rc;
use std::sync::Mutex;
use std::thread;

fn main() {
    let counter = Rc::new(Mutex::new(0));
    let mut handles = vec![];

    for _ in 0..10 {
        let counter = Rc::clone(&counter);
        let handle = thread::spawn(move || {
            let mut num = counter.lock().unwrap();

            *num += 1;
        });
        handles.push(handle);
    }

    for handle in handles {
        handle.join().unwrap();
    }

    println!("결과: {}", *counter.lock().unwrap());
}
```

*Listing 16-14: `Rc<T>`를 사용하여 여러 스레드가 `Mutex<T>`를 소유하도록 시도*

다시 한번 컴파일하면... 다른 오류가 발생합니다! 컴파일러가 많이 가르쳐 주고 있습니다.

```console
$ cargo run
   Compiling shared-state v0.1.0 (file:///projects/shared-state)
error[E0277]: `Rc<Mutex<i32>>` cannot be sent between threads safely
   --> src/main.rs:11:36
    |
11  |           let handle = thread::spawn(move || {
    |                        ------------- ^------
    |                        |             |
    |  ______________________|_____________within this `{closure@src/main.rs:11:36: 11:43}`
    | |                      |
    | |                      required by a bound introduced by this call
12  | |             let mut num = counter.lock().unwrap();
13  | |
14  | |             *num += 1;
15  | |         });
    | |_________^ `Rc<Mutex<i32>>` cannot be sent between threads safely
```

와, 오류 메시지가 정말 장황합니다! 여기 집중할 중요한 부분이 있습니다: ``Rc<Mutex<i32>>` cannot be sent between threads safely``. 컴파일러는 그 이유도 알려줍니다: ``the trait `Send` is not implemented for `Rc<Mutex<i32>>```. 다음 섹션에서 `Send`에 대해 이야기할 것입니다: 스레드에서 사용하는 타입이 동시 상황에서 사용하도록 의도되었는지 확인하는 트레이트 중 하나입니다.

불행히도 `Rc<T>`는 스레드 간에 공유하기에 안전하지 않습니다. `Rc<T>`가 참조 카운트를 관리할 때 각 `clone` 호출에 대해 카운트에 추가하고 각 복제가 드롭될 때 카운트에서 빼기합니다. 그러나 다른 스레드가 카운트 변경을 중단시키지 못하도록 동시성 기본 요소를 사용하지는 않습니다. 이로 인해 잘못된 카운트가 발생할 수 있습니다—메모리 누수 또는 사용하기 전에 드롭되는 값으로 이어질 수 있는 미묘한 버그입니다. 필요한 것은 `Rc<T>`와 정확히 같지만 스레드 안전 방식으로 참조 카운트를 변경하는 타입입니다.

### `Arc<T>`로 원자적 참조 카운팅

다행히도 `Arc<T>`는 동시 상황에서 안전하게 사용할 수 있는 `Rc<T>` *같은* 타입입니다. *a*는 *원자적(atomic)*을 의미하며 *원자적으로 참조 카운팅*되는 타입입니다. 원자성은 여기서 자세히 다루지 않을 추가적인 종류의 동시성 기본 요소입니다: 자세한 내용은 [`std::sync::atomic`][atomic]의 표준 라이브러리 문서를 참조하세요. 이 시점에서는 원자성이 기본 타입처럼 작동하지만 스레드 간에 공유하기에 안전하다는 것만 알면 됩니다.

그러면 왜 모든 기본 타입이 원자적이지 않고 왜 표준 라이브러리 타입이 기본적으로 `Arc<T>`를 사용하도록 구현되지 않았는지 궁금할 수 있습니다. 그 이유는 스레드 안전성에는 실제로 필요하지 않을 때 지불하고 싶지 않은 성능 페널티가 따르기 때문입니다. 단일 스레드 내에서만 값에 대한 작업을 수행하는 경우 원자성이 제공하는 보장을 적용하지 않아도 되므로 코드가 더 빠르게 실행될 수 있습니다.

예제로 돌아가 봅시다: `Arc<T>`와 `Rc<T>`는 같은 API를 가지므로 `use` 줄, `new` 호출, `clone` 호출을 변경하여 프로그램을 수정합니다. Listing 16-15의 코드는 마침내 컴파일되고 실행됩니다:

```rust
use std::sync::{Arc, Mutex};
use std::thread;

fn main() {
    let counter = Arc::new(Mutex::new(0));
    let mut handles = vec![];

    for _ in 0..10 {
        let counter = Arc::clone(&counter);
        let handle = thread::spawn(move || {
            let mut num = counter.lock().unwrap();

            *num += 1;
        });
        handles.push(handle);
    }

    for handle in handles {
        handle.join().unwrap();
    }

    println!("결과: {}", *counter.lock().unwrap());
}
```

*Listing 16-15: `Arc<T>`를 사용하여 `Mutex<T>`를 래핑하여 여러 스레드 간에 소유권 공유*

이 코드는 다음을 출력합니다:

```text
결과: 10
```

해냈습니다! 0에서 10까지 카운트했는데 매우 인상적이지 않을 수 있지만 `Mutex<T>`와 스레드 안전성에 대해 많이 가르쳐 주었습니다. 이 프로그램의 구조를 사용하여 카운터를 증가시키는 것보다 더 복잡한 작업을 수행할 수도 있습니다. 이 전략을 사용하면 계산을 독립적인 부분으로 나누고 해당 부분을 스레드에 걸쳐 분할한 다음 `Mutex<T>`를 사용하여 각 스레드가 최종 결과를 자체 부분으로 업데이트하도록 할 수 있습니다.

단순한 숫자 연산을 수행하는 경우 [`std::sync::atomic` 모듈][atomic]에서 제공하는 `Mutex<T>` 타입보다 더 간단한 타입이 있습니다. 이러한 타입은 기본 타입에 대한 안전하고 동시적이며 원자적인 액세스를 제공합니다. 이 예제에서는 기본 타입에 `Mutex<T>`를 사용하기로 선택했기 때문에 `Mutex<T>`가 작동하는 방식에 집중할 수 있습니다.

### `RefCell<T>`/`Rc<T>`와 `Mutex<T>`/`Arc<T>` 사이의 유사점

`counter`가 불변이지만 그 안의 값에 대한 가변 참조를 얻을 수 있었다는 것을 눈치챘을 수 있습니다; 이것은 `Mutex<T>`가 `Cell` 패밀리처럼 내부 가변성을 제공한다는 것을 의미합니다. 15장에서 `RefCell<T>`를 사용하여 `Rc<T>` 내부의 내용을 변형할 수 있도록 했던 것과 같은 방식으로 `Mutex<T>`를 사용하여 `Arc<T>` 내부의 내용을 변형합니다.

주목할 또 다른 세부 사항은 Rust가 `Mutex<T>` 사용 시 모든 종류의 논리 오류로부터 보호할 수 없다는 것입니다. 15장에서 `Rc<T>` 사용이 참조 순환을 만들 위험이 있으며 두 `Rc<T>` 값이 서로를 참조하여 메모리 누수를 유발한다는 것을 기억하세요. 마찬가지로 `Mutex<T>`는 *교착 상태*를 만들 위험이 있습니다. 이것은 작업이 두 리소스를 잠그고 두 스레드가 각각 하나의 잠금을 획득하여 영원히 서로를 기다리게 할 때 발생합니다. 교착 상태에 관심이 있으시면 교착 상태가 있는 Rust 프로그램을 만들어 보세요; 그런 다음 모든 언어의 뮤텍스에 대한 교착 상태 완화 전략을 연구하고 Rust에서 구현해 보세요. `Mutex<T>` 및 `MutexGuard`에 대한 표준 라이브러리 API 문서는 유용한 정보를 제공합니다.

`Send`와 `Sync` 트레이트, 그리고 사용자 정의 타입과 함께 사용하는 방법에 대해 이야기하면서 이 장을 마무리하겠습니다.

[atomic]: https://doc.rust-lang.org/std/sync/atomic/index.html

---

## `Sync`와 `Send` 트레이트로 확장 가능한 동시성

> **원문:** https://doc.rust-lang.org/book/ch16-04-extensible-concurrency-sync-and-send.html

흥미롭게도 Rust 언어에는 동시성 기능이 *매우* 적습니다. 이 장에서 지금까지 이야기한 거의 모든 동시성 기능은 언어가 아닌 표준 라이브러리의 일부였습니다. 동시성 처리 옵션은 언어나 표준 라이브러리에 국한되지 않습니다; 자체 동시성 기능을 작성하거나 다른 사람이 작성한 것을 사용할 수 있습니다.

그러나 두 가지 동시성 개념이 언어에 내장되어 있습니다: `std::marker` 트레이트 `Sync`와 `Send`.

### `Send`로 스레드 간 소유권 이전 허용하기

`Send` 마커 트레이트는 `Send`를 구현하는 타입의 값의 소유권이 스레드 간에 이전될 수 있음을 나타냅니다. 거의 모든 Rust 타입이 `Send`이지만 `Rc<T>`를 포함한 일부 예외가 있습니다: `Rc<T>` 값을 복제하고 복제의 소유권을 다른 스레드로 이전하려고 하면 두 스레드가 동시에 참조 카운트를 업데이트할 수 있기 때문에 `Send`가 될 수 없습니다. 이러한 이유로 `Rc<T>`는 스레드 안전 비용을 지불하고 싶지 않은 단일 스레드 상황에서 사용하도록 구현되었습니다.

따라서 Rust의 타입 시스템과 트레이트 바운드는 `Rc<T>` 값을 실수로 스레드 간에 안전하지 않게 보내는 것을 방지합니다. Listing 16-14에서 이것을 시도했을 때 ``the trait `Send` is not implemented for `Rc<Mutex<i32>>``라는 오류가 발생했습니다. 스레드 안전한 `Arc<T>`로 전환했을 때 코드가 컴파일되었습니다.

`Send` 타입으로 완전히 구성된 모든 타입도 자동으로 `Send`로 표시됩니다. 19장에서 논의할 원시 포인터를 제외한 거의 모든 기본 타입이 `Send`입니다.

### `Sync`로 여러 스레드에서 액세스 허용하기

`Sync` 마커 트레이트는 `Sync`를 구현하는 타입이 여러 스레드에서 참조되어도 안전함을 나타냅니다. 즉, `&T`(`T`에 대한 불변 참조)가 `Send`이면 타입 `T`는 `Sync`입니다. 이것은 참조가 다른 스레드로 안전하게 전송될 수 있음을 의미합니다. `Send`와 유사하게 기본 타입은 `Sync`이고 전적으로 `Sync` 타입으로 구성된 타입도 `Sync`입니다.

스마트 포인터 `Rc<T>`도 `Send`가 아닌 것과 같은 이유로 `Sync`가 아닙니다. `RefCell<T>` 타입(15장에서 이야기했습니다)과 관련 `Cell<T>` 타입 패밀리도 `Sync`가 아닙니다. `RefCell<T>`가 런타임에 수행하는 빌림 검사 구현은 스레드 안전하지 않습니다. 스마트 포인터 `Mutex<T>`는 `Sync`이며 "여러 스레드 간에 `Mutex<T>` 공유하기" 섹션에서 본 것처럼 여러 스레드에서 액세스를 공유하는 데 사용할 수 있습니다.

### `Send`와 `Sync` 수동 구현은 안전하지 않음

`Send`와 `Sync` 타입으로 구성된 타입은 자동으로 `Send`와 `Sync`이기도 하므로 이러한 트레이트를 수동으로 구현할 필요가 없습니다. 마커 트레이트로서 구현할 메서드도 없습니다. 동시성과 관련된 불변성을 적용하는 데만 유용합니다.

이러한 트레이트를 수동으로 구현하려면 안전하지 않은 Rust 코드를 구현해야 합니다. 안전하지 않은 Rust 코드 사용에 대해서는 20장에서 이야기할 것입니다; 지금은 중요한 정보는 `Send`와 `Sync` 부분으로 구성되지 않은 새 동시 타입을 구축하려면 안전 보장을 유지하기 위해 신중한 생각이 필요하다는 것입니다. ["The Rustonomicon"](https://doc.rust-lang.org/nomicon/index.html)에는 이러한 보장과 유지 방법에 대한 자세한 정보가 있습니다.

### 요약

이것이 이 책에서 동시성을 다루는 마지막 부분은 아닙니다: 다음 장의 async와 21장의 전체 프로젝트는 이 장에서 논의한 개념을 더 현실적인 상황에서 사용합니다.

앞서 언급했듯이 Rust가 동시성을 처리하는 방식의 매우 적은 부분이 언어의 일부이기 때문에 많은 동시성 솔루션이 크레이트로 구현됩니다. 이것들은 표준 라이브러리보다 빠르게 발전하므로 다중 스레드 상황에서 사용할 현재의 최신 크레이트를 온라인에서 검색하세요.

Rust 표준 라이브러리는 메시지 전달용 채널과 동시 컨텍스트에서 사용하기 안전한 `Mutex<T>` 및 `Arc<T>`와 같은 스마트 포인터 타입을 제공합니다. 타입 시스템과 빌림 검사기는 이러한 솔루션을 사용하는 코드가 데이터 레이스나 유효하지 않은 참조로 끝나지 않도록 보장합니다. 코드가 컴파일되면 여러 스레드에서 행복하게 실행되며 다른 언어에서 흔히 발생하는 추적하기 어려운 버그 종류가 없다고 확신할 수 있습니다. 동시 프로그래밍은 더 이상 두려워할 개념이 아닙니다: 앞으로 나아가 프로그램을 겁 없이 동시적으로 만드세요!

다음으로, Rust 프로그램이 커짐에 따라 문제를 모델링하고 솔루션을 구조화하는 관용적인 방법에 대해 이야기할 것입니다. 또한 Rust의 관용구가 객체 지향 프로그래밍에서 익숙할 수 있는 것들과 어떻게 관련되는지 논의할 것입니다.

---

## Async와 Await

## 비동기 프로그래밍의 기초: Async, Await, Future, 그리고 Stream

> **원문:** https://doc.rust-lang.org/book/ch17-00-async-await.html

많은 작업이 완료되는 데 시간이 필요합니다. 비디오 내보내기와 같은 CPU 바운드 작업이나 네트워크를 통해 파일을 다운로드하는 것과 같은 I/O 바운드 작업이 있을 수 있습니다. 둘 다 완료되기를 기다리는 것이 포함됩니다.

Rust의 *비동기 프로그래밍* 모델은 이러한 종류의 작업을 처리하는 방법을 제공합니다. 이 접근 방식의 핵심 요소는 *퓨처*, Rust의 `async` 및 `await` 키워드, 그리고 이들을 함께 작동하게 하는 런타임입니다.

시작하기 전에, 이 장에서 다루지 않는 관련 항목 두 가지를 언급해야 합니다:

* 이 장에서 사용하는 `trpl` 크레이트는 기본적으로 `futures` 크레이트의 타입과 함수, 그리고 인기 있는 런타임의 유틸리티를 재내보내는 래퍼입니다. 이러한 "어댑터" 크레이트는 Rust 생태계에서 일반적이며 공개 API 결정을 표준 라이브러리와 분리하여 반복할 수 있도록 합니다.

* Rust에서 비동기 프로그래밍의 중요한 역할을 하는 `Pin` 및 `Unpin` 타입에 대한 세부 사항도 다루지 않습니다. 대부분의 일상적인 async Rust에서는 직접 다룰 필요가 없습니다.

이것을 염두에 두고, 비동기 프로그래밍이 존재하는 *이유*와 Rust의 어디에 맞는지 살펴보겠습니다.

### 병렬성 대 동시성

지금까지 병렬성과 동시성을 대부분 상호 교환적으로 취급했습니다. 이제 더 정밀하게 구분해야 합니다. 이 둘의 차이는 작업을 완료하기 위해 여러 사람이 있는 팀으로 작업할 때 나타납니다.

팀의 모든 구성원이 작업 A 또는 작업 B를 수행할 수 있고 각 구성원이 작업 중 하나를 선택하여 처음부터 끝까지 작업을 진행하면 이것이 *병렬* 작업의 예입니다. 두 작업 사이를 전환하는 대신 각 사람이 자신에게 할당된 작업에만 집중하고 처음부터 끝까지 진행하면 됩니다.

한 사람만 있거나 팀 구성원이 아직 한 작업만 할 수 있는 상태에서 두 작업 사이를 전환해야 할 때, 이것이 *동시* 작업의 예입니다. 한 작업이 막힐 때 "컨텍스트 전환"하여 다른 작업에서 진행합니다. 어느 시점에서든 실제로는 한 작업만 진행되지만, 시간이 지남에 따라 둘 다 진행됩니다.

이 두 접근 방식은 서로 배타적이지 않습니다. 종종 병렬성과 동시성을 함께 가집니다. 팀의 각 사람이 작업 세트를 받아 전환하면서 처리할 수 있습니다.

async Rust에서 퓨처는 동시성의 기본 단위입니다. 퓨처는 자체적으로 다른 퓨처로 구성될 수 있는 비동기 작업을 나타냅니다. 컴파일러가 async 코드 블록을 동시에 실행되는 다른 퓨처와 함께 인터리브될 수 있는 상태 머신으로 변환하기 때문에 복잡한 상태 전환 코드를 직접 작성할 필요가 없습니다.

이 장에서 다루는 내용:

* Rust의 `async` 및 `await` 구문 사용 방법
* 16장의 일부 동일한 도전을 해결하기 위해 async 모델 사용 방법
* 다중 스레딩과 async가 서로 보완하는 방법

---

## 퓨처와 Async 구문

> **원문:** https://doc.rust-lang.org/book/ch17-01-futures-and-syntax.html

Rust의 비동기 프로그래밍의 핵심 요소는 *퓨처*와 Rust의 `async` 및 `await` 키워드입니다.

*퓨처*는 지금은 준비되지 않았지만 미래의 어느 시점에 준비될 값입니다. Rust는 이를 빌딩 블록으로 `Future` 트레이트를 제공하므로 다른 async 작업이 다른 데이터 구조로 구현될 수 있지만 공통 인터페이스를 가집니다. Rust에서 퓨처는 `Future` 트레이트를 구현하는 타입입니다.

`async` 키워드는 블록과 함수를 중단 가능하고 재개 가능한 것으로 표시합니다. async 블록이나 async 함수 내에서 `await` 키워드를 사용하여 *await 포인트*라고 불리는 곳에서 퓨처가 준비될 때까지 기다릴 수 있습니다. async 블록이나 함수 내에서 퓨처를 await하는 각 장소는 런타임이 async 블록이나 함수를 일시 중지하고 재개할 수 있는 장소입니다.

퓨처를 준비가 되었는지 확인하는 프로세스를 *폴링*이라고 합니다.

### 첫 번째 Async 프로그램

이 장의 async 예제를 실행하려면 `trpl` 크레이트가 필요합니다. 프로젝트를 설정하세요:

```console
$ cargo new hello-async
$ cd hello-async
$ cargo add trpl
```

#### 웹 스크레이퍼 만들기

다음은 HTML 페이지에서 제목 요소를 가져오는 `page_title` 함수입니다:

```rust
use trpl::Html;

async fn page_title(url: &str) -> Option<String> {
    let response = trpl::get(url).await;
    let response_text = response.text().await;
    Html::parse(&response_text)
        .select_first("title")
        .map(|title| title.inner_html())
}
```

주요 포인트:

* 함수가 `async` 키워드로 표시됨
* `trpl::get(url).await`는 URL을 가져오고 응답을 await함
* `response.text().await`는 응답 본문 텍스트를 await함
* 두 단계 모두 비동기이며 명시적 `await`가 필요함
* Rust의 퓨처는 *지연적*: await하기 전까지 실행되지 않음

#### `await`와 메서드 체이닝

```rust
async fn page_title(url: &str) -> Option<String> {
    let response_text = trpl::get(url).await.text().await;
    Html::parse(&response_text)
        .select_first("title")
        .map(|title| title.inner_html())
}
```

후위 `await` 구문을 사용하면 우아한 메서드 체이닝이 가능합니다.

#### `async fn` 내부 작동 방식

Rust가 `async`로 표시된 함수를 볼 때 본문이 async 블록인 비async 함수로 컴파일합니다. `async fn`은 대략 다음과 같습니다:

```rust
use std::future::Future;
use trpl::Html;

fn page_title(url: &str) -> impl Future<Output = Option<String>> {
    async move {
        let text = trpl::get(url).await.text().await;
        Html::parse(&text)
            .select_first("title")
            .map(|title| title.inner_html())
    }
}
```

### 런타임으로 Async 코드 실행하기

async 코드는 *런타임*이 필요합니다. 런타임은 비동기 코드 실행의 세부 사항을 관리합니다.

#### `main`이 async가 될 수 없는 이유

```rust
// 이것은 컴파일되지 않습니다!
async fn main() {
    // ...
}
```

`main` 함수는 런타임을 *초기화*할 수 있지만 그 자체가 런타임은 아닙니다.

#### `block_on`을 사용하여 퓨처 실행하기

```rust
fn main() {
    let args: Vec<String> = std::env::args().collect();

    trpl::block_on(async {
        let url = &args[1];
        match page_title(url).await {
            Some(title) => println!("{url}의 제목은 {title}"),
            None => println!("{url}에 제목이 없습니다"),
        }
    })
}
```

`trpl::block_on()`은 퓨처를 받아 완료될 때까지 현재 스레드를 차단합니다.

### 두 URL 동시에 경주하기

```rust
use trpl::{Either, Html};

fn main() {
    let args: Vec<String> = std::env::args().collect();

    trpl::block_on(async {
        let title_fut_1 = page_title(&args[1]);
        let title_fut_2 = page_title(&args[2]);

        let (url, maybe_title) =
            match trpl::select(title_fut_1, title_fut_2).await {
                Either::Left(left) => left,
                Either::Right(right) => right,
            };

        println!("{url}이 먼저 반환됨");
        match maybe_title {
            Some(title) => println!("페이지 제목: '{title}'"),
            None => println!("제목이 없습니다"),
        }
    })
}

async fn page_title(url: &str) -> (&str, Option<String>) {
    let response_text = trpl::get(url).await.text().await;
    let title = Html::parse(&response_text)
        .select_first("title")
        .map(|title| title.inner_html());
    (url, title)
}
```

#### 핵심 구성 요소

**퓨처 생성:**
```rust
let title_fut_1 = page_title(&args[1]);
let title_fut_2 = page_title(&args[2]);
```
퓨처는 생성되지만 실행되지 않음(지연 평가).

**`trpl::select()`로 경주:**
```rust
trpl::select(title_fut_1, title_fut_2).await
```
먼저 완료되는 퓨처를 반환합니다.

**`Either` 타입:**
```rust
enum Either<A, B> {
    Left(A),
    Right(B),
}
```
`Result`와 유사하지만 성공/실패 의미 없음. `Left`는 첫 번째 인수가 먼저 완료, `Right`는 두 번째 인수가 먼저 완료를 나타냄.

### Await 포인트와 상태 머신 이해하기

각 **await 포인트**(모든 `await` 키워드)는 제어가 런타임에 반환되는 곳입니다. Rust는 보이지 않는 상태 머신을 생성하여 자동으로 상태를 관리합니다. `page_title` 함수의 경우 개념적으로 다음과 같은 상태를 생성합니다:

```rust
enum PageTitleFuture<'a> {
    Initial { url: &'a str },
    GetAwaitPoint { url: &'a str },
    TextAwaitPoint { response: trpl::Response },
}
```

**장점:**
* 컴파일러가 상태 머신을 자동으로 생성하고 관리
* 일반 빌림 및 소유권 규칙이 여전히 적용
* 컴파일러가 이러한 규칙을 검증하고 오류 메시지 제공
* 개발자가 지루한 상태 전환 코드를 수동으로 작성할 필요 없음

---

## Async로 동시성 적용하기

> **원문:** https://doc.rust-lang.org/book/ch17-02-concurrency-with-async.html

이 섹션에서는 16장에서 스레드로 해결했던 동시성 문제에 async/await 패턴을 적용하는 방법을 보여줍니다.

### `spawn_task`로 새 태스크 생성하기

`trpl` 크레이트는 `thread::spawn`과 유사한 `spawn_task`와 async `sleep` 함수를 제공합니다.

```rust
use std::time::Duration;

fn main() {
    trpl::block_on(async {
        trpl::spawn_task(async {
            for i in 1..10 {
                println!("hi number {i} from the first task!");
                trpl::sleep(Duration::from_millis(500)).await;
            }
        });

        for i in 1..5 {
            println!("hi number {i} from the second task!");
            trpl::sleep(Duration::from_millis(500)).await;
        }
    });
}
```

#### Join 핸들 사용하기

태스크 완료를 기다리려면 join 핸들에서 `await`를 사용합니다:

```rust
let handle = trpl::spawn_task(async {
    for i in 1..10 {
        println!("hi number {i} from the first task!");
        trpl::sleep(Duration::from_millis(500)).await;
    }
});

for i in 1..5 {
    println!("hi number {i} from the second task!");
    trpl::sleep(Duration::from_millis(500)).await;
}

handle.await.unwrap();
```

#### 익명 퓨처와 `trpl::join` 사용하기

태스크를 생성하는 대신 퓨처를 직접 결합합니다:

```rust
let fut1 = async {
    for i in 1..10 {
        println!("hi number {i} from the first task!");
        trpl::sleep(Duration::from_millis(500)).await;
    }
};

let fut2 = async {
    for i in 1..5 {
        println!("hi number {i} from the second task!");
        trpl::sleep(Duration::from_millis(500)).await;
    }
};

trpl::join(fut1, fut2).await;
```

**중요:** `trpl::join`은 **공정**합니다. 각 퓨처를 동등하게 번갈아 확인합니다. 이것은 일관되고 결정적인 출력을 생성합니다(OS 스케줄러가 실행 순서를 결정하는 스레드와 달리).

### 메시지 전달을 사용하여 태스크 간 데이터 전송하기

#### 기본 Async 채널

```rust
fn main() {
    trpl::block_on(async {
        let (tx, mut rx) = trpl::channel();

        let val = String::from("hi");
        tx.send(val).unwrap();

        let received = rx.recv().await.unwrap();
        println!("received '{received}'");
    });
}
```

**동기 채널과의 주요 차이점:**
- 수신자는 가변이어야 함: `mut rx`
- `recv()`는 `await`가 필요한 퓨처를 반환
- `send()`는 차단하지 않음(무제한 채널)

#### 송신과 수신 분리하기

단일 async 블록 내의 코드는 순차적으로 실행됩니다. 진정한 동시성을 위해서는 별도의 async 블록이 필요합니다:

```rust
let tx_fut = async move {
    let vals = vec![
        String::from("hi"),
        String::from("from"),
        String::from("the"),
        String::from("future"),
    ];

    for val in vals {
        tx.send(val).unwrap();
        trpl::sleep(Duration::from_secs(1)).await;
    }
};

let rx_fut = async {
    while let Some(value) = rx.recv().await {
        println!("received '{value}'");
    }
};

trpl::join(tx_fut, rx_fut).await;
```

#### `async move`로 소유권 이동하기

프로그램이 멈추는(행잉되는) 것을 방지하려면 `async move`를 사용합니다. 채널을 닫으면 프로그램이 정상적으로 종료됩니다.

```rust
let tx_fut = async move {
    // tx가 완료 후 드롭되어 채널이 닫힘
    for val in vals {
        tx.send(val).unwrap();
        trpl::sleep(Duration::from_secs(1)).await;
    }
};
```

#### `join!` 매크로로 여러 생산자

```rust
let (tx, mut rx) = trpl::channel();

let tx1 = tx.clone();
let tx1_fut = async move {
    let vals = vec![
        String::from("hi"),
        String::from("from"),
        String::from("the"),
        String::from("future"),
    ];

    for val in vals {
        tx1.send(val).unwrap();
        trpl::sleep(Duration::from_secs(1)).await;
    }
};

let rx_fut = async {
    while let Some(value) = rx.recv().await {
        println!("received '{value}'");
    }
};

let tx_fut = async move {
    let vals = vec![
        String::from("more"),
        String::from("messages"),
        String::from("for"),
        String::from("you"),
    ];

    for val in vals {
        tx.send(val).unwrap();
        trpl::sleep(Duration::from_secs(1)).await;
    }
};

trpl::join!(tx1_fut, tx_fut, rx_fut);
```

**핵심 포인트:**
- 여러 생산자를 위해 `tx`를 복제하여 `tx1` 생성
- 3개 이상의 퓨처에는 `trpl::join!` 매크로 사용(이진 `trpl::join` 대신)
- 두 송신 퓨처 모두 `tx`와 `tx1`이 드롭되도록 `async move`여야 함
- 다른 sleep 지연으로 인해 메시지가 인터리브됨

### 핵심 요점

1. **Async 블록은 내부적으로 순차적으로 실행됨** - 동시성을 위해 `trpl::join`과 함께 별도의 async 블록 사용
2. **값을 드롭해야 할 때 `async move` 사용** (예: 채널 송신자)
3. **`trpl::join`은 공정함** - 결정적인 실행 순서 제공
4. `while let Some(value) = rx.recv().await`는 알 수 없는 수의 async 메시지를 처리

---

## 여러 퓨처 다루기

> **원문:** https://doc.rust-lang.org/book/ch17-03-more-futures.html

### 런타임에 제어 양보하기

각 await 포인트에서 Rust는 런타임에 태스크를 일시 중지하고 await 중인 퓨처가 준비되지 않은 경우 다른 태스크로 전환할 기회를 줍니다. 반대로, Rust는 await 포인트에서**만** async 블록을 일시 중지합니다—await 포인트 사이의 모든 것은 동기적입니다.

async 블록에서 await 포인트 없이 광범위한 작업을 수행하면 해당 퓨처가 다른 퓨처의 진행을 차단합니다. 이를 다른 퓨처를 **굶주리게 한다**(starving)고 합니다.

#### 굶주림 문제

**Listing 17-14**는 장시간 실행되는 작업을 시뮬레이션하기 위한 `slow` 함수를 소개합니다:

```rust
extern crate trpl;

use std::{thread, time::Duration};

fn main() {
    trpl::block_on(async {
        // 나중에 여기서 `slow`를 호출할 것입니다
    });
}

fn slow(name: &str, ms: u64) {
    thread::sleep(Duration::from_millis(ms));
    println!("'{name}'이(가) {ms}ms 동안 실행됨");
}
```

**Listing 17-15**는 `trpl::select`를 사용하여 두 퓨처의 굶주림 문제를 보여줍니다:

```rust
extern crate trpl;

use std::{thread, time::Duration};

fn main() {
    trpl::block_on(async {
        let a = async {
            println!("'a' 시작됨.");
            slow("a", 30);
            slow("a", 10);
            slow("a", 20);
            trpl::sleep(Duration::from_millis(50)).await;
            println!("'a' 완료됨.");
        };

        let b = async {
            println!("'b' 시작됨.");
            slow("b", 75);
            slow("b", 10);
            slow("b", 15);
            slow("b", 350);
            trpl::sleep(Duration::from_millis(50)).await;
            println!("'b' 완료됨.");
        };

        trpl::select(a, b).await;
    });
}

fn slow(name: &str, ms: u64) {
    thread::sleep(Duration::from_millis(ms));
    println!("'{name}'이(가) {ms}ms 동안 실행됨");
}
```

**출력:**
```
'a' 시작됨.
'a'이(가) 30ms 동안 실행됨
'a'이(가) 10ms 동안 실행됨
'a'이(가) 20ms 동안 실행됨
'b' 시작됨.
'b'이(가) 75ms 동안 실행됨
'b'이(가) 10ms 동안 실행됨
'b'이(가) 15ms 동안 실행됨
'b'이(가) 350ms 동안 실행됨
'a' 완료됨.
```

두 퓨처의 `slow` 호출 사이에 인터리빙이 없습니다. `a` 퓨처가 `b`가 기회를 얻기 전에 모든 작업을 완료합니다.

#### 해결책: Await 포인트 추가하기

**Listing 17-16**은 slow 작업 사이에 `trpl::sleep`을 추가합니다:

```rust
extern crate trpl;

use std::{thread, time::Duration};

fn main() {
    trpl::block_on(async {
        let one_ms = Duration::from_millis(1);

        let a = async {
            println!("'a' 시작됨.");
            slow("a", 30);
            trpl::sleep(one_ms).await;
            slow("a", 10);
            trpl::sleep(one_ms).await;
            slow("a", 20);
            trpl::sleep(one_ms).await;
            println!("'a' 완료됨.");
        };

        let b = async {
            println!("'b' 시작됨.");
            slow("b", 75);
            trpl::sleep(one_ms).await;
            slow("b", 10);
            trpl::sleep(one_ms).await;
            slow("b", 15);
            trpl::sleep(one_ms).await;
            slow("b", 350);
            trpl::sleep(one_ms).await;
            println!("'b' 완료됨.");
        };

        trpl::select(a, b).await;
    });
}

fn slow(name: &str, ms: u64) {
    thread::sleep(Duration::from_millis(ms));
    println!("'{name}'이(가) {ms}ms 동안 실행됨");
}
```

**출력:**
```
'a' 시작됨.
'a'이(가) 30ms 동안 실행됨
'b' 시작됨.
'b'이(가) 75ms 동안 실행됨
'a'이(가) 10ms 동안 실행됨
'b'이(가) 10ms 동안 실행됨
'a'이(가) 20ms 동안 실행됨
'b'이(가) 15ms 동안 실행됨
'a' 완료됨.
```

이제 퓨처들의 작업이 인터리브됩니다.

#### `yield_now` 사용하기

**Listing 17-17**은 sleep 호출을 `trpl::yield_now`로 대체합니다:

```rust
extern crate trpl;

use std::{thread, time::Duration};

fn main() {
    trpl::block_on(async {
        let a = async {
            println!("'a' 시작됨.");
            slow("a", 30);
            trpl::yield_now().await;
            slow("a", 10);
            trpl::yield_now().await;
            slow("a", 20);
            trpl::yield_now().await;
            println!("'a' 완료됨.");
        };

        let b = async {
            println!("'b' 시작됨.");
            slow("b", 75);
            trpl::yield_now().await;
            slow("b", 10);
            trpl::yield_now().await;
            slow("b", 15);
            trpl::yield_now().await;
            slow("b", 350);
            trpl::yield_now().await;
            println!("'b' 완료됨.");
        };

        trpl::select(a, b).await;
    });
}

fn slow(name: &str, ms: u64) {
    thread::sleep(Duration::from_millis(ms));
    println!("'{name}'이(가) {ms}ms 동안 실행됨");
}
```

`yield_now()`는 의도가 더 명확하고 `sleep()`보다 빠릅니다. 타이머에는 세분성 제한이 있기 때문입니다(예: 최소 1ms 슬립).

#### 협력적 멀티태스킹

이것이 **협력적 멀티태스킹**입니다: 각 퓨처가 await 포인트를 통해 언제 제어를 넘길지 결정합니다. 각 퓨처는 너무 오래 차단하지 않도록 할 책임이 있습니다. 이 패턴은 일부 Rust 기반 임베디드 운영 체제에서 **유일한** 멀티태스킹 형태입니다.

**핵심 성능 참고:** 모든 줄에서 양보하지 마세요—양보에는 오버헤드가 있습니다. 실제 성능 병목 현상을 측정하세요.

---

### 자체 Async 추상화 구축하기

퓨처를 조합하여 새로운 패턴을 만들 수 있습니다. 예를 들어, `timeout` 함수를 구축해 보겠습니다.

**Listing 17-18**은 예상되는 사용법을 보여줍니다:

```rust
extern crate trpl;

use std::time::Duration;

fn main() {
    trpl::block_on(async {
        let slow = async {
            trpl::sleep(Duration::from_secs(5)).await;
            "드디어 완료됨"
        };

        match timeout(slow, Duration::from_secs(2)).await {
            Ok(message) => println!("'{message}'로 성공"),
            Err(duration) => {
                println!("{}초 후 실패", duration.as_secs())
            }
        }
    });
}
```

**Listing 17-19**는 함수 시그니처를 보여줍니다:

```rust
async fn timeout<F: Future>(
    future_to_try: F,
    max_time: Duration,
) -> Result<F::Output, Duration> {
    // 여기에 구현
}
```

**Listing 17-20**은 `trpl::select`를 사용하여 `timeout`을 구현합니다:

```rust
extern crate trpl;

use std::time::Duration;

use trpl::Either;

async fn timeout<F: Future>(
    future_to_try: F,
    max_time: Duration,
) -> Result<F::Output, Duration> {
    match trpl::select(future_to_try, trpl::sleep(max_time)).await {
        Either::Left(output) => Ok(output),
        Either::Right(_) => Err(max_time),
    }
}
```

**작동 방식:**
- `trpl::select`는 인수를 순서대로 폴링합니다(무작위로 공정하게가 아님)
- `future_to_try`를 먼저 전달하여 우선순위를 줍니다
- `future_to_try`가 먼저 완료되면: `Left(output)` 반환 → `Ok(output)`
- 타이머가 먼저 완료되면: `Right(())` 반환 → `Err(max_time)`

**출력:**
```
2초 후 실패
```

퓨처가 다른 퓨처와 조합되기 때문에 더 작은 async 빌딩 블록을 사용하여 강력한 도구를 구축하고 타임아웃을 재시도 및 네트워크 작업과 결합할 수 있습니다.

---

## 스트림: 순차적 퓨처

> **원문:** https://doc.rust-lang.org/book/ch17-04-streams.html

### 개요

스트림은 시간이 지남에 따라 사용 가능해지는 항목의 시퀀스를 나타내는 패턴입니다. 이터레이터의 비동기 버전이며 다음과 같은 용도로 사용할 수 있습니다:
- 큐에서 사용 가능해지는 항목
- 파일 시스템에서 점진적으로 가져오는 데이터 청크
- 네트워크를 통해 도착하는 데이터
- 과도한 네트워크 호출을 피하기 위한 이벤트 배치 처리
- 장시간 실행 작업에 타임아웃 설정
- UI 이벤트 스로틀링

### 스트림 vs 이터레이터

**주요 차이점:**
1. **시간**: 이터레이터는 동기적; 스트림은 비동기적
2. **API**: 이터레이터는 동기 `next()` 메서드 사용; 스트림은 비동기 `recv()` 또는 유사한 메서드 사용

**유사점**: 스트림은 이터레이션의 비동기 형태와 같습니다. 모든 이터레이터에서 스트림을 만들 수 있습니다.

### 기본 스트림 예제

#### 초기 시도 (컴파일되지 않는 코드)

```rust
extern crate trpl; // mdbook 테스트에 필요

fn main() {
    trpl::block_on(async {
        let values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
        let iter = values.iter().map(|n| n * 2);
        let mut stream = trpl::stream_from_iter(iter);

        while let Some(value) = stream.next().await {
            println!("값은: {value}");
        }
    });
}
```

**컴파일러 오류:**
```
error[E0599]: no method named `next` found for struct `tokio_stream::iter::Iter` in the current scope
```

### 해결책: StreamExt 트레이트 가져오기

문제는 `next()` 메서드에 접근하려면 `StreamExt` 트레이트가 스코프에 있어야 한다는 것입니다.

#### 작동하는 코드

```rust
extern crate trpl; // mdbook 테스트에 필요

use trpl::StreamExt;

fn main() {
    trpl::block_on(async {
        let values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
        let iter = values.iter().map(|n| n * 2);
        let mut stream = trpl::stream_from_iter(iter);

        while let Some(value) = stream.next().await {
            println!("값은: {value}");
        }
    });
}
```

### 스트림 트레이트

- **`Stream`**: `Iterator`와 `Future` 트레이트를 결합하는 저수준 트레이트
- **`StreamExt`**: 다음을 포함한 고수준 API를 제공하는 확장 트레이트:
  - `next()` 메서드
  - `Iterator` 트레이트 메서드와 유사한 유틸리티 메서드

**참고**: `Stream`과 `StreamExt`는 아직 Rust 표준 라이브러리의 일부가 아니지만, 대부분의 생태계 크레이트는 유사한 정의를 사용합니다.

### 스트림 조합하기

스트림에서 `filter`, `map`, `fold` 등과 같은 이터레이터 메서드와 유사한 메서드를 사용할 수 있습니다:

```rust
use trpl::StreamExt;

fn main() {
    trpl::block_on(async {
        let values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
        let iter = values.iter().map(|n| n * 2);
        let mut stream = trpl::stream_from_iter(iter);

        // 짝수만 필터링
        let mut filtered = stream.filter(|value| value % 4 == 0);

        while let Some(value) = filtered.next().await {
            println!("값은: {value}");
        }
    });
}
```

### 스트림과 타임아웃 결합하기

스트림은 타임아웃이나 다른 시간 기반 작업과 결합할 수 있습니다:

```rust
use std::time::Duration;
use trpl::StreamExt;

fn main() {
    trpl::block_on(async {
        let mut interval = trpl::interval(Duration::from_millis(500));

        let mut count = 0;
        while let Some(_) = interval.next().await {
            count += 1;
            println!("틱 {count}");

            if count >= 5 {
                break;
            }
        }
    });
}
```

### 핵심 요점

1. **스트림은 비동기 이터레이터입니다** - 시간이 지남에 따라 사용 가능해지는 값의 시퀀스를 나타냅니다
2. **`StreamExt` 트레이트가 필요합니다** - `next()` 메서드를 사용하려면 스코프에 있어야 합니다
3. **이터레이터에서 스트림 생성 가능** - `stream_from_iter`를 사용하여 동기 이터레이터를 스트림으로 변환
4. **스트림은 조합 가능합니다** - `filter`, `map`, `fold` 등의 메서드를 체이닝할 수 있습니다
5. **시간 기반 작업에 유용** - 간격, 타임아웃, 스로틀링에 적합합니다

---

## Async를 위한 트레이트 자세히 살펴보기

> **원문:** https://doc.rust-lang.org/book/ch17-05-traits-for-async.html

### 개요

이 장에서는 `Future`, `Stream`, `StreamExt` 트레이트와 `Pin` 타입 및 `Unpin` 트레이트를 심층적으로 살펴봅니다. 이러한 세부 사항은 async Rust가 내부적으로 어떻게 작동하는지 이해하는 데 필수적이지만, 일상적인 프로그래밍에는 필요하지 않습니다.

### `Future` 트레이트

#### 정의

```rust
use std::pin::Pin;
use std::task::{Context, Poll};

pub trait Future {
    type Output;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output>;
}
```

#### 핵심 구성 요소

- **`Output` 연관 타입**: `Iterator`의 `Item`과 유사하며, 퓨처가 해결되는 값을 나타냅니다
- **`poll` 메서드**: `Pin<&mut Self>`를 받아 `Poll<Self::Output>`을 반환합니다

#### `Poll` 타입

```rust
pub enum Poll<T> {
    Ready(T),
    Pending,
}
```

- `Pending`: 퓨처가 아직 수행할 작업이 있음; 호출자는 나중에 다시 확인해야 함
- `Ready(T)`: 퓨처가 완료됨; 값을 사용할 수 있음

**중요**: 퓨처가 `Ready`를 반환한 후에는 `poll`을 다시 호출하지 마세요. 많은 퓨처가 패닉을 일으킵니다.

#### `await` 작동 방식

`await`를 사용하면 Rust가 이를 `poll` 호출로 컴파일합니다. 런타임은 각 퓨처에서 `poll`을 반복적으로 호출하는 루프를 관리하여 작업이 논블로킹되도록 퓨처 간에 제어를 넘깁니다:

```rust
let mut page_title_fut = page_title(url);
loop {
    match page_title_fut.poll() {
        Ready(value) => match value {
            Some(title) => println!("{url}의 제목은 {title}"),
            None => println!("{url}에 제목이 없음"),
        }
        Pending => {
            // 런타임이 이 퓨처를 일시 중지하고 다른 것을 확인
        }
    }
}
```

### `Pin` 타입과 `Unpin` 트레이트

#### 문제: 자기 참조 퓨처

async 블록은 필드 간에 내부 참조를 포함할 수 있는 상태 머신으로 컴파일됩니다. 이러한 데이터 구조를 이동하면 내부 참조가 여전히 이전 메모리 위치를 가리켜 정의되지 않은 동작이 발생합니다.

#### `Pin` 해결책

`Pin`은 포인터와 같은 타입(`&`, `&mut`, `Box`, `Rc`)을 위한 래퍼로, 래핑된 데이터가 메모리에서 이동하지 않음을 보장합니다. 포인터만 이동할 수 있으며, 가리키는 데이터는 고정된 상태를 유지합니다.

```rust
// 데이터가 고정되어 있으며, Box 포인터가 아님
Pin<Box<SomeType>>
```

#### `Unpin` 트레이트

`Unpin`은 (`Send`와 `Sync`처럼) 마커 트레이트로, 타입이 고정되어 있어도 이동해도 안전하다는 것을 컴파일러에 알려줍니다. Rust의 대부분의 타입은 자동으로 `Unpin`을 구현합니다:

- 기본 타입(숫자, 불리언): 항상 `Unpin`
- `Vec`, `String`, 대부분의 표준 타입: 자동으로 `Unpin`
- async 블록의 대부분의 퓨처: `!Unpin` (명시적으로 구현하지 않음)

**핵심 포인트**: `Unpin`이 "일반적인" 경우이고; `!Unpin`이 고정 보장이 필요한 특별한 경우입니다.

#### 예제: Pin으로 `join_all` 수정하기

컴파일되지 않는 원본 코드:

```rust
let futures: Vec<Box<dyn Future<Output = ()>>> =
    vec![Box::new(tx1_fut), Box::new(rx_fut), Box::new(tx_fut)];

trpl::join_all(futures).await; // 오류: Unpin이 구현되지 않음
```

`pin!` 매크로로 수정:

```rust
use std::pin::{Pin, pin};

let tx1_fut = pin!(async move {
    // ... async 코드 ...
});

let rx_fut = pin!(async {
    // ... async 코드 ...
});

let tx_fut = pin!(async move {
    // ... async 코드 ...
});

let futures: Vec<Pin<&mut dyn Future<Output = ()>>> =
    vec![tx1_fut, rx_fut, tx_fut];

trpl::join_all(futures).await; // 이제 컴파일됨!
```

### `Stream` 트레이트

#### 정의

```rust
use std::pin::Pin;
use std::task::{Context, Poll};

trait Stream {
    type Item;

    fn poll_next(
        self: Pin<&mut Self>,
        cx: &mut Context<'_>
    ) -> Poll<Option<Self::Item>>;
}
```

#### Future와 Iterator를 결합하는 방법

- **`Iterator`에서**: 항목 시퀀스를 위한 `Option<Self::Item>`
- **`Future`에서**: 시간에 따른 준비 상태 확인을 위한 `Poll`
- **결과**: 시간이 지남에 따라 준비되는 항목의 시퀀스

#### `StreamExt` 트레이트

`StreamExt`는 `next` 메서드를 포함하여 스트림 작업을 위한 편리한 메서드를 제공합니다:

```rust
trait StreamExt: Stream {
    async fn next(&mut self) -> Option<Self::Item>
    where
        Self: Unpin;

    // 다른 메서드들...
}
```

주요 이점:
- 모든 `Stream` 타입에 대해 자동으로 구현됨
- 기본 구현 제공(예: `next`)
- 기본 `Stream` 트레이트를 변경하지 않고 커뮤니티 혁신 허용
- 커스텀 `Stream`을 구현할 때 `poll_next`만 구현하면 됨; `next`는 자동으로 제공됨

### 요약

- **`Future`**: `poll`을 통해 시간이 지남에 따라 준비되는 단일 값
- **`Pin`**: 데이터가 메모리에서 이동하지 않음을 보장
- **`Unpin`**: 타입이 이동해도 안전함을 나타내는 마커 트레이트
- **`Stream`**: `poll_next`를 통해 시간이 지남에 따라 준비되는 값의 시퀀스
- **`StreamExt`**: `next`와 같은 인체공학적 메서드를 제공하는 편의 트레이트

이러한 트레이트는 저수준 라이브러리나 런타임을 구축할 때 가장 중요합니다. 일상적인 Rust 코드에서는 오류 메시지에서 만날 때 이해하는 데 도움이 됩니다.

---

## 퓨처, 태스크, 스레드

> **원문:** https://doc.rust-lang.org/book/ch17-06-futures-tasks-threads.html

### 모든 것을 종합하기: 퓨처, 태스크, 스레드

#### 개요
이 장에서는 Rust에서 동시성을 위해 스레드와 async/퓨처 중 언제 사용해야 하는지 논의하며, 선택이 종종 "둘 중 하나"가 아니라 "둘 다"임을 설명합니다.

#### 스레딩 vs Async 모델

**스레딩 모델:**
- 수십 년 동안 운영 체제에서 사용되는 전통적인 접근 방식
- 스레드 *간* 동시성을 관리
- 트레이드오프: 스레드당 상당한 메모리 사용, OS 지원 필요
- "실행 후 잊기" 모델—스레드는 네이티브 퓨처 동등물 없이 완료까지 실행
- CPU 바운드, 병렬화 가능한 작업에 더 적합

**Async 모델:**
- 동시 작업이 라이브러리 수준 런타임(OS가 아님)이 관리하는 태스크에서 실행
- 태스크 *간* 및 *내*에서 동시성 가능
- 태스크가 퓨처를 관리; 런타임 실행기가 태스크를 관리
- 태스크는 런타임 이점이 있는 경량의 런타임 관리 스레드 대안
- I/O 바운드, 고도로 동시적인 작업에 더 적합

#### 동시성 단위의 계층
1. **퓨처** - 가장 세분화된 단위; 다른 퓨처의 트리를 나타낼 수 있음
2. **태스크** - 퓨처를 관리; 런타임 이점이 있는 경량 스레드와 유사
3. **스레드** - 동기 작업의 경계; OS가 관리

#### 작업 훔치기(Work Stealing)
이 장에서 사용되는 것을 포함한 많은 런타임은 기본적으로 멀티스레딩을 사용하며 최적의 성능을 위해 스레드 간에 태스크를 투명하게 이동하는 "작업 훔치기" 접근 방식을 사용합니다.

#### 결정 규칙의 경험 법칙

| 시나리오 | 더 나은 선택 |
|----------|---------------|
| 매우 병렬화 가능(CPU 바운드) | 스레드 |
| 매우 동시적(I/O 바운드) | Async |
| 병렬성과 동시성 모두 | 스레드와 async 결합 |

#### 코드 예제: 스레드와 Async 혼합

```rust
extern crate trpl; // mdbook 테스트용

use std::{thread, time::Duration};

fn main() {
    let (tx, mut rx) = trpl::channel();

    thread::spawn(move || {
        for i in 1..11 {
            tx.send(i).unwrap();
            thread::sleep(Duration::from_secs(1));
        }
    });

    trpl::block_on(async {
        while let Some(message) = rx.recv().await {
            println!("{message}");
        }
    });
}
```

**Listing 17-25**가 보여주는 것:
- async 채널 생성
- 값을 보내는 스레드(블로킹 송신자) 생성
- 메시지를 await하는 async 블록 실행
- 실제 패턴: 전용 스레드의 계산 바운드 작업과 UI로의 async 채널 알림

#### 핵심 요점

- 스레드와 태스크는 경쟁하기보다 서로 보완합니다
- 많은 실제 애플리케이션이 두 접근 방식을 결합하는 것이 유리합니다
- 예: 스레드의 비디오 인코딩(CPU 바운드)과 UI로의 async 채널 알림
- Rust는 선택한 접근 방식에 관계없이 안전하고 빠른 동시 코드를 작성할 수 있는 도구를 제공합니다

#### 다음 단계
21장에서는 이러한 개념을 더 현실적인 웹 서버 프로젝트에 적용하여 스레딩 vs 태스크/퓨처 접근 방식을 더 직접적으로 비교합니다.

### 선택 가이드라인

#### 스레드 사용이 좋은 경우:
- CPU 집약적인 계산 작업
- 작업이 독립적이고 상태를 공유하지 않는 경우
- 운영 체제 수준의 병렬성이 필요한 경우
- 블로킹 I/O를 처리해야 하는 경우

#### Async 사용이 좋은 경우:
- 많은 동시 연결을 처리하는 경우(예: 웹 서버)
- 네트워크 I/O가 많은 경우
- 메모리 효율성이 중요한 경우
- 수천 개의 동시 작업이 필요한 경우

#### 두 가지를 결합하는 경우:
- CPU 바운드 작업과 I/O 바운드 작업이 모두 있는 경우
- 블로킹 라이브러리를 async 코드와 통합해야 하는 경우
- 최대의 성능과 확장성이 필요한 경우

### 요약

퓨처, 태스크, 스레드는 Rust의 동시성 모델에서 각각 다른 역할을 합니다:

1. **퓨처**는 비동기 계산의 가장 기본적인 빌딩 블록입니다
2. **태스크**는 퓨처를 실행하고 관리하는 경량 실행 단위입니다
3. **스레드**는 OS 수준의 병렬 실행을 제공합니다

이 세 가지를 적절히 조합하면 안전하고 효율적인 동시 프로그램을 작성할 수 있습니다. Rust의 타입 시스템과 소유권 모델은 동시성 버그를 컴파일 타임에 잡아내어 "두려움 없는 동시성"을 가능하게 합니다.
