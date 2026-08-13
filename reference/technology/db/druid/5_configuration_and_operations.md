# Druid 설정과 운영

## Druid 설정

> 원본: https://druid.apache.org/docs/latest/configuration/
> 원본: https://druid.apache.org/docs/latest/configuration/extensions
> 원본: https://druid.apache.org/docs/latest/operations/basic-cluster-tuning

설정 파일 구성과 공통 설정, JVM 설정, 프로세스별(Coordinator/Overlord/MiddleManager/Historical/Broker/Router) 핵심 프로퍼티, 익스텐션 로딩, 기본 클러스터 튜닝 방법을 정리.

---

### 목차

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

### 설정 파일 구성

Druid 설정은 계층적인 디렉터리 구조로 관리.

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

- `_common/common.runtime.properties`: 익스텐션, ZooKeeper, 메타데이터 스토리지, 딥 스토리지 등 모든 서비스가 공유하는 설정 위치
- 서비스별 `runtime.properties`: 각 프로세스(Broker, Coordinator 등) 고유 설정 위치
- 서비스별 `jvm.config`: 프로세스마다 힙 크기와 JVM 플래그 지정

#### 프로퍼티 값 보간(interpolation)

프로퍼티 값에 동적 참조 사용 가능.

- `${sys:java.io.tmpdir}`: Java 시스템 프로퍼티 참조
- `${env:VARIABLE_NAME}`: 환경 변수 참조
- `${file:UTF-8:/path/to/file}`: 로컬 파일 내용 참조
- `${env:VAR:-defaultValue}`: 기본값 지정

보간을 막으려면 `$$` 접두사로 이스케이프.

---

### JVM 공통 설정

모든 서비스의 `jvm.config`에 다음 네 가지 플래그 권장.

- `-Duser.timezone=UTC`: 시간대를 UTC로 통일. 다른 시간대는 테스트되지 않음
- `-Dfile.encoding=UTF-8`: 문자 인코딩을 UTF-8로 고정. 로컬 인코딩은 미지원
- `-Djava.io.tmpdir=<경로>`: Druid의 여러 구성 요소가 임시 파일을 사용하며 크기가 커질 수 있으므로 비휘발성이고 빠른 스토리지 지정 필요. NFS는 회피
- `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`: 모든 로깅을 log4j2로 통합

---

### 공통 설정

`common.runtime.properties`에 두는 설정.

#### 익스텐션 로딩

- `druid.extensions.directory`: 기본값 `extensions` — 익스텐션 루트 디렉터리
- `druid.extensions.loadList`: 기본값 `null` — 로드할 익스텐션의 JSON 배열. `null`이면 디렉터리의 모든 익스텐션 로드
- `druid.extensions.searchCurrentClassloader`: 기본값 `true` — 메인 클래스로더에서도 익스텐션 검색
- `druid.extensions.useExtensionClassloaderFirst`: 기본값 `false` — 익스텐션 JAR를 Druid 기본 JAR보다 우선 로드
- `druid.modules.excludeList`: 기본값 `[]` — 로드에서 제외할 모듈 클래스의 JSON 배열

#### ZooKeeper

- `druid.zk.service.host`: 기본값 없음(필수) — ZooKeeper 접속 문자열
- `druid.zk.paths.base`: 기본값 `/druid` — ZooKeeper 기본 경로
- `druid.zk.service.sessionTimeoutMs`: 기본값 `30000` — 세션 타임아웃(ms)
- `druid.zk.service.connectionTimeoutMs`: 기본값 `15000` — 연결 타임아웃(ms)
- `druid.zk.service.compress`: 기본값 `true` — 생성하는 znode 압축 여부
- `druid.zk.service.acl`: 기본값 `false` — ACL 보안 활성화
- `druid.discovery.curator.path`: 기본값 `/druid/discovery` — 서비스 디스커버리 경로

기본 하위 경로는 `druid.zk.paths.base` 아래에 생성됨. 예: `druid.zk.paths.announcementsPath`는 `${druid.zk.paths.base}/announcements`, `druid.zk.paths.liveSegmentsPath`는 `${druid.zk.paths.base}/segments`.

#### 메타데이터 스토리지

- `druid.metadata.storage.type`: 기본값 `derby` — 백엔드 종류(`derby`, `mysql`, `postgresql`)
- `druid.metadata.storage.connector.connectURI`: 기본값 없음 — JDBC 접속 URI
- `druid.metadata.storage.connector.user`: 기본값 없음 — DB 사용자
- `druid.metadata.storage.connector.password`: 기본값 없음 — 패스워드(Password Provider 지원)
- `druid.metadata.storage.connector.createTables`: 기본값 `true` — 테이블이 없으면 자동 생성
- `druid.metadata.storage.tables.base`: 기본값 `druid` — 테이블 이름 접두사

주요 메타데이터 테이블은 `druid_segments`(세그먼트 메타데이터) · `druid_dataSource`(데이터소스 정의) · `druid_tasks`(태스크) · `druid_audit`(설정 변경 감사 로그). Derby는 단일 노드 실험용이며, 클러스터에서는 MySQL 또는 PostgreSQL 사용.

#### 딥 스토리지

- `druid.storage.type`: 기본값 `local` — 딥 스토리지 종류(`local`, `s3`, `hdfs`, `noop` 등)
- `druid.storage.storageDirectory`: 기본값 `/tmp/druid/localStorage` — local 타입일 때 저장 디렉터리

S3 사용 시(`druid-s3-extensions` 필요):

- `druid.storage.bucket`: S3 버킷 이름
- `druid.storage.baseKey`: 객체 키 접두사
- `druid.storage.disableAcl`: ACL 비활성화(기본 `false`)

HDFS 사용 시(`druid-hdfs-storage` 필요):

- `druid.storage.storageDirectory`: HDFS 경로
- `druid.storage.compressionFormat`: `zip` 또는 `lz4`(기본 `zip`)

#### 태스크 로그

- `druid.indexer.logs.type`: 기본값 `file` — 태스크 로그 저장소(`file`, `s3`, `azure`, `google`, `hdfs`, `noop`)
- `druid.indexer.logs.directory`: 기본값 `log` — file 타입일 때 저장 경로
- `druid.indexer.logs.kill.enabled`: 기본값 `false` — 오래된 태스크 로그 자동 삭제
- `druid.indexer.logs.kill.durationToRetain`: 기본값 없음(활성화 시 필수) — 로그 보존 기간
- `druid.indexer.logs.kill.delay`: 기본값 `21600000`(6시간) — 삭제 주기(ms)

#### 요청 로깅과 감사 로깅

