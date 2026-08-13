# Elasticsearch Ingest 파이프라인과 데이터 스트림

## Elasticsearch Ingest Pipelines (수집 파이프라인)

### 개요

- Elasticsearch 수집 파이프라인(Ingest Pipeline): 데이터를 인덱싱하기 전에 일반적인 변환을 수행하는 용도
  - 예: 필드 제거, 텍스트에서 값 추출, 데이터 보강(enrich)
- 파이프라인은 프로세서(processor)라는 일련의 구성 가능한 작업으로 구성됨
  - 각 프로세서는 순차적으로 실행 → 수신 문서에 특정 변경 적용
  - 프로세서 실행 종료 → Elasticsearch가 변환된 문서를 데이터 스트림 또는 인덱스에 추가

```
[수집 문서] → [프로세서 1] → [프로세서 2] → [프로세서 N] → [인덱스에 저장]
```

### 파이프라인 생성 및 관리

#### 파이프라인 관리 방법

- Kibana의 Ingest Pipelines 기능 또는 ingest API로 수집 파이프라인 생성·관리 가능
- Elasticsearch는 파이프라인을 클러스터 상태(cluster state)에 저장

#### 필수 조건

- Ingest 노드 역할: `ingest` 역할이 부여된 노드가 파이프라인 처리 담당 → 수집 파이프라인 사용하려면 클러스터에 `ingest` 역할 노드 최소 1개 필요
- 대량 수집 부하: 수집 부하가 많은 경우 전용 수집 노드(dedicated ingest nodes) 별도 구성 권장
- 보안 권한: Elasticsearch 보안 기능 활성화 시 수집 파이프라인 관리에 `manage_pipeline` 클러스터 권한 필요

#### 파이프라인 생성 (PUT API)

파이프라인 생성은 `PUT _ingest/pipeline` API 사용

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

#### 파이프라인 구성 요소

- `description`: 수집 파이프라인에 대한 설명
- `version`: 외부 시스템에서 수집 파이프라인을 추적하는 데 사용하는 버전 번호
- `processors`: 인덱싱 전 문서에 변환을 수행하는 프로세서 목록, 지정 순서대로 순차 실행
- `on_failure`: 프로세서 실패 직후 실행할 프로세서 목록 (파이프라인 수준 오류 처리)
- `_meta`: 수집 파이프라인에 대한 선택적 메타데이터, 자유롭게 구성 가능

#### 파이프라인 조회 (GET API)

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

#### 파이프라인 삭제 (DELETE API)

```json
# 특정 파이프라인 삭제
DELETE _ingest/pipeline/my-pipeline-id

# 모든 파이프라인 삭제
DELETE _ingest/pipeline/*
```

### 파이프라인 적용

#### 인덱싱 요청에서 파이프라인 사용

`pipeline` 쿼리 파라미터로 개별 또는 벌크 인덱싱 요청에 파이프라인 적용 가능

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

#### update_by_query 및 reindex에서 사용

`pipeline` 파라미터는 `update_by_query` 또는 `reindex` API와 함께 사용 가능

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

#### 기본 및 최종 파이프라인 설정

##### 기본 파이프라인 (Default Pipeline)

`index.default_pipeline` 인덱스 설정으로 기본 파이프라인 설정 → `pipeline` 파라미터 미지정 인덱싱 요청에 이 파이프라인 적용

```json
PUT my-index
{
  "settings": {
    "index.default_pipeline": "my-default-pipeline"
  }
}
```

기본 파이프라인 비활성화는 `pipeline` 파라미터를 `_none`으로 설정

```json
PUT my-index/_doc/1?pipeline=_none
{
  "message": "기본 파이프라인 없이 인덱싱"
}
```

##### 최종 파이프라인 (Final Pipeline)

- `index.final_pipeline` 인덱스 설정으로 최종 파이프라인 설정
- Elasticsearch는 요청 또는 기본 파이프라인 이후 이 파이프라인 적용 → 둘 다 지정되지 않아도 최종 파이프라인은 실행됨

```json
PUT my-index
{
  "settings": {
    "index.default_pipeline": "my-default-pipeline",
    "index.final_pipeline": "my-final-pipeline"
  }
}
```

중요: 최종 파이프라인은 `pipeline` 파라미터 값과 관계없이 항상 실행됨

### 파이프라인에서 데이터 접근

#### 소스 필드 접근

프로세서는 수신 문서의 소스 필드에 대해 읽기·쓰기 권한 보유 → 필드 접근은 해당 필드 이름 사용

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

`_source` 접두사 사용도 가능, 점 표기법(dot notation)으로 객체 필드 접근 가능

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

#### 메타데이터 필드 접근

Mustache 템플릿 스니펫으로 메타데이터 필드 값 접근 가능

- `{{{_index}}}`: 대상 인덱스
- `{{{_id}}}`: 문서 ID (자동 생성 ID는 접근 불가)
- `{{{_routing}}}`: 라우팅 값

중요: 문서 ID를 자동 생성하는 경우 프로세서에서 `{{{_id}}}` 사용 불가 → Elasticsearch가 수집 완료 후에 자동 생성된 `_id` 값을 할당하기 때문

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

#### 수집 메타데이터 접근

파이프라인은 기본적으로 `_ingest.timestamp` 수집 메타데이터 필드만 생성 → 이 필드는 Elasticsearch가 문서의 인덱싱 요청을 받은 타임스탬프를 포함

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

참고: 소스 필드·메타데이터 필드와 달리 수집 메타데이터 필드는 기본적으로 인덱싱되지 않음 → `_ingest.timestamp` 또는 다른 수집 메타데이터 필드를 인덱싱하려면 set 프로세서 사용

##### 스크립트 프로세서에서의 접근

- `_ingest.timestamp`는 스크립트 프로세서를 제외한 모든 프로세서에서 `{{_ingest.timestamp}}` 형식으로 사용 가능
- 스크립트 프로세서에서는 `metadata().now`를 사용

현재 파이프라인의 이름은 `_ingest.pipeline` 수집 메타데이터 키에서 접근 가능

#### `_ingest` 접두사가 있는 소스 필드 처리

데이터에 `_ingest` 키로 시작하는 소스 필드가 포함된 경우 `_source._ingest`로 접근

### 프로세서 참조

Elasticsearch는 40개 이상의 구성 가능한 프로세서를 포함, 아래는 주요 프로세서 목록

#### 프로세서 전체 목록

