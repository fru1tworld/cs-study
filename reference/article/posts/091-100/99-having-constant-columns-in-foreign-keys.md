# 외래 키에 "상수" 컬럼 사용하기

> 원문: https://blog.jooq.org/having-constant-columns-in-foreign-keys/

방금 트위터에서 아주 흥미로운 질문을 받았습니다:

> "@lukaseder 간단한 질문: PostgreSQL에서 복합 외래 키 중 하나의 값이 상수인 경우가 가능한가요... 아니면 테이블에 그 상수를 저장해야 하나요? constraint foreign key (foo_id, 'bar_subtype') references foo(foo_id,foo_type) ?"
>
> — Look! The Emperor is NAKED V⃝ (@connolly_s) [2020년 9월 10일]

(PostgreSQL) 테이블에서 "상수" 외래 키 컬럼을 가질 수 있을까요? 다행히도 가능합니다. "계산 컬럼(computed columns)" 또는 "생성 컬럼(generated columns)"이라는 훌륭한 표준 기능을 사용하면 됩니다. 때로는 어떤 이유로든 스키마를 완전히 정규화할 수 없는 경우가 있습니다. 다음과 같은 복합 기본 키를 가진 테이블이 있는 경우가 있을 수 있습니다:

```sql
CREATE TABLE t1 (
  a int,
  b int,
  t1 int,
  PRIMARY KEY (a, b)
)
```

그리고 참조하는 테이블 t2에서는 기본 키 컬럼 중 하나를 항상 특정 값, 예를 들어 1로 참조하게 됩니다. 물론, b = 1을 보장하는 CHECK 제약 조건을 가진 테이블 t2를 만들 수 있습니다:

```sql
CREATE TABLE t2 (
  a int,
  b int NOT NULL DEFAULT 1 CHECK (b = 1),
  t2 int,
  FOREIGN KEY (a, b) REFERENCES t1
)
```

하지만 왜 생성 컬럼을 사용하지 않을까요?

```sql
CREATE TABLE t2 (
  a int,
  b int GENERATED ALWAYS AS (1) STORED,
  t2 int,
  FOREIGN KEY (a, b) REFERENCES t1
)
```

제 생각에는 이 방식이 훨씬 더 강력합니다. PostgreSQL 12 기준으로는 STORED만 지원됩니다(값이 디스크에 저장된다는 의미). 이 경우에는 VIRTUAL이 더 나을 수 있습니다(행을 읽을 때만 값이 생성된다는 의미). 테스트 데이터를 삽입해 보겠습니다:

```sql
INSERT INTO t1 (a, b, t1)
VALUES(1, 1, 1), (1, 2, 2), (2, 1, 3);

INSERT INTO t2 (a, t2)
VALUES (1, 11), (2, 12);

SELECT *
FROM t1
NATURAL LEFT JOIN t2
```

예상한 결과가 나옵니다. t2에는 (b = 1)인 값만 삽입할 수 있습니다:

| a | b | t1 | t2 |
|---|---|----|-----|
| 1 | 1 | 1  | 11  |
| 2 | 1 | 3  | 12  |
| 1 | 2 | 2  |     |

기억해 두면 좋은 멋진 기법입니다. 계산 컬럼 또는 생성 컬럼은 다양한 RDBMS에서 사용 가능하며, 최소한 다음을 포함합니다:

* Db2
* MySQL
* Oracle
* PostgreSQL
* SQL Server
