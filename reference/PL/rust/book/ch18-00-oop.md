# 객체 지향 프로그래밍 기능

## 개요

**객체 지향 프로그래밍(OOP)**은 프로그램을 모델링하는 방식입니다. 객체라는 프로그래밍 개념은 1960년대 프로그래밍 언어 **Simula**에서 처음 도입되었습니다. Alan Kay가 객체 간 메시지를 전달하는 프로그래밍 아키텍처에 영향을 받아 1967년에 **"객체 지향 프로그래밍"**이라는 용어를 만들었습니다.

이 장에서는 일반적으로 OOP로 간주되는 특성들을 살펴보고, 이러한 특성들이 관용적인 Rust에서 어떻게 구현되는지 알아봅니다. 일부 정의에 따르면 Rust는 객체 지향이고, 다른 정의에 따르면 그렇지 않습니다. 또한 Rust에서 객체 지향 디자인 패턴을 구현하는 방법과 Rust의 고유한 강점을 활용한 대안적 솔루션과의 트레이드오프를 비교합니다.

---

# 객체 지향 언어의 특성

## 객체는 데이터와 동작을 포함한다

**Gang of Four** 디자인 패턴 책에 따르면:

> 객체 지향 프로그램은 객체들로 구성됩니다. **객체**는 데이터와 그 데이터를 조작하는 절차를 함께 패키징합니다. 이 절차들은 일반적으로 **메서드** 또는 **연산**이라고 불립니다.

**핵심 포인트:**
- Rust는 이 정의를 충족함
- 구조체(struct)와 열거형(enum)은 데이터를 가지고, `impl` 블록은 메서드를 제공
- 기술적으로 "객체"라고 부르지 않지만 동일한 기능을 제공

## 캡슐화(Encapsulation)로 구현 세부사항 숨기기

캡슐화는 객체의 구현 세부사항이 외부 코드에서 접근할 수 없음을 의미합니다. 상호작용은 공개 API를 통해서만 이루어집니다.

### AveragedCollection 예제

```rust
pub struct AveragedCollection {
    list: Vec<i32>,
    average: f64,
}

impl AveragedCollection {
    pub fn add(&mut self, value: i32) {
        self.list.push(value);
        self.update_average();
    }

    pub fn remove(&mut self) -> Option<i32> {
        let result = self.list.pop();
        match result {
            Some(value) => {
                self.update_average();
                Some(value)
            }
            None => None,
        }
    }

    pub fn average(&self) -> f64 {
        self.average
    }

    fn update_average(&mut self) {
        let total: i32 = self.list.iter().sum();
        self.average = total as f64 / self.list.len() as f64;
    }
}
```

**핵심 포인트:**
- 구조체는 `pub`이지만 필드들은 기본적으로 **비공개(private)**
- 공개 메서드(`add`, `remove`, `average`)만 외부에서 접근 가능
- 비공개 `update_average` 메서드가 일관성 유지
- 평균 필드를 외부 코드에서 직접 수정할 수 없음
- 구현 세부사항(예: `Vec<i32>`를 `HashSet<i32>`로)을 외부 코드 수정 없이 변경 가능

Rust는 `pub` 키워드 시스템을 통해 **캡슐화 요구사항을 충족**합니다.

## 상속(Inheritance)

상속은 객체가 다른 객체의 정의에서 요소(데이터와 동작)를 상속받는 메커니즘입니다.

### Rust의 입장

**Rust는 전통적인 상속을 지원하지 않습니다.** 매크로를 사용하지 않고는 부모 구조체의 필드와 메서드 구현을 상속받는 구조체를 정의할 방법이 없습니다.

### 상속을 사용하는 두 가지 이유

**1. 코드 재사용:**
- Rust는 **트레이트 메서드 구현**을 대신 사용
- 트레이트에 기본 구현을 제공할 수 있음 (예: `Summary` 트레이트의 `summarize` 메서드)
- 자식 클래스의 메서드 오버라이딩처럼 구현을 재정의할 수 있음

**2. 타입 시스템 / 다형성(Polymorphism):**
- 부모 타입이 예상되는 곳에서 자식 타입을 사용할 수 있게 함
- Rust는 상속 대신 **트레이트 객체**를 통해 이를 달성

### 다형성

다형성은 여러 타입의 데이터와 작동할 수 있는 코드를 의미합니다.

