# 다른 모든 것이 실패할 때: "the Unsafe" 사용하기

> 원문: https://blog.jooq.org/when-all-else-fails-using-the-unsafe/

때때로 당신은 해킹을 해야 합니다. 정말 그래야만 할 때가 있습니다. [XKCD의 말을 듣지 마세요. 해킹을 항상 후회하는 것은 아닙니다](http://xkcd.com/292/). 우리 블로그에서는 이전에 몇 가지 해킹 방법을 보여드린 적이 있습니다:

- [Java에서 체크 예외를 런타임 예외처럼 던지기](https://blog.jooq.org/throw-checked-exceptions-like-runtime-exceptions-in-java/)
- [Java에서 private final 필드를 수정하는 지저분한 해킹](https://blog.jooq.org/a-dirt-ugly-hack-to-modify-private-final-fields-in-java/)

하지만 우리는 겨우 표면만 긁었을 뿐입니다. [ZeroTurnaround](http://zeroturnaround.com/) / [RebelLabs](http://zeroturnaround.com/rebellabs/)의 친구들이 최근 ["the Unsafe"를 사용하는 방법에 대한 훌륭한 글](http://zeroturnaround.com/rebellabs/dangerous-code-how-to-be-unsafe-with-java-classes-objects-in-memory/)을 게시했습니다. `sun.misc.Unsafe` 클래스를 사용하여 Java에서 메모리에 직접 접근하는 방법입니다. 첫 번째 페이지에서는 Unsafe 객체 자체와 리플렉션을 통해 어떻게 접근하는지 소개합니다...

```java
public static Unsafe getUnsafe() {
    try {
        Field f = Unsafe.class
            .getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    } catch (Exception e) {
        /* ... */
    }
}
```

... 이어지는 섹션들에서는 "unsafe" 메모리 접근 메서드를 [메모리에서 Class를 주소 지정하는 것](http://zeroturnaround.com/rebellabs/dangerous-code-how-to-be-unsafe-with-java-classes-objects-in-memory/3/)과 [메모리에서 객체를 다루는 것](http://zeroturnaround.com/rebellabs/dangerous-code-how-to-be-unsafe-with-java-classes-objects-in-memory/4/)에 어떻게 매핑하는지 잘 설명합니다...

```java
// 대담하다면, 힙을 직접 조작해보세요!
Object helperArray[] = new Object[1];
helperArray[0] = targetObject;
long baseOffset =
    unsafe.arrayBaseOffset(Object[].class);
long addressOfObject =
    unsafe.getLong(helperArray, baseOffset);
```

그러나 이것이 그렇게 쉽다고 생각하지 마세요. Java 힙을 직접 조작하려면 클래스 헤더의 다양한 필드와 플래그에 대해 많이 이해해야 하고, 32비트와 64비트 JVM을 항상 구분해야 한다는 것을 기억해야 합니다.
