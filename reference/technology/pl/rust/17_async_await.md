# Async와 Await

# 비동기 프로그래밍의 기초: Async, Await, Future, 그리고 Stream

> **원문:** https://doc.rust-lang.org/book/ch17-00-async-await.html

많은 작업이 완료되는 데 시간이 필요합니다. 비디오 내보내기와 같은 CPU 바운드 작업이나 네트워크를 통해 파일을 다운로드하는 것과 같은 I/O 바운드 작업이 있을 수 있습니다. 둘 다 완료되기를 기다리는 것이 포함됩니다.

Rust의 *비동기 프로그래밍* 모델은 이러한 종류의 작업을 처리하는 방법을 제공합니다. 이 접근 방식의 핵심 요소는 *퓨처*, Rust의 `async` 및 `await` 키워드, 그리고 이들을 함께 작동하게 하는 런타임입니다.

시작하기 전에, 이 장에서 다루지 않는 관련 항목 두 가지를 언급해야 합니다:

* 이 장에서 사용하는 `trpl` 크레이트는 기본적으로 `futures` 크레이트의 타입과 함수, 그리고 인기 있는 런타임의 유틸리티를 재내보내는 래퍼입니다. 이러한 "어댑터" 크레이트는 Rust 생태계에서 일반적이며 공개 API 결정을 표준 라이브러리와 분리하여 반복할 수 있도록 합니다.

* Rust에서 비동기 프로그래밍의 중요한 역할을 하는 `Pin` 및 `Unpin` 타입에 대한 세부 사항도 다루지 않습니다. 대부분의 일상적인 async Rust에서는 직접 다룰 필요가 없습니다.

이것을 염두에 두고, 비동기 프로그래밍이 존재하는 *이유*와 Rust의 어디에 맞는지 살펴보겠습니다.

## 병렬성 대 동시성

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

# 퓨처와 Async 구문

> **원문:** https://doc.rust-lang.org/book/ch17-01-futures-and-syntax.html

Rust의 비동기 프로그래밍의 핵심 요소는 *퓨처*와 Rust의 `async` 및 `await` 키워드입니다.

*퓨처*는 지금은 준비되지 않았지만 미래의 어느 시점에 준비될 값입니다. Rust는 이를 빌딩 블록으로 `Future` 트레이트를 제공하므로 다른 async 작업이 다른 데이터 구조로 구현될 수 있지만 공통 인터페이스를 가집니다. Rust에서 퓨처는 `Future` 트레이트를 구현하는 타입입니다.

`async` 키워드는 블록과 함수를 중단 가능하고 재개 가능한 것으로 표시합니다. async 블록이나 async 함수 내에서 `await` 키워드를 사용하여 *await 포인트*라고 불리는 곳에서 퓨처가 준비될 때까지 기다릴 수 있습니다. async 블록이나 함수 내에서 퓨처를 await하는 각 장소는 런타임이 async 블록이나 함수를 일시 중지하고 재개할 수 있는 장소입니다.

퓨처를 준비가 되었는지 확인하는 프로세스를 *폴링*이라고 합니다.

## 첫 번째 Async 프로그램

이 장의 async 예제를 실행하려면 `trpl` 크레이트가 필요합니다. 프로젝트를 설정하세요:

```console
$ cargo new hello-async
$ cd hello-async
$ cargo add trpl
```

### 웹 스크레이퍼 만들기

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

### `await`와 메서드 체이닝

```rust
async fn page_title(url: &str) -> Option<String> {
    let response_text = trpl::get(url).await.text().await;
    Html::parse(&response_text)
        .select_first("title")
        .map(|title| title.inner_html())
}
```

후위 `await` 구문을 사용하면 우아한 메서드 체이닝이 가능합니다.

### `async fn` 내부 작동 방식

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

## 런타임으로 Async 코드 실행하기

async 코드는 *런타임*이 필요합니다. 런타임은 비동기 코드 실행의 세부 사항을 관리합니다.

### `main`이 async가 될 수 없는 이유

```rust
// 이것은 컴파일되지 않습니다!
async fn main() {
    // ...
}
```

`main` 함수는 런타임을 *초기화*할 수 있지만 그 자체가 런타임은 아닙니다.

### `block_on`을 사용하여 퓨처 실행하기

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

## 두 URL 동시에 경주하기

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

### 핵심 구성 요소

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

## Await 포인트와 상태 머신 이해하기

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

# Async로 동시성 적용하기

