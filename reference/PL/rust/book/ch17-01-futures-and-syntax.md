# 퓨처와 Async 구문

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
