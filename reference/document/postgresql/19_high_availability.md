# Chapter 26. 고가용성, 로드 밸런싱 및 복제

PostgreSQL 18 공식 문서 - High Availability, Load Balancing, and Replication 번역

---

## 개요

데이터베이스 서버는 함께 작동하여 어떤 서버가 실패하더라도 다른 서버가 작업을 인계받을 수 있도록 구성할 수 있으며, 이를 통해 여러 서버가 동일한 데이터를 제공할 수 있습니다. 또한 로드 밸런싱을 통해 여러 서버가 동일한 데이터를 제공하도록 할 수 있습니다. 이상적으로는 데이터베이스 서버가 원활하게 협력할 수 있어야 합니다. 정적 웹 페이지를 제공하는 웹 서버는 각 웹 서버에 웹 페이지를 한 번만 배치하면 되기 때문에 아주 쉽게 결합할 수 있습니다. 사실, 읽기 전용 데이터베이스 서버도 쉽게 결합할 수 있습니다. 불행히도, 대부분의 데이터베이스 서버는 읽기/쓰기 요청이 혼합되어 있으며, 읽기/쓰기 서버는 쓰기가 다른 서버로 전달되어 일관성을 유지할 수 있도록 조정하기가 훨씬 어렵습니다.

이 동기화 문제는 데이터베이스 서버 협력의 근본적인 어려움입니다. 모든 사용 사례에서 쓰기 동기화 문제를 해결하는 단일 솔루션은 없기 때문에 다양한 솔루션이 존재합니다. 각 솔루션은 특정 워크로드에 적합한 방식으로 이 문제를 해결하며, 모든 솔루션은 특정 영역에서 성능 저하를 최소화합니다.

### 주요 용어

- Primary/Master: 데이터를 수정할 수 있는 읽기/쓰기 서버
- Standby/Secondary: Primary의 변경 사항을 추적하는 서버
  - Warm Standby: 승격(Promoted)될 때까지 연결 불가
  - Hot Standby: 읽기 전용 쿼리 수용 가능

### 동기화 방식

- 동기(Synchronous): 모든 서버가 커밋할 때까지 대기 - 데이터 손실 없음
- 비동기(Asynchronous): 지연 허용 - 성능 우수하나 데이터 손실 가능

일부 솔루션은 서버 간 통신이 필요한 동기식 솔루션을 사용하는 반면, 다른 솔루션은 비동기식으로 서버 간 동기화가 느슨하게 연결되어 서버 간 자연스러운 지연이 발생합니다.

---

## 26.1. 다양한 솔루션 비교

이 섹션에서는 PostgreSQL의 고가용성, 로드 밸런싱 및 복제를 위한 다양한 솔루션을 비교합니다.

### 1. 공유 디스크 장애조치 (Shared Disk Failover)

공유 디스크 장애조치는 여러 서버가 공유하는 단일 디스크 배열을 사용하여 단일 복사본만 유지함으로써 동기화 오버헤드를 피합니다. 주 데이터베이스 서버가 실패하면 대기 서버가 마치 데이터베이스 충돌 복구 후처럼 데이터베이스를 마운트하고 시작할 수 있습니다. 이를 통해 빠른 장애조치가 가능하고 데이터 손실이 없습니다.

특징:
- 데이터베이스 사본 1개만 유지
- 주 서버 실패 시 대기 서버가 디스크 마운트
- 빠른 장애조치, 데이터 손실 없음

단점:
- 공유 디스크 배열 장애 시 모두 영향
- 대기 서버가 공유 스토리지에 동시에 접근할 수 없음

공유 하드웨어 기능은 네트워크 스토리지 장치에서 일반적입니다. 한 가지 주의할 점은 대기 서버가 공유 스토리지에 접근해서는 안 된다는 것입니다. 그렇지 않으면 데이터가 손상될 수 있습니다.

### 2. 파일 시스템(블록 장치) 복제 (File System/Block Device Replication)

공유 하드웨어 기능의 대안은 파일 시스템 복제를 사용하는 것입니다. 여기서 모든 파일 시스템 변경 사항이 다른 컴퓨터에 미러링됩니다. 유일한 제한 사항은 대기 서버에서 주 서버와 동일한 순서로 미러링이 이루어져야 한다는 것입니다.

특징:
- 파일 시스템 변경사항을 다른 컴퓨터에 미러링
- 미러링 순서 일치 필수

예시:
- DRBD (Linux용 파일 시스템 복제 솔루션)

### 3. Write-Ahead Log 배송 (Write-Ahead Log Shipping)

웜 및 핫 대기 서버는 WAL(Write-Ahead Log) 레코드 스트림을 읽어 대기 서버를 최신 상태로 유지할 수 있습니다. 주 서버가 실패하면 대기 서버에는 주 서버의 거의 모든 데이터가 포함되며 새로운 주 서버가 될 수 있습니다. 이것은 동기식 또는 비동기식으로 수행될 수 있으며 전체 데이터베이스 서버에 대해서만 수행될 수 있습니다.

방식:
- WAL 레코드 스트림 읽기로 대기 서버 유지
- 동기/비동기 모두 가능
- 전체 데이터베이스에만 적용 가능

구현:
- 파일 기반 로그 배송 (26.2절)
- 스트리밍 복제 (26.2.5절)
- 두 가지 조합 사용 가능

대기 서버는 파일 기반 로그 배송(26.2절), 스트리밍 복제(26.2.5절) 또는 두 가지의 조합을 사용하여 구현할 수 있습니다.

핫 대기에 대한 정보는 26.4절을 참조하십시오.

### 4. 논리적 복제 (Logical Replication)

논리적 복제를 사용하면 데이터베이스 서버가 데이터 변경 사항의 논리적 스트림을 다른 서버로 보낼 수 있습니다. PostgreSQL 논리적 복제는 WAL에서 논리적 데이터 수정 스트림을 구성합니다. 논리적 복제는 테이블 단위로 데이터를 복제할 수 있습니다. 또한 변경 사항을 보내는 서버(발행 서버)가 다른 서버의 변경 사항을 구독할 수도 있어 양방향 복제가 가능합니다.

특징:
- 테이블별 단위로 복제 가능
- WAL에서 논리적 데이터 수정 스트림 구성
- 양방향 복제 가능 (발행 서버가 다른 서버의 변경사항 구독 가능)
- Logical decoding 인터페이스(47장)로 타사 확장 지원

### 5. 트리거 기반 Primary-Standby 복제 (Trigger-Based Primary-Standby Replication)

트리거 기반 복제 설정에서는 일반적으로 주 서버에서 하나의 지정된 대기 서버로 모든 데이터 수정을 비동기적으로 전송합니다. 대기 서버는 쿼리에 응답할 수 있으며 로컬에서 데이터를 변경할 수 있습니다(예: 분석 데이터). 이는 대규모 분석 또는 데이터 웨어하우스 쿼리를 분산하는 데 유용합니다.

특징:
- 테이블 단위 복제
- 비동기 업데이트
- 대기 서버가 쿼리 응답 가능
- 로컬 데이터 변경 허용

예시:
- Slony-I

단점:
- 장애조치 시 데이터 손실 가능

### 6. SQL 기반 복제 미들웨어 (SQL-Based Replication Middleware)

