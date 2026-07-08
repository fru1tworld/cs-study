# Getting Started with CDP: Flat Mode

> 공식 가이드 기반 정리: chrome-remote-interface / puppeteer 문서 계열

## 1. 개요

Chrome DevTools Protocol에 처음 접근할 때 가장 먼저 마주치는 개념이 "세션(Session)"과 "플랫 모드(Flat Mode)"다. 하나의 WebSocket 연결로 여러 타겟(탭, iframe, worker)을 동시에 제어해야 하므로, 메시지를 어떤 세션으로 라우팅할지 구분하는 방식이 필요하다.

---

## 2. 연결 수립 흐름

```
1. 브라우저를 원격 디버깅 포트로 실행
   chrome --remote-debugging-port=9222

2. HTTP로 디버깅 대상 목록 조회
   GET http://localhost:9222/json/list

3. 원하는 타겟의 webSocketDebuggerUrl로 WebSocket 연결

4. (다중 타겟 제어 시) Target.attachToTarget으로 세션 생성
```

```bash
curl http://localhost:9222/json/list
```

```json
[
  {
    "id": "1A2B3C",
    "type": "page",
    "url": "https://example.com",
    "webSocketDebuggerUrl": "ws://localhost:9222/devtools/page/1A2B3C"
  }
]
```

---

## 3. 최소 연결 예시 (단일 타겟)

단일 탭만 제어한다면 `webSocketDebuggerUrl`에 직접 연결해 별도 세션 관리 없이 명령을 보낼 수 있다.

```javascript
const ws = new WebSocket("ws://localhost:9222/devtools/page/1A2B3C");
ws.onopen = () => {
  ws.send(JSON.stringify({ id: 1, method: "Page.enable" }));
  ws.send(JSON.stringify({
    id: 2,
    method: "Page.navigate",
    params: { url: "https://example.com" },
  }));
};
```

---

## 4. 다중 타겟 제어: Flat Mode

브라우저 최상위 엔드포인트(`/devtools/browser/...`)에 연결해 여러 탭/워커를 함께 다루려면 [Target 도메인](./target.md)으로 세션을 생성하고, 각 메시지에 `sessionId`를 붙여 라우팅하는 Flat Mode를 사용한다.

```javascript
// 1. 브라우저 레벨 WebSocket 연결
const ws = new WebSocket(browserWSEndpoint);

// 2. 특정 타겟에 flatten 세션으로 연결
send({ id: 1, method: "Target.attachToTarget", params: { targetId, flatten: true } });
// 응답으로 sessionId 수신

// 3. 이후 모든 명령에 sessionId를 함께 보냄
send({ id: 2, sessionId, method: "Page.navigate", params: { url: "https://example.com" } });
```

| 항목 | 브라우저 레벨 연결 | Flat Mode 세션 |
|------|----------------------|-------------------|
| 대상 | 브라우저 전체 (`Target`, `Browser` 등 전역 도메인) | 개별 탭/프레임/워커 |
| 메시지 라우팅 | `sessionId` 없음 | 메시지마다 `sessionId` 포함 |
| 사용 사례 | 새 탭 생성, 타겟 목록 조회 | 실제 페이지 조작(Page, DOM, Input 등) |

---

## 5. 이벤트 구독과 도메인 활성화

CDP 대부분의 도메인은 사용 전 `enable` 명령을 보내야 이벤트가 발생하기 시작한다.

```javascript
send({ id: 3, sessionId, method: "Page.enable" });
send({ id: 4, sessionId, method: "DOM.enable" });
send({ id: 5, sessionId, method: "Network.enable" });
```

이 활성화 호출을 빠뜨리면 이후 이벤트 리스너가 아무 이벤트도 받지 못하는 흔한 실수로 이어진다.

---

## 6. 실무에서 자주 겪는 문제

| 문제 | 원인/해결 |
|------|-----------|
| 명령이 응답 없이 멈춤 | `enable` 호출 누락, 또는 잘못된 `sessionId` 사용 |
| 팝업/새 탭을 놓침 | `Target.setAutoAttach` 미설정 |
| 네비게이션 후 `nodeId`가 무효화됨 | `DOM.documentUpdated` 이벤트 이후 노드 재조회 필요 (`DOM.md` 참고) |
| iframe 내부 조작 불가 | 크로스 오리진 iframe은 별도 타겟(OOPIF)일 수 있음 |

---

## 7. 요약

- CDP 연결은 HTTP로 타겟 목록을 조회한 뒤 WebSocket으로 연결하는 것에서 시작한다.
- 다중 타겟을 다루려면 `Target.attachToTarget({flatten: true})`으로 세션을 만들고 `sessionId`로 메시지를 라우팅한다.
- 대부분의 도메인은 `enable` 호출 없이는 이벤트를 발생시키지 않는다.

---

## 참고 자료

- [Target 도메인](./target.md)
- [DOM 도메인](./dom.md)
- [chrome.debugger API](../chrome-extension/debugger-api.md)
