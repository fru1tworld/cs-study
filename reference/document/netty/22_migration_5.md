# Netty 5 마이그레이션 가이드

> 이 문서는 Netty 공식 Wiki의 "Netty 5 Migration Guide" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/netty-5-migration-guide.html

---

Netty 5 Migration Guide
=======================

## 패키지

netty 5와 netty 4가 같은 클래스패스에 공존할 수 있도록 netty 5 클래스의 패키지명을 `io.netty5.*`로 변경했습니다.

## Buffer

Netty 5는 `ByteBuf`보다 더 단순하고 안전한 새 Buffer API를 도입합니다.

### 용량 (Capacity)

Netty 4.1의 `ByteBuf`는 쓰기가 일어날 때 특정 최대 용량까지 자동으로 확장됐습니다.

새 `Buffer` API는 더 이상 그렇게 동작하지 않으며, 용량과 최대 용량의 구분도 없습니다. 코드는 적절한 크기의 버퍼를 할당하거나(크기 인자가 이제 필수임), 필요할 때 `ensureWritable()`을 호출해야 합니다. `ensureWritable()` 메서드는 단일 메모리 복사로 compaction(과거 `discardReadBytes()`와 동일)과 확장을 동시에 수행할 수 있는 인자도 받을 수 있게 되었습니다.

### 어댑터

두 API 사이를 변환할 수 있는 어댑터 세트가 포함되어 있어, 모든 핸들러와 관련 코드가 마이그레이션될 때까지 두 API가 공존할 수 있게 합니다.

`ByteBufBuffer.wrap()` 메서드는 `ByteBuf` 인스턴스를 받아 새 API의 버퍼 인스턴스인 `Buffer`를 반환합니다. 반대로 `ByteBufAdaptor.intoByteBuf` 메서드는 `Buffer`를 받아 `ByteBuf`를 반환합니다. 두 메서드는 가능한 가장 효율적인 방법으로 변환을 수행하며, 여러 번의 변환은 어댑터 중첩을 피하기 위해 서로 상쇄됩니다.

서로 다른 API에 의존하는 핸들러 사이에 끼울 수 있는 `BufferConversionHandler`도 포함되어 있습니다. `BufferConversionHandler`는 `ByteBufHolder`나 `BufferHolder` 객체는 변환할 수 없다는 점에 유의하세요. `Buffer`나 `ByteBuf` 인스턴스를 포함하는 객체는 예를 들어 커스텀 `MessageToMessageCodec` 같은 것으로 변환해야 합니다.

### API 변경

새 `Buffer` API의 핵심 차이는 *aliasing*이 더 이상 허용되지 않는다는 점입니다. aliasing은 두 개 이상의 버퍼 객체가 같은 기반 메모리를 가리키는 상황을 말합니다. 따라서 `slice()`, `duplicate()` 같은 메서드는 더 이상 제공되지 않습니다. 그 대신 라이프사이클 처리가 단순해지고, API에서 참조 카운트를 완전히 제거할 수 있습니다.

`slice()`, `duplicate()`와 그 `retain*` 변형, 그리고 `retain()` 패밀리를 사용하던 곳에서는 이제 `split()`, `readSplit()`, `copy()`를 대신 사용합니다.

`split` 패밀리는 `slice()`와 비슷하지만 버퍼의 끝에서만 자를 수 있고, 반환된 버퍼 슬라이스는 원본 버퍼에서 _제거_ 됩니다. 그 결과 aliasing이 방지됩니다.

`retain()`과 `release()` 메서드는 사라졌습니다. 대신 버퍼는 라이프사이클 끝에 호출될 `close()` 메서드를 갖습니다. 버퍼가 완전히 로컬 스코프와 라이프사이클을 가질 때를 위해 버퍼는 `AutoCloseable`도 구현합니다.

