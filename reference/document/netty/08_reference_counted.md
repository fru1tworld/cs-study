# 참조 카운트 객체 (Reference Counted Objects)

> 원본: https://netty.io/wiki/reference-counted-objects.html

---

Netty 4부터 일부 객체의 라이프사이클은 참조 카운트(reference count)에 의해 관리됩니다. 덕분에 Netty는 객체가 더 이상 사용되지 않는 즉시 해당 객체(또는 객체가 보유한 공유 자원)를 객체 풀(또는 객체 할당자)로 반환할 수 있습니다. 가비지 컬렉션과 reference queue는 도달 불가 시점에 대한 효율적인 실시간 보장을 제공하지 못하는데, 참조 카운트는 약간의 불편을 감수하는 대신 이에 대한 대안을 제공합니다.

[`ByteBuf`](http://netty.io/4.0/api/index.html?io/netty/buffer/ByteBuf.html)는 할당/해제 성능을 개선하기 위해 참조 카운트를 활용하는 가장 대표적인 타입입니다. 이 페이지에서는 `ByteBuf`를 예로 들어 Netty의 참조 카운트가 어떻게 동작하는지 설명합니다.

## 참조 카운트의 기초

새로 할당된 참조 카운트 객체의 초기 참조 카운트는 1입니다.

```java
ByteBuf buf = ctx.alloc().directBuffer();
assert buf.refCnt() == 1;
```

참조 카운트 객체를 release하면 참조 카운트는 1만큼 감소합니다. 참조 카운트가 0에 도달하면 객체는 해제되거나 자신이 속해 있던 객체 풀로 반환됩니다.

```java
assert buf.refCnt() == 1;
// release()는 참조 카운트가 0이 됐을 때만 true를 반환한다.
boolean destroyed = buf.release();
assert destroyed;
assert buf.refCnt() == 0;
```

### Dangling reference (소멸된 객체에 대한 접근)

참조 카운트가 0인 객체에 접근하려고 하면 `IllegalReferenceCountException`이 발생합니다.

```java
assert buf.refCnt() == 0;
try {
  buf.writeLong(0xdeadbeef);
  throw new Error("should not reach here");
} catch (IllegalReferenceCountException e) {
  // Expected
}
```

### 참조 카운트 증가시키기

참조 카운트는 아직 소멸되지 않은 한 `retain()` 연산으로도 증가시킬 수 있습니다.

```java
ByteBuf buf = ctx.alloc().directBuffer();
assert buf.refCnt() == 1;

buf.retain();
assert buf.refCnt() == 2;

boolean destroyed = buf.release();
assert !destroyed;
assert buf.refCnt() == 1;
```

### 누가 객체를 소멸시키는가?

일반적인 원칙은 **참조 카운트 객체를 마지막으로 접근한 쪽이 그 객체의 소멸도 책임진다**는 것입니다. 구체적으로:

* [송신 측] 컴포넌트가 참조 카운트 객체를 [수신 측] 컴포넌트에 전달해야 한다면, 송신 측은 보통 직접 소멸시킬 필요가 없으며 그 결정을 수신 측에 위임합니다.
* 어떤 컴포넌트가 참조 카운트 객체를 소비하고, 이후 다른 컴포넌트로 참조를 넘기지 않는다면, 해당 컴포넌트가 객체를 소멸시켜야 합니다.

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

| 동작 | 누가 release해야 하는가? | 누가 release했는가? |
|------|---------------------|----------------|
| 1. `main()`이 `buf`를 만든다 | `buf`→`main()` | |
| 2. `main()`이 `buf`로 `a()`를 호출 | `buf`→`a()` | |
| 3. `a()`는 단순히 `buf`를 반환 | `buf`→`main()` | |
| 4. `main()`이 `buf`로 `b()`를 호출 | `buf`→`b()` | |
| 5. `b()`가 `buf`의 사본을 반환 | `buf`→`b()`, `copy`→`main()` | `b()`가 `buf`를 release |
| 6. `main()`이 `copy`로 `c()`를 호출 | `copy`→`c()` | |
| 7. `c()`가 `copy`를 소비 | `copy`→`c()` | `c()`가 `copy`를 release |

### 파생 버퍼 (Derived buffers)

`ByteBuf.duplicate()`, `ByteBuf.slice()`, `ByteBuf.order(ByteOrder)`는 부모 버퍼의 메모리 영역을 공유하는 _파생(derived)_ 버퍼를 만듭니다. 파생 버퍼는 자신의 참조 카운트를 갖지 않고 부모 버퍼의 참조 카운트를 공유합니다.

```java
ByteBuf parent = ctx.alloc().directBuffer();
ByteBuf derived = parent.duplicate();

// 파생 버퍼를 만들어도 참조 카운트는 증가하지 않는다.
assert parent.refCnt() == 1;
assert derived.refCnt() == 1;
```

반면, `ByteBuf.copy()`와 `ByteBuf.readBytes(int)`는 _파생 버퍼가 아닙니다_. 반환된 `ByteBuf`는 새로 할당된 것이며 release해야 합니다.

부모 버퍼와 그 파생 버퍼는 같은 참조 카운트를 공유하고, 파생 버퍼를 만든다고 해서 참조 카운트가 증가하지는 않는다는 점에 유의하세요. 따라서 파생 버퍼를 애플리케이션의 다른 컴포넌트로 넘기려면 먼저 그 위에 `retain()`을 호출해야 합니다.

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

### `ByteBufHolder` 인터페이스

때때로 `ByteBuf`는 [`DatagramPacket`](http://netty.io/4.0/api/index.html?io/netty/channel/socket/DatagramPacket.html), [`HttpContent`](http://netty.io/4.0/api/index.html?io/netty/handler/codec/http/HttpContent.html), [`WebSocketframe`](http://netty.io/4.0/api/index.html?io/netty/handler/codec/http/websocketx/WebSocketFrame.html) 같은 버퍼 홀더에 담겨 있기도 합니다. 이 타입들은 [`ByteBufHolder`](http://netty.io/4.0/api/index.html?io/netty/buffer/ByteBufHolder.html)라는 공통 인터페이스를 상속합니다.

버퍼 홀더는 파생 버퍼와 마찬가지로 자신이 담고 있는 버퍼의 참조 카운트를 공유합니다.

## `ChannelHandler`에서의 참조 카운트

### Inbound 메시지

이벤트 루프가 데이터를 `ByteBuf`로 읽고 그것으로 `channelRead()` 이벤트를 트리거하면, 해당 파이프라인의 `ChannelHandler`가 그 버퍼를 release할 책임을 집니다. 따라서 받은 데이터를 소비하는 핸들러는 자신의 `channelRead()` 핸들러 메서드에서 데이터에 대해 `release()`를 호출해야 합니다.

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

앞서 '누가 객체를 소멸시키는가?' 섹션에서 설명했듯이, 핸들러가 버퍼(또는 참조 카운트 객체)를 다음 핸들러에 넘길 때는 직접 release할 필요가 없습니다.

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    ...
    ctx.fireChannelRead(buf);
}
```

Netty의 참조 카운트 타입은 `ByteBuf`만이 아닙니다. 디코더가 생성한 메시지를 다룬다면, 그 메시지도 참조 카운트 타입일 가능성이 높습니다.

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

확신이 없거나 메시지를 release하는 코드를 단순화하고 싶다면 `ReferenceCountUtil.release()`를 사용할 수 있습니다.

```java
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    try {
        ...
    } finally {
        ReferenceCountUtil.release(msg);
    }
}
```

또는 받은 모든 메시지에 대해 `ReferenceCountUtil.release(msg)`를 호출해주는 `SimpleChannelHandler` 상속을 고려할 수도 있습니다.

### Outbound 메시지

Inbound 메시지와 달리, outbound 메시지는 애플리케이션이 직접 만들고, 회선에 써낸 뒤에 release하는 것은 Netty의 책임입니다. 다만 쓰기 요청을 가로채는 핸들러는 중간에 만들어진 객체들을 적절히 release해야 합니다(예: 인코더).

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

## 버퍼 누수 트러블슈팅

참조 카운트의 단점은 객체를 누수시키기 쉽다는 것입니다. JVM은 Netty의 참조 카운트를 인식하지 못하기 때문에, 참조 카운트가 0이 아니더라도 객체가 도달 불가능해지면 자동으로 가비지 컬렉트해버립니다. 한 번 가비지 컬렉트된 객체는 되살릴 수 없으므로 자신이 왔던 풀로도 반환되지 못하고, 그 결과 메모리 누수가 발생합니다.

다행히, 누수 추적이 어렵긴 해도 Netty는 기본적으로 약 1%의 버퍼 할당을 샘플링해 애플리케이션의 누수 여부를 검사합니다. 누수가 있는 경우 다음과 같은 로그 메시지를 보게 됩니다.

> `LEAK: ByteBuf.release() was not called before it's garbage-collected. Enable advanced leak reporting to find out where the leak occurred. To enable advanced leak reporting, specify the JVM option '-Dio.netty.leakDetectionLevel=advanced' or call ResourceLeakDetector.setLevel()`

