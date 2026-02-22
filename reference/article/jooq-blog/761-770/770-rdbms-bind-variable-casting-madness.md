# RDBMS 바인드 변수 캐스팅 광기

> 원문: https://blog.jooq.org/rdbms-bind-variable-casting-madness/

2011년 8월 31일, lukaseder 작성

_위대한 Oracle Database_ TM에서 매일 작업하는 데 익숙해진 나는, jOOQ에서 Derby, HSQLDB, DB2 등의 RDBMS를 지원하기 시작하려 했을 때 처음으로 꽤나 창의력을 발휘해야 했다. Oracle은 여러분에게서 힘든 작업을 덜어주는 데 정말로 훌륭한 역할을 한다. 바인드 변수 타입은 최적의 쿼리 실행 계획과 성능을 찾기 위해 쿼리 실행의 여러 시점에서 평가된다. 이 시점들은 다음과 같이 구분할 수 있다:

1. SQL 파싱 시점: SQL 문이 처음 파싱될 때 이미 많은 정보를 사용할 수 있다. 이는 가장 최적의 실행 계획으로 이어진다. 대부분의 RDBMS는 이를 어느 정도 수행한다.

2. 바인딩 시점: 변수를 바인딩할 때, JDBC 드라이버는 `java.lang.Integer`가 `NUMBER(10)`으로, 또는 `java.lang.String`이 `VARCHAR2`로 가장 잘 표현된다는 것을 평가하는 데 그다지 똑똑할 필요가 없다. 바인드 변수 엿보기(bind-variable peeking)가 활성화되어 있다면, 바인드 변수 타입을 반드시 쉽게 추론할 수 없는 경우에도 실행 계획은 여전히 꽤 좋을 수 있다.

3. 실행 시점: 다른 모든 방법이 실패하면, 커서를 순회하는 동안에도 발견된 데이터에 대한 검사를 수행할 수 있다. 실행 계획에 영향을 미치기에는 너무 늦지만, 데이터 무결성을 유지하는 데에는 늦지 않다.

그래서 나는 다음 준비된 문(prepared statement)들 중 어떤 것이라도 바인드 변수 캐스팅이 필요할 것이라고는 거의 상상할 수 없었다:

```sql
-- SQL 파싱 시점에서 바인드 타입을 쉽게 추론하거나 최적의 기본 캐스트 가능
INSERT INTO table (field1, field2) VALUES (?, ?)
INSERT INTO table (field1, field2) SELECT ?, ? FROM DUAL
UPDATE table SET field1 = ?, field2 = ?
SELECT * FROM table WHERE field1 = ? AND field2 = ?
SELECT field1 * ? / ? FROM table
SELECT field1 || ? || field2 FROM table

-- 바인딩 시점에서도 바인드 타입 추론 가능
SELECT ?, ? FROM DUAL

-- 기타...
```

하지만 여러분 중 일부는 DB2와 Derby가 구현하기로 선택한 초강력 타입 시스템의 깊이를 충분히 잘 알고 있을 것이다. 예를 들어 DB2에서는 다음 두 값이 매우 다르다:

```
cast(null as char(3))
cast(null as integer)
```

예를 들어 많은 RDBMS에서 다음은 작동하지 않는다

```sql
-- 작동하지 않음
INSERT INTO table (field1, field2) SELECT ?, ? FROM DUAL
-- 다음과 같이 작성해야 함
INSERT INTO table (field1, field2) SELECT cast(? as char(3)), cast(? as integer) FROM DUAL
```

범용 데이터베이스 추상화 프레임워크에게 있어 이것을 jOOQ가 목표로 하는 방식대로 클라이언트 코드로부터 숨기는 것은 꽤 도전적인 일이 될 수 있다. 그래서 여기 이 캐스팅 광기에 대한 개요와 각 RDBMS가 어떻게 분류될 수 있는지를 정리했다:

### 친절한 RDBMS

이 RDBMS들은 나의 친구들이다. 강력하고, 풍부한 기능을 제공하며, 모든 종류의 데이터 타입을 추론하므로 바인드 변수를 캐스팅할 필요가 없다. 이 녀석들에게 박수를 보내자:

* MySQL (*)
* Postgres
* Oracle
* SQL Server
* SQLite ()

(*) MySQL은 약간 특별하다. 어떤 알 수 없는 이유로, `CAST()` 함수에서 사용되는 타입이 테이블의 DDL 문에서 정의된 타입과 다르다. 이에 대한 [문서](https://dev.mysql.com/doc/refman/5.5/en/cast-functions.html#function_cast)를 참조하라.

() SQLite는 매우 특별하다. 음, 그들은 그냥 신경 쓰지 않는다. `NUMERIC` 컬럼에 `VARCHAR`를 넣고 싶다고? 문제없다. `DATE` 컬럼에 `BOOLEAN`을? 더 좋다! 그들은 이것을 "[타입 친화성(type affinity)](https://www.sqlite.org/datatype3.html)"이라고 부른다.

### 친절한 RDBMS, 다만 가끔 JDBC 버그가 있는

매우 안타깝게도, 이 JDBC 드라이버들은 꽤 복잡한 SQL(예: JOIN 절 내의 중첩된 SELECT)에서 캐스팅되지 않은 변수 바인딩과 관련하여 1~2개의 버그를 가지고 있는 것으로 보인다. 그래서 안전을 위해 캐스팅하는 것이 더 낫다.

* Ingres
* Sybase SQL Anywhere

### 까다로운 것들

이들은 많은 추론을 수행한다. 하지만 동시에 의도적으로 추론을 생략하는 경우도 많다. 이 녀석들에 대해서는 다음 쿼리에서 캐스트를 생략할 수 있을지 절대 확신할 수 없다:

* H2
* HSQLDB

나는 최근 캐스팅과 관련하여 H2와 HSQLDB에 여러 버그를 보고했다. 그 중 약 50%가 "설계대로 작동함(works-as-designed)"으로 거부되었다. 그래서 앞으로 몇 년간은 "까다로운" 상태로 남을 것 같다. 또한 흥미로운 점: H2는 HSQLDB의 전직 개발자가 개발했다([H2 역사](http://www.h2database.com/html/history.html#history)에 대해 읽어보라). 대대적인 재설계 이후에도 여전히 매우 유사하다. 하지만 캐스팅이 필요한 부분은 완전히 다르다. "의도적으로" 타입 추론을 생략했다는 그들의 주장에 눈을 감아주자 ;-)

### 길 잃은 것들

이 RDBMS들은 거의 어떤 타입도 추론하지 않는다. 사실, 연관된 타입이 없는 바인드 변수는 많은 상황에서 완전히 무의미하다. DB2 v9.7까지는, 리터럴 `null`조차도 연관된 타입 없이는 의미가 없었다. 이것은 데이터베이스 추상화 레이어에서 처리하기가 매우 어렵다. Java 관점에서 `null`은 실제로 값이 아니기 때문이다. 엄격한 것들은 다음과 같다:

* Derby
* DB2

많은 경우에, jOOQ를 사용할 때조차도, 타입을 올바르게 맞추기 위해 jOOQ 캐스트 API를 광범위하게 사용해야 할 것이다... 감히 할 수 있다면 말이다. 왜냐하면 Derby에서는 `INTEGER`에서 `VARCHAR`로의 간단한 캐스트조차 먼저 `CHAR`로 캐스팅하지 않으면 실행할 수 없기 때문이다. [지옥의 변환 테이블](https://db.apache.org/derby/docs/10.8/ref/rrefsqlj33562.html)을 참조하라.
