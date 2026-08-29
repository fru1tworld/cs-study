# 러스트 객체 지향, 패턴과 매칭, unsafe

## 객체 지향 프로그래밍 기능

> **원문:** https://doc.rust-lang.org/book/ch18-00-oop.html

### 개요

- **객체 지향 프로그래밍**(OOP)은 프로그램을 모델링하는 방식
- 객체라는 프로그래밍 개념은 1960년대 프로그래밍 언어 **Simula**에서 처음 도입됨
- Alan Kay가 객체 간 메시지를 전달하는 프로그래밍 아키텍처에 영향을 받아 1967년에 "객체 지향 프로그래밍"이라는 용어를 만듦
- 이 장에서 다루는 내용
  - 일반적으로 OOP로 간주되는 특성들 → 관용적인 Rust에서의 구현 방식
  - 정의에 따라 Rust가 객체 지향으로 간주될 수도, 아닐 수도 있음
  - Rust에서 객체 지향 디자인 패턴을 구현하는 방법과 Rust의 고유한 강점을 활용한 대안적 솔루션의 트레이드오프 비교

---

## 객체 지향 언어의 특성

### 객체는 데이터와 동작을 포함한다

**Gang of Four** 디자인 패턴 책에 따르면:

> 객체 지향 프로그램은 객체들로 구성됩니다. **객체**는 데이터와 그 데이터를 조작하는 절차를 함께 패키징합니다. 이 절차들은 일반적으로 **메서드** 또는 **연산**이라고 불립니다.

**핵심 포인트:**
- Rust는 이 정의를 충족함
- 구조체(struct)와 열거형(enum)은 데이터를 가지고, `impl` 블록은 메서드를 제공
- 기술적으로 "객체"라고 부르지 않지만 동일한 기능을 제공

### 캡슐화(Encapsulation)로 구현 세부사항 숨기기

- 캡슐화: 객체의 구현 세부사항이 외부 코드에서 접근할 수 없음을 의미
- 상호작용은 공개 API를 통해서만 이루어짐

#### AveragedCollection 예제

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

- Rust는 `pub` 키워드 시스템을 통해 캡슐화 요구사항을 충족함

### 상속(Inheritance)

- 상속: 객체가 다른 객체의 정의에서 요소(데이터와 동작)를 상속받는 메커니즘

#### Rust의 입장

- **Rust는 전통적인 상속을 지원하지 않음**
- 매크로를 사용하지 않고는 부모 구조체의 필드와 메서드 구현을 상속받는 구조체를 정의할 방법 없음

#### 상속을 사용하는 두 가지 이유

**1. 코드 재사용:**
- Rust는 **트레이트 메서드 구현**을 대신 사용
- 트레이트에 기본 구현을 제공할 수 있음 (예: `Summary` 트레이트의 `summarize` 메서드)
- 자식 클래스의 메서드 오버라이딩처럼 구현을 재정의할 수 있음

**2. 타입 시스템 / 다형성(Polymorphism):**
- 부모 타입이 예상되는 곳에서 자식 타입을 사용할 수 있게 함
- Rust는 상속 대신 **트레이트 객체**를 통해 이를 달성

#### 다형성

- 다형성: 여러 타입의 데이터와 작동할 수 있는 코드를 의미

Rust의 접근 방식:
- **제네릭**(Generics)으로 다양한 가능한 타입을 추상화
- **트레이트 바운드**(Trait bounds)로 타입이 제공해야 하는 것에 제약을 부과
- 이를 **제한된 파라메트릭 다형성**(bounded parametric polymorphism)이라고 함

#### Rust가 상속을 피하는 이유

- 불필요한 코드 공유: 하위 클래스가 필요하지 않은 부모 특성까지 모두 상속
- 유연성 감소: 적용되지 않는 메서드를 상속받을 수 있음
- 단일 상속 제한: 일부 언어는 하나의 클래스에서만 상속 가능
- 런타임 오류: 적용 불가능한 상속된 메서드 호출 시 문제 발생

- Rust는 런타임 다형성을 달성하기 위해 상속 대신 **트레이트 객체**를 사용함

---

## 트레이트 객체를 사용하여 다양한 타입의 값 허용하기

### 문제 정의

- Rust의 벡터는 단일 타입의 요소만 저장 가능
- 열거형이 하나의 해결책이지만, 라이브러리가 사용자에게 유효한 타입 집합을 확장할 수 있도록 허용해야 할 때가 있음
- 예: GUI 라이브러리가 컴파일 시점에 모든 컴포넌트 타입을 알지 못해도 다양한 컴포넌트를 그릴 수 있어야 함

### 공통 동작을 위한 트레이트 정의하기

#### Draw 트레이트

```rust
pub trait Draw {
    fn draw(&self);
}
```

#### 트레이트 객체란?

**트레이트 객체**는 다음 두 가지를 가리킴:
1. 지정된 트레이트를 구현하는 타입의 인스턴스
2. 런타임에 해당 타입의 트레이트 메서드를 조회하기 위한 테이블

- 생성 방법: 포인터(참조 또는 `Box<T>`) 뒤에 `dyn` 키워드와 트레이트 이름을 붙임

#### 트레이트 객체를 사용하는 Screen 구조체

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

### 트레이트 객체 vs 제네릭 비교

#### 제네릭 접근 방식 (동종 컬렉션)

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

- 제한사항
  - 모든 컴포넌트가 같은 타입이어야 함
  - `Screen` 구조체는 모든 `Button` 또는 모든 `TextField`의 목록만 가짐
  - 컴파일러가 단형화(monomorphization)를 수행하여 각 타입에 대한 특정 코드 생성

#### 트레이트 객체 접근 방식 (이종 컬렉션)

