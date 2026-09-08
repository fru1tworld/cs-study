# Lean 4 functor, monad, proposition

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/Functors___-Monads-and--do--Notation/

>

> 원문: https://lean-lang.org/doc/reference/latest/Basic-Propositions/

이 글에서는 effect abstraction과 `do` notation을 살펴본 뒤, 기본 논리 proposition과 proof를 다룬다.

## functor

Functor는 context 안의 값에 pure function을 적용하는 추상화다. 핵심 operation인 `Functor.map`은 `<$>`로 표기하며, structure를 유지하면서 element type을 변환한다.

```lean
#eval (fun n => n + 1) <$> some 4
#eval (fun n => n + 1) <$> [1, 2, 3]
```

- functor law
  - identity 보존
  - composition 보존
- law는 typeclass가 강제하지 않으므로 instance 제공자가 별도 theorem으로 보장

## applicative functor

Applicative functor는 서로 독립적인 effectful computation을 조합한다.

- 핵심 operation
  - `pure`
  - `<*>`
  - `seq`

Monad보다 약한 interface이므로 병렬성과 정적 구조를 보존할 수 있다. `do` notation의 일부도 applicative로 변환될 수 있다.

## monad

다음 computation을 앞 computation의 결과에 따라 선택해야 한다면 monad를 사용한다. Monad는 이런 순차적인 effect를 다루는 추상화다.

- 핵심 operation
  - `pure : α → m α`
  - `bind : m α → (α → m β) → m β`
- 표기: `>>=`
- monad law
  - left identity
  - right identity
  - associativity
- Lean typeclass는 law proof field를 요구하지 않음

```lean
def safeHalf (n : Nat) : Option Nat := do
  guard (n % 2 == 0)
  pure (n / 2)
```

## `do` notation

- monadic bind와 pure를 imperative-looking syntax로 표현함
- 주요 statement
  - `let x := value`
  - `let x ← action`
  - `x ← action`
  - `action`
  - `return value`
  - `if`, `match`, `for`, `while`
- `<-`가 아니라 Unicode `←` 사용
- editor에서 `\l` 또는 `\leftarrow` 입력 지원

## early return

`return`은 현재 `do` block의 나머지 computation을 건너뛰며, compiler가 이를 적절한 control flow로 elaboration한다. 따라서 nested function이나 loop에서는 어느 scope에서 반환하는지 확인해야 한다. Loop 문맥에 따라 `break`와 `continue`도 사용할 수 있다.

## failure와 exception

- `MonadExcept ε m`
  - typed exception throw, catch
- `throw error`
- `try ... catch`
- `Option`은 error detail 없는 failure 표현
- `Except ε`는 pure typed error
- `EIO ε`와 `IO`는 runtime error, external effect와 결합

## state와 reader

- `StateM σ α`
  - state를 명시적으로 threading함
- `ReaderM ρ α`
  - read-only environment 전달
- transformer를 통해 effect 조합 가능
- type alias보다 monad stack의 error, state 순서가 semantics에 영향

## `for`와 iteration

`for x in xs do`는 `ForIn` typeclass를 통해 collection별 iterator와 monad를 결합한다. Loop body는 monadic computation이며, `ForInStep`으로 done과 yield control을 표현한다. 자세한 iterator model은 8번 문서에서 다룬다.

## proposition

> 원문: https://lean-lang.org/doc/reference/latest/Basic-Propositions/

Lean에서는 `Prop`에 속한 type이 proposition이고, 그 type의 inhabitant가 proof다. Proof에는 proof irrelevance가 적용된다. 논리 연결자도 inductive type이나 definition으로 제공하므로 type과 term을 다루던 방식으로 논리식을 구성할 수 있다.

## `True`와 `False`

- `True`
  - constructor `True.intro` 하나를 가진 proposition
- `False`
  - constructor가 없는 proposition
- `False.elim`
  - `False` proof에서 임의 proposition, type 도출

```lean
example : True := True.intro

example (h : False) : 2 = 3 := False.elim h
```

## conjunction

- `p ∧ q`
- constructor: `And.intro : p → q → p ∧ q`
- projection: `h.left`, `h.right`
- tactic
  - 만들기: `constructor`, `exact ⟨hp, hq⟩`
  - 분해: `rcases h with ⟨hp, hq⟩`

## disjunction

- `p ∨ q`
- constructor
  - `Or.inl : p → p ∨ q`
  - `Or.inr : q → p ∨ q`
- 사용하려면 두 constructor case를 모두 처리함

```lean
example (p q : Prop) : p → p ∨ q := fun hp => Or.inl hp
```

## negation

`¬p`는 `p → False`의 notation이므로, negation의 proof는 `p`를 가정했을 때 모순을 만드는 함수다. Constructive logic에서는 `¬¬p → p`를 일반적으로 증명할 수 없지만, classical reasoning을 열면 excluded middle을 이용해 증명할 수 있다.

## implication과 equivalence

- implication `p → q`
  - proof는 `p` proof를 받아 `q` proof를 반환하는 함수
- iff `p ↔ q`
  - forward와 backward implication을 가진 structure
- `Iff.intro`, `h.mp`, `h.mpr` 사용
- `rw [iffTheorem]`과 `simp`가 proposition rewriting에 활용

## quantifier

- universal `∀ x, p x`
  - dependent function type
- existential `∃ x, p x`
  - witness와 proof를 가진 inductive proposition
- existential은 `Prop`이므로 일반 computation data 추출에 제한
- 계산 가능한 witness가 필요하면 subtype, Sigma type 고려

```lean
example : ∃ n : Nat, n + 1 = 3 := by
  refine ⟨2, ?_⟩
  rfl
```

## equality

- `Eq a b`, notation `a = b`
- constructor `rfl`
- equality elimination이 substitution과 rewriting의 기반
- `HEq`
  - type이 다른 두 term 사이 heterogeneous equality
  - dependent type reasoning에서 필요할 수 있음
- function extensionality
  - 모든 input에서 결과가 같으면 함수 equality
- propositional extensionality
  - iff인 proposition을 equality로 연결

## decidability

- `Decidable p`
  - proposition `p` 또는 `¬p`를 계산해 선택할 수 있다는 data
- `if h : p then ... else ...`와 `decide`가 instance 사용
- many basic propositions have synthesized `Decidable` instance
- `classical`은 비구성적 decidability를 local로 제공할 수 있음
- proof computation을 기대하면 constructive instance 사용 필요

## Boolean과 proposition 연결

`Bool`은 계산 data이고 `Prop`은 논리 명제다. Boolean 결과를 명제로 다루려면 `b = true`를 사용하고, decidable proposition을 Boolean으로 계산하려면 `decide p`를 사용한다. 이때 reflection theorem이 Boolean procedure와 logical specification을 연결한다. Automation도 계산된 Boolean 결과의 correctness theorem을 이용해 proof를 생성할 수 있다.

## monadic program 검증

- effect type이 program이 수행할 수 있는 동작을 signature에 표시함
- pure model과 interpreter를 분리하면 theorem 작성이 쉬워짐
- `mvcgen`은 monadic `do` program을 verification condition으로 분해 가능
- exception, state, IO의 specification은 precondition, postcondition, invariant로 구성
