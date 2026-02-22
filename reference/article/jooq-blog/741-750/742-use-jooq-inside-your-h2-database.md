# H2 데이터베이스 안에서 jOOQ 사용하기
> 원문: https://blog.jooq.org/use-jooq-inside-your-h2-database/

2011년 11월 4일, lukaseder 작성

## H2 저장 함수(Stored Functions)

H2는 저장 함수에 대해 두 가지 동작 모드를 지원합니다:

1. 소스 코드를 직접 제공하는 "인라인 모드"
2. 데이터베이스 클래스패스에 있는 Java 클래스의 public static 메서드를 참조하는 "참조 모드"

자세한 내용은 [CREATE ALIAS](http://www.h2database.com/html/grammar.html#create_alias)와 [사용자 정의 함수](http://www.h2database.com/html/features.html#user_defined_functions)에 대한 H2 문서를 참조하세요.

동작 모드에 관계없이, H2 저장 함수는 Java로 작성됩니다. 현재 PL/SQL과 같은 절차적 언어는 없습니다. 함수에서 추가 데이터 처리를 위해 데이터베이스 접근이 필요한 경우, 개발자는 JDBC를 사용해야 하므로 저장 함수 개발이 장황해집니다.

## H2 저장 함수 내에서 jOOQ 사용하기

해결책은 H2 데이터베이스 함수 내에서 jOOQ를 사용하는 것입니다. 이 접근 방식은 컴파일 시점에 안전한 SQL 코드를 제공합니다.

### 저장 함수 작성하기

```java
package org.jooq.test.h2;

import static test.generated.Tables.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import test.generated.tables.TBook;

public class Functions {
  /
   * 이 함수는 주어진 저자가 작성한
   * 책의 수를 반환합니다.
   */
  public static int countBooks(
    Connection connection,
    Integer authorId)
  throws SQLException {
    return DSL.using(connection, SQLDialect.H2)
              .selectCount()
              .from(T_BOOK)
              .where(T_BOOK.AUTHOR_ID.eq(authorId))
              .fetchOne(0, int.class);
  }
}
```

### H2에 ALIAS로 메서드 선언하기

```sql
CREATE ALIAS countBooks
   FOR "org.jooq.test.h2.Functions.countBooks";
```

### SQL에서 함수 사용하기

```sql
select t_author.last_name, countBooks(id)
from t_author
```

### jOOQ의 생성된 클래스 사용하기

jOOQ는 저장 함수에 대해 컴파일 시점에 안전한 접근을 제공하는 Routines 클래스를 생성합니다:

```java
package org.jooq.test.h2;

import static test.generated.Tables.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import test.generated.Routines;

public class Test {
  public static void main(String[] args)
  throws SQLException {
    Connection connection =
      DriverManager.getConnection(
        "jdbc:h2:~/test", "sa", "");

    System.out.println(
    DSL.using(connection, SQLDialect.H2)
       .select(
             T_AUTHOR.LAST_NAME,
             Routines.countbooks(T_AUTHOR.ID))
       .from(T_AUTHOR)
       .fetch());
  }
}
```

출력 결과:

```
+---------+---------------------+
|LAST_NAME|"PUBLIC"."COUNTBOOKS"|
+---------+---------------------+
|Orwell   |                    2|
|Coelho   |                    2|
|Hesse    |                    0|
+---------+---------------------+
```

## 결론

jOOQ는 Java가 지원되는 곳이라면 어디서든 동작하며, 데이터베이스의 Java 저장 프로시저 및 함수 지원을 위한 PL/SQL과 같은 확장 역할을 합니다.