- 트레이트 객체를 사용하면 단일 `Screen` 인스턴스가 `Box<Button>`과 `Box<TextField>`를 함께 보유 가능

### 트레이트 구현하기

#### Button 구현

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

#### SelectBox 구현 (사용자 코드)

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

### 실제 사용 예제

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

### 컴파일 시점 타입 안전성

- 트레이트를 구현하지 않는 타입 사용 → 컴파일 오류 발생

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

### 덕 타이핑(Duck Typing) 개념

- 트레이트 객체는 동적 타입 언어의 **덕 타이핑** 개념을 구현: "오리처럼 걷고 오리처럼 꽥꽥거리면, 오리임에 틀림없다"
- `Screen`은 구체적인 타입을 확인하지 않고, 컴포넌트가 `draw` 메서드에 응답하는지만 확인

### 동적 디스패치(Dynamic Dispatch)

#### 정적 vs 동적 디스패치

- **정적 디스패치:** 컴파일러가 컴파일 시점에 어떤 메서드를 호출할지 알고 있음 (단형화를 통한 제네릭에서 사용)
- **동적 디스패치:** 컴파일러가 런타임에 어떤 메서드를 호출할지 결정하는 코드를 생성 (트레이트 객체에서 사용)

#### 런타임 비용

- 트레이트 객체를 사용할 때 → Rust는 트레이트 객체 내부의 포인터를 사용하여 런타임에 어떤 메서드를 호출할지 결정 → 이로 인해:
- 런타임 조회 비용 발생
- 메서드 인라이닝 및 관련 최적화 방지

#### 트레이드오프 비교

- 타입 동질성
  - 제네릭: 동종 컬렉션
  - 트레이트 객체: 이종 컬렉션
- 디스패치
  - 제네릭: 정적(컴파일 시점)
  - 트레이트 객체: 동적(런타임)
- 성능
  - 제네릭: 더 빠름(단형화)
  - 트레이트 객체: 더 느림(런타임 조회)
- 유연성
  - 제네릭: 덜 유연함
  - 트레이트 객체: 더 유연함
- 컴파일러 최적화
  - 제네릭: 더 나은 인라이닝 가능
  - 트레이트 객체: 제한된 최적화

**핵심 요약:**
- **트레이트 객체**는 동일한 트레이트를 구현하는 여러 다른 타입을 단일 컬렉션에 저장 가능
- **타입 안전성**은 컴파일 시점에 강제됨
- **덕 타이핑 의미론**으로 유연하고 확장 가능한 설계 가능
- **동적 디스패치**는 런타임 비용을 추가하지만 유연성 제공
- 동종 컬렉션에는 **제네릭**, 이종 컬렉션에는 **트레이트 객체** 선호

---

## 객체 지향 디자인 패턴 구현하기

- 이 섹션에서는 **상태 패턴**(State Pattern)을 Rust에서 구현하는 방법을 다룸
- 상태 패턴: 값이 내부적으로 가질 수 있는 상태 집합을 정의하고 → 상태 객체로 표현 → 값의 동작이 상태에 따라 변경됨

### 상태 패턴 개념

패턴의 핵심:
- 값이 내부적으로 가질 수 있는 상태 집합을 정의
- 상태는 상태 객체로 표현됨
- 값의 동작이 상태에 따라 변경됨
- Rust에서는 객체와 상속 대신 구조체와 트레이트를 사용

- 주요 장점: 비즈니스 요구사항이 변경되면 코드 전체가 아닌 하나의 상태 객체 내부 코드만 업데이트하면 됨

### 블로그 포스트 워크플로우 예제

- 최종 기능
  1. 블로그 포스트는 빈 초안(draft)으로 시작
  2. 초안이 완료되면 포스트의 검토(review)를 요청
  3. 포스트가 승인(approve)되면 게시(publish)됨
  4. 게시된 블로그 포스트만 출력할 콘텐츠를 반환
  5. 다른 변경은 효과 없음(예: 검토 전에 초안을 승인해도 아무 일도 일어나지 않음)

---

### 파트 1: 전통적인 객체 지향 스타일

#### 원하는 API 사용법

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

#### Post 정의 및 새 인스턴스 생성

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

#### 포스트 콘텐츠의 텍스트 저장

```rust
impl Post {
    pub fn add_text(&mut self, text: &str) {
        self.content.push_str(text);
    }
}
```

- `add_text`는 상태에 의존하지 않으므로 상태 패턴의 일부가 아님

#### 검토 요청 - 포스트 상태 변경

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

#### approve() 추가로 content 동작 변경

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

#### 상태 패턴 평가

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

### 파트 2: 상태와 동작을 타입으로 인코딩하기

- 상태를 런타임 값으로 캡슐화하는 대신, 타입 시스템 자체에 인코딩

#### 타입 기반 상태 구현

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

#### 검토 및 승인 상태 추가

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

#### 업데이트된 사용법

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

#### 타입 기반 접근 방식의 트레이드오프

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

### 요약

이 장에서 다루는 Rust의 상태 패턴 접근 방식 두 가지:

1. **전통적인 OO 스타일**(트레이트 객체): 트레이트 객체를 사용하여 런타임에 상태와 전환을 캡슐화 → 상태에 따라 동적으로 동작을 디스패치
2. **타입 기반 스타일**(타입으로 상태 인코딩): Rust의 타입 시스템을 활용하여 컴파일 시점에 유효하지 않은 상태를 불가능하게 만듦

- 핵심 결론
  - Rust는 객체 지향 패턴을 구현할 수 있지만, 소유권과 타입 시스템 같은 기능을 통해 다른 트레이드오프를 가진 대안적 패턴도 가능
  - 객체 지향 패턴이 항상 Rust의 강점을 최대한 활용하는 최선의 솔루션은 아니지만, 사용 가능한 옵션 중 하나

