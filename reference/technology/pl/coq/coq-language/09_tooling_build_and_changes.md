# Rocq 도구, 빌드, 변경 사항

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/practical-tools/utilities.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/practical-tools/coq-commands.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/using/tools/coqdoc.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/practical-tools/coqide.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/addendum/parallel-proof-processing.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/appendix/history-and-changes/index.html

- 설치, project path, build, CLI, 문서화, IDE, 병렬 proof, 9.x 변경 정리

## 명령 체계

현재 통합 진입점은 `rocq`이며, 주요 하위 명령은 다음과 같다.

- `rocq repl`: 대화형 REPL
- `rocq compile`: `.v`를 `.vo`로 컴파일
- `rocq makefile`: project Makefile 생성
- `rocq dep`: module dependency 계산
- `rocq doc`: source에서 HTML, LaTeX 문서 생성
- `rocq check`: compiled library 검사
- `rocq wc`: source 통계

문서나 script에는 과거 command인 `coqtop`, `coqc`, `coq_makefile`, `coqdep`, `coqdoc`, `coqchk`가 남아 있을 수 있다. 다음은 `rocq`로 version을 확인하고, REPL을 실행하고, 파일을 컴파일하는 예시다.

```console
$ rocq --version
$ rocq repl
$ rocq compile Foo.v
```

## 설치와 환경 격리

opam을 이용한 일반적인 흐름은 project용 switch를 만들고, 그 switch의 환경을 활성화한 뒤 `rocq-prover`를 설치하는 것이다.

```console
$ opam switch create rocq-project ocaml-base-compiler.5.2.1
$ eval $(opam env --switch=rocq-project)
$ opam install rocq-prover
```

실제 package 이름과 지원하는 OCaml 버전은 사용할 Rocq 릴리스의 metadata에서 확인한다. 프로젝트별 switch를 두면 Rocq와 plugin 버전을 고정하고 다른 프로젝트와의 의존성 충돌을 피할 수 있어 CI 재현성도 높아진다. 구성한 환경은 `opam switch export`로 기록할 수 있다.

## logical path와 physical path

Rocq의 module name은 filesystem path만으로 결정되지 않는다. physical path와 logical path의 연결은 다음 option으로 지정한다.

- `-Q physical logical`
  - 하위 디렉터리까지 logical prefix 아래에 연결
  - 불러올 때 `MyProject.Sub.File`처럼 prefix부터 적어야 함
- `-R physical logical`
  - `-Q`처럼 하위 디렉터리까지 연결
  - 추가로 prefix를 생략한 짧은 이름(`Require Import File`)도 허용함

같은 source를 서로 다른 logical path로 두 번 load하면 type identity 문제가 생길 수 있다. 다음 명령은 `theories` 디렉터리를 logical prefix `MyProject`에 연결한 상태로 `theories/Util.v`를 컴파일한다.

```console
$ rocq compile -Q theories MyProject theories/Util.v
```

source 내부에서는 이 module을 `From MyProject Require Import Util.`처럼 불러온다.

## `_CoqProject`

`_CoqProject`에는 project source, include path, logical mapping, compiler option을 기록한다. 다음 예시는 `theories`를 `MyProject`에 연결하고, project에 포함할 source file 두 개를 나열한다.

```text
-Q theories MyProject
theories/Util.v
theories/Main.v
```

이 파일을 바탕으로 `rocq makefile -f _CoqProject -o Makefile.coq`를 실행하면 build file이 생성된다. 작성할 때는 공백, 인용, 금지된 filename 규칙을 확인해야 한다. 생성된 Makefile은 직접 수정하지 말고 local extension file을 활용하는 것을 권장한다.

## `rocq makefile`

다음은 `_CoqProject`에서 `Makefile.coq`를 생성한 뒤, 그 Makefile로 4개 job을 병렬 실행해 build하는 예시다.

```console
$ rocq makefile -f _CoqProject -o Makefile.coq
$ make -f Makefile.coq -j4
```

생성된 Makefile은 의존성 순서를 자동으로 계산하고 `.vo`, `.vos`, `.vok`, 문서화 target을 제공한다. timing target으로 성능 회귀를 측정할 수도 있다. 전체가 아니라 일부 target만 병렬로 빌드하려면 `make only TGTS="foo.vo bar.vo" -j`처럼 대상을 지정한다.

사용자 rule은 `CoqMakefile.local`과 `CoqMakefile.local-late`로 추가한다. clean target은 생성물을 제거하므로 직접 작성한 소스와 생성 파일의 경계를 구분해야 한다.

## Dune build

Dune은 OCaml과 Rocq가 섞인 프로젝트나 여러 package를 함께 관리할 때 유용하다. theory stanza에 package, namespace, plugin 의존성을 선언하면 의존성 그래프와 증분 빌드, 설치 rule을 통합해 관리할 수 있다. 다만 지원하는 stanza와 extension 버전은 Rocq 버전에 맞춰 확인해야 한다. opam package metadata와 함께 사용하면 배포도 자동화할 수 있다.

## compiled file

- `.vo`
  - statement와 opaque proof를 포함한 완전 compiled object
- `.vos`
  - opaque proof 본문을 생략하고 statement 중심으로 빠르게 처리한 interface
