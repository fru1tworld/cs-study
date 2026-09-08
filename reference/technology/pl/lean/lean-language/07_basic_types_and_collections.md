# Lean 4 기본 타입과 컬렉션

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/Basic-Types/

Lean 프로그램과 증명에서 반복해서 사용하는 기본 데이터 타입을 다룬다. 각 타입의 값을 어떻게 만들고 분해하는지, 계산할 때는 어떤 특성이 있는지 함께 살펴본다.

## 단위 타입과 빈 타입

- `Unit`
  - 값이 `()` 하나뿐인 타입
  - 의미 있는 반환값이 없는 계산의 결과에 사용
- `Empty`
  - 생성자가 없어 값이 존재하지 않는 타입
  - 도달할 수 없는 경우를 표현
- `Nomatch` 또는 empty elimination으로 불가능한 값에서 임의 결과 도출 가능

```lean
def ignoreResult (_ : Nat) : Unit := ()

def absurd {α : Sort u} (h : Empty) : α := nomatch h
```

## Boolean

`Bool`은 `true`, `false` 두 값을 가진 계산 가능한 데이터 타입이다. `&&`, `||`, `!`로 Boolean 연산을 수행하고, `if b then x else y`의 조건에 `b : Bool`을 사용할 수 있다.

`Bool`이 실행해 결과를 얻는 데이터라면 `Prop`은 증명의 대상이다. 둘 사이를 연결할 때는 `decide`, `of_decide_eq_true` 같은 도구를 사용해 decidable proposition과 Boolean을 오간다.

```lean
#eval (4 < 7)
#eval true && !false
```

## 자연수와 정수

- `Nat`
  - 0과 successor로 구성된 자연수
  - 구조적 재귀와 귀납법의 대표 대상
  - 뺄셈은 0 아래로 내려가지 않는 truncated subtraction
- `Int`
  - 음수를 포함하는 정수
  - 자연수와 다른 나눗셈, 나머지 규칙에 주의
- 숫자 literal은 `OfNat` typeclass를 통해 문맥의 타입으로 해석
- 산술 표기는 `Add`, `Sub`, `Mul`, `Div`, `Mod` 등의 typeclass를 사용

```lean
#check (42 : Nat)
#check (-42 : Int)
#eval (3 - 5 : Nat)
#eval (3 - 5 : Int)
```

## 유한 자연수 `Fin`

`Fin n`은 `n`보다 작은 자연수와 그 범위 증명을 함께 보관한다. 배열이나 벡터처럼 범위를 타입에 포함한 index에 유용하며, 조건을 만족하는 자연수가 없는 `Fin 0`에는 inhabitant가 없다. Runtime에서 proof field는 계산에 영향을 주지 않지만, 값을 생성할 때는 범위 proof가 필요하다.

```lean
def lastIndex (n : Nat) : Fin (n + 1) :=
  ⟨n, Nat.lt_succ_self n⟩
```

## 곱과 합

- `Prod α β`, 표기 `α × β`
  - 두 값을 동시에 보관
  - `(a, b)`, `.1`, `.2`, pattern matching 사용
- `Sum α β`
  - 둘 중 한 타입의 값을 보관
  - `Sum.inl`, `Sum.inr`로 생성
  - 두 case를 모두 처리해야 제거 가능
- 여러 반환값은 보통 tuple로 표현

```lean
def swap (p : α × β) : β × α := (p.2, p.1)

def describe : Sum Nat String → String
  | .inl n => s!"number: {n}"
  | .inr s => s
```

## 선택적 값과 오류

- `Option α`
  - `some a` 또는 `none`
  - 실패 이유가 필요 없는 부분 함수에 적합
- `Except ε α`
  - `ok a` 또는 `error e`
  - typed error를 반환하는 계산에 적합
- 둘 다 `Functor`, `Applicative`, `Monad` instance를 통해 `do` notation 사용 가능

```lean
def head? : List α → Option α
  | [] => none
  | x :: _ => some x

def positive (n : Int) : Except String Nat :=
  if h : 0 ≤ n then
    .ok n.toNat
  else
    .error "negative input"
```

## subtype과 dependent pair

- subtype `{x : α // p x}`
  - 값 `x`와 그 값이 만족하는 property proof를 묶음
  - second component가 `Prop`이므로 계산용 추가 data보다는 invariant 표현에 적합
- `Sigma β`, 표기 `(x : α) × β x`
  - 두 번째 component의 타입이 첫 번째 값에 의존
  - proof뿐 아니라 실제 계산 data도 보관 가능
- `.val`, `.property` 또는 pattern matching으로 분해

```lean
def EvenNat := {n : Nat // n % 2 = 0}

def zeroEven : EvenNat := ⟨0, rfl⟩
```

## 리스트