---

## 패턴과 매칭

## 패턴과 매칭

> **원문:** https://doc.rust-lang.org/book/ch19-00-patterns.html

### 개요

- 패턴(Pattern): Rust에서 복잡하거나 단순한 타입의 구조와 매칭하기 위한 특별한 문법
- `match` 표현식 및 다른 구조와 함께 패턴을 사용 → 프로그램의 제어 흐름을 더 세밀하게 제어 가능

### 패턴의 구성 요소

패턴은 다음 요소들의 조합으로 구성됨:

- **리터럴(Literals)**: 구체적인 값
- **구조 분해된 배열, 열거형, 구조체, 튜플**: 복합 타입의 분해
- **변수(Variables)**: 값을 바인딩할 이름
- **와일드카드(Wildcards)**: 모든 값과 매칭
- **플레이스홀더(Placeholders)**: 특정 위치의 값을 무시

### 패턴 예시

패턴의 예시:

- `x` - 단순 변수 패턴
- `(a, 3)` - 튜플 패턴 (변수와 리터럴의 조합)
- `Some(Color::Red)` - 열거형 패턴

**핵심 포인트:**
- 패턴이 유효한 컨텍스트에서 이러한 구성 요소들은 데이터의 형태를 설명함
- 프로그램은 값을 패턴과 비교하여 특정 코드를 실행하기에 적합한 형태인지 판단함

### 패턴의 작동 방식

- 패턴 사용법: 어떤 값과 비교 → 매칭되면 코드에서 값의 각 부분을 사용
- 6장의 `match` 표현식(동전 분류기 예제) 참고
  - 값이 패턴의 형태에 맞으면 이름이 지정된 부분을 사용할 수 있음
  - 값이 패턴과 맞지 않으면 해당 패턴과 연관된 코드는 실행되지 않음

```rust
// match 표현식의 기본 구조 예시
match value {
    pattern1 => code1,
    pattern2 => code2,
    _ => default_code,
}
```

### 이 장에서 다루는 내용

이 장은 패턴에 관한 모든 것을 다루는 참조 자료:

1. **패턴을 사용할 수 있는 유효한 위치**
   - `match` 표현식
   - `if let` 조건부 표현식
   - `while let` 조건부 루프
   - `for` 루프
   - `let` 문
   - 함수 매개변수

2. **반박 가능(Refutable)과 반박 불가능(Irrefutable) 패턴의 차이**
   - 반박 불가능 패턴: 전달된 모든 값과 매칭되는 패턴
   - 반박 가능 패턴: 일부 값에서 매칭에 실패할 수 있는 패턴

3. **다양한 패턴 문법**
   - 리터럴 매칭
   - 명명된 변수
   - 다중 패턴
   - 범위 매칭
   - 구조 분해
   - 값 무시
   - 매치 가드
   - `@` 바인딩

**핵심 포인트:**
- 패턴은 Rust에서 값의 구조를 표현하는 강력한 방법
- `match` 표현식은 패턴의 가장 일반적인 사용처
- 패턴을 마스터하면 Rust 코드를 더 명확하고 표현력 있게 작성할 수 있음

---

## 패턴을 사용할 수 있는 모든 곳

> **원문:** https://doc.rust-lang.org/book/ch19-01-all-the-places-for-patterns.html

### 개요

- Rust에서 패턴은 언어 전반에 걸쳐 많은 곳에서 사용됨
- 이 장에서는 패턴을 적용할 수 있는 모든 유효한 위치를 다룸

**핵심 포인트:**
- 패턴은 Rust의 여러 구문에서 사용되는 강력한 기능
- `match`, `let`, `if let`, `while let`, `for`, 함수 매개변수에서 사용 가능
- 각 위치마다 패턴의 동작과 요구 사항이 다름

### `match` 갈래 (Arms)

`match` 표현식의 갈래에서 패턴을 사용:

```rust
match VALUE {
    PATTERN => EXPRESSION,
    PATTERN => EXPRESSION,
    PATTERN => EXPRESSION,
}
```

**예제:**

```rust
match x {
    None => None,
    Some(i) => Some(i + 1),
}
```

**핵심 포인트:**
- `match` 표현식은 **완전해야(exhaustive)** 함 - 모든 가능성을 다루어야 함
- 마지막 갈래에서 포괄 패턴(catch-all pattern)인 `_`를 사용하여 나머지 경우를 처리
- `_` 패턴은 모든 것과 매칭되지만 변수에 바인딩하지 않음

### `let` 문

- 모든 `let` 문은 명시적으로 보이지 않더라도 패턴을 사용함

**구문:**

```rust
let PATTERN = EXPRESSION;
```

**간단한 예제:**

```rust
let x = 5;
```

**튜플 구조 분해 예제:**

```rust
fn main() {
    let (x, y, z) = (1, 2, 3);
}
```

- 패턴 `(x, y, z)`는 튜플을 구조 분해하여 각 값을 해당 변수에 바인딩

**오류 처리:**

- 패턴의 요소 수는 값의 요소 수와 일치해야 함

```rust
fn main() {
    let (x, y) = (1, 2, 3);  // 오류: 패턴은 2개 요소, 튜플은 3개 요소
}
```

**오류 메시지:**

```
error[E0308]: mismatched types
expected a tuple with 3 elements, found one with 2 elements
```

### 조건부 `if let` 표현식

- `if let`: 단일 경우만 매칭하는 더 짧은 구문 제공, `else if`와 `else if let`과 결합 가능

**예제:**