SQL 기반 복제 미들웨어 프로그램을 사용하면 프로그램이 모든 SQL 쿼리를 가로채서 하나 또는 모든 서버로 보낼 수 있습니다. 각 서버는 독립적으로 작동합니다. 읽기-쓰기 쿼리는 모든 서버로 전송되어 모든 서버에서 변경 사항이 발생합니다. 그러나 읽기 전용 쿼리는 하나의 서버에만 전송되어 읽기 작업 부하가 분산될 수 있습니다.

동작:
- 모든 SQL 쿼리 인터셉트 후 서버로 전송
- 읽기-쓰기 쿼리: 모든 서버 전송
- 읽기 전용 쿼리: 1개 서버만 전송 가능

주의사항:
- `random()`, `CURRENT_TIMESTAMP`, 시퀀스 등 비결정적 함수값 일관성 문제

예시:
- Pgpool-II
- Continuent Tungsten

해결방안:
- 미들웨어/애플리케이션에서 단일 소스 결정
- 2단계 커밋(Two-Phase Commit) 사용

### 7. 비동기 다중 마스터 복제 (Asynchronous Multimaster Replication)

비동기 다중 마스터 복제는 각 서버가 독립적으로 작동하고 충돌을 해결하기 위해 주기적으로 통신하는 방식입니다. 이는 불규칙한 연결이나 느린 통신 링크(예: 노트북이나 원격 서버)가 있는 환경에 이상적입니다.

특징:
- 불규칙한 연결, 느린 통신 링크 환경에 적합 (노트북, 원격 서버)
- 각 서버 독립 작동
- 주기적 동기화로 충돌 식별
- 충돌 해결 유연성

예시:
- Bucardo

### 8. 동기 다중 마스터 복제 (Synchronous Multimaster Replication)

동기 다중 마스터 복제에서는 각 서버가 쓰기 요청을 수용할 수 있으며, 수정된 데이터는 트랜잭션이 커밋되기 전에 각 서버에서 모든 서버로 전송됩니다. 과도한 쓰기 활동은 트랜잭션이 어디서든 커밋될 수 있도록 서버 전체에 잠금이 필요하기 때문에 과도한 잠금과 커밋 지연을 유발할 수 있습니다. 사실상 쓰기 성능은 종종 단일 서버보다 나쁩니다. 읽기 요청은 모든 서버로 분산될 수 있어 읽기 집약적인 워크로드에 유리합니다.

특징:
- 모든 서버가 쓰기 요청 수용
- 트랜잭션 커밋 전 모든 서버에 전송
- 비결정적 함수 문제 없음

단점:
- 과도한 잠금
- 커밋 지연
- 성능 저하

최적 사용 사례:
- 대부분 읽기 작업인 워크로드

PostgreSQL:
- PostgreSQL은 이 유형의 복제를 제공하지 않음
- 2단계 커밋(PREPARE TRANSACTION 및 COMMIT PREPARED)으로 구현 가능하지만 애플리케이션 수준 코드에서 원자적 커밋 관리 필요

### 고가용성, 로드 밸런싱 및 복제 기능 매트릭스 (Table 26.1)

| 기능 | 공유 디스크 | 파일 시스템 복제 | WAL 로그 배송 | 논리적 복제 | 트리거 기반 | SQL 복제 | 비동기 MM | 동기 MM |
|------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| 인기 예시 | NAS | DRBD | 내장 스트리밍 | 내장 논리적, pglogical | Londiste, Slony | pgpool-II | Bucardo | - |
| 특수 하드웨어 불필요 | - | O | O | O | O | O | O | O |
| 여러 주 서버 허용 | - | - | - | O | - | O | O | O |
| 주 서버 오버헤드 없음 | O | - | O | O | - | O | - | - |
| 다중 서버 대기 없음 | O | - | sync off | sync off | O | - | O | - |
| 주 서버 장애 시 데이터 손실 없음 | O | O | sync on | sync on | - | O | - | O |
| 복제본 읽기 전용 쿼리 | - | - | hot standby | O | O | O | O | O |
| 테이블 단위 세분성 | - | - | - | O | O | - | O | O |
| 충돌 해결 불필요 | O | O | O | - | O | O | - | O |

### 범주 외 솔루션

#### 데이터 파티셔닝 (Data Partitioning)

데이터 파티셔닝은 테이블을 데이터셋으로 분할합니다. 각 세트는 하나의 서버만 수정할 수 있습니다. 예를 들어, 런던 사무실과 파리 사무실에 서버가 있고 각 사무실이 자체 판매 데이터만 수정할 수 있으며 데이터 상호 조회가 가능한 구성이 있습니다. 이러한 설정은 애플리케이션 수준 코드로 쉽게 구현할 수 있으며, 이러한 분할 기능은 PL/Proxy 도구셋을 참조하십시오.

#### 다중 서버 병렬 쿼리 실행 (Multiple-Server Parallel Query Execution)

위의 많은 솔루션은 여러 서버가 여러 쿼리를 처리할 수 있도록 합니다. 그러나 어떤 솔루션도 단일 쿼리가 여러 서버를 사용하여 더 빠르게 완료될 수 있도록 허용하지 않습니다. 이 솔루션은 여러 서버에 데이터를 분할하고 각 서버가 쿼리의 일부를 실행한 후 결과를 중앙 서버로 반환하여 사용자에게 전달하도록 합니다. 이는 PL/Proxy 도구셋으로 구현할 수 있습니다.

PostgreSQL 오픈 소스의 특성으로 인해 여러 상용 솔루션이 존재하며, 독자적인 장애조치, 복제, 로드 밸런싱 기능을 제공합니다.

---

## 26.2. 로그 배송 대기 서버 (Log-Shipping Standby Servers)

연속 아카이빙(Continuous Archiving)을 사용하여 하나 이상의 대기 서버가 있는 고가용성(HA) 클러스터 구성을 만들 수 있습니다. 이를 웜 대기(Warm Standby) 또는 로그 배송(Log Shipping) 이라고 합니다.

주 서버와 대기 서버가 함께 작동하여 이 높은 수준의 기능을 제공하지만, 서버는 느슨하게 결합되어 있습니다. 주 서버는 연속 아카이빙 모드에서 작동하는 반면, 각 대기 서버는 주 서버에서 WAL 파일을 읽을 수 있는 연속 복구 모드에서 작동합니다. 데이터베이스 테이블 변경이 필요하지 않으므로 이 구성은 다른 복제 솔루션에 비해 관리 오버헤드가 낮습니다. 또한 이 구성은 주 서버의 성능에 상대적으로 적은 영향을 미칩니다.

주 서버에서 대기 서버로 직접 WAL 레코드를 이동하는 것을 일반적으로 로그 배송이라고 합니다. PostgreSQL은 WAL 레코드가 한 번에 하나의 파일(WAL 세그먼트)을 전송하는 파일 기반 로그 배송을 구현합니다. WAL 파일(16MB)은 로컬 또는 원격으로 쉽고 효율적으로 배송할 수 있습니다(예: scp, rsync, ftp). 스트리밍 복제를 사용하면 레코드 단위 로그 배송도 가능합니다.

