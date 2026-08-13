# HTTP 메서드와 상태 코드

## RFC 5789 - PATCH Method for HTTP

> 발행일: 2010년 3월
> 상태: Standards Track

### Abstract

- 다수 애플리케이션에 리소스 부분 수정 기능 필요 → 기존 HTTP PUT은 문서 완전 교체만 허용
- 이 문서: 기존 HTTP 리소스 수정을 위한 신규 메서드 PATCH 추가

### 1. 소개

- 이 명세: 기존 리소스에 부분 수정을 적용하는 새 HTTP/1.1([RFC2616]) 메서드 PATCH 정의
- 신규 메서드 필요 이유
  - 기존 HTTP 인프라와의 상호운용성 개선
  - 리소스 부분 수정에 다른 메서드를 오용하는 데서 오는 혼란 방지
  - 기존 PUT 메서드는 문서 완전 교체만 허용
  - PATCH라는 이름은 과거 HTTP에서 사용된 적 있으나 완전하게 정의되지 않았음
- 부분 PUT이 아닌 신규 메커니즘을 정의한 이유 → 부분 PUT의 의미를 재정의하면 기존 파이프라인된 PUT과의 상호운용이 어려워짐
- 키워드(MUST·MUST NOT·REQUIRED·SHALL·SHALL NOT·SHOULD·SHOULD NOT·RECOMMENDED·MAY·OPTIONAL)는 [RFC2119] 기준 해석
- ABNF 표기는 [RFC2616] 섹션 2.1 기준

### 2. PATCH 메서드

- PATCH: 요청 엔티티에 기술된 변경 사항 집합을 Request-URI로 식별되는 리소스에 적용 요청
  - 변경 사항 집합은 미디어 타입으로 식별되는 "패치 문서" 형식으로 표현
  - Request-URI가 기존 리소스를 가리키지 않는 경우 → 패치 문서 유형·권한 등에 따라 서버가 새 리소스 생성 가능(MAY)
- PUT과 PATCH의 차이
  - PUT: 동봉 엔티티는 원본 서버에 저장된 리소스의 수정된 버전 전체, 클라이언트는 저장된 버전의 교체 요청
  - PATCH: 동봉 엔티티는 현재 리소스를 새 버전으로 만들기 위해 어떻게 수정할지 설명하는 명령어 집합
  - PATCH는 Request-URI로 식별되는 리소스에 영향 + 다른 리소스에도 부수적 영향 가능(MAY)
- PATCH는 [RFC2616] 섹션 9.1 기준 안전하지도 멱등하지도 않음
  - 멱등한 PATCH 요청 발행도 가능 → 동시간대 동일 리소스에 대한 두 PATCH 요청 간 충돌로 인한 악영향 방지에 도움
- 여러 PATCH 요청의 충돌은 PUT 충돌보다 위험할 수 있음 → 일부 패치 형식이 알려진 기준점 위에서 동작해야 하기 때문(그렇지 않으면 리소스 손상 가능)
  - 이런 패치 형식을 사용하는 클라이언트는, 마지막 접근 이후 리소스가 갱신되면 요청이 실패하도록 조건부 요청 사용 권장(SHOULD)
    - 예: 패치 요청의 If-Match 헤더에 ETag 사용
- PATCH로 리소스 생성·수정에 기본적인 제한 없음
  - 서버는 전체 변경 사항 집합을 원자적으로 적용 필수(MUST)
  - 작업 도중(예: GET 응답으로) 일부만 적용된 표현 제공 금지(MUST NOT)
- PATCH의 "성공" 응답 의미: 전체 패치가 적용되었음만 의미, 이전·이후에 적용되는 다른(패치가 아닌) 작업의 결과는 포함하지 않음
  - 패치 적용 후 서버 상태 확인을 위한 GET은, 중간에 다른 사용자의 요청이 있었을 수 있어 보장되지 않음
- 서버는 수신한 패치 문서가 Request-URI로 식별된 리소스 유형에 적합한지 확인 필수(MUST)
  - 관련성 없는 패치 문서를 잘못 적용하면 리소스 손상·보안 취약점 유발 가능 → 서버가 방지 필요
- PATCH의 대체 형식 지원 여부는 전적으로 서버가 결정
  - 서버가 단일 유형의 패치 문서만 수락해도 완전히 수용 가능
  - PATCH vs PUT 선택은 궁극적으로 클라이언트 몫이나, 리소스가 어떤 패치 문서 형식을 지원할지는 서버가 결정
- 이 문서에서 정의되지 않은 부분의 PATCH 처리는 PUT과 유사해야 함
  - 성공·오류 응답 의미론, 인가, 요청 헤더 상호작용 포함
  - 특정 패치 문서 형식이 명시적으로 허용하지 않는 한, PATCH 요청의 엔티티 헤더는 패치 문서에만 적용 권장(SHOULD) → 생성·수정되는 리소스에는 적용 금지
- 단일 기본 패치 문서 형식은 존재하지 않음
  - application/json-patch+json [RFC6902] 같은 신규 패치 문서 형식 등록 권장(SHOULD)
- PATCH 응답 캐시 조건: 명시적 신선도 정보(Expires 헤더, Cache-Control: max-age)를 포함하면서, Content-Location 값이 Request-URI와 일치하는 경우에만 캐시 가능

#### 2.1 간단한 PATCH 예시

```http
PATCH /file.txt HTTP/1.1
Host: www.example.com
Content-Type: application/example
If-Match: "e0023aa4e"
Content-Length: 100

[변경 사항 설명]
```

성공적인 PATCH에 대한 응답:

```http
HTTP/1.1 204 No Content
Content-Location: /file.txt
ETag: "e0023aa4f"
```

- [RFC2616] 섹션 10.2.1에 정의된 200 응답 코드도 사용 가능
- 100바이트의 패치 문서가 파일 하나에 적용된 경우
  - 가상의 "application/example" 패치 형식은 패치가 완전히 적용되거나 전혀 적용되지 않도록 의미가 정의 → 서버의 원자성 요구사항 강제
- 응답의 ETag로 리소스 현재 내용 식별 가능 → 리소스 상태 추적 및 향후 PATCH에 활용
  - ETag 사용 시, PATCH 응답에서 수신한 ETag 값이 리소스 현재 상태에 대한 유일한 신뢰 가능 정보

#### 2.2 오류 처리

PATCH에서 발생 가능한 알려진 조건들:

