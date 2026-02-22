# RFC 9110 - HTTP Semantics

> 발행일: 2022년 6월
> 대체 문서: RFC 2616, RFC 7231 등
> 상태: Standards Track

## 1. 개요

RFC 9110은 HTTP의 핵심 의미론(Semantics)을 정의하는 문서입니다. HTTP/1.1, HTTP/2, HTTP/3 모든 버전에 공통으로 적용되는 의미론을 버전별 메시지 문법과 분리하여 정리했습니다.

### HTTP란?

> "HTTP는 분산, 협업, 하이퍼텍스트 정보 시스템을 위한 무상태(stateless) 애플리케이션 레벨 프로토콜"

HTTP는 균일한 인터페이스(uniform interface) 를 통해 클라이언트가 리소스 자체가 아닌 표현(representation) 을 주고받으며 리소스와 상호작용합니다.

## 2. HTTP 메서드

### 2.1 메서드 개요

| 메서드 | 설명 | 안전 | 멱등 |
|--------|------|:----:|:----:|
| GET | 리소스의 표현 조회 | ✓ | ✓ |
| HEAD | GET과 동일하나 본문 제외 | ✓ | ✓ |
| POST | 리소스가 정의한 의미에 따라 표현 처리 | ✗ | ✗ |
| PUT | 대상 리소스의 상태를 생성 또는 교체 | ✗ | ✓ |
| DELETE | 대상 리소스와의 연결 제거 | ✗ | ✓ |
| CONNECT | 중개자를 통한 터널 설정 | ✗ | ✗ |
| OPTIONS | 대상 리소스의 통신 옵션 설명 | ✓ | ✓ |
| TRACE | 메시지 루프백 진단 수행 | ✓ | ✓ |

### 2.2 안전한 메서드 (Safe Methods)

서버 상태를 변경하지 않는 읽기 전용 메서드입니다:
- GET, HEAD, OPTIONS, TRACE

### 2.3 멱등성 (Idempotent Methods)

동일한 요청을 여러 번 수행해도 결과가 동일한 메서드입니다:
- GET, HEAD, PUT, DELETE, OPTIONS, TRACE

```
예시: DELETE /users/123
- 첫 번째 요청: 사용자 삭제됨 → 200 OK
- 두 번째 요청: 이미 삭제됨 → 404 Not Found
→ 최종 상태는 동일 (사용자가 없음)
```

### 2.4 메서드 상세

#### GET
```http
GET /users/123 HTTP/1.1
Host: api.example.com
```
- 지정된 리소스의 표현을 검색
- 가장 빈번하게 사용되는 메서드

#### POST
```http
POST /users HTTP/1.1
Host: api.example.com
Content-Type: application/json

{"name": "홍길동", "email": "hong@example.com"}
```
- 새 리소스 생성
- 폼 데이터 제출
- 기타 리소스가 정의한 처리 수행

#### PUT
```http
PUT /users/123 HTTP/1.1
Host: api.example.com
Content-Type: application/json

{"name": "홍길동", "email": "newemail@example.com"}
```
- 대상 리소스의 전체 상태 를 교체
- 리소스가 없으면 생성

#### DELETE
```http
DELETE /users/123 HTTP/1.1
Host: api.example.com
```
- 대상 리소스 제거

## 3. 상태 코드

### 3.1 1xx - 정보성 응답 (Informational)

| 코드 | 이름 | 설명 |
|------|------|------|
| 100 | Continue | 요청 본문 전송 진행 |
| 101 | Switching Protocols | 프로토콜 업그레이드 승인 |

### 3.2 2xx - 성공 (Successful)

| 코드 | 이름 | 설명 |
|------|------|------|
| 200 | OK | 요청 성공 |
| 201 | Created | 새 리소스 생성됨 (POST/PUT) |
| 202 | Accepted | 요청이 처리를 위해 접수됨 |
| 204 | No Content | 성공했으나 응답 본문 없음 |
| 206 | Partial Content | 범위 요청 충족 |

### 3.3 3xx - 리다이렉션 (Redirection)

