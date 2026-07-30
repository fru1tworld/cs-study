# Chrome 확장 프로그램: 보안 강화(CSP)와 설치 정책

## Chrome 확장 프로그램: 보안 강화 마이그레이션 (CSP)

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/migrate/improve-security

### 1. 개요

Manifest V3는 확장 프로그램의 공격 표면을 줄이기 위해 Content Security Policy(CSP) 적용 방식을 Manifest V2보다 훨씬 엄격하게 바꿨다. 핵심 변화는 "원격 코드 실행(Remote Code Execution)을 원천적으로 금지"하는 방향이다.

---

### 2. Manifest V2 → V3 CSP 변화

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

### 3. 원격 코드 실행 금지의 의미

Manifest V2 시절 일부 확장 프로그램은 CDN에서 JS 라이브러리를 실시간으로 내려받아 실행했다. 이는 편리하지만, 해당 CDN이 침해당하면 사용자 브라우저에 설치된 모든 확장 프로그램이 즉시 악성 코드를 실행하는 공급망 공격 경로가 된다. Manifest V3는 확장 프로그램 패키지에 포함된 코드만 실행하도록 강제해 이 경로를 차단한다.

| 예외 | 설명 |
|------|------|
| WebAssembly | 특정 조건 하에 허용 (패키지 내부에 포함되어야 함) |
| 원격 데이터(코드 아님) | JSON 설정값, 텍스트 콘텐츠 등 실행되지 않는 데이터를 원격에서 가져오는 것은 허용 |
| 프레임워크의 동적 import | 패키지 내부 파일 간 동적 import는 허용, 외부 URL은 불가 |

---

### 4. `eval` 대체 패턴

```javascript
// 금지되는 패턴
const fn = new Function("return " + userExpression);
eval(someString);

// 대안: 사전에 정의된 안전한 파서/화이트리스트 로직 사용
const ALLOWED_OPS = { add: (a, b) => a + b, sub: (a, b) => a - b };
const result = ALLOWED_OPS[opName]?.(a, b);
```

---

### 5. 마이그레이션 체크리스트

| 점검 항목 | 조치 |
|-----------|------|
| CDN에서 JS를 로드하는 코드 | 해당 라이브러리를 패키지에 번들링 |
| `eval`/`new Function` 사용 | 안전한 대체 로직으로 재작성 |
| 인라인 `<script>` 태그 | 별도 `.js` 파일로 분리 후 `<script src="...">`로 로드 |
| HTML 속성 인라인 핸들러(`onclick="..."`) | `addEventListener`로 전환 |
| Third-party 분석/광고 SDK | Manifest V3 호환 버전(원격 코드 미포함)으로 교체 |

---

### 6. 웹 표준 CSP와의 관계

확장 프로그램 CSP는 웹 페이지의 [CSP3](../../../standard/w3c/csp3.md) 명세와 문법은 유사하지만, 브라우저가 확장 프로그램 자체에 강제하는 정책이라 사이트 운영자가 완화할 수 없는 고정 제약(원격 코드 실행 금지 등)이 추가로 걸린다는 점이 다르다.

---

### 7. 요약

- Manifest V3 CSP는 원격 코드 실행을 원천 차단해 공급망 공격 표면을 줄인다.
- `content_security_policy`가 `extension_pages`/`sandbox`로 분리되었다.
- CDN 의존 라이브러리는 번들링, `eval` 사용 코드는 안전한 대체 로직으로 재작성해야 한다.

---

### 참고 자료

