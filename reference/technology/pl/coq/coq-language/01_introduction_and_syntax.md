# Rocq 소개 및 기본 구문

## 문서 범위

> 원문: https://rocq-prover.org/doc/v9.1/refman/index.html

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/basic.html

기준 문서는 The Rocq Prover 9.1.1 Reference Manual이다. Rocq는 9.0부터 Coq에서 바뀐 이름이다. 이 문서들은 도구 이름을 Rocq로 쓰고, Coq는 `_CoqProject`처럼 실제 식별자에 남아 있는 경우와 이름 전환을 설명할 때만 쓴다.

- 이 문서의 범위
  - 증명 보조기의 역할과 신뢰 모델
  - 소스 파일, 문장, 식별자, 주석의 기본 규칙
  - 속성, 옵션, 플래그, 테이블
  - 환경을 조회하는 기본 명령

## Rocq의 역할

Rocq는 수학적 개념과 프로그램을 형식화하고 정리의 증명을 기계적으로 검사하는 대화형 정리 증명기, 즉 증명 보조기다. 보통 명제나 프로그램의 성질을 선언한 뒤 tactic으로 증명 목표를 단계적으로 변환한다. 이 과정에서 완성된 proof term은 kernel이 최종 검사한다.

- 대표적인 적용 사례
  - CompCert 검증 C 컴파일러
  - 4색 정리의 기계 검증
  - 프로토콜, 언어 의미론, 알고리즘 검증

## 작은 신뢰 기반

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/index.html

사용자가 작성한 표면 언어는 elaboration을 거쳐 핵심 언어인 Calculus of Inductive Constructions(CIC)로 변환된다. kernel은 이렇게 얻은 항이 기대한 타입을 갖는지 검사한다. 명제를 타입으로, 증명을 그 타입의 항으로 보는 Curry-Howard 대응에 따라 이 타입 검사가 증명의 검사로 이어진다.

tactic이나 자동화 도구, 외부 plugin이 잘못된 proof term을 만들더라도 kernel 검사를 통과할 수 없다. 이처럼 신뢰 대상을 작고 명확한 kernel로 제한하는 원칙을 de Bruijn criterion이라 부른다.

## 소스 파일과 문장

소스 파일의 기본 확장자는 `.v`이며, 파일은 command 또는 vernacular sentence의 연속으로 이루어진다. 일반 명령은 마침표 `.`로 끝나고, 그 뒤에는 공백이나 줄바꿈, 파일 끝이 와야 한다.

- 명령 처리 순서
  - lexing과 parsing
  - 이름 해석, 암시적 인수 추론, elaboration
  - 환경 갱신 또는 proof mode 진입
  - 완성된 선언의 kernel 검사

다음 예제에서는 뒤의 두 명령이 첫 명령에서 정의한 `double`을 사용한다는 점을 보면 된다.

```coq
Definition double (n : nat) : nat := n + n.

Compute double 4.
Check double.
```

- `Definition`: 전역 상수 정의
- `Compute`: 식을 계산해 출력
- `Check`: 항의 타입 확인

이처럼 파일의 앞선 명령이 만든 환경은 뒤 명령에 적용된다.

## 주석

주석은 `(*`와 `*)`로 감싸며 중첩할 수 있다. 다만 문자열 안에 있는 주석 구분자는 주석을 열거나 닫지 않고 문자열의 일부로 처리된다.

```coq
(* 바깥 주석
   (* 안쪽 주석 *)
*)
Definition answer := 42.
```

## 식별자와 이름

단순 식별자는 문자나 허용된 Unicode 문자로 시작하고, 그 뒤에는 문자, 숫자, 밑줄, 프라임 등을 쓸 수 있다. 예약어는 식별자로 사용할 수 없다.

qualified name은 `Stdlib.Lists.List.map`처럼 module 경로를 마침표로 연결한 이름이다. 현재 scope에서 모호하지 않다면 가장 짧은 이름인 shortest qualified identifier를 써도 되지만, 같은 짧은 이름이 여러 module에 있으면 qualification이 필요하다. 어떤 이름이 무엇을 가리키는지는 import, module, section, open scope의 영향을 받는다.

