# ORM은 "수정된" 값이 아니라 "변경된" 값을 업데이트해야 한다

> 원문: https://blog.jooq.org/orms-should-update-changed-values-not-just-modified-ones/

SQL `UPDATE` 문을 작성하는 방법은 여러 가지가 있다. 단일 컬럼만 업데이트하거나 모든 컬럼을 업데이트할 수 있다. 어떤 접근 방식이 가장 좋을까? 이 글에서는 이에 대해 논의해 보겠다.

## 핵심 주장

먼저 두 가지 중요한 개념을 구분해야 한다:

- 변경됨(Changed): 값이 "터치"되거나 "더티(dirty)" 상태로 표시되어 데이터베이스와 동기화해야 하는 경우. 실제로 이전 값과 다른지 여부와는 무관하다.
- 수정됨(Modified): 값이 실제로 이전에 알려진 값과 다른 경우

필자는 ORM이 PUT 의미론(모든 값을 업데이트)이나 PATCH 의미론(수정된 값만 업데이트)이 아니라, "변경된" 값을 업데이트해야 한다고 주장한다.

## 핵심 문제

많은 ORM은 불행히도 레코드의 모든 값을 업데이트하거나, 수정된 값만 업데이트한다. 전자는 비효율적일 수 있고, 후자는 잘못될 수 있다.

대부분의 ORM은 업데이트를 다음 두 가지 방식 중 하나로 처리한다:
1. 모든 컬럼을 업데이트한다 (Hibernate의 기본 동작)
2. 수정된 컬럼만 업데이트한다 (Hibernate의 `@DynamicUpdate`)

어느 접근 방식도 최적이 아니다. 첫 번째는 비효율적이고, 두 번째는 기능적으로 올바르지 않을 수 있다.

## 동시성 문제

수정된 값만 업데이트하는 PATCH 의미론을 사용하면 동시 업데이트가 예상치 못한 결과를 만들 수 있다. 한 세션이 `last_name`을 "Smith"로 업데이트하는 동안 다른 세션이 `first_name`을 "Jane"으로 업데이트하면, 의도했던 "Jane Doe"나 "John Smith"가 아닌 "Jane Smith"가 될 수 있다.

반대로, 카운터를 단순히 증가시키는 경우에는 PATCH 의미론이 유리하다: `UPDATE customer SET clicks = clicks + 1`은 클라이언트 측 캐시된 값을 피할 때 동시성 문제를 완전히 회피한다.

## 기술적 예제

### jOOQ의 접근 방식

jOOQ는 "변경됨(changed)" 의미론을 구현한다:

```java
customer.setFirstName("John");
customer.setLastName("Smith");
assertTrue(customer.changed(CUSTOMER.FIRST_NAME));
assertTrue(customer.changed(CUSTOMER.LAST_NAME));
assertFalse(customer.changed(CUSTOMER.CLICKS));
customer.store();
```

이 코드는 다음 SQL을 생성한다:

```sql
UPDATE customer
SET first_name = ?,
    last_name = ?
WHERE customer_id = ?
```

`setFirstName()`과 `setLastName()`을 호출했기 때문에, 실제 값이 변경되었는지 여부와 관계없이 해당 컬럼들이 "변경됨"으로 표시된다. `clicks` 컬럼은 터치되지 않았으므로 UPDATE 문에 포함되지 않는다.

### Oracle 트리거 예제

트리거는 컬럼이 명시적으로 업데이트될 때만 실행되며, 수정될 때가 아니다:

```sql
CREATE TRIGGER t BEFORE UPDATE OF c, d ON x
BEGIN
  IF updating('c') THEN
    dbms_output.put_line('Updating c');
  END IF;
END;
```

컬럼 `b`를 업데이트하면, PUT 의미론에서 `b`가 UPDATE 문에 포함되더라도 이 트리거는 실행되지 않는다. 트리거는 컬럼 `c`나 `d`가 UPDATE 문에 포함될 때만 실행된다.

## 성능 고려사항

PATCH를 선호하는 이유:
- UNDO/REDO 로그 오버헤드 감소
- 불필요한 인덱스 업데이트 감소
- 더 작은 문장 크기
- 백업 및 복구 효율성

PUT을 선호하는 이유:
- 개선된 커서 캐시 성능 (미미한 이점)
- 동일한 문장의 일괄 처리가 더 쉬움

## 데이터베이스 기본값과 INSERT

"변경됨" 의미론은 INSERT 작업에도 중요하다:

```java
CustomerRecord c1 = ctx.newRecord(CUSTOMER);
c1.setFirstName("John");
c1.setLastName("Doe");
c1.store();
```

이 코드는 다음 SQL을 생성한다:

```sql
INSERT INTO customer (first_name, last_name)
VALUES (?, ?);
```

`clicks`와 `purchases` 컬럼은 데이터베이스 기본값을 사용하게 된다. 만약 모든 컬럼이 명시적으로 포함되었다면 이러한 기본값이 적용되지 않았을 것이다.

예를 들어, 다음과 같은 테이블 정의가 있다고 가정하자:

```sql
CREATE TABLE customer (
  customer_id INT PRIMARY KEY,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  clicks INT DEFAULT 0,
  purchases INT DEFAULT 0
);
```

모든 컬럼을 포함하는 INSERT를 하면:

```sql
INSERT INTO customer (customer_id, first_name, last_name, clicks, purchases)
VALUES (?, ?, ?, NULL, NULL);
```

이렇게 하면 `clicks`와 `purchases`가 기본값인 0이 아니라 NULL이 된다. 이는 의도한 동작이 아닐 수 있다.

## 권장 해결책

1. jOOQ 사용 - 기본적으로 "변경됨" 의미론을 제공한다
2. Hibernate의 `@DynamicUpdate` 사용 - 완벽하지 않지만 PUT 의미론보다 낫다 (주의사항 있음)
3. 스키마 정규화 - 자주 업데이트되는 컬럼을 다른 테이블로 분리한다
4. SQL 직접 작성 - 특히 `clicks = clicks + 1`과 같은 표현식에 유용하다

카운터나 합계처럼 자주 증가하는 필드의 경우, 클라이언트 측 계산 대신 직접 SQL 표현식을 사용하는 것이 좋다:

```java
// 나쁜 예: 동시성 문제 발생 가능
int clicks = customer.getClicks();
customer.setClicks(clicks + 1);
customer.store();

// 좋은 예: SQL 표현식 직접 사용
ctx.update(CUSTOMER)
   .set(CUSTOMER.CLICKS, CUSTOMER.CLICKS.add(1))
   .where(CUSTOMER.CUSTOMER_ID.eq(customerId))
   .execute();
```

## 결론

이 글에서 다음과 같은 인용이 있다: "모든 컬럼을 업데이트하는 것은 직관에 반한다. 그냥 '맞지 않는' 느낌이다. `SELECT *`와 비슷하다." 이는 개발자들이 불필요한 컬럼 선택을 피하는 것처럼 불필요한 컬럼 업데이트도 피해야 함을 강조한다.

"변경됨"과 "수정됨" 값 사이의 구분은 정확성, 성능, 그리고 적절한 트리거/제약 조건 동작을 위해 매우 중요하다. ORM을 선택할 때 이러한 의미론을 올바르게 처리하는지 확인하는 것이 좋다.