| 코드 | 이름 | 설명 |
|------|------|------|
| 300 | Multiple Choices | 여러 표현 옵션 존재 |
| 301 | Moved Permanently | 영구적 URI 변경 |
| 302 | Found | 임시 URI 리다이렉션 |
| 304 | Not Modified | 조건부 요청 - 변경 없음 |
| 307 | Temporary Redirect | 임시 리다이렉션 (메서드 유지) |
| 308 | Permanent Redirect | 영구 리다이렉션 (메서드 유지) |

### 3.4 4xx - 클라이언트 오류 (Client Error)

| 코드 | 이름 | 설명 |
|------|------|------|
| 400 | Bad Request | 잘못된 문법 |
| 401 | Unauthorized | 인증 필요 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 405 | Method Not Allowed | 허용되지 않는 메서드 |
| 409 | Conflict | 리소스 상태 충돌 |
| 413 | Content Too Large | 페이로드가 처리 한계 초과 |
| 429 | Too Many Requests | 요청 속도 제한 초과 |

### 3.5 5xx - 서버 오류 (Server Error)

| 코드 | 이름 | 설명 |
|------|------|------|
| 500 | Internal Server Error | 예상치 못한 서버 상태 |
| 501 | Not Implemented | 기능 미지원 |
| 502 | Bad Gateway | 게이트웨이가 잘못된 응답 수신 |
| 503 | Service Unavailable | 일시적 서버 사용 불가 |
| 504 | Gateway Timeout | 게이트웨이 시간 초과 |

## 4. 헤더 필드

### 4.1 표현 메타데이터 (Representation Metadata)

| 헤더 | 설명 | 예시 |
|------|------|------|
| Content-Type | 미디어 타입과 파라미터 | `application/json; charset=utf-8` |
| Content-Length | 페이로드 옥텟 수 | `1234` |
| Content-Encoding | 적용된 변환 | `gzip`, `deflate`, `br` |
| Content-Language | 콘텐츠 언어 태그 | `ko-KR`, `en-US` |
| Content-Location | 식별된 리소스 URI | `/documents/123` |

### 4.2 검증자 필드 (Validator Fields)

조건부 요청에 사용됩니다:

| 헤더 | 설명 | 예시 |
|------|------|------|
| ETag | 비교용 불투명 엔티티 태그 | `"33a64df5"` |
| Last-Modified | 리소스 수정 타임스탬프 | `Sat, 29 Oct 2022 19:43:31 GMT` |

### 4.3 요청 컨텍스트 필드

| 헤더 | 설명 | 예시 |
|------|------|------|
| User-Agent | 클라이언트 소프트웨어 식별 | `Mozilla/5.0 (...)` |
| Referer | 참조 리소스 URI | `https://example.com/page` |
| Host | 요청 대상 권한 | `api.example.com` |
| Accept | 선호 표현 형식 | `application/json` |

### 4.4 응답 컨텍스트 필드

| 헤더 | 설명 | 예시 |
|------|------|------|
| Server | 오리진 서버 소프트웨어 | `nginx/1.18.0` |
| Location | 리다이렉트 또는 생성된 리소스 URI | `/users/456` |
| Retry-After | 재시도 권장 시간 | `120` 또는 날짜 |

### 4.5 캐시 관련 필드

| 헤더 | 설명 |
|------|------|
| Vary | 표현 선택에 영향을 준 헤더 |
| Allow | 지원되는 메서드 열거 |

## 5. 콘텐츠 협상 (Content Negotiation)

### 5.1 사전 협상 (Proactive Negotiation)

클라이언트가 선호도를 미리 전송합니다:

```http
GET /document HTTP/1.1
Host: example.com
Accept: application/json, text/html;q=0.9, */*;q=0.1
Accept-Language: ko-KR, ko;q=0.9, en;q=0.8
Accept-Encoding: gzip, deflate, br
Accept-Charset: utf-8
```

품질 값 (q-parameter):
- 범위: 0.0 ~ 1.0
- 1.0이 가장 높은 선호도 (기본값)
- 0은 "허용하지 않음"

### 5.2 반응 협상 (Reactive Negotiation)

