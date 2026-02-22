# JDBC 배치에 대해 몰랐던 것

> 원문: https://blog.jooq.org/what-you-didnt-know-about-jdbc-batch/

이전 블로그 포스트인 "Java 개발자가 SQL을 작성할 때 저지르는 10가지 흔한 실수"에서, 대용량 데이터셋을 삽입할 때 배치 처리의 중요성을 강조했습니다. 대부분의 데이터베이스와 JDBC 드라이버는 단일 prepared statement를 배치 모드로 실행할 때 상당한 성능 향상을 제공합니다.

## JDBC 배치 코드 예제

다음은 raw JDBC를 사용하여 네 명의 저자 레코드를 삽입하는 방법입니다:

```java
PreparedStatement s = connection.prepareStatement(
    "INSERT INTO author(id, first_name, last_name)"
  + "  VALUES (?, ?, ?)");

s.setInt(1, 1);
s.setString(2, "Erich");
s.setString(3, "Gamma");
s.addBatch();

s.setInt(1, 2);
s.setString(2, "Richard");
s.setString(3, "Helm");
s.addBatch();

s.setInt(1, 3);
s.setString(2, "Ralph");
s.setString(3, "Johnson");
s.addBatch();

s.setInt(1, 4);
s.setString(2, "John");
s.setString(3, "Vlissides");
s.addBatch();

int[] result = s.executeBatch();
```

## jOOQ 대안

jOOQ 라이브러리를 사용하면 fluent API와 `.bind()` 메서드 호출로 배치 작업을 단순화할 수 있습니다:

```java
create.batch(
        insertInto(AUTHOR, ID, FIRST_NAME, LAST_NAME)
       .values((Integer) null, null, null))
      .bind(1, "Erich", "Gamma")
      .bind(2, "Richard", "Helm")
      .bind(3, "Ralph", "Johnson")
      .bind(4, "John", "Vlissides")
      .execute();
```

## 핵심 인사이트

여러분이 아마 몰랐을 것은 성능 향상이 실제로 얼마나 극적인지, 그리고 MySQL의 JDBC 드라이버는 실제로 배치를 제대로 지원하지 않는 반면, Derby, H2, HSQLDB는 배치로부터 실질적인 이점을 얻지 못하는 것처럼 보인다는 사실입니다.

## 벤치마크 결과

다음은 `INSERT` 문에 대해 각 데이터베이스를 자체적으로 비교했을 때의 성능 향상을 보여주는 표입니다:

| 데이터베이스 | 배치 처리 시 성능 향상 |
|----------|-------------------------------|
| DB2 | 503% |
| Derby | 7% |
| H2 | 20% |
| HSQLDB | 25% |
| MySQL | 5% |
| MySQL (rewriteBatchedStatements=true 사용 시) | 332% |
| Oracle | 503% |
| PostgreSQL | 325% |
| SQL Server | 325% |

## 결론

실제 결과와 관계없이, 벤치마크에서 사용된 데이터셋 크기에 대해 배치 처리가 배치를 사용하지 않는 것보다 나쁜 경우는 없다고 말할 수 있습니다.

더 자세한 해석과 UPDATE 문 결과에 대해서는 James Sutherland의 Java Persistence Performance 블로그에 있는 상세한 벤치마크를 참조하세요.