- `druid.request.logging.type`: 기본값 `noop` — 쿼리 요청 로거(`file`, `emitter`, `slf4j`, `filtered`, `composing`, `switching`)
- `druid.request.logging.dir`: file 타입일 때 로그 디렉터리
- `druid.request.logging.filePattern`: 기본값 `"yyyy-MM-dd'.log'"` — 파일명 패턴(Joda 형식)
- `druid.request.logging.rollPeriod`: 기본값 `P1D` — 로그 롤링 주기
- `druid.audit.manager.type`: 기본값 `sql` — 감사 로그 저장 방식(`log`, `sql`)
- `druid.audit.manager.logLevel`: 기본값 `INFO` — 감사 로그 레벨
- `druid.audit.manager.maxPayloadSizeBytes`: 기본값 `-1` — 감사 페이로드 최대 크기(-1은 무제한)

#### TLS/HTTPS

- `druid.enablePlaintextPort`: 기본값 `true` — HTTP 커넥터 활성화
- `druid.enableTlsPort`: 기본값 `false` — HTTPS 커넥터 활성화
- `druid.server.https.keyStorePath`: 기본값 없음(TLS 시 필수) — KeyStore 파일 경로
- `druid.server.https.keyStoreType`: 기본값 없음(TLS 시 필수) — KeyStore 타입
- `druid.server.https.certAlias`: 기본값 없음(TLS 시 필수) — 인증서 별칭
- `druid.server.https.keyStorePassword`: 기본값 없음(TLS 시 필수) — KeyStore 패스워드

내부 서비스 간 TLS 통신에는 `simple-client-sslcontext` 익스텐션 필요. `druid.client.https.protocol`(기본 `TLSv1.2`) · `druid.client.https.trustStorePath` · `druid.client.https.trustStorePassword` 설정.

#### 인증과 인가

- `druid.auth.authenticatorChain`: 기본값 `["allowAll"]` — 인증기(Authenticator) 체인
- `druid.auth.authorizers`: 기본값 `["allowAll"]` — 인가기(Authorizer) 목록
- `druid.escalator.type`: 기본값 `noop` — 내부 통신용 에스컬레이터 타입
- `druid.auth.unsecuredPaths`: 기본값 `[]` — 인증을 건너뛸 경로 목록
- `druid.auth.allowUnauthenticatedHttpOptions`: 기본값 `false` — 미인증 HTTP OPTIONS 허용

#### 내부 HTTP 클라이언트

- `druid.global.http.numConnections`: 기본값 `20` — 대상 URL당 커넥션 풀 크기
- `druid.global.http.eagerInitialization`: 기본값 `false` — 커넥션 사전 생성
- `druid.global.http.compressionCodec`: 기본값 `gzip` — 압축 코덱(`gzip`, `identity`)
- `druid.global.http.readTimeout`: 기본값 `PT15M` — 읽기 타임아웃
- `druid.global.http.numMaxThreads`: 기본값 `(코어 수 * 3 / 2) + 1` — 최대 I/O 스레드 수
- `druid.global.http.clientConnectTimeout`: 기본값 `500` — 연결 타임아웃(ms)

#### 기타 공통 설정

- `druid.javascript.enabled`: 기본값 `false` — JavaScript 필터·추출기·집계기 사용 허용
- `druid.indexing.doubleStorage`: 기본값 `double` — double 컬럼 저장 정밀도(`float`은 32비트)
- `druid.server.hiddenProperties`: 기본값 password/key/token 계열 — `/status/properties` 엔드포인트에서 숨길 프로퍼티
- `druid.server.http.showDetailedJettyErrors`: 기본값 `true` — 오류 응답에 Jetty 상세 정보 포함
- `druid.server.http.errorResponseTransform.strategy`: 기본값 `none` — 오류 메시지 변환 전략(`none`, `allowedRegex`)

---

### Coordinator 설정

#### 기본 서비스 설정

- `druid.host`: 기본값 호스트의 canonical hostname — 서비스 광고 주소
- `druid.plaintextPort`: 기본값 `8081` — HTTP 포트
- `druid.tlsPort`: 기본값 `8281` — HTTPS 포트
- `druid.service`: 기본값 `druid/coordinator` — 서비스 이름

#### 운영 설정

- `druid.coordinator.period`: 기본값 `PT60S` — 코디네이션 주기
- `druid.coordinator.period.indexingPeriod`: 기본값 `PT1800S` — 데이터 관리 듀티(컴팩션 등) 실행 주기
- `druid.coordinator.startDelay`: 기본값 `PT300S` — 기동 후 클러스터 상태 파악을 위한 대기 시간
- `druid.coordinator.load.timeout`: 기본값 `PT15M` — 세그먼트 할당 타임아웃
- `druid.coordinator.balancer.strategy`: 기본값 `cost` — 세그먼트 밸런싱 전략(`cost`, `diskNormalized`, `random`)
- `druid.manager.segments.pollDuration`: 기본값 `PT1M` — 세그먼트 메타데이터 폴링 주기
- `druid.manager.rules.pollDuration`: 기본값 `PT1M` — 룰 폴링 주기

#### 미사용 세그먼트 정리(kill)

- `druid.coordinator.kill.on`: 기본값 `false` — 미사용 세그먼트 자동 삭제 활성화
- `druid.coordinator.kill.period`: 기본값 `indexingPeriod`와 동일 — kill 태스크 실행 주기
- `druid.coordinator.kill.durationToRetain`: 기본값 `P90D` — 미사용 세그먼트 보존 기간
- `druid.coordinator.kill.bufferPeriod`: 기본값 `P30D` — 삭제 전 유예 기간
- `druid.coordinator.kill.maxSegments`: 기본값 `100` — kill 태스크당 삭제할 세그먼트 수

#### 동적 설정

Coordinator 동적 설정은 재시작 없이 API로 변경 가능.

- `smartSegmentLoading`: 기본값 `true` — 세그먼트 로딩 파라미터 자동 최적화
- `maxSegmentsToMove`: 기본값 `100`(smart 모드에서는 전체의 2%) — 동시에 이동할 최대 세그먼트 수
- `maxSegmentsInNodeLoadingQueue`: 기본값 `500` — Historical별 로딩 큐 최대 크기
- `replicationThrottleLimit`: 기본값 `500` — 코디네이션 1회당 최대 레플리카 할당 수
- `replicantLifetime`: 기본값 `15` — 로드 큐 대기 허용 실행 횟수
- `useRoundRobinSegmentAssignment`: 기본값 `true` — 라운드 로빈 세그먼트 할당
- `pauseCoordination`: 기본값 `false` — 코디네이션 듀티 전체 일시 중지
- `decommissioningNodes`: 기본값 없음 — 세그먼트를 비울(drain) 노드 목록

