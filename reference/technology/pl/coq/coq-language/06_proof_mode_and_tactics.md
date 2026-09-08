# Rocq proof mode와 기본 tactic

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/proofs/writing-proofs/index.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proofs/writing-proofs/proof-mode.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proof-engine/tactics.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proofs/writing-proofs/equality.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proofs/writing-proofs/reasoning-inductives.html

이 문서는 proof state 관리, 핵심 tactic, equality, inductive reasoning, SSReflect 개요를 정리한다.

## proof mode

proof mode에는 theorem statement 뒤에 `Proof`를 써서 들어간다. proof state는 다음으로 구성된다.

- local context: 변수, 가정, 지역 정의
- goal: 채워야 할 type
- 여러 focused, unfocused, shelved goal

tactic은 주어진 목표를 proof term의 조각과 새로운 하위 목표로 바꾼다. 이렇게 생긴 하위 목표를 모두 해결하면 proof를 완료할 수 있다. 다음 예제에서는 `destruct`로 가정을 나누고, `split`이 만든 두 하위 목표를 bullet으로 각각 해결한다.

```coq
Theorem and_comm : forall P Q : Prop, P /\ Q -> Q /\ P.
Proof.
  intros P Q H.
  destruct H as [HP HQ].
  split.
  - exact HQ.
  - exact HP.
Qed.
```

## proof 종료

- `Qed`
  - proof term 검사 후 opaque theorem 저장
- `Defined`
  - transparent definition으로 저장
- `Admitted`
  - proof 없이 axiom으로 선언 → 미완성 작업 표시
- `Abort`
  - 진행 중 proof 폐기
- `Save name`
  - 지정한 이름으로 저장하는 형태

`Admitted`로 종료한 proof는 증명을 완료한 것이 아니다. 배포 전에는 `Print Assumptions`와 source 검색으로 `Admitted`, `admit`, 추가 axiom이 남아 있는지 확인해야 한다.

## goal focusing

- bullet `-`, `+`, `*`
  - 분기 구조를 명시함
  - 잘못된 nesting과 미완료 branch를 감지함
- braces `{ ... }`
  - subgoal 범위를 명시적으로 묶음
- `Focus`, `Unfocus`
  - 특정 goal에 집중하는 저수준 제어
- `shelve`
  - goal을 나중으로 미룸
- `Unshelve`
  - shelved goal을 다시 활성화함
- `all:` selector
  - 모든 goal에 tactic 적용

## exact와 refinement

- `exact term`
  - term type이 goal과 맞으면 즉시 해결
- `assumption`
  - context에서 goal과 맞는 가정 검색
- `refine term_with_holes`
  - hole마다 새 subgoal 생성
- `apply theorem`
  - theorem 결론을 goal과 맞추고 premise를 subgoal로 생성
- `eapply`
  - 일부 인수를 evar로 남긴 채 적용

```coq
Goal forall P Q : Prop, P -> (P -> Q) -> Q.
Proof.
  intros P Q HP HPQ.
  apply HPQ.
  exact HP.
Qed.
```

`apply HPQ`는 `HPQ`의 결론 `Q`를 goal과 맞추고 premise `P`를 새 goal로 남기며, 이 goal은 `exact HP`로 해결된다.

## context 관리

- `intro x`, `intros`
  - `forall` binder와 implication premise를 context로 이동
- `revert x`
  - context 항을 다시 goal의 binder로 이동
- `clear H`
  - 의존하지 않는 가정 제거
- `clearbody x`
  - local definition의 body 제거하고 assumption으로 변경
- `rename x into y`
  - local name 변경
- `pose proof t as H`
  - theorem instance를 context에 추가
- `specialize (H args)`
  - quantified hypothesis에 인수 적용
- `assert (P) as H`
  - 중간 명제를 별도 subgoal로 증명
- `cut P`
  - 중간 명제를 implication 형태로 삽입

## 논리 connective

- conjunction `P /\ Q`
  - `split`, `constructor`
  - 가설 분석은 `destruct H as [HP HQ]`
- disjunction `P \/ Q`
  - `left`, `right`
  - 가설 분석은 `destruct H as [HP | HQ]`
- existential `exists x, P x`
  - `exists witness`
  - 가설 분석은 `destruct H as [x Hx]`
- false
  - `exfalso`로 goal을 `False`로 변경
  - `contradiction`으로 명백한 모순 탐색

## constructor와 case analysis

- `constructor`
  - goal inductive type의 constructor 적용
- `split`
  - 두 premise constructor에 특화된 형태
