# Chrome DevTools Protocol (CDP) 정리

Chrome DevTools Protocol은 Chrome DevTools가 브라우저를 검사하고 제어할 때 사용하는 프로토콜이다. 확장 프로그램의 `chrome.debugger` API와 Puppeteer, Playwright, `chrome-devtools-mcp` 같은 브라우저 자동화 도구도 이를 기반으로 동작한다.

- [Getting Started (Flat Mode)](./01_getting-started-flat-mode.md): 연결 수립, 세션, Flat Mode 기본 흐름
- [Target 도메인](./02_target.md): 타겟(탭/워커/iframe) 발견과 세션 관리
- [Page 도메인](./03_page.md): 네비게이션, 로드 생명주기, 스크린샷/PDF
- [DOM 도메인](./04_dom.md): DOM 트리 조회/조작, 좌표 계산
- [Input 도메인](./05_input.md): 신뢰된(trusted) 마우스/키보드 이벤트 생성
- [Accessibility 도메인](./06_accessibility.md): 접근성 트리 조회, 의미 기반 요소 특정

## 관련 문서

- [chrome.debugger API](../chrome-extension/02_debugger_and_network.md)
- [브라우저 자동화 내부 동작](../automation-internals/README.md)
- [WAI-ARIA 1.2](../../../../standard/w3c/wai-aria.md)
