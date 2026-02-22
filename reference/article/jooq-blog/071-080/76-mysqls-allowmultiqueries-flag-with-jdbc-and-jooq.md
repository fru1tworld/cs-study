# MySQL의 allowMultiQueries 플래그와 JDBC 및 jOOQ

> 원문: https://blog.jooq.org/mysqls-allowmultiqueries-flag-with-jdbc-and-jooq/

MySQL의 JDBC 커넥터에는 `allowMultiQueries`라는 보안 기능이 포함되어 있으며, 기본값은 `false`입니다. 이 기능을 활성화하면 단일 쿼리에서 여러 SQL 문을 실행할 수 있지만, 이는 보안상의 영향을 미칩니다.

## 보안 기능 설명

이 플래그는 기본적으로 특정 SQL 인젝션 취약점을 방지합니다. 예를 들어, `allowMultiQueries=false` 상태에서 다음 코드는 실패합니다:

```java
s.executeUpdate("""
    insert into t values (1);
    insert into t values (2);
""");
```

다음과 같은 오류가 발생합니다: `You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'insert into t values (2)' at line 2`

JDBC 연결 URL에서 `allowMultiQueries=true`로 설정하면 이러한 배치 문이 실행될 수 있습니다.

## 이 보안 기능이 존재하는 이유

이 기능은 다음과 같은 문자열 연결로 인한 SQL 인젝션 위험을 대상으로 합니다:

```java
s.executeUpdate("insert into t values (" + value + ")");
```

만약 `value`에 `"1); drop table t;"`와 같은 악의적인 SQL이 포함되어 있다면, `allowMultiQueries=false`가 여러 문의 실행을 차단하지 않는 한 의도한 대로 실행될 것입니다.

중요한 주의사항: "다중 쿼리를 비활성화해도 모든 SQL 인젝션을 방지하지는 못합니다. 바인드 변수(PreparedStatement, 저장 프로시저 또는 jOOQ)를 사용하는 것이 최선의 방어책입니다."

## jOOQ의 allowMultiQueries 의존성

jOOQ의 일반적으로 안전한 설계에도 불구하고, 여러 기능을 위해 내부적으로 `allowMultiQueries=true`가 필요합니다:

### GROUP_CONCAT 처리

jOOQ는 세션 변수 수정을 앞뒤로 추가합니다:

```sql
SET @t = @@group_concat_max_len;
SET @@group_concat_max_len = 4294967295;
SELECT group_concat(...) FROM T;
SET @@group_concat_max_len = @t;
```

이는 MySQL의 기본 1024자 제한에서 발생하는 암묵적 잘림(silent truncation)을 방지합니다.

### CREATE OR REPLACE 함수 에뮬레이션

별도의 DROP과 CREATE 문을 요구하는 대신, jOOQ는 다중 쿼리 지원이 필요한 결합된 구문을 생성합니다.

### FOR UPDATE WAIT n 지원

MySQL 8.0.26에는 네이티브 지원이 없으므로, jOOQ는 이를 에뮬레이션합니다:

```sql
SET @t = @@innodb_lock_wait_timeout;
SET @@innodb_lock_wait_timeout = 2;
SELECT * FROM t FOR UPDATE;
SET @@innodb_lock_wait_timeout = @t;
```

### 익명 블록

jOOQ는 임시 저장 프로시저를 생성하여 절차적 익명 블록을 지원합니다:

```sql
CREATE PROCEDURE block_1629705082441_2328258()
BEGIN
  DECLARE i INT;
  SET i = 1;
  IF i = 0 THEN
    INSERT INTO a (col) VALUES (1);
  END IF;
END;
CALL block_1629705082441_2328258();
DROP PROCEDURE block_1629705082441_2328258;
```

## 권장 사항

MySQL에서 jOOQ를 사용하는 경우, jOOQ 연결에 대해서만 `allowMultiQueries=true`를 활성화하고 다른 곳에서는 보안을 위해 `false`를 유지하세요. 향후 jOOQ 버전에서는 다중 쿼리 실행을 별도의 라운드 트립으로 구성할 수 있게 되어, 이 요구 사항이 제거될 수 있습니다. `Settings.renderGroupConcatMaxLenSessionVariable` 플래그를 사용하면 원하는 경우 특정 세션 변수 관리를 비활성화할 수 있습니다.
