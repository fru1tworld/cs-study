# 데이터베이스 추상화와 SQL 인젝션

> 원문: https://blog.jooq.org/database-abstraction-and-sql-injection/

저는 jOOQ와 경쟁 관계에 있는 다양한 데이터베이스 추상화 도구들의 사용자 그룹을 구독하고 있습니다. 그 중 하나가 ActiveJDBC인데, 이것은 Active Record 디자인 패턴의 Java 구현체입니다. 이 프로젝트의 유지보수자인 Igor Polevoy가 최근 다음과 같이 주장했습니다:

> "SQL 인젝션은 웹 애플리케이션 문제이며, ORM과 직접적인 관련이 없습니다. ActiveJDBC는 전달받은 모든 SQL을 처리할 것입니다."

정말 그럴까요? 데이터베이스 추상화 레이어가 SQL 인젝션 방지를 클라이언트 애플리케이션에 위임해야 할까요?

## SQL 인젝션 배경

SQL 인젝션은 대부분의 개발자들이 직업 생활 중 한 번쯤은 다뤄봤을 문제입니다. Wikipedia에서 이 문제를 잘 설명하고 있습니다. 다음과 같은 Java 코드(또는 다른 언어)가 있다고 가정해 봅시다:

```java
statement = "SELECT * FROM users WHERE name = '" + userName + "';";
```

"userName"이 HTTP 요청에서 가져온 변수라고 상상해 보세요. HTTP 요청 파라미터를 맹목적으로 붙여넣으면 다음과 같은 간단한 공격에 노출됩니다:

```
-- 공격자가 userName 필드에 다음 코드를 보냅니다:
userName = "a';DROP TABLE users; SELECT * FROM userinfo WHERE 't' = 't";

-- 결과적으로 다음과 같은 구문이 됩니다:
statement = "SELECT * FROM users WHERE name = 'a';"
          + "DROP TABLE users;"
          + "SELECT * FROM userinfo WHERE 't' = 't';";
```

이런 일이 당신에게는 일어나지 않는다고요? 아마 그럴 수도 있습니다. 하지만 이 문제는 Stack Overflow에서 꽤 자주 볼 수 있습니다. "SQL injection"으로 검색하면 2000개 이상의 결과가 나옵니다. 그러니까 당신이 이를 방지하는 방법을 알고 있더라도, 팀의 누군가는 모를 수 있습니다. 좋아요, 하지만 이 위협을 인지하지 못한 어떤 프로그래머가 500개의 구문 중 하나를 잘못 작성했다고 해서 그게 _그렇게_ 나쁜 건 아니지 않나요? 다시 생각해 보세요. sqlmap이라는 도구에 대해 들어본 적 있나요? 이 도구는 인젝션 문제의 심각도에 따라 몇 초/몇 분/몇 시간 내에 애플리케이션 내의 문제가 있는 페이지를 찾아낼 것입니다. 그뿐만 아니라 문제가 있는 페이지를 찾으면, 데이터베이스에서 모든 종류의 데이터를 추출할 수 있습니다. 정말 모든 종류의 데이터를 말하는 것입니다. sqlmap 기능 중 일부를 소개합니다:

> "사용자, 비밀번호 해시, 권한, 역할, 데이터베이스, 테이블 및 컬럼을 열거하는 것을 지원합니다. 특정 데이터베이스 이름, 모든 데이터베이스에 걸쳐 특정 테이블, 또는 모든 데이터베이스의 테이블에 걸쳐 특정 컬럼을 검색하는 것을 지원합니다. 데이터베이스 소프트웨어가 MySQL, PostgreSQL 또는 Microsoft SQL Server일 때 데이터베이스 서버의 기반 파일 시스템에서 모든 파일을 다운로드하고 업로드하는 것을 지원합니다. 데이터베이스 소프트웨어가 MySQL, PostgreSQL 또는 Microsoft SQL Server일 때 데이터베이스 서버의 기반 운영 체제에서 임의의 명령을 실행하고 표준 출력을 가져오는 것을 지원합니다."

그렇습니다! SQL 인젝션에 취약한 코드가 있다면, 공격자는 특정 상황에서 서버를 장악할 수 있습니다!! 우리 회사에서는 알려진 취약점이 있는 일부 오픈 소스 블로그 소프트웨어에 대해 샌드박스 환경에서 sqlmap을 시도해 보았습니다. 단 한 줄의 SQL도 작성하지 않고 순식간에 서버를 장악할 수 있었습니다.

## 데이터베이스 추상화와 SQL 인젝션

자, 이제 여러분의 관심을 끌었으니, Igor Polevoy가 말한 것에 대해 다시 생각해 봅시다:

