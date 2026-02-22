# 제29장. 논리적 복제 (Logical Replication)

> PostgreSQL 18 공식 문서 번역

원문: [https://www.postgresql.org/docs/current/logical-replication.html](https://www.postgresql.org/docs/current/logical-replication.html)

---

## 목차

- [개요](#개요)
- [29.1. 발행 (Publication)](#291-발행-publication)
  - [29.1.1. 복제 식별자 (Replica Identity)](#2911-복제-식별자-replica-identity)
- [29.2. 구독 (Subscription)](#292-구독-subscription)
  - [29.2.1. 복제 슬롯 관리](#2921-복제-슬롯-관리)
  - [29.2.2. 예제: 논리적 복제 설정](#2922-예제-논리적-복제-설정)
  - [29.2.3. 예제: 지연된 복제 슬롯 생성](#2923-예제-지연된-복제-슬롯-생성)
- [29.3. 논리적 복제 장애 조치](#293-논리적-복제-장애-조치)
- [29.4. 행 필터 (Row Filters)](#294-행-필터-row-filters)
  - [29.4.1. 행 필터 규칙](#2941-행-필터-규칙)
  - [29.4.2. 표현식 제한사항](#2942-표현식-제한사항)
  - [29.4.3. UPDATE 변환](#2943-update-변환)
  - [29.4.4. 파티션 테이블](#2944-파티션-테이블)
  - [29.4.5. 초기 데이터 동기화](#2945-초기-데이터-동기화)
  - [29.4.6. 다중 행 필터 결합](#2946-다중-행-필터-결합)
  - [29.4.7. 예제](#2947-예제)
- [29.5. 컬럼 목록 (Column Lists)](#295-컬럼-목록-column-lists)
  - [29.5.1. 예제](#2951-예제)
- [29.6. 생성된 컬럼 복제](#296-생성된-컬럼-복제)
- [29.7. 충돌 (Conflicts)](#297-충돌-conflicts)
- [29.8. 제한사항 (Restrictions)](#298-제한사항-restrictions)
- [29.9. 아키텍처 (Architecture)](#299-아키텍처-architecture)
  - [29.9.1. 초기 스냅샷](#2991-초기-스냅샷)
- [29.10. 모니터링 (Monitoring)](#2910-모니터링-monitoring)
- [29.11. 보안 (Security)](#2911-보안-security)
- [29.12. 구성 설정](#2912-구성-설정)
  - [29.12.1. 발행자](#29121-발행자)
  - [29.12.2. 구독자](#29122-구독자)
- [29.13. 업그레이드](#2913-업그레이드)
  - [29.13.1. 발행자 업그레이드 준비](#29131-발행자-업그레이드-준비)
  - [29.13.2. 구독자 업그레이드 준비](#29132-구독자-업그레이드-준비)
  - [29.13.3. 논리적 복제 클러스터 업그레이드](#29133-논리적-복제-클러스터-업그레이드)
- [29.14. 빠른 설정](#2914-빠른-설정)

---

## 개요

논리적 복제(Logical Replication) 는 복제 식별자(일반적으로 기본 키)를 기반으로 데이터 객체와 해당 변경 사항을 복제하는 방법입니다. 이는 정확한 블록 주소와 바이트 단위 복제를 사용하는 물리적 복제와 대조됩니다.

논리적 복제는 발행 및 구독 모델(publish and subscribe model) 을 사용합니다. 하나 이상의 구독자(subscriber) 가 발행자(publisher) 노드의 하나 이상의 발행(publication) 을 구독합니다. 구독자는 발행에서 데이터를 가져오며, 캐스케이딩 복제를 위해 데이터를 다시 발행할 수도 있습니다.

논리적 복제는 테이블 데이터의 초기 스냅샷을 가져온 다음 후속 변경 사항을 전송합니다. 이는 단일 구독 내에서 트랜잭션 일관성을 보장합니다.

### 주요 사용 사례

논리적 복제의 일반적인 사용 사례는 다음과 같습니다:

- 데이터베이스의 증분 변경 사항을 수신하는 즉시 구독자에게 전송
- 변경 사항이 구독자에 도착할 때 트리거 실행
- 여러 데이터베이스를 단일 데이터베이스로 통합 (예: 분석 목적)
- 서로 다른 PostgreSQL 메이저 버전 간 복제
- 서로 다른 플랫폼 간 복제 (예: Linux에서 Windows로)
- 서로 다른 사용자 그룹에게 복제된 데이터에 대한 접근 권한 부여
- 데이터베이스의 하위 집합을 다른 서버와 공유

---

## 29.1. 발행 (Publication)

발행(Publication) 은 테이블 또는 테이블 그룹에서 생성된 변경 사항의 집합이며, 변경 집합(change set) 또는 복제 집합(replication set)이라고도 합니다. 발행은 모든 물리적 복제 주 서버에서 정의할 수 있으며, 발행이 정의된 노드를 발행자(publisher) 라고 합니다.

### 주요 특성

- 데이터베이스 범위: 각 발행은 하나의 데이터베이스에만 존재합니다.
- 스키마와 독립적: 발행은 스키마와 다르며 테이블 접근에 영향을 주지 않습니다.
- 다중 발행: 각 테이블은 여러 발행에 추가될 수 있습니다.
- 콘텐츠 유형: 테이블과 스키마의 모든 테이블을 포함할 수 있습니다.
- 명시적 추가: `ALL TABLES`로 생성된 발행을 제외하고 객체는 명시적으로 추가해야 합니다.

### 작업 유형

발행은 다음 변경 사항의 조합으로 제한할 수 있습니다:

- `INSERT`
- `UPDATE`
- `DELETE`
- `TRUNCATE`

기본값: 모든 작업 유형이 복제됩니다.

> 참고: 이러한 사양은 DML 작업에만 적용되며 초기 데이터 동기화에는 영향을 미치지 않습니다. 행 필터는 `TRUNCATE` 명령에 영향을 미치지 않습니다.

### 발행 관리

발행은 다음 명령으로 관리됩니다:

- 생성: `CREATE PUBLICATION` 명령
- 수정: `ALTER PUBLICATION` 명령
- 삭제: `DROP PUBLICATION` 명령

`ALTER PUBLICATION`을 사용하여 테이블을 동적으로 추가하고 제거할 수 있습니다:

- `ADD TABLE` - 트랜잭션 작업
- `DROP TABLE` - 트랜잭션 작업

두 작업 모두 트랜잭션이 커밋되면 올바른 스냅샷에서 복제를 시작/중지합니다.

모든 발행은 여러 구독자를 가질 수 있습니다.

### 29.1.1. 복제 식별자 (Replica Identity)

발행된 테이블은 `UPDATE` 및 `DELETE` 작업을 복제하기 위해 복제 식별자(replica identity) 가 구성되어 있어야 합니다. 이를 통해 구독자 측에서 적절한 행을 식별할 수 있습니다.

#### 복제 식별자 옵션

##### 1. 기본 키 (기본값)
기본 키가 존재하는 경우 기본적으로 사용됩니다.

##### 2. 고유 인덱스
다른 고유 인덱스를 복제 식별자로 설정할 수 있습니다(특정 추가 요구 사항 있음).

##### 3. FULL
- 전체 행이 키가 됩니다.
- 적합한 키가 없을 때 사용됩니다.
- 구독자 측의 인덱스가 행 검색을 지원할 수 있습니다.
- 후보 인덱스 요구 사항:
  - btree 또는 hash여야 합니다.
  - 부분 인덱스가 아니어야 합니다.
  - 인덱스의 가장 왼쪽 필드는 발행된 테이블 컬럼을 참조하는 컬럼(표현식이 아님)이어야 합니다.

> 경고: 적합한 인덱스가 없으면 `FULL` 복제 식별자는 매우 비효율적이며 대체 수단으로만 사용해야 합니다.

##### 4. NOTHING / 기본 키 없는 DEFAULT / 삭제된 인덱스의 USING INDEX
이러한 작업을 복제하는 발행에서 `UPDATE` 또는 `DELETE` 작업을 지원할 수 없습니다. 이러한 작업을 시도하면 발행자에서 오류가 발생합니다.

#### 중요 규칙

- INSERT 작업: 복제 식별자에 관계없이 진행됩니다.
- 구독자 구성: 발행자에 `FULL` 이외의 복제 식별자가 설정된 경우 구독자 측에 동일하거나 더 적은 컬럼이 설정되어야 합니다.

#### 구성

복제 식별자 설정에 대한 자세한 내용은 `ALTER TABLE...REPLICA IDENTITY`를 참조하세요.

---

## 29.2. 구독 (Subscription)

구독(Subscription) 은 논리적 복제의 다운스트림 측입니다. 구독이 정의된 노드를 구독자(subscriber) 라고 합니다. 구독은 다른 데이터베이스에 대한 연결과 구독하려는 발행 집합을 정의합니다.

### 주요 특성

- 구독자 데이터베이스는 다른 PostgreSQL 인스턴스처럼 동작하며 다른 데이터베이스의 발행자가 될 수 있습니다.
- 구독자 노드는 여러 구독을 가질 수 있습니다.
- 단일 발행자-구독자 쌍 사이에 여러 구독이 존재할 수 있습니다(발행 객체 중복에 주의해야 합니다).
- 각 구독은 하나의 복제 슬롯을 통해 변경 사항을 수신합니다.
- 논리적 복제 구독은 동기 복제를 위한 대기 서버가 될 수 있습니다(대기 이름은 기본적으로 구독 이름입니다).
- 구독은 현재 사용자가 슈퍼유저인 경우에만 `pg_dump`로 덤프됩니다.

### 관리 명령

구독은 세 가지 주요 명령으로 관리됩니다:

- CREATE SUBSCRIPTION - 구독 추가
- ALTER SUBSCRIPTION - 구독 중지/재개
- DROP SUBSCRIPTION - 구독 제거

> 중요 참고: 구독이 삭제되고 다시 생성되면 동기화 정보가 손실되어 데이터를 다시 동기화해야 합니다.

### 스키마 및 테이블 매칭

- 스키마 정의는 복제되지 않습니다. 발행된 테이블이 구독자에 존재해야 합니다.
- 일반 테이블만 복제 대상이 될 수 있습니다(뷰 불가).
- 테이블은 정규화된 테이블 이름 으로 매칭됩니다(다른 이름의 테이블로의 복제는 지원되지 않습니다).
- 컬럼은 이름 으로 매칭됩니다(순서는 일치할 필요 없음).
- 데이터 유형은 텍스트 표현이 대상 유형으로 변환될 수 있으면 일치할 필요가 없습니다(예: `integer` -> `bigint`).
- 대상 테이블은 추가 컬럼 을 가질 수 있습니다(기본값으로 채워짐).
- 바이너리 복제 는 컬럼 매칭에 대해 더 제한적입니다.

### 29.2.1. 복제 슬롯 관리

각 활성 구독은 원격(발행) 측의 복제 슬롯에서 변경 사항을 수신합니다.

#### 테이블 동기화 슬롯

테이블 동기화 슬롯은 일시적이며, 초기 테이블 동기화를 위해 내부적으로 생성되고 더 이상 필요하지 않으면 자동으로 삭제됩니다. 명명 패턴: `pg_%u_sync_%u_%llu` (구독 OID, 테이블 relid, 시스템 식별자)

#### 슬롯 관리 시나리오

##### 1. 복제 슬롯이 이미 존재하는 경우

`create_slot = false` 옵션을 사용하여 기존 슬롯과 연결합니다:

```sql
CREATE SUBSCRIPTION sub1
  CONNECTION '...'
  PUBLICATION pub1
  WITH (create_slot = false);
```

##### 2. 구독 생성 시 원격 호스트에 연결할 수 없는 경우

`connect = false` 옵션 사용(`pg_dump`에서 사용):

```sql
CREATE SUBSCRIPTION sub1
  CONNECTION '...'
  PUBLICATION pub1
  WITH (connect = false);
```

구독 활성화 전에 원격 복제 슬롯을 수동으로 생성해야 합니다.

##### 3. 구독 삭제 시 복제 슬롯 유지

구독자 데이터베이스를 다른 호스트로 이동할 때 유용합니다:

```sql
ALTER SUBSCRIPTION sub1 DISABLE;
ALTER SUBSCRIPTION sub1 SET (slot_name = NONE);
DROP SUBSCRIPTION sub1;
```

##### 4. 구독 삭제 시 원격 호스트에 연결할 수 없는 경우

삭제 전에 슬롯을 분리합니다:

```sql
ALTER SUBSCRIPTION sub1 SET (slot_name = NONE);
DROP SUBSCRIPTION sub1;
```

원격 인스턴스가 더 이상 존재하지 않으면 추가 조치가 필요 없습니다. 그렇지 않으면 남은 슬롯을 수동으로 삭제하여 WAL 예약 및 디스크 공간 문제를 방지해야 합니다.

### 29.2.2. 예제: 논리적 복제 설정

#### 단계 1: 발행자에 테스트 테이블 생성

```sql
/* pub # */ CREATE TABLE t1(a int, b text, PRIMARY KEY(a));
/* pub # */ CREATE TABLE t2(c int, d text, PRIMARY KEY(c));
/* pub # */ CREATE TABLE t3(e int, f text, PRIMARY KEY(e));
```

#### 단계 2: 구독자에 동일한 테이블 생성

```sql
/* sub # */ CREATE TABLE t1(a int, b text, PRIMARY KEY(a));
/* sub # */ CREATE TABLE t2(c int, d text, PRIMARY KEY(c));
/* sub # */ CREATE TABLE t3(e int, f text, PRIMARY KEY(e));
```

#### 단계 3: 발행자 측에 데이터 삽입

```sql
/* pub # */ INSERT INTO t1 VALUES (1, 'one'), (2, 'two'), (3, 'three');
/* pub # */ INSERT INTO t2 VALUES (1, 'A'), (2, 'B'), (3, 'C');
/* pub # */ INSERT INTO t3 VALUES (1, 'i'), (2, 'ii'), (3, 'iii');
```

#### 단계 4: 발행 생성

```sql
/* pub # */ CREATE PUBLICATION pub1 FOR TABLE t1;
/* pub # */ CREATE PUBLICATION pub2 FOR TABLE t2 WITH (publish = 'truncate');
/* pub # */ CREATE PUBLICATION pub3a FOR TABLE t3 WITH (publish = 'truncate');
/* pub # */ CREATE PUBLICATION pub3b FOR TABLE t3 WHERE (e > 5);
```

발행 세부사항:
- `pub1`: t1의 모든 작업을 발행
- `pub2`: t2의 TRUNCATE 작업만 발행
- `pub3a`: t3의 TRUNCATE 작업만 발행
- `pub3b`: 행 필터(e > 5)가 있는 t3 발행

#### 단계 5: 구독 생성

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=sub1'
/* sub - */ PUBLICATION pub1;

/* sub # */ CREATE SUBSCRIPTION sub2
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=sub2'
/* sub - */ PUBLICATION pub2;

/* sub # */ CREATE SUBSCRIPTION sub3
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=sub3'
/* sub - */ PUBLICATION pub3a, pub3b;
```

#### 단계 6: 초기 데이터 복사 확인

발행의 `publish` 작업에 관계없이 초기 테이블 데이터가 복사됩니다.

초기 동기화 후 구독자:

```sql
/* sub # */ SELECT * FROM t1;
 a |   b
---+-------
 1 | one
 2 | two
 3 | three
(3 rows)

/* sub # */ SELECT * FROM t2;
 c | d
---+---
 1 | A
 2 | B
 3 | C
(3 rows)

/* sub # */ SELECT * FROM t3;
 e |  f
---+-----
 1 | i
 2 | ii
 3 | iii
(3 rows)
```

> 참고: `pub3b`에 행 필터(e > 5)가 있지만 `pub3a`에 행 필터가 없으므로 초기 데이터 복사에는 모든 행이 포함됩니다.

#### 단계 7: 발행자에 추가 데이터 삽입

```sql
/* pub # */ INSERT INTO t1 VALUES (4, 'four'), (5, 'five'), (6, 'six');
/* pub # */ INSERT INTO t2 VALUES (4, 'D'), (5, 'E'), (6, 'F');
/* pub # */ INSERT INTO t3 VALUES (4, 'iv'), (5, 'v'), (6, 'vi');
```

삽입 후 발행자 데이터:

```sql
/* pub # */ SELECT * FROM t1;
 a |   b
---+-------
 1 | one
 2 | two
 3 | three
 4 | four
 5 | five
 6 | six
(6 rows)

/* pub # */ SELECT * FROM t2;
 c | d
---+---
 1 | A
 2 | B
 3 | C
 4 | D
 5 | E
 6 | F
(6 rows)

/* pub # */ SELECT * FROM t3;
 e |  f
---+-----
 1 | i
 2 | ii
 3 | iii
 4 | iv
 5 | v
 6 | vi
(6 rows)
```

#### 단계 8: 일반 복제가 발행 설정을 준수하는지 확인

일반 복제 중에는 적절한 `publish` 작업이 적용됩니다:
- `pub2`와 `pub3a`는 INSERT 작업을 복제하지 않습니다.
- `pub3b`는 행 필터(e > 5)에 맞는 데이터만 복제합니다.

일반 복제 후 구독자 데이터:

```sql
/* sub # */ SELECT * FROM t1;
 a |   b
---+-------
 1 | one
 2 | two
 3 | three
 4 | four
 5 | five
 6 | six
(6 rows)

/* sub # */ SELECT * FROM t2;
 c | d
---+---
 1 | A
 2 | B
 3 | C
(3 rows)

/* sub # */ SELECT * FROM t3;
 e |  f
---+-----
 1 | i
 2 | ii
 3 | iii
 6 | vi
(4 rows)
```

결과 설명:
- `t1`: 모든 6행 (pub1이 모든 작업을 발행)
- `t2`: 3행만 (pub2는 INSERT를 복제하지 않으므로 새 행이 추가되지 않음)
- `t3`: 4행 (초기 동기화에서 3행, pub3b의 행 필터로 인해 e=6인 행만 추가)

### 29.2.3. 예제: 지연된 복제 슬롯 생성

원격 복제 슬롯이 자동으로 생성되지 않는 경우 구독 활성화 전에 수동으로 생성해야 합니다. 이 예제들은 표준 논리적 디코딩 출력 플러그인(`pgoutput`)을 사용합니다.

#### 초기 설정

예제를 위한 발행 생성:

```sql
/* pub # */ CREATE PUBLICATION pub1 FOR ALL TABLES;
```

#### 예제 1: `connect = false` (슬롯 이름이 기본적으로 구독 이름)

구독자에서 - 구독 생성:

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub'
/* sub - */ PUBLICATION pub1
/* sub - */ WITH (connect=false);
WARNING:  subscription was created, but is not connected
HINT:  To initiate replication, you must manually create the replication slot, enable the subscription, and refresh the subscription.
```

발행자에서 - 슬롯 수동 생성:

```sql
/* pub # */ SELECT * FROM pg_create_logical_replication_slot('sub1', 'pgoutput');
 slot_name |    lsn
-----------+-----------
 sub1      | 0/19404D0
(1 row)
```

구독자에서 - 활성화 완료:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 ENABLE;
/* sub # */ ALTER SUBSCRIPTION sub1 REFRESH PUBLICATION;
```

#### 예제 2: `connect = false`와 명시적 `slot_name`

구독자에서 - 구독 생성:

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub'
/* sub - */ PUBLICATION pub1
/* sub - */ WITH (connect=false, slot_name='myslot');
WARNING:  subscription was created, but is not connected
HINT:  To initiate replication, you must manually create the replication slot, enable the subscription, and refresh the subscription.
```

발행자에서 - 지정된 이름으로 슬롯 수동 생성:

```sql
/* pub # */ SELECT * FROM pg_create_logical_replication_slot('myslot', 'pgoutput');
 slot_name |    lsn
-----------+-----------
 myslot    | 0/19059A0
(1 row)
```

구독자에서 - 활성화 완료:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 ENABLE;
/* sub # */ ALTER SUBSCRIPTION sub1 REFRESH PUBLICATION;
```

#### 예제 3: `slot_name = NONE` (완전 지연된 슬롯 생성)

구독자에서 - 구독 생성:

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub'
/* sub - */ PUBLICATION pub1
/* sub - */ WITH (slot_name=NONE, enabled=false, create_slot=false);
```

발행자에서 - 임의의 이름으로 슬롯 수동 생성:

```sql
/* pub # */ SELECT * FROM pg_create_logical_replication_slot('myslot', 'pgoutput');
 slot_name |    lsn
-----------+-----------
 myslot    | 0/1905930
(1 row)
```

구독자에서 - 구독과 슬롯 연결:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 SET (slot_name='myslot');
```

구독자에서 - 활성화 완료:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 ENABLE;
/* sub # */ ALTER SUBSCRIPTION sub1 REFRESH PUBLICATION;
```

#### 구독 옵션 요약 표

| 옵션 | 목적 | 예제 |
|------|------|------|
| `create_slot` | 복제 슬롯 자동 생성 여부 | `WITH (create_slot = false)` |
| `connect` | 발행자에 즉시 연결 여부 | `WITH (connect = false)` |
| `slot_name` | 사용할 복제 슬롯 이름 | `WITH (slot_name = 'myslot')` 또는 `WITH (slot_name = NONE)` |
| `enabled` | 구독 초기 활성화 여부 | `WITH (enabled = false)` |
| `application_name` | 동기 복제용 애플리케이션 이름 | `CONNECTION '...' application_name=sub1'` |

---

## 29.3. 논리적 복제 장애 조치

논리적 복제 장애 조치를 통해 주 발행자 노드가 다운되더라도 구독자 노드가 발행자로부터 데이터 복제를 계속할 수 있습니다. 이를 위해서는 다음이 필요합니다:

1. 발행자 노드에 해당하는 물리적 대기 서버
2. `failover = true` 매개변수를 사용하여 대기 서버에 동기화된 논리적 슬롯
3. 적절한 동기화 구성

### 주요 개념

장애 조치 매개변수: 구독 생성 시 `failover = true`를 지정하여 논리적 슬롯을 대기 서버에 동기화합니다. 이를 통해 대기 서버 승격 후 원활한 전환이 가능합니다.

동기화 요구사항:
- 슬롯 동기화는 비동기적으로 수행됩니다.
- 대기 서버가 구독자보다 앞서 유지되도록 `synchronized_standby_slots`를 구성합니다.
- 장애 조치 전에 대기 서버가 준비되었는지 확인해야 합니다.

### 장애 조치 준비 상태 확인 단계

#### 단계 1: 구독자에서 복제 슬롯 식별

동기화해야 할 슬롯을 찾기 위해 구독자 노드에서 실행:

```sql
/* sub # */ SELECT
               array_agg(quote_literal(s.subslotname)) AS slots
           FROM  pg_subscription s
           WHERE s.subfailover AND
                 s.subslotname IS NOT NULL;
```

결과 예시:

```
 slots
-------
 {'sub1','sub2','sub3'}
(1 row)
```

#### 단계 2: 구독자에서 테이블 동기화 슬롯 식별

장애 조치가 활성화된 구독이 포함된 각 데이터베이스에서 실행:

```sql
/* sub # */ SELECT
               array_agg(quote_literal(slot_name)) AS slots
           FROM
           (
               SELECT CONCAT('pg_', srsubid, '_sync_', srrelid, '_', ctl.system_identifier) AS slot_name
               FROM pg_control_system() ctl, pg_subscription_rel r, pg_subscription s
               WHERE r.srsubstate = 'f' AND s.oid = r.srsubid AND s.subfailover
           );
```

결과 예시:

```
 slots
-------
 {'pg_16394_sync_16385_7394666715149055164'}
(1 row)
```

> 참고: 테이블 동기화 슬롯은 테이블 복사가 완료된 경우에만 동기화해야 합니다(52.55절 참조).

#### 단계 3: 대기 서버에서 슬롯 확인

식별된 슬롯이 대기 서버에 존재하고 장애 조치 준비가 되었는지 확인:

```sql
/* standby # */ SELECT slot_name, (synced AND NOT temporary AND invalidation_reason IS NULL) AS failover_ready
               FROM pg_replication_slots
               WHERE slot_name IN
                   ('sub1','sub2','sub3', 'pg_16394_sync_16385_7394666715149055164');
```

결과 예시:

```
  slot_name                                 | failover_ready
--------------------------------------------+----------------
  sub1                                      | t
  sub2                                      | t
  sub3                                      | t
  pg_16394_sync_16385_7394666715149055164   | t
(4 rows)
```

성공 기준: 모든 슬롯이 `failover_ready = true`로 존재하면 장애 조치 후 새 주 서버에서 구독을 계속할 수 있습니다.

### 계획된 장애 조치를 위한 대체 방법

계획된 장애 조치 또는 모든 구독자(PostgreSQL 및 비PostgreSQL) 확인을 위해 주 서버에서 직접 쿼리:

```sql
/* primary # */ SELECT array_agg(quote_literal(r.slot_name)) AS slots
               FROM pg_replication_slots r
               WHERE r.failover AND NOT r.temporary;
```

결과 예시:

```
 slots
-------
 {'sub1','sub2','sub3', 'pg_16394_sync_16385_7394666715149055164'}
(1 row)
```

### 중요 고려사항

- 장애 조치 후 지정된 대기 서버가 서비스할 각 구독자 노드 에서 단계 1-2를 실행합니다.
- 비PostgreSQL 구독자는 복제 슬롯을 식별하기 위해 자체 방법을 사용할 수 있습니다.
- 장애 조치를 진행하기 전에 확인 목록을 완료합니다.
- 모든 슬롯이 대기 서버에 존재하고 `failover_ready = true`를 표시해야 합니다.

---

## 29.4. 행 필터 (Row Filters)

행 필터를 사용하면 발행된 테이블에서 구독자로 데이터를 선택적으로 복제할 수 있습니다. 기본적으로 발행된 테이블의 모든 데이터가 복제되지만 행 필터는 `WHERE` 절을 사용하여 복제할 행을 지정하여 이를 줄입니다.

### 29.4.1. 행 필터 규칙

주요 규칙:
- 행 필터는 변경 사항을 발행하기 전에 적용됩니다.
- 행 필터가 `false` 또는 `NULL`로 평가되면 해당 행은 복제되지 않습니다.
- `WHERE` 절은 복제 연결 역할을 사용하여 평가됩니다.
- 행 필터는 `TRUNCATE` 명령에 영향을 미치지 않습니다.

### 29.4.2. 표현식 제한사항

허용되는 표현식:
- 단순 표현식만 허용
- 포함할 수 없는 것:
  - 사용자 정의 함수, 연산자, 유형 또는 콜레이션
  - 시스템 컬럼 참조
  - 불변(immutable)이 아닌 내장 함수

컬럼 제한:
- `UPDATE` 또는 `DELETE` 작업의 경우: 행 필터는 복제 식별자가 포함하는 컬럼만 포함해야 합니다.
- `INSERT` 작업만 해당: 행 필터는 모든 컬럼을 사용할 수 있습니다.

### 29.4.3. UPDATE 변환

`UPDATE`가 처리될 때 행 필터는 이전 행과 새 행 모두에 대해 평가됩니다:

변환 규칙:

| 이전 행 | 새 행 | 변환 |
|---------|-------|------|
| 일치 안 함 | 일치 안 함 | 복제 안 함 |
| 일치 안 함 | 일치 | `INSERT` |
| 일치 | 일치 안 함 | `DELETE` |
| 일치 | 일치 | `UPDATE` |

설명:
- 이전 행이 필터를 만족하지만 새 행이 만족하지 않으면 -> `DELETE` (일관성 유지)
- 이전 행이 필터를 만족하지 않지만 새 행이 만족하면 -> `INSERT` (일관성 유지)
- 둘 다 일치하거나 둘 다 일치하지 않으면 -> 일반 `UPDATE` 또는 복제 없음

### 29.4.4. 파티션 테이블

`publish_via_partition_root` 매개변수가 사용할 행 필터를 결정합니다:

- `true`인 경우: 루트 파티션 테이블의 행 필터가 사용됩니다.
- `false`(기본값)인 경우: 각 파티션의 행 필터가 사용됩니다.

### 29.4.5. 초기 데이터 동기화

- 행 필터 표현식을 만족하는 데이터만 구독자에 복사됩니다.
- 테이블이 서로 다른 `WHERE` 절을 가진 여러 발행에 있는 경우 모든 표현식을 만족하는 행이 복사됩니다(OR 논리).

> 경고: 초기 데이터 동기화는 `publish` 매개변수를 고려하지 않으므로 일반적으로 DML을 통해 복제되지 않는 일부 행이 복사될 수 있습니다.

> 참고: 15 이전 릴리스의 구독자는 초기 복사 중 행 필터를 사용하지 않습니다(전체 테이블 데이터가 복사됨).

### 29.4.6. 다중 행 필터 결합

동일한 테이블이 서로 다른 행 필터를 가진 여러 발행에 나타나면 표현식이 OR로 결합 됩니다. 다음 경우 다른 모든 필터는 중복됩니다:

1. 하나의 발행에 행 필터가 없는 경우
2. 하나의 발행이 `FOR ALL TABLES`를 사용하는 경우
3. 하나의 발행이 `FOR TABLES IN SCHEMA`를 사용하고 해당 테이블을 포함하는 경우

### 29.4.7. 예제

#### 설정: 테이블 생성

```sql
CREATE TABLE t1(a int, b int, c text, PRIMARY KEY(a,c));
CREATE TABLE t2(d int, e int, f int, PRIMARY KEY(d));
CREATE TABLE t3(g int, h int, i int, PRIMARY KEY(g));
```

#### 행 필터가 있는 발행 생성

```sql
CREATE PUBLICATION p1 FOR TABLE t1 WHERE (a > 5 AND c = 'NSW');
CREATE PUBLICATION p2 FOR TABLE t1, t2 WHERE (e = 99);
CREATE PUBLICATION p3 FOR TABLE t2 WHERE (d = 10), t3 WHERE (g = 10);
```

#### 행 필터 보기

```sql
\dRp+   -- 행 필터가 있는 발행 세부 정보 표시
\d t1   -- 발행 및 필터 세부 정보가 있는 테이블 표시
```

#### 구독자 설정

```sql
CREATE TABLE t1(a int, b int, c text, PRIMARY KEY(a,c));
CREATE SUBSCRIPTION s1
  CONNECTION 'host=localhost dbname=test_pub application_name=s1'
  PUBLICATION p1;
```

#### 예제 1: 행 필터가 있는 INSERT

```sql
-- 발행자
INSERT INTO t1 VALUES
  (2, 102, 'NSW'),   -- 일치하지 않음 (a > 5)
  (6, 106, 'NSW'),   -- 필터 일치
  (9, 109, 'NSW');   -- 필터 일치

-- 구독자는 일치하는 행만 봄
SELECT * FROM t1;
-- a | b   | c
-- 6 | 106 | NSW
-- 9 | 109 | NSW
```

#### 예제 2: 두 행이 모두 일치하는 UPDATE

```sql
-- 이전 행과 새 행 모두 필터를 만족 -> 일반 UPDATE
UPDATE t1 SET b = 999 WHERE a = 6;

-- 구독자는 변경 사항을 반영
SELECT * FROM t1;
-- a | b   | c
-- 6 | 999 | NSW
-- 9 | 109 | NSW
```

#### 예제 3: INSERT로 변환된 UPDATE

```sql
-- 이전 행 (a=2, NSW)는 일치하지 않았고, 새 행 (a=555, NSW)는 일치
UPDATE t1 SET a = 555 WHERE a = 2;

-- 구독자는 INSERT를 봄
SELECT * FROM t1;
-- a   | b   | c
-- 6   | 999 | NSW
-- 9   | 109 | NSW
-- 555 | 102 | NSW  -- 삽입됨
```

#### 예제 4: DELETE로 변환된 UPDATE

```sql
-- 이전 행 (a=9, NSW)는 일치했고, 새 행 (a=9, VIC)는 일치하지 않음
UPDATE t1 SET c = 'VIC' WHERE a = 9;

-- 구독자는 행을 삭제
SELECT * FROM t1;
-- a   | b   | c
-- 6   | 999 | NSW
-- 555 | 102 | NSW  -- a=9 행이 삭제됨
```

#### 파티션 테이블 예제

```sql
-- 발행자: 파티션 테이블 생성
CREATE TABLE parent(a int PRIMARY KEY) PARTITION BY RANGE(a);
CREATE TABLE child PARTITION OF parent DEFAULT;

-- 구독자: 동일한 구조
CREATE TABLE parent(a int PRIMARY KEY) PARTITION BY RANGE(a);
CREATE TABLE child PARTITION OF parent DEFAULT;

-- publish_via_partition_root=true로 발행
CREATE PUBLICATION p4
  FOR TABLE parent WHERE (a < 5),
           child WHERE (a >= 5)
  WITH (publish_via_partition_root=true);

CREATE SUBSCRIPTION s4
  CONNECTION 'host=localhost dbname=test_pub application_name=s4'
  PUBLICATION p4;

-- 데이터 삽입
INSERT INTO parent VALUES (2), (4), (6);
INSERT INTO child VALUES (3), (5), (7);

-- 구독자는 5 미만인 행만 봄 (부모의 필터 사용)
SELECT * FROM parent ORDER BY a;
-- a
-- 2
-- 3
-- 4
```

`publish_via_partition_root=false`일 때는 자식 파티션의 필터가 대신 사용됩니다:

```sql
DROP PUBLICATION p4;
CREATE PUBLICATION p4
  FOR TABLE parent,
           child WHERE (a >= 5)
  WITH (publish_via_partition_root=false);

-- 구독자는 이제 5 이상인 행을 봄 (자식의 필터 사용)
SELECT * FROM child ORDER BY a;
-- a
-- 5
-- 6
-- 7
```

---

## 29.5. 컬럼 목록 (Column Lists)

각 발행은 선택적으로 각 테이블의 어떤 컬럼이 구독자에 복제되는지 지정할 수 있습니다. 구독자 측의 테이블은 발행된 모든 컬럼을 최소한 가지고 있어야 합니다. 컬럼 목록이 지정되지 않으면 발행자의 모든 컬럼이 복제됩니다.

### 주요 사항

#### 컬럼 목록 동작
- 선택적 지정: 발행은 어떤 컬럼이 복제되는지 정의할 수 있습니다.
- 새 컬럼 자동 복제: 컬럼 목록이 지정되지 않으면 나중에 테이블에 추가된 모든 컬럼이 자동으로 복제됩니다.
- 동등하지 않음: 모든 컬럼을 나열하는 컬럼 목록을 갖는 것은 컬럼 목록이 전혀 없는 것과 동일하지 않습니다.
- 컬럼 순서: 목록의 컬럼 순서는 보존되지 않습니다.
- 단순 참조만: 컬럼 목록은 단순 컬럼 참조만 포함할 수 있습니다.

#### 보안 고려사항

> 경고: 보안을 위해 컬럼 목록에 의존하지 마세요. 악의적인 구독자는 특별히 발행되지 않은 컬럼에서 데이터를 얻을 수 있습니다. 보안이 중요한 경우 발행자 측에서 보호를 적용하세요.

#### 생성된 컬럼
생성된 컬럼은 컬럼 목록에 지정할 수 있으며, `publish_generated_columns` 발행 매개변수와 관계없이 발행할 수 있습니다(29.6절 참조).

#### 스키마 수준 발행
발행이 `FOR TABLES IN SCHEMA`를 발행할 때 컬럼 목록 지정은 지원되지 않습니다.

#### 파티션 테이블
파티션 테이블의 경우 `publish_via_partition_root` 매개변수가 어떤 컬럼 목록이 사용되는지 결정합니다:
- `true`인 경우: 루트 파티션 테이블의 컬럼 목록이 사용됩니다.
- `false`(기본값)인 경우: 각 파티션의 컬럼 목록이 사용됩니다.

#### UPDATE/DELETE 작업
발행이 `UPDATE` 또는 `DELETE` 작업을 발행하는 경우 모든 컬럼 목록은 테이블의 복제 식별자 컬럼(`REPLICA IDENTITY`로 정의됨)을 포함해야 합니다. `INSERT` 전용 발행의 경우 복제 식별자 컬럼을 생략할 수 있습니다.

#### TRUNCATE 명령
컬럼 목록은 `TRUNCATE` 명령에 영향을 미치지 않습니다.

#### 초기 데이터 동기화
- 발행된 컬럼만 초기 동기화 중에 복사됩니다.
- 예외: 구독자가 15 이전 릴리스인 경우 모든 컬럼이 복사됩니다(컬럼 목록 무시).
- 생성된 컬럼 예외: 구독자가 18 이전 릴리스인 경우 발행자에 정의되어 있어도 생성된 컬럼이 복사되지 않습니다.

#### 경고: 다중 발행의 컬럼 목록 결합

동일한 테이블에 서로 다른 컬럼 목록이 있는 여러 발행을 포함하는 구독은 지원되지 않습니다.

문제점:
- `CREATE SUBSCRIPTION`은 처음에 이러한 구독 생성을 방지합니다.
- 구독 생성 후 컬럼 목록을 변경하면 이 상황에 빠질 수 있습니다.
- 구독된 발행의 컬럼 목록을 변경하면 구독자 측에서 오류가 발생할 수 있습니다.

해결책:
1. 발행 측의 컬럼 목록이 모두 일치하도록 조정하거나
2. 구독을 다시 생성하거나
3. `ALTER SUBSCRIPTION ... DROP PUBLICATION`을 사용하여 문제가 되는 발행을 제거하고 다시 추가합니다.

### 29.5.1. 예제

#### 단계 1: 발행자에 테이블 생성

```sql
/* pub # */ CREATE TABLE t1(id int, a text, b text, c text, d text, e text, PRIMARY KEY(id));
```

#### 단계 2: 컬럼 목록이 있는 발행 생성

```sql
/* pub # */ CREATE PUBLICATION p1 FOR TABLE t1 (id, b, a, d);
```

> 참고: 목록의 컬럼 순서는 중요하지 않습니다(id, b, a, d는 id, a, b, d와 동등합니다).

#### 단계 3: 발행 컬럼 목록 보기

`psql` 사용:

```sql
/* pub # */ \dRp+
```

출력:

```
                                     Publication p1
Owner   | All tables | Inserts | Updates | Deletes | Truncates | Generated columns | Via root
--------|------------|---------|---------|---------|-----------|-------------------|----------
postgres| f          | t       | t       | t       | t         | none              | f

Tables:
    "public.t1" (id, a, b, d)
```

#### 단계 4: 테이블별 컬럼 목록 보기

```sql
/* pub # */ \d t1
```

출력:

```
             Table "public.t1"
Column |  Type   | Collation | Nullable | Default
-------|---------|-----------|----------|----------
id     | integer |           | not null |
a      | text    |           |          |
b      | text    |           |          |
c      | text    |           |          |
d      | text    |           |          |
e      | text    |           |          |

Indexes:
    "t1_pkey" PRIMARY KEY, btree (id)

Publications:
    "p1" (id, a, b, d)
```

#### 단계 5: 구독자 테이블 및 구독 생성

```sql
/* sub # */ CREATE TABLE t1(id int, b text, a text, d text, PRIMARY KEY(id));
/* sub # */ CREATE SUBSCRIPTION s1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=s1'
/* sub - */ PUBLICATION p1;
```

> 참고: 구독자 테이블은 발행된 컬럼만 필요합니다.

#### 단계 6: 발행자에 데이터 삽입

```sql
/* pub # */ INSERT INTO t1 VALUES(1, 'a-1', 'b-1', 'c-1', 'd-1', 'e-1');
/* pub # */ INSERT INTO t1 VALUES(2, 'a-2', 'b-2', 'c-2', 'd-2', 'e-2');
/* pub # */ INSERT INTO t1 VALUES(3, 'a-3', 'b-3', 'c-3', 'd-3', 'e-3');

/* pub # */ SELECT * FROM t1 ORDER BY id;
```

출력:

```
id |  a  |  b  |  c  |  d  |  e
---|-----|-----|-----|-----|-----
 1 | a-1 | b-1 | c-1 | d-1 | e-1
 2 | a-2 | b-2 | c-2 | d-2 | e-2
 3 | a-3 | b-3 | c-3 | d-3 | e-3
(3 rows)
```

#### 단계 7: 구독자에서 복제 확인

```sql
/* sub # */ SELECT * FROM t1 ORDER BY id;
```

출력 (발행된 컬럼만 복제됨):

```
id |  b  |  a  |  d
---|-----|-----|-----
 1 | b-1 | a-1 | d-1
 2 | b-2 | a-2 | d-2
 3 | b-3 | a-3 | d-3
(3 rows)
```

> 참고: 발행자의 컬럼 `c`와 `e`는 컬럼 목록에 없었기 때문에 복제되지 않습니다.

---

## 29.6. 생성된 컬럼 복제

구독자 테이블의 생성된 컬럼은 일반적으로 발행자 테이블과 동일하게 정의됩니다. 두 테이블 모두 `GENERATED` 컬럼이 있는 경우 구독자 테이블의 생성된 컬럼 값이 항상 사용되며, 구독자의 정의에 따라 계산됩니다.

### 주요 동작

PostgreSQL 18.0 이전: 논리적 복제는 `GENERATED` 컬럼을 전혀 발행하지 않습니다.

PostgreSQL 18.0 이상: 사용자는 저장된 생성 컬럼을 일반 컬럼처럼 발행하도록 선택할 수 있습니다.

### 생성된 컬럼 발행 방법

두 가지 방법을 사용할 수 있습니다:

1. `publish_generated_columns` 매개변수를 `stored`로 설정
   - PostgreSQL에 현재 및 미래의 저장된 생성 컬럼을 발행하도록 지시합니다.
   - 발행의 모든 테이블에 적용됩니다.

2. 테이블 컬럼 목록 지정
   - 발행할 저장된 생성 컬럼을 명시적으로 지정합니다.
   - `publish_generated_columns` 매개변수보다 우선합니다.

### 예제

발행자 설정:

```sql
/* pub # */ CREATE TABLE tab_gen_to_gen (a int, b int GENERATED ALWAYS AS (a + 1) STORED);
/* pub # */ INSERT INTO tab_gen_to_gen VALUES (1),(2),(3);
/* pub # */ CREATE PUBLICATION pub1 FOR TABLE tab_gen_to_gen;
/* pub # */ SELECT * FROM tab_gen_to_gen;
 a | b
---+---
 1 | 2
 2 | 3
 3 | 4
```

구독자 설정:

```sql
/* sub # */ CREATE TABLE tab_gen_to_gen (a int, b int GENERATED ALWAYS AS (a * 100) STORED);
/* sub # */ CREATE SUBSCRIPTION sub1 CONNECTION 'dbname=test_pub' PUBLICATION pub1;
/* sub # */ SELECT * from tab_gen_to_gen;
 a | b
---+----
 1 | 100
 2 | 200
 3 | 300
```

주목: 구독자는 발행자의 계산(a + 1)이 아닌 자체 생성 컬럼 계산(a * 100)을 사용합니다.

### 복제 결과 요약

| 생성 컬럼 발행? | 발행자 컬럼 | 구독자 컬럼 | 결과 |
|----------------|------------|------------|------|
| 아니오 | GENERATED | GENERATED | 발행자 컬럼이 복제되지 않음; 구독자의 생성 값 사용 |
| 아니오 | GENERATED | 일반 | 발행자 컬럼이 복제되지 않음; 구독자의 기본값 사용 |
| 아니오 | GENERATED | --없음-- | 발행자 컬럼이 복제되지 않음; 아무 일도 발생하지 않음 |
| 예 | GENERATED | GENERATED | 오류 - 지원되지 않음 |
| 예 | GENERATED | 일반 | 발행자 컬럼 값이 구독자에 복제됨 |
| 예 | GENERATED | --없음-- | 오류 - 구독자에 컬럼 없음 |

### 중요 경고

1. 단일 구독 내에서 동일한 테이블에 대해 서로 다른 컬럼 목록을 가진 여러 발행을 지원하지 않음
2. 충돌 시나리오: 하나의 발행은 생성된 컬럼을 발행하고 다른 발행은 동일한 테이블에 대해 발행하지 않는 경우
3. 18 이전 구독자: 발행자에 정의되어 있어도 초기 테이블 동기화에서 생성된 컬럼을 복사하지 않음

### 사용 사례

이 기능은 출력 플러그인을 통해 비PostgreSQL 데이터베이스로 데이터를 복제할 때 특히 유용하며, 특히 대상 데이터베이스가 생성된 컬럼을 지원하지 않는 경우에 유용합니다.

---

## 29.7. 충돌 (Conflicts)

논리적 복제는 일반 DML 작업과 유사하게 동작합니다. 데이터가 구독자 노드에서 로컬로 변경되더라도 업데이트됩니다. 들어오는 데이터가 제약 조건을 위반하면 복제가 중지됩니다(충돌). `UPDATE` 또는 `DELETE` 작업의 경우 누락된 데이터도 충돌로 간주되지만 오류가 발생하지 않으며 해당 작업은 단순히 건너뜁니다.

### 충돌 유형

#### `insert_exists`
- `NOT DEFERRABLE` 고유 제약 조건을 위반하는 행 삽입
- 원본 및 커밋 타임스탬프 세부 정보를 기록하려면 구독자에서 `track_commit_timestamp` 활성화 필요
- 결과: 수동으로 해결될 때까지 오류 발생

#### `update_origin_differs`
- 이전에 다른 원본에서 수정한 행 업데이트
- 구독자에서 `track_commit_timestamp`가 활성화된 경우에만 감지됨
- 결과: 로컬 행 원본에 관계없이 업데이트가 항상 적용됨

#### `update_exists`
- 업데이트된 행 값이 `NOT DEFERRABLE` 고유 제약 조건을 위반
- 충돌 세부 정보를 기록하려면 `track_commit_timestamp` 활성화 필요
- 파티션 테이블의 경우: 행이 새 파티션으로 이동하면 `insert_exists`가 발생할 수 있음
- 결과: 수동으로 해결될 때까지 오류 발생

#### `update_missing`
- 업데이트할 행을 찾을 수 없음
- 결과: 업데이트가 자동으로 건너뜀

#### `delete_origin_differs`
- 이전에 다른 원본에서 수정한 행 삭제
- `track_commit_timestamp`가 활성화된 경우에만 감지됨
- 결과: 로컬 행 원본에 관계없이 삭제가 항상 적용됨

#### `delete_missing`
- 삭제할 행을 찾을 수 없음
- 결과: 삭제가 자동으로 건너뜀

#### `multiple_unique_conflicts`
- 삽입 또는 업데이트하는 행이 여러 `NOT DEFERRABLE` 고유 제약 조건을 위반
- 충돌 세부 정보를 위해 `track_commit_timestamp` 활성화 필요
- 결과: 수동으로 해결될 때까지 오류 발생

### 로그 형식

```
LOG:  conflict detected on relation "_schemaname_._tablename_": conflict=_conflict_type_
DETAIL:  _detailed_explanation_.
{_detail_values_ [; ... ]}.
```

#### 세부 값 형식

```
Key (column_name [, ...])=(column_value [, ...])
existing local row [(column_name [, ...])=](column_value [, ...])
remote row [(column_name [, ...])=](column_value [, ...])
replica identity {(column_name [, ...])=(column_value [, ...]) | full [(column_name [, ...])=](column_value [, ...])}
```

### 로그 정보 세부사항

LOG 섹션:
- `_schemaname_._tablename_`: 관련된 로컬 릴레이션 식별
- `_conflict_type_`: 충돌 유형 (예: `insert_exists`, `update_exists`)

DETAIL 섹션:
- `_detailed_explanation_`: 가능한 경우 원본, 트랜잭션 ID, 커밋 타임스탬프 포함
- Key 섹션: 고유 제약 조건을 위반한 키 값 (`insert_exists`, `update_exists`, `multiple_unique_conflicts`의 경우)
- existing local row: 원본이 다르거나 키가 충돌할 때 로컬 행 세부 정보
- remote row: 충돌을 일으킨 원격 삽입/업데이트의 새 행 (업데이트에서 변경되지 않은/토스트된 컬럼의 경우 null 값)
- replica identity: 기존 행을 검색하는 데 사용된 복제 식별자 키 값; `REPLICA IDENTITY FULL`로 표시된 경우 전체 행 포함
- `_column_name_`: 사용자가 모든 테이블 컬럼에 접근할 권한이 없는 경우에만 기록됨
- `_column_value_`: 큰 값은 64바이트로 잘림

### 예제 오류 로그

```
ERROR:  conflict detected on relation "public.test": conflict=insert_exists
DETAIL:  Key already exists in unique index "t_pkey", which was modified locally in transaction 740 at 2024-06-26 10:47:04.727375+08.
Key (c)=(1); existing local row (1, 'local'); remote row (1, 'remote').
CONTEXT:  processing remote data for replication origin "pg_16395" during "INSERT" for replication target relation "public.test" in transaction 725 finished at 0/14C0378
```

### 권한 및 행 수준 보안

논리적 복제 작업은 구독 소유자 역할의 권한으로 실행됩니다. 다음으로 인해 충돌이 발생할 수 있습니다:
- 대상 테이블에 대한 권한 실패
- `INSERT`, `UPDATE`, `DELETE` 또는 `TRUNCATE` 작업을 거부하는 행 수준 보안(RLS) 정책

> 참고: RLS 제한은 향후 PostgreSQL 버전에서 해제될 수 있습니다.

### 충돌 해결

#### 수동 해결 단계

1. 충돌 식별 - 다음을 포함하는 오류 메시지에서:
   - LSN (Log Sequence Number): 예: `0/14C0378`
   - 복제 원본 이름: 예: `pg_16395`

2. 해결 방법 선택:

   옵션 A: ALTER SUBSCRIPTION SKIP 사용
   ```sql
   ALTER SUBSCRIPTION subscription_name SKIP (LSN '0/14C0378');
   ```

   옵션 B: pg_replication_origin_advance() 사용
   ```sql
   -- 먼저 구독 비활성화
   ALTER SUBSCRIPTION subscription_name DISABLE;

   -- 또는 구독 생성 시 disable_on_error 옵션 사용

   -- 그런 다음 다음 LSN으로 전진
   SELECT pg_replication_origin_advance('pg_16395', '0/14C0379');

   -- 현재 위치 확인
   SELECT * FROM pg_replication_origin_status;

   -- 구독 다시 활성화
   ALTER SUBSCRIPTION subscription_name ENABLE;
   ```

3. 근본 문제 해결:
   - 들어오는 변경과 충돌하지 않도록 구독자의 데이터 변경
   - 대상 테이블의 권한 수정
   - 또는 충돌하는 트랜잭션 건너뛰기

#### 중요 고려사항

- 트랜잭션 건너뛰기 는 충돌하는 작업뿐만 아니라 해당 트랜잭션의 모든 변경 사항을 건너뛰는 것을 포함합니다. 이로 인해 구독자 불일치가 발생할 수 있습니다.
- 구독자의 track_commit_timestamp 설정은 로컬 변경을 유지할지 원격 변경을 채택할지 결정하는 데 도움이 되는 원본 및 커밋 타임스탬프 세부 정보의 로깅을 활성화합니다.
- 병렬 스트리밍 모드 의 경우: 완료 LSN이 기록되지 않을 수 있습니다. 완료 LSN을 얻으려면 스트리밍 모드를 `on` 또는 `off`로 변경하세요.

### 관련 뷰 및 함수

- `pg_stat_subscription_stats`: 충돌 통계 보기
- `pg_replication_origin_status`: 복제 원본의 현재 위치 보기
- `pg_replication_origin_advance()`: 복제 원본 위치 전진
- `ALTER SUBSCRIPTION ... SKIP`: 충돌하는 트랜잭션 건너뛰기
- `ALTER SUBSCRIPTION ... DISABLE`: 구독 비활성화

---

## 29.8. 제한사항 (Restrictions)

이 섹션에서는 PostgreSQL 논리적 복제 기능의 현재 제한 사항과 누락된 기능을 문서화합니다.

### 제한사항 전체 목록

#### 1. 데이터베이스 스키마 및 DDL 명령이 복제되지 않음
- 데이터베이스 스키마와 DDL(Data Definition Language) 명령은 발행자와 구독자 간에 복제되지 않습니다.
- 초기 스키마는 다음을 사용하여 수동으로 복사해야 합니다:
  ```bash
  pg_dump --schema-only
  ```
- 후속 스키마 변경은 수동으로 동기화를 유지해야 합니다.
- 스키마는 양쪽에서 완전히 동일할 필요가 없습니다.
- 발행자에서 스키마가 변경되고 복제된 데이터가 구독자에 도착했지만 테이블 스키마에 맞지 않으면 스키마가 업데이트될 때까지 복제에 오류가 발생합니다.
- 모범 사례: 간헐적인 오류를 방지하기 위해 추가적인 스키마 변경을 구독자에 먼저 적용합니다.

#### 2. 시퀀스 데이터가 복제되지 않음
- 시퀀스 데이터 자체는 복제되지 않습니다.
- 시리얼 또는 ID 컬럼 데이터는 테이블 데이터의 일부로 복제됩니다.
- 구독자의 시퀀스 객체는 여전히 시작 값을 표시합니다.
- 읽기 전용 구독자의 경우: 일반적으로 문제가 되지 않습니다.
- 전환/장애 조치 시나리오의 경우: 다음을 사용하여 시퀀스를 최신 값으로 수동 업데이트해야 합니다:
  - 발행자에서 복사한 데이터 (예: `pg_dump`)
  - 실제 테이블에서 결정된 값

#### 3. TRUNCATE 명령 복제 제한
- `TRUNCATE` 명령은 복제가 지원됩니다.
- 외래 키로 연결된 테이블에 주의해야 합니다.
- 구독자는 발행자와 동일한 테이블 그룹을 잘라냅니다(캐스케이딩 테이블 포함), 구독에 없는 테이블 제외.
- 영향을 받는 모든 테이블이 동일한 구독의 일부인 경우 올바르게 작동합니다.
- 구독자에서 잘린 테이블이 동일한(또는 어떤) 구독에 없는 테이블에 외래 키 링크가 있는 경우 실패합니다.

#### 4. 대형 객체가 복제되지 않음
- 대형 객체(PostgreSQL 문서 33장)는 복제되지 않습니다.
- 해결 방법: 대신 일반 테이블에 데이터를 저장합니다.

#### 5. 테이블만 복제 지원
- 복제는 파티션 테이블을 포함한 테이블에만 지원됩니다.
- 다른 릴레이션 유형은 오류를 생성합니다:
  - 뷰
  - 구체화된 뷰
  - 외부 테이블

#### 6. 파티션 테이블 복제 제약
- 복제는 기본적으로 발행자의 리프 파티션 에서 시작됩니다.
- 파티션은 유효한 대상 테이블로 구독자에 존재해야 합니다.
- 대상 파티션은 다음일 수 있습니다:
  - 리프 파티션 자체
  - 추가로 하위 파티션됨
  - 독립적인 테이블
- 대체 접근 방식: `CREATE PUBLICATION`에서 `publish_via_partition_root` 매개변수를 사용하여 개별 리프 파티션 대신 파티션된 루트 테이블의 ID와 스키마를 사용하여 복제합니다.

#### 7. REPLICA IDENTITY FULL 제한
- 발행된 테이블에서 `REPLICA IDENTITY FULL`을 사용할 때:
  - 기본 B-tree 또는 Hash 연산자 클래스가 없는 데이터 유형(예: `point`, `box`)의 속성을 포함하는 테이블에는 `UPDATE` 및 `DELETE` 작업을 구독자에 적용할 수 없습니다.
  - 해결 방법 : 테이블에 기본 키 또는 복제 식별자 가 정의되어 있는지 확인합니다.

### 요약

이러한 제한으로 인해 PostgreSQL의 논리적 복제는 다음에 가장 적합합니다:
- 테이블 데이터만 복제
- 읽기 전용 복제본 또는 수동 준비가 있는 예정된 전환
- 수동으로 동기화된 스키마
- 복잡한 기하학적 또는 사용자 정의 데이터 유형이 없는 테이블(기본 키/복제 식별자 사용 시 제외)

---

## 29.9. 아키텍처 (Architecture)

논리적 복제는 물리적 스트리밍 복제와 유사한 아키텍처로 구축되었습니다. 두 가지 주요 프로세스로 구현됩니다:

1. walsender 프로세스 - WAL의 논리적 디코딩을 시작하고 표준 논리적 디코딩 출력 플러그인(`pgoutput`)을 로드합니다.
2. apply 프로세스 - 데이터를 로컬 테이블에 매핑하고 올바른 트랜잭션 순서로 수신된 대로 개별 변경 사항을 적용합니다.

### 주요 프로세스 흐름

- `pgoutput` 플러그인은 WAL에서 읽은 변경 사항을 논리적 복제 프로토콜로 변환합니다(54.5절).
- 데이터는 발행 사양에 따라 필터링됩니다.
- 데이터는 스트리밍 복제 프로토콜을 사용하여 apply 워커로 지속적으로 전송됩니다.

### 세션 복제 역할

구독자 데이터베이스의 apply 프로세스는 항상 `session_replication_role`이 `replica`로 설정된 상태로 실행됩니다. 이는 다음을 의미합니다:

- 기본적으로: 트리거와 규칙이 구독자에서 실행되지 않습니다.
- 선택 사항: 사용자는 특정 테이블에서 트리거와 규칙을 활성화할 수 있습니다:
  ```sql
  ALTER TABLE table_name ENABLE TRIGGER trigger_name;
  ALTER TABLE table_name ENABLE RULE rule_name;
  ```

### 트리거 동작

- 논리적 복제 apply 프로세스는 행 트리거만 실행 하며 문 트리거는 실행하지 않습니다.
- 초기 테이블 동기화는 `COPY` 명령처럼 구현되며 `INSERT`에 대해 행 트리거와 문 트리거 모두 실행합니다.

### 29.9.1. 초기 스냅샷

#### 동기화 프로세스

1. 초기 데이터 스냅샷: 기존 테이블 데이터가 스냅샷되고 특별한 apply 프로세스의 병렬 인스턴스에서 복사됩니다.
2. 테이블 동기화 워커: 동기화할 각 테이블에 대해 전용 워커가 생성됩니다.
3. 각 워커:
   - 자체 복제 슬롯을 생성합니다.
   - 기존 데이터를 복사합니다.
   - 복사가 완료되면 데이터가 다른 백엔드에 표시됩니다.

#### 동기화 단계

초기 복사가 완료되면 워커는 동기화 모드 로 진입합니다:
- 테이블이 메인 apply 프로세스와 동기화된 상태가 되도록 합니다.
- 표준 논리적 복제를 사용하여 초기 데이터 복사 중 발생한 모든 변경 사항을 스트리밍합니다.
- 발행자에서 발생한 순서대로 변경 사항을 적용하고 커밋합니다.
- 완료되면 메인 apply 프로세스로 제어를 반환하고 정상적으로 복제를 계속합니다.

### 중요 참고 사항

#### 발행 매개변수

발행의 `publish` 매개변수는 어떤 DML 작업이 복제될지만 영향을 미칩니다 . 초기 데이터 동기화는 기존 테이블 데이터를 복사할 때 이 매개변수를 고려하지 않습니다 .

#### 오류 처리

테이블 동기화 워커가 복사 중 실패하면:
- apply 워커가 실패를 감지합니다.
- 테이블 동기화 워커가 동기화 프로세스를 계속하기 위해 다시 생성됩니다.
- 이 동작은 일시적인 오류가 복제 설정을 영구적으로 방해하지 않도록 합니다.
- 관련 구성: `wal_retrieve_retry_interval`

---

## 29.10. 모니터링 (Monitoring)

논리적 복제 모니터링은 유사한 아키텍처를 공유하기 때문에 물리적 스트리밍 복제 모니터링과 비슷합니다. 발행 노드 모니터링에 대한 정보는 26.2.5.2절 - 모니터링을 참조하세요.

### 구독 모니터링

구독에 대한 모니터링 정보는 `pg_stat_subscription` 뷰를 통해 사용할 수 있으며, 각 구독 워커에 대해 하나의 행이 포함됩니다.

### 주요 사항

- 워커 상태: 구독은 상태에 따라 0개 이상의 활성 구독 워커를 가질 수 있습니다.
- 정상 작동: 일반적으로 활성화된 구독에 대해 단일 apply 프로세스가 실행됩니다.
- 비활성화/충돌된 구독: 뷰에 0개의 행이 있습니다.
- 초기 동기화: 초기 데이터 동기화가 진행 중일 때 동기화 중인 테이블에 대한 추가 워커가 나타납니다.
- 병렬 스트리밍: `streaming` 트랜잭션이 병렬로 적용되는 경우 추가 병렬 apply 워커가 있을 수 있습니다.

---

## 29.11. 보안 (Security)

이 섹션에서는 발행자와 구독자 모두를 다루는 PostgreSQL 논리적 복제의 보안 요구 사항과 권한을 문서화합니다.

### 발행자 보안 요구 사항

#### 복제 역할 속성
- `REPLICATION` 속성이 있어야 합니다(또는 슈퍼유저여야 함).
- `LOGIN` 속성이 있어야 합니다.
- `pg_hba.conf`에서 접근이 구성되어야 합니다.

#### 행 보안 정책
- 역할에 `SUPERUSER` 및 `BYPASSRLS` 가 없으면 발행자 행 보안 정책이 실행될 수 있습니다.
- 행 보안 정책 실행을 방지하려면 연결 문자열에 다음을 포함합니다:
  ```
  options=-crow_security=off
  ```
- 활성화된 경우 테이블 소유자가 행 보안 정책을 추가하면 안전하지 않은 정책을 실행하지 않고 복제가 중단됩니다.

#### 권한 요구 사항
- 초기 테이블 데이터를 복사하려면 발행된 테이블에 대한 `SELECT` 권한이 있어야 합니다(또는 슈퍼유저여야 함).
- 발행을 생성하려면 데이터베이스에 `CREATE` 권한이 필요합니다.

### 발행 권한

#### 발행에 테이블 추가
- 일반 테이블 : 사용자가 테이블에 대한 소유권 이 있어야 합니다.
- 스키마의 모든 테이블 : 사용자가 슈퍼유저 여야 합니다.
- 모든 테이블 또는 스키마의 자동 발행 : 사용자가 슈퍼유저 여야 합니다.

#### 발행 접근 제어
- 현재 발행에 대한 권한이 없습니다.
- 연결할 수 있는 모든 구독은 모든 발행에 접근할 수 있습니다.
- 행 필터, 컬럼 목록 또는 선택적 테이블 포함을 통해 정보를 숨기는 경우 동일한 데이터베이스의 다른 발행이 동일한 정보를 노출할 수 있음을 인식하세요.
- 발행 권한은 향후 PostgreSQL 버전에서 추가될 수 있습니다.

### 구독 보안 요구 사항

#### 생성 권한
- 사용자가 `pg_create_subscription` 역할의 권한을 가져야 합니다.
- 데이터베이스에 `CREATE` 권한이 있어야 합니다.

#### Apply 프로세스 실행

##### 기본 동작 (run_as_owner = false)
- 세션 수준: 구독 소유자의 권한으로 실행
- 작업별: INSERT, UPDATE, DELETE 또는 TRUNCATE 작업에 대해 테이블 소유자로 역할 전환
- 요구 사항: 구독 소유자는 복제된 테이블을 소유한 각 역할로 `SET ROLE`할 수 있어야 합니다.

##### run_as_owner = true인 경우
- 역할 전환이 발생하지 않음
- 모든 작업이 구독 소유자의 권한으로 수행됨
- 권한 요구 사항 감소: 구독 소유자는 대상 테이블에 대한 `SELECT`, `INSERT`, `UPDATE` 및 `DELETE` 권한만 필요
- 보안 위험: 테이블 소유자가 구독 소유자의 권한으로 임의 코드를 실행할 수 있음(예: 트리거를 통해)
- 권장 사항: 데이터베이스 내 사용자 보안이 중요하지 않은 경우가 아니면 피하세요.

### 권한 재확인

#### 발행자
- 권한은 복제 연결 시작 시 한 번 확인됩니다.
- 각 변경 레코드를 읽을 때 재확인되지 않습니다.

#### 구독자
- 구독 소유자의 권한은 적용될 때 각 트랜잭션마다 재확인 됩니다.
- 트랜잭션 적용 중 구독 소유권이 변경되면 현재 트랜잭션은 이전 소유자의 권한으로 계속됩니다.

### 주요 보안 고려사항

1. 행 보안 정책: 모든 테이블 소유자를 신뢰하지 않는 경우 `crow_security=off`를 사용하세요.
2. 발행 가시성: 발행은 인증된 구독자에게 전역적으로 접근 가능합니다.
3. 역할 전환 위험: `run_as_owner = true` 옵션은 트리거를 통해 보안 취약점을 도입할 수 있습니다.
4. 권한 모니터링: 각 트랜잭션에 대한 구독자 권한을 정기적으로 재확인하세요.

---

## 29.12. 구성 설정

논리적 복제에는 여러 구성 옵션을 설정해야 합니다. 이러한 옵션은 복제의 한쪽에서만 관련이 있습니다.

### 29.12.1. 발행자

다음 설정은 발행자 측에서 구성해야 합니다:

#### 필수 설정

1. `wal_level`
   - `logical`로 설정해야 합니다.

2. `max_replication_slots`
   - 연결 예상되는 구독 수 이상으로 설정해야 합니다.
   - 테이블 동기화를 위한 예비 포함해야 합니다.

3. `max_wal_senders`
   - 최소한 `max_replication_slots`와 동일하게 설정해야 합니다.
   - 동시에 연결된 물리적 복제본 수를 더합니다.

#### 영향을 받는 설정

- `idle_replication_slot_timeout` - 논리적 복제 슬롯에 영향
- `wal_sender_timeout` - 논리적 복제 walsender에 영향

### 29.12.2. 구독자

다음 설정은 구독자 측에서 구성해야 합니다:

#### 필수 설정

1. `max_active_replication_origins`
   - 추가할 구독 수 이상으로 설정해야 합니다.
   - 테이블 동기화를 위한 예비 포함해야 합니다.

2. `max_logical_replication_workers`
   - 최소한 구독 수(리더 apply 워커용) 이상으로 설정해야 합니다.
   - 테이블 동기화 워커 및 병렬 apply 워커를 위한 예비를 더합니다.

3. `max_worker_processes`
   - 복제 워커를 수용하기 위해 조정이 필요할 수 있습니다.
   - 최소: `max_logical_replication_workers + 1`
   - 참고: 확장 및 병렬 쿼리도 워커 슬롯을 소비합니다.

4. `max_sync_workers_per_subscription`
   - 구독 초기화 중 초기 데이터 복사의 병렬 처리를 제어합니다.
   - 새 테이블 추가 시 병렬 처리를 제어합니다.

5. `max_parallel_apply_workers_per_subscription`
   - 진행 중인 트랜잭션 스트리밍의 병렬 처리를 제어합니다.
   - 구독 매개변수 `streaming = parallel`과 함께 사용됩니다.

#### 영향을 받는 설정

- `wal_receiver_timeout` - 논리적 복제 워커에 영향
- `wal_receiver_status_interval` - 논리적 복제 워커에 영향
- `wal_retrieve_retry_interval` - 논리적 복제 워커에 영향

---

## 29.13. 업그레이드

논리적 복제 클러스터의 마이그레이션은 이전 논리적 복제 클러스터의 모든 구성원이 버전 17.0 이상인 경우에만 가능합니다.

### 29.13.1. 발행자 업그레이드 준비

#### 주요 사항
- `pg_upgrade`는 논리적 슬롯 마이그레이션을 시도하여 수동 재정의를 방지합니다.
- 버전 17.0 이상의 클러스터에만 지원됩니다.
- 17.0 이전 클러스터의 슬롯은 자동으로 무시됩니다.

#### 업그레이드 전 필수 조건

1. 구독자에서 구독 비활성화:
   ```sql
   ALTER SUBSCRIPTION ... DISABLE;
   ```

2. 새 클러스터의 구성 요구 사항:
   - `wal_level = logical`
   - `max_replication_slots >=` 이전 클러스터의 슬롯 수

3. 플러그인 요구 사항:
   - 슬롯에서 참조하는 출력 플러그인이 새 PostgreSQL 실행 파일 디렉토리에 설치되어야 합니다.

4. 복제 상태:
   - 이전 클러스터가 모든 트랜잭션과 논리적 디코딩 메시지를 구독자에 복제했어야 합니다.
   - 모든 슬롯이 사용 가능해야 합니다(`pg_replication_slots.conflicting = true`인 충돌 슬롯 없음).
   - 새 클러스터에 영구 논리적 슬롯이 없어야 합니다(`pg_replication_slots.temporary = false`인 슬롯 없음).

### 29.13.2. 구독자 업그레이드 준비

#### 구독자 구성
업그레이드 전에 새 구독자에서 구독자 구성을 설정합니다.

#### pg_upgrade가 마이그레이션하는 것
- `pg_subscription_rel` 시스템 카탈로그의 구독 종속성
- 구독의 테이블 정보
- 구독의 복제 원본(이전 구독자가 중단한 위치에서 복제를 계속할 수 있음)

참고: 버전 17.0 이상의 클러스터에만 마이그레이션이 지원됩니다.

#### 필수 조건

1. 구독 테이블 상태 가 다음 중 하나여야 합니다:
   - `i` (초기화) 또는 `r` (준비됨)
   - 확인: `pg_subscription_rel.srsubstate`

2. 각 구독에 대한 복제 원본 항목 이 존재해야 합니다:
   - `pg_subscription` 및 `pg_replication_origin` 테이블 모두 확인

3. 구성 요구 사항:
   - `max_active_replication_origins >=` 이전 클러스터의 구독 수

### 29.13.3. 논리적 복제 클러스터 업그레이드

#### 중요 참고 사항

- 쓰기 작업 허용됨: 구독자를 업그레이드하는 동안 발행자에서 쓰기 작업을 수행할 수 있습니다. 변경 사항은 구독자 업그레이드가 완료되면 복제됩니다.

- 제한 적용: 논리적 복제 제한이 클러스터 업그레이드에 적용됩니다(29.8절 참조).

- 필수 조건 적용: 발행자와 구독자 업그레이드 필수 조건이 모두 적용됩니다.

> 경고 : 논리적 복제 클러스터 업그레이드에는 다양한 노드에 걸쳐 여러 단계가 필요합니다. 모든 작업이 트랜잭션이 아닙니다. 항상 25.3.2절에 설명된 대로 백업을 수행하세요.

#### 29.13.3.1. 2노드 논리적 복제 클러스터 업그레이드 단계

설정: `node1`에 발행자, `node2`에 구독자, 구독 `sub1_node1_node2`

##### 전체 업그레이드 단계

1. node2에서 구독 비활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 DISABLE;
   ```

2. 발행자(node1) 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1 stop
   ```

3. 업그레이드된 인스턴스 초기화:
   ```bash
   (새 버전으로 data1_upgraded 초기화)
   ```

4. pg_upgrade로 발행자 업그레이드:
   ```bash
   pg_upgrade \
       --old-datadir "/opt/PostgreSQL/postgres/17/data1" \
       --new-datadir "/opt/PostgreSQL/postgres/18/data1_upgraded" \
       --old-bindir "/opt/PostgreSQL/postgres/17/bin" \
       --new-bindir "/opt/PostgreSQL/postgres/18/bin"
   ```

5. 업그레이드된 발행자 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1_upgraded start -l logfile
   ```

6. 구독자(node2) 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data2 stop
   ```

7. 업그레이드된 구독자 인스턴스 초기화:
   ```bash
   (새 버전으로 data2_upgraded 초기화)
   ```

8. pg_upgrade로 구독자 업그레이드:
   ```bash
   pg_upgrade \
       --old-datadir "/opt/PostgreSQL/postgres/17/data2" \
       --new-datadir "/opt/PostgreSQL/postgres/18/data2_upgraded" \
       --old-bindir "/opt/PostgreSQL/postgres/17/bin" \
       --new-bindir "/opt/PostgreSQL/postgres/18/bin"
   ```

9. 업그레이드된 구독자 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data2_upgraded start -l logfile
   ```

10. node2에 누락된 테이블 생성 (다운타임 중 node1에서 생성된 것):
    ```sql
    /* node2 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

11. node2에서 구독 활성화:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 ENABLE;
    ```

12. 구독 발행 새로고침:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 REFRESH PUBLICATION;
    ```

참고: 발행자를 먼저 업그레이드하거나 구독자를 먼저 업그레이드할 수 있습니다. 역순으로도 유사한 단계가 적용됩니다.

#### 29.13.3.2. 캐스케이드 논리적 복제 클러스터 업그레이드 단계

설정: `node1` -> `node2` -> `node3` (캐스케이드 체인)
- `node2`는 `node1`을 구독 (구독: `sub1_node1_node2`)
- `node3`는 `node2`를 구독 (구독: `sub1_node2_node3`)

##### 업그레이드 순서 (20단계)

1. node2의 node1 구독 비활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 DISABLE;
   ```

2. node1 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1 stop
   ```

3. `data1_upgraded` 초기화

4. pg_upgrade로 node1 업그레이드

5. 업그레이드된 node1 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1_upgraded start -l logfile
   ```

6. node3의 node2 구독 비활성화:
   ```sql
   /* node3 # */ ALTER SUBSCRIPTION sub1_node2_node3 DISABLE;
   ```

7. node2 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data2 stop
   ```

8. `data2_upgraded` 초기화

9. pg_upgrade로 node2 업그레이드

10. 업그레이드된 node2 시작:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data2_upgraded start -l logfile
    ```

11. node2에 누락된 테이블 생성:
    ```sql
    /* node2 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

12. node2 구독 활성화:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 ENABLE;
    ```

13. node2 구독 새로고침:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 REFRESH PUBLICATION;
    ```

14. node3 중지:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data3 stop
    ```

15. `data3_upgraded` 초기화

16. pg_upgrade로 node3 업그레이드

17. 업그레이드된 node3 시작:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data3_upgraded start -l logfile
    ```

18. node3에 누락된 테이블 생성:
    ```sql
    /* node3 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

19. node3 구독 활성화:
    ```sql
    /* node3 # */ ALTER SUBSCRIPTION sub1_node2_node3 ENABLE;
    ```

20. node3 구독 새로고침:
    ```sql
    /* node3 # */ ALTER SUBSCRIPTION sub1_node2_node3 REFRESH PUBLICATION;
    ```

#### 29.13.3.3. 2노드 순환 논리적 복제 클러스터 업그레이드 단계

설정: 양방향 복제 `node1` <-> `node2`
- `node1`은 구독 `sub1_node2_node1`을 가짐 (node2에서)
- `node2`는 구독 `sub1_node1_node2`를 가짐 (node1에서)

##### 업그레이드 순서 (16단계)

1. node2의 node1 구독 비활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 DISABLE;
   ```

2. node1 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1 stop
   ```

3. `data1_upgraded` 초기화

4. pg_upgrade로 node1 업그레이드

5. 업그레이드된 node1 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1_upgraded start -l logfile
   ```

6. node2 구독 활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 ENABLE;
   ```

7. node1에 누락된 테이블 생성:
   ```sql
   /* node1 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
   ```

8. 데이터 복사를 위한 node1 구독 새로고침:
   ```sql
   /* node1 # */ ALTER SUBSCRIPTION sub1_node2_node1 REFRESH PUBLICATION;
   ```

9. node1의 node2 구독 비활성화:
   ```sql
   /* node1 # */ ALTER SUBSCRIPTION sub1_node2_node1 DISABLE;
   ```

10. node2 중지:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data2 stop
    ```

11. `data2_upgraded` 초기화

12. pg_upgrade로 node2 업그레이드

13. 업그레이드된 node2 시작:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data2_upgraded start -l logfile
    ```

14. node1 구독 활성화:
    ```sql
    /* node1 # */ ALTER SUBSCRIPTION sub1_node2_node1 ENABLE;
    ```

15. node2에 누락된 테이블 생성:
    ```sql
    /* node2 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

16. 데이터 복사를 위한 node2 구독 새로고침:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 REFRESH PUBLICATION;
    ```

### 주요 요점

- 버전 요구 사항: 모든 노드가 17.0 이상이어야 합니다.
- 백업 중요: 업그레이드 전 항상 백업하세요.
- 구독 관리: 업그레이드 전 비활성화, 후 활성화
- 테이블 동기화: 다운타임 중 생성된 테이블을 수동으로 생성
- 발행 새로고침: 상태 동기화를 위해 항상 새로고침
- 순서 중요: 지정된 노드 업그레이드 순서를 따르세요.

---

## 29.14. 빠른 설정

### 단계 1: postgresql.conf 구성

발행자 데이터베이스에서 다음 구성 옵션을 설정합니다:

```
wal_level = logical
```

참고: 다른 필수 설정은 기본 설정에 충분한 기본값이 있습니다.

### 단계 2: pg_hba.conf 구성

복제 연결을 허용하도록 `pg_hba.conf`를 조정합니다(네트워크 구성 및 사용자에 따라 값 조정):

```
host     all     repuser     0.0.0.0/0     md5
```

### 단계 3: 발행 생성 (발행자 데이터베이스에서)

```sql
CREATE PUBLICATION mypub FOR TABLE users, departments;
```

이렇게 하면 `users` 및 `departments` 테이블을 포함하는 `mypub`라는 발행이 생성됩니다.

### 단계 4: 구독 생성 (구독자 데이터베이스에서)

```sql
CREATE SUBSCRIPTION mysub CONNECTION 'dbname=foo host=bar user=repuser' PUBLICATION mypub;
```

이렇게 하면 다음을 수행하는 `mysub`라는 구독이 생성됩니다:
- 지정된 연결 문자열을 사용하여 발행자에 연결
- `mypub` 발행을 구독

### 결과

구독이 생성되면 복제 프로세스가 자동으로:
1. `users` 및 `departments`의 초기 테이블 내용을 동기화 합니다.
2. 해당 테이블에 대한 증분 변경 사항을 복제하기 시작 합니다.

이것은 PostgreSQL에서 기본 논리적 복제에 필요한 최소 구성을 나타냅니다.

---

## 참고 자료

- [PostgreSQL 18 공식 문서 - Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [CREATE PUBLICATION](https://www.postgresql.org/docs/current/sql-createpublication.html)
- [CREATE SUBSCRIPTION](https://www.postgresql.org/docs/current/sql-createsubscription.html)
- [ALTER SUBSCRIPTION](https://www.postgresql.org/docs/current/sql-altersubscription.html)
