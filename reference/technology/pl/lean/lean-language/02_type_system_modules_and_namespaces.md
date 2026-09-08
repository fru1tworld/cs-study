# Lean 4 타입 체계, module, namespace

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/The-Type-System/

>

> 원문: https://lean-lang.org/doc/reference/latest/Source-Files-and-Modules/

>

> 원문: https://lean-lang.org/doc/reference/latest/Namespaces-and-Sections/

- dependent type theory, universe, definitional equality, source module, namespace, section 정리

## proposition as type

Lean에서 proposition은 `Prop`에 속하는 type이고, proof는 그 proposition type의 term이다. Theorem 선언은 이 proof term을 이름에 연결한다. 아래처럼 term을 직접 작성할 수도 있고 tactic으로 proof를 작성할 수도 있지만, tactic 역시 최종적으로 term을 생성한다.

```lean
theorem andSwap (p q : Prop) : p ∧ q → q ∧ p :=
  fun h => And.intro h.right h.left
```

## dependent function type

- 비의존 함수 type: `α → β`
- dependent function type: `(x : α) → β x`
- `∀ x : α, p x`도 dependent function type이며 결과가 `Prop`인 표기임
- lambda: `fun x => body`
- application: `f x`

```lean
def id {α : Sort u} (x : α) : α := x

theorem allSelf : ∀ n : Nat, n = n := fun n => rfl
```

## universe

- `Prop`
  - proof-irrelevant proposition universe
- `Type u`
  - data와 일반 type의 universe hierarchy
- `Sort u`
  - `Prop`과 `Type` hierarchy를 함께 다루는 일반 표기
- `Type`은 보통 `Type 0`
- `Type u : Type (u + 1)`
- universe polymorphic definition은 여러 level에서 재사용 가능

```lean
universe u v

def compose {α : Type u} {β : Type v} {γ : Type _}
    (g : β → γ) (f : α → β) : α → γ :=
  fun x => g (f x)
```

## proof irrelevance

같은 proposition의 모든 proof는 관찰 가능한 계산에서 구별되지 않는다. 어떤 proof term인지보다 해당 proposition에 term이 존재한다는 사실, 즉 inhabitance가 중요하다는 뜻이다. 이 성질은 proof field를 가진 structure의 equality reasoning을 단순화하는 데 영향을 주며, `Prop`의 proof는 runtime code에서 제거될 수 있다.

## definitional equality

- 별도 equality proof 없이 kernel 계산으로 같은 expression으로 판단되는 관계
- 주요 계산
  - beta: lambda application
  - delta: reducible definition unfolding
  - iota: recursor, pattern match 계산
  - zeta: `let` 치환
  - projection reduction
- `rfl`은 양변이 definitionally equal하면 성공함
- theorem 기반 equality는 `rw`, `simp`, `calc` 등이 필요함

```lean
example : (fun n : Nat => n + 1) 2 = 3 := by
  rfl
```

## inductive type

- constructor로 값과 proposition proof rule 정의
- recursor와 induction principle 자동 생성
- parameter와 index를 사용한 family 정의 가능

```lean
inductive Vec (α : Type u) : Nat → Type u where
  | nil : Vec α 0
  | cons : α → Vec α n → Vec α (n + 1)
```

- positivity와 universe rule을 kernel이 검사함
- pattern matching과 recursive definition은 recursor 기반 core term으로 elaboration됨

## quotient와 extensionality

- Lean core theory는 quotient type 지원
- quotient로 equivalence relation에 따른 type 구성 가능
- propositional extensionality와 function extensionality는 Lean의 논리 환경에서 제공됨
- `Classical` namespace는 choice, excluded middle 기반 reasoning 제공
- theorem이 어떤 axiom에 의존하는지는 `#print axioms`로 확인

## source file

- 기본 확장자: `.lean`
- UTF-8 text 사용
- command가 위에서 아래로 environment 갱신
- 주석
  - line comment: `--`
  - block comment: `/- ... -/`
  - block comment 중첩 가능