- Append: 기존 배열에 값 추가
- Attachment: 첨부 파일(PDF, Office 문서 등)에서 텍스트 추출
- Bytes: 사람이 읽을 수 있는 바이트 값을 바이트 단위로 변환 (예: 1kb → 1024)
- Circle: 원 정의를 근사 다각형으로 변환
- Community ID: 네트워크 플로우 데이터에 대한 Community ID 계산
- Convert: 필드를 다른 데이터 타입으로 변환
- CSV: CSV 라인에서 필드 추출
- Date: 문자열에서 날짜 파싱
- Date index name: 날짜/타임스탬프 기반으로 올바른 시간 기반 인덱스 지정
- Dissect: 정규 표현식 없이 구조화된 필드 추출
- Dot expander: 점이 포함된 필드를 객체 필드로 확장
- Drop: 문서 삭제 (인덱싱 방지)
- Enrich: 다른 인덱스의 데이터로 문서 보강
- Fail: 예외 발생 및 파이프라인 중지
- Fingerprint: 문서 콘텐츠의 해시 계산
- Foreach: 배열이나 객체의 각 요소에 프로세서 실행
- Geo-grid: 지오 그리드 정의를 GeoJSON으로 변환
- GeoIP: IP 주소에서 지리적 위치 정보 추가
- Grok: 정규 표현식 기반 패턴 매칭으로 구조화된 필드 추출
- Gsub: 정규 표현식을 적용하여 문자열 필드 변환
- HTML strip: HTML 태그 제거
- Inference: 사전 학습된 모델로 추론 수행
- IP Location: IP 주소에서 위치 정보 추가
- Join: 배열 요소를 단일 문자열로 결합
- JSON: JSON 문자열을 구조화된 JSON으로 변환
- KV: 키-값 쌍 추출
- Lowercase: 문자열을 소문자로 변환
- Network direction: 네트워크 방향 계산
- Pipeline: 다른 파이프라인 호출
- Redact: 민감한 정보 마스킹
- Registered domain: 등록된 도메인 추출
- Remove: 필드 제거
- Rename: 필드 이름 변경
- Reroute: 다른 인덱스나 데이터 스트림으로 문서 라우팅
- Script: Painless 스크립트 실행
- Set: 필드 값 설정
- Set security user: 보안 사용자 정보 설정
- Sort: 배열 요소 정렬
- Split: 문자열을 배열로 분할
- Terminate: 파이프라인 종료
- Trim: 문자열에서 공백 제거
- Uppercase: 문자열을 대문자로 변환
- URL decode: URL 인코딩된 문자열 디코딩
- URI parts: URI를 구성 요소로 파싱
- User agent: 사용자 에이전트 문자열 파싱

#### 사용 가능한 프로세서 목록 조회

노드 정보 API로 사용 가능한 프로세서 목록 조회 가능

```json
GET _nodes/ingest?filter_path=nodes.*.ingest.processors
```

### 주요 프로세서 상세 설명

#### Set 프로세서

필드에 값 설정

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

#### Remove 프로세서

기존 필드 제거, 필드가 존재하지 않으면 예외 발생

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

#### Rename 프로세서

기존 필드의 이름 변경

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

#### Convert 프로세서

필드를 다른 데이터 타입으로 변환

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

지원되는 타입: `integer`, `long`, `float`, `double`, `string`, `boolean`, `ip`, `auto`

#### Grok 프로세서

Grok 프로세서는 단일 텍스트 필드 내에서 구조화된 필드 추출 → Grok 패턴은 재사용 가능한 별칭 표현식을 지원하는 정규 표현식과 동일

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

##### 사용자 정의 패턴

`pattern_definitions` 옵션으로 사용자 정의 패턴 추가 가능

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

##### 다중 패턴

하나의 패턴으로 필드의 잠재적 구조를 모두 캡처할 수 없는 경우 여러 패턴 제공 가능

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

##### trace_match 파라미터

어떤 패턴이 매칭되어 필드를 채웠는지 추적하려면 `trace_match` 파라미터 사용

```json
{
  "grok": {
    "field": "message",
    "patterns": ["pattern1", "pattern2"],
    "trace_match": true
  }
}
```

이 추적 메타데이터로 어떤 패턴이 매칭되었는지 디버깅 가능 → 해당 정보는 수집 메타데이터에 저장되며 인덱싱되지 않음

##### 내장 패턴 조회

```json
GET _ingest/processor/grok
```

ECS 필드 이름을 추출하는 패턴 반환은 `ecs_compatibility` 쿼리 파라미터에 `v1` 지정

```json
GET _ingest/processor/grok?ecs_compatibility=v1
```

#### Dissect 프로세서

Dissect 프로세서는 정규 표현식 없이 단일 텍스트 필드에서 구조화된 필드 추출 → Grok보다 단순하며 대부분의 경우 더 빠름

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

Dissect는 키 수정자도 지원 → 특정 필드 무시·값 추가·패딩 건너뛰기 등의 동작 지정 가능

#### Date 프로세서

문자열에서 날짜를 파싱하여 날짜 필드로 변환

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

#### Script 프로세서

들어오는 문서에서 인라인 또는 저장된 스크립트 실행, 스크립트는 수집 컨텍스트에서 실행됨

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

##### Painless 스크립트에서 필드 접근

스크립트 프로세서는 수신 문서의 JSON 소스 필드를 맵·리스트·프리미티브 집합으로 파싱 → Painless 스크립트에서 이 필드들에 접근할 때는 맵 접근 연산자 사용

```painless
ctx['my-field']
// 또는 단축형
ctx.my_field
```

#### Foreach 프로세서

배열이나 객체의 요소 수를 미리 알 수 없을 때 각 요소를 동일한 방식으로 처리하려면 foreach 프로세서 사용 → 배열 또는 객체 값을 포함하는 필드와 각 요소에 적용할 프로세서 지정 가능

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

- 배열 또는 객체를 반복할 때 foreach 프로세서는 현재 요소의 값을 `_ingest._value` 수집 메타데이터 필드에 저장
  - `_ingest._value`는 자식 필드를 포함한 전체 요소 값 포함
  - `_ingest._value` 필드에서 점 표기법으로 자식 필드 값 접근 가능
- 객체를 반복할 때 foreach 프로세서는 현재 요소의 키를 `_ingest._key`에 문자열로 저장

#### Drop 프로세서

오류 없이 문서 삭제, 특정 조건에 따라 문서가 인덱싱되는 것을 방지하는 데 유용

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

#### Reroute 프로세서

