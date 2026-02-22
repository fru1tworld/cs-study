# 주요 RDBMS의 JDBC 연결 URL 모음

> 원문: https://blog.jooq.org/jdbc-connection-urls-of-the-most-popular-rdbms/

JDBC로 RDBMS에 연결해야 하는데 JDBC 연결 URL이나 드라이버 이름이 바로 떠오르지 않으시나요? 걱정하지 마세요. 아래에서 사용 중인 RDBMS를 찾아보시면 됩니다:

## BigQuery

- 드라이버: `com.simba.googlebigquery.jdbc42.Driver`
- URL: `jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;ProjectId=<project-id>;OAuthType=0;OAuthServiceAcctEmail=<service-account>;OAuthPvtKeyPath=path-to-key.json`

## CockroachDB

- 드라이버: `org.postgresql.Driver`
- URL: `jdbc:postgresql://<host>/<database>`

## Db2

- 드라이버: `com.ibm.db2.jcc.DB2Driver`
- URL: `jdbc:db2://<host>:50000/<database>`

## Derby

- 드라이버: `org.apache.derby.jdbc.EmbeddedDriver`
- URL: `jdbc:derby:<path-to-database>;create=true`

## DuckDB

- 드라이버: `org.duckdb.DuckDBDriver`
- URL: `jdbc:duckdb:<path-to-database>`

## EXASOL

- 드라이버: `com.exasol.jdbc.EXADriver`
- URL: `jdbc:exa:<host>:9563;schema=<default-schema>`

## Firebird

- 드라이버: `org.firebirdsql.jdbc.FBDriver`
- URL: `jdbc:firebirdsql:<host>:<path-to-database>`

## H2

- 드라이버: `org.h2.Driver`
- URL: `jdbc:h2:<path-to-database>`

## HANA

- 드라이버: `com.sap.db.jdbc.Driver`
- URL: `jdbc:sap://<host>:39017`

## HSQLDB

- 드라이버: `org.hsqldb.jdbcDriver`
- URL: `jdbc:hsqldb:file:<path-to-database>`

## Ignite

- 드라이버: `org.apache.ignite.IgniteJdbcThinDriver`
- URL: `jdbc:ignite:thin://<host>`

## Informix

- 드라이버: `com.informix.jdbc.IfxDriver`
- URL: `jdbc:informix-sqli://<host>:9088/<database>:INFORMIXSERVER=informix`

## Ingres

- 드라이버: `com.ingres.jdbc.IngresDriver`
- URL: `jdbc:ingres://<host>:II7/<database>`

## MariaDB

- 드라이버: `org.mariadb.jdbc.Driver`
- URL: `jdbc:mariadb://<host>/<default-schema>`

## MemSQL

- 드라이버: `com.mysql.cj.jdbc.Driver`
- URL: `jdbc:mysql://127.0.0.1/<default-schema>`

## MySQL

- 드라이버: `com.mysql.cj.jdbc.Driver`
- URL: `jdbc:mysql://127.0.0.1/<default-schema>`

## Oracle

- 드라이버: `oracle.jdbc.OracleDriver`
- URL: `jdbc:oracle:thin:@<database>:1521/<SID>`

## PostgreSQL

- 드라이버: `org.postgresql.Driver`
- URL: `jdbc:postgresql://<host>/<database>`

## Redshift

- 드라이버: `com.amazon.redshift.jdbc.Driver`
- URL: `jdbc:redshift://<cluster-id>.<region>.redshift.amazonaws.com:5439/<database>`

## Snowflake

- 드라이버: `net.snowflake.client.jdbc.SnowflakeDriver`
- URL: `jdbc:snowflake://<server>.<region>.snowflakecomputing.com:443/?db=<database>&schema=<schema>`

## SQLite

- 드라이버: `org.sqlite.JDBC`
- URL: `jdbc:sqlite:<path-to-database>`

## SQL Server

- 드라이버: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
- URL: `jdbc:sqlserver://<host>:1433;databaseName=<database>`

## Sybase ASE

- 드라이버: `com.sybase.jdbc4.jdbc.SybDriver`
- URL: `jdbc:sybase:Tds:<host>:5000/<database>`

## Sybase SQL Anywhere

- 드라이버: `com.sybase.jdbc4.jdbc.SybDriver`
- URL: `jdbc:sybase:Tds:<host>:2638`

## Teradata

- 드라이버: `com.teradata.jdbc.TeraDriver`
- URL: `jdbc:teradata://<host>/DATABASE=<database>,DBS_PORT=1025`

## Trino

- 드라이버: `io.trino.jdbc.TrinoDriver`
- URL: `jdbc:trino://<host>:8080/memory/default`

## Vertica

- 드라이버: `com.vertica.jdbc.Driver`
- URL: `jdbc:vertica://<host>:5433/<database>`

## YugabyteDB

- 드라이버: `com.yugabyte.Driver`
- URL: `jdbc:yugabytedb://<host>/<database>`
