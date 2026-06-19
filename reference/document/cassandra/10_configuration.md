# Cassandra 설정

> 이 문서는 Apache Cassandra 공식 문서의 "Configuration" 섹션을 한국어로 번역한 것입니다.
> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/

---

## 목차

1. [개요](#개요)
2. [cassandra.yaml](#cassandrayaml)
   - [클러스터 및 노드 식별 설정](#클러스터-및-노드-식별-설정)
   - [힌트 핸드오프(Hinted Handoff) 설정](#힌트-핸드오프hinted-handoff-설정)
   - [인증 및 권한 부여 설정](#인증-및-권한-부여-설정)
   - [파티셔너 및 데이터 디렉터리 설정](#파티셔너-및-데이터-디렉터리-설정)
   - [CDC(Change Data Capture) 설정](#cdcchange-data-capture-설정)
   - [디스크 장애 정책 설정](#디스크-장애-정책-설정)
   - [캐시 설정](#캐시-설정)
   - [커밋로그(Commit Log) 설정](#커밋로그commit-log-설정)
   - [시드 프로바이더(Seed Provider) 설정](#시드-프로바이더seed-provider-설정)
   - [동시성(Concurrency) 설정](#동시성concurrency-설정)
   - [멤테이블(Memtable) 설정](#멤테이블memtable-설정)
   - [인덱스 요약(Index Summary) 설정](#인덱스-요약index-summary-설정)
   - [fsync 및 디스크 최적화 설정](#fsync-및-디스크-최적화-설정)
   - [네트워크 포트 설정](#네트워크-포트-설정)
   - [Listen 주소 설정](#listen-주소-설정)
   - [네이티브 트랜스포트(Native Transport) 설정](#네이티브-트랜스포트native-transport-설정)
   - [RPC/클라이언트 주소 설정](#rpc클라이언트-주소-설정)
   - [소켓 버퍼 설정](#소켓-버퍼-설정)
   - [백업 및 스냅샷 설정](#백업-및-스냅샷-설정)
   - [컬럼 인덱스 설정](#컬럼-인덱스-설정)
   - [컴팩션(Compaction) 설정](#컴팩션compaction-설정)
   - [스트리밍(Streaming) 설정](#스트리밍streaming-설정)
   - [요청 타임아웃(Request Timeout) 설정](#요청-타임아웃request-timeout-설정)
   - [모니터링 및 헬스 설정](#모니터링-및-헬스-설정)
   - [청크 캐시 및 버퍼 풀 설정](#청크-캐시-및-버퍼-풀-설정)
   - [스니치(Snitch) 설정](#스니치snitch-설정)
   - [노드 간 압축 설정](#노드-간-압축-설정)
   - [암호화(Encryption) 설정](#암호화encryption-설정)
   - [가드레일 및 임계값 설정](#가드레일-및-임계값-설정)
3. [cassandra-rackdc.properties](#cassandra-rackdcproperties)
4. [cassandra-topology.properties](#cassandra-topologyproperties)
5. [cassandra-env.sh](#cassandra-envsh)
6. [jvm-* 옵션 파일](#jvm-옵션-파일)
7. [logback.xml](#logbackxml)
8. [단위(Unit)와 분리된 파라미터 이름](#단위unit와-분리된-파라미터-이름)
9. [참고 자료](#참고-자료)

---

## 개요

이 섹션은 Apache Cassandra를 어떻게 설정하는지 설명합니다. Cassandra의 설정은 여러 개의 설정 파일로 구성되며, 각 파일은 서로 다른 역할을 담당합니다. 주요 설정 파일은 다음과 같습니다.

- **cassandra.yaml**: Cassandra의 기본(주) 설정 파일입니다. 클러스터 이름, 노드 식별, 네트워크, 스토리지, 동시성, 타임아웃, 인증/권한 등 거의 모든 핵심 설정이 이 파일에 정의됩니다.
- **cassandra-rackdc.properties**: 랙(rack)과 데이터센터(datacenter)의 토폴로지 정보를 설정합니다. `GossipingPropertyFileSnitch`와 EC2 스니치에서 사용됩니다.
- **cassandra-env.sh**: JVM 환경 변수를 설정하는 bash 스크립트입니다. 힙 크기 등 동적으로 계산해야 하는 값을 다룰 때 사용합니다.
- **cassandra-topology.properties**: `PropertyFileSnitch`에서 사용하는 네트워크 토폴로지 정의 파일입니다.
- **commitlog-archiving.properties**: 커밋로그 아카이빙 설정 파일입니다.
- **logback.xml**: 로깅 프레임워크(logback) 설정 파일입니다.
- **jvm-\* 파일**: JVM의 정적 옵션(가비지 컬렉션, 힙 설정 등)을 정의하는 파일입니다.

이 문서는 각 설정 파일과 그 안의 주요 파라미터를 상세하게 설명합니다.

---

## cassandra.yaml

`cassandra.yaml` 파일은 Apache Cassandra의 기본(주) 설정 파일입니다. 클러스터 동작의 거의 모든 측면을 이 파일에서 제어할 수 있습니다. 아래에서는 주요 파라미터들을 범주별로 나누어 설명합니다.

> 참고: 일부 파라미터는 기본적으로 주석 처리(commented out)되어 있어 명시적으로 활성화해야 합니다. 또한 Cassandra 4.1부터 많은 파라미터의 이름이 단위(unit)와 분리되어, 값에 `ms`, `s`, `KiB`, `MiB` 등의 단위를 직접 명시할 수 있습니다(예: `read_request_timeout: 5000ms`).

---

### 클러스터 및 노드 식별 설정

#### cluster_name

클러스터의 이름(name)입니다. 이 설정은 주로 한 논리적 클러스터에 속한 머신이 다른 클러스터에 잘못 합류(join)하는 것을 방지하기 위해 사용됩니다.

- **기본값:** `'Test Cluster'`

#### num_tokens

이 노드에 링(ring) 상에서 무작위로 할당되는 토큰(token)의 개수를 정의합니다. 다른 노드 대비 토큰 수가 많을수록 더 큰 비율의 데이터를 저장하게 됩니다. 이 값을 지정하지 않으면 Cassandra는 레거시 호환성을 위해 기본값인 1개 토큰을 사용합니다.

- **기본값:** `16`

#### allocate_tokens_for_keyspace

이 노드에 대해 `num_tokens`개의 토큰을 자동 할당(automatic allocation)하도록 트리거합니다. 할당 알고리즘은 해당 데이터센터 내 노드들 간에 복제 부하(replicated load)를 최적화하는 방향으로 토큰을 선택하려 시도합니다.

- **기본값:** `KEYSPACE` *(기본적으로 주석 처리됨)*

#### allocate_tokens_for_local_replication_factor

복제 팩터(replica factor)를 키스페이스나 데이터센터와 무관하게 명시적으로 설정합니다. 이는 NTS(NetworkTopologyStrategy)처럼 데이터센터 내에서의 복제 팩터를 의미합니다.

- **기본값:** `3`

#### initial_token

`initial_token`을 사용하면 토큰을 수동으로 지정할 수 있습니다. vnodes(`num_tokens > 1`)와 함께 사용할 수 있으며, 이 경우 쉼표로 구분된 목록을 제공해야 합니다. 하지만 주로 vnodes가 활성화되지 않은 레거시 클러스터에 노드를 추가할 때 사용됩니다.

- **기본값:** *(기본적으로 주석 처리됨)*

---

### 힌트 핸드오프(Hinted Handoff) 설정

힌트 핸드오프는 일시적으로 다운된 노드를 대신해 코디네이터가 쓰기를 임시 저장(hint)했다가, 해당 노드가 복구되면 전달하는 메커니즘입니다.

#### hinted_handoff_enabled

`true` 또는 `false`로 설정하여 힌트 핸드오프를 전역적으로 활성화/비활성화합니다.

- **기본값:** `true`

#### hinted_handoff_disabled_datacenters

`hinted_handoff_enabled`가 `true`일 때, 힌트 핸드오프를 수행하지 않을 데이터센터의 블랙리스트(blacklist)입니다.

- **기본값:** *(기본적으로 주석 처리됨; 복합 옵션)*

#### max_hint_window

다운된 호스트에 대해 힌트가 생성되는 최대 시간을 정의합니다. 호스트가 이 시간만큼 다운되어 있으면, 그 호스트에 대한 새로운 힌트는 더 이상 생성되지 않습니다. 다시 살아있는 것이 확인된 후 또 다운되어야 힌트 생성이 재개됩니다.

- **기본값:** `3h`

#### hinted_handoff_throttle

전달 스레드(delivery thread)당 초당 최대 스로틀(throttle)을 KiB 단위로 지정합니다. 이 값은 클러스터 내 노드 수에 비례하여 줄어듭니다.

- **기본값:** `1024KiB`

#### max_hints_delivery_threads

힌트를 전달하는 데 사용할 스레드의 수입니다. 멀티 데이터센터 배포 환경에서는 이 값을 늘리는 것을 고려하십시오. 데이터센터 간 핸드오프는 일반적으로 더 느리기 때문입니다.

- **기본값:** `2`

#### hints_directory

Cassandra가 힌트를 저장해야 하는 디렉터리입니다. 설정하지 않으면 기본 디렉터리는 `$CASSANDRA_HOME/data/hints`입니다.

- **기본값:** `/var/lib/cassandra/hints` *(기본적으로 주석 처리됨)*

#### hints_flush_period

힌트가 내부 버퍼에서 디스크로 얼마나 자주 플러시(flush)되어야 하는지를 정의합니다. 이는 `fsync`를 트리거하지 **않습니다**.

- **기본값:** `10000ms`

#### max_hints_file_size

단일 힌트 파일의 최대 크기를 메비바이트(MiB) 단위로 지정합니다.

- **기본값:** `128MiB`

#### batchlog_replay_throttle

배치로그 재생(replay)에 대한 초당 전체 최대 스로틀을 KiB 단위로 지정합니다. 이 값은 클러스터 내 노드 수에 비례하여 줄어듭니다.

- **기본값:** `1024KiB`

---

### 인증 및 권한 부여 설정

#### authenticator

`IAuthenticator`를 구현하는 인증 백엔드(authentication backend)로, 사용자를 식별(identify)하는 데 사용됩니다. Cassandra는 기본 제공으로 `org.apache.cassandra.auth.AllowAllAuthenticator`와 `org.apache.cassandra.auth.PasswordAuthenticator`를 제공합니다.

- **기본값:** `AllowAllAuthenticator`

#### authorizer

`IAuthorizer`를 구현하는 권한 부여 백엔드(authorization backend)로, 접근을 제한하거나 권한(permission)을 부여하는 데 사용됩니다. Cassandra는 기본 제공으로 `org.apache.cassandra.auth.AllowAllAuthorizer`와 `org.apache.cassandra.auth.CassandraAuthorizer`를 제공합니다.

- **기본값:** `AllowAllAuthorizer`

#### role_manager

인증 및 권한 부여 백엔드의 일부로, `IRoleManager`를 구현합니다. 역할(role) 간의 권한 부여(grant)와 멤버십(membership)을 관리하는 데 사용됩니다. Cassandra는 기본 제공으로 `org.apache.cassandra.auth.CassandraRoleManager`를 제공합니다.

- **기본값:** `CassandraRoleManager`

#### network_authorizer

`INetworkAuthorizer`를 구현하는 네트워크 권한 부여 백엔드(network authorization backend)로, 특정 데이터센터(DC)에 대한 사용자 접근을 제한하는 데 사용됩니다. Cassandra는 기본 제공으로 `org.apache.cassandra.auth.AllowAllNetworkAuthorizer`와 `org.apache.cassandra.auth.CassandraNetworkAuthorizer`를 제공합니다.

- **기본값:** `AllowAllNetworkAuthorizer`

#### roles_validity

역할 캐시(roles cache)의 유효 기간(validity period)입니다. 역할 관리자(role manager)에 따라 부여된 역할을 가져오는 것은 비용이 큰 작업일 수 있습니다. 기본값은 2000이며, 캐싱을 완전히 비활성화하려면 0으로 설정합니다.

- **기본값:** `2000ms`

#### roles_update_interval

역할 캐시(활성화된 경우)의 새로고침 간격(refresh interval)입니다. 이 간격이 지나면 캐시 항목이 새로고침 대상이 됩니다. 기본값은 `roles_validity`와 동일한 값입니다.

- **기본값:** `2000ms` *(기본적으로 주석 처리됨)*

#### permissions_validity

권한 캐시(permissions cache)의 유효 기간입니다. 권한 부여기(authorizer)에 따라 권한을 가져오는 것은 비용이 큰 작업일 수 있습니다. 기본값은 2000이며, 비활성화하려면 0으로 설정합니다.

- **기본값:** `2000ms`

#### permissions_update_interval

권한 캐시(활성화된 경우)의 새로고침 간격입니다. 이 간격이 지나면 캐시 항목이 새로고침 대상이 됩니다. 기본값은 `permissions_validity`와 동일한 값입니다.

- **기본값:** `2000ms` *(기본적으로 주석 처리됨)*

#### credentials_validity

자격 증명 캐시(credentials cache)의 유효 기간입니다. 이 캐시는 제공된 `PasswordAuthenticator` 구현과 긴밀하게 결합되어 있습니다. 기본값은 2000이며, 자격 증명 캐싱을 비활성화하려면 0으로 설정합니다.

- **기본값:** `2000ms`

#### credentials_update_interval

자격 증명 캐시(활성화된 경우)의 새로고침 간격입니다. 이 간격이 지나면 캐시 항목이 새로고침 대상이 됩니다. 기본값은 `credentials_validity`와 동일한 값입니다.

- **기본값:** `2000ms` *(기본적으로 주석 처리됨)*

---

### 파티셔너 및 데이터 디렉터리 설정

#### partitioner

파티셔너(partitioner)는 행 그룹(파티션 키 기준)을 클러스터의 노드들에 분산시키는 역할을 합니다. 파티셔너는 모든 데이터를 다시 로드하지 않고서는 변경할 수 **없습니다**. 기본 파티셔너는 `Murmur3Partitioner`입니다.

- **기본값:** `org.apache.cassandra.dht.Murmur3Partitioner`

#### data_file_directories

Cassandra가 데이터를 디스크에 저장해야 하는 디렉터리입니다. 여러 디렉터리를 지정하면, Cassandra는 토큰 범위를 분할하여 데이터를 디렉터리 간에 고르게 분산시킵니다. 설정하지 않으면 기본 디렉터리는 `$CASSANDRA_HOME/data/data`입니다.

- **기본값:** *(기본적으로 주석 처리됨; 복합 옵션)*

#### local_system_data_file_directory

Cassandra가 로컬 시스템 키스페이스(local system keyspaces)의 데이터를 저장해야 하는 디렉터리입니다. 기본적으로 Cassandra는 이를 `data_file_directories`에 지정된 첫 번째 데이터 디렉터리에 저장합니다.

- **기본값:** *(기본적으로 주석 처리됨)*

#### commitlog_directory

커밋로그(commit log) 디렉터리입니다. 자기 디스크(magnetic HDD)에서 실행할 때는, 데이터 디렉터리와 분리된 별도의 스핀들(spindle)에 두어야 합니다. 설정하지 않으면 기본 디렉터리는 `$CASSANDRA_HOME/data/commitlog`입니다.

- **기본값:** `/var/lib/cassandra/commitlog` *(기본적으로 주석 처리됨)*

#### saved_caches_directory

저장된 캐시(saved caches) 디렉터리입니다. 설정하지 않으면 기본 디렉터리는 `$CASSANDRA_HOME/data/saved_caches`입니다.

- **기본값:** `/var/lib/cassandra/saved_caches` *(기본적으로 주석 처리됨)*

---

### CDC(Change Data Capture) 설정

#### cdc_enabled

노드별로 CDC 기능을 활성화/비활성화합니다. 이 설정은 쓰기 경로 할당 거부(write path allocation rejection)에 사용되는 로직을 수정합니다.

- **기본값:** `false`

#### cdc_raw_directory

`cdc_enabled: true`이고 세그먼트에 CDC가 활성화된 테이블에 대한 뮤테이션(mutation)이 포함되어 있을 경우, 플러시 시 `CommitLogSegments`가 이 디렉터리로 이동됩니다. 데이터 디렉터리와 분리된 별도의 스핀들에 두어야 합니다.

- **기본값:** `/var/lib/cassandra/cdc_raw` *(기본적으로 주석 처리됨)*

#### cdc_total_space

CDC 로그가 디스크에서 사용할 전체 공간입니다. 공간이 이 값을 초과하면 Cassandra는 `WriteTimeoutException`을 발생시킵니다.

- **기본값:** `4096MiB`

---

### 디스크 장애 정책 설정

#### disk_failure_policy

데이터 디스크 장애에 대한 정책입니다.

- `die`: 노드를 종료(shut down)합니다.
- `stop_paranoid`: 단일 SSTable 오류에 대해서도 종료합니다.
- `stop`: 종료하되 노드를 조사 가능한 상태(inspectable)로 유지합니다.
- `best_effort`: 남아 있는 SSTable을 기반으로 응답합니다.
- `ignore`: 요청이 실패하도록 둡니다.

- **기본값:** `stop`

#### commit_failure_policy

커밋 디스크 장애에 대한 정책입니다.

- `die`: JVM을 종료(kill)합니다.
- `stop`: 노드를 종료합니다.
- `stop_commit`: 커밋로그만 종료합니다.
- `ignore`: 배치가 실패하도록 둡니다.

- **기본값:** `stop`

---

### 캐시 설정

#### prepared_statements_cache_size

네이티브 프로토콜 준비된 구문(prepared statement) 캐시의 최대 크기입니다. 유효한 값은 `auto`이거나 0보다 큰 값입니다. 너무 큰 값을 지정하면 장시간 실행되는 GC가 발생할 수 있습니다.

- **기본값:** *(기본적으로 auto)*

#### key_cache_size

메모리 내 키 캐시(key cache)의 최대 크기입니다. 키 캐시 히트(hit)마다 1번의 seek를 절약합니다. 기본값은 비워 두어 `auto`(`min(힙의 5%(MiB 단위), 100MiB)`)가 되도록 합니다. 키 캐시를 비활성화하려면 0으로 설정합니다.

- **기본값:** *(기본적으로 auto)*

#### key_cache_save_period

Cassandra가 키 캐시를 저장해야 하는 주기를 초 단위로 지정합니다. 캐시는 `saved_caches_directory`에 저장됩니다. 저장된 캐시는 콜드 스타트(cold-start) 속도를 크게 향상시킵니다.

- **기본값:** `4h`

#### key_cache_keys_to_save

키 캐시에서 저장할 키의 개수입니다. 기본적으로 비활성화되어 있으며, 이는 모든 키를 저장한다는 의미입니다.

- **기본값:** `100` *(기본적으로 주석 처리됨)*

#### row_cache_class_name

행 캐시(row cache) 구현 클래스 이름입니다. 사용 가능한 구현은 `org.apache.cassandra.cache.OHCProvider`(완전 오프힙)와 `org.apache.cassandra.cache.SerializingCacheProvider`입니다.

- **기본값:** `org.apache.cassandra.cache.OHCProvider` *(기본적으로 주석 처리됨)*

#### row_cache_size

메모리 내 행 캐시의 최대 크기입니다. OHC 캐시 구현은 맵 구조를 관리하기 위한 추가적인 오프힙 메모리를 필요로 합니다. 기본값은 0으로, 행 캐싱을 비활성화합니다.

- **기본값:** `0MiB`

#### row_cache_save_period

Cassandra가 행 캐시를 저장해야 하는 주기를 초 단위로 지정합니다. 캐시는 `saved_caches_directory`에 저장됩니다. 기본값은 0으로, 행 캐시 저장을 비활성화합니다.

- **기본값:** `0s`

#### counter_cache_size

메모리 내 카운터 캐시(counter cache)의 최대 크기입니다. 카운터 캐시는 핫(hot) 카운터 셀에 대한 카운터 락(lock) 경합을 줄이는 데 도움이 됩니다. 기본값은 비워 두어 `auto`(`min(힙의 2.5%(MiB 단위), 50MiB)`)가 되도록 합니다.

- **기본값:** *(기본적으로 auto)*

#### counter_cache_save_period

Cassandra가 카운터 캐시(키만)를 저장해야 하는 주기를 초 단위로 지정합니다. 캐시는 `saved_caches_directory`에 저장됩니다.

- **기본값:** `7200s`

---

### 커밋로그(Commit Log) 설정

#### commitlog_sync

커밋로그의 동기화(sync) 모드입니다.

- `periodic`: 쓰기가 즉시 ack되고, 일정 간격마다 동기화됩니다.
- `batch`: 쓰기가 플러시될 때까지 블록됩니다.
- `group`: 쓰기가 블록되지만 시간 윈도 내에서 배치로 묶입니다.

- **기본값:** `periodic`

#### commitlog_sync_period

커밋로그를 디스크로 주기적으로 동기화하는 간격(periodic syncing)입니다.

- **기본값:** `10000ms` (최소 단위: ms)

#### commitlog_segment_size

개별 커밋로그 파일 세그먼트의 크기입니다. 커밋로그 세그먼트는 그 안의 모든 데이터가 SSTable로 플러시된 후 아카이브, 삭제 또는 재활용될 수 있습니다.

- **기본값:** `32MiB` (최소 단위: MiB)

#### commitlog_total_space

커밋로그가 디스크에서 사용할 전체 공간입니다. 공간이 이 값을 초과하면, Cassandra는 가장 오래된 세그먼트에 있는 모든 더티(dirty) 컬럼 패밀리를 플러시합니다.

- **기본값:** `8192MiB` (최소 단위: MiB)

---

### 시드 프로바이더(Seed Provider) 설정

#### seed_provider

`SeedProvider` 인터페이스를 구현하고 파라미터의 `Map<String, String>`을 받는 생성자를 가진 모든 클래스가 사용될 수 있습니다. 시드는 노드 디스커버리(node discovery)를 위한 컨택 포인트(contact point)입니다.

- **기본값:** 시드가 `127.0.0.1:7000`인 `SimpleSeedProvider`

---

### 동시성(Concurrency) 설정

#### concurrent_reads

OS와 드라이브가 작업을 재정렬(reorder)할 수 있을 만큼 충분히 낮은 위치에 작업을 큐잉하도록, `(16 * 드라이브 수)`로 설정해야 합니다.

- **기본값:** `32`

#### concurrent_writes

이상적인 값은 시스템의 코어 수에 따라 달라집니다. `(8 * 코어 수)`가 좋은 기준(rule of thumb)입니다.

- **기본값:** `32`

#### concurrent_counter_writes

동시 카운터 쓰기 작업입니다. 카운터 쓰기는 read-before-write 시맨틱을 가지므로 읽기와 유사합니다.

- **기본값:** `32`

#### concurrent_materialized_view_writes

머티리얼라이즈드 뷰(materialized view)에 대한 동시 쓰기 작업입니다. 읽기가 관여하므로 동시 읽기(concurrent_reads)와 동시 쓰기(concurrent_writes) 중 더 작은 값으로 제한해야 합니다.

- **기본값:** `32`

---

### 멤테이블(Memtable) 설정

#### memtable_heap_space

멤테이블(memtable)에 사용할 수 있는 전체 메모리입니다. 이 한도를 초과하면 Cassandra는 플러시가 완료될 때까지 쓰기 수락을 중단합니다.

- **기본값:** `2048MiB` (최소 단위: MiB)

#### memtable_offheap_space

멤테이블을 위한 오프힙(off-heap) 메모리 할당량입니다.

- **기본값:** `2048MiB` (최소 단위: MiB)

#### memtable_cleanup_threshold

플러시 중이 아닌 멤테이블이 차지하는 크기와 허용된 전체 크기의 비율로, 이 비율에 도달하면 가장 큰 멤테이블의 플러시를 트리거합니다.

- **기본값:** `0.11`

#### memtable_allocation_type

할당 방식 옵션입니다. `heap_buffers`, `offheap_buffers`, `offheap_objects` 중 하나입니다.

- **기본값:** `heap_buffers`

#### memtable_flush_writers

디스크당 멤테이블 플러시 라이터(flush writer) 스레드 수와 동시에 플러시할 수 있는 멤테이블의 총 개수를 설정합니다.

- **기본값:** `2`

---

### 인덱스 요약(Index Summary) 설정

#### index_summary_capacity

SSTable 인덱스 요약(index summary)을 위한 고정 메모리 풀 크기입니다. 메모리 사용량이 이 한도를 초과하면, 읽기 속도가 낮은 SSTable의 인덱스 요약을 축소합니다.

- **기본값:** *(힙의 5%, 비워 두면 auto)* (최소 단위: KiB)

#### index_summary_resize_interval

고정 크기 풀에서 메모리를 재분배하기 위해 인덱스 요약을 얼마나 자주 다시 샘플링(resample)할지를 정합니다.

- **기본값:** `60m` (최소 단위: m)

---

### fsync 및 디스크 최적화 설정

#### trickle_fsync

순차 쓰기(sequential writing)를 수행할 때, OS가 더티 버퍼를 강제로 플러시하도록 일정 간격마다 `fsync()`를 호출할지 여부입니다.

- **기본값:** `false`

#### trickle_fsync_interval

순차 쓰기 중 주기적인 fsync 작업을 트리거하는 간격 임계값입니다.

- **기본값:** `10240KiB` (최소 단위: KiB)

#### disk_optimization_strategy

디스크 읽기를 최적화하기 위한 전략입니다.

- `ssd`: 솔리드 스테이트 디스크(SSD)용입니다(기본값).
- `spinning`: 스피닝 디스크(자기 디스크)용입니다.

- **기본값:** `ssd`

---

### 네트워크 포트 설정

#### storage_port

명령과 데이터를 위한 TCP 포트입니다. 보안상의 이유로, 이 포트를 인터넷에 노출해서는 안 됩니다.

- **기본값:** `7000`

#### ssl_storage_port

레거시 암호화 통신을 위한 SSL 포트입니다. 이 속성은 `server_encryption_options`에서 활성화하지 않는 한 사용되지 않습니다.

- **기본값:** `7001`

---

### Listen 주소 설정

#### listen_address

바인딩하고 다른 Cassandra 노드에게 연결하라고 알릴 주소 또는 인터페이스입니다. 여러 노드가 서로 통신할 수 있게 하려면 반드시 이 값을 변경해야 합니다!

- **기본값:** `localhost`

#### listen_interface

`listen_address` 또는 `listen_interface` 중 하나만 설정하고, 둘 다 설정하지 마십시오. 인터페이스는 단일 주소에 대응해야 하며, IP 별칭(IP aliasing)은 지원되지 않습니다.

- **기본값:** `eth0` *(주석 처리됨)*

#### broadcast_address

다른 Cassandra 노드에게 브로드캐스트할 주소입니다. 이 값을 비워 두면 `listen_address`와 동일한 값으로 설정됩니다.

- **기본값:** `1.2.3.4` *(주석 처리됨)*

#### listen_on_broadcast_address

여러 개의 물리적 네트워크 인터페이스를 사용할 때, `listen_address` 외에 `broadcast_address`에서도 수신 대기하려면 이 값을 `true`로 설정합니다.

- **기본값:** `false` *(주석 처리됨)*

#### internode_authenticator

`IInternodeAuthenticator`를 구현하는 노드 간 인증 백엔드(internode authentication backend)로, 피어 노드(peer node)로부터의 연결을 허용/거부하는 데 사용됩니다.

- **기본값:** `AllowAllInternodeAuthenticator` *(주석 처리됨)*

---

### 네이티브 트랜스포트(Native Transport) 설정

#### start_native_transport

네이티브 트랜스포트 서버를 시작할지 여부입니다. 네이티브 트랜스포트가 바인딩되는 주소는 `rpc_address`로 정의됩니다.

- **기본값:** `true`

#### native_transport_port

CQL 네이티브 트랜스포트가 클라이언트의 연결을 수신 대기할 포트입니다. 보안상의 이유로, 이 포트를 인터넷에 노출해서는 안 됩니다.

- **기본값:** `9042`

#### native_transport_port_ssl

암호화가 표준 포트와 별도로 활성화될 때 사용되는 네이티브 트랜스포트 전용 SSL 포트입니다.

- **기본값:** `9142` *(주석 처리됨)*

#### native_transport_max_threads

요청을 처리하기 위한 최대 스레드 수입니다(유휴 스레드는 30초 후 중지됩니다).

- **기본값:** `128` *(주석 처리됨)*

#### native_transport_max_frame_size

허용되는 프레임(frame)의 최대 크기입니다. 이보다 큰 프레임은 유효하지 않은 것으로 간주되어 거부됩니다.

- **기본값:** `16MiB` *(주석 처리됨, 최소 단위: MiB)*

#### native_transport_allow_older_protocols

Cassandra가 더 오래된, 그러나 현재 지원되는 프로토콜 버전을 존중(honor)할지를 제어합니다.

- **기본값:** `true`

---

### RPC/클라이언트 주소 설정

#### rpc_address

네이티브 트랜스포트 서버를 바인딩할 주소 또는 인터페이스입니다. `listen_address`와 달리 `0.0.0.0`을 지정할 수 있다는 점에 유의하십시오.

- **기본값:** `localhost`

#### rpc_interface

`rpc_address` 또는 `rpc_interface` 중 하나만 설정하고, 둘 다 설정하지 마십시오. 인터페이스는 단일 주소에 대응해야 하며, IP 별칭은 지원되지 않습니다.

- **기본값:** `eth1` *(주석 처리됨)*

#### broadcast_rpc_address

드라이버와 다른 Cassandra 노드에게 브로드캐스트할 RPC 주소입니다. 이 값은 `0.0.0.0`으로 설정할 수 없습니다.

- **기본값:** `1.2.3.4` *(주석 처리됨)*

#### rpc_keepalive

RPC/네이티브 연결에서 keepalive를 활성화하거나 비활성화합니다.

- **기본값:** `true`

---

### 소켓 버퍼 설정

#### internode_socket_send_buffer_size

노드 간 통신을 위한 소켓 버퍼(socket buffer) 크기입니다. 설정 시 `net.core.wmem_max`에 의해 제한됩니다.

- **기본값:** *(주석 처리됨, 최소 단위: B)*

#### internode_socket_receive_buffer_size

노드 간 통신을 위한 소켓 버퍼 크기입니다. 설정 시 `net.core.rmem_max`에 의해 제한됩니다.

- **기본값:** *(주석 처리됨, 최소 단위: B)*

---

### 백업 및 스냅샷 설정

#### incremental_backups

`true`로 설정하면, Cassandra가 로컬에서 플러시되거나 스트리밍된 각 SSTable에 대해 `backups/` 하위 디렉터리에 하드 링크(hard link)를 생성합니다.

- **기본값:** `false`

#### snapshot_before_compaction

각 컴팩션 전에 스냅샷(snapshot)을 찍을지 여부입니다. 이 옵션을 사용할 때는 주의해야 합니다. Cassandra가 스냅샷을 자동으로 정리해 주지 않기 때문입니다.

- **기본값:** `false`

#### auto_snapshot

키스페이스 트렁케이트(truncation)나 컬럼 패밀리 삭제(drop) 전에 데이터의 스냅샷을 찍을지 여부입니다. 강력히 권장되는 기본값인 `true`를 사용해야 합니다.

- **기본값:** `true`

---

### 컬럼 인덱스 설정

#### column_index_size

파티션 내 행의 콜레이션 인덱스(collation index)의 세분화 단위(granularity)입니다. BIG과 BTI SSTable 포맷 모두에 적용됩니다.

- **기본값:** `4KiB`

#### column_index_cache_size

SSTable별로 인덱싱된 키 캐시 항목 중 이 크기를 초과하는 것은 힙에 유지되지 않습니다.

- **기본값:** `2KiB`

---

### 컴팩션(Compaction) 설정

#### concurrent_compactors

허용할 동시 컴팩션(compaction)의 수입니다. 안티엔트로피 리페어(anti-entropy repair)를 위한 검증 컴팩션(validation compaction)은 포함하지 **않습니다**.

- **기본값:** `1`

#### concurrent_validations

허용할 동시 리페어 검증(repair validation)의 수입니다.

- **기본값:** `0`

#### concurrent_materialized_view_builders

허용할 동시 머티리얼라이즈드 뷰 빌더(builder) 작업의 수입니다.

- **기본값:** `1`

#### compaction_throughput

전체 시스템에 걸쳐 주어진 총 처리량(throughput)으로 컴팩션을 스로틀링합니다.

- **기본값:** `64MiB/s`

#### sstable_preemptive_open_interval

컴팩션할 때, 대체 SSTable이 완전히 작성되기 전에 미리 열려서, 작성된 범위에 대해 이전 SSTable 대신 사용될 수 있습니다.

- **기본값:** `50MiB`

---

### 스트리밍(Streaming) 설정

#### stream_entire_sstables

활성화하면, Cassandra가 노드 간에 적격한(eligible) 전체 SSTable을 제로카피(zero-copy)로 스트리밍할 수 있습니다.

- **기본값:** `true`

#### stream_throughput_outbound

이 노드의 모든 아웃바운드 스트리밍 파일 전송을 주어진 총 처리량으로 스로틀링합니다.

- **기본값:** `24MiB/s`

#### inter_dc_stream_throughput_outbound

데이터센터 간 모든 스트리밍 파일 전송을 스로틀링합니다.

- **기본값:** `24MiB/s`

#### streaming_keep_alive_period

실패한 스트림을 조기에 탐지하기 위한 유휴 상태 제어 메시지(idle state control message)의 주기를 설정합니다.

- **기본값:** `300s`

---

### 요청 타임아웃(Request Timeout) 설정

#### read_request_timeout

코디네이터가 읽기 작업이 완료될 때까지 기다려야 하는 시간입니다. 허용되는 가장 낮은 값은 10ms입니다.

- **기본값:** `5000ms`

#### range_request_timeout

코디네이터가 순차(seq) 또는 인덱스 스캔(index scan)이 완료될 때까지 기다려야 하는 시간입니다. 허용되는 가장 낮은 값은 10ms입니다.

- **기본값:** `10000ms`

#### write_request_timeout

코디네이터가 쓰기가 완료될 때까지 기다려야 하는 시간입니다. 허용되는 가장 낮은 값은 10ms입니다.

- **기본값:** `2000ms`

#### counter_write_request_timeout

코디네이터가 카운터 쓰기가 완료될 때까지 기다려야 하는 시간입니다. 허용되는 가장 낮은 값은 10ms입니다.

- **기본값:** `5000ms`

#### cas_contention_timeout

코디네이터가 동일한 행에 대한 다른 제안(proposal)과 경합하는 CAS 작업을 계속 재시도해야 하는 시간입니다.

- **기본값:** `1000ms`

#### truncate_request_timeout

코디네이터가 트렁케이트(truncate)가 완료될 때까지 기다려야 하는 시간입니다.

- **기본값:** `60000ms`

#### request_timeout

기타 잡다한 작업에 대한 기본 타임아웃입니다. 허용되는 가장 낮은 값은 10ms입니다.

- **기본값:** `10000ms`

---

### 모니터링 및 헬스 설정

#### slow_query_log_timeout

노드가 느린 쿼리(slow query)를 로깅하기까지의 시간입니다. 이 타임아웃을 초과하는 SELECT 쿼리는 집계된 로그 메시지를 생성합니다.

- **기본값:** `500ms`

---

### 청크 캐시 및 버퍼 풀 설정

#### file_cache_enabled

SSTable 청크 캐시(chunk cache)를 활성화합니다. 청크 캐시는 최근에 접근한 SSTable의 섹션을 압축되지 않은 버퍼(uncompressed buffer)로 메모리에 저장합니다.

- **기본값:** `false`

#### file_cache_size

SSTable 청크 캐시 및 버퍼 풀링(buffer pooling)에 사용할 최대 메모리입니다.

- **기본값:** `512MiB`

#### buffer_pool_use_heap_if_exhausted

SSTable 버퍼 풀이 소진(exhausted)되었을 때 힙(on heap)에 할당할지 오프힙(off heap)에 할당할지를 나타내는 플래그입니다.

- **기본값:** `true`

#### networking_cache_size

노드 간 및 클라이언트-서버 네트워킹 버퍼에 사용할 최대 메모리입니다.

- **기본값:** `128MiB`

---

### 스니치(Snitch) 설정

> 아래 항목들은 cassandra.yaml의 표준 설정으로, 클러스터의 네트워크 토폴로지를 인식하고 요청을 효율적으로 라우팅하기 위해 사용됩니다.

#### endpoint_snitch

스니치(snitch)는 Cassandra가 노드들이 어떻게 그룹화되어 있는지, 그리고 어떤 호스트가 서로 "가까운지(close)"를 학습하는 데 사용하는 정책입니다. 이를 통해 요청이 효율적으로 라우팅되고, 복제본을 데이터센터와 랙별로 그룹화하여 균등하게 분산할 수 있습니다. 클러스터가 시작된 후에는 노드 간 토폴로지 정보의 불일치를 방지하기 위해, 모든 노드가 같은 스니치 설정을 사용해야 합니다. 대표적인 값은 다음과 같습니다.

- `SimpleSnitch`: 단일 데이터센터 배포(또는 클라우드의 단일 존)에 적합합니다. 랙이나 데이터센터 정보를 인식하지 않습니다.
- `GossipingPropertyFileSnitch`: 프로덕션에서 권장됩니다. 각 노드의 `cassandra-rackdc.properties`에 설정된 데이터센터/랙 정보를 사용하고, 가십(gossip)을 통해 다른 노드에 전파합니다.
- `PropertyFileSnitch`: `cassandra-topology.properties` 파일을 사용하여 토폴로지를 정의합니다.
- `Ec2Snitch` / `Ec2MultiRegionSnitch`: AWS EC2 환경에 적합합니다.
- `RackInferringSnitch`: IP 주소의 옥텟(octet)으로부터 랙과 데이터센터를 추론합니다.

- **기본값:** `SimpleSnitch`

#### dynamic_snitch_update_interval

동적 스니치(dynamic snitch)가 노드의 점수(score)를 얼마나 자주 다시 계산할지를 정합니다.

- **기본값:** `100ms`

#### dynamic_snitch_reset_interval

동적 스니치가 모든 노드의 점수를 얼마나 자주 리셋할지를 정합니다. 이를 통해 나쁜 상태였던 노드가 다시 복구될 기회를 갖습니다.

- **기본값:** `600000ms`

#### dynamic_snitch_badness_threshold

선호되는(가장 가까운) 노드 대비 어느 정도의 성능 차이가 있어야 동적 스니치가 그 노드를 회피할지를 정하는 임계값입니다. 예를 들어 값이 `0.2`이면, 선호 노드보다 20% 이상 나쁜 경우에만 다른 노드로 라우팅을 전환합니다.

- **기본값:** `0.1` 혹은 `1.0`(버전에 따라 상이)

---

### 노드 간 압축 설정

#### internode_compression

노드 간 트래픽(internode traffic)에 적용할 압축 수준을 제어합니다.

- `all`: 모든 트래픽을 압축합니다.
- `dc`: 서로 다른 데이터센터 간 트래픽만 압축합니다.
- `none`: 압축하지 않습니다.

- **기본값:** `dc`

#### inter_dc_tcp_nodelay

데이터센터 간 통신에서 TCP 패킷에 대해 `TCP_NODELAY`(Nagle 알고리즘 비활성화)를 적용할지 여부입니다. 이를 비활성화하면 더 큰 패킷으로 통합되어 데이터센터 간 처리량은 향상되지만 지연 시간이 늘어날 수 있습니다.

- **기본값:** `false`

---

### 암호화(Encryption) 설정

#### server_encryption_options

노드 간 통신(internode communication)에 대한 암호화 옵션입니다. 주요 하위 옵션은 다음과 같습니다.

- `internode_encryption`: 노드 간 암호화 적용 범위입니다. `none`, `all`, `dc`(데이터센터 간만), `rack`(랙 간만) 중 하나입니다.
- `keystore`: 키스토어 파일 경로입니다(예: `conf/.keystore`).
- `keystore_password`: 키스토어 비밀번호입니다.
- `truststore`: 트러스트스토어 파일 경로입니다(예: `conf/.truststore`).
- `truststore_password`: 트러스트스토어 비밀번호입니다.
- `protocol`: 사용할 보안 프로토콜입니다(예: `TLS`).
- `cipher_suites`: 허용할 암호 스위트(cipher suite) 목록입니다.
- `require_client_auth`: 노드 간 상호 인증(클라이언트 인증서)을 요구할지 여부입니다.

#### client_encryption_options

클라이언트-서버(네이티브 트랜스포트) 통신에 대한 암호화 옵션입니다. 주요 하위 옵션은 다음과 같습니다.

- `enabled`: 클라이언트 암호화를 활성화할지 여부입니다.
- `optional`: 암호화/비암호화 연결을 모두 허용할지 여부입니다.
- `keystore`: 키스토어 파일 경로입니다.
- `keystore_password`: 키스토어 비밀번호입니다.
- `require_client_auth`: 클라이언트 인증서를 요구할지 여부입니다.

---

### 가드레일 및 임계값 설정

> 아래 항목들은 운영상 위험한 작업을 경고하거나 차단하기 위한 표준 임계값 설정입니다.

#### tombstone_warn_threshold

단일 쿼리에서 스캔되는 툼스톤(tombstone)의 수가 이 값을 초과하면 경고(WARN) 로그를 남깁니다.

- **기본값:** `1000`

#### tombstone_failure_threshold

단일 쿼리에서 스캔되는 툼스톤의 수가 이 값을 초과하면 쿼리를 중단(abort)시킵니다. 이는 너무 많은 툼스톤으로 인해 노드의 메모리가 고갈되는 것을 방지하기 위함입니다.

- **기본값:** `100000`

#### batch_size_warn_threshold

배치(batch) 크기가 이 값을 초과하면 경고 로그를 남깁니다.

- **기본값:** `5KiB`

#### batch_size_fail_threshold

배치 크기가 이 값을 초과하면 배치를 거부(reject)합니다.

- **기본값:** `50KiB`

#### gc_warn_threshold

GC 일시 정지(pause) 시간이 이 값을 초과하면 경고 로그를 남깁니다. 0으로 설정하면 GC 로깅이 비활성화됩니다.

- **기본값:** `1000ms`

#### max_value_size

단일 컬럼 값(column value)의 최대 크기입니다. 이 값을 초과하는 값을 포함한 SSTable은 손상된(corrupt) 것으로 간주됩니다.

- **기본값:** `256MiB`

---

## cassandra-rackdc.properties

`cassandra-rackdc.properties` 파일은 Apache Cassandra 클러스터의 데이터센터(datacenter)와 랙(rack) 토폴로지 정보를 설정합니다. 이 설정은 여러 스니치 타입에서 효율적인 요청 라우팅과 균형 잡힌 복제본 분산을 가능하게 하기 위해 사용됩니다.

### 이 파일을 사용하는 스니치

세 가지 스니치 옵션이 이 파일을 활용합니다.

- **GossipingPropertyFileSnitch** (프로덕션에 권장되며, 기본값)
- **AWS EC2 단일 리전 스니치(Ec2Snitch)**
- **AWS EC2 멀티 리전 스니치(Ec2MultiRegionSnitch)**

`GossipingPropertyFileSnitch`는 로컬 노드의 `cassandra-rackdc.properties` 파일에 설정된 데이터센터 및 랙 정보를 사용하고, 그 정보를 가십(gossip)을 통해 다른 노드에 전파합니다.

### 설정 파라미터

#### GossipingPropertyFileSnitch 설정

**dc**

데이터센터 이름을 지정합니다(대소문자 구분).

- **기본값:** `DC1`

**rack**

랙 지정 이름을 정의합니다(대소문자 구분).

- **기본값:** `RAC1`

#### AWS EC2 스니치 설정

**ec2_naming_scheme**

데이터센터/랙 명명 규칙을 결정합니다.

- `legacy`: 데이터센터는 가용 영역(availability zone)에서 마지막 문자를 제외한 부분으로 도출되며, 랙은 존의 접미사(suffix)입니다.
- `standard` (기본값): 데이터센터에는 AWS 리전 이름을 사용하고, 랙에는 리전 이름에 존 문자(zone letter)를 더한 값을 사용합니다.

> **중요:** 4.0 이전(pre-4.0) 클러스터를 업그레이드하는 경우에는 반드시 `legacy` 값을 사용해야 합니다.

#### 공통 설정

**prefer_local**

활성화하면, 데이터센터 내(intra-datacenter) 통신에 로컬 또는 내부 IP 주소(internal IP addressing)를 사용합니다.

- **기본값:** `true` *(기본적으로 주석 처리됨)*

### 예시

```properties
# GossipingPropertyFileSnitch용
dc=DC1
rack=RAC1

# EC2 스니치용 (선택)
# ec2_naming_scheme=standard

# 데이터센터 내 내부 IP 사용 (선택)
# prefer_local=true
```

---

## cassandra-topology.properties

`cassandra-topology.properties` 파일은 `PropertyFileSnitch` 스니치 옵션과 함께 동작하며, 클러스터 노드들이 어떤 데이터센터와 랙에 속하는지를 정의합니다. 스니치는 네트워크 토폴로지(랙과 데이터센터에 의한 근접성)를 결정하여 요청이 효율적으로 라우팅되도록 하고, 데이터베이스가 복제본을 균등하게 분산할 수 있도록 합니다.

### 핵심 요구사항

- **모든 노드에 동일 적용**: 이 파일은 클러스터의 모든 노드에 동일하게 복사되어야 합니다.
- **대소문자 구분**: 데이터센터와 랙 이름은 대소문자를 구분합니다.
- **전체 노드 목록**: 클러스터의 모든 노드를 properties 파일에 포함해야 합니다.
- **명명 일관성**: 데이터센터 이름은 키스페이스 정의(keyspace definition)에 명시된 대로 정의해야 합니다.

### 설정 형식

이 파일은 IP 주소를 데이터센터 및 랙 할당에 매핑하는 단순한 키-값 구조를 사용합니다.

```
IP_ADDRESS=DATACENTER:RACK
```

### 예시 설정

문서에서 제공하는 예시는 세 개의 데이터센터를 보여줍니다.

```properties
# DC1에 할당된 IP들 (RAC1, RAC2)
175.56.12.105=DC1:RAC1
175.50.13.200=DC1:RAC1
175.54.35.197=DC1:RAC1
120.53.24.101=DC1:RAC2
120.55.16.200=DC1:RAC2
120.57.102.103=DC1:RAC2

# DC2에 할당된 IP들 (RAC1, RAC2)
110.56.12.120=DC2:RAC1
110.50.13.201=DC2:RAC1
110.54.35.184=DC2:RAC1
50.33.23.120=DC2:RAC2
50.45.14.220=DC2:RAC2
50.17.10.203=DC2:RAC2

# DC3에 할당된 IP들 (RAC1)
172.106.12.120=DC3:RAC1
172.106.12.121=DC3:RAC1

# 알 수 없는 노드를 위한 default 항목
default=DC3:RAC1
```

이 구조는 클러스터 토폴로지 전반에 걸쳐 적절한 복제본 분산과 효율적인 요청 라우팅을 가능하게 합니다.

---

## cassandra-env.sh

`cassandra-env.sh`는 정적 설정 파일을 수정하지 않고도 Cassandra에 추가적인 JVM 옵션을 전달할 수 있게 해주는 bash 스크립트입니다. 힙 크기 등 일부 값을 시스템 특성에 따라 동적으로 계산해야 할 때 특히 유용합니다.

### 주요 사용 사례

JVM 설정을 계산(computation)해야 할 때는 이 파일이 적합하며, 정적(static) 설정에는 `cassandra-jvm-options` 파일을 사용하는 것이 좋습니다.

### 주요 설정 옵션

이 파일은 Cassandra 시작 동작을 제어하는 다양한 `-D` 파라미터를 지원합니다.

#### 부트스트랩 및 클러스터링

- `cassandra.auto_bootstrap=false`: 초기 클러스터 설정 시 자동 부트스트랩을 비활성화합니다.
- `cassandra.join_ring=true|false`: 노드가 클러스터에 합류할지 여부를 제어합니다.
- `cassandra.load_ring_state=true|false`: 비활성화하면 재시작 시 가십 상태(gossip state)를 초기화합니다.

#### 성능 및 리소스

- `cassandra.available_processors=<number>`: 다중 인스턴스 배포에서 CPU 프로세서 할당 수를 지정합니다.
- `cassandra.prepared_statements_cache_size_in_bytes=<size>`: 준비된 구문 캐시 크기를 설정합니다.

#### 네트워크 및 포트

- `cassandra.native_transport_port=<port>`: CQL 포트입니다(기본값: 9042).
- `cassandra.storage_port=<port>`: 노드 간 통신 포트입니다(기본값: 7000).
- `cassandra.ssl_storage_port=<port>`: 암호화 통신 포트입니다(기본값: 7001).

#### 노드 운영

- `cassandra.replace_address=<address>`: 죽은(dead) 노드를 교체(replace)합니다.
- `cassandra.ring_delay_ms=<ms>`: 링에 정식으로 합류하기 전의 지연 시간입니다(기본값: 1000ms).

#### 기타 설정

- `cassandra.write_survey=true`: 컴팩션/압축 전략 테스트를 활성화합니다.
- `cassandra.boot_without_jna=true`: JNA 초기화가 실패하더라도 시작을 허용합니다.

---

## jvm-* 옵션 파일

`jvm-*` 파일은 Cassandra에서 JVM 설정을 위한 기본 설정 메커니즘으로, 3.0 이전 버전에서 사용되던 구식 `cassandra-env.sh` 접근 방식을 대체합니다.

### 주요 파일

#### 서버 설정용

- `jvm-server.options` (기본 파일)
- `jvm8-server.options` (Java 8 전용)
- `jvm11-server.options` (Java 11 전용)

#### 클라이언트 도구용

- `jvm-clients.options` (기본 파일)
- `jvm8-clients.options` (Java 8 전용)
- `jvm11-clients.options` (Java 11 전용)

### 설정 유형

이 파일들은 다음을 포함합니다.

- 시작 파라미터(startup parameters)
- 가비지 컬렉션(garbage collection) 등 일반 JVM 설정
- 힙(heap) 설정

클라이언트용 변형(variant)은 `nodetool`이나 `sstable` 유틸리티 같은 도구를 위한 JVM 옵션을 설정합니다.

### 핵심 차이점

`jvm-*` 파일은 **정적(static) JVM 설정만** 저장하는 반면, 레거시 `cassandra-env.sh` bash 스크립트는 JVM 설정을 시스템 설정에 따라 동적으로 계산해야 할 때 여전히 유용합니다.

---

## logback.xml

`logback.xml` 파일은 선택적인(optional) Apache Cassandra 설정 파일로, `system.log`와 `debug.log`에 기록되는 항목들의 로깅 레벨을 제어합니다. 로깅 레벨은 `nodetool setlogginglevels`를 사용해서도 설정할 수 있습니다.

### 주요 어펜더(Appender)

이 설정은 네 가지 주요 어펜더 타입을 지원합니다.

- **SYSTEMLOG**: WARN과 ERROR 메시지를 지정된 파일에 동기적(synchronously)으로 기록합니다.
- **DEBUGLOG**: DEBUG 메시지를 파일에 동기적으로 기록합니다.
- **ASYNCDEBUGLOG**: DEBUG 메시지를 파일에 비동기적(asynchronously)으로 기록합니다.
- **STDOUT**: 모든 메시지를 사람이 읽을 수 있는 형식으로 콘솔에 출력합니다.

### 핵심 설정 요소

#### 로깅 레벨

이 파일은 여섯 가지 레벨(ALL, TRACE, DEBUG, INFO, WARN, ERROR)을 지원하며, TRACE가 가장 상세하고 ERROR가 가장 덜 상세합니다. 기본 레벨은 `INFO`입니다.

#### 롤링 정책(Rolling Policy)

이 설정은 파일 크기와 시간 간격을 기준으로 로그 로테이션을 관리하기 위해 `SizeAndTimeBasedRollingPolicy`를 사용합니다.

#### 파일 명명 패턴

아카이브는 `system.log.%d{yyyy-MM-dd}.%i.zip`와 같은 패턴을 사용하며, 매일(daily) 롤오버되고 인덱스가 붙은 압축(zip) 파일로 저장됩니다.

### 가상 테이블 로깅(Virtual Table Logging)

로그는 `VirtualTableAppender`(CQLLOG)를 사용하여 `system_views.system_log` 가상 테이블로 라우팅될 수 있습니다. 이 테이블은 기본적으로 50,000개 행으로 제한되며, 절대 최대값은 100,000개 행입니다. `cassandra.virtual.logs.max.rows` 속성이 이 용량을 제어하며, 메모리를 보존하기 위해 WARN 레벨 필터를 사용하는 것이 권장됩니다.

CQLLOG 어펜더는 기본적으로 주석 처리되어 있어, 설정에서 명시적으로 활성화해야 합니다.

---

## 단위(Unit)와 분리된 파라미터 이름

Apache Cassandra 4.1부터 많은 `cassandra.yaml` 파라미터의 이름이 단위(unit)와 분리되었습니다. 과거에는 `read_request_timeout_in_ms`처럼 파라미터 이름 자체에 단위가 포함되어 있었습니다. 새로운 명명 규칙에서는 파라미터 이름에서 단위 접미사가 제거되고(`read_request_timeout`), 대신 값에 단위를 직접 명시합니다.

예를 들면 다음과 같습니다.

```yaml
# 기존 방식 (레거시)
read_request_timeout_in_ms: 5000

# 새로운 방식
read_request_timeout: 5000ms
```

지원되는 단위는 다음과 같습니다.

- 시간(time): `ms`(밀리초), `s`(초), `m`(분), `h`(시간), `d`(일)
- 데이터 크기(data size): `B`(바이트), `KiB`(키비바이트), `MiB`(메비바이트), `GiB`(기비바이트)
- 처리량(rate): `MiB/s` 등

이 변경은 단위를 명시적으로 만들어 설정의 가독성을 높이고 오해를 줄이기 위한 것입니다. 레거시 형식의 파라미터 이름도 하위 호환성을 위해 일정 기간 지원됩니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Configuration 섹션](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/)
- [cassandra.yaml 파일](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/cass_yaml_file.html)
- [cassandra-rackdc.properties 파일](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/cass_rackdc_file.html)
- [cassandra-env.sh 파일](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/cass_env_sh_file.html)
- [cassandra-topology.properties 파일](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/cass_topo_file.html)
- [logback.xml 파일](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/cass_logback_xml_file.html)
- [jvm-* 옵션 파일](https://cassandra.apache.org/doc/latest/cassandra/managing/configuration/cass_jvm_options_file.html)
