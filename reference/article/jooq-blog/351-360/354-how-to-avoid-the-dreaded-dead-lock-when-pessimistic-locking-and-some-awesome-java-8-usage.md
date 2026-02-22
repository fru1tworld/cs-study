# 비관적 잠금 시 두려운 데드락을 피하는 방법 - 그리고 멋진 Java 8 사용법!

> 원문: https://blog.jooq.org/how-to-avoid-the-dreaded-dead-lock-when-pessimistic-locking-and-some-awesome-java-8-usage/

데드락은 아무도 원하지 않는 것입니다. 데드락은 두 개 이상의 세션이 서로 연결된 리소스에 대한 잠금을 보유하고 있어 어느 세션도 진행할 수 없는 상황입니다. 비관적 잠금, 즉 SQL의 `FOR UPDATE` 절을 사용할 때 이런 상황이 발생할 수 있습니다.

## 비관적 잠금이란?

비관적 잠금은 데이터베이스에서 행 수준 잠금을 사용하여 동시 트랜잭션 간의 충돌을 방지하는 전략입니다. 이는 여러 세션이 동일한 행에 접근할 때 데이터 무결성을 보장하기 위해 사용됩니다.

다음과 같은 간단한 잠금 테이블을 만들어 보겠습니다:

```sql
CREATE TABLE locks (v NUMBER(18));
INSERT INTO locks SELECT level FROM dual CONNECT BY level <= 10;
```

이렇게 하면 10개의 행이 생성되며, 각 행은 테스트용 개별 행 수준 잠금 리소스로 사용됩니다.

## 데드락이 발생하는 방법

두 세션이 반대 순서로 잠금을 획득하면 데드락이 발생합니다:

- 세션 1: 행 1을 잠그고, 그 다음 행 2를 잠그려고 시도
- 세션 2: 행 2를 잠그고, 그 다음 행 1을 잠그려고 시도

이 경우 두 세션 모두 상대방이 보유한 잠금을 기다리게 되어 순환 대기 조건이 형성됩니다. Oracle은 이를 감지하고 다음 오류를 발생시킵니다:

```
ORA-00060: deadlock detected while waiting for resource
```

데이터베이스는 교착 상태를 해결하기 위해 세션 중 하나를 종료합니다.

## 해결책: 순서대로 잠금 획득

데드락을 피하는 간단한 방법은 모든 잠금이 항상 오름차순으로 획득되어야 한다는 규칙을 수립하는 것입니다. 잠금 1과 2가 필요하다면, 항상 그 순서대로 획득하세요. 이렇게 하면 데드락을 유발하는 순환 대기 조건을 방지할 수 있습니다.

## Java 8 시연

다음은 jOOλ 라이브러리를 사용한 멀티스레드 Java 예제입니다. 네 개의 스레드가 반복적으로 잠금을 획득합니다:

```java
IntStream.range(0, 4)
    .mapToObj(i -> new Thread(() -> {
        AtomicLong counter = new AtomicLong();

        try (Connection c = getConnection()) {
            for (;;) {
                try (PreparedStatement stmt = c.prepareStatement(
                    "SELECT * FROM locks WHERE v BETWEEN ? AND ? ORDER BY v FOR UPDATE"
                )) {
                    int lo = ThreadLocalRandom.current().nextInt(10);
                    int hi = lo + 1;

                    stmt.setInt(1, lo);
                    stmt.setInt(2, hi);
                    stmt.executeQuery();

                    c.commit();
                    counter.incrementAndGet();
                }
                catch (SQLException e) {
                    c.rollback();
                    // 데드락 예외 처리
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }))
    .forEach(Thread::start);
```

### 올바른 접근 방식: ORDER BY v

잠금이 오름차순으로 획득되면(`ORDER BY v` 사용), 모든 스레드에서 시스템이 합리적인 처리량을 유지합니다:

