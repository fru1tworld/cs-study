# 궁극의 SQL-DSL: Scala에서의 jOOQ

> 원문: https://blog.jooq.org/the-ultimate-sql-dsl-jooq-in-scala/

최근 Eclipse용 Scala IDE의 새로운 버전에 대한 광고를 접하게 되었는데, 이것이 Scala 언어의 탄생지인 EPFL의 Laboratoire des Methodes de Programmation(LAMP)에서 받았던 대학 프로그래밍 수업을 떠올리게 했습니다. 당시 Scala는 꽤 괴상하게 보였습니다. 매우 우아하고, 다소 비효율적이며, 어느 정도 독단적이었죠. 제 기억으로는 객체 지향적이기보다 훨씬 더 함수형에 가까웠고, Martin Odersky는 성공의 열쇠가 두 패러다임을 결합하는 것이라는 점에 동의하기 어려워했습니다. 하지만 Scala는 지난 8년간 먼 길을 걸어왔습니다. 그래서 jOOQ가 Scala로 이식 가능한지 궁금해졌습니다. 그 답은 저를 놀라게 합니다:

jOOQ는 100% Scala에 대응합니다!!

물론, 이것은 jOOQ의 플루언트 API만의 덕분이 아닙니다. 대부분은 Scala가 Java 위에 구축된 방식 덕분입니다. 다음 샘플 코드를 확인해 보세요:

```scala
package org.jooq.scala

import java.sql.Connection
import java.sql.DriverManager
import scala.collection.JavaConversions._
import org.jooq.impl.Factory._
import org.jooq.util.maven.example.mysql.Test2Factory
import org.jooq.util.maven.example.mysql.Tables._

object Test {
  def main(args: Array[String]) {
    Class.forName("com.mysql.jdbc.Driver");
    val connection = DriverManager.getConnection(
      "jdbc:mysql://localhost/test", "root", "");
    val create = new Test2Factory(connection);

    val result = (create
      select (
          T_BOOK.TITLE as "book title",
          T_AUTHOR.FIRST_NAME as "author's first name",
          T_AUTHOR.LAST_NAME as "author's last name")
      from T_AUTHOR
      join T_BOOK on (T_AUTHOR.ID equal T_BOOK.AUTHOR_ID)
      where (T_AUTHOR.ID in (1, 2, 3))
      orderBy (T_AUTHOR.LAST_NAME asc) fetch)

    println(result)

    for (r <- (create
               select (T_AUTHOR.FIRST_NAME, T_AUTHOR.LAST_NAME, count)
               from T_AUTHOR
               join T_BOOK on (T_AUTHOR.ID equal T_BOOK.AUTHOR_ID)
               where (T_AUTHOR.ID in (1, 2, 3))
               groupBy (T_AUTHOR.FIRST_NAME, T_AUTHOR.LAST_NAME)
               orderBy (T_AUTHOR.LAST_NAME asc)
               fetch)) {
      print(r.getValue(T_AUTHOR.FIRST_NAME))
      print(" ")
      print(r.getValue(T_AUTHOR.LAST_NAME))
      print(" wrote ")
      print(r.getValue(count))
      println(" books ")
    }
  }
}
```

예상대로, 콘솔에는 다음 데이터가 출력됩니다:

```
+------------+-------------------+------------------+
|book title  |author's first name|author's last name|
+------------+-------------------+------------------+
|O Alquimista|Paulo              |Coelho            |
|Brida       |Paulo              |Coelho            |
|1984        |George             |Orwell            |
|Animal Farm |George             |Orwell            |
+------------+-------------------+------------------+

Paulo Coelho wrote 2 books
George Orwell wrote 2 books
```

## 2-in-1을 얻을 수 있습니다

Scala에서 jOOQ의 플루언트 API는 Java에서보다 더욱 SQL처럼 보입니다. 그리고 2-in-1을 얻게 됩니다:

1. 타입 안전 쿼리 - SQL 구문이 컴파일된다는 의미
2. 타입 안전 쿼리 - 데이터베이스 스키마가 코드의 일부가 된다는 의미

제가 지금까지 볼 수 있는 가장 큰 단점은, Scala에 `val`과 같은 새로운 예약어가 있다는 것인데, 이것은 jOOQ에서 매우 중요한 메서드입니다. 어떻게든 해결할 수 있을 것이라 생각합니다. Scala 사용자와 SQL 애호가 여러분! 부디! 피드백을 주세요 :-)
