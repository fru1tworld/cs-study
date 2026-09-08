# Lean 4 term과 runtime

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/Run-Time-Code/

>

> 원문: https://lean-lang.org/doc/reference/latest/Terms/

- program과 proof를 구성하는 기본 term syntax, evaluation, compiler, FFI 경계 정리

## term

Term은 수학적 object, type, proposition, proof, program을 표현한다. Elaborator는 확장 가능한 표면 term syntax를 core `Expr`로 변환하며, 같은 syntax라도 expected type에 따라 다르게 해석할 수 있다. 이렇게 얻은 core term은 kernel 검사를 받고, 실행 가능한 부분은 compiler의 입력이 된다.

## identifier

- simple identifier와 qualified identifier 사용
- Unicode 문자 사용 가능
- keyword와 충돌하는 이름은 escaped identifier syntax가 필요할 수 있음
- namespace, `open`, declaration scope가 name resolution에 영향
- `_root_.Name`으로 root namespace부터 명시 가능
- `@name`은 implicit argument 삽입을 제어하는 explicit application에 사용됨

## function type

- `α → β`: non-dependent function
- `(x : α) → β x`: dependent function
- `∀ x : α, p x`: proposition에 자주 쓰는 dependent function 표기
- binder 종류
  - `(x : α)`: explicit
  - `{x : α}`: implicit
  - `{{x : α}}`: strict implicit
  - `[x : C α]`: instance implicit

```lean
def const {α β : Type} (x : α) (_ : β) : α := x
```

## function

- lambda syntax

```lean
fun x => x + 1
fun (x : Nat) (y : Nat) => x + y
```

Pattern lambda를 쓰면 constructor별 clause를 작성할 수 있다. Binder type은 expected function type으로부터 추론되며, dependent result에서는 binder의 이름과 type이 결과 type에 나타날 수도 있다.

## application

- 공백으로 함수 적용 표현: `f x y`
- left associative: `(f x) y`
- named argument: `(name := value)`
- explicit implicit argument
  - `@f α x`
  - `f (α := Nat) x`
- pipeline, composition notation은 library와 notation scope에 따라 제공됨

## literal

- numeric literal은 `OfNat` instance로 type-directed elaboration
- negative literal은 `Neg`와 numeric representation 사용
- decimal, scientific literal은 지원 typeclass와 syntax에 따라 elaboration
- string literal은 `String` 또는 `OfScientific`와 별도 string coercion 문맥 사용
- character literal은 `Char`
- expected type이 모호하면 annotation 필요

```lean
#check (42 : Nat)
#check (42 : Int)
#check ('λ' : Char)
#check ("Lean" : String)
```

## constructor와 structure

- constructor application
  - fully qualified constructor
  - expected type에서 constructor name 추론
  - anonymous constructor `⟨...⟩`
- structure literal

```lean
structure Point where
  x : Int
  y : Int

def p : Point := { x := 1, y := 2 }
def moved : Point := { p with x := p.x + 1 }
```

`moved`는 기존 `p`를 바탕으로 `x` field를 교체한 값이다. Field는 `obj.field` 표기로 접근하며, method-style dot notation은 첫 explicit parameter와 namespace를 이용해 해석한다.

## conditional

- `if condition then a else b`
- condition은 `Decidable proposition`을 사용할 수 있음
- `if h : p then ... else ...`
  - branch에서 proof `h : p` 또는 `h : ¬p` 사용 가능
- `dite`
  - branch result가 proof에 의존하는 dependent conditional
- boolean conditional과 proposition conditional을 구분해야 함

## pattern matching

```lean
def head? : List α → Option α
  | [] => none
  | x :: _ => some x
```

- `match subject with` term으로도 사용 가능
- 여러 subject, nested pattern, wildcard, named pattern 지원
- equation compiler가 recursor 기반 core term으로 변환함
- dependent match는 expected type과 index equality를 사용함
- coverage와 inaccessible pattern 검사 수행

## `let`과 `have`

- `let x := value; body`
  - local computational definition
- `have h : proposition := proof; body`
  - local proof 또는 중간 term
- `show type from term`
  - expected type을 명시함
- layout-sensitive command, term block에서 indentation이 scope를 결정할 수 있음

