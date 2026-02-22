# jOOQ로 SQL 디버그 로깅

> 원문: https://blog.jooq.org/debug-logging-sql-with-jooq/

이 멋진 작은 기능은 jOOQ 매뉴얼에서 크게 광고되지는 않지만, 아마도 우리 개발자 대부분이 있으면 원하고 좋아하는 기능일 것이다. log4j나 slf4j를 jOOQ와 함께 클래스패스에 넣으면, jOOQ는 설정에 따라 해당 프레임워크를 로깅에 사용한다. jOOQ는 ERROR/WARN/INFO 레벨 로깅에서는 꽤 조용하지만, DEBUG/TRACE 레벨에서는 상당히 수다스러운 도구가 된다.

## DEBUG 레벨 로깅 예시

다음 쿼리를 실행할 때의 DEBUG 레벨 출력 샘플을 확인해보자:

```java
Result<Record> result = ctx
  .select(T_AUTHOR.LAST_NAME, count().as("c"))
  .from(T_BOOK)
  .join(T_AUTHOR)
  .on(T_BOOK.AUTHOR_ID.eq(T_AUTHOR.ID))
  .where(T_BOOK.TITLE.ne("1984"))
  .groupBy(T_AUTHOR.LAST_NAME)
  .having(count().eq(2))
  .fetch();
```

그러면 출력은 다음과 같다:

```
Executing query          : select "t_author"."last_name",
  count(*) as "c" from "t_book" join "t_author" on
  "t_book"."author_id" = "t_author"."id" where
  "t_book"."title" <> '1984' group by "t_author"."last_name"
  having count(*) = 2

Fetched result           : +---------+----+
                         : |last_name|   c|
                         : +---------+----+
                         : |Coelho   |   2|
                         : +---------+----+
```

쿼리 텍스트는 인라인된 파라미터와 함께 로그 출력에 출력된다(즉, 바인드 변수가 로깅을 위해 대체된다). 그런 다음 결과의 처음 5개 행에 대한 텍스트 테이블 표현이 출력된다.

## TRACE 레벨 로깅 예시

TRACE 레벨에서는 jOOQ 내부를 훨씬 더 많이 볼 수 있지만, 보통 이것은 jOOQ를 디버깅할 때만 필요하다:

```
Executing query          : select "t_author"."last_name",
  count(*) as "c" from "t_book" join "t_author" on
  "t_book"."author_id" = "t_author"."id" where
  "t_book"."title" <> '1984' group by "t_author"."last_name"
  having count(*) = 2

Preparing statement      : select "t_author"."last_name",
  count(*) as "c" from "t_book" join "t_author" on
  "t_book"."author_id" = "t_author"."id" where
  "t_book"."title" <> cast(? as varchar)
  group by "t_author"."last_name"
  having count(*) = cast(? as int)

Binding variable 1       : 1984 (class java.lang.String)
Binding variable 2       : 2 (class java.lang.Integer)
Attaching                : RecordImpl [ RecordImpl [values=[null, null]] ]
Fetching record          : RecordImpl [values=[Coelho, 2]]
Fetched result           : +---------+----+
                         : |last_name|   c|
                         : +---------+----+
                         : |Coelho   |   2|
                         : +---------+----+
```

앞서 보여준 DEBUG 레벨 출력에 더해, 바인드 변수와 jOOQ가 도입한 몇 가지 추가 타입 캐스트를 포함하여 실제로 실행되는 진짜 SQL 문도 확인할 수 있다. 또한 모든 바인드 변수가 문서화되고, 페치된 모든 레코드가 문서화된다. 로깅 양이 상당히 많다. 필요하지 않을 때는 반드시 꺼두도록 하자!

## 로깅 설정

jOOQ의 향후 버전에서는 로깅이 더 설정 가능하게 될 것이다.
