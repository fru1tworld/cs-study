# Chrome 확장 프로그램 (Manifest V3) 공식 문서 정리

Manifest V3 기준 Chrome 확장 프로그램 개발에 필요한 핵심 개념/API를 정리한다.

| 문서 | 설명 |
|------|------|
| [Service Worker Lifecycle](./service-worker-lifecycle.md) | 이벤트 기반 백그라운드 실행 모델과 상태 관리 |
| [Content Scripts](./content-scripts.md) | 페이지 DOM에 주입되는 Isolated World 스크립트 |
| [scripting API](./scripting-api.md) | 동적 스크립트/CSS 주입 API |
| [debugger API](./debugger-api.md) | CDP 기반 탭 원격 제어 API |
| [sidePanel API](./side-panel-api.md) | 지속형 사이드 패널 UI |
| [보안 강화 (CSP) 마이그레이션](./improve-security-csp.md) | Manifest V3의 원격 코드 실행 차단 정책 |
| [네트워크 요청 다루기](./network-requests.md) | webRequest → declarativeNetRequest 전환 |
| [WebMCP Origin Trial](./webmcp-origin-trial.md) | AI 에이전트용 실험적 웹 API 트라이얼 |
| [ExtensionInstallForcelist](./extension-install-forcelist.md) | 기업 환경의 확장 강제 설치 정책 |

## 관련 문서

- [CDP (Chrome DevTools Protocol)](../cdp/README.md)
- [브라우저 자동화 내부 동작](../automation-internals/README.md)
- [CSP3 (W3C)](../../../standard/w3c/csp3.md)
