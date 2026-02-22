# SQL 수평 확장 성공 사례

> 원문: https://blog.jooq.org/a-success-story-of-sql-scaling-horizontally/

[NoSQL](https://en.wikipedia.org/wiki/NoSQL "NoSQL") 데이터베이스를 지지하는 가장 강력한 핵심 논거 중 하나는, NoSQL이 전통적인 [관계형 데이터베이스](https://en.wikipedia.org/wiki/Relational_database "Relational database")와 달리 기본적으로(natively) 수평 확장이 가능하다는 것이다. 개인적으로 나는, 일부 NoSQL 데이터베이스의 이러한 "기본 기능"을 실제로 활용하려면 여전히 뛰어난 아키텍트여야 한다고 생각한다. 잘못 설계하면 여전히 끔찍하게 실패하는 시스템을 만들 수 있다. 반면에, 중간 규모의 애플리케이션(2,500만 장의 사진, 초당 90개의 좋아요)이 [Postgres](https://www.postgresql.org "PostgreSQL") SQL 데이터베이스에서 [샤딩(sharding)](https://en.wikipedia.org/wiki/Shard_%28database_architecture%29 "Shard (database architecture)") 기법을 사용하여 [수평 확장](https://en.wikipedia.org/wiki/Scalability "Scalability")을 달성한 성공 사례를 읽으면 기분이 좋아진다. 결국, 이것은 가능한 일이다. 전체 성공 사례는 여기에서 읽을 수 있다:

[http://instagram-engineering.tumblr.com/post/10853187575/sharding-ids-at-instagram](http://instagram-engineering.tumblr.com/post/10853187575/sharding-ids-at-instagram "Instagram에서의 샤딩 ID")