Rust의 접근 방식:
- **제네릭(Generics)**으로 다양한 가능한 타입을 추상화
- **트레이트 바운드(Trait bounds)**로 타입이 제공해야 하는 것에 제약을 부과
- 이를 **제한된 파라메트릭 다형성(bounded parametric polymorphism)**이라고 함

### Rust가 상속을 피하는 이유

- 불필요한 코드 공유: 하위 클래스가 필요하지 않은 부모 특성까지 모두 상속
- 유연성 감소: 적용되지 않는 메서드를 상속받을 수 있음
- 단일 상속 제한: 일부 언어는 하나의 클래스에서만 상속 가능
- 런타임 오류: 적용 불가능한 상속된 메서드 호출 시 문제 발생

Rust는 런타임 다형성을 달성하기 위해 상속 대신 **트레이트 객체**를 사용합니다.

---

# 트레이트 객체를 사용하여 다양한 타입의 값 허용하기

## 문제 정의

Rust의 벡터는 단일 타입의 요소만 저장할 수 있습니다. 열거형이 하나의 해결책이지만, 라이브러리가 사용자에게 유효한 타입 집합을 확장할 수 있도록 허용해야 할 때가 있습니다. 예를 들어 GUI 라이브러리가 컴파일 시점에 모든 컴포넌트 타입을 알지 못해도 다양한 컴포넌트를 그릴 수 있어야 합니다.

## 공통 동작을 위한 트레이트 정의하기

### Draw 트레이트

```rust
pub trait Draw {
    fn draw(&self);
}
```

### 트레이트 객체란?

**트레이트 객체**는 다음 두 가지를 가리킵니다:
1. 지정된 트레이트를 구현하는 타입의 인스턴스
2. 런타임에 해당 타입의 트레이트 메서드를 조회하기 위한 테이블

포인터(참조 또는 `Box<T>`) 뒤에 `dyn` 키워드와 트레이트 이름을 붙여 생성합니다.

### 트레이트 객체를 사용하는 Screen 구조체

```rust
pub trait Draw {
    fn draw(&self);
}

pub struct Screen {
    pub components: Vec<Box<dyn Draw>>,
}

impl Screen {
    pub fn run(&self) {
        for component in self.components.iter() {
            component.draw();
        }
    }
}
```

## 트레이트 객체 vs 제네릭 비교

### 제네릭 접근 방식 (동종 컬렉션)

```rust
pub trait Draw {
    fn draw(&self);
}

pub struct Screen<T: Draw> {
    pub components: Vec<T>,
}

impl<T> Screen<T>
where
    T: Draw,
{
    pub fn run(&self) {
        for component in self.components.iter() {
            component.draw();
        }
    }
}
```

**제한사항:** 모든 컴포넌트가 같은 타입이어야 합니다. `Screen` 구조체는 모든 `Button` 또는 모든 `TextField`의 목록을 가집니다. 컴파일러가 단형화(monomorphization)를 수행하여 각 타입에 대한 특정 코드를 생성합니다.

### 트레이트 객체 접근 방식 (이종 컬렉션)

트레이트 객체를 사용하면 단일 `Screen` 인스턴스가 `Box<Button>`과 `Box<TextField>`를 함께 보유할 수 있습니다.

## 트레이트 구현하기

### Button 구현

```rust
pub struct Button {
    pub width: u32,
    pub height: u32,
    pub label: String,
}

impl Draw for Button {
    fn draw(&self) {
        // 버튼을 실제로 그리는 코드
    }
}
```

### SelectBox 구현 (사용자 코드)

```rust
use gui::Draw;

struct SelectBox {
    width: u32,
    height: u32,
    options: Vec<String>,
}

impl Draw for SelectBox {
    fn draw(&self) {
        // 선택 상자를 실제로 그리는 코드
    }
}
```

## 실제 사용 예제

```rust
use gui::{Button, Screen};

fn main() {
    let screen = Screen {
        components: vec![
            Box::new(SelectBox {
                width: 75,
                height: 10,
                options: vec![
                    String::from("Yes"),
                    String::from("Maybe"),
                    String::from("No"),
                ],
            }),
            Box::new(Button {
                width: 50,
                height: 10,
                label: String::from("OK"),
            }),
        ],
    };

    screen.run();
}
```

## 컴파일 시점 타입 안전성

트레이트를 구현하지 않는 타입을 사용하면 컴파일 오류가 발생합니다:

```rust
use gui::Screen;

fn main() {
    let screen = Screen {
        components: vec![Box::new(String::from("Hi"))],
    };

    screen.run();
}
```