---

### Overlord 설정

#### 기본 서비스 설정

- `druid.plaintextPort`: 기본값 `8090` — HTTP 포트
- `druid.tlsPort`: 기본값 `8290` — HTTPS 포트
- `druid.service`: 기본값 `druid/overlord` — 서비스 이름

#### 태스크 실행

- `druid.indexer.runner.type`: 기본값 `httpRemote` — 태스크 실행 방식. `local`은 Overlord 내부 실행, `remote`/`httpRemote`는 MiddleManager로 분배(`httpRemote` 권장)
- `druid.indexer.storage.type`: 기본값 `local` — 태스크 상태 저장 위치(`local`, `metadata`). 클러스터에서는 `metadata` 사용
- `druid.indexer.storage.recentlyFinishedThreshold`: 기본값 `PT24H` — 완료 태스크 결과 보존 기간
- `druid.indexer.queue.maxSize`: 기본값 `Integer.MAX_VALUE` — 동시 활성 태스크 최대 수
- `druid.indexer.queue.startDelay`: 기본값 `PT1M` — 큐 기동 지연
- `druid.indexer.queue.storageSyncRate`: 기본값 `PT1M` — 상태 동기화 주기

#### 락과 세그먼트 할당

- `druid.indexer.tasklock.forceTimeChunkLock`: 기본값 `true` — 타임 청크 단위 락 강제
- `druid.indexer.tasklock.batchSegmentAllocation`: 기본값 `true` — 세그먼트 할당 요청 배치 처리
- `druid.indexer.tasklock.batchAllocationWaitTime`: 기본값 `0` — 배치 실행 전 대기 시간(ms)

#### remote/httpRemote 모드

- `druid.indexer.runner.taskAssignmentTimeout`: 기본값 `PT5M` — 태스크 할당 완료 대기 시간
- `druid.indexer.runner.minWorkerVersion`: 기본값 `"0"` — 태스크를 받을 수 있는 최소 워커 버전
- `druid.indexer.runner.maxRetriesBeforeBlacklist`: 기본값 `5` — 워커 블랙리스트 등록 전 허용 실패 횟수
- `druid.indexer.runner.workerBlackListBackoffTime`: 기본값 `PT15M` — 블랙리스트 해제 대기 시간

---

### MiddleManager와 Peon 설정

MiddleManager는 태스크를 별도 JVM(Peon)으로 포크해 실행.

- `druid.plaintextPort`: 기본값 `8091` — HTTP 포트
- `druid.service`: 기본값 `druid/middleManager` — 서비스 이름
- `druid.worker.capacity`: 기본값 `(코어 수 * 2) - 1` — 동시 실행 가능한 태스크 슬롯 수
- `druid.worker.version`: 태스크 호환성 판별용 워커 버전
- `druid.indexer.runner.javaOpts`: Peon 프로세스에 넘길 JVM 인자
- `druid.indexer.task.baseTaskDir`: 기본값 `var/druid/task` — 태스크 작업 디렉터리
- `druid.indexer.task.tmpDir`: Peon 임시 파일 디렉터리

Peon의 processing 설정은 `druid.indexer.fork.property.` 접두사로 MiddleManager `runtime.properties`에 지정. 예를 들어 `druid.indexer.fork.property.druid.processing.numThreads`는 각 태스크의 처리 스레드 수를 결정.

---

### Historical 설정

#### 스토리지와 세그먼트 캐시

- `druid.server.maxSize`: 기본값 `0` — 이 노드가 서빙할 수 있는 세그먼트 총 크기 상한
- `druid.server.tier`: 기본값 `_default_tier` — 세그먼트 분배에 사용할 티어 이름
- `druid.segmentCache.locations`: 기본값 없음(필수) — 세그먼트를 내려받아 둘 로컬 디렉터리와 크기
- `druid.segmentCache.numLoadingThreads`: 기본값 `10` — 세그먼트 병렬 로딩 스레드 수

#### 쿼리 처리와 HTTP

- `druid.server.http.numThreads`: 기본값 `max(10, (코어 수 * 2) + 1)` — HTTP 요청 처리 스레드 수
- `druid.processing.numThreads`: 기본값 `코어 수 - 1` — 쿼리 처리 스레드 수
- `druid.processing.numMergeBuffers`: 기본값 `(코어 수 / 2) - 1` — 병합 버퍼 수
- `druid.processing.buffer.sizeBytes`: 기본값 `1073741824`(1GiB) — 스레드당 처리 버퍼 크기

#### 캐시

- `druid.historical.cache.useCache`: 기본값 `true` — 세그먼트 단위 쿼리 캐시 읽기
- `druid.historical.cache.populateCache`: 기본값 `true` — 캐시 쓰기

---

### Broker 설정

#### 기본 서비스 설정

- `druid.plaintextPort`: 기본값 `8082` — HTTP 포트
- `druid.service`: 기본값 `druid/broker` — 서비스 이름

#### 쿼리 라우팅과 처리

- `druid.broker.balancer.type`: 기본값 `random` — 같은 세그먼트를 가진 서버 중 선택 전략(`random`, `connectionCount`)
- `druid.broker.select.tier`: 기본값 `_default_tier` — 세그먼트 서빙 시 우선할 티어 전략
- `druid.processing.numThreads`: 기본값 `코어 수 - 1` — 처리 스레드 수
- `druid.processing.numMergeBuffers`: 기본값 `(코어 수 / 2) - 1` — groupBy 결과 병합용 버퍼 수
- `druid.broker.http.numConnections`: Historical/태스크당 아웃바운드 커넥션 수(튜닝 절 참고)
- `druid.broker.http.maxQueuedBytes`: 채널당 읽기 대기 바이트 상한(백프레셔)

#### 캐시

- `druid.broker.cache.useCache`: 기본값 `true` — Broker 캐시 읽기
- `druid.broker.cache.populateCache`: 기본값 `true` — Broker 캐시 쓰기
- `druid.broker.cache.defaultTtl`: 기본값 `PT1H` — 캐시 엔트리 수명

---

### Router 설정

Router는 쿼리를 여러 Broker 그룹으로 분배해 중요한 데이터에 대한 쿼리가 덜 중요한 데이터 쿼리의 영향을 받지 않도록 격리하며, 웹 콘솔도 호스팅.

