# Java가 오늘 설계되었다면: Synchronizable 인터페이스

> 원문: https://blog.jooq.org/if-java-were-designed-today-the-synchronizable-interface/

게시일: 2016년 1월 12일, lukaseder

Java는 크게 발전해왔지만 초기 설계 당시의 레거시 결정들을 여전히 안고 있습니다. 자주 후회되는 측면 중 하나는 "모든 객체가 (잠재적으로) 모니터를 포함한다"는 것입니다. 이 결함은 Java 5에서 `java.util.concurrent.locks.Lock`과 관련 타입들과 같은 새로운 동시성 API로 부분적으로 해결되었습니다. 그 전에는 개발자들이 `synchronized` 키워드와 까다로운 `wait()` 및 `notify()` 메커니즘에 의존해야 했습니다.

## synchronized 수정자는 이제 거의 사용되지 않습니다

원래의 언어 설계는 메서드에 다음과 같은 편의 수정자를 허용했습니다:

```java
// 이것들은 의미적으로 동일합니다:
public synchronized void method() {
    ...
}

public void method() {
    synchronized (this) {
        ...
    }
}

// 이것들도 마찬가지입니다:
public static synchronized void method() {
    ...
}

public static void method() {
    synchronized (ClassOfMethod.class) {
        ...
    }
}
```

*(참고: 바이트코드는 다르지만, 고수준 의미론은 동등합니다.)*

개발자들은 동기화 지속 시간을 최소화하기 위해 전체 메서드 범위를 동기화하는 경우가 드뭅니다. 동기화가 필요할 때마다 메서드를 추출하는 것은 번거롭습니다. 게다가 모니터는 캡슐화를 깨뜨립니다 - `this`나 클래스 자체를 사용하면 누구든지 당신의 모니터에 동기화할 수 있습니다. 대부분의 현대 개발자들은 대신 명시적인 private 락 객체를 생성합니다:

```java
class SomeClass {
    private Object LOCK = new Object();

    public void method() {
        ...

        synchronized (LOCK) {
            ...
        }

        ...
    }
}
```

이것이 고전적인 `synchronized` 블록의 표준 사용 사례라면, 실제로 모든 객체에 모니터가 필요할까요?

## 더 현대적인 Java 버전에서의 Synchronized

오늘날의 Java에 대한 이해를 바탕으로, 언어는 문자열이나 배열과 같은 임의의 객체에 대해 `synchronized`를 허용하지 않을 것입니다:

```java
// 작동하지 않을 것입니다
synchronized ("abc") {
    ...
}
```

대신, 특별한 `Synchronizable` 마커 인터페이스가 구현자가 모니터를 소유함을 보장할 것입니다. `synchronized` 블록은 `Synchronizable` 인수만 받아들일 것입니다:

```java
Synchronizable lock = ...

synchronized (lock) {
    ...
}
```

이 접근 방식은 foreach와 try-with-resources 패턴을 따릅니다:

```java
Iterable<Object> iterable = ...

// ":" 오른쪽의 타입은 Iterable이어야 합니다
for (Object o : iterable) {
    ...
}

// 할당 타입은 AutoCloseable이어야 합니다
try (AutoCloseable closeable = ...) {
    ...
}

// 할당 타입은 함수형 인터페이스여야 합니다
Runnable runnable = () -> {};
```

언어 기능은 해당 컨텍스트에서 사용되는 타입에 제약을 부과합니다. foreach와 try-with-resources는 특정 JDK 타입을 요구합니다. 람다 표현식은 일치하는 구조적 타입을 요구합니다 - Java에서는 난해하지만 영리한 방식입니다. 안타깝게도 하위 호환성 때문에 새로운 `synchronized` 제한이 불가능합니다. 그러나 `Synchronizable`이 아닌 타입에 대한 선택적 경고는 향후 주요 릴리스에서 불필요한 모니터를 제거할 수 있게 해줄 수 있습니다. 이것은 C 언어 접근 방식을 따릅니다 - 뮤텍스는 특별한 것이지 보편적인 것이 아닙니다.

---

## 댓글 섹션

Thire A (2016년 1월 12일): jOOQ를 NetBeans와 통합하는 방법에 대해 질문합니다.

lukaseder (2016년 1월 12일): jooq.org 문서의 Maven 기반 튜토리얼을 안내합니다.

dsjn (2016년 1월 12일): synchronized 메서드와 synchronized 블록이 바이트코드 수준에서 상당히 다르다고 지적하며, Artima의 JVM 내부 리소스를 인용합니다.

lukaseder (2016년 1월 12일): 기술적인 바이트코드 차이를 인정하면서도 더 높은 추상화 수준에서의 의미론적 동등성을 유지한다고 설명합니다.

Peter Verhas (2016년 1월 13일): 비슷한 글을 읽었다고 언급하며 Value Type 제안을 참조합니다.

lukaseder (2016년 1월 15일): 우연의 일치라고 언급하며 JDK 10 value type 제안에 대해 알고 있다고 말합니다.

Peter Verhas (2016년 1월 13일): 왜 `Synchronizable`이 final 클래스가 아닌 마커 인터페이스여야 하는지 질문합니다.

lukaseder (2016년 1월 15일): 대안적인 설계 가능성을 탐구하는 Reddit 토론을 참조합니다.

Ivo (2016년 1월 14일): Plumbr의 락 경합 모니터링 경험을 논의하며, synchronized 코드 성능 문제는 잘못된 동시성 설계 패턴에서 비롯된다고 지적합니다. 그들의 안티패턴 글 링크를 공유합니다.

lukaseder (2016년 1월 15일): Ivo에게 인사이트 공유에 감사를 표합니다.
