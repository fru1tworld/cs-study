# 아직 SQL PIVOT을 사용하지 않는가? 사용해야 한다!

> 원문: https://blog.jooq.org/are-you-using-sql-pivot-yet-you-should/

때때로 SQL을 작업하다 보면 흔치 않은 요구사항을 마주하게 됩니다. 이러한 요구사항 중 하나가 행을 열로 변환하는 것인데, 이를 "피벗팅(pivoting)"이라고 합니다. Stack Overflow의 Valiante 사용자는 정확히 이런 상황에 처해 있었습니다. 다음과 같은 테이블이 있다고 가정해 봅시다:

| dnId | propNameId | propertyName    | propertyValue     |
|------|------------|-----------------|-------------------|
| 1    | 1          | objectsid       | S-1-5-32-548      |
| 1    | 2          | _objectclass    | group             |
| 1    | 3          | cn              | Account Operators |
| 1    | 4          | samaccountname  | Account Operators |
| 1    | 5          | name            | Account Operators |
| 2    | 1          | objectsid       | S-1-5-32-544      |
| 2    | 2          | _objectclass    | group             |
| 2    | 3          | cn              | Administrators    |
| 2    | 4          | samaccountname  | Administrators    |
| 2    | 5          | name            | Administrators    |
| 3    | 1          | objectsid       | S-1-5-32-551      |
| 3    | 2          | _objectclass    | group             |
| 3    | 3          | cn              | Backup Operators  |
| 3    | 4          | samaccountname  | Backup Operators  |
| 3    | 5          | name            | Backup Operators  |

위 데이터에서 원하는 것은 "행을 열로 변환"하는 것입니다. 즉, 각 `dnId`당 하나의 행만 있고, 속성 이름이 열이 되며, 속성 값이 해당 열의 값이 되도록 하는 것입니다:

| dnId | objectsid       | _objectclass | cn                | samaccountname    | name              |
|------|-----------------|--------------|-------------------|-------------------|-------------------|
| 1    | S-1-5-32-548    | group        | Account Operators | Account Operators | Account Operators |
| 2    | S-1-5-32-544    | group        | Administrators    | Administrators    | Administrators    |
| 3    | S-1-5-32-551    | group        | Backup Operators  | Backup Operators  | Backup Operators  |

## SQL Server PIVOT 사용하기

Oracle과 SQL Server는 이 목적을 위해 `PIVOT` 키워드를 제공합니다. SQL Server에서 어떻게 작동하는지 살펴보겠습니다:

```sql
SELECT p.*
FROM (
  SELECT dnId, propertyName, propertyValue
  FROM myTable
) AS t
PIVOT(
  MAX(propertyValue)
  FOR propertyName IN (
    objectsid, _objectclass, cn, samaccountname, name
  )
) AS p;
```

이 쿼리에서 몇 가지 주목할 점이 있습니다:

1. 피벗하려는 열만 선택하기 위해 파생 테이블 `t`를 사용합니다. 이것은 `PIVOT` 절이 결과에서 원하는 열을 어떻게 알 수 있는지 때문에 중요합니다.
2. `PIVOT` 절은 집계 함수(여기서는 `MAX`)를 사용합니다. 이는 동일한 `dnId`와 `propertyName` 조합에 대해 여러 값이 있을 수 있기 때문에 필요합니다.
3. `FOR ... IN` 절은 피벗하려는 속성 이름들을 지정합니다.

## Oracle PIVOT 사용하기

Oracle에서의 문법은 약간 다릅니다. 특히 `IN` 절에서 문자열 리터럴을 사용해야 하고, 별칭을 지정해야 합니다:

```sql
SELECT p.*
FROM (
  SELECT dnId, propertyName, propertyValue
  FROM myTable
) t
PIVOT(
  MAX(propertyValue)
  FOR propertyName IN (
    'objectsid' AS "objectsid",
    '_objectclass' AS "_objectclass",
    'cn' AS "cn",
    'samaccountname' AS "samaccountname",
    'name' AS "name"
  )
) p;
```

Oracle에서는 `IN` 절 내의 값들이 문자열 리터럴로 작은따옴표로 감싸져 있고, `AS` 키워드를 사용하여 결과 열의 별칭을 지정합니다.

## PIVOT 작동 방식

`PIVOT`(JOIN과 마찬가지로)은 테이블 참조에 적용되어 변환하는 키워드입니다. `PIVOT` 절의 주요 구성 요소를 이해해 봅시다:

1. 집계 함수: `MAX(propertyValue)`와 같은 집계 함수가 필요합니다. 이는 피벗되지 않은 열(이 경우 `dnId`)로 그룹화하고, 각 피벗 열에 대해 값을 집계하기 때문입니다.

