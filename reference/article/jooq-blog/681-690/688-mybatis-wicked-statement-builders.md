# MyBatis의 기발한 Statement 빌더

> 원문: https://blog.jooq.org/mybatis-wicked-statement-builders/

MyBatis는 JDBC 위에 구축된 데이터베이스 추상화 프레임워크로, 개발자가 SQL을 별도의 파일로 외부화할 수 있게 해줍니다. 이 프레임워크는 XML 기반 접근 방식으로 잘 알려져 있지만, 덜 알려진 기능인 statement 빌더도 제공합니다.

## 전통적인 MyBatis 접근 방식

이 프레임워크는 일반적으로 SQL 문을 위해 XML 설정 파일을 사용합니다:

```xml
<select id="selectPerson" parameterType="int" resultType="hashmap">
  SELECT * FROM PERSON WHERE ID = #{id}
</select>
```

## Statement 빌더 기능

MyBatis는 Java 코드가 SQL 문법과 유사하게 보이도록 하는 statement 빌더를 제공합니다. 다음은 공식 MyBatis 문서에서 가져온 예제입니다:

```java
private String selectPersonSql() {
  BEGIN(); // ThreadLocal 변수를 초기화
  SELECT("P.ID, P.USERNAME, P.PASSWORD, P.FULL_NAME");
  SELECT("P.LAST_NAME, P.CREATED_ON, P.UPDATED_ON");
  FROM("PERSON P");
  FROM("ACCOUNT A");
  INNER_JOIN("DEPARTMENT D on D.ID = P.DEPARTMENT_ID");
  INNER_JOIN("COMPANY C on D.COMPANY_ID = C.ID");
  WHERE("P.ID = A.ID");
  WHERE("P.FIRST_NAME like ?");
  OR();
  WHERE("P.LAST_NAME like ?");
  GROUP_BY("P.ID");
  HAVING("P.LAST_NAME like ?");
  OR();
  HAVING("P.FIRST_NAME like ?");
  ORDER_BY("P.ID");
  ORDER_BY("P.FULL_NAME");
  return SQL();
}
```

## 기술적 분석

이 접근 방식은 SQL 문 구성 중에 ThreadLocal 변수를 사용하여 상태를 유지하며, API는 내부 StringBuilder에 문자열을 추가합니다. 창의적이긴 하지만, 이 메커니즘은 비관례적이며 정적 스타일의 메서드 호출에 의존합니다.

이 글은 개념의 독창성을 인정하면서도 실용적 유용성에 대해서는 중립적인 입장을 취하며, INSERT, UPDATE, DELETE 문 예제는 전체 MyBatis 문서를 참조하도록 안내하고 있습니다.