- `destruct x`
  - constructor별 case 생성
- `case_eq expression`
  - expression의 case와 equality 보존
- `remember term as x eqn:H`
  - 복잡한 term을 이름과 equality로 고정

dependent index가 있는 경우에는 단순히 `destruct`를 적용하면 정보가 사라질 수 있다.

## induction

- `induction x`
  - x의 induction principle 적용
- `induction x using principle`
  - 사용자 지정 principle 사용
- `dependent induction`
  - dependent equality를 다루는 강화된 induction
- `functional induction`
  - 함수 정의에서 생성된 principle 사용

induction 전에 다른 변수를 고정하거나 명제 자체가 필요한 일반성을 갖추지 못하면 귀납 가설이 너무 약해질 수 있다. 이때는 `revert`로 변수를 다시 일반화한 뒤 induction을 적용하거나, 더 일반적인 lemma를 먼저 증명한다. 문제에 맞는 induction principle을 별도로 만드는 방법도 있다.

## inversion

- `inversion H`
  - inductive hypothesis를 만들 수 있는 constructor를 역으로 분석
  - 불가능한 constructor 제거
  - index equality와 constructor injectivity 추출
- `inversion_clear H`
  - inversion 뒤 원래 가설 제거
- `dependent destruction H`
  - dependent equality 처리를 단순화할 수 있음

큰 가설에 inversion을 반복 적용하면 proof term과 context도 커질 수 있다. 따라서 증명에 필요한 정보만 추출하는 편이 좋다.

## equality tactic

- `reflexivity`
  - definitional equality로 goal 해결
- `symmetry`
  - equality 방향 전환
- `transitivity middle`
  - 두 equality goal로 분할
- `rewrite H`
  - 왼쪽에서 오른쪽으로 재작성
- `rewrite <- H`
  - 반대 방향 재작성
- `replace old with new`
  - 교체와 equality 증명 분리
- `subst x`
  - equality로 variable 치환 후 제거
- `f_equal`
  - 같은 함수 적용 결과의 equality를 인수 equality로 축소
- `injection H`
  - 같은 constructor의 equality에서 인수 equality 추출
- `discriminate H`
  - 서로 다른 constructor equality에서 모순 도출

## setoid rewriting

setoid rewriting은 일반적인 등식뿐 아니라 동치 관계를 이용한 재작성을 지원한다. `Add Parametric Relation`과 `Add Parametric Morphism`으로 관계와 congruence를 등록하면, `setoid_rewrite`가 등록된 morphism을 따라 재작성한다. 함수 extensionality나 사용자 정의 동치 관계를 바탕으로 증명할 때 유용하다.

## reduction과 계산 tactic

- `simpl`
  - 안전한 unfolding과 계산 중심 단순화
- `cbn`
  - call-by-name 전략 reduction
- `cbv`
  - call-by-value 전략 reduction
- `lazy`
  - lazy evaluation 기반 reduction
- `unfold name`
  - 지정한 definition만 펼침
- `fold term`
  - 펼쳐진 term을 definition 형태로 되돌림
- `change new_goal`
  - definitionally equal한 goal로 교체
- `vm_compute`
  - virtual machine으로 빠르게 계산
- `native_compute`
  - native compilation 기반 계산
- 빠른 계산 tactic도 proof-producing 또는 kernel-checkable 결과로 goal 해결

## proof 유지보수

긴 tactic chain은 의미 있는 중간 lemma로 나누고, 목표가 분기되는 곳에는 bullet을 사용한다. 이름은 자동 생성된 값에 의존하지 않도록 intro pattern으로 명시한다.

library 변경의 영향을 줄이려면 내부 구현을 과도하게 `unfold`하기보다 공개된 theorem과 notation을 사용한다. 증명 상태는 `Show Proof`, `Show Existentials`, `Show Conjectures`로 살펴볼 수 있다.

## SSReflect 개요

> 원문: https://rocq-prover.org/doc/v9.1/refman/proof-engine/ssreflect-proof-language.html

SSReflect는 Mathematical Components에서 발전한 대안 proof language로, 작은 규모의 reflection과 예측 가능한 proof script를 중시한다.

- 대표 요소
  - `move`와 intro pattern
  - `case`, `elim`, `apply`
  - compact `rewrite`
  - `have`, `suff`, `wlog`
  - view와 `reflect` predicate

SSReflect는 standard tactic과 섞어 쓸 수 있지만, 이때 proof style의 일관성을 유지하는 것이 중요하다.
