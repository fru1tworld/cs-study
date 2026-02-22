# 메시징에 RDBMS를 사용하는 것은 완전히 괜찮다

> 원문: https://blog.jooq.org/using-your-rdbms-for-messaging-is-totally-ok/

2014년 9월 26일 lukaseder 작성

논쟁적인 데이터베이스 주제는 reddit에서 확실한 성공을 거둔다. 왜냐하면 모든 사람이 이런 주제에 대해 의견을 가지고 있기 때문이다. 더 중요한 것은, 많은 사람들이 *독단적인* 의견을 가지고 있다는 점인데, 이것은 항상 실용주의보다 더 많은 논쟁을 불러일으킨다.

그래서 최근에 나는 [Mike Hadlow](https://twitter.com/mikehadlow)가 쓴 [The Database As Queue Anti-Pattern](http://mikehadlow.blogspot.ch/2012/04/database-as-queue-anti-pattern.html)이라는 오래된 글에 대한 링크를 올렸고, [/r/programming](http://www.reddit.com/r/programming)에서 꽤 괜찮은 결과를 얻었다:

[![reddit-queueing](https://i0.wp.com/blog.jooq.org/wp-content/uploads/2014/09/reddit-queueing.png?resize=519%2C92&ssl=1)](http://redd.it/2gjget)

Mike의 글은 데이터베이스에서의 모든 종류의 큐잉에 반대하는 내용이었는데, 이는 최근 토론에서 몇몇 [JavaZone](http://2014.javazone.no) 발표자들에게서 들은 의견과 일치한다. 그들은 모두 데이터베이스에서의 메시징이 "악"이라는 데 동의했다.

... 그리고 나는 이렇게 말한다: 아니다. 데이터베이스에서의 메시징은 안티패턴이 아니다. 그것은 완전히 괜찮다(괜찮을 수 있다). 그 이유를 살펴보자:

## KISS와 YAGNI

먼저, 수천 가지의 메시지 유형과 시간당 수백만 개의 메시지를 배포할 계획이 없다면, 아마 매우 단순한 메시징 문제를 가지고 있을 것이다. 이미 RDBMS를 사용하고 있으므로, 그 RDBMS를 메시징에도 사용하는 것은 분명히 하나의 옵션이다.

물론, 많은 분들이 지금 이렇게 생각할 것이다:

> 망치만 가지고 있으면 모든 것이 못처럼 보인다

... 그리고 당신이 옳다! 하지만 이봐요, 망치만 가지고 있는 이유는 다음 중 하나일 수 있다:

* 정교한 도구 상자를 갖출 시간이 없다
* 정교한 도구 상자를 갖출 돈이 없다
* 실제로 정교한 도구 상자가 정말로 필요하지 않다

이러한 주장들에 대해 생각해 보라. 그렇다, 해결책이 완벽하지 않거나 심지어 못생겼을 수도 있다...

[![The PHP Hammer](https://i0.wp.com/blog.jooq.org/wp-content/uploads/2014/04/6a0120a85dcdae970b017742d249d5970d-800wi.jpg?resize=502%2C370&ssl=1)](http://en.wiktionary.org/wiki/if_all_you_have_is_a_hammer,_everything_looks_like_a_nail)

하지만 우리는 엔지니어이고, 따라서 우리는 완벽함을 논하기 위해 여기 있는 것이 아니다. 우리는 고객에게 가치를 전달하기 위해 여기 있고, 망치로 일을 해낼 수 있다면, 왜 우리의 허영심을 잊고 그냥 그 망치를 사용해서 일을 해내지 않겠는가.

## 트랜잭션 큐

완벽한 세상에서, 큐는 트랜잭션적이고 (기저 이론이 허용하는 범위 내에서) 원자적 메시지 전달 또는 실패를 보장한다 – 이것은 우리가 JMS에서 영원히 당연하게 여겨왔던 것이다.

[GeeCON Krakow](http://geecon.org/)에서, 나는 [Konrad Malawski](https://twitter.com/ktosopl)와 그의 [Akka Persistence](http://2014.geecon.org/speakers/konrad-malawski.html) 발표에 관해 매우 흥미로운 토론을 했다. [Akka](http://akka.io/)에서 모든 과대광고와 유행어(예: 리액티브 프로그래밍 등)를 제거하면, Akka는 JMS의 독점적 대안에 불과하다는 것을 알 수 있다. 더 현대적으로 보이지만 JMS에서 사용하는 데 익숙한 수많은 기능들(예: 트랜잭션 큐 영속성)이 부족하다.

Konrad Malawski와의 토론에서 흥미로운 측면 중 하나는 100% 메시지 전달 보장은 신화라는 사실이다([여기에 자세한 내용](http://doc.akka.io/docs/akka/2.3-M2/general/message-delivery-guarantees.html)). 이는 다음과 같은 결론으로 이어진다:

> 메시징은 정말로 어렵다

정말로 그렇다! 그래서 정교한 MQ 시스템을 내장해야 한다고 정말로 생각한다면, 그것이 실제로 어떻게 작동하는지, 그리고 그것을 올바르게 운영하는 방법을 배워야 한다는 사실을 주의해야 한다.

RDBMS 기반 큐를 사용한다면, 이러한 추가적인 트랜잭션 복잡성을 제거할 수 있다. 왜냐하면 큐 작업이 데이터베이스와 이미 가지고 있는 트랜잭션에 참여하기 때문이다. ACID를 무료로 얻을 수 있다!

## 추가적인 운영 노력이 필요 없다

개발자들이 매우 자주 과소평가하는 것(우리는 이것을 아무리 강조해도 지나치지 않다)은 새로운 외부 시스템을 추가할 때 운영 팀에 발생하는 비용이다.

하나의 단순한 RDBMS(와 자신의 애플리케이션)만 있는 것은 매우 매우 간결하고 단순한 아키텍처다. RDBMS, MQ, 그리고 애플리케이션이 있는 것은 이미 더 복잡하다.

운영 환경 데이터베이스를 운영할 때 자신이 무엇을 하고 있는지 아는 훌륭한 DBA가 많이 있다. 훌륭한 "MQA"를 찾는 것은 훨씬 더 어렵다.

## Oracle을 사용하고 있다면: Oracle AQ를 사용하라

Oracle은 [Oracle AQ](http://docs.oracle.com/database/121/ADQUE/release_change.htm#ADQUE3676)라고 불리는 매우 정교한 내장 큐잉 API를 가지고 있으며, 이것은 [JMS와 상호 운용](http://docs.oracle.com/cd/E24329_01/web.1211/e24385/aq_jms.htm)될 수 있다.

AQ의 큐는 기본적으로 메시지 유형의 직렬화된 버전을 포함하는 테이블일 뿐이다. jOOQ를 사용하고 있다면, [최근에 Oracle AQ를 jOOQ와 통합하는 방법에 대해 블로그에 올린 글](https://blog.jooq.org/using-oracle-aq-in-java-wont-get-any-easier-than-this/ "Using Oracle AQ in Java Won't Get Any Easier Than This")이 있다.

## RDBMS 중심 애플리케이션은 훨씬 더 쉬울 수 있다

우리는 이전에도 이에 대해 블로그에 글을 올린 적이 있다: [왜 당신의 지루한 데이터가 당신의 섹시한 새 기술보다 더 오래 살아남을 것인가](https://blog.jooq.org/why-your-data-will-outlast-your-sexy-new-technology/ "Why Your Boring Data Will Outlast Your Sexy New Technology").

당신의 데이터는 애플리케이션보다 오래 살아남을 수 있다. [Paypal이 Java를 JavaScript로 교체한 것](http://www.zdnet.com/how-replacing-java-with-javascript-is-paying-off-for-paypal-7000023697/)을 생각해 보라(반대 방향으로도 갈 수 있었다). 하지만 결국, Paypal이 모든 데이터베이스도 교체했다고 생각하는가? 나는 그렇지 않다고 생각한다. Oracle에서 DB2로(다른 벤더), 또는 Oracle에서 MongoDB로(다른 DBMS 유형) 마이그레이션하는 것은 대부분 기술적인 결정보다는 정치적인 결정에 의해 동기 부여된다. 특히, 사람들은 RDBMS에서 NoSQL 데이터베이스로 *완전히* 마이그레이션하지 않는다. 그들은 보통 *특정 도메인*만 NoSQL로 구현한다(예: 문서 저장 또는 그래프 순회).

위의 내용이 정말로 당신에게 적용된다고 가정하면(물론, 적용되지 않을 수도 있다): 당신의 RDBMS가 시스템의 중심에 있다면, 시스템 구성 요소 간의 통신을 위해 RDBMS에서 큐를 실행하는 것은 꽤 명백한 선택이지 않은가? 모든 시스템 부분이 이미 데이터베이스에 연결되어 있다. 왜 그렇게 유지하지 않겠는가?

## 결론

여기에 나열된 주장들은 모두 꽤 명백하고 실용적이다. 어느 시점에서, 메시징 요구 사항이 정교한 MQ 시스템과의 통합을 정당화할 만큼 정말로 커지면, 이 주장들은 더 이상 유효하지 않게 된다.

하지만 많은 사람들이 "망치 / 못" 주장에 대해 강한 의견을 가지고 있다. 그 의견들은 옳을 수 있지만 시기상조일 수 있다. 소프트웨어 엔지니어링에서 매우 자주, 하나의 도구만으로 작업하는 것이 전적으로 허용 가능하고 충분하다. 소프트웨어의 망치: RDBMS.
