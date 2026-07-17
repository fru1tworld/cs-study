# Druid 배치 인제스천

> 원본: https://druid.apache.org/docs/latest/ingestion/native-batch
> 원본: https://druid.apache.org/docs/latest/ingestion/input-sources
> 원본: https://druid.apache.org/docs/latest/ingestion/hadoop

네이티브 배치 인제스천 태스크(`index_parallel`, `index`)의 구조와 튜닝, 파티셔닝 방식, 다양한 입력 소스(input source) 설정, 그리고 Hadoop 기반 인제스천의 지원 종료 내용을 정리합니다.

---

## 목차

1. [네이티브 배치 인제스천 개요](#네이티브-배치-인제스천-개요)
2. [태스크 제출 방법](#태스크-제출-방법)
3. [병렬 태스크 예시](#병렬-태스크-예시)
4. [ioConfig](#ioconfig)
5. [tuningConfig](#tuningconfig)
6. [splitHintSpec](#splithintspec)
7. [partitionsSpec](#partitionsspec)
8. [세그먼트 푸시 모드](#세그먼트-푸시-모드)
9. [병렬성 설정과 용량 계획](#병렬성-설정과-용량-계획)
10. [HTTP 상태 조회 엔드포인트](#http-상태-조회-엔드포인트)
11. [입력 소스](#입력-소스)
12. [Hadoop 기반 인제스천 (지원 종료)](#hadoop-기반-인제스천-지원-종료)
13. [참고 자료](#참고-자료)

---

## 네이티브 배치 인제스천 개요

네이티브 배치 인제스천은 두 가지 태스크 타입을 제공합니다.

- **`index_parallel`**: 멀티스레드 배치 인덱싱을 지휘하는 슈퍼바이저(supervisor) 태스크입니다. 슈퍼바이저가 입력 데이터를 분할한 뒤 워커(worker) 태스크를 생성해 각 부분을 동시에 처리하고, 모든 작업이 성공하면 세그먼트를 한꺼번에 발행(publish)합니다.
- **`index`**: 단일 스레드로 동작하는 단순 인덱싱 태스크로, 개발·테스트 환경에 적합합니다.

모든 인제스천 스펙은 세 가지 핵심 섹션으로 구성됩니다.

| 섹션 | 역할 |
| --- | --- |
| `dataSchema` | 타임스탬프 컬럼, 디멘션(dimension), 메트릭(metric), 변환(transform) 등 데이터 저장 방식을 정의합니다. |
| `ioConfig` | 입력 위치와 입력 포맷을 지정합니다. |
| `tuningConfig` | 성능 파라미터를 제어합니다. 생략하면 기본값을 사용합니다. |

배치 인제스천은 `bz2`, `gz`, `xz`, `zip`, `sz`(Snappy), `zst`(ZSTD) 압축 포맷을 지원합니다.

---

## 태스크 제출 방법

1. **Load Data UI**: 웹 콘솔에서 스펙을 정의하고 제출합니다.
2. **Tasks API**: Overlord의 `/druid/indexer/v1/task` 엔드포인트에 JSON 스펙을 POST로 전송합니다.
3. **인덱싱 스크립트**: Druid 배포판에 포함된 `bin/post-index-task` 스크립트를 사용합니다.

---

## 병렬 태스크 예시

`index_parallel` 태스크의 전체 스펙 예시입니다. `single_dim` 파티셔닝과 2개의 동시 서브태스크를 사용합니다.

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia_parallel_index_test",
      "timestampSpec": {
        "column": "timestamp"
      },
      "dimensionsSpec": {
        "dimensions": [
          "country", "page", "language", "user", "unpatrolled",
          "newPage", "robot", "anonymous", "namespace",
          "continent", "region", "city"
        ]
      },
      "metricsSpec": [
        { "type": "count", "name": "count" },
        { "type": "doubleSum", "name": "added", "fieldName": "added" },
        { "type": "doubleSum", "name": "deleted", "fieldName": "deleted" },
        { "type": "doubleSum", "name": "delta", "fieldName": "delta" }
      ],
      "granularitySpec": {
        "segmentGranularity": "DAY",
        "queryGranularity": "second",
        "intervals": ["2013-08-31/2013-09-02"]
      }
    },
    "ioConfig": {
      "type": "index_parallel",
      "inputSource": {
        "type": "local",
        "baseDir": "examples/indexing/",
        "filter": "wikipedia_index_data*"
      },
      "inputFormat": {
        "type": "json"
      }
    },
    "tuningConfig": {
      "type": "index_parallel",
      "partitionsSpec": {
        "type": "single_dim",
        "partitionDimension": "country",
        "targetRowsPerSegment": 5000000
      },
      "maxNumConcurrentSubTasks": 2
    }
  }
}
```

---

## ioConfig

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | 태스크 타입. `index_parallel`로 설정합니다. | 없음 | 예 |
| `inputFormat` | 입력 데이터 파싱 방법을 지정합니다. | 없음 | 예 |
| `appendToExisting` | 기존 세그먼트를 대체하지 않고 추가 샤드(shard)로 세그먼트를 생성합니다. dynamic 파티셔닝에서만 사용할 수 있습니다. | false | 아니요 |
| `dropExisting` | `appendToExisting`이 false일 때, 지정한 interval에 완전히 포함되는 기존 세그먼트를 발행 시점에 모두 교체합니다. | false | 아니요 |

---

## tuningConfig

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | 태스크 타입. `index_parallel`로 설정합니다. | 없음 | 예 |
| `maxRowsInMemory` | 중간 persist를 수행할 행 수 기준입니다. | 1000000 | 아니요 |
| `maxBytesInMemory` | persist 전에 힙에 유지할 수 있는 집계 데이터의 메모리 한도입니다. | 최대 JVM 힙의 1/6 | 아니요 |
| `maxColumnsToMerge` | 병합 단계에서 한 번에 병합하는 세그먼트 수를 제한합니다. | -1 (무제한) | 아니요 |
| `maxTotalRows` | 사용 중단(deprecated). 대신 `partitionsSpec`을 사용합니다. | 20000000 | 아니요 |
| `numShards` | 사용 중단(deprecated). 대신 `partitionsSpec`을 사용합니다. | null | 아니요 |
| `splitHintSpec` | 1단계에서 각 태스크가 읽을 데이터 양을 제어하는 힌트입니다. | size-based | 아니요 |
| `partitionsSpec` | 2차 파티셔닝 방식을 지정합니다. | dynamic 또는 hashed | 아니요 |
| `indexSpec` | 세그먼트 저장 포맷 옵션입니다. | null | 아니요 |
| `indexSpecForIntermediatePersists` | 중간 persist 세그먼트의 저장 포맷 옵션입니다. | `indexSpec`과 동일 | 아니요 |
| `maxPendingPersists` | 대기열에 쌓을 수 있는 persist의 최대 개수입니다. | 0 | 아니요 |
| `forceGuaranteedRollup` | perfect rollup 모드를 강제합니다. | false | 아니요 |
| `reportParseExceptions` | 파싱 오류 발생 시 인제스천을 중단합니다. | false | 아니요 |
| `pushTimeout` | 세그먼트 푸시를 기다리는 시간(밀리초)입니다. | 0 | 아니요 |
| `segmentWriteOutMediumFactory` | 세그먼트 생성에 사용할 매체(medium)입니다. | druid.peon 설정 | 아니요 |
| `maxNumConcurrentSubTasks` | 동시에 실행할 워커 태스크의 최대 개수입니다. | 1 | 아니요 |
| `maxRetry` | 태스크 실패 시 최대 재시도 횟수입니다. | 3 | 아니요 |
| `maxNumSegmentsToMerge` | 동시에 병합할 수 있는 세그먼트의 최대 개수입니다. | 100 | 아니요 |
| `totalNumMergeTasks` | 병합 단계에 사용할 태스크 수입니다. | 10 | 아니요 |
| `taskStatusCheckPeriodMs` | 태스크 상태 폴링 주기(밀리초)입니다. | 1000 | 아니요 |
| `chatHandlerTimeout` | 세그먼트 보고 타임아웃입니다. | PT10S | 아니요 |
| `chatHandlerNumRetries` | 세그먼트 보고 재시도 횟수입니다. | 5 | 아니요 |
| `awaitSegmentAvailabilityTimeoutMillis` | 인제스천 완료 후 세그먼트가 조회 가능해질 때까지 기다리는 시간입니다. | 0 | 아니요 |

---

## splitHintSpec

첫 단계(입력 읽기)에서 각 워커 태스크에 할당하는 데이터 양을 제어합니다.

### 크기 기반(Size-based, 기본값)

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `maxSize`로 설정합니다. | 없음 | 예 |
| `maxSplitSize` | 서브태스크당 최대 바이트 수입니다. | 1GiB | 아니요 |
| `maxNumFiles` | 서브태스크당 최대 파일 수입니다. | 1000 | 아니요 |

### 세그먼트 기반(Segments-based, DruidInputSource용)

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `segments`로 설정합니다. | 없음 | 예 |
| `maxInputSegmentBytesPerTask` | 서브태스크당 최대 세그먼트 바이트 수입니다. | 1GiB | 아니요 |
| `maxNumSegments` | 서브태스크당 최대 세그먼트 수입니다. | 1000 | 아니요 |

---

## partitionsSpec

시간 기반 1차 파티셔닝 아래에서 세그먼트를 나누는 2차 파티셔닝 방식을 지정합니다.

### dynamic 파티셔닝

행 수 기준으로 세그먼트를 생성합니다. 단일 단계로 실행되며, 여러 워커 태스크가 각각 독립적으로 세그먼트를 만들고 임계값을 넘으면 세그먼트를 푸시합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `dynamic`으로 설정합니다. | 없음 | 예 |
| `maxRowsPerSegment` | 세그먼트당 행 수 임계값입니다. | 5000000 | 아니요 |
| `maxTotalRows` | 세그먼트를 푸시하기 전까지 누적할 총 행 수입니다. | 20000000 | 아니요 |

### hashed 파티셔닝

지정한 디멘션들의 해시 값으로 행을 분배하며, 여러 단계(phase)로 실행됩니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `hashed`로 설정합니다. | 없음 | 예 |
| `numShards` | 샤드 개수를 명시적으로 지정합니다. | 없음 | 아니요 |
| `targetRowsPerSegment` | 파티션당 목표 행 수입니다. | 5000000 | 아니요 |
| `partitionDimensions` | 파티셔닝에 사용할 디멘션입니다. 비워 두면 모든 디멘션을 사용합니다. | null | 아니요 |
| `partitionFunction` | 해시 계산 함수입니다. | `murmur3_32_abs` | 아니요 |

실행 단계: (선택) 디멘션 카디널리티 추정 → 부분(partial) 세그먼트 생성 → 부분 세그먼트 병합

### single_dim (단일 디멘션 범위) 파티셔닝

하나의 디멘션 값 범위로 파티셔닝하여 데이터 지역성(locality)을 높입니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `single_dim`으로 설정합니다. | 없음 | 예 |
| `partitionDimension` | 범위 파티셔닝에 사용할 디멘션입니다. | 없음 | 예 |
| `targetRowsPerSegment` | 파티션당 목표 행 수입니다. | 없음 | 이 속성 또는 `maxRowsPerSegment` 중 하나 |
| `maxRowsPerSegment` | 파티션당 소프트 최대 행 수입니다. | 없음 | 이 속성 또는 `targetRowsPerSegment` 중 하나 |
| `assumeGrouped` | 입력 데이터가 이미 그룹핑되었다고 가정합니다. | false | 아니요 |

실행 단계: 디멘션 분포 산출(히스토그램 구축) → 부분 세그먼트 생성 → 부분 세그먼트 병합

### range (다중 디멘션 범위) 파티셔닝

single_dim 방식을 여러 디멘션으로 확장하여 세그먼트 분포의 균형을 개선합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `range`로 설정합니다. | 없음 | 예 |
| `partitionDimensions` | 파티셔닝에 사용할 디멘션 배열입니다. 쿼리에서 자주 사용하는 순서대로 나열합니다. | 없음 | 예 |
| `targetRowsPerSegment` | 파티션당 목표 행 수입니다. | 없음 | 이 속성 또는 `maxRowsPerSegment` 중 하나 |
| `maxRowsPerSegment` | 파티션당 소프트 최대 행 수입니다. | 없음 | 이 속성 또는 `targetRowsPerSegment` 중 하나 |
| `assumeGrouped` | 입력 데이터가 이미 그룹핑되었다고 가정합니다. | false | 아니요 |

---

## 세그먼트 푸시 모드

- **일괄 푸시(Bulk Pushing) — perfect rollup**: 태스크 완료 시점에 모든 세그먼트를 한꺼번에 푸시합니다. `forceGuaranteedRollup: true`로 활성화하며, `appendToExisting`과 함께 사용할 수 없습니다.
- **점진적 푸시(Incremental Pushing) — best-effort rollup**: 누적 행 수가 `maxTotalRows`를 초과하면 인제스천 도중에 세그먼트를 푸시하고, 이후 인제스천을 계속합니다.

---

## 병렬성 설정과 용량 계획

- **`maxNumConcurrentSubTasks`**: 슈퍼바이저는 사용 가능한 태스크 슬롯 수와 무관하게 이 값까지 워커 태스크를 생성합니다. 실행 중 총 태스크 수는 슈퍼바이저를 포함해 `maxNumConcurrentSubTasks + 1`입니다. 슬롯이 부족하면 태스크가 pending 상태로 대기합니다.
- **용량 계획**: 배치와 스트리밍을 함께 운영한다면, 스트림 인제스천이 막히지 않도록 `(모든 병렬 태스크의 maxNumConcurrentSubTasks 합계 + 슈퍼바이저 수) < 배치 태스크 한도`를 유지해야 합니다.

---

## HTTP 상태 조회 엔드포인트

슈퍼바이저 태스크는 다음 모니터링 엔드포인트를 제공합니다.

| 엔드포인트 | 반환 내용 |
| --- | --- |
| `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/mode` | `parallel` 또는 `sequential` |
| `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/phase` | 현재 단계 이름 |
| `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/progress` | 현재 단계의 진행률 추정치 |
| `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/subtasks/running` | 실행 중인 워커 태스크 ID 목록 |
| `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/subtaskspecs` | 전체 워커 태스크 스펙 |
| `/druid/worker/v1/chat/{SUPERVISOR_TASK_ID}/subtaskspec/{SUB_TASK_SPEC_ID}/state` | 워커 태스크 상태(스펙과 시도 이력 포함) |

---

## 입력 소스

입력 소스는 네이티브 배치 인덱싱 태스크가 데이터를 읽는 위치를 정의합니다. 네이티브 병렬 태스크(`index_parallel`)와 단순 태스크(`index`)만 입력 소스를 지원합니다.

분할 가능(splittable)한 입력 소스는 병렬 태스크에서 워커 태스크에 분산 처리할 수 있으며, S3, Google Cloud Storage, Azure, HDFS, HTTP, Local, Druid, Combining, Iceberg, Delta Lake가 여기에 해당합니다.

여러 입력 소스가 메타데이터 추적을 위한 시스템 필드를 지원합니다.

- `__file_uri`: 파일/객체의 전체 URI
- `__file_bucket`: 스토리지 버킷/컨테이너 이름
- `__file_path`: 파일 경로

인제스천 태스크는 Druid 프로세스를 실행하는 운영 체제 계정으로 동작한다는 점에 유의해야 합니다.

### S3 입력 소스

**필요 익스텐션**: `druid-s3-extensions`

Amazon S3에서 객체를 직접 읽습니다. URI 목록, prefix 목록, 명시적 객체 지정 중 하나를 사용하며, 병렬 태스크에서 분할 가능합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `s3`로 설정합니다. | 없음 | 예 |
| `uris` | S3 URI 문자열의 JSON 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `prefixes` | S3 위치 prefix의 JSON 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `objects` | S3 객체(bucket/path 쌍)의 JSON 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `objectGlob` | 객체 경로에 적용할 glob 패턴입니다(예: `**.json`). | 없음 | 아니요 |
| `systemFields` | 반환할 시스템 필드(`__file_uri`, `__file_bucket`, `__file_path`)입니다. | 없음 | 아니요 |
| `endpointConfig` | 기본 S3 endpoint/region을 재정의합니다. | 없음 | 아니요 |
| `clientConfig` | 재정의한 endpoint에 사용할 S3 클라이언트 속성입니다. | 없음 | 아니요 |
| `proxyConfig` | 프록시 설정(host, port, username, password)입니다. | 없음 | 아니요 |
| `properties` | 접근 자격 증명과 role 위임(assume role) 설정입니다. | 없음 | 아니요 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "s3",
      "objectGlob": "**.json",
      "uris": ["s3://foo/bar/file.json", "s3://bar/foo/file2.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

자격 증명을 직접 지정하는 예시입니다.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "s3",
      "objectGlob": "**.json",
      "uris": ["s3://foo/bar/file.json"],
      "properties": {
        "accessKeyId": "KLJ78979SDFdS2",
        "secretAccessKey": "KLS89s98sKJHKJKJH8721lljkd",
        "assumeRoleArn": "arn:aws:iam::2981002874992:role/role-s3"
      }
    },
    "inputFormat": { "type": "json" }
  }
}
```

### Google Cloud Storage 입력 소스

**필요 익스텐션**: `druid-google-extensions`

Google Cloud Storage에서 객체를 직접 읽으며, URI·prefix·객체 지정을 지원합니다. 분할 가능합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `google`로 설정합니다. | 없음 | 예 |
| `uris` | GCS URI의 JSON 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `prefixes` | GCS URI prefix의 JSON 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `objects` | GCS 객체(bucket/path 쌍)의 JSON 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `objectGlob` | 객체 경로에 적용할 glob 패턴입니다(예: `**.json`). | 없음 | 아니요 |
| `systemFields` | 시스템 필드(`__file_uri`, `__file_bucket`, `__file_path`)입니다. | 없음 | 아니요 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "google",
      "objectGlob": "**.json",
      "uris": ["gs://foo/bar/file.json", "gs://bar/foo/file2.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

### Azure 입력 소스

**필요 익스텐션**: `druid-azure-extensions`

Azure Blob Storage 또는 Azure Data Lake에서 데이터를 읽습니다. 레거시 `azure` 타입보다 최신 `azureStorage` 타입 사용을 권장합니다.

**azureStorage (권장)**

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `azureStorage`로 설정합니다. | 없음 | 예 |
| `uris` | `azureStorage://storageAccount/container/path` 형식의 URI입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `prefixes` | `azureStorage://storageAccount/container/prefix` 형식의 URI prefix입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `objects` | bucket/path 쌍으로 이루어진 객체 배열입니다. | 없음 | `uris`, `prefixes`, `objects` 중 하나 |
| `objectGlob` | glob 패턴입니다(예: `**.json`). | 없음 | 아니요 |
| `systemFields` | 시스템 필드(`__file_uri`, `__file_bucket`, `__file_path`)입니다. | 없음 | 아니요 |
| `properties` | 인증 설정입니다. | 없음 | 아니요 |

인증에는 다음 속성을 사용할 수 있습니다.

- `sharedAccessStorageToken`: Shared Access Token 문자열
- `key`: 스토리지 계정 루트 키
- `appRegistrationClientId`: Azure 앱 클라이언트 ID
- `appRegistrationClientSecret`: Azure 앱 클라이언트 시크릿 (`appRegistrationClientId` 사용 시 필수)
- `tenantId`: Azure 테넌트 ID (`appRegistrationClientId` 사용 시 필수)

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "azureStorage",
      "objectGlob": "**.json",
      "uris": ["azureStorage://storageAccount/container/prefix1/file.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

레거시 `azure` 입력 소스는 `azure://container/path` 형식을 사용하며, 신규 구성에는 권장하지 않습니다.

### HDFS 입력 소스

**필요 익스텐션**: `druid-hdfs-storage`

HDFS에서 파일을 직접 읽습니다. 와일드카드를 지원하며 병렬 태스크에서 분할 가능합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `hdfs`로 설정합니다. | 없음 | 예 |
| `paths` | HDFS 경로(JSON 배열 또는 쉼표 구분 문자열)입니다. | 없음 | 예 |
| `systemFields` | 시스템 필드(`__file_uri`, `__file_path`)입니다. | 없음 | 아니요 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "hdfs",
      "paths": ["hdfs://namenode_host/foo/bar/file.json", "hdfs://namenode_host/bar/foo/file2.json"]
    },
    "inputFormat": { "type": "json" }
  }
}
```

### HTTP 입력 소스

HTTP/HTTPS로 원격 파일을 읽습니다. 분할 가능하며, 워커 태스크 하나가 파일 하나를 읽습니다. 허용 프로토콜(HTTP, HTTPS, FTP, file)은 설정으로 제어할 수 있습니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `http`로 설정합니다. | 없음 | 예 |
| `uris` | 입력 파일의 URI 목록입니다. | 없음 | 예 |
| `httpAuthenticationUsername` | Basic 인증 사용자 이름입니다. | 없음 | 아니요 |
| `httpAuthenticationPassword` | Basic 인증 패스워드의 password provider입니다. | 없음 | 아니요 |
| `systemFields` | 시스템 필드(`__file_uri`, `__file_path`)입니다. | 없음 | 아니요 |

password provider는 환경 변수나 커스텀 구현으로 외부 시크릿을 참조할 수 있습니다.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "http",
      "uris": ["http://example.com/uri1"],
      "httpAuthenticationUsername": "username",
      "httpAuthenticationPassword": "password123"
    },
    "inputFormat": { "type": "json" }
  }
}
```

### Inline 입력 소스

스펙 안에 데이터를 직접 넣어 인제스천합니다. 데모나 간단한 테스트에 유용합니다.

| 속성 | 설명 | 필수 |
| --- | --- | --- |
| `type` | `inline`으로 설정합니다. | 예 |
| `data` | 인제스천할 인라인 데이터입니다. | 예 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "inline",
      "data": "0,values,formatted\n1,as,CSV"
    },
    "inputFormat": { "type": "csv" }
  }
}
```

### Local 입력 소스

로컬 스토리지의 파일을 읽습니다. 주로 개념 검증(proof-of-concept) 용도이며 분할 가능합니다.

| 속성 | 설명 | 필수 |
| --- | --- | --- |
| `type` | `local`로 설정합니다. | 예 |
| `filter` | 파일 와일드카드 필터입니다. | `baseDir`을 지정한 경우 예 |
| `baseDir` | 재귀적으로 탐색할 디렉터리입니다. | `baseDir` 또는 `files` 중 하나 이상 |
| `files` | 인제스천할 파일 경로 목록입니다. | `baseDir` 또는 `files` 중 하나 이상 |
| `systemFields` | 시스템 필드(`__file_uri`, `__file_path`)입니다. | 아니요 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "local",
      "filter": "*.csv",
      "baseDir": "/data/directory",
      "files": ["/bar/foo", "/foo/bar"]
    },
    "inputFormat": { "type": "csv" }
  }
}
```

### Druid 입력 소스

기존 Druid 세그먼트에서 데이터를 읽어 재색인(reindexing), 롤업(rollup), 재파티셔닝에 활용합니다. 분할 가능합니다.

| 속성 | 설명 | 필수 |
| --- | --- | --- |
| `type` | `druid`로 설정합니다. | 예 |
| `dataSource` | 원본 데이터소스 이름입니다. | 예 |
| `interval` | 시간 범위를 지정하는 ISO-8601 interval입니다. | 예 |
| `filter` | 읽을 행을 제한하는 쿼리 필터입니다. | 아니요 |

주요 특징은 다음과 같습니다.

- 타임스탬프 컬럼은 epoch 밀리초 값을 담은 숫자형 `__time` 필드로 나타납니다.
- 입력 데이터소스와 출력 데이터소스가 같아도 됩니다.
- `inputFormat`이 필요 없습니다.

`wikipedia_raw`를 시간 단위로 롤업해 `wikipedia_rollup`으로 만드는 예시입니다.

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia_rollup",
      "timestampSpec": {
        "column": "__time",
        "format": "millis"
      },
      "dimensionsSpec": {
        "dimensions": ["countryName", "page"]
      },
      "metricsSpec": [
        { "type": "count", "name": "cnt" }
      ],
      "granularitySpec": {
        "type": "uniform",
        "queryGranularity": "HOUR",
        "segmentGranularity": "DAY",
        "intervals": ["2016-06-27/P1D"],
        "rollup": true
      }
    },
    "ioConfig": {
      "type": "index_parallel",
      "inputSource": {
        "type": "druid",
        "dataSource": "wikipedia_raw",
        "interval": "2016-06-27/P1D"
      }
    },
    "tuningConfig": {
      "type": "index_parallel",
      "partitionsSpec": { "type": "hashed" },
      "forceGuaranteedRollup": true,
      "maxNumConcurrentSubTasks": 1
    }
  }
}
```

### SQL 입력 소스

**필요 익스텐션**: MySQL은 `mysql-metadata-storage`, PostgreSQL은 `postgresql-metadata-storage`

RDBMS에서 SQL 쿼리로 데이터를 직접 읽습니다. 쿼리 하나가 별도의 워커 태스크에서 실행됩니다.

| 속성 | 설명 | 필수 |
| --- | --- | --- |
| `type` | `sql`로 설정합니다. | 예 |
| `database` | 데이터베이스 연결 정보(type, connectorConfig)입니다. | 예 |
| `foldCase` | 컬럼 이름의 대소문자 접기(case folding) 여부입니다. | 아니요 |
| `sqls` | 데이터를 조회할 SQL 쿼리 목록입니다. | 예 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "sql",
      "database": {
        "type": "mysql",
        "connectorConfig": {
          "connectURI": "jdbc:mysql://host:port/schema",
          "user": "user",
          "password": "password"
        }
      },
      "sqls": [
        "SELECT * FROM table1 WHERE timestamp BETWEEN '2013-01-01 00:00:00' AND '2013-01-01 11:59:59'",
        "SELECT * FROM table2 WHERE timestamp BETWEEN '2013-01-01 00:00:00' AND '2013-01-01 11:59:59'"
      ]
    }
  }
}
```

권장 사항은 다음과 같습니다.

- 불필요한 데이터를 가져오지 않도록 SQL 쿼리를 interval 기준으로 필터링합니다.
- 워커 태스크 간 데이터가 고르게 분배되도록 페이지네이션을 활용합니다.
- 인제스천 중 로컬 저장에 필요한 디스크 공간을 충분히 확보합니다.
- Middle Manager/Indexer의 데이터 크기 제약을 모니터링합니다.

### Combining 입력 소스

분할 가능한 여러 입력 소스를 동시에 읽습니다. 모든 delegate가 같은 `inputFormat`을 사용해야 합니다.

| 속성 | 설명 | 필수 |
| --- | --- | --- |
| `type` | `combining`으로 설정합니다. | 예 |
| `delegates` | 분할 가능한 입력 소스의 목록입니다. | 예 |

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "combining",
      "delegates": [
        {
          "type": "local",
          "filter": "*.csv",
          "baseDir": "/data/directory",
          "files": ["/bar/foo", "/foo/bar"]
        },
        {
          "type": "druid",
          "dataSource": "wikipedia",
          "interval": "2013-01-01/2013-01-02"
        }
      ]
    },
    "inputFormat": { "type": "csv" }
  }
}
```

### Iceberg 입력 소스

**필요 익스텐션**: `druid-iceberg-extensions` (그리고 `druid-s3-extensions` 같은 스토리지별 익스텐션)

Apache Iceberg 테이블 포맷의 데이터를 읽습니다. 설정한 카탈로그(Hive/REST/Glue)에서 최신 스냅샷을 스캔합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `iceberg`로 설정합니다. | 없음 | 예 |
| `tableName` | 카탈로그에 등록된 Iceberg 테이블 이름입니다. | 없음 | 예 |
| `namespace` | 테이블의 Iceberg namespace입니다. | 없음 | 예 |
| `icebergCatalog` | 카탈로그 설정 객체입니다. | 없음 | 예 |
| `icebergFilter` | 읽을 데이터 파일을 줄이는 필터입니다. | 없음 | 아니요 |
| `warehouseSource` | 웨어하우스 파일을 읽을 네이티브 입력 소스입니다. | 없음 | 예 |
| `snapshotTime` | 특정 스냅샷을 지정하는 ISO8601 타임스탬프입니다. | 없음 | 아니요 |
| `residualFilterMode` | 파티션 컬럼이 아닌 컬럼에 걸린 필터 처리 방식(`ignore`/`fail`)입니다. | ignore | 아니요 |

카탈로그 타입은 `hive`, `rest`, `glue`, `local`을 지원합니다. 필터 타입으로는 `equals`, `interval`, `range`, `timeWindow`와 논리 연산자 `and`, `or`, `not`을 사용할 수 있습니다.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "iceberg",
      "tableName": "iceberg_table",
      "namespace": "iceberg_namespace",
      "icebergCatalog": {
        "type": "hive",
        "warehousePath": "hdfs://warehouse/path",
        "catalogUri": "thrift://hive-metastore.x.com:8970",
        "catalogProperties": {
          "hive.metastore.connect.retries": "1"
        }
      },
      "icebergFilter": {
        "type": "interval",
        "filterColumn": "event_time",
        "intervals": ["2023-05-10T19:00:00.000Z/2023-05-10T20:00:00.000Z"]
      },
      "warehouseSource": { "type": "hdfs" }
    },
    "inputFormat": { "type": "parquet" }
  }
}
```