- `druid.router.defaultBrokerServiceName`: 예: `druid:broker-cold` — 매칭 규칙이 없을 때 사용할 기본 Broker 서비스
- `druid.router.tierToBrokerMap`: 예: `{"hot":"druid:broker-hot","_default_tier":"druid:broker-cold"}` — 데이터 티어와 Broker 서비스 매핑
- `druid.router.sql.enable`: 기본값 `false` — SQL 쿼리도 라우팅 전략으로 분배
- `druid.router.managementProxy.enabled`: 기본값 `false` — Coordinator/Overlord API 프록시(웹 콘솔에 필요)
- `druid.router.http.numConnections`: 기본값 `50` — Broker로의 커넥션 수
- `druid.router.http.readTimeout`: 기본값 `PT5M` — 읽기 타임아웃
- `druid.router.http.numMaxThreads`: 기본값 `100` — 프록시 클라이언트 최대 스레드 수
- `druid.router.avatica.balancer.type`: 기본값 `rendezvousHash` — JDBC(Avatica) 커넥션 분배 알고리즘

라우팅 전략(strategy)은 다음 네 가지.

- timeBoundary: 모든 timeBoundary 쿼리를 최우선 순위 Broker로 전송
- priority: 쿼리 컨텍스트의 priority 값으로 분배(`minPriority` 기본 0, `maxPriority` 기본 1)
- manual: 쿼리 컨텍스트의 `brokerService` 파라미터로 지정한 Broker로 전송
- JavaScript: JavaScript 함수로 라우팅 로직을 직접 작성

---

### 쿼리 처리 설정

여러 프로세스에 공통으로 적용되는 processing/groupBy 설정.

- `druid.processing.buffer.sizeBytes`: 기본값 1GiB — 스레드당 오프힙 처리 버퍼. TopN·GroupBy 중간 결과 저장
- `druid.processing.numThreads`: 기본값 `코어 수 - 1` — 쿼리 처리 스레드 수. 동시 처리 가능한 세그먼트 수 결정
- `druid.processing.numMergeBuffers`: 기본값 `(코어 수 / 2) - 1` — GroupBy 병합 버퍼 수. 동시 실행 가능한 GroupBy 쿼리 수 제한
- `druid.processing.tmpDir`: 처리 중 임시 파일 위치
- `druid.query.groupBy.maxOnDiskStorage`: 버퍼가 가득 찼을 때 디스크로 스필(spill)할 수 있는 최대 크기
- `druid.query.groupBy.maxMergingDictionarySize`: 병합 딕셔너리 최대 크기
- `druid.query.groupBy.singleThreaded`: 기본값 `false` — GroupBy 단일 스레드 실행 강제

GroupBy 쿼리는 중첩되지 않으면 쿼리당 병합 버퍼 1개 사용, 중첩되면 깊이와 무관하게 2개 사용.

---

### 익스텐션

#### 코어 익스텐션

코어 익스텐션은 Druid 커미터가 관리하며 배포판에 포함됨.

- 딥 스토리지: `druid-s3-extensions` · `druid-hdfs-storage` · `druid-azure-extensions` · `druid-google-extensions`
- 메타데이터 스토리지: `mysql-metadata-storage` · `postgresql-metadata-storage`
- 데이터 포맷: `druid-parquet-extensions` · `druid-avro-extensions` · `druid-orc-extensions` · `druid-protobuf-extensions`
- 스트리밍 인제스천: `druid-kafka-indexing-service` · `druid-kinesis-indexing-service`
- 분석: `druid-datasketches` · `druid-bloom-filter` · `druid-multi-stage-query`
- 보안: `druid-basic-security` · `druid-kerberos` · `druid-pac4j`

#### 커뮤니티 익스텐션

`druid-cassandra-storage`, `druid-redis-cache`, `druid-deltalake-extensions`, `druid-iceberg-extensions`, `prometheus-emitter`, `graphite-emitter`, `kafka-emitter` 등 존재. 커뮤니티 익스텐션은 코어 익스텐션만큼 광범위하게 테스트되지 않았을 수 있음.

#### 익스텐션 로드 방법

코어 익스텐션은 `common.runtime.properties`의 `druid.extensions.loadList`에 이름을 추가하면 됨.

```properties
druid.extensions.loadList=["postgresql-metadata-storage", "druid-hdfs-storage"]
```

커뮤니티 익스텐션은 `pull-deps` 도구로 Maven 좌표를 지정해 내려받은 뒤 `loadList`에 추가. 커뮤니티 익스텐션의 groupId는 보통 `org.apache.druid.extensions.contrib`.

```bash
java -cp "lib/*" -Ddruid.extensions.directory="extensions" \
  org.apache.druid.cli.Main tools pull-deps -c "groupId:artifactId:version"
```

---

### 기본 클러스터 튜닝

#### Historical 튜닝

힙 크기 공식:

```
힙 = (0.5GiB × CPU 코어 수) + (2 × 전체 lookup 맵 크기) + druid.cache.sizeInBytes
```

- lookup은 갱신 시 원자적 교체를 위해 기존 맵과 새 맵이 동시에 존재하므로 2배로 계산
- 힙이 약 24GiB를 넘으면 Shenandoah나 ZGC 같은 GC 검토

처리 설정 권장값:

- `druid.processing.numThreads`: `코어 수 - 1`
- `druid.processing.buffer.sizeBytes`: 500MiB
- `druid.processing.numMergeBuffers`: 처리 스레드의 1/4

다이렉트 메모리 공식:

```
다이렉트 메모리 = (numThreads + numMergeBuffers + 1) × buffer.sizeBytes
```

`+1`은 세그먼트 압축 해제 버퍼 몫.

세그먼트 캐시: `druid.segmentCache.locations` 총량이 시스템 여유 메모리(페이지 캐시로 쓸 수 있는 양)를 넘지 않게 설정하고, 스토리지는 SSD를 강력히 권장.

#### Broker 튜닝

- 힙: 소·중형 클러스터(서버 ~15대)는 4~8GiB, 대형 클러스터(~100노드)는 30~60GiB. 세그먼트 수와 전체 데이터 크기에 비례해 증가
- 다이렉트 메모리: `druid.processing.buffer.sizeBytes` 500MiB, `numMergeBuffers`는 Historical 이상으로 설정. Broker는 처리 스레드가 불필요하고 결과 병합은 힙에서 수행
- 백프레셔: `druid.broker.http.maxQueuedBytes`를 대략 `2MiB × Historical 수`로 설정
- 비율: Historical 15대당 Broker 1대를 출발점으로 하되, 고가용성을 위해 최소 2대 구성

#### 커넥션 풀 사이징

