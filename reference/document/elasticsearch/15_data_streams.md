# Elasticsearch 데이터 스트림 (Data Streams)

> 이 문서는 Elasticsearch 9.x 공식 문서의 "Data Streams" 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/docs/manage-data/data-store/data-streams

## 목차

1. [개요](#개요)
2. [데이터 스트림이란?](#데이터-스트림이란)
3. [백킹 인덱스 (Backing Indices)](#백킹-인덱스-backing-indices)
4. [쓰기 인덱스 (Write Index)](#쓰기-인덱스-write-index)
5. [세대 (Generation)](#세대-generation)
6. [롤오버 (Rollover)](#롤오버-rollover)
7. [데이터 스트림 설정](#데이터-스트림-설정)
8. [데이터 스트림 사용](#데이터-스트림-사용)
9. [데이터 스트림 수정](#데이터-스트림-수정)
10. [시계열 데이터 스트림 (TSDS)](#시계열-데이터-스트림-tsds)
11. [로그 데이터 스트림 (LogsDB)](#로그-데이터-스트림-logsdb)
12. [데이터 스트림 수명 주기](#데이터-스트림-수명-주기)

---

## 개요

데이터 스트림 은 추가 전용(append-only) 시계열 데이터를 저장하도록 최적화된 인덱스 집합 위에 있는 추상화 계층입니다. 데이터 스트림에 인덱싱되는 모든 문서는 `date` 또는 `date_nanos` 필드 타입으로 매핑된 `@timestamp` 필드를 포함해야 합니다.

데이터 스트림은 로그, 이벤트, 메트릭 및 기타 지속적으로 생성되는 데이터에 적합합니다.

### 데이터 스트림의 주요 장점

- 단일 명명 리소스: 여러 인덱스에 걸쳐 추가 전용 시계열 데이터를 저장하면서 요청에 대해 단일 명명 리소스를 제공합니다.
- 자동 라우팅: 인덱싱 및 검색 요청을 데이터 스트림에 직접 제출할 수 있으며, 스트림이 자동으로 데이터를 저장하는 백킹 인덱스로 요청을 라우팅합니다.
- 자동화된 관리: 인덱스 수명 주기 관리(ILM) 또는 데이터 스트림 수명 주기를 사용하여 백킹 인덱스 관리를 자동화할 수 있습니다.

---

## 데이터 스트림이란?

데이터 스트림은 추가 전용 시계열 데이터를 여러 인덱스에 저장하면서 단일 명명 리소스를 통해 요청을 처리할 수 있게 해줍니다.

### 데이터 스트림의 구성 요소

```
데이터 스트림: my-data-stream
├── .ds-my-data-stream-2024.01.01-000001 (백킹 인덱스)
├── .ds-my-data-stream-2024.01.02-000002 (백킹 인덱스)
├── .ds-my-data-stream-2024.01.03-000003 (백킹 인덱스)
└── .ds-my-data-stream-2024.01.04-000004 (쓰기 인덱스 - 현재)
```

### 인덱스 모드

데이터 스트림의 인덱스 모드는 새로 생성되는 백킹 인덱스에 사용됩니다. 가능한 값은 다음과 같습니다:

- `standard`: 표준 모드 (기본값)
- `time_series`: 시계열 데이터 스트림 모드
- `logsdb`: 로그 데이터 최적화 모드
- `lookup`: 조회 전용 모드

---

## 백킹 인덱스 (Backing Indices)

데이터 스트림은 하나 이상의 숨겨진(hidden) 자동 생성 백킹 인덱스로 구성됩니다.

### 백킹 인덱스 명명 규칙

백킹 인덱스가 생성될 때 다음 규칙에 따라 이름이 지정됩니다:

```
.ds-<데이터-스트림-이름>-<yyyy.MM.dd>-<세대>
```

예를 들어, `web-server-logs` 데이터 스트림의 세대가 34이고 가장 최근 백킹 인덱스가 2099년 3월 7일에 생성되었다면:

```
.ds-web-server-logs-2099.03.07-000034
```

### 백킹 인덱스 특성

- 높은 세대 번호를 가진 백킹 인덱스는 더 최근의 데이터를 포함합니다.
- 백킹 인덱스의 이름 패턴은 구현 세부 사항이며, 유일하게 보장되는 것은 각 데이터 스트림 세대 인덱스가 고유한 이름을 가진다는 것입니다.
- 축소(shrink) 또는 복원(restore)과 같은 일부 작업은 백킹 인덱스의 이름을 변경할 수 있습니다. 이러한 이름 변경은 백킹 인덱스를 데이터 스트림에서 제거하지 않습니다.

### 중요 참고 사항

- 세대는 새 인덱스가 데이터 스트림에 추가되지 않아도 변경될 수 있습니다(예: 기존 백킹 인덱스가 축소될 때).
- 일부 세대의 백킹 인덱스는 존재하지 않을 수 있습니다.

---

## 쓰기 인덱스 (Write Index)

가장 최근에 생성된 백킹 인덱스가 데이터 스트림의 쓰기 인덱스 입니다.

### 쓰기 인덱스 특성

- 스트림은 새 문서를 이 인덱스에만 추가합니다.
- 인덱스에 직접 요청을 보내더라도 다른 백킹 인덱스에는 새 문서를 추가할 수 없습니다.
- 쓰기 인덱스는 제거할 수 없습니다.

### 쓰기 인덱스 변경

롤오버가 발생하면 새 백킹 인덱스가 생성되어 스트림의 새 쓰기 인덱스가 됩니다.

---

## 세대 (Generation)

각 데이터 스트림은 세대를 추적합니다: 000001부터 시작하는 6자리의 0이 채워진 정수입니다.

### 세대 증가

롤오버가 발생할 때마다 데이터 스트림의 세대가 증가합니다.

### 예시

```
세대 1: .ds-my-logs-2024.01.01-000001
세대 2: .ds-my-logs-2024.01.08-000002
세대 3: .ds-my-logs-2024.01.15-000003
```

---

## 롤오버 (Rollover)

롤오버는 현재 쓰기 인덱스가 특정 크기 또는 기간에 도달할 때 새 백킹 인덱스를 생성하는 프로세스입니다.

### 롤오버의 중요성

로그나 메트릭과 같은 시계열 데이터 작업 시 단일 인덱스에 무기한으로 쓰면 성능과 리소스 사용에 문제가 발생할 수 있습니다. Elasticsearch는 인덱스 롤오버와 라우팅 할당을 사용하여 수집, 검색 및 저장을 최적화합니다.

### 롤오버 없이 발생하는 문제

- 검색 성능 저하
- 클러스터에 대한 높은 관리 부담
- 단일 인덱스의 지속적인 성장

### 롤오버 권장 사항

일관된 타임스탬프 범위를 위해 ILM 정책의 롤오버 액션에서 `max_age`를 지정하는 것이 좋습니다. 예를 들어, 롤오버 액션에 `max_age`를 1일로 설정하면 백킹 인덱스가 일관되게 하루 분량의 데이터를 포함합니다.

### 롤오버 발생 조건

다음 조건 중 하나 이상을 충족할 때 롤오버가 발생합니다:

- `max_age`: 인덱스가 특정 기간에 도달
- `max_docs`: 인덱스의 문서 수가 특정 값에 도달
- `max_primary_shard_size`: 프라이머리 샤드 크기가 특정 값에 도달

### 수동 롤오버

롤오버 API를 사용하여 데이터 스트림을 수동으로 롤오버할 수 있습니다:

```json
POST my-data-stream/_rollover
```

조건을 지정하지 않으면 Elasticsearch가 무조건적으로 롤오버를 수행합니다.

#### 조건부 롤오버 예시

```json
POST my-data-stream/_rollover
{
  "conditions": {
    "max_age": "7d",
    "max_docs": 1000,
    "max_primary_shard_size": "50gb"
  }
}
```

### 지연 롤오버 (Lazy Rollover)

`lazy` 옵션을 `true`로 설정하면 롤오버 액션이 데이터 스트림에 다음 쓰기 시 롤오버가 필요하다는 신호만 표시합니다. 이 옵션은 데이터 스트림에서만 허용됩니다.

---

## 데이터 스트림 설정

데이터 스트림을 설정하려면 일치하는 인덱스 템플릿이 필요합니다. 대부분의 경우 하나 이상의 컴포넌트 템플릿을 사용하여 인덱스 템플릿을 구성합니다.

### 1단계: 컴포넌트 템플릿 생성

컴포넌트 템플릿은 매핑, 설정, 별칭을 정의하는 재사용 가능한 구성 요소입니다. 일반적으로 매핑과 인덱스 설정에 별도의 컴포넌트 템플릿을 사용합니다.

#### 매핑용 컴포넌트 템플릿

```json
PUT _component_template/my-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date",
          "format": "date_optional_time||epoch_millis"
        },
        "message": {
          "type": "text"
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword"
            }
          }
        }
      }
    }
  },
  "_meta": {
    "description": "데이터 스트림용 매핑"
  }
}
```

#### 설정용 컴포넌트 템플릿

```json
PUT _component_template/my-settings
{
  "template": {
    "settings": {
      "index.lifecycle.name": "my-lifecycle-policy",
      "index.number_of_shards": 1,
      "index.number_of_replicas": 1
    }
  },
  "_meta": {
    "description": "데이터 스트림용 설정"
  }
}
```

### 2단계: 인덱스 템플릿 생성

컴포넌트 템플릿을 사용하여 인덱스 템플릿을 생성합니다.

```json
PUT _index_template/my-data-stream-template
{
  "index_patterns": ["my-data-stream*"],
  "data_stream": {},
  "composed_of": ["my-mappings", "my-settings"],
  "priority": 500,
  "_meta": {
    "description": "my-data-stream용 템플릿"
  }
}
```

### 인덱스 템플릿 요구 사항

- index_patterns: 데이터 스트림 이름과 일치하는 하나 이상의 인덱스 패턴
- data_stream: 템플릿이 데이터 스트림을 생성하도록 활성화 (빈 객체도 가능)
- composed_of: 매핑과 인덱스 설정을 포함하는 컴포넌트 템플릿 목록
- priority: 200보다 높은 우선순위 (내장 템플릿과의 충돌 방지)

### @timestamp 필드 요구 사항

`@timestamp` 필드에 대한 `date` 또는 `date_nanos` 매핑이 필요합니다. 매핑을 지정하지 않으면 Elasticsearch가 기본 옵션으로 `@timestamp`를 `date` 필드로 매핑합니다.

#### date_nanos 사용 시 제한 사항

`date_nanos` 필드 타입은 1970년부터 2262년 사이의 날짜만 저장할 수 있습니다. 집계 버킷은 `date_nanos` 필드를 쿼리하더라도 밀리초 해상도로 표시됩니다.

### 3단계: 데이터 스트림 생성

데이터 스트림을 생성하는 방법은 두 가지입니다:

#### 방법 1: 문서 인덱싱을 통한 자동 생성

```json
POST my-data-stream/_doc
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "message": "첫 번째 로그 메시지",
  "host": {
    "name": "server-01"
  }
}
```

#### 방법 2: 데이터 스트림 API를 통한 명시적 생성

```json
PUT _data_stream/my-data-stream
```

### 템플릿 병합 순서

여러 컴포넌트 템플릿이 `composed_of` 필드에 지정되면 지정된 순서대로 병합됩니다. 즉, 나중에 지정된 컴포넌트 템플릿이 이전 컴포넌트 템플릿을 재정의합니다.

1. 컴포넌트 템플릿이 순서대로 병합
2. 부모 인덱스 템플릿의 구성이 다음에 병합
3. 마지막으로 인덱스 요청 자체의 구성이 병합

매핑 정의는 재귀적으로 병합됩니다.

### 내장 컴포넌트 템플릿

Elasticsearch에는 다음과 같은 내장 컴포넌트 템플릿이 포함되어 있습니다:

- `logs-mappings`
- `logs-settings`
- `metrics-mappings`
- `metrics-settings`
- `synthetics-mapping`
- `synthetics-settings`

### Kibana에서 인덱스 템플릿 생성

Kibana에서 인덱스 템플릿을 생성하려면:

1. 메인 메뉴 열기
2. Stack Management > Index Management로 이동
3. Index Templates 뷰에서 "Create template" 클릭

---

## 데이터 스트림 사용

데이터 스트림을 설정한 후 다음 작업을 수행할 수 있습니다:

### 문서 추가

#### 단일 문서 추가

인덱스 API와 함께 POST를 사용합니다:

```json
POST my-data-stream/_doc
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "message": "로그 메시지",
  "host": {
    "name": "server-01"
  }
}
```

중요: 인덱스 API의 `PUT /<target>/_doc/<_id>` 형식을 사용하여 데이터 스트림에 새 문서를 추가할 수 없습니다.

#### 문서 ID 지정

문서 ID를 지정하려면 `PUT /<target>/_create/<_id>` 형식을 사용합니다:

```json
PUT my-data-stream/_create/my-doc-id
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "message": "특정 ID를 가진 문서"
}
```

#### 대량 문서 추가

단일 요청으로 여러 문서를 추가하려면 bulk API를 사용합니다. create 액션만 지원됩니다.

```json
POST my-data-stream/_bulk
{"create":{}}
{"@timestamp":"2024-01-15T10:30:00.000Z","message":"첫 번째 메시지"}
{"create":{}}
{"@timestamp":"2024-01-15T10:31:00.000Z","message":"두 번째 메시지"}
{"create":{}}
{"@timestamp":"2024-01-15T10:32:00.000Z","message":"세 번째 메시지"}
```

### 데이터 스트림 검색

읽기 요청을 데이터 스트림에 제출하면 스트림이 모든 백킹 인덱스로 요청을 라우팅합니다.

```json
GET my-data-stream/_search
{
  "query": {
    "match": {
      "message": "로그"
    }
  }
}
```

#### 닫힌 백킹 인덱스

닫힌 백킹 인덱스는 데이터 스트림을 검색하더라도 검색할 수 없습니다. 닫힌 인덱스의 문서를 업데이트하거나 삭제할 수도 없습니다.

### 데이터 스트림 통계 가져오기

데이터 스트림 통계 API를 사용하여 하나 이상의 데이터 스트림에 대한 통계를 검색합니다:

```json
GET _data_stream/my-data-stream/_stats
```

모든 데이터 스트림의 통계:

```json
GET _data_stream/_stats
```

#### 응답 정보

- `data_stream_count`: 데이터 스트림 수
- `backing_indices`: 백킹 인덱스의 총 크기(바이트)
- `total_store_size`: 선택된 데이터 스트림의 모든 샤드 총 크기
- `maximum_timestamp`: 데이터 스트림의 가장 높은 `@timestamp` 값

### 문서 업데이트 및 삭제

데이터 스트림은 기존 데이터가 거의 업데이트되지 않는 사용 사례를 위해 설계되었습니다. 기존 문서에 대한 업데이트 또는 삭제 요청을 데이터 스트림에 직접 보낼 수 없습니다.

#### 쿼리별 업데이트

데이터 스트림에서 많은 수의 문서를 업데이트해야 하는 경우 update by query API를 사용할 수 있습니다:

```json
POST my-data-stream/_update_by_query
{
  "query": {
    "match": {
      "host.name": "old-server"
    }
  },
  "script": {
    "source": "ctx._source.host.name = 'new-server'"
  }
}
```

#### 쿼리별 삭제

```json
POST my-data-stream/_delete_by_query
{
  "query": {
    "match": {
      "message": "error"
    }
  }
}
```

#### 개별 문서 업데이트/삭제

개별 문서를 업데이트하거나 삭제하려면 문서가 포함된 백킹 인덱스에 직접 요청을 제출해야 합니다.

1. 먼저 `seq_no_primary_term: true`로 검색하여 필요한 시퀀스 번호와 프라이머리 텀을 가져옵니다:

```json
GET my-data-stream/_search
{
  "seq_no_primary_term": true,
  "query": {
    "match": {
      "_id": "my-doc-id"
    }
  }
}
```

2. 백킹 인덱스에 직접 업데이트/삭제 요청:

```json
PUT .ds-my-data-stream-2024.01.15-000001/_doc/my-doc-id?if_seq_no=10&if_primary_term=1
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "message": "업데이트된 메시지"
}
```

#### bulk API를 사용한 다중 문서 업데이트/삭제

```json
POST _bulk
{"delete":{"_index":".ds-my-data-stream-2024.01.15-000001","_id":"doc1"}}
{"index":{"_index":".ds-my-data-stream-2024.01.15-000001","_id":"doc2","if_seq_no":5,"if_primary_term":1}}
{"@timestamp":"2024-01-15T10:30:00.000Z","message":"업데이트된 문서"}
```

### 닫힌 백킹 인덱스 열기

닫힌 백킹 인덱스를 다시 열려면 open index API를 사용합니다:

#### 단일 백킹 인덱스 열기

```json
POST .ds-my-data-stream-2024.01.15-000001/_open
```

#### 데이터 스트림의 모든 닫힌 백킹 인덱스 열기

```json
POST my-data-stream/_open
```

---

## 데이터 스트림 수정

각 데이터 스트림에는 일치하는 인덱스 템플릿이 있습니다. 이 템플릿의 매핑과 인덱스 설정은 스트림을 위해 생성되는 새 백킹 인덱스에 적용됩니다.

### 동적 설정 및 매핑 변경

대부분의 동적 인덱스 설정과 새 필드 추가와 같은 매핑 변경은 인덱스 템플릿을 업데이트한 후 적용됩니다. 변경 사항은 이후에 생성되는 새 백킹 인덱스에만 적용됩니다.

### 정적 설정 및 기존 필드 매핑 변경

기존 필드 매핑 수정이나 정적 인덱스 설정 변경은 종종 데이터 스트림의 백킹 인덱스에 변경 사항을 적용하기 위해 재인덱싱이 필요합니다.

### 재인덱싱 (Reindex)

재인덱싱을 사용하여 데이터 스트림의 매핑이나 설정을 변경할 수 있습니다.

#### 재인덱싱 단계

1. 원하는 매핑 또는 설정 변경 사항이 포함되도록 인덱스 템플릿을 생성하거나 업데이트합니다.
2. 기존 데이터 스트림을 템플릿과 일치하는 새 스트림으로 재인덱싱합니다.

```json
POST _reindex
{
  "source": {
    "index": "my-data-stream"
  },
  "dest": {
    "index": "my-new-data-stream",
    "op_type": "create"
  }
}
```

중요: 데이터 스트림은 추가 전용이므로 재인덱싱 시 `op_type`을 `create`로 설정해야 합니다. 재인덱싱은 데이터 스트림의 기존 문서를 업데이트할 수 없습니다.

#### 재인덱싱 요구 사항

- 재인덱싱은 소스의 모든 문서에 대해 `_source`가 활성화되어 있어야 합니다.
- 대상은 재인덱싱 API를 호출하기 전에 원하는 대로 구성되어야 합니다.
- 재인덱싱은 소스나 연관된 템플릿에서 설정을 복사하지 않습니다.
- 매핑, 샤드 수, 레플리카 등은 미리 구성해야 합니다.

### Modify Data Streams API

백킹 인덱스를 데이터 스트림에 추가하거나 제거하려면 Modify Data Streams API를 사용합니다:

```json
POST _data_stream/_modify
{
  "actions": [
    {
      "remove_backing_index": {
        "data_stream": "my-data-stream",
        "index": ".ds-my-data-stream-2024.01.15-000001"
      }
    },
    {
      "add_backing_index": {
        "data_stream": "my-data-stream",
        "index": ".ds-my-data-stream-2024.01.15-000001-downsample"
      }
    }
  ]
}
```

#### 액션 설명

- add_backing_index: 기존 인덱스를 데이터 스트림의 백킹 인덱스로 추가합니다. 인덱스는 이 작업의 일부로 숨겨집니다.
  - 경고: `add_backing_index` 액션으로 인덱스를 추가하면 부적절한 데이터 스트림 동작이 발생할 수 있습니다. 이것은 전문가 수준의 API로 간주되어야 합니다.
- remove_backing_index: 데이터 스트림에서 백킹 인덱스를 제거합니다. 인덱스는 이 작업의 일부로 숨김 해제됩니다. 데이터 스트림의 쓰기 인덱스는 제거할 수 없습니다.

### 분석기 변경

데이터 스트림의 쓰기 인덱스는 닫을 수 없습니다. 데이터 스트림의 쓰기 인덱스와 향후 백킹 인덱스에 대한 분석기를 업데이트하려면:

1. 스트림에서 사용하는 인덱스 템플릿에서 분석기를 업데이트합니다.
2. 데이터 스트림을 롤오버하여 새 분석기를 스트림의 쓰기 인덱스와 향후 백킹 인덱스에 적용합니다.

```json
POST my-data-stream/_rollover
```

### 데이터 스트림 삭제

데이터 스트림과 그 백킹 인덱스를 삭제하려면:

```json
DELETE _data_stream/my-data-stream
```

와일드카드 표현식(`*`)이 지원됩니다:

```json
DELETE _data_stream/logs-*
```

경고: 데이터 스트림 삭제 API 요청은 데이터 스트림뿐만 아니라 스트림의 백킹 인덱스와 포함된 모든 데이터도 삭제합니다.

---

## 시계열 데이터 스트림 (TSDS)

시계열 데이터 스트림(TSDS, Time Series Data Stream)은 메트릭 데이터와 같은 시계열 데이터를 더 효율적으로 저장하도록 설계된 특별한 유형의 데이터 스트림입니다.

### TSDS의 주요 특징

- 차원 기반 라우팅: 라우팅 로직이 차원 필드를 사용하여 시계열의 모든 데이터 포인트를 동일한 샤드에 매핑하여 저장 효율성과 쿼리 성능을 향상시킵니다.
- 중복 데이터 거부: 중복 데이터 포인트가 거부됩니다.
- 효율적인 저장: 차원과 메트릭을 사용하여 데이터를 최적화된 방식으로 저장합니다.

### 차원 (Dimensions)

차원은 시계열을 식별하는 필드입니다. 예를 들어, 호스트 이름, 서비스 이름, 지역 등이 차원이 될 수 있습니다.

### index.routing_path 설정

각 TSDS 백킹 인덱스 내에서 Elasticsearch는 `index.routing_path` 인덱스 설정을 사용하여 동일한 차원을 가진 문서를 동일한 샤드로 라우팅합니다.

TSDS의 일치하는 인덱스 템플릿을 생성할 때 하나 이상의 차원을 지정해야 합니다.

#### 자동 구성

명시적으로 지정하지 않으면 `index.routing_path`는 `time_series_dimension`이 `true`로 설정된 매핑에서 자동으로 설정됩니다.

인덱스 템플릿에서 `index.mode` 설정을 구성할 때 `index.routing_path` 설정은 `time_series_dimension` 속성이 활성화된 필드 매핑에서 자동으로 파생됩니다.

### TSDS 설정 예시

#### 컴포넌트 템플릿

```json
PUT _component_template/my-tsds-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword",
              "time_series_dimension": true
            }
          }
        },
        "service": {
          "properties": {
            "name": {
              "type": "keyword",
              "time_series_dimension": true
            }
          }
        },
        "cpu": {
          "properties": {
            "usage": {
              "type": "double",
              "time_series_metric": "gauge"
            }
          }
        },
        "memory": {
          "properties": {
            "usage": {
              "type": "double",
              "time_series_metric": "gauge"
            }
          }
        }
      }
    }
  }
}
```

#### 인덱스 템플릿

```json
PUT _index_template/my-tsds-template
{
  "index_patterns": ["metrics-*"],
  "data_stream": {},
  "composed_of": ["my-tsds-mappings"],
  "priority": 500,
  "template": {
    "settings": {
      "index.mode": "time_series"
    }
  }
}
```

### 라우팅 경로 제한

- `index.routing_path` 설정은 와일드카드 패턴(예: `dim.*`)을 허용하고 동적으로 새 필드와 일치시킬 수 있습니다.
- 그러나 Elasticsearch는 `index.routing_path` 값과 일치하는 스크립팅, 런타임 또는 비차원 필드를 추가하는 매핑 업데이트를 거부합니다.

### 컴포넌트 템플릿 고려 사항

`index.routing_path`가 정의된 경우 참조하는 필드는 `time_series_dimension` 속성이 활성화된 동일한 컴포넌트 템플릿에서 정의되어야 합니다. 각 컴포넌트 템플릿이 자체적으로 유효해야 하기 때문입니다.

### Pass-Through 필드

Pass-through 필드는 차원 컨테이너로 구성될 수 있습니다. 이 경우 하위 필드가 자동으로 라우팅 경로에 포함됩니다.

### 최적화된 라우팅 전략

이 전략은 샤드 라우팅 및 `_tsid` 생성을 위해 차원을 여러 번 처리하지 않도록 하여 향상된 수집 성능을 제공합니다. 사용할 때 `index.routing_path`가 설정되지 않고 샤드 라우팅이 전체 `_tsid`를 사용하여 샤드 핫스팟을 방지하는 데 도움이 될 수 있습니다.

### 사용자 지정 라우팅 제한

TSDS 문서는 사용자 지정 `_routing` 값을 지원하지 않습니다. 마찬가지로 TSDS의 매핑에서 `_routing` 값을 요구할 수 없습니다.

### 시간 경계 인덱스

시간 경계 인덱스는 특정 시간 범위의 데이터만 포함하도록 제한되어 쿼리 성능을 최적화합니다.

---

## 로그 데이터 스트림 (LogsDB)

로그 데이터 스트림은 로그 데이터를 더 효율적으로 저장하는 데이터 스트림 유형입니다.

### LogsDB 인덱스 모드

버전 9.0부터 `logsdb` 인덱스 모드는 `logs-*-*` 패턴과 일치하는 이름을 가진 데이터 스트림에 자동으로 적용됩니다.

이 기본값은 버전 9.0 이상에서 생성된 Elasticsearch 인스턴스와 `logs-*-*` 패턴과 일치하는 데이터 스트림이 없었던 이전 인스턴스에 적용됩니다.

### 저장 효율성

벤치마크에서 로그 데이터 스트림에 저장된 로그 데이터는 일반 데이터 스트림보다 약 2.5배 적은 디스크 공간 을 사용했습니다. 인덱싱 성능에 작은(10-20%) 패널티가 있습니다.

정확한 영향은 데이터 세트와 Elasticsearch 버전에 따라 다릅니다.

### 저장 공간 절감

Elasticsearch의 logsdb 인덱스 모드는 logsdb 없이 최신 버전의 Elasticsearch와 비교하여 로그 데이터의 저장 공간을 최대 65%까지 감소 시킵니다.

내부 LogsDB 벤치마크에서 Elasticsearch 버전 8.17.0에서 출시된 LogsDB 성능과 비교하여:
- 약 16% 저장 공간 감소
- 약 19% 중앙값 인덱싱 처리량 증가

8.17의 표준 모드와 비교하면 LogsDB는 이제 최대 4배 더 효율적인 저장 이 가능하며 인덱싱 처리량 패널티는 최대 10%입니다.

### Synthetic _source

필요한 구독이 있는 경우 logsdb 인덱스 모드는 synthetic _source 를 사용하여 원본 `_source` 필드 저장을 생략합니다. 대신 문서 검색 시 doc values 또는 stored fields에서 문서 소스가 합성됩니다.

필요한 구독이 없는 경우 logsdb 모드는 원본 `_source` 필드를 사용합니다.

synthetic _source를 포함한 완전한 logsdb 기능은 서버리스 고객과 엔터프라이즈 라이선스를 보유한 조직에서 사용할 수 있습니다.

### 작동 방식

Logsdb는 다음을 통해 최적화합니다:
- 데이터 순서 최적화
- synthetic _source로 비저장 필드 값을 즉석에서 재구성하여 중복 제거
- 고급 알고리즘과 코덱으로 압축 개선
- Elasticsearch 내의 열 형식 저장을 활용하여 효율적인 로그 저장 및 검색

### 기본 설정

logsdb 인덱스 모드는 다음 설정을 사용합니다:

```json
{
  "index.mode": "logsdb",
  "index.mapping.synthetic_source_keep": "arrays",
  "index.sort.field": ["host.name", "@timestamp"],
  "index.sort.order": ["desc", "desc"],
  "index.codec": "best_compression",
  "index.mapping.ignore_malformed": true
}
```

### LogsDB 인덱스 모드 명시적 설정

```json
PUT _index_template/my-logsdb-template
{
  "index_patterns": ["logs-*-*"],
  "data_stream": {},
  "priority": 500,
  "template": {
    "settings": {
      "index.mode": "logsdb"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "message": {
          "type": "text"
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword"
            }
          }
        }
      }
    }
  }
}
```

### Synthetic _source와 Stored Fields

logsdb 인덱스 모드가 synthetic _source를 사용하고 매핑에서 필드에 대해 `doc_values`가 비활성화된 경우, Elasticsearch는 해당 필드에 대해 `store` 설정을 `true`로 설정할 수 있습니다. 이렇게 하면 synthetic source를 사용하여 문서 소스를 재구성할 때 필드 데이터에 계속 액세스할 수 있습니다.

예를 들어, `store`가 `false`이고 원본 값을 재구성하는 데 적합한 multi-field가 없는 text 필드에서 이 조정이 발생합니다.

---

## 데이터 스트림 수명 주기

데이터 스트림 수명 주기(DLM, Data Stream Lifecycle)는 버전 8.14부터 사용 가능하며, 기본 데이터 구조의 유지 관리를 자동으로 처리하는 데이터 스트림 관리 시스템입니다.

### ILM과의 비교

데이터 스트림 수명 주기는 ILM만큼 기능이 풍부하지 않습니다. 특히 현재 데이터 티어링, 축소(shrink) 또는 검색 가능한 스냅샷을 지원하지 않습니다. 그러나 이러한 특정 기능이 필요하지 않은 사용 사례는 데이터 스트림 수명 주기가 더 적합할 것입니다.

데이터 스트림 수명 주기는 데이터 티어와 같은 불필요한 하드웨어 중심 개념 없이 가장 일반적인 수명 주기 관리 요구 사항에 집중할 수 있는 최적화된 수명 주기 도구입니다.

### 주요 기능

#### 자동 롤오버

자동 롤오버는 들어오는 데이터를 더 작은 조각으로 나누어 더 나은 성능과 역호환되지 않는 매핑 변경을 용이하게 합니다.

#### 구성 가능한 보존

데이터가 보장되어 저장되는 기간을 구성할 수 있습니다. Elasticsearch는 이 기간보다 오래된 데이터를 나중에 삭제할 수 있습니다. 보존은 데이터 스트림 수준 또는 전역 수준에서 구성할 수 있습니다.

#### 다운샘플링

데이터 스트림 수명 주기는 데이터 스트림 백킹 인덱스의 다운샘플링도 지원합니다.

### 수명 주기 설정

#### 인덱스 템플릿에서 수명 주기 설정

```json
PUT _index_template/my-data-stream-template
{
  "index_patterns": ["my-data-stream*"],
  "data_stream": {},
  "priority": 500,
  "template": {
    "lifecycle": {
      "data_retention": "7d"
    }
  }
}
```

#### 수명 주기 API 사용

```json
PUT _data_stream/my-data-stream/_lifecycle
{
  "data_retention": "7d"
}
```

### 보존 및 다운샘플링 설정 예시

```json
PUT _data_stream/my-data-stream/_lifecycle
{
  "data_retention": "7d",
  "downsampling": [
    {
      "after": "1d",
      "fixed_interval": "10m"
    },
    {
      "after": "7d",
      "fixed_interval": "1d"
    }
  ]
}
```

### 다운샘플링 구성

다운샘플링은 다운샘플링 구성 객체의 선택적 배열입니다. 각 객체는 다음을 정의합니다:

- after: 백킹 인덱스가 다운샘플링될 시기를 나타내는 간격 (롤오버 이후 시간 프레임)
- fixed_interval: 다운샘플링 간격 (최소 `fixed_interval` 값은 5분)

최대 10개의 다운샘플링 라운드를 구성할 수 있습니다.

다운샘플링 액션은 인덱스 시계열 종료 시간이 지난 후에 실행됩니다.

### 보존 작동 방식

`data_retention`이 정의되면 이 데이터 스트림에 추가되는 모든 문서는 최소한 이 시간 동안 저장됩니다. 이 기간 이후 언제든지 문서가 삭제될 수 있습니다. 비어 있으면 이 데이터 스트림의 모든 문서가 무기한 저장됩니다.

모든 예약된 다운샘플링 라운드를 완료한 후 수명 주기가 실행될 때마다 백킹 인덱스가 데이터 보존 대상인지 검사합니다. 지정된 데이터 보존 기간이 경과하면(롤오버 시간 이후) 백킹 인덱스가 삭제됩니다.

### 수명 주기 정보 가져오기

```json
GET _data_stream/my-data-stream/_lifecycle
```

### 수명 주기 통계 가져오기

```json
GET _data_stream/_lifecycle/stats
```

---

## Data Stream API 참조

### 데이터 스트림 생성

```json
PUT _data_stream/my-data-stream
```

### 데이터 스트림 정보 가져오기

```json
GET _data_stream/my-data-stream
```

모든 데이터 스트림:

```json
GET _data_stream
```

와일드카드 사용:

```json
GET _data_stream/logs-*
```

### 데이터 스트림 통계

```json
GET _data_stream/my-data-stream/_stats
```

### 데이터 스트림 수명 주기 설정

```json
PUT _data_stream/my-data-stream/_lifecycle
{
  "data_retention": "30d"
}
```

### 데이터 스트림 수명 주기 가져오기

```json
GET _data_stream/my-data-stream/_lifecycle
```

### 데이터 스트림 수정

```json
POST _data_stream/_modify
{
  "actions": [
    {
      "add_backing_index": {
        "data_stream": "my-data-stream",
        "index": "my-index"
      }
    }
  ]
}
```

### 데이터 스트림 롤오버

```json
POST my-data-stream/_rollover
```

### 데이터 스트림 삭제

```json
DELETE _data_stream/my-data-stream
```

---

## 모범 사례

### 1. 수명 주기 관리 항상 구성

수명 주기 관리가 활성화되지 않으면 시계열 데이터 스트림이 롤오버되지 않는 매우 큰 인덱스로 성장할 수 있습니다. 이는 성능 문제를 일으킬 수 있습니다. Elastic Stack 프로덕션 배포에서는 항상 수명 주기 관리를 구성하세요.

### 2. 데이터 스트림 명명 체계 사용

Elastic 데이터 스트림 명명 체계를 사용하는 것이 좋습니다:

```
<type>-<dataset>-<namespace>
```

예시:
- `logs-nginx-production`
- `metrics-system-monitoring`
- `traces-apm-default`

### 3. 롤오버에 max_age 지정

일관된 타임스탬프 범위를 보장하기 위해 ILM 정책의 롤오버 액션에서 `max_age`를 지정하세요.

### 4. 적절한 인덱스 모드 선택

- 로그 데이터: `logsdb` 모드 사용 (저장 공간 최대 65% 절감)
- 메트릭 데이터: `time_series` 모드 사용 (차원 기반 최적화)
- 일반 데이터: `standard` 모드 사용

### 5. 컴포넌트 템플릿 재사용

매핑과 설정에 별도의 컴포넌트 템플릿을 사용하면 여러 인덱스 템플릿에서 재사용할 수 있습니다.

---

## 보안 요구 사항

Elasticsearch 보안 기능이 활성화된 경우 다음 인덱스 권한이 필요합니다:

### 문서 추가

- `create_doc`, `create`, `index` 또는 `write` 인덱스 권한

### 데이터 스트림 자동 생성

- `auto_configure`, `create_index` 또는 `manage` 인덱스 권한

### 데이터 스트림 통계 조회

- `monitor` 또는 `manage` 인덱스 권한

### 롤오버 수행

- `manage` 인덱스 권한

### 데이터 스트림 삭제

- `delete_index` 권한

---

## 참고 자료

- [Data Streams | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams)
- [Set up a data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/set-up-data-stream)
- [Use a data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/use-data-stream)
- [Time series data streams | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/time-series-data-stream-tsds)
- [Logs data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/logs-data-stream)
- [Data stream lifecycle | Elastic Docs](https://www.elastic.co/docs/manage-data/lifecycle/data-stream)
- [Modify a data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/modify-data-stream)
- [Templates | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/templates)
