# Elasticsearch 스크립팅과 ES|QL

## 스크립팅 (Scripting)

> 원문: https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting.html

---

### 목차

1. [스크립팅 개요](#1-스크립팅-개요)
2. [스크립트 사용 방법](#2-스크립트-사용-방법)
3. [Painless 스크립팅 언어](#3-painless-스크립팅-언어)
4. [Painless 언어 명세](#4-painless-언어-명세)
5. [스크립트에서 문서 필드 접근](#5-스크립트에서-문서-필드-접근)
6. [스크립팅 실행 컨텍스트](#6-스크립팅-실행-컨텍스트)
7. [Lucene 표현식 언어](#7-lucene-표현식-언어)
8. [검색 템플릿](#8-검색-템플릿)
9. [스크립트 보안](#9-스크립트-보안)
10. [스크립트와 검색 성능](#10-스크립트와-검색-성능)
11. [스크립트 API](#11-스크립트-api)
12. [고급 스크립팅](#12-고급-스크립팅)

---

### 1. 스크립팅 개요

#### 1.1 스크립팅이란?

스크립팅을 사용하면 Elasticsearch에서 사용자 정의 표현식을 평가할 수 있습니다. 예를 들어, 스크립트를 사용하여 계산된 값을 필드로 반환하거나 쿼리에 대한 사용자 정의 점수를 평가할 수 있습니다.

#### 1.2 기본 스크립팅 언어

Elasticsearch의 기본 스크립팅 언어는 Painless 입니다. Painless는 Elasticsearch를 위해 특별히 설계된 언어로, 스크립팅 API의 모든 용도에 사용할 수 있으며 가장 높은 유연성을 제공합니다.

#### 1.3 사용 가능한 스크립팅 언어

| 언어 | 샌드박스 | 플러그인 유형 | 용도 |
|------|----------|---------------|------|
| Painless | 예 | 내장 | Elasticsearch 전용 범용 스크립팅 |
| Expression | 예 | 내장 | 사용자 정의 랭킹 및 정렬 |
| Mustache | 예 | 내장 | 템플릿 작업 |
| Java | 아니오 | 사용 불가 | 전문가 수준 API |

Painless 외의 다른 언어들은 특정 목적에 유용할 수 있지만 유연성이 제한됩니다.

---

### 2. 스크립트 사용 방법

#### 2.1 스크립트 구조

스크립팅을 지원하는 모든 API에서 스크립트는 일관된 패턴을 따릅니다. 기본 구조는 다음 세 가지 구성 요소로 이루어집니다:

```json
"script": {
  "lang":   "...",
  "source" | "id": "...",
  "params": { ... }
}
```

##### 구성 요소 설명

언어 지정 (`lang`)

스크립트 언어를 명시적으로 지정하지 않으면 기본값은 `painless`입니다. 이는 구문과 사용 가능한 함수를 결정합니다.

스크립트 소스 (`source` 또는 `id`)

스크립트는 두 가지 방식으로 제공할 수 있습니다:
- `source`: API 요청에 직접 작성하는 인라인 스크립트
- `id`: 저장된 스크립트 API를 통해 관리되는 저장된 스크립트에 대한 참조

매개변수 (`params`)

이 섹션에는 스크립트에 변수로 전달되는 명명된 매개변수가 포함됩니다.

#### 2.2 첫 번째 스크립트 작성

다음은 간단한 스크립트 예제입니다:

```json
GET my-index-000001/_search
{
  "script_fields": {
    "my_doubled_field": {
      "script": {
        "source": "doc['my_field'].value * 2"
      }
    }
  }
}
```

#### 2.3 매개변수 사용

스크립트에서 하드코딩된 값 대신 매개변수를 사용하면 컴파일 시간을 줄일 수 있습니다. Elasticsearch는 스크립트를 컴파일하고 캐시에 저장하는데, 매개변수만 다른 스크립트는 동일한 컴파일된 스크립트를 재사용할 수 있습니다.

```json
GET my-index-000001/_search
{
  "script_fields": {
    "my_doubled_field": {
      "script": {
        "source": "doc['my_field'].value * params['multiplier']",
        "params": {
          "multiplier": 2
        }
      }
    }
  }
}
```

#### 2.4 저장된 스크립트

자주 사용하는 스크립트는 저장하여 재사용할 수 있습니다:

스크립트 저장:

```json
POST _scripts/my-script
{
  "script": {
    "lang": "painless",
    "source": "doc['my_field'].value * params['multiplier']"
  }
}
```

저장된 스크립트 사용:

```json
GET my-index-000001/_search
{
  "script_fields": {
    "my_doubled_field": {
      "script": {
        "id": "my-script",
        "params": {
          "multiplier": 2
        }
      }
    }
  }
}
```

#### 2.5 스크립트를 사용한 문서 업데이트

스크립트를 사용하여 문서를 업데이트할 수 있습니다:

```json
POST my-index-000001/_update/1
{
  "script": {
    "source": "ctx._source.counter += params.count",
    "lang": "painless",
    "params": {
      "count": 4
    }
  }
}
```

---

### 3. Painless 스크립팅 언어

#### 3.1 Painless란?

Painless는 Elasticsearch 5.0에서 Groovy를 대체하기 위해 도입된 Elasticsearch의 기본 스크립팅 언어입니다. JVM 위에 구축되어 Java와 유사한 구문에 Elasticsearch 작업에 특화된 보안 기능과 샌드박스 보호를 결합합니다.

#### 3.2 핵심 특성

##### 보안 및 성능

Painless는 "제한된 Java API에 대한 접근을 방지하는 세분화된 허용 목록"을 통해 작동합니다. 바이트코드로 직접 컴파일되어 해석 오버헤드를 제거하고 JVM 최적화를 가능하게 합니다.

##### 목적에 맞게 설계됨

Painless는 범용 스크립팅 솔루션이 아니라 "시스템 리소스에 대한 무단 접근을 방지하면서 네이티브 성능을 제공하는 Elasticsearch 전용 언어"로 설계되었습니다.

#### 3.3 주요 사용 사례

##### 검색 향상
- 비즈니스 로직을 기반으로 한 사용자 정의 검색 점수
- 쿼리 실행 중 런타임 필드 생성
- 재인덱싱 없는 실시간 필터링

##### 데이터 처리
- 인덱싱 중 문서 변환
- 비정형 필드에서 구조화된 데이터 추출
- 메트릭 및 요약 계산

##### 운영 자동화
- Watcher를 통한 알림 패턴 모니터링
- 알림을 위한 알림 페이로드 변환

#### 3.4 Painless 사용 위치

개발자는 다음에서 Painless 스크립트를 작성할 수 있습니다:
- Dev Tools 콘솔
- 인제스트 파이프라인
- 검색 쿼리
- 런타임 필드
- 업데이트 API 작업
- Watcher 구성

#### 3.5 기본 구문 예제

```painless
// 간단한 함수 예제
String hello(String name) {
  return "Hello, " + name + "!";
}

// 함수 호출
hello("World")  // "Hello, World!" 반환
```

---

### 4. Painless 언어 명세

#### 4.1 언어 기반

Painless의 구문은 Java 구문과 유사하며, 동적 타이핑, Map 및 List 접근자 단축키, 배열 초기화자와 같은 추가 기능을 포함합니다.

구현은 파싱을 위한 ANTLR4와 바이트코드 생성을 위한 ASM을 사용하며, 스크립트는 실행을 위해 JVM 바이트코드로 직접 컴파일됩니다.

#### 4.2 주석

Painless는 Java 스타일 주석을 지원합니다:

```painless
// 한 줄 주석

/*
 * 여러 줄 주석
 */
```

#### 4.3 키워드

Painless의 예약어에는 다음이 포함됩니다:
- 제어 흐름: `if`, `else`, `for`, `while`, `do`, `break`, `continue`, `return`
- 타입: `boolean`, `byte`, `short`, `char`, `int`, `long`, `float`, `double`, `void`, `def`
- 기타: `true`, `false`, `null`, `new`, `instanceof`, `this`

참고: Painless는 표준 Java와 달리 `switch` 문을 지원하지 않습니다.

#### 4.4 데이터 타입

##### 기본 타입

Painless는 기본 JVM 데이터를 나타내는 8가지 기본 타입을 제공합니다:

정수 타입:
- `byte`: 8비트 부호 있는 정수, 범위 [-128, 127], 기본값 0
- `short`: 16비트 부호 있는 정수, 범위 [-32768, 32767], 기본값 0
- `char`: 16비트 부호 없는 유니코드 문자, 범위 [0, 65535], 기본값 0
- `int`: 32비트 부호 있는 정수, 기본값 0
- `long`: 64비트 부호 있는 정수, 기본값 0

부동 소수점 타입:
- `float`: 32비트 IEEE 754 단정밀도, 기본값 0.0
- `double`: 64비트 IEEE 754 배정밀도, 기본값 0.0

불리언 타입:
- `boolean`: 두 가지 가능한 값(true/false), 기본값 false

```painless
int count = 10;
double price = 99.99;
boolean isActive = true;
```

##### 참조 타입

참조 타입은 잠재적인 멤버 필드와 메서드를 가진 명명된 구조를 나타내며, 힙 메모리에 할당됩니다:

```painless
List l = new ArrayList();
l.add(1);
int i = l.get(0) + 2;
```

##### 동적 타입 (def)

`def` 타입은 런타임 타입 유연성을 제공하며, 단일 식별자를 통해 모든 기본 또는 참조 타입 값을 나타냅니다:

```painless
def dp = 1;                    // int를 나타냄
def dr = new ArrayList();      // ArrayList를 나타냄
dr = dp;                       // 이제 int를 나타냄
```

참고: 성능 고려사항: 명시적 타입에 비해 약간의 오버헤드가 있습니다.

##### 문자열 타입

```painless
String r = "some text";
String s = 'some text';
String t = new String("some text");
```

큰따옴표와 작은따옴표 모두 사용할 수 있습니다.

##### 배열 타입

```painless
// 단일 차원
int[] x = new int[10];

// 다차원
int[][][] ia3 = new int[2][3][4];

// 배열 접근
x[0] = 5;
int value = x[0];
int len = x.length;
```

#### 4.5 연산자

Painless는 연산자를 6가지 주요 카테고리로 구성합니다:

##### 산술 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | 더하기 |
| `-` | 빼기 |
| `*` | 곱하기 |
| `/` | 나누기 |
| `%` | 나머지 |
| `++` | 증가 |
| `--` | 감소 |

##### 비교 연산자

| 연산자 | 설명 |
|--------|------|
| `==` | 같음 |
| `!=` | 같지 않음 |
| `<` | 보다 작음 |
| `<=` | 보다 작거나 같음 |
| `>` | 보다 큼 |
| `>=` | 보다 크거나 같음 |

##### 논리 연산자

| 연산자 | 설명 |
|--------|------|
| `&&` | 논리 AND |
| `\|\|` | 논리 OR |
| `!` | 논리 NOT |

##### 비트 연산자

| 연산자 | 설명 |
|--------|------|
| `&` | 비트 AND |
| `\|` | 비트 OR |
| `^` | 비트 XOR |
| `~` | 비트 NOT |
| `<<` | 왼쪽 시프트 |
| `>>` | 오른쪽 시프트 |
| `>>>` | 부호 없는 오른쪽 시프트 |

##### 기타 연산자

| 연산자 | 설명 |
|--------|------|
| `?:` | 조건 (삼항) |
| `?:` | 엘비스 연산자 (null 병합) |
| `?.` | null 안전 접근 |
| `instanceof` | 타입 검사 |

##### 연산자 우선순위

연산자는 0(가장 높음)에서 17(가장 낮음)까지의 숫자 우선순위 척도를 따릅니다:
- 레벨 0: 괄호
- 레벨 1-3: 메서드 호출, 필드 접근, 배열 연산
- 레벨 4-6: 산술 및 시프트 연산
- 레벨 7-9: 비교 및 동등성 검사
- 레벨 10-14: 비트 및 논리 연산
- 레벨 15-17: 조건, 엘비스 및 할당 연산

#### 4.6 문

##### 조건문

```painless
if (doc['status'].value == 'active') {
    // 활성 상태일 때 처리
} else if (doc['status'].value == 'pending') {
    // 대기 상태일 때 처리
} else {
    // 기타 상태 처리
}
```

##### 반복문

for 루프:

```painless
// 표준 for 루프
for (int i = 0; i < 10; i++) {
    // 반복 처리
}

// 향상된 for 루프
for (def item : list) {
    // 각 항목 처리
}

// 대안 구문
for (item in list) {
    // 각 항목 처리
}
```

while 루프:

```painless
while (ctx._source.count < 10) {
    ctx._source.count++;
}
```

do-while 루프:

```painless
do {
    // 작업 수행
} while (condition);
```

#### 4.7 함수

함수는 특정 작업을 수행하기 위한 하나 이상의 문으로 구성된 코드 조각입니다:

```painless
// 함수 정의
boolean isNegative(def x) {
    return x < 0;
}

// 함수 호출
if (isNegative(someValue)) {
    // 음수 처리
}
```

반환 타입이 void인 함수:

```painless
void addToList(List l, def d) {
    l.add(d);
}
```

#### 4.8 람다 표현식

Painless는 Java의 함수형 프로그래밍 기능과 동일한 구문을 가진 람다 표현식과 메서드 참조를 지원합니다:

```painless
// 기본 람다
list.removeIf(item -> item == 2);

// 타입이 있는 매개변수
list.removeIf((int item) -> item == 2);

// 블록 본문
list.removeIf((int item) -> { return item == 2; });

// 여러 매개변수
list.sort((x, y) -> x - y);
```

메서드 참조:

```painless
// 정적 또는 인스턴스 메서드 참조
list.sort(Integer::compare);

// 현재 스크립트 내 함수 참조
list.sort(this::myCompare);
```

#### 4.9 정규 표현식

Painless는 정규 표현식 상수를 패턴 생성의 기본 메커니즘으로 지원합니다:

```painless
Pattern p = /[aeiou]/
```

주의: 잘못 작성된 정규 표현식은 성능을 크게 저하시킬 수 있습니다. 가능하면 특히 자주 실행되는 스크립트에서 정규 표현식 사용을 피하세요.

##### 패턴 플래그

| 문자 | Java 상수 | 예제 |
|------|-----------|------|
| `c` | CANON_EQ | `'a' ==~ /a/c` |
| `i` | CASE_INSENSITIVE | `'A' ==~ /a/i` |
| `l` | LITERAL | `'[a]' ==~ /[a]/l` |
| `m` | MULTILINE | `'a\nb\nc' =~ /^b$/m` |
| `s` | DOTALL | `'a\nb\nc' =~ /.b./s` |
| `U` | UNICODE_CHARACTER_CLASS | `'E' ==~ /\\w/U` |
| `u` | UNICODE_CASE | `'E' ==~ /e/iu` |
| `x` | COMMENTS | `'a' ==~ /a #comment/x` |

여러 플래그를 결합할 수 있습니다 (예: `/foo/iUx`).

---

### 5. 스크립트에서 문서 필드 접근

#### 5.1 업데이트 스크립트

update, update-by-query 및 reindex API 스크립트는 다음을 노출하는 `ctx` 변수에 접근합니다:

- `ctx._source`: 문서 `_source` 필드에 대한 접근
- `ctx.op`: 작업 유형 (`index` 또는 `delete`)
- `ctx._index`: 문서 메타데이터 필드에 대한 접근

참고: 이러한 스크립트는 `doc` 변수를 사용할 수 없으며 문서 접근을 위해 `ctx`에 의존해야 합니다.

```json
POST my-index-000001/_update/1
{
  "script": {
    "source": """
      ctx._source.counter += params.count;
      ctx._source.tags.add(params.tag);
    """,
    "params": {
      "count": 4,
      "tag": "new_tag"
    }
  }
}
```

#### 5.2 검색 및 집계 스크립트

필드 값에 접근하는 세 가지 주요 방법이 있습니다:

##### Doc 값 (가장 빠름)

`doc['field_name']` 구문은 컬럼형 필드 값을 검색합니다. 이것은 "스크립트에서 필드 값에 접근하는 가장 빠르고 효율적인 방법"입니다.

```json
GET my-index-000001/_search
{
  "script_fields": {
    "profit": {
      "script": {
        "source": "doc['selling_price'].value - doc['cost_price'].value"
      }
    }
  }
}
```

제한 사항:
- JSON 객체를 반환할 수 없음
- 누락된 필드에서 오류 발생
- 텍스트 필드의 경우 `fielddata` 활성화 필요 (성능 집약적)

##### _source 필드

`params._source.field_name` 구문을 통해 접근합니다. doc 값보다 느리지만 결과당 여러 값을 반환하는 스크립트 필드에 유용합니다.

```json
GET my-index-000001/_search
{
  "script_fields": {
    "full_name": {
      "script": {
        "source": "params._source.first_name + ' ' + params._source.last_name"
      }
    }
  }
}
```

##### 저장된 필드

매핑에서 `"store": true`로 표시된 필드는 `params._fields['field_name'].value` 구문으로 접근합니다.

```json
GET my-index-000001/_search
{
  "script_fields": {
    "full_info": {
      "script": {
        "source": "params._fields['title'].value + ' - ' + params._fields['author'].value"
      }
    }
  }
}
```

#### 5.3 특수 변수

`_score`: `function_score` 쿼리, 스크립트 기반 정렬 및 집계에서 사용할 수 있습니다. 문서 관련성을 나타냅니다.

`_termStats`: `script_score` 쿼리에서 용어 통계 데이터를 제공합니다. `termFreq()`, `docFreq()`, `totalTermFreq()`, `uniqueTermsCount()` 및 `matchedTermsCount()`와 같은 메서드를 포함합니다.

---

### 6. 스크립팅 실행 컨텍스트

Painless 스크립트는 사용 가능한 변수, 허용된 API 및 반환 유형을 결정하는 특정 컨텍스트 내에서 실행됩니다.

#### 6.1 데이터 수정 컨텍스트

##### 런타임 필드

스크립트는 인덱스에 저장하지 않고 검색 작업 중에 동적으로 필드 값을 계산합니다.

사용 가능한 변수:
- `params` (Map): 사용자 정의 쿼리 매개변수
- `doc` (Map): List로서의 문서 필드
- `params['_source']` (Map): 저장된 문서에서 추출된 JSON

emit() 메서드:

`emit` 메서드는 필수이며 필드 유형에 따라 값을 받습니다:

| 필드 타입 | emit 메서드 |
|-----------|-------------|
| boolean | `emit(boolean)` |
| date | `emit(long)` |
| double | `emit(double)` |
| geo_point | `emit(double lat, double lon)` |
| ip | `emit(String)` |
| long | `emit(long)` |
| keyword | `emit(String)` |

중요: `emit` 메서드는 `null` 값을 허용할 수 없습니다. 참조된 필드에 값이 없으면 이 메서드를 호출하지 마세요.

```json
PUT my-index-000001/_mapping
{
  "runtime": {
    "day_of_week": {
      "type": "keyword",
      "script": {
        "source": "emit(doc['@timestamp'].value.getDayOfWeekEnum().toString())"
      }
    }
  }
}
```

##### 인제스트 프로세서

스크립트는 인덱싱 전에 인제스트 파이프라인을 통해 삽입 중인 문서를 수정합니다.

사용 가능한 변수:
- `ctx` (Map): 인덱싱 중인 필드의 JSON 구조
- `ctx['_index']` (String): 대상 인덱스 이름
- `params` (Map, 읽기 전용): 쿼리를 통해 전달된 사용자 정의 매개변수

```json
PUT _ingest/pipeline/my-pipeline
{
  "processors": [
    {
      "script": {
        "source": """
          String[] envSplit = ctx['env'].splitOnToken('-');
          ctx['environment'] = envSplit[1].trim();
        """
      }
    }
  ]
}
```

##### 업데이트

스크립트는 Elasticsearch의 업데이트 작업을 통해 개별 문서를 수정하여 단일 문서 내에서 필드를 추가, 수정 또는 삭제할 수 있습니다.

읽기 전용 변수:
- `params`: 쿼리를 통해 전달된 사용자 정의 값
- `ctx['_routing']`: 샤드 선택 값
- `ctx['_index']`: 인덱스 이름
- `ctx['_id']`: 고유 문서 식별자
- `ctx['_version']`: 현재 문서 버전 (정수)
- `ctx['_now']`: 밀리초 단위의 현재 타임스탬프

변경 가능한 소스:
- `ctx['_source']`: 저장된 문서에 존재하는 필드에 대해 추출된 JSON을 포함

```json
POST my-index-000001/_update/1
{
  "script": {
    "source": """
      ctx._source.sold = true;
      ctx._source.final_price = ctx._source.price - params.discount;
    """,
    "params": {
      "discount": 100
    }
  }
}
```

작업 제어:
- `ctx['op']`: 업데이트의 경우 `index`가 기본값; 작업 없음은 `none`으로, 문서 제거는 `delete`로 설정

#### 6.2 검색 및 검색 컨텍스트

##### 정렬

스크립트는 표준 필드 정렬 메커니즘을 넘어 결과 순서를 사용자 정의합니다.

반환 값: 구성된 `type` 매개변수에 따라 `double`(숫자 정렬용) 또는 `String`(문자열 기반 정렬용)을 반환해야 합니다.

```json
GET my-index-000001/_search
{
  "sort": [
    {
      "_script": {
        "type": "number",
        "script": {
          "source": "doc['field'].value.length() * params.factor",
          "params": {
            "factor": 1.5
          }
        },
        "order": "desc"
      }
    }
  ]
}
```

##### 필드

스크립트는 스크립트 필드를 통해 검색 결과에서 반환되는 계산된 값을 생성합니다.

```json
GET my-index-000001/_search
{
  "script_fields": {
    "calculated_field": {
      "script": {
        "source": "doc['price'].value * 1.1"
      }
    }
  }
}
```

##### 필터

스크립트는 스크립트 쿼리를 사용하여 bool 쿼리 내에서 사용자 정의 필터링 로직을 구현합니다.

```json
GET my-index-000001/_search
{
  "query": {
    "bool": {
      "filter": {
        "script": {
          "script": {
            "source": "doc['price'].value > params.min_price",
            "params": {
              "min_price": 100
            }
          }
        }
      }
    }
  }
}
```

#### 6.3 점수 컨텍스트

##### 점수

스크립트는 function score 쿼리에서 관련성 계산을 사용자 정의합니다.

사용 가능한 변수:
- `_score`: 기본 쿼리에서 문서의 유사성 점수를 나타내는 읽기 전용 double
- `doc`: 필드 접근을 제공하는 읽기 전용 맵
- `params`: 쿼리를 통해 전달된 사용자 정의 매개변수의 읽기 전용 맵

반환 값: 현재 문서의 재계산된 점수를 나타내는 `double`을 반환해야 합니다.

```json
GET my-index-000001/_search
{
  "query": {
    "function_score": {
      "query": {
        "match_all": {}
      },
      "script_score": {
        "script": {
          "source": "_score * Math.log(2 + doc['popularity'].value)"
        }
      }
    }
  }
}
```

##### 유사도

스크립트는 관련성 랭킹을 위한 사용자 정의 텍스트 유사도 알고리즘을 정의합니다.

#### 6.4 집계 컨텍스트

##### 메트릭 집계

분산 메트릭 계산을 지원하는 4개의 특수 컨텍스트(initialization, map, combine, reduce)가 있습니다.

```json
GET my-index-000001/_search
{
  "aggs": {
    "profit": {
      "scripted_metric": {
        "init_script": "state.transactions = []",
        "map_script": "state.transactions.add(doc['profit'].value)",
        "combine_script": "double total = 0; for (t in state.transactions) { total += t } return total",
        "reduce_script": "double total = 0; for (s in states) { total += s } return total"
      }
    }
  }
}
```

##### 버킷 스크립트

스크립트는 파이프라인 작업을 위해 집계 버킷 경로에서 값을 계산합니다.

```json
GET my-index-000001/_search
{
  "aggs": {
    "sales_per_month": {
      "date_histogram": {
        "field": "date",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_sales": { "sum": { "field": "sales" } },
        "total_costs": { "sum": { "field": "costs" } },
        "profit": {
          "bucket_script": {
            "buckets_path": {
              "sales": "total_sales",
              "costs": "total_costs"
            },
            "script": "params.sales - params.costs"
          }
        }
      }
    }
  }
}
```

##### 버킷 셀렉터

스크립트는 파이프라인 집계에서 계산된 조건에 따라 버킷을 필터링합니다.

```json
GET my-index-000001/_search
{
  "aggs": {
    "sales_per_month": {
      "date_histogram": {
        "field": "date",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_sales": { "sum": { "field": "sales" } },
        "sales_bucket_filter": {
          "bucket_selector": {
            "buckets_path": {
              "totalSales": "total_sales"
            },
            "script": "params.totalSales > 1000"
          }
        }
      }
    }
  }
}
```

#### 6.5 알림 컨텍스트

##### Watcher 조건

스크립트는 알림 트리거 조건을 평가합니다.

##### Watcher 변환

스크립트는 알림 작업 내에서 데이터를 변환합니다.

---

### 7. Lucene 표현식 언어

#### 7.1 개요

Lucene 표현식은 JavaScript 표현식을 바이트코드로 컴파일하여 "고성능 사용자 정의 랭킹 및 정렬 함수"를 제공합니다. 설계는 낮은 문서당 오버헤드를 통해 속도를 우선시하여 "네이티브 스크립트를 작성한 것보다 더 빠르게" 실행할 수 있습니다.

#### 7.2 구문 기본

표현식은 "JavaScript 구문의 하위 집합: 단일 표현식"을 지원합니다. 사용 가능한 변수에는 문서 필드(예: `doc['myfield'].value`), 필드 메서드, 스크립트 매개변수 및 script_score 컨텍스트의 `_score`가 포함됩니다.

#### 7.3 숫자 필드 연산

API는 필드 값과 통계 함수에 대한 접근을 제공합니다:

| 접근 방법 | 설명 |
|-----------|------|
| `doc['field_name'].value` | 기본 접근 (double 반환) |
| `.min()` | 최소값 |
| `.max()` | 최대값 |
| `.median()` | 중앙값 |
| `.avg()` | 평균 |
| `.sum()` | 합계 |
| `.empty` | 비어 있는지 여부 (boolean) |
| `.length` | 개수 |

불리언 필드는 숫자로 매핑됩니다: true = 1, false = 0.

```json
GET my-index-000001/_search
{
  "sort": [
    {
      "_script": {
        "type": "number",
        "script": {
          "lang": "expression",
          "source": "doc['price'].value * doc['quantity'].value"
        },
        "order": "desc"
      }
    }
  ]
}
```

#### 7.4 날짜 필드 기능

날짜 필드는 1970년 1월 1일 이후 밀리초를 나타냅니다. API는 다음과 같은 시간 구성 요소를 포함합니다:

| 접근 방법 | 설명 |
|-----------|------|
| `doc['field_name'].date.year` | 연도 |
| `doc['field_name'].date.monthOfYear` | 월 (1-12) |
| `doc['field_name'].date.dayOfMonth` | 일 |
| `doc['field_name'].date.dayOfWeek` | 요일 (1-7) |
| `doc['field_name'].date.hourOfDay` | 시 |
| `doc['field_name'].date.minuteOfHour` | 분 |
| `doc['field_name'].date.secondOfMinute` | 초 |

```
doc['date1'].date.year - doc['date0'].date.year
```

#### 7.5 지리 포인트 지원

표현식은 `.lat` 및 `.lon` 속성을 통해 지리 좌표에 접근합니다. `haversin()` 함수는 거리를 계산합니다:

```
haversin(38.9072, 77.0369, doc['location'].lat, doc['location'].lon)
```

#### 7.6 주요 제한 사항

"숫자, 불리언, 날짜 및 geo_point 필드만 접근할 수 있으며" 저장된 필드는 사용할 수 없습니다.

---

### 8. 검색 템플릿

#### 8.1 개요

검색 템플릿은 변수를 받아 처리하는 저장된 검색으로, 최종 사용자에게 쿼리 구문을 노출하지 않고 쿼리를 재사용할 수 있게 합니다. 커스텀 애플리케이션이나 검색 인터페이스 구현에 특히 유용합니다.

#### 8.2 템플릿 생성

템플릿은 `lang: "mustache"`로 저장된 스크립트 생성 API를 사용하여 생성됩니다:

```json
PUT _scripts/my-search-template
{
  "script": {
    "lang": "mustache",
    "source": {
      "query": {
        "match": {
          "message": "{{query_string}}"
        }
      },
      "from": "{{from}}",
      "size": "{{size}}"
    }
  }
}
```

#### 8.3 변수 구문

"Mustache 변수는 이중 중괄호로 묶입니다: `{{my-var}}`" 실행 중에 `params` 객체의 값으로 대체됩니다.

#### 8.4 주요 작업

##### 유효성 검사

렌더 검색 템플릿 API는 실제 검색을 실행하지 않고 다양한 매개변수로 템플릿을 테스트하여 렌더링된 출력을 표시합니다.

```json
POST _render/template
{
  "id": "my-search-template",
  "params": {
    "query_string": "hello world",
    "from": 0,
    "size": 10
  }
}
```

##### 실행

검색 템플릿 API는 사용자 정의 `params`로 저장된 템플릿을 실행합니다:

```json
GET my-index-000001/_search/template
{
  "id": "my-search-template",
  "params": {
    "query_string": "hello world",
    "from": 0,
    "size": 10
  }
}
```

다중 검색 템플릿 API를 사용하면 여러 템플릿 검색을 한 번에 실행할 수 있으며, 개별 요청보다 오버헤드가 줄어드는 경우가 많습니다.

#### 8.5 고급 기능

##### 기본값

변수의 기본값을 설정하려면 다음 구문을 사용합니다:

```
{{my-var}}{{^my-var}}default value{{/my-var}}
```

##### 텍스트 처리

| 기능 | 구문 |
|------|------|
| URL 인코딩 | `{{#url}}{{value}}{{/url}}` |
| 배열 연결 | `{{#join}}{{array}}{{/join}}` |
| JSON 변환 | `{{#toJson}}{{object}}{{/toJson}}` |

##### 조건부 로직

섹션(`{{#condition}}...{{/condition}}`)과 반전 섹션(`{{^condition}}...{{/condition}}`)을 사용하면 템플릿 내에서 if/else 동작을 구현할 수 있습니다.

```json
{
  "script": {
    "lang": "mustache",
    "source": """
      {
        "query": {
          "bool": {
            "must": [
              {{#use_term}}
              { "term": { "status": "{{status}}" } }
              {{/use_term}}
              {{^use_term}}
              { "match_all": {} }
              {{/use_term}}
            ]
          }
        }
      }
    """
  }
}
```

#### 8.6 Mustache 기능

템플릿은 중첩 객체를 위한 섹션과 배열 반복을 위한 리스트를 지원합니다. 설정 구분자 구문을 사용하면 기본 이중 괄호를 사용자 정의 구분자로 바꿀 수 있습니다.

지원되지 않음: 부분(Partials)은 Elasticsearch 검색 템플릿에서 사용할 수 없습니다.

---

### 9. 스크립트 보안

#### 9.1 보안 아키텍처

Painless는 보안을 핵심 설계 원칙으로 하는 "목적에 맞게 구축된" 언어로 작동합니다. 보안 모델은 여러 계층을 사용합니다:

1. 세분화된 허용 목록: "허용 목록에 없는 모든 것은 오류가 발생합니다." 이는 무단 작업을 방지합니다.

2. 운영 체제 샌드박싱: Elasticsearch는 Seccomp(Linux), Seatbelt(macOS) 및 ActiveProcessLimit(Windows)를 사용하여 프로세스 분기 또는 외부 프로세스 실행을 방지합니다.

3. 집계 스크립트 제한: 스크립트 메트릭 집계의 스크립트는 정의된 목록으로 제한되거나 완전히 비활성화될 수 있습니다.

#### 9.2 주요 보안 설정

##### 허용된 스크립트 유형

`script.allowed_types`를 구성하여 실행되는 스크립트를 제어합니다:
- `inline`: 인라인 스크립트만 허용
- `stored`: 저장된 스크립트만 허용
- `none`: 모든 스크립트 실행 방지

Kibana 사용자 참고: 일부 Kibana 기능이 인라인 스크립트에 의존하므로 `inline` 스크립트를 허용하도록 설정하세요.

##### 허용된 스크립트 컨텍스트

`script.allowed_contexts`를 사용하여 허용된 실행 컨텍스트를 지정합니다(예: "score, update"). 기본값은 모든 컨텍스트를 허용합니다.

##### 스크립트 메트릭 집계 제어

집계의 스크립트를 다음 설정으로 제한합니다:
- `search.aggs.only_allowed_metric_scripts: true`
- `search.aggs.allowed_inline_metric_scripts`(목록 또는 빈 배열)
- `search.aggs.allowed_stored_metric_scripts`(목록 또는 빈 배열)

#### 9.3 리소스 접근 제한

Painless는 합법적인 작업(검색 점수 및 데이터 처리 등)을 위한 유연성을 유지하면서 클러스터 보안을 손상시킬 수 있는 파일 시스템, 네트워크 및 기타 시스템 리소스에 대한 접근을 방지합니다.

---

### 10. 스크립트와 검색 성능

#### 10.1 스크립트 캐싱 기본

Elasticsearch는 자동 컴파일 캐싱을 통해 스크립트 성능을 최적화합니다. "컴파일된 스크립트는 캐시에 배치되어 스크립트를 참조하는 요청이 컴파일 페널티를 발생시키지 않습니다."

#### 10.2 캐시 구성

크기 관리: 캐시는 동시에 접근하는 모든 스크립트를 수용할 수 있도록 충분히 커야 합니다. 노드 통계로 시스템을 모니터링하고, 캐시 제거가 빈번하게 발생하면서 컴파일 횟수가 증가하는 추세가 보이면 캐시 설정을 조정하세요.

주요 설정:
- `script.cache.max_size`: 캐시 용량 제어
- `script.cache.expire`: 시간 기반 만료 구성(기본적으로 스크립트는 만료되지 않음)
- `script.max_size_in_bytes`: 65,535바이트 기본 크기 제한을 늘리려면 이 값을 설정

#### 10.3 검색 성능 최적화

##### 핵심 트레이드오프

스크립트는 Elasticsearch의 인덱스 구조를 활용할 수 없어 잠재적으로 느린 쿼리가 발생할 수 있습니다. "스크립트는 매우 유용하지만 Elasticsearch의 인덱스 구조나 관련 최적화를 사용할 수 없습니다."

##### 실용적인 최적화 전략

검색 쿼리 중에 값을 계산하는 대신 인제스트 중에 계산하고 저장하세요:

예제 시나리오: 검색 중 `math_score`와 `verbal_score`를 합산하는 스크립트를 사용하는 대신, 문서 인제스트 중에 이 합계를 `total_score` 필드로 미리 계산합니다.

구현 단계:
1. 매핑에 계산된 필드 추가
2. 스크립트 프로세서가 있는 인제스트 파이프라인 생성
3. 인덱싱 중 파이프라인 사용
4. 스크립트 대신 미리 계산된 필드에 대해 쿼리

결과: 이 접근 방식은 "인덱스 프로세스를 느리게 하지만 더 빠른 검색을 가능하게 하여" 거의 실시간 쿼리 응답을 가능하게 합니다.

```json
// 1. 매핑에 필드 추가
PUT my-index-000001
{
  "mappings": {
    "properties": {
      "math_score": { "type": "integer" },
      "verbal_score": { "type": "integer" },
      "total_score": { "type": "integer" }
    }
  }
}

// 2. 인제스트 파이프라인 생성
PUT _ingest/pipeline/calculate-total
{
  "processors": [
    {
      "script": {
        "source": "ctx.total_score = ctx.math_score + ctx.verbal_score"
      }
    }
  ]
}

// 3. 파이프라인을 사용하여 인덱싱
PUT my-index-000001/_doc/1?pipeline=calculate-total
{
  "math_score": 85,
  "verbal_score": 90
}

// 4. 미리 계산된 필드에 대해 쿼리
GET my-index-000001/_search
{
  "query": {
    "range": {
      "total_score": {
        "gte": 150
      }
    }
  }
}
```

---

### 11. 스크립트 API

#### 11.1 개요

스크립트 API를 통해 저장된 스크립트와 검색 템플릿을 관리할 수 있으며, 사용 가능한 스크립트 컨텍스트와 언어를 조회하는 기능도 제공합니다.

#### 11.2 주요 엔드포인트

##### 스크립트 관리

| 엔드포인트 | 설명 |
|------------|------|
| `GET /_scripts/{id}` | 저장된 스크립트 또는 검색 템플릿 검색 |
| `DELETE /_scripts/{id}` | 저장된 스크립트 또는 검색 템플릿 제거 |
| `POST /_scripts/{id}/{context}` | 스크립트 또는 검색 템플릿 생성 또는 업데이트 |

##### 스크립트 정보

| 엔드포인트 | 설명 |
|------------|------|
| `GET /_script_context` | 지원되는 스크립트 컨텍스트 목록 |
| `GET /_script_language` | 지원되는 스크립트 언어 목록 |

##### 스크립트 실행

| 엔드포인트 | 설명 |
|------------|------|
| `POST /_scripts/painless/_execute` | Painless 언어를 사용하여 스크립트 실행 |

#### 11.3 저장된 스크립트 생성 또는 업데이트

##### 엔드포인트

- `PUT /_scripts/{id}`
- `POST /_scripts/{id}`
- `PUT /_scripts/{id}/{context}`
- `POST /_scripts/{id}/{context}`

##### 필요한 권한

- 클러스터 권한: `manage`

##### 경로 매개변수

| 매개변수 | 필수 | 설명 |
|----------|------|------|
| `id` | 예 | 클러스터 내에서 스크립트 또는 템플릿의 고유 식별자 |
| `context` | 아니오 | 스크립트가 실행되는 실행 컨텍스트. 지정하면 API가 해당 컨텍스트에서 스크립트를 즉시 컴파일하여 오류를 사전에 확인합니다. |

##### 쿼리 매개변수

- context: 실행 컨텍스트(둘 다 지정되면 경로 매개변수로 재정의됨)
- master_timeout: 마스터 노드에 대한 연결 대기 기간(시간 단위 지원)
- timeout: 응답 대기 기간(시간 단위 지원)

##### 요청 본문

요청에는 다음을 포함하는 `script` 객체가 필요합니다:

- lang (string): 프로그래밍 언어(`painless`, `expression`, `mustache`, `java`)
- source (string 또는 object): 스크립트 코드 또는 템플릿 정의
- options (object, 선택 사항): 언어별 구성

##### 예제

저장된 스크립트 생성:

```json
PUT _scripts/my-stored-script
{
  "script": {
    "lang": "painless",
    "source": "Math.log(_score * 2) + params['my_modifier']"
  }
}
```

검색 템플릿 생성:

```json
PUT _scripts/my-search-template
{
  "script": {
    "lang": "mustache",
    "source": {
      "query": {
        "match": {
          "message": "{{query_string}}"
        }
      },
      "from": "{{from}}",
      "size": "{{size}}"
    }
  }
}
```

응답:

```json
{
  "acknowledged": true
}
```

#### 11.4 저장된 스크립트 검색

```json
GET _scripts/my-stored-script
```

응답:

```json
{
  "_id": "my-stored-script",
  "found": true,
  "script": {
    "lang": "painless",
    "source": "Math.log(_score * 2) + params['my_modifier']"
  }
}
```

#### 11.5 저장된 스크립트 삭제

```json
DELETE _scripts/my-stored-script
```

---

### 12. 고급 스크립팅

#### 12.1 사용자 정의 스크립팅 엔진

`ScriptEngine`은 Elasticsearch 내에서 사용자 정의 스크립팅 언어를 구현하는 백엔드 역할을 합니다. `ScriptEngine` 인터페이스로 통합되며, 플러그인은 초기화 시 등록을 위해 `ScriptPlugin`을 구현합니다.

##### 주요 구현 구성 요소

언어 정의:

엔진은 `getType()`을 통해 사용자 정의 언어 식별자를 정의합니다. 예를 들어, "expert_scripts"가 쿼리의 `lang` 매개변수 값이 됩니다.

스크립트 인식:

`compile()` 메서드는 유효한 스크립트 소스를 식별합니다. 예를 들어 "pure_df"를 스크립트 소스로 인식하면 사용자가 `source` 매개변수에서 참조합니다.

사용자 정의 로직:

`execute()` 메서드는 점수 메커니즘을 구현합니다.

##### 사용 시기

표준 Painless에서 제공하지 않는 저수준 스크립팅 기능이 필요한 경우 사용자 정의 스크립트 엔진을 구현합니다:
- 점수 중 용어 빈도 접근이 필요한 스크립트
- 사용자 정의 구문 요구 사항이 있는 특수 언어

#### 12.2 Painless API 참조

Painless는 컨텍스트별로 허용된 메서드와 클래스의 엄격한 목록을 유지하여 모든 스크립트가 안전하게 실행되도록 보장합니다.

##### API 구조

문서는 API를 두 가지 카테고리로 구성합니다:
1. 공유 API - 모든 스크립팅 컨텍스트에서 사용 가능
2. 특수 API - 컨텍스트별 기능

##### 사용 가능한 컨텍스트

참조 문서는 23개의 스크립팅 컨텍스트를 다룹니다:
- 집계 작업(Aggs, Aggs Combine, Aggs Init, Aggs Map, Aggs Reduce)
- 검색 함수(Score, Filter, Interval)
- 데이터 처리(Ingest, Analysis, Update)
- 모니터링(Watcher Condition, Watcher Transform)
- 템플릿 작업 및 정렬 함수

##### 메서드 소스

대부분의 메서드는 JRE에서 직접 노출되며, 일부는 Elasticsearch 또는 Painless 자체에서 제공됩니다.

---

### 참고 자료

- [Elasticsearch 스크립팅 가이드](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting.html)
- [Painless 스크립팅 언어](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-guide.html)
- [Painless 언어 명세](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-lang-spec.html)
- [스크립트 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/script-apis.html)
- [검색 템플릿](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html)
- [스크립팅 보안](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-security.html)
- [스크립트와 검색 성능](https://www.elastic.co/guide/en/elasticsearch/reference/current/scripts-and-search-speed.html)
- [Painless 실행 컨텍스트](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-contexts.html)

---

## ES|QL (Elasticsearch Query Language)

### 1. ES|QL 개요

#### 1.1 ES|QL이란?

ES|QL(Elasticsearch Query Language)은 데이터를 필터링, 변환, 분석하기 위한 파이프라인 기반 쿼리 언어입니다. ES|QL을 사용하면 특정 이벤트를 찾고, 통계 분석을 수행하며, 시각화를 생성할 수 있습니다.

ES|QL은 Elasticsearch에 저장된 데이터와 향후 다른 런타임의 데이터를 필터링·변환·분석하는 강력한 방법을 제공합니다. 최종 사용자, SRE 팀, 애플리케이션 개발자, 관리자 모두 쉽게 배우고 사용할 수 있도록 설계되었습니다.

#### 1.2 주요 특징

- 파이프라인 기반: 파이프(`|`)로 데이터를 단계별로 조작·변환합니다. 각 작업의 출력이 다음 작업의 입력이 되는 방식으로 일련의 처리 단계를 구성할 수 있습니다.

- 고성능 실행 엔진: 새로운 ES|QL 실행 엔진은 성능을 염두에 두고 설계되었습니다. 행 단위가 아닌 블록 단위로 작동하며, 벡터화 및 캐시 지역성을 목표로 하고, 특수화 및 멀티스레딩을 수용합니다.

- 전용 엔드포인트: ES|QL은 Query DSL로 변환되지 않으며, `_query` 엔드포인트를 통해 Elasticsearch 내부에서 직접 실행됩니다.

- 다양한 사용 환경: Elasticsearch API, Kibana Discover, 대시보드, Dev Tools, Elastic Security 및 Observability의 분석 도구에서 ES|QL을 사용할 수 있습니다.

#### 1.3 ES|QL 사용 가능 환경

- Elasticsearch `_query` 엔드포인트를 통한 프로그래밍 방식 접근
- Kibana Discover
- Kibana 대시보드
- Dev Tools
- Elastic Security 분석 도구
- Elastic Observability 분석 도구

---

### 2. ES|QL 구문 (Syntax)

#### 2.1 기본 구조

ES|QL 쿼리는 소스 명령(source command)으로 시작하고, 선택적으로 처리 명령(processing commands)이 파이프 문자(|)로 구분되어 뒤따릅니다.

```
소스-명령
| 처리-명령1
| 처리-명령2
```

쿼리 결과는 마지막 처리 명령이 생성하는 테이블입니다.

#### 2.2 구문 규칙

- 대소문자: ES|QL 키워드는 대소문자를 구분하지 않습니다.
- 가독성: 처리 명령마다 줄을 바꾸고 들여쓰기를 적용할 수 있습니다.
- 기본 제한: 명시적인 LIMIT를 지정하지 않으면 암시적으로 1,000개 제한이 적용됩니다.

#### 2.3 기본 예제

```esql
FROM kibana_sample_data_logs
| LIMIT 10
| KEEP @timestamp, clientip, machine.os
```

설명:
- `FROM` 명령은 인덱스, 데이터뷰 또는 별칭을 선택해 행 테이블로 이벤트를 반환합니다.
- `LIMIT` 명령은 반환할 행 수를 제한합니다.
- `KEEP` 명령은 반환할 열을 지정합니다.

#### 2.4 주석

ES|QL은 주석을 지원합니다:

```esql
FROM employees
| WHERE emp_no == 10001  // 특정 직원 필터링
| KEEP first_name, last_name, salary
```

#### 2.5 이스케이프 및 특수 문자

특수 문자가 포함된 인덱스 이름이나 식별자를 이스케이프하려면 백틱(`` ` ``)을 사용합니다. 큰따옴표(`"`)는 문자열 리터럴에만 사용됩니다.

```esql
FROM `my-index-*`
| LIMIT 10
```

#### 2.6 날짜 수학 (Date Math)

인덱스, 별칭 및 데이터 스트림을 참조할 때 날짜 수학을 사용할 수 있습니다. 이는 시계열 데이터에 유용합니다.

```esql
FROM "<logs-{now/d}>"
| LIMIT 10
```

---

### 3. ES|QL 명령 (Commands)

ES|QL 명령은 두 가지 유형으로 나뉩니다:
- 소스 명령 (Source Commands): 쿼리의 시작점으로, 데이터를 가져옵니다.
- 처리 명령 (Processing Commands): 입력 테이블을 수정합니다.

#### 3.1 소스 명령

##### 3.1.1 FROM

`FROM` 명령은 데이터 스트림, 인덱스 또는 별칭에서 데이터가 포함된 테이블을 반환합니다. 결과 테이블의 각 행은 문서를 나타내고, 각 열은 필드에 해당합니다.

구문:
```esql
FROM <인덱스_패턴>
```

예제:
```esql
// 단일 인덱스
FROM employees
| LIMIT 10

// 와일드카드 패턴
FROM projects*
| LIMIT 10

// 여러 인덱스 (쉼표로 구분)
FROM employees, contractors
| LIMIT 10
```

메타데이터 필드 사용:
```esql
FROM employees METADATA _index, _id
| KEEP _index, _id, first_name, last_name
```

지원되는 메타데이터 필드:
- `_index`: 문서가 속한 인덱스 이름 (keyword 타입)
- `_id`: 문서 ID (keyword 타입)
- `_score`: 관련성 점수 (전문 검색 함수 사용 시 갱신됨)

##### 3.1.2 ROW

`ROW` 명령은 지정한 값으로 하나 이상의 열이 있는 행을 생성합니다. 테스트에 유용합니다.

구문:
```esql
ROW <column1>=<value1>, <column2>=<value2>, ...
```

예제:
```esql
ROW a=1, b="hello", c=true

ROW name="John Doe", age=30, is_active=true
| EVAL greeting = CONCAT("Hello, ", name)
```

다중값 열:
```esql
ROW key=[1, 2, 3, 4, 5]
| MV_EXPAND key
```

##### 3.1.3 SHOW

`SHOW` 명령은 배포 및 기능에 대한 정보를 반환합니다.

구문:
```esql
SHOW INFO
```

예제:
```esql
SHOW INFO
```

이 명령은 배포 버전, 빌드 날짜, 빌드 해시를 반환합니다.

##### 3.1.4 TS (Time Series) - 기술 프리뷰

`TS` 명령은 시계열 데이터 스트림에서 메트릭 값을 집계하는 데 사용됩니다.

특징:
- 시계열 분석은 시간 버킷별로 메트릭 값을 요약하는 집계 쿼리를 기반으로 합니다.
- ES|QL 컴퓨트 엔진을 통한 벡터화된 쿼리 실행을 추가합니다.
- DSL 쿼리 대비 10배 이상의 성능 향상을 제공합니다.

---

#### 3.2 처리 명령

##### 3.2.1 WHERE

`WHERE` 명령은 지정한 조건이 true인 행만 남긴 테이블을 반환합니다.

구문:
```esql
WHERE <조건>
```

예제:
```esql
// 기본 필터링
FROM employees
| WHERE salary > 50000
| LIMIT 10

// 날짜 수학을 사용한 시간 범위 필터링
FROM sample_data
| WHERE @timestamp > NOW() - 1 hour

// 여러 조건
FROM employees
| WHERE department == "Engineering" AND years_of_experience >= 5
| KEEP first_name, last_name, department
```

##### 3.2.2 EVAL

`EVAL` 명령은 계산된 값을 새 열로 테이블에 추가합니다.

구문:
```esql
EVAL <새_열_이름> = <표현식>
```

예제:
```esql
// 단위 변환
FROM sample_data
| EVAL duration_ms = event_duration / 1000000.0

// 여러 열 계산
FROM kibana_sample_data_logs
| STATS avg_bytes = AVG(bytes) BY geo.src
| EVAL avg_bytes_kb = ROUND(avg_bytes / 1024, 2)
| EVAL avg_bytes_mb = ROUND(avg_bytes / (1024 * 1024), 4)
```

##### 3.2.3 STATS ... BY

`STATS` 명령은 AVG, COUNT, SUM 등의 집계 함수를 사용해 데이터를 그룹화하고 집계합니다.

구문:
```esql
STATS <집계_표현식> [BY <그룹_필드>]
```

지원되는 집계 함수:
- `AVG`: 평균값
- `COUNT`: 개수
- `COUNT_DISTINCT`: 고유 값 개수
- `MAX`: 최대값
- `MIN`: 최소값
- `MEDIAN`: 중앙값
- `MEDIAN_ABSOLUTE_DEVIATION`: 중앙값 절대 편차
- `PERCENTILE`: 백분위수
- `SUM`: 합계
- `STD_DEV`: 표준 편차
- `VARIANCE`: 분산
- `VALUES`: 모든 고유 값
- `TOP`: 상위 N개 값
- `WEIGHTED_AVG`: 가중 평균

예제:
```esql
// 기본 집계
FROM employees
| STATS avg_salary = AVG(salary), max_salary = MAX(salary)

// 그룹별 집계
FROM employees
| STATS
    avg_salary = AVG(salary),
    median_salary = MEDIAN(salary),
    headcount = COUNT(*)
  BY department
| SORT avg_salary DESC

// 여러 그룹 필드
FROM kibana_sample_data_logs
| STATS total = COUNT(*) BY machine.os, geo.src
| SORT total DESC
| LIMIT 20
```

##### 3.2.4 INLINE STATS (INLINESTATS) - 프리뷰

`INLINESTATS` 명령은 그룹별 통계를 계산한 뒤 그 결과를 원래 행에 새 열로 추가합니다. STATS와 달리 행을 축소하지 않습니다.

구문:
```esql
INLINESTATS <집계_표현식> [BY <그룹_필드>]
```

예제:
```esql
// 각 행에 그룹 통계 추가
FROM employees
| INLINESTATS avg_dept_salary = AVG(salary) BY department
| EVAL salary_vs_avg = salary - avg_dept_salary
| KEEP first_name, department, salary, avg_dept_salary, salary_vs_avg
```

동작 방식:
입력 데이터:
| a | b  |
|---|-----|
| 1 | 10 |
| 1 | 20 |
| 2 | 20 |
| 2 | 15 |

`INLINESTATS MIN(b) BY a` 실행 후:
| a | b  | MIN(b) |
|---|-----|--------|
| 1 | 10 | 10     |
| 1 | 20 | 10     |
| 2 | 20 | 15     |
| 2 | 15 | 15     |

##### 3.2.5 SORT

`SORT` 명령은 하나 이상의 열을 기준으로 결과를 정렬합니다.

구문:
```esql
SORT <열> [ASC|DESC] [NULLS FIRST|NULLS LAST]
```

예제:
```esql
// 오름차순 정렬
FROM employees
| SORT salary ASC
| LIMIT 10

// 내림차순 정렬
FROM kibana_sample_data_logs
| STATS total = COUNT(machine.os) BY machine.os
| SORT total DESC

// 여러 열로 정렬
FROM employees
| SORT department ASC, salary DESC
| LIMIT 20
```

##### 3.2.6 LIMIT

`LIMIT` 명령은 반환할 행 수를 제한합니다.

구문:
```esql
LIMIT <숫자>
```

제한 사항:
- LIMIT 값과 무관하게 쿼리는 10,000개를 초과하는 행을 반환하지 않습니다. 이 상한선은 설정으로 변경할 수 있습니다.
- Kibana Discover에서는 10,000개 이상의 행을 표시하지 않습니다.

예제:
```esql
FROM employees
| SORT salary DESC
| LIMIT 5
```

명령 순서의 중요성:
```esql
// 정렬 후 제한 (상위 3개)
FROM employees
| SORT salary DESC
| LIMIT 3

// 제한 후 정렬 (임의의 3개를 정렬) - 다른 결과!
FROM employees
| LIMIT 3
| SORT salary DESC
```

##### 3.2.7 KEEP

`KEEP` 명령은 반환할 열과 순서를 지정합니다. 와일드카드를 지원합니다.

구문:
```esql
KEEP <열1>, <열2>, ...
```

예제:
```esql
// 특정 열만 유지
FROM employees
| KEEP first_name, last_name, salary, department

// 와일드카드 사용
FROM employees
| KEEP first_*, *_date, salary
```

##### 3.2.8 DROP

`DROP` 명령은 지정한 열을 테이블에서 제거합니다.

구문:
```esql
DROP <열1>, <열2>, ...
```

예제:
```esql
FROM employees
| DROP internal_id, temp_field, debug_info
```

##### 3.2.9 RENAME

`RENAME` 명령은 하나 이상의 열 이름을 바꿉니다.

구문:
```esql
RENAME <기존_이름> AS <새_이름>
```

주의 사항:
- 새 이름이 기존 열 이름과 충돌하면 기존 열이 제거됩니다.
- 여러 열을 같은 이름으로 변경하면 가장 오른쪽 열만 유지됩니다.

예제:
```esql
FROM employees
| RENAME first_name AS fname, last_name AS lname
| KEEP fname, lname, salary
```

##### 3.2.10 DISSECT

`DISSECT` 명령은 구분자 기반 패턴으로 문자열을 분해하고 지정한 키를 열로 추출합니다.

특징:
- 구분자 기반으로 동작해 GROK보다 빠릅니다.
- 데이터 구조가 일관되게 반복될 때 효과적입니다.
- 추출된 값은 모두 keyword 타입으로 출력됩니다.

구문:
```esql
DISSECT <필드> "<패턴>"
```

예제:
```esql
FROM logs
| DISSECT message "%{timestamp} %{level} %{message_text}"

// 로그 에이전트 파싱
FROM kibana_sample_data_logs
| DISSECT agent "%{browser}/%{version}"
| KEEP browser, version, @timestamp
```

##### 3.2.11 GROK

`GROK` 명령은 정규 표현식 기반 패턴으로 문자열을 분석하고 지정한 키를 열로 추출합니다.

특징:
- 정규 표현식 기반으로 DISSECT보다 표현력이 강하지만 일반적으로 느립니다.
- 행마다 텍스트 구조가 달라질 때 더 적합합니다.
- Oniguruma 정규 표현식 라이브러리를 사용합니다.
- 사용자 정의 패턴이나 다중 패턴은 지원하지 않습니다.

구문:
```esql
GROK <필드> "<패턴>"
```

예제:
```esql
FROM kibana_sample_data_logs
| GROK agent "%{WORD:browser}/%{NUMBER:version}"
| KEEP browser, version, @timestamp

// 복잡한 로그 파싱
FROM logs
| GROK message "%{IP:client_ip} - %{USER:user} \\[%{HTTPDATE:timestamp}\\] \"%{WORD:method} %{URIPATHPARAM:request}\""
```

##### 3.2.12 DISSECT와 GROK 하이브리드 사용

일부 라인이 일관되게 반복되지만 전체 라인은 그렇지 않은 하이브리드 사용 사례에서 DISSECT와 GROK를 함께 사용할 수 있습니다.

```esql
FROM logs
| DISSECT message "%{timestamp} %{rest}"
| GROK rest "%{LOGLEVEL:level} %{GREEDYDATA:details}"
```

##### 3.2.13 ENRICH

`ENRICH` 명령은 다른 인덱스의 데이터를 이용해 현재 테이블을 보강합니다.

구문:
```esql
ENRICH <정책_이름> ON <매치_필드> [WITH <필드1>, <필드2>, ...]
```

특징:
- 기본적으로 정책에 정의된 모든 보강 필드가 열로 추가됩니다.
- `WITH`로 추가할 필드를 명시적으로 지정할 수 있습니다.
- `WITH new_name=<field>` 형태로 열 이름을 변경할 수 있습니다.
- 이름 충돌이 발생하면 새로 생성된 열이 기존 열을 덮어씁니다.

예제:
```esql
FROM kibana_sample_data_logs
| STATS avg_bytes = AVG(bytes) BY geo.src
| EVAL avg_bytes_kb = ROUND(avg_bytes / 1024, 2)
| ENRICH geo-data ON geo.src WITH continent, country
| KEEP avg_bytes_kb, geo.src, country, continent
```

##### 3.2.14 MV_EXPAND

`MV_EXPAND` 명령은 다중값 열의 각 값을 별도 행으로 펼치며, 나머지 열은 복제됩니다.

구문:
```esql
MV_EXPAND <다중값_열>
```

주의 사항:
- MV_EXPAND로 생성된 출력 행은 순서가 보장되지 않으며, 이전 SORT의 정렬 순서를 따르지 않을 수 있습니다.

예제:
```esql
ROW tags=["elasticsearch", "search", "analytics"]
| MV_EXPAND tags

// 실제 사용 예
FROM products
| MV_EXPAND categories
| STATS product_count = COUNT(*) BY categories
```

##### 3.2.15 LOOKUP JOIN

`LOOKUP JOIN` 명령은 ES|QL 쿼리 결과 테이블과 지정한 조회 인덱스의 일치하는 레코드를 결합합니다.

특징:
- Elasticsearch의 첫 번째 SQL 스타일 JOIN입니다.
- LEFT OUTER JOIN으로 동작합니다.
- `lookup` 인덱스 모드가 필요합니다.
- 조회 인덱스는 단일 샤드로 구성됩니다.
- Elasticsearch 9.2부터 Cross-Cluster Search(CCS)를 지원합니다.

구문:
```esql
LOOKUP JOIN <조회_인덱스> ON <매치_필드>
```

예제:
```esql
// 서비스 소유자 정보 조인
FROM app_logs
| LOOKUP JOIN service_owners ON service_id

// IP 위협 인텔리전스 조인
FROM firewall_logs
| LOOKUP JOIN threat_intel ON source_ip
| WHERE threat_level == "high"
```

---

### 4. ES|QL 함수 및 연산자

#### 4.1 집계 함수 (Aggregation Functions)

STATS 및 INLINESTATS 명령에서 지원되는 집계 함수:

| 함수 | 설명 |
|------|------|
| `AVG(field)` | 입력 값의 산술 평균 |
| `COUNT(*)` | 행의 총 개수 |
| `COUNT(field)` | 필드 값의 개수 (null 제외) |
| `COUNT_DISTINCT(field)` | 고유 값의 개수 |
| `MAX(field)` | 최대값 |
| `MIN(field)` | 최소값 |
| `MEDIAN(field)` | 중앙값 |
| `MEDIAN_ABSOLUTE_DEVIATION(field)` | 중앙값 절대 편차 |
| `PERCENTILE(field, percentile)` | 지정된 백분위수 값 |
| `SUM(field)` | 합계 |
| `STD_DEV(field)` | 표준 편차 |
| `VARIANCE(field)` | 분산 |
| `VALUES(field)` | 모든 고유 값 |
| `TOP(field, limit, order)` | 상위/하위 N개 값 |
| `WEIGHTED_AVG(value, weight)` | 가중 평균 |
| `SAMPLE(field, n)` | 무작위 n개 샘플 |

예제:
```esql
FROM employees
| STATS
    avg_salary = AVG(salary),
    max_salary = MAX(salary),
    min_salary = MIN(salary),
    median_salary = MEDIAN(salary),
    total_employees = COUNT(*),
    unique_departments = COUNT_DISTINCT(department),
    salary_sum = SUM(salary),
    salary_stddev = STD_DEV(salary)
  BY department
| SORT avg_salary DESC
```

#### 4.2 시계열 집계 함수 (Time-Series Aggregate Functions)

| 함수 | 설명 |
|------|------|
| `AVG_OVER_TIME` | 시간에 따른 평균 |
| `COUNT_OVER_TIME` | 시간에 따른 개수 |
| `MAX_OVER_TIME` | 시간에 따른 최대값 |
| `MIN_OVER_TIME` | 시간에 따른 최소값 |
| `SUM_OVER_TIME` | 시간에 따른 합계 |
| `FIRST_OVER_TIME` | 시간 범위의 첫 번째 값 |
| `LAST_OVER_TIME` | 시간 범위의 마지막 값 |
| `DELTA` | 첫 번째와 마지막 값의 차이 |
| `DERIV` | 초당 변화율 |
| `RATE` | 초당 증가율 |

#### 4.3 수학 함수 (Mathematical Functions)

| 함수 | 설명 |
|------|------|
| `ABS(n)` | 절대값 |
| `ACOS(n)` | 아크코사인 |
| `ASIN(n)` | 아크사인 |
| `ATAN(n)` | 아크탄젠트 |
| `ATAN2(y, x)` | 두 인수의 아크탄젠트 |
| `CBRT(n)` | 세제곱근 |
| `CEIL(n)` | 올림 (가장 작은 정수 >= n) |
| `COS(n)` | 코사인 |
| `COSH(n)` | 하이퍼볼릭 코사인 |
| `E()` | 오일러 수 (e) |
| `EXP(n)` | e의 n승 |
| `FLOOR(n)` | 내림 (가장 큰 정수 <= n) |
| `HYPOT(a, b)` | 피타고라스 정리 (sqrt(a^2 + b^2)) |
| `LOG(n)` | 자연 로그 |
| `LOG10(n)` | 밑이 10인 로그 |
| `PI()` | 원주율 (π) |
| `POW(base, exp)` | 거듭제곱 |
| `ROUND(n, decimals)` | 반올림 |
| `SIGNUM(n)` | 부호 (-1, 0, 1) |
| `SIN(n)` | 사인 |
| `SINH(n)` | 하이퍼볼릭 사인 |
| `SQRT(n)` | 제곱근 |
| `TAN(n)` | 탄젠트 |
| `TANH(n)` | 하이퍼볼릭 탄젠트 |
| `TAU()` | 타우 (2π) |

예제:
```esql
FROM measurements
| EVAL
    absolute_value = ABS(temperature_diff),
    rounded = ROUND(price, 2),
    ceiling = CEIL(score),
    floor_val = FLOOR(score),
    power = POW(base, exponent),
    square_root = SQRT(area)
| KEEP absolute_value, rounded, ceiling, floor_val, power, square_root
```

#### 4.4 문자열 함수 (String Functions)

| 함수 | 설명 |
|------|------|
| `BIT_LENGTH(s)` | 비트 단위 길이 |
| `BYTE_LENGTH(s)` | 바이트 단위 길이 |
| `CONCAT(s1, s2, ...)` | 문자열 연결 |
| `CONTAINS(s, substring)` | 부분 문자열 포함 여부 |
| `ENDS_WITH(s, suffix)` | 접미사로 끝나는지 확인 |
| `FROM_BASE64(s)` | Base64 디코딩 |
| `HASH(algorithm, s)` | 해시 생성 |
| `LEFT(s, length)` | 왼쪽에서 length 문자 |
| `LENGTH(s)` | 문자열 길이 |
| `LOCATE(substring, s)` | 부분 문자열 위치 (1부터 시작) |
| `LTRIM(s)` | 왼쪽 공백 제거 |
| `MD5(s)` | MD5 해시 |
| `REPEAT(s, n)` | 문자열 n번 반복 |
| `REPLACE(s, regex, replacement)` | 정규식 대체 |
| `REVERSE(s)` | 문자열 뒤집기 |
| `RIGHT(s, length)` | 오른쪽에서 length 문자 |
| `RTRIM(s)` | 오른쪽 공백 제거 |
| `SHA1(s)` | SHA1 해시 |
| `SHA256(s)` | SHA256 해시 |
| `SPACE(n)` | n개의 공백 |
| `SPLIT(s, delimiter)` | 구분자로 분할 |
| `STARTS_WITH(s, prefix)` | 접두사로 시작하는지 확인 |
| `SUBSTRING(s, start, length)` | 부분 문자열 추출 |
| `TO_BASE64(s)` | Base64 인코딩 |
| `TO_LOWER(s)` | 소문자로 변환 |
| `TO_UPPER(s)` | 대문자로 변환 |
| `TRIM(s)` | 양쪽 공백 제거 |
| `URL_ENCODE(s)` | URL 인코딩 |
| `URL_DECODE(s)` | URL 디코딩 |

예제:
```esql
FROM users
| EVAL
    full_name = CONCAT(first_name, " ", last_name),
    name_upper = TO_UPPER(first_name),
    email_domain = SUBSTRING(email, LOCATE("@", email) + 1, LENGTH(email)),
    trimmed = TRIM(description),
    has_prefix = STARTS_WITH(username, "admin")
| KEEP full_name, name_upper, email_domain, trimmed, has_prefix
```

#### 4.5 날짜/시간 함수 (Date-Time Functions)

| 함수 | 설명 |
|------|------|
| `DATE_DIFF(unit, start, end)` | 두 날짜의 차이 |
| `DATE_EXTRACT(part, date)` | 날짜 부분 추출 |
| `DATE_FORMAT(format, date)` | 날짜 형식화 |
| `DATE_PARSE(format, string)` | 문자열을 날짜로 파싱 |
| `DATE_TRUNC(interval, date)` | 날짜 잘라내기 |
| `DAY_NAME(date)` | 요일 이름 |
| `MONTH_NAME(date)` | 월 이름 |
| `NOW()` | 현재 시간 |
| `TRANGE(from, to)` | 시간 범위 필터 |

예제:
```esql
FROM employees
| EVAL
    year_hired = DATE_TRUNC(1 year, hire_date),
    years_employed = DATE_DIFF("year", hire_date, NOW()),
    formatted_date = DATE_FORMAT("yyyy-MM-dd", hire_date),
    day = DAY_NAME(hire_date)
| KEEP first_name, hire_date, year_hired, years_employed, formatted_date, day

// 날짜 파싱
FROM logs
| EVAL parsed_date = DATE_PARSE("yyyy-MM-dd HH:mm:ss", timestamp_string)
```

#### 4.6 조건 함수 (Conditional Functions)

| 함수 | 설명 |
|------|------|
| `CASE(cond1, val1, cond2, val2, ..., default)` | 조건부 값 반환 |
| `COALESCE(v1, v2, ...)` | 첫 번째 non-null 값 |
| `GREATEST(v1, v2, ...)` | 최대값 |
| `LEAST(v1, v2, ...)` | 최소값 |
| `NULLIF(v1, v2)` | v1==v2이면 null, 아니면 v1 |

CASE 함수:
- 조건과 값의 쌍을 받아 첫 번째 true 조건에 해당하는 값을 반환합니다.
- 인수 개수가 홀수이면 마지막 인수가 기본값으로 사용됩니다.
- 인수 개수가 짝수이고 일치하는 조건이 없으면 null을 반환합니다.

예제:
```esql
FROM employees
| EVAL
    salary_tier = CASE(
        salary >= 100000, "High",
        salary >= 50000, "Medium",
        "Low"
    ),
    display_name = COALESCE(nickname, first_name, "Unknown"),
    max_value = GREATEST(score1, score2, score3),
    min_value = LEAST(score1, score2, score3)
| KEEP first_name, salary, salary_tier, display_name, max_value, min_value
```

#### 4.7 타입 변환 함수 (Type Conversion Functions)

| 함수 | 설명 |
|------|------|
| `TO_BOOLEAN(v)` | 불리언으로 변환 |
| `TO_DOUBLE(v)` | double로 변환 |
| `TO_INTEGER(v)` | 정수로 변환 |
| `TO_LONG(v)` | long으로 변환 |
| `TO_STRING(v)` | 문자열로 변환 |
| `TO_IP(v)` | IP 주소로 변환 |
| `TO_DATETIME(v)` | 날짜/시간으로 변환 |
| `TO_VERSION(v)` | 버전으로 변환 |
| `TO_GEOPOINT(v)` | 지리적 포인트로 변환 |
| `TO_GEOSHAPE(v)` | 지리적 도형으로 변환 |
| `TO_CARTESIANPOINT(v)` | 데카르트 포인트로 변환 |
| `TO_CARTESIANSHAPE(v)` | 데카르트 도형으로 변환 |
| `TO_DENSE_VECTOR(v)` | 밀집 벡터로 변환 |

타입 변환 구문:
```esql
// 함수 형태
EVAL client_ip = TO_IP(client_ip)

// 축약 구문
EVAL client_ip = client_ip::IP
```

여러 인덱스에서 타입 불일치 처리:
```esql
FROM index1, index2
| EVAL client_ip = TO_IP(client_ip)  // union 타입을 단일 타입으로 변환
| KEEP client_ip, @timestamp
```

#### 4.8 다중값 함수 (Multivalue Functions)

다중값 필드를 처리하는 함수:

| 함수 | 설명 |
|------|------|
| `MV_APPEND(v1, v2)` | 두 다중값을 결합 |
| `MV_AVG(mv)` | 다중값의 평균 |
| `MV_CONCAT(mv, delimiter)` | 다중값을 문자열로 연결 |
| `MV_CONTAINS(mv, value)` | 값 포함 여부 |
| `MV_COUNT(mv)` | 다중값의 개수 |
| `MV_DEDUPE(mv)` | 중복 제거 |
| `MV_FIRST(mv)` | 첫 번째 값 |
| `MV_LAST(mv)` | 마지막 값 |
| `MV_MAX(mv)` | 최대값 |
| `MV_MIN(mv)` | 최소값 |
| `MV_MEDIAN(mv)` | 중앙값 |
| `MV_PERCENTILE(mv, p)` | 백분위수 |
| `MV_SORT(mv)` | 정렬 |
| `MV_SLICE(mv, start, end)` | 슬라이스 |
| `MV_SUM(mv)` | 합계 |
| `MV_UNION(mv1, mv2)` | 합집합 |
| `MV_ZIP(mv1, mv2)` | 쌍으로 결합 |

예제:
```esql
ROW a=[3, 5, 1, 6]
| EVAL
    avg_a = MV_AVG(a),
    count_a = MV_COUNT(a),
    max_a = MV_MAX(a),
    min_a = MV_MIN(a)

// 다중값을 문자열로 변환
ROW username=["Dave", "Mike", "Keith", "Ben"]
| EVAL combined = MV_CONCAT(username, ", ")
```

집계 함수와 함께 사용:
```esql
FROM employees
| STATS avg_salary_change = ROUND(AVG(MV_AVG(salary_change)), 10)
```

#### 4.9 전문 검색 함수 (Full-Text Search Functions)

##### MATCH

`MATCH` 함수는 지정된 필드에 대해 매치 쿼리를 수행합니다. Elasticsearch Query DSL의 `match` 쿼리와 동일한 동작입니다.

사용 가능한 필드 타입:
- `text`, `semantic_text` 등 text 계열
- `keyword`, `boolean`, `date`, 숫자 타입

구문:
```esql
// 함수 형태
WHERE MATCH(field, "search terms")

// 축약 연산자 형태
WHERE field:"search terms"
```

예제:
```esql
// 기본 매치
FROM library
| WHERE MATCH(title, "elasticsearch guide")
| LIMIT 10

// 옵션 포함
FROM library
| WHERE MATCH(author, "Frank Herbert", {"minimum_should_match": 2, "operator": "AND"})
| LIMIT 5

// 축약 형태
FROM articles
| WHERE content:"machine learning"
| LIMIT 10
```

##### MATCH_PHRASE

`MATCH_PHRASE` 함수는 정확한 구문을 검색합니다.

```esql
FROM articles
| WHERE MATCH_PHRASE(content, "quick brown fox")
| LIMIT 10
```

##### QSTR

`QSTR` 함수는 Query DSL의 `query_string` 쿼리와 동일한 기능을 제공합니다. 와일드카드, 불리언 로직, 다중 필드 검색 등 고급 검색 패턴을 지원합니다.

```esql
FROM logs
| WHERE QSTR("status:error AND (service:auth* OR service:api*)")
| LIMIT 100
```

관련성 점수:
```esql
// _score를 얻으려면 METADATA _score를 명시적으로 선언해야 합니다
FROM articles METADATA _score
| WHERE MATCH(content, "elasticsearch")
| SORT _score DESC
| LIMIT 10
```

#### 4.10 공간 함수 (Spatial Functions)

##### 거리 및 관계 함수

| 함수 | 설명 |
|------|------|
| `ST_DISTANCE(geom1, geom2)` | 두 지오메트리 간의 거리 |
| `ST_INTERSECTS(geom1, geom2)` | 교차 여부 |
| `ST_DISJOINT(geom1, geom2)` | 분리 여부 (ST_INTERSECTS의 반대) |
| `ST_CONTAINS(geom1, geom2)` | 포함 여부 |
| `ST_WITHIN(geom1, geom2)` | 내부에 있는지 여부 |

##### 지오메트리 속성 함수

| 함수 | 설명 |
|------|------|
| `ST_X(point)` | X 좌표 (경도) |
| `ST_Y(point)` | Y 좌표 (위도) |
| `ST_NPOINTS(geom)` | 포인트 개수 |
| `ST_ENVELOPE(geom)` | 바운딩 박스 |
| `ST_XMAX(geom)` | 최대 X 좌표 |
| `ST_XMIN(geom)` | 최소 X 좌표 |
| `ST_YMAX(geom)` | 최대 Y 좌표 |
| `ST_YMIN(geom)` | 최소 Y 좌표 |

##### 그리드 인코딩 함수

| 함수 | 설명 |
|------|------|
| `ST_GEOHASH(point, precision)` | Geohash 인코딩 |
| `ST_GEOTILE(point, zoom)` | 지도 타일 인코딩 |
| `ST_GEOHEX(point, precision)` | H3 육각형 인코딩 |

예제:
```esql
FROM airports
| WHERE ST_DISTANCE(location, TO_GEOPOINT("POINT(-73.935242 40.730610)")) < 50000
| EVAL distance_km = ST_DISTANCE(location, TO_GEOPOINT("POINT(-73.935242 40.730610)")) / 1000
| KEEP name, city, distance_km
| SORT distance_km
| LIMIT 10
```

#### 4.11 IP 함수

| 함수 | 설명 |
|------|------|
| `CIDR_MATCH(ip, cidr_block)` | IP가 CIDR 블록에 포함되는지 확인 |

예제:
```esql
FROM firewall_logs
| WHERE CIDR_MATCH(source_ip, "10.0.0.0/8") OR CIDR_MATCH(source_ip, "192.168.0.0/16")
| STATS count = COUNT(*) BY action
```

#### 4.12 연산자 (Operators)

##### 비교 연산자

| 연산자 | 설명 |
|--------|------|
| `==` | 같음 |
| `!=` | 같지 않음 |
| `<` | 보다 작음 |
| `<=` | 보다 작거나 같음 |
| `>` | 보다 큼 |
| `>=` | 보다 크거나 같음 |

##### 논리 연산자

| 연산자 | 설명 |
|--------|------|
| `AND` | 논리 AND |
| `OR` | 논리 OR |
| `NOT` | 논리 NOT |

##### 산술 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | 더하기 |
| `-` | 빼기 |
| `*` | 곱하기 |
| `/` | 나누기 |
| `%` | 나머지 (모듈로) |

##### 기타 연산자

| 연산자 | 설명 |
|--------|------|
| `LIKE` | 패턴 매칭 |
| `RLIKE` | 정규식 매칭 |
| `IN (v1, v2, ...)` | 값 포함 여부 |
| `IS NULL` | null 여부 |
| `IS NOT NULL` | non-null 여부 |

예제:
```esql
FROM employees
| WHERE
    salary > 50000
    AND department IN ("Engineering", "Product")
    AND manager IS NOT NULL
    AND first_name LIKE "J*"
| KEEP first_name, last_name, salary, department
```

---

### 5. ES|QL REST API

#### 5.1 API 엔드포인트

ES|QL 쿼리를 실행하려면 `POST /_query` 엔드포인트를 사용합니다.

#### 5.2 기본 요청

```json
POST /_query
{
  "query": "FROM employees | WHERE salary > 50000 | LIMIT 10"
}
```

#### 5.3 응답 형식

지원되는 형식:
- `json` (기본값)
- `csv`
- `tsv`
- `txt`
- `yaml`
- `cbor`
- `smile`
- `arrow`

형식 지정 방법:
```
POST /_query?format=csv
```

또는 HTTP 헤더 사용:
```
Accept: text/csv
```

#### 5.4 컬럼형 결과

기본적으로 ES|QL은 결과를 행 형태로 반환합니다. `json`, `yaml`, `cbor`, `smile` 형식에서는 결과를 컬럼형으로 받을 수 있습니다.

```json
POST /_query?format=json
{
  "query": "FROM employees | LIMIT 5",
  "columnar": true
}
```

컬럼형 응답 예제:
```json
{
  "columns": [
    {"name": "first_name", "type": "keyword"},
    {"name": "salary", "type": "long"}
  ],
  "values": [
    ["John", "Jane", "Bob", "Alice", "Charlie"],
    [50000, 60000, 55000, 70000, 45000]
  ]
}
```

#### 5.5 필터 매개변수

Query DSL 쿼리를 `filter` 매개변수에 지정하면 ES|QL 쿼리가 실행될 문서 집합을 사전에 필터링할 수 있습니다.

```json
POST /_query
{
  "query": "FROM employees | STATS avg_salary = AVG(salary) BY department",
  "filter": {
    "range": {
      "hire_date": {
        "gte": "2020-01-01"
      }
    }
  }
}
```

#### 5.6 프로파일링

`profile` 매개변수를 `true`로 설정하면 응답에 쿼리 실행 방식에 대한 추가 프로파일 정보가 포함됩니다.

```json
POST /_query
{
  "query": "FROM employees | WHERE salary > 50000 | LIMIT 10",
  "profile": true
}
```

#### 5.7 매개변수

쿼리 매개변수화를 위해 `params` 배열을 사용할 수 있습니다:

```json
POST /_query
{
  "query": "FROM employees | WHERE department == ? AND salary > ?",
  "params": ["Engineering", 50000]
}
```

---

### 6. Kibana에서 ES|QL 사용

#### 6.1 Discover에서 ES|QL 시작하기

1. Kibana에서 Discover 로 이동합니다.
2. 애플리케이션 메뉴 바에서 Try ES|QL 을 선택합니다.
3. ES|QL 편집기가 활성화되어 쿼리를 작성할 수 있습니다.

#### 6.2 ES|QL 편집기 기능

- 자동 완성: 명령, 함수, 필드 이름에 대한 자동 완성
- 구문 강조: ES|QL 쿼리의 구문 강조
- 인라인 문서: 함수 및 명령에 대한 도움말

#### 6.3 시각화

ES|QL 결과를 기반으로 Kibana에서 시각화를 생성할 수 있습니다:
- 테이블
- 막대 차트
- 라인 차트
- 기타 Lens 시각화

#### 6.4 제한 사항

- Discover는 최대 10,000개의 행만 표시합니다.
- Discover는 최대 50개의 열만 표시합니다.
- CSV 내보내기는 최대 10,000개의 행으로 제한됩니다.
- ES|QL 모드에서는 필터 바 인터페이스가 비활성화됩니다.

---

### 7. ES|QL 제한 사항

#### 7.1 결과 크기 제한

- LIMIT 명령의 값과 무관하게 쿼리는 최대 10,000개의 행만 반환합니다.
- 이 상한은 `esql.query.result_truncation_max_size` 설정으로 조정할 수 있습니다.
- 명시적 LIMIT가 없을 때 적용되는 기본값은 1,000입니다.

#### 7.2 지원되지 않는 필드 타입

지원하지 않는 타입의 열을 쿼리하면 오류가 반환됩니다. 쿼리에서 명시적으로 사용하지 않는 경우, 지원되지 않는 타입의 열은 null로 반환됩니다(중첩 필드 제외).

지원되지 않거나 제한된 타입:
- `nested`: 전혀 반환되지 않음
- `flattened`: ES|QL에서 지원되지 않음
- 공간 타입: SORT 명령에서 지원되지 않음
- `aggregate_metric_double`: TO_AGGREGATE_METRIC_DOUBLE 함수 사용 필요
- `dense_vector`: 특정 함수 사용 필요

#### 7.3 _source 필드 제한

ES|QL은 `_source` 필드가 비활성화된 인덱스를 지원하지 않습니다.

#### 7.4 텍스트 필드 제한

- 전문 검색 함수(MATCH, QSTR)를 사용하지 않는 텍스트 필드 쿼리는 대소문자를 구분합니다.
- 매핑 기반 분석이 ES|QL 쿼리에 적용되지 않아 오탐지 또는 미탐지가 발생할 수 있습니다.
- 권장 사항: 텍스트 필드 대신 keyword 하위 필드를 직접 쿼리하세요.

#### 7.5 전문 검색 제한

전문 검색 함수(MATCH, QSTR 등)는 FROM 소스 명령 바로 뒤의 WHERE 절에서 사용해야 합니다. 다른 처리 명령 이후에 사용하면 유효성 검사 오류가 발생합니다.

```esql
// 올바른 사용
FROM articles
| WHERE MATCH(content, "elasticsearch")
| LIMIT 10

// 오류 발생 가능
FROM articles
| EVAL content_length = LENGTH(content)
| WHERE MATCH(content, "elasticsearch")  // 유효성 검사 오류
| LIMIT 10
```

#### 7.6 대규모 쿼리 제한

필터 없이 많은 인덱스를 한 번에 쿼리하면 Kibana에서 다음과 같은 오류가 발생할 수 있습니다:
```
[esql] > Unexpected error from Elasticsearch: The content length (536885793) is bigger than the maximum allowed string (536870888).
```

---

### 8. 실전 예제

#### 8.1 로그 분석

```esql
// 최근 1시간의 오류 로그 분석
FROM logs-*
| WHERE @timestamp > NOW() - 1 hour AND level == "error"
| STATS error_count = COUNT(*) BY service, error_type
| SORT error_count DESC
| LIMIT 20
```

#### 8.2 사용자 활동 분석

```esql
FROM user_activity
| WHERE @timestamp > NOW() - 1 day
| STATS
    page_views = COUNT(*),
    unique_pages = COUNT_DISTINCT(page_url),
    avg_duration = AVG(duration_ms)
  BY user_id
| SORT page_views DESC
| LIMIT 100
```

#### 8.3 성능 메트릭 분석

```esql
FROM metrics-*
| WHERE @timestamp > NOW() - 6 hours
| EVAL response_time_ms = response_time / 1000000
| STATS
    avg_response = AVG(response_time_ms),
    p95_response = PERCENTILE(response_time_ms, 95),
    p99_response = PERCENTILE(response_time_ms, 99),
    request_count = COUNT(*)
  BY service_name, endpoint
| WHERE avg_response > 100
| SORT p99_response DESC
```

#### 8.4 데이터 보강 및 변환

```esql
FROM orders
| EVAL order_date = DATE_TRUNC(1 day, created_at)
| STATS
    total_revenue = SUM(amount),
    order_count = COUNT(*),
    avg_order_value = AVG(amount)
  BY order_date, region
| ENRICH region-info ON region WITH country, continent
| EVAL revenue_per_order = ROUND(total_revenue / order_count, 2)
| SORT order_date DESC, total_revenue DESC
```

#### 8.5 지리적 분석

```esql
FROM stores
| EVAL distance_km = ST_DISTANCE(location, TO_GEOPOINT("POINT(-122.4194 37.7749)")) / 1000
| WHERE distance_km < 50
| STATS
    total_sales = SUM(daily_sales),
    store_count = COUNT(*)
  BY BUCKET(distance_km, 10) AS distance_bucket
| SORT distance_bucket
```

#### 8.6 보안 로그 분석

```esql
FROM firewall_logs METADATA _index
| WHERE @timestamp > NOW() - 24 hours
| EVAL
    is_internal = CIDR_MATCH(source_ip, "10.0.0.0/8") OR CIDR_MATCH(source_ip, "192.168.0.0/16"),
    is_blocked = action == "DENY"
| STATS
    blocked_count = COUNT(*) WHERE is_blocked,
    total_count = COUNT(*)
  BY source_ip, is_internal
| EVAL block_rate = ROUND(blocked_count * 100.0 / total_count, 2)
| WHERE block_rate > 50
| SORT blocked_count DESC
| LIMIT 50
```

#### 8.7 텍스트 로그 파싱

```esql
FROM raw_logs
| GROK message "%{IP:client_ip} - %{USER:user} \\[%{HTTPDATE:timestamp}\\] \"%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:http_version}\" %{NUMBER:status} %{NUMBER:bytes}"
| EVAL
    status_code = TO_INTEGER(status),
    response_bytes = TO_INTEGER(bytes)
| STATS
    request_count = COUNT(*),
    total_bytes = SUM(response_bytes),
    error_count = COUNT(*) WHERE status_code >= 400
  BY method, LEFT(request, 50) AS endpoint_prefix
| SORT request_count DESC
```

#### 8.8 다중값 필드 처리

```esql
FROM products
| MV_EXPAND tags
| STATS
    product_count = COUNT(*),
    avg_price = AVG(price)
  BY tags
| SORT product_count DESC
| LIMIT 20
```

---

### 9. 참고 자료

- [ES|QL 레퍼런스](https://www.elastic.co/docs/reference/query-languages/esql)
- [ES|QL 구문 레퍼런스](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-syntax.html)
- [ES|QL 명령](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-commands.html)
- [ES|QL 함수 및 연산자](https://www.elastic.co/guide/en/elasticsearch/reference/current/esql-functions-operators.html)
- [ES|QL REST API](https://www.elastic.co/docs/reference/query-languages/esql/esql-rest)
- [ES|QL 제한 사항](https://www.elastic.co/docs/reference/query-languages/esql/limitations)
- [Kibana에서 ES|QL 사용하기](https://www.elastic.co/docs/explore-analyze/query-filter/languages/esql-kibana)
- [ES|QL 전문 검색](https://www.elastic.co/search-labs/blog/filtering-in-esql-full-text-search-match-qstr-kql)
- [ES|QL LOOKUP JOIN](https://www.elastic.co/docs/reference/query-languages/esql/esql-lookup-join)
- [ES|QL 시계열 지원](https://www.elastic.co/search-labs/blog/esql-elasticsearch-9-2-multi-field-joins-ts-command)