`List α`는 `[]`와 `x :: xs`로 구성된 immutable inductive type이다. 길이가 타입에 포함되지 않으며, 생성자 구조를 따라 처리하는 구조적 재귀와 pattern matching에 적합하다.

- 핵심 operation
  - `map`, `filter`, `foldl`, `foldr`
  - `append`, `reverse`, `length`
  - `head?`, `get?`, `find?`
- `xs[i]?`는 bounds failure를 `Option`으로 표현
- `xs[i]` 형태는 bounds proof를 요구하거나 문맥에 따라 panic 가능한 access로 elaboration될 수 있으므로 API signature 확인 필요

```lean
def sum : List Nat → Nat
  | [] => 0
  | x :: xs => x + sum xs

#eval [1, 2, 3].map (· * 2)
```

## 배열과 바이트 배열

- `Array α`
  - random access와 update에 적합한 contiguous collection
  - functional interface를 제공하지만 compiler가 uniqueness를 활용해 효율적으로 갱신 가능
- `ByteArray`
  - byte data에 특화된 compact representation
  - binary IO와 encoding 경계에서 사용
- index 접근 방식
  - proof를 요구하는 안전한 접근
  - `Option`을 반환하는 checked access
  - bounds 위반 시 panic하는 access
- theorem에서는 어느 실패 모델을 택했는지 명시해야 함

```lean
#eval #[10, 20, 30].size
#eval #[10, 20, 30][1]!
```

## 문자열과 문자

`Char`는 Unicode scalar value를, `String`은 UTF-8 text를 표현한다. 문자열 literal의 타입은 `String`이며 interpolation에는 `s!"..."`를 사용한다.

UTF-8에서는 byte offset과 character position이 같지 않다. 따라서 text algorithm을 작성할 때 byte, Unicode scalar value, grapheme cluster 중 무엇을 세는지 먼저 구분해야 한다. 사용자에게 보이는 문자 단위 처리는 core `String`만으로 충분하지 않을 수 있다.

```lean
#eval "Lean" ++ " 4"
#eval s!"length: {"Lean".length}"
```

## 비교와 순서

- proposition 기반 관계
  - `LT.lt`, `LE.le`
  - theorem과 rewriting에 적합
- Boolean 비교
  - `BEq.beq`
  - 실행 가능한 equality 검사
- `DecidableEq α`
  - equality proposition을 결정할 수 있음을 나타냄
- `Ord α`
  - 세 방향 비교 결과를 계산
- 서로 다른 interface가 같은 의미를 가져야 한다면 연결 정리를 제공해야 함

## hashing과 key

Hash 기반 collection에서는 equality와 hash가 일관되어야 한다. 같은 key는 같은 hash를 가져야 하지만 서로 다른 key의 hash collision은 허용되므로, 최종 판단에는 equality 검사를 사용한다. 구조체의 instance를 자동 생성할 때도 field 전체가 원하는 identity를 나타내는지 확인해야 한다.

## map과 set

Lean core, 표준 라이브러리, 외부 라이브러리는 서로 다른 map과 set 구현을 제공할 수 있다. 어떤 구현을 쓸지는 증명과 실행에서 필요한 특성을 함께 고려해 선택한다.

- 선택 기준
  - theorem 전개가 쉬운 association list
  - 순서를 보존하는 tree
  - 평균 constant-time lookup을 목표로 하는 hash table

API마다 mutable 여부, key requirement, iteration order가 다르다. 증명이 이런 구현 세부에 의존하지 않게 하려면 abstract interface와 invariant를 분리하는 편이 좋다.

## equality와 extensionality

- inductive value equality는 constructor와 field equality로 환원 가능
- function은 본문이 구문상 같지 않아도 모든 입력의 결과가 같으면 extensional equality를 사용할 수 있음
- collection equality theorem은 흔히 다음 둘을 구분
  - representation equality
  - membership, lookup 결과가 같다는 observational equality
- quotient나 normalized representation을 사용할 때 차이가 중요

## deriving

- declaration 뒤 `deriving`으로 반복 instance 생성 가능

```lean
inductive Color where
  | red | green | blue
  deriving Repr, BEq, DecidableEq
```

- 자주 쓰는 derived instance
  - `Repr`
  - `BEq`
  - `DecidableEq`
  - `Hashable`
  - `Inhabited`
- 자동 생성 결과도 public behavior의 일부이므로 equality, hash semantics 검토 필요

## 타입 선택 지침

- 실패 이유 불필요: `Option`
- 실패 이유 필요: `Except`
- 고정 범위 index: `Fin`
- 값과 논리 invariant: subtype
- 값에 따라 달라지는 계산 data: Sigma
- 재귀적 proof와 순차 처리: `List`
- random access와 runtime 성능: `Array`
- binary payload: `ByteArray`
- 계산 가능한 조건: `Bool` 또는 `Decidable p`
- 논리적으로 증명할 조건: `Prop`