위 메시지가 안내하는 JVM 옵션을 추가해 애플리케이션을 다시 실행하면, 누수된 버퍼가 마지막으로 접근된 위치들을 볼 수 있습니다. 다음 출력은 우리 단위 테스트(`XmlFrameDecoderTest.testDecodeWithXml()`)에서 발견된 누수 예시입니다.

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

Netty 5 이상을 사용한다면, 누수된 버퍼를 마지막으로 처리한 핸들러를 찾는 데 도움을 주는 추가 정보가 제공됩니다. 다음 예제는 누수된 버퍼가 `EchoServerHandler#0`이라는 핸들러에 의해 처리된 뒤 가비지 컬렉트되었음을 보여줍니다. 즉, `EchoServerHandler#0`이 버퍼를 release하지 않았을 가능성이 높다는 뜻입니다.

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

### 누수 검출 레벨

현재 누수 검출 레벨은 4단계로 구분됩니다.

* `DISABLED` - 누수 검출을 완전히 비활성화. 권장되지 않습니다.
* `SIMPLE` - 1%의 버퍼에 대해 누수 여부만 알려줍니다. **기본값**입니다.
* `ADVANCED` - 1%의 버퍼에 대해 누수된 버퍼가 어디서 접근되었는지도 알려줍니다.
* `PARANOID` - `ADVANCED`와 동일하되, 모든 단일 버퍼에 대해 동작합니다. 자동화된 테스트 단계에서 유용합니다. 빌드 출력에 '`LEAK: `'가 포함되어 있다면 빌드를 실패시키는 식으로 사용할 수 있습니다.

