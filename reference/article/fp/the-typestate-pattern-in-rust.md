# Rust의 타입스테이트 패턴 (The Typestate Pattern in Rust)

- 원문: [The Typestate Pattern in Rust](https://cliffle.com/blog/rust-typestate/)
- 작성일: 2019년 6월 5일
- 작성자: Cliff L. Biffle (Cliffle)
- 태그: 디자인 패턴, Rust, 타입 시스템

> 이 문서는 Cliff L. Biffle의 블로그 글 "The Typestate Pattern in Rust" 전문을 한국어로 번역한 것입니다. (각주 포함)

## 목차

  * [타입스테이트란 무엇인가?](#타입스테이트란-무엇인가)
  * [간단한 예: 산 것과 죽은 것](#간단한-예-산-것과-죽은-것)
  * [둘 이상의 "살아 있는" 상태](#둘-이상의-살아-있는-상태)
  * [실전 속 타입스테이트: serde](#실전-속-타입스테이트-serde)
  * [변형: 상태 타입 매개변수](#변형-상태-타입-매개변수)
  * [변형: 실제 상태를 담는 상태 타입](#변형-실제-상태를-담는-상태-타입)
  * [결론](#결론)

---

*타입스테이트 패턴(typestate pattern)*은 객체의 런타임 *상태(state)*에 관한 정보를 그 객체의 컴파일 타임 *타입(type)*에 인코딩하는 API 설계 패턴입니다. 특히, 타입스테이트 패턴을 사용하는 API는 다음을 갖습니다.

  1. 객체가 특정 상태에 있을 때만 사용할 수 있는, 객체에 대한 연산(메서드나 함수 등),

  2. 잘못된 상태에서 그 연산을 사용하려는 시도가 컴파일에 실패하도록, 이러한 상태들을 타입 수준에서 인코딩하는 방법,

  3. 객체의 런타임 동적 상태를 바꾸는 것에 더해, 또는 그 대신, 객체의 타입 수준 상태를 *바꾸는* 상태 전이 연산(메서드나 함수). 그래서 이전 상태에서 쓰던 연산이 더 이상 불가능해지도록 합니다.

이것이 유용한 이유는 다음과 같습니다.

  * 특정 종류의 오류를 런타임에서 컴파일 타임으로 옮겨, 프로그래머에게 더 빠른 피드백을 줍니다.
  * IDE와 잘 맞물립니다. IDE는 특정 상태에서 불가능한 연산을 제안하지 않을 수 있습니다.
  * 런타임 검사를 제거하여, 코드를 더 빠르고/작게 만들 수 있습니다.

이 패턴은 Rust에서 너무나 쉬워서 거의 *당연하게* 느껴질 정도이며, 어쩌면 당신은 깨닫지 못한 채 이미 이 패턴을 사용하는 코드를 작성했을지도 모릅니다. 흥미롭게도, 대부분의 다른 프로그래밍 언어에서는 이를 구현하기가 *매우 어렵습니다*. 대부분의 언어는 위의 2번과/또는 3번 항목을 만족하지 못합니다.

저는 이 패턴의 미묘한 점들을 자세히 다룬 글을 본 적이 없어서, 여기 제 기여를 남깁니다.

# 타입스테이트란 무엇인가?

[타입스테이트(Typestates)](https://en.wikipedia.org/wiki/Typestate_analysis)는 *상태*(프로그램이 처리하는 동적 정보)의 속성을 *타입* 수준(컴파일러가 미리 검사할 수 있는 정적 세계)으로 옮기는 기법입니다.

타입스테이트는 제가 여기서 다룰 특정 패턴보다 더 넓은 주제이며, 그래서 저는 이를 "타입스테이트 *패턴*"이라고 부릅니다.

여기서 우리가 관심 있는 타입스테이트의 특수한 경우는, 그것이 런타임의 *연산 순서(order of operations)*를 컴파일 타임에 강제할 수 있는 방식입니다. 다음은 Rust에서 타입스테이트 패턴으로 강제할 수 있는 속성들의 예입니다(제 주장입니다. 이 모두의 구현체를 가지고 있지는 않습니다).

  * "버퍼가 유효한 UTF-8인지 확인한 경우에만 그것을 변환할 수 있다."

  * "파일 핸들이 닫힌 후에는 그 핸들에 어떤 I/O 연산도 수행해서는 안 된다."

  * "이 메시지들은 인증이 성공한 후에만 클라이언트로 보낼 수 있고, 세션을 종료한 후에는 보낼 수 없다."

  * "동작 A를 하고 나면, D를 할 수 있기 전에 B 또는 C 중 하나를(둘 다는 아니고) 반드시 수행해야 한다."

대부분의 다른 언어에서는 이를 런타임 검사와 오류/예외로 처리해야 할 것입니다. 또는, 게을러져서 아예 확인하지 않고, 대신 문서에 언급하고는 사람들이 그것을 읽기를 바랄 수도 있습니다!

타입스테이트 패턴을 사용하면, 이 규칙들을 어기는 코드가 컴파일되지 못하게 막아, 프로그래머가 실수를 더 일찍 찾도록 돕고 런타임 검사의 오버헤드를 제거할 수 있습니다.

# 간단한 예: 산 것과 죽은 것

Rust 라이브러리에는 API가 "살아 있는(living)"과 "죽은(dead)" 두 상태를 가질 수 있게 하는 흔한 패턴이 있습니다. 구체적으로 말하면, 표준 라이브러리의 [`std::fs::File`](https://doc.rust-lang.org/std/fs/struct.File.html)은 "열림(open)"과 "닫힘(closed)" 두 상태를 가집니다. `File`에 접근할 수 있다면, 그것은 열려 있는 것입니다. 하나를 얻는[^1] 유일한 방법은 `open` 연산뿐입니다.

    // 이 시점에서, 우리는 아직 File에 접근할 수 없다.

    // 하나를 연다:
    let file = std::fs::File::open("myfile.txt")?;

    // 여기서 우리는 `file`에 접근할 수 있고, 그것은 열려 있다.
    // 만약 여는 데 실패했다면, 우리는 오류를 받았을 것이다.

파일을 스코프 밖으로 나가게 두어 닫을 수 있지만, 이 논의를 위해 `drop`을 사용해 명시적으로 접근 권한을 포기합시다.

    drop(file);

    // 닫은 후에 파일을 사용하려 하면 컴파일 오류다:
    // file.read_to_string(&mut buffer);  <-- 컴파일되지 않음

이것이 동작하는 이유는 `drop`의 시그니처 때문입니다.

    pub fn drop<T>(value: T);

즉, `drop`은 그 인자를 참조(`&T`)가 아니라 *값으로(by value)* 받습니다. 이는 인자가 `drop` 함수로 *이동(move)*되어 들어가, 호출자가 그것에 대한 접근을 잃는다는 뜻입니다.

이것은 "`File`을 닫은 후에는 그 위에서 다른 연산을 수행할 수 없다"는 속성을 강제하는, 타입스테이트 패턴의 한 예입니다.

이것이 당신에게 [RAII](https://en.wikipedia.org/wiki/Resource_acquisition_is_initialization)처럼 보인다면, 맞습니다. Rust에서 RAII 패턴의 대부분의 경우는 두 상태 타입스테이트 패턴의 적용이기도 합니다. 예를 들어, `Box`는 그것이 담은 포인터의 두 가지 속성을 유지합니다.

  1. 어느 시점에서든 주어진 힙 할당 청크를 가리키는 `Box`는 하나뿐일 수 있다.

  2. 힙 할당 청크가 해제되고 나면, 어떤 `Box`도 그것을 가리킬 수 없다.

그러나 다른 언어, 특히 C++에서의 RAII와 비교하면 결정적인 차이가 있습니다. 상태를 바꿀 때, 이전 상태에 있던 *그 객체*는, 그것이 열린 파일이든 스마트 포인터든, *더 이상 사용될 수 없습니다*. 이는 Rust의 "이동(moving)" 개념이 이전 소유자로 하여금 값에 대한 접근을 잃게 만들기 때문인데, 대부분의 다른 언어에서는 그렇지 않습니다.

## 곁가지: 다른 언어에서 이것이 잘 안 되는 이유

다음은 `Box`를 사용한, RAII-as-타입스테이트의 간단한 Rust 예제입니다. `Box`가 이동된 후에 그것을 사용하려 시도할 것이고, 이는 컴파일에 실패할 것입니다.

    fn take_box_by_value(value: Box<i32>) {
        // 아무것도 안 함; value는 드롭될 때 해제된다
    }

    fn main() {
        // 정수를 힙에 할당한다.
        let ptr = Box::new(42);
        // 그것을 이동으로 함수에 넘긴다.
        take_box_by_value(ptr);

        // 그것을 역참조한다.
        println!("{}", *ptr); // <-- 컴파일 오류: value moved
                              // (이것은 좋은 일이다)
    }

다음은 C++의 동등한 예제입니다. (C++이 Rust와 매우 비슷하기 때문에 골랐습니다.) 이 예제는 *동작하지 않습니다*. 즉, *컴파일됩니다*.

    static int take_unique_ptr_by_value(std::unique_ptr<int> value) {
        // 아무것도 안 함; value는 소멸자가 실행될 때 해제된다
    }

    int main(int argc, char *argv[]) {
        // 정수를 힙에 할당한다.
        auto ptr = std::make_unique<int>(42);
        // 그것을 이동으로 함수에 넘긴다.
        // C++에서 이동 시맨틱은 기본값이 아니므로 std::move를 쓴다.
        take_unique_ptr_by_value(std::move(ptr));
        // 그것을 역참조한다.
        std::cout << *ptr << std::endl;
    }

이 프로그램은 GCC 7 `-Wall`에서 경고 하나 없이 컴파일됩니다. 여기서 하는 일이 기술적으로 합법적이기 때문입니다. C++에서 값을 이동하는 것은 원본에 접근할 수 있는 우리의 능력에 영향을 주지 않습니다. 사실 C++은 우리가 `ptr`을 이동한 후에 그것이 무엇을 담는지조차 정의합니다. 이제 그것은 `nullptr`입니다. 그래서 이것은 아무것도 출력하지 않고 크래시하는 "유효한" 프로그램입니다.[^2]

C++에서의 *이동(move)*은 사실 복사(copy)인데, 원본을 바꿀 수 있는 특별한 복사입니다. 이동 후에 원본으로 무언가를 하는 것을 막는 것은 아무것도 없습니다. 잘해 봐야, 우리가 시도할 때 런타임에 예외를 던질 수 있을 뿐입니다. (최악의 경우, 이 문제를 무시하고 정의되지 않은 일을 할 수 있습니다.)

두 언어의 이동 시맨틱에서의 이 차이는 *미묘하지만*, 이 작은 차이가 C++에서 견고한 타입스테이트 패턴을 구현하기 어렵게 만들기에 충분합니다. 다른 언어 중에 이동 시맨틱(또는 선형 타입처럼 같은 일을 할 수 있는 메커니즘)을 *가진* 언어는 거의 없으며, 이는 이 패턴을 훨씬, 훨씬 더 어렵게 만듭니다.

# 둘 이상의 "살아 있는" 상태

RAII를 넘어서, 우리는 객체에 대해 둘 이상의 "살아 있는" 상태를 갖고 싶을 수 있습니다.

동기를 부여하는 예가 여기 있습니다. HTTP 프로토콜 응답을 생성하는 인터페이스를 설계해 봅시다. HTTP의 세세한 부분까지 파고들 필요는 없습니다. 이 논의를 위해 알아야 할 것은, HTTP 응답이 다음으로 구성된다는 것뿐입니다.

  1. 정확히 하나의 상태 줄(status line).
  2. 0개 이상의 헤더(headers).
  3. 선택적 본문(optional body).

우리는 이것이 컴파일되는 API를 원합니다.

    fn a_simple_response(r: HttpResponse) {
        r.status_line(200, "OK")
          .header("X-Unexpected", "Spanish-Inquisition")
          .header("Content-Length", "6")
          .body("Hello!")
    }

그리고 이것은 컴파일되지 않기를 원합니다.

    fn broken_response_1(r: HttpResponse) {
        r.header("X-Unexpected", "Spanish-Inquisition")
          // 오류: status_line 호출을 잊었음
    }

    fn broken_response_2(r: HttpResponse) {
        r.status_line(200, "OK")
          .body("Hello!")
          .header("X-Unexpected", "Spanish-Inquisition")
            // 오류: body 다음에 헤더를 보내려 했음
    }

(제가 *메서드 체이닝(method chaining)*, 즉 `x.foo().bar()`를 쓰고 있다는 점에 주목하세요. 이유는 곧 분명해질 것입니다.)

이를 하는 한 가지 방법은 각 상태를 *별도의 타입*으로 모델링하는 것입니다.

  * `status_line` 연산은 `HttpResponse`를 `HttpResponseAfterStatus`로 변환합니다.
  * `header`는 타입을 그대로 둡니다.
  * `body`는 `HttpResponseAfterStatus`를 *소비(consume)*하고 `()`를 반환합니다.

구체적으로:

    struct HttpResponse { ... }
    struct HttpResponseAfterStatus { ... }

    impl HttpResponse {
        fn status_line(self, code: u8, message: &str)
            -> HttpResponseAfterStatus
        {
            // ...본문 생략
        }
    }

    impl HttpResponseAfterStatus {
        fn header(self, key: &str, value: &str) -> Self {
            // ...본문 생략
        }

        fn body(self, text: &str) {
            // ...본문 생략
        }
    }

각 연산이 `self`를 *소비*하고 특정 상태의 새 객체를 만들거나, (`body`의 경우) 아무것도 만들지 않고 과정을 끝낸다는 점에 주목하세요.

  * 상태 줄 이전에는 헤더를 보낼 수 없습니다. 그 연산이 아예 정의되어 있지 않기 때문입니다.
  * 헤더 이후에는 상태 줄을 보낼 수 없습니다. 마찬가지로, 그 타입에 정의되어 있지 않기 때문입니다.
  * 본문을 보낸 후에는 *아무것도* 할 수 없습니다. 객체가 우리에게서 가져가졌기 때문입니다.

꽤 괜찮은 인터페이스입니다. 구현을 생각해 봅시다.

`self`(`HttpResponse` 또는 `HttpResponseAfterStatus`)가 매 단계에서 소비되고 다시 생성되므로, 그것은 저렴해야, 즉 상당히 작아야 합니다. 쉬운 기법은 두 타입 모두를, 모든 타입 상태에서 동일하게 유지되는 실제 상태에 대한 스마트 포인터를 감싸는 단순한 래퍼(wrapper)로 만드는 것입니다.

    struct HttpResponse(Box<ActualResponseState>);
    struct HttpResponseAfterStatus(Box<ActualResponseState>);

    struct ActualResponseState { ... }

이 API는 괜찮지만, 헤더를 루프에서 생성하고 싶다면 소비된 `self` 값을 계속 교체해야 하므로 어색해집니다.

    fn many_headers(r: HttpResponse, headers: Vec<Header>) {
        let mut r = r.status_line(200, "OK");
        for h in headers {
            // 이렇게 해야 하는 건 좀 짜증난다:
            r = r.header(h.key, h.value);
            // 다행히, `r =` 부분을 잊으면
            // 컴파일이 실패한다.
        }
        r.body("hello!")
    }

이를 더 쾌적하게 만들기 위해, 타입 상태를 바꾸지 않는 연산들은 흔히 `&self`나 `&mut self`를 받도록 정의됩니다.

    // 다시 시도해 보자:
    impl HttpResponseAfterStatus {
        fn header(&mut self, key: &str, value: &str) {
            // ...
        }
    }

    fn many_headers(r: HttpResponse, headers: Vec<Header>) {
        let mut r = r.status_line(200, "OK");
        for h in headers {
            // 아아, 훨씬 낫다.
            r.header(h.key, h.value);
        }
        r.body("hello!")
    }

선택적으로(위에는 나오지 않았습니다), 그 연산이 self에 대한 참조를 *반환하게* 하여, 사용자가 메서드 체이닝을 쓸지 말지 선택하도록 할 수도 있습니다.

# 실전 속 타입스테이트: `serde`

Rust 생태계에서 널리 쓰이는 타입스테이트 패턴의 예는 여럿 있습니다. 제가 아는 가장 유명한 것은 `serde`입니다. [`Serializer`](https://docs.serde.rs/serde/ser/trait.Serializer.html)는 타입스테이트를 사용해 꽤 복잡한 상태 기계를 모델링합니다. 예를 들면,

  * `Serializer`에서 시작하여,
  * `serialize_struct` 연산이 그것을 소비하고 [`SerializeStruct`](https://docs.serde.rs/serde/ser/trait.SerializeStruct.html) 트레이트를 구현하는 객체를 만듭니다.
  * `serialize_field`와/또는 `skip_field` 메서드를 0번 이상 호출할 수 있습니다.
  * `end` 메서드가 `SerializeStruct`를 소비하고 결과를 만듭니다.

`serialize_struct`를 실수로 두 번 호출하거나, `serialize_struct`와 `serialize_i32`를 둘 다 호출하거나, `end`를 호출한 후에 구조체에 필드를 추가할 수 없습니다. 하나가 기대되는 자리에 두 값을 직렬화할 수 없습니다. 이 중 어느 것이라도 시도하면 컴파일 오류가 납니다.

`Serializer`는 제3자가 새 데이터 형식을 정의하기 위해 구현하는 트레이트입니다. 그 트레이트가 타입스테이트 패턴으로 명세되어 있으므로, *안전한 코드(safe code)로는 구현체가 잘못 동작하는 것이 기본적으로 불가능합니다*. 무작위로 패닉(panic)하는 것을 제외하면 말이죠. 저는 이 견고함이 `serde`의 성공의 한 이유라고 짐작합니다.

# 변형: 상태 타입 매개변수

각 상태마다 *별도의* 구조체를 두는 대신, 상태를 단일 제네릭 구조체의 타입 *매개변수(parameter)*로 모델링할 수 있습니다. 이는 흔히 상용구(boilerplate)가 더 적고 완전히 별도의 타입을 두는 것보다 더 강력하지만, 설명하기도 더 어려워서 먼저 다루지 않았습니다.

HTTP 응답 예제를 다시 봅시다. 상태 타입 매개변수를 사용해 다음과 같이 모델링할 수 있습니다.

    // S는 상태 매개변수다. 사용자가 HttpResponse<u8> 같은
    // 이상한 타입을 시도하는 것을 막기 위해, S가 우리의
    // ResponseState 트레이트(아래)를 구현하도록 요구한다.
    struct HttpResponse<S: ResponseState> {
        // 이전 예제와 동일한 필드.
        state: Box<ActualResponseState>,
        // 매개변수가 사용된다는 것을 컴파일러에게
        // 확신시켜 준다.
        marker: std::marker::PhantomData<S>,
    }

    // 상태 타입 선택지.
    enum Start {} // 상태 줄을 기대 중
    enum Headers {} // 헤더 또는 본문을 기대 중

    trait ResponseState {}
    impl ResponseState for Start {}
    impl ResponseState for Headers {}

여기서 잠깐 멈추겠습니다. 일어나는 일이 많기 때문입니다.

거의 모든 `T`에 적용될 수 있는 `Vec<T>` 같은 제네릭 타입과 달리, 우리는 `HttpResponse<S>` 타입이 `S`의 두 값, 즉 `HttpResponse<Start>`와 `HttpResponse<Headers>`하고만 쓰이기를 기대합니다. (엄밀히 말하면, 작성된 그대로는 사용자가 자신의 커스텀 타입에 대해 `ResponseState`를 구현하고 그것을 `HttpResponse`와 함께 쓰려 시도할 수 있습니다. 그게 거슬린다면 [봉인된 트레이트 패턴(sealed trait pattern)](https://rust-lang-nursery.github.io/api-guidelines/future-proofing.html#sealed-traits-protect-against-downstream-implementations-c-sealed)을 써서 고칠 수 있습니다.)

`State`와 `Headers`는 타입으로만 존재하고 값으로는 존재하지 않는 타입입니다. 이를 보장하기 위해 변형이 0개인 enum(zero-variant enum) 패턴을 사용했습니다. 이런 타입은 넓게 *팬텀 타입(phantom types)*이라 불리며, `std::marker::PhantomData`라는 이름이 여기서 유래합니다.

좋습니다, `HttpResponse`에 대한 연산을 몇 개 정의해 봅시다.

    /// Start 상태에서만 유효한 연산들.
    impl HttpResponse<Start> {
        fn new() -> Self {
            // ...
        }

        fn status_line(self, code: u8, message: &str)
            -> HttpResponse<Headers>
        {
            // ...
        }
    }

    /// Headers 상태에서만 유효한 연산들.
    impl HttpResponse<Headers> {
        fn header(&mut self, key: &str, value: &str) {
            // ...
        }

        fn body(self, contents: &str) {
            // ...
        }
    }

여기까지는 제네릭을 쓰지 않은 원래 코드와 동등합니다. 그렇다면 이것이 왜 더 강력할까요?

상태 타입 매개변수는 몇 가지 흥미로운 것을 가능하게 합니다.

  1. 모든 상태에서 유효한 연산, 또는 상태의 부분집합에서 유효한 연산을 추가하기가 쉽고 간결합니다.

  2. `HttpResponse`에 대한 이 모든 연산이 `HttpResponse`의 동일한 생성된 `rustdoc`에, 다만 impl 블록마다 하나씩, 별도의 제목 아래에 나타납니다. 위에서 보인 것처럼 각 impl 블록에 문서 주석을 붙여 사용자가 따라오기 쉽게 할 수 있습니다. 상태마다 하나의 타입을 두는 이전 예제에서는, 메서드들이 여러 페이지에 흩어져 따라가기가 더 어렵습니다.

모든 상태에서 유효한 연산을 추가하려면, 그냥 `S`를 제약하지 않고 둡니다.

    /// 이 연산들은 어떤 상태에서도 사용 가능하다.
    impl<S> HttpResponse<S> {
        fn bytes_so_far(&self) -> usize { /* ... */ }
    }

이 예제에는 상태가 둘뿐이지만, 더 많았다면 *둘 이상의* 상태에서는 유효하지만 *모든 상태*에서는 유효하지 않은 연산을 추가하고 싶을 수 있습니다. 이를 하려면, 그 상태들을 식별하는 트레이트와, 그 연산들을 정의하는 제약된 impl 블록을 쓸 수 있습니다.

    /// 데이터를 보내고 있는 상태들이 구현하는 트레이트.
    trait SendingState {}
    impl SendingState for Headers {}
    // 다른 상태들이 여기 올 수 있다

    /// 이 연산들은 SendingState를 구현하는
    /// 상태에서만 사용 가능하다.
    impl<S> HttpResponse<S>
        where S: SendingState
    {
        fn spam_spam_spam(&mut self);
    }

# 변형: 실제 상태를 담는 상태 타입

각 상태를 완전히 별도의 타입으로 모델링했던 원래 버전에서는, 한 구조체에는 필드를 추가하고 다른 구조체에는 추가하지 않아 상태마다 다른 정보를 저장하기가 쉬웠습니다. 이제 모든 상태에서 단일 제네릭 구조체를 쓰면서, 이 능력을 잃은 것처럼 보입니다. 하지만 이를 고칠 수 있습니다.

바로 위 예제에서, 상태 타입 매개변수로 쓰인 상태 타입들은 런타임에 인스턴스화될 수 없는 팬텀 타입이었습니다. 이것은 흔히 유용하지만, *반드시* 그래야 하는 것은 아닙니다. 상태 타입이 구체적(concrete)이라면, 그 안에 무언가를 저장할 수 있습니다. 공통 구조체 *안에* `S`를 저장함으로써, 우리는 그 내용을 물려받습니다.

예를 들어, HTTP 응답에서 클라이언트에게 돌려보낸 상태 코드를 추적하고 싶을 수 있습니다. `Option<u8>`을 쓸 수도 있는데, 처음에는 `None`이었다가 `status_line`에서 `Some(code)`로 설정될 것입니다. 하지만 이는 세 가지 이유로 이상적이지 않습니다.

  1. 어떤 상태의 어떤 함수든, 상태 줄을 보낸 후에야 코드에 접근하는 것이 말이 되는데도, 그 코드에 접근하려 시도할 수 있습니다.

  2. 상태 줄을 보낸 후에는 그 필드가 설정되어 있음을 *아는데도*, 코드에 대한 모든 접근이 `None`을 처리해야 합니다.

  3. 모든 상태에서 코드를 위한 공간을 할당하게 되는데, 이는 낭비입니다. 이 경우에는 필드가 작지만(1바이트), 만약 크다면 어떨까요?

상태 매개변수로 쓰이는 상태 타입 안에 상태를 넣으면 이 문제들이 사라집니다.

    // 이전과 비슷하게:
    struct HttpResponse<S: ResponseState> {
        // 이전 예제와 동일한 필드.
        state: Box<ActualResponseState>,
        // PhantomData<S> 대신, 실제 복사본을 저장한다.
        extra: S,
    }

    // 상태 타입 선택지. 이제 이것들이 HttpResponse에 필드를 추가할 수 있다.

    // Start는 필드를 추가하지 않는다.
    struct Start;

    // Headers는 우리가 보낸 응답 코드를 기록하는 필드를 추가한다.
    struct Headers {
        response_code: u8,
    }

    trait ResponseState {}
    impl ResponseState for Start {}
    impl ResponseState for Headers {}

이제 `HttpResponse<Start>`를 다룰 때는, `response_code`에 접근하려 할 수 없습니다. 그것은 아예 거기 없습니다. 이는 또한 `Start` 상태에서 응답이 `Headers` 상태보다 1바이트 더 작다는 뜻이기도 합니다. 여기서는 아마 무의미하지만, 더 많은 상태를 저장해야 한다면 유용해질 수 있습니다.

반면 `Headers` 상태에서는 `response_code`를 가지고 있음이 보장되고, 그것에 직접 접근할 수 있습니다.

    impl HttpResponse<Start> {
        fn status_line(self, response_code: u8, message: &str)
            -> HttpResponse<Headers>
        {
            // 새 상태에 응답 코드를 담는다.
            // 실제 HTTP 구현이라면 아마 데이터도
            // 좀 보내고 싶을 것이다. ;-)
            HttpResponse {
                state: self.state,
                extra: Headers {
                    response_code,
                },
            }
        }
    }

    impl HttpResponse<Headers> {
        fn response_code(&self) -> u8 {
            // 자, 보세요, 응답 코드입니다
            self.extra.response_code
        }
    }

저는 비디오 드라이버를 제공하는 제 [m4vga](https://github.com/cbiffle/m4vga-rs) 크레이트에서 이 변형을 사용합니다. 비디오 드라이버는 얼마나 설정했는지에 따라 여러 상태에 있을 수 있고, 각 상태에서 서로 다른 양의 정보를 저장합니다.

# 결론

타입스테이트 패턴은 Rust에서 자연스럽게 쓰이며, 올바르게 쓰기는 쉽고 잘못 쓰기는 불가능한 API를 설계하게 해 줍니다. 제가 다루지 않은 변형이 더 있을 것이라 확신합니다. 그것들에 대해 듣고 싶으니, 연락 주세요.

또한, Rust 외의 언어에서 이 패턴을 성공적으로 구현한 사례에 대해서도 듣고 싶습니다. 언뜻 보기에는 검사되는 이동 시맨틱(checked move semantics)을 가진 언어가 필요해 보이지만, 우회할 방법을 찾을 수 있으리라 장담합니다.

태그: [#design-patterns](https://cliffle.com/tags/design-patterns/) [#rust](https://cliffle.com/tags/rust/) [#type-systems](https://cliffle.com/tags/type-systems/)

[^1]: "X하는 유일한 방법"처럼 제가 말할 때마다, 암묵적인 단서가 있습니다. "안전한 코드(safe code)를 사용해서"라는 것입니다. *unsafe* 코드를 사용하면, 충분히 노력할 경우 언어의 어떤 불변식이든 위반할 수 있습니다! 여기서 논하는 모든 보장은 안전한 코드에 관한 것입니다.

[^2]: 좋습니다, 엄밀히 말하면 `nullptr`을 역참조하는 것은 C++에서 정의되지 않은 동작(undefined behavior)입니다. 하지만 실제로는, 고대 버전의 Unix나 매우 모호한 임베디드 시스템에 있지 않은 한, 프로그램을 크래시시킵니다.
