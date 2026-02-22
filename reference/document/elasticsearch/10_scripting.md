# 스크립팅 (Scripting)

> 이 문서는 Elasticsearch 9.x 공식 문서의 "Scripting" 섹션을 한국어로 번역한 것입니다.
> 원문: https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting.html

---

## 목차

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

## 1. 스크립팅 개요

### 1.1 스크립팅이란?

스크립팅을 사용하면 Elasticsearch에서 사용자 정의 표현식을 평가할 수 있습니다. 예를 들어, 스크립트를 사용하여 계산된 값을 필드로 반환하거나 쿼리에 대한 사용자 정의 점수를 평가할 수 있습니다.

### 1.2 기본 스크립팅 언어

Elasticsearch의 기본 스크립팅 언어는 Painless 입니다. Painless는 Elasticsearch를 위해 특별히 설계된 언어로, 스크립팅 API의 모든 용도에 사용할 수 있으며 가장 높은 유연성을 제공합니다.

### 1.3 사용 가능한 스크립팅 언어

| 언어 | 샌드박스 | 플러그인 유형 | 용도 |
|------|----------|---------------|------|
| Painless | 예 | 내장 | Elasticsearch 전용 범용 스크립팅 |
| Expression | 예 | 내장 | 사용자 정의 랭킹 및 정렬 |
| Mustache | 예 | 내장 | 템플릿 작업 |
| Java | 아니오 | 사용 불가 | 전문가 수준 API |

Painless 외의 다른 언어들은 특정 목적에 유용할 수 있지만 유연성이 제한됩니다.

---

## 2. 스크립트 사용 방법

### 2.1 스크립트 구조

스크립팅이 지원되는 모든 API에서 스크립트는 일관된 패턴을 따릅니다. 기본 구조는 세 가지 주요 구성 요소를 포함합니다:

```json
"script": {
  "lang":   "...",
  "source" | "id": "...",
  "params": { ... }
}
```

#### 구성 요소 설명

언어 지정 (`lang`)

스크립트 언어를 명시적으로 지정하지 않으면 기본값은 `painless`입니다. 이는 구문과 사용 가능한 함수를 결정합니다.

스크립트 소스 (`source` 또는 `id`)

스크립트는 두 가지 방식으로 제공할 수 있습니다:
- `source`: API 요청에 직접 작성하는 인라인 스크립트
- `id`: 저장된 스크립트 API를 통해 관리되는 저장된 스크립트에 대한 참조

매개변수 (`params`)

이 섹션에는 스크립트에 변수로 전달되는 명명된 매개변수가 포함됩니다.

### 2.2 첫 번째 스크립트 작성

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

### 2.3 매개변수 사용

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

### 2.4 저장된 스크립트

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

### 2.5 스크립트를 사용한 문서 업데이트

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

## 3. Painless 스크립팅 언어

### 3.1 Painless란?

Painless는 Elasticsearch 5.0에서 Groovy를 대체하기 위해 도입된 Elasticsearch의 기본 스크립팅 언어입니다. Java Virtual Machine 위에 구축되어 Java와 유사한 구문을 Elasticsearch 작업을 위해 특별히 설계된 향상된 보안 기능 및 샌드박스 보호와 결합합니다.

### 3.2 핵심 특성

#### 보안 및 성능

Painless는 "제한된 Java API에 대한 접근을 방지하는 세분화된 허용 목록"을 통해 작동합니다. 바이트코드로 직접 컴파일되어 해석 오버헤드를 제거하고 JVM 최적화를 가능하게 합니다.

#### 목적에 맞게 설계됨

범용 스크립팅 솔루션이 아니라 Painless는 "시스템 리소스에 대한 무단 접근을 방지하면서 네이티브 성능을 가능하게 하는 Elasticsearch 전용으로 설계"되었습니다.

### 3.3 주요 사용 사례

#### 검색 향상
- 비즈니스 로직을 기반으로 한 사용자 정의 검색 점수
- 쿼리 실행 중 런타임 필드 생성
- 재인덱싱 없는 실시간 필터링