- 잘못된 형식의 패치 문서: 서버가 패치 문서 형식이 부적절하다고 판단하면 400 (Bad Request) 응답 권장(SHOULD)
  - "적절한 형식"의 정의는 패치 문서의 미디어 유형에 따라 다름
- 지원되지 않는 패치 문서: 클라이언트가 서버 미지원 형식의 패치 문서를 보낸 경우 415 (Unsupported Media Type) 응답 가능
  - 이 응답에는 [RFC2616] 섹션 3.7에 따라 Accept-Patch 응답 헤더를 포함해 지원 가능한 패치 문서 미디어 유형 안내 권장(SHOULD)
- 처리할 수 없는 요청: 패치 문서 형식은 유효하나 서버가 요청을 처리할 수 없는 경우 422 (Unprocessable Entity) 사용 가능([RFC4918] 섹션 11.2)
  - 예: 수정이 리소스를 유효하지 않은 상태로 만드는 경우
- 리소스를 찾을 수 없음: 존재하지 않는 리소스에 패치 문서를 적용할 수 없는 경우(예: 새 리소스 생성을 지원하지 않는 패치 문서 유형) → 404 (Not Found) 응답 권장(SHOULD)
- 충돌 상태: 리소스의 현재 상태로 인해 요청을 적용할 수 없는 특정 상황(예: 삭제 대상 부분이 이미 없는 등, 패치가 가정한 구조가 존재하지 않음) → 409 (Conflict) 응답 권장(SHOULD)
- 동시 수정 충돌: 동일 리소스에 대해 동시에 수신한 두 PATCH 요청(또는 PATCH와 PUT/POST)을 큐에 넣을 수 없는 경우 409 (Conflict) 응답 권장(SHOULD)
- 전제조건 실패: 조건부 요청의 전제조건이 실패한 경우 412 (Precondition Failed) 응답 권장(SHOULD)

- 위 오류 목록은 완전하지 않음 → 서버는 적절한 경우 다른 상태 코드 사용 필수(MUST)
  - HTTP 오류 응답 본문은 클라이언트가 실패 요청에 후속 조치를 취할 수 있는 충분한 정보 포함 권장(SHOULD)
- 클라이언트는 실패한 요청 처리를 위해 GET으로 리소스 현재 상태 조회 가능
  - 예: 409 Conflict 응답 시, 현재 사본을 가져와 오류가 포함된 차이점 보고서로 새 패치를 구성하거나 현재 사본에 대해 변환해 패치를 재구성

### 3. OPTIONS에서 지원 알림

- 서버는 특정 리소스에 대한 OPTIONS 응답의 Allow 헤더에 PATCH를 포함해 PATCH 지원을 알릴 수 있음
  - Accept-Patch 헤더 존재 시 PATCH 허용을 암시해 Allow 헤더를 사실상 불필요하게 만들지만, 하위 호환성을 위해 PATCH 지원 시 Allow 헤더에도 포함 권장(SHOULD)

#### 3.1 Accept-Patch 헤더

- 이 명세: 서버가 수락하는 패치 문서 미디어 유형을 지정하는 신규 응답 헤더 Accept-Patch 도입
  - PATCH를 지원하는 모든 리소스의 OPTIONS 응답에 반드시 포함(MUST)
  - OPTIONS 응답에 이 헤더가 있으면 해당 리소스에 PATCH 허용을 암시, 헤더가 없다고 PATCH 불허를 의미하지는 않음(Accept-Patch 지원이 필수가 아닐 수 있음)
- Accept-Patch 구문(ABNF):

```
Accept-Patch = "Accept-Patch" ":" 1#media-type
```

  - media-type 구문은 [RFC2616] 섹션 3.7 정의 따름

#### 3.2 OPTIONS 요청 및 응답 예시

```http
OPTIONS /example/buddies.xml HTTP/1.1
Host: www.example.com
```

PATCH가 지원됨을 나타내는 예시 응답:

```http
HTTP/1.1 200 OK
Allow: GET, PUT, POST, OPTIONS, HEAD, DELETE, PATCH
Accept-Patch: application/example, text/example
```

- 실제 패치 문서 형식은 RFC가 정의하지 않음 → 예시에는 가상 패치 문서 형식 2개 사용

### 4. IANA 고려사항

#### 4.1 Accept-Patch 응답 헤더 등록

- Accept-Patch 응답 헤더는 [RFC3864]에 정의된 영구 등록소에 추가
  - 헤더 필드 이름: Accept-Patch
  - 적용 프로토콜: HTTP
  - 저자/변경 관리자: IETF
  - 명세 문서: 이 명세

### 5. 보안 고려사항

- PATCH 요청에 대한 보안 고려사항은 PUT([RFC2616] 섹션 9.6)과 거의 동일
  - 접근 제어·인증을 통한 요청 인가, 우발적 덮어쓰기 방지, 전송 오류에 대한 데이터 무결성 보호 포함
- PUT을 사용한 완전 교체 문서 대비, 패치된 문서가 손상될 수 있다는 우려 가능
  - 완화 방법: ETag·If-Match 요청 헤더를 사용한 조건부 요청
  - PATCH 실패 시, 클라이언트는 리소스가 올바른 상태인지 확인하기 위해 GET 요청 발행 가능
  - PATCH 응답 수신 전 전송 연결이 실패한 경우, 캐시를 우회하는 GET으로 애플리케이션 상태 확인 가능
- 악성 콘텐츠를 검사하는 중개자(예: 바이러스 검사 HTTP 중개자)는 PATCH 작업에 특별한 어려움을 겪음
  - 소스 문서·패치 문서가 개별적으로는 악성이 아니어도, 서버가 패치를 적용한 결과 악성 리소스가 생성될 수 있음
  - 바이트 범위 다운로드·패치 문서·압축 파일 업로드 등 유사 메커니즘의 기존 위험과 같은 선상
  - 이런 콘텐츠 포함을 탐지하려는 중개자는 개별 데이터 조각이 아닌 조립된 콘텐츠를 확인하는 방식 외에는 대응 수단 없음
- 개별 패치 문서는 수정될 리소스의 유형에 따라 고유한 보안 고려사항을 가질 수 있음
  - 서버는 악의적인 클라이언트가 PATCH 작업으로 과도한 서버 리소스(CPU·메모리·디스크 I/O)를 소비하지 못하도록 적절한 예방 조치 필수(MUST)

