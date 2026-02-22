# Elasticsearch Ingest Pipelines (수집 파이프라인)

## 개요

Elasticsearch 수집 파이프라인(Ingest Pipeline)은 데이터를 인덱싱하기 전에 일반적인 변환을 수행할 수 있게 해줍니다. 예를 들어, 파이프라인을 사용하여 필드를 제거하거나, 텍스트에서 값을 추출하거나, 데이터를 보강(enrich)할 수 있습니다.

파이프라인은 프로세서(processor) 라고 불리는 일련의 구성 가능한 작업으로 구성됩니다. 각 프로세서는 순차적으로 실행되어 들어오는 문서에 특정 변경을 가합니다. 프로세서가 실행된 후 Elasticsearch는 변환된 문서를 데이터 스트림 또는 인덱스에 추가합니다.

```
[수집 문서] → [프로세서 1] → [프로세서 2] → [프로세서 N] → [인덱스에 저장]
```

## 파이프라인 생성 및 관리

### 파이프라인 관리 방법

Kibana의 Ingest Pipelines 기능 또는 ingest API 를 사용하여 수집 파이프라인을 생성하고 관리할 수 있습니다. Elasticsearch는 파이프라인을 클러스터 상태(cluster state)에 저장합니다.

### 필수 조건

- Ingest 노드 역할: `ingest` 노드 역할이 있는 노드가 파이프라인 처리를 담당합니다. 수집 파이프라인을 사용하려면 클러스터에 최소 하나 이상의 `ingest` 역할이 있는 노드가 있어야 합니다.
- 대량 수집 부하: 대량 수집 부하의 경우 전용 수집 노드(dedicated ingest nodes)를 생성하는 것이 권장됩니다.
- 보안 권한: Elasticsearch 보안 기능이 활성화된 경우 수집 파이프라인을 관리하려면 `manage_pipeline` 클러스터 권한이 필요합니다.

### 파이프라인 생성 (PUT API)

파이프라인을 생성하려면 `PUT _ingest/pipeline` API를 사용합니다.

```json
PUT _ingest/pipeline/my-pipeline-id
{
  "description": "내 파이프라인 설명",
  "version": 1,
  "processors": [
    {
      "set": {
        "description": "my-keyword-field를 foo로 설정",
        "field": "my-keyword-field",
        "value": "foo"
      }
    }
  ],
  "on_failure": [
    {
      "set": {
        "field": "error.message",
        "value": "파이프라인 처리 중 오류 발생"
      }
    }
  ],
  "_meta": {
    "author": "kim",
    "purpose": "예제 파이프라인"
  }
}
```

### 파이프라인 구성 요소

| 필드 | 설명 |
|------|------|
| `description` | 수집 파이프라인에 대한 설명 |
| `version` | 외부 시스템에서 수집 파이프라인을 추적하는 데 사용되는 버전 번호 |
| `processors` | 인덱싱 전에 문서에 변환을 수행하는 프로세서 목록. 지정된 순서대로 순차적으로 실행됨 |
| `on_failure` | 프로세서 실패 직후 실행할 프로세서 목록 (파이프라인 수준 오류 처리) |
| `_meta` | 수집 파이프라인에 대한 선택적 메타데이터. 자유롭게 구성 가능 |

### 파이프라인 조회 (GET API)

```json
# 특정 파이프라인 조회
GET _ingest/pipeline/my-pipeline-id

# 모든 파이프라인 조회
GET _ingest/pipeline

# 와일드카드 사용
GET _ingest/pipeline/my-*
```

응답 예시:

```json
{
  "my-pipeline-id": {
    "description": "내 파이프라인 설명",
    "version": 1,
    "processors": [
      {
        "set": {
          "field": "my-keyword-field",
          "value": "foo"
        }
      }
    ]
  }
}
```

### 파이프라인 삭제 (DELETE API)

```json
# 특정 파이프라인 삭제
DELETE _ingest/pipeline/my-pipeline-id

# 모든 파이프라인 삭제
DELETE _ingest/pipeline/*
```

## 파이프라인 적용

### 인덱싱 요청에서 파이프라인 사용

`pipeline` 쿼리 파라미터를 사용하여 개별 또는 벌크 인덱싱 요청에 파이프라인을 적용합니다.

```json
# 단일 문서 인덱싱
PUT my-data-stream/_doc/1?pipeline=my-pipeline
{
  "message": "Hello, World!"
}

# 벌크 인덱싱
PUT my-data-stream/_bulk?pipeline=my-pipeline
{ "create": { } }
{ "message": "Hello, World 1!" }
{ "create": { } }
{ "message": "Hello, World 2!" }
```

### update_by_query 및 reindex에서 사용

`pipeline` 파라미터는 `update_by_query` 또는 `reindex` API와 함께 사용할 수도 있습니다.

```json
# update_by_query 예시
POST my-index/_update_by_query?pipeline=my-pipeline
{
  "query": {
    "match_all": {}
  }
}

# reindex 예시
POST _reindex
{
  "source": {
    "index": "source-index"
  },
  "dest": {
    "index": "dest-index",
    "pipeline": "my-pipeline"
  }
}
```