```rust
fn main() {
    let favorite_color: Option<&str> = None;
    let is_tuesday = false;
    let age: Result<u8, _> = "34".parse();

    if let Some(color) = favorite_color {
        println!("Using your favorite color, {color}, as the background");
    } else if is_tuesday {
        println!("Tuesday is green day!");
    } else if let Ok(age) = age {
        if age > 30 {
            println!("Using purple as the background color");
        } else {
            println!("Using orange as the background color");
        }
    } else {
        println!("Using blue as the background color");
    }
}
```

**핵심 포인트:**
- `if let`은 기존 변수를 섀도잉(shadowing)하는 새 변수를 도입할 수 있음
- 변수 스코프는 해당 블록 내로 제한됨
- `match`와 달리 컴파일러는 `if let`의 완전성(exhaustiveness)을 **검사하지 않음**

### `while let` 조건부 루프

- `while let`: 패턴이 계속 매칭되는 동안 루프를 실행

**예제:**

```rust
fn main() {
    let (tx, rx) = std::sync::mpsc::channel();
    std::thread::spawn(move || {
        for val in [1, 2, 3] {
            tx.send(val).unwrap();
        }
    });

    while let Ok(value) = rx.recv() {
        println!("{value}");
    }
}
```

**출력:**

```
1
2
3
```

- `recv()`가 `Ok`를 반환하는 동안 루프 계속 → 송신자가 연결을 끊으면(`Err` 발생) 종료

### `for` 루프

- `for` 루프에서 `for` 키워드 뒤에 오는 값은 패턴

**구문:**

```rust
for PATTERN in ITERABLE {
    // 코드
}
```

**튜플 구조 분해 예제:**

```rust
fn main() {
    let v = vec!['a', 'b', 'c'];

    for (index, value) in v.iter().enumerate() {
        println!("{value} is at index {index}");
    }
}
```

**출력:**

```
a is at index 0
b is at index 1
c is at index 2
```

- `enumerate()` 메서드는 `(index, value)` 튜플을 생성하며, 이를 패턴으로 구조 분해함

### 함수 매개변수

- 함수 매개변수: 값을 구조 분해할 수 있는 패턴

**간단한 예제:**

```rust
fn foo(x: i32) {
    // 코드
}
```

**튜플 구조 분해 예제:**

```rust
fn print_coordinates(&(x, y): &(i32, i32)) {
    println!("Current location: ({x}, {y})");
}

fn main() {
    let point = (3, 5);
    print_coordinates(&point);
}
```

**출력:**

```
Current location: (3, 5)
```

- 매개변수 패턴 `&(x, y)`는 튜플 참조를 구조 분해함

**핵심 포인트:**
- 함수 매개변수에서 패턴을 사용하여 직접 구조 분해 가능
- 클로저 매개변수 목록에서도 함수 매개변수와 동일하게 패턴이 작동함

### 요약

패턴은 6개의 주요 위치에서 나타남:

- `match` 갈래: 완전해야 함(모든 경우 처리)
- `let` 문: 변수 바인딩 및 구조 분해
- `if let` 표현식: 조건부 패턴 매칭
- `while let` 루프: 패턴 기반 루프 조건
- `for` 루프: 값 반복 및 구조 분해
- 함수 매개변수: 인수 구조 분해

**핵심 포인트:**
- `match`는 완전성이 필수이지만 `if let`은 선택적
- 패턴은 구조 분해를 통해 복잡한 데이터 타입을 쉽게 분해
- 다음 섹션에서는 **반박 가능성(refutability)** - 패턴이 반드시 성공해야 하는지 실패할 수 있는지 - 를 탐구

---

## 반박 가능성: 패턴이 매칭에 실패할 수 있는지 여부

> **원문:** https://doc.rust-lang.org/book/ch19-02-refutability.html

### 개요

Rust의 패턴은 두 가지 형태로 나뉨:

1. **반박 불가능한 패턴(Irrefutable Patterns)**: **모든 가능한 값**에 대해 매칭되는 패턴, 매칭 실패 불가
   - 예: `let x = 5;`에서 `x`는 모든 것과 매칭됨
2. **반박 가능한 패턴(Refutable Patterns)**: 일부 가능한 값에 대해 **매칭에 실패할 수 있는** 패턴
   - 예: `if let Some(x) = a_value`에서 `Some(x)`는 값이 `None`이면 실패함

### 패턴 사용 규칙

**핵심 포인트:**
- **반박 불가능한 패턴만 허용**: `let` 문, 함수 매개변수, `for` 루프
- **반박 가능하거나 반박 불가능한 패턴 모두 허용**: `if let`, `while let`, `let...else` 표현식
- 컴파일러는 `if let`/`while let`/`let...else`에서 반박 불가능한 패턴을 사용하면 경고를 발생시킴 (이들은 실패를 처리하도록 설계되었기 때문)

### 예제: `let`에서 반박 가능한 패턴 사용 시 오류

```rust
fn main() {
    let some_option_value: Option<i32> = None;
    let Some(x) = some_option_value;  // ERROR
}
```

**컴파일러 오류:**
```
error[E0005]: refutable pattern in local binding
  |
3 |     let Some(x) = some_option_value;
  |         ^^^^^^^ pattern `None` not covered
  |
  = note: `let` bindings require an "irrefutable pattern"
```

**핵심 포인트:**
- `let` 바인딩은 반박 불가능한 패턴을 요구함
- `Some(x)` 패턴은 `None` 케이스를 처리하지 않으므로 반박 가능함
- 컴파일러는 커버되지 않은 패턴(`None`)을 알려줌

### 해결책: `let...else` 사용

```rust
fn main() {
    let some_option_value: Option<i32> = None;
    let Some(x) = some_option_value else {
        return;
    };
}
```

