# Java에서 Oracle AQ를 사용하는 것이 이보다 쉬울 수 없다

> 원문: https://blog.jooq.org/using-oracle-aq-in-java-wont-get-any-easier-than-this/

jOOQ의 멋진 점 중 하나는 코드 생성기가 여러분의 전체 데이터베이스 스키마를 사용 가능한 Java 코드로 변환한다는 것입니다. 여기에는 테이블, 시퀀스, 사용자 정의 타입 등이 포함됩니다.

jOOQ 3.5 버전부터는 이제 네이티브 Oracle AQ(Advanced Queuing) 지원도 포함됩니다!

다음 스키마를 고려해 보세요:

```sql
CREATE OR REPLACE TYPE book_t AS OBJECT (
  ID NUMBER(7),
  title VARCHAR2(100 CHAR),
  language VARCHAR2(2 CHAR)
)
/

CREATE OR REPLACE TYPE books_t AS VARRAY(32) OF book_t
/

CREATE OR REPLACE TYPE author_t AS OBJECT (
  ID NUMBER(7),
  first_name VARCHAR2(100 CHAR),
  last_name VARCHAR2(100 CHAR),
  books books_t
)
/
```

이제 위의 author_t 타입을 기반으로 새로운 큐를 생성해 보겠습니다:

```sql
BEGIN
  DBMS_AQADM.CREATE_QUEUE_TABLE(
    queue_table        => 'new_author_aq_t',
    queue_payload_type => 'author_t'
  );

  DBMS_AQADM.CREATE_QUEUE(
    queue_name  => 'new_author_aq',
    queue_table => 'new_author_aq_t'
  );

  DBMS_AQADM.START_QUEUE(
    queue_name => 'new_author_aq'
  );
END;
/
```

... 그리고 jOOQ가 마법을 부리도록 하세요. jOOQ의 코드 생성기는 이제 생성된 타입 객체(`author_t`)와 자동으로 연결된 `Queues.java` 파일을 생성합니다:

```java
public class Queues {

    public static final Queue<AuthorT> NEW_AUTHOR_AQ =
        new QueueImpl<AuthorT>("NEW_AUTHOR_AQ", SP, AUTHOR_T);
}
```

큐가 멋진 이유는 데이터베이스를 통해 OBJECT 타입을 전송할 수 있기 때문입니다. RAW 타입으로 직렬화하는 대신에 말이죠. 이 접근 방식은 정말 훌륭합니다. 왜냐하면 jOOQ가 이미 Oracle OBJECT, VARRAY, TABLE 타입을 Java로 또는 그 반대로 변환하는 복잡한 JDBC OracleData 직렬화를 구현했기 때문입니다. 이제 그 기능을 무료로 사용하여 Oracle AQ와도 통합할 수 있습니다!

큐에 데이터를 넣는(Enqueue) 것이 이보다 쉬울 수 없습니다:

```java
// AuthorT 객체를 생성하고, 중첩된 BooksT 컬렉션과 BookT 객체를 포함
AuthorT author = new AuthorT(
    1,
    "George",
    "Orwell",
    new BooksT(
        new BookT(1, "1984", "en"),
        new BookT(2, "Animal Farm", "en")
    )
);

// 큐에 객체를 넣음
DBMS_AQ.enqueue(configuration, NEW_AUTHOR_AQ, author);
```

그리고 큐에서 데이터를 꺼내는(Dequeue) 것도 마찬가지로 쉽습니다:

```java
// 큐에서 다음 메시지를 꺼냄 (블로킹 연산)
AuthorT author = DBMS_AQ.dequeue(configuration, NEW_AUTHOR_AQ);
```

Java 8과 함께, 우리는 jOOQ의 새로운 API를 활용하여 큐를 Java 8 Stream으로 표현할 수 있습니다:

```java
// 큐에서 10개의 메시지를 스트림으로 처리
stream(configuration, NEW_AUTHOR_AQ)
    .limit(10)
    .forEach(author -> {
        System.out.println(
            author.getFirstName() + " " + author.getLastName()
        );
    });
```

## 트랜잭션 동작

이 기능의 훌륭한 점 중 하나는 트랜잭션 동작입니다. Oracle AQ는 비관적 잠금(pessimistic locking) 메커니즘을 사용합니다. 즉, 트랜잭션이 실패하면 메시지가 자동으로 큐에 반환됩니다. 이를 통해 메시지 처리의 원자성을 보장할 수 있습니다.

## 결론

jOOQ 3.5의 Oracle AQ 지원을 통해, Java에서 Oracle Advanced Queuing을 사용하는 것이 크게 단순화되었습니다. JDBC나 Oracle의 네이티브 API를 통해 작업하는 것의 복잡성은 이제 과거의 일이 되었습니다. jOOQ의 코드 생성기가 자동으로 AQ 객체의 Java 표현을 생성하므로, 개발자는 비즈니스 로직에 집중할 수 있습니다.
