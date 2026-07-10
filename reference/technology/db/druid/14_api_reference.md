# Druid API 레퍼런스

> 원본: https://druid.apache.org/docs/latest/api-reference/
> 원본: https://druid.apache.org/docs/latest/api-reference/sql-api
> 원본: https://druid.apache.org/docs/latest/api-reference/sql-ingestion-api
> 원본: https://druid.apache.org/docs/latest/api-reference/tasks-api
> 원본: https://druid.apache.org/docs/latest/api-reference/supervisor-api
> 원본: https://druid.apache.org/docs/latest/api-reference/data-management-api
> 원본: https://druid.apache.org/docs/latest/api-reference/service-status-api

Druid가 제공하는 HTTP API 가운데 SQL 쿼리, SQL 기반 인제스천(MSQ), 태스크, 슈퍼바이저(supervisor), 데이터 관리, 서비스 상태 API의 주요 엔드포인트를 정리합니다.

---

## 목차

1. [API 레퍼런스 개요](#api-레퍼런스-개요)
2. [Druid SQL API](#druid-sql-api)
3. [SQL 인제스천 API](#sql-인제스천-api)
4. [Tasks API](#tasks-api)
5. [Supervisor API](#supervisor-api)
6. [데이터 관리 API](#데이터-관리-api)
7. [서비스 상태 API](#서비스-상태-api)
8. [참고 자료](#참고-자료)

---

## API 레퍼런스 개요

Druid는 클러스터의 각 기능을 HTTP API로 노출합니다. 공식 문서의 API 레퍼런스 섹션은 다음 범주로 구성됩니다.

| 범주 | 설명 |
| --- | --- |
| Druid SQL queries | Druid SQL API로 SQL 쿼리를 제출합니다. |
| SQL-based ingestion | SQL 기반 배치 인제스천 요청을 제출합니다. |
| JSON querying | JSON 기반 네이티브(native) 쿼리를 제출합니다. |
| Tasks | 데이터 인제스천 작업(태스크)을 관리합니다. |
| Supervisors | 스트리밍 인제스천의 생명주기를 담당하는 슈퍼바이저를 관리합니다. |
| Retention rules | 데이터 보존(retention) 정책을 정의하고 관리합니다. |
| Data management | 데이터 세그먼트(segment)를 관리합니다. |
| Automatic compaction | 인제스천 이후 세그먼트 크기를 최적화합니다. |
| Lookups | 키-값 데이터소스(lookup)를 관리합니다. |
| Service status | 클러스터 구성 요소의 상태를 모니터링합니다. |
| Dynamic configuration | Coordinator와 Overlord 프로세스의 동적 설정을 관리합니다. |
| Legacy metadata | 데이터소스 메타데이터를 조회합니다. |

Java API로는 Avatica JDBC 드라이버 기반의 **SQL JDBC driver**를 제공하며, 이 드라이버로 Druid에 접속해 SQL 쿼리를 실행할 수 있습니다.

예시 요청의 `ROUTER_IP:ROUTER_PORT`는 Router 서비스 주소(예: `localhost:8888`)로 바꿔서 사용합니다.

---

## Druid SQL API

### 쿼리 제출 (Historical에 적재된 데이터)

```
POST /druid/v2/sql
```

적재된 데이터에 대해 SQL 쿼리를 실행합니다. 요청 본문은 JSON 객체이며 다음 필드를 사용합니다.

| 필드 | 설명 |
| --- | --- |
| `query` | 실행할 SQL 문. 문자열 안에 여러 개의 `SET` 문을 넣어 컨텍스트 파라미터를 지정할 수 있습니다. |
| `resultFormat` | 결과 포맷. `object`, `array`, `objectLines`, `arrayLines`, `csv` 중 하나입니다. |
| `header` | `true`이면 첫 행에 컬럼 이름을 반환합니다. |
| `typesHeader` | `true`이면 Druid 런타임 타입 정보를 함께 반환합니다. `header: true`가 필요합니다. |
| `sqlTypesHeader` | `true`이면 SQL 타입 정보를 함께 반환합니다. `header: true`가 필요합니다. |
| `context` | 쿼리 컨텍스트 파라미터(`timeZone`, `queryId` 등)를 담는 JSON 객체입니다. |
| `parameters` | 파라미터화 쿼리(parameterized query)에 사용할 값의 배열. 각 원소는 `type`과 `value`를 가집니다. |

결과 포맷별 특징은 다음과 같습니다.

| `resultFormat` | 설명 | `Content-Type` |
| --- | --- | --- |
| `object` | JSON 객체의 배열 | `application/json` |
| `array` | JSON 배열의 배열 | `application/json` |
| `objectLines` | 줄바꿈으로 구분한 JSON 객체(newline-delimited) | — |
| `arrayLines` | 줄바꿈으로 구분한 JSON 배열 | — |
| `csv` | 쉼표로 구분한 값. 큰따옴표로 이스케이프합니다. | — |

요청 예시는 다음과 같습니다.

```json
{
  "query": "SELECT COUNT(*) AS cnt FROM wikipedia WHERE __time >= CURRENT_TIMESTAMP - INTERVAL '1' DAY",
  "resultFormat": "object",
  "header": true,
  "typesHeader": true,
  "sqlTypesHeader": true,
  "context": {
    "sqlTimeZone": "America/Los_Angeles"
  }
}
```

JSON 대신 `Content-Type: text/plain` 또는 `application/x-www-form-urlencoded`로 본문에 SQL 문만 담아 보낼 수도 있습니다. 이 경우 결과는 `object` 포맷의 `application/json`으로 반환됩니다.

응답 상태 코드는 다음과 같습니다.

| 코드 | 의미 |
| --- | --- |
| `200 OK` | 쿼리 성공. 결과와 메타데이터를 반환합니다. |
| `400 BAD REQUEST` | 잘못된 요청. 오류 상세를 반환합니다. |
| `500 INTERNAL SERVER ERROR` | 서버 오류. 오류 객체를 반환합니다. |

### 오류 처리와 응답 잘림

응답 본문을 전송하는 도중 오류가 발생하면 서버는 응답을 중간에 끊습니다. `objectLines`, `arrayLines` 같은 줄 단위 포맷에서 마지막 줄바꿈이 없다면 응답이 잘린 것이므로 오류로 처리해야 합니다. 정상적으로 완료된 응답은 항상 마지막 줄바꿈을 포함합니다.

오류 객체는 `error`, `errorMessage`, `errorClass`, `host` 필드를 포함합니다.

### 쿼리 취소

```
DELETE /druid/v2/sql/{sqlQueryId}
```

실행 중인 쿼리를 취소합니다. 성공하면 HTTP `202 ACCEPTED`를 반환합니다. 취소는 최선 노력(best-effort) 방식이라 요청 이후에도 쿼리가 잠시 계속 실행될 수 있습니다. 취소하려면 쿼리가 접근하는 모든 리소스에 대한 READ 권한이 필요합니다.

### 딥 스토리지 쿼리 (Query from deep storage)

딥 스토리지(deep storage)에 저장된 세그먼트를 비동기(async)로 조회하는 엔드포인트입니다.

**쿼리 제출**

```
POST /druid/v2/sql/statements
```

요청 본문은 `/druid/v2/sql`과 유사하며 다음 컨텍스트 파라미터를 추가로 사용합니다.

- `context.executionMode`: 현재 `ASYNC`만 지원합니다.
- `context.selectDestination`: 대용량 결과(약 3,000행 초과)는 `durableStorage`를 사용합니다.

응답은 `queryId`, `state`, `createdAt`, `schema`, `durationMs` 필드를 담은 JSON 객체입니다.

**쿼리 상태 조회**

```
GET /druid/v2/sql/statements/{queryId}
```

- `detail` (선택, boolean): 스테이지(stage), 카운터(counter), 경고(warning) 정보를 포함해 반환합니다.

완료된 쿼리의 응답에는 전체 행 수와 결과 페이지 목록(`pages` 배열)을 담은 `result` 객체가 포함됩니다.

**결과 조회**

```
GET /druid/v2/sql/statements/{queryId}/results
```

| 파라미터 | 설명 |
| --- | --- |
| `page` (선택, int) | 조회할 페이지 번호. 생략하면 전체 결과를 순서대로 반환합니다. |
| `resultFormat` (선택) | `arrayLines`, `objectLines`, `array`, `object`, `csv` 중 하나입니다. |
| `filename` (선택) | `Content-Disposition` 헤더를 붙여 파일로 내려받게 합니다. 최대 255자이며 특수 문자는 사용할 수 없습니다. |

성공 시 HTTP `200`을 반환하고, 쿼리가 없거나 실패한 경우 `404`를 반환합니다.

**쿼리 취소**

```
DELETE /druid/v2/sql/statements/{queryId}
```

성공적으로 취소하면 빈 본문과 함께 HTTP `202 ACCEPTED`를 반환합니다. 딥 스토리지 쿼리의 오류 객체에는 `errorCode`, `persona`, `category`, `context` 필드가 추가로 포함됩니다.

---

## SQL 인제스천 API

MSQ(멀티 스테이지 쿼리) 태스크 엔진으로 SQL 기반 인제스천을 실행하는 API입니다.

### 쿼리(태스크) 제출

```
POST /druid/v2/sql/task
```

MSQ 태스크 엔진에 SQL 요청을 제출합니다. `query`, `context`, `parameters` 필드를 사용하는 JSON-over-HTTP 형식이며, INSERT, REPLACE, SELECT 문을 지원합니다.

주요 컨텍스트 파라미터는 다음과 같습니다.

- `maxNumTasks`: 병렬로 실행할 최대 태스크 수를 제한합니다.
- `finalizeAggregations`: 인제스천 시 집계(aggregation) 타입의 최종화 여부를 제어합니다.

컨텍스트 파라미터는 쿼리 문자열 안의 `SET` 문으로도 지정할 수 있습니다.

```sql
SET maxNumTasks=3; SET finalizeAggregations=false;
INSERT INTO ...
```

제출 성공 시 응답은 다음과 같습니다.

```json
{
  "taskId": "query-f795a235-4dc7-4fef-abac-3ae3f9686b79",
  "state": "RUNNING"
}
```

### 태스크 상태 조회

```
GET /druid/indexer/v1/task/{taskId}/status
```

`statusCode`(`RUNNING`, `SUCCESS`, `FAILED`)를 비롯한 태스크 메타데이터와 데이터소스 정보를 반환합니다.

### 태스크 리포트 조회

```
GET /druid/indexer/v1/task/{taskId}/reports
```

`multiStageQuery.payload` 아래에 실행 스테이지, 워커(worker) 상세, 카운터, 세그먼트 로딩 상태가 중첩된 구조로 담겨 있습니다. 리포트로 행 수, 전송 바이트, 병렬 워커별 처리 진행 상황을 추적할 수 있습니다.

### 태스크 취소

```
POST /druid/indexer/v1/task/{taskId}/shutdown
```

실행 중인 MSQ 태스크를 취소합니다. 응답은 다음과 같습니다.

```json
{
  "task": "query-655efe33-781a-4c50-ae84-c2911b42d63c"
}
```

---

## Tasks API

Overlord가 관리하는 인제스천 태스크의 조회·제출·종료 API입니다.

### 태스크 목록 조회

```
GET /druid/indexer/v1/tasks
```

모든 태스크를 조회하며, 다음 쿼리 파라미터로 필터링합니다.

| 파라미터 | 설명 |
| --- | --- |
| `state` | 태스크 상태로 필터링합니다. `running`, `complete`, `waiting`, `pending` 중 하나입니다. |
| `datasource` | 데이터소스 이름으로 필터링합니다. |
| `createdTimeInterval` | 생성 시각 기준 ISO-8601 인터벌. 구분자로 `_`를 사용합니다(예: `2023-06-27_2023-06-28`). |
| `max` | 반환할 완료(complete) 태스크의 최대 개수입니다. |
| `type` | 태스크 타입으로 필터링합니다. |

응답 예시는 다음과 같습니다.

```json
[
  {
    "id": "query-223549f8-b993-4483-b028-1b0d54713cad",
    "type": "query_worker",
    "status": "SUCCESS",
    "dataSource": "wikipedia_api"
  }
]
```

상태별 전용 엔드포인트도 제공합니다.

| 엔드포인트 | 설명 |
| --- | --- |
| `GET /druid/indexer/v1/completeTasks` | 완료된 태스크 목록. `?state=complete`와 동일하며 위 쿼리 파라미터를 지원합니다. |
| `GET /druid/indexer/v1/runningTasks` | 실행 중인 태스크 목록 |
| `GET /druid/indexer/v1/waitingTasks` | 대기(waiting) 상태 태스크 목록 |
| `GET /druid/indexer/v1/pendingTasks` | 보류(pending) 상태 태스크 목록 |

### 개별 태스크 조회

| 엔드포인트 | 설명 |
| --- | --- |
| `GET /druid/indexer/v1/task/{taskId}` | 태스크의 전체 설정(payload)과 스펙을 반환합니다. |
| `GET /druid/indexer/v1/task/{taskId}/status` | 실행 시간(duration), 위치(location), 오류 정보 등 상태 메타데이터를 반환합니다. |
| `GET /druid/indexer/v1/task/{taskId}/reports` | 인제스천 통계와 파싱 예외(parse exception)를 담은 완료 리포트를 반환합니다. |
| `GET /druid/indexer/v1/task/{taskId}/log` | 태스크 실행 생명주기 동안의 이벤트 로그를 반환합니다. `offset` 파라미터로 앞부분 항목을 제외할 수 있습니다. |

`GET /druid/indexer/v1/task/{taskId}/segments`는 더 이상 지원하지 않으며 `404`를 반환합니다. 대신 `segment/added/bytes` 메트릭을 사용합니다.

### 여러 태스크 상태 일괄 조회

```
POST /druid/indexer/v1/taskStatus
```

태스크 ID 배열을 본문으로 받아 각 태스크의 상태 객체를 반환합니다.

```json
["index_parallel_wikipedia_auto_jndhkpbo_2023-06-26T17:23:05.308Z"]
```

### 태스크 제출

```
POST /druid/indexer/v1/task
```

JSON 인제스천 스펙을 제출하고 태스크 ID를 반환받습니다.

```json
{
  "type": "index_parallel",
  "spec": {
    "dataSchema": {
      "dataSource": "wikipedia_auto",
      "granularitySpec": {
        "intervals": ["2015-09-12/2015-09-13"]
      }
    },
    "ioConfig": { "type": "index_parallel" },
    "tuningConfig": { "type": "index_parallel" }
  }
}
```

응답 예시는 다음과 같습니다.

```json
{"task": "index_parallel_wikipedia_odofhkle_2023-06-23T21:07:28.226Z"}
```

### 태스크 종료

| 엔드포인트 | 설명 |
| --- | --- |
| `POST /druid/indexer/v1/task/{taskId}/shutdown` | 지정한 태스크를 종료합니다. |
| `POST /druid/indexer/v1/datasources/{datasource}/shutdownAllTasks` | 지정한 데이터소스와 연관된 모든 태스크를 종료합니다. |

### 보류 세그먼트 정리

```
DELETE /druid/indexer/v1/pendingSegments/{datasource}
```

메타데이터 스토리지에서 보류 중인(pending) 세그먼트를 수동으로 정리하고, 삭제한 개수(`numDeleted`)를 반환합니다.

---

## Supervisor API

Kafka·Kinesis 같은 스트리밍 인제스천의 슈퍼바이저를 관리하는 API입니다.

### 슈퍼바이저 정보 조회

| 엔드포인트 | 설명 |
| --- | --- |
| `GET /druid/indexer/v1/supervisor` | 활성 슈퍼바이저 이름(ID)의 문자열 배열을 반환합니다. |
| `GET /druid/indexer/v1/supervisor?full` | 전체 스펙을 포함한 활성 슈퍼바이저 객체 배열을 반환합니다. |
| `GET /druid/indexer/v1/supervisor?state=true` | 각 슈퍼바이저의 `id`, `state`, `detailedState`, `healthy`, `suspended` 상태 객체 배열을 반환합니다. |
| `GET /druid/indexer/v1/supervisor/{supervisorId}` | 단일 슈퍼바이저의 스펙(`dataSchema`, `ioConfig`, `tuningConfig` 포함)을 반환합니다. |
| `GET /druid/indexer/v1/supervisor/{supervisorId}/status` | 태스크 상태와 최근 예외를 포함한 현재 상태 리포트를 반환합니다. |
| `GET /druid/indexer/v1/supervisor/{supervisorId}/health` | 슈퍼바이저 상태와 Overlord 설정 임계값을 기준으로 건강 상태를 반환합니다. 비정상이면 `503`을 반환합니다. |
| `GET /druid/indexer/v1/supervisor/{supervisorId}/stats` | 태스크별 인제스천 행 카운터의 스냅샷과 이동 평균을 반환합니다. 태스크 그룹 ID → 태스크 ID → 행 통계의 중첩 맵 구조입니다. |

### 감사 이력(audit history)

| 엔드포인트 | 설명 |
| --- | --- |
| `GET /druid/indexer/v1/supervisor/history` | 모든 슈퍼바이저의 스펙 감사 이력을 반환합니다. |
| `GET /druid/indexer/v1/supervisor/{supervisorId}/history` | 단일 슈퍼바이저의 감사 이력을 반환합니다. `count` 파라미터로 최근 n건만 조회할 수 있습니다. |

### 생성·수정

```
POST /druid/indexer/v1/supervisor
```

새 슈퍼바이저를 생성하거나 기존 스펙을 갱신합니다. 본문에는 `type`(`kafka` 또는 `kinesis`), `spec`(그 안에 `dataSchema`, `ioConfig`, 선택적 `tuningConfig`)을 담습니다. `?skipRestartIfUnmodified=true`를 지정하면 스펙이 변경되지 않은 경우 재시작을 생략합니다.

### 일시 중지와 재개

| 엔드포인트 | 설명 |
| --- | --- |
| `POST /druid/indexer/v1/supervisor/{supervisorId}/suspend` | 실행 중인 슈퍼바이저를 일시 중지합니다. 재개할 때까지 태스크가 중지된 상태로 유지되며, 그동안에도 슈퍼바이저는 로그와 메트릭을 계속 내보냅니다. |
| `POST /druid/indexer/v1/supervisor/suspendAll` | 모든 슈퍼바이저를 일시 중지합니다. 대상이 없어도 `200`을 반환합니다. |
| `POST /druid/indexer/v1/supervisor/{supervisorId}/resume` | 일시 중지된 슈퍼바이저의 인덱싱 태스크를 재개합니다. |
| `POST /druid/indexer/v1/supervisor/resumeAll` | 모든 슈퍼바이저를 재개합니다. 대상이 없어도 `200`을 반환합니다. |

### 리셋(reset)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/reset
```

슈퍼바이저가 실행 중일 때만 사용할 수 있습니다. 슈퍼바이저 메타데이터(저장된 오프셋)를 모두 지우고, `useEarliestOffset` 설정에 따라 가장 이른 위치 또는 가장 최근 위치부터 데이터를 다시 읽게 합니다. 저장된 오프셋을 지운 뒤 활성 태스크를 종료하고 재생성해 유효한 위치부터 읽기 시작합니다. 오프셋 유실(메시지 보존 기간 만료, 토픽 삭제 후 재생성 등)로 슈퍼바이저가 멈춘 상태를 복구할 때 사용하며, 메시지를 건너뛰어 데이터 유실이나 중복이 생길 수 있으므로 주의해서 사용해야 합니다.

### 오프셋 리셋(reset offsets)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/resetOffsets
```

전체를 리셋하지 않고 지정한 파티션의 오프셋만 리셋합니다. 저장된 오프셋이 없으면 지정한 오프셋을 메타데이터 스토리지에 기록합니다. 리셋 후에는 해당 파티션과 관련된 활성 태스크를 종료·재생성해 지정한 오프셋부터 읽기 시작하고, 지정하지 않은 파티션은 마지막 저장 오프셋부터 계속 읽습니다. 이 역시 메시지 누락으로 데이터 유실·중복이 생길 수 있습니다.

요청 본문(reset offsets metadata)의 구조는 다음과 같습니다.

| 필드 | 타입 | 설명 | 필수 |
| --- | --- | --- | --- |
| `type` | String | 페이로드 타입. 슈퍼바이저의 타입과 일치해야 하며 `kafka` 또는 `kinesis`입니다. | 예 |
| `partitions` | Object | 리셋 메타데이터 객체. 내부의 `type`은 `end`로 설정해 리셋할 끝(end) 시퀀스 번호를 나타냅니다. | 예 |

### 종료(terminate)

| 엔드포인트 | 설명 |
| --- | --- |
| `POST /druid/indexer/v1/supervisor/{supervisorId}/terminate` | 슈퍼바이저와 연관된 인덱싱 태스크를 종료하고 세그먼트 발행(publish)을 트리거합니다. 재시작 시 다시 로드되지 않도록 메타데이터 스토리지에 톰스톤(tombstone) 마커를 남기며, 종료된 슈퍼바이저는 메타데이터 스토리지에 남아 이력을 조회할 수 있습니다. 잘못된 ID이거나 실행 중이 아니면 `404`를 반환합니다. |
| `POST /druid/indexer/v1/supervisor/terminateAll` | 모든 슈퍼바이저를 종료합니다. 대상이 없어도 `200`을 반환합니다. |

### 조기 핸드오프(handoff)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/taskGroups/handoff
```

지정한 태스크 그룹의 핸드오프를 조기에 트리거합니다. 최선 노력 방식의 API로 핸드오프 실행을 보장하지 않으며, 요청이 수락되면 `202 ACCEPTED`를 반환하고 백그라운드에서 핸드오프를 시작합니다.

### 셧다운 (deprecated)

```
POST /druid/indexer/v1/supervisor/{supervisorId}/shutdown
```

슈퍼바이저를 종료하는 이전 방식의 엔드포인트입니다. deprecated 상태로 향후 릴리스에서 제거될 예정이므로, 동일한 기능의 terminate 엔드포인트를 사용해야 합니다.

---

## 데이터 관리 API

세그먼트의 사용(used)·미사용(unused) 상태 전환과 영구 삭제를 담당하는 API입니다. Overlord가 제공하는 API는 클러스터 전체에서 일관된 메타데이터를 보장합니다. 같은 데이터소스에 대해 인덱싱 태스크나 kill 태스크가 실행 중일 때는 이 API를 동시에 사용하지 않아야 합니다. 또한 세그먼트를 used로 표시하더라도 실제로 Historical 프로세스에 로드하려면 로드 규칙(load rule)이 별도로 설정되어 있어야 합니다.

### 단일 세그먼트

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `DELETE` | `/druid/indexer/v1/datasources/{datasource}/segments/{segmentId}` | 세그먼트를 unused로 표시합니다. 세그먼트가 존재하지 않아도 `200`을 반환하므로 응답 페이로드로 실제 결과를 확인해야 합니다. |
| `POST` | `/druid/indexer/v1/datasources/{datasource}/segments/{segmentId}` | 세그먼트를 used로 표시해 활성 상태로 되돌립니다. |

### 세그먼트 그룹

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `POST` | `/druid/indexer/v1/datasources/{datasource}/markUnused` | 여러 세그먼트를 unused로 표시합니다. |
| `POST` | `/druid/indexer/v1/datasources/{datasource}/markUsed` | 여러 세그먼트를 used로 표시합니다. |

두 엔드포인트 모두 요청 본문에 `segmentIds` 배열 또는 `interval`(ISO 8601 형식) 중 하나를 담습니다. 선택적으로 `versions` 파라미터를 지정해 세그먼트 버전으로 필터링할 수 있습니다.

```json
{
  "interval": "2015-09-12/2015-09-13"
}
```

### 데이터소스 전체

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `DELETE` | `/druid/indexer/v1/datasources/{datasource}` | 데이터소스의 모든 세그먼트를 unused로 표시합니다. Historical 프로세스에서 내리는 소프트 삭제(soft delete)입니다. |
| `POST` | `/druid/indexer/v1/datasources/{datasource}` | 다른 세그먼트에 가려지지(overshadowed) 않은 모든 unused 세그먼트를 used로 표시합니다. |

### 세그먼트 영구 삭제

```
DELETE /druid/coordinator/v1/datasources/{datasource}/intervals/{interval}
```

지정한 인터벌의 세그먼트를 영구 삭제하는 kill 태스크를 전송합니다. 인터벌은 ISO 8601 형식에 구분자로 `_`를 사용합니다(예: `2015-09-12_2015-09-13`). 성공 시 빈 본문과 함께 HTTP `200`을 반환합니다.

---

## 서비스 상태 API

클러스터의 각 서비스(프로세스) 상태를 확인하는 API입니다.

### 공통 엔드포인트 (모든 서비스)

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `GET` | `/status` | Druid 버전, 로드된 익스텐션(extension), 메모리 사용량 등을 반환합니다. |
| `GET` | `/status/health` | 서비스 동작 여부를 확인하는 단순 헬스 체크입니다. |
| `GET` | `/status/properties` | 현재 설정 프로퍼티를 반환합니다. |
| `GET` | `/status/selfDiscovered/status` | 노드가 클러스터에 자기 탐색(self-discovery)으로 합류했는지를 JSON으로 반환합니다. |
| `GET` | `/status/selfDiscovered` | 노드 탐색 여부를 HTTP 상태 코드로 반환합니다. |

### Coordinator

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `GET` | `/druid/coordinator/v1/leader` | 현재 리더 Coordinator의 주소를 반환합니다. |
| `GET` | `/druid/coordinator/v1/isLeader` | 해당 서버가 현재 리더이면 `true`를 반환합니다. |
| `GET` | `/druid/coordinator/v1/config/cloneStatus` | Historical 클로닝(cloning)의 현재 상태를 반환합니다. |
| `GET` | `/druid/coordinator/v1/config/syncedBrokers` | 최신 설정으로 동기화된 Broker 목록을 반환합니다. |

### Overlord

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `GET` | `/druid/indexer/v1/leader` | 현재 리더 Overlord의 주소를 반환합니다. |
| `GET` | `/druid/indexer/v1/isLeader` | 해당 서버가 현재 리더 Overlord인지 반환합니다. |

### Historical / Broker 로드 상태

| 메서드 | 엔드포인트 | 설명 |
| --- | --- | --- |
| `GET` | `/druid/historical/v1/loadstatus` | 로컬 캐시의 모든 세그먼트가 로드되었는지 반환합니다. |
| `GET` | `/druid/historical/v1/readiness` | Historical의 세그먼트 준비 상태를 HTTP 상태 코드로 반환합니다. |
| `GET` | `/druid/broker/v1/loadstatus` | Broker가 클러스터의 모든 세그먼트를 인지하고 있는지를 나타내는 플래그를 반환합니다. |
| `GET` | `/druid/broker/v1/readiness` | Broker가 쿼리를 받을 준비가 되었는지를 HTTP 상태 코드로 반환합니다. |

---

## 참고 자료

- [API reference 개요](https://druid.apache.org/docs/latest/api-reference/)
- [Druid SQL API](https://druid.apache.org/docs/latest/api-reference/sql-api)
- [SQL-based ingestion API](https://druid.apache.org/docs/latest/api-reference/sql-ingestion-api)
- [Tasks API](https://druid.apache.org/docs/latest/api-reference/tasks-api)
- [Supervisor API](https://druid.apache.org/docs/latest/api-reference/supervisor-api)
- [Data management API](https://druid.apache.org/docs/latest/api-reference/data-management-api)
- [Service status API](https://druid.apache.org/docs/latest/api-reference/service-status-api)
- [JSON querying API](https://druid.apache.org/docs/latest/api-reference/json-querying-api)
- [SQL JDBC driver API](https://druid.apache.org/docs/latest/api-reference/sql-jdbc)