- `.vok`
  - `.vos` 경로로 proof가 검증됐음을 나타내는 marker
- `.glob`
  - documentation과 identifier reference 정보

source timestamp나 dependency가 바뀌면 다시 컴파일해야 한다.

## REPL

다음은 `-Q`로 logical path를 지정해 REPL을 시작한 뒤 `MyProject`의 `Main` module을 불러오는 모습이다.

```console
$ rocq repl -Q theories MyProject
Welcome to Rocq ...
Rocq < From MyProject Require Import Main.
```

REPL은 command를 한 문장씩 실행한다. 시작할 때 사용할 수 있는 주요 option은 다음과 같다.

- `-load-vernac-source`로 source load 가능
- `-require-import`로 시작 시 module import 가능
- `-q`로 사용자 rc file load 방지
- `-init-file`로 별도 startup script 지정

batch mode는 script와 CI 진단에 유용하다.

## batch compilation

- `rocq compile file.v`
  - `.vo` 생성
- `-Q`, `-R`
  - logical path 설정
- `-vos`
  - proof를 건너뛴 interface 생성
- `-vok`
  - `.vos` dependency를 사용해 opaque proof 검사
- `-time`, `-time-file`
  - command별 시간 기록
- `-profile-ltac`
  - tactic profiling

대규모 project에서는 파일을 직접 나열하기보다 build tool을 사용하는 것을 권장한다.

## dependency와 검사

- `rocq dep`
  - `Require` 관계 분석
- `rocq check`
  - `.vo`의 typing과 dependency consistency 검사
  - plugin code를 실행하지 않고 proof object 확인 가능

검증 artifact를 배포할 때는 `rocq check`를 별도 CI 단계로 두는 방식이 유용하다.

## `rocq doc`

`rocq doc`은 literate comment와 Rocq source를 HTML, LaTeX로 변환한다. 문서 comment와 section, list, 강조, verbatim, hyperlink를 지원하고, code 표시를 숨기거나 특정 영역만 노출할 수도 있다. identifier link는 `.glob` 정보를 이용해 생성하며, output 형식과 경로, index는 command line option으로 조정한다. 예를 들어 `Foo.v`를 HTML로 변환하려면 다음과 같이 실행한다.

```console
$ rocq doc --html Foo.v
```

## IDE와 editor

- RocqIDE
  - source buffer와 goal view 제공
  - 문장 단위 앞, 뒤 실행
  - 비동기 mode, query, debugger, Unicode input 지원
- VsRocq
  - VS Code, VSCodium용 language server 기반 extension
  - `_CoqProject` 인식, continuous checking, proof view 제공
- Proof General
  - Emacs 기반 상호작용 환경

어떤 editor를 쓰든 editor와 shell이 서로 다른 opam environment를 사용하지 않는지 확인해야 한다.

## asynchronous proof processing

비동기 proof 처리는 opaque proof를 별도 worker에 맡겨 문서 처리 지연을 줄인다. proof annotation으로 처리 전략을 제어하고 worker 수도 제한할 수 있으며, proof block의 경계는 오류 복구와 병렬성에 영향을 준다.

병렬 mode에서도 최종 proof는 kernel 검사를 거친다. 다만 tactic의 전역 side effect나 일부 plugin 때문에 비동기 처리가 제한될 수 있다.

## profiling

- command timing
  - 느린 declaration 식별
- Ltac profiling
  - tactic call tree와 누적 시간 확인
- memory option과 OCaml runtime 통계
- native computation precompile
  - 반복 계산 비용 절감 가능

안정적인 benchmark를 얻으려면 clean build와 incremental build를 구분해야 한다.

## Coq에서 Rocq로의 전환

9.0부터 프로젝트 명칭과 command 체계가 Rocq 중심으로 바뀌었다. 표준 library prefix는 `Coq`에서 `Stdlib`으로 바뀌었지만, `From Coq`는 deprecated 경고와 함께 계속 동작하고 `_CoqProject` 같은 파일 이름도 그대로 쓰인다. 따라서 전환할 때는 다음 항목을 확인한다.

- executable 이름
- opam package 이름
- website와 documentation URL
- CI image와 editor extension
- deprecated command warning

이름을 기계적으로 일괄 rename하기보다 9.0의 porting, renaming advice를 확인하는 것을 권장한다.

## 9.1 변경 확인

> 원문: https://rocq-prover.org/doc/v9.1/refman/changes.html

release note는 다음 범주로 구성된다.

- kernel
- specification language와 type inference
- notation
- tactic, Ltac, Ltac2, SSReflect
- command와 option
- CLI와 RocqIDE
- Corelib, infrastructure, extraction

minor release에서도 parsing, warning, 타입 추론이 바뀔 수 있다. 따라서 업그레이드 전에는 clean build를 수행하고 warning을 검토하는 것을 권장한다.

## project 점검 목록

- Rocq, OCaml, plugin version 고정
- `_CoqProject` 또는 Dune에 logical path 단일 정의
- source에 `Admitted`와 예상하지 않은 axiom이 없는지 확인
- clean build와 `rocq check` 성공
- generated file을 version control 정책에 맞게 분리
- documentation과 editor 설정이 같은 logical path 사용
- major upgrade release note 검토
