# 누수하는 추상화, 또는 Hibernate로 Oracle DATE를 올바르게 바인딩하는 방법

> 원문: https://blog.jooq.org/leaky-abstractions-or-how-to-bind-oracle-date-correctly-with-hibernate/

최근 우리는 [SQL/JDBC와 jOOQ에서 Oracle DATE 타입을 올바르게 바인딩하는 방법](https://blog.jooq.org/are-you-binding-your-oracle-dates-correctly-i-bet-you-arent/)에 대해 블로그에 글을 썼다. 이 글은 [reddit에서 꽤 관심을 받았고](https://www.reddit.com/r/java/comments/2opr2g/are_you_binding_your_oracle_dates_correctly_i_bet/), Hibernate, JPA, 트랜잭션 관리, 커넥션 풀링에 대해 [자신의 블로그](http://vladmihalcea.com/)에서 자주 글을 쓰는 Vlad Mihalcea로부터 흥미로운 코멘트가 있었다.

Vlad는 이 문제가 Hibernate로도 해결될 수 있다고 지적했다.

## 문제점

문제가 무엇인지 빠르게 요약해보자. Oracle의 DATE는 SQL 표준이나 다른 모든 데이터베이스, 또는 java.sql.Date에서 말하는 날짜가 실제로 아니다. Oracle의 DATE 타입은 실제로 TIMESTAMP(0)이다. 즉, 소수점 이하 초 정밀도가 0인 타임스탬프다.

쿼리가 Oracle DATE 컬럼(해당 컬럼에 인덱스가 있는)에 필터를 사용하고, 바인드 값에 java.sql.Timestamp를 사용하면 문제가 생긴다. 예를 들어:

```java
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * " +
    "FROM rentals " +
    "WHERE rental_date > ? AND rental_date < ?");

stmt.setTimestamp(1, start);
stmt.setTimestamp(2, end);
```

그러면 실행 계획이 매우 나빠질 것이다. FULL TABLE SCAN 또는 어쩌면 INDEX FULL SCAN이 발생하는데, 정작 일반 INDEX RANGE SCAN이 되어야 한다. 실행 계획에서 "Predicate Information" 부분을 보면 다음과 같은 것을 볼 수 있다:

```
Predicate Information (identified by operation id):
---------------------------------------------------
   1 - filter(INTERNAL_FUNCTION("RENTAL_DATE")>=:1 AND
              INTERNAL_FUNCTION("RENTAL_DATE")<=:2)
```

INTERNAL_FUNCTION은 데이터베이스의 덜 정밀한 DATE 컬럼을 바인드 변수의 더 정밀한 TIMESTAMP 타입에 맞추기 위해 변환하는 Oracle의 메커니즘이다. 이 "확장 변환(widening conversion)"은 옵티마이저가 범위 스캔을 효과적으로 사용하는 것을 방해하여, 대신 전체 인덱스 스캔을 강제한다.

해결책은 바인드 변수를 DATE로 명시적으로 캐스팅하거나, 벤더 특화 oracle.sql.DATE 타입을 사용하는 것이다.

## Hibernate의 UserType을 사용한 해결

Hibernate의 독점 API인 `org.hibernate.usertype.UserType`을 사용하여 이 문제를 해결할 수 있다.

다음은 완전한 구현이다:

```java
import oracle.sql.DATE;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/
 * java.sql.Timestamp 값에 대해 Oracle DATE를 올바르게 바인딩하기 위한
 * 커스텀 Hibernate UserType
 */
public class OracleDate implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.TIMESTAMP };
    }

    @Override
    public Class<?> returnedClass() {
        return Timestamp.class;
    }

    @Override
    public Object nullSafeGet(
        ResultSet rs,
        String[] names,
        SessionImplementor session,
        Object owner
    ) throws HibernateException, SQLException {
        return rs.getTimestamp(names[0]);
    }

    @Override
    public void nullSafeSet(
        PreparedStatement st,
        Object value,
        int index,
        SessionImplementor session
    ) throws HibernateException, SQLException {

        // 여기가 핵심이다. 벤더 특화 oracle.sql.DATE 타입을 사용한다
        st.setObject(index, new DATE(value));
    }

    // ... equals, hashCode, 기타 메서드들

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y || (x != null && x.equals(y));
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
        throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
        throws HibernateException {
        return original;
    }
}
```

핵심은 `nullSafeSet()` 메서드에 있는데, 이 메서드가 벤더 특화 `oracle.sql.DATE` 타입을 사용하여 실행 계획에서 명시적으로 바인드 변수를 캐스팅하는 것과 동일한 효과를 낸다.

이것을 엔티티에 적용하려면 다음 어노테이션들을 사용한다:

```java
@Entity
@TypeDefs(value = @TypeDef(
    name = "oracle_date",
    typeClass = OracleDate.class
))
public class Rental {

    @Column(name = "rental_date")
    @Type(type = "oracle_date")
    private Timestamp rentalDate;

    // ... 나머지 엔티티 코드
}
```

## JPA 2.1 AttributeConverter의 한계

JPA 2.1의 `AttributeConverter`가 적합해 보일 수 있다. 다음과 같이 작성할 수 있다:

```java
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import oracle.sql.DATE;

import java.sql.Timestamp;

@Converter
public class OracleDateConverter
    implements AttributeConverter<Timestamp, DATE> {

    @Override
    public DATE convertToDatabaseColumn(Timestamp attribute) {
        return attribute == null ? null : new DATE(attribute);
    }

    @Override
    public Timestamp convertToEntityAttribute(DATE dbData) {
        return dbData == null ? null : dbData.timestampValue();
    }
}
```

불행히도 Hibernate 4.3.7은 당신이 `VARBINARY` 타입의 변수를 바인딩하려고 한다고 생각할 것이다. Hibernate가 Oracle의 벤더 특화 DATE 타입을 인식하지 못하기 때문에, JPA 표준 솔루션은 이 문제에 대해 충분하지 않다. 이것은 [HHH-9553](https://hibernate.atlassian.net/browse/HHH-9553)으로 버그가 등록되어 있다.

## 올바른 실행 계획

바인딩이 올바르게 수행되면(DATE로 캐스팅), 실행 계획은 TABLE ACCESS FULL 대신 INDEX RANGE SCAN으로 효율적이 된다:

```
Predicate Information (identified by operation id):
---------------------------------------------------
   1 - access("RENTAL_DATE">=:1 AND "RENTAL_DATE"<=:2)
```

INTERNAL_FUNCTION이 사라지고, 옵티마이저가 인덱스를 올바르게 사용할 수 있게 되어 쿼리 성능이 크게 향상된다.

## 결론: 누수하는 추상화에 대하여

추상화는 모든 수준에서 누수한다. 비록 그것이 JCP에 의해 "표준"으로 간주되더라도 말이다. 표준은 종종 사후에 산업계의 사실상 표준을 정당화하는 수단이다(물론 약간의 정치가 관련되어 있다). Hibernate가 표준으로 시작하지 않았고 14년 전에 표준적인 J2EE 사람들이 영속성에 대해 생각하던 방식을 대대적으로 혁신했다는 것을 잊지 말자.

관련된 추상화 계층들을 나열해보자:

1. Oracle SQL - 실제 구현. 날짜는 DATE 타입으로 저장된다.
2. SQL 표준 - DATE(시간 없이)와 TIMESTAMP(시간 포함)를 지정하지만, Oracle은 이를 무시한다.
3. ojdbc - Oracle 기능을 위해 JDBC를 확장한다.
4. JDBC - 시간 타입에 대해 SQL 표준을 따른다.
5. Hibernate - Oracle/ojdbc 기능을 위한 독점 API를 제공한다.
6. JPA - 시간 타입에 대해 SQL 표준과 JDBC를 따른다.

실제 구현(Oracle SQL)이 Hibernate의 UserType이나 JPA의 Converter를 통해 당신의 엔티티 모델로 바로 누수되었다. 표준화된 추상화에만 의존하기보다는 실제 성능 문제를 해결하려면 Oracle SQL, JDBC, Hibernate 계층 전반에 걸친 벤더 특화 API가 필요하다.

실행 전 사이에 새로운 계획의 계산을 강제하기 위해 공유 풀과 버퍼 캐시를 플러시하는 것을 잊지 마라. 생성된 SQL은 매번 동일하기 때문이다:

```sql
ALTER SYSTEM FLUSH SHARED_POOL;
ALTER SYSTEM FLUSH BUFFER_CACHE;
```

jOOQ는 Hibernate의 대안이 될 수 있지만, 반드시 Hibernate를 완전히 대체할 필요는 없다. 많은 사용자들이 jOOQ와 Hibernate를 결합하여 사용할 때 긍정적인 경험을 보고했다. Hibernate가 지루한 CRUD 작업을 처리하게 하고, jOOQ가 정교하면서도 직관적인 쿼리 DSL을 통해 복잡한 쿼리와 리포팅을 처리하게 하는 것이다.
