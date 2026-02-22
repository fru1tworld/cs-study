# Mute 디자인 패턴

> 원문: https://blog.jooq.org/the-mute-design-pattern/

최근에 Mute-Design-Pattern(뮤트 디자인 패턴)™을 따르는 코드를 많이 작성하고 계셨나요? 예를 들어:

```java
try {
    complex();
    logic();
    here();
}
catch (Exception ignore) {
    // 절대 발생하지 않을 거야 ㅎㅎ
    System.exit(-1);
}
```

Java 8을 사용하면 더 쉬운 방법이 있습니다! 이 매우 유용한 도구를 여러분의 Utilities 또는 Helper 클래스에 추가하세요:

```java
public class Helper {
    @FunctionalInterface
    interface CheckedRunnable {
        void run() throws Throwable;
    }

    public static void mute(CheckedRunnable r) {
        try {
            r.run();
        }
        catch (Throwable ignore) {
            // OK, 안전하게 가는 게 나아
            ignore.printStackTrace();
        }
    }
}
```

이제 모든 로직을 이 멋진 작은 래퍼로 감쌀 수 있습니다:

```java
mute(() -> {
    complex();
    logic();
    here();
});
```

끝입니다! 더 좋은 점은, 어떤 경우에는 메서드 참조를 사용할 수도 있다는 것입니다:

```java
try (Connection con = ...;
     PreparedStatement stmt = ...) {
    mute(stmt::executeUpdate);
}
```

---

역자 주: 이 글은 풍자적인 글입니다. 실제로 예외를 무시하고 삼키는 것은 매우 나쁜 프로그래밍 관행입니다. 저자는 개발자들이 종종 예외를 적절히 처리하지 않고 무시하는 안티패턴을 유머러스하게 비판하고 있습니다. 실제 프로덕션 코드에서는 예외를 적절히 처리하고, 로깅하며, 필요한 경우 상위로 전파해야 합니다.