수집 중에 문서를 다른 대상 인덱스 또는 데이터 스트림으로 라우팅

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

중요: reroute 프로세서가 실행되면 이후의 모든 프로세서는 건너뜀(최종 파이프라인 포함) → 파이프라인 내에서 reroute 프로세서는 최대 하나만 실행되므로 if/else-if 조건처럼 상호 배타적인 라우팅 조건 정의 가능

#### Pipeline 프로세서

다른 파이프라인을 호출하는 특수 프로세서

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

#### Enrich 프로세서

다른 인덱스의 데이터로 문서 보강, enrich 프로세서를 포함하는 파이프라인은 추가 설정 필요

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

참고: enrich 인덱스는 생성 후 업데이트하거나 문서를 추가할 수 없음 → 데이터 갱신은 소스 인덱스를 업데이트한 후 enrich 정책을 재실행해 새 enrich 인덱스를 생성해야 함

#### GeoIP 프로세서

Maxmind GeoLite2 City Database의 데이터 기반으로 IP 주소의 지리적 위치 정보 추가

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

참고: 이 프로세서는 위도·경도를 포함하는 location 필드를 문서에 추가하지만, 매핑에서 명시적으로 정의하지 않으면 해당 필드는 `geo_point` 타입으로 인덱싱되지 않음

Elasticsearch는 Elastic GeoIP 엔드포인트에서 이 데이터베이스의 업데이트를 자동 다운로드: `https://geoip.elastic.co/v1/database`

#### User Agent 프로세서

사용자 에이전트 문자열을 파싱하여 브라우저·운영 체제 등의 정보 추출

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

#### Bytes 프로세서

사람이 읽을 수 있는 바이트 값을 바이트 단위로 변환 (예: 1kb → 1024)

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

#### Community ID 프로세서

네트워크 플로우 데이터에 대한 Community ID 계산

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

seed 파라미터(0~65535)로 해시 충돌 방지 가능, 기본값은 0

#### Fingerprint 프로세서

문서 콘텐츠의 해시 계산

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

#### Inference 프로세서

사전 학습된 데이터 프레임 분석 모델이나 자연어 처리용으로 배포된 모델을 사용해 수집 데이터에 대해 추론 수행

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

참고: `input_output` 필드는 `target_field` 및 `field_map` 필드와 함께 사용 불가 → NLP 모델은 `input_output` 옵션 사용, 데이터 프레임 분석 모델은 `target_field` 및 `field_map` 옵션 사용

### 조건부 실행

#### if 조건 사용

Elasticsearch 6.5부터 모든 프로세서에는 실행 조건을 지정하는 선택적 `if` 필드 존재, `if` 값은 true 또는 false로 평가되는 Painless 스크립트

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

#### Null 안전 연산자 사용

부모 객체가 없는 필드에 프로세서가 접근하려 하면 Elasticsearch는 NullPointerException 반환 → 방지하려면 `?.` 같은 null 안전 연산자로 스크립트 작성

```json
{
  "set": {
    "if": "ctx?.network?.name != null",
    "field": "network.type",
    "value": "internal"
  }
}
```

#### 저장된 스크립트 사용

더 복잡한 조건부 로직은 저장된 스크립트를 if 조건으로 지정 가능

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

### 오류 처리

#### ignore_failure 사용

프로세서가 실패하더라도 파이프라인의 나머지 프로세서를 계속 실행하려면 `ignore_failure`를 true로 설정

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

#### on_failure 사용

프로세서 실패 시 즉시 실행할 프로세서 목록을 지정하려면 `on_failure` 파라미터 사용 → `on_failure`가 지정되면 해당 구성이 비어 있더라도 Elasticsearch는 파이프라인의 나머지 프로세서를 계속 실행

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

중첩된 오류 처리를 위해 `on_failure` 프로세서 목록 중첩도 가능

#### 파이프라인 수준 on_failure

- 파이프라인 수준에도 `on_failure` 지정 가능
- 프로세서 자체에 `on_failure`가 없는 상태에서 실패 발생 → Elasticsearch는 파이프라인 수준 `on_failure`를 폴백으로 사용, 이 경우 파이프라인의 나머지 프로세서는 실행하지 않음

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

#### 오류 처리용 메타데이터 필드

파이프라인 실패에 관한 추가 정보는 다음 메타데이터 필드로 확인 가능, `on_failure` 블록 안에서만 접근 가능

- `_ingest.on_failure_message`: 실패 메시지
- `_ingest.on_failure_processor_type`: 실패한 프로세서 타입
- `_ingest.on_failure_processor_tag`: 실패한 프로세서의 태그
- `_ingest.on_failure_pipeline`: 실패한 파이프라인 이름

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

### 파이프라인 테스트

#### Simulate Pipeline API

simulate pipeline API는 요청 본문에 제공된 문서 집합에 대해 파이프라인 실행 → 기존 파이프라인 지정 또는 요청 본문에 파이프라인 정의 직접 포함 가능

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

#### Verbose 모드

시뮬레이션 요청에 `verbose` 파라미터 추가 → 파이프라인을 거치는 동안 각 프로세서가 수집 문서에 미치는 영향을 단계별로 확인 가능

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

#### Simulate Ingest API

- 별도의 Simulate Ingest API도 존재
- 인덱스로의 데이터 수집을 시뮬레이션하기 위해 제공된 문서 집합에 수집 파이프라인 실행
- 문제 해결이나 파이프라인 개발 시 사용, 실제로 데이터를 인덱싱하지는 않음

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

### 파이프라인 모니터링

#### 노드 통계 API

노드 통계 API로 전역 및 파이프라인별 수집 통계 조회 가능 → 이 통계로 가장 자주 실행되거나 처리 시간이 가장 많이 소요되는 파이프라인 파악 가능

```json
GET _nodes/stats/ingest?filter_path=nodes.*.ingest
```

### 예제: 로그 파싱

#### Common Log Format 파싱

Apache HTTP 서버 접근 로그를 Common Log Format으로 파싱하는 예제

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

#### 테스트

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

### 성능 고려 사항

#### 프로세서 성능 팁

- drop 프로세서를 앞에 배치: 불필요한 문서를 조기에 필터링
- 필드 존재 확인: 복잡한 작업 전에 필드 존재 여부를 먼저 확인
- 관련 프로세서 그룹화: 관련된 프로세서를 묶어 가독성 향상

#### 주의해서 사용해야 할 프로세서

다음 프로세서는 오버헤드가 있으므로 주의해서 사용

