# Rocq 귀납 타입과 재귀

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/variants.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/records.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/inductive.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/coinductive.html

이 문서는 variant, record, inductive family, pattern matching, recursion, coinduction을 정리한다.

## 단순 귀납 타입

`Inductive`는 유한한 constructor 집합으로 타입을 정의한다. 이 constructor들이 해당 타입의 값을 만드는 유일한 기본 방법이 된다.

- 선언과 함께 생성되는 요소
  - inductive constant
  - constructor constant
  - elimination 또는 induction principle

다음 `color`는 인수 없는 constructor 세 개로 이루어진 타입이며, `Check color_ind`로 선언과 함께 생성된 induction principle을 확인한다.

```coq
Inductive color : Type :=
  | red
  | green
  | blue.

Check color_ind.
```

constructor는 인수를 가질 수도 있다. 아래 `nat_tree`의 `leaf`는 `nat` 하나를, `node`는 `nat_tree` 두 개를 받는다.

```coq
Inductive nat_tree : Type :=
  | leaf : nat -> nat_tree
  | node : nat_tree -> nat_tree -> nat_tree.
```

## parameter와 index

parameter는 inductive family 전체에서 고정되며, 각 constructor의 결과에도 같은 값으로 나타난다. 반면 index는 constructor마다 달라질 수 있어 값의 구조 정보를 타입에 반영한다. 아래에서는 원소 타입 `A`를 parameter로 두고, `vector`에 길이 index를 추가한다.

```coq
Inductive list (A : Type) : Type :=
  | nil : list A
  | cons : A -> list A -> list A.

Inductive vector (A : Type) : nat -> Type :=
  | vnil : vector A 0
  | vcons : forall n, A -> vector A n -> vector A (S n).
```

위 예제에서 `A`는 parameter이고, `vector`의 길이를 나타내는 `nat`은 index다. 이런 indexed family를 match하면 각 branch에서 index에 관한 정보가 정제된다.

## 귀납적 명제

결과 sort가 `Prop`인 귀납 타입에서는 constructor가 명제의 증명 규칙 역할을 한다. 이를 이용해 관계나 불변식, 실행 의미론을 귀납적 명제로 표현할 수 있다.

```coq
Inductive even : nat -> Prop :=
  | even_O : even 0
  | even_SS : forall n, even n -> even (S (S n)).
```

`even 4`의 증명은 위 constructor들을 조합해 구성한다. 반대로 case analysis는 증명을 만들 수 있는 constructor를 분석하며, induction은 그 구조에 맞는 귀납 가설을 제공한다.

## positivity condition

귀납 타입이 자기 자신을 참조할 때는 strictly positive 위치에 나타나야 한다. 정의 중인 타입이 함수 인수의 음의 위치에 나타나는 형태는 금지된다. 이 positivity 검사는 논리적 일관성과 귀납 원리의 정당성을 지키는 데 필요하며, 중첩되거나 상호 참조하는 귀납 정의에는 더 복잡한 검사가 적용된다.

## mutual inductive type

서로를 참조하는 타입은 `with`로 묶어 함께 정의할 수 있다. 이때 parameter는 모든 타입에서 같아야 하고, positivity condition은 블록 전체의 constructor에 함께 적용된다. 아래 예제에서 `tree`는 `forest`를, `forest`는 다시 `tree`를 참조한다.

```coq
Inductive tree : Type :=
  | tnode : forest -> tree
with forest : Type :=
  | fnil : forest
  | fcons : tree -> forest -> forest.
```

## pattern matching

`match subject with ... end`는 constructor별 계산을 정의하며, 모든 constructor를 다뤄야 한다. branch의 결과 타입이 subject나 index에 의존한다면 return clause로 결과 타입을 명시해야 할 수 있다.

```coq
Definition color_code (c : color) : nat :=
  match c with
  | red => 0
  | green => 1
  | blue => 2
  end.
```

- dependent match의 핵심 요소
  - `as`: match 대상에 이름 부여
  - `in`: inductive family와 index pattern 지정
  - `return`: branch 전체가 만족할 motive 지정

표면 언어에서 쓰는 nested pattern은 elaboration 과정에서 핵심 match로 변환된다.

## elimination restriction

