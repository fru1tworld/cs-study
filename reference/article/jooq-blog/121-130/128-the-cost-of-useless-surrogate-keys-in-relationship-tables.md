# 관계 테이블에서 불필요한 대리 키의 비용

> 원문: https://blog.jooq.org/the-cost-of-useless-surrogate-keys-in-relationship-tables/

자연 키(natural key)를 식별하는 것은 어려운 일입니다. 이미 SUPPLIER라는 이름을 가진 공급업체가 있는 상황에서 다른 공급업체가 같은 이름으로 새로 나타나면 어떻게 될까요? 주민등록번호는 (일반적으로) 자연 키가 될 수 있지만, 이를 공유하면 안 된다는 것을 깨달았을 때는 어떻게 할까요? 경우에 따라 ISO 표준 코드와 같이 분명한 후보가 있습니다. 예를 들어 [ISO 639 언어 코드](https://ko.wikipedia.org/wiki/ISO_639), [ISO 3166 국가 코드](https://ko.wikipedia.org/wiki/ISO_3166), [ISO 4217 통화 코드](https://ko.wikipedia.org/wiki/ISO_4217) 등이 있습니다.

하지만 대부분의 스키마 설계에서는 [자연 키와 대리 키(surrogate key)](https://en.wikipedia.org/wiki/Surrogate_key) 논쟁에서 물러나 대리 키를 적용하는 것이 합리적입니다. 키 변경은 어렵고, 우리 모두는 키가 절대 변경되지 않을 것이라고 생각하지만 실제로는 어느 시점에서 변경이 일어나고 맙니다. 그래서 변경되지 않을 것이 확실한 대리 키를 만들어 두면, 자연 키가 변경될 때 키 변경에 신경 쓸 필요가 없습니다. 다른 사람의 데이터를 기반으로 식별자를 구축하면 안 됩니다.

## 관계 테이블(Relationship Tables)

하지만 관계 테이블(또는 연결 테이블, 조인 테이블, N:M 테이블 등 여러 이름으로 불리는)은 어떨까요? Sakila 데이터베이스의 FILM_ACTOR 테이블을 예로 들어보겠습니다. 이 테이블은 FILM과 ACTOR 사이의 N:M 관계를 모델링합니다.

```sql
CREATE TABLE film_actor (
  actor_id int NOT NULL REFERENCES actor,
  film_id int NOT NULL REFERENCES film,
  CONSTRAINT film_actor_pkey PRIMARY KEY (actor_id, film_id)
);
```

이 테이블의 개별 행에 `FILM_ACTOR_ID`나 `ID`와 같은 또 다른 열을 추가하는 것은 실제로 의미가 없습니다. 이 테이블은 명확한 자연 키 후보가 있습니다. 바로 ACTOR와 FILM에 대한 외래 키의 집합입니다. 이러한 외래 키 자체가 대리 키(만약 그렇다면)입니다. 하지만 두 개의 외래 키 조합은 "관계"를 식별하는 데 완벽하게 적합한 자연 키입니다. 여기서 변경은 절대 일어날 수 없습니다. 일어난다면 그것은 완전히 새로운 관계입니다!

그래서 자연 키 열(들)이 기본 키가 되어야 합니다.

## 클러스터형 인덱스(Clustered Indexes)

이 토론에서 중요한 것은 데이터베이스가 데이터를 저장하는 방식입니다. [클러스터형 인덱스](https://use-the-index-luke.com/sql/clustering/index-organized-clustered-index)와 [힙 테이블](https://use-the-index-luke.com/sql/clustering)의 차이점은 인덱스에 저장되는 값에 있습니다.

### 클러스터형 인덱스

인덱스 열 값은 트리 구조에 포함됩니다. 다른 모든 열 값은 (테이블에 분산되지 않고) 리프 노드에 포함됩니다.

- 기본 키 검색에서 쿼리 열이 인덱스의 일부가 아닐 때: O(log N)
- 기본 키 검색에서 쿼리 열이 인덱스의 일부일 때: O(log N) (동일)
- 기본 키가 아닌 키 검색에서 쿼리 열이 보조 인덱스의 일부가 아닐 때: O(log N) + O(M log N)
- 기본 키가 아닌 키 검색에서 쿼리 열이 보조 인덱스의 일부일 때: O(log N) (동일)

### 비클러스터형 인덱스(힙 테이블)

인덱스는 테이블 외부에 있습니다. 모든 검색은 동일하게 빠릅니다(또는 느립니다).

- 기본 키 검색에서 쿼리 열이 인덱스의 일부가 아닐 때: O(log N) + O(1)
- 기본 키 검색에서 쿼리 열이 인덱스의 일부일 때: O(log N) (동일)
- 기본 키가 아닌 키 검색에서 쿼리 열이 보조 인덱스의 일부가 아닐 때: O(log N) + O(M)
- 기본 키가 아닌 키 검색에서 쿼리 열이 보조 인덱스의 일부일 때: O(log N) (동일)

### RDBMS 기본 동작

| RDBMS | 기본 설정 |
|-------|----------|
| MySQL InnoDB | 클러스터형 인덱스만 지원 |
| MySQL MyISAM | 힙 테이블만 지원 |
| Oracle | 둘 다 지원, 힙 테이블이 기본값 |
| PostgreSQL | 둘 다 지원, 힙 테이블이 기본값 |
| SQL Server | 둘 다 지원, 클러스터형 인덱스가 기본값 |

## 벤치마크

이 벤치마크에서 저는 다음과 같은 스키마를 사용하겠습니다:

```sql
create table parent_1 (id int not null primary key);
create table parent_2 (id int not null primary key);

-- 대리 키를 가진 자식 테이블
create table child_surrogate (
  id int auto_increment,
  parent_1_id int not null references parent_1,
  parent_2_id int not null references parent_2,
  payload_1 int,
  payload_2 int,
  primary key (id),
  unique (parent_1_id, parent_2_id)
);

-- 자연(복합) 키를 가진 자식 테이블
create table child_natural (
  parent_1_id int not null references parent_1,
  parent_2_id int not null references parent_2,
  payload_1 int,
  payload_2 int,
  primary key (parent_1_id, parent_2_id)
);
```

다시 한번 말씀드리지만, `CHILD_SURROGATE`의 경우 대리 키 `ID`는 완전히 무의미합니다. 엔티티가 아닌 관계를 모델링하고 있기 때문입니다. `(PARENT_1_ID, PARENT_2_ID)`의 조합이 관계를 완벽하게 정의하며, 이것이 자연스러운 후보 키입니다. 하지만 여러 테이블에 일관되게 `ID`를 적용하고 싶어서 여기에도 적용합니다. 결국 우리의 코드에서 이 테이블을 계속 참조하게 될 것이고, 나중에 관계가 추가 열을 가진 엔티티로 발전할 수도 있으니까요.

그리고 만약 여러분이 JPA 사용자라면, "composite keys with JPA/Hibernate are such a pain"이라는 말을 듣거나 직접 느꼈을 것입니다. 그 말이 사실일 수 있지만, 저는 여러분이 JPA를 선택한 것에 동의하지는 않습니다. 어쨌든, 이 글의 주제를 벗어나므로...

저는 다음과 같은 데이터를 삽입했습니다:

- `PARENT_1`에 10,000개의 행
- `PARENT_2`에 100개의 행
- `CHILD_SURROGATE`에 1,000,000개의 행
- `CHILD_NATURAL`에 1,000,000개의 행

두 CHILD 테이블 모두 각 PARENT_1 행당 100개의 자식 행을 가지게 됩니다.

그리고 다음 두 쿼리를 번갈아 가며 여러 번 실행합니다:

```sql
-- 대리 키 테이블 조인
SELECT c.payload_1 + c.payload_2 AS a
FROM parent_1 AS p1
JOIN child_surrogate AS c ON p1.id = c.parent_1_id
WHERE p1.id = 4;

-- 자연 키 테이블 조인
SELECT c.payload_1 + c.payload_2 AS a
FROM parent_1 AS p1
JOIN child_natural AS c ON p1.id = c.parent_1_id
WHERE p1.id = 4;
```

저는 MySQL 8에서 InnoDB와 MyISAM 두 가지 스토리지 엔진으로 이 테스트를 실행했습니다.

## 결과

### InnoDB 사용 시 (클러스터형 인덱스)

```
Run 0, Statement 1 (대리 키): 3104ms; Statement 2 (자연 키): 1910ms
Run 1, Statement 1 (대리 키): 3097ms; Statement 2 (자연 키): 1905ms
Run 2, Statement 1 (대리 키): 3045ms; Statement 2 (자연 키): 2276ms
Run 3, Statement 1 (대리 키): 3589ms; Statement 2 (자연 키): 1910ms
Run 4, Statement 1 (대리 키): 2961ms; Statement 2 (자연 키): 1897ms
```

### MyISAM 사용 시 (힙 테이블)

```
Run 0, Statement 1 (대리 키): 3473ms; Statement 2 (자연 키): 3288ms
Run 1, Statement 1 (대리 키): 3328ms; Statement 2 (자연 키): 3341ms
Run 2, Statement 1 (대리 키): 3674ms; Statement 2 (자연 키): 3307ms
Run 3, Statement 1 (대리 키): 3373ms; Statement 2 (자연 키): 3275ms
Run 4, Statement 1 (대리 키): 3298ms; Statement 2 (자연 키): 3322ms
```

보시다시피:

- MyISAM에서는 거의 차이가 없습니다.
- InnoDB에서는 대리 키 설계가 약 50% 느립니다.

이 테스트에서 저는 `CHILD_SURROGATE.PARENT_1_ID`에 보조 인덱스를 넣지 않았습니다. 이는 이미 고유 제약 조건에 의해 인덱싱되어 있기 때문입니다. 보조 인덱스를 추가하면 다시 빨라질 수 있지만, 그렇게 하면 공간을 낭비하게 되어 대리 키의 의미가 무색해집니다. 게다가 쓰기 작업마다 보조 인덱스를 업데이트해야 합니다.

## 결론

모든 테이블에 대리 키를 넣는 것의 순수한 교리적 가치는 이해합니다. 대리 키 값이 테이블/엔티티의 변경 불가능한(immutable) 식별자가 될 것이기 때문입니다.

하지만 명확한 후보 키가 있는 *관계 테이블*, 즉 다대다 관계를 정의하는 외래 키 집합에서는, 대리 키가 가치를 더하지 않을 뿐만 아니라 테이블이 클러스터형 인덱스를 사용할 때 특정 쿼리 세트의 성능을 실질적으로 저하시킵니다.

MySQL InnoDB나 SQL Server와 같은 RDBMS를 사용하고 있다면, 대리 키를 제거하여 상당한 개선의 여지가 있는지 확인해 보세요. 마이크로 최적화가 아닙니다. 제 벤치마크에서는 이러한 단순한 쿼리에서 50%의 성능 향상을 보여주었습니다!

## 추가 참고

PostgreSQL 사용자의 경우, PostgreSQL이 기본적으로 힙 테이블을 사용하기 때문에 MySQL InnoDB와 동일한 결과를 보이지 않습니다. 이는 Reddit 사용자의 의견에서 언급된 바 있습니다. PostgreSQL에서 클러스터형 인덱스와 유사한 동작을 원한다면 CLUSTER 명령을 사용하거나 다른 접근 방식을 고려해야 합니다.
