# RFC 8259 - The JavaScript Object Notation (JSON) Data Interchange Format

> **발행일**: 2017년 12월
> **대체 문서**: RFC 7159
> **상태**: Standards Track (Internet Standard)

## 1. 개요

JSON(JavaScript Object Notation)은 **경량의 텍스트 기반 데이터 교환 형식**입니다. JavaScript의 객체 리터럴 문법에서 파생되었지만, **언어 독립적**이며 거의 모든 프로그래밍 언어에서 파싱 및 생성이 가능합니다.

### 핵심 특징

| 특징 | 설명 |
|------|------|
| **텍스트 기반** | 사람이 읽고 쓸 수 있음 |
| **언어 독립적** | 모든 언어에서 사용 가능 |
| **경량** | XML보다 간결함 |
| **구조화** | 객체와 배열로 복잡한 데이터 표현 |

---

## 2. JSON 문법

### 2.1 값(Value)의 종류

JSON은 6가지 타입의 값을 지원합니다:

```
value = object / array / number / string / true / false / null
```

| 타입 | 설명 | 예시 |
|------|------|------|
| **object** | 키-값 쌍의 순서 없는 집합 | `{"name": "홍길동"}` |
| **array** | 값의 순서 있는 목록 | `[1, 2, 3]` |
| **number** | 숫자 (정수 또는 실수) | `42`, `-3.14`, `1.0e10` |
| **string** | 유니코드 문자열 | `"Hello, 세계"` |
| **boolean** | 참/거짓 | `true`, `false` |
| **null** | 빈 값 | `null` |

### 2.2 객체(Object)

```json
{
  "name": "홍길동",
  "age": 30,
  "isStudent": false,
  "address": {
    "city": "서울",
    "zip": "12345"
  }
}
```

**문법 규칙**:
- 중괄호 `{}`로 감싸기
- 키는 반드시 **문자열** (큰따옴표 필수)
- 키와 값은 콜론 `:`으로 구분
- 멤버(키-값 쌍)는 쉼표 `,`로 구분
- 키의 순서는 의미가 없음 (unordered)

### 2.3 배열(Array)

```json
[
  "사과",
  "바나나",
  "체리"
]
```

**문법 규칙**:
- 대괄호 `[]`로 감싸기
- 값들은 쉼표 `,`로 구분
- 순서가 의미를 가짐 (ordered)
- 다양한 타입 혼합 가능

### 2.4 문자열(String)

```json
"Hello, World!"
"한글 문자열"
"탭:\t 줄바꿈:\n 따옴표:\""
"유니코드: \u0041 = A"
```

**이스케이프 시퀀스**:

| 시퀀스 | 의미 |
|--------|------|
| `\"` | 큰따옴표 |
| `\\` | 역슬래시 |
| `\/` | 슬래시 |
| `\b` | 백스페이스 |
| `\f` | 폼 피드 |
| `\n` | 줄바꿈 (Line Feed) |
| `\r` | 캐리지 리턴 |
| `\t` | 탭 |
| `\uXXXX` | 유니코드 (4자리 16진수) |

### 2.5 숫자(Number)

```json
{
  "integer": 42,
  "negative": -17,
  "decimal": 3.14159,
  "exponent": 6.022e23,
  "negativeExp": 1.6e-19
}
```

**숫자 규칙**:
- 10진수만 허용 (8진수, 16진수 불가)
- 앞에 0을 붙일 수 없음 (`07` 불가)
- `NaN`, `Infinity` 불가
- 선택적으로 소수부와 지수부 포함 가능

---

## 3. 인코딩

### 3.1 문자 인코딩

> **JSON 텍스트는 UTF-8로 인코딩되어야 합니다(MUST).**

| 인코딩 | 상태 |
|--------|------|
| **UTF-8** | 필수 (기본값, BOM 없이) |
| UTF-16 | 허용되지 않음 |
| UTF-32 | 허용되지 않음 |

### 3.2 BOM (Byte Order Mark)

```
JSON 텍스트의 시작에 BOM을 추가해서는 안 됩니다(MUST NOT).
```

### 3.3 공백 (Whitespace)

다음 4가지 공백 문자만 허용됩니다:

| 문자 | 코드 | 이름 |
|------|------|------|
| Space | 0x20 | 공백 |
| Tab | 0x09 | 수평 탭 |
| LF | 0x0A | 줄바꿈 |
| CR | 0x0D | 캐리지 리턴 |

---

## 4. 파싱 고려사항

### 4.1 숫자 정밀도

> "JSON 파서는 숫자의 정밀도나 범위에 제한을 둘 수 있습니다."

