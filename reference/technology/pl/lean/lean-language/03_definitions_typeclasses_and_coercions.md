# Lean 4 definition, axiom, attribute, typeclass, coercion

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/Definitions/

>

> 원문: https://lean-lang.org/doc/reference/latest/Axioms/

>

> 원문: https://lean-lang.org/doc/reference/latest/Attributes/

>

> 원문: https://lean-lang.org/doc/reference/latest/Type-Classes/

>

> 원문: https://lean-lang.org/doc/reference/latest/Coercions/

- 전역 declaration과 elaborator의 자동 합성 기능 정리

## `def`

`def`는 계산 가능한 값이나 함수에 이름을 붙이는 선언이다. 이름과 universe parameter, explicit 또는 implicit parameter, result type, body로 구성한다. Result type은 생략할 수 있지만 public API에서는 명시하는 편이 좋다.

```lean
def square (n : Nat) : Nat := n * n
```

위의 `square`처럼 선언한 definition은 compiler가 executable code로 만들 수 있다. Pattern clause를 사용하면 equation compiler가 이를 recursor, match, recursive core term으로 변환하며, recursive definition은 termination 검사도 통과해야 한다.

## `theorem`

`theorem`은 proposition type의 proof를 선언한다. Kernel 관점에서는 type과 body를 가진 constant지만, body는 일반 reduction에서 opaque하게 취급된다. 다른 proof에서는 이 theorem을 rewriting이나 `apply`에 사용할 수 있다.

```lean
theorem square_zero : square 0 = 0 := by
  rfl
```

## `opaque`와 `abbrev`

- `opaque`
  - body를 elaboration 이후 unfolding하지 않는 opaque declaration
  - theorem 외 계산되지 않아야 할 구현에 사용
- `abbrev`
  - reducible abbreviation
  - elaborator와 definitional equality가 적극적으로 펼침
  - 새 추상 경계보다 표기 단축에 적합
- `def`
  - semireducible 기본 transparency
- transparency 차이는 typeclass synthesis, unification, reduction 성능에 영향

## pattern definition

```lean
def length : List α → Nat
  | [] => 0
  | _ :: xs => length xs + 1
```

- pattern clause의 constructor coverage 검사
- recursive call termination 분석
- generated equation theorem을 simplifier가 활용 가능
- overlapping pattern과 inaccessible pattern 규칙 확인 필요

## termination

- structural recursion
  - recursive argument의 직접 하위 구조로 호출
- well-founded recursion
  - `termination_by`로 감소 measure 또는 relation 지정
  - `decreasing_by`로 감소 proof 제공
- partial, unsafe definition
  - 논리 reduction에 사용할 수 없는 실행 전용 선택지
- termination은 논리적 일관성과 kernel normalization에 필요함

## `axiom`과 `constant`

`axiom`과 `constant`는 body 없이 type만 environment에 추가하는, 논리적으로 같은 종류의 declaration이다. Axiom을 사용한다는 사실 자체가 모순을 뜻하지는 않으며 이론의 전제를 추가하는 것이다. 다만 서로 양립할 수 없는 axiom을 조합하면 `False`를 증명할 수 있게 된다.

```lean
axiom choiceLike {α : Sort u} : Nonempty α → α

#print axioms choiceLike
```

## `sorry`

`sorry`는 미완성 term을 임시 axiom으로 대체한다. Warning은 출력되지만 file elaboration을 계속할 수 있어 proof를 작성하는 중간 단계에 활용할 수 있다. 이 사용은 `sorryAx` 의존성으로 남으므로 production proof에서는 제거해야 한다. 직접 쓴 `sorry`뿐 아니라 간접 의존성도 `#print axioms theoremName`으로 확인할 수 있다.

## classical reasoning

- `open Classical` 또는 `classical` tactic으로 classical instance 사용
- choice, propositional extensionality, excluded middle 등이 proof에 사용될 수 있음
- classical axiom은 Lean 표준 논리 환경에서 일관되게 제공되지만 constructive computation과 구분 필요
- decidability instance가 필요한 proof에서 local `classical` 사용 가능

## attribute

- declaration에 metadata나 subsystem 동작 연결
- 구문: `@[attr] declaration`
- 기존 declaration에 `attribute [attr] name`
- locality
  - local attribute
  - scoped attribute
  - persistent environment attribute
