# ORA-38104: ON 절에서 참조된 컬럼은 업데이트할 수 없음 해결 방법

> 원문: https://blog.jooq.org/how-to-work-around-ora-38104-columns-referenced-in-the-on-clause-cannot-be-updated/

## 서론

Oracle의 `MERGE` 문에서 흔히 마주치게 되는 제한 사항 중 하나가 바로 ORA-38104 오류입니다. 이 오류는 `ON` 절에서 참조된 컬럼을 `UPDATE` 절에서 수정하려고 할 때 발생합니다. 이 글에서는 이 제한 사항을 우회하는 여러 가지 방법을 소개합니다.

## 문제 상황

다음과 같은 테이블이 있다고 가정해 봅시다:

```sql
CREATE TABLE person (
  id NUMBER(10) NOT NULL PRIMARY KEY,
  user_name VARCHAR2(50),
  score NUMBER(10)
);
```

이제 다음과 같은 `MERGE` 문을 실행하려고 합니다:

```sql
MERGE INTO person t
USING (SELECT 1 id, 'foo' user_name, 100 score FROM dual) s
ON (t.id = s.id OR t.user_name = s.user_name)
WHEN MATCHED THEN UPDATE
  SET t.user_name = s.user_name, t.score = 100
WHEN NOT MATCHED THEN INSERT (id, user_name, score)
  VALUES (s.id, s.user_name, s.score)
```

이 쿼리를 실행하면 다음 오류가 발생합니다:

```
ORA-38104: Columns referenced in the ON Clause cannot be updated: "T"."USER_NAME"
```

## 왜 이런 제한이 있는가?

Oracle은 `MERGE` 문 실행 중에 행이 매칭된 그룹과 매칭되지 않은 그룹 사이를 이동하는 것을 방지하기 위해 이 제한을 두었습니다. `ON` 절에서 사용된 컬럼 값이 변경되면, 해당 행이 다른 그룹으로 이동할 수 있기 때문입니다.

하지만 실제로는 이 제한이 지나치게 엄격한 경우가 많습니다. 많은 경우에 개발자는 단순히 일회성 데이터 마이그레이션 쿼리를 작성하는 것이며, 이런 상황에서 이 제한은 불필요한 장애물이 됩니다.

## 해결 방법 1: 행 값 표현식 (Row Value Expressions) 사용

가장 간단하고 효율적인 해결 방법은 행 값 표현식을 사용하는 것입니다. 튜플 비교를 사용하면 파서가 컬럼 참조를 감지하지 못합니다:

```sql
MERGE INTO person t
USING (SELECT 1 id, 'foo' user_name, 100 score FROM dual) s
ON (t.id = s.id OR (t.user_name, 'dummy') = ((s.user_name, 'dummy')))
WHEN MATCHED THEN UPDATE
  SET t.user_name = s.user_name, t.score = 100
WHEN NOT MATCHED THEN INSERT (id, user_name, score)
  VALUES (s.id, s.user_name, s.score)
```

이 방법의 장점은 인덱스 사용이 여전히 가능하다는 것입니다. Oracle 옵티마이저는 튜플 비교를 개별 컬럼 비교로 분해할 수 있어서 실행 계획에 영향을 주지 않습니다.

## 해결 방법 2: NVL()을 사용한 인라인 뷰

인라인 뷰를 생성하고 컬럼을 함수로 감싸면 파서가 원래 컬럼 참조를 인식하지 못합니다:

```sql
MERGE INTO (
  SELECT id, user_name, nvl(user_name, null) n FROM person
) t
USING (SELECT 1 id, 'foo' user_name, 100 score FROM dual) s
ON (t.id = s.id OR t.n = s.user_name)
WHEN MATCHED THEN UPDATE
  SET t.user_name = s.user_name, t.score = 100
WHEN NOT MATCHED THEN INSERT (id, user_name, score)
  VALUES (s.id, s.user_name, s.score)
```

`NVL(user_name, null)`은 사실상 `user_name`과 동일한 값을 반환하지만, 파서 입장에서는 다른 컬럼으로 인식됩니다. 이 방법도 실행 계획 효율성을 유지합니다.

## 해결 방법 3: 상관 서브쿼리 (Correlated Subquery) 사용

