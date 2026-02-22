# Sybase에서도 클라우드 열풍

> 원문: https://blog.jooq.org/cloud-fever-now-also-at-sybase/

2011년 10월 19일 lukaseder 게시

SQL Server(SQL Azure)와 MySQL(Google Cloud SQL)에 이어, 이제 SQL Anywhere 데이터베이스도 클라우드에서 사용할 수 있게 되었습니다.

http://www.sybase.ch/fujibeta

이것은 Sybase SQL Anywhere OnDemand, 코드명 Fuji라고 불립니다. 아마도 이 이름의 의미는 여러분의 데이터가 후지(Fuji)로 이전될 수도 있다는 뜻일 겁니다. 아니면 여러분의 DBA가 후지에서 근무할 수도 있다는 뜻이겠죠. 누가 알겠습니까 ;-)

이렇게 많은 클라우드 기반 SQL 솔루션들이 나오고 있으니, jOOQ에 대한 통합 테스트를 어디서부터 추가해야 할지 모르겠습니다. 어쨌든, 흥미진진한 시대가 다가오고 있습니다!

---

## 댓글

Saqib Ali (2011년 10월 19일 20:39)

모두가 클라우드 SQL 비즈니스에 뛰어들고 싶어하는군요. 그런데 SAP의 Fuji는 정확히 진정한 클라우드 기반 솔루션이 아니라는 점을 발견했습니다. 이것은 온프레미스 시스템으로, Amazon EC2로 *클라우드 버스팅(cloud burst)*할 수 있는 방식입니다. 흥미로운 개념이고, 다른 곳에서는 본 적이 없습니다. 대부분의 다른 SQL 서비스들은 완전히 클라우드 기반(예: Google Cloud SQL)이거나 오프프레미스에서 호스팅(예: Oracle)되는 방식입니다.

lukaseder (2011년 10월 19일 20:45)

흥미로운 피드백 감사합니다. 이 모든 것이 어디로 향하게 될지 지켜봅시다!
