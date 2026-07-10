# 동시성 프로그래밍

# 겁 없는 동시성

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

# 스레드를 사용하여 코드를 동시에 실행하기

> **원문:** https://doc.rust-lang.org/book/ch16-01-threads.html

대부분의 현재 운영 체제에서 실행된 프로그램의 코드는 *프로세스*에서 실행되고 운영 체제는 여러 프로세스를 동시에 관리합니다. 프로그램 내에서 동시에 실행되는 독립적인 부분도 가질 수 있습니다. 이러한 독립적인 부분을 실행하는 기능을 *스레드*라고 합니다. 예를 들어 웹 서버는 여러 스레드를 가져 동시에 둘 이상의 요청에 응답할 수 있습니다.

프로그램의 계산을 여러 스레드로 분할하여 동시에 여러 작업을 실행하면 성능을 향상시킬 수 있지만 복잡성도 추가됩니다. 스레드는 동시에 실행될 수 있으므로 다른 스레드에서 코드 부분이 실행되는 순서에 대한 본질적인 보장이 없습니다. 이것은 다음과 같은 문제를 일으킬 수 있습니다:

* 스레드가 일관되지 않은 순서로 데이터나 리소스에 액세스하는 레이스 조건
* 두 스레드가 서로를 기다리며 둘 다 계속 진행하지 못하는 교착 상태
* 특정 상황에서만 발생하여 재현 및 신뢰성 있게 수정하기 어려운 버그

Rust는 스레드 사용의 부정적인 효과를 완화하려고 시도하지만 다중 스레드 컨텍스트에서 프로그래밍하는 것은 여전히 신중한 생각이 필요하고 단일 스레드에서 실행되는 프로그램과 다른 코드 구조가 필요합니다.

프로그래밍 언어는 스레드를 몇 가지 다른 방식으로 구현하며 많은 운영 체제는 새 스레드를 생성하기 위해 언어가 호출할 수 있는 API를 제공합니다. Rust 표준 라이브러리는 스레드 구현의 *1:1* 모델을 사용합니다. 이 모델에서 프로그램은 언어 스레드당 하나의 운영 체제 스레드를 사용합니다. 다른 트레이드오프를 만드는 다른 스레딩 모델을 구현하는 크레이트가 있습니다.

## `spawn`으로 새 스레드 만들기

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

## `join` 핸들을 사용하여 모든 스레드가 완료될 때까지 대기하기

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

## 스레드에서 `move` 클로저 사용하기

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

# 메시지 전달을 사용하여 스레드 간 데이터 전송하기

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

메인 스레드에서 더 이상 `recv` 함수를 명시적으로 호출하지 않습니다: 대신 `rx`를 반복자로 취급합니다. 수신된 각 값을 출력합니다. 채널이 닫히면 반복이 종료됩니다.

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

---

# 공유 상태 동시성

> **원문:** https://doc.rust-lang.org/book/ch16-03-shared-state.html

메시지 전달은 동시성을 처리하는 좋은 방법이지만 유일한 방법은 아닙니다. 또 다른 방법은 여러 스레드가 같은 공유 데이터에 액세스하는 것입니다. Go 언어 문서의 슬로건의 이 부분을 다시 고려해 보세요: "메모리를 공유하여 통신하지 마세요."

메모리를 공유하여 통신하는 것은 어떤 모습일까요? 또한 메시지 전달 지지자들이 왜 메모리 공유를 사용하지 말라고 주의할까요?

어떤 면에서 모든 프로그래밍 언어의 채널은 단일 소유권과 유사합니다. 채널로 값을 전송하면 해당 값을 더 이상 사용해서는 안 되기 때문입니다. 공유 메모리 동시성은 다중 소유권과 같습니다: 여러 스레드가 동시에 같은 메모리 위치에 액세스할 수 있습니다. 15장에서 보았듯이 스마트 포인터가 다중 소유권을 가능하게 했으므로 다중 소유권은 복잡성을 추가할 수 있습니다. 이러한 다른 소유자가 관리되어야 하기 때문입니다. Rust의 타입 시스템과 소유권 규칙은 이 관리를 올바르게 하는 데 큰 도움이 됩니다. 예를 들어 공유 메모리에 대한 가장 일반적인 동시성 기본 요소 중 하나인 뮤텍스를 살펴보겠습니다.

## 뮤텍스를 사용하여 한 번에 하나의 스레드에서 데이터에 액세스 허용하기

*뮤텍스*는 *상호 배제(mutual exclusion)*의 약자로, 뮤텍스는 주어진 시간에 하나의 스레드만 일부 데이터에 액세스할 수 있도록 합니다. 뮤텍스의 데이터에 액세스하려면 스레드는 먼저 뮤텍스의 *잠금*을 획득하기를 요청하여 액세스를 원한다는 신호를 보내야 합니다. 잠금은 현재 누가 데이터에 대한 배타적 액세스 권한이 있는지 추적하는 뮤텍스의 일부인 데이터 구조입니다. 따라서 뮤텍스는 잠금 시스템을 통해 보유한 데이터를 *보호*하는 것으로 설명됩니다.

