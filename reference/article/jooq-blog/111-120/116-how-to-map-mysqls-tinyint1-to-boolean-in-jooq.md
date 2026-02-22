# jOOQ에서 MySQL의 TINYINT(1)을 Boolean으로 매핑하는 방법

> 원문: https://blog.jooq.org/how-to-map-mysqls-tinyint1-to-boolean-in-jooq/

## 개요

MySQL 8은 네이티브 `BOOLEAN` 타입을 지원하지 않습니다. 대신 `BOOL`은 단순히 `TINYINT`의 별칭(alias)입니다. `BOOL` 타입으로 컬럼을 생성하면 실제로는 `TINYINT(1)`로 저장되며, 여기서 `(1)`은 정밀도(precision)가 아닌 표시 너비(display width)를 나타냅니다.

## MySQL의 동작 방식

`TINYINT(1)`에서 `(1)`은 정밀도가 아닙니다. 이것은 "일부 더 이상 사용되지 않는(deprecated) 모드를 사용하여 데이터를 가져올 때 타입의 표시 너비"를 나타냅니다. MySQL은 이러한 컬럼에 불리언이 아닌 값도 허용합니다:

```sql
insert into t(b) values (0), (1), (10);
select * from t where b;
```

이 쿼리는 값이 1과 10인 행을 반환하며, MySQL이 불리언 컨텍스트에서 0이 아닌 모든 값을 true로 처리한다는 것을 보여줍니다.

## jOOQ 설정

기본적으로 jOOQ는 `TINYINT(1)`을 불리언 컬럼으로 인식하지 않습니다. 개발자들이 불리언 의도 없이 이 타입을 사용할 수도 있기 때문입니다. jOOQ 3.12.0부터 타입 설정에서 표시 너비를 매칭할 수 있습니다:

```xml
<forcedTypes>
  <forcedType>
    <name>BOOLEAN</name>
    <includeTypes>(?i:TINYINT\(1\))</includeTypes>
  </forcedType>
</forcedTypes>
```

이 설정을 사용하면 더 깔끔한 코드를 작성할 수 있습니다:

```java
selectFrom(T).where(T.B).fetch();
```

이 방식은 코드 생성 시 모든 `TINYINT(1)` 컬럼을 일관되게 불리언으로 처리합니다.
