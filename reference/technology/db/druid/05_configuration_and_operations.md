# Druid 설정과 운영

## Druid 설정

> 원본: https://druid.apache.org/docs/latest/configuration/

> 원본: https://druid.apache.org/docs/latest/configuration/extensions

> 원본: https://druid.apache.org/docs/latest/operations/basic-cluster-tuning

- 설정 파일 구성과 공통 설정, JVM 설정, 프로세스별(Coordinator/Overlord/MiddleManager/Historical/Broker/Router) 핵심 프로퍼티, 익스텐션 로딩, 기본 클러스터 튜닝 방법을 정리

<a id="설정-파일-구성"></a>
### 설정 파일 구성

Druid는 모든 서비스가 공유하는 설정과 서비스별 설정을 디렉터리로 나누어 관리한다. 공통 설정은 `_common`에 두고, 각 서비스 디렉터리에는 고유한 런타임 설정과 JVM 설정을 둔다.

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

- 프로퍼티 값에 동적 참조 사용 가능

- `${sys:java.io.tmpdir}`: Java 시스템 프로퍼티 참조
- `${env:VARIABLE_NAME}`: 환경 변수 참조
- `${file:UTF-8:/path/to/file}`: 로컬 파일 내용 참조
- `${env:VAR:-defaultValue}`: 기본값 지정

- 보간을 막으려면 `$$` 접두사로 이스케이프

<a id="jvm-공통-설정"></a>

### JVM 공통 설정

- 모든 서비스의 `jvm.config`에 다음 네 가지 플래그 권장

- `-Duser.timezone=UTC`: 시간대를 UTC로 통일
  - 다른 시간대는 테스트되지 않음
- `-Dfile.encoding=UTF-8`: 문자 인코딩을 UTF-8로 고정
  - 로컬 인코딩은 미지원
- `-Djava.io.tmpdir=<경로>`: Druid의 여러 구성 요소가 임시 파일을 사용하며 크기가 커질 수 있으므로 비휘발성이고 빠른 스토리지 지정 필요
  - NFS는 회피
- `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`: 모든 로깅을 log4j2로 통합

<a id="공통-설정"></a>

### 공통 설정

- `common.runtime.properties`에 두는 설정

#### 익스텐션 로딩

- `druid.extensions.directory`: 익스텐션 루트 디렉터리 (기본값 `extensions`)
- `druid.extensions.loadList`: 로드할 익스텐션의 JSON 배열 (기본값 `null`)
  - `null`이면 디렉터리의 모든 익스텐션 로드
- `druid.extensions.searchCurrentClassloader`: 메인 클래스로더에서도 익스텐션 검색 (기본값 `true`)
- `druid.extensions.useExtensionClassloaderFirst`: 익스텐션 JAR를 Druid 기본 JAR보다 우선 로드 (기본값 `false`)
- `druid.modules.excludeList`: 로드에서 제외할 모듈 클래스의 JSON 배열 (기본값 `[]`)

#### ZooKeeper

- `druid.zk.service.host`: ZooKeeper 접속 문자열 (기본값 없음(필수))
- `druid.zk.paths.base`: ZooKeeper 기본 경로 (기본값 `/druid`)
- `druid.zk.service.sessionTimeoutMs`: 세션 타임아웃(ms) (기본값 `30000`)
- `druid.zk.service.connectionTimeoutMs`: 연결 타임아웃(ms) (기본값 `15000`)
- `druid.zk.service.compress`: 생성하는 znode 압축 여부 (기본값 `true`)
- `druid.zk.service.acl`: ACL 보안 활성화 (기본값 `false`)
- `druid.discovery.curator.path`: 서비스 디스커버리 경로 (기본값 `/druid/discovery`)

- 기본 하위 경로는 `druid.zk.paths.base` 아래에 생성됨
  - 예: `druid.zk.paths.announcementsPath`는 `${druid.zk.paths.base}/announcements`, `druid.zk.paths.liveSegmentsPath`는 `${druid.zk.paths.base}/segments`.

#### 메타데이터 스토리지

- `druid.metadata.storage.type`: 백엔드 종류(`derby`, `mysql`, `postgresql`) (기본값 `derby`)
- `druid.metadata.storage.connector.connectURI`: JDBC 접속 URI (기본값 없음)
- `druid.metadata.storage.connector.user`: DB 사용자 (기본값 없음)
- `druid.metadata.storage.connector.password`: 패스워드(Password Provider 지원) (기본값 없음)
- `druid.metadata.storage.connector.createTables`: 테이블이 없으면 자동 생성 (기본값 `true`)
- `druid.metadata.storage.tables.base`: 테이블 이름 접두사 (기본값 `druid`)

- 주요 메타데이터 테이블은 `druid_segments`(세그먼트 메타데이터), `druid_dataSource`(데이터소스 정의), `druid_tasks`(태스크), `druid_audit`(설정 변경 감사 로그). Derby는 단일 노드 실험용이며, 클러스터에서는 MySQL 또는 PostgreSQL 사용

#### 딥 스토리지

- `druid.storage.type`: 딥 스토리지 종류(`local`, `s3`, `hdfs`, `noop` 등) (기본값 `local`)
- `druid.storage.storageDirectory`: local 타입일 때 저장 디렉터리 (기본값 `/tmp/druid/localStorage`)

- S3 사용 시(`druid-s3-extensions` 필요):

- `druid.storage.bucket`: S3 버킷 이름
- `druid.storage.baseKey`: 객체 키 접두사
- `druid.storage.disableAcl`: ACL 비활성화(기본 `false`)

- HDFS 사용 시(`druid-hdfs-storage` 필요):

- `druid.storage.storageDirectory`: HDFS 경로
- `druid.storage.compressionFormat`: `zip` 또는 `lz4`(기본 `zip`)

#### 태스크 로그

- `druid.indexer.logs.type`: 태스크 로그 저장소(`file`, `s3`, `azure`, `google`, `hdfs`, `noop`) (기본값 `file`)
- `druid.indexer.logs.directory`: file 타입일 때 저장 경로 (기본값 `log`)
- `druid.indexer.logs.kill.enabled`: 오래된 태스크 로그 자동 삭제 (기본값 `false`)
- `druid.indexer.logs.kill.durationToRetain`: 로그 보존 기간 (기본값 없음(활성화 시 필수))
- `druid.indexer.logs.kill.delay`: 삭제 주기(ms) (기본값 `21600000`(6시간))