2. FOR 절: 피벗할 열을 지정합니다. 이 열의 고유 값들이 새로운 열 이름이 됩니다.

3. IN 절: 어떤 값들을 열로 변환할지 지정합니다. 여기에 나열된 값만 결과에 열로 나타납니다.

`PIVOT`이 적용되면, 파생 테이블에서 `FOR` 절에 지정된 열과 집계되는 열이 제거되고, `IN` 절에 나열된 각 값에 대해 새로운 열이 생성됩니다. 나머지 열(여기서는 `dnId`)은 암시적으로 그룹화 기준이 됩니다.

## 더 복잡한 예제: 파생 테이블과 조인하기

`PIVOT`의 진정한 힘은 다른 쿼리와 결합할 때 발휘됩니다. 예를 들어, 피벗된 테이블을 다른 파생 테이블과 조인하여 특정 속성-값 쌍이 누락된 행을 식별할 수 있습니다:

```sql
SELECT d.dnId, p.*
FROM (
  SELECT DISTINCT dnId
  FROM myTable
) d
LEFT JOIN (
  SELECT dnId, propertyName, propertyValue
  FROM myTable
) t
PIVOT(
  MAX(propertyValue)
  FOR propertyName IN (
    objectsid, _objectclass, cn, samaccountname, name
  )
) p ON d.dnId = p.dnId
WHERE p.objectsid IS NULL
   OR p._objectclass IS NULL
   OR p.cn IS NULL;
```

이 쿼리는 모든 고유한 `dnId` 값을 가져온 다음, 피벗된 결과와 LEFT JOIN하여 특정 속성이 누락된 레코드를 찾습니다.

## PIVOT을 지원하지 않는 데이터베이스를 위한 대안

단순한 PIVOT 시나리오에서 Oracle이나 SQL Server 이외의 데이터베이스 사용자는 `GROUP BY`와 `MAX(CASE ...)` 표현식을 사용하여 동등한 쿼리를 작성할 수 있습니다:

```sql
SELECT
  dnId,
  MAX(CASE WHEN propertyName = 'objectsid' THEN propertyValue END) AS objectsid,
  MAX(CASE WHEN propertyName = '_objectclass' THEN propertyValue END) AS _objectclass,
  MAX(CASE WHEN propertyName = 'cn' THEN propertyValue END) AS cn,
  MAX(CASE WHEN propertyName = 'samaccountname' THEN propertyValue END) AS samaccountname,
  MAX(CASE WHEN propertyName = 'name' THEN propertyValue END) AS name
FROM myTable
GROUP BY dnId;
```

이 접근 방식은 MySQL, PostgreSQL, SQLite 등 `PIVOT` 키워드를 지원하지 않는 거의 모든 관계형 데이터베이스에서 작동합니다. `CASE` 표현식은 각 행에 대해 조건부로 값을 반환하고, `MAX` 집계 함수는 그룹 내에서 NULL이 아닌 값을 선택합니다.

## jOOQ에서의 PIVOT

jOOQ는 API를 통해 SQL `PIVOT` 절을 지원합니다. jOOQ를 사용하면 타입 안전한 방식으로 피벗 쿼리를 작성할 수 있습니다:

```java
// jOOQ를 사용한 PIVOT 쿼리 예제
create.select()
      .from(
          select(MY_TABLE.DN_ID, MY_TABLE.PROPERTY_NAME, MY_TABLE.PROPERTY_VALUE)
          .from(MY_TABLE)
      )
      .pivot(max(MY_TABLE.PROPERTY_VALUE))
      .on(MY_TABLE.PROPERTY_NAME)
      .in(
          "objectsid",
          "_objectclass",
          "cn",
          "samaccountname",
          "name"
      )
      .fetch();
```

jOOQ는 대상 데이터베이스에 따라 적절한 SQL을 생성하며, `PIVOT`을 네이티브로 지원하지 않는 데이터베이스의 경우 `GROUP BY`와 `CASE` 표현식을 사용한 동등한 쿼리로 에뮬레이션합니다.

## 결론

SQL `PIVOT`은 정규화된 이름-값 쌍 데이터를 비정규화된 열 형식으로 변환해야 할 때 매우 유용한 기능입니다. Oracle과 SQL Server에서는 `PIVOT` 키워드를 직접 사용할 수 있고, 다른 데이터베이스에서는 `GROUP BY`와 `CASE` 표현식으로 동일한 결과를 얻을 수 있습니다.

다음에 행을 열로 변환해야 할 때, `PIVOT`을 떠올려 보세요!