로그 배송은 비동기식이므로, 즉 WAL 레코드가 트랜잭션 커밋 후에 배송됩니다. 결과적으로 주 서버가 심각한 장애를 겪을 경우 대기 서버에 복제되지 않은 커밋된 트랜잭션으로 인해 데이터 손실이 발생할 수 있는 창이 있습니다. 파일 기반 로그 배송에서 이 데이터 손실 창의 크기는 archive_timeout을 사용하여 제한할 수 있으며, 이 값은 몇 초로 설정할 수 있습니다. 그러나 주 서버의 네트워크 대역폭이 크게 증가할 수 있으므로 이러한 낮은 설정은 비용이 됩니다. 스트리밍 복제(26.2.5절 참조)는 훨씬 작은 데이터 손실 창을 제공합니다.

복구 성능은 충분히 좋아서 대기 서버는 일반적으로 활성화되면 완전한 작동까지 잠시만 걸립니다. 결과적으로 이것은 높은 가용성을 제공하는 웜 대기 구성이라고 합니다. 아카이브된 베이스 백업에서 서버를 복원하고 롤포워드하는 것은 아마도 오래 걸리므로 이 기술은 높은 가용성이 아닌 재해 복구를 위한 솔루션만 제공합니다. 대기 서버는 재해 복구, 데이터 보호 또는 둘 다에 사용될 수 있습니다.

### 26.2.1. 계획 (Planning)

일반적으로 주 서버와 대기 서버를 가능한 한 비슷하게 설계하는 것이 현명합니다. 특히 테이블스페이스와 관련된 경로 이름은 동일한 경로를 통해 전달되어야 합니다. 따라서 주 서버에서 CREATE TABLESPACE를 실행하면 대기 서버의 복구 시작 전에 필요한 새 마운트 포인트가 생성되어 있어야 합니다. 주 서버에서 다른 테이블스페이스 레이아웃을 사용하려면 복구 중에 tablespace_map 파일을 수정하거나 테이블스페이스를 사용하지 않고 대기 서버의 테이블스페이스 레이아웃을 조정할 수 있습니다.

하드웨어가 꼭 동일할 필요는 없지만, 경험상 아키텍처 간의 차이를 마이그레이션하는 것은 지원되지 않습니다. 따라서 32비트에서 64비트로 또는 그 반대로 이동하는 것은 작동하지 않습니다. 동일한 메이저 버전에서 다른 마이너 버전을 실행하는 것은 일반적으로 작동하지만 공식적으로 지원되지 않습니다. 릴리스 노트를 주의 깊게 읽으십시오.

PostgreSQL 버전 호환성:
- 다른 메이저 버전 간 로그 배송은 불가능
- 다른 마이너 버전 간 가능 (공식 지원 없음)
- 권장: 주 서버와 대기 서버를 동일 버전으로 유지
- 업그레이드 전략: 대기 서버를 먼저 업그레이드

### 26.2.2. 대기 서버 운영 (Standby Server Operation)

대기 모드에서 서버는 아카이브된 위치에서 WAL을 지속적으로 적용하거나 기본 연결을 통해 직접 적용합니다.

대기 서버 시작 시 `standby.signal` 파일이 데이터 디렉토리에 존재하면 서버는 대기 모드로 진입합니다. 대기 서버에서 `restore_command`를 설정해야 하며, 대기 서버에서 `archive_cleanup_command`를 사용하여 더 이상 필요하지 않은 WAL 세그먼트 파일을 제거할 수 있습니다. `pg_archivecleanup` 유틸리티는 이 목적으로 설계되었으므로 일반적인 단일 대기 구성에서 `archive_cleanup_command`로 사용할 수 있습니다.

#### WAL 복구 순서 (우선순위)

1. `restore_command`를 통한 WAL 아카이브 복구
2. `pg_wal` 디렉토리의 WAL 복구
3. 스트리밍 복제를 통한 주 서버 연결
4. 위의 모든 방법 재시도 (루프)

대기 모드는 `pg_ctl stop`을 사용하여 종료되거나 서버가 장애조치를 요청받을 때까지 종료됩니다. 대기 모드 종료 전에 아카이브 또는 `pg_wal`에서 사용 가능한 즉시 재생할 WAL이 완전히 재생되지만, 주 서버에 다시 연결하려는 시도는 없습니다.

#### 대기 서버 승격 (Promotion)

```bash
# 대기 모드 종료 및 일반 모드로 변환:
pg_ctl promote

# 또는 SQL 함수:
SELECT pg_promote();
```

### 26.2.3. 대기 서버를 위한 주 서버 준비 (Preparing the Primary for Standby Servers)

연속 아카이빙을 Section 25.3.1에 설명된 대로 주 서버에 설정합니다. 아카이브 디렉토리는 주 서버가 다운되어도 대기 서버에서 접근할 수 있어야 합니다. 즉, 주 서버가 아닌 대기 서버 자체 또는 다른 신뢰할 수 있는 서버에 있어야 합니다.

대기 서버에 연결하도록 스트리밍 복제를 설정하려면(Section 26.2.5 참조), `max_wal_senders`를 필요한 대기 서버 수를 지원하기에 충분한 값으로 설정합니다. 복제 슬롯을 사용하려면 적어도 슬롯 수만큼 `max_replication_slots`가 설정되어 있는지 확인합니다.

#### 주 서버 설정 예제

```ini
# postgresql.conf (Primary)

# 복제 연결을 위한 설정
max_wal_senders = 10  # 대기 서버 수 이상으로 설정
max_replication_slots = 10  # 복제 슬롯 사용 시 설정
```

```
# pg_hba.conf (Primary)
# TYPE  DATABASE        USER            ADDRESS            METHOD
host    replication     foo             192.168.1.100/32   md5
```

### 26.2.4. 대기 서버 설정 (Setting Up a Standby Server)

대기 서버를 설정하려면 주 서버에서 베이스 백업을 복원합니다(Section 25.3.5 참조). 대기 서버의 클러스터 데이터 디렉토리에 `standby.signal`이라는 파일을 만들고 `restore_command`를 설정하여 아카이브된 WAL을 검색하는 간단한 명령으로 설정합니다.

#### 1. 베이스 백업 복구

```bash
# 주 서버에서 생성한 베이스 백업을 대기 서버로 복구
# (Section 25.3.5 참조)
```

#### 2. standby.signal 파일 생성

```bash
# 대기 서버의 클러스터 데이터 디렉토리에 생성
touch /path/to/data/directory/standby.signal
```

#### 3. 기본 설정

```ini
# postgresql.conf (Standby)

# WAL 복구 명령
restore_command = 'cp /path/to/archive/%f %p'

# 스트리밍 복제 설정 (선택사항)
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass options=''-c wal_sender_timeout=5000'''

# 타임라인 설정 (다중 대기 HA의 경우)
recovery_target_timeline = 'latest'  # 기본값

# WAL 아카이브 정리 (선택사항)
archive_cleanup_command = 'pg_archivecleanup /path/to/archive %r'
```

#### 4. 완전 설정 예제

```ini
# 대기 서버의 postgresql.conf
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass options=''-c wal_sender_timeout=5000'''
restore_command = 'cp /path/to/archive/%f %p'
archive_cleanup_command = 'pg_archivecleanup /path/to/archive %r'
```

#### 5. HA용 추가 설정

