# Netty를 범용 라이브러리로 사용하기

> 이 문서는 Netty 공식 Wiki의 "Using as a generic library" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/using-as-a-generic-library.html

---

Netty는 네트워크 애플리케이션을 만들기 위한 프레임워크지만, 소켓 I/O를 수행하지 않는 프로그램에서도 유용한 기반 클래스를 함께 제공합니다.

## Buffer API

`io.netty.buffer`는 `ByteBuf`라는 범용 버퍼 타입을 제공합니다. `java.nio.ByteBuffer`와 비슷하지만 더 빠르고, 더 사용자 친화적이며, 확장 가능합니다.

### 사용자 친화성

`java.nio.ByteBuffer.flip()` 호출을 잊고 버퍼에 아무것도 들어 있지 않은 이유를 의아해해본 적이 있나요? `ByteBuf`에는 읽기용과 쓰기용 두 개의 인덱스가 있어 그런 일이 일어나지 않습니다.

```java
ByteBuf buf = ...;
buf.writeUnsignedInt(42);
assertThat(buf.readUnsignedInt(), is(42));
```

`ByteBuf`는 버퍼 내용에 더 쉽게 접근할 수 있는 풍부한 접근 메서드 세트를 갖고 있습니다. 예를 들어 부호 있는/없는 정수, 검색, 문자열용 접근 메서드가 있습니다.

### 확장성

`java.nio.ByteBuffer`는 서브클래싱할 수 없지만 `ByteBuf`는 가능합니다. 편의를 위해 추상 골격(skeletal) 구현도 제공됩니다. 따라서 파일 기반 버퍼, 합성 버퍼, 심지어 하이브리드 같은 자신만의 버퍼 구현을 작성할 수 있습니다.

### 성능

새 `java.nio.ByteBuffer`가 할당될 때 그 내용은 0으로 채워집니다. 이 "zeroing"은 CPU 사이클과 메모리 대역폭을 소비합니다. 보통 그 직후 어떤 데이터 소스로부터 버퍼가 채워지므로, zeroing은 결국 헛수고가 됩니다.

`java.nio.ByteBuffer`는 회수를 JVM 가비지 컬렉터에 의존합니다. heap 버퍼에는 잘 동작하지만 direct 버퍼에는 그렇지 않습니다. 설계상 direct 버퍼는 오랫동안 살아있을 것으로 가정됩니다. 따라서 짧게 살았다 사라지는 direct NIO 버퍼를 많이 할당하면 종종 `OutOfMemoryError`가 발생합니다. 또한 (숨겨져 있고 독점적인) API로 direct 버퍼를 명시적으로 해제하는 것도 그리 빠르지 않습니다.

`ByteBuf`의 라이프사이클은 참조 카운트에 묶여 있습니다. 카운트가 0이 되면, 그 기반 메모리 영역(`byte[]` 또는 direct 버퍼)이 명시적으로 dereferenced/deallocated되거나 풀로 반환됩니다.

Netty는 또한 버퍼를 zeroing하는 데 CPU 사이클이나 메모리 대역폭을 낭비하지 않는 견고한 버퍼 풀 구현을 제공합니다.

```java
ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;
ByteBuf buf = alloc.directBuffer(1024);
...
buf.release(); // direct 버퍼가 풀로 반환된다.
```

다만 참조 카운트가 만능은 아닙니다. 풀링된 버퍼의 기반 메모리 영역이 풀로 반환되기 전에 JVM이 가비지 컬렉트해버리면, 누수가 결국 풀을 고갈시킵니다.

누수를 추적하는 데 도움이 되도록 Netty는 누수 검출 메커니즘을 제공하며, 애플리케이션 성능과 누수 보고서의 상세도 사이에서 트레이드오프를 조정할 수 있을 만큼 유연합니다. 자세한 내용은 [참조 카운트 객체](./08_reference_counted.md)를 참고하세요.

## 리스너를 등록할 수 있는 Future와 이벤트 루프

태스크를 비동기로 수행하는 일 — 태스크를 스케줄링하고 완료 시 알림을 받는 일 — 은 흔하고, 또 쉬워야 합니다. `java.util.concurrent.Future`가 처음 등장했을 때의 흥분은 오래가지 않았습니다. 완료 알림을 받으려면 _블록_ 해야 했기 때문입니다. 비동기 프로그래밍에서는 결과를 기다리는 대신 완료 시 _무엇을 할지_ 를 지정합니다.

