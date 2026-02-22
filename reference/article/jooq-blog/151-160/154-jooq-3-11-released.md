# jOOQ 3.11 릴리스 - 4개의 새로운 데이터베이스, 암시적 조인, 진단 기능 등

> 원문: https://blog.jooq.org/jooq-3-11-released-with-4-new-databases-implicit-joins-diagnostics-and-much-more/

## 새로운 데이터베이스 지원

jOOQ 3.11에서는 4개의 추가 SQL 방언(dialect)에 대한 지원이 도입되었습니다.

Professional Edition:
- Aurora MySQL Edition
- Aurora PostgreSQL Edition
- Azure SQL Data Warehouse

Enterprise Edition:
- Teradata

## 암시적 조인(Implicit Joins) 기능

명시적인 JOIN 문 없이 경로 표기법을 통해 관련 엔티티 컬럼에 접근할 수 있는 중요한 기능이 추가되었습니다. 기존에는 다음과 같이 작성해야 했습니다:

```sql
SELECT author.first_name, author.last_name, book.title
FROM book
JOIN author ON book.author_id = author.id
```

이제 개발자는 다음과 같이 사용할 수 있습니다:

```java
ctx.select(BOOK.author().FIRST_NAME, BOOK.author().LAST_NAME, BOOK.TITLE)
   .from(BOOK)
   .fetch();
```

조인 그래프는 쿼리 렌더링 시 자동으로 계산됩니다.

## DiagnosticsListener SPI

새로운 서비스 제공자 인터페이스(Service Provider Interface)가 SQL 및 API 사용 검증을 가능하게 하며, 다음 항목들을 감지합니다:

- 중복 구문(duplicate statements) - 바인드 변수 누락 표시
- 반복되는 동일 구문 - 배치 처리 후보
- 불필요한 컬럼 페치(fetching)
- 과도한 행 조회

이 기능은 JDBC로 생성된 SQL을 역공학(reverse engineer)하여 N+1 쿼리 패턴과 같은 성능 문제를 식별하는 데 도움을 줍니다.

## 추가 개선 사항

- 익명 블록(Anonymous Blocks): 절차적 코드 블록 지원 (Oracle 스타일의 DECLARE/BEGIN)
- 파서 개선: 공개 API 접근이 가능한 향상된 SQL 변환 기능
- Java 10 호환성: Java 10을 공식적으로 지원하는 첫 번째 릴리스
- Asterisk 지원: SELECT * 연산에 대한 공식 API 지원
- 콜레이션(Collation) 지원: 여러 구문 요소에 걸친 지정 가능
- DDL 확장: GRANT, REVOKE, EXPLAIN 및 주석(comment) 문 지원
