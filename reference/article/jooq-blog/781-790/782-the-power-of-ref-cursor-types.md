# REF CURSOR 타입의 힘

> 원문: https://blog.jooq.org/the-power-of-ref-cursor-types/

2011년 7월 24일 lukaseder 작성

많은 RDBMS들이 CURSOR, REF CURSOR 또는 ARRAY/TABLE 타입에 대한 지원을 구현하기 시작했습니다. 이러한 타입들은 JDBC의 `java.sql.Array`와 `java.sql.ResultSet`과 대략 동일한 의미를 가집니다. 원칙적으로, 이러한 타입들은 SQL의 어디에서든 나타날 수 있지만, 일부 RDBMS는 특정 기능 하위 집합에 대한 지원만 제한합니다. 이러한 타입들이 정확히 무엇인지 알아보겠습니다.

## ARRAY 타입

ARRAY 타입은 가장 이해하기 쉽습니다. 배열은 보통 타입이 지정된 값의 컬렉션으로 구현됩니다. 테이블 컬럼과 저장 프로시저 모두에서 사용할 수 있습니다. 대부분의 데이터베이스 스키마 설계자들은 테이블에서 ARRAY 타입을 사용하는 것이 반드시 좋은 생각은 아니라는 데 동의할 것입니다. 이는 "제1정규형이 아닌" 형태로 정규화된 스키마로 이어지기 때문입니다. 반면에, ARRAY 타입은 저장 프로시저 파라미터로 사용될 때, 특히 저장 함수의 결과로 사용될 때 매우 강력할 수 있습니다. 두 시나리오(테이블 컬럼, 함수 결과) 모두에서, 배열은 `TABLE(...)` 또는 `UNNEST(...)`와 같은 연산자를 사용하여 언네스팅할 수 있습니다. 이러한 연산자들은 배열의 내용을 테이블을 인수로 취하는 모든 SQL 절에서 사용할 수 있게 만들어줍니다.

예제 (HSQLDB):

```sql
-- 임시 익명 타입 배열 언네스팅
SELECT element
FROM unnest(array[1, 2, 3]) AS unnested(element);
```

유사한 예제 (익명 ARRAY 타입을 지원하지 않는 Oracle):

```sql
-- 숫자의 타입 배열 생성
CREATE TYPE number_array AS VARRAY(10) OF NUMBER(7);

-- 임시 배열 인스턴스 언네스팅. Oracle은 이러한
-- 언네스팅된 배열의 컬럼을 "COLUMN_VALUE"로 명명합니다
SELECT column_value
FROM table(number_array(1, 2, 3));
```

두 구조 모두 하나의 컬럼과 세 개의 레코드(1, 2, 3)를 가진 단순한 테이블을 결과로 생성합니다. 앞서 언급한 바와 같이, 이러한 ARRAY 타입의 진정한 힘은 저장 함수에서 결과로 사용할 때 분명해집니다. 예를 들어, Oracle에서는 다음과 같은 함수를 정의할 수 있습니다:

```sql
-- 간단한 예제 함수
CREATE FUNCTION get_array RETURN number_array IS
BEGIN
  RETURN number_array(1, 2, 3);
END;

-- 배열을 반환하는 함수의 결과 언네스팅
SELECT column_value
FROM TABLE(get_array);
```

이러한 구문적 개요를 통해, 잘 정의된 타입을 반환하는 매우 복잡한 함수를 정의할 수 있으며, 이를 다시 SQL 테이블로 언네스팅할 수 있습니다. ARRAY 타입을 OBJECT 타입과 결합하면 이 기능의 힘은 거의 무한해집니다(이를 지원하는 RDBMS에서). 다시 Oracle의 예를 들면:

```sql
-- 간단하고 재사용 가능한 person 타입
CREATE TYPE person AS OBJECT(
  id NUMBER(7),
  name VARCHAR2(100)
);

-- 이러한 사용자들의 배열
CREATE TYPE person_array AS VARRAY(10) OF person;

-- SQL에서 사용되는 이러한 사용자들의 언네스팅된 배열:
SELECT *
FROM TABLE(person_array(
  person(1, 'Jim'),
  person(2, 'Joe')
));
```

위의 SELECT 문은 직관적으로 다음과 같은 두 개의 컬럼과 두 개의 레코드를 가진 테이블을 결과로 생성합니다:

| ID | NAME |
|----|------|
| 1  | Jim  |
| 2  | Joe  |

다시 말하지만, 이것은 Jim과 Joe를 포함하는 위의 결과를 계산하기 위해 먼저 훨씬 더 복잡한 처리를 수행하는 저장 함수와 함께 사용될 수 있습니다.

## TABLE 타입

