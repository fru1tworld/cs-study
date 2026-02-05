# jOOQ 뉴스레터: 2014년 9월 2일 - 정말 지원이 필요한가?

> 원문: https://blog.jooq.org/jooq-newsletter-september-02-2014-do-you-really-need-support/

게시일: 2014년 9월 2일

## 정말 지원이 필요한가요?

Data Geekery는 비용을 20% 절감할 수 있는 지원 없는 라이선스 옵션을 jOOQ에 도입했습니다. "NO SUPPORT" 할인 코드를 사용하면 보장된 응답 시간이 제거되지만, 보증, 업그레이드, 버그 수정은 그대로 유지됩니다. Professional 및 Enterprise 에디션에서 20% 절감 혜택을 받을 수 있습니다. 대량 구매(10개 이상 라이선스)의 경우 단계별 가격 정책이 적용됩니다.

## Data Geekery 1주년 기념

Data Geekery GmbH는 2013년 8월 15일에 설립되어 첫 번째 창립 기념일을 맞이하며 중요한 이정표들을 세웠습니다:

- 2013년 10월: 듀얼 라이선싱 하에서 jOOQ 3.2 출시
- 2014년 1월: 수익성 달성
- 2014년 2월: 키셋 페이지네이션을 포함한 jOOQ 3.3 출시
- 2014년 6월: CTE 및 DDL 지원이 포함된 jOOQ 3.4 출시; GitHub 스타 500개 달성
- 2014년 8월: 400번째 블로그 포스트 게시; 총 650,000회 이상의 조회수

월간 다운로드 수는 전년 대비 두 배로 증가했습니다. Simplernate, CQLC, iciql과 같은 프레임워크들이 내장 도메인 특화 언어에 대한 jOOQ의 접근 방식을 채택했습니다.

## 오늘의 트윗

커뮤니티 멤버들은 명시성에 있어서 ORM보다 SQL의 장점을 강조했으며, 키셋 페이지네이션 구현을 칭찬했습니다.

Sean Cassidy와 Petri Kainulainen 같은 사용자들이 ORM보다 SQL의 우수성과 키셋 페이지네이션 구현에 대해 긍정적인 언급을 했습니다.

## SQL 영역 - 두려운 COUNT(*) 함수

"COUNT(*)는 정확히 하나의 결과 레코드가 있는지 확인하는 실용적인 방법처럼 보이지만", 존재 여부 확인을 위해서는 EXISTS 술어와 함께 CASE 표현식을 사용하는 것이 더 나은 성능을 보이는 경우가 많습니다.

`COUNT(*)`는 레코드가 존재하는지 확인하는 데 일반적으로 사용되지만, 이는 비효율적일 수 있습니다. EXISTS 술어와 CASE 표현식을 사용하면 존재 여부 확인에서 더 빠른 대안이 됩니다.

## SQL 영역 - 뷰에 대한 제약 조건

Oracle과 SQL Server는 유효하지 않은 데이터 삽입을 방지하기 위해 뷰에 CHECK OPTION을 지원합니다. 가격이 100 미만인 것을 제한하는 expensive_books 뷰를 사용한 예제가 제공되었습니다. jOOQ 3.5에서 이 기능을 지원할 예정입니다.

## 다가오는 이벤트

- 9월 9-11일: JavaZone, 오슬로
- 9월 28일 - 10월 2일: JavaOne, 샌프란시스코
- 10월 23일: Soft-Shake, 제네바
- 10월 24일: GeeCON CZ, 프라하
- 10월 25일: Firebird Conference, 프라하

---

태그: jOOQ, newsletter, Data Geekery, support, licensing, COUNT, EXISTS, CHECK OPTION, views, SQL
