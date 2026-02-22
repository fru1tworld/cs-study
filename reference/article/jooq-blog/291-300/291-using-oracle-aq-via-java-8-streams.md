# Java 8 스트림을 통해 Oracle AQ 사용하기

> 원문: https://blog.jooq.org/using-oracle-aq-via-java-8-streams/

게시일: 2016년 2월 8일
저자: lukaseder

## 소개

Oracle 데이터베이스의 가장 훌륭한 기능 중 하나는 Oracle AQ: Oracle Database Advanced Queuing입니다. 이 시스템은 데이터베이스 내에 직접 트랜잭션 메시징 시스템을 구현합니다. 데이터베이스가 중심이 되고 여러 애플리케이션이 이를 접근하는 아키텍처에서는 프로세스 간 통신에 AQ를 사용하는 것이 유익합니다. Java EE 환경에서는 전용 MQ 솔루션을 사용할 수도 있지만, 데이터베이스를 메시징에 활용하면 여러 장점이 있습니다.

## jOOQ로 PL/SQL AQ API 사용하는 방법

메시지 인큐잉과 디큐잉을 위한 PL/SQL API는 간단하며, jOOQ의 `OracleDSL.DBMS_AQ` API를 사용하여 Java에서 접근할 수 있습니다.

### 큐 설정 예제

```sql
CREATE OR REPLACE TYPE message_t AS OBJECT (
  ID         NUMBER(7),
  title      VARCHAR2(100 CHAR)
)
/

BEGIN
  DBMS_AQADM.CREATE_QUEUE_TABLE(
    queue_table => 'message_aq_t',
    queue_payload_type => 'message_t'
  );

  DBMS_AQADM.CREATE_QUEUE(
    queue_name => 'message_q',
    queue_table => 'message_aq_t'
  );

  DBMS_AQADM.START_QUEUE(
    queue_name => 'message_q'
  );
  COMMIT;
END;
/
```

### 생성된 jOOQ 클래스

jOOQ 코드 생성기는 타입 안전한 클래스를 생성합니다:

```java
class Queues {
    static final Queue<MessageTRecord> MESSAGE_Q =
        new QueueImpl<>("NEW_AUTHOR_AQ", MESSAGE_T);
}

class MessageTRecord {
    void setId(Integer id) { ... }
    Integer getId() { ... }
    void setTitle(String title) { ... }
    String getTitle() { ... }
    MessageTRecord(
        Integer id, String title
    ) { ... }
}
```

### 기본 사용법

```java
// jOOQ 설정
Configuration c = ...

// 메시지 인큐
DBMS_AQ.enqueue(c, MESSAGE_Q,
    new MessageTRecord(1, "test"));

// 다시 디큐
MessageTRecord message = DBMS_AQ.dequeue(c, MESSAGE_Q);
```

## Java 8 기능 활용하기

메시지 큐는 무한한 블로킹 메시지 스트림을 나타냅니다. Java 8부터 우리는 이러한 메시지 스트림을 위한 훌륭한 API인 Stream API를 가지고 있습니다. jOOQ 3.8은 AQ API와 Java 8 스트림을 결합하는 통합 기능을 도입합니다:

```java
// jOOQ 설정
Configuration c = ...

DBMS_AQ.dequeueStream(c, MESSAGE_Q)
       .filter(m -> "test".equals(m.getTitle()))
       .forEach(System.out::println);
```

이 파이프라인은 메시지 큐를 리스닝하고, 모든 메시지를 소비하며, "test"를 포함하지 않는 메시지를 필터링하고, 나머지 메시지를 출력합니다.

## 블로킹 스트림

스트림은 블로킹이며 무한합니다. 새 메시지가 도착하지 않는 동안 처리는 단순히 대기하며 블로킹됩니다. 순차 스트림은 이를 자연스럽게 처리하지만, 병렬 스트림은 특별한 고려가 필요합니다. jOOQ 3.8 트랜잭션은 `ForkJoinPool.ManagedBlocker` 내에서 실행됩니다:

```java
static <T> Supplier<T> blocking(Supplier<T> supplier) {
    return new Supplier<T>() {
        volatile T result;

        @Override
        public T get() {
            try {
                ForkJoinPool.managedBlock(new ManagedBlocker() {
                    @Override
                    public boolean block() {
                        result = supplier.get();
                        return true;
                    }

                    @Override
                    public boolean isReleasable() {
                        return result != null;
                    }
                });
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return asyncResult;
        }
    };
}
```

`ManagedBlocker`는 `ForkJoinWorkerThread`에 의해 실행될 때 특별한 코드를 실행하여, 풀에서의 스레드 고갈과 데드락을 방지합니다.

### 병렬 디큐잉

초고속 병렬 AQ 디큐잉을 위해:

```java
// jOOQ 설정. 참조된 ConnectionPool이
// 충분한 커넥션을 가지고 있는지 확인하세요
Configuration c = ...

DBMS_AQ.dequeueStream(c, MESSAGE_Q)
       .parallel()
       .filter(m -> "test".equals(m.getTitle()))
       .forEach(System.out::println);
```

여러 스레드가 메시지를 병렬로 디큐할 것입니다.

## 현재 버전 대안

jOOQ 3.8을 기다리지 않고도, 디큐 작업을 직접 래핑할 수 있습니다:

```java
Stream<MessageTRecord> stream = Stream.generate(() ->
    DSL.using(config).transactionResult(c ->
        dequeue(c, MESSAGE_Q)
    )
);
```

## 보너스: 비동기 디큐잉

큐잉 시스템은 비동기성의 이점을 누립니다. Java 8의 `CompletionStage`(그리고 그 구현체 `CompletableFuture`)는 비동기 알고리즘을 모델링합니다. jOOQ 3.8을 사용하면:

```java
// jOOQ 설정. 참조된 ConnectionPool이
// 충분한 커넥션을 가지고 있는지 확인하세요
Configuration c = ...

CompletionStage<MessageTRecord> stage =
DBMS_AQ.dequeueAsync(c, MESSAGE_Q)
       .thenCompose(m -> ...)
       ...;
```

저자는 "jOOQ 3.8과 Java 8을 사용한 비동기, 블로킹 SQL 문에 대한 더 정교한 사용 사례를 살펴보는 또 다른 글을 jOOQ 블로그에 곧 게시할 예정이니 기대해 주세요."라고 언급했습니다.

## 댓글 섹션

사용자들이 PostgreSQL LISTEN/NOTIFY 지원에 대해 문의했습니다. 저자는 이 기능에 대한 JDBC 지원이 만족스럽지 않다고 설명했습니다. 진정한 블로킹 기능 없이 데이터베이스 폴링이 필요하여 구현이 어렵다는 것입니다.

---

태그: Infinite Stream, Java 8, jooq, Oracle, Oracle AQ, Oracle Streams, streams, Streams API
