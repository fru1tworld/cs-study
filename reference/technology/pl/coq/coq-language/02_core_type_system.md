# Rocq 핵심 타입 체계

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/sorts.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/inductive.html#recursive-functions-fix

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/definitions.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/conversion.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/cic.html

이 문서는 Calculus of Inductive Constructions의 sort, 함수, 정의, conversion, typing rule을 정리한다. 표면 구문보다는 kernel이 검사하는 핵심 의미에 초점을 둔다.

## sort

올바르게 타입이 부여된 항은 모두 타입을 가지며, 그 타입 자체도 상위 sort에 속한다.

- 주요 sort
  - `Prop`: 논리 명제
  - `SProp`: 엄격한 proof irrelevance를 제공하는 명제
  - `Set`: 계산 가능한 작은 데이터 타입을 위한 sort
  - `Type@{u}`: universe level `u`의 일반 타입

사용자가 `Type`이라고 표기하면 universe 변수가 자동으로 생성될 수 있다. 이렇게 무한한 universe 계층을 두어 `Type : Type`에서 생기는 모순을 방지한다.

```coq
Check True.      (* Prop *)
Check nat.       (* Set *)
Check Prop.      (* Type *)
Check Type.      (* Type *)
```

기본 출력에서는 universe level이 보이지 않아 `Check Type.`도 `Type : Type`처럼 보인다. `Set Printing Universes.`를 켜면 `Type@{u} : Type@{u+1}`처럼 서로 다른 level이 드러난다.

## `Prop`과 계산

`Prop`의 항은 명제의 증거다. proof irrelevance 관점에서는 같은 명제의 증명들을 계산 결과로 구분할 필요가 없다. 일반적으로 `Prop`의 증거를 분석해 `Set`이나 `Type`의 데이터를 만드는 elimination도 제한된다. 논리적 증거와 계산 데이터를 분리하면 extraction 과정에서 증명을 제거할 수 있기 때문이다. 다만 singleton elimination처럼 안전한 예외는 허용된다.

## 함수 타입과 lambda

- 비의존 함수 타입: `A -> B`
- 의존 함수 타입: `forall x : A, B x`
- lambda abstraction: `fun x : A => t`
- 함수 적용: `f a`

`A -> B`는 결과 타입이 인수에 의존하지 않는 `forall _ : A, B`의 표기다. 아래의 `id_nat`은 이런 비의존 함수 타입을 쓰고, `id`는 먼저 받은 타입 `A`가 뒤의 인수와 결과 타입에 나타나는 의존 함수 타입을 쓴다.

```coq
Definition id_nat : nat -> nat := fun n => n.

Definition id : forall A : Type, A -> A :=
  fun (A : Type) (x : A) => x.
```

`id`의 `fun (A : Type) (x : A)`처럼 binder 여러 개를 묶어 작성할 수 있다. binder를 `{A : Type}`로 쓰면 maximal implicit binder, `[A : Type]`로 쓰면 non-maximal implicit binder가 된다. typeclass를 위한 별도 binder 문법은 없고, implicit binder의 타입이 class면 instance resolution이 그 인수를 채운다. 보통은 `{H : C A}`나 implicit generalization을 쓰는 `` `{C A} `` 형태로 쓴다. binder type을 생략하면 환경과 설정에 따라 추론될 수 있다.

## 가정

- `Axiom name : type`
  - 증명 없이 전역 가정 추가
- `Parameter name : type`
  - module signature나 추상 interface에서 자주 사용함
- `Variable name : type`
  - section의 지역 context에 가정 추가

가정을 추가했을 때 살펴볼 것은 kernel의 신뢰성이 아니라 증명에 사용한 논리적 전제다. 최종 정리가 어떤 가정에 의존하는지는 `Print Assumptions`로 확인할 수 있다.

```coq
Axiom excluded_middle : forall P : Prop, P \/ ~ P.

Print Assumptions excluded_middle.
```

## 지역 정의와 전역 정의

- `let x := t in u`
  - 식 내부의 지역 정의
  - zeta reduction으로 치환됨
- `Definition`
  - 이름, 선택적 인수, 타입, 본문을 가진 전역 상수 생성
- `Let`
  - section 또는 module 안의 지역 상수 선언
- `Example`, `Lemma`, `Theorem`, `Fact`, `Remark`, `Corollary`, `Proposition`
  - kernel 관점에서는 타입과 proof term을 가진 상수
  - 이름은 문서 의도와 관례를 나타냄

```coq
Definition twice (f : nat -> nat) (x : nat) : nat := f (f x).