#### 요청 로깅과 감사 로깅

- `druid.request.logging.type`: 쿼리 요청 로거(`file`, `emitter`, `slf4j`, `filtered`, `composing`, `switching`) (기본값 `noop`)
- `druid.request.logging.dir`: file 타입일 때 로그 디렉터리
- `druid.request.logging.filePattern`: 파일명 패턴(Joda 형식) (기본값 `"yyyy-MM-dd'.log'"`)
- `druid.request.logging.rollPeriod`: 로그 롤링 주기 (기본값 `P1D`)
- `druid.audit.manager.type`: 감사 로그 저장 방식(`log`, `sql`) (기본값 `sql`)
- `druid.audit.manager.logLevel`: 감사 로그 레벨 (기본값 `INFO`)
- `druid.audit.manager.maxPayloadSizeBytes`: 감사 페이로드 최대 크기(-1은 무제한) (기본값 `-1`)

#### TLS/HTTPS

- `druid.enablePlaintextPort`: HTTP 커넥터 활성화 (기본값 `true`)
- `druid.enableTlsPort`: HTTPS 커넥터 활성화 (기본값 `false`)
- `druid.server.https.keyStorePath`: KeyStore 파일 경로 (기본값 없음(TLS 시 필수))
- `druid.server.https.keyStoreType`: KeyStore 타입 (기본값 없음(TLS 시 필수))
- `druid.server.https.certAlias`: 인증서 별칭 (기본값 없음(TLS 시 필수))
- `druid.server.https.keyStorePassword`: KeyStore 패스워드 (기본값 없음(TLS 시 필수))

- 내부 서비스 간 TLS 통신에는 `simple-client-sslcontext` 익스텐션 필요
  - `druid.client.https.protocol`(기본 `TLSv1.2`), `druid.client.https.trustStorePath`, `druid.client.https.trustStorePassword` 설정

#### 인증과 인가

- `druid.auth.authenticatorChain`: 인증기(Authenticator) 체인 (기본값 `["allowAll"]`)
- `druid.auth.authorizers`: 인가기(Authorizer) 목록 (기본값 `["allowAll"]`)
- `druid.escalator.type`: 내부 통신용 에스컬레이터 타입 (기본값 `noop`)
- `druid.auth.unsecuredPaths`: 인증을 건너뛸 경로 목록 (기본값 `[]`)
- `druid.auth.allowUnauthenticatedHttpOptions`: 미인증 HTTP OPTIONS 허용 (기본값 `false`)

#### 내부 HTTP 클라이언트

- `druid.global.http.numConnections`: 대상 URL당 커넥션 풀 크기 (기본값 `20`)
- `druid.global.http.eagerInitialization`: 커넥션 사전 생성 (기본값 `false`)
- `druid.global.http.compressionCodec`: 압축 코덱(`gzip`, `identity`) (기본값 `gzip`)
- `druid.global.http.readTimeout`: 읽기 타임아웃 (기본값 `PT15M`)
- `druid.global.http.numMaxThreads`: 최대 I/O 스레드 수 (기본값 `(코어 수 * 3 / 2) + 1`)
- `druid.global.http.clientConnectTimeout`: 연결 타임아웃(ms) (기본값 `500`)

#### 기타 공통 설정

- `druid.javascript.enabled`: JavaScript 필터, 추출기, 집계기 사용 허용 (기본값 `false`)
- `druid.indexing.doubleStorage`: double 컬럼 저장 정밀도(`float`은 32비트) (기본값 `double`)
- `druid.server.hiddenProperties`: `/status/properties` 엔드포인트에서 숨길 프로퍼티 (기본값 password/key/token 계열)
- `druid.server.http.showDetailedJettyErrors`: 오류 응답에 Jetty 상세 정보 포함 (기본값 `true`)
- `druid.server.http.errorResponseTransform.strategy`: 오류 메시지 변환 전략(`none`, `allowedRegex`) (기본값 `none`)

<a id="coordinator-설정"></a>

### Coordinator 설정

#### 기본 서비스 설정

- `druid.host`: 서비스 광고 주소 (기본값 호스트의 canonical hostname)
- `druid.plaintextPort`: HTTP 포트 (기본값 `8081`)
- `druid.tlsPort`: HTTPS 포트 (기본값 `8281`)
- `druid.service`: 서비스 이름 (기본값 `druid/coordinator`)

#### 운영 설정

- `druid.coordinator.period`: 코디네이션 주기 (기본값 `PT60S`)
- `druid.coordinator.period.indexingPeriod`: 데이터 관리 듀티(컴팩션 등) 실행 주기 (기본값 `PT1800S`)
- `druid.coordinator.startDelay`: 기동 후 클러스터 상태 파악을 위한 대기 시간 (기본값 `PT300S`)
- `druid.coordinator.load.timeout`: 세그먼트 할당 타임아웃 (기본값 `PT15M`)
- `druid.coordinator.balancer.strategy`: 세그먼트 밸런싱 전략(`cost`, `diskNormalized`, `random`) (기본값 `cost`)
- `druid.manager.segments.pollDuration`: 세그먼트 메타데이터 폴링 주기 (기본값 `PT1M`)
- `druid.manager.rules.pollDuration`: 룰 폴링 주기 (기본값 `PT1M`)

#### 미사용 세그먼트 정리(kill)

- `druid.coordinator.kill.on`: 미사용 세그먼트 자동 삭제 활성화 (기본값 `false`)
- `druid.coordinator.kill.period`: kill 태스크 실행 주기 (기본값 `indexingPeriod`와 동일)
- `druid.coordinator.kill.durationToRetain`: 미사용 세그먼트 보존 기간 (기본값 `P90D`)
- `druid.coordinator.kill.bufferPeriod`: 삭제 전 유예 기간 (기본값 `P30D`)
- `druid.coordinator.kill.maxSegments`: kill 태스크당 삭제할 세그먼트 수 (기본값 `100`)

#### 동적 설정

- Coordinator 동적 설정은 재시작 없이 API로 변경 가능

