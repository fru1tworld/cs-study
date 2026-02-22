# Java 상식: 이중 검사 잠금 패턴 (Double-Checked Locking Pattern)

> 원문: https://blog.jooq.org/the-double-checked-locking-pattern/

Java에서 지연 초기화(lazy initialisation)를 할 때, 멀티스레드 환경에서 안전하게 구현하는 방법 중 하나가 이중 검사 잠금 패턴(Double-Checked Locking Pattern)입니다.

## 기본적인 동기화 접근법

가장 간단한 스레드 안전 방식은 전체 getter 메서드를 동기화하는 것입니다:

```java
class Foo {
    private Helper helper = null;
    public synchronized Helper getHelper() {
        if (helper == null) {
            helper = new Helper();
        }
        return helper;
    }
}
```

이 방법은 동작하지만 성능 비용이 있습니다. 초기화가 완료된 후에도 매번 접근할 때마다 인스턴스에 대한 잠금을 획득해야 합니다.

## 이중 검사 잠금 해결책

초기화 후 반복적인 잠금 획득을 피하기 위해, 이중 검사 잠금은 동기화 오버헤드를 줄입니다. 중요한 점은 `volatile` 키워드가 필수적이라는 것입니다 - 이는 스레드 간에 쓰기 작업의 가시성을 보장합니다.

JDK의 `java.io.File.toPath()` 메서드가 이 패턴을 올바르게 구현한 예입니다:

```java
private volatile Path filePath;

public Path toPath() {
    Path result = filePath;
    if (result == null) {
        synchronized (this) {
            result = filePath;
            if (result == null) {
                result = FileSystems.getDefault().getPath(path);
                filePath = result;
            }
        }
    }
    return result;
}
```

## 패턴의 동작 방식

1. 먼저 필드를 확인합니다 (첫 번째 검사)
2. null인 경우에만 synchronized 블록에 진입합니다
3. 블록 내부에서 다시 확인합니다 (두 번째 검사)
4. 여전히 null이면 초기화를 수행합니다

이 방식은 초기 설정 이후 동기화 오버헤드를 최소화하면서 스레드 안전성과 성능의 균형을 맞춥니다.

## 핵심 포인트

- 이중 검사 잠금은 메모리 모델 개선으로 인해 Java 1.5 이상이 필요합니다
- `volatile` 키워드는 정확성을 위해 필수적입니다
- 이 패턴은 초기화 완료 후 잠금 경합을 줄입니다
- JDK 자체에서도 이 패턴을 사용합니다 (예: `java.io.File.toPath()`)

## Java 1.5 이상이 필요한 이유

Java 1.5 이전에는 이 패턴이 신뢰할 수 없었습니다. 이전 버전에서는 필요한 메모리 보장이 부족했기 때문입니다. Java 5에서 메모리 모델이 개선되면서 `volatile` 키워드의 의미가 강화되었고, 이로 인해 이중 검사 잠금 패턴이 안전하게 사용될 수 있게 되었습니다.

## 지역 변수 사용의 중요성

코드에서 `Path result = filePath;`처럼 지역 변수를 사용하는 것에 주목하세요. 이는 volatile 필드를 한 번만 읽도록 하여 성능을 개선합니다. volatile 읽기는 일반 읽기보다 비용이 더 들기 때문입니다.