**오류:**
```
error[E0277]: the trait bound `String: Draw` is not satisfied
 --> src/main.rs:5:26
  |
5 |         components: vec![Box::new(String::from("Hi"))],
  |                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ the trait `Draw` is not implemented for `String`
```

## 덕 타이핑(Duck Typing) 개념

트레이트 객체는 동적 타입 언어의 **덕 타이핑** 개념을 구현합니다: "오리처럼 걷고 오리처럼 꽥꽥거리면, 오리임에 틀림없다!" `Screen`은 구체적인 타입을 확인하지 않고, 컴포넌트가 `draw` 메서드에 응답하는지만 신경 씁니다.

## 동적 디스패치(Dynamic Dispatch)

### 정적 vs 동적 디스패치

- **정적 디스패치:** 컴파일러가 컴파일 시점에 어떤 메서드를 호출할지 알고 있음 (단형화를 통한 제네릭에서 사용)
- **동적 디스패치:** 컴파일러가 런타임에 어떤 메서드를 호출할지 결정하는 코드를 생성 (트레이트 객체에서 사용)

### 런타임 비용

트레이트 객체를 사용할 때, Rust는 트레이트 객체 내부의 포인터를 사용하여 런타임에 어떤 메서드를 호출할지 결정합니다. 이로 인해:
- 런타임 조회 비용 발생
- 메서드 인라이닝 및 관련 최적화 방지

### 트레이드오프 비교

| 측면 | 제네릭 | 트레이트 객체 |
|------|--------|--------------|
| 타입 동질성 | 동종 컬렉션 | 이종 컬렉션 |
| 디스패치 | 정적 (컴파일 시점) | 동적 (런타임) |
| 성능 | 더 빠름 (단형화) | 더 느림 (런타임 조회) |
| 유연성 | 덜 유연함 | 더 유연함 |
| 컴파일러 최적화 | 더 나은 인라이닝 가능 | 제한된 최적화 |

**핵심 요약:**
- **트레이트 객체**는 동일한 트레이트를 구현하는 여러 다른 타입을 단일 컬렉션에 저장 가능
- **타입 안전성**은 컴파일 시점에 강제됨
- **덕 타이핑 의미론**으로 유연하고 확장 가능한 설계 가능
- **동적 디스패치**는 런타임 비용을 추가하지만 유연성 제공
- 동종 컬렉션에는 **제네릭**, 이종 컬렉션에는 **트레이트 객체** 선호

---

# 객체 지향 디자인 패턴 구현하기

이 섹션에서는 **상태 패턴(State Pattern)**을 Rust에서 구현하는 방법을 보여줍니다. 상태 패턴은 값이 내부적으로 가질 수 있는 상태 집합을 정의하고, 상태 객체로 표현하며, 값의 동작이 상태에 따라 변경됩니다.

## 상태 패턴 개념

패턴의 핵심:
- 값이 내부적으로 가질 수 있는 상태 집합을 정의
- 상태는 상태 객체로 표현됨
- 값의 동작이 상태에 따라 변경됨
- Rust에서는 객체와 상속 대신 구조체와 트레이트를 사용

**주요 장점:** 비즈니스 요구사항이 변경되면 코드 전체가 아닌 하나의 상태 객체 내부 코드만 업데이트하면 됩니다.

## 블로그 포스트 워크플로우 예제

최종 기능:
1. 블로그 포스트는 빈 초안(draft)으로 시작
2. 초안이 완료되면 포스트의 검토(review)를 요청
3. 포스트가 승인(approve)되면 게시(publish)됨
4. 게시된 블로그 포스트만 출력할 콘텐츠를 반환
5. 다른 변경은 효과 없음 (예: 검토 전에 초안을 승인해도 아무 일도 일어나지 않음)

---

## 파트 1: 전통적인 객체 지향 스타일

### 원하는 API 사용법

```rust
use blog::Post;

fn main() {
    let mut post = Post::new();

    post.add_text("I ate a salad for lunch today");
    assert_eq!("", post.content());

    post.request_review();
    assert_eq!("", post.content());

    post.approve();
    assert_eq!("I ate a salad for lunch today", post.content());
}
```

### Post 정의 및 새 인스턴스 생성

```rust
pub struct Post {
    state: Option<Box<dyn State>>,
    content: String,
}

impl Post {
    pub fn new() -> Post {
        Post {
            state: Some(Box::new(Draft {})),
            content: String::new(),
        }
    }
}

trait State {}

struct Draft {}

impl State for Draft {}
```