### 기본 및 최종 파이프라인 설정

#### 기본 파이프라인 (Default Pipeline)

`index.default_pipeline` 인덱스 설정을 사용하여 기본 파이프라인을 설정합니다. `pipeline` 파라미터가 지정되지 않은 인덱싱 요청에 이 파이프라인이 적용됩니다.

```json
PUT my-index
{
  "settings": {
    "index.default_pipeline": "my-default-pipeline"
  }
}
```

기본 파이프라인을 비활성화하려면 `pipeline` 파라미터를 `_none`으로 설정합니다.

```json
PUT my-index/_doc/1?pipeline=_none
{
  "message": "기본 파이프라인 없이 인덱싱"
}
```

#### 최종 파이프라인 (Final Pipeline)

`index.final_pipeline` 인덱스 설정을 사용하여 최종 파이프라인을 설정합니다. Elasticsearch는 요청 또는 기본 파이프라인 후에 이 파이프라인을 적용합니다. 둘 다 지정되지 않아도 최종 파이프라인은 실행됩니다.

```json
PUT my-index
{
  "settings": {
    "index.default_pipeline": "my-default-pipeline",
    "index.final_pipeline": "my-final-pipeline"
  }
}
```

중요: 최종 파이프라인은 `pipeline` 파라미터 값과 관계없이 항상 실행됩니다.

## 파이프라인에서 데이터 접근

### 소스 필드 접근

프로세서는 들어오는 문서의 소스 필드에 대해 읽기 및 쓰기 접근 권한을 가집니다. 프로세서에서 필드 키에 접근하려면 해당 필드 이름을 사용합니다.

```json
PUT _ingest/pipeline/my-pipeline
{
  "processors": [
    {
      "set": {
        "field": "my-long-field",
        "value": 10
      }
    }
  ]
}
```

`_source` 접두사를 사용할 수도 있으며, 점 표기법(dot notation)으로 객체 필드에 접근할 수 있습니다.

```json
PUT _ingest/pipeline/my-pipeline
{
  "processors": [
    {
      "set": {
        "field": "_source.my-object.my-nested-field",
        "value": "nested value"
      }
    }
  ]
}
```

### 메타데이터 필드 접근

Mustache 템플릿 스니펫을 사용하여 메타데이터 필드 값에 접근할 수 있습니다.

| 메타데이터 필드 | 설명 |
|----------------|------|
| `{{{_index}}}` | 대상 인덱스 |
| `{{{_id}}}` | 문서 ID (자동 생성 ID는 접근 불가) |
| `{{{_routing}}}` | 라우팅 값 |

중요: 문서 ID를 자동으로 생성하는 경우 프로세서에서 `{{{_id}}}`를 사용할 수 없습니다. Elasticsearch는 수집 후에 자동 생성된 `_id` 값을 할당합니다.

```json
PUT _ingest/pipeline/my-pipeline
{
  "processors": [
    {
      "set": {
        "field": "routing_info",
        "value": "{{{_routing}}}"
      }
    }
  ]
}
```

### 수집 메타데이터 접근

파이프라인은 기본적으로 `_ingest.timestamp` 수집 메타데이터 필드만 생성합니다. 이 필드는 Elasticsearch가 문서의 인덱싱 요청을 받은 타임스탬프를 포함합니다.

```json
PUT _ingest/pipeline/my-pipeline
{
  "processors": [
    {
      "set": {
        "description": "수집 타임스탬프를 'event.ingested'로 인덱싱",
        "field": "event.ingested",
        "value": "{{{_ingest.timestamp}}}"
      }
    }
  ]
}
```

참고: 소스 필드와 메타데이터 필드와 달리 Elasticsearch는 기본적으로 수집 메타데이터 필드를 인덱싱하지 않습니다. `_ingest.timestamp` 또는 다른 수집 메타데이터 필드를 인덱싱하려면 set 프로세서를 사용하세요.

#### 스크립트 프로세서에서의 접근

- `_ingest.timestamp`는 스크립트 프로세서를 제외한 모든 프로세서에서 `{{_ingest.timestamp}}` 형식으로 사용 가능
- 스크립트 프로세서에서는 `metadata().now`를 사용

현재 파이프라인의 이름은 `_ingest.pipeline` 수집 메타데이터 키에서 접근할 수 있습니다.

### `_ingest` 접두사가 있는 소스 필드 처리

데이터에 `_ingest` 키로 시작하는 소스 필드가 포함된 경우 `_source._ingest`를 사용하여 접근합니다.

## 프로세서 참조

Elasticsearch는 40개 이상의 구성 가능한 프로세서를 포함합니다. 아래는 주요 프로세서의 목록입니다.

### 프로세서 전체 목록

