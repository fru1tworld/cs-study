# 람다 계산법과 타입

> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/Stlc.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/Types.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/toc.html

## untyped lambda calculus

타입 없는 람다 계산법은 다음 세 요소로 구성된다.

- variable `x`
- abstraction `λx. t`
- application `t₁ t₂`

함수를 정의하고 적용하는 것만으로 계산을 표현한다. abstraction `λx. t`는 매개변수 `x`를 본문 `t` 안에서 바인딩한다.

## application 결합

application은 보통 왼쪽으로 결합한다.

```text
f x y = (f x) y
```

반면 abstraction의 본문은 가능한 한 멀리까지 이어진다.

```text
λx. f x = λx. (f x)
```

표기가 애매하다면 괄호를 넣어 AST가 어떤 모양인지 먼저 확정한다.

## free variable과 bound variable

`λx. t`에서 해당 binder가 묶는 `x`는 bound variable이고, 자신을 묶는 바깥 binder가 없는 변수는 free variable이다. 같은 이름의 binder가 중첩되면 shadowing이 일어날 수 있다. 따라서 치환이 올바른지 판단할 때는 이름뿐 아니라 바인딩 구조를 봐야 한다.

## alpha equivalence

bound variable의 이름만 일관되게 바꾼 term은 같은 구조로 본다.

```text
λx. x  ≡α  λy. y
```

free variable의 이름까지 바꾸면 일반적으로 같은 term이 아니다. 이런 바인딩 구조는 구현에서 이름, de Bruijn index, locally nameless 등으로 표현할 수 있다.

## substitution

`[x := s]t`는 term `t`의 free variable `x`를 `s`로 대체한다는 뜻이다. 바인딩된 `x`는 대체하지 않으며, 새로 넣은 변수도 기존 binder에 묶이지 않게 해야 한다. 필요한 경우에는 binder를 새 이름으로 alpha-renaming한 뒤 치환한다.

## variable capture

```text
[x := y](λy. x)
```

문자열을 그대로 치환해 `λy. y`를 만들면 원래 free였던 `y`가 binder에 묶인다. 이를 피하려면 binder의 이름을 먼저 바꿔 `λz. y`와 같은 결과를 얻어야 한다. proof assistant로 언어를 형식화할 때 바인딩 표현을 신중하게 정하는 이유다.

## beta reduction

```text
(λx. t) s → [x := s]t
```

beta reduction은 함수 적용의 핵심 계산 규칙이다. 다만 인수 `s`를 언제 계산하는지는 평가 전략에 따라 달라진다. call-by-value에서는 인수가 value가 된 뒤 beta reduction을 적용하고, call-by-name에서는 인수를 먼저 계산하지 않은 채 치환한다.

## type

타입은 term을 어떤 종류의 값으로 사용할 수 있는지 나타낸다. Boolean이나 자연수 같은 base type이 있고, 함수를 나타내는 function type은 다음과 같이 쓴다.

```text
T₁ → T₂
```

`T₁ → T₂`는 `T₁` 타입의 입력을 받아 `T₂` 타입의 출력을 만드는 함수의 타입이다.

## typing context

typing context는 변수 이름을 타입에 대응시키는 finite map이며, 보통 `Γ`로 쓴다.

```text
Γ, x : T
```

`Γ, x : T`는 기존 context에 `x : T` 바인딩을 추가한 것이다. 같은 이름이 이미 있다면 어떻게 shadowing하는지와 변수를 조회하는 규칙을 확인해야 한다.

## typing judgment

```text
Γ ⊢ t : T
```

이 judgment는 context `Γ` 아래에서 term `t`가 타입 `T`를 갖는다는 뜻이다. 실행 결과가 아니라 정적 분류를 나타내며, 추론 규칙을 연결해 typing derivation을 구성한다.

## variable rule

```text
x : T ∈ Γ
─────────
Γ ⊢ x : T
```

