# 타입으로 설계하기 (Designing with Types) 시리즈 정리

> 원문: [Designing with types 시리즈](https://fsharpforfunandprofit.com/series/designing-with-types/)
> 저자: Scott Wlaschin | 정리일: 2026-07-10

이 문서는 원문 전체의 번역이 아니라, 시리즈 8편의 핵심 개념과 설계 기법을 재구성해 정리한 학습 노트다. 코드 예제는 원문의 것을 그대로 실었고, 코드 내 영어 주석만 한국어로 옮겼다. 전체 코드와 상세한 논의는 각 파트의 원문 링크를 참고한다.

## 목차

1. [파트 1: 서론 — 타입을 설계 도구로 쓰기](#파트-1-서론--타입을-설계-도구로-쓰기)
2. [파트 2: 단일 케이스 유니언 타입](#파트-2-단일-케이스-유니언-타입)
3. [파트 3: 잘못된 상태를 표현 불가능하게 만들기](#파트-3-잘못된-상태를-표현-불가능하게-만들기)
4. [파트 4: 새로운 도메인 개념 발견하기](#파트-4-새로운-도메인-개념-발견하기)
5. [파트 5: 상태를 명시적으로 만들기](#파트-5-상태를-명시적으로-만들기)
6. [파트 6: 제약이 있는 문자열](#파트-6-제약이-있는-문자열)
7. [파트 7: 문자열이 아닌 타입](#파트-7-문자열이-아닌-타입)
8. [파트 8: 결론](#파트-8-결론)

---

## 파트 1: 서론 — 타입을 설계 도구로 쓰기

> 원문: [Designing with types: Introduction](https://fsharpforfunandprofit.com/posts/designing-with-types-intro/)

시리즈 전체를 관통하는 주장은 하나다. F#의 타입 시스템은 단순히 컴파일러를 통과시키기 위한 장치가 아니라, 설계 단계에서 도메인의 관계와 제약을 코드에 직접 새겨 넣는 도구라는 것이다. 타입을 잘 쓰면 설계가 투명해지고, 비즈니스 규칙이 문서가 아닌 타입 정의 자체에 드러난다.

### 예제 도메인: Contact 레코드

시리즈 전체에서 연락처(`Contact`)를 다듬어 가는 과정을 예제로 삼는다. 출발점은 흔히 보는 평평한 레코드다. 이름 필드들, 이메일 주소와 검증 여부 플래그, 우편 주소 필드들과 유효성 플래그가 한 레코드에 뒤섞여 있다.

```fsharp
type Contact =
    {
    FirstName: string;
    MiddleInitial: string;
    LastName: string;

    EmailAddress: string;
    // 이메일 주소의 소유가 확인되면 true
    IsEmailVerified: bool;

    Address1: string;
    Address2: string;
    City: string;
    State: string;
    Zip: string;
    // 주소 검증 서비스를 통과하면 true
    IsAddressValid: bool;
    }
```

이 구조의 문제는 서로 무관한 관심사가 한 덩어리에 섞여 있다는 점이다.

### 핵심 지침: 원자적 단위로 묶기

첫 번째 리팩터링 지침은 다음과 같다. **함께 일관성을 유지해야 하는(원자적인) 데이터는 레코드나 튜플로 묶고, 관련 없는 데이터를 불필요하게 한데 묶지 않는다.**

`Contact`에는 세 개의 원자적 묶음이 숨어 있다.

- **개인 이름**: 이름, 중간 이니셜, 성. 항상 함께 다뤄지는 한 단위다.
- **이메일 정보**: 이메일 주소와 검증 플래그. 주소가 바뀌면 플래그도 함께 초기화돼야 하므로 하나의 단위다.
- **우편 주소 정보**: 주소 필드들과 유효성 플래그. 마찬가지로 함께 움직인다.

이를 반영해 타입을 분해한다.

```fsharp
type PersonalName =
    {
    FirstName: string;
    // "option"으로 선택적임을 표시
    MiddleInitial: string option;
    LastName: string;
    }

type EmailContactInfo =
    {
    EmailAddress: string;
    IsEmailVerified: bool;
    }

type PostalAddress =
    {
    Address1: string;
    Address2: string;
    City: string;
    State: string;
    Zip: string;
    }

type PostalContactInfo =
    {
    Address: PostalAddress;
    IsAddressValid: bool;
    }

type Contact =
    {
    Name: PersonalName;
    EmailContactInfo: EmailContactInfo;
    PostalContactInfo: PostalContactInfo;
    }
```

### 이 단계의 요점

- 함수를 하나도 작성하지 않고 타입 분해만으로 도메인 모델이 좋아진다.
- 상호 의존적인 필드를 묶으면 "어떤 필드들이 함께 갱신되는가"가 구조에 드러난다.
- `option` 타입은 "이 값은 없을 수 있다"를 주석이 아니라 타입으로 알려 준다.
- `PostalAddress`처럼 검증 플래그와 분리한 타입은 다른 곳에서도 재사용할 수 있다.
- F#은 타입 정의 문법이 가벼워서 이런 잦은 분해·재구성이 실용적이다.

---

## 파트 2: 단일 케이스 유니언 타입

> 원문: [Designing with types: Single case union types](https://fsharpforfunandprofit.com/posts/designing-with-types-single-case-dus/)

파트 1에서 정리한 `Contact`에도 여전히 문제가 남아 있다. 이메일 주소, 우편번호, 주(State) 코드가 전부 그냥 `string`이다. 서로 의미가 다른 값인데 타입이 같으니, 이메일 자리에 우편번호를 넣어도 컴파일러가 잡지 못한다. 이런 상태를 흔히 "원시 타입 집착(primitive obsession)"이라 부른다.

### 케이스가 하나뿐인 유니언으로 감싸기

해결책은 원시 타입을 의미 있는 타입으로 감싸는 것이다. F#에서는 케이스가 하나뿐인 판별 유니언(discriminated union)이 가장 편한 도구다.

```fsharp
type EmailAddress = EmailAddress of string
type ZipCode = ZipCode of string
type StateCode = StateCode of string
```

레코드로도 감쌀 수 있지만, 단일 케이스 유니언은 생성과 패턴 매칭(해체) 문법이 더 간결해서 래퍼 용도로 우수하다.

```fsharp
type EmailAddress = EmailAddress of string

// 생성자를 함수처럼 사용
"a" |> EmailAddress
["a"; "b"; "c"] |> List.map EmailAddress

// 인라인 해체
let a' = "a" |> EmailAddress
let (EmailAddress a'') = a'

let addresses =
    ["a"; "b"; "c"]
    |> List.map EmailAddress

let addresses' =
    addresses
    |> List.map (fun (EmailAddress e) -> e)
```

### 검증은 생성자에서

감싸는 김에 검증도 생성 시점으로 끌어온다. 검증을 통과한 값만 타입 안으로 들어올 수 있게 하면, 도메인 전체에서 "이 타입의 값은 이미 유효하다"고 믿을 수 있다. 원문은 연속(continuation) 기반 생성자를 기본으로 두고, 그 위에 `Option`을 반환하는 직접 생성자를 얹는다. 호출자는 상황에 따라 예외를 던지거나, 기본값을 쓰거나, 메시지를 로깅하는 식으로 실패 처리 방식을 고를 수 있다.

```fsharp
module EmailAddress =

    type T = EmailAddress of string

    // 연속(continuation)을 받는 생성
    let createWithCont success failure (s:string) =
        if System.Text.RegularExpressions.Regex.IsMatch(s,@"^\S+@\S+\.\S+$")
            then success (EmailAddress s)
            else failure "Email address must contain an @ sign"

    // 직접 생성
    let create s =
        let success e = Some e
        let failure _  = None
        createWithCont success failure s

    // 연속을 받아 풀기
    let apply f (EmailAddress e) = f e

    // 직접 풀기
    let value e = apply id e
```

이 타입들을 적용하면 도메인 타입은 이렇게 바뀐다.

```fsharp
type PersonalName =
    {
    FirstName: string;
    MiddleInitial: string option;
    LastName: string;
    }

type EmailContactInfo =
    {
    EmailAddress: EmailAddress.T;
    IsEmailVerified: bool;
    }

type PostalAddress =
    {
    Address1: string;
    Address2: string;
    City: string;
    State: StateCode.T;
    Zip: ZipCode.T;
    }

type PostalContactInfo =
    {
    Address: PostalAddress;
    IsAddressValid: bool;
    }

type Contact =
    {
    Name: PersonalName;
    EmailContactInfo: EmailContactInfo;
    PostalContactInfo: PostalContactInfo;
    }
```

### 캡슐화와 사용 지점

- 타입과 관련 함수를 모듈로 묶고, 시그니처 파일(.fsi)로 생성자를 감추면 검증을 우회한 직접 생성을 막을 수 있다.
- 감싸기/풀기는 서비스 경계(UI 입력, DB 저장·조회)에서 수행하고, 도메인 로직 안에서는 감싼 값을 통째로 다룬다. 도메인 코드 곳곳에서 풀었다 감쌌다 반복하는 것은 신호가 나쁜 설계다.

### 이 단계의 요점

- 의미가 다른 문자열에 서로 다른 타입을 주면 혼용 실수가 컴파일 에러가 된다.
- 검증은 비즈니스 로직 곳곳에 흩어 놓지 말고 생성자 한 곳에 둔다.
- 단일 케이스 유니언은 정적 타입 언어의 흔한 코드 냄새인 원시 타입 집착을 치료한다.

---

## 파트 3: 잘못된 상태를 표현 불가능하게 만들기

> 원문: [Designing with types: Making illegal states unrepresentable](https://fsharpforfunandprofit.com/posts/designing-with-types-making-illegal-states-unrepresentable/)

시리즈에서 가장 유명한 파트다. 비즈니스 규칙을 런타임 검증이 아니라 타입 구조 자체로 강제해서, 잘못된 상태가 아예 만들어질 수 없게 하자는 내용이다.

### 비즈니스 규칙 예제

새 요구사항: **"연락처에는 이메일 또는 우편 주소가 반드시 있어야 한다."**

유효한 경우는 세 가지다.

1. 이메일만 있음
2. 우편 주소만 있음
3. 둘 다 있음

그리고 금지된 경우가 하나 있다. 둘 다 없는 경우다.

### 잘못된 시도: 둘 다 옵션으로

기존 레코드는 두 필드를 모두 요구하므로 규칙을 표현하지 못한다. 그렇다고 두 필드를 모두 `option`으로 만들면, 금지된 상태(둘 다 `None`)가 오히려 합법적인 값이 된다. 결국 런타임 검증과 문서에 의존하게 되고, 검증을 빠뜨린 코드 경로에서 조용히 규칙이 깨진다.

### 해법: 판별 유니언으로 유효한 경우만 나열

유효한 세 가지 경우를 유니언의 케이스로 직접 나열한다.

```fsharp
type ContactInfo =
    | EmailOnly of EmailContactInfo
    | PostOnly of PostalContactInfo
    | EmailAndPost of EmailContactInfo * PostalContactInfo

type Contact =
    {
    Name: Name;
    ContactInfo: ContactInfo;
    }
```

이제 "둘 다 없는" 네 번째 상태는 문법적으로 만들 수 없다. 규칙 위반이 런타임 버그가 아니라 컴파일 에러가 된다.

### 새 타입으로 작업하기

이메일만으로 연락처를 만드는 생성자와, 우편 주소를 갱신하는 헬퍼는 이렇게 된다. 패턴 매칭 덕분에 기존 상태별로 어떤 전이가 일어나는지가 코드에 전부 드러난다.

```fsharp
let contactFromEmail name emailStr =
    let emailOpt = EmailAddress.create emailStr
    // 이메일이 유효한 경우와 아닌 경우를 처리
    match emailOpt with
    | Some email ->
        let emailContactInfo =
            {EmailAddress=email; IsEmailVerified=false}
        let contactInfo = EmailOnly emailContactInfo
        Some {Name=name; ContactInfo=contactInfo}
    | None -> None

let updatePostalAddress contact newPostalAddress =
    let {Name=name; ContactInfo=contactInfo} = contact
    let newContactInfo =
        match contactInfo with
        | EmailOnly email ->
            EmailAndPost (email,newPostalAddress)
        | PostOnly _ -> // 기존 주소는 무시
            PostOnly newPostalAddress
        | EmailAndPost (email,_) -> // 기존 주소는 무시
            EmailAndPost (email,newPostalAddress)
    // 새 연락처를 생성
    {Name=name; ContactInfo=newContactInfo}
```

### 이 단계의 요점

- **자기 문서화**: 타입 정의를 읽는 것만으로 비즈니스 규칙이 보인다. 별도 문서가 필요 없다.
- **설계에 의한 정확성**: 개발자가 실수로 잘못된 연락처를 만들 수 없다. 컴파일러가 규칙을 집행한다.
- **깨지는 변경이 곧 장점**: 규칙이 바뀌어 유니언 케이스가 바뀌면 관련 코드 전부가 컴파일 에러를 낸다. 고쳐야 할 곳을 컴파일러가 전부 짚어 주므로, 조용한 회귀가 없다.
- 타입이 복잡해 보인다면 그것은 도메인이 원래 그만큼 복잡하기 때문이다. 타입을 단순하게 만든다고 복잡성이 사라지지 않는다. 검증 코드와 문서 어딘가로 숨을 뿐이다.

---

## 파트 4: 새로운 도메인 개념 발견하기

> 원문: [Designing with types: Discovering new concepts](https://fsharpforfunandprofit.com/posts/designing-with-types-discovering-the-domain/)

타입 설계는 단순히 규칙을 코드로 옮기는 작업이 아니다. 설계 과정에서 도메인에 숨어 있던 개념을 발견하게 해 주는 분석 도구이기도 하다.

### 규칙이 확장되면

파트 3의 규칙이 확장된다. **"연락처에는 이메일, 우편 주소, 집 전화, 회사 전화 중 최소 하나가 있어야 한다."**

파트 3 방식대로 유효한 조합을 전부 케이스로 나열하면 2⁴−1 = 15개 케이스가 필요하다. 연락 수단이 하나 늘 때마다 케이스 수가 배로 는다. 명백히 지속 불가능하다.

### 관점 전환: ContactMethod의 발견

조합 폭발은 문제를 잘못 모델링했다는 신호다. 도메인을 다시 들여다보면, 진짜 개념은 "이메일 필드, 주소 필드, 전화 필드가 각각 있다"가 아니라 **"연락처는 연락 수단(contact method)의 목록을 가진다"는 것**이다. 연락 수단이라는 새 개념을 타입으로 끌어올린다.

```fsharp
type ContactMethod =
    | Email of EmailContactInfo
    | PostalAddress of PostalContactInfo
    | HomePhone of PhoneContactInfo
    | WorkPhone of PhoneContactInfo

type Contact =
    {
    Name: PersonalName;
    PrimaryContactMethod: ContactMethod;
    SecondaryContactMethods: ContactMethod list;
    }
```

"최소 하나"라는 규칙은 필수인 기본 연락 수단(`PrimaryContactMethod`) 필드 하나와 나머지 목록으로 표현한다. 조합 폭발이 사라지고, 연락 수단이 늘어도 케이스 하나만 추가하면 된다.

### 부수 효과: 누락 없는 처리

`ContactMethod`가 판별 유니언이므로, 이를 소비하는 코드는 패턴 매칭에서 모든 케이스를 다뤄야 한다. 나중에 새 연락 수단이 추가되면 관련 코드 전부에서 컴파일 경고/에러가 나므로, 새 케이스 처리를 빠뜨릴 수 없다.

### 이 단계의 요점

- 타입 주도 설계는 숨어 있던 도메인 개념을 드러낸다.
- 케이스 조합이 폭발한다면 모델의 축이 잘못됐다는 신호다. 더 깊은 추상을 찾아라.
- 이는 DDD에서 말하는 "더 깊은 통찰을 향한 리팩터링(refactoring toward deeper insight)"과 같은 방향이다.

---

## 파트 5: 상태를 명시적으로 만들기

> 원문: [Designing with types: Making state explicit](https://fsharpforfunandprofit.com/posts/designing-with-types-representing-states/)

불리언 플래그나 enum, 조건문에 암묵적으로 숨어 있는 상태를 유니언 타입으로 명시적인 상태 기계(state machine)로 만들자는 내용이다.

### 상태 기계로 보기

많은 도메인 객체가 사실상 상태 기계다. 서로 배타적인 상태들이 있고, 이벤트에 의해 상태 사이를 전이한다.

- 이메일 주소: 미검증 → (검증 메일 클릭) → 검증됨
- 장바구니: 비어 있음 → 활성 → 결제 완료
- 택배: 미배송 → 배송 중 → 배송 완료

상태마다 허용되는 행동과 붙어 있는 데이터가 다르다는 점이 중요하다.

### 플래그 대신 상태별 케이스

`IsEmailVerified: bool` 같은 플래그 방식의 문제는, 플래그와 데이터 사이의 규칙(예: "검증 일시는 검증된 경우에만 존재한다")이 타입에 드러나지 않는다는 것이다. 상태마다 케이스를 만들고 그 상태에서만 유효한 데이터를 케이스에 붙인다. 상태 전이는 상태 기계 타입을 받아 상태 기계 타입을 돌려주는 이벤트 처리 함수로 표현한다.

```fsharp
module EmailContactInfo =
    open System

    // 자리 표시자
    type EmailAddress = string

    // UnverifiedData = 이메일만
    type UnverifiedData = EmailAddress

    // VerifiedData = 이메일과 검증된 시각
    type VerifiedData = EmailAddress * DateTime

    // 상태 집합
    type T =
        | UnverifiedState of UnverifiedData
        | VerifiedState of VerifiedData

    let create email =
        // 생성 시점에는 미검증
        UnverifiedState email

    // "검증됨" 이벤트 처리
    let verified emailContactInfo dateVerified =
        match emailContactInfo with
        | UnverifiedState email ->
            // 검증된 상태의 새 정보를 생성
            VerifiedState (email, dateVerified)
        | VerifiedState _ ->
            // 무시
            emailContactInfo

    let sendVerificationEmail emailContactInfo =
        match emailContactInfo with
        | UnverifiedState email ->
            // 이메일 발송
            printfn "sending email"
        | VerifiedState _ ->
            // 아무것도 안 함
            ()

    let sendPasswordReset emailContactInfo =
        match emailContactInfo with
        | UnverifiedState email ->
            // 무시
            ()
        | VerifiedState _ ->
            // 재설정 메일 발송
            printfn "sending password reset"
```

패턴 매칭이 모든 상태 처리를 강제하므로, 빠뜨린 상태가 있으면 컴파일러가 알려 준다. "비밀번호 재설정 메일은 검증된 주소로만 보낼 수 있다" 같은 규칙이 `sendPasswordReset`처럼 상태별 분기로 명시되고, 미검증 상태로는 발송이 일어날 수 없다.

### 언제 쓰고 언제 안 쓰나

이 패턴이 잘 맞는 경우: 상태가 도메인에서 중요하고, 전이가 애플리케이션 안에서 일어나며, 규칙이 비교적 고정적일 때. 반대로 상태가 사소하거나(틀려도 결과가 가볍거나), 전이가 도메인 바깥에서 일어난다면 굳이 적용할 필요 없다.

---

## 파트 6: 제약이 있는 문자열

> 원문: [Designing with types: Constrained strings](https://fsharpforfunandprofit.com/posts/designing-with-types-more-semantic-types/)

파트 2에서 이메일처럼 의미가 뚜렷한 문자열을 감쌌다면, 이번에는 "최대 50자" 같은 더 평범한 제약도 타입으로 만들자는 내용이다.

### 문제: string이 숨기는 것들

`string` 타입은 최대 길이, 앞뒤 공백 처리, 허용 문자 집합 같은 제약을 전혀 드러내지 않는다. 그 결과 제약 검사가 엉뚱한 계층(예: DB 저장 시점)에서 일어나고, 계층마다 다르게 잘리거나 조용히 실패한다. 서비스 경계를 넘을 때 양쪽이 서로 다른 가정을 하는 것도 문제다.

### WrappedString 프레임워크

공통 인터페이스(`IWrappedString`)와 재사용 가능한 부품 — 정규화(canonicalization) 함수, 검증 함수, 이들을 조합하는 `create` — 을 한 모듈에 모은다. 그 위에서 `String50`, `String100` 같은 길이 제약 타입을 정의한다.

```fsharp
module WrappedString =

    /// 모든 감싼 문자열이 지원하는 인터페이스
    type IWrappedString =
        abstract Value : string

    /// 감싼 값의 option을 생성
    /// 1) 먼저 입력을 정규화한다
    /// 2) 검증에 성공하면 주어진 생성자를 적용해 Some을 반환
    /// 3) 검증에 실패하면 None을 반환
    /// null 값은 절대 유효하지 않다.
    let create canonicalize isValid ctor (s:string) =
        if s = null
        then None
        else
            let s' = canonicalize s
            if isValid s'
            then Some (ctor s')
            else None

    /// 감싼 값에 주어진 함수를 적용
    let apply f (s:IWrappedString) =
        s.Value |> f

    /// 감싼 값을 꺼내기
    let value s = apply id s

    /// 동등성 검사
    let equals left right =
        (value left) = (value right)

    /// 비교
    let compareTo left right =
        (value left).CompareTo (value right)

    /// 생성 전에 문자열을 표준형으로 만든다
    /// * 모든 공백 문자를 스페이스로 변환
    /// * 양끝을 트림
    let singleLineTrimmed s =
        System.Text.RegularExpressions.Regex.Replace(s,"\s"," ").Trim()

    /// 길이 기반 검증 함수
    let lengthValidator len (s:string) =
        s.Length <= len

    /// 길이 100의 문자열
    type String100 = String100 of string with
        interface IWrappedString with
            member this.Value = let (String100 s) = this in s

    /// 길이 100 문자열의 생성자
    let string100 = create singleLineTrimmed (lengthValidator 100) String100

    /// 길이 50의 문자열
    type String50 = String50 of string with
        interface IWrappedString with
            member this.Value = let (String50 s) = this in s

    /// 길이 50 문자열의 생성자
    let string50 = create singleLineTrimmed (lengthValidator 50) String50
```

`String50`과 `String100`은 서로 다른 타입이므로 실수로 섞어 쓸 수 없고, 생성 시점에 정규화와 길이 검증이 강제된다.

### 도메인 타입을 같은 틀 위에서 재정의

`EmailAddress`, `ZipCode` 같은 도메인 타입도 이 부품들을 조합해 다시 정의한다.

```fsharp
module EmailAddress =

    type T = EmailAddress of string with
        interface WrappedString.IWrappedString with
            member this.Value = let (EmailAddress s) = this in s

    let create =
        let canonicalize = WrappedString.singleLineTrimmed
        let isValid s =
            (WrappedString.lengthValidator 100 s) &&
            System.Text.RegularExpressions.Regex.IsMatch(s,@"^\S+@\S+\.\S+$")
        WrappedString.create canonicalize isValid EmailAddress

    /// 아무 감싼 문자열이나 EmailAddress로 변환
    let convert s = WrappedString.apply create s


module ZipCode =

    type T = ZipCode of string with
        interface WrappedString.IWrappedString with
            member this.Value = let (ZipCode s) = this in s

    let create =
        let canonicalize = WrappedString.singleLineTrimmed
        let isValid s =
            System.Text.RegularExpressions.Regex.IsMatch(s,@"^\d{5}$")
        WrappedString.create canonicalize isValid ZipCode

    /// 아무 감싼 문자열이나 ZipCode로 변환
    let convert s = WrappedString.apply create s
```

`PersonalName`의 각 필드도 `String50` 같은 제약 타입으로 바꾸고, 감싼 문자열을 Map의 키로 쓰기 위한 헬퍼도 원문에서 소개된다.

### 이 단계의 요점

- 타입 주도 설계는 "최대 몇 자인가" 같은 결정을 미루지 않고 설계 시점에 내리게 강제한다.
- 제약이 타입 계약의 일부가 되면, 이 타입을 소비하는 모든 서비스가 같은 가정을 공유한다.
- 같은 패턴을 보안 맥락으로도 확장할 수 있다. HTML 이스케이프가 보장된 문자열, SQL 안전 문자열 같은 타입이다.

---

## 파트 7: 문자열이 아닌 타입

> 원문: [Designing with types: Non-string types](https://fsharpforfunandprofit.com/posts/designing-with-types-non-strings/)

단일 케이스 유니언 기법은 문자열 전용이 아니다. 정수, 날짜 같은 다른 원시 타입에도 똑같이 적용된다.

### 의미가 다른 정수 구분하기

`CustomerId`와 `OrderId`는 둘 다 `int`지만 전혀 다른 개념이다. 각각 타입으로 감싸면 고객 ID 자리에 주문 ID를 넣는 실수가 컴파일 에러가 된다.

```fsharp
type CustomerId = CustomerId of int
type OrderId = OrderId of int

let custId = CustomerId 42
let orderId = OrderId 42

// 컴파일 에러
printfn "cust is equal to order? %b" (custId = orderId)
```

### 의미가 다른 날짜 구분하기

같은 `DateTime`이라도 로컬 시각과 UTC 시각을 타입으로 구분할 수 있다.

```fsharp
type LocalDttm = LocalDttm of System.DateTime
type UtcDttm = UtcDttm of System.DateTime

let SetOrderDate (d:LocalDttm) =
    () // 무언가 수행

let SetAuditTimestamp (d:UtcDttm) =
    () // 무언가 수행
```

### 제약이 있는 날짜와 정수

날짜에 유효 범위를 강제하면 범위 밖 값 문제를 이른 시점에 잡는다.

```fsharp
type SafeDate = SafeDate of System.DateTime

let create dttm =
    let min = new System.DateTime(1980,1,1)
    let max = new System.DateTime(2038,1,1)
    if dttm < min || dttm > max
    then None
    else Some (SafeDate dttm)
```

장바구니 수량 예제가 정수 제약의 효과를 보여 준다. 수량을 그냥 `int`로 두면 1 아래로 감소시키는 버그가 그냥 지나간다.

```fsharp
module ShoppingCartWithBug =

    let mutable itemQty = 1  // 집에서 따라 하지 마세요!

    let incrementClicked() =
        itemQty <- itemQty + 1

    let decrementClicked() =
        itemQty <- itemQty - 1
```

수량을 범위가 제약된 타입으로 만들면 그런 값 자체를 만들 수 없고, 감소가 실패할 수 있다는 사실이 `Option` 반환 타입으로 드러난다.

```fsharp
module ShoppingCartQty =

    type T = ShoppingCartQty of int

    let initialValue = ShoppingCartQty 1

    let create i =
        if (i > 0 && i < 100)
        then Some (ShoppingCartQty i)
        else None

    let increment t = create (t + 1)
    let decrement t = create (t - 1)
```

### 측정 단위(units of measure)와의 비교

F#에는 수치 타입에 단위를 붙이는 측정 단위 기능이 따로 있다. 밀리초와 초를 혼동하는 버그를 이렇게 막을 수 있다.

```fsharp
[<Measure>] type sec
[<Measure>] type ms

let toMs (secs:int<sec>) =
    secs * 1000<ms/sec>

let toSecs (ms:int<ms>) =
    ms / 1000<ms/sec>

let sleep (ms:int<ms>) =
    System.Threading.Thread.Sleep (ms * 1<_>)

let commandTimeout (s:int<sec>) (cmd:System.Data.IDbCommand) =
    cmd.CommandTimeout <- (s * 1<_>)
```

둘의 트레이드오프는 다음과 같다.

- **측정 단위**: 산술 연산이 많을 때 유리하다. 단위가 연산을 그대로 통과하므로 감싸기/풀기 오버헤드가 없다. 대신 캡슐화와 값 제약(범위 검증 등)은 제공하지 못한다.
- **단일 케이스 유니언**: 생성 시 검증과 캡슐화를 제공한다. 비즈니스 규칙을 강제하는 용도로는 이쪽이 낫다.

### 이 단계의 요점

- 원시 타입은 표현 방식(int, DateTime)이 아니라 도메인 의미를 반영해야 한다.
- 타입 정의에 심은 제약은 잘못된 값의 전파를 원천 차단한다.

---

## 파트 8: 결론

> 원문: [Designing with types: Conclusion](https://fsharpforfunandprofit.com/posts/designing-with-types-conclusion/)

### 시리즈 요약

1. 큰 구조를 원자적 구성 요소로 분해하기 (파트 1)
2. 단일 케이스 유니언으로 원시 값에 의미 부여하기 (파트 2)
3. 잘못된 상태를 표현 불가능하게 만들기 (파트 3)
4. 타입 설계를 도메인 분석 도구로 써서 새 개념 발견하기 (파트 4)
5. 플래그/enum 대신 명시적 상태 기계 쓰기 (파트 5)
6. 제약이 있는 문자열 타입 (파트 6)
7. 문자열이 아닌 원시 타입에도 같은 기법 적용하기 (파트 7)

### Before / After

- **처음 코드**: 원시 문자열과 불리언 플래그(`IsEmailVerified`, `IsAddressValid`)로 가득한 평평한 `Contact` 레코드(파트 1 첫 코드). 비즈니스 규칙은 애플리케이션 코드 어딘가에 암묵적으로 존재.
- **최종 코드**: 약 350줄. 재사용 모듈(`WrappedString`, `EmailAddress`, `ZipCode`, `StateCode`)과 도메인 모듈(`PersonalName`, `EmailContactInfo`, `PostalContactInfo`)로 구성. 생성 시점 검증(Option 반환), 이메일/우편 검증 상태 기계, 길이 제약 문자열(`String50`, `String100`), 연락 수단 유니언이 포함된다. 전체 리스팅은 원문에 있다.

### 최종 결론

1. **명시성**: 모든 비즈니스 규칙과 제약이 타입 정의에 보인다. 미묘한 버그가 줄어든다.
2. **컴파일 타임 안전성**: 오류 처리가 생성 시점에 강제되고, 잘못된 상태는 표현 자체가 불가능하다.
3. **정확성**: null 검사 누락, 플래그 갱신 순서, 계층마다 다른 문자열 잘림 같은 버그 부류가 단위 테스트 없이도 통째로 사라진다.
4. **트레이드오프**: 코드가 선불로 상당히 늘어난다. 그 대가로 런타임 신뢰성을 얻는 교환이며, 저자는 도메인이 중요한 코드라면 남는 장사라고 본다.
