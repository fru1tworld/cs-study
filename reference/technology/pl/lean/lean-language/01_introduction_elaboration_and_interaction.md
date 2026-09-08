# Lean 4 소개, elaboration, 상호작용

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/

>

> 원문: https://lean-lang.org/doc/reference/latest/Introduction/

>

> 원문: https://lean-lang.org/doc/reference/latest/Elaboration-and-Compilation/

>

> 원문: https://lean-lang.org/doc/reference/latest/Interacting-with-Lean/

- 기준 문서: The Lean Language Reference 4.34.0-rc2
- 공식 reference는 정확한 언어 기술을 위한 문서이며 입문 tutorial과 목적이 다름
- Lean 4의 역할, source 처리 pipeline, editor, query command 상호작용 정리

## Lean 4의 두 역할

Lean 4는 dependent type theory 기반의 대화형 정리 증명기이자 strict pure functional programming language다. 따라서 증명과 일반 프로그램을 같은 언어로 작성할 수 있다.

- 같은 언어에서 수행 가능한 작업
  - 수학적 object와 proposition 정의
  - proof term과 tactic proof 작성
  - 일반 program과 `IO` application 작성
  - parser, elaborator, tactic, command 확장
- Lean 4 구현의 대부분이 Lean 자체로 작성된 self-hosting 구조임

## 신뢰 모델

Elaborator는 사용자 syntax와 automation의 결과를 core term으로 변환하고, 작은 kernel이 그 term의 type을 검사한다. Tactic 역시 proof를 직접 신뢰시키는 대신 kernel이 검사할 proof term을 생성한다. 이 때문에 parser, macro, elaborator, tactic의 bug가 일반적으로 잘못된 theorem을 kernel에 등록할 수는 없다.

다만 axiom, `sorry`, unsafe definition을 사용하거나 compiler와 runtime으로 실행할 때는 각각의 신뢰 경계를 별도로 고려해야 한다.

## 역사

- 2013년 Leonardo de Moura가 Microsoft Research에서 프로젝트 시작
- Lean 0.1: 2014년 공개
- Lean 3: self-extensible tactic, notation, command와 Mathlib 생태계 성장
- Lean 4 개발: 2018년 시작
- Lean 4.0: 2023년 9월 공개
- Lean 4의 방향
  - 작은 독립 구현 가능한 kernel
  - 높은 성능과 대규모 project 확장성
  - language 자체로 frontend와 automation 확장

## source 처리 pipeline

> 원문: https://lean-lang.org/doc/reference/latest/Elaboration-and-Compilation/

- parsing
  - 문자와 token을 `Syntax` tree로 변환함
- macro expansion
  - syntactic sugar를 더 기본적인 syntax로 변환함
- elaboration
  - 표면 syntax를 core expression으로 변환함
  - implicit argument, coercion, typeclass instance, universe를 해결함
- kernel checking
  - core expression이 type theory 규칙을 만족하는지 검사함
- compilation
  - 실행할 definition을 runtime code로 변환함

## command 단위 처리

실제 파일 처리는 파일 전체를 위 단계별로 한 번씩 통과시키는 방식이 아니다. Top-level command 하나를 parse, elaborate, check한 뒤 environment를 갱신하고, 다음 command는 이 갱신된 environment에서 처리한다. 따라서 앞 command는 뒤 command의 해석에 영향을 줄 수 있다.

- 앞 command가 뒤 command에 줄 수 있는 영향
  - 새 definition, theorem, type
  - 새 notation, macro, parser
  - namespace와 open declaration
  - attribute와 option

## parser

- extensible recursive-descent parser 사용
- precedence 처리를 위해 Pratt parsing 사용
- grammar production은 syntax kind 이름을 가짐
- ambiguous parse
  - 가장 긴 parse 우선
  - 같은 길이 후보가 남으면 choice node를 elaborator가 해소함
- parse 실패 시 `Syntax.missing`으로 오류 복구 가능
- `SourceInfo`가 source 위치, 공백, 생성 origin 기록
  - `original`: source에서 직접 생성
  - `synthetic`: macro나 code가 생성
  - `none`: source 연결 없음

## macro expansion

Macro는 `Syntax`를 입력받아 `Syntax`를 반환하며, outer syntax를 반복적으로 확장한다. 이때 hygiene가 macro 내부 identifier와 사용자 scope의 우발적인 충돌을 방지한다. Macro는 type 정보를 사용하지 않는 syntax 변환에 적합하고, type에 따라 변환을 달리해야 한다면 elaborator extension이 더 적합하다.

## elaborator

- command elaborator
  - top-level command의 environment effect 구현
- term elaborator
  - expected type과 syntax에서 core `Expr` 생성