| 프로세서 | 설명 |
|----------|------|
| Append | 기존 배열에 값을 추가 |
| Attachment | 첨부 파일(PDF, Office 문서 등)에서 텍스트 추출 |
| Bytes | 사람이 읽을 수 있는 바이트 값을 바이트 단위로 변환 (예: 1kb → 1024) |
| Circle | 원 정의를 근사 다각형으로 변환 |
| Community ID | 네트워크 플로우 데이터에 대한 Community ID 계산 |
| Convert | 필드를 다른 데이터 타입으로 변환 |
| CSV | CSV 라인에서 필드 추출 |
| Date | 문자열에서 날짜 파싱 |
| Date index name | 날짜/타임스탬프 기반으로 올바른 시간 기반 인덱스 지정 |
| Dissect | 정규 표현식 없이 구조화된 필드 추출 |
| Dot expander | 점이 포함된 필드를 객체 필드로 확장 |
| Drop | 문서 삭제 (인덱싱 방지) |
| Enrich | 다른 인덱스의 데이터로 문서 보강 |
| Fail | 예외 발생 및 파이프라인 중지 |
| Fingerprint | 문서 콘텐츠의 해시 계산 |
| Foreach | 배열이나 객체의 각 요소에 프로세서 실행 |
| Geo-grid | 지오 그리드 정의를 GeoJSON으로 변환 |
| GeoIP | IP 주소에서 지리적 위치 정보 추가 |
| Grok | 정규 표현식 기반 패턴 매칭으로 구조화된 필드 추출 |
| Gsub | 정규 표현식을 적용하여 문자열 필드 변환 |
| HTML strip | HTML 태그 제거 |
| Inference | 사전 학습된 모델로 추론 수행 |
| IP Location | IP 주소에서 위치 정보 추가 |
| Join | 배열 요소를 단일 문자열로 결합 |
| JSON | JSON 문자열을 구조화된 JSON으로 변환 |
| KV | 키-값 쌍 추출 |
| Lowercase | 문자열을 소문자로 변환 |
| Network direction | 네트워크 방향 계산 |
| Pipeline | 다른 파이프라인 호출 |
| Redact | 민감한 정보 마스킹 |
| Registered domain | 등록된 도메인 추출 |
| Remove | 필드 제거 |
| Rename | 필드 이름 변경 |
| Reroute | 다른 인덱스나 데이터 스트림으로 문서 라우팅 |
| Script | Painless 스크립트 실행 |
| Set | 필드 값 설정 |
| Set security user | 보안 사용자 정보 설정 |
| Sort | 배열 요소 정렬 |
| Split | 문자열을 배열로 분할 |
| Terminate | 파이프라인 종료 |
| Trim | 문자열에서 공백 제거 |
| Uppercase | 문자열을 대문자로 변환 |
| URL decode | URL 인코딩된 문자열 디코딩 |
| URI parts | URI를 구성 요소로 파싱 |
| User agent | 사용자 에이전트 문자열 파싱 |

### 사용 가능한 프로세서 목록 조회

노드 정보 API를 사용하여 사용 가능한 프로세서 목록을 가져올 수 있습니다.

```json
GET _nodes/ingest?filter_path=nodes.*.ingest.processors
```

## 주요 프로세서 상세 설명

### Set 프로세서

필드에 값을 설정합니다.

```json
PUT _ingest/pipeline/set-example
{
  "processors": [
    {
      "set": {
        "field": "host.os.name",
        "value": "Linux"
      }
    }
  ]
}
```

동적 값 설정:

```json
{
  "set": {
    "field": "source",
    "value": "{{{_source.host.name}}}"
  }
}
```

### Remove 프로세서

기존 필드를 제거합니다. 필드가 존재하지 않으면 예외가 발생합니다.

```json
PUT _ingest/pipeline/remove-example
{
  "processors": [
    {
      "remove": {
        "field": "temporary_field",
        "ignore_failure": true
      }
    }
  ]
}
```

여러 필드 제거:

```json
{
  "remove": {
    "field": ["field1", "field2", "field3"]
  }
}
```

### Rename 프로세서

기존 필드의 이름을 변경합니다.

```json
PUT _ingest/pipeline/rename-example
{
  "processors": [
    {
      "rename": {
        "field": "provider",
        "target_field": "cloud.provider",
        "ignore_failure": true
      }
    }
  ]
}
```

### Convert 프로세서

필드를 다른 데이터 타입으로 변환합니다.

```json
PUT _ingest/pipeline/convert-example
{
  "processors": [
    {
      "convert": {
        "field": "bytes",
        "type": "integer"
      }
    }
  ]
}
```

지원되는 타입: `integer`, `long`, `float`, `double`, `string`, `boolean`, `auto`

### Grok 프로세서

Grok 프로세서는 단일 텍스트 필드 내에서 구조화된 필드를 추출합니다. Grok 패턴은 재사용 가능한 별칭 표현식을 지원하는 정규 표현식과 같습니다.

```json
PUT _ingest/pipeline/grok-example
{
  "description": "Apache 로그 파싱",
  "processors": [
    {
      "grok": {
        "field": "message",
        "patterns": ["%{COMBINEDAPACHELOG}"]
      }
    }
  ]
}
```