**핵심 포인트:**
- `let...else`는 반박 가능한 패턴을 허용함
- `else` 블록은 패턴이 매칭되지 않을 때 실행됨
- `else` 블록은 반드시 분기를 벗어나야 함 (`return`, `break`, `panic!` 등)

### 경고: `let...else`에서 반박 불가능한 패턴 사용

```rust
fn main() {
    let x = 5 else {
        return;
    };
}
```

**컴파일러 경고:**
```
warning: irrefutable `let...else` pattern
  |
2 |     let x = 5 else {
  |     ^^^^^^^^^
  |
  = note: this pattern will always match, so the `else` clause is useless
```

**핵심 포인트:**
- 반박 불가능한 패턴은 항상 매칭되므로 `else` 절이 무의미함
- 이런 경우 일반 `let` 문을 사용해야 함

### `match` 갈래 규칙

**핵심 포인트:**
- `match` 갈래는 **반박 가능한 패턴**을 사용해야 함
- 단, **마지막 갈래**는 남은 모든 값을 매칭하기 위해 반박 불가능한 패턴을 사용해야 함
- 이렇게 해야 모든 가능한 값이 처리됨을 보장함

### 요약

- `let` 문: 반박 불가능한 패턴만 허용
- 함수 매개변수: 반박 불가능한 패턴만 허용
- `for` 루프: 반박 불가능한 패턴만 허용
- `if let`: 반박 가능한 패턴 허용(권장)
- `while let`: 반박 가능한 패턴 허용(권장)
- `let...else`: 반박 가능한 패턴 허용(권장)
- `match` 갈래: 반박 가능한 패턴 허용(마지막 갈래 제외)

---

## 패턴 문법

> **원문:** https://doc.rust-lang.org/book/ch19-03-pattern-syntax.html

### 개요

- 이 섹션에서는 Rust에서 유효한 모든 패턴 문법을 다루고, 각 패턴을 언제 왜 사용하는지 설명

### 리터럴 매칭

- 리터럴 값에 대해 직접 패턴을 매칭 가능

```rust
fn main() {
    let x = 1;

    match x {
        1 => println!("one"),
        2 => println!("two"),
        3 => println!("three"),
        _ => println!("anything"),
    }
}
```

**핵심 포인트:**
- 코드가 특정 구체적인 값에 따라 동작해야 할 때 유용함
- 정수, 문자열 등 다양한 리터럴 타입과 매칭 가능

### 명명된 변수 매칭

- 명명된 변수: 모든 값과 일치하는 반박 불가능(irrefutable) 패턴
- 단, 패턴에서 선언된 변수는 새로운 스코프를 생성하고 외부 변수를 가림(shadow)

```rust
fn main() {
    let x = Some(5);
    let y = 10;

    match x {
        Some(50) => println!("Got 50"),
        Some(y) => println!("Matched, y = {y}"),  // y가 외부 y를 가림
        _ => println!("Default case, x = {x:?}"),
    }

    println!("at the end: x = {x:?}, y = {y}");  // y는 여전히 10
}
```

**핵심 포인트:**
- `match` 내부의 `Some(y)`는 새로운 변수 `y`를 생성
- 이 `y`는 외부 스코프의 `y`와 다른 변수
- `match` 블록이 끝나면 내부 `y`는 스코프를 벗어남

### 다중 패턴 매칭

- `|` 연산자(or 연산자)를 사용하여 여러 패턴 매칭 가능

```rust
fn main() {
    let x = 1;

    match x {
        1 | 2 => println!("one or two"),
        3 => println!("three"),
        _ => println!("anything"),
    }
}
```

**핵심 포인트:**
- `|`는 "또는"을 의미
- 여러 값에 대해 동일한 동작을 수행할 때 유용

### `..=`로 범위 매칭

- 포괄적인(inclusive) 값의 범위를 매칭

```rust
fn main() {
    let x = 5;

    match x {
        1..=5 => println!("one through five"),
        _ => println!("something else"),
    }
}
```

- 범위는 숫자 값과 `char`에서 동작함

```rust
fn main() {
    let x = 'c';

    match x {
        'a'..='j' => println!("early ASCII letter"),
        'k'..='z' => println!("late ASCII letter"),
        _ => println!("something else"),
    }
}
```

**핵심 포인트:**
- `..=`는 양 끝 값을 포함하는 범위
- 숫자와 문자 타입에서만 사용 가능
- 컴파일러가 컴파일 시점에 범위가 비어있지 않음을 확인

### 값 구조 분해

#### 구조체 구조 분해

- 패턴을 사용하여 구조체 필드를 분해

```rust
struct Point {
    x: i32,
    y: i32,
}

fn main() {
    let p = Point { x: 0, y: 7 };

    let Point { x, y } = p;  // 축약 문법
    assert_eq!(0, x);
    assert_eq!(7, y);
}
```

- 구조 분해와 리터럴 값 테스트를 혼합 가능

```rust
struct Point {
    x: i32,
    y: i32,
}

fn main() {
    let p = Point { x: 0, y: 7 };

    match p {
        Point { x, y: 0 } => println!("On the x axis at {x}"),
        Point { x: 0, y } => println!("On the y axis at {y}"),
        Point { x, y } => println!("On neither axis: ({x}, {y})"),
    }
}
```

**핵심 포인트:**
- `Point { x, y }`는 `Point { x: x, y: y }`의 축약형
- 특정 필드 값을 리터럴과 매칭하면서 다른 필드는 변수로 캡처 가능

#### 열거형 구조 분해

- 열거형 배리언트를 정의에 따라 구조 분해

