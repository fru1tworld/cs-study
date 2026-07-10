> 원본: https://rockthejvm.com/articles/scala-3-new-types

# Scala 3: 새로운 타입

## 이 글의 대상 독자

모든 수준의 Scala 개발자를 대상으로 한다. 후반부로 갈수록 복잡도가 올라간다.

## 소개

Scala 3에는 스타일 변경, 문법 변화, 폐기된 기능 등 다양한 신기능이 도입됐다.

이 글에서는 그중에서도 **Scala 3에서 새롭게 사용할 수 있게 된 타입**을 집중적으로 살펴본다. 보다 깊은 내용은 Scala 3 New Features 강좌에서 다루고 있다.

## 리터럴 타입

기술적으로는 Scala 2.13에서 처음 등장했지만, 여기서 함께 살펴본다. Scala는 이제 리터럴 값을 개별 타입 싱글턴으로 취급하며, 명시적으로 선언할 수 있다:

```scala
val aNumber = 3       // Int로 추론
val three: 3 = 3      // 리터럴 타입 3
```

타입을 명시하지 않으면 컴파일러가 일반적인 방식으로 타입을 추론한다. 리터럴 타입을 명시하면 컴파일러가 새로운 타입을 자동으로 만든다. 리터럴 타입 `3`은 `Int`의 서브타입이 된다:

```scala
def passNumber(n: Int) = println(n)
passNumber(aNumber)
passNumber(three)     // 정상 동작
```

그러나 리터럴 타입을 매개변수로 받는 메서드는 해당 값만 허용한다:

```scala
def passStrict(n: 3) = println(n)
passStrict(3)
// passStrict(aNumber) // 컴파일 에러
```

리터럴 타입은 숫자형(Double 등), Boolean, String에 모두 적용된다. Scala에서 String 리터럴은 자동으로 인터닝된다:

```scala
val myFavoriteLanguage: "Scala" = "Scala"
val pi: 3.14 = 3.14
val truth: true = true
```

리터럴 타입은 컴파일 타임 검증을 강제해서 부주의한 코드를 방지한다:

```scala
def doSomethingWithYourLife(meaning: Option[42]): Unit =
    meaning.foreach(m => s"I've made it: $m")
```

이 메서드는 `Some(42)` 또는 `None`만 받을 수 있다. 값이 중요한 경우 리터럴 타입으로 선언을 제한하면 잘못된 인자를 컴파일 시점에 잡아낼 수 있다.

## 유니온 타입

유니온 타입은 Scala 3에서 완전히 새로 추가된 기능이다. "A 또는 B" 형태로 타입을 결합할 수 있으며, 모나딕 타입인 `Either`와는 다른 개념이다:

```scala
def ambivalentMethod(arg: String | Int) = arg match {
    case _: String => println(s"a String: $arg")
    case _: Int    => println(s"an int: $arg")
}
```

이 메서드는 `Int`와 `String` 인자를 모두 받을 수 있다. 이전 Scala 버전에서는 불가능했다:

```scala
ambivalentMethod(33)
ambivalentMethod("33")
```

유니온 타입으로 선언된 값에서 특정 타입의 메서드를 사용하려면 패턴 매칭이 필요하다.

리터럴 타입과 결합하면 TypeScript와 비슷한 기능을 구현할 수 있다:

```scala
type ErrorOr[T] = T | "error"
def handleResource(file: ErrorOr[File]): Unit = {
    // 구현 코드
}
```

이 메서드 안에서는 에러 케이스를 먼저 처리하지 않으면 File API에 접근할 수 없다. 부주의한 코드를 방지하고 컴파일 타임에 에러 처리를 강제하는 셈이다.

컴파일러는 여러 타입을 반환하는 표현식에서 유니온 타입을 자동으로 추론하지 않는다. 대신 상속 관계상 가장 가까운 공통 조상 타입을 계산한다. 유니온 타입을 사용하려면 명시적으로 선언해야 한다:

```scala
val stringOrInt = if (43 > 0) "a string" else 43                   // Any로 추론
val aStringOrInt: String | Int = if (43 > 0) "a string" else 43    // OK
```

## 인터섹션 타입

대칭적으로, Scala 3에는 인터섹션 타입도 도입됐다. 유니온 타입이 "A 또는 B"라면, 인터섹션 타입은 "A이면서 동시에 B"를 뜻한다:

```scala
trait Camera {
    def takePhoto(): Unit = println("snap")
}
trait Phone {
    def makeCall(): Unit = println("ring")
}

def useSmartDevice(sp: Camera & Phone): Unit = {
    sp.takePhoto()
    sp.makeCall()
}
```

`useSmartDevice` 안에서 컴파일러는 인자가 Camera와 Phone API를 모두 충족한다고 보장한다. 인터섹션 타입은 두 trait을 모두 믹스인한 타입만 허용한다:

```scala
class Smartphone extends Camera with Phone

useSmartDevice(new Smartphone) // 정상 동작
```

인터섹션 타입은 구성 타입 모두의 서브타입이 된다.

두 타입이 같은 메서드 정의를 공유해도 컴파일러는 문제를 제기하지 않는다. 실제로 전달되는 타입이 충돌을 해소해야 하며, 구현은 하나만 존재하므로 호출 지점에서 충돌이 생기지 않는다. Scala는 이를 trait 선형화(linearization)로 처리하는데, 별도 문서에서 다루고 있다.

같은 메서드 시그니처를 공유하되 반환 타입이 다른 경우를 생각해 보자:

```scala
trait HostConfig
trait HostController {
    def get: Option[HostConfig]
}

trait PortConfig
trait PortController {
    def get: Option[PortConfig]
}
```

웹 서버에서 둘 다 믹스인하면:

```scala
def getConfigs(controller: HostController & PortController) = controller.get
```

**첫 번째 질문:** 이 코드가 컴파일될까? 그렇다. `get` 메서드가 양쪽에 공통으로 존재하기 때문이다.

**두 번째 질문:** 반환 타입은 뭘까? 두 컨트롤러를 모두 확장하는 실제 타입은 `get`이 `Option[HostConfig]`과 `Option[PortConfig]`를 동시에 반환해야 한다. 답은 `get`이 `Option[HostConfig] & Option[PortConfig]`를 반환한다는 것이다. 컴파일러가 이를 자동으로 알아낸다:

```scala
def getConfigs(controller: HostController & PortController)
    : Option[HostConfig] & Option[PortConfig]
    = controller.get
```

**세 번째 질문:** 인터섹션 타입이 공변성(variance)과 함께 동작할까? 그렇다. `Option`은 공변적이므로, Option 간의 서브타이핑 관계는 제네릭 타입 간의 서브타이핑 관계와 일치한다. 따라서 `Option[A] & Option[B]`는 `Option[A & B]`와 같다:

```scala
def getConfigs(controller: HostController & PortController)
    : Option[HostConfig & PortConfig]
    = controller.get
```

이 코드도 정상적으로 컴파일된다.

## 결론

이 글에서는 Scala 3의 두 가지 새로운 타입인 인터섹션 타입과 유니온 타입을 살펴봤다. Scala 2.13의 리터럴 타입과 결합하면, 훨씬 강력하고 표현력 있는 API를 설계할 수 있을 것이다.