#### 데이터 처리
- 인덱싱 중 문서 변환
- 비정형 필드에서 구조화된 데이터 추출
- 메트릭 및 요약 계산

#### 운영 자동화
- Watcher를 통한 알림 패턴 모니터링
- 알림을 위한 알림 페이로드 변환

### 3.4 Painless 사용 위치

개발자는 다음에서 Painless 스크립트를 작성할 수 있습니다:
- Dev Tools 콘솔
- 인제스트 파이프라인
- 검색 쿼리
- 런타임 필드
- 업데이트 API 작업
- Watcher 구성

### 3.5 기본 구문 예제

```painless
// 간단한 함수 예제
String hello(String name) {
  return "Hello, " + name + "!";
}

// 함수 호출
hello("World")  // "Hello, World!" 반환
```

---

## 4. Painless 언어 명세

### 4.1 언어 기반

Painless의 구문은 Java 구문과 유사하며, 동적 타이핑, Map 및 List 접근자 단축키, 배열 초기화자와 같은 추가 기능을 포함합니다.

구현은 파싱을 위한 ANTLR4와 바이트코드 생성을 위한 ASM을 사용하며, 스크립트는 실행을 위해 JVM 바이트코드로 직접 컴파일됩니다.

### 4.2 주석

Painless는 Java 스타일 주석을 지원합니다:

```painless
// 한 줄 주석

/*
 * 여러 줄 주석
 */
```

### 4.3 키워드

Painless의 예약어에는 다음이 포함됩니다:
- 제어 흐름: `if`, `else`, `for`, `while`, `do`, `break`, `continue`, `return`
- 타입: `boolean`, `byte`, `short`, `char`, `int`, `long`, `float`, `double`, `void`, `def`
- 기타: `true`, `false`, `null`, `new`, `instanceof`, `this`

참고: Painless는 표준 Java와 달리 `switch` 문을 지원하지 않습니다.

### 4.4 데이터 타입

#### 기본 타입

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

#### 참조 타입

참조 타입은 잠재적인 멤버 필드와 메서드를 가진 명명된 구조를 나타내며, 힙 메모리에 할당됩니다:

```painless
List l = new ArrayList();
l.add(1);
int i = l.get(0) + 2;
```

#### 동적 타입 (def)

`def` 타입은 런타임 타입 유연성을 제공하며, 단일 식별자를 통해 모든 기본 또는 참조 타입 값을 나타냅니다:

```painless
def dp = 1;                    // int를 나타냄
def dr = new ArrayList();      // ArrayList를 나타냄
dr = dp;                       // 이제 int를 나타냄
```

참고: 성능 고려사항: 명시적 타입에 비해 약간의 오버헤드가 있습니다.

#### 문자열 타입

```painless
String r = "some text";
String s = 'some text';
String t = new String("some text");
```

큰따옴표와 작은따옴표 모두 사용할 수 있습니다.

#### 배열 타입

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

### 4.5 연산자

Painless는 연산자를 6가지 주요 카테고리로 구성합니다:

#### 산술 연산자

| 연산자 | 설명 |
|--------|------|
| `+` | 더하기 |
| `-` | 빼기 |
| `*` | 곱하기 |
| `/` | 나누기 |
| `%` | 나머지 |
| `++` | 증가 |
| `--` | 감소 |

#### 비교 연산자

| 연산자 | 설명 |
|--------|------|
| `==` | 같음 |
| `!=` | 같지 않음 |
| `<` | 보다 작음 |
| `<=` | 보다 작거나 같음 |
| `>` | 보다 큼 |
| `>=` | 보다 크거나 같음 |

#### 논리 연산자

| 연산자 | 설명 |
|--------|------|
| `&&` | 논리 AND |
| `\|\|` | 논리 OR |
| `!` | 논리 NOT |

#### 비트 연산자

