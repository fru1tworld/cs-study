# Netty 5

## Netty 5.0의 새로운 기능과 주요 변경사항

> 원본: https://netty.io/wiki/new-and-noteworthy-in-5.0.html

---

4.1 이후 메이저 Netty 릴리스의 주목할 만한 변경사항과 새 기능 정리.

- 3.x와 4.0 사이의 변경과 달리 5.0은 설계 단순성 면에서 상당한 도약 → 변경 규모 자체는 크지 않음
- 4.x → 5.0 전환을 최대한 매끄럽게 만들도록 노력 → 마이그레이션 중 문제 발생 시 커뮤니티에 알릴 것

### 코어 변경

#### 새 Buffer API가 ByteBuf를 대체

- Netty 5는 ByteBuf보다 단순하고 안전한 새 Buffer API 도입
- 자세한 내용: [PR #11347](https://github.com/netty/netty/pull/11347)
- 새 API의 주요 차이점
  - Aliasing이 더 이상 허용되지 않음 → 같은 메모리를 가리키는 여러 버퍼를 가질 수 없음
    - `slice`, `duplicate` 및 그 retain 변형 제거
    - 대신 `split`, `readSplit`, `send` 같은 새 API 도입 → 이 메서드들의 계약이 aliasing을 방지
  - 참조 카운팅이 사실상 사라짐
    - `retain`과 `release` 메서드 제거 → 대신 버퍼는 라이프사이클 종료 시점에 호출하는 `close` 메서드를 가짐
    - API와 통합 코드는 버퍼가 항상 단일하고 명확한 소유권을 갖도록 설계 필요 → 런타임에 참조 카운팅으로 차용/참조를 추적할 수 없으므로, API에서 버퍼 "차용(borrowing)"은 최소화·지양
    - `retain`을 사용하던 대부분의 위치 → 사실 슈퍼클래스의 무조건적 `release` 효과를 상쇄하기 위한 것 → 이런 경우 대개 버퍼의 readable 부분만 전달하고 싶은 것이므로 `split` 사용 가능
  - `send` 메서드와 `Send` 인터페이스로 타입 시스템에 소유권 이전 표현 가능
  - 버퍼는 항상 big-endian이며 `*LE` 메서드들이 사라짐
    - little-endian 읽기/쓰기가 필요하면 `Integer`, `Long` 등의 `reverseBytes` 메서드와 버퍼의 big-endian 읽기/쓰기 메서드를 함께 사용
    - `BufferUtil`에는 "medium"(3바이트) 정수의 바이트 순서를 반전하는 메서드 존재
  - 버퍼 구현의 테스트 커버리지가 더 높아짐 · 동작도 더 일관적

#### ChannelHandler

##### 단순화된 핸들러 타입 계층

- `ChannelInboundHandler`와 `ChannelOutboundHandler`가 `ChannelHandler`로 통합 → `ChannelHandler`는 이제 inbound·outbound 핸들러 메서드를 모두 가짐
- `ChannelInboundHandlerAdapter`, `ChannelOutboundHandlerAdapter`, `ChannelDuplexHandlerAdapter` 제거 → `ChannelHandlerAdapter`로 대체
- 핸들러가 inbound인지 outbound인지 더 이상 구분할 수 없으므로 `CombinedChannelDuplexHandler`는 `ChannelHandlerAppender`로 대체
- 자세한 내용: [PR #1999](https://github.com/netty/netty/pull/1999)

##### `SimpleChannelInboundHandler.channelRead0()` → `messageReceived()`

- `SimpleChannelInboundHandler` 사용 시 `channelRead0()`를 `messageReceived()`로 이름 변경 필요

##### `ChannelHandler` 메서드 시그니처 변경

- `flush`와 `read`를 제외한 모든 `ChannelHandler` outbound 메서드는 이제 `Future<Void>`를 반환 → 적절한 전파와 체이닝 보장
- inbound 예외를 처리한다는 점을 명확히 하기 위해 `exceptionCaught(...)`를 `channelExceptionCaught(...)`로 이름 변경

##### 사용자 이벤트

- 파이프라인을 통해 양방향으로 사용자/커스텀 이벤트 발행 가능
  - inbound 이벤트: `fireChannelInboundEvent(...)`(`fireUserEventTriggered(...)` 대체)
  - outbound 이벤트: `sendOutboundEvent(...)`
- 두 경우 모두 `ChannelHandler`에 정의된 메서드로 가로챌 수 있음

##### `ChannelHandler.pendingOutboundBytes(...)` 메서드 추가

- `ChannelPipeline` 안의 `ChannelHandler`가 `Channel`의 writability에 쉽게 영향을 줄 수 있게 됨 → `ChannelHandler`가 outbound 데이터를 직접 버퍼링할 때 back pressure에도 영향을 줄 수 있음

#### 전송 (Transport)

##### Half-closure

- Netty 5는 코어에 half-closure 지원을 내장
  - `ChannelHandler.shutdown`과 `ChannelHandler.channelShutdown` 도입
  - `Channel.isShutdown(...)`과 `ChannelOutboundInvoker.shutdown(...)` 추가
- 이로써 기존 `DuplexChannel` 추상화는 완전히 제거
- 자세한 내용: [PR #12468](https://github.com/netty/netty/pull/12468)

##### `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않음

- `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않음 → attribute를 사용하려면 여전히 `AttributeMap`을 상속하는 `Channel`을 직접 사용 필요

##### `ChannelPipeline.add*(EventExecutorGroup...)` 제거

- netty 4.x에서는 `EventExecutorGroup`을 명시적으로 지정해 `ChannelPipeline`에 `ChannelHandler`를 추가 가능 → 라이프사이클 관점에서 여러 문제 존재
  - `handlerRemoved(...)`, `handlerAdded(...)`가 "잘못된 시점"에 호출될 수 있음 → 최악의 경우 `handlerRemoved(...)`가 호출되어 핸들러가 더 이상 사용되지 않을 것이라 가정하고 native 메모리를 해제했는데, 이후 `channelRead(...)`가 호출되어 이미 해제된 메모리에 접근하다 JVM 크래시 가능
  - 파이프라인의 동시 접근/수정에서 "가시성"을 올바르게 구현하는 것도 상당히 까다로움
- 검토 결과 → 사용자가 실제로 원하는 것은 비즈니스 로직 처리를 위해 수신 메시지를 다른 스레드에서 처리하는 것 → 이는 사용자가 직접 만든 커스텀 구현으로 처리하는 편이 낫음(무엇이 언제 해제될 수 있는지는 사용자가 더 잘 알기 때문)

##### `Channel.eventLoop()`이 `Channel.executor()`로 이름 변경

- netty 5.x에서 `ChannelOutboundInvoker`에 `executor()` 메서드 추가 → `EventExecutor`를 반환
- `Channel`에서는 `eventLoop()`을 제거하고 `executor()`를 오버라이드해 `EventLoop`을 반환하도록 변경

##### `EventLoopGroup.isCompatible(...)` 메서드 추가

- 사용 전에 `Channel` 서브타입이 `EventLoopGroup`/`EventLoop`과 호환되는지 확인 가능 → 올바른 `Channel` 서브타입 선택에 도움

##### `EventLoop.registerForIo(...)`와 `EventLoop.deregisterForIo` 추가

- `EventLoop` 인터페이스에 `Channel` 등록·해제용 메서드 추가
- 사용자가 직접 호출하면 안 되며, `Channel` 구현체 내부에서만 사용

##### `Channel.Unsafe` 제거

- `Channel.Unsafe` 인터페이스가 완전히 제거 → 사용자가 내부 구현을 직접 건드릴 수 없게 됨

##### `ChannelOutboundBuffer`가 더 이상 `Channel` API의 일부가 아님

- `ChannelOutboundBuffer`는 `AbstractChannel` 구현의 내부 구현 세부사항 → `Channel` 공개 API에서 완전히 제거

##### `Channel.beforeBeforeWritable()` 제거 및 `Channel.bytesBeforeUnwritable()`이 `writableBytes()`로 이름 변경

- 전혀 사용되지 않던 `Channel.beforeBeforeWritable()` 메서드 제거
- `Channel.bytesBeforeUnwritable()`을 `Channel.writableBytes()`로 이름 변경

#### Future / Promise

##### `ProgressiveFuture`/`ProgressivePromise`와 `ChannelProgressiveFuture`/`ChannelProgressivePromise` 제거

- netty 5에서 Progressive*Future / Progressive*Promise 지원 완전 제거
- 가끔 유용할 수 있었지만 → promise를 체이닝할 때 파이프라인의 모든 핸들러가 특별한 처리 필요 → 실제로는 그렇게 하지 못하는 경우가 많아 현실적으로 매우 불편
- 사용량 자체도 많지 않았음 → "가끔만" 동작하는 기능을 유지하는 것보다 완전히 제거하는 것이 낫다는 판단 → 유지보수해야 할 코드도 줄어드는 이점

##### `VoidChannelPromise` 제거

- netty 4.1.x에서는 `voidPromise()` 메서드로 특수한 `ChannelPromise` 구현을 얻어 `write` 등 다양한 IO 연산에 사용 → 객체 생성 감소가 취지였으나 실제로는 많은 문제 야기
  - 이 promise에 `ChannelFutureListener`를 추가하는 모든 `ChannelHandler`는 리스너 추가 전에 먼저 `unvoid()` 호출 필요 → 그렇지 않으면 `addListener` 호출 시 `RuntimeException` 발생
  - `wait()` / `sync()` 연산이 전혀 지원되지 않음
  - 일부 연산은 `VoidChannelPromise` 사용을 허용하고 다른 연산은 허용하지 않는 불일관성 존재

##### `Promise`와 `Future`의 API 변경

- `Future.addListeners()`, `Future.removeListeners()`, `Future.removeListener()` 제거 → 이전에 추가한 리스너를 제거하는 기능이었지만 거의 사용되지 않음 → 제거로 복잡도와 API 표면 감소
- `sync`와 `await`의 uninterruptible 변형도 제거
- `Future.isFailed()` 메서드 추가 → future가 완료되었고 실패했는지 확인, 기존 `Future.isSuccess()`(완료+성공 확인)와 대칭
- `Promise.setUncancellable()`은 이제 promise가 "incomplete"에서 "uncancellable" 상태로 전이될 때만 `true` 반환 → 이미 완료된 promise에는 `false` 반환(4.1에서는 그런 경우에도 `true` 반환)
- `Future.map()`과 `Future.flatMap()` 메서드 추가 → 기존 future를 기반으로 새 future를 손쉽게 합성 가능, 실패와 취소를 적절히 전파
- `Future` 인터페이스에서 모든 블로킹 메서드 제거 → `EventLoop`을 잘못 블록하는 실수 방지 목적 → `EventLoop` 외부에서 블록이 필요하면 `Future.asStage()`로 변환한 뒤 반환된 `FutureCompletionStage`의 블로킹 메서드 사용

### 코덱 변경

#### 압축 지원

- 별도의 `EmbeddedChannel` 없이도 다른 코덱에서 쉽게 재사용할 수 있도록, 모든 압축 구현이 새 [Compression API](https://github.com/netty/netty/pull/11685)를 사용하도록 변경

#### HTTP 코덱

- netty-core에서 multi-part 지원 제거 → 향후 contributor 저장소로 netty 5에 마이그레이션 예정 (https://github.com/netty/netty/pull/11830)
- 오래된 websocket draft 지원 제거 (https://github.com/netty/netty/pull/11831)
- HTTP/2 헤더 검증이 기본 활성화 → 잘못된 형식의 요청이 기본적으로 거부됨

#### 거의 쓰이지 않는 코덱이 Netty Contrib로 이동

- 코드베이스 경량화 및 유지보수 부담 감소 목적 → 다음 코덱과 핸들러가 [Netty Contrib](https://github.com/netty-contrib)으로 이동
  - netty-codec-xml
  - netty-codec-redis
  - netty-codec-memcache
  - netty-codec-stomp
  - netty-codec-haproxy
  - netty-codec-mqtt
  - netty-codec-socks
  - netty-handler-proxy
  - `io.netty.handler.codec.json`
  - `io.netty.handler.codec.marshalling`
  - `io.netty.handler.codec.protobuf`
  - `io.netty.handler.codec.serialization`
  - `io.netty.handler.codec.xml`
  - `io.netty.handler.pcap`

### GraalVM과 Native Image

- Netty는 native image에서 최소한의 크기 오버헤드로 런타임에 자동 초기화됨
- 최소 지원 Graal 버전: 22.1 · Java 17

---

## Netty 5 마이그레이션 가이드

> 원본: https://netty.io/wiki/netty-5-migration-guide.html

---

### 패키지

- netty 5와 netty 4가 같은 클래스패스에 공존할 수 있도록, netty 5 클래스의 패키지명을 `io.netty5.*`로 변경

### Buffer

- Netty 5는 `ByteBuf`보다 더 단순하고 안전한 새 Buffer API 도입

#### 용량 (Capacity)

- Netty 4.1의 `ByteBuf`는 쓰기가 일어날 때 특정 최대 용량까지 자동으로 확장
- 새 `Buffer` API는 더 이상 그렇게 동작하지 않음 → 용량과 최대 용량의 구분도 없음
  - 코드는 적절한 크기의 버퍼를 할당(크기 인자가 이제 필수)하거나, 필요할 때 `ensureWritable()` 호출 필요
  - `ensureWritable()` 메서드는 단일 메모리 복사로 compaction(과거 `discardReadBytes()`와 동일)과 확장을 동시에 수행할 수 있는 인자를 받을 수 있게 됨

#### 어댑터

- 두 API 사이를 변환할 수 있는 어댑터 세트 포함 → 모든 핸들러와 관련 코드가 마이그레이션될 때까지 두 API가 공존 가능
- `ByteBufBuffer.wrap()` 메서드는 `ByteBuf` 인스턴스를 받아 새 API의 `Buffer` 인스턴스를 반환
- `ByteBufAdaptor.intoByteBuf()` 메서드는 `Buffer`를 받아 `ByteBuf`를 반환
  - 두 메서드는 가능한 한 효율적인 방법으로 변환 수행 → 반복 변환 시 어댑터가 중첩되지 않도록 서로 상쇄
- 서로 다른 API에 의존하는 핸들러 사이에 삽입할 수 있는 `BufferConversionHandler`도 포함
  - `BufferConversionHandler`는 `ByteBufHolder`나 `BufferHolder` 객체는 변환하지 못함 → `Buffer`나 `ByteBuf` 인스턴스를 포함하는 객체는 커스텀 `MessageToMessageCodec` 등을 사용해 변환 필요

#### API 변경

- 새 `Buffer` API의 핵심 차이 → aliasing이 더 이상 허용되지 않음(두 개 이상의 버퍼 객체가 같은 기반 메모리를 가리키는 상황)
  - `slice()`, `duplicate()` 같은 메서드는 더 이상 제공되지 않음 → 대신 라이프사이클 처리가 단순해지고, API에서 참조 카운트를 완전히 제거 가능
- `slice()`, `duplicate()`와 그 `retain*` 변형, 그리고 `retain()` 패밀리를 사용하던 곳 → 이제 `split()`, `readSplit()`, `copy()`를 대신 사용
- `split` 패밀리는 `slice()`와 비슷하지만 버퍼의 끝에서만 자를 수 있고, 반환된 버퍼 슬라이스는 원본 버퍼에서 제거됨 → 그 결과 aliasing 방지
- `retain()`과 `release()` 메서드는 사라짐 → 대신 버퍼는 라이프사이클 끝에 호출될 `close()` 메서드를 가짐
  - 버퍼가 완전히 로컬 스코프와 라이프사이클을 가질 때를 위해 버퍼는 `AutoCloseable`도 구현
- `retain()`을 사용하던 대부분의 위치 → 슈퍼/서브 클래스의 무조건적 `release()` 효과를 상쇄하기 위한 것 → 이런 경우 `split()`으로 대체 가능
  - 일반적으로 버퍼의 readable 부분을 전달하는 패턴 → `split()`은 두 개의 버퍼를 생성하고 각각이 별도로 닫혀야 함
- 새 `send()` 메서드는 버퍼의 "소유권"이 한 곳에서 다른 곳으로 이동함을 타입 시스템에 인코딩하는 데 사용
  - 예: `CompositeBuffer` 팩토리 메서드는 합성 버퍼가 컴포넌트 버퍼에 대한 배타적 접근을 얻도록 보장하기 위해 이를 사용 → 버퍼 합성을 통한 aliasing 방지
- 버퍼는 이제 항상 big-endian이며 `*LE` 접근자 메서드는 사라짐 → little-endian 읽기/쓰기를 수행하려면 `Integer`나 `Long`의 `reverseBytes` 메서드와 big-endian 읽기/쓰기를 함께 사용

### Future / Promise

#### ChannelFuture / ChannelPromise 제거

- API와 타입 계층 단순화를 위해 `ChannelFuture`/`ChannelPromise`(그리고 그 모든 서브타입/구현체)를 완전히 제거 → 대체로 `Future<Void>`와 `Promise<Void>`를 직접 사용

#### ProgressiveFuture / ProgressivePromise와 ChannelProgressiveFuture / ChannelProgressivePromise 제거

- netty 5에서 Progressive*Future / Progressive*Promise 지원 완전 제거
- 가끔 유용했을 수 있지만 → promise를 체이닝할 때 파이프라인의 모든 핸들러가 특별한 조치 필요 → 실제로는 그렇게 하지 못한 경우가 많고, 현실에서 매우 번거로움
- 사용량 자체가 많지 않았으므로 완전히 제거하는 것이 최선이라고 결정 → "가끔만" 동작하는 무언가를 갖는 것보다 아예 지원하지 않는 것이 나음 → 유지보수할 코드도 줄어듦

#### VoidChannelPromise 제거

- netty 4.1.x에서는 `voidPromise()` 메서드로 특수한 `ChannelPromise` 구현을 얻어 다양한 IO 연산(`write` 등)에 사용 → 객체 생성 수 감소가 취지였으나 실제로는 많은 문제 야기

#### Promise와 Future의 API 변경

- `Future.addListeners()`, `Future.removeListeners()`, `Future.removeListener()` 제거 → 이전에 추가한 리스너를 제거하는 기능이었으나 거의 사용되지 않아 복잡도와 API 표면 감소
- `Future.isFailed()` 메서드 추가 → future가 완료되었고 실패했는지 확인, 기존 `Future.isSuccess()`(완료+성공 확인)와 유사
- 새 `Future.map()`과 `Future.flatMap()` 메서드 추가 → 기존 future를 기반으로 새 future를 손쉽게 합성·생성 가능, 실패와 취소를 적절히 전파 처리
- `CompletionStage`로 변환하는 새 메서드들 추가 → 다른 API와의 상호운용이 쉬워짐
- `EventLoop`을 실수로 블록하기 쉬운 구조 → `Future` 인터페이스에서 모든 블로킹 메서드 제거 → `EventLoop` 외부에서 블로킹이 필요하면 `Future.asStage()`로 변환, 반환된 `FutureCompletionStage`가 블로킹 메서드 제공

### Channel

#### `Channel.eventLoop()`이 `Channel.executor()`로 이름 변경

- netty 5.x에서 `ChannelOutboundInvoker`에 `executor()` 메서드 추가 → `EventExecutor`를 반환
- `Channel`에서는 `eventLoop()`을 제거하고 `executor()`를 오버라이드해 `EventLoop`을 반환하도록 변경

#### 반환 타입이 `ChannelFuture`에서 `Future<Void>`로 변경

- `ChannelFuture`/`ChannelPromise` 제거에 따라 메서드들의 반환 타입도 `Future<Void>`로 변경

#### Half-closure

- Netty 5는 코어에 half-closure 지원이 내장됨
  - `ChannelHandler.shutdown`과 `ChannelHandler.channelShutdown` 도입
  - `Channel.isShutdown(...)`과 `ChannelOutboundInvoker.shutdown(...)` 추가
- 완전히 제거된 기존 `DuplexChannel` 추상화를 대체
- 자세한 내용: [PR #12468](https://github.com/netty/netty/pull/12468)

#### `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않음

- `ChannelHandlerContext`가 더 이상 `AttributeMap`을 상속하지 않도록 변경 → attribute를 사용한다면 여전히 `AttributeMap`을 상속하는 `Channel`을 직접 사용 필요

#### `Channel.Unsafe` 제거

- `Channel.Unsafe` 인터페이스가 완전히 제거 → 사용자가 내부를 망칠 수 없게 됨

#### `ChannelOutboundBuffer`가 더 이상 `Channel` API의 일부가 아님

- `ChannelOutboundBuffer`는 `AbstractChannel` 구현의 구현 디테일 → `Channel` 자체에서 완전히 제거

#### `Channel.beforeBeforeWritable()` 제거 및 `Channel.bytesBeforeUnwritable()`이 `writableBytes()`로 이름 변경

- 전혀 사용되지 않던 `Channel.beforeBeforeWritable()` 메서드 제거
- `Channel.bytesBeforeUnwritable()`을 `Channel.writableBytes()`로 이름 변경

### ChannelPipeline

#### `ChannelPipeline.add*(EventExecutorGroup...)` 제거

- netty 4.x에서는 `EventExecutorGroup`을 명시적으로 지정해 `ChannelPipeline`에 `ChannelHandler`를 추가 가능 → 라이프사이클 관점에서 여러 문제 존재
  - `handlerRemoved(...)`, `handlerAdded(...)`가 "잘못된 시점"에 호출될 수 있음 → 최악의 경우 `handlerRemoved(...)`가 호출되어 핸들러가 다시 사용되지 않을 것이라 가정하고 native 메모리를 해제했는데, 이후 `channelRead(...)`가 호출되어 이미 해제된 메모리에 접근하려다 JVM 크래시 가능
  - 파이프라인의 동시 접근/수정에서의 "가시성"을 올바르게 구현하는 것도 꽤 까다로움
- 검토 결과 → 사용자가 실제로 원하는 것은 비즈니스 로직 처리를 위해 수신 메시지를 다른 스레드에서 처리하는 것 → 커스텀 구현으로 처리하는 편이 낫음(무엇이 언제 소멸될 수 있는지는 사용자가 더 잘 알기 때문)

#### 반환 타입이 `ChannelFuture`에서 `Future<Void>`로 변경

- `ChannelFuture`/`ChannelPromise` 제거에 따라 메서드들의 반환 타입도 `Future<Void>`로 변경

### ChannelHandler

- Netty 5는 `ChannelHandler`의 타입 계층을 크게 단순화

#### 단순화된 핸들러 타입 계층

- `ChannelInboundHandler`와 `ChannelOutboundHandler`가 [`ChannelHandler`]로 통합 → [`ChannelHandler`]는 이제 inbound·outbound 핸들러 메서드를 모두 가짐
- `ChannelPromise`를 받던 모든 outbound 메서드는 `Future<Void>`를 반환하도록 변경 → 사용 시 오류 감소 및 API 단순화
- `ChannelInboundHandlerAdapter`, `ChannelOutboundHandlerAdapter`, `ChannelDuplexHandlerAdapter` 제거 → [`ChannelHandlerAdapter`]로 대체
- 핸들러가 inbound인지 outbound인지 구분할 수 없으므로 `CombinedChannelDuplexHandler`는 [`ChannelHandlerAppender`]로 대체
- 자세한 내용: [PR #1999](https://github.com/netty/netty/pull/1999)

#### `channelRead0()` → `messageReceived()`

- [`SimpleChannelInboundHandler`] 사용 시 `channelRead0()`을 `messageReceived()`로 이름 변경 필요

#### 사용자 이벤트

- 파이프라인을 통해 양방향으로 사용자/커스텀 이벤트 발사 가능
  - inbound 이벤트: `fireChannelInboundEvent(...)`(`fireUserEventTriggered(...)` 대체)
  - outbound 이벤트: `sendOutboundEvent(...)`
- 두 가지 모두 평소처럼 `ChannelHandler`에 정의된 메서드로 가로챌 수 있음

#### `ChannelHandler.pendingOutboundBytes(...)` 메서드 추가

- `ChannelPipeline` 안의 `ChannelHandler`가 `Channel`의 writability에 손쉽게 영향을 줄 수 있음 → `ChannelHandler` 자체가 outbound 데이터를 버퍼링할 때 back pressure에 영향을 줄 수 있게 됨

### EventLoopGroup / EventLoop

- netty 4.x에서는 전송별로 서로 다른 `EventLoopGroup`/`EventLoop` 구현 존재(예: `NioEventLoopGroup`, `EpollEventLoopGroup` 등)
- netty 5에서는 이를 단일 구현인 `MultiThreadEventLoopGroup`으로 통합 → 전송에 특화된 `IoHandlerFactory`를 인자로 받음(예: `NioHandler.newFactory()`, `EpollHandler.newFactory()` 등)
- 이 변경의 이점
  - `MultiThreadEventLoopGroup`을 상속해 데코레이터나 커스텀 메트릭을 추가하기 쉬움
  - 동일한 구현을 다른 전송에서도 재사용 가능
  - JDK의 `ThreadPoolExecutor`가 제공하는 커스터마이징 방식과 매우 유사

#### `EventLoopGroup.isCompatible(...)` 메서드 추가

- 사용 전에 `Channel` 서브타입이 `EventLoopGroup`/`EventLoop`과 호환되는지 미리 확인 가능 → 적절한 `Channel` 서브타입 선택에 도움

#### `EventLoop.registerForIo(...)`와 `EventLoop.deregisterForIo` 추가

- `EventLoop` 인터페이스에 `Channel` 등록·해제용 새 메서드 추가 → 사용자가 직접 호출하는 것이 아니라 `Channel` 구현체 내부에서만 사용

### 거의 쓰이지 않는 코덱이 Netty Contrib로 이동

- 코드베이스 경량화 및 유지보수 부담 감소 목적 → 다음 코덱과 핸들러가 [Netty Contrib](https://github.com/netty-contrib)으로 이동
  - netty-codec-xml
  - netty-codec-redis
  - netty-codec-memcache
  - netty-codec-stomp
  - netty-codec-haproxy
  - netty-codec-mqtt
  - netty-codec-socks
  - netty-handler-proxy
  - `io.netty.handler.codec.json`
  - `io.netty.handler.codec.marshalling`
  - `io.netty.handler.codec.protobuf`
  - `io.netty.handler.codec.serialization`
  - `io.netty.handler.codec.xml`
  - `io.netty.handler.pcap`

---

## Netty 5.x User Guide

> 원본: https://netty.io/wiki/user-guide-for-5.x.html
>
> [4.x User Guide](./2_user_guide_4x.md)와 거의 동일한 내용 → 5.x에 맞게 일부 클래스명만 다름(예: `ChannelInboundHandlerAdapter` → `ChannelHandlerAdapter`)

---

#### 서드파티 번역

- [Simplified Chinese](http://ifeve.com/netty5-user-guide/)

### 서문 (Preface)

#### 문제

- 서로 통신하기 위해 범용 애플리케이션이나 라이브러리 사용 → 예: 웹 서버에서 정보를 가져오거나 웹 서비스를 통해 원격 프로시저를 호출하기 위한 HTTP 클라이언트 라이브러리
- 범용 프로토콜이나 그 구현은 때때로 잘 확장되지 않음
  - 거대한 파일이나 이메일, 금융 정보·멀티플레이어 게임 데이터처럼 거의 실시간성을 요구하는 메시지를 교환할 때 범용 HTTP 서버를 쓰지 않는 것과 같은 이치
  - 필요한 것은 특정 목적에 맞춰 고도로 최적화된 프로토콜 구현 → 예: AJAX 기반 채팅 애플리케이션·미디어 스트리밍·대용량 파일 전송에 최적화된 HTTP 서버
  - 더 나아가 필요에 정확히 맞는 완전히 새로운 프로토콜을 설계·구현하고 싶은 경우도 있음
- 또 한 가지 피할 수 없는 경우 → 오래된 시스템과의 상호 운용성을 위해 레거시 독점 프로토콜을 다뤄야 할 때
  - 이 경우 중요한 것 → 결과 애플리케이션의 안정성과 성능을 희생하지 않으면서 얼마나 빠르게 그 프로토콜을 구현할 수 있느냐

### 해결책

- [Netty 프로젝트](http://netty.io/) — 유지보수성이 좋고 고성능·고확장성을 갖춘 프로토콜 서버와 클라이언트를 빠르게 개발할 수 있도록, 비동기 이벤트 기반 네트워크 애플리케이션 프레임워크와 도구를 제공하는 프로젝트
- 다시 말해 Netty는 프로토콜 서버와 클라이언트 같은 네트워크 애플리케이션을 빠르고 쉽게 개발할 수 있도록 해주는 NIO 클라이언트-서버 프레임워크 → TCP·UDP 소켓 서버 개발 같은 네트워크 프로그래밍을 크게 단순화하고 매끄럽게 만들어줌
- '빠르고 쉽다'는 것이 결과 애플리케이션이 유지보수성이나 성능 문제로 고생한다는 뜻은 아님
  - Netty는 FTP, SMTP, HTTP를 비롯한 다양한 바이너리/텍스트 기반 레거시 프로토콜의 구현 경험을 바탕으로 신중하게 설계됨 → 개발 편의성·성능·안정성·유연성을 어느 하나도 타협하지 않고 동시에 달성
- 같은 장점을 주장하는 다른 네트워크 애플리케이션 프레임워크와의 차이 → Netty가 기반으로 삼는 철학에 있음
  - Netty는 처음부터 API와 구현 양쪽 모두에서 가장 편안한 개발 경험을 제공하도록 설계 → 이 가이드를 읽고 Netty를 직접 다뤄보면 이 철학이 개발을 훨씬 수월하게 만들어준다는 것을 느끼게 됨

### 시작하기 (Getting Started)

- 이 장에서는 간단한 예제를 통해 Netty의 핵심 구성 요소를 둘러보며 빠르게 시작할 수 있도록 안내
- 이 장이 끝날 즈음이면 Netty 위에서 클라이언트와 서버를 바로 작성할 수 있게 됨
- 하향식(top-down) 학습을 선호한다면 Chapter 2, Architectural Overview부터 시작한 뒤 다시 여기로 돌아와도 됨

#### 시작 전 준비물

- 이 장의 예제를 실행하기 위한 최소 요구사항 두 가지
  - 최신 버전의 Netty — [프로젝트 다운로드 페이지](http://netty.io/downloads.html)에서 받을 수 있음
  - JDK 1.6 이상 — 사용하는 벤더의 웹사이트 참고
- 이 장에서 소개되는 클래스에 대해 궁금한 점이 있으면 API 레퍼런스 참고
  - 이 문서의 모든 클래스명은 편의를 위해 온라인 API 레퍼런스로 링크됨
  - 잘못된 정보·문법 오류·오타가 있거나 문서를 개선할 아이디어가 있다면 [Netty 프로젝트 커뮤니티에 문의](http://netty.io/community.html)

#### Discard 서버 작성하기

- 세상에서 가장 단순한 프로토콜은 'Hello, World!'가 아니라 [`DISCARD`](http://tools.ietf.org/html/rfc863) → 받은 데이터를 아무런 응답 없이 버리는 프로토콜
- `DISCARD` 프로토콜을 구현하기 위해 해야 할 일은 받은 데이터를 모두 무시하는 것뿐 → Netty가 발생시키는 I/O 이벤트를 처리하는 핸들러 구현부터 시작

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

1. `DiscardServerHandler`는 [`ChannelHandler`]의 구현체인 [`ChannelHandlerAdapter`]를 상속. [`ChannelHandler`]는 오버라이드할 수 있는 다양한 이벤트 핸들러 메서드를 제공 → 지금은 핸들러 인터페이스를 직접 구현하기보다는 [`ChannelHandlerAdapter`]를 상속하는 것으로 충분
2. 여기서는 `channelRead()` 이벤트 핸들러 메서드를 오버라이드. 클라이언트에서 새 데이터를 받을 때마다 받은 메시지와 함께 호출됨. 이 예제에서 받은 메시지의 타입은 [`ByteBuf`]
3. `DISCARD` 프로토콜을 구현하려면 핸들러는 받은 메시지를 무시해야 함. [`ByteBuf`]는 참조 카운트(reference-counted) 객체이며 `release()` 메서드로 명시적으로 해제 필요. 핸들러로 전달된 모든 참조 카운트 객체를 해제하는 것은 핸들러의 책임. 일반적으로 `channelRead()` 핸들러 메서드는 다음과 같이 구현.

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

4. `exceptionCaught()` 이벤트 핸들러 메서드는 I/O 오류로 인해 Netty가 예외를 일으키거나, 이벤트를 처리하던 핸들러 구현체에서 예외가 던져졌을 때 `Throwable`과 함께 호출됨. 대부분의 경우 잡힌 예외를 로깅하고 관련 채널을 닫아야 하지만, 예외 상황에 어떻게 대응할지에 따라 구현은 달라질 수 있음(예: 연결을 닫기 전에 에러 코드를 담은 응답 메시지를 보내는 경우).

- 지금까지 `DISCARD` 서버의 절반을 구현. 남은 일 → `DiscardServerHandler`로 서버를 시작하는 `main()` 메서드 작성

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

1. [`NioEventLoopGroup`]은 I/O 작업을 처리하는 멀티스레드 이벤트 루프. Netty는 다양한 종류의 전송(transport)을 위한 여러 [`EventLoopGroup`] 구현체를 제공. 이 예제에서는 서버 사이드 애플리케이션을 구현하므로 두 개의 [`NioEventLoopGroup`]을 사용.
   - 첫 번째는 흔히 'boss'라고 부르며 들어오는 연결을 받아들임
   - 두 번째는 흔히 'worker'라고 부르며, boss가 연결을 수락한 뒤 그 연결을 worker에 등록하면 worker가 그 연결의 트래픽을 처리
   - 사용되는 스레드 수와 그 스레드들이 생성된 [`Channel`]에 어떻게 매핑되는지는 [`EventLoopGroup`] 구현에 따라 다르며, 생성자를 통해 설정 가능
2. [`ServerBootstrap`]은 서버를 설정하는 헬퍼 클래스. [`Channel`]을 직접 사용해 서버를 설정할 수도 있지만 번거롭고 대부분의 경우 그럴 필요 없음.
3. 여기서는 들어오는 연결을 수락하기 위해 새로운 [`Channel`]을 인스턴스화할 때 사용할 [`NioServerSocketChannel`] 클래스를 지정.
4. 여기에 지정한 핸들러는 새로 수락된 [`Channel`]마다 항상 호출됨. [`ChannelInitializer`]는 사용자가 새 [`Channel`]을 설정할 수 있도록 도와주는 특수한 핸들러. 보통 새 [`Channel`]의 [`ChannelPipeline`]에 `DiscardServerHandler` 같은 핸들러를 추가해 네트워크 애플리케이션을 구현. 애플리케이션이 복잡해질수록 더 많은 핸들러를 파이프라인에 추가하게 되며, 이 익명 클래스는 결국 별도의 최상위 클래스로 추출됨.
5. `Channel` 구현체에 특화된 파라미터도 설정 가능. TCP/IP 서버 작성 중이므로 `tcpNoDelay`, `keepAlive` 같은 소켓 옵션을 설정 가능. 지원되는 `ChannelOption`의 개요는 [`ChannelOption`] apidoc과 구체적인 [`ChannelConfig`] 구현체 참고.
6. `option()`과 `childOption()`의 차이 → `option()`은 들어오는 연결을 수락하는 [`NioServerSocketChannel`]을 위한 것, `childOption()`은 부모 [`ServerChannel`](여기서는 [`NioServerSocketChannel`])이 수락한 `Channel`들을 위한 것.
7. 이제 준비가 끝났음. 남은 일은 포트에 바인드하고 서버를 시작하는 것. 여기서는 머신에 있는 모든 NIC(network interface card)의 `8080` 포트에 바인드. (서로 다른 바인드 주소로) `bind()` 메서드를 원하는 만큼 호출 가능.

- Netty 위에서 첫 번째 서버 완성.

#### 받은 데이터 들여다보기

- 첫 서버 작성 후 정말 동작하는지 확인 → 가장 쉬운 방법은 telnet 명령 사용(예: `telnet localhost 8080`을 입력하고 타이핑)
- 하지만 Discard 서버라서 어떤 응답도 받지 못하므로 정말 동작하는지 단언할 수 없음 → 받은 데이터를 출력하도록 서버 수정
- `channelRead()` 메서드는 데이터가 수신될 때마다 호출됨 → `DiscardServerHandler`의 `channelRead()` 메서드에 코드 추가

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

1. 이 비효율적인 루프는 `System.out.println(in.toString(io.netty.util.CharsetUtil.US_ASCII))`로 단순화 가능.
2. 또는 여기서 `in.release()`를 호출해도 됨.

- 다시 telnet 명령을 실행하면 서버가 받은 내용을 출력하는 것을 확인 가능
- Discard 서버의 전체 소스 코드는 배포본의 [`io.netty.example.discard`] 패키지에 있음

#### Echo 서버 작성하기

- 지금까지는 응답 없이 데이터를 소비하기만 함 → 서버는 보통 요청에 응답해야 함 → 받은 데이터를 그대로 돌려보내는 [`ECHO`](http://tools.ietf.org/html/rfc862) 프로토콜을 구현하며 클라이언트에 응답 메시지를 작성하는 방법 학습
- 이전 절 discard 서버와의 유일한 차이 → 받은 데이터를 콘솔에 출력하는 대신 다시 보냄 → 따라서 `channelRead()` 메서드만 수정하면 충분

```java
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg); // (1)
        ctx.flush(); // (2)
    }
```

1. [`ChannelHandlerContext`] 객체는 다양한 I/O 이벤트와 동작을 트리거할 수 있는 여러 연산을 제공. 여기서는 받은 메시지를 그대로 쓰기 위해 `write(Object)`를 호출. `DISCARD` 예제와 달리 받은 메시지를 해제하지 않은 점에 주목 → Netty가 메시지를 회선(wire)에 써낼 때 대신 해제해줌.
2. `ctx.write(Object)`는 메시지를 회선까지 내보내지 않음. 내부적으로 버퍼링되었다가 `ctx.flush()`에 의해 회선으로 flush됨. 간결하게 `ctx.writeAndFlush(msg)`를 호출해도 됨.

- 다시 telnet 명령을 실행하면 보낸 내용을 그대로 돌려받는 것을 확인 가능
- Echo 서버의 전체 소스 코드는 배포본의 [`io.netty.example.echo`] 패키지에 있음

#### Time 서버 작성하기

- 이 절에서 구현할 프로토콜 → [`TIME`](http://tools.ietf.org/html/rfc868) 프로토콜
  - 어떤 요청도 받지 않은 상태에서 32비트 정수를 담은 메시지를 보내고, 메시지를 보낸 후 연결을 닫는다는 점에서 이전 예제들과 다름
  - 이 예제에서는 메시지를 구성·전송하고 완료 시 연결을 닫는 방법 학습
- 받은 데이터는 모두 무시하고 연결이 수립되자마자 메시지를 보낼 것이므로 이번에는 `channelRead()` 메서드를 사용할 수 없음 → 대신 `channelActive()` 메서드를 오버라이드

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

- 4.x User Guide와의 차이는 핸들러 베이스 클래스가 `ChannelInboundHandlerAdapter`에서 `ChannelHandlerAdapter`로 바뀐 점뿐 → 자세한 설명은 [4.x User Guide](./2_user_guide_4x.md)의 같은 섹션 참고
- Time 서버가 의도대로 동작하는지 테스트하려면 UNIX `rdate` 명령 사용 가능

```
$ rdate -o <port> -p <host>
```

- 여기서 `<port>`는 `main()` 메서드에서 지정한 포트 번호, `<host>`는 보통 `localhost`

#### Time 클라이언트 작성하기

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

- 매우 단순해 보이고 서버 측 예제와 크게 다르지 않음 → 그러나 이 핸들러는 가끔 `IndexOutOfBoundsException`을 던짐. 그 이유는 다음 절에서 다룸.

#### 스트림 기반 전송 다루기

##### 소켓 버퍼의 작은 함정

- TCP/IP 같은 스트림 기반 전송에서는 받은 데이터가 소켓 수신 버퍼에 저장됨
- 안타깝게도 스트림 기반 전송의 버퍼는 패킷의 큐가 아니라 바이트의 큐 → 자세한 내용은 [4.x User Guide](./2_user_guide_4x.md) 참고

##### 첫 번째 해결책

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

##### 두 번째 해결책

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

- 추가로 Netty는 대부분의 프로토콜을 매우 쉽게 구현할 수 있게 해주는 즉시 사용 가능한 디코더들을 제공
  - 바이너리 프로토콜의 경우 [`io.netty.example.factorial`]
  - 텍스트 라인 기반 프로토콜의 경우 [`io.netty.example.telnet`]

#### `ByteBuf` 대신 POJO로 말하기

- `UnixTime` 같은 POJO를 도입하면 핸들러가 더 유지보수 가능하고 재사용 가능해짐

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

- 서버 측에서도 같은 기법을 적용

```java
@Override
public void channelActive(ChannelHandlerContext ctx) {
    ChannelFuture f = ctx.writeAndFlush(new UnixTime());
    f.addListener(ChannelFutureListener.CLOSE);
}
```

- 인코더 구현

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

- 더 단순화하려면 [`MessageToByteEncoder`] 사용 가능

```java
public class TimeEncoder extends MessageToByteEncoder<UnixTime> {
    @Override
    protected void encode(ChannelHandlerContext ctx, UnixTime msg, ByteBuf out) {
        out.writeInt((int) msg.value());
    }
}
```

- 마지막으로 남은 작업 → 서버 측 [`ChannelPipeline`]에서 `TimeServerHandler` 앞에 `TimeEncoder`를 끼워 넣는 것

#### 애플리케이션 종료하기

- Netty 애플리케이션을 종료할 때는 보통 생성한 모든 [`EventLoopGroup`]에 대해 `shutdownGracefully()`를 호출하는 것으로 충분
- 이 메서드는 [`Future`]를 반환 → [`EventLoopGroup`]이 완전히 종료되고 그 그룹에 속한 모든 [`Channel`]이 닫히면 알림을 받을 수 있음

#### 요약

- 이 장에서는 Netty를 빠르게 둘러보고 Netty 위에서 동작하는 네트워크 애플리케이션을 작성하는 방법을 시연
- 자세한 정보는 이후의 장과 [`io.netty.example`] 패키지의 예제 참고
- [커뮤니티](http://netty.io/community.html)는 질문과 아이디어를 언제나 기다리고 있으며, 피드백을 바탕으로 Netty와 그 문서를 계속 개선

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