#### 사용자 정의 패턴

`pattern_definitions` 옵션을 사용하여 사용자 정의 패턴을 추가할 수 있습니다.

```json
PUT _ingest/pipeline/custom-grok
{
  "processors": [
    {
      "grok": {
        "field": "message",
        "patterns": ["my %{FAVORITE_DOG:dog} is colored %{RGB:color}"],
        "pattern_definitions": {
          "FAVORITE_DOG": "beagle",
          "RGB": "RED|GREEN|BLUE"
        }
      }
    }
  ]
}
```

#### 다중 패턴

하나의 패턴으로 필드의 잠재적 구조를 모두 캡처할 수 없는 경우 여러 패턴을 제공할 수 있습니다.

```json
{
  "grok": {
    "field": "message",
    "patterns": [
      "%{IP:client} %{WORD:method} %{URIPATHPARAM:request}",
      "%{IP:client} - %{DATA:user} \\[%{HTTPDATE:timestamp}\\]"
    ]
  }
}
```

#### trace_match 파라미터

어떤 패턴이 매칭되어 필드를 채웠는지 추적하려면 `trace_match` 파라미터를 사용합니다.

```json
{
  "grok": {
    "field": "message",
    "patterns": ["pattern1", "pattern2"],
    "trace_match": true
  }
}
```

이 추적 메타데이터는 어떤 패턴이 매칭되었는지 디버깅할 수 있게 해줍니다. 이 정보는 수집 메타데이터에 저장되며 인덱싱되지 않습니다.

#### 내장 패턴 조회

```json
GET _ingest/processor/grok
```

ECS 필드 이름을 추출하는 패턴을 반환하려면 `ecs_compatibility` 쿼리 파라미터에 `v1`을 지정합니다.

```json
GET _ingest/processor/grok?ecs_compatibility=v1
```

### Dissect 프로세서

Dissect 프로세서는 정규 표현식을 사용하지 않고 문서 내 단일 텍스트 필드에서 구조화된 필드를 추출합니다. 이로 인해 Dissect는 Grok보다 단순하고 종종 더 빠른 대안입니다.

```json
PUT _ingest/pipeline/dissect-example
{
  "processors": [
    {
      "dissect": {
        "field": "message",
        "pattern": "%{clientip} %{ident} %{auth} [%{timestamp}] \"%{verb} %{request} HTTP/%{httpversion}\" %{status} %{size}"
      }
    }
  ]
}
```

Dissect는 키 수정자도 지원합니다. 예를 들어 특정 필드를 무시하거나, 필드를 추가하거나, 패딩을 건너뛰도록 지시할 수 있습니다.

### Date 프로세서

문자열에서 날짜를 파싱하여 날짜 필드로 변환합니다.

```json
PUT _ingest/pipeline/date-example
{
  "processors": [
    {
      "date": {
        "field": "timestamp",
        "target_field": "@timestamp",
        "formats": ["dd/MMM/yyyy:HH:mm:ss Z"],
        "timezone": "Asia/Seoul"
      }
    }
  ]
}
```

### Script 프로세서

들어오는 문서에서 인라인 또는 저장된 스크립트를 실행합니다. 스크립트는 수집 컨텍스트에서 실행됩니다.

```json
PUT _ingest/pipeline/script-example
{
  "processors": [
    {
      "script": {
        "description": "'env' 필드에서 'tags' 추출",
        "lang": "painless",
        "source": """
          String[] envSplit = ctx['env'].splitOnToken(params['delimiter']);
          ArrayList tags = new ArrayList();
          tags.add(envSplit[params['position']].trim());
          ctx['tags'] = tags;
        """,
        "params": {
          "delimiter": "-",
          "position": 1
        }
      }
    }
  ]
}
```

#### Painless 스크립트에서 필드 접근

스크립트 프로세서는 들어오는 문서의 JSON 소스 필드를 맵, 리스트, 프리미티브 세트로 파싱합니다. Painless 스크립트에서 이러한 필드에 접근하려면 맵 접근 연산자를 사용합니다.

```painless
ctx['my-field']
// 또는 단축형
ctx.my_field
```

### Foreach 프로세서

배열이나 객체 요소의 수를 알 수 없는 경우 각 요소를 동일한 방식으로 처리하기 어려울 수 있습니다. foreach 프로세서를 사용하면 배열 또는 객체 값을 포함하는 필드와 각 요소에서 실행할 프로세서를 지정할 수 있습니다.

```json
PUT _ingest/pipeline/foreach-example
{
  "processors": [
    {
      "foreach": {
        "field": "values",
        "processor": {
          "uppercase": {
            "field": "_ingest._value"
          }
        }
      }
    }
  ]
}
```

배열 또는 객체를 반복할 때 foreach 프로세서는 현재 요소의 값을 `_ingest._value` 수집 메타데이터 필드에 저장합니다. `_ingest._value`는 자식 필드를 포함한 전체 요소 값을 포함합니다. `_ingest._value` 필드에서 점 표기법을 사용하여 자식 필드 값에 접근할 수 있습니다.

