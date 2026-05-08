# Thread Affinity (스레드 친화도)

> 이 문서는 Netty 공식 Wiki의 "Thread Affinity" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/thread-affinity.html

---

Netty로 저지연(low-latency) 네트워크 애플리케이션을 개발 중이라면 'thread affinity'에 대해 들어봤을 겁니다. Thread affinity는 애플리케이션 스레드를 특정 CPU 코어 또는 CPU 집합 위에서만 실행되도록 강제하는 데 사용할 수 있습니다.

이렇게 하면 운영체제 스케줄링 과정에서 스레드 마이그레이션을 제거할 수 있습니다. 다행히 [Java-Thread-Affinity](https://github.com/OpenHFT/Java-Thread-Affinity)라는 자바 라이브러리가 있어 Netty 애플리케이션과 쉽게 통합할 수 있습니다.

먼저, Maven `pom.xml` 파일에 다음 의존성을 추가합니다.

```xml
<dependency>
    <groupId>net.openhft</groupId>
    <artifactId>affinity</artifactId>
    <version>3.0.6</version>
</dependency>
```

다음으로, 특정 전략을 가진 `AffinityThreadFactory`를 생성하고 이를 지연시간에 민감한 스레드를 포함하게 될 `EventLoopGroup`에 전달합니다. 예를 들어:

```java
final int acceptorThreads = 1;
final int workerThreads = 10;
EventLoopGroup acceptorGroup = new NioEventLoopGroup(acceptorThreads);
ThreadFactory threadFactory = new AffinityThreadFactory("atf_wrk", AffinityStrategies.DIFFERENT_CORE);
EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads, threadFactory);

ServerBootstrap serverBootstrap = new ServerBootstrap().group(acceptorGroup, workerGroup);
```

가능한 가장 낮은 지연을 달성하려면 대상 CPU를 OS 스케줄러로부터 격리(isolate)하는 것을 고려해야 합니다. 대상 CPU를 격리하면 OS 스케줄러가 그 CPU에 다른 사용자 공간 프로세스를 스케줄링하지 못하게 됩니다. 이는 `isolcpus` 커널 부트 파라미터로 가능합니다(즉, `grub.conf`에 `isolcpus=<cpu-list>`를 추가).

자세한 내용은 프로젝트의 [GitHub](https://github.com/OpenHFT/Java-Thread-Affinity)를 참고하세요.
