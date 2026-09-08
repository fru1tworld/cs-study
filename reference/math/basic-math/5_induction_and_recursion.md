# 귀납적 정의와 재귀

> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Basics.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Induction.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/IndProp.html

## 귀납적 정의

귀납적 정의는 유한한 constructor 규칙으로 만들 수 있는 가장 작은 대상을 정의한다. 자연수라면 `0`이 자연수이고, `n`이 자연수일 때 `S n`도 자연수라는 두 규칙을 둔다. 이 규칙으로 만들 수 없는 값은 자연수에 포함하지 않는다. 따라서 생성 규칙을 보면 data를 만드는 방법뿐 아니라 어떤 case를 다뤄야 하는지도 알 수 있다.

## 자연수 구조

모든 자연수는 `0` 또는 `S n`이라는 두 constructor 형태 중 하나다. 산술 표기는 이 구조 위에서 정의되며, 자연수를 다루는 recursion과 induction도 두 형태에 맞춰 구성된다.

## 구조적 재귀

구조적 재귀는 입력의 constructor를 pattern matching한 뒤, 그 안에 있는 더 작은 recursive field에 재귀 호출을 적용한다. termination checker는 이 호출에서 대상이 구조적으로 작아지는지 확인한다.

```text
add 0 m     = m
add (S n) m = S (add n m)
```

위 정의는 첫 번째 인수를 기준으로 재귀한다. 이처럼 어느 인수를 기준으로 정의했는지에 따라 simplification이 진행되는 방향도 달라진다.

## 리스트 구조

리스트는 빈 리스트 `[]` 또는 원소와 꼬리를 묶은 `x :: xs`로 구성된다. 여기서 재귀적으로 다룰 부분은 꼬리 `xs`이므로, 리스트 함수도 두 constructor에 맞춰 다음과 같이 정의한다.

```text
length []       = 0
length (x::xs)  = S (length xs)
```

## 구조적 귀납법

구조적 귀납법은 recursive data의 모든 값에 대해 성질이 성립함을 보이는 증명 원리다. 리스트의 성질 `P xs`를 증명하려면 다음 두 경우를 다룬다.

- `P []`
- 임의의 `x`, `xs`에 대해 `P xs → P (x :: xs)`

두 번째 경우에는 꼬리 `xs`에 대한 귀납 가설을 사용할 수 있다. 함수 정의에서 `xs`를 재귀 호출한 것과 증명에서 귀납 가설을 사용하는 부분이 대응한다.

## induction principle

귀납적 선언에서는 해당 구조를 분석할 수 있는 elimination principle이 자동으로 생성된다. 자연수의 induction principle은 보통 다음 형태다.

```text
P 0 →
(∀ n, P n → P (S n)) →
∀ n, P n
```

증명하려는 정리를 이 형태에 맞게 일반화해 두면 귀납 가설을 적용하기 쉽다.

## 중첩 구조

트리처럼 recursive child가 여러 개인 constructor에서는 각 child에 대한 귀납 가설을 얻을 수 있다. 서로 의존하는 data를 함께 정의했다면 그 관계를 반영한 induction principle이 필요할 수 있다. 중첩된 data에서도 기본 원리만으로 필요한 가설을 얻기 어렵다면 custom induction principle을 사용할 수 있다.

## 귀납적 proposition

constructor로 정의할 수 있는 대상에는 data뿐 아니라 증명 관계도 있다. 예를 들어 짝수라는 성질은 0이 짝수이고, `n`이 짝수라면 `S (S n)`도 짝수라는 규칙으로 정의할 수 있다.

이 성질의 proof object에는 어떤 constructor를 거쳐 증명했는지가 담긴다. 따라서 proposition의 증명에 귀납법을 적용하면 그 도출 구조를 따라가게 된다.

## inference rule로 읽기

```text
────────
Even 0

Even n
──────────
Even (n+2)
```

선 위에는 전제(premise), 아래에는 결론(conclusion)을 쓴다. 각 규칙에 이름을 붙이고 연결하면 derivation tree가 된다. 이 표기는 귀납적 proposition의 constructor와 같은 정보를 담고 있다.

## derivation induction

`Even n`의 증명에 귀납법을 적용하면 마지막에 사용한 규칙을 기준으로 경우를 나눈다. 이는 자연수 `n` 자체에 귀납법을 적용할 때와 다른 정보를 제공한다. 의미론 증명에서 evaluation derivation을 귀납 대상으로 자주 택하는 것도 이 때문이다. 증명하려는 정리에 어떤 정보가 필요한지 보고 대상을 선택한다.

## strong induction

강한 귀납법에서는 `P n`을 보일 때 모든 `m < n`에 대한 가설을 사용할 수 있다. 재귀 호출이 바로 이전 값이 아니라 임의의 더 작은 값으로 진행되는 알고리즘을 증명할 때 편리하다. 일반 귀납법으로 유도할 수 있으며, well-founded induction을 자연수에 적용한 특수한 경우로도 볼 수 있다.

## well-founded induction

well-founded induction은 무한히 내려갈 수 없는 관계를 기준으로 한다. 종료를 보이는 척도를 자연수 하나로 표현하기 어렵다면 lexicographic order나 multiset order 같은 관계를 활용할 수 있다. Software Foundations 초반에는 구조적 귀납법을 중심으로 익히고, 이후 이런 방식으로 확장한다.

## recursion과 termination

논리적 일관성을 유지하려면 일반 재귀를 무제한으로 허용할 수 없다. 종료를 다루는 대표적인 방식은 다음과 같다.

- structural recursion
- well-founded recursion
- 명시적 fuel parameter

fuel을 사용한다면 값이 0이 되어 계산을 중단하는 경우도 명세에 반영해야 한다.

## accumulator와 invariant

누산기를 사용하는 꼬리 재귀 함수에서는 원하는 정리에 곧바로 귀납법을 적용했을 때 가설과 목표가 맞지 않을 수 있다. 이때는 누산기가 어떤 값이든 성립하는 더 일반적인 보조 정리를 먼저 증명한다.

```text
rev_append xs acc = reverse xs ++ acc
```

위처럼 임의의 `acc`에 대한 불변식을 증명해 두면, 필요한 값을 대입해 원래 정리를 얻을 수 있다.

## simultaneous induction

서로 의존하는 성질 중 하나만 증명하려 하면 귀납 단계에 필요한 정보가 부족할 수 있다. 이때는 정리들을 conjunction으로 묶거나 mutual principle을 사용해 함께 증명한다.

## 흔한 오류

- size가 줄지 않는 recursive call
- inductive hypothesis를 현재 대상 자체에 적용
- recursive field가 아닌 값을 induction
- relation proof가 필요한데 underlying data만 induction
- accumulator를 고정한 뒤 induction해 hypothesis가 약해짐
- constructor가 만들 수 없는 case를 별도 근거 없이 가정

## 자체 점검

- 자연수 덧셈의 어느 argument가 재귀 argument인지 definition을 보고 찾기
- `length (xs ++ ys) = length xs + length ys`의 induction 대상 선택
- binary tree의 node에 두 child가 있다면 inductive hypothesis가 몇 개인지 설명
- even derivation induction과 자연수 induction의 case 차이 설명
- tail-recursive sum의 accumulator invariant 작성
- 종료하지 않을 수 있는 일반 재귀가 proof system의 consistency에 왜 위험한지 생각