- `smartSegmentLoading`: 세그먼트 로딩 파라미터 자동 최적화 (기본값 `true`)
- `maxSegmentsToMove`: 동시에 이동할 최대 세그먼트 수 (기본값 `100`(smart 모드에서는 전체의 2%))
- `maxSegmentsInNodeLoadingQueue`: Historical별 로딩 큐 최대 크기 (기본값 `500`)
- `replicationThrottleLimit`: 코디네이션 1회당 최대 레플리카 할당 수 (기본값 `500`)
- `replicantLifetime`: 로드 큐 대기 허용 실행 횟수 (기본값 `15`)
- `useRoundRobinSegmentAssignment`: 라운드 로빈 세그먼트 할당 (기본값 `true`)
- `pauseCoordination`: 코디네이션 듀티 전체 일시 중지 (기본값 `false`)
- `decommissioningNodes`: 세그먼트를 비울(drain) 노드 목록 (기본값 없음)

<a id="overlord-설정"></a>

### Overlord 설정

#### 기본 서비스 설정

- `druid.plaintextPort`: HTTP 포트 (기본값 `8090`)
- `druid.tlsPort`: HTTPS 포트 (기본값 `8290`)
- `druid.service`: 서비스 이름 (기본값 `druid/overlord`)

#### 태스크 실행

- `druid.indexer.runner.type`: 태스크 실행 방식 (기본값 `httpRemote`)
  - `local`은 Overlord 내부 실행, `remote`/`httpRemote`는 MiddleManager로 분배(`httpRemote` 권장)
- `druid.indexer.storage.type`: 태스크 상태 저장 위치(`local`, `metadata`). 클러스터에서는 `metadata` 사용 (기본값 `local`)
- `druid.indexer.storage.recentlyFinishedThreshold`: 완료 태스크 결과 보존 기간 (기본값 `PT24H`)
- `druid.indexer.queue.maxSize`: 동시 활성 태스크 최대 수 (기본값 `Integer.MAX_VALUE`)
- `druid.indexer.queue.startDelay`: 큐 기동 지연 (기본값 `PT1M`)
- `druid.indexer.queue.storageSyncRate`: 상태 동기화 주기 (기본값 `PT1M`)

#### 락과 세그먼트 할당

- `druid.indexer.tasklock.forceTimeChunkLock`: 타임 청크 단위 락 강제 (기본값 `true`)
- `druid.indexer.tasklock.batchSegmentAllocation`: 세그먼트 할당 요청 배치 처리 (기본값 `true`)
- `druid.indexer.tasklock.batchAllocationWaitTime`: 배치 실행 전 대기 시간(ms) (기본값 `0`)

#### remote/httpRemote 모드

- `druid.indexer.runner.taskAssignmentTimeout`: 태스크 할당 완료 대기 시간 (기본값 `PT5M`)
- `druid.indexer.runner.minWorkerVersion`: 태스크를 받을 수 있는 최소 워커 버전 (기본값 `"0"`)
- `druid.indexer.runner.maxRetriesBeforeBlacklist`: 워커 블랙리스트 등록 전 허용 실패 횟수 (기본값 `5`)
- `druid.indexer.runner.workerBlackListBackoffTime`: 블랙리스트 해제 대기 시간 (기본값 `PT15M`)

<a id="middlemanager와-peon-설정"></a>

### MiddleManager와 Peon 설정

- MiddleManager는 태스크를 별도 JVM(Peon)으로 포크해 실행

- `druid.plaintextPort`: HTTP 포트 (기본값 `8091`)
- `druid.service`: 서비스 이름 (기본값 `druid/middleManager`)
- `druid.worker.capacity`: 동시 실행 가능한 태스크 슬롯 수 (기본값 `(코어 수 * 2) - 1`)
- `druid.worker.version`: 태스크 호환성 판별용 워커 버전
- `druid.indexer.runner.javaOpts`: Peon 프로세스에 넘길 JVM 인자
- `druid.indexer.task.baseTaskDir`: 태스크 작업 디렉터리 (기본값 `var/druid/task`)
- `druid.indexer.task.tmpDir`: Peon 임시 파일 디렉터리

- Peon의 processing 설정은 `druid.indexer.fork.property.` 접두사로 MiddleManager `runtime.properties`에 지정
  - 예를 들어 `druid.indexer.fork.property.druid.processing.numThreads`는 각 태스크의 처리 스레드 수를 결정

<a id="historical-설정"></a>

### Historical 설정

#### 스토리지와 세그먼트 캐시

- `druid.server.maxSize`: 이 노드가 서빙할 수 있는 세그먼트 총 크기 상한 (기본값 `0`)
- `druid.server.tier`: 세그먼트 분배에 사용할 티어 이름 (기본값 `_default_tier`)
- `druid.segmentCache.locations`: 세그먼트를 내려받아 둘 로컬 디렉터리와 크기 (기본값 없음(필수))
- `druid.segmentCache.numLoadingThreads`: 세그먼트 병렬 로딩 스레드 수 (기본값 `10`)

#### 쿼리 처리와 HTTP

- `druid.server.http.numThreads`: HTTP 요청 처리 스레드 수 (기본값 `max(10, (코어 수 * 2) + 1)`)
- `druid.processing.numThreads`: 쿼리 처리 스레드 수 (기본값 `코어 수 - 1`)
- `druid.processing.numMergeBuffers`: 병합 버퍼 수 (기본값 `(코어 수 / 2) - 1`)
- `druid.processing.buffer.sizeBytes`: 스레드당 처리 버퍼 크기 (기본값 `1073741824`(1GiB))

#### 캐시

- `druid.historical.cache.useCache`: 세그먼트 단위 쿼리 캐시 읽기 (기본값 `true`)
- `druid.historical.cache.populateCache`: 캐시 쓰기 (기본값 `true`)

<a id="broker-설정"></a>

### Broker 설정

#### 기본 서비스 설정

- `druid.plaintextPort`: HTTP 포트 (기본값 `8082`)
- `druid.service`: 서비스 이름 (기본값 `druid/broker`)

#### 쿼리 라우팅과 처리

- `druid.broker.balancer.type`: 같은 세그먼트를 가진 서버 중 선택 전략(`random`, `connectionCount`) (기본값 `random`)
- `druid.broker.select.tier`: 세그먼트 서빙 시 우선할 티어 전략 (기본값 `_default_tier`)
- `druid.processing.numThreads`: 처리 스레드 수 (기본값 `코어 수 - 1`)
- `druid.processing.numMergeBuffers`: groupBy 결과 병합용 버퍼 수 (기본값 `(코어 수 / 2) - 1`)
- `druid.broker.http.numConnections`: Historical/태스크당 아웃바운드 커넥션 수(튜닝 절 참고)
- `druid.broker.http.maxQueuedBytes`: 채널당 읽기 대기 바이트 상한(백프레셔)

