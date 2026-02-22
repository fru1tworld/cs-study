> 원문: https://blog.jooq.org/how-to-quickly-rename-all-primary-keys-in-oracle/

# Oracle에서 모든 기본 키(Primary Key)를 빠르게 이름 변경하는 방법

## 시스템 생성 이름의 문제점

기본 키(Primary Key)에 명시적인 이름을 지정하지 않으면, Oracle은 `SYS_C0042007`과 같은 암호화된 식별자를 자동으로 생성합니다. 이러한 이름들은 실행 계획(Execution Plan)에 나타나기 때문에 분석을 어렵게 만들고, 데이터베이스 간 스키마 이식성을 저해합니다.

## 해결 방법

일관된 명명 규칙을 수립하는 것이 좋습니다. 예를 들어 `PK_[테이블명]` 형식을 사용할 수 있습니다.

다음은 기존의 기본 키들을 일괄적으로 이름 변경하는 PL/SQL 코드입니다:

```sql
SET SERVEROUTPUT ON
BEGIN
  FOR stmts IN (
    SELECT
      'ALTER TABLE ' || table_name ||
      ' RENAME CONSTRAINT ' || constraint_name ||
      ' TO PK_' || table_name AS stmt
    FROM user_constraints
    WHERE constraint_name LIKE 'SYS%'
    AND constraint_type = 'P'
  ) LOOP
    dbms_output.put_line(stmts.stmt);
    EXECUTE IMMEDIATE stmts.stmt;
  END LOOP;

  FOR stmts IN (
    SELECT
      'ALTER INDEX ' || index_name ||
      ' RENAME TO PK_' || table_name AS stmt
    FROM user_constraints
    WHERE index_name LIKE 'SYS%'
    AND constraint_type = 'P'
  ) LOOP
    dbms_output.put_line(stmts.stmt);
    EXECUTE IMMEDIATE stmts.stmt;
  END LOOP;
END;
/
```

이 스크립트는 두 가지 작업을 수행합니다:

1. 제약 조건(Constraint) 이름 변경: `SYS`로 시작하는 시스템 생성 기본 키 제약 조건을 찾아 `PK_[테이블명]` 형식으로 이름을 변경합니다.

2. 인덱스(Index) 이름 변경: 기본 키와 연결된 시스템 생성 인덱스도 동일한 명명 규칙으로 이름을 변경합니다.

## 핵심 요점

설명적인 제약 조건 이름을 사용하면 실행 계획의 가독성이 크게 향상되고, 데이터베이스 유지보수가 훨씬 수월해집니다. 새로운 테이블을 생성할 때는 처음부터 명시적인 이름을 지정하는 습관을 들이는 것이 좋습니다.
