# Jodd SQL 생성기

> 원문: https://blog.jooq.org/the-jodd-sql-generator/

jOOQ 블로그에서 우리는 다른 무료 또는 상용 SQL 빌더들과 비교하는 것을 결코 지치지 않습니다. 과거에 우리가 본 가장 흥미로운 것 중 하나는 [MyBatis SQL Statement Builder](https://mybatis.org/mybatis-3/statement-builders.html)였습니다. 이러한 접근 방식들 중 일부에서 재미있는 점은 "타입 안전성"이 단순히 플루언트 API를 제공하는 것으로 이해된다는 사실입니다. [Jodd](http://jodd.org/)는 "참을 수 없는 Java의 가벼움(The Unbearable Lightness of Java)"을 제공한다고 주장하는 플랫폼입니다. 이것은 우리가 전적으로 지지할 수 있는 매우 멋진 주장입니다. 그러면 [Jodd의 SQL 생성기](http://jodd.org/doc/db/sqlgen.html)를 사용한 참을 수 없이 가벼운 SQL 쿼리 빌딩을 살펴보겠습니다:

```java
Boy bb = new Boy();
Girl bg = new Girl();

DbSqlBuilder dsb = sql()
     ._("select")      // "select"
     .columnsAll("bb") // "bb.ID, bb.GIRL_ID, bb.NAME"
     .columnsIds("bg") // "bb.NAME, bg.ID"
     ._(" from")       // " from" (하드코딩된 문자열)
     .table(bb, "bb")  // "BOY bb"
     .table(bg, "bg")  // ", GIRL bg"
     ._()              // " " (단일 공백)
     .match("bb", bb)  // "(1=1)"  모든 bb 필드가 null이므로
     ._(" and ")       // " and "
     .match("bg", bg); // "(1=1)" 모든 bg 필드가 null이므로
```

솔직히 말해서, 흥미롭습니다! Jodd의 SQL 빌딩에 대해 어떻게 생각하시나요?
