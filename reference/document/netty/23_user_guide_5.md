# Netty 5.x User Guide

> 이 문서는 Netty 공식 Wiki의 "User Guide for 5.x" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/user-guide-for-5.x.html
>
> 4.x User Guide([03_user_guide.md](./03_user_guide.md))와 거의 동일한 내용이며, 5.x에 맞게 일부 클래스명만 다릅니다(예: `ChannelInboundHandlerAdapter` → `ChannelHandlerAdapter`).

---

### 서드파티 번역

* [Simplified Chinese](http://ifeve.com/netty5-user-guide/)

## 서문 (Preface)

### 문제

오늘날 우리는 서로 통신하기 위해 범용 애플리케이션이나 라이브러리를 사용합니다. 예를 들어 웹 서버에서 정보를 가져오거나 웹 서비스를 통해 원격 프로시저를 호출하기 위해 HTTP 클라이언트 라이브러리를 자주 사용합니다.

그러나 범용 프로토콜이나 그 구현은 때때로 잘 확장되지 않습니다. 거대한 파일이나 이메일, 금융 정보·멀티플레이어 게임 데이터처럼 거의 실시간성을 요구하는 메시지를 교환할 때 범용 HTTP 서버를 쓰지 않는 것과 같은 이치입니다. 필요한 것은 특정 목적에 맞춰 고도로 최적화된 프로토콜 구현입니다. 예를 들어 AJAX 기반 채팅 애플리케이션, 미디어 스트리밍, 대용량 파일 전송에 최적화된 HTTP 서버를 만들고 싶을 수 있습니다. 더 나아가 자신의 필요에 정확히 맞는 완전히 새로운 프로토콜을 설계·구현하고 싶을 수도 있습니다.

또 한 가지 피할 수 없는 경우는, 오래된 시스템과의 상호 운용성을 위해 레거시 독점 프로토콜을 다뤄야 할 때입니다. 이 경우 중요한 것은 결과 애플리케이션의 안정성과 성능을 희생하지 않으면서 얼마나 빠르게 그 프로토콜을 구현할 수 있느냐입니다.

## 해결책

_[Netty 프로젝트](http://netty.io/)_ 는 유지보수성이 좋고 고성능·고확장성을 갖춘 프로토콜 서버와 클라이언트를 빠르게 개발하기 위한 비동기 이벤트 기반 네트워크 애플리케이션 프레임워크와 도구를 제공하기 위한 노력입니다.

다시 말해, Netty는 프로토콜 서버와 클라이언트 같은 네트워크 애플리케이션을 빠르고 쉽게 개발할 수 있도록 해주는 NIO 클라이언트-서버 프레임워크입니다. TCP·UDP 소켓 서버 개발 같은 네트워크 프로그래밍을 크게 단순화하고 매끄럽게 만들어줍니다.

'빠르고 쉽다'는 것이 결과 애플리케이션이 유지보수성이나 성능 문제로 고생한다는 뜻은 아닙니다. Netty는 FTP, SMTP, HTTP를 비롯한 다양한 바이너리/텍스트 기반 레거시 프로토콜의 구현 경험을 바탕으로 신중하게 설계되었습니다. 그 결과 Netty는 개발 편의성, 성능, 안정성, 유연성을 어느 하나도 타협하지 않고 동시에 달성하는 길을 찾아냈습니다.

같은 장점을 주장하는 다른 네트워크 애플리케이션 프레임워크를 이미 발견한 사용자들도 있을 것이고, 그렇다면 Netty가 그것들과 무엇이 다른지 궁금할 것입니다. 답은 Netty가 기반으로 삼는 철학에 있습니다. Netty는 첫날부터 API와 구현 양쪽 모두에서 가장 편안한 경험을 제공하도록 설계되어 있습니다. 손에 잡히는 무언가는 아니지만, 이 가이드를 읽고 Netty를 다뤄보면 이 철학이 여러분의 삶을 훨씬 편하게 만들어준다는 것을 깨닫게 될 것입니다.

## 시작하기 (Getting Started)

이 장에서는 간단한 예제를 통해 Netty의 핵심 구성 요소를 둘러보며 빠르게 시작할 수 있도록 안내합니다. 이 장이 끝날 즈음이면 Netty 위에서 클라이언트와 서버를 바로 작성할 수 있게 될 것입니다.

만약 하향식(top-down) 학습을 선호한다면, Chapter 2, Architectural Overview부터 시작한 뒤 다시 여기로 돌아와도 좋습니다.

### 시작 전 준비물

이 장의 예제를 실행하기 위한 최소 요구사항은 두 가지뿐입니다. 최신 버전의 Netty와 JDK 1.6 이상입니다. 최신 버전의 Netty는 [프로젝트 다운로드 페이지](http://netty.io/downloads.html)에서 받을 수 있습니다. JDK는 사용하시는 벤더의 웹사이트를 참고해 다운로드하세요.

문서를 읽다 보면 이 장에서 소개되는 클래스에 대해 더 궁금한 점이 생길 수 있습니다. 그럴 때마다 API 레퍼런스를 참고하세요. 이 문서의 모든 클래스명은 편의를 위해 온라인 API 레퍼런스로 링크되어 있습니다. 또한 잘못된 정보, 문법 오류, 오타가 있거나 문서를 개선할 좋은 아이디어가 있다면 [Netty 프로젝트 커뮤니티에 문의](http://netty.io/community.html)하기를 주저하지 마세요.

### Discard 서버 작성하기

세상에서 가장 단순한 프로토콜은 'Hello, World!'가 아니라 [`DISCARD`](http://tools.ietf.org/html/rfc863)입니다. 받은 데이터를 아무런 응답 없이 버리는 프로토콜이죠.

`DISCARD` 프로토콜을 구현하기 위해 해야 할 일은 받은 데이터를 모두 무시하는 것뿐입니다. Netty가 발생시키는 I/O 이벤트를 처리하는 핸들러 구현부터 바로 시작해 봅시다.

```java
package io.netty.example.discard;

import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerAdapter;

/**
 * Handles a server-side channel.
 */
public class DiscardServerHandler extends ChannelHandlerAdapter { // (1)

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        // Discard the received data silently.
        ((ByteBuf) msg).release(); // (3)
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
```

1. `DiscardServerHandler`는 [`ChannelHandler`]의 구현체인 [`ChannelHandlerAdapter`]를 상속합니다. [`ChannelHandler`]는 오버라이드할 수 있는 다양한 이벤트 핸들러 메서드를 제공합니다. 지금은 핸들러 인터페이스를 직접 구현하기보다는 [`ChannelHandlerAdapter`]를 상속하는 것으로 충분합니다.
2. 여기서는 `channelRead()` 이벤트 핸들러 메서드를 오버라이드합니다. 이 메서드는 클라이언트에서 새 데이터를 받을 때마다 받은 메시지와 함께 호출됩니다. 이 예제에서 받은 메시지의 타입은 [`ByteBuf`]입니다.
3. `DISCARD` 프로토콜을 구현하려면 핸들러는 받은 메시지를 무시해야 합니다. [`ByteBuf`]는 참조 카운트(reference-counted) 객체이며 `release()` 메서드로 명시적으로 해제해야 합니다. 핸들러로 전달된 모든 참조 카운트 객체를 해제하는 것은 **핸들러의 책임**이라는 점을 꼭 기억하세요. 일반적으로 `channelRead()` 핸들러 메서드는 다음과 같이 구현합니다.

   ```java
   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) {
       try {
           // Do something with msg
       } finally {
           ReferenceCountUtil.release(msg);
       }
   }
   ```

4. `exceptionCaught()` 이벤트 핸들러 메서드는 I/O 오류로 인해 Netty가 예외를 일으키거나, 이벤트를 처리하던 핸들러 구현체에서 예외가 던져졌을 때 `Throwable`과 함께 호출됩니다. 대부분의 경우 잡힌 예외를 로깅하고 관련 채널을 닫아야 하지만, 예외 상황에 어떻게 대응할지에 따라 이 메서드의 구현은 달라질 수 있습니다. 예를 들어 연결을 닫기 전에 에러 코드를 담은 응답 메시지를 보내고 싶을 수도 있습니다.

지금까지는 잘 됐습니다. `DISCARD` 서버의 절반을 구현했습니다. 이제 남은 일은 `DiscardServerHandler`로 서버를 시작하는 `main()` 메서드를 작성하는 것입니다.

```java
package io.netty.example.discard;
    
import io.netty.bootstrap.ServerBootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
    
/**
 * Discards any incoming data.
 */
public class DiscardServer {
    
    private int port;
    
    public DiscardServer(int port) {
        this.port = port;
    }
    
    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new DiscardServerHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)          // (5)
             .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
    
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)
    
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new DiscardServer(port).run();
    }
}
```

1. [`NioEventLoopGroup`]은 I/O 작업을 처리하는 멀티스레드 이벤트 루프입니다. Netty는 다양한 종류의 전송(transport)을 위한 여러 [`EventLoopGroup`] 구현체를 제공합니다. 이 예제에서는 서버 사이드 애플리케이션을 구현하므로 두 개의 [`NioEventLoopGroup`]을 사용합니다. 첫 번째는 흔히 'boss'라고 부르며 들어오는 연결을 받아들입니다. 두 번째는 흔히 'worker'라고 부르며, boss가 연결을 수락한 뒤 그 연결을 worker에 등록하면 worker가 그 연결의 트래픽을 처리합니다. 사용되는 스레드 수와 그 스레드들이 생성된 [`Channel`]에 어떻게 매핑되는지는 [`EventLoopGroup`] 구현에 따라 다르며, 생성자를 통해 설정할 수도 있습니다.
2. [`ServerBootstrap`]은 서버를 설정하는 헬퍼 클래스입니다. [`Channel`]을 직접 사용해 서버를 설정할 수도 있지만, 그건 번거롭고 대부분의 경우 그럴 필요가 없습니다.
3. 여기서는 들어오는 연결을 수락하기 위해 새로운 [`Channel`]을 인스턴스화할 때 사용할 [`NioServerSocketChannel`] 클래스를 지정합니다.
4. 여기에 지정한 핸들러는 새로 수락된 [`Channel`]에 의해 항상 평가됩니다. [`ChannelInitializer`]는 사용자가 새 [`Channel`]을 설정할 수 있도록 도와주는 특수한 핸들러입니다. 보통 새 [`Channel`]의 [`ChannelPipeline`]에 `DiscardServerHandler` 같은 핸들러를 추가해 네트워크 애플리케이션을 구현하게 됩니다. 애플리케이션이 복잡해질수록 더 많은 핸들러를 파이프라인에 추가하게 되며, 이 익명 클래스는 결국 별도의 최상위 클래스로 추출하게 됩니다.
5. `Channel` 구현체에 특화된 파라미터도 설정할 수 있습니다. 우리는 TCP/IP 서버를 작성 중이므로 `tcpNoDelay`, `keepAlive` 같은 소켓 옵션을 설정할 수 있습니다. 지원되는 `ChannelOption`의 개요는 [`ChannelOption`] apidoc과 구체적인 [`ChannelConfig`] 구현체를 참고하세요.
6. `option()`과 `childOption()`의 차이를 눈치채셨나요? `option()`은 들어오는 연결을 수락하는 [`NioServerSocketChannel`]을 위한 것입니다. `childOption()`은 부모 [`ServerChannel`](여기서는 [`NioServerSocketChannel`])이 수락한 [`Channel`]들을 위한 것입니다.
7. 이제 준비가 끝났습니다. 남은 일은 포트에 바인드하고 서버를 시작하는 것입니다. 여기서는 머신에 있는 모든 NIC(network interface card)의 `8080` 포트에 바인드합니다. (서로 다른 바인드 주소로) `bind()` 메서드를 원하는 만큼 호출할 수도 있습니다.

축하합니다! Netty 위에서 첫 번째 서버를 막 완성했습니다.

### 받은 데이터 들여다보기

첫 서버를 작성했으니 정말 동작하는지 확인해 봐야겠죠. 가장 쉬운 방법은 *telnet* 명령을 사용하는 것입니다. 예를 들어 명령줄에서 `telnet localhost 8080`을 입력하고 무언가 타이핑해보세요.

하지만 서버가 잘 동작한다고 단언할 수 있을까요? Discard 서버이기 때문에 정말로는 알 수 없습니다. 어떤 응답도 받지 못할 테니까요. 정말 동작하는지 증명하기 위해 받은 데이터를 출력하도록 서버를 수정해 봅시다.

`channelRead()` 메서드는 데이터가 수신될 때마다 호출된다는 사실은 이미 알고 있습니다. `DiscardServerHandler`의 `channelRead()` 메서드에 코드를 좀 넣어봅시다.

```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf in = (ByteBuf) msg;
    try {
        while (in.isReadable()) { // (1)
            System.out.print((char) in.readByte());
            System.out.flush();
        }
    } finally {
        ReferenceCountUtil.release(msg); // (2)
    }
}
```

1. 이 비효율적인 루프는 사실 `System.out.println(in.toString(io.netty.util.CharsetUtil.US_ASCII))`로 단순화할 수 있습니다.
2. 또는 여기서 `in.release()`를 호출해도 됩니다.

다시 *telnet* 명령을 실행하면 서버가 받은 내용을 출력하는 것을 볼 수 있습니다.

Discard 서버의 전체 소스 코드는 배포본의 [`io.netty.example.discard`] 패키지에 있습니다.

### Echo 서버 작성하기

지금까지는 응답 없이 데이터를 소비하기만 했습니다. 그러나 서버는 보통 요청에 응답해야 합니다. 받은 데이터를 그대로 돌려보내는 [`ECHO`](http://tools.ietf.org/html/rfc862) 프로토콜을 구현하면서 클라이언트에 응답 메시지를 작성하는 방법을 배워봅시다.

이전 절에서 만든 discard 서버와의 유일한 차이는 받은 데이터를 콘솔에 출력하는 대신 다시 보낸다는 것입니다. 따라서 다시 한 번 `channelRead()` 메서드만 수정하면 충분합니다.

```java
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg); // (1)
        ctx.flush(); // (2)
    }
```

1. [`ChannelHandlerContext`] 객체는 다양한 I/O 이벤트와 동작을 트리거할 수 있는 여러 연산을 제공합니다. 여기서는 받은 메시지를 그대로 쓰기 위해 `write(Object)`를 호출합니다. `DISCARD` 예제와 달리 받은 메시지를 해제하지 않은 점에 주목하세요. Netty가 메시지를 회선(wire)에 써낼 때 대신 해제해주기 때문입니다.
2. `ctx.write(Object)`는 메시지를 회선까지 내보내지 않습니다. 내부적으로 버퍼링되었다가 `ctx.flush()`에 의해 회선으로 flush됩니다. 간결하게 `ctx.writeAndFlush(msg)`를 호출할 수도 있습니다.

다시 *telnet* 명령을 실행하면 보낸 내용을 그대로 돌려받는 것을 볼 수 있습니다.

Echo 서버의 전체 소스 코드는 배포본의 [`io.netty.example.echo`] 패키지에 있습니다.

### Time 서버 작성하기

이 절에서 구현할 프로토콜은 [`TIME`](http://tools.ietf.org/html/rfc868) 프로토콜입니다. 이 프로토콜은 어떤 요청도 받지 않은 상태에서 32비트 정수를 담은 메시지를 보내고, 메시지를 보낸 후 연결을 닫는다는 점에서 이전 예제들과 다릅니다. 이 예제에서는 메시지를 구성·전송하고 완료 시 연결을 닫는 방법을 배우게 됩니다.

받은 데이터는 모두 무시하고 연결이 수립되자마자 메시지를 보낼 것이므로, 이번에는 `channelRead()` 메서드를 사용할 수 없습니다. 대신 `channelActive()` 메서드를 오버라이드해야 합니다. 다음은 그 구현입니다.

```java
package io.netty.example.time;

public class TimeServerHandler extends ChannelHandlerAdapter {

    @Override
    public void channelActive(final ChannelHandlerContext ctx) { // (1)
        final ByteBuf time = ctx.alloc().buffer(4); // (2)
        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));
        
        final ChannelFuture f = ctx.writeAndFlush(time); // (3)
        f.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) {
                assert f == future;
                ctx.close();
            }
        }); // (4)
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

(이하 4.x User Guide와 거의 동일한 설명이 이어집니다. 핸들러 베이스 클래스가 `ChannelInboundHandlerAdapter`에서 `ChannelHandlerAdapter`로 바뀐 점만 다릅니다. 자세한 설명은 [03_user_guide.md](./03_user_guide.md)의 같은 섹션을 참고하세요.)

Time 서버가 의도대로 동작하는지 테스트하려면 UNIX `rdate` 명령을 사용할 수 있습니다.

```
$ rdate -o <port> -p <host>
```

여기서 `<port>`는 `main()` 메서드에서 지정한 포트 번호이고, `<host>`는 보통 `localhost`입니다.

### Time 클라이언트 작성하기

```java
package io.netty.example.time;

public class TimeClient {
    public static void main(String[] args) throws Exception {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new TimeClientHandler());
                }
            });
            
            // Start the client.
            ChannelFuture f = b.connect(host, port).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
```

```java
public class TimeClientHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf m = (ByteBuf) msg;
        try {
            long currentTimeMillis = (m.readUnsignedInt() - 2208988800L) * 1000L;
            System.out.println(new Date(currentTimeMillis));
            ctx.close();
        } finally {
            m.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

매우 단순해 보이고 서버 측 예제와 다르지 않게 보입니다. 그러나 이 핸들러는 가끔 `IndexOutOfBoundsException`을 던지며 동작을 거부합니다. 그 이유는 다음 절에서 다룹니다.

### 스트림 기반 전송 다루기

#### 소켓 버퍼의 작은 함정

TCP/IP 같은 스트림 기반 전송에서는 받은 데이터가 소켓 수신 버퍼에 저장됩니다. 안타깝게도 스트림 기반 전송의 버퍼는 패킷의 큐가 아니라 바이트의 큐입니다. (이하 4.x User Guide와 동일한 내용이므로 [03_user_guide.md](./03_user_guide.md)를 참고하세요.)

#### 첫 번째 해결책

```java
public class TimeClientHandler extends ChannelHandlerAdapter {
    private ByteBuf buf;
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        buf = ctx.alloc().buffer(4);
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        buf.release();
        buf = null;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf m = (ByteBuf) msg;
        buf.writeBytes(m);
        m.release();
        
        if (buf.readableBytes() >= 4) {
            long currentTimeMillis = (buf.readUnsignedInt() - 2208988800L) * 1000L;
            System.out.println(new Date(currentTimeMillis));
            ctx.close();
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

#### 두 번째 해결책

```java
public class TimeDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }
        out.add(in.readBytes(4));
    }
}
```

```java
b.handler(new ChannelInitializer<SocketChannel>() {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new TimeDecoder(), new TimeClientHandler());
    }
});
```

```java
public class TimeDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(
            ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        out.add(in.readBytes(4));
    }
}
```

추가로, Netty는 대부분의 프로토콜을 매우 쉽게 구현할 수 있게 해주는 즉시 사용 가능한 디코더들을 제공합니다.

* 바이너리 프로토콜의 경우 [`io.netty.example.factorial`]
* 텍스트 라인 기반 프로토콜의 경우 [`io.netty.example.telnet`]

### `ByteBuf` 대신 POJO로 말하기

`UnixTime` 같은 POJO를 도입하면 핸들러가 더 유지보수 가능하고 재사용 가능해집니다.

```java
package io.netty.example.time;