- doc comment
  - `/-- ... -/`
  - 바로 뒤 declaration documentation에 연결됨

## module

Source file 하나는 module 하나에 대응하며, module name은 source root를 기준으로 한 경로에서 결정된다. 예를 들어 `MyProject/Data/Tree.lean` 파일의 module name은 `MyProject.Data.Tree`다. 이 이름과 filesystem layout이 맞지 않으면 import가 실패할 수 있다. Module의 compile 결과는 `.olean`을 중심으로 한 artifact로 저장된다.

## import

```lean
import Std.Data.HashMap
```

`import`는 파일 앞부분의 command 영역에 작성하며, 지정한 module의 environment extension과 declaration을 불러온다. Transitive dependency는 build system이 추적한다. 불러오는 module이 많아지면 elaboration과 build에 필요한 메모리나 시간이 늘 수 있으므로 필요한 module을 구체적으로 지정하는 편이 좋다.

## prelude

일반 source는 기본 prelude를 자동으로 import한다. 파일 시작에 `prelude` command를 두면 이 자동 import를 끌 수 있지만, 이는 bootstrapping이나 core 구현 같은 특수한 상황을 위한 기능이다. 일반 project에서는 기본 prelude를 유지하는 편이 좋다.

## namespace

- 이름 충돌 방지를 위한 qualified name scope임

```lean
namespace Geometry

structure Point where
  x : Float
  y : Float

def origin : Point := ⟨0, 0⟩

end Geometry

#check Geometry.Point
```

Namespace는 중첩할 수 있으며 declaration의 full name에 prefix를 추가한다. 위 예제의 `Point`를 `Geometry.Point`로 참조하는 것도 이 때문이다. 이 이름 구분이 runtime object를 만드는 것은 아니다.

## `open`

- `open Namespace`
  - namespace의 이름을 짧게 참조 가능
- `open scoped ScopeName`
  - scoped notation과 instance 활성화
- `export Namespace (names)`
  - 선택한 이름을 현재 namespace를 통해 다시 노출
- `open`은 이름을 복사하지 않고 name resolution 후보를 늘림
- 충돌하면 qualified name 사용

## `section`

- 공통 variable과 option의 lexical scope 제공
- section name은 선택 사항임
- section을 닫으면 declaration이 실제로 참조한 variable만 parameter로 일반화됨

```lean
section
  variable {α : Type u}
  variable (f : α → α)

  def twice (x : α) : α := f (f x)
end

#check twice
```

## variable와 include

- `variable (x : α)`
  - 뒤 declaration에서 사용할 section variable 선언
- 실제로 사용한 variable만 자동 포함
- `include x`
  - declaration type이나 body에 직접 나타나지 않아도 section variable 포함 요청
- `omit x`
  - 포함 요청 취소 또는 특정 declaration에서 제외
- theorem의 typeclass assumption 누락, 과잉을 제어할 때 유용

## private와 protected

- `private`
  - module 외부에서 안정적으로 참조하지 못하는 내부 이름 생성
- `protected`
  - namespace를 open해도 자동으로 짧은 이름 후보에 넣지 않음
  - dot notation 등 제한된 해석에 사용 가능
- public API와 helper declaration 구분에 활용

## scoped environment

- namespace와 section이 제어하는 항목
  - 이름 prefix
  - open namespace
  - variable와 local instance
  - local notation
  - option
  - attribute의 지역 효과
- file 구조가 elaboration 결과에 직접 영향하므로 scope 종료 위치를 명확히 유지

## project 구조 점검

- module name과 경로 일치
- source root를 Lake 설정과 editor가 동일하게 인식
- circular import 금지
- public namespace와 internal namespace 분리
- 광범위한 `open`보다 필요한 이름 qualification
- `section` variable이 declaration signature에 어떻게 일반화되는지 `#check @name`으로 확인
