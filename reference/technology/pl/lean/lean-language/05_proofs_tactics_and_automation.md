# Lean 4 proof tactic과 자동화

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/Tactic-Proofs/

>

> 원문: https://lean-lang.org/doc/reference/latest/The-Simplifier/

>

> 원문: https://lean-lang.org/doc/reference/latest/The--grind--tactic/

>

> 원문: https://lean-lang.org/doc/reference/latest/The--mvcgen--tactic/

- tactic proof state, 기본 tactic, simplifier, `grind`, `mvcgen`, custom tactic 정리

## tactic proof

`by` 뒤에 작성한 tactic sequence는 proof term을 생성한다. 각 tactic은 다음 정보를 가진 proof state를 읽고 goal을 처리한다.

- proof state
  - ordered goal 목록
  - 각 goal의 local context
  - 채워야 할 target type
- tactic 결과
  - 실패
  - goal을 새 subgoal로 변환
  - subgoal 없이 goal 완료

모든 goal을 완료해야 declaration을 저장할 수 있다. 다음 예제는 `intro`로 가정을 받은 뒤 `exact`로 목표에 맞는 proof term을 제공한다.

```lean
theorem andComm (p q : Prop) : p ∧ q → q ∧ p := by
  intro h
  exact And.intro h.right h.left
```

## goal 읽기

```text
p q : Prop
h : p ∧ q
⊢ q ∧ p
```

- `⊢` 위: local variable과 hypothesis
- `⊢` 아래: target
- metavariable, instance, let binding이 context에 나타날 수 있음
- goal 이름은 case focusing과 error message에 사용됨

## tactic control

- newline 또는 `;`
  - tactic sequence
- `<;>`
  - 왼쪽 tactic이 만든 모든 subgoal에 오른쪽 tactic 적용
- `all_goals`
  - 모든 goal에 tactic 적용
- `first | t1 | t2`
  - 먼저 성공하는 tactic 선택
- `repeat t`
  - 실패할 때까지 반복
- `try t`
  - 실패해도 proof state 유지하며 성공 처리
- `case`, `next`, bullet
  - 특정 subgoal에 focus

## 기본 tactic

- `exact term`
  - goal type의 term 제공
- `assumption`
  - context에서 goal과 맞는 hypothesis 사용
- `intro x`
  - function, forall, implication binder를 context로 이동
- `apply theorem`
  - theorem conclusion을 goal에 맞추고 premise를 subgoal로 생성
- `refine term_with_holes`
  - hole마다 subgoal 생성
- `constructor`
  - inductive target constructor 적용
- `left`, `right`
  - disjunction branch 선택
- `exists witness`
  - existential witness 제공

## case와 induction

- `cases h`
  - inductive value 또는 proof를 constructor별 분해
- `induction n with`
  - induction principle 적용
- `rcases h with pattern`
  - nested constructor와 existential을 pattern으로 분해
- `obtain pattern := term`
  - 새 fact를 만들며 pattern 분해
- `nomatch h`
  - inhabitant가 불가능한 inductive type 제거
- indexed family에서는 generated equality와 dependent elimination 확인 필요

## equality

- `rfl`
  - definitional equality
- `rw [h]`
  - equality theorem으로 rewriting
- `rw [← h]`
  - 반대 방향
- `calc`
  - 중간 relation chain을 구조적으로 작성
- `congr`
  - congruence로 equality subgoal 생성
- `subst x`
  - equality를 사용해 variable 제거
- `cases h`
  - equality proof를 `rfl` case로 elimination

```lean
example (a b c : Nat) (h₁ : a = b) (h₂ : b = c) : a = c := by
  calc
    a = b := h₁
    _ = c := h₂
```

## assumption과 local definition 관리

- `have h : P := proof`
  - 중간 fact 추가
- `suffices h : P by ...`
  - 충분 조건을 먼저 선언
- `let x := term`
  - local definition
- `clear h`
  - 사용하지 않는 hypothesis 제거
- `revert x`
  - context에서 target binder로 되돌림
- `generalize h : term = x`
  - term을 variable로 일반화하고 equality 보존

## simplifier

> 원문: https://lean-lang.org/doc/reference/latest/The-Simplifier/

Simplifier는 rewrite rule database를 사용해 expression을 안쪽부터 바깥쪽으로 정규화한다. Lean에서 자주 쓰는 automation으로, 대상과 사용할 rule을 다음과 같이 지정할 수 있다.

- `simp`
  - target과 선택한 hypothesis 단순화
- `simp at h ⊢`
  - hypothesis와 target 동시 단순화
