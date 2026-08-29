# Netty 관련 글과 프로젝트

## Netty 관련 글 (Related Articles)

> 원본: https://netty.io/wiki/related-articles.html

---

새 글이 위에 오도록 게시일 기준 내림차순 정렬 · 중복 항목 없어야 함.

### 2025

- **bin的技术小屋**가 작성한 **Netty 소스 코드 분석** 시리즈, **4.1.112.Final 및 4.1.56.Final** 기준.

    - [《时间轮在 Netty , Kafka 中的设计与实现》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247490510&idx=1&sn=3271290a590cb81f11841c3f62c949c7&chksm=ce77dd89f900549fadfd6f7f6703ef10f7736b02c2940fc3fcc31e87b1747c7ee1aa4add627d&scene=178&cur_album_id=2217816582418956300#rd) — "Netty와 Kafka에서의 time wheel 설계와 구현"

    - [《Netty 如何自动探测内存泄露的发生》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247490390&idx=1&sn=617e9304df9c2c9c9d9da882f0759dcc&chksm=ce77dd11f90054078dc7a9145e982194f102954e9727b8fc104e76d2cbf02a6e88bf9564e0e6&scene=178&cur_album_id=2217816582418956300#rd) — "Netty는 자원 누수를 어떻게 자동으로 검출하는가?"

    - [《谈一谈 Netty 的内存管理 —— 且看 Netty 如何实现 Java 版的 Jemalloc》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247490143&idx=1&sn=b85a1fe4be6578af8e968ef99fd6dd33&chksm=ce77dc18f900550ee7aa39c8e30c46eaabe984adf82c88a85abec5fd62d3cdf03af6928d9459&scene=178&cur_album_id=2217816582418956300#rd) — "Jemalloc4 기반의 Netty 메모리 관리"

    - [《聊一聊 Netty 数据搬运工 ByteBuf 体系的设计与实现》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247489860&idx=1&sn=f3cc139194355de6fb7463fac05a7263&chksm=ce77df03f900561506eaf41a17777ec5f8dadbb37594a60b466238eb718c9cce38ad6bf2263f&scene=178&cur_album_id=2217816582418956300#rd) — "ByteBuf의 설계와 구현"

    - [《Netty 优雅停机的设计与实现》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247485290&idx=1&sn=58d7eb2c25c2e51b41e35a4ce4dab517&chksm=ce77c12df900483bf5a0edc6b31e710d6e3019deeeb788ed89faa69f9055872e85d518ee52f6&scene=178&cur_album_id=2217816582418956300#rd) — "Netty의 graceful shutdown 설계와 구현"

    - [《且看 Netty 如何应对 TCP 连接的正常关闭，异常关闭，半关闭场景》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247485060&idx=1&sn=736360af6eb3a4db496de2d6665ebd3c&chksm=ce77c0c3f90049d5e44692c2cf837e8d85bb28758b243d505c43c48ce703da1edadfc19360b1&scene=178&cur_album_id=2217816582418956300#rd) — "Netty가 TCP 연결의 정상 종료, 비정상 종료, 반종료 상황을 어떻게 처리하는가?"

    - [《一文聊透 Netty IO 事件的编排利器 pipeline | 详解所有 IO 事件的触发时机以及传播路径》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484823&idx=1&sn=9396fb0f5dbac5e32d0fa1129d385fbc&chksm=ce77c3d0f9004ac678283b7d178835e740eb5e5b80740cc624ae7a307be7a7eb0c7766842878&scene=178&cur_album_id=2217816582418956300#rd) — "pipeline의 설계와 구현"

    - [《一文搞懂Netty发送数据全流程 | 你想知道的细节全在这里》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484532&idx=1&sn=c3a8b37a2eb09509d9914494ef108c68&chksm=ce77c233f9004b25a29f9fdfb179e41646092d09bc89df2147a9fab66df13231e46dd6a5c26d&scene=178&cur_album_id=2217816582418956300#rd) — "Netty가 데이터를 보내는 전체 과정 분석, 알고 싶은 모든 디테일이 여기에"

    - [《详解 Recycler 对象池的精妙设计与实现》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484419&idx=1&sn=3a75a495f0f117cca1548da1e0f3e6e6&chksm=ce77c244f9004b52d74b8ffce149ea64e5a8f0ba6713b105d003fdaef8eaa9ad3a5a65d20427&scene=178&cur_album_id=2217816582418956300#rd) — "Recycler 객체 풀의 설계와 구현"

    - [《Netty 如何高效接收网络数据？一文聊透 ByteBuf 动态自适应扩缩容机制》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484244&idx=1&sn=831060fc38caa201d69f87305de7f86a&chksm=ce77c513f9004c05b48f849ff99997d6d7252453135ae856a029137b88aa70b8e046013d596e&scene=178&cur_album_id=2217816582418956300#rd) — "Netty가 네트워크 데이터를 효율적으로 수신하는 방법과 ByteBuf의 동적 적응 확장·축소 메커니즘"

    - [《Netty 是如何高效接收网络连接》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484184&idx=1&sn=726877ce28cf6e5d2ac3225fae687f19&chksm=ce77c55ff9004c493b592288819dc4d4664b5949ee97fed977b6558bc517dad0e1f73fab0f46&scene=178&cur_album_id=2217816582418956300#rd) — "Netty가 네트워크 연결을 효율적으로 수신하는 방법"

    - [《一文聊透 Netty 核心引擎 Reactor 的运转架构》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484087&idx=1&sn=0c065780e0f05c23c8e6465ede86cba0&chksm=ce77c4f0f9004de63be369a664105708bc5975b52993f4a6df223caed34cc1ef6185a16acd75&scene=178&cur_album_id=2217816582418956300#rd) — "Netty 핵심 엔진 Reactor(EventLoop)의 동작 아키텍처"

    - [《详细图解 Netty Reactor 启动全流程》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247484005&idx=1&sn=52f51269902a58f40d33208421109bc3&chksm=ce77c422f9004d340e5b385ef6ba24dfba1f802076ace80ad6390e934173a10401e64e13eaeb&scene=178&cur_album_id=2217816582418956300#rd) — "Netty Reactor(이벤트 루프) 시작 전체 과정"

    - [《Reactor 在 Netty 中的实现(创建篇)》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247483907&idx=1&sn=084c470a8fe6234c2c9461b5f713ff30&chksm=ce77c444f9004d52e7c6244bee83479070effb0bc59236df071f4d62e91e25f01715fca53696&scene=178&cur_album_id=2217816582418956300#rd) — "Netty에서의 Reactor IO 모델 구현"

    - [《从内核角度看 Netty IO模型》](https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247483737&idx=1&sn=7ef3afbb54289c6e839eed724bb8a9d6&chksm=ce77c71ef9004e08e3d164561e3a2708fc210c05408fa41f7fe338d8e85f39c1ad57519b614e&scene=178&cur_album_id=2217816582418956300#rd) — "리눅스 커널 관점에서 보는 Netty IO 모델"

### 2024

* [Netty](https://speakerdeck.com/sullis/netty-chicago-java-user-group-2024-04-17) — Chicago Java User Group 발표 자료, Sean Sullivan

### 2023

* [Netty, the IO framework that propels them all](https://www.youtube.com/watch?v=NvnOg6g4114) — Devoxx Belgium 발표, Stephane Landelle

### 2022

* [Getting started with Netty 4](http://www.mastertheboss.com/jboss-frameworks/netty/jboss-netty-tutorial) — Francesco Marchioni

### 2021

* [Netty data model, threading, and gotchas](https://medium.com/@akhaku/netty-data-model-threading-and-gotchas-cab820e4815a) — Ammar Khaku

### 2018

* [Need for speed: Boosting Apache Cassandra's performance using Netty](https://www.youtube.com/watch?v=ZXytCqujbwE) — Devoxx Ukraine 2018 발표, Dinesh Joshi
* 闪电侠가 작성한 Netty 시리즈 (중국어)

  * [netty源码分析之揭开reactor线程的面纱（一）](https://www.jianshu.com/p/0d0eece6d467) "Reactor 스레드 모델 소스 코드 분석 (1)"
  * [netty源码分析之揭开reactor线程的面纱（二）](https://www.jianshu.com/p/467a9b41833e) "Reactor 스레드 모델 소스 코드 분석 (2)"
  * [netty源码分析之揭开reactor线程的面纱（三）](https://www.jianshu.com/p/58fad8e42379) "Reactor 스레드 모델 소스 코드 분석 (3)"
  * [netty源码分析之服务端启动全解析](https://www.jianshu.com/p/c5068caab217) "서버 시작 소스 코드 분석"
  * [netty源码分析之新连接接入全解析](https://www.jianshu.com/p/0242b1d4dd21) "새 연결 수락 소스 코드 분석"
  * [netty源码分析之pipeline(一)](https://www.jianshu.com/p/6efa9c5fa702) "pipeline 소스 코드 분석 (1)"
  * [netty源码分析之pipeline(二)](https://www.jianshu.com/p/087b7e9a27a2) "pipeline 소스 코드 분석 (2)"
  * [netty源码分析之writeAndFlush全解析](https://www.jianshu.com/p/feaeaab2ce56) "writeAndFlush() 소스 코드 분석"
  * [netty源码分析之拆包器的奥秘](https://www.jianshu.com/p/dc26e944da95) "ByteToMessageDecoder 소스 코드 분석"
  * [netty源码分析之LengthFieldBasedFrameDecoder](https://www.jianshu.com/p/a0a51fd79f62) "LengthFieldBasedFrameDecoder 소스 코드 분석"
  * [netty 堆外内存泄露排查盛宴](https://www.jianshu.com/p/4e96beb37935) "netty-socketio 사용 시의 메모리 누수 트러블슈팅"

### 2016

* Laurent Caillette가 작성한 Netty 시리즈 (프랑스어)

  * [Netty (1/6)](https://groups.google.com/d/msg/techos/7OhdFbsHOjI/RZs1Ncs0EAAJ)
  * [Netty (2/6) : Modèle de programmation](https://groups.google.com/d/msg/techos/BArGTlHcgu0/VKdJdz12EAAJ) "프로그래밍 모델"
  * [Netty (3/6) : Recyclage de la mémoire](https://groups.google.com/d/msg/techos/pPKiuxd3RkU/-vG78bDHEAAJ) "메모리 재활용"
  * [Netty (4/6) : Que faire avec](https://groups.google.com/d/msg/techos/BZxM5IKKXXk/iGQwqgwqEQAJ) "무엇을 할 수 있나"
  * [Netty (5/6) : Étude de cas ; existant](https://groups.google.com/d/msg/techos/y5p_y8fHdYA/5aHXc4BuEQAJ) "사례 연구"
  * [Netty (6/6) : Constatations ; conclusion](https://groups.google.com/d/msg/techos/CGGil5VK4oQ/6DWDc4puEQAJ) "관찰과 결론"

### 2015

* [Netty at Layer: Powering Client API](https://medium.com/@mubarak.seyed/netty-at-layer-powering-client-api-2a6ca3617d48) — Mubarak Seyed
* [Whirlpool: Microservices Using Netty And Kafka](https://keyholesoftware.com/2016/05/24/whirlpool-microservices-using-netty-and-kafka/) — John Boardman
* [Netty: A Different Kind of Web(Socket) Server](https://keyholesoftware.com/2015/03/16/netty-a-different-kind-of-websocket-server/) — John Boardman

### 2014

* [Real-time collaborative authoring in AEM with WebSockets and Netty](http://mszu.github.io/rtca-aem/) (슬라이드) — Mark Szumowski
* [High Performance Services in Scala](http://www.youtube.com/watch?v=THcAwoA5G2w) (영상) — Greg Soltis
* [Multithreaded UDP server with Netty on Linux](http://marrachem.blogspot.com/2014/09/multi-threaded-udp-server-with-netty-on.html) — Mikom
* [Real time analytics with Netty, Storm, Kafka](http://www.slideshare.net/tantrieuf31/real-time-analytics-with-netty-storm-kafka) (슬라이드) — Trieu Nguyen
* [State of the Art JVM Networking with Netty 4](https://speakerdeck.com/daschl/state-of-the-art-jvm-networking-with-netty-4) (슬라이드) — Michael Nitschinger, JAX 2014
* [Netty 4 - Intro → Changes → HTTP → Lessons learned](http://normanmaurer.me/presentations/2014-http-netty/slides.html) — Norman Maurer
* [Why Netty](http://normanmaurer.me/presentations/2014-netflix-netty/slides.html) (슬라이드) — Norman Maurer at Netflix
* [Netty best practices](http://normanmaurer.me/presentations/2014-twitter-meetup-netty/slides.html) (슬라이드 + [영상](https://www.youtube.com/watch?v=WsMOJqAYW5M)) — Norman Maurer at #NettyMeetup
* [Netty at Facebook](https://www.youtube.com/watch?v=zK2A9gWKxIE) (영상) — Andrew Cox at #NettyMeetup
* [State of Netty](https://speakerdeck.com/trustin/state-of-netty) (슬라이드 + [영상](https://www.youtube.com/watch?v=0aoeSsKarc8)) — Trustin Lee at #NettyMeetup
* [Netty at Twitter with Finagle](https://www.youtube.com/watch?v=HJP_108i0ik) (영상) — Evan Meagher at #NettyMeetup
* [Netty server implementation: Part 1](https://www.youtube.com/watch?v=5CHc0hEhKbM) and [Part 2](https://www.youtube.com/watch?v=Xwa_JYtI5-M) (영상) — Christian Tucker
* [Netty and a Fresh View of Web Development](http://www.iteratia.com/blog/netty-and-a-fresh-view-of-web-development/) — Iteratia Corporation
* [Moving on to Netty 4](http://bhardwajgaurav.wordpress.com/2014/04/20/n/) — Gaurav Bhardwaj
* [Netty best practices](https://www.youtube.com/watch?v=_GRIyCMNGGI) (영상 + [슬라이드](http://normanmaurer.me/presentations/2014-facebook-eng-netty/slides.html)) — Norman Maurer at Facebook
* [Netty 4 - A lookup behind the scenes](http://normanmaurer.me/presentations/2014-eclipsecon-na-netty/slides.html) — Norman Maurer
* [Netty: Basics for beginners](http://bhardwaj-gaurav.blogspot.com/2014/02/netty-basics-for-beginners.html) — Gaurav Bhardwaj
* [Netty at Twitter with Finagle](https://blog.twitter.com/2014/netty-at-twitter-with-finagle) — Jeff Smick
* [Making HTTP content compression work in Netty 4](http://andreas.haufler.info/2014/01/making-http-content-compression-work-in.html) — Andreas Haufler
* [Bridging Netty, RestEasy, and Weld](http://john-ament.blogspot.com/2014/01/bridging-netty-resteasy-and-weld.html) — John Ament

### 2013

* [Let's do our own full blown HTTP server with Netty 4](http://adolgarev.blogspot.com/2013/12/lets-do-our-own-full-blown-http-server.html) — Aleksandr Dolgaryev
* [Netty 4 - Network - Application development the easy way](http://normanmaurer.me/presentations/2013-wjax-netty/) — Norman Maurer (WJAX 2013)
* [Building scalable network application with Netty](http://www.slideshare.net/JaapterWoerds/netty-jfall) — Jaap ter Woerds
* [Making Storm Fly with Netty](http://yahooeng.tumblr.com/post/64758709722/making-storm-fly-with-netty) — Robert Evans
* [Netty 4 at Twitter: Reduced GC Overhead](https://blog.twitter.com/2013/netty-4-at-twitter-reduced-gc-overhead) — Trustin Lee
* [What is Netty?](http://ayedo.github.io/netty/2013/06/19/what-is-netty.html) — Alon Dolev
* [Creating a multiplayer HTML5 game using Websockets and Netty 4](http://nerdronix.blogspot.com/2013/06/creating-multiplayer-game-using-html-5.html) — Abraham Menacherry
* [Configuring Netty 4 with spring](http://nerdronix.blogspot.com/2013/06/netty-4-configuration-using-spring-maven.html) — Abraham Menacherry
* [Framework Benchmarks Round 2](http://www.techempower.com/blog/2013/04/05/frameworks-round-2/) — TechEmpower.com
* [Framework Benchmarks](http://www.techempower.com/blog/2013/03/28/framework-benchmarks/) — TechEmpower.com
* [Use Netty to proxy your requests](http://www.mastertheboss.com/netty/use-netty-to-proxy-your-requests) — MasterTheBoss.com
* [Never `awaitUninterruptibly()` on Netty Channels](http://nitschinger.at/Never-await-Uninterruptibly-on-Netty-Channels) — Michael Nitschinger
* [Netty 4 presentation](http://de.slideshare.net/normanmaurer/netty4) (슬라이드) — Norman Maurer
* [Reaching 200K events/sec](http://aphyr.com/posts/269-reaching-200k-events-sec) — Kyle Kingsbury
* [Routing Web Service Done Quickly with Scala and Netty](http://blog.sustainablesoftware.com.au/2013/01/21/routing-web-service-done-quickly-with-scala-and-netty/) — Sustainable Software
* [Netty channels and Scala implicits](http://uberblo.gs/2013/01/netty-channels-with-scala-implicits) — Franz Bettag

### 2012

* [Distributing data from cloud to more than 100 million users](http://www.youtube.com/watch?v=xRmh65mE1Qc) (영상 + [슬라이드](http://java.cz/dwn/1003/70613_cloud_distr_nc.pdf)) — Zbyněk Šlajchrt
* [Liftweb and Netty for Web development](http://www.slideshare.net/theoengland/liftweb-and-netty-for-web-developmentkey) — Franz Bettag
* [Benchmarking web frameworks for games](http://blog.juiceboxmobile.com/2012/11/20/benchmarking-web-frameworks-for-games/) — Jason McGuirk
* [Asynchronous & non-blocking Scala: a look at Netty and NIO](http://vimeo.com/53402471) (영상) — Brendan McAdams
* [Interview about Netty 4](http://www.youtube.com/watch?v=VBOvIxXITDM) (영상, 독일어) — Norman Maurer
* [Run Servlets with Netty](http://www.jroller.com/agoubard/entry/run_servlets_with_netty#.USMIPDWNKUo) — Anthony Goubard
* [High performance network programming on the JVM](http://www.slideshare.net/eonnen/high-performance-network-programming-on-the-jvm-oscon-2012) — Erik Onnen
* [How we got SPDY working with Netty 3.5 for Socko](http://sockoweb.org/2012/06/19/spdy-netty.html) — Vibul Imtarnasan
* [Netty tutorial part 1.5: On ChannelHandlers and channel options](http://seeallhearall.blogspot.com/2012/06/netty-tutorial-part-15-on-channel.html) — nickman
* [Netty tutorial part 1: Introduction to Netty](http://seeallhearall.blogspot.com/2012/05/netty-tutorial-part-1-introduction-to.html) — nickman
* [Using SPDY and HTTP transparently using Netty](http://www.smartjava.org/content/using-spdy-and-http-transparently-using-netty) — Jos Dirksen
* [Fun with Netty and Async I/O](http://timboudreau.com/blog/Fun_with_Netty_and_Async_IO/read) — Tim Boudreau

### 2011

* [Zero-copy event-driven servers with Netty](http://de.slideshare.net/danbim/zerocopy-eventdriven-servers-with-netty) (슬라이드) — Daniel Bimschas
* [Multiplayer tic-tac-toe using WebSocket, Netty, and JQuery](http://kevinwebber.ca/blog/2011/11/2/multiplayer-tic-tac-toe-in-java-using-the-websocket-api-nett.html) — Kevin Webber
* [Non-blocking I/O with Netty](http://www.slideshare.net/zaubersoftware/non-blocking-io-with-netty) (스페인어) — Mariano Cortesi, Fernando Zunino
* [The Node redumption!](http://blog.creapptives.com/post/9924551244/the-node-redumption) (Node.js vs Netty) — maxpert
* [How to: basic Netty server](http://www.youtube.com/watch?v=aI_bvkT94sA) (영상) — TheProgrammingTuts
* [Twitter search is now 3x faster](http://engineering.twitter.com/2011/04/twitter-search-is-now-3x-faster_1656.html) — Krishna Gade
* [Clojure: Web Socket introduction](http://blog.jayfields.com/2011/02/clojure-web-socket-introduction.html) — Jay Fields

### 2010

* [Netty `externalResources()` hangs](http://biasedbit.com/netty-releaseexternalresources-hangs/) — Bruno de Carvalho
* [A Simple Netty HTTP Server in Clojure](http://eigenjoy.com/2010/07/30/a-simple-netty-http-server-in-clojure/) — Nate Murray
* [512,000 concurrent websockets with Groovy++ and Gretty](http://groovy.dzone.com/articles/512000-concurrent-websockets) — Alex Tkachman
* [Handshaking tutorial with Netty](http://biasedbit.com/handshaking-tutorial-with-netty/) — Bruno de Carvalho
* [Do we really need Servlet Containers always? (5 parts)](http://thesoftwarekraft.blogspot.com/2010/07/do-we-really-need-servlet-containers.html) — Archanaa Panda
* [How Netty helped me build a highly scalable HTTP server over GPRS](http://thesoftwarekraft.blogspot.com/2010/07/how-jboss-netty-helped-me-build-highly.html) — Archanaa Panda

### 2009

* [Performance Comparison of Apache MINA and JBoss Netty Revisited](http://blog.toadhead.net/index.php/2009/11/25/performance-comparison-of-apache-mina-and-jboss-netty-revisited/) — Mike Heath
* [Writing Your Own Netty Transport](http://rafaelmarins.com/pub/writing-your-own-netty-transport) — Rafael Marins
* [Plurk Comet: Handling 100,000+ Concurrent Connections with Netty](http://amix.dk/blog/post/19456) — Amir Salihefendic
* [Netty: Custom Pipeline Factory](http://www.znetdevelopment.com/blogs/2009/04/23/netty-custom-pipeline-factory/) — Nicholas Hagen
* [Netty: Using Handlers](http://www.znetdevelopment.com/blogs/2009/04/21/netty-using-handlers/) — Nicholas Hagen
* [Scalable NIO Servers (4 parts)](http://www.znetdevelopment.com/blogs/2009/04/07/scalable-nio-servers-part-1-performance/) — Nicholas Hagen
* [Performance Comparison of Apache MINA and JBoss Netty](http://blog.toadhead.net/index.php/2009/03/03/performance-comparison-of-apache-mina-and-jboss-netty/) — Mike Heath

### 2008

* [(N)IO Frameworks in Java](http://www.ashishpaliwal.com/blog/2008/10/nio-frameworks-in-java/) — Ashish Paliwal

---

## Netty 관련 프로젝트 (Related Projects)

> 이 문서는 Netty 공식 Wiki의 "Related projects" 페이지를 한국어로 번역한 문서.
> 원본: https://netty.io/wiki/related-projects.html

---

* [Akka](http://akka.io/) — actor 모델과 software transactional memory를 활용해 확장성·내결함성·동시성·원격 호출을 간단하게 구현할 수 있는 Scala 기반 플랫폼.
* [Apache BookKeeper](http://bookkeeper.apache.org/) — 확장 가능하고 내결함성이 있으며 저지연인 로그 저장소.
* [Apache HBase](http://hbase.apache.org/) — [Hadoop](https://hadoop.apache.org/) 데이터베이스, 분산·확장 가능한 빅데이터 저장소.
* [Apache Cassandra](http://cassandra.apache.org/) — 컬럼 지향 분산 데이터베이스.
* [Apache Flink](https://flink.apache.org) — 분산·상태 보존 스트림 처리 프레임워크.
* [Apache James Server](http://james.apache.org/server) — SMTP, POP3, IMAP, NNTP를 통합한 모듈러 이메일 서버 플랫폼.
* [Apache Pulsar](https://pulsar.incubator.apache.org/) — 오픈소스 분산 pub-sub 메시징 시스템.
* [Apache Spark](http://spark.apache.org) — "빅데이터" 애플리케이션에 흔히 쓰이는 빠르고 범용적인 클러스터 컴퓨팅 프레임워크.
* [Apache Tajo](http://tajo.apache.org) — 분산·내결함성·저지연·고처리량 SQL 엔진. 대규모 데이터셋의 ETL과 ad-hoc 쿼리 처리를 제공.
* [Arquillian](http://www.jboss.org/arquillian.html) — JVM을 위한 혁신적인 in-container 테스트 플랫폼.
* [Async HTTP Client](https://github.com/AsyncHttpClient/async-http-client) — HTTP 요청을 실행하고 응답을 비동기로 처리할 수 있는 사용하기 쉬운 라이브러리.
* [Atomix](http://atomix.io/atomix/) — Raft 합의 알고리즘 기반의 내결함성 분산 시스템 조정용 이벤트 기반 프레임워크.
* [BungeeCord](http://www.spigotmc.org/threads/392/) — 여러 Minecraft 서버를 클라우드/허브 시스템으로 결합하는 사실상 표준 프록시 솔루션.
* [ClusterVAS](https://gitlab.com/opentoolset/clustervas) — OpenVAS 인스턴스를 원격으로 관리하는 도커 기반 노드 매니저·오케스트레이터 SDK. 통신 레이어는 Netty 기반의 Netty Agents로 구축되어 있음.
* [Copycat](http://atomix.io/copycat/) — Raft 합의 알고리즘 기반의 내결함성 상태 머신 복제 프레임워크.
* [Couchbase](http://www.couchbase.com) — 인터랙티브 애플리케이션에 최적화된 분산 NoSQL 문서 지향 데이터베이스.
* [drasyl](https://drasyl.org) — 분산 애플리케이션을 빠르게 개발하기 위한 고성능 프레임워크.
* [Elasticsearch](http://www.elasticsearch.org) — Lucene 위에 만든 분산 RESTful 검색 엔진.
* [Eucalyptus](http://open.eucalyptus.com) — 기존 IT 인프라를 수정·전용 하드웨어·재구성 없이 활용하여 온프레미스 클라우드 컴퓨팅을 구현하는 소프트웨어 인프라.
* [Finagle](http://twitter.github.io/finagle/) — 고동시성 서버를 만드는 데 사용되는 JVM용 확장 가능 RPC 시스템.
* [Forest](https://github.com/le-moulin-studio/forest) — 범용 friend-to-friend 플랫폼.
* [Gatling](https://gatling.io) — Netty와 Akka로 개발된 비동기·효율적 부하 테스트 도구.
* [gRPC](https://grpc.io) — 고성능 오픈소스 범용 RPC 프레임워크.
* [Hammersmith](https://github.com/bwmcadams/hammersmith) — Scala용 순수 비동기 MongoDB 드라이버.
* [Higgs](https://github.com/zcourts/higgs) — 고성능 메시지 지향 네트워크 라이브러리.
* [Holmes](http://ccheneau.github.com/Holmes/) — 호환 기기로 영상·음악·사진·팟캐스트(RSS)를 재생하기 위한 DLNA/UPnP 프로토콜을 구현한 자바 애플리케이션.
* [HornetQ](http://www.jboss.org/hornetq) — 다중 프로토콜을 지원하고 임베드 가능한 고성능 클러스터 비동기 메시징 시스템.
* [http-client](https://github.com/brunodecarvalho/http-client) — 고성능·고처리량 지향 HTTP 클라이언트 라이브러리.
* [Infinispan](http://www.jboss.org/infinispan) — 매우 확장 가능하고 고가용성을 갖춘 데이터 그리드 플랫폼.
* [jaC64](http://code.google.com/p/jac64-op/) — 멀티플레이어 지원 C64 에뮬레이터.
* [Jagornet DHCP](https://github.com/jagornet/dhcp) — Netty 기반 자바로 작성된 표준 준수 오픈소스 DHCPv4/DHCPv6 서버.
* [jasync-sql](https://github.com/jasync-sql/jasync-sql) — Kotlin으로 작성된 Netty 기반 비동기 MySQL/PostgreSQL 드라이버.
* [JBossWS](http://www.jboss.org/jbossws) — JAX-WS 호환 풍부 기능 웹 서비스 스택.
* [Jetserver](https://github.com/menacher/java-game-server) — JBoss Netty와 Jetlang 기반의 고속 멀티플레이어 자바 게임 서버. TCP·UDP 전송과 Flash AMF3 프로토콜을 지원.
* [JXTA](http://jxta.kenai.com) — 휴대폰·무선 PDA에서 PC·서버까지 네트워크상의 어떤 연결된 기기든 P2P 방식으로 통신·협업할 수 있게 하는 오픈 프로토콜 집합.
* [Ldap4j](https://github.com/adaptiverecognition/ldap4j) — Netty를 전송으로 사용할 수 있고 Netty 코덱으로도 사용할 수 있는 LDAP 클라이언트.
* [Ldaptive](https://www.ldaptive.org) — 네트워크 전송에 Netty를 사용하는 단순 고성능 LDAP 클라이언트.
* [Lettuce](https://lettuce.io) — 논블로킹 Reactive 애플리케이션 개발에 적합한 확장 가능한 Redis 클라이언트.
* [LittleProxy](https://github.com/adamfisk/LittleProxy) — 고성능 HTTP 프록시.
* [MessagePack](http://msgpack.org) — 여러 언어 사이에 구조화된 객체를 교환할 수 있게 해주는 바이너리 기반 효율적 객체 직렬화 라이브러리.
* [Micronaut](https://micronaut.io/) — 모듈러·테스트 가능한 마이크로서비스/서버리스 애플리케이션을 만드는 모던 JVM 기반 풀스택 프레임워크.
* [Mobicents Media Server](http://www.mobicents.org) — 전화 통화나 VoIP 연결과 연관된 오디오·비디오 스트림을 처리하는 미디어 게이트웨이 서버.
* [Mobicents SIP Servlets](http://www.mobicents.org/products_sip_servlets.html) — 오픈소스 인증된 SIP Servlet 구현.
* [Mock Server](http://mock-server.com/) — Netty를 사용해 HTTP/HTTPS 인터페이스를 가진 시스템을 모킹하는 모킹·프록싱 프레임워크.
* [Moquette MQTT broker](http://code.google.com/p/moquette-mqtt) — Netty를 프로토콜 코덱으로 사용하는 단순한 MQTT 브로커.
* [Naggati](https://github.com/twitter/naggati2) — Scala로 작성된 Netty용 프로토콜 빌더 (DEPRECATED).
* [Netflow.io](https://github.com/wasted/netflow) — wasted.io에서 사용되는 Scala/Netty Netflow Collector.
* [Netty Agents](https://gitlab.com/opentoolset/netty-agents) — Netty 기반의 사용하기 쉬운 메시지 기반 통신 라이브러리.
* [Netty Tools](https://github.com/cgbystrom/netty-tools) — Netty와 함께 쓰면 유용한 작은 도구 모음. 다양한 HTTP 클라이언트·서버, 대역폭 미터, Thrift RPC 프로세서를 포함.
* [Netty-ICAP Codec](https://github.com/jmimo/netty-icap) — RFC3507을 완전히 준수하는 고성능 ICAP 코덱 구현. 주로 외부 서버로 작업을 오프로드하는 프록시 환경에서 사용.
* [Netty-Livereload](https://github.com/alexvictoor/netty-livereload) — Netty WebSocket 구현 위에서 동작하는 [Livereload](http://livereload.com/) 프로토콜 구현.
* [Netty-SocketIO](https://github.com/mrniko/netty-socketio) — Netty 위에서 작성된 Socket.IO 서버.
* [Netty-Transport-jSerialComm](https://github.com/Ziver/Netty-Transport-jSerialComm) — jSerialComm 라이브러리를 사용하는 Netty용 시리얼 포트 전송.
* [Netty-ZMTP](https://github.com/spotify/netty-zmtp) — ZeroMQ 메시지 전송 프로토콜 ZMTP/1.0을 구현하는 Netty 채널 핸들러 모음.
* [Nitmproxy](https://github.com/chhsiao90/nitmproxy) — HTTP/SOCKS 프록시, HTTP/1.1, HTTP2 프로토콜을 지원하는 프록시 서버.
* [nio_uring](https://github.com/bbeaupain/nio_uring) — io_uring을 사용하는 Netty 영감의 자바용 고성능 I/O 라이브러리.
* [Riposte](https://github.com/Nike-Inc/riposte) — 운영 준비된 HTTP API를 빠르게 개발하기 위한 Netty 기반 마이크로서비스 프레임워크.
* [ScaleCube](https://github.com/scalecube/scalecube-services) — 서비스 디스커버리·위치 투명성·내결함성·실시간 장애 검출 같은 분산 애플리케이션 횡단 관심사를 다루는 메시지 기반·비동기 lock-free 라이브러리.
* [Slacker](https://github.com/sunng87/slacker) — Netty 기반의 비동기 Clojure RPC 클라이언트/서버 라이브러리.
* [Socket-IO - Service Fabric I/O](https://github.com/servicefabric/socketio) — Netty 기반의 매우 빠른 Socket.IO 서버.
* [Nifty](https://github.com/facebook/nifty) — Netty 기반 Thrift 전송 구현.
* [NIOSMTP](https://github.com/normanmaurer/niosmtp) — 비동기 SMTP 클라이언트 구현.
* [OpenTSDB](http://opentsdb.net) — 컴퓨터 시스템에서 수집한 메트릭을 저장·인덱싱·서빙하기 위해 HBase 위에 작성된 분산·확장 가능한 시계열 데이터베이스.
* [Peregrine](http://peregrine_mapreduce.bitbucket.org/) — 데이터 파티션을 가로질러 반복 작업을 실행하기 위해 설계된 map-reduce 프레임워크.
* [Play Framework](http://www.playframework.com) — 개발자 생산성을 중심에 두고 RESTful 아키텍처를 지향하는 J2EE 스택의 대안 웹 프레임워크.
* [PS3 Media Server](http://code.google.com/p/ps3mediaserver) — PS3용 DLNA 호환 UPNP 미디어 서버. 모든 종류의 미디어 파일을 트랜스코딩·스트리밍.
* [Protobuf-RPC-Pro](https://github.com/pjklauser/protobuf-rpc-pro) — Google Protocol Buffer RPC 서비스의 자바 구현.
* [Pushy](http://relayrides.github.io/pushy/) — APNs(iOS/OS X) 푸시 알림을 보내는 자바 라이브러리.
* [Ratpack](http://www.ratpack.io/) — 고성능 웹 애플리케이션을 만들기 위한 단순하고 강력한 툴킷.
* [Redisson](https://github.com/mrniko/redisson) — Redis 서버 위에 분산·확장 가능한 자바 자료구조(Set, SortedSet, Map, ConcurrentMap, List, Queue, Deque, Lock, AtomicLong, CountDownLatch, Pub/Sub, HyperLogLog)를 제공.
* [RESTExpress](https://github.com/RestExpress/RestExpress) — 자바로 독립 실행형 REST 서비스를 구축하기 위한 경량 마이크로 프레임워크. JSON·XML 직렬화와 ISO 8601 날짜 형식을 기본 지원.
* [RHQ collectd decoder](https://github.com/rhq-project/netty-collectd) — collectd UDP 데이터그램을 디코드.
* [Spigot](http://www.spigotmc.org/) — CraftBukkit 기반의 고성능 Minecraft 서버. 커스텀 네트워크 스택에 Netty 사용.
* [Spinach](https://github.com/mp911de/spinach) — 동기·비동기 연결을 모두 제공하는 확장 가능한 스레드 안전 [Disque](https://github.com/antirez/disque) 클라이언트.
* [Swift-NIO](https://github.com/apple/swift-nio) — Swift(iOS)용 크로스 플랫폼 비동기 이벤트 기반 네트워크 애플리케이션 프레임워크. 고성능 프로토콜 서버·클라이언트를 빠르게 개발할 수 있도록 설계되었음.
* [Termd](https://github.com/termd/termd) — Netty 위에 Telnet 서버, SSH 서버, 웹 기반 터미널을 제공하는 자바용 터미널 애플리케이션 빌드 라이브러리.
* [Teiid](http://www.jboss.org/teiid) — 여러 이종 데이터 저장소의 데이터를 애플리케이션에서 통합 접근할 수 있게 해주는 데이터 가상화 시스템.
* [Torrent4J](https://github.com/torrent4j/torrent4j) — 순수 자바로 구현된 BitTorrent 라이브러리.
* [TomP2P](http://tomp2p.net) — 위치 키에 대한 값을 정렬된 테이블에 저장하는 확장 DHT(분산 해시 테이블).
* [Unfiltered](http://unfiltered.ws) — 다양한 서버 백엔드에서 일관된 어휘로 요청을 처리할 수 있게 해주는 Scala용 HTTP 요청 처리 툴킷.
* [Universal Media Server](https://github.com/UniversalMediaServer/UniversalMediaServer/) — PS3 등 렌더러를 위한 DLNA 호환 UPNP 미디어 서버.
* [Vert.x](http://vertx.io) — JVM에서 reactive 애플리케이션을 만들기 위한 툴킷.
* [WaarpFtp](http://waarp.github.com/WaarpFtp/) — Netty 기반 FTP 서버.
* [Bixby](https://github.com/llnek/bixby) — Clojure 서버 사이드 애플리케이션 프레임워크.
* [Webbit](http://webbitserver.org) — 이벤트 기반 WebSocket·HTTP 서버.
* [Websocket-MQTT-Forwarder](https://github.com/sylvek/websocket-mqtt-forwarder) — 이벤트 기반 WebSocket-MQTT 브로커 포워더.
* [Xeres](https://github.com/zapek/Xeres) — peer-to-peer(friend-to-friend) 애플리케이션.
* [Xitrum](https://xitrum-framework.github.io/) — Netty와 Hazelcast 위에 동작하는 비동기·클러스터드 Scala 웹 프레임워크 및 HTTP(S) 서버.
* [zooterrain](http://github.com/berndfo/zooterrain) — 모든 ZooKeeper znode와 그 변경 사항을 WebSocket을 통해 브라우저에 푸시하는 소형 독립 실행형 웹 서버.