대기 서버가 장애조치 후 새로운 주 서버가 되는 경우, 주 서버처럼 WAL 아카이빙, 연결, 인증 설정이 필요합니다.

### 26.2.5. 스트리밍 복제 (Streaming Replication)

스트리밍 복제를 사용하면 대기 서버가 파일 기반 로그 배송을 사용할 때보다 더 최신 상태를 유지할 수 있습니다. 대기 서버는 주 서버에 연결하고, 주 서버는 생성되는 즉시 WAL 레코드를 대기 서버로 스트리밍하여 WAL 파일이 채워지기를 기다리지 않습니다.

스트리밍 복제는 기본적으로 비동기식(Section 26.2.8 참조)이며, 주 서버에서 트랜잭션이 커밋된 후 대기 서버에 변경 사항이 나타나기 전에 작은 지연이 있습니다. 그러나 이 지연은 파일 기반 로그 배송보다 훨씬 작으며, 일반적으로 대기 서버가 로드를 유지할 수 있다면 1초 미만입니다. 스트리밍 복제에서 `archive_timeout`은 데이터 손실 창 크기를 줄이는 데 필요하지 않습니다.

#### 스트리밍 복제 특징

- 대기 서버가 주 서버에 연결
- 주 서버는 생성되는 즉시 WAL 레코드 스트림
- WAL 파일이 완성될 때까지 대기하지 않음
- 기본값: 비동기 모드
- 지연 시간: 일반적으로 1초 이내 (대기 서버가 부하를 감당할 수 있을 때)

#### 필수 설정 단계

```ini
# 1. 파일 기반 로그 배송 대기 서버를 먼저 설정 (26.2.4 참조)

# 2. primary_conninfo 설정으로 스트리밍 복제 활성화
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass'

# 3. 주 서버에서 listen_addresses 설정
listen_addresses = '*'  # 또는 대기 서버 IP

# 4. 주 서버에서 max_wal_senders 충분히 설정
max_wal_senders = 10
```

스트리밍 복제만 사용하고(WAL 아카이브 없이) 대기 서버가 다시 연결할 때까지 어떤 WAL 세그먼트가 필요할지 알 수 없습니다. `wal_keep_size`를 지정하면 이전 WAL 세그먼트가 너무 빨리 재활용되지 않도록 합니다. 또는 복제 슬롯(Section 26.2.6)을 설정하여 필요한 모든 세그먼트가 유지되도록 할 수 있습니다.

#### WAL 세그먼트 재활용 관리

```ini
# 옵션 1: wal_keep_size 사용
wal_keep_size = 1GB

# 옵션 2: 복제 슬롯 사용 (권장)
# 26.2.6 참조

# 옵션 3: 아카이브 사용
archive_command = '...'
```

#### 대기 서버 연결 확인

대기 서버 시작 후:
- 대기 서버에서 `walreceiver` 프로세스 확인
- 주 서버에서 `walsender` 프로세스 확인

#### 26.2.5.1. 인증 (Authentication)

클라이언트 인증을 위해 `replication` 권한을 설정하여 대기 서버가 `walsender`에 연결할 수 있도록 하는 것이 매우 중요합니다. `pg_hba.conf`에서 `replication`을 데이터베이스 필드로 사용하여 이 액세스를 설정합니다. 일반 유저가 `walsender`에 연결하는 것을 방지하고 싶다면 `REPLICATION` 권한을 가진 유저를 만들고 그 유저만 허용하는 것이 좋습니다.

```
# pg_hba.conf (Primary)
# TYPE  DATABASE        USER            ADDRESS                 METHOD
host    replication     foo             192.168.1.100/32        md5
```

예제 설명:
- 대기 서버 IP: 192.168.1.100
- 복제 계정: foo
- 인증 방식: md5

```ini
# postgresql.conf (Standby)
# 주 서버 연결 정보
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass'
```

#### ~/.pgpass 파일 사용

```
# ~/.pgpass (Standby)
192.168.1.50:5432:replication:foo:foopass
```

```bash
chmod 600 ~/.pgpass
```

#### 복제 사용자 생성

```sql
-- 주 서버에서 복제 사용자 생성
CREATE ROLE foo WITH LOGIN REPLICATION PASSWORD 'foopass';
```

#### 권한 설명

| 권한 | 설명 |
|------|------|
| REPLICATION | 매우 높은 권한, 데이터 수정 불가 |
| SUPERUSER | REPLICATION보다 높음, 데이터 수정 가능 |
| LOGIN | 로그인 권한 |

#### 26.2.5.2. 모니터링 (Monitoring)

스트리밍 복제의 중요한 측면은 주 서버에서 복제 지연을 모니터링하는 것입니다. 이것은 대기 서버가 주 서버를 따라잡는 데 걸리는 시간이며, 대기 서버가 과부하되면 증가할 수 있습니다.

##### 지연 시간 계산

```sql
-- 주 서버에서:
SELECT pg_current_wal_lsn();

-- 대기 서버에서:
SELECT pg_last_wal_receive_lsn();

-- 지연 시간 = 주 서버의 WAL LSN - 대기 서버의 Receive LSN
```

##### 주 서버 모니터링

```sql
-- WAL Sender 프로세스 확인
SELECT * FROM pg_stat_replication;
```

주요 컬럼:
- `sent_lsn`: 주 서버에서 전송한 위치
- 주 서버 현재 WAL 위치와 `sent_lsn`의 큰 차이 -> 주 서버 과부하
- `sent_lsn`과 대기 서버의 `receive_lsn`의 큰 차이 -> 네트워크 지연 또는 대기 서버 과부하

##### 대기 서버 모니터링

```sql
-- WAL Receiver 상태 확인
SELECT * FROM pg_stat_wal_receiver;

-- pg_last_wal_replay_lsn과 flushed_lsn의 큰 차이
-- -> WAL을 받는 속도가 재생 속도보다 빠름
```

##### 프로세스 상태 확인

```bash
# 대기 서버의 walreceiver 프로세스 상태
ps aux | grep walreceiver

# 주 서버의 walsender 프로세스 상태
ps aux | grep walsender
```

### 26.2.6. 복제 슬롯 (Replication Slots)

복제 슬롯은 주 서버가 대기 서버에 필요한 모든 정보를 유지하도록 하는 자동화된 방법을 제공합니다.

#### 개념

- 주 서버가 대기 서버에서 받을 때까지 WAL 세그먼트 제거 방지
- 주 서버가 대기 서버 연결 해제 시에도 행 제거 방지 (복구 충돌 회피)

#### 대체 방법 비교

| 방법 | 장점 | 단점 |
|------|------|------|
| 복제 슬롯 | 필요한 세그먼트만 보유 | WAL이 pg_wal을 채울 수 있음 |
| wal_keep_size | 간단한 설정 | 필요 이상 많은 세그먼트 보유 |
| archive_command | 유연함 | 많은 세그먼트 보유 가능 |
| hot_standby_feedback | Vacuum 행 보호 | 대기 서버 연결 해제 시 효과 없음 |

#### 주의사항

복제 슬롯이 `pg_wal`을 가득 채우지 않도록 주의해야 합니다. `max_slot_wal_keep_size`로 제한할 수 있습니다.

#### 26.2.6.1. 복제 슬롯 조회 및 조작

##### 복제 슬롯 조회