#### 캐시

- `druid.broker.cache.useCache`: Broker 캐시 읽기 (기본값 `true`)
- `druid.broker.cache.populateCache`: Broker 캐시 쓰기 (기본값 `true`)
- `druid.broker.cache.defaultTtl`: 캐시 엔트리 수명 (기본값 `PT1H`)

<a id="router-설정"></a>

### Router 설정

- Router는 쿼리를 여러 Broker 그룹으로 분배해 중요한 데이터에 대한 쿼리가 덜 중요한 데이터 쿼리의 영향을 받지 않도록 격리하며, 웹 콘솔도 호스팅

- `druid.router.defaultBrokerServiceName`: 예: `druid:broker-cold`: 매칭 규칙이 없을 때 사용할 기본 Broker 서비스
- `druid.router.tierToBrokerMap`: 예: `{"hot":"druid:broker-hot","_default_tier":"druid:broker-cold"}`: 데이터 티어와 Broker 서비스 매핑
- `druid.router.sql.enable`: SQL 쿼리도 라우팅 전략으로 분배 (기본값 `false`)
- `druid.router.managementProxy.enabled`: Coordinator/Overlord API 프록시(웹 콘솔에 필요) (기본값 `false`)
- `druid.router.http.numConnections`: Broker로의 커넥션 수 (기본값 `50`)
- `druid.router.http.readTimeout`: 읽기 타임아웃 (기본값 `PT5M`)
- `druid.router.http.numMaxThreads`: 프록시 클라이언트 최대 스레드 수 (기본값 `100`)
- `druid.router.avatica.balancer.type`: JDBC(Avatica) 커넥션 분배 알고리즘 (기본값 `rendezvousHash`)

- 라우팅 전략(strategy)은 다음 네 가지

- timeBoundary: 모든 timeBoundary 쿼리를 최우선 순위 Broker로 전송
- priority: 쿼리 컨텍스트의 priority 값으로 분배(`minPriority` 기본 0, `maxPriority` 기본 1)
- manual: 쿼리 컨텍스트의 `brokerService` 파라미터로 지정한 Broker로 전송
- JavaScript: JavaScript 함수로 라우팅 로직을 직접 작성

<a id="쿼리-처리-설정"></a>

### 쿼리 처리 설정

- 여러 프로세스에 공통으로 적용되는 processing/groupBy 설정

- `druid.processing.buffer.sizeBytes`: 스레드당 오프힙 처리 버퍼 (기본값 1GiB)
  - TopN, GroupBy 중간 결과 저장
- `druid.processing.numThreads`: 쿼리 처리 스레드 수 (기본값 `코어 수 - 1`)
  - 동시 처리 가능한 세그먼트 수 결정
- `druid.processing.numMergeBuffers`: GroupBy 병합 버퍼 수 (기본값 `(코어 수 / 2) - 1`)
  - 동시 실행 가능한 GroupBy 쿼리 수 제한
- `druid.processing.tmpDir`: 처리 중 임시 파일 위치
- `druid.query.groupBy.maxOnDiskStorage`: 버퍼가 가득 찼을 때 디스크로 스필(spill)할 수 있는 최대 크기
- `druid.query.groupBy.maxMergingDictionarySize`: 병합 딕셔너리 최대 크기
- `druid.query.groupBy.singleThreaded`: GroupBy 단일 스레드 실행 강제 (기본값 `false`)

- GroupBy 쿼리는 중첩되지 않으면 쿼리당 병합 버퍼 1개 사용, 중첩되면 깊이와 무관하게 2개 사용

<a id="익스텐션"></a>

### 익스텐션

#### 코어 익스텐션

- 코어 익스텐션은 Druid 커미터가 관리하며 배포판에 포함됨

- 딥 스토리지: `druid-s3-extensions`, `druid-hdfs-storage`, `druid-azure-extensions`, `druid-google-extensions`
- 메타데이터 스토리지: `mysql-metadata-storage`, `postgresql-metadata-storage`
- 데이터 포맷: `druid-parquet-extensions`, `druid-avro-extensions`, `druid-orc-extensions`, `druid-protobuf-extensions`
- 스트리밍 인제스천: `druid-kafka-indexing-service`, `druid-kinesis-indexing-service`
- 분석: `druid-datasketches`, `druid-bloom-filter`, `druid-multi-stage-query`
- 보안: `druid-basic-security`, `druid-kerberos`, `druid-pac4j`

#### 커뮤니티 익스텐션

- `druid-cassandra-storage`, `druid-redis-cache`, `druid-deltalake-extensions`, `druid-iceberg-extensions`, `prometheus-emitter`, `graphite-emitter`, `kafka-emitter` 등 존재
  - 커뮤니티 익스텐션은 코어 익스텐션만큼 광범위하게 테스트되지 않았을 수 있음

#### 익스텐션 로드 방법

- 코어 익스텐션은 `common.runtime.properties`의 `druid.extensions.loadList`에 이름을 추가하면 됨

```properties
druid.extensions.loadList=["postgresql-metadata-storage", "druid-hdfs-storage"]
```

- 커뮤니티 익스텐션은 `pull-deps` 도구로 Maven 좌표를 지정해 내려받은 뒤 `loadList`에 추가
  - 커뮤니티 익스텐션의 groupId는 보통 `org.apache.druid.extensions.contrib`.

```bash
java -cp "lib/*" -Ddruid.extensions.directory="extensions" \
  org.apache.druid.cli.Main tools pull-deps -c "groupId:artifactId:version"
```

<a id="기본-클러스터-튜닝"></a>

### 기본 클러스터 튜닝

#### Historical 튜닝

- 힙 크기 공식:

```
힙 = (0.5GiB × CPU 코어 수) + (2 × 전체 lookup 맵 크기) + druid.cache.sizeInBytes
```

- lookup은 갱신 시 원자적 교체를 위해 기존 맵과 새 맵이 동시에 존재하므로 2배로 계산
- 힙이 약 24GiB를 넘으면 Shenandoah나 ZGC 같은 GC 검토

- 처리 설정 권장값:

