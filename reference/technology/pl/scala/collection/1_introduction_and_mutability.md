# Scala 컬렉션 서론과 가변·불변 컬렉션

## 서론 (Introduction)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/introduction.html>

컬렉션 프레임워크(collections framework)는 Scala 2.13 표준 라이브러리의 심장이며, Scala 3.x에서도 그대로 사용됩니다. 이 프레임워크는 컬렉션 타입들을 위한 공통적이고, 일관되며, 모든 것을 아우르는 틀을 제공합니다. 이 프레임워크 덕분에 메모리 안의 데이터를 높은 수준에서 다룰 수 있으며, 프로그램의 기본 구성 단위가 개별 원소가 아니라 컬렉션 전체가 됩니다.

이런 프로그래밍 스타일에는 어느 정도 학습이 필요합니다. 다행히 Scala 컬렉션이 가진 여러 좋은 성질이 적응을 도와줍니다. Scala 컬렉션은 사용하기 쉽고, 간결하고, 안전하고, 빠르고, 범용적입니다.

**사용하기 쉽습니다(easy to use):** 20~50개 정도의 작은 메서드 어휘만 익히면 대부분의 컬렉션 문제를 두어 번의 연산으로 해결할 수 있습니다. 복잡한 반복 구조나 재귀(recursion)를 붙들고 씨름할 필요가 없습니다. 영속 컬렉션(persistent collection)과 부수 효과 없는(side-effect-free) 연산 덕분에, 새 데이터로 기존 컬렉션을 실수로 훼손할 걱정을 하지 않아도 됩니다. 이터레이터(iterator)와 컬렉션 갱신 사이의 간섭도 사라집니다.

> 💡 **왜 필요한가** — Java에서 컬렉션을 순회하는 도중에 그 컬렉션을 수정하면 `ConcurrentModificationException`이 발생하는 것을 겪어 보셨을 것입니다. "이터레이터와 컬렉션 갱신 사이의 간섭이 사라진다"는 말이 바로 이 문제를 가리킵니다. 영속 컬렉션은 수정 시 기존 컬렉션을 그대로 두고 새 컬렉션을 만들어 돌려주므로, 이런 종류의 버그가 원천적으로 생기지 않습니다.

**간결합니다(concise):** 예전에는 한 개 이상의 반복문이 필요했던 일을 단어 하나로 해낼 수 있습니다. 함수형 연산을 가벼운 문법으로 표현하고 여러 연산을 힘들이지 않고 조합할 수 있어서, 그 결과는 마치 나만의 대수(algebra)를 다루는 느낌을 줍니다.

**안전합니다(safe):** 이 장점은 직접 겪어 봐야 실감이 납니다. Scala 컬렉션의 정적 타입(statically typed) 특성과 함수형 특성 덕분에, 여러분이 저지를 수 있는 오류의 압도적 다수가 컴파일 시점에 잡힙니다. 그 이유는 다음과 같습니다. (1) 컬렉션 연산 자체가 매우 많이 사용되므로 충분히 검증되어 있습니다. (2) 컬렉션 연산을 사용할 때 입력과 출력이 함수의 매개변수와 결과로 명시적으로 드러납니다. (3) 이렇게 명시된 입력과 출력은 정적 타입 검사의 대상이 됩니다. 결론적으로, 잘못된 사용의 대다수는 타입 오류로 나타납니다. 수백 줄짜리 프로그램이 첫 실행에 바로 동작하는 일도 결코 드물지 않습니다.

**빠릅니다(fast):** 컬렉션 연산은 라이브러리 안에서 조율되고 최적화되어 있습니다. 그 결과 컬렉션을 사용하는 것은 대체로 꽤 효율적입니다. 자료 구조와 연산을 손수 세심하게 튜닝하면 조금 더 나은 성능을 낼 수도 있겠지만, 중간에 최적이 아닌 구현 결정을 내려서 오히려 훨씬 나쁜 성능을 얻게 될 수도 있습니다.

