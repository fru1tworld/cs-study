> 원본: https://rockthejvm.com/articles/scala-3-givens-and-implicits

# Scala 3: Given과 Implicit의 상호 운용

## 소개

짧은 글이다. 여기서는 given 인스턴스와 using 절 조합이 기존 implicit 기반 메커니즘과 어떻게 함께 동작하는지 살펴본다.

## 배경과 동기

먼저 Scala 3의 given/using 조합에 익숙하지 않다면, 다음 글을 먼저 읽어보길 권한다:

- Scala 3: Given and Using Clauses
- Scala 3: Givens vs Implicits Quickly Explained

Scala 3에서 given/using 조합이 도입된 이유는, 오용하기 쉬운 `implicit` 키워드의 권한을 줄이기 위해서다. implicit이 담당하던 주요 역할은 다음과 같다:

- 암시적 인자(implicit argument) — 이제 given/using으로 해결
- 타입 클래스(type class) — 역시 given/using으로 해결
- 확장 메서드(extension method) — Scala에서 자체 언어 수준 구문이 생김
- 암시적 변환(implicit conversion) — 이제 명시적으로 강제해야 함

## 함께 쓰기

Scala 3의 given 인스턴스와 using 절은 기존의 implicit val/object + implicit 인자 조합을 대체하기 위해 설계됐지만, `implicit` 키워드가 Scala 3에서 아예 사라진 건 아니다. 점진적으로 지원이 중단(deprecated)되고, 최종적으로는 언어에서 제거될 예정이다.

그런데 여기서 혼란이 생긴다. implicit을 계속 써야 하는 건가? 기존 코드베이스와는 어떻게 작업해야 하나?

짧은 답변:

- implicit을 더 이상 쓰지 말 것. 암시적 값/객체와 암시적 인자에는 새로운 `given`/`using` 조합을 사용한다.
- given을 쓰면 된다.

그렇다면 implicit이 잔뜩 있는 기존 코드베이스에서 새로운 given 기반 메커니즘을 어떻게 쓸 수 있을까?

`given` 메커니즘은 메서드에 필요한 인스턴스를 찾아 삽입하는 목적에서 `implicit` 메커니즘과 동일하게 동작한다. 구체적으로, `using` 절을 지정하면 컴파일러는 다음 위치에서 순서대로 `given` 인스턴스를 탐색한다:

- 메서드가 정의된 로컬 스코프
- 명시적으로 import된 모든 클래스, 객체, 패키지의 스코프
- 호출 대상 클래스의 companion 객체 스코프
- 제네릭 메서드 호출 시 관련된 모든 타입의 companion 객체 스코프

이 메커니즘은 고급 Scala 과정에서 더 깊이 다루지만, 이 글에서는 이 정도면 충분하다.

예를 들어, `given` 인스턴스를 필요로 하는 간단한 무인자 메서드를 생각해보자:

```scala
def aMethodWithGivenArg[T](using instance: T) = instance
```

이건 사실상 Scala 3의 내장 `summon[T]` 메서드 정의와 같다. `aMethodWithGivenArg[Int]`를 호출하면 컴파일러는 `Int` 타입의 `given` 값을 다음 위치에서 찾는다:

- 메서드가 정의된 스코프
- 모든 import의 스코프
- 메서드가 클래스 안에 정의됐다면, 그 클래스의 companion 객체 스코프
- 호출에 관여하는 모든 타입의 companion 스코프 — 이 경우 `Int` 객체

따라서 다음과 같이 정의하면:

```scala
given meaningOfLife: Int = 42
```

이렇게 호출할 수 있다:

```scala
val theAnswer = aMethodWithGivenArg[Int] // 42
```

완전히 동일한 메커니즘이 implicit 인자를 받는 메서드에서도 작동한다:

```scala
def aMethodWithImplicitArg[T](implicit instance: T) = instance
```

이건 Scala 2의 내장 `implicitly[T]` 메서드 정의와 정확히 같다(implicit이 아직 남아 있는 동안 Scala 3에서도 사용 가능하다). `aMethodWithImplicitArg[Int]`를 호출하면 컴파일러는 `implicit Int`에 대해 정확히 같은 탐색을 수행한다:

- 메서드가 속한 클래스/객체의 스코프
- import 스코프
- companion 스코프
- 관련된 모든 타입의 companion 스코프 — 이 경우 `Int` 객체

보다시피, 메커니즘은 동일하다. implicit을 정의하면:

```scala
implicit meaningOfLife: Int = 43
```

다음과 같이 메서드를 호출할 수 있다:

```scala
val theAnswer = aMethodWithImplicitArg[Int] // 43
```

Scala 2에서 Scala 3으로의 원활한 전환을 위해, given/implicit 인스턴스를 찾는 메커니즘은 동일하게 유지된다. 약간 다른 문법 — `implicit` 대신 `given`/`using` — 만 사용하면서 동일한 멘탈 모델을 유지할 수 있다.

이제 `implicit`으로 작성된 코드와 연동하려면, 새 메서드를 `given`/`using`으로 정의하기만 하면 된다. 기존 implicit 값들은 잘 동작한다:

```scala
// 새 메서드
def aMethodWithGivenArg[T](using instance: T) = instance

// 기존 implicit
implicit val theOnlyInt: Int = 42

val theInt: Int = aMethodWithGivenArg[Int] // 42
```

반대 방향으로도 동작한다. implicit을 사용하는 기존 메서드에 `given` 값을 정의해도 잘 작동한다:

```scala
// 기존 메서드
def aMethodWithImplicitArg[T](implicit instance: T) = instance

// 새 given
given meaningOfLife: Int = 42

val theAnswer = aMethodWithImplicitArg[Int] // 42
```

동시에, 같은 스코프에 `implicit val`과 `given`이 둘 다 존재하면 컴파일러가 모호성(ambiguity) 오류를 발생시킨다:

```scala
// 기존 메서드
def aMethodWithImplicitArg[T](implicit instance: T) = instance

// 혼란 발생
given meaningOfLife: Int = 42
implicit val theOnlyInt: Int = 42
```

이 오류는 메서드가 `implicit` 인자를 받든 `using` 절을 쓰든 관계없이 발생한다.

## 결론

이 글에서 살펴본 것처럼, 새로운 `given`/`using` 메커니즘은 기존의 `implicit` val/object + `implicit` 인자와 동일하게 동작하며, 양쪽 간에 아무 문제 없이 상호 운용할 수 있다. 이 호환성은 Scala 3으로의 전환을 수월하게 하기 위해 설계됐다.

앞으로는 새로운 `given`/`using` 구조를 사용해야 한다.