객체를 반복할 때 foreach 프로세서는 현재 요소의 키를 `_ingest._key`에 문자열로 저장합니다.

### Drop 프로세서

오류를 발생시키지 않고 문서를 삭제합니다. 특정 조건에 따라 문서가 인덱싱되는 것을 방지하는 데 유용합니다.

```json
PUT _ingest/pipeline/drop-example
{
  "processors": [
    {
      "drop": {
        "if": "ctx.network_name == 'Guest'"
      }
    }
  ]
}
```

저장된 스크립트를 사용한 고급 예시:

```json
PUT _scripts/my-prod-tag-script
{
  "script": {
    "lang": "painless",
    "source": """
      Collection tags = ctx.tags;
      if(tags != null){
        for (String tag : tags) {
          if (tag.toLowerCase().contains('prod')) {
            return false;
          }
        }
      }
      return true;
    """
  }
}

PUT _ingest/pipeline/drop-with-script
{
  "processors": [
    {
      "drop": {
        "description": "'prod' 태그가 없는 문서 삭제",
        "if": { "id": "my-prod-tag-script" }
      }
    }
  ]
}
```

### Reroute 프로세서

수집 중에 문서를 다른 대상 인덱스 또는 데이터 스트림으로 라우팅합니다.

```json
PUT _ingest/pipeline/reroute-example
{
  "processors": [
    {
      "reroute": {
        "tag": "nginx",
        "if": "ctx?.log?.file?.path?.contains('nginx')",
        "dataset": "nginx"
      }
    }
  ]
}
```

폴백 값 사용:

```json
{
  "reroute": {
    "dataset": ["{{service.name}}", "generic"],
    "namespace": "default"
  }
}
```

중요: reroute 프로세서가 실행된 후 현재 파이프라인의 다른 모든 프로세서는 건너뜁니다(최종 파이프라인 포함). 이는 파이프라인 내에서 최대 하나의 reroute 프로세서만 실행되어 if, else-if, else-if 조건과 유사한 상호 배타적 라우팅 조건을 정의할 수 있게 합니다.

### Pipeline 프로세서

다른 파이프라인을 호출하는 특수 프로세서입니다.

```json
PUT _ingest/pipeline/main-pipeline
{
  "processors": [
    {
      "pipeline": {
        "name": "inner-pipeline"
      }
    }
  ]
}
```

조건부 파이프라인 호출:

```json
{
  "pipeline": {
    "name": "nginx-pipeline",
    "if": "ctx?.field1 == 'value1'"
  }
}
```

### Enrich 프로세서

다른 인덱스의 데이터로 문서를 보강합니다. enrich 프로세서를 포함하는 파이프라인은 추가 설정이 필요합니다.

1. 소스 인덱스 생성
2. enrich 정책 생성
3. enrich 인덱스 생성 (정책 실행)
4. 파이프라인에 enrich 프로세서 추가

```json
PUT _ingest/pipeline/enrich-example
{
  "processors": [
    {
      "enrich": {
        "policy_name": "users-policy",
        "field": "email",
        "target_field": "user"
      }
    }
  ]
}
```

참고: enrich 인덱스는 생성 후 업데이트하거나 문서를 추가할 수 없습니다. 대신 소스 인덱스를 업데이트하고 enrich 정책을 다시 실행하여 업데이트된 소스 인덱스에서 새 enrich 인덱스를 생성합니다.

### GeoIP 프로세서

Maxmind GeoLite2 City Database의 데이터를 기반으로 IP 주소의 지리적 위치 정보를 추가합니다.

```json
PUT _ingest/pipeline/geoip-example
{
  "processors": [
    {
      "geoip": {
        "field": "ip"
      }
    }
  ]
}
```

참고: 이 프로세서가 위도와 경도를 포함하는 location 필드로 문서를 보강하지만, 매핑에서 명시적으로 정의하지 않으면 이 필드는 Elasticsearch에서 `geo_point` 타입으로 인덱싱되지 않습니다.

Elasticsearch는 Elastic GeoIP 엔드포인트에서 이 데이터베이스의 업데이트를 자동으로 다운로드합니다: `https://geoip.elastic.co/v1/database`

### User Agent 프로세서

사용자 에이전트 문자열을 파싱하여 브라우저, 운영 체제 등의 정보를 추출합니다.

```json
PUT _ingest/pipeline/user-agent-example
{
  "processors": [
    {
      "user_agent": {
        "field": "agent"
      }
    }
  ]
}
```

### Bytes 프로세서

사람이 읽을 수 있는 바이트 값을 바이트 단위로 변환합니다 (예: 1kb → 1024).

```json
PUT _ingest/pipeline/bytes-example
{
  "processors": [
    {
      "bytes": {
        "field": "file.size"
      }
    }
  ]
}
```