`retain()`을 사용하던 대부분의 위치는 사실 슈퍼/서브 클래스의 무조건적 `release()` 효과를 상쇄하기 위함이었습니다. 이런 경우 `split()`을 대신 사용할 수 있습니다. 보통 버퍼의 readable 부분을 넘기는 패턴이고, `split()`이 두 개의 버퍼를 만들어내며 각각이 닫혀야 하기 때문입니다.

새 `send()` 메서드는 버퍼의 "소유권"이 한 곳에서 다른 곳으로 이동함을 타입 시스템에 인코딩하는 데 사용됩니다. 예를 들어 `CompositeBuffer` 팩토리 메서드는 합성 버퍼가 컴포넌트 버퍼에 대한 배타적 접근을 얻도록 보장하기 위해 이를 사용합니다. 이는 버퍼 합성을 통한 aliasing을 방지합니다.

버퍼는 이제 항상 big-endian이며 `*LE` 접근자 메서드는 사라졌습니다. little-endian 읽기/쓰기를 수행하려면 `Integer`나 `Long`의 `reverseBytes` 메서드와 big-endian 읽기/쓰기를 함께 사용하세요.

## Future / Promise

### ChannelFuture / ChannelPromise 제거

API와 타입 계층을 단순화하기 위해 `ChannelFuture`/`ChannelPromise`(그리고 그 모든 서브타입/구현체)를 완전히 제거하기로 결정했습니다. 대체로 `Future<Void>`와 `Promise<Void>`를 직접 사용합니다.

### ProgressiveFuture / ProgressivePromise와 ChannelProgressiveFuture / ChannelProgressivePromise 제거

netty 5에서 Progressive*Future / Progressive*Promise 지원이 완전히 제거되었습니다. 가끔 유용했을 수 있지만, promise를 체이닝할 때 파이프라인의 모든 핸들러가 특별한 조치를 해야 했습니다. 실제로는 그렇게 하지 못한 경우가 많고, 현실에서 매우 번거롭습니다.

이러한 문제로 인해, 이 기능 사용량 자체가 많지 않았으므로 완전히 제거하는 것이 최선이라고 결정했습니다. "가끔만" 동작하는 무언가를 갖는 것보다 아예 지원하지 않는 것이 낫습니다. 또한 유지보수할 코드도 줄어듭니다.

### VoidChannelPromise 제거

netty 4.1.x에서는 `voidPromise()` 메서드를 사용해 다양한 IO 연산(`write` 등)에서 사용할 수 있는 특수 `ChannelPromise` 구현을 얻어 객체 생성 수를 줄일 수 있었습니다. 동기는 좋았지만, 이 특수 케이스의 `ChannelPromise`는 실제로 많은 문제를 가져왔습니다.

### Promise와 Future의 API 변경

* `Future.addListeners()`, `Future.removeListeners()`, `Future.removeListener()`가 제거되었습니다. 이전에 추가한 리스너를 제거하는 기능이었는데, 거의 사용되지 않아 복잡도와 API 표면을 줄일 수 있었습니다.
* `Future.isFailed()` 메서드가 추가되었습니다. future가 완료되었고 실패했는지 확인합니다. future가 완료되었고 성공했는지 확인하는 기존 `Future.isSuccess()`와 유사합니다.
* 새 `Future.map()`과 `Future.flatMap()` 메서드가 추가되어 기존 future를 기반으로 새 future를 손쉽게 합성·생성할 수 있습니다. 이 메서드들은 실패와 취소를 적절히 전파해 처리합니다.
* `CompletionStage`로 변환하는 새 메서드들이 추가되어 다른 API와의 상호운용이 쉬워졌습니다.
* 사람들이 잘못 사용해 `EventLoop`을 블록하기 쉬워서, `Future` 인터페이스에서 모든 블로킹 메서드가 제거되었습니다. 여전히 `EventLoop` 외부에서 블록해야 한다면 `Future.asStage()`를 통해 변환해야 합니다. 반환된 `FutureCompletionStage`가 블로킹 메서드를 제공합니다.

## Channel

### `Channel.eventLoop()`이 `Channel.executor()`로 이름 변경

