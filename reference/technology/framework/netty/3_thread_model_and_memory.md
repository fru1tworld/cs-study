# Netty 스레드 모델, 참조 카운트, 메모리 할당자

## 스레드 모델 (Thread Model)

> 원본: https://netty.io/wiki/thread-model.html

---

간단히 말하면, 채널의 경우:

1. 전송(transport)이나 타입과 무관하게, 모든 upstream(즉, inbound) 이벤트는 그 채널의 I/O를 수행하는 스레드(I/O 스레드)에서 발사(fire)되어야 함
2. 모든 downstream(즉, outbound) 이벤트는 I/O 스레드든 아니든 어떤 스레드에서도 트리거 가능. 다만 downstream 이벤트의 부수효과로 트리거되는 upstream 이벤트는 반드시 I/O 스레드에서 발사되어야 함 (예: `Channel.close()`가 `channelDisconnected`, `channelUnbound`, `channelClosed`를 트리거하면 이들은 I/O 스레드에서 발사되어야 함)

현재의 문제(UGLY: upstream 핸들러에서 race condition을 유발 / BAD: race condition은 없지만 기대되는 스레드 모델을 위반):

- UGLY: downstream 이벤트의 부수효과로 트리거되는 upstream 이벤트가 호출자(caller) 스레드에서 트리거됨
- UGLY: 로컬 전송(local transport)은 항상 호출자 스레드를 사용해 이벤트를 트리거함
- BAD: `channelOpen`이 `ChannelFactory.newChannel()`을 호출한 스레드에서 트리거되며, 그 스레드는 I/O 스레드가 아님
  - 다만 이렇게 하지 않으면 `channelOpen`에서 채널을 닫아 동시 활성 채널 수를 제한하는 것이 불가능함 → I/O 스레드에서 처리하면 그만큼 비효율적이기 때문
- BAD: 클라이언트 측 채널은 두 개의 I/O 스레드가 실행 → 하나는 연결 시도 수행, 다른 하나는 실제 I/O 수행

해결할 작업 항목:

- 클라이언트 측 boss, 서버 측 boss, NioWorker를 모든 I/O 연산을 수행할 수 있는 범용 I/O 스레드로 통합 → 그러면:
  - 연결 시도를 한 스레드가 그대로 read/write를 계속 수행 가능해짐 → 클라이언트 측 채널 문제 해결
  - Netty가 열린 서버 포트 수만큼 스레드를 만드는 문제도 해결됨
  - NioWorker 풀을 더 쉽게 공유 가능·채널-워커 매핑에 더 큰 유연성 확보 가능
  - 모든 전송(socket, datagram, SCTP, ...)이 상속할 수 있는 추상 I/O 스레드 클래스를 만들 수 있는지도 검토 필요 → 현재 socket, datagram, SCTP 사이에 중복 과다
- 호출자 스레드가 I/O 스레드가 아니면, Netty는 이후 I/O 스레드에서 upstream 이벤트를 트리거함 → 이 변경과 함께 `ChannelPipeline`과 `ChannelHandlerContext`에 `sendUpstreamLater()` 메서드를 추가해 사용자가 자신만의 upstream 이벤트를 이후 I/O 스레드에서 트리거할 수 있도록 허용
  - 다만 현재 스레드가 I/O 스레드가 아닐 때만 `sendUpstreamLater()`를 사용하도록 강제할 수는 없음 → `OMATPE`나 `MATPE`가 이를 방해하기 때문 → `sendUpstream()`과 `sendUpstreamLater()` 중 무엇을 호출할지는 사용자가 직접 판단 필요
- `ChannelFactory.newChannel()`은 즉시 이벤트를 트리거하면 안 됨 → 채널을 호출자에게 반환하기 전에 I/O 스레드가 해당 채널의 등록을 완료했음을 알려줄 때까지 대기 필요
- 로컬 전송 재작성 필요

질문:

