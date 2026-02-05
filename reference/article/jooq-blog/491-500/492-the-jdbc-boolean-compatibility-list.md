# JDBC 불리언 호환성 목록

> 원문: https://blog.jooq.org/the-jdbc-boolean-compatibility-list/

흥미롭게도, 불리언 타입은 SQL 표준에서 비교적 늦게 도입되었습니다. 구체적으로 SQL:1999에서 도입되었습니다. 오늘날에도 모든 데이터베이스가 `BOOLEAN`이나 `BIT` 타입을 네이티브로 지원하지는 않습니다. 가장 중요한 점은, Oracle에서 불리언 타입을 지원받으려면 아직 한참을 기다려야 한다는 것입니다. 이 주제에 대한 2002년의 "Ask Tom" 관점은 여기서 확인할 수 있습니다: https://asktom.oracle.com/pls/apex/f?p=100:11:0::::P11_QUESTION_ID:6263249199595

> Oracle. 왜 불리언이 없는 거야?

사람들은 이 제한을 숫자나 문자열 리터럴을 사용하여 우회해왔습니다. 예를 들어 `1 / 0`, `Y / N`, `T / F` 또는 SQL 표준인 `'true' / 'false'` 같은 것들입니다.

## JDBC에서의 불리언

JDBC API 관점에서, 불리언 값은 `PreparedStatement.setBoolean()`을 통해 바인드 값으로 설정하거나 `ResultSet.getBoolean()` 및 유사한 메서드를 통해 결과 집합에서 가져올 수 있습니다. 데이터베이스가 불리언을 지원한다면, Java의 `boolean` 타입은 SQL `BOOLEAN`에 잘 매핑됩니다 - `NULL`을 존중하려면 Java의 `Boolean` 래퍼 타입이 더 적합했겠지만요. 그러나 불리언 값을 `INTEGER`, `CHAR(1)` 또는 `VARCHAR(1)` 컬럼에 저장하는 경우, 다양한 데이터베이스에서 상황이 다르게 보입니다. 다음 예제를 살펴보세요:

```sql
CREATE TABLE booleans (
  val char(1)
);
```

그런 다음, 이 Java 프로그램을 실행합니다 (간결하게 유지하기 위해 jOOQ를 사용합니다):

```java
try {
    DSL.using(configuration)
       .execute(
       "insert into boolean (val) values (?)", true);
}
catch (Exception e) {
    e.printStackTrace();
}

DSL.using(configuration)
   .fetch("select * from booleans");
```

모든 데이터베이스/JDBC 드라이버가 위의 코드를 지원하는 것은 아닙니다. 다음 데이터베이스들은 위의 프로그램을 실행합니다:

- Firebird ('Y' 또는 'N'을 삽입)
- HSQLDB ('1' 또는 '0'을 삽입)
- IBM DB2 ('1' 또는 '0'을 삽입)
- MariaDB ('1' 또는 '0'을 삽입)
- Microsoft Access ('1' 또는 '0'을 삽입)
- MySQL ('1' 또는 '0'을 삽입)
- Oracle ('1' 또는 '0'을 삽입)
- SQL Server ('1' 또는 '0'을 삽입)
- Sybase ('1' 또는 '0'을 삽입)

... 반면에 다음 데이터베이스들은 예외를 발생시킵니다:

- CUBRID
- Derby
- H2
- Ingres
- PostgreSQL
- SQLite

## SQL 표준에서의 불리언

SQL 표준이 `CAST()` 함수의 명세에서 `boolean`을 문자열로 변환하는 방법을 지정하고 있다는 점을 언급할 가치가 있습니다:

> 6.13 <cast specification>
> [...]
> 10) TD가 고정 길이 문자열이면,
>    LTD를 TD의 문자 길이라 하자.
> [...]
> e) SD가 boolean이면,
> 경우:
> i) SV가 True이고 LTD가 4보다 작지 않으면,
>    TV는 'TRUE'를 오른쪽으로 LTD-4개의
>    공백으로 확장한 것이다.
> ii) SV가 False이고 LTD가 5보다 작지 않으면,
>    TV는 'FALSE'를 오른쪽으로 LTD-5개의 공백으로 확장한 것이다.
> iii) 그렇지 않으면, 예외 조건이 발생한다:
>    데이터 예외 - 캐스트에 대한 잘못된 문자 값.

따라서, 대부분의 오픈 소스 데이터베이스들은 "올바른" 동작으로 해석될 수 있는 것을 보여줍니다. 역사적 관점에서 1/0이 허용되는 동작이어야 하지만요. 오픈 소스 테스트 데이터베이스를 사용할 때 이 제한에 주의하세요! 이 내용과 H2 데이터베이스에 대한 더 많은 정보는 H2 사용자 그룹의 이 스레드를 참조하세요.
