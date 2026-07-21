# RFC 8628 - OAuth 2.0 Device Authorization Grant

> 발행일: 2019년 8월
> 상태: Proposed Standard

## 1. 개요

Device Authorization Grant(흔히 "Device Flow" 또는 "Device Code Flow")는 브라우저나 편리한 입력 수단이 없는 기기(스마트 TV, CLI 도구, IoT 기기, 콘솔 등)가 OAuth 2.0으로 사용자를 인증시키는 방식이다.
 기기 자체에서 로그인 화면을 띄우는 대신, 사용자가 스마트폰이나 PC 같은 별도 기기의 브라우저에서 짧은 코드를 입력해 인가를 완료한다.

[RFC 6749 OAuth2](./RFC6749-OAuth2.md)의 4가지 기본 Grant Type(Authorization Code, Implicit, Password, Client Credentials)로는 다루기 어려운 "입력 제약 기기(input-constrained device)" 시나리오를 위한 확장 Grant Type이다.

---

## 2. 전체 흐름

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

| 단계 | 설명 |
|------|------|
| (A) | 기기(클라이언트)가 인가 서버에 Device Authorization 요청 |
| (B) | 서버가 `device_code`, `user_code`, `verification_uri` 등을 응답 |
| (C) | 사용자가 스마트폰/PC 브라우저로 `verification_uri`에 접속해 `user_code` 입력 후 로그인·승인 |
| (D) | 기기는 화면에 `user_code`와 `verification_uri`(또는 QR 코드)를 표시해 사용자를 안내 |
| (E) | 기기가 `device_code`로 토큰 엔드포인트를 주기적으로 폴링 |
| (F) | 사용자가 승인을 완료하면 다음 폴링 응답에서 액세스 토큰 발급 |

---

## 3. 요청/응답 상세

### 3.1 Device Authorization 요청

```http
POST /device_authorization HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded

client_id=1406020730
&scope=example_scope
```

### 3.2 Device Authorization 응답

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

| 필드 | 설명 |
|------|------|
| `device_code` | 기기가 토큰을 폴링할 때 사용하는 긴 코드 (사용자에게 노출 안 함) |
| `user_code` | 사용자가 직접 입력하는 짧은 코드 (대소문자 구분 없음, 헷갈리는 문자 제외 권장) |
| `verification_uri` | 사용자가 접속할 인가 페이지 URL |
| `verification_uri_complete` | `user_code`가 미리 채워진 URL (QR 코드용) |
| `expires_in` | `device_code`/`user_code` 유효 시간(초) |
| `interval` | 폴링 최소 간격(초), 기본 5초 |

### 3.3 토큰 폴링 요청

```http
POST /token HTTP/1.1
Host: authorization-server.com
Content-Type: application/x-www-form-urlencoded

grant_type=urn:ietf:params:oauth:grant-type:device_code
&device_code=GmRhmhcxhwAzkoEqiMEg_DnyEysNkuNhszIySk9eS
&client_id=1406020730
```

### 3.4 폴링 중 에러 응답

| 에러 코드 | 의미 | 클라이언트 대응 |
|-----------|------|------------------|
| `authorization_pending` | 사용자가 아직 승인하지 않음 | `interval` 간격으로 계속 폴링 |
| `slow_down` | 폴링이 너무 빠름 | 다음 폴링부터 간격을 5초 더 늘림 |
| `access_denied` | 사용자가 거부함 | 폴링 중단, 실패 처리 |
| `expired_token` | `device_code` 만료 | 폴링 중단, 처음부터 재시도 안내 |

---

## 4. 사용자 경험 설계 가이드

| 항목 | 권장 사항 |
|------|-----------|
| `user_code` 형식 | 8자 내외, 하이픈으로 구분(`WDJB-MJHT`), 혼동되는 문자(0/O, 1/I) 제외 |
| QR 코드 | `verification_uri_complete`를 QR로 표시하면 사용자가 코드를 직접 입력하지 않아도 됨 |
| 대소문자 처리 | 서버는 `user_code` 비교 시 대소문자를 구분하지 않아야 함(SHOULD NOT) |
| 폴링 예의 | `interval`보다 빠르게 폴링하지 않고, `slow_down` 수신 시 간격을 늘려야 함(MUST) |

---

## 5. 보안 고려사항

| 위험 | 대응 |
|------|------|
| `user_code` 무차별 대입 | 짧은 코드이므로 시도 횟수 제한, 짧은 만료 시간(보통 10~30분) 필수 |
| 피싱(사용자가 공격자가 보여준 코드를 승인) | 인가 페이지에 요청 클라이언트 이름/스코프를 명확히 표시 |
| `device_code` 유출 | `device_code`는 사용자에게 노출하지 않고 기기 내부에서만 사용 |
| 폴링 남용 | 서버가 `interval` 미준수 클라이언트에 `slow_down` 또는 차단으로 대응 |

---

## 6. 요약

- Device Authorization Grant는 입력이 제한된 기기가 별도 기기의 브라우저를 통해 OAuth 인가를 완료하는 흐름이다.
- 기기는 `device_code`로 폴링하고, 사용자는 `user_code`를 별도 기기에서 입력한다.
- CLI 도구(GitHub CLI, AWS CLI 등), 스마트 TV 앱 로그인에 널리 쓰인다.
- [RFC 6749 OAuth2](./RFC6749-OAuth2.md) 프레임워크 위에 정의된 확장 Grant Type이다.

---

## 참고 자료

- [RFC 8628 원문](https://www.rfc-editor.org/rfc/rfc8628)
- [RFC 6749 OAuth2](./RFC6749-OAuth2.md)
