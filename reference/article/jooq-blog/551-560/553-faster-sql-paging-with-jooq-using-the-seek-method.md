# jOOQ에서 Seek 메서드를 사용한 더 빠른 SQL 페이징

> 원문: https://blog.jooq.org/faster-sql-paging-with-jooq-using-the-seek-method/

## 개요

이 블로그 글은 "Seek 메서드"(또는 "keyset 페이징"이라고도 함)를 설명합니다. 이는 대용량 데이터셋에서 전통적인 OFFSET 기반 페이징보다 뛰어난 성능을 보이는 대안적인 페이징 기법입니다.

## OFFSET의 문제점

전통적인 오프셋 페이징은 결과를 반환하기 전에 데이터베이스가 이전의 모든 레코드를 건너뛰고 카운트해야 합니다. 단순한 쿼리는 적절하게 수행되지만:

```sql
SELECT first_name, last_name, score
FROM players
WHERE game_id = 42
ORDER BY score DESC
LIMIT 10;
```

더 높은 페이지 번호로 페이징할수록 점점 더 느려집니다:

```sql
SELECT first_name, last_name, score
FROM players
WHERE game_id = 42
ORDER BY score DESC
LIMIT 10
OFFSET 100000;
```

## Seek 메서드의 작동 방식

Seek 메서드는 레코드를 건너뛰는 대신, "seek 조건자"를 사용하여 마지막으로 조회한 행 이후의 레코드를 가져옵니다. 예를 들어, `OFFSET 10`을 사용하는 대신, 결과가 마지막 플레이어의 점수 이후부터 시작해야 한다고 지정합니다:

```sql
SELECT first_name, last_name, score
FROM players
WHERE game_id = 42
AND score < 949
ORDER BY score DESC
LIMIT 10;
```

중복된 점수 값을 처리하기 위해, 조건자에 유니크 컬럼을 포함시킵니다:

```sql
SELECT player_id, first_name, last_name, score
FROM players
WHERE game_id = 42
AND (score, player_id) < (949, 15)
ORDER BY score DESC, player_id DESC
LIMIT 10;
```

## 주요 장점

- 성능: 전체 테이블 순회 대신 인덱스 범위 스캔을 통해 페이지 깊이와 관계없이 일정한 시간 내에 실행됩니다
- 안정성: "안정적인" 페이징 - 삽입이나 삭제가 발생해도 페이지 간에 중복 레코드가 발생하지 않습니다
- 확장성: 벤치마크 데이터에 따르면 높은 페이지 번호에서 극적으로 개선된 성능을 보여줍니다

## jOOQ 3.3 구현

jOOQ는 `SEEK` 절을 통해 네이티브 구문 지원을 제공합니다:

```java
DSL.using(configuration)
   .select(PLAYERS.PLAYER_ID, PLAYERS.FIRST_NAME,
           PLAYERS.LAST_NAME, PLAYERS.SCORE)
   .from(PLAYERS)
   .where(PLAYERS.GAME_ID.eq(42))
   .orderBy(PLAYERS.SCORE.desc(), PLAYERS.PLAYER_ID.asc())
   .seek(949, 15)
   .limit(10)
   .fetch();
```

이 접근 방식은 비교 조건자를 수동으로 구성하는 것보다 페이징 로직을 더 읽기 쉽게 만들면서 타입 안전성을 제공합니다.