뮤텍스는 사용하기 어려운 것으로 유명합니다. 두 가지 규칙을 기억해야 하기 때문입니다:

* 데이터를 사용하기 전에 잠금을 획득해야 합니다.
* 뮤텍스가 보호하는 데이터 사용이 완료되면 다른 스레드가 잠금을 획득할 수 있도록 데이터 잠금을 해제해야 합니다.

뮤텍스의 실제 비유로 마이크가 하나뿐인 회의 패널 토론을 상상해 보세요. 패널리스트가 발언하기 전에 마이크 사용을 원한다고 요청하거나 신호를 보내야 합니다. 마이크를 받으면 원하는 만큼 이야기한 다음 발언을 원하는 다음 패널리스트에게 마이크를 넘깁니다. 패널리스트가 마이크 사용이 끝났을 때 마이크를 넘기는 것을 잊으면 아무도 발언할 수 없습니다. 공유 마이크 관리가 잘못되면 패널이 계획대로 작동하지 않습니다!

뮤텍스 관리는 올바르게 하기가 매우 까다로울 수 있으며 이것이 많은 사람들이 채널에 열광하는 이유입니다. 그러나 Rust의 타입 시스템과 소유권 규칙 덕분에 잠금과 잠금 해제를 잘못하는 것이 불가능합니다.

## `Mutex<T>`의 API

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

## 여러 스레드 간에 `Mutex<T>` 공유하기

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

## 여러 스레드와 다중 소유권

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

## `Arc<T>`로 원자적 참조 카운팅

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

## `RefCell<T>`/`Rc<T>`와 `Mutex<T>`/`Arc<T>` 사이의 유사점

`counter`가 불변이지만 그 안의 값에 대한 가변 참조를 얻을 수 있었다는 것을 눈치챘을 수 있습니다; 이것은 `Mutex<T>`가 `Cell` 패밀리처럼 내부 가변성을 제공한다는 것을 의미합니다. 15장에서 `RefCell<T>`를 사용하여 `Rc<T>` 내부의 내용을 변형할 수 있도록 했던 것과 같은 방식으로 `Mutex<T>`를 사용하여 `Arc<T>` 내부의 내용을 변형합니다.

주목할 또 다른 세부 사항은 Rust가 `Mutex<T>` 사용 시 모든 종류의 논리 오류로부터 보호할 수 없다는 것입니다. 15장에서 `Rc<T>` 사용이 참조 순환을 만들 위험이 있으며 두 `Rc<T>` 값이 서로를 참조하여 메모리 누수를 유발한다는 것을 기억하세요. 마찬가지로 `Mutex<T>`는 *교착 상태*를 만들 위험이 있습니다. 이것은 작업이 두 리소스를 잠그고 두 스레드가 각각 하나의 잠금을 획득하여 영원히 서로를 기다리게 할 때 발생합니다. 교착 상태에 관심이 있으시면 교착 상태가 있는 Rust 프로그램을 만들어 보세요; 그런 다음 모든 언어의 뮤텍스에 대한 교착 상태 완화 전략을 연구하고 Rust에서 구현해 보세요. `Mutex<T>` 및 `MutexGuard`에 대한 표준 라이브러리 API 문서는 유용한 정보를 제공합니다.

`Send`와 `Sync` 트레이트, 그리고 사용자 정의 타입과 함께 사용하는 방법에 대해 이야기하면서 이 장을 마무리하겠습니다.

[atomic]: https://doc.rust-lang.org/std/sync/atomic/index.html

---

# `Sync`와 `Send` 트레이트로 확장 가능한 동시성

> **원문:** https://doc.rust-lang.org/book/ch16-04-extensible-concurrency-sync-and-send.html

흥미롭게도 Rust 언어에는 동시성 기능이 *매우* 적습니다. 이 장에서 지금까지 이야기한 거의 모든 동시성 기능은 언어가 아닌 표준 라이브러리의 일부였습니다. 동시성 처리 옵션은 언어나 표준 라이브러리에 국한되지 않습니다; 자체 동시성 기능을 작성하거나 다른 사람이 작성한 것을 사용할 수 있습니다.

그러나 두 가지 동시성 개념이 언어에 내장되어 있습니다: `std::marker` 트레이트 `Sync`와 `Send`.

## `Send`로 스레드 간 소유권 이전 허용하기

`Send` 마커 트레이트는 `Send`를 구현하는 타입의 값의 소유권이 스레드 간에 이전될 수 있음을 나타냅니다. 거의 모든 Rust 타입이 `Send`이지만 `Rc<T>`를 포함한 일부 예외가 있습니다: `Rc<T>` 값을 복제하고 복제의 소유권을 다른 스레드로 이전하려고 하면 두 스레드가 동시에 참조 카운트를 업데이트할 수 있기 때문에 `Send`가 될 수 없습니다. 이러한 이유로 `Rc<T>`는 스레드 안전 비용을 지불하고 싶지 않은 단일 스레드 상황에서 사용하도록 구현되었습니다.

