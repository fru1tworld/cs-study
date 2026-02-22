# Java ThreadLocal을 사용한 의도치 않은 재귀 방지

> 원문: https://blog.jooq.org/inadvertent-recursion-protection-with-java-threadlocals/

게시일: 2013년 3월 29일, lukaseder 작성

## 들어가며

Apache Jackrabbit과 같은 서드파티 라이브러리를 확장할 때 실용적인 문제를 다룹니다. 이러한 라이브러리는 접근 제어 검사가 있는 계층적 데이터 모델을 가지고 있습니다. 커스텀 접근 제어를 구현할 때 재귀적인 접근 검사가 트리거되어 잠재적으로 `StackOverflowError`가 발생할 수 있는 문제가 있습니다.

여러분의 접근 제어 알고리즘은 콘텐츠 저장소의 다른 노드에 접근할 것입니다... 그러면 다시 접근 제어가 트리거됩니다... 그리고 다시 다른 노드에 접근하게 됩니다.

## 해결 방법

두 가지 해결책이 있습니다:

1. 올바른 방법: 라이브러리 내부 구조를 이해하고 적절한 메커니즘(Jackrabbit의 System Sessions 같은)을 사용하여 커스텀 접근 제어 로직 수행 시 접근 제어를 우회합니다.

2. 빠른 트릭: `ThreadLocal` 변수를 사용하여 의도치 않은 재귀를 감지하고 방지합니다.

## 코드 구현

### 기본 재귀 방지

```java
static final ThreadLocal<Boolean> RECURSION_CONTROL
    = new ThreadLocal<Boolean>();

public static <T> T protect(
        T resultOnRecursion,
        Protectable<T> delegate)
throws Exception {

    // 아직 재귀가 시작되지 않았는지 확인
    if (RECURSION_CONTROL.get() == null) {
        try {
            // 재귀 시작을 표시
            RECURSION_CONTROL.set(true);
            return delegate.call();
        }
        finally {
            // 항상 정리
            RECURSION_CONTROL.remove();
        }
    }
    else {
        // 재귀 감지됨, 기본값 반환
        return resultOnRecursion;
    }
}

public interface Protectable<T> {
    T call() throws Exception;
}
```

이 ThreadLocal은 레벨 1로 재귀를 시작했는지 여부를 나타내며, 실행이 보호된 코드에 다시 진입할 때 이를 감지하여 무한 루프를 방지합니다.

### 사용 예제

```java
public static void main(String[] args)
throws Exception {
    protect(null, new Protectable<Void>() {
        @Override
        public Void call() throws Exception {
            System.out.println("재귀 중?");
            main(null);
            System.out.println("아니요!");
            return null;
        }
    });
}
```

### 고급: 다중 재귀 레벨

`Boolean` 대신 `Integer` 카운터를 사용하여 N 레벨의 재귀를 허용하는 정교한 버전입니다:

```java
static final ThreadLocal<Integer> RECURSION_CONTROL
    = new ThreadLocal<Integer>();

public static <T> T protect(
        T resultOnRecursion,
        Protectable<T> delegate)
throws Exception {
    Integer level = RECURSION_CONTROL.get();
    level = (level == null) ? 0 : level;

    // 최대 5레벨까지 재귀 허용
    if (level < 5) {
        try {
            // 진입 시 증가
            RECURSION_CONTROL.set(level + 1);
            return delegate.call();
        }
        finally {
            // 종료 시 감소
            if (level > 0)
                RECURSION_CONTROL.set(level - 1);
            else
                RECURSION_CONTROL.remove();
        }
    }
    else {
        // 최대 깊이 초과, 기본값 반환
        return resultOnRecursion;
    }
}
```

이 버전은 재귀 깊이를 추적하고 N 레벨까지 허용합니다(예제에서는 5). 진입 시 증가하고 종료 시 감소하며, 최대 깊이를 초과하면 기본값을 반환합니다.

## 중요한 주의사항

"아마도 여러분은 몇 분만 더 시간을 들여서 호스트 라이브러리의 내부 구조가 실제로 어떻게 작동하는지 배워야 할 것입니다." 이 트릭에 의존하기보다는 적절한 해결책(Jackrabbit의 System Sessions 사용 같은)이 더 바람직합니다.

## 결론

라이브러리 내부 구조를 이해하고 적절한 해결책을 구현하는 것이 이 트릭을 사용하는 것보다 바람직합니다.

## 댓글 섹션

토론에서 Arnaud가 Java 7에서 도입된 try-with-resources 구문을 활용하여 재귀 제어를 관리하는 `AutoCloseable` 기반 접근 방식을 시연한 가치 있는 기여가 있었습니다.
