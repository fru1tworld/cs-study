# Elasticsearch 시작하기

> 이 문서는 Elasticsearch 9.x 공식 문서의 "Getting Started" 섹션을 한국어로 번역한 것입니다.
> 원본: https://www.elastic.co/guide/en/elasticsearch/reference/current/getting-started.html

## 개요

Elasticsearch는 분산형 검색 및 분석 엔진이자 확장 가능한 데이터 저장소이며, 프로덕션 규모의 워크로드에서 속도와 관련성에 최적화된 벡터 데이터베이스입니다. Elasticsearch는 무료 오픈 소스 ELK 또는 Elastic Stack의 핵심으로서, 빠른 검색, 세밀하게 조정된 관련성, 그리고 쉽게 확장할 수 있는 강력한 분석을 위해 데이터를 안전하게 저장합니다.

이 가이드에서는 다음을 학습합니다:
- Elasticsearch 실행하기
- 요청 보내기
- 문서 인덱싱하기
- 데이터 검색하기
- 정리하기

---

## 1. Elasticsearch 실행하기

Elasticsearch를 설정하는 가장 간단한 방법은 Elastic Cloud의 Elasticsearch Service를 사용하여 관리형 배포를 생성하는 것입니다. Elasticsearch를 직접 설치하고 관리하려는 경우 다음 옵션이 있습니다:

- Linux, MacOS 또는 Windows 설치 패키지를 사용하여 Elasticsearch 실행
- Docker 컨테이너에서 Elasticsearch 실행

### Docker를 사용한 단일 노드 클러스터 실행

Docker를 사용하면 개발이나 테스트 목적으로 Elasticsearch와 Kibana를 빠르게 설정할 수 있습니다.

#### 사전 요구 사항