```rust
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}

fn main() {
    let msg = Message::ChangeColor(0, 160, 255);

    match msg {
        Message::Quit => println!("Quit variant"),
        Message::Move { x, y } => println!("Move in x: {x}, y: {y}"),
        Message::Write(text) => println!("Text: {text}"),
        Message::ChangeColor(r, g, b) => println!("RGB({r}, {g}, {b})"),
    }
}
```

**핵심 포인트:**
- 각 배리언트의 구조에 맞는 패턴 사용
- 유닛 배리언트, 튜플 구조체 배리언트, 구조체 배리언트 각각 다르게 매칭

#### 중첩된 구조체와 열거형

```rust
enum Color {
    Rgb(i32, i32, i32),
    Hsv(i32, i32, i32),
}

enum Message {
    ChangeColor(Color),
    // ... 다른 배리언트
}

fn main() {
    let msg = Message::ChangeColor(Color::Hsv(0, 160, 255));

    match msg {
        Message::ChangeColor(Color::Rgb(r, g, b)) => {
            println!("RGB: {r}, {g}, {b}");
        }
        Message::ChangeColor(Color::Hsv(h, s, v)) => {
            println!("HSV: {h}, {s}, {v}");
        }
        _ => (),
    }
}
```

#### 구조체와 튜플

- 구조 분해 패턴을 혼합하고 중첩 가능

```rust
fn main() {
    struct Point {
        x: i32,
        y: i32,
    }

    let ((feet, inches), Point { x, y }) = ((3, 10), Point { x: 3, y: -10 });
}
```

**핵심 포인트:**
- 복잡한 타입도 한 번에 구조 분해 가능
- 패턴은 값의 형태를 반영해야 함

### 값 무시하기

#### `_`로 전체 값 무시

- 바인딩 없이 전체 값을 무시

```rust
fn foo(_: i32, y: i32) {
    println!("This code only uses the y parameter: {y}");
}
```

**핵심 포인트:**
- 함수 시그니처에서 사용하지 않는 매개변수에 유용
- 트레이트 구현 시 특정 매개변수가 필요 없을 때 사용

#### `_`로 값의 일부 무시

- 패턴의 특정 부분을 무시

```rust
fn main() {
    let mut setting_value = Some(5);
    let new_setting_value = Some(10);

    match (setting_value, new_setting_value) {
        (Some(_), Some(_)) => {
            println!("Can't overwrite an existing value");
        }
        _ => {
            setting_value = new_setting_value;
        }
    }
}
```

- 튜플에서 여러 값을 무시

```rust
fn main() {
    let numbers = (2, 4, 8, 16, 32);

    match numbers {
        (first, _, third, _, fifth) => {
            println!("Some numbers: {first}, {third}, {fifth}");
        }
    }
}
```

#### `_` 접두사로 미사용 변수 표시

- 변수 이름 앞에 `_`를 붙여 미사용 변수 경고를 억제

```rust
fn main() {
    let _x = 5;  // 경고 없음
    let y = 10;  // 경고: 미사용 변수
}
```

- **중요한 차이점**: `_x`는 여전히 값을 바인딩하지만, `_`는 전혀 바인딩하지 않음

```rust
// 오류 발생 - String이 _s로 이동됨
fn main() {
    let s = Some(String::from("Hello!"));
    if let Some(_s) = s {
        println!("found a string");
    }
    println!("{s:?}");  // 오류: s가 이동됨
}

// 컴파일됨 - String이 이동되지 않음
fn main() {
    let s = Some(String::from("Hello!"));
    if let Some(_) = s {
        println!("found a string");
    }
    println!("{s:?}");  // OK: s는 이동되지 않음
}
```

#### `..`로 나머지 부분 무시

- `..` 문법으로 값의 나머지 부분을 무시

```rust
fn main() {
    struct Point {
        x: i32,
        y: i32,
        z: i32,
    }

    let origin = Point { x: 0, y: 0, z: 0 };

    match origin {
        Point { x, .. } => println!("x is {x}"),
    }
}
```

튜플에서 `..` 사용 예:

```rust
fn main() {
    let numbers = (2, 4, 8, 16, 32);

    match numbers {
        (first, .., last) => {
            println!("Some numbers: {first}, {last}");
        }
    }
}
```

- **모호성 규칙**: `..`는 패턴당 한 번만 사용 가능

```rust
// 컴파일되지 않음
fn main() {
    let numbers = (2, 4, 8, 16, 32);

    match numbers {
        (.., second, ..) => {  // 오류: 모호함
            println!("Some numbers: {second}")
        },
    }
}
```

**핵심 포인트:**
- `..`는 하나 이상의 값을 무시
- 모호성을 피하기 위해 패턴당 한 번만 사용 가능

### 매치 가드 (Match Guards)

- 매치 가드: 암(arm)이 선택되기 위해 추가로 만족해야 하는 `if` 조건, `match` 표현식에서만 사용 가능

```rust
fn main() {
    let num = Some(4);

    match num {
        Some(x) if x % 2 == 0 => println!("The number {x} is even"),
        Some(x) => println!("The number {x} is odd"),
        None => (),
    }
}
```

- 매치 가드를 사용하여 변수 가림(shadowing) 회피 가능

```rust
fn main() {
    let x = Some(5);
    let y = 10;

    match x {
        Some(50) => println!("Got 50"),
        Some(n) if n == y => println!("Matched, n = {n}"),  // 외부 y와 비교
        _ => println!("Default case, x = {x:?}"),
    }

    println!("at the end: x = {x:?}, y = {y}");
}
```

- `|`로 여러 패턴을 매치 가드와 결합

```rust
fn main() {
    let x = 4;
    let y = false;

    match x {
        4 | 5 | 6 if y => println!("yes"),  // 가드가 모든 패턴에 적용
        _ => println!("no"),
    }
}
```

