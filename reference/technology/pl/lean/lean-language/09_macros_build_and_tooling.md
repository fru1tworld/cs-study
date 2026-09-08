# Lean 4 macro, build, tooling

## 문서 범위

> 원문: https://lean-lang.org/doc/reference/latest/Notations-and-Macros/

>

> 원문: https://lean-lang.org/doc/reference/latest/Build-Tools-and-Distribution/

>

> 원문: https://lean-lang.org/doc/reference/latest/ValidatingProofs/

>

> 원문: https://lean-lang.org/doc/reference/latest/Error-Explanations/

>

> 원문: https://lean-lang.org/doc/reference/latest/platforms/

- syntax 확장부터 package build, proof 신뢰 경계, 오류 진단까지의 실전 흐름 정리

## syntax category

Lean parser는 모든 text를 하나의 고정 grammar로 처리하지 않는다. 문법을 확장하려면 다음 중 어느 syntax category에 들어가는지 명시해야 한다.

- 주요 syntax category
  - term
  - tactic
  - command
  - pattern
  - level

같은 token도 category와 parser state에 따라 다르게 해석될 수 있다.

## notation

Notation은 기존 term을 읽기 좋은 표기로 드러내며, prefix, infix, postfix, mixfix 형태로 정의할 수 있다. 이때 precedence와 associativity가 parse tree를 결정한다. Precedence가 너무 낮거나 높으면 예상과 다르게 결합할 수 있다. Public notation은 API이므로 이름 충돌과 사용자가 표기를 찾기 쉬운지도 고려한다.

```lean
infixl:65 " +++ " => Nat.add

#check 1 +++ 2 +++ 3
```

## precedence

- 숫자가 클수록 더 강하게 결합
- left, right associativity를 명시
- operand 위치별 precedence 제약 가능
- ambiguous parse는 parentheses로 해결할 수 있지만 notation 설계 자체를 점검하는 편이 좋음

## scoped notation

Notation을 namespace와 비슷한 scope에 배치하면 필요한 곳에서만 `open scoped`로 활성화할 수 있다. 이 방식은 global parser namespace의 오염을 줄이므로, library가 범용 token을 제공할 때 활용하는 편이 좋다.

## macro

Macro는 syntax를 다른 syntax로 변환하는 compile-time extension으로, 단순한 notation보다 구조적인 rewrite가 필요할 때 사용한다. Expansion 결과는 다시 elaboration되며, macro 자체는 type information을 직접 사용하기 전 단계에서 동작하는 경우가 많다. Type에 따라 변환을 달리해야 한다면 elaborator extension을 검토한다.

```lean
syntax "unless " term " then " doSeq : term

macro_rules
  | `(unless $cond then $body) => `(if !$cond then do $body)
```

## hygiene

Macro가 도입한 identifier가 호출 지점의 identifier를 우연히 capture하지 않도록 hygiene가 적용된다. Syntax quotation으로 생성한 이름은 scope information을 가지므로 raw string을 조립하기보다 quotation과 antiquotation을 사용하는 편이 좋다. 호출 지점의 이름을 의도적으로 참조해야 한다면 resolution을 명시해야 한다.

## quotation과 antiquotation

- `` `( ... ) `` 형태로 syntax tree 작성
- `$x`, `$xs,*` 등으로 기존 syntax 삽입
- expected syntax category가 quotation 해석을 제한
- source location과 scope가 보존되어 error message와 hygiene가 개선됨
- quotation pattern을 macro rule의 left side에서도 사용

## command와 elaborator extension

- macro expansion만으로 부족한 경우 custom elaborator 작성 가능
- elaborator는 expected type, local context, metavariable 같은 정보에 접근
- 새로운 command는 environment를 변경할 수 있음
- extension이 생성하는 declaration의 name, visibility, source location 관리 필요
- kernel이 최종 term을 검사하더라도 extension 자체의 error message와 termination 품질은 별도 책임

## toolchain 관리

- `elan`
  - Lean toolchain 설치, 선택, 업데이트
  - project의 `lean-toolchain` 파일을 읽어 version 선택
- project마다 version을 고정해 parser, elaborator, library 변화로 인한 재현성 문제를 줄임
- 임의 최신 version으로 자동 이동시키기보다 의도적으로 upgrade하고 test 수행

## Lake

- Lean package와 build를 관리하는 도구
- 주요 역할
  - dependency resolution
  - library, executable target 정의
  - source build
  - external command 실행
  - package artifact 관리
- `lakefile.lean` 또는 지원되는 declarative configuration으로 package 설정
- `lake-manifest.json`은 resolved dependency를 기록

## project 구성

- 일반적인 구성 요소
  - `lean-toolchain`: Lean version
  - package configuration: package와 target
  - source directory: module hierarchy
  - manifest: dependency revision
- module name과 file path가 대응
- import graph가 compilation dependency 결정
- library root와 namespace를 일관되게 유지하면 import가 명확해짐

## target

- Lean library
  - import 가능한 module collection
- executable
  - `main`을 가진 native program target
- external library 또는 facet
  - build pipeline 확장에 사용 가능
- target별 source root, dependency, compiler option을 분리

## build artifact

Lean source는 elaboration 후 environment 정보를 담는 artifact로 compile된다. Import는 source text를 매번 처음부터 처리하는 대신 이 compiled module을 활용한다. Lean version이나 option이 바뀌면 artifact를 다시 생성해야 한다. Stale artifact가 의심될 때는 clean rebuild가 진단에 도움이 되지만, 먼저 version과 dependency가 맞는지 확인한다.

## dependency

- package source는 Git revision, local path 등으로 지정 가능
- reproducible build를 위해 floating branch보다 고정 revision 권장
- local path dependency는 함께 개발할 때 편리하지만 배포, CI 환경에서 path 존재를 보장해야 함
- transitive dependency의 Lean version compatibility 확인

## 자주 쓰는 명령

```console
$ elan show
$ lake update
$ lake build
$ lake env lean Main.lean
```

- `lake update`는 manifest와 dependency state를 변경할 수 있으므로 version upgrade 의도 없이 습관적으로 실행하지 않음
- `lake env`는 package가 선택한 Lean environment에서 command 실행

## proof validation

Elaborator와 tactic이 생성한 proof term은 kernel이 다시 검사해 선언된 type을 가지는지 확인한다. 이렇게 작은 trusted kernel을 두어 tactic이나 macro의 bug가 잘못된 theorem을 바로 승인하지 못하게 한다. 이때 import된 declaration, axiom, opaque theorem도 신뢰 관계에 포함된다.

## axiom 확인

Theorem이 어떤 axiom에 의존하는지는 `#print axioms`로 확인한다.

