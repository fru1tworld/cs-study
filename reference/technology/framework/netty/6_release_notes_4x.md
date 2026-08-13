# Netty 4.x 릴리스 노트와 마이그레이션

## Netty 4.0의 새로운 기능과 주요 변경사항

> 원본: https://netty.io/wiki/new-and-noteworthy-in-4.0.html

---

메이저 Netty 릴리스의 주목할 만한 변경사항과 새 기능 정리. 애플리케이션을 새 버전으로 포팅할 때 참고.

### 프로젝트 구조 변경

Netty의 패키지명이 `org.jboss.netty`에서 `io.netty`로 변경됨. [JBoss.org에서 분리](http://netty.io/news/2011/11/04/new-web-site.html)됐기 때문.

바이너리 JAR는 여러 서브모듈로 분할 → 사용자가 클래스패스에서 불필요한 기능 제외 가능. 현재 구조는 다음과 같음.

- `netty-parent`: Maven 부모 POM
- `netty-common`: 유틸리티 클래스와 로깅 facade
- `netty-buffer`: `java.nio.ByteBuffer`를 대체하는 `ByteBuf` API
- `netty-transport`: Channel API와 핵심 전송
- `netty-transport-rxtx`: [Rxtx](http://goo.gl/vTFBv) 전송
- `netty-transport-sctp`: [SCTP](http://goo.gl/oXxaU) 전송
- `netty-transport-udt`: [UDT](http://udt.sourceforge.net/) 전송
- `netty-handler`: 유용한 `ChannelHandler` 구현
- `netty-codec`: 인코더·디코더 작성을 돕는 코덱 프레임워크
- `netty-codec-http`: HTTP, Web Sockets, SPDY, RTSP 관련 코덱
- `netty-codec-socks`: SOCKS 프로토콜 관련 코덱
- `netty-all`: 위의 모든 아티팩트를 합친 올인원 JAR
- `netty-tarball`: tarball 배포본
- `netty-example`: 예제
- `netty-testsuite-*`: 통합 테스트 모음
- `netty-microbench`: 마이크로벤치마크

`netty-all.jar`을 제외한 모든 아티팩트는 OSGi bundle → 원하는 OSGi 컨테이너에서 사용 가능.

### 일반 API 변경

* 대부분의 Netty 연산이 메서드 체이닝을 지원 → 코드가 간결해짐.
* 설정용이 아닌 getter는 더 이상 `get-` 접두어를 갖지 않음(예: `Channel.getRemoteAddress()` → `Channel.remoteAddress()`).
  * boolean 속성은 혼동을 피하기 위해 여전히 `is-` 접두어를 가짐(예: 'empty'는 형용사이자 동사이므로 `empty()`는 두 의미를 가질 수 있음).
* 4.0 CR4와 4.0 CR5 사이의 API 변경은 [Netty 4.0.0.CR5 released with new-new API](http://netty.io/news/2013/06/18/4-0-0-CR5.html) 참고.

### Buffer API 변경

#### `ChannelBuffer` → `ByteBuf`

위에서 언급한 구조 변경으로 buffer API가 별도 패키지로 분리됨. Netty를 네트워크 애플리케이션 프레임워크로 사용하지 않더라도 buffer API만 단독으로 사용 가능. 이에 따라 `ChannelBuffer`라는 타입명은 의미가 없어져 `ByteBuf`로 변경됨.

새 버퍼를 생성하던 유틸리티 클래스 `ChannelBuffers`는 `Unpooled`와 `ByteBufUtil` 두 유틸리티 클래스로 분할됨. `Unpooled`라는 이름에서 짐작할 수 있듯이, 4.0은 `ByteBufAllocator` 구현으로 할당할 수 있는 풀링된(pooled) `ByteBuf`를 도입.

#### `ByteBuf`는 인터페이스가 아니라 추상 클래스

내부 성능 테스트에 따르면 `ByteBuf`를 인터페이스에서 추상 클래스로 변경한 것이 전체 처리량을 약 5% 향상.

#### 대부분의 버퍼는 최대 용량을 가진 동적 버퍼

3.x에서 버퍼는 고정 또는 동적. 고정 버퍼는 만들어지면 용량이 변하지 않는 반면, 동적 버퍼는 `write*(...)` 메서드가 더 큰 공간을 필요로 할 때마다 용량이 변함.

4.0부터 모든 버퍼가 동적. 다만 예전 동적 버퍼보다 개선됨. 버퍼 용량을 더 쉽고 안전하게 늘리거나 줄일 수 있음. 새 메서드 `ByteBuf.capacity(int newCapacity)` 덕분에 쉬워졌고, 버퍼의 최대 용량을 설정할 수 있어 무한정 커지지 않게 막을 수 있다는 점에서 안전함.

```java
// dynamicBuffer()는 더 이상 없음 - buffer()를 사용한다.
ByteBuf buf = Unpooled.buffer();

// 버퍼 용량 증가.
buf.capacity(1024);
...

// 버퍼 용량 감소(마지막 512바이트가 삭제된다).
buf.capacity(512);
```

유일한 예외는 `wrappedBuffer()`로 만든, 단일 버퍼나 단일 byte 배열을 wrap한 버퍼. 기존 버퍼를 wrap하는 목적(메모리 복사 절약)을 무력화하므로 그 용량은 늘릴 수 없음. wrap 후 용량을 바꾸려면 충분한 용량을 가진 새 버퍼를 만들고 wrap하려던 버퍼를 그대로 복사해야 함.

#### 새 버퍼 타입: `CompositeByteBuf`

`CompositeByteBuf`라는 새 버퍼 구현은 합성 버퍼 구현을 위한 다양한 고급 연산을 정의. 합성 버퍼는 비교적 비싼 랜덤 접근을 대가로 대량 메모리 복사 연산을 절약 가능. 새 합성 버퍼를 만들려면 이전처럼 `Unpooled.wrappedBuffer(...)`를 쓰거나, `Unpooled.compositeBuffer(...)` 또는 `ByteBufAllocator.compositeBuffer()`를 사용.

#### 예측 가능한 NIO 버퍼 변환

3.x의 `ChannelBuffer.toByteBuffer()`와 그 변형은 결정성이 충분하지 않았음. 사용자가 데이터를 공유하는 view 버퍼를 받게 될지, 데이터가 분리된 복사본 버퍼를 받게 될지 알 수 없었음. 4.0은 `toByteBuffer()`를 `ByteBuf.nioBufferCount()`, `nioBuffer()`, `nioBuffers()`로 대체. `nioBufferCount()`가 `0`을 반환하면 사용자는 항상 `copy().nioBuffer()`를 호출해 복사된 버퍼를 얻을 수 있음.

#### Little endian 지원 변경

Little endian 지원은 크게 바뀜. 이전에는 `LittleEndianHeapChannelBufferFactory`를 지정하거나, 원하는 byte order로 기존 버퍼를 wrap해야 little endian 버퍼를 얻을 수 있었음. 4.0은 새 메서드 `ByteBuf.order(ByteOrder)`를 추가. 이 메서드는 호출 대상에 대해 원하는 byte order를 가진 view를 반환.

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

#### 풀링 버퍼 (Pooled buffers)

Netty 4는 [buddy 할당](http://en.wikipedia.org/wiki/Buddy_memory_allocation)과 [slab 할당](http://en.wikipedia.org/wiki/Slab_allocation)을 결합한 [jemalloc](http://www.canonware.com/jemalloc/) 변형의 고성능 버퍼 풀을 도입. 다음과 같은 이점이 있음.

* 잦은 버퍼 할당/해제로 인한 GC 압박 감소
* 0으로 채워야만 하는 새 버퍼 생성 시 발생하는 메모리 대역폭 소비 감소
* direct 버퍼의 적시 해제

이 기능을 활용하려면(unpooled 버퍼를 원하지 않는 한) [`ByteBufAllocator`](http://netty.io/4.0/api/index.html?io/netty/buffer/AbstractByteBufAllocator.html)에서 버퍼를 얻어야 함.

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

`ByteBuf`가 원격 피어로 쓰이고 나면, 자신이 왔던 풀로 자동 반환됨.

기본 `ByteBufAllocator`는 `PooledByteBufAllocator`. 버퍼 풀링을 사용하지 않거나 자체 할당자를 쓰려면 `Channel.config().setAllocator(...)`로 `UnpooledByteBufAllocator` 같은 대체 할당자를 지정.

참고: 현재 시점에서 기본 할당자는 `UnpooledByteBufAllocator`. `PooledByteBufAllocator`에 메모리 누수가 없음을 확인한 뒤 다시 기본값으로 되돌릴 예정.

##### `ByteBuf`는 항상 참조 카운트됨

`ByteBuf`의 라이프사이클을 더 예측 가능한 방식으로 제어하기 위해, Netty는 더 이상 가비지 컬렉터에 의존하지 않고 명시적인 참조 카운터를 채택. 기본 규칙은 다음과 같음.

* 버퍼가 할당되면 초기 참조 카운트는 1
* 참조 카운트가 0으로 감소하면 버퍼는 해제되거나 자신이 왔던 풀로 반환
* 다음 시도는 `IllegalReferenceCountException`을 일으킴
  * 참조 카운트가 0인 버퍼에 접근하는 경우
  * 참조 카운트를 음수로 감소시키는 경우
  * 참조 카운트를 `Integer.MAX_VALUE` 이상으로 증가시키는 경우
* 파생 버퍼(slice, duplicate)와 swap된 버퍼(little endian 버퍼)는 파생된 원본 버퍼와 참조 카운트를 공유. 파생 버퍼를 만들 때 참조 카운트는 변하지 않음.

`ByteBuf`가 `ChannelPipeline`에서 사용될 때 추가로 기억해야 할 규칙은 다음과 같음.

* 파이프라인의 각 inbound(upstream) 핸들러는 받은 메시지를 release해야 함. Netty가 자동으로 release해주지 않음.
  * 코덱 프레임워크는 메시지를 자동으로 release하므로, 메시지를 그대로 다음 핸들러에 전달하려면 사용자가 참조 카운트를 증가시켜야 함.
* outbound(downstream) 메시지가 파이프라인의 시작에 도달하면 Netty가 그것을 회선에 써낸 뒤 release.

##### 자동 버퍼 누수 검출

참조 카운트는 매우 강력하지만 오류를 만들기도 쉬움. 사용자가 버퍼 release를 잊은 위치를 찾을 수 있도록, 누수 검출기는 누수된 버퍼가 할당된 위치의 스택 트레이스를 자동으로 로깅.

누수 검출기는 `PhantomReference`에 의존하며, 스택 트레이스 획득은 매우 비용이 큰 연산이므로 약 1%의 할당만 샘플링. 가능한 모든 누수를 찾으려면 애플리케이션을 충분히 오랫동안 실행 필요.

모든 누수를 찾아 수정한 뒤에는 `-Dio.netty.noResourceLeakDetection` JVM 옵션으로 이 기능을 완전히 끄고 런타임 오버헤드를 제거할 수 있음.

### `io.netty.util.concurrent`

새 독립 buffer API와 함께, 4.0은 일반적으로 비동기 애플리케이션 작성에 유용한 다양한 구성 요소를 `io.netty.util.concurrent`라는 새 패키지에 제공. 그중 일부는 다음과 같음.

* `Future`와 `Promise` - `ChannelFuture`와 비슷하지만 `Channel`에 의존하지 않음
* `EventExecutor`와 `EventExecutorGroup` - 범용 이벤트 루프 API

이들은 이 문서 뒷부분에서 설명할 channel API의 기반으로 사용됨. 예를 들어 `ChannelFuture`는 `io.netty.util.concurrent.Future`를 상속하고, `EventLoopGroup`은 `EventExecutorGroup`을 상속.

![Event loop type hierarchy diagram](https://github.com/netty/netty.github.com/raw/master/images/concurrent.png)

### Channel API 변경

4.0에서 `io.netty.channel` 패키지의 많은 클래스가 대대적으로 개편됨 → 단순한 텍스트 search-and-replace만으로는 3.x 애플리케이션을 4.0에서 동작시킬 수 없음. 이 절은 변경 사항을 망라하기보다, 이런 큰 변화의 배경 설명에 초점.

#### 개편된 ChannelHandler 인터페이스

##### Upstream → Inbound, Downstream → Outbound

'upstream'과 'downstream'이라는 용어는 초보자에게 꽤 헷갈렸음. 4.0은 가능한 곳마다 'inbound'와 'outbound'를 사용.

##### 새 `ChannelHandler` 타입 계층

3.x에서 `ChannelHandler`는 단순한 태그 인터페이스였고, `ChannelUpstreamHandler`, `ChannelDownstreamHandler`, `LifeCycleAwareChannelHandler`가 실제 핸들러 메서드를 정의. Netty 4에서는 `ChannelHandler`가 `LifeCycleAwareChannelHandler`를 흡수하면서, inbound·outbound 양쪽에 유용한 몇 가지 메서드도 함께 가짐.

```java
public interface ChannelHandler {
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
```

다음 다이어그램이 새 타입 계층을 보여줌.

![ChannelHandler type hierarchy diagram](https://github.com/netty/netty.github.com/raw/master/images/handler.png)

##### 이벤트 객체 없는 `ChannelHandler`

3.x에서는 모든 I/O 연산이 `ChannelEvent` 객체를 만들었음. read/write마다 추가로 새 `ChannelBuffer`도 만들었음. 이는 자원 관리와 버퍼 풀링을 JVM에 위임하므로 Netty 내부를 꽤 단순화시켰지만, 부하가 높은 Netty 기반 애플리케이션에서 종종 관찰되는 GC 압박과 불확실성의 근본 원인이 되곤 했음.

4.0은 이벤트 객체를 강타입 메서드 호출로 대체 → 이벤트 객체 생성을 거의 완전히 제거. 3.x에는 `handleUpstream()`이나 `handleDownstream()` 같은 catch-all 이벤트 핸들러 메서드가 있었지만, 더 이상 없음. 모든 이벤트 타입이 자기만의 핸들러 메서드를 가짐.

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

`ChannelHandlerContext`도 위 변경에 맞춰 바뀜.

```java
// Before:
ctx.sendUpstream(evt);
 
// After:
ctx.fireChannelRead(receivedMessage);
```

이 변경들은 사용자가 더 이상 존재하지 않는 `ChannelEvent` 인터페이스를 상속할 수 없음을 의미. 그렇다면 사용자가 `IdleStateEvent` 같은 자기만의 이벤트 타입은 어떻게 정의할까. 4.0의 `ChannelHandlerContext`는 커스텀 이벤트를 트리거하는 `fireUserEventTriggered` 메서드를 제공하고, `ChannelInboundHandler`는 커스텀 이벤트 처리 전용 핸들러 메서드 `userEventTriggered()`를 가짐.

##### 단순화된 채널 상태 모델

3.x에서 새 연결된 `Channel`이 만들어질 때 최소 세 개의 `ChannelStateEvent`(`channelOpen`, `channelBound`, `channelConnected`)가 트리거됨. `Channel`이 닫힐 때도 최소 세 개(`channelDisconnected`, `channelUnbound`, `channelClosed`)가 트리거됨.

![Netty 3 Channel state diagram](https://github.com/netty/netty.github.com/raw/master/images/state_3.png)

그러나 그렇게 많은 이벤트를 트리거할 가치는 의심스러움. 사용자에게는 `Channel`이 read/write를 수행할 수 있는 상태가 되었을 때 알림을 받는 것이 더 유용.

![Netty 4 Channel state diagram](https://github.com/netty/netty.github.com/raw/master/images/state_small_4.png)

`channelOpen`, `channelBound`, `channelConnected`는 `channelActive`로 통합. `channelDisconnected`, `channelUnbound`, `channelClosed`는 `channelInactive`로 통합. 마찬가지로 `Channel.isBound()`와 `isConnected()`는 `isActive()`로 통합됨.

`channelRegistered`와 `channelUnregistered`는 `channelOpen`과 `channelClosed`의 동등물이 아님에 유의. 다음 그림처럼 `Channel`의 동적 등록·등록 해제·재등록을 지원하기 위해 도입된 새 상태.

![Netty 4 Channel state diagram for re-registration](https://github.com/netty/netty.github.com/raw/master/images/state_4.png)

##### `write()`는 자동으로 flush하지 않음

4.0은 `Channel`의 outbound 버퍼를 명시적으로 flush하는 `flush()` 연산을 새로 도입했고, `write()` 연산은 자동으로 flush하지 않음. 메시지 단위로 동작하는 `java.io.BufferedOutputStream`이라고 생각하면 됨.

이 변경 때문에 무언가를 쓴 뒤 `ctx.flush()` 호출을 잊지 않도록 매우 주의 필요. 또는 단축 메서드 `writeAndFlush()` 사용 가능.

#### 합리적이고 오류가 적은 inbound 트래픽 일시 중지

3.x에는 `Channel.setReadable(boolean)`이 제공하는 직관적이지 않은 inbound 트래픽 일시 중지 메커니즘이 있었음. ChannelHandler 사이의 복잡한 상호작용을 만들었고, 잘못 구현하면 핸들러끼리 쉽게 간섭할 수 있었음.

4.0은 `read()`라는 새 outbound 연산을 추가. `Channel.config().setAutoRead(false)`로 기본 auto-read 플래그를 끄면, Netty는 사용자가 명시적으로 `read()` 연산을 호출하기 전까지 아무것도 읽지 않음. 사용자가 발행한 `read()` 연산이 완료되어 채널이 다시 읽기를 멈추면 inbound 이벤트 `channelReadSuspended()`가 트리거되며, 사용자는 또 다른 `read()`를 발행할 수 있음. `read()` 연산을 가로채 더 고급의 트래픽 제어도 가능.

##### 들어오는 연결 수락 일시 중지

3.x에서는 I/O 스레드를 블록하거나 서버 소켓을 닫는 것 외에 들어오는 연결 수락을 멈추라고 Netty에 알릴 방법이 없었음. 4.0은 일반 채널과 마찬가지로 auto-read 플래그가 설정되지 않았을 때 `read()` 연산을 존중.

#### Half-closed 소켓

TCP와 SCTP는 사용자가 소켓을 완전히 닫지 않고도 outbound 트래픽을 종료할 수 있게 해줌. 그런 소켓을 'half-closed 소켓'이라고 부르며, `SocketChannel.shutdownOutput()` 메서드로 만들 수 있음. 원격 피어가 outbound 트래픽을 종료하면 `SocketChannel.read(..)`는 `-1`을 반환하는데, 닫힌 연결과 겉으로는 구분이 되지 않았음.

3.x에는 `shutdownOutput()` 연산이 없었음. 또한 `SocketChannel.read(..)`가 `-1`을 반환하면 항상 연결을 닫았음.

half-closed 소켓을 지원하기 위해 4.0은 `SocketChannel.shutdownOutput()` 메서드를 추가했고, 사용자는 `ALLOW_HALF_CLOSURE` `ChannelOption`을 설정해 `SocketChannel.read(..)`가 `-1`을 반환해도 Netty가 자동으로 연결을 닫지 않도록 할 수 있음.

#### 유연한 I/O 스레드 할당

3.x에서는 `ChannelFactory`가 `Channel`을 만들고, 새로 만든 `Channel`이 자동으로 숨은 I/O 스레드에 등록됨. 4.0은 `ChannelFactory`를 하나 이상의 `EventLoop`로 구성된 새 인터페이스 `EventLoopGroup`으로 대체. 또한 새 `Channel`은 자동으로 `EventLoopGroup`에 등록되지 않으며, 사용자가 `EventLoopGroup.register()`를 명시적으로 호출해야 함.

이 변경 덕분에(`ChannelFactory`와 I/O 스레드의 분리), 같은 `EventLoopGroup`에 서로 다른 `Channel` 구현을 등록하거나, 같은 `Channel` 구현을 서로 다른 `EventLoopGroup`에 등록 가능. 예를 들어 NIO 서버 소켓, NIO 클라이언트 소켓, NIO UDP 소켓, in-VM 로컬 채널을 같은 I/O 스레드에서 실행할 수 있음. 지연 최소화가 필요한 프록시 서버 작성 시 매우 유용.

#### 기존 JDK 소켓에서 `Channel` 만들기

3.x는 `java.nio.channels.SocketChannel` 같은 기존 JDK 소켓에서 새 Channel을 만들 방법이 없었음. 4.0에서는 가능.

#### I/O 스레드에서 `Channel` 등록 해제와 재등록

3.x에서 새 `Channel`이 만들어지면 그 기반 소켓이 닫힐 때까지 단일 I/O 스레드에 완전히 묶임. 4.0에서 사용자는 `Channel`을 자기 I/O 스레드에서 등록 해제하여 기반 JDK 소켓을 완전히 통제할 수 있음. 예를 들어 Netty가 제공하는 고수준 non-blocking I/O를 활용해 복잡한 프로토콜을 다루다가 나중에 `Channel`을 등록 해제하고 blocking 모드로 전환해 가능한 한 최대 처리량으로 파일을 전송할 수 있음. 물론 등록 해제된 `Channel`을 다시 등록도 가능.

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

#### I/O 스레드에서 임의의 태스크 스케줄링

`Channel`이 `EventLoopGroup`에 등록되면, 실제로는 `EventLoopGroup`이 관리하는 `EventLoop` 중 하나에 등록됨. `EventLoop`는 `java.util.concurrent.ScheduledExecutorService`를 구현. 즉, 사용자는 자신의 채널이 속한 I/O 스레드에서 임의의 `Runnable`이나 `Callable`을 실행하거나 스케줄링 가능. 뒤에서 설명할 잘 정의된 새 스레드 모델과 함께라면 스레드 안전한 핸들러 작성이 훨씬 쉬워짐.

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

#### 단순화된 종료

이제 `releaseExternalResources()`는 없음. `EventLoopGroup.shutdownGracefully()`를 호출하면 모든 열려 있는 채널을 즉시 닫고 모든 I/O 스레드가 스스로 멈추도록 만들 수 있음.

#### 타입 안전한 `ChannelOption`

Netty에서 `Channel`의 소켓 파라미터를 설정하는 방법은 두 가지. 하나는 `SocketChannelConfig.setTcpNoDelay(true)`처럼 `ChannelConfig`의 setter를 직접 호출하는 것으로, 가장 타입 안전한 방법. 다른 하나는 `ChannelConfig.setOption()` 메서드를 호출하는 것으로, 어떤 소켓 옵션을 설정해야 할지 런타임에 결정해야 할 때 이상적. 그러나 3.x에서는 옵션을 (문자열, 객체) 쌍으로 지정해야 해서 오류가 잦았음. 옵션 이름이나 값을 잘못 지정하면 `ClassCastException`을 만나거나, 지정한 옵션이 조용히 무시되기도 했음.

4.0은 소켓 옵션에 대한 타입 안전한 접근을 제공하는 새 타입 `ChannelOption`을 도입.

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

#### AttributeMap

사용자 요청에 따라 `Channel`과 `ChannelHandlerContext`에 임의의 객체를 붙일 수 있게 됨. `Channel`과 `ChannelHandlerContext`가 상속하는 새 인터페이스 `AttributeMap`이 추가되었고, 그 대신 `ChannelLocal`과 `Channel.attachment`는 제거됨. attribute는 연관된 `Channel`이 가비지 컬렉트될 때 함께 가비지 컬렉트됨.

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

#### 새 bootstrap API

bootstrap API는 목적은 같지만 처음부터 다시 작성됨. 보일러플레이트에서 흔히 보이는, 서버 또는 클라이언트를 구동하기 위한 일반적인 단계를 수행.

새 bootstrap도 fluent 인터페이스를 사용.

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

##### `ChannelPipelineFactory` → `ChannelInitializer`

위 예제에서 알 수 있듯이 `ChannelPipelineFactory`는 더 이상 없음. `Channel`과 `ChannelPipeline` 설정을 더 잘 제어할 수 있게 해주는 `ChannelInitializer`로 대체됨.

새 `ChannelPipeline`은 사용자가 직접 만들지 않는다는 점에 유의. 다양한 사용 사례를 관찰한 결과, Netty 프로젝트 팀은 사용자가 직접 파이프라인 구현을 만들거나 기본 구현을 상속하는 것에 어떤 이점도 없다는 결론. 따라서 `ChannelPipeline`은 사용자가 직접 생성하지 않으며, `Channel`이 자동으로 생성.

#### `ChannelFuture` → `ChannelFuture`와 `ChannelPromise`

`ChannelFuture`는 `ChannelFuture`와 `ChannelPromise`로 분할됨. 비동기 연산의 소비자와 생산자의 계약을 명확히 할 뿐만 아니라, `ChannelFuture`의 상태를 변경할 수 없게 만들어 (필터링 같은) 체인에서 반환된 `ChannelFuture`를 더 안전하게 사용할 수 있게 함.

이 변경으로 일부 메서드는 상태 변경을 위해 `ChannelFuture`가 아닌 `ChannelPromise`를 받음.

### 잘 정의된 스레드 모델

3.x에는 잘 정의된 스레드 모델이 없었으며, 3.5에서 일관성을 고치려는 시도가 있었음. 4.0은 사용자가 스레드 안전성에 대해 너무 걱정하지 않고도 ChannelHandler를 작성할 수 있게 도와주는 엄격한 스레드 모델을 정의.

* `ChannelHandler`가 `@Sharable`로 애노테이션되지 않는 한, Netty는 절대로 그 메서드를 동시(concurrently)로 호출하지 않음. 핸들러 메서드의 종류(inbound, outbound, life cycle 이벤트)와 무관.
  * 사용자는 더 이상 inbound·outbound 이벤트 핸들러 메서드를 동기화할 필요 없음.
  * 4.0은 `@Sharable`로 애노테이션되지 않은 `ChannelHandler`를 두 번 이상 추가하는 것을 허용하지 않음.
* Netty가 만드는 각 `ChannelHandler` 메서드 호출 사이에는 항상 [happens-before](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html#MemoryVisibility) 관계가 있음.
  * 사용자는 핸들러의 상태를 보존하기 위해 `volatile` 필드를 정의할 필요 없음.
* 사용자는 `ChannelPipeline`에 핸들러를 추가할 때 `EventExecutor`를 지정할 수 있음.
  * 지정한 경우 그 `ChannelHandler`의 핸들러 메서드는 항상 지정된 `EventExecutor`에 의해 호출됨.
  * 지정하지 않은 경우 핸들러 메서드는 연관된 `Channel`이 등록된 `EventLoop`에 의해 호출됨.
* 핸들러나 채널에 할당된 `EventExecutor`/`EventLoop`은 항상 단일 스레드.
  * 핸들러 메서드는 항상 동일한 스레드에 의해 호출됨.
  * 멀티스레드 `EventExecutor`/`EventLoop`이 지정되면 스레드 중 하나가 먼저 선택되고, 등록 해제 전까지 그 스레드만 사용됨.
  * 같은 파이프라인의 두 핸들러에 서로 다른 `EventExecutor`가 할당되면 동시에 호출됨. 같은 파이프라인 내 핸들러들만 공유 데이터에 접근하더라도, 한 명 이상의 핸들러가 그것에 접근한다면 스레드 안전성 주의 필요.
* `ChannelFuture`에 추가된 `ChannelFutureListeners`는 항상 그 future의 연관 `Channel`에 할당된 `EventLoop` 스레드에 의해 호출됨.
* `ChannelHandlerInvoker`는 `Channel` 이벤트의 순서를 제어하는 데 사용 가능. `DefaultChannelHandlerInvoker`는 `EventLoop` 스레드에서 온 이벤트는 즉시 실행하고, 다른 스레드에서 온 이벤트는 `EventExecutor`에 `Runnable` 객체로 실행. 아래 예제는 `EventLoop` 스레드와 다른 스레드에서 채널과 상호작용할 때의 함의를 보여줌. (이 기능은 이후 제거됨. [관련 커밋](https://github.com/netty/netty/commit/68cd670eb9b8827b4d3b7602f4ea8e14c38691ac#diff-6e08f48c25a292a7612f9d6941e67303) 참고)

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

#### `ExecutionHandler`는 더 이상 없음 - 코어에 들어감

`ChannelPipeline`에 `ChannelHandler`를 추가할 때 `EventExecutor`를 지정하면, 추가된 `ChannelHandler`의 핸들러 메서드를 항상 그 `EventExecutor`를 통해 호출하라고 파이프라인에 알릴 수 있음.

```java
Channel ch = ...;
ChannelPipeline p = ch.pipeline();
EventExecutor e1 = new DefaultEventExecutor(16);
EventExecutor e2 = new DefaultEventExecutor(8);
 
p.addLast(new MyProtocolCodec());
p.addLast(e1, new MyDatabaseAccessingHandler());
p.addLast(e2, new MyHardDiskAccessingHandler());
```

### 코덱 프레임워크 변경

4.0은 핸들러가 자신의 버퍼를 만들고 관리하도록 요구하므로(이 문서의 Per-handler buffer 섹션 참고), 코덱 프레임워크에는 상당한 내부 변경이 있었음. 다만 사용자 입장에서의 변경은 그리 크지 않음.

* 핵심 코덱 클래스는 `io.netty.handler.codec` 패키지로 이동
* `FrameDecoder`는 `ByteToMessageDecoder`로 이름 변경
* `OneToOneEncoder`와 `OneToOneDecoder`는 `MessageToMessageEncoder`와 `MessageToMessageDecoder`로 대체
* `decode()`, `decodeLast()`, `encode()`의 메서드 시그니처는 제네릭 지원과 중복 파라미터 제거를 위해 약간 변경

#### Codec embedder → `EmbeddedChannel`

Codec embedder는 코덱을 포함한 모든 종류의 파이프라인을 테스트할 수 있게 해주는 `io.netty.channel.embedded.EmbeddedChannel`로 대체됨.

#### HTTP 코덱

HTTP 디코더는 이제 단일 HTTP 메시지에 대해 항상 여러 메시지 객체를 만듦.

```
1       * HttpRequest / HttpResponse
0 - n   * HttpContent
1       * LastHttpContent
```

자세한 내용은 갱신된 `HttpSnoopServer` 예제 참고. 단일 HTTP 메시지에 대해 여러 메시지를 다루고 싶지 않다면 파이프라인에 `HttpObjectAggregator`를 넣으면 됨. `HttpObjectAggregator`는 여러 메시지를 단일 `FullHttpRequest`나 `FullHttpResponse`로 변환.

#### 전송 구현 변경

다음 전송이 새로 추가됨.

* OIO SCTP 전송
* UDT 전송

### 사례 연구: Factorial 예제 포팅

이 절은 Factorial 예제를 3.x에서 4.0으로 포팅하는 대략적 단계를 보여줌. Factorial 예제는 이미 `io.netty.example.factorial` 패키지에서 4.0으로 포팅됨. 모든 변경 사항은 예제 소스 코드에서 확인 가능.

#### 서버 포팅

1. 새 bootstrap API를 사용하도록 `FactorialServer.run()` 메서드를 다시 작성.
   1. `ChannelFactory`는 더 이상 없음. 직접 `NioEventLoopGroup`을 인스턴스화(들어오는 연결 수락용 하나, 수락된 연결 처리용 하나).
2. `FactorialServerPipelineFactory`를 `FactorialServerInitializer`로 이름 변경.
   1. `ChannelInitializer<Channel>`을 상속하게 변경.
   2. 새 `ChannelPipeline`을 만드는 대신 `Channel.pipeline()`으로 가져옴.
3. `FactorialServerHandler`가 `ChannelInboundHandlerAdapter`를 상속하게 변경.
   1. `channelDisconnected()`를 `channelInactive()`로 대체.
   2. `handleUpstream()`은 더 이상 사용되지 않음.
   3. `messageReceived()`를 `channelRead()`로 이름 변경하고 메서드 시그니처를 그에 맞게 조정.
   4. `ctx.write()`를 `ctx.writeAndFlush()`로 대체.
4. `BigIntegerDecoder`가 `ByteToMessageDecoder<BigInteger>`를 상속하게 변경.
5. `NumberEncoder`가 `MessageToByteEncoder<Number>`를 상속하게 변경.
   1. `encode()`는 더 이상 버퍼를 반환하지 않음. `MessageToByteEncoder`가 제공하는 버퍼에 인코딩된 데이터를 채움.

#### 클라이언트 포팅

대부분 서버 포팅과 동일하지만, 잠재적으로 큰 스트림을 쓸 때는 주의 필요.

1. 새 bootstrap API를 사용하도록 `FactorialClient.run()` 메서드를 다시 작성.
2. `FactorialClientPipelineFactory`를 `FactorialClientInitializer`로 이름 변경.
3. `FactorialClientHandler`가 `ChannelInboundHandlerAdapter`를 상속하게 변경.

---

## Netty 4.1의 새로운 기능과 주요 변경사항

> 원본: https://netty.io/wiki/new-and-noteworthy-in-4.1.html

---

### TL;DR

4.0과의 후방 호환성을 최대한 유지했지만, 4.1에는 4.0과 완전히 호환되지 않을 수 있는 변경사항이 일부 포함됨. 새 버전에 맞춰 애플리케이션을 반드시 다시 컴파일할 것.

재컴파일 시 deprecation 경고가 나타날 수 있음. 권장 대안으로 모두 수정해 두면 다음 버전 업그레이드 시 마이그레이션이 수월.

### 코어 변경

#### Android 지원

다음 이유로 Android(4.0 이상)를 공식 지원하기로 결정.

* 모바일 기기가 점점 강력해짐
* ADK의 NIO와 SSLEngine에서 알려진 주요 문제들이 Ice Cream Sandwich 이후 수정됨
* 코덱과 핸들러를 모바일 애플리케이션에서도 재사용하고 싶어 하는 수요가 분명히 존재

다만 Android용 자동화 테스트 스위트는 아직 없음. Android에서 문제를 발견하면 [이슈를 등록](https://github.com/netty/netty/issues). Android 테스트를 빌드 프로세스에 포함시키는 기여도 환영.

#### `ChannelHandlerContext.attr(..)` == `Channel.attr(..)`

[`Channel`]과 [`ChannelHandlerContext`]는 모두 [`AttributeMap`] 인터페이스를 구현해 사용자 정의 속성을 붙일 수 있음. 그런데 두 객체가 각각 별도의 속성 저장소를 가지고 있어 혼란이 발생했음. 예를 들어 `Channel.attr(KEY_X).set(valueX)`로 설정한 속성을 `ChannelHandlerContext.attr(KEY_X).get()`으로는 조회할 수 없었고, 그 반대도 마찬가지. 이 동작은 혼란스러울 뿐 아니라 메모리 낭비이기도 했음.

이 문제를 해결하기 위해 [`Channel`]당 내부적으로 단 하나의 맵만 유지하도록 변경. [`AttributeMap`]은 항상 [`AttributeKey`]를 키로 사용하며, [`AttributeKey`]가 유일성을 보장하므로 [`Channel`]당 속성 맵이 두 개 이상 필요 없음. [`AttributeKey`]를 [`ChannelHandler`]의 private static final 필드로 정의하면 키 충돌 위험 없음.

#### `Channel.hasAttr(...)`

이제 속성이 존재하는지 효율적으로 확인 가능.

#### 더 쉽고 정확한 버퍼 누수 추적

이전에는 버퍼 누수 발생 위치를 추적하기 어려웠고 누수 경고도 충분한 정보를 제공하지 못했음. 이제는 약간의 오버헤드를 감수하고 활성화할 수 있는 고급 누수 보고 메커니즘이 추가됨.

자세한 내용은 [참조 카운트 객체](./08_reference_counted.md) 참고. 이 기능은 중요성 때문에 4.0.14.Final에도 백포트됨.

#### 기본 할당자 `PooledByteBufAllocator`

4.x에서는 한계가 있음에도 `UnpooledByteBufAllocator`가 기본 할당자였음. `PooledByteBufAllocator`가 충분히 검증되었고 고급 누수 추적 메커니즘도 갖춰졌으므로, 이제 기본값으로 변경.

#### 글로벌 유일 채널 ID

이제 모든 [`Channel`]은 다음 요소들을 조합해 생성되는 전역 유일 ID를 가짐.

* MAC 주소(EUI-48 또는 EUI-64), 가능하면 글로벌 유일한 것
* 현재 프로세스 ID
* `System#currentTimeMillis()`
* `System#nanoTime()`
* 무작위 32비트 정수
* 순차적으로 증가하는 32비트 정수

[`Channel`]의 ID는 `Channel.id()` 메서드로 얻을 수 있음.

#### `EmbeddedChannel` 사용성

[`EmbeddedChannel`]의 `readInbound()`와 `readOutbound()`가 제네릭 타입 파라미터로 반환값을 추론하므로 다운캐스트 불필요. 단위 테스트 코드의 장황함이 크게 줄어듦.

```java
EmbeddedChannel ch = ...;

// BEFORE:
FullHttpRequest req = (FullHttpRequest) ch.readInbound();

// AFTER:
FullHttpRequest req = ch.readInbound();
```

#### `ThreadFactory` 대신 `Executor` 사용 가능

일부 애플리케이션은 특정 `Executor`에서 태스크를 실행해야 함. 4.x에서는 이벤트 루프 생성 시 `ThreadFactory`만 지정할 수 있었지만, 이제는 `Executor`를 직접 지정 가능.

자세한 내용은 [PR #1762](https://github.com/netty/netty/pull/1762) 참고.

#### 클래스 로더 친화성

`AttributeKey` 같은 일부 타입은 컨테이너 환경에서 문제가 있었지만, 이제는 클래스 로더를 올바르게 인식하도록 개선됨.

#### `ByteBufAllocator.calculateNewCapacity()`

`ByteBuf` 확장 시 새 용량을 계산하는 로직이 `AbstractByteBuf`에서 `ByteBufAllocator`로 이동. 버퍼 용량 계산은 해당 버퍼를 관리하는 `ByteBufAllocator`가 더 잘 알기 때문.

### 새 코덱과 핸들러

* 바이너리 memcache 프로토콜 코덱
* 압축 코덱
  * BZip2
  * FastLZ
  * LZ4
  * LZF
* DNS 프로토콜 코덱
* HAProxy 프로토콜 코덱
* MQTT 프로토콜 코덱
* SPDY/3.1 지원
* STOMP 코덱
* 버전 4, 4a, 5를 지원하는 SOCKSx 코덱. `socksx` 패키지 참고.
* XML 문서 스트리밍을 가능하게 하는 [`XmlFrameDecoder`].
* JSON 객체 스트리밍을 가능하게 하는 [`JsonObjectDecoder`].
* IP 필터링 핸들러

### 그 외 코덱 변경

#### `AsciiString`

[`AsciiString`]은 1바이트 문자만 담는 새로운 `CharSequence` 구현. US-ASCII나 ISO-8859-1 문자열을 다룰 때 유용.

Netty의 HTTP 코덱과 STOMP 코덱은 헤더 이름을 표현하는 데 `AsciiString`을 사용. `AsciiString`은 `ByteBuf`로 인코딩할 때 변환 비용이 없으므로 `String`보다 나은 성능을 제공.

#### `TextHeaders`

[`TextHeaders`]는 HTTP 헤더 같은 문자열 [multimap](http://en.wikipedia.org/wiki/Multimap)을 위한 범용 자료구조를 제공. `HttpHeaders`도 `TextHeaders`로 다시 작성됨.

#### `MessageAggregator`

[`MessageAggregator`]는 `HttpObjectAggregator`처럼 작은 여러 메시지를 더 큰 하나의 메시지로 집계하는 범용 기능을 제공. `HttpObjectAggregator`도 `MessageAggregator`로 다시 작성됨.

#### `HttpObjectAggregator`의 더 나은 oversized 메시지 처리

4.0에서는 클라이언트가 `100-continue` 헤더를 보내더라도, 클라이언트가 본문을 전송하기 전에 oversized HTTP 메시지를 거부할 방법이 없었음.

이번 릴리스에서는 오버라이드 가능한 `handleOversizedMessage` 메서드를 추가해 사용자가 원하는 처리를 직접 구현할 수 있게 함. 기본 동작은 '413 Request Entity Too Large' 응답 후 연결을 닫는 것.

#### `ChunkedInput`과 `ChunkedWriteHandler`

[`ChunkedInput`]에는 두 개의 새 메서드 `progress()`와 `length()`가 있어, 각각 전송 진행 상태와 스트림 전체 길이를 반환. [`ChunkedWriteHandler`]는 이 정보를 사용해 [`ChannelProgressiveFutureListener`]에 알림.

#### `SnappyFramedEncoder`와 `SnappyFramedDecoder`

이 두 클래스는 `SnappyFrameEncoder`와 `SnappyFrameDecoder`로 이름이 변경됨. 기존 클래스는 deprecated로 표시되었으며, 내부적으로 새 클래스의 서브클래스로 구현되어 있음.

[`AttributeKey`]: http://netty.io/4.1/api/io/netty/util/AttributeKey.html
[`AttributeMap`]: http://netty.io/4.1/api/io/netty/util/AttributeMap.html
[`EventExecutor`]: http://netty.io/4.1/api/io/netty/util/concurrent/EventExecutor.html

[`Channel`]: http://netty.io/4.1/api/io/netty/channel/Channel.html
[`ChannelHandler`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandler.html
[`ChannelHandlerAdapter`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandlerAdapter.html
[`ChannelHandlerContext`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandlerContext.html
[`ChannelHandlerInvoker`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandlerInvoker.html
[`ChannelPipeline`]: http://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html

[`SimpleChannelInboundHandler`]: http://netty.io/4.1/api/io/netty/channel/SimpleChannelInboundHandler.html

[`EmbeddedChannel`]: http://netty.io/4.1/api/io/netty/channel/embedded/EmbeddedChannel.html

[`JsonObjectDecoder`]: http://netty.io/4.1/api/io/netty/handler/codec/json/JsonObjectDecoder.html
[`XmlFrameDecoder`]: http://netty.io/4.1/api/io/netty/handler/codec/xml/XmlFrameDecoder.html

[`AsciiString`]: http://netty.io/4.1/api/io/netty/handler/codec/AsciiString.html
[`TextHeaders`]: http://netty.io/4.1/api/io/netty/handler/codec/TextHeaders.html
[`MessageAggregator`]: http://netty.io/4.1/api/io/netty/handler/codec/MessageAggregator.html

[`ChunkedInput`]: http://netty.io/4.1/api/io/netty/handler/stream/ChunkedInput.html
[`ChunkedWriteHandler`]: http://netty.io/4.1/api/io/netty/handler/stream/ChunkedWriteHandler.html
[`ChannelProgressiveFutureListener`]: http://netty.io/4.1/api/io/netty/channel/ChannelProgressiveFutureListener.html

---

## Netty 4.2 마이그레이션 가이드

> 원본: https://netty.io/wiki/netty-4.2-migration-guide.html

---

## Netty 4.2 Migration Guide

Netty 4.2는 Netty 4.1과 대부분 후방 호환되지만, 알아둘 필요가 있는 몇 가지 호환성 변경과 알아두면 좋은 새 기능 몇 가지가 있음.

가장 큰 변경 중 하나는 최소 Java 버전 요구사항을 Java 6에서 Java 8로 올린 것. Netty 팀 입장에서는 큰 개선이지만, Java 8 미만을 사용하는 유지보수 프로젝트가 거의 없으므로 대부분의 사용자에게는 영향 없음.

### 권장 업그레이드 절차

다음은 실전 경험을 바탕으로 정리한 Netty 4.2 업그레이드 권장 절차. 다양한 시스템에 적용할 수 있도록 고수준·범용으로 기술 → 이 가이드의 다른 절도 함께 읽어 자신의 시스템과 관련된 세부 사항을 확인할 것.

시작 전에 의존성 관리 모범 사례를 따르길 권장. 특히 shading은 Maven/Gradle 같은 의존성 관리 도구로부터 의존성을 숨기므로 가능하면 회피. 또한 `netty-bom` 같은 BOM 파일을 사용해 의존성 버전을 일관되게 관리하면 런타임에 버전이 뒤섞이는 문제 방지 가능.

Netty 4.2 업그레이드는 시스템 규모와 복잡도에 따라 여러 스프린트와 배포 사이클이 걸릴 수 있으므로, 완료에 필요한 시간과 자원을 미리 계획할 것.

Netty 4.2로 업그레이드하는 권장 단계는 다음과 같음.

1. 최신 Netty 4.1.x로 업그레이드
   - Netty 4.2 업그레이드를 시작하기 전에, 시스템이 최신 4.1.x에서 정상 동작하는지 확인.
2. 클라이언트 TLS 연결에 대해 endpoint validation을 명시적으로 설정
   - Netty 4.2에서 endpoint validation(즉, hostname 검증)의 기본값이 변경됨. TLS 클라이언트가 있다면 endpoint validation을 명시적으로 on(권장) 또는 off(틈새 사용 사례)로 설정.
   - endpoint validation은 반드시 Netty 4.1.112에서 추가된 [SslContextBuilder.endpointIdentificationAlgorithm(String)](https://netty.io/4.1/api/io/netty/handler/ssl/SslContextBuilder.html#endpointIdentificationAlgorithm-java.lang.String-) API로 설정해야 함.
3. 의존성에 대해서도 1, 2단계를 반복
   - 시스템의 모든 컴포넌트가 같은 Netty 버전을 사용하고 endpoint validation을 올바르게 설정하도록 하는 것이 중요.
   - 이유 1: endpoint validation을 잘못 설정한 오래된 클라이언트 라이브러리가 Netty 4.2 업그레이드 후 동작을 멈추는 상황 방지.
   - 이유 2: Netty 4.1과 4.2는 클래스패스에 공존할 수 없으므로 한 번에 업그레이드해야 함.
4. 시스템이 pooled 할당자를 사용하도록 명시적으로 설정
   - Netty 4.2는 기본 할당자를 adaptive로 변경. 이 변경의 위험을 줄이려면 먼저 4.2로 업그레이드하면서 명시적으로 pooled 할당자를 설정하고, 두 번째 단계로 그 설정을 제거하는 것을 권장.
   - pooled 할당자를 명시 설정하려면 시스템 프로퍼티 `io.netty.allocator.type=pooled`를 설정.
5. 위 변경을 적용한 시스템을 배포해 정상 동작 확인
   - 1~4단계의 변경은 한 번에 배포 가능. 다만 라이브러리·프레임워크가 자체 업그레이드·릴리스 사이클을 가질 수 있다는 점은 유의.
   - 이 시점에서 시스템은 여전히 Netty 4.1을 실행 중이지만 이제 4.2 업그레이드 준비 완료.
6. 최신 Netty 4.2.x로 업그레이드하고 배포
   - 이 단계에서는 단순히 Netty 버전을 마지막 4.2.x로 올리고 롤아웃.
   - 빌드를 점검해 Netty 4.1 의존성이 남아 있지 않은지 확인.
   - 빌드 파이프라인과 배포가 롤아웃되면서 TLS 클라이언트의 endpoint validation 관련 오류를 주시 → 대부분 시스템에서 가장 큰 위험 요인.
   - 모두 잘 보이면, adaptive 할당자에 관심이 없는 경우 여기서 멈춰도 됨. 그렇지 않다면 다음 단계로 진행.
7. adaptive 할당자로 전환
   - adaptive 할당자가 새 기본값이지만 모두에게 최선은 아님. 어떤 워크로드에서는 더 좋고 어떤 워크로드에서는 더 나쁨.
   - 부하 테스트나 soak 테스트 환경이 있다면 adaptive 할당자를 거기서 먼저 시도. 단순히 4단계에서 추가한 `io.netty.allocator.type=pooled` 시스템 프로퍼티를 제거하면 됨.
   - 좋아 보이면 환경을 거쳐 운영까지 변경을 배포하면서, 변경이 롤아웃되는 동안 heap·direct 메모리 사용량과 GC 활동을 주시.
   - adaptive 할당자에서 메모리 사용량이 크게 증가한다면, [Netty Allocator Events.jfc](https://github.com/netty/netty/blob/4.2/microbench/src/main/resources/Netty%20Allocator%20Events.jfc) 기록 프로파일을 사용해 flight recording을 얻고 그 기록을 첨부해 이슈를 등록.
   - adaptive 할당자가 자기 워크로드에서 성능이 나쁘다면, adaptive를 개선하는 동안 pooled 할당자에 머무르면 됨.

위 단계를 따르면 큰 깜짝 상황 없이 Netty 4.2로 성공적으로 업그레이드할 가능성이 높아짐.

### 호환성 주요 사항

가장 중요한 호환성 변경. Netty 팀은 여러 대규모·고프로파일 프로젝트에서 4.2를 광범위하게 검증 테스트했고, 통합자가 Netty 의존성을 4.1에서 4.2로 업그레이드할 때 가장 자주 마주치는 사항은 다음과 같음.

주의: 클라이언트 TLS 연결에 대해 hostname verification이 기본 활성화됨.

`SslContextBuilder.endpointIdentificationAlgorithm` 설정의 기본값이 4.1에서는 `null`이었지만 이제 `HTTPS`로 설정됨. hostname verification을 하지 않는 것은 더 순진했던 시절의 구식이고 안전하지 않은 관행이지만, 갑자기 활성화하면 많은 시스템이 깨질 수 있어 그동안 변경하지 못했음.

이 기본값은 시스템 프로퍼티를 통해 Netty 4.1 동작으로 되돌릴 수 있음.

```
io.netty.handler.ssl.defaultEndpointVerificationAlgorithm=NONE
```

이 override는 Netty 4.2 마이그레이션 과도기를 위한 임시 수단. endpoint identification을 의도적으로 비활성화해야 하는 경우에는 `SslContextBuilder` 옵션으로 명시적으로 설정해야 함.

주의: `adaptive` 메모리 할당자가 새 기본값임.

Netty 4.1에서 `adaptive` 메모리 할당자는 실험적이었음. 이제 `pooled` 대신 새 기본값으로 변경.

대부분의 워크로드에서 메모리 사용량이 줄어들고 성능은 pooled 할당자와 비슷하거나 약간 더 좋을 것으로 예상. adaptive 할당자는 실제 워크로드에 맞게 자동으로 튜닝되며, 처음부터 가상 스레드와 잘 동작하도록 설계됨.

다만 애플리케이션마다 할당자를 사용하는 방식과 부하 패턴이 다르므로, 경우에 따라 adaptive 할당자가 더 나쁜 성능을 보일 수 있음. Netty 4.1과 동일하게 pooled 할당자를 계속 사용하려면 다음 시스템 프로퍼티로 기본 할당자를 변경 가능.

```
io.netty.allocator.type=pooled
```

주의: BouncyCastle 업그레이드.

(여전히 선택적인) BouncyCastle 의존성을 갱신함. BouncyCastle이 자기 아티팩트의 버전을 매기는 방식 때문에 이는 호환성 변경.

BouncyCastle 의존성이 모두 `*-jdk15on` 변형에서 `*-jdk18on` 변형으로 변경됨.

예를 들어 Netty와 `bcprov-jdk15on`을 함께 의존하고 있다면, Netty 4.2 마이그레이션의 일환으로 의존하는 BouncyCastle 아티팩트 변경 필요.

또한 의존 버전을 1.69에서 1.80으로 올렸으며, 이 자체가 API에 호환성 변경을 도입.

주의: `netty-incubator-transport-io_uring` 모듈은 더 이상 지원되지 않음.

Netty 4.2에서 io_uring 전송을 incubator에서 졸업시켜 완전히 지원되는 first-class 전송 모듈로 만듦.

이 작업의 일환으로 Netty 전송 내부에 다수의 리팩토링을 했고, 그 결과 incubator 버전과는 완전히 호환되지 않음. 좋은 소식은 이 리팩토링 덕분에 `netty-transport-native-io_uring`이 훨씬 더 우수한 구현이라는 점.

통합자는 incubator 모듈 사용을 멈추고, 대신 우수한 io_uring 통합을 위해 Netty 4.2로 옮길 것을 권장. 그렇게 하려면 안타깝게도 코드 변경 필요. 다음 사항 주의.

* 패키지명이 `io.netty.incubator.channel.uring`에서 `io.netty.channel.uring`으로 변경
* 클래스명이 `IOUring`에서 `IoUring`으로 변경(소문자 'o'에 주의)
* 다수의 io_uring 채널 옵션이 변경됨
* `IOUringEventLoopGroup`은 더 이상 존재하지 않으며 `MultiThreadIoEventLoopGroup`과 `IoUringIoHandler`로 대체
* io_uring 사용은 이제 Java 9 이상 요구

### 그 외 호환성 변경

Netty 4.2에는 실제 문제로 이어질 가능성은 낮지만 알아두어야 할 호환성 변경이 다수 포함됨.

* `protobuf-java` 의존성이 2.6.1에서 3.25.5로 메이저 버전 업그레이드됨.
* `netty-codec` 모듈이 여러 서브 모듈로 분할되었으며, `netty-codec` 모듈은 그것들에 의존. 즉, `netty-codec`은 이제 단일 jar이 아니라 여러 jar 파일.
* 사용되지 않는 WebSocket draft 명세 지원을 제거함.
* Java 8 사용 시 ALPN이 기본 지원되므로 Jetty alpn/npn 지원을 제거함.
* 파이프라인 콜스택이 평탄화되어, 파이프라인을 통한 메시지 처리 시 스택 깊이가 줄어듦. 파이프라인이 동시에 수정되거나 child-executor를 사용하는 경우 메시지 처리 동작이 달라질 수 있음.
* 최소 GLibC 요구 버전이 2.12(2010-05, 예: CentOS 6)에서 2.17(2012-12, 예: CentOS 7)로 상향됨.
* tcnative 동적 링크 OpenSSL 통합 테스트를 OpenSSL 1.0.1e(2013) 대신 OpenSSL 1.0.2k(2017)로 진행 중.
* JPMS에서 automatic module 대신 real module로 전환함.

API 변경의 완전하고 망라된 목록은 RevAPI가 정리:
https://github.com/netty/netty/blob/3ca17a96cf84cbcb08776d3731b222b82814ead7/pom.xml#L1271-L7794

### 새 모범 사례

Netty 4.2는 일부 새 API를 도입하고 일부 기존 API를 deprecated 처리. Netty 4.2 마이그레이션의 일환으로, deprecated API 사용을 정리할 기회를 코드베이스에서 살펴보길 권장.

권장: EventLoopGroups를 위한 IoHandlerFactories

`NioEventLoopGroup`을 비롯한 모든 전송별 이벤트 루프 그룹이 deprecated 처리됨.

이제 통합자는 전송별 `IoHandlerFactory`를 `MultiThreadIoEventLoopGroup` 생성자에 전달해야 함.

예를 들어, 다음과 같이 하지 말 것(금지).

```
EventLoopGroup group = new NioEventLoopGroup(); // 금지
```

대신 다음과 같이 할 것(권장).

```
EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()); // 권장
```

각 전송에 대해 정적 `newFactory` 메서드가 있음.

* `NioIoHandler.newFactory()`
* `EpollIoHandler.newFactory()`
* `KQueueIoHandler.newFactory()`
* `IoUringIoHandler.newFactory()`
* `LocalIoHandler.newFactory()`

`Bootstrap`/`ServerBootstrap` 인스턴스에서 관련 채널이나 채널 팩토리는 여전히 설정해야 함에 유의.

권장: `MultiThreadIoEventLoopGroup`과 `SingleThreadIoEventLoop`의 확장성

`MultiThreadIoEventLoopGroup`과 `SingleThreadIoEventLoop` 클래스는 사용자가 오버라이드할 수 있는 다양한 메서드를 제공하며, 다음을 가능하게 함.

* 등록된 채널/핸들 수 확인
* IO 처리 시간 vs 태스크 처리 시간 이해
* 루프 실행당 처리된 채널/핸들 수 확인
* promise 커스터마이징/데코레이션

또한 올바른 `IoHandle` 타입을 구현한다면 채널 외의 것도 등록 가능 → Netty 컴포넌트를 재사용하거나 현재 존재하지 않는 `IoHandle` 구현을 추가할 가능성이 열림. 예를 들어 io_uring을 이용한 파일 IO 구현에 활용 가능.

권장: SelfSignedCertificate 대신 netty-pkitesting 사용

Netty 4.1에는 TLS 구현 테스트에 사용하는 `SelfSignedCertificate` 클래스가 포함됨. 이 클래스는 광범위한 사용을 전제로 설계된 것이 아니었으며, 범용 PKI·TLS 테스트 도구로 쓰기에는 적합하지 않은 API 결정들이 있음.

Netty 4.2에서는 새 모듈 `netty-pkitesting`을 도입하며, 여기에는 `CertificateBuilder` 클래스가 포함됨. `CertificateBuilder`는 통합자가 다양한 PKI·TLS 시나리오를 테스트할 수 있도록 설계됨.

이전처럼 자체 서명 인증서를 만들 수도 있고, 적절한 CA·issuer·leaf 인증서 체인을 만들 수도 있음. `CertificateBuilder`는 인증서 체인과 그에 대응하는 개인키를 모두 포함하는 `X509Bundle` 객체를 만들며, 번들은 파일을 쓰지 않고 완전히 인메모리로 생성됨. 인증서·키·키 저장소를 위한 파일이 필요한 경우, 번들 객체에는 이를 위한 임시 파일을 만드는 편리한 메서드가 다수 포함되어 있음.

`netty-pkitesting` 모듈에는 인증서 폐기 시나리오를 테스트할 수 있는 단순한 Certificate Revocation List 서버도 포함됨.
