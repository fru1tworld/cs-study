# Async를 위한 트레이트 자세히 살펴보기

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