- script: Painless 스크립트는 오버헤드 있음
- enrich: Elasticsearch에 대한 조회 필요
- geoip/user_agent: 데이터베이스 조회
- grok (복잡한 패턴): 고정 형식의 경우 dissect 고려

#### 대안 선택

- 고정 형식의 경우: grok 대신 dissect 사용 고려
- 단순 변환의 경우: script 대신 내장 프로세서 사용
- 대량 보강의 경우: 별도의 데이터 준비 단계 고려

### 플러그인 프로세서

- 추가 프로세서는 플러그인으로 설치 가능
- 플러그인 프로세서는 클러스터의 모든 노드에 설치 필요, 그렇지 않으면 해당 프로세서를 포함하는 파이프라인 생성이 실패
- `elasticsearch.yml`에서 `plugin.mandatory` 설정 시 해당 플러그인을 필수로 지정 가능

```yaml
plugin.mandatory: my-processor-plugin
```

### 모범 사례

#### 읽기 쉽고 유지 관리 가능한 파이프라인 만들기

- 조건문 사용: if 문으로 수집 파이프라인 프로세서가 특정 조건이 충족될 때만 적용되도록 함
- 잠재적 문제 예상: 데이터에 대한 잠재적 문제를 예상하고 null 안전 연산자(`?.`)로 데이터가 잘못 처리되는 것을 방지
- 프로세서 설명 추가: 각 프로세서에 description을 추가해 프로세서의 목적이나 구성 설명

```json
{
  "rename": {
    "description": "provider 필드를 cloud.provider로 이름 변경",
    "field": "provider",
    "target_field": "cloud.provider"
  }
}
```

- 태그 사용: 프로세서에 tag를 추가해 오류 처리 시 식별 용이하게 함

```json
{
  "grok": {
    "tag": "grok-apache-logs",
    "field": "message",
    "patterns": ["%{COMBINEDAPACHELOG}"]
  }
}
```

### API 참조 요약

- `PUT _ingest/pipeline/<pipeline-id>`: 파이프라인 생성 또는 업데이트
- `GET _ingest/pipeline/<pipeline-id>`: 파이프라인 조회
- `DELETE _ingest/pipeline/<pipeline-id>`: 파이프라인 삭제
- `POST _ingest/pipeline/_simulate`: 파이프라인 시뮬레이션
- `POST _ingest/pipeline/<pipeline-id>/_simulate`: 특정 파이프라인 시뮬레이션
- `GET _nodes/stats/ingest`: 수집 통계 조회
- `GET _nodes/ingest`: 수집 노드 정보 조회
- `GET _ingest/processor/grok`: Grok 패턴 조회

### 참고 자료