> **원문:** https://doc.rust-lang.org/book/ch17-02-concurrency-with-async.html

이 섹션에서는 16장에서 스레드로 해결했던 동시성 문제에 async/await 패턴을 적용하는 방법을 보여줍니다.

## `spawn_task`로 새 태스크 생성하기

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

### Join 핸들 사용하기

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

### 익명 퓨처와 `trpl::join` 사용하기

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

## 메시지 전달을 사용하여 태스크 간 데이터 전송하기

### 기본 Async 채널

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

### 송신과 수신 분리하기

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

### `async move`로 소유권 이동하기

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

### `join!` 매크로로 여러 생산자

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

## 핵심 요점

1. **Async 블록은 내부적으로 순차적으로 실행됨** - 동시성을 위해 `trpl::join`과 함께 별도의 async 블록 사용
2. **값을 드롭해야 할 때 `async move` 사용** (예: 채널 송신자)
3. **`trpl::join`은 공정함** - 결정적인 실행 순서 제공
4. `while let Some(value) = rx.recv().await`는 알 수 없는 수의 async 메시지를 처리

---

# 여러 퓨처 다루기

> **원문:** https://doc.rust-lang.org/book/ch17-03-more-futures.html

## 런타임에 제어 양보하기

각 await 포인트에서 Rust는 런타임에 태스크를 일시 중지하고 await 중인 퓨처가 준비되지 않은 경우 다른 태스크로 전환할 기회를 줍니다. 반대로, Rust는 await 포인트에서**만** async 블록을 일시 중지합니다—await 포인트 사이의 모든 것은 동기적입니다.

async 블록에서 await 포인트 없이 광범위한 작업을 수행하면 해당 퓨처가 다른 퓨처의 진행을 차단합니다. 이를 다른 퓨처를 **굶주리게 한다**(starving)고 합니다.

### 굶주림 문제

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

### 해결책: Await 포인트 추가하기

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

### `yield_now` 사용하기

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

### 협력적 멀티태스킹

이것이 **협력적 멀티태스킹**입니다: 각 퓨처가 await 포인트를 통해 언제 제어를 넘길지 결정합니다. 각 퓨처는 너무 오래 차단하지 않도록 할 책임이 있습니다. 이 패턴은 일부 Rust 기반 임베디드 운영 체제에서 **유일한** 멀티태스킹 형태입니다.

**핵심 성능 참고:** 모든 줄에서 양보하지 마세요—양보에는 오버헤드가 있습니다. 실제 성능 병목 현상을 측정하세요.

---

## 자체 Async 추상화 구축하기

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

# 스트림: 순차적 퓨처

> **원문:** https://doc.rust-lang.org/book/ch17-04-streams.html

## 개요

스트림은 시간이 지남에 따라 사용 가능해지는 항목의 시퀀스를 나타내는 패턴입니다. 이터레이터의 비동기 버전이며 다음과 같은 용도로 사용할 수 있습니다:
- 큐에서 사용 가능해지는 항목
- 파일 시스템에서 점진적으로 가져오는 데이터 청크
- 네트워크를 통해 도착하는 데이터
- 과도한 네트워크 호출을 피하기 위한 이벤트 배치 처리
- 장시간 실행 작업에 타임아웃 설정
- UI 이벤트 스로틀링

## 스트림 vs 이터레이터

**주요 차이점:**
1. **시간**: 이터레이터는 동기적; 스트림은 비동기적
2. **API**: 이터레이터는 동기 `next()` 메서드 사용; 스트림은 비동기 `recv()` 또는 유사한 메서드 사용

**유사점**: 스트림은 이터레이션의 비동기 형태와 같습니다. 모든 이터레이터에서 스트림을 만들 수 있습니다.

## 기본 스트림 예제

### 초기 시도 (컴파일되지 않는 코드)

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

## 해결책: StreamExt 트레이트 가져오기

문제는 `next()` 메서드에 접근하려면 `StreamExt` 트레이트가 스코프에 있어야 한다는 것입니다.

### 작동하는 코드

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

## 스트림 트레이트

- **`Stream`**: `Iterator`와 `Future` 트레이트를 결합하는 저수준 트레이트
- **`StreamExt`**: 다음을 포함한 고수준 API를 제공하는 확장 트레이트:
  - `next()` 메서드
  - `Iterator` 트레이트 메서드와 유사한 유틸리티 메서드