- 이 모든 변경을 v3에서 후방 호환을 유지하며 적용할 수 있는지, 아니면 v4에서 처리하는 편이 더 쉬운지가 관건 → 핸들러에서 모든 I/O를 수행하고 `ChannelFuture`를 적극 활용하는 완전 비동기 사용자 애플리케이션은 현재의 결함 있는 스레드 모델에 영향을 받지 않음 → 사용자는 어떻게든 이 문제를 우회 가능 → 두 브랜치에 동일한 변경을 가하기보다 v4로 옮겨가는 편이 나을 수 있음

답변:

- v3에 backport하기에 작업 범위가 너무 크면 그냥 앞으로 나아가고 v3는 그대로 두는 편이 나음 → 사용자에게 가장 자주 문제가 될 `Channel.close()` race를 제거할 수 있는 더 간단한 v3 우회책을 찾을 수 있을지도 모름 (normanmaurer)

---

## Thread Affinity (스레드 친화도)

> 원본: https://netty.io/wiki/thread-affinity.html

---

Netty로 저지연(low-latency) 네트워크 애플리케이션을 개발 중이라면 'thread affinity'를 들어봤을 것. Thread affinity는 애플리케이션 스레드를 특정 CPU 코어 또는 CPU 집합 위에서만 실행되도록 강제하는 용도로 사용 가능.

