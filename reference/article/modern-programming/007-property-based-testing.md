# 프로퍼티 기반 테스팅 입문

> 원문: [An introduction to property-based testing](https://fsharpforfunandprofit.com/posts/property-based-testing/)
> 저자: Scott Wlaschin | 정리일: 2026-07-10

예제 기반 단위 테스트의 한계를 "지옥에서 온 엔터프라이즈 개발자"라는 가상의 악당을 통해 드러내고, 그 해법으로 프로퍼티 기반 테스팅과 FsCheck을 소개하는 글의 학습 노트다.

## 발단: 테스트는 언제 충분한가

글은 TDD를 하는 두 동료의 대화로 시작한다. 한 명이 `add` 함수의 테스트를 작성하면 다른 한 명이 그 테스트만 통과하는 최소한의 구현을 내놓는다. 테스트를 몇 개나 써야 구현이 올바르다고 확신할 수 있을까? 이 질문이 글 전체를 끌고 간다.

## 문제: 악의적 순응 (Malicious Compliance)

여기서 "지옥에서 온 엔터프라이즈 개발자(EDFH, The Enterprise Developer From Hell)"가 등장한다. 번아웃에 빠져 의욕을 잃은 이 개발자는 테스트를 통과시키라는 지시에 문자 그대로만 따른다. 테스트는 통과하지만 기능적으로는 엉터리인 구현을 내놓는 식이다.

`add 1 2 = 3`이라는 테스트가 있으면 EDFH는 이렇게 구현한다.

```fsharp
let add x y =
  if x=1 && y=2 then
    3
  else
    0
```

테스트 케이스를 더 추가하면 EDFH는 조건 분기를 그만큼 늘려서 대응한다. 심지어 "Transformation Priority Premise"(테스트를 통과시키는 최소 변경을 우선하라는 원칙)를 근거로 들며 자기 방식이 정당하다고 우긴다. 하드코딩된 예제 몇 개로는 이런 구현을 걸러낼 수 없다.

## 1차 해법: 무작위 입력

하드코딩을 막는 직관적인 방법은 입력을 무작위로 생성하는 것이다. EDFH가 어떤 값이 들어올지 예측할 수 없게 만든다.

```fsharp
let rand = System.Random()
let randInt() = rand.Next()

[<Test>]
let ``Add two random numbers 100 times, expect their sum``() =
  for _ in [1..100] do
    let x = randInt()
    let y = randInt()
    let expected = x + y
    let actual = add x y
    Assert.AreEqual(expected,actual)
```

그런데 이 테스트에는 치명적인 결함이 있다. 기대값을 `x + y`로 계산한다는 것은 테스트 안에서 구현을 그대로 복제했다는 뜻이다. 검증 대상 로직으로 검증 기준을 만들면 테스트가 아무것도 보장하지 못한다.

## 2차 해법: 프로퍼티로 검증하기

구현을 복제하지 않으면서 `add`를 검증하려면 관점을 바꿔야 한다. 특정 입력-출력 쌍이 아니라, 올바른 덧셈이라면 어떤 입력에서든 반드시 성립해야 하는 보편적 성질(property)을 검사하는 것이다. 세 가지 성질이 나온다.

첫째, 교환법칙 — 파라미터 순서를 바꿔도 결과가 같아야 한다.

```fsharp
let commutativeProperty x y =
  let result1 = add x y
  let result2 = add y x
  result1 = result2

[<Test>]
let addDoesNotDependOnParameterOrder() =
  propertyCheck commutativeProperty
```

둘째, 1을 두 번 더한 결과는 2를 한 번 더한 결과와 같아야 한다.

```fsharp
let add1TwiceIsAdd2Property x _ =
  let result1 = x |> add 1 |> add 1
  let result2 = x |> add 2
  result1 = result2

[<Test>]
let addOneTwiceIsSameAsAddTwo() =
  propertyCheck add1TwiceIsAdd2Property
```

셋째, 항등원 — 0을 더하면 아무 일도 없어야 한다.

```fsharp
let identityProperty x _ =
  let result1 = x |> add 0
  result1 = x

[<Test>]
let addZeroIsSameAsDoingNothing() =
  propertyCheck identityProperty
```

공통 로직은 `propertyCheck` 함수로 뽑아낸다. 무작위 입력 100쌍에 대해 프로퍼티가 참인지 확인하는 구조다.

```fsharp
let propertyCheck property =
  for _ in [1..100] do
    let x = randInt()
    let y = randInt()
    let result = property x y
    Assert.IsTrue(result)
```

어느 프로퍼티도 `+`로 기대값을 계산하지 않는다는 점이 중요하다. 구현을 복제하지 않고도 덧셈다움을 검증한다.

## 프로퍼티는 곧 명세다

이렇게 모은 프로퍼티들은 사실상 명세(specification) 역할을 한다. 매직 넘버 몇 개가 아니라 어떤 입력에서든 성립하는 불변의 관계로 요구사항을 정의하는 것이다. 수학자가 연산을 공리로 정의하는 방식과 같다. 실제로 교환법칙·결합법칙·항등원은 수학에서 덧셈을 특징짓는 성질들이다. 프로퍼티를 뽑아내는 과정 자체가 요구사항에 숨어 있던 가정과 엣지 케이스를 드러낸다.

## FsCheck 도입

직접 만든 `propertyCheck`에는 한계가 많다. int 두 개짜리 프로퍼티에만 쓸 수 있고, 실패 시 어떤 입력이 문제였는지 알려주지 않으며, 엣지 케이스(0, 음수, 극단값)를 체계적으로 시도하지 않는다. 이런 일을 대신해 주는 것이 Haskell QuickCheck을 F#으로 포팅한 FsCheck 프레임워크다.

```fsharp
let add x y = x + y

let commutativeProperty (x,y) =
  let result1 = add x y
  let result2 = add y x
  result1 = result2

Check.Quick commutativeProperty
```

출력은 `Ok, passed 100 tests.` — 무작위 입력 생성, 반복 실행, 타입 추론에 따른 제너레이터 선택을 FsCheck이 알아서 한다.

## 악의적 구현 잡아내기

EDFH가 덧셈 대신 곱셈을 구현했다고 하자. 곱셈도 교환법칙은 만족하므로 교환법칙 프로퍼티만으로는 못 잡는다. 하지만 "1을 두 번 더하기 = 2 더하기" 프로퍼티가 잡아낸다.

```fsharp
let add x y =
  x * y // 악의적 구현: 덧셈 대신 곱셈

let add1TwiceIsAdd2Property x =
  let result1 = x |> add 1 |> add 1
  let result2 = x |> add 2
  result1 = result2

Check.Quick add1TwiceIsAdd2Property
```

FsCheck 출력: `Falsifiable, after 1 test (1 shrink): 1`

여기서 FsCheck의 강력한 기능인 축소(shrinking)가 드러난다. 반례를 찾으면 그대로 보고하는 게 아니라 프로퍼티를 여전히 위반하는 가장 작은 입력까지 줄여서 보여준다. 디버깅할 때 `x = 1`처럼 최소 반례가 주어지면 원인 파악이 훨씬 쉽다.

## EDFH의 반격과 결합법칙

EDFH는 다시 적응한다. 작은 값에서는 올바르게 동작하고 큰 값에서만 틀리는 구현을 내놓는 것이다.

```fsharp
let add x y =
  if (x < 10) || (y < 10) then
    x + y  // 작은 값에서는 올바름
  else
    x * y  // 큰 값에서는 틀림
```

기존 프로퍼티들은 작은 상수(0, 1, 2)만 쓰기 때문에 이 구현을 통과시킬 수 있다. 해법은 임의의 값 세 개를 조합하는 결합법칙 프로퍼티를 추가하는 것이다.

```fsharp
let associativeProperty x y z =
  let result1 = add x (add y z)
  let result2 = add (add x y) z
  result1 = result2

Check.Quick associativeProperty
```

FsCheck 출력: `Falsifiable, after 38 tests (4 shrinks): 8 2 10`

무작위 생성이 결국 경계를 넘는 조합(중간 결과가 10 이상이 되는 경우)을 찾아내고, 축소를 거쳐 작은 반례를 보고한다.

## 결론

프로퍼티 기반 테스팅의 핵심 가치는 이렇다.

- 요구사항을 예제가 아닌 보편적 불변식으로 표현하게 강제한다. 이 과정에서 요구사항에 대한 이해 자체가 깊어진다.
- 개발자가 떠올리지 못한 엣지 케이스를 무작위 생성이 대신 찾아준다.
- 구현을 복제하는 순환 검증의 함정을 피한다.
- 테스트 수는 줄면서 커버리지는 넓어진다. 예제 기반 테스트와 같은 문제를 다루지만 장기적으로 더 엄밀하고 노력이 덜 든다.

어려운 부분은 "이 코드의 프로퍼티가 무엇인가"를 찾는 일인데, 저자는 후속 글(Choosing properties for property-based testing)에서 프로퍼티를 발굴하는 일반 패턴들을 다룬다.
