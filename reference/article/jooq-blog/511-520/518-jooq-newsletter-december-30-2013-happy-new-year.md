# jOOQ 뉴스레터: 2013년 12월 30일. 새해 복 많이 받으세요!

> 원문: https://blog.jooq.org/jooq-newsletter-december-30-2013-happy-new-year/

게시일: 2013년 12월 30일, lukaseder 작성

---

## 오늘의 트윗

Andy Van Den Heuvel의 트윗을 소개합니다. 그는 jOOQ와 MyBatis가 Java 개발에서 SQL을 다시 주요 초점으로 되돌리는 "포스트-JPA 프레임워크"를 대표한다고 평가했습니다.

---

## jOOQ 관점에서 바라본 2013년

2013년은 jOOQ에게 변혁의 해였습니다. 몇 가지 주요 성과를 강조합니다:

- 상업용 라이선스와 지원을 제공하는 회사 설립
- jOOQ 3.0 출시, Java에서 SQL에 대한 행 수준 타입 안전성 도입
- Java에서 SQL을 일급 시민으로 효과적으로 취급하는 경쟁 제품이 없다는 자부심

### 2013년 주요 릴리스:
- jOOQ 3.1: MariaDB, SQL Server 2012, Oracle 12c 지원 추가; 새로운 POJO 매핑 SPI
- jOOQ 3.2: Record 및 쿼리 렌더링 라이프사이클 SPI; 고급 SQL 변환 기능; 매처 전략 개선

2014년에는 MS Access 지원과 keyset 페이지네이션 구현이 포함될 예정입니다.

---

## 매니저 설득하기

이 섹션에서는 jOOQ 도입에 대한 비즈니스 사례를 다룹니다. 업계 표준과의 비교 프레임워크를 제시합니다:
- JDBC (저수준 데이터베이스 접근)
- JPA (고수준 추상화)

목표는 jOOQ를 도입하는 팀에게 투자 수익률(ROI)을 입증하는 것입니다.

---

## 다가오는 이벤트

2014년 초 예정된 발표:
- 2014년 1월 14일: JUG-HH 함부르크 (독일어)
- 2014년 1월 23일: Rhein JUG 뒤셀도르프 (독일어)
- 2014년 1월 27일: JUGM 뮌헨 (독일어)

---

## SQL 영역 – LATERAL 조인

이 글에서는 테이블 표현식을 위한 SQL:1999 LATERAL 키워드를 소개합니다. 주요 포인트는 다음과 같습니다:

- FROM 절 내에서 테이블 간 교차 참조 허용
- 비스칼라 테이블 반환 함수를 조인할 때 특히 유용
- T-SQL(SQL Server, Sybase)에서는 CROSS APPLY와 OUTER APPLY로 오랫동안 사용 가능
- 최근 PostgreSQL 9.3과 Oracle 12c에 추가됨
- jOOQ 3.3에서 지원 예정