이렇게 하면 운영체제 스케줄링 과정에서 스레드 마이그레이션 제거 가능. 다행히 [Java-Thread-Affinity](https://github.com/OpenHFT/Java-Thread-Affinity)라는 자바 라이브러리가 있어 Netty 애플리케이션과 쉽게 통합 가능.

먼저 Maven `pom.xml` 파일에 다음 의존성을 추가.

```xml
<dependency>
    <groupId>net.openhft</groupId>
    <artifactId>affinity</artifactId>
    <version>3.0.6</version>
</dependency>
```

다음으로 원하는 전략으로 `AffinityThreadFactory`를 생성하고, 지연 시간에 민감한 스레드를 처리할 `EventLoopGroup`에 전달. 예를 들어:

```java
final int acceptorThreads = 1;
final int workerThreads = 10;
EventLoopGroup acceptorGroup = new NioEventLoopGroup(acceptorThreads);
ThreadFactory threadFactory = new AffinityThreadFactory("atf_wrk", AffinityStrategies.DIFFERENT_CORE);
EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads, threadFactory);

ServerBootstrap serverBootstrap = new ServerBootstrap().group(acceptorGroup, workerGroup);
```

가능한 가장 낮은 지연을 달성하려면 대상 CPU를 OS 스케줄러로부터 격리(isolate)하는 것을 고려 필요 → 대상 CPU를 격리하면 OS 스케줄러가 그 CPU에 다른 사용자 공간 프로세스를 스케줄링하지 못하게 됨 → `isolcpus` 커널 부트 파라미터로 가능 (즉, `grub.conf`에 `isolcpus=<cpu-list>` 추가).

자세한 내용은 프로젝트의 [GitHub](https://github.com/OpenHFT/Java-Thread-Affinity) 참고.

---

## 참조 카운트 객체 (Reference Counted Objects)

> 원본: https://netty.io/wiki/reference-counted-objects.html

---

Netty 4부터 일부 객체의 라이프사이클은 참조 카운트(reference count)로 관리됨. 덕분에 Netty는 객체가 더 이상 사용되지 않는 즉시 해당 객체(또는 객체가 보유한 공유 자원)를 객체 풀(또는 객체 할당자)로 반환 가능. 가비지 컬렉션과 reference queue는 도달 불가 시점에 대한 효율적인 실시간 보장을 제공하지 못함 → 참조 카운트는 약간의 불편을 감수하는 대신 이에 대한 대안을 제공.

[`ByteBuf`](http://netty.io/4.0/api/index.html?io/netty/buffer/ByteBuf.html)는 할당/해제 성능을 개선하기 위해 참조 카운트를 활용하는 가장 대표적인 타입. 이 페이지에서는 `ByteBuf`를 예로 들어 Netty의 참조 카운트가 어떻게 동작하는지 설명.

### 참조 카운트의 기초

새로 할당된 참조 카운트 객체의 초기 참조 카운트는 1.

```java
ByteBuf buf = ctx.alloc().directBuffer();
assert buf.refCnt() == 1;
```

참조 카운트 객체를 release하면 참조 카운트가 1만큼 감소. 참조 카운트가 0에 도달하면 객체는 해제되거나 자신이 속해 있던 객체 풀로 반환됨.

```java
assert buf.refCnt() == 1;
// release()는 참조 카운트가 0이 됐을 때만 true를 반환한다.
boolean destroyed = buf.release();
assert destroyed;
assert buf.refCnt() == 0;
```

#### Dangling reference (소멸된 객체에 대한 접근)

참조 카운트가 0인 객체에 접근하려고 하면 `IllegalReferenceCountException` 발생.

```java
assert buf.refCnt() == 0;
try {
  buf.writeLong(0xdeadbeef);
  throw new Error("should not reach here");
} catch (IllegalReferenceCountException e) {
  // Expected
}
```

#### 참조 카운트 증가시키기

참조 카운트는 아직 소멸되지 않은 한 `retain()` 연산으로도 증가 가능.

```java
ByteBuf buf = ctx.alloc().directBuffer();
assert buf.refCnt() == 1;

buf.retain();
assert buf.refCnt() == 2;

boolean destroyed = buf.release();
assert !destroyed;
assert buf.refCnt() == 1;
```

#### 누가 객체를 소멸시키는가?

일반 원칙: 참조 카운트 객체를 마지막으로 접근한 쪽이 그 객체의 소멸도 책임짐. 구체적으로:

- [송신 측] 컴포넌트가 참조 카운트 객체를 [수신 측] 컴포넌트에 전달해야 한다면, 송신 측은 보통 직접 소멸시킬 필요 없이 그 결정을 수신 측에 위임
- 어떤 컴포넌트가 참조 카운트 객체를 소비하고 이후 다른 컴포넌트로 참조를 넘기지 않는다면, 해당 컴포넌트가 객체를 소멸시켜야 함

간단한 예:

```java
public ByteBuf a(ByteBuf input) {
    input.writeByte(42);
    return input;
}

public ByteBuf b(ByteBuf input) {
    try {
        output = input.alloc().directBuffer(input.readableBytes() + 1);
        output.writeBytes(input);
        output.writeByte(42);
        return output;
    } finally {
        input.release();
    }
}

public void c(ByteBuf input) {
    System.out.println(input);
    input.release();
}

public void main() {
    ...
    ByteBuf buf = ...;
    // buf를 System.out에 출력하고 소멸시킨다.
    c(b(a(buf)));
    assert buf.refCnt() == 0;
}
```

동작별 release 책임과 실제 release 주체:

- 1. `main()`이 `buf`를 만듦
  - 누가 release해야 하는가: `buf`→`main()`
- 2. `main()`이 `buf`로 `a()`를 호출
  - 누가 release해야 하는가: `buf`→`a()`
- 3. `a()`는 단순히 `buf`를 반환
  - 누가 release해야 하는가: `buf`→`main()`
- 4. `main()`이 `buf`로 `b()`를 호출
  - 누가 release해야 하는가: `buf`→`b()`
- 5. `b()`가 `buf`의 사본을 반환
  - 누가 release해야 하는가: `buf`→`b()`, `copy`→`main()`
  - 누가 release했는가: `b()`가 `buf`를 release
- 6. `main()`이 `copy`로 `c()`를 호출
  - 누가 release해야 하는가: `copy`→`c()`
- 7. `c()`가 `copy`를 소비
  - 누가 release해야 하는가: `copy`→`c()`
  - 누가 release했는가: `c()`가 `copy`를 release

#### 파생 버퍼 (Derived buffers)

`ByteBuf.duplicate()`, `ByteBuf.slice()`, `ByteBuf.order(ByteOrder)`는 부모 버퍼의 메모리 영역을 공유하는 _파생(derived)_ 버퍼를 만듦. 파생 버퍼는 자신의 참조 카운트를 갖지 않고 부모 버퍼의 참조 카운트를 공유함.

```java
ByteBuf parent = ctx.alloc().directBuffer();
ByteBuf derived = parent.duplicate();

// 파생 버퍼를 만들어도 참조 카운트는 증가하지 않는다.
assert parent.refCnt() == 1;
assert derived.refCnt() == 1;
```

반면 `ByteBuf.copy()`와 `ByteBuf.readBytes(int)`는 파생 버퍼가 아님. 반환된 `ByteBuf`는 새로 할당된 것이며 release 필요.

부모 버퍼와 그 파생 버퍼는 같은 참조 카운트를 공유하고, 파생 버퍼를 만든다고 해서 참조 카운트가 증가하지는 않음에 유의. 따라서 파생 버퍼를 애플리케이션의 다른 컴포넌트로 넘기려면 먼저 그 위에 `retain()` 호출 필요.

```java
ByteBuf parent = ctx.alloc().directBuffer(512);
parent.writeBytes(...);

try {
    while (parent.isReadable(16)) {
        ByteBuf derived = parent.readSlice(16);
        derived.retain();
        process(derived);
    }
} finally {
    parent.release();
}
...

public void process(ByteBuf buf) {
    ...
    buf.release();
}
```

#### `ByteBufHolder` 인터페이스

때때로 `ByteBuf`는 [`DatagramPacket`](http://netty.io/4.0/api/index.html?io/netty/channel/socket/DatagramPacket.html), [`HttpContent`](http://netty.io/4.0/api/index.html?io/netty/handler/codec/http/HttpContent.html), [`WebSocketframe`](http://netty.io/4.0/api/index.html?io/netty/handler/codec/http/websocketx/WebSocketFrame.html) 같은 버퍼 홀더에 담겨 있기도 함. 이 타입들은 [`ByteBufHolder`](http://netty.io/4.0/api/index.html?io/netty/buffer/ByteBufHolder.html)라는 공통 인터페이스를 상속.

버퍼 홀더는 파생 버퍼와 마찬가지로 자신이 담고 있는 버퍼의 참조 카운트를 공유함.

### `ChannelHandler`에서의 참조 카운트

#### Inbound 메시지

이벤트 루프가 데이터를 `ByteBuf`로 읽고 그것으로 `channelRead()` 이벤트를 트리거하면, 해당 파이프라인의 `ChannelHandler`가 그 버퍼를 release할 책임을 짐. 따라서 받은 데이터를 소비하는 핸들러는 자신의 `channelRead()` 핸들러 메서드에서 데이터를 `release()` 해야 함.

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    try {
        ...
    } finally {
        buf.release();
    }
}
```

앞서 '누가 객체를 소멸시키는가?'에서 설명했듯, 핸들러가 버퍼(또는 참조 카운트 객체)를 다음 핸들러에 넘길 때는 직접 release할 필요 없음.

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    ...
    ctx.fireChannelRead(buf);
}
```

Netty의 참조 카운트 타입은 `ByteBuf`만이 아님. 디코더가 생성한 메시지를 다룬다면, 그 메시지도 참조 카운트 타입일 가능성이 높음.

```java
// 핸들러가 `HttpRequestDecoder` 다음에 위치한다고 가정
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
        HttpRequest req = (HttpRequest) msg;
        ...
    }
    if (msg instanceof HttpContent) {
        HttpContent content = (HttpContent) msg;
        try {
            ...
        } finally {
            content.release();
        }
    }
}
```

확신이 없거나 메시지를 release하는 코드를 단순화하고 싶다면 `ReferenceCountUtil.release()` 사용 가능.

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    try {
        ...
    } finally {
        ReferenceCountUtil.release(msg);
    }
}
```

또는 받은 모든 메시지에 대해 `ReferenceCountUtil.release(msg)`를 호출해주는 `SimpleChannelHandler` 상속도 고려 가능.

#### Outbound 메시지

Inbound 메시지와 달리, outbound 메시지는 애플리케이션이 직접 만들고, 회선에 써낸 뒤 release하는 것은 Netty의 책임. 다만 쓰기 요청을 가로채는 핸들러는 중간에 만들어진 객체들을 적절히 release해야 함(예: 인코더).

```java
// 단순 패스스루
public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
    System.err.println("Writing: " + message);
    ctx.write(message, promise);
}

// 변환
public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
    if (message instanceof HttpContent) {
        // HttpContent를 ByteBuf로 변환한다.
        HttpContent content = (HttpContent) message;
        try {
            ByteBuf transformed = ctx.alloc().buffer();
            ....
            ctx.write(transformed, promise);
        } finally {
            content.release();
        }
    } else {
        // HttpContent가 아닌 메시지는 그대로 통과시킨다.
        ctx.write(message, promise);
    }
}
```

### 버퍼 누수 트러블슈팅

참조 카운트의 단점은 객체를 누수시키기 쉽다는 것. JVM은 Netty의 참조 카운트를 인식하지 못하기 때문에, 참조 카운트가 0이 아니더라도 객체가 도달 불가능해지면 자동으로 가비지 컬렉트해버림. 한 번 가비지 컬렉트된 객체는 되살릴 수 없어 자신이 왔던 풀로도 반환되지 못함 → 결과적으로 메모리 누수 발생.

다행히 누수 추적이 어렵긴 해도 Netty는 기본적으로 약 1%의 버퍼 할당을 샘플링해 애플리케이션의 누수 여부를 검사함. 누수가 있는 경우 다음과 같은 로그 메시지 확인 가능.

> `LEAK: ByteBuf.release() was not called before it's garbage-collected. Enable advanced leak reporting to find out where the leak occurred. To enable advanced leak reporting, specify the JVM option '-Dio.netty.leakDetectionLevel=advanced' or call ResourceLeakDetector.setLevel()`

