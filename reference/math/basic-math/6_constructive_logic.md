# 구성적 논리

> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Logic.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/ProofObjects.html

## 구성적 증명

구성적 증명에서는 명제가 참임을 보일 직접적인 근거를 구성한다. disjunction을 증명할 때는 어느 쪽이 참인지 밝혀야 하고, existential을 증명할 때는 실제 witness를 제시해야 한다. implication의 증명도 입력으로 받은 증명을 출력 증명으로 바꾸는 방법을 담는다. 이렇게 구성한 증명은 계산 정보로 해석할 수 있다.

## Curry–Howard 대응

Curry–Howard 대응은 명제를 타입에, 증명을 프로그램에 대응시킨다. 주요 논리 연산을 이 관점에서 읽으면 다음과 같다.

- `P → Q`: `P`의 proof를 받아 `Q`의 proof를 반환하는 function
- `P ∧ Q`: 두 proof의 pair
- `P ∨ Q`: 어느 쪽인지 표시한 tagged value
- `True`: 값이 하나 있는 type
- `False`: 값이 없는 type
- `∀ x, P x`: 모든 `x`에 proof를 주는 dependent function
- `∃ x, P x`: witness와 proof의 묶음

## proof object

tactic은 proof term을 만드는 도구이며, 완성된 term은 kernel이 타입 검사한다. 같은 정리에도 여러 proof object가 존재할 수 있지만, 어느 script로 만들었든 최종적으로 신뢰하는 것은 생성된 term의 타입 검사다.

## implication as function

```text
P → Q
```

`P → Q`는 `P` 타입의 입력을 받아 `Q` 타입의 출력을 반환하는 함수처럼 읽을 수 있다. 이 관점에서 가정을 도입하는 것은 함수 인수를 받는 일에, 보조 정리를 적용하는 것은 함수를 호출하는 일에 대응한다.

## conjunction과 product

```text
P ∧ Q
```

`P ∧ Q`의 증명은 `P`와 `Q`의 증명을 모두 담은 pair를 만드는 것과 같다. 이 증명을 사용할 때는 projection이나 pattern matching으로 필요한 쪽을 꺼낸다. 논리적 conjunction이 product data type과 구조적으로 닮은 이유다.

## disjunction과 sum

```text
P ∨ Q
```

`P ∨ Q`의 증명은 왼쪽 또는 오른쪽 증명을 어느 쪽인지 나타내는 tag와 함께 담는다. 이를 사용하려면 두 constructor의 경우를 모두 처리해야 하므로, sum data type의 pattern matching과 대응한다.

## existential과 dependent pair

```text
∃ x : A, P x
```

존재 명제의 증명에는 witness `x`와 `P x`의 증명이 함께 들어간다. 다만 proof assistant의 `Prop`에 있는 existential은 논리적 정보로 취급되므로, witness를 임의의 계산 타입으로 꺼내는 데 제한이 있을 수 있다. 계산 결과로 사용해야 한다면 sigma type이나 subtype처럼 data 수준의 표현을 검토한다.

## 배중률

```text
P ∨ ¬P
```

모든 명제에 대해 `P ∨ ¬P`가 성립한다는 원리가 배중률(law of excluded middle)이다. 고전 논리에서는 일반적으로 사용하지만, 구성적 논리의 핵심 체계에서는 임의의 `P`에 대해 자동으로 주어지지 않는다. 다만 `P`가 decidable하다면 해당 명제에 대해서는 어느 쪽인지 계산할 수 있다.

## 이중 부정 제거

```text
¬¬P → P
```

이중 부정 제거는 고전 논리에서는 유효하지만 구성적 논리에서는 일반적으로 증명할 수 없다. 반대 방향인 `P → ¬¬P`는 구성적으로 증명할 수 있으므로, 증명에서 어느 방향을 사용하는지 구분해야 한다.

## 모순에 의한 증명

`¬P`를 증명하려고 `P`를 가정한 뒤 모순을 도출하는 방식은 구성적 논리에서도 허용된다. 반면 `P`를 증명하려고 `¬P`에서 모순을 도출했다면, 일반적으로 이중 부정 제거라는 고전 논리 원리가 필요하다. 두 방식은 필요한 전제가 다르다.

## decidability

명제 `P`가 decidable하다는 것은 `P`와 `¬P` 중 어느 쪽인지 계산해 선택할 수 있다는 뜻이다. 자연수의 equality나 order처럼 유한한 계산으로 판정할 수 있는 조건은 많지만, 모든 명제를 이렇게 판정할 수 있는 것은 아니다. Boolean 판정 함수가 있는 경우에는 그 함수의 정확성 정리를 함께 사용해 증명을 자동화할 수 있다.

## reflection

reflection은 논리 명제와 Boolean 계산을 연결한다.

```text
reflect P b
```

`reflect P b`는 `b = true`이면 `P`가, `b = false`이면 `¬P`가 성립한다는 연결 정보를 담는다. 이 연결을 증명해 두면 큰 증명을 검증된 계산으로 바꿀 수 있다. equality test를 활용할 때도 그 결과가 명제로서의 equality와 일치한다는 정리가 필요하다.

## proof irrelevance

proof irrelevance는 `Prop`에 속한 같은 명제의 증명들을 계산 관점에서 구분하지 않는 원리다. 어떤 경로로 증명했는지보다 명제가 증명되었다는 사실에 집중한다. 증명에 담긴 data를 실행 중에 관찰해야 한다면 `Prop`이 아니라 계산 가능한 타입에 정보를 두어야 한다.

## classical axiom 사용

Rocq에서는 필요한 고전 논리 공리를 명시적으로 import하거나 가정할 수 있다. 고전 논리를 사용한다고 proof assistant가 불건전해지는 것은 아니지만, 정리가 의존하는 공리와 계산적 내용은 달라진다. 구성적 정리로 유지하려면 필요한 범위를 넘어 고전 논리를 import하지 않도록 한다.

## Software Foundations에서 중요한 이유

이 관점을 익히면 Software Foundations의 정리를 프로그램처럼 읽을 수 있다. 귀납적 proposition의 constructor로 proof data를 만들고 existential witness를 직접 제시하는 연습이 반복되기 때문이다. Boolean 함수와 명제를 연결하는 정리도 자주 증명하며, 이 경험은 이후 권에서 타입 시스템과 logical relation을 이해하는 바탕이 된다.

## 자체 점검

- `P ∧ Q → Q ∧ P`를 proof term의 data 이동으로 설명
- `P → ¬¬P`의 function 구조 작성
- `¬¬P → P`에서 constructive하게 막히는 지점 설명
- `∃ n, P n` proof가 반드시 포함해야 하는 두 요소 적기
- `P ∨ Q` 가정을 사용하는 function이 처리해야 하는 case 적기
- Boolean equality test의 결과만으로 proposition equality를 증명하려면 어떤 theorem이 필요한지 설명
