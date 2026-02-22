# JAX-RS와 jOOQ로 간단한 라이선스 서버 구축하기

> 원문: https://blog.jooq.org/using-jooq-with-jax-rs-to-build-a-simple-license-server/

## 소개

일부 사용 사례에서는 가벼운 단일 계층 서버 측 아키텍처가 바람직합니다. 일반적으로 RESTful API를 노출하고 클라이언트 측에서 AngularJS와 같은 프레임워크를 사용하여 UI를 구현하는 방식입니다.

## Java 표준과 기술

JAX-RS는 "RESTful 애플리케이션을 위한 표준 API"로서 "JEE 7의 일부"이며, 표준 JSON 구현도 함께 제공됩니다. JAX-RS는 JEE 컨테이너 외부에서도 사용할 수 있습니다.

이 예제에서는 다음과 같은 핵심 기술들을 사용합니다:
- 빌드 및 실행을 위한 Maven
- 가벼운 Servlet 구현체인 Jetty
- JAX-RS 참조 구현체인 Jersey
- 데이터 접근 계층으로 jOOQ
- PostgreSQL 데이터베이스

## 데이터베이스 스키마

라이선스 서버를 위해 두 개의 테이블을 생성합니다:

LICENSE 테이블은 다음 컬럼들로 라이선스 정보를 저장합니다:
- ID (SERIAL8, 기본 키)
- LICENSE_DATE (타임스탬프)
- LICENSEE (이메일 주소를 저장하는 텍스트)
- LICENSE (라이선스 키를 저장하는 텍스트)
- VERSION (정규식 패턴을 저장하는 varchar, 기본값 '.*')

LOG_VERIFY 테이블은 검증 시도를 추적합니다:
- ID (SERIAL8, 기본 키)
- LICENSEE (텍스트)
- LICENSE (텍스트)
- REQUEST_IP (varchar)
- VERSION (varchar)
- MATCH (불리언)

저장 함수가 라이선스 키를 생성합니다:

```sql
CREATE OR REPLACE FUNCTION
LICENSE_SERVER.GENERATE_KEY(
    IN license_date TIMESTAMP WITH TIME ZONE,
    IN email TEXT
) RETURNS VARCHAR
AS $$
BEGIN
    RETURN 'license-key';
END;
$$ LANGUAGE PLPGSQL;
```

"실제 알고리즘은 비밀 솔트(salt)를 사용하여 함수 인자들을 해시할 수 있습니다. 튜토리얼의 목적상 상수 문자열로 충분합니다."

## Maven 설정

프로젝트 POM은 다음을 포함합니다:

```xml
<modelVersion>4.0.0</modelVersion>
<groupId>org.jooq</groupId>
<artifactId>jooq-webservices</artifactId>
<packaging>war</packaging>
<version>1.0</version>
```

의존성에는 Jersey (JAX-RS 구현체), jOOQ, PostgreSQL 드라이버, 그리고 로깅이 포함됩니다. maven-compiler-plugin은 Java 1.7을 대상으로 하고, maven-jetty-plugin은 개발용으로 구성됩니다.

## 서비스 구현

`LicenseService` 클래스는 JAX-RS 데코레이터로 어노테이션됩니다:

```java
@Path("/license/")
@Component
@Scope("request")
public class LicenseService {
```

### Generate 엔드포인트

`/license/generate` 엔드포인트는 새로운 라이선스 키를 생성합니다:

```java
@GET
@Produces("text/plain")
@Path("/generate")
public String generate(
    final @QueryParam("mail") String mail
)
```

이 메서드는 jOOQ의 DSL을 사용하여 LICENSE 테이블에 레코드를 삽입하고, 데이터베이스 함수를 호출하여 키를 생성한 후 반환합니다.

### Verify 엔드포인트

`/license/verify` 엔드포인트는 라이선스를 검증합니다:

```java
@GET
@Produces("text/plain")
@Path("/verify")
public String verify(
    final @Context HttpServletRequest request,
    final @QueryParam("mail") String mail,
    final @QueryParam("license") String license,
    final @QueryParam("version") String version
)
```

