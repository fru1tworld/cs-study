# 집합, 함수, 관계

> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Maps.html
>
> 원문: https://softwarefoundations.cis.upenn.edu/current/lf-current/Rel.html

## 집합

집합은 어떤 영역(domain)의 원소를 모은 것이다. x가 집합 A에 속하면 `x ∈ A`, 속하지 않으면 `x ∉ A`로 쓴다. 외연적(extensional) 관점에서는 두 집합에 속하는 원소가 정확히 같으면 같은 집합으로 본다.

증명 보조 도구에서는 원소가 집합에 속하는지를 나타내는 술어(predicate)로 집합을 표현할 수 있다.

```text
A : X → Prop
x ∈ A  ≈  A x
```

## 부분집합

`A ⊆ B`는 모든 x에 대해 `x ∈ A`이면 `x ∈ B`라는 뜻이다. 한쪽으로만 포함될 수 있으므로 집합이 같다는 조건보다 약하다. A가 B에 포함되고 B도 A에 포함됨을 보이면 두 집합의 원소가 같다는 외연적 동등성을 얻는다.

## 집합 연산

- union `A ∪ B`
  - `A` 또는 `B`에 속함
- intersection `A ∩ B`
  - `A`와 `B`에 모두 속함
- difference `A \ B`
  - `A`에 속하고 `B`에는 속하지 않음
- complement
  - 정한 universe 안에서 `A`에 속하지 않는 원소
- Cartesian product `A × B`
  - pair `(a, b)`의 집합

## 함수

함수는 각 입력에 정확히 하나의 출력을 대응시킨다. `f : A → B`에서 A는 정의역(domain), B는 공역(codomain)이며, x에 f를 적용한 값은 `f x`로 쓴다. 함수 자체도 일급 값(first-class value)으로 다룰 수 있다.

## 합성

`f : A → B`, `g : B → C`라면 `(g ∘ f)(x) = g(f(x))`로 합성한다. 표기에서는 g가 먼저 나오지만 실제 계산은 f를 적용한 뒤 g를 적용하므로 순서를 주의해서 읽어야 한다. 항등 함수(identity function)와 합성 법칙은 함수형 프로그램을 변환할 때도 중요하다.

## injective

- 서로 다른 input이 같은 output으로 합쳐지지 않는 함수

```text
∀ x y, f x = f y → x = y
```

- equality의 constructor injectivity와 연결
- inverse가 있으면 injectivity를 보이기 쉬움

## surjective

- codomain의 모든 값이 어떤 input의 output으로 나타남

```text
∀ y, ∃ x, f x = y
```

- existential witness가 input 역할

## bijective

- injective이면서 surjective
- domain과 codomain 사이 one-to-one correspondence
- inverse function 구성과 밀접한 관계

## total function과 partial function

전함수(total function)는 정의역의 모든 입력에 출력이 존재한다. 부분 함수(partial function)는 일부 입력에서 결과가 정의되지 않는다. Rocq의 일반 함수는 전함수이므로, 부분성을 표현하려면 `Option B`를 사용하거나 정의역을 부분 타입(subtype)으로 제한하거나 입력과 출력 사이의 관계를 정의한다.

## finite map

유한 맵(finite map)은 키에서 값으로 가는 부분 함수로 볼 수 있다. 키가 있으면 조회 결과가 `Some value`이고 없으면 `None`이다. 값을 갱신한 뒤 같은 키를 조회하면 새 값을 얻고, 다른 키를 조회하면 이전 결과가 유지된다. 이런 성질을 이용해 프로그래밍 언어의 문맥과 상태를 표현한다.

## relation

- 여러 대상 사이의 성질
- binary relation

```text
R : A → A → Prop
```

`R x y`는 x와 y가 관계를 맺는다는 명제다. 함수와 달리 하나의 x가 여러 y와 관계를 맺거나, 어느 y와도 관계를 맺지 않을 수 있다.

## relation의 성질

- reflexive

```text
∀ x, R x x
```

- symmetric

```text
∀ x y, R x y → R y x
```

- transitive

```text
∀ x y z, R x y → R y z → R x z
```

- antisymmetric

```text
∀ x y, R x y → R y x → x = y
```

## equivalence relation

동치 관계(equivalence relation)는 반사적(reflexive), 대칭적(symmetric), 추이적(transitive)인 관계다. 등식이 대표적인 예이며, 관찰 가능한 동작이 같은 프로그램들을 묶을 때도 동치 관계를 쓴다. 이 경우에는 구문 자체가 같은지와 의미가 같은지를 구분해야 한다.

## preorder와 partial order

- preorder
  - reflexive
  - transitive
- partial order
  - preorder
  - antisymmetric
- natural number의 `≤`, set inclusion 등이 대표 사례
- subtyping은 설정에 따라 preorder로 다뤄질 수 있음

## deterministic relation

- 하나의 input이 관계 맺는 output이 최대 하나

```text
∀ x y₁ y₂, R x y₁ → R x y₂ → y₁ = y₂
```

이 조건은 small-step 평가가 결정적이라는 정리에 쓰인다. 관계 자체를 함수로 만드는 것은 아니지만, 결과가 있다면 그 결과가 유일함을 보장한다.

## normal form

- relation `R`로 더 이상 이동할 수 없는 값

```text
normal_form R x := ¬∃ y, R x y
```

이 정의는 정상적인 값과 진행할 수 없는 식(stuck term)을 구분할 때 사용한다. 더 이상 이동할 수 없다는 조건만으로 성공적인 값이라고 판단할 수는 없다.

## reflexive-transitive closure

반사 추이 폐쇄(reflexive-transitive closure)는 관계의 단계를 0번 이상 적용하는 것을 뜻한다. 0단계도 허용하므로 반사적이고, 여러 단계를 이어 붙일 수 있으므로 추이적이다. small-step 의미론에서는 다음처럼 여러 단계의 실행을 나타낼 때 쓴다.

```text
t →* t'
```

한 단계의 실행 `→`와 0단계 이상을 포함하는 `→*`를 구분해서 읽어야 한다.

## relation 합성과 역

- 합성
  - 어떤 중간 대상 `y`가 있어 `R x y`와 `S y z`
- inverse relation
  - argument 순서를 뒤집음
- evaluation을 거꾸로 돌리는 관계가 항상 function이나 deterministic한 것은 아님

## 자체 점검

- `f n = n + 1`이 자연수에서 injective인지 증명 개요 작성
- 같은 함수가 자연수에서 surjective인지 판단
- equality가 equivalence relation인 이유 설명
- `<`가 reflexive인지, transitive인지 판단
- `≤`가 symmetric인지, antisymmetric인지 판단
- finite map update 뒤 같은 key와 다른 key lookup 결과 적기
- deterministic relation과 total function의 차이 설명