- 대표 attribute
  - `@[simp]`: simplification rule
  - `@[instance]`: typeclass instance
  - `@[reducible]`, `@[irreducible]`: transparency hint
  - `@[inline]`, `@[implemented_by]`: compiler 동작
  - `@[deprecated]`: migration warning
  - `@[grind]`: `grind` fact, pattern 등록

## custom attribute

Lean metaprogramming API로 새 attribute를 정의하면 declaration name 집합이나 parameterized metadata를 저장하고, command elaboration 시 등록 callback을 실행할 수 있다. 이를 library automation이나 code generation registry에 활용할 수 있으며, 사용자가 효과를 예측할 수 있도록 attribute의 동작과 scope를 문서에 명시해야 한다.

## typeclass

- type-directed interface resolution 기능임
- `class`는 instance search 대상으로 표시된 structure 선언

```lean
class Hash (α : Type u) where
  hash : α → UInt64

def hashTwice [Hash α] (x : α) : UInt64 :=
  Hash.hash x + Hash.hash x
```

위의 square bracket binder `[Hash α]`는 instance implicit parameter다. Elaborator가 expected class type을 기준으로 instance synthesis를 실행하므로 호출자가 instance를 매번 직접 전달하지 않아도 된다. Method projection은 dot notation과 함께 사용할 수도 있다.

## instance

```lean
instance : Hash Nat where
  hash n := UInt64.ofNat n
```

- anonymous, named instance 가능
- parameterized instance로 다른 instance를 조합 가능
- priority로 탐색 순서 조정
- `local instance`로 scope 제한
- `scoped instance`로 명시적으로 scope를 열 때 활성화
- instance chain에 cycle이 생기지 않도록 input이 더 구체적으로 감소하는 방향 설계

## input과 output parameter

- 일반 parameter
  - instance search input과 결과를 모두 제약함
- `outParam`
  - 다른 input에서 instance를 찾은 뒤 결과로 결정되는 parameter
- `semiOutParam`
  - 값이 알려져 있으면 제약에 사용하고 아니면 output처럼 처리
- 여러 가능한 output이 있는 관계를 무리하게 typeclass로 모델링하면 coherence 문제 발생 가능

## default priority와 synthesis

- search는 local instance, registered instance, generated instance 등을 후보로 사용
- metavariable가 너무 많으면 stuck 상태가 될 수 있음
- `#synth Class args`로 독립 진단
- `set_option trace.Meta.synthInstance true`로 search trace 가능
- broad fallback instance는 낮은 priority와 좁은 scope 권장

## coercion

- 실제 type을 기대 type으로 자동 변환하는 elaborator 기능임
- 주요 class
  - `Coe α β`: 일반 coercion
  - `CoeTail α β`: chain 끝에 사용하는 coercion
  - `CoeHTCT α β`: heterogeneous coercion 기반
  - `CoeFun`: 값을 함수처럼 사용
  - `CoeSort`: 값을 type 또는 sort처럼 사용

```lean
structure UserId where
  value : Nat

instance : Coe UserId Nat where
  coe id := id.value
```

이 instance가 있으면 기대 type이 `Nat`인 위치에 `UserId`를 놓았을 때 `value`를 꺼내는 projection이 삽입될 수 있다. Elaborator는 제한된 탐색으로 coercion chain을 구성하므로, 양방향 변환을 많이 등록하면 모호하거나 예상하지 못한 변환이 생길 수 있다.

## numeric literal과 polymorphism

숫자 literal은 `OfNat` typeclass로 해석되며, expected type과 instance가 실제 type을 결정한다. `+`, `*`, 비교 연산도 typeclass 기반 notation인 경우가 많아 주변 type 정보가 해석에 영향을 준다.

```lean
#check (10 : Nat)
#check (10 : Int)
```

- type annotation이 없으면 default priority와 surrounding context로 추론
- overloaded expression 오류 시 중간 type annotation 추가

## typeclass와 coercion 디버깅

- `#check @name`
  - implicit parameter 전체 확인
- `#synth`
  - 필요한 instance 존재 확인
- explicit qualification
  - method name 충돌 제거
- type ascription
  - overloaded literal과 coercion 방향 고정
- trace option
  - 후보 탐색과 실패 원인 확인
- API 설계 단계에서 자동 변환을 줄이는 것이 가장 효과적인 디버깅 방법임