| 연산자 | 설명 |
|--------|------|
| `&` | 비트 AND |
| `\|` | 비트 OR |
| `^` | 비트 XOR |
| `~` | 비트 NOT |
| `<<` | 왼쪽 시프트 |
| `>>` | 오른쪽 시프트 |
| `>>>` | 부호 없는 오른쪽 시프트 |

#### 기타 연산자

| 연산자 | 설명 |
|--------|------|
| `?:` | 조건 (삼항) |
| `?:` | 엘비스 연산자 (null 병합) |
| `?.` | null 안전 접근 |
| `instanceof` | 타입 검사 |

#### 연산자 우선순위

연산자는 0(가장 높음)에서 17(가장 낮음)까지의 숫자 우선순위 척도를 따릅니다:
- 레벨 0: 괄호
- 레벨 1-3: 메서드 호출, 필드 접근, 배열 연산
- 레벨 4-6: 산술 및 시프트 연산
- 레벨 7-9: 비교 및 동등성 검사
- 레벨 10-14: 비트 및 논리 연산
- 레벨 15-17: 조건, 엘비스 및 할당 연산

### 4.6 문

#### 조건문

```painless
if (doc['status'].value == 'active') {
    // 활성 상태일 때 처리
} else if (doc['status'].value == 'pending') {
    // 대기 상태일 때 처리
} else {
    // 기타 상태 처리
}
```

#### 반복문

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

### 4.7 함수

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

### 4.8 람다 표현식

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

### 4.9 정규 표현식

Painless는 정규 표현식 상수를 패턴 생성의 기본 메커니즘으로 지원합니다:

```painless
Pattern p = /[aeiou]/
```

주의: 잘못 작성된 정규 표현식은 성능을 크게 저하시킬 수 있습니다. 가능하면 특히 자주 실행되는 스크립트에서 정규 표현식 사용을 피하세요.

#### 패턴 플래그

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

## 5. 스크립트에서 문서 필드 접근

### 5.1 업데이트 스크립트

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

### 5.2 검색 및 집계 스크립트

필드 값에 접근하는 세 가지 주요 방법이 있습니다:

#### Doc 값 (가장 빠름)

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

#### _source 필드

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

#### 저장된 필드

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

### 5.3 특수 변수

`_score`: `function_score` 쿼리, 스크립트 기반 정렬 및 집계에서 사용할 수 있습니다. 문서 관련성을 나타냅니다.

`_termStats`: `script_score` 쿼리에서 용어 통계 데이터를 제공합니다. `termFreq()`, `docFreq()`, `totalTermFreq()`, `uniqueTermsCount()` 및 `matchedTermsCount()`와 같은 메서드를 포함합니다.

---

## 6. 스크립팅 실행 컨텍스트

Painless 스크립트는 사용 가능한 변수, 허용된 API 및 반환 유형을 결정하는 특정 컨텍스트 내에서 실행됩니다.

### 6.1 데이터 수정 컨텍스트

#### 런타임 필드

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

#### 인제스트 프로세서

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

#### 업데이트

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

### 6.2 검색 및 검색 컨텍스트

#### 정렬

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

#### 필드

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

#### 필터

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

### 6.3 점수 컨텍스트

#### 점수

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

#### 유사도

스크립트는 관련성 랭킹을 위한 사용자 정의 텍스트 유사도 알고리즘을 정의합니다.

### 6.4 집계 컨텍스트

#### 메트릭 집계

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

#### 버킷 스크립트

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

#### 버킷 셀렉터

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

### 6.5 알림 컨텍스트

#### Watcher 조건

스크립트는 알림 트리거 조건을 평가합니다.

#### Watcher 변환

스크립트는 알림 작업 내에서 데이터를 변환합니다.

---

## 7. Lucene 표현식 언어

### 7.1 개요

Lucene 표현식은 JavaScript 표현식을 바이트코드로 컴파일하여 "고성능 사용자 정의 랭킹 및 정렬 함수"를 제공합니다. 설계는 낮은 문서당 오버헤드를 통해 속도를 우선시하여 "네이티브 스크립트를 작성한 것보다 더 빠르게" 실행할 수 있습니다.

### 7.2 구문 기본

