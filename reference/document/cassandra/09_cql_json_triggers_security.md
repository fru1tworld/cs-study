# CQL JSON, 트리거, 보안

> 원본: https://cassandra.apache.org/doc/latest/cassandra/cql/

---

## 목차

1. [JSON 지원(JSON Support)](#json-지원json-support)
   - [SELECT JSON](#select-json)
   - [INSERT JSON](#insert-json)
   - [Cassandra 데이터 타입의 JSON 인코딩](#cassandra-데이터-타입의-json-인코딩)
   - [fromJson() 함수](#fromjson-함수)
   - [toJson() 함수](#tojson-함수)
2. [트리거(Triggers)](#트리거triggers)
   - [CREATE TRIGGER](#create-trigger)
   - [DROP TRIGGER](#drop-trigger)
3. [보안(Security)](#보안security)
   - [데이터베이스 역할(Database Roles)](#데이터베이스-역할database-roles)
     - [CREATE ROLE](#create-role)
     - [ALTER ROLE](#alter-role)
     - [DROP ROLE](#drop-role)
     - [GRANT ROLE](#grant-role)
     - [REVOKE ROLE](#revoke-role)
     - [LIST ROLES](#list-roles)
   - [사용자(Users)](#사용자users)
     - [CREATE USER](#create-user)
     - [ALTER USER](#alter-user)
     - [DROP USER](#drop-user)
     - [LIST USERS](#list-users)
   - [데이터 제어(Data Control)](#데이터-제어data-control)
     - [권한(Permissions)](#권한permissions)
     - [GRANT PERMISSION](#grant-permission)
     - [REVOKE PERMISSION](#revoke-permission)
     - [LIST PERMISSIONS](#list-permissions)

---

## JSON 지원(JSON Support)

Cassandra 2.2부터 `SELECT` 및 `INSERT` 문에 JSON 지원이 추가되었습니다. CQL API를 근본적으로 변경하지는 않으며(스키마는 여전히 강제됩니다), JSON 문서를 편리하게 다룰 수 있는 수단을 제공합니다.

---

### SELECT JSON

`SELECT` 문에서 `JSON` 키워드를 사용하면 각 행(row)을 JSON 인코딩된 맵(map) 하나로 반환합니다. 결과 맵의 키(key)는 일반 결과 집합(result set)의 컬럼명(column name)과 동일합니다. 예를 들어 `SELECT JSON a, ttl(b) FROM ...` 쿼리는 `"a"`와 `"ttl(b)"`를 키로 가지는 맵을 반환합니다.

대문자를 포함하는 대소문자 구분(case-sensitive) 컬럼명은 큰따옴표(double quote)로 둘러싸입니다. 이는 `INSERT JSON` 동작과의 대칭성(symmetry)을 위한 것입니다. 예를 들어 `SELECT JSON myColumn FROM ...` 는 이스케이프된 맵 키 `"\"myColumn\""`를 결과로 만듭니다.

맵의 값(value)은 결과 집합 값의 JSON 인코딩 표현입니다.

---

### INSERT JSON

`JSON` 키워드를 사용하면 JSON 인코딩된 맵을 하나의 행으로 삽입할 수 있습니다. JSON 맵의 형식은 일반적으로 동일한 테이블에 대해 `SELECT JSON`이 반환하는 형식과 일치해야 하며, 대소문자 구분 컬럼명은 큰따옴표로 둘러싸여야 합니다.

예시:

```
INSERT INTO mytable JSON '{ "\"myKey\"": 0, "value": 0}';
```

기본적으로 생략된(omitted) 컬럼은 `NULL`로 설정되며, 기존 값을 제거하고 툼스톤(tombstone)을 생성합니다. `DEFAULT UNSET`을 사용하면 생략된 컬럼의 기존 값이 보존(preserve)됩니다.

---

### Cassandra 데이터 타입의 JSON 인코딩

Cassandra는 가능한 경우 데이터 타입을 네이티브(native) JSON 표현으로 인코딩하고 받아들입니다. 단일 필드(single-field) 타입의 경우 CQL 리터럴(literal) 형식과 일치하는 문자열 표현도 허용합니다. 컬렉션(collection), 튜플(tuple), 사용자 정의 타입(user-defined type) 같은 복합 타입(compound type)은 네이티브 JSON 컬렉션(맵 또는 리스트)이나 JSON 인코딩된 문자열 표현을 사용해야 합니다.

| 타입(Type) | 허용되는 입력 형식(Formats accepted) | 반환 형식(Return format) | 비고(Notes) |
|------|------------------|---------------|----|
| `ascii` | string | string | JSON의 `\u` 문자 이스케이프를 사용합니다. |
| `bigint` | integer, string | integer | 문자열은 유효한 64비트 정수여야 합니다. |
| `blob` | string | string | 문자열은 `0x` 다음에 짝수 개의 16진수(hex) 자리로 구성되어야 합니다. |
| `boolean` | boolean, string | boolean | 문자열은 `"true"` 또는 `"false"`여야 합니다. |
| `date` | string | string | 형식은 `YYYY-MM-DD`, 시간대는 UTC입니다. |
| `decimal` | integer, float, string | float | 클라이언트 디코더(decoder)에서 IEEE-754 정밀도를 초과할 수 있습니다. |
| `double` | integer, float, string | float | 문자열은 유효한 정수 또는 부동소수점(float)이어야 합니다. |
| `float` | integer, float, string | float | 문자열은 유효한 정수 또는 부동소수점이어야 합니다. |
| `inet` | string | string | IPv4 또는 IPv6 주소입니다. |
| `int` | integer, string | integer | 문자열은 유효한 32비트 정수여야 합니다. |
| `list` | list, string | list | JSON의 네이티브 리스트 표현을 사용합니다. |
| `map` | map, string | map | JSON의 네이티브 맵 표현을 사용합니다. |
| `smallint` | integer, string | integer | 문자열은 유효한 16비트 정수여야 합니다. |
| `set` | list, string | list | JSON의 네이티브 리스트 표현을 사용합니다. |
| `text` | string | string | JSON의 `\u` 문자 이스케이프를 사용합니다. |
| `time` | string | string | 형식은 `HH-MM-SS[.fffffffff]`입니다. |
| `timestamp` | integer, string | string | 문자열은 날짜 형태의 타임스탬프를 허용하며, 형식은 `YYYY-MM-DD HH:MM:SS.SSS`입니다. |
| `timeuuid` | string | string | 상수(constant) 형식에 따른 Type 1 UUID입니다. |
| `tinyint` | integer, string | integer | 문자열은 유효한 8비트 정수여야 합니다. |
| `tuple` | list, string | list | JSON의 네이티브 리스트 표현을 사용합니다. |
| `UDT` | map, string | map | 필드명을 키로 사용하는 네이티브 맵 표현입니다. |
| `uuid` | string | string | 상수 형식에 따릅니다. |
| `varchar` | string | string | JSON의 `\u` 문자 이스케이프를 사용합니다. |
| `varint` | integer, string | integer | 가변 길이(variable length)이며, 클라이언트 디코더에서 32/64비트를 초과(overflow)할 수 있습니다. |

---

### fromJson() 함수

`fromJson()` 함수는 `INSERT JSON`과 유사하게 동작하지만, 단일 컬럼 값(single column value)에만 적용됩니다. `INSERT` 문의 `VALUES` 절(clause), 또는 `UPDATE`·`DELETE`·`SELECT` 문의 컬럼 값 위치에서 사용할 수 있습니다. `SELECT` 문의 셀렉션 절(selection clause)에서는 사용할 수 없습니다.

---

### toJson() 함수

`toJson()` 함수는 `SELECT JSON`과 유사하게 동작하지만, 개별 컬럼 값(individual column value)에만 적용됩니다. `SELECT` 문의 셀렉션 절(selection clause)에서만 사용할 수 있습니다.

---

## 트리거(Triggers)

트리거(trigger)는 표준 식별자(identifier) 구문을 따르는 이름으로 식별됩니다. 트리거 로직은 Java를 포함한 모든 JVM 언어로 작성할 수 있으며, 데이터베이스 외부에 위치합니다.

트리거 코드는 Cassandra 설치 디렉터리의 `lib/triggers` 하위 디렉터리에 배치합니다. 클러스터(cluster) 시작 시점에 로드되며, 클러스터에 참여하는 모든 노드(node)에 존재해야 합니다.

테이블에 정의된 트리거는 요청된 DML 문이 실행되기 전(before)에 실행됩니다. 이는 트랜잭션(transaction)의 원자성(atomicity)을 보장합니다.

---

### CREATE TRIGGER

트리거 생성 구문은 다음과 같습니다:

```
create_trigger_statement ::= CREATE TRIGGER [ IF NOT EXISTS ] trigger_name
	ON table_name
	USING string
```

예시:

```
CREATE TRIGGER myTrigger ON myTable USING 'org.apache.cassandra.triggers.InvertedIndex';
```

---

### DROP TRIGGER

트리거 삭제 구문은 다음과 같습니다:

```
drop_trigger_statement ::= DROP TRIGGER [ IF EXISTS ] trigger_name
	ON table_name
```

예시:

```
DROP TRIGGER myTrigger ON myTable;
```

---

## 보안(Security)

### 데이터베이스 역할(Database Roles)

CQL은 데이터베이스 역할(database role)을 사용하여 개별 사용자(user)와 사용자 그룹(group)을 모두 표현합니다. 역할 이름의 구문은 다음과 같이 정의됩니다:

```
role_name ::= identifier | string
```

---

#### CREATE ROLE

`CREATE ROLE` 문은 새로운 역할을 생성합니다:

```
create_role_statement ::= CREATE ROLE [ IF NOT EXISTS ] role_name
                          [ WITH role_options ]
role_options ::= role_option ( AND role_option )*
role_option ::= PASSWORD '=' string
                | LOGIN '=' boolean
                | SUPERUSER '=' boolean
                | OPTIONS '=' map_literal
                | ACCESS TO DATACENTERS set_literal
                | ACCESS TO ALL DATACENTERS
```

예시:

```
CREATE ROLE new_role;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true;
CREATE ROLE bob WITH PASSWORD = 'password_b' AND LOGIN = true AND SUPERUSER = true;
CREATE ROLE carlos WITH OPTIONS = { 'custom_option1' : 'option1_value', 'custom_option2' : 99 };
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND ACCESS TO DATACENTERS {'DC1', 'DC3'};
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND ACCESS TO ALL DATACENTERS;
```

기본적으로 역할은 `LOGIN` 권한이나 `SUPERUSER` 상태를 가지지 않습니다.

데이터베이스 리소스(resource)에 대한 권한(permission)은 역할에 부여됩니다. 리소스 타입에는 키스페이스(keyspace), 테이블(table), 함수(function), 그리고 역할 자체가 포함됩니다. 역할은 다른 역할에 할당되어 계층적(hierarchical) 권한 구조를 형성할 수 있습니다. 이 계층 구조에서 권한과 `SUPERUSER` 상태는 상속(inherit)되지만, `LOGIN` 권한은 상속되지 않습니다.

역할이 `LOGIN` 권한을 가지면, 클라이언트는 접속 시 해당 역할로 인증(identify)될 수 있습니다. 접속이 유지되는 동안 클라이언트는 해당 역할에 할당된 모든 역할과 권한을 획득합니다.

데이터베이스 역할 리소스에 대한 `CREATE` 권한을 가진 클라이언트만 `CREATE ROLE` 요청을 실행할 수 있습니다(단, `SUPERUSER`인 경우는 예외). Cassandra의 역할 관리(role management)는 플러그형(pluggable)이며, 커스텀 구현체는 위에 나열된 옵션의 일부만 지원할 수 있습니다.

역할명에 영숫자(alphanumeric)가 아닌 문자가 포함되는 경우 따옴표로 묶어야 합니다.

##### 내부 인증을 위한 자격 증명 설정(Setting credentials for internal authentication)

내부 인증(internal authentication)을 위한 비밀번호를 설정하려면 `WITH PASSWORD` 절을 사용하고, 비밀번호를 작은따옴표로 감싸야 합니다.

내부 인증이 구성되지 않았거나 역할이 `LOGIN` 권한을 가지지 않는 경우 `WITH PASSWORD` 절은 필요하지 않습니다.

##### 특정 데이터센터로의 접속 제한(Restricting connections to specific datacenters)

`network_authorizer`가 구성된 경우, `ACCESS TO DATACENTERS` 절에 접근 가능한 데이터센터의 집합 리터럴(set literal)을 지정하여 로그인 역할(login role)을 특정 데이터센터로 제한할 수 있습니다. 데이터센터를 생략하면 묵시적으로 모든 데이터센터에 대한 접근이 허용됩니다. 명확성을 위해 `ACCESS TO ALL DATACENTERS` 절을 사용할 수 있으나, 기능적인 차이는 없습니다.

##### 조건부 역할 생성(Creating a role conditionally)

이미 존재하는 역할을 생성하려고 하면 유효하지 않은 쿼리(invalid query) 오류가 발생합니다. `IF NOT EXISTS` 옵션을 사용하면 역할이 이미 존재하는 경우 해당 문은 아무 동작도 하지 않습니다(no-op).

```
CREATE ROLE other_role;
CREATE ROLE IF NOT EXISTS other_role;
```

---

#### ALTER ROLE

`ALTER ROLE` 문은 역할 옵션을 수정(modify)합니다:

```
alter_role_statement ::= ALTER ROLE role_name WITH role_options
```

예시:

```
ALTER ROLE bob WITH PASSWORD = 'PASSWORD_B' AND SUPERUSER = false;
```

##### 특정 데이터센터로의 접속 제한(Restricting connections to specific datacenters)

`network_authorizer`가 구성된 경우, `ACCESS TO DATACENTERS` 절에 접근 가능한 데이터센터의 집합 리터럴을 지정하여 로그인 역할을 특정 데이터센터로 제한할 수 있습니다. 데이터센터 제한을 해제하려면 `ACCESS TO ALL DATACENTERS` 절을 사용합니다.

`ALTER ROLE` 문 실행 조건은 다음과 같습니다:

- 다른 역할의 `SUPERUSER` 상태를 변경하려면 클라이언트가 `SUPERUSER` 상태여야 합니다.
- 클라이언트는 자신이 현재 보유한 역할의 `SUPERUSER` 상태를 변경할 수 없습니다.
- 클라이언트는 로그인 시 사용한 역할의 일부 속성(예: `PASSWORD`)만 수정할 수 있습니다.
- 역할 속성을 수정하려면 해당 역할에 대한 `ALTER` 권한이 있어야 합니다.

---

#### DROP ROLE

`DROP ROLE` 문은 역할을 제거(remove)합니다:

```
drop_role_statement ::= DROP ROLE [ IF EXISTS ] role_name
```

`DROP ROLE`을 실행하려면 해당 역할에 대한 `DROP` 권한이 있어야 합니다. 클라이언트는 로그인 시 사용한 자신의 역할을 `DROP`할 수 없으며, `SUPERUSER` 상태인 클라이언트만 다른 `SUPERUSER` 역할을 `DROP`할 수 있습니다.

존재하지 않는 역할을 삭제하려고 하면 유효하지 않은 쿼리 오류가 발생합니다. `IF EXISTS` 옵션을 사용하면 역할이 존재하지 않는 경우 해당 문은 아무 동작도 하지 않습니다(no-op).

**참고(Note):** `DROP ROLE`은 의도적으로 열려 있는 사용자 세션을 종료하지 않습니다. 현재 연결된 세션은 그대로 유지되며, 인가(authorization)가 필요하지 않은 데이터베이스 작업은 계속 수행할 수 있습니다. 인가가 활성화되어 있으면 삭제된 역할의 권한도 함께 철회되며, 이는 `cassandra.yaml`에 구성된 캐싱(caching) 옵션의 영향을 받습니다. 삭제된 역할이 새로운 권한이나 역할을 부여받아 다시 생성되면, 여전히 연결되어 있는 클라이언트 세션은 새로 부여된 권한과 역할을 획득하게 됩니다.

---

#### GRANT ROLE

`GRANT ROLE` 문은 한 역할을 다른 역할에 할당합니다:

```
grant_role_statement ::= GRANT role_name TO role_name
```

예시:

```
GRANT report_writer TO alice;
```

이 문은 `report_writer` 역할을 `alice`에게 부여합니다. `report_writer`에 부여된 모든 권한은 `alice`도 획득합니다.

역할은 방향성 비순환 그래프(directed acyclic graph, DAG)로 모델링되므로 순환 부여(circular grant)는 허용되지 않습니다. 다음 예시들은 오류를 발생시킵니다:

```
GRANT role_a TO role_b;
GRANT role_b TO role_a;

GRANT role_a TO role_b;
GRANT role_b TO role_c;
GRANT role_c TO role_a;
```

---

#### REVOKE ROLE

`REVOKE ROLE` 문은 역할 할당을 해제합니다:

```
revoke_role_statement ::= REVOKE role_name FROM role_name
```

예시:

```
REVOKE report_writer FROM alice;
```

이 문은 `alice`에게서 `report_writer` 역할을 철회합니다. `alice`가 `report_writer` 역할을 통해 획득했던 모든 권한도 함께 철회됩니다.

---

#### LIST ROLES

시스템에 알려진 모든 역할(또는 특정 역할에 부여된 역할)은 `LIST ROLES` 문으로 조회할 수 있습니다:

```
list_roles_statement ::= LIST ROLES [ OF role_name ] [ NORECURSIVE ]
```

예시:

```
LIST ROLES;
```

시스템에 알려진 모든 역할을 반환합니다. 이를 위해서는 데이터베이스 역할 리소스에 대한 `DESCRIBE` 권한이 필요합니다.

다음 예시는 `alice`에게 부여된 모든 역할을 반환하며, 전이적으로(transitively) 획득한 역할도 포함합니다:

```
LIST ROLES OF alice;
```

다음 예시는 `bob`에게 직접(directly) 부여된 역할만 반환하며, 전이적으로 획득한 역할은 포함하지 않습니다:

```
LIST ROLES OF bob NORECURSIVE;
```

---

### 사용자(Users)

Cassandra 2.2에서 역할(role) 개념이 도입되기 전에는 인증(authentication)과 인가(authorization)가 `USER`라는 개념에 기반하고 있었습니다. 하위 호환성(backward compatibility)을 위해 레거시(legacy) 구문이 보존되었으며, `USER` 중심 문들은 `ROLE` 기반의 동등한 문(equivalent)에 대한 동의어(synonym)가 되었습니다. 즉, 사용자를 생성/수정하는 것은 역할을 생성/수정하는 또 다른 구문입니다.

---

#### CREATE USER

`CREATE USER` 문은 새로운 사용자를 생성합니다:

```
create_user_statement ::= CREATE USER [ IF NOT EXISTS ] role_name
                          [ WITH PASSWORD string ]
                          [ user_option ]
user_option: SUPERUSER | NOSUPERUSER
```

예시:

```
CREATE USER alice WITH PASSWORD 'password_a' SUPERUSER;
CREATE USER bob WITH PASSWORD 'password_b' NOSUPERUSER;
```

`CREATE USER`는 `LOGIN = true`인 `CREATE ROLE`과 동등합니다. 다음 문 쌍들은 서로 등가입니다:

```
CREATE USER alice WITH PASSWORD 'password_a' SUPERUSER;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND SUPERUSER = true;

CREATE USER IF NOT EXISTS alice WITH PASSWORD 'password_a' SUPERUSER;
CREATE ROLE IF NOT EXISTS alice WITH PASSWORD = 'password_a' AND LOGIN = true AND SUPERUSER = true;

CREATE USER alice WITH PASSWORD 'password_a' NOSUPERUSER;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true AND SUPERUSER = false;

CREATE USER alice WITH PASSWORD 'password_a' NOSUPERUSER;
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true;

CREATE USER alice WITH PASSWORD 'password_a';
CREATE ROLE alice WITH PASSWORD = 'password_a' AND LOGIN = true;
```

---

#### ALTER USER

`ALTER USER` 문은 사용자 옵션을 수정합니다:

```
alter_user_statement ::= ALTER USER role_name [ WITH PASSWORD string ] [ user_option ]
```

예시:

```
ALTER USER alice WITH PASSWORD 'PASSWORD_A';
ALTER USER bob SUPERUSER;
```

---

#### DROP USER

`DROP USER` 문은 사용자를 제거합니다:

```
drop_user_statement ::= DROP USER [ IF EXISTS ] role_name
```

---

#### LIST USERS

기존 사용자는 `LIST USERS` 문으로 열거할 수 있습니다:

```
list_users_statement ::= LIST USERS
```

이 문은 `LIST ROLES`와 동등하지만, 출력에는 `LOGIN` 권한을 가진 역할만 포함됩니다.

---

### 데이터 제어(Data Control)

#### 권한(Permissions)

리소스에 대한 권한은 역할에 부여됩니다. Cassandra에는 여러 종류의 리소스 타입이 있으며, 각각 계층적(hierarchical)으로 모델링됩니다:

- 데이터 리소스(Data resource), 즉 키스페이스(Keyspace)와 테이블(Table)의 계층은 `ALL KEYSPACES` → `KEYSPACE` → `TABLE` 구조입니다.
- 함수 리소스(Function resource)는 `ALL FUNCTIONS` → `KEYSPACE` → `FUNCTION` 구조입니다.
- 역할을 표현하는 리소스는 `ALL ROLES` → `ROLE` 구조입니다.
- JMX ObjectName을 표현하는 리소스(MBean/MXBean 집합에 매핑됨)는 `ALL MBEANS` → `MBEAN` 구조입니다.

권한은 이 계층 구조의 어느 레벨에서든 부여될 수 있으며, 하위로 전파됩니다(flow downwards). 상위 리소스에 권한을 부여하면 하위의 모든 리소스에 동일한 권한이 자동으로 적용됩니다. 예를 들어 `KEYSPACE`에 `SELECT`를 부여하면 해당 키스페이스 내 모든 `TABLE`에 자동으로 부여됩니다. 마찬가지로 `ALL FUNCTIONS`에 권한을 부여하면 키스페이스 범위에 관계없이 정의된 모든 함수에 부여됩니다. 특정 키스페이스에 속한 모든 함수에만 권한을 부여하는 것도 가능합니다.

권한 변경 사항은 기존 클라이언트 세션에도 즉시 반영됩니다. 재접속이 필요하지 않습니다.

사용 가능한 권한의 전체 집합은 다음과 같습니다:

- `CREATE`
- `ALTER`
- `DROP`
- `SELECT`
- `MODIFY`
- `AUTHORIZE`
- `DESCRIBE`
- `EXECUTE`

모든 권한이 모든 리소스 타입에 적용되는 것은 아닙니다. 예를 들어 `EXECUTE`는 함수나 mbean 맥락에서만 의미가 있습니다. 적용할 수 없는 리소스에 권한을 `GRANT`하려고 하면 오류 응답(error response)이 반환됩니다.

##### 권한 테이블(Permissions Table)

| 권한(Permission) | 리소스(Resource) | 허용되는 작업(Operations) |
|---|---|---|
| `CREATE` | `ALL KEYSPACES` | 임의의 키스페이스에서 `CREATE KEYSPACE` 및 `CREATE TABLE` |
| `CREATE` | `KEYSPACE` | 지정된 키스페이스에서 `CREATE TABLE` |
| `CREATE` | `ALL FUNCTIONS` | 임의의 키스페이스에서 `CREATE FUNCTION` 및 `CREATE AGGREGATE` |
| `CREATE` | `ALL FUNCTIONS IN KEYSPACE` | 지정된 키스페이스에서 `CREATE FUNCTION` 및 `CREATE AGGREGATE` |
| `CREATE` | `ALL ROLES` | `CREATE ROLE` |
| `ALTER` | `ALL KEYSPACES` | 임의의 키스페이스에서 `ALTER KEYSPACE` 및 `ALTER TABLE` |
| `ALTER` | `KEYSPACE` | 지정된 키스페이스에서 `ALTER KEYSPACE` 및 `ALTER TABLE` |
| `ALTER` | `TABLE` | `ALTER TABLE` |
| `ALTER` | `ALL FUNCTIONS` | `CREATE FUNCTION` 및 `CREATE AGGREGATE`: 기존 함수 교체(replacing) |
| `ALTER` | `ALL FUNCTIONS IN KEYSPACE` | `CREATE FUNCTION` 및 `CREATE AGGREGATE`: 지정된 키스페이스 내 기존 함수 교체 |
| `ALTER` | `FUNCTION` | `CREATE FUNCTION` 및 `CREATE AGGREGATE`: 기존 함수 교체 |
| `ALTER` | `ALL ROLES` | 임의의 역할에 대한 `ALTER ROLE` |
| `ALTER` | `ROLE` | `ALTER ROLE` |
| `DROP` | `ALL KEYSPACES` | 임의의 키스페이스에서 `DROP KEYSPACE` 및 `DROP TABLE` |
| `DROP` | `KEYSPACE` | 지정된 키스페이스에서 `DROP TABLE` |
| `DROP` | `TABLE` | `DROP TABLE` |
| `DROP` | `ALL FUNCTIONS` | 임의의 키스페이스에서 `DROP FUNCTION` 및 `DROP AGGREGATE` |
| `DROP` | `ALL FUNCTIONS IN KEYSPACE` | 지정된 키스페이스에서 `DROP FUNCTION` 및 `DROP AGGREGATE` |
| `DROP` | `FUNCTION` | `DROP FUNCTION` |
| `DROP` | `ALL ROLES` | 임의의 역할에 대한 `DROP ROLE` |
| `DROP` | `ROLE` | `DROP ROLE` |
| `SELECT` | `ALL KEYSPACES` | 임의의 테이블에 대한 `SELECT` |
| `SELECT` | `KEYSPACE` | 지정된 키스페이스 내 임의의 테이블에 대한 `SELECT` |
| `SELECT` | `TABLE` | 지정된 테이블에 대한 `SELECT` |
| `SELECT` | `ALL MBEANS` | 임의의 mbean에 대해 getter 메서드 호출 |
| `SELECT` | `MBEANS` | 와일드카드 패턴(wildcard pattern)에 일치하는 임의의 mbean에 대해 getter 메서드 호출 |
| `SELECT` | `MBEAN` | 지정된 mbean에 대해 getter 메서드 호출 |
| `MODIFY` | `ALL KEYSPACES` | 임의의 테이블에 대한 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE` |
| `MODIFY` | `KEYSPACE` | 지정된 키스페이스 내 임의의 테이블에 대한 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE` |
| `MODIFY` | `TABLE` | 지정된 테이블에 대한 `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE` |
| `MODIFY` | `ALL MBEANS` | 임의의 mbean에 대해 setter 메서드 호출 |
| `MODIFY` | `MBEANS` | 와일드카드 패턴에 일치하는 임의의 mbean에 대해 setter 메서드 호출 |
| `MODIFY` | `MBEAN` | 지정된 mbean에 대해 setter 메서드 호출 |
| `AUTHORIZE` | `ALL KEYSPACES` | 임의의 테이블에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `KEYSPACE` | 지정된 키스페이스 내 임의의 테이블에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `TABLE` | 지정된 테이블에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `ALL FUNCTIONS` | 임의의 함수에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `ALL FUNCTIONS IN KEYSPACE` | 지정된 키스페이스에서의 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `FUNCTION` | 지정된 함수에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `ALL MBEANS` | 임의의 mbean에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `MBEANS` | 와일드카드 패턴에 일치하는 임의의 mbean에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `MBEAN` | 지정된 mbean에 대한 `GRANT PERMISSION` 및 `REVOKE PERMISSION` |
| `AUTHORIZE` | `ALL ROLES` | 임의의 역할에 대한 `GRANT ROLE` 및 `REVOKE ROLE` |
| `AUTHORIZE` | `ROLES` | 지정된 역할에 대한 `GRANT ROLE` 및 `REVOKE ROLE` |
| `DESCRIBE` | `ALL ROLES` | 모든 역할 또는 다른 지정된 역할에 부여된 역할에 대한 `LIST ROLES` |
| `DESCRIBE` | `ALL MBEANS` | 플랫폼의 MBeanServer로부터 임의의 mbean에 대한 메타데이터 조회 |
| `DESCRIBE` | `MBEANS` | 플랫폼의 MBeanServer로부터 와일드카드 패턴에 일치하는 임의의 mbean에 대한 메타데이터 조회 |
| `DESCRIBE` | `MBEAN` | 플랫폼의 MBeanServer로부터 지정된 mbean에 대한 메타데이터 조회 |
| `EXECUTE` | `ALL FUNCTIONS` | 임의의 함수를 사용한 `SELECT`, `INSERT`, `UPDATE`, 그리고 `CREATE AGGREGATE`에서 임의의 함수 사용 |
| `EXECUTE` | `ALL FUNCTIONS IN KEYSPACE` | 지정된 키스페이스 내 임의의 함수를 사용한 `SELECT`, `INSERT`, `UPDATE`, 그리고 `CREATE AGGREGATE`에서 해당 키스페이스 내 임의의 함수 사용 |
| `EXECUTE` | `FUNCTION` | 지정된 함수를 사용한 `SELECT`, `INSERT`, `UPDATE`, 그리고 `CREATE AGGREGATE`에서 해당 함수 사용 |
| `EXECUTE` | `ALL MBEANS` | 임의의 mbean에 대해 작업(operation) 실행 |
| `EXECUTE` | `MBEANS` | 와일드카드 패턴에 일치하는 임의의 mbean에 대해 작업 실행 |
| `EXECUTE` | `MBEAN` | 지정된 mbean에 대해 작업 실행 |

---

#### GRANT PERMISSION

`GRANT PERMISSION` 문은 권한을 할당합니다:

```
grant_permission_statement ::= GRANT permissions ON resource TO role_name
permissions ::= ALL [ PERMISSIONS ] | permission [ PERMISSION ]
permission ::= CREATE | ALTER | DROP | SELECT | MODIFY | AUTHORIZE | DESCRIBE | EXECUTE
resource ::=    ALL KEYSPACES
                | KEYSPACE keyspace_name
                | [ TABLE ] table_name
                | ALL ROLES
                | ROLE role_name
                | ALL FUNCTIONS [ IN KEYSPACE keyspace_name ]
                | FUNCTION function_name '(' [ cql_type ( ',' cql_type )* ] ')'
                | ALL MBEANS
                | ( MBEAN | MBEANS ) string
```

예시:

```
GRANT SELECT ON ALL KEYSPACES TO data_reader;
```

이 예시는 `data_reader` 역할을 가진 사용자에게 모든 키스페이스의 모든 테이블에 대해 `SELECT` 문을 실행할 수 있는 권한을 부여합니다:

```
GRANT MODIFY ON KEYSPACE keyspace1 TO data_writer;
```

이는 `data_writer` 역할을 가진 사용자에게 `keyspace1` 키스페이스의 모든 테이블에 대해 `UPDATE`, `INSERT`, `DELETE`, `TRUNCATE` 쿼리를 실행할 수 있는 권한을 부여합니다:

```
GRANT DROP ON keyspace1.table1 TO schema_owner;
```

이는 `schema_owner` 역할을 가진 사용자에게 `keyspace1.table1` 테이블을 `DROP`할 수 있는 권한을 부여합니다:

```
GRANT EXECUTE ON FUNCTION keyspace1.user_function( int ) TO report_writer;
```

이 명령은 `report_writer` 역할을 가진 사용자에게 `keyspace1.user_function( int )` 함수를 사용하는 `SELECT`, `INSERT`, `UPDATE` 쿼리를 실행할 수 있는 권한을 부여합니다:

```
GRANT DESCRIBE ON ALL ROLES TO role_admin;
```

이는 `role_admin` 역할을 가진 사용자에게 `LIST ROLES` 문으로 시스템의 모든 역할을 조회할 수 있는 권한을 부여합니다.

##### GRANT ALL

`GRANT ALL` 형식을 사용하면 대상 리소스에 따라 적절한 권한 집합이 자동으로 결정됩니다.

##### 자동 부여(Automatic Granting)

`CREATE KEYSPACE`, `CREATE TABLE`, `CREATE FUNCTION`, `CREATE AGGREGATE`, `CREATE ROLE` 문으로 리소스가 생성되면, 해당 문을 실행한 데이터베이스 사용자(식별된 역할)는 새 리소스에 대해 적용 가능한 모든 권한을 자동으로 부여받습니다.

---

#### REVOKE PERMISSION

`REVOKE PERMISSION` 문은 역할로부터 권한을 제거합니다:

```
revoke_permission_statement ::= REVOKE permissions ON resource FROM role_name
```

예시:

```
REVOKE SELECT ON ALL KEYSPACES FROM data_reader;
REVOKE MODIFY ON KEYSPACE keyspace1 FROM data_writer;
REVOKE DROP ON keyspace1.table1 FROM schema_owner;
REVOKE EXECUTE ON FUNCTION keyspace1.user_function( int ) FROM report_writer;
REVOKE DESCRIBE ON ALL ROLES FROM role_admin;
```

드라이버(driver)의 정상 동작을 위해 일부 테이블은 `SELECT` 권한을 철회할 수 없습니다. 다음 테이블들은 할당된 역할과 무관하게 모든 인가된 사용자에게 항상 접근 가능합니다:

```
* system_schema.keyspaces
* system_schema.columns
* system_schema.tables
* system.local
* system.peers
```

---

#### LIST PERMISSIONS

`LIST PERMISSIONS` 문은 부여된 권한을 표시합니다:

```
list_permissions_statement ::= LIST permissions [ ON resource ] [ OF role_name [ NORECURSIVE ] ]
```

예시:

```
LIST ALL PERMISSIONS OF alice;
```

`alice`에게 부여된 모든 권한을 표시하며, 다른 역할을 통해 전이적으로 획득한 권한도 포함합니다:

```
LIST ALL PERMISSIONS ON keyspace1.table1 OF bob;
```

`keyspace1.table1`에 대해 `bob`에게 부여된 모든 권한을 표시하며, 다른 역할을 통해 전이적으로 획득한 권한도 포함합니다. 또한 `keyspace1.table1`에 적용될 수 있는 리소스 계층 상위의 권한도 포함됩니다. 예를 들어 `bob`이 `keyspace1`에 대한 `ALTER` 권한을 가지고 있다면 이 권한도 쿼리 결과에 포함됩니다. `NORECURSIVE` 스위치를 추가하면 결과가 `bob` 또는 `bob`이 보유한 역할에 직접 부여된 권한으로만 제한됩니다:

```
LIST SELECT PERMISSIONS OF carlos;
```

`carlos` 또는 `carlos`가 보유한 역할에 부여된 권한 중 임의의 리소스에 대한 `SELECT` 권한만 표시합니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [CQL JSON](https://cassandra.apache.org/doc/latest/cassandra/cql/json.html)
- [CQL Triggers](https://cassandra.apache.org/doc/latest/cassandra/cql/triggers.html)
- [CQL Security](https://cassandra.apache.org/doc/latest/cassandra/cql/security.html)