## hole

- `_`
  - elaborator가 추론해야 할 anonymous hole
- `?_`
  - editor goal로 표시되는 synthetic hole
- `?name`
  - named metavariable 문맥
- `by` 전술 블록(tactic block)
  - term을 생성함

  ```lean
  by
      ...
  ```
- 완성 declaration에 unresolved hole이 남을 수 없음

## type ascription

- `(term : Type)`
  - term expected type 지정
- `(term : Type) : Type2`
  - nested ascription 가능
- overloaded notation, universe, coercion 디버깅에 유용
- `show Type from term`은 proof와 긴 expression 가독성에 유용

## quotation과 antiquotation

- syntax quotation `` `(term| ...) `` 등으로 syntax tree 생성
- antiquotation `$x`로 기존 syntax 삽입
- category별 quotation
  - term
  - command
  - tactic
  - pattern
- macro와 elaborator extension 작성에 사용
- hygienic identifier 처리로 capture 방지

## proof term

`by` tactic block도 term syntax이므로, 직접 작성한 term proof와 tactic proof를 혼합할 수 있다. 다음은 함수 적용으로 구성한 직접 term proof다.

```lean
theorem impTrans (p q r : Prop) :
    (p → q) → (q → r) → p → r :=
  fun hpq hqr hp => hqr (hpq hp)
```

같은 proof를 `by exact ...`로 감싸면 tactic mode로 작성할 수 있다. Proof irrelevance 덕분에 proof 구현의 차이보다 유지보수하기 좋은 style을 기준으로 선택할 수 있다.

## runtime code

> 원문: https://lean-lang.org/doc/reference/latest/Run-Time-Code/

- kernel evaluator와 compiler runtime은 목적이 다름
- kernel
  - definitional equality와 type checking을 위한 normalization
  - termination이 보장된 safe definition 사용
- compiler
  - executable program 성능 중심
  - proof, type erase, specialization, closure conversion 등 수행
- `#reduce`와 `#eval`의 차이도 이 경계에서 발생함

## code generation

Lean compiler는 source definition을 intermediate representation으로 변환하고, runtime은 reference counting으로 memory를 관리한다. Unique reference를 확인할 수 있으면 immutable update를 in-place mutation으로 최적화한다. 내부에서는 값을 제자리에서 바꾸더라도 외부에서 관찰하는 semantics는 pure functional model을 유지한다. Packed array와 primitive type에는 최적화된 표현을 제공한다.

## `unsafe`

`unsafe`를 사용하면 termination이나 kernel reduction의 요구를 벗어난 executable definition을 작성할 수 있다. 대신 이를 safe definition의 body나 theorem proof에 사용할 수는 없다. Runtime crash, nontermination, memory safety 문제가 발생할 수 있는 실행과 proof soundness의 경계를 구분하는 표시다.

```lean
unsafe def loop : Nat := loop
```

- 논리적 계산에는 사용할 수 없지만 compiled execution에서는 호출 가능

## 외부 구현과 FFI

- `@[extern "symbol"]`
  - Lean declaration을 외부 symbol과 연결
- `@[implemented_by impl]`
  - logical reference implementation과 runtime implementation 분리
- FFI에서 확인할 항목
  - ABI와 platform
  - object ownership과 reference count
  - exception과 error code
  - thread safety
  - data representation
- 외부 구현의 correctness는 kernel이 검증하지 않음

## runtime 초기화

Module initialization code는 executable load 시 실행될 수 있으며, global environment extension이나 runtime resource를 준비하는 데 사용된다. 초기화 순서는 import dependency와 compiler 규칙을 따른다. 이 과정에서 hidden global state가 늘어나면 test와 reproducibility를 확보하기 어려워질 수 있다.

## 성능 점검

- proof에서 쓰는 reducible model과 runtime 구현 구분
- persistent data update가 uniqueness optimization을 받는지 확인
- `Array`, `ByteArray`, `String` 같은 primitive-friendly type 활용
- tail recursion과 allocation profile 확인
- `#eval` 속도만으로 optimized executable 성능을 단정하지 않음
- benchmark는 `lake build`로 만든 executable에서 수행 권장
