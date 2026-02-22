# MentaBean, Mentawai의 ORM/SQL Builder

> 원문: https://blog.jooq.org/mentabean-the-ormsql-builder-of-mentawai/

방금 놀라운 발견을 했습니다. 저는 항상 Stack Overflow에서 jOOQ 관련 주제를 면밀히 관찰하고 있기 때문에, 한 열성적인 jOOQ 사용자의 질문에 달린 다소 홍보성 답변들을 즉시 발견할 수 있었습니다:

- https://stackoverflow.com/questions/5625832/java-jooq-persistence-framework-performance-and-feed-back#7387325
- https://stackoverflow.com/questions/5620985/is-there-any-good-dynamic-sql-builder-library-in-java#7351575

MentaBean은 최근 Mentawai로부터 독립된 ORM/SQL Builder입니다. Mentawai는 Servlet 사양 위에 구축된 라이브러리로, 수천 명의 브라질 개발자들의 삶을 단순화하기 위해 만들어졌습니다(포럼의 메시지 수를 세어 보면 알 수 있습니다).

저는 MentaBean의 개발자 중 한 명인 Sergio Oliveira Jr.와 Stack Overflow 채팅을 나눴는데, Hibernate/JPA 스택의 무거움과 복잡성으로 고통받는 다른 사람들과 이야기하는 것은 언제나 흥미로운 일이기 때문입니다. 그의 좌우명은 인상적인데, 그가 생텍쥐페리를 인용한 것을 제가 다시 인용해도 된다면 이렇습니다: "완벽이란 더 이상 추가할 것이 없을 때가 아니라, 더 이상 제거할 것이 없을 때 달성된다." 이것은 eXtreme Programming 용어로 같은 것을 매우 시적으로 표현한 것입니다: 무자비하게 리팩토링하라(Refactor Mercilessly). 이것을 주요 패러다임으로 삼는다면, 훌륭하고 재미있는 소프트웨어가 발전할 수 있다고 믿습니다. 말할 것도 없이, 저는 Sergio가 좋아지기 시작했습니다 :-)

Mentawai(그리고 이에 연관된 MentaBean)가 현재 크게 주목받고 있는 Play! Framework나 잘 자리 잡은 Wicket 라이브러리를 능가할 것처럼 처음에는 보이지 않지만, 전 세계적으로 OSS(오픈소스 소프트웨어)에 얼마나 많은 노력이 투입되고 있는지 보는 것은 여전히 좋은 일이라고 생각합니다.

더 자세한 내용은 Mentawai 홈페이지를 참고하세요:

http://www.mentaframework.org/quick-start.jsp
