# 프로그래밍 언어 의미론과 추론 규칙

> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Imp.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/Smallstep.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/plf-current/Equiv.html

## syntax

syntax는 언어에서 올바른 프로그램이 어떤 모양인지 정의한다. 사용자가 입력하는 문자열 형태를 concrete syntax라고 하고, parser가 이를 분석해 만든 트리 구조를 abstract syntax라고 한다. 의미론 정리는 보통 이 abstract syntax를 대상으로 한다.

## abstract syntax tree

expression과 command를 귀납적 data로 정의하면 abstract syntax tree를 구성할 수 있다. 예를 들어 다음과 같은 constructor를 둔다.

- number
- variable
- addition
- assignment
- sequence
- conditional
- loop

이 트리에 귀납법을 적용하면 프로그램의 모든 구문 형태를 다룰 수 있다.

## meta-variable

분석 대상 언어의 실제 변수와 그 언어를 설명하기 위한 수학적 변수는 구분해야 한다. 예를 들어 `t`, `t'`, `v`는 term이나 value를 나타내는 meta-variable로 자주 쓰인다. 따옴표나 글꼴로 구분하기도 하지만, 정확한 의미는 문맥에서 확인한다.

## judgment

judgment는 특정 관계나 성질이 성립한다는 주장을 형식화한 것이다. 다음은 평가, 타입, 프로그램 논리에서 사용하는 예다.

```text
t ⇓ v
t → t'
Γ ⊢ t : T
{P} c {Q}
```

각 judgment를 읽으려면 인수와 관계가 무엇을 뜻하는지 먼저 알아야 한다. 그 judgment가 성립한다는 증명은 derivation tree로 표현한다.

## inference rule

```text
premise₁   premise₂
────────────────── RuleName
     conclusion
```

추론 규칙은 모든 premise가 성립할 때 conclusion을 도출할 수 있음을 나타낸다. premise가 없는 규칙은 공리 형태의 base constructor가 된다. 이런 규칙은 귀납적 proposition의 constructor로 직접 표현할 수 있으며, 증명에서는 규칙 이름으로 각 case를 식별한다.

## derivation tree

derivation tree는 결론을 얻기 위해 적용한 규칙을 기록한다. 각 premise는 다시 하위 트리의 결론이 되고, leaf에는 premise가 없는 규칙이나 주어진 가정이 놓인다. 증명할 때는 이 도출 트리의 높이를 기준으로 귀납법을 적용할 수도 있다.

## big-step semantics

big-step semantics는 프로그램이 최종 결과로 평가되는 전체 과정을 하나의 관계로 표현한다.

```text
t ⇓ v
```

중간 상태가 관계 내부의 도출 과정에 들어가므로, 종료하는 실행을 간결하게 설명할 수 있다. 다만 이 방식으로는 divergence와 stuck을 구분하기 어려울 수 있다.

## small-step semantics

small-step semantics는 한 번의 계산 단계를 관계로 표현한다.

```text
t → t'
```

중간 상태를 직접 관찰할 수 있어 동시성이나 평가 순서를 표현하기에 적합하다. 여러 단계의 실행을 나타낼 때는 reflexive-transitive closure를 사용한다.

```text
t →* t'
```

## evaluation context

evaluation context는 expression 안에서 다음으로 계산할 위치를 나타낸다. context rule을 통해 하위 expression의 계산 단계를 전체 expression의 단계로 연결하고, 여기에 left-to-right나 call-by-value 같은 전략을 반영한다. 따라서 평가 순서는 의미론 자체의 일부가 될 수 있다.

## value

value는 평가가 완료된 정상 결과로 간주하는 구문이다. 언어에 따라 number, Boolean, lambda abstraction 등이 value에 해당한다. value는 보통 더 이상 계산되지 않지만, 더 계산할 수 없다는 이유만으로 value가 되는 것은 아니다. 오류 때문에 stuck 상태가 된 term도 더 진행할 수 없기 때문이다.

## normal form과 stuck

다음 단계가 없는 term을 normal form이라고 하며, 그중 value가 아닌 term은 stuck 상태다. 타입 안전성의 progress theorem은 타입이 올바른 closed term이 value이거나 다음 단계로 진행할 수 있다고 보장해 stuck 상태를 배제한다.

## determinism

determinism은 한 term에서 두 개의 다음 단계를 도출할 수 있더라도 그 결과가 같다는 성질이다.

```text
t → t₁ ∧ t → t₂ → t₁ = t₂
```

이를 증명하려면 규칙이 겹치는 경우와 평가 순서를 분석해야 한다. 의미론을 비결정적으로 설계할 수도 있으므로, 모든 언어에 당연히 성립하는 성질로 가정하지 않는다.

## termination과 divergence

유한한 단계 뒤 value나 최종 상태에 도달하면 termination, 계산이 끝없이 이어지면 divergence라고 한다. 모든 프로그램의 종료 여부를 결정하는 일반적인 알고리즘은 기대할 수 없다. 증명에서는 다루려는 성질에 맞춰 termination relation, coinduction, step-index, fuel 등을 사용한다.

## semantic equivalence

semantic equivalence는 두 프로그램이 관찰 가능한 면에서 같은 동작을 한다는 뜻이다. 구문이 똑같아야 한다는 조건보다 약해서 더 넓게 활용할 수 있지만, 무엇을 관찰할지는 명시해야 한다. 다음과 같은 기준을 사용할 수 있다.

- 같은 최종 결과
- 모든 초기 상태에서 같은 최종 상태
- 같은 종료 동작
- 모든 context에서 구분 불가능

## simulation

simulation은 한 의미론의 계산 단계를 다른 의미론이 따라갈 수 있음을 보이는 관계로, 컴파일러의 정확성과 최적화를 증명할 때 사용한다. forward simulation과 backward simulation은 보장 범위가 다르다. 특히 비결정성이나 divergence가 있으면 어떤 조건에서 대응이 성립하는지 확인해야 한다.

## rule induction

평가 증명에 귀납법을 적용하면 마지막 평가 규칙에 따라 case가 나뉜다. 정리가 평가 결과에 관한 것이라면 구문 자체에 귀납법을 적용하는 것보다 직접적인 접근일 수 있다. 각 premise에는 귀납 가설을 적용하고, 불가능한 도출은 inversion으로 제거한다.

## 자체 점검

- `1 + (2 + 3)`의 left-to-right small-step sequence 작성
- 같은 expression의 big-step derivation이 무엇을 숨기는지 설명
- normal form이지만 value가 아닌 term 예시 만들기
- 두 reduction rule이 같은 term에 동시에 적용되면 determinism proof에서 무엇을 확인해야 하는지 설명
- `→`와 `→*`의 차이를 0-step case로 설명
- program equivalence를 정의할 때 observation을 명시하지 않으면 생기는 문제 설명