일부 RDBMS(예: Oracle)는 인메모리 ARRAY 타입(예: 이전에 본 VARRAY 타입)과 인메모리 TABLE 타입을 구별합니다. 이 글에서 주요한 차이점은 VARRAY 타입은 최대 크기가 있는 반면, TABLE 타입은 임의의 길이로 확장할 수 있다는 사실입니다. 또한, 예를 들어 다른 테이블에 중첩된 테이블에 레코드를 추가하려는 경우, SQL에서 직접 중첩 테이블을 조작하기 위한 Oracle의 API가 VARRAY 타입보다 더 풍부합니다. 그러나 여기서 철저한 비교는 범위를 벗어날 것입니다.

## REF CURSOR

커서, 특히 REF CURSOR는 데이터를 직접 모두 포함하지 않고 반복(또는 "루프")할 수 있다는 점에서 다르게 처리됩니다. 또한, REF CURSOR는 약하게 타입이 지정된 객체입니다. 이는 REF CURSOR의 컬럼 수와 타입이 SQL 컴파일 시점(또는 "파싱 시점")에 알 수 없고, SQL 문이 실행될 때에만 알 수 있다는 것을 의미합니다. 이 때문에 SQL에서 직접 REF CURSOR를 사용하기가 더 어렵습니다. 예를 들어, Oracle의 `TABLE(...)` 함수는 REF CURSOR 타입을 파라미터로 지원하지 않습니다. JDBC를 사용하여 저장 프로시저에서 Oracle 테이블 타입을 페치하는 것에 대한 가능한 것과 불가능한 것에 대한 개요는 Stack Overflow 질문도 참조하세요. 그럼에도 불구하고, REF CURSOR는 저장 프로시저 또는 저장 함수에서 반환될 수 있으며, JDBC에서 다른 `java.sql.ResultSet`과 마찬가지로 가져올 수 있습니다.

## jOOQ의 ARRAY/CURSOR 타입 지원

jOOQ의 주요 목표 중 하나는 사용자가 고급 RDBMS 개념을 Java에 쉽게 통합할 수 있도록 하는 것입니다. 이는 JDBC를 사용할 때 쉽지 않은 일입니다. 이러한 개념들은 (제가 아는 한) 아직 SQL:2008에서 표준화되지 않았습니다. 이러한 개념을 지원하는 모든 RDBMS는 각자의 구문을 사용합니다. 가장 독특하면서도 (가장 강력한) 구문은 H2 데이터베이스에서 발견되는데, 배열의 언네스팅을 다음과 같이 할 수 있습니다:

```sql
-- 임시 익명 타입 배열 언네스팅
SELECT *
FROM TABLE(
  ID INT=(1, 2),
  NAME VARCHAR=('Hello', 'World'));
```

사용자로부터 이러한 많은 SQL 구문 사실을 숨기는 것 외에도, jOOQ는 사용자로부터 JDBC 문 준비와 타입 매핑도 숨기는 것을 목표로 합니다. Oracle에서 준비된 문에 배열을 전달하는 것은 간단하지 않으며, 저장 함수에서 ResultSet을 페치하는 것도 마찬가지입니다. 그리고 Hibernate/JPA, Spring, myBatis 등을 포함한 오늘날의 주요 프레임워크 중 어느 것도 이러한 데이터 타입과 저장 프로시저를 자동으로 Java에 쉽게 통합하는 방법을 지원하지 않습니다 - Spring은 프로시저당 하나의 사용자 정의 매퍼를 작성할 수 있게 해주긴 하지만 말입니다. jOOQ에서는 이것이 가능해질 것입니다. ARRAY 타입은 꽤 오래전부터 jOOQ에서 지원되어 왔지만, 이를 언네스팅하는 것은 현재 진행 중인 "프로젝트 CURSOR"의 일부입니다. API는 위에서 SQL로 제시된 예제를 처리할 수 있도록 확장되고 있습니다. 이러한 API 사용 예는 다음과 같습니다:

```java
// 일반적인 jOOQ 팩토리 생성
Factory create = new OracleFactory(connection);

// 생성된 getArray 함수에서 반환된 값들을 반복
for (Record record : create.select()
                           .from(create.table(getArray()))
                           .fetch()) {

    // 이것은 1, 2, 3을 출력합니다
    System.out.println(record.getValue(0));
}
```

## 미래 투자로서의 고급 데이터 타입

이러한 데이터 타입의 힘은 PL/SQL 및 기타 데이터베이스 언어를 사용하는 DBA와 데이터베이스 프로그래머들에게 오랫동안 알려져 있었고 (사랑받아 왔습니다). Java 개발자들은 JDBC 및/또는 JPA 지원의 어색함(또는 부재) 때문에 주로 이를 기피해 왔습니다. 경험이 부족한 JDBC 개발자가 Oracle 준비된 문에 객체 배열을 올바르게 바인딩하거나, CallableStatement에서 REF CURSOR를 페치하는 것은 어렵습니다. 여기서 묘사된 기능은 이미 jOOQ 1.6.3에서 사용할 수 있습니다. CURSOR와 ARRAY 타입을 다루는 많은 다른 기능들이 가까운 미래에 구현될 것입니다. ...그러니 여러분의 데이터베이스가 제공하는 전체 기능 세트를 사용하기 시작하세요!
