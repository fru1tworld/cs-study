# SQL GROUP BY와 함수적 종속성: 매우 유용한 기능

> 원문: https://blog.jooq.org/sql-group-by-and-functional-dependencies-a-very-useful-feature/

관계형 데이터베이스에서는 "함수적 종속성(Functional Dependency)"이라는 용어를 다음과 같이 정의합니다 (위키피디아 인용):

"관계형 데이터베이스 이론에서 함수적 종속성은 데이터베이스의 릴레이션에서 두 속성 집합 간의 제약 조건입니다. 다시 말해, 함수적 종속성은 릴레이션에서 속성들 간의 관계를 설명하는 제약 조건입니다."

SQL에서 함수적 종속성은 유니크 제약 조건(예: 기본 키 제약 조건)이 있을 때마다 나타납니다. 다음과 같은 테이블이 있다고 가정해 봅시다:

```sql
CREATE TABLE actor (
  actor_id BIGINT NOT NULL PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL
);
```

`FIRST_NAME`과 `LAST_NAME` 둘 다 `ACTOR_ID` 컬럼에 대해 함수적 종속성을 가진다고 말할 수 있습니다.

## 좋습니다. 그래서 어쨌다는 건가요?

이것은 단순히 유니크 제약 조건에 적용할 수 있는 수학적 명제만이 아닙니다. SQL에서 매우 유용하게 쓰입니다. 이는 모든 `ACTOR_ID` 값에 대해 (함수적으로 종속된) `FIRST_NAME`과 `LAST_NAME` 값이 단 하나만 존재할 수 있다는 것을 의미합니다. 그 반대는 성립하지 않습니다. 주어진 `FIRST_NAME`과/또는 `LAST_NAME` 값에 대해 여러 개의 `ACTOR_ID` 값이 있을 수 있습니다. 동명이인 배우가 여러 명 있을 수 있기 때문입니다. 주어진 `ACTOR_ID` 값에 대해 대응하는 `FIRST_NAME`과 `LAST_NAME` 값이 단 하나뿐이기 때문에, `GROUP BY` 절에서 이 컬럼들을 생략할 수 있습니다. 다음 테이블도 있다고 가정해 봅시다:

```sql
CREATE TABLE film_actor (
  actor_id BIGINT NOT NULL,
  film_id BIGINT NOT NULL,

  PRIMARY KEY (actor_id, film_id),
  FOREIGN KEY (actor_id) REFERENCES actor (actor_id),
  FOREIGN KEY (film_id) REFERENCES film (film_id)
);
```

이제 배우별 출연 영화 수를 세고 싶다면, 다음과 같이 작성할 수 있습니다:

```sql
SELECT
  actor_id, first_name, last_name, COUNT(*)
FROM actor
JOIN film_actor USING (actor_id)
GROUP BY actor_id
ORDER BY COUNT(*) DESC
```

이것은 타이핑을 많이 줄여주기 때문에 매우 유용합니다. 사실, `GROUP BY`의 의미론이 정의된 방식에 따르면, `SELECT` 절에 다음 중 하나에 해당하는 모든 종류의 컬럼 참조를 넣을 수 있습니다:

- `GROUP BY` 절에 나타나는 컬럼 표현식
- `GROUP BY` 절의 컬럼 표현식 집합에 함수적으로 종속된 컬럼 표현식
- 집계 함수

## 안타깝게도, 모든 데이터베이스가 이를 지원하지는 않습니다

예를 들어 Oracle을 사용하고 있다면, 위의 기능을 활용할 수 없습니다. `SELECT` 절에 나타나는 모든 비집계 컬럼 표현식이 `GROUP BY` 절에도 반드시 나타나야 하는 전통적인 동등한 버전을 작성해야 합니다:

```sql
SELECT
  actor_id, first_name, last_name, COUNT(*)
FROM actor
JOIN film_actor USING (actor_id)
GROUP BY actor_id, first_name, last_name
--                 ^^^^^^^^^^  ^^^^^^^^^ 불필요함
ORDER BY COUNT(*) DESC
```

## 추가 읽을거리:

- 함수적 종속성에 관한 위키피디아 문서
- SQL GROUP BY가 어떻게 설계되었어야 했는가 – Neo4j의 암시적 GROUP BY처럼
- SQL의 GROUP BY와 HAVING 절을 정말로 이해하고 있나요?
- SQL GROUP BY와 집계를 Java 8로 변환하는 방법
- GROUP BY ROLLUP / CUBE
