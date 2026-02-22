# Google Cloud SQL, jOOQ의 다음 단계?

> 원문: https://blog.jooq.org/google-cloud-sql-the-next-step-for-jooq/

"클라우드(The Cloud)"는 아마도 2011년 최대의 IT 버즈워드일 것이다. 이전의 "웹 2.0"이나 "닷컴"처럼 무의미하고 수명이 짧을 수도 있지만, 분명한 것은 대기업들이 지금 바로 "클라우드"를 겨냥하고 있다는 점이다. Microsoft가 [Windows Azure](https://www.microsoft.com/azure/default.mspx)와 그 하위 제품인 [SQL Azure](https://en.wikipedia.org/wiki/SQL_Azure)에 대한 전면적인 마케팅 캠페인을 펼친 데 이어, 이제 Google Labs에서도 비슷한 Google의 공세가 시작되었다:

https://code.google.com/apis/sql/docs/developers_guide_java.html

물론 마케팅 측면에서 "공세"라는 표현은 지나친 과장이다. Google Labs 제품들은 대체로 꽤 기술자 취향(geeky)으로 보이고 Microsoft의 제품들에 비하면 훨씬 덜 전문적이다. 하지만 그 접근 방식은 흥미롭다. 특히 클라우드에서 SQL 플랫폼으로 MySQL을 선택한 점이 그렇다. [NoSQL](https://en.wikipedia.org/wiki/NoSQL)은 전통적인 SQL이 수평적으로 확장(scale horizontally)하지 못하는 문제에 대한 대응으로 등장했다. Oracle 데이터베이스를 위해 큰 장비를 구매하면, 애플리케이션이 성장함에 따라 메모리와 CPU 파워를 추가하여 수직적으로 확장(scale vertically)하게 된다. 단일 쿼리의 성능 병목을 방지하기 위해 SQL을 정밀하게 튜닝할 것이다 -- 가급적이면 jOOQ를 사용해서 ;-). 그리고 그 작업을 위해 비싼 DBA들에게 비용을 지불하게 된다.

그러나 SQL이 클라우드로 이동하면, 수평적 확장이 더 현실적이 될 수도 있다... 이것이 어디로 향할지 매우 궁금하다. 분명한 것은, jOOQ가 Google Cloud SQL과 SQL Azure를 완전히 지원하는 최초의 Java 데이터베이스 추상화 도구 중 하나가 되어야 한다는 것이다.