```sql
-- 모든 복제 슬롯 확인
SELECT * FROM pg_replication_slots;

-- 슬롯 정보 확인
SELECT slot_name, slot_type, active FROM pg_replication_slots;
```

#### 26.2.6.2. 설정 예제

##### 1. 물리 복제 슬롯 생성

```sql
postgres=# SELECT * FROM pg_create_physical_replication_slot('node_a_slot');
  slot_name  | lsn
-------------+-----
 node_a_slot |

postgres=# SELECT slot_name, slot_type, active FROM pg_replication_slots;
  slot_name  | slot_type | active
-------------+-----------+--------
 node_a_slot | physical  | f
(1 row)
```

##### 2. 대기 서버 설정

```ini
# postgresql.conf (Standby)
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass'
primary_slot_name = 'node_a_slot'
```

### 26.2.7. 캐스케이딩 복제 (Cascading Replication)

캐스케이딩 복제 기능을 사용하면 대기 서버가 WAL 레코드를 주 서버에서 직접 받지 않고 다른 대기 서버에서 받을 수 있습니다. 이것은 캐스케이딩 복제라고 합니다.

#### 개념

```
대기 서버가 다른 대기 서버에게 WAL 레코드를 스트림
Primary <- Cascading Standby <- Downstream Standby
```

#### 용어

| 용어 | 정의 |
|------|------|
| Cascading Standby | Receiver이면서 Sender인 대기 서버 |
| Upstream Server | 주 서버에 더 가까운 대기 서버 |
| Downstream Server | 주 서버에서 더 먼 대기 서버 |

#### 특징

1. 주 서버 직접 연결 수 감소
2. 사이트 간 대역폭 오버헤드 최소화
3. 각 대기 서버는 단 하나의 업스트림 서버에만 연결
4. 제한 없는 다운스트림 서버 개수
5. 현재 비동기만 가능 (동기 복제 미지원)
6. Hot Standby Feedback 업스트림 전파
7. `recovery_target_timeline = 'latest'`로 승격 후 새 주 서버 추적

#### 캐스케이딩 대기 서버 설정

```ini
# postgresql.conf (Cascading Standby)

# 다른 대기 서버로부터 WAL 수신
primary_conninfo = 'host=upstream_standby_ip port=5432 user=foo password=foopass'

# 다른 대기 서버에게 WAL 스트림 전송
max_wal_senders = 10
hot_standby = on

# 인증 설정: Section 20.1 (pg_hba.conf)
```

### 26.2.8. 동기 복제 (Synchronous Replication)

PostgreSQL 스트리밍 복제는 기본적으로 비동기식입니다. 주 서버가 충돌하면 비동기 대기 서버에 복제되지 않은 일부 트랜잭션이 손실될 수 있습니다. 데이터 손실량은 장애 시 복제 지연에 비례하며, 분배되지 않은 WAL 세그먼트가 즉시 생성되지 않기 때문에 낮은 지연이 보장됩니다.

동기 복제는 트랜잭션의 모든 수정 사항이 하나 이상의 동기 대기 서버로 전송되었음을 확인할 수 있는 기능을 제공합니다. 이는 데이터베이스 서버가 제공할 수 있는 내구성 수준을 확장합니다.

#### 개념 비교

| 모드 | 데이터 손실 | 응답 시간 | 사용 사례 |
|------|-----------|---------|---------|
| 비동기 | 주 서버 크래시 시 손실 가능 | 빠름 | 대부분의 용도 |
| 동기 | 주 서버+대기 서버 동시 크래시만 | 느림 | 높은 내구성 필요 |
| 동기(remote_write) | OS 크래시 (PostgreSQL 아님) | 중간 | 균형잡힌 설정 |
| 동기(remote_apply) | 대기 서버 재생 후 | 가장 느림 | 로드 밸런싱 |

#### 이론 용어

- 2-safe replication: 주 서버 + 대기 서버 모두 디스크에 저장 확인
- group-1-safe: `synchronous_commit = 'remote_write'`일 때

#### 동작 방식

1. 트랜잭션 커밋 시 주 서버에서 대기
2. WAL을 대기 서버 디스크에 기록 확인 대기
3. 대기 서버 응답 확인 후 트랜잭션 완료
4. 최소 대기 시간: 주 서버 <-> 대기 서버 왕복 시간

#### 대기하지 않는 경우

- 읽기 전용 트랜잭션
- 트랜잭션 롤백
- 서브트랜잭션 커밋 (최상위 레벨만 대기)
- 데이터 로딩, 인덱스 빌드 등 긴 작업 중간
- 모든 2단계 커밋 작업

#### 26.2.8.1. 기본 설정 (Basic Configuration)

##### 1. 스트리밍 복제 먼저 설정

```ini
# postgresql.conf (Primary)
primary_conninfo = 'host=192.168.1.100 port=5432 ...'
```

##### 2. 동기 복제 활성화

```ini
# postgresql.conf (Primary)

# 핵심 설정
synchronous_standby_names = 's1'

# 일반적으로 이미 기본값: on
synchronous_commit = on
```

##### 3. WAL Receiver 상태 보고

```ini
# postgresql.conf (Standby)

# WAL을 디스크에 쓸 때마다 상태 보고 (기본값: 10초)
wal_receiver_status_interval = 10s
# 0으로 설정하면 비활성화
```

##### 4. 동작 확인

대기 서버에서 응답 메시지:
- `synchronous_commit = on`: WAL 디스크 저장 시
- `synchronous_commit = remote_write`: 디스크 쓰기(fsync 제외) 후
- `synchronous_commit = remote_apply`: 트랜잭션 재생 후

#### 26.2.8.2. 다중 동기 대기 서버 (Multiple Synchronous Standbys)

##### FIRST 방식 (우선순위 기반)

```ini
synchronous_standby_names = 'FIRST 2 (s1, s2, s3)'
```

동작:
- s1, s2가 동기 (우선순위 순)
- s3는 잠재적 동기 (s1 또는 s2 장애 시 대체)
- s4 (리스트 밖)는 비동기

특징:
- 트랜잭션이 2개 대기 서버 응답 대기
- 하나 장애 시 자동 다음 대기 서버로 변경

##### ANY 방식 (쿼럼 기반)

```ini
synchronous_standby_names = 'ANY 2 (s1, s2, s3)'
```

동작:
- s1, s2, s3 중 최소 2개 응답 필요
- s4 (리스트 밖)는 비동기

특징:
- 더 유연한 구성
- 쿼럼 개념 사용

##### 상태 확인

```sql
SELECT * FROM pg_stat_replication;
-- sync_state: 'sync', 'potential', 'async' 확인
```

#### 26.2.8.3. 성능 계획 (Planning for Performance)

##### 성능 영향

- 대기 서버 응답 대기 시간 증가
- 트랜잭션 락 계속 유지 (리소스 미활용)
- 응답 시간 증가 -> 경합도 상승

##### 권장 설정: 애플리케이션 수준 차별화

```ini
# 예: 중요도별 설정

# 중요 데이터 (10%)
synchronous_commit = on

# 덜 중요한 데이터 (90%)
synchronous_commit = off
```

##### 애플리케이션 코드

