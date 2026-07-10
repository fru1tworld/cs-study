# Druid 시작하기

> 원본: https://druid.apache.org/docs/latest/design/
> 원본: https://druid.apache.org/docs/latest/tutorials/
> 원본: https://druid.apache.org/docs/latest/tutorials/docker
> 원본: https://druid.apache.org/docs/latest/operations/single-server
> 원본: https://druid.apache.org/docs/latest/tutorials/cluster

Druid 소개와 적합한 사용 사례, 로컬 퀵스타트, Docker 실행, 단일 서버 배포, 클러스터 구성을 차례로 설명합니다.

---

## 목차

1. [Druid 소개](#druid-소개)
2. [로컬 퀵스타트](#로컬-퀵스타트)
3. [Docker로 실행](#docker로-실행)
4. [단일 서버 배포](#단일-서버-배포)
5. [클러스터 구성](#클러스터-구성)
6. [참고 자료](#참고-자료)

---

## Druid 소개

Apache Druid는 대규모 데이터셋에서 빠른 슬라이스 앤 다이스(slice-and-dice) 분석을 수행하기 위한 실시간 분석 데이터베이스(real-time analytics database)입니다. 빠른 집계가 필요한 분석 애플리케이션의 UI와 고동시성(high-concurrency) API의 백엔드로 주로 사용하며, 이벤트 지향(event-oriented) 데이터에 특히 강합니다.

### 주요 사용 분야

- 클릭스트림 분석 (웹, 모바일)
- 네트워크 텔레메트리 분석 (네트워크 흐름 모니터링)
- 서버 메트릭 저장
- 공급망(supply chain) 분석 (제조 메트릭)
- 애플리케이션 성능 메트릭
- 디지털 마케팅/광고 분석
- 비즈니스 인텔리전스(BI)와 OLAP
- 고객 분석, IoT 분석, 금융 분석, 헬스케어 분석, 소셜 미디어 분석

### 핵심 특징

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

### Druid가 적합한 경우

- 삽입(insert) 비율이 높지만 갱신(update)은 드문 워크로드
- 대부분의 쿼리가 집계와 리포팅("group by" 쿼리, 검색, 스캔)인 경우
- 쿼리 지연 시간 목표가 100ms에서 수 초 사이인 경우
- 데이터에 시간 요소가 있는 경우 (Druid는 시간 축에 대한 최적화와 설계를 갖추고 있습니다)
- 테이블이 여러 개여도 쿼리는 하나의 큰 분산 테이블만 대상으로 하는 경우 (작은 "lookup" 테이블과의 조인은 가능합니다)
- 카디널리티가 높은 컬럼(URL, 사용자 ID 등)에 대해 빠른 카운트와 랭킹이 필요한 경우
- Kafka, HDFS, 플랫 파일, Amazon S3 같은 객체 스토리지에서 데이터를 로드하는 경우

### Druid가 적합하지 않은 경우

- 기본 키(primary key)를 이용한 기존 레코드의 저지연 갱신이 필요한 경우 — Druid는 스트리밍 삽입은 지원하지만 스트리밍 갱신은 지원하지 않으며, 갱신은 백그라운드 배치 작업으로 수행합니다
- 쿼리 지연 시간이 중요하지 않은 오프라인 리포팅 시스템을 구축하는 경우
- 큰 테이블끼리 조인하는 "빅 조인"이 필요하고 그 쿼리가 오래 걸려도 괜찮은 경우

---

## 로컬 퀵스타트

Druid를 노트북 같은 단일 장비에 설치하고 샘플 데이터를 인제스천하고 조회하는 과정입니다.

### 사전 요구 사항

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

### 설치

[Apache Druid 다운로드 페이지](https://druid.apache.org/downloads/)에서 배포판을 내려받아 압축을 풀고 디렉터리로 이동합니다.

```bash
tar -xzf apache-druid-37.0.0-bin.tar.gz
cd apache-druid-37.0.0
```

### Druid 시작

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

### 샘플 데이터 로드

배포판에는 2015-09-12 하루 동안의 Wikipedia 편집 이벤트 샘플이 포함되어 있습니다. 웹 콘솔에서 다음 절차로 적재합니다.

1. **Query** 뷰로 이동해 **Connect external data**를 클릭합니다.
2. **Local disk**를 선택하고 다음 값을 입력합니다.
   - Base directory: `quickstart/tutorial/`
   - File filter: `wikiticker-2015-09-12-sampled.json.gz`
3. **Connect data**를 클릭합니다.
4. Parse 화면에서 데이터가 올바르게 파싱되는지 확인하고 **Done**을 클릭합니다.
5. 데이터소스 이름을 `wikiticker-2015-09-12-sampled`에서 `wikipedia`로 변경합니다.
6. **Run**을 클릭해 인제스천을 실행합니다.

### 데이터 쿼리

인제스천이 완료되면 Query 뷰에서 SQL로 조회합니다.

```sql
SELECT channel, COUNT(*) FROM "wikipedia"
GROUP BY channel ORDER BY COUNT(*) DESC
```

### 중지와 초기화

- **중지**: Druid를 실행한 터미널에서 `CTRL+C`를 누릅니다.
- **초기화**: 완전히 새로운 상태로 되돌리려면 중지 후 `apache-druid-37.0.0/var` 디렉터리를 삭제하고 다시 시작합니다.

---

## Docker로 실행

Docker Compose로 Druid 클러스터 전체를 컨테이너로 띄울 수 있습니다.

### 사전 요구 사항

- Docker 설치
- 기본 `docker-compose.yml` 기준으로 Docker에 최소 6GiB 메모리를 할당해야 합니다. 각 컨테이너는 최대 7GiB(직접 메모리 6GiB + 힙 1GiB)까지 사용할 수 있습니다.
- 컨테이너가 에러 코드 137로 종료된다면 메모리 부족이 원인일 가능성이 높습니다. macOS에서는 Docker Desktop 환경설정에서 메모리 할당량을 늘립니다.

### 구성

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

### environment 파일의 주요 변수

| 변수 | 설명 |
| --- | --- |
| `DRUID_MAXDIRECTMEMORYSIZE` | 직접 메모리 크기 (기본 6GiB) |
| `DRUID_XMX` | 최대 힙 크기 (기본 1GB) |
| `DRUID_XMS` | 초기 힙 크기 (기본 1GB) |
| `DRUID_SINGLE_NODE_CONF` | 사용할 단일 서버 프로필 (기본 `micro-quickstart`, 더 작은 환경에서는 `nano-quickstart` 등) |
| `DRUID_CONFIG_COMMON`, `DRUID_CONFIG_${service}` | 프로덕션 환경에서 커스텀 설정 파일 경로 지정 |
| `DRUID_LOG4J`, `DRUID_LOG_LEVEL` | 로깅 설정과 로그 레벨 |

`druid_` 접두사가 붙은 환경 변수는 Java 명령줄 프로퍼티로 변환됩니다. 예를 들어 `druid_metadata_storage_type=postgresql`은 `-Ddruid.metadata.storage.type=postgresql`이 됩니다.

### 실행

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

### 컨테이너 내부 확인과 데이터 보존

컨테이너 셸에 접속하려면 다음 명령을 사용합니다.

```bash
docker exec -ti <id> sh
```

- Druid 설치 경로: `/opt/druid`
- 시작 스크립트: `/druid.sh`

데이터는 Docker 볼륨에 저장되므로 클러스터를 재시작해도 유지됩니다. 이후 퀵스타트 튜토리얼의 절차대로 데이터를 로드하고 쿼리하면 됩니다. 프로덕션에서는 필요한 서비스 의존성에 맞게 Compose 파일을 커스터마이징합니다.

---

## 단일 서버 배포

한 대의 서버에 Druid 전체를 배포할 때는 `bin/start-druid` 스크립트를 사용하는 것이 표준 방식입니다.

### 자동 설정 (bin/start-druid)

`bin/start-druid`는 시스템 리소스에 맞게 메모리를 자동으로 설정합니다.

- 기본적으로 시스템의 모든 프로세서를 사용합니다
- 시스템 메모리의 최대 80%까지 사용할 수 있습니다
- 설정은 `conf/druid/auto`에서 읽습니다

### 사전 구성 프로필 (deprecated)

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

## 클러스터 구성

프로덕션 규모에서는 프로세스를 서버 역할별로 나눠 여러 장비에 배포합니다.

### 클러스터 아키텍처

권장 아키텍처는 세 가지 서버 유형으로 구성됩니다.

| 서버 유형 | 실행 프로세스 | 역할 |
| --- | --- | --- |
| Master 서버 | Coordinator, Overlord | 클러스터의 메타데이터와 코디네이션을 담당합니다. |
| Data 서버 | Historical, Middle Manager | 실제 데이터 저장과 인제스천(indexing)을 수행합니다. CPU, RAM, SSD의 이점을 크게 받습니다. |
| Query 서버 | Broker, Router | Broker는 외부 클라이언트의 쿼리를 받아 클러스터에 분배하고, 선택적으로 인메모리 쿼리 캐시를 유지합니다. Router는 선택적인 프록시입니다. |

### 하드웨어 권장 사항

| 서버 | AWS 기준 | 사양 | 설정 경로 |
| --- | --- | --- | --- |
| Master 서버 | m5.2xlarge | 8 vCPU, 32GiB RAM | `conf/druid/cluster/master` |
| Data 서버 (2대) | i3.4xlarge | 각 16 vCPU, 122GiB RAM, 2×1.9TB SSD | `conf/druid/cluster/data` |
| Query 서버 | m5.2xlarge | 8 vCPU, 32GiB RAM | `conf/druid/cluster/query` |

### 배포판 준비

각 서버에서 배포판 압축을 풀고 디렉터리로 이동합니다.

```bash
tar -xzf apache-druid-37.0.0-bin.tar.gz
cd apache-druid-37.0.0
```

클러스터 공통 설정은 `conf/druid/cluster/_common/common.runtime.properties`에서 수정합니다.

### 메타데이터 저장소 설정

`conf/druid/cluster/_common/common.runtime.properties`에서 다음 프로퍼티를 메타데이터 저장소 주소로 변경합니다.

- `druid.metadata.storage.connector.connectURI`
- `druid.metadata.storage.connector.host`

프로덕션에서는 복제(replication) 구성을 갖춘 전용 MySQL 또는 PostgreSQL 사용을 권장합니다.

### 딥 스토리지 설정

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

### ZooKeeper 설정

`conf/druid/cluster/_common/common.runtime.properties`의 `druid.zk.service.host`에 ZooKeeper 서버들의 host:port 쌍을 쉼표로 구분해 지정합니다.

```properties
druid.zk.service.host=127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002
```

### 단일 서버 배포에서 마이그레이션

기존 단일 서버 배포의 설정을 클러스터로 옮길 때는 다음과 같이 진행합니다.

- **Master**: 기존 coordinator-overlord 설정을 `conf/druid/cluster/master/coordinator-overlord`로 복사합니다.
- **Data**: 새 하드웨어의 CPU/RAM을 Data 서버 대수로 나눈 분할 비율(split factor)에 맞춰 Historical과 Middle Manager 설정을 조정합니다.
- **Query**: 하드웨어 사양이 적절하다면 Broker와 Router 설정을 수정 없이 복사합니다.

### 서버 시작

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

### 방화벽 포트

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

## 참고 자료

- [Apache Druid 공식 문서](https://druid.apache.org/docs/latest/design/)
- [Local quickstart](https://druid.apache.org/docs/latest/tutorials/)
- [Docker tutorial](https://druid.apache.org/docs/latest/tutorials/docker)
- [Single server deployment](https://druid.apache.org/docs/latest/operations/single-server)
- [Clustered deployment](https://druid.apache.org/docs/latest/tutorials/cluster)
- [Apache Druid 다운로드](https://druid.apache.org/downloads/)
