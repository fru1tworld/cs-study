# 프로덕션 사용자에게 ALL PRIVILEGES를 부여하지 마라

> 원문: https://blog.jooq.org/do-not-grant-all-privileges-to-your-production-users/

jOOQ 3.11은 Timur Shaidullin의 기여 덕분에 이슈 #6812를 통해 `GRANT`와 `REVOKE` 문을 지원할 예정입니다. 통합 테스트를 구현하면서, 저는 이 문들이 다양한 데이터베이스에서 어떻게 작동하는지 조사했고, 대부분 표준화되어 있으며 SQL 표준의 일부라는 것을 발견했습니다.

## MySQL 사용자 생성과 접근 문제

MySQL에서 다음 명령으로 사용자를 생성했습니다:

```sql
CREATE USER 'NO_RIGHTS'@'%' IDENTIFIED BY 'NO_RIGHTS';
```

`jdbc:mysql://host/database` 연결 문자열을 사용하여 JDBC로 연결을 시도하면 다음과 같은 오류가 발생합니다:

```
Caused by: java.sql.SQLSyntaxErrorException: Access denied for user 'NO_RIGHTS'@'%' to database 'test'
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:112)
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:89)
	at com.mysql.cj.jdbc.exceptions.SQLExceptionsMapping.translateException(SQLExceptionsMapping.java:116)
	at com.mysql.cj.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:853)
	at com.mysql.cj.jdbc.ConnectionImpl.(ConnectionImpl.java:440)
	at com.mysql.cj.jdbc.ConnectionImpl.getInstance(ConnectionImpl.java:241)
	at com.mysql.cj.jdbc.NonRegisteringDriver.connect(NonRegisteringDriver.java:221)
	at org.jooq.test.jOOQAbstractTest.getConnection1(jOOQAbstractTest.java:1132)
	at org.jooq.test.jOOQAbstractTest.getConnection0(jOOQAbstractTest.java:1064)
	... 31 more
```

## 누락된 CONNECT 권한

데이터베이스에 기본 연결 권한을 부여하는 문서화된 방법이 없습니다. 원하는 명령은 다음과 같을 것입니다:

```sql
GRANT CONNECT ON test TO 'NO_RIGHTS'@'%';
```

이에 대한 기능 요청이 제출되었습니다: https://bugs.mysql.com/bug.php?id=89030

## 우회 해결책

저는 우아하지는 않지만 작동하는 우회 방법을 제시합니다:

```sql
CREATE VIEW v_unused AS SELECT 1;
GRANT SHOW VIEW ON test.v_unused TO 'NO_RIGHTS'@'%';
```

이 접근 방식은 더미 뷰에 "SHOW VIEW" 권한만(SELECT 권한조차 아닌) 부여하며, 이것이 암묵적으로 "CONNECT" 권한을 부여합니다. 그 후 `SHOW TABLES;`를 실행하면 다음과 같은 결과가 나옵니다:

```
Tables_in_test
--------------
v_unused
```

## 인터넷 검색 분석

저는 "Access denied for user 'NO_RIGHTS'@'%' to database 'test'" 오류 메시지를 Google에서 검색하고 상위 결과를 분석했습니다. 다음은 검토한 8개의 출처입니다:

### 1. MySQL 매뉴얼

권장: "GRANT ALL PRIVILEGES ON \*.\* TO 'finley'@'localhost' WITH GRANT OPTION;"

### 2. 임의의 포럼 (Zabbix)

권장:

```sql
CREATE DATABASE `zabbix_db`;
GRANT ALL PRIVILEGES ON `zabbix_db`.* TO `zabbix_user`@'localhost' IDENTIFIED BY 'XXXXXXXXX';
FLUSH PRIVILEGES;
```

### 3. ServerFault 질문

다음과 같은 변형을 제안하는 두 개의 답변:

```sql
CREATE USER 'username'@'192.168.22.2' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON databasename.* TO 'username'@'192.168.22.2';
```

그리고: "GRANT ALL on database.\* to 'user'@'localhost' identified by 'paswword';"

### 4. cPanel 포럼

```sql
GRANT USAGE ON *.* TO 'someusr'@'localhost'
GRANT ALL PRIVILEGES ON `qorbit_store`.* TO 'someusr'@'localhost'
```

### 5. GitHub Travis CI 이슈

```sql
mysql -u root -e "CREATE DATABASE mydb;"
mysql -u root -e "GRANT ALL PRIVILEGES ON mydb.* TO 'travis'@'%';"
```

### 6. MariaDB 포럼

권장: "grant all privileges on newdb.\* to sam@localhost;"

### 7. Geeklog 포럼

과도하게 허용적인 조언과 더 제한적인 조언 모두를 제공합니다. 허용적인 버전:

```sql
GRANT ALL PRIVILEGES ON database_name TO user@host IDENTIFIED BY 'password';
FLUSH PRIVILEGES;
```

그런 다음 더 제한적인 권한은 다음이 필요하다고 언급합니다: "ALTER, CREATE, DELETE, INSERT, SELECT, UPDATE 권한"

### 8. Drupal 포럼

마스킹되지 않은 비밀번호와 함께 GRANT ALL을 포함:

```sql
$grt = "GRANT ALL ON *.* TO 'swhisa_swhisa'@'%'";
mysql_query($grt) or die(mysql_error());
```

## 핵심 관찰

거의 모든 검색 결과가 최소 권한 접근 방식에 대한 논의 없이 `GRANT ALL`을 권장합니다. 최소 권한으로 시작하고 필요에 따라 권한을 추가하라고 권장하는 게시물은 단 하나도 없었습니다.

## 결론 및 권장 사항

이 접근 방식을 무관한 해결책에 비유하며, 저자는 다음과 같이 말합니다: "항상 권한이 없는 사용자로 시작하세요 - 그런 다음 필요에 따라 권한을 추가하되, 아껴서 추가하세요!"

이 글에는 유머러스한 메모가 포함되어 있습니다: "이제 나가서 REVOKE를 시작하세요 (모두가 크리스마스 휴가를 떠난 후까지 기다리는 것이 좋겠지만요 ;)"
