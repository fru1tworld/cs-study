# Testcontainers를 사용하여 jOOQ 코드 생성하기

> 원문: https://blog.jooq.org/using-testcontainers-to-generate-jooq-code/

작성일: 2021년 8월 27일 (2024년 9월 12일 업데이트)

작성자: lukaseder

---

## 핵심 개념

### 데이터베이스 우선 설계 철학

jOOQ는 데이터베이스 우선 원칙을 기반으로 구축되었습니다. 저자는 "데이터는 질량을 가진다"고 강조합니다. 이는 네트워크를 통해 데이터를 이동하는 것이 비용이 많이 든다는 의미이며, 따라서 코드가 데이터 쪽으로 이동해야 한다는 것입니다. 이 철학이 jOOQ가 미리 존재하는 데이터베이스 스키마를 필요로 하는 이유입니다.

### 기존의 문제점

역사적으로 jOOQ의 코드 생성기를 실제 운영 데이터베이스에 연결하는 것은 어려웠으며, 특히 공유 개발 인스턴스의 경우 더욱 그랬습니다. jOOQ는 이를 대체 코드 생성 모드를 통해 해결했습니다:

- JPADatabase: JPA 엔티티 기반 메타모델용
- XMLDatabase: XML 스키마 버전용
- DDLDatabase: DDL 스크립트용 (Flyway, pg_dump 출력)
- LiquibaseDatabase: Liquibase 마이그레이션 시뮬레이션

이러한 대안들은 고급 저장 프로시저나 데이터 타입과 같은 벤더별 기능에 대해 제한이 있습니다.

---

## 현대적 솔루션: Testcontainers 통합

### 기본 설정

현대적인 접근 방식은 Testcontainers를 사용하여 Docker에서 프로그래밍 방식으로 데이터베이스 인스턴스를 실행합니다. 주요 단계는 다음과 같습니다:

1. JDBC URL 설정

```
jdbc:tc:postgresql:13:///sakila?TC_TMPFS=/testtmpfs:rw&TC_INITSCRIPT=file:${basedir}/src/main/resources/postgres-sakila-schema.sql
```

2. Maven 의존성

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
```

3. 코드 생성기 설정

Maven 플러그인 설정에서 표준 PostgreSQL 드라이버 대신 `ContainerDatabaseDriver`를 사용합니다.

---

## 데이터베이스 마이그레이션을 포함한 고급 설정

### 다단계 프로세스

Flyway 또는 Liquibase를 포함하는 프로덕션 시나리오의 경우:

1. Testcontainers 인스턴스 시작
2. 데이터베이스 마이그레이션 실행
3. jOOQ 코드 생성
4. 선택적으로 통합 테스트 실행

### Groovy Maven 플러그인을 사용한 구현

이 글에서는 `groovy-maven-plugin`을 통해 컨테이너를 시작하는 방법을 보여줍니다:

```groovy
db = new org.testcontainers.containers.PostgreSQLContainer(
  "postgres:latest")
  .withUsername("${db.username}")
  .withDatabaseName("postgres")
  .withPassword("${db.password}");

db.start();
project.properties.setProperty('db.url', db.getJdbcUrl());
```

### Flyway 통합

코드 생성 전 동일한 빌드 단계에서 마이그레이션을 실행합니다:

```xml
<plugin>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-maven-plugin</artifactId>
  <configuration>
    <url>${db.url}</url>
    <user>${db.username}</user>
    <password>${db.password}</password>
    <locations>
      <location>filesystem:src/main/resources/db/migration</location>
    </locations>
  </configuration>
</plugin>
```

### 통합 테스트 설정

Maven Surefire 플러그인에 컨테이너 세부 정보를 전달합니다:

```xml
<systemPropertyVariables>
  <db.url>${db.url}</db.url>
  <db.username>${db.username}</db.username>
  <db.password>${db.password}</db.password>
</systemPropertyVariables>
```

---

## 주요 장점

- 벤더별 기능 지원 - 실제 프로덕션 데이터베이스 제품 사용 가능
- 재현 가능한 빌드 - 일관된 컨테이너화된 환경
- 테스트 격리 - 각 빌드마다 새로운 데이터베이스 상태 제공
- CI/CD 친화적 - Docker 기반으로 모든 환경에서 작동

---

## 참고 자료

- GitHub 예제: jOOQ-testcontainers-flyway-example
- JDBC 지원에 대한 Testcontainers 문서
- JDBC 라운드 트립 비용 및 절차적 로직에 관한 관련 블로그 포스트

---

## 커뮤니티 인사이트 (댓글)

기여자들은 Gradle 기반 구현과 통합 접근 방식을 공유했으며, Airbyte의 데이터베이스 프로젝트 예제와 Testcontainers가 표준으로 지원하지 않는 데이터베이스를 위한 커스텀 솔루션도 포함되어 있습니다.
