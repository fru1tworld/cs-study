> 원본: https://rockthejvm.com/articles/scala-3-givens-vs-implicits

# Scala 3: Given vs Implicit 빠르게 비교하기

## 이 글의 대상 독자

이 글은 implicit에 어느 정도 익숙한 Scala 프로그래머를 대상으로 한다.

## 들어가며

처음부터 Scala 3로 시작했다면 given/using의 핵심 개념만 알면 충분하다. Scala 3에서 implicit은 단계적으로 폐지되므로 이 글을 굳이 읽을 필요가 없다. 말장난을 좀 섞자면, 그냥 `given`을 `using`하면 된다.

Scala 2에서 넘어왔고, implicit에 익숙하며, given/using 조합으로 전환해야 한다면 계속 읽어보자.

## 배경: Implicit의 전면 개편

Implicit은 Scala에서 가장 강력한 기능 중 하나다. 전통적인 객체지향 타입 계층 바깥에 있는 코드 추상화를 가능하게 해 준다. Implicit은 수많은 실전 시나리오에서 그 유용성이 검증되었다. 몇 가지만 나열하면 다음과 같다.

- Implicit은 Scala에서 타입 클래스를 만드는 핵심 도구다. Cats 같은 라이브러리는 implicit 없이는 존재할 수 없다.
- `20.seconds` 같은 표현처럼 기존 타입의 기능을 확장하는 것도 implicit으로 가능하다.
- Implicit을 사용하면 새로운 타입을 자동으로 생성하고, 컴파일 시점에 타입 간 관계를 강제할 수 있다. 심지어 implicit으로 타입 수준 연산까지 할 수 있다.

Scala 2에서는 이 모든 기능이 하나의 `implicit` 키워드 아래, implicit `val`, implicit `def`, implicit class를 통해 제공된다. 그러나 이런 통합 방식에는 단점이 있으며, implicit은 여러 비판을 받아왔다. 가장 중요한 것들을 꼽으면:

1. implicit을 사용하는 코드가 동작할 때, 어떤 implicit이 그것을 가능하게 했는지 정확히 찾아내기가 매우 어렵다. 코드베이스가 커질수록 기하급수적으로 어려워진다.
2. implicit이 필요한 코드를 작성할 때, 컴파일되게 하려면 _올바른_ implicit을 import해야 한다. 자동 import는 사실상 불가능하다(IDE가 마음을 읽을 수는 없으므로). 결국 a) 어떤 import가 필요한지 마법처럼 알거나, b) 좌절하거나 둘 중 하나다.
3. implicit 인자가 없는 implicit `def`는 타입 변환을 수행할 수 있다. 대부분의 경우 이런 변환은 위험하고 추적하기도 어렵다. 게다가 implicit 기능 중 가장 쉽게 쓸 수 있어서, 이중으로 위험하다.
4. Implicit은 배우기 정말 어려워서 많은 입문자를 Scala에서 멀어지게 한다.
5. 기타 불편한 점들:
   - 실제로 참조하지 않을 implicit에도 이름을 붙여야 하는 것
   - 메서드에 implicit 파라미터가 있을 때 생기는 구문 혼동
   - 구조와 의도의 불일치: 예를 들어 `implicit def`는 "메서드"의 의미로 사용되는 경우가 없다

## 암시적 변환 (Implicit Conversion)

암시적 변환은 이제 명시적으로 선언해야 한다. 이로써 큰 부담이 해소된다.

Scala 3 이전에는 확장 메서드와 타입 클래스 패턴에 암시적 변환이 필수였다. Scala 3에서는 확장 메서드 개념이 독립적으로 존재하므로, 예전에 implicit이 필요했던 패턴 상당수를 변환 없이 구현할 수 있다. 따라서 변환이 필요한 경우가 크게 줄어들 것이다. 동시에, 변환 자체가 위험하고, 기존에는 몰래 적용되었기 때문에 이중으로 위험했다. Scala 3에서는 변환을 특정 방식으로 선언해야 한다.

Scala 3 이전에는 암시적 변환을 그 위력(과 위험)에 비해 너무 쉽게 작성할 수 있었다. 다음과 같은 클래스가 있다고 하자.

```scala
case class Person(name: String) {
  def greet: String = s"Hey, I'm $name. Scala rocks!"
}
```

한 줄짜리 암시적 변환을 이렇게 쓸 수 있었다.

```scala
implicit def stringToPerson(string: String): Person = Person(string)
```

그러면 이렇게 호출할 수 있었다.

```scala
"Alice".greet
```

Scala 3에서는 자기가 무엇을 하는지 확실히 알도록 여러 단계를 거쳐야 한다. 암시적 변환은 `Conversion[A, B]`의 `given` 인스턴스로 선언한다. Person 클래스 예제는 다음과 같다.

```scala
given stringToPerson: Conversion[String, Person] with {
  def apply(s: String): Person = Person(s)
}
```

하지만 이것만으로는 암시적 마법에 의존할 수 없다. `implicitConversions` 패키지도 명시적으로 import해야 한다. 즉, 다음과 같이 작성해야 한다.

```scala
import scala.language.implicitConversions

// 코드 어딘가에서
"Alice".greet
```

