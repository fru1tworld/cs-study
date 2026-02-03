# SQL로 RDBMS 서버 버전을 확인하는 방법

> 원문: https://blog.jooq.org/how-to-get-an-rdbms-server-version-with-sql/

대부분의 RDBMS는 메타데이터 테이블 형태로 버전 정보를 제공합니다. SQL 접근만 가능한 상황에서 데이터베이스 버전을 확인해야 할 때, 각 데이터베이스 시스템별로 사용할 수 있는 쿼리를 정리했습니다.

## 데이터베이스별 버전 확인 쿼리

### ClickHouse

```sql
select version();
```

### CockroachDB

```sql
select version();
```

### Db2

```sql
select service_level from table (sysproc.env_get_inst_info()) t
```

### Derby

```sql
select getdatabaseproductversion() from (values (1)) t (a);
```

### DuckDB

```sql
select version();
```

### Exasol

```sql
select param_value
from exa_metadata
where param_name = 'databaseProductVersion';
```

### Firebird

```sql
select rdb$get_context('SYSTEM', 'ENGINE_VERSION')
from rdb$database;
```

### H2

```sql
select h2version();
```

### HANA

```sql
select * from m_database;
```

### HSQLDB

```sql
select character_value
from information_schema.sql_implementation_info
where implementation_info_name = 'DBMS VERSION'
```

### Informix

```sql
select dbinfo('version', 'full') from systables where tabid = 1;
```

### MariaDB

```sql
select version();
```

### MemSQL (SingleStore)

```sql
select @@memsql_version;
-- 또는
select version();
```

### MySQL

```sql
select version();
```

### Oracle

```sql
select version_full from v$instance;
```

### PostgreSQL

```sql
select version();
```

### Snowflake

```sql
select current_version();
```

### SQL Server

```sql
select @@version;
```

### SQLite

```sql
select sqlite_version();
```

### Sybase SQL Anywhere

```sql
select top 1 version from SYSHISTORY order by 1 desc;
```

### Teradata

```sql
select infodata from dbc.dbcinfov where infokey = 'VERSION'
```

### Trino

```sql
select version();
```

## 참고 사항

프로그래밍 방식으로 버전 정보가 필요한 경우, JDBC의 `DatabaseMetaData.getDatabaseProductVersion()` 메서드를 사용하면 데이터베이스에 관계없이 동일한 방식으로 버전 정보를 얻을 수 있습니다.