- 가드는 모든 패턴에 적용됨: `(4 | 5 | 6) if y => ...`

**핵심 포인트:**
- 패턴만으로 표현할 수 없는 복잡한 조건에 유용
- 가드는 `|`로 결합된 모든 패턴에 적용됨
- 외부 변수를 참조하여 섀도잉 문제 해결 가능

### @ 바인딩

- `@` 연산자: 값을 패턴과 테스트하면서 동시에 변수에 바인딩

```rust
fn main() {
    enum Message {
        Hello { id: i32 },
    }

    let msg = Message::Hello { id: 5 };

    match msg {
        Message::Hello { id: id_variable @ 3..=7 } => {
            println!("Found an id in range: {id_variable}")
        }
        Message::Hello { id: 10..=12 } => {
            println!("Found an id in another range")
        }
        Message::Hello { id } => println!("Found some other id: {id}"),
    }
}
```

출력: `Found an id in range: 5`

**핵심 포인트:**
- `@`를 사용하면 값을 테스트하면서 동시에 캡처 가능
- 범위 패턴과 함께 사용할 때 특히 유용
- 첫 번째 암은 범위 테스트와 값 캡처를 동시에 수행
- 두 번째 암은 범위만 테스트하고 값을 사용할 수 없음

### 요약

- Rust의 패턴은 다양한 종류의 데이터를 구별하는 강력한 방법 제공

**주요 패턴 문법:**
- **리터럴 매칭**: 특정 값과 직접 비교
- **명명된 변수**: 모든 값을 캡처 (섀도잉 주의)
- **다중 패턴 (`|`)**: 여러 패턴 중 하나와 매칭
- **범위 (`..=`)**: 숫자나 문자의 범위 매칭
- **구조 분해**: 구조체, 열거형, 튜플의 내부 값 추출
- **값 무시 (`_`, `..`)**: 필요 없는 값 무시
- **매치 가드**: 추가 조건으로 암 선택 제한
- **@ 바인딩**: 테스트와 캡처 동시 수행

- 컴파일러는 `match` 표현식에서 패턴의 완전성(exhaustiveness)을 보장함
- `let` 문과 함수 매개변수의 패턴은 값을 더 작은 부분으로 구조 분해할 수 있게 해줌

---

## 안전하지 않은 러스트

## 안전하지 않은 Rust (Unsafe Rust)

> **원문:** https://doc.rust-lang.org/book/ch20-01-unsafe-rust.html

### 개요

- Unsafe Rust: Rust 내부에 숨겨진 두 번째 언어, 컴파일 타임에 메모리 안전성 보장을 강제하지 않음
- 존재하는 이유:

1. **정적 분석은 보수적임** - 컴파일러는 유효하지 않은 프로그램을 수용하지 않기 위해 일부 유효한 프로그램도 거부함
2. **하드웨어는 본질적으로 안전하지 않음** - 저수준 시스템 프로그래밍에는 Rust의 안전성 규칙이 방지하는 작업이 필요함

### 다섯 가지 Unsafe 슈퍼파워

- `unsafe` 키워드를 사용하면 안전한 Rust에서 허용되지 않는 다섯 가지 작업을 수행 가능:

1. 원시 포인터(raw pointer) 역참조
2. 안전하지 않은 함수 또는 메서드 호출
3. 가변 정적 변수 접근 또는 수정
4. 안전하지 않은 트레이트 구현
5. 유니온(union)의 필드 접근

- **중요**: `unsafe`는 빌림 검사기나 다른 안전성 검사를 비활성화하지 않음 → 메모리 안전성 검증 없이 이 다섯 가지 기능에만 접근을 허용

---

### 1. 원시 포인터 역참조

#### 원시 포인터 생성

- 원시 포인터(`*const T`와 `*mut T`): 참조와 유사하지만 빌림 규칙을 우회 가능

```rust
fn main() {
    let mut num = 5;

    let r1 = &raw const num;  // *const i32
    let r2 = &raw mut num;    // *mut i32
}
```

- 원시 포인터는 안전한 코드에서 생성 가능하지만, 역참조는 `unsafe` 필요

```rust
fn main() {
    let mut num = 5;

    let r1 = &raw const num;
    let r2 = &raw mut num;

    unsafe {
        println!("r1 is: {}", *r1);
        println!("r2 is: {}", *r2);
    }
}
```

#### 참조와의 주요 차이점

**원시 포인터의 특징:**
- 빌림 규칙을 무시할 수 있음 (같은 위치에 불변과 가변 포인터 모두 가능)
- 유효한 메모리를 가리킨다는 보장이 없음
- null일 수 있음
- 자동 정리(cleanup)가 구현되어 있지 않음

#### 임의의 메모리에서 생성

```rust
fn main() {
    let address = 0x012345usize;
    let r = address as *const i32;  // 위험: 정의되지 않은 동작
}
```

---

### 2. 안전하지 않은 함수 또는 메서드 호출

- 안전하지 않은 함수는 호출하기 위해 `unsafe` 블록 필요

```rust
fn main() {
    unsafe fn dangerous() {}

    unsafe {
        dangerous();
    }
}
```

- 블록 없이 호출 → 컴파일러 오류 발생:
```
error[E0133]: call to unsafe function `dangerous` is unsafe and requires unsafe block
```

#### 안전하지 않은 코드 위에 안전한 추상화 만들기

- unsafe 코드를 안전한 함수로 감싸서 `unsafe`가 코드베이스 전체로 퍼지는 것을 방지

