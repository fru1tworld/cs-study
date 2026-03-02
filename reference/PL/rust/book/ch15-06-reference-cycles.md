# 참조 순환은 메모리를 누수시킬 수 있습니다

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

`leaf`가 생성된 후 `Rc<Node>`는 강한 카운트 1과 약한 카운트 0을 가집니다. 내부 범위에서 `branch`를 만들고 `leaf`와 연결합니다. 이 시점에서 카운트를 출력하면 `branch`의 `Rc<Node>`는 강한 카운트 1과 약한 카운트 1을 가집니다(`leaf.parent`가 `Weak<Node>`로 `branch`를 가리키기 때문). `leaf`에 대한 카운트를 출력하면 강한 카운트 2를 가지는 것을 볼 수 있습니다. `branch`가 이제 `branch.children`에 저장된 `leaf`의 `Rc<Node>` 복제본을 가지고 있지만 여전히 약한 카운트 0을 가지기 때문입니다.

내부 범위가 끝나면 `branch`가 범위를 벗어나고 `Rc<Node>`의 강한 카운트가 0으로 감소하므로 `Node`가 드롭됩니다. `leaf.parent`의 약한 카운트 1은 `Node`가 드롭되는지 여부에 영향을 미치지 않으므로 메모리 누수가 없습니다!

범위 끝 이후에 `leaf`의 부모에 액세스하려고 하면 다시 `None`을 얻습니다. 프로그램 끝에서 `leaf`의 `Rc<Node>`는 `leaf` 변수가 이제 `Rc<Node>`에 대한 유일한 참조이기 때문에 강한 카운트 1과 약한 카운트 0을 가집니다.

카운트와 값 드롭을 관리하는 모든 로직은 `Rc<T>`와 `Weak<T>` 및 `Drop` 트레이트의 구현에 내장되어 있습니다. 노드 정의에서 자식에서 부모로의 관계가 `Weak<T>` 참조여야 함을 지정함으로써 메모리 누수와 참조 순환을 만들지 않고 부모 노드가 자식 노드를 가리키고 그 반대로도 할 수 있습니다.

## 요약

이 장에서는 스마트 포인터를 사용하여 Rust가 기본적으로 일반 참조로 만드는 것과 다른 보장과 트레이드오프를 만드는 방법을 다루었습니다. `Box<T>` 타입은 알려진 크기를 가지고 힙에 할당된 데이터를 가리킵니다. `Rc<T>` 타입은 힙의 데이터에 대한 참조 수를 추적하여 해당 데이터가 여러 소유자를 가질 수 있도록 합니다. `RefCell<T>` 타입은 내부 가변성을 통해 불변 타입이 필요하지만 해당 타입의 내부 값을 변경해야 할 때 사용할 수 있는 타입을 제공합니다; 또한 컴파일 시간이 아닌 런타임에 빌림 규칙을 적용합니다.

또한 스마트 포인터 타입의 많은 기능을 가능하게 하는 `Deref`와 `Drop` 트레이트에 대해서도 논의했습니다. 메모리 누수를 유발할 수 있는 참조 순환과 `Weak<T>`를 사용하여 방지하는 방법을 탐구했습니다.

이 장이 관심을 끌었고 직접 스마트 포인터를 구현하고 싶다면 ["The Rustonomicon"](https://doc.rust-lang.org/nomicon/index.html)에서 더 유용한 정보를 확인하세요.

다음으로 Rust의 동시성에 대해 이야기하겠습니다. 몇 가지 새로운 스마트 포인터에 대해서도 배우게 됩니다.