### 6. 참조

- 규범적
  - [RFC2119] Bradner, S., "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, RFC 2119, March 1997.
  - [RFC2616] Fielding et al., "Hypertext Transfer Protocol -- HTTP/1.1", RFC 2616, June 1999.
  - [RFC3864] Klyne, Nottingham, Mogul, "Registration Procedures for Message Header Fields", BCP 90, RFC 3864, September 2004.
- 정보적
  - [RFC4918] Dusseault, L., "HTTP Extensions for Web Distributed Authoring and Versioning (WebDAV)", RFC 4918, June 2007.

---

## RFC 6585 - Additional HTTP Status Codes

> 발행일: 2012년 4월
> 상태: Standards Track (Updates: RFC 2616)

### Abstract

- 다양한 일반적 상황에 대한 추가 HTTP 상태 코드 명시

### 1. 소개

- 이 문서: 다양한 일반적 상황에 대한 추가 HTTP [RFC2616] 상태 코드 명시 → 상호운용성 개선, 부정확한 상태 코드 사용에서 오는 혼란 방지
- 이 상태 코드들은 선택적 → 서버가 지원해야 할 요구사항 없음
  - 클라이언트가 인식하지 못하는 상태 코드를 해당 클래스의 일반 상태 코드로 처리하도록 되어 있음(예: 499를 400으로 처리) → 안전하게 배포 가능

### 2. 요구사항

- 키워드(MUST·MUST NOT·REQUIRED·SHALL·SHALL NOT·SHOULD·SHOULD NOT·RECOMMENDED·MAY·OPTIONAL)는 [RFC2119] 기준 해석

### 3. 428 Precondition Required

- 428: origin 서버가 요청에 조건부 처리를 요구함을 나타냄
- 용도: 클라이언트가 리소스 상태를 GET하고 수정 후 PUT으로 다시 보내는 사이, 제3자가 서버 상태를 수정해 충돌이 발생하는 "lost update" 문제 방지
  - 조건부 요청을 강제해 클라이언트가 리소스의 현재 사본을 기반으로 작업하고 있음을 보장
- 428 응답: 요청을 성공적으로 재제출하는 방법 설명 권장(SHOULD)

예시:

```http
HTTP/1.1 428 Precondition Required
Content-Type: text/html

<html>
   <head>
      <title>Precondition Required</title>
   </head>
   <body>
      <h1>Precondition Required</h1>
      <p>This request is required to be conditional;
      try using "If-Match".</p>
   </body>
</html>
```

- 428 상태 코드를 가진 응답은 캐시 금지(MUST NOT)

### 4. 429 Too Many Requests

- 429: 사용자가 주어진 시간 동안 너무 많은 요청을 보냈음을 나타냄("rate limiting")
- 응답 표현: 조건을 설명하는 세부 사항 포함 권장(SHOULD), 새 요청 전 대기 시간을 나타내는 Retry-After 헤더 포함 가능(MAY)

예시:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: text/html
Retry-After: 3600

<html>
   <head>
      <title>Too Many Requests</title>
   </head>
   <body>
      <h1>Too Many Requests</h1>
      <p>I only allow 50 requests per hour to this Web site per
         logged in user.  Try again soon.</p>
   </body>
</html>
```

- 이 명세는 사용자 식별 방법·요청 계산 방법을 정의하지 않음
  - 예: origin 서버가 리소스별·서버 전체·서버 집합 단위로 요청을 제한하는 방식, 인증 자격 증명 또는 상태 기반 쿠키로 사용자를 식별하는 방식 모두 이 명세의 범위 밖
- 429 상태 코드를 가진 응답은 캐시 금지(MUST NOT)

### 5. 431 Request Header Fields Too Large

- 431: 요청의 헤더 필드가 너무 커서 서버가 요청 처리를 거부함을 나타냄
  - 요청 헤더 필드 크기를 줄인 후 재제출 가능(MAY)
- 전체 요청 헤더 필드 집합이 너무 클 때·단일 헤더 필드에 문제가 있을 때 모두 사용 가능
  - 후자의 경우, 응답 표현은 어떤 헤더 필드가 너무 컸는지 명시 권장(SHOULD)

예시:

```http
HTTP/1.1 431 Request Header Fields Too Large
Content-Type: text/html

<html>
   <head>
      <title>Request Header Fields Too Large</title>
   </head>
   <body>
      <h1>Request Header Fields Too Large</h1>
      <p>The "Example" header was too large.</p>
   </body>
</html>
```

- 431 상태 코드를 가진 응답은 캐시 금지(MUST NOT)

### 6. 511 Network Authentication Required

- 511: 클라이언트가 네트워크 접근 권한을 얻기 위해 인증해야 함을 나타냄
- 응답 표현: 사용자가 자격 증명을 제출할 수 있는 리소스 링크 포함 권장(SHOULD)(예: HTML 양식)
- 511 응답에는 challenge나 로그인 인터페이스 자체를 포함 금지(SHOULD NOT) → 브라우저가 로그인 인터페이스를 원래 요청한 URL과 연관된 것으로 표시해 혼란 유발 가능
- 511 상태는 origin 서버가 생성 금지(SHOULD NOT) → 네트워크 접근을 제어하는 가로채기 프록시(intercepting proxy)가 사용하는 용도
- 511 상태 코드를 가진 응답은 캐시 금지(MUST NOT)

#### 6.1 511 상태 코드와 Captive Portal

- 511 설계 목적: 요청이 전달된 서버가 아닌 중간 네트워크 인프라의 응답을 기대하는 소프트웨어(특히 비브라우저 에이전트)에 "captive portal"이 야기하는 문제 완화
  - captive portal 배포를 장려하기 위한 것이 아니라, 이로 인한 피해를 제한하기 위한 것
- 인증·이용약관 동의 등 요구 후 접근 권한을 부여하는 네트워크 운영자는, 이를 완료하지 않은 클라이언트("unknown client")를 MAC 주소로 식별
  - unknown client의 모든 트래픽은 차단, TCP 포트 80의 트래픽만 예외로 "로그인"을 전담하는 HTTP 서버("login server")로 전송, login server 자체로의 트래픽도 허용

예: 사용자 에이전트가 네트워크에 연결해 TCP 포트 80에서 다음 요청 전송:

```http
GET /index.htm HTTP/1.1
Host: www.example.com
```

login server의 511 응답:

```http
HTTP/1.1 511 Network Authentication Required
Content-Type: text/html