- [Improve security posture (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/migrate/improve-security)
- [CSP3](../../../standard/w3c/csp3.md)

---

## Chrome 정책: ExtensionInstallForcelist

> 공식 문서: https://chromeenterprise.google/policies/extension-install-forcelist/

### 1. 개요

`ExtensionInstallForcelist`는 기업/조직이 관리하는 Chrome 브라우저에 특정 확장 프로그램을 자동으로 설치하고, 사용자가 임의로 제거하지 못하도록 강제하는 엔터프라이즈 정책이다. Chrome Enterprise/그룹 정책(Windows GPO), macOS의 관리형 환경설정(managed preferences), 또는 클라우드 기반 Chrome Browser Cloud Management를 통해 배포된다.

---

### 2. 정책 형식

```json
{
  "ExtensionInstallForcelist": [
    "ahfgeienlihckogmohjhadlkjgocpleb;https://clients2.google.com/service/update2/crx",
    "extension_id_2"
  ]
}
```

| 구성 요소 | 설명 |
|-----------|------|
| 확장 프로그램 ID | Chrome Web Store 또는 사설 배포 시스템의 32자 확장 ID |
| 업데이트 URL (선택) | 세미콜론(`;`) 뒤에 명시. 생략하면 기본 Chrome Web Store 업데이트 URL 사용 |

---

### 3. 관련 정책과의 관계

| 정책 | 역할 |
|------|------|
| `ExtensionInstallForcelist` | 강제 설치 + 제거 불가 |
| `ExtensionInstallAllowlist` | 사용자가 설치할 수 있는 확장 화이트리스트 |
| `ExtensionInstallBlocklist` | 설치를 금지할 확장 블랙리스트 (`*`로 전체 차단 후 개별 허용 조합 가능) |
| `ExtensionSettings` | 위 정책들을 세분화해 확장 ID별/업데이트 URL별로 더 정교하게 제어하는 통합 정책 (신규 배포 시 권장) |

```json
{
  "ExtensionSettings": {
    "ahfgeienlihckogmohjhadlkjgocpleb": {
      "installation_mode": "force_installed",
      "update_url": "https://clients2.google.com/service/update2/crx"
    },
    "*": {
      "installation_mode": "blocked"
    }
  }
}
```

---

### 4. 배포 경로

| 플랫폼 | 배포 방식 |
|--------|-----------|
| Windows | 그룹 정책(GPO) 또는 레지스트리 |
| macOS | 관리형 환경설정 프로필(.mobileconfig / MDM) |
| Linux | JSON 정책 파일 (`/etc/opt/chrome/policies/managed/`) |
| ChromeOS | Google Admin Console |
| 클라우드 관리 | Chrome Browser Cloud Management (플랫폼 무관, 사용자 로그인 기반) |

---

### 5. 사설(비공개) 확장 프로그램 배포

Chrome Web Store에 공개하지 않은 사내 전용 확장 프로그램도 자체 호스팅한 update manifest XML을 `update_url`로 지정해 강제 설치할 수 있다. 이는 기업 내부 도구(사내 SSO 확장, 보안 에이전트 확장 등)를 일반 사용자에게 노출하지 않고 배포하는 표준적인 방법이다.

---

### 6. 사용자 경험에 미치는 영향

| 항목 | 동작 |
|------|------|
| 제거 시도 | `chrome://extensions`에서 제거 버튼이 비활성화되거나 재설치됨 |
| 비활성화 시도 | 정책에 따라 토글 자체가 잠기거나, 껐다가도 정책 재적용 시 다시 켜짐 |
| 신뢰 표시 | 관리형 브라우저에서는 확장 목록에 "관리자가 설치함" 배지가 표시됨 |

---

### 7. 보안 관점

기업 입장에서는 필수 보안 도구(엔드포인트 보호 확장, DLP 확장)를 사용자가 임의로 끄지 못하게 강제하는 용도로 쓰이지만, 반대로 이 정책이 침해되면(관리 콘솔 계정 탈취 등) 조직 내 모든 브라우저에 악성 확장을 강제 배포할 수 있는 경로가 되므로, 관리 콘솔 접근 권한 자체의 보호가 중요하다.

---

### 8. 요약

- `ExtensionInstallForcelist`는 관리형 Chrome 환경에서 특정 확장을 강제 설치·제거 불가 상태로 만드는 정책이다.
- 신규 배포에는 더 세분화된 제어가 가능한 `ExtensionSettings` 사용이 권장된다.
- 사설 확장 프로그램도 자체 update URL로 강제 배포할 수 있다.

---

### 참고 자료

- [ExtensionInstallForcelist (Chrome Enterprise 공식)](https://chromeenterprise.google/policies/extension-install-forcelist/)
- [ExtensionSettings (Chrome Enterprise 공식)](https://chromeenterprise.google/policies/#ExtensionSettings)
