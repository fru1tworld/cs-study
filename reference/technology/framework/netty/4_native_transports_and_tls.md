# Netty 네이티브 전송과 TLS

## Native Transports (네이티브 전송)

> 원본: https://netty.io/wiki/native-transports.html

---

Netty는 다음과 같은 플랫폼별 JNI 전송을 제공합니다.

- Linux (4.0.16부터)
- MacOS/BSD (4.1.11부터)

이 JNI 전송들은 특정 플랫폼에 특화된 기능을 제공하고, 가비지를 더 적게 생성하며, NIO 기반 전송에 비해 일반적으로 더 나은 성능을 발휘합니다.

### 네이티브 전송 사용하기

해당 네이티브 라이브러리가 포함되도록 의존성에 적절한 classifier를 반드시 지정해야 합니다.

#### Linux 네이티브 전송 사용하기

네이티브 전송은 NIO 전송과 호환되므로 다음 search-and-replace만 하면 됩니다.

* `NioEventLoopGroup` → `EpollEventLoopGroup`
* `NioEventLoop` → `EpollEventLoop`
* `NioServerSocketChannel` → `EpollServerSocketChannel`
* `NioSocketChannel` → `EpollSocketChannel`

네이티브 전송은 Netty 코어의 일부가 아니므로 Maven `pom.xml`(또는 사용 중인 빌드 시스템에서의 동일 항목)에 `netty-transport-native-epoll`을 의존성으로 추가해야 합니다.

```xml
  <dependencies>
    <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-epoll</artifactId>
      <version>${project.version}</version>
      <classifier>linux-x86_64</classifier>
    </dependency>
    ...
  </dependencies>
```

위에서 classifier는 `linux-x86_64`이고, 이는 의존성에 포함된 네이티브 바이너리가 64비트 x86 CPU 위에서 실행되는 Linux용으로 컴파일되었음을 의미합니다. 다른 CPU 아키텍처나 일부 특정 Linux 배포판은 다른 classifier가 필요합니다.

**참고:** 공식 Linux 빌드는 모두 GLIBC에 링크되어 있습니다. 즉, libc 구현으로 Musl을 사용하는 운영체제는 Netty 네이티브 전송의 공식 빌드에서 지원되지 않습니다. 지원되지 않는 CPU 아키텍처나 libc 구현에서 Netty 네이티브 전송을 사용하려면 직접 빌드해야 합니다. 빌드 방법은 아래에서 안내합니다.