> "SQL 인젝션은 웹 애플리케이션 문제이며, ORM과 직접적인 관련이 없습니다. ActiveJDBC는 전달받은 모든 SQL을 처리할 것입니다."

네, 그의 말이 맞을 수도 있습니다. ActiveJDBC가 JDBC를 위한 가벼운 래퍼로서 다음과 같은 멋진 CRUD 단순화를 가능하게 한다는 점을 고려하면 말이죠(그들의 웹사이트에서 가져온 예시입니다):

```java
List<Employee> people =
Employee.where("department = ? and hire_date > ? ", "IT", hireDate)
        .offset(21)
        .limit(10)
        .orderBy("hire_date asc");
```

SQL 인젝션의 위험을 발견하셨나요? 맞습니다. 기반 PreparedStatements에 바인드 값을 사용하더라도, 이 도구는 JDBC만큼이나 안전하지 않습니다. 주의를 기울이면 SQL 인젝션을 피할 수 있습니다. 또는 곳곳에서 문자열을 연결하기 시작할 수도 있습니다. 하지만 그것을 인지하고 있어야 합니다!

jOOQ는 이런 상황을 어떻게 처리할까요? jOOQ 매뉴얼에서 바인드 값이 명시적으로 또는 암시적으로 어떻게 처리되는지 설명합니다. 다음은 몇 가지 예시입니다:

```java
// "Poe"에 대한 바인드 값을 암시적으로 생성
create.select()
      .from(T_AUTHOR)
      .where(LAST_NAME.equal("Poe"));

// "Poe"에 대한 (명명된) 바인드 값을 명시적으로 생성
create.select()
      .from(T_AUTHOR)
      .where(LAST_NAME.equal(param("lastName", "Poe")));

// 생성된 SQL 문자열에 "Poe"를 명시적으로 인라인
create.select()
      .from(T_AUTHOR)
      .where(LAST_NAME.equal(inline("Poe")));
```

위의 예시들은 다음을 생성합니다:

```
SELECT * FROM T_AUTHOR WHERE LAST_NAME = ?
SELECT * FROM T_AUTHOR WHERE LAST_NAME = ?
SELECT * FROM T_AUTHOR WHERE LAST_NAME = 'Poe'
```

"Poe"가 인라인되는 경우, 구문 오류와 SQL 인젝션을 방지하기 위해 이스케이핑이 jOOQ에 의해 처리됩니다. 하지만 jOOQ는 생성된 SQL에 SQL 문자열을 직접 주입하는 것도 지원합니다. 예를 들면:

```java
// 일반 SQL을 jOOQ에 주입
create.select()
      .from(T_AUTHOR)
      .where("LAST_NAME = 'Poe'");
```

이 경우, JDBC와 마찬가지로 SQL 인젝션이 발생할 수 있습니다.

## 결론

본질적으로, Igor의 말이 맞습니다. 자신의 코드로 인해 생성되는 SQL 인젝션 문제를 인식하는 것은 (클라이언트) 애플리케이션 개발자의 책임입니다. 하지만 JDBC 위에 구축된 데이터베이스 추상화 프레임워크가 API에서 가능한 한 SQL 인젝션을 피할 수 있다면, 그것이 더 좋습니다. SQL 인젝션 관점에서, 데이터베이스 추상화 프레임워크는 세 가지 범주로 나눌 수 있습니다:

단순 유틸리티. 여기에는 Spring의 JdbcTemplate이나 Apache의 DbUtils가 포함됩니다. 이들은 실제로 JDBC API를 편의성으로 향상시킬 뿐입니다(더 적은 예외 처리, 더 적은 장황함, 더 간단한 변수 바인딩, 더 간단한 데이터 페칭). 물론, 이러한 도구들은 SQL 인젝션을 방지하지 않습니다.

완전한 SQL 추상화. 여기에는 jOOQ, JaQu, JPA의 CriteriaQuery 등이 포함됩니다. 이들의 일반적인 작동 모드는 생성된 SQL에 항상 바인드 값을 렌더링합니다. 이것은 대부분의 경우 SQL 인젝션을 방지합니다.

그 외. 다른 많은 프레임워크들(ActiveJDBC와 Hibernate 포함)은 주로 (SQL 또는 HQL) 문자열 연산에 기반합니다. 이들은 많은 SQL 관련 사항을 추상화하지만, SQL 인젝션을 전혀 방지하지 않습니다.

따라서, Java 애플리케이션에서 SQL 추상화 도구를 선택할 때, SQL 인젝션의 심각성을 주의하세요. 그리고 당신의 도구가 이를 방지하는 데 도움이 되는지 아닌지도 주의하세요!

## Igor의 응답

Igor가 이 글에 대해 흥미로운 응답을 게시했음을 참고하세요.