표현식은 "JavaScript 구문의 하위 집합: 단일 표현식"을 지원합니다. 사용 가능한 변수에는 문서 필드(예: `doc['myfield'].value`), 필드 메서드, 스크립트 매개변수 및 script_score 컨텍스트의 `_score`가 포함됩니다.

### 7.3 숫자 필드 연산

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

### 7.4 날짜 필드 기능

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

### 7.5 지리 포인트 지원

표현식은 `.lat` 및 `.lon` 속성을 통해 지리 좌표에 접근합니다. `haversin()` 함수는 거리를 계산합니다:

```
haversin(38.9072, 77.0369, doc['location'].lat, doc['location'].lon)
```

### 7.6 주요 제한 사항

"숫자, 불리언, 날짜 및 geo_point 필드만 접근할 수 있으며" 저장된 필드는 사용할 수 없습니다.

---

## 8. 검색 템플릿

### 8.1 개요

검색 템플릿은 다양한 변수를 허용하는 저장된 검색으로, 최종 사용자에게 쿼리 구문을 노출하지 않고 재사용 가능한 쿼리를 활성화합니다. 사용자 정의 애플리케이션 및 검색 지원 인터페이스에 특히 유용합니다.

### 8.2 템플릿 생성

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

### 8.3 변수 구문

"Mustache 변수는 이중 중괄호로 묶입니다: `{{my-var}}`" 실행 중에 `params` 객체의 값으로 대체됩니다.

### 8.4 주요 작업

#### 유효성 검사

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

#### 실행

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

다중 검색 템플릿 API를 사용하여 여러 템플릿 검색을 동시에 실행할 수 있으며, 종종 개별 요청에 비해 오버헤드가 줄어듭니다.

### 8.5 고급 기능

#### 기본값

변수의 기본값을 설정하려면 다음 구문을 사용합니다:

```
{{my-var}}{{^my-var}}default value{{/my-var}}
```

#### 텍스트 처리

| 기능 | 구문 |
|------|------|
| URL 인코딩 | `{{#url}}{{value}}{{/url}}` |
| 배열 연결 | `{{#join}}{{array}}{{/join}}` |
| JSON 변환 | `{{#toJson}}{{object}}{{/toJson}}` |

#### 조건부 로직

섹션(`{{#condition}}...{{/condition}}`)과 반전 섹션(`{{^condition}}...{{/condition}}`)은 템플릿 내에서 if/else 동작을 활성화합니다.

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

### 8.6 Mustache 기능

템플릿은 중첩 객체용 섹션 과 배열 반복용 리스트 를 지원합니다. 사용자 정의 구분자는 설정 구분자 구문을 통해 기본 이중 괄호를 대체할 수 있습니다.

지원되지 않음: 부분(Partials)은 Elasticsearch 검색 템플릿에서 사용할 수 없습니다.

---

## 9. 스크립트 보안

### 9.1 보안 아키텍처

Painless는 보안을 핵심 설계 원칙으로 하는 "목적에 맞게 구축된" 언어로 작동합니다. 보안 모델은 여러 계층을 사용합니다:

1. 세분화된 허용 목록: "허용 목록에 없는 모든 것은 오류가 발생합니다." 이는 무단 작업을 방지합니다.

2. 운영 체제 샌드박싱: Elasticsearch는 Seccomp(Linux), Seatbelt(macOS) 및 ActiveProcessLimit(Windows)를 사용하여 프로세스 분기 또는 외부 프로세스 실행을 방지합니다.

3. 집계 스크립트 제한: 스크립트 메트릭 집계의 스크립트는 정의된 목록으로 제한되거나 완전히 비활성화될 수 있습니다.

### 9.2 주요 보안 설정

#### 허용된 스크립트 유형

`script.allowed_types`를 구성하여 실행되는 스크립트를 제어합니다:
- `inline`: 인라인 스크립트만 허용
- `stored`: 저장된 스크립트만 허용
- `none`: 모든 스크립트 실행 방지