## 문자열과 숫자

문자열 literal은 큰따옴표로 감싸고, 문자열 안의 큰따옴표는 두 번 써서 표현한다. 숫자는 notation scope에 따라 `nat`이나 `Z` 등 서로 다른 수 타입으로 해석될 수 있으므로, 특정 scope를 지정하려면 `%scope`를 붙인다.

```coq
Check 10%nat.
Check "rocq"%string.
```

## 설정의 네 종류

> 원문: https://rocq-prover.org/doc/v9.1/refman/language/core/basic.html#settings

- attribute
  - 바로 뒤 command의 동작을 수정함
  - `#[...]` 형태로 선언함
  - locality, deprecation, universe 동작 등을 제어함
- flag
  - 켜짐 또는 꺼짐 상태를 가짐
  - `Set`, `Unset`, `Test`로 관리함
- option
  - 문자열, 숫자 등 값을 가짐
  - `Set`, `Add`, `Remove` 등 해당 option의 명령으로 관리함
- table
  - 여러 값을 누적하는 전역 또는 지역 상태임
  - hint database, search blacklist 등이 대표적임

다음은 flag를 관리하는 세 명령을 `Printing All`에 적용한 예다.

```coq
Set Printing All.
Unset Printing All.
Test Printing All.
```

## locality

locality는 설정이나 선언이 영향을 미치는 범위를 제어한다. 자주 보는 형태는 다음과 같다.

- `Local`: 현재 section 또는 module 밖으로 내보내지 않음
- `Global`: import한 사용자에게도 효과가 전달될 수 있음
- `Export`: module을 export할 때 효과 전달

지원하는 locality attribute는 command마다 다르다. library를 작성할 때는 설정이 어디까지 영향을 미치는지 드러나도록 암묵적인 기본 범위에 의존하기보다 locality를 명시하는 것을 권장한다.

## 환경 조회 명령

> 원문: https://rocq-prover.org/doc/v9.1/refman/proof-engine/vernacular-commands.html

- `Check term`
  - elaboration된 항과 타입 확인
- `Print name`
  - 정의, 정리, 귀납 타입의 본문과 인수 확인
- `About name`
  - 선언 정보와 implicit argument 등 요약 확인
- `Locate name`
  - 표기가 가리키는 fully qualified name 확인
- `Search pattern`
  - 환경에서 타입 패턴과 맞는 정리 검색
- `SearchRewrite pattern`
  - 재작성에 사용할 등식 검색
- `Inspect num`
  - 내부 개체 정보 조회에 사용함
- `Print All`
  - 현재 환경 전체 출력 → 큰 프로젝트에서는 주의 필요

```coq
Check Nat.add.
Print Nat.add.
Locate "+".
Search (_ + 0 = _).
```

## 오류와 경고

- parsing 오류
  - 문법에 맞지 않거나 문장 종료가 잘못된 경우 발생함
- elaboration 오류
  - 이름을 찾지 못하거나 implicit argument를 추론하지 못한 경우 발생함
- typing 오류
  - 실제 타입과 기대 타입이 일치하지 않는 경우 발생함
- universe inconsistency
  - universe 제약을 동시에 만족할 수 없는 경우 발생함

오류와 달리 warning은 명령이 성공했을 때도 출력될 수 있다. 표시할 warning category는 `Set Warnings`로 조정한다.

## 최소 파일 예시

```coq
From Stdlib Require Import Arith.

Definition square (n : nat) : nat := n * n.

Example square_four : square 4 = 16.
Proof.
  reflexivity.
Qed.
```

- `From Stdlib Require Import Arith`: library module을 load하고 이름을 현재 scope로 가져옴
- `Example`: theorem과 같은 방식의 증명 선언
- `Proof`: proof mode 진입
- `Qed`: proof term을 검사한 뒤 opaque constant로 저장