inductive object의 sort에 따라 제거할 수 있는 목표 sort가 달라진다. `Set`이나 `Type`의 데이터는 일반적으로 계산 데이터로 제거할 수 있지만, `Prop`의 일반 증명을 분석해 informative data를 만드는 작업은 제한된다. 다만 empty proposition이나 singleton proposition에는 제한된 strong elimination이 허용될 수 있다.

## record

`Record`와 `Structure`는 하나의 constructor와 이름 있는 필드를 가진 귀납 타입을 정의한다. 선언하면 각 필드에 접근하는 projection도 자동으로 생성된다.

```coq
Record point : Type := {
  px : nat;
  py : nat
}.

Definition origin : point := {| px := 0; py := 0 |}.
Compute px origin.
```

record에는 dependent field도 둘 수 있다. primitive projection을 사용하면 projection reduction과 module 활용 방식이 달라질 수 있다. record update는 기본 핵심 구문이 아니므로, 필드를 바꾼 값이 필요하면 새 record를 구성하거나 library notation을 사용한다.

## 구조적 재귀

`Fixpoint`로 재귀 함수를 정의할 때는 종료를 보장하기 위해 지정된 structural argument가 매 호출에서 작아져야 한다. 이 guard condition을 만족하지 못하면 선언이 거부된다.

```coq
Fixpoint length {A : Type} (xs : list A) : nat :=
  match xs with
  | nil => 0
  | cons _ tail => S (length tail)
  end.
```

`length`는 pattern matching으로 얻은 직접 하위 구조 `tail`에 대해 재귀 호출하며, 이것이 구조적 재귀의 기본 형태다. structural argument는 `{struct xs}`로 명시할 수도 있다. termination 증명이 필요한 일반 재귀에는 well-founded recursion이나 `Program Fixpoint`를 활용한다.

## 상호 재귀

mutual inductive type과 마찬가지로 여러 `Fixpoint`도 `with`로 묶어 함께 정의할 수 있다. 이때 각 호출은 허용된 structural decrease를 만족해야 한다. 생성된 함수의 계산 규칙은 각 match branch에 적용된다.

## induction principle

inductive 선언은 기본 induction principle을 자동 생성한다. 이름에는 보통 목표 sort에 따라 다음 접미사가 붙는다.

- `_ind`: `Prop` 목표
- `_rec`: `Set` 목표
- `_rect`: `Type` 목표

`Scheme`으로는 별도의 원리를 생성할 수 있고, mutual type에는 `Combined Scheme`으로 결합 원리를 만들 수 있다.

```coq
Check nat_ind.
Check list_ind.
```

## coinductive type

coinductive type은 잠재적으로 무한한 관찰 구조를 표현한다. `CoInductive`로 타입을 선언하고 `CoFixpoint`로 값을 생성하며, 종료성 대신 productivity를 요구한다. 즉, 유한한 계산 뒤에는 constructor 하나를 관찰할 수 있어야 하므로 corecursive call은 constructor 아래의 guarded 위치에 있어야 한다.

```coq
CoInductive stream (A : Type) : Type :=
  | Cons : A -> stream A -> stream A.

CoFixpoint repeat {A : Type} (x : A) : stream A :=
  Cons A x (repeat x).
```

coinductive value는 무제한으로 완전 정규화할 수 없으므로 projection이나 한 단계 match를 통해 유한하게 관찰한다. 이런 값의 등식을 증명할 때는 bisimulation 패턴이 중요하다.

## `destruct`와 `induction` 선택

constructor별 경우만 나누면 충분할 때는 `destruct x`를 사용한다. `destruct`는 recursive hypothesis를 만들지 않으므로, 재귀적 하위 구조에 대한 귀납 가설이 필요하면 `induction x`를 선택한다. indexed family의 index 등식까지 처리해야 한다면 `inversion`이나 `dependent destruction`이 유용하다.

## 흔한 실패 원인

- recursive call의 인수가 문법적으로 더 작지 않음
- dependent match의 return type을 추론하지 못함
- `Prop`에서 `Type`으로 허용되지 않는 elimination 시도
- constructor parameter와 index 순서를 혼동함
- induction 전에 변수를 너무 일찍 고정해 귀납 가설이 약해짐