**핵심 포인트:**
- `Post`는 `Option<T>` 안에 트레이트 객체 `Box<dyn State>`를 보유
- 비공개 `state` 필드가 유효하지 않은 상태의 포스트 생성 방지
- 새 포스트는 `Draft`로 시작

### 포스트 콘텐츠의 텍스트 저장

```rust
impl Post {
    pub fn add_text(&mut self, text: &str) {
        self.content.push_str(text);
    }
}
```

`add_text`는 상태에 의존하지 않으므로 상태 패턴의 일부가 아닙니다.

### 검토 요청 - 포스트 상태 변경

```rust
pub struct Post {
    state: Option<Box<dyn State>>,
    content: String,
}

impl Post {
    pub fn new() -> Post {
        Post {
            state: Some(Box::new(Draft {})),
            content: String::new(),
        }
    }

    pub fn add_text(&mut self, text: &str) {
        self.content.push_str(text);
    }

    pub fn content(&self) -> &str {
        ""
    }

    pub fn request_review(&mut self) {
        if let Some(s) = self.state.take() {
            self.state = Some(s.request_review())
        }
    }
}

trait State {
    fn request_review(self: Box<Self>) -> Box<dyn State>;
}

struct Draft {}

impl State for Draft {
    fn request_review(self: Box<Self>) -> Box<dyn State> {
        Box::new(PendingReview {})
    }
}

struct PendingReview {}

impl State for PendingReview {
    fn request_review(self: Box<Self>) -> Box<dyn State> {
        self
    }
}
```

**핵심 개념:**
- `request_review` 메서드 시그니처: `self: Box<Self>`가 박스된 상태의 소유권을 가져옴
- `take()`가 `Option`에서 `Some` 값을 이동시키고, 그 자리에 `None`을 남김
- `Draft`는 `PendingReview`로 변환됨
- `PendingReview`는 다시 검토 요청해도 `PendingReview` 상태 유지

### approve() 추가로 content 동작 변경

```rust
pub struct Post {
    state: Option<Box<dyn State>>,
    content: String,
}

impl Post {
    pub fn new() -> Post {
        Post {
            state: Some(Box::new(Draft {})),
            content: String::new(),
        }
    }

    pub fn add_text(&mut self, text: &str) {
        self.content.push_str(text);
    }

    pub fn content(&self) -> &str {
        self.state.as_ref().unwrap().content(self)
    }

    pub fn request_review(&mut self) {
        if let Some(s) = self.state.take() {
            self.state = Some(s.request_review())
        }
    }

    pub fn approve(&mut self) {
        if let Some(s) = self.state.take() {
            self.state = Some(s.approve())
        }
    }
}

trait State {
    fn request_review(self: Box<Self>) -> Box<dyn State>;
    fn approve(self: Box<Self>) -> Box<dyn State>;

    fn content<'a>(&self, post: &'a Post) -> &'a str {
        ""
    }
}

struct Draft {}

impl State for Draft {
    fn request_review(self: Box<Self>) -> Box<dyn State> {
        Box::new(PendingReview {})
    }

    fn approve(self: Box<Self>) -> Box<dyn State> {
        self
    }
}

struct PendingReview {}

impl State for PendingReview {
    fn request_review(self: Box<Self>) -> Box<dyn State> {
        self
    }

    fn approve(self: Box<Self>) -> Box<dyn State> {
        Box::new(Published {})
    }
}

struct Published {}

impl State for Published {
    fn request_review(self: Box<Self>) -> Box<dyn State> {
        self
    }

    fn approve(self: Box<Self>) -> Box<dyn State> {
        self
    }

    fn content<'a>(&self, post: &'a Post) -> &'a str {
        &post.content
    }
}
```

**핵심 포인트:**
- `Post::content()`가 상태 객체의 `content()`를 호출
- `as_ref()`가 `Option` 내부 값의 참조를 가져옴
- `unwrap()`은 메서드들이 항상 `state`가 `Some`임을 보장하므로 안전
- 역참조 강제(Deref coercion)로 `&Box<dyn State>`에서 트레이트 메서드 호출 가능
- 트레이트의 기본 `content()`는 빈 문자열 반환; `Published`만 오버라이드
- 라이프타임 어노테이션이 반환된 참조를 `post` 인자의 라이프타임과 연결

### 상태 패턴 평가

**장점:**
- 규칙이 상태 객체에 캡슐화됨
- `Post` 메서드에 `match` 표현식 불필요
- 새 상태 추가 시 새 구조체만 추가하면 됨
- 새 기능으로 쉽게 확장 가능

