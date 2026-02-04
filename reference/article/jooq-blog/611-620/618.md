# 높은 복잡도와 낮은 처리량: ORM을 사용해야 하는 이유

> 원문: https://blog.jooq.org/high-complexity-and-low-throughput-reasons-for-using-an-orm/

저는 최근 Mike Hadlow의 블로그에서 매우 잘 쓰여지고 객관적인 블로그 게시물을 발견했습니다:

http://mikehadlow.blogspot.ca/2012/06/when-should-i-use-orm.html

특히 모델 복잡도(model complexity)와 처리량(throughput)을 그린 다이어그램이 마음에 들었습니다. ORM 논쟁은 정기적으로 다양한 블로그에서 반복됩니다. Jeff Atwood의 이 기사처럼 다소 극단적인 입장을 취하는 것부터:

http://www.codinghorror.com/blog/2006/06/object-relational-mapping-is-the-vietnam-of-computer-science.html

Martin Fowler의 이 글처럼 좀 더 객관적인 입장까지:

http://martinfowler.com/bliki/OrmHate.html

저는 ORM이 반복적인 SQL 작성이 지루했던 시절과 CRUD가 개념으로 설명되지 않았던 시절에 우리에게 무엇을 제공했는지 아직도 감사하게 생각합니다. 하지만 ORM은 Joel Spolsky의 유명한 기사에서 설명된 것처럼 새는 추상화(leaky abstraction)입니다:

https://www.joelonsoftware.com/articles/LeakyAbstractions.html

언급된 기사는 ORM이 언제 좋은 것인지, 그리고 jOOQ, MyBatis, Apache DbUtils 또는 일반 JDBC와 같은 도구를 사용하여 SQL에 더 가깝게 작업하는 것이 언제 더 나은지를 매우 명확하게 보여줍니다.

읽으면서 즐기시기 바랍니다!

관련 글들:

- ORM은 안티패턴이다
- ORM은 선택이 아니다
- 단순하게 유지하기
- ORM의 10가지 장점
- 당신의 ORM은 형편없다
- ORM을 사용해야 할까 말아야 할까? 물론이지.