- [Elasticsearch Ingest Pipelines 공식 문서](https://www.elastic.co/docs/manage-data/ingest/transform-enrich/ingest-pipelines)
- [Ingest Processor Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/processors.html)
- [Create or Update Pipeline API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-ingest-put-pipeline)
- [Simulate Pipeline API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-ingest-simulate)
- [Error Handling](https://www.elastic.co/docs/manage-data/ingest/transform-enrich/error-handling)
- [Example: Parse Logs](https://www.elastic.co/docs/manage-data/ingest/transform-enrich/example-parse-logs)

---

## Elasticsearch 데이터 스트림 (Data Streams)

> 원본: https://www.elastic.co/docs/manage-data/data-store/data-streams

### 목차

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

### 개요

- 데이터 스트림은 추가 전용(append-only) 시계열 데이터를 저장하도록 최적화된 인덱스 집합 위의 추상화 계층
- 데이터 스트림에 인덱싱되는 모든 문서는 `date` 또는 `date_nanos` 필드 타입으로 매핑된 `@timestamp` 필드를 포함해야 함
- 로그·이벤트·메트릭 및 기타 지속적으로 생성되는 데이터에 적합

#### 데이터 스트림의 주요 장점

- 단일 명명 리소스: 여러 인덱스에 걸쳐 추가 전용 시계열 데이터를 저장하면서 요청에 대해 단일 명명 리소스 제공
- 자동 라우팅: 인덱싱 및 검색 요청을 데이터 스트림에 직접 제출 가능, 스트림이 자동으로 데이터를 저장하는 백킹 인덱스로 요청 라우팅
- 자동화된 관리: 인덱스 수명 주기 관리(ILM) 또는 데이터 스트림 수명 주기로 백킹 인덱스 관리 자동화 가능

---

### 데이터 스트림이란?

데이터 스트림은 추가 전용 시계열 데이터를 여러 인덱스에 저장하면서 단일 명명 리소스를 통해 요청 처리 가능

#### 데이터 스트림의 구성 요소

```
데이터 스트림: my-data-stream
├── .ds-my-data-stream-2024.01.01-000001 (백킹 인덱스)
├── .ds-my-data-stream-2024.01.02-000002 (백킹 인덱스)
├── .ds-my-data-stream-2024.01.03-000003 (백킹 인덱스)
└── .ds-my-data-stream-2024.01.04-000004 (쓰기 인덱스 - 현재)
```

#### 인덱스 모드

데이터 스트림의 인덱스 모드는 새로 생성되는 백킹 인덱스에 사용, 가능한 값은 다음과 같음

- `standard`: 표준 모드 (기본값)
- `time_series`: 시계열 데이터 스트림 모드
- `logsdb`: 로그 데이터 최적화 모드
- `lookup`: 조회 전용 모드

---

### 백킹 인덱스 (Backing Indices)

데이터 스트림은 하나 이상의 숨겨진(hidden) 자동 생성 백킹 인덱스로 구성됨

#### 백킹 인덱스 명명 규칙

백킹 인덱스가 생성될 때 다음 규칙에 따라 이름 지정

```
.ds-<데이터-스트림-이름>-<yyyy.MM.dd>-<세대>
```

예를 들어 `web-server-logs` 데이터 스트림의 세대가 34이고 가장 최근 백킹 인덱스가 2099년 3월 7일에 생성됐다면 다음과 같음

```
.ds-web-server-logs-2099.03.07-000034
```

#### 백킹 인덱스 특성

- 높은 세대 번호를 가진 백킹 인덱스는 더 최근의 데이터를 포함
- 백킹 인덱스의 이름 패턴은 구현 세부 사항이며, 유일하게 보장되는 것은 각 데이터 스트림 세대 인덱스가 고유한 이름을 가진다는 점
- 축소(shrink) 또는 복원(restore) 같은 일부 작업은 백킹 인덱스의 이름을 변경할 수 있음 → 이러한 이름 변경은 백킹 인덱스를 데이터 스트림에서 제거하지 않음

#### 중요 참고 사항

- 세대는 새 인덱스가 데이터 스트림에 추가되지 않아도 변경될 수 있음(예: 기존 백킹 인덱스가 축소될 때)
- 일부 세대의 백킹 인덱스는 존재하지 않을 수 있음

---

### 쓰기 인덱스 (Write Index)

가장 최근에 생성된 백킹 인덱스가 데이터 스트림의 쓰기 인덱스

#### 쓰기 인덱스 특성

- 스트림은 새 문서를 이 인덱스에만 추가
- 인덱스에 직접 요청을 보내더라도 다른 백킹 인덱스에는 새 문서 추가 불가
- 쓰기 인덱스는 제거 불가

#### 쓰기 인덱스 변경

롤오버 발생 → 새 백킹 인덱스가 생성되어 스트림의 새 쓰기 인덱스가 됨

---

### 세대 (Generation)

각 데이터 스트림은 세대를 추적: 000001부터 시작하는 6자리의 0이 채워진 정수

#### 세대 증가

롤오버가 발생할 때마다 데이터 스트림의 세대가 증가

#### 예시

```
세대 1: .ds-my-logs-2024.01.01-000001
세대 2: .ds-my-logs-2024.01.08-000002
세대 3: .ds-my-logs-2024.01.15-000003
```

---

### 롤오버 (Rollover)

롤오버는 현재 쓰기 인덱스가 특정 크기 또는 기간에 도달할 때 새 백킹 인덱스를 생성하는 프로세스

#### 롤오버의 중요성

로그나 메트릭 같은 시계열 데이터 작업 시 단일 인덱스에 무기한으로 쓰면 성능과 리소스 사용에 문제 발생 가능 → Elasticsearch는 인덱스 롤오버와 라우팅 할당으로 수집·검색·저장 최적화

#### 롤오버 없이 발생하는 문제

- 검색 성능 저하
- 클러스터에 대한 높은 관리 부담
- 단일 인덱스의 지속적인 성장

#### 롤오버 권장 사항

일관된 타임스탬프 범위를 위해 ILM 정책의 롤오버 액션에서 `max_age` 지정 권장 → 예: 롤오버 액션에 `max_age`를 1일로 설정하면 백킹 인덱스가 일관되게 하루 분량의 데이터를 포함

#### 롤오버 발생 조건

다음 조건 중 하나 이상을 충족할 때 롤오버 발생

- `max_age`: 인덱스가 특정 기간에 도달
- `max_docs`: 인덱스의 문서 수가 특정 값에 도달
- `max_primary_shard_size`: 프라이머리 샤드 크기가 특정 값에 도달

#### 수동 롤오버

롤오버 API로 데이터 스트림을 수동 롤오버 가능

```json
POST my-data-stream/_rollover
```

조건을 지정하지 않으면 Elasticsearch가 무조건적으로 롤오버 수행

##### 조건부 롤오버 예시

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

#### 지연 롤오버 (Lazy Rollover)

`lazy` 옵션을 `true`로 설정하면 롤오버 액션이 데이터 스트림에 다음 쓰기 시 롤오버가 필요하다는 신호만 표시 → 이 옵션은 데이터 스트림에서만 허용됨

---

### 데이터 스트림 설정

데이터 스트림 설정에는 일치하는 인덱스 템플릿 필요, 대부분의 경우 하나 이상의 컴포넌트 템플릿으로 인덱스 템플릿 구성

#### 1단계: 컴포넌트 템플릿 생성

컴포넌트 템플릿은 매핑·설정·별칭을 정의하는 재사용 가능한 구성 요소, 일반적으로 매핑과 인덱스 설정에 별도의 컴포넌트 템플릿 사용

##### 매핑용 컴포넌트 템플릿

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

##### 설정용 컴포넌트 템플릿

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

#### 2단계: 인덱스 템플릿 생성

컴포넌트 템플릿을 사용해 인덱스 템플릿 생성

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

#### 인덱스 템플릿 요구 사항

- index_patterns: 데이터 스트림 이름과 일치하는 하나 이상의 인덱스 패턴
- data_stream: 템플릿이 데이터 스트림을 생성하도록 활성화 (빈 객체도 가능)
- composed_of: 매핑과 인덱스 설정을 포함하는 컴포넌트 템플릿 목록
- priority: 200보다 높은 우선순위 (내장 템플릿과의 충돌 방지)

#### @timestamp 필드 요구 사항

`@timestamp` 필드에 대한 `date` 또는 `date_nanos` 매핑 필요, 매핑 미지정 시 Elasticsearch가 기본 옵션으로 `@timestamp`를 `date` 필드로 매핑

##### date_nanos 사용 시 제한 사항

`date_nanos` 필드 타입은 1970년부터 2262년 사이의 날짜만 저장 가능, 집계 버킷은 `date_nanos` 필드를 쿼리하더라도 밀리초 해상도로 표시됨

#### 3단계: 데이터 스트림 생성

데이터 스트림 생성 방법은 두 가지

##### 방법 1: 문서 인덱싱을 통한 자동 생성

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

##### 방법 2: 데이터 스트림 API를 통한 명시적 생성

```json
PUT _data_stream/my-data-stream
```

#### 템플릿 병합 순서

여러 컴포넌트 템플릿이 `composed_of` 필드에 지정되면 지정된 순서대로 병합됨 → 나중에 지정된 컴포넌트 템플릿이 이전 컴포넌트 템플릿을 재정의

1. 컴포넌트 템플릿이 순서대로 병합
2. 부모 인덱스 템플릿의 구성이 다음에 병합
3. 마지막으로 인덱스 요청 자체의 구성이 병합

매핑 정의는 재귀적으로 병합됨

#### 내장 컴포넌트 템플릿

Elasticsearch에는 다음과 같은 내장 컴포넌트 템플릿 포함

- `logs-mappings`
- `logs-settings`
- `metrics-mappings`
- `metrics-settings`
- `synthetics-mapping`
- `synthetics-settings`

#### Kibana에서 인덱스 템플릿 생성

Kibana에서 인덱스 템플릿 생성 절차

1. 메인 메뉴 열기
2. Stack Management > Index Management로 이동
3. Index Templates 뷰에서 "Create template" 클릭

---

### 데이터 스트림 사용

데이터 스트림 설정 후 다음 작업 수행 가능

#### 문서 추가

##### 단일 문서 추가

인덱스 API와 함께 POST 사용

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

중요: 인덱스 API의 `PUT /<target>/_doc/<_id>` 형식으로는 데이터 스트림에 새 문서 추가 불가

##### 문서 ID 지정

문서 ID 지정은 `PUT /<target>/_create/<_id>` 형식 사용

```json
PUT my-data-stream/_create/my-doc-id
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "message": "특정 ID를 가진 문서"
}
```

##### 대량 문서 추가

단일 요청으로 여러 문서 추가는 bulk API 사용, create 액션만 지원됨

```json
POST my-data-stream/_bulk
{"create":{}}
{"@timestamp":"2024-01-15T10:30:00.000Z","message":"첫 번째 메시지"}
{"create":{}}
{"@timestamp":"2024-01-15T10:31:00.000Z","message":"두 번째 메시지"}
{"create":{}}
{"@timestamp":"2024-01-15T10:32:00.000Z","message":"세 번째 메시지"}
```

#### 데이터 스트림 검색

읽기 요청을 데이터 스트림에 제출하면 스트림이 요청을 모든 백킹 인덱스로 라우팅

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

##### 닫힌 백킹 인덱스

닫힌 백킹 인덱스는 데이터 스트림을 통해 검색하더라도 검색되지 않음 → 닫힌 인덱스의 문서 업데이트·삭제도 불가

#### 데이터 스트림 통계 가져오기

데이터 스트림 통계 API로 하나 이상의 데이터 스트림에 대한 통계 검색

```json
GET _data_stream/my-data-stream/_stats
```

모든 데이터 스트림의 통계:

```json
GET _data_stream/_stats
```

##### 응답 정보

- `data_stream_count`: 데이터 스트림 수
- `backing_indices`: 데이터 스트림의 현재 백킹 인덱스 수
- `total_store_size`: 선택된 데이터 스트림의 모든 샤드 총 크기
- `maximum_timestamp`: 데이터 스트림의 가장 높은 `@timestamp` 값

#### 문서 업데이트 및 삭제

데이터 스트림은 기존 데이터가 거의 업데이트되지 않는 사용 사례를 위해 설계됨 → 기존 문서에 대한 업데이트·삭제 요청은 데이터 스트림에 직접 전송 불가

##### 쿼리별 업데이트

데이터 스트림에서 많은 수의 문서를 업데이트해야 하는 경우 update by query API 사용 가능

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

##### 쿼리별 삭제

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

##### 개별 문서 업데이트/삭제

개별 문서 업데이트·삭제는 문서가 포함된 백킹 인덱스에 직접 요청 제출 필요

1. 먼저 `seq_no_primary_term: true`로 검색해 필요한 시퀀스 번호와 프라이머리 텀 확보

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

##### bulk API를 사용한 다중 문서 업데이트/삭제

```json
POST _bulk
{"delete":{"_index":".ds-my-data-stream-2024.01.15-000001","_id":"doc1"}}
{"index":{"_index":".ds-my-data-stream-2024.01.15-000001","_id":"doc2","if_seq_no":5,"if_primary_term":1}}
{"@timestamp":"2024-01-15T10:30:00.000Z","message":"업데이트된 문서"}
```

#### 닫힌 백킹 인덱스 열기

닫힌 백킹 인덱스를 다시 열려면 open index API 사용

##### 단일 백킹 인덱스 열기

```json
POST .ds-my-data-stream-2024.01.15-000001/_open
```

##### 데이터 스트림의 모든 닫힌 백킹 인덱스 열기

```json
POST my-data-stream/_open
```

---

### 데이터 스트림 수정

각 데이터 스트림에는 일치하는 인덱스 템플릿 존재, 이 템플릿의 매핑과 인덱스 설정은 스트림을 위해 생성되는 새 백킹 인덱스에 적용됨

#### 동적 설정 및 매핑 변경

대부분의 동적 인덱스 설정과 새 필드 추가 같은 매핑 변경은 인덱스 템플릿을 업데이트한 후 적용됨 → 변경 사항은 이후에 생성되는 새 백킹 인덱스에만 적용

#### 정적 설정 및 기존 필드 매핑 변경

기존 필드 매핑 수정이나 정적 인덱스 설정 변경은 종종 데이터 스트림의 백킹 인덱스에 변경 사항을 적용하기 위해 재인덱싱 필요

#### 재인덱싱 (Reindex)

재인덱싱으로 데이터 스트림의 매핑이나 설정 변경 가능

##### 재인덱싱 단계

1. 원하는 매핑 또는 설정 변경 사항이 포함되도록 인덱스 템플릿 생성 또는 업데이트
2. 기존 데이터 스트림을 템플릿과 일치하는 새 스트림으로 재인덱싱

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

중요: 데이터 스트림은 추가 전용이므로 재인덱싱 시 `op_type`을 `create`로 설정해야 함 → 재인덱싱은 데이터 스트림의 기존 문서 업데이트 불가

##### 재인덱싱 요구 사항

- 재인덱싱은 소스의 모든 문서에 대해 `_source`가 활성화되어 있어야 함
- 대상은 재인덱싱 API를 호출하기 전에 원하는 대로 구성돼야 함
- 재인덱싱은 소스나 연관된 템플릿에서 설정을 복사하지 않음
- 매핑, 샤드 수, 레플리카 등은 미리 구성 필요

#### Modify Data Streams API

백킹 인덱스를 데이터 스트림에 추가하거나 제거하려면 Modify Data Streams API 사용

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

##### 액션 설명

- add_backing_index: 기존 인덱스를 데이터 스트림의 백킹 인덱스로 추가, 인덱스는 이 작업의 일부로 숨겨짐
  - 경고: `add_backing_index` 액션으로 인덱스를 추가하면 부적절한 데이터 스트림 동작 발생 가능, 전문가 수준의 API로 간주해야 함
- remove_backing_index: 데이터 스트림에서 백킹 인덱스 제거, 인덱스는 이 작업의 일부로 숨김 해제됨, 데이터 스트림의 쓰기 인덱스는 제거 불가

#### 분석기 변경

데이터 스트림의 쓰기 인덱스는 닫기 불가 → 데이터 스트림의 쓰기 인덱스와 향후 백킹 인덱스에 대한 분석기 업데이트 절차

1. 스트림에서 사용하는 인덱스 템플릿에서 분석기 업데이트
2. 데이터 스트림을 롤오버해 새 분석기를 스트림의 쓰기 인덱스와 향후 백킹 인덱스에 적용

```json
POST my-data-stream/_rollover
```

#### 데이터 스트림 삭제

데이터 스트림과 그 백킹 인덱스 삭제 방법

```json
DELETE _data_stream/my-data-stream
```

와일드카드 표현식(`*`) 지원됨

```json
DELETE _data_stream/logs-*
```

경고: 데이터 스트림 삭제 API 요청은 데이터 스트림뿐만 아니라 스트림의 백킹 인덱스와 포함된 모든 데이터도 삭제함

---

### 시계열 데이터 스트림 (TSDS)

시계열 데이터 스트림(TSDS, Time Series Data Stream)은 메트릭 데이터 같은 시계열 데이터를 더 효율적으로 저장하도록 설계된 특별한 유형의 데이터 스트림

#### TSDS의 주요 특징

- 차원 기반 라우팅: 라우팅 로직이 차원 필드를 사용해 시계열의 모든 데이터 포인트를 동일한 샤드에 매핑 → 저장 효율성과 쿼리 성능 향상
- 중복 데이터 거부: 중복 데이터 포인트 거부됨
- 효율적인 저장: 차원과 메트릭을 사용해 데이터를 최적화된 방식으로 저장

#### 차원 (Dimensions)

차원은 시계열을 식별하는 필드, 예: 호스트 이름·서비스 이름·지역

#### index.routing_path 설정

- 각 TSDS 백킹 인덱스 내에서 Elasticsearch는 `index.routing_path` 인덱스 설정으로 동일한 차원을 가진 문서를 동일한 샤드로 라우팅
- TSDS의 일치하는 인덱스 템플릿 생성 시 하나 이상의 차원 지정 필요

##### 자동 구성

- 명시적으로 지정하지 않으면 `index.routing_path`는 `time_series_dimension`이 `true`로 설정된 매핑에서 자동 설정됨
- 인덱스 템플릿에서 `index.mode` 설정 구성 시 `index.routing_path` 설정은 `time_series_dimension` 속성이 활성화된 필드 매핑에서 자동 파생됨

#### TSDS 설정 예시

##### 컴포넌트 템플릿

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

##### 인덱스 템플릿

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

#### 라우팅 경로 제한

- `index.routing_path` 설정은 와일드카드 패턴(예: `dim.*`) 허용, 동적으로 새 필드와 일치 가능
- 단 Elasticsearch는 `index.routing_path` 값과 일치하는 스크립팅·런타임 또는 비차원 필드를 추가하는 매핑 업데이트를 거부

#### 컴포넌트 템플릿 고려 사항

`index.routing_path`가 정의된 경우 참조하는 필드는 `time_series_dimension` 속성이 활성화된 동일한 컴포넌트 템플릿에서 정의돼야 함 → 각 컴포넌트 템플릿이 자체적으로 유효해야 하기 때문

#### Pass-Through 필드

Pass-through 필드는 차원 컨테이너로 구성 가능, 이 경우 하위 필드가 자동으로 라우팅 경로에 포함됨

#### 최적화된 라우팅 전략

이 전략은 샤드 라우팅과 `_tsid` 생성을 위해 차원을 여러 번 처리하지 않도록 하여 수집 성능 향상 → `index.routing_path`를 설정하지 않아도 샤드 라우팅이 전체 `_tsid`를 사용하므로 샤드 핫스팟 방지에 도움

#### 사용자 지정 라우팅 제한

TSDS 문서는 사용자 지정 `_routing` 값을 지원하지 않음 → 마찬가지로 TSDS의 매핑에서 `_routing` 값 요구 불가

#### 시간 경계 인덱스

시간 경계 인덱스는 특정 시간 범위의 데이터만 포함하도록 제한되어 쿼리 성능 최적화

---

### 로그 데이터 스트림 (LogsDB)

로그 데이터 스트림은 로그 데이터를 더 효율적으로 저장하는 데이터 스트림 유형

#### LogsDB 인덱스 모드

- 버전 9.0부터 `logsdb` 인덱스 모드는 `logs-*-*` 패턴과 일치하는 이름을 가진 데이터 스트림에 자동 적용
- 이 기본값은 버전 9.0 이상에서 생성된 Elasticsearch 인스턴스와, `logs-*-*` 패턴과 일치하는 데이터 스트림이 없었던 이전 인스턴스에 적용됨

#### 저장 효율성

- 벤치마크에서 로그 데이터 스트림에 저장된 로그 데이터는 일반 데이터 스트림보다 약 2.5배 적은 디스크 공간을 사용, 인덱싱 성능에는 작은(10-20%) 패널티 존재
- 정확한 영향은 데이터 세트와 Elasticsearch 버전에 따라 다름

#### 저장 공간 절감

- Elasticsearch의 logsdb 인덱스 모드는 logsdb 없이 최신 버전의 Elasticsearch와 비교해 로그 데이터의 저장 공간을 최대 65%까지 감소시킴
- 내부 LogsDB 벤치마크에서 Elasticsearch 버전 8.17.0에서 출시된 LogsDB 성능과 비교 시
  - 약 16% 저장 공간 감소
  - 약 19% 중앙값 인덱싱 처리량 증가
- 8.17의 표준 모드와 비교하면 LogsDB는 이제 최대 4배 더 효율적인 저장이 가능하며 인덱싱 처리량 패널티는 최대 10%

#### Synthetic _source

- 필요한 구독이 있는 경우 logsdb 인덱스 모드는 synthetic _source를 사용해 원본 `_source` 필드 저장을 생략, 대신 문서 검색 시 doc values 또는 stored fields에서 문서 소스를 합성
- 필요한 구독이 없는 경우 logsdb 모드는 원본 `_source` 필드를 그대로 사용

synthetic _source를 포함한 완전한 logsdb 기능은 서버리스 고객과 엔터프라이즈 라이선스를 보유한 조직에서 사용 가능

#### 작동 방식

LogsDB는 다음을 통해 최적화
- 데이터 정렬 최적화
- synthetic _source로 비저장 필드 값을 즉석에서 재구성해 중복 제거
- 고급 알고리즘과 코덱으로 압축 개선
- Elasticsearch의 열 형식 저장을 활용해 효율적인 로그 저장 및 검색

#### 기본 설정

logsdb 인덱스 모드는 다음 설정 사용

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

#### LogsDB 인덱스 모드 명시적 설정

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

#### Synthetic _source와 Stored Fields

- logsdb 인덱스 모드가 synthetic _source를 사용하고 매핑에서 필드에 대해 `doc_values`가 비활성화된 경우, Elasticsearch는 해당 필드에 대해 `store` 설정을 `true`로 지정 가능 → synthetic source로 문서 소스를 재구성할 때도 필드 데이터에 계속 액세스 가능
- 예: `store`가 `false`이고 원본 값을 재구성하는 데 적합한 multi-field가 없는 text 필드에서 이 조정 발생

---

### 데이터 스트림 수명 주기

데이터 스트림 수명 주기(DLM, Data Stream Lifecycle)는 백킹 인덱스의 유지 관리를 자동으로 처리하는 데이터 스트림 관리 시스템

#### ILM과의 비교

- 데이터 스트림 수명 주기는 ILM만큼 기능이 풍부하지 않음, 특히 현재 데이터 티어링·축소(shrink) 또는 검색 가능한 스냅샷 미지원
- 그러나 이러한 특정 기능이 필요하지 않은 사용 사례는 데이터 스트림 수명 주기가 더 적합
- 데이터 스트림 수명 주기는 데이터 티어와 같은 불필요한 하드웨어 중심 개념 없이 가장 일반적인 수명 주기 관리 요구 사항에 집중할 수 있는 최적화된 수명 주기 도구

#### 주요 기능

##### 자동 롤오버

자동 롤오버는 수집되는 데이터를 더 작은 단위로 나누어 성능을 높이고 하위 호환되지 않는 매핑 변경을 용이하게 함

##### 구성 가능한 보존

- 데이터가 보장되어 저장되는 최소 기간 구성 가능
- Elasticsearch는 이 기간 이후 언제든지 오래된 데이터 삭제 가능
- 보존은 데이터 스트림 단위 또는 클러스터 전역 수준에서 구성 가능

##### 다운샘플링

데이터 스트림 수명 주기는 데이터 스트림 백킹 인덱스의 다운샘플링도 지원

#### 수명 주기 설정

##### 인덱스 템플릿에서 수명 주기 설정

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

##### 수명 주기 API 사용

```json
PUT _data_stream/my-data-stream/_lifecycle
{
  "data_retention": "7d"
}
```

#### 보존 및 다운샘플링 설정 예시

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

#### 다운샘플링 구성

다운샘플링은 다운샘플링 구성 객체의 선택적 배열, 각 객체는 다음을 정의

- after: 백킹 인덱스가 다운샘플링될 시기를 나타내는 간격 (롤오버 이후 시간 프레임)
- fixed_interval: 다운샘플링 간격 (최소 `fixed_interval` 값은 5분)

최대 10개의 다운샘플링 라운드 구성 가능

다운샘플링 액션은 인덱스 시계열 종료 시간이 지난 후에 실행됨

#### 보존 작동 방식

- `data_retention`이 정의되면 이 데이터 스트림에 추가되는 모든 문서는 최소한 해당 기간 동안 저장됨, 이 기간 이후에는 언제든지 삭제 가능, 값이 없으면 모든 문서가 무기한 저장됨
- 예약된 다운샘플링 라운드를 모두 완료한 후, 수명 주기가 실행될 때마다 백킹 인덱스가 데이터 보존 대상인지 검사 → 지정된 데이터 보존 기간이 경과하면(롤오버 시점 기준) 백킹 인덱스 삭제

#### 수명 주기 정보 가져오기

```json
GET _data_stream/my-data-stream/_lifecycle
```

#### 수명 주기 통계 가져오기

```json
GET _data_stream/_lifecycle/stats
```

---

### Data Stream API 참조

#### 데이터 스트림 생성

```json
PUT _data_stream/my-data-stream
```

#### 데이터 스트림 정보 가져오기

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

#### 데이터 스트림 통계

```json
GET _data_stream/my-data-stream/_stats
```

#### 데이터 스트림 수명 주기 설정

```json
PUT _data_stream/my-data-stream/_lifecycle
{
  "data_retention": "30d"
}
```

#### 데이터 스트림 수명 주기 가져오기

```json
GET _data_stream/my-data-stream/_lifecycle
```

#### 데이터 스트림 수정

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

#### 데이터 스트림 롤오버

```json
POST my-data-stream/_rollover
```

#### 데이터 스트림 삭제

```json
DELETE _data_stream/my-data-stream
```

---

### 모범 사례

#### 1. 수명 주기 관리 항상 구성

수명 주기 관리를 설정하지 않으면 데이터 스트림이 롤오버 없이 단일 인덱스로 계속 커져 성능 문제 발생 가능 → Elastic Stack 프로덕션 배포에서는 항상 수명 주기 관리 구성 필요

#### 2. 데이터 스트림 명명 체계 사용

Elastic 데이터 스트림 명명 체계 사용 권장

```
<type>-<dataset>-<namespace>
```

예시:
- `logs-nginx-production`
- `metrics-system-monitoring`
- `traces-apm-default`

#### 3. 롤오버에 max_age 지정

일관된 타임스탬프 범위를 보장하려면 ILM 정책의 롤오버 액션에서 `max_age` 지정 필요

#### 4. 적절한 인덱스 모드 선택

- 로그 데이터: `logsdb` 모드 사용 (저장 공간 최대 65% 절감)
- 메트릭 데이터: `time_series` 모드 사용 (차원 기반 최적화)
- 일반 데이터: `standard` 모드 사용

#### 5. 컴포넌트 템플릿 재사용

매핑과 설정에 별도의 컴포넌트 템플릿을 사용하면 여러 인덱스 템플릿에서 재사용 가능

---

### 보안 요구 사항

Elasticsearch 보안 기능이 활성화된 경우 다음 인덱스 권한 필요

#### 문서 추가

- `create_doc`, `create`, `index` 또는 `write` 인덱스 권한

#### 데이터 스트림 자동 생성

- `auto_configure`, `create_index` 또는 `manage` 인덱스 권한

#### 데이터 스트림 통계 조회

- `monitor` 또는 `manage` 인덱스 권한

#### 롤오버 수행

- `manage` 인덱스 권한

#### 데이터 스트림 삭제

- `delete_index` 권한

---

### 참고 자료

- [Data Streams | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams)
- [Set up a data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/set-up-data-stream)
- [Use a data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/use-data-stream)
- [Time series data streams | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/time-series-data-stream-tsds)
- [Logs data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/logs-data-stream)
- [Data stream lifecycle | Elastic Docs](https://www.elastic.co/docs/manage-data/lifecycle/data-stream)
- [Modify a data stream | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams/modify-data-stream)
- [Templates | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/templates)