위 메시지가 안내하는 JVM 옵션을 추가해 애플리케이션을 다시 실행하면, 누수된 버퍼가 마지막으로 접근된 위치들을 확인 가능. 다음 출력은 단위 테스트(`XmlFrameDecoderTest.testDecodeWithXml()`)에서 발견된 누수 예시.

```
Running io.netty.handler.codec.xml.XmlFrameDecoderTest
15:03:36.886 [main] ERROR io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected.
Recent access records: 1
#1:
	io.netty.buffer.AdvancedLeakAwareByteBuf.toString(AdvancedLeakAwareByteBuf.java:697)
	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithXml(XmlFrameDecoderTest.java:157)
	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithTwoMessages(XmlFrameDecoderTest.java:133)
	...

Created at:
	io.netty.buffer.UnpooledByteBufAllocator.newDirectBuffer(UnpooledByteBufAllocator.java:55)
	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:155)
	io.netty.buffer.UnpooledUnsafeDirectByteBuf.copy(UnpooledUnsafeDirectByteBuf.java:465)
	io.netty.buffer.WrappedByteBuf.copy(WrappedByteBuf.java:697)
	io.netty.buffer.AdvancedLeakAwareByteBuf.copy(AdvancedLeakAwareByteBuf.java:656)
	io.netty.handler.codec.xml.XmlFrameDecoder.extractFrame(XmlFrameDecoder.java:198)
	io.netty.handler.codec.xml.XmlFrameDecoder.decode(XmlFrameDecoder.java:174)
	io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:227)
	io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:140)
	io.netty.channel.ChannelHandlerInvokerUtil.invokeChannelReadNow(ChannelHandlerInvokerUtil.java:74)
	io.netty.channel.embedded.EmbeddedEventLoop.invokeChannelRead(EmbeddedEventLoop.java:142)
	io.netty.channel.DefaultChannelHandlerContext.fireChannelRead(DefaultChannelHandlerContext.java:317)
	io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:846)
	io.netty.channel.embedded.EmbeddedChannel.writeInbound(EmbeddedChannel.java:176)
	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithXml(XmlFrameDecoderTest.java:147)
	io.netty.handler.codec.xml.XmlFrameDecoderTest.testDecodeWithTwoMessages(XmlFrameDecoderTest.java:133)
	...
```