```lean
#print axioms Classical.choice
#print axioms myTheorem
```

Classical axiom을 사용했다고 해서 논리적 모순이 있다는 뜻은 아니다. 다만 constructive content 추출, 계산 가능성, 외부 checker 호환성에 영향을 주므로, project에서 허용할 axiom policy를 정하고 CI에서 검사할 수 있다.

## `sorry`

- 미완성 proof를 임시 placeholder로 통과시킴
- declaration은 경고와 함께 axiom에 의존하는 형태가 됨
- exploration에는 유용하지만 완료된 검증 artifact에는 남기지 않음
- warning을 error로 취급하거나 source scan을 통해 CI에서 차단 가능

## `unsafe`

- termination 또는 logical soundness를 kernel proof로 사용하지 않는 runtime code 표시
- `unsafe` definition은 theorem의 신뢰 가능한 proof computation에 사용할 수 없음
- compiler runtime과 kernel reduction의 경계를 이해해야 함
- performance optimization을 위해 `unsafe`를 도입하면 pure specification과 equivalence theorem을 별도로 유지하는 방식이 유용

## compiler와 kernel

- native compiler가 executable을 생성
- kernel type checking이 theorem의 논리적 유효성을 담당
- compiler correctness 자체가 모든 theorem의 신뢰 조건은 아님
- proof를 외부 형식으로 export하거나 독립 checker로 검사하면 다른 신뢰 경계를 구성할 수 있음
- code generation 결과의 동작 검증은 theorem과 extraction, compiler pipeline의 연결을 별도로 다뤄야 함

## 오류 읽기

- parser error
  - token과 grammar category 점검
  - delimiter와 indentation 확인
- elaboration error
  - expected type과 inferred type 비교
  - implicit argument와 typeclass instance 확인
- unsolved goals
  - tactic 종료 시 남은 metavariable 확인
- unknown identifier
  - import, namespace, spelling, visibility 확인
- application type mismatch
  - function parameter 순서와 coercion을 하나씩 확인

## 진단 명령

- `#check`: expression type 확인
- `#print`: declaration 내용 확인
- `#reduce`, `#eval`: 계산 결과 관찰
- `set_option pp.all true`: 숨겨진 implicit structure 표시
- `set_option trace... true`: 특정 subsystem trace 활성화
- 최소 재현 파일을 만들고 import를 줄이면 elaboration 문제를 분리하기 쉬움

## platform

- 공식 지원 platform과 architecture는 release별 문서 확인
- native dependency와 C toolchain 요구사항이 platform마다 다를 수 있음
- editor extension, Lake, compiler가 같은 toolchain을 사용하는지 확인
- CI와 local machine의 Lean version, environment variable, native library 차이를 기록

## release 변화 대응

- language reference의 `latest`는 이동하는 문서
- project는 `lean-toolchain`에 version을 고정
- upgrade 절차
  - release note 확인
  - dependency compatibility 확인
  - 별도 branch에서 toolchain 변경
  - full build와 test
  - warning과 deprecated syntax 정리
- 문서 예제가 compile되지 않으면 먼저 문서 version과 project toolchain 비교

## 권장 개발 흐름

- 작은 declaration을 작성하고 즉시 editor feedback 확인
- public syntax extension은 scope와 precedence를 함께 test
- theorem 완료 전에 `#print axioms`로 dependency 확인
- package version과 manifest를 함께 review
- `lake build`로 전체 import graph 검증
- executable은 representative input으로 runtime test
- proof와 runtime code의 trusted boundary를 문서화

## 문서 묶음 완료

- [시작 문서](./01_introduction_elaboration_and_interaction.md)
- 이 문서까지 Lean 4 language reference의 핵심 영역을 9개 주제로 정리함
