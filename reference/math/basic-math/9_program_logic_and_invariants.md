# 프로그램 논리와 불변식

> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/Hoare.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/Hoare2.html

## program state

프로그램 상태는 변수 이름을 값에 대응시키는 map으로 표현할 수 있다. 이때 `st x`는 상태 `st`에서 변수 `x`의 값이다. assignment도 기존 상태를 파괴하는 대신 새로운 mapping을 만드는 것으로 모델링할 수 있으며, update theorem으로 갱신한 변수와 나머지 변수의 조회 결과를 구분한다.

## assertion

assertion은 프로그램 상태에 대한 명제다.

```text
Assertion := State → Prop
```

예를 들어 `X = 0`, `X ≤ Y`, 배열의 특정 구간이 정렬되어 있다는 조건이 assertion에 해당한다. 같은 조건이라도 현재 상태에 따라 참인지가 달라진다.

## precondition과 postcondition

precondition은 프로그램 실행 전에 요구하는 성질이고, postcondition은 정상 종료 뒤 보장하는 성질이다. 이 둘로 명세를 작성하면 구현 중간의 계산보다 외부에서 관찰할 계약에 집중할 수 있다.

## Hoare triple

```text
{P} c {Q}
```

`{P} c {Q}`는 `P`를 만족하는 상태에서 command `c`가 종료하면 결과 상태가 `Q`를 만족한다는 뜻이다. 기본 Hoare logic은 이처럼 partial correctness로 해석하므로, 종료 자체는 별도로 증명해야 한다.

## skip rule

```text
{P} skip {P}
```

`skip`은 상태를 바꾸지 않으므로 같은 assertion을 유지한다. 이 성질을 그대로 나타낸 가장 단순한 추론 규칙이다.

## assignment rule

assignment rule은 원하는 postcondition에서 출발해, 실행 전에 무엇이 참이어야 하는지 역으로 계산한다.

```text
{Q[X ↦ a]} X := a {Q}
```

`Q[X ↦ a]`는 assertion `Q`에서 `X`의 값을 expression `a`로 대체한 것이다. 여기서는 assertion을 치환하므로 프로그램 자체의 치환과 구분해야 한다.

## sequence rule

```text
{P} c₁ {R}    {R} c₂ {Q}
────────────────────────
      {P} c₁; c₂ {Q}
```

중간 assertion `R`은 첫 command의 postcondition이면서 두 번째 command의 precondition으로, 두 증명을 연결한다. backward reasoning에서는 `c₂`에 필요한 precondition부터 계산한다. 이렇게 찾은 중간 assertion이 전체 증명 구조를 결정한다.

## conditional rule

조건문의 두 branch는 guard가 참인 경우와 거짓인 경우로 나눠 증명한다. 각 branch의 precondition에 해당 guard 정보를 추가한다.

```text
{P ∧ b} c₁ {Q}
{P ∧ ¬b} c₂ {Q}
```

이때 Boolean으로 계산하는 guard를 assertion의 명제로 해석하는 연결이 필요하다.

## consequence rule

consequence rule로 이미 증명한 triple을 사용할 때는 implication 방향에 주의해야 한다. 강한 precondition에서만 성립하는 triple을 더 약한 precondition에서도 쓸 수 있다고 오해하기 쉽다. 필요한 방향은 다음과 같다.

- 새 precondition이 기존 precondition을 함의한다.
- 기존 postcondition이 새 postcondition을 함의한다.

방향이 헷갈린다면 각 조건을 만족하는 상태의 포함 관계를 그려 확인한다.

## loop invariant

loop invariant는 반복을 시작하기 전과 각 iteration 뒤에 유지되는 assertion이다. while을 증명하려면 이 불변식으로 다음 정보를 연결해야 한다.

- 초기 상태
- 반복 body의 보존 성질
- loop 종료 조건
- 최종 postcondition

## while rule

```text
{I ∧ b} c {I}
────────────────────
{I} while b do c {I ∧ ¬b}
```

`I`는 불변식으로, guard가 참일 때 body를 실행해도 유지되어야 한다. loop가 끝났다면 `I`와 guard의 부정을 함께 사용할 수 있다. 다만 이 규칙만으로는 loop가 종료한다는 사실까지 보장하지 않는다.

## invariant 찾기