- `druid.processing.numThreads`: `코어 수 - 1`
- `druid.processing.buffer.sizeBytes`: 500MiB
- `druid.processing.numMergeBuffers`: 처리 스레드의 1/4

- 다이렉트 메모리 공식:

```
다이렉트 메모리 = (numThreads + numMergeBuffers + 1) × buffer.sizeBytes
```

- `+1`은 세그먼트 압축 해제 버퍼 몫

- 세그먼트 캐시: `druid.segmentCache.locations` 총량이 시스템 여유 메모리(페이지 캐시로 쓸 수 있는 양)를 넘지 않게 설정하고, 스토리지는 SSD를 강력히 권장

#### Broker 튜닝

- 힙: 소, 중형 클러스터(서버 ~15대)는 4~8GiB, 대형 클러스터(~100노드)는 30~60GiB. 세그먼트 수와 전체 데이터 크기에 비례해 증가
- 다이렉트 메모리: `druid.processing.buffer.sizeBytes` 500MiB, `numMergeBuffers`는 Historical 이상으로 설정
  - Broker는 처리 스레드가 불필요하고 결과 병합은 힙에서 수행
- 백프레셔: `druid.broker.http.maxQueuedBytes`를 대략 `2MiB × Historical 수`로 설정
- 비율: Historical 15대당 Broker 1대를 출발점으로 하되, 고가용성을 위해 최소 2대 구성

#### 커넥션 풀 사이징

Historical이 Broker의 요청을 처리할 여유를 남기려면, 모든 Broker의 `druid.broker.http.numConnections` 합을 각 Historical의 `druid.server.http.numThreads`보다 약간 작게 잡아야 한다. Broker 자체의 `druid.server.http.numThreads`도 자신의 `numConnections`보다 약간 크게 설정한다.

기준선은 프로세스당 동시 쿼리 50개에 비쿼리 요청 10개를 더하는 것이다. 이 기준이면 Historical과 태스크에 `druid.server.http.numThreads=60`을 설정한다. Broker 3대가 각각 `numConnections=10`을 사용한다면 Historical 하나에 들어오는 쿼리 커넥션은 30개이므로 `numThreads`는 40 이상이 필요하다. 풀이 너무 작으면 클러스터를 충분히 활용하지 못하고, 너무 크면 OOM과 자원 경합 위험이 커진다.

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

- Coordinator 힙: Broker 힙과 같거나 약간 작게
  - 서버 수, 세그먼트 수, 태스크 수에 비례
- Overlord 힙: Coordinator 힙의 25~50%. 실행 중인 태스크 수에 비례
- 수십만 개 이상 세그먼트가 있는 대형 클러스터에서는 Coordinator 동적 설정 `percentOfSegmentsToConsiderPerMove`를 기본 100에서 66 정도로 낮춰 코디네이션 주기 단축 가능
- Router 힙: 256MiB에서 시작
  - Broker로 프록시만 하므로 자원 요구가 가벼움

#### GroupBy 튜닝 지표

- `GroupByStatsMonitor`(`org.apache.druid.server.metrics.GroupByStatsMonitor`)를 켜면 다음 지표로 버퍼 크기 조정 가능

- `mergeBuffer/maxBytesUsed`: 한계에 근접하면 `buffer.sizeBytes` 증가
- `groupBy/maxSpilledBytes`: 버퍼 부족 신호 → 버퍼 크기 또는 `maxOnDiskStorage` 조정
- `groupBy/spilledQueries`: 0이 아니면 버퍼가 작다는 의미
- `mergeBuffer/pendingRequests`: 0이 아니면 병합 버퍼 고갈 → `numMergeBuffers` 증가

#### 세그먼트별 다이렉트 메모리 버퍼

- 세그먼트 압축 해제: 읽는 세그먼트의 컬럼당 64KiB 할당(`64KiB × 컬럼 수 × 세그먼트 수`)
- 인제스천 중 세그먼트 병합: String 컬럼마다 `카디널리티 × 4바이트` 버퍼를 세그먼트별로 할당
  - 이 할당은 `druid.processing.numMergeBuffers`와 무관

#### JVM과 OS 튜닝

- GC 권장 설정:

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

- 시스템 권장 사항:

- Historical, MiddleManager, Indexer에는 SSD를 강력히 권장
- Historical 디스크는 RAID보다 JBOD가 처리량 면에서 유리할 수 있음
- 스왑은 사용 금지
  - 메모리 맵된 세그먼트 파일 때문에 성능이 예측 불가능해짐
- `/tmp`를 tmpfs에 마운트하면 GC 일시 정지 감소 가능
- GC 로그와 Druid 로그는 데이터와 다른 디스크에 배치
- Transparent Huge Pages 비활성화
- `ulimit`(열 수 있는 파일 수)을 세그먼트 파일 수보다 충분히 크게 설정(`/etc/security/limits.conf`), 메모리 맵이 많은 Historical을 위해 `/proc/sys/vm/max_map_count`도 증가(`/etc/sysctl.d/`)
- 모든 이벤트와 호스트에서 UTC 시간대 사용

<a id="참고-자료"></a>

### 참고 자료

