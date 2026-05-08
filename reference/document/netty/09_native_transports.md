# Native Transports (네이티브 전송)

> 이 문서는 Netty 공식 Wiki의 "Native transports" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/native-transports.html

---

Netty는 다음과 같은 플랫폼별 JNI 전송을 제공합니다.

- Linux (4.0.16부터)
- MacOS/BSD (4.1.11부터)

이 JNI 전송들은 특정 플랫폼에 특화된 기능을 추가하고, 가비지를 더 적게 만들며, NIO 기반 전송에 비해 일반적으로 더 좋은 성능을 보여줍니다.

## 네이티브 전송 사용하기

해당 네이티브 라이브러리가 포함되도록 의존성에 적절한 classifier를 반드시 지정해야 합니다.

### Linux 네이티브 전송 사용하기

네이티브 전송은 NIO 전송과 호환되므로 다음과 같이 단순한 search-and-replace만 하면 됩니다.

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

### MacOS/BSD 네이티브 전송 사용하기

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

## 네이티브 전송 빌드하기

이미 네이티브 전송 JAR이 있다면, JAR 안에 필요한 공유 라이브러리 파일(`.so`, `.dll`, `.dynlib` 등)이 포함되어 있고 자동으로 로드되므로 직접 빌드할 필요는 없습니다.

### Linux 네이티브 전송 빌드

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

### MacOS/BSD 네이티브 전송 빌드

네이티브 전송을 빌드하려면 MacOS 10.12 이상이 필요합니다. 필요한 도구와 라이브러리도 설치하세요.

```bash
brew install autoconf automake libtool
```