따라서 Rust의 타입 시스템과 트레이트 바운드는 `Rc<T>` 값을 실수로 스레드 간에 안전하지 않게 보내는 것을 방지합니다. Listing 16-14에서 이것을 시도했을 때 ``the trait `Send` is not implemented for `Rc<Mutex<i32>>``라는 오류가 발생했습니다. 스레드 안전한 `Arc<T>`로 전환했을 때 코드가 컴파일되었습니다.

`Send` 타입으로 완전히 구성된 모든 타입도 자동으로 `Send`로 표시됩니다. 19장에서 논의할 원시 포인터를 제외한 거의 모든 기본 타입이 `Send`입니다.

## `Sync`로 여러 스레드에서 액세스 허용하기

`Sync` 마커 트레이트는 `Sync`를 구현하는 타입이 여러 스레드에서 참조되어도 안전함을 나타냅니다. 즉, `&T`(`T`에 대한 불변 참조)가 `Send`이면 타입 `T`는 `Sync`입니다. 이것은 참조가 다른 스레드로 안전하게 전송될 수 있음을 의미합니다. `Send`와 유사하게 기본 타입은 `Sync`이고 전적으로 `Sync` 타입으로 구성된 타입도 `Sync`입니다.

스마트 포인터 `Rc<T>`도 `Send`가 아닌 것과 같은 이유로 `Sync`가 아닙니다. `RefCell<T>` 타입(15장에서 이야기했습니다)과 관련 `Cell<T>` 타입 패밀리도 `Sync`가 아닙니다. `RefCell<T>`가 런타임에 수행하는 빌림 검사 구현은 스레드 안전하지 않습니다. 스마트 포인터 `Mutex<T>`는 `Sync`이며 "여러 스레드 간에 `Mutex<T>` 공유하기" 섹션에서 본 것처럼 여러 스레드에서 액세스를 공유하는 데 사용할 수 있습니다.

## `Send`와 `Sync` 수동 구현은 안전하지 않음

`Send`와 `Sync` 타입으로 구성된 타입은 자동으로 `Send`와 `Sync`이기도 하므로 이러한 트레이트를 수동으로 구현할 필요가 없습니다. 마커 트레이트로서 구현할 메서드도 없습니다. 동시성과 관련된 불변성을 적용하는 데만 유용합니다.

이러한 트레이트를 수동으로 구현하려면 안전하지 않은 Rust 코드를 구현해야 합니다. 안전하지 않은 Rust 코드 사용에 대해서는 20장에서 이야기할 것입니다; 지금은 중요한 정보는 `Send`와 `Sync` 부분으로 구성되지 않은 새 동시 타입을 구축하려면 안전 보장을 유지하기 위해 신중한 생각이 필요하다는 것입니다. ["The Rustonomicon"](https://doc.rust-lang.org/nomicon/index.html)에는 이러한 보장과 유지 방법에 대한 자세한 정보가 있습니다.

## 요약

이것이 이 책에서 동시성을 다루는 마지막 부분은 아닙니다: 다음 장의 async와 21장의 전체 프로젝트는 이 장에서 논의한 개념을 더 현실적인 상황에서 사용합니다.

앞서 언급했듯이 Rust가 동시성을 처리하는 방식의 매우 적은 부분이 언어의 일부이기 때문에 많은 동시성 솔루션이 크레이트로 구현됩니다. 이것들은 표준 라이브러리보다 빠르게 발전하므로 다중 스레드 상황에서 사용할 현재의 최신 크레이트를 온라인에서 검색하세요.

Rust 표준 라이브러리는 메시지 전달용 채널과 동시 컨텍스트에서 사용하기 안전한 `Mutex<T>` 및 `Arc<T>`와 같은 스마트 포인터 타입을 제공합니다. 타입 시스템과 빌림 검사기는 이러한 솔루션을 사용하는 코드가 데이터 레이스나 유효하지 않은 참조로 끝나지 않도록 보장합니다. 코드가 컴파일되면 여러 스레드에서 행복하게 실행되며 다른 언어에서 흔히 발생하는 추적하기 어려운 버그 종류가 없다고 확신할 수 있습니다. 동시 프로그래밍은 더 이상 두려워할 개념이 아닙니다: 앞으로 나아가 프로그램을 겁 없이 동시적으로 만드세요!

다음으로, Rust 프로그램이 커짐에 따라 문제를 모델링하고 솔루션을 구조화하는 관용적인 방법에 대해 이야기할 것입니다. 또한 Rust의 관용구가 객체 지향 프로그래밍에서 익숙할 수 있는 것들과 어떻게 관련되는지 논의할 것입니다.
