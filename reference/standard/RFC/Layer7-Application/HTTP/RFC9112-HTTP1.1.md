# RFC 9112 - HTTP/1.1

> 발행일: 2022년 6월
> 대체 문서: RFC 7230
> 상태: Standards Track

## 1. 개요

RFC 9112는 HTTP/1.1의 메시지 문법과 파싱 에 집중한 문서입니다. HTTP 의미론(RFC 9110)과 분리되어, HTTP/1.1 프로토콜의 전송 형식을 정의합니다.

## 2. 메시지 형식

### 2.1 기본 구조

HTTP/1.1 메시지는 다음 순서로 구성됩니다:

```
시작 라인 (start-line)
CRLF
헤더 필드들 (header fields)
CRLF
빈 줄 (CRLF)
[선택적 메시지 본문]
```

### 2.2 메시지 예시

요청 메시지:
```http
GET /index.html HTTP/1.1
Host: www.example.com
User-Agent: Mozilla/5.0
Accept: text/html
Accept-Language: ko-KR

```

응답 메시지:
```http
HTTP/1.1 200 OK
Date: Mon, 27 Jul 2023 12:28:53 GMT
Content-Type: text/html; charset=UTF-8
Content-Length: 138
Connection: keep-alive

<!DOCTYPE html>
<html>
...
</html>
```

## 3. 요청 라인과 상태 라인

### 3.1 요청 라인 (Request Line)

```
method SP request-target SP HTTP-version CRLF
```

| 구성 요소 | 설명 | 예시 |
|-----------|------|------|
| method | HTTP 메서드 토큰 | GET, POST, PUT |
| SP | 공백 (0x20) | ` ` |
| request-target | 요청 대상 | `/users/123` |
| HTTP-version | 프로토콜 버전 | `HTTP/1.1` |
| CRLF | 줄바꿈 | `\r\n` |

예시:
```
GET /api/users?id=123 HTTP/1.1
POST /submit HTTP/1.1
DELETE /resources/456 HTTP/1.1
```

### 3.2 요청 대상 형식 (Request Target Forms)

| 형식 | 용도 | 예시 |
|------|------|------|
| origin-form | 일반 요청 | `/path/to/resource?query=value` |
| absolute-form | 프록시 요청 | `http://example.com/path` |
| authority-form | CONNECT 메서드 | `example.com:443` |
| asterisk-form | OPTIONS 전체 | `*` |

### 3.3 상태 라인 (Status Line)

```
HTTP-version SP status-code SP [reason-phrase] CRLF
```

| 구성 요소 | 설명 | 예시 |
|-----------|------|------|
| HTTP-version | 프로토콜 버전 | `HTTP/1.1` |
| status-code | 3자리 상태 코드 | `200`, `404` |
| reason-phrase | 이유 문구 (선택) | `OK`, `Not Found` |

예시:
```
HTTP/1.1 200 OK
HTTP/1.1 404 Not Found
HTTP/1.1 500 Internal Server Error
```

> 참고: 이유 문구(reason-phrase)는 선택 사항이며, 클라이언트는 이를 무시해도 됩니다.

### 3.4 Host 헤더 필수

모든 HTTP/1.1 요청에서 Host 헤더는 필수입니다.

```http
GET /index.html HTTP/1.1
Host: www.example.com
```

Host 헤더가 없거나 여러 개이면 400 Bad Request 응답

## 4. 헤더 필드 문법

### 4.1 기본 구조

```
field-name ":" OWS field-value OWS
```

| 요소 | 설명 |
|------|------|
| field-name | 필드 이름 (대소문자 구분 없음) |
| `:` | 콜론 구분자 |
| OWS | 선택적 공백 (Optional Whitespace) |
| field-value | 필드 값 |

### 4.2 규칙

```
✓ 올바른 형식:
Content-Type: application/json
Content-Type:application/json
Content-Type:  application/json

✗ 잘못된 형식:
Content-Type : application/json  (필드명과 콜론 사이에 공백)
```

### 4.3 라인 폴딩 (Line Folding)

더 이상 생성해서는 안 됨 (obs-fold):

```http
# 구식 (사용 금지)
Subject: This is a
 very long subject line

# 현대적 방식
Subject: This is a very long subject line
```

수신 시 라인 폴딩은 공백으로 대체되어야 합니다.

### 4.4 헤더 필드 순서

- 동일한 필드명의 헤더는 쉼표로 구분된 값 목록과 동일하게 처리
- 순서가 의미를 가지는 경우 (예: Set-Cookie) 순서 유지 필요

```http
# 다음 두 표현은 동일
Cache-Control: no-cache
Cache-Control: no-store

Cache-Control: no-cache, no-store
```

## 5. 메시지 본문

### 5.1 본문 길이 결정

메시지 본문의 길이는 다음 우선순위로 결정됩니다:

```
1. Transfer-Encoding이 있는 경우
   → Transfer-Encoding 사용 (Content-Length 무시)

2. Content-Length가 있는 경우
   → Content-Length 값 사용

3. 요청이고 위 둘 다 없는 경우
   → 본문 길이 = 0

4. 응답이고 위 둘 다 없는 경우
   → 연결 종료까지 읽음
```

### 5.2 본문이 없는 응답

다음 응답에는 본문이 없습니다:

| 조건 | 설명 |
|------|------|
| HEAD 요청에 대한 응답 | 본문 제외 |
| 1xx 상태 코드 | 정보성 응답 |
| 204 No Content | 콘텐츠 없음 |
| 304 Not Modified | 수정 없음 |
| CONNECT 요청의 2xx 응답 | 터널 설정 |

