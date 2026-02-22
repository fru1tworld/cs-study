# SQL 행별 업데이트, 배치 업데이트, 벌크 업데이트 간의 성능 차이

> 원문: https://blog.jooq.org/the-performance-difference-between-sql-row-by-row-updating-batch-updating-and-bulk-updating/

최근 작성한 블로그 게시물의 댓글에서 이 주제가 다시 떠올랐기 때문에 간단한 벤치마크를 실행해보기로 했습니다. 데이터베이스에서 데이터를 업데이트하는 방법에는 크게 세 가지가 있습니다:

1. 행별 업데이트(Row-by-row updating): 한 번에 하나의 행만 업데이트합니다. "행별(row-by-row)"은 "느림별(slow-by-slow)"과 운율이 맞습니다(힌트 힌트).
2. 배치 업데이트(Batch updating): 여러 행을 한꺼번에 모아서 배치로 업데이트합니다.
3. 벌크 업데이트(Bulk updating): 단일 SQL 문으로 한 번에 모든 행을 업데이트합니다.

이 세 가지 접근 방식의 성능 차이를 벤치마크해 보겠습니다.

## 벤치마크 설정

다음과 같은 테이블 구조를 사용합니다:

```sql
CREATE TABLE post (
  id INT NOT NULL PRIMARY KEY,
  text VARCHAR2(1000) NOT NULL,
  archived NUMBER(1) NOT NULL CHECK (archived IN (0, 1)),
  creation_date DATE NOT NULL
);

CREATE INDEX post_creation_date_i ON post (creation_date);
```

벤치마크에서는 10,000개의 행을 삽입하고, 2017년 1월부터 시작하는 생성 날짜를 가진 데이터를 사용합니다. 이 중 2018년 1월 1일 이전의 레코드(약 3,649개 행)를 보관 처리(archived)하는 업데이트를 수행합니다.

## PL/SQL 벤치마크

먼저 PL/SQL에서 세 가지 접근 방식을 비교해 보겠습니다.

### 문장 1: 벌크 업데이트 (가장 빠름)

```sql
UPDATE post
SET archived = 1
WHERE archived = 0 AND creation_date < DATE '2018-01-01';
```

### 문장 2: FORALL을 사용한 업데이트 (두 번째)

```sql
DECLARE
  TYPE post_ids_t IS TABLE OF post.id%TYPE;
  v_post_ids post_ids_t;
BEGIN
  SELECT id
  BULK COLLECT INTO v_post_ids
  FROM post
  WHERE archived = 0
  AND creation_date < DATE '2018-01-01';

  FORALL i IN 1 .. v_post_ids.count
    UPDATE post SET archived = 1 WHERE id = v_post_ids(i);
END;
```

### 문장 3: 행별 루프를 사용한 업데이트 (가장 느림)

```sql
BEGIN
  FOR rec IN (
    SELECT id
    FROM post
    WHERE archived = 0
    AND creation_date < DATE '2018-01-01'
  ) LOOP
    UPDATE post SET archived = 1 WHERE id = rec.id;
  END LOOP;
END;
```

### PL/SQL 벤치마크 결과

5번의 반복 실행 후 평균을 계산한 결과:

| 접근 방식 | 평균 실행 시간 |
|-----------|---------------|
| 문장 1 (벌크 업데이트) | 0.0098초 |
| 문장 2 (FORALL) | 0.01291초 |
| 문장 3 (행별 루프) | 0.02519초 |

문장 1과 문장 3 사이의 차이는 2.5배입니다!

## 왜 이런 차이가 발생할까요?

PL/SQL에서 행별 처리가 느린 이유는 두 가지입니다:

1. 엔진 전환 오버헤드: PL/SQL 엔진과 SQL 엔진 사이를 수천 번 왔다 갔다 해야 합니다.
2. 알고리즘 복잡도 변화: post 테이블을 한 번만 O(N) 시간에 순회하는 대신, 개별 ID 값을 O(log N) 시간에 N번 조회하게 됩니다. 즉, O(N)이 O(N log N)으로 증가합니다.

FORALL 구문은 배치 처리를 통해 엔진 전환 오버헤드를 줄이지만, 여전히 개별 ID 조회로 인한 O(N log N) 복잡도는 피할 수 없습니다.

## Java/JDBC 벤치마크

이제 Java에서 동일한 벤치마크를 실행해 보겠습니다. 여기서는 네트워크 지연 시간이 추가되어 성능 차이가 더욱 극대화됩니다.

### 문장 1: 행별 업데이트 (매번 새 PreparedStatement 생성)