Netty 5 이상을 사용하면, 누수된 버퍼를 마지막으로 처리한 핸들러를 찾는 데 도움을 주는 추가 정보가 제공됨. 다음 예제는 누수된 버퍼가 `EchoServerHandler#0`이라는 핸들러에 의해 처리된 뒤 가비지 컬렉트되었음을 보여줌. 즉, `EchoServerHandler#0`이 버퍼를 release하지 않았을 가능성이 높음.

```
12:05:24.374 [nioEventLoop-1-1] ERROR io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected.
Recent access records: 2
#2:
	Hint: 'EchoServerHandler#0' will handle the message from this point.
	io.netty.channel.DefaultChannelHandlerContext.fireChannelRead(DefaultChannelHandlerContext.java:329)
	io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:846)
	io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:133)
	io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:485)
	io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:452)
	io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:346)
	io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:794)
	java.lang.Thread.run(Thread.java:744)
#1:
	io.netty.buffer.AdvancedLeakAwareByteBuf.writeBytes(AdvancedLeakAwareByteBuf.java:589)
	io.netty.channel.socket.nio.NioSocketChannel.doReadBytes(NioSocketChannel.java:208)
	io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:125)
	io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:485)
	io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:452)
	io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:346)
	io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:794)
	java.lang.Thread.run(Thread.java:744)
Created at:
	io.netty.buffer.UnpooledByteBufAllocator.newDirectBuffer(UnpooledByteBufAllocator.java:55)
	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:155)
	io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:146)
	io.netty.buffer.AbstractByteBufAllocator.ioBuffer(AbstractByteBufAllocator.java:107)
	io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:123)
	io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:485)
	io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:452)
	io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:346)
	io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:794)
	java.lang.Thread.run(Thread.java:744)
```

