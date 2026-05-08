# Netty 5.0의 새로운 기능과 주요 변경사항

> 이 문서는 Netty 공식 Wiki의 "New and Noteworthy in 5.0" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/new-and-noteworthy-in-5.0.html

---

이 문서는 4.1 이후 메이저 Netty 릴리스의 주목할 만한 변경사항과 새 기능을 안내합니다.

3.x와 4.0 사이의 변경과 달리, 5.0은 설계 단순성에서 상당한 돌파구를 마련했지만 변경 폭이 그렇게 크지는 않습니다. 4.x에서 5.0으로의 전환을 가능한 한 매끄럽게 만들려고 노력했지만, 마이그레이션 중 문제가 있다면 알려주세요.

## 코어 변경

### 새 Buffer API가 ByteBuf를 대체

Netty 5는 ByteBuf보다 더 단순하고 안전한 새 Buffer API를 도입합니다. 자세한 내용은 [PR #11347](https://github.com/netty/netty/pull/11347)을 참고하세요. 요약하면 새 API의 주요 차이점은 다음과 같습니다.

* **Aliasing이 더 이상 허용되지 않음.** 즉, 같은 메모리를 가리키는 여러 버퍼를 가질 수 없습니다.
  * 따라서 `slice`, `duplicate`와 그것들의 retain 변형이 사라졌습니다.
  * 대체로 `split`, `readSplit`, `send` 같은 새 API가 도입되었습니다. 이 메서드들의 계약이 aliasing을 막아줍니다.
* **참조 카운팅이 사실상 사라짐.**
  * `retain`과 `release` 메서드가 사라졌습니다. 대신 버퍼는 라이프사이클 끝에서 호출될 `close` 메서드를 갖습니다.
  * API와 통합은 버퍼가 항상 유일하고 명확한 소유권을 갖도록 설계되어야 합니다. 더 이상 런타임에 차용/참조를 추적하는 데 참조 카운팅을 사용할 수 없으므로, API에서 버퍼 "차용(borrowing)"은 최소화·억제되어야 합니다.
  * `retain`을 사용하던 대부분의 위치는 사실 슈퍼 클래스의 무조건적 `release` 효과를 상쇄하기 위한 것이었습니다. 이런 경우에는 보통 버퍼의 readable 부분만 넘기고 싶어 하므로 `split`을 사용할 수 있습니다.
* `send` 메서드와 Send 인터페이스를 사용해 타입 시스템에 소유권 이전을 인코딩할 수 있습니다.
* **버퍼는 항상 big-endian이며 `*LE` 메서드들이 사라졌습니다.**
  * little-endian 읽기/쓰기를 수행하려면 `Integer`, `Long` 등의 `reverseBytes` 메서드와 버퍼의 big-endian 읽기/쓰기 메서드를 함께 사용하세요.
  * `BufferUtil`에는 "medium"(3바이트) 정수의 바이트 순서를 뒤집는 메서드가 있습니다.
* 버퍼 구현의 테스트 커버리지가 더 높아졌고 동작도 더 일관적입니다.

### ChannelHandler

#### 단순화된 핸들러 타입 계층

`ChannelInboundHandler`와 `ChannelOutboundHandler`가 [`ChannelHandler`]로 통합되었습니다. [`ChannelHandler`]는 이제 inbound·outbound 핸들러 메서드를 모두 갖습니다.

`ChannelInboundHandlerAdapter`, `ChannelOutboundHandlerAdapter`, `ChannelDuplexHandlerAdapter`는 제거되고 `ChannelHandlerAdapter`로 대체되었습니다.

이제 핸들러가 inbound인지 outbound인지 구분할 수 없으므로, `CombinedChannelDuplexHandler`는 `ChannelHandlerAppender`로 대체되었습니다.

자세한 내용은 [PR #1999](https://github.com/netty/netty/pull/1999)를 참고하세요.

#### `SimpleChannelInboundHandler.channelRead0()` → `messageReceived()`

`SimpleChannelInboundHandler`를 사용한다면 `channelRead0()`를 `messageReceived()`로 이름을 바꿔야 합니다.

#### `ChannelHandler` 메서드 시그니처 변경

(`flush`와 `read`를 제외한) 모든 `ChannelHandler` outbound 메서드는 이제 `Future<Void>`를 반환합니다. 이는 적절한 전파와 체이닝을 보장합니다. 또한 inbound 예외를 처리한다는 점을 명확히 하기 위해 `exceptionCaught(...)`를 `channelExceptionCaught(...)`로 이름을 바꿨습니다.

#### 사용자 이벤트

이제 파이프라인을 통해 양방향으로 사용자/커스텀 이벤트를 발사할 수 있습니다. inbound 이벤트는 `fireChannelInboundEvent(...)`(`fireUserEventTriggered(...)` 대체), outbound 이벤트는 `sendOutboundEvent(...)`를 사용합니다. 두 가지 모두 평소처럼 `ChannelHandler`에 정의된 메서드로 가로챌 수 있습니다.

#### `ChannelHandler.pendingOutboundBytes(...)` 메서드 추가

이제 `ChannelPipeline` 안의 `ChannelHandler`가 `Channel`의 writability에 손쉽게 영향을 줄 수 있습니다. 즉, `ChannelHandler` 자체가 outbound 데이터를 버퍼링할 때 back pressure에 영향을 줄 수 있게 되었습니다.

### 전송 (Transport)

#### Half-closure

이제 Netty 5는 코어에 half-closure 지원이 내장되어 있습니다. 이를 위해 `ChannelHandler.shutdown`과 `ChannelHandler.channelShutdown`이 도입되었습니다. 또한 `Channel.isShutdown(...)`과 `ChannelOutboundInvoker.shutdown(...)`도 추가되었습니다. 이는 완전히 제거된 기존 `DuplexChannel` 추상화를 대체합니다. 자세한 내용은 [PR #12468](https://github.com/netty/netty/pull/12468)을 참고하세요.

#### `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않음

`ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않도록 변경했습니다. attribute를 사용한다면 여전히 `AttributeMap`을 상속하는 `Channel`을 직접 사용해야 합니다.

#### `ChannelPipeline.add*(EventExecutorGroup...)` 제거

netty 4.x에서는 `EventExecutorGroup`을 명시적으로 지정해 `ChannelPipeline`에 `ChannelHandler`를 추가할 수 있게 했습니다. 좋은 아이디어처럼 보였지만 라이프사이클 관점에서 꽤 많은 문제가 있었습니다.

- `handlerRemoved(...)`, `handlerAdded(...)`가 "잘못된 시점"에 호출될 수 있습니다. 이는 많은 문제를 일으킵니다. 최악의 경우, `handlerRemoved(...)`가 호출되어 핸들러가 다시 사용되지 않을 것이라 가정하고 native 메모리를 해제했는데, 이후 `channelRead(...)`가 호출되어 이미 해제된 메모리에 접근하려다 JVM이 크래시할 수 있습니다.
- 파이프라인의 동시 접근/수정에서의 "가시성"을 올바르게 구현하는 것도 꽤 까다롭습니다.

이런 문제들을 보면서, 사용자가 정말 원하는 것은 비즈니스 로직 처리를 위해 들어오는 메시지를 다른 스레드가 처리하는 것이라는 점을 깨달았습니다. 이는 사용자가 만든 커스텀 구현으로 처리하는 편이 낫습니다. 무엇이 언제 파괴될 수 있는지 사용자가 더 잘 알기 때문입니다.

#### `Channel.eventLoop()`이 `Channel.executor()`로 이름 변경

netty 5.x에서 `ChannelOutboundInvoker`에 `executor()` 메서드를 추가했습니다. 이 메서드는 `EventExecutor`를 반환하므로, `Channel`에서 `eventLoop()`을 제거하고 `executor()`를 오버라이드해 `Channel`에는 `EventLoop`을 반환하도록 했습니다.

#### `EventLoopGroup.isCompatible(...)` 메서드 추가

이제 사용 시도 전에 `Channel` 서브타입이 `EventLoopGroup`/`EventLoop`과 호환되는지 확인할 수 있습니다. 올바른 `Channel` 서브타입을 선택하는 데 도움이 됩니다.

#### `EventLoop.registerForIo(...)`와 `EventLoop.deregisterForIo` 추가

`EventLoop` 인터페이스에 `Channel` 등록·등록 해제용 새 메서드들이 추가되었습니다. 사용자가 직접 사용해서는 안 되며 `Channel` 구현체 자체가 사용해야 합니다.

#### `Channel.Unsafe` 제거

`Channel.Unsafe` 인터페이스가 완전히 제거되어, 사용자가 내부를 망칠 수 없게 되었습니다.

#### `ChannelOutboundBuffer`가 더 이상 `Channel` API의 일부가 아님

`ChannelOutboundBuffer`는 우리 `AbstractChannel` 구현의 구현 디테일이므로 `Channel` 자체에서 완전히 제거되었습니다.

#### `Channel.beforeBeforeWritable()` 제거 및 `Channel.bytesBeforeUnwritable()`이 `writableBytes()`로 이름 변경

전혀 사용되지 않던 `Channel.beforeBeforeWritable()` 메서드를 제거하고, `Channel.bytesBeforeUnwritable()`을 `Channel.writableBytes`로 이름을 바꿨습니다.

### Future / Promise

#### `ProgressiveFuture`/`ProgressivePromise`와 `ChannelProgressiveFuture`/`ChannelProgressivePromise` 제거

netty 5에서 Progressive*Future / Progressive*Promise 지원이 완전히 제거되었습니다. 가끔 유용했을 수 있지만, promise를 체이닝할 때 파이프라인의 모든 핸들러가 특별한 조치를 해야 했습니다. 실제로는 그렇게 하지 못한 경우가 많고, 현실에서 매우 번거롭습니다.

이러한 문제로 인해, 이 기능 사용량 자체가 많지 않았으므로 완전히 제거하는 것이 최선이라고 결정했습니다. "가끔만" 동작하는 무언가를 갖는 것보다 아예 지원하지 않는 것이 낫습니다. 또한 유지보수할 코드도 줄어듭니다.

#### `VoidChannelPromise` 제거

netty 4.1.x에서는 `voidPromise()` 메서드를 사용해 다양한 IO 연산(`write` 등)에서 사용할 수 있는 특수 `ChannelPromise` 구현을 얻어 객체 생성 수를 줄일 수 있었습니다. 동기는 좋았지만, 이 특수 케이스의 `ChannelPromise`는 실제로 많은 문제를 가져왔습니다.

* 이 promise에 `ChannelFutureListener`를 추가하는 모든 `ChannelHandler`는 리스너 추가가 안전하도록 먼저 `unvoid()`를 호출해야 했습니다. 그러지 않으면 `addListener` 호출 시 `RuntimeException`이 발생합니다.
* `wait()` / `sync()` 연산이 전혀 지원되지 않았습니다.
* 어떤 연산은 `VoidChannelPromise` 사용을 허용하고, 어떤 연산은 그렇지 않았습니다.

#### `Promise`와 `Future`의 API 변경

* `Future.addListeners()`, `Future.removeListeners()`, `Future.removeListener()`가 제거되었습니다. 이전에 추가한 리스너를 제거하는 기능이었는데, 거의 사용되지 않아 복잡도와 API 표면을 줄일 수 있었습니다.
* `sync`와 `await`의 uninterruptible 변형도 제거되었습니다.
* `Future.isFailed()` 메서드가 추가되었습니다. future가 완료되었고 실패했는지 확인합니다. future가 완료되었고 성공했는지 확인하는 기존 `Future.isSuccess()`와 유사합니다.
* `Promise.setUncancellable`은 이제 promise가 "incomplete"에서 "uncancellable"로 전이될 때만 `true`를 반환합니다. 구체적으로, 이미 완료된 promise에 대해서는 `false`를 반환합니다(4.1에서는 그런 경우에도 `true`를 반환했습니다).
* 새 `Future.map()`과 `Future.flatMap()` 메서드가 추가되어, 기존 future를 기반으로 새 future를 손쉽게 합성·생성할 수 있습니다. 이 메서드들은 실패와 취소를 적절히 전파해 처리합니다.
* 사람들이 잘못 사용해 `EventLoop`을 블록하기 쉬워서, `Future` 인터페이스에서 모든 블로킹 메서드가 제거되었습니다. 여전히 `EventLoop` 외부에서 블록해야 한다면 `Future.asStage()`를 통해 변환해야 합니다. 반환된 `FutureCompletionStage`가 블로킹 메서드를 제공합니다.

## 코덱 변경

### 압축 지원

별도의 `EmbeddedChannel`을 만들지 않고도 다른 코덱에서 재사용하기 쉽도록, 모든 압축 구현이 새 [Compression API](https://github.com/netty/netty/pull/11685)를 사용하도록 변경되었습니다.

### HTTP 코덱

- netty-core에서 multi-part 지원이 제거되었습니다. 향후 contributor 저장소로 netty 5에 마이그레이션될 예정입니다. (https://github.com/netty/netty/pull/11830)
- 오래된 websocket draft 지원이 제거되었습니다. (https://github.com/netty/netty/pull/11831)
- HTTP/2 헤더 검증이 기본 활성화됩니다. 이로 인해 잘못된 형식의 요청이 기본적으로 거부됩니다.

### 거의 쓰이지 않는 코덱이 Netty Contrib로 이동

코드베이스를 슬림하게 만들고 유지보수 부담을 줄이기 위해, 다음 코덱과 핸들러가 [Netty Contrib](https://github.com/netty-contrib)으로 이동했습니다.

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

이제 Netty는 native image에서 최소한의 크기 오버헤드로 런타임에 자동 초기화됩니다. 최소 지원 Graal 버전은 22.1, Java 17입니다.
