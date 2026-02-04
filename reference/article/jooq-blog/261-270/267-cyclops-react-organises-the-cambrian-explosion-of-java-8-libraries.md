# Cyclops-react가 Java 8 라이브러리의 캄브리아 폭발을 정리하다

> 원문: https://blog.jooq.org/cyclops-react-organises-the-cambrian-explosion-of-java-8-libraries/

AOL의 John Mcclean이 작성한 매우 흥미로운 게스트 포스트를 jOOQ 블로그에 소개하게 되어 기쁩니다. AOL은 1985년에 설립된 글로벌 디지털 미디어 및 기술 회사로, 한때 America Online으로 알려졌으며 현재는 Verizon 그룹의 일부입니다. AOL은 비디오, 모바일, 광고 기술 및 플랫폼, 그리고 개방형 생태계라는 네 가지 영역에 집중하고 있습니다. AOL은 Microsoft 인벤토리와 TechCrunch, The Huffington Post, MAKERS와 같은 오리지널 콘텐츠 브랜드를 활용하여 글로벌 프로그래매틱 플랫폼 전반에서 퍼블리셔와 광고주를 연결합니다.

John은 AOL의 아키텍트입니다. 그는 광고 기술 및 플랫폼 그룹에서 근무하며 광고 수요측 예측 팀을 이끌고 있습니다. 이 팀은 수십억 개의 RTB, 노출 및 조회가능성 레코드를 실시간으로 처리하여 광고 캠페인의 가격-볼륨 곡선 및 기타 예측을 밀리초 단위로 생성하는 시스템을 구축하고 운영합니다. John은 또한 AOL 오픈 소스 프로젝트인 cyclops-react와 Microserver의 리드 개발자입니다. AOL의 예측 시스템에서 추출된 이 프로젝트들은 Java 개발자들을 함수형, 리액티브, 마이크로서비스의 경로로 안내함으로써 AOL이 대규모로 작동하는 새로운 기능을 신속하게 배포할 수 있게 해줍니다.

## Cyclops-react란 무엇인가?

Java 8에서 람다 표현식과 디폴트 메서드가 도입되면서 10년 만에 Java 언어에 가장 큰 구조적 변화가 일어났습니다. 이를 기반으로 Stream, Optional, CompletableFuture와 같은 멋진 새 API들이 구축되었고, 마침내 Java 개발자들은 더 함수형 스타일로 코딩할 수 있게 되었습니다. 이것은 매우 환영할 만한 일이었지만, 많은 사람들에게 이러한 개선은 충분하지 않았습니다. Stream, Optional, CompletableFuture는 모두 동일한 추상 구조를 공유하고 동일한 규칙을 따릅니다. 그러나 API들은 공통 메서드 이름에 합의하지 않았고, 공통 인터페이스를 제공하지도 않습니다. 예를 들어 `Stream#map` / `Optional#map`이 `CompletableFuture#thenApply`가 됩니다. 또한 Stream과 Optional에 추가된 기능이 일반적인 컬렉션에는 없습니다. `List#map`은 어디에 있을까요? JDK Stream 구현은 성능이 좋고, 완전히 지연 평가되며, 확장을 위해 잘 설계되었지만 제한된 연산자 집합만 제공합니다(아마도 데이터 병렬성에 초점을 맞추었기 때문일 것입니다).

이러한 공백을 채우기 위해 jOOλ와 같은 라이브러리가 순차 Stream 확장(Seq라고 불림)을 가지고 등장했습니다. Seq는 많은 추가 스트리밍 연산자를 추가합니다. jOOλ는 일반적으로 튜플과 같은 많은 누락된 함수형 기능을 추가합니다. cyclops-react의 핵심 목표는 FutureStreams와 같은 독창적인 기능을 추가하는 것 외에도 JDK API와 서드파티 함수형 라이브러리를 연결하는 메커니즘을 제공하는 것입니다. Java 8 출시 이후 멋진 라이브러리들의 캄브리아 폭발이 있었습니다. vavr과 Project Reactor 같은 라이브러리들입니다. cyclops-react는 우선 JDK를 확장하고 jOOλ, pCollections, Agrona와 같은 다른 라이브러리를 활용하여 이를 수행합니다. 이러한 라이브러리들도 가능한 경우 JDK 인터페이스를 확장하여 영속 컬렉션 및 대기 없는 다중 생산자 단일 소비자 큐와 같은 기능을 추가합니다.

