# 철도 지향 프로그래밍 (Railway Oriented Programming)

> 원문: [Railway Oriented Programming](https://fsharpforfunandprofit.com/rop/) · [Railway oriented programming (recipe-part2)](https://fsharpforfunandprofit.com/posts/recipe-part2/)
> 저자: Scott Wlaschin | 정리일: 2026-07-10

함수형 프로그래밍에서 에러를 다루는 대표적인 패턴인 철도 지향 프로그래밍(ROP)을 정리한 학습 노트다. 앞부분은 강연 소개 페이지(rop) 내용을, 뒷부분은 본문 격인 recipe-part2 포스트의 전체 흐름을 따라간다.

## 1부. 강연 소개 페이지 (fsharpforfunandprofit.com/rop)

### 무엇을 다루는 강연인가

함수형 프로그래밍 예제 대부분은 "해피 패스"만 가정한다. 하지만 실제 애플리케이션은 검증 실패, 로깅, 네트워크 오류, 예외 같은 비정상 경로를 반드시 처리해야 한다. 이 강연은 이런 에러 처리를 철도 선로 비유로 깔끔하게 구성하는 방법을 보여준다.

강연 영상은 NDC London 2014, NDC Oslo 2014, Functional Programming eXchange 2014 버전이 있고, 슬라이드는 SlideShare와 GitHub(PowerPoint 원본)에 공개되어 있다. 코드 예제로는 일반적인 C# 코드와 ROP 방식 F# 코드를 비교하는 GitHub 저장소, ROP를 라이브러리로 만든 Chessie 프로젝트(NuGet 배포), 예제 웹 서비스 프로젝트, FizzBuzz 구현 예제가 있다.

### 핵심 기법 목록

강연과 포스트에서 다루는 기법은 다음과 같다.

- 문자열 에러 대신 Either 양쪽에 커스텀 에러 타입 사용
- 모나딕 함수 연결을 위한 bind(`>>=`)
- 모나딕 함수끼리의 합성을 위한 Kleisli 합성(`>=>`)
- 일반 함수를 끼워 넣기 위한 map(`fmap`)
- unit 반환 함수를 위한 tee (F#은 IO 모나드를 쓰지 않으므로)
- 예외를 에러로 변환하는 매핑
- 모나딕 함수 두 개를 병렬로 결합하는 `&&&` 연산자
- 도메인 주도 설계를 위한 커스텀 에러 타입
- 로깅, 도메인 이벤트, 보상 트랜잭션으로의 확장

### 모나드와의 관계

이 접근은 Haskell의 Either 타입과 본질적으로 같지만, 저자는 의도적으로 모나드 용어를 앞세우지 않는다. 모나드 튜토리얼이 아니라 에러 처리라는 구체적인 문제를 푸는 데 집중하는 글이기 때문이다. 추상적 이론보다 구체적이고 시각적인 이해에서 출발하는 편이 교육적으로 낫다는 판단이다.

"그냥 Either랑 bind 쓰면 되잖아?"라는 반응에 저자는 빵을 만들려는 사람에게 "밀가루랑 오븐 쓰면 돼"라고 답하는 것과 같다고 비유한다. 재료만 던져주는 게 아니라 실제로 따라 할 수 있는 레시피, 즉 템플릿을 제공하는 게 목표다. 좋은 템플릿의 조건은 이렇다.

- 대부분의 상황에 쓸 수 있을 만큼 범용적일 것
- 일관된 코딩 스타일을 강제할 만큼 제약이 있을 것
- 구현 방법이 사실상 하나로 명확할 것
- 나중에 코드를 보는 사람이 유지보수하기 쉬울 것

F# 특유의 사정도 있다. F#에는 타입 클래스가 없어 모나드 구현을 재사용하기 어렵기 때문에, Rop.fs 라이브러리는 필요한 함수를 처음부터 직접 정의한다. 코드 재사용성은 희생하지만 외부 의존성이 전혀 없다는 장점이 있다.

### 주의: 남용하지 말 것

저자 스스로 "Against Railway-Oriented Programming"이라는 별도 글에서 이 패턴을 극단으로 밀어붙일 때의 문제를 다룬다. ROP는 만능 해법이 아니라 특정 상황(순차적 검증·변환 파이프라인)에 잘 맞는 도구다.

## 2부. 본문: Railway oriented programming (recipe-part2)

### 결과가 둘인 함수 설계하기

유스케이스의 각 단계는 성공할 수도, 실패할 수도 있다. 이를 타입으로 표현하면 성공/실패 두 케이스를 가진 `Result` 타입이 된다.

```fsharp
type Result<'TSuccess,'TFailure> =
    | Success of 'TSuccess
    | Failure of 'TFailure
```

검증 함수를 이 타입으로 작성하면 이렇다.

```fsharp
type Request = {name:string; email:string}

let validateInput input =
   if input.name = "" then Failure "Name must not be blank"
   else if input.email = "" then Failure "Email must not be blank"
   else Success input
```

### 철도 비유

이런 함수는 철도의 분기기(스위치)와 같다. 입력이라는 선로 하나로 들어와서, 성공 선로와 실패 선로 둘 중 하나로 나간다. 이런 함수를 여러 개 이어붙이면 두 갈래 선로가 끝까지 나란히 달리는 형태가 된다. 핵심은 한번 실패 선로로 빠진 데이터는 이후 단계를 전부 건너뛰고 실패 선로를 타고 끝까지 직행한다는 점이다.

### 기본 합성의 문제

일반 함수는 `>>` 연산자로 합성한다. 그런데 분기 함수는 출력이 두 갈래(Result)인데 입력은 한 갈래(일반 값)라서, 출력과 입력의 모양이 안 맞아 그대로 이어붙일 수 없다. 어댑터가 필요하다.

### bind 함수

bind는 한 갈래 입력을 받는 분기 함수를 두 갈래 입력을 받는 함수로 바꿔주는 어댑터다. 입력이 Success면 안의 값을 꺼내 함수에 넘기고, Failure면 함수를 건너뛰고 실패를 그대로 통과시킨다.

```fsharp
let bind switchFunction twoTrackInput =
    match twoTrackInput with
    | Success s -> switchFunction s
    | Failure f -> Failure f
```

타입 시그니처는 `('a -> Result<'b,'c>) -> Result<'a,'c> -> Result<'b,'c>`. 분기 함수를 받아 완전한 두-선로 함수로 바꿔준다.

### 검증 함수 결합

검증 세 개를 각각 작은 함수로 쪼갠 뒤 bind로 이어붙인다.

```fsharp
let validate1 input =
   if input.name = "" then Failure "Name must not be blank"
   else Success input

let validate2 input =
   if input.name.Length > 50 then Failure "Name too long"
   else Success input

let validate3 input =
   if input.email = "" then Failure "Email must not be blank"
   else Success input

let combinedValidation =
    validate1
    >> bind validate2
    >> bind validate3
```

`validate1`은 첫 단계라 일반 입력을 그대로 받으므로 bind가 필요 없고, 이후 단계만 bind로 감싼다. 어느 단계에서든 실패하면 나머지 검증은 실행되지 않고 실패 메시지가 끝까지 전달된다.

### 파이프 스타일의 bind 연산자

bind를 중위 연산자 `>>=`로 정의하면 파이프처럼 자연스럽게 읽힌다.

```fsharp
let (>>=) twoTrackInput switchFunction =
    bind switchFunction twoTrackInput

let combinedValidation x =
    x
    |> validate1
    >>= validate2
    >>= validate3
```

### 분기 함수끼리 직접 합성: `>=>`

bind가 "값을 함수에 먹이는" 방식이라면, 분기 함수 두 개를 곧바로 합성해 새 분기 함수를 만드는 방법도 있다. Kleisli 합성이라 부르는 `>=>` 연산자다.

```fsharp
let (>=>) switch1 switch2 x =
    match switch1 x with
    | Success s -> switch2 s
    | Failure f -> Failure f

let combinedValidation =
    validate1
    >=> validate2
    >=> validate3
```

값 없이 함수만으로 정의할 수 있어 더 깔끔하다. bind는 값 지향, `>=>`는 함수 지향이라는 차이만 있고 서로 정의 가능하다(`>=>`는 `switch1 >> bind switch2`).

### 일반 함수를 선로에 올리기: switch와 map

실패할 일이 없는 일반 함수(예: 이메일 소문자 정규화)도 파이프라인에 끼워 넣고 싶다. 방법은 두 가지다.

분기 함수로 승격시키는 `switch` — 결과를 항상 Success로 감싼다.

```fsharp
let switch f x =
    f x |> Success
```

두-선로 함수로 승격시키는 `map` — 성공 선로에서만 함수를 적용하고 실패는 통과시킨다.

```fsharp
let map oneTrackFunction twoTrackInput =
    match twoTrackInput with
    | Success s -> Success (oneTrackFunction s)
    | Failure f -> Failure f
```

정규화 함수를 파이프라인에 추가한 예시.

```fsharp
let canonicalizeEmail input =
   { input with email = input.email.Trim().ToLower() }

let usecase =
    validate1
    >=> validate2
    >=> validate3
    >=> switch canonicalizeEmail
```

### 막다른 함수: tee

DB 갱신처럼 부수 효과만 있고 의미 있는 반환값이 없는(unit 반환) 함수는 그대로는 합성이 끊긴다. 철도 배관의 T자 이음쇠처럼, 함수를 실행하되 입력을 그대로 흘려보내는 `tee`로 해결한다.

```fsharp
let tee f x =
    f x |> ignore
    x

let updateDatabase input =
   ()

let usecase =
    validate1
    >=> validate2
    >=> validate3
    >=> switch canonicalizeEmail
    >=> switch (tee updateDatabase)
```

### 예외 처리: tryCatch

DB 갱신이 타임아웃 등으로 예외를 던질 수 있다면, 예외를 잡아 실패 선로로 보내는 어댑터를 만든다.

```fsharp
let tryCatch f x =
    try
        f x |> Success
    with
    | ex -> Failure ex.Message

let usecase =
    validate1
    >=> validate2
    >=> validate3
    >=> switch canonicalizeEmail
    >=> tryCatch (tee updateDatabase)
```

### 두 선로를 모두 다루는 함수: doubleMap

로깅처럼 성공 선로와 실패 선로 양쪽에서 각각 다른 일을 해야 하는 경우도 있다. 성공용 함수와 실패용 함수를 하나씩 받는 `doubleMap`을 쓴다.

```fsharp
let doubleMap successFunc failureFunc twoTrackInput =
    match twoTrackInput with
    | Success s -> Success (successFunc s)
    | Failure f -> Failure (failureFunc f)

let log twoTrackInput =
    let success x = printfn "DEBUG. Success: %A" x; x
    let failure x = printfn "ERROR. %A" x; x
    doubleMap success failure twoTrackInput
```

참고로 `map`은 `doubleMap`에 실패용 함수로 `id`를 넘긴 특수한 경우다.

### 두-선로 값 만들기

단순 생성자 두 개도 이름을 붙여두면 편하다.

```fsharp
let succeed x = Success x
let fail x = Failure x
```

### 병렬 검증: plus와 `&&&`

지금까지의 직렬 연결은 첫 실패에서 멈추므로 에러를 하나만 보고한다. 검증을 병렬로 돌려 모든 에러를 한꺼번에 모으고 싶다면, 두 분기 함수를 "더하는" 방법을 정의하면 된다. 성공끼리 합치는 방법과 실패끼리 합치는 방법을 파라미터로 받는다.

```fsharp
let plus addSuccess addFailure switch1 switch2 x =
    match (switch1 x),(switch2 x) with
    | Success s1,Success s2 -> Success (addSuccess s1 s2)
    | Failure f1,Success _  -> Failure f1
    | Success _ ,Failure f2 -> Failure f2
    | Failure f1,Failure f2 -> Failure (addFailure f1 f2)
```

검증에서는 성공 시 어차피 같은 입력이므로 첫 결과를 쓰고, 실패 메시지는 세미콜론으로 이어붙인다.

```fsharp
let (&&&) v1 v2 =
    let addSuccess r1 r2 = r1
    let addFailure s1 s2 = s1 + "; " + s2
    plus addSuccess addFailure v1 v2

let combinedValidation =
    validate1
    &&& validate2
    &&& validate3
```

이제 이름과 이메일이 둘 다 비어 있으면 두 에러가 모두 담긴 실패를 받는다.

### 동적 함수 주입

설정에 따라 파이프라인에 함수를 넣거나 빼고 싶을 때도 있다. 디버그 모드일 때만 로거를 끼우는 예시다. 뺄 때는 `id`(항등 함수)를 대신 넣으면 된다.

```fsharp
type Config = {debug:bool}

let debugLogger twoTrackInput =
    let success x = printfn "DEBUG. Success: %A" x; x
    let failure = id
    doubleMap success failure twoTrackInput

let injectableLogger config =
    if config.debug then debugLogger else id

let usecase config =
    combinedValidation
    >> map canonicalizeEmail
    >> injectableLogger config
```

### 도구 상자 정리

지금까지 만든 어댑터를 표로 정리하면 다음과 같다.

| 함수 | 역할 |
|------|------|
| `succeed` | 성공 두-선로 값 생성 |
| `fail` | 실패 두-선로 값 생성 |
| `bind` | 분기 함수를 두-선로 입력용으로 변환 |
| `>>=` | bind의 중위 연산자 |
| `>>` | 일반 함수 합성 |
| `>=>` | 분기 함수끼리 합성 |
| `switch` | 일반 함수를 분기 함수로 승격 |
| `map` | 일반 함수를 두-선로 함수로 승격 |
| `tee` | 막다른(unit) 함수를 통과형 함수로 변환 |
| `tryCatch` | 예외를 실패로 변환하며 승격 |
| `doubleMap` | 성공/실패 선로를 각각 처리 |
| `plus` | 분기 함수 병렬 결합 |
| `&&&` | 병렬 검증 연산자 |

### 완성된 참조 구현

`either`라는 공통 기반 함수를 두면 나머지 대부분을 그 위에서 간결하게 정의할 수 있다.

```fsharp
type Result<'TSuccess,'TFailure> =
    | Success of 'TSuccess
    | Failure of 'TFailure

let succeed x = Success x
let fail x = Failure x

let either successFunc failureFunc twoTrackInput =
    match twoTrackInput with
    | Success s -> successFunc s
    | Failure f -> failureFunc f

let bind f = either f fail
let (>>=) x f = bind f x
let (>=>) s1 s2 = s1 >> bind s2
let switch f = f >> succeed
let map f = either (f >> succeed) fail
let tee f x = f x; x

let tryCatch f exnHandler x =
    try
        f x |> succeed
    with
    | ex -> exnHandler ex |> fail

let doubleMap successFunc failureFunc =
    either (successFunc >> succeed) (failureFunc >> fail)

let plus addSuccess addFailure switch1 switch2 x =
    match (switch1 x),(switch2 x) with
    | Success s1,Success s2 -> Success (addSuccess s1 s2)
    | Failure f1,Success _  -> Failure f1
    | Success _ ,Failure f2 -> Failure f2
    | Failure f1,Failure f2 -> Failure (addFailure f1 f2)
```

### 제네릭 타입과 타입 안전성

이 함수들이 전부 완전히 제네릭하다는 점이 중요하다. 타입이 충분히 일반적이면 구현 방법이 사실상 하나로 강제된다. 예를 들어 `map`의 타입 시그니처를 만족하는 올바른 구현은 하나뿐이라, 잘못 만들 여지가 거의 없다. 제네릭함이 곧 버그를 줄이는 제약으로 작동한다.

### 사용 지침

- 유스케이스의 기반 데이터 흐름 모델로 두-선로 철도를 쓴다.
- 유스케이스의 각 단계를 하나의 함수로 만든다.
- 연결에는 표준 합성 `>>`를 쓴다.
- 두-선로 입력이 필요한 분기 함수에는 `bind`를 쓴다.
- 일반 함수를 흐름에 끼울 때는 `map`을 쓴다.
- 팀원이 낯설어할 수 있으니 특수 연산자(`>>=`, `>=>` 등) 남용은 피한다.

### 핵심 정리

ROP는 실패가 이후 단계를 자동으로 우회하는, 합성 가능하고 제네릭한 에러 처리 패턴이다. 검증·정규화·저장·로깅 같은 실무 요구사항을 하나의 선형 파이프라인 안에 깔끔하게 담을 수 있고, 각 부품이 작고 독립적이라 테스트와 유지보수가 쉽다.