<html>
   <head>
      <title>Network Authentication Required</title>
      <meta http-equiv="refresh"
            content="0; url=https://login.example.net/">
   </head>
   <body>
      <p>You need to <a href="https://login.example.net/">
      authenticate with the local network</a> in order to gain
      access.</p>
   </body>
</html>
```

- 511 상태 코드는 비브라우저 클라이언트가 이 응답을 origin 서버의 응답으로 해석하지 않도록 보장, META HTML 요소는 사용자 에이전트를 login server로 리다이렉트

### 7. 보안 고려사항

#### 7.1 428 Precondition Required

- 428은 선택적 → 클라이언트는 "lost update" 충돌 방지를 이 코드 사용에만 의존 불가

#### 7.2 429 Too Many Requests

- 서버가 공격을 받거나 단일 당사자로부터 매우 많은 요청을 받는 상황에서, 각 요청에 429로 응답하는 것도 리소스를 소비
  - 서버는 429 사용이 필수 아님 → 리소스 사용 제한 시 연결 종료 등 다른 조치가 더 적절할 수 있음

#### 7.3 431 Request Header Fields Too Large

- 서버는 431 사용이 필수 아님 → 공격을 받는 상황에서 연결 종료 등 다른 조치가 더 적절할 수 있음

#### 7.4 511 Network Authentication Required

- 511 응답은 일반적 사용에서 요청 URL에 표시된 origin 서버로부터 오지 않음 → 여러 보안 문제 야기
  - 예: 공격 중개자가 원래 도메인의 네임스페이스에 쿠키를 삽입, 사용자 에이전트가 보낸 쿠키·HTTP 인증 자격 증명 관찰 등
- 다만 이 위험은 511에 고유하지 않음 → 511을 사용하지 않는 captive portal도 동일한 문제 유발
- SSL/TLS 연결(일반적으로 포트 443)에서 이 상태 코드를 사용하는 captive portal은 클라이언트에서 인증서 오류를 유발

### 8. IANA 고려사항

HTTP 상태 코드 레지스트리 업데이트 항목:

- 428 · Precondition Required · [RFC6585]
- 429 · Too Many Requests · [RFC6585]
- 431 · Request Header Fields Too Large · [RFC6585]
- 511 · Network Authentication Required · [RFC6585]

### 9. 참고 문헌

- 규범적: [RFC2119], [RFC2616]
- 정보적: [CORS], [Favicon], [OAuth2.0], [P3P], [RFC4791](CalDAV), [RFC4918](WebDAV), [WIDGETS], [WebFinger]

### 부록 B. Captive Portal에 의해 제기되는 문제점

- 클라이언트가 portal의 응답과 원래 통신하려던 HTTP 서버의 응답을 구별할 수 없어 여러 문제 발생 → 511 상태 코드는 이 문제 중 일부 완화 목적
- 예 1: 브라우저가 사이트 식별에 사용하는 "favicon.ico" [Favicon]
  - 사용자 미인증 등으로 favicon이 의도된 사이트가 아닌 captive portal에서 가져와지면, 대부분의 구현이 favicon을 적극적으로 캐시하므로 portal 세션 이후에도 브라우저 캐시에 "고착" → portal의 favicon이 정당한 사이트를 "접수"한 것처럼 보임
- 예 2: Platform for Privacy Preferences [P3P] 지원 시
  - 구현 방식에 따라, 브라우저가 p3p.xml에 대한 portal의 응답을 서버 응답으로 해석 → portal이 광고하는 개인정보 보호 정책(또는 그 부재)이 의도된 사이트에 적용되는 것으로 해석될 수 있음
  - WebFinger [WebFinger]·Cross-Origin Resource Sharing [CORS]·Open Authorization [OAuth2.0] 등 다른 웹 기반 프로토콜도 이 문제에 취약 가능
- 비브라우저 애플리케이션도 영향
  - Web Distributed Authoring and Versioning (WebDAV) [RFC4918]과 Calendaring Extensions to WebDAV (CalDAV) [RFC4791] 등은 모두 HTTP 기반(원격 저작·일정 관리 용도) → captive portal 뒤에서 사용 시 가짜 오류 표시, 극단적인 경우 콘텐츠 손상 가능
  - 위젯 [WIDGETS], 소프트웨어 업데이트, Twitter 클라이언트·iTunes Music Store 같은 전문화된 소프트웨어도 영향 가능
- HTTP 리다이렉션으로 트래픽을 portal로 보내면 이 문제를 해결한다고 믿는 경우가 있으나, 이러한 용도의 대부분이 리다이렉트를 "따르기" 때문에 좋은 해결책이 아님

---

## RFC 9457 - Problem Details for HTTP APIs (RFC 7807 대체)

> 발행일: 2023년 7월
> 상태: Standards Track (Obsoletes: RFC 7807)

### Abstract

- HTTP API용 신규 오류 응답 형식을 정의할 필요 없이, HTTP 응답 콘텐츠로 기계 판독 가능한 오류 상세 정보를 전달하는 "problem detail" 정의
- 이 문서는 RFC 7807 대체

### 1. 소개

- RFC 9110 섹션 15에 정의된 HTTP 상태 코드만으로는 오류에 대한 충분한 정보를 전달하기 어려운 경우가 많음
  - 웹 브라우저 사용자는 HTML 응답 콘텐츠를 이해할 수 있는 경우가 많으나, HTTP API의 비인간 소비자는 그렇게 하기 어려움
- 이 명세: 발생한 문제의 구체적 내용을 설명하는 간단한 JSON·XML 문서 형식 "problem details" 정의
- 예: 클라이언트 계정에 충분한 크레딧이 없다는 응답
  - API 설계자는 403 Forbidden 상태 코드로 일반 HTTP 소프트웨어(클라이언트 라이브러리·캐시·프록시 등)에 응답의 일반적 의미 전달
  - API 특정 problem details(거부 이유·계정 잔액 등)는 응답 콘텐츠에 포함 → 클라이언트가 적절히 대응(예: 계정에 추가 크레딧 이체 트리거)
- 이 명세는 특정 "problem type"(예: "크레딧 부족")을 URI로 식별
  - HTTP API는 자체 제어 URI로 고유 문제를 식별하거나, 상호운용성·공통 의미 활용을 위해 기존 URI 재사용 가능(섹션 4.2 참조)
- Problem details는 문제의 특정 발생을 식별하는 URI 등 기타 정보 포함 가능 → 지원·포렌식 목적에 유용
- Problem details의 데이터 모델은 JSON 객체
  - JSON 문서로 직렬화 시 미디어 타입 "application/problem+json" 사용
  - 부록 B는 동등한 XML 형식 정의, 미디어 타입 "application/problem+xml" 사용
- HTTP 응답으로 전달 시 사전 협상(proactive negotiation)으로 콘텐츠 협상 가능(RFC 9110 섹션 12.1)
  - 사람이 읽는 문자열(title·description 등)의 언어는 Accept-Language 요청 헤더(RFC 9110 섹션 12.5.4)로 협상 가능하나, 협상 결과가 여전히 비선호 기본 표현일 수 있음
- Problem details는 모든 HTTP 상태 코드와 함께 사용 가능하나 4xx·5xx 응답 의미에 가장 자연스럽게 부합
  - 문제 상세 정보를 전달하는 유일한 방법은 아님 → 응답이 여전히 리소스의 표현인 경우, 해당 애플리케이션 형식으로 상세 정보를 설명하는 것이 더 바람직한 경우 많음
  - 정의된 HTTP 상태 코드가 추가 상세 정보 없이 많은 상황을 다루는 경우도 있음
- 이 명세의 목적: 오류 형식이 필요한 애플리케이션 위한 공통 오류 형식 정의 → 자체 정의 필요성 및 기존 HTTP 상태 코드 의미를 재정의하려는 유혹 회피
  - 애플리케이션이 이를 오류 전달에 사용하지 않기로 해도, 이 설계 검토가 기존 형식으로 오류를 전달할 때의 설계 결정에 도움될 수 있음
- RFC 7807 대비 변경사항 목록: 부록 D 참조

### 2. 요구사항 언어

- 키워드(MUST·MUST NOT·REQUIRED·SHALL·SHALL NOT·SHOULD·SHOULD NOT·RECOMMENDED·NOT RECOMMENDED·MAY·OPTIONAL, 모두 대문자로 표기될 때)는 BCP 14 [RFC2119][RFC8174] 기준 해석

### 3. Problem Details JSON 객체

- Problem details의 표준 모델은 JSON 객체
  - JSON 문서로 직렬화 시 "application/problem+json" 미디어 타입으로 식별

예:

```http
POST /purchase HTTP/1.1
Host: store.example.com
Content-Type: application/json
Accept: application/json, application/problem+json

