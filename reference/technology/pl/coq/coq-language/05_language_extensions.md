# Rocq 언어 확장과 elaboration

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/extensions/index.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/extensions/evars.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/extensions/implicit-arguments.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/extensions/match.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/user-extensions/syntax-extensions.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/type-classes.html

이 문서는 사용자가 작성하는 풍부한 표면 언어가 핵심 CIC term으로 변환되는 과정을 정리한다.

## command 처리 단계

- lexing
  - 문자 stream을 token으로 변환함
- parsing
  - token을 command, term syntax tree로 변환함
- synterp
  - 문법과 command의 해석 준비 단계를 처리함
- interp
  - term elaboration, 환경 조회, 갱신, proof 시작 등을 수행함

한 command가 환경을 바꾸면 바로 다음 command의 parsing과 elaboration에도 영향을 줄 수 있다. 이 때문에 파일 중간에서도 notation이나 tactic grammar를 확장할 수 있다.

## term elaboration

term elaboration은 표면 term에 다음 정보를 보충한다.

- 생략된 type
- implicit argument
- coercion
- notation 해석
- universe instance
- overloaded name

이 과정에서는 기대 타입을 위에서 아래로 전달하고 실제 타입을 아래에서 위로 합성한다. 정보를 모두 보충한 결과가 kernel이 검사하는 fully elaborated term이다.

## existential variable

아직 정해지지 않은 항은 evar 또는 hole로 표현한다. `_`는 elaborator가 해결해야 할 이름 없는 hole이고, 문맥에 따라 `?name`처럼 이름을 붙인 evar도 사용할 수 있다. 이 빈자리는 unification, typeclass resolution, tactic으로 채우며, 해결되지 않은 evar가 남으면 일반 선언을 완료할 수 없다.

```coq
Check @nil _.
Check (fun x : _ => x).
```

tactic 중에는 `eapply`, `econstructor`, `eexists`처럼 하위 목표를 evar로 남긴 채 적용하는 것이 있다. 유연하지만 원인과 멀리 떨어진 곳에서 실패할 수 있으므로 과용하지 않도록 주의한다.

## implicit argument

다른 인수나 기대 타입에서 추론할 수 있는 인수는 implicit argument로 선언해 생략할 수 있다. 다음 예제는 중괄호 binder로 타입 인수 `A`를 implicit으로 선언한다.

```coq
Definition id {A : Type} (x : A) : A := x.

Check id 3.
Check @id nat 3.
```

- `@f`: implicit insertion을 끄고 모든 인수를 명시함
- `Arguments` command로 implicit 여부와 이름 설정 가능
- maximal insertion
  - 뒤에 explicit argument가 없어도 implicit argument 삽입 가능
- strict implicit
  - 추론 근거가 충분한 위치에서만 삽입함
- `Set Implicit Arguments`로 자동 선언 정책 변경 가능

## argument scope와 unfolding

`Arguments`로 각 인수에 notation scope를 연결할 수 있고, simplification에서 argument별 unfolding 힌트를 설정할 수도 있다. argument 이름을 바꾸거나 clear하는 설정도 가능하다. 다만 public API의 argument 이름을 바꾸면 named application을 쓰는 사용자에게 호환성 문제가 생길 수 있다.

## extended pattern matching

- 표면 언어가 제공하는 편의 기능
  - nested pattern
  - 여러 subject 동시 match
  - `if ... then ... else`
  - destructuring `let`
  - alias pattern
  - disjunctive pattern
  - wildcard

elaborator는 이런 편의 구문을 핵심 `match`와 motive로 변환한다. dependent match에서는 return predicate 추론이 중요한데, 추론에 실패하면 `as`, `in`, `return`을 명시해 필요한 정보를 제공한다.

```coq
Definition head_or {A} (d : A) (xs : list A) : A :=
  match xs with
  | nil => d
  | cons x _ => x
  end.
```

## notation

notation은 기존 term을 읽기 쉬운 custom syntax로 연결한다. notation을 정의할 때 다루는 핵심 요소는 다음과 같다.

- precedence level
- associativity
- binder 여부
- parsing pattern
- printing pattern

notation을 선언하는 command는 다음과 같다.

- `Notation`: 일반 표기
- `Infix`: 이항 연산자 표기
- `Reserved Notation`: 사용 전에 parsing 형태 예약

다음 예제는 `Infix`로 `app`에 `++` 표기를 연결하면서 associativity와 level을 함께 지정한다.

```coq
Infix "++" := app (right associativity, at level 60).
```