**단점:**
- 상태들이 서로 결합됨 (각 상태에서 전환 정의)
- `request_review`와 `approve` 메서드의 중복 로직
- 기존 상태 사이에 새 상태 추가 시 여러 구조체 변경 필요

---

## 파트 2: 상태와 동작을 타입으로 인코딩하기

상태를 런타임 값으로 캡슐화하는 대신, 타입 시스템 자체에 인코딩합니다.

### 타입 기반 상태 구현

```rust
pub struct Post {
    content: String,
}

pub struct DraftPost {
    content: String,
}

impl Post {
    pub fn new() -> DraftPost {
        DraftPost {
            content: String::new(),
        }
    }

    pub fn content(&self) -> &str {
        &self.content
    }
}

impl DraftPost {
    pub fn add_text(&mut self, text: &str) {
        self.content.push_str(text);
    }
}
```

**핵심 포인트:**
- `Post::new()`가 `Post`가 아닌 `DraftPost`를 반환
- 초안 포스트에는 `content()` 메서드가 없음 - 컴파일러가 게시되지 않은 콘텐츠 접근 방지
- 비공개 `content` 필드와 생성자 없음으로 `Post` 직접 생성 불가
- 유효하지 않은 연산이 런타임 검사가 아닌 컴파일 시점 오류

### 검토 및 승인 상태 추가

```rust
pub struct Post {
    content: String,
}

pub struct DraftPost {
    content: String,
}

impl Post {
    pub fn new() -> DraftPost {
        DraftPost {
            content: String::new(),
        }
    }

    pub fn content(&self) -> &str {
        &self.content
    }
}

impl DraftPost {
    pub fn add_text(&mut self, text: &str) {
        self.content.push_str(text);
    }

    pub fn request_review(self) -> PendingReviewPost {
        PendingReviewPost {
            content: self.content,
        }
    }
}

pub struct PendingReviewPost {
    content: String,
}

impl PendingReviewPost {
    pub fn approve(self) -> Post {
        Post {
            content: self.content,
        }
    }
}
```

**핵심 개념:**
- `request_review()`와 `approve()`가 `self`를 소비 (소유권 가져감)
- 변환이 한 타입을 다른 타입으로 변경: `DraftPost` -> `PendingReviewPost` -> `Post`
- 소비된 타입의 남아있는 인스턴스 없음
- `PendingReviewPost`도 `content()` 메서드 없음
- `Post`(게시됨)만 `content()` 메서드 있음

### 업데이트된 사용법

```rust
use blog::Post;

fn main() {
    let mut post = Post::new();

    post.add_text("I ate a salad for lunch today");

    let post = post.request_review();

    let post = post.approve();

    assert_eq!("I ate a salad for lunch today", post.content());
}
```

**변경 사항:**
- 상태 전환 후 `post`를 재할당해야 함
- 초안이나 검토 포스트의 콘텐츠를 확인할 수 없음 - 컴파일러가 방지
- 게시되지 않은 상태에 대한 assertion 없음

### 타입 기반 접근 방식의 트레이드오프

**장점:**
- 유효하지 않은 상태가 불가능 - 컴파일 시점 안전성
- 게시되지 않은 콘텐츠를 실수로 표시할 수 없음
- 타입 시스템이 유효한 상태 전환을 강제
- 프로덕션 전에 특정 버그 발견

**단점:**
- 전통적인 객체 지향 상태 패턴에서 벗어남
- 더 장황한 타입 시그니처
- 상태 전환을 통해 변수 이름 재할당 필요

---

## 요약

이 장에서는 Rust에서 상태 패턴에 대한 두 가지 접근 방식을 제시합니다:

1. **전통적인 OO 스타일** (트레이트 객체): 트레이트 객체를 사용하여 런타임에 상태와 전환을 캡슐화하고, 상태에 따라 동적으로 동작을 디스패치합니다.

2. **타입 기반 스타일** (타입으로 상태 인코딩): Rust의 타입 시스템을 활용하여 컴파일 시점에 유효하지 않은 상태를 불가능하게 만듭니다.

**핵심 결론:** Rust는 객체 지향 패턴을 구현할 수 있지만, 소유권과 타입 시스템 같은 기능을 통해 다른 트레이드오프를 가진 대안적 패턴도 가능합니다. 객체 지향 패턴이 항상 Rust의 강점을 최대한 활용하는 최선의 솔루션은 아니지만, 사용 가능한 옵션입니다.
