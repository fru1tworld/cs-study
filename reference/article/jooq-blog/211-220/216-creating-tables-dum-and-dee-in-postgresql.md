> 원문: https://blog.jooq.org/creating-tables-dum-and-dee-in-postgresql/

# PostgreSQL에서 Table Dum과 Dee 생성하기

이 블로그 포스트에서는 두 가지 이론적인 SQL 개념인 Table Dee와 Table Dum을 PostgreSQL에서 구현하는 방법을 살펴봅니다.

## 핵심 개념

관계 이론(Relational Theory)에 따르면:

- Table Dee: "속성(attribute)이 없고 단일 튜플(tuple)을 가지는 관계(relation). True의 역할을 합니다."
- Table Dum: "속성이 없고 튜플도 없는 관계. False의 역할을 합니다."

## 구현

PostgreSQL에서 이러한 테이블을 생성하는 방법을 보여드립니다:

```sql
CREATE TABLE dum();
CREATE TABLE dee();
INSERT INTO dee DEFAULT VALUES;
```

제약 조건을 유지하기 위해 트리거(trigger)를 사용하여 수정을 방지합니다:

```sql
CREATE FUNCTION dum_trg ()
RETURNS trigger
AS $$
BEGIN
  RAISE EXCEPTION 'Dum must be empty';
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER dum_trg
BEFORE INSERT OR UPDATE OR DELETE OR TRUNCATE
ON dum
FOR EACH STATEMENT
EXECUTE PROCEDURE dum_trg();
```

비슷한 트리거가 Dee의 단일 행이 삭제되지 않도록 보호합니다.

## 실제 결과

- `SELECT * FROM dum;`은 0개의 열(column)과 0개의 행(row)을 반환합니다
- `SELECT * FROM dee;`는 열 없이 1개의 행을 반환합니다
- 두 테이블 모두 데이터베이스 제약 조건을 통해 이론적 속성을 유지합니다

## 알려진 제한 사항

이 구현은 PostgreSQL의 일관성 문제를 드러냅니다:
- `SELECT DISTINCT * FROM dee;`는 오류를 발생시킵니다
- `UNION` 연산은 빈 열 결과에서 중복 제거를 제대로 처리하지 못합니다

## 결론

데이터베이스 엔진의 한계로 인해 완벽하지는 않지만, 이러한 이론적 구조는 창의적인 테이블 설계와 트리거 메커니즘을 사용하여 PostgreSQL에서 실질적으로 구현할 수 있습니다.
