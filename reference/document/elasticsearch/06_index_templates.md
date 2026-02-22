# 인덱스 템플릿 (Index Templates)

> 이 문서는 Elasticsearch 9.x 공식 문서의 "Index Templates" 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html

## 목차

1. [개요](#개요)
2. [템플릿 유형](#템플릿-유형)
3. [인덱스 템플릿](#인덱스-템플릿)
4. [컴포넌트 템플릿](#컴포넌트-템플릿)
5. [템플릿 우선순위와 병합](#템플릿-우선순위와-병합)
6. [내장 인덱스 템플릿과 충돌 방지](#내장-인덱스-템플릿과-충돌-방지)
7. [인덱스 템플릿 API](#인덱스-템플릿-api)
8. [컴포넌트 템플릿 API](#컴포넌트-템플릿-api)
9. [템플릿 시뮬레이션](#템플릿-시뮬레이션)
10. [데이터 스트림용 템플릿](#데이터-스트림용-템플릿)
11. [모범 사례](#모범-사례)

---

## 개요

템플릿은 인덱스나 데이터 스트림을 생성할 때 Elasticsearch가 설정, 매핑 및 기타 구성을 적용하는 메커니즘입니다. 구성은 인덱스 생성 전에 이루어지며, 인덱스가 수동으로 생성되거나 문서 인덱싱을 통해 생성될 때 일치하는 템플릿이 적용할 설정과 매핑을 결정합니다.

### 템플릿의 주요 역할

- 설정 (Settings): 샤드 수, 레플리카 수, 리프레시 간격 등 인덱스 설정 정의
- 매핑 (Mappings): 필드 타입, 분석기, 동적 매핑 규칙 등 정의
- 별칭 (Aliases): 인덱스에 대한 별칭 정의
- 수명 주기 (Lifecycle): 데이터 스트림의 수명 주기 관리 설정

### 적용 조건

다음 조건들이 템플릿 사용 시 적용됩니다:

- 컴포저블 인덱스 템플릿 은 레거시 템플릿(Elasticsearch 7.8에서 deprecated)을 대체합니다. 레거시 템플릿은 컴포저블 템플릿이 일치하지 않을 때만 적용됩니다.
- 인덱스 생성 요청에서 명시적으로 지정된 설정 은 인덱스 템플릿과 컴포넌트 템플릿의 설정보다 우선합니다.
- 인덱스 템플릿 설정 은 컴포넌트 템플릿 설정보다 우선합니다.
- 여러 템플릿이 일치 할 경우, 가장 높은 우선순위를 가진 템플릿이 적용됩니다.
- 내장 Elasticsearch 인덱스 템플릿과의 이름 패턴 충돌 을 피해야 합니다.

---

## 템플릿 유형

Elasticsearch는 두 가지 유형의 템플릿을 제공합니다:

### 인덱스 템플릿 (Index Templates)

인덱스 템플릿은 인덱스나 데이터 스트림을 생성할 때 적용되는 주요 구성 객체입니다.

- `index_patterns`를 사용하여 인덱스 이름과 매칭
- `priority` 값을 통해 충돌 해결
- 설정, 매핑, 별칭을 직접 정의하거나 컴포넌트 템플릿을 참조할 수 있음
- 데이터 스트림 또는 일반 인덱스 생성 여부 지정

### 컴포넌트 템플릿 (Component Templates)

컴포넌트 템플릿은 설정, 매핑, 별칭을 정의하는 재사용 가능한 빌딩 블록입니다.

- 직접 적용되지 않음: 인덱스 템플릿에서 참조되어야만 적용됨
- 재사용 가능: 여러 인덱스 템플릿에서 공유 가능
- 모듈화: 매핑, 설정, 별칭을 별도로 관리 가능

인덱스 템플릿과 참조된 컴포넌트 템플릿을 함께 컴포저블 템플릿(composable templates) 이라고 합니다.

---

## 인덱스 템플릿

인덱스 템플릿은 인덱스 생성 시 적용되는 구성을 정의합니다. 인덱스 템플릿에 지정된 매핑, 설정, 별칭은 생성되는 각 인덱스에 전달됩니다. 이러한 사양은 인덱스 템플릿을 구성하는 컴포넌트 템플릿에서 올 수도 있습니다.

### 인덱스 템플릿 관리

인덱스 템플릿은 다음을 통해 생성하고 관리할 수 있습니다:
- Kibana의 Index Management 페이지
- 인덱스 템플릿 API

### 기본 인덱스 템플릿 예제

```json
PUT _index_template/my-index-template
{
  "index_patterns": ["my-index-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "_source": {
        "enabled": true
      },
      "properties": {
        "host_name": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        }
      }
    },
    "aliases": {
      "my-alias": {}
    }
  },
  "priority": 200,
  "version": 1,
  "_meta": {
    "description": "내 인덱스용 템플릿"
  }
}
```

### 컴포넌트 템플릿을 참조하는 인덱스 템플릿 예제

```json
PUT _index_template/template_1
{
  "index_patterns": ["te*", "bar*"],
  "template": {
    "settings": {
      "number_of_shards": 1
    },
    "mappings": {
      "_source": {
        "enabled": true
      },
      "properties": {
        "host_name": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        }
      }
    },
    "aliases": {
      "mydata": {}
    }
  },
  "priority": 501,
  "composed_of": ["component_template1", "runtime_component_template"],
  "version": 3,
  "_meta": {
    "description": "내 사용자 정의 템플릿"
  }
}
```

### 인덱스 템플릿 파라미터

#### 필수 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `index_patterns` | string 또는 string[] | 데이터 스트림 및 인덱스 이름을 매칭하는 와일드카드(`*`) 표현식 배열 |

#### 선택적 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `template` | object | 별칭, 매핑, 설정을 포함하는 템플릿 구성 |
| `composed_of` | string[] | 병합할 컴포넌트 템플릿 이름의 순서가 있는 목록 |
| `priority` | number | 여러 템플릿이 매칭될 때 우선순위. 기본값은 0 (가장 낮은 우선순위) |
| `version` | number | 외부 버전 관리를 위한 버전 번호 |
| `_meta` | object | 클러스터 상태에 저장되는 사용자 정의 메타데이터 |
| `data_stream` | object | 데이터 스트림을 생성하려면 이 파라미터 포함 (빈 객체도 가능) |
| `allow_auto_create` | boolean | `action.auto_create_index` 클러스터 설정 재정의 |
| `ignore_missing_component_templates` | string[] | 누락된 컴포넌트 템플릿 참조 처리 |
| `deprecated` | boolean | 템플릿을 deprecated로 표시 |

#### template 객체 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `aliases` | object | 인덱스 별칭 구성 (filter, routing, is_write_index 등) |
| `mappings` | object | 필드 매핑 정의 (properties, dynamic, _source 등) |
| `settings` | object | 인덱스 설정 (샤드 수, 레플리카 수 등) |
| `lifecycle` | object | 데이터 스트림 수명 주기 (data_retention, downsampling) - 8.11.0+ |

---

## 컴포넌트 템플릿

컴포넌트 템플릿은 설정, 매핑, 별칭을 정의하는 재사용 가능한 빌딩 블록입니다. 컴포넌트 템플릿은 인덱스에 직접 적용되지 않으며, 인덱스 템플릿에서 참조되어야만 적용됩니다.

### 컴포넌트 템플릿 관리

컴포넌트 템플릿은 다음을 통해 생성하고 관리할 수 있습니다:
- Kibana의 Index Management 페이지
- 컴포넌트 템플릿 API

### 내장 컴포넌트 템플릿

Elasticsearch에는 다음과 같은 내장 컴포넌트 템플릿이 포함되어 있습니다:

- `logs-mappings`
- `logs-settings`
- `metrics-mappings`
- `metrics-settings`
- `synthetics-mapping`
- `synthetics-settings`

### 기본 컴포넌트 템플릿 예제

```json
PUT _component_template/component_template1
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        }
      }
    }
  }
}
```

### 런타임 필드를 포함한 컴포넌트 템플릿 예제

```json
PUT _component_template/runtime_component_template
{
  "template": {
    "mappings": {
      "runtime": {
        "day_of_week": {
          "type": "keyword",
          "script": {
            "source": "emit(doc['@timestamp'].value.dayOfWeekEnum.getDisplayName(TextStyle.FULL, Locale.ENGLISH))"
          }
        }
      }
    }
  }
}
```

### 설정용 컴포넌트 템플릿 예제

```json
PUT _component_template/my-settings
{
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1,
      "index.lifecycle.name": "my-lifecycle-policy"
    }
  },
  "_meta": {
    "description": "설정용 컴포넌트 템플릿"
  }
}
```

### 별칭용 컴포넌트 템플릿 예제

```json
PUT _component_template/my-aliases
{
  "template": {
    "aliases": {
      "my-alias": {
        "filter": {
          "term": {
            "status": "active"
          }
        },
        "routing": "1"
      }
    }
  }
}
```

### 컴포넌트 템플릿 파라미터

#### 필수 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `template` | object | 별칭, 매핑, 설정을 포함하는 템플릿 구성 |

#### 선택적 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `version` | number | 외부 버전 관리를 위한 버전 번호 |
| `_meta` | object | 클러스터 상태에 저장되는 사용자 정의 메타데이터 |
| `deprecated` | boolean | 템플릿을 deprecated로 표시 |

#### template.aliases 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `filter` | object | 문서 접근을 제한하는 쿼리 |
| `index_routing` | string | 인덱싱 작업에 사용할 라우팅 값 |
| `search_routing` | string | 검색 작업에 사용할 라우팅 값 |
| `routing` | string | 인덱스 및 검색 작업 모두에 사용할 라우팅 값 |
| `is_hidden` | boolean | 별칭 숨김 여부 (기본값: false) |
| `is_write_index` | boolean | 쓰기 인덱스 지정 여부 (기본값: false) |

#### template.mappings 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `properties` | object | 필드 이름과 데이터 타입 정의 |
| `dynamic` | string | 동적 매핑 동작 (`strict`, `runtime`, `true`, `false`) |
| `date_detection` | boolean | 자동 날짜 필드 감지 |
| `numeric_detection` | boolean | 자동 숫자 필드 감지 |
| `_source` | object | 소스 필드 저장 제어 (enabled, includes, excludes) |
| `_routing` | object | 필수 라우팅 필드 지정 |
| `runtime` | object | 런타임 필드 정의 |
| `enabled` | boolean | 매핑 활성화/비활성화 |
| `subobjects` | boolean | 하위 객체 허용 여부 |

#### template.lifecycle 속성 (데이터 스트림용)

| 속성 | 타입 | 설명 |
|-----|------|------|
| `data_retention` | string | 문서의 최소 저장 기간 |
| `enabled` | boolean | 수명 주기 활성화 여부 (기본값: true) |
| `downsampling` | array | 다운샘플링 구성 배열 |

---

## 템플릿 우선순위와 병합

### 우선순위 (Priority)

우선순위는 새 데이터 스트림이나 인덱스가 생성될 때 인덱스 템플릿의 우선 적용 순서를 결정합니다.

- 가장 높은 우선순위 를 가진 인덱스 템플릿이 선택됩니다.
- 우선순위가 지정되지 않으면 템플릿은 우선순위 0 (가장 낮음)으로 처리됩니다.

```json
PUT _index_template/high-priority-template
{
  "index_patterns": ["logs-*"],
  "priority": 500,
  "template": {
    "settings": {
      "number_of_shards": 2
    }
  }
}

PUT _index_template/low-priority-template
{
  "index_patterns": ["logs-*"],
  "priority": 100,
  "template": {
    "settings": {
      "number_of_shards": 1
    }
  }
}
```

위 예제에서 `logs-my-app` 인덱스를 생성하면 `high-priority-template`이 적용되어 샤드 수가 2가 됩니다.

### 컴포넌트 템플릿 병합 순서

`composed_of` 필드에 여러 컴포넌트 템플릿이 지정되면 지정된 순서대로 병합됩니다. 즉, 나중에 지정된 컴포넌트 템플릿이 이전 컴포넌트 템플릿을 재정의 합니다.

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["base-mappings", "extended-mappings", "final-settings"],
  "priority": 200
}
```

병합 순서:
1. `base-mappings`가 먼저 적용됨
2. `extended-mappings`가 `base-mappings`를 재정의
3. `final-settings`가 최종 재정의
4. 인덱스 템플릿 자체의 `template` 설정이 마지막으로 적용

### 전체 우선순위 규칙

1. 인덱스 생성 요청에서 명시적으로 지정된 설정이 가장 높은 우선순위
2. 인덱스 템플릿의 `template` 섹션 설정
3. 컴포넌트 템플릿 설정 (나중에 지정된 것이 우선)

```json
// 인덱스 생성 시 명시적 설정이 템플릿 설정을 재정의
PUT my-index-000001
{
  "settings": {
    "number_of_shards": 5
  }
}
```

### 매핑 병합

매핑 정의는 재귀적으로 병합됩니다. 동일한 필드에 대해 다른 설정이 정의되면 나중 설정이 우선합니다.

```json
// 컴포넌트 템플릿 1
PUT _component_template/base-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "message": {
          "type": "text"
        },
        "timestamp": {
          "type": "date"
        }
      }
    }
  }
}

// 컴포넌트 템플릿 2 - message 필드를 keyword로 재정의
PUT _component_template/override-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "message": {
          "type": "keyword"
        }
      }
    }
  }
}

// 인덱스 템플릿
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["base-mappings", "override-mappings"]
}
```

결과: `message` 필드는 `keyword` 타입이 되고, `timestamp` 필드는 `date` 타입으로 유지됩니다.

---

## 내장 인덱스 템플릿과 충돌 방지

Elasticsearch는 다음 패턴에 대해 우선순위 100 인 내장 인덱스 템플릿을 유지합니다:

- `.kibana-reporting*`
- `logs-*-*`
- `metrics-*-*`
- `synthetics-*-*`
- `profiling-*`
- `security_solution-*-*`

Fleet 통합은 우선순위 200 까지의 유사한 중복 패턴을 생성합니다.

### 충돌 방지 전략

#### 1. Fleet 또는 Elastic Agent 사용 시

Fleet이나 Elastic Agent를 사용하는 경우 사용자 정의 템플릿에 100보다 낮은 우선순위 를 할당합니다.

```json
PUT _index_template/my-custom-template
{
  "index_patterns": ["logs-*-*"],
  "priority": 50,
  "template": {
    "settings": {
      "number_of_replicas": 2
    }
  }
}
```

#### 2. Fleet을 사용하지 않을 때

Fleet이나 Elastic Agent를 사용하지 않고 `logs-*`와 같은 인덱스 패턴에 대한 템플릿을 생성하려면 500보다 높은 우선순위 를 할당합니다.

```json
PUT _index_template/my-logs-template
{
  "index_patterns": ["logs-*"],
  "priority": 501,
  "template": {
    "settings": {
      "number_of_shards": 3
    }
  }
}
```

#### 3. 중복되지 않는 인덱스 패턴 사용

내장 패턴과 중복되지 않는 고유한 인덱스 패턴을 사용합니다.

```json
PUT _index_template/my-app-logs
{
  "index_patterns": ["myapp-logs-*"],
  "priority": 200,
  "template": {
    "settings": {
      "number_of_shards": 1
    }
  }
}
```

#### 4. 이름 지정 규칙

- 사용자 정의 인덱스 템플릿 이름에 `@` 기호를 사용하지 마세요.
- Elastic Stack 버전 9.1부터 Fleet은 `fleet-synced-integrations*` 인덱스를 사용하므로 이 이름을 피해야 합니다.

#### 5. 내장 템플릿 비활성화 (권장하지 않음)

```yaml
# elasticsearch.yml
stack.templates.enabled: false
```

> 경고: 이 설정은 Elastic Agent 통합과 Fleet이 올바르게 작동하지 않게 할 수 있으므로 권장하지 않습니다.

---

## 인덱스 템플릿 API

### 인덱스 템플릿 생성/업데이트

```
PUT /_index_template/{template_name}
POST /_index_template/{template_name}
```

#### 경로 파라미터

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `template_name` | 예 | 인덱스 또는 템플릿 이름 |

#### 쿼리 파라미터

| 파라미터 | 기본값 | 설명 |
|---------|-------|------|
| `create` | false | `true`면 기존 템플릿 대체 방지 |
| `master_timeout` | 30s | 마스터 노드 연결 대기 시간 |
| `cause` | - | 템플릿 생성/업데이트 이유 (사용자 정의) |

#### 예제

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "template": {
    "settings": {
      "number_of_shards": 1
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "message": {
          "type": "text"
        }
      }
    }
  },
  "priority": 200,
  "composed_of": ["my-component-template"],
  "version": 1,
  "_meta": {
    "description": "내 애플리케이션용 템플릿"
  }
}
```

### 인덱스 템플릿 조회

```
GET /_index_template
GET /_index_template/{template_name}
GET /_index_template/{template_name_pattern}
```

#### 예제

```json
// 모든 인덱스 템플릿 조회
GET _index_template

// 특정 템플릿 조회
GET _index_template/my-template

// 와일드카드로 조회
GET _index_template/my-*
```

#### 응답 예시

```json
{
  "index_templates": [
    {
      "name": "my-template",
      "index_template": {
        "index_patterns": ["my-*"],
        "template": {
          "settings": {
            "index": {
              "number_of_shards": "1"
            }
          },
          "mappings": {
            "properties": {
              "@timestamp": {
                "type": "date"
              }
            }
          }
        },
        "composed_of": ["my-component-template"],
        "priority": 200,
        "version": 1
      }
    }
  ]
}
```

### 인덱스 템플릿 삭제

```
DELETE /_index_template/{template_name}
```

#### 예제

```json
// 단일 템플릿 삭제
DELETE _index_template/my-template

// 와일드카드로 여러 템플릿 삭제
DELETE _index_template/my-*
```

### 인덱스 템플릿 존재 여부 확인

```
HEAD /_index_template/{template_name}
```

#### 예제

```json
HEAD _index_template/my-template
```

응답:
- `200`: 템플릿 존재
- `404`: 템플릿 미존재

---

## 컴포넌트 템플릿 API

### 컴포넌트 템플릿 생성/업데이트

```
PUT /_component_template/{template_name}
POST /_component_template/{template_name}
```

#### 경로 파라미터

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `template_name` | 예 | 컴포넌트 템플릿 이름 |

#### 쿼리 파라미터

| 파라미터 | 기본값 | 설명 |
|---------|-------|------|
| `create` | false | `true`면 기존 템플릿 대체 방지 |
| `master_timeout` | 30s | 마스터 노드 연결 대기 시간 |
| `cause` | - | 템플릿 생성/업데이트 이유 (사용자 정의) |

#### 예제

```json
PUT _component_template/my-component-template
{
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "_source": {
        "enabled": false
      },
      "properties": {
        "host_name": {
          "type": "keyword"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        }
      }
    }
  },
  "version": 1,
  "_meta": {
    "description": "기본 매핑 및 설정용 컴포넌트 템플릿"
  }
}
```

### 컴포넌트 템플릿 조회

```
GET /_component_template
GET /_component_template/{template_name}
GET /_component_template/{template_name_pattern}
```

#### 예제

```json
// 모든 컴포넌트 템플릿 조회
GET _component_template

// 특정 템플릿 조회
GET _component_template/my-component-template

// 와일드카드로 조회
GET _component_template/my-*
```

### 컴포넌트 템플릿 삭제

```
DELETE /_component_template/{template_name}
```

#### 예제

```json
DELETE _component_template/my-component-template
```

> 참고: 인덱스 템플릿에서 참조 중인 컴포넌트 템플릿은 삭제할 수 없습니다.

### 컴포넌트 템플릿 존재 여부 확인

```
HEAD /_component_template/{template_name}
```

---

## 템플릿 시뮬레이션

템플릿을 실제로 적용하기 전에 효과를 테스트할 수 있습니다.

### 인덱스 템플릿 시뮬레이션

특정 인덱스 이름에 어떤 설정이 적용될지 확인합니다.

```
POST /_index_template/_simulate_index/{index_name}
```

#### 예제

```json
POST _index_template/_simulate_index/my-index-000001
```

#### 응답 예시

```json
{
  "template": {
    "settings": {
      "index": {
        "number_of_shards": "1",
        "number_of_replicas": "1"
      }
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        }
      }
    },
    "aliases": {}
  },
  "overlapping": [
    {
      "name": "other-template",
      "index_patterns": ["my-*"]
    }
  ]
}
```

### 템플릿 구성 시뮬레이션

기존 인덱스 템플릿의 구성을 시뮬레이션합니다.

```
POST /_index_template/_simulate/{template_name}
```

#### 예제

```json
POST _index_template/_simulate/my-template
```

### 새 템플릿 정의 시뮬레이션

템플릿을 저장하지 않고 새 템플릿 정의의 효과를 시뮬레이션합니다.

```json
POST _index_template/_simulate
{
  "index_patterns": ["my-*"],
  "template": {
    "settings": {
      "number_of_shards": 2
    }
  },
  "composed_of": ["my-component-template"]
}
```

---

## 데이터 스트림용 템플릿

데이터 스트림을 생성하려면 `data_stream` 객체가 포함된 인덱스 템플릿이 필요합니다.

### 데이터 스트림용 인덱스 템플릿 예제

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

### data_stream 객체 속성

| 속성 | 타입 | 설명 |
|-----|------|------|
| `hidden` | boolean | `true`면 데이터 스트림이 숨겨짐 |
| `allow_custom_routing` | boolean | 사용자 정의 라우팅 허용 여부 |

### @timestamp 필드 요구 사항

데이터 스트림용 템플릿은 `@timestamp` 필드에 대해 `date` 또는 `date_nanos` 매핑이 필요합니다. 매핑을 지정하지 않으면 Elasticsearch가 기본 옵션으로 `@timestamp`를 `date` 필드로 매핑합니다.

```json
PUT _component_template/my-data-stream-mappings
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
        }
      }
    }
  }
}
```

### 수명 주기 설정

데이터 스트림의 수명 주기를 템플릿에서 설정할 수 있습니다.

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

### 시계열 데이터 스트림 (TSDS) 템플릿

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
        "cpu": {
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

### LogsDB 모드 템플릿

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

---

## 모범 사례

### 1. 컴포넌트 템플릿으로 모듈화

매핑과 설정을 별도의 컴포넌트 템플릿으로 분리하여 재사용성을 높입니다.

```json
// 매핑용 컴포넌트 템플릿
PUT _component_template/base-mappings
{
  "template": {
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "message": { "type": "text" }
      }
    }
  }
}

// 설정용 컴포넌트 템플릿
PUT _component_template/base-settings
{
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    }
  }
}

// 인덱스 템플릿에서 조합
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["base-mappings", "base-settings"],
  "priority": 200
}
```

### 2. 버전 관리

템플릿에 버전 번호를 사용하여 변경 사항을 추적합니다.

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "version": 2,
  "_meta": {
    "description": "v2: 레플리카 수 증가",
    "updated_at": "2024-01-15"
  },
  "template": {
    "settings": {
      "number_of_replicas": 2
    }
  }
}
```

### 3. 템플릿 테스트

운영 환경에 적용하기 전에 시뮬레이션 API로 테스트합니다.

```json
POST _index_template/_simulate_index/my-index-000001
```

### 4. 적절한 우선순위 설정

- 내장 템플릿: 우선순위 100
- Fleet 통합 템플릿: 우선순위 200
- 사용자 정의 템플릿 (Fleet 사용): 우선순위 50-99
- 사용자 정의 템플릿 (Fleet 미사용): 우선순위 500+

### 5. 명확한 이름 지정

템플릿 이름에서 목적을 명확히 합니다.

```json
PUT _component_template/logs-mappings-v1
PUT _component_template/logs-settings-production
PUT _index_template/logs-application-prod
```

### 6. 누락된 컴포넌트 템플릿 처리

아직 존재하지 않는 컴포넌트 템플릿을 참조하는 인덱스 템플릿을 생성할 수 있습니다.

```json
PUT _index_template/my-template
{
  "index_patterns": ["my-*"],
  "composed_of": ["existing-component", "future-component"],
  "ignore_missing_component_templates": ["future-component"],
  "priority": 200
}
```

### 7. 데이터 스트림 명명 체계

Elastic 데이터 스트림 명명 체계를 따릅니다:

```
<type>-<dataset>-<namespace>
```

예시:
- `logs-nginx-production`
- `metrics-system-monitoring`
- `traces-apm-default`

---

## 문제 해결

### 템플릿이 적용되지 않을 때

1. 인덱스 패턴 확인: 인덱스 이름이 `index_patterns`와 일치하는지 확인합니다.

```json
GET _index_template/my-template
```

2. 우선순위 충돌 확인: 더 높은 우선순위의 템플릿이 있는지 확인합니다.

```json
POST _index_template/_simulate_index/my-index-000001
```

3. 템플릿 유효성 검사: 템플릿 구문이 올바른지 확인합니다.

### 컴포넌트 템플릿 삭제 오류

참조 중인 컴포넌트 템플릿은 삭제할 수 없습니다.

```json
// 어떤 인덱스 템플릿이 참조하는지 확인
GET _index_template?filter_path=index_templates.*.index_template.composed_of
```

### 매핑 충돌

동일한 필드에 대해 호환되지 않는 매핑이 정의된 경우 오류가 발생합니다.

```json
// 올바른 접근: 필드 타입 일관성 유지
PUT _component_template/mappings-v2
{
  "template": {
    "mappings": {
      "properties": {
        "status": {
          "type": "keyword"  // 모든 템플릿에서 동일한 타입 사용
        }
      }
    }
  }
}
```

---

## 참고 자료

- [Templates | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/templates)
- [Create or update an index template API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-put-index-template)
- [Create or update a component template API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-cluster-put-component-template)
- [Simulate an index template API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-indices-simulate-template)
- [Data Streams | Elastic Docs](https://www.elastic.co/docs/manage-data/data-store/data-streams)
- [Index Settings Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html)
- [Mapping Reference](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html)
