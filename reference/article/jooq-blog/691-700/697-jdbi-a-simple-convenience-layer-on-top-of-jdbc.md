# jDBI: JDBC 위에 구축된 간단한 편의 레이어

> 원문: https://blog.jooq.org/jdbi-a-simple-convenience-layer-on-top-of-jdbc/

저는 항상 [jOOQ](https://www.jooq.org/)와 유사한 도구들을 찾아보고 있습니다. 적어도 같은 도메인에서 작동하는 도구들 - 데이터베이스 접근 추상화 도메인에서 말입니다. [jDBI](http://jdbi.codehaus.org)는 매력적으로 보입니다. 이 도구는 JDBC에 일반적으로 부족한 부분에 대해 간단한 솔루션을 제공합니다. 다음은 [소개 문서](http://jdbi.codehaus.org/five_minute_intro/)에서 가져온 몇 가지 기능입니다:

### Fluent API

JDBC는 결과를 얻기 위해 보통 세 단계가 필요하다는 점에서 상당히 장황합니다:

1. 커넥션 획득
2. 스테이트먼트 준비
3. 결과 페치 (하나의 값만 필요하더라도 결과 집합을 순회해야 함)

다음은 jDBI가 이러한 고충을 완화하기 위해 fluent API를 모델링하는 방법입니다:

```java
// 인메모리 H2 데이터베이스 사용
DataSource ds = JdbcConnectionPool.create("jdbc:h2:mem:test",
                                          "username",
                                          "password");
DBI dbi = new DBI(ds);
Handle h = dbi.open();
h.execute(
  "create table something (id int primary key, name varchar(100))");
h.execute(
  "insert into something (id, name) values (?, ?)", 1, "Brian");

String name = h.createQuery("select name from something where id = :id")
               .bind("id", 1)
               .map(StringMapper.FIRST)
               .first();

assertThat(name, equalTo("Brian"));

h.close();
```

### DAO 레이어 단순화

DAO 레이어에서는 동일한 SQL 코드를 반복해서 작성하는 경우가 많습니다. Hibernate / JPA는 이를 처리하는 데 상당히 편리하지만, 항상 그렇게 큰 의존성을 원하는 것은 아닙니다. 그래서 jDBI는 EJB 3.0의 핵심을 제공합니다. 명명된 쿼리를 위한 간단한 애노테이션입니다 (다만, Brian McCallister가 자체 애노테이션 대신 JPA 애노테이션을 사용할 수 있었을 것이라고 생각합니다):

```java
public interface MyDAO
{
  @SqlUpdate(
    "create table something (id int primary key, name varchar(100))")
  void createSomethingTable();

  @SqlUpdate("insert into something (id, name) values (:id, :name)")
  void insert(@Bind("id") int id, @Bind("name") String name);

  @SqlQuery("select name from something where id = :id")
  String findNameById(@Bind("id") int id);

  /
   * 인자 없는 close는 커넥션을 닫는 데 사용됩니다
   */
  void close();
}
```

다음은 위의 DAO를 사용하는 방법입니다:

```java
// 풀링된 DataSource를 통해 인메모리 H2 데이터베이스 사용
JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:mem:test2",
                                                  "username",
                                                  "password");
DBI dbi = new DBI(ds);
MyDAO dao = dbi.open(MyDAO.class);

dao.createSomethingTable();
dao.insert(2, "Aaron");

String name = dao.findNameById(2);
assertThat(name, equalTo("Aaron"));

dao.close();
ds.dispose();
```

### 요약

jOOQ에서 유용하게 활용할 수 있는지 지금 확인해 볼 몇 가지 다른 아주 좋은 기능들이 있습니다. 여기에서 매뉴얼을 읽고 이 작은 보석을 발견해 보세요:

http://jdbi.codehaus.org/archive.html

또는 여기에서 소스를 받으세요:

https://github.com/brianm/jdbi
