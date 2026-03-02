# 스트림: 순차적 퓨처

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
