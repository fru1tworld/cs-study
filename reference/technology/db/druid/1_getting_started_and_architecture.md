# Druid 시작하기, 아키텍처, 프로세스와 서비스

## Druid 시작하기

> 원본: https://druid.apache.org/docs/latest/design/
> 원본: https://druid.apache.org/docs/latest/tutorials/
> 원본: https://druid.apache.org/docs/latest/tutorials/docker
> 원본: https://druid.apache.org/docs/latest/operations/single-server
> 원본: https://druid.apache.org/docs/latest/tutorials/cluster

Druid 소개와 적합한 사용 사례, 로컬 퀵스타트, Docker 실행, 단일 서버 배포, 클러스터 구성을 차례로 설명합니다.

---

### 목차

1. [Druid 소개](#druid-소개)
2. [로컬 퀵스타트](#로컬-퀵스타트)
3. [Docker로 실행](#docker로-실행)
4. [단일 서버 배포](#단일-서버-배포)
5. [클러스터 구성](#클러스터-구성)
6. [참고 자료](#참고-자료)

---

### Druid 소개

Apache Druid는 대규모 데이터셋에서 빠른 슬라이스 앤 다이스(slice-and-dice) 분석을 수행하기 위한 실시간 분석 데이터베이스(real-time analytics database)입니다. 빠른 집계가 필요한 분석 애플리케이션의 UI와 고동시성(high-concurrency) API의 백엔드로 주로 사용하며, 이벤트 지향(event-oriented) 데이터에 특히 강합니다.

#### 주요 사용 분야

- 클릭스트림 분석 (웹, 모바일)
- 네트워크 텔레메트리 분석 (네트워크 흐름 모니터링)
- 서버 메트릭 저장
- 공급망(supply chain) 분석 (제조 메트릭)
- 애플리케이션 성능 메트릭
- 디지털 마케팅/광고 분석
- 비즈니스 인텔리전스(BI)와 OLAP
- 고객 분석, IoT 분석, 금융 분석, 헬스케어 분석, 소셜 미디어 분석

#### 핵심 특징

| 특징 | 설명 |
| --- | --- |
| 컬럼 지향 저장(column-oriented storage) | 쿼리에 필요한 컬럼만 로드하므로 소수 컬럼만 조회하는 쿼리가 매우 빠릅니다. |
| 확장 가능한 분산 시스템 | 수십~수백 대 규모의 클러스터로 배포하며, 초당 수백만 건의 인제스천(ingestion) 속도를 처리합니다. |
| 대규모 병렬 처리(MPP) | 하나의 쿼리를 클러스터 전체에서 병렬로 처리합니다. |
| 실시간·배치 인제스천 | 실시간(스트리밍) 또는 배치로 데이터를 적재하며, 적재한 즉시 쿼리할 수 있습니다. |
| 자가 치유·자가 균형·운영 편의성 | 스케일 아웃/인 시 클러스터가 자동으로 재균형을 잡고, 서버 장애 시 자동으로 우회하며, 다운타임 없이 운영합니다. |
| 클라우드 네이티브 내결함성 아키텍처 | 인제스천한 데이터의 사본을 딥 스토리지(deep storage — 클라우드 스토리지, HDFS, 공유 파일시스템)에 보관하므로, 모든 서버가 실패하더라도 딥 스토리지에서 복구할 수 있습니다. |
| 빠른 필터링을 위한 인덱스 | Roaring 또는 CONCISE 압축 비트맵 인덱스로 여러 컬럼에 걸친 필터링과 검색을 빠르게 수행합니다. |
| 시간 기반 파티셔닝 | 데이터를 먼저 시간으로 파티셔닝하고, 추가로 다른 필드로도 파티셔닝할 수 있습니다. 시간 범위가 있는 쿼리는 해당 범위의 데이터만 접근합니다. |
| 근사(approximate) 알고리즘 | 근사 count-distinct, 근사 랭킹, 근사 히스토그램·분위수 계산 알고리즘을 제공합니다. 제한된 메모리로 정확한 계산보다 훨씬 빠르게 동작하며, 정확도가 더 중요한 경우 정확 계산 모드도 지원합니다. |
| 인제스천 시점 자동 요약(roll-up) | 인제스천 시점에 데이터를 사전 집계(pre-aggregate)하는 롤업을 선택적으로 지원하여 저장 비용을 줄이고 성능을 높입니다. |

#### Druid가 적합한 경우

- 삽입(insert) 비율이 높지만 갱신(update)은 드문 워크로드
- 대부분의 쿼리가 집계와 리포팅("group by" 쿼리, 검색, 스캔)인 경우
- 쿼리 지연 시간 목표가 100ms에서 수 초 사이인 경우
- 데이터에 시간 요소가 있는 경우 (Druid는 시간 축에 대한 최적화와 설계를 갖추고 있습니다)
- 테이블이 여러 개여도 쿼리는 하나의 큰 분산 테이블만 대상으로 하는 경우 (작은 "lookup" 테이블과의 조인은 가능합니다)
- 카디널리티가 높은 컬럼(URL, 사용자 ID 등)에 대해 빠른 카운트와 랭킹이 필요한 경우
- Kafka, HDFS, 플랫 파일, Amazon S3 같은 객체 스토리지에서 데이터를 로드하는 경우

#### Druid가 적합하지 않은 경우

- 기본 키(primary key)를 이용한 기존 레코드의 저지연 갱신이 필요한 경우 — Druid는 스트리밍 삽입은 지원하지만 스트리밍 갱신은 지원하지 않으며, 갱신은 백그라운드 배치 작업으로 수행합니다
- 쿼리 지연 시간이 중요하지 않은 오프라인 리포팅 시스템을 구축하는 경우
- 큰 테이블끼리 조인하는 "빅 조인"이 필요하고 그 쿼리가 오래 걸려도 괜찮은 경우

---

### 로컬 퀵스타트

Druid를 노트북 같은 단일 장비에 설치하고 샘플 데이터를 인제스천하고 조회하는 과정입니다.

#### 사전 요구 사항

- **하드웨어**: 최소 6GiB RAM을 갖춘 장비
- **운영 체제**: Linux, Mac OS X 또는 기타 Unix 계열 OS (Windows는 지원하지 않습니다)
- **소프트웨어**:
  - Java 17 — PATH에 있거나 `JAVA_HOME` 또는 `DRUID_JAVA_HOME` 환경 변수로 위치를 지정합니다
  - Python 3
  - Perl 5

Java 요구 사항 충족 여부는 배포판에 포함된 스크립트로 확인할 수 있습니다.

```bash
apache-druid-37.0.0/bin/verify-java
```

#### 설치

[Apache Druid 다운로드 페이지](https://druid.apache.org/downloads/)에서 배포판을 내려받아 압축을 풀고 디렉터리로 이동합니다.

```bash
tar -xzf apache-druid-37.0.0-bin.tar.gz
cd apache-druid-37.0.0
```

#### Druid 시작

패키지 루트 디렉터리에서 다음 명령을 실행합니다.

```bash
./bin/start-druid
```

이 명령은 ZooKeeper와 모든 Druid 서비스를 함께 시작합니다. 사용할 총 메모리를 직접 지정하려면 `-m` 옵션을 사용합니다.

```bash
./bin/start-druid -m 16g
```

서비스가 모두 뜨면 브라우저에서 웹 콘솔에 접속합니다.

- 기본: `http://localhost:8888`
- TLS 활성화 시: `https` 프로토콜, 9088 포트

모든 프로세스가 완전히 시작되기까지 몇 초가 걸릴 수 있습니다.

#### 샘플 데이터 로드

배포판에는 2015-09-12 하루 동안의 Wikipedia 편집 이벤트 샘플이 포함되어 있습니다. 웹 콘솔에서 다음 절차로 적재합니다.

1. **Query** 뷰로 이동해 **Connect external data**를 클릭합니다.
2. **Local disk**를 선택하고 다음 값을 입력합니다.
   - Base directory: `quickstart/tutorial/`
   - File filter: `wikiticker-2015-09-12-sampled.json.gz`
3. **Connect data**를 클릭합니다.
4. Parse 화면에서 데이터가 올바르게 파싱되는지 확인하고 **Done**을 클릭합니다.
5. 데이터소스 이름을 `wikiticker-2015-09-12-sampled`에서 `wikipedia`로 변경합니다.
6. **Run**을 클릭해 인제스천을 실행합니다.

#### 데이터 쿼리

인제스천이 완료되면 Query 뷰에서 SQL로 조회합니다.

```sql
SELECT channel, COUNT(*) FROM "wikipedia"
GROUP BY channel ORDER BY COUNT(*) DESC
```

#### 중지와 초기화

- **중지**: Druid를 실행한 터미널에서 `CTRL+C`를 누릅니다.
- **초기화**: 완전히 새로운 상태로 되돌리려면 중지 후 `apache-druid-37.0.0/var` 디렉터리를 삭제하고 다시 시작합니다.

---

### Docker로 실행

Docker Compose로 Druid 클러스터 전체를 컨테이너로 띄울 수 있습니다.

#### 사전 요구 사항

- Docker 설치
- 기본 `docker-compose.yml` 기준으로 Docker에 최소 6GiB 메모리를 할당해야 합니다. 각 컨테이너는 최대 7GiB(직접 메모리 6GiB + 힙 1GiB)까지 사용할 수 있습니다.
- 컨테이너가 에러 코드 137로 종료된다면 메모리 부족이 원인일 가능성이 높습니다. macOS에서는 Docker Desktop 환경설정에서 메모리 할당량을 늘립니다.

#### 구성

Compose 파일은 총 8개의 컨테이너를 생성합니다.

| 구성 요소 | 역할 |
| --- | --- |
| ZooKeeper | 클러스터 코디네이션 |
| PostgreSQL | 메타데이터 저장소 |
| Druid 서비스 6개 | micro-quickstart 구성 기반의 Druid 프로세스 |

`druid_shared`라는 이름의 볼륨(named volume)이 딥 스토리지 역할을 하며, 세그먼트와 태스크 로그를 공유하기 위해 각 컨테이너의 `/opt/shared`에 마운트됩니다.

작업 디렉터리에 다음 두 파일을 내려받습니다.

- `docker-compose.yml` — Druid GitHub 저장소의 `distribution/docker/`에서 제공
- `environment` — 컨테이너 공통 환경 설정 파일

#### environment 파일의 주요 변수

| 변수 | 설명 |
| --- | --- |
| `DRUID_MAXDIRECTMEMORYSIZE` | 직접 메모리 크기 (기본 6GiB) |
| `DRUID_XMX` | 최대 힙 크기 (기본 1GB) |
| `DRUID_XMS` | 초기 힙 크기 (기본 1GB) |
| `DRUID_SINGLE_NODE_CONF` | 사용할 단일 서버 프로필 (기본 `micro-quickstart`, 더 작은 환경에서는 `nano-quickstart` 등) |
| `DRUID_CONFIG_COMMON`, `DRUID_CONFIG_${service}` | 프로덕션 환경에서 커스텀 설정 파일 경로 지정 |
| `DRUID_LOG4J`, `DRUID_LOG_LEVEL` | 로깅 설정과 로그 레벨 |

`druid_` 접두사가 붙은 환경 변수는 Java 명령줄 프로퍼티로 변환됩니다. 예를 들어 `druid_metadata_storage_type=postgresql`은 `-Ddruid.metadata.storage.type=postgresql`이 됩니다.

#### 실행

설정 파일이 있는 디렉터리에서 실행합니다.

```bash
# 포그라운드 실행 (셸이 붙은 상태)
docker compose up

# 백그라운드 실행
docker compose up -d

# 종료
docker compose down
```

웹 콘솔은 `http://localhost:8888`로 접속합니다. 모든 프로세스가 완전히 시작되기까지 몇 초가 걸리며, 시작 직후 잠깐 나타나는 오류는 무시해도 됩니다. 8888 포트가 이미 사용 중이라면 `docker-compose.yml`의 `ports` 항목을 `9999:8888`처럼 바꿔 충돌을 피할 수 있습니다.

#### 컨테이너 내부 확인과 데이터 보존

컨테이너 셸에 접속하려면 다음 명령을 사용합니다.

```bash
docker exec -ti <id> sh
```

- Druid 설치 경로: `/opt/druid`
- 시작 스크립트: `/druid.sh`

데이터는 Docker 볼륨에 저장되므로 클러스터를 재시작해도 유지됩니다. 이후 퀵스타트 튜토리얼의 절차대로 데이터를 로드하고 쿼리하면 됩니다. 프로덕션에서는 필요한 서비스 의존성에 맞게 Compose 파일을 커스터마이징합니다.

---

### 단일 서버 배포

한 대의 서버에 Druid 전체를 배포할 때는 `bin/start-druid` 스크립트를 사용하는 것이 표준 방식입니다.

#### 자동 설정 (bin/start-druid)

`bin/start-druid`는 시스템 리소스에 맞게 메모리를 자동으로 설정합니다.

- 기본적으로 시스템의 모든 프로세서를 사용합니다
- 시스템 메모리의 최대 80%까지 사용할 수 있습니다
- 설정은 `conf/druid/auto`에서 읽습니다

#### 사전 구성 프로필 (deprecated)

`conf/druid/single-server/` 아래에 하드웨어 크기별 예시 설정이 있습니다. 이 프로필들은 deprecated 상태이며, `bin/start-druid` 사용을 권장합니다.

| 프로필 | 대상 하드웨어 | 시작 명령 | 설정 경로 |
| --- | --- | --- | --- |
| nano-quickstart | 1 CPU, 4GB RAM | `bin/start-nano-quickstart` | `conf/druid/single-server/nano-quickstart` |
| micro-quickstart | 4 CPU, 16GB RAM | `bin/start-micro-quickstart` | `conf/druid/single-server/micro-quickstart` |
| small | 8 CPU, 64GB RAM | `bin/start-small` | `conf/druid/single-server/small` |
| medium | 16 CPU, 128GB RAM | `bin/start-medium` | `conf/druid/single-server/medium` |
| large | 32 CPU, 256GB RAM | `bin/start-large` | `conf/druid/single-server/large` |
| xlarge | 64 CPU, 512GB RAM | `bin/start-xlarge` | `conf/druid/single-server/xlarge` |

참고 사항은 다음과 같습니다.

- `nano-quickstart`는 작은 Docker 컨테이너처럼 리소스가 제한된 환경을 대상으로 합니다.
- `micro-quickstart`는 노트북 같은 작은 장비에서의 평가(evaluation) 용도에 적합합니다.
- 모든 구성에는 기본적으로 ZooKeeper 인스턴스가 포함됩니다.
- `small` 이상의 큰 구성은 Amazon i3 계열 EC2 인스턴스 크기를 기준으로 성능 기대치를 잡습니다.

---

### 클러스터 구성

프로덕션 규모에서는 프로세스를 서버 역할별로 나눠 여러 장비에 배포합니다.

#### 클러스터 아키텍처

권장 아키텍처는 세 가지 서버 유형으로 구성됩니다.

| 서버 유형 | 실행 프로세스 | 역할 |
| --- | --- | --- |
| Master 서버 | Coordinator, Overlord | 클러스터의 메타데이터와 코디네이션을 담당합니다. |
| Data 서버 | Historical, Middle Manager | 실제 데이터 저장과 인제스천(indexing)을 수행합니다. CPU, RAM, SSD의 이점을 크게 받습니다. |
| Query 서버 | Broker, Router | Broker는 외부 클라이언트의 쿼리를 받아 클러스터에 분배하고, 선택적으로 인메모리 쿼리 캐시를 유지합니다. Router는 선택적인 프록시입니다. |

#### 하드웨어 권장 사항

| 서버 | AWS 기준 | 사양 | 설정 경로 |
| --- | --- | --- | --- |
| Master 서버 | m5.2xlarge | 8 vCPU, 32GiB RAM | `conf/druid/cluster/master` |
| Data 서버 (2대) | i3.4xlarge | 각 16 vCPU, 122GiB RAM, 2×1.9TB SSD | `conf/druid/cluster/data` |
| Query 서버 | m5.2xlarge | 8 vCPU, 32GiB RAM | `conf/druid/cluster/query` |

#### 배포판 준비

각 서버에서 배포판 압축을 풀고 디렉터리로 이동합니다.

```bash
tar -xzf apache-druid-37.0.0-bin.tar.gz
cd apache-druid-37.0.0
```

클러스터 공통 설정은 `conf/druid/cluster/_common/common.runtime.properties`에서 수정합니다.

#### 메타데이터 저장소 설정

`conf/druid/cluster/_common/common.runtime.properties`에서 다음 프로퍼티를 메타데이터 저장소 주소로 변경합니다.

- `druid.metadata.storage.connector.connectURI`
- `druid.metadata.storage.connector.host`

프로덕션에서는 복제(replication) 구성을 갖춘 전용 MySQL 또는 PostgreSQL 사용을 권장합니다.

#### 딥 스토리지 설정

로컬 저장소 대신 분산 딥 스토리지를 사용하도록 변경합니다.

**S3 사용 시:**

```properties
druid.extensions.loadList=["druid-s3-extensions"]

druid.storage.type=s3
druid.storage.bucket=your-bucket
druid.storage.baseKey=druid/segments
druid.s3.accessKey=...
druid.s3.secretKey=...

druid.indexer.logs.type=s3
druid.indexer.logs.s3Bucket=your-bucket
druid.indexer.logs.s3Prefix=druid/indexing-logs
```

**HDFS 사용 시:**

```properties
druid.extensions.loadList=["druid-hdfs-storage"]

druid.storage.type=hdfs
druid.storage.storageDirectory=/druid/segments

druid.indexer.logs.type=hdfs
druid.indexer.logs.directory=/druid/indexing-logs
```

HDFS를 사용하는 경우 Hadoop 설정 XML 파일(core-site.xml 등)을 `conf/druid/cluster/_common/`에 배치합니다.

#### ZooKeeper 설정

`conf/druid/cluster/_common/common.runtime.properties`의 `druid.zk.service.host`에 ZooKeeper 서버들의 host:port 쌍을 쉼표로 구분해 지정합니다.

```properties
druid.zk.service.host=127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002
```

#### 단일 서버 배포에서 마이그레이션

기존 단일 서버 배포의 설정을 클러스터로 옮길 때는 다음과 같이 진행합니다.

- **Master**: 기존 coordinator-overlord 설정을 `conf/druid/cluster/master/coordinator-overlord`로 복사합니다.
- **Data**: 새 하드웨어의 CPU/RAM을 Data 서버 대수로 나눈 분할 비율(split factor)에 맞춰 Historical과 Middle Manager 설정을 조정합니다.
- **Query**: 하드웨어 사양이 적절하다면 Broker와 Router 설정을 수정 없이 복사합니다.

#### 서버 시작

각 서버에서 역할에 맞는 스크립트를 실행합니다.

```bash
# Master 서버 (외부 ZooKeeper 사용 시)
bin/start-cluster-master-no-zk-server

# Master 서버 (같은 장비에서 ZooKeeper 함께 실행 시)
bin/start-cluster-master-with-zk-server

# Data 서버
bin/start-cluster-data-server

# Query 서버
bin/start-cluster-query-server
```

#### 방화벽 포트

| 서버 | 포트 | 용도 |
| --- | --- | --- |
| Master | 1527 | Derby 메타데이터 저장소 (Derby 사용 시) |
| Master | 2181 | ZooKeeper (함께 실행 시) |
| Master | 8081 | Coordinator |
| Master | 8090 | Overlord |
| Data | 8083 | Historical |
| Data | 8091, 8100–8199 | Middle Manager 및 태스크 |
| Query | 8082 | Broker |
| Query | 8088 | Router |

---

### 참고 자료

- [Apache Druid 공식 문서](https://druid.apache.org/docs/latest/design/)
- [Local quickstart](https://druid.apache.org/docs/latest/tutorials/)
- [Docker tutorial](https://druid.apache.org/docs/latest/tutorials/docker)
- [Single server deployment](https://druid.apache.org/docs/latest/operations/single-server)
- [Clustered deployment](https://druid.apache.org/docs/latest/tutorials/cluster)
- [Apache Druid 다운로드](https://druid.apache.org/downloads/)

---

## Druid 아키텍처

> 원본: https://druid.apache.org/docs/latest/design/architecture
> 원본: https://druid.apache.org/docs/latest/design/storage
> 원본: https://druid.apache.org/docs/latest/design/segments
> 원본: https://druid.apache.org/docs/latest/design/deep-storage
> 원본: https://druid.apache.org/docs/latest/design/metadata-storage
> 원본: https://druid.apache.org/docs/latest/design/zookeeper

Druid의 서비스 구성과 서버 유형, 스토리지 설계, 세그먼트(segment) 파일 구조, 그리고 외부 의존성인 딥 스토리지(deep storage)·메타데이터 스토리지·ZooKeeper를 차례로 설명합니다.

---

### 목차

1. [전체 아키텍처](#전체-아키텍처)
2. [스토리지 설계](#스토리지-설계)
3. [세그먼트](#세그먼트)
4. [딥 스토리지](#딥-스토리지)
5. [메타데이터 스토리지](#메타데이터-스토리지)
6. [ZooKeeper](#zookeeper)
7. [참고 자료](#참고-자료)

---

### 전체 아키텍처

Apache Druid는 클라우드 친화적이고 운영하기 쉬운 분산 아키텍처를 채택했습니다. 각 서비스를 독립적으로 확장할 수 있으며, 한 구성 요소에 장애가 발생해도 다른 구성 요소로 곧바로 번지지 않는 내결함성(fault tolerance)을 제공합니다.

#### Druid 서비스

Druid에는 다음과 같은 서비스 유형이 있습니다.

| 서비스 | 역할 |
| --- | --- |
| **Coordinator** | 클러스터의 데이터 가용성을 관리합니다. |
| **Overlord** | 데이터 인제스천(ingestion) 워크로드의 할당을 제어합니다. |
| **Broker** | 외부 클라이언트의 쿼리를 처리합니다. |
| **Router** | 요청을 Broker, Coordinator, Overlord로 라우팅합니다. |
| **Historical** | 쿼리 가능한 데이터를 저장하고 서빙합니다. |
| **Middle Manager & Peon** | 데이터 인제스천을 담당합니다. Peon은 개별 인제스천 태스크(task)를 별도 JVM에서 실행합니다. |
| **Indexer** | Middle Manager + Peon의 대안으로, 태스크를 단일 JVM 안의 스레드로 실행합니다. (experimental) |

#### 서버 유형

배포 시에는 보통 서비스를 세 가지 서버 유형으로 묶어 구성합니다.

- **Master 서버**: Coordinator와 Overlord 서비스를 실행하며, 인제스천과 데이터 가용성을 관리합니다.
- **Query 서버**: Broker와 Router 서비스를 실행하며, 웹 콘솔 UI를 포함해 사용자 대면 엔드포인트를 제공합니다.
- **Data 서버**: Historical과 Middle Manager 서비스를 실행하며, 인제스천을 수행하고 데이터를 저장합니다.

#### 서비스 배치(colocation) 고려 사항

- 세그먼트 수가 많은 클러스터에서는 Coordinator와 Overlord를 분리 배치해 리소스 경합을 줄이는 것이 유리할 수 있습니다.
- 인제스천 부하나 쿼리 부하가 큰 경우 Historical과 Middle Manager를 분리 배치해 CPU·메모리 충돌을 방지할 수 있습니다.

#### 외부 의존성

Druid 클러스터는 세 가지 외부 시스템에 의존합니다.

| 의존성 | 설명 |
| --- | --- |
| **딥 스토리지(Deep storage)** | S3, HDFS, 네트워크 파일시스템 같은 공유 파일 스토리지입니다. 인제스천된 모든 데이터를 보관하며 서비스 간 세그먼트 전달을 담당합니다. |
| **메타데이터 스토리지(Metadata storage)** | PostgreSQL, MySQL 같은 전통적인 RDBMS로, 세그먼트와 태스크의 메타데이터를 저장합니다. |
| **ZooKeeper** | 서비스 디스커버리, 조율(coordination), 리더 선출(leader election)을 담당합니다. |

---

### 스토리지 설계

#### 데이터소스와 세그먼트

Druid는 데이터를 **데이터소스(datasource)** 단위로 저장하며, 이는 관계형 데이터베이스의 테이블에 해당합니다. 각 데이터소스는 시간 기준으로 파티셔닝(partitioning)되고(예: 일 단위 청크), 필요하면 다른 속성으로 추가 파티셔닝할 수 있습니다. 각 시간 청크(time chunk) 안에서 데이터는 하나 이상의 **세그먼트**로 나뉩니다. 세그먼트는 보통 수백만 행을 담는 단일 파일이며, 데이터소스에 따라 세그먼트가 몇 개에서 수백만 개까지 존재할 수 있습니다.

각 세그먼트는 Middle Manager에서 가변(mutable)·미커밋(uncommitted) 상태로 생성됩니다. 세그먼트 빌드 과정에서 다음과 같은 방식으로 데이터를 쿼리에 최적화된 형태로 변환합니다.

- 컬럼 지향(columnar) 포맷으로 변환
- 빠른 필터링을 위한 비트맵(bitmap) 인덱스 생성
- 다양한 압축 적용
  - 문자열 컬럼의 사전 인코딩(dictionary encoding)과 ID 저장 공간 최소화
  - 비트맵 인덱스의 비트맵 압축
  - 모든 컬럼에 대한 타입 인지(type-aware) 압축

세그먼트는 커밋되면 딥 스토리지로 옮겨지고 이후 **불변(immutable)** 이 됩니다. 이때 메타데이터 스토리지에 세그먼트의 스키마, 크기, 딥 스토리지 내 위치를 기술하는 메타데이터 레코드를 기록합니다. 이 레코드는 Coordinator가 클러스터에서 어떤 데이터를 사용할 수 있는지 파악하는 근거가 됩니다.

#### 인덱싱과 핸드오프(handoff)

**인덱싱 측 절차:**

1. 인덱싱 태스크가 시작되면 Overlord를 통해 세그먼트 식별자를 할당받습니다.
2. 실시간(realtime) 태스크(Kafka, Kinesis)는 세그먼트를 만들자마자 곧바로 쿼리 대상으로 서빙합니다.
3. 태스크가 끝나면 세그먼트를 딥 스토리지로 푸시하고 메타데이터를 발행(publish)합니다.
4. 실시간 태스크는 Historical 서비스가 세그먼트를 로드할 때까지 기다렸다가 종료합니다.

**Coordinator/Historical 측 절차:**

1. Coordinator가 메타데이터 스토리지를 주기적으로(기본 60초) 폴링해 새로 발행된 세그먼트를 찾습니다.
2. 발행되었지만 아직 사용 불가한 세그먼트를 발견하면 Historical 하나를 골라 로드를 지시합니다.
3. Historical이 세그먼트를 로드해 서빙을 시작합니다.
4. 세그먼트 핸드오프를 기다리던 인덱싱 태스크가 있으면 이 시점에 종료합니다.

#### 세그먼트 식별자

세그먼트 식별자는 네 부분으로 구성됩니다.

- **데이터소스 이름**
- **시간 간격(interval)**: 인제스천 시 `segmentGranularity`로 지정한 시간 청크에 대응합니다.
- **버전 번호**: 보통 세그먼트 집합이 처음 생성된 시점의 ISO8601 타임스탬프입니다.
- **파티션 번호**: 정수이며, 데이터소스 + 간격 + 버전 조합 안에서 유일합니다.

예시:

```
clarity-cloud0_2018-05-21T16:00:00.000Z_2018-05-21T17:00:00.000Z_2018-05-21T15:56:09.909Z_1
```

파티션 번호가 0인 세그먼트는 식별자에서 파티션 번호를 생략합니다.

#### 세그먼트 버전과 MVCC

버전 번호는 배치(batch) 덮어쓰기를 지원하는 **다중 버전 동시성 제어(MVCC, multi-version concurrency control)** 를 구현합니다. 특정 시간 간격의 데이터를 덮어쓰면 같은 데이터소스·같은 시간 간격에 더 높은 버전 번호를 가진 새 세그먼트 집합이 생성됩니다.

새 데이터는 클러스터에 로드되는 동안 쿼리에 보이지 않다가, 로드가 끝나면 쿼리가 즉시(instantaneously) 새 세그먼트로 전환되고 이후 이전 버전은 클러스터에서 제거됩니다. 사용자 입장에서는 일관성이 깨지는 순간 없이 매끄럽게 데이터가 교체됩니다.

#### 세그먼트 생명주기

세그먼트의 생명주기는 세 영역에 걸쳐 있습니다.

1. **메타데이터 스토리지**: 세그먼트가 만들어지면 메타데이터(보통 수 KB 크기의 JSON payload)를 메타데이터 스토리지에 저장합니다. 이 발행(publishing) 시점에 boolean `used` 플래그로 쿼리 가능 여부를 제어합니다.
2. **딥 스토리지**: 메타데이터를 발행하기 직전에 세그먼트 데이터 파일을 딥 스토리지로 푸시합니다.
3. **쿼리 가용성**: 세그먼트는 실시간 태스크, 딥 스토리지 직접 쿼리, 또는 Historical 서비스에서 쿼리할 수 있습니다.

`sys.segments` SQL 테이블에서 세그먼트 상태를 나타내는 플래그를 확인할 수 있습니다.

| 플래그 | 의미 |
| --- | --- |
| `is_published` | 메타데이터가 발행되었고 `used`가 true입니다. |
| `is_available` | 실시간 태스크나 Historical에서 현재 쿼리 가능합니다. |
| `is_realtime` | 실시간 태스크에서만 사용 가능합니다. |
| `is_overshadowed` | 발행되었지만 다른 세그먼트에 완전히 가려진(overshadowed) 상태입니다. |

#### 가용성과 일관성

**인제스천 일관성**: 주요 인제스천 방식(Kafka, Kinesis 같은 supervised 스트리밍, SQL `REPLACE`, 네이티브 배치)은 트랜잭션 기반의 전부-아니면-전무(all-or-nothing) 발행을 제공합니다. 인제스천이 실패하면 발행되지 않은 데이터는 롤백됩니다.

**멱등성(idempotency)**: 일부 인제스천 방식은 같은 작업을 반복 실행해도 데이터가 중복되지 않도록 보장합니다.

- supervised 스트리밍 방식은 스트림 오프셋(offset)을 세그먼트 메타데이터와 함께 원자적으로 저장합니다.
- SQL `REPLACE`는 멱등합니다. (자기 자신을 참조하는 연산은 예외)
- 네이티브 배치 인제스천은 append 모드를 쓰거나 자기 참조 소스를 읽는 경우가 아니면 멱등합니다.

**쿼리 일관성**: Broker는 쿼리 시작 시점에 어떤 세그먼트 버전을 사용할지 결정하므로, 하나의 쿼리에는 일관된 세그먼트 집합이 참여합니다.

**원자적 교체(atomic replacement)**: 시간 청크 단위로 이전 버전에서 새 버전으로의 전환이 순간적으로 일어나며 성능에 영향을 주지 않습니다. 이는 **core set** 개념에 기반합니다. 더 높은 버전의 새 세그먼트들이 모두 사용 가능해진 뒤에야 Broker가 전환하며, 시간 청크당 버전별로 core set은 하나만 존재합니다.

**deprecated 잠금 모드**: `forceTimeChunkLock`을 false로 설정하면 기존 버전을 그대로 사용하는 원자적 업데이트 그룹(atomic update group) 방식이 활성화됩니다. 같은 버전의 원자적 그룹을 여러 개 둘 수 있어 시간 청크 일부만 교체하거나 교체와 추가(append)를 동시에 수행할 수 있는 더 유연한 방식이지만, deprecated 상태입니다.

**장애 조치(failover)**: 복제본을 서빙하던 Historical 여러 대가 동시에 내려가 세그먼트가 사용 불가 상태가 되더라도 쿼리는 남은 세그먼트로 계속 수행됩니다. Druid는 백그라운드에서 사용 불가한 세그먼트를 다른 Historical에 자동으로 다시 로드합니다.

---

### 세그먼트

#### 세그먼트 파일 개요

Druid는 데이터를 시간 간격 기준으로 파티셔닝한 세그먼트 파일에 저장합니다. 데이터가 있는 간격마다 세그먼트가 만들어지며, 데이터가 없는 간격에는 세그먼트가 생성되지 않습니다. 여러 인제스천 작업이 같은 간격에 데이터를 넣으면 해당 간격에 세그먼트가 여러 개 생길 수 있고, 컴팩션(compaction)이 이를 간격당 하나의 세그먼트로 병합해 성능을 최적화합니다.

- 시간 간격은 `granularitySpec`의 `segmentGranularity` 파라미터로 제어합니다.
- 무거운 쿼리 부하에서 잘 동작하려면 세그먼트 파일 크기를 **300~700MB** 범위로 유지하는 것이 좋습니다.
- 이 범위를 벗어나면 시간 간격의 세밀도(granularity)를 조정하거나, 파티셔닝 방식을 바꾸거나, `targetRowsPerSegment`(권장 시작값: 500만 행)를 조정합니다.

#### 컬럼 지향 파일 구조

세그먼트는 컬럼 지향 구조로, 컬럼별 데이터를 별도 자료 구조에 저장합니다. 쿼리에 실제로 필요한 컬럼만 스캔하므로 쿼리 지연 시간이 줄어듭니다. 컬럼은 크게 세 가지 유형입니다.

**Timestamp 컬럼과 메트릭 컬럼**

LZ4로 압축된 정수 또는 부동소수점 배열로 저장합니다. 쿼리 시 필요한 행을 압축 해제해 값을 꺼낸 뒤 집계 연산을 적용하며, 쿼리에 쓰이지 않는 컬럼은 아예 건너뜁니다.

**디멘션 컬럼**

필터와 group-by 연산을 지원해야 하므로 세 가지 자료 구조를 사용합니다.

1. **사전(dictionary)**: 값(항상 문자열로 취급)을 정수 ID에 매핑해, 리스트와 비트맵을 압축된 형태로 표현할 수 있게 합니다.
2. **리스트(list)**: 사전으로 인코딩된 컬럼 값의 목록입니다. GroupBy·TopN 쿼리에 필요하며, 집계만 수행하는 쿼리라면 이 구조 없이도 동작할 수 있습니다.
3. **비트맵(bitmap)**: 컬럼의 고유 값마다 비트맵 하나를 만들어 어떤 행이 그 값을 담고 있는지 표시합니다. AND·OR 연산을 빠르게 적용할 수 있어 필터링에 유리합니다.

"Page" 컬럼에 "Justin Bieber", "Ke$ha" 두 값이 있는 예시입니다.

```
1: 사전
   {
    "Justin Bieber": 0,
    "Ke$ha":         1
   }

2: 컬럼 데이터(리스트)
   [0, 0, 1, 1]

3: 비트맵 — 컬럼의 고유 값마다 하나
   value="Justin Bieber": [1, 1, 0, 0]
   value="Ke$ha":         [0, 0, 1, 1]
```

비트맵의 크기는 데이터 크기 × 컬럼 카디널리티(cardinality)입니다. 카디널리티가 높은 컬럼일수록 비트맵이 극도로 희소(sparse)해져 압축률이 매우 높아집니다. Druid는 이런 특성에 맞는 비트맵 압축 알고리즘(예: Roaring bitmap compression)을 사용합니다.

#### 다중 값(multi-value) 컬럼

한 행이 한 컬럼에 여러 문자열 값(개념상 배열)을 가질 수 있습니다. 행이 여러 값을 가지면 다음과 같이 저장이 달라집니다.

- 리스트 항목이 사전 ID의 배열이 됩니다.
- 해당 행이 값 개수만큼 여러 비트맵에서 0이 아닌 항목을 갖게 됩니다.

위 예시에서 두 번째 행이 두 값을 모두 가진다면 다음과 같습니다.

```
2: 컬럼 데이터(리스트)
   [0, [0, 1], 1, 1]

3: 비트맵
   value="Justin Bieber": [1, 1, 0, 0]
   value="Ke$ha":         [0, 1, 1, 1]
```

#### null 값 처리

- 문자열 컬럼은 null을 사전의 ID 0으로 저장하고, 필터링에 쓸 수 있도록 null 값에 대한 비트맵 항목도 함께 저장합니다.
- 숫자 컬럼은 null 여부를 검사하는 집계·필터 연산을 위해 별도의 null 값 비트맵 인덱스를 유지합니다.
- 기본 모드에서는 세그먼트에 없는 디멘션이 빈 값(blank)처럼 동작하고, SQL 호환 모드에서는 null처럼 동작합니다. 마찬가지로 일부 세그먼트에 없는 메트릭은 집계 시 존재하지 않는 것처럼 처리됩니다.

#### 세그먼트 이름과 샤딩(sharding)

세그먼트 식별자는 다음 형식을 따릅니다.

```
datasource_intervalStart_intervalEnd_version_partitionNum
```

같은 간격에 파티션이 여러 개인 예시입니다.

```
foo_2015-01-01/2015-01-02_v1_0
foo_2015-01-01/2015-01-02_v1_1
foo_2015-01-01/2015-01-02_v1_2
```

새 스키마로 재색인하면 새 버전 ID가 부여됩니다.

```
foo_2015-01-01/2015-01-02_v2_0
foo_2015-01-01/2015-01-02_v2_1
foo_2015-01-01/2015-01-02_v2_2
```

하나의 간격에 존재하는 여러 세그먼트는 **블록(block)** 을 이룹니다. `shardSpec` 유형에 따라 다르지만, 대부분의 shard spec에서는 블록이 완성되어야(간격의 모든 세그먼트가 로드되어야) 해당 간격에 대한 쿼리가 완료됩니다. 예외는 linear shard spec으로, 샤드가 전부 로드되지 않아도 쿼리가 완료될 수 있습니다. 예컨대 linear shard spec을 쓰는 실시간 인제스천에서는 세그먼트가 일부만 로드된 상태에서도 쿼리가 정상 수행됩니다.

#### 세그먼트 구성 파일

세그먼트는 다음 파일들로 구성됩니다.

| 파일 | 내용 |
| --- | --- |
| `version.bin` | 세그먼트 포맷 버전을 나타내는 4바이트 정수입니다. 현재 포맷은 v9입니다. |
| `meta.smoosh` | smoosh 파일 안에 담긴 다른 파일들의 이름과 오프셋(offset) 메타데이터입니다. |
| `XXXXX.smoosh` | 여러 파일을 하나로 합친 바이너리 파일입니다. 파일 디스크립터 수를 줄이기 위해 사용하며, 최대 2GB까지 담습니다. 각 컬럼(타임스탬프 컬럼 `__time` 포함)의 데이터 파일과, 추가 세그먼트 메타데이터를 담은 `index.drd` 파일이 들어 있습니다. |

#### 압축

- 문자열·long·float·double 컬럼의 값 블록에는 기본적으로 LZ4 압축을 사용합니다.
- 문자열 컬럼의 비트맵과 숫자 컬럼의 null 값 인덱스에는 Roaring 압축을 사용합니다.
- 카디널리티가 높은 문자열 컬럼의 다중 값 필터링에서는 Roaring이 Concise보다 확실히 빠릅니다. Concise가 저장 공간은 조금 더 작을 수 있으나 필터링 성능은 떨어집니다.
- 압축은 개별 컬럼 단위가 아니라 세그먼트 수준에서 `IndexSpec` 파라미터로 설정합니다.

#### 세그먼트 간 스키마 차이

같은 데이터소스 안에서도 세그먼트마다 스키마가 다를 수 있습니다. 어떤 세그먼트에 특정 디멘션이나 메트릭이 없어도 여러 세그먼트에 걸친 쿼리는 정상 동작합니다. 없는 디멘션은 빈 값(SQL 호환 모드에서는 null)처럼, 없는 메트릭은 존재하지 않는 것처럼 처리됩니다.

#### 세그먼트 갱신의 함의

Druid의 MVCC 버전은 세그먼트 포맷 버전(v9)과는 별개의 개념입니다. 여러 간격에 걸친 갱신은 전체가 아니라 **간격 단위로만 원자적**입니다. v2 세그먼트가 v1을 교체하는 도중에는 클러스터에 v1과 v2가 섞여 있을 수 있습니다.

```
갱신 전:  foo_2015-01-01/2015-01-02_v1_0
          foo_2015-01-02/2015-01-03_v1_1
          foo_2015-01-03/2015-01-04_v1_2

로드 중:  foo_2015-01-01/2015-01-02_v1_0
          foo_2015-01-02/2015-01-03_v2_1
          foo_2015-01-03/2015-01-04_v1_2
```

v2 세그먼트는 완성되는 즉시 클러스터에 로드되어 겹치는 시간 구간의 v1 세그먼트를 교체하므로, 전환 기간의 쿼리는 v1과 v2가 섞인 결과를 볼 수 있습니다.

---

### 딥 스토리지

딥 스토리지는 세그먼트를 저장하는 곳으로, Apache Druid가 직접 제공하지 않는 외부 스토리지 메커니즘입니다. 딥 스토리지의 핵심 역할은 **데이터 내구성(durability)** 입니다. Druid 프로세스들이 이 스토리지에 접근해 세그먼트를 가져올 수만 있다면, Druid 노드를 아무리 많이 잃어도 데이터는 유실되지 않습니다.

세그먼트를 딥 스토리지에만 둘지, 딥 스토리지와 Historical 프로세스 양쪽에 둘지는 설정한 로드 규칙(load rule)에 따라 결정됩니다.

#### 지원 구현체

**로컬(local)**

단일 서버 배포, 또는 NFS 같은 공유 파일시스템을 쓰는 다중 서버 환경에 적합합니다. 프로덕션 클러스터에서는 확장성과 견고성 면에서 클라우드 기반 스토리지가 더 낫습니다.

| 속성 | 가능한 값 | 설명 | 기본값 |
| --- | --- | --- | --- |
| `druid.storage.type` | `local` | 필수 설정입니다. | — |
| `druid.storage.storageDirectory` | 임의의 로컬 디렉터리 | 세그먼트를 저장할 디렉터리입니다. 세그먼트 캐시 위치와 달라야 합니다. | `/tmp/druid/localStorage` |
| `druid.storage.zip` | `true`, `false` | 세그먼트를 디렉터리로 저장할지 zip 파일로 저장할지 지정합니다. | `false` |

예시 설정:

```
druid.storage.type=local
druid.storage.storageDirectory=/tmp/druid/localStorage
```

**Amazon S3 및 S3 호환 스토리지**

Minio 같은 S3 호환 스토리지를 포함합니다. `druid-s3-extensions` 익스텐션(extension) 문서를 참고합니다.

**Google Cloud Storage**

`druid-google-extensions` 익스텐션으로 지원합니다.

**Azure Blob Storage**

`druid-azure-extensions` 익스텐션으로 지원합니다.

**HDFS**

`druid-hdfs-storage` 익스텐션으로 지원합니다.

**기타**

위 목록 외에도 커뮤니티 익스텐션 목록에 추가적인 딥 스토리지 구현체가 있습니다.

#### 딥 스토리지에서 직접 쿼리

Druid는 주로 딥 스토리지에만 있는 세그먼트에 대한 쿼리도 지원합니다. Historical 프로세스에 로드된 세그먼트를 쿼리할 때보다 성능은 낮지만, Historical 프로세스의 수나 용량을 늘리지 않고도 더 많은 데이터에 접근할 수 있어 전체 스토리지 비용을 낮출 수 있습니다. 즉 쿼리 지연 시간을 어느 정도 감수하는 대신 비용 효율을 얻는 트레이드오프입니다. 자세한 내용은 Query from deep storage 문서를 참고합니다.

---

### 메타데이터 스토리지

Druid는 시스템에 관한 각종 메타데이터를 외부 메타데이터 스토리지에 보관합니다. 실제 데이터를 저장하는 곳이 아니라, Druid 클러스터가 동작하는 데 필수적인 메타데이터만 담습니다.

#### 지원 데이터베이스

- **Derby**: 기본값이지만 프로덕션에는 적합하지 않습니다.
- **MySQL**, **PostgreSQL**: 프로덕션 환경에 권장합니다.

메타데이터 스토리지는 산발적인 태스크 실패를 막기 위해 반드시 ACID를 준수해야 합니다.

Derby 설정 예시(Coordinator 설정, `metadata.storage.*` 속성은 Derby를 쓰는 경우 Coordinator를 가리켜야 합니다):

```
druid.metadata.storage.type=derby
druid.metadata.storage.connector.connectURI=jdbc:derby://localhost:1527//opt/var/druid_state/derby;create=true
```

#### 저장 내용

메타데이터 스토리지에는 다음 다섯 범주의 레코드가 저장됩니다.

- 세그먼트 레코드
- 규칙(rule) 레코드
- 설정(configuration) 레코드
- 태스크 관련 테이블
- 감사(audit) 레코드

#### 세그먼트 테이블

`druid.metadata.storage.tables.segments` 속성으로 제어합니다. Coordinator가 이 테이블을 폴링해 클러스터에서 사용 가능해야 하는 세그먼트 집합을 결정합니다.

- `used` 컬럼: 1이면 세그먼트를 로드해야 함을, 0이면 언로드해야 함을 나타냅니다.
- `used_status_last_updated` 컬럼: `used` 상태가 마지막으로 변경된 시각입니다. 사용하지 않게 된 세그먼트를 메타데이터 스토리지에서 완전히 삭제할 후보인지 판단할 때 유용합니다.
- `payload` 컬럼: 세그먼트 전체 메타데이터를 JSON으로 저장합니다. dataSource, interval, version, loadSpec, dimensions, metrics, shardSpec, binaryVersion, size, identifier 필드를 포함합니다.

#### 규칙 테이블

세그먼트를 클러스터 어디에 배치할지 결정하는 규칙을 저장합니다. Coordinator가 세그먼트 할당을 결정할 때 이 규칙을 참고합니다.

#### 설정 테이블

런타임 설정 객체를 저장합니다. 클러스터 전체 설정을 실행 중에 변경할 수 있도록 하기 위한 용도입니다.

#### 태스크 관련 테이블

Overlord와 Middle Manager가 태스크를 관리하면서 생성하고 사용합니다.

#### 감사 테이블

Coordinator 규칙 변경을 비롯한 설정 변경 이력을 기록합니다.

#### 커넥션 풀(DBCP) 설정

`druid.metadata.storage.connector.dbcp.` 접두사로 데이터베이스 커넥션 풀 속성을 지정할 수 있습니다. 단, `username`, `password`, `connectURI`, `validationQuery`, `testOnBorrow`는 `druid.metadata.storage.connector.` 접두사를 사용해야 합니다.

#### 접근 프로세스

메타데이터 스토리지에 접근하는 프로세스는 다음 세 가지뿐입니다.

1. 인덱싱 서비스 프로세스
2. Realtime 프로세스
3. Coordinator 프로세스

따라서 네트워크 수준에서 이 프로세스들만 메타데이터 스토리지에 접근하도록 권한을 제한해야 합니다.

---

### ZooKeeper

Druid는 현재 클러스터 상태 관리에 Apache ZooKeeper(ZK)를 사용합니다. ZooKeeper 릴리스 페이지에서 받을 수 있는 모든 안정(stable) 버전을 지원합니다.

#### ZooKeeper가 담당하는 작업

1. **Coordinator 리더 선출**
2. **Historical의 세그먼트 "게시" 프로토콜**: Historical과 Realtime 프로세스가 자신의 존재와 서빙 중인 세그먼트를 알립니다.
3. **Overlord 리더 선출**
4. **Overlord–Middle Manager 태스크 관리**

#### 리더 선출

Coordinator 리더 선출은 Curator의 LeaderLatch 레시피를 사용하며, 다음 경로에서 수행됩니다.

```
${druid.zk.paths.coordinatorPath}/_COORDINATOR
```

Overlord 리더 선출도 같은 방식의 메커니즘을 사용합니다.

#### 세그먼트 게시(announcement) 프로토콜

Historical 프로세스는 두 가지 znode 경로를 사용합니다.

- **announcements 경로**: 각 Historical 서비스가 자신의 존재를 알리는 임시(ephemeral) znode를 만듭니다.

```
${druid.zk.paths.announcementsPath}/${druid.host}
```

- **live segments 경로**: 영구(permanent) znode를 만든 뒤,

```
${druid.zk.paths.liveSegmentsPath}/${druid.host}
```

세그먼트를 로드할 때마다 그 아래에 세그먼트별 임시 znode를 추가합니다.

```
${druid.zk.paths.liveSegmentsPath}/${druid.host}/_segment_identifier_
```

Coordinator와 Broker는 이 경로들을 감시(watch)해 클러스터의 어떤 프로세스가 어떤 세그먼트를 서빙 중인지 추적합니다.

---

### 참고 자료

- [Architecture](https://druid.apache.org/docs/latest/design/architecture)
- [Storage overview](https://druid.apache.org/docs/latest/design/storage)
- [Segments](https://druid.apache.org/docs/latest/design/segments)
- [Deep storage](https://druid.apache.org/docs/latest/design/deep-storage)
- [Metadata storage](https://druid.apache.org/docs/latest/design/metadata-storage)
- [ZooKeeper](https://druid.apache.org/docs/latest/design/zookeeper)

---

## Druid 프로세스와 서비스

> 원본: https://druid.apache.org/docs/latest/design/architecture
> 원본: https://druid.apache.org/docs/latest/design/coordinator
> 원본: https://druid.apache.org/docs/latest/design/overlord
> 원본: https://druid.apache.org/docs/latest/design/broker
> 원본: https://druid.apache.org/docs/latest/design/historical
> 원본: https://druid.apache.org/docs/latest/design/middlemanager
> 원본: https://druid.apache.org/docs/latest/design/indexer
> 원본: https://druid.apache.org/docs/latest/design/peons
> 원본: https://druid.apache.org/docs/latest/design/router

Druid를 구성하는 서비스(프로세스)의 종류와 역할, 서버 유형별 배치 방식, 그리고 각 서비스의 동작 방식과 설정을 설명합니다.

---

### 목차

1. [서비스와 서버 유형 개요](#서비스와-서버-유형-개요)
2. [Coordinator](#coordinator)
3. [Overlord](#overlord)
4. [Broker](#broker)
5. [Historical](#historical)
6. [Middle Manager](#middle-manager)
7. [Peon](#peon)
8. [Indexer](#indexer)
9. [Router](#router)
10. [참고 자료](#참고-자료)

---

### 서비스와 서버 유형 개요

#### 서비스 종류

Druid는 여러 종류의 서비스로 구성됩니다.

| 서비스 | 역할 |
| --- | --- |
| **Coordinator** | 클러스터의 데이터 가용성(availability)을 관리합니다. |
| **Overlord** | 데이터 인제스천(ingestion) 워크로드의 할당을 제어합니다. |
| **Broker** | 외부 클라이언트의 쿼리를 처리합니다. |
| **Router** | 요청을 Broker, Coordinator, Overlord로 라우팅합니다. |
| **Historical** | 쿼리 가능한 데이터를 저장합니다. |
| **Middle Manager**와 **Peon** | 데이터를 적재합니다. Peon은 Middle Manager가 생성하는 별도 JVM입니다. |
| **Indexer** | Middle Manager + Peon 조합을 대체하는 실험적(experimental) 서비스입니다. |

#### 서버 유형

Druid 서비스는 목적에 따라 세 가지 서버 유형에 나누어 배치하는 방식을 권장합니다.

**Master 서버** — 데이터 인제스천과 가용성을 관리합니다.

- **Coordinator**: Historical 서비스를 감시하고 세그먼트(segment)를 할당하며 균형을 유지합니다.
- **Overlord**: Middle Manager를 감시하고 인제스천 태스크(task)를 할당하며 세그먼트 발행(publish)을 조율합니다.

**Query 서버** — 사용자와 클라이언트가 접근하는 엔드포인트를 제공합니다.

- **Broker**: 외부 쿼리를 받아 Data 서버로 전달하고 결과를 병합합니다.
- **Router**: 통합 API 게이트웨이 역할을 하며, 관리용 웹 콘솔을 제공합니다.

**Data 서버** — 인제스천 작업을 실행하고 쿼리 가능한 데이터를 저장합니다.

- **Historical**: 과거 데이터와 커밋된 스트리밍 데이터의 저장·쿼리를 담당합니다.
- **Middle Manager**: 새 데이터를 적재하고 세그먼트를 발행합니다.
- **Peon**: Middle Manager가 별도 JVM으로 생성해 개별 태스크를 실행합니다.

#### 배치 권장 사항

- 세그먼트 수가 매우 많은 클러스터에서는 Coordinator와 Overlord를 분리해 자원을 각각 할당하는 편이 좋습니다.
- 인제스천 부하나 쿼리 부하가 커지면 Historical과 Middle Manager를 서로 다른 호스트에 배치해 CPU·메모리 경합을 피할 수 있습니다.
- 인제스천 실행 방식은 MiddleManager, MiddleManager 없는 Kubernetes 기반 인제스천, Indexer 중 하나만 선택해 배포하는 것이 일반적이며, 여러 방식을 동시에 운용하지 않습니다.

---

### Coordinator

Coordinator는 세그먼트의 생명주기(lifecycle)를 관리하는 서비스입니다. 설정에 따라 Historical 서비스에 세그먼트를 로드하거나 삭제하도록 지시하고, 세그먼트가 적절히 복제되도록 보장하며, 클러스터 전체에 세그먼트가 고르게 분포하도록 균형을 맞춥니다.

#### 실행

```
org.apache.druid.cli.Main server coordinator
```

#### 주요 기능

**세그먼트 관리**

- 새 세그먼트 로드와 오래된 세그먼트 삭제
- Historical 노드 간 복제 유지
- 클러스터 균형을 위한 세그먼트 재배치

**정리(cleanup) 작업**

- 새 데이터로 대체된(overshadowed) 이전 버전 세그먼트 제거
- 특정 조건(-INF에서 시작하거나 INF에서 끝나며 core partition이 0인 경우)을 만족하는 미사용 eternity tombstone 세그먼트 제거

**세그먼트 가용성**

Historical 서비스가 재시작하면 Coordinator가 장애를 감지하고 해당 세그먼트를 다른 노드에 재할당합니다. 다만 설정한 수명(lifetime)이 지나면 만료되는 임시 구조를 유지하기 때문에, 노드가 빠르게 복구되면 불필요한 재할당을 하지 않습니다.

#### 밸런싱 전략

세그먼트 분배에는 세 가지 전략이 있습니다.

| 전략 | 설명 |
| --- | --- |
| **cost** (기본값) | 데이터 구간(interval)의 근접도에 기반한 배치 비용을 최소화합니다. 시간상 인접한 세그먼트가 같은 서버에 몰리지 않게 합니다. |
| **diskNormalized** | 서버의 디스크 사용 비율로 비용에 가중치를 둡니다. 알려진 문제가 있습니다. |
| **random** | 실험적 전략으로, 프로덕션에서는 권장하지 않습니다. |

모든 전략은 디스크 여유가 가장 부족한 Historical에서 세그먼트를 우선 옮깁니다.

#### 자동 컴팩션(Automatic Compaction)

Coordinator는 작은 세그먼트를 병합하거나 큰 세그먼트를 분할하는 컴팩션을 관리합니다. 컴팩션에 쓸 수 있는 태스크 용량은 다음과 같이 계산합니다.

```
min(전체 worker capacity의 합 * slotRatio, maxSlots)
```

컴팩션이 활성화되어 있으면 최소 한 개의 컴팩션 태스크는 항상 제출됩니다.

Coordinator의 듀티(duty)는 별도 그룹으로 분리해 실행 주기를 따로 지정할 수 있습니다.

```
druid.coordinator.dutyGroups
druid.coordinator.<SOME_GROUP_NAME>.duties
druid.coordinator.<SOME_GROUP_NAME>.period
```

#### 연결과 설정

Coordinator는 클러스터 정보를 위해 ZooKeeper에 연결하고, 세그먼트 메타데이터와 로드 규칙(rule)을 위해 메타데이터 데이터베이스에 연결합니다. 상세 설정은 [Coordinator configuration](https://druid.apache.org/docs/latest/configuration/#coordinator)과 [Rule Configuration](https://druid.apache.org/docs/latest/operations/rule-configuration)을 참고하십시오.

#### FAQ 요점

- 클라이언트는 Coordinator에 직접 접속하지 않습니다.
- Historical과 Broker 서비스는 Coordinator의 존재를 인지하지 않습니다.
- 서비스 시작 순서는 중요하지 않습니다. Coordinator가 없으면 토폴로지 변경(세그먼트 로드·삭제·재배치)만 일어나지 않을 뿐입니다.

---

### Overlord

Overlord는 태스크의 생명주기를 관리합니다. 태스크를 접수하고, 태스크 분배를 조율하고, 태스크 락(lock)을 생성하며, 호출자에게 상태를 반환합니다.

#### 실행 모드

| 모드 | 설명 |
| --- | --- |
| **local** (기본값) | Overlord가 직접 Peon을 생성해 태스크를 실행합니다. Middle Manager와 Peon 설정이 함께 필요하며, 단순한 워크플로에 적합합니다. |
| **remote** | Overlord와 Middle Manager가 서로 다른 서버에서 별도 서비스로 동작합니다. indexing service를 Druid 인제스천의 주 엔드포인트로 사용할 때 권장합니다. |

#### 워커 블랙리스트(Blacklisted Workers)

Middle Manager의 태스크 실패가 임계값을 초과하면 Overlord가 해당 워커를 블랙리스트에 올립니다. 관련 설정은 다음과 같습니다.

```
druid.indexer.runner.maxRetriesBeforeBlacklist
druid.indexer.runner.workerBlackListBackoffTime
druid.indexer.runner.workerBlackListCleanupPeriod
druid.indexer.runner.maxPercentageBlacklistWorkers
```

동시에 블랙리스트에 올릴 수 있는 Middle Manager는 최대 20%이며, 블랙리스트에 오른 워커는 주기적으로 다시 화이트리스트로 돌아옵니다.

#### 오토스케일링(Autoscaling)

오토스케일링을 활성화하면, 태스크가 오랫동안 pending 상태로 남을 때 새 Middle Manager를 추가하고, 오랫동안 유휴 상태인 Middle Manager를 제거합니다.

상세 설정은 [Overlord configuration](https://druid.apache.org/docs/latest/configuration/#overlord)을, HTTP 엔드포인트는 Service status API reference를 참고하십시오.

---

### Broker

Broker는 분산 클러스터 구성에서 쿼리를 라우팅하는 서비스입니다. 어떤 세그먼트가 어느 서비스에 있는지를 담은 ZooKeeper 메타데이터를 해석해 쿼리를 알맞은 서비스로 전달하고, 개별 서비스가 반환한 결과 집합을 병합해 최종 결과를 만듭니다.

#### 실행

```
org.apache.druid.cli.Main server broker
```

#### 쿼리 전달(Forwarding Queries)

Broker는 ZooKeeper 정보를 바탕으로 세그먼트의 타임라인과 각 세그먼트를 서빙하는 Historical·Peon 서비스를 파악합니다. 시간 구간이 포함된 쿼리가 들어오면, 해당 데이터소스의 타임라인에서 쿼리 구간을 조회해 데이터를 가진 서비스를 찾아 쿼리를 전달합니다.

#### 캐싱(Caching)

Broker는 LRU 무효화 전략을 쓰는 캐시로 세그먼트별(per-segment) 결과를 저장합니다. 캐시는 로컬에 둘 수도 있고 memcached로 분산 구성할 수도 있습니다. 실시간(real-time) 세그먼트는 내용이 계속 변해 캐시된 결과를 신뢰할 수 없으므로 캐시하지 않습니다.

상세 설정은 [Broker configuration](https://druid.apache.org/docs/latest/configuration/#broker)을 참고하십시오.

---

### Historical

Historical은 과거 데이터의 저장과 쿼리를 담당하는 서비스입니다. 세그먼트를 로컬에 캐시하고, 디스크 캐시와 메모리에서 쿼리를 서빙합니다.

#### 실행

```
org.apache.druid.cli.Main server historical
```

#### 세그먼트 로딩

Historical은 딥 스토리지(deep storage)에서 세그먼트 파일을 받아 로컬 세그먼트 캐시에 저장합니다. Coordinator는 Historical과 직접 통신하지 않고 ZooKeeper의 load queue 경로를 통해 세그먼트 할당을 지시합니다. Historical은 큐에서 새 항목을 감지하면 다음 과정을 수행합니다.

1. ZooKeeper에서 세그먼트 메타데이터를 조회합니다.
2. 딥 스토리지에서 세그먼트를 내려받아 처리합니다.
3. ZooKeeper의 served segments 경로를 통해 해당 세그먼트를 서빙 중임을 알립니다.

#### 메모리 맵 캐시

세그먼트 캐시는 메모리 매핑(memory mapping)을 사용하므로, 자주 접근하는 세그먼트 부분은 운영 체제가 메모리에 유지합니다. 쿼리 성능은 필요한 데이터가 메모리 맵 캐시에 있는지, 디스크 읽기가 필요한지에 따라 달라집니다. 시스템 메모리가 클수록 세그먼트가 메모리에 남을 확률이 높아져 쿼리 응답이 빨라집니다.

#### 쿼리

Historical 서비스는 쿼리 로깅과 메트릭 리포팅을 지원해 성능 모니터링과 분석에 활용할 수 있습니다.

상세 설정은 [Historical configuration](https://druid.apache.org/docs/latest/configuration/#historical)을 참고하십시오.

---

### Middle Manager

Middle Manager는 제출된 태스크를 실행하는 워커(worker) 서비스입니다. 태스크를 직접 실행하지 않고, 별도 JVM에서 동작하는 Peon에게 전달합니다. 이 구조 덕분에 태스크마다 자원과 로그가 격리됩니다.

- Peon 하나는 한 번에 태스크 하나만 실행합니다.
- Middle Manager 하나는 여러 Peon을 관리할 수 있습니다.

#### 실행

```
org.apache.druid.cli.Main server middleManager
```

상세 설정은 [Middle Manager and Peon configuration](https://druid.apache.org/docs/latest/configuration/#middle-manager-and-peon)을, HTTP 엔드포인트는 [Service status API reference](https://druid.apache.org/docs/latest/api-reference/service-status-api#middle-manager)를 참고하십시오.

---

### Peon

Peon은 Middle Manager가 생성하는 태스크 실행 엔진입니다. 각 Peon은 별도 JVM으로 실행되며 단일 태스크 하나의 실행을 담당합니다. Peon은 항상 자신을 생성한 Middle Manager와 같은 호스트에서 동작합니다.

#### 실행

Peon은 보통 Middle Manager가 생성하므로 단독으로 실행하는 일은 드물지만, 필요하다면 다음 명령으로 실행할 수 있습니다.

```
org.apache.druid.cli.Main internal peon <task_file> <status_file>
```

- `task_file`: 태스크 JSON 객체가 담긴 파일 경로
- `status_file`: 태스크 상태를 기록할 파일 경로

프로덕션 환경에서는 Peon을 Middle Manager와 분리해 단독으로 운용하는 경우가 거의 없습니다.

상세 설정은 Peon Query Configuration과 Additional Peon Configuration 문서를 참고하십시오.

---

### Indexer

Indexer는 Middle Manager + Peon 조합을 대체하는 선택적(optional)·실험적(experimental) 서비스입니다. 태스크를 별도 프로세스가 아니라 단일 JVM 안의 스레드로 실행하므로, 설정과 배포가 더 간단하고 자원 공유 효율이 높습니다.

배치(batch) 인제스천에는 Middle Manager + Peon 시스템 또는 Kubernetes 기반 대안을 권장하며, Indexer는 설정을 단순화하고 싶은 스트리밍 인제스천 워크로드에 적합합니다.

#### 실행

```
org.apache.druid.cli.Main server indexer
```

#### 주요 설정

| 속성 | 설명 |
| --- | --- |
| `druid.worker.globalIngestionHeapLimitBytes` | 인제스천에 쓸 전역 힙 한도. 기본값은 JVM 힙의 1/6입니다. |
| `druid.worker.capacity` | 태스크 슬롯 수 |
| `druid.worker.numConcurrentMerges` | 동시 병합 수. 기본값은 capacity/2(내림)입니다. |
| `druid.server.http.numThreads` | chat handler용과 일반 요청용으로 같은 크기의 스레드 풀을 만듭니다. |

#### 태스크 자원 공유

**메모리 관리**

전역 인제스천 힙 한도는 태스크 슬롯에 균등하게 분배됩니다. 태스크별 힙 한도가 태스크의 `maxBytesInMemory` 설정을 재정의(override)하며, 최대 메모리 사용량은 대략 `maxBytesInMemory * (2 + maxPendingPersists)`에 이릅니다.

**쿼리 자원**

처리 스레드, 버퍼, (선택 시) 캐시는 공유 엔드포인트를 통해 모든 태스크가 함께 사용합니다.

**HTTP 스레드**

같은 크기의 스레드 풀 두 개가 각각 태스크 제어 메시지와 일반 요청을 처리하고, lookup 처리용 스레드 두 개가 별도로 존재합니다.

#### 현재 제한 사항

- 태스크별 로그를 따로 제공하지 않습니다. 모든 태스크 로그가 Indexer 서비스 로그에 함께 기록됩니다.
- 모든 태스크에 동일한 메모리 한도가 적용됩니다. 이 제약은 이후 릴리스에서 제거할 계획입니다.

상세 설정은 Indexer Configuration 문서를 참고하십시오.

---

### Router

Router는 여러 Broker 서비스에 쿼리를 분배하는 서비스로, 테라바이트급 이상의 클러스터에서 특히 유용합니다. 데이터 관리와 클러스터 모니터링을 위한 웹 콘솔도 Router가 호스팅합니다.

#### 실행

```
org.apache.druid.cli.Main server router
```

#### 주요 설정

| 속성 | 설명 |
| --- | --- |
| `druid.router.defaultBrokerServiceName` | 기본으로 사용할 Broker 서비스 |
| `druid.router.coordinatorServiceName` | Coordinator 서비스 이름 |
| `druid.router.tierToBrokerMap` | 티어(tier) 이름을 Broker 서비스에 매핑 |
| `druid.router.http.numConnections` | 커넥션 풀 크기 |
| `druid.router.http.readTimeout` | 요청 타임아웃 |
| `druid.router.http.numMaxThreads` | 프록시 스레드 수 |
| `druid.server.http.numThreads` | 서버 스레드 풀 크기 |

#### 관리 프록시(Management Proxy)

`druid.router.managementProxy.enabled=true`로 활성화하면 Router가 Coordinator·Overlord API의 프록시 역할을 합니다.

| 경로 | 대상 |
| --- | --- |
| `/druid/coordinator/*` | Coordinator (암시적 라우팅) |
| `/druid/indexer/*` | Overlord (암시적 라우팅) |
| `/proxy/coordinator/*` | Coordinator (명시적, 접두사 제거 후 전달) |
| `/proxy/overlord/*` | Overlord (명시적, 접두사 제거 후 전달) |

#### 라우팅 전략(Routing Strategies)

| 전략 | 설명 |
| --- | --- |
| **timeBoundary** | 모든 timeBoundary 쿼리를 우선순위가 가장 높은 Broker로 보냅니다. |
| **priority** | 쿼리의 priority 값을 기준으로 라우팅하며 `minPriority`, `maxPriority` 임계값을 설정할 수 있습니다. |
| **manual** | 쿼리 컨텍스트의 `brokerService` 값을 읽어 라우팅하고, 없으면 `defaultManualBrokerService`를 사용합니다. |
| **JavaScript** | 쿼리 명세를 처리하는 JavaScript 함수로 커스텀 라우팅 로직을 구현합니다. |

SQL 쿼리도 `druid.router.sql.enable=true`로 설정하면 기본 Broker 할당 대신 라우팅 전략을 적용합니다.

#### Avatica JDBC 쿼리 밸런싱

Avatica JDBC 연결은 connection ID를 해싱해 같은 연결의 요청이 항상 같은 Broker로 가도록 보장합니다.

**Rendezvous hash (기본값)**

```
druid.router.avatica.balancer.type=rendezvousHash
```

**Consistent hash**

```
druid.router.avatica.balancer.type=consistentHash
```

#### 프로덕션 설정 예시

```properties
druid.host=#{IP_ADDR}:8080
druid.plaintextPort=8080
druid.service=druid/router
druid.router.defaultBrokerServiceName=druid:broker-cold
druid.router.coordinatorServiceName=druid:coordinator
druid.router.tierToBrokerMap={"hot":"druid:broker-hot","_default_tier":"druid:broker-cold"}
druid.router.http.numConnections=50
druid.router.http.readTimeout=PT5M
druid.router.http.numMaxThreads=100
druid.server.http.numThreads=100
```

상세 설정은 [Router configuration](https://druid.apache.org/docs/latest/configuration/#router)을 참고하십시오.

---

### 참고 자료

- [Druid Architecture](https://druid.apache.org/docs/latest/design/architecture)
- [Coordinator service](https://druid.apache.org/docs/latest/design/coordinator)
- [Overlord service](https://druid.apache.org/docs/latest/design/overlord)
- [Broker service](https://druid.apache.org/docs/latest/design/broker)
- [Historical service](https://druid.apache.org/docs/latest/design/historical)
- [Middle Manager service](https://druid.apache.org/docs/latest/design/middlemanager)
- [Indexer service](https://druid.apache.org/docs/latest/design/indexer)
- [Peon service](https://druid.apache.org/docs/latest/design/peons)
- [Router service](https://druid.apache.org/docs/latest/design/router)
- [Configuration reference](https://druid.apache.org/docs/latest/configuration/)
- [Basic cluster tuning](https://druid.apache.org/docs/latest/operations/basic-cluster-tuning)