### Delta Lake 입력 소스

**필요 익스텐션**: `druid-deltalake-extensions`

Delta Lake 테이블 포맷의 데이터를 읽습니다. 최신 스냅샷 또는 지정한 버전을 스캔합니다.

| 속성 | 설명 | 기본값 | 필수 |
| --- | --- | --- | --- |
| `type` | `delta`로 설정합니다. | 없음 | 예 |
| `tablePath` | Delta 테이블의 위치입니다. | 없음 | 예 |
| `filter` | 스냅샷 안에서 읽을 데이터 파일을 거르는 필터입니다. | 없음 | 아니요 |
| `snapshotVersion` | 읽을 스냅샷의 정수 버전입니다. | 최신 스냅샷 | 아니요 |

필터는 비교 연산자(`=`, `>`, `>=`, `<`, `<=`)와 논리 연산자(`and`, `or`, `not`)를 지원합니다.

```json
{
  "ioConfig": {
    "type": "index_parallel",
    "inputSource": {
      "type": "delta",
      "tablePath": "/delta-table/foo",
      "filter": {
        "type": "and",
        "filters": [
          { "type": "=", "column": "name", "value": "Employee4" },
          { "type": ">=", "column": "age", "value": "30" }
        ]
      },
      "snapshotVersion": 3
    }
  }
}
```

---

## Hadoop 기반 인제스천 (지원 종료)

Apache Hadoop 기반 인제스천 지원은 **Apache Druid 37.0.0에서 제거**되었습니다. 공식 문서는 다음 대안을 권장합니다.

1. **SQL 기반 인제스천 (SQL-based ingestion)**
2. **네이티브 배치 인제스천 (native batch ingestion)**

인제스천과 무관한 Hadoop 생태계 익스텐션, 예를 들어 딥 스토리지(deep storage)용 `druid-hdfs-storage` 등은 계속 지원됩니다.

Firehose 기반 인제스천 역시 **Druid 26.0에서 제거**되었으며, 기존 firehose 설정은 입력 소스 기반 인제스천으로 마이그레이션해야 합니다.

---

## 참고 자료

- [JSON-based batch ingestion (native batch)](https://druid.apache.org/docs/latest/ingestion/native-batch)
- [Input sources](https://druid.apache.org/docs/latest/ingestion/input-sources)
- [Hadoop-based ingestion (removed)](https://druid.apache.org/docs/latest/ingestion/hadoop)
- [SQL-based ingestion](https://druid.apache.org/docs/latest/multi-stage-query/)
- [Ingestion overview](https://druid.apache.org/docs/latest/ingestion/)