**참고**: `Stream`과 `StreamExt`는 아직 Rust 표준 라이브러리의 일부가 아니지만, 대부분의 생태계 크레이트는 유사한 정의를 사용합니다.

## 스트림 조합하기

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

## 스트림과 타임아웃 결합하기

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

## 핵심 요점

1. **스트림은 비동기 이터레이터입니다** - 시간이 지남에 따라 사용 가능해지는 값의 시퀀스를 나타냅니다
2. **`StreamExt` 트레이트가 필요합니다** - `next()` 메서드를 사용하려면 스코프에 있어야 합니다
3. **이터레이터에서 스트림 생성 가능** - `stream_from_iter`를 사용하여 동기 이터레이터를 스트림으로 변환
4. **스트림은 조합 가능합니다** - `filter`, `map`, `fold` 등의 메서드를 체이닝할 수 있습니다
5. **시간 기반 작업에 유용** - 간격, 타임아웃, 스로틀링에 적합합니다

---

# Async를 위한 트레이트 자세히 살펴보기

> **원문:** https://doc.rust-lang.org/book/ch17-05-traits-for-async.html

## 개요

이 장에서는 `Future`, `Stream`, `StreamExt` 트레이트와 `Pin` 타입 및 `Unpin` 트레이트를 심층적으로 살펴봅니다. 이러한 세부 사항은 async Rust가 내부적으로 어떻게 작동하는지 이해하는 데 필수적이지만, 일상적인 프로그래밍에는 필요하지 않습니다.

## `Future` 트레이트

### 정의

```rust
use std::pin::Pin;
use std::task::{Context, Poll};

pub trait Future {
    type Output;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output>;
}
```

### 핵심 구성 요소

- **`Output` 연관 타입**: `Iterator`의 `Item`과 유사하며, 퓨처가 해결되는 값을 나타냅니다
- **`poll` 메서드**: `Pin<&mut Self>`를 받아 `Poll<Self::Output>`을 반환합니다

### `Poll` 타입

```rust
pub enum Poll<T> {
    Ready(T),
    Pending,
}
```

- `Pending`: 퓨처가 아직 수행할 작업이 있음; 호출자는 나중에 다시 확인해야 함
- `Ready(T)`: 퓨처가 완료됨; 값을 사용할 수 있음

**중요**: 퓨처가 `Ready`를 반환한 후에는 `poll`을 다시 호출하지 마세요. 많은 퓨처가 패닉을 일으킵니다.

### `await` 작동 방식

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

## `Pin` 타입과 `Unpin` 트레이트

### 문제: 자기 참조 퓨처

async 블록은 필드 간에 내부 참조를 포함할 수 있는 상태 머신으로 컴파일됩니다. 이러한 데이터 구조를 이동하면 내부 참조가 여전히 이전 메모리 위치를 가리켜 정의되지 않은 동작이 발생합니다.

### `Pin` 해결책

`Pin`은 포인터와 같은 타입(`&`, `&mut`, `Box`, `Rc`)을 위한 래퍼로, 래핑된 데이터가 메모리에서 이동하지 않음을 보장합니다. 포인터만 이동할 수 있으며, 가리키는 데이터는 고정된 상태를 유지합니다.

```rust
// 데이터가 고정되어 있으며, Box 포인터가 아님
Pin<Box<SomeType>>
```

### `Unpin` 트레이트

`Unpin`은 (`Send`와 `Sync`처럼) 마커 트레이트로, 타입이 고정되어 있어도 이동해도 안전하다는 것을 컴파일러에 알려줍니다. Rust의 대부분의 타입은 자동으로 `Unpin`을 구현합니다:

- 기본 타입(숫자, 불리언): 항상 `Unpin`
- `Vec`, `String`, 대부분의 표준 타입: 자동으로 `Unpin`
- async 블록의 대부분의 퓨처: `!Unpin` (명시적으로 구현하지 않음)

**핵심 포인트**: `Unpin`이 "일반적인" 경우이고; `!Unpin`이 고정 보장이 필요한 특별한 경우입니다.

### 예제: Pin으로 `join_all` 수정하기

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

## `Stream` 트레이트

### 정의

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

### Future와 Iterator를 결합하는 방법

- **`Iterator`에서**: 항목 시퀀스를 위한 `Option<Self::Item>`
- **`Future`에서**: 시간에 따른 준비 상태 확인을 위한 `Poll`
- **결과**: 시간이 지남에 따라 준비되는 항목의 시퀀스

