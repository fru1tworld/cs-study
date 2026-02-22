# Oracle GlassFish, 또는 왜 오픈 소스에 대해 다시 생각해야 하는가

> 원문: https://blog.jooq.org/oracle-glassfish-or-why-you-should-think-about-open-source-again/

2013년 11월 12일 lukaseder 작성

[Oracle이 최근 JEE의 레퍼런스 구현체인 GlassFish에 대한 상용 서비스 중단을 발표](https://blogs.oracle.com/theaquarium/entry/java_ee_and_glassfish_server)한 것은 JEE 관련 커뮤니티에서 많은 반응을 불러일으켰습니다. 그 반응은 다음과 같습니다:

- [Markus Eisele의 JEE 미래에 대한 다소 비관적인 전망](http://blog.eisele.net/2013/11/rip-glassfish-thanks-for-all-fish.html)
- [Adam Bien의 GlassFish를 GitHub로 옮기자는 건설적인(또는 냉소적인? Adam Bien의 경우 절대 확신할 수 없습니다) 제안](http://www.adam-bien.com/roller/abien/entry/what_oracle_could_do_for)
- [Tomitribe의 오픈 소스가 실제로 무엇인지에 대한 실용적인 검토](http://www.tomitribe.com/blog/2013/11/feed-the-fish/)
- [Stephen Colebourne의 "다리 하나를 제거하면 전체가 흔들린다"는 주장](https://twitter.com/jodastephen/status/397675833759772672)
- [Eberhard Wolff, Oliver Gierke, Stefan Tilkov, Markus Eisele, Anton Arhipov 사이의 많은 흥미로운 트윗들](https://twitter.com/ewolff/status/399148112750850048)
- [Bruno Borges가 Oracle 관점에서 몇 가지 사항을 명확히 한 권위 있는 견해](https://blogs.oracle.com/brunoborges/entry/6_facts_about_glassfish_announcement)

이 사건은 위의 많은 사람들이 우리 커뮤니티의 핵심 플레이어이자 영향력 있는 인물들이고, Oracle의 이 조치가 JEE의 미래에 무엇을 의미하는지에 대해 동의하지도 않고 알지도 못하기 때문에 전체 Java 생태계에 큰 영향을 미치는 것 같습니다.

위의 모든 것들 중에서 제 생각에 가장 흥미로운 관점은 [tomitribe의 것](http://www.tomitribe.com/blog/2013/11/feed-the-fish/)으로, 오픈 소스에 대한 순수한 비즈니스 관점에서 바라보고 있습니다. 그들은 이렇게 말합니다:

> 오픈 소스는 무료가 아니다

다른 말로 하면, *["공짜 점심 같은 건 없다"](https://en.wikipedia.org/wiki/There_ain't_no_such_thing_as_a_free_lunch)*입니다. 그리고 tomitribe를 더 인용하자면, 그들이 제시하는 매우 흥미로운 생각은 이것입니다:

> 이것이 저에게 말해주는 것은 우리 산업이 여전히 오픈 소스를 완전히 이해하지 못하고 있다는 것입니다.

우리는 분명히 오픈 소스를 이해하지 못합니다. 저 자신도 오픈 소스 소프트웨어 벤더입니다. 저는 오픈 소스가 다음과 같다고 믿습니다:

훌륭한 마케팅 도구

사람들은 오픈 소스를 "일반적으로 좋은 것"으로 봅니다. 제가 [jOOQ](https://www.jooq.org)에 대해 컨퍼런스에서 이야기했을 때, 그것이 완전한 오픈 소스 소프트웨어였을 때([아직 이중 라이선스가 아닐 때](https://blog.jooq.org/jooq-3-2-offering-commercial-licensing-and-support/)), 저는 무료 광고를 할 기회를 많이 얻었습니다. 이것은 제가 대안적인 상용 라이선스를 제공하기 시작하면서 급격히 바뀌었습니다.

좋은 도구 활성화 수단

저는 다음에 무료로 접근할 수 있습니다:

- [GitHub](https://github.com)나 [BitBucket](https://bitbucket.org) 같은 소스 컨트롤
- [SourceForge](https://sourceforge.net)나 [Maven Central](http://search.maven.org) 같은 배포 채널
- [YourKit](http://www.yourkit.com/), [Atlassian](https://www.atlassian.com) 라이선스
- 그 외 많은 것들...

여기서도 마찬가지입니다. 제가 이제 "상용" 소프트웨어 벤더가 되면서, 일부 도구들은 더 이상 저에게 접근 가능하지 않습니다.

### 진실은: 오픈 소스는 비즈니스 전략이다

정말 그렇습니다. 그리고 그것은 [RedHat](http://www.redhat.com/)이나 [Pivotal](http://www.gopivotal.com/)에게 과거에 잘 작동한 것 같습니다. 다른 누구에게도 효과가 있었을까요? 우리는 아직 모릅니다. 대부분의 다른 대기업들은 "전통적인" 분야에서 엄청난 양의 수익을 내기 때문에 단순히 오픈 소스를 "감당할 수" 있기 때문입니다. 사실, 그들은 오픈 소스에 인력과 혁신을 투자하는 데 너무 뛰어나서, [Weblogic](https://www.oracle.com/de/products/middleware/cloud-app-foundation/weblogic/overview/index.html)이나 [Websphere](http://www-01.ibm.com/software/ch/de/websphere/)보다 더 나은 더 완전한 JEE 구현체를 작성하기 어렵기 때문에 상업적 경쟁을 억제합니다.

분명히, [Larry Ellison조차도 데이터 센터의 미래가 범용 머신 사용에 있다](http://medianetwork.oracle.com/video/player/2685494045001)는 것에 동의한다고 합니다. 동시에, [RedHat은 Oracle에게 "무료를 시도해 보라"고 제안합니다](http://readwrite.com/2013/09/09/red-hat-oracle-have-you-tried-free).

GlassFish의 상용 지원 중단이 JEE에 미치는 영향이 무엇이든, 우리는 이 대규모 ["프리미엄"](https://en.wikipedia.org/wiki/Freemium) 모델이 우리 세계에 어떤 영향을 미칠지 완전히 이해하기 시작한 단계에 불과합니다. 이것은 단지 소프트웨어 산업에 관한 것만이 아닙니다. 전체 인터넷이 우리에게 "무료" 것들을 가져다주었습니다. 우리는 다음을 얻습니다:

- "무료" 표준 (W3C, IETF 표준을 ISO 표준과 비교해 보세요!)
- "무료" Facebook과 Twitter와 GMail 계정
- "무료" 신문
- "무료" 음악과 영화
- 모든 종류의 일에 대한 "무료" 상품 서비스
- 저임금 국가로 무엇이든 오프쇼어할 수 있기 때문에 "무료" 노동력

이것은 최근 ["We Learn Nothing"의 저자 Tim Kreider](http://mobile.nytimes.com/2013/10/27/opinion/sunday/slaves-of-the-internet-unite.html)에 의해 다뤄졌는데, 그는 New York Times에 "무료 글"을 쓰는 것이 어떻게 *"노출"*을 구축하는 데 도움이 되는지, 그리고 이 모든 힘든 저널리즘 작업이 더 이상 돈이 되지 않기 때문에 그것이 어떻게 그냥 말도 안 되는 것인지를 묘사합니다.

*"노출"*을 구축한다는 것이 뭔가 떠오르시나요?

네, 저는 [GitHub](https://github.com/)에서 무료 오픈 소스를 작성하고, [Stack Overflow](https://stackoverflow.com/)에서 복잡한 질문에 무료로 답변함으로써 *"노출"*을 구축할 수 있습니다. 저는 개인적으로 [jOOQ](https://www.jooq.org)를 광고하기 위해 두 도구를 모두 사용합니다, 의심의 여지 없이. 그래서 저는 서비스(콘텐츠)에 대한 서비스(광고)를 얻습니다. 제 거래는 저에게 공정해 보입니다. 하지만 수많은 GitHub와 Stack Overflow 사용자들이 기여합니다... 그냥 기여하기 위해서. 누구에게? GitHub와 Stack Overflow에게. 그리고 왜? 저는 모르겠습니다.

그래서, Oracle이 [MySQL](https://dev.mysql.com/), [Hudson](http://hudson-ci.org/), 그리고 [Sun](https://www.oracle.com/us/sun/index.html)에서 물려받은 다른 제품들에서 이전에 그랬던 것처럼 지원을 줄이고 관심을 느슨하게 하기 시작하면 GlassFish에 기여해야 할까요?

[Karl Marx](https://en.wikipedia.org/wiki/Karl_Marx)가 이미 우리에게 가르쳐 준 것을 기억합시다. 우리의 자본주의 개념은 필연적으로 우리를 다음으로 이끌 것입니다 ([Wikipedia](https://en.wikipedia.org/wiki/Karl_Marx)에서 인용):

- 기술 진보
- 생산성 향상
- 성장
- 합리성
- 과학 혁명

절대적으로! 전 세계의 수많은 소프트웨어 개발자들이 더 나은 도구들(성장, 진보)을 아무것도 아닌 것보다 더 많지 않게... *무료로* 생산하는 것보다 생산성이 더 좋아질 수 있는 방법은 없습니다!

### 그래서, 다른 사람들의 오픈 소스 전략의 졸이 되지 마세요

그러니까, Oracle이 JEE의 오픈 소스 레퍼런스 구현체 지원에서 물러나는 것이 무엇을 의미하는지 곰곰이 생각하는 대신, 직접 적극적이 되세요! 맹목적으로 오픈 소스를 소비하지 말고, *당신의* 특정 필요에 따라 오픈 소스나 상용 소프트웨어를 의식적으로 선택함으로써 다른 옵션과 같은 하나의 옵션으로 만드세요.

그러한 광고에서 당신 자신의 이익을 얻지 않는 한, 컨퍼런스에서 *그들의* 멋진 제품을 *무료로* 광고하는 것을 그만두세요. 오픈 소스는 그저 또 다른 비즈니스 모델일 뿐입니다.
