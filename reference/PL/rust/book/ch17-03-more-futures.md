# 여러 퓨처 다루기

## 런타임에 제어 양보하기

각 await 포인트에서 Rust는 런타임에 태스크를 일시 중지하고 await 중인 퓨처가 준비되지 않은 경우 다른 태스크로 전환할 기회를 줍니다. 반대로, Rust는 await 포인트에서**만** async 블록을 일시 중지합니다—await 포인트 사이의 모든 것은 동기적입니다.

async 블록에서 await 포인트 없이 광범위한 작업을 수행하면 해당 퓨처가 다른 퓨처의 진행을 차단합니다. 이를 다른 퓨처를 **굶주리게 한다(starving)**고 합니다.

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
