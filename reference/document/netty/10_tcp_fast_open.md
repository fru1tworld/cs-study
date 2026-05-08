# TCP Fast Open

> 이 문서는 Netty 공식 Wiki의 "TCP Fast Open" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/tcp-fast-open.html

---

## 서문

TCP Fast Open(줄여서 TFO)은 TCP 연결을 수립할 때 초기 SYN 패킷과 함께 소량의 데이터를 보낼 수 있도록 해주는 TCP 프로토콜 확장입니다. 이는 round-trip을 절약하고 특정 상황에서 응답 시간을 줄여줍니다.

다만 TFO는 TCP의 동작을 바꾸기 때문에 항상 사용할 수 있는 것은 아닙니다. SYN 패킷이 재전송되면 수신 측에서 이 데이터가 중복으로 보일 수 있기 때문입니다. 이런 이유로 TFO는 초기 패킷의 데이터를 멱등(idempotent)하게 처리할 수 있을 때만 사용해야 합니다. 이는 프로토콜과 애플리케이션에 따라 다릅니다.

멱등성이 보장되는 중요한 사용 사례 하나는 TLS Client Hello 메시지입니다. 이는 클라이언트가 TLS 핸드셰이크를 시작하기 위해 서버에 보내는 첫 메시지로, TLS 연결 수립 시 round-trip을 절약합니다. `4.1.61.Final` 버전부터 Netty의 `SslHandler`는 가능한 경우 자동으로 TFO를 활용합니다. 구체적으로, 서버 측과 클라이언트 측 TFO는 epoll 전송에서만 지원되며, Linux 커널에서 지원이 활성화되어 있어야 합니다. `4.1.67.Final` 버전부터는 `kqueue` 전송에서도 클라이언트 측 연결에 대해 TCP FastOpen이 지원됩니다.

## TFO 활성화

먼저 운영체제에서 TFO가 지원되고 활성화되어 있어야 합니다.

_MacOS_ 에서는 기본적으로 활성화되어 있습니다.

_Linux_ 에서는 `/proc/sys/net/ipv4/tcp_fastopen` 파일로 제어합니다. 이 파일은 다음 값 중 하나를 가집니다.

0. TFO 비활성화.
1. 나가는 연결(클라이언트)에 대해 TFO 활성화.
2. 들어오는 연결(서버)에 대해 TFO 활성화.
3. 클라이언트와 서버 모두에 대해 TFO 활성화.

`root` 권한으로 원하는 값을 파일에 써서 설정을 바꿀 수 있습니다.

두 번째 단계는 Netty에서 TFO를 활성화하는 것입니다. 서버와 클라이언트는 방식이 다릅니다.

서버의 경우 `ServerBootstrap`에 옵션으로 `ChannelOption.TCP_FASTOPEN`을 설정합니다.

```java
ServerBootstrap sb = ...;
sb.option(ChannelOption.TCP_FASTOPEN, maxPendingFastOpen);
```

이게 전부입니다. 이 옵션은 한 번에 소켓에 보류될 수 있는 fast-open 요청 수를 지정합니다. 아직 수립되지 않은 연결의 fast-open 페이로드에 묶일 수 있는 시스템 자원을 제한하는 셈입니다. 자세한 내용은 [RFC 7413의 Passive Open 부록](https://tools.ietf.org/html/rfc7413#appendix-A.2)을 참고하세요.

클라이언트의 경우는 조금 더 손이 갑니다. 먼저 `Bootstrap`에 `ChannelOption.TCP_FASTOPEN_CONNECT` 옵션을 `true`로 설정합니다.

```java
Bootstrap cb = ...;
cb.option(ChannelOption.TCP_FASTOPEN_CONNECT, true);
```

그런 다음, 재전송으로 인해 여러 번 도착할 수 있다는 점을 고려해 SYN 패킷과 함께 보낼 수 있는 데이터를 결정해야 합니다. 이 데이터는 채널이 연결되기 _전에_ 채널의 outbound 버퍼에 놓아야 합니다. 연결 전 채널을 얻으려면 `register`를 호출합니다. 다음은 그 예시입니다.

```java
Bootstrap cb = new Bootstrap();
cb.option(ChannelOption.TCP_FASTOPEN_CONNECT, true);
// ...handler 설정 등...
Channel channel = cb.register().sync().channel(); // 미연결 채널을 얻는다.

ByteBuf fastOpenData = ...;
ByteBuf normalData = ...;

channel.write(fastOpenData);           // TFO 데이터를 쓴다.
channel.connect(remoteAddress).sync(); // 연결 수립 (TFO 데이터가 flush된다).
channel.write(normalData);             // 이후 TCP 연결은 평소처럼 동작한다.
```

위 코드의 중요한 점: 우리는 `register()`로 만든 채널 위에서 `connect()`를 호출합니다. `Bootstrap.connect()`를 사용하면 자체 outbound 버퍼를 가진 새 채널이 만들어지므로 사용할 수 없습니다.