netty 5.x에서 `ChannelOutboundInvoker`에 `executor()` 메서드를 추가했습니다. 이 메서드는 `EventExecutor`를 반환하므로 `Channel`에서 `eventLoop()`을 제거하고 `executor()`를 오버라이드해 `Channel`에는 `EventLoop`을 반환하도록 했습니다.

### 반환 타입이 `ChannelFuture`에서 `Future<Void>`로 변경

`ChannelFuture`/`ChannelPromise`를 제거함에 따라 메서드들의 반환 타입도 `Future<Void>`로 변경되었습니다.

### Half-closure

이제 Netty 5는 코어에 half-closure 지원이 내장되어 있습니다. 이를 위해 `ChannelHandler.shutdown`과 `ChannelHandler.channelShutdown`이 도입되었습니다. 또한 `Channel.isShutdown(...)`과 `ChannelOutboundInvoker.shutdown(...)`도 추가되었습니다. 이는 완전히 제거된 기존 `DuplexChannel` 추상화를 대체합니다. 자세한 내용은 [PR #12468](https://github.com/netty/netty/pull/12468)을 참고하세요.

### `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않음

`ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않도록 변경했습니다. attribute를 사용한다면 여전히 `AttributeMap`을 상속하는 `Channel`을 직접 사용해야 합니다.

### `Channel.Unsafe` 제거

`Channel.Unsafe` 인터페이스가 완전히 제거되어 사용자가 내부를 망칠 수 없게 되었습니다.

### `ChannelOutboundBuffer`가 더 이상 `Channel` API의 일부가 아님

`ChannelOutboundBuffer`는 우리 `AbstractChannel` 구현의 구현 디테일이므로 `Channel` 자체에서 완전히 제거되었습니다.

### `Channel.beforeBeforeWritable()` 제거 및 `Channel.bytesBeforeUnwritable()`이 `writableBytes()`로 이름 변경

전혀 사용되지 않던 `Channel.beforeBeforeWritable()` 메서드를 제거하고, `Channel.bytesBeforeUnwritable()`을 `Channel.writableBytes`로 이름을 바꿨습니다.

## ChannelPipeline

### `ChannelPipeline.add*(EventExecutorGroup...)` 제거

netty 4.x에서는 `EventExecutorGroup`을 명시적으로 지정해 `ChannelPipeline`에 `ChannelHandler`를 추가할 수 있게 했습니다. 좋은 아이디어처럼 보였지만 라이프사이클 관점에서 꽤 많은 문제가 있었습니다.

- `handlerRemoved(...)`, `handlerAdded(...)`가 "잘못된 시점"에 호출될 수 있습니다. 이는 많은 문제를 일으킵니다. 최악의 경우, `handlerRemoved(...)`가 호출되어 핸들러가 다시 사용되지 않을 것이라 가정하고 native 메모리를 해제했는데, 이후 `channelRead(...)`가 호출되어 이미 해제된 메모리에 접근하려다 JVM이 크래시할 수 있습니다.
- 파이프라인의 동시 접근/수정에서의 "가시성"을 올바르게 구현하는 것도 꽤 까다롭습니다.

이런 문제들을 보면서, 사용자가 정말 원하는 것은 비즈니스 로직 처리를 위해 들어오는 메시지를 다른 스레드가 처리하는 것이라는 점을 깨달았습니다. 이는 사용자가 만든 커스텀 구현으로 처리하는 편이 낫습니다. 무엇이 언제 파괴될 수 있는지 사용자가 더 잘 알기 때문입니다.

### 반환 타입이 `ChannelFuture`에서 `Future<Void>`로 변경

`ChannelFuture`/`ChannelPromise`를 제거함에 따라 메서드들의 반환 타입도 `Future<Void>`로 변경되었습니다.

## ChannelHandler

Netty 5는 `ChannelHandler`의 타입 계층을 크게 단순화합니다.

### 단순화된 핸들러 타입 계층

`ChannelInboundHandler`와 `ChannelOutboundHandler`가 [`ChannelHandler`]로 통합되었습니다. [`ChannelHandler`]는 이제 inbound·outbound 핸들러 메서드를 모두 갖습니다. `ChannelPromise`를 받던 모든 outbound 메서드는 `Future<Void>`를 반환하도록 변경되었습니다. 이 변경은 사용 시 오류를 줄이고 API를 단순화합니다.

`ChannelInboundHandlerAdapter`, `ChannelOutboundHandlerAdapter`, `ChannelDuplexHandlerAdapter`는 제거되고 [`ChannelHandlerAdapter`]로 대체되었습니다.

이제 핸들러가 inbound인지 outbound인지 구분할 수 없으므로 `CombinedChannelDuplexHandler`는 [`ChannelHandlerAppender`]로 대체되었습니다.

자세한 내용은 [PR #1999](https://github.com/netty/netty/pull/1999)를 참고하세요.

### `channelRead0()` → `messageReceived()`

[`SimpleChannelInboundHandler`]를 사용한다면 `channelRead0()`을 `messageReceived()`로 이름을 바꿔야 합니다.

### 사용자 이벤트

이제 파이프라인을 통해 양방향으로 사용자/커스텀 이벤트를 발사할 수 있습니다. inbound 이벤트는 `fireChannelInboundEvent(...)`(`fireUserEventTriggered(...)` 대체), outbound 이벤트는 `sendOutboundEvent(...)`를 사용합니다. 두 가지 모두 평소처럼 `ChannelHandler`에 정의된 메서드로 가로챌 수 있습니다.

### `ChannelHandler.pendingOutboundBytes(...)` 메서드 추가

이제 `ChannelPipeline` 안의 `ChannelHandler`가 `Channel`의 writability에 손쉽게 영향을 줄 수 있습니다. 즉, `ChannelHandler` 자체가 outbound 데이터를 버퍼링할 때 back pressure에 영향을 줄 수 있게 되었습니다.

## EventLoopGroup / EventLoop

netty 4.x에서는 다양한 전송에 대해 서로 다른 `EventLoopGroup`/`EventLoop` 구현을 갖고 있었습니다(예: `NioEventLoopGroup`, `EpollEventLoopGroup` 등). netty 5에서는 이를 변경해 `MultiThreadEventLoopGroup`이라 불리는 단 하나의 `EventLoopGroup` 구현만 갖도록 했습니다. `MultiThreadEventLoopGroup`은 전송에 특화된 `IoHandlerFactory`를 받습니다(예: `NioHandler.newFactory()`, `EpollHandler.newFactory()` 등). 이 변경은 큰 장점이 많습니다. 예를 들어 `MultiThreadEventLoopGroup`을 상속해 데코레이션을 추가하거나 커스텀 메트릭 등을 추가하기 쉽습니다. 이 구현은 다른 전송에서도 재사용할 수 있습니다. 이는 JDK가 `ThreadPoolExecutor`로 제공하는 커스터마이징 가능성과 매우 유사합니다.

### `EventLoopGroup.isCompatible(...)` 메서드 추가

이제 사용 시도 전에 `Channel` 서브타입이 `EventLoopGroup`/`EventLoop`과 호환되는지 확인할 수 있습니다. 올바른 `Channel` 서브타입을 선택하는 데 도움이 됩니다.

### `EventLoop.registerForIo(...)`와 `EventLoop.deregisterForIo` 추가

`EventLoop` 인터페이스에 `Channel` 등록·등록 해제용 새 메서드들이 추가되었습니다. 사용자가 직접 사용해서는 안 되며 `Channel` 구현체 자체가 사용해야 합니다.

## 거의 쓰이지 않는 코덱이 Netty Contrib로 이동

코드베이스를 슬림하게 만들고 유지보수 부담을 줄이기 위해 다음 코덱과 핸들러가 [Netty Contrib](https://github.com/netty-contrib)으로 이동했습니다.

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
