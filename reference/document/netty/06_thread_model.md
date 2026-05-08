# 스레드 모델 (Thread Model)

> 이 문서는 Netty 공식 Wiki의 "Thread model" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/thread-model.html
>
> 참고: 이 페이지는 Netty 내부 설계 토의의 흔적이 남은 메모성 문서입니다. 사용자가 따라하는 가이드라기보다 Netty 자체의 스레드 모델 변천을 이해하기 위한 자료입니다.

---

간단히 말해, 채널의 경우:

1. 전송(transport)이나 타입과 무관하게, **모든 upstream(즉, inbound) 이벤트는 그 채널의 I/O를 수행하는 스레드(I/O 스레드)에서 발사(fire)되어야 한다.**
2. 모든 downstream(즉, outbound) 이벤트는 I/O 스레드든 아니든 어떤 스레드에서도 트리거할 수 있다. 다만 downstream 이벤트의 부수효과로 트리거되는 upstream 이벤트는 반드시 I/O 스레드에서 발사되어야 한다. (예: `Channel.close()`가 `channelDisconnected`, `channelUnbound`, `channelClosed`를 트리거하면 이들은 I/O 스레드에서 발사되어야 한다.)

현재의 문제(UGLY - upstream 핸들러에서 race condition을 유발, BAD - race condition은 없지만 기대되는 스레드 모델을 위반):

* UGLY: downstream 이벤트의 부수효과로 트리거되는 upstream 이벤트가 호출자(caller) 스레드에서 트리거된다.
* UGLY: 로컬 전송(local transport)은 항상 호출자 스레드를 사용해 이벤트를 트리거한다.
* BAD: `channelOpen`이 `ChannelFactory.newChannel()`을 호출한 스레드에서 트리거되며, 그 스레드는 I/O 스레드가 아니다.
  좀 좋지 않은 부분이지만, 그렇지 않으면 여기서 채널을 닫아 동시에 활성화된 채널 수를 제한하는 것이 불가능하다. 이것을 I/O 스레드에서 한다면 그만큼 효율적이지 않을 것이다.
* BAD: 클라이언트 측 채널은 두 개의 I/O 스레드에 의해 실행된다. 하나는 연결 시도를 수행하고, 다른 하나는 실제 I/O를 수행한다.

해결할 작업 항목:

* 클라이언트 측 boss, 서버 측 boss, NioWorker를 모든 I/O 연산을 수행할 수 있는 범용 I/O 스레드로 통합한다. 그러면:
  * 연결 시도를 한 스레드가 그대로 read/write를 계속 수행할 수 있게 되어 클라이언트 측 채널 문제가 해결된다.
  * Netty가 열린 서버 포트 수만큼 스레드를 만드는 문제도 해결된다.
  * NioWorker 풀을 더 쉽게 공유할 수 있고, 채널-워커 매핑에 더 큰 유연성을 가질 수 있다.
  * 모든 전송(socket, datagram, SCTP, ...)이 상속할 수 있는 추상 I/O 스레드 클래스를 만들 수 있는지도 살펴봐야 한다. 현재 socket, datagram, SCTP 사이에 중복이 너무 많다.
* 호출자 스레드가 I/O 스레드가 아니라면, Netty는 이후에 I/O 스레드에서 upstream 이벤트를 트리거한다. 이 변경과 함께, `ChannelPipeline`과 `ChannelHandlerContext`에 `sendUpstreamLater()` 메서드를 추가해 사용자가 자신만의 upstream 이벤트를 이후에 I/O 스레드에서 트리거할 수 있도록 허용한다.
  * 그러나 현재 스레드가 I/O 스레드가 아닐 때만 `sendUpstreamLater()`를 사용하게 할 수는 없다. `OMATPE`나 `MATPE`가 이를 방해할 것이기 때문이다. 따라서 사용자가 직접 결정하도록 해야 한다 (즉, `sendUpstream()`을 부를지 `sendUpstreamLater()`를 부를지).
* `ChannelFactory.newChannel()`은 즉시 이벤트를 트리거해서는 안 된다. `newChannel()`은 새 채널을 호출자에게 반환하기 전에, 채널이 I/O 스레드에 등록되었음을 I/O 스레드가 알려줄 때까지 기다려야 한다.
* 로컬 전송을 다시 작성한다.

질문:

* 이 모든 변경을 v3에서 후방 호환을 유지하며 적용할 수 있을까? v4에서 처리하는 편이 더 쉽지 않을까? 핸들러에서 모든 I/O를 수행하고 `ChannelFuture`를 적극 활용하는 완전 비동기 사용자 애플리케이션은 현재의 결함 있는 스레드 모델에 영향을 받지 않으므로, 사용자는 어떻게든 이 문제를 우회할 수 있다. 따라서 두 브랜치에 동일한 변경을 가하기보다는 v4로 옮겨가는 편이 나을 수도 있다.

답변:

* v3에 'backport'하기에 작업이 너무 크다면 그냥 앞으로 나아가고 v3에서는 '무시'하는 것이 낫다고 생각한다. 사용자를 가장 자주 괴롭힐 가능성이 높은 `Channel.close()` race를 적어도 제거할 수 있는 더 '쉬운' v3 우회책을 찾을 수 있을지도 모르겠다. (normanmaurer)
