# Oracle 팁: v$sql 테이블 또는 뷰가 존재하지 않음

> 원문: https://blog.jooq.org/oracle-tip-vsql-table-or-view-does-not-exist/

## 문제 상황

실행 계획을 분석하기 위해 `v$sql` 테이블에서 SQL 문을 조회하려고 할 때, 다음과 같은 오류가 발생하는 경우가 있습니다: "ORA-00942: 테이블 또는 뷰가 존재하지 않습니다"

이 오류를 발생시키는 쿼리는 일반적으로 다음과 같은 형태입니다:

```sql
SELECT last_active_time, sql_id, child_number, sql_text
FROM v$sql
WHERE upper(sql_fulltext) LIKE '%SOME_SQL_TEXT%'
ORDER BY last_active_time DESC;
```

## 원인

이 오류는 사용자가 Oracle의 동적 성능 뷰(dynamic performance view)에 접근하는 데 필요한 데이터베이스 권한이 없기 때문에 발생합니다. 이러한 뷰는 기본적으로 부여되지 않는 특정 권한을 필요로 합니다.

## 해결 방법

DBA(또는 로컬 인스턴스에서 적절한 권한을 가진 사용자)가 다음과 같이 접근 권한을 부여해야 합니다:

```sql
sqlplus "/ as sysdba"
SQL> GRANT SELECT ANY DICTIONARY TO MY_USER;
Grant succeeded
```

이 권한 부여가 실행되면, 사용자는 성공적으로 `v$sql`을 조회하고 `DBMS_XPLAN.DISPLAY_CURSOR` 함수를 사용하여 실행 계획을 분석하는 데 필요한 SQL_ID를 얻을 수 있습니다.
