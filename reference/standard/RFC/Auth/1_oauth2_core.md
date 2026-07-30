# OAuth 2.0 코어와 Bearer 토큰

## RFC 6749 - The OAuth 2.0 Authorization Framework

> 발행일: 2012년 10월
> 상태: Standards Track

### 1. 개요

OAuth 2.0은 제3자 애플리케이션이 사용자 자격 증명을 직접 다루지 않고 리소스에 접근할 수 있게 해주는 인가(Authorization) 프레임워크 입니다.

#### 핵심 개념

> "사용자의 비밀번호를 제3자에게 공유하지 않고, 제한된 접근 권한을 부여"

| 특징 | 설명 |
|------|------|
| 권한 위임 | 사용자가 앱에 자신의 리소스 접근 권한 부여 |
| 토큰 기반 | 비밀번호 대신 액세스 토큰 사용 |
| 범위 제한 | 필요한 권한만 선택적으로 부여 |
| 시간 제한 | 토큰 만료로 보안 강화 |

---

### 2. 역할 (Roles)

#### 2.1 네 가지 역할

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  ┌──────────────┐          ┌──────────────────────┐             │
│  │   Resource   │          │  Authorization       │             │
│  │    Owner     │◄────────►│     Server           │             │
│  │   (사용자)    │          │  (인가 서버)          │             │
│  └──────────────┘          └──────────────────────┘             │
│         │                            │                           │
│         │ 권한 부여                   │ 토큰 발급                  │
│         ▼                            ▼                           │
│  ┌──────────────┐          ┌──────────────────────┐             │
│  │    Client    │─────────►│    Resource          │             │
│  │   (앱/서비스) │  토큰     │      Server          │             │
│  │              │  사용     │   (API 서버)         │             │
│  └──────────────┘          └──────────────────────┘             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

| 역할 | 설명 | 예시 |
|------|------|------|
| Resource Owner | 보호된 리소스의 소유자 | 사용자 (end-user) |
| Resource Server | 리소스를 호스팅하는 서버 | Google Drive API |
| Client | 리소스 접근을 요청하는 애플리케이션 | 3rd party 앱 |
| Authorization Server | 토큰을 발급하는 서버 | Google OAuth Server |

---

### 3. 프로토콜 흐름

#### 3.1 추상적 프로토콜 흐름

```
     +--------+                               +---------------+
     |        |--(A)- Authorization Request ->|   Resource    |
     |        |                               |     Owner     |
     |        |<-(B)-- Authorization Grant ---|               |
     |        |                               +---------------+
     |        |
     |        |                               +---------------+
     |        |--(C)-- Authorization Grant -->| Authorization |
     | Client |                               |     Server    |
     |        |<-(D)----- Access Token -------|               |
     |        |                               +---------------+
     |        |
     |        |                               +---------------+
     |        |--(E)----- Access Token ------>|    Resource   |
     |        |                               |     Server    |
     |        |<-(F)--- Protected Resource ---|               |
     +--------+                               +---------------+
```

#### 3.2 단계별 설명

| 단계 | 설명 |
|------|------|
| (A) | 클라이언트가 리소스 소유자에게 인가 요청 |
| (B) | 리소스 소유자가 인가 승인 (Authorization Grant) 발급 |
| (C) | 클라이언트가 인가 승인을 인가 서버에 제출 |
| (D) | 인가 서버가 액세스 토큰 발급 |
| (E) | 클라이언트가 액세스 토큰으로 리소스 요청 |
| (F) | 리소스 서버가 보호된 리소스 반환 |

---

### 4. Authorization Grant 유형

#### 4.1 개요

| Grant Type | 용도 | 보안 수준 |
|------------|------|-----------|
| Authorization Code | 서버 사이드 앱 | 높음 |
| Implicit | SPA (더 이상 권장하지 않음) | 낮음 |
| Resource Owner Password | 신뢰할 수 있는 앱 | 중간 |
| Client Credentials | 서버 간 통신 | 높음 |

---

#### 4.2 Authorization Code Grant

가장 보안성이 높고 권장되는 방식

```
+----------+
| Resource |
|   Owner  |
+----+-----+
     ^
     |
    (B)
+----|----+          Client Identifier      +---------------+
|         |>---(A)-- & Redirection URI ---->|               |
|  User-  |                                 | Authorization |
|  Agent  |<---(B)-- Authorization Code ----|     Server    |
|         |                                 |               |
+----|----+                                 +---------------+
     |                                            ^      v
    (B)                                           |      |
     |                                            |      |
     v                                            |      |
+---------+                                       |      |
|         |>---(C)-- Authorization Code ----------'      |
|  Client |          & Redirection URI                   |
|         |                                              |
|         |<---(D)-- Access Token ----------------------'
+---------+       (w/ Optional Refresh Token)
```

##### Step 1: 인가 요청

```http
GET /authorize?
  response_type=code
  &client_id=s6BhdRkqt3
  &redirect_uri=https://client.example.org/callback
  &scope=openid%20profile
  &state=xyz123
HTTP/1.1
Host: authorization-server.com
```

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| response_type | 필수 | `code` 고정 |
| client_id | 필수 | 클라이언트 식별자 |
| redirect_uri | 선택 | 콜백 URI |
| scope | 선택 | 요청 권한 범위 |
| state | 권장 | CSRF 방지용 랜덤 문자열 |

##### Step 2: 인가 응답 (콜백)

```http
HTTP/1.1 302 Found
Location: https://client.example.org/callback?
  code=SplxlOBeZQQYbYS6WxSbIA
  &state=xyz123
```

##### Step 3: 토큰 요청

```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=authorization_code
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=https://client.example.org/callback
```

##### Step 4: 토큰 응답

```json
{
  "access_token": "SlAV32hkKG",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "8xLOxBtZp8",
  "scope": "openid profile"
}
```

##### Authorization Code 제약사항

Authorization Code는 보안을 위해 다음과 같은 제약사항을 가집니다:

| 제약사항 | 설명 | 권장값 |
|----------|------|--------|
| 일회용 (Single Use) | 코드는 한 번만 사용 가능. 재사용 시도 시 해당 코드로 발급된 모든 토큰 무효화 권장 | - |
| 짧은 만료 시간 | 발급 후 빠르게 만료되어야 함 | 10분 이내 권장 |
| 클라이언트 바인딩 | 특정 client_id에만 유효 | - |
| Redirect URI 바인딩 | 토큰 요청 시 동일한 redirect_uri 필수 | - |

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Authorization Code 수명 주기                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  [발급] ─────────── 유효 기간 (10분 이내) ─────────── [만료/사용]          │
│    │                                                    │                │
│    │                                                    │                │
│    └── 사용되면 → 즉시 무효화                            │                │
│    └── 재사용 시도 → 관련 토큰 모두 폐기 (보안 조치)       │                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

보안 권장사항:
- 인가 서버는 코드 사용 여부를 추적해야 함
- 코드 재사용 감지 시, 해당 코드로 발급된 액세스 토큰과 리프레시 토큰을 모두 폐기
- 코드는 암호학적으로 안전한 난수 생성기로 생성해야 함 (최소 128비트 엔트로피 권장)

