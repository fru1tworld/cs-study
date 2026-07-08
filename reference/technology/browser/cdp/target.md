# CDP: Target 도메인

> 공식 문서: https://chromedevtools.github.io/devtools-protocol/tot/Target/

## 1. 개요

CDP `Target` 도메인은 브라우저 내부에 존재하는 "타겟"(탭, iframe, Service Worker, Worker, 확장 프로그램 백그라운드 등)을 발견하고, 각 타겟에 대한 CDP 세션을 생성/관리한다. 하나의 브라우저는 여러 타겟을 동시에 가질 수 있고, 각 타겟마다 독립적인 CDP 세션이 필요하다는 점에서 CDP 전체 구조의 "루트" 역할을 하는 도메인이다.

---

## 2. 핵심 메서드

| 메서드 | 역할 |
|--------|------|
| `getTargets` | 현재 존재하는 모든 타겟 목록 조회 |
| `createTarget` | 새 탭(타겟) 생성 |
| `closeTarget` | 타겟 종료 |
| `attachToTarget` | 특정 타겟에 CDP 세션 연결, `sessionId` 반환 |
| `detachFromTarget` | 세션 해제 |
| `setAutoAttach` | 새로 생성되는 타겟(예: `window.open()`으로 열린 팝업, 새 iframe)에 자동으로 연결 |
| `sendMessageToTarget` (구) | 특정 세션에 메시지 전달 (플랫 모드 이전 방식) |

---

## 3. 타겟 유형

| `type` 값 | 설명 |
|-----------|------|
| `page` | 일반 탭 |
| `iframe` | (Site Isolation으로 별도 프로세스가 된) 자식 프레임 |
| `worker` | Dedicated Worker |
| `service_worker` | Service Worker (확장 프로그램 SW 포함) |
| `browser` | 브라우저 프로세스 자체 (탭과 무관한 전역 명령용) |
| `other` | 그 외 |

---

## 4. Flat Mode (평면 세션 모드)

CDP 초기에는 여러 타겟에 명령을 보내려면 `sendMessageToTarget`으로 메시지를 한 번 더 감싸는 중첩 방식(session multiplexing)을 사용해야 했다. Flat Mode는 각 세션을 최상위 메시지에 `sessionId` 필드만 붙여 구분하는 방식으로 단순화한 것으로, 현재 대부분의 CDP 클라이언트 라이브러리(Puppeteer, Playwright, chrome-remote-interface 등)가 채택하고 있다.

```javascript
// Flat Mode: 최상위 메시지에 sessionId만 추가
ws.send(JSON.stringify({
  id: 1,
  sessionId: "ABCDEF123456",
  method: "Page.navigate",
  params: { url: "https://example.com" },
}));
```

```javascript
// (구) Nested Mode: Target.sendMessageToTarget으로 감싸야 했음
ws.send(JSON.stringify({
  id: 1,
  method: "Target.sendMessageToTarget",
  params: {
    sessionId: "ABCDEF123456",
    message: JSON.stringify({ id: 2, method: "Page.navigate", params: { url: "..." } }),
  },
}));
```

Flat Mode를 쓰려면 최초 연결 시 `Target.attachToTarget`을 `flatten: true` 옵션과 함께 호출해 세션을 연다.

```javascript
const { sessionId } = await client.send("Target.attachToTarget", {
  targetId,
  flatten: true,
});
```

---

## 5. setAutoAttach와 다중 타겟 자동화

```javascript
await client.send("Target.setAutoAttach", {
  autoAttach: true,
  waitForDebuggerOnStart: false,
  flatten: true,
});
```

`autoAttach: true`로 설정하면 팝업 창, 새로 열리는 탭, 크로스 오리진 iframe(OOPIF) 등이 생성될 때마다 자동으로 CDP 세션이 연결되어 `Target.attachedToTarget` 이벤트가 발생한다. 이는 자동화 도구가 사용자의 명시적 개입 없이 여러 탭/프레임을 동시에 추적할 때 필수적인 메커니즘이다.

---

## 6. OOPIF와의 관계

Site Isolation 정책 하에서 크로스 오리진 iframe은 부모와 다른 렌더링 프로세스에서 실행되는 별도 타겟이 된다. 따라서 부모 페이지의 `DOM`/`Page` 세션만으로는 그 iframe 내부를 조회할 수 없고, `Target` 도메인으로 해당 iframe 타겟에 별도 세션을 맺어야 한다. 배경 지식은 [OOPIF / Site Isolation](../automation-internals/oopif-site-isolation.md) 문서를 참고한다.

---

## 7. 요약

- `Target` 도메인은 탭/워커/iframe 등 CDP가 다루는 모든 "타겟"을 발견하고 세션을 관리하는 루트 도메인이다.
- Flat Mode는 세션 메시지 중첩 없이 `sessionId`만으로 라우팅하는 현재 표준 방식이다.
- `setAutoAttach`로 새로 생성되는 타겟(팝업, OOPIF)에 자동 연결해 다중 타겟 자동화를 구현한다.

---

## 참고 자료

- [CDP Target 도메인 (공식)](https://chromedevtools.github.io/devtools-protocol/tot/Target/)
- [Getting Started with CDP (Flat Mode)](./getting-started-flat-mode.md)
- [OOPIF / Site Isolation](../automation-internals/oopif-site-isolation.md)
