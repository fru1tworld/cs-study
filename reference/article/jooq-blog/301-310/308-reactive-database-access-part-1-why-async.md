# 리액티브 데이터베이스 접근 - Part 1 - 왜 "비동기"인가

> 원문: https://blog.jooq.org/reactive-database-access-part-1-why-async/

## 리액티브란?

*리액티브 애플리케이션*이라는 개념이 요즘 점점 더 인기를 얻고 있으며, 아마 인터넷 어딘가에서 이미 들어보셨을 것입니다. 그렇지 않다면, [리액티브 선언문](https://www.reactivemanifesto.org/)을 읽어보시거나 다음과 같은 간단한 요약에 동의할 수 있을 것입니다: 간단히 말해서, 리액티브 애플리케이션은 다음과 같은 애플리케이션입니다:

- 비동기 프로그래밍 기법을 활용하여 계산 자원(CPU 및 메모리 사용량 측면에서)을 최적으로 활용합니다
- 장애에 대처하는 방법을 알고 있어서, 그냥 충돌하고 사용자에게 사용 불가능해지는 대신 우아하게 성능을 저하시킵니다
- 부하가 증가함에 따라 여러 머신/노드로 확장하고(그리고 다시 축소할 수 있으며) 집중적인 워크로드에 적응할 수 있습니다

리액티브 애플리케이션은 순수하게 야생의 원시적인 그린필드에만 존재하지 않습니다. 어느 시점에서는 의미 있는 일을 하기 위해 데이터를 저장하고 접근해야 하며, 그 데이터가 관계형 데이터베이스에 있을 가능성이 높습니다.

## 지연 시간과 데이터베이스 접근

애플리케이션이 데이터베이스와 통신할 때 대부분의 경우 데이터베이스 서버는 애플리케이션과 동일한 서버에서 실행되지 않습니다. 운이 나쁘면 애플리케이션을 호스팅하는 서버(또는 서버 집합)가 데이터베이스 서버와 다른 데이터 센터에 있을 수도 있습니다. 지연 시간 측면에서 이것이 의미하는 바는 다음과 같습니다:

애플리케이션이 첫 페이지에서 간단한 `SELECT` 쿼리를 실행한다고 가정해 봅시다(이것이 좋은 아이디어인지 아닌지 여기서 논쟁하지 않겠습니다). 애플리케이션과 데이터베이스 서버가 같은 데이터 센터에 있다면, 약 500 µs 정도의 지연 시간이 발생합니다(돌아오는 데이터의 양에 따라 다릅니다). 이제 이 시간 동안 CPU가 할 수 있는 모든 것(위 그림의 모든 녹색과 검은색 사각형들)과 비교해 보세요 – 곧 다시 이 주제로 돌아올 것이니 기억해 두세요.

## 스레드의 비용

환영 페이지 쿼리를 동기 방식(JDBC가 하는 방식)으로 실행하고 데이터베이스에서 결과가 돌아올 때까지 기다린다고 가정해 봅시다. 이 모든 시간 동안 결과가 돌아오기를 기다리는 스레드를 독점하게 됩니다. 아무것도 하지 않고 그냥 존재하는 Java 스레드는 최대 1 MB의 스택 메모리를 차지할 수 있으므로, 사용자당 하나의 스레드를 할당하는 스레드 서버를 사용한다면(Tomcat, 당신을 보고 있습니다) Hacker News에 소개될 때도 애플리케이션이 작동하려면 상당한 메모리가 필요합니다(동시 사용자당 1 MB).

Play Framework로 구축된 것과 같은 리액티브 애플리케이션은 이벤트 서버 모델을 따르는 서버를 사용합니다: "한 사용자, 한 스레드" 원칙을 따르는 대신 요청을 이벤트 집합(데이터베이스 접근이 이러한 이벤트 중 하나가 됨)으로 처리하고 이벤트 루프를 통해 실행합니다.

이러한 서버는 많은 스레드를 사용하지 않습니다. 예를 들어, Play Framework의 기본 구성은 CPU 코어당 하나의 스레드를 생성하며 풀에 최대 24개의 스레드를 가집니다. 그럼에도 불구하고, 이러한 유형의 서버 모델은 동일한 하드웨어에서 스레드 모델보다 훨씬 더 많은 동시 요청을 처리할 수 있습니다. 비결은, 태스크가 대기해야 할 때 다른 이벤트에 스레드를 넘겨주는 것입니다 – 다시 말해: 비동기 방식으로 프로그래밍하는 것입니다.

## 고통스러운 비동기 프로그래밍

비동기 프로그래밍은 실제로 새로운 것이 아니며, 이를 다루기 위한 프로그래밍 패러다임은 70년대부터 존재했고 그 이후로 조용히 발전해 왔습니다. 그러나 비동기 프로그래밍이 대부분의 개발자에게 반드시 행복한 기억을 떠올리게 하는 것은 아닙니다. 몇 가지 전형적인 도구와 그 단점들을 살펴보겠습니다.

### 콜백

일부 언어(Javascript, 당신을 보고 있습니다)는 최근까지(ECMAScript 6에서 Promise가 도입됨) 비동기 프로그래밍을 위한 유일한 도구로 콜백만 가진 채 70년대에 갇혀 있었습니다. 이것은 "크리스마스 트리 프로그래밍"으로도 알려져 있습니다. [호호호.](http://callbackhell.com/)

### 스레드

Java 개발자로서, 비동기라는 단어가 반드시 매우 긍정적인 의미를 갖는 것은 아니며 종종 악명 높은 `synchronized` 키워드와 연관됩니다:

> "synchronized 코드의 95%는 망가져 있습니다. 나머지 5%는 Brian Goetz가 작성했습니다." – Venkat Subramaniam

Java에서 스레드로 작업하는 것은 어렵습니다, 특히 가변 상태를 사용할 때 – 기본 애플리케이션 서버가 모든 비동기 작업을 추상화하고 걱정하지 않아도 되게 하는 것이 훨씬 편리하죠, 그렇지 않나요? 불행히도 방금 본 것처럼 이것은 성능 측면에서 상당히 큰 비용이 듭니다. 그리고, 이 스택 트레이스를 한번 보세요.

한 가지 측면에서, 스레드 서버가 비동기 프로그래밍에 대해 갖는 관계는 Hibernate가 SQL에 대해 갖는 관계와 같습니다 – 장기적으로 큰 비용을 치르게 되는 누수 추상화입니다. 그리고 이를 깨달았을 때는 종종 너무 늦어서 추상화에 갇혀 성능을 높이기 위해 모든 수단을 동원해 싸우게 됩니다. 데이터베이스 접근에서는 추상화를 포기하는 것이 비교적 쉽지만(순수 SQL을 사용하거나, 더 좋게는 jOOQ를 사용), 비동기 프로그래밍의 경우 더 나은 도구는 이제야 인기를 얻기 시작했습니다. 함수형 프로그래밍에 뿌리를 둔 프로그래밍 모델인 Future로 전환해 보겠습니다.

## Future: 비동기 프로그래밍의 SQL

Scala에서 찾을 수 있는 Future는 비동기 프로그래밍을 다시 즐겁게 만들기 위해 수십 년 동안 존재해 온 함수형 프로그래밍 기법을 활용합니다.

### Future 기본 사항

`scala.concurrent.Future[T]`는 성공하면 결국 타입 `T`의 값을 포함할 상자로 볼 수 있습니다. 실패하면 실패의 원인이 된 `Throwable`이 유지됩니다. Future는 기다리고 있는 계산이 결과를 산출하면 *성공*했다고 하고, 계산 중에 오류가 있으면 *실패*했다고 합니다. 어느 경우든, Future가 계산을 완료하면 *완료*되었다고 합니다.

Future가 선언되자마자 실행을 시작합니다. 이는 달성하려는 계산이 비동기적으로 실행된다는 것을 의미합니다. 예를 들어, Play Framework의 WS 라이브러리를 사용하여 Play Framework 웹사이트에 대한 GET 요청을 실행할 수 있습니다:

```scala
val response: Future[WSResponse] =
  WS.url("http://www.playframework.com").get()
```

이 호출은 즉시 반환되며 다른 작업을 계속할 수 있게 해줍니다. 미래의 어느 시점에서 호출이 실행되었을 수 있으며, 그때 결과에 접근하여 무언가를 할 수 있습니다. Java의 `java.util.concurrent.Future<V>`와 달리(Future가 완료되었는지 확인하거나 `get()` 메서드로 검색하는 동안 블록할 수 있게 해주는), Scala의 Future는 실행 결과로 무엇을 할지 지정할 수 있게 해줍니다.

### Future 변환하기

상자 안에 있는 것을 조작하는 것도 쉬우며 결과가 사용 가능해질 때까지 기다릴 필요가 없습니다:

```scala
val response: Future[WSResponse] =
  WS.url("http://www.playframework.com").get()

val siteOnline: Future[Boolean] =
  response.map { r =>
    r.status == 200
  }

siteOnline.foreach { isOnline =>
  if(isOnline) {
    println("The Play site is up")
  } else {
    println("The Play site is down")
  }
}
```

이 예제에서, 응답의 상태를 확인하여 `Future[WSResponse]`를 `Future[Boolean]`으로 변환합니다. 이 코드는 어떤 지점에서도 블록하지 않는다는 것을 이해하는 것이 중요합니다: 응답이 사용 가능해질 때만 응답 처리를 위한 스레드가 가용해지고 `map` 함수 내부의 코드가 실행됩니다.

### 실패한 Future 복구하기

실패 복구도 매우 편리합니다:

```scala
val response: Future[WSResponse] =
  WS.url("http://www.playframework.com").get()

val siteAvailable: Future[Option[Boolean]] =
  response.map { r =>
    Some(r.status == 200)
  } recover {
    case ce: java.net.ConnectException => None
  }
```

Future의 맨 끝에서 특정 유형의 예외를 처리하고 피해를 제한하는 `recover` 메서드를 호출합니다. 이 예제에서는 `java.net.ConnectException`의 불행한 경우만 `None` 값을 반환하여 처리하고 있습니다.

### Future 합성하기

Future의 킬러 기능은 합성 가능성입니다. 비동기 프로그래밍 워크플로우를 구축할 때 매우 전형적인 사용 사례는 여러 동시 작업의 결과를 결합하는 것입니다. Future(와 Scala)는 이를 상당히 쉽게 만들어 줍니다:

```scala
def siteAvailable(url: String): Future[Boolean] =
  WS.url(url).get().map { r =>
    r.status == 200
}

val playSiteAvailable =
  siteAvailable("http://www.playframework.com")

val playGithubAvailable =
  siteAvailable("https://github.com/playframework")

val allSitesAvailable: Future[Boolean] = for {
  siteAvailable <- playSiteAvailable
  githubAvailable <- playGithubAvailable
} yield (siteAvailable && githubAvailable)
```

`allSitesAvailable` Future는 두 Future가 완료될 때까지 기다리는 *for 컴프리헨션*을 사용하여 구축됩니다. 두 Future `playSiteAvailable`과 `playGithubAvailable`은 선언되자마자 실행을 시작하고 for 컴프리헨션이 이들을 합성합니다. 그리고 이 Future들 중 하나가 실패하면, 결과 `Future[Boolean]`은 직접적으로 실패하게 됩니다(다른 Future가 완료되기를 기다리지 않고).

## 계속 읽기

이 시리즈의 일부로 곧 Part 2와 Part 3를 게시할 예정이니 계속 주목해 주세요:

- Part 1: 왜 리액티브인가, 왜 "비동기"인가 & Future 소개
- Part 2: Actor 소개
- Part 3: Scala, Future, Actor와 함께 jOOQ 사용하기