```java
try (Statement s = c.createStatement();
     ResultSet rs = s.executeQuery(
         "SELECT id FROM post WHERE archived = 0 " +
         "AND creation_date < DATE '2018-01-01'")) {
    while (rs.next()) {
        try (PreparedStatement u = c.prepareStatement(
                "UPDATE post SET archived = 1 WHERE id = ?")) {
            u.setInt(1, rs.getInt(1));
            u.executeUpdate();
        }
    }
}
```

### 문장 2: 행별 업데이트 (PreparedStatement 캐싱)

```java
try (Statement s = c.createStatement();
     ResultSet rs = s.executeQuery(
         "SELECT id FROM post WHERE archived = 0 " +
         "AND creation_date < DATE '2018-01-01'");
     PreparedStatement u = c.prepareStatement(
         "UPDATE post SET archived = 1 WHERE id = ?")) {
    while (rs.next()) {
        u.setInt(1, rs.getInt(1));
        u.executeUpdate();
    }
}
```

### 문장 3: 배치 업데이트

```java
try (Statement s = c.createStatement();
     ResultSet rs = s.executeQuery(
         "SELECT id FROM post WHERE archived = 0 " +
         "AND creation_date < DATE '2018-01-01'");
     PreparedStatement u = c.prepareStatement(
         "UPDATE post SET archived = 1 WHERE id = ?")) {
    while (rs.next()) {
        u.setInt(1, rs.getInt(1));
        u.addBatch();
    }
    u.executeBatch();
}
```

### 문장 4: 벌크 업데이트

```java
try (Statement s = c.createStatement()) {
    s.executeUpdate(
        "UPDATE post\n" +
        "SET archived = 1\n" +
        "WHERE archived = 0\n" +
        "AND creation_date < DATE '2018-01-01'");
}
```

### Java/JDBC 벤치마크 결과

5번의 반복 실행 결과:

| 실행 | 문장 1 (새 PS) | 문장 2 (캐시된 PS) | 문장 3 (배치) | 문장 4 (벌크) |
|------|---------------|-------------------|--------------|--------------|
| Run 1 | PT3.480S | PT2.948S | PT0.113S | PT0.025S |
| Run 2 | PT3.379S | PT3.522S | PT0.144S | PT0.030S |
| Run 3 | PT3.445S | PT3.135S | PT0.116S | PT0.025S |
| Run 4 | PT3.711S | PT3.495S | PT0.122S | PT0.026S |
| Run 5 | PT3.502S | PT3.527S | PT0.119S | PT0.029S |

문장 1과 문장 4 사이의 차이는 무려 100배입니다!!

## Java에서 성능 차이가 더 큰 이유

Java에서 성능 차이가 PL/SQL보다 훨씬 더 극명한 이유는 네트워크 왕복(network roundtrip) 때문입니다.

- 행별 업데이트: 각 UPDATE 문마다 Java 애플리케이션에서 데이터베이스 서버로 네트워크 왕복이 발생합니다. 3,649개의 행을 업데이트하면 3,649번의 네트워크 왕복이 필요합니다.
- 배치 업데이트: 모든 UPDATE 문을 하나의 배치로 모아서 한 번의 네트워크 왕복으로 전송합니다.
- 벌크 업데이트: 단일 SQL 문 하나만 전송하므로 네트워크 왕복이 한 번뿐입니다.

## 결론 및 권장 사항

1. 항상 벌크 업데이트를 선호하세요: 가능하다면 단일 SQL 문으로 모든 데이터를 업데이트하세요. 이것이 가장 빠른 방법입니다.

2. 벌크가 불가능하면 배치를 사용하세요: SQL의 제약으로 인해 벌크 업데이트가 불가능한 경우, JDBC의 `addBatch()`와 `executeBatch()`를 사용한 배치 업데이트를 사용하세요.

3. 익명 블록을 고려하세요: 여러 문장을 그룹화하여 단일 전송으로 보낼 수 있습니다.

4. ORM 동작을 모니터링하세요: ORM은 자동 배칭을 도와줄 수 있지만, 벌크 연산으로 최적화하는 것은 대부분 불가능합니다.

행별(slow-by-slow) 연산을 중단하고, 단일 SQL 문으로 벌크 연산을 실행할 수 있을 때는 반드시 그렇게 하세요!

## 동시성에 대한 참고 사항

이 분석은 비동시성(non-concurrent) 시나리오에만 적용됩니다. 동시 업데이트는 잠금 전략, 트랜잭션 격리 수준, 데이터베이스별 구현에 따른 복잡성을 도입하므로 별도의 분석이 필요합니다.
