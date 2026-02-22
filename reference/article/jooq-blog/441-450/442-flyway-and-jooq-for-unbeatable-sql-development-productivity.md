# Flyway와 jOOQ로 무적의 SQL 개발 생산성

> 원문: https://blog.jooq.org/flyway-and-jooq-for-unbeatable-sql-development-productivity/

데이터베이스 마이그레이션을 수행할 때, Data Geekery에서는 jOOQ와 Flyway – Database Migrations Made Easy를 함께 사용하는 것을 권장합니다. 이 글에서는 두 프레임워크를 시작하는 간단한 방법을 살펴보겠습니다.

## 철학

jOOQ와 Flyway를 사용할 때, 우리가 채택하는 워크플로우는 다음 네 단계의 반복적인 프로세스입니다:

1. 데이터베이스 증분(Database increment) – 데이터베이스에 새 컬럼이 필요하면, 필요한 DDL을 Flyway 스크립트에 작성합니다.
2. 데이터베이스 마이그레이션(Database migration) – 스크립트를 다른 개발자들과 공유합니다.
3. 코드 재생성(Code re-generation) – 마이그레이션 후 jOOQ 아티팩트를 재생성합니다.
4. 개발(Development) – 생성된 코드를 기반으로 비즈니스 로직 개발을 계속합니다.

## Maven 설정 - 속성

```xml
<properties>
    <db.url>jdbc:h2:~/flyway-test</db.url>
    <db.username>sa</db.username>
</properties>
```

## Maven 의존성

```xml
<dependency>
    <groupId>org.jooq</groupId>
    <artifactId>jooq</artifactId>
    <version>3.4.0</version>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>1.4.177</version>
</dependency>
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.16</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
    <version>1.7.5</version>
</dependency>
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.11</version>
    <scope>test</scope>
</dependency>
```

## Flyway Maven 플러그인 설정

```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <version>3.0</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>migrate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <url>${db.url}</url>
        <user>${db.username}</user>
        <locations>
            <location>filesystem:src/main/resources/db/migration</location>
        </locations>
    </configuration>
</plugin>
```

Flyway 플러그인을 `generate-sources` 단계에서 실행한다는 점에 주목하세요. 또한 Flyway가 클래스패스에서만 마이그레이션 스크립트를 찾는 것을 방지하기 위해 `db/migration` 경로 앞에 `filesystem:`을 접두사로 붙여야 합니다.

## jOOQ Maven 플러그인 설정

```xml
<plugin>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-codegen-maven</artifactId>
    <version>${org.jooq.version}</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <jdbc>
            <url>${db.url}</url>
            <user>${db.username}</user>
        </jdbc>
        <generator>
            <database>
                <includes>.*</includes>
                <inputSchema>FLYWAY_TEST</inputSchema>
            </database>
            <target>
                <packageName>org.jooq.example.flyway.db.h2</packageName>
                <directory>target/generated-sources/jooq-h2</directory>
            </target>
        </generator>
    </configuration>
</plugin>
```

이 설정은 FLYWAY_TEST 스키마를 읽고 이를 `target/generated-sources/jooq-h2` 디렉토리 내의 `org.jooq.example.flyway.db.h2` 패키지로 리버스 엔지니어링합니다.

## SQL 마이그레이션 스크립트

V1__initialise_database.sql:

```sql
DROP SCHEMA flyway_test IF EXISTS;
CREATE SCHEMA flyway_test;
```

V2__create_author_table.sql:

```sql
CREATE SEQUENCE flyway_test.s_author_id START WITH 1;
CREATE TABLE flyway_test.author (
  id INT NOT NULL,
  first_name VARCHAR(50),
  last_name VARCHAR(50) NOT NULL,
  date_of_birth DATE,
  year_of_birth INT,
  address VARCHAR(50),
  CONSTRAINT pk_t_author PRIMARY KEY (ID)
);
```

V3__create_book_table_and_records.sql:

```sql
CREATE TABLE flyway_test.book (
  id INT NOT NULL,
  author_id INT NOT NULL,
  title VARCHAR(400) NOT NULL,
  CONSTRAINT pk_t_book PRIMARY KEY (id),
  CONSTRAINT fk_t_book_author_id FOREIGN KEY (author_id) REFERENCES flyway_test.author(id)
);
INSERT INTO flyway_test.author VALUES (next value for flyway_test.s_author_id, 'George', 'Orwell', '1903-06-25', 1903, null);
INSERT INTO flyway_test.author VALUES (next value for flyway_test.s_author_id, 'Paulo', 'Coelho', '1947-08-24', 1947, null);
INSERT INTO flyway_test.book VALUES (1, 1, '1984');
INSERT INTO flyway_test.book VALUES (2, 1, 'Animal Farm');
INSERT INTO flyway_test.book VALUES (3, 2, 'O Alquimista');
INSERT INTO flyway_test.book VALUES (4, 2, 'Brida');
```

## 마이그레이션 및 코드 생성 실행

다음 명령을 실행합니다:

```
mvn clean install
```

예상되는 Flyway 출력은 세 개의 마이그레이션 모두를 통해 스키마 버전 진행을 보여주며, "Successfully applied 3 migrations to schema PUBLIC."로 끝납니다.

## Java 통합 테스트 예제

```java
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.Test;
import java.sql.DriverManager;
import static java.util.Arrays.asList;
import static org.jooq.example.flyway.db.h2.Tables.*;
import static org.junit.Assert.assertEquals;

public class AfterMigrationTest {
    @Test
    public void testQueryingAfterMigration() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:~/flyway-test", "sa", "")) {
            Result<?> result =
            DSL.using(c)
               .select(
                   AUTHOR.FIRST_NAME,
                   AUTHOR.LAST_NAME,
                   BOOK.ID,
                   BOOK.TITLE
               )
               .from(AUTHOR)
               .join(BOOK)
               .on(AUTHOR.ID.eq(BOOK.AUTHOR_ID))
               .orderBy(BOOK.ID.asc())
               .fetch();

            assertEquals(4, result.size());
            assertEquals(asList(1, 2, 3, 4), result.getValues(BOOK.ID));
        }
    }
}
```

`mvn clean install`을 다시 실행하면, 위의 통합 테스트가 이제 컴파일되고 통과할 것입니다!

## 반복/진화 예제

V4__le_french.sql:

```sql
ALTER TABLE flyway_test.book
  ALTER COLUMN title RENAME TO le_titre;
```

이 스크립트를 추가하고 `mvn clean install`을 실행하면, Flyway는 마이그레이션을 성공적으로 적용하지만, Java 테스트는 컴파일 오류와 함께 실패합니다. "BOOK.TITLE"이 더 이상 존재하지 않으므로 "BOOK.LE_TITRE"로 변경해야 하기 때문입니다.

## 결론

이 튜토리얼은 Flyway와 jOOQ를 사용하여 견고한 개발 프로세스를 구축하는 방법을 매우 쉽게 보여줍니다. 이를 통해 SQL 관련 오류를 개발 생명주기 초기에 - 프로덕션이 아닌 컴파일 타임에 즉시 - 방지할 수 있습니다!

이 접근 방식은 누군가가 Maven 모듈에 새 마이그레이션 스크립트를 추가할 때마다 이전의 모든 단계가 자동으로 실행되도록 보장합니다.
