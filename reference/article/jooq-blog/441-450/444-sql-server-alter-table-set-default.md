# SQL Server ALTER TABLE SET DEFAULT

> 원문: https://blog.jooq.org/sql-server-alter-table-set-default/

Oracle에서 컬럼의 기본값을 수정하는 것은 매우 간단합니다:

```sql
CREATE TABLE t (
  val NUMBER(7) DEFAULT 1 NOT NULL
);

-- 이런, 기본값이 잘못되었네요. 변경해봅시다
ALTER TABLE t MODIFY val DEFAULT -1;

-- 이제 됐습니다
```

SQL Server는 DEFAULT 컬럼 속성을 단순한 속성이 아닌 실제 제약조건(constraint)으로 취급합니다. 이로 인해 시스템이 생성한 제약조건 이름을 가진 경우 직접 수정하는 것이 문제가 됩니다.

하지만 jOOQ 3.4는 다음과 같은 Transact-SQL 프로그램을 생성하여 이러한 복잡성을 추상화합니다:

```sql
DECLARE @constraint NVARCHAR(max);
DECLARE @command NVARCHAR(max);

SELECT @constraint = name
FROM sys.default_constraints
WHERE parent_object_id = object_id('t')
AND parent_column_id = columnproperty(
    object_id('t'), 'val', 'ColumnId');

IF @constraint IS NOT NULL
BEGIN
  SET @command = 'ALTER TABLE t DROP CONSTRAINT '
    + @constraint;
  EXECUTE sp_executeSQL @command

  SET @command = 'ALTER TABLE t ADD CONSTRAINT '
    + @constraint + ' DEFAULT -1 FOR val';
  EXECUTE sp_executeSQL @command
END
ELSE
BEGIN
  SET @command = 'ALTER TABLE t ADD DEFAULT -1 FOR val';
  EXECUTE sp_executeSQL @command
END
```

jOOQ를 사용한 Java 코드는 매우 간결하게 작성할 수 있습니다:

```java
DSL.using(configuration)
   .alterTable(T)
   .alter(T.VAL)
   .defaultValue(-1)
   .execute();
```

이 프로그램은 기본값이 이미 존재하는 경우 제약조건을 삭제하고 원래 이름으로 다시 생성하거나, 기본값이 존재하지 않는 경우 시스템이 생성한 이름으로 새로운 제약조건을 생성합니다.
