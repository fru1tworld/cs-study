# `Drop` 트레이트로 정리 시 코드 실행하기

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