JDK 인터페이스를 재사용하고 확장하는 것을 넘어, 우리의 목표는 개발자들이 reactive-streams API와 같은 서드파티 표준을 활용하고 정해진 표준이 없는 곳에서는 우리 자체의 추상화를 구축함으로써 외부 라이브러리와 쉽게 통합할 수 있도록 하는 것이었습니다. 현재 우리가 통합에 집중하고 있는 라이브러리는 Google의 Guava, RxJava, Functional Java, Project Reactor, 그리고 vavr입니다. 우리는 이전에 인터페이스가 존재하지 않았거나 불가능했던 Stream, Optional, CompletableFuture와 같은 타입을 래핑하기 위한 추상화를 만들었습니다. 우리가 이러한 목표를 선택한 이유는 마이크로서비스 아키텍처 전반에서 cyclops-react를 프로덕션에 사용하고 있고, 문제에 적합한 기술을 활용하면서 나머지 코드베이스와 원활하게 통합할 수 있는 것이 중요하기 때문입니다.

cyclops-react는 상당히 크고 기능이 풍부한 프로젝트이며, 추가로 여러 통합 모듈을 가지고 있습니다. 아래 글에서는 사용 가능한 일부 기능들을 다루면서 특히 cyclops-react가 JDK 전반과 선도적인 Java 8 오픈 소스 커뮤니티의 용감한 신세계를 어떻게 연결하는지 보여주는 것을 목표로 하겠습니다.

## JDK 확장하기

cyclops-react는 가능한 경우 JDK API를 확장합니다. 예를 들어 ReactiveSeq는 오류 처리, 비동기 처리 등을 위한 기능을 추가하며 JDK Stream과 jOOλ의 Seq를 모두 확장합니다. cyclops-react 컬렉션 확장은 새로운 컬렉션 구현을 만드는 대신 적절한 JDK 인터페이스를 구현하고 확장합니다. cyclops-react의 LazyFutureStream은 차례로 ReactiveSeq를 확장하고, Future들의 Stream에 대해 마치 단순한 Stream인 것처럼 집계 연산을 수행할 수 있게 합니다(이는 많은 수의 일반적인 Java I/O 작업을 비동기적이고 성능 좋게 처리하는 데 매우 유용합니다). ListX는 List를 확장하지만 즉시 실행되는 연산자를 추가합니다.

```java
ListX<Integer> tenTimes = ListX.of(1,2,3,4)
                               .map(i->i*10);
```

cyclops-react는 사용자가 탐색할 수 있는 많은 연산자를 추가합니다. 예를 들어, 여러 컬렉션에 동시에 함수를 적용할 수 있습니다. reactive-streams API는 데이터의 생산자(publishers)와 소비자(subscribers) 사이의 자연스러운 다리 역할을 합니다. 모든 cyclops-react 데이터 타입은 reactive-streams의 Publisher 인터페이스를 구현하며, 모든 cyclops-react 타입으로 변환할 수 있는 Subscriber 구현도 제공됩니다. 이는 Project Reactor와 같은 다른 reactive-streams 기반 라이브러리와의 직접적인 통합을 간단하게 만듭니다. 예를 들어 SortedSetX와 같은 cyclops publisher에서 Reactor Flux를 지연 방식으로 채우거나, Reactor 타입에서 cyclops-react 타입을 채울 수 있습니다.

```java
Flux<Integer> stream = Flux.from(
  SortedSetX.of(1,2,3,4,5,6,7,8));
//Flux[1,2,3,4,5,6,7,8]

ListX<Character> list = ListX.fromPublisher(
  Flux.just("a","b","c"));
```

Reactor의 Flux와 Mono 타입은 cyclops-react의 For comprehension과 직접 작동할 수 있습니다(지원되는 각 라이브러리는 통합 모듈에 자체적인 네이티브 For comprehension 클래스 세트도 가지고 있습니다).

```java
// import static com.aol.cyclops.control.For.*;

Publishers.each2(
  Flux.just(1,2,3),
  i -> ReactiveSeq.range(i,5),Tuple::tuple).printOut();

/*
(1, 1)
(1, 2)
(1, 3)
(1, 4)
(2, 2)
(2, 3)
(2, 4)
(3, 3)
(3, 4)
*/
```

For comprehension은 flatMap과 map 메서드를 가진 타입에 대해 중첩된 반복을 관리하는 방법으로, 적절한 메서드를 연쇄적으로 호출합니다. cyclops-react에서 중첩된 문장은 이전 문장의 요소에 접근할 수 있으므로, For comprehension은 기존 동작을 관리하는 매우 유용한 방법이 될 수 있습니다. 예를 들어, null 값을 반환할 수 있고 null 매개변수가 제공되면 NPE를 발생시키는 기존 메서드 findId와 loadData에 대한 호출을 안전하게 하려면, findId()가 값이 있는 Optional을 반환할 때만 loadData를 안전하게 실행하는 For comprehension을 사용할 수 있습니다.

