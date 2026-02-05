# 새로운 SQL 방언의 가능성은 여전히 있다

> 원문: https://blog.jooq.org/theres-still-potential-for-new-sql-dialects/

2011년 8월 22일, lukaseder

최근의 기능 요청이 jOOQ에서 지원하는 새로운 SQL 방언에 대한 많은 가능성이 여전히 있다는 것을 상기시켜 주었습니다. 사용자 Philippe은 자신의 조직에서 다양한 프로젝트에 jOOQ를 고려하고 있으며, 그가 부족하다고 느끼는 방언 중 하나는 Sybase의 Adaptive Server Enterprise입니다. Sybase는 여전히 널리 사용되는 오래된 데이터베이스 중 하나로, 특히 의료 산업 분야에서 많이 사용됩니다. 그 역사는 1980년대 중반에 시작되었는데, 어느 시점에서 Microsoft의 SQL Server로 포크되었습니다. 이것은 jOOQ에 잘 반영되어 있으며, Sybase SQL Anywhere와 SQL Server에서 많은 방언별 동작 패턴이 동일하다는 것을 볼 수 있습니다. "Ingres 공룡"으로부터의 일부 공통 유산도 볼 수 있습니다.

jOOQ의 Sybase 지원은 현재 SQL Anywhere로 제한되어 있지만, Sybase ASE도 지원하지 못할 이유는 없습니다! Philippe의 기여를 기대하고 있습니다. 참고로, jOOQ에서 지원을 고려할 만한 다른 방언들도 있습니다:

- #430: Firebird
- #561: Informix
- #562: Interbase
- #659: SQL Azure
- #629: Teradata

RDBMS 역사에 대한 멋진 차트는 여기에서 확인하세요: https://en.wikipedia.org/wiki/User:Intgr/RDBMS_timeline

Philippe과의 토론은 여기에서 확인하세요: http://groups.google.com/group/jooq-user/browse_thread/thread/130b44cb59b14a82
