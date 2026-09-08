# Rocq library, plugin, extraction

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/using/libraries/index.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/coq-library.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/extraction.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/using/libraries/writing.html

- Corelib, Stdlib, package, plugin, extraction, functional induction 정리

## library와 plugin 차이

- library
  - 컴파일된 Rocq script로 구성됨
  - 정의, 정리, notation, hint, setting 제공
- plugin
  - OCaml code를 포함할 수 있음
  - 새 command, tactic, parser, 내부 기능 추가 가능

library의 proof term은 kernel이 검사한다. plugin은 Rocq의 동작 자체를 바꿀 수 있으므로 그 코드에 더 높은 신뢰가 필요하다. 다만 plugin을 사용한 `.vo`도 `rocqchk`로 독립 검사할 수 있다. `rocqchk`는 plugin을 로드하지 않고 컴파일된 객체를 검사한다.

## Corelib와 Stdlib

- Corelib
  - Rocq 실행과 기본 논리에 필요한 최소 library
  - prelude, 기본 inductive type, equality 등 포함
- Stdlib
  - 일반 프로그래밍, 수학용 library
  - 배포, 패키징 정책에 따라 별도 package로 설치될 수 있음

9.0부터 표준 library의 prefix는 `Stdlib`이고, 기존 `Coq` prefix는 deprecated 경고와 함께 계속 동작한다. 그래서 오래된 코드에는 `From Coq Require ...`가 남아 있을 수 있다. 실제로 사용하는 module은 `Locate Library`, package 문서, `rocq -where`로 확인한다.

## prelude

- 기본 notation
  - function type, logical connective, equality
- 기본 논리
  - `True`, `False`, conjunction, disjunction, existential
- 기본 datatype
  - `bool`, `nat`, option, sum, pair 등
- equality
  - Leibniz equality `eq`
  - `eq_refl` constructor
- specification datatype
  - `sig`, `sigT`, `sumbool` 등

이 가운데 일부 module은 자동으로 load되지만, 나머지는 `Require`로 직접 불러와야 한다.

## module 불러오기

다음 코드는 `From Stdlib Require Import`로 `List`와 `Arith`를 load하면서 import하고, `ListNotations`는 `Import`로 따로 활성화한다.

```coq
From Stdlib Require Import List Arith.
Import ListNotations.
```

- `Require`
  - compiled module load
- `Import`
  - 짧은 이름과 export object 활성화
- `Export`
  - 현재 module 사용자가 효과를 이어받음

fully qualified name을 쓰면 import하지 않고도 이름 충돌을 줄일 수 있다. import를 최소한으로 유지하면 build time과 namespace pollution도 줄어든다.

## package와 opam

Rocq ecosystem의 package는 opam repository에서 배포되는 경우가 많다. package에는 다음 요소가 포함될 수 있다.

- `.vo` library
- plugin binary
- source와 documentation
- findlib, dune metadata

compiler와 library version은 project별 opam switch로 격리하는 것을 권장하며, major version을 올리기 전에는 dependency가 호환되는지 확인해야 한다. 사용할 package는 Rocq Package Index에서 찾아볼 수 있다.

## plugin 채택 기준

- 유지보수와 최신 Rocq 호환성
- Rocq CI 참여 여부
- generated proof term을 `rocqchk`로 검사 가능한지 여부
- axiom과 unsafe feature 사용 여부
- build system과 package manager 통합 수준
- proof script가 plugin-specific syntax에 얼마나 결합되는지 여부

## program extraction

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/extraction.html

program extraction은 계산 가능한 Rocq 정의를 외부 함수형 언어의 코드로 변환한다. 이 과정에서 논리 증명과 `Prop`의 비계산 부분은 제거할 수 있다.

- 주요 target
  - OCaml
  - Haskell
  - Scheme

추출된 코드가 Rocq kernel에서 직접 실행되는 코드와 동일한 runtime 표현을 갖는 것은 아니다. 외부 코드로 실행하는 만큼 compiler와 runtime도 신뢰해야 하는 기반에 추가된다.

## extraction command

다음 예제는 `add`를 정의한 뒤 target language를 OCaml로 정하고, `add`를 `add.ml` 파일로 추출한다.

