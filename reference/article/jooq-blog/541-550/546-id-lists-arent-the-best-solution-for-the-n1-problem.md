# ID 리스트는 N+1 문제의 최적 해결책이 아니다

> 원문: https://blog.jooq.org/id-lists-arent-the-best-solution-for-the-n1-problem/

## 개요

이 블로그 글에서는 IN 조건절과 함께 ID 리스트를 사용하는 방식이 N+1 문제보다는 낫지만, 반드시 최적의 데이터베이스 쿼리 전략은 아닌 이유를 살펴봅니다.

## N+1 문제란?

N+1 문제는 ORM이 하나의 효율적인 쿼리 대신 많은 개별 쿼리를 실행할 때 발생하는 전형적인 문제입니다. Hibernate와 같은 ORM에서 엔티티가 지연 로딩(lazy fetching)으로 설정되어 있을 때 이러한 쿼리 집합이 종종 생성됩니다.

```sql
SELECT id, name FROM albums
SELECT id, name FROM songs WHERE album_id = 1
SELECT id, name FROM songs WHERE album_id = 2
[... 각 앨범에 대해 반복 ...]
```

일반적인 "해결책"은 이러한 쿼리들을 IN 조건절을 사용한 단일 쿼리로 통합하는 것입니다:

```sql
SELECT id, title, filename FROM songs
WHERE album_id IN (1, 2, 3, 4, 5)
```

이렇게 하면 쿼리 수가 N+1에서 단 2개로 줄어들지만, 새로운 문제들이 발생합니다.

## IN 리스트 해결책의 문제점

### IN 리스트 크기 제한

데이터베이스마다 최대 리스트 길이를 제한합니다:

- Oracle: 1,000개 요소
- Sybase ASE: 2,000개 요소
- SQL Server 2008 R2: 2,100개 요소
- SQLite: 총 999개의 바인드 값

### 변수 바인딩 성능 문제

일부 JDBC 드라이버에서는 변수 바인딩에 상당한 작업이 수반됩니다. 대규모 바인드 값 집합(10,000개 이상)은 애플리케이션과 데이터베이스 간의 전송 오버헤드를 발생시킵니다.

### 커서 캐시 미스

각각의 고유한 IN 리스트 크기는 새로운 SQL 문을 생성하여 데이터베이스 커서 캐시 재사용을 방해합니다. Oracle과 같은 정교한 데이터베이스는 커서 캐시를 유지하여 실행 계획을 재사용합니다. 동일한 SQL 문의 후속 실행은 이미 수행된 비용이 많이 드는 실행 계획 계산의 이점을 누릴 수 있습니다. 그러나 IN 리스트 크기가 달라지면 각 쿼리가 데이터베이스에서 새로운 커서와 새로운 실행 계획을 생성하게 됩니다.

## 더 나은 대안들

### 1. JOIN을 통한 명시적 즉시 로딩 (Explicit Eager Fetching)

```sql
SELECT a.id a_id, a.name a_name, s.id s_id, s.name s_name
FROM albums a
JOIN songs s ON s.album_id = a.id
```

약간 더 많은 데이터 전송을 대가로 단일 쿼리 실행의 이점을 얻습니다.

### 2. 원본 쿼리를 사용한 세미 조인 (Semi-Join)

```sql
SELECT id, name FROM songs
WHERE album_id IN (SELECT id FROM albums)
```

또는 EXISTS를 사용:

```sql
SELECT s.id, s.name FROM songs s
WHERE EXISTS (SELECT 1 FROM albums a WHERE a.id = s.album_id)
```

### 3. 배열 바인드 변수 (데이터베이스별)

Oracle 문법:

```sql
SELECT id, name FROM songs
WHERE album_id IN (SELECT * FROM TABLE(?))
```

이 방식은 Oracle의 `numbers AS TABLE OF NUMBER(38)`와 같은 미리 정의된 배열 타입이 필요합니다.

### 4. 이산 크기 IN 리스트 (Discrete-Sized IN Lists)

가변 길이 리스트를 미리 정해진 크기(2, 3, 5, 8, 13)로 채웁니다:

```sql
-- 4개의 ID를 크기 5로 패딩
album_id IN (1, 2, 3, 4, 4)
```

이렇게 하면 가변 데이터를 수용하면서도 커서 캐시 일관성을 유지할 수 있습니다.

## 결론

상당한 양의 데이터가 데이터 처리에 관여하게 되면, 일반적인 ORM 모델로는 더 이상 충분하지 않을 수 있습니다. jOOQ와 같은 도구를 사용한 명시적 SQL 구성은 ORM 기본값에 의존하는 것보다 더 나은 최적화를 제공합니다. SQL을 다시 제어하세요.