```java
List<Data> data =
For.optional(findId())
   .optional(this::loadData);
// loadData는 findId()가 값을 반환할 때만 호출됩니다
```

마찬가지로, Try와 같은 타입을 사용하여 findId나 loadData의 예외 결과를 처리할 수 있고, Future를 사용하여 연쇄된 메서드를 비동기적으로 실행할 수 있습니다.

## 크로스 라이브러리 추상화 구축하기

Java 8은 모나드를 Java에 도입했습니다(Stream, Optional, CompletableFuture). 하지만 재사용에 도움이 될 공통 인터페이스를 제공하지 않았고, 실제로 CompletableFuture에서 사용되는 메서드 이름은 동일한 기능에 대해 Optional과 Stream에서 사용되는 것과 크게 다릅니다. 그래서 map은 thenApply가 되었고 flatMap은 thenCompose가 되었습니다. Java 8 세계 전반에서 모나드는 점점 더 일반적인 패턴이 되고 있지만, 종종 이들을 가로지르는 추상화 방법이 없습니다.

cyclops-react에서는 모나드를 표현하는 인터페이스를 정의하려고 시도하는 대신, 래퍼 인터페이스 세트와 Java 8용 주요 함수형 스타일 라이브러리의 다양한 인스턴스를 이러한 래퍼에 적응시키는 여러 커스텀 어댑터를 구축했습니다. 래퍼는 AnyM(Any Monad의 줄임말)을 확장하며 두 개의 하위 인터페이스가 있습니다 - 단일 값으로 해결되는 모든 모나딕 타입을 나타내는 AnyMValue(Optional이나 CompletableFuture처럼)와 궁극적으로 값의 시퀀스로 해결되는 AnyMSeq(Stream이나 List처럼)가 있습니다. cyclops 확장 래퍼는 RxJava, Guava, Reactor, FunctionalJava, vavr의 타입을 래핑하는 메커니즘을 제공합니다.

```java
// Reactor, RxJava, FunctionalJava, vavr, Guava의
// 모든 타입을 래핑할 수 있습니다
AnyMSeq<Integer> wrapped =
  Fj.list(List.list(1,2,3,4,5));

// 그리고 조작할 수 있습니다
AnyMSeq<Integer> timesTen = wrapped.map(i->i*10);
```

cyclops-react는 이러한 래퍼(및 다른 cyclops-react 타입)가 상속하는 공통 인터페이스 세트를 제공하여 개발자가 더 일반적이고 재사용 가능한 코드를 작성할 수 있게 합니다. AnyM은 reactive-streams publishers를 확장하므로, cyclops-react로 모든 vavr, Guava, FunctionalJava 또는 RxJava 타입을 reactive-streams publisher로 만들 수 있습니다.

```java
AnyMSeq<Integer> wrapped =
  Javaslang.traversable(List.of(1,2,3,4,5));

// 래핑된 타입은 reactive-streams publisher입니다
Flux<Integer> fromVavr = Flux.from(wrapped);

wrapped.forEachWithError(
  System.out::println,
  System.out::err);
```

더 나아가 cyclops-react의 리액티브 기능이 AnyM 타입에 직접 제공됩니다. 이는 예를 들어 vavr이나 FunctionalJava Stream에서 데이터 방출을 스케줄링하거나 reduce 연산을 지연 방식으로 또는 비동기적으로 실행할 수 있음을 의미합니다.

```java
AnyMSeq<Integer> wrapped =
  Javaslang.traversable(Stream.of(1,2,3,4,5));

CompletableFuture<Integer> asyncResult =
  wrapped.futureOperations(Executors.newFixedThreadPool(1))
         .reduce(50, (acc, next) -> acc + next);
//CompletableFuture[1550]

AnyMSeq<Integer> wrapped =
  FJ.list(list.list(1,2,3,4,5));

Eval<Integer> lazyResult =
  wrapped.map(i -> i * 10)
         .lazyOperations()
         .reduce(50, (acc,next) -> acc + next);
//Eval[15500]

HotStream<Integer> emitting = wrapped.schedule(
  "0 * * * * ?",
  Executors.newScheduledThreadPool(1));

emitting.connect()
        .debounce(1,TimeUnit.DAYS)
        .forEachWithError(
           this::logSuccess,
           this::logFailure);
```

cyclops-react와 더 넓은 Java 8 생태계에서 탐험할 것이 많습니다. 여러분이 Java 8의 경계를 직접 가지고 놀고, 배우고, 확장하면서 즐거운 모험을 하시길 바랍니다!
