# 간단한 SQL 표현식의 다양한 SQL 방언으로의 미친 번역

> 원문: https://blog.jooq.org/crazy-translations-of-simple-sql-expressions-to-various-sql-dialects/

jOOQ는 SQL을 표현하기 위한 자바 내부 DSL이며, 14개 이상의 RDBMS 벤더에서 동작합니다. 이것이 가능한 이유는 jOOQ가 AST(추상 구문 트리)를 내부적으로 유지하고, 실제 SQL 문자열을 생성하기 직전에 올바른 SQL 방언으로 표현식을 렌더링하기 때문입니다.

지원하는 벤더들 중 일부 API 기능이 네이티브 SQL로 사용 가능하지 않은 경우, jOOQ는 해당 SQL 표현식을 동등한 표현식으로 에뮬레이션합니다. 이것은 꽤 미친 짓이 될 수 있습니다! 이 글에서 그 중 일부를 보여드리겠습니다.

## sin(3)

가장 간단한 것부터 시작해봅시다: 사인 함수. 렌더링 결과는 다음과 같습니다:

```sql
-- Access, ASE, CUBRID, DB2, Derby, Firebird, H2, HANA, HSQLDB, Ingres, MariaDB, MySQL, Oracle, PostgreSQL, SQLite, SQL Server, Sybase SQL Anywhere, Vertica:
sin(3)
```

세상에, 모든 벤더에서 동일합니다! 하지만 너무 흥분하지 마세요. 모든 것이 이렇게 간단하지는 않습니다.

## power(2, 4)

거듭제곱 함수도 꽤 괜찮습니다:

```sql
-- Access, ASE, CUBRID, DB2, Firebird, H2, HANA, HSQLDB, Ingres, MariaDB, MySQL, Oracle, PostgreSQL, SQLite, SQL Server, Sybase SQL Anywhere, Vertica:
power(2, 4)

-- Derby:
exp((ln(2) * 4))
```

Derby는 `power()` 함수를 지원하지 않습니다. 하지만 우리 모두 학교에서 배웠듯이, x^y = e^(ln(x) * y)이므로 에뮬레이션이 가능합니다!

## sinh(3)

쌍곡사인 함수를 살펴봅시다. 이 함수는 삼각함수의 쌍곡선 버전입니다:

```sql
-- DB2, Derby, Firebird, H2, Oracle, SQLite:
sinh(3)

-- Access, ASE, CUBRID, HANA, HSQLDB, Ingres, MariaDB, MySQL, PostgreSQL, SQL Server, Sybase SQL Anywhere, Vertica:
((exp((3 * 2)) - 1) / (exp(3) * 2))
```

고등학교 수학 수업을 떠올리게 하는 공식입니다! sinh(x) = (e^(2x) - 1) / (2 * e^x)로 계산됩니다. 많은 데이터베이스에서 `sinh()` 함수를 네이티브로 지원하지 않기 때문에 이렇게 수학적으로 에뮬레이션해야 합니다.

## lpad('abc', 3)

문자열 왼쪽 패딩 함수입니다. 여기서 상황이 더 재미있어집니다:

```sql
-- Access, ASE, CUBRID, DB2, Derby, Firebird, H2, HANA, HSQLDB, Ingres, MariaDB, MySQL, Oracle, PostgreSQL, Sybase SQL Anywhere, Vertica:
lpad('abc', 3)

-- SQL Server:
replicate(' ', (3 - len('abc'))) + 'abc'

-- SQLite:
substr(replace(replace(substr(quote(zeroblob(((3 - length('abc') - 1 + length('abc')) / length('abc') + 1) / 2)), 3), '''', ''), '0', ' '), 1, (3 - length('abc'))) || 'abc'
```

SQLite의 에뮬레이션을 보세요! SQLite에서 문자열을 반복하는 `repeat()` 함수가 없기 때문에, `zeroblob()`과 `quote()` 함수를 사용한 창의적인 해결책이 필요합니다. 이것이 바로 잘 통합 테스트된 도구를 사용해야 하는 이유입니다!

## datediff()

날짜 연산은 아마도 SQL에서 가장 벤더 간 호환성이 낮은 영역일 것입니다. 날짜 차이를 계산하는 함수를 살펴봅시다:

```sql
-- MariaDB, MySQL:
datediff(current_date, date '2000-01-01')

-- Oracle:
(current_date - date '2000-01-01')

-- PostgreSQL:
(current_date - date '2000-01-01')

-- SQL Server:
datediff(day, '2000-01-01', current_date)

-- Derby:
{fn timestampdiff(sql_tsi_day, date('2000-01-01'), current_date)}

-- SQLite:
((strftime('%s', current_date) - strftime('%s', '2000-01-01')) / 86400)

-- H2, HSQLDB:
datediff('day', date '2000-01-01', current_date)
```

구문이 완전히 다릅니다. PostgreSQL은 단순 뺄셈을, MySQL은 `datediff()` 함수를, SQL Server는 인자 순서가 다른 `datediff()` 함수를, Derby는 JDBC 이스케이프 구문을, SQLite는 Unix 타임스탬프를 86400(하루의 초 수)으로 나누는 방식을 사용합니다.

## bit_count(5)

가장 극단적인 예제입니다. 정수의 비트 수를 세는 함수:

```sql
-- MariaDB, MySQL:
bit_count(5)

-- 나머지 대부분의 데이터베이스:
(
  ((5) & 1) +
  (((5) >> 1) & 1) +
  (((5) >> 2) & 1) +
  (((5) >> 3) & 1) +
  (((5) >> 4) & 1) +
  (((5) >> 5) & 1) +
  (((5) >> 6) & 1) +
  ...
)
```

위의 끔찍한 점은 `bit_count()` 함수가 거의 지원되지 않을 뿐만 아니라, 비트 연산 자체도 많이 지원되지 않는다는 것입니다. MySQL과 MariaDB만이 `bit_count()`를 네이티브로 지원하고, 다른 데이터베이스들은 비트 시프트와 AND 연산을 수동으로 수행하여 각 비트를 개별적으로 계산해야 합니다.

## 결론

이 글에서 보여드린 것처럼, SQL 표준화는 이론적으로는 존재하지만 실제로는 각 벤더마다 상당한 차이가 있습니다. 간단해 보이는 함수들도 데이터베이스에 따라 완전히 다른 방식으로 구현해야 할 수 있습니다.

jOOQ와 같은 추상화 도구는 이러한 벤더별 차이를 자동으로 처리하여, 개발자가 한 번 작성하고 여러 데이터베이스 시스템에 배포할 수 있게 해줍니다. 수동으로 이러한 번역을 관리하는 것은 오류가 발생하기 쉽고 시간이 많이 소요되는 작업입니다.