### `StreamExt` 트레이트

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

## 요약

- **`Future`**: `poll`을 통해 시간이 지남에 따라 준비되는 단일 값
- **`Pin`**: 데이터가 메모리에서 이동하지 않음을 보장
- **`Unpin`**: 타입이 이동해도 안전함을 나타내는 마커 트레이트
- **`Stream`**: `poll_next`를 통해 시간이 지남에 따라 준비되는 값의 시퀀스
- **`StreamExt`**: `next`와 같은 인체공학적 메서드를 제공하는 편의 트레이트

이러한 트레이트는 저수준 라이브러리나 런타임을 구축할 때 가장 중요합니다. 일상적인 Rust 코드에서는 오류 메시지에서 만날 때 이해하는 데 도움이 됩니다.

---

# 퓨처, 태스크, 스레드

> **원문:** https://doc.rust-lang.org/book/ch17-06-futures-tasks-threads.html

## 모든 것을 종합하기: 퓨처, 태스크, 스레드

### 개요
이 장에서는 Rust에서 동시성을 위해 스레드와 async/퓨처 중 언제 사용해야 하는지 논의하며, 선택이 종종 "둘 중 하나"가 아니라 "둘 다"임을 설명합니다.

### 스레딩 vs Async 모델

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

### 동시성 단위의 계층
1. **퓨처** - 가장 세분화된 단위; 다른 퓨처의 트리를 나타낼 수 있음
2. **태스크** - 퓨처를 관리; 런타임 이점이 있는 경량 스레드와 유사
3. **스레드** - 동기 작업의 경계; OS가 관리

### 작업 훔치기(Work Stealing)
이 장에서 사용되는 것을 포함한 많은 런타임은 기본적으로 멀티스레딩을 사용하며 최적의 성능을 위해 스레드 간에 태스크를 투명하게 이동하는 "작업 훔치기" 접근 방식을 사용합니다.

### 결정 규칙의 경험 법칙

| 시나리오 | 더 나은 선택 |
|----------|---------------|
| 매우 병렬화 가능(CPU 바운드) | 스레드 |
| 매우 동시적(I/O 바운드) | Async |
| 병렬성과 동시성 모두 | 스레드와 async 결합 |

### 코드 예제: 스레드와 Async 혼합

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

### 핵심 요점

- 스레드와 태스크는 경쟁하기보다 서로 보완합니다
- 많은 실제 애플리케이션이 두 접근 방식을 결합하는 것이 유리합니다
- 예: 스레드의 비디오 인코딩(CPU 바운드)과 UI로의 async 채널 알림
- Rust는 선택한 접근 방식에 관계없이 안전하고 빠른 동시 코드를 작성할 수 있는 도구를 제공합니다

### 다음 단계
21장에서는 이러한 개념을 더 현실적인 웹 서버 프로젝트에 적용하여 스레딩 vs 태스크/퓨처 접근 방식을 더 직접적으로 비교합니다.

## 선택 가이드라인

### 스레드 사용이 좋은 경우:
- CPU 집약적인 계산 작업
- 작업이 독립적이고 상태를 공유하지 않는 경우
- 운영 체제 수준의 병렬성이 필요한 경우
- 블로킹 I/O를 처리해야 하는 경우

### Async 사용이 좋은 경우:
- 많은 동시 연결을 처리하는 경우(예: 웹 서버)
- 네트워크 I/O가 많은 경우
- 메모리 효율성이 중요한 경우
- 수천 개의 동시 작업이 필요한 경우

### 두 가지를 결합하는 경우:
- CPU 바운드 작업과 I/O 바운드 작업이 모두 있는 경우
- 블로킹 라이브러리를 async 코드와 통합해야 하는 경우
- 최대의 성능과 확장성이 필요한 경우

## 요약

퓨처, 태스크, 스레드는 Rust의 동시성 모델에서 각각 다른 역할을 합니다:

1. **퓨처**는 비동기 계산의 가장 기본적인 빌딩 블록입니다
2. **태스크**는 퓨처를 실행하고 관리하는 경량 실행 단위입니다
3. **스레드**는 OS 수준의 병렬 실행을 제공합니다

이 세 가지를 적절히 조합하면 안전하고 효율적인 동시 프로그램을 작성할 수 있습니다. Rust의 타입 시스템과 소유권 모델은 동시성 버그를 컴파일 타임에 잡아내어 "두려움 없는 동시성"을 가능하게 합니다.