지원되는 단위: "b", "kb", "mb", "gb", "tb", "pb" (대소문자 구분 없음)

### Community ID 프로세서

네트워크 플로우 데이터에 대한 Community ID를 계산합니다.

```json
PUT _ingest/pipeline/community-id-example
{
  "processors": [
    {
      "community_id": {
        "source_ip": "source.ip",
        "destination_ip": "destination.ip"
      }
    }
  ]
}
```

seed 파라미터(0~65535)로 해시 충돌을 방지할 수 있습니다. 기본값은 0입니다.

### Fingerprint 프로세서

문서 콘텐츠의 해시를 계산합니다.

```json
PUT _ingest/pipeline/fingerprint-example
{
  "processors": [
    {
      "fingerprint": {
        "fields": ["field1", "field2"],
        "target_field": "fingerprint",
        "method": "SHA-256"
      }
    }
  ]
}
```

지원되는 해시 메서드: MD5, SHA-1, SHA-256, SHA-512, MurmurHash3

### Inference 프로세서

사전 학습된 데이터 프레임 분석 모델 또는 자연어 처리 작업을 위해 배포된 모델을 사용하여 파이프라인에서 수집되는 데이터에 대해 추론을 수행합니다.

```json
PUT _ingest/pipeline/inference-example
{
  "processors": [
    {
      "inference": {
        "model_id": "my-model",
        "target_field": "ml.inference",
        "field_map": {
          "text_field": "text"
        }
      }
    }
  ]
}
```

참고: `input_output` 필드는 `target_field` 및 `field_map` 필드와 함께 사용할 수 없습니다. NLP 모델의 경우 `input_output` 옵션을 사용하고, 데이터 프레임 분석 모델의 경우 `target_field` 및 `field_map` 옵션을 사용합니다.

## 조건부 실행

### if 조건 사용

Elasticsearch 6.5부터 모든 프로세서에는 프로세서가 실행되어야 할 시점을 구성하는 선택적 `if` 필드가 있습니다. `if` 키의 값은 true 또는 false로 평가되어야 하는 Painless 스크립트입니다.

```json
PUT _ingest/pipeline/conditional-example
{
  "description": "조건부로 필드 이름을 변경하는 파이프라인",
  "processors": [
    {
      "rename": {
        "if": "ctx.source == 'billing'",
        "field": "provider",
        "target_field": "cloud.provider"
      }
    }
  ]
}
```

### Null 안전 연산자 사용

프로세서 스크립트가 부모 객체가 존재하지 않는 필드에 접근하려고 하면 Elasticsearch는 NullPointerException을 반환합니다. 이러한 예외를 방지하려면 `?.`와 같은 null 안전 연산자를 사용하고 스크립트를 null 안전하게 작성하세요.

```json
{
  "set": {
    "if": "ctx?.network?.name != null",
    "field": "network.type",
    "value": "internal"
  }
}
```

### 저장된 스크립트 사용

더 복잡한 조건부 로직의 경우 저장된 스크립트를 if 조건으로 지정할 수 있습니다.

```json
PUT _scripts/check-network-guest
{
  "script": {
    "lang": "painless",
    "source": "ctx?.network?.name == 'Guest'"
  }
}

PUT _ingest/pipeline/use-stored-script
{
  "processors": [
    {
      "drop": {
        "if": { "id": "check-network-guest" }
      }
    }
  ]
}
```

## 오류 처리

### ignore_failure 사용

프로세서 실패를 무시하고 파이프라인의 나머지 프로세서를 실행하려면 `ignore_failure`를 true로 설정합니다.

```json
PUT _ingest/pipeline/ignore-failure-example
{
  "processors": [
    {
      "rename": {
        "description": "'provider'를 'cloud.provider'로 이름 변경",
        "field": "provider",
        "target_field": "cloud.provider",
        "ignore_failure": true
      }
    }
  ]
}
```

### on_failure 사용

프로세서 실패 직후 실행할 프로세서 목록을 지정하려면 `on_failure` 파라미터를 사용합니다. `on_failure`가 지정되면 `on_failure` 구성이 비어 있더라도 Elasticsearch는 파이프라인의 나머지 프로세서를 실행합니다.

```json
PUT _ingest/pipeline/on-failure-example
{
  "processors": [
    {
      "rename": {
        "field": "provider",
        "target_field": "cloud.provider",
        "on_failure": [
          {
            "set": {
              "field": "error.message",
              "value": "'provider' 필드가 존재하지 않습니다. 'cloud.provider'로 이름을 변경할 수 없습니다.",
              "override": false
            }
          }
        ]
      }
    }
  ]
}
```

중첩된 오류 처리를 위해 `on_failure` 프로세서 목록을 중첩할 수도 있습니다.

### 파이프라인 수준 on_failure

파이프라인에 `on_failure`을 지정할 수도 있습니다. `on_failure` 값이 없는 프로세서가 실패하면 Elasticsearch는 이 파이프라인 수준 파라미터를 폴백으로 사용합니다. Elasticsearch는 파이프라인의 나머지 프로세서를 실행하려고 시도하지 않습니다.

