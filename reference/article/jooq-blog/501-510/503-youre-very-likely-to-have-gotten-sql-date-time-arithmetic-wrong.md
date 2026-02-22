# SQL 날짜 시간 산술을 잘못 이해했을 가능성이 매우 높다!

> 원문: https://blog.jooq.org/youre-very-likely-to-have-gotten-sql-date-time-arithmetic-wrong/

SQL에서 날짜/시간 연산에 관해 검색하면, 대부분 이런 류의 블로그 글에서 조언을 얻게 됩니다:

- [Oracle 9/10에서의 날짜/시간 산술](http://www.akadia.com/services/ora_date_time.html)
- [날짜에 시간 단위를 더하는 방법에 대한 FAQ](http://www.orafaq.com/wiki/FAQ:_Adding_hours,_minutes,_seconds_to_a_date)
- [Oracle 날짜 산술 팁](http://www.dba-oracle.com/t_date_arithmetic.htm)

그리고 사람들이 이런 조언을 따르면, [이런 끔찍한 일](http://www.javaadvent.com/2012/12/what-date-is-dec.html)이 벌어집니다:

```sql
SELECT TO_CHAR(hiredate,'DD.MM.YYYY:HH24:MI:SS') "Hiredate",
       TO_CHAR(&Today,'DD.MM.YYYY:HH24:MI:SS') "Today",
       trunc(86400*(&Today-hiredate))-60*(trunc((86400*(&&Today-hiredate))/60)) "Sec",
       trunc((86400*(&Today-hiredate))/60)-60*(trunc(((86400*(&&Today-hiredate))/60)/60)) "Min",
       trunc(((86400*(&Today-hiredate))/60)/60)-24*(trunc((((86400*(&&Today-hiredate))/60)/60)/24)) "Hrs",
       trunc((((86400*(&Today-hiredate))/60)/60)/24) "Days"
FROM emp;
```

## 분수 일(Fractional Days)을 사용한 날짜 시간 산술을 절대 하지 마세요!

위 예제에서 사용한 `86400` 초를 세는 방식이나, 10분을 더하기 위해 `SYSDATE + (10/1440)`을 쓰는 접근법은 기본적으로 잘못된 것입니다. 여러분은 다음과 같은 것들을 고려하지 않았습니다:

- 시간대(Timezones)
- 일광 절약 시간(Daylight savings time)
- 윤초(Leap seconds)
- 기타 복잡한 요소들

그냥 내장 함수와/또는 인터벌 데이터 타입을 사용하세요. 항상요!

## 모든 데이터베이스별 차이를 기억할 수 없습니다!

네, 저도 압니다. 모든 데이터베이스마다 다른 함수와 구문을 사용하니까요. 하지만 그건 SQL의 다른 부분도 마찬가지입니다. jOOQ가 이런 표준화 작업을 도와드립니다. `timestampAdd()` 함수를 살펴보세요:

```java
import static org.jooq.DatePart.DAY;
import static org.jooq.DatePart.HOUR;
import static org.jooq.DatePart.MINUTE;
import static org.jooq.DatePart.MONTH;
import static org.jooq.DatePart.SECOND;
import static org.jooq.DatePart.YEAR;
import static org.jooq.SQLDialect.INGRES;
import static org.jooq.SQLDialect.SQL99;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.timestampAdd;
import static org.jooq.impl.DSL.using;

import java.sql.Timestamp;
import java.util.EnumSet;

import org.jooq.QueryPart;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;

public class Compatibility {

    public static void main(String[] args) {
        Timestamp t = new Timestamp(0);

        print(select(
            timestampAdd(t, 2, YEAR)  .as("yy"),
            timestampAdd(t, 2, MONTH) .as("mm"),
            timestampAdd(t, 2, DAY)   .as("dd"),
            timestampAdd(t, 2, HOUR)  .as("hh"),
            timestampAdd(t, 2, MINUTE).as("mi"),
            timestampAdd(t, 2, SECOND).as("ss")
        ));
    }

    private static void print(QueryPart part) {
        System.out.println("Printing " + part);
        System.out.println("---------------------");

        EnumSet<SQLDialect> dialects =
            EnumSet.noneOf(SQLDialect.class);
        for (SQLDialect dialect:SQLDialect.values())
            if (dialect != SQL99 && dialect != INGRES)
                dialects.add(dialect.family());

        for (SQLDialect dialect: dialects)
            System.out.println(
                String.format("%1$s: \n%2$s\n",
                dialect, using(dialect, new Settings()
                         .withRenderFormatted(true))
                         .renderInlined(part)
            ));

        System.out.println();
        System.out.println();
    }
}
```

위 프로그램은 다양한 데이터베이스 방언에 대해 다음과 같은 SQL을 생성합니다:

### CUBRID:

```sql
select
  date_add(datetime '1970-01-01 01:00:00.0', interval 2 year) "yy",
  date_add(datetime '1970-01-01 01:00:00.0', interval 2 month) "mm",
  date_add(datetime '1970-01-01 01:00:00.0', interval 2 day) "dd",
  date_add(datetime '1970-01-01 01:00:00.0', interval 2 hour) "hh",
  date_add(datetime '1970-01-01 01:00:00.0', interval 2 minute) "mi",
  date_add(datetime '1970-01-01 01:00:00.0', interval 2 second) "ss"
from "db_root"
```

### Derby:

```sql
select
  {fn timestampadd(sql_tsi_year, 2, timestamp('1970-01-01 01:00:00.0')) } as "yy",
  {fn timestampadd(sql_tsi_month, 2, timestamp('1970-01-01 01:00:00.0')) } as "mm",
  {fn timestampadd(sql_tsi_day, 2, timestamp('1970-01-01 01:00:00.0')) } as "dd",
  {fn timestampadd(sql_tsi_hour, 2, timestamp('1970-01-01 01:00:00.0')) } as "hh",
  {fn timestampadd(sql_tsi_minute, 2, timestamp('1970-01-01 01:00:00.0')) } as "mi",
  {fn timestampadd(sql_tsi_second, 2, timestamp('1970-01-01 01:00:00.0')) } as "ss"
from "SYSIBM"."SYSDUMMY1"
```

### Firebird:

```sql
select
  dateadd(year, 2, timestamp '1970-01-01 01:00:00.0') "yy",
  dateadd(month, 2, timestamp '1970-01-01 01:00:00.0') "mm",
  dateadd(day, 2, timestamp '1970-01-01 01:00:00.0') "dd",
  dateadd(hour, 2, timestamp '1970-01-01 01:00:00.0') "hh",
  dateadd(minute, 2, timestamp '1970-01-01 01:00:00.0') "mi",
  dateadd(second, 2, timestamp '1970-01-01 01:00:00.0') "ss"
from "RDB$DATABASE"
```

### H2:

```sql
select
  dateadd('year', 2, timestamp '1970-01-01 01:00:00.0') "yy",
  dateadd('month', 2, timestamp '1970-01-01 01:00:00.0') "mm",
  dateadd('day', 2, timestamp '1970-01-01 01:00:00.0') "dd",
  dateadd('hour', 2, timestamp '1970-01-01 01:00:00.0') "hh",
  dateadd('minute', 2, timestamp '1970-01-01 01:00:00.0') "mi",
  dateadd('second', 2, timestamp '1970-01-01 01:00:00.0') "ss"
from dual
```

### HSQLDB:

```sql
select
  {fn timestampadd(sql_tsi_year, 2, timestamp '1970-01-01 01:00:00.0') } as "yy",
  {fn timestampadd(sql_tsi_month, 2, timestamp '1970-01-01 01:00:00.0') } as "mm",
  {fn timestampadd(sql_tsi_day, 2, timestamp '1970-01-01 01:00:00.0') } as "dd",
  {fn timestampadd(sql_tsi_hour, 2, timestamp '1970-01-01 01:00:00.0') } as "hh",
  {fn timestampadd(sql_tsi_minute, 2, timestamp '1970-01-01 01:00:00.0') } as "mi",
  {fn timestampadd(sql_tsi_second, 2, timestamp '1970-01-01 01:00:00.0') } as "ss"
from "INFORMATION_SCHEMA"."SYSTEM_USERS"
```

### MariaDB:

```sql
select
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 year) as `yy`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 month) as `mm`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 day) as `dd`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 hour) as `hh`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 minute) as `mi`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 second) as `ss`
from dual
```

### MySQL:

```sql
select
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 year) as `yy`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 month) as `mm`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 day) as `dd`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 hour) as `hh`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 minute) as `mi`,
  date_add(timestamp '1970-01-01 01:00:00.0', interval 2 second) as `ss`
from dual
```

### PostgreSQL:

```sql
select
  (timestamp '1970-01-01 01:00:00.0' + (2 || ' year')::interval) as "yy",
  (timestamp '1970-01-01 01:00:00.0' + (2 || ' month')::interval) as "mm",
  (timestamp '1970-01-01 01:00:00.0' + (2 || ' day')::interval) as "dd",
  (timestamp '1970-01-01 01:00:00.0' + (2 || ' hour')::interval) as "hh",
  (timestamp '1970-01-01 01:00:00.0' + (2 || ' minute')::interval) as "mi",
  (timestamp '1970-01-01 01:00:00.0' + (2 || ' second')::interval) as "ss"
```

### SQLite:

```sql
select
  datetime('1970-01-01 01:00:00.0', '+' || 2 || ' year') yy,
  datetime('1970-01-01 01:00:00.0', '+' || 2 || ' month') mm,
  datetime('1970-01-01 01:00:00.0', '+' || 2 || ' day') dd,
  datetime('1970-01-01 01:00:00.0', '+' || 2 || ' hour') hh,
  datetime('1970-01-01 01:00:00.0', '+' || 2 || ' minute') mi,
  datetime('1970-01-01 01:00:00.0', '+' || 2 || ' second') ss
```

### DB2:

```sql
select
  (timestamp '1970-01-01 01:00:00.0' + 2 year) "yy",
  (timestamp '1970-01-01 01:00:00.0' + 2 month) "mm",
  (timestamp '1970-01-01 01:00:00.0' + 2 day) "dd",
  (timestamp '1970-01-01 01:00:00.0' + 2 hour) "hh",
  (timestamp '1970-01-01 01:00:00.0' + 2 minute) "mi",
  (timestamp '1970-01-01 01:00:00.0' + 2 second) "ss"
from "SYSIBM"."DUAL"
```

### Oracle:

```sql
select
  (timestamp '1970-01-01 01:00:00.0' + numtoyminterval(2, 'year')) "yy",
  (timestamp '1970-01-01 01:00:00.0' + numtoyminterval(2, 'month')) "mm",
  (timestamp '1970-01-01 01:00:00.0' + numtodsinterval(2, 'day')) "dd",
  (timestamp '1970-01-01 01:00:00.0' + numtodsinterval(2, 'hour')) "hh",
  (timestamp '1970-01-01 01:00:00.0' + numtodsinterval(2, 'minute')) "mi",
  (timestamp '1970-01-01 01:00:00.0' + numtodsinterval(2, 'second')) "ss"
from dual
```

### SQL Server:

```sql
select
  dateadd(yy, 2, '1970-01-01 01:00:00.0') [yy],
  dateadd(mm, 2, '1970-01-01 01:00:00.0') [mm],
  dateadd(dd, 2, '1970-01-01 01:00:00.0') [dd],
  dateadd(hh, 2, '1970-01-01 01:00:00.0') [hh],
  dateadd(mi, 2, '1970-01-01 01:00:00.0') [mi],
  dateadd(ss, 2, '1970-01-01 01:00:00.0') [ss]
```

### Sybase (ASE):

```sql
select
  dateadd(yy, 2, '1970-01-01 01:00:00.0') [yy],
  dateadd(mm, 2, '1970-01-01 01:00:00.0') [mm],
  dateadd(dd, 2, '1970-01-01 01:00:00.0') [dd],
  dateadd(hh, 2, '1970-01-01 01:00:00.0') [hh],
  dateadd(mi, 2, '1970-01-01 01:00:00.0') [mi],
  dateadd(ss, 2, '1970-01-01 01:00:00.0') [ss]
from [SYS].[DUMMY]
```

보시다시피, 각 데이터베이스마다 고유한 방식으로 날짜/시간 산술을 처리합니다. Oracle은 `numtoyminterval()`과 `numtodsinterval()` 함수를, MySQL/MariaDB는 `date_add()`와 `interval` 구문을, PostgreSQL은 인터벌 캐스팅을, SQL Server/Sybase는 `dateadd()` 함수를, SQLite는 `datetime()`과 수정자 문자열을 사용합니다. Derby와 HSQLDB는 JDBC 이스케이프 구문을 활용합니다.

## 결론

분수 일을 사용한 수동 날짜 산술을 절대 하지 마세요. 대신 각 벤더별 내장 함수와 인터벌 데이터 타입을 사용하세요. jOOQ는 이러한 차이를 추상화하여 단일 API로 모든 데이터베이스에서 올바른 날짜/시간 산술을 수행할 수 있게 해줍니다.

항상 내장 함수와/또는 인터벌 데이터 타입을 사용하세요. 예외 없이요!

---

*이 글은 lukaseder가 2014년 2월 6일에 게시했습니다. (2014년 2월 7일 업데이트)*