- [Configuration reference](https://druid.apache.org/docs/latest/configuration/)
- [Extensions](https://druid.apache.org/docs/latest/configuration/extensions)
- [Basic cluster tuning](https://druid.apache.org/docs/latest/operations/basic-cluster-tuning)
- [Router service](https://druid.apache.org/docs/latest/design/router)

## Druid 운영

> 원본: https://druid.apache.org/docs/latest/operations/web-console

> 원본: https://druid.apache.org/docs/latest/operations/rolling-updates

> 원본: https://druid.apache.org/docs/latest/operations/high-availability

> 원본: https://druid.apache.org/docs/latest/operations/rule-configuration

> 원본: https://druid.apache.org/docs/latest/operations/metrics

> 원본: https://druid.apache.org/docs/latest/operations/alerts

> 원본: https://druid.apache.org/docs/latest/operations/clean-metadata-store

클러스터 설정을 마쳤다면 실행 상태를 확인하고, 서비스를 업데이트하며, 오래된 데이터를 정리할 방법이 필요하다. 이 절에서는 웹 콘솔에서 시작해 롤링 업데이트와 고가용성 구성, retention 규칙, 메트릭과 알림, 메타데이터 스토리지 정리를 살펴본다.

<a id="웹-콘솔"></a>
### 웹 콘솔

- Druid 웹 콘솔은 데이터 관리, 클러스터 상태 모니터링, 쿼리 실행을 위한 내장 인터페이스
  - Router 서비스가 호스팅

#### 접속과 사전 요구 사항

- 접속 주소: `http://<ROUTER_IP>:<ROUTER_PORT>`
- 다음 두 설정 필요(기본적으로 활성화됨)
  - Router의 management proxy 활성화
  - Broker 프로세스에서 Druid SQL 활성화
- 보안 참고: 사용자 권한을 적절히 구성해야 하며, Druid를 root 사용자로 실행하면 금지

#### 주요 뷰

- Home: Status, Datasources, Segments, Supervisors, Tasks, Services, Lookups로 이동하는 카드를 보여주는 대시보드
- Query: 멀티 탭 SQL 인터페이스
  - Druid 24.0부터 기본인 multi-stage query task engine 지원
- Data loader: 단계별 마법사(wizard)로 인제스천(ingestion) 스펙을 작성하며, 각 단계마다 데이터 미리보기 제공
- Datasources: 로드된 모든 데이터소스와 크기, 가용성 표시
  - retention 규칙 편집, 자동 컴팩션 설정, 데이터 삭제, 세그먼트 타임라인 조회 지원
- Supervisors: 인덱싱 태스크 슈퍼바이저 관리
  - suspend, resume, reset, 스펙 제출 가능하며 상세 진행 리포트 제공
- Tasks: 실행 중, 완료된 태스크 목록을 Type, Datasource, Status별로 그룹화해 표시
  - 태스크를 직접 제출하고 상세 정보 조회 가능
- Segments: 클러스터의 모든 세그먼트 표시
  - Datasource, Start, End, Version, Partition 열로 필터링, 정렬 가능
- Services: 클러스터 노드 상태를 Type 또는 Tier별로 그룹화해 요약 통계와 함께 표시
- Lookups: 쿼리 타임 lookup을 생성하고 편집하는 관리 인터페이스

#### Query 뷰 세부 기능

- 스키마/데이터소스 브라우저 패널
- Run/Preview 버튼으로 쿼리 실행
- API 엔드포인트를 선택하는 engine 선택기
- 실시간 진행 상황 추적과 라이브 리포트
- 쿼리 히스토리와 태스크 모니터링
- SQL 실행 계획(EXPLAIN) 확인, 인제스천 스펙 변환 도구

<a id="롤링-업데이트"></a>

### 롤링 업데이트

- Druid 클러스터는 무중단(zero downtime)으로 롤링 업데이트 가능
  - 서비스는 다음 순서로 업데이트

- 1\. Historical
- 2\. Middle Manager와 Indexer
- 3\. Broker
- 4\. Router
- 5\. Overlord (autoscaling을 사용하면 Middle Manager보다 먼저 업데이트 가능)
- 6\. Coordinator (또는 Coordinator+Overlord 통합 프로세스)

- 다운그레이드할 때는 이 순서를 반대로, Coordinator부터 시작

#### Historical

Historical은 재시작하면 이전에 서빙하던 모든 세그먼트를 다시 메모리 매핑한다. 하드웨어에 따라 이 작업에 수 초에서 수 분이 걸리므로 한 번에 하나씩 업데이트한다.

#### Overlord

- 한 번에 하나씩 순차적으로 업데이트

#### Middle Manager / Indexer

- 실시간 인덱싱 태스크가 실행 중이므로 세 가지 전략 중 하나 사용

- 1\. 태스크 복원(restore) 기반
  - `druid.indexer.task.restoreTasksOnRestart=true`를 설정하면 Middle Manager 재시작 시 태스크 복원됨
  - 단, 실시간(realtime) 태스크만 복원 지원, 그 외 태스크는 재제출 필요
- 2\. 정상 종료(graceful termination)
  - disable API로 Middle Manager가 새 태스크를 받지 않도록 만든 뒤, 실행 중인 태스크가 모두 끝나면 업데이트

```bash
# 새 태스크 수락 중지
POST http://<MM_IP>:<PORT>/druid/worker/v1/disable

# 실행 중인 태스크 확인 (빈 목록이 되면 업데이트 가능)
GET http://<MM_IP>:<PORT>/druid/worker/v1/tasks
```

- 재시작하면 자동으로 다시 활성화됨

- 3\. Autoscaling 기반 교체
  - autoscaling을 사용하는 환경에서는 다음 두 속성의 버전 값을 올리면, 새 버전의 Middle Manager가 대량으로 기동되고 기존 프로세스는 우아하게 종료됨

```properties
druid.indexer.runner.minWorkerVersion=#{VERSION}
druid.indexer.autoscale.workerVersion=#{VERSION}
```

#### Standalone Real-time

- 한 번에 하나씩 순차적으로 업데이트

#### Broker

- 한 번에 하나씩 업데이트하되, 각 프로세스 사이에 간격 확보
  - Broker는 유효한 결과를 반환하려면 클러스터의 전체 상태를 먼저 로드해야 하기 때문

#### Coordinator

- 한 번에 하나씩 업데이트

<a id="고가용성"></a>

### 고가용성

#### ZooKeeper

- 고가용성 ZooKeeper를 구성하려면 3개 또는 5개 노드로 이루어진 ZooKeeper 클러스터 필요
  - 전용 하드웨어를 사용하거나, Overlord, Coordinator를 호스팅하는 Master 서버 3~5대에서 함께 실행 가능

#### 메타데이터 스토리지

- 고가용성 메타데이터 저장을 위해 복제(replication)와 페일오버(failover)를 활성화한 MySQL 또는 PostgreSQL 사용 권장

#### Coordinator와 Overlord

Coordinator와 Overlord는 여러 대를 실행하고 동일한 ZooKeeper 클러스터와 메타데이터 스토리지에 연결해 고가용성을 구성한다. 각 서비스는 한 번에 하나만 활성(active) 상태가 되고, 비활성 서버는 요청을 활성 서버로 리다이렉트한다. 활성 서버에 장애가 나면 자동으로 페일오버한다.

#### Broker

- Broker는 수평 확장(scale out) 가능하고 실행 중인 모든 서버가 활성 상태로 쿼리 처리
  - 쿼리 분산을 위해 로드 밸런서 뒤에 배치하는 것을 권장

<a id="retention-규칙-설정"></a>

### Retention 규칙 설정

Retention 규칙은 Druid가 어떤 데이터를 유지하고 어떤 데이터를 클러스터에서 제거(drop)할지 결정한다. 규칙은 JSON 객체로 메타데이터 스토리지에 영속 저장되며, Coordinator가 세그먼트마다 적용할 규칙을 평가한다.

#### 규칙 평가 순서

Coordinator는 목록 순서대로 규칙을 읽고, 각 세그먼트에 처음으로 매칭되는 규칙 하나만 적용한다. 따라서 같은 세그먼트에 여러 규칙이 매칭되더라도 앞에 둔 규칙이 적용되므로 순서를 의도에 맞게 정해야 한다.

#### 규칙 설정 방법

- 웹 콘솔: Datasources > 데이터소스 선택 > Actions > Edit retention rules > +New rule > 속성 설정 > Save

- Coordinator API:

```bash
# 기본 규칙 설정
POST /druid/coordinator/v1/rules/_default

# 특정 데이터소스 규칙 설정
POST /druid/coordinator/v1/rules/{datasourceName}

# 전체 규칙 조회
GET /druid/coordinator/v1/rules
```

- API 요청마다 원하는 순서로 정렬한 규칙 배열 전체를 전달 필요

#### 로드(load) 규칙

- 세그먼트를 Historical 티어에 할당하고 복제본 수를 지정
  - 모든 로드 규칙은 다음 속성을 지원

- `tieredReplicants`: 티어 이름과 복제본 수의 매핑
- `useDefaultTierForNull`: 기본값 `true`. `tieredReplicants`를 지정하지 않으면 `{"_default_tier": 2}` 사용

- Forever Load Rule (`loadForever`): 모든 세그먼트에 적용

```json
{
  "type": "loadForever",
  "tieredReplicants": {
    "hot": 1,
    "_default_tier": 1
  }
}
```

- Period Load Rule (`loadByPeriod`): ISO 8601 기간 내의 세그먼트를 대상
  - `includeFuture`가 true이면 기간과 겹치거나 기간 시작 이후에 시작하는 세그먼트를 매칭

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

- Interval Load Rule (`loadByInterval`): 고정된 날짜 구간을 대상

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

- 세그먼트를 클러스터에서 제거할 조건을 정의
  - 드롭된 세그먼트도 딥 스토리지(deep storage)에는 남아 있음

- Forever Drop Rule (`dropForever`): 모든 세그먼트를 드롭

```json
{
  "type": "dropForever"
}
```

- Period Drop Rule (`dropByPeriod`): 기간에 매칭되는 데이터를 드롭

```json
{
  "type": "dropByPeriod",
  "period": "P1M",
  "includeFuture": true
}
```

- Period Drop Before Rule (`dropBeforeByPeriod`): 지정한 기간 이전의 오래된 데이터를 드롭

```json
{
  "type": "dropBeforeByPeriod",
  "period": "P1M"
}
```

- Interval Drop Rule (`dropByInterval`): 특정 구간의 데이터가 로드되지 않도록 함

```json
{
  "type": "dropByInterval",
  "interval": "2012-01-01/2013-01-01"
}
```

#### 브로드캐스트(broadcast) 규칙

- 세그먼트를 모든 Broker에 로드(테스트 환경 전용). Broker에 `druid.segmentCache.locations` 설정 필요

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

- 영구 삭제: 규칙으로 클러스터에서 드롭된 세그먼트는 항상 `unused`로 표시됨
  - `unused` 세그먼트를 딥 스토리지에서까지 삭제하려면 kill 태스크 제출 필요
- 드롭된 데이터 재로드: 규칙 하나로는 불가능
  - (1) retention 기간을 늘리고, (2) API 또는 웹 콘솔에서 세그먼트를 `used`로 표시하면 Coordinator가 누락된 세그먼트를 다시 로드

<a id="메트릭"></a>

### 메트릭

- Druid는 설정 가능한 emitter로 메트릭을 내보냄
  - 모든 메트릭은 `timestamp`, `metric`(메트릭 이름), `service`, `host`, `version`, `buildRevision`, 숫자 `value` 필드를 공통으로 가짐
  - 대부분의 메트릭 값은 `druid.monitoring.emissionPeriod`로 정한 방출 주기마다 초기화됨

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
- `service/heartbeat`: 서비스 동작 지표
  - 값 1 (`ServiceStatusMonitor` 필요)

#### 시스템 메트릭 (OshiSysMonitor 권장)

- `sys/mem/used`: 시스템 메모리 사용량
- `sys/cpu`: 프로세스별 CPU 사용률
- `sys/disk/read/size`: 디스크 읽기 바이트 수
- `cgroup/memory/usage/bytes`: 컨테이너 메모리 사용량 (cgroup 환경)

- 각 메트릭에는 필터링과 집계를 위한 디멘션(dimension)이 함께 붙음(`dataSource`, `taskId`, `server`, `tier` 등).

<a id="알림"></a>

### 알림

- Druid는 예상하지 못한 상황을 만나면 알림(alert)을 생성
  - 알림은 JSON 객체로 런타임 로그 파일에 기록하거나 HTTP로 Apache Kafka 같은 외부 서비스에 내보낼 수 있음
  - 알림 방출은 기본적으로 비활성화되어 있으므로, 사용하려면 emitter 설정에서 명시적으로 활성화 필요

#### 공통 알림 필드

- `timestamp`: 알림이 생성된 시각
- `service`: 알림을 발생시킨 서비스 이름
- `host`: 알림을 발생시킨 호스트 이름
- `severity`: 심각도
  - 예: `anomaly`, `component-failure`, `service-failure`
- `description`: 알림에 대한 맥락 정보
- `data`: 예외의 경우 `exceptionType`, `exceptionMessage`, `exceptionStackTrace`를 담은 JSON 객체

- 알림은 요청 로깅(request logging), 메트릭 수집과 함께 Druid 운영 모니터링을 구성하는 요소 중 하나

<a id="메타데이터-스토리지-정리"></a>

### 메타데이터 스토리지 정리

데이터소스와 개체를 자주 만들고 삭제하면 메타데이터 스토리지에 오래된 레코드가 쌓여 성능이 떨어질 수 있다. Druid의 메타데이터 자동 정리 기능으로 이런 레코드를 제거할 수 있으며, 기본 retention 기간은 90일이다. 다만 컴팩션 설정과 인덱서 태스크 로그 정리는 기본적으로 비활성화되어 있다.

#### 정리 대상 메타데이터 유형

- 1\. 세그먼트 레코드와 딥 스토리지의 세그먼트: kill 태스크 설정 필요
- 2\. 감사(audit) 레코드: retention 기간이 지나면 전부 정리 대상
- 3\. 슈퍼바이저 레코드: 슈퍼바이저 종료 후 retention 기간이 지나면 대상
- 4\. 규칙(rule) 레코드: kill 태스크 필요, 모든 세그먼트가 kill된 후 대상
- 5\. 컴팩션 설정 레코드: 세그먼트가 없는 비활성 데이터소스 대상
- 6\. 데이터소스 레코드: 슈퍼바이저가 생성한 레코드, 슈퍼바이저 종료 후 대상
- 7\. 인덱싱 상태 레코드: 미사용이거나 대기(pending) 상태로 retention 기간이 지난 경우
- 8\. 인덱서 태스크 로그: 딥 스토리지와 메타데이터에서 함께 제거

#### Coordinator 설정 속성

- `druid.coordinator.period.metadataStoreManagementPeriod`: 메타데이터 관리 작업 실행 주기 (기본값 없음)
- `druid.coordinator.kill.on`: 세그먼트 레코드 정리 활성화 (기본값 true)
- `druid.coordinator.kill.period`: kill 태스크 실행 주기 (ISO 8601) (기본값 P1D)
- `druid.coordinator.kill.durationToRetain`: 삭제 전 보존 기간 (기본값 P90D)
- `druid.coordinator.kill.bufferPeriod`: 정리 전 버퍼 기간 (기본값 없음)
- `druid.coordinator.kill.maxSegments`: 태스크당 최대 삭제 세그먼트 수 (기본값 없음)
- `druid.coordinator.kill.audit.on`: 감사 레코드 정리 활성화 (기본값 false)
- `druid.coordinator.kill.audit.period`: 감사 레코드 정리 주기 (기본값 P1D)
- `druid.coordinator.kill.audit.durationToRetain`: 감사 레코드 보존 기간 (기본값 P90D)
- `druid.coordinator.kill.supervisor.on`: 슈퍼바이저 레코드 정리 활성화 (기본값 false)
- `druid.coordinator.kill.supervisor.period`: 슈퍼바이저 레코드 정리 주기 (기본값 P1D)
- `druid.coordinator.kill.supervisor.durationToRetain`: 슈퍼바이저 레코드 보존 기간 (기본값 P90D)
- `druid.coordinator.kill.rule.on`: 규칙 레코드 정리 활성화 (기본값 false)
- `druid.coordinator.kill.rule.period`: 규칙 레코드 정리 주기 (기본값 P1D)
- `druid.coordinator.kill.rule.durationToRetain`: 규칙 레코드 보존 기간 (기본값 P90D)
- `druid.coordinator.kill.compaction.on`: 컴팩션 설정 정리 활성화 (기본값 false)
- `druid.coordinator.kill.compaction.period`: 컴팩션 설정 정리 주기 (기본값 P1D)
- `druid.coordinator.kill.datasource.on`: 데이터소스 레코드 정리 활성화 (기본값 false)
- `druid.coordinator.kill.datasource.period`: 데이터소스 레코드 정리 주기 (기본값 P1D)
- `druid.coordinator.kill.datasource.durationToRetain`: 데이터소스 레코드 보존 기간 (기본값 P90D)

#### Overlord 설정 속성

- `druid.overlord.kill.indexingStates.on`: 인덱싱 상태 레코드 정리 활성화 (기본값 false)
- `druid.overlord.kill.indexingStates.period`: 인덱싱 상태 정리 주기 (기본값 P1D)
- `druid.overlord.kill.indexingStates.durationToRetain`: 비활성 상태 보존 기간 (기본값 P7D)
- `druid.overlord.kill.indexingStates.pendingDurationToRetain`: 대기 상태 보존 기간 (기본값 P7D)
- `druid.indexer.logs.kill.enabled`: 태스크 로그 정리 활성화 (기본값 false)
- `druid.indexer.logs.kill.durationToRetain`: 태스크 로그 보존 기간 (밀리초) (기본값 없음)
- `druid.indexer.logs.kill.initialDelay`: 첫 정리까지의 초기 지연 (밀리초) (기본값 없음)
- `druid.indexer.logs.kill.delay`: 정리 작업 간 지연 (밀리초) (기본값 없음)

#### 사전 요구 사항과 주의점

- 규칙 레코드와 컴팩션 설정 정리에는 kill 태스크 활성화(`druid.coordinator.kill.on=true`)가 선행돼야 함
- 메타데이터 관리 주기(`metadataStoreManagementPeriod`)는 개별 정리 작업 주기와 같거나 더 짧아야 함
- kill 태스크는 dynamic configuration의 `killDataSourceWhitelist`를 따름
- kill 태스크는 메타데이터뿐 아니라 딥 스토리지의 실제 데이터까지 삭제하는 유일한 정리 작업
- 데이터소스가 존재하기 전에 만든 컴팩션 설정은 조기에 삭제될 수 있음
- 감사(audit) 규정 준수가 필요하면 정리를 활성화하기 전에 감사 레코드를 미리 내보내야 함
- 컴팩션 설정이 크면 감사 로그 크기 제한을 초과할 수 있으므로 `druid.audit.manager.maxPayloadSizeBytes` 조정 필요
- 인덱싱 상태 정리는 기본 비활성화이며, 자동 컴팩션 슈퍼바이저를 사용할 때만 해당 레코드가 생성됨
- 정리를 끄려면 `druid.coordinator.kill.on=false`와 함께 각 개체별 정리 플래그를 `false`로 설정

### 참고 자료

- [Web console](https://druid.apache.org/docs/latest/operations/web-console)
- [Rolling updates](https://druid.apache.org/docs/latest/operations/rolling-updates)
- [High availability](https://druid.apache.org/docs/latest/operations/high-availability)
- [Using rules to drop and retain data](https://druid.apache.org/docs/latest/operations/rule-configuration)
- [Metrics](https://druid.apache.org/docs/latest/operations/metrics)
- [Alerts](https://druid.apache.org/docs/latest/operations/alerts)
- [Automated cleanup for metadata records](https://druid.apache.org/docs/latest/operations/clean-metadata-store)