- tactic elaborator
  - proof goal을 조작해 proof term 완성
- elaboration 중 생성되는 정보
  - inferred type
  - metavariable와 constraint
  - source position별 hover, goal, completion metadata
  - message와 diagnostic
- editor의 interactive 기능은 info tree를 활용함

## metavariable

Metavariable은 아직 정해지지 않은 term이나 type을 나타낸다. `_` hole, implicit argument, tactic goal 등에 사용되며, unification, typeclass synthesis, tactic을 통해 해결된다. 해결되지 않은 metavariable이 남으면 declaration을 완료하지 못할 수 있다. 작성 중인 term의 예상 type을 확인하려면 editor에서 `?_`나 named hole을 활용할 수 있다.

```lean
def identity {α : Type} (x : α) : α := x

#check identity 3
#check identity "lean"
```

## kernel

Kernel은 elaboration을 마친 declaration의 type correctness를 검사한다. Definition body, theorem proof, inductive declaration, universe constraint가 검사 대상이며, 이를 통과한 declaration을 environment에서 신뢰 가능한 상수로 사용한다.

Kernel이 검사하는 core language는 표면 language보다 작다. 이 작은 언어 안에 proof irrelevance와 quotient 등 Lean core theory의 규칙을 구현한다.

## compiler와 runtime

- executable definition은 intermediate representation을 거쳐 native code로 컴파일 가능
- proof와 type-only 정보는 runtime code에서 지워질 수 있음
- reference counting과 in-place optimization으로 functional program 효율 개선
- `unsafe` code는 evaluator, compiler에서 실행할 수 있지만 kernel reduction에는 사용할 수 없음
- theorem soundness와 executable correctness의 신뢰 경계 구분 필요

## initialization

- `initialize`
  - module load 시 실행되는 초기화 code 등록
- `builtin_initialize`
  - builtin module 초기화에 사용하는 variant
- 초기화는 environment extension, runtime registry 설정 등에 사용됨
- import 순서와 initialization side effect를 고려해야 함

## editor 중심 상호작용

> 원문: https://lean-lang.org/doc/reference/latest/Interacting-with-Lean/

공식 권장 환경은 VS Code와 Lean 4 extension을 중심으로 구성된다. Source를 수정하면 language server가 변경 사항을 점진적으로 처리하고 다음 정보를 UI에 표시한다.

- 주요 UI 정보
  - 현재 goal과 local context
  - identifier hover type과 documentation
  - error, warning, information message
  - go to definition과 references
  - completion과 code action
- file이 project의 toolchain과 Lake environment에서 열려야 정확한 import 해석 가능

## query command

- `#check term`
  - term type 확인
- `#print name`
  - declaration 내용 확인
- `#print axioms name`
  - theorem이 의존하는 axiom 확인
- `#reduce term`
  - kernel reduction 기반 계산
- `#eval term`
  - compiled evaluator로 실행하고 결과 출력
- `#synth TypeClass args`
  - typeclass instance synthesis 결과 확인
- `#guard proposition`
  - test assertion 작성
- `#where name`
  - source 위치 확인

```lean
#check Nat.add
#eval (List.range 5).map (fun n => n * n)
#reduce (fun x : Nat => x + 1) 2
#synth Repr Nat
```

## `#eval`과 `#reduce`

- `#reduce`
  - kernel reduction 사용
  - trusted definitional computation 관찰
  - 큰 계산에는 느릴 수 있음
- `#eval`
  - compiler, runtime 사용
  - 일반 program과 `IO` 실행 가능
  - `Repr`, `ToString`, `ToExpr` 등으로 출력 형식 결정
- `sorry`에 의존한 expression은 일반 `#eval`이 거부할 수 있음
- `#eval!`은 안전 검사를 우회하므로 진단 목적에만 신중히 사용

## option

- `set_option name value in command`
  - command 하나에 option 적용
- section 안 `set_option`
  - scope에 option 적용
- 자주 사용하는 영역
  - pretty printing
  - trace
  - recursion, elaboration 제한
  - tactic heartbeat

```lean
set_option pp.all true in
#check fun x => x
```

## message와 trace

- message severity
  - information
  - warning
  - error
- `set_option trace.category true`
  - 특정 subsystem trace 활성화
- `trace_state`
  - tactic proof state 출력
- `dbg_trace`
  - program 실행 중 debug message
- trace는 출력량과 성능 비용이 크므로 좁은 scope에 적용 권장

## 최소 파일

```lean
def double (n : Nat) : Nat := n + n

theorem double_eq_add (n : Nat) : double n = n + n := by
  rfl

#eval double 21
#print axioms double_eq_add
```
