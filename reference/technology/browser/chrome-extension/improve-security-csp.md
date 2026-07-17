# Chrome 확장 프로그램: 보안 강화 마이그레이션 (CSP)

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/migrate/improve-security

## 1. 개요

Manifest V3는 확장 프로그램의 공격 표면을 줄이기 위해 Content Security Policy(CSP) 적용 방식을 Manifest V2보다 훨씬 엄격하게 바꿨다. 핵심 변화는 "원격 코드 실행(Remote Code Execution)을 원천적으로 금지"하는 방향이다.

---

## 2. Manifest V2 → V3 CSP 변화

| 항목 | Manifest V2 | Manifest V3 |
|------|-------------|-------------|
| `content_security_policy` 형식 | 문자열 하나로 확장 페이지 전체에 적용 | `extension_pages`와 `sandbox` 키로 분리 |
| 원격 스크립트 로드 | CSP에서 완화하면 CDN 등에서 JS를 불러와 실행 가능 | 사실상 금지 (모든 코드는 패키지 내부에 있어야 함) |
| `eval()` / `new Function()` | CSP 완화 시 허용 가능 | 확장 페이지에서는 기본적으로 차단 |
| 인라인 스크립트 | 완화 가능 | 기본 정책에서 항상 차단 (변경 불가) |

```json
// Manifest V3
{
  "content_security_policy": {
    "extension_pages": "script-src 'self'; object-src 'self'",
    "sandbox": "sandbox allow-scripts; script-src 'self' 'unsafe-inline'; object-src 'self'"
  }
}
```

---

## 3. 원격 코드 실행 금지의 의미

Manifest V2 시절 일부 확장 프로그램은 CDN에서 JS 라이브러리를 실시간으로 내려받아 실행했다. 이는 편리하지만, 해당 CDN이 침해당하면 사용자 브라우저에 설치된 모든 확장 프로그램이 즉시 악성 코드를 실행하는 공급망 공격 경로가 된다. Manifest V3는 확장 프로그램 패키지에 포함된 코드만 실행하도록 강제해 이 경로를 차단한다.

| 예외 | 설명 |
|------|------|
| WebAssembly | 특정 조건 하에 허용 (패키지 내부에 포함되어야 함) |
| 원격 데이터(코드 아님) | JSON 설정값, 텍스트 콘텐츠 등 실행되지 않는 데이터를 원격에서 가져오는 것은 허용 |
| 프레임워크의 동적 import | 패키지 내부 파일 간 동적 import는 허용, 외부 URL은 불가 |

---

## 4. `eval` 대체 패턴

```javascript
// 금지되는 패턴
const fn = new Function("return " + userExpression);
eval(someString);

// 대안: 사전에 정의된 안전한 파서/화이트리스트 로직 사용
const ALLOWED_OPS = { add: (a, b) => a + b, sub: (a, b) => a - b };
const result = ALLOWED_OPS[opName]?.(a, b);
```

---

## 5. 마이그레이션 체크리스트

| 점검 항목 | 조치 |
|-----------|------|
| CDN에서 JS를 로드하는 코드 | 해당 라이브러리를 패키지에 번들링 |
| `eval`/`new Function` 사용 | 안전한 대체 로직으로 재작성 |
| 인라인 `<script>` 태그 | 별도 `.js` 파일로 분리 후 `<script src="...">`로 로드 |
| HTML 속성 인라인 핸들러(`onclick="..."`) | `addEventListener`로 전환 |
| Third-party 분석/광고 SDK | Manifest V3 호환 버전(원격 코드 미포함)으로 교체 |

---

## 6. 웹 표준 CSP와의 관계

확장 프로그램 CSP는 웹 페이지의 [CSP3](../../../standard/w3c/csp3.md) 명세와 문법은 유사하지만, 브라우저가 확장 프로그램 자체에 강제하는 정책이라 사이트 운영자가 완화할 수 없는 고정 제약(원격 코드 실행 금지 등)이 추가로 걸린다는 점이 다르다.

---

## 7. 요약

- Manifest V3 CSP는 원격 코드 실행을 원천 차단해 공급망 공격 표면을 줄인다.
- `content_security_policy`가 `extension_pages`/`sandbox`로 분리되었다.
- CDN 의존 라이브러리는 번들링, `eval` 사용 코드는 안전한 대체 로직으로 재작성해야 한다.

---

## 참고 자료

- [Improve security posture (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/migrate/improve-security)
- [CSP3](../../../standard/w3c/csp3.md)
