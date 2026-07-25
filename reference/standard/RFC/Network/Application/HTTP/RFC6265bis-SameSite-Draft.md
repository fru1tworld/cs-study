# RFC 6265bis - HTTP State Management (SameSite 초안)

> 문서: draft-ietf-httpbis-rfc6265bis
> 상태: IETF httpbis 작업 그룹 초안 (RFC 6265를 대체할 예정, 아직 RFC 번호 미확정)

## 1. 개요

`draft-ietf-httpbis-rfc6265bis`는 2011년 발행된 [RFC 6265 HTTP Cookies](./RFC6265-HTTP-Cookies.md)를 대체하기 위한 초안으로, 현재 대부분의 브라우저가 실제로 구현·강제하는 쿠키 동작(특히 `SameSite` 속성)을 표준 문서에 반영하는 것이 핵심이다.
 RFC 6265에는 `SameSite` 속성 자체가 없었고, 이후 브라우저 벤더들이 CSRF 방어를 위해 독자적으로 도입한 것을 이 초안이 표준화한다.

| 구분 | RFC 6265 (2011) | 6265bis 초안 |
|------|------------------|----------------|
| `SameSite` 속성 | 없음 | 정의됨 (`Strict` / `Lax` / `None`) |
| `SameSite` 기본값 | 없음(모든 요청에 전송) | `Lax` (미지정 시) |
| `Secure` 요구사항 | `SameSite=None`과 무관 | `SameSite=None`은 `Secure` 필수 |
| `__Host-` / `__Secure-` 접두사 | 없음 | 정의됨 |
| 스킴 기반 매칭 | 도메인만 비교 | "schemeful same-site" 개념 도입 |

---

## 2. SameSite 속성

쿠키가 크로스 사이트 요청에 함께 전송될지를 제어해 CSRF 공격 표면을 줄인다.

| 값 | 동작 | 사용 예 |
|----|------|---------|
| `Strict` | 같은 사이트에서 시작한 요청에만 전송. 다른 사이트에서 링크를 클릭해 이동해도 전송 안 함 | 은행 세션 쿠키처럼 최고 수준 보호가 필요한 경우 |
| `Lax` (기본값) | 최상위 탐색(top-level navigation)이면서 안전한 메서드(GET 등)인 경우에는 전송, `<img>`/`<iframe>`/`fetch` 등 서브리소스 요청에는 전송 안 함 | 대부분의 로그인 세션 쿠키 |
| `None` | 모든 크로스 사이트 요청에 전송 (반드시 `Secure`와 함께 사용) | 서드파티 임베드, 결제 위젯, SSO iframe |

```http
Set-Cookie: session=abc123; SameSite=Lax; Secure; HttpOnly
Set-Cookie: sso_token=xyz789; SameSite=None; Secure
```

### 2.1 기본값 변경의 영향

`SameSite`를 명시하지 않은 쿠키는 브라우저가 `Lax`로 취급한다.
 이 때문에:

- 크로스 사이트 POST 요청에 쿠키가 자동 전송되지 않아 CSRF 위험이 줄어듦
- 서드파티 iframe에서 세션 쿠키가 필요한 서비스(예: 결제 위젯, SSO)는 `SameSite=None; Secure`를 명시해야 정상 동작

---

## 3. `Secure` 속성과의 결합 강제

`SameSite=None`인 쿠키는 `Secure` 속성 없이는 브라우저가 거부한다.

```http
# 거부됨 (Chrome 등 최신 브라우저)
Set-Cookie: token=abc; SameSite=None

# 정상
Set-Cookie: token=abc; SameSite=None; Secure
```

HTTP(비TLS) 연결에서 `SameSite=None` 쿠키를 서드파티로 사용하려는 시도를 원천 차단해 평문 채널에서의 크로스 사이트 쿠키 탈취를 방지한다.

---

## 4. `__Host-` / `__Secure-` 쿠키 이름 접두사

쿠키 이름 자체에 보안 속성을 강제하는 접두사를 도입했다.

| 접두사 | 강제 조건 |
|--------|-----------|
| `__Secure-` | `Secure` 속성 필수 |
| `__Host-` | `Secure` 필수, `Path=/` 필수, `Domain` 속성 사용 불가(현재 호스트에만 귀속) |

```http
Set-Cookie: __Host-session=abc123; Secure; Path=/; SameSite=Strict
```

`__Host-` 접두사는 서브도메인이 상위 도메인 쿠키를 오염시키는 공격(cookie tossing)을 막는 데 특히 유용하다.

---

## 5. Schemeful Same-Site

기존에는 `http://example.com`과 `https://example.com`을 "같은 사이트"로 취급했지만, 6265bis는 스킴(http/https)까지 일치해야 같은 사이트로 판단하는 개념을 도입한다.

| 비교 대상 | 기존(Registrable Domain만 비교) | Schemeful Same-Site |
|-----------|-----------------------------------|----------------------|
| `http://example.com` vs `https://example.com` | Same-Site | Cross-Site |
| `https://a.example.com` vs `https://b.example.com` | Same-Site | Same-Site |

이를 통해 HTTPS로 마이그레이션하는 과정에서 발생할 수 있는 다운그레이드 공격(HTTP 채널을 이용한 쿠키 탈취) 경로를 줄인다.

---

## 6. 요약

- 6265bis는 RFC 6265를 대체할 초안으로, 실제 브라우저가 구현한 `SameSite` 동작을 표준화한다.
- `SameSite` 기본값은 `Lax`이며, `None`은 `Secure`와 함께여야 한다.
- `__Host-`/`__Secure-` 접두사로 쿠키 속성을 이름 수준에서 강제할 수 있다.
- Schemeful Same-Site 개념으로 HTTP/HTTPS를 다른 사이트로 취급해 보안을 강화한다.

---

## 참고 자료

- [draft-ietf-httpbis-rfc6265bis (IETF Datatracker)](https://datatracker.ietf.org/doc/draft-ietf-httpbis-rfc6265bis/)
- [RFC 6265 HTTP Cookies (현행, 대체 예정)](./RFC6265-HTTP-Cookies.md)
- [RFC 6454 Web Origin](./RFC6454-Web-Origin.md)