1. 원하는 postcondition에서 시작
2. loop가 아직 끝나지 않았을 때 빠지는 정보를 변수 관계로 보충
3. 초기화 뒤 invariant가 성립하는지 확인
4. body 한 번 뒤 유지되는지 확인
5. invariant와 exit condition이 postcondition을 imply하는지 확인

## 예시 형태

원래 값 `n`까지 더하는 loop에서 현재 index를 `i`, 누산기를 `sum`이라고 하자. 최종 목표인 `sum = target`은 반복 중에는 유지되지 않으므로 그대로 불변식으로 쓸 수 없다. 대신 지금까지 처리한 구간과 남은 구간의 관계를 표현해야 한다. 초기값이 필요하다면 ghost variable로 보존할 수 있다.

## strengthening과 weakening

강한 assertion은 만족하는 상태가 더 적은 만큼 더 많은 정보를 제공한다. 약한 assertion은 더 많은 상태에서 성립하지만 제공하는 정보는 적다. consequence rule에서는 필요한 triple에 맞게 precondition을 강화하고, 이미 증명한 postcondition을 원하는 보장으로 약화한다.

## weakest precondition

weakest precondition은 command `c` 실행 뒤 `Q`를 보장하기 위한 가장 약한 precondition이다.

```text
wp c Q
```

이를 이용하면 assignment와 sequence의 backward reasoning을 체계적으로 진행할 수 있다. 조건문에서는 branch별 조건을 결합하며, loop에서는 일반적으로 불변식 탐색과 fixpoint 문제까지 다뤄야 한다.

## partial correctness와 total correctness

partial correctness는 프로그램이 종료한다면 postcondition이 성립한다는 뜻이다. total correctness는 종료 자체와 종료 뒤의 postcondition을 모두 보장한다. 무한 loop는 종료하는 경우가 없어서 많은 partial-correctness triple을 공허하게 만족할 수 있으므로, 명세에서 어느 의미를 사용하는지 밝혀야 한다.

## termination 증명

종료를 증명할 때는 각 iteration에서 well-founded order를 따라 엄격히 감소하는 variant나 ranking function을 사용한다. 자연수를 척도로 삼았다면 body를 실행할 때마다 값이 감소하면서 0 아래로 내려가지 않음을 보인다. 중첩된 loop에서는 lexicographic measure가 필요할 수 있다.

## soundness와 completeness

soundness는 논리의 규칙으로 증명한 triple이 의미론에서도 유효하다는 성질이다. relative completeness는 assertion language에서 필요한 사실을 증명할 수 있다고 가정할 때, 모든 유효한 triple을 논리로 증명할 수 있다는 성질이다. 규칙을 사용하기 편리한지와 그 규칙이 의미론적으로 타당한지는 구분해서 살펴야 한다.

## 자동 검증 조건 생성

주석을 붙인 프로그램에서 verification condition을 생성하면, theorem prover나 SMT solver가 남은 논리적 증명 과제를 해결할 수 있다. 사용자가 제공해야 하는 핵심 정보는 주로 loop invariant다. 자동 검증에 성공했더라도 명세와 불변식이 의도한 성질을 담고 있는지는 사람이 검토해야 한다.

## Software Foundations 진입 체크리스트

- `State → Prop`을 assertion으로 읽을 수 있음
- Hoare triple이 termination을 자동 보장하지 않음을 이해
- assignment rule이 backward substitution인 이유를 설명 가능
- sequence의 intermediate assertion 역할 이해
- loop invariant의 initialization, preservation, exit 세 조건 구분
- stronger와 weaker assertion의 방향 구분

## 자체 점검

- `{X = 0} X := X + 1 {X = 1}`을 assignment rule로 설명
- 두 assignment sequence의 중간 assertion 찾기
- `if`의 두 branch에 guard와 guard의 부정이 추가되는 이유 설명
- `while true do skip`이 partial correctness와 total correctness에서 어떻게 다른지 설명
- 자연수 countdown loop의 invariant와 termination variant 작성
- consequence rule에서 precondition implication 방향을 집합 포함 관계로 설명

## 학습 완료 기준

2~5번 문서를 이해했다면 Logical Foundations를 시작하기에 충분하다. 6번은 Rocq의 proof-as-program 관점을 보충하고, 7~9번은 Programming Language Foundations에서 만날 표기와 정리를 미리 살펴보는 용도다. 모든 내용을 외우기보다 실제 `.v` 파일에서 정의와 정리를 실행하며 익혀 나가면 된다.