```sql
-- 트랜잭션별 동기 복제 설정
BEGIN;
SET synchronous_commit = on;
-- 중요한 변경사항
COMMIT;

BEGIN;
SET synchronous_commit = off;
-- 덜 중요한 데이터
COMMIT;
```

##### 네트워크 대역폭

필수: 네트워크 대역폭 > WAL 생성 속도

#### 26.2.8.4. 고가용성 계획 (Planning for High Availability)

##### 문제점

동기 대기 서버 크래시 -> 트랜잭션 커밋 완료 불가능!

##### 해결책: 다중 대기 서버

```ini
# FIRST 방식
synchronous_standby_names = 'FIRST 2 (s1, s2, s3)'
```

이점:
- s1, s2 중 하나 장애 -> s3로 자동 변경
- 계속 2개 동기 상태 유지

##### 대기 서버 상태 전이

```
catchup 모드
    |
    v (lag이 0이 되면)
streaming 모드 (동기 대기 서버 가능)
```

모니터링:
```sql
SELECT * FROM pg_stat_replication;
-- state: 'catchup' or 'streaming' 확인
```

##### 주 서버 재시작 중 커밋 대기

주 서버가 재시작되면:
- 대기 중인 트랜잭션 -> 완전히 커밋된 것으로 표시
- 모든 대기 서버가 WAL을 받았다는 보장 없음
- 일부 대기 서버에는 커밋이 안 보일 수 있음
- 보장: 커밋 승인 전까지 모든 동기 대기 서버 수신 확인

##### 대기 서버 부족 시 대응

```ini
# 수를 줄이거나 비활성화
synchronous_standby_names = ''

# 설정 리로드
SELECT pg_reload_conf();
```

##### 대기 서버 재생성 중 커밋 대기 방지

```sql
-- synchronous_commit = off에서 실행
SET synchronous_commit = off;
SELECT pg_backup_start('label');
-- ... backup 작업 ...
SELECT pg_backup_stop();
```

### 26.2.9. 대기 상태에서의 연속 아카이빙 (Continuous Archiving in Standby)

대기 상태에서 연속 아카이빙을 사용하는 경우, 두 가지 다른 시나리오가 있습니다: WAL 아카이브가 주 서버와 대기 서버 간에 공유되는 경우와 대기 서버가 자체 WAL 아카이브를 가지는 경우입니다.

#### 2가지 시나리오

| 시나리오 | 설명 | 설정 |
|---------|------|------|
| 공유 아카이브 | 주 서버와 대기 서버가 같은 아카이브 사용 | 복잡한 테스트 필요 |
| 독립 아카이브 | 대기 서버가 자신의 아카이브 사용 | `archive_mode = always` |

#### 1. 독립 아카이브 설정

```ini
# postgresql.conf (Standby)
archive_mode = always
archive_command = 'cp %p /path/to/archive/%f'
```

동작:
- 아카이브 복구 또는 스트리밍 복제로 받은 모든 WAL 아카이브
- 간단하고 추천

#### 2. 공유 아카이브 설정 (복잡함)

```ini
# archive_command 또는 archive_library에서:
# 1. 파일이 이미 존재하는지 확인
# 2. 내용이 동일한지 확인
# 3. 경쟁 조건 방지 (2개 서버가 동시에 같은 파일 아카이브)
```

문제점:
- 주 서버와 대기 서버가 동시에 같은 파일 아카이브 시도
- 파일 덮어쓰기 또는 컨텐츠 불일치 위험

#### 3. archive_mode = on 설정

```ini
archive_mode = on  # 기본값
```

동작:
- 복구 또는 대기 모드에서는 아카이버 비활성화
- 대기 서버 승격 후 활성화
- 대기 서버가 자신이 생성하지 않은 WAL/Timeline은 아카이브하지 않음

해결책:
대기 서버에 도달하기 전에 모든 WAL을 아카이브하도록 보장
- 파일 기반 로그 배송: 자동 보장 (대기 서버는 아카이브된 파일만 복구)
- 스트리밍 복제: 수동으로 보장 필요

### 요약 비교표

| 기능 | 파일 기반 | 스트리밍 | 동기 | 캐스케이딩 |
|------|---------|---------|------|-----------|
| 데이터 손실 | 중간 | 낮음 | 최소 | 낮음 |
| 지연 시간 | 높음 | 낮음 | 높음 | 낮음 |
| 구성 복잡도 | 낮음 | 중간 | 높음 | 높음 |
| 성능 영향 | 낮음 | 낮음 | 중간 | 낮음 |
| 적용 시기 | 언제든 | 실시간 | 실시간 | 계층구조 |

---

## 26.3. 장애 조치 (Failover)

주 서버가 실패하면 대기 서버가 장애 조치 절차를 시작해야 합니다.

### 주 서버 실패 시

대기 서버가 장애 조치 절차를 시작해야 합니다.

### 대기 서버 실패 시

- 별도의 장애 조치 불필요
- 대기 서버 재시작 가능 시 -> 복구 프로세스 즉시 재시작 (재시작 가능한 복구 활용)
- 대기 서버 재시작 불가 시 -> 새로운 대기 서버 인스턴스 생성 필요

### STONITH (Shoot The Other Node In The Head)

주요 위험 상황:
- 주 서버 실패 -> 대기 서버가 새로운 주 서버 승격
- 이후 구 주 서버 재시작 시 반드시 메커니즘 필요
- 두 시스템이 모두 주 서버라고 생각하면 -> 데이터 손실 위험

이것을 때때로 split brain이라고 하며, 구 주 서버가 자신이 더 이상 주 서버가 아님을 인식하도록 하는 메커니즘이 필요합니다.

### 장애 조치 아키텍처

#### 2-System 모델 (주요)

```
Primary <-> Heartbeat <-> Standby
```
- 연결성과 주 서버 생존 여부 지속 확인

#### 3-System 모델 (선택적)

```
Primary <-> Heartbeat <-> Standby
    |                        |
    +-> Witness Server <-----+
```
- 부적절한 장애 조치 방지
- 추가 복잡성 있으므로 신중한 설정 및 테스트 필요

### PostgreSQL의 제한사항

PostgreSQL은 다음을 제공하지 않음:
- 주 서버 실패 감지 시스템 소프트웨어
- 대기 데이터베이스 서버에 알림 메커니즘

필요 도구:
- 외부 도구 사용 필수
- OS 수준의 기능 통합 (IP 주소 마이그레이션 등)

### 장애 조치 후 절차

#### "Degenerate State" (축소된 상태)

- 구 대기 서버 = 새로운 주 서버 (단독 운영)
- 구 주 서버 = 다운 상태

#### 정상 운영 복구

1. 옵션 1: 구 주 서버 복구 시 대기 서버 재구성
2. 옵션 2: 새로운 제3 시스템에 대기 서버 구성
3. pg_rewind 활용: 대형 클러스터에서 복구 속도 향상

```
pg_rewind 유틸리티 사용 가능
-> 역할 전환 기간 단축
```

### Primary <-> Standby 전환

#### 장점

- 빠른 전환
- 정기적인 각 시스템의 유지보수 다운타임 가능
- 장애 조치 메커니즘 테스트 (정상 작동 확인)

#### 권장사항

- 정기적인 전환 수행
- 서면 운영 절차 작성

### Logical Replication 고려사항

Logical Replication Slot 동기화 사용 시:

```
Primary -> Standby로 전환 전:
1. Logical slots 동기화 상태 확인 필요
2. Section 47.2.3 참조: Replication Slot Synchronization
3. Section 29.3 참조: Logical Replication Failover
   -> 정확한 절차 따라야 함
```

### 장애 조치 트리거 명령어

#### Log-Shipping Standby Server 장애 조치 시작

```bash
# 방법 1: pg_ctl 사용
pg_ctl promote

# 방법 2: SQL 함수 사용
SELECT pg_promote();
```

#### 예외사항

보고 서버 설정 시 (읽기 전용, 고가용성 아님):
- 승격(Promote) 불필요

### 체크리스트

- [ ] STONITH 메커니즘 구현
- [ ] Heartbeat 감시 시스템 구성
- [ ] 외부 장애 조치 도구 선택 및 통합
- [ ] `pg_ctl promote` 또는 `pg_promote()` 명령어 테스트
- [ ] pg_rewind 유틸리티 숙지 (대형 클러스터)
- [ ] 정기적 Primary-Standby 전환 테스트
- [ ] 서면 운영 절차 작성
- [ ] Logical replication slot 동기화 상태 확인

---

## 26.4. 핫 대기 (Hot Standby)

핫 대기(Hot Standby)라는 용어는 서버가 아카이브 복구 또는 대기 모드에 있는 동안 클라이언트를 연결하고 읽기 전용 쿼리를 실행할 수 있는 기능을 설명하는 데 사용됩니다. 이는 복제 목적과 원하는 정밀도로 백업을 복원하는 데 유용합니다.

핫 대기라는 용어는 또한 서버가 복구 중이거나 대기 모드에 있는 동안 연결하여 서버에서 쿼리를 실행할 수 있는 기능을 나타냅니다. 이 기능은 복제 목적과 원하는 정밀도로 백업을 복원하는 데 유용합니다.

### 26.4.1. 사용자 개요 (User's Overview)

`hot_standby` 매개변수가 대기 서버에서 true로 설정되면 `recovery_min_apply_delay`에 대해 구성된 지연에 도달한 후 연결을 수락하기 시작합니다. 모든 연결은 엄격히 읽기 전용입니다. 임시 테이블도 작성할 수 없습니다.

핫 대기의 데이터는 약간의 시간 지연이 있어 주 서버에서 커밋된 최신 레코드가 반영되지 않을 수 있습니다. 그러나 동일한 대기 서버에 연결된 사용자는 동시에 동일한 데이터를 볼 수 있습니다. 이것은 최종적 일관성(Eventually Consistent) 상태입니다.

#### 허용되는 명령어

##### 쿼리 접근
```sql
SELECT
COPY TO
```

##### 커서 명령
```sql
DECLARE, FETCH, CLOSE
```

##### 설정 명령
```sql
SHOW, SET, RESET
```

##### 트랜잭션 관리
```sql
BEGIN, END, ABORT, START TRANSACTION
SAVEPOINT, RELEASE, ROLLBACK TO SAVEPOINT
EXCEPTION 블록 및 내부 서브트랜잭션
```

##### 락 명령
```sql
LOCK TABLE (ACCESS SHARE, ROW SHARE, ROW EXCLUSIVE 모드만)
```

##### 플랜 및 리소스
```sql
PREPARE, EXECUTE, DEALLOCATE, DISCARD
```

##### 플러그인 및 확장
```sql
LOAD
```

##### 기타
```sql
UNLISTEN
```

#### 금지되는 명령어

##### DML (Data Manipulation Language)
```sql
INSERT, UPDATE, DELETE, MERGE, COPY FROM, TRUNCATE
```

##### DDL (Data Definition Language)
```sql
CREATE, DROP, ALTER, COMMENT
```

##### 락 관련
```sql
SELECT ... FOR SHARE | UPDATE
LOCK (ACCESS EXCLUSIVE MODE 이상)
```

##### 트랜잭션 설정
```sql
BEGIN READ WRITE
START TRANSACTION READ WRITE
SET TRANSACTION READ WRITE
SET transaction_read_only = off
```

##### 2단계 커밋
```sql
PREPARE TRANSACTION, COMMIT PREPARED, ROLLBACK PREPARED
```

##### 시퀀스 업데이트
```sql
nextval(), setval()
```

##### 리스너
```sql
LISTEN, NOTIFY
```

#### Hot Standby 상태 확인

```sql
-- PostgreSQL 14 이상
SHOW in_hot_standby;

-- 이전 버전
SHOW transaction_read_only;
```

### 26.4.2. 쿼리 충돌 처리 (Handling Query Conflicts)

주 서버와 대기 서버 모두에서 동일한 데이터에 대해 작동하면 두 종류의 작업 간에 잠재적인 충돌이 발생합니다. 핫 대기에서 발생할 수 있는 가장 큰 충돌 원인은 독점 잠금(exclusive lock)을 취하는 것입니다.

#### 충돌 유형

##### 1. 접근 제외 잠금 (Access Exclusive Locks)

- 주 서버의 `LOCK` 명령과 DDL 작업이 대기 서버 쿼리와 충돌

##### 2. 테이블스페이스 삭제

```sql
DROP TABLESPACE -- 대기 서버의 임시 파일과 충돌
```

##### 3. 데이터베이스 삭제

```sql
DROP DATABASE -- 대기 서버 세션과 충돌
```

##### 4. Vacuum 정리

- WAL의 vacuum 정리 레코드가 대기 서버 트랜잭션과 충돌
- 행 버전 정리 시 MVCC 가시성 문제 발생

#### 충돌 해결 메커니즘

관련 파라미터:
```
max_standby_archive_delay     -- 아카이브 WAL 읽기 시 최대 지연
max_standby_streaming_delay   -- 스트리밍 복제 시 최대 지연
```

동작 원리:
1. 지정된 시간 초과 시 충돌 쿼리 취소
2. 쿼리 취소 오류 발생
3. DROP DATABASE의 경우 전체 세션 종료

#### 해결 방안

##### 1. hot_standby_feedback 활성화

```
hot_standby_feedback = on
```

장점:
- VACUUM이 최근 삭제된 행 제거 방지
- 충돌 감소

단점:
- 주 서버의 데드 행 정리 지연
- 테이블 팽창 가능성

##### 2. 파라미터 조정

```ini
# 고가용성 서버 (권장)
max_standby_archive_delay = 10s
max_standby_streaming_delay = 10s

# 분석용 서버
max_standby_archive_delay = -1      # 무한 대기
max_standby_streaming_delay = -1    # 무한 대기
```

##### 3. 모니터링

```sql
-- 충돌 통계 확인
SELECT * FROM pg_stat_database_conflicts;
SELECT * FROM pg_stat_database;
```

##### 4. 로깅 설정

```ini
log_recovery_conflict_waits = on    -- deadlock_timeout 초과 시 로그
```

### 26.4.3. 관리자 개요 (Administrator's Overview)

`hot_standby`가 postgresql.conf에서 `on`으로 설정되고 `standby.signal` 파일이 있으면 서버는 복구 모드에서 연결을 허용하도록 시도하여 핫 대기 모드로 실행됩니다.

#### Hot Standby 시작 확인

필수 조건:
- `hot_standby = on` (postgresql.conf)
- `standby.signal` 파일 존재

