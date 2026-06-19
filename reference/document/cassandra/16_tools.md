# Cassandra 도구

> 이 문서는 Apache Cassandra 공식 문서의 "Tools" 섹션을 한국어로 번역한 것입니다.
> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/tools/

---

## 목차

1. [cqlsh: CQL 셸](#cqlsh-cql-셸)
   - [개요](#개요)
   - [호환성](#호환성)
   - [선택적 의존성(Optional Dependencies)](#선택적-의존성optional-dependencies)
   - [설정 파일(Configuration Files)](#설정-파일configuration-files)
   - [명령줄 옵션(Command Line Options)](#명령줄-옵션command-line-options)
   - [특수 명령(Special Commands)](#특수-명령special-commands)
     - [CONSISTENCY](#consistency)
     - [SERIAL CONSISTENCY](#serial-consistency)
     - [SHOW](#show)
     - [SOURCE](#source)
     - [CAPTURE](#capture)
     - [HELP](#help)
     - [HISTORY](#history)
     - [TRACING](#tracing)
     - [PAGING](#paging)
     - [EXPAND](#expand)
     - [LOGIN](#login)
     - [EXIT](#exit)
     - [CLEAR](#clear)
     - [DESCRIBE / DESC](#describe--desc)
     - [COPY TO](#copy-to)
     - [COPY FROM](#copy-from)
     - [COPY 공통 옵션(Shared COPY Options)](#copy-공통-옵션shared-copy-options)
   - [따옴표 이스케이프(Escaping Quotes)](#따옴표-이스케이프escaping-quotes)
2. [nodetool](#nodetool)
   - [사용법(Usage)와 시놉시스(Synopsis)](#사용법usage와-시놉시스synopsis)
   - [전역 옵션(Global / JMX Options)](#전역-옵션global--jmx-options)
   - [전체 서브커맨드 목록](#전체-서브커맨드-목록)
   - [주요 서브커맨드 상세](#주요-서브커맨드-상세)
3. [SSTable 도구](#sstable-도구)
   - [sstabledump](#sstabledump)
   - [sstableloader](#sstableloader)
   - [sstablemetadata](#sstablemetadata)
   - [sstablescrub](#sstablescrub)
   - [sstableverify](#sstableverify)
   - [sstablesplit](#sstablesplit)
   - [sstableupgrade](#sstableupgrade)
   - [sstableutil](#sstableutil)
   - [sstablerepairedset](#sstablerepairedset)
   - [sstablelevelreset](#sstablelevelreset)
   - [sstableofflinerelevel](#sstableofflinerelevel)
   - [sstableexpiredblockers](#sstableexpiredblockers)
   - [sstablepartitions](#sstablepartitions)
4. [cassandra-stress](#cassandra-stress)
   - [개요](#개요-1)
   - [명령 구문(Command Syntax)](#명령-구문command-syntax)
   - [명령(Commands)](#명령commands)
   - [주요 옵션 그룹(Primary Options)](#주요-옵션-그룹primary-options)
   - [User 모드와 YAML 프로파일](#user-모드와-yaml-프로파일)
   - [예제 실행(Example Invocations)](#예제-실행example-invocations)

---

## cqlsh: CQL 셸

### 개요

`cqlsh`는 CQL(Cassandra Query Language)을 사용하여 Cassandra와 상호작용하기 위한 명령줄 인터페이스(command-line interface)입니다. 모든 Cassandra 패키지의 `bin/` 디렉터리에 함께 배포됩니다. `cqlsh`는 Python 네이티브 프로토콜 드라이버(Python native protocol driver)를 사용하여 구현되었으며, 사용자가 지정한 단일 노드(single node)에 연결합니다.

### 호환성

특정 `cqlsh` 버전은 공식적으로 그에 대응하는 Cassandra 릴리스(release)와만 동작하는 것이 보장됩니다. 다른 버전과도 비공식적으로(unofficially) 동작할 수는 있습니다.

### 선택적 의존성(Optional Dependencies)

`cqlsh`는 일부 기능을 위해 선택적 Python 모듈을 사용합니다.

- **pytz**: Python 3.9 이상에서는 `cqlshrc` 설정 또는 `TZ` 환경 변수(environment variable)를 통해 타임존(timezone) 표시를 활성화합니다. Python 3.8 이하에서는 명시적으로 `pytz`를 설치해야 합니다.
- **cython**: 핵심 Python 모듈을 컴파일(compile)하여 `COPY` 작업의 성능을 향상시킵니다.

### 설정 파일(Configuration Files)

- **cqlshrc**: 기본 위치는 `~/.cassandra/cqlshrc`이며 설정 옵션을 담고 있습니다. 샘플 문서는 `conf/cqlshrc.sample`에서 확인할 수 있습니다.
- **credentials**: 사용자 이름(username)과 비밀번호(password)를 담고 있으며, `cqlshrc`의 `auth_provider` 클래스명(classname)과 일치해야 합니다. 이 파일은 반드시 사용자 소유(user-owned)여야 하며 접근 권한(permissions)을 제한해야 합니다.
- **cql_history**: 기본 위치는 `~/.cassandra/cql_history`입니다. `CQL_HISTORY` 환경 변수로 위치를 변경하거나, `/dev/null`을 지정하여 완전히 비활성화할 수 있습니다. Cassandra 4.0부터 지원됩니다.

### 명령줄 옵션(Command Line Options)

| 옵션 | 용도 |
|------|------|
| `--version` | 프로그램 버전을 표시합니다. |
| `-h, --help` | 도움말 메시지를 표시합니다. |
| `-C, --color` | 컬러(color) 출력을 강제합니다. |
| `--no-color` | 컬러 출력을 비활성화합니다. |
| `--browser=BROWSER` | 도움말 표시에 사용할 브라우저(browser)를 지정합니다. |
| `--ssl` | SSL 연결을 활성화합니다. |
| `-u, --username=USERNAME` | 인증(authentication)에 사용할 사용자 이름입니다. |
| `-p, --password=PASSWORD` | 인증에 사용할 비밀번호입니다. |
| `-k, --keyspace=KEYSPACE` | 대상 키스페이스(keyspace)입니다. |
| `-f, --file=FILE` | 파일에서 명령을 실행한 뒤 종료합니다. |
| `--debug` | 디버깅(debugging) 정보를 표시합니다. |
| `--coverage` | 커버리지(coverage) 데이터를 수집합니다. |
| `--encoding=ENCODING` | 출력 인코딩(encoding)을 지정합니다(기본값: utf-8). |
| `--cqlshrc=CQLSHRC` | `cqlshrc`의 대체 위치를 지정합니다. |
| `--credentials=CREDENTIALS` | `credentials`의 대체 위치를 지정합니다. |
| `--cqlversion=CQLVERSION` | 특정 CQL 버전을 지정합니다. |
| `--protocol-version=PROTOCOL_VERSION` | 특정 프로토콜(protocol) 버전을 지정합니다. |
| `-e, --execute=EXECUTE` | 문장(statement)을 실행하고 종료합니다. |
| `--connect-timeout=CONNECT_TIMEOUT` | 연결 타임아웃(초 단위, 기본값: 5)입니다. |
| `--request-timeout=REQUEST_TIMEOUT` | 요청 타임아웃(초 단위, 기본값: 10)입니다. |
| `-t, --tty` | TTY 모드를 강제합니다. |
| `-v` | 현재 버전을 출력합니다. |

### 특수 명령(Special Commands)

`cqlsh`는 표준 CQL 문 외에도 자체적인 특수 명령(special command)을 제공합니다.

#### CONSISTENCY

이후 수행되는 작업에 대한 일관성 수준(consistency level)을 설정합니다. 지원 값은 다음과 같습니다: `ANY`, `ONE`, `TWO`, `THREE`, `QUORUM`, `ALL`, `LOCAL_QUORUM`, `LOCAL_ONE`, `SERIAL`, `LOCAL_SERIAL`.

#### SERIAL CONSISTENCY

조건부 업데이트(conditional update, 즉 `IF`가 포함된 `INSERT`/`UPDATE`/`DELETE`)에 대한 직렬 일관성(serial consistency)을 설정합니다. 옵션은 `SERIAL` 또는 `LOCAL_SERIAL`입니다. 이는 Paxos 단계(phase)의 일관성을 정의하며, 일반 일관성(regular consistency)은 학습 단계(learn phase)의 보장(guarantee)을 정의합니다.

#### SHOW

여러 하위 명령을 제공합니다.

- **SHOW VERSION**: `cqlsh`, Cassandra, CQL, 네이티브 프로토콜(native protocol) 버전을 표시합니다.
- **SHOW HOST**: 연결된 노드의 IP 주소, 포트(port), 클러스터 이름(cluster name)을 출력합니다.
- **SHOW REPLICAS**: 지정한 토큰(token)과 키스페이스에 대한 레플리카(replica) IP 주소를 나열합니다. Cassandra 4.2부터 사용할 수 있습니다.
  - 사용법: `SHOW REPLICAS <token> (<keyspace>)`
- **SHOW SESSION**: 특정 트레이싱(tracing) 세션을 보기 좋게 출력(pretty-print)합니다.
  - 사용법: `SHOW SESSION <session id>`

#### SOURCE

파일의 각 줄을 CQL 문 또는 `cqlsh` 명령으로 실행합니다.

- 사용법: `SOURCE '<file path>'`

#### CAPTURE

명령 출력을 콘솔에 표시하지 않고 파일로 캡처(capture)합니다.

- `CAPTURE '<file>';` — 캡처를 시작합니다.
- `CAPTURE OFF;` — 캡처를 중지합니다.
- `CAPTURE;` — 현재 설정을 확인합니다.

쿼리 결과(query result)만 캡처되며, 오류(error)는 정상적으로 화면에 표시됩니다. 홈 디렉터리(home directory)를 가리키는 틸드(tilde, `~`) 표기를 지원합니다.

#### HELP

`cqlsh` 명령에 대한 정보를 제공합니다. `HELP`만 입력하면 사용 가능한 주제(topic) 목록을 표시하고, `HELP <topic>`은 특정 주제에 대한 정보를 표시합니다.

#### HISTORY

마지막으로 실행한 n개의 명령을 표시합니다(기본값: 50). 세션(session)별로 설정됩니다.

- 사용법: `HISTORY <n>`

#### TRACING

쿼리 이벤트(query event) 트레이싱을 활성화하거나 비활성화합니다.

- 사용법: `TRACING ON` 또는 `TRACING OFF`

#### PAGING

읽기 쿼리(read query)의 페이징(paging)을 제어합니다.

- `PAGING ON` — 페이징을 활성화합니다.
- `PAGING OFF` — 페이징을 비활성화합니다.
- `PAGING <page size>` — 페이지 크기를 행(row) 단위로 설정합니다.

#### EXPAND

수직(vertical) 행 출력을 활성화하거나 비활성화합니다. 컬럼이 많거나 내용이 큰 경우에 유용합니다.

- 사용법: `EXPAND ON` 또는 `EXPAND OFF`

#### LOGIN

현재 세션에서 지정한 사용자로 인증합니다.

- 사용법: `LOGIN <username> [<password>]`

#### EXIT

현재 세션과 `cqlsh` 프로세스를 종료합니다.

- 사용법: `EXIT` 또는 `QUIT`

#### CLEAR

콘솔(console)을 지웁니다.

- 사용법: `CLEAR` 또는 `CLS`

#### DESCRIBE / DESC

스키마 요소(schema element)의 설명을 DDL(Data Definition Language) 문 형태로 출력합니다. `DESC`는 `DESCRIBE`의 축약형(substitute)으로 사용할 수 있습니다.

구문(syntax):

- `DESCRIBE CLUSTER` — 클러스터 이름과 파티셔너(partitioner)를 출력합니다.
- `DESCRIBE SCHEMA` — 전체 스키마를 재생성(recreate)하는 DDL을 출력합니다.
- `DESCRIBE KEYSPACES` — 모든 키스페이스를 출력합니다.
- `DESCRIBE KEYSPACE <name>` — 특정 키스페이스를 출력합니다.
- `DESCRIBE TABLES` — 모든 테이블(table)을 출력합니다.
- `DESCRIBE TABLE <name>` — 특정 테이블을 출력합니다.
- `DESCRIBE INDEX <name>` — 특정 인덱스(index)를 출력합니다.
- `DESCRIBE MATERIALIZED VIEW <name>` — 특정 구체화 뷰(materialized view)를 출력합니다.
- `DESCRIBE TYPES` — 모든 사용자 정의 타입(user-defined type)을 출력합니다.
- `DESCRIBE TYPE <name>` — 특정 타입을 출력합니다.
- `DESCRIBE FUNCTIONS` — 모든 함수(function)를 출력합니다.
- `DESCRIBE FUNCTION <name>` — 특정 함수를 출력합니다.
- `DESCRIBE AGGREGATES` — 모든 집계 함수(aggregate)를 출력합니다.
- `DESCRIBE AGGREGATE <name>` — 특정 집계 함수를 출력합니다.

#### COPY TO

테이블 데이터를 CSV 파일로 내보냅니다(export).

- 사용법: `COPY <table> [(<columns>)] TO <file> WITH <option> [AND <option>...]`

**옵션**:

- `MAXREQUESTS` — 동시에 가져올 수 있는 최대 토큰 범위(token range) 페치(fetch) 수입니다(기본값: 6).
- `PAGESIZE` — 페이지당 행 수입니다(기본값: 1000).
- `PAGETIMEOUT` — 페이지 타임아웃입니다(기본값: 1000개 항목당 10초).
- `BEGINTOKEN, ENDTOKEN` — 토큰 범위입니다(기본값: 전체 링(full ring)).
- `MAXOUTPUTSIZE` — 최대 출력 줄 수입니다. `-1`은 무제한(기본값)입니다.
- `ENCODING` — 문자 인코딩입니다(기본값: utf8).

특수 파일 값: `STDOUT`을 지정하면 표준 출력(standard output)으로 출력합니다.

#### COPY FROM

CSV 데이터를 테이블로 가져옵니다(import).

- 사용법: `COPY <table> [(<columns>)] FROM <file> WITH <option> [AND <option>...]`

**옵션**:

- `INGESTRATE` — 초당 처리되는 최대 행 수입니다(기본값: 100000).
- `MAXROWS` — 가져올 최대 행 수입니다(`-1`은 무제한, 기본값).
- `SKIPROWS` — 건너뛸 처음 행 수입니다(기본값: 0).
- `SKIPCOLS` — 무시할 컬럼들을 쉼표(comma)로 구분하여 지정합니다.
- `MAXPARSEERRORS` — 최대 파싱 오류(parsing error) 수입니다(`-1`은 무제한, 기본값).
- `MAXINSERTERRORS` — 최대 삽입 오류(insert error) 수입니다(`-1`은 무제한, 기본값: 1000).
- `ERRFILE` — 오류 파일 위치입니다(기본값: `import_<ks>_<table>.err`).
- `MAXBATCHSIZE` — 배치(batch)당 행 수입니다(기본값: 20).
- `MINBATCHSIZE` — 배치당 최소 행 수입니다(기본값: 10).
- `CHUNKSIZE` — 워커 프로세스(worker process)당 행 수입니다(기본값: 5000).

특수 파일 값: `STDIN`을 지정하면 표준 입력(standard input)에서 읽어 들입니다.

#### COPY 공통 옵션(Shared COPY Options)

다음 옵션은 `COPY TO`와 `COPY FROM` 양쪽 모두에서 사용할 수 있습니다.

- `NULLVAL` — 널(null) 자리 표시자(placeholder)입니다(기본값: `"null"`).
- `HEADER` — 컬럼 이름을 포함하거나 검사합니다(기본값: false).
- `DECIMALSEP` — 소수점 구분자(decimal separator)입니다(기본값: `"."`).
- `THOUSANDSSEP` — 천 단위 구분자(thousands separator)입니다(기본값: 빈 문자열).
- `BOOLSTYLE` — 불리언(boolean) 형식입니다(기본값: `"True,False"`).
- `NUMPROCESSES` — 자식 워커 프로세스(child worker process) 수입니다(기본값: 16, 최대값: `num_cores-1`).
- `MAXATTEMPTS` — 실패한 작업의 최대 재시도(retry) 횟수입니다(기본값: 5).
- `REPORTFREQUENCY` — 상태 업데이트 간격(초 단위)입니다(기본값: 0.25).
- `RATEFILE` — 선택적 속도 통계(rate statistics) 출력 파일입니다.

### 따옴표 이스케이프(Escaping Quotes)

날짜(date), IP 주소, 문자열(string)에는 작은따옴표(single quote)가 필요합니다. 문자열 내부에 들어가는 리터럴(literal) 아포스트로피(apostrophe)는 작은따옴표를 두 번 연속(`''`) 사용하여 이스케이프(escape)합니다.

단순 텍스트(simple text)는 따옴표 없이 문자열로 반환되지만, 복합 타입(complex type) 텍스트는 따옴표와 이스케이프가 적용된 문자열로 반환됩니다. 컬렉션(collection)과 사용자 정의 타입(user-defined type)에서는 이스케이프가 명시적으로 보이게 됩니다.

---

## nodetool

`nodetool`은 Cassandra 노드를 모니터링하고 관리하기 위한 명령줄 유틸리티입니다. JMX(Java Management Extensions)를 통해 노드와 통신합니다.

### 사용법(Usage)와 시놉시스(Synopsis)

```
nodetool [(-pwf <passwordFilePath> | --password-file <passwordFilePath>)]
        [(-p <port> | --port <port>)] [(-pp | --print-port)]
        [(-h <host> | --host <host>)] [(-u <username> | --username <username>)]
        [(-pw <password> | --password <password>)] <command> [<args>]
```

### 전역 옵션(Global / JMX Options)

다음 JMX 공통 옵션은 모든 `nodetool` 명령에서 사용할 수 있습니다.

- `-h <host>, --host <host>` — 노드의 호스트 이름(hostname) 또는 IP 주소입니다.
- `-p <port>, --port <port>` — 원격 JMX 에이전트(remote jmx agent) 포트 번호입니다.
- `-pp, --print-port` — 4.0 모드로 동작하여 호스트를 포트 번호로 구분(disambiguate)합니다.
- `-pw <password>, --password <password>` — 원격 JMX 에이전트 비밀번호입니다.
- `-pwf <passwordFilePath>, --password-file <passwordFilePath>` — JMX 비밀번호 파일(password file) 경로입니다.
- `-u <username>, --username <username>` — 원격 JMX 에이전트 사용자 이름입니다.
- `--` — 명령줄 옵션과 인자(argument) 목록을 분리하는 데 사용합니다(인자가 명령줄 옵션으로 오인될 수 있을 때 유용합니다).

### 전체 서브커맨드 목록

`nodetool`은 매우 많은 서브커맨드를 제공합니다. 기능별로 분류한 전체 목록은 다음과 같습니다.

- **노드 관리(Node Management)**: `assassinate`, `bootstrap`, `decommission`, `join`, `move`, `removenode`
- **클러스터 정보(Cluster Information)**: `describecluster`, `describering`, `gossipinfo`, `ring`, `status`
- **데이터 작업(Data Operations)**: `cleanup`, `compact`, `flush`, `forcecompact`, `garbagecollect`, `import`, `rebuild`, `refresh`, `scrub`, `upgradesstables`, `verify`
- **스냅샷(Snapshots)**: `clearsnapshot`, `listsnapshots`, `snapshot`
- **리페어 작업(Repair Operations)**: `repair`, `repair_admin`, `autorepairstatus`
- **캐시(Caching)**: `invalidatecountercache`, `invalidatekeycache`, `invalidaterowcache`, `setcachecapacity`, `setcachekeystosave`
- **인덱싱(Indexing)**: `rebuild_index`, `recompress_sstables`
- **모니터링(Monitoring)**: `clientstats`, `compactionhistory`, `compactionstats`, `failuredetector`, `gcstats`, `info`, `netstats`, `proxyhistograms`, `tablehistograms`, `tablestats`, `toppartitions`, `tpstats`
- **설정 조회 명령(Get Commands)**: `getauthcacheconfig`, `getautorepairconfig`, `getbatchlogreplaythrottle`, `getcolumnindexsize`, `getcompactionthreshold`, `getcompactionthroughput`, `getconcurrency`, `getconcurrentcompactors`, `getconcurrentviewbuilders`, `getdefaultrf`, `getendpoints`, `getfullquerylog`, `getguardrailsconfig`, `getinterdcstreamthroughput`, `getlogginglevels`, `getmaxhintwindow`, `getseeds`, `getsnapshotthrottle`, `getsstables`, `getstreamthroughput`, `gettimeout`, `gettraceprobability`
- **설정 변경 명령(Set Commands)**: `setauthcacheconfig`, `setautorepairconfig`, `setbatchlogreplaythrottle`, `setcachecapacity`, `setcachekeystosave`, `setcolumnindexsize`, `setcompactionthreshold`, `setcompactionthroughput`, `setconcurrency`, `setconcurrentcompactors`, `setconcurrentviewbuilders`, `setdefaultrf`, `setguardrailsconfig`, `sethintedhandoffthrottlekb`, `setinterdcstreamthroughput`, `setlogginglevel`, `setmaxhintwindow`, `setsnapshotthrottle`, `setstreamthroughput`, `settimeout`, `settraceprobability`
- **프로토콜 및 전송(Protocol & Transport)**: `disablebinary`, `enablebinary`, `statusbinary`, `disableoldprotocolversions`, `enableoldprotocolversions`
- **가십 및 힌트 핸드오프(Gossip & Handoff)**: `disablegossip`, `enablegossip`, `statusgossip`, `disablehandoff`, `enablehandoff`, `pausehandoff`, `resumehandoff`, `statushandoff`
- **로깅 및 감사(Logging & Auditing)**: `disableauditlog`, `enableauditlog`, `getauditlog`, `disablefullquerylog`, `enablefullquerylog`, `getfullquerylog`, `resetfullquerylog`
- **백업(Backup)**: `disablebackup`, `enablebackup`, `statusbackup`
- **스트리밍 및 데이터(Streaming & Data)**: `checktokenmetadata`, `datapaths`, `getendpoints`, `getsstables`, `profileload`, `rangekeysample`, `relocatesstables`, `sstablerepairedset`
- **힌트 관리(Hints Management)**: `disablehintsfordc`, `enablehintsfordc`, `listpendinghints`, `truncatehints`
- **CIDR 및 보안(CIDR & Security)**: `cidrfilteringstats`, `dropcidrgroup`, `getcidrgroupsofip`, `invalidatecidrpermissionscache`, `listcidrgroups`, `reloadcidrgroupscache`, `updatecidrgroup`
- **캐시 무효화(Cache Invalidation)**: `invalidatecredentialscache`, `invalidatejmxpermissionscache`, `invalidatenetworkpermissionscache`, `invalidatepermissionscache`, `invalidaterolescache`
- **스키마 및 트리거(Schema & Triggers)**: `reloadlocalschema`, `resetlocalschema`, `reloadtriggers`
- **SSL 및 기타(SSL & Other)**: `reloadssl`, `checktokenmetadata`, `drain`, `help`, `stop`, `stopdaemon`, `sjk`, `version`, `viewbuildstatus`

### 주요 서브커맨드 상세

> 참고: `nodetool` 서브커맨드 페이지는 자동 생성(auto-generated)되며 각 항목은 NAME(한 줄 목적 설명), SYNOPSIS(시놉시스), OPTIONS(옵션)로만 구성됩니다. 아래에서 시놉시스의 `[...common...]`은 위 [전역 옵션](#전역-옵션global--jmx-options) 섹션에 나열한 공통 JMX 옵션을 의미합니다.

---

#### status

- **목적**: 클러스터 정보(상태, 부하(load), ID 등)를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] status [(-r | --resolve-ip)] [--] [<keyspace>]
  ```
- **옵션**:
  - `-r, --resolve-ip` — IP 대신 노드의 도메인 이름(domain name)을 표시합니다.
  - `--` — 명령줄 옵션과 인자를 분리합니다.
  - `[<keyspace>]` — 키스페이스 이름입니다.

---

#### info

- **목적**: 노드 정보(가동 시간(uptime), 부하 등)를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] info [(-O | --out-of-range-ops)] [(-T | --tokens)]
  ```
- **옵션**:
  - `-O, --out-of-range-ops` — 잘못된 토큰(invalid token)에 대한 작업 횟수를 키스페이스별로 표시합니다.
  - `-T, --tokens` — 모든 토큰을 표시합니다.

---

#### repair

- **목적**: 하나 이상의 테이블을 리페어(repair)합니다.
- **시놉시스**:
  ```
  nodetool [...common...] repair
      [(-dc <specific_dc> | --in-dc <specific_dc>)...]
      [(-dcpar | --dc-parallel)]
      [(-et <end_token> | --end-token <end_token>)]
      [(-force | --force)] [(-full | --full)]
      [(-hosts <specific_host> | --in-hosts <specific_host>)...]
      [(-iuk | --ignore-unreplicated-keyspaces)]
      [(-j <job_threads> | --job-threads <job_threads>)]
      [(-local | --in-local-dc)] [(-os | --optimise-streams)]
      [(-paxos-only | --paxos-only)] [(-pl | --pull)]
      [(-pr | --partitioner-range)] [(-prv | --preview)]
      [(-seq | --sequential)] [(-skip-paxos | --skip-paxos)]
      [(-st <start_token> | --start-token <start_token>)]
      [(-tr | --trace)] [(-vd | --validate)]
      [--] [<keyspace> <tables>...]
  ```
- **옵션**:
  - `-dc, --in-dc` — 특정 데이터센터(datacenter)를 리페어합니다.
  - `-dcpar, --dc-parallel` — 데이터센터들을 병렬(parallel)로 리페어합니다.
  - `-et, --end-token` — 리페어 범위가 끝나는 토큰을 지정합니다(포함, inclusive).
  - `-force, --force` — 다운(down)된 엔드포인트(endpoint)를 필터링하여 제외합니다.
  - `-full, --full` — 전체 리페어(full repair)를 수행합니다.
  - `-hosts, --in-hosts` — 특정 호스트(host)를 리페어합니다.
  - `-iuk, --ignore-unreplicated-keyspaces` — 복제되지 않은 키스페이스(unreplicated keyspace)를 무시합니다.
  - `-j, --job-threads` — 동시 리페어 작업 스레드(repair job thread) 수입니다(기본값: 1, 최대: 4).
  - `-local, --in-local-dc` — 동일 데이터센터 내 노드에 대해서만 리페어합니다.
  - `-os, --optimise-streams` — 스트림(stream) 수를 줄입니다(실험적(experimental) 기능).
  - `-paxos-only, --paxos-only` — 테이블 데이터가 아닌 Paxos 작업만 리페어합니다.
  - `-pl, --pull` — 원격에서 로컬 노드로 단방향(one-way) 리페어 스트리밍을 수행합니다.
  - `-pr, --partitioner-range` — 파티셔너가 반환하는 첫 번째 범위만 리페어합니다.
  - `-prv, --preview` — 실제 리페어를 수행하지 않고 스트리밍할 범위와 데이터 양을 산정합니다.
  - `-seq, --sequential` — 순차(sequential) 리페어를 수행합니다.
  - `-skip-paxos, --skip-paxos` — Paxos 리페어 단계를 건너뜁니다.
  - `-st, --start-token` — 리페어 범위가 시작되는 토큰을 지정합니다(제외, exclusive).
  - `-tr, --trace` — 리페어를 트레이싱합니다. `system_traces.events`에 로깅됩니다.
  - `-vd, --validate` — 리페어된 데이터가 노드 간에 동기화되었는지 확인합니다.
  - `[<keyspace> <tables>...]` — 키스페이스와 하나 이상의 테이블입니다.

---

#### compact

- **목적**: 하나 이상의 테이블에 대해 (메이저) 컴팩션(major compaction)을 강제로 수행하거나, 지정한 SSTable에 대해 사용자 정의 컴팩션(user-defined compaction)을 수행합니다.
- **시놉시스**:
  ```
  nodetool [...common...] compact
      [(-et <end_token> | --end-token <end_token>)]
      [--partition <partition_key>] [(-s | --split-output)]
      [(-st <start_token> | --start-token <start_token>)]
      [--user-defined] [--]
      [<keyspace> <tables>...] or <SSTable file>...
  ```
- **옵션**:
  - `-et, --end-token` — 컴팩션 범위가 끝나는 토큰을 지정합니다(포함).
  - `--partition <partition_key>` — 파티션 키(partition key)의 문자열 표현입니다.
  - `-s, --split-output` — 하나의 큰 파일을 만들지 않습니다.
  - `-st, --start-token` — 컴팩션 범위가 시작되는 토큰을 지정합니다(포함).
  - `--user-defined` — 나열된 파일들을 사용자 정의 컴팩션에 제출합니다.
  - `[<keyspace> <tables>...] or <SSTable file>...` — 키스페이스와 하나 이상의 테이블, 또는 `--user-defined` 사용 시 SSTable 데이터 파일 목록입니다.

---

#### cleanup

- **목적**: 더 이상 노드에 속하지 않는 키(key)를 즉시 정리(cleanup)합니다. 기본적으로 모든 키스페이스를 정리합니다.
- **시놉시스**:
  ```
  nodetool [...common...] cleanup [(-j <jobs> | --jobs <jobs>)] [--] [<keyspace> <tables>...]
  ```
- **옵션**:
  - `-j, --jobs` — 동시에 정리할 SSTable 수입니다. `0`으로 설정하면 사용 가능한 모든 컴팩션 스레드를 사용합니다.
  - `[<keyspace> <tables>...]` — 키스페이스와 하나 이상의 테이블입니다.

---

#### flush

- **목적**: 하나 이상의 테이블을 플러시(flush)합니다(메모리의 데이터를 디스크로 기록).
- **시놉시스**:
  ```
  nodetool [...common...] flush [--] [<keyspace> <tables>...]
  ```
- **옵션**:
  - `[<keyspace> <tables>...]` — 키스페이스와 하나 이상의 테이블입니다.

---

#### drain

- **목적**: 노드를 드레인(drain)합니다(쓰기 수신을 중단하고 모든 테이블을 플러시). 일반적으로 노드 종료 또는 업그레이드 전에 사용합니다.
- **시놉시스**:
  ```
  nodetool [...common...] drain
  ```
- **옵션**: 공통 JMX 옵션 외에 별도의 옵션이 없습니다.

---

#### decommission

- **목적**: *현재 연결 중인 노드*를 디커미션(decommission)합니다(클러스터에서 우아하게 제거).
- **시놉시스**:
  ```
  nodetool [...common...] decommission [(-f | --force)]
  ```
- **옵션**:
  - `-f, --force` — 레플리카 수가 설정된 RF(Replication Factor) 미만으로 줄어들더라도 이 노드의 디커미션을 강제합니다.

---

#### removenode

- **목적**: 현재 노드 제거 작업의 상태를 표시하거나, 대기 중인 제거를 강제로 완료하거나, 제공된 ID의 노드를 제거합니다.
- **시놉시스**:
  ```
  nodetool [...common...] removenode [--] <status>|<force>|<ID>
  ```
- **옵션**:
  - `<status>|<force>|<ID>` — 현재 노드 제거 작업의 상태 표시(`status`), 대기 중인 제거의 강제 완료(`force`), 또는 제공된 호스트 ID 제거 중 하나입니다.

---

#### snapshot

- **목적**: 지정한 키스페이스 또는 지정한 테이블의 스냅샷(snapshot)을 생성합니다.
- **시놉시스**:
  ```
  nodetool [...common...] snapshot
      [(-cf <table> | --column-family <table> | --table <table>)]
      [(-kt <ktlist> | --kt-list <ktlist> | -kc <ktlist> | --kc.list <ktlist>)]
      [(-sf | --skip-flush)] [(-t <tag> | --tag <tag>)]
      [--ttl <ttl>] [--] [<keyspaces...>]
  ```
- **옵션**:
  - `-cf, --column-family, --table` — 테이블 이름입니다(이 옵션을 사용하려면 정확히 하나의 키스페이스를 지정해야 합니다).
  - `-kt, --kt-list, -kc, --kc.list` — 스냅샷을 생성할 `Keyspace.table` 목록입니다.
  - `-sf, --skip-flush` — 스냅샷 생성 전에 멤테이블(memtable)을 플러시하지 않습니다(이 경우 스냅샷에는 플러시되지 않은 데이터가 포함되지 않습니다).
  - `-t, --tag` — 스냅샷의 이름입니다.
  - `--ttl` — 생성된 스냅샷에 대한 TTL(Time To Live)을 지정합니다.
  - `[<keyspaces...>]` — 키스페이스 목록입니다(기본값: 모든 키스페이스).

---

#### tablestats

- **목적**: 테이블에 대한 통계(statistics)를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] tablestats
      [(-F <format> | --format <format>)] [(-H | --human-readable)]
      [-i] [(-l | --sstable-location-check)]
      [(-s <sort_key> | --sort <sort_key>)] [(-t <top> | --top <top>)]
      [--] [<keyspace.table>...]
  ```
- **옵션**:
  - `-F, --format` — 출력 형식입니다(`json`, `yaml`).
  - `-H, --human-readable` — 바이트(byte)를 사람이 읽기 쉬운 형태(KiB, MiB, GiB, TiB)로 표시합니다.
  - `-i` — 지정한 테이블 목록을 무시하고 나머지 테이블을 표시합니다.
  - `-l, --sstable-location-check` — SSTable이 올바른 위치(location)에 있는지 확인합니다.
  - `-s, --sort` — 지정한 정렬 키(sort key)로 테이블을 정렬합니다.
  - `-t, --top` — 정렬 키 기준 상위 K개 테이블만 표시합니다.
  - `[<keyspace.table>...]` — 테이블(또는 키스페이스) 이름 목록입니다.

---

#### ring

- **목적**: 토큰 링(token ring)에 대한 정보를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] ring [(-r | --resolve-ip)] [--] [<keyspace>]
  ```
- **옵션**:
  - `-r, --resolve-ip` — IP 대신 노드의 도메인 이름을 표시합니다.
  - `<keyspace>` — 정확한 소유권(ownership) 정보(토폴로지 인식, topology awareness)를 위한 키스페이스를 지정합니다.

---

#### gossipinfo

- **목적**: 클러스터의 가십(gossip) 정보를 표시합니다.
- **시놉시스**:
  ```
  nodetool [...common...] gossipinfo [(-r | --resolve-ip)]
  ```
- **옵션**:
  - `-r, --resolve-ip` — IP 대신 노드의 도메인 이름을 표시합니다.

---

#### netstats

- **목적**: 제공된 호스트(기본값: 연결 중인 노드)의 네트워크 정보를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] netstats [(-H | --human-readable)]
  ```
- **옵션**:
  - `-H, --human-readable` — 바이트를 사람이 읽기 쉬운 형태(KiB, MiB, GiB, TiB)로 표시합니다.

---

#### compactionstats

- **목적**: 컴팩션(compaction)에 대한 통계를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] compactionstats [(-H | --human-readable)] [(-V | --vtable)]
  ```
- **옵션**:
  - `-H, --human-readable` — 바이트를 사람이 읽기 쉬운 형태(KiB, MiB, GiB, TiB)로 표시합니다.
  - `-V, --vtable` — 가상 테이블(vtable) 출력과 일치하는 필드(field)를 표시합니다.

---

#### describecluster

- **목적**: 클러스터의 이름, 스니치(snitch), 파티셔너, 스키마 버전(schema version)을 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] describecluster
  ```
- **옵션**: 공통 JMX 옵션 외에 별도의 옵션이 없습니다.

---

#### move

- **목적**: 토큰 링에서 노드를 새 토큰(new token)으로 이동(move)합니다.
- **시놉시스**:
  ```
  nodetool [...common...] move [--] <new token>
  ```
- **옵션**:
  - `<new token>` — 새 토큰입니다.

---

#### scrub

- **목적**: 하나 이상의 테이블을 스크럽(scrub)합니다(SSTable을 다시 빌드(rebuild)). 손상된 SSTable을 복구하는 데 사용합니다.
- **시놉시스**:
  ```
  nodetool [...common...] scrub
      [(-j <jobs> | --jobs <jobs>)] [(-n | --no-validate)]
      [(-ns | --no-snapshot)] [(-r | --reinsert-overflowed-ttl)]
      [(-s | --skip-corrupted)] [--] [<keyspace> <tables>...]
  ```
- **옵션**:
  - `-j, --jobs` — 동시에 스크럽할 SSTable 수입니다. `0`으로 설정하면 사용 가능한 모든 컴팩션 스레드를 사용합니다.
  - `-n, --no-validate` — 컬럼 검증기(column validator)를 사용해 컬럼을 검증하지 않습니다.
  - `-ns, --no-snapshot` — `disableSnapshot`이 false인 경우 스크럽 대상 CF(Column Family)는 먼저 스냅샷됩니다(기본값 false).
  - `-r, --reinsert-overflowed-ttl` — CASSANDRA-14092의 영향으로 만료 날짜(expiration date)가 오버플로(overflow)된 행을, 지원되는 최대 만료 날짜인 `2038-01-19T03:14:06+00:00`으로 다시 작성(rewrite)합니다. 해당 행들은 영향받은 행의 컴팩션 중 생성되었을 수 있는 잠재적 툼스톤(tombstone)을 덮어쓰거나 대체하기 위해, 원래 타임스탬프에 1밀리초(millisecond)를 더한 값으로 다시 작성됩니다.
  - `-s, --skip-corrupted` — 카운터 테이블(counter table)을 스크럽할 때에도 손상된 파티션(corrupted partition)을 건너뜁니다(기본값 false).
  - `[<keyspace> <tables>...]` — 키스페이스와 하나 이상의 테이블입니다.

---

#### upgradesstables

- **목적**: 현재 버전이 아닌 SSTable(요청된 테이블의)을 다시 작성하여 현재 버전으로 업그레이드합니다.
- **시놉시스**:
  ```
  nodetool [...common...] upgradesstables
      [(-a | --include-all-sstables)] [(-j <jobs> | --jobs <jobs>)]
      [(-t <max_timestamp> | --max-timestamp <max_timestamp>)]
      [--] [<keyspace> <tables>...]
  ```
- **옵션**:
  - `-a, --include-all-sstables` — 이미 현재 버전인 SSTable을 포함하여 모든 SSTable을 대상으로 합니다.
  - `-j, --jobs` — 동시에 업그레이드할 SSTable 수입니다. `0`으로 설정하면 사용 가능한 모든 컴팩션 스레드를 사용합니다.
  - `-t, --max-timestamp` — 이 max-timestamp보다 이전(오래된)에 생성된 SSTable만 컴팩션합니다(즉, 로컬 생성 시각이 지정한 타임스탬프보다 오래된 것).
  - `[<keyspace> <tables>...]` — 키스페이스와 하나 이상의 테이블입니다.

---

#### verify

- **목적**: 하나 이상의 테이블을 검증(verify)합니다(데이터 체크섬(checksum) 확인).
- **시놉시스**:
  ```
  nodetool [...common...] verify
      [(-c | --check-version)] [(-d | --dfp)]
      [(-e | --extended-verify)] [(-f | --force)]
      [(-q | --quick)] [(-r | --rsc)] [(-t | --check-tokens)]
      [--] [<keyspace> <tables>...]
  ```
- **옵션**:
  - `-c, --check-version` — 모든 SSTable이 최신 버전인지도 확인합니다.
  - `-d, --dfp` — 손상된 SSTable이 발견되면 디스크 실패 정책(disk failure policy)을 호출합니다.
  - `-e, --extended-verify` — SSTable 체크섬 확인을 넘어 각 셀(cell) 데이터까지 검증합니다.
  - `-f, --force` — verify 도구의 비활성화를 무시합니다(주의 사항은 CASSANDRA-9947 참조).
  - `-q, --quick` — 빠른 검사를 수행합니다. 체크섬 검증을 위해 모든 데이터를 읽는 것을 피합니다.
  - `-r, --rsc` — 손상된 SSTable의 리페어 상태(repair status)를 변경(mutate)합니다.
  - `-t, --check-tokens` — SSTable의 모든 토큰이 이 노드 소유인지 검증합니다.
  - `[<keyspace> <tables>...]` — 키스페이스와 하나 이상의 테이블입니다.

---

#### clearsnapshot

- **목적**: 주어진 이름의 스냅샷을 주어진 키스페이스에서 제거합니다.
- **시놉시스**:
  ```
  nodetool [...common...] clearsnapshot
      [--all] [--older-than <older_than>]
      [--older-than-timestamp <older_than_timestamp>]
      [-t <snapshot_name>] [--] [<keyspaces>...]
  ```
- **옵션**:
  - `--all` — 모든 스냅샷을 제거합니다.
  - `--older-than <older_than>` — 지정한 기간(time period)보다 오래된 스냅샷을 제거합니다.
  - `--older-than-timestamp <older_than_timestamp>` — 지정한 타임스탬프(ISO 형식, 예: `'2022-12-03T10:15:30Z'`)보다 오래된 스냅샷을 제거합니다.
  - `-t <snapshot_name>` — 주어진 이름의 스냅샷을 제거합니다.
  - `[<keyspaces>...]` — 이 키스페이스들에서 스냅샷을 제거합니다.

---

#### listsnapshots

- **목적**: 디스크 사용량(size on disk)과 실제 크기(true size)를 포함하여 모든 스냅샷을 나열합니다. 실제 크기(true size)는 디스크에 백업되지 않은 모든 SSTable의 총 크기이며, 디스크 사용량(size on disk)은 스냅샷이 디스크에서 차지하는 총 크기입니다. `Total TrueDiskSpaceUsed`는 SSTable 중복 제거(deduplication)를 수행하지 않습니다.
- **시놉시스**:
  ```
  nodetool [...common...] listsnapshots [(-e | --ephemeral)] [(-nt | --no-ttl)]
  ```
- **옵션**:
  - `-e, --ephemeral` — 임시(ephemeral) 스냅샷을 포함합니다.
  - `-nt, --no-ttl` — TTL이 있는 스냅샷을 건너뜁니다.

---

#### tpstats

- **목적**: 스레드 풀(thread pool)의 사용 통계를 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] tpstats [(-F <format> | --format <format>)]
  ```
- **옵션**:
  - `-F, --format` — 출력 형식입니다(`json`, `yaml`).

---

#### getcompactionthroughput

- **목적**: 시스템의 컴팩션 처리량(throughput) 상한(MiB/s)을 반올림한 수로 출력합니다.
- **시놉시스**:
  ```
  nodetool [...common...] getcompactionthroughput [(-d | --precise-mib)]
  ```
- **옵션**:
  - `-d, --precise-mib` — 시스템의 컴팩션 처리량 상한(MiB/s)을 정밀한 수(double)로 출력합니다.

---

#### setcompactionthroughput

- **목적**: 시스템의 컴팩션 처리량 상한(MiB/s)을 설정하거나, `0`을 지정하여 스로틀링(throttling)을 비활성화합니다.
- **시놉시스**:
  ```
  nodetool [...common...] setcompactionthroughput [--] <value_in_mb>
  ```
- **옵션**:
  - `<value_in_mb>` — MiB 단위의 값입니다. `0`은 스로틀링 비활성화를 의미합니다.

---

#### rebuild

- **목적**: 다른 노드에서 스트리밍하여 데이터를 다시 빌드(rebuild)합니다(부트스트랩(bootstrap)과 유사).
- **시놉시스**:
  ```
  nodetool [...common...] rebuild
      [--exclude-local-dc]
      [(-ks <specific_keyspace> | --keyspace <specific_keyspace>)]
      [(-s <specific_sources> | --sources <specific_sources>)]
      [(-ts <specific_tokens> | --tokens <specific_tokens>)]
      [--] <src-dc-name>
  ```
- **옵션**:
  - `--exclude-local-dc` — 스트리밍 소스(source)에서 로컬 데이터센터의 노드를 제외합니다.
  - `-ks, --keyspace` — 특정 키스페이스를 다시 빌드합니다.
  - `-s, --sources` — `-ts`를 사용할 때 이 노드가 스트리밍해 올 호스트를 지정합니다. 여러 호스트는 쉼표로 구분합니다(예: `127.0.0.1,127.0.0.2,...`).
  - `-ts, --tokens` — 특정 토큰 범위를 다시 빌드합니다. 형식은 `(start_token_1,end_token_1],(start_token_2,end_token_2],...(start_token_n,end_token_n]`입니다.
  - `<src-dc-name>` — 스트리밍 소스를 선택할 데이터센터 이름입니다. 기본적으로 임의의 데이터센터를 선택합니다(`--exclude-local-dc` 설정 시 로컬 데이터센터 제외).

---

## SSTable 도구

SSTable 도구는 디스크에 저장된 SSTable 파일을 직접 다루기 위한 오프라인(offline) 유틸리티 모음입니다.

> **중요**: 이 도구들을 실행하기 전에 반드시 Cassandra를 중지(stop)해야 하며, 그렇지 않으면 예기치 않은 결과(unexpected result)가 발생합니다. (단, `sstablepartitions`는 Cassandra가 실행 중이지 않아도 됩니다.)

여러 도구가 공통적으로 `--debug`(스택 트레이스(stack trace) 표시), `-h, --help`(도움말 표시), `-v, --verbose`(상세 출력) 옵션을 공유합니다.

---

### sstabledump

- **기능**: 주어진 SSTable의 내용을 JSON 형식으로 표준 출력(standard output)에 덤프(dump)합니다.
- **사용법**:
  ```
  sstabledump <options> <sstable file path>
  ```
- **옵션**:
  - `-d` — 줄(line)마다 내부 CQL 행(row) 표현(internal CQL row representation)을 표시합니다.
  - `-e` — 파티션 키만 표시합니다.
  - `-k <arg>` — 특정 파티션 키로 결과를 필터링합니다.
  - `-x <arg>` — 하나 이상의 파티션 키를 제외합니다.
  - `-t` — ISO8601 형식 대신 원시(raw) 타임스탬프를 출력합니다.
  - `-l` — 각 행을 별도의 JSON 객체로 형식화합니다.
- **예시**:
  - 전체 테이블 덤프: `sstabledump /path/to/mc-1-big-Data.db`
  - 단일 파티션 키: `sstabledump /path/to/mc-1-big-Data.db -k <partition-key-value>`
  - 특정 키 제외: `sstabledump /path/to/mc-1-big-Data.db -x key1 key2`
  - 키만 표시: `sstabledump /path/to/mc-1-big-Data.db -e`
  - 줄 단위 JSON: `sstabledump /path/to/mc-1-big-Data.db -l`

  모든 출력은 JSON 형식이므로 파일로 리다이렉션(redirection)하여 추가 분석에 활용할 수 있습니다.

---

### sstableloader

- **기능**: 디렉터리의 SSTable을 설정된 클러스터로 일괄 적재(bulk-load)합니다. 경로의 상위 디렉터리(parent directory)가 대상 키스페이스와 테이블 이름이 됩니다(예: `/path/to/keyspace1/standard1/`의 파일은 `keyspace1.standard1`로 적재됩니다). 원본 파일을 정리하지 않고 새 SSTable을 생성합니다.
- **사용법**:
  ```
  sstableloader <options> <dir_path>
  ```
- **옵션**:
  - `-d, --nodes <initial hosts>` — **필수**. 링 정보를 얻기 위한 초기 호스트(initial host)들을 쉼표로 구분합니다.
  - `-u, --username <username>` — Cassandra 인증 사용자 이름입니다.
  - `-pw, --password <password>` — Cassandra 인증 비밀번호입니다.
  - `-p, --port <native transport port>` — 네이티브 연결 포트입니다(기본값: 9042).
  - `-sp, --storage-port <storage port>` — 노드 간(internode) 통신 포트입니다(기본값: 7000).
  - `-ssp, --ssl-storage-port <ssl storage port>` — TLS 노드 간 통신 포트입니다(기본값: 7001).
  - `--no-progress` — 진행 상황(progress) 표시를 억제합니다.
  - `-t, --throttle <throttle>` — (사용 중단(Deprecated)) Mbit 단위 스로틀 속도입니다(기본값: 무제한).
  - `--throttle-mib <throttle-mib>` — MiB/s 단위 스로틀 속도입니다(기본값: 무제한).
  - `-idct, --inter-dc-throttle <inter-dc-throttle>` — (사용 중단) Mbit 단위 데이터센터 간(inter-datacenter) 스로틀입니다(기본값: 무제한).
  - `--inter-dc-throttle-mib <inter-dc-throttle-mib>` — MiB/s 단위 데이터센터 간 스로틀입니다(기본값: 무제한).
  - `--entire-sstable-throttle-mib <throttle-mib>` — MiB/s 단위 전체 SSTable(entire SSTable) 스로틀입니다(기본값: 무제한).
  - `--entire-sstable-inter-dc-throttle-mib <inter-dc-throttle-mib>` — MiB/s 단위 전체 SSTable 데이터센터 간 스로틀입니다(기본값: 무제한).
  - `-cph, --connections-per-host <connectionsPerHost>` — 호스트당 동시 연결(concurrent connection) 수입니다.
  - `-i, --ignore <NODES>` — 스트리밍에서 제외할 노드 목록(쉼표 구분)입니다.
  - `-alg, --ssl-alg <ALGORITHM>` — 클라이언트 SSL 알고리즘입니다(기본값: SunX509).
  - `-ciphers, --ssl-ciphers <CIPHER-SUITES>` — SSL 암호화 스위트(cipher suite) 목록(쉼표 구분)입니다.
  - `-ks, --keystore <KEYSTORE>` — 클라이언트 SSL 키스토어(keystore) 전체 경로입니다.
  - `-kspw, --keystore-password <KEYSTORE-PASSWORD>` — 키스토어 비밀번호입니다.
  - `-st, --store-type <STORE-TYPE>` — 스토어 타입(store type)입니다.
  - `-ts, --truststore <TRUSTSTORE>` — 클라이언트 SSL 트러스트스토어(truststore) 전체 경로입니다.
  - `-tspw, --truststore-password <TRUSTSTORE-PASSWORD>` — 트러스트스토어 비밀번호입니다.
  - `-prtcl, --ssl-protocol <PROTOCOL>` — SSL 연결 프로토콜입니다(기본값: TLS).
  - `-ap, --auth-provider <auth provider>` — 사용자 정의 `AuthProvider` 클래스명입니다.
  - `-f, --conf-path <path to config file>` — 스트리밍 및 암호화 옵션을 위한 `cassandra.yaml` 파일 경로입니다(`stream_throughput_outbound`, `server_encryption_options`, `client_encryption_options`만 읽으며, CLI 옵션이 YAML 값을 덮어씁니다).
  - `-v, --verbose` — 상세 출력입니다.
  - `-h, --help` — 도움말 메시지를 표시합니다.
- **예시**: 스냅샷 SSTable 적재, SSL 클러스터 설정, 진행 상황 억제, 상세 로깅, 스로틀링, 호스트당 다중 연결을 통한 속도 향상 등.

---

### sstablemetadata

- **기능**: 관련된 `Statistics.db`, `Summary.db` 파일에서 SSTable에 대한 정보를 표준 출력에 출력합니다. 실행 전 Cassandra를 중지해야 합니다(스크립트가 이를 확인하지는 않습니다).
- **사용법**:
  ```
  sstablemetadata <options> <sstable filename(s)>
  ```
- **옵션**:
  - `--colors` — 출력에 ANSI 컬러 시퀀스(color sequence)를 적용합니다.
  - `--gc_grace_seconds <arg>` — 삭제 가능한(droppable) 툼스톤 계산을 위한 `gc_grace_seconds` 값을 지정합니다.
  - `--help` — 도움말 정보를 표시합니다.
  - `--scan` — 추가 통계를 위해 전체 SSTable 스캔(scan)을 수행합니다(3.0 이상 SSTable에서만, 기본값 false).
  - `--timestamp_unit <arg>` — 셀 타임스탬프의 시간 단위(time unit)를 정의합니다.
  - `--unicode` — 히스토그램(histogram)과 진행 막대(progress bar)를 유니코드 문자로 그립니다.
- **예시**:
  - `sstablemetadata /var/lib/cassandra/data/keyspace1/standard1-.../mc-1-big-Data.db`
  - `sstablemetadata --gc_grace_seconds 100 /path/to/sstable-Data.db`
  - `sstablemetadata -s /path/to/sstable-Data.db` (전체 테이블 스캔)

---

### sstablescrub

- **기능**: 손상된(broken) SSTable을 복구합니다. 스크럽 과정에서 SSTable을 다시 작성하며 손상된 행은 건너뜁니다. 먼저 Cassandra를 중지해야 합니다.
- **사용법**:
  ```
  sstablescrub <options> <keyspace> <table>
  ```
- **옵션**:
  - `--debug` — 스택 트레이스를 표시합니다.
  - `-h, --help` — 도움말 메시지를 표시합니다.
  - `-m, --manifest-check` — 실제 SSTable을 스크럽하지 않고 레벨드 매니페스트(leveled manifest)만 검사하고 복구합니다.
  - `-n, --no-validate` — 컬럼 검증기를 사용해 컬럼을 검증하지 않습니다.
  - `-r, --reinsert-overflowed-ttl` — CASSANDRA-14092의 영향으로 만료 날짜가 오버플로된 행을 지원되는 최대 만료 날짜인 `2038-01-19T03:14:06+00:00`으로 다시 작성합니다.
  - `-s, --skip-corrupted` — 카운터 테이블에서 손상된 행을 건너뜁니다.
  - `-v, --verbose` — 상세 출력입니다.
- **예시**: `sstablescrub keyspace1 standard1`; `--no-validate keyspace1 standard1`; `--skip-corrupted keyspace1 counter1`; `--reinsert-overflowed-ttl keyspace1 counter1`

---

### sstableverify

- **기능**: 제공된 테이블에 대해 SSTable의 오류 또는 손상 여부를 검사합니다. 실행 전 Cassandra를 중지해야 합니다. 기본 검증(basic verification)은 표준 검사를 수행하고, 확장 검증(extended verification)은 개별 값(value)의 오류 또는 손상까지 검증합니다(추가 시간과 리소스가 필요합니다).
- **사용법**:
  ```
  sstableverify <options> <keyspace> <table>
  ```
- **옵션**:
  - `--debug` — 스택 트레이스를 표시합니다.
  - `-e, --extended` — 확장 검증을 수행합니다.
  - `-h, --help` — 도움말 메시지를 표시합니다.
  - `-v, --verbose` — 상세 출력입니다.
  - `-f, --force` — 도구 사용을 허용합니다(위험성은 CASSANDRA-17017 참조).
- **예시**: 기본 검증, 확장 검증, 손상 파일 탐지에 대한 출력 예시가 제공됩니다.

---

### sstablesplit

- **기능**: 큰 SSTable 파일을 더 작은 파일로 분할(split)하여 디스크 공간을 회수합니다. 일종의 안티컴팩션(anticompaction)으로 볼 수 있습니다. 먼저 Cassandra를 중지해야 합니다(스크립트가 이를 확인하지는 않습니다).
- **사용법**:
  ```
  sstablesplit <options> <filename>
  ```
- **옵션**:
  - `--debug` — 스택 트레이스를 표시합니다.
  - `-h, --help` — 도움말 메시지를 표시합니다.
  - `--no-snapshot` — 분할 전 SSTable의 스냅샷 생성을 건너뜁니다.
  - `-s, --size <size>` — 출력 SSTable의 최대 크기(MB)입니다(기본값: 50).
- **예시**: 기본 분할; 와일드카드(wildcard)를 이용한 분할; `--size 1`; `--size 1 --no-snapshot`

---

### sstableupgrade

- **기능**: 주어진 테이블(또는 스냅샷)의 SSTable을 현재 Cassandra 버전으로 업그레이드합니다. 일반적으로 버전 업그레이드 후에 사용합니다. 이전 버전으로 다운그레이드(downgrade)할 수도 있습니다. 실행 중인 Cassandra보다 오래된 메이저 버전의 스냅샷을 복원하기 전에 필요합니다. 먼저 Cassandra를 중지해야 합니다.
- **사용법**:
  ```
  sstableupgrade <options> <keyspace> <table> [snapshot_name]
  ```
- **옵션**:
  - `--debug` — 스택 트레이스를 표시합니다.
  - `-h, --help` — 도움말 메시지를 표시합니다.
  - `-k, --keep-source` — 원본(source) SSTable을 삭제하지 않습니다.
- **예시**: 표준 업그레이드(원본 삭제); `-k`를 사용한 업그레이드(원본 보존); 스냅샷 업그레이드(세 번째 인자로 스냅샷 이름 지정).

---

### sstableutil

- **기능**: 제공된 테이블의 SSTable 파일을 나열합니다. 먼저 Cassandra를 중지해야 합니다(스크립트가 이를 확인하지는 않습니다).
- **사용법**:
  ```
  sstableutil <options> <keyspace> <table>
  ```
- **옵션**:
  - `-c, --cleanup` — 미처리(outstanding) 트랜잭션(transaction)을 정리합니다.
  - `-d, --debug` — 스택 트레이스를 표시합니다.
  - `-h, --help` — 도움말 메시지를 표시합니다.
  - `-o, --oplog` — 작업 로그(operation log)를 포함합니다.
  - `-t, --type <arg>` — 파일 타입을 지정합니다: `all`(최종(final) 또는 임시(temporary)), `tmp`(임시만), `final`(최종만).
  - `-v, --verbose` — 상세 출력입니다.
- **예시**: `sstableutil keyspace eventlog` — 관련된 모든 파일(CRC, Data, Digest, Filter, Index, Statistics, Summary, TOC)을 전체 경로와 함께 나열합니다.

---

### sstablerepairedset

- **기능**: 주어진 SSTable 집합에 `repairedAt` 상태를 설정하여, 리페어되지 않은(un-repaired) SSTable에 대해서만 리페어를 수행할 수 있게 합니다(리페어에 오랜 시간이 걸리는 대용량 데이터셋에 유용합니다). 먼저 Cassandra를 중지해야 합니다.
- **사용법**:
  ```
  sstablerepairedset --really-set <options> [-f <sstable-list> | <sstables>]
  ```
- **옵션**:
  - `--really-set` — 실제로 상태를 설정하기 위한 필수 플래그입니다.
  - `--is-repaired` — `repairedAt` 상태를 마지막 수정 시각(last modified time)으로 설정합니다.
  - `--is-unrepaired` — `repairedAt` 상태를 `0`으로 설정합니다.
  - `-f` — SSTable 목록이 담긴 파일을 입력으로 사용합니다.
- **예시**: `find ... | xargs ... --really-set --is-unrepaired`로 미리페어 표시; `nodetool repair` 실행 후 리페어 표시; `sstablemetadata | grep "Repaired at"`로 확인.

---

### sstablelevelreset

- **기능**: `LeveledCompactionStrategy`가 설정된 경우, 주어진 SSTable 집합의 레벨(level)을 0으로 초기화(reset)합니다. 최소 SSTable 크기를 조정하고 새 설정으로 컴팩션을 다시 시작해야 할 때 유용합니다. 먼저 Cassandra를 중지해야 합니다(스크립트가 이를 확인하지는 않습니다).
- **사용법**:
  ```
  sstablelevelreset --really-reset <keyspace> <table>
  ```
- **옵션**:
  - `--really-reset` — (필수) 침습적(intrusive)인 명령이 실수로 실행되지 않도록 보장하는 필수 플래그입니다.
- **출력 메시지 예시**: `"Changing level from 1 to 0 on ..."`; `"Skipped ... since it is already on level 0"`; `"ColumnFamily not found: keyspace/evenlog"`; `"Found no sstables, did you give the correct keyspace/table?"`

---

### sstableofflinerelevel

- **기능**: `LeveledCompactionStrategy`를 사용할 때, 최근 부트스트랩된 노드에서 SSTable이 L0에 묶여 컴팩션이 따라잡지 못하는 경우가 있습니다. 이 도구는 SSTable을 마지막 토큰(last token) 기준으로 정렬하고 위에서 아래로(top-down) 레벨을 재구성하여 겹침(overlap) 없이 가능한 한 가장 높은 레벨로 끌어올립니다. 먼저 Cassandra를 중지해야 합니다(스크립트가 이를 확인하지는 않습니다). 잘못된 키스페이스/테이블을 지정하면 `IllegalArgumentException`을 발생시킵니다.
- **사용법**:
  ```
  sstableofflinerelevel [--dry-run] <keyspace> <table>
  ```
- **옵션**:
  - `--dry-run` — 실제로 아무것도 수정하지 않고 현재 레벨 분포(level distribution)와 변경 후 예측 레벨을 표시합니다.
- **예시**: `sstableofflinerelevel --dry-run keyspace eventlog`; `sstableofflinerelevel keyspace eventlog`

---

### sstableexpiredblockers

- **기능**: 다른 SSTable이 드롭(drop)되는 것을 막고 있는 모든 SSTable을 나열합니다(만료된 SSTable의 가장 최근 툼스톤보다 오래된 데이터를 가지고 있어 막는 경우). 만료된 툼스톤만 가진 만료 SSTable은 컴팩션 중에 폐기되어야 하지만, 다른 SSTable에 더 새로운 데이터가 존재하면 제거할 수 없습니다. 먼저 Cassandra를 중지해야 합니다.
- **사용법**:
  ```
  sstableexpiredblockers <keyspace> <table>
  ```
  (두 인자 모두 필수)
- **옵션**: 문서화된 옵션이 없습니다.
- **출력**: 막는 SSTable이 없으면 아무것도 출력하지 않으며, 있을 경우 `<sstable> blocks <#> expired sstables from getting dropped`를 출력하고 그 뒤에 차단된 SSTable 목록(파일 경로, 최소/최대 타임스탬프, 최대 로컬 삭제 시각 포함)을 표시합니다.
- **예시**: `sstableexpiredblockers keyspace1 standard1`

---

### sstablepartitions

- **기능**: SSTable의 큰 파티션(large partition)을 식별하고 파티션 크기(바이트), 행 수(row count), 셀 수(cell count), 툼스톤 수(tombstone count)를 출력합니다. 하나 이상의 SSTable 파일/디렉터리를 각각 분석합니다. 임계값(threshold)을 지정하면 임계값을 초과하는 파티션만 표시됩니다. 추정 백분위수(estimated percentile)와 정확한 최소/최대/개수를 포함한 요약 통계를 제공합니다. 기본적으로 사람이 읽기 쉬운 형식이며, `--csv`로 기계 판독(machine-readable) 형식을 출력할 수 있습니다. Cassandra가 실행 중일 필요는 없습니다.
- **사용법**:
  ```
  sstablepartitions <options> <sstable files or directories>
  ```
- **옵션**:
  - `-t, --min-size <arg>` — 파티션 크기 임계값(바이트 또는 단위 표기, 예: 10KiB, 20MiB, 30GiB)입니다.
  - `-w, --min-rows <arg>` — 파티션 행 수 임계값입니다.
  - `-c, --min-cells <arg>` — 파티션 셀 수 임계값입니다.
  - `-o, --min-tombstones <arg>` — 파티션 툼스톤 수 임계값입니다.
  - `-k, --key <arg>` — 전체를 스캔하는 대신 포함할 특정 파티션 키입니다.
  - `-x, --exclude-key <arg>` — 제외할 파티션 키입니다.
  - `-r, --recursive` — SSTable을 재귀적(recursive)으로 스캔합니다.
  - `-b, --backups` — 디렉터리 스캔 시 백업(backup) 데이터를 포함합니다.
  - `-s, --snapshots` — 디렉터리 스캔 시 스냅샷을 포함합니다.
  - `-u, --current-timestamp <arg>` — TTL 만료 계산을 위한 에폭(epoch) 이후 초 단위 타임스탬프입니다.
  - `-y, --partitions-only` — 행/셀/툼스톤 세부 정보를 제외하고 간단한 파티션 정보만 표시합니다.
  - `-m, --csv` — CSV 기계 판독 형식으로 출력합니다.
- **예시**: 단일 SSTable; 여러 SSTable이 있는 디렉터리; 크기 기준 필터(100MiB 이상); 툼스톤 수 기준 필터(1000개 이상); 결합 필터를 사용한 CSV 출력.

---

## cassandra-stress

### 개요

`cassandra-stress` 도구는 Cassandra 클러스터를 대상으로 하는 벤치마킹(benchmarking) 및 부하 테스트(load-testing) 유틸리티입니다. 임의의 CQL 테이블과 쿼리를 테스트할 수 있어, 사용자가 자신의 데이터 모델(data model)을 직접 벤치마크할 수 있습니다. 특히 User 모드(user mode)는 사용자 자신의 스키마를 검증하는 데 중점을 둡니다.

`cassandra-stress`로 할 수 있는 일:

- 스키마가 어떻게 동작하는지 빠르게 파악합니다.
- 데이터베이스가 어떻게 확장(scale)되는지 이해합니다.
- 데이터 모델과 설정을 최적화합니다.
- 운영 용량(production capacity)을 산정합니다.

### 명령 구문(Command Syntax)

기본 형식은 다음과 같습니다:

```
cassandra-stress <command> [options]
```

특정 명령이나 옵션에 대한 정보는 다음으로 확인할 수 있습니다:

```
cassandra-stress help <command|option>
```

### 명령(Commands)

- **read**: 클러스터에 대해 동시 읽기(concurrent read) 작업을 수행합니다(미리 채워진(pre-populated) 클러스터가 필요합니다).
- **write**: 클러스터에 대해 동시 쓰기(concurrent write) 작업을 수행합니다.
- **mixed**: 설정 가능한 비율(ratio)과 분포(distribution)로 읽기/쓰기를 교차(interleave) 수행합니다.
- **counter_write**: 동시 카운터 컬럼(counter column) 업데이트를 수행합니다.
- **counter_read**: 동시 카운터 읽기를 수행합니다(사전에 `counter_write` 테스트가 필요합니다).
- **user**: 사용자가 제공한 쿼리(user-provided query)를 설정 가능한 분포로 교차 수행합니다.
- **help**: 명령 또는 옵션에 대한 도움말을 제공합니다.
- **print**: 분포 정의(distribution definition)를 검사하여 출력합니다.
- **legacy**: 레거시(legacy) 호환 모드를 지원합니다.
- **version**: 도구 버전을 출력합니다.

### 주요 옵션 그룹(Primary Options)

도구는 다음과 같은 주요 옵션 그룹을 받습니다.

- **-pop**: 모집단 분포(population distribution)와 파티션 방문 순서(partition visit sequencing)를 지정합니다.
- **-insert**: 배치(batching) 및 파티션 업데이트 방식을 지정합니다.
- **-col**: 컬럼 명세(크기(size), 개수(count), 데이터 생성, 이름)를 지정합니다.
- **-rate**: 스레딩(threading), 속도 제한(rate limiting) 또는 자동 설정(automatic configuration)을 지정합니다.
- **-mode**: Thrift 또는 CQL 프로토콜을 선택합니다.
- **-errors**: 오류 처리(error handling) 전략을 지정합니다.
- **-sample**: 지연 시간(latency) 측정 샘플링(sampling)을 지정합니다.
- **-schema**: 복제(replication), 압축(compression), 컴팩션 설정을 지정합니다.
- **-node**: 대상 노드(target node)를 지정합니다.
- **-log**: 진행 상황 로깅(progress logging)을 설정합니다.
- **-transport**: 사용자 정의 전송 팩토리(transport factory) 구현을 지정합니다.
- **-port**: Cassandra 연결 포트를 지정합니다.
- **-sendto**: 작업을 전달할 대상(원격 데몬 모드)을 지정합니다.
- **-graph**: 메트릭(metric) 시각화를 위한 그래프를 생성합니다.
- **-tokenrange**: 토큰 범위(token range) 설정을 지정합니다.

### User 모드와 YAML 프로파일

User 모드를 사용하면 자신의 스키마에 부하 테스트를 수행할 수 있어 장기적으로 시간을 절약할 수 있습니다. 사용자의 스키마로 부하 테스트를 수행하여 애플리케이션이 확장 가능한지 확인할 수 있습니다. User 모드는 YAML 파일로 프로파일(profile)을 정의합니다.

YAML 설정 파일은 다음을 정의합니다:

```yaml
specname: [식별자(identifier)]
keyspace: [이름]
keyspace_definition: [CREATE KEYSPACE CQL 문]
table: [이름]
table_definition: [CREATE TABLE CQL 문]
columnspec: [컬럼 명세(column specifications)]
insert: [배치 설정(batch configuration)]
queries: [쿼리 정의(query definitions)]
```

#### 컬럼 명세(Column Specifications)

`EXP()`, `EXTREME()`, `GAUSSIAN()`, `UNIFORM()`, `FIXED()`와 같은 분포 타입을 지원하며, 텍스트/블롭(text/blob) 타입에 대해 최솟값/최댓값(min/max) 범위를 지정할 수 있습니다.

#### Insert 설정(Insert Configuration)

파티션 배치 전략(partition batching strategy), 행 선택 비율(row selection ratio), 배치 타입(`UNLOGGED`/`LOGGED`)을 정의합니다.

#### 쿼리(Queries)

필드 바인딩(field binding) 명세를 포함한 사용자 정의 CQL 문을 정의합니다.

### 예제 실행(Example Invocations)

User 모드 예시:

```
cassandra-stress user profile=./example.yaml duration=1m "ops(insert=1,latest_event=1,events=1)" truncate=once
```

### 고급 기능(Advanced Features)

문서에서는 경량 트랜잭션(lightweight transaction) 지원, 다중 YAML 프로파일(multiple YAML profiles), 전송 옵션을 통한 SSL 설정, 테스트 실행 간 성능 시각화를 위한 그래프 생성 등을 지원한다고 설명합니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [cqlsh: the CQL shell](https://cassandra.apache.org/doc/latest/cassandra/managing/tools/cqlsh.html)
- [nodetool](https://cassandra.apache.org/doc/latest/cassandra/managing/tools/nodetool/nodetool.html)
- [SSTable Tools](https://cassandra.apache.org/doc/latest/cassandra/managing/tools/sstable/index.html)
- [cassandra-stress](https://cassandra.apache.org/doc/4.1/cassandra/tools/cassandra_stress.html)