```json
PUT _ingest/pipeline/pipeline-level-on-failure
{
  "processors": [
    { "rename": { "field": "provider", "target_field": "cloud.provider" } },
    { "rename": { "field": "region", "target_field": "cloud.region" } }
  ],
  "on_failure": [
    {
      "set": {
        "field": "_index",
        "value": "failed-documents"
      }
    },
    {
      "set": {
        "field": "error.message",
        "value": "{{_ingest.on_failure_message}}"
      }
    }
  ]
}
```

### 오류 처리용 메타데이터 필드

파이프라인 실패에 대한 추가 정보는 문서 메타데이터 필드에서 사용할 수 있습니다. 이러한 필드는 `on_failure` 블록 내에서만 접근할 수 있습니다.

| 필드 | 설명 |
|------|------|
| `_ingest.on_failure_message` | 실패 메시지 |
| `_ingest.on_failure_processor_type` | 실패한 프로세서 타입 |
| `_ingest.on_failure_processor_tag` | 실패한 프로세서의 태그 |
| `_ingest.on_failure_pipeline` | 실패한 파이프라인 이름 |

```json
{
  "on_failure": [
    {
      "set": {
        "field": "error.message",
        "value": "프로세서 {{_ingest.on_failure_processor_type}}에서 오류 발생: {{_ingest.on_failure_message}}"
      }
    }
  ]
}
```

## 파이프라인 테스트

### Simulate Pipeline API

simulate pipeline API는 요청 본문에 제공된 문서 집합에 대해 특정 파이프라인을 실행합니다. 제공된 문서에 대해 실행할 기존 파이프라인을 지정하거나 요청 본문에 파이프라인 정의를 제공할 수 있습니다.

```json
POST _ingest/pipeline/_simulate
{
  "pipeline": {
    "processors": [
      {
        "set": {
          "field": "field1",
          "value": "value1"
        }
      },
      {
        "rename": {
          "field": "field1",
          "target_field": "field2"
        }
      }
    ]
  },
  "docs": [
    {
      "_index": "my-index",
      "_id": "id1",
      "_source": {
        "foo": "bar"
      }
    },
    {
      "_index": "my-index",
      "_id": "id2",
      "_source": {
        "foo": "rab"
      }
    }
  ]
}
```

기존 파이프라인으로 시뮬레이션:

```json
POST _ingest/pipeline/my-pipeline/_simulate
{
  "docs": [
    {
      "_source": {
        "message": "test message"
      }
    }
  ]
}
```

### Verbose 모드

시뮬레이션 요청에 `verbose` 파라미터를 추가하여 파이프라인을 통과할 때 각 프로세서가 수집 문서에 어떤 영향을 미치는지 확인할 수 있습니다.

```json
POST _ingest/pipeline/_simulate?verbose
{
  "pipeline": {
    "processors": [
      {
        "set": {
          "field": "field1",
          "value": "value1"
        }
      },
      {
        "uppercase": {
          "field": "field1"
        }
      }
    ]
  },
  "docs": [
    {
      "_source": {
        "foo": "bar"
      }
    }
  ]
}
```

### Simulate Ingest API

별도의 Simulate Ingest API 도 있습니다. 이 API는 인덱스로 데이터를 수집하는 것을 시뮬레이션하기 위해 제공된 문서 집합에 대해 수집 파이프라인을 실행합니다. 이 API는 문제 해결이나 파이프라인 개발용으로 사용되며 실제로 데이터를 인덱싱하지 않습니다.

```json
POST _ingest/_simulate
{
  "docs": [
    {
      "_index": "my-index",
      "_source": {
        "message": "test"
      }
    }
  ]
}
```

## 파이프라인 모니터링

### 노드 통계 API

노드 통계 API를 사용하여 전역 및 파이프라인별 수집 통계를 가져올 수 있습니다. 이러한 통계를 사용하여 가장 자주 실행되거나 처리에 가장 많은 시간을 소비하는 파이프라인을 결정합니다.

```json
GET _nodes/stats/ingest?filter_path=nodes.*.ingest
```

## 예제: 로그 파싱

### Common Log Format 파싱

Apache HTTP 서버 접근 로그를 Common Log Format으로 파싱하는 예제입니다.

```json
PUT _ingest/pipeline/apache-logs
{
  "description": "Apache HTTP Server 접근 로그 파싱 파이프라인",
  "processors": [
    {
      "set": {
        "field": "event.ingested",
        "value": "{{{_ingest.timestamp}}}"
      }
    },
    {
      "grok": {
        "field": "message",
        "patterns": [
          "%{IPORHOST:source.ip} - %{DATA:user.name} \\[%{HTTPDATE:apache.access.time}\\] \"%{WORD:http.request.method} %{DATA:url.original} HTTP/%{NUMBER:http.version}\" %{NUMBER:http.response.status_code:int} (?:%{NUMBER:http.response.body.bytes:long}|-)"
        ],
        "ignore_missing": true
      }
    },
    {
      "date": {
        "field": "apache.access.time",
        "target_field": "@timestamp",
        "formats": ["dd/MMM/yyyy:HH:mm:ss Z"],
        "ignore_failure": true
      }
    },
    {
      "geoip": {
        "field": "source.ip",
        "target_field": "source.geo",
        "ignore_missing": true
      }
    },
    {
      "user_agent": {
        "field": "user_agent.original",
        "ignore_missing": true
      }
    },
    {
      "remove": {
        "field": "apache.access.time",
        "ignore_failure": true
      }
    }
  ]
}
```

