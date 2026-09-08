# Content Security Policy Level 3 (CSP3)

> W3C Working Draft (지속 갱신 중)

## 1. 개요

CSP(Content Security Policy)는 서버가 HTTP 응답 헤더로 페이지에서 허용할 리소스 출처와 로드 및 실행 방식을 선언하는 브라우저 보안 기능이다. 브라우저가 이 정책에 따라 동작을 제한해 XSS, 데이터 주입, 클릭재킹 등의 공격 표면을 줄인다. Level 3에서는 nonce/hash 기반 스크립트 허용, `strict-dynamic`, Trusted Types 연계 등을 Level 2보다 정교하게 다룬다.

```http
Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-r4nd0m123'; object-src 'none'
```

---

## 2. 지시자(Directive) 분류

- Fetch 지시자: `default-src`, `script-src`, `style-src`, `img-src`, `connect-src`, `font-src`, `frame-src`, `media-src`, `worker-src`
  - 역할: 리소스 유형별 허용 출처 지정
- Document 지시자: `base-uri`, `sandbox`
  - 역할: 문서 속성/샌드박스 정책
- Navigation 지시자: `form-action`, `frame-ancestors`
  - 역할: 폼 제출 대상, iframe 임베드 허용 부모 제한
- Reporting 지시자: `report-to`, `report-uri`(구)
  - 역할: 정책 위반 시 보고 대상
- Other 지시자: `upgrade-insecure-requests`, `require-trusted-types-for`
  - 역할: HTTP→HTTPS 자동 승격, Trusted Types 강제

참고
- `worker-src` → 동작 방식은 다른 Fetch 지시자와 유사하나, CSP3 스펙 원문의 절 구성상으로는 `webrtc`와 함께 "Other Directives"로 분류됨
- `upgrade-insecure-requests`와 `require-trusted-types-for` → CSP3가 직접 정의하는 지시자가 아니라, 각각 [Upgrade Insecure Requests](https://www.w3.org/TR/upgrade-insecure-requests/)와 [Trusted Types](https://www.w3.org/TR/trusted-types/) 스펙에서 정의하고 CSP 헤더 문법에 통합되는 것

---

## 3. 소스 표현식(Source Expression)

- `'self'`: 문서와 같은 출처
- `'none'`: 모든 출처 차단
- `https://example.com`: 특정 출처만 허용
- `'unsafe-inline'`: 인라인 `<script>`/`style` 속성 허용(사용하지 않을 것을 강하게 권고)
- `'unsafe-eval'`: `eval()`, `new Function()` 등 허용(권장하지 않음)
- `'nonce-<값>'`: 응답마다 무작위 생성한 nonce와 일치하는 `<script nonce="...">`만 허용
- `'sha256-<해시>'`: 특정 해시값과 일치하는 인라인 스크립트만 허용
- `'strict-dynamic'`: nonce/hash로 신뢰된 스크립트가 동적으로 로드하는 추가 스크립트도 신뢰 전파

```html
<!-- 서버가 매 요청마다 다른 nonce 발급 -->
<script nonce="r4nd0m123">
  console.log('신뢰된 인라인 스크립트');
</script>
```

---

## 4. `strict-dynamic`의 의미

`script-src https://cdn.example.com`처럼 출처를 허용하는 방식은 신뢰한 CDN이 침해되면 페이지도 영향을 받을 수 있고, URL 목록을 관리하는 비용도 든다. `strict-dynamic`은 개별 스크립트에서 시작한 신뢰를 동적으로 로드하는 스크립트에 전달하는 방식이다.

```http
Content-Security-Policy: script-src 'nonce-abc123' 'strict-dynamic'
```

- nonce가 붙은 `<script>` 태그만 최초 신뢰 획득
- 그 신뢰된 스크립트가 `document.createElement('script')`로 추가 로드하는 스크립트 → URL 화이트리스트 없이도 자동 신뢰
- 이 모드에서는 `'self'`, 호스트 화이트리스트가 대부분 무시됨 → URL 기반 정책과 병행 시 하위 호환 폴백 함께 명시 필요

---

## 5. Trusted Types 연계

CSP3는 Trusted Types API와 연동해 DOM XSS를 일으킬 수 있는 문자열 대입을 제한한다. `innerHTML` 등에 문자열을 그대로 넣는 대신 정해진 정책을 거친 값을 사용하도록 강제하는 방식이다.

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

정책 위반이 발생하면 브라우저가 지정한 대상으로 보고서를 보내도록 설정할 수 있다.

```http
Content-Security-Policy: default-src 'self'; report-to csp-endpoint
Report-To: {"group":"csp-endpoint","max_age":10886400,"endpoints":[{"url":"https://example.com/csp-reports"}]}
```

- `report-uri`: 구 방식, 여전히 널리 지원되지만 폐기 예정
- `report-to`: 신규 Reporting API 기반, `Report-To` 헤더와 함께 사용
- `Content-Security-Policy-Report-Only`: 정책을 강제하지 않고 위반만 보고하므로 마이그레이션 시 유용

---

## 7. `frame-ancestors`와 클릭재킹 방지

```http
Content-Security-Policy: frame-ancestors 'self' https://trusted-parent.com
```

`frame-ancestors`를 사용하면 기존 `X-Frame-Options`보다 세밀한 출처 목록을 지정하고, 다른 CSP 정책과 함께 관리할 수 있다.

---

## 8. 실무에서 자주 쓰는 조합

- XSS 방어 기본형: `default-src 'self'; object-src 'none'; base-uri 'self'`
- nonce 기반 스크립트 허용: `script-src 'nonce-<값>' 'strict-dynamic' https:` (구형 브라우저 폴백)
- iframe 임베드 제한: `frame-ancestors 'self'`
- 혼합 콘텐츠 자동 업그레이드: `upgrade-insecure-requests`
- 마이그레이션 중 관찰 모드: `Content-Security-Policy-Report-Only` 헤더로 먼저 배포

---

## 9. 요약

- CSP3 → 출처 화이트리스트 대신 nonce/hash + `strict-dynamic`으로 스크립트 신뢰를 관리하는 방향으로 발전
- Trusted Types 연계 → DOM XSS의 싱크(sink) 자체를 타입 시스템으로 차단 가능
- `frame-ancestors`가 `X-Frame-Options`를 대체, `report-to`가 `report-uri`를 대체하는 흐름
- Chrome 확장 프로그램의 CSP 정책([Chrome 확장 프로그램 보안 가이드](../../technology/frontend/browser/chrome-extension/04_security_and_policy.md) 참고)도 이 CSP3 모델을 기반으로 함

---

## 참고 자료

- [CSP3 (W3C)](https://www.w3.org/TR/CSP3/)
- [Trusted Types (W3C)](https://www.w3.org/TR/trusted-types/)
- [MDN Content-Security-Policy](https://developer.mozilla.org/docs/Web/HTTP/Headers/Content-Security-Policy)
