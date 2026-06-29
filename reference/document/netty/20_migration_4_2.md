# Netty 4.2 마이그레이션 가이드

> 원본: https://netty.io/wiki/netty-4.2-migration-guide.html

---

# Netty 4.2 Migration Guide

Netty 4.2는 Netty 4.1과 대부분 후방 호환되지만, 알아둘 필요가 있는 몇 가지 호환성 변경과 알아두면 좋은 새 기능 몇 가지가 있습니다.

가장 큰 변경 중 하나는 최소 Java 버전 요구사항을 Java 6에서 Java 8로 올린 것입니다. Netty 팀 입장에서는 큰 개선이지만, Java 8 미만을 사용하는 유지보수 프로젝트가 거의 없으므로 대부분의 사용자에게는 영향이 없을 것입니다.

## 권장 업그레이드 절차

다음은 실전 경험을 바탕으로 정리한 Netty 4.2 업그레이드 권장 절차입니다. 다양한 시스템에 적용할 수 있도록 고수준·범용으로 기술하였으며, 이 가이드의 다른 절도 함께 읽어 자신의 시스템과 관련된 세부 사항을 확인하세요.

시작 전에 의존성 관리 모범 사례를 따르길 권장합니다. 특히 shading은 Maven/Gradle 같은 의존성 관리 도구로부터 의존성을 숨기므로 가능하면 피하세요. 또한 `netty-bom` 같은 BOM 파일을 사용해 의존성 버전을 일관되게 관리하면 런타임에 버전이 뒤섞이는 문제를 방지할 수 있습니다.

Netty 4.2 업그레이드는 시스템 규모와 복잡도에 따라 여러 스프린트와 배포 사이클이 걸릴 수 있으므로, 완료에 필요한 시간과 자원을 미리 계획하세요.

Netty 4.2로 업그레이드하는 권장 단계는 다음과 같습니다.

