# Netty 5.0의 새로운 기능과 주요 변경사항

> 원본: https://netty.io/wiki/new-and-noteworthy-in-5.0.html

---

4.1 이후 메이저 Netty 릴리스의 주목할 만한 변경사항과 새 기능을 정리합니다.

3.x와 4.0 사이의 변경과 달리, 5.0은 설계 단순성 면에서 상당한 도약을 이뤄냈지만 변경 규모 자체는 크지 않습니다. 4.x에서 5.0으로의 전환을 최대한 매끄럽게 만들기 위해 노력했으나, 마이그레이션 중 문제가 발생하면 알려 주세요.

## 코어 변경

### 새 Buffer API가 ByteBuf를 대체

Netty 5는 ByteBuf보다 단순하고 안전한 새 Buffer API를 도입합니다. 자세한 내용은 [PR #11347](https://github.com/netty/netty/pull/11347)을 참고하세요. 새 API의 주요 차이점은 다음과 같습니다.

* **Aliasing이 더 이상 허용되지 않음.** 즉, 같은 메모리를 가리키는 여러 버퍼를 가질 수 없습니다.
  * 따라서 `slice`, `duplicate` 및 그 retain 변형이 제거되었습니다.
  * 대신 `split`, `readSplit`, `send` 같은 새 API가 도입되었습니다. 이 메서드들의 계약이 aliasing을 방지합니다.
* **참조 카운팅이 사실상 사라짐.**
  * `retain`과 `release` 메서드가 제거되었습니다. 대신 버퍼는 라이프사이클 종료 시점에 호출하는 `close` 메서드를 갖습니다.
  * API와 통합 코드는 버퍼가 항상 단일하고 명확한 소유권을 갖도록 설계되어야 합니다. 더 이상 런타임에 참조 카운팅으로 차용/참조를 추적할 수 없으므로, API에서 버퍼 "차용(borrowing)"은 최소화하고 지양해야 합니다.
  * `retain`을 사용하던 대부분의 위치는 사실 슈퍼클래스의 무조건적 `release` 효과를 상쇄하기 위한 것이었습니다. 이런 경우 대개 버퍼의 readable 부분만 전달하고 싶은 것이므로 `split`을 사용할 수 있습니다.
* `send` 메서드와 `Send` 인터페이스를 사용해 타입 시스템에 소유권 이전을 표현할 수 있습니다.
* **버퍼는 항상 big-endian이며 `*LE` 메서드들이 사라졌습니다.**
  * little-endian 읽기/쓰기가 필요하면 `Integer`, `Long` 등의 `reverseBytes` 메서드와 버퍼의 big-endian 읽기/쓰기 메서드를 함께 사용하세요.
  * `BufferUtil`에는 "medium"(3바이트) 정수의 바이트 순서를 반전하는 메서드가 있습니다.
* 버퍼 구현의 테스트 커버리지가 더 높아졌고 동작도 더 일관적입니다.

### ChannelHandler

#### 단순화된 핸들러 타입 계층

`ChannelInboundHandler`와 `ChannelOutboundHandler`가 `ChannelHandler`로 통합되었습니다. `ChannelHandler`는 이제 inbound·outbound 핸들러 메서드를 모두 갖습니다.

`ChannelInboundHandlerAdapter`, `ChannelOutboundHandlerAdapter`, `ChannelDuplexHandlerAdapter`는 제거되고 `ChannelHandlerAdapter`로 대체되었습니다.

핸들러가 inbound인지 outbound인지 더 이상 구분할 수 없으므로, `CombinedChannelDuplexHandler`는 `ChannelHandlerAppender`로 대체되었습니다.

자세한 내용은 [PR #1999](https://github.com/netty/netty/pull/1999)를 참고하세요.

#### `SimpleChannelInboundHandler.channelRead0()` → `messageReceived()`

`SimpleChannelInboundHandler`를 사용한다면 `channelRead0()`를 `messageReceived()`로 이름을 바꿔야 합니다.

#### `ChannelHandler` 메서드 시그니처 변경

`flush`와 `read`를 제외한 모든 `ChannelHandler` outbound 메서드는 이제 `Future<Void>`를 반환합니다. 이를 통해 적절한 전파와 체이닝이 보장됩니다. 또한 inbound 예외를 처리한다는 점을 명확히 하기 위해 `exceptionCaught(...)`를 `channelExceptionCaught(...)`로 이름 변경했습니다.

#### 사용자 이벤트

이제 파이프라인을 통해 양방향으로 사용자/커스텀 이벤트를 발행할 수 있습니다. inbound 이벤트는 `fireChannelInboundEvent(...)`(`fireUserEventTriggered(...)` 대체), outbound 이벤트는 `sendOutboundEvent(...)`를 사용합니다. 두 경우 모두 `ChannelHandler`에 정의된 메서드로 가로챌 수 있습니다.

#### `ChannelHandler.pendingOutboundBytes(...)` 메서드 추가

`ChannelPipeline` 안의 `ChannelHandler`가 `Channel`의 writability에 쉽게 영향을 줄 수 있게 되었습니다. 즉, `ChannelHandler`가 outbound 데이터를 직접 버퍼링할 때 back pressure에도 영향을 줄 수 있습니다.

### 전송 (Transport)

#### Half-closure

Netty 5는 코어에 half-closure 지원을 내장합니다. 이를 위해 `ChannelHandler.shutdown`과 `ChannelHandler.channelShutdown`이 도입되었으며, `Channel.isShutdown(...)`과 `ChannelOutboundInvoker.shutdown(...)`도 추가되었습니다. 이로써 기존 `DuplexChannel` 추상화는 완전히 제거되었습니다. 자세한 내용은 [PR #12468](https://github.com/netty/netty/pull/12468)을 참고하세요.

#### `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않음

`ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않습니다. attribute를 사용하려면 여전히 `AttributeMap`을 상속하는 `Channel`을 직접 사용해야 합니다.

#### `ChannelPipeline.add*(EventExecutorGroup...)` 제거

netty 4.x에서는 `EventExecutorGroup`을 명시적으로 지정해 `ChannelPipeline`에 `ChannelHandler`를 추가할 수 있었습니다. 좋은 아이디어처럼 보였지만 라이프사이클 관점에서 여러 문제가 있었습니다.

- `handlerRemoved(...)`, `handlerAdded(...)`가 "잘못된 시점"에 호출될 수 있습니다. 최악의 경우, `handlerRemoved(...)`가 호출되어 핸들러가 더 이상 사용되지 않을 것이라 가정하고 native 메모리를 해제했는데, 이후 `channelRead(...)`가 호출되어 이미 해제된 메모리에 접근하다 JVM이 크래시할 수 있습니다.
- 파이프라인의 동시 접근/수정에서 "가시성"을 올바르게 구현하는 것도 상당히 까다롭습니다.

이런 문제들을 검토한 결과, 사용자가 실제로 원하는 것은 비즈니스 로직 처리를 위해 수신 메시지를 다른 스레드에서 처리하는 것임을 알게 되었습니다. 이는 사용자가 직접 만든 커스텀 구현으로 처리하는 것이 낫습니다. 무엇이 언제 해제될 수 있는지는 사용자가 더 잘 알기 때문입니다.

#### `Channel.eventLoop()`이 `Channel.executor()`로 이름 변경

netty 5.x에서 `ChannelOutboundInvoker`에 `executor()` 메서드를 추가했습니다. 이 메서드는 `EventExecutor`를 반환하므로, `Channel`에서는 `eventLoop()`을 제거하고 `executor()`를 오버라이드해 `EventLoop`을 반환하도록 했습니다.

#### `EventLoopGroup.isCompatible(...)` 메서드 추가

사용 전에 `Channel` 서브타입이 `EventLoopGroup`/`EventLoop`과 호환되는지 확인할 수 있습니다. 올바른 `Channel` 서브타입을 선택하는 데 도움이 됩니다.

#### `EventLoop.registerForIo(...)`와 `EventLoop.deregisterForIo` 추가

`EventLoop` 인터페이스에 `Channel` 등록·해제용 메서드들이 추가되었습니다. 이 메서드들은 사용자가 직접 호출하면 안 되며, `Channel` 구현체 내부에서만 사용해야 합니다.

#### `Channel.Unsafe` 제거

`Channel.Unsafe` 인터페이스가 완전히 제거되어, 사용자가 내부 구현을 직접 건드릴 수 없게 되었습니다.

#### `ChannelOutboundBuffer`가 더 이상 `Channel` API의 일부가 아님

`ChannelOutboundBuffer`는 `AbstractChannel` 구현의 내부 구현 세부사항이므로 `Channel` 공개 API에서 완전히 제거되었습니다.

#### `Channel.beforeBeforeWritable()` 제거 및 `Channel.bytesBeforeUnwritable()`이 `writableBytes()`로 이름 변경

전혀 사용되지 않던 `Channel.beforeBeforeWritable()` 메서드를 제거하고, `Channel.bytesBeforeUnwritable()`을 `Channel.writableBytes()`로 이름을 바꿨습니다.

### Future / Promise

#### `ProgressiveFuture`/`ProgressivePromise`와 `ChannelProgressiveFuture`/`ChannelProgressivePromise` 제거

netty 5에서 Progressive*Future / Progressive*Promise 지원이 완전히 제거되었습니다. 가끔 유용할 수 있었지만, promise를 체이닝할 때 파이프라인의 모든 핸들러가 특별한 처리를 해야 했습니다. 실제로는 그렇게 하지 못하는 경우가 많아 현실적으로 매우 불편했습니다.

사용량 자체도 많지 않았기 때문에, "가끔만" 동작하는 기능을 유지하는 것보다 완전히 제거하는 것이 낫다는 판단을 내렸습니다. 유지보수해야 할 코드도 줄어드는 이점이 있습니다.

#### `VoidChannelPromise` 제거

netty 4.1.x에서는 `voidPromise()` 메서드로 특수한 `ChannelPromise` 구현을 얻어 `write` 등 다양한 IO 연산에 사용함으로써 객체 생성을 줄일 수 있었습니다. 취지는 좋았지만 이 특수 `ChannelPromise`는 실제로 많은 문제를 야기했습니다.

* 이 promise에 `ChannelFutureListener`를 추가하는 모든 `ChannelHandler`는 리스너 추가 전에 먼저 `unvoid()`를 호출해야 했습니다. 그렇지 않으면 `addListener` 호출 시 `RuntimeException`이 발생합니다.
* `wait()` / `sync()` 연산이 전혀 지원되지 않았습니다.
* 일부 연산은 `VoidChannelPromise` 사용을 허용하고, 다른 연산은 허용하지 않는 불일관성이 있었습니다.

#### `Promise`와 `Future`의 API 변경

* `Future.addListeners()`, `Future.removeListeners()`, `Future.removeListener()`가 제거되었습니다. 이전에 추가한 리스너를 제거하는 기능이었지만 거의 사용되지 않았고, 제거함으로써 복잡도와 API 표면을 줄일 수 있었습니다.
* `sync`와 `await`의 uninterruptible 변형도 제거되었습니다.
* `Future.isFailed()` 메서드가 추가되었습니다. future가 완료되었고 실패했는지 확인하며, future가 완료되었고 성공했는지 확인하는 기존 `Future.isSuccess()`와 대칭을 이룹니다.
* `Promise.setUncancellable()`은 이제 promise가 "incomplete"에서 "uncancellable" 상태로 전이될 때만 `true`를 반환합니다. 이미 완료된 promise에 대해서는 `false`를 반환합니다(4.1에서는 그런 경우에도 `true`를 반환했습니다).
* `Future.map()`과 `Future.flatMap()` 메서드가 추가되어, 기존 future를 기반으로 새 future를 손쉽게 합성할 수 있습니다. 이 메서드들은 실패와 취소를 적절히 전파합니다.
* `Future` 인터페이스에서 모든 블로킹 메서드가 제거되었습니다. `EventLoop`을 잘못 블록하는 실수를 방지하기 위해서입니다. `EventLoop` 외부에서 블록이 필요하다면 `Future.asStage()`로 변환한 뒤 반환된 `FutureCompletionStage`의 블로킹 메서드를 사용해야 합니다.

## 코덱 변경

### 압축 지원

별도의 `EmbeddedChannel` 없이도 다른 코덱에서 쉽게 재사용할 수 있도록, 모든 압축 구현이 새 [Compression API](https://github.com/netty/netty/pull/11685)를 사용하도록 변경되었습니다.

### HTTP 코덱

- netty-core에서 multi-part 지원이 제거되었습니다. 향후 contributor 저장소로 netty 5에 마이그레이션될 예정입니다. (https://github.com/netty/netty/pull/11830)
- 오래된 websocket draft 지원이 제거되었습니다. (https://github.com/netty/netty/pull/11831)
- HTTP/2 헤더 검증이 기본 활성화됩니다. 이로 인해 잘못된 형식의 요청이 기본적으로 거부됩니다.

### 거의 쓰이지 않는 코덱이 Netty Contrib로 이동

코드베이스를 경량화하고 유지보수 부담을 줄이기 위해, 다음 코덱과 핸들러가 [Netty Contrib](https://github.com/netty-contrib)으로 이동했습니다.

* netty-codec-xml
* netty-codec-redis
* netty-codec-memcache
* netty-codec-stomp
* netty-codec-haproxy
* netty-codec-mqtt
* netty-codec-socks
* netty-handler-proxy
* `io.netty.handler.codec.json`
* `io.netty.handler.codec.marshalling`
* `io.netty.handler.codec.protobuf`
* `io.netty.handler.codec.serialization`
* `io.netty.handler.codec.xml`
* `io.netty.handler.pcap`

## GraalVM과 Native Image

Netty는 native image에서 최소한의 크기 오버헤드로 런타임에 자동 초기화됩니다. 최소 지원 Graal 버전은 22.1, Java 17입니다.