- `simpa using proof`
  - proof type과 goal을 각각 단순화한 뒤 정확히 맞춤
- `simp only [rules]`
  - 기본 simp set을 제외하고 명시 rule만 사용

## simp rule

Simp rule은 equality theorem을 normal form 방향으로 사용하며, iff theorem으로는 proposition을 rewriting한다. Proposition의 proof가 있으면 target을 `True` 방향으로 단순화할 수도 있다. Global rule은 `@[simp] theoremName`으로 등록하고, 특정 호출에서만 사용할 definition과 theorem은 `simp [definition, theorem]`처럼 지정한다.

Conditional rule을 적용하려면 premise를 재귀적으로 해결해야 한다. 또한 rule의 방향이 중요하다. 잘못된 양방향 rule을 등록하면 loop가 생기거나 normal form이 불안정해질 수 있다.

## simp set과 설정

- `simp [-rule]`
  - 특정 기본 rule 제외
- `simp (config := { ... })`
  - reduction, contextual reasoning, iteration 등 조정
- named simp extension을 library가 제공할 수 있음
- simplification과 rewriting 차이
  - `rw`: 지정한 rule과 위치를 직접 제어
  - `simp`: database 전체로 canonical form 추구
- proof 안정성이 중요하면 `simp only` 사용 고려

## `grind`

> 원문: https://lean-lang.org/doc/reference/latest/The--grind--tactic/

`grind`는 SMT solver에서 영감을 받아 proof를 생성하는 automation이다. Premise와 conclusion을 함께 처리해 contradiction을 도출하며, 이를 위해 다음 engine들이 협력한다.

- 협력 engine
  - congruence closure
  - constraint propagation
  - guided case analysis
  - E-matching
  - associativity, commutativity reasoning
  - linear integer arithmetic
  - commutative ring, field solver
- 표준 library의 `@[grind]` annotation 활용

```lean
example (a b c : Nat) (h₁ : a = b) (h₂ : b = c) : a = c := by
  grind
```

## `grind` 사용 경계

- 적합한 목표
  - equality propagation
  - algebraic datatype case
  - annotated lemma 기반 first-order reasoning
  - 제한된 산술
- 부적합한 목표
  - combinatorial branch가 폭발하는 SAT encoding
  - 대규모 bit-level 문제
- 순수 Boolean, bitvector 문제는 `bv_decide` 고려
- `grind?` 또는 최소화 기능으로 실제 필요한 fact 확인
- library lemma에는 적절한 `@[grind]` pattern과 direction 설정

## `mvcgen`

> 원문: https://lean-lang.org/doc/reference/latest/The--mvcgen--tactic/

`mvcgen`은 monadic verification condition generator다. Predicate transformer와 specification theorem을 사용해 imperative style의 `do` program에 대한 correctness goal을 작은 verification condition으로 분해한다. 사용하려면 먼저 다음과 같이 준비한다.

```lean
import Std.Tactic.Do
open Std.Do
```

- supported monad에 rule과 specification 등록 가능
- proof mode에서 program statement별 precondition, postcondition 확인
- generated VC는 `simp`, `grind`, 산술 tactic 등으로 해결 가능

## 결정 절차와 automation 선택

- definitional computation: `rfl`, `decide`, `native_decide`
- rewriting과 정규화: `rw`, `simp`
- equality closure와 구조적 자동화: `grind`
- arithmetic: core, Std, Mathlib에서 제공하는 해당 solver
- imperative monadic program: `mvcgen`
- bitvector, Boolean: `bv_decide`
- theorem search와 broad automation은 import한 library에 따라 달라짐

## custom tactic

Custom tactic은 syntax declaration으로 grammar를 추가하고, tactic elaborator에서 이 syntax를 `TacticM` computation으로 변환하는 방식으로 구현한다. Goal API로 target과 context를 조회하거나 metavariable을 assign하고, quotation과 antiquotation으로 term 및 tactic syntax를 생성할 수 있다.

이렇게 만든 tactic도 최종 proof term은 kernel 검사를 받는다. 사용자가 직접 호출할 tactic이라면 error message와 location, deterministic behavior도 고려해야 한다.

## proof 유지보수

- broad automation 하나로 큰 goal 전체를 숨기지 않음
- 의미 있는 중간 lemma와 `calc` chain 사용
- `simp` rule 방향을 canonical form으로 설계
- automation dependency를 import와 attribute에서 명확히 함
- `#print axioms`로 최종 theorem 신뢰 전제 확인
- 성능 문제가 생기면 heartbeat 증가보다 trace, goal 축소를 먼저 수행