- 모든 Broker의 `druid.broker.http.numConnections` 합이 각 Historical의 `druid.server.http.numThreads`보다 약간 작아야 함
- Broker 자신의 `druid.server.http.numThreads`는 자신의 `numConnections`보다 약간 크게 설정
- 기준선: 프로세스당 동시 쿼리 50개 + 비쿼리 요청 10개. 예를 들어 Historical과 태스크에는 `druid.server.http.numThreads=60` 설정
- 예시: Broker 3대가 각각 `numConnections=10`이면 Historical 하나가 받는 쿼리 커넥션은 30개 → Historical의 `numThreads`는 40 이상 필요
- 풀이 너무 작으면 클러스터를 다 활용하지 못하고, 너무 크면 OOM과 리소스 경합 위험 발생

#### MiddleManager와 태스크 튜닝

- MiddleManager 힙: 약 128MiB면 충분(태스크를 포크만 하므로 자원 요구가 적음)
- 태스크 힙 공식: `1GiB + (2 × 전체 lookup 맵 크기)`
- 태스크 처리 설정 권장값(MiddleManager `runtime.properties`에 지정):

```properties
druid.indexer.fork.property.druid.processing.numThreads=2
druid.indexer.fork.property.druid.processing.numMergeBuffers=2
druid.indexer.fork.property.druid.processing.buffer.sizeBytes=100000000
```

- 태스크 다이렉트 메모리: `(numThreads + numMergeBuffers + 1) × buffer.sizeBytes`
- 전체 메모리: `MiddleManager 힙 + (druid.worker.capacity × 태스크 1개 메모리)`
- Kafka/Kinesis 인제스천을 쓰면 컴팩션 등 다른 태스크를 위한 여유 슬롯을 확보하고, 용량이 부족하면 MiddleManager 머신 추가

#### Coordinator, Overlord, Router

- Coordinator 힙: Broker 힙과 같거나 약간 작게. 서버 수, 세그먼트 수, 태스크 수에 비례
- Overlord 힙: Coordinator 힙의 25~50%. 실행 중인 태스크 수에 비례
- 수십만 개 이상 세그먼트가 있는 대형 클러스터에서는 Coordinator 동적 설정 `percentOfSegmentsToConsiderPerMove`를 기본 100에서 66 정도로 낮춰 코디네이션 주기 단축 가능
- Router 힙: 256MiB에서 시작. Broker로 프록시만 하므로 자원 요구가 가벼움

#### GroupBy 튜닝 지표

`GroupByStatsMonitor`(`org.apache.druid.server.metrics.GroupByStatsMonitor`)를 켜면 다음 지표로 버퍼 크기 조정 가능.

- `mergeBuffer/maxBytesUsed`: 한계에 근접하면 `buffer.sizeBytes` 증가
- `groupBy/maxSpilledBytes`: 버퍼 부족 신호 → 버퍼 크기 또는 `maxOnDiskStorage` 조정
- `groupBy/spilledQueries`: 0이 아니면 버퍼가 작다는 의미
- `mergeBuffer/pendingRequests`: 0이 아니면 병합 버퍼 고갈 → `numMergeBuffers` 증가

#### 세그먼트별 다이렉트 메모리 버퍼

- 세그먼트 압축 해제: 읽는 세그먼트의 컬럼당 64KiB 할당(`64KiB × 컬럼 수 × 세그먼트 수`)
- 인제스천 중 세그먼트 병합: String 컬럼마다 `카디널리티 × 4바이트` 버퍼를 세그먼트별로 할당. 이 할당은 `druid.processing.numMergeBuffers`와 무관

#### JVM과 OS 튜닝

GC 권장 설정:

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

시스템 권장 사항:

- Historical, MiddleManager, Indexer에는 SSD를 강력히 권장
- Historical 디스크는 RAID보다 JBOD가 처리량 면에서 유리할 수 있음
- 스왑은 사용 금지. 메모리 맵된 세그먼트 파일 때문에 성능이 예측 불가능해짐
- `/tmp`를 tmpfs에 마운트하면 GC 일시 정지 감소 가능
- GC 로그와 Druid 로그는 데이터와 다른 디스크에 배치
- Transparent Huge Pages 비활성화
- `ulimit`(열 수 있는 파일 수)을 세그먼트 파일 수보다 충분히 크게 설정(`/etc/security/limits.conf`), 메모리 맵이 많은 Historical을 위해 `/proc/sys/vm/max_map_count`도 증가(`/etc/sysctl.d/`)
- 모든 이벤트와 호스트에서 UTC 시간대 사용

---

### 참고 자료

