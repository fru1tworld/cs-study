# OAuth 2.0 Device Authorization Grant

## RFC 8628 - OAuth 2.0 Device Authorization Grant

> 발행일: 2019년 8월
> 상태: Proposed Standard

### 1. 개요

스마트 TV, CLI 도구, IoT 기기, 콘솔처럼 브라우저나 편리한 입력 수단이 없는 기기에서는 로그인 화면을 직접 다루기 어렵다. Device Authorization Grant는 사용자가 스마트폰이나 PC의 브라우저에서 짧은 코드를 입력해 인가를 완료하도록 하는 방식이다. 흔히 "Device Flow" 또는 "Device Code Flow"라고 부른다.

이 방식은 [RFC 6749 OAuth2](./RFC6749-OAuth2.md)의 네 가지 기본 Grant Type(Authorization Code, Implicit, Password, Client Credentials)으로 다루기 어려운 "입력 제약 기기(input-constrained device)"를 위한 확장 Grant Type이다.

---

### 2. 전체 흐름

```
+----------+                                +----------------+
|          |>---(A)-- Client Identifier --->|                |
|          |                                |                |
|          |<---(B)-- Device Code,      ----|                |
|  Device  |          User Code,            |                |
|  Client  |          Verification URI      |                |
|          |                                |                |
|  [폴링]  |>---(E)-- Device Code       --->|                |
|          |          (반복 요청)            |  Authorization |
|          |<---(F)-- Access Token      ----|     Server     |
+----------+     (승인 완료 후)              |                |
                                            +----------------+
      ^
      |  (C) 사용자가 별도 기기(폰/PC)에서
      |      Verification URI 접속 후 User Code 입력
      v
+----------+
|  User    |
| (다른 기기의 브라우저)
+----------+
```

- (A): 기기(클라이언트)가 인가 서버에 Device Authorization 요청
- (B): 서버가 `device_code`, `user_code`, `verification_uri` 등을 응답
- (C): 사용자가 스마트폰/PC 브라우저로 `verification_uri`에 접속해 `user_code`를 입력한 뒤 로그인하고 승인
- (D): 기기는 화면에 `user_code`와 `verification_uri`(또는 QR 코드)를 표시해 사용자 안내
- (E): 기기가 `device_code`로 토큰 엔드포인트를 주기적으로 폴링
- (F): 사용자가 승인을 완료하면 다음 폴링 응답에서 액세스 토큰 발급

---

### 3. 요청/응답 상세

#### 3.1 Device Authorization 요청

```http
POST /device_authorization HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded

client_id=1406020730
&scope=example_scope
```

#### 3.2 Device Authorization 응답

```json
{
  "device_code": "GmRhmhcxhwAzkoEqiMEg_DnyEysNkuNhszIySk9eS",
  "user_code": "WDJB-MJHT",
  "verification_uri": "https://example.com/device",
  "verification_uri_complete": "https://example.com/device?user_code=WDJB-MJHT",
  "expires_in": 1800,
  "interval": 5
}
```

- `device_code`: 기기가 토큰을 폴링할 때 사용하는 긴 코드, 사용자에게 노출 안 함
- `user_code`: 사용자가 직접 입력하는 짧은 코드, 대소문자 구분 없음, 헷갈리는 문자 제외 권장
- `verification_uri`: 사용자가 접속할 인가 페이지 URL
- `verification_uri_complete`: `user_code`가 미리 채워진 URL, QR 코드용
- `expires_in`: `device_code`/`user_code` 유효 시간(초)
- `interval`: 폴링 최소 간격(초), 기본 5초

#### 3.3 토큰 폴링 요청

```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded

grant_type=urn:ietf:params:oauth:grant-type:device_code
&device_code=GmRhmhcxhwAzkoEqiMEg_DnyEysNkuNhszIySk9eS
&client_id=1406020730
```

#### 3.4 폴링 중 에러 응답

- `authorization_pending`: 사용자가 아직 승인하지 않음 → `interval` 간격으로 계속 폴링
- `slow_down`: 폴링이 너무 빠름 → 다음 폴링부터 간격을 5초 더 늘림
- `access_denied`: 사용자가 거부함 → 폴링 중단, 실패 처리
- `expired_token`: `device_code` 만료 → 폴링 중단, 처음부터 재시도 안내

---

### 4. 사용자 경험 설계 가이드

- `user_code` 형식: 8자 내외, 하이픈으로 구분(`WDJB-MJHT`), 혼동되는 문자(0/O, 1/I) 제외
- QR 코드: `verification_uri_complete`를 QR로 표시 → 사용자가 코드를 직접 입력하지 않아도 됨
- 대소문자 처리: 서버는 `user_code` 비교 시 대소문자를 구분하지 않아야 함(SHOULD NOT)
- 폴링 간격: `interval`보다 빠르게 폴링하지 않고, `slow_down` 수신 시 간격을 늘려야 함(MUST)

---

### 5. 보안 고려사항

- `user_code`는 짧아서 무차별 대입 공격에 노출되기 쉽다. 시도 횟수를 제한하고 만료 시간도 짧게 설정해야 한다(보통 10~30분).
- 사용자가 공격자가 보여 준 코드를 승인하는 피싱을 막으려면 인가 페이지에 요청한 클라이언트의 이름과 스코프를 명확히 표시해야 한다.
- `device_code`가 유출되지 않도록 사용자에게 노출하지 않고 기기 내부에서만 사용한다.
- 클라이언트가 `interval`을 지키지 않으면 서버는 `slow_down` 응답이나 차단으로 폴링 남용에 대응한다.

---

### 6. 요약

- Device Authorization Grant: 입력이 제한된 기기가 별도 기기의 브라우저를 통해 OAuth 인가를 완료하는 흐름
- 기기는 `device_code`로 폴링, 사용자는 `user_code`를 별도 기기에서 입력
- CLI 도구(GitHub CLI, AWS CLI 등), 스마트 TV 앱 로그인에 널리 쓰임
- [RFC 6749 OAuth2](./RFC6749-OAuth2.md) 프레임워크 위에 정의된 확장 Grant Type

---

### 참고 자료

- [RFC 8628 원문](https://www.rfc-editor.org/rfc/rfc8628)
- [RFC 6749 OAuth2](./RFC6749-OAuth2.md)
