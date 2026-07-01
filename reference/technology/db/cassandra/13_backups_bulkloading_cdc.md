# Cassandra 백업, 벌크 로딩, CDC

> 원본: https://cassandra.apache.org/doc/latest/cassandra/managing/operating/

---

## 목차

1. [백업(Backups)](#1-백업backups)
   - [1.1 소개](#11-소개)
   - [1.2 백업의 종류](#12-백업의-종류)
   - [1.3 데이터 디렉터리 구조](#13-데이터-디렉터리-구조)
   - [1.4 예제 테이블 준비](#14-예제-테이블-준비)
   - [1.5 스냅샷(Snapshots)](#15-스냅샷snapshots)
   - [1.6 증분 백업(Incremental Backups)](#16-증분-백업incremental-backups)
   - [1.7 스냅샷과 증분 백업으로부터 복원](#17-스냅샷과-증분-백업으로부터-복원)
2. [벌크 로딩(Bulk Loading)](#2-벌크-로딩bulk-loading)
   - [2.1 개요](#21-개요)
   - [2.2 주요 도구](#22-주요-도구)
   - [2.3 sstableloader](#23-sstableloader)
   - [2.4 nodetool import](#24-nodetool-import)
   - [2.5 sstableloader 실습 워크플로](#25-sstableloader-실습-워크플로)
   - [2.6 nodetool import 예시](#26-nodetool-import-예시)
   - [2.7 외부 데이터 벌크 로딩](#27-외부-데이터-벌크-로딩)
   - [2.8 주의사항](#28-주의사항)
3. [변경 데이터 캡처(Change Data Capture, CDC)](#3-변경-데이터-캡처change-data-capture-cdc)
   - [3.1 개요](#31-개요)
   - [3.2 CDC 동작 방식](#32-cdc-동작-방식)
   - [3.3 설정(Configuration)](#33-설정configuration)
   - [3.4 테이블에 CDC 활성화/비활성화](#34-테이블에-cdc-활성화비활성화)
   - [3.5 CommitLogSegment 읽기](#35-commitlogsegment-읽기)
   - [3.6 경고](#36-경고)
4. [참고 자료](#4-참고-자료)

---

## 1. 백업(Backups)

### 1.1 소개

Apache Cassandra는 데이터를 불변(immutable) SSTable 파일에 저장합니다. Apache Cassandra 데이터베이스에서 백업(backup)이란 SSTable 파일로 저장되어 있는 데이터베이스 데이터의 백업 복사본을 의미합니다.

백업의 핵심 목적은 다음과 같습니다.

- **내구성(durability)** — 데이터 복사본을 보관하여 내구성을 확보합니다.
- **테이블 복원** — 노드 장애, 파티션 장애, 네트워크 장애로 인해 테이블 데이터가 손실되었을 때 복원합니다.
- **이식성(portability)** — SSTable 파일을 다른 머신으로 전송할 수 있습니다.

### 1.2 백업의 종류

Cassandra는 두 가지 백업 전략을 지원합니다.

#### 스냅샷(Snapshots)

스냅샷(snapshot)은 특정 시점에 하드 링크(hard link)를 통해 생성한 테이블의 SSTable 파일 복사본입니다. 테이블의 DDL도 함께 보존됩니다. 스냅샷은 수동으로 생성하거나 자동으로 생성할 수 있습니다.

- `snapshot_before_compaction` 설정(cassandra.yaml)은 컴팩션(compaction) 작업 전에 자동으로 스냅샷을 생성할지를 제어합니다(기본값: `false`).
- `auto_snapshot` 설정은 키스페이스 truncate(잘라내기) 또는 테이블 drop(삭제) 전에 자동으로 스냅샷을 생성할지를 활성화합니다(기본값: `true`).
- 기본적으로 Cassandra는 truncate 작업 중에 자동 스냅샷이 완료되기를 60초 동안 기다립니다.

#### 증분 백업(Incremental Backups)

증분 백업(incremental backup)은 멤테이블(memtable)이 SSTable로서 디스크에 플러시(flush)될 때 하드 링크로 생성되는 테이블 SSTable 파일의 복사본입니다. 증분 백업은 일반적으로 스냅샷과 함께 사용하여 백업 소요 시간과 저장 공간 사용량을 최소화합니다.

증분 백업은 기본적으로 비활성화되어 있으며, cassandra.yaml의 `incremental_backups` 설정을 통해 또는 nodetool 명령을 통해 명시적으로 활성화해야 합니다. 활성화되면, Cassandra는 로컬에서 플러시되거나 스트리밍되는 각 SSTable에 대해 해당 키스페이스 데이터 디렉터리 내의 `backups/` 하위 디렉터리에 하드 링크를 생성합니다.

### 1.3 데이터 디렉터리 구조

Cassandra의 디렉터리 구조는 데이터를 키스페이스 디렉터리(keyspace directory), 테이블 디렉터리(table directory), 그리고 백업/스냅샷 하위 디렉터리(backup/snapshot subdirectory)로 구성합니다. 각 테이블 디렉터리 안에는 각각의 백업 유형을 저장하기 위해 별도의 `backups/` 디렉터리와 `snapshots/` 디렉터리가 존재합니다.

### 1.4 예제 테이블 준비

아래 예제는 두 개의 키스페이스(`cqlkeyspace`와 `catalogkeyspace`)와 샘플 테이블을 사용합니다.

먼저 `cqlkeyspace` 키스페이스와 두 개의 테이블 `t`, `t2`를 생성하고 데이터를 삽입합니다.

```sql
CREATE KEYSPACE cqlkeyspace
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

USE cqlkeyspace;
CREATE TABLE t (
   id int,
   k int,
   v text,
   PRIMARY KEY (id)
);
CREATE TABLE t2 (
   id int,
   k int,
   v text,
   PRIMARY KEY (id)
);

INSERT INTO t (id, k, v) VALUES (0, 0, 'val0');
INSERT INTO t (id, k, v) VALUES (1, 1, 'val1');

INSERT INTO t2 (id, k, v) VALUES (0, 0, 'val0');
INSERT INTO t2 (id, k, v) VALUES (1, 1, 'val1');
INSERT INTO t2 (id, k, v) VALUES (2, 2, 'val2');
```

쿼리 결과는 다음과 같습니다.

```
id | k | v
----+---+------
 1 | 1 | val1
 0 | 0 | val0

 (2 rows)

id | k | v
----+---+------
 1 | 1 | val1
 0 | 0 | val0
 2 | 2 | val2

 (3 rows)
```

다음으로 두 번째 키스페이스 `catalogkeyspace`와 테이블 `journal`, `magazine`을 생성하고 데이터를 삽입합니다.

```sql
CREATE KEYSPACE catalogkeyspace
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 3};

USE catalogkeyspace;
CREATE TABLE journal (
   id int,
   name text,
   publisher text,
   PRIMARY KEY (id)
);

CREATE TABLE magazine (
   id int,
   name text,
   publisher text,
   PRIMARY KEY (id)
);

INSERT INTO journal (id, name, publisher) VALUES (0, 'Apache Cassandra Magazine', 'Apache Cassandra');
INSERT INTO journal (id, name, publisher) VALUES (1, 'Couchbase Magazine', 'Couchbase');

INSERT INTO magazine (id, name, publisher) VALUES (0, 'Apache Cassandra Magazine', 'Apache Cassandra');
INSERT INTO magazine (id, name, publisher) VALUES (1, 'Couchbase Magazine', 'Couchbase');
```

### 1.5 스냅샷(Snapshots)

#### 1.5.1 스냅샷 명령 구문

스냅샷을 생성하는 명령은 `nodetool snapshot`입니다. 전체 사용법 정보는 다음과 같습니다.

```
NAME
       nodetool snapshot - Take a snapshot of specified keyspaces or a snapshot
       of the specified table

SYNOPSIS
       nodetool [(-h <host> | --host <host>)] [(-p <port> | --port <port>)]
               [(-pp | --print-port)] [(-pw <password> | --password <password>)]
               [(-pwf <passwordFilePath> | --password-file <passwordFilePath>)]
               [(-u <username> | --username <username>)] snapshot
               [(-cf <table> | --column-family <table> | --table <table>)]
               [(-kt <ktlist> | --kt-list <ktlist> | -kc <ktlist> | --kc.list <ktlist>)]
               [(-sf | --skip-flush)] [(-t <tag> | --tag <tag>)] [--] [<keyspaces...>]

OPTIONS
       -cf <table>, --column-family <table>, --table <table>
           The table name (you must specify one and only one keyspace for using
           this option)

       -h <host>, --host <host>
           Node hostname or ip address

       -kt <ktlist>, --kt-list <ktlist>, -kc <ktlist>, --kc.list <ktlist>
           The list of Keyspace.table to take snapshot.(you must not specify
           only keyspace)

       -p <port>, --port <port>
           Remote jmx agent port number

       -pp, --print-port
           Operate in 4.0 mode with hosts disambiguated by port number

       -pw <password>, --password <password>
           Remote jmx agent password

       -pwf <passwordFilePath>, --password-file <passwordFilePath>
           Path to the JMX password file

       -sf, --skip-flush
           Do not flush memtables before snapshotting (snapshot will not
           contain unflushed data)

       -t <tag>, --tag <tag>
           The name of the snapshot

       -u <username>, --username <username>
           Remote jmx agent username

       --
           This option can be used to separate command-line options from the
           list of argument, (useful when arguments might be mistaken for
           command-line options

       [<keyspaces...>]
           List of keyspaces. By default, all keyspaces
```

각 옵션에 대한 한국어 설명은 다음과 같습니다.

- `-cf <table>`, `--column-family <table>`, `--table <table>`: 테이블 이름을 지정합니다(이 옵션을 사용하려면 반드시 키스페이스를 하나만 지정해야 합니다).
- `-h <host>`, `--host <host>`: 노드의 호스트명 또는 IP 주소입니다.
- `-kt <ktlist>`, `--kt-list <ktlist>`, `-kc <ktlist>`, `--kc.list <ktlist>`: 스냅샷을 생성할 `Keyspace.table` 목록입니다(키스페이스만 단독으로 지정해서는 안 됩니다).
- `-p <port>`, `--port <port>`: 원격 JMX 에이전트 포트 번호입니다.
- `-pp`, `--print-port`: 포트 번호로 호스트를 구분하는 4.0 모드로 동작합니다.
- `-pw <password>`, `--password <password>`: 원격 JMX 에이전트 비밀번호입니다.
- `-pwf <passwordFilePath>`, `--password-file <passwordFilePath>`: JMX 비밀번호 파일의 경로입니다.
- `-sf`, `--skip-flush`: 스냅샷 생성 전에 멤테이블을 플러시하지 않습니다(스냅샷에는 플러시되지 않은 데이터가 포함되지 않습니다).
- `-t <tag>`, `--tag <tag>`: 스냅샷의 이름(태그)입니다.
- `-u <username>`, `--username <username>`: 원격 JMX 에이전트 사용자명입니다.
- `--`: 이 옵션은 명령줄 옵션과 인자 목록을 구분하는 데 사용할 수 있습니다(인자가 명령줄 옵션으로 잘못 해석될 수 있을 때 유용합니다).
- `[<keyspaces...>]`: 키스페이스 목록입니다. 기본적으로는 모든 키스페이스를 대상으로 합니다.

#### 1.5.2 스냅샷을 위한 설정

자동 스냅샷을 비활성화하려면 cassandra.yaml에서 다음과 같이 설정합니다.

```yaml
auto_snapshot: false
snapshot_before_compaction: false
```

#### 1.5.3 스냅샷 생성

##### 키스페이스의 모든 테이블 스냅샷

`catalogkeyspace`의 모든 테이블에 대해 `catalog-ks` 태그를 가진 스냅샷을 생성합니다.

```
$ nodetool snapshot --tag catalog-ks catalogkeyspace
```

결과:

```
Requested creating snapshot(s) for [catalogkeyspace] with snapshot name [catalog-ks] and
options {skipFlush=false}
Snapshot directory: catalog-ks
```

여러 키스페이스에 대한 스냅샷을 한 번에 생성할 수 있습니다.

```
$ nodetool snapshot --tag catalog-cql-ks catalogkeyspace, cqlkeyspace
```

##### 키스페이스의 단일 테이블 스냅샷

```
$ nodetool snapshot --tag magazine --table magazine catalogkeyspace
```

결과:

```
Requested creating snapshot(s) for [catalogkeyspace] with snapshot name [magazine] and
options {skipFlush=false}
Snapshot directory: magazine
```

##### 동일 키스페이스 내 여러 테이블 스냅샷

`--kt-list` 옵션을 사용합니다.

```
$ nodetool snapshot --kt-list cqlkeyspace.t,cqlkeyspace.t2 --tag multi-table
```

결과:

```
Requested creating snapshot(s) for ["CQLKeyspace".t,"CQLKeyspace".t2] with snapshot name [multi-
table] and options {skipFlush=false}
Snapshot directory: multi-table
```

다른 태그로 또 다른 스냅샷을 생성합니다.

```
$ nodetool snapshot --kt-list cqlkeyspace.t, cqlkeyspace.t2 --tag multi-table-2
```

결과:

```
Requested creating snapshot(s) for ["CQLKeyspace".t,"CQLKeyspace".t2] with snapshot name [multi-
table-2] and options {skipFlush=false}
Snapshot directory: multi-table-2
```

##### 서로 다른 키스페이스의 여러 테이블 스냅샷

```
$ nodetool snapshot --kt-list catalogkeyspace.journal,cqlkeyspace.t --tag multi-ks
```

결과:

```
Requested creating snapshot(s) for [catalogkeyspace.journal,cqlkeyspace.t] with snapshot
name [multi-ks] and options {skipFlush=false}
Snapshot directory: multi-ks
```

#### 1.5.4 스냅샷 목록 조회

`nodetool listsnapshots` 명령을 사용합니다.

```
$ nodetool listsnapshots
```

결과:

```
Snapshot Details:
Snapshot name Keyspace name   Column family name True size Size on disk
multi-table   cqlkeyspace     t2                 4.86 KiB  5.67 KiB
multi-table   cqlkeyspace     t                  4.89 KiB  5.7 KiB
multi-ks      cqlkeyspace     t                  4.89 KiB  5.7 KiB
multi-ks      catalogkeyspace journal            4.9 KiB   5.73 KiB
magazine      catalogkeyspace magazine           4.9 KiB   5.73 KiB
multi-table-2 cqlkeyspace     t2                 4.86 KiB  5.67 KiB
multi-table-2 cqlkeyspace     t                  4.89 KiB  5.7 KiB
catalog-ks    catalogkeyspace journal            4.9 KiB   5.73 KiB
catalog-ks    catalogkeyspace magazine           4.9 KiB   5.73 KiB

Total TrueDiskSpaceUsed: 44.02 KiB
```

이 명령은 스냅샷 이름(Snapshot name), 키스페이스 이름(Keyspace name), 컬럼 패밀리(테이블) 이름(Column family name), 실제 크기(True size), 디스크상의 크기(Size on disk)를 표시합니다.

#### 1.5.5 스냅샷 디렉터리 찾기

`find` 명령을 사용하여 스냅샷 디렉터리를 찾습니다.

```
$ find -name snapshots
```

결과:

```
./cassandra/data/data/cqlkeyspace/t-d132e240c21711e9bbee19821dcea330/snapshots
./cassandra/data/data/cqlkeyspace/t2-d993a390c22911e9b1350d927649052c/snapshots
./cassandra/data/data/catalogkeyspace/journal-296a2d30c22a11e9b1350d927649052c/snapshots
./cassandra/data/data/catalogkeyspace/magazine-446eae30c22a11e9b1350d927649052c/snapshots
```

특정 스냅샷 디렉터리로 이동합니다.

```
$ cd ./cassandra/data/data/catalogkeyspace/journal-296a2d30c22a11e9b1350d927649052c/snapshots && ls -l
```

결과:

```
total 0
drwxrwxr-x. 2 ec2-user ec2-user 265 Aug 19 02:44 catalog-ks
drwxrwxr-x. 2 ec2-user ec2-user 265 Aug 19 02:52 multi-ks
```

스냅샷 디렉터리의 내용:

```
$ cd catalog-ks && ls -l
```

결과:

```
total 44
-rw-rw-r--. 1 ec2-user ec2-user   31 Aug 19 02:44 manifest.jsonZ
-rw-rw-r--. 4 ec2-user ec2-user   47 Aug 19 02:38 na-1-big-CompressionInfo.db
-rw-rw-r--. 4 ec2-user ec2-user   97 Aug 19 02:38 na-1-big-Data.db
-rw-rw-r--. 4 ec2-user ec2-user   10 Aug 19 02:38 na-1-big-Digest.crc32
-rw-rw-r--. 4 ec2-user ec2-user   16 Aug 19 02:38 na-1-big-Filter.db
-rw-rw-r--. 4 ec2-user ec2-user   16 Aug 19 02:38 na-1-big-Index.db
-rw-rw-r--. 4 ec2-user ec2-user 4687 Aug 19 02:38 na-1-big-Statistics.db
-rw-rw-r--. 4 ec2-user ec2-user   56 Aug 19 02:38 na-1-big-Summary.db
-rw-rw-r--. 4 ec2-user ec2-user   92 Aug 19 02:38 na-1-big-TOC.txt
-rw-rw-r--. 1 ec2-user ec2-user  814 Aug 19 02:44 schema.cql
```

각 스냅샷에는 복원 시 CQL로 테이블을 다시 생성하기 위한 DDL을 담고 있는 `schema.cql` 파일이 포함됩니다.

#### 1.5.6 스냅샷 삭제

`nodetool clearsnapshot` 명령을 사용하여 스냅샷을 삭제합니다. 스냅샷 이름을 지정하거나 `--all` 옵션을 사용할 수 있습니다.

특정 스냅샷을 삭제합니다.

```
$ nodetool clearsnapshot -t magazine cqlkeyspace
```

키스페이스의 모든 스냅샷을 삭제합니다.

```
$ nodetool clearsnapshot -all cqlkeyspace
```

### 1.6 증분 백업(Incremental Backups)

#### 1.6.1 증분 백업을 위한 설정

cassandra.yaml에서 증분 백업을 활성화합니다.

```yaml
incremental_backups: true
```

이 설정은 기본적으로 비활성화되어 있는데, 데이터가 플러시될 때마다 새로운 SSTable 파일이 생성되어 상당한 저장 공간을 소비할 수 있기 때문입니다. 증분 백업은 nodetool을 통해서도 관리할 수 있습니다.

- 활성화: `nodetool enablebackup`
- 비활성화: `nodetool disablebackup`
- 상태 확인: `nodetool statusbackup`

#### 1.6.2 증분 백업 생성

활성화한 후, nodetool로 테이블 데이터를 플러시합니다.

```
$ nodetool flush cqlkeyspace t
$ nodetool flush cqlkeyspace t2
$ nodetool flush catalogkeyspace journal magazine
```

#### 1.6.3 증분 백업 찾기

백업 디렉터리를 찾습니다.

```
$ find -name backups
```

결과:

```
./cassandra/data/data/cqlkeyspace/t-d132e240c21711e9bbee19821dcea330/backups
./cassandra/data/data/cqlkeyspace/t2-d993a390c22911e9b1350d927649052c/backups
./cassandra/data/data/catalogkeyspace/journal-296a2d30c22a11e9b1350d927649052c/backups
./cassandra/data/data/catalogkeyspace/magazine-446eae30c22a11e9b1350d927649052c/backups
```

#### 1.6.4 증분 백업 생성 예시

테이블을 플러시합니다.

```
$ nodetool flush cqlkeyspace t
```

백업 디렉터리가 존재하는지 확인합니다(초기에는 비어 있음).

```
$ cd ./cassandra/data/data/cqlkeyspace/t-d132e240c21711e9bbee19821dcea330/backups && ls -l
```

결과:

```
total 0
```

데이터를 추가하고 플러시한 후에는 백업 파일이 나타납니다.

```
$ nodetool flush cqlkeyspace t
$ cd ./cassandra/data/data/cqlkeyspace/t-d132e240c21711e9bbee19821dcea330/backups && ls -l
```

결과:

```
total 36
-rw-rw-r--. 2 ec2-user ec2-user   47 Aug 19 00:32 na-1-big-CompressionInfo.db
-rw-rw-r--. 2 ec2-user ec2-user   43 Aug 19 00:32 na-1-big-Data.db
-rw-rw-r--. 2 ec2-user ec2-user   10 Aug 19 00:32 na-1-big-Digest.crc32
-rw-rw-r--. 2 ec2-user ec2-user   16 Aug 19 00:32 na-1-big-Filter.db
-rw-rw-r--. 2 ec2-user ec2-user    8 Aug 19 00:32 na-1-big-Index.db
-rw-rw-r--. 2 ec2-user ec2-user 4673 Aug 19 00:32 na-1-big-Statistics.db
-rw-rw-r--. 2 ec2-user ec2-user   56 Aug 19 00:32 na-1-big-Summary.db
-rw-rw-r--. 2 ec2-user ec2-user   92 Aug 19 00:32 na-1-big-TOC.txt
```

추가로 플러시를 수행하면, 타임스탬프가 다른 SSTable 파일들이 연속적인 백업을 구분합니다.

```
total 72
-rw-rw-r--. 2 ec2-user ec2-user   47 Aug 19 00:32 na-1-big-CompressionInfo.db
-rw-rw-r--. 2 ec2-user ec2-user   43 Aug 19 00:32 na-1-big-Data.db
-rw-rw-r--. 2 ec2-user ec2-user   10 Aug 19 00:32 na-1-big-Digest.crc32
-rw-rw-r--. 2 ec2-user ec2-user   16 Aug 19 00:32 na-1-big-Filter.db
-rw-rw-r--. 2 ec2-user ec2-user    8 Aug 19 00:32 na-1-big-Index.db
-rw-rw-r--. 2 ec2-user ec2-user 4673 Aug 19 00:32 na-1-big-Statistics.db
-rw-rw-r--. 2 ec2-user ec2-user   56 Aug 19 00:32 na-1-big-Summary.db
-rw-rw-r--. 2 ec2-user ec2-user   92 Aug 19 00:32 na-1-big-TOC.txt
-rw-rw-r--. 2 ec2-user ec2-user   47 Aug 19 00:35 na-2-big-CompressionInfo.db
-rw-rw-r--. 2 ec2-user ec2-user   41 Aug 19 00:35 na-2-big-Data.db
-rw-rw-r--. 2 ec2-user ec2-user   10 Aug 19 00:35 na-2-big-Digest.crc32
-rw-rw-r--. 2 ec2-user ec2-user   16 Aug 19 00:35 na-2-big-Filter.db
-rw-rw-r--. 2 ec2-user ec2-user    8 Aug 19 00:35 na-2-big-Index.db
-rw-rw-r--. 2 ec2-user ec2-user 4673 Aug 19 00:35 na-2-big-Statistics.db
-rw-rw-r--. 2 ec2-user ec2-user   56 Aug 19 00:35 na-2-big-Summary.db
-rw-rw-r--. 2 ec2-user ec2-user   92 Aug 19 00:35 na-2-big-TOC.txt
```

### 1.7 스냅샷과 증분 백업으로부터 복원

삭제(drop)된 테이블을 복원하기 위한 주요 도구는 다음과 같습니다.

- `sstableloader`
- `nodetool refresh`

스냅샷은 증분 백업과 유사하게 동작하지만 추가 파일을 포함합니다. 중요한 점은, 스냅샷은 CQL에서 테이블을 생성하기 위한 스키마 DDL을 담은 `schema.cql` 파일을 포함한다는 것입니다. 반면, 테이블 백업(증분 백업)은 DDL을 포함하지 않으므로, 증분 백업으로부터 복원할 때는 스냅샷에서 DDL을 얻어야 합니다.

---

## 2. 벌크 로딩(Bulk Loading)

### 2.1 개요

Apache Cassandra는 SSTable을 통해 벌크 로딩(bulk loading)을 지원하며, SSTable이 유일하게 지원되는 형식입니다. 공식 문서는 다음과 같이 명시합니다. "Cassandra는 CSV, JSON, XML과 같은 다른 형식의 데이터를 직접 로드하는 것을 지원하지 않습니다."

벌크 로딩의 주요 목적은 다음 세 가지입니다.

- **증분 백업과 스냅샷 복원** — 이미 SSTable 형식인 증분 백업과 스냅샷을 복원합니다.
- **기존 SSTable을 다른 클러스터로 로드** — 노드 수나 복제 전략(replication strategy)이 다른 클러스터로 기존 SSTable을 로드합니다.
- **외부 데이터 가져오기(import)** — 외부 데이터를 클러스터로 가져옵니다.

### 2.2 주요 도구

Cassandra는 두 가지 주요 메커니즘을 제공합니다.

1. **sstableloader** — 기본 벌크 업로드 도구로, 복제 전략과 복제 인수(replication factor)를 준수하면서 실행 중인 클러스터로 SSTable 데이터를 스트리밍합니다.
2. **nodetool import** — 더 이상 사용이 권장되지 않는(deprecated) `nodetool refresh` 명령을 대체하는 권장 도구입니다.

### 2.3 sstableloader

기본 구문:

```
sstableloader [options] <dir_path>
```

#### 2.3.1 필수 요건

- 링(ring) 정보를 얻기 위한 하나 이상의 초기 호스트(initial hosts)를 쉼표로 구분하여 지정합니다.
- SSTable을 담고 있는 디렉터리 경로를 지정합니다.
- 디렉터리 구조는 반드시 `keyspace/table/` 형식을 따라야 합니다.

#### 2.3.2 주요 옵션

- `-d,--nodes <initial hosts>` (필수) — 연결할 초기 호스트입니다.
- `-k,--target-keyspace <name>` — 대상 키스페이스를 지정합니다.
- `-p,--port <port>` — 네이티브 트랜스포트(native transport) 포트입니다(기본값 9042).
- `-sp,--storage-port <port>` — 노드 간(internode) 통신 포트입니다(기본값 7000).
- `--throttle-mib <speed>` — 아웃바운드 스로틀(throttle)을 MiB/s 단위로 지정합니다.
- `--inter-dc-throttle-mib <speed>` — 데이터센터 간(cross-datacenter) 스로틀을 MiB/s 단위로 지정합니다.
- `-f,--conf-path <path>` — 스트리밍 및 암호화 설정을 위한 cassandra.yaml 경로입니다.
- `--no-progress` — 진행 상황 표시를 억제합니다.
- `-v,--verbose` — 자세한 출력(verbose output)을 활성화합니다.

#### 2.3.3 추가 SSL/인증 옵션

이 도구는 키스토어 경로(keystore path), 인증 자격 증명(authentication credentials), 알고리즘 지정(algorithm specification), 암호 스위트 선택(cipher suite selection)을 포함한 포괄적인 보안 설정을 지원합니다.

> 참고: `sstableloader`는 노드 간 통신을 위해 포트 7000을 사용합니다. 또한 로드하려는 SSTable은 로드 대상이 되는 Cassandra 버전과 호환되어야 합니다.

### 2.4 nodetool import

구문:

```
nodetool [(-h <host> | --host <host>)] [(-p <port> | --port <port>)]
       [(-pp | --print-port)] [(-pw <password> | --password <password>)]
       [(-pwf <passwordFilePath> | --password-file <passwordFilePath>)]
       [(-u <username> | --username <username>)] import
       [(-c | --no-invalidate-caches)] [(-e | --extended-verify)]
       [(-l | --keep-level)] [(-q | --quick)] [(-r | --keep-repaired)]
       [(-t | --no-tokens)] [(-v | --no-verify)] [--] <keyspace> <table>
       <directory> ...
```

#### 2.4.1 주요 장점

- 특정 디렉터리 구조가 요구되지 않습니다.
- 백업/스냅샷 디렉터리를 복사하지 않고도 직접 작업할 수 있습니다.
- 명령줄에서 테이블과 키스페이스를 지정할 수 있습니다.

#### 2.4.2 주요 옵션

- `-c, --no-invalidate-caches` — 행 캐시(row cache)를 보존합니다(캐시를 무효화하지 않음).
- `-e, --extended-verify` — 모든 SSTable 값을 검증합니다.
- `-l, --keep-level` — 기존 SSTable 레벨(level)을 유지합니다.
- `-q, --quick` — 검증을 건너뛰어 더 빠르게 가져옵니다.
- `-r, --keep-repaired` — 복구(repair) 상태를 보존합니다.
- `-t, --no-tokens` — 토큰 소유권(token ownership) 검증을 건너뜁니다.
- `-v, --no-verify` — SSTable 검증을 비활성화합니다.

### 2.5 sstableloader 실습 워크플로

#### 2.5.1 증분 백업으로부터 로드

1. 대상 디렉터리 구조를 생성합니다: `/catalogkeyspace/magazine/`
2. 대상 테이블이 존재하는지 확인합니다(비어 있어도 됨).
3. 백업 SSTable을 대상 디렉터리로 복사합니다.
4. 다음 명령을 실행합니다.

```
sstableloader --nodes 10.0.2.238 /catalogkeyspace/magazine/
```

#### 2.5.2 스냅샷으로부터 로드

1. 디렉터리 구조를 생성하거나, 스냅샷 폴더에 대한 심볼릭 링크(symlink)를 생성합니다.
2. 테이블이 삭제된 경우, 스냅샷의 `schema.cql`을 사용하여 테이블을 다시 생성합니다.
3. 동일한 명령 구조로 `sstableloader`를 실행합니다.

> 참고: 다른 클러스터로 로드한 테이블을 복구(repair)하더라도 원본 테이블은 복구되지 않습니다.

### 2.6 nodetool import 예시

#### 2.6.1 증분 백업 가져오기

```
nodetool import -- cqlkeyspace t \
./cassandra/data/data/cqlkeyspace/t-d132e240c21711e9bbee19821dcea330/backups
```

#### 2.6.2 스냅샷 가져오기

```
nodetool import -- catalogkeyspace journal \
./cassandra/data/data/catalogkeyspace/journal-296a2d30c22a11e9b1350d927649052c/snapshots/catalog-ks/
```

### 2.7 외부 데이터 벌크 로딩

외부 데이터(CSV, JSON, XML)의 직접적인 벌크 로딩은 지원되지 않습니다. 이를 위한 워크플로는 다음과 같습니다.

1. `CQLSSTableWriter` Java API를 사용하여 SSTable을 생성합니다.
2. 그런 다음 `sstableloader` 또는 `nodetool import`를 사용하여 벌크 로딩을 수행합니다.

#### 2.7.1 CQLSSTableWriter 요구 사항

- 출력 디렉터리(반드시 존재해야 함)
- 테이블 스키마(`CREATE TABLE` 구문)
- 준비된(prepared) `INSERT` 구문
- 파티셔너(partitioner) 지정(기본값은 `Murmur3Partitioner`)

#### 2.7.2 기본 구현 패턴

```java
public static final String OUTPUT_DIR = "./sstables";

String type = "CREATE TYPE CQLKeyspace.intType (a int, b int)";
String schema = "CREATE TABLE CQLKeyspace.t ("
                 + "  id int PRIMARY KEY,"
                 + "  k int,"
                 + "  v1 text,"
                 + "  v2 intType,"
                 + ")";
String insertStmt = "INSERT INTO CQLKeyspace.t (id, k, v1, v2) VALUES (?, ?, ?, ?)";

File outputDir = new File(OUTPUT_DIR + File.separator + "CQLKeyspace"
                         + File.separator + "t");

CQLSSTableWriter writer = CQLSSTableWriter.builder()
                                         .inDirectory(outputDir)
                                         .withType(type)
                                         .forTable(schema)
                                         .withBufferSizeInMiB(256)
                                         .using(insertStmt).build();

UserType userType = writer.getUDType("intType");
writer.addRow(0, 0, "val0", userType.newValue().setInt("a", 0).setInt("b", 0));
writer.addRow(1, 1, "val1", userType.newValue().setInt("a", 1).setInt("b", 1));
writer.close();
```

#### 2.7.3 CQLSSTableWriter 주요 메서드

| 메서드 | 용도 |
|--------|------|
| `addRow(List/Map/Object...)` | CQL 컬럼에 대응하는 타입으로 새 행을 삽입합니다 |
| `rawAddRow()` | 사전 직렬화된(pre-serialized) 바이너리 값을 추가합니다 |
| `getUDType(String)` | UDTValue 생성을 위한 사용자 정의 타입(user-defined type)을 가져옵니다 |
| `close()` | SSTable 생성을 마무리합니다 |

#### 2.7.4 CQLSSTableWriter.Builder 메서드

| 메서드 | 용도 |
|--------|------|
| `inDirectory(File/String)` | 필수: 출력 위치를 설정합니다 |
| `forTable(String schema)` | 필수: 완전한 정규 이름(fully-qualified name)으로 테이블을 정의합니다 |
| `using(String insert)` | 필수: 바인드 변수(bind variable)가 포함된 `INSERT` 구문을 지정합니다 |
| `withBufferSizeInMiB(int)` | 버퍼 크기를 설정합니다(기본값 128MB) |
| `withPartitioner(IPartitioner)` | 기본 `Murmur3Partitioner`를 재정의합니다 |
| `sorted()` | 입력이 파티션 키 순서로 미리 정렬되어 있을 것을 기대합니다 |
| `build()` | `CQLSSTableWriter` 인스턴스를 생성합니다 |

### 2.8 주의사항

- 공식 문서는 다음을 강조합니다. "증분 백업을 복원하기 전에, 멤테이블에 있는 데이터를 백업하기 위해 `nodetool flush`를 실행하십시오."
- "다른 클러스터로 로드한 테이블을 복구(repair)하더라도 원본 테이블은 복구되지 않습니다."
- 증분 백업은 DDL을 포함하지 않으므로, 대상 테이블이 미리 존재해야 합니다.
- 스냅샷은 테이블 재생성을 위한 `schema.cql`을 포함합니다.
- 두 도구 모두 특정 호스트와 포트로 연결하여 클러스터를 대상으로 지정할 수 있습니다.

---

## 3. 변경 데이터 캡처(Change Data Capture, CDC)

### 3.1 개요

변경 데이터 캡처(CDC)는 운영자에게 특정 테이블을 아카이빙(archival) 대상으로 표시하고, CDC 로그가 설정 가능한 디스크 크기 임계값에 도달하면 쓰기를 거부하는 기능을 제공합니다.

테이블에서 CDC가 활성화되면, 지정된 디렉터리에 CommitLogSegment에 대한 하드 링크(hard-link)가 생성됩니다. 정수 오프셋(integer offset)이 담긴 인덱스 파일이 디스크에 영속화된(persisted) 데이터를 추적하며, 'COMPLETED' 마커가 최종 처리 완료를 나타냅니다.

### 3.2 CDC 동작 방식

CDC는 일련의 파일 기반 메커니즘을 통해 동작합니다.

#### 3.2.1 CommitLogSegment 처리

테이블에 CDC가 활성화되어 있으면, "cassandra.yaml에 지정된 디렉터리에 해당 세그먼트에 대한 하드 링크가 생성됩니다." 세그먼트가 디스크에 fsync될 때, 만약 해당 세그먼트에 CDC 데이터가 존재하면 `<segment_name>_cdc.idx` 파일이 생성되며, 이 파일에는 "원본 세그먼트의 데이터 중 얼마만큼이 디스크에 영속화되었는지를 나타내는 정수 오프셋"이 담깁니다.

#### 3.2.2 인덱스 파일 메커니즘

메모리 매핑된(memory-mapped) 핸들에서 로그를 실시간으로 파싱하지 않고 인덱스 파일을 사용하는 이유는, 데이터가 아직 영속화되지 않고 커널 버퍼(kernel buffer)에 존재할 수 있기 때문입니다. 소비자(consumer)가 `_cdc.idx` 파일에 명시된 오프셋까지만 읽도록 함으로써, 내구성이 보장된(durable) CDC 데이터만 파싱하도록 보장합니다.

#### 3.2.3 완료 마커(Completion Marker)

"최종 세그먼트 플러시(flush) 시, `_cdc.idx` 파일에 사람이 읽을 수 있는 단어 'COMPLETED'가 적힌 두 번째 줄이 추가되어, Cassandra가 해당 파일에 대한 모든 처리를 완료했음을 나타냅니다."

#### 3.2.4 엣지 케이스 고려 사항

"드물게(예: 느린 디스크) 소비자가 `_cdc.idx` 파일에서 빈 값을 읽을 수 있습니다. 이는 갱신(update)이 파일을 먼저 truncate(잘라내기)한 뒤 다시 파일에 쓰는 방식으로 이루어지기 때문입니다. 이러한 경우 소비자는 인덱스 파일 읽기를 다시 시도해야 합니다."

#### 3.2.5 용량 관리(Capacity Management)

"허용되는 전체 디스크 공간의 임계값은 yaml에 지정되며, 이 임계값에 도달하면 새로 할당되는 CommitLogSegment는 소비자가 지정된 `cdc_raw` 디렉터리에서 파일을 파싱하여 제거할 때까지 CDC 데이터를 허용하지 않습니다."

### 3.3 설정(Configuration)

#### cassandra.yaml 파라미터

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| `cdc_enabled` | `false` | 노드 전역(node-wide)으로 CDC 동작을 활성화하거나 비활성화합니다 |
| `cdc_raw_directory` | `$CASSANDRA_HOME/data/cdc_raw` | 모든 멤테이블이 플러시된 후 CommitLogSegment가 이동할 목적지입니다 |
| `cdc_total_space` | min(4096MiB, 볼륨 공간의 1/8) | "CDC를 허용하는 모든 활성 CommitLogSegment와 `cdc_raw_directory`에 있는 모든 플러시된 CDC 세그먼트의 합으로 계산됩니다" |
| `cdc_free_space_check_interval` | 250ms | "용량에 도달했을 때, 불필요하게 CPU 사이클을 소모하지 않도록 `cdc_raw_directory`가 차지하는 공간을 다시 계산하는 빈도를 제한합니다" |

각 파라미터에 대한 상세 설명은 다음과 같습니다.

- **`cdc_enabled`** (기본값: `false`) — CDC 동작을 노드 전역으로 활성화하거나 비활성화합니다.
- **`cdc_raw_directory`** (기본값: `$CASSANDRA_HOME/data/cdc_raw`) — 모든 멤테이블이 플러시된 후 CommitLogSegment가 이동할 목적지 디렉터리입니다.
- **`cdc_total_space`** (기본값: 4096MiB와 볼륨 공간의 1/8 중 작은 값) — CDC를 허용하는 모든 활성 CommitLogSegment의 합과, `cdc_raw_directory`에 있는 모든 플러시된 CDC 세그먼트의 합으로 계산됩니다.
- **`cdc_free_space_check_interval`** (기본값: 250ms) — 용량에 도달한 상태에서 공간 사용량을 재계산하는 빈도입니다.

### 3.4 테이블에 CDC 활성화/비활성화

CDC는 `cdc` 테이블 속성(table property)을 통해 관리합니다.

```sql
CREATE TABLE foo (a int, b text, PRIMARY KEY(a)) WITH cdc=true;

ALTER TABLE foo WITH cdc=true;

ALTER TABLE foo WITH cdc=false;
```

테이블은 생성 시점에 CDC를 활성화하거나, 이후에 변경할 수 있습니다. 전체 구문은 테이블 생성(`creating tables`) 및 테이블 변경(`altering tables`) 관련 문서를 참조하십시오.

### 3.5 CommitLogSegment 읽기

CDC 데이터는 Cassandra의 Java API를 사용해 프로그래밍 방식으로 읽습니다.

#### 3.5.1 CommitLogReader

`CommitLogReader.java` 클래스를 사용합니다. 사용법은 비교적 직관적이며 다양한 시그니처(signature)를 지원합니다. 참조 구현(reference implementation)은 세그먼트 처리를 위한 표준 패턴을 보여줍니다.

#### 3.5.2 CommitLogReadHandler

"디스크에서 읽은 변형(mutation)을 처리하려면 `CommitLogReadHandler`를 구현하십시오." 이 인터페이스는 커밋 로그 세그먼트에서 추출된 변형을 처리하기 위한 계약(contract)을 정의합니다.

### 3.6 경고

**핵심 운영 요건:**

"어떤 형태든 소비 프로세스(consumption process)가 마련되어 있지 않은 상태에서 CDC를 활성화하지 마십시오."

"노드에서 CDC가 활성화되고 이어서 테이블에서도 CDC가 활성화되면, `cdc_free_space_in_mb`가 가득 차게 되며, 소비 프로세스가 마련되어 있지 않으면 CDC가 활성화된 테이블에 대한 쓰기가 거부됩니다."

소비 프로세스가 없으면 디스크 공간이 빠르게 가득 차고, CDC가 활성화된 테이블에 대한 쓰기가 거부됩니다.

#### 더 읽어보기

- Change Data Capture (CASSANDRA-8844 JIRA 티켓)
- Improve determinism of CDC data availability (CASSANDRA-12148 JIRA 티켓)

---

## 4. 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Backups](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/backups.html)
- [Bulk Loading](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/bulk_loading.html)
- [Change Data Capture (CDC)](https://cassandra.apache.org/doc/latest/cassandra/managing/operating/cdc.html)