```rust
use std::slice;

fn split_at_mut(values: &mut [i32], mid: usize) -> (&mut [i32], &mut [i32]) {
    let len = values.len();
    let ptr = values.as_mut_ptr();

    assert!(mid <= len);

    unsafe {
        (
            slice::from_raw_parts_mut(ptr, mid),
            slice::from_raw_parts_mut(ptr.add(mid), len - mid),
        )
    }
}

fn main() {
    let mut vector = vec![1, 2, 3, 4, 5, 6];
    let (left, right) = split_at_mut(&mut vector, 3);
    assert_eq!(left, &mut [1, 2, 3]);
    assert_eq!(right, &mut [4, 5, 6]);
}
```

- **핵심 포인트**: 함수 자체는 검증된 전제 조건으로 안전한 인터페이스를 제공하므로 `unsafe`로 표시할 필요 없음

#### `extern` 함수를 사용하여 외부 코드 호출

- 외부 함수 인터페이스(FFI)를 통해 C 함수 호출

```rust
unsafe extern "C" {
    fn abs(input: i32) -> i32;
}

fn main() {
    unsafe {
        println!("Absolute value of -3 according to C: {}", abs(-3));
    }
}
```

##### 함수를 안전하게 표시하기

- 외부 함수가 증명 가능하게 안전하다면 `safe` 키워드 사용

```rust
unsafe extern "C" {
    safe fn abs(input: i32) -> i32;
}

fn main() {
    println!("Absolute value of -3 according to C: {}", abs(-3));
}
```

- **주의**: 함수를 `safe`로 표시하는 것은 Rust에게 하는 약속 → 자동으로 안전해지지는 않음

#### 다른 언어에서 Rust 함수 호출

- 이름 맹글링(name mangling)을 비활성화하려면 `#[unsafe(no_mangle)]` 사용

```rust
#[unsafe(no_mangle)]
pub extern "C" fn call_from_c() {
    println!("Just called a Rust function from C!");
}
```

---

### 3. 가변 정적 변수 접근 또는 수정

#### 불변 정적 변수

- 접근이 안전함

```rust
static HELLO_WORLD: &str = "Hello, world!";

fn main() {
    println!("value is: {HELLO_WORLD}");
}
```

#### 가변 정적 변수

- 접근과 수정에 `unsafe` 필요

```rust
static mut COUNTER: u32 = 0;

/// SAFETY: 한 번에 하나의 스레드에서만 호출해야 합니다.
/// 여러 스레드에서 동시에 호출하면 정의되지 않은 동작이 발생합니다.
unsafe fn add_to_count(inc: u32) {
    unsafe {
        COUNTER += inc;
    }
}

fn main() {
    unsafe {
        // SAFETY: 이것은 `main`의 단일 스레드에서만 호출됩니다.
        add_to_count(3);
        println!("COUNTER: {}", *(&raw const COUNTER));
    }
}
```

- **모범 사례**: 가능하면 16장의 동시성 기법을 대신 사용

---

### 4. 안전하지 않은 트레이트 구현

- 안전하지 않은 트레이트를 선언하고 구현

```rust
unsafe trait Foo {
    // 메서드가 여기에 옵니다
}

unsafe impl Foo for i32 {
    // 메서드 구현이 여기에 옵니다
}
```

#### 예시: Send와 Sync

- 원시 포인터를 포함하는 타입에 대해 `Send` 또는 `Sync`를 수동으로 구현

```rust
unsafe impl Send for MyType {}
unsafe impl Sync for MyType {}
```

---

### 5. 유니온의 필드 접근

- 유니온 필드에 접근(주로 C 상호 운용을 위해)

```rust
union MyUnion {
    i: i32,
    f: f32,
}
```

- Rust가 저장된 데이터의 타입을 보장할 수 없어 안전하지 않음 → 자세한 내용은 [Rust 레퍼런스](https://doc.rust-lang.org/reference/items/unions.html) 참고

---

### Miri로 Unsafe 코드 검사하기

- Miri: 코드를 동적으로 실행하여 정의되지 않은 동작을 감지하는 공식 도구

```bash
# Miri 설치
rustup +nightly component add miri

# 프로젝트에서 실행
cargo +nightly miri run
cargo +nightly miri test
```

#### 예시

- 문제가 있는 코드에 Miri 실행

```rust
fn main() {
    use std::slice;
    let address = 0x01234usize;
    let r = address as *mut i32;
    let values: &[i32] = unsafe { slice::from_raw_parts_mut(r, 10000) };
}
```

Miri 출력:
```
error: Undefined Behavior: pointer not dereferenceable: pointer must be dereferenceable
for 40000 bytes, but got 0x1234[noalloc] which is a dangling pointer
```

- **참고**: Miri는 동적임 → 실행된 코드의 버그를 잡지만 모든 문제를 찾는 것을 보장하지는 않음

---

### 모범 사례

**핵심 포인트:**
- **unsafe 블록을 작게 유지** - 잠재적 문제의 범위를 최소화
- **SAFETY 주석 작성** - 코드가 안전한 이유를 설명:
  ```rust
  // SAFETY: 이 포인터는 ... 때문에 유효함이 보장됩니다.
  unsafe { /* 코드 */ }
  ```
- **Miri 사용** - unsafe 코드를 검증
- **안전한 추상화 선호** - unsafe 코드를 안전한 API로 감싸기
- **요구 사항 문서화** - 호출자가 보장해야 할 것을 명확히 명시

---

### 추가 자료

- [The Rustonomicon](https://doc.rust-lang.org/nomicon/) - Unsafe Rust 공식 가이드
- [Miri GitHub 저장소](https://github.com/rust-lang/miri)
- [Rust Reference - FFI](https://doc.rust-lang.org/reference/items/external-blocks.html)
