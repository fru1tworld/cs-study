# Chapter 21. 데이터베이스 역할 (Database Roles)

PostgreSQL은 역할(roles) 개념을 사용하여 데이터베이스 접근 권한을 관리합니다. 역할은 설정 방식에 따라 데이터베이스 사용자(database user) 또는 데이터베이스 사용자 그룹(group of database users) 으로 간주될 수 있습니다.

역할은 데이터베이스 객체(예: 테이블, 함수)를 소유할 수 있으며, 다른 역할에게 해당 객체에 대한 권한을 할당하여 누가 어떤 객체에 접근할 수 있는지를 제어할 수 있습니다. 또한 역할의 멤버십 을 다른 역할에게 부여할 수 있어, 멤버 역할이 다른 역할에 할당된 권한을 사용할 수 있게 됩니다.

역할의 개념은 "사용자(users)"와 "그룹(groups)"의 개념을 포괄합니다. PostgreSQL 8.1 이전 버전에서는 사용자와 그룹이 별개의 개체 유형이었지만, 현재는 역할만 존재합니다. 모든 역할은 사용자, 그룹, 또는 둘 다로 작동할 수 있습니다.

이 장에서는 역할을 생성하고 관리하는 방법을 설명합니다. 다양한 데이터베이스 객체에 대한 역할 권한의 효과에 대한 자세한 정보는 Section 5.8에서 확인할 수 있습니다.

---

## 목차