---

#### 4.3 Implicit Grant (더 이상 권장하지 않음)

> 주의: 별도의 IETF 초안인 OAuth 2.1(아직 RFC로 확정되지 않음)에서는 제거될 예정.
 PKCE를 사용한 Authorization Code Flow 권장.

```
+----------+
| Resource |
|   Owner  |
+----+-----+
     ^
     |
    (B)
+----|----+          Client Identifier      +---------------+
|         |>---(A)-- & Redirection URI ---->|               |
|  User-  |                                 | Authorization |
|  Agent  |<---(B)--- Access Token ---------|     Server    |
|         |          (in URI Fragment)      |               |
+---------+                                 +---------------+
```

요청:
```http
GET /authorize?
  response_type=token
  &client_id=s6BhdRkqt3
  &redirect_uri=https://client.example.org/callback
  &scope=openid
  &state=xyz
```

응답:
```
https://client.example.org/callback#
  access_token=2YotnFZFEjr1zCsicMWpAA
  &token_type=Bearer
  &expires_in=3600
  &state=xyz
```

---

#### 4.4 Resource Owner Password Credentials Grant

신뢰할 수 있는 앱에서만 사용 (예: 같은 회사의 공식 앱)

```
+----------+
| Resource |
|  Owner   |
+----+-----+
     |
     |    Resource Owner
    (A) Password Credentials
     |
     v
+---------+                                  +---------------+
|         |>--(B)---- Resource Owner ------->|               |
|         |         Password Credentials     | Authorization |
| Client  |                                  |     Server    |
|         |<--(C)---- Access Token ---------|               |
|         |    (w/ Optional Refresh Token)  |               |
+---------+                                  +---------------+
```

요청:
```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded

grant_type=password
&username=johndoe
&password=A3ddj3w
&scope=read write
&client_id=s6BhdRkqt3
&client_secret=7Fjfp0ZBr1KtDRbnfVdmIw
```

---

#### 4.5 Client Credentials Grant

서버 간 통신 (사용자 개입 없음)

```
+---------+                                  +---------------+
|         |                                  |               |
|         |>--(A)- Client Authentication --->| Authorization |
| Client  |                                  |     Server    |
|         |<--(B)---- Access Token ---------|               |
|         |                                  |               |
+---------+                                  +---------------+
```

요청:
```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=client_credentials
&scope=api:read
```

---

### 5. 액세스 토큰

#### 5.1 토큰 사용

```http
GET /api/user HTTP/1.1
Host: api.example.com
Authorization: Bearer mF_9.B5f-4.1JqM
```

#### 5.2 토큰 유형

| 유형 | 설명 |
|------|------|
| Bearer Token | 토큰 소유자에게 권한 부여 (가장 일반적) |
| MAC Token | 메시지 인증 코드 포함 |

#### 5.3 Bearer 토큰 전송 방법

```http
# 1. Authorization 헤더 (권장)
Authorization: Bearer mF_9.B5f-4.1JqM

# 2. Form-Encoded Body
POST /resource HTTP/1.1
Content-Type: application/x-www-form-urlencoded

access_token=mF_9.B5f-4.1JqM

# 3. URI Query Parameter (비권장)
GET /resource?access_token=mF_9.B5f-4.1JqM
```

---

### 6. 리프레시 토큰

#### 6.1 개념

```
액세스 토큰: 짧은 수명 (분~시간)
리프레시 토큰: 긴 수명 (일~주~월)
```

#### 6.2 리프레시 흐름

```
+--------+                                           +---------------+
|        |--(A)------- Authorization Grant --------->|               |
|        |                                           |               |
|        |<-(B)----------- Access Token -------------|               |
|        |               & Refresh Token             |               |
|        |                                           |               |
|        |                            +----------+   |               |
|        |--(C)---- Access Token ---->|          |   |               |
|        |                            |          |   |               |
|        |<-(D)- Protected Resource --|          |   | Authorization |
| Client |                            | Resource |   |     Server    |
|        |--(E)---- Access Token ---->|  Server  |   |               |
|        |                            |          |   |               |
|        |<-(F)- Invalid Token Error -|          |   |               |
|        |                            +----------+   |               |
|        |                                           |               |
|        |--(G)----------- Refresh Token ----------->|               |
|        |                                           |               |
|        |<-(H)----------- Access Token -------------|               |
+--------+           & Optional Refresh Token        +---------------+
```

#### 6.3 리프레시 요청

```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=refresh_token
&refresh_token=tGzv3JOkF0XG5Qx2TlKWIA
&scope=read
```

---

### 7. 클라이언트 등록

#### 7.1 클라이언트 유형

| 유형 | 설명 | 예시 |
|------|------|------|
| Confidential | 자격 증명을 안전하게 보관 | 서버 사이드 앱 |
| Public | 자격 증명을 보호할 수 없음 | SPA, 모바일 앱 |

#### 7.2 등록 정보

```json
{
  "client_id": "s6BhdRkqt3",
  "client_secret": "7Fjfp0ZBr1KtDRbnfVdmIw",
  "client_name": "My Application",
  "redirect_uris": [
    "https://client.example.org/callback",
    "https://client.example.org/callback2"
  ],
  "grant_types": ["authorization_code", "refresh_token"],
  "scope": "openid profile email"
}
```

---

### 8. 보안 고려사항

#### 8.1 CSRF 방지

```
state 파라미터 필수 사용:
1. 클라이언트가 랜덤 state 생성
2. 인가 요청에 포함
3. 콜백에서 state 검증
```

#### 8.2 Authorization Code 인터셉션 방지

PKCE (Proof Key for Code Exchange) 사용:

