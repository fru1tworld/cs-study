# 모든 라이브러리는 제로 의존성 정책을 따라야 한다

> 원문: https://blog.jooq.org/all-libraries-should-follow-a-zero-dependency-policy/

이 클릭베이트 제목이 달린 재미있는 글이 최근 내 눈길을 끌었다:

[Medium.com에서 보기](https://medium.com/friendship-dot-js/i-peeked-into-my-node-modules-directory-and-you-wont-believe-what-happened-next-b89f63d21558)

node 생태계에서 JavaScript 개발의 현재 상태에 대한 재미있는 (비록 그다지 진지하거나 사실적이지는 않은) 비난이다.

## 의존성 지옥은 새로운 것이 아니다

[의존성 지옥(Dependency hell)은 위키피디아에 등재된 용어다](https://en.wikipedia.org/wiki/Dependency_hell). 위키피디아는 이를 다음과 같이 정의한다:

> "의존성 지옥은 특정 버전의 다른 소프트웨어 패키지에 의존하는 소프트웨어 패키지를 설치한 일부 소프트웨어 사용자들의 좌절감을 나타내는 구어적 용어이다."

의존성 지옥의 큰 문제는 작은 라이브러리들이 너무 많은 코드 중복을 피하기 위해 의존하는 추가 의존성을 끌어온다는 사실이다. 예를 들어, Guava 18.0을 누가 사용하는지 확인해 보자:
[https://mvnrepository.com/artifact/com.google.guava/guava/18.0/usages](https://mvnrepository.com/artifact/com.google.guava/guava/18.0/usages)

다음과 같은 라이브러리들을 발견할 것이다:

- com.fasterxml.jackson.core » jackson-databind
- org.springframework » spring-context-support
- org.reflections » reflections
- org.joda » joda-convert
- ... 2000개 이상

이제 여러분의 애플리케이션에서 아직 Guava 15.0을 사용하고 있다고 가정해 보자. Spring을 업그레이드하고 싶다. 애플리케이션이 여전히 작동할까? 그 업그레이드가 바이너리 호환성을 가질까? Spring이 이것을 보장해 줄까, 아니면 알아서 해결해야 할까? 이제 Spring이 Joda Time도 사용하고, Joda Time도 Guava를 사용한다면? 이게 작동하기는 할까? 반대로, Guava가 Joda Time에 의존할 수 있을까, 아니면 순환적인 Maven Central 의존성이 모든 특이점들을 하나의 거대한 블랙홀로 붕괴시킬까?

## 진실은: 의존성이 필요 없다

... 그리고 "여러분"이라 함은 Guava (또는 무엇이든)를 사용하여 복잡한 엔터프라이즈 애플리케이션을 작성하는 최종 사용자를 의미하는 것이 아니다. 당신은 필요하다. 하지만 _당신_, 친애하는 라이브러리 개발자여. 당신은 확실히 어떤 의존성도 필요 없다.

[jOOQ](https://www.jooq.org)의 예를 들어보자. SQL 문자열 조작 라이브러리로서, 우리는 [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/)에 대한 의존성을 가져왔다. 왜냐하면:

- 그들에게는 우리가 사용하고 싶은 좋은 StringUtils가 있다
- 그들의 라이선스도 ASL 2.0이어서 jOOQ의 라이선스와 호환된다

하지만 jOOQ 3.x와 commons-lang 2.x 의존성을 하드와이어링하는 대신, 우리는 그들의 클래스 중 하나와 그 일부만을 내재화하여 `org.jooq.tools.StringUtils`로 재패키징하는 것을 선택했다. 본질적으로, 우리가 필요한 것은:

- abbreviate()
- isEmpty()
- isBlank()
- leftPad() (안녕 node 개발자들)

... 그리고 몇 가지 더였다. 이것이 전체 의존성을 끌어오는 것을 정당화하지는 않는다, 그렇지 않은가? 왜냐하면 우리에게는 상관없겠지만, 더 오래된 버전이나 더 새로운 버전의 commons-lang을 사용하고 싶어할 수 있는 수천 명의 사용자들에게는 중요하기 때문이다. 그리고 이것은 시작에 불과하다. commons-lang에 전이 의존성이 있다면? 기타 등등.

## 라이브러리 개발자 여러분, 제발 의존성을 피해주세요

그러니 제발, 친애하는 라이브러리 개발자들이여. 라이브러리에 의존성을 추가하는 것을 피해달라. 의존해야 하는 유일한 것들은:

- JDK
- JSR에 의해 관리되는 일부 API (예: JPA)

그게 전부다. _당신_ 라이브러리 개발자들이 합리적이 되고 게으름을 피우지 않기 시작한다면, 우리 모두 더 나은 소프트웨어를 작성하고 전체 인터넷을 다운로드하는 것을 멈출 수 있다.

## 위 규칙의 예외

- 프레임워크 및 플랫폼 벤더(예: Spring, Java EE)는 제외된다. 그들은 전체 플랫폼을 정의한다. 즉, 그들은 잘 문서화된 의존성 세트를 _부과한다_. 애플리케이션에서 프레임워크/플랫폼을 사용한다면, 플랫폼의 규칙을 따라야 한다

그게 전부다. jOOQ와 같은 작은 라이브러리는 어떤 의존성도 가지면 안 된다.
