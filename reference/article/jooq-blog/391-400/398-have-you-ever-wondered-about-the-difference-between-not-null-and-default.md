# NOT NULL과 DEFAULT의 차이에 대해 궁금해본 적 있는가?

> 원문: https://blog.jooq.org/have-you-ever-wondered-about-the-difference-between-not-null-and-default/

2014년 11월 11일

저자: lukaseder

---

SQL에서 DDL을 작성할 때, 컬럼에 NOT NULL이나 DEFAULT와 같은 여러 제약 조건을 지정할 수 있다. 어떤 사람들은 이 두 제약 조건이 실제로 중복되는지 궁금해할 수 있다. 즉, 이미 DEFAULT 절이 있는데 NOT NULL 제약 조건을 지정할 필요가 여전히 있는가?

그렇다, NOT NULL 제약 조건을 여전히 지정해야 한다. 그리고 아니다, 두 제약 조건은 중복되지 않는다.

## DEFAULT의 작동 방식

DEFAULT는 insert / update 문에서 명시적인 값이 없을 때 삽입될 값이다. 이전 블로그 글에서 SQL DEFAULT 키워드가 어떻게 작동하는지 살펴보았다:

- [잘 알려지지 않은 SQL 기능: DEFAULT VALUES](https://blog.jooq.org/lesser-known-sql-features-default-values/)

DEFAULT 제약 조건은 실제로 DML 문과 그것들이 업데이트하는 다양한 컬럼을 지정하는 방식과만 상호작용한다. 명시적인 값을 지정하지 않으면, DEFAULT 값이 사용된다. 명시적으로 NULL을 지정하면, NULL이 사용된다.

다음 여섯 가지 SQL 문을 고려해보자:

```sql
-- 1. 컬럼이 생략된 경우, DEFAULT 값이 삽입된다
INSERT INTO t (a, b) VALUES (1, 2);

-- 2. DEFAULT 키워드를 사용하면 DEFAULT 값이 삽입된다
INSERT INTO t (a, b, c) VALUES (1, 2, DEFAULT);

-- 3. DEFAULT VALUES 절을 사용하면 모든 컬럼에 DEFAULT 값이 삽입된다
INSERT INTO t DEFAULT VALUES;

-- 4. 명시적으로 NULL을 사용하면 NULL이 삽입된다 (NOT NULL이 없는 경우에만)
INSERT INTO t (a, b, c) VALUES (1, 2, NULL);

-- 5. UPDATE에서 DEFAULT를 사용하면 DEFAULT 값으로 업데이트된다
UPDATE t SET c = DEFAULT;

-- 6. UPDATE에서 명시적으로 NULL을 사용하면 NULL로 업데이트된다 (NOT NULL이 없는 경우에만)
UPDATE t SET c = NULL;
```

위의 문장 1, 2, 3, 5는 c 컬럼에 NOT NULL 제약 조건이 있든 없든 상관없이 작동한다. 이 문장들은 DEFAULT 값을 사용하기 때문이다.

그러나 문장 4와 6은 c 컬럼에 NOT NULL 제약 조건이 있으면 실패한다. 왜냐하면 명시적으로 NULL 값을 삽입/업데이트하려고 시도하기 때문이다.

## NOT NULL은 더 보편적인 보장이다

NOT NULL 제약 조건은 조작하는 DML 문의 "외부"에서도 컬럼의 내용을 제약하는 훨씬 더 보편적인 보장이다.

예를 들어, 데이터 집합이 있고 그 다음에 DEFAULT 제약 조건을 추가한다면, 이것은 기존 데이터에 영향을 주지 않고 새로 삽입되는 데이터에만 영향을 미친다.

그러나 데이터 집합이 있고 그 다음에 NOT NULL 제약 조건을 추가한다면, 제약 조건이 유효한 경우에만 - 즉, 컬럼에 NULL 값이 없는 경우에만 - 실제로 그렇게 할 수 있다. 그렇지 않으면 오류가 발생한다.

## 쿼리 옵티마이저에 대한 유용성

NOT NULL 제약 조건에만 적용되는 또 다른 매우 흥미로운 사용 사례는 쿼리 옵티마이저와 쿼리 실행 계획에 대한 유용성이다. 특히 Oracle을 사용할 때, NOT IN 술어를 사용하는 쿼리는 컬럼에 인덱스와 그 NOT NULL 제약 조건이 있을 때 훨씬 더 빠르다. 불행히도 NULL 값은 Oracle 인덱스에 포함되지 않기 때문에, 인덱스와 그 특정 제약 조건이 필요하다.

NOT IN 술어가 NULL 값과 어떻게 상호작용하는지에 대한 자세한 내용은 다음 글을 참조하라:

- [NOT IN 술어에서 NULL 값을 가질 수 있는 다양한 상황](https://blog.jooq.org/beware-of-null-values-in-not-in-predicates/)

이것은 많은 SQL 개발자들의 삶에서 많은 디버깅 시간이 소비된 부분이다. NOT IN 술어와 안티 조인에서 NULL 값을 가질 수 있는 다양한 상황들 말이다.

## 결론

요약하면:

- DEFAULT는 DML 문에서 명시적인 값이 제공되지 않을 때 사용될 값을 지정한다
- NOT NULL은 컬럼이 절대로 NULL 값을 포함할 수 없다는 보편적인 보장이다

컬럼이 NULL을 포함해서는 안 된다면, DEFAULT 절이 있더라도 항상 NOT NULL을 명시적으로 지정해야 한다. 두 제약 조건은 서로 다른 목적을 가지며, 완전한 컬럼 무결성과 최적화 이점을 위해 함께 사용해야 한다.
