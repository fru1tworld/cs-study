# Rocq module, universe, SProp, rewrite rule

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/sections.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/modules.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/primitive.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/universe-polymorphism.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/sprop.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/rewrite-rules.html

이 문서는 대규모 개발 프로젝트를 구성하고 추상화하는 핵심 언어 기능을 정리한다.

## section

section은 여러 선언이 공통 variable, hypothesis, context를 공유하는 범위로, `Section name`과 `End name`으로 구분한다. section을 닫으면 각 선언이 실제로 의존하는 지역 변수만 parameter로 일반화된다. 이 과정에서 section 자체가 namespace를 만들지는 않는다.

다음 예제에서는 section 안에서 선언한 `A`와 `f`가 `twice`의 parameter로 일반화되는지 `Check @twice`로 확인한다.

```coq
Section Generic.
  Context {A : Type}.
  Variable f : A -> A.

  Definition twice (x : A) : A := f (f x).
End Generic.

Check @twice.
```

`Context`로는 여러 assumption을 한 번에 추가할 수 있고, `Let`은 section 내부의 지역 정의로 사용할 수 있다. `Proof using`은 증명이 어떤 section variable에 의존하는지 명시하고 검사하는 데 쓴다. 예상보다 많은 parameter가 일반화되었다면 선언 본문과 type이 실제로 무엇에 의존하는지 확인해야 한다.

## locality와 section 종료

일반 선언은 section이 끝날 때 일반화되어 전역 환경에 추가되지만, `Local` 선언은 section 밖으로 내보내지 않는다. hint, notation, typeclass instance 같은 부수 효과의 범위는 command마다 다르므로, library에서는 `Local`, `Global`, `Export`를 명시해 import 효과를 예측할 수 있게 하는 것을 권장한다.

## module system

module system은 관련 선언을 namespace와 추상화 경계로 묶는다. 주요 구성은 다음과 같다.

- `Module`: 구현
- `Module Type`: interface 또는 signature
- functor: module을 인수로 받는 module
- `Include`: module 또는 module type의 구성 포함
- `Import`: 짧은 이름과 notation 사용 가능하게 함
- `Export`: 현재 module 사용자에게 import 효과 전달

다음 예제는 `STACK` signature를 정의하고, `list`로 구현한 `ListStack`이 이 signature를 만족하는지 `<:`로 검사한다.

```coq
Module Type STACK.
  Parameter t : Type -> Type.
  Parameter empty : forall A, t A.
  Parameter push : forall A, A -> t A -> t A.
End STACK.

Module ListStack <: STACK.
  Definition t := list.
  Definition empty A := @nil A.
  Definition push A x xs := @cons A x xs.
End ListStack.
```

`<:`는 구현이 signature를 만족하는지 검사만 하고, 결과 module은 구현 그대로 남는다. 그래서 `ListStack.t`가 `list`라는 사실도, signature에 없는 선언도 밖에서 쓸 수 있다. 반면 `Module M : T`는 module을 signature로 sealing한다. 밖에서는 `T`에 선언된 이름만 보이고, `T`에서 `Parameter`로만 선언된 항목은 구현이 있어도 정의가 감춰져 계산으로 펼칠 수 없다.

## functor

functor는 module을 parameter로 받아 새 module을 만든다. 알고리즘이 특정 자료구조 구현 대신 interface에 의존하도록 분리할 때 유용하다.

```coq
Module UseStack (S : STACK).
  Definition singleton {A} (x : A) : S.t A :=
    S.push A x (S.empty A).
End UseStack.
```

`UseStack`은 구체적인 구현 대신 `S`의 `push`와 `empty`를 사용하고, `Module ListUse := UseStack ListStack.`처럼 module을 넘겨 적용한다. functor는 applicative module semantics를 사용하며, qualified name과 module path가 type identity에 영향을 줄 수 있다.

## qualified name과 import

- `M.x`: fully 또는 partially qualified access
- `Import M`: 현재 scope에서 `x` 같은 짧은 이름 허용
- `Export M`: 이후 현재 module을 import한 scope에도 효과 전파