로그 메시지:
```
LOG: entering standby mode
... 시간 경과 ...
LOG: consistent recovery state reached
LOG: database system is ready to accept read-only connections
```

#### 공유 메모리 파라미터 설정

대기 서버의 다음 파라미터는 주 서버 이상이어야 합니다:

```
max_connections
max_prepared_transactions
max_locks_per_transaction
max_wal_senders
max_worker_processes
```

권장사항:
1. 증가할 때: 대기 서버 먼저 -> 주 서버
2. 감소할 때: 주 서버 먼저 -> 대기 서버

#### 파라미터 불일치 경고

```
WARNING: hot standby is not possible because of insufficient parameter settings
DETAIL: max_connections = 80 is a lower setting than on the primary server, where its value was 100.
LOG: recovery has paused
DETAIL: If recovery is unpaused, the server will shut down.
```

#### 복구 중 금지 명령어

```sql
CREATE INDEX              -- DDL
GRANT, REVOKE            -- 권한
ANALYZE, VACUUM, CLUSTER -- 유지보수
```

#### 시스템 뷰 동작

##### pg_stat_activity
```sql
-- 복구 트랜잭션은 활성 상태로 표시되지 않음
SELECT * FROM pg_stat_activity;
```

##### pg_locks
```sql
-- 백업 프로세스가 보유한 AccessExclusiveLocks 표시
SELECT * FROM pg_locks;
```

##### pg_prepared_xacts
```sql
-- 복구 중에는 항상 비어있음
SELECT * FROM pg_prepared_xacts;
```

#### 특별 고려사항

##### 1. Hint Bit 업데이트
```
-- 주 서버의 hint bit는 WAL 로깅되지 않음
-- 대기 서버가 다시 쓰기 수행 (읽기 전용이지만 쓰기 발생)
```

##### 2. 임시 파일
```
-- 사용자가 대용량 정렬 임시 파일 생성 가능
-- relcache 정보 파일 재생성 가능
```

##### 3. 외부 데이터베이스 접근
```sql
-- 읽기 전용이지만 dblink로 원격 DB 쓰기 가능
-- PL 함수로 외부 작업 수행 가능
```

##### 4. Advisory Locks
```sql
-- 정상 작동
-- WAL 로깅되지 않음
-- 데드락 감지 지원
SELECT pg_advisory_lock(1);
```

#### 프로세스 활동

```
Checkpointer: 활성 (restartpoint 수행)
Background Writer: 활성 (블록 정리)
Autovacuum: 비활성 (복구 종료 후 시작)
```

### 26.4.4. 핫 대기 매개변수 참조 (Hot Standby Parameter Reference)

#### 주 서버 파라미터

```ini
wal_level = replica 또는 logical    -- 필수
```

#### 대기 서버 파라미터

```ini
hot_standby = on                              -- Hot Standby 활성화
max_standby_archive_delay = 300s              -- 아카이브 읽기 최대 지연
max_standby_streaming_delay = 300s            -- 스트리밍 최대 지연
hot_standby_feedback = off                    -- VACUUM 정리 피드백
log_recovery_conflict_waits = off             -- 충돌 대기 로깅
```

### 26.4.5. 주의사항 (Caveats)

핫 대기 모드에는 몇 가지 제한 사항이 있습니다.

#### 1. 대형 서브트랜잭션

```
-- 64개 초과 서브트랜잭션
-- 읽기 전용 연결 시작 지연
-- 장기 실행 쓰기 트랜잭션 완료까지 대기
```

로그 메시지:
```
LOG: delaying hot standby connections until all transactions are finished
```

#### 2. 체크포인트 필요성

```
-- 주 서버 체크포인트 시 유효한 시작점 생성
-- 주 서버 종료 시 대기 서버가 hot standby 재진입 불가능할 수 있음
```

#### 3. AccessExclusiveLocks와 Prepared Transactions

```
-- 복구 종료 시 준비된 트랜잭션의 AccessExclusiveLocks
-- 일반적인 2배 lock table 항목 필요
-- max_locks_per_transaction을 2배로 설정 권장
```

설정 예:
```ini
-- 주 서버
max_locks_per_transaction = 64

-- 대기 서버 (권장)
max_locks_per_transaction = 128
```

#### 4. 직렬화 격리 수준 미지원

```sql
-- 불가능
BEGIN ISOLATION LEVEL SERIALIZABLE;

-- 오류 발생
ERROR: cannot use SERIALIZABLE isolation level in hot standby mode
```

### 실제 구성 예

```ini
# postgresql.conf (주 서버)
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /backup/wal/%f && cp %p /backup/wal/%f'

# postgresql.conf (대기 서버)
hot_standby = on
max_standby_archive_delay = 300s
max_standby_streaming_delay = 300s
hot_standby_feedback = on
max_connections = 200          # 주 서버 이상
max_locks_per_transaction = 64
```

이 설정으로 안정적인 Hot Standby 환경을 구축할 수 있습니다.

---

## 참고

- Section 25.3: Continuous Archiving and Point-in-Time Recovery
- Section 26.2.5: Streaming Replication (상세)
- Section 26.3: Failover
- Section 26.4: Hot Standby
- Section 29.3: Logical Replication Failover
- Section 47.2.3: Replication Slot Synchronization

---

## 요약

PostgreSQL의 고가용성, 로드 밸런싱 및 복제는 다양한 솔루션을 통해 구현할 수 있습니다. 각 솔루션은 특정 요구사항과 환경에 맞게 선택해야 합니다.

### 주요 솔루션 요약

| 솔루션 | 특징 | 적합한 사용 사례 |
|--------|------|------------------|
| 공유 디스크 장애조치 | 단일 데이터 복사본, 빠른 장애조치 | 공유 스토리지 환경 |
| 파일 시스템 복제 | 블록 수준 미러링 | DRBD 등 사용 환경 |
| WAL 로그 배송 | 내장 기능, 전체 DB 복제 | 일반적인 HA 구성 |
| 논리적 복제 | 테이블 단위 복제, 양방향 가능 | 선택적 데이터 복제 |
| 트리거 기반 복제 | 테이블 단위, 비동기 | 분석/웨어하우스 분산 |
| SQL 미들웨어 | 쿼리 분산 | 로드 밸런싱 중심 |
| 비동기 다중 마스터 | 독립 서버, 주기적 동기화 | 불규칙한 연결 환경 |
| 동기 다중 마스터 | 모든 서버 쓰기 가능 | 읽기 중심 워크로드 |

### 핵심 개념

1. WAL (Write-Ahead Log): PostgreSQL의 복제 핵심 메커니즘
2. 스트리밍 복제: 실시간에 가까운 데이터 동기화
3. 동기/비동기: 데이터 내구성과 성능 간의 트레이드오프
4. 핫 대기: 복구 중에도 읽기 전용 쿼리 가능
5. 복제 슬롯: WAL 세그먼트 자동 관리
6. 캐스케이딩: 계층적 복제 구조로 확장성 향상

### 권장 사항

- 중요한 데이터는 동기 복제 사용
- 성능이 중요한 경우 비동기 복제 고려
- 정기적인 장애조치 테스트 수행
- 서면 운영 절차 작성 및 유지
- 모니터링 시스템 구축
