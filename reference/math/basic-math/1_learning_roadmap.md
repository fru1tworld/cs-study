# Software Foundations 수학 사전 지식 로드맵

> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Preface.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/toc.html

## 결론부터

LF와 PLF를 시작하기 위해 별도의 고급 수학 과목을 먼저 공부할 필요는 없다. 미적분, 선형대수, 확률론보다 다음 네 영역에 익숙해지는 것이 도움이 된다.

- 명제와 논리 기호 읽기
- 직접 증명, 경우 분석, 귀납법
- 집합, 함수, 관계
- 재귀적 정의와 간단한 함수형 프로그래밍

공식 서문도 논리나 프로그래밍 언어의 특정 배경을 전제로 삼지 않는다. 다만 수학적 정의와 논증을 읽어 본 경험이 있으면 내용을 따라가기 수월하다고 설명한다.

## 있으면 좋은 프로그래밍 배경

- 함수 정의와 호출
- immutable data
- algebraic data type 또는 enum
- pattern matching
- recursion
- higher-order function
- 변수의 scope와 binding

이 개념들을 아직 모르더라도 Logical Foundations의 앞부분에서 배울 수 있다. 명령형 언어에만 익숙하다면 기존 값을 바꾸는 대신 새 값을 만드는 함수형 프로그래밍 방식에 적응할 시간이 필요하다.

## 필요하지 않은 배경

- 미분과 적분
- 행렬 계산
- 복소수
- 해석학
- 추상대수의 군, 환, 체
- 확률과 통계
- category theory
- 고급 model theory

이 분야들은 나중에 연구 주제를 확장할 때 도움이 될 수 있다. 다만 LF와 PLF를 시작하기 전에 모두 익혀야 하는 것은 아니다.

## 책에서 실제로 만나는 수학

- Logical Foundations
  - inductive data와 recursive function
  - equality와 rewriting
  - proposition과 proof
  - inductively defined relation
  - map과 partial function
  - operational semantics
  - Hoare logic
- Programming Language Foundations
  - relation의 closure와 normalization
  - progress와 preservation
  - simply typed lambda calculus
  - subtyping과 polymorphism
  - logical relation의 기초

## 우선순위

### 반드시 익힐 내용

- 명제 기호와 quantifier
- implication proof의 구조
- equality와 substitution
- case analysis
- mathematical induction
- set membership
- function과 relation

### 진행하면서 익혀도 되는 내용

- constructive logic
- inductively defined proposition
- reflexive-transitive closure
- structural operational semantics
- typing judgment
- Hoare triple과 invariant

### 필요할 때 확장할 내용

- lambda calculus
- dependent type
- Curry–Howard correspondence
- logical relation
- denotational semantics
- separation logic

## 두 가지 학습 경로

### 바로 시작하는 경로

2~5번 문서를 읽은 뒤 Logical Foundations의 `Basics`를 시작한다. 각 장의 Rocq 코드를 직접 실행하면서 따라가고, 막히는 개념이 나오면 6~9번 문서에서 필요한 설명을 찾아본다.

### 수학을 먼저 다지는 경로

2~6번 문서의 자체 점검 문제를 종이에 풀고, 7~9번 문서로 PL 표기법을 익힌다. 이후 LF를 순서대로 진행하되, PLF를 시작하기 전에는 7~9번 내용을 다시 복습한다.

## 준비도 점검

- 다음 문장을 자연어로 읽을 수 있는가

```text
∀ n : ℕ, n + 0 = n
```

- `P → Q`를 증명할 때 무엇을 가정해야 하는지 아는가
- `P ∧ Q`를 증명하려면 몇 개의 하위 목표가 필요한지 아는가
- `P ∨ Q`라는 가정을 사용할 때 왜 경우를 나누는지 아는가
- 자연수 귀납법의 base case와 inductive step을 구분할 수 있는가
- 리스트에 대한 귀납이 `[]`와 `x :: xs` 두 경우라는 점을 이해하는가
- 함수의 injective와 surjective를 구분할 수 있는가
- relation이 Boolean function과 반드시 같지 않다는 점을 이해하는가

질문의 절반 이상에 답할 수 있다면 책을 바로 시작해도 된다. 답하기 어렵다면 2~5번 문서를 먼저 읽으며 기호와 증명 방식에 익숙해지는 편이 좋다.

## 공부할 때의 원칙

- 기호를 암기하기보다 문장으로 다시 읽기
- proof script보다 현재 goal과 hypothesis의 변화를 먼저 보기
- theorem을 만나면 quantifier와 implication을 바깥부터 분해
- 자동화 tactic 전에 작은 proof를 직접 구성
- 계산으로 끝나는 equality와 induction이 필요한 equality를 구분
- 종이 증명과 Rocq proof가 같은 논리 구조임을 계속 대응

## 용어 기준

Coq는 2025년부터 Rocq Prover로 이름이 바뀌었다. 전환 과정에서 Software Foundations에 두 명칭이 함께 나타날 수 있으므로, 이 문서에서는 도구 자체를 Rocq라고 부르고 기존 생태계나 경로를 설명할 때는 문맥에 따라 Coq를 함께 사용한다.
