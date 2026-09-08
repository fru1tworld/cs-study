# Rocq 자동화와 tactic 언어

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/proofs/automatic-tactics/index.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proofs/automatic-tactics/auto.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/micromega.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/ring.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proof-engine/ltac.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/proof-engine/ltac2.html

- built-in solver, hint database, generalized rewriting, Ltac1, Ltac2 정리

## 자동화 사용 원칙

자동 tactic도 최종적으로 kernel이 검사할 proof term을 생성한다. 이를 이용하면 반복적인 논리와 산술 추론을 줄이고, library 전체에서 공유할 증명 패턴을 표현할 수 있다.

다만 탐색 공간이 커지면 성능이 불안정해지고, 어떤 hint가 증명을 해결했는지 파악하기 어려워진다. 환경이 바뀌면서 탐색 경로가 달라질 수도 있으므로 작은 domain별 hint database를 만들고 탐색 깊이와 사용할 lemma를 제한한다. 자동화 전후의 목표 형태를 단순하게 유지하고 profiling과 trace로 병목을 살피는 것도 도움이 된다.

## `auto`와 `eauto`

- `auto`
  - assumption, introduction, registered hint를 이용한 제한적 proof search
  - existential variable을 공격적으로 만들지 않음
- `eauto`
  - evar를 허용해 더 유연한 search 수행
  - 더 강하지만 search space와 디버깅 비용 증가

두 tactic 모두 인수로 탐색 깊이, 사용할 lemma, hint database를 지정할 수 있다. 다음 예제는 `Nat.le_refl`을 `order` database에 등록하고, `auto with order`로 그 database를 사용해 목표를 해결한다.

```coq
Hint Resolve Nat.le_refl : order.

Goal forall n : nat, n <= n.
Proof.
  auto with order.
Qed.
```

## hint database

주요 hint 종류는 다음과 같다.

- `Hint Resolve`: conclusion이 goal과 맞는 theorem 적용
- `Hint Immediate`: premise를 즉시 해결할 수 있을 때 적용
- `Hint Constructors`: inductive constructor 등록
- `Hint Unfold`: definition unfolding 등록
- `Hint Extern`: goal pattern에 custom tactic 연결

`core` database는 전역 기본 탐색에 사용된다. domain의 경계를 유지하려면 별도 이름의 database를 만들고, locality attribute로 hint가 전파되는 범위를 제어한다. 등록한 hint는 `Remove Hints`로 제거할 수 있으며, priority는 숫자가 낮을수록 먼저 시도된다는 점에 유의한다.

## 논리 solver

- `tauto`
  - intuitionistic propositional logic 중심 결정 절차
- `intuition`
  - propositional 구조를 분해하고 추가 tactic과 조합 가능
- `firstorder`
  - first-order reasoning과 quantifier 처리
- `congruence`
  - equality와 constructor 성질을 이용한 congruence closure
- `easy`
  - 자주 나타나는 단순 목표를 빠르게 해결

classical logic이 필요한 목표라면 관련 axiom이나 library를 명시적으로 import해야 한다.

## 산술 solver

- Micromega 계열
  - `lia`: 자연수, 정수의 linear arithmetic
  - `lra`: 유리수, 실수 등 ordered ring의 linear arithmetic
  - `nia`: nonlinear integer arithmetic
  - `nra`: nonlinear real/rational arithmetic
  - `psatz`: polynomial inequality와 Positivstellensatz 기반 reasoning
  - `zify`: 자연수, 정수 goal 전처리

solver가 지원하는 연산과 domain을 벗어난 목표에는 전처리 lemma가 필요하다. 다음 예제는 `Lia`를 불러온 뒤 자연수의 linear 부등식을 `lia` 한 번으로 해결한다.

```coq
From Stdlib Require Import Lia.

Goal forall x y : nat, x <= y -> x + 2 <= y + 2.
Proof.
  lia.
Qed.
```

## `ring`, `field`, `nsatz`

- `ring`
  - commutative semiring, ring의 polynomial identity를 normal form 비교로 해결
- `ring_simplify`
  - goal과 hypothesis의 polynomial expression 정규화
- `field`
  - rational expression의 denominator를 처리해 field equality 해결
  - denominator nonzero subgoal 생성 가능
- `field_simplify`
  - denominator 제거와 side condition 생성
- `nsatz`
  - integral domain의 polynomial equality, disequality reasoning

다음 예제는 정수 다항식의 전개 항등식을 `ring`으로 해결한다.

```coq
From Stdlib Require Import ZArith.
Open Scope Z_scope.

Goal forall x y : Z, (x + y) * (x + y) = x*x + 2*x*y + y*y.
Proof.
  intros. ring.
Qed.
```

## generalized rewriting

- `autorewrite with db`
  - 등록된 rewrite hint 반복 적용
- `Hint Rewrite theorem : db`
  - 방향이 정해진 rewrite rule 등록
- `setoid_rewrite`
  - equality 외 relation과 morphism을 따라 rewriting