#### 누수 검출 레벨

현재 누수 검출 레벨은 4단계로 구분:

- `DISABLED`: 누수 검출을 완전히 비활성화. 권장되지 않음
- `SIMPLE`: 1%의 버퍼에 대해 누수 여부만 알려줌. 기본값
- `ADVANCED`: 1%의 버퍼에 대해 누수된 버퍼가 어디서 접근되었는지도 알려줌
- `PARANOID`: `ADVANCED`와 동일하되 모든 단일 버퍼에 대해 동작. 자동화된 테스트 단계에서 유용 → 빌드 출력에 '`LEAK: `'가 포함되어 있으면 빌드를 실패시키는 식으로 사용 가능

누수 검출 레벨은 JVM 옵션 `-Dio.netty.leakDetection.level`로 지정.

```
java -Dio.netty.leakDetection.level=advanced ...
```

참고: 이 프로퍼티는 예전에 `io.netty.leakDetectionLevel`이라는 이름이었음.

#### 누수를 피하기 위한 모범 사례

- 단위 테스트와 통합 테스트를 `PARANOID` 누수 검출 레벨로, 그리고 `SIMPLE` 레벨로 실행
- 클러스터 전체로 롤아웃하기 전에 `SIMPLE` 레벨로 충분히 오랫동안 카나리(canary)를 돌려 누수 여부 확인
- 누수가 있으면 `ADVANCED` 레벨로 다시 카나리를 돌려 누수 발생 위치에 대한 힌트 확보
- 누수가 있는 애플리케이션은 클러스터 전체에 배포 금지

#### 단위 테스트에서의 누수 수정

단위 테스트에서는 버퍼나 메시지를 release하는 것을 빠뜨리기 쉬움. 누수 경고가 발생하더라도 이것이 곧 애플리케이션에 누수가 있다는 의미는 아님. `try-finally` 블록으로 모든 버퍼를 직접 release하는 대신, `ReferenceCountUtil.releaseLater()` 유틸리티 메서드 활용 가능.

```java
import static io.netty.util.ReferenceCountUtil.*;

@Test
public void testSomething() throws Exception {
    // ReferenceCountUtil.releaseLater()는 buf의 참조를 보관하고 있다가
    // 테스트 스레드가 종료될 때 release한다.
    ByteBuf buf = releaseLater(Unpooled.directBuffer(512));
    ...
}
```

---

외부 링크:

