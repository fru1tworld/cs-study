# Alvor: JDBC에 전달되는 문자열의 정적 SQL 분석

> 원문: https://blog.jooq.org/alvor-static-sql-analysis-in-strings-passed-to-jdbc/

저는 방금 Alvor라는 것을 발견했습니다. 이것은 정적 SQL 분석을 위한 Eclipse 플러그인입니다. 이 도구는 JDBC 메서드에 전달되어 이후 실행되는 String, StringBuilder, StringBuffer, CharSequence 및 기타 많은 타입들을 평가합니다. 여기에는 다음이 포함됩니다:

- 구문 정확성
- 의미론적 정확성
- 객체 가용성

이것은 자체 내부 SQL 문법과 비교하고, 실제 데이터베이스에 대해서도 검사합니다(JDBC 드라이버, JDBC URL, 사용자명, 비밀번호가 제공된 경우).

저는 몇 가지 거짓 양성(false positive)을 경험했습니다. 일반 SQL 문에서는 약 20%, 저장 프로시저 호출에서는 100%입니다(이는 지원되지 않는 것으로 보입니다).

그러나 잘못된 SQL 문자열 연결, 오타가 있는 테이블/컬럼 이름, 타입 불일치 등으로 인한 많은 버그를 찾아낼 수 있을 것으로 보입니다!

저는 FindBugs 메일링 리스트에 정적 SQL 분석이 FindBugs 자체에 추가되어야 한다고 제안하는 게시글을 올렸습니다. 좋은 제어 흐름 분석을 통해 원격 엣지 케이스까지 감지할 수 있을 것입니다.

더 자세한 정보는 여기에서 확인하세요: https://code.google.com/p/alvor/
