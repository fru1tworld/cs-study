# Java 24와 sun.misc.Unsafe

> 이 문서는 Netty 공식 Wiki의 "Java 24 and sun.misc.Unsafe" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/java-24-and-sun.misc.unsafe.html

---

Java 24는 [JEP 498](https://openjdk.org/jeps/498)을 통합하면서 `sun.misc.Unsafe`의 메모리 접근 메서드가 처음 사용될 때 콘솔에 경고를 출력하기 시작했습니다.

Netty는 역사적으로 native(off-heap) 메모리를 더 효율적으로 다루기 위해 이 메서드들에 의존해 왔습니다. 즉, Netty를 사용하는 애플리케이션이 Java 24 이상에서 실행되면 이 경고가 보일 수 있다는 뜻입니다.

## Netty 4.1과 Unsafe

Netty 4.1을 사용할 때 이 경고가 출력되지 않게 하려면 사용자가 다음 JVM 명령줄 인자로 Unsafe 메모리 접근을 명시적으로 허용해야 합니다.

```
--sun-misc-unsafe-memory-access=allow
```

### Netty 4.1.120과 4.1.121

Netty 4.1.120과 4.1.121은 우리 사용자에게 이 경고가 보이지 않게 하기 위해, Java 24 이상에서 실행될 때 `sun.misc.Unsafe` 사용을 기본적으로 비활성화했습니다 ([PR #14943](https://github.com/netty/netty/pull/14943)).

이 변경된 기본값은 Netty 4.1.122에서 되돌려졌고 ([PR #15296](https://github.com/netty/netty/pull/15296)), 동작이 Netty 4.1.119와 같아졌습니다.

이 동작 변경을 되돌린 이유는 1) 성능 회귀가 예상보다 컸고, 2) JNI 사용(예: 우리 native 전송이나 native TLS 구현 사용 시)도 비슷한 경고를 일으키지만 Netty 4.x 버전에서는 우회할 방법이 없기 때문입니다.

## Netty 4.2와 Unsafe

Netty 4.2.0과 4.2.1은 별도의 예방 조치를 하지 않으며, Netty 4.1(4.1.120/4.1.121 제외)과 동일한 경고를 출력합니다.

Netty 4.2.2 이상은 `Unsafe`에 대한 의존을 피하기 위해 `MemorySegment` API 사용을 지원합니다. 메모리 세그먼트 지원은 두 가지 변형으로 제공됩니다.

1. **선호되는 방식:** 시스템의 `malloc`/`free` 함수에 직접 링크. ([PR #15366](https://github.com/netty/netty/pull/15366))
   * 이 메커니즘은 Java 24부터 지원되지만 native access가 활성화되어 있어야 합니다.
   * native access를 활성화하려면 다음 명령줄 인자로 JVM을 실행해야 합니다:
   * `--enable-native-access=io.netty.common`
   * 이 메커니즘은 Netty 4.2.3부터 사용할 수 있습니다.
   * 할당과 해제 시 오버헤드가 가장 적기 때문에 선호되는 방식입니다.
2. **Fallback:** 공유 메모리 세그먼트 arena 사용. ([PR #15231](https://github.com/netty/netty/pull/15231))
   * 이 메커니즘은 native access를 요구하지 않습니다.
   * 그러나 JDK 버그로 인해 Java 25부터만 사용할 수 있습니다 ([PR #15338](https://github.com/netty/netty/pull/15338))
   * 이 메커니즘은 Netty 4.2.2부터 사용할 수 있습니다.
   * 이 메커니즘은 native access를 요구하지 않으므로 최후의 fallback입니다. 다만 공유 arena를 해제하는 일은 thread-local handshake(safepoint의 더 저렴한 형태)와 JIT 비최적화를 수반하기 때문에 비쌉니다.

## Netty가 Unsafe를 사용하는 용도

Netty와 그 native 전송은 운영체제와 데이터를 더 빠르게 주고받기 위해 native(off-heap) 메모리가 필요합니다. 즉, 데이터를 heap에 복사하지 않고 직접 주고받기 위함입니다.

native 메모리를 효과적으로 다루려면 메모리를 할당할 뿐 아니라 사용을 마치는 즉시 해제할 수단이 필요합니다. Netty는 direct `ByteBuffer` 내부의 cleaner 인스턴스에 접근하기 위해 `Unsafe`가 필요합니다. 이 cleaner가 direct `ByteBuffer` 인스턴스의 메모리를 해제하는 데 사용되는 메커니즘입니다.

`Unsafe`를 사용할 수 없는 경우에는 `ByteBuffer` cleaner도 사용할 수 없으므로, Netty는 메모리 해제를 가비지 컬렉터에 의존해야 합니다.

가비지 컬렉터는 heap 메모리 압박에 반응해 동작하지만, direct 버퍼는 heap에 큰 메모리 압박을 만들지 않습니다. 그 결과 native 메모리가 꼭 필요한 시점보다 더 늦게 해제되고, 이는 다시 메모리 사용량 증가로 이어집니다. JDK는 이를 우회하기 위해 새 direct `ByteBuffer` 인스턴스를 할당할 때 가끔 명시적으로 가비지 컬렉터를 돌리고 100밀리초 동안 sleep합니다.

이 동작 — 늘어난 명시적 GC와 100밀리초의 sleep — 은 Netty 입장에서 받아들일 수 없습니다. 이벤트 루프 스레드에서 sleep이나 어떠한 블로킹 연산도 허용할 수 없습니다.

이런 이유로, direct `ByteBuffer` cleaner를 사용할 수 없는 경우 Netty는 기본적으로 heap 버퍼를 사용합니다.

결과적으로 `Unsafe`가 없는 시스템에서는 heap 사용량이 크게 증가할 수 있고, 가비지 컬렉션에 사용되는 시간과 CPU 자원도 함께 증가하며, 시스템 전반의 성능이 저하됩니다.
