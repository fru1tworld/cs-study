# 전략적 Scala 스타일: 최소 권한의 원칙

> 원문: [Strategic Scala Style: Principle of Least Power](https://www.lihaoyi.com/post/StrategicScalaStylePrincipleofLeastPower.html)
> 저자: Li Haoyi | 번역일: 2026-07-10

Scala 언어는 크고 복잡하며, 개발자가 같은 일을 다양한 방식으로 할 수 있는 온갖 도구를 제공한다. 모든 문제에 가능한 해법이 이렇게 널려 있다면, 개발자는 어떤 것을 골라야 할까? 이 글은 "전략적" 수준의 스타일 가이드라인을 제시하려는 블로그 시리즈의 첫 번째 글이다. "공백을 얼마나 둘 것인가"나 camelCase 대 PascalCase 같은 수준을 넘어, Scala 언어로 작업하는 개발자가 가능한 해법의 뷔페에서 무엇을 고를지 판단하는 데 도움을 주는 것이 목적이다.

## 전략적 Scala 스타일에 관하여

이 가이드라인은 내가 Scala로 오픈소스·클로즈드소스 프로젝트를 진행하며 쌓은 경험에 기반한다. 그럼에도 모두 일관된 근본 원칙에서 도출된 것이므로, "나는 이게 좋고 너는 저게 좋고" 식의 흔한 취향 논쟁을 넘어서는 근거를 제공할 수 있기를 바란다.

이 가이드라인은 독자가 Scala의 언어 기능 대부분과 그것으로 무엇을 할 수 있는지 이미 알고 있다고 가정하며, 해법을 고를 때 그 기능들 사이에서 어떻게 선택할지에만 집중한다. 순수하게 "바닐라 Scala"와 표준 라이브러리 기능/API만 다룬다. Akka나 Scalaz에 관한 내용은 여기에 없다.

"모나딕" 진영, "리액티브" 진영, "타입 레벨" 진영, "Scala.js" 진영(즉, 사실상 모든 사람)에서 온 이들은 분명 일부 가이드라인에 동의하지 않을 것이다. 그래도 문서 전체가 충분히 폭넓게 적용될 수 있어서, 반대 의견이

> "이 항목들의 순서를 바꿔야 한다고 생각한다"

라거나

> "여기에 끼워 넣을 만한 다른 기법이 있다"

정도가 되기를 바란다.

> "전부 틀렸고 이건 끔찍하다"

가 아니라 말이다. 어느 항목이든 동의할 수도, 동의하지 않을 수도 있다. 아래 댓글로 알려 달라!

## 빠른 참조: 최소 권한의 원칙

이 가이드라인 전체를 떠받치는 원칙은 다음과 같다.

- **철학: 최소 권한의 원칙**
  1. *복잡성은 적이다*
  2. *리팩터링을 두려워하지 마라*
  3. *과잉 설계하지 마라*

가이드라인 전체를 한눈에 보면 다음과 같다.

- **불변성과 가변성**
  1. *가능한 한 불변성을 사용하라*
  2. *실제로 변하는 것을 모델링하는 경우가 아니라면*
  3. *또는 성능을 위한 경우가 아니라면*
  4. *가변성을 쓰더라도 범위를 최대한 좁혀라*
  5. *이중 가변성은 쓰지 마라. 아마 필요 없을 것이다*
  6. *무엇을 하는지 확실히 알지 못한다면 이벤트 소싱/CQRS를 쓰지 마라*
- **공개 인터페이스**
  1. *패키지의 가장 단순한 인터페이스는 표준 타입을 쓰는 정적 메서드다*
  2. *다음은 커스텀 타입(아마도 메서드가 있는)을 받거나 반환하는 정적 메서드다*
  3. *다음은 사용자가 클래스를 직접 인스턴스화해야 하는 방식이다*
  4. *마지막은 사용자가 클래스를 상속해야 하는 방식이다*
- **데이터 타입**
  1. *가능하면 내장 원시 타입, 컬렉션, 그리고 그것들의 조합을 사용하라*
  2. *콜백이나 팩토리 하나만 필요하다면 불투명한 함수를 사용하라*
  3. *여러 값을 묶고 싶다면 단순한 `case class`를 사용하라*
  4. *서로 다른 여러 종류의 값을 전달하고 싶다면 `sealed trait`를 사용하라*
  5. *불투명한 `class`나 `trait`는 최후의 수단으로 사용하라*
- **에러 처리**
  1. *잘못될 수 있는 일이 하나뿐임을 안다면 Option을 사용하라*
  2. *잘못될 수 있는 일이 여러 가지임을 안다면 단순한 sealed trait를 사용하라*
  3. *무엇이 잘못될 수 있는지 모른다면 예외를 사용하라*
  4. *에러 플래그는 (거의) 절대 설정하지 마라*
- **비동기 반환 타입**
  1. *가장 단순한 경우, `T`를 반환하라*
  2. *비동기 결과에는 `Future[T]`를 반환하라*
  3. *`onSuccess: T => Unit` 같은 콜백을 받는 것은 최후의 수단이다*
- **의존성 주입**
  1. *첫째, 의존성을 하드코딩하라*
  2. *둘째, 그것이 필요한 메서드의 파라미터로 전달하라*
  3. *셋째, 감싸는 클래스에 전달해 여러 메서드에 주입하라*
  4. *넷째, 감싸는 클래스가 여러 파일의 트레이트로 나뉘어 있다면 추상 멤버를 사용하라*
  5. *다섯째, 메서드 파라미터를 implicit으로 만들어라*
  6. *다른 모든 방법이 실패하면 "동적 변수", 즉 전역 가변 상태를 사용하라*
  7. *세터 주입은 쓰지 마라*

## 철학: 최소 권한의 원칙

이 맥락에서 이 원칙은 다음과 같이 적용할 수 있다.

> 해법에 선택지가 있다면, 문제를 해결할 수 있는 것 중 가장 힘이 약한 해법을 골라라

이는 직관적으로 당연한 얘기가 아니다. 개발자는 강력하고 유연한 해법을 만들려고 애쓴다. 하지만 무엇이든 할 수 있는 강력하고 유연한 해법은 분석하기가 가장 어렵고, 몇 가지 일만 하는 — 사실 *몇 가지 일밖에 할 수 없는* — 제한된 해법은 나중에 누군가 살펴보고, 분석하고, 손대기가 훨씬 수월하다.

이 규칙의 기원은 프로그래밍 언어에 관한 것이었다. 즉,

> 언어에 선택지가 있다면, 문제를 해결할 수 있는 것 중 가장 힘이 약한 언어를 골라라

그리고 그 근거는 위에서 내가 제시한 것과 거의 같은 논리다.

왜 이 원칙이 Scala 언어 프로그래밍에 적용될까? 정반대 원칙을 상상하는 것도 어렵지 않다.

> 해법에 선택지가 있다면, 문제를 해결할 수 있는 것 중 가장 강력한 해법을 골라라

이렇게 하면 예를 들어 메타프로그래밍 같은 해법이 다른 "기본적인" 해법보다 선호될 것이다. 메타프로그래밍이 고급 기술이라서가 아니라, 아주 적은 양의 코드로 무엇이든 해낼 수 있는 엄청난 유연성을 주는 경우가 많기 때문이다. 그게 왜 나쁜가?

몇 가지 이유로 나쁘다고 주장할 수 있다.

1. Scala로 프로그래밍할 때 복잡성은 적이다
2. Scala는 정적 타입 언어이므로 리팩터링을 두려워할 필요가 없다
3. 따라서 미래의 작업을 예상해 과잉 설계할 필요가 없다. 필요할 때 리팩터링하면 된다

### 복잡성은 적이다

Scala를 쓰는 개발자들의 가장 흔한 불평은 코드가 헷갈리고 "읽기 어렵고" "복잡하다"는 것, 그리고 컴파일러가 느리다는 것이다. 컴파일 속도는 대개 손쓸 수 없는 영역이라 이야기하지 않겠지만, 나머지 불평을 감안하면 코드를 "읽기 쉽고" "덜 복잡하게" 만드는 일이 이 언어로 일하는 개발자의 우선순위가 되어야 한다.

모든 언어가 그런 것은 아니다! 예를 들어 Python이나 Ruby에서는 코드가 화이트보드에 스케치한 것과 똑같이 생겼다는 의미로 "읽기 쉽다"거나 "실행 가능한 의사코드"라고들 한다. 주된 불평은 리팩터링 가능성/유지보수성과 런타임 성능에 몰려 있다. Python이나 Ruby로 프로그래밍할 때 숙련된 프로그래머는 코드의 리팩터링 가능성을 높이는 데 공을 들인다. 예를 들어 타입 오류를 잡기 위해 유닛 테스트를 잔뜩 작성하는데, Java나 Scala 같은 정적 타입 언어에서 하는 것보다 훨씬 많이 한다. 이는 Java나 Scala에서 유닛 테스트를 덜 쓰는 것보다 "낫다"거나 "못하다"가 아니라, 언어가 제시하는 서로 다른 제약과 문제에 맞추기 위한 "다름"일 뿐이다.

Scala로 돌아오면, 개발자는 코드를 "읽기 쉽고" "덜 복잡하게" 만드는 데 추가로 공을 들여야 한다. 모든 언어에서 그래야 하는 것은 아니지만, Scala 프로그래밍 언어의 이 약점을 완화하려면 그렇게 해야 한다. 다행히 Scala는 이를 돕는 다른 도구들을 제공한다.

### 리팩터링을 두려워하지 마라

Python이나 Ruby 같은 동적 타입 언어에서는, 그리고 Java나 C처럼 정적 타이핑이 살짝 약한 언어에서조차, 사소한 리팩터링이 꽤 어렵거나 겁나는 일인 경우가 많다. 예를 들어 큰 Python 코드베이스에서 필드나 메서드 이름을 바꾸는 일은 모든 호출 지점을 제대로 고쳤는지 확신할 방법이 없어 어렵다. Java에서도 캐스팅과 리플렉션이 널리 쓰이는 탓에 클래스를 추가/삭제/수정하고 컴파일이 전부 성공했는데도 런타임에 `ClassCastException`이나 `InvocationTargetException`이 튀어나올 수 있다. 이에 맞서기 위해 사람들은 종종 약간 선제적으로 프로그래밍한다. 작은 코드베이스에서도 아직 쓰지 않는 인자를 함수에 넘겨 두거나, "필요한 것보다 더 많이" 넘긴다. 예를 들어 필요한 메서드/콜백 하나만이 아니라 객체 전체를 넘기는 식이다. 이는 대개 미묘하고 무의식적이지만, 목표는 보통 나중에 리팩터링할 필요를 피하려는 것이다. 더 많은 일을 해야 하게 되면 기존 코드를 최소한만 바꿔서 할 수 있도록 말이다.

Scala 프로그래밍 언어에서는 리팩터링을 두려워하지 않아도 된다. 함수에 인자를 하나 더 연결하는 사소한 변경부터 코드베이스의 더 큰 재구성까지, 컴파일러가 길을 안내해 준다. `.toString`이나 `==` 주변처럼 여전히 위험한 지점이 있긴 하지만, 그 수가 적어서 동적 언어의 많은 리팩터링에서처럼 "작업을 가로막는 장벽"이 아니라 "약간 성가신 골칫거리" 수준에 그친다.

### 과잉 설계하지 마라

따라서 Scala에서는 미래의 필요를 예상해 과잉 설계하는 것을 피해야 한다.

- 함수가 어떤 객체의 메서드 하나만 필요하고 나중에 다른 것도 필요할지 확신이 없다면, 객체 전체가 아니라 그 메서드만 넘겨서 다른 것을 *쓸 수 없게* 하라.

- 한 번만 쓰는 헬퍼 메서드가 나중에 재사용될지 확신이 없다면, 그것을 사용하는 메서드 안에 중첩시켜 당장은 재사용*할 수 없게* 하라.

직관에 반하는 것처럼 보일 수 있지만, 여기에는 목적이 있다. 현재 필요 없는 이 모든 일이 *일어날 수 없게* 강제함으로써, 여러분의 코드로 *할 수 있는* 일의 집합을 제한하는 것이다. 이렇게 하면 코드베이스의 서로 다른 부분 사이의 인터페이스를 최대한 강제로 좁히게 되고, 다음과 같은 이점이 생긴다.

- 인터페이스 양쪽이 독립적으로 진화할 자유가 커진다! 복잡한 객체 전체 대신 메서드 하나만 함수에 넘기면, 나중에 그 함수를 갈아 끼우기가 아주 쉬워진다.

- 코드의 어느 부분이 어느 부분과 상호작용하는지 더 잘 추론할 수 있다. 객체 전체가 아니라 메서드 하나만 함수에 넘기면 그 함수가 그 호출 하나만 쓴다는 것을 *즉시* 알 수 있다. 이전에는 어디서 쓰이는지 보려고 소스를 파헤쳐야 했을 것이다.

결과적으로, 선제적 과잉 설계를 하지 않음으로써 우리는 *수정하기 쉬움*을 *읽고 이해하기 쉬움*과 맞바꾸는 것이다. 그런데 앞서 말한 대로 Scala의 약점이 복잡한 코드를 이해하기 어렵다는 것이고 강점이 리팩터링이 쉽다는 것이라면, 이는 합리적인 거래다. Scala가 잘하는 것(리팩터링)에 기대어 Scala가 못하는 것(복잡성)의 문제를 완화하는 셈이다. 아래 가이드라인들은 바로 이 원칙에서 나온다.

## 불변성과 가변성

불변성과 가변성 사이에서 결정할 때는...

1. *가능한 한 불변성을 사용하라*
2. *실제로 변하는 것을 모델링하는 경우가 아니라면*
3. *또는 성능을 위한 경우가 아니라면*
4. *가변성을 쓰더라도 범위를 최대한 좁혀라*
5. *이중 가변성은 쓰지 마라. 아마 필요 없을 것이다*
6. *무엇을 하는지 확실히 알지 못한다면 이벤트 소싱/CQRS를 쓰지 마라*

불변인 것은 변하지 않고, 변하지 말아야 할 것이 변하는 것은 흔한 버그의 원천이다. 나중에 무언가를 바꿔야 할지 확신이 없다면 일단 `val`이나 `collection.Seq`로 불변으로 두고, 필요해질 때 `var`나 `mutable.Buffer`로 옮겨 가라.

### 기본은 불변성

이건 괜찮다.

```scala
val x = if(myBoolean) expr1 else expr
```

이건 안 된다.

```scala
var x: ExprType = null
if(myBoolean) x = expr1 else x = expr
```

불변 스타일로 곧바로 표현할 수 있는 것이라면 그렇게 하라.

### 변하는 것에는 가변성

일반적으로, 시간이 지나며 실제로 변하는 무언가를 모델링하고 있다면 가변 `var`나 `mutable.Buffer` 같은 컬렉션을 쓰는 것은 괜찮다. 예를 들어 비디오 게임에서는 이런 코드가 있을 수 있다.

```scala
class Item{ ... }

class Player(var health: Int = 100, val items: mutable.Buffer[Item] = mutable.Buffer.empty)

val player = new Player()
```

여기서는 `health`와 `items`가 시간에 따라 변할 수 있는 무언가를 실제로 모델링하고 있다. 이건 괜찮다. 이론적으로는 이벤트 소싱이나 CQRS 같은 화려한 기법으로 이를 "불변으로" 모델링할 수도 있다. 실전에서는 변하는 것을 가변 상태로 모델링하는 것으로 충분하다.

반면 이건 안 된다.

```scala
class Item{ ... }

class Player(var health: Int = 100, var items: mutable.Buffer[Item] = null)

val player = new Player()

player.items = mutable.Buffer.empty[Items]
```

여기서는 `items` 변수를 `null`로 초기화해 놓고 나중에 빈 리스트로 다시 "제대로" 초기화한다. Java를 비롯한 다른 언어에서 아주 흔한 패턴인데, 이건 안 된다. `player.items`를 사용하기 전에 변경하는 것을 잊거나, 더 그럴듯하게는 여러분이 쓰는 어떤 메서드가 `player.items`가 설정되기 전에 그것을 쓴다는 사실을 잊으면, 지금 당장 또는 (더 나쁘게는) 한참 뒤에 `NullPointerException`으로 터진다.

이 원칙을 어기는 실제 사례를 [Scala Parallel Collections 라이브러리](http://docs.scala-lang.org/overviews/parallel-collections/configuration.html#task-support)에서 가져왔다.

```scala
scala> import scala.collection.parallel._
import scala.collection.parallel._

scala> val pc = mutable.ParArray(1, 2, 3)
pc: scala.collection.parallel.mutable.ParArray[Int] = ParArray(1, 2, 3)

scala> pc.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(2))

scala> pc map { _ + 1 }
res0: scala.collection.parallel.mutable.ParArray[Int] = ParArray(2, 3, 4)
```

보다시피 가변 속성 `.tasksupport`를 사용해 병렬 `map` 연산이 어떻게 실행될지 설정한다. 이건 나쁘다. 명시적으로든 implicit으로든 `map`의 파라미터로 쉽게 넘길 수 있었기 때문이다. `tasksupport`는 실제로 변하는 값을 모델링하는 것이 아니므로, 초기화만을 위해 가변 `var`를 쓰는 것은 명백히 나쁜 스타일이고 이걸 작성한 사람은 반성해야 한다.

일반적으로, 모델링하는 대상이 시간에 따라 변한다면 가변성을 써도 괜찮다. 모델링하는 대상이 변하지 않고 그저 초기화 과정의 일부로 가변성을 쓰고 있다면, 다시 생각해 봐야 한다.

### 성능을 위한 가변성

이건 괜찮다.

```scala
val fibs = mutable.Buffer(1, 1)
while(fibs.length < 100){
  fibs.append(fibs(fibs.length-1) + fibs(fibs.length-2))
}
```

가변 코드는 불변 버전보다 자릿수가 다른 수준으로 빠른 경우가 아주 많다. 알고리즘을 구현할 때는 더 두드러지는데, CLRS 같은 책의 흔하고 빠른 알고리즘들은 가변 방식으로 되어 있기 때문이다. 소량의 가변성으로 성능을 10배, 100배 끌어올릴 수 있다면, 병렬화, 캐싱, 배칭 같은 온갖 골치 아픈 것들을 걷어내 코드의 *나머지* 부분을 단순화할 수 있다. 그 거래를 두려워하지 마라.

### 가변성의 범위를 제한하라

이것이

```scala
def getFibs(n: Int): Seq[Int] = {
  val fibs = mutable.Buffer(1, 1)
  while(fibs.length < n){
    fibs.append(fibs(fibs.length-1) + fibs(fibs.length-2))
  }
  fibs
}
```

이것보다 낫다.

```scala
def getFibs(n: Int, fibs: mutable.Buffer[Int]): Unit = {
  fibs.clear()
  fibs.append(1)
  fibs.append(1)
  while(fibs.length < n){
    fibs.append(fibs(fibs.length-1) + fibs(fibs.length-2))
  }
}
```

코드 일부에 가변성을 도입하기로 결정했더라도, 불필요하게 사방으로 새어 나가게 두지 마라! 이상적으로는 함수 하나 안에 전부 캡슐화되어, 바깥세상에서는 내부를 불변으로 구현한 같은 함수와 똑같아 보여야 한다.

때로는 가변성이 함수, 클래스, 모듈 경계를 넘어야 *하는* 경우도 있다. 예를 들어 성능이 필요하다면 위의 두 번째 예제가 첫 번째보다 실제로 빠르고, 할당을 줄여 가비지 컬렉션 부담도 줄인다. 그래도 그 성능이 필요하다고 100% 확신하는 게 아니라면 첫 번째 예제를 기본으로 삼아라.

### 이중 가변성 금지

Java에서 이것은 나쁘지만 아주 흔하다.

```java
ArrayList<Int> myList = new java.util.ArrayList<Int>()
```

Scala에서 동등한 나쁜 코드는 이렇다.

```scala
var myList = mutable.Buffer[Int]
```

하지만 컨테이너도 가변이고 컨테이너를 담는 변수도 가변이어야 하는 경우는 *거의 없다*! 더 나은 Java 코드는

```java
final ArrayList<Int> myList = new java.util.ArrayList<Int>()
```

Scala에서는

```scala
val myList = mutable.Buffer[Int]
```

또는

```scala
var myList = Vector[Int]
```

불변 컬렉션을 담는 가변 `var`로 할지, 가변 컬렉션을 담는 불변 `val`로 할지는 논쟁의 여지가 있지만, 이중으로 가변이길 원하는 경우는 기본적으로 없다.

### 이벤트 소싱/CQRS

가변적인 상태 변경 코드를 "순수"하거나 "불변"으로 만드는 기법 이야기를 종종 듣게 된다. 대신 거의 불변인 추가 전용(append-only) 이벤트 로그를 저장하는 방식이다. 이는 불변성의 많은 이점을 지닌다. 새 이벤트가 상황을 바꾸더라도 옛 이벤트들이 그대로 남아 있어서, 특정 시점까지의 이벤트를 재생하면 그 시점의 시스템 "상태"를 조회할 수 있다. 가변 `var`나 `mutable.Buffer` 같은 컬렉션으로는 할 수 없는 일이고, 이 기법은 여러 곳에서 큰 효과를 낸다. 예를 들어

- 데이터베이스. 트랜잭션 로그 덕분에 "스트리밍" 복제와 트랜잭션 간 격리가 가능하다.

- 비디오 게임. 입력 로그를 저장하면 세션 동안 일어난 모든 일을 나중에 다시 재생해 볼 수 있다.

이 기법들은 인메모리로도, 디스크에 영속화해서도, 나아가 추가 전용 로그를 보관하는 완전한 데이터베이스/데이터스토어와 함께도 사용할 수 있다. 이 기법들이 어떻게 동작하는지 온전한 설명은 이 문서의 범위를 벗어난다.

일반적으로, 이 기법들이 주는 이점 — 재생 가능성, 격리, 스트리밍 복제 — 을 원한다면 얼마든지 사용하라. 하지만 대부분의 사용 사례에는 과한 도구일 가능성이 높으므로, 그 이점이 필요하다는 것을 확실히 알기 전에는 기본으로 쓰지 말아야 한다.

## 공개 인터페이스

다른 사람이 사용할 패키지의 인터페이스를 정의할 때는...

1. *패키지의 가장 단순한 인터페이스는 표준 타입을 쓰는 정적 메서드다*
2. *다음은 커스텀 타입(아마도 메서드가 있는)을 받거나 반환하는 정적 메서드다*
3. *다음은 사용자가 클래스를 직접 인스턴스화해야 하는 방식이다*
4. *마지막은 사용자가 클래스를 상속해야 하는 방식이다*

"공개 인터페이스(Published Interface)"라는 용어는 "보통의" Java `interface`나 Scala `trait`와 구별해서 쓴다. 공개 인터페이스는 프로그램의 제법 큰 부분들 사이의 더 큰 규모의 인터페이스로, 여러 `class`와 `object`로 구성된다. `package`나 `.jar` 전체가 개발자에게 제시하는 인터페이스를 말한다.

### 정적 메서드만

아래는 바랄 수 있는 최고의 공개 인터페이스다.

```scala
object TheirCode{
  /**
   * `T`의 집합과, 각 `T`의 나가는 간선으로부터 어떤 다른 `T`에
   * 도달할 수 있는지 정의하는 함수를 받는다
   *
   * 강한 연결 요소(strongly connected component)의 집합을 반환한다
   * (각 요소는 Set[T])
   *
   * 노드 수 N과 전체 간선 수 E에 대해 O(N + E)
   */
  def stronglyConnectedComponents[T](nodes: Set[T],
                                     edges: T => Set[T]): Set[Set[T]] = {
    ... 500줄의 정신 나간 코드 ...
  }
}

object MyCode{
  import TheirCode._
  stronglyConnectedComponents(..., ...)
}
```

이 인터페이스는 사소하지 않은 알고리즘을 캡슐화한다. 내부에서는 가변 상태를 잔뜩 쓰고, 여러분이 직접 만들어 내지는 못했을 법한 알고리즘이다.

Tarjan 알고리즘을 쓰고 있을까? 이중 스택(Double-Stack) 알고리즘을 쓰고 있을까? 이 인터페이스의 소비자 입장에서는 상관없다. `Set[T]`와 나가는 간선을 나타내는 `T => Set[T]`를 넣으면 모든 강한 연결 요소의 `Set[Set[T]]`가 튀어나온다는 것만 알면 된다. 500줄짜리 정신 나간 알고리즘 코드가 있는 게 보이지만, 그중 어느 것도 신경 쓸 필요가 없다. 2줄짜리 시그니처와 5줄짜리 문서 주석이, 이걸 여러분의 코드에서 사용하는 데 알아야 할 전부다.

물론 *항상* 알려진 타입만 다루는 초단순 정적 함수 하나짜리 인터페이스를 제공할 수 있는 것은 아니다. 코드가 한 가지 이상의 일을 할 수도 있고, 함수 하나의 인자에 다 욱여넣을 수 없을 만큼 다양한 설정이 필요할 수도 있다. 이런 아주 단순한 "알려진 타입만 다루는 정적 함수뿐인 인터페이스"는 지향해야 할 목표다.

### 커스텀 타입의 인스턴스화

아래는 그보다 못한 인터페이스다.

```scala
object TheirCode{
  trait Frag{
    def render: String
  }
  // HTML 생성자들
  def div(children: Frag*): Frag
  def p(children: Frag*): Frag
  def h1(children: Frag*): Frag
  ...
  implicit def stringFrag(s: String): Frag
}

object MyCode{
  import TheirCode._
  val frag = div(
    h1("Hello World"),
    p("I am a paragraph")
  )
  frag.render // <div><h1>Hello World</h1><p>I am a paragraph</p></div>
}
```

여기서 `TheirCode`의 인터페이스는 개발자에게 요구하기 시작한다. "함수 하나"만 호출해서 원하는 것을 얻을 수 없고, 이제 `Frag`가 무엇인지, `TheirCode`가 노출하는 정적 메서드 중 어느 것이 `Frag`를 반환하는지, implicit 변환으로 자기 것을 어떻게 `Frag`로 바꾸는지 배워야 한다. 마지막으로 *자신이* `Frag`로 무엇을 할 수 있는지도 알아야 한다. 이 경우에는 `render`를 호출해 `String`으로 바꿀 수 있다. 그리고 *그제서야* 개발자는 이 라이브러리로 쓸모 있는 일을 할 수 있다! 결국 여러분의 라이브러리를 쓰려는 개발자가 생각하는 것은

> `Frag`를 만들고 다루는 법을 배우고 싶다

가 아니라

> HTML 문자열을 만들고 내 문자열 몇 개를 거기 넣고 싶다

이다. 물론 개발자는 원하는 일을 하기 전에 이 `Frag` 관련 내용을 전부 배워야 *하겠지만*, 배워야 할 것이 적을수록 좋다.

이 예제 인터페이스가 끔찍하게 복잡한 것은 아니지만, 앞의 `stronglyConnectedComponents` 예제보다는 확실히 복잡하다! 그리고 이건 전부 아니면 전무가 아니다. 사용자가 배워야 할 커스텀 타입과 생성자를 더 많이 도입할 수도, 덜 도입할 수도 있으며, 많아질수록 외부인이 여러분의 코드 사용법을 파악하기가 그만큼 어려워진다.

거듭 말하지만 인터페이스를 이보다 단순하게 만드는 것이 항상 가능하지는 않고, 나도 이런 스타일의 인터페이스를 쓰는 코드를 많이 공개했다. 그래도 클래스 상속을 요구하는 것보다는 낫다...

### 클래스 상속

API 사용자에게 클래스나 트레이트 상속을 강제하는 것은 최후의 수단이어야 한다. 상속 중심으로 설계된 API를 못 쓴다는 얘기가 아니다. Java 세계에서는 오랫동안 그렇게 써 왔다. 그래도 정적 함수 몇 개를 노출하는 것, 개발자가 다뤄야 할 클래스/타입을 노출하는 것, 개발자에게 여러분의 클래스/트레이트 상속을 강제하는 것 사이에 선택지가 있다면, 상속은 목록의 맨 마지막에 두고 최후의 수단으로만 써야 한다.

## 데이터 타입

어떤 값에 어떤 종류의 타입을 쓸지 고를 때는...

1. *가능하면 내장 원시 타입, 컬렉션, 그리고 그것들의 조합을 사용하라*
2. *콜백이나 팩토리 하나만 필요하다면 불투명한 함수를 사용하라*
3. *여러 값을 묶고 싶다면 단순한 `case class`를 사용하라*
4. *서로 다른 여러 종류의 값을 전달하고 싶다면 `sealed trait`를 사용하라*
5. *불투명한 `class`나 `trait`는 최후의 수단으로 사용하라*

이는 그 값이 메서드 파라미터든, 클래스 파라미터든, 필드든, 메서드 반환 타입이든 똑같이 적용된다. 일반적으로 각 사용 사례에 "가장 단순한" 것을 쓰면, 나중에 코드를 보는 사람이 그 값에 대해 더 강한 가정을 할 수 있다.

원시 타입이나 내장 컬렉션이 보이면 무엇이 들어 있는지 정확히 안다. 불투명한 함수가 보이면 할 수 있는 일이 호출뿐임을 안다. 단순한 `case class`가 보이면 멍청한 구조체(dumb struct)라고 꽤 확신할 수 있다. `sealed trait`라면 여러 멍청한 구조체 중 하나일 수 있다. 손수 만든 커스텀 `class`나 `trait`라면 아무것도 장담할 수 없다. 뭐든 될 수 있다!

가장 단순한 타입에서 시작해 필요할 때만 목록을 따라 힘이 더 센 쪽으로 내려감으로써, 최소 권한의 원칙을 지키고 API 사용자에게 그 값이 어떻게 쓰일지에 대한 신호를 보내는 것이다.

### 내장 타입

가능한 곳에서는 항상 내장 원시 타입과 컬렉션을 써야 한다. 저마다 이런저런 결함이 있긴 하지만, 잘 알려져 있고 "지루"하며, 여러분의 인터페이스 사용법을 살펴보는 사람은 이미 아는 데이터 타입으로만 상호작용하면 된다는 것을 안다. 같은 개념을 직접 만든 커스텀 버전 대신 표준 `Int`, `String`, `Seq`, `Option`과 그 조합을 써야 한다.

객체를 메서드에 넘기는데 그 메서드가 객체의 필드 하나만 접근한다면, 그 필드를 직접 넘기는 것을 고려하라. 예를 들어 이것은

```scala
class Foo(val x: Int, val s: String, val d: Double){
  ... 다른 것들 ...
}

def handle(foo: Foo) = {
  ... foo.x ... // foo의 유일한 사용처
}

val foo = new Foo(123, "hellol", 1.23)

handle(foo)
```

이렇게 다시 쓰는 편이 나을 수 있다.

```scala
class Foo(val x: Int, val s: String, val d: Double)

def handle(x: Int) = {
  ... x ... // x의 유일한 사용처
}

val foo = new Foo(123, "hellol", 1.23)

handle(foo.x)
```

이렇게 하면 `handle`이 `x: Int`와 `s: String`과 그 밖의 것들을 담고 있는 `Foo` 전체를 *실제로* 필요로 하지 않는다는 점이 분명해진다. 정수 `x` 하나만 필요할 뿐이다. 게다가 코드베이스의 다른 곳에서 `handle`을 재사용하거나 유닛 테스트에서 돌려 볼 때, `Int` 하나 넘기고 나머지는 버리자고 `Foo` 전체를 인스턴스화하는 수고를 겪을 필요가 없다.

마찬가지로, 객체를 반환하는데 그중 필드 하나만 쓰인다면 그 필드만 반환하라. 나중에 객체의 나머지가 필요해지면 그때 리팩터링해서 쓸 수 있게 하면 된다.

### 함수

내장 타입보다 복잡한 것이 불투명한 함수다. `Function0[R]`/`() => R`이든, `Function1[T1, R]`/`T1 => R`이든, 번호가 더 큰 `FunctionN`이든 마찬가지다. 이 타입들로 할 수 있는 유일한 일은 인자를 넣어 호출해서 반환값을 얻는 것뿐이다.

함수를 받고 반환하는 것은 Java 세계에서 단일 추상 메서드(single-abstract-method) 인터페이스를 받고 반환하던 많은 경우를 대체할 수 있다. 예를 들어 [Runnable](https://docs.oracle.com/javase/7/docs/api/java/lang/Runnable.html)과 [Comparator[T]](https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html)는 Scala 세계에서 `() => Unit`과 `(T, T) => Int`로 대체할 수 있다.

일반적으로, 객체를 메서드에 넘기는데 그 메서드가 객체의 메서드 하나만 호출한다면, 그 메서드가 `FunctionN`을 받게 하고 람다를 넘기는 것을 고려하라. 예를 들어 이것은

```scala
class Exponentiator(val x: Int){
   ... 몇몇 메서드 ...
  def exponentiate(d: Double): Double = math.pow(d, x)
   ... 더 많은 메서드 ...
}

def handle(exp: Exponentiator) = {
  val myDouble = ...
  ... exp.exponentiate(myDouble) ... // foo의 유일한 사용처
}

val myExp = new Exponentiator(2)

handle(myExp)
```

이렇게 다시 쓰는 편이 나을 수 있다.

```scala
class Exponentiator(val x: Int){
   ... 몇몇 메서드 ...
  def exponentiate(d: Double): Double = math.pow(d, x)
   ... 더 많은 메서드 ...
}

def handle(op: Double => Double) = {
  val myDouble = ...
  ... op(myDouble) ... // foo의 유일한 사용처
}

val exp = new Exponentiator(2)

handle(exp.exponentiate)
```

역시 이 변환은 `handle` 함수가 `exp`의 어느 부분을 쓰는지 더 분명하게 드러내 주고, 덕분에 `handle`을 다른 곳에서 쓰거나 유닛 테스트에서 로직을 스텁으로 대체하기가 쉬워진다.

### 단순한 case class

역시 내장 타입보다 복잡하지만 함수와는 다른 방향으로 복잡한 것이 case class다. 여러 값을 즉석에서 묶고 각 구성 요소에 이름을 붙일 수 있게 해 준다. 직접 만든 임의의(ad-hoc) 클래스가 아니라 "단순한" `case class`로 하는 것의 장점은, 여러분의 `case class`를 보는 모든 사람이 무엇을 기대할지 즉시 안다는 것이다. 생성자, 접근자, 패턴 매칭, hashCode, toString, 동등성 비교 — 멍청한 구조체 스타일의 객체를 다룰 때 있으면 좋은 것들 전부다.

예를 들어 이런 코드를 작성하고 있다면

```scala
class Foo(_x: Int, _y: Int){
  def x = _x
  def y = _y
  def hypotenus = math.sqrt(x * x + y * y)
  override def hashCode = x + y // 즉석에서 만든 커스텀 해시 함수
  override def equals(other: Any) = other match{
    case f: Foo => f.x == x && f.y == y
    case _ => false
  }
}
```

대신 이렇게 쓰는 것을 고려하라.

```scala
case class Foo(x: Int, y: Int){
  def hypotenus = math.sqrt(x * x + y * y)
}
```

이는 여러 목적에 부합한다.

- 장황함을 줄인다

- 잘못될 수 있는 지점을 줄인다(예: 위의 즉석 커스텀 해시 함수)

- 우리의 멍청한 구조체가 다른 모든 사람의 멍청한 구조체처럼 동작하게 만든다

일반적으로 멍청한 구조체 클래스와 더 복잡한 임의의 클래스 사이에 뚜렷한 경계선은 없다. 멍청한 구조체도 소량의 로직(예: 위의 `hypotenus` 메서드)을 담을 수 있고 실제로 자주 담으며, 임의의 클래스도 소량의 "멍청한" 데이터를 담는다. 그래도 클래스의 주된 목적이 코드의 저장소가 아니라 "멍청한" 데이터 구조라면 `case class`로 만드는 것을 고려하라.

### sealed trait

여러 가지 중 하나를 전달해야 하고 그 각각이 "멍청한 구조체"라면 `sealed trait` 사용을 고려하라. 이는 미래의 유지보수자에게 이 타입에 속할 수 있는 것들의 집합이 유한하고 고정되어 있음을 알려 준다. "새로운" 클래스가 나타나 그 인터페이스/트레이트를 상속하고 그것까지 처리해야 하는 상황을 걱정할 필요가 전혀 없다.

### 임의의 클래스

값에 부여할 수 있는 타입 중 가장 강력한 것이다. 내장 타입, 함수, `case class`를 볼 때 코드 사용자는 대략 무엇을 기대할지 한눈에 안다. 임의의 `class`나 `trait`를 볼 때는 그 클래스가 어떻게 쓰일지 아무것도 알 수 없고, 실제로 문서를 읽거나 소스 코드를 파헤쳐야 한다.

게다가 임의의 클래스는 말 그대로 임의적이라서, 더 단순한 데이터 타입이 제공하는 구체적인 기능들을 잃는다. 예를 들면

- 내장 타입과 원시 타입은 리플렉션 없이 implicit만으로 전부 쉽게 직렬화할 수 있다(예: [spray-json](https://github.com/spray/spray-json) 사용)
- case class와 sealed trait는 리플렉션 없이 매크로 몇 개로 거의 쉽게 직렬화할 수 있다(예: [spray-json-shapeless](https://github.com/fommil/spray-json-shapeless)나 [uPickle](http://lihaoyi.github.io/upickle-pprint/upickle/) 사용)
- 내장 타입, 원시 타입, case class는 모두 자동으로 구조적 `==` 동등성, `.hashCode`, 의미 있는 `.toString`을 제공한다
- 내장 타입과 case class는 모두 구조 분해 패턴 매칭을 지원하며, 이는 종종 매우 편리하다

임의의 클래스를 쓰기 시작하면 이 모든 것을 잃는다. 그들이 주는 유연성의 대가로 수많은 기능을 잃는 것이다!

임의의 클래스나 트레이트가 *나쁘다*는 말이 아니다. 사실 대부분의 Scala 프로그램에는 그것이 잔뜩 있고, Java 프로그램은 기본적으로 100% 임의의 클래스다! 요점은, 가능한 곳에서는 임의의 클래스 대신 더 단순한 것 — 단순한 case class, sealed trait, 함수, 내장 타입 — 을 쓰려고 노력해야 한다는 것이다. 거듭 말하지만 이는 타입이 보이는 모든 곳에 적용된다. 메서드 인자로든, 반환 타입으로든, 메서드 지역 변수든 객체나 클래스 소속이든 변수와 값의 타입으로든.

## 에러 처리

1. *잘못될 수 있는 일이 하나뿐임을 안다면 Option을 사용하라*
2. *잘못될 수 있는 일이 여러 가지임을 안다면 단순한 sealed trait를 사용하라*
3. *무엇이 잘못될 수 있는지 모른다면 예외를 사용하라*
4. *에러 플래그는 (거의) 절대 설정하지 마라*

일반적으로 `Option`, 단순한 sealed trait, 예외가 에러를 다루는 가장 흔한 방법이다. 여러분이 쓰게 될 여러 API가 이 기법들을 뒤죽박죽 섞어 쓰고 있을 가능성이 높다. 대체로 이런 뒤섞인 에러들은 최대한 빨리 감싸서 일관된 형식으로 만드는 것이 합리적이다. 예를 들어 sealed trait를 쓰기로 했다면, 알려진 예외들을 전부 `try`-`catch`로 감싸고 `Option`을 반환하는 함수의 `Some`/`None`을 여러분의 sealed trait의 해당 하위 클래스로 변환하라. 예외를 쓰기로 했다면 `Option.getOrElse(throw new MyCustomException)`으로 `Option`들을 더 알기 쉬운 이름의 예외로 변환하라.

### Option

함수에서 에러를 반환하는 가장 단순한 메커니즘이다. 결과를 담은 `Some`을 받거나, 아무것도 없는 `None`을 받는다.

단순한 경우에는 아주 잘 동작한다. 예를 들어 `Map[K, V]#get(k: K)`가 `Option[T]`를 반환하는 것은 잘못될 수 있는 일이 정말로 하나뿐이기 때문이다. 키가 존재하지 않는 것 말이다. 키가 *왜* 존재하지 않는지 말할 것도 없고, 그 밖에 잘못될 수 있는 일도 없다. 이것이 `Option`의 이상적인 사용 사례다. `for` 컴프리헨션이나 `map`/`flatMap`으로 연결해서 실패를 쉽게 전파할 수도 있다.

`Option`의 단점은 에러에 대한 추가 정보를 담을 곳이 전혀 없다는 것이다. 여러 실패를 구별해야 한다면, 예를 들어 사용자에게 에러 메시지를 보여 주기 위해서라면, 더 강력한 무언가가 필요하다. 작고 고정된 에러 집합을 다루기 위해 `Option[Option[T]]`나 심지어 `Option[Option[Option[T]]]`를 반환할 수도 있겠지만, 반환값으로 쓸 커스텀 sealed trait를 정의하는 편이 아마 더 합리적이다.

### 단순한 sealed trait

`Option[T]`로 유연성이 부족할 때는 에러를 표현하는 커스텀 sealed trait를 정의하는 방법으로 물러날 수 있다. 예를 들어

```scala
sealed trait Result
object Result{
  case class Success(value: String) extends Result
  case class Failure(msg: String) extends Result
  case class Error(msg: String) extends Result
}

def functionMayFail: Result
```

이렇게 하면 `functionMayFail`을 호출하는 누구든 가능한 경우가 셋 — `Success`, `Failure`, `Error` — 이고 각각에 고유한 결과가 딸려 있음을 보게 된다. 직접 `map`/`flatMap`을 정의하면 `for` 컴프리헨션에서 사용해 다양한 실패 상태를 전파할 수 있다.

### 예외

예외는 논쟁적이지만, "거의 절대" 잘못될 리 없는데도 실제로는 잘못되는 일들에는 제자리가 있다. `/ by zero`, `ClassNotFound`, `NoSuchFileException`, `StackOverflowError`, `OutOfMemoryError`, 키보드 인터럽트 등등.

일반적으로, 이런 에러로 실패할 수 있는 모든 것이 Option이나 어떤 ADT를 반환하게 만드는 것은 대개 실현 불가능하다. 코드베이스의 모든 표현식이 잠재적으로 `StackOverflow`나 `OutOfMemory`나 `ClassNotFound`를 일으킬 수 있다! 산술 코드는 `/` 호출로 가득한데, 그 하나하나를 `Option`으로 감싸는 것은 (보일러플레이트 측면에서도 성능 비용 측면에서도) 무리다. 그러면 뭔가 잘못되면 어떻게 되는가?

이런 경우가 나타나면 프로그램을 멈추고 디버깅하겠다고 결정할 수 있다. 그리고 그것이 바로 잡지 않은 예외가 하는 일이다. 스택을 타고 올라가 무엇이 잘못됐는지 디버깅을 돕는 메시지와 스택 트레이스를 남기며 프로그램을 종료한다. 완벽하다!

문제가 발생한 뒤 어떤 코드를 실행하고 싶다고 결정할 수도 있다. 예를 들어 나중에 다른 곳에서 디버깅할 수 있게 에러를 보고하고, 예상치 못한 실패에도 불구하고 "다음" 작업은 영향받지 않을 테니 프로세스를 계속 돌리는 것이다. 그럴 때 예외를 잡고 계속 진행하면 된다!

예를 들어 한 가지 작업을 수행하는 크고 복잡한 서브시스템을 작성하고 있다면, 다음을 가정하는 것이 대개 합리적이다.

1. 온갖 "불가능한" 에러를 일으키는 버그가 있을 것이다
2. 에러가 발생하면 진단 정보와 함께 어떻게든 보고하고 싶다
3. 이 버그들에도 불구하고 프로그램의 나머지는 계속 돌아가길 원한다

이것이 그 서브시스템 호출 주위에 큰 `try`-`catch`를 두르고, 예외를 로깅하고, 넘어가는 것을 고려할 만한 경우다. 이론적으로는 실패 가능 지점 하나하나에 세밀한 `try`-`catch`를 두르고 전부 `Option`이나 자체 `sealed trait`로 변환한 뒤 같은 방식으로 에러를 로깅하고 넘어갈 수도 *있지만*, 그렇게 해 봤자 (이 경우에는) 아무 이득 없이 코드만 상당히 뒤엉키게 만들 뿐이다.

### 에러 플래그

에러 플래그는 대략 이런 관례다.

- 성공하면 무언가를 한다
- 실패하면 아무것도 하지 않고 특별한 변수를 설정한다. 예를 들어

```scala
var error = -1 // 에러 없음

def doThing() = {
  ...
  if (didntWork) error = 5 //
  ...
}

doThing()
if (error == 5) println("It failed =/")
```

여기에는 몇 가지 문제가 있다.

- 위에서 다룬 Option, ADT, 예외를 쓰는 경우보다 훨씬 안전하지 않다. 에러 플래그 확인을 잊으면 프로그램은 계속 돌아가면서 잘못된 일을 할 수도 있다!
- 에러 플래그 확인을 잊기가 *엄청나게* 쉽다. 타입 시그니처에 드러나지 않고, 메서드마다 매번 확인해야 하는 특별한 가변 변수가 있다는 사실을 기억해야 한다.

전반적으로, 가능한 한 피해야 한다.

*때로는 피할 수 없다*. 에러 플래그는 "뭔가 실패했다"는 정보를 전달하는 아마도 가장 빠른 방법이고, 핫 코드 경로에서는 그것이 중요할 수 있다. 성능을 위한 가변성과 마찬가지로, 코드를 프로파일링해서 병목이 에러 처리임을 확인한 뒤 마지막 5%의 성능을 짜내기 위해 *가끔* 에러 플래그로 내려가는 것은 합리적이다.

그렇게 하더라도 에러 플래그를 최대한 단단히 캡슐화해서 클래스나 메서드나 패키지 내부에 국한시키고, 그것이 위험한 성능 해킹임을 문서에 미친 듯이 적어 놓아라.

## 비동기 반환 타입

1. *가장 단순한 경우, `T`를 반환하라*
2. *비동기 결과에는 `Future[T]`를 반환하라*
3. *`onSuccess: T => Unit` 같은 콜백을 받는 것은 최후의 수단이다*

비동기 없이 해결할 수 있다면 그렇게 해야 한다. 그렇지 않다면 `Future[T]`가 첫 번째로 선호되는 비동기 반환 타입이다. 콜백 함수로 비동기 결과를 "반환"하는 것은 `Future`가 통하지 않을 때(예: 한 번 이상 호출되어야 할 때)만 사용하라.

### T 반환

```scala
def foo(): T
```

가장 단순한 경우이고, 거의 논할 가치도 없다. 비동기 없이 해결할 수 있다면 그렇게 하라. 코드는 더 빨리 돌고, 스택 트레이스는 더 유용해지고, 디버거는 더 잘 동작한다. 논블로킹 비동기가 그렇게나 화제여도, 그럴 수 있는 상황이라면 평범한 옛날식 동기 코드가 더 잘 동작한다.

### Future 반환

```scala
def foo(): Future[T]
```

다음 대안은 비동기 결과를 나타내는 `Future[T]`를 반환하는 것이다. 다른 언어들(예: Java나 Javascript)과 저수준 API들이 콜백을 자주 쓰긴 하지만, 최소 권한 척도에서는 `Future`가 먼저 온다. 콜백보다 엄격하게 약하기 때문이다. 콜백과 달리 결과를 단 한 번만 제공할 수 있다.

이 척도를 따르면, 여러분의 코드에서 `Future`를 받은 사람은 그것이 딱 한 번만 발화한다는 것을 안다. 게다가 여러분의 코드에 비동기 콜백을 넘기는 사람은 그것이 여러 번 발화하리라는 것을 *안다*. 그렇지 않았다면 대신 `Future`를 줬을 테니까!

`Future`는 콜백보다 많은 이점을 제공한다. 예를 들어 연속된 `Future`들은 콜백으로는 할 수 없는 방식으로 "평평하게 펼칠" 수 있다. 또 `Future`는 기본적으로 에러를 전파하는 반면, 콜백 기반 API에서는 에러를 실수로 무시하기가 아주 쉽다.

### 콜백 받기: T => Unit

```scala
def foo(onSuccess: T => Unit): Unit
```

콜백은 비동기 결과를 제공하는 가장 오래되고 가장 "날것"의 방법이다. 함수를 호출하면, 완료됐을 때 여러분이 넘겨준 함수를 결과와 함께 호출해 주고, 그 결과로 무언가를 할 수 있다.

일반적으로 가능하면 `Future`를 선호하고 콜백은 피해야 한다. 여러 번 발화하는 경우처럼 `Future`가 통하지 않고 콜백이 필요한 경우도 있지만, `Future`가 통하는 곳에서는 `Future`를 써라.

## 의존성 주입

1. *첫째, 의존성을 하드코딩하라*
2. *둘째, 그것이 필요한 메서드의 파라미터로 전달하라*
3. *셋째, 감싸는 클래스에 전달해 여러 메서드에 주입하라*
4. *넷째, 감싸는 클래스가 여러 파일의 트레이트로 나뉘어 있다면 추상 멤버를 사용하라*
5. *다섯째, 메서드 파라미터를 implicit으로 만들어라*
6. *다른 모든 방법이 실패하면 "동적 변수", 즉 전역 가변 상태를 사용하라*
7. *세터 주입은 쓰지 마라*

### 하드코딩

```scala
def sayHello(msg: String) = println("Hello World: " + msg)

sayHello("Mooo")
```

가장 단순한 경우이고 매우 흔하다. 의존성(예: `println`)을 하드코딩해도 문제없다면 그냥 그렇게 하라. 갈아 끼울 유연성이 추가로 필요해지면 나중에 리팩터링하라.

### 메서드 파라미터

```scala
def sayHello(msg: String, log: String => Unit) = log("Hello World: " + msg)

sayHello("Mooo", System.out.println)
sayHello("ErrorMooo", System.err.println)
```

다음 단계로, `println`을 갈아 끼워야 한다면 메서드 파라미터로 전달하라. 여기서는 `System.out.println`으로도 `System.err.println`으로도 호출할 수 있고, 원격 로거로도, 결과를 검사하기 위한 테스트 로거로도 호출할 수 있음을 볼 수 있다.

### 생성자 주입

같은 로거를 수많은 함수에 넘기고 있다면

```scala
def func(log: String => Unit) = ... func2(log) ...
def func2(log: String => Unit) = ... func3(log) ...
def func3(log: String => Unit) = ... func4(log) ...
def func4(log: String => Unit) = ... func5(log) ...
def func5(log: String => Unit) = ... func6(log) ...
def func6(log: String => Unit) = ... func7(log) ...
def func7(log: String => Unit) = ... func8(log) ...
def func8(log: String => Unit) = ... func9(log) ...
def func9(log: String => Unit) = ... sayHello("Moo", log) ...

def sayHello(msg: String, log: String => Unit) = log("Hello World: " + msg)

func(System.out.println)
func(System.err.println)
```

그 함수들을 클래스로 감싸고 클래스에 주입할 수 있다.

```scala
class Container(log: String => Unit){
  def func() = ... func2() ...
  def func2() = ... func3() ...
  def func3() = ... func4() ...
  def func4() = ... func5() ...
  def func5() = ... func6() ...
  def func6() = ... func7() ...
  def func7() = ... func8() ...
  def func8() = ... func9() ...
  def func9() = ... sayHello("Moo") ...

  def sayHello(msg: String) = log("Hello World: " + msg)
}

new Container(System.out.println).func()
new Container(System.err.println).func()
```

같은 로거를 서로 다른 `class`나 `object`에 주입해야 할 때도 같은 기법이 통한다. 그 `class`나 `object`들을 (이 경우) `Container` 클래스 안에 중첩시키면, 모든 메서드 시그니처에 `log`를 추가하지 않고도 전부 접근할 수 있다.

### 추상 멤버

생성자 주입을 쓰려는데 `Container` 클래스가 너무 커지고 있다면, 개별 `trait`로 나눠 별도 파일에 두어라.

```scala
// Foo.scala
trait Foo{
  def log: String => Unit
  def func() = ... func2(log) ...
  def func2() = ... func3(log) ...
  def func3() = ... func4(log) ...
}

// Bar.scala
trait Bar extends Foo{
  def log: String => Unit
  def func4() = ... func5(log) ...
  def func5() = ... func6(log) ...
  def func6() = ... func7(log) ...
}

// Baz.scala
trait Baz extends Bar{
  def log: String => Unit
  def func7() = ... func8(log) ...
  def func9() = ... sayHello("Moo", log) ...
  def sayHello(msg: String) = log("Hello World: " + msg)
}

// Main.scala
class Container(val log: String => Unit) extends Foo with Bar with Baz
new Container(System.out.println).func()
new Container(System.err.println).func()
```

이렇게 하면 각 `trait`에 `log`를 전달해야 한다. `trait`는 생성자 파라미터를 받을 수 없지만, 추상 메서드 `log`를 주고 메인 `Container` 클래스가 이 `trait`들을 전부 확장하면서 `log`를 구체적으로 구현하게 하면 된다.

### implicit 메서드 파라미터

같은 로거를 사방에 넘기고 있고

```scala
def sayHello(msg: String, log: String => Unit) = log("Hello World: " + msg)

sayHello("Mooo", System.out.println)
sayHello("Mooo2", System.out.println)
sayHello("Mooo3", System.out.println)
sayHello("Mooo4", System.out.println)
sayHello("Mooo5", System.out.println)
sayHello("Mooo6", System.out.println)
sayHello("Mooo7", System.out.println)
sayHello("Mooo8", System.out.println)
sayHello("Mooo9", System.out.println)
```

*게다가* 사용처가 한 개(또는 소수)의 클래스에 국한되지 않고 "사방에 흩어져" 있다면, implicit 파라미터로 만들어라.

```scala
def sayHello(msg: String)(implicit log: String => Unit) = log("Hello World: " + msg)

implicit val logger: String => Unit = System.out.println
sayHello("Mooo")
sayHello("Mooo2")
sayHello("Mooo3")
sayHello("Mooo4")
sayHello("Mooo5")
sayHello("Mooo6")
sayHello("Mooo7")
sayHello("Mooo8")
sayHello("Mooo9")
```

일반적으로 이것은 *정말 많이* — 최소한 수십 개 호출 지점에 — 넘기고 있을 때만 해야 한다. 그래도 큰 프로그램에서는, 특히 로깅처럼 흔한 것에는 그리 무리한 일이 아니다.

하지만 사용처가 모두 비교적 한곳에 몰려 있다면, 코드의 작은 한 구역만을 위해 새 implicit 파라미터를 만들기보다 생성자 주입을 선호해야 한다. implicit 파라미터는 호출 지점이 흩어져 있어서 그 모든 클래스에 생성자 주입을 하기가 지겨워지는 경우를 위해 아껴 두어라.

### "동적 변수", 즉 전역 가변 상태

흔한 패턴으로, 코드를 실행하기 전에 어떤 전역/스레드 로컬 변수를 설정해 두고 코드가 그것을 전역으로 읽는 방식이다. Lift 같은 Scala 프레임워크부터 Flask나 Django 같은 Python 프레임워크까지, 세상 거의 모든 웹 프레임워크에서 쓰인다. 이렇게 생겼다.

```scala
var log: String => Unit = null
def func() = ... func2(log) ...
def func2() = ... func3(log) ...
def func3() = ... func4(log) ...
def func4() = ... func5(log) ...
def func5() = ... func6(log) ...
def func6() = ... func7(log) ...
def func7() = ... func8(log) ...
def func8() = ... func9(log) ...
def func9() = ... sayHello("Moo", log) ...

def sayHello(msg: String) = log("Hello World: " + msg)

log = System.out.println
func() // stdout에 로깅
log = System.err.println
func() // stderr에 로깅
```

편리한 점이 많다. 모든 것을 `class`로 감쌀 필요도 없고, 모든 메서드 시그니처에 `implicit`을 추가하거나 `log` 함수를 손수 돌릴 필요도 없다.

반면 가장 위험하기도 하다. 코드의 어느 부분에서 `log` 함수를 "사용할 수 있고" 어느 부분에서 없는지가 더 이상 분명하지 않다. `log`를 쓰는 메서드를 초기화되기 전에 "너무 일찍" 호출하는 실수를 저지르기 쉽고, 그런 코드는 컴파일은 멀쩡히 되지만 런타임에 터진다.

```scala
var log: String => Unit = null
def func() = ... func2(log) ...
def func2() = ... func3(log) ...
def func3() = ... func4(log) ...
def func4() = ... func5(log) ...
def func5() = ... func6(log) ...
def func6() = ... func7(log) ...
def func7() = ... func8(log) ...
def func8() = ... func9(log) ...
def func9() = ... sayHello("Moo", log) ...

def sayHello(msg: String) = log("Hello World: " + msg)

func() // 콰쾅
log = System.out.println
func() // stdout에 로깅
log = System.err.println
func() // stderr에 로깅
```

이 문제는 앞서 제안한 의존성 주입 기법들에는 존재하지 않는다. 하드코딩, (명시적이든 implicit이든) 메서드 파라미터, 생성자 주입, 추상 멤버 주입 — 이 모든 경우에 컴파일러는 어떤 메서드에서 `log` 함수를 사용할 수 있고 어떤 메서드에서 없는지 정적으로 알고, 사용할 수 없는 곳에서 `log`를 쓰려고 하면 친절하게 컴파일 에러를 내 준다. 동적 변수에서는 그런 보장을 전혀 받지 못한다.

따라서 이 방식은 가장 큰 힘을 주는 동시에 가장 적은 안전을 주므로, 가능하면 피해야 한다.

### 세터 주입

"세터 주입"이란 객체를 인스턴스화한 뒤, 그 객체의 메서드를 호출할 때 사용될 어떤 변수를 나중에 설정해 주는 것을 말한다.

불변성과 가변성 섹션에서 설명했듯이, 하지 마라. 가변 상태는 변하는 것을 모델링할 때만 써야 하며, 함수 호출에 파라미터를 전달하는 임시방편으로 쓰면 안 된다.
