# Content Security Policy Level 3 (CSP3)

> W3C Working Draft (지속 갱신 중)

## 1. 개요

CSP(Content Security Policy)는 서버가 HTTP 응답 헤더로 "이 페이지가 어떤 출처의 리소스를, 어떤 방식으로 로드/실행할 수 있는지" 화이트리스트를 선언해 XSS, 데이터 주입, 클릭재킹 같은 공격 표면을 줄이는 브라우저 보안 메커니즘이다. Level 3는 Level 2 대비 nonce/hash 기반 스크립트 허용, `strict-dynamic`, Trusted Types 연계 등을 정교화했다.

```http
Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-r4nd0m123'; object-src 'none'
```

---

## 2. 지시자(Directive) 분류

| 분류 | 대표 지시자 | 역할 |
|------|-------------|------|
| Fetch 지시자 | `default-src`, `script-src`, `style-src`, `img-src`, `connect-src`, `font-src`, `frame-src`, `media-src`, `worker-src` | 리소스 유형별 허용 출처 지정 |
| Document 지시자 | `base-uri`, `sandbox` | 문서 속성/샌드박스 정책 |
| Navigation 지시자 | `form-action`, `frame-ancestors` | 폼 제출 대상, iframe 임베드 허용 부모 제한 |
| Reporting 지시자 | `report-to`, `report-uri`(구) | 정책 위반 시 보고 대상 |
| Other 지시자 | `upgrade-insecure-requests`, `require-trusted-types-for` | HTTP→HTTPS 자동 승격, Trusted Types 강제 |

---

## 3. 소스 표현식(Source Expression)

| 표현식 | 의미 |
|--------|------|
| `'self'` | 문서와 같은 출처 |
| `'none'` | 모든 출처 차단 |
| `https://example.com` | 특정 출처만 허용 |
| `'unsafe-inline'` | 인라인 `<script>`/`style` 속성 허용 (강력히 비권장) |
| `'unsafe-eval'` | `eval()`, `new Function()` 등 허용 (비권장) |
| `'nonce-<값>'` | 응답마다 무작위 생성한 nonce와 일치하는 `<script nonce="...">`만 허용 |
| `'sha256-<해시>'` | 특정 해시값과 일치하는 인라인 스크립트만 허용 |
| `'strict-dynamic'` | nonce/hash로 신뢰된 스크립트가 동적으로 로드하는 추가 스크립트도 신뢰 전파 |

```html
<!-- 서버가 매 요청마다 다른 nonce 발급 -->
<script nonce="r4nd0m123">
  console.log('신뢰된 인라인 스크립트');
</script>
```

---

## 4. `strict-dynamic`의 의미

기존 화이트리스트 방식(`script-src https://cdn.example.com`)은 CDN 하나가 손상되면 전체가 뚫리고, URL 목록 관리 비용도 크다. `strict-dynamic`은 접근 방식을 바꾼다.

```http
Content-Security-Policy: script-src 'nonce-abc123' 'strict-dynamic'
```

- nonce가 붙은 `<script>` 태그만 최초 신뢰를 얻는다.
- 그 신뢰된 스크립트가 `document.createElement('script')`로 추가 로드하는 스크립트는 URL 화이트리스트 없이도 자동으로 신뢰된다.
- 대신 이 모드에서는 `'self'`, 호스트 화이트리스트가 대부분 무시되므로, URL 기반 정책과 병행하려면 하위 호환 폴백을 함께 명시한다.

---

## 5. Trusted Types 연계

CSP3는 DOM XSS의 근본 원인인 "문자열을 그대로 innerHTML 등에 대입하는 패턴"을 차단하는 Trusted Types API와 연동된다.

```http
Content-Security-Policy: require-trusted-types-for 'script'; trusted-types default
```

```javascript
// require-trusted-types-for 'script' 적용 시, 일반 문자열 대입은 차단됨
element.innerHTML = userInput; // TypeError 발생

// Trusted Types 정책을 통과한 값만 허용
const policy = trustedTypes.createPolicy('default', {
  createHTML: (input) => sanitize(input)
});
element.innerHTML = policy.createHTML(userInput);
```

---

## 6. 리포팅

정책 위반 시 브라우저가 보고서를 전송하도록 설정할 수 있다.

```http
Content-Security-Policy: default-src 'self'; report-to csp-endpoint
Report-To: {"group":"csp-endpoint","max_age":10886400,"endpoints":[{"url":"https://example.com/csp-reports"}]}
```

| 방식 | 상태 |
|------|------|
| `report-uri` | 구 방식, 여전히 널리 지원되지만 폐기 예정 |
| `report-to` | 신규 Reporting API 기반, `Report-To` 헤더와 함께 사용 |
| `Content-Security-Policy-Report-Only` | 정책을 강제하지 않고 위반만 보고 (마이그레이션 시 유용) |

---

## 7. `frame-ancestors`와 클릭재킹 방지

```http
Content-Security-Policy: frame-ancestors 'self' https://trusted-parent.com
```

레거시 `X-Frame-Options` 헤더를 대체하며, 더 세밀한 출처 목록과 CSP 정책 하나로 통합 관리가 가능하다.

---

## 8. 실무에서 자주 쓰는 조합

| 목적 | 정책 예시 |
|------|-----------|
| XSS 방어 기본형 | `default-src 'self'; object-src 'none'; base-uri 'self'` |
| nonce 기반 스크립트 허용 | `script-src 'nonce-<값>' 'strict-dynamic' https:` (구형 브라우저 폴백) |
| iframe 임베드 제한 | `frame-ancestors 'self'` |
| 혼합 콘텐츠 자동 업그레이드 | `upgrade-insecure-requests` |
| 마이그레이션 중 관찰 모드 | `Content-Security-Policy-Report-Only` 헤더로 먼저 배포 |

---

## 9. 요약

- CSP3는 출처 화이트리스트 대신 nonce/hash + `strict-dynamic`으로 스크립트 신뢰를 관리하는 방향으로 발전했다.
- Trusted Types 연계로 DOM XSS의 싱크(sink) 자체를 타입 시스템으로 차단할 수 있다.
- `frame-ancestors`가 `X-Frame-Options`를 대체하고, `report-to`가 `report-uri`를 대체하는 흐름이다.
- Chrome 확장 프로그램의 CSP 정책([Chrome improve-security 가이드](../../technology/browser/chrome-extension/improve-security-csp.md) 참고)도 이 CSP3 모델을 기반으로 한다.

---

## 참고 자료

- [CSP3 (W3C)](https://www.w3.org/TR/CSP3/)
- [Trusted Types (W3C)](https://www.w3.org/TR/trusted-types/)
- [MDN Content-Security-Policy](https://developer.mozilla.org/docs/Web/HTTP/Headers/Content-Security-Policy)
