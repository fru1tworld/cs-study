# jOOQ 뉴스레터: 2014년 3월 12일

> 원문: https://blog.jooq.org/jooq-newsletter-march-12-2014/

뉴스레터 구독 링크: http://eepurl.com/Qc9ir

## 오늘의 트윗

이번 뉴스레터에서는 트윗을 통해 jOOQ에 대한 고객들의 열정을 소개합니다:

Dominik Dorn이 트윗했습니다: "#JOOQ is awesome!" (2014년 2월 14일)

Mariusz Nosiński가 공유했습니다: "어떻게 #jOOQ를 이제야 알게 됐지? PlayFramework와 함께 사용하기 좋은 'Database First' 도구로 선택했습니다" (2014년 3월 6일)

이러한 인정에 감사드립니다.

## jOOQ와 Scala

이 섹션에서는 jOOQ의 향후 Scala 통합에 대해 다룹니다. jOOQ와 Scala는 현재 Scala의 언어 기능을 통해 잘 통합되어 있어 개발자들이 Scala 코드에서 자연스럽게 읽히는 SQL을 작성할 수 있습니다.

다음 코드 예제는 Scala에서의 jOOQ-SQL 문법을 보여줍니다:

```scala
select (
  BOOK.ID * BOOK.AUTHOR_ID,
  BOOK.ID + BOOK.AUTHOR_ID * 3 + 4,
  BOOK.TITLE || " abc" || " xy"
)
from BOOK
leftOuterJoin (
  select (x.ID, x.YEAR_OF_BIRTH)
  from x
  limit 1
)
on BOOK.AUTHOR_ID === x.ID
where (BOOK.ID <> 2)
and (BOOK.TITLE in ("O Alquimista", "Brida"))
fetch
```

2014년 4월에 Vienna Scala User Group과 협력하여 이 통합에 도전할 예정입니다. "Scala에는 현재 SQL을 일급 언어로 완전히 수용하는 적절한 SQL 프레임워크가 없습니다." 우리는 2014년 동안 이 공백을 해결할 계획이며, 비엔나에서 4월 7일에 이벤트가 예정되어 있습니다.

## 커뮤니티 존

jOOQ는 단순한 제품 그 이상입니다. Java와 SQL 개발자들을 위한 "완전한 경험"입니다. MailChimp 비교 통계에 따르면 jOOQ 뉴스레터의 성과는 참여도에서 업계 평균을 초과합니다.

아이디어가 있으신가요? 기여하고 싶으신가요? 공유하고 싶은 블로그 포스트가 있으신가요? contact@datageekery.com으로 이메일을 보내주시고, 커뮤니티에 참여하고, 동료들과 경험을 공유해 주세요.

## Java 존 - Java 8에서의 SQL

이 섹션에서는 Java 8과 SQL 통합에 대해 다룹니다. "Java 8 람다 표현식 경험을 개선하기 위한 최신 오픈 소스 제품"인 jOOλ(jOO-Lambda)를 소개합니다.

Java에서 SQL과 상호작용하는 여러 접근 방식을 비교합니다:
- JDBC
- jOOλ
- jOOQ
- Spring JDBC
- Apache DbUtils

전체 비교는 https://www.jooq.org/java-8-and-sql 에서 확인할 수 있으며, 이는 Java 8 Friday 블로그 시리즈의 일부입니다. 종합적인 Java 8 리소스 모음은 http://www.baeldung.com/java8 을 참조하세요.

## 예정된 이벤트

예정된 발표:

- 2014년 3월 20일: JUGS 취리히 (독일어, SQL에 관하여)
- 2014년 4월 4일: JUG Saxony Day 드레스덴 (독일어, jOOQ에 관하여)
- 2014년 4월 7일: VSUG 비엔나 (영어, Scala에서의 jOOQ에 관하여)
- 2014년 5월 14-16일: Geecon 크라쿠프 (영어, jOOQ에 관하여)

사내 발표:

- 2014년 3월 25일: SBB, 스위스 연방 철도 (독일어, jOOQ에 관하여)
- 2014년 3월 29일: Trivadis TechEvent (영어, jOOQ에 관하여)

해당 회사 직원분들의 참석을 환영하며, 발표 호스팅에 관심 있는 다른 조직의 문의도 기다립니다. 이벤트 정보는 https://www.jooq.org/news 에서 확인할 수 있습니다.