{
  "item": 123456,
  "quantity": 2
}
```

```http
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json
Content-Language: en

{
 "type": "https://example.com/probs/out-of-credit",
 "title": "You do not have enough credit.",
 "detail": "Your current balance is 30, but that costs 50.",
 "instance": "/account/12345/msgs/abc",
 "balance": 30,
 "accounts": ["/account/12345",
              "/account/67890"]
}
```

- 크레딧 부족 문제(type으로 식별)는 "title"에서 403의 이유를 나타내고, "instance"로 특정 문제 발생을 식별, "detail"에 발생별 상세 정보를 제공, 두 확장을 추가
  - "balance": 계정 잔액 전달
  - "accounts": 계정을 충전할 수 있는 링크 나열
- 이를 수용하도록 설계된 경우, 문제별 확장은 동일 problem type의 둘 이상의 인스턴스도 전달 가능

예:

```http
POST /details HTTP/1.1
Host: account.example.com
Accept: application/json

{
  "age": 42.3,
  "profile": {
    "color": "yellow"
  }
}
```

```http
HTTP/1.1 422 Unprocessable Content
Content-Type: application/problem+json
Content-Language: en

{
 "type": "https://example.net/validation-error",
 "title": "Your request is not valid.",
 "errors": [
             {
               "detail": "must be a positive integer",
               "pointer": "#/age"
             },
             {
               "detail": "must be 'green', 'red' or 'blue'",
               "pointer": "#/profile/color"
             }
          ]
}
```

- 이 가상 problem type은 "errors" 확장 정의 → 각 유효성 검사 오류의 상세 정보를 설명하는 배열
  - 각 멤버는 문제를 설명하는 "detail"과, JSON Pointer로 요청 콘텐츠 내 문제 위치를 가리키는 "pointer"를 포함하는 객체
- API가 동일 type을 공유하지 않는 여러 문제를 만나는 경우, 가장 관련성이 높거나 긴급한 문제를 응답에 표현 권장(RECOMMENDED)
  - 여러 이질적 type을 전달하는 범용 "배치" problem type 구성도 가능하나, HTTP 의미론에 잘 매핑되지 않음
- API가 클라이언트가 Accept에 나열하지 않은 "application/problem+json" 타입으로 응답하는 것도 HTTP에서 허용(RFC 9110 섹션 12.5.1 참조)

#### 3.1 Problem Details 객체의 멤버

- Problem detail 객체는 아래 멤버를 가질 수 있음
  - 멤버의 값 타입이 지정된 타입과 일치하지 않으면 해당 멤버는 무시 필수(MUST) → 존재하지 않는 것처럼 처리 지속

##### 3.1.1 "type"

- "type": problem type을 식별하는 URI 참조를 포함하는 JSON 문자열
  - 소비자는 "type" URI(필요 시 해석 후)를 problem type의 주요 식별자로 사용 필수(MUST)
  - 멤버가 존재하지 않을 때 값은 "about:blank"으로 간주
- type URI가 로케이터(http·https 스키마 등)인 경우, 역참조하면 해당 problem type에 대한 사람이 읽을 수 있는 문서를 제공 권장(SHOULD)(예: HTML 사용)
  - 소비자는 개발자에게 정보를 제공할 때(디버깅 도구 사용 중 등) 제외하고는 type URI를 자동 역참조 금지(SHOULD NOT)
- "type"에 상대 URI가 포함된 경우, RFC 3986 섹션 5에 따라 문서의 기본 URI를 기준으로 해석 → 상대 URI는 혼란을 야기할 수 있고, 모든 구현에서 올바르게 처리되지 않을 수 있음
  - 예: 두 리소스 "https://api.example.org/foo/bar/123"과 "https://api.example.org/widget/456"이 모두 상대 URI 참조 "example-problem"과 동일한 "type"으로 응답하면, 해석 시 서로 다른 리소스("https://api.example.org/foo/bar/example-problem"과 "https://api.example.org/widget/example-problem")를 식별
  - 결과: 가능하면 "type"에 절대 URI 사용 권장(RECOMMENDED), 상대 URI 사용 시 전체 경로 포함 권장(예: "/types/123")
- type URI는 해석 불가능한 URI일 수도 있음
  - 예: tag URI 스키마로 problem type을 고유 식별
    ```
    tag:example@example.org,2021-09-17:OutOfLuck
    ```
  - 그러나 이 명세는 해석 가능한 type URI 권장 → 향후 URI를 해석해 오류 정보를 발견하는 도구가 도입될 수 있음, 이 경우 해석 불가능 URI에서 해석 가능 URI로 전환하면 problem type의 새로운 신원이 생성되어 하위 호환성을 깨는 변경이 발생

##### 3.1.2 "status"

- "status": 이 문제 발생에 대해 원본 서버가 생성한 HTTP 상태 코드(RFC 9110 섹션 15)를 나타내는 JSON 숫자
  - 존재하는 경우 자문 정보일 뿐 → 소비자 편의를 위해 사용된 HTTP 상태 코드 전달
  - 생성자는, 이 형식을 이해하지 못하는 일반 HTTP 소프트웨어도 올바르게 동작하도록 보장하기 위해 실제 HTTP 응답에서 동일한 상태 코드 사용 필수(MUST)
  - 추가 주의사항: 섹션 5 참조
- 소비자는 status 멤버로, 생성자가 사용한 원래 상태 코드가 중개자·캐시 등에 의해 변경되었을 때와 메시지 콘텐츠가 HTTP 정보 없이 지속되었을 때를 판별 가능
  - 일반 HTTP 소프트웨어는 여전히 HTTP 상태 코드를 사용

##### 3.1.3 "title"

- "title": problem type에 대한 짧은, 사람이 읽을 수 있는 요약을 포함하는 JSON 문자열
  - 지역화(RFC 9110 섹션 12.1의 사전 콘텐츠 협상 등) 목적 제외하고는 문제 발생 간 변경 금지(SHOULD NOT)
  - "title" 문자열은 자문 정보 → type URI의 의미를 알지 못하거나 발견할 수 없는 사용자(오프라인 로그 분석 등)를 위한 보조 정보

##### 3.1.4 "detail"

- "detail": 이 문제의 특정 발생에 대한 사람이 읽을 수 있는 설명을 포함하는 JSON 문자열
  - 존재하는 경우, 디버깅 정보 제공보다 클라이언트가 문제를 수정하도록 돕는 데 초점을 맞춰야 함
  - 소비자는 정보를 얻기 위해 "detail" 멤버를 파싱 금지(SHOULD NOT) → 확장이 그러한 정보를 얻기 위한 더 적합하고 오류가 적은 방법

##### 3.1.5 "instance"

- "instance": 문제의 특정 발생을 식별하는 URI 참조를 포함하는 JSON 문자열
  - 역참조 가능하면 problem details 객체를 그로부터 가져올 수 있음, 사전 콘텐츠 협상(RFC 9110 섹션 12.5.1)으로 다른 형식으로 반환 가능
  - 역참조 불가능하면 서버에는 의미 있으나 클라이언트에는 불투명한 문제 발생의 고유 식별자로 기능
- 상대 URI 처리 방식·권장사항은 "type"과 동일: RFC 3986 섹션 5 기준 해석, 가능하면 절대 URI 사용 권장(RECOMMENDED), 상대 URI 사용 시 전체 경로 포함 권장(예: "/instances/123")

#### 3.2 확장 멤버

- Problem type 정의는 해당 problem type에 특정한 추가 멤버로 problem details 객체를 확장 가능(MAY)
  - 예: 위의 크레딧 부족 문제는 "balance"·"accounts" 확장 정의
  - 예: "유효성 검사 오류" 예시는 발견된 개별 오류 발생의 목록을 포함하는 "errors" 확장 정의, 각각의 상세 정보와 위치 포인터 포함
- Problem details를 소비하는 클라이언트는 인식하지 못하는 확장 무시 필수(MUST) → problem type이 진화하며 향후 추가 정보를 포함할 수 있게 함
- 확장 생성 시 problem type 작성자는 이름을 신중하게 선택 필요
  - XML 형식(부록 B)에서 사용되려면 XML 명세 섹션 2.3의 Name 규칙 준수 필요

### 4. 새로운 Problem Type 정의

- HTTP API가 오류 조건을 나타내는 응답을 정의해야 할 때, 신규 problem type을 정의하는 것이 적절할 수 있음
  - 정의 전, problem type이 무엇에 적합하고 무엇이 다른 메커니즘에 맡기는 것이 더 나은지 이해 필요
- Problem details는 기본 구현을 위한 디버깅 도구가 아님 → HTTP 인터페이스 자체에 대한 더 큰 세부 정보를 노출하는 방법
  - 신규 problem type 설계자는 보안 고려사항(섹션 5)을 신중히 고려 필요, 특히 오류 메시지를 통해 구현 내부를 노출하고 공격 벡터를 노출하는 위험 주의
- 진정으로 일반적인 문제(웹의 모든 리소스에 적용될 수 있는 조건)는 일반적으로 일반 상태 코드로 표현하는 것이 더 나음
  - 예: "쓰기 접근 불허" 문제는 PUT 요청에 대한 403 Forbidden 상태 코드로 자명 → 별도 problem type 불필요
- 애플리케이션은 이미 정의한 형식으로 오류를 전달하는 더 적절한 방법을 가지고 있을 수 있음
  - Problem details의 목적: 신규 "fault"·"error" 문서 형식을 수립할 필요성을 피하는 것, 기존 도메인별 형식을 대체하는 것이 아님
- HTTP 콘텐츠 협상을 사용해 기존 HTTP API에 problem details 지원을 추가하는 것도 가능(예: Accept 요청 헤더로 이 형식에 대한 선호 표시, HTTP 섹션 12.5.1 참조)
- 신규 problem type 정의는 다음을 문서화 필수(MUST)
  - type URI(일반적으로 "http"·"https" 스키마)
  - 이를 적절하게 설명하는 짧은 title
  - 함께 사용할 HTTP 상태 코드
- Problem type 정의는 적절한 상황에서 Retry-After 응답 헤더(HTTP 섹션 10.2.3)의 사용을 명시 가능(MAY)
- Problem type URI는 문제를 해결하는 방법을 설명하는 HTML 문서로 해석되어야 함(SHOULD)
- Problem type 정의는 problem details 객체에 추가 멤버를 명시 가능(MAY)
  - 예: 기계가 문제를 해결하는 데 사용할 다른 리소스에 대한 타입이 지정된 링크
  - 추가 멤버가 정의된 경우, 이름은 문자(ALPHA)로 시작 권장(SHOULD), ALPHA·DIGIT·"_"로 구성 권장(SHOULD)(JSON 이외의 형식으로도 직렬화 가능하도록), 3자 이상 권장(SHOULD)

#### 4.1 예시

- 예: 온라인 쇼핑 카트 API에서 사용자의 크레딧 부족으로 구매를 할 수 없음을 나타내야 하는 경우
  - 이 정보를 수용할 수 있는 애플리케이션별 형식이 이미 있다면 그것을 사용하는 것이 가장 좋음
  - 없다면 problem detail 형식(API가 JSON 기반이면 JSON, XML 기반이면 XML) 사용 가능
  - 레지스트리(섹션 4.2)에서 목적에 맞는 이미 정의된 type URI 검색 → 있으면 재사용
  - 없다면 신규 type URI(관리 하에 있고 시간이 지나도 안정적이어야 함)를 발행·문서화하고, 적절한 title·함께 사용할 HTTP 상태 코드·의미와 처리 방법을 문서화

#### 4.2 등록된 Problem Type

- 이 명세: 재사용을 촉진하기 위해 공통적으로 널리 사용되는 problem type URI를 위한 "HTTP Problem Types" 레지스트리 정의
  - 레지스트리 정책: RFC 8126 섹션 4.6에 따른 Specification Required
  - 요청 평가 시 지정 전문가는 커뮤니티 피드백, problem type의 정의 수준, 이 명세의 요구사항을 고려 필요
  - 벤더별·애플리케이션별·배포별 값은 등록 불가
  - 명세 문서는 안정적이고 자유롭게 이용 가능한 방식으로(이상적으로는 URL에 위치) 게시되어야 하나, 표준일 필요는 없음
- 등록 시 type URI에 접두사 "https://iana.org/assignments/http-problem-types#" 사용 가능(MAY) (이런 URI는 해석 불가능할 수 있음)
- 등록 요청 템플릿
  - Type URI: problem type을 위한 URI
  - Title: problem type에 대한 간단한 설명
  - Recommended HTTP status code: 해당 type과 함께 사용하기에 가장 적합한 상태 코드
  - Reference: type을 정의하는 명세에 대한 참조
- 등록 요청처: https://iana.org/assignments/http-problem-types 레지스트리 참조

##### 4.2.1 about:blank

이 명세는 하나의 Problem Type "about:blank"을 등록:

- Type URI: about:blank
- Title: See HTTP Status Code
- Recommended HTTP status code: N/A
- Reference: RFC 9457

- "about:blank" URI가 problem type으로 사용될 때, 문제에 HTTP 상태 코드 이상의 추가적인 의미가 없음을 나타냄
- "about:blank" 사용 시 title은 해당 코드에 대한 권장 HTTP 상태 구문과 동일 권장(SHOULD)(예: 404에 대한 "Not Found" 등), 클라이언트 선호(Accept-Language 요청 헤더로 표현)에 맞게 지역화 가능(MAY)
- "type" 멤버가 정의되는 방식(섹션 3.1.1)에 따라, "about:blank" URI는 해당 멤버의 기본값 → 명시적 "type" 멤버가 없는 모든 problem details 객체는 암묵적으로 이 URI 사용

### 5. 보안 고려사항

- 신규 problem type 정의 시 포함되는 정보를 신중하게 검토 필요, 실제로 문제를 생성할 때(직렬화 방식과 무관하게) 제공되는 상세 정보도 면밀히 검토 필요
- 위험: 시스템 손상·시스템 접근·시스템 사용자의 개인정보 침해에 악용될 수 있는 정보 유출
- 발생 정보에 대한 링크를 제공하는 생성자는, 스택 덤프 같은 구현 세부사항을 HTTP 인터페이스로 제공하는 것을 피하도록 권장 → 서버 구현·데이터 등 민감한 세부사항 노출 가능
- "status" 멤버는 HTTP 상태 코드 자체에서 사용 가능한 정보를 복제 → 둘 사이 불일치 가능성 존재
  - 불일치는 중개자가 전송 중 HTTP 상태 코드를 변경한 것(프록시·캐시 등)을 나타낼 수 있어, 상대적 우선순위는 명확하지 않음
  - 일반 HTTP 소프트웨어(프록시·로드 밸런서·방화벽·바이러스 스캐너 등)는 이 멤버에 전달된 상태 코드를 알거나 준수할 가능성이 낮음

### 6. IANA 고려사항

- "Media Types" 레지스트리 아래 "application" 레지스트리에서, "application/problem+json" 및 "application/problem+xml" 등록을 이 문서를 참조하도록 갱신
- 섹션 4.2에 명시된 "HTTP Problem Types" 레지스트리 생성, 섹션 4.2.1에 따라 "about:blank"으로 채움

### 7. 참조

- 규범적
  - [ABNF] RFC 5234, "Augmented BNF for Syntax Specifications: ABNF"
  - [HTTP] RFC 9110, "HTTP Semantics"
  - [JSON] RFC 8259, "The JavaScript Object Notation (JSON) Data Interchange Format"
  - [RFC2119] BCP 14, RFC 2119
  - [RFC8126] BCP 26, RFC 8126, "Guidelines for Writing an IANA Considerations Section in RFCs"
  - [RFC8174] BCP 14, RFC 8174, "Ambiguity of Uppercase vs Lowercase in RFC 2119 Key Words"
  - [URI] RFC 3986, "Uniform Resource Identifier (URI): Generic Syntax"
  - [XML] W3C Recommendation, "Extensible Markup Language (XML) 1.0"
- 정보적
  - [ABOUT] RFC 6694, "The 'about' URI Scheme"
  - [HTML5] WHATWG, "HTML: Living Standard"
  - [ISO-19757-2] ISO/IEC 19757-2:2008, RELAX NG
  - [JSON-POINTER] RFC 6901, "JavaScript Object Notation (JSON) Pointer"
  - [JSON-SCHEMA] draft-bhutton-json-schema-01
  - [RDFA] W3C Recommendation, "RDFa Core 1.1"
  - [RFC7807] "Problem Details for HTTP APIs", 대체된 이전 버전
  - [TAG] RFC 4151, "The 'tag' URI Scheme"
  - [WEB-LINKING] RFC 8288, "Web Linking"
  - [XSLT] W3C Recommendation, "Associating Style Sheets with XML documents 1.0"

### 부록 A. HTTP Problem을 위한 JSON Schema

- 이 섹션은 HTTP problem details를 위한 비규범적 JSON Schema 제시 → 스키마와 명세 텍스트 사이 불일치가 있는 경우 명세 텍스트가 우선

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "An RFC 7807 problem object",
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "format": "uri-reference",
      "description": "A URI reference that identifies the problem type."
    },
    "title": {
      "type": "string",
      "description": "A short, human-readable summary of the problem type."
    },
    "status": {
      "type": "integer",
      "description": "The HTTP status code generated by the origin server for this occurrence of the problem.",
      "minimum": 100,
      "maximum": 599
    },
    "detail": {
      "type": "string",
      "description": "A human-readable explanation specific to this occurrence of the problem."
    },
    "instance": {
      "type": "string",
      "format": "uri-reference",
      "description": "A URI reference that identifies the specific occurrence of the problem. It may or may not yield further information if dereferenced."
    }
  }
}
```