Theorem twice_id : forall n, twice (fun x => x) n = n.
Proof.
  intros n. reflexivity.
Qed.
```

## opaque와 transparent

- `Qed`
  - 증명을 opaque하게 저장함
  - 일반 reduction에서 본문을 펼치지 않음
- `Defined`
  - 증명을 transparent하게 저장함
  - 계산에서 본문을 펼칠 수 있음
- `Definition`
  - 기본적으로 transparent한 계산 정의

opacity는 타입 검사를 막는 것이 아니라 본문을 자동으로 펼치는 방식과 계산 동작을 제어한다. 따라서 계산에 사용할 증명 기반 프로그램은 본문을 펼칠 수 있도록 `Defined`로 저장해야 할 수 있다.

## conversion rule

두 항이 문법적으로 달라도 계산 규칙으로 같은 normal form에 도달하면 definitionally equal로 취급할 수 있다. type checker는 이 definitional equality를 별도 등식 증명 없이 사용한다. 주요 reduction은 다음과 같다.

- alpha conversion: bound variable 이름 변경
- beta reduction: `(fun x => t) u`에서 `x`를 `u`로 치환
- delta reduction: transparent constant 본문 펼침
- iota reduction: constructor를 대상으로 한 `match` 계산
- zeta reduction: `let` 정의 치환
- eta expansion: 함수 또는 record의 관찰 가능한 형태 확장

아래 예제의 좌변은 beta reduction으로 시작해 계산하면 `3`이 되므로, 별도 등식 없이 `reflexivity`로 증명된다.

```coq
Example beta_example : (fun n => n + 1) 2 = 3.
Proof.
  reflexivity.
Qed.
```

## 명제적 등식과 정의적 등식

정의적 등식(definitional equality)은 kernel의 계산으로 같음을 확인하므로 `reflexivity`가 바로 성공할 수 있다. 명제적 등식(propositional equality)은 `eq` 귀납 타입의 항으로 증명하며, 정리를 이용한 `rewrite`가 필요할 수 있다. 이 차이를 구분해야 `reflexivity`가 실패했을 때 계산만으로 해결되는 문제인지 살펴볼 수 있다.

```coq
Check eq_refl.
Check Nat.add_comm.
```

## typing judgment

typing judgment는 직관적으로 context `Γ`에서 term `t`가 type `T`를 가진다는 판단이다. 여기서 context에는 variable assumption, local definition, universe constraint가 들어간다.

핵심 typing rule은 다음과 같다.

- variable은 context에 선언된 type을 가짐
- lambda는 dependent function type을 만듦
- application은 함수의 domain과 인수 type이 맞아야 함
- conversion 가능한 type은 서로 대체 가능
- constant는 global environment의 선언을 따름

## cumulativity와 subtyping

작은 universe의 타입은 더 큰 universe에서도 사용할 수 있다. 즉, 적절한 제약 아래에서는 `Type@{i}`가 `Type@{j}`에 포함되며, 귀납 타입과 함수 타입에도 cumulativity 규칙이 적용될 수 있다. 선언들을 조합하면서 universe 제약이 누적되므로, 이 제약들이 충돌하면 universe inconsistency가 발생한다.

## proof term 직접 작성

다음은 `P /\ Q -> Q /\ P`의 증명을 tactic 없이 항으로 직접 작성한 예다.

```coq
Definition and_comm_term : forall P Q : Prop, P /\ Q -> Q /\ P :=
  fun (P Q : Prop) (h : P /\ Q) =>
    match h with
    | conj hp hq => conj hq hp
    end.
```

위처럼 항을 직접 작성하면 의존 관계와 계산 구조가 명시적으로 드러난다. tactic을 사용하면 큰 항을 목표 중심으로 조금씩 구성할 수 있지만, 최종적으로는 같은 종류의 proof term이 생성된다. 진행 중이거나 완성된 proof term은 `Show Proof`로 확인할 수 있다.

## 타입 검사에 유용한 명령

```coq
Set Printing All.
Check @eq_refl.
Unset Printing All.

Print and_comm_term.
Print Assumptions and_comm_term.
```

- `@name`: implicit argument 삽입을 끈 형태로 적용
- `Set Printing All`: universe, implicit, coercion 등 생략된 정보 표시

예제처럼 디버깅이 끝나면 `Unset Printing All`로 출력 설정을 되돌리는 것이 좋다.