Docker가 설치되어 있지 않다면 먼저 [Docker Desktop](https://www.docker.com/products/docker-desktop)을 설치하세요. Windows를 사용하는 경우 WSL(Windows Subsystem for Linux)도 설치해야 합니다.

Docker Desktop을 사용하는 경우 최소 4GB의 메모리를 할당해야 합니다. Settings > Resources 에서 메모리 사용량을 조정할 수 있습니다.

#### 단일 노드 클러스터 시작

다음 명령어로 단일 노드 Elasticsearch 클러스터를 시작할 수 있습니다:

```bash
docker network create elastic

docker run -d --name elasticsearch \
  --net elastic \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:9.0.0
```

참고:
- `discovery.type=single-node`: 단일 노드 배포를 생성하여 클러스터 조인을 시도하지 않고 자체적으로 마스터로 선출되며 부트스트랩 검사를 우회합니다.
- 첫 번째 명령어는 새로운 Docker 네트워크를 생성하여 여러 Docker 컨테이너를 연결할 수 있게 합니다.
- 포트 9200은 REST API용으로 노출됩니다.

#### 보안이 활성화된 클러스터 시작

프로덕션 환경에서는 보안을 활성화하는 것이 권장됩니다. Elasticsearch 8.0부터 보안이 기본적으로 활성화됩니다:

```bash
docker run -d --name es01 \
  --net elastic \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  docker.elastic.co/elasticsearch/elasticsearch:9.0.0
```

처음 Elasticsearch를 시작할 때 생성된 elastic 비밀번호와 등록 토큰을 복사해두세요. 이 자격 증명은 처음 시작할 때만 표시됩니다.

#### 인증서 복사

보안이 활성화된 경우 인증서를 로컬 머신으로 복사해야 합니다:

```bash
docker cp es01:/usr/share/elasticsearch/config/certs/http_ca.crt .
```

#### 연결 확인

인증서 파일을 사용하여 Elasticsearch 클러스터에 인증된 호출을 만들어 연결을 확인합니다:

```bash
curl --cacert http_ca.crt -u elastic:$ELASTIC_PASSWORD https://localhost:9200
```

#### Docker Compose 사용

Docker Compose는 멀티 컨테이너 Docker 애플리케이션을 정의하고 실행하기 위한 도구입니다. YAML 파일에서 환경 변수, 네트워크 설정, 볼륨을 포함한 Elasticsearch 구성을 선언적으로 지정할 수 있습니다.

```yaml
version: "3.8"
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.0.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - esdata:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:9.0.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200

volumes:
  esdata:
    driver: local
```

Docker Compose를 사용하여 시작:

```bash
docker-compose up -d
```

참고:
- 컨테이너 외부의 볼륨에 Elasticsearch 데이터를 저장하면 성능과 데이터 지속성 두 가지 이점이 있습니다.
- 프로덕션 용도로는 `vm.max_map_count` 커널 설정을 최소 262144로 설정해야 합니다.

---

## 2. 요청 보내기

REST API를 통해 Elasticsearch에 데이터와 기타 요청을 보냅니다. Elasticsearch 언어 클라이언트와 curl을 포함하여 HTTP 요청을 보낼 수 있는 모든 클라이언트를 사용하여 Elasticsearch와 상호 작용할 수 있습니다.

### curl 사용하기

curl을 사용하여 Elasticsearch에 요청을 보낼 수 있습니다:

```bash
curl -X GET "http://localhost:9200"
```

성공적인 응답 예시:

```json
{
  "name" : "elasticsearch",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "version" : {
    "number" : "9.0.0",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "...",
    "build_date" : "2025-04-08T...",
    "build_snapshot" : false,
    "lucene_version" : "10.0.0",
    "minimum_wire_compatibility_version" : "8.18.0",
    "minimum_index_compatibility_version" : "8.0.0"
  },
  "tagline" : "You Know, for Search"
}
```

### 보안이 활성화된 경우

보안이 활성화된 경우 인증 정보를 포함해야 합니다:

```bash
curl -u elastic:$ELASTIC_PASSWORD https://localhost:9200
```

또는 API 키를 사용:

```bash
curl -H "Authorization: ApiKey ${ES_API_KEY}" https://localhost:9200
```

### Kibana Dev Tools Console 사용하기

Kibana의 개발자 콘솔은 요청을 실험하고 테스트하는 쉬운 방법을 제공합니다. 콘솔에 접근하려면 Kibana를 열고 Management > Dev Tools 로 이동합니다.

Dev Tools Console에서는 간소화된 구문을 사용할 수 있습니다:

```
GET /
```

이는 curl의 `curl -X GET "http://localhost:9200"`과 동일합니다.

### 클라이언트 라이브러리

Elasticsearch는 다양한 프로그래밍 언어용 공식 클라이언트 라이브러리를 제공합니다:
- Java
- Python
- JavaScript/Node.js
- .NET
- Go
- PHP
- Ruby
- Rust

---

## 3. 문서 인덱싱하기

Elasticsearch에 JSON 객체(문서)를 REST API를 통해 전송하여 데이터를 인덱싱합니다. 구조화된 텍스트든 비구조화된 텍스트든, 숫자 데이터든 지리 공간 데이터든, Elasticsearch는 빠른 검색을 지원하는 방식으로 효율적으로 저장하고 인덱싱합니다.

### 핵심 개념

#### 인덱스 (Index)

인덱스는 Elasticsearch의 기본 저장 단위입니다. 인덱스는 이름이나 별칭으로 고유하게 식별되는 문서의 컬렉션입니다. 이 고유한 이름은 검색 쿼리 및 기타 작업에서 인덱스를 대상으로 지정하는 데 사용되므로 중요합니다. 인덱스는 효율적인 검색을 수행하도록 설계된 고도로 최적화된 형식으로 저장된 문서의 컬렉션으로 생각할 수 있습니다.

#### 문서 (Document)

Elasticsearch에서 문서는 인덱싱할 수 있는 기본 정보 단위입니다. JSON 형식으로 표현되며, 이는 보편적인 인터넷 데이터 교환 형식입니다. 각 문서는 필드의 컬렉션으로, 데이터를 포함하는 키-값 쌍입니다. 키는 문자열이고, 값은 텍스트, 숫자, 불리언, 날짜 등 다양한 유형이 될 수 있습니다.

### 단일 문서 인덱싱

인덱스 요청을 제출하여 인덱스에 단일 문서를 추가합니다. 인덱스가 아직 존재하지 않으면 이 요청이 자동으로 생성합니다.

#### POST를 사용한 문서 추가

```
POST books/_doc
{
  "name": "Snow Crash",
  "author": "Neal Stephenson",
  "release_date": "1992-06-01",
  "page_count": 470
}
```

응답:

```json
{
  "_index": "books",
  "_id": "O0lG2YEBQ3h_hU3pY3rN",
  "_version": 1,
  "result": "created",
  "_shards": {
    "total": 2,
    "successful": 1,
    "failed": 0
  },
  "_seq_no": 0,
  "_primary_term": 1
}
```

`POST /<index>/_doc` 엔드포인트를 사용하면 Elasticsearch가 자동으로 문서 ID를 생성합니다.

#### PUT을 사용한 특정 ID로 문서 추가

특정 ID로 문서를 추가하려면 `PUT /<index>/_doc/<id>` 또는 `PUT /<index>/_create/<id>`를 사용합니다:

```
PUT books/_doc/1
{
  "name": "Brave New World",
  "author": "Aldous Huxley",
  "release_date": "1932-06-01",
  "page_count": 268
}
```

`_create` 엔드포인트를 사용하면 문서가 아직 존재하지 않을 때만 인덱싱됩니다. 동일한 ID의 문서가 이미 인덱스에 존재하면 409 응답을 반환합니다.

```
PUT books/_create/2
{
  "name": "1984",
  "author": "George Orwell",
  "release_date": "1949-06-08",
  "page_count": 328
}
```

### 여러 문서 한 번에 인덱싱 (Bulk API)

`_bulk` 엔드포인트를 사용하여 한 번의 요청으로 여러 문서를 추가합니다. 벌크 데이터는 NDJSON(Newline Delimited JSON) 형식이어야 합니다.

#### NDJSON 형식

NDJSON 구조에서 각 레코드는 두 줄이 필요합니다:
- 첫 번째 줄: 레코드가 인덱싱될 인덱스와 `_id` 지정
- 두 번째 줄: 인덱싱할 실제 문서

중요: 마지막 줄을 포함한 각 줄은 줄바꿈 문자(`\n`)로 끝나야 합니다.

```
POST books/_bulk
{"index":{"_id":"3"}}
{"name":"The Handmaid's Tale","author":"Margaret Atwood","release_date":"1985-06-01","page_count":311}
{"index":{"_id":"4"}}
{"name":"Fahrenheit 451","author":"Ray Bradbury","release_date":"1953-10-19","page_count":227}
{"index":{"_id":"5"}}
{"name":"The Road","author":"Cormac McCarthy","release_date":"2006-09-26","page_count":"287"}
```

응답:

```json
{
  "took": 30,
  "errors": false,
  "items": [
    {
      "index": {
        "_index": "books",
        "_id": "3",
        "_version": 1,
        "result": "created",
        "_shards": {
          "total": 2,
          "successful": 1,
          "failed": 0
        },
        "_seq_no": 1,
        "_primary_term": 1,
        "status": 201
      }
    },
    ...
  ]
}
```

#### curl을 사용한 Bulk 요청

먼저 `bulk_data.json` 파일을 생성합니다:

```json
{"index":{"_index":"myindex","_id":"1"}}
{"name":"John Doe","age":30,"city":"New York"}
{"index":{"_index":"myindex","_id":"2"}}
{"name":"Jane Smith","age":25,"city":"San Francisco"}
{"index":{"_index":"myindex","_id":"3"}}
{"name":"Sam Brown","age":35,"city":"Chicago"}
```

그런 다음 curl로 요청을 보냅니다:

```bash
curl -H "Content-Type: application/x-ndjson" \
  -X POST "http://localhost:9200/_bulk" \
  --data-binary "@bulk_data.json"
```

참고:
- curl에서 텍스트 파일 입력을 제공할 때 `-d` 대신 `--data-binary` 플래그를 사용해야 합니다. 후자는 줄바꿈을 보존하지 않습니다.
- `Content-Type` 헤더를 `application/json` 또는 `application/x-ndjson`으로 설정합니다.
- NDJSON은 줄바꿈 문자를 구분자로 사용하므로 JSON이 예쁘게 출력되지 않도록 해야 합니다.

#### Bulk API 작업 유형

- `index`: 문서를 추가하거나 필요에 따라 교체
- `create`: 동일한 ID의 문서가 대상에 이미 존재하면 실패
- `update`: 기존 문서를 업데이트
- `delete`: 문서를 삭제

```
POST _bulk
{"index":{"_index":"books","_id":"6"}}
{"name":"Neuromancer","author":"William Gibson","release_date":"1984-07-01","page_count":271}
{"create":{"_index":"books","_id":"7"}}
{"name":"Do Androids Dream of Electric Sheep?","author":"Philip K. Dick","release_date":"1968-01-01","page_count":210}
{"update":{"_index":"books","_id":"1"}}
{"doc":{"page_count":269}}
{"delete":{"_index":"books","_id":"5"}}
```

---

## 4. 데이터 검색하기

인덱싱된 문서는 `_search` API를 사용하여 거의 실시간으로 검색할 수 있습니다. Elasticsearch는 Query DSL(Domain Specific Language)을 기본 쿼리 언어로 사용합니다.

### 모든 문서 검색 (match_all)

`match_all` 쿼리는 인덱스의 모든 문서와 일치하는 가장 간단한 쿼리입니다:

```
GET books/_search
{
  "query": {
    "match_all": {}
  }
}
```

응답:

```json
{
  "took": 5,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 6,
      "relation": "eq"
    },
    "max_score": 1.0,
    "hits": [
      {
        "_index": "books",
        "_id": "O0lG2YEBQ3h_hU3pY3rN",
        "_score": 1.0,
        "_source": {
          "name": "Snow Crash",
          "author": "Neal Stephenson",
          "release_date": "1992-06-01",
          "page_count": 470
        }
      },
      ...
    ]
  }
}
```

#### 응답 필드 설명

- `took`: 쿼리 실행에 걸린 시간(밀리초)
- `timed_out`: 쿼리 시간 초과 여부
- `_shards`: 쿼리된 샤드 수와 성공/실패 수
- `hits.total.value`: 일치하는 총 문서 수
- `hits.max_score`: 가장 높은 점수를 받은 문서의 점수
- `hits.hits`: 일치하는 문서 배열
- `_score`: 각 문서가 쿼리와 얼마나 잘 일치하는지를 측정하는 관련성 점수

참고: 기본적으로 `match_all` 쿼리는 관련성 점수로 정렬된 인덱스의 처음 10개 문서를 반환합니다.

### 결과 수 변경

반환할 문서 수를 변경하려면 `size` 매개변수를 사용합니다:

```
GET books/_search
{
  "size": 50,
  "query": {
    "match_all": {}
  }
}
```

### 특정 필드 검색 (match)

`match` 쿼리는 전체 텍스트 검색을 위한 표준 쿼리입니다. 쿼리 텍스트는 각 필드에 지정된 분석기 구성에 따라 분석됩니다:

```
GET books/_search
{
  "query": {
    "match": {
      "name": "brave"
    }
  }
}
```

응답에서 "Brave New World"가 반환됩니다.

### 여러 단어 검색

```
GET books/_search
{
  "query": {
    "match": {
      "name": {
        "query": "snow crash",
        "operator": "or"
      }
    }
  }
}
```

- `operator: "or"` (기본값): 검색어 중 하나라도 일치하면 반환
- `operator: "and"`: 모든 검색어가 일치해야 반환

### 정확한 값 검색 (term)

`term` 쿼리는 정확한 값을 검색할 때 사용합니다. 필터링, 정렬, 집계에 적합합니다:

```
GET books/_search
{
  "query": {
    "term": {
      "author.keyword": "Neal Stephenson"
    }
  }
}
```

참고: `keyword` 필드는 정확한 값으로만 검색 가능합니다. 이메일 주소, 호스트 이름, 상태 코드, 우편번호, 태그와 같은 필드에 적합합니다.

### 범위 검색 (range)

특정 시간 범위 내에 출판된 콘텐츠를 찾으려면 `range` 쿼리를 사용합니다:

```
GET books/_search
{
  "query": {
    "range": {
      "release_date": {
        "gte": "1980-01-01",
        "lte": "1999-12-31"
      }
    }
  }
}
```

범위 연산자:
- `gte`: 크거나 같음 (greater than or equal)
- `gt`: 큼 (greater than)
- `lte`: 작거나 같음 (less than or equal)
- `lt`: 작음 (less than)

날짜 수학 표현식도 사용 가능합니다:

```
GET books/_search
{
  "query": {
    "range": {
      "release_date": {
        "gte": "now-30d/d"
      }
    }
  }
}
```

### 불리언 쿼리 (bool)

`bool` 쿼리는 불리언 논리를 사용하여 여러 쿼리를 결합할 수 있는 복합 쿼리입니다:

```
GET books/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "world" } }
      ],
      "filter": [
        { "range": { "page_count": { "gte": 200 } } }
      ],
      "should": [
        { "match": { "author": "Huxley" } }
      ],
      "must_not": [
        { "term": { "author.keyword": "George Orwell" } }
      ]
    }
  }
}
```

`bool` 쿼리 절:
- `must`: 반드시 일치해야 함. 점수에 기여함.
- `filter`: 반드시 일치해야 함. 점수에 기여하지 않음(필터 컨텍스트).
- `should`: 일치하면 좋음. 점수를 높임.
- `must_not`: 일치하면 안 됨.

### 쿼리 컨텍스트 vs 필터 컨텍스트

쿼리 컨텍스트:
쿼리 절이 "이 문서가 이 쿼리 절과 얼마나 잘 일치하는가?"라는 질문에 답합니다. 문서가 일치하는지 여부를 결정하는 것 외에도 `_score` 메타데이터 필드에 관련성 점수를 계산합니다.

필터 컨텍스트:
필터링을 사용하면 정확한 기준에 따라 검색 결과를 좁힐 수 있습니다. 전체 텍스트 검색과 달리 필터는 이진(예/아니오)이며 관련성 점수에 영향을 주지 않습니다. 필터는 제외된 결과에 점수를 매길 필요가 없기 때문에 쿼리보다 빠르게 실행됩니다.

### 다중 필드 검색 (multi_match)

`multi_match` 쿼리를 사용하면 여러 필드에서 용어를 검색할 수 있습니다:

```
GET books/_search
{
  "query": {
    "multi_match": {
      "query": "science fiction",
      "fields": ["name", "author", "description"]
    }
  }
}
```

---

## 5. 매핑 (Mapping)

매핑은 문서와 문서에 포함된 필드가 저장되고 인덱싱되는 방식을 정의하는 프로세스입니다. 각 문서는 필드의 컬렉션이며, 각 필드에는 고유한 데이터 유형이 있습니다. 데이터를 매핑할 때 문서와 관련된 필드 목록을 포함하는 매핑 정의를 생성합니다.

### 동적 매핑 (Dynamic Mapping)

동적 매핑을 사용하면 Elasticsearch가 문서의 필드 데이터 유형을 자동으로 감지하고 매핑을 생성합니다. 새 필드가 있는 추가 문서를 인덱싱하면 Elasticsearch가 이러한 필드를 자동으로 추가합니다.

이전에 추가한 문서들은 인덱스 생성 시 매핑을 지정하지 않았기 때문에 동적 매핑을 사용했습니다.

현재 매핑을 확인하려면:

```
GET books/_mapping
```

응답:

```json
{
  "books": {
    "mappings": {
      "properties": {
        "author": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "name": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "page_count": {
          "type": "long"
        },
        "release_date": {
          "type": "date"
        }
      }
    }
  }
}
```

#### 동적 매핑으로 새 필드 추가

새 필드가 포함된 문서를 인덱싱하면 동적 매핑에 의해 자동으로 필드가 추가됩니다:

```
POST books/_doc
{
  "name": "The Great Gatsby",
  "author": "F. Scott Fitzgerald",
  "release_date": "1925-04-10",
  "page_count": 180,
  "language": "EN"
}
```

`GET books/_mapping`을 다시 실행하면 `language` 필드가 추가된 것을 확인할 수 있습니다.

### 명시적 매핑 (Explicit Mapping)

프로덕션 사용 사례에서는 명시적 매핑을 사용하여 각 필드의 데이터 유형을 지정하는 것이 권장됩니다. 이렇게 하면 특정 사용 사례에 맞게 데이터가 인덱싱되는 방식을 완전히 제어할 수 있습니다.

명시적 매핑을 사용하여 인덱스 생성:

```
PUT my-explicit-mappings-books
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      },
      "author": {
        "type": "keyword"
      },
      "release_date": {
        "type": "date",
        "format": "yyyy-MM-dd"
      },
      "page_count": {
        "type": "integer"
      },
      "description": {
        "type": "text"
      }
    }
  }
}
```

### 주요 필드 유형

#### text vs keyword

- text: 본문 텍스트나 제품 설명과 같이 전체 텍스트 검색이 필요한 필드에 사용합니다. 텍스트는 인덱싱 전에 분석기에 의해 토큰화됩니다.

- keyword: 필터링, 정렬 또는 집계 사용 시 정확한 값 검색이 필요한 필드에 사용합니다. 이메일 주소, 호스트 이름, 상태 코드, 우편번호, 태그와 같은 필드에 적합합니다. keyword 필드는 정확한 값으로만 검색 가능합니다.

#### 기타 일반적인 필드 유형

- `long`, `integer`, `short`, `byte`: 정수 유형
- `double`, `float`: 부동 소수점 유형
- `date`: 날짜 유형
- `boolean`: 불리언 유형
- `object`: JSON 객체
- `nested`: 객체 배열 (각 객체를 별도로 쿼리 가능)
- `geo_point`: 위도/경도 좌표
- `dense_vector`: 벡터 검색용 밀집 벡터

### 기존 인덱스에 필드 추가

명시적 매핑은 미리 알고 있는 필드에 대해 인덱스 생성 시 정의해야 합니다. 데이터가 발전함에 따라 언제든지 매핑에 새 필드를 추가할 수 있습니다:

```
PUT my-explicit-mappings-books/_mapping
{
  "properties": {
    "genre": {
      "type": "keyword"
    }
  }
}
```

---

## 6. 집계 (Aggregations)

집계는 Query DSL을 사용하여 Elasticsearch 데이터를 분석하기 위한 기본 도구입니다. 집계를 통해 데이터의 복잡한 요약을 작성하고 주요 메트릭, 패턴 및 트렌드에 대한 통찰력을 얻을 수 있습니다. 집계는 검색에 사용되는 것과 동일한 데이터 구조를 활용하므로 매우 빠릅니다.

### 집계 유형

#### 메트릭 집계 (Metric Aggregations)

필드 값에서 합계나 평균과 같은 메트릭을 계산합니다:

```
GET books/_search
{
  "size": 0,
  "aggs": {
    "avg_page_count": {
      "avg": {
        "field": "page_count"
      }
    }
  }
}
```

#### 버킷 집계 (Bucket Aggregations)

필드 값, 범위 또는 기타 기준에 따라 문서를 버킷으로 그룹화합니다:

```
GET books/_search
{
  "size": 0,
  "aggs": {
    "authors": {
      "terms": {
        "field": "author.keyword"
      }
    }
  }
}
```

#### 파이프라인 집계 (Pipeline Aggregations)

다른 집계의 결과에 대해 집계를 실행합니다.

---

## 7. 정리하기

이 튜토리얼에서 생성한 인덱스를 삭제하려면 다음 명령을 실행합니다. 인덱스를 삭제하면 해당 문서, 샤드 및 메타데이터가 영구적으로 삭제됩니다.

### 문서 삭제

ID로 특정 문서 삭제:

```
DELETE books/_doc/1
```

### 인덱스 삭제

인덱스와 모든 문서 삭제:

```
DELETE books
```

여러 인덱스 삭제:

```
DELETE books,my-explicit-mappings-books
```

### Docker 컨테이너 정리

Docker를 사용한 경우:

```bash
# 컨테이너 중지
docker stop elasticsearch kibana

# 컨테이너 제거
docker rm elasticsearch kibana

# 네트워크 제거
docker network rm elastic

# 볼륨 제거 (데이터 영구 삭제)
docker volume rm esdata
```

Docker Compose를 사용한 경우:

```bash
docker-compose down -v
```

---

## 다음 단계

Elasticsearch 시작하기를 완료했습니다! 다음 주제를 계속 탐색해 보세요:

- [전체 텍스트 검색 및 필터링](https://www.elastic.co/docs/reference/query-languages/query-dsl/full-text-filter-tutorial): 전체 텍스트 검색 및 필터링을 포함한 다양한 데이터 쿼리 옵션에 대해 학습
- [ES|QL 시작하기](https://www.elastic.co/docs/reference/query-languages/esql/esql-getting-started): Elasticsearch의 새로운 분석 쿼리 언어인 ES|QL 학습
- [시맨틱 검색](https://www.elastic.co/docs/solutions/search/semantic-search/semantic-search-inference): NLP 모델을 사용한 시맨틱 검색 구현
- [벡터 검색](https://www.elastic.co/docs/solutions/search/vector/bring-your-own-vectors): 벡터 임베딩을 사용한 유사도 검색

---

## Elasticsearch 9.0의 새로운 기능

Elasticsearch 9.0은 2025년 4월 8일에 GA(General Availability)로 출시되었습니다. 주요 변경 사항:

### Lucene 10
Elasticsearch 9.0은 이제 Elasticsearch를 구동하는 오픈 소스 검색 라이브러리의 최신 버전인 Lucene 10에서 실행됩니다.

### Better Binary Quantization (BBQ)
8.16에서 기술 프리뷰로 처음 도입된 BBQ(Better Binary Quantization)가 이제 정식 출시되어 PQ(Product Quantization)와 같은 기존 양자화 기술에 대한 고성능 대안을 제공합니다. OpenSearch보다 5배 빠른 성능 향상을 제공합니다.

### ES|QL의 JOIN 지원
ES|QL은 강력한 분석 쿼리 언어로 계속 발전하며 이제 LOOKUP JOIN을 포함한 조인을 지원하여 실시간 크로스 인덱스 및 크로스 데이터셋 쿼리를 수행할 수 있습니다.

---

## 참고 자료

- [Elasticsearch 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Elasticsearch API 문서](https://www.elastic.co/docs/api/doc/elasticsearch/v9/)
- [Elastic 시작하기](https://www.elastic.co/docs/get-started)
- [Query DSL 참조](https://www.elastic.co/docs/reference/query-languages/query-dsl)
- [매핑 참조](https://www.elastic.co/docs/manage-data/data-store/mapping)
- [Elasticsearch 클라이언트 라이브러리](https://www.elastic.co/guide/en/elasticsearch/client/index.html)