### 부록 B. HTTP Problem과 XML

- XML을 사용하는 HTTP 기반 API는 이 부록에서 정의된 형식으로 problem details 표현 가능

XML 형식을 위한 RELAX NG 스키마:

```
default namespace ns = "urn:ietf:rfc:7807"

start = problem

problem =
  element problem {
    (  element  type            { xsd:anyURI }?
     & element  title           { xsd:string }?
     & element  detail          { xsd:string }?
     & element  status          { xsd:positiveInteger }?
     & element  instance        { xsd:anyURI }? ),
    anyNsElement
  }

anyNsElement =
  (  element    ns:*  { anyNsElement | text }
   | attribute  *     { text })*
```

- 이 스키마는 문서화 목적으로만 의도 → XML 형식의 모든 제약 조건을 캡처하는 규범적 스키마가 아님, 선택한 스키마 언어의 기능에 따라 다른 XML 스키마 언어로 유사한 제약 조건 정의 가능
- 이 형식의 미디어 타입: "application/problem+xml"
- 확장 배열·객체는 자식 요소를 포함하는 요소를 객체로 간주해 XML 형식으로 직렬화, "i"라는 이름의 자식 요소만 포함하는 요소는 배열로 간주
  - 위 예시가 XML에서는 다음과 같이 나타남:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/problem+xml