이렇게 하면 암시적 변환을 쓰려는 의지가 확실해야 한다. 확장 메서드에 더 이상 변환이 필요하지 않다는 점까지 합치면, 암시적 변환의 사용은 크게 줄어들 것이다.

## Scala 3 Given, Implicit 그리고 이름 붙이기

첫째, Scala 2에서는 실제로 참조하지 않을 implicit에도 이름을 붙여야 했다. given에서는 더 이상 그럴 필요가 없다. 이름 없이 given 인스턴스를 선언할 수 있다.

```scala
given Ordering[String] {
    // 구현
}
```

마찬가지로, `using` 절에서도 주입될 값에 이름을 붙이지 않아도 된다.

```scala
def sortThings[T](things: List[T])(using Ordering[T]) = ...
```

## Given, Implicit 그리고 구문 모호성

둘째, given은 `using` 절이 있는 메서드를 호출할 때 생기는 구문 모호성을 해결한다. 예를 들어보자. 다음과 같은 메서드가 있을 때:

```scala
def getMap(implicit size: Int): Map[String, Int] = ...
```

스코프에 implicit이 있더라도 `getMap("Alice")`라고 쓸 수 없다. 인자가 컴파일러가 삽입했을 implicit 값을 덮어쓰기 때문에, 컴파일러에서 타입 에러가 발생한다.

Given은 이 문제를 해결한다. 다음과 같은 메서드가 있을 때:

```scala
def getMap(using size: Int): Map[String, Int] = ...
```

`size`에 원하는 값을 명시적으로 전달하려면, 전달한다는 것도 명시해야 한다.

```scala
getMap(using 42)("Alice")
```

이렇게 하면 의도가 매우 명확하다. 스코프에 `given` Int가 있다면, 그냥 `getMap("Alice")`라고 호출하면 된다. given 값이 이미 `size`에 주입되었기 때문이다.

## Scala 3 Given이 추적 문제를 해결하는 방식

Scala 2에서 implicit은 추적하기 극히 어렵기로 악명 높다. 대규모 코드에서 implicit 인자를 받는 메서드를 사용하고, 암시적 변환을 쓰고, 해당 타입에 속하지 않는 메서드(확장 메서드)를 호출하면서도, _그것들이 어디서 오는지 전혀 모를 수 있다_.

Given은 여러 방식으로 이 문제를 해결하려 한다.

첫째, given 인스턴스는 명시적으로 import해야 하므로, import된 것 중 어떤 것이 실제로 given 인스턴스인지 더 잘 추적할 수 있다.

둘째, given은 `using` 절을 통한 인자 자동 주입에만 사용된다. 따라서 _명시적으로 전달하지 않는 메서드 인자_를 찾을 때, import된 given 인스턴스만 확인하면 된다. 다른 암시적 마법(암시적 변환, 확장 메서드)은 각각 명확하게 정의된 메커니즘이 있어서, 비슷하게 추적할 수 있다.

## Scala 3 Given과 자동 Import

이 부분은 어렵다. given은 해당 타입의 `using` 절이 있는 곳에 자동으로 주입되므로, implicit과 비슷한 메커니즘이다. 다음과 같이 `using` 절이 있는 메서드를 호출할 때:

```scala
def sortTheList[T](list: List[T])(using comparator: Comparator[T]) = ...
```

IDE가 마음을 읽고 올바른 `given` 인스턴스를 자동으로 import해 줄 수는 없다. import는 여전히 명시적으로 해야 한다.

다만, 기존의 implicit 해석 메커니즘은 "no implicits found" 정도의 매우 일반적인 에러만 보여주었다. Scala 3 컴파일러는 훨씬 의미 있는 에러를 표시하도록 크게 개선되어, `given` 탐색이 실패하면 컴파일러가 어디서 막혔는지 알려준다. 개발자는 그 정보를 바탕으로 나머지 `given` 인스턴스를 스코프에 제공할 수 있다.

## Scala 3 Given, Implicit 그리고 의도

implicit def는 메서드처럼 사용하려고 만든 것이 아니다. 따라서 코드의 구조(메서드)와 의도(변환) 사이에 명백한 불일치가 있다. Scala 3의 새로운 맥락적 추상화(contextual abstraction)는 의도를 명확히 하여 이 문제를 해결한다.

- Given/using 절: "암시적" 인자 전달에 사용
- 암시적 변환: `Conversion` 인스턴스를 생성하여 수행
- 확장 메서드: 고유한 일급 구문 구조를 가짐

## 마무리

Implicit은 강력하고 위험하며, Scala를 독특하게 만드는 기능 중 하나다. Scala 3은 implicit 메커니즘을 넘어, 각 기능이 무엇을 달성하려는지 의도가 훨씬 명확한 방향으로 나아간다. given/using + 확장 메서드 + 명시적 암시적 변환이라는 새로운 체계는, 기존 방식에 익숙한 사람들의 저항을 받을 수 있다. 하지만 몇 년 후를 내다보면, 이 변화 덕분에 더 명확한 Scala 코드를 작성하게 되었다는 것에 감사하게 될 거라 낙관한다.