상관 서브쿼리를 사용하는 방법도 있습니다:

```sql
MERGE INTO person t
USING (SELECT 1 id, 'foo' user_name, 100 score FROM dual) s
ON (t.id = s.id OR (SELECT t.user_name FROM dual) = s.user_name)
WHEN MATCHED THEN UPDATE
  SET t.user_name = s.user_name, t.score = 100
WHEN NOT MATCHED THEN INSERT (id, user_name, score)
  VALUES (s.id, s.user_name, s.score)
```

이 방법은 동작하지만, 인덱스 최적화를 방해할 수 있어 성능이 저하될 수 있습니다. 대량의 데이터를 처리할 때는 이 방법을 피하는 것이 좋습니다.

## 해결 방법 4: WHERE 절 사용 (AND 조건의 경우)

만약 `ON` 절에서 `OR` 대신 `AND` 조건을 사용하는 경우라면, `WHERE` 절로 조건을 이동하는 것이 가능합니다:

```sql
MERGE INTO person t
USING (SELECT 1 id, 'foo' user_name, 100 score FROM dual) s
ON (t.id = s.id)
WHEN MATCHED THEN UPDATE
  SET t.user_name = s.user_name, t.score = 100
  WHERE t.user_name = s.user_name
WHEN NOT MATCHED THEN INSERT (id, user_name, score)
  VALUES (s.id, s.user_name, s.score)
```

하지만 이 방법은 쿼리의 의미를 변경할 수 있으므로 주의가 필요합니다. `WHERE` 절은 이미 매칭된 행을 필터링하는 역할을 하기 때문에, 원래 `ON` 절에서 의도한 매칭 로직과 다르게 동작할 수 있습니다.

## jOOQ에서의 활용

jOOQ를 사용하는 경우, 이러한 해결 방법들을 프로그래밍 방식으로 적용할 수 있습니다. 예를 들어 행 값 표현식을 사용하는 방법:

```java
// jOOQ를 사용한 해결 방법
ctx.mergeInto(PERSON)
   .using(
       select(
           val(1).as("id"),
           val("foo").as("user_name"),
           val(100).as("score")
       ).from(dual())
   )
   .on(PERSON.ID.eq(field("id", Integer.class))
       .or(row(PERSON.USER_NAME, val("dummy"))
           .eq(row(field("user_name", String.class), val("dummy")))))
   .whenMatchedThenUpdate()
   .set(PERSON.USER_NAME, field("user_name", String.class))
   .set(PERSON.SCORE, val(100))
   .whenNotMatchedThenInsert(PERSON.ID, PERSON.USER_NAME, PERSON.SCORE)
   .values(field("id", Integer.class), field("user_name", String.class), field("score", Integer.class))
   .execute();
```

## 주의 사항

이러한 해결 방법들은 본질적으로 Oracle 파서의 한계를 이용한 것입니다. 다시 말해, 이것들은 파서 버그를 악용하는 것이지 정당한 해결책이 아닙니다.

Oracle 12c와 18c에서 테스트된 이러한 방법들이 향후 버전에서도 동작할 것이라는 보장은 없습니다. Oracle이 시맨틱 체커를 개선하면 이러한 우회 방법들이 더 이상 작동하지 않을 수 있습니다.

따라서 이러한 기법들은 다음과 같은 상황에서만 사용하는 것이 좋습니다:

- 일회성 데이터 마이그레이션 쿼리
- 애드혹 데이터 수정 작업
- 즉시 실행 후 폐기될 스크립트

프로덕션 환경에서 지속적으로 사용되는 코드에는 이러한 해결 방법에 의존하지 않는 것이 바람직합니다.

## 결론

ORA-38104 오류는 Oracle `MERGE` 문의 제한 사항 중 하나이지만, 여러 가지 우회 방법이 존재합니다. 가장 권장되는 방법은 행 값 표현식을 사용하는 것으로, 이 방법은 인덱스 사용을 유지하면서도 파서를 우회할 수 있습니다.

그러나 이상적으로는 Oracle이 이 제한 사항을 완전히 제거하는 것이 가장 좋은 해결책일 것입니다. 개발자가 파서의 허점을 찾아 우회해야 하는 상황 자체가 바람직하지 않기 때문입니다.