Kibana 사용자 참고: 일부 Kibana 기능이 인라인 스크립트에 의존하므로 `inline` 스크립트를 허용하도록 설정하세요.

#### 허용된 스크립트 컨텍스트

`script.allowed_contexts`를 사용하여 허용된 실행 컨텍스트를 지정합니다(예: "score, update"). 기본값은 모든 컨텍스트를 허용합니다.

#### 스크립트 메트릭 집계 제어

집계의 스크립트를 다음 설정으로 제한합니다:
- `search.aggs.only_allowed_metric_scripts: true`
- `search.aggs.allowed_inline_metric_scripts`(목록 또는 빈 배열)
- `search.aggs.allowed_stored_metric_scripts`(목록 또는 빈 배열)

### 9.3 리소스 접근 제한

Painless는 합법적인 작업(검색 점수 및 데이터 처리 등)을 위한 유연성을 유지하면서 클러스터 보안을 손상시킬 수 있는 파일 시스템, 네트워크 및 기타 시스템 리소스에 대한 접근을 방지합니다.

---

## 10. 스크립트와 검색 성능

### 10.1 스크립트 캐싱 기본

Elasticsearch는 자동 컴파일 캐싱을 통해 스크립트 성능을 최적화합니다. "컴파일된 스크립트는 캐시에 배치되어 스크립트를 참조하는 요청이 컴파일 페널티를 발생시키지 않습니다."

### 10.2 캐시 구성

크기 관리: 캐시는 동시에 접근하는 모든 스크립트를 보관할 수 있도록 적절한 크기여야 합니다. 노드 통계를 사용하여 시스템을 모니터링하세요 - 상당한 캐시 제거와 함께 컴파일 수가 증가하는 것을 관찰하면 캐시 구성을 조정해야 합니다.

주요 설정:
- `script.cache.max_size`: 캐시 용량 제어
- `script.cache.expire`: 시간 기반 만료 구성(기본적으로 스크립트는 만료되지 않음)
- `script.max_size_in_bytes`: 65,535바이트 스크립트 크기 제한 증가

### 10.3 검색 성능 최적화

#### 핵심 트레이드오프

스크립트는 Elasticsearch의 인덱스 구조를 활용할 수 없어 잠재적으로 느린 쿼리가 발생할 수 있습니다. "스크립트는 매우 유용하지만 Elasticsearch의 인덱스 구조나 관련 최적화를 사용할 수 없습니다."

#### 실용적인 최적화 전략

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

## 11. 스크립트 API

### 11.1 개요

스크립트 API는 저장된 스크립트와 검색 템플릿의 관리를 활성화하고, 사용 가능한 스크립트 컨텍스트와 언어 쿼리를 지원합니다.

### 11.2 주요 엔드포인트

#### 스크립트 관리

| 엔드포인트 | 설명 |
|------------|------|
| `GET /_scripts/{id}` | 저장된 스크립트 또는 검색 템플릿 검색 |
| `DELETE /_scripts/{id}` | 저장된 스크립트 또는 검색 템플릿 제거 |
| `POST /_scripts/{id}/{context}` | 스크립트 또는 검색 템플릿 생성 또는 업데이트 |

#### 스크립트 정보

| 엔드포인트 | 설명 |
|------------|------|
| `GET /_script_context` | 지원되는 스크립트 컨텍스트 목록 |
| `GET /_script_language` | 지원되는 스크립트 언어 목록 |

#### 스크립트 실행

| 엔드포인트 | 설명 |
|------------|------|
| `POST /_scripts/painless/_execute` | Painless 언어를 사용하여 스크립트 실행 |

### 11.3 저장된 스크립트 생성 또는 업데이트

#### 엔드포인트

- `PUT /_scripts/{id}`
- `POST /_scripts/{id}`
- `PUT /_scripts/{id}/{context}`
- `POST /_scripts/{id}/{context}`

#### 필요한 권한

- 클러스터 권한: `manage`

#### 경로 매개변수

