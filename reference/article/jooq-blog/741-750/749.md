# 실전에서의 jOOQ

> 원문: https://blog.jooq.org/jooq-in-the-wild/

2011년 10월 27일 lukaseder 작성

jOOQ에 대한 공개 의존성을 가진 최초의 오픈 소스 프로젝트들이 웹에 등장하기 시작했다. 그 중 하나는 Maik Schreiber가 만든 [blizzys-backup](https://github.com/blizzy78/blizzys-backup/)이라는 소규모 백업 도구이다. 이 도구는 jOOQ를 사용하여 예약된 백업과 백업 파일 메타데이터를 설명하는 3~4개의 관계(relation)를 가진 소규모 H2 데이터베이스를 다룬다. 흥미롭게도, Maik은 반복(iteration) 동안 기반이 되는 [java.sql.ResultSet](https://download.oracle.com/javase/6/docs/api/java/sql/ResultSet.html)을 열어둔 채로 [org.jooq.Cursor](https://jooq.org/javadoc/latest/org/jooq/Cursor.html)를 사용하여 데이터를 지연 페칭(lazy fetching)하는 방식을 선호하는 것으로 보인다.

BackupRun 클래스의 예제 코드 스니펫은 다음과 같다:

```java
Cursor<Record> cursor = null;
try {
  cursor = database.factory()
    .select(Backups.ID)
    .from(Backups.BACKUPS)
    .where(Backups.ID.notEqual(Integer.valueOf(backupId)))
    .orderBy(Backups.RUN_TIME.desc())
    .fetchLazy();

  while (cursor.hasNext()) {
    int backupId = cursor.fetchOne().getValue(Backups.ID).intValue();
    // [...]
  }

  // 참고: 위의 루프는 다음과 같이 다시 작성할 수 있다.
  // Cursor<R extends Record>는 Iterable<R>을 확장(extends)하기 때문이다:
  for (Record record : cursor) {
    int backupId = record.getValue(...);
  }
} finally {
  database.closeQuietly(cursor);
}
```

또 다른 예제는 다음과 같다:

```java
Cursor<Record> cursor = null;
try {
  cursor = database.factory()
    .select(Files.ID,
            Files.BACKUP_PATH)
    .from(Files.FILES)
    .leftOuterJoin(Entries.ENTRIES)
      .on(Entries.FILE_ID.equal(Files.ID))
    .where(Entries.FILE_ID.isNull())
    .fetchLazy();

  while (cursor.hasNext()) {
    Record record = cursor.fetchOne();
    FileEntry file = new FileEntry(
      record.getValue(Files.ID).intValue(),
      record.getValue(Files.BACKUP_PATH));
    // [...]
  }
} finally {
  database.closeQuietly(cursor);
}
```

blizzys-backup는 최초 실행 시 스키마를 자동으로 설치한다. jOOQ의 DDL 문(statement) 지원 계획은 jOOQ에 더 큰 가치를 부여할 것이다. 현재의 문자열 연결 방식을 보여주는 샘플 구문은 다음과 같다:

```java
factory.query("CREATE TABLE IF NOT EXISTS backups (" +
                "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "run_time DATETIME NOT NULL, " +
                "num_entries INT NULL" +
              ")").execute();
```

더 많은 정보는 [https://github.com/blizzy78/blizzys-backup/](https://github.com/blizzy78/blizzys-backup/)에서 확인할 수 있다.
