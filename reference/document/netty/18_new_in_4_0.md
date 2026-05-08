# Netty 4.0의 새로운 기능과 주요 변경사항

> 이 문서는 Netty 공식 Wiki의 "New and Noteworthy in 4.0" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/new-and-noteworthy-in-4.0.html

---

이 문서는 메이저 Netty 릴리스의 주목할 만한 변경사항과 새 기능을 안내해 애플리케이션을 새 버전으로 포팅하는 데 참고가 되도록 합니다.

## 프로젝트 구조 변경

Netty의 패키지명이 `org.jboss.netty`에서 `io.netty`로 변경되었습니다. [JBoss.org에서 떨어져 나왔기](http://netty.io/news/2011/11/04/new-web-site.html) 때문입니다.

바이너리 JAR는 여러 서브모듈로 분할되어, 사용자가 클래스패스에서 불필요한 기능을 제외할 수 있게 되었습니다. 현재 구조는 다음과 같습니다.

| Artifact ID | 설명 |
| --- | --- |
| `netty-parent` | Maven 부모 POM |
| `netty-common` | 유틸리티 클래스와 로깅 facade |
| `netty-buffer` | `java.nio.ByteBuffer`를 대체하는 `ByteBuf` API |
| `netty-transport` | Channel API와 핵심 전송 |
| `netty-transport-rxtx` | [Rxtx](http://goo.gl/vTFBv) 전송 |
| `netty-transport-sctp` | [SCTP](http://goo.gl/oXxaU) 전송 |
| `netty-transport-udt` | [UDT](http://udt.sourceforge.net/) 전송 |
| `netty-handler` | 유용한 `ChannelHandler` 구현 |
| `netty-codec` | 인코더·디코더 작성을 돕는 코덱 프레임워크 |
| `netty-codec-http` | HTTP, Web Sockets, SPDY, RTSP 관련 코덱 |
| `netty-codec-socks` | SOCKS 프로토콜 관련 코덱 |
| `netty-all` | 위의 모든 아티팩트를 합친 올인원 JAR |
| `netty-tarball` | tarball 배포본 |
| `netty-example` | 예제 |
| `netty-testsuite-*` | 통합 테스트 모음 |
| `netty-microbench` | 마이크로벤치마크 |

`netty-all.jar`을 제외한 모든 아티팩트는 OSGi bundle이며, 원하는 OSGi 컨테이너에서 사용할 수 있습니다.

## 일반 API 변경

* 대부분의 Netty 연산이 메서드 체이닝을 지원해 코드가 간결해졌습니다.
* 설정용이 아닌 getter는 더 이상 `get-` 접두어를 갖지 않습니다(예: `Channel.getRemoteAddress()` → `Channel.remoteAddress()`).
  * boolean 속성은 혼동을 피하기 위해 여전히 `is-` 접두어를 갖습니다(예: 'empty'는 형용사이자 동사이므로 `empty()`는 두 의미를 가질 수 있음).
* 4.0 CR4와 4.0 CR5 사이의 API 변경에 대해서는 [Netty 4.0.0.CR5 released with new-new API](http://netty.io/news/2013/06/18/4-0-0-CR5.html)를 참고하세요.

## Buffer API 변경

### `ChannelBuffer` → `ByteBuf`

위에서 언급한 구조 변경 덕분에 buffer API는 별도의 패키지로 사용할 수 있게 되었습니다. Netty를 네트워크 애플리케이션 프레임워크로 채택할 의향이 없더라도 buffer API만 사용할 수 있습니다. 따라서 `ChannelBuffer`라는 타입명은 더 이상 의미가 없어 `ByteBuf`로 이름이 변경되었습니다.

새 버퍼를 생성하던 유틸리티 클래스 `ChannelBuffers`는 `Unpooled`와 `ByteBufUtil` 두 유틸리티 클래스로 분할되었습니다. `Unpooled`라는 이름에서 짐작할 수 있듯이, 4.0은 `ByteBufAllocator` 구현으로 할당할 수 있는 풀링된(pooled) `ByteBuf`를 도입했습니다.

### `ByteBuf`는 인터페이스가 아니라 추상 클래스

내부 성능 테스트에 따르면 `ByteBuf`를 인터페이스에서 추상 클래스로 변경한 것이 전체 처리량을 약 5% 향상시켰습니다.

### 대부분의 버퍼는 최대 용량을 가진 동적 버퍼

3.x에서 버퍼는 고정 또는 동적이었습니다. 고정 버퍼는 만들어지면 용량이 변하지 않는 반면, 동적 버퍼는 `write*(...)` 메서드가 더 큰 공간을 필요로 할 때마다 용량이 변합니다.

4.0부터 모든 버퍼가 동적입니다. 다만 예전 동적 버퍼보다 개선되었습니다. 버퍼 용량을 더 쉽고 안전하게 늘리거나 줄일 수 있습니다. 새 메서드 `ByteBuf.capacity(int newCapacity)` 덕분에 쉬워졌고, 버퍼의 최대 용량을 설정할 수 있어 무한정 커지지 않게 막을 수 있다는 점에서 안전해졌습니다.

```java
// dynamicBuffer()는 더 이상 없음 - buffer()를 사용한다.
ByteBuf buf = Unpooled.buffer();

// 버퍼 용량 증가.
buf.capacity(1024);
...

// 버퍼 용량 감소(마지막 512바이트가 삭제된다).
buf.capacity(512);
```

유일한 예외는 `wrappedBuffer()`로 만든, 단일 버퍼나 단일 byte 배열을 wrap한 버퍼입니다. 기존 버퍼를 wrap하는 목적(메모리 복사 절약)을 무력화하므로 그 용량은 늘릴 수 없습니다. wrap 후 용량을 바꾸고 싶다면 충분한 용량을 가진 새 버퍼를 만들고 wrap하려던 버퍼를 그대로 복사해야 합니다.

### 새 버퍼 타입: `CompositeByteBuf`

`CompositeByteBuf`라는 새 버퍼 구현은 합성 버퍼 구현을 위한 다양한 고급 연산을 정의합니다. 합성 버퍼는 비교적 비싼 랜덤 접근을 대가로 대량 메모리 복사 연산을 절약할 수 있게 해줍니다. 새 합성 버퍼를 만들려면 이전처럼 `Unpooled.wrappedBuffer(...)`를 쓰거나, `Unpooled.compositeBuffer(...)` 또는 `ByteBufAllocator.compositeBuffer()`를 사용하세요.

### 예측 가능한 NIO 버퍼 변환

3.x의 `ChannelBuffer.toByteBuffer()`와 그 변형은 결정성이 충분하지 않았습니다. 사용자가 결과로 데이터가 공유되는 view 버퍼를 받게 될지, 아니면 데이터가 분리된 복사본 버퍼를 받게 될지 알 수 없었습니다. 4.0은 `toByteBuffer()`를 `ByteBuf.nioBufferCount()`, `nioBuffer()`, `nioBuffers()`로 대체했습니다. `nioBufferCount()`가 `0`을 반환하면 사용자는 항상 `copy().nioBuffer()`를 호출해 복사된 버퍼를 얻을 수 있습니다.

### Little endian 지원 변경

Little endian 지원은 크게 바뀌었습니다. 이전에는 `LittleEndianHeapChannelBufferFactory`를 지정하거나, 원하는 byte order로 기존 버퍼를 wrap해서 little endian 버퍼를 얻어야 했습니다. 4.0은 새 메서드 `ByteBuf.order(ByteOrder)`를 추가합니다. 이 메서드는 호출 대상에 대해 원하는 byte order를 가진 view를 반환합니다.

```java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteOrder;
 
ByteBuf buf = Unpooled.buffer(4);
buf.setInt(0, 1);
// '00000001' 출력
System.out.format("%08x%n", buf.getInt(0)); 
 
ByteBuf leBuf = buf.order(ByteOrder.LITTLE_ENDIAN);
// '01000000' 출력
System.out.format("%08x%n", leBuf.getInt(0));
 
assert buf != leBuf;
assert buf == buf.order(ByteOrder.BIG_ENDIAN);
```

### 풀링 버퍼 (Pooled buffers)

Netty 4는 [buddy 할당](http://en.wikipedia.org/wiki/Buddy_memory_allocation)과 [slab 할당](http://en.wikipedia.org/wiki/Slab_allocation)을 결합한 [jemalloc](http://www.canonware.com/jemalloc/) 변형의 고성능 버퍼 풀을 도입합니다. 다음과 같은 이점이 있습니다.

* 잦은 버퍼 할당/해제로 인한 GC 압박 감소
* 0으로 채워야만 하는 새 버퍼 생성 시 발생하는 메모리 대역폭 소비 감소
* direct 버퍼의 적시 해제

이 기능을 활용하려면(unpooled 버퍼를 원하지 않는 한) [`ByteBufAllocator`](http://netty.io/4.0/api/index.html?io/netty/buffer/AbstractByteBufAllocator.html)에서 버퍼를 얻어야 합니다.

```java
Channel channel = ...;
ByteBufAllocator alloc = channel.alloc();
ByteBuf buf = alloc.buffer(512);
....
channel.write(buf);
 
ChannelHandlerContext ctx = ...
ByteBuf buf2 = ctx.alloc().buffer(512);
....
channel.write(buf2)
```

`ByteBuf`가 원격 피어로 쓰이고 나면, 자신이 왔던 풀로 자동으로 반환됩니다.

기본 `ByteBufAllocator`는 `PooledByteBufAllocator`입니다. 버퍼 풀링을 사용하지 않거나 자체 할당자를 쓰고 싶다면 `Channel.config().setAllocator(...)`로 `UnpooledByteBufAllocator` 같은 대체 할당자를 지정하세요.

참고: 현재 시점에서 기본 할당자는 `UnpooledByteBufAllocator`입니다. `PooledByteBufAllocator`에 메모리 누수가 없음을 확인한 뒤 다시 기본값으로 되돌릴 예정입니다.

#### `ByteBuf`는 항상 참조 카운트됨

`ByteBuf`의 라이프사이클을 더 예측 가능한 방식으로 제어하기 위해, Netty는 더 이상 가비지 컬렉터에 의존하지 않고 명시적인 참조 카운터를 채택합니다. 기본 규칙은 다음과 같습니다.

* 버퍼가 할당되면 초기 참조 카운트는 1입니다.
* 참조 카운트가 0으로 감소하면 버퍼는 해제되거나 자신이 왔던 풀로 반환됩니다.
* 다음 시도는 `IllegalReferenceCountException`을 일으킵니다.
  * 참조 카운트가 0인 버퍼에 접근하는 경우
  * 참조 카운트를 음수로 감소시키는 경우
  * 참조 카운트를 `Integer.MAX_VALUE` 이상으로 증가시키는 경우
* 파생 버퍼(slice, duplicate)와 swap된 버퍼(little endian 버퍼)는 파생된 원본 버퍼와 참조 카운트를 공유합니다. 파생 버퍼를 만들 때 참조 카운트가 변하지 않는다는 점에 유의하세요.

`ByteBuf`가 `ChannelPipeline`에서 사용될 때 추가로 기억해야 할 규칙은 다음과 같습니다.

* 파이프라인의 각 inbound(upstream) 핸들러는 받은 메시지를 release해야 합니다. Netty가 자동으로 release해주지 않습니다.
  * 코덱 프레임워크는 메시지를 자동으로 release하므로, 메시지를 그대로 다음 핸들러에 전달하려면 사용자가 참조 카운트를 증가시켜야 합니다.
* outbound(downstream) 메시지가 파이프라인의 시작에 도달하면 Netty가 그것을 회선에 써낸 뒤 release합니다.

#### 자동 버퍼 누수 검출

참조 카운트는 매우 강력하지만 오류를 만들기도 쉽습니다. 사용자가 버퍼 release를 잊은 위치를 찾을 수 있도록, 누수 검출기는 누수된 버퍼가 할당된 위치의 스택 트레이스를 자동으로 로깅합니다.

누수 검출기는 `PhantomReference`에 의존하며 스택 트레이스 획득은 매우 비싼 연산이므로 약 1%의 할당만 샘플링합니다. 따라서 가능한 모든 누수를 찾으려면 충분히 오랫동안 애플리케이션을 실행해보는 것이 좋습니다.

모든 누수를 찾아 수정한 뒤에는 `-Dio.netty.noResourceLeakDetection` JVM 옵션으로 이 기능을 완전히 끄고 런타임 오버헤드를 제거할 수 있습니다.

## `io.netty.util.concurrent`

새 독립 buffer API와 함께, 4.0은 일반적으로 비동기 애플리케이션 작성에 유용한 다양한 구성 요소를 `io.netty.util.concurrent`라는 새 패키지에 제공합니다. 그중 일부는 다음과 같습니다.

* `Future`와 `Promise` - `ChannelFuture`와 비슷하지만 `Channel`에 의존하지 않음
* `EventExecutor`와 `EventExecutorGroup` - 범용 이벤트 루프 API

이들은 이 문서 뒷부분에서 설명할 channel API의 기반으로 사용됩니다. 예를 들어 `ChannelFuture`는 `io.netty.util.concurrent.Future`를 상속하고, `EventLoopGroup`은 `EventExecutorGroup`을 상속합니다.

![Event loop type hierarchy diagram](https://github.com/netty/netty.github.com/raw/master/images/concurrent.png)

## Channel API 변경

4.0에서 `io.netty.channel` 패키지의 많은 클래스가 대대적으로 개편되어, 단순한 텍스트 search-and-replace로는 3.x 애플리케이션이 4.0에서 동작하지 않습니다. 이 절은 모든 변경의 망라형 자료라기보다 이런 큰 변화의 사고 과정을 보여주려고 합니다.

### 개편된 ChannelHandler 인터페이스

#### Upstream → Inbound, Downstream → Outbound

'upstream'과 'downstream'이라는 용어는 초보자에게 꽤 헷갈렸습니다. 4.0은 가능한 곳마다 'inbound'와 'outbound'를 사용합니다.

#### 새 `ChannelHandler` 타입 계층

3.x에서 `ChannelHandler`는 단순한 태그 인터페이스였고, `ChannelUpstreamHandler`, `ChannelDownstreamHandler`, `LifeCycleAwareChannelHandler`가 실제 핸들러 메서드를 정의했습니다. Netty 4에서는 `ChannelHandler`가 `LifeCycleAwareChannelHandler`를 흡수하면서, inbound·outbound 양쪽에 유용한 몇 가지 메서드도 함께 갖습니다.

```java
public interface ChannelHandler {
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
```

다음 다이어그램이 새 타입 계층을 보여줍니다.

![ChannelHandler type hierarchy diagram](https://github.com/netty/netty.github.com/raw/master/images/handler.png)

#### 이벤트 객체 없는 `ChannelHandler`

3.x에서는 모든 I/O 연산이 `ChannelEvent` 객체를 만들었습니다. read/write마다 추가로 새 `ChannelBuffer`도 만들었습니다. 이는 자원 관리와 버퍼 풀링을 JVM에 위임하므로 Netty 내부를 꽤 단순화시켰습니다. 그러나 부하가 높은 Netty 기반 애플리케이션에서 종종 관찰되는 GC 압박과 불확실성의 근본 원인이 되곤 했습니다.

4.0은 이벤트 객체를 강타입 메서드 호출로 대체해 이벤트 객체 생성을 거의 완전히 제거했습니다. 3.x에는 `handleUpstream()`이나 `handleDownstream()` 같은 catch-all 이벤트 핸들러 메서드가 있었지만, 더 이상 그렇지 않습니다. 모든 이벤트 타입이 자기만의 핸들러 메서드를 갖습니다.

```java
// Before:
void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e);
void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e);
 
// After:
void channelRegistered(ChannelHandlerContext ctx);
void channelUnregistered(ChannelHandlerContext ctx);
void channelActive(ChannelHandlerContext ctx);
void channelInactive(ChannelHandlerContext ctx);
void channelRead(ChannelHandlerContext ctx, Object message);
 
void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise);
void connect(
        ChannelHandlerContext ctx, SocketAddress remoteAddress,
        SocketAddress localAddress, ChannelPromise promise);
void disconnect(ChannelHandlerContext ctx, ChannelPromise promise);
void close(ChannelHandlerContext ctx, ChannelPromise promise);
void deregister(ChannelHandlerContext ctx, ChannelPromise promise);
void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise);
void flush(ChannelHandlerContext ctx);
void read(ChannelHandlerContext ctx);
```

`ChannelHandlerContext`도 위 변경에 맞춰 바뀌었습니다.

```java
// Before:
ctx.sendUpstream(evt);
 
// After:
ctx.fireChannelRead(receivedMessage);
```

이 변경들은 사용자가 더 이상 존재하지 않는 `ChannelEvent` 인터페이스를 상속할 수 없다는 것을 의미합니다. 그렇다면 사용자가 `IdleStateEvent` 같은 자기만의 이벤트 타입은 어떻게 정의할까요? 4.0의 `ChannelHandlerContext`는 커스텀 이벤트를 트리거하는 `fireUserEventTriggered` 메서드를 갖고 있고, `ChannelInboundHandler`는 커스텀 이벤트 처리 전용 핸들러 메서드 `userEventTriggered()`를 갖습니다.

#### 단순화된 채널 상태 모델

3.x에서 새 연결된 `Channel`이 만들어질 때 최소 세 개의 `ChannelStateEvent`(`channelOpen`, `channelBound`, `channelConnected`)가 트리거됩니다. `Channel`이 닫힐 때도 최소 세 개(`channelDisconnected`, `channelUnbound`, `channelClosed`)가 트리거됩니다.

![Netty 3 Channel state diagram](https://github.com/netty/netty.github.com/raw/master/images/state_3.png)

그러나 그렇게 많은 이벤트를 트리거할 가치는 의심스럽습니다. 사용자에게는 `Channel`이 read/write를 수행할 수 있는 상태가 되었을 때 알림을 받는 것이 더 유용합니다.

![Netty 4 Channel state diagram](https://github.com/netty/netty.github.com/raw/master/images/state_small_4.png)

`channelOpen`, `channelBound`, `channelConnected`는 `channelActive`로 통합되었습니다. `channelDisconnected`, `channelUnbound`, `channelClosed`는 `channelInactive`로 통합되었습니다. 마찬가지로 `Channel.isBound()`와 `isConnected()`는 `isActive()`로 통합되었습니다.

`channelRegistered`와 `channelUnregistered`는 `channelOpen`과 `channelClosed`의 동등물이 아니라는 점에 유의하세요. 다음 그림처럼 `Channel`의 동적 등록·등록 해제·재등록을 지원하기 위해 도입된 새 상태입니다.

![Netty 4 Channel state diagram for re-registration](https://github.com/netty/netty.github.com/raw/master/images/state_4.png)

#### `write()`는 자동으로 flush하지 않음

4.0은 `Channel`의 outbound 버퍼를 명시적으로 flush하는 `flush()` 연산을 새로 도입했고, `write()` 연산은 자동으로 flush하지 않습니다. 메시지 단위로 동작하는 `java.io.BufferedOutputStream`이라고 생각하면 됩니다.

이 변경 때문에 무언가를 쓴 뒤 `ctx.flush()` 호출을 잊지 않도록 매우 주의해야 합니다. 또는 단축 메서드 `writeAndFlush()`를 사용할 수도 있습니다.

### 합리적이고 오류가 적은 inbound 트래픽 일시 중지

3.x에는 `Channel.setReadable(boolean)`이 제공하는 직관적이지 않은 inbound 트래픽 일시 중지 메커니즘이 있었습니다. ChannelHandler 사이의 복잡한 상호작용을 만들었고, 잘못 구현하면 핸들러끼리 쉽게 간섭할 수 있었습니다.

4.0은 `read()`라는 새 outbound 연산을 추가했습니다. `Channel.config().setAutoRead(false)`로 기본 auto-read 플래그를 끄면, Netty는 사용자가 명시적으로 `read()` 연산을 호출하기 전까지 아무것도 읽지 않습니다. 사용자가 발행한 `read()` 연산이 완료되어 채널이 다시 읽기를 멈추면 inbound 이벤트 `channelReadSuspended()`가 트리거되며, 사용자는 또 다른 `read()`를 발행할 수 있습니다. `read()` 연산을 가로채 더 고급의 트래픽 제어를 할 수도 있습니다.

#### 들어오는 연결 수락 일시 중지

3.x에서는 I/O 스레드를 블록하거나 서버 소켓을 닫는 것 외에 들어오는 연결 수락을 멈추라고 Netty에 알릴 방법이 없었습니다. 4.0은 일반 채널과 마찬가지로 auto-read 플래그가 설정되지 않았을 때 `read()` 연산을 존중합니다.

### Half-closed 소켓

TCP와 SCTP는 사용자가 소켓을 완전히 닫지 않고도 outbound 트래픽을 종료할 수 있게 해줍니다. 그런 소켓을 'half-closed 소켓'이라고 부르며, `SocketChannel.shutdownOutput()` 메서드로 만들 수 있습니다. 원격 피어가 outbound 트래픽을 종료하면 `SocketChannel.read(..)`는 `-1`을 반환하는데, 닫힌 연결과 겉으로는 구분이 되지 않았습니다.

3.x에는 `shutdownOutput()` 연산이 없었습니다. 또한 `SocketChannel.read(..)`가 `-1`을 반환하면 항상 연결을 닫았습니다.

half-closed 소켓을 지원하기 위해 4.0은 `SocketChannel.shutdownOutput()` 메서드를 추가했고, 사용자는 `ALLOW_HALF_CLOSURE` `ChannelOption`을 설정해 `SocketChannel.read(..)`가 `-1`을 반환해도 Netty가 자동으로 연결을 닫지 않도록 할 수 있습니다.

### 유연한 I/O 스레드 할당

3.x에서는 `ChannelFactory`가 `Channel`을 만들고, 새로 만든 `Channel`이 자동으로 숨은 I/O 스레드에 등록됩니다. 4.0은 `ChannelFactory`를 하나 이상의 `EventLoop`로 구성된 새 인터페이스 `EventLoopGroup`으로 대체합니다. 또한 새 `Channel`은 자동으로 `EventLoopGroup`에 등록되지 않으며, 사용자가 `EventLoopGroup.register()`를 명시적으로 호출해야 합니다.

이 변경 덕분에(`ChannelFactory`와 I/O 스레드의 분리), 같은 `EventLoopGroup`에 서로 다른 `Channel` 구현을 등록하거나, 같은 `Channel` 구현을 서로 다른 `EventLoopGroup`에 등록할 수 있습니다. 예를 들어 NIO 서버 소켓, NIO 클라이언트 소켓, NIO UDP 소켓, in-VM 로컬 채널을 같은 I/O 스레드에서 실행할 수 있습니다. 지연 최소화가 필요한 프록시 서버 작성 시 매우 유용합니다.

### 기존 JDK 소켓에서 `Channel` 만들기

3.x는 `java.nio.channels.SocketChannel` 같은 기존 JDK 소켓에서 새 Channel을 만들 방법이 없었습니다. 4.0에서는 가능합니다.

### I/O 스레드에서 `Channel` 등록 해제와 재등록

3.x에서 새 `Channel`이 만들어지면 그 기반 소켓이 닫힐 때까지 단일 I/O 스레드에 완전히 묶입니다. 4.0에서 사용자는 `Channel`을 자기 I/O 스레드에서 등록 해제하여 기반 JDK 소켓을 완전히 통제할 수 있습니다. 예를 들어 Netty가 제공하는 고수준 non-blocking I/O를 활용해 복잡한 프로토콜을 다루다가 나중에 `Channel`을 등록 해제하고 blocking 모드로 전환해 가능한 한 최대 처리량으로 파일을 전송할 수 있습니다. 물론 등록 해제된 `Channel`을 다시 등록할 수도 있습니다.

```java
java.nio.channels.FileChannel myFile = ...;
java.nio.channels.SocketChannel mySocket = java.nio.channels.SocketChannel.open();
 
// 여기서 일부 블로킹 연산을 수행한다.
...
 
// Netty가 이어받는다.
SocketChannel ch = new NioSocketChannel(mySocket);
EventLoopGroup group = ...;
group.register(ch);
...
 
// Netty에서 등록 해제.
ch.deregister().sync();
 
// 여기서 일부 블로킹 연산을 수행한다.
mySocket.configureBlocking(true);
myFile.transferFrom(mySocket, ...);
 
// 다른 이벤트 루프 그룹에 다시 등록.
EventLoopGroup anotherGroup = ...;
anotherGroup.register(ch);
```

### I/O 스레드에서 임의의 태스크 스케줄링

`Channel`이 `EventLoopGroup`에 등록되면, 실제로는 `EventLoopGroup`이 관리하는 `EventLoop` 중 하나에 등록됩니다. `EventLoop`는 `java.util.concurrent.ScheduledExecutorService`를 구현합니다. 즉, 사용자는 자신의 채널이 속한 I/O 스레드에서 임의의 `Runnable`이나 `Callable`을 실행하거나 스케줄링할 수 있습니다. 뒤에서 설명할 잘 정의된 새 스레드 모델과 함께라면 스레드 안전한 핸들러를 작성하기가 훨씬 쉬워집니다.

```java
public class MyHandler extends ChannelOutboundHandlerAdapter {
    ...
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise p) {
        ...
        ctx.write(msg, p);
        
        // write 타임아웃 스케줄.
        ctx.executor().schedule(new MyWriteTimeoutTask(p), 30, TimeUnit.SECONDS);
        ...
    }
}
 
public class Main {
    public static void main(String[] args) throws Exception {
        // I/O 스레드에서 임의의 태스크 실행.
        Channel ch = ...;
        ch.executor().execute(new Runnable() { ... });
    }
}
```

### 단순화된 종료

이제 `releaseExternalResources()`는 없습니다. `EventLoopGroup.shutdownGracefully()`를 호출하면 모든 열려 있는 채널을 즉시 닫고 모든 I/O 스레드가 스스로 멈추도록 만들 수 있습니다.

### 타입 안전한 `ChannelOption`

Netty에서 `Channel`의 소켓 파라미터를 설정하는 방법은 두 가지입니다. 하나는 `SocketChannelConfig.setTcpNoDelay(true)`처럼 `ChannelConfig`의 setter를 직접 호출하는 것입니다. 가장 타입 안전한 방법입니다. 다른 하나는 `ChannelConfig.setOption()` 메서드를 호출하는 것입니다. 어떤 소켓 옵션을 설정해야 할지 런타임에 결정해야 할 때가 있는데, 이런 경우 이 메서드가 이상적입니다. 그러나 3.x에서는 옵션을 (문자열, 객체) 쌍으로 지정해야 해서 오류가 잦았습니다. 옵션 이름이나 값을 잘못 지정하면 `ClassCastException`을 만나거나, 지정한 옵션이 조용히 무시되기도 했습니다.

4.0은 소켓 옵션에 대한 타입 안전한 접근을 제공하는 새 타입 `ChannelOption`을 도입했습니다.

```java
ChannelConfig cfg = ...;
 
// Before:
cfg.setOption("tcpNoDelay", true);
cfg.setOption("tcpNoDelay", 0);  // 런타임 ClassCastException
cfg.setOption("tcpNoDelays", true); // 옵션 이름 오타 - 조용히 무시됨
 
// After:
cfg.setOption(ChannelOption.TCP_NODELAY, true);
cfg.setOption(ChannelOption.TCP_NODELAY, 0); // 컴파일 오류
```

### AttributeMap

사용자 요청에 따라 `Channel`과 `ChannelHandlerContext`에 임의의 객체를 붙일 수 있게 되었습니다. `Channel`과 `ChannelHandlerContext`가 상속하는 새 인터페이스 `AttributeMap`이 추가되었고, 그 대신 `ChannelLocal`과 `Channel.attachment`는 제거되었습니다. attribute는 연관된 `Channel`이 가비지 컬렉트될 때 함께 가비지 컬렉트됩니다.

```java
public class MyHandler extends ChannelInboundHandlerAdapter {
 
    private static final AttributeKey<MyState> STATE =
            AttributeKey.valueOf("MyHandler.state");
 
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        ctx.attr(STATE).set(new MyState());
        ctx.fireChannelRegistered();
    }
 
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        MyState state = ctx.attr(STATE).get();
    }
    ...
}
```

### 새 bootstrap API

bootstrap API는 목적은 같지만 처음부터 다시 작성되었습니다. 보일러플레이트에서 흔히 보이는, 서버 또는 클라이언트를 구동하기 위한 일반적인 단계를 수행합니다.

새 bootstrap도 fluent 인터페이스를 사용합니다.

```java
public static void main(String[] args) throws Exception {
    // 서버를 설정한다.
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .option(ChannelOption.SO_BACKLOG, 100)
         .localAddress(8080)
         .childOption(ChannelOption.TCP_NODELAY, true)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ch.pipeline().addLast(handler1, handler2, ...);
             }
         });
 
        // 서버 시작.
        ChannelFuture f = b.bind().sync();
 
        // 서버 소켓이 닫힐 때까지 대기.
        f.channel().closeFuture().sync();
    } finally {
        // 모든 이벤트 루프를 종료해 모든 스레드를 멈춤.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        
        // 모든 스레드가 종료될 때까지 대기.
        bossGroup.terminationFuture().sync();
        workerGroup.terminationFuture().sync();
    }
}
```

#### `ChannelPipelineFactory` → `ChannelInitializer`

위 예제에서 알 수 있듯이 `ChannelPipelineFactory`는 더 이상 없습니다. `Channel`과 `ChannelPipeline` 설정을 더 잘 제어할 수 있게 해주는 `ChannelInitializer`로 대체되었습니다.

새 `ChannelPipeline`은 사용자가 직접 만들지 않는다는 점에 유의하세요. 지금까지 보고된 많은 사용 사례를 관찰한 결과, Netty 프로젝트 팀은 사용자가 자기 파이프라인 구현을 만들거나 기본 구현을 상속하는 데 어떤 이점도 없다고 결론 내렸습니다. 따라서 `ChannelPipeline`은 더 이상 사용자가 만들지 않으며, `Channel`이 자동으로 만듭니다.

### `ChannelFuture` → `ChannelFuture`와 `ChannelPromise`

`ChannelFuture`는 `ChannelFuture`와 `ChannelPromise`로 분할되었습니다. 비동기 연산의 소비자와 생산자의 계약을 명확히 할 뿐만 아니라, `ChannelFuture`의 상태를 변경할 수 없게 만들어 (필터링 같은) 체인에서 반환된 `ChannelFuture`를 더 안전하게 사용할 수 있게 합니다.

이 변경으로 일부 메서드는 상태 변경을 위해 `ChannelFuture`가 아닌 `ChannelPromise`를 받습니다.

## 잘 정의된 스레드 모델

3.x에는 잘 정의된 스레드 모델이 없었으며, 3.5에서 일관성을 고치려는 시도가 있었습니다. 4.0은 사용자가 스레드 안전성에 대해 너무 걱정하지 않고도 ChannelHandler를 작성할 수 있게 도와주는 엄격한 스레드 모델을 정의합니다.

* `ChannelHandler`가 `@Sharable`로 애노테이션되지 않는 한, Netty는 절대로 그 메서드를 동시(concurrently)로 호출하지 않습니다. 핸들러 메서드의 종류(inbound, outbound, life cycle 이벤트)와 무관합니다.
  * 사용자는 더 이상 inbound·outbound 이벤트 핸들러 메서드를 동기화할 필요가 없습니다.
  * 4.0은 `@Sharable`로 애노테이션되지 않은 `ChannelHandler`를 두 번 이상 추가하는 것을 허용하지 않습니다.
* Netty가 만드는 각 `ChannelHandler` 메서드 호출 사이에는 항상 [happens-before](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html#MemoryVisibility) 관계가 있습니다.
  * 사용자는 핸들러의 상태를 보존하기 위해 `volatile` 필드를 정의할 필요가 없습니다.
* 사용자는 `ChannelPipeline`에 핸들러를 추가할 때 `EventExecutor`를 지정할 수 있습니다.
  * 지정한 경우 그 `ChannelHandler`의 핸들러 메서드는 항상 지정된 `EventExecutor`에 의해 호출됩니다.
  * 지정하지 않은 경우 핸들러 메서드는 연관된 `Channel`이 등록된 `EventLoop`에 의해 호출됩니다.
* 핸들러나 채널에 할당된 `EventExecutor`/`EventLoop`은 항상 단일 스레드입니다.
  * 핸들러 메서드는 항상 동일한 스레드에 의해 호출됩니다.
  * 멀티스레드 `EventExecutor`/`EventLoop`이 지정되면 스레드 중 하나가 먼저 선택되고, 등록 해제 전까지 그 스레드만 사용됩니다.
  * 같은 파이프라인의 두 핸들러에 서로 다른 `EventExecutor`가 할당되면 동시에 호출됩니다. 같은 파이프라인 내 핸들러들만 공유 데이터에 접근하더라도, 한 명 이상의 핸들러가 그것에 접근한다면 사용자는 스레드 안전성에 주의해야 합니다.
* `ChannelFuture`에 추가된 `ChannelFutureListeners`는 항상 그 future의 연관 `Channel`에 할당된 `EventLoop` 스레드에 의해 호출됩니다.
* `ChannelHandlerInvoker`는 `Channel` 이벤트의 순서를 제어하는 데 사용할 수 있습니다. `DefaultChannelHandlerInvoker`는 `EventLoop` 스레드에서 온 이벤트는 즉시 실행하고, 다른 스레드에서 온 이벤트는 `EventExecutor`에 `Runnable` 객체로 실행합니다. 아래 예제는 `EventLoop` 스레드와 다른 스레드에서 채널과 상호작용할 때의 함의를 보여줍니다. (이 기능은 이후 제거되었습니다. [관련 커밋](https://github.com/netty/netty/commit/68cd670eb9b8827b4d3b7602f4ea8e14c38691ac#diff-6e08f48c25a292a7612f9d6941e67303) 참고)

###### Write Ordering - `EventLoop` 스레드와 다른 스레드 혼합

```java
Channel ch = ...;
ByteBuf a, b, c = ...;

// Thread 1에서 - EventLoop 스레드가 아님
ch.write(a);
ch.write(b);

// .. 다른 일들이 일어남

// EventLoop 스레드에서
ch.write(c);

// a, b, c가 기반 전송에 어떤 순서로 쓰이는지는 잘 정의되어 있지 않다.
// 순서가 중요하고 이런 스레드 상호작용이 발생한다면,
// 순서를 강제하는 것은 사용자의 책임이다.
```

### `ExecutionHandler`는 더 이상 없음 - 코어에 들어감

`ChannelPipeline`에 `ChannelHandler`를 추가할 때 `EventExecutor`를 지정하면, 추가된 `ChannelHandler`의 핸들러 메서드를 항상 그 `EventExecutor`를 통해 호출하라고 파이프라인에 알릴 수 있습니다.

```java
Channel ch = ...;
ChannelPipeline p = ch.pipeline();
EventExecutor e1 = new DefaultEventExecutor(16);
EventExecutor e2 = new DefaultEventExecutor(8);
 
p.addLast(new MyProtocolCodec());
p.addLast(e1, new MyDatabaseAccessingHandler());
p.addLast(e2, new MyHardDiskAccessingHandler());
```

## 코덱 프레임워크 변경

4.0은 핸들러가 자신의 버퍼를 만들고 관리하도록 요구하므로(이 문서의 Per-handler buffer 섹션 참고), 코덱 프레임워크에는 상당한 내부 변경이 있었습니다. 다만 사용자 입장에서의 변경은 그리 크지 않습니다.

* 핵심 코덱 클래스는 `io.netty.handler.codec` 패키지로 이동했습니다.
* `FrameDecoder`는 `ByteToMessageDecoder`로 이름이 변경되었습니다.
* `OneToOneEncoder`와 `OneToOneDecoder`는 `MessageToMessageEncoder`와 `MessageToMessageDecoder`로 대체되었습니다.
* `decode()`, `decodeLast()`, `encode()`의 메서드 시그니처는 제네릭 지원과 중복 파라미터 제거를 위해 약간 바뀌었습니다.

### Codec embedder → `EmbeddedChannel`

Codec embedder는 코덱을 포함한 모든 종류의 파이프라인을 테스트할 수 있게 해주는 `io.netty.channel.embedded.EmbeddedChannel`로 대체되었습니다.

### HTTP 코덱

HTTP 디코더는 이제 단일 HTTP 메시지에 대해 항상 여러 메시지 객체를 만듭니다.

```
1       * HttpRequest / HttpResponse
0 - n   * HttpContent
1       * LastHttpContent
```

자세한 내용은 갱신된 `HttpSnoopServer` 예제를 참고하세요. 단일 HTTP 메시지에 대해 여러 메시지를 다루고 싶지 않다면 파이프라인에 `HttpObjectAggregator`를 넣으면 됩니다. `HttpObjectAggregator`는 여러 메시지를 단일 `FullHttpRequest`나 `FullHttpResponse`로 변환합니다.

### 전송 구현 변경

다음 전송이 새로 추가되었습니다.

* OIO SCTP 전송
* UDT 전송

## 사례 연구: Factorial 예제 포팅

이 절은 Factorial 예제를 3.x에서 4.0으로 포팅하는 대략적 단계를 보여줍니다. Factorial 예제는 이미 `io.netty.example.factorial` 패키지에서 4.0으로 포팅되었습니다. 모든 변경 사항을 보려면 예제 소스 코드를 살펴보세요.

### 서버 포팅

1. 새 bootstrap API를 사용하도록 `FactorialServer.run()` 메서드를 다시 작성합니다.
   1. `ChannelFactory`는 더 이상 없습니다. 직접 `NioEventLoopGroup`을 인스턴스화합니다(들어오는 연결 수락용 하나, 수락된 연결 처리용 하나).
2. `FactorialServerPipelineFactory`를 `FactorialServerInitializer`로 이름을 바꿉니다.
   1. `ChannelInitializer<Channel>`을 상속하게 만듭니다.
   2. 새 `ChannelPipeline`을 만드는 대신 `Channel.pipeline()`으로 가져옵니다.
3. `FactorialServerHandler`가 `ChannelInboundHandlerAdapter`를 상속하게 만듭니다.
   1. `channelDisconnected()`를 `channelInactive()`로 대체.
   2. `handleUpstream()`은 더 이상 사용되지 않습니다.
   3. `messageReceived()`를 `channelRead()`로 이름을 바꾸고 메서드 시그니처를 그에 맞게 조정.
   4. `ctx.write()`를 `ctx.writeAndFlush()`로 대체.
4. `BigIntegerDecoder`가 `ByteToMessageDecoder<BigInteger>`를 상속하게 만듭니다.
5. `NumberEncoder`가 `MessageToByteEncoder<Number>`를 상속하게 만듭니다.
   1. `encode()`는 더 이상 버퍼를 반환하지 않습니다. `ByteToMessageDecoder`가 제공하는 버퍼에 인코딩된 데이터를 채웁니다.

### 클라이언트 포팅

대부분 서버 포팅과 동일하지만, 잠재적으로 큰 스트림을 쓸 때는 주의가 필요합니다.

1. 새 bootstrap API를 사용하도록 `FactorialClient.run()` 메서드를 다시 작성합니다.
2. `FactorialClientPipelineFactory`를 `FactorialClientInitializer`로 이름을 바꿉니다.
3. `FactorialClientHandler`가 `ChannelInboundHandlerAdapter`를 상속하게 만듭니다.
