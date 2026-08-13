# Netty 소개, 요구사항, 개발 환경

## Netty 소개

> 원본: https://netty.io/wiki/index.html
> 원본 소스: https://github.com/netty/netty/wiki/Home

---

### 그 외 자료

- [Netty 홈페이지](https://netty.io/)
- [문서 홈](https://netty.io/wiki/index.html)
- [전체 문서](https://netty.io/wiki/all-documents.html)
- [서드파티 글 모음](https://netty.io/wiki/related-articles.html)
- [서드파티 프로젝트](https://netty.io/wiki/related-projects.html)
- [Netty를 채택한 사례 (Adopters)](https://netty.io/wiki/adopters.html)
- [Wiki 변경 이력](https://github.com/netty/netty/wiki/_history)

---

### 문서 구성

- 01 `01_introduction.md`: 원문 Home
- 02 `02_requirements.md`: 원문 Requirements for 4.x
- 03 `03_user_guide.md`: 원문 User Guide for 4.x
- 04 `04_developer_guide.md`: 원문 Developer Guide
- 05 `05_setting_up_dev.md`: 원문 Setting up Development Environment
- 06 `06_thread_model.md`: 원문 Thread Model
- 07 `07_thread_affinity.md`: 원문 Thread Affinity
- 08 `08_reference_counted.md`: 원문 Reference Counted Objects
- 09 `09_native_transports.md`: 원문 Native Transports
- 10 `10_tcp_fast_open.md`: 원문 TCP Fast Open
- 11 `11_ssl_context.md`: 원문 SslContextBuilder and Private Key
- 12 `12_forked_tomcat_native.md`: 원문 Forked Tomcat Native
- 13 `13_memory_allocator.md`: 원문 Analyzing Memory Allocator Behavior
- 14 `14_microbenchmarks.md`: 원문 Microbenchmarks
- 15 `15_using_as_library.md`: 원문 Using as a Generic Library
- 16 `16_java24_unsafe.md`: 원문 Java 24 and sun.misc.Unsafe
- 17 `17_threat_model.md`: 원문 Threat Model
- 18 `18_new_in_4_0.md`: 원문 New and Noteworthy in 4.0
- 19 `19_new_in_4_1.md`: 원문 New and Noteworthy in 4.1
- 20 `20_migration_4_2.md`: 원문 Netty 4.2 Migration Guide
- 21 `21_new_in_5_0.md`: 원문 New and Noteworthy in 5.0
- 22 `22_migration_5.md`: 원문 Netty 5 Migration Guide
- 23 `23_user_guide_5.md`: 원문 User Guide for 5.x
- 24 `24_related_articles.md`: 원문 Related Articles
- 25 `25_related_projects.md`: 원문 Related Projects

---

## Netty 4.x 요구사항

> 원본: https://netty.io/wiki/requirements-for-4.x.html

---

## Netty

Netty 프로젝트는 다양한 서브 모듈로 구성됨 → 각 서브 모듈의 구체적인 요구사항은 아래 해당 섹션 참고.

일반적으로 각 서브 모듈의 기본 기능은 실행 시 Java 6 이상, 컴파일 시 Java 7 이상 필요.

## [codec-http2](https://github.com/netty/netty/tree/4.1/codec-http2)

이 코덱은 [HPACK](https://tools.ietf.org/html/rfc7541)을 포함한 [HTTP/2 프로토콜](https://tools.ietf.org/html/rfc7540) 구현 제공.

### 전송 보안 (TLS)

[HTTP/2 RFC](https://tools.ietf.org/html/rfc7540#section-3.3)는 TLS 사용을 강제하지 않음 → 다만 TLS를 사용하는 경우에는 RFC가 요구하는 조건을 따라야 함 [[1](https://tools.ietf.org/html/rfc7540#section-9.2)][[2](https://tools.ietf.org/html/rfc7540#section-3.3)][[3](https://tools.ietf.org/html/rfc7540#section-3.4)].

TLS 위에서 동작하는 HTTP/2는 `h2` 프로토콜 협상을 위해 [ALPN](https://tools.ietf.org/html/rfc7301) 사용을 의무화함. ALPN은 비교적 최신 표준 → ALPN을 지원하지 않는 시스템을 위해 Netty는 가능한 경우 [NPN](https://tools.ietf.org/html/draft-agl-tls-nextprotoneg-04)을 통한 프로토콜 협상도 지원.

#### OpenSSL을 사용한 TLS

현재 Netty에서 TLS를 다룰 때 권장되는 방식.

##### OpenSSL을 사용했을 때의 이점

- 속도: 로컬 테스트 결과 JDK 대비 3배 정도의 성능 향상 관찰됨. [HTTP/2 RFC](https://tools.ietf.org/html/rfc7540#section-9.2.2)가 요구하는 유일한 cipher suite에서 사용되는 GCM은 10~500배 더 빠름
- Cipher: OpenSSL은 자체 cipher 제공 → JDK의 한계에 종속되지 않음. 그 결과 Java 7에서도 GCM 사용 가능
- ALPN에서 NPN으로의 fallback: OpenSSL은 ALPN과 NPN을 동시에 지원 가능. Netty의 JDK 구현은 한 번에 ALPN 또는 NPN 중 하나만 지원하며, [NPN은 JDK 7에서만 지원](https://wiki.eclipse.org/Jetty/Feature/NPN)됨
- Java 버전 비종속성: JDK 업데이트 버전마다 다른 라이브러리 버전을 쓸 필요 없음. 이는 Netty가 사용하는 JDK ALPN/NPN 구현의 한계

##### OpenSSL 사용을 위한 요구사항

- ALPN 지원을 위해서는 [OpenSSL](https://www.openssl.org/) 1.0.2 이상 필요, NPN의 경우 1.0.1 이상 필요
- [netty-tcnative](https://github.com/netty/netty-tcnative) 1.1.33.Fork7 이상이 클래스패스에 있어야 함
- netty-tcnative의 지원 플랫폼: `linux-x86_64`, `mac-x86_64`, `windows-x86_64`. 그 외 플랫폼을 지원하려면 netty-tcnative를 직접 빌드해야 함

위 요구사항이 모두 충족되면 Netty는 OpenSSL을 기본 TLS 제공자로 자동 선택함.

##### netty-tcnative 설정

[netty-tcnative wiki](http://netty.io/wiki/forked-tomcat-native.html) 참고.

#### JDK를 사용한 TLS (Jetty ALPN/NPN)

OpenSSL을 사용할 수 없다면 JDK를 사용한 TLS가 대안.

Java 8u251 및 Java 9부터 ALPN/NPN 기본 지원. 그 이전 JDK에서는 OpenJDK용 [Jetty-ALPN](https://github.com/jetty-project/jetty-alpn) (Java 8 미만은 [Jetty-NPN](https://github.com/jetty-project/jetty-npn)) bootclasspath 확장 사용 필요. Jetty `alpn-boot` jar 경로를 `Xbootclasspath` JVM 옵션으로 지정.

```sh
java -Xbootclasspath/p:/path/to/jetty/alpn/extension.jar ...
```

사용 중인 Java 버전에 [정확히 맞는 Jetty-ALPN jar 릴리스](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-versions) 사용 필요.

##### JDK Cipher

Java 7은 HTTP/2 RFC가 [권장하는 cipher suite](https://tools.ietf.org/html/rfc7540#section-9.2.2)를 지원하지 않음. 가능하면 Java 8 사용 권장, [Bouncy Castle](https://www.bouncycastle.org/java.html) 같은 대체 JCE 구현을 쓰는 방법도 있음. 이마저 어렵다면 다른 cipher를 사용할 수도 있으나, 대상 서비스가 HTTP/2 RFC가 금지하는 해당 cipher를 지원하는지, 그로 인한 보안 위험은 없는지 반드시 확인 필요.

또한 Java 8u60 이전에는 GCM이 [매우 느리다(1 MB/s)](https://bugzilla.redhat.com/show_bug.cgi?id=1135504)는 점 유의. Java 8u60에서 GCM은 10배 빨라졌으나(10~20 MB/s), 그래도 OpenSSL(약 200 MB/s, AES-NI 지원 시 약 1 GB/s)에 비하면 여전히 느림. GCM cipher suite는 HTTP/2의 cipher 요구사항을 충족하는 유일한 suite.

#### ALPN 또는 NPN 활성화

[SslContextBuilder](https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/ssl/SslContextBuilder.java#L279)는 ALPN/NPN을 설정하기 위한 [ApplicationProtocolConfig](https://github.com/netty/netty/blob/4.1/handler/src/main/java/io/netty/handler/ssl/ApplicationProtocolConfig.java) setter 제공. ALPN 사용 예시는 [HTTP/2 예제](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/http2/helloworld), NPN 사용 예시는 [SPDY 예제](https://github.com/netty/netty/tree/4.1/example/src/main/java/io/netty/example/spdy) 참고.

---

## Developer Guide (개발자 가이드)

> 원본: https://netty.io/wiki/developer-guide.html

---

> 튜토리얼을 찾는 경우 [문서 홈](https://netty.io/wiki/index.html) 방문. 질문이 있는 경우 [StackOverflow.com](https://stackoverflow.com/questions/tagged/netty)에 질문.
>
> 이 가이드는 'user guide'가 아님. Netty를 사용해 애플리케이션을 만드는 '사용자'가 아니라, Netty 자체를 개발하려는 'dev'(기여자)를 위한 문서.

#### 시작하기 전에

* [개발 환경을 설정.](./05_setting_up_dev.md)
* 단일 라인 변경이나 오타 수정 같은 사소한 기여가 아니라면, [Individual Contributor License Agreement (icla)](http://netty.io/s/icla)를 읽고 서명하거나, 고용주가 [Corporate Contributor License Agreement (ccla)](http://netty.io/s/ccla)에 서명 필요.

#### 체크리스트

커밋을 푸시하거나 풀 리퀘스트를 보내기 전에 다음 체크리스트 활용.

* 셸에서 `mvn test`를 실행했을 때 빌드가 실패 없이 성공하는지 확인
* 작업으로 인해 새로운 inspector 경고가 생기지 않는지 확인
* 커밋 메시지나 PR 설명이 [커밋 메시지 규칙](https://netty.io/wiki/writing-a-commit-message.html)에 부합하는지 확인
* PR을 대상 브랜치의 HEAD에 [rebase](http://git-scm.com/book/en/Git-Branching-Rebasing) 하고 모든 충돌을 해결했는지 확인
* [Contributor License Agreement](https://docs.google.com/spreadsheet/viewform?formkey=dHBjc1YzdWhsZERUQnhlSklsbG1KT1E6MQ)에 서명했는지 확인

#### 푸시 권한이 있는 기여자에게

* PR에는 여러 커밋이 포함되는 경우가 많음. 해당 커밋들은 설명 주석과 함께 적은 수의 커밋으로 squash 필요
* 특별한 이유가 없는 한 웹 UI를 통해 PR을 머지하지 말 것. [GitHub help 페이지](https://help.github.com/articles/using-pull-requests#merging-a-pull-request)의 'Patch and Apply' 섹션 참고
* [공식 웹사이트를 갱신](https://github.com/netty/netty/wiki/Working-with-official-web-site)하거나 [새 버전을 릴리스](https://netty.io/wiki/releasing-new-version.html)할 때는 신중하게 진행

---

## 개발 환경 설정

> 원본: https://netty.io/wiki/setting-up-development-environment.html

---

> 튜토리얼을 찾는 경우 [문서 홈](https://netty.io/wiki/index.html) 방문. 질문이 있는 경우 [StackOverflow.com](https://stackoverflow.com/questions/tagged/netty)에 질문.
>
> 이 가이드는 'user guide'가 아님. Netty를 사용해 애플리케이션을 만드는 사용자가 아니라, Netty 자체를 개발하고 기여하려는 개발자를 위한 문서.

### 64비트 운영체제 사용

64비트 운영체제 사용 필요.

### 필요한 빌드 도구 설치

머신에는 다음이 설치되어 있어야 함.

- [AdoptOpenJDK](https://adoptopenjdk.net) 또는 [Azul Systems](https://www.azul.com/downloads/zulu-community/) 같은 벤더의 64비트 OpenJDK 8 이상
- [Apache Maven 3.1.1](http://maven.apache.org/) 이상
- [Git](http://git-scm.com/)

Linux를 사용하는 경우 다음 패키지들 설치 필요.

```sh
# yum install lsb-core autoconf automake libtool make tar \
              glibc-devel libaio-devel openssl-devel apr-devel \
              lksctp-tools

# apt-get install autoconf automake libtool make tar \
                  libaio-dev libssl-dev libapr1-dev \
                  lksctp-tools cmake ninja-build
```

`rustup` 등을 사용해 Rust 툴체인도 설치 필요.

macOS를 사용하는 경우:

```sh
brew install autoconf automake libtool cmake ninja rustup openssl
rustup default stable
```

git 설정도 필요.

Windows를 사용하는 경우, 체크아웃 시 LF가 자동으로 CRLF로 변환되도록 설정.

```sh
git config --global core.autocrlf true
```

macOS를 사용하는 경우, 커밋 시 CRLF가 자동으로 LF로 변환되도록 설정.

```sh
git config --global core.autocrlf input
```

추가 정보는 [Building The Native Transports](./09_native_transports.md)도 참고.

netty-tcnative(특히 BoringSSL 부분)를 빌드하려면 `cmake`, `ninja-build`, `golang` 패키지도 설치. Fedora/CentOS의 `ninja-build`와 `golang`은 [Fedora EPEL](https://fedoraproject.org/wiki/EPEL) 저장소에서 받을 수 있음.

### 빌드 검증

코드를 체크아웃한 뒤 `./mvnw install -DskipTests -T1C`를 실행해 빌드 환경이 정상 동작하는지 확인. 이 명령은 로컬 Maven 캐시도 함께 채워줌 → 이후 개별 모듈을 빌드할 때 도움.

### IntelliJ IDEA 설정

Netty 프로젝트 팀은 [IntelliJ IDEA](http://www.jetbrains.com/idea/)를 주 IDE로 사용. 코딩 스타일만 따르면 다른 개발 환경을 써도 무방함.

#### OS와 동일한 비트 수의 IDE 사용

64비트 운영체제를 사용하고 있다면 64비트 버전의 IntelliJ IDEA 사용. 예를 들어 64비트 Windows라도 시작 메뉴 단축키가 32비트 바이너리를 가리킬 수 있음 → 이 경우 설치 디렉터리에서 `idea64.exe`를 직접 실행 필요. 그렇지 않으면 IntelliJ가 `io.netty:netty-tcnative:windows-x86_32`를 찾을 수 없다는 오류 발생.

#### `Use --release …` 컴파일러 옵션 해제

Netty 4.1 이하 버전은 Java 6을 최소 버전으로 요구 → Java 6 바이트코드로 컴파일 필요. 다만 성능을 위해 코드베이스에는 상위 Java API 호출도 포함되어 있으며, 런타임 버전 검사로 적절히 보호됨. 따라서 Netty는 `--source`와 `--target` 플래그로 컴파일 필요. 그런데 IntelliJ는 기본적으로 `--release` 플래그를 사용하며, 이 플래그는 API 버전 검사도 함께 활성화함.

`--release` 플래그가 활성화된 상태에서는 Netty 프로젝트 빌드 시 컴파일 오류 발생 → [Compiler Settings 다이얼로그](https://www.jetbrains.com/help/idea/troubleshooting-common-maven-issues.html#check_compiler_settings)에서 해당 옵션 해제 필요.

#### 코드 스타일

[이 코드 스타일 설정](http://netty.io/files/IntelliJ%20IDEA%20Code%20Style.zip)을 다운로드하고 `Netty project.xml`을 `<IntelliJ config directory>/codestyles` 디렉터리에 압축 해제. 그런 다음 'Netty project'를 기본 코드 스타일로 선택.

#### Inspection 프로파일

[이 inspection 프로파일](http://netty.io/files/IntelliJ%20IDEA%20Inspection%20Profile.xml.zip)을 다운로드해 압축 해제한 뒤 IntelliJ IDEA에 임포트해 기본값으로 사용. inspection 프로파일을 임포트하는 방법은 [여기](http://www.jetbrains.com/idea/webhelp/customizing-profiles.html#d1372841e358)에서 확인 가능.

수정 사항이 inspection 경고를 유발하지 않도록 주의. 거짓 양성(false positive)으로 판단되는 경우에는 IDE의 안내에 따라 `@SuppressWarnings` 애노테이션이나 `noinspection` 라인 주석으로 경고 억제. inspection 기능 사용에 대한 자세한 내용은 [웹 도움말](http://www.jetbrains.com/idea/webhelp/inspecting-source-code.html) 참고.

#### Copyright 프로파일

Copyright 텍스트:

```plain
Copyright $today.year The Netty Project

The Netty Project licenses this file to you under the Apache License,
version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at:

  https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
```

주석에서 copyright를 감지할 키워드:

```
The Netty project licenses
```

기존 copyright에 다음이 포함되어 있다면 교체 허용:

```
The Netty project licenses
```

### M2E와 Java 7/8을 사용한 Eclipse 설정

1. 64비트 버전의 Eclipse를 사용하고 있는지 확인.
2. [os-maven-plugin을 다운로드](http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.5.0.Final/os-maven-plugin-1.5.0.Final.jar)해서 `<ECLIPSE_INSTALLATION_DIR>/plugins`(Eclipse 4.5) 또는 `<ECLIPSE_INSTALLATION_DIR>/dropins`(Eclipse 4.6) 디렉터리에 넣기. `pom.xml`에 지정된 확장을 m2e가 평가하지 못하는 문제를 우회하기 위한 조치 (이름과 달리 Maven 플러그인인 동시에 Eclipse 플러그인).
3. 'File → Import... → Existing Maven Projects' 메뉴를 통해 프로젝트 임포트.
4. Netty 프로젝트의 Maven `pom.xml` 설정은 Java SE 1.6을 강제하면서, 가능한 경우 Java 7/8(1.7/1.8) 기능을 암묵적으로 사용함. 이로 인해 Eclipse에서 컴파일 오류 발생 가능. 두 가지 우회 방법 존재.
   1. 'Window → Preferences → Installed JRE' 메뉴 확인:
      * 'Installed JRE'에서 Java 7/8 설치를 사용할 수 있는지 확인.
      * 'Installed JRE → Execution Environments → Java SE 1.6'에서 이 Java 7/8 설치를 Java 6으로 매핑.
   2. 또는 각 Netty 모듈마다 프로젝트 단위로 Java 7/8 JRE 선택 가능.