### 5.3 예시

Content-Length 사용:
```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 27

{"status": "ok", "id": 123}
```

## 6. Transfer-Encoding

### 6.1 개요

Transfer-Encoding은 메시지 속성 이지 표현 특성이 아닙니다.

```http
HTTP/1.1 200 OK
Content-Type: text/plain
Transfer-Encoding: chunked

[청크 형식의 데이터]
```

### 6.2 청크 전송 코딩 (Chunked Transfer Coding)

필수 지원: 모든 HTTP/1.1 구현체는 청크 전송을 파싱할 수 있어야 합니다.

#### 청크 형식

```
chunk-size (16진수) CRLF
chunk-data CRLF
...
0 CRLF
[trailer-section]
CRLF
```

#### 예시

```http
HTTP/1.1 200 OK
Content-Type: text/plain
Transfer-Encoding: chunked

7\r\n
Mozilla\r\n
11\r\n
Developer Network\r\n
0\r\n
\r\n
```

해석:
```
7 (16진수) = 7바이트 → "Mozilla"
11 (16진수) = 17바이트 → "Developer Network"
0 = 종료 청크
```

### 6.3 청크 확장 (Chunk Extensions)

```
chunk-size [; chunk-ext] CRLF
```

```http
a; name=value\r\n
data......\r\n
```

### 6.4 트레일러 섹션 (Trailer Section)

청크 전송이 완료된 후 추가 헤더 필드를 전송할 수 있습니다:

```http
Transfer-Encoding: chunked
Trailer: Checksum

7\r\n
Mozilla\r\n
0\r\n
Checksum: abc123\r\n
\r\n
```

### 6.5 제한사항

- 동일 본문에 청크 인코딩 2회 이상 적용 불가
- 요청에 Transfer-Encoding이 있으면 Content-Length 무시

## 7. 연결 관리

### 7.1 지속 연결 (Persistent Connections)

HTTP/1.1은 기본적으로 지속 연결 을 사용합니다.

```
HTTP/1.0: 기본 비지속, Connection: keep-alive로 유지
HTTP/1.1: 기본 지속, Connection: close로 종료
```

#### 연결 유지

```http
GET /page1 HTTP/1.1
Host: example.com
# 연결 유지됨 (기본)

GET /page2 HTTP/1.1
Host: example.com
# 같은 연결 재사용
```

#### 연결 종료

```http
GET /final HTTP/1.1
Host: example.com
Connection: close
# 응답 후 연결 종료
```

### 7.2 파이프라이닝 (Pipelining)

여러 요청을 응답을 기다리지 않고 연속 전송:

```
Client                           Server
  |---- GET /a ------------------->|
  |---- GET /b ------------------->|
  |---- GET /c ------------------->|
  |<------------------ 200 /a -----|
  |<------------------ 200 /b -----|
  |<------------------ 200 /c -----|
```

#### 파이프라이닝 규칙

| 규칙 | 설명 |
|------|------|
| 순서 보장 | 요청 순서와 응답 순서가 일치해야 함 |
| 안전한 메서드만 | GET, HEAD 등 멱등/안전 메서드만 파이프라인 가능 |
| 재시도 주의 | 연결 끊김 시 비멱등 요청은 자동 재시도 금지 |

### 7.3 연결 종료 처리

```
정상 종료:
1. Connection: close 전송
2. 마지막 응답 완료
3. TCP 연결 종료

TLS 연결:
- closure alert 교환 권장
- 갑작스런 종료는 절단 공격으로 오해될 수 있음
```

### 7.4 연결 타임아웃

서버는 유휴 연결에 타임아웃을 적용할 수 있습니다:

```
유휴 시간 초과 → 연결 종료
클라이언트는 재연결 필요
```

## 8. 메시지 파싱 견고성

### 8.1 관대한 수신

```
# 빈 줄로 시작하는 요청 무시
CRLF
CRLF
GET /index.html HTTP/1.1
Host: example.com
```

### 8.2 잘못된 요청 처리

| 오류 | 응답 |
|------|------|
| 유효하지 않은 요청 라인 | 400 Bad Request |
| 지원하지 않는 HTTP 버전 | 505 HTTP Version Not Supported |
| 너무 긴 URI | 414 URI Too Long |
| 너무 긴 헤더 | 431 Request Header Fields Too Large |

## 9. HTTP/1.1 vs HTTP/1.0 비교

| 기능 | HTTP/1.0 | HTTP/1.1 |
|------|----------|----------|
| 지속 연결 | 선택적 (keep-alive) | 기본 |
| Host 헤더 | 선택적 | 필수 |
| 청크 전송 | 미지원 | 필수 지원 |
| 파이프라이닝 | 미지원 | 지원 |
| 캐시 제어 | 제한적 | 향상됨 |

## 요약

RFC 9112는 HTTP/1.1의 메시지 형식을 정의합니다:

- 메시지 구조: 시작 라인 + 헤더 + 빈 줄 + 본문
- 요청 라인: `메서드 SP 요청대상 SP HTTP버전`
- 상태 라인: `HTTP버전 SP 상태코드 SP 이유문구`
- 헤더 문법: `필드명: 값` (필드명과 콜론 사이 공백 금지)
- 본문 길이: Transfer-Encoding > Content-Length 우선순위
- 청크 전송: 길이 미리 알 수 없는 데이터 스트리밍
- 지속 연결: HTTP/1.1 기본, 파이프라이닝 지원