서버가 요청 특성에 따라 표현을 선택하거나, 300 (Multiple Choices) 응답으로 옵션을 제공합니다.

### 5.3 Vary 헤더

```http
Vary: Accept-Language, Accept-Encoding
```

어떤 요청 헤더가 표현 선택에 영향을 주었는지 문서화하여 캐시 효율성을 높입니다.

## 6. 조건부 요청 (Conditional Requests)

효율적인 리소스 동기화를 위한 메커니즘입니다.

### 6.1 사전 조건 필드

| 헤더 | 설명 | 용도 |
|------|------|------|
| If-Match | ETag 일치 요구 | 충돌 방지 업데이트 |
| If-None-Match | ETag 불일치 요구 | 캐시 검증 |
| If-Modified-Since | 타임스탬프 이후 수정 요구 | 캐시 검증 |
| If-Unmodified-Since | 타임스탬프 이후 미변경 요구 | 충돌 방지 |
| If-Range | 검증자 일치 시 부분 검색 | 범위 요청 |

### 6.2 캐시 검증 예시

```http
GET /resource HTTP/1.1
Host: example.com
If-None-Match: "abc123"
If-Modified-Since: Sat, 29 Oct 2022 19:43:31 GMT
```

응답 (변경 없음):
```http
HTTP/1.1 304 Not Modified
ETag: "abc123"
```

→ 본문 전송 없이 캐시된 콘텐츠 사용 가능

### 6.3 충돌 방지 업데이트

```http
PUT /document/123 HTTP/1.1
Host: example.com
If-Match: "version1"
Content-Type: application/json

{"content": "updated data"}
```

ETag가 일치하지 않으면 412 Precondition Failed 반환

## 7. 인증 (Authentication)

### 7.1 인증 체계 구조

Challenge와 Credential은 체계 이름과 선택적 파라미터로 구성됩니다:
- Basic: Base64 인코딩된 사용자명:비밀번호
- Bearer: 토큰 기반 인증
- Digest: 해시 기반 인증

### 7.2 Challenge-Response 흐름

```
1. 클라이언트 → 보호된 리소스 요청
   GET /protected HTTP/1.1

2. 서버 → 인증 Challenge
   HTTP/1.1 401 Unauthorized
   WWW-Authenticate: Bearer realm="api"

3. 클라이언트 → Credential 제공
   GET /protected HTTP/1.1
   Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

4. 서버 → 인증 성공
   HTTP/1.1 200 OK
```

### 7.3 인증 관련 헤더

| 헤더 | 방향 | 용도 |
|------|------|------|
| WWW-Authenticate | 서버→클라이언트 | 오리진 서버 Challenge |
| Authorization | 클라이언트→서버 | 오리진 서버 Credential |
| Proxy-Authenticate | 프록시→클라이언트 | 프록시 Challenge |
| Proxy-Authorization | 클라이언트→프록시 | 프록시 Credential |

### 7.4 보호 공간 (Protection Spaces)

Realm 은 동일한 자격 증명이 필요한 리소스를 그룹화합니다:

```http
WWW-Authenticate: Basic realm="Admin Area"
WWW-Authenticate: Basic realm="User Area"
```

### 7.5 주의사항

> URI 내 자격 증명 (userinfo)은 노출 위험으로 인해 사용 중단(deprecated) 됨

```
❌ https://user:password@example.com/api
✓ Authorization 헤더 사용
```

## 요약

RFC 9110은 HTTP의 핵심 의미론을 정의합니다:
- 메서드: 리소스에 대한 작업 정의 (GET, POST, PUT, DELETE 등)
- 상태 코드: 요청 결과 표현 (1xx-5xx)
- 헤더 필드: 메타데이터 전달
- 콘텐츠 협상: 클라이언트-서버 간 표현 형식 협상
- 조건부 요청: 효율적인 캐싱과 충돌 방지
- 인증: 리소스 접근 제어

이 문서는 HTTP/1.1, HTTP/2, HTTP/3 모든 버전에 공통으로 적용되는 통합된 HTTP 바이블 입니다.
