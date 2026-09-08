# 브라우저 자동화 내부 동작 정리

Puppeteer, Playwright, MCP 서버처럼 CDP 기반 자동화 도구를 사용할 때 접하는 브라우저 내부 동작을 정리한다. 공식 CDP와 Chrome 문서만으로 파악하기 어려운 부분은 관련 계층의 동작을 함께 살펴본다.

- [chrome.debugger와 SW Keepalive](./01_chrome-debugger-and-keepalive.md): 디버거 세션이 확장 SW 생존에 미치는 영향과 Chrome 118 전후 변화
- [Chrome 권한 목록 개요](./02_chrome-permissions-list.md): 확장 프로그램 권한 분류와 최소 권한 설계
- [Blink 좌표 공간](./03_blink-coordinate-spaces.md): CSS 픽셀/물리 픽셀/문서 좌표 구분
- [OOPIF / Site Isolation](./04_oopif-site-isolation.md): 크로스 사이트 iframe의 프로세스 분리와 CDP 세션
- [User Activation](./05_user-activation.md): 사용자 제스처 추적과 API 제약
- [chrome-devtools-mcp 개요](./06_chrome-devtools-mcp.md): CDP를 MCP 도구로 감싼 브라우저 자동화 서버
- [MDN 입력 이벤트 레퍼런스](./07_mdn-input-events-reference.md): isTrusted, User Activation, compositionstart, devicePixelRatio
- [Trusted Input 구현 비교](./08_trusted-input-implementations.md): Chromium/Puppeteer/Playwright/React의 입력 처리 방식 비교

## 관련 문서

- [CDP 개요](../cdp/README.md)
- [Chrome 확장 프로그램](../chrome-extension/README.md)