> PKCE는 RFC 6749가 아닌 별도의 확장 스펙인 [RFC 7636](https://www.rfc-editor.org/rfc/rfc7636)에서 정의되며, `S256`과 `plain` 두 가지 code_challenge_method를 지원합니다 (아래는 권장 방식인 S256 예시).

```
1. 클라이언트가 code_verifier (랜덤 문자열) 생성
2. code_challenge = BASE64URL(SHA256(code_verifier))
3. 인가 요청에 code_challenge 포함
4. 토큰 요청에 code_verifier 포함
5. 서버가 검증
```

#### 8.3 토큰 보안

| 권장사항 | 설명 |
|----------|------|
| HTTPS 필수 | 모든 통신에 TLS 사용 |
| 짧은 만료 시간 | 액세스 토큰은 짧게 |
| 범위 최소화 | 필요한 scope만 요청 |
| 토큰 저장 | 안전한 저장소 사용 |

#### 8.4 Redirect URI 검증

```
- 정확히 일치하는 URI만 허용
- 와일드카드 사용 금지
- localhost는 개발 환경에서만
```

---

### 9. 에러 응답

#### 9.1 인가 에러

```http
HTTP/1.1 302 Found
Location: https://client.example.org/callback?
  error=access_denied
  &error_description=The+resource+owner+denied+the+request
  &state=xyz
```

| 에러 코드 | 설명 |
|-----------|------|
| invalid_request | 요청이 잘못됨 |
| unauthorized_client | 클라이언트가 이 grant type을 사용할 수 없음 |
| access_denied | 리소스 소유자가 거부 |
| unsupported_response_type | 지원하지 않는 response_type |
| invalid_scope | 요청한 scope가 유효하지 않음 |
| server_error | 서버 내부 오류 |
| temporarily_unavailable | 일시적 서버 불가 |

#### 9.2 토큰 에러

```json
{
  "error": "invalid_grant",
  "error_description": "The authorization code has expired"
}
```

| 에러 코드 | 설명 |
|-----------|------|
| invalid_request | 요청에 필수 파라미터가 없거나, 지원하지 않는 파라미터 값을 포함하거나, 파라미터가 중복됨 |
| invalid_client | 클라이언트 인증 실패 (알 수 없는 클라이언트, 인증 정보 누락 등) |
| invalid_grant | 인가 승인(코드/자격 증명)이 유효하지 않거나 만료되었거나 이미 사용됨 |
| unauthorized_client | 클라이언트가 이 grant type을 사용할 권한이 없음 |
| unsupported_grant_type | 인가 서버가 지원하지 않는 grant_type |
| invalid_scope | 요청한 scope가 유효하지 않거나 허용된 범위를 초과함 |

---

### 10. 요약

RFC 6749 OAuth 2.0은 안전한 권한 위임을 위한 프레임워크입니다:

- 4가지 역할: Resource Owner, Client, Authorization Server, Resource Server
- 4가지 Grant Type: Authorization Code, Implicit, Password, Client Credentials
- 토큰 기반: Access Token + Refresh Token
- 권장 방식: Authorization Code + PKCE
- 보안 필수: HTTPS, state 파라미터, 범위 최소화

OAuth 2.0은 현대 웹 서비스의 인증/인가 표준 으로, Google, Facebook, GitHub 등 대부분의 서비스가 채택하고 있습니다.

---

### 참고 자료

- [RFC 6749 원문](https://www.rfc-editor.org/rfc/rfc6749)
- [RFC 6750 Bearer Token](https://www.rfc-editor.org/rfc/rfc6750)
- [RFC 7636 PKCE](https://www.rfc-editor.org/rfc/rfc7636)
- [OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)

---

Internet Engineering Task Force (IETF)                          M. Jones
Request for Comments: 6750                                     Microsoft
Category: Standards Track                                       D. Hardt
ISSN: 2070-1721                                              Independent
                                                            October 2012

## OAuth 2.0 인가 프레임워크: Bearer 토큰 사용법

#### Abstract

이 명세서는 OAuth 2.0 보호 리소스에 접근하기 위한 HTTP 요청에서 bearer 토큰을 사용하는 방법을 설명합니다. bearer 토큰(이하 "bearer")을 소유한 모든 당사자는 해당 토큰을 소유한 다른 당사자와 동일한 방식으로 관련 리소스에 접근할 수 있습니다(암호화 키의 소유를 증명할 필요 없이). bearer 토큰의 오용을 방지하기 위해, bearer 토큰은 저장 및 전송 시 노출로부터 보호되어야 합니다.

### 이 문서의 상태

이 문서는 인터넷 표준 트랙 문서입니다.
 이 문서는 Internet Engineering Task Force(IETF)의 산출물입니다.
 이 문서는 IETF 커뮤니티의 합의를 대표합니다.
 이 문서는 공개 검토를 받았으며 Internet Engineering Steering Group(IESG)에 의해 발행이 승인되었습니다.
 인터넷 표준에 대한 추가 정보는 RFC 5741의 섹션 2에서 확인할 수 있습니다.

이 문서의 현재 상태, 정오표, 피드백 제공 방법에 대한 정보는 http://www.rfc-editor.org/info/rfc6750 에서 확인할 수 있습니다.

### 저작권 공지

Copyright (c) 2012 IETF Trust 및 문서 작성자로 명시된 사람들.
 모든 권리 보유.

이 문서는 BCP 78 및 이 문서의 발행일에 유효한 IETF 문서에 관한 IETF Trust의 법적 조항(http://trustee.ietf.org/license-info)의 적용을 받습니다.
 이 문서에 관한 귀하의 권리와 제한 사항이 설명되어 있으므로 이 문서를 주의 깊게 검토하십시오.
 이 문서에서 추출된 코드 구성 요소는 Trust Legal Provisions의 섹션 4.e에 설명된 대로 Simplified BSD License 텍스트를 포함해야 하며, Simplified BSD License에 설명된 대로 보증 없이 제공됩니다.

### 목차

   1. 소개
      1.1. 표기 규칙
      1.2. 용어
      1.3. 개요
   2. 인증된 요청
      2.1. Authorization Request Header Field
      2.2. Form-Encoded Body Parameter
      2.3. URI Query Parameter
   3. WWW-Authenticate 응답 헤더 필드
      3.1. 에러 코드
   4. 액세스 토큰 응답 예제
   5. 보안 고려사항
      5.1. 보안 위협
      5.2. 위협 완화
      5.3. 권장사항 요약
   6. IANA 고려사항
      6.1. OAuth Access Token Type 등록
         6.1.1. "Bearer" OAuth Access Token Type
      6.2. OAuth Extensions Error 등록
         6.2.1. "invalid_request" 에러 값
         6.2.2. "invalid_token" 에러 값
         6.2.3. "insufficient_scope" 에러 값
   7. 참조
      7.1. 규범적 참조
      7.2. 참고적 참조
   부록 A. 감사의 글

---

### 1. 소개

OAuth 2.0 인가 프레임워크 [RFC6749]는 서드파티 애플리케이션이 리소스 소유자와 HTTP 서비스 간의 승인 상호작용을 조율하여 리소스 소유자를 대신하여, 또는 서드파티 애플리케이션이 자체적으로 접근을 획득하도록 허용함으로써, HTTP 서비스에 대한 제한된 접근을 얻을 수 있게 합니다.
 리소스 소유자의 자격 증명을 사용하여 보호된 리소스에 접근하는 대신, 클라이언트는 특정 범위(scope), 수명(lifetime), 및 기타 접근 속성을 나타내는 문자열인 액세스 토큰을 획득합니다.
 액세스 토큰은 리소스 소유자의 승인을 받아 인가 서버에 의해 서드파티 클라이언트에게 발급됩니다.
 클라이언트는 리소스 서버가 호스팅하는 보호된 리소스에 접근하기 위해 액세스 토큰을 사용합니다.

이 명세서는 TLS(Transport Layer Security) [RFC5246]를 활용하여 HTTP/1.1 [RFC2616]을 통해 OAuth 2.0 보호 리소스에 접근하기 위한 bearer 토큰 사용 방법을 설명합니다.
 TLS는 이 명세서의 구현 및 사용에 있어 필수입니다. bearer 토큰을 사용하기 위한 다른 명세서에서는 TLS 요구사항을 확장할 수 있습니다.
 또한 이 명세서는 리소스 서버가 불충분한 인가로 인해 보호된 리소스 요청을 거부할 때 사용할 `WWW-Authenticate` HTTP 응답 헤더 필드의 Bearer 인증 체계를 정의합니다.

이 명세서는 HTTP/1.1 [RFC2617]에서 사용하기 위한 Bearer 인증 체계를 정의하지만, 이것이 서버 인증을 위해 HTTP `WWW-Authenticate` 및 `Authorization` 헤더 필드를 사용하는 것을 제한하지 않습니다.
 RFC 2617에 정의된 대로 프록시 인증에 사용될 수 있습니다.

#### 1.1. 표기 규칙

이 문서에서 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"은 RFC 2119 [RFC2119]에 설명된 대로 해석됩니다.

이 문서는 "Augmented BNF for Syntax Specifications: ABNF" [RFC5234]에 설명된 ABNF(Augmented Backus-Naur Form) 표기법을 사용합니다.
 또한, HTTP/1.1 [RFC2617]에서 auth-param 및 auth-scheme 규칙을, "Uniform Resource Identifier (URI): Generic Syntax" [RFC3986]에서 URI-reference 규칙을 사용합니다.

달리 명시되지 않는 한, 모든 프로토콜 파라미터 이름과 값은 대소문자를 구분합니다.

#### 1.2. 용어

Bearer Token:
   토큰을 소유한 모든 당사자("bearer")가 해당 토큰을 소유한 다른 당사자와 동일한 방식으로 토큰을 사용할 수 있는 속성을 가진 보안 토큰입니다. bearer 토큰의 사용은 암호화 키 자료(proof-of-possession)의 소유를 증명할 것을 요구하지 않습니다.

다른 모든 용어는 "The OAuth 2.0 Authorization Framework" [RFC6749]에 정의된 대로입니다.

#### 1.3. 개요

OAuth는 클라이언트가 리소스 소유자를 대신하여 보호된 리소스에 접근하는 수단을 제공합니다.
 일반적인 경우, 클라이언트가 보호된 리소스에 접근하기 전에 먼저 리소스 소유자로부터 인가를 획득하고, 인가 부여를 액세스 토큰으로 교환해야 합니다.
 액세스 토큰은 인가 부여에 의해 승인된 접근의 범위, 기간 및 기타 속성을 나타냅니다.
 클라이언트는 액세스 토큰을 리소스 서버에 제시하여 보호된 리소스에 접근합니다.
 일부 경우에는 클라이언트가 사전에 인가 부여를 획득하기 위해 리소스 소유자와 상호작용할 필요 없이 인가 서버에 직접 인가를 위해 자신의 자격 증명을 제시할 수 있습니다.

보호된 리소스에 접근하기 위해 액세스 토큰을 획득하고 사용하는 추상적 흐름은 아래와 같습니다:

```
     +--------+                               +---------------+
     |        |--(A)- Authorization Request ->|   Resource    |
     |        |                               |     Owner     |
     |        |<-(B)-- Authorization Grant ---|               |
     |        |                               +---------------+
     |        |
     |        |                               +---------------+
     |        |--(C)-- Authorization Grant -->| Authorization |
     | Client |                               |     Server    |
     |        |<-(D)----- Access Token -------|               |
     |        |                               +---------------+
     |        |
     |        |                               +---------------+
     |        |--(E)----- Access Token ------>|    Resource   |
     |        |                               |     Server    |
     |        |<-(F)--- Protected Resource ---|               |
     +--------+                               +---------------+
```

                     그림 1: 추상적 프로토콜 흐름

이 문서에서 설명하는 추상적 흐름은 단계 (E)와 (F)의 상호작용을 나타냅니다.
 이 단계에서 클라이언트가 리소스 서버에 의해 유효한 것으로 검증되는 bearer 액세스 토큰을 제시하는 방법을 설명합니다.

### 2. 인증된 요청

이 섹션에서는 리소스 서버에 리소스를 요청할 때 bearer 액세스 토큰을 전송하는 세 가지 방법을 정의합니다.
 클라이언트는 각 요청에서 토큰을 전송하기 위해 하나 이상의 방법을 사용해서는 안 됩니다(MUST NOT).

#### 2.1. Authorization Request Header Field

HTTP/1.1 [RFC2617]에 정의된 "Authorization" 요청 헤더 필드에 bearer 액세스 토큰을 전송할 때, 클라이언트는 "Bearer" HTTP 인가 체계를 사용하여 액세스 토큰을 전송합니다.

예를 들어:

```
     GET /resource HTTP/1.1
     Host: server.example.com
     Authorization: Bearer mF_9.B5f-4.1JqM
```

이 체계의 "Authorization" 헤더 필드의 구문은 RFC 2617의 섹션 2에 정의된 Basic 체계의 사용법을 따릅니다.
 Bearer는 [HTTP-AUTH]에 정의될 auth-scheme 구문의 b64token 구문을 따르며 토큰에 이어 하나 이상의 auth-param이 뒤따릅니다. bearer 자격 증명의 구문은 다음과 같습니다:

```
     b64token    = 1*( ALPHA / DIGIT /
                       "-" / "." / "_" / "~" / "+" / "/" ) *"="
     credentials = "Bearer" 1*SP b64token
```

클라이언트는 "Authorization" 요청 헤더 필드에 "Bearer" HTTP 인가 체계를 사용하여 bearer 토큰으로 인증된 요청을 보내야 합니다(SHOULD).
 리소스 서버는 이 방법을 반드시 지원해야 합니다(MUST).

#### 2.2. Form-Encoded Body Parameter

HTTP 요청 엔티티 본문에 액세스 토큰을 전송할 때, 클라이언트는 "access_token" 파라미터를 사용하여 요청 본문에 액세스 토큰을 추가합니다.
 클라이언트는 다음 조건이 모두 충족되지 않는 한 이 방법을 사용해서는 안 됩니다(MUST NOT):

   o  HTTP 요청 엔티티 헤더에 "Content-Type" 헤더 필드가
      "application/x-www-form-urlencoded"로 설정되어 포함되어 있을 것.

   o  엔티티 본문이 HTML 4.01 [W3C.REC-html401-19991224]에서
      정의한 "application/x-www-form-urlencoded" 콘텐츠 타입의
      인코딩 요구사항을 따를 것.

   o  HTTP 요청 엔티티 본문이 단일 파트(single-part)일 것.

   o  엔티티 본문에 인코딩할 콘텐츠가 전적으로 ASCII [USASCII]
      문자로 구성되어 있을 것(MUST).

   o  HTTP 요청 메서드가 요청 본문에 대해 정의된 의미론을 가진 것일 것.
      특히, 이는 "GET" 메서드를 사용해서는 안 된다(MUST NOT)는
      것을 의미합니다.

엔티티 본문은 다른 요청별 파라미터를 포함할 수 있으며(MAY), 이 경우 "access_token" 파라미터는 "&" 문자(ASCII 코드 38)를 사용하여 요청별 파라미터와 적절히 구분되어야 합니다(MUST).

예를 들어, 클라이언트는 전송 계층 보안을 사용하여 다음 HTTP 요청을 보냅니다:

```
     POST /resource HTTP/1.1
     Host: server.example.com
     Content-Type: application/x-www-form-urlencoded

     access_token=mF_9.B5f-4.1JqM
```

"application/x-www-form-urlencoded" 방법은 참여하는 브라우저가 "Authorization" 요청 헤더 필드에 접근할 수 없는 애플리케이션 컨텍스트를 제외하고는 사용해서는 안 됩니다(SHOULD NOT).
 리소스 서버는 이 방법을 지원할 수 있습니다(MAY).

#### 2.3. URI Query Parameter

HTTP 요청 URI에 액세스 토큰을 전송할 때, 클라이언트는 "Uniform Resource Identifier (URI): Generic Syntax" [RFC3986]에 정의된 대로 "access_token" 파라미터를 사용하여 요청 URI 쿼리 컴포넌트에 액세스 토큰을 추가합니다.

예를 들어, 클라이언트는 전송 계층 보안을 사용하여 다음 HTTP 요청을 보냅니다:

```
     GET /resource?access_token=mF_9.B5f-4.1JqM HTTP/1.1
     Host: server.example.com
```

HTTP 요청 URI 쿼리는 다른 요청별 파라미터를 포함할 수 있으며, 이 경우 "access_token" 파라미터는 "&" 문자(ASCII 코드 38)를 사용하여 요청별 파라미터와 적절히 구분되어야 합니다(MUST).

예를 들어:

```
    https://server.example.com/resource?access_token=mF_9.B5f-4.1JqM&p=q
```

URI Query Parameter 방법을 사용하는 클라이언트는 "no-store" 옵션을 포함한 Cache-Control 헤더도 전송해야 합니다(SHOULD).
 이러한 요청에 대한 서버 성공(2XX 상태) 응답은 "private" 옵션이 포함된 Cache-Control 헤더를 포함해야 합니다(SHOULD).

URI 방법과 관련된 보안 취약점(섹션 5 참조) 때문에, 특히 액세스 토큰이 포함된 URL이 로그에 기록될 가능성이 높기 때문에, "Authorization" 요청 헤더 필드나 HTTP 요청 엔티티 본문에 액세스 토큰을 전송하는 것이 불가능한 경우를 제외하고는 사용해서는 안 됩니다(SHOULD NOT).
 리소스 서버는 이 방법을 지원할 수 있습니다(MAY).

이 방법은 현재 사용을 문서화하기 위해 포함되었습니다.
 보안 결함(섹션 5 참조) 및 예약된 쿼리 파라미터 이름을 사용하여 "Architecture of the World Wide Web, Volume One" [W3C.REC-webarch-20041215]에 따른 URI 네임스페이스 모범 사례에 반하기 때문에 사용이 권장되지 않습니다.

### 3. WWW-Authenticate 응답 헤더 필드

보호된 리소스 요청이 인증 자격 증명을 포함하지 않거나 보호된 리소스에 대한 접근을 가능하게 하는 액세스 토큰을 포함하지 않는 경우, 리소스 서버는 HTTP "WWW-Authenticate" 응답 헤더 필드를 반드시 포함해야 합니다(MUST).
 다른 조건에 대한 응답으로도 이를 포함할 수 있습니다(MAY). "WWW-Authenticate" 헤더 필드는 HTTP/1.1 [RFC2617]에 정의된 프레임워크를 사용합니다.

이 명세서에 의해 정의된 모든 challenge는 auth-scheme 값 "Bearer"를 반드시 사용해야 합니다(MUST). 이 체계 뒤에 하나 이상의 auth-param 값이 반드시 따라야 합니다(MUST). 이 명세서에 의해 사용되거나 정의된 auth-param 속성은 다음과 같습니다.
 다른 auth-param 속성도 사용될 수 있습니다(MAY).

"realm" 속성은 HTTP/1.1 [RFC2617]에 설명된 방식으로 보호 범위를 나타내기 위해 포함될 수 있습니다(MAY). "realm" 속성은 한 번 이상 나타나서는 안 됩니다(MUST NOT).

"scope" 속성은 [RFC6749]의 섹션 3.3에 정의되어 있습니다. "scope" 속성은 요청된 리소스에 접근하기 위한 액세스 토큰의 필요 범위를 나타내는 대소문자 구분 scope 값의 공백으로 구분된 목록입니다. "scope" 값은 구현에 의해 정의됩니다.
 이를 위한 중앙화된 레지스트리는 없으며, 허용되는 값은 인가 서버에 의해 정의됩니다. "scope" 값의 순서는 중요하지 않습니다.
 일부 경우에, 보호된 리소스를 활용하기에 충분한 접근 범위를 가진 새 액세스 토큰을 요청할 때 "scope" 값이 사용됩니다. "scope" 속성의 사용은 선택적입니다(OPTIONAL). "scope" 속성은 한 번 이상 나타나서는 안 됩니다(MUST NOT). "scope" 값은 프로그래밍 방식 사용을 위한 것이며 최종 사용자에게 표시하기 위한 것이 아닙니다.

두 가지 scope 값 예제는 다음과 같습니다.
 이들은 각각 OpenID Connect [OpenID.Messages] 및 Open Authentication Technology Committee(OATC) Online Multimedia Authorization Protocol [OMAP] OAuth 2.0 사용 사례에서 가져온 것입니다:

```
     scope="openid profile email"
     scope="urn:example:channel=HBO&urn:example:rating=G,PG-13"
```

보호된 리소스 요청이 액세스 토큰을 포함하고 인증에 실패한 경우, 리소스 서버는 접근 요청이 거부된 이유를 클라이언트에게 제공하기 위해 "error" 속성을 포함해야 합니다(SHOULD). 파라미터 값은 섹션 3.1에 설명되어 있습니다.
 또한, 리소스 서버는 개발자에게 최종 사용자에게 표시하기 위한 것이 아닌 사람이 읽을 수 있는 설명을 제공하기 위해 "error_description" 속성을 포함할 수 있습니다(MAY).
 또한 에러를 설명하는 사람이 읽을 수 있는 웹 페이지를 식별하는 절대 URI가 포함된 "error_uri" 속성을 포함할 수 있습니다(MAY). "error", "error_description", "error_uri" 속성은 한 번 이상 나타나서는 안 됩니다(MUST NOT).

"scope" 속성의 값([RFC6749]의 부록 A.4에 명시됨)은 scope 값을 표현하기 위한 %x21 / %x23-5B / %x5D-7E 및 scope 값 사이의 구분자를 위한 %x20 이외의 문자를 포함해서는 안 됩니다(MUST NOT). "error" 및 "error_description" 속성의 값([RFC6749]의 부록 A.7 및 A.8에 명시됨)은 %x20-21 / %x23-5B / %x5D-7E 이외의 문자를 포함해서는 안 됩니다(MUST NOT). "error_uri" 속성의 값([RFC6749]의 부록 A.9에 명시됨)은 URI-reference 구문을 준수해야 하며(MUST) 따라서 %x21 / %x23-5B / %x5D-7E 이외의 문자를 포함해서는 안 됩니다(MUST NOT).

예를 들어, 인증 없는 보호된 리소스 요청에 대한 응답으로:

```
     HTTP/1.1 401 Unauthorized
     WWW-Authenticate: Bearer realm="example"
```

그리고 만료된 액세스 토큰을 사용한 인증 시도가 포함된 보호된 리소스 요청에 대한 응답으로:

```
     HTTP/1.1 401 Unauthorized
     WWW-Authenticate: Bearer realm="example",
                       error="invalid_token",
                       error_description="The access token expired"
```

#### 3.1. 에러 코드

요청이 실패하면, 리소스 서버는 적절한 HTTP 상태 코드(일반적으로 400, 401, 403 또는 405)를 사용하여 응답하고 응답에 다음 에러 코드 중 하나를 포함합니다:

invalid_request
      요청에 필수 파라미터가 누락되었거나, 지원되지 않는 파라미터 또는 파라미터 값이 포함되었거나, 동일한 파라미터가 반복되었거나, 액세스 토큰을 포함하기 위해 하나 이상의 방법이 사용되었거나, 그 외 형식이 잘못되었습니다.
 리소스 서버는 HTTP 400 (Bad Request) 상태 코드로 응답해야 합니다(SHOULD).

invalid_token
      제공된 액세스 토큰이 만료되었거나, 취소되었거나, 형식이 잘못되었거나, 다른 이유로 유효하지 않습니다.
 리소스는 HTTP 401 (Unauthorized) 상태 코드로 응답해야 합니다(SHOULD).
 클라이언트는 새 액세스 토큰을 요청하고 보호된 리소스 요청을 재시도할 수 있습니다(MAY).

insufficient_scope
      요청에 액세스 토큰이 제공하는 것보다 더 높은 권한이 필요합니다.
 리소스 서버는 HTTP 403 (Forbidden) 상태 코드로 응답해야 하며(SHOULD) 보호된 리소스에 접근하는 데 필요한 scope가 포함된 "scope" 속성을 포함할 수 있습니다(MAY).

요청에 인증 정보가 없는 경우(예: 클라이언트가 인증이 필요하다는 것을 인식하지 못했거나 지원되지 않는 인증 방법을 사용하여 시도한 경우), 리소스 서버는 에러 코드 또는 기타 에러 정보를 포함해서는 안 됩니다(SHOULD NOT).

예를 들어:

```
     HTTP/1.1 401 Unauthorized
     WWW-Authenticate: Bearer realm="example"
```

### 4. 액세스 토큰 응답 예제

일반적으로, bearer 토큰은 OAuth 2.0 [RFC6749] 액세스 토큰 응답의 일부로 클라이언트에 반환됩니다.
 이러한 응답의 예는 다음과 같습니다:

```
     HTTP/1.1 200 OK
     Content-Type: application/json;charset=UTF-8
     Cache-Control: no-store
     Pragma: no-cache

     {
       "access_token":"mF_9.B5f-4.1JqM",
       "token_type":"Bearer",
       "expires_in":3600,
       "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA"
     }
```

### 5. 보안 고려사항

이 섹션에서는 bearer 토큰을 사용할 때 토큰 처리에 관한 관련 보안 위협을 설명하고 이러한 위협을 완화하는 방법을 설명합니다.

#### 5.1. 보안 위협

다음 목록은 어떤 형태의 토큰을 활용하는 프로토콜에 대한 여러 일반적인 위협을 제시합니다.
 이 위협 목록은 NIST Special Publication 800-63 [NIST800-63]에 기반합니다.
 이 문서는 OAuth 2.0 인가 명세서 [RFC6749] 위에 구축되므로, 해당 문서 또는 관련 문서에 설명된 위협에 대한 논의는 제외합니다.

Token manufacture/modification(토큰 위조/변조): 공격자가 가짜 토큰을 생성하거나 기존 토큰의 토큰 내용(예: 인증 또는 속성 문)을 수정하여, 리소스 서버가 클라이언트에게 부적절한 접근을 허용하도록 할 수 있습니다.
 예를 들어, 공격자가 유효 기간을 연장하기 위해 토큰을 수정할 수 있습니다.
 악의적인 클라이언트가 보아서는 안 되는 정보에 접근하기 위해 어설션을 수정할 수 있습니다.

Token disclosure(토큰 노출): 토큰에는 민감한 정보를 포함하는 인증 및 속성 문이 포함될 수 있습니다.

Token redirect(토큰 리다이렉트): 공격자가 한 리소스 서버에서 소비하기 위해 생성된 토큰을 사용하여, 해당 토큰이 자신을 위한 것이라고 잘못 믿는 다른 리소스 서버에 접근합니다.

Token replay(토큰 재사용): 공격자가 과거에 해당 리소스 서버에서 이미 사용된 토큰을 사용하려고 시도합니다.

#### 5.2. 위협 완화

디지털 서명이나 MAC(Message Authentication Code)를 사용하여 토큰의 내용을 보호함으로써 광범위한 위협을 완화할 수 있습니다.
 또는, bearer 토큰이 인가 정보를 직접 인코딩하는 대신 인가 정보에 대한 참조를 포함할 수 있습니다.
 이러한 참조는 공격자가 추측하기에 불가능해야 합니다(MUST). 참조를 사용하면 참조를 인가 정보로 해석하기 위해 서버와 토큰 발급자 간의 추가 상호작용이 필요할 수 있습니다.
 이러한 상호작용의 메커니즘은 이 명세서에서 정의하지 않습니다.

이 문서는 토큰의 인코딩이나 내용을 명시하지 않으므로, 토큰 무결성 보호를 보장하는 수단에 대한 상세한 권장사항은 이 문서의 범위 밖입니다.
 토큰 무결성 보호는 토큰이 수정되는 것을 방지하기에 충분해야 합니다(MUST).

토큰 리다이렉트에 대처하기 위해, 인가 서버가 의도된 수신자(audience)의 신원, 일반적으로 단일 리소스 서버(또는 리소스 서버 목록)를 토큰에 포함하는 것이 중요합니다.
 토큰의 사용을 특정 scope로 제한하는 것도 권장됩니다(RECOMMENDED).

인가 서버는 TLS를 반드시 구현해야 합니다(MUST). 구현해야 하는 버전은 시간이 지남에 따라 달라지며, 구현 시점의 광범위한 배포 및 알려진 보안 취약점에 따라 달라집니다.
 이 문서 작성 시점에서, TLS 버전 1.2 [RFC5246]가 가장 최신 버전이지만, 실제 배포가 매우 제한적이며 구현 툴킷에서 쉽게 사용할 수 없을 수 있습니다.
 TLS 버전 1.0 [RFC2246]이 가장 널리 배포된 버전이며 가장 넓은 상호 운용성을 제공합니다.

토큰 노출을 방지하기 위해, 기밀성 및 무결성 보호를 제공하는 암호 모음을 사용하여 TLS [RFC5246]를 적용한 기밀성 보호가 반드시 적용되어야 합니다(MUST). 이를 위해 클라이언트와 인가 서버 간의 통신 상호작용뿐만 아니라 클라이언트와 리소스 서버 간의 상호작용에도 기밀성 및 무결성 보호를 활용해야 합니다.
 TLS가 이 명세서에서 구현 및 사용에 필수이므로, 통신 채널을 통한 토큰 노출을 방지하기 위한 선호되는 접근 방식입니다.
 클라이언트가 토큰의 내용을 관찰하는 것이 방지되는 경우, TLS 보호 사용에 추가하여 토큰 암호화가 반드시 적용되어야 합니다(MUST).
 토큰 노출에 대한 추가 방어로, 클라이언트는 보호된 리소스에 요청을 할 때 인증서 해지 목록(CRL) [RFC5280] 확인을 포함하여 TLS 인증서 체인을 반드시 검증해야 합니다(MUST).

쿠키는 일반적으로 평문으로 전송됩니다.
 따라서 쿠키에 포함된 모든 정보는 노출 위험이 있습니다.
 그러므로, bearer 토큰은 평문으로 전송될 수 있는 쿠키에 저장해서는 안 됩니다(MUST NOT).
 쿠키에 대한 보안 고려사항은 "HTTP State Management Mechanism" [RFC6265]을 참조하십시오.

로드 밸런서를 활용하는 배포를 포함한 일부 배포에서는, 리소스 서버에 대한 TLS 연결이 리소스를 제공하는 실제 서버 이전에 종료됩니다.
 이로 인해 TLS 연결이 종료되는 프론트엔드 서버와 리소스를 제공하는 백엔드 서버 사이에서 토큰이 보호되지 않을 수 있습니다.
 이러한 배포에서는 프론트엔드와 백엔드 서버 간의 토큰 기밀성을 보장하기 위한 충분한 조치가 반드시 사용되어야 합니다(MUST).
 토큰의 암호화는 이러한 가능한 조치 중 하나입니다.

토큰 캡처 및 재사용에 대처하기 위해 다음 권장사항이 제시됩니다: 첫째, 토큰의 수명이 반드시 제한되어야 합니다(MUST). 이를 달성하는 한 가지 수단은 토큰의 보호된 부분 안에 유효 시간 필드를 넣는 것입니다.
 짧은 수명(1시간 이하)의 토큰을 사용하면 유출될 경우의 영향을 줄일 수 있다는 점에 유의하십시오.
 둘째, 클라이언트와 인가 서버 간 및 클라이언트와 리소스 서버 간 교환의 기밀성 보호가 반드시 적용되어야 합니다(MUST). 결과적으로, 통신 경로상의 도청자가 토큰 교환을 관찰할 수 없습니다.
 따라서, 이러한 경로상 공격자는 토큰을 재사용할 수 없습니다.
 또한, 리소스 서버에 토큰을 제시할 때, 클라이언트는 "HTTP Over TLS" [RFC2818]의 섹션 3.1에 따라 해당 리소스 서버의 신원을 반드시 검증해야 합니다(MUST). 이러한 보호된 리소스 요청을 할 때 클라이언트가 TLS 인증서 체인을 반드시 검증해야 한다는 점에 유의하십시오.
 인증되지 않고 인가되지 않은 리소스 서버에 토큰을 제시하거나 인증서 체인을 검증하지 못하면 공격자가 토큰을 탈취하고 보호된 리소스에 대한 무단 접근을 허용합니다.

#### 5.3. 권장사항 요약

Bearer 토큰 보호: 클라이언트 구현은 bearer 토큰이 의도하지 않은 당사자에게 유출되지 않도록 반드시 보장해야 합니다(MUST). 해당 당사자들이 보호된 리소스에 접근하기 위해 이를 사용할 수 있기 때문입니다.
 이것은 bearer 토큰을 사용할 때의 주요 보안 고려사항이며 이후의 모든 더 구체적인 권장사항의 기반이 됩니다.

TLS 인증서 체인 검증: 클라이언트는 보호된 리소스에 요청을 할 때 TLS 인증서 체인을 반드시 검증해야 합니다(MUST).
 이를 하지 않으면 DNS 하이재킹 공격이 토큰을 탈취하고 의도하지 않은 접근을 허용할 수 있습니다.

항상 TLS(https) 사용: 클라이언트는 bearer 토큰으로 요청을 할 때 항상 TLS [RFC5246](https) 또는 동등한 전송 보안을 반드시 사용해야 합니다(MUST).
 이를 하지 않으면 공격자에게 의도하지 않은 접근을 제공할 수 있는 수많은 공격에 토큰이 노출됩니다.

쿠키에 bearer 토큰 저장 금지: 구현은 평문으로 전송될 수 있는 쿠키에 bearer 토큰을 저장해서는 안 됩니다(MUST NOT)(쿠키의 기본 전송 모드가 이에 해당합니다).
 쿠키에 bearer 토큰을 저장하는 구현은 반드시 교차 사이트 요청 위조에 대한 예방 조치를 취해야 합니다(MUST).

짧은 수명의 bearer 토큰 발급: 토큰 서버는 짧은 수명(1시간 이하)의 bearer 토큰을 발급해야 합니다(SHOULD). 특히 웹 브라우저 내에서 실행되는 클라이언트 또는 정보 유출이 발생할 수 있는 기타 환경에 토큰을 발급할 때 그러합니다.
 짧은 수명의 bearer 토큰을 사용하면 유출될 경우의 영향을 줄일 수 있습니다.

범위가 제한된 bearer 토큰 발급: 토큰 서버는 의도된 신뢰 당사자 또는 신뢰 당사자 집합으로 사용을 범위 지정하는 audience 제한이 포함된 bearer 토큰을 발급해야 합니다(SHOULD).

페이지 URL에 bearer 토큰 전달 금지: Bearer 토큰은 페이지 URL에 전달되어서는 안 됩니다(SHOULD NOT)(예: 쿼리 문자열 파라미터로).
 대신, bearer 토큰은 기밀성 조치가 취해진 HTTP 메시지 헤더 또는 메시지 본문에 전달되어야 합니다(SHOULD).
 브라우저, 웹 서버, 및 기타 소프트웨어는 브라우저 히스토리, 웹 서버 로그 및 기타 데이터 구조에서 URL을 적절히 보호하지 못할 수 있습니다. bearer 토큰이 페이지 URL에 전달되면, 공격자가 히스토리 데이터, 로그 또는 기타 보안이 적용되지 않은 위치에서 이를 탈취할 수 있습니다.

### 6. IANA 고려사항

#### 6.1. OAuth Access Token Type 등록

이 명세서는 [RFC6749]에 정의된 OAuth Access Token Types 레지스트리에 다음 액세스 토큰 타입을 등록합니다.

##### 6.1.1. "Bearer" OAuth Access Token Type

   Type name:
      Bearer

   Additional Token Endpoint Response Parameters:
      (없음)

   HTTP Authentication Scheme(s):
      Bearer

   Change controller:
      IETF

   Specification document(s):
      RFC 6750

#### 6.2. OAuth Extensions Error 등록

이 명세서는 [RFC6749]에 정의된 OAuth Extensions Error 레지스트리에 다음 에러 값을 등록합니다.

##### 6.2.1. "invalid_request" 에러 값

   Error name:
      invalid_request

   Error usage location:
      Resource access error response

   Related protocol extension:
      Bearer access token type

   Change controller:
      IETF

   Specification document(s):
      RFC 6750

##### 6.2.2. "invalid_token" 에러 값

   Error name:
      invalid_token

   Error usage location:
      Resource access error response

   Related protocol extension:
      Bearer access token type

   Change controller:
      IETF

   Specification document(s):
      RFC 6750

##### 6.2.3. "insufficient_scope" 에러 값

   Error name:
      insufficient_scope

   Error usage location:
      Resource access error response

   Related protocol extension:
      Bearer access token type

   Change controller:
      IETF

   Specification document(s):
      RFC 6750

### 7. 참조

#### 7.1. 규범적 참조

   [RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
              Requirement Levels", BCP 14, RFC 2119, March 1997.

   [RFC2246]  Dierks, T. and C. Allen, "The TLS Protocol Version 1.0",
              RFC 2246, January 1999.

   [RFC2616]  Fielding, R., Gettys, J., Mogul, J., Frystyk, H.,
              Masinter, L., Leach, P., and T. Berners-Lee, "Hypertext
              Transfer Protocol -- HTTP/1.1", RFC 2616, June 1999.

   [RFC2617]  Franks, J., Hallam-Baker, P., Hostetler, J., Lawrence, S.,
              Leach, P., Luotonen, A., and L. Stewart, "HTTP
              Authentication: Basic and Digest Access Authentication",
              RFC 2617, June 1999.

   [RFC2818]  Rescorla, E., "HTTP Over TLS", RFC 2818, May 2000.

   [RFC3986]  Berners-Lee, T., Fielding, R., and L. Masinter, "Uniform
              Resource Identifier (URI): Generic Syntax", STD 66,
              RFC 3986, January 2005.

   [RFC5234]  Crocker, D. and P. Overell, "Augmented BNF for Syntax
              Specifications: ABNF", STD 68, RFC 5234, January 2008.

   [RFC5246]  Dierks, T. and E. Rescorla, "The Transport Layer Security
              (TLS) Protocol Version 1.2", RFC 5246, August 2008.

   [RFC5280]  Cooper, D., Santesson, S., Farrell, S., Boeyen, S.,
              Housley, R., and W. Polk, "Internet X.509 Public Key
              Infrastructure Certificate and Certificate Revocation List
              (CRL) Profile", RFC 5280, May 2008.

   [RFC6265]  Barth, A., "HTTP State Management Mechanism", RFC 6265,
              April 2011.

   [RFC6749]  Hardt, D., Ed., "The OAuth 2.0 Authorization Framework",
              RFC 6749, October 2012.

   [USASCII]  American National Standards Institute, "Coded Character
              Set -- 7-bit American Standard Code for Information
              Interchange", ANSI X3.4, 1986.

   [W3C.REC-html401-19991224]
              Raggett, D., Le Hors, A., and I. Jacobs, "HTML 4.01
              Specification", World Wide Web Consortium
              Recommendation REC-html401-19991224, December 1999,
              <http://www.w3.org/TR/1999/REC-html401-19991224>.

   [W3C.REC-webarch-20041215]
              Jacobs, I. and N. Walsh, "Architecture of the World Wide
              Web, Volume One", World Wide Web Consortium
              Recommendation REC-webarch-20041215, December 2004,
              <http://www.w3.org/TR/2004/REC-webarch-20041215>.

#### 7.2. 참고적 참조

   [HTTP-AUTH]
              Fielding, R., Ed., and J. Reschke, Ed., "Hypertext
              Transfer Protocol (HTTP/1.1): Authentication", Work
              in Progress, October 2012.

   [NIST800-63]
              Burr, W., Dodson, D., Newton, E., Perlner, R., Polk, T.,
              Gupta, S., and E. Nabbus, "NIST Special Publication
              800-63-1, INFORMATION SECURITY", December 2011,
              <http://csrc.nist.gov/publications/>.

   [OMAP]     Huff, J., Schlacht, D., Nadalin, A., Simmons, J.,
              Rosenberg, P., Madsen, P., Ace, T., Rickelton-Abdi, C.,
              and B. Boyer, "Online Multimedia Authorization Protocol:
              An Industry Standard for Authorized Access to Internet
              Multimedia Resources", April 2012,
              <http://www.oatc.us/Standards/Download.aspx>.

   [OpenID.Messages]
              Sakimura, N., Bradley, J., Jones, M., de Medeiros, B.,
              Mortimore, C., and E. Jay, "OpenID Connect Messages 1.0",
              June 2012,
              <http://openid.net/specs/openid-connect-messages-1_0.html>.

### 부록 A. 감사의 글

다음 사람들이 이 문서의 예비 버전에 기여하였습니다: Blaine Cook (BT), Brian Eaton (Google), Yaron Y. Goland (Microsoft), Brent Goldman (Facebook), Raffi Krikorian (Twitter), Luke Shepard (Facebook), Allen Tom (Yahoo!). 이 문서의 내용과 개념은 OAuth 커뮤니티, Web Resource Authorization Profiles (WRAP) 커뮤니티, 및 OAuth Working Group의 산출물입니다.
 David Recordon이 OAuth 2.0 [RFC6749]으로 발전한 초기 명세서 초안에 기반하여 이 명세서의 예비 버전을 작성하였습니다.
 Michael B. Jones가 David의 예비 문서의 일부를 사용하여 이 명세서의 첫 번째 버전(00)을 작성하고 이후 모든 버전을 편집하였습니다.

OAuth Working Group에는 이 문서에 대한 아이디어와 문구를 제안한 수십 명의 매우 활발한 기여자가 있으며, 여기에는 다음이 포함됩니다: Michael Adams, Amanda Anganes, Andrew Arnott, Derek Atkins, Dirk Balfanz, John Bradley, Brian Campbell, Francisco Corella, Leah Culver, Bill de hOra, Breno de Medeiros, Brian Ellin, Stephen Farrell, Igor Faynberg, George Fletcher, Tim Freeman, Evan Gilbert, Yaron Y. Goland, Eran Hammer, Thomas Hardjono, Dick Hardt, Justin Hart, Phil Hunt, John Kemp, Chasen Le Hara, Barry Leiba, Amos Jeffries, Michael B. Jones, Torsten Lodderstedt, Paul Madsen, Eve Maler, James Manger, Laurence Miao, William J. Mills, Chuck Mortimore, Anthony Nadalin, Axel Nennker, Mark Nottingham, David Recordon, Julian Reschke, Rob Richards, Justin Richer, Peter Saint-Andre, Nat Sakimura, Rob Sayre, Marius Scurtescu, Naitik Shah, Justin Smith, Christian Stuebner, Jeremy Suriel, Doug Tangren, Paul Tarjan, Hannes Tschofenig, Franklin Tse, Sean Turner, Paul Walker, Shane Weeden, Skylar Woodward, Zachary Zeltsan.

### 저자 주소

   Michael B. Jones
   Microsoft

   EMail: mbj@microsoft.com
   URI:   http://self-issued.info/

   Dick Hardt
   Independent

   EMail: dick.hardt@gmail.com
   URI:   http://dickhardt.org/