Content-Language: en

<?xml version="1.0" encoding="UTF-8"?>
<problem xmlns="urn:ietf:rfc:7807">
  <type>https://example.com/probs/out-of-credit</type>
  <title>You do not have enough credit.</title>
  <detail>Your current balance is 30, but that costs 50.</detail>
  <instance>https://example.net/account/12345/msgs/abc</instance>
  <balance>30</balance>
  <accounts>
    <i>https://example.net/account/12345</i>
    <i>https://example.net/account/67890</i>
  </accounts>
</problem>
```

- 이 형식은 XML 네임스페이스 사용 → 주로 다른 XML 기반 형식에 임베딩 가능하게 하기 위함, 이것이 다른 네임스페이스의 요소·속성으로 확장 가능·필요함을 의미하지는 않음
  - RELAX NG 스키마는 XML 형식에서 사용되는 하나의 네임스페이스 요소만 명시적으로 허용
  - 모든 확장 배열·객체는 해당 네임스페이스만 사용해 XML 마크업으로 직렬화 필수(MUST)
- XML 형식 사용 시, 참조된 XSL Transformations (XSLT) 코드로 XML을 변환하도록 클라이언트에 지시하는 XML 처리 명령을 XML에 임베딩 가능
  - 이 코드가 XML을 (X)HTML로 변환하는 경우, XML 형식을 제공하면서도 변환을 수행할 수 있는 클라이언트에서 사람 친화적인 (X)HTML을 렌더링·표시 가능
  - XSLT 코드를 실행할 수 있는 클라이언트 수를 최대화하기 위해 XSLT 1.0 사용이 바람직

### 부록 C. 다른 형식과 함께 Problem Details 사용

- 일부 상황에서는 여기에 설명된 것 이외의 형식에 problem details를 임베딩하는 것이 유리할 수 있음
  - 예: HTML을 사용하는 API가 problem details를 표현하기 위해 HTML도 사용하고 싶은 경우
- Problem details는 기존 직렬화(JSON·XML) 중 하나를 해당 형식에 캡슐화하거나, problem detail의 모델(섹션 3)을 해당 형식의 규약으로 변환해 다른 형식에 임베딩 가능
  - 예: HTML에서는 script 태그 안에 JSON을 캡슐화해 문제 임베딩 가능

```html
<script type="application/problem+json">
  {
   "type": "https://example.com/probs/out-of-credit",
   "title": "You do not have enough credit.",
   "detail": "Your current balance is 30, but that costs 50.",
   "instance": "/account/12345/msgs/abc",
   "balance": 30,
   "accounts": ["/account/12345",
                "/account/67890"]
  }
</script>
```

  - 또는 Resource Description Framework in Attributes (RDFa)로의 매핑을 정의해 임베딩 가능
- 이 명세는 다른 형식에 problem details를 임베딩하는 것에 대한 특정 권장사항을 제시하지 않음 → 적절한 임베딩 방법은 사용 중인 형식과 해당 형식의 적용 방식 모두에 의존

### 부록 D. RFC 7807로부터의 변경사항

이 개정판의 변경 사항:

- 섹션 4.2는 공통 problem type URI의 레지스트리 도입
- 섹션 3은 여러 문제가 어떻게 처리되어야 하는지 명확화
- 섹션 3.1.1은 역참조할 수 없는 type URI의 사용에 대한 지침 제공

### 참고 자료

- [RFC 5789 원문](https://www.rfc-editor.org/rfc/rfc5789)
- [RFC 6585 원문](https://www.rfc-editor.org/rfc/rfc6585)
- [RFC 9457 원문](https://www.rfc-editor.org/rfc/rfc9457)
- [RFC 7807 (대체됨)](https://www.rfc-editor.org/rfc/rfc7807)