- [Why do we need to manually handle reference counting for Netty ByteBuf if JVM GC is still in place?](http://stackoverflow.com/questions/28647048/why-do-we-need-to-manually-handle-reference-counting-for-netty-bytebuf-if-jvm-gc)
- [Buffer ownership in Netty 4: How is buffer life-cycle managed?](http://stackoverflow.com/questions/15781276/buffer-ownership-in-netty-4-how-is-buffer-life-cycle-managed)

---

## 메모리 할당자 동작 분석

> 원본: https://netty.io/wiki/analyzing-memory-allocator-behavior.html

---

Netty 4.2는 `ByteBuf` 인스턴스를 만드는 데 사용되는 세 가지 서로 다른 메모리 할당자 포함:

1. `unpooled` 할당자: 호출할 때마다 시스템 메모리에서 할당하고, `ByteBuf`가 더 이상 사용되지 않으면 즉시 시스템에 반환
2. `pooled` 할당자: 시스템 메모리의 _chunk_ 단위로 할당하고, 여러 `ByteBuf` 인스턴스 사이에서 공유·재사용. 메모리는 arena, chunk, size-class로 조직 → `jemalloc` 할당자의 설계를 따름. Netty 4.1의 기본값이었음
3. `adaptive` 할당자: `pooled`처럼 시스템 메모리의 _chunk_를 할당. chunk는 일반적으로 더 작고 더 많으며, magazine이라는 그룹에 모임 → magazine은 size class당 하나씩 magazine group으로 조직 → 그룹은 멀티스레드 경쟁(contention)에 반응해 커짐. Netty 4.2의 기본값

각 할당자에는 장단점이 있어, 어떤 애플리케이션에는 더 적합하고 어떤 애플리케이션에는 덜 적합함.

Netty가 기본으로 사용할 할당자는 `io.netty.allocator.type` 시스템 프로퍼티로 제어하며, 위에 나온 `name` 중 하나로 설정.

### FlightRecorder 이벤트 기록

`pooled`와 `adaptive` 할당자는 버퍼나 chunk를 할당/해제할 때 Java FlightRecorder 이벤트를 발사 가능.

이 이벤트들은 성능 오버헤드가 상당하기 때문에 기본적으로 비활성화되어 있으며, 기록에 사용할 FlightRecorder 프로파일에서 명시적으로 활성화 필요.

Netty 팀은 _오직_ Netty 할당자 이벤트만 포함하고 다른 모든 것은 비활성화하는 FlightRecorder 프로파일을 함께 마련해 둠. 이 프로파일은 다음 위치에서 다운로드 가능: https://github.com/netty/netty/blob/4.2/microbench/src/main/resources/Netty%20Allocator%20Events.jfc

이 프로파일을 사용하면 결과 기록에 개인 정보나 지적 재산이 포함되지 않아 결과를 공개적으로 공유 가능하다는 장점도 있음.

프로파일을 Netty 애플리케이션이 실행 중인 시스템에 다운로드한 뒤, 다음 명령으로 할당 프로파일 확보 가능:

- 프로파일링하려는 Netty 애플리케이션의 PID 얻기:
  - `$ jps`
- 다음과 비슷한 명령으로 프로파일링 시작:
  - `$ jcmd <PID> JFR.start name=netty-allocator-profiling duration=30s filename=netty-allocator.jfr settings=/path/to/netty.jfc maxsize=200m`
- 주기적으로 상태를 확인하며 프로파일링이 끝날 때까지 대기:
  - `$ jcmd <PID> JFR.check`
- 기록이 끝나면 `.jfr` 파일을 다운로드해서 분석 가능

### 할당자 Flight Recording 분석

`netty-microbench` 모듈에는 `AllocationPatternSimulator`라는 프로그램이 있음. 이 프로그램은 할당 프로파일을 분석하고 `pooled` 및 `adaptive` 할당자에 동시에 적용해 그 할당 패턴을 시뮬레이션하며, 두 할당자의 메모리 사용량을 비교 차트로 보여줌.

이 프로그램을 사용하려면 Netty 소스 코드를 체크아웃하고 컴파일 필요. 그런 다음 `AllocationPatternSimulator` 클래스를 실행하면서 첫 번째 프로그램 인자로 할당 프로파일 `.jfr` 파일을 전달하면 됨.