```
Thread-1:941   Thread-2:966   Thread-3:965   Thread-4:960
Thread-1:1941  Thread-2:1966  Thread-3:1965  Thread-4:1960
Thread-1:2941  Thread-2:2966  Thread-3:2965  Thread-4:2960
```

네 개의 스레드 모두 초당 약 1,000번 이상의 반복을 완료하며, 균형 잡힌 처리량을 보여줍니다.

### 잘못된 접근 방식: ORDER BY DBMS_RANDOM.VALUE

이제 잠금 획득 순서를 무작위로 변경해 봅시다:

```sql
SELECT * FROM locks WHERE v BETWEEN ? AND ? ORDER BY DBMS_RANDOM.VALUE FOR UPDATE
```

`ORDER BY DBMS_RANDOM.VALUE`를 사용하면 즉시 데드락 연쇄가 발생하고, 결국 대부분의 스레드가 종료됩니다:

```
Thread-1:72    Thread-2:79    Thread-3:79    Thread-4:90
Exception: ORA-00060: deadlock detected while waiting for resource
Thread-1:145   Thread-2:152   Thread-3:152   Thread-4:163
Exception: ORA-00060: deadlock detected while waiting for resource
Exception: ORA-00060: deadlock detected while waiting for resource
Thread-1:218   Thread-2:225   Thread-3:225   Thread-4:236
```

결국 하나의 스레드만 살아남아 다른 스레드보다 수천 번 더 많은 반복을 완료합니다. 이는 심각한 성능 저하를 의미합니다.

## 실행 계획 이해하기

올바른 순서로 잠금을 획득할 때의 실행 계획에서는 잠금 획득 전에 정렬이 발생하여 적절한 순서화가 가능합니다. 실행 계획에 의존하지 말고, 항상 명시적으로 `ORDER BY` 절을 사용하여 잠금 순서를 보장하세요.

## 경합 모니터링

데드락 외에도 "enq: TX – row lock contention" 이벤트에 주의해야 합니다. 이는 Oracle의 `v$session` 뷰에서 확인할 수 있으며, 세션이 다른 세션이 보유한 행 잠금을 기다리며 차단되었음을 나타냅니다.

```sql
SELECT * FROM v$session WHERE event = 'enq: TX - row lock contention';
```

심각한 경합은 데드락만큼이나 해로울 수 있으며, 극적인 성능 저하를 유발합니다.

## 실용적 지침

다음은 우선순위 순서로 정리된 권장 사항입니다:

1. 가능하면 비관적 잠금을 완전히 피하세요. 낙관적 잠금이나 다른 동시성 제어 메커니즘을 고려하세요.

2. 필요한 경우 세션당 단일 행 잠금으로 제한하세요. 여러 행을 잠그지 않으면 데드락 가능성이 크게 줄어듭니다.

3. 여러 잠금을 획득해야 한다면 항상 일관된 순서로 획득하세요. 무작위 순서로 잠금을 획득하지 마세요.

4. 프로덕션 시스템을 면밀히 모니터링하세요. 데드락과 경합 이벤트를 추적하여 문제가 심각해지기 전에 대응하세요.

그리고 마지막으로 농담 삼아 하는 규칙: 무슨 일이 일어났는지 보러 출근하는 것을 피하세요.

## 결론

비관적 잠금은 분산 애플리케이션을 동기화하기 위한 유효한 도구이지만, 데드락과 과도한 경합을 방지하기 위해 신중한 구현이 필요합니다. 핵심 메시지는 다음과 같습니다: 항상 일관된 순서로 잠금을 획득하고, 가능하면 비관적 잠금의 사용을 최소화하세요.

Java 8의 스트림과 람다 표현식을 사용하면 이러한 동시성 시나리오를 테스트하는 코드를 간결하고 우아하게 작성할 수 있습니다. jOOλ와 같은 라이브러리는 이러한 작업을 더욱 쉽게 만들어 줍니다.