### 테스트

```json
POST _ingest/pipeline/apache-logs/_simulate
{
  "docs": [
    {
      "_source": {
        "message": "192.168.1.1 - john [10/Oct/2023:13:55:36 +0900] \"GET /index.html HTTP/1.1\" 200 2326"
      }
    }
  ]
}
```

## 성능 고려 사항

### 프로세서 성능 팁

1. drop 프로세서를 초기에 배치: 원하지 않는 문서를 필터링하기 위해 drop 프로세서를 초기에 배치합니다.
2. 필드 존재 확인: 복잡한 작업 전에 필드 존재 확인을 실행합니다.
3. 관련 프로세서 그룹화: 관련 프로세서를 함께 그룹화합니다.

### 주의해서 사용해야 할 프로세서

다음 프로세서는 오버헤드가 있으므로 주의해서 사용해야 합니다:

| 프로세서 | 이유 |
|----------|------|
| script | Painless 스크립트는 오버헤드가 있음 |
| enrich | Elasticsearch에 대한 조회 필요 |
| geoip/user_agent | 데이터베이스 조회 |
| grok (복잡한 패턴) | 고정 형식의 경우 dissect 고려 |

### 대안 선택

- 고정 형식의 경우: grok 대신 dissect 사용 고려
- 단순 변환의 경우: script 대신 내장 프로세서 사용
- 대량 보강의 경우: 별도의 데이터 준비 단계 고려

## 플러그인 프로세서

추가 프로세서를 플러그인으로 설치할 수 있습니다. 모든 플러그인 프로세서는 클러스터의 모든 노드에 설치해야 합니다. 그렇지 않으면 Elasticsearch는 해당 프로세서를 포함하는 파이프라인 생성에 실패합니다.

`elasticsearch.yml`에서 `plugin.mandatory`를 설정하여 플러그인을 필수로 표시합니다.

```yaml
plugin.mandatory: my-processor-plugin
```

## 모범 사례

### 읽기 쉽고 유지 관리 가능한 파이프라인 만들기

1. 조건문 사용: if 문을 사용하여 수집 파이프라인 프로세서가 특정 조건이 충족될 때만 적용되도록 합니다.

2. 잠재적 문제 예상: 데이터에 대한 잠재적 문제를 예상하고 null 안전 연산자(`?.`)를 사용하여 데이터가 잘못 처리되는 것을 방지합니다.

3. 프로세서 설명 추가: 각 프로세서에 description을 추가하여 프로세서의 목적이나 구성을 설명합니다.

```json
{
  "rename": {
    "description": "provider 필드를 cloud.provider로 이름 변경",
    "field": "provider",
    "target_field": "cloud.provider"
  }
}
```

4. 태그 사용: 프로세서에 tag를 추가하여 오류 처리 시 식별을 용이하게 합니다.

```json
{
  "grok": {
    "tag": "grok-apache-logs",
    "field": "message",
    "patterns": ["%{COMBINEDAPACHELOG}"]
  }
}
```

## API 참조 요약

| API | 설명 |
|-----|------|
| `PUT _ingest/pipeline/<pipeline-id>` | 파이프라인 생성 또는 업데이트 |
| `GET _ingest/pipeline/<pipeline-id>` | 파이프라인 조회 |
| `DELETE _ingest/pipeline/<pipeline-id>` | 파이프라인 삭제 |
| `POST _ingest/pipeline/_simulate` | 파이프라인 시뮬레이션 |
| `POST _ingest/pipeline/<pipeline-id>/_simulate` | 특정 파이프라인 시뮬레이션 |
| `GET _nodes/stats/ingest` | 수집 통계 조회 |
| `GET _nodes/ingest` | 수집 노드 정보 조회 |
| `GET _ingest/processor/grok` | Grok 패턴 조회 |

## 참고 자료

- [Elasticsearch Ingest Pipelines 공식 문서](https://www.elastic.co/docs/manage-data/ingest/transform-enrich/ingest-pipelines)
- [Ingest Processor Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/processors.html)
- [Create or Update Pipeline API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-ingest-put-pipeline)
- [Simulate Pipeline API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-ingest-simulate)
- [Error Handling](https://www.elastic.co/docs/manage-data/ingest/transform-enrich/error-handling)
- [Example: Parse Logs](https://www.elastic.co/docs/manage-data/ingest/transform-enrich/example-parse-logs)