**병렬 처리가 가능합니다(parallel):** [`scala-parallel-collections` 모듈](https://index.scala-lang.org/scala/scala-parallel-collections/scala-parallel-collections)은 여러 코어에 걸쳐 컬렉션 연산을 병렬로 실행하는 기능을 제공합니다. 병렬 컬렉션은 일반적으로 순차 컬렉션과 동일한 연산을 지원합니다. 순차 컬렉션에 `par` 메서드를 호출하기만 하면 병렬 컬렉션으로 바꿀 수 있습니다.

> ⚠️ **짚고 넘어가기** — Scala 2.12까지는 병렬 컬렉션이 표준 라이브러리에 포함되어 있었지만, 2.13부터는 별도의 `scala-parallel-collections` 모듈로 분리되었습니다. 따라서 `par`를 사용하려면 이 모듈을 의존성으로 추가해야 합니다. 원문이 "모듈이 제공한다"고 쓴 것은 이런 배경 때문입니다.

**범용적입니다(universal):** 컬렉션은 의미가 통하는 모든 타입에 대해 동일한 연산을 제공합니다. 그래서 상당히 작은 연산 어휘만으로도 많은 일을 해낼 수 있습니다. 예를 들어 문자열(string)은 개념적으로 문자들의 시퀀스(sequence)입니다. 그에 따라 Scala 컬렉션에서는 문자열이 모든 시퀀스 연산을 지원합니다. 배열(array)도 마찬가지입니다.

**예제:** 다음은 Scala 컬렉션의 여러 장점을 한 줄로 보여 주는 코드입니다.

```scala
val (minors, adults) = people partition (_.age < 18)
```

이 연산이 무엇을 하는지는 곧바로 알 수 있습니다. `people` 컬렉션을 나이에 따라 `minors`(미성년자)와 `adults`(성인)로 나눕니다. `partition` 메서드는 최상위 컬렉션 타입인 `IterableOps`에 정의되어 있으므로, 이 코드는 배열을 포함한 어떤 종류의 컬렉션에서도 동작합니다. 결과로 나오는 `minors`와 `adults` 컬렉션은 `people` 컬렉션과 같은 타입이 됩니다.

> 📘 **처음 배우는 분께** — 위 코드에서 `people partition (...)`은 `people.partition(...)`과 같은 뜻입니다. Scala에서는 인자가 하나인 메서드를 점(`.`)과 괄호 없이 중위 표기법(infix notation)으로 쓸 수 있습니다. `_.age < 18`은 "각 원소의 `age`가 18보다 작은가"를 검사하는 익명 함수(anonymous function)의 축약 표기이고, `val (minors, adults) = ...`는 `partition`이 돌려주는 튜플(tuple)의 두 값을 한 번에 두 변수로 분해해서 받는 문법입니다.

이 코드는 전통적인 컬렉션 처리에 필요했던 한 개에서 세 개의 반복문(배열이라면 중간 결과를 다른 곳에 버퍼링해야 하므로 세 개)보다 훨씬 간결합니다. 기본적인 컬렉션 어휘를 익히고 나면, 이런 코드를 작성하는 것이 명시적인 반복문을 작성하는 것보다 훨씬 쉽고 안전하다는 사실도 알게 될 것입니다.

더 나아가 `partition` 연산은 꽤 빠르며, 여러 코어 위의 병렬 컬렉션에서는 더욱 빨라질 수 있습니다.

이 문서는 사용자 관점에서 Scala 컬렉션 클래스들의 API를 깊이 있게 논의합니다. 모든 핵심 클래스와 그 클래스들이 정의하는 메서드들을 차례로 둘러보게 될 것입니다.

---

## 가변 컬렉션과 불변 컬렉션 (Mutable and Immutable Collections)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/overview.html>

---

Scala 컬렉션은 가변(mutable) 컬렉션과 불변(immutable) 컬렉션을 체계적으로 구분합니다. **가변** 컬렉션은 제자리에서(in place) 갱신하거나, 줄이거나, 늘릴 수 있습니다. 즉 부수 효과(side effect)로서 컬렉션의 요소를 변경하거나, 추가하거나, 제거할 수 있다는 뜻입니다. 이와 대조적으로 **불변** 컬렉션은 절대 변하지 않습니다. 불변 컬렉션에도 추가, 제거, 갱신을 흉내 내는 연산이 여전히 존재하지만, 이런 연산은 매번 새로운 컬렉션을 반환하고 기존 컬렉션은 변경하지 않은 채 그대로 둡니다.

> 📘 **처음 배우는 분께 — "불변인데 어떻게 추가하나요?"**
> 불변 컬렉션에 요소를 "추가"한다는 말은 기존 컬렉션을 고치는 게 아니라,
> **기존 요소 + 새 요소를 담은 새 컬렉션을 하나 더 만들어 돌려준다**는 뜻입니다.
> 예를 들어 `Set(1, 2) + 3`을 실행하면 `Set(1, 2)`는 그대로 남아 있고,
> `Set(1, 2, 3)`이라는 새 집합이 반환됩니다. "복사본을 새로 만든다"고 이해하면 됩니다.
> (실제로는 내부 구조를 최대한 공유해서 통째로 복사하는 것보다 훨씬 효율적으로 동작합니다.)

모든 컬렉션 클래스는 `scala.collection` 패키지 또는 그 하위 패키지인 `mutable`과 `immutable`에 들어 있습니다. 클라이언트 코드에서 필요로 하는 대부분의 컬렉션 클래스는 세 가지 변형(variant)으로 존재하며, 각각 `scala.collection`, `scala.collection.immutable`, `scala.collection.mutable` 패키지에 위치합니다. 각 변형은 가변성(mutability)에 관해 서로 다른 특성을 가집니다.

`scala.collection.immutable` 패키지의 컬렉션은 **누구에게나** 불변임이 보장됩니다. 이런 컬렉션은 생성된 이후에 절대 변하지 않습니다. 따라서 같은 컬렉션 값에 서로 다른 시점에 반복해서 접근하더라도 항상 같은 요소를 가진 컬렉션을 얻는다는 사실에 의지할 수 있습니다.

`scala.collection.mutable` 패키지의 컬렉션은 컬렉션을 제자리에서 변경하는 연산을 일부 제공한다고 알려져 있습니다. 따라서 가변 컬렉션을 다룰 때는 어떤 코드가 어떤 컬렉션을 언제 변경하는지 이해하고 있어야 합니다.

`scala.collection` 패키지의 컬렉션은 가변일 수도 있고 불변일 수도 있습니다. 예를 들어 [collection.IndexedSeq[T]](https://www.scala-lang.org/api/2.13.18/scala/collection/IndexedSeq.html)는 [collection.immutable.IndexedSeq[T]](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/IndexedSeq.html)와 [collection.mutable.IndexedSeq[T]](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/IndexedSeq.html) 둘 모두의 상위 클래스(superclass)입니다. 일반적으로 `scala.collection` 패키지의 루트(root) 컬렉션은 컬렉션 전체에 영향을 주는 변환(transformation) 연산을 지원하고, `scala.collection.immutable` 패키지의 불변 컬렉션은 보통 개별 값을 추가하거나 제거하는 연산을 더하며, `scala.collection.mutable` 패키지의 가변 컬렉션은 보통 루트 인터페이스에 부수 효과를 동반하는 수정 연산을 추가합니다.

> 💡 **왜 필요한가 — 굳이 패키지를 세 개로 나눈 이유**
> "가변/불변 두 개면 충분하지 않나?"라는 의문이 들 수 있습니다. 루트 패키지(`scala.collection`)가
> 따로 있는 이유는 **"가변이든 불변이든 상관없이 받겠다"는 함수를 쓸 수 있게 하기 위해서**입니다.
> 예를 들어 매개변수 타입을 `collection.Seq[Int]`로 선언하면, 호출하는 쪽이 가변 시퀀스를 주든
> 불변 시퀀스를 주든 모두 받을 수 있습니다. 반대로 `collection.immutable.Seq[Int]`로 선언하면
> "아무도 못 바꾸는 시퀀스만 받겠다"는 더 강한 계약을 표현할 수 있습니다.
> 즉 세 패키지는 API 작성자가 요구 조건의 강도를 선택할 수 있게 해 주는 장치입니다.

루트 컬렉션과 불변 컬렉션의 또 다른 차이는, 불변 컬렉션의 클라이언트는 **아무도** 그 컬렉션을 변경할 수 없다는 보장을 받는 반면, 루트 컬렉션의 클라이언트는 자기 자신이 컬렉션을 변경하지 않겠다고 약속할 뿐이라는 점입니다. 이런 컬렉션의 정적 타입(static type)이 컬렉션을 수정하는 연산을 전혀 제공하지 않더라도, 런타임 타입(run-time type)은 여전히 다른 클라이언트가 변경할 수 있는 가변 컬렉션일 가능성이 있습니다.

> ⚠️ **짚고 넘어가기 — `collection.Seq`는 "읽기 전용"이지 "불변"이 아닙니다**
> 위 문단이 이 페이지에서 가장 오해하기 쉬운 부분입니다. 루트 타입(예: `collection.Seq`)으로
> 참조하면 **나에게는** 수정 메서드가 보이지 않지만, 그 실체가 `mutable.ArrayBuffer`라면
> 원본을 쥐고 있는 다른 코드가 언제든 내용을 바꿀 수 있습니다. 즉 루트 타입은
> "읽기 전용 창구(read-only view)"일 뿐, 값 자체가 얼어 있다는 뜻이 아닙니다.
> "시간이 지나도 절대 안 변한다"는 보장이 필요하면 반드시 `collection.immutable` 쪽 타입을 쓰세요.

기본적으로 Scala는 항상 불변 컬렉션을 선택합니다. 예를 들어 아무 접두어 없이, 그리고 어딘가에서 `Set`을 임포트(import)하지 않은 채 그냥 `Set`이라고 쓰면 불변 집합(set)을 얻고, `Iterable`이라고 쓰면 불변 이터러블(iterable) 컬렉션을 얻습니다. 이것들이 `scala` 패키지에서 기본으로 임포트되는 바인딩(binding)이기 때문입니다. 가변 기본 버전을 쓰려면 `collection.mutable.Set`이나 `collection.mutable.Iterable`처럼 명시적으로 써야 합니다.

가변 버전과 불변 버전의 컬렉션을 모두 사용하고 싶을 때 유용한 관례는 `collection.mutable` 패키지만 임포트하는 것입니다.

```scala
import scala.collection.mutable
```

이렇게 하면 접두어 없이 쓴 `Set`은 여전히 불변 컬렉션을 가리키고, `mutable.Set`은 가변 대응물(counterpart)을 가리키게 됩니다.

> 💡 **왜 필요한가 — `mutable.` 접두어가 곧 경고 표시**
> `import scala.collection.mutable.Set`처럼 클래스를 직접 임포트하면 코드 곳곳의 `Set`이
> 가변인지 불변인지 임포트 문을 다시 확인해야만 알 수 있습니다. 반면 패키지만 임포트하는
> 관례를 따르면 코드에서 `mutable.Set`이라는 표기 자체가 "여기서 부수 효과가 일어날 수 있다"는
> 시각적 경고 역할을 합니다. 코드 리뷰 시 가변 상태가 쓰이는 지점을 한눈에 찾을 수 있습니다.

컬렉션 계층 구조의 마지막 패키지는 `scala.collection.generic`입니다. 이 패키지에는 구체적인 컬렉션을 추상화하기 위한 구성 요소(building block)들이 들어 있습니다.

편의성과 하위 호환성(backwards compatibility)을 위해 몇몇 중요한 타입은 `scala` 패키지에 별칭(alias)이 있어서, 임포트 없이 단순한 이름만으로 사용할 수 있습니다. 한 가지 예가 `List` 타입인데, 다음과 같은 여러 방법으로 접근할 수 있습니다.

```scala
scala.collection.immutable.List   // 실제로 정의된 곳
scala.List                        // scala 패키지의 별칭을 통해
List                              // scala._ 는 항상
                                  // 자동으로 임포트되기 때문
```

별칭이 있는 다른 타입으로는 [Iterable](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterable.html), [Seq](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Seq.html), [IndexedSeq](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/IndexedSeq.html), [Iterator](https://www.scala-lang.org/api/2.13.18/scala/collection/Iterator.html), [LazyList](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/LazyList.html), [Vector](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Vector.html), [StringBuilder](https://www.scala-lang.org/api/2.13.18/scala/collection/mutable/StringBuilder.html), [Range](https://www.scala-lang.org/api/2.13.18/scala/collection/immutable/Range.html)가 있습니다.

다음 그림은 `scala.collection` 패키지의 모든 컬렉션을 보여 줍니다. 이것들은 모두 높은 수준의 추상 클래스(abstract class) 또는 트레이트(trait)이며, 일반적으로 가변 구현과 불변 구현을 모두 가집니다.

[![일반 컬렉션 계층 구조](https://docs.scala-lang.org/resources/images/tour/collections-diagram-213.svg)](https://docs.scala-lang.org/resources/images/tour/collections-diagram-213.svg)

다음 그림은 `scala.collection.immutable` 패키지의 모든 컬렉션을 보여 줍니다.

[![불변 컬렉션 계층 구조](https://docs.scala-lang.org/resources/images/tour/collections-immutable-diagram-213.svg)](https://docs.scala-lang.org/resources/images/tour/collections-immutable-diagram-213.svg)

그리고 다음 그림은 `scala.collection.mutable` 패키지의 모든 컬렉션을 보여 줍니다.

[![가변 컬렉션 계층 구조](https://docs.scala-lang.org/resources/images/tour/collections-mutable-diagram-213.svg)](https://docs.scala-lang.org/resources/images/tour/collections-mutable-diagram-213.svg)

범례(legend):

[![그래프 범례](https://docs.scala-lang.org/resources/images/tour/collections-legend-diagram.svg)](https://docs.scala-lang.org/resources/images/tour/collections-legend-diagram.svg)

### 컬렉션 API 개요 (An Overview of the Collections API)

가장 중요한 컬렉션 클래스들은 위 그림에 나와 있습니다. 이 클래스들 사이에는 상당히 많은 공통점이 있습니다. 예를 들어 모든 종류의 컬렉션은 컬렉션 클래스 이름 뒤에 요소들을 나열하는, 동일하고 일관된 문법으로 생성할 수 있습니다.

```scala
Iterable("x", "y", "z")
Map("x" -> 24, "y" -> 25, "z" -> 26)
Set(Color.red, Color.green, Color.blue)
SortedSet("hello", "world")
Buffer(x, y, z)
IndexedSeq(1.0, 2.0)
LinearSeq(a, b, c)
```

같은 원칙이 특정 컬렉션 구현체에도 그대로 적용됩니다. 예를 들면 다음과 같습니다.

```scala
List(1, 2, 3)
HashMap("x" -> 24, "y" -> 25, "z" -> 26)
```

이 모든 컬렉션은 `toString`으로 출력할 때도 위에 쓰인 것과 같은 형태로 표시됩니다.

모든 컬렉션은 `Iterable`이 제공하는 API를 지원하되, 의미가 있는 곳에서는 타입을 특수화(specialize)합니다. 예를 들어 `Iterable` 클래스의 `map` 메서드는 결과로 또 다른 `Iterable`을 반환합니다. 그러나 이 결과 타입은 하위 클래스에서 재정의(override)됩니다. 예컨대 `List`에서 `map`을 호출하면 다시 `List`가 나오고, `Set`에서 호출하면 다시 `Set`이 나오는 식입니다.

```
scala> List(1, 2, 3) map (_ + 1)
res0: List[Int] = List(2, 3, 4)
scala> Set(1, 2, 3) map (_ * 2)
res0: Set[Int] = Set(2, 4, 6)
```

컬렉션 라이브러리 전반에 걸쳐 구현되어 있는 이 동작을 **균일 반환 타입 원칙**(uniform return type principle)이라고 부릅니다.

> 📘 **처음 배우는 분께 — 균일 반환 타입 원칙이 주는 편안함**
> 이 원칙을 한 문장으로 줄이면 "**넣은 컬렉션 종류 그대로 돌려받는다**"입니다.
> `List`를 변환하면 `List`가, `Set`을 변환하면 `Set`이 나오므로, 연산을 거칠 때마다
> 결과가 무슨 타입인지 고민하거나 다시 변환할 필요가 없습니다. 덕분에
> `list.map(...).filter(...).take(...)` 같은 메서드 체이닝을 해도 처음부터 끝까지
> 같은 컬렉션 타입이 유지됩니다.

컬렉션 계층 구조에 속한 대부분의 클래스는 루트, 가변, 불변이라는 세 가지 변형으로 존재합니다. 유일한 예외는 `Buffer` 트레이트로, 가변 컬렉션으로만 존재합니다.

이어지는 문서에서 이 클래스들을 하나씩 살펴보겠습니다.
