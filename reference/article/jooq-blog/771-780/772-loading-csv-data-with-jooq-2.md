# jOOQ로 CSV 데이터 로딩하기

> 원문: https://blog.jooq.org/loading-csv-data-with-jooq-2/

최근 jOOX에 대한 작업을 마친 후, jOOQ의 개발이 계속 진행되었습니다. 다가오는 릴리스 1.6.5의 주요 새 기능은 CSV 데이터 로딩 지원입니다.

jOOQ Factory는 이제 CSV 파일을 생성된 테이블에 로딩하기 위한 전용 플루언트 API에 대한 접근을 제공하며, 필드 매핑과 대량 로드의 일괄 처리와 관련된 다양한 파라미터를 지정할 수 있습니다. 다음은 이 API가 어떻게 보이는지에 대한 예제입니다:

```java
Factory create = new Factory(connection, SQLDialect.ORACLE);

Loader<TAuthor> loader =
create.loadInto(AUTHOR)
      .onDuplicateKeyError()
      .onErrorAbort()
      .commitAll()
      .loadCSV("1;'Kafka'\n" +
               "2;Frisch")
      .fields(Author.ID, Author.LAST_NAME)
      .quote('\'')
      .separator(';')
      .ignoreRows(0)
      .execute();
```

결과로 반환되는 Loader 객체는 로딩 과정에 대한 정보를 제공합니다:

- `processed()` - 처리된 행의 수
- `stored()` - 저장된(INSERT 또는 UPDATE) 행의 수
- `ignored()` - 무시된 행의 수
- `errors()` - `LoaderError` 객체의 리스트. 각 `LoaderError`는 다음 정보를 포함합니다:
  - `exception()` - 발생한 `SQLException`
  - `rowIndex()` - 오류를 발생시킨 행의 인덱스
  - `row()` - 해당 행 데이터
  - `query()` - 오류를 발생시킨 쿼리

이전에 구현된 내보내기 API와 함께, 이제 Result 객체를 CSV로 내보내고, Excel이나 기타 오피스 소프트웨어에서 수정한 다음, CSV를 다시 로딩할 수 있습니다.

향후 jOOQ 버전에 대한 다른 아이디어로는 XML 및 JSON 데이터 소스에서 데이터를 로딩하는 것, 데이터를 '병합(merging)'하는 것(즉, DELETE 연산을 포함하는 것) 등이 있습니다.
