# Async로 동시성 적용하기

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

프로그램이 행이 걸리는 것을 방지하려면 `async move`를 사용합니다. 채널을 닫으면 프로그램이 정상적으로 종료됩니다.

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
4. **`while let Some(value) = rx.recv().await`**는 알 수 없는 수의 async 메시지를 처리