1. **최신 Netty 4.1.x로 업그레이드.** \
Netty 4.2 업그레이드를 시작하기 전에, 시스템이 최신 4.1.x에서 정상 동작하는지 확인하세요.
2. **클라이언트 TLS 연결에 대해 endpoint validation을 명시적으로 설정.** \
Netty 4.2에서 endpoint validation(즉, hostname 검증)의 기본값이 변경됩니다. TLS 클라이언트가 있다면 endpoint validation을 명시적으로 _on_(권장) 또는 _off_(틈새 사용 사례)으로 설정하세요. endpoint validation은 _반드시_ Netty 4.1.112에서 추가된 [SslContextBuilder.endpointIdentificationAlgorithm(String)](https://netty.io/4.1/api/io/netty/handler/ssl/SslContextBuilder.html#endpointIdentificationAlgorithm-java.lang.String-) API로 설정해야 합니다.
3. **의존성에 대해서도 1, 2단계를 반복.** \
시스템의 모든 컴포넌트가 같은 Netty 버전을 사용하고 endpoint validation을 올바르게 설정하도록 하는 것이 중요합니다. 우선, endpoint validation을 잘못 설정한 오래된 클라이언트 라이브러리가 Netty 4.2 업그레이드 후 동작을 멈추는 깜짝 상황을 피할 수 있습니다. 둘째, Netty 4.1과 4.2는 클래스패스에 공존할 수 없으므로 한 번에 업그레이드해야 합니다.
4. **시스템이 pooled 할당자를 사용하도록 명시적으로 설정.** \
Netty 4.2는 기본 할당자를 `adaptive`로 변경합니다. 이 변경의 위험을 줄이려면 먼저 4.2로 업그레이드하면서 명시적으로 pooled 할당자를 설정하고, 두 번째 단계로 그 설정을 제거하는 것을 권장합니다. pooled 할당자를 명시 설정하려면 시스템 프로퍼티 `io.netty.allocator.type=pooled`를 설정하세요.
5. **위 변경을 적용한 시스템을 배포해 정상 동작 확인.** \
1~4단계의 변경은 한 번에 배포할 수 있습니다. 다만 라이브러리·프레임워크가 자체 업그레이드·릴리스 사이클을 가질 수 있다는 점은 유의하세요. 이 시점에서 시스템은 여전히 Netty 4.1을 실행 중이지만 이제 4.2 업그레이드 준비가 끝났습니다.
6. **최신 Netty 4.2.x로 업그레이드하고 배포.** \
이 단계에서는 단순히 Netty 버전을 마지막 4.2.x로 올리고 롤아웃합니다. 빌드를 점검해 Netty 4.1 의존성이 남아 있지 않은지 확인하세요. 빌드 파이프라인과 배포가 롤아웃되면서 TLS 클라이언트의 endpoint validation 관련 오류를 주시하세요. 대부분 시스템에서 가장 큰 위험 요인입니다. 모두 잘 보이면, `adaptive` 할당자에 관심이 없는 경우 여기서 멈춰도 됩니다. 그렇지 않다면 다음 단계로 진행합니다.
7. **adaptive 할당자로 전환.** \
`adaptive` 할당자가 새 기본값이지만 모두에게 최선은 아닙니다. 어떤 워크로드에서는 더 좋고 어떤 워크로드에서는 더 나쁩니다. 부하 테스트나 soak 테스트 환경이 있다면 `adaptive` 할당자를 거기서 먼저 시도하세요. 단순히 4단계에서 추가한 `io.netty.allocator.type=pooled` 시스템 프로퍼티를 제거하면 됩니다. 좋아 보이면 환경을 거쳐 운영까지 변경을 배포하면서, 변경이 롤아웃되는 동안 heap·direct 메모리 사용량과 GC 활동을 주시하세요. `adaptive` 할당자에서 메모리 사용량이 크게 증가한다면, [Netty Allocator Events.jfc](https://github.com/netty/netty/blob/4.2/microbench/src/main/resources/Netty%20Allocator%20Events.jfc) 기록 프로파일을 사용해 flight recording을 얻고 그 기록을 첨부해 이슈를 등록해 주세요. `adaptive` 할당자가 자기 워크로드에서 성능이 나쁘다면, 우리가 `adaptive`를 더 좋게 만드는 동안 `pooled` 할당자에 머무르면 됩니다.

위 단계를 따르면 큰 깜짝 상황 없이 Netty 4.2로 성공적으로 업그레이드할 가능성이 높아집니다.

## 호환성 주요 사항

가장 중요한 호환성 변경입니다. Netty 팀은 여러 대규모·고프로파일 프로젝트에서 4.2를 광범위하게 검증 테스트했고, 통합자가 Netty 의존성을 4.1에서 4.2로 업그레이드할 때 가장 자주 마주치는 사항은 다음과 같습니다.

**⚠️ 클라이언트 TLS 연결에 대해 hostname verification이 기본 활성화됩니다.**

`SslContextBuilder.endpointIdentificationAlgorithm` 설정의 기본값이 4.1에서는 `null`이었지만 이제 `HTTPS`로 설정됩니다. hostname verification을 하지 않는 것은 더 순진했던 시절의 구식이고 안전하지 않은 관행이지만, 갑자기 활성화하면 많은 시스템이 깨질 수 있어 그동안 변경할 수 없었습니다.

이 기본값은 시스템 프로퍼티를 통해 Netty 4.1 동작으로 되돌릴 수 있습니다.

```
io.netty.handler.ssl.defaultEndpointVerificationAlgorithm=NONE
```

이 override는 Netty 4.2 마이그레이션 과도기를 위한 임시 수단입니다. endpoint identification을 의도적으로 비활성화해야 하는 경우에는 `SslContextBuilder` 옵션으로 명시적으로 설정해야 합니다.

**⚠️ `adaptive` 메모리 할당자가 새 기본값입니다.**

Netty 4.1에서 `adaptive` 메모리 할당자는 실험적이었습니다. 이제 `pooled` 대신 새 기본값으로 만들었습니다.

대부분의 워크로드에서 메모리 사용량이 줄어들고 성능은 pooled 할당자와 비슷하거나 약간 더 좋을 것으로 예상됩니다. adaptive 할당자는 실제 워크로드에 맞게 자동으로 튜닝되며, 처음부터 가상 스레드와 잘 동작하도록 설계되었습니다.

다만 애플리케이션마다 할당자를 사용하는 방식과 부하 패턴이 다르므로, 경우에 따라 adaptive 할당자가 더 나쁜 성능을 보일 수도 있습니다. Netty 4.1과 동일하게 pooled 할당자를 계속 사용하려면 다음 시스템 프로퍼티로 기본 할당자를 변경할 수 있습니다.

```
io.netty.allocator.type=pooled
```

**⚠️ BouncyCastle 업그레이드.**

(여전히 선택적인) BouncyCastle 의존성을 갱신했습니다. BouncyCastle이 자기 아티팩트의 버전을 매기는 방식 때문에 이는 호환성 변경입니다.

BouncyCastle 의존성이 모두 `*-jdk15on` 변형에서 `*-jdk18on` 변형으로 변경되었습니다.

예를 들어 Netty와 `bcprov-jdk15on`을 함께 의존하고 있다면, Netty 4.2 마이그레이션의 일환으로 의존하는 BouncyCastle 아티팩트를 변경해야 합니다.

또한 의존 버전을 1.69에서 1.80으로 올렸으며, 이 자체가 API에 호환성 변경을 도입합니다.

**⚠️ `netty-incubator-transport-io_uring` 모듈은 더 이상 지원되지 않습니다.**

Netty 4.2에서 io_uring 전송을 incubator에서 *졸업*시켜 완전히 지원되는 first-class 전송 모듈로 만들었습니다.

이 작업의 일환으로 Netty 전송 내부에 다수의 리팩토링을 했고, 그 결과 incubator 버전과는 완전히 호환되지 않습니다. 좋은 소식은 이 리팩토링 덕분에 `netty-transport-native-io_uring`이 훨씬 더 우수한 구현이라는 점입니다.

통합자는 incubator 모듈 사용을 멈추고, 대신 우수한 io_uring 통합을 위해 Netty 4.2로 옮길 것을 권장합니다. 그렇게 하려면 안타깝게도 코드 변경이 필요합니다. 다음 사항을 주의하세요.

* 패키지명이 `io.netty.incubator.channel.uring`에서 `io.netty.channel.uring`으로 변경됩니다.
* 클래스명이 `IOUring`에서 `IoUring`으로 변경됩니다(소문자 ‘o’에 주의).
* 다수의 io_uring 채널 옵션이 변경되었습니다.
* `IOUringEventLoopGroup`은 더 이상 존재하지 않으며 `MultiThreadIoEventLoopGroup`과 `IoUringIoHandler`로 대체됩니다.
* io_uring 사용은 이제 Java 9 이상을 요구합니다.

## 그 외 호환성 변경

Netty 4.2에는 실제 문제로 이어질 가능성은 낮지만 알아두어야 할 호환성 변경이 다수 포함되어 있습니다.

* `protobuf-java` 의존성이 2.6.1에서 3.25.5로 메이저 버전 업그레이드되었습니다.
* `netty-codec` 모듈이 여러 서브 모듈로 분할되었으며, `netty-codec` 모듈은 그것들에 의존합니다. 즉, `netty-codec`은 이제 단일 jar이 아니라 여러 jar 파일입니다.
* 사용되지 않는 WebSocket draft 명세 지원을 제거했습니다.
* Java 8 사용 시 ALPN이 기본 지원되므로 Jetty alpn/npn 지원을 제거했습니다.
* 파이프라인 콜스택이 평탄화되어, 파이프라인을 통한 메시지 처리 시 스택 깊이가 줄어듭니다. 파이프라인이 동시에 수정되거나 child-executor를 사용하는 경우 메시지 처리 동작이 달라질 수 있습니다.
* 최소 GLibC 요구 버전이 2.12(2010-05, 예: CentOS 6)에서 2.17(2012-12, 예: CentOS 7)로 올라갔습니다.
* tcnative 동적 링크 OpenSSL 통합 테스트를 OpenSSL 1.0.1e(2013) 대신 OpenSSL 1.0.2k(2017)로 진행하고 있습니다.
* JPMS에서 automatic module 대신 real module로 전환했습니다.

API 변경의 완전하고 망라된 목록은 RevAPI가 정리했습니다:
https://github.com/netty/netty/blob/3ca17a96cf84cbcb08776d3731b222b82814ead7/pom.xml#L1271-L7794

## 새 모범 사례

Netty 4.2는 일부 새 API를 도입하고 일부 기존 API를 deprecated 처리합니다. Netty 4.2 마이그레이션의 일환으로, deprecated API 사용을 정리할 기회를 코드베이스에서 살펴보길 권장합니다.

**✅ EventLoopGroups를 위한 IoHandlerFactories**

`NioEventLoopGroup`을 비롯한 모든 전송별 이벤트 루프 그룹이 deprecated 처리되었습니다.

이제 통합자는 전송별 `IoHandlerFactory`를 `MultiThreadIoEventLoopGroup` 생성자에 전달해야 합니다.

예를 들어, 다음과 같이 하지 말고

```
EventLoopGroup group = new NioEventLoopGroup(); // ❌
```

대신 다음과 같이 하세요.

```
EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()); // ✅
```

각 전송에 대해 정적 `newFactory` 메서드가 있습니다.

* `NioIoHandler.newFactory()`
* `EpollIoHandler.newFactory()`
* `KQueueIoHandler.newFactory()`
* `IoUringIoHandler.newFactory()`
* `LocalIoHandler.newFactory()`

`Bootstrap`/`ServerBootstrap` 인스턴스에서 관련 채널이나 채널 팩토리는 여전히 설정해야 한다는 점에 유의하세요.

**✅ `MultiThreadIoEventLoopGroup`과 `SingleThreadIoEventLoop`의 확장성**

`MultiThreadIoEventLoopGroup`과 `SingleThreadIoEventLoop` 클래스는 사용자가 오버라이드할 수 있는 다양한 메서드를 제공하며, 다음을 가능하게 합니다.

* 등록된 채널/핸들 수 확인
* IO 처리 시간 vs 태스크 처리 시간 이해
* 루프 실행당 처리된 채널/핸들 수 확인
* promise 커스터마이징/데코레이션

또한 올바른 `IoHandle` 타입을 구현한다면 채널 외의 것도 등록할 수 있어, Netty 컴포넌트를 재사용하거나 현재 존재하지 않는 `IoHandle` 구현을 추가할 가능성이 열렸습니다. 예를 들어 io_uring을 이용한 파일 IO 구현에 활용할 수 있습니다.

**✅ SelfSignedCertificate 대신 netty-pkitesting 사용**

Netty 4.1에는 TLS 구현 테스트에 사용하는 `SelfSignedCertificate` 클래스가 포함되어 있습니다. 이 클래스는 광범위한 사용을 전제로 설계된 것이 아니었으며, 범용 PKI·TLS 테스트 도구로 쓰기에는 적합하지 않은 API 결정들이 있습니다.

Netty 4.2에서는 새 모듈 `netty-pkitesting`을 도입하며, 여기에는 `CertificateBuilder` 클래스가 포함됩니다. `CertificateBuilder`는 통합자가 다양한 PKI·TLS 시나리오를 테스트할 수 있도록 설계되었습니다.

이전처럼 자체 서명 인증서를 만들 수도 있고, 적절한 CA·issuer·leaf 인증서 체인을 만들 수도 있습니다. `CertificateBuilder`는 인증서 체인과 그에 대응하는 개인키를 모두 포함하는 `X509Bundle` 객체를 만들며, 번들은 파일을 쓰지 않고 완전히 인메모리로 생성됩니다. 인증서·키·키 저장소를 위한 파일이 필요한 경우, 번들 객체에는 이를 위한 임시 파일을 만드는 편리한 메서드가 다수 포함되어 있습니다.

`netty-pkitesting` 모듈에는 인증서 폐기 시나리오를 테스트할 수 있는 단순한 Certificate Revocation List 서버도 포함되어 있습니다.