`io.netty.concurrent.Future`는 JDK `Future`의 서브타입입니다. 리스너를 추가할 수 있고, future가 완료되면 이벤트 루프에 의해 그 리스너가 호출됩니다.

`io.netty.util.concurrent.EventExecutor`는 `java.util.concurrent.ScheduledExecutorService`를 상속하는 단일 스레드 이벤트 루프입니다. 자신만의 이벤트 루프를 만들거나, 기능이 풍부한 태스크 executor로 사용할 수 있습니다. 보통은 병렬성을 활용하기 위해 여러 개의 `EventExecutor`를 만듭니다.

```java
EventExecutorGroup group = new DefaultEventExecutorGroup(4); // 스레드 4개
Future<?> f = group.submit(new Runnable() { ... });
f.addListener(new FutureListener<?> {
  public void operationComplete(Future<?> f) {
    ..
  }
});
...
```

### 글로벌 이벤트 루프

가끔은 라이프사이클 관리 없이도 항상 사용할 수 있는 단일 executor가 필요할 때가 있습니다. `GlobalEventExecutor`는 단일 스레드 싱글턴 `EventExecutor`로, 스레드를 lazy하게 시작하고 한동안 보류된 태스크가 없으면 정지합니다.

```java
GlobalEventExecutor.INSTANCE.execute(new Runnable() { ... });
```

내부적으로 Netty는 다른 `EventExecutor`들의 종료를 통지하는 데 이 클래스를 사용합니다.

## 플랫폼 의존 연산

_참고: 이 기능은 내부 사용 전용입니다. 충분한 수요가 있다면 internal 패키지 밖으로 옮기는 것을 검토 중입니다._

`io.netty.util.internal.PlatformDependent`는 플랫폼에 의존적이고 잠재적으로 unsafe한 연산을 제공합니다. `sun.misc.Unsafe`를 비롯한 플랫폼 의존 독점 API 위의 얇은 레이어라고 생각하시면 됩니다.

## 그 외 유틸리티

고성능 네트워크 애플리케이션 프레임워크를 만들기 위해 유용한 유틸리티들을 도입했습니다. 그중 일부가 도움이 될 수도 있습니다.

### 스레드 로컬 객체 풀

스레드가 오래 살고 같은 타입의 단명한 객체를 많이 할당한다면, `Recycler`라는 스레드 로컬 객체 풀을 사용할 수 있습니다. 만들어내는 가비지 양을 줄여 메모리 대역폭 소비와 가비지 컬렉터 부담을 절약합니다.

```java
public class MyObject {

  private static final Recycler<MyObject> RECYCLER = new Recycler<MyObject>() {
    protected MyObject newObject(Recycler.Handle<MyObject> handle) {
      return new MyObject(handle);
    }
  }

  public static MyObject newInstance(int a, String b) {
    MyObject obj = RECYCLER.get();
    obj.myFieldA = a;
    obj.myFieldB = b;
    return obj;
  }
    
  private final Recycler.Handle<MyObject> handle;
  private int myFieldA;
  private String myFieldB;

  private MyObject(Handle<MyObject> handle) {
    this.handle = handle;
  }
  
  public boolean recycle() {
    myFieldA = 0;
    myFieldB = null;
    return handle.recycle(this);
  }
}

MyObject obj = MyObject.newInstance(42, "foo");
...
obj.recycle();
```

### 사용자 확장 가능한 enum

`enum`은 정적인 상수 집합에는 좋지만 확장할 수 없습니다. 런타임에 더 많은 상수를 추가하거나 서드파티가 추가 상수를 정의할 수 있게 하려면, 확장 가능한 `io.netty.util.ConstantPool`을 사용하세요.

```java
public final class Foo extends AbstractConstant<Foo> {
  Foo(int id, String name) {
    super(id, name);
  }
}

public final class MyConstants {

  private static final ConstantPool<Foo> pool = new ConstantPool<Foo>() {
    @Override
    protected Foo newConstant(int id, String name) {
      return new Foo(id, name);
    }
  };

  public static Foo valueOf(String name) {
    return pool.valueOf(name);
  }

  public static final Foo A = valueOf("A");
  public static final Foo B = valueOf("B");
}

private final class YourConstants {
  public static final Foo C = MyConstants.valueOf("C");
  public static final Foo D = MyConstants.valueOf("D");
}
```