**권장사항**:
- IEEE 754 배정밀도 범위 내의 숫자 사용
- 정수: -(2^53)+1 ~ (2^53)-1 범위 권장
- 매우 큰 숫자는 문자열로 전달 권장

```json
{
  "safeInteger": 9007199254740991,
  "bigNumberAsString": "123456789012345678901234567890"
}
```

### 4.2 중복 키 처리

> "객체 내 키 이름은 고유해야 합니다(SHOULD)."

```json
{
  "name": "첫번째",
  "name": "두번째"
}
```

- 중복 키가 있을 경우 동작이 **예측 불가능**
- 파서마다 처리 방식이 다름 (첫번째/마지막 값 사용)
- **중복 키 사용 금지** 권장

### 4.3 문자열 비교

- 객체 멤버 접근 시 **유니코드 코드 포인트** 기반 비교
- 대소문자 구분
- NFC/NFD 정규화 없이 바이트 단위 비교

---

## 5. MIME 타입

### 5.1 Content-Type

```
application/json
```

### 5.2 HTTP 헤더 예시

```http
Content-Type: application/json; charset=utf-8
```

> **참고**: `charset` 파라미터는 선택적이며, 생략 시 UTF-8로 간주됩니다.

---

## 6. 보안 고려사항

### 6.1 eval() 사용 금지

```javascript
// 위험! 절대 사용 금지
const data = eval('(' + jsonString + ')');

// 안전한 방법
const data = JSON.parse(jsonString);
```

### 6.2 JSON 하이재킹

JSON 배열을 직접 반환하는 API는 취약할 수 있습니다:

```javascript
// 취약한 응답
[{"secret": "password123"}]

// 안전한 응답 (객체로 감싸기)
{"data": [{"secret": "password123"}]}
```

### 6.3 리소스 제한

파서는 다음에 대한 제한을 고려해야 합니다:
- 최대 중첩 깊이
- 문자열 최대 길이
- 숫자 정밀도/범위
- 객체/배열의 최대 요소 수

---

## 7. JSON vs 다른 형식

### 7.1 JSON vs XML

| 특성 | JSON | XML |
|------|------|-----|
| 가독성 | 높음 | 중간 |
| 크기 | 작음 | 큼 |
| 스키마 | 선택적 (JSON Schema) | 강력함 (XSD) |
| 속성 | 없음 | 있음 |
| 주석 | 불가 | 가능 |
| 배열 | 네이티브 지원 | 태그 반복 필요 |

### 7.2 JSON vs YAML

| 특성 | JSON | YAML |
|------|------|------|
| 가독성 | 중간 | 매우 높음 |
| 주석 | 불가 | 가능 |
| 복잡성 | 단순 | 복잡 |
| 파싱 속도 | 빠름 | 느림 |
| 보안 | 안전 | 취약점 있음 |

---

## 8. 예제

### 8.1 REST API 응답

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "user": {
      "id": 12345,
      "username": "hong_gildong",
      "email": "hong@example.com",
      "roles": ["user", "admin"],
      "createdAt": "2023-01-15T09:30:00Z"
    }
  },
  "meta": {
    "requestId": "abc-123",
    "timestamp": "2023-12-01T10:00:00Z"
  }
}
```

### 8.2 설정 파일

```json
{
  "database": {
    "host": "localhost",
    "port": 5432,
    "name": "myapp_db",
    "ssl": true
  },
  "server": {
    "port": 8080,
    "cors": {
      "origins": ["https://example.com", "https://api.example.com"],
      "methods": ["GET", "POST", "PUT", "DELETE"]
    }
  },
  "features": {
    "darkMode": true,
    "analytics": false
  }
}
```

---

## 9. 요약

RFC 8259는 JSON 데이터 형식을 정의합니다:

- **6가지 데이터 타입**: object, array, string, number, boolean, null
- **텍스트 기반**: UTF-8 인코딩 필수
- **언어 독립적**: 모든 프로그래밍 언어에서 사용 가능
- **간결함**: XML보다 적은 오버헤드
- **MIME 타입**: `application/json`

JSON은 현대 웹 API의 **사실상 표준 데이터 형식**으로, REST API, 설정 파일, 데이터 저장 등 다양한 용도로 사용됩니다.

---

## 참고 자료

- [RFC 8259 원문](https://www.rfc-editor.org/rfc/rfc8259)
- [JSON.org](https://www.json.org/json-ko.html)
- [JSON Schema](https://json-schema.org/)
- [ECMA-404 JSON 표준](https://www.ecma-international.org/publications-and-standards/standards/ecma-404/)