- [Configuration reference](https://druid.apache.org/docs/latest/configuration/)
- [Extensions](https://druid.apache.org/docs/latest/configuration/extensions)
- [Basic cluster tuning](https://druid.apache.org/docs/latest/operations/basic-cluster-tuning)
- [Router service](https://druid.apache.org/docs/latest/design/router)

---

## Druid 운영

> 원본: https://druid.apache.org/docs/latest/operations/web-console
> 원본: https://druid.apache.org/docs/latest/operations/rolling-updates
> 원본: https://druid.apache.org/docs/latest/operations/high-availability
> 원본: https://druid.apache.org/docs/latest/operations/rule-configuration
> 원본: https://druid.apache.org/docs/latest/operations/metrics
> 원본: https://druid.apache.org/docs/latest/operations/alerts
> 원본: https://druid.apache.org/docs/latest/operations/clean-metadata-store

웹 콘솔, 롤링 업데이트, 고가용성 구성, retention 규칙, 메트릭과 알림, 메타데이터 스토리지 정리 등 Druid 클러스터 운영에 필요한 내용을 정리.

---

### 목차

1. [웹 콘솔](#웹-콘솔)
2. [롤링 업데이트](#롤링-업데이트)
3. [고가용성](#고가용성)
4. [Retention 규칙 설정](#retention-규칙-설정)
5. [메트릭](#메트릭)
6. [알림](#알림)
7. [메타데이터 스토리지 정리](#메타데이터-스토리지-정리)
8. [참고 자료](#참고-자료)

---

### 웹 콘솔

Druid 웹 콘솔은 데이터 관리, 클러스터 상태 모니터링, 쿼리 실행을 위한 내장 인터페이스. Router 서비스가 호스팅.

#### 접속과 사전 요구 사항

- 접속 주소: `http://<ROUTER_IP>:<ROUTER_PORT>`
- 다음 두 설정 필요(기본적으로 활성화됨)
  - Router의 management proxy 활성화
  - Broker 프로세스에서 Druid SQL 활성화
- 보안 참고: 사용자 권한을 적절히 구성해야 하며, Druid를 root 사용자로 실행하면 금지

#### 주요 뷰

- Home: Status, Datasources, Segments, Supervisors, Tasks, Services, Lookups로 이동하는 카드를 보여주는 대시보드
- Query: 멀티 탭 SQL 인터페이스. Druid 24.0부터 기본인 multi-stage query task engine 지원
- Data loader: 단계별 마법사(wizard)로 인제스천(ingestion) 스펙을 작성하며, 각 단계마다 데이터 미리보기 제공
- Datasources: 로드된 모든 데이터소스와 크기·가용성 표시. retention 규칙 편집, 자동 컴팩션 설정, 데이터 삭제, 세그먼트 타임라인 조회 지원
- Supervisors: 인덱싱 태스크 슈퍼바이저 관리. suspend, resume, reset, 스펙 제출 가능하며 상세 진행 리포트 제공
- Tasks: 실행 중·완료된 태스크 목록을 Type, Datasource, Status별로 그룹화해 표시. 태스크를 직접 제출하고 상세 정보 조회 가능
- Segments: 클러스터의 모든 세그먼트 표시. Datasource, Start, End, Version, Partition 열로 필터링·정렬 가능
- Services: 클러스터 노드 상태를 Type 또는 Tier별로 그룹화해 요약 통계와 함께 표시
- Lookups: 쿼리 타임 lookup을 생성하고 편집하는 관리 인터페이스

#### Query 뷰 세부 기능

- 스키마/데이터소스 브라우저 패널
- Run/Preview 버튼으로 쿼리 실행
- API 엔드포인트를 선택하는 engine 선택기
- 실시간 진행 상황 추적과 라이브 리포트
- 쿼리 히스토리와 태스크 모니터링
- SQL 실행 계획(EXPLAIN) 확인, 인제스천 스펙 변환 도구

---

### 롤링 업데이트

Druid 클러스터는 무중단(zero downtime)으로 롤링 업데이트 가능. 서비스는 다음 순서로 업데이트.

1. Historical
2. Middle Manager와 Indexer
3. Broker
4. Router
5. Overlord (autoscaling을 사용하면 Middle Manager보다 먼저 업데이트 가능)
6. Coordinator (또는 Coordinator+Overlord 통합 프로세스)

다운그레이드할 때는 이 순서를 반대로, Coordinator부터 시작.

#### Historical

한 번에 하나씩 업데이트. Historical 프로세스는 재시작 후 업데이트 이전에 서빙하던 모든 세그먼트를 다시 메모리 매핑해야 하므로, 하드웨어에 따라 시작에 수 초에서 수 분 소요.

#### Overlord

한 번에 하나씩 순차적으로 업데이트.

#### Middle Manager / Indexer

실시간 인덱싱 태스크가 실행 중이므로 세 가지 전략 중 하나 사용.

1. 태스크 복원(restore) 기반
   - `druid.indexer.task.restoreTasksOnRestart=true`를 설정하면 Middle Manager 재시작 시 태스크 복원됨
   - 단, 실시간(realtime) 태스크만 복원 지원, 그 외 태스크는 재제출 필요
2. 우아한 종료(graceful termination)
   - disable API로 Middle Manager가 새 태스크를 받지 않도록 만든 뒤, 실행 중인 태스크가 모두 끝나면 업데이트

```bash
# 새 태스크 수락 중지
POST http://<MM_IP>:<PORT>/druid/worker/v1/disable

# 실행 중인 태스크 확인 (빈 목록이 되면 업데이트 가능)
GET http://<MM_IP>:<PORT>/druid/worker/v1/tasks
```

재시작하면 자동으로 다시 활성화됨.

3. Autoscaling 기반 교체
   - autoscaling을 사용하는 환경에서는 다음 두 속성의 버전 값을 올리면, 새 버전의 Middle Manager가 대량으로 기동되고 기존 프로세스는 우아하게 종료됨

```properties
druid.indexer.runner.minWorkerVersion=#{VERSION}
druid.indexer.autoscale.workerVersion=#{VERSION}
```

#### Standalone Real-time

한 번에 하나씩 순차적으로 업데이트.

#### Broker

한 번에 하나씩 업데이트하되, 각 프로세스 사이에 간격 확보. Broker는 유효한 결과를 반환하려면 클러스터의 전체 상태를 먼저 로드해야 하기 때문.

#### Coordinator

한 번에 하나씩 업데이트.

---

### 고가용성

#### ZooKeeper

고가용성 ZooKeeper를 구성하려면 3개 또는 5개 노드로 이루어진 ZooKeeper 클러스터 필요. 전용 하드웨어를 사용하거나, Overlord·Coordinator를 호스팅하는 Master 서버 3~5대에서 함께 실행 가능.

#### 메타데이터 스토리지

고가용성 메타데이터 저장을 위해 복제(replication)와 페일오버(failover)를 활성화한 MySQL 또는 PostgreSQL 사용 권장.

#### Coordinator와 Overlord

Coordinator와 Overlord의 고가용성을 위해 여러 대의 서버를 실행하는 것을 권장. 동일한 ZooKeeper 클러스터와 메타데이터 스토리지를 바라보도록 구성하면 자동으로 페일오버. 한 번에 하나만 활성(active) 상태이며, 비활성 서버는 요청을 현재 활성 서버로 리다이렉트.

#### Broker

Broker는 수평 확장(scale out) 가능하고 실행 중인 모든 서버가 활성 상태로 쿼리 처리. 쿼리 분산을 위해 로드 밸런서 뒤에 배치하는 것을 권장.

---

### Retention 규칙 설정

retention 규칙은 Druid가 어떤 데이터를 유지하고 어떤 데이터를 삭제(drop)할지 결정. 규칙은 JSON 객체로 메타데이터 스토리지에 영속 저장되며, Coordinator가 각 세그먼트에 대해 규칙을 평가.

#### 규칙 평가 순서

Coordinator는 규칙 목록에 나타난 순서대로 규칙을 읽음. 각 세그먼트는 처음으로 매칭되는 규칙 하나에만 적용되므로 규칙의 순서가 매우 중요.

#### 규칙 설정 방법

웹 콘솔: Datasources > 데이터소스 선택 > Actions > Edit retention rules > +New rule > 속성 설정 > Save

Coordinator API:

```bash
# 기본 규칙 설정
POST /druid/coordinator/v1/rules/_default

# 특정 데이터소스 규칙 설정
POST /druid/coordinator/v1/rules/{datasourceName}

# 전체 규칙 조회
GET /druid/coordinator/v1/rules
```

API 요청마다 원하는 순서로 정렬한 규칙 배열 전체를 전달 필요.

#### 로드(load) 규칙

세그먼트를 Historical 티어에 할당하고 복제본 수를 지정. 모든 로드 규칙은 다음 속성을 지원.

- `tieredReplicants`: 티어 이름과 복제본 수의 매핑
- `useDefaultTierForNull`: 기본값 `true`. `tieredReplicants`를 지정하지 않으면 `{"_default_tier": 2}` 사용

Forever Load Rule (`loadForever`) — 모든 세그먼트에 적용.

```json
{
  "type": "loadForever",
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

Period Load Rule (`loadByPeriod`) — ISO 8601 기간 내의 세그먼트를 대상. `includeFuture`가 true이면 기간과 겹치거나 기간 시작 이후에 시작하는 세그먼트를 매칭.

```json
{
  "type": "loadByPeriod",
  "period": "P1M",
  "includeFuture": true,
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

Interval Load Rule (`loadByInterval`) — 고정된 날짜 구간을 대상.

```json
{
  "type": "loadByInterval",
  "interval": "2012-01-01/2013-01-01",
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

#### 드롭(drop) 규칙

세그먼트를 클러스터에서 제거할 조건을 정의. 드롭된 세그먼트도 딥 스토리지(deep storage)에는 남아 있음.

Forever Drop Rule (`dropForever`) — 모든 세그먼트를 드롭.

```json
{
  "type": "dropForever"
}
```

Period Drop Rule (`dropByPeriod`) — 기간에 매칭되는 데이터를 드롭.

```json
{
  "type": "dropByPeriod",
  "period": "P1M",
  "includeFuture": true
}
```

Period Drop Before Rule (`dropBeforeByPeriod`) — 지정한 기간 이전의 오래된 데이터를 드롭.

```json
{
  "type": "dropBeforeByPeriod",
  "period": "P1M"
}
```

Interval Drop Rule (`dropByInterval`) — 특정 구간의 데이터가 로드되지 않도록 함.

```json
{
  "type": "dropByInterval",
  "interval": "2012-01-01/2013-01-01"
}
```

#### 브로드캐스트(broadcast) 규칙

세그먼트를 모든 Broker에 로드(테스트 환경 전용). Broker에 `druid.segmentCache.locations` 설정 필요.

```json
{ "type": "broadcastForever" }
```

```json
{
  "type": "broadcastByPeriod",
  "period": "P1M",
  "includeFuture": true
}
```

```json
{
  "type": "broadcastByInterval",
  "interval": "2012-01-01/2013-01-01"
}
```

#### 영구 삭제와 재로드

- 영구 삭제: 규칙으로 클러스터에서 드롭된 세그먼트는 항상 `unused`로 표시됨. `unused` 세그먼트를 딥 스토리지에서까지 삭제하려면 kill 태스크 제출 필요
- 드롭된 데이터 재로드: 규칙 하나로는 불가능. (1) retention 기간을 늘리고, (2) API 또는 웹 콘솔에서 세그먼트를 `used`로 표시하면 Coordinator가 누락된 세그먼트를 다시 로드

---

### 메트릭

Druid는 설정 가능한 emitter로 메트릭을 내보냄. 모든 메트릭은 `timestamp`, `metric`(메트릭 이름), `service`, `host`, `version`, `buildRevision`, 숫자 `value` 필드를 공통으로 가짐. 대부분의 메트릭 값은 `druid.monitoring.emissionPeriod`로 정한 방출 주기마다 초기화됨.

#### 쿼리 메트릭 (Broker/Historical)

- `query/time`: 쿼리 완료까지 걸린 시간(ms), 정상 값 < 1s
- `query/bytes`: 클라이언트에 반환한 응답 바이트 수
- `query/success/count`: 성공한 쿼리 수 (`QueryCountStatsMonitor` 필요)
- `query/failed/count`: 실패한 쿼리 수
- `query/node/time`: 개별 historical/realtime 프로세스 쿼리 시간, 정상 값 < 1s
- `query/cpu/time`: 소비한 CPU 시간(마이크로초)

#### SQL 메트릭

- `sqlQuery/time`: SQL 쿼리 완료 시간, 정상 값 < 1s
- `sqlQuery/planningTimeMs`: SQL을 네이티브 쿼리로 변환하는 데 걸린 시간
- `sqlQuery/bytes`: SQL 쿼리 응답 바이트 수

#### 인제스천(ingestion) 메트릭

- `ingest/events/processed`: 방출 주기당 처리한 이벤트 수
- `ingest/kafka/lag`: Kafka 파티션 전체의 오프셋 지연(lag)
- `ingest/kafka/maxLag`: 파티션별 최대 지연
- `ingest/rows/published`: 성공적으로 발행(publish)된 행 수

#### Coordinator 메트릭

- `segment/assigned/count`: 로드하도록 할당된 세그먼트 수
- `segment/moved/count`: 재분배(rebalance)로 이동한 세그먼트 수
- `segment/dropped/count`: 과잉 복제로 드롭된 세그먼트 수
- `segment/unavailable/count`: 로드를 기다리는 세그먼트 수, 정상 값 0

#### JVM 및 상태(health) 메트릭

- `jvm/mem/used`: 현재 힙 사용량
- `jvm/mem/max`: 사용 가능한 최대 메모리
- `jvm/gc/count`: GC 발생 횟수
- `jvm/gc/cpu`: GC에 소비한 시간(나노초). 전체 CPU의 10~30% 수준이어야 함
- `service/heartbeat`: 서비스 동작 지표. 값 1 (`ServiceStatusMonitor` 필요)

#### 시스템 메트릭 (OshiSysMonitor 권장)

- `sys/mem/used`: 시스템 메모리 사용량
- `sys/cpu`: 프로세스별 CPU 사용률
- `sys/disk/read/size`: 디스크 읽기 바이트 수
- `cgroup/memory/usage/bytes`: 컨테이너 메모리 사용량 (cgroup 환경)

각 메트릭에는 필터링과 집계를 위한 디멘션(dimension)이 함께 붙음(`dataSource`, `taskId`, `server`, `tier` 등).

---

### 알림

Druid는 예상하지 못한 상황을 만나면 알림(alert)을 생성. 알림은 JSON 객체로 런타임 로그 파일에 기록하거나 HTTP로 Apache Kafka 같은 외부 서비스에 내보낼 수 있음. 알림 방출은 기본적으로 비활성화되어 있으므로, 사용하려면 emitter 설정에서 명시적으로 활성화 필요.

#### 공통 알림 필드

- `timestamp`: 알림이 생성된 시각
- `service`: 알림을 발생시킨 서비스 이름
- `host`: 알림을 발생시킨 호스트 이름
- `severity`: 심각도. 예: `anomaly`, `component-failure`, `service-failure`
- `description`: 알림에 대한 맥락 정보
- `data`: 예외의 경우 `exceptionType`, `exceptionMessage`, `exceptionStackTrace`를 담은 JSON 객체

알림은 요청 로깅(request logging), 메트릭 수집과 함께 Druid 운영 모니터링을 구성하는 요소 중 하나.

---

### 메타데이터 스토리지 정리

데이터소스와 개체를 자주 생성·삭제하는 환경에서는 메타데이터 스토리지에 오래된 레코드가 쌓여 성능이 저하될 수 있음. Druid는 메타데이터 레코드를 자동으로 정리하는 기능을 제공. 기본 retention 기간은 90일이며, 컴팩션 설정과 인덱서 태스크 로그 정리는 기본적으로 비활성화됨.

#### 정리 대상 메타데이터 유형

1. 세그먼트 레코드와 딥 스토리지의 세그먼트 — kill 태스크 설정 필요
2. 감사(audit) 레코드 — retention 기간이 지나면 전부 정리 대상
3. 슈퍼바이저 레코드 — 슈퍼바이저 종료 후 retention 기간이 지나면 대상
4. 규칙(rule) 레코드 — kill 태스크 필요, 모든 세그먼트가 kill된 후 대상
5. 컴팩션 설정 레코드 — 세그먼트가 없는 비활성 데이터소스 대상
6. 데이터소스 레코드 — 슈퍼바이저가 생성한 레코드, 슈퍼바이저 종료 후 대상
7. 인덱싱 상태 레코드 — 미사용이거나 대기(pending) 상태로 retention 기간이 지난 경우
8. 인덱서 태스크 로그 — 딥 스토리지와 메타데이터에서 함께 제거

#### Coordinator 설정 속성

- `druid.coordinator.period.metadataStoreManagementPeriod`: 기본값 없음 — 메타데이터 관리 작업 실행 주기
- `druid.coordinator.kill.on`: 기본값 true — 세그먼트 레코드 정리 활성화
- `druid.coordinator.kill.period`: 기본값 P1D — kill 태스크 실행 주기 (ISO 8601)
- `druid.coordinator.kill.durationToRetain`: 기본값 P90D — 삭제 전 보존 기간
- `druid.coordinator.kill.bufferPeriod`: 기본값 없음 — 정리 전 버퍼 기간
- `druid.coordinator.kill.maxSegments`: 기본값 없음 — 태스크당 최대 삭제 세그먼트 수
- `druid.coordinator.kill.audit.on`: 기본값 false — 감사 레코드 정리 활성화
- `druid.coordinator.kill.audit.period`: 기본값 P1D — 감사 레코드 정리 주기
- `druid.coordinator.kill.audit.durationToRetain`: 기본값 P90D — 감사 레코드 보존 기간
- `druid.coordinator.kill.supervisor.on`: 기본값 false — 슈퍼바이저 레코드 정리 활성화
- `druid.coordinator.kill.supervisor.period`: 기본값 P1D — 슈퍼바이저 레코드 정리 주기
- `druid.coordinator.kill.supervisor.durationToRetain`: 기본값 P90D — 슈퍼바이저 레코드 보존 기간
- `druid.coordinator.kill.rule.on`: 기본값 false — 규칙 레코드 정리 활성화
- `druid.coordinator.kill.rule.period`: 기본값 P1D — 규칙 레코드 정리 주기
- `druid.coordinator.kill.rule.durationToRetain`: 기본값 P90D — 규칙 레코드 보존 기간
- `druid.coordinator.kill.compaction.on`: 기본값 false — 컴팩션 설정 정리 활성화
- `druid.coordinator.kill.compaction.period`: 기본값 P1D — 컴팩션 설정 정리 주기
- `druid.coordinator.kill.datasource.on`: 기본값 false — 데이터소스 레코드 정리 활성화
- `druid.coordinator.kill.datasource.period`: 기본값 P1D — 데이터소스 레코드 정리 주기
- `druid.coordinator.kill.datasource.durationToRetain`: 기본값 P90D — 데이터소스 레코드 보존 기간

#### Overlord 설정 속성

- `druid.overlord.kill.indexingStates.on`: 기본값 false — 인덱싱 상태 레코드 정리 활성화
- `druid.overlord.kill.indexingStates.period`: 기본값 P1D — 인덱싱 상태 정리 주기
- `druid.overlord.kill.indexingStates.durationToRetain`: 기본값 P7D — 비활성 상태 보존 기간
- `druid.overlord.kill.indexingStates.pendingDurationToRetain`: 기본값 P7D — 대기 상태 보존 기간
- `druid.indexer.logs.kill.enabled`: 기본값 false — 태스크 로그 정리 활성화
- `druid.indexer.logs.kill.durationToRetain`: 기본값 없음 — 태스크 로그 보존 기간 (밀리초)
- `druid.indexer.logs.kill.initialDelay`: 기본값 없음 — 첫 정리까지의 초기 지연 (밀리초)
- `druid.indexer.logs.kill.delay`: 기본값 없음 — 정리 작업 간 지연 (밀리초)

#### 사전 요구 사항과 주의점

- 규칙 레코드와 컴팩션 설정 정리에는 kill 태스크 활성화(`druid.coordinator.kill.on=true`)가 선행되어야 함
- 메타데이터 관리 주기(`metadataStoreManagementPeriod`)는 개별 정리 작업 주기와 같거나 더 짧아야 함
- kill 태스크는 dynamic configuration의 `killDataSourceWhitelist`를 따름
- kill 태스크는 메타데이터뿐 아니라 딥 스토리지의 실제 데이터까지 삭제하는 유일한 정리 작업
- 데이터소스가 존재하기 전에 만든 컴팩션 설정은 조기에 삭제될 수 있음
- 감사(audit) 규정 준수가 필요하면 정리를 활성화하기 전에 감사 레코드를 미리 내보내야 함
- 컴팩션 설정이 크면 감사 로그 크기 제한을 초과할 수 있으므로 `druid.audit.manager.maxPayloadSizeBytes` 조정 필요
- 인덱싱 상태 정리는 기본 비활성화이며, 자동 컴팩션 슈퍼바이저를 사용할 때만 해당 레코드가 생성됨
- 정리를 끄려면 `druid.coordinator.kill.on=false`와 함께 각 개체별 정리 플래그를 `false`로 설정

---

### 참고 자료

- [Web console](https://druid.apache.org/docs/latest/operations/web-console)
- [Rolling updates](https://druid.apache.org/docs/latest/operations/rolling-updates)
- [High availability](https://druid.apache.org/docs/latest/operations/high-availability)
- [Using rules to drop and retain data](https://druid.apache.org/docs/latest/operations/rule-configuration)
- [Metrics](https://druid.apache.org/docs/latest/operations/metrics)
- [Alerts](https://druid.apache.org/docs/latest/operations/alerts)
- [Automated cleanup for metadata records](https://druid.apache.org/docs/latest/operations/clean-metadata-store)
