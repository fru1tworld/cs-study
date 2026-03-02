# 스레드를 사용하여 코드를 동시에 실행하기

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

Rust의 소유권 규칙이 다시 우리를 구했습니다! Listing 16-3의 코드에서 오류가 발생한 이유는 Rust가 보수적이어서 스레드에 대해 `v`만 빌렸기 때문입니다. 이것은 메인 스레드가 이론적으로 생성된 스레드의 참조를 무효화할 수 있다는 것을 의미했습니다. Rust에게 `v`의 소유권을 생성된 스레드로 이동하도록 지시함으로써 메인 스레드가 더 이상 `v`를 사용하지 않을 것이라고 Rust에게 보장합니다. Listing 16-4를 같은 방식으로 변경하면 메인 스레드에서 `v`를 사용하려고 할 때 소유권 규칙을 위반합니다. `move` 키워드는 Rust의 보수적인 빌림 기본값을 무시합니다; 소유권 규칙을 위반하도록 허용하지 않습니다.

스레드와 스레드 API에 대한 기본 이해를 바탕으로 스레드로 *할 수 있는* 것을 살펴보겠습니다.
