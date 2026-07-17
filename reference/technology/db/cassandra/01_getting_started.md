# Cassandra 시작하기

> 원본: https://cassandra.apache.org/doc/latest/cassandra/getting-started/

설치, 설정, 데이터 삽입 및 조회, 클라이언트 드라이버(driver), 프로덕션(production) 권장 사항을 차례로 설명합니다.

---

## 목차

1. [설치](#설치)
2. [설정](#설정)
3. [데이터 삽입 및 조회](#데이터-삽입-및-조회)
4. [클라이언트 드라이버](#클라이언트-드라이버)
5. [프로덕션 권장 사항](#프로덕션-권장-사항)
6. [참고 자료](#참고-자료)

---

## 설치

Apache Cassandra는 여러 가지 방식으로 설치할 수 있습니다. 가장 빠르게 사용해 보려면 Docker 이미지를 이용하고, 단일 노드(node)에서 실험하거나 root 권한 없이 설치하려면 바이너리 tarball을 사용하며, 운영 체제 패키지 관리자와 통합하려면 Debian 또는 RPM 패키지를 사용합니다.

### 사전 요구 사항(Prerequisites)

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

### Docker로 설치

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

### 바이너리 tarball로 설치

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

### Debian 패키지로 설치

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

### RPM 패키지로 설치

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

### 설치 확인

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

## 설정

Cassandra의 동작은 여러 설정 파일로 제어하며, 파일 위치는 설치 방식에 따라 다릅니다.

### 설정 파일 위치

- **Docker**: `/etc/cassandra` 디렉터리
- **tarball**: 설치 위치 내부의 `conf` 디렉터리
- **패키지(Package)**: `/etc/cassandra` 디렉터리

### 주요 설정 파일

| 파일 | 설명 |
| --- | --- |
| `cassandra.yaml` | 주(main) 설정 파일입니다. 민감한 설정을 담고 있으므로 신뢰할 수 있는 사용자만 접근하도록 제한해야 합니다. |
| `cassandra-env.sh` | 힙 크기(heap size), JVM 명령줄 인자 등 JVM 수준의 환경 변수를 설정합니다. |
| `cassandra-rackdc.properties` 또는 `cassandra-topology.properties` | 클러스터 노드의 랙(rack)과 데이터센터(datacenter) 정보를 지정합니다. |
| `logback.xml` | 로깅 설정과 로깅 레벨을 제어합니다. |
| `jvm-*` 파일 | 서버 및 클라이언트용 JVM 설정 파일들입니다. |
| `commitlog_archiving.properties` | commitlog의 아카이빙(archiving) 파라미터를 설정합니다. |
| `cqlshrc.sample` | CQL 셸(`cqlsh`)을 위한 설정 템플릿입니다. |

### cassandra.yaml의 주요 런타임 속성

- **`cluster_name`**: 클러스터(cluster)를 식별하는 이름입니다.
- **`seeds`**: 클러스터의 시드 노드(seed node) IP 주소를 쉼표로 구분한 목록입니다.
- **`storage_port`**: 기본값은 `7000`입니다. 방화벽에서 이 포트로의 접근을 허용하는지 확인해야 합니다. (노드 간 통신에 사용됩니다.)
- **`listen_address`**: 노드 간 통신에 사용할 IP 주소이며, 기본값은 localhost입니다.
- **`listen_interface`**: `listen_address` 대신 네트워크 인터페이스를 지정할 때 사용하는 대안 속성입니다.
- **`native_transport_port`**: 기본값은 `9042`입니다. `cqlsh`를 비롯한 클라이언트 연결에 사용됩니다.

### 디렉터리 위치 속성

- **`data_file_directories`**: SSTable과 데이터 파일의 위치입니다.
- **`commitlog_directory`**: commitlog 파일을 저장하는 위치입니다.
- **`saved_caches_directory`**: 저장된 캐시(cache)의 위치입니다.
- **`hints_directory`**: hints 파일을 저장하는 위치입니다.

> 성능 팁: 디스크가 여러 개라면 commitlog와 데이터 파일을 서로 다른 디스크에 두는 것을 고려하십시오.

### 기본 로깅 동작

logback 로거의 기본 동작은 다음과 같습니다.

- `system.log`에 INFO 레벨로 기록
- `debug.log`에 DEBUG 레벨로 기록
- 포그라운드(foreground) 실행 시 콘솔에 INFO 레벨로 출력

로깅 설정은 `logback.xml`을 수정하거나 `nodetool setlogginglevel` 명령으로 변경할 수 있습니다.

---

## 데이터 삽입 및 조회

Cassandra와 상호작용하는 기본 API는 **CQL**(Cassandra Query Language)이며, 다음 세 가지 방법으로 사용할 수 있습니다.

1. **cqlsh** — 명령줄 셸(command-line shell)
2. **클라이언트 드라이버(client drivers)** — 커뮤니티에서 제공하는 라이브러리
3. **Apache Zeppelin** — 노트북(notebook) 형태의 인터페이스

### cqlsh 사용

`cqlsh`는 모든 Cassandra 배포판에 포함된 명령줄 도구로, `bin` 디렉터리에 있습니다. 노드에 접속하려면 다음과 같이 실행합니다.

```bash
$ bin/cqlsh localhost
```

노드를 지정하지 않으면 `localhost`에 접속합니다. 접속 후에는 CQL 명령을 대화형(interactive)으로 실행할 수 있습니다.

### 예시 세션

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

### 그 밖의 자료

`CREATE KEYSPACE`, `CREATE TABLE`, `INSERT`, `SELECT` 등 CQL 전체 문법은 [CQL 레퍼런스 섹션](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/ddl.html)을 참고하십시오. 사용하려는 언어에 맞는 클라이언트 드라이버 문서도 함께 확인하는 것이 좋습니다.

---

## 클라이언트 드라이버

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

## 프로덕션 권장 사항

프로덕션 클러스터를 구축할 때는 단일 노드 개발 환경과 다른 여러 사항을 고려해야 합니다.

### 토큰(Tokens)과 가상 노드(vnodes)

토큰 개수(`num_tokens`)는 클러스터의 탄력성(elasticity)과 가용성(availability) 사이의 균형을 결정합니다. `cassandra.yaml`에서 설정합니다.

- **1 token**: 가용성과 클러스터 최대 규모가 최대이고 피어(peer) 수가 가장 적지만, 확장이 유연하지 못합니다.
- **4 tokens**: 최종적으로 30개를 초과하는 클러스터에 권장됩니다. 균형을 위해 약 20%의 추가 노드가 필요합니다.
- **8 tokens**: 성능에 거의 영향 없이 워크로드를 분산합니다.
- **16 tokens**: 탄력적으로 확장·축소하는 클러스터에 가장 적합하지만, 50개 이상의 노드 클러스터에서는 문제가 생길 수 있습니다.

균등한 토큰 할당을 위해 `allocate_tokens_for_local_replication_factor`를 복제 인수에 맞게 설정해야 합니다.

### 리드 어헤드(Read Ahead)

리드 어헤드는 하드웨어 유형에 따라 최적값이 다릅니다.

- **회전식 디스크(spinning disk)**: 64KB로 시작하는 것을 권장합니다.
- **SSD**: 4KB로 시작하는 것을 권장합니다.

파티션이 작거나 단일 파티션 키(single-partition-key) 테이블에서는 리드 어헤드가 오히려 역효과를 내어, 키/값(key/value) 워크로드에서 지연 시간(latency)과 처리량(throughput)을 최대 5배까지 저하시킬 수 있습니다.

리눅스 `blockdev` 도구로 조정하며, 인자의 단위는 512바이트 섹터(sector)입니다.

### 압축(Compression)

기본 압축 알고리즘은 `LZ4Compressor`입니다. `chunk_length_in_kb` 파라미터(Cassandra 4.0부터 기본값 16KB)는 성능에 크게 영향을 미치며, 청크(chunk)가 지나치게 크면 읽기 증폭(read amplification)이 발생합니다.

### 컴팩션(Compaction)

컴팩션 전략(strategy)은 워크로드 특성에 따라 선택합니다. 같은 클러스터 안에서도 테이블마다 서로 다른 전략을 적용할 수 있습니다.

### 암호화 및 보안(Encryption & Security)

프로덕션 클러스터 구축 시 노드 간(peer-to-peer) 암호화와 클라이언트-서버(client-server) 암호화를 초기부터 함께 설정해야 합니다. 배포 이후에 적용하면 다운타임 위험이 따릅니다.

### 복제 전략(Replication Strategy)

프로덕션 클러스터는 반드시 `NetworkTopologyStrategy`를 사용해야 하며, `SimpleStrategy`는 사용하지 않아야 합니다. `NetworkTopologyStrategy`는 다중 랙(multi-rack) 및 다중 데이터센터(multi-datacenter) 구성을 지원합니다.

### 스니치(Snitch) 설정

클러스터 프로비저닝 이후에 랙을 변경하는 작업은 지원되지 않으므로, 처음부터 올바르게 구성해야 합니다.

- **온프레미스 또는 혼합(hybrid) 환경**: `GossipingPropertyFileSnitch` 권장
- **AWS 전용 배포**: `Ec2Snitch` 권장

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Getting Started 섹션](https://cassandra.apache.org/doc/latest/cassandra/getting-started/)
- [Installing Cassandra](https://cassandra.apache.org/doc/latest/cassandra/installing/installing.html)
- [Configuring Cassandra](https://cassandra.apache.org/doc/latest/cassandra/getting-started/configuring.html)
- [Inserting and Querying](https://cassandra.apache.org/doc/latest/cassandra/getting-started/querying.html)
- [Client Drivers](https://cassandra.apache.org/doc/latest/cassandra/getting-started/drivers.html)
- [Production Recommendations](https://cassandra.apache.org/doc/latest/cassandra/getting-started/production.html)
- [CQL 레퍼런스](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/)
- [Apache Cassandra 다운로드](https://cassandra.apache.org/download/)
