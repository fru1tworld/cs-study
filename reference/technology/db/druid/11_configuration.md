# Druid 설정

> 원본: https://druid.apache.org/docs/latest/configuration/
> 원본: https://druid.apache.org/docs/latest/configuration/extensions
> 원본: https://druid.apache.org/docs/latest/operations/basic-cluster-tuning

설정 파일 구성과 공통 설정, JVM 설정, 프로세스별(Coordinator/Overlord/MiddleManager/Historical/Broker/Router) 핵심 프로퍼티, 익스텐션 로딩, 기본 클러스터 튜닝 방법을 정리합니다.

---

## 목차

1. [설정 파일 구성](#설정-파일-구성)
2. [JVM 공통 설정](#jvm-공통-설정)
3. [공통 설정](#공통-설정)
4. [Coordinator 설정](#coordinator-설정)
5. [Overlord 설정](#overlord-설정)
6. [MiddleManager와 Peon 설정](#middlemanager와-peon-설정)
7. [Historical 설정](#historical-설정)
8. [Broker 설정](#broker-설정)
9. [Router 설정](#router-설정)
10. [쿼리 처리 설정](#쿼리-처리-설정)
11. [익스텐션](#익스텐션)
12. [기본 클러스터 튜닝](#기본-클러스터-튜닝)
13. [참고 자료](#참고-자료)

---

## 설정 파일 구성

Druid 설정은 계층적인 디렉터리 구조로 관리합니다.

```
conf/druid/
    _common/
        common.runtime.properties   (모든 서비스가 공유하는 공통 설정)
        log4j2.xml
    broker/
        runtime.properties          (서비스별 설정)
        jvm.config                  (힙 크기 등 JVM 플래그)
    coordinator/
    historical/
    middleManager/
    router/
        ...
```

- **`_common/common.runtime.properties`**: 익스텐션, ZooKeeper, 메타데이터 스토리지, 딥 스토리지 등 모든 서비스가 공유하는 설정을 둡니다.
- **서비스별 `runtime.properties`**: 각 프로세스(Broker, Coordinator 등) 고유의 설정을 둡니다.
- **서비스별 `jvm.config`**: 프로세스마다 힙 크기와 JVM 플래그를 지정합니다.

### 프로퍼티 값 보간(interpolation)

프로퍼티 값에 동적 참조를 사용할 수 있습니다.

| 문법 | 의미 |
|---|---|
| `${sys:java.io.tmpdir}` | Java 시스템 프로퍼티 참조 |
| `${env:VARIABLE_NAME}` | 환경 변수 참조 |
| `${file:UTF-8:/path/to/file}` | 로컬 파일 내용 참조 |
| `${env:VAR:-defaultValue}` | 기본값 지정 |

보간을 막으려면 `$$` 접두사로 이스케이프합니다.

---

## JVM 공통 설정

모든 서비스의 `jvm.config`에 다음 네 가지 플래그를 권장합니다.

| 플래그 | 설명 |
|---|---|
| `-Duser.timezone=UTC` | 시간대를 UTC로 통일합니다. 다른 시간대는 테스트되지 않았습니다. |
| `-Dfile.encoding=UTF-8` | 문자 인코딩을 UTF-8로 고정합니다. 로컬 인코딩은 지원하지 않습니다. |
| `-Djava.io.tmpdir=<경로>` | Druid의 여러 구성 요소가 임시 파일을 사용하며 크기가 커질 수 있으므로, 비휘발성이고 빠른 스토리지를 지정합니다. NFS는 피합니다. |
| `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager` | 모든 로깅을 log4j2로 통합합니다. |

---

## 공통 설정

`common.runtime.properties`에 두는 설정입니다.

### 익스텐션 로딩

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.extensions.directory` | `extensions` | 익스텐션 루트 디렉터리 |
| `druid.extensions.loadList` | `null` | 로드할 익스텐션의 JSON 배열. `null`이면 디렉터리의 모든 익스텐션을 로드 |
| `druid.extensions.searchCurrentClassloader` | `true` | 메인 클래스로더에서도 익스텐션 검색 |
| `druid.extensions.useExtensionClassloaderFirst` | `false` | 익스텐션 JAR를 Druid 기본 JAR보다 우선 로드 |
| `druid.modules.excludeList` | `[]` | 로드에서 제외할 모듈 클래스의 JSON 배열 |

### ZooKeeper

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.zk.service.host` | 없음(필수) | ZooKeeper 접속 문자열 |
| `druid.zk.paths.base` | `/druid` | ZooKeeper 기본 경로 |
| `druid.zk.service.sessionTimeoutMs` | `30000` | 세션 타임아웃(ms) |
| `druid.zk.service.connectionTimeoutMs` | `15000` | 연결 타임아웃(ms) |
| `druid.zk.service.compress` | `true` | 생성하는 znode 압축 여부 |
| `druid.zk.service.acl` | `false` | ACL 보안 활성화 |
| `druid.discovery.curator.path` | `/druid/discovery` | 서비스 디스커버리 경로 |

기본 하위 경로는 `druid.zk.paths.base` 아래에 만들어집니다. 예: `druid.zk.paths.announcementsPath`는 `${druid.zk.paths.base}/announcements`, `druid.zk.paths.liveSegmentsPath`는 `${druid.zk.paths.base}/segments`입니다.

### 메타데이터 스토리지

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.metadata.storage.type` | `derby` | 백엔드 종류(`derby`, `mysql`, `postgresql`) |
| `druid.metadata.storage.connector.connectURI` | 없음 | JDBC 접속 URI |
| `druid.metadata.storage.connector.user` | 없음 | DB 사용자 |
| `druid.metadata.storage.connector.password` | 없음 | 패스워드(Password Provider 지원) |
| `druid.metadata.storage.connector.createTables` | `true` | 테이블이 없으면 자동 생성 |
| `druid.metadata.storage.tables.base` | `druid` | 테이블 이름 접두사 |

주요 메타데이터 테이블은 `druid_segments`(세그먼트 메타데이터), `druid_dataSource`(데이터소스 정의), `druid_tasks`(태스크), `druid_audit`(설정 변경 감사 로그)입니다. Derby는 단일 노드 실험용이며 클러스터에서는 MySQL 또는 PostgreSQL을 사용합니다.

### 딥 스토리지

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.storage.type` | `local` | 딥 스토리지 종류(`local`, `s3`, `hdfs`, `noop` 등) |
| `druid.storage.storageDirectory` | `/tmp/druid/localStorage` | local 타입일 때 저장 디렉터리 |

**S3 사용 시**(`druid-s3-extensions` 필요):

| 프로퍼티 | 설명 |
|---|---|
| `druid.storage.bucket` | S3 버킷 이름 |
| `druid.storage.baseKey` | 객체 키 접두사 |
| `druid.storage.disableAcl` | ACL 비활성화(기본 `false`) |

**HDFS 사용 시**(`druid-hdfs-storage` 필요):

| 프로퍼티 | 설명 |
|---|---|
| `druid.storage.storageDirectory` | HDFS 경로 |
| `druid.storage.compressionFormat` | `zip` 또는 `lz4`(기본 `zip`) |

### 태스크 로그

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.indexer.logs.type` | `file` | 태스크 로그 저장소(`file`, `s3`, `azure`, `google`, `hdfs`, `noop`) |
| `druid.indexer.logs.directory` | `log` | file 타입일 때 저장 경로 |
| `druid.indexer.logs.kill.enabled` | `false` | 오래된 태스크 로그 자동 삭제 |
| `druid.indexer.logs.kill.durationToRetain` | 없음(활성화 시 필수) | 로그 보존 기간 |
| `druid.indexer.logs.kill.delay` | `21600000`(6시간) | 삭제 주기(ms) |

### 요청 로깅과 감사 로깅

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.request.logging.type` | `noop` | 쿼리 요청 로거(`file`, `emitter`, `slf4j`, `filtered`, `composing`, `switching`) |
| `druid.request.logging.dir` | — | file 타입일 때 로그 디렉터리 |
| `druid.request.logging.filePattern` | `"yyyy-MM-dd'.log'"` | 파일명 패턴(Joda 형식) |
| `druid.request.logging.rollPeriod` | `P1D` | 로그 롤링 주기 |
| `druid.audit.manager.type` | `sql` | 감사 로그 저장 방식(`log`, `sql`) |
| `druid.audit.manager.logLevel` | `INFO` | 감사 로그 레벨 |
| `druid.audit.manager.maxPayloadSizeBytes` | `-1` | 감사 페이로드 최대 크기(-1은 무제한) |

### TLS/HTTPS

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.enablePlaintextPort` | `true` | HTTP 커넥터 활성화 |
| `druid.enableTlsPort` | `false` | HTTPS 커넥터 활성화 |
| `druid.server.https.keyStorePath` | 없음(TLS 시 필수) | KeyStore 파일 경로 |
| `druid.server.https.keyStoreType` | 없음(TLS 시 필수) | KeyStore 타입 |
| `druid.server.https.certAlias` | 없음(TLS 시 필수) | 인증서 별칭 |
| `druid.server.https.keyStorePassword` | 없음(TLS 시 필수) | KeyStore 패스워드 |

내부 서비스 간 TLS 통신에는 `simple-client-sslcontext` 익스텐션이 필요하며, `druid.client.https.protocol`(기본 `TLSv1.2`), `druid.client.https.trustStorePath`, `druid.client.https.trustStorePassword`를 설정합니다.

### 인증과 인가

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.auth.authenticatorChain` | `["allowAll"]` | 인증기(Authenticator) 체인 |
| `druid.auth.authorizers` | `["allowAll"]` | 인가기(Authorizer) 목록 |
| `druid.escalator.type` | `noop` | 내부 통신용 에스컬레이터 타입 |
| `druid.auth.unsecuredPaths` | `[]` | 인증을 건너뛸 경로 목록 |
| `druid.auth.allowUnauthenticatedHttpOptions` | `false` | 미인증 HTTP OPTIONS 허용 |

### 내부 HTTP 클라이언트

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.global.http.numConnections` | `20` | 대상 URL당 커넥션 풀 크기 |
| `druid.global.http.eagerInitialization` | `false` | 커넥션 사전 생성 |
| `druid.global.http.compressionCodec` | `gzip` | 압축 코덱(`gzip`, `identity`) |
| `druid.global.http.readTimeout` | `PT15M` | 읽기 타임아웃 |
| `druid.global.http.numMaxThreads` | `(코어 수 * 3 / 2) + 1` | 최대 I/O 스레드 수 |
| `druid.global.http.clientConnectTimeout` | `500` | 연결 타임아웃(ms) |

### 기타 공통 설정

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.javascript.enabled` | `false` | JavaScript 필터·추출기·집계기 사용 허용 |
| `druid.indexing.doubleStorage` | `double` | double 컬럼 저장 정밀도(`float`은 32비트) |
| `druid.server.hiddenProperties` | password/key/token 계열 | `/status/properties` 엔드포인트에서 숨길 프로퍼티 |
| `druid.server.http.showDetailedJettyErrors` | `true` | 오류 응답에 Jetty 상세 정보 포함 |
| `druid.server.http.errorResponseTransform.strategy` | `none` | 오류 메시지 변환 전략(`none`, `allowedRegex`) |

---

## Coordinator 설정

### 기본 서비스 설정

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.host` | 호스트의 canonical hostname | 서비스 광고 주소 |
| `druid.plaintextPort` | `8081` | HTTP 포트 |
| `druid.tlsPort` | `8281` | HTTPS 포트 |
| `druid.service` | `druid/coordinator` | 서비스 이름 |

### 운영 설정

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.coordinator.period` | `PT60S` | 코디네이션 주기 |
| `druid.coordinator.period.indexingPeriod` | `PT1800S` | 데이터 관리 듀티(컴팩션 등) 실행 주기 |
| `druid.coordinator.startDelay` | `PT300S` | 기동 후 클러스터 상태 파악을 위한 대기 시간 |
| `druid.coordinator.load.timeout` | `PT15M` | 세그먼트 할당 타임아웃 |
| `druid.coordinator.balancer.strategy` | `cost` | 세그먼트 밸런싱 전략(`cost`, `diskNormalized`, `random`) |
| `druid.manager.segments.pollDuration` | `PT1M` | 세그먼트 메타데이터 폴링 주기 |
| `druid.manager.rules.pollDuration` | `PT1M` | 룰 폴링 주기 |

### 미사용 세그먼트 정리(kill)

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.coordinator.kill.on` | `false` | 미사용 세그먼트 자동 삭제 활성화 |
| `druid.coordinator.kill.period` | `indexingPeriod`와 동일 | kill 태스크 실행 주기 |
| `druid.coordinator.kill.durationToRetain` | `P90D` | 미사용 세그먼트 보존 기간 |
| `druid.coordinator.kill.bufferPeriod` | `P30D` | 삭제 전 유예 기간 |
| `druid.coordinator.kill.maxSegments` | `100` | kill 태스크당 삭제할 세그먼트 수 |

### 동적 설정

Coordinator 동적 설정은 재시작 없이 API로 변경할 수 있습니다.

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `smartSegmentLoading` | `true` | 세그먼트 로딩 파라미터 자동 최적화 |
| `maxSegmentsToMove` | `100`(smart 모드에서는 전체의 2%) | 동시에 이동할 최대 세그먼트 수 |
| `maxSegmentsInNodeLoadingQueue` | `500` | Historical별 로딩 큐 최대 크기 |
| `replicationThrottleLimit` | `500` | 코디네이션 1회당 최대 레플리카 할당 수 |
| `replicantLifetime` | `15` | 로드 큐 대기 허용 실행 횟수 |
| `useRoundRobinSegmentAssignment` | `true` | 라운드 로빈 세그먼트 할당 |
| `pauseCoordination` | `false` | 코디네이션 듀티 전체 일시 중지 |
| `decommissioningNodes` | 없음 | 세그먼트를 비울(drain) 노드 목록 |

---

## Overlord 설정

### 기본 서비스 설정

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.plaintextPort` | `8090` | HTTP 포트 |
| `druid.tlsPort` | `8290` | HTTPS 포트 |
| `druid.service` | `druid/overlord` | 서비스 이름 |

### 태스크 실행

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.indexer.runner.type` | `httpRemote` | 태스크 실행 방식. `local`은 Overlord 내부 실행, `remote`/`httpRemote`는 MiddleManager로 분배(`httpRemote` 권장) |
| `druid.indexer.storage.type` | `local` | 태스크 상태 저장 위치(`local`, `metadata`). 클러스터에서는 `metadata` 사용 |
| `druid.indexer.storage.recentlyFinishedThreshold` | `PT24H` | 완료 태스크 결과 보존 기간 |
| `druid.indexer.queue.maxSize` | `Integer.MAX_VALUE` | 동시 활성 태스크 최대 수 |
| `druid.indexer.queue.startDelay` | `PT1M` | 큐 기동 지연 |
| `druid.indexer.queue.storageSyncRate` | `PT1M` | 상태 동기화 주기 |

### 락과 세그먼트 할당

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.indexer.tasklock.forceTimeChunkLock` | `true` | 타임 청크 단위 락 강제 |
| `druid.indexer.tasklock.batchSegmentAllocation` | `true` | 세그먼트 할당 요청 배치 처리 |
| `druid.indexer.tasklock.batchAllocationWaitTime` | `0` | 배치 실행 전 대기 시간(ms) |

### remote/httpRemote 모드

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.indexer.runner.taskAssignmentTimeout` | `PT5M` | 태스크 할당 완료 대기 시간 |
| `druid.indexer.runner.minWorkerVersion` | `"0"` | 태스크를 받을 수 있는 최소 워커 버전 |
| `druid.indexer.runner.maxRetriesBeforeBlacklist` | `5` | 워커 블랙리스트 등록 전 허용 실패 횟수 |
| `druid.indexer.runner.workerBlackListBackoffTime` | `PT15M` | 블랙리스트 해제 대기 시간 |

---

## MiddleManager와 Peon 설정

MiddleManager는 태스크를 별도 JVM(Peon)으로 포크해 실행합니다.

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.plaintextPort` | `8091` | HTTP 포트 |
| `druid.service` | `druid/middleManager` | 서비스 이름 |
| `druid.worker.capacity` | `(코어 수 * 2) - 1` | 동시 실행 가능한 태스크 슬롯 수 |
| `druid.worker.version` | — | 태스크 호환성 판별용 워커 버전 |
| `druid.indexer.runner.javaOpts` | — | Peon 프로세스에 넘길 JVM 인자 |
| `druid.indexer.task.baseTaskDir` | `var/druid/task` | 태스크 작업 디렉터리 |
| `druid.indexer.task.tmpDir` | — | Peon 임시 파일 디렉터리 |

Peon의 processing 설정은 `druid.indexer.fork.property.` 접두사로 MiddleManager `runtime.properties`에 지정합니다. 예를 들어 `druid.indexer.fork.property.druid.processing.numThreads`는 각 태스크의 처리 스레드 수를 정합니다.

---

## Historical 설정

### 스토리지와 세그먼트 캐시

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.server.maxSize` | `0` | 이 노드가 서빙할 수 있는 세그먼트 총 크기 상한 |
| `druid.server.tier` | `_default_tier` | 세그먼트 분배에 사용할 티어 이름 |
| `druid.segmentCache.locations` | 없음(필수) | 세그먼트를 내려받아 둘 로컬 디렉터리와 크기 |
| `druid.segmentCache.numLoadingThreads` | `10` | 세그먼트 병렬 로딩 스레드 수 |

### 쿼리 처리와 HTTP

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.server.http.numThreads` | `max(10, (코어 수 * 2) + 1)` | HTTP 요청 처리 스레드 수 |
| `druid.processing.numThreads` | `코어 수 - 1` | 쿼리 처리 스레드 수 |
| `druid.processing.numMergeBuffers` | `(코어 수 / 2) - 1` | 병합 버퍼 수 |
| `druid.processing.buffer.sizeBytes` | `1073741824`(1GiB) | 스레드당 처리 버퍼 크기 |

### 캐시

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.historical.cache.useCache` | `true` | 세그먼트 단위 쿼리 캐시 읽기 |
| `druid.historical.cache.populateCache` | `true` | 캐시 쓰기 |

---

## Broker 설정

### 기본 서비스 설정

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.plaintextPort` | `8082` | HTTP 포트 |
| `druid.service` | `druid/broker` | 서비스 이름 |

### 쿼리 라우팅과 처리

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.broker.balancer.type` | `random` | 같은 세그먼트를 가진 서버 중 선택 전략(`random`, `connectionCount`) |
| `druid.broker.select.tier` | `_default_tier` | 세그먼트 서빙 시 우선할 티어 전략 |
| `druid.processing.numThreads` | `코어 수 - 1` | 처리 스레드 수 |
| `druid.processing.numMergeBuffers` | `(코어 수 / 2) - 1` | groupBy 결과 병합용 버퍼 수 |
| `druid.broker.http.numConnections` | — | Historical/태스크당 아웃바운드 커넥션 수(튜닝 절 참고) |
| `druid.broker.http.maxQueuedBytes` | — | 채널당 읽기 대기 바이트 상한(백프레셔) |

### 캐시

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.broker.cache.useCache` | `true` | Broker 캐시 읽기 |
| `druid.broker.cache.populateCache` | `true` | Broker 캐시 쓰기 |
| `druid.broker.cache.defaultTtl` | `PT1H` | 캐시 엔트리 수명 |

---

## Router 설정

Router는 쿼리를 여러 Broker 그룹으로 분배해서 중요한 데이터에 대한 쿼리가 덜 중요한 데이터 쿼리의 영향을 받지 않도록 격리하며, 웹 콘솔도 호스팅합니다.

| 프로퍼티 | 기본값/예시 | 설명 |
|---|---|---|
| `druid.router.defaultBrokerServiceName` | 예: `druid:broker-cold` | 매칭 규칙이 없을 때 사용할 기본 Broker 서비스 |
| `druid.router.tierToBrokerMap` | 예: `{"hot":"druid:broker-hot","_default_tier":"druid:broker-cold"}` | 데이터 티어와 Broker 서비스 매핑 |
| `druid.router.sql.enable` | `false` | SQL 쿼리도 라우팅 전략으로 분배 |
| `druid.router.managementProxy.enabled` | `false` | Coordinator/Overlord API 프록시(웹 콘솔에 필요) |
| `druid.router.http.numConnections` | `50` | Broker로의 커넥션 수 |
| `druid.router.http.readTimeout` | `PT5M` | 읽기 타임아웃 |
| `druid.router.http.numMaxThreads` | `100` | 프록시 클라이언트 최대 스레드 수 |
| `druid.router.avatica.balancer.type` | `rendezvousHash` | JDBC(Avatica) 커넥션 분배 알고리즘 |

라우팅 전략(strategy)은 다음 네 가지가 있습니다.

- **timeBoundary**: 모든 timeBoundary 쿼리를 최우선 순위 Broker로 보냅니다.
- **priority**: 쿼리 컨텍스트의 priority 값으로 분배합니다(`minPriority` 기본 0, `maxPriority` 기본 1).
- **manual**: 쿼리 컨텍스트의 `brokerService` 파라미터로 지정한 Broker로 보냅니다.
- **JavaScript**: JavaScript 함수로 라우팅 로직을 직접 작성합니다.

---

## 쿼리 처리 설정

여러 프로세스에 공통으로 적용되는 processing/groupBy 설정입니다.

| 프로퍼티 | 기본값 | 설명 |
|---|---|---|
| `druid.processing.buffer.sizeBytes` | 1GiB | 스레드당 오프힙 처리 버퍼. TopN·GroupBy 중간 결과를 저장 |
| `druid.processing.numThreads` | `코어 수 - 1` | 쿼리 처리 스레드 수. 동시 처리 가능한 세그먼트 수를 결정 |
| `druid.processing.numMergeBuffers` | `(코어 수 / 2) - 1` | GroupBy 병합 버퍼 수. 동시 실행 가능한 GroupBy 쿼리 수를 제한 |
| `druid.processing.tmpDir` | — | 처리 중 임시 파일 위치 |
| `druid.query.groupBy.maxOnDiskStorage` | — | 버퍼가 가득 찼을 때 디스크로 스필(spill)할 수 있는 최대 크기 |
| `druid.query.groupBy.maxMergingDictionarySize` | — | 병합 딕셔너리 최대 크기 |
| `druid.query.groupBy.singleThreaded` | `false` | GroupBy 단일 스레드 실행 강제 |

GroupBy 쿼리는 중첩되지 않으면 쿼리당 병합 버퍼 1개, 중첩되면 깊이와 무관하게 2개를 사용합니다.

---

## 익스텐션

### 코어 익스텐션

코어 익스텐션은 Druid 커미터가 관리하며 배포판에 포함됩니다.

| 분류 | 익스텐션 |
|---|---|
| 딥 스토리지 | `druid-s3-extensions`, `druid-hdfs-storage`, `druid-azure-extensions`, `druid-google-extensions` |
| 메타데이터 스토리지 | `mysql-metadata-storage`, `postgresql-metadata-storage` |
| 데이터 포맷 | `druid-parquet-extensions`, `druid-avro-extensions`, `druid-orc-extensions`, `druid-protobuf-extensions` |
| 스트리밍 인제스천 | `druid-kafka-indexing-service`, `druid-kinesis-indexing-service` |
| 분석 | `druid-datasketches`, `druid-bloom-filter`, `druid-multi-stage-query` |
| 보안 | `druid-basic-security`, `druid-kerberos`, `druid-pac4j` |

### 커뮤니티 익스텐션

`druid-cassandra-storage`, `druid-redis-cache`, `druid-deltalake-extensions`, `druid-iceberg-extensions`, `prometheus-emitter`, `graphite-emitter`, `kafka-emitter` 등이 있습니다. 커뮤니티 익스텐션은 코어 익스텐션만큼 광범위하게 테스트되지 않았을 수 있습니다.

### 익스텐션 로드 방법

코어 익스텐션은 `common.runtime.properties`의 `druid.extensions.loadList`에 이름을 추가하면 됩니다.

```properties
druid.extensions.loadList=["postgresql-metadata-storage", "druid-hdfs-storage"]
```

커뮤니티 익스텐션은 `pull-deps` 도구로 Maven 좌표를 지정해 내려받은 뒤 `loadList`에 추가합니다. 커뮤니티 익스텐션의 groupId는 보통 `org.apache.druid.extensions.contrib`입니다.

```bash
java -cp "lib/*" -Ddruid.extensions.directory="extensions" \
  org.apache.druid.cli.Main tools pull-deps -c "groupId:artifactId:version"
```

---

## 기본 클러스터 튜닝

### Historical 튜닝

**힙 크기 공식:**

```
힙 = (0.5GiB × CPU 코어 수) + (2 × 전체 lookup 맵 크기) + druid.cache.sizeInBytes
```

- lookup은 갱신 시 원자적 교체를 위해 기존 맵과 새 맵이 동시에 존재하므로 2배로 계산합니다.
- 힙이 약 24GiB를 넘으면 Shenandoah나 ZGC 같은 GC를 검토합니다.

**처리 설정 권장값:**

| 프로퍼티 | 권장값 |
|---|---|
| `druid.processing.numThreads` | `코어 수 - 1` |
| `druid.processing.buffer.sizeBytes` | 500MiB |
| `druid.processing.numMergeBuffers` | 처리 스레드의 1/4 |

**다이렉트 메모리 공식:**

```
다이렉트 메모리 = (numThreads + numMergeBuffers + 1) × buffer.sizeBytes
```

`+1`은 세그먼트 압축 해제 버퍼 몫입니다.

**세그먼트 캐시:** `druid.segmentCache.locations` 총량이 시스템 여유 메모리(페이지 캐시로 쓸 수 있는 양)를 넘지 않게 잡고, 스토리지는 SSD를 강력히 권장합니다.

### Broker 튜닝

- **힙**: 소·중형 클러스터(서버 ~15대)는 4~8GiB, 대형 클러스터(~100노드)는 30~60GiB. 세그먼트 수와 전체 데이터 크기에 비례해 늘립니다.
- **다이렉트 메모리**: `druid.processing.buffer.sizeBytes` 500MiB, `numMergeBuffers`는 Historical 이상으로 설정합니다. Broker는 처리 스레드가 필요 없고 결과 병합은 힙에서 이뤄집니다.
- **백프레셔**: `druid.broker.http.maxQueuedBytes`를 대략 `2MiB × Historical 수`로 설정합니다.
- **비율**: Historical 15대당 Broker 1대를 출발점으로 하되, 고가용성을 위해 최소 2대를 둡니다.

### 커넥션 풀 사이징

- 모든 Broker의 `druid.broker.http.numConnections` 합이 각 Historical의 `druid.server.http.numThreads`보다 약간 작아야 합니다.
- Broker 자신의 `druid.server.http.numThreads`는 자신의 `numConnections`보다 약간 크게 잡습니다.
- 기준선: 프로세스당 동시 쿼리 50개 + 비쿼리 요청 10개. 예를 들어 Historical과 태스크에는 `druid.server.http.numThreads=60`을 설정합니다.
- 예시: Broker 3대가 각각 `numConnections=10`이면 Historical 하나가 받는 쿼리 커넥션은 30개이므로 Historical의 `numThreads`는 40 이상이어야 합니다.
- 풀이 너무 작으면 클러스터를 다 활용하지 못하고, 너무 크면 OOM과 리소스 경합 위험이 생깁니다.

### MiddleManager와 태스크 튜닝

- **MiddleManager 힙**: 약 128MiB면 충분합니다(태스크를 포크만 하므로 자원 요구가 적음).
- **태스크 힙 공식**: `1GiB + (2 × 전체 lookup 맵 크기)`
- **태스크 처리 설정 권장값**(MiddleManager `runtime.properties`에 지정):

```properties
druid.indexer.fork.property.druid.processing.numThreads=2
druid.indexer.fork.property.druid.processing.numMergeBuffers=2
druid.indexer.fork.property.druid.processing.buffer.sizeBytes=100000000
```

- **태스크 다이렉트 메모리**: `(numThreads + numMergeBuffers + 1) × buffer.sizeBytes`
- **전체 메모리**: `MiddleManager 힙 + (druid.worker.capacity × 태스크 1개 메모리)`
- Kafka/Kinesis 인제스천을 쓰면 컴팩션 등 다른 태스크를 위한 여유 슬롯을 확보하고, 용량이 부족하면 MiddleManager 머신을 추가합니다.

### Coordinator, Overlord, Router

- **Coordinator 힙**: Broker 힙과 같거나 약간 작게. 서버 수, 세그먼트 수, 태스크 수에 비례합니다.
- **Overlord 힙**: Coordinator 힙의 25~50%. 실행 중인 태스크 수에 비례합니다.
- 수십만 개 이상 세그먼트가 있는 대형 클러스터에서는 Coordinator 동적 설정 `percentOfSegmentsToConsiderPerMove`를 기본 100에서 66 정도로 낮춰 코디네이션 주기를 단축할 수 있습니다.
- **Router 힙**: 256MiB에서 시작합니다. Broker로 프록시만 하므로 자원 요구가 가볍습니다.

### GroupBy 튜닝 지표

`GroupByStatsMonitor`(`org.apache.druid.server.metrics.GroupByStatsMonitor`)를 켜면 다음 지표로 버퍼 크기를 조정할 수 있습니다.

| 지표 | 해석 |
|---|---|
| `mergeBuffer/maxBytesUsed` | 한계에 근접하면 `buffer.sizeBytes` 증가 |
| `groupBy/maxSpilledBytes` | 버퍼 부족 신호. 버퍼 크기 또는 `maxOnDiskStorage` 조정 |
| `groupBy/spilledQueries` | 0이 아니면 버퍼가 작다는 뜻 |
| `mergeBuffer/pendingRequests` | 0이 아니면 병합 버퍼 고갈. `numMergeBuffers` 증가 |

### 세그먼트별 다이렉트 메모리 버퍼

- 세그먼트 압축 해제: 읽는 세그먼트의 컬럼당 64KiB를 할당합니다(`64KiB × 컬럼 수 × 세그먼트 수`).
- 인제스천 중 세그먼트 병합: String 컬럼마다 `카디널리티 × 4바이트` 버퍼를 세그먼트별로 할당합니다. 이 할당은 `druid.processing.numMergeBuffers`와 무관합니다.

### JVM과 OS 튜닝

**GC 권장 설정:**

```
-XX:+UseG1GC
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
-XX:MaxDirectMemorySize=<계산값>
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/var/logs/druid/historical.gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=50
-XX:GCLogFileSize=10m
```

**시스템 권장 사항:**

- Historical, MiddleManager, Indexer에는 SSD를 강력히 권장합니다.
- Historical 디스크는 RAID보다 JBOD가 처리량 면에서 유리할 수 있습니다.
- 스왑은 사용하지 않습니다. 메모리 맵된 세그먼트 파일 때문에 성능이 예측 불가능해집니다.
- `/tmp`를 tmpfs에 마운트하면 GC 일시 정지를 줄일 수 있습니다.
- GC 로그와 Druid 로그는 데이터와 다른 디스크에 둡니다.
- Transparent Huge Pages를 비활성화합니다.
- `ulimit`(열 수 있는 파일 수)을 세그먼트 파일 수보다 충분히 크게 설정하고(`/etc/security/limits.conf`), 메모리 맵이 많은 Historical을 위해 `/proc/sys/vm/max_map_count`도 늘립니다(`/etc/sysctl.d/`).
- 모든 이벤트와 호스트에서 UTC 시간대를 사용합니다.

---

## 참고 자료

- [Configuration reference](https://druid.apache.org/docs/latest/configuration/)
- [Extensions](https://druid.apache.org/docs/latest/configuration/extensions)
- [Basic cluster tuning](https://druid.apache.org/docs/latest/operations/basic-cluster-tuning)
- [Router service](https://druid.apache.org/docs/latest/design/router)
