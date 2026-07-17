> 원본: https://rockthejvm.com/articles/scala-3-type-lambdas

# Scala 3의 Type Lambda

이 글은 난이도가 좀 있다. 고차원적으로 사고할 수 있는 경험 많은 Scala 개발자를 대상으로 한다. [이전 글](https://rockthejvm.com/scala-types-kinds)을 먼저 읽고 오면 이해에 도움이 된다. Type lambda는 Scala 3에서 표현 자체는 간단하지만, 그 의미는 깊다.

이 기능은(수십 가지 다른 변경 사항과 함께) [Scala 3 New Features](https://rockthejvm.com/p/scala-3-new-features) 과정에서 자세히 다룬다.

## 1. 배경

[이전 글](https://blog.rockthejvm.com/scala-types-kinds)에서 Scala의 타입을 *kind*로 분류하는 방법을 살펴봤다. Scala 3에서도 이 부분은 달라지지 않는다. 다만 새로운 개념과 이를 표현하는 구문 구조를 도입했는데, 처음 보면 어렵게 느껴질 수 있다.

간략히 복습하면:

- Scala 타입은 *kind*에 속한다. Kind는 *타입의 타입*이라고 생각하면 된다.
- `Int`, `String` 같은 일반 타입이나 제네릭이 아닌 클래스는 *값 수준(value-level)* kind에 속한다. 값에 직접 붙일 수 있는 타입이다.
- `List` 같은 제네릭 타입은 level-1 kind에 속한다. 일반(level-0) 타입을 타입 인자로 받는다.
- Scala에서는 고차 카인드 타입(higher-kinded type)도 표현할 수 있다. 제네릭 타입의 타입 인자가 또 제네릭인 경우로, level-2 kind라 부른다.
- 제네릭 타입은 단독으로 값에 붙일 수 없고, 적절한 타입 인자(하위 kind의 타입)가 있어야 한다. 그래서 타입 생성자(type constructor)라고 부른다.

## 2. 타입은 함수처럼 생겼다

앞서 말했듯이, 제네릭 타입은 적절한 타입 인자가 주어져야 값에 붙일 수 있다. `List` 타입을 값에 직접 쓸 수는 없고, `List[Int]`(또는 다른 구체 타입)처럼 써야 한다.

따라서 `List`(제네릭 타입 자체)를 하나의 함수처럼 볼 수 있다. level-0 타입을 받아서 level-0 타입을 돌려주는 함수다. 이 "함수" -- level-0 타입에서 level-0 타입으로의 변환 -- 가 바로 `List`가 속한 *kind*를 나타낸다. Scala 2에서는 이런 타입을 표현하는 방법이 끔찍했다(`{ type T[A] = List[A] })#T`, 보기만 해도 싫다). Scala 3에서는 함수와 훨씬 비슷한 형태로 쓸 수 있다:

```scala
[X] =>> List[X]
```

이 구조는 "타입 인자 `X`를 받아서 `List[X]` 타입을 만들어내는 타입"이라고 읽으면 된다. `List` 타입(자체)이 하는 일과 정확히 같다: 타입 인자를 받아서 새로운 타입을 만든다.

복잡도를 높여가며 몇 가지 예를 더 보자:

- `[T] =>> Map[String, T]`는 타입 인자 `T` 하나를 받아서 키가 `String`이고 값이 `T`인 `Map` 타입을 돌려주는 타입이다.
- `[T, E] =>> Either[Option[T], E]`는 타입 인자 두 개를 받아서 `Option[T]`와 `E`로 구성된 구체적인 `Either` 타입을 돌려주는 타입이다.
- `[F[_]] =>> F[Int]`는 그 자체가 제네릭인 타입 인자(`List` 같은)를 받아서, 그 타입에 `Int`를 적용한 결과를 돌려주는 타입이다(타입이 너무 많이 나오는 건 알고 있다).

## 3. Type Lambda가 필요한 이유

Type lambda는 고차 카인드 타입을 다루기 시작하면 중요해진다. 가장 유명한 고차 카인드 타입 클래스 중 하나인 Monad를 보자. 가장 단순한 형태는 이렇다:

```scala
trait Monad[M[_]] {
  def pure[A](a: A): M[A]
  def flatMap[A, B](m: M[A])(f: A => M[B]): M[B]
}
```

`Either`가 모나드 구조라는 것도 알고 있을 것이다(이건 다른 글에서 다룰 수도 있겠다). 그래서 `Either`에 대한 `Monad`를 작성할 수 있다. 하지만 `Either`는 타입 인자를 두 개 받는 반면, `Monad`는 타입 인자가 하나만 필요하다. 어떻게 작성할까? 다음과 같이 쓸 수 있으면 좋겠다:

```scala
class EitherMonad[T] extends Monad[Either[T, ?]] {
  // ... 구현
}
```

이렇게 하면 `EitherMonad`가 `Either[Exception, Int]`와 `Either[String, Int]` 모두에서 동작할 수 있다(여기서 `Int`가 원하는 타입이다). 에러 타입 `E`가 주어졌을 때, 구체적인 `E`가 무엇이든 `EitherMonad`가 `Either[E, Int]`에 대해 동작하기를 원한다.

안타깝게도, 위 구조는 유효한 Scala 코드가 아니다.

정답은 다음과 같이 작성하는 것이다:

```scala
class EitherMonad[T] extends Monad[[E] =>> Either[T, E]] {
  // ... 구현
}
```

인자가 두 개인 함수의 부분 적용(partial application)을 다른 함수에 전달하는 것과 비슷하다.

정말 추상적이고 머리에 잘 안 들어온다면, 충분히 그럴 수 있다.

Scala 3 이전에는 Cats 같은 라이브러리가 컴파일러 플러그인([kind-projector](https://github.com/typelevel/kind-projector))에 의존해서 위의 `?` 구조와 비슷한 것을 구현했다. Scala 3에서는 이것이 언어 차원에서 공식적으로 허용된다.

## 4. 결론

간단한 구문 구조 하나로 Scala 3는 API 설계자들이 오랫동안 고심해온(그리고 온갖 우회 방법을 동원해야 했던) 문제를 해결했다. 바로 일부 타입 인자를 "비워둔 채로" 고차 카인드 타입을 정의하는 방법이다.

향후 글에서 type lambda의 고급 활용법과 주의점을 더 살펴볼 예정이다.
