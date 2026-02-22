# 가장 인기 있는 JDBC 드라이버의 Maven 좌표

> 원문: https://blog.jooq.org/maven-coordinates-of-the-most-popular-jdbc-drivers/

애플리케이션에 JDBC 드라이버를 추가해야 하는데, Maven 좌표를 모르시나요?

이 블로그 포스트는 jOOQ 통합 테스트에서 사용하는 가장 인기 있는 드라이버들을 나열합니다. 최신 버전은 [https://central.sonatype.com/](https://central.sonatype.com/)에서 `g:groupId a:artifactId` 파라미터로 직접 조회할 수 있습니다. 예를 들어, H2 데이터베이스와 드라이버는 다음에서 확인할 수 있습니다: [https://central.sonatype.com/search?q=g%3Acom.h2database+a%3Ah2](https://central.sonatype.com/search?q=g%3Acom.h2database+a%3Ah2)

이 목록은 Maven Central에 있는 드라이버만 포함합니다. 다운로드하기 전에 드라이버의 라이선스를 확인하여 규정을 준수하시기 바랍니다.

## Db2

```xml
<dependency>
    <groupId>com.ibm.db2</groupId>
    <artifactId>jcc</artifactId>
</dependency>
```

## Derby

```xml
<dependency>
    <groupId>org.apache.derby</groupId>
    <artifactId>derby</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.derby</groupId>
    <artifactId>derbyclient</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.derby</groupId>
    <artifactId>derbytools</artifactId>
</dependency>
```

## DuckDB

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
</dependency>
```

## Firebird

```xml
<dependency>
    <groupId>org.firebirdsql.jdbc</groupId>
    <artifactId>jaybird</artifactId>
</dependency>
```

## H2

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```

## HANA

```xml
<dependency>
    <groupId>com.sap.cloud.db.jdbc</groupId>
    <artifactId>ngdbc</artifactId>
</dependency>
```

## HSQLDB

```xml
<dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
</dependency>
```

## Informix

```xml
<dependency>
    <groupId>com.ibm.informix</groupId>
    <artifactId>jdbc</artifactId>
</dependency>
```

## MariaDB

```xml
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
</dependency>
```

## MySQL

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

## Oracle

```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
</dependency>
```

## PostgreSQL

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

## Redshift

```xml
<dependency>
    <groupId>com.amazon.redshift</groupId>
    <artifactId>redshift-jdbc42</artifactId>
</dependency>
```

## Snowflake

```xml
<dependency>
    <groupId>net.snowflake</groupId>
    <artifactId>snowflake-jdbc</artifactId>
</dependency>
```

## SQLite

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
</dependency>
```

## SQL Server

```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
</dependency>
```

## Sybase ASE

```xml
<dependency>
    <groupId>net.sourceforge.jtds</groupId>
    <artifactId>jtds</artifactId>
</dependency>
```

## Trino

```xml
<dependency>
    <groupId>io.trino</groupId>
    <artifactId>trino-jdbc</artifactId>
</dependency>
```

## YugabyteDB

```xml
<dependency>
    <groupId>com.yugabyte</groupId>
    <artifactId>jdbc-yugabytedb</artifactId>
</dependency>
```

---

커넥션 URL과 드라이버 클래스 이름에 대해서는 이전 포스트를 참고하세요:

> [가장 인기 있는 RDBMS의 JDBC 커넥션 URL](https://blog.jooq.org/jdbc-connection-urls-of-the-most-popular-rdbms/)
