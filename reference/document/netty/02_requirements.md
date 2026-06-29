# Netty 4.x 요구사항

> 원본: https://netty.io/wiki/requirements-for-4.x.html

---

# Netty

Netty 프로젝트는 다양한 서브 모듈로 구성되어 있습니다. 각 서브 모듈의 구체적인 요구사항은 아래 해당 섹션을 참고하세요.

일반적으로 각 서브 모듈의 기본 기능은 실행 시 Java 6 이상, 컴파일 시 Java 7 이상이 필요합니다.

# [codec-http2](https://github.com/netty/netty/tree/4.1/codec-http2)

이 코덱은 [HPACK](https://tools.ietf.org/html/rfc7541)을 포함한 [HTTP/2 프로토콜](https://tools.ietf.org/html/rfc7540) 구현을 제공합니다.

## 전송 보안 (TLS)

[HTTP/2 RFC](https://tools.ietf.org/html/rfc7540#section-3.3)는 TLS 사용을 강제하지는 않지만, TLS를 사용하는 경우에는 RFC가 요구하는 조건을 따라야 합니다 [[1](https://tools.ietf.org/html/rfc7540#section-9.2)][[2](https://tools.ietf.org/html/rfc7540#section-3.3)][[3](https://tools.ietf.org/html/rfc7540#section-3.4)].

TLS 위에서 동작하는 HTTP/2는 `h2` 프로토콜 협상을 위해 [ALPN](https://tools.ietf.org/html/rfc7301) 사용을 의무화합니다. ALPN은 비교적 최신 표준이므로, ALPN을 지원하지 않는 시스템을 위해 Netty는 가능한 경우 [NPN](https://tools.ietf.org/html/draft-agl-tls-nextprotoneg-04)을 통한 프로토콜 협상도 지원합니다.

### OpenSSL을 사용한 TLS

현재 Netty에서 TLS를 다룰 때 권장되는 방식입니다.

#### OpenSSL을 사용했을 때의 이점

1. **속도**: 로컬 테스트 결과 JDK 대비 3배 정도의 성능 향상이 관찰되었습니다. [HTTP/2 RFC](https://tools.ietf.org/html/rfc7540#section-9.2.2)가 요구하는 유일한 cipher suite에서 사용되는 GCM은 10~500배 더 빠릅니다.
2. **Cipher**: OpenSSL은 자체 cipher를 제공하기 때문에 JDK의 한계에 종속되지 않습니다. 그 결과 Java 7에서도 GCM을 사용할 수 있습니다.
3. **ALPN에서 NPN으로의 fallback**: OpenSSL은 ALPN과 NPN을 동시에 지원할 수 있습니다. Netty의 JDK 구현은 한 번에 ALPN 또는 NPN 중 하나만 지원하며, [NPN은 JDK 7에서만 지원](https://wiki.eclipse.org/Jetty/Feature/NPN)됩니다.
4. **Java 버전 비종속성**: JDK 업데이트 버전마다 다른 라이브러리 버전을 써야 할 필요가 없습니다. 이는 Netty가 사용하는 JDK ALPN/NPN 구현의 한계입니다.

#### OpenSSL 사용을 위한 요구사항

1. ALPN 지원을 위해서는 [OpenSSL](https://www.openssl.org/) 1.0.2 이상, NPN의 경우 1.0.1 이상이 필요합니다.
2. [netty-tcnative](https://github.com/netty/netty-tcnative) 1.1.33.Fork7 이상이 클래스패스에 있어야 합니다.
3. netty-tcnative의 지원 플랫폼: `linux-x86_64`, `mac-x86_64`, `windows-x86_64`. 그 외 플랫폼을 지원하려면 netty-tcnative를 직접 빌드해야 합니다.

위 요구사항이 모두 충족되면 Netty는 OpenSSL을 기본 TLS 제공자로 자동 선택합니다.

#### netty-tcnative 설정

[netty-tcnative wiki](http://netty.io/wiki/forked-tomcat-native.html)를 참고하세요.

### JDK를 사용한 TLS (Jetty ALPN/NPN)

OpenSSL을 사용할 수 없다면 JDK를 사용한 TLS가 대안이 됩니다.

Java 8u251 및 Java 9부터 ALPN/NPN을 기본 지원합니다. 그 이전 JDK에서는 OpenJDK용 [Jetty-ALPN](https://github.com/jetty-project/jetty-alpn) (Java 8 미만은 [Jetty-NPN](https://github.com/jetty-project/jetty-npn)) bootclasspath 확장을 사용해야 합니다. Jetty `alpn-boot` jar 경로를 `Xbootclasspath` JVM 옵션으로 지정합니다.

```sh
java -Xbootclasspath/p:/path/to/jetty/alpn/extension.jar ...
```

사용 중인 Java 버전에 [정확히 맞는 Jetty-ALPN jar 릴리스](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions)를 사용해야 한다는 점을 유의하세요.

#### JDK Cipher

Java 7은 HTTP/2 RFC가 [권장하는 cipher suite](https://tools.ietf.org/html/rfc7540#section-9.2.2)를 지원하지 **않습니다**. 가능하면 Java 8 사용을 권장하며, [Bouncy Castle](https://www.bouncycastle.org/java.html) 같은 대체 JCE 구현을 쓰는 방법도 있습니다. 이마저 어렵다면 다른 cipher를 사용할 수도 있지만, 대상 서비스가 HTTP/2 RFC가 금지하는 해당 cipher를 지원하는지, 그로 인한 보안 위험은 없는지 반드시 확인해야 합니다.

또한 Java 8u60 이전에는 GCM이 [매우 느리다(1 MB/s)](https://bugzilla.redhat.com/show_bug.cgi?id=1135504)는 점에 유의하세요. Java 8u60에서 GCM은 10배 빨라졌지만(10~20 MB/s), 그래도 OpenSSL(약 200 MB/s, AES-NI 지원 시 약 1 GB/s)에 비하면 여전히 느립니다. GCM cipher suite는 HTTP/2의 cipher 요구사항을 충족하는 유일한 suite입니다.

### ALPN 또는 NPN 활성화

[SslContextBuilder](https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/ssl/SslContextBuilder.java#L279)는 ALPN/NPN을 설정하기 위한 [ApplicationProtocolConfig](https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/ssl/ApplicationProtocolConfig.java) setter를 제공합니다. ALPN 사용 예시는 [HTTP/2 예제](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http2/helloworld), NPN 사용 예시는 [SPDY 예제](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/spdy)를 참고하세요.