import java.util.Date;

public class UnixTime {

    private final long value;
    
    public UnixTime() {
        this(System.currentTimeMillis() / 1000L + 2208988800L);
    }
    
    public UnixTime(long value) {
        this.value = value;
    }
        
    public long value() {
        return value;
    }
        
    @Override
    public String toString() {
        return new Date((value() - 2208988800L) * 1000L).toString();
    }
}
```

```java
@Override
protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (in.readableBytes() < 4) {
        return;
    }
    out.add(new UnixTime(in.readUnsignedInt()));
}
```

```java
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    UnixTime m = (UnixTime) msg;
    System.out.println(m);
    ctx.close();
}
```

서버 측에서도 같은 기법을 적용합니다.

```java
@Override
public void channelActive(ChannelHandlerContext ctx) {
    ChannelFuture f = ctx.writeAndFlush(new UnixTime());
    f.addListener(ChannelFutureListener.CLOSE);
}
```

인코더 구현은 다음과 같습니다.

```java
package io.netty.example.time;

public class TimeEncoder extends ChannelHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        UnixTime m = (UnixTime) msg;
        ByteBuf encoded = ctx.alloc().buffer(4);
        encoded.writeInt((int) m.value());
        ctx.write(encoded, promise);
    }
}
```

더 단순화하려면 [`MessageToByteEncoder`]를 사용할 수 있습니다.

```java
public class TimeEncoder extends MessageToByteEncoder<UnixTime> {
    @Override
    protected void encode(ChannelHandlerContext ctx, UnixTime msg, ByteBuf out) {
        out.writeInt((int) msg.value());
    }
}
```

마지막으로 남은 작업은 서버 측 [`ChannelPipeline`]에서 `TimeServerHandler` 앞에 `TimeEncoder`를 끼워 넣는 것입니다.

### 애플리케이션 종료하기

Netty 애플리케이션을 종료하는 일은 보통 생성한 모든 [`EventLoopGroup`]에 대해 `shutdownGracefully()`를 호출하는 것만으로 충분합니다. 이 메서드는 [`Future`]를 반환하며, 이 Future는 [`EventLoopGroup`]이 완전히 종료되고 그 그룹에 속한 모든 [`Channel`]이 닫혔을 때 알림을 줍니다.

### 요약

이 장에서는 Netty를 빠르게 둘러보고 Netty 위에서 동작하는 네트워크 애플리케이션을 작성하는 방법을 시연했습니다. 자세한 정보는 이후의 장과 [`io.netty.example`] 패키지의 예제를 참고하세요.

[커뮤니티](http://netty.io/community.html)는 여러분의 질문과 아이디어를 언제나 기다리고 있으며, 여러분의 피드백을 바탕으로 Netty와 그 문서를 계속 개선해 나가고 있습니다.

[`Bootstrap`]: http://netty.io/5.0/api/io/netty/bootstrap/Bootstrap.html
[`ByteBuf`]: http://netty.io/5.0/api/io/netty/buffer/ByteBuf.html
[`ByteBufAllocator`]: http://netty.io/5.0/api/io/netty/buffer/ByteBufAllocator.html
[`ByteToMessageDecoder`]: http://netty.io/5.0/api/io/netty/handler/codec/ByteToMessageDecoder.html
[`Channel`]: http://netty.io/5.0/api/io/netty/channel/Channel.html
[`ChannelConfig`]: http://netty.io/5.0/api/io/netty/channel/ChannelConfig.html
[`ChannelFuture`]: http://netty.io/5.0/api/io/netty/channel/ChannelFuture.html
[`ChannelFutureListener`]: http://netty.io/5.0/api/io/netty/channel/ChannelFutureListener.html
[`ChannelHandlerContext`]: http://netty.io/5.0/api/io/netty/channel/ChannelHandlerContext.html
[`ChannelHandler`]: http://netty.io/5.0/api/io/netty/channel/ChannelHandler.html
[`ChannelHandlerAdapter`]: http://netty.io/5.0/api/io/netty/channel/ChannelHandlerAdapter.html
[`ChannelInitializer`]: http://netty.io/5.0/api/io/netty/channel/ChannelInitializer.html
[`ChannelOption`]: http://netty.io/5.0/api/io/netty/channel/ChannelOption.html
[`ChannelPipeline`]: http://netty.io/5.0/api/io/netty/channel/ChannelPipeline.html
[`ChannelPromise`]: http://netty.io/5.0/api/io/netty/channel/ChannelPromise.html
[`EventLoopGroup`]: http://netty.io/5.0/api/io/netty/channel/EventLoopGroup.html
[`Future`]: http://netty.io/5.0/api/io/netty/util/concurrent/Future.html
[`MessageToByteEncoder`]: http://netty.io/5.0/api/io/netty/handler/codec/MessageToByteEncoder.html
[`NioEventLoopGroup`]: http://netty.io/5.0/api/io/netty/channel/nio/NioEventLoopGroup.html
[`NioServerSocketChannel`]: http://netty.io/5.0/api/io/netty/channel/socket/nio/NioServerSocketChannel.html
[`NioSocketChannel`]: http://netty.io/5.0/api/io/netty/channel/socket/nio/NioSocketChannel.html
[`ReplayingDecoder`]: http://netty.io/5.0/api/io/netty/handler/codec/ReplayingDecoder.html
[`ServerBootstrap`]: http://netty.io/5.0/api/io/netty/bootstrap/ServerBootstrap.html
[`ServerChannel`]: http://netty.io/5.0/api/io/netty/channel/ServerChannel.html
[`SocketChannel`]: http://netty.io/5.0/api/io/netty/channel/socket/SocketChannel.html

[`io.netty.example`]: https://github.com/netty/netty/tree/master/example/src/main/java/io/netty/example
[`io.netty.example.discard`]: http://netty.io/5.0/xref/io/netty/example/discard/package-summary.html
[`io.netty.example.echo`]: http://netty.io/5.0/xref/io/netty/example/echo/package-summary.html
[`io.netty.example.factorial`]: http://netty.io/5.0/xref/io/netty/example/factorial/package-summary.html
[`io.netty.example.telnet`]: http://netty.io/5.0/xref/io/netty/example/telnet/package-summary.html
