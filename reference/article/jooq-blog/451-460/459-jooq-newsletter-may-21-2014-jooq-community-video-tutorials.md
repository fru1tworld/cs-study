# jOOQ 뉴스레터: 2014년 5월 21일 - jOOQ 커뮤니티 비디오 튜토리얼

> 원문: https://blog.jooq.org/jooq-newsletter-may-21-2014-jooq-community-video-tutorials/

게시일: 2014년 5월 21일 | 작성자: lukaseder

[뉴스레터 구독하기](http://eepurl.com/VcofL)

---

## 오늘의 트윗

jOOQ에 대한 세 가지 개발자 소감을 소개합니다:

1. Chris Martin은 Django로 작업하면서 jOOQ가 그리웠다고 표현했습니다: "Django ORM을 쓰다 보니 정말 jOOQ가 그립네요." (2014년 5월 5일)

2. Moutaz Salem은 jOOQ를 발견하고 열광적으로 반응했습니다: "#JOOQ 그동안 어디 있었던 거야!" (2014년 4월 28일)

3. Simon Martinelli는 Hibernate에서 마이그레이션을 고려했습니다: Hibernate가 지연 로딩을 위해 트랜잭션을 요구하는 것에 의문을 제기하며 jOOQ로의 마이그레이션을 제안했습니다. (2014년 4월 6일)

---

## jOOQ 3.4 출시 임박

개발팀은 6월 초에 jOOQ 3.4가 곧 출시될 것이라고 발표하며, 세 가지 주요 기능을 강조했습니다:

- 타입 안전한 DDL 문 지원
- 공통 테이블 표현식(Common Table Expressions) 지원
- 통합 SQL 변환 API

SQL 변환은 방언 표준화, 멀티 테넌시, 소프트 삭제, 행 수준 보안에 유용합니다. 개선된 VisitListener와 커스텀 QueryPart는 "하드코어 SQL 변환 매니아"들의 역량을 향상시킬 것입니다.

뉴스레터에서는 로드맵 최적화로 인해 Informix 지원이 jOOQ 3.5로 연기되었음을 알렸습니다.

---

## 커뮤니티 존 - 비디오 기여

두 개의 커뮤니티 비디오 발표가 소개되었습니다:

1. Dmitry Lebedev와 Rustam Arslanov의 jOOQ 및 Flyway 발표 (라트비아어)

커뮤니티 회원들이 지역 Java 사용자 그룹에서 jOOQ를 발표할 것을 권장하며, 브랜드 자료, 예제 슬라이드 및 프로젝트를 제공해 드립니다. 연락처: contact@datageekery.com

---

## SQL 존 - 정렬 간접 지정(Sort Indirection)

이 섹션에서는 ORDER BY 절에서 CASE 표현식을 사용한 커스텀 정렬 구현을 설명합니다:

```sql
ORDER BY
  CASE WHEN name = 'val1' THEN 1
       WHEN name = 'val2' THEN 2
       WHEN name = 'val3' THEN 3
       WHEN name = 'val4' THEN 4
  END
```

jOOQ는 이 패턴에 대한 네이티브 편의 메서드를 제공합니다. 전체 기사: [SQL에서 정렬 간접 지정을 구현하는 방법](https://blog.jooq.org/how-to-implement-sort-indirection-in-sql/)

---

## SQL 존 - 식별자 혼란(Identifier Madness)

이 글에서는 서로 다른 데이터베이스 시스템 간의 대소문자 구분에 의존하지 말 것을 경고합니다. 주요 권장 사항:

- 벤더에 구애받지 않는 SQL에서 대소문자 구분에 절대 의존하지 마세요
- 식별자가 대소문자를 구분하도록 강제하세요
- 일관된 대소문자 사용(모두 대문자 또는 모두 소문자)
- jOOQ는 데이터베이스에서 보고된 대로 식별자를 자동으로 인용합니다

참고: [SQL Ways - Sybase 식별자](http://wiki.ispirer.com/sqlways/sybase/identifiers)

---

## 다가오는 이벤트

완료된 이벤트:
- 크라쿠프의 GeeCON (완료) - 컨퍼런스가 JavaScript, Akka, Node.js, Vert.x에 초점을 맞추고 있음에도 불구하고 SQL 발표에 많은 참석자가 참여했습니다

예정된 발표 일정:
- 2014년 5월 21일: 베른의 JUGS (독일어, SQL 중심)
- 2014년 6월 9-11일: 크라쿠프의 33rd Degree (영어, jOOQ 중심)
- 2014년 6월 12-13일: 탈린의 Geekout (영어, jOOQ 중심)

이벤트 정보: [www.jooq.org/news](https://www.jooq.org/news)

---

## 태그

식별자 혼란, jOOQ, jOOQ 3.4, jOOQ 뉴스레터, 정렬 간접 지정, SQL
