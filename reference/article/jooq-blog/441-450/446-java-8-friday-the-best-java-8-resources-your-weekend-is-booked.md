# Java 8 금요일: 최고의 Java 8 리소스 - 주말이 꽉 찬다

> 원문: https://blog.jooq.org/java-8-friday-the-best-java-8-resources-your-weekend-is-booked/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 우리가 jOOQ의 fluent API와 쿼리 DSL에 정말 빠져 있기 때문에, Java 8이 우리 생태계에 가져다 줄 것들에 대해 정말 설레고 있습니다.

매주 금요일마다, 우리는 람다 표현식, 메서드 참조, 디폴트 메서드, Streams API 및 기타 훌륭한 것들을 활용하는 튜토리얼 스타일의 멋진 새로운 Java 8 기능들을 몇 가지 보여드리고 있습니다. 소스 코드는 [GitHub](https://github.com/jOOQ/jOOQ-s-Java-8-Goodies)에서 확인하실 수 있습니다.

물론 우리만 Java 8에 대해 글을 쓰는 것은 아닙니다... 이번 Java 8 금요일 시리즈에서는 이 주제에 대해 진행되어 온 최고의 콘텐츠들을 요약해 보고자 합니다.

## Brian Goetz의 Stack Overflow 답변들

[Brian Goetz](https://www.java.net/jcp/communityspotlight/brian-goetz)는 JSR 335의 스펙 리더였습니다. 그의 전문가 그룹 팀과 함께, 그는 Java 8의 성공을 위해 매우 열심히 일해 왔습니다. 그러나 이제 JSR 335가 출시되었다고 해서 그의 일이 끝난 것은 아닙니다. Brian은 [Stack Overflow](https://stackoverflow.com/users/3553087/brian-goetz)에서 Java 커뮤니티의 질문들에 대해 권위 있는 답변을 제공해 주는 친절함을 보여주고 있습니다.

- [인터페이스 메서드에서 "synchronized"가 허용되지 않는 이유는 무엇인가요?](https://stackoverflow.com/q/23453568/521799) - [답변](https://stackoverflow.com/a/23463334/521799)
- [인터페이스 메서드에서 "final"이 허용되지 않는 이유는 무엇인가요?](https://stackoverflow.com/q/23453287/521799) - [답변](https://stackoverflow.com/a/23476994/521799)
- ["Java Concurrency In Practice"가 아직도 유효한가요?](https://stackoverflow.com/q/10202768/521799) - [답변](https://stackoverflow.com/a/10214606/521799)
- [객체가 람다인지 올바르게 판별하는 방법은?](https://stackoverflow.com/q/23870478/521799) - [답변](https://stackoverflow.com/a/23870800/521799)
- [Iterable이 stream()과 parallelStream() 메서드를 제공하지 않는 이유는?](https://stackoverflow.com/q/23114015/521799) - [답변](https://stackoverflow.com/a/23177907/521799)
- [중첩된 Java 8 병렬 스트림 액션 내에서 세마포어를 사용하면 데드락이 발생할 수 있습니다. 이것은 버그인가요?](https://stackoverflow.com/q/23442183/521799) - [답변](https://stackoverflow.com/a/23480863/521799)
- [java.lang.Object의 메서드에 대해 디폴트 메서드를 정의하는 것이 금지된 이유는?](https://stackoverflow.com/q/24016962/521799) - [답변](https://stackoverflow.com/a/24026292/521799)
- [클로저를 비교하는 방법이 있나요?](https://stackoverflow.com/q/24095875/521799) - [답변](https://stackoverflow.com/a/24098805/521799)
- [Java 8 스트림 직렬 vs 병렬 성능](https://stackoverflow.com/q/24027247/521799) - [답변](https://stackoverflow.com/a/24027283/521799)
- [Java 8 JDK를 사용하여 Iterable을 Stream으로 변환](https://stackoverflow.com/q/23932061/521799) - [답변](https://stackoverflow.com/a/23936723/521799)

## Baeldung.com의 Java 8 리소스 모음

이 리소스 목록은 [Baeldung.com](http://www.baeldung.com/java8)의 사람들이 제공하는 매우 유용한 Java 8 리소스 목록(대부분 스펙에 대한 권위 있는 링크들) 없이는 완성되지 않을 것입니다.

## jOOQ 블로그의 Java 8 금요일 시리즈

야호, 바로 우리입니다! :-) 네, 우리는 jOOQ와 Java 8을 통합할 때의 경험에서 얻은 최신 정보를 여러분께 전달하기 위해 열심히 노력해 왔습니다.

- [Streams API 사용 시 10가지 미묘한 실수](https://blog.jooq.org/java-8-friday-10-subtle-mistakes-when-using-the-streams-api/)
- [JavaScript가 Nashorn과 jOOQ로 SQL을 만나다](https://blog.jooq.org/java-8-friday-javascript-goes-sql-with-nashorn-and-jooq/)
- [언어 설계는 미묘하다](https://blog.jooq.org/java-8-friday-language-design-is-subtle/)
- [더 이상 ORM이 필요 없다](https://blog.jooq.org/java-8-friday-no-more-need-for-orms/)
- [레거시 라이브러리들을 폐기하자](https://blog.jooq.org/java-8-friday-lets-deprecate-those-legacy-libs/)
- [간결한 동시성](https://blog.jooq.org/java-8-friday-goodies-lean-concurrency/)
- [Map 개선사항](https://blog.jooq.org/java-8-friday-goodies-map-enhancements/)
- [SQL ResultSet 스트림](https://blog.jooq.org/java-8-friday-goodies-sql-resultset-streams/)
- [잘 알려지지 않은 Java 8 기능: 일반화된 대상 타입 추론](https://blog.jooq.org/a-lesser-known-java-8-feature-generalized-target-type-inference/)
- [Java 8에 아직도 LINQ가 필요한가? 아니면 LINQ보다 나은가?](https://blog.jooq.org/does-java-8-still-need-linq-or-is-it-better-than-linq/)

## ZeroTurnaround의 RebelLabs 블로그

ZeroTurnaround 콘텐츠 마케팅 전략의 일환으로, ZeroTurnaround는 꽤 오래 전에 RebelLabs를 시작했습니다. 그곳에서 다양한 작성자들이 JRebel 및 기타 ZT 제품과 반드시 관련이 있지는 않은 Java 주제에 대한 흥미로운 기사들을 게시합니다. 거기에 게시된 훌륭한 Java 8 관련 콘텐츠가 있습니다.

- [Java 8 디폴트 메서드에 대한 당신의 중독이 판다를 슬프게 하고 동료들을 화나게 할 수 있는 방법!](http://zeroturnaround.com/rebellabs/how-your-addiction-to-java-8-default-methods-may-make-pandas-sad-and-your-teammates-angry/)
- [Java 8이 역대 가장 빠른 JVM인가? Fork-Join의 성능 벤치마킹](http://zeroturnaround.com/rebellabs/is-java-8-the-fastest-jvm-ever-performance-benchmarking-of-fork-join/)
- [Java 8에서 람다로 당신의 세계를 망치지 않는 방법](http://zeroturnaround.com/rebellabs/how-to-avoid-ruining-your-world-with-lambdas-in-java-8/)
- [Java 8의 모나딕 퓨처: 데이터 흐름을 구성하고 콜백 지옥을 피하는 방법](http://zeroturnaround.com/rebellabs/monadic-futures-in-java8/)
- [Java 8로 마이그레이션하면 코드베이스에 어떤 일이 일어나는가: 실제 예시](http://zeroturnaround.com/rebellabs/what-migrating-to-java-8-will-do-to-your-codebase-a-practical-example/)

## Takipi 블로그

ZeroTurnaround와 저희처럼, [Takipi](http://www.takipiblog.com/)의 친구들도 그들의 블로그에서 멋진 Java 8 콘텐츠를 제공합니다.

- [Java 8 StampedLocks vs. ReadWriteLocks 그리고 Synchronized](http://www.takipiblog.com/2014/05/30/java-8-stampedlocks-vs-readwritelocks-and-synchronized/)
- [당신이 들어보지 못한 Java 8의 10가지 기능](http://www.takipiblog.com/2014/04/30/10-features-in-java-8-you-havent-heard-of/)
- [반드시 읽어야 할 15가지 Java 8 튜토리얼](http://www.takipiblog.com/2014/04/09/15-must-read-java-8-tutorials/)
- [Java 8의 새로운 병렬성 API: 화려함과 매력 뒤에 숨겨진 것](http://www.takipiblog.com/2014/04/03/new-parallelism-apis-in-java-8-behind-the-glitz-and-glamour/)
- [Java 8 람다 표현식의 어두운 면](http://www.takipiblog.com/2014/03/25/the-dark-side-of-lambda-expressions-in-java-8/)

## Benji Weber의 재미있는 Java 8 실험들

이 블로그 시리즈는 읽기가 특히 재미있었습니다. [Benji Weber](http://benjiweber.co.uk/blog/)는 정말로 틀을 벗어나 생각하며 디폴트 메서드, 메서드 참조 및 그 모든 것들로 미친 짓을 합니다. Java 개발자들이 지금까지 꿈만 꿀 수 있었던 것들입니다.

- [Nashorn을 사용한 JSON에서 Java 인터페이스로](http://benjiweber.co.uk/blog/2014/05/08/json-to-java-interfaces-with-nashorn/)
- [Java에서의 패턴 매칭](http://benjiweber.co.uk/blog/2014/05/03/pattern-matching-in-java/)
- [Java 값 객체](http://benjiweber.co.uk/blog/2014/04/24/named-classless-java-value-objects/)
- [Java 포워딩-인터페이스 패턴](http://benjiweber.co.uk/blog/2014/04/14/java-forwarding-interface-pattern/)
- [순수 Java 데이터베이스 쿼리에서의 조인](http://benjiweber.co.uk/blog/2014/04/13/joins-in-pure-java-database-queries/)
- [체크 예외와 스트림](http://benjiweber.co.uk/blog/2014/03/22/checked-exceptions-and-streams/)
- [Java 8을 사용한 타입 안전 데이터베이스 상호작용](http://benjiweber.co.uk/blog/2013/12/28/typesafe-database-interaction-with-java-8/)

## Geeks from Paradise 블로그의 Java 8 단상

[Informatech의 Edwin Dalorzo](http://blog.informatech.cr/)는 Java 8과 .NET 사이의 다양하고 근거 있는 비교를 우리에게 선사해 왔습니다. 이것은 특히 Streams와 LINQ를 비교할 때 흥미롭습니다.

- [Java 8에 인터페이스 오염이 있는 이유](http://blog.informatech.cr/2014/04/04/jdk-8-interface-pollution/)
- [Java 8을 사용한 메모이제이션된 피보나치 수](http://blog.informatech.cr/2013/05/08/memoized-fibonacci-numbers-with-java-8/)
- [Java 8 Optional 객체](http://blog.informatech.cr/2013/04/10/java-optional-objects/)
- [Java Streams API 미리보기](http://blog.informatech.cr/2013/03/25/java-streams-api-preview/)
- [Java Streams 미리보기 vs .Net LINQ를 사용한 고차 프로그래밍](http://blog.informatech.cr/2013/03/24/java-streams-preview-vs-net-linq/)

## 이 목록이 완전한가요?

아니요, 다른 많은 매우 흥미로운 블로그 시리즈들이 빠져 있습니다. 공유할 시리즈가 있으신가요? 이 게시물을 업데이트할 준비가 되어 있으니, 댓글 섹션에서 알려주세요.