- [21.1. 데이터베이스 역할](#211-데이터베이스-역할-database-roles)
- [21.2. 역할 속성](#212-역할-속성-role-attributes)
- [21.3. 역할 멤버십](#213-역할-멤버십-role-membership)
- [21.4. 역할 삭제](#214-역할-삭제-dropping-roles)
- [21.5. 사전 정의된 역할](#215-사전-정의된-역할-predefined-roles)
- [21.6. 함수 보안](#216-함수-보안-function-security)

---

## 21.1. 데이터베이스 역할 (Database Roles)

데이터베이스 역할은 운영 체제 사용자와 개념적으로 완전히 분리되어 있습니다. 실제로는 대응 관계를 유지하는 것이 편리할 수 있지만, 필수 사항은 아닙니다. 데이터베이스 역할은 데이터베이스 클러스터 설치 전체에서 전역적으로 적용됩니다 (개별 데이터베이스별로 적용되는 것이 아닙니다).

### 역할 생성

역할을 생성하려면 `CREATE ROLE` SQL 명령을 사용합니다:

```sql
CREATE ROLE name;
```

`name`은 SQL 식별자 규칙을 따릅니다: 특수 문자가 없는 순수한 이름이거나 큰따옴표로 둘러싸인 이름이어야 합니다. (실제로는 보통 `LOGIN`과 같은 추가 옵션을 명령에 추가합니다. 자세한 내용은 아래에서 설명합니다.)

### 역할 삭제

기존 역할을 제거하려면 유사한 `DROP ROLE` 명령을 사용합니다:

```sql
DROP ROLE name;
```

### 셸 명령어 래퍼

편의를 위해 `createuser`와 `dropuser` 프로그램이 제공됩니다. 이 프로그램들은 셸 명령줄에서 호출할 수 있는 SQL 명령의 래퍼입니다:

```bash
createuser name
dropuser name
```

### 기존 역할 확인

기존 역할 집합을 확인하려면 `pg_roles` 시스템 카탈로그를 조회합니다:

```sql
SELECT rolname FROM pg_roles;
```

로그인 가능한 역할만 확인하려면:

```sql
SELECT rolname FROM pg_roles WHERE rolcanlogin;
```

psql 프로그램의 `\du` 메타 명령도 기존 역할을 나열하는 데 유용합니다:

```
\du
```

### 초기 슈퍼유저 역할

새로 초기화된 시스템에는 항상 하나의 사전 정의된 로그인 가능 역할이 포함되어 있습니다. 이 역할은 항상 "슈퍼유저"이며, 기본적으로 (특별히 지정하지 않는 한) 데이터베이스 클러스터를 초기화하기 위해 `initdb`를 실행한 운영 체제 사용자와 동일한 이름을 갖습니다. 일반적으로 이 역할의 이름은 `postgres`입니다. 더 많은 역할을 생성하려면 먼저 이 초기 역할로 연결해야 합니다.

### 데이터베이스 연결

데이터베이스 서버에 대한 모든 연결은 특정 역할의 이름으로 이루어지며, 이 역할이 해당 연결로 시작된 세션의 초기 접근 권한을 결정합니다. 특정 데이터베이스 연결에 사용할 역할 이름은 연결 요청을 시작하는 클라이언트가 애플리케이션별 방식으로 나타냅니다. 예를 들어, `psql` 프로그램은 `-U` 명령줄 옵션을 사용하여 연결할 역할을 나타냅니다.

```bash
psql -U rolename
```

많은 애플리케이션은 현재 운영 체제 사용자의 이름을 기본값으로 가정합니다 (`createuser`와 `psql` 포함). 따라서 역할과 운영 체제 사용자 간에 명명 대응을 유지하는 것이 편리한 경우가 많습니다.

### 보안 고려사항

주어진 클라이언트 연결이 연결할 수 있는 역할 집합은 Chapter 20에서 설명하는 클라이언트 인증 설정에 의해 결정됩니다. (따라서 클라이언트는 반드시 운영 체제 사용자 이름과 일치하는 역할로 연결할 필요가 없습니다. 마치 사람의 로그인 이름이 반드시 실제 이름과 일치할 필요가 없는 것과 같습니다.) 역할 신원이 연결된 클라이언트에 사용 가능한 데이터베이스 권한 집합을 결정하므로, 다중 사용자 환경에서는 권한을 신중하게 구성하는 것이 중요합니다.

---

## 21.2. 역할 속성 (Role Attributes)

데이터베이스 역할은 권한을 정의하고 클라이언트 인증 시스템과 상호 작용하는 여러 속성을 가질 수 있습니다.

### 1. 로그인 권한 (Login Privilege)

`LOGIN` 속성을 가진 역할만 데이터베이스 연결의 초기 역할 이름으로 사용할 수 있습니다. `LOGIN` 속성을 가진 역할은 "데이터베이스 사용자"와 동일한 것으로 간주될 수 있습니다. 로그인 권한이 있는 역할을 생성하려면 다음 중 하나를 사용합니다:

```sql
CREATE ROLE name LOGIN;
CREATE USER name;
```

(`CREATE USER`는 기본적으로 `LOGIN`을 부여한다는 점을 제외하고 `CREATE ROLE`과 동일합니다. 반면 `CREATE ROLE`은 기본적으로 부여하지 않습니다.)

### 2. 슈퍼유저 상태 (Superuser Status)

데이터베이스 슈퍼유저는 로그인 권한을 제외한 모든 권한 검사를 우회합니다. 이것은 위험한 권한이므로 부주의하게 사용해서는 안 됩니다; 대부분의 작업은 비슈퍼유저로 수행하는 것이 가장 좋습니다. 새 데이터베이스 슈퍼유저를 생성하려면 `CREATE ROLE name SUPERUSER`를 사용합니다. 이미 슈퍼유저인 역할로 수행해야 합니다.

```sql
CREATE ROLE name SUPERUSER;
```

### 3. 데이터베이스 생성 (Database Creation)

역할이 데이터베이스를 생성하려면 명시적인 권한이 부여되어야 합니다 (슈퍼유저는 어차피 모든 것을 우회하므로 제외). 그러한 역할을 생성하려면 `CREATE ROLE name CREATEDB`를 사용합니다.

```sql
CREATE ROLE name CREATEDB;
```

### 4. 역할 생성 (Role Creation)

역할이 더 많은 역할을 생성하려면 명시적인 권한이 부여되어야 합니다 (슈퍼유저는 어차피 모든 것을 우회하므로 제외). 그러한 역할을 생성하려면 `CREATE ROLE name CREATEROLE`을 사용합니다.

```sql
CREATE ROLE name CREATEROLE;
```

`CREATEROLE` 권한의 제약 사항:

`CREATEROLE` 권한을 가진 역할은 다른 역할에 대해 `ALTER ROLE`도 수행할 수 있고, 어떤 역할에도 `COMMENT`와 `SECURITY LABEL`을 사용할 수 있습니다; 그러나 이러한 방식으로 슈퍼유저 역할을 변경하는 것은 불가능합니다.

또한 `CREATEROLE`은 슈퍼유저가 아닌 역할에 대해 `ALTER ROLE ... SET`과 `ALTER ROLE ... RENAME`을 허용합니다. 이 명령은 슈퍼유저에 대해서도 수행할 수 있지만, `REPLICATION` 역할에 대해서도 가능합니다.

그러나 `CREATEROLE`은 `REPLICATION` 역할을 생성하거나, `REPLICATION` 또는 `BYPASSRLS` 권한을 부여하거나 취소할 수 있는 능력을 부여하지 않습니다.

### 5. 복제 시작 (Initiating Replication)

역할이 스트리밍 복제를 시작하려면 명시적인 권한이 부여되어야 합니다 (슈퍼유저는 어차피 모든 것을 우회하므로 제외). 스트리밍 복제에 사용되는 역할은 `LOGIN` 권한도 가져야 합니다. 그러한 역할을 생성하려면 `CREATE ROLE name REPLICATION LOGIN`을 사용합니다.

```sql
CREATE ROLE name REPLICATION LOGIN;
```

### 6. 비밀번호 (Password)

비밀번호는 클라이언트 인증 방법이 데이터베이스에 연결할 때 사용자가 비밀번호를 입력하도록 요구하는 경우에만 중요합니다. `password`와 `md5` 인증 방법이 비밀번호를 사용합니다. 데이터베이스 비밀번호는 운영 체제 비밀번호와 별개입니다. 역할 생성 시 비밀번호를 지정합니다:

```sql
CREATE ROLE name PASSWORD 'string';
```

### 7. 권한 상속 (Inheritance of Privileges)

역할은 기본적으로 멤버인 역할의 권한을 상속받습니다. 그러나 상속 없이 역할을 생성하려면 `CREATE ROLE name NOINHERIT`를 사용합니다.

```sql
CREATE ROLE name NOINHERIT;
```

또는 역할 멤버십 그래프에서 특정 지점에서 상속을 활성화하거나 비활성화하려면 `WITH INHERIT TRUE` 또는 `WITH INHERIT FALSE`를 사용합니다.

```sql
WITH INHERIT TRUE
WITH INHERIT FALSE
```

### 8. 행 수준 보안 우회 (Bypassing Row-Level Security)

역할이 모든 행 수준 보안(RLS) 정책을 우회하려면 명시적인 권한이 부여되어야 합니다 (슈퍼유저는 어차피 모든 것을 우회하므로 제외). 그러한 역할을 생성하려면 `CREATE ROLE name BYPASSRLS`를 사용합니다. 슈퍼유저로 수행해야 합니다.

```sql
CREATE ROLE name BYPASSRLS;
```

### 9. 연결 제한 (Connection Limit)

연결 제한은 역할이 만들 수 있는 동시 연결 수를 지정할 수 있습니다. -1 (기본값)은 제한 없음을 의미합니다. 역할 생성 시 연결 제한을 지정합니다:

```sql
CREATE ROLE name CONNECTION LIMIT 'integer';
```

### 역할 속성 수정

역할의 속성은 `ALTER ROLE`로 생성 후 수정할 수 있습니다. 자세한 내용은 `CREATE ROLE`과 `ALTER ROLE` 참조 페이지를 참조하세요.

역할은 Chapter 19에서 설명하는 많은 런타임 구성 설정에 대해 역할별 기본값을 가질 수도 있습니다. 예를 들어, 연결할 때마다 인덱스 스캔을 비활성화하려면 (어떤 이유로든) 다음을 사용할 수 있습니다:

```sql
ALTER ROLE myname SET enable_indexscan TO off;
```

이렇게 하면 설정이 저장됩니다 (즉시 설정되지는 않음). 후속 연결에서 이 역할이 세션을 시작할 때마다 `SET enable_indexscan TO off`가 실행된 것처럼 됩니다. 세션 중에 설정을 변경할 수도 있습니다; 이것은 기본값일 뿐입니다. 역할별 기본 설정을 제거하려면 `ALTER ROLE rolename RESET varname`을 사용합니다.

```sql
ALTER ROLE rolename RESET varname;
```

`LOGIN` 권한이 없는 역할에 연결된 역할별 기본값은 실제로 쓸모가 없습니다. 절대 호출되지 않기 때문입니다.

### CREATEROLE 사용자의 자동 권한 부여

`CREATEROLE` 권한을 가진 사용자가 새 역할을 생성하면, 자동으로 다음과 같이 해당 역할에 대한 권한이 부여됩니다:

```sql
GRANT created_user TO creating_user WITH ADMIN TRUE, SET FALSE, INHERIT FALSE;
```

생성하는 역할은 `ADMIN OPTION`을 통해 생성된 역할을 관리하고 다른 사용자에게 멤버십을 부여할 수 있습니다. 그러나 생성하는 역할은 기본적으로 생성된 역할의 권한을 상속하지 않으며 (`INHERIT FALSE`), `SET ROLE`을 통해 생성된 역할에 접근할 수 없습니다 (`SET FALSE`). 하지만 `ADMIN OPTION`이 있으므로 자신에게 역할을 재부여하여 원하는 대로 `INHERIT` 및/또는 `SET` 옵션을 변경할 수 있습니다.

---

## 21.3. 역할 멤버십 (Role Membership)

권한 관리를 편리하게 하기 위해 사용자를 그룹화하는 것이 종종 편리합니다. 이렇게 하면 권한을 전체 그룹에 부여하거나 취소할 수 있습니다. PostgreSQL에서는 권한 그룹을 나타내는 역할을 생성한 다음, 개별 사용자 역할에 해당 그룹 역할의 멤버십을 부여하여 이를 수행합니다.

### 그룹 역할 설정

그룹 역할을 설정하려면 먼저 역할을 생성합니다:

```sql
CREATE ROLE name;
```

일반적으로 그룹으로 사용되는 역할은 `LOGIN` 속성이 없지만, 원하면 설정할 수 있습니다.

### 멤버십 부여 및 취소

역할이 존재하면 `GRANT`와 `REVOKE` 명령을 사용하여 멤버를 추가하고 제거할 수 있습니다:

```sql
GRANT group_role TO role1, ... ;
REVOKE group_role FROM role1, ... ;
```

다른 그룹 역할에도 멤버십을 부여할 수 있습니다 (역할 멤버와 역할 그룹 간에 실제 구분이 없기 때문입니다). 데이터베이스는 순환 멤버십 루프 설정을 허용하지 않습니다. 또한 역할 멤버십을 `PUBLIC`에 부여하는 것은 허용되지 않습니다.

### 권한 사용 방식

그룹 역할의 멤버는 다음 두 가지 방식으로 역할의 권한을 사용할 수 있습니다:

#### 방식 1: SET ROLE (명시적)

그룹의 모든 멤버는 `SET` 옵션으로 부여된 멤버십인 경우 명시적으로 `SET ROLE`을 수행하여 일시적으로 그룹 역할이 "될" 수 있습니다. 이 상태에서 데이터베이스 세션은 원래 로그인 역할이 아닌 그룹 역할의 권한에 접근할 수 있으며, 생성된 모든 데이터베이스 객체는 로그인 역할이 아닌 그룹 역할이 소유한 것으로 간주됩니다.

```sql
SET ROLE group_role;
```

#### 방식 2: INHERIT (자동 상속)

`INHERIT` 옵션으로 부여된 멤버십을 가진 멤버 역할은 `SET ROLE`을 먼저 수행할 필요 없이 직접 또는 간접적으로 멤버인 역할의 권한을 자동으로 사용합니다. 예를 들어, 다음과 같이 설정했다고 가정합니다:

```sql
CREATE ROLE joe LOGIN;
CREATE ROLE admin;
CREATE ROLE wheel;
CREATE ROLE island;

GRANT admin TO joe WITH INHERIT TRUE;
GRANT wheel TO admin WITH INHERIT FALSE;
GRANT island TO joe WITH INHERIT TRUE, SET FALSE;
```

`joe`로 연결 직후 데이터베이스 세션은 `joe`에 직접 부여된 모든 권한과 `admin`에 부여된 모든 권한을 사용할 수 있습니다. 왜냐하면 `joe`가 `INHERIT` 옵션을 통해 `admin`을 "상속"하기 때문입니다. 그러나 `wheel`에 부여된 권한은 사용할 수 없습니다. `joe`는 `admin`을 통해 간접적으로 `wheel`의 멤버이지만, 해당 멤버십은 `INHERIT FALSE`로 부여되었기 때문입니다. `island`에 부여된 권한은 `joe`가 `INHERIT TRUE`로 해당 멤버십을 부여받았으므로 사용할 수 있습니다.

joe 역할의 권한 상황:

| 상황 | 접근 가능한 권한 |
|------|------------------|
| 초기 로그인 | joe + admin + island (INHERIT로 받음) |
| `SET ROLE admin;` 후 | admin만 가능 |
| `SET ROLE wheel;` 후 | wheel만 가능 |
| `SET ROLE island;` | 불가능 (SET FALSE로 설정) |

### SET ROLE의 동작

`SET ROLE admin;` 후에 세션은 `admin`에 직접 부여된 권한만 사용할 수 있으며, `joe`에 부여된 권한은 사용할 수 없습니다. `SET ROLE wheel;` 후에 세션은 `wheel`에 직접 부여된 권한만 사용할 수 있으며, `joe`나 `admin`에 부여된 권한은 사용할 수 없습니다. 원래 권한 상태는 다음 중 하나로 복원할 수 있습니다:

```sql
SET ROLE joe;
SET ROLE NONE;
RESET ROLE;
```

### SET ROLE 선택 규칙

`SET ROLE` 명령을 사용하면 로그인 역할이 직접 또는 간접적으로 멤버인 모든 역할을 선택할 수 있습니다. 단, 해당 역할에서 로그인 역할로 이어지는 멤버십 연쇄가 있고 각 멤버십이 `SET TRUE`(기본값)를 가져야 합니다. 따라서 위의 예에서 `joe`에서 `wheel`로 이동하기 전에 `admin`이 될 필요가 없습니다; 대신 `SET ROLE wheel`을 직접 사용할 수 있습니다. 마찬가지로 `admin`에 직접적인 멤버십은 없지만 `island`에서 `joe`로 이어지는 멤버십 연쇄에 `SET FALSE`가 포함되어 있으므로 `SET ROLE island`는 불가능합니다.

### 특수 속성의 비상속

`LOGIN`, `SUPERUSER`, `CREATEDB`, `CREATEROLE` 속성은 일반 권한으로 상속되지 않는 특수 속성으로 취급됩니다. 이러한 속성을 가진 역할로 실제로 `SET ROLE`을 해야 해당 속성을 사용할 수 있습니다. 계속되는 예에서 `admin` 역할에 `CREATEDB`와 `CREATEROLE`을 부여할 수도 있습니다. 그러면 `joe`로 연결하는 세션은 이러한 권한을 즉시 갖지 않고, `SET ROLE admin`을 수행한 후에만 갖게 됩니다.

### 그룹 역할 삭제

그룹 역할을 삭제하려면 `DROP ROLE`을 사용합니다:

```sql
DROP ROLE name;
```

그룹 역할에 대한 모든 멤버십은 자동으로 취소됩니다 (멤버 역할 자체는 영향받지 않음).

### SQL 표준과의 차이

과거 호환성을 위해 `CREATE ROLE`은 기본적으로 `INHERIT`를 설정하는 반면, 다른 형태의 `GRANT`에서는 기본적으로 `INHERIT FALSE`를 갖습니다. 그러나 SQL 표준에서는 기본값이 `INHERIT FALSE`입니다. SQL 표준과 일치하는 동작을 원한다면 개별 사용자 역할에 `NOINHERIT` 속성을 사용하고 그룹 역할에 `INHERIT` 속성을 사용하세요.

---

## 21.4. 역할 삭제 (Dropping Roles)

데이터베이스 역할은 데이터베이스 객체를 소유할 수 있고 다른 객체에 대한 권한을 가질 수 있기 때문에, 역할을 삭제하는 것은 종종 단순히 `DROP ROLE`을 수행하는 것만으로는 충분하지 않습니다. 역할이 소유한 모든 객체는 먼저 삭제되거나 다른 소유자에게 재할당되어야 하며, 역할에 부여된 모든 권한은 취소되어야 합니다.

### 객체 소유권 이전

객체의 소유권은 `ALTER` 명령을 사용하여 한 번에 하나씩 이전할 수 있습니다:

```sql
ALTER TABLE bobs_table OWNER TO alice;
```

### 일괄 소유권 이전 (REASSIGN OWNED)

또는 `REASSIGN OWNED` 명령을 사용하여 삭제할 역할이 소유한 모든 객체의 소유권을 단일 다른 역할로 재할당할 수 있습니다:

```sql
REASSIGN OWNED BY doomed_role TO successor_role;
```

주의사항:
- `REASSIGN OWNED`는 다른 데이터베이스의 객체에 접근할 수 없으므로, 역할이 여러 데이터베이스에 객체를 소유하고 있다면 클러스터의 각 데이터베이스에서 실행해야 합니다.
- `REASSIGN OWNED`는 공유 객체(데이터베이스, 테이블스페이스)의 소유권도 변경합니다.

### 남은 객체 삭제 (DROP OWNED)

`REASSIGN OWNED`가 문제 역할이 소유한 모든 객체를 처리하면, `DROP OWNED`를 사용하여 해당 역할이 소유한 나머지 객체를 삭제할 수 있습니다:

```sql
DROP OWNED BY doomed_role;
```

이 명령은 대상 역할에 부여된 모든 권한도 취소합니다. (이것은 이전에 `REASSIGN OWNED`로 이전된 객체에 대한 권한을 포함합니다: `REASSIGN OWNED`는 객체에 대해 이전 소유자가 가진 권한을 취소하지 않습니다.)

주의사항:
- `REASSIGN OWNED`와 마찬가지로 `DROP OWNED`는 다른 데이터베이스의 객체에 접근할 수 없습니다.
- `DROP OWNED`는 전체 데이터베이스나 테이블스페이스를 삭제하지 않으므로, 역할이 현재 데이터베이스에 없는 데이터베이스나 테이블스페이스를 소유하고 있다면 수동으로 처리해야 합니다.

### 역할 삭제

객체 소유권과 권한이 정리되면 역할을 삭제할 수 있습니다:

```sql
DROP ROLE doomed_role;
```

### 완전한 삭제 절차

```sql
REASSIGN OWNED BY doomed_role TO successor_role;
DROP OWNED BY doomed_role;
-- 클러스터의 각 데이터베이스에서 위 명령 반복 실행
DROP ROLE doomed_role;
```

중요: 순서 준수 필수 (REASSIGN OWNED -> DROP OWNED -> DROP ROLE)

### 오류 메시지

종속 객체가 남아있는 경우 `DROP ROLE`이 깨끗하게 삭제하지 못하면 어떤 객체를 재할당하거나 삭제해야 하는지 식별하는 메시지가 발행됩니다.

---

## 21.5. 사전 정의된 역할 (Predefined Roles)

PostgreSQL은 일반적으로 필요한 특권 기능과 정보에 대한 접근을 제공하는 사전 정의된 역할 집합을 제공합니다. 관리자(슈퍼유저 권한을 가진 역할 포함)는 환경의 사용자에게 이러한 역할을 `GRANT`하여 특정 기능에 대한 접근을 제공할 수 있습니다.

```sql
GRANT pg_signal_backend TO admin_user;
```

### 사전 정의된 역할 목록

| 역할 | 허용되는 접근 |
|------|---------------|
| `pg_checkpoint` | `CHECKPOINT` 명령 실행 허용 |
| `pg_create_subscription` | 데이터베이스에 대한 `CREATE` 권한이 있는 사용자가 `CREATE SUBSCRIPTION` 실행 허용 |
| `pg_database_owner` | 없음. 이 역할의 멤버십은 자동으로 현재 데이터베이스 소유자에게 부여됨 |
| `pg_execute_server_program` | 데이터베이스를 실행하는 사용자로 `COPY` 및 기타 서버 측 프로그램 실행 함수를 사용하여 서버의 프로그램 실행 허용 |
| `pg_maintain` | 모든 관계에서 `VACUUM`, `ANALYZE`, `CLUSTER`, `REFRESH MATERIALIZED VIEW`, `REINDEX`, `LOCK TABLE` 실행 허용 |
| `pg_monitor` | 다양한 모니터링 뷰와 함수를 읽고/실행합니다. 이 역할은 `pg_read_all_settings`, `pg_read_all_stats`, `pg_stat_scan_tables`의 멤버입니다 |
| `pg_read_all_data` | 모든 테이블, 뷰, 시퀀스에서 `SELECT` 권한이 있고 모든 스키마에서 `USAGE` 권한이 있는 것처럼 모든 데이터(테이블, 뷰, 시퀀스) 읽기 허용 |
| `pg_read_all_settings` | 슈퍼유저에게만 표시되도록 제한된 구성 변수를 포함한 모든 구성 변수 읽기 허용 |
| `pg_read_all_stats` | 슈퍼유저에게만 표시되도록 제한된 정보를 포함한 모든 `pg_stat_*` 뷰 읽기 허용 및 슈퍼유저만 볼 수 있는 통계 관련 확장 사용 허용 |
| `pg_read_server_files` | 데이터베이스가 서버에서 접근할 수 있는 모든 위치에서 `COPY` 및 기타 파일 접근 함수를 사용하여 파일 읽기 허용 |
| `pg_signal_autovacuum_worker` | autovacuum 워커에 신호 전송 허용 (현재 테이블에서 vacuum 취소 또는 종료) |
| `pg_signal_backend` | 다른 백엔드에 신호 전송 허용 (예: 쿼리 취소 또는 세션 종료) |
| `pg_stat_scan_tables` | `ACCESS SHARE` 잠금을 필요로 하는 모니터링 함수 실행 허용 (예: `pgrowlocks(text)`) |
| `pg_use_reserved_connections` | `reserved_connections`를 통해 예약된 연결 슬롯 사용 허용 |
| `pg_write_all_data` | 모든 테이블, 뷰, 시퀀스에서 `INSERT`, `UPDATE`, `DELETE` 권한이 있고 모든 스키마에서 `USAGE` 권한이 있는 것처럼 모든 데이터(테이블, 뷰, 시퀀스) 쓰기 허용 |
| `pg_write_server_files` | 데이터베이스가 서버에서 접근할 수 있는 모든 위치에서 `COPY` 및 기타 파일 접근 함수를 사용하여 파일 쓰기 허용 |

### 상세 설명

#### pg_database_owner

`pg_database_owner` 역할은 현재 데이터베이스 소유자를 암시적으로 "멤버"로 가지며, 따라서 해당 역할에 부여된 권한을 갖습니다. `pg_database_owner`는 다른 역할에 멤버로 가질 수 없습니다. 다른 역할과 마찬가지로 이 역할이 객체를 소유하거나 기본 접근 권한의 대상이 될 수 있습니다.

특히 `pg_database_owner` 역할은 `public` 스키마를 소유합니다. 이를 통해 데이터베이스 소유자가 해당 데이터베이스에서 로컬 사용을 제어합니다. 기본적으로 이 역할을 통해 모든 역할은 `public` 스키마에 객체를 생성할 수 있습니다.

#### pg_read_all_data 및 pg_write_all_data

`pg_read_all_data`와 `pg_write_all_data` 역할은 각각 데이터에 대한 읽기 또는 쓰기 권한만 제공하며, 행 수준 보안이 활성화되면 해당 정책을 우회하지 않습니다. 따라서 `BYPASSRLS`가 설정되지 않으면 행 수준 보안이 접근을 차단할 수 있습니다. 행 수준 보안을 사용할 때는 `pg_read_all_data` 및 `pg_write_all_data` 역할에 `BYPASSRLS`를 설정하는 것이 권장됩니다.

#### pg_signal_backend

`pg_signal_backend` 역할은 신뢰할 수 있는 비슈퍼유저가 다른 백엔드에 신호를 보낼 수 있도록 하기 위한 것입니다. 현재 이 역할은 다른 세션에서 쿼리를 취소하거나 세션을 종료하기 위해 신호를 보낼 수 있게 합니다. 그러나 이 역할의 멤버인 사용자는 슈퍼유저가 소유한 백엔드에 신호를 보낼 수 없습니다.

### 서버 파일 접근 역할의 보안 주의사항

`pg_read_server_files`, `pg_write_server_files`, `pg_execute_server_program` 역할은 사용자가 데이터베이스 서버에서 데이터베이스를 실행하는 사용자로 서버 파일에 접근하거나 서버에서 프로그램을 실행할 수 있게 합니다. 이러한 역할의 멤버에게 부여되는 접근은 모든 데이터베이스 수준 권한 검사를 우회하므로, 이러한 역할을 부여할 때는 슈퍼유저 접근 권한으로 사용할 수 있는 운영 체제 파일 시스템 접근 권한도 함께 부여된다는 점을 고려해야 합니다. 파일 시스템 접근 권한이 적절히 제한되어 있더라도, 사용자는 서버에서 클라이언트로 전송할 데이터에 특권 데이터를 포함시킬 수 있습니다.

경고: 이러한 역할을 부여할 때는 매우 신중해야 합니다.

### 버전 변경 사항

사전 정의된 역할은 아닙니다(이름처럼) — 역할 수정, 특히 `ALTER ROLE`을 사용한 비밀번호 할당이 가능합니다.

향후 버전에서 특권 기능이 추가되면 이러한 역할의 권한이 변경될 수 있습니다. 관리자는 릴리스 노트에서 변경 사항을 모니터링해야 합니다.

---

## 21.6. 함수 보안 (Function Security)

함수, 트리거, 행 수준 보안 정책은 사용자가 자신도 모르게 삽입된 다른 사용자의 코드를 실행할 수 있게 합니다. 따라서 이러한 메커니즘은 사용자가 백엔드 서버에 코드를 삽입하여 다른 사용자가 의도치 않게 실행하게 하는 "트로이 목마" 유형의 공격에 제한 없이 노출되도록 허용합니다.

### 보안 대책

가장 강력한 보호는 객체를 정의할 수 있는 사람에 대한 엄격한 통제입니다. 이것이 실행 가능하지 않은 경우, 신뢰할 수 있는 소유자가 정의한 객체만 참조하는 쿼리를 작성하세요. `search_path`에서 신뢰할 수 없는 사용자가 객체를 생성할 수 있는 스키마를 제거하세요.

- 엄격한 접근 제어: 객체를 정의할 수 있는 사람에 대한 통제
- 신뢰할 수 있는 소유자의 객체만 참조: 신뢰할 수 있는 소유자가 정의한 객체만 사용
- search_path 관리: 신뢰할 수 없는 사용자가 객체를 생성할 수 있는 스키마를 `search_path`에서 제거

### 함수 실행 환경

함수는 데이터베이스 서버 데몬의 운영 체제 권한으로 백엔드 서버 프로세스 내에서 실행됩니다. 함수에 사용되는 프로그래밍 언어가 검사되지 않은 메모리 접근을 허용하면, 서버의 내부 데이터 구조를 변경할 수 있습니다.

### 신뢰할 수 없는 언어

따라서 무엇보다도 이러한 함수는 시스템 접근 제어를 우회할 수 있습니다. 이러한 접근을 허용하는 함수 언어는 "신뢰할 수 없음(untrusted)"으로 간주되며, PostgreSQL은 슈퍼유저만 이러한 언어로 작성된 함수를 생성할 수 있도록 허용합니다.

주요 사항:
- 메모리 접근 제어가 없는 언어: 서버의 내부 데이터 구조 변경 가능
- 시스템 접근 제어 우회: 이론적으로 모든 시스템 접근 제어 우회 가능
- 제한 사항: PostgreSQL에서는 이러한 언어로 작성된 함수는 슈퍼유저만 생성 가능

---

## 참고 자료

- [PostgreSQL 18 공식 문서 - Chapter 21. Database Roles](https://www.postgresql.org/docs/18/user-manag.html)
- [CREATE ROLE](https://www.postgresql.org/docs/18/sql-createrole.html)
- [ALTER ROLE](https://www.postgresql.org/docs/18/sql-alterrole.html)
- [DROP ROLE](https://www.postgresql.org/docs/18/sql-droprole.html)
- [GRANT](https://www.postgresql.org/docs/18/sql-grant.html)
- [REVOKE](https://www.postgresql.org/docs/18/sql-revoke.html)
