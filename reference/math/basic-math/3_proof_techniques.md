# 기본 증명 기법

> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Tactics.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Induction.html

## 증명의 구성 요소

증명에서는 현재 보여야 하는 명제인 goal과, 이를 위해 사용할 수 있는 가정인 hypothesis를 함께 살펴본다. 각 proof step은 goal을 더 작은 목표로 나누거나 가정을 활용하는 단계다. 나뉜 하위 목표를 모두 증명하면 전체 증명이 끝난다.

## 정의에 의한 증명

정의를 펼쳐 계산하는 것만으로 양변이 같아지는 명제가 있다. Rocq의 definitional equality와 연결되는 경우로, 다음 식이 예다.

```text
double 2 = 4
```

이 식은 함수 정의를 펼치고 자연수를 계산하면 확인할 수 있다. 계산이 끝나지 않거나 변수가 남아 있다면 다른 정리를 사용해야 할 수 있다.

## 직접 증명

- 주어진 정의와 가정을 순서대로 사용해 결론 도출
- implication goal이면 antecedent를 가정
- conjunction goal이면 두 부분 분리
- universal goal이면 임의의 대상 도입
- existential goal이면 witness 선택

## equality rewriting

`x = y`가 있으면 식 안의 x를 y로 바꿀 수 있다. 목표의 구조에 맞춰 치환 방향을 선택하고, 바꾼 뒤에는 계산만으로 증명이 끝나는지 확인한다. 무작정 rewrite를 반복하면 goal이 더 복잡해질 수 있으므로 어떤 형태로 정리할지 먼저 정하는 편이 좋다.

## lemma 적용

이미 증명한 함의나 전칭 정리를 현재 goal에 적용할 수 있다. 정리의 결론이 goal과 맞으면, 그 결론에 필요한 전제들이 새 하위 목표가 된다. 이때 인자와 암묵적 매개변수에 어떤 값이 대입되는지 확인해야 한다. 큰 정리를 적용하는 것보다 goal의 형태에 맞는 작은 보조 정리(lemma)를 쓰는 편이 흐름을 이해하기 쉽다.

## 경우 분석

- 값의 가능한 constructor를 모두 나눠 처리
- Boolean
  - `true`
  - `false`
- 자연수
  - `0`
  - `S n`
- 리스트
  - `[]`
  - `x :: xs`
- 가능한 case를 빠뜨리지 않으면 전체 대상을 덮음

## 귀류법

`¬P`를 증명할 때는 P를 가정한 뒤 `False`를 도출한다. 고전 논리에서는 반대로 `¬P`를 가정해 모순을 이끌어 내는 방식으로 P를 증명할 수도 있다. 다만 구성적 논리에서는 후자의 방식이 항상 허용되는 것은 아니므로, 증명에 어떤 고전 논리 원리를 사용했는지 구분해야 한다.

## mathematical induction

수학적 귀납법은 무한히 많은 자연수의 경우를 유한한 증명 구조로 다룬다. 모든 자연수에 대해 `P n`을 보이려면 다음 두 단계를 증명한다.

- 기초 단계(base case): `P 0`
- 귀납 단계(inductive step): `P n → P (S n)`

귀납 단계에서는 더 작은 대상에 대한 귀납 가정(inductive hypothesis)을 사용할 수 있다. 따라서 이 가정이 현재 목표의 어느 부분에 필요한지 찾는 것이 중요하다.

## induction과 case analysis의 차이

경우 분석은 생성자(constructor)의 모양에 따라 경우를 나눌 뿐, 내부의 재귀적 데이터에 관한 가정을 주지는 않는다. 귀납법은 같은 방식으로 경우를 나누면서 재귀적 하위 데이터에 대한 귀납 가정도 제공한다. 후속자 `S n` 안의 n에 대한 성질이 필요하다면 귀납법을 고려할 수 있다.

## 일반화

귀납법을 적용하기 전에 변수를 너무 일찍 고정하면 귀납 가정을 필요한 곳에 쓰지 못할 수 있다. 이런 경우에는 정리를 더 일반적으로 진술하는 편이 증명하기 쉽다. 특정 누산기(accumulator) 값 대신 모든 누산기 값을 다루거나, 특정 리스트 꼬리 대신 임의의 꼬리를 다루는 식이다. 보여야 할 결론은 넓어지지만 귀납 가정을 재사용할 수 있는 범위도 넓어진다.

## 보조 정리

- main theorem에서 반복되거나 독립적인 핵심 성질을 lemma로 분리
- 좋은 lemma의 특징
  - 한 가지 개념만 표현
  - 재사용 가능
  - 필요한 만큼 일반적
  - main proof의 기술적 세부를 감춤
- accumulator를 쓰는 tail recursion proof에서는 별도 invariant lemma가 자주 필요

## inversion

Inversion은 귀납적으로 정의된 명제의 증명이 어떤 생성자에서 나왔는지 거꾸로 분석한다. 유도의 마지막 규칙을 살펴 불가능한 경우를 제거하고, 생성자 인자 사이의 등식을 얻는다. 단순히 데이터의 모양을 나누는 것과는 다르며, 타입 관계나 평가 관계를 증명할 때 자주 사용한다.

## contradiction 활용

문맥에 P와 `¬P`가 함께 있거나 서로 다른 생성자가 같다는 불가능한 가정이 있으면 모순을 도출할 수 있다. 이때 어떤 가정들이 모순을 일으켰는지 증명에 드러내면 나중에 증명을 고치기 쉽다.

## 증명 계획 세우기

1. theorem을 자연어로 다시 씀
2. 가장 바깥 connective에 맞춰 변수를 도입하거나 goal을 분리
3. recursive data가 있으면 어느 대상을 induction할지 결정
4. inductive hypothesis가 원하는 형태인지 확인
5. 각 case에서 계산과 rewriting 수행
6. 반복되는 부분을 lemma로 분리

## 흔한 실패

- 계산으로 끝나지 않는 goal에 reflexivity만 반복
- induction 대상이 아닌 주변 변수를 induction
- 필요한 변수를 induction 전에 고정
- disjunction hypothesis의 한 case만 처리
- existential witness 없이 property부터 증명하려 함
- theorem의 방향과 반대 방향을 암묵적으로 사용
- classical reasoning을 constructive theorem에서 몰래 가정

## 종이 증명과 tactic 대응

- 임의의 값을 택함
  - variable introduction
- 가정함
  - implication introduction
- 두 명제를 각각 보임
  - conjunction introduction
- 경우를 나눔
  - destruct 또는 case analysis
- 귀납 가정을 사용함
  - induction hypothesis 적용
- 양변에 같은 값을 대입함
  - rewrite 또는 substitution
- 정의에서 즉시 따름
  - simplification 또는 reflexivity

## 자체 점검

- 모든 자연수 `n`에 대해 `0 + n = n`을 증명할 때 induction이 필요한지 판단
- 모든 자연수 `n`에 대해 `n + 0 = n`을 증명할 때 definition만으로 끝나는지 판단
- list reverse가 length를 보존한다는 theorem의 induction 대상을 고르기
- `P ∨ Q → Q ∨ P`의 proof case를 적기
- `∃ n : ℕ, n + n = 6`의 witness와 남는 goal 적기
- accumulator 기반 reverse의 정확성을 위해 어떤 더 일반적인 lemma가 필요한지 생각