변수의 타입은 context에서 조회한 결과로 정한다. free variable이 context에 없다면 이 규칙을 적용할 수 없다.

## abstraction rule

```text
Γ, x : T₁ ⊢ t : T₂
────────────────────
Γ ⊢ λx:T₁. t : T₁ → T₂
```

본문을 검사할 때 매개변수의 타입 `T₁`을 context에 추가한다. 그 아래에서 본문의 타입이 `T₂`라면 전체 abstraction의 타입은 `T₁ → T₂`가 된다.

## application rule

```text
Γ ⊢ t₁ : T₁ → T₂    Γ ⊢ t₂ : T₁
──────────────────────────────────
          Γ ⊢ t₁ t₂ : T₂
```

함수의 입력 타입과 인수의 타입이 일치해야 application에 타입을 부여할 수 있다. 이때 결과 타입은 함수의 codomain인 `T₂`다.

## canonical forms

canonical forms는 특정 타입의 closed value가 어떤 구문 형태인지 설명한다. 예를 들어 함수 타입의 closed value는 lambda abstraction이고, Boolean 타입의 closed value는 `true` 또는 `false`다. progress 증명에서는 이를 이용해 타입이 올바른 value의 형태를 좁힌다.

## weakening

weakening은 term에 쓰이지 않는 바인딩을 context에 추가해도 typing이 유지된다는 성질이다. context를 다루는 대표적인 보조 정리로, substitution theorem을 증명하는 바탕이 된다. 바인딩을 어떻게 형식화했는지에 따라 증명 난도는 달라질 수 있다.

## substitution lemma

substitution lemma의 전형적인 형태는 다음과 같다.

```text
Γ, x : U ⊢ t : T
Γ ⊢ v : U
────────────────
Γ ⊢ [x := v]t : T
```

타입이 올바른 value를 같은 타입의 변수 자리에 대입해도 타입이 보존된다는 뜻이다. beta reduction이 치환으로 계산되므로, preservation theorem의 해당 case를 증명할 때 이 보조 정리가 필요하다.

## progress

progress는 빈 context에서 타입이 올바른 term이 value이거나 다음 단계로 진행할 수 있음을 보장한다. 정적 타입 시스템이 타입 오류로 인한 stuck 상태를 막는다는 근거 중 하나지만, 계산이 반드시 종료된다는 뜻은 아니다.

## preservation

preservation은 타입이 올바른 term을 한 단계 계산해도 타입이 유지된다는 성질이다.

```text
⊢ t : T ∧ t → t' → ⊢ t' : T
```

subject reduction이라고도 하며, 증명에는 앞서 본 substitution lemma를 주요 도구로 사용한다.

## type safety

progress와 preservation을 결합하면 타입이 올바른 closed term이 평가 도중 타입 오류로 stuck 상태에 빠지지 않음을 보일 수 있다. 이 보장은 모든 프로그램의 종료나 원하는 비즈니스 성질까지 포함하지 않는다. 타입 안전성을 설명할 때는 해당 타입 시스템이 배제하는 오류의 범위를 정확히 밝혀야 한다.

## polymorphism 예고

다형성은 하나의 term을 여러 타입에서 재사용할 수 있게 한다. 다음과 같은 형태가 있다.

- parametric polymorphism: 타입 변수를 명시적으로 추상화한다.
- ad-hoc polymorphism: typeclass나 overload를 사용한다.
- dependent polymorphism: 값에 따라 타입이 달라진다.

parametric polymorphism과 existential type은 이 기초 과정 이후에 다룰 확장 주제다.

## 자체 점검

- `λx. λy. x`에서 각 variable의 binding 범위 표시
- `[x := y](λy. x)`에서 capture를 피하는 substitution 작성
- `f x y`의 AST 괄호 작성
- application typing rule의 두 premise 설명
- progress가 termination을 보장하지 않는 이유 설명
- preservation proof에서 substitution lemma가 필요한 reduction case 찾기
