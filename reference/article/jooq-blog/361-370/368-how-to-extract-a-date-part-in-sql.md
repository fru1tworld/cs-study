# SQL에서 날짜 부분을 추출하는 방법

> 원문: https://blog.jooq.org/how-to-extract-a-date-part-in-sql/

Modern SQL 트위터 계정(@ModernSQL, Markus Winand가 운영)에서 최근 다음과 같은 내용을 게시했습니다:

> "날짜/시간의 일부를 얻는 올바른 방법은: EXTRACT(YEAR FROM CURRENT_DATE) = 2015"

네, 표준은... 그것이 철저하게 구현되기만 한다면...

jOOQ가 지원하는 18개의 RDBMS 전체에서 EXTRACT가 어떻게 렌더링되는지 살펴봅시다. 다음은 각 SQL 방언과 DatePart에 대해 jOOQ가 생성하는 SQL 표현식의 자바 프로그램입니다:

```java
import static org.jooq.impl.DSL.currentDate;
import static org.jooq.impl.DSL.extract;
import static org.jooq.impl.DSL.using;

import java.util.stream.Stream;

import org.jooq.DatePart;
import org.jooq.SQLDialect;

public class Extract {
    public static void main(String[] args) {
        Stream
        .of(SQLDialect.values())
        .map(SQLDialect::family)
        .distinct()
        .forEach(family -> {
            System.out.println();
            System.out.println(family);

            Stream
            .of(DatePart.values())
            .map(part -> extract(currentDate(), part))
            .forEach(expr -> {
                System.out.println(
                    using(family).render(expr)
                );
            });
        });
    }
}
```

위 프로그램의 출력은 다음과 같습니다(jOOQ 버전 3.5 또는 3.6에 따라 다소 다름):

```
DEFAULT
extract(year from current_date())
extract(month from current_date())
extract(day from current_date())
extract(hour from current_date())
extract(minute from current_date())
extract(second from current_date())

CUBRID
extract(year from current_date())
extract(month from current_date())
extract(day from current_date())
extract(hour from current_date())
extract(minute from current_date())
extract(second from current_date())

DERBY
year(current_date)
month(current_date)
day(current_date)
hour(current_date)
minute(current_date)
second(current_date)

FIREBIRD
extract(year from current_date)
extract(month from current_date)
extract(day from current_date)
extract(hour from current_date)
extract(minute from current_date)
extract(second from current_date)

H2
extract(year from current_date())
extract(month from current_date())
extract(day from current_date())
extract(hour from current_date())
extract(minute from current_date())
extract(second from current_date())

HSQLDB
extract(year from current_date)
extract(month from current_date)
extract(day from current_date)
extract(hour from current_date)
extract(minute from current_date)
extract(second from current_date)

MARIADB
extract(year from current_date())
extract(month from current_date())
extract(day from current_date())
extract(hour from current_date())
extract(minute from current_date())
extract(second from current_date())

MYSQL
extract(year from current_date())
extract(month from current_date())
extract(day from current_date())
extract(hour from current_date())
extract(minute from current_date())
extract(second from current_date())

POSTGRES
extract(year from current_date)
extract(month from current_date)
extract(day from current_date)
extract(hour from current_date)
extract(minute from current_date)
extract(second from current_date)

SQLITE
strftime('%Y', current_date)
strftime('%m', current_date)
strftime('%d', current_date)
strftime('%H', current_date)
strftime('%M', current_date)
strftime('%S', current_date)
```

위에서 보듯이, 대부분의 오픈 소스 데이터베이스는 표준 EXTRACT 구문을 지원합니다. 예외적으로 Derby는 함수 기반 접근 방식을 사용하고, SQLite는 strftime() 함수를 사용합니다.

```
ACCESS
datepart('yyyy', date())
datepart('m', date())
datepart('d', date())
datepart('h', date())
datepart('n', date())
datepart('s', date())

ASE
datepart(yy, current_date())
datepart(mm, current_date())
datepart(dd, current_date())
datepart(hh, current_date())
datepart(mi, current_date())
datepart(ss, current_date())

DB2
year(current_date)
month(current_date)
day(current_date)
hour(current_date)
minute(current_date)
second(current_date)

INGRES
date_part('year', current_date)
date_part('month', current_date)
date_part('day', current_date)
date_part('hour', current_date)
date_part('minute', current_date)
date_part('second', current_date)

ORACLE (3.5 기준)
to_char(trunc(sysdate), 'YYYY')
to_char(trunc(sysdate), 'MM')
to_char(trunc(sysdate), 'DD')
to_char(trunc(sysdate), 'HH24')
to_char(trunc(sysdate), 'MI')
to_char(trunc(sysdate), 'SS')

ORACLE (3.6 기준)
extract(year from current_date)
extract(month from current_date)
extract(day from current_date)
extract(hour from current_date)
extract(minute from current_date)
extract(second from current_date)

SQLSERVER
datepart(yy, convert(date, current_timestamp))
datepart(mm, convert(date, current_timestamp))
datepart(dd, convert(date, current_timestamp))
datepart(hh, convert(date, current_timestamp))
datepart(mi, convert(date, current_timestamp))
datepart(ss, convert(date, current_timestamp))

SYBASE
datepart(yy, current date)
datepart(mm, current date)
datepart(dd, current date)
datepart(hh, current date)
datepart(mi, current date)
datepart(ss, current date)
```

보시다시피, 상용 데이터베이스들은 EXTRACT 함수 구현에 있어서 훨씬 더 다양합니다:

- Access, ASE, SQL Server, Sybase는 DATEPART 함수를 사용합니다
- DB2는 Derby와 마찬가지로 함수 기반 접근 방식(YEAR(), MONTH() 등)을 사용합니다
- Ingres는 DATE_PART 함수를 사용합니다
- Oracle은 jOOQ 3.5에서는 TO_CHAR 함수를 사용했지만, jOOQ 3.6에서는 표준 EXTRACT 구문으로 변경되었습니다

## 결론

이것이 바로 jOOQ와 같은 도구가 필요한 이유입니다. SQL 표준이 존재하지만, 실제 데이터베이스 벤더들의 구현은 상당히 다양합니다. jOOQ는 이러한 모든 방언의 차이를 추상화하여, 개발자가 데이터베이스에 독립적인 코드를 작성할 수 있도록 해줍니다. 한 번의 자바 코드로 18개의 서로 다른 데이터베이스에서 올바르게 동작하는 SQL을 생성할 수 있습니다.

XKCD가 말했듯이, 표준은 좋은 것이지만... 그것이 모든 곳에서 동일하게 구현되기만 한다면 말이죠.
