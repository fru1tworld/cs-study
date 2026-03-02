# 메시지 전달을 사용하여 스레드 간 데이터 전송하기

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

## 채널과 소유권 이전

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

## 여러 값 보내기 및 수신자가 대기하는 것 확인하기

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

메인 스레드에서 더 이상 `recv` 함수를 명시적으로 호출하지 않습니다: 대신 `rx`를 반복자로 취급합니다. 수신된 각 값에 대해 출력합니다. 채널이 닫히면 반복이 종료됩니다.

Listing 16-10의 코드를 실행하면 각 줄 사이에 1초 일시 중지가 있는 다음 출력이 표시됩니다:

```text
받음: hi
받음: from
받음: the
받음: thread
```

메인 스레드의 `for` 루프에 일시 중지하거나 지연시키는 코드가 없으므로 메인 스레드가 생성된 스레드에서 값을 받기 위해 대기하고 있음을 알 수 있습니다.

## 송신자를 복제하여 여러 생산자 만들기

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