```coq
From Corelib Require Import Extraction.

Fixpoint add (n m : nat) : nat :=
  match n with
  | O => m
  | S n' => S (add n' m)
  end.

Extraction Language OCaml.
Extraction "add.ml" add.
```

- `Extraction term`
  - 지정한 declaration만 출력
- `Recursive Extraction term`
  - dependency를 재귀적으로 포함해 출력
- `Separate Extraction`
  - dependency를 포함해 원래 module(파일)별로 나눠 출력
- `Extraction Library module`
  - module 단위 추출

## extraction 설정

- target language 선택
- optimization과 inlining
- 불필요한 인수 제거
- opaque proof 접근 정책
- constant realization
  - Rocq constant를 외부 language 구현에 연결
- inductive realization
  - Rocq datatype을 외부 datatype에 연결
- FFI code 생성 지원
- filename, module name 충돌 방지 설정

## axiom realization 주의

axiom에는 계산할 본문이 없으므로 추출된 프로그램에서 사용하려면 외부 구현을 제공해야 한다. 이 realization이 잘못되면 Rocq에서 증명한 의미와 실제 runtime 동작이 달라질 수 있다. 추출 전에 `Print Assumptions`로 논리적 의존성을 확인해야 하는 이유다. 증명에만 쓰이는 axiom은 제거될 수 있지만, informative axiom은 runtime 동작에 영향을 줄 수 있다.

## Rocq와 ML type 차이

extraction은 dependent type 정보의 상당 부분을 지운다. universe와 proof parameter, subset type의 proof component가 제거되고, higher-rank나 GADT에 가까운 구조는 target language에서 근사될 수 있다. singleton과 empty type이 어떻게 변환되는지도 주의해야 한다. extraction warning을 무시하지 말고 생성된 code가 type-check되는지 확인한다.

## 검증 경계

- Rocq가 보장하는 것
  - source definition과 theorem 사이의 형식적 관계
  - extraction transformation이 의도한 모델에 따른 code 생성
- 추가로 검증해야 하는 것
  - target compiler correctness
  - runtime, FFI 구현
  - input/output parser
  - resource exhaustion과 performance
  - axiom realization

end-to-end 보장이 필요하다면 verified compiler를 쓰고 FFI 경계를 좁게 유지하는 방안을 고려한다.

## program derivation

program derivation은 명세와 증명에서 계산 가능한 프로그램을 도출하는 접근이다. 구성적인 존재 증명에서 witness를 추출할 수 있으며, 이때 `sig`, `sigT` 같은 informative specification을 활용한다. `Prop`의 존재 명제는 일반적으로 witness 추출 대상이 아니므로 계산할 내용은 `Set`이나 `Type`에 남겨야 한다.

## functional induction

일반적인 구조적 귀납법이 함수의 실제 재귀 호출 구조와 맞지 않는다면, 함수 정의에서 맞춤 귀납 원리를 만드는 functional induction을 사용한다. well-founded recursion이나 중첩 재귀를 추론할 때 유용하다.

- 주요 기능
  - advanced recursive function 정의
  - functional scheme 생성
  - `functional induction` tactic
  - equation lemma 활용

다만 생성된 원리에 지나치게 의존하면 함수 정의를 바꿀 때 proof가 받는 영향이 커질 수 있다.

## library 작성 지침

- 공개 module path와 declaration name을 안정적인 API로 취급
- internal helper는 module 또는 locality로 숨김
- 최소 import와 명시적 export 사용
- notation scope를 별도 정의해 충돌 방지
- hint와 instance의 locality, priority 문서화
- deprecation attribute와 warning으로 migration 경로 제공
- axiom 사용과 extraction 가정 명시
- `.vo`만이 아니라 source, license, version constraint 함께 배포

## 호환성 관리

Rocq major version이 바뀌면 plugin API와 command 이름도 바뀔 수 있다. library source는 warning category와 deprecated syntax를 정기적으로 점검한다. plugin은 compiler와 Rocq ABI에 더 강하게 묶여 있다. CI matrix에서는 지원하는 Rocq version별로 build하는 것을 권장하며, 재현 환경은 lock file이나 opam switch export로 기록한다.