| 매개변수 | 필수 | 설명 |
|----------|------|------|
| `id` | 예 | 클러스터 내에서 스크립트 또는 템플릿의 고유 식별자 |
| `context` | 예 | 스크립트가 실행되는 실행 컨텍스트. API는 이 컨텍스트에서 스크립트를 즉시 컴파일하여 오류를 방지합니다. |

#### 쿼리 매개변수

- context: 실행 컨텍스트(둘 다 지정되면 경로 매개변수로 재정의됨)
- master_timeout: 마스터 노드에 대한 연결 대기 기간(시간 단위 지원)
- timeout: 응답 대기 기간(시간 단위 지원)

#### 요청 본문

요청에는 다음을 포함하는 `script` 객체가 필요합니다:

- lang (string): 프로그래밍 언어(`painless`, `expression`, `mustache`, `java`)
- source (string 또는 object): 스크립트 코드 또는 템플릿 정의
- options (object, 선택 사항): 언어별 구성

#### 예제

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

### 11.4 저장된 스크립트 검색

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

### 11.5 저장된 스크립트 삭제

```json
DELETE _scripts/my-stored-script
```

---

## 12. 고급 스크립팅

### 12.1 사용자 정의 스크립팅 엔진

`ScriptEngine`은 Elasticsearch 내에서 사용자 정의 스크립팅 언어를 구현하기 위한 백엔드 역할을 합니다. `ScriptEngine` 인터페이스를 통해 통합되며, 플러그인은 초기화 중 등록을 위해 `ScriptPlugin`을 구현합니다.

#### 주요 구현 구성 요소

언어 정의:

엔진은 `getType()`을 통해 사용자 정의 언어 식별자를 정의합니다. 예를 들어, "expert_scripts"가 쿼리의 `lang` 매개변수 값이 됩니다.

스크립트 인식:

`compile()` 메서드는 유효한 스크립트 소스를 식별합니다. 예를 들어 "pure_df"를 스크립트 소스로 인식하면 사용자가 `source` 매개변수에서 참조합니다.

사용자 정의 로직:

`execute()` 메서드는 점수 메커니즘을 구현합니다.

#### 사용 시기

표준 Painless에서 사용할 수 없는 고급 스크립팅 내부가 필요할 때 사용자 정의 스크립트 엔진을 구현합니다:
- 점수 중 용어 빈도 접근이 필요한 스크립트
- 사용자 정의 구문 요구 사항이 있는 특수 언어

### 12.2 Painless API 참조

Painless는 모든 스크립트가 안전하도록 컨텍스트별로 허용된 메서드와 클래스의 엄격한 목록을 가지고 있습니다.

#### API 구조

문서는 API를 두 가지 카테고리로 구성합니다:
1. 공유 API - 모든 스크립팅 컨텍스트에서 사용 가능
2. 특수 API - 컨텍스트별 기능

#### 사용 가능한 컨텍스트

참조는 23개의 고유한 스크립팅 컨텍스트를 다룹니다:
- 집계 작업(Aggs, Aggs Combine, Aggs Init, Aggs Map, Aggs Reduce)
- 검색 함수(Score, Filter, Interval)
- 데이터 처리(Ingest, Analysis, Update)
- 모니터링(Watcher Condition, Watcher Transform)
- 템플릿 작업 및 정렬 함수

#### 메서드 소스

대부분의 메서드는 Java Runtime Environment(JRE)에서 직접 노출되고, 일부는 Elasticsearch 또는 Painless 자체의 일부입니다.

---

## 참고 자료

- [Elasticsearch 스크립팅 가이드](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting.html)
- [Painless 스크립팅 언어](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-guide.html)
- [Painless 언어 명세](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-lang-spec.html)
- [스크립트 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/script-apis.html)
- [검색 템플릿](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html)
- [스크립팅 보안](https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-scripting-security.html)
- [스크립트와 검색 성능](https://www.elastic.co/guide/en/elasticsearch/reference/current/scripts-and-search-speed.html)
- [Painless 실행 컨텍스트](https://www.elastic.co/guide/en/elasticsearch/painless/current/painless-contexts.html)
