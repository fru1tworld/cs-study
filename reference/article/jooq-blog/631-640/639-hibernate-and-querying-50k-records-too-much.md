# Hibernate와 5만 건 레코드 조회. 너무 과한 걸까?

> 원문: https://blog.jooq.org/hibernate-and-querying-50k-records-too-much/

Hibernate가 5만 건의 레코드를 조회할 때 빠르게 한계에 도달할 수 있다는 것을 보여주는 흥미로운 글이 있습니다. 5만 건은 정교한 데이터베이스 입장에서는 상대적으로 적은 수의 레코드입니다:

http://koenserneels.blogspot.ch/2013/03/bulk-fetching-with-hibernate.html

물론 Hibernate는 일반적으로 이러한 상황을 처리할 수 있습니다. 하지만 Hibernate를 튜닝하고 더 고급 기능들을 파고들어야 합니다. 2차 캐시(second-level caching), 플러시(flushing), 퇴거(evicting) 같은 것들이 정말로 기본 동작이어야 하는지 생각하게 됩니다...
