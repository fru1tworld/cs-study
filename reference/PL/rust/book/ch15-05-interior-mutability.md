# `RefCell<T>`와 내부 가변성 패턴

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

일부 분석이 불가능하기 때문에 Rust 컴파일러가 코드가 소유권 규칙을 준수하는지 확신할 수 없으면 올바른 프로그램을 거부할 수 있습니다; 이런 방식으로 보수적입니다. Rust가 올바르지 않은 프로그램을 수락하면 사용자는 Rust가 제공하는 보장을 신뢰할 수 없습니다. 그러나 Rust가 올바른 프로그램을 거부하면 프로그래머가 불편할 것이지만 재앙적인 일은 일어나지 않습니다. `RefCell<T>` 타입은 코드가 빌림 규칙을 따르는 것을 확신하지만 컴파일러가 그것을 이해하고 보장할 수 없을 때 유용합니다.

`Rc<T>`와 유사하게 `RefCell<T>`는 단일 스레드 시나리오에서만 사용하기 위한 것이며 다중 스레드 컨텍스트에서 사용하려고 하면 컴파일 시간 오류가 발생합니다. 다중 스레드 프로그램에서 `RefCell<T>`의 기능을 얻는 방법은 16장에서 다룰 것입니다.

다음은 `Box<T>`, `Rc<T>` 또는 `RefCell<T>`를 선택하는 이유의 요약입니다:

* `Rc<T>`는 같은 데이터의 여러 소유자를 가능하게 합니다; `Box<T>`와 `RefCell<T>`는 단일 소유자를 가집니다.
* `Box<T>`는 컴파일 시간에 확인되는 불변 또는 가변 빌림을 허용합니다; `Rc<T>`는 컴파일 시간에 확인되는 불변 빌림만 허용합니다; `RefCell<T>`는 런타임에 확인되는 불변 또는 가변 빌림을 허용합니다.
* `RefCell<T>`는 런타임에 확인되는 가변 빌림을 허용하기 때문에, `RefCell<T>`가 불변이더라도 `RefCell<T>` 내부의 값을 변형할 수 있습니다.

불변 값 내에서 값을 변형하는 것은 *내부 가변성* 패턴입니다. 내부 가변성이 유용한 상황과 그것이 어떻게 가능한지 살펴보겠습니다.

## 내부 가변성: 불변 값에 대한 가변 빌림

빌림 규칙의 결과는 불변 값이 있을 때 그것을 가변으로 빌릴 수 없다는 것입니다. 예를 들어, 이 코드는 컴파일되지 않습니다:

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

`RefCell<T>`를 사용하여 불변 값을 변형하고 그것이 왜 유용한지 보여주는 실제 예제를 살펴보겠습니다.

### 내부 가변성 사용 사례: 모의 객체

때때로 테스트 중에 프로그래머는 타입을 다른 타입 대신 사용하여 특정 동작을 관찰하고 올바르게 구현되었는지 assert합니다. 이 자리 표시자 타입을 *테스트 더블*이라고 합니다. 영화 제작에서 스턴트맨이 배우를 대신하여 특히 까다로운 장면을 촬영하는 "스턴트 더블"처럼 생각하세요. 테스트 더블은 테스트를 실행할 때 다른 타입을 대신합니다. *모의 객체*는 테스트 중에 무슨 일이 발생했는지 기록하여 올바른 작업이 수행되었는지 assert할 수 있는 특정 유형의 테스트 더블입니다.

Rust는 다른 언어에서의 객체와 같은 의미로 객체를 가지고 있지 않으며, Rust는 일부 다른 언어처럼 표준 라이브러리에 내장된 모의 객체 기능이 없습니다. 그러나 모의 객체와 같은 목적을 수행하는 구조체를 확실히 만들 수 있습니다.

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

이 코드의 중요한 부분 하나는 `Messenger` 트레이트가 `self`에 대한 불변 참조와 메시지 텍스트를 받는 `send`라는 메서드 하나를 가지고 있다는 것입니다. 이 트레이트는 모의 객체가 구현해야 하는 인터페이스이므로 실제 객체처럼 같은 방식으로 사용할 수 있습니다. 중요한 다른 부분은 `LimitTracker`의 `set_value` 메서드의 동작을 테스트하고 싶다는 것입니다. `value` 매개변수로 전달하는 것을 변경할 수 있지만 `set_value`는 우리가 assertion을 만들 수 있는 아무것도 반환하지 않습니다. `Messenger` 트레이트를 구현하는 것과 `max`에 대한 특정 값을 가진 `LimitTracker`를 만들 때 `value`에 다른 숫자를 전달하면 메신저가 적절한 메시지를 보내도록 지시받았다고 말할 수 있기를 원합니다.

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

`a`, `b`, `c`를 출력하면 모두 5 대신 수정된 값 15를 가지고 있는 것을 볼 수 있습니다:

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