Netty는 `ChannelOption`을 정의할 때 `ConstantPool`을 사용해, 코어가 아닌 전송도 타입 안전한 방식으로 자체 옵션을 정의할 수 있도록 합니다.

### 속성 맵 (Attribute map)

빠르고 타입 안전하며 스레드 안전한 키-값 모음을 위해 `io.netty.util.AttributeMap` 인터페이스를 사용할 수 있습니다.

```java
public class Foo extends DefaultAttributeMap {
  ...
}

public static final AttributeKey<String> ATTR_A = AttributeKey.valueOf("A");
public static final AttributeKey<Integer> ATTR_B = AttributeKey.valueOf("B");

Foo o = ...;
o.attr(ATTR_A).set("foo");
o.attr(ATTR_B).set(42);
```

이미 눈치챘겠지만 `AttributeKey`는 `Constant`의 일종입니다.

### Hashed wheel timer

Hashed wheel timer는 `java.util.Timer`와 `java.util.concurrent.ScheduledThreadPoolExecutor`의 확장 가능한 대안입니다. 다음 표에서 보듯이 많은 스케줄링 작업과 그 취소를 효율적으로 처리할 수 있습니다.

| | 새 태스크 스케줄 | 태스크 취소 |
| --- | --- | --- |
| `HashedWheelTimer` | O(1) | O(1) |
| `java.util.Timer`와 `ScheduledThreadPoolExecutor` | O(logN) | O(logN) (N = 보류 중 태스크 수) |

내부적으로는 태스크의 시점(timing)을 키로 하는 해시 테이블을 사용해 대부분의 타이머 연산에서 상수 시간을 제공합니다. (`java.util.Timer`는 이진 힙을 사용합니다.)

자세한 내용은 [이 슬라이드("Hashed and Hierarchical Timing Wheels," Dharmapurikar)](http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt)와 [이 논문("Hashed and Hierarchical Timing Wheels: Data Structures for the Efficient Implementation of a Timer Facility," Varghese and Lauck)](http://www.cs.columbia.edu/~nahum/w6998/papers/sosp87-timing-wheels.pdf)을 참고하세요.

### 그 외 잡다한 유틸리티

다음 클래스들은 유용하지만, Guava 같은 다른 라이브러리에 더 좋은 대안이 있을 수도 있습니다.

* `io.netty.util.CharsetUtil`은 자주 쓰이는 `java.nio.charset.Charset`을 제공합니다.
* `io.netty.util.NetUtil`은 IPv4/IPv6 loopback 주소의 `InetAddress` 같은, 자주 쓰이는 네트워크 관련 상수를 제공합니다.
* `io.netty.util.DefaultThreadFactory`는 executor 스레드를 손쉽게 설정할 수 있게 해주는 범용 `ThreadFactory` 구현입니다.

## Guava 및 JDK8과의 비교

Netty는 의존성 집합을 최소화하려고 하므로, 일부 유틸리티 클래스는 [Guava](http://code.google.com/p/guava-libraries/) 같은 다른 인기 라이브러리의 그것과 비슷합니다.

이런 라이브러리들은 JDK API의 불편함을 줄여주는 다양한 유틸리티 클래스와 대체 자료형을 제공하며, 이 일을 꽤 잘 해냅니다.

Netty는 다음을 위한 구성 요소 제공에 집중합니다.

* 비동기 프로그래밍
* 다음과 같은 저수준 연산 (이른바 "mechanical sympathy"):
  * Off-heap 접근
  * 독점 intrinsic 연산 접근
  * 플랫폼 의존 동작

자바는 때때로 Netty가 제공하던 구성 요소를 포섭하는 아이디어를 채택하면서 발전합니다. 예를 들어 JDK 8은 [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)를 추가했는데, 이는 `io.netty.util.concurrent.Future`와 어느 정도 겹칩니다. 그런 경우 Netty의 구성 요소는 좋은 마이그레이션 경로를 제공합니다. 우리는 향후 마이그레이션을 염두에 두고 API를 부지런히 갱신할 것입니다.