이름이 충돌하면 더 긴 qualification을 사용한다. notation scope는 module import와 별개이므로, 필요한 scope를 따로 열 수 있다.

## primitive object

primitive object는 kernel과 runtime이 직접 지원하는 효율적인 데이터 표현이다. 주요 primitive는 다음과 같다.

- machine integer
- floating-point number
- persistent array
- byte 기반 string

primitive는 일반 귀납적 인코딩보다 효율적으로 계산할 수 있고, 그 연산에도 type checker가 이해하는 reduction rule이 있다. 사용할 때는 특정 backend 표현에 과도하게 의존하기보다 추상 interface를 통하는 편이 유리하다.

## universe polymorphism

universe polymorphism은 하나의 선언을 여러 universe level에 재사용하는 기능이다. monomorphic 선언은 현재 universe context에 고정되지만, polymorphic 선언은 사용할 때마다 새 universe instance를 만들 수 있다. universe polymorphism은 속성이나 설정으로 제어하며, 다음 예제는 `#[universes(polymorphic)]` 속성을 사용한다.

```coq
#[universes(polymorphic)]
Definition pid@{u} (A : Type@{u}) (x : A) := x.
```

예제의 `@{u}`는 universe를 명시하는 문법이다. constraint는 `u < v`, `u <= v`, `u = v`처럼 쓰고, universe 이름은 선언과 scope 규칙을 따른다.

## cumulativity

cumulativity 덕분에 작은 universe의 type을 큰 universe가 요구되는 위치에서 사용할 수 있다. polymorphic inductive type 자체도 `Cumulative Inductive`나 `#[universes(cumulative)]`로 cumulative하게 선언할 수 있다. 이때 같은 inductive type의 두 instance는 universe variance에 따라 convertible하다. noncumulative로 선언하면 모든 universe가 같을 때만 convertible하다.

constraint를 약하게 두면 선언의 재사용성은 높아지지만 추론 결과를 이해하기 어려워질 수 있다. 추론된 universe는 다음 명령으로 출력해 확인한다.

```coq
Set Printing Universes.
Check pid.
Print pid.
Unset Printing Universes.
```

## template polymorphism

template polymorphism은 귀납 타입의 universe를 사용 문맥에 따라 특별하게 일반화하는 기존 메커니즘으로, full universe polymorphism과는 동작과 표현이 다르다. `Set`과 `Type`을 오가는 container 선언에서 이 차이가 드러날 수 있으므로, 새 library에서는 어떤 universe 전략을 의도했는지 문서에 명시할 필요가 있다.

## `SProp`

`SProp`은 strict proposition을 위한 proof-irrelevant sort다. 같은 타입에 속한 모든 항을 definitionally irrelevant하게 다루고, proof equality가 계산이나 dependent typing에 드러나는 것을 강하게 제한한다. 이렇게 타입 체계 차원에서 `Prop`보다 강한 proof irrelevance를 제공한다.

- 사용 사례
  - proof component를 완전히 irrelevant하게 유지하는 구조
  - definitional uniqueness of identity proofs가 필요한 encoding

`SProp`의 elimination과 pattern matching에는 별도 제한이 적용된다. 일반적인 명제 증명에서는 여전히 `Prop`이 기본 선택이다.

## user-defined rewrite rule

user-defined rewrite rule은 symbol과 rewrite rule을 kernel reduction에 추가하는 고급 기능으로, 사용하기 전에 관련 typing flag를 활성화해야 한다. 구성 요소는 다음과 같다.

- rewrite 대상 symbol 선언
- 왼쪽 pattern과 오른쪽 term을 가진 rule 선언
- universe 및 implicit argument 처리

rule은 type preservation을 만족해야 하며, confluence와 termination은 사용자가 신중하게 보장해야 한다. 잘못 설계하면 conversion 판단이 끝나지 않거나 결과를 예측하기 어려워질 수 있다. 따라서 일반적인 정리 재작성에는 `rewrite`, `setoid_rewrite`, hint database를 사용하고, 계산 체계 자체를 확장해야 할 때만 kernel 수준의 rewrite rule을 고려한다.