누수 검출 레벨은 JVM 옵션 `-Dio.netty.leakDetection.level`로 지정합니다.

```
java -Dio.netty.leakDetection.level=advanced ...
```

**참고**: 이 프로퍼티는 예전에 `io.netty.leakDetectionLevel`이라는 이름이었습니다.

### 누수를 피하기 위한 모범 사례

* 단위 테스트와 통합 테스트를 `PARANOID` 누수 검출 레벨로, 그리고 `SIMPLE` 레벨로 실행하세요.
* 클러스터 전체로 롤아웃하기 전에 `SIMPLE` 레벨로 충분히 오랫동안 카나리(canary)를 돌려 누수가 있는지 확인하세요.
* 누수가 있다면 `ADVANCED` 레벨로 다시 카나리를 돌려 누수가 어디서 발생하는지에 대한 힌트를 얻으세요.
* 누수가 있는 애플리케이션을 클러스터 전체에 배포하지 마세요.

### 단위 테스트에서의 누수 수정

단위 테스트에서는 버퍼나 메시지를 release하는 것을 빠뜨리기 쉽습니다. 누수 경고가 발생하더라도 이것이 곧 애플리케이션에 누수가 있다는 의미는 아닙니다. `try-finally` 블록으로 모든 버퍼를 직접 release하는 대신, `ReferenceCountUtil.releaseLater()` 유틸리티 메서드를 활용할 수 있습니다.

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

***

**외부 링크:**

- [Why do we need to manually handle reference counting for Netty ByteBuf if JVM GC is still in place?](http://stackoverflow.com/questions/28647048/why-do-we-need-to-manually-handle-reference-counting-for-netty-bytebuf-if-jvm-gc)
- [Buffer ownership in Netty 4: How is buffer life-cycle managed?](http://stackoverflow.com/questions/15781276/buffer-ownership-in-netty-4-how-is-buffer-life-cycle-managed)
