# Cassandra 시작하기, 아키텍처, 데이터 모델링

## Cassandra 시작하기

> 원본: https://cassandra.apache.org/doc/latest/cassandra/getting-started/

설치, 설정, 데이터 삽입 및 조회, 클라이언트 드라이버(driver), 프로덕션(production) 권장 사항을 차례로 설명합니다.

---

### 목차

1. [설치](#설치)
2. [설정](#설정)
3. [데이터 삽입 및 조회](#데이터-삽입-및-조회)
4. [클라이언트 드라이버](#클라이언트-드라이버)
5. [프로덕션 권장 사항](#프로덕션-권장-사항)
6. [참고 자료](#참고-자료)

---

### 설치

Apache Cassandra는 여러 가지 방식으로 설치할 수 있습니다. 가장 빠르게 사용해 보려면 Docker 이미지를 이용하고, 단일 노드(node)에서 실험하거나 root 권한 없이 설치하려면 바이너리 tarball을 사용하며, 운영 체제 패키지 관리자와 통합하려면 Debian 또는 RPM 패키지를 사용합니다.

#### 사전 요구 사항(Prerequisites)

**Java**

- Cassandra를 실행하려면 Java가 필요합니다. Oracle Java Standard Edition 또는 OpenJDK 중 하나를 사용할 수 있습니다.
- Java 8 또는 Java 11을 설치합니다. Java 11에 대한 실험적(experimental) 지원은 Cassandra 4.0에서 추가되었으며, Cassandra 4.0.2부터 정식 지원됩니다.
- 다음 명령으로 설치 여부를 확인합니다.

```bash
java -version
```

**Python**

- `cqlsh`(CQL 셸)를 사용하려면 Python이 필요합니다.
- Python 3.6 이상을 권장합니다. (Python 2.7은 사용 가능하지만 deprecated 상태입니다.)
- 다음 명령으로 설치 여부를 확인합니다.

```bash
python --version
```

---

#### Docker로 설치

가장 빠르게 Cassandra를 사용해 볼 수 있는 방법입니다. 사전에 Docker(macOS/Windows의 Docker Desktop, 또는 Linux의 docker)가 설치되어 있어야 합니다.

1. 이미지를 받습니다(pull).

```bash
docker pull cassandra:latest
```

2. Cassandra를 시작합니다.

```bash
docker run --name cass_cluster cassandra:latest
```

3. CQL 셸에 접속합니다.

```bash
docker exec -it cass_cluster cqlsh
```

---

#### 바이너리 tarball로 설치

tarball 설치는 모든 파일을 하나의 위치에 압축 해제하며, 바이너리와 설정 파일이 하위 디렉터리에 위치합니다. root 권한이 필요하지 않습니다.

1. Apache Cassandra 다운로드 사이트(또는 미러)에서 바이너리 tarball을 내려받습니다.

```bash
curl -OL https://www.apache.org/dyn/closer.lua/cassandra/4.0.0/apache-cassandra-4.0.0-bin.tar.gz
```

2. 압축을 풉니다.

```bash
tar xzvf apache-cassandra-4.0.0-bin.tar.gz
```

3. 설치 디렉터리 구조는 다음과 같습니다.

```
<tarball_installation>/
    bin/          (cassandra, cqlsh, nodetool 등 명령어)
    conf/         (cassandra.yaml 등 설정 파일)
    data/         (commit log, hints, SSTable)
    logs/         (system.log, debug.log 등 로그)
    tools/        (cassandra-stress 등 도구)
```

4. 서비스를 시작합니다.

```bash
cd apache-cassandra-4.0.0/ && bin/cassandra
```

5. 시작 과정을 로그로 확인합니다.

```bash
tail -f logs/system.log
```

아래 로그가 나타나면 클라이언트 연결을 받을 준비가 된 것입니다.

```
Starting listening for CQL clients on localhost/127.0.0.1:9042
```

---

#### Debian 패키지로 설치

1. 저장소(repository)를 추가합니다. (4.0 계열은 `40x`를 사용합니다.)

```bash
echo "deb [signed-by=/etc/apt/keyrings/apache-cassandra.asc] https://debian.cassandra.apache.org 40x main" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list
```

2. 저장소 키(key)를 추가합니다.

```bash
curl -o /etc/apt/keyrings/apache-cassandra.asc https://downloads.apache.org/cassandra/KEYS
```

3. 패키지 목록을 갱신하고 설치합니다.

```bash
sudo apt-get update
sudo apt-get install cassandra
```

---

#### RPM 패키지로 설치

1. root 권한으로 `/etc/yum.repos.d/cassandra.repo` 파일에 저장소를 추가합니다.

```
[cassandra]
name=Apache Cassandra
baseurl=https://redhat.cassandra.apache.org/40x/
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://downloads.apache.org/cassandra/KEYS
```

2. 설치합니다.

```bash
sudo yum update
sudo yum install cassandra
```

3. 서비스를 시작합니다.

```bash
sudo service cassandra start
```

---

#### 설치 확인

`nodetool`로 노드 상태를 확인합니다.
```bash
bin/nodetool status
```

`Status` 열이 `UN`(Up/Normal)으로 표시되면 노드가 정상 동작 중입니다.

CQL 셸로 데이터베이스에 접속합니다.

```bash
bin/cqlsh
```

---

### 설정

Cassandra의 동작은 여러 설정 파일로 제어하며, 파일 위치는 설치 방식에 따라 다릅니다.

#### 설정 파일 위치

- **Docker**: `/etc/cassandra` 디렉터리
- **tarball**: 설치 위치 내부의 `conf` 디렉터리
- **패키지(Package)**: `/etc/cassandra` 디렉터리

#### 주요 설정 파일

| 파일 | 설명 |
| --- | --- |
| `cassandra.yaml` | 주(main) 설정 파일입니다. 민감한 설정을 담고 있으므로 신뢰할 수 있는 사용자만 접근하도록 제한해야 합니다. |
| `cassandra-env.sh` | 힙 크기(heap size), JVM 명령줄 인자 등 JVM 수준의 환경 변수를 설정합니다. |
| `cassandra-rackdc.properties` 또는 `cassandra-topology.properties` | 클러스터 노드의 랙(rack)과 데이터센터(datacenter) 정보를 지정합니다. |
| `logback.xml` | 로깅 설정과 로깅 레벨을 제어합니다. |
| `jvm-*` 파일 | 서버 및 클라이언트용 JVM 설정 파일들입니다. |
| `commitlog_archiving.properties` | commitlog의 아카이빙(archiving) 파라미터를 설정합니다. |
| `cqlshrc.sample` | CQL 셸(`cqlsh`)을 위한 설정 템플릿입니다. |

#### cassandra.yaml의 주요 런타임 속성

- **`cluster_name`**: 클러스터(cluster)를 식별하는 이름입니다.
- **`seeds`**: 클러스터의 시드 노드(seed node) IP 주소를 쉼표로 구분한 목록입니다.
- **`storage_port`**: 기본값은 `7000`입니다. 방화벽에서 이 포트로의 접근을 허용하는지 확인해야 합니다. (노드 간 통신에 사용됩니다.)
- **`listen_address`**: 노드 간 통신에 사용할 IP 주소이며, 기본값은 localhost입니다.
- **`listen_interface`**: `listen_address` 대신 네트워크 인터페이스를 지정할 때 사용하는 대안 속성입니다.
- **`native_transport_port`**: 기본값은 `9042`입니다. `cqlsh`를 비롯한 클라이언트 연결에 사용됩니다.

#### 디렉터리 위치 속성

- **`data_file_directories`**: SSTable과 데이터 파일의 위치입니다.
- **`commitlog_directory`**: commitlog 파일을 저장하는 위치입니다.
- **`saved_caches_directory`**: 저장된 캐시(cache)의 위치입니다.
- **`hints_directory`**: hints 파일을 저장하는 위치입니다.

> 성능 팁: 디스크가 여러 개라면 commitlog와 데이터 파일을 서로 다른 디스크에 두는 것을 고려하십시오.

#### 기본 로깅 동작

logback 로거의 기본 동작은 다음과 같습니다.

- `system.log`에 INFO 레벨로 기록
- `debug.log`에 DEBUG 레벨로 기록
- 포그라운드(foreground) 실행 시 콘솔에 INFO 레벨로 출력

로깅 설정은 `logback.xml`을 수정하거나 `nodetool setlogginglevel` 명령으로 변경할 수 있습니다.

---

### 데이터 삽입 및 조회

Cassandra와 상호작용하는 기본 API는 **CQL**(Cassandra Query Language)이며, 다음 세 가지 방법으로 사용할 수 있습니다.

1. **cqlsh** — 명령줄 셸(command-line shell)
2. **클라이언트 드라이버(client drivers)** — 커뮤니티에서 제공하는 라이브러리
3. **Apache Zeppelin** — 노트북(notebook) 형태의 인터페이스

#### cqlsh 사용

`cqlsh`는 모든 Cassandra 배포판에 포함된 명령줄 도구로, `bin` 디렉터리에 있습니다. 노드에 접속하려면 다음과 같이 실행합니다.

```bash
$ bin/cqlsh localhost
```

노드를 지정하지 않으면 `localhost`에 접속합니다. 접속 후에는 CQL 명령을 대화형(interactive)으로 실행할 수 있습니다.

#### 예시 세션

```
$ bin/cqlsh localhost
Connected to Test Cluster at localhost:9042.
[cqlsh 5.0.1 | Cassandra 3.8 | CQL spec 3.4.2 | Native protocol v4]
Use HELP for help.
cqlsh> SELECT cluster_name, listen_address FROM system.local;

 cluster_name | listen_address
--------------+----------------
 Test Cluster |      127.0.0.1

(1 rows)
```

`system.local` 테이블에서 클러스터 이름(`cluster_name`)과 수신 주소(`listen_address`)를 조회해 클러스터 메타데이터를 확인하는 예시입니다.

#### 그 밖의 자료

`CREATE KEYSPACE`, `CREATE TABLE`, `INSERT`, `SELECT` 등 CQL 전체 문법은 [CQL 레퍼런스 섹션](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/ddl.html)을 참고하십시오. 사용하려는 언어에 맞는 클라이언트 드라이버 문서도 함께 확인하는 것이 좋습니다.

---

### 클라이언트 드라이버

커뮤니티에서 다양한 언어의 클라이언트 드라이버를 제공합니다. 드라이버를 선택하기 전에 지원하는 Cassandra 버전과 기능을 반드시 확인해야 합니다.

언어별로 알려진 드라이버 목록은 다음과 같습니다.

| 언어 | 드라이버 |
| --- | --- |
| **Java** | Achilles, Astyanax, Casser, Datastax Java driver, Kundera, PlayORM |
| **Python** | Datastax Python driver |
| **Ruby** | Datastax Ruby driver |
| **C# / .NET** | Cassandra Sharp, Datastax C# driver, Fluent Cassandra |
| **Node.js** | Datastax Nodejs driver |
| **PHP** | CQL \| PHP, Datastax PHP driver, PHP-Cassandra, PHP Library for Cassandra |
| **C++** | Datastax C++ driver, libQTCassandra |
| **Scala** | Datastax Spark connector, Phantom, Quill |
| **Clojure** | Alia, Cassaforte, Hayt |
| **Erlang** | CQerl, Erlcass |
| **Go** | CQLc, Gocassa, GoCQL |
| **Haskell** | Cassy |
| **Rust** | Rust CQL |
| **Perl** | Cassandra::Client, DBD::Cassandra |
| **Elixir** | Xandra, CQEx |
| **Dart** | dart_cassandra_cql |

---

### 프로덕션 권장 사항

프로덕션 클러스터를 구축할 때는 단일 노드 개발 환경과 다른 여러 사항을 고려해야 합니다.

#### 토큰(Tokens)과 가상 노드(vnodes)

토큰 개수(`num_tokens`)는 클러스터의 탄력성(elasticity)과 가용성(availability) 사이의 균형을 결정합니다. `cassandra.yaml`에서 설정합니다.

- **1 token**: 가용성과 클러스터 최대 규모가 최대이고 피어(peer) 수가 가장 적지만, 확장이 유연하지 못합니다.
- **4 tokens**: 최종적으로 30개를 초과하는 클러스터에 권장됩니다. 균형을 위해 약 20%의 추가 노드가 필요합니다.
- **8 tokens**: 성능에 거의 영향 없이 워크로드를 분산합니다.
- **16 tokens**: 탄력적으로 확장·축소하는 클러스터에 가장 적합하지만, 50개 이상의 노드 클러스터에서는 문제가 생길 수 있습니다.

균등한 토큰 할당을 위해 `allocate_tokens_for_local_replication_factor`를 복제 인수에 맞게 설정해야 합니다.

#### 리드 어헤드(Read Ahead)

리드 어헤드는 하드웨어 유형에 따라 최적값이 다릅니다.

- **회전식 디스크(spinning disk)**: 64KB로 시작하는 것을 권장합니다.
- **SSD**: 4KB로 시작하는 것을 권장합니다.

파티션이 작거나 단일 파티션 키(single-partition-key) 테이블에서는 리드 어헤드가 오히려 역효과를 내어, 키/값(key/value) 워크로드에서 지연 시간(latency)과 처리량(throughput)을 최대 5배까지 저하시킬 수 있습니다.

리눅스 `blockdev` 도구로 조정하며, 인자의 단위는 512바이트 섹터(sector)입니다.

#### 압축(Compression)

기본 압축 알고리즘은 `LZ4Compressor`입니다. `chunk_length_in_kb` 파라미터(Cassandra 4.0부터 기본값 16KB)는 성능에 크게 영향을 미치며, 청크(chunk)가 지나치게 크면 읽기 증폭(read amplification)이 발생합니다.

#### 컴팩션(Compaction)

컴팩션 전략(strategy)은 워크로드 특성에 따라 선택합니다. 같은 클러스터 안에서도 테이블마다 서로 다른 전략을 적용할 수 있습니다.

#### 암호화 및 보안(Encryption & Security)

프로덕션 클러스터 구축 시 노드 간(peer-to-peer) 암호화와 클라이언트-서버(client-server) 암호화를 초기부터 함께 설정해야 합니다. 배포 이후에 적용하면 다운타임 위험이 따릅니다.

#### 복제 전략(Replication Strategy)

프로덕션 클러스터는 반드시 `NetworkTopologyStrategy`를 사용해야 하며, `SimpleStrategy`는 사용하지 않아야 합니다. `NetworkTopologyStrategy`는 다중 랙(multi-rack) 및 다중 데이터센터(multi-datacenter) 구성을 지원합니다.

#### 스니치(Snitch) 설정

클러스터 프로비저닝 이후에 랙을 변경하는 작업은 지원되지 않으므로, 처음부터 올바르게 구성해야 합니다.

- **온프레미스 또는 혼합(hybrid) 환경**: `GossipingPropertyFileSnitch` 권장
- **AWS 전용 배포**: `Ec2Snitch` 권장

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Getting Started 섹션](https://cassandra.apache.org/doc/latest/cassandra/getting-started/)
- [Installing Cassandra](https://cassandra.apache.org/doc/latest/cassandra/installing/installing.html)
- [Configuring Cassandra](https://cassandra.apache.org/doc/latest/cassandra/getting-started/configuring.html)
- [Inserting and Querying](https://cassandra.apache.org/doc/latest/cassandra/getting-started/querying.html)
- [Client Drivers](https://cassandra.apache.org/doc/latest/cassandra/getting-started/drivers.html)
- [Production Recommendations](https://cassandra.apache.org/doc/latest/cassandra/getting-started/production.html)
- [CQL 레퍼런스](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/)
- [Apache Cassandra 다운로드](https://cassandra.apache.org/download/)

---

## Cassandra 아키텍처

> 원본: https://cassandra.apache.org/doc/latest/cassandra/architecture/

---

### 목차

1. [개요](#개요)
2. [Dynamo 기반 아키텍처](#dynamo-기반-아키텍처)
   - [데이터셋 분할: 일관성 해싱](#데이터셋-분할-일관성-해싱)
   - [멀티 마스터 복제](#멀티-마스터-복제)
   - [튜닝 가능한 일관성 수준](#튜닝-가능한-일관성-수준)
   - [클러스터 멤버십과 장애 감지](#클러스터-멤버십과-장애-감지)
   - [점진적 확장](#점진적-확장)
3. [스토리지 엔진](#스토리지-엔진)
   - [커밋 로그](#커밋-로그)
   - [멤테이블](#멤테이블)
   - [SSTable](#sstable)
4. [보장 사항](#보장-사항)
5. [스니치](#스니치)
6. [참고 자료](#참고-자료)

---

### 개요

Apache Cassandra는 오픈 소스 분산 NoSQL 데이터베이스(distributed NoSQL database)입니다. 분할된 와이드 컬럼 스토리지 모델(partitioned wide-column storage model)을 구현하며 최종 일관성(eventually consistent) 시맨틱을 제공합니다.

Cassandra는 Facebook에서 시작되었으며, 두 가지 영향력 있는 시스템의 설계 원칙을 결합했습니다. 분산 스토리지와 복제(distributed storage and replication)는 Amazon의 Dynamo에서, 데이터 모델과 스토리지 엔진 아키텍처는 Google의 Bigtable에서 가져왔습니다. 시스템은 스테이지 기반 이벤트 구동 아키텍처(staged event-driven architecture, SEDA)를 채택합니다.

#### 설계 목표

Cassandra는 다음과 같은 핵심 요구 사항을 충족하도록 설계되었습니다.

- 완전한 멀티 프라이머리 데이터베이스 복제(full multi-primary database replication)
- 낮은 지연 시간의 읽기/쓰기를 갖춘 글로벌 가용성(global availability)
- 범용 하드웨어(commodity hardware)에서의 수평 확장(horizontal scaling)
- 프로세서를 추가할 때마다 선형적으로 증가하는 처리량(linear throughput)
- 온라인 부하 분산(online load balancing) 및 클러스터 확장
- 파티션 키 기반 쿼리(partitioned key-oriented query) 지원
- 유연한 스키마 설계(flexible schema design)

#### 주요 기능

**데이터 구성 (CQL을 통한):**

Cassandra는 다음 단위로 정보를 구성합니다.

- **키스페이스(keyspace)**: 복제 설정을 담는 컨테이너
- **테이블(table)**: 행과 컬럼 구조
- **파티션(partition)**: 노드 위치를 결정하는 기본 키(primary key)의 구성 요소
- **행(row)**: 고유한 기본 키로 식별되는 컬럼들의 집합
- **컬럼(column)**: 타입이 지정된 개별 데이터 요소

**고급 기능:**

컬렉션 타입(collection types), 사용자 정의 타입(user-defined types), 스토리지 연결 인덱싱(storage-attached indexing, SAI), 로컬 보조 인덱스(local secondary indexes), 원자적 시맨틱을 갖춘 경량 트랜잭션(lightweight transactions), 구체화된 뷰(materialized views)를 지원합니다.

**의도적인 제한:**

Cassandra는 높은 가용성과 성능을 유지하기 위해 교차 파티션 트랜잭션(cross-partition transactions), 분산 조인(distributed joins), 외래 키 제약 조건(foreign key constraints)을 의도적으로 제외합니다.

#### 운영

설정은 `cassandra.yaml` 파일을 통해 이루어집니다. 관리 도구로는 런타임 클러스터 제어를 위한 `nodetool`과 함께 감사 로깅(audit logging), 쿼리 분석, 스냅샷(snapshots), 증분 백업(incremental backups)을 위한 유틸리티가 있습니다.

---

### Dynamo 기반 아키텍처

Apache Cassandra는 Amazon의 Dynamo 분산 스토리지 시스템에서 여러 핵심 기법을 채택했습니다. 각 노드는 세 가지 주요 구성 요소를 포함합니다: 요청 코디네이션(request coordination), 링 멤버십 및 장애 감지(ring membership/failure detection), 로컬 영속성(local persistence). Cassandra는 Dynamo의 클러스터링 메커니즘을 활용하면서 자체적인 LSM(Log Structured Merge) 트리 스토리지 엔진을 구현합니다.

시스템은 Dynamo에서 파생된 네 가지 핵심 기법에 의존합니다.

- 데이터셋 분할을 위한 일관성 해싱(consistent hashing)
- 버전이 매겨진 데이터와 튜닝 가능한 일관성을 갖춘 멀티 마스터 복제(multi-master replication)
- 가십 프로토콜(gossip protocol)을 통한 분산 클러스터 멤버십
- 표준 하드웨어에서의 점진적 확장(incremental scaling)

#### 데이터셋 분할: 일관성 해싱

단순한 해시 기반 버킷팅(naive hash-based bucketing) 대신, Cassandra는 일관성 해싱(consistent hashing)을 사용합니다. 이 방식에서 "모든 노드는 연속적인 해시 링(continuous hash ring) 위의 하나 이상의 토큰(token)에 매핑됩니다." 노드가 추가되거나 제거될 때, 키 중 극히 일부만 재매핑하면 되므로 매끄러운 클러스터 확장이 가능합니다.

##### 토큰 링 아키텍처

"토큰 범위(token range)라고도 불리는 키 범위들은 동일한 물리적 노드 집합에 매핑됩니다." 복제 계수(replication factor) 3을 가진 8개 노드 클러스터에서, 각 토큰 범위는 서로 다른 3개의 노드에 복제되어 단일 장애 지점(single point of failure)을 방지합니다.

##### 가상 노드 (vnodes)

클러스터 균형을 개선하기 위해 Cassandra는 물리적 노드당 여러 개의 토큰을 할당합니다. 이 접근 방식은 세 가지 이점을 제공합니다.

1. 새 노드가 거의 동일한 양의 데이터 분포를 받습니다.
2. 제거(decommission)된 노드가 복제본 전반에 걸쳐 데이터를 고르게 잃습니다.
3. 사용 불가능한 노드가 쿼리 부하를 고르게 분산합니다.

그러나 여러 토큰을 사용하는 것은 트레이드오프를 동반합니다. 링 위에서 이웃 관계(neighbor relationship)가 늘어나면, 여러 노드가 동시에 장애를 일으킬 때 중단(outage) 확률이 높아지고, 클러스터 유지 보수 작업이 그에 비례하여 느려집니다.

#### 멀티 마스터 복제

##### 복제 계수와 전략

"모든 데이터 파티션은 높은 가용성과 내구성(durability)을 유지하기 위해 클러스터 전반의 여러 노드에 복제됩니다." 복제 계수(replication factor)는 존재하는 복사본의 수를 결정합니다. Cassandra는 플러그 가능한(pluggable) 복제 전략을 지원합니다.

- **NetworkTopologyStrategy** (운영 환경 권장): 데이터센터별로 복제 계수를 지정하며, 각 데이터센터 내에서 서로 다른 랙(rack)에 복제본을 분산하려고 시도합니다.
- **SimpleStrategy** (테스트 전용): 모든 노드를 동일하게 취급하며, 데이터센터와 랙 구성을 무시합니다.

##### 데이터 버전 관리

Cassandra는 "최종 쓰기 우선(last-write-wins)" 충돌 해결 방식을 구현합니다. "시스템에 들어오는 모든 변경(mutation)은 타임스탬프(timestamp)와 함께 들어옵니다." 이 접근 방식은 Dynamo의 벡터 클록(vector clock)보다 단순하며, NTP를 통한 적절한 시간 동기화에 의존합니다.

##### 복제본 동기화

세 가지 메커니즘이 복제본의 수렴(convergence)을 담당합니다.

1. **읽기 복구(Read Repair)**: 읽기 시점의 최선 노력(best-effort) 동기화
2. **힌티드 핸드오프(Hinted Handoff)**: 쓰기 시점의 최선 노력 전달
3. **안티 엔트로피 복구(Anti-Entropy Repair)**: 머클 트리(Merkle tree)를 사용하여 복제본 간 불일치 데이터를 식별하고 해결

Cassandra는 Dynamo의 복구 기능을 서브 레인지(sub-range) 복구와 증분(incremental) 복구 옵션으로 확장합니다.

#### 튜닝 가능한 일관성 수준

Cassandra는 "일관성 수준(Consistency Level)을 통해 작업별로 일관성과 가용성 사이의 트레이드오프"를 제공합니다. 이는 Dynamo의 `R + W > N` 원칙을 구현한 것입니다.

| 수준(Level) | 동작 |
|-------------|------|
| `ONE`, `TWO`, `THREE` | 지정된 수의 복제본이 응답 |
| `QUORUM` | 복제본의 과반수(`n/2 + 1`)가 응답 |
| `ALL` | 모든 복제본이 응답 |
| `LOCAL_QUORUM` | 로컬 데이터센터 내 과반수가 응답 |
| `EACH_QUORUM` | 각 데이터센터의 과반수가 응답 |
| `LOCAL_ONE` | 로컬 복제본 하나가 응답 |
| `ANY` | 복제본 하나 또는 코디네이터 힌트(쓰기 전용) |

"쓰기 작업은 일관성 수준과 관계없이 항상 모든 복제본으로 전송됩니다." 일관성 수준은 단지 코디네이터(coordinator)가 몇 개의 응답을 기다릴지를 제어할 뿐입니다.

#### 클러스터 멤버십과 장애 감지

##### 가십 프로토콜

"가십(Gossip)은 Cassandra가 엔드포인트 멤버십(endpoint membership)과 노드 간 네트워크 프로토콜 버전 같은 기본적인 클러스터 부트스트래핑 정보를 전파하는 방식입니다." 모든 노드는 매초 독립적으로 가십 작업을 실행합니다.

1. 로컬 하트비트(heartbeat) 상태를 갱신합니다.
2. 클러스터 내 무작위 노드와 상태를 교환합니다.
3. 확률적으로 도달 불가능한(unreachable) 노드와 가십을 시도합니다.
4. 필요하면 시드 노드(seed node)와 가십합니다.

"시드 노드는 다른 시드 노드를 보지 않고도 링에 부트스트랩(bootstrap)하는 것이 허용되며", 가십 핫스팟(hotspot) 역할을 합니다. 여러 개의 시드 노드(이상적으로는 랙/데이터센터당 하나)는 클러스터 탐색(discovery)을 용이하게 합니다.

##### 장애 감지

"Cassandra의 모든 노드는 Phi Accrual 장애 감지기(Phi Accrual Failure Detector)의 변형을 실행하며", 하트비트 상태를 기반으로 피어(peer)의 가용성을 독립적으로 판단합니다. "특정 시간 동안 어떤 노드로부터 증가하는 하트비트를 보지 못하면, 장애 감지기는 그 노드를 유죄(convict)로 판정하고", 읽기 대신 쓰기를 힌트(hint)로 라우팅합니다.

중요한 점은 "Cassandra는 운영자의 명시적인 지시 없이는 가십 상태에서 노드를 절대 제거하지 않으며", 이로써 일시적 장애 동안 불필요한 데이터 재분배(rebalancing)를 방지합니다.

#### 점진적 확장

Cassandra는 "범용 하드웨어(commodity hardware)"를 위해 설계되었으며, "노드는 언제든지 장애를 일으킬 수 있다"는 가정을 전제로 합니다. 시스템은 리소스 사용을 자동으로 튜닝하며, 압축(compression)과 캐싱(caching)을 활용하여 스토리지 효율을 극대화합니다.

##### 단순한 쿼리 모델

SQL 시스템과 달리, "Cassandra는 교차 파티션 트랜잭션(cross-partition transactions)을 제공하지 않기로 선택했습니다." 대신 "단일 파티션 작업에 대해 어떤 규모에서든 빠르고 일관된 지연 시간"을 제공합니다. 시스템은 경량 트랜잭션(lightweight transactions)을 통한 단일 파티션 비교 후 교환(compare-and-swap)을 지원합니다.

##### 스토리지 인터페이스

Cassandra는 "와이드 컬럼 스토어(wide-column store) 인터페이스"를 제공하며, "파티션은 여러 행을 포함하고, 각 행은 개별적으로 타입이 지정된 컬럼의 유연한 집합을 가집니다." 모든 행은 파티션 키(partition key)와 클러스터링 키(clustering key)로 고유하게 식별됩니다. 이 설계는 동시적인 메타데이터 전용 스키마 변경(metadata-only schema changes)을 통해 "사용자가 기존 데이터셋에 새 컬럼을 유연하게 추가할 수 있도록" 합니다.

---

### 스토리지 엔진

Cassandra 스토리지 엔진은 "전통적인 관계형 데이터베이스의 B-트리(B-tree) 설계 대신 추가 전용(append-only) 접근 방식을 활용하는 LSM(Log Structured Merge) 트리"를 사용합니다. 이는 쓰기 성능을 최적화하면서 블룸 필터(Bloom filter)를 통해 읽기/쓰기 트레이드오프를 관리합니다.

**쓰기 경로(Write Path) 순서:**

1. 커밋 로그(commit log)에 기록
2. 멤테이블(memtable)에 쓰기
3. 멤테이블에서 플러시(flush)
4. 디스크의 SSTable에 저장

#### 커밋 로그

커밋 로그(Commit Log)는 선행 기록 로깅(Write-Ahead Logging, WAL)을 담당합니다. 시스템은 모든 쓰기를 "디스크의 로컬 추가 전용 커밋 로그"에 기록하여 크래시 복구(crash recovery)를 보장합니다. 주요 설정은 다음과 같습니다.

- **`commitlog_segment_size`**: 기본값 32MiB. 새 세그먼트를 생성하기 전 세그먼트 크기를 제한합니다.
- **`commitlog_sync`**: 내구성 보장 방식을 지정하며 `periodic`(기본값, 10000ms) 또는 `batch` 중 선택합니다.
- **`commitlog_directory`**: 저장 위치. 자기 디스크(magnetic drive)의 경우 별도의 스핀들(spindle)을 권장합니다.
- **`commitlog_compression`**: LZ4, Snappy, Deflate, Zstd 압축을 지원합니다.
- **`commitlog_total_space`**: 기본값 8192MiB. 초과 시 플러시를 트리거합니다.

#### 멤테이블

멤테이블(Memtable)은 인메모리 라이트백 캐시(write-back cache)로, 설정된 한계에 도달할 때까지 데이터를 정렬된 순서로 저장합니다. "일반적으로 테이블당 하나의 활성 멤테이블이 있으며, 데이터 파티션의 캐시 역할을 합니다." 플러시(flush)는 메모리 임계값 또는 커밋 로그 압박(pressure)에 의해 트리거됩니다. 데이터는 `nodetool flush` 또는 자동 메커니즘을 통해 영속화됩니다.

#### SSTable

SSTable은 불변(immutable) 디스크 데이터 파일이며, 여러 구성 요소(component)를 포함합니다.

- **`Data.db`**: 실제 행(row) 내용
- **`Partitions.db`**: 파티션 키를 파일 위치에 매핑
- **`Rows.db`**: 다중 행 파티션을 위한 행 인덱스
- **`Index.db`**: 파티션 키 위치
- **`Summary.db`**: Index 항목의 샘플링
- **`Filter.db`**: 파티션 키의 블룸 필터(Bloom filter)
- **`Statistics.db`**: 타임스탬프, 툼스톤(tombstone), TTL 등의 메타데이터
- **`Digest.crc32`**: 체크섬 검증
- **`TOC.txt`**: 구성 요소 파일 목록

파일 내에서 "행은 파티션별로 구성되며 토큰 순서(token order)로 정렬되고", "행은 클러스터링 키(clustering key) 순서로 저장됩니다."

##### SSTable 버전

공식 문서는 0.7.0부터 5.0까지의 버전 진행 과정을 상세히 다루며, BigFormat 버전과 함께 Cassandra 5.0에서 도입된 새로운 BTI(Trie-indexed) 포맷을 포함합니다. BTI 포맷은 `sstable.selected_format: bti` 설정으로 구성할 수 있습니다.

---

### 보장 사항

Cassandra는 페타바이트(petabyte) 규모의 데이터셋을 처리하는 웹 스케일 애플리케이션을 위해 설계된, 고도로 확장 가능하고 신뢰성 있는 데이터베이스입니다. 시스템은 CAP 정리(CAP theorem) 원칙에 기반하여 확장성, 가용성, 신뢰성에 대한 구체적인 보장을 제공합니다.

#### CAP 정리 기반

CAP 정리에 따르면, 분산 데이터 스토어는 다음 세 가지 속성을 동시에 보장할 수 없습니다.

- **일관성(Consistency)**: "모든 읽기는 가장 최근의 쓰기를 받거나 오류를 반환한다"
- **가용성(Availability)**: "모든 요청이 응답을 받는다"
- **분할 내성(Partition tolerance)**: 네트워크 분할(network partition) 장애를 견디면서 계속 동작한다

Cassandra는 고가용성 웹 애플리케이션을 지원하기 위해 엄격한 일관성보다 가용성과 분할 내성을 우선시합니다.

#### 핵심 보장

Cassandra는 다음을 보장합니다.

**확장성과 가용성**

- 가십 기반 프로토콜을 통한 노드 추가/제거로 높은 확장성을 제공합니다.
- 가십 기반 장애 감지를 갖춘 내결함성(fault-tolerant) 아키텍처를 통해 높은 가용성을 제공합니다.

**데이터 내구성**

- 서로 다른 노드와 데이터센터에 데이터를 복제하여 내구성을 보장합니다.
- 복제본이 존재하는 한, 복구 불가능한 장애가 발생해도 전체 데이터 손실로 이어지지 않습니다.

**일관성 모델**

- "최종 일관성(eventually consistent) 스토리지 시스템"으로, 업데이트는 결국 모든 복제본에 도달합니다.
- 일시적으로 분기된(divergent) 데이터 버전은 일관된 상태로 조정(reconcile)됩니다.
- Paxos 합의 프로토콜(consensus protocol)을 사용하는 경량 트랜잭션(lightweight transactions)은 선형화 가능한 일관성(linearizable consistency)을 제공합니다.
- 비교 후 설정(compare-and-set, CAS) 트랜잭션은 격리(isolation)를 보장합니다.

**다중 테이블 작업**

- 여러 테이블에 걸친 배치 쓰기(batched writes)는 전부 성공하거나 전혀 적용되지 않습니다.
- 배치로그 복제(batchlog replication)는 코디네이터 노드 장애에도 작업 완료를 보장합니다.

**인덱싱**

- 보조 인덱스(secondary index)는 "로컬 복제본과 일관성이 유지됨"이 보장됩니다.

---

### 스니치

Cassandra에서 스니치(snitch)는 두 가지 핵심 기능을 수행합니다. "요청을 효율적으로 라우팅하기 위해 Cassandra에게 네트워크 토폴로지(network topology)에 대해 충분히 알려주는" 것과, 노드를 데이터센터와 랙으로 조직하여 "상관 장애(correlated failure)를 피하도록 클러스터 전반에 복제본을 분산"하는 것입니다.

`cassandra.yaml`의 `endpoint_snitch` 파라미터는 `IEndpointSnitch`를 구현하는 클래스로 설정되어야 하며, 이 클래스는 동적 스니치(dynamic snitch)에 의해 래핑되어 두 엔드포인트가 같은 데이터센터에 있는지 또는 같은 랙에 있는지를 결정합니다.

#### 동적 스니치

동적 스니치(Dynamic Snitching)는 읽기 지연 시간(read latency)을 모니터링하여 느린 호스트로 요청이 라우팅되는 것을 방지합니다. 동적 스니치는 구성된 하위 스니치(underlying snitch)에서 기본적인 클러스터 토폴로지 정보를 얻고, 노드의 읽기 지연 시간과 컴팩션(compaction) 작업을 함께 추적합니다. 설정은 `cassandra.yaml`에서 다음 속성으로 이루어집니다.

- **`dynamic_snitch`**: 동적 스니치 기능을 활성화/비활성화합니다.
- **`dynamic_snitch_update_interval`**: 기본값 100ms. 비용이 큰 호스트 점수(host score) 계산의 빈도를 제어합니다.
- **`dynamic_snitch_reset_interval`**: 기본값 10m. 노드 점수를 주기적으로 초기화하는 간격입니다.
- **`dynamic_snitch_badness_threshold`**: 느린 고정 호스트가 선호도를 잃는 시점을 결정하는 백분율 임계값입니다. 0.2는 성능이 20% 저하될 때까지 Cassandra가 복제본을 전환하지 않고 기다린다는 의미입니다.

#### 스니치 유형

**GossipingPropertyFileSnitch**: 운영 환경에서 권장되는 선택입니다. 로컬 랙과 데이터센터 설정을 `cassandra-rackdc.properties` 파일에 정의하고 가십 프로토콜(gossip protocol)을 통해 다른 노드에 전파합니다. 레거시 마이그레이션을 위해 `cassandra-topology.properties` 파일이 존재하면 이를 폴백(fallback)으로 사용합니다. `PropertyFileSnitch`처럼 랙과 데이터센터를 수동으로 동기화할 필요 없이, 각 노드가 자신의 랙과 데이터센터 이름을 개별적으로 정의합니다.

**SimpleSnitch**: 복제 전략(replication strategy)의 순서를 근접성(proximity) 지표로 취급합니다. 읽기 복구(read repair)를 비활성화한 단일 데이터센터 배포에서 캐시 지역성(cache locality)을 높이고자 할 때만 적합합니다.

**PropertyFileSnitch**: `cassandra-topology.properties` 내의 명시적인 랙과 데이터센터 구성에 의존하여 근접성을 결정합니다.

**Ec2Snitch**: 단일 리전(region) EC2 배포(또는 2017년 이후 인터 리전 VPC가 활성화된 다중 리전)를 위해 설계되었습니다. EC2 API에서 리전(Region)과 가용 영역(Availability Zone) 데이터를 자동으로 가져와, 리전을 데이터센터에, 가용 영역을 랙에 매핑합니다.

**Ec2MultiRegionSnitch**: 공인 IP(public IP)를 브로드캐스트 주소(broadcast address)로 사용하여 리전 간 연결을 지원합니다. 공용 방화벽에서 `storage_port` 또는 `ssl_storage_port`를 열어야 하며, Cassandra는 리전 내 통신에서는 사설 IP(private IP)로 전환합니다.

**RackInferringSnitch**: IP 옥텟(octet)에서 랙/데이터센터를 추론합니다(각각 3번째와 2번째 옥텟). 주로 커스텀 스니치 개발을 위한 참조 구현(reference implementation) 역할을 합니다.

**GoogleCloudSnitch / CloudstackSnitch**: 각각 Google Cloud Platform 및 Apache CloudStack 환경을 위한 스니치입니다.

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Architecture - Overview](https://cassandra.apache.org/doc/latest/cassandra/architecture/overview.html)
- [Architecture - Dynamo](https://cassandra.apache.org/doc/latest/cassandra/architecture/dynamo.html)
- [Architecture - Storage Engine](https://cassandra.apache.org/doc/latest/cassandra/architecture/storage-engine.html)
- [Architecture - Guarantees](https://cassandra.apache.org/doc/latest/cassandra/architecture/guarantees.html)
- [Architecture - Snitch](https://cassandra.apache.org/doc/4.1/cassandra/architecture/snitch.html)

---

## Cassandra 데이터 모델링

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/

---

### 목차

1. [소개](#소개)
2. [개념적 데이터 모델링](#개념적-데이터-모델링)
3. [RDBMS 설계와의 차이](#rdbms-설계와의-차이)
4. [애플리케이션 쿼리 정의](#애플리케이션-쿼리-정의)
5. [논리적 데이터 모델링](#논리적-데이터-모델링)
6. [물리적 데이터 모델링](#물리적-데이터-모델링)
7. [데이터 모델 평가 및 개선](#데이터-모델-평가-및-개선)
8. [데이터베이스 스키마 정의](#데이터베이스-스키마-정의)
9. [데이터 모델링 도구](#데이터-모델링-도구)
10. [참고 자료](#참고-자료)

---

### 소개

Apache Cassandra는 관계형 데이터베이스(relational database)와는 근본적으로 다른 **쿼리 주도(query-driven) 데이터 모델링 접근법**을 사용합니다. 공식 문서는 다음과 같이 설명합니다. "데이터 모델링은 쿼리 주도적입니다. 데이터 접근 패턴(data access pattern)과 애플리케이션 쿼리가 데이터의 구조와 조직 방식을 결정합니다."

관계형 데이터베이스처럼 도메인 엔티티(entity)를 먼저 모델링한 뒤 쿼리를 작성하는 방식과 달리, 애플리케이션이 수행할 쿼리를 먼저 파악하고 그 쿼리를 지원하는 테이블을 설계합니다. 이를 **쿼리 우선(query-first)** 모델링이라고 합니다.

#### 관계형 모델과의 핵심 차이

전통적인 데이터베이스는 외래 키(foreign key)로 관련 테이블에 데이터를 정규화(normalize)하는 반면, Cassandra는 여러 테이블에 데이터를 중복 저장하는 **비정규화(denormalization)** 를 사용합니다. 이는 저장 효율보다 읽기 성능(read performance)을 우선하는 설계 선택입니다. Cassandra는 "관계형 데이터베이스를 위한 관계형 데이터 모델링을 지원하지 않습니다."

#### 파티션 전략(Partition Strategy)

데이터 분산(data distribution)은 기본 키(primary key) 구성 요소에서 파생된 **파티션 키(partition key)** 에 의존합니다. 문서에 따르면 "파티션 키는 기본 키의 첫 번째 필드에서 생성"되며, 클러스터 노드(cluster node) 전반에 걸친 일관된 해싱(consistent hashing)에 사용됩니다.

Cassandra의 기본 키는 다음과 같은 형태를 가질 수 있습니다.

- **단순 키(Simple)**: 단일 필드 (예: `PRIMARY KEY (id)`)
- **복합 키(Composite/Compound)**: 여러 필드로 구성되며, 첫 번째 필드가 파티션 키를 생성하고 나머지 필드는 파티션 내에서 정렬을 담당하는 **클러스터링 키(clustering key)** 역할을 합니다.

#### 설계 시 고려 사항

효과적인 데이터 모델은 다음을 유지해야 합니다.

- 파티션 크기를 값(value) 기준 100,000개 미만, 디스크 기준 100MB 미만으로 유지
- 데이터 중복(redundancy)의 최소화
- 경량 트랜잭션(Lightweight Transactions, LWT)의 제한적 사용
- 여러 테이블이 아닌 단일 테이블에 대한 쿼리 수행

문서는 "구체화 뷰(materialized view, MV)는 실험적(experimental)" 기능이며, 대체 기본 키(alternate primary key)를 통해 단일 테이블에 대한 여러 쿼리를 지원할 수 있다고 언급합니다.

---

### 개념적 데이터 모델링

#### 개요

개념적 데이터 모델(conceptual data model)은 도메인(domain)을 추상적으로 표현한 것으로, **기술 독립적(technology independent)** 이며 특정 데이터베이스 시스템에 종속되지 않습니다. 공식 문서는 Cassandra의 분산 해시테이블(distributed hashtable) 구조로 매핑할 관계형 도메인 모델을 제시하면서 개념적 데이터 모델링을 소개합니다.

#### 호텔 예약 예제 도메인

문서는 주요 예제로 **호텔 예약(hotel reservation) 시스템** 을 사용하며, 이는 다음을 포함합니다.

- 호텔(hotel)과 투숙(guest stay)
- 객실 요금(rate) 및 가용성(availability)을 포함한 객실 집합
- 게스트(guest)를 위한 예약 기록(reservation record)
- 관심 지점(point of interest, POI) — 공원, 박물관, 쇼핑몰, 기념물 등
- 호텔 및 명소의 지오로케이션(geolocation) 데이터

문서는 "모두에게 익숙한 도메인을 선택"함으로써 도메인 복잡성보다 Cassandra의 동작 메커니즘에 집중할 수 있다고 강조합니다. 호텔 예약 예제는 데이터 구조와 설계 패턴을 충분히 보여주면서도 이해하기 쉽다는 점에서 균형 잡힌 선택입니다.

#### 엔티티-관계 모델(Entity-Relationship Model)

개념적 도메인은 엔티티-관계(entity-relationship, ER) 모델을 사용하여 표현되며, 다음 요소들로 구성됩니다.

- **엔티티(Entity)**: 사각형(rectangle)으로 표현
- **속성(Attribute)**: 타원(oval)으로 표시
- **고유 식별자(Unique identifier)**: 밑줄이 그어진 속성
- **관계(Relationship)**: 마름모(diamond)로 표현
- **다중성(Multiplicity)**: 관계와 엔티티를 연결하는 커넥터로 표시

---

### RDBMS 설계와의 차이

관계형 데이터베이스(RDBMS)에서 Cassandra로 전환할 때 고려해야 할 근본적인 설계 차이는 다음과 같습니다.

#### 조인 없음(No Joins)

Cassandra는 조인(join)을 지원하지 않습니다. 클라이언트 측에서 조인을 수행하는 방법(드물게 사용해야 함)도 있지만, 권장되는 접근법은 "조인 결과를 표현하는 비정규화된 두 번째 테이블을 생성"하는 것입니다.

#### 참조 무결성 없음(No Referential Integrity)

외래 키 제약(foreign key constraint)을 가진 관계형 데이터베이스와 달리, Cassandra는 참조 무결성(referential integrity)을 강제하지 않습니다. 연관된 ID를 저장할 수는 있지만, 연쇄 삭제(cascading delete)와 같은 작업은 제공되지 않습니다.

#### 비정규화가 표준(Denormalization is Standard)

관계형 설계에서는 일반적으로 지양되는 비정규화가 Cassandra에서는 "완전히 정상적(perfectly normal)"입니다. 이는 전통적인 데이터베이스의 정규화 원칙과 뚜렷하게 대조됩니다. 문서는 Cassandra가 3.0 버전부터 비정규화된 데이터를 자동으로 관리하기 위해 구체화 뷰(materialized view)를 도입했다고 언급합니다.

#### 쿼리 우선 설계(Query-First Design)

도메인 엔티티를 먼저 모델링하는 대신, Cassandra는 쿼리를 중심으로 설계할 것을 요구합니다. 이 접근법은 "애플리케이션이 사용할 가장 일반적인 쿼리 경로(query path)를 식별한 다음, 이를 지원하는 데 필요한 테이블을 생성"하는 것을 포함합니다.

#### 저장소 최적화의 중요성(Storage Optimization Matters)

"Cassandra 테이블은 각각 디스크의 별도 파일에 저장"되므로, 물리적 저장에 대한 신중한 고려가 필요합니다. 쿼리당 검색되는 파티션 수를 최소화하는 것이 중요한 설계 목표입니다.

#### 고정된 정렬 전략(Fixed Sorting Strategy)

`ORDER BY`로 유연한 정렬이 가능한 RDBMS와 달리, Cassandra의 정렬 순서는 클러스터링 컬럼(clustering column)을 선택함으로써 테이블 생성 시점에 고정됩니다.

---

### 애플리케이션 쿼리 정의

쿼리 우선 데이터 모델링은 애플리케이션이 수행해야 하는 핵심 쿼리를 식별하는 것에서 시작합니다. 문서는 호텔 애플리케이션의 사용자 인터페이스(UI) 디자인에서 출발하여 필수 쿼리를 도출합니다.

각 워크플로(workflow) 단계는 하나의 작업을 수행하며 그 결과가 후속 단계를 "잠금 해제(unlock)"합니다. 워크플로 다이어그램은 쿼리들이 애플리케이션 작업을 어떻게 연결하는지를 보여주며, 한 쿼리에서 얻은 호텔 키(hotel key) 같은 데이터가 이후의 상세 조회를 가능하게 합니다.

#### 쇼핑 관련 쿼리(Shopping Queries, Q1-Q5)

- **Q1.** 주어진 관심 지점(POI) 근처의 호텔을 찾는다. (*Find hotels near a given point of interest.*)
- **Q2.** 주어진 호텔의 이름과 위치 등 정보를 찾는다. (*Find information about a given hotel, such as its name and location.*)
- **Q3.** 주어진 호텔 근처의 관심 지점을 찾는다. (*Find points of interest near a given hotel.*)
- **Q4.** 주어진 날짜 범위에서 이용 가능한 객실을 찾는다. (*Find an available room in a given date range.*)
- **Q5.** 객실의 요금과 편의 시설(amenities)을 찾는다. (*Find the rate and amenities for a room.*)

#### 예약 및 게스트 관련 쿼리(Reservation and Guest Queries, Q6-Q9)

- **Q6.** 확인 번호(confirmation number)로 예약을 조회한다. (*Lookup a reservation by confirmation number.*)
- **Q7.** 호텔, 날짜, 게스트 이름으로 예약을 조회한다. (*Lookup a reservation by hotel, date, and guest name.*)
- **Q8.** 게스트 이름으로 모든 예약을 조회한다. (*Lookup all reservations by guest name.*)
- **Q9.** 게스트 상세 정보를 조회한다. (*View guest details.*)

---

### 논리적 데이터 모델링

#### 핵심 개념

논리적 데이터 모델링(logical data modeling) 단계에서는 애플리케이션 쿼리를 토대로 테이블을 설계합니다. 문서에 따르면 "개념적 모델의 엔티티와 관계를 담으면서, 각 쿼리마다 하나의 테이블을 포함하는 논리적 모델을 생성"해야 합니다.

#### 주요 설계 원칙

**테이블 명명(Table Naming)**: 테이블은 주요 엔티티 이름 뒤에 쿼리 속성을 붙여 명명합니다. 예를 들어 `hotels_by_poi`라는 패턴은 관심 지점(POI)으로 필터링하여 호텔을 찾도록 설계된 테이블임을 나타냅니다.

**기본 키 설계(Primary Key Design)**: 기본 키는 "각 파티션에 저장되는 데이터의 양과 디스크에서의 조직 방식"을 결정합니다. 이는 Cassandra의 읽기 처리 속도에 직접 영향을 미치므로 매우 중요한 설계 요소입니다. 기본 키는 다음으로 구성됩니다.

- **파티션 키 컬럼(Partition key columns, K)**: 데이터 그룹화를 정의합니다.
- **클러스터링 컬럼(Clustering columns, C↑ 또는 C↓)**: 고유성(uniqueness)을 보장하고 정렬 순서를 제어합니다. (↑는 오름차순 ASC, ↓는 내림차순 DESC)

**정적 컬럼(Static Columns)**: 특정 속성이 동일한 파티션 키의 모든 인스턴스에 걸쳐 일정하게 유지되는 경우, 이를 정적 컬럼으로 표시하여 중복 저장을 피할 수 있습니다.

#### 시각화: Chebotko 표기법(Chebotko Notation)

Chebotko 표기법은 설계 내 쿼리와 테이블 간의 관계를 간단하고 정보가 풍부하게 시각화하는 방법을 제공합니다. 이 표준화된 다이어그램 표기법은 다음을 표시합니다.

- 테이블 이름과 컬럼 목록
- 기호로 표시된 기본 키 구성 요소 (K, C↑, C↓ 등)
- 각 테이블이 어떤 쿼리를 지원하는지 나타내는 쿼리 라인

#### 일반적인 패턴(Common Patterns)

**와이드 파티션 패턴(Wide Partition Pattern)**: 빠른 다중 행(multi-row) 접근을 지원하기 위해 관련된 여러 행을 단일 파티션에 그룹화합니다. 이 패턴은 한 파티션 내의 여러 항목에 걸친 쿼리에 대해 잘 확장됩니다.

**시계열 패턴(Time Series Pattern)**: 와이드 파티션 접근법을 확장하여 타임스탬프(timestamp)가 찍힌 측정값을 저장합니다. 분석(analytics), 센서 데이터, 금융 애플리케이션 등에서 흔히 사용됩니다.

#### 피해야 할 안티 패턴(Anti-Patterns to Avoid)

문서는 만료된 항목의 삭제에 의존하는 "큐(queue)" 설계를 경계할 것을 경고합니다. 삭제된 데이터는 **툼스톤(tombstone)** 이 되어 시간이 지남에 따라 읽기 성능을 저하시키기 때문입니다. 데이터 삭제에 의존하는 모든 설계는 재고되어야 합니다.

---

### 물리적 데이터 모델링

#### 개요

물리적 데이터 모델링(physical data modeling) 단계는 논리적 모델 개발 이후에 진행됩니다. 문서에 따르면 "논리적 데이터 모델이 정의되고 나면 물리적 모델을 만드는 과정은 비교적 간단합니다."

논리적 모델의 각 요소에 CQL 데이터 타입을 할당합니다. 기본 타입(basic type), 컬렉션(collection), 사용자 정의 타입(user-defined type, UDT)을 포함한 모든 유효한 CQL 데이터 타입을 사용할 수 있습니다. 이 과정에는 크기 계산과 구현 전 테스트가 포함됩니다.

#### Chebotko 표기법 확장(Chebotko Notation Extensions)

물리적 모델 표현은 각 컬럼에 타입 정보를 추가합니다. 시각적 표시자는 다음을 나타냅니다.

- 키스페이스(keyspace) 지정
- 컬렉션(collection)
- 사용자 정의 타입(UDT)
- 정적 컬럼(static column)
- 보조 인덱스(secondary index) 컬럼

#### 호텔 데이터 모델 예제

이 설계는 두 개의 키스페이스로 나뉩니다.

- **hotel 키스페이스**: 호텔(hotel) 및 가용성(availability) 테이블 포함
- **reservation 키스페이스**: 예약(reservation) 및 게스트(guest) 정보 테이블 포함

##### 호텔 테이블의 특성

`hotels` 테이블은 다음을 사용합니다.

- 호텔 식별자(예: "AZ123")에 `text` 타입 사용
- 위치 데이터에 커스텀 `address` 사용자 정의 타입(UDT) 사용
- 전화번호에 `text` 사용 (국제 형식의 다양성을 수용하기 위함)

문서는 `uuid` 타입이 더 나은 고유성을 제공하지만, 호텔 업계의 관행은 짧은 코드(short code)를 식별자로 선호한다고 언급합니다.

#### 예약 모델

예약 설계는 다음 기준으로 쿼리를 지원하는 세 개의 비정규화된 테이블을 포함합니다.

- 확인 번호(confirmation number)
- 게스트 정보(guest information)
- 호텔과 날짜의 조합(hotel and date combination)

`address` UDT는 reservation 키스페이스 내에서 다시 선언(redeclare)되어야 합니다. 이는 UDT가 정의된 키스페이스 내에서만 존재하는 범위 제한(scope limitation)을 반영합니다.

---

### 데이터 모델 평가 및 개선

#### 파티션 크기 계산(Calculating Partition Size)

성능 문제를 방지하려면 파티션 크기를 평가해야 합니다. Cassandra의 하드 리밋(hard limit)은 파티션당 20억(2 billion) 셀(cell)이지만, "그 한계에 도달하기 훨씬 전에 성능 문제가 발생할 수 있습니다."

제시된 공식은 다음과 같습니다.

**Nv = Nr(Nc - Npk - Ns) + Ns**

각 항목의 의미는 다음과 같습니다.

- **Nv** = 파티션 내 값(셀)의 개수 (number of values/cells)
- **Nr** = 행의 개수 (number of rows)
- **Nc** = 컬럼의 개수 (number of columns)
- **Npk** = 기본 키 컬럼의 총 개수 (number of primary key columns)
- **Ns** = 정적 컬럼의 개수 (number of static columns)

핵심 통찰은 "파티션 크기의 주요 결정 요인은 파티션 내의 행 수"라는 점입니다.

#### 디스크 크기 계산(Calculating Size on Disk)

디스크 공간 요구량을 추정하는 더 복잡한 공식은 다음과 같습니다.

**St = Σ sizeOf(파티션 키 컬럼) + Σ sizeOf(정적 컬럼) + Nr × (Σ sizeOf(일반/클러스터링 컬럼)) + Nv × sizeOf(메타데이터)**

셀당 메타데이터(metadata)는 일반적으로 평균 8바이트입니다. 예제에서는 `available_rooms_by_hotel_date` 테이블의 파티션 크기를 계산하여 약 1.1MB의 결과를 도출합니다.

#### 큰 파티션 분할하기(Breaking Up Large Partitions)

파티션이 너무 커지면, 그 해결 기법은 단순합니다. "파티션 키에 컬럼을 하나 더 추가"하는 것입니다. 두 가지 접근법이 제시됩니다.

1. **버케팅(Bucketing)** — 월(month)과 같은 그룹화 컬럼을 도입하여 파티션 크기를 조절합니다.
2. **샤딩(Sharding)** — 새로운 컬럼을 샤딩 키(sharding key)로 추가합니다.

문서는 이 방법이 "추가적인 애플리케이션 로직"을 요구하지만, 파티션이 실용적인 한계를 초과하는 것을 막아준다고 언급합니다.

---

### 데이터베이스 스키마 정의

아래는 호텔 예약 예제에 대한 최종 CQL 스키마입니다. 각 테이블은 `comment`를 통해 자신이 지원하는 쿼리(Q1~Q9)를 명시하고 있습니다.

#### Hotel 키스페이스

```sql
CREATE KEYSPACE hotel WITH replication =
  {'class': 'SimpleStrategy', 'replication_factor' : 3};

CREATE TYPE hotel.address (
  street text,
  city text,
  state_or_province text,
  postal_code text,
  country text );

CREATE TABLE hotel.hotels_by_poi (
  poi_name text,
  hotel_id text,
  name text,
  phone text,
  address frozen<address>,
  PRIMARY KEY ((poi_name), hotel_id) )
  WITH comment = 'Q1. Find hotels near given poi'
  AND CLUSTERING ORDER BY (hotel_id ASC) ;

CREATE TABLE hotel.hotels (
  id text PRIMARY KEY,
  name text,
  phone text,
  address frozen<address>,
  pois set<text> )
  WITH comment = 'Q2. Find information about a hotel';

CREATE TABLE hotel.pois_by_hotel (
  poi_name text,
  hotel_id text,
  description text,
  PRIMARY KEY ((hotel_id), poi_name) )
  WITH comment = 'Q3. Find pois near a hotel';

CREATE TABLE hotel.available_rooms_by_hotel_date (
  hotel_id text,
  date date,
  room_number smallint,
  is_available boolean,
  PRIMARY KEY ((hotel_id), date, room_number) )
  WITH comment = 'Q4. Find available rooms by hotel date';

CREATE TABLE hotel.amenities_by_room (
  hotel_id text,
  room_number smallint,
  amenity_name text,
  description text,
  PRIMARY KEY ((hotel_id, room_number), amenity_name) )
  WITH comment = 'Q5. Find amenities for a room';
```

#### Reservation 키스페이스

```sql
CREATE KEYSPACE reservation WITH replication = {'class':
  'SimpleStrategy', 'replication_factor' : 3};

CREATE TYPE reservation.address (
  street text,
  city text,
  state_or_province text,
  postal_code text,
  country text );

CREATE TABLE reservation.reservations_by_confirmation (
  confirm_number text,
  hotel_id text,
  start_date date,
  end_date date,
  room_number smallint,
  guest_id uuid,
  PRIMARY KEY (confirm_number) )
  WITH comment = 'Q6. Find reservations by confirmation number';

CREATE TABLE reservation.reservations_by_hotel_date (
  hotel_id text,
  start_date date,
  end_date date,
  room_number smallint,
  confirm_number text,
  guest_id uuid,
  PRIMARY KEY ((hotel_id, start_date), room_number) )
  WITH comment = 'Q7. Find reservations by hotel and date';

CREATE TABLE reservation.reservations_by_guest (
  guest_last_name text,
  hotel_id text,
  start_date date,
  end_date date,
  room_number smallint,
  confirm_number text,
  guest_id uuid,
  PRIMARY KEY ((guest_last_name), hotel_id) )
  WITH comment = 'Q8. Find reservations by guest name';

CREATE TABLE reservation.guests (
  guest_id uuid PRIMARY KEY,
  first_name text,
  last_name text,
  title text,
  emails set<text>,
  phone_numbers list<text>,
  addresses map<text, frozen<address>>,
  confirm_number text
) WITH comment = 'Q9. Find guest by ID';
```

> 참고: `address` UDT가 `hotel`과 `reservation` 두 키스페이스에 각각 정의되어 있습니다. 이는 UDT가 정의된 키스페이스 범위 내에서만 유효하기 때문입니다.

---

### 데이터 모델링 도구

"Cassandra 스키마를 설계 및 관리하고 쿼리를 작성하는 데 도움이 되는 여러 도구가 있습니다."

#### 사용 가능한 도구

**Hackolade**
Cassandra를 비롯한 여러 NoSQL 데이터베이스를 지원하는 데이터 모델링 솔루션입니다. 파티션 키, 클러스터링 컬럼 같은 CQL 고유 기능과 컬렉션, UDT를 포함한 데이터 타입을 처리합니다. Chebotko 다이어그램 생성을 지원합니다.

**Kashlev Data Modeler**
"접근 패턴 식별, 개념적/논리적/물리적 데이터 모델링, 스키마 생성을 포함하여 이 문서에서 설명하는 데이터 모델링 방법론을 자동화하는 Cassandra 데이터 모델링 도구"입니다. 설계의 출발점으로 사용할 수 있는 모델 패턴(model pattern)을 제공합니다.

**DataStax DevCenter**
스키마 및 쿼리 관리 도구로, 현재는 적극적으로 유지보수되지 않지만 무료로 다운로드할 수 있습니다. CQL 구문 강조(syntax highlighting), 명령어 자동 완성, 오류 감지, 다중 스크립트 창, 클러스터 연결, 성능 분석을 위한 쿼리 추적(query trace) 기능을 제공합니다.

**IDE 플러그인(IDE Plugins)**
IntelliJ IDEA, Apache NetBeans 등의 개발 환경을 위한 CQL 플러그인이 존재합니다. 일반적으로 스키마 관리와 쿼리 실행 기능을 제공합니다.

#### 중요한 고려 사항

도구를 선택할 때는, Cassandra를 관계형 데이터베이스처럼 취급하는 JDBC/ODBC 드라이버에 의존하지 않고 **CQL을 네이티브로 지원**하는지 확인해야 합니다. 이는 Cassandra 모범 사례(best practice)와의 정합성을 보장합니다.

---

### 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Data Modeling - Introduction](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/intro.html)
- [Conceptual Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_conceptual.html)
- [RDBMS Design](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_rdbms.html)
- [Defining Application Queries](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_queries.html)
- [Logical Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_logical.html)
- [Physical Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_physical.html)
- [Evaluating and Refining Data Models](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_refining.html)
- [Defining Database Schema](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_schema.html)
- [Cassandra Data Modeling Tools](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_tools.html)
- *Cassandra: The Definitive Guide* (3rd Edition), Jeff Carpenter & Eben Hewitt, O'Reilly Media, 2020