precedence를 잘못 지정하면 예상과 다른 parse tree가 만들어질 수 있으므로, `Locate "notation"`으로 실제 해석을 확인한다. 별도 grammar가 필요하다면 custom entry로 DSL 수준의 문법을 구성할 수도 있다.

## notation scope

notation scope는 같은 기호의 여러 해석을 분리한다.

- `Declare Scope`, `Delimit Scope`, `Bind Scope`로 정의
- `Open Scope`: 현재 scope에서 기본 활성화
- `%scope`: 특정 term에만 명시
- `Local Open Scope`: 현재 section 또는 module에 제한

```coq
Open Scope nat_scope.
Check (1 + 2)%nat.
```

## coercion

coercion은 기대 type과 실제 type이 다를 때 자동으로 삽입되는 함수로, source class와 target class 사이에 coercion graph를 구성한다. 함수로의 coercion, sort로의 coercion 같은 특수 class도 있다.

coercion을 쓰면 구조체 wrapper를 값처럼 사용하고 수 체계 embedding을 간결하게 표현할 수 있다. 반면 삽입된 함수가 보이지 않아 오류나 성능 문제의 원인을 파악하기 어렵다. 이때는 `Set Printing Coercions`와 `Print Graph` 계열 조회를 활용한다.

## typeclass

typeclass는 record 기반 interface와 자동 instance search를 결합한다. `Class`로 class를 선언하고 `Instance`로 구현을 등록하면, backtracking proof search가 필요한 instance를 조립한다. 다음 예제는 `Eqb` class를 선언하고 `nat`에 대한 instance를 등록한다.

```coq
Class Eqb (A : Type) := {
  eqb : A -> A -> bool
}.

#[export] Instance nat_Eqb : Eqb nat := {
  eqb := Nat.eqb
}.
```

- typeclass binder: `{E : Eqb A}` 또는 backtick 표기
- `Existing Instance`: 기존 상수를 instance로 등록
- `Typeclasses Opaque`와 `Typeclasses Transparent`: search 중 unfolding 제어
- priority로 후보 탐색 순서 조정 가능
- loop와 search explosion을 막기 위해 instance 방향을 신중하게 설계함

## canonical structure

canonical structure는 unification 중 특정 projection 식을 해결할 수 있도록 구조체 값을 등록하는 방식으로, algebraic hierarchy와 notation overloading에 오래 사용되어 왔다. typeclass와 목적은 겹치지만, typeclass가 별도의 proof search를 수행하는 데 비해 canonical structure는 unification에 canonical solution을 제공한다. 같은 hierarchy에서 두 방식을 무계획하게 섞으면 추론 과정을 디버깅하기 어려워진다.

## `Program`

`Program`은 dependent type을 가진 프로그램을 작성할 때 proof obligation을 자동으로 생성한다. refinement type, subset type, well-founded recursion에 유용하며, 생성된 equality와 obligation의 형태는 elaboration 전략의 영향을 받는다. 주요 command는 다음과 같다.

- `Program Definition`
- `Program Fixpoint`
- `Program Lemma`
- `Next Obligation`
- `Solve Obligations`

다음 예제는 `0`인 경우의 결과 자리를 `_`로 비워 두고, 여기서 생긴 obligation을 `Next Obligation`에서 증명한다.

```coq
Program Definition predecessor (n : {n : nat | n > 0}) : nat :=
  match n with
  | exist _ (S k) _ => k
  | exist _ 0 pf => _
  end.
Next Obligation.
  inversion pf.
Qed.
```

## 환경과 파일 command

- `Require`: compiled library load
- `Require Import`: load 후 짧은 이름 활성화
- `Require Export`: downstream import에도 효과 전달
- `Load`: source file의 command를 현재 문맥에서 실행
- `Add LoadPath`: load path 변경 → 현대 프로젝트에서는 `_CoqProject` 또는 build tool 설정 권장
- `.vo`: 완전 컴파일 결과
- `.vos`: opaque proof를 생략한 빠른 interface
- `.vok`: `.vos` 기반 proof 확인 표시

## elaboration 디버깅

elaboration 결과를 확인할 때는 다음 printing 설정으로 생략된 정보를 출력한다.

```coq
Set Printing All.
Set Printing Implicit.
Set Printing Coercions.
Set Printing Universes.
```

- 작은 term에 `Check`와 `About` 적용
- `@`로 implicit insertion 제거
- `%scope`로 notation 해석 고정
- fully qualified name으로 name resolution 고정
- typeclass search trace를 필요한 범위에서만 활성화
- 문제 확인 뒤 printing, debug 설정 복구