[sbt](http://www.scala-sbt.org/) 프로젝트에서 네이티브 전송을 사용하려면 `libraryDependencies`에 다음을 추가하세요.

```
"io.netty" % "netty-transport-native-epoll" % "${project.version}" classifier "linux-x86_64"
```

#### MacOS/BSD 네이티브 전송 사용하기

네이티브 전송은 NIO 전송과 호환되므로 다음 search-and-replace만 하면 됩니다.

* `NioEventLoopGroup` → `KQueueEventLoopGroup`
* `NioEventLoop` → `KQueueEventLoop`
* `NioServerSocketChannel` → `KQueueServerSocketChannel`
* `NioSocketChannel` → `KQueueSocketChannel`

네이티브 전송은 Netty 코어의 일부가 아니므로 Maven `pom.xml`에 `netty-transport-native-kqueue`를 의존성으로 추가해야 합니다.

```xml
  <dependencies>
    <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-kqueue</artifactId>
      <version>${project.version}</version>
      <classifier>osx-x86_64</classifier>
    </dependency>
    ...
  </dependencies>
```

[sbt](http://www.scala-sbt.org/) 프로젝트에서 사용하려면 `libraryDependencies`에 다음을 추가하세요.

```
"io.netty" % "netty-transport-native-kqueue" % "${project.version}" classifier "osx-x86_64"
```

### 네이티브 전송 빌드하기

이미 네이티브 전송 JAR이 있다면, JAR 안에 필요한 공유 라이브러리 파일(`.so`, `.dll`, `.dylib` 등)이 포함되어 있고 자동으로 로드되므로 직접 빌드할 필요는 없습니다.

#### Linux 네이티브 전송 빌드

네이티브 전송을 빌드하려면 64비트 커널 2.6 이상의 Linux를 사용해야 합니다. 필요한 도구와 라이브러리도 설치하세요.

```bash
# RHEL/CentOS/Fedora:
sudo yum install autoconf automake libtool make tar \
                 glibc-devel \
                 libgcc.i686 glibc-devel.i686
# Debian/Ubuntu:
sudo apt-get install autoconf automake libtool make tar \
                     gcc
```

#### MacOS/BSD 네이티브 전송 빌드

네이티브 전송을 빌드하려면 MacOS 10.12 이상이 필요합니다. 필요한 도구와 라이브러리도 설치하세요.

```bash
brew install autoconf automake libtool
```

---

## TCP Fast Open

> 원본: https://netty.io/wiki/tcp-fast-open.html

---

### 서문

TCP Fast Open(줄여서 TFO)은 TCP 연결을 수립할 때 초기 SYN 패킷과 함께 소량의 데이터를 보낼 수 있도록 해주는 TCP 프로토콜 확장입니다. 이는 round-trip을 절약하고 특정 상황에서 응답 시간을 줄여줍니다.

다만 TFO는 TCP의 동작을 바꾸기 때문에 항상 사용할 수 있는 것은 아닙니다. SYN 패킷이 재전송되면 수신 측에서 이 데이터가 중복으로 보일 수 있기 때문입니다. 이런 이유로 TFO는 초기 패킷의 데이터를 멱등(idempotent)하게 처리할 수 있을 때만 사용해야 합니다. 이는 프로토콜과 애플리케이션에 따라 다릅니다.

멱등성이 보장되는 중요한 사용 사례 하나는 TLS Client Hello 메시지입니다. 이는 클라이언트가 TLS 핸드셰이크를 시작하기 위해 서버에 보내는 첫 메시지로, TLS 연결 수립 시 round-trip을 절약합니다. `4.1.61.Final` 버전부터 Netty의 `SslHandler`는 가능한 경우 자동으로 TFO를 활용합니다. 구체적으로, 서버 측과 클라이언트 측 TFO는 epoll 전송에서만 지원되며, Linux 커널에서 지원이 활성화되어 있어야 합니다. `4.1.67.Final` 버전부터는 `kqueue` 전송에서도 클라이언트 측 연결에 대해 TCP FastOpen이 지원됩니다.

### TFO 활성화

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

이 옵션은 소켓에 동시에 보류될 수 있는 fast-open 요청 수를 지정합니다. 아직 수립되지 않은 연결의 fast-open 페이로드에 묶일 수 있는 시스템 자원을 제한하는 셈입니다. 자세한 내용은 [RFC 7413의 Passive Open 부록](https://tools.ietf.org/html/rfc7413#appendix-A.2)을 참고하세요.

클라이언트의 경우는 설정이 조금 더 필요합니다. 먼저 `Bootstrap`에 `ChannelOption.TCP_FASTOPEN_CONNECT` 옵션을 `true`로 설정합니다.

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

중요한 점은 `register()`로 얻은 채널에서 직접 `connect()`를 호출한다는 것입니다. `Bootstrap.connect()`를 사용하면 자체 outbound 버퍼를 가진 새 채널이 생성되므로 TFO 데이터를 미리 쓸 수 없습니다.

---

## SslContextBuilder와 개인키 (Private Key)

> 원본: https://netty.io/wiki/sslcontextbuilder-and-private-key.html

---

`SslContextBuilder`와 Netty의 `SslContext` 구현은 PKCS8 키만 지원합니다.

다른 형식의 키를 사용하려면 먼저 PKCS8로 변환해야 합니다. openssl을 사용하면 손쉽게 변환할 수 있습니다.

예를 들어 암호화되지 않은 PKCS1 키를 PKCS8로 변환하려면 다음 명령을 사용합니다.

```sh
openssl pkcs8 -topk8 -nocrypt -in pkcs1_key_file -out pkcs8_key.pem
```

자세한 내용은 [OpenSSL 문서](https://www.openssl.org/docs/manmaster/man1/pkcs8.html)를 참고하세요.

---

## Forked Tomcat Native (netty-tcnative)

> 원본: https://netty.io/wiki/forked-tomcat-native.html

---

[netty-tcnative](https://github.com/netty/netty-tcnative)는 [Tomcat Native](http://tomcat.apache.org/native-doc/)의 fork입니다. Twitter, Inc가 기여한 다음과 같은 변경 사항이 포함되어 있습니다.

* 네이티브 라이브러리의 배포와 링크 단순화
* 프로젝트의 완전한 mavenize
* OpenSSL 지원 개선

유지보수 부담을 줄이기 위해 안정 upstream 릴리스마다 전용 브랜치를 만들고 그 위에 변경사항을 올리며, 유지보수하는 브랜치 수는 최소한으로 유지합니다.

### 아티팩트

`netty-tcnative`는 멀티 모듈 프로젝트이고 다양한 환경에서 사용할 수 있도록 여러 아티팩트를 빌드합니다.

| ArtifactId | 설명 | Maven Central 제공 |
| --- | --- | --- |
| netty-tcnative-{os_arch} | "기본" 아티팩트로, [libapr-1](https://apr.apache.org/)과 [OpenSSL](https://www.openssl.org/) 양쪽에 대해 동적 링크되어 있습니다. 이 아티팩트를 사용하려면 시스템에 libapr-1과 OpenSSL이 설치되고 설정되어 있어야 합니다. 사이트 관리자가 애플리케이션을 다시 빌드하지 않고도 OpenSSL을 자유롭게 업그레이드할 수 있는 운영 환경에서 유용합니다. 자체 APR/OpenSSL 빌드를 만들지 않는 한 Windows에서는 지원되지 않습니다. | yes |
| netty-tcnative-boringssl-static-{os_arch} | Google의 [boringssl](https://boringssl.googlesource.com/)에 정적 링크된 아티팩트입니다. boringssl은 OpenSSL의 fork로, 코드 풋프린트를 줄이고 작성 시점 기준 안정 Linux 릴리스에는 아직 포함되지 않은 추가 기능(ALPN 등)을 제공합니다. 정적 링크 덕분에 추가 설치 단계 없이 시스템에서 tcnative를 바로 사용할 수 있습니다. APR이 필요하지 않습니다. | yes |
| netty-tcnative-boringssl-static | 지원되는 모든 `netty-tcnative-boringssl-static-{os_arch}` 정적 링크 라이브러리를 담은 uber jar입니다. jar 크기가 다소 크지만, 애플리케이션이 플랫폼별로 올바른 jar를 직접 선택할 필요가 없어 설정이 크게 단순해집니다. | yes |
| netty-tcnative-openssl-static-{os_arch} | libapr-1과 OpenSSL 양쪽에 정적 링크된 아티팩트입니다. 추가 설치 단계 없이 tcnative를 바로 사용할 수 있습니다. | no |
| netty-tcnative-libressl-static-{os_arch} | 곧 출시 예정. | no |

### Gradle와 Bazel

Maven 패키징이 `2.0.49.Final` 버전에서 약간 달라졌습니다. Maven 사용자에게는 문제가 없었지만 Gradle/Bazel 사용자에게는 약간의 문제를 일으킵니다.

- Gradle의 경우, `tcnative` 의존성을 _classifier와 함께_ 명시적으로 선언하는 것이 해결책입니다.
- Bazel의 경우, 이 수정이 포함된 Bazel 버전을 사용하는 것이 해결책입니다: https://github.com/bazelbuild/rules_jvm_external/pull/687

### netty-tcnative-boringssl "uber" jar 다운로드 방법

플랫폼별 classifier가 필요 없어 가장 단순하게 사용할 수 있는 의존성입니다.

```xml
<project>
  ...
  <dependencies>
    ...
    <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-tcnative-boringssl-static</artifactId>
      <version>2.0.0.Final</version>
    </dependency>
    ...
  </dependencies>
  ...
</project>
```

### netty-tcnative-boringssl-static 다운로드 방법

애플리케이션의 `pom.xml`에 `os-maven-plugin` 확장과 플랫폼에 맞는 `netty-tcnative-boringssl-static` 의존성을 추가합니다.

```xml
<project>
  ...
  <dependencies>
    ...
    <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-tcnative-boringssl-static</artifactId>
      <version>2.0.0.Final</version>
      <classifier>${os.detected.classifier}</classifier>
    </dependency>
    ...
  </dependencies>
  ...
  <build>
    ...
    <extensions>
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.4.0.Final</version>
      </extension>
    </extensions>
    ...
  </build>
  ...
</project>
```

netty-tcnative는 [Maven Central](http://repo1.maven.org/maven2/io/netty/netty-tcnative/)에 배포 시 classifier를 사용해 다음 플랫폼용 배포본을 제공합니다.

| Classifier | 설명 |
| --- | --- |
| windows-x86_64 | Windows 배포본 |
| osx-x86_64 | Mac 배포본 |
| linux-x86_64 | Linux 배포본 |

### netty-tcnative 다운로드 방법

애플리케이션 `pom.xml`에 `os-maven-plugin` 확장과 `netty-tcnative` 의존성을 추가합니다.

```xml
<project>
  <properties>
    <!-- Fedora 'like' 시스템에서 classifier가 확장되도록 -->
    <!-- os-maven-plugin 확장을 설정한다. -->
     <os.detection.classifierWithLikes>fedora</os.detection.classifierWithLikes>
  </properties>
  ...
  <dependencies>
    ...
    <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-tcnative</artifactId>
      <version>2.0.0.Final</version>
      <classifier>${os.detected.classifier}</classifier>
    </dependency>
    ...
  </dependencies>
  ...
  <build>
    ...
    <extensions>
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.4.0.Final</version>
      </extension>
    </extensions>
    ...
  </build>
  ...
</project>
```

netty-tcnative는 [Maven Central](http://repo1.maven.org/maven2/io/netty/netty-tcnative/)에 배포 시 다양한 플랫폼용 배포본을 위해 classifier를 사용합니다. Linux에서 OpenSSL은 Fedora 파생 배포판과 그 외 Linux 릴리스에서 서로 다른 soname을 사용합니다. `1.1.33.Fork7` 버전부터는 이 한계를 우회하기 위해 Linux용으로 두 가지 별도 버전을 배포합니다(아래 표 참고).

| Classifier | 설명 |
| --- | --- |
| windows-x86_64 | Windows 배포본 (대신 boringssl 사용 권장. 아래 복잡성 참고) |
| osx-x86_64 | Mac 배포본 |
| linux-x86_64 | Fedora 파생이 아닌 Linux용 |
| linux-x86_64-fedora | Fedora 파생용 |

`classifierWithLikes`를 사용하면 `os-maven-plugin`이 생성하는 `os.detected.classifier` 프로퍼티 값이 바뀝니다. 빌드에서 `os.detected.classifier`에 의존하는 다른 의존성이 있다면, `antrun` 플러그인으로 netty-tcnative classifier를 직접 구성할 수도 있습니다.

```xml
<project>
  <dependencies>
    <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-tcnative</artifactId>
      <version>2.0.0.Final</version>
      <classifier>${tcnative.classifier}</classifier>
    </dependency>
  </dependencies>

  <build>
    <extensions>
      <!-- "os.detected" 프로퍼티 초기화에 os-maven-plugin 사용 -->
      <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>1.4.0.Final</version>
      </extension>
    </extensions>
    <plugins>
      <!-- 적절한 "tcnative.classifier" 프로퍼티를 설정하기 위해 Ant 사용 -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-antrun-plugin</artifactId>
        <executions>
          <execution>
            <phase>initialize</phase>
            <configuration>
              <exportAntProperties>true</exportAntProperties>
              <target>
                <condition property="tcnative.classifier"
                           value="${os.detected.classifier}-fedora"
                           else="${os.detected.classifier}">
                  <isset property="os.detected.release.fedora"/>
                </condition>
              </target>
            </configuration>
            <goals>
              <goal>run</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

### 사용 방법

OpenSSL 기반 `SSLEngine` 구현을 사용하려면 `io.netty.handler.ssl.SslContext` 클래스를 사용합니다.

```java
public static void main(String[] args) throws Exception {
  File certificate = new File("certificate");
  File privateKey = new File("privateKey");
  SslContext sslContext =
    SslContextBuilder.forServer(certificate, privateKey)
                     .sslProvider(SslProvider.OPENSSL)
                     .build();
  ...
}

public class MyChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final SslContext sslCtx;
  
  public MyChannelInitializer(SslContext sslCtx) {
    this.sslCtx = sslCtx;
  }
  
  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline p = ch.pipeline();
    if (sslCtx != null) {
      p.addLast(sslCtx.newHandler(ch.alloc()));
    }
    ...
  }
}
```

Netty는 netty-tcnative-XXX 아티팩트에 포함된 공유 라이브러리를 임시 디렉터리에 압축 해제한 뒤 `java.lang.System.load(String)`로 로드합니다.

#### 동적 링크된 `netty-tcnative`의 사전 요구사항

`netty-tcnative` 아티팩트의 공유 라이브러리는 Apache Portable Runtime(APR)과 OpenSSL에 동적 링크되어 있습니다. 이 라이브러리들은 시스템 라이브러리 디렉터리, `$LD_LIBRARY_PATH`, `%PATH%` 등 라이브러리 탐색 경로에 위치해야 합니다.

* Linux를 사용 중이라면 시스템 패키지 매니저로 설치할 수 있으니 별다른 작업이 필요 없을 가능성이 큽니다.
* Mac을 사용 중이라면 [Homebrew](http://brew.sh/)로 `openssl` 패키지를 설치해야 합니다.
* Windows를 사용 중이라면:
  * APR을 직접 빌드하고,
  * [Windows용 OpenSSL](http://slproweb.com/products/Win32OpenSSL.html)을 설치하고,
  * .DLL 파일이 들어 있는 디렉터리들을 `%PATH%`에 추가해야 합니다.

**중요:** Linux에서 ALPN을 사용하려면(HTTP/2에 필요) openssl 1.0.2 이상이 설치되어 있어야 합니다. 배포판 패키지 시스템이 이 버전을 제공하지 않는다면 직접 컴파일하고 [How to build](http://netty.io/wiki/forked-tomcat-native.html#wiki-h2-2)에서 설명하는 대로 LD_LIBRARY_PATH를 설정해야 합니다.

#### 정적 링크된 `netty-tcnative-*-static`의 사전 요구사항

Fedora 30 이상을 사용 중이라면 `dnf -y install libxcrypt-compat`을 실행해 필요한 의존성을 설치하세요.

설치하지 않으면 다음과 같은 오류가 발생할 수 있습니다.

> "libcrypt.so.1: cannot open shared object file: No such file or directory"

### 빌드 방법

`Linux x86_64`, `Mac OS X x86_64`, `Windows x86_64`용 네이티브 라이브러리 JAR를 공식적으로 배포하므로 보통 `netty-tcnative`를 직접 빌드할 필요는 없습니다. SNAPSHOT 빌드가 필요하다면 [Sonatype Snapshots](https://oss.sonatype.org/content/repositories/snapshots/io/netty/)을 확인하세요.

Windows x86_32처럼 네이티브 라이브러리 JAR를 공식 배포하지 않는 플랫폼에서는 이 절의 지침을 따르세요.

#### 의존성 업데이트와 검증

직접 빌드할 때는 서드파티 의존성의 무결성을 검증하는 것이 중요합니다. 또한 의존성을 업데이트하면 체크섬 불일치가 발생할 수 있으므로, 패키지 메인테이너는 체크섬을 갱신하기 전에 패키지 무결성을 먼저 확인해야 합니다.

#### autoconf
- http://ftp.gnu.org/gnu/autoconf/autoconf-X.Y.Z.tar.gz 에서 tgz와 tgz.sig를 다운로드
- `gpg --keyserver keys.gnupg.net --recv-keys 2527436A`
- `gpg --verify autoconf-X.Y.Z.tar.gz.sig`

#### apr
- https://apr.apache.org/download.cgi 에서 다운로드
- apr의 MD5 sum을 안전하게 가져오기: apache 사이트는 http로 링크되어 있으나 https로 변경
- sum을 파일에 붙여넣기
- `md5 -r apr-X.Y.Z.tar.gz | cut -d " " -f 1 | diff -u apr-X.Y.Z.tar.gz.md5 -` 로 sum 검증

#### libressl
- signify 공개키: RWQg/nutTVqCUVUw8OhyHt9n51IC8mdQRd1b93dOyVrwtIXmMI+dtGFe
- (공개키는 https://www.openbsd.org/libressl/signing.html 에서 받을 수 있음)
- http://ftp.openbsd.org/pub/OpenBSD/LibreSSL/ 에서 tarball, signature, SHA256.sig 다운로드
- https://www.openbsd.org/libressl/signing.html 의 libressl.pub로 검증:
  `signify -C -x SHA256.sig  -p /path/to/libressl.pub libressl-X.Y.Z.tar.gz`

#### openssl
- https://www.openssl.org/source/ 에서 tarball과 SHA256 서명을 다운로드
- `sha1sum openssl-X.Y.Z.tar.gz | cut -d " " -f 1 | diff -u openssl-X.Y.Z.tar.gz.sha256 -` 로 sum 검증

#### boringssl

boringssl은 현재 tcnative 빌드에서 hermetic하게 버전 관리되지 않습니다. 대신 빌드 시 [upstream Google git repo](https://boringssl.googlesource.com/boringssl/)의 `chromium-stable` 브랜치에서 직접 가져옵니다.

#### Linux에서 빌드

| 사전 요구사항 | 설명 |
| --- | --- |
| 기본 도구 | autoconf, automake, libtool, glibc-devel, make, tar, [xutils-dev 또는 imake] |
| APR | apr-devel 또는 libapr1-dev |
| OpenSSL | openssl-devel 또는 libssl-dev |
| GCC | 4.8 이상 필요 |
| [CMake](https://cmake.org/download/) | 2.8.8 이상 필요 |
| Perl | 5.6.1 이상 필요 |
| [Ninja](https://ninja-build.org/) | 1.3.4 이상 필요 |
| [Go](https://golang.org/dl/) | 1.5.1 이상 필요 |

패키지를 빌드하고 로컬 Maven 저장소에 설치합니다.

```bash
git clone https://github.com/netty/netty-tcnative.git
cd netty-tcnative
# 특정 버전 빌드: (예: netty-tcnative-2.0.0.Final)
git checkout netty-tcnative-[version]
# 스냅샷 빌드: (예: 2.0.0.Final)
git checkout [branch]
./mvnw clean install
```

#### Linux(s390x)에서 빌드

IBM Z(s390x)에서 Linux용 `netty-tcnative` 라이브러리를 빌드하려면 [여기](https://github.com/linux-on-ibm-z/docs/wiki/Building-netty-tcnative)의 지침을 따라 빌드한 뒤 로컬 Maven 저장소에 설치하세요.

#### Mac OS X에서 빌드

먼저 [Xcode](https://itunes.apple.com/de/app/xcode/id497799835?mt=12)를 설치한 뒤 명령줄 도구도 설치하세요.

```bash
xcode-select --install
```

`netty-tcnative` 저장소를 clone하고 [Homebrew](http://brew.sh/)로 필요한 패키지를 설치합니다.

```bash
git clone https://github.com/netty/netty-tcnative.git
cd netty-tcnative
brew bundle
```

패키지를 빌드하고 로컬 Maven 저장소에 설치합니다.

```bash
# 특정 버전 빌드: (예: netty-tcnative-2.0.0.Final)
git checkout netty-tcnative-[version]
# 스냅샷 빌드: (예: 2.0.0.Final)
git checkout [branch]
./mvnw clean install
```

#### Windows에서 빌드

이 절은 64비트 Windows에서의 빌드를 다룹니다. 32비트 시스템에서는 일부 단계가 달라질 수 있습니다.

다음 패키지들을 설치하세요.

* [Visual C++ 2013 Express](https://www.visualstudio.com/en-us/news/vs2013-community-vs.aspx)
* [Windows 10 SDK](https://dev.windows.com/en-us/downloads/windows-10-sdk)
* [CMake](https://cmake.org/files/v3.4/cmake-3.4.3-win32-x86.exe)
  * 설치 중 `Add CMake to the system PATH for current user`를 선택
* [Perl](http://www.activestate.com/activeperl/downloads/thank-you?dl=http://downloads.activestate.com/ActivePerl/releases/5.22.1.2201/ActivePerl-5.22.1.2201-MSWin32-x64-299574.msi)
* [Ninja](https://github.com/ninja-build/ninja/releases/download/v1.6.0/ninja-win.zip)
  * 실행 파일을 `C:\Workspaces\`에 압축 해제하고 `C:\Workspaces`를 `PATH`에 추가
* [Go](https://storage.googleapis.com/golang/go1.5.3.windows-amd64.msi)
* [Yasm](http://www.tortall.net/projects/yasm/releases/yasm-1.3.0-win64.exe)
  * `C:\Workspaces`에 다운로드
  * 환경 변수 `ASM_NASM=C:\Workspaces\yasm-1.3.0-win64.exe` 설정
* [OpenSSL](http://slproweb.com/download/Win64OpenSSL-1_0_2f.exe)
  * 설치 중 `Copy OpenSSL DLLs to: The OpenSSL binaries (/bin) directory` 선택
  * 다음 환경 변수 설정:
    * `OPENSSL_INCLUDE_DIR=C:\OpenSSL-Win64\include`
    * `OPENSSL_LIB_DIR=C:\OpenSSL-Win64\lib`
* [Apache Portable Runtime (APR) 1.5.2](http://www.us.apache.org/dist/apr/apr-1.5.2-win32-src.zip)
  * `C:\Workspaces\apr-1.5.2`에 압축 해제
  * `C:\Workspaces\apr-1.5.2`에서 명령 프롬프트(`cmd.exe`)로 다음 명령 실행:
    * `"C:\Program Files (x86)\Microsoft Visual Studio 12.0\VC\vcvarsall.bat" x86_amd64`
    * `nmake /f Makefile.win ARCH="x64 Release" PREFIX=..\apr-1.5.2-dist buildall install`
    * `xcopy include\arch\win32\*.h ..\apr-1.5.2-dist\include /d`
    * `xcopy include\arch\*.h ..\apr-1.5.2-dist /d`
  * 다음 환경 변수 설정:
    * `APR_INCLUDE_DIR=C:\Workspaces\apr-1.5.2-dist\include`
    * `APR_LIB_DIR=C:\Workspaces\apr-1.5.2-dist\lib`

명령 프롬프트(`cmd.exe`)를 열고 Visual C++ 환경 변수를 로드합니다.

```bat
"C:\Program Files (x86)\Microsoft Visual Studio 12.0\VC\vcvarsall.bat" x86_amd64
```

`netty-tcnative`를 clone하고 빌드합니다.

```bat
git clone https://github.com/netty/netty-tcnative.git
cd netty-tcnative
REM 특정 버전 빌드: (예: netty-tcnative-2.0.0.Final)
git checkout netty-tcnative-[version]
REM 스냅샷 빌드: (예: 2.0.0.Final)
git checkout [branch]
mvnw clean install
```

### 새 fork 만들기

`bootstrap` 브랜치를 체크아웃하고 upstream tcnative 버전을 인수로 `new-fork` 스크립트를 실행하면, upstream tcnative 버전과 동일한 이름의 완전히 mavenize된 새 브랜치로 이동합니다. 예를 들어:

```
$ git checkout bootstrap
$ ./new-fork 1.1.29 1
```

위 명령은 `tcnative-1.1.29`의 mavenize된 fork를 담은 `1.1.29` 브랜치를 새로 만듭니다. 이 fork에는 `tcnative` 메인 소스 코드를 수정하는 패치가 포함되어 있지 않으므로, 이미 패치된 다른 브랜치에서 일부 커밋을 cherry-pick해야 할 수 있습니다.
