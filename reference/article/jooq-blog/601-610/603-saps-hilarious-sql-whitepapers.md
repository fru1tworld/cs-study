# SAP의 유쾌한 SQL 백서(들)

> 원문: https://blog.jooq.org/saps-hilarious-sql-whitepapers/

게시일: 2013년 7월 31일
작성자: Lukas Eder

저는 `TOP .. START AT` 절에 대해 조사하던 중 Sybase SQL Anywhere 12 백서를 발견했는데, 이 문서가 정말 재미있게 작성되어 있었습니다. 특히 섹션 7의 "DaffySQL 문법에 대한 개선된 지원"이라는 제목의 부분이 흥미로웠습니다.

이 발췌문은 `LIMIT` 절의 구현에 대한 유머러스한 설명을 담고 있었습니다. 문서는 쿼리 결과에 대한 수수께끼를 제시한 다음, 세 가지 다른 쿼리 접근 방식이 동일한 결과를 반환한다는 것을 밝혔습니다.

백서에서 특히 재미있었던 부분은 `LIMIT`가 0부터 시작하는 인덱싱을 사용하는 반면, `TOP START AT`는 1부터 시작하는 방식을 사용한다는 점을 설명하는 방식이었습니다. 작성자는 0 기반 번호 매기기를 돈을 세는 것에 비유하며 이렇게 썼습니다: "'오프셋'이라고요, 이해하셨나요? 마치 '여기 10달러가 있습니다. 제가 세어볼게요: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9.' 이런 식이죠."

이 백서는 `LIMIT` 기능이 MySQL과 PostgreSQL 사용자들이 기존 코드를 다시 작성하지 않고도 SQL Anywhere로 마이그레이션할 수 있도록 돕기 위해 추가되었다고 설명했습니다.

그러니 쿨하게 SQL Anywhere로 마이그레이션하세요!

저는 계속해서 이 문서를 읽어나가고 있습니다.

---

태그: cool, LIMIT, MySQL, OFFSET, PostgreSQL, SQL, Sybase, Sybase SQL Anywhere, TOP .. START AT
