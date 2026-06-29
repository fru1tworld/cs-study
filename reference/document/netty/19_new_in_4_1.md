# Netty 4.1의 새로운 기능과 주요 변경사항

> 원본: https://netty.io/wiki/new-and-noteworthy-in-4.1.html

---

## TL;DR

4.0과의 후방 호환성을 최대한 유지했지만, 4.1에는 4.0과 완전히 호환되지 않을 수 있는 변경사항이 일부 포함되어 있습니다. 새 버전에 맞춰 애플리케이션을 반드시 다시 컴파일하세요.

재컴파일 시 deprecation 경고가 나타날 수 있습니다. 권장 대안으로 모두 수정해 두면 다음 버전 업그레이드 시 마이그레이션이 수월합니다.

## 코어 변경

### Android 지원

다음 이유로 Android(4.0 이상)를 공식 지원하기로 결정했습니다.

* 모바일 기기가 점점 강력해지고 있고,
* ADK의 NIO와 SSLEngine에서 알려진 주요 문제들이 Ice Cream Sandwich 이후 수정되었으며,
* 코덱과 핸들러를 모바일 애플리케이션에서도 재사용하고 싶어 하는 수요가 분명히 존재하기 때문입니다.

다만 Android용 자동화 테스트 스위트는 아직 없습니다. Android에서 문제를 발견하면 [이슈를 등록](https://github.com/netty/netty/issues)해 주세요. Android 테스트를 빌드 프로세스에 포함시키는 기여도 환영합니다.

### `ChannelHandlerContext.attr(..)` == `Channel.attr(..)`

[`Channel`]과 [`ChannelHandlerContext`]는 모두 [`AttributeMap`] 인터페이스를 구현해 사용자 정의 속성을 붙일 수 있습니다. 그런데 두 객체가 각각 별도의 속성 저장소를 가지고 있어 혼란이 발생했습니다. 예를 들어 `Channel.attr(KEY_X).set(valueX)`로 설정한 속성을 `ChannelHandlerContext.attr(KEY_X).get()`으로는 조회할 수 없었고, 그 반대도 마찬가지였습니다. 이 동작은 혼란스러울 뿐 아니라 메모리 낭비이기도 했습니다.

이 문제를 해결하기 위해 [`Channel`]당 내부적으로 단 하나의 맵만 유지하도록 변경했습니다. [`AttributeMap`]은 항상 [`AttributeKey`]를 키로 사용하며, [`AttributeKey`]가 유일성을 보장하므로 [`Channel`]당 속성 맵이 두 개 이상 필요하지 않습니다. [`AttributeKey`]를 [`ChannelHandler`]의 private static final 필드로 정의하면 키 충돌 위험이 없습니다.

### `Channel.hasAttr(...)`

이제 속성이 존재하는지 효율적으로 확인할 수 있습니다.

### 더 쉽고 정확한 버퍼 누수 추적

이전에는 버퍼 누수 발생 위치를 추적하기 어려웠고 누수 경고도 충분한 정보를 제공하지 못했습니다. 이제는 약간의 오버헤드를 감수하고 활성화할 수 있는 고급 누수 보고 메커니즘이 추가되었습니다.

자세한 내용은 [참조 카운트 객체](./08_reference_counted.md)를 참고하세요. 이 기능은 중요성 때문에 4.0.14.Final에도 백포트되었습니다.

### 기본 할당자 `PooledByteBufAllocator`

4.x에서는 한계가 있음에도 `UnpooledByteBufAllocator`가 기본 할당자였습니다. `PooledByteBufAllocator`가 충분히 검증되었고 고급 누수 추적 메커니즘도 갖춰졌으므로, 이제 기본값으로 변경할 때가 되었습니다.

### 글로벌 유일 채널 ID

이제 모든 [`Channel`]은 다음 요소들을 조합해 생성되는 전역 유일 ID를 갖습니다.

* MAC 주소(EUI-48 또는 EUI-64), 가능하면 글로벌 유일한 것
* 현재 프로세스 ID
* `System#currentTimeMillis()`
* `System#nanoTime()`
* 무작위 32비트 정수
* 순차적으로 증가하는 32비트 정수

[`Channel`]의 ID는 `Channel.id()` 메서드로 얻을 수 있습니다.

### `EmbeddedChannel` 사용성

[`EmbeddedChannel`]의 `readInbound()`와 `readOutbound()`가 제네릭 타입 파라미터로 반환값을 추론하므로 다운캐스트가 필요 없어집니다. 단위 테스트 코드의 장황함이 크게 줄어듭니다.

```java
EmbeddedChannel ch = ...;

// BEFORE:
FullHttpRequest req = (FullHttpRequest) ch.readInbound();

// AFTER:
FullHttpRequest req = ch.readInbound();
```

### `ThreadFactory` 대신 `Executor` 사용 가능

일부 애플리케이션은 특정 `Executor`에서 태스크를 실행해야 합니다. 4.x에서는 이벤트 루프 생성 시 `ThreadFactory`만 지정할 수 있었지만, 이제는 `Executor`를 직접 지정할 수도 있습니다.

자세한 내용은 [PR #1762](https://github.com/netty/netty/pull/1762)을 참고하세요.

### 클래스 로더 친화성

`AttributeKey` 같은 일부 타입은 컨테이너 환경에서 문제가 있었지만, 이제는 클래스 로더를 올바르게 인식하도록 개선되었습니다.

### `ByteBufAllocator.calculateNewCapacity()`

`ByteBuf` 확장 시 새 용량을 계산하는 로직이 `AbstractByteBuf`에서 `ByteBufAllocator`로 이동했습니다. 버퍼 용량 계산은 해당 버퍼를 관리하는 `ByteBufAllocator`가 더 잘 알기 때문입니다.

## 새 코덱과 핸들러

* 바이너리 memcache 프로토콜 코덱
* 압축 코덱
  * BZip2
  * FastLZ
  * LZ4
  * LZF
* DNS 프로토콜 코덱
* HAProxy 프로토콜 코덱
* MQTT 프로토콜 코덱
* SPDY/3.1 지원
* STOMP 코덱
* 버전 4, 4a, 5를 지원하는 SOCKSx 코덱; `socksx` 패키지 참고.
* XML 문서 스트리밍을 가능하게 하는 [`XmlFrameDecoder`].
* JSON 객체 스트리밍을 가능하게 하는 [`JsonObjectDecoder`].
* IP 필터링 핸들러

## 그 외 코덱 변경

### `AsciiString`

[`AsciiString`]은 1바이트 문자만 담는 새로운 `CharSequence` 구현입니다. US-ASCII나 ISO-8859-1 문자열을 다룰 때 유용합니다.

Netty의 HTTP 코덱과 STOMP 코덱은 헤더 이름을 표현하는 데 `AsciiString`을 사용합니다. `AsciiString`은 `ByteBuf`로 인코딩할 때 변환 비용이 없으므로 `String`보다 나은 성능을 제공합니다.

### `TextHeaders`

[`TextHeaders`]는 HTTP 헤더 같은 문자열 [multimap](http://en.wikipedia.org/wiki/Multimap)을 위한 범용 자료구조를 제공합니다. `HttpHeaders`도 `TextHeaders`로 다시 작성되었습니다.

### `MessageAggregator`

[`MessageAggregator`]는 `HttpObjectAggregator`처럼 작은 여러 메시지를 더 큰 하나의 메시지로 집계하는 범용 기능을 제공합니다. `HttpObjectAggregator`도 `MessageAggregator`로 다시 작성되었습니다.

### `HttpObjectAggregator`의 더 나은 oversized 메시지 처리

4.0에서는 클라이언트가 `100-continue` 헤더를 보내더라도, 클라이언트가 본문을 전송하기 전에 oversized HTTP 메시지를 거부할 방법이 없었습니다.

이번 릴리스에서는 오버라이드 가능한 `handleOversizedMessage` 메서드를 추가해 사용자가 원하는 처리를 직접 구현할 수 있습니다. 기본 동작은 '413 Request Entity Too Large' 응답 후 연결을 닫는 것입니다.

### `ChunkedInput`과 `ChunkedWriteHandler`

[`ChunkedInput`]에는 두 개의 새 메서드 `progress()`와 `length()`가 있어, 각각 전송 진행 상태와 스트림 전체 길이를 반환합니다. [`ChunkedWriteHandler`]는 이 정보를 사용해 [`ChannelProgressiveFutureListener`]에 알립니다.

### `SnappyFramedEncoder`와 `SnappyFramedDecoder`

이 두 클래스는 `SnappyFrameEncoder`와 `SnappyFrameDecoder`로 이름이 변경되었습니다. 기존 클래스는 deprecated로 표시되었으며, 내부적으로 새 클래스의 서브클래스로 구현되어 있습니다.

[`AttributeKey`]: http://netty.io/4.1/api/io/netty/util/AttributeKey.html
[`AttributeMap`]: http://netty.io/4.1/api/io/netty/util/AttributeMap.html
[`EventExecutor`]: http://netty.io/4.1/api/io/netty/util/concurrent/EventExecutor.html

[`Channel`]: http://netty.io/4.1/api/io/netty/channel/Channel.html
[`ChannelHandler`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandler.html
[`ChannelHandlerAdapter`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandlerAdapter.html
[`ChannelHandlerContext`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandlerContext.html
[`ChannelHandlerInvoker`]: http://netty.io/4.1/api/io/netty/channel/ChannelHandlerInvoker.html
[`ChannelPipeline`]: http://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html

[`SimpleChannelInboundHandler`]: http://netty.io/4.1/api/io/netty/channel/SimpleChannelInboundHandler.html

[`EmbeddedChannel`]: http://netty.io/4.1/api/io/netty/channel/embedded/EmbeddedChannel.html

[`JsonObjectDecoder`]: http://netty.io/4.1/api/io/netty/handler/codec/json/JsonObjectDecoder.html
[`XmlFrameDecoder`]: http://netty.io/4.1/api/io/netty/handler/codec/xml/XmlFrameDecoder.html

[`AsciiString`]: http://netty.io/4.1/api/io/netty/handler/codec/AsciiString.html
[`TextHeaders`]: http://netty.io/4.1/api/io/netty/handler/codec/TextHeaders.html
[`MessageAggregator`]: http://netty.io/4.1/api/io/netty/handler/codec/MessageAggregator.html

[`ChunkedInput`]: http://netty.io/4.1/api/io/netty/handler/stream/ChunkedInput.html
[`ChunkedWriteHandler`]: http://netty.io/4.1/api/io/netty/handler/stream/ChunkedWriteHandler.html
[`ChannelProgressiveFutureListener`]: http://netty.io/4.1/api/io/netty/channel/ChannelProgressiveFutureListener.html
