# 전략적 Scala 스타일: 실용적 타입 안전성

> 원문: [Strategic Scala Style: Practical Type Safety](https://www.lihaoyi.com/post/StrategicScalaStylePracticalTypeSafety.html)
> 저자: Li Haoyi | 번역일: 2026-07-10

이 글은 Scala 프로그래밍 언어의 타입 안전성을 활용해, Scala 프로그램을 작성할 때 저지르는 실수를 잡아내는 방법을 탐구한다.

Scala에는 에러를 잡아 주는 컴파일러가 있고 많은 사람이 이 언어를 "타입 안전"하다고 부르지만, 사실 Scala를 작성하는 방식에는 안전성을 더 많이 또는 더 적게 제공하는 온갖 스펙트럼이 존재한다. 여기서는 코드를 스펙트럼의 "더 안전한" 쪽으로 옮기는 데 쓸 수 있는 다양한 기법을 논의한다. 절대적 증명과 논리로 무장한 이론적인 측면은 의도적으로 무시하고, Scala 컴파일러가 여러분의 어리석은 버그를 더 많이 잡아내게 만드는 실용적인 측면에 집중한다.

이 글은 전략적 Scala 스타일 시리즈의 세 번째 글이다([Principle of Least Power](http://www.lihaoyi.com/post/StrategicScalaStylePrincipleofLeastPower.html)와 [Conciseness & Names](http://www.lihaoyi.com/post/StrategicScalaStyleConcisenessNames.html)에 이어).

- 기초
  - 타입이란 무엇인가?
  - 안전성이란 무엇인가?
  - 타입 안전성이란 무엇인가?
- Scalazzi Scala
  - null을 피하라
  - 예외를 피하라
  - 부수 효과를 피하라
  - Scalazzi Scala의 한계
- 구조화된 데이터
  - 문자열 대신 구조화된 데이터를 사용하라
  - 불변 조건을 타입에 인코딩하라
- 스스로를 설명하는 데이터
  - 정수 열거형을 피하라
  - 문자열 플래그를 피하라
  - 정수 ID를 박싱하라
- 결론

*타입 안전성*이라는 용어에는 여러 측면이 있다. Haskell이나 Scala 프로그래밍 언어 타입 시스템의 이론적 토대를 연구하며 경력 전체를 보낼 수도 있고, Haskell 런타임이나 Java 가상 머신 내부의 타입 구현을 연구하며 *또 다른* 경력 전체를 보낼 수도 있다. 우리는 그 두 분야 모두 무시한다.

대신 이 글은 Scala 언어를 "타입 안전"하게 사용하는 실용적인 측면을 탐구한다. 컴파일러가 *타입*을 알고 있다는 사실 덕분에 실수의 결과를 완화하고, 실수를 개발 중에 바로 고칠 수 있는 컴파일 에러로 바꿔서 코드 개발에 *안전성*을 제공하는 방법 말이다. 숙련된 Scala 프로그래머라면 이 글의 모든 내용이 "기초적"이거나 "당연"하게 느껴질 것이다. 그러나 경험이 적은 사람이라면 Scala로 문제를 풀 때 쓰는 도구 상자에 추가할 유용한 기법들을 발견할 수 있기를 바란다.

여기서 설명하는 각 기법에는 트레이드오프가 있다. 늘어나는 장황함, 복잡성, 추가 클래스 파일, 나빠지는 런타임 성능 등이다. 이 글에서는 그런 트레이드오프를 무시할 텐데, 대개는 꽤 자명하기 때문이다. 이 글은 가능성을 열거만 할 뿐, 그 트레이드오프가 가치 있는지에 대한 깊은 논의로는 들어가지 않는다. 또한 이 글은 *바닐라 Scala*에만 해당하며 [Scalaz](https://github.com/scalaz/scalaz), [Cats](https://github.com/typelevel/cats), [Shapeless](https://github.com/milessabin/shapeless) 같은 방대한 서드파티 라이브러리는 다루지 않는다. 그 라이브러리들에는 고유한 관용구와 기법이 있어서, 전문성 있는 누군가가 써 준다면 별도의 글로 다룰 가치가 있다!

이 시리즈의 [다른](http://www.lihaoyi.com/post/StrategicScalaStylePrincipleofLeastPower.html) [두](http://www.lihaoyi.com/post/StrategicScalaStyleConcisenessNames.html) 글처럼, 어떤 가이드라인도 지나치게 논쟁적이지 않기를 바라며, Scala 언어에 익숙한 사람들이 "상식"으로 여길 만한 것들을 문서화하는 것이 목표다. 이 글이 사람들이 공유하는 공통 원칙과 알아 둘 기본 기법의 토대를 놓아서, Scala 프로그램을 설계할 때 여러분의 프로젝트별 가이드라인, 선호, 판단과 결합해 쓸 수 있기를 바란다.

## 기초

타입 안전성을 둘러싼 구체적인 기법과 트레이드오프를 논의하기 전에, 잠시 물러서서 이게 다 무슨 얘기인지 물어보는 것이 좋겠다. 타입이란 무엇인가? "안전"하다는 것은 무슨 뜻인가?

### 타입이란 무엇인가?

**타입이란 프로그램의 어떤 값에 대해 컴파일 타임에 알고 있는 것이다.**

기본적으로 모든 프로그래밍 언어의 타입 시스템은 서로 다르다. 어떤 언어에는 제네릭이 있고, 어떤 언어에는 실체화된(reified) 제네릭이 있다. Java처럼 실체화된 타입을 가진 언어에서는 값의 "타입"이 항상 클래스나 인터페이스에 대응하고 런타임에 검사할 수 있다. C처럼 그렇지 않은 언어도 있다. Python 같은 동적 언어에는 정적 타입이 없어서 타입이 런타임에*만* 존재한다.

이 글의 주제인 Scala 언어에는 상대적으로 복잡하고 임의적인 고유 타입 시스템이 있다. 이를 형식화하려는 시도도 있는데, 예를 들어 [Dotty 프로젝트](https://github.com/lampepfl/dotty)가 그렇다. 이 글은 그 모든 것을 무시한다.

이 글에서는 위의 정의를 사용한다. *타입*이란 프로그램의 어떤 값에 대해 컴파일 타임에 알고 있는 것이다. 예를 들면

- `Int`인 것은 확실히 `-2147483648`부터 `2147483647`까지의 32비트 정수를 담고 있다
- `Option[T]`인 것은 확실히 `Some[T]` 아니면 `None`을 담고 있다
- `CharSequence`인 것은 확실히 `Char`들을 담고 있고 `.length`, `.chatAt`, `.subsequence` 메서드를 호출하게 해 주지만, 그것이 `String`인지 `StringBuffer`인지 다른 무엇인지는 모른다. 가변인지 불변인지, 내용을 어떻게 저장하는지, 성능 특성이 어떤지도 모른다
- `String`인 것 역시 문자들을 갖지만, 불변이고, 내용을 내부 문자 배열에 저장하며, 인덱스로 `Char`를 O(1)에 조회할 수 있다는 것을 안다

값의 타입은 무언가가 무엇일 수 있는지와 무엇일 수 없는지를 모두 알려 준다. `Option[String]`은 `Some`이거나 `None`일 수 있지만, 32비트 정수*일 수는 없다*! Scala에서 이는 코드에서 검사해야 할 것이 아니라, 컴파일 중에 컴파일러가 대신 확인해 주므로 옳다고 믿고 의지할 수 있는 것이다.

이 지식이 바로 값의 "타입"을 구성한다.

### 타입이 아닌 것은?

#### 클래스

이 정의에 따르면 타입은 클래스가 아니다. 물론 Java에서, 그리고 JVM 위의 Scala에서 모든 타입은 클래스(또는 인터페이스)로 표현된다. 하지만 이는 예를 들어 [Scala.js](http://www.scala-js.org/)에서는 성립하지 않는다. 거기서는 컴파일이 끝난 뒤 조사할 잔재가 전혀 남지 않는 합성 타입(`js.Any`를 확장하는 `trait`)을 정의할 수 있고, 다른 프로그래밍 언어들에서도 마찬가지다.

타입이 클래스로 뒷받침된다는 것은 사실이지만, 이 글과는 대체로 무관한 구현 세부 사항이다.

#### Scala 타입 시스템

여기서 논의하는 "타입" 개념은 모호하고 폭넓으며, Scala만이 아니라 더 많은 언어에 적용된다. [Scala 고유의 타입 시스템](http://ktoso.github.io/scala-types-of-types/)은 복잡하다. 클래스, 추상 타입, refinement 타입, 트레이트, 타입 바운드, 컨텍스트 바운드, 그리고 그보다 더 난해한 것들까지 있다.

이 글의 관점에서 이것들은 모두 같은 목적에 봉사하는 세부 사항이다. 프로그램의 값들에 대해 여러분이 아는 것을 컴파일러에게 설명하고, 여러분이 하는 일이 하겠다고 말한 것과 일치하는지 컴파일러가 검사하게 해 주는 것 말이다.

### 안전성이란 무엇인가?

**안전성이란 실수를 저질렀을 때 그 결과가 사소하다는 뜻이다.**

안전성의 정의는 아마 타입의 정의보다도 많을 것이다. 위의 정의는 타입보다 폭넓다. 보안 관행, 격리, 분산 시스템의 견고성과 회복탄력성 등 많은 것에 적용된다.

사람들은 온갖 실수를 저지른다. 코드의 오타, 부실한 부하 추정, 잘못된 명령어 복사·붙여넣기. 실수를 저지르면 무슨 일이 일어나는가?

- 에디터에서 빨간 밑줄을 보고 5초 만에 고친다
- 10초 걸리는 전체 컴파일을 기다린 뒤 고친다
- 10초 걸리는 테스트 스위트를 돌린 뒤 고친다
- 실수를 배포하고, 몇 시간 뒤 버그를 발견해 고치고, 수정본을 배포한다
- 실수를 배포하고, 버그가 몇 주 동안 눈에 띄지 않으며, 발견해서 고친 뒤에도 그것이 남긴 손상된 데이터의 잔해를 치우는 데 몇 주가 걸린다
- 실수를 배포하고, [45분 뒤 회사가 완전히 파산한 것을 발견한다](http://pythonsweetness.tumblr.com/post/64740079543/how-to-lose-172222-a-second-for-45-minutes). 여러분의 직장, 팀, 조직과 계획, 전부 사라진다.

"타입"이나 "컴파일 타임" 개념을 제쳐 두더라도, 환경마다 안전성의 수준이 다르다는 것은 자명하다. 런타임 에러조차 일찍 잡히고 디버깅이 쉬우면 영향이 작을 수 있다. 무언가가 맞지 않을 때 런타임에 `TypeError`를 던지는 Python의 습관이, 맞지 않을 때 값을 강제 변환하는 PHP의 습관(문제를 가려 버려 데이터 손상과 추적하기 어려운 난해한 버그로 이어지기 쉽다)보다 훨씬 "안전"한 이유다.

### 타입 안전성이란 무엇인가?

**타입 안전성이란 값에 대해 컴파일 타임에 아는 것을 활용해 대부분의 실수가 낳는 결과를 최소화하는 것이다.**

예를 들어 "사소한 결과"란 개발 중에 이해하기 쉬운 컴파일 에러를 보고 30초 만에 고치는 것일 수 있다.

이 정의는 위의 "타입"과 "안전성" 정의에서 곧바로 따라 나온다. 대부분의 타입 안전성 정의보다 상당히 폭넓다. 특히

- 타입 안전성은 Haskell을 작성하는 것이 *아니다*. 개념이 그보다 훨씬 넓다.

- 타입 안전성은 가변 상태를 피하는 것이 *아니다*. 위에서 말한 목표에 기여하는 경우가 아니라면.

- 타입 안전성은 도달해야 할 절대치가 *아니라*, 최적화하려고 노력할 속성이다.

- 타입 안전성은 모두에게 같지 *않다*. 사람마다 저지르는 실수의 종류가 다르고 그 실수의 해악 수준이 다르다면, 실수의 전체 해악을 최소화하기 위해 서로 다른 것을 최적화해야 한다.

- 에러 메시지가 난해하고 해결이 어렵다면 컴파일 에러조차 심각한 결과를 낳을 수 있다! 10초 만에 고칠 수 있는 친절한 컴파일 에러와, 고치기 전에 이해하는 데만 30분이 걸릴 수도 있는 [거대한 컴파일 에러](https://www.lihaoyi.com/post/TypeSafety/ParboiledError.txt)는 다르다.

타입 안전성의 정의는 매우 다양하다. C++로 일하는 개발자, Python을 쓰는 웹 개발자, 프로그래밍 언어를 연구하는 교수에게 물으면 저마다 다른 정의를 내놓을 것이다. 이 글에서는 위의 임의적이고 폭넓은 정의를 사용한다.

**타입 안전성이란 값에 대해 컴파일 타임에 아는 것을 활용해 대부분의 실수가 낳는 결과를 최소화하는 것이다.**

## Scalazzi Scala

"타입 안전"하게 Scala를 작성하는 방법에 대해 많은 사람이 많이 고민해 왔다. 이른바 Scala 언어의 "[Scalazzi 부분집합](http://yowconference.com.au/slides/yowwest2014/Morris-ParametricityTypesDocumentationCodeReadability.pdf)"이 그런 철학 중 하나다.

![Scala의 Scalazzi 안전 부분집합](https://www.lihaoyi.com/post/TypeSafety/ScalazziScala.png)

이 가이드라인에 대해 논의할 것이 많지만, 내가 가장 흥미롭다고 생각하는 몇 가지만 살펴보겠다.

- null을 피하라
- 예외를 피하라
- 부수 효과를 피하라

### null을 피하라

누락됐거나, 초기화되지 않았거나, 사용할 수 없는 데이터를 `null`로 표현하고 싶은 유혹은 아주 크다. 예를 들어 초기화되지 않은 값으로

```scala
class Foo{
  val name: String = null // 원한다면 유용한 것으로 오버라이드하라
}
```

또는 함수에 넘기는 "값 없음" 인자로

```scala
def listFiles(path: String) = ...

listFiles("/usr/local/bin/")
listFiles(null) // 인자 없음, 현재 작업 디렉터리를 기본값으로
```

Scalazzi Scala는 이렇게 하지 말라고 말하며, 거기에는 그럴 만한 이유가 있다.

- `null`은 프로그램의 *어디에나* — 어떤 변수나 값에든 — 나타날 수 있고, 어떤 변수가 `null`이고 아닌지 통제할 방법이 없다

- `null`은 프로그램 전체로 *전파*된다. `null`을 함수에 넘기고, 다른 변수에 대입하고, 컬렉션에 저장할 수 있다.

이 둘이 합쳐지면, `null` 값은 초기화된 곳에서 멀리 떨어진 곳에서 에러를 일으키는 경향이 있고 추적하기 어렵다. 무언가가 `NullPointerException`으로 터지면 먼저 말썽인 변수를 찾아야 하고(한 줄의 코드에 여러 변수가 쓰이고 있을 수 있다!), 그다음 그 변수가 함수를 드나들고 컬렉션에 저장됐다 꺼내지는 경로를 따라가며, `null`이 애초에 어디서 왔는지 알아낼 때까지 추적해야 한다.

Python 같은 동적 언어에서는 이런 우발적인 잘못된 값 전파가 흔하고, 잘못된 값이 어디서 왔는지 알아내려고 사방에 `print` 문을 추가하며 프로그램을 몇 시간씩 추적하는 일이 드물지 않다. 대개는 누군가 함수 인자를 뒤바꿔서 `user_email` 대신 `user_id`를 넘겼다든가 하는 똑같이 사소한 문제인데도, 추적해서 고치는 데는 상당한 노력이 든다.

Scala처럼 타입 체커가 있는 컴파일 언어에서는 그런 실수 다수가 코드를 실행하기도 전에 컴파일러에게 잡힌다. `String`이 기대되는 곳에 `Int`나 `Seq[Double]`을 넘기면 타입 에러가 난다. 모든 것이 잡히지는 않지만, 더 지독한 실수들 다수는 잡힌다. `null`이 아니어야 할 곳에 `null`을 넘기는 것만 빼고.

`null`의 대안 몇 가지를 보자.

**있을 수도 없을 수도 있는 값을 표현하려는 경우 — 함수 인자든 오버라이드할 클래스 속성이든**:

```scala
class Foo{
  val name: String = null // 원한다면 유용한 것으로 오버라이드하라
}
```

대신 `Option[T]` 사용을 고려하라.

```scala
class Foo{
  val name: Option[String] = None // 원한다면 유용한 것으로 오버라이드하라
}
```

`"foo"`와 `null` 대신 `Some("foo")`와 `None`을 쓰는 것은 비슷해 보이지만, 그렇게 하면 모두가 그것이 `None`일 수 있음을 알게 되고, `null`과 달리 `String`이 기대되는 곳에 `Some[String]`을 넣으려 하면 컴파일 에러가 난다.

**초기화되지 않은 변수의 자리 표시자로 `var`에 `null`을 쓰고 있는 경우**

```scala
def findHaoyiEmail(items: Seq[(String, String)]) = {
  var email: String = null // 원한다면 유용한 것으로 오버라이드하라

  for((name, value) <- items){
    if (name == "Haoyi") email = value
  }

  if (email == null) email = "Not Found"
  doSomething(email)
}
```

`val`로 바꿔 선언과 초기화를 한 번에 할 수 없을지 고민해 보라.

```scala
def findHaoyiEmail(items: Seq[(String, String)]) = {
  val email =
    items.collect{case (name, value) if name == "Haoyi" => value}
         .headOption
         .getOrElse("Not Found")
  doSomething(email)
}
```

`email` 값을 표현식 하나로 초기화할 수 없더라도, Scala에서는 `val`을 선언할 때 중괄호 `{...}` 안에 임의의 코드를 넣을 수 있다. 그러니 `var`를 "나중에" 초기화하려고 쓸 예정이던 코드는 대부분 `{...}` 안에 넣어서 `val`의 선언과 초기화를 한 번에 할 수 있을 것이다.

```scala
def findHaoyiEmail(items: Seq[(String, String)]) = {
  val email = {
    ...
  }
  doSomething(email)
}
```

그렇게 함으로써 `email`이 `null`이 되는 일을 아예 막을 수 있다.

프로그램에서 `null`을 피하는 것만으로 *이론적* 상황이 바뀌지는 않는다. 이론상으로는 여전히 누군가 `null`을 넘길 수 있고, 그러면 디버깅하기 어려운 문제를 추적하는 똑같은 처지에 놓인다. 그러나 *실질적* 환경은 바뀐다. 디버깅하기 어려운 `NullPointerException`을 추적하는 데 쓰는 시간이 훨씬 줄어든다. 여전히 이따금 그런 일이 생기지만(초기화 순서 때문에 나타나는 `null`이나 `null`을 쓰는 서드파티 라이브러리 때문에), *전체적으로* 실수의 대가는 낮아지는 경향이 있다. 더 많은 버그가 추적 비용이 비싼 런타임 예외가 아니라, 개발 중에 바로 고치면 되는 컴파일 에러가 된다.

### 예외를 피하라

예외는 기본적으로 코드 조각의 추가 반환값이다. 여러분이 작성하는 어떤 코드든 `return` 키워드나 블록의 마지막 표현식으로 "정상적인" 방식으로 반환할 수도 있고, 예외를 던질 수도 있다. 그리고 그 예외는 임의의 데이터를 담을 수 있다.

Java 같은 다른 언어들은 던져질 수 있는 예외를 반드시 잡도록 컴파일러가 검사하게 하려 했지만, 그들의 "검사 예외(checked exception)" 기능은 대체로 성공하지 못했다. 어떤 검사 예외를 던지는지 선언해야 하는 불편함 때문에 사람들은 흔히 메서드에 뭉뚱그려 `throws Exception`을 선언하거나, 검사 예외를 잡아서 비검사 런타임 예외로 다시 던진다. C#이나 Scala 같은 이후의 언어들은 검사 예외라는 아이디어를 완전히 버렸다.

왜 예외를 쓰지 말아야 할까?

- 코드 조각이 던질 수 있는 온갖 종류의 예외를 정적으로 알 방법이 없다. 따라서 그 코드의 가능한 "반환" 경우를 전부 처리했는지 알 수 없다

- 어떤 예외를 던지는지에 대한 표기는 선택 사항이고, 개발과 리팩터링이 진행되면서 현실과 어이없을 만큼 쉽게 어긋난다.

- 예외는 전파되므로, 여러분이 쓰는 라이브러리가 모든 메서드에 던지는 예외를 표기하는 규율을 지켰더라도, 여러분 자신의 코드에서는 아마 대충 하게 되어 표기하지 않을 것이다

예외의 대체물로, 실패 모드가 하나뿐인 함수의 결과에는 `Option[T]`를 반환하고, 실패 모드가 둘 이상인 결과에는 `Either[T, V]`나 직접 만든 sealed trait 계층을 반환할 수 있다.

```scala
sealed trait Result
case class Success(value: String) extends Result
case class InvalidInput(value: String, msg: String) extends Result
case class SubprocessFailed(returncode: Int, stderr: String) extends Result
case object TimedOut extends Result

def doThing(cmd: String): Result = ???
```

sealed trait 계층을 쓰면 정확히 어떤 실패 모드가 존재하고 각 경우에 어떤 데이터가 있는지 사용자에게 쉽게 전달할 수 있으며, 사용자가 `doThing`의 결과를 `match`할 때 처리하지 않은 경우가 있으면 컴파일러가 경고해 준다.

그래도 일반적으로 *모든* 예외를 없앨 수는 없다.

- 사소하지 않은 프로그램에는 실패 모드가 워낙 다양해서 그것을 빠짐없이 나열하는 것은 비현실적이다

- 다수는 충분히 드물어서, 정확한 원인을 몰라도 뭉뚱그려 잡아 "뭔가 잘못됐다"는 일반적인 동작 — 로깅이나 보고, 로직 재시도 등 — 을 실제로 하고 싶어진다.

- 이런 드문 실패 모드에는 나중에 직접 살펴볼 수 있게 디버깅 정보와 함께 로깅하는 것이 할 수 있는 최선이며, 스택 트레이스를 생성하는 예외는 이를 기본으로 해 준다.

하지만 스택 트레이스가 있어도, 원치 않는 예외가 왜 계속 나타나는지 알아내는 것은 평범한 `T`가 필요한 곳에 `Option[T]`를 쓰려다 컴파일 에러를 보는 것보다 훨씬 많은 시간이 든다.

Scala 프로그래밍은 예외와 함께 사는 세계다.

- `NullPointerException`
- 잘못된 패턴 매칭에서 나오는 `MatchError`
- 온갖 파일시스템·네트워크 실패에서 나오는 `IOException`
- 0으로 나눌 때의 `ArithmeticException`
- 배열을 잘못 다뤘을 때의 `IndexOutOfBoundsException`
- 서드파티 코드를 잘못 썼을 때의 `IllegalArgumentException`

그 밖에도 많다! 그래도 예외가 어디에나 있다고 해서 여러분이 문제를 보태야 하는 것은 아니다. 여러분 자신의 코드에서는 예외 사용을 피해서 타입 안전성을 높이고, `Option[T]`나 `Either[T, V]`나 `sealed trait` 데이터 타입에서 실패 경우 처리를 잊었을 때 컴파일러가 실수를 잡아 줄 기회를 더 많이 주어라.

### 부수 효과를 피하라

적어도 Scala에서 부수 효과는 컴파일러가 추적하지 않는다. 겉보기에는 "타입"과 아무 관련이 없지만, 위에서 정의한 타입 안전성을 훼손한다고 주장할 수 있다.

예시로 이런 코드를 보자.

```scala
var result = 0

for (i <- 0 until 10){
  result += i
}

if (result > 10) result = result + 5

println(result) // 50
makeUseOfResult(result)
```

실제 코드베이스에서 벌어지는 일보다 단순하지만 요점은 보여 준다. `result`를 자리 표시자 값으로 초기화한 뒤, 부수 효과를 이용해 `result`를 수정해서, 아마도 우리가 중요하게 여기는 무언가를 하는 `makeUseOfResult` 함수에 넘길 준비를 한다.

잘못될 수 있는 것이 많다. 실수로 변경 코드 하나를 빠뜨릴 수 있다.

```scala
var result = 0

for (i <- 0 until 10){
  results += i
}

println(result) // 45
makeUseOfResult(result) // 잘못된 입력이 들어간다!
```

"뻔한" 실수처럼 보일 수 있지만, 코드가 10줄이 아니라 1000줄이면 리팩터링 도중에 저지르기 쉬운 실수다. 그러면 `makeUseOfResult`는 잘못된 입력을 받아 엉뚱한 일을 할 수 있다. 똑같이 흔한 또 다른 실패 모드도 있다.

```scala
var result = 0

foo()

for (i <- 0 until 10){
  results += i
}

if (result > 10) result = result + 5

println(result) // 50
makeUseOfResult(result)

...

def foo() = {
  ...
  makeUseOfResult(result)
  ...
}
```

여기서는 리팩터링 중에 초기화 코드를 실수로 망가뜨린 것이 아니라, `result` 변수의 초기화가 끝나기 전에 실수로 그것을 사용하고 있다! 이 예제의 `foo()` 헬퍼처럼 간접적으로 쓰고 있을 수도 있지만, 효과는 같다. 프로그램의 어느 시점에는 `result`가 *잘못된* 값을 가진 것으로 "보이고", 나중에는 다른 *올바른* 값을 가진 것으로 보인다.

이 경우 "해법"은 부수 효과를 제거하고 `result`의 서로 다른 "단계"에 서로 다른 이름을 주는 것이다.

```scala
val summed = (0 until 10).sum

val result = if (summed > 10) summed + 5 else summed

println(result) // 50
makeUseOfResult(result)
```

올바르게 작성했을 때는 동등하지만, 위에서 본 두 가지 우발적 실패 모드에는 면역이다. 실수로 계산 단계 하나를 빠뜨리면 컴파일 에러가 난다.

```scala
val summed = (0 until 10).sum

println(result) // 컴파일 실패: not found: value result
makeUseOfResult(result)
```

마찬가지로 준비되기 전에 `result`를 쓰면, *간접적으로라도*, 컴파일 에러가 난다.

```scala
foo()

// 컴파일 실패:
// forward reference extends over definition of value summed
val summed = (0 until 10).sum

val result = if (summed > 10) summed + 5 else summed

println(result) // 50
makeUseOfResult(result)

def foo() = {

  makeUseOfResult(result)

}
def makeUseOfResult(i: Int) = ()
```

값의 타입이 기대되는 타입과 어긋난다는 의미의 "타입 에러"로 여겨지지 않을 수는 있지만, 이것*은* 컴파일러가 코드에 대해 아는 지식을 이용해 우리의 실수를 잡아내고, 일찍 고치기 쉽게 만들어 영향을 최소화하는 경우다. 앞서 정의한 *타입 안전성*의 아주 넓은 우산 아래에 확실히 들어간다.

### Scalazzi Scala의 한계

위에서 정의한 Scalazzi Scala로는 완벽하게 유효하지만, 코드 리뷰에 나타나면 분명히 사람들을 화나게 할 코드가 있다.

```scala
def fibonacci(n: Double, count: Double = 0, chain: String = "1 1"): Int = {
  if (count >= n - 2) chain.takeWhile(_ != ' ').length
  else{
    val parts = chain.split(" ", 3)
    fibonacci(n, count+1, parts(0) + parts(1) + " " + chain)
  }
}
for(i <- 0 until 10) println(fibonacci(i))
1
1
1
2
3
5
8
13
21
34
```

이 코드는 정확하고, Scalazzi Scala 가이드라인을 글자 그대로 따른다.

- `null` 없음
- 예외 없음
- `isInstanceOf`나 `asInstanceOf` 없음
- 부수 효과 없음, 모든 값 불변
- `classOf`나 `getClass` 없음
- 리플렉션 없음

그런데도 대부분의 사람은 이것이 끔찍하고 안전하지 않은 코드라는 데 동의할 것이다. 왜일까? 답은 구조화된 데이터에 있다...

## 구조화된 데이터

모든 데이터의 "모양"이 같은 것은 아니다. `(이름, 전화번호)` 쌍을 담은 데이터가 있다면 저장 방법은 다양하다.

- `Array[Byte]`로: 결국 파일시스템이 저장하는 형식이 이것이다! 데이터를 디스크에 덤프하면 이렇게 된다.
- `String`으로: 데이터 파일을 텍스트 에디터로 열면 보이는 것이 이것이다.
- `Seq[(String, String)]`로
- `Set[(String, String)]`로
- `Map[String, String]`으로

전부 이름/전화번호 쌍을 담기에 유효한 데이터 구조다. 어떤 표현을 쓸지 어떻게 고를까?

### 문자열 대신 구조화된 데이터를 사용하라

`String`을 데이터 저장소로 취급해서, 이어 붙여 데이터를 저장하고 잘라내서 데이터를 추출하고 싶은 유혹이 자주 든다. 예를 들어 객체의 해시코드를 16진수 형태로 얻고 싶은데, 그것이 이미 `toString`의 일부임을 발견했다고 하자.

```scala
@ println((new Object).toString)
java.lang.Object@1ca40818
```

그래서 `@`로 자르면 원하는 것을 얻기에 충분하겠다고 판단할 수 있다.

```scala
@ val myHashCode = (new Object).toString.split("@")(1)
myHashCode: String = "36de8d79"
```

그리고 맞을 것이다! 하지만 이것은 "가끔" 동작할 뿐, 다른 많은 경우에는 실패한다. 예를 들어 객체에 커스텀 `toString`이 있을 때다.

```scala
@ case class Foo()
defined class Foo
@ val myHashCode = (new Foo).toString.split("@")(1)
java.lang.ArrayIndexOutOfBoundsException: 1
  cmd33$.<init>(Main.scala:349)
  cmd33$.<clinit>(Main.scala:-1)
```

해법은 원하는 데이터를 얻으려고 `toString` 문자열을 분해하는 대신, `toString`이 쓰는 것과 같은 기법으로 직접 계산하는 것이다. 즉 `.hashCode`를 쓰고 16진수로 변환한다.

```scala
@ Integer.toHexString((new Object).hashCode)
res36: String = "a58a099"
@ Integer.toHexString((new Foo).hashCode)
res37: String = "114a6"
```

앞의 예제를 다시 쓰자면, 사용자 이름과 전화번호를 쉼표로 구분한 값으로 담은 문자열을 받아서, 전화번호가 `415`로 시작하는 — 캘리포니아 출신임을 나타내는 — 사람을 모두 찾고 싶다고 하자.

```scala
val phoneBook = """
Haoyi,6172340123
Bob,4159239232
Charlie,4159239232
Alice,8239239232
"""
```

이런 코드를 떠올릴 수 있다.

```scala
val lines = phoneBook.split("\n")

val californians = lines.filter(_.contains(",415"))
// Bob,4159239232
// Charlie,4159239232
```

그리고 동작할 것이다. 텍스트 형식이 바뀌기 전까지는. 예를 들어 탭으로 구분한 값으로 바뀌면, 코드는 조용히 아무것도 반환하지 않기 시작한다.

```scala
val phoneBook = """
Haoyi   6172340123
Bob     4159239232
Charlie 4159239232
Alice   4159239232
"""

val lines = phoneBook.split("\n")

val californians = lines.filter(_.contains(",415"))
// <아무것도 없음>
```

이런 상황에서 더 나은 방법은, 답을 얻기 위해 데이터를 다루기 전에 먼저 `phoneBook`을 기대하는 구조화된 데이터 형식(여기서는 `Seq[(String, String)]`)으로 파싱하는 것이다.

```scala
val parsedPhoneBook = for(line <- phoneBook.split("\n")) yield {
  val Seq(name, phone) = line.split(",")
  (name, phone)
}
// Seq(
//   ("Haoyi", "6172340123"),
//   ("Bob", "4159239232"),
//   ("Charlie", "4159239232"),
//   ("Alice"," 8239239232")
// )

parsedPhoneBook.filter(_._2.startsWith("415"))
// Seq(
//   ("Bob", "4159239232"),
//   ("Charlie", "4159239232")
// )
```

이런 시나리오에서는 입력 데이터 형식이 예기치 않게 바뀌면 `parsedPhoneBook`을 계산할 때 코드가 요란하게 실패한다. 게다가 컴파일러는 그 지점을 지난 뒤에는 데이터의 모양이 잘못되어 코드가 오동작하는 일이 없음을 정적으로 검사해 준다! 따라서 데이터 형식 에러는 딱 한 곳에서만 일어난다고 확신할 수 있고, 그 이후로는 코드가 "안전"하다고 — 잘못된 입력 때문에 로직 한가운데서 아무 데서나 터지지 않는다고 — 자신할 수 있다.

여기서는 문자열 튜플만 다루고 있지만, 데이터 형식이 더 복잡해져서 더 다양한 타입의 필드가 더 많아질수록, 작업을 시작하기 전에 데이터를 구조화하고 기대하는 모양에 맞는지 확인하는 일은 더욱 중요해진다.

어떤 사람들에게는 당연해 보이겠지만, `toString`을 잘라서 원하는 것을 얻는 방식을 받아들일 만한 해법으로 여기는 사람도 많다. 예를 들어 셸 스크립팅이나 Perl 배경이 있다면 이런 문자열 자르기가 일하는 평범한 방식이다. 정규식과 그 밖의 고급 문자열 자르기 기법까지 동원하면 실제로 꽤 많은 것을 해낼 수 있다.

그래도 이런 방식은 Scala 프로그램에 설 자리가 없다. 데이터가 "문자열 안에 있다"고 가정하면, 그것은 참일 수도 아닐 수도 있는 것이고 컴파일러가 검증을 도와줄 수 없다. 반면 `.hashCode`나 `toHexString`처럼 객체의 메서드와 속성으로 데이터에 접근하고 변환하면, 무엇을 넘기든 그것들이 존재하고 동작하리라고 꽤 확신할 수 있다. 그리고 실수로 잘못 사용하면 컴파일러가 알려 줄 가능성이 높은 반면, 문자열 자르기에서 실수하면 런타임 에러를 얻는다. 이것이 Scala 코드의 타입 안전성을 높이는 한 가지 방법이다.

### 불변 조건을 타입에 인코딩하라

"문자열에 담아 두기" 사고방식에서 벗어나 값을 구조화된 데이터(메서드와 속성이 있는 객체, 컬렉션) 형태로 저장하고 접근하더라도, 여전히 표현 방식의 선택지는 넓다. 예를 들어 위 예제에서 우리는 사용자와 전화번호를 담은 전화번호부를 `Seq[(String, String)]`에 저장하기로 했다. 하지만 `Map[String, String]`이나 `Set[(String, String)]`을 골라도 이상하지 않다. 그러면 어느 것을 써야 할까?

알고 보면 성능 특성이 다르다는 점(`Set`은 튜플의 존재 여부를 빠르게 확인하는 `contains`가 있고, `Map`은 키의 존재 여부를 빠르게 확인하는 `contains`가 있다) 외에도, 이 데이터 구조들은 저마다 다른 것을 약속한다.

- `Seq[(String, String)]`: 중복 항목(이름, 전화번호, 또는 둘 다)이 있을 수 있고 항목의 순서가 의미 있다
- `Set[(String, String)]`: 이름 중복도 전화번호 중복도 가능하지만 (이름, 전화번호) 쌍 각각은 유일하다. 항목 순서는 의미 없다
- `Map[String, String]`: 전화번호는 중복될 수 있지만 이름은 중복될 수 없다. 항목 순서는 의미 없다.

코드가 어떤 데이터 구조에 저장하는지 보는 것만으로 컴파일 타임에 특정 속성들을 확신할 수 있다. `Map`이 있다면 같은 사람이 여러 전화번호를 갖는 경우를 걱정할 필요가 전혀 없음을 알지만, `Set`이나 `Seq`라면 그런 일이 일어날 수 있고 그 경우를 처리하는 코드를 작성해야 할 수도 있다.

비슷하게, 이름으로 사용자의 전화번호를 얻는 `lookup` 함수를 작성하고 있다면

```scala
def lookup(name: String): ???
```

무엇을 반환해야 할까? 반환할 수 있는 것은

- `String`: 이름과 전화번호가 1대1로 대응하고 조회하는 이름이 항상 존재한다고 확신할 때
- `Option[String]`: 이름과 전화번호가 1대1로 대응하지만 이름이 전화번호부에 없을 때도 있을 때

이 두 반환 타입은 위의 `Map[String, String]` 경우에 대응한다. 하지만 반환할 수 있는 것은 그게 다가 아니다. 이런 것도 반환할 수 있다.

- `Seq[String]`: 이름이 전화번호부에서 여러 전화번호에 (중복 포함해서) 대응하거나 존재하지 않을 수 있고, 순서가 의미 있을 때

- `Set[String]`: 이름이 전화번호부에서 중복 없이 여러 전화번호에 대응하거나 존재하지 않을 수 있고, 순서가 의미 없을 때

이들은 위의 `Seq[(String, String)]`와 `Set[(String, String)]` 경우에 대응한다. 그래도 반환 타입의 가능성은 아직 남아 있다.

- `(String, Seq[String])`: 이름이 여러 전화번호에 (중복 포함해서) 대응할 수 있지만 *최소 하나는 반드시 존재*하고, 순서가 의미 있을 때

- `(String, Set[String])`: 이름이 중복 없이 여러 전화번호에 대응할 수 있지만 *최소 하나는 반드시 존재*하고, 순서가 의미 없을 때

`(String, Seq[String])` 튜플을 반환하면, `lookup` 함수의 호출자에게 항상 *최소 하나*의 항목을 반환하며 절대 0개는 아니라고 보장하는 것이다. 이는 호출자에게 유용할 수 있다. 호출자는

```scala
val (first, rest) = lookup(name)
rest.foldLeft(first)(...)
```

를

```scala
val numbers = lookup(name)
numbers.reduce(...)
```

대신 쓸 수 있고, 빈 리스트가 반환되어 `java.lang.UnsupportedOperationException: empty.reduceLeft`가 던져지는 일이 없다고 100% 확신할 수 있다.

`(String, Seq[String])` 튜플을 반환하는 지경에 이르면 API가 살짝 불편해지기 시작하므로, 값이 *정확히* 무엇을 담을 수 있는지 엄격하게 정의하는 데까지 가지 않는 사람도 많다. 그래도 극단적인 안전성을 최적화하고 싶다면 가능한 기법이다.

Scalaz 같은 라이브러리는 이를 조금 더 편하게 쓸 수 있는 [NonEmptyList](http://eed3si9n.com/learning-scalaz/Validation.html#NonEmptyList) 데이터 구조를 제공한다. 내부적으로는 기본적으로 같지만(원소 하나와 리스트의 튜플), 덜 투박하게 다룰 수 있는 좋은 API를 제공한다.

결과가 비어 있지 않음을 강제하려고 함수에서 `(String, Seq[String])`을 반환하지 않더라도, 이런 편리한 데이터 타입을 위해 Scalaz에 의존하지 않더라도, 이런 분석은 코드의 타입 안전성을 높이는 데 여전히 매우 유용하다. 원하는 데이터를 담을 수 "있는" 아무 타입이나 고르지 말고, 그 값에 *정확히* 무엇이 나타날 수 있는지 잠시 생각한 뒤 가능한 데이터 타입 중에서 의도를 가장 정밀하게 표현하는 타입을 고르는 것은 대개 그만한 가치가 있다.

## 스스로를 설명하는 데이터

"모양"이 같은 데이터라고 해서 전부 같은 것은 아니다! 예를 들어 뉴턴(N) 단위 값을 나타내는 `Double`과 파운드 단위 값을 나타내는 `Double`은 둘 다 `Double`이어도 완전히 다르며, 이 둘을 혼동하면 [3억 2,700만 달러짜리 우주선이 추락해 불타 버릴 수 있다](https://en.wikipedia.org/wiki/Mars_Climate_Orbiter#Cause_of_failure). 마찬가지로 타임스탬프를 담은 `Int`, ID를 담은 `Int`, 열거형이나 플래그 값을 담은 `Int`는 전부 `Int`인데도 같은 것이 아니다. ID `Int`를 열거형 `Int`에 더할 일은 확실히 없다. 할 수는 있지만, 기본적으로 절대 원하는 일이 아니다.

여기에는 해법이 있다. 이 값들이 모양은 같아도 의미가 다르다는 것을 컴파일러에게 가르치는 것이다! 즉 서로 다른 타입을 주는 것이다. 이를 적용할 수 있는 흔한 경우 몇 가지를 보자.

### 정수 열거형을 피하라

가능한 값들의 열거를 이렇게 정의하는 패턴은 드물지 않다.

```scala
val UNIT_TYPE_UNKNOWN = 0
val UNIT_TYPE_USERSPACEONUSE = 1
val UNIT_TYPE_OBJECTBOUNDINGBOX = 2
```

여기에는 여러 장점이 있다.

- `Int`는 값싸고, 저장하거나 전달하는 데 최소한의 메모리만 든다
- 리터럴 대신 상수를 쓰면 코드 곳곳에서 `2` 같은 매직 값이 튀어나오는 것보다 "무엇을" 하려는지 조금 더 분명해진다

나쁘지 않다. 하지만 이 코드는 가능한 것보다 "덜 안전"하다. 예를 들어 다음은 무의미하고 기본적으로 잘못됐지만 컴파일 에러를 일으키지 않는다.

```scala
UNIT_TYPE_UNKNOWN + 10
```

이것도 마찬가지다.

```scala
UNIT_TYPE_USERSPACEONUSE - UNIT_TYPE_OBJECTBOUNDINGBOX
```

두 경우 모두 계산 결과는 `UNIT_TYPE`이 기대되는 어디에서도 사실상 쓸 수 없다. `UNIT_TYPE`을 받는 함수에 `UNIT_TYPE_UNKNOWN + 10`을 넘긴다면 제대로 동작하리라 기대할 수 없다.

`UNIT_TYPE` 값들은 `Int`로 선언되어 있지만, `Int`의 일반 연산 중 어느 것도 사실상 적용되지 않는다. 마찬가지로 `Int`는 `+2147483647`부터 `-2147483648`까지 어떤 값이든 될 수 있지만, 그 *40억* 개의 값 중 이 `UNIT_TYPE` 상수가 쓰이는 자리에 쓸 수 있는 것은 단 *셋*뿐이다. 그리고 `UNIT_TYPE` 상수에 무효한 `Int` 연산을 하는 것을 막을 방법이 없듯이, `0`부터 `2`까지의 `UNIT_TYPE` 상수가 기대되는 곳에 무효한 `Int`(예를 들어 `15`)를 넘기는 것을 막을 방법도 없다.

더 타입 안전한 해법은 이렇다.

```scala
sealed trait UnitType
object UnitType{
  case object Unknown extends UnitType
  case object UserSpaceOnUse extends UnitType
  case object ObjectBoundingBox extends UnitType
}
```

또는 이렇게도 할 수 있다.

```scala
// `name: String` 파라미터를 받게 해서 보기 좋은 toString을 줄 수도 있다
case class UnitType private ()
object UnitType{
  val Unknown = new UnitType
  val UserSpaceOnUse = new UnitType
  val ObjectBoundingBox = new UnitType
}
```

두 경우 모두 같은 것을 달성한다. `UnitType`을 진짜 타입으로 만드는 것이다. 그러면 그것에 `Int` 연산을 하려고 하면 컴파일 에러가 난다.

```scala
@ UnitType.Unknown + 10
type mismatch;
 found   : Int(10)
 required: String
UnitType.Unknown + 10
                   ^
Compilation Failed
```

`UnitType`이 기대되는 곳에 무효한 `Int`를 넘기려 해도 마찬가지다.

```scala
@ val currentUnitType: UnitType = 15
type mismatch;
 found   : Int(15)
 required: UnitType
val currentUnitType: UnitType = 15
                                ^
Compilation Failed
```

당연히 `UnitType`을 `Int`로, 그 반대로 의도적으로 변환하는 것은 언제나 가능하다.

```scala
@ def unitTypeToInt(ut: UnitType): Int = ut match{
    case UnitType.Unknown => 0
    case UnitType.UserSpaceOnUse => 1
    case UnitType.ObjectBoundingBox => 2
  }
defined function unitTypeToInt
@ unitTypeToInt(UnitType.Unknown)
res46: Int = 0
@ unitTypeToInt(UnitType.ObjectBoundingBox)
res47: Int = 2
```

`UnitType`을 직렬화해서 네트워크로 보내거나 정수 상수에 의존하는 서드파티 라이브러리에 넘긴다면 이 변환은 분명 필요할 것이다. 그래도 이 변환을 할 때는 명시적으로 해야 하고, 의도하지 않은 것을 변환하려고 "실수로" `unitTypeToInt`(또는 그 역인 `intToUnitType`)를 호출할 가능성은 낮다.

위의 타입 안전성 정의에서처럼, 중요한 것은 *이론적* 문제가 무엇인지만이 아니라 그 문제에 부딪힐 *실질적* 확률이다. 그리고 변환 함수를 실수로 호출할 가능성은 값을 엉뚱한 자리에 넣을 가능성보다 훨씬 낮다!

### 문자열 플래그를 피하라

정수 열거형을 피하고 싶은 것과 같은 이유로, 플래그를 표현하려고 문자열을 돌리는 것도 피하고 싶을 것이다. 같은 예제를 문자열을 돌리는 흔한 패턴에 맞게 손보면

```scala
val UNIT_TYPE_UNKNOWN = "unknown"
val UNIT_TYPE_USERSPACEONUSE = "user-space-on-use"
val UNIT_TYPE_OBJECTBOUNDINGBOX = "object-bounding-box"
```

정수를 쓸 때와 같은 문제가 있다. 이 `UNIT_TYPE` 플래그에 `.subString`, `.length`, `.toUpperCase`를 비롯한 모든 문자열 메서드를 호출할 수 있는데, 전부 무의미하고 결코 원하는 일이 아니다. 마찬가지로 `UNIT_TYPE` 플래그가 필요한 곳에 온갖 `String`을 넘길 수 있는데, 이 세 가지 특별한 값 외에는 거의 전부 무효하다.

해법은 정수 열거형을 피할 때 썼던 것과 같다. `UnitType`을 표현하는 타입을 정의하고 `String` 대신 그것을 쓰는 것이다.

```scala
sealed trait UnitType
object UnitType{
  case object Unknown extends UnitType
  case object UserSpaceOnUse extends UnitType
  case object ObjectBoundingBox extends UnitType
}

// 또는 이렇게

class UnitType private ()
object UnitType{
  val Unknown = new UnitType
  val UserSpaceOnUse = new UnitType
  val ObjectBoundingBox = new UnitType
}
```

### 정수 ID를 박싱하라

각종 ID는 흔히 원시 타입으로 저장된다. 자동 증가 ID는 대개 `Int`나 `Long`이고, UUID는 대개 `String`이나 `java.util.UUID`다. 하지만 `Int`나 `Long`과 달리 ID에는 몇 가지 고유한 속성이 있다.

- **산술 연산이 대체로 전부 말이 안 된다**: `userId: Int`가 있을 때 `userId * 2`는 기본적으로 절대 하지 않는 일이다.

- **서로 다른 ID는 교환할 수 없다**: `userId: Int`와 함수 `def deploy(machineId: Int)`가 있을 때, `deploy(userId)` 호출은 기본적으로 절대 원하는 일이 아니다

ID는 아마 메모리에 `Int`나 `Long`이나 `String`이나 `java.util.UUID`로 저장되고, 데이터베이스나 캐시에도 같은 방식으로 저장되겠지만, 사실상 원시 타입으로 대체될 수 없고, 원시 타입 연산에 쓸 수 없으며, 서로 대체할 수도 없다. 따라서 각종 ID를 별개의 클래스로 박싱하는 것이 합리적일 수 있다. 예를 들어

```scala
case class UserId(id: Int)
case class MachineId(id: Int)
case class EventId(id: Int)
...
```

메모리 오버헤드와 성능 오버헤드가 좀 있지만, `userId: UserId`와 `def deploy(machine: MachineId)`가 있을 때 실수로 `deploy(userId)`를 호출하면 런타임 에러가 아니라 컴파일 에러가 되도록 보장해 준다. 아니면 더 나쁘게는 런타임에 *아무 에러도 없이* *조용히 엉뚱한 머신을 배포하는* 상황 말이다! 방탄은 아니다. "악의적인" 프로그래머는 `deploy(MachineId(userId.id))`를 호출해 이 장벽을 쉽게 우회할 수 있다. 그래도 우발적 오용의 대부분은 잡아낼 것이다.

주어진 예제는 짧고 "뻔"하지만, 다른 사람이 몇 달 또는 몇 년 전에 작성했을 수도 있는 실제 코드에서는 상황이 이것보다 훨씬 덜 분명하다.

```scala
def deploy(machineId: Int) = ...

deploy(myUserId) // 틀림
deploy(myMachineId) // 맞음
```

더 현실적인 시나리오에서는 이런 것을 보고 있을 것이다.

```scala
def deploy(machineId: Int,
           deployingUser: Int,
           proxyMachine: Int,
           timeout: Int,
           machineType: String,
           ipAddress: String,
           timeout: Int) = ...


deploy(
  myMachineId,
  myProxyId,
  myUserId,
  1000,
  "c3.xlarge",
  "192.168.1.1"
) // 틀림

deploy(
  myMachineId,
  myUserId,
  myProxyId,
  1000,
  "c3.xlarge",
  "192.168.1.1"
) // 맞음
```

이런 시나리오에서라면 서로 다른 종류의 ID를 서로 다른 타입으로 박싱하는 비용을 기꺼이 치를 만하다.

```scala
def deploy(machineId: MachineId,
           deployingUser: UserId,
           proxyMachine: MachineId,
           timeout: Int,
           machineType: String,
           ipAddress: String,
           timeout: Int) = ...

deploy(
  myMachineId,
  myProxyId,
  myUserId,
  1000,
  "c3.xlarge",
  "192.168.1.1"
)
// type mismatch;
//   found   : MachineId
//   required: UserId
// myProxyId,
// ^
// Compilation Failed
```

`// 틀림` 예제가 런타임 에러나 런타임 오동작이 아니라 컴파일 에러가 되도록 하기 위해서다!

이런 기법은 얼마든지 멀리 밀고 나갈 수 있다. 예를 들어 `machineId`와 `proxyMachine`은 둘 다 `MachineId`가 아니라 서로 다른 타입이어야 할까? 그 답은 하나를 다른 하나로 얼마나 자주 재사용하느냐에 달렸다. 실제로 다른 타입(예: `DeployTargetId`와 `ProxyMachineId`)으로 만들더라도 재사용이 불가능해지는 것은 아니다. `.id` 속성으로 하나를 풀었다가 원하는 래퍼로 다시 감싸는 보일러플레이트를 조금 더 쓰면 된다.

```scala
case class DeployTargetId(id: Int)
case class ProxyMachineId(id: Int)

val deployTargetId: DeployTargetId = ???
val proxyId = ProxyMachineId(deployTargetId.id)
```

실용적으로, ID를 주고받으면서 감싸고 푸는 일이 거의 없다면 별도의 래퍼를 줄 가치가 더 크다. 코드 전체에서 ID를 끊임없이 감싸고 풀어야 한다는 것을 알게 된다면, 별도의 래퍼 타입을 줄 가치가 없는 것이다.

## 결론

이 글에서는 실용적인 관점에서 "타입 안전"하다는 것이 무슨 뜻인지 이야기하고, Scala 코드의 타입 안전성 속성을 개선하는 데 도움이 되는 여러 기법을 살펴봤다. 이 모든 경우에 더해진 안전성은 근사치다. 사람들이 여러분 코드의 "뒤로" 손을 뻗어 나쁜 짓을 하는 것 — 박싱한 정수 ID를 풀거나, 문자열이 아닌 플래그를 `match`해서 다시 문자열로 되돌리는 것 — 을 막는 이론적 장벽은 없다. 정말 필요할 때 이것들을 푸는 불편함이, 다른 상황에서 이런 "쉬운" 실수들을 제거해서 얻는 안전성으로 상쇄되고도 남기를 바랄 뿐이다.

그런데 이득이 *실제로* 비용을 능가하는지 어떻게 알까?

그것은 코드를 어떻게 사용하느냐에 달린 경험적인 질문이다. 얼마나 자주 불편을 겪을지 대 얼마나 자주 실수에서 구원받을지. 기본적으로 이 기법들 전부에 성능이든 편의성이든 트레이드오프가 있고, 각 시나리오에 맞는 트레이드오프가 무엇인지는 개발자 각자가 판단할 몫이다.

이 글을 읽으면서, 컴파일러가 실수를 잡아 주고 타입 안전성을 높이도록 여러분의 Scala 코드에 활용할 수 있는 유용한 기법들을 배웠기를 바란다.

Scala를 작성할 때 타입 안전성을 높이기 위해 어떤 다른 팁이나 기법을 쓰는가? 아래 댓글로 알려 달라!