- `rewrite_strat`
  - traversal과 reduction 전략을 세밀하게 지정

rewrite hint를 반복 적용하다 보면 loop에 빠질 수 있으므로, rule은 normal form으로 향하는 방향으로 설계해야 한다.

## Ltac1

Ltac1은 tactic을 조합해 새로운 증명 절차를 만드는 동적 언어다. `Ltac name args := expression.` 형태로 정의하며 다음과 같은 값을 다룬다.

- tactic
- term
- identifier
- integer
- pattern 결과

Ltac1의 언어 의미에서는 backtracking이 핵심이다. 다음 `solve_conjunction`은 goal의 형태에 따라 `split`이나 `constructor`를 고르고, 이를 `repeat`로 반복한다.

```coq
Ltac solve_conjunction :=
  repeat match goal with
  | |- _ /\ _ => split
  | |- True => constructor
  end.
```

## Ltac1 control

- sequence
  - `t1; t2`: `t1`의 모든 subgoal에 `t2` 적용
  - `t1; [t2 | t3]`: branch별 tactic 지정
- choice
  - `first [t1 | t2]`: 먼저 성공한 branch 사용
  - `firstorder`와 다른 별도 tactic임
- repetition
  - `repeat t`: 실패할 때까지 반복, 전체는 성공
  - `do n t`: 정해진 횟수 반복
- progress
  - `progress t`: proof state가 바뀌지 않으면 실패
- failure
  - `fail n "message"`: backtracking level과 메시지 지정
  - `try t`: 실패를 성공으로 바꿈
- timeout
  - `timeout n t`: 실행 시간 제한

## Ltac1 pattern matching

- `match goal with`
  - goal과 context pattern에 따라 tactic 선택
- `lazymatch`
  - 처음 match된 branch로 확정하며, 그 branch의 tactic이 실패해도 다음 branch를 시도하지 않음
- `multimatch`
  - 여러 match 결과의 backtracking 허용
- `context[...]`
  - term 내부 context pattern 탐색
- `constr:(...)`
  - term quotation
- `fresh`
  - 충돌하지 않는 identifier 생성
- `eval reduction in term`
  - tactic language에서 term 계산

## Ltac1 디버깅

- `idtac`: 메시지와 값 출력
- `Set Ltac Debug`: 실행 trace 확인
- `Set Ltac Profiling`: tactic별 시간, 호출 통계 수집
- `Show Ltac Profile`: profiling 결과 출력
- `Timeout`: runaway search 제한

자동화가 느리거나 멈추면 지나치게 넓은 `repeat`, `eauto`, `match goal` pattern부터 점검한다.

## Ltac2

Ltac2는 Ltac1의 후속으로 나온 typed tactic language다. 주요 특징은 다음과 같다.

- 정적 type system
- algebraic data type와 pattern matching
- 명시적인 exception과 control flow
- quotation, antiquotation 기반 metaprogramming
- Rocq 내부 API에 구조화된 접근

`Ltac2 name args := expression.` 형태로 정의하며, tactic value와 일반 계산을 더 분명하게 구분한다. 다음은 Ltac2를 불러온 뒤 message를 출력하는 `hello`를 정의하는 예시다.

```coq
From Ltac2 Require Import Ltac2.

Ltac2 hello () := Message.print (Message.of_string "hello").
```

## Ltac2 effect와 goal

- 표준 I/O와 message API
- fatal error와 catch 가능한 exception
- backtracking effect
- current goal과 hypothesis 접근
- `Control.enter` 등으로 goal별 실행 제어

Ltac2의 typed API는 term, identifier, message를 서로 혼동하는 일을 줄인다.

## quotation과 antiquotation

quotation은 Rocq term이나 pattern syntax를 Ltac2 값으로 가져오고, antiquotation은 Ltac2 변수의 값을 quotation 내부에 삽입한다. quotation mode에는 strict와 non-strict가 있으며, match도 term match, goal match, value match로 구분된다. custom tactic notation을 정의하면 사용자용 syntax를 제공할 수도 있다.

## Ltac1과 Ltac2 연동

Ltac2에서 Ltac1을 호출할 수 있고, Ltac2 tactic을 노출해 Ltac1에서 호출할 수도 있다. 이 연동을 이용하면 기존 자동화를 단계적으로 옮길 수 있으며, 전환할 때는 다음 차이에 주의한다.

- evaluation 시점
- variable binding
- exception과 backtracking 의미
- quotation syntax

새로 만드는 대규모 자동화에는 Ltac2가 더 구조적인 선택이지만, 기존 ecosystem에서는 Ltac1의 비중이 크다.

## 자동화 선택 기준

- 단순 논리: `auto`, `easy`, `intuition`
- first-order quantifier: `firstorder`
- linear arithmetic: `lia`, `lra`
- nonlinear arithmetic: `nia`, `nra`, `psatz`
- polynomial equality: `ring`
- field expression: `field`
- project-specific 반복 proof: hint database와 Ltac2
- 반사 기반 대규모 library: SSReflect와 domain별 solver 고려