이것은 LOG_VERIFY에 INSERT 쿼리를 수행합니다. 동등한 SQL은 다음과 같습니다:

```sql
INSERT INTO LOG_VERIFY (
  LICENSE,
  LICENSEE,
  REQUEST_IP,
  MATCH,
  VERSION
)
VALUES (
  :license,
  :mail,
  :remoteAddr,
  (SELECT COUNT(*) FROM LICENSE
   WHERE LICENSEE = :mail
   AND LICENSE = :license
   AND :version ~ VERSION) > 0,
  :version
)
RETURNING MATCH;
```

이것은 "상당히 흥미로운" 방식인데, 검증 로직을 INSERT 문에 직접 내장하기 때문입니다.

## 유틸리티 메서드

서비스는 "트랜잭션을 캡슐화하고 jOOQ DSLContext를 초기화하는" `run()` 메서드를 포함합니다.

```java
private String run(CtxRunnable runnable) {
    Connection c = null;
    try {
        Class.forName("org.postgresql.Driver");
        c = getConnection(
            "jdbc:postgresql:postgres",
            "postgres",
            System.getProperty("pw", "test"));
        DSLContext ctx = DSL.using(
            new DefaultConfiguration()
               .set(new DefaultConnectionProvider(c))
               .set(SQLDialect.POSTGRES)
               .set(new Settings()
                   .withExecuteLogging(false)));
        return runnable.run(ctx);
    }
    catch (Exception e) {
        e.printStackTrace();
        Response.status(Status.SERVICE_UNAVAILABLE);
        return "Service Unavailable";
    }
    finally {
        JDBCUtils.safeClose(c);
    }
}
```

함수형 인터페이스 `CtxRunnable`이 데이터베이스 작업을 추상화합니다.

## Spring 설정

Spring 설정 파일은 최소한으로 구성됩니다:

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="...">
  <context:component-scan
     base-package="org.jooq.example.jaxrs" />
</beans>
```

## Jetty 설정

`web.xml` 파일은 Spring과 Jersey를 구성합니다:

```xml
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>classpath:applicationContext.xml</param-value>
</context-param>
<listener>
    <listener-class>
        org.springframework.web.context.ContextLoaderListener
    </listener-class>
</listener>
<servlet>
    <servlet-name>Jersey Spring Web Application</servlet-name>
    <servlet-class>
        com.sun.jersey.spi.spring.container.servlet.SpringServlet
    </servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>Jersey Spring Web Application</servlet-name>
    <url-pattern>/*</url-pattern>
</servlet-mapping>
```

## 서버 실행

서버는 Maven으로 시작할 수 있습니다:

```
mvn jetty:run
```

커스텀 포트를 사용하려면:

```
mvn jetty:run -Djetty.port=8088
```

## API 테스트

세 가지 예제 요청이 제공됩니다:

라이선스 생성:
```
http://localhost:8088/jooq-jax-rs-example/license/generate?mail=test@example.com
```
반환값: `license-key`

유효한 라이선스 검증:
```
http://localhost:8088/jooq-jax-rs-example/license/verify?mail=test@example.com&license=license-key&version=3.2.0
```
반환값: `true`

유효하지 않은 라이선스 검증:
```
http://localhost:8088/jooq-jax-rs-example/license/verify?mail=test@example.com&license=wrong&version=3.2.0
```
반환값: `false`

## 데이터베이스 결과

테스트 요청을 실행한 후, 데이터베이스는 다음을 포함합니다:

LICENSE 테이블:
| id | licensee | license | version |
|----|----------|---------|---------|
| 3 | test@example.com | license-key | .* |

LOG_VERIFY 테이블:
| id | licensee | license | match |
|----|----------|---------|-------|
| 2 | test@example.com | license-key | t |
| 5 | test@example.com | wrong | f |

## 결론

"전체 예제는 Apache Software License 2.0 조건 하에 무료로 다운로드할 수 있습니다." GitHub 저장소: `https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-jax-rs-example`
