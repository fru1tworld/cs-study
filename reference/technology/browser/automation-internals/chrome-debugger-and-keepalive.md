# chrome.debugger와 Service Worker Keepalive

## 1. 배경

Manifest V3의 [Service Worker Lifecycle](../chrome-extension/service-worker-lifecycle.md)은 이벤트 기반으로 SW를 시작/종료하는 것이 원칙이다. 하지만 [chrome.debugger](../chrome-extension/debugger-api.md)로 CDP 세션이 열려 있는 동안 SW가 예고 없이 종료되면, 진행 중인 자동화 작업(네비게이션 대기, 이벤트 리스너)이 끊겨 상태가 불일치해질 수 있다. 이 때문에 브라우저는 오랫동안 "디버거가 부착된 동안에는 SW를 유휴 종료하지 않는다"는 암묵적 관례를 유지해 왔다.

---

## 2. Chrome 118 전후 동작 변화

Chrome 118 부근부터 이 keepalive 동작이 조정되어, 단순히 `chrome.debugger.attach()`로 세션을 맺어두는 것만으로 SW가 무기한 살아있음을 보장받을 수 없게 됐다. 실제로 SW를 계속 살려두려면 다음과 같은 능동적인 활동이 필요하다.

| SW를 계속 살려두는 요인 | 비고 |
|--------------------------|------|
| 진행 중인 `sendCommand` 호출 (응답 대기 중) | 요청-응답 사이에는 유지됨 |
| 주기적으로 CDP 명령을 보내는 활동 | 완전히 유휴 상태가 지속되면 종료 후보가 됨 |
| `chrome.alarms`로 주기적 웨이크업 등록 | SW 종료 후에도 다음 알람에서 재시작 가능 |
| `chrome.storage.session`에 진행 상태 저장 | 재시작 후 상태 복원 |

---

## 3. 실무 대응 패턴

자동화/디버깅 확장 프로그램을 만들 때는 "디버거 세션이 곧 SW 생존을 보장한다"고 가정하지 않고, SW가 재시작되더라도 작업을 이어갈 수 있도록 설계해야 한다.

```javascript
// SW 재시작에도 진행 상태를 복원할 수 있도록 세션 정보를 storage에 기록
async function attachDebugger(tabId) {
  await chrome.debugger.attach({ tabId }, "1.3");
  await chrome.storage.session.set({ debuggingTabId: tabId });
}

// SW가 재시작되면 최상위 스코프에서 상태를 복원
chrome.runtime.onStartup.addListener(async () => {
  const { debuggingTabId } = await chrome.storage.session.get("debuggingTabId");
  if (debuggingTabId) {
    // 세션이 여전히 유효한지 확인 후 필요시 재부착
  }
});
```

| 설계 원칙 | 이유 |
|-----------|------|
| 긴 폴링/대기 루프를 SW 안에서 직접 돌리지 않기 | 유휴 판정으로 종료될 수 있음 |
| 상태를 `chrome.storage.session`에 주기적으로 반영 | SW 재시작 후 복원 가능하게 함 |
| `onDetach` 이벤트 처리 | 디버거 세션이 예기치 않게 끊긴 경우를 감지하고 재연결 로직 실행 |

---

## 4. 브라우저 자동화 도구(예: chrome-devtools-mcp)에 대한 함의

[chrome-devtools-mcp](./chrome-devtools-mcp.md)처럼 확장 프로그램이 아니라 별도 프로세스(Node.js 등)에서 `--remote-debugging-port`로 브라우저에 직접 연결하는 도구는 이 SW keepalive 이슈의 영향을 받지 않는다. 이 문제는 어디까지나 "확장 프로그램 내부에서 `chrome.debugger` API로 자기 자신의 SW를 디버깅하는 경우"에 국한된다.

---

## 5. 요약

- Chrome 118 전후로 `chrome.debugger` 세션만으로 확장 SW의 무기한 생존을 보장할 수 없게 됐다.
- SW 재시작을 전제로 상태를 영속화하고, `onDetach`로 세션 단절을 감지하는 설계가 필요하다.
- 별도 프로세스에서 원격 디버깅 포트로 연결하는 자동화 도구는 이 제약과 무관하다.

---

## 참고 자료

- [Service Worker Lifecycle](../chrome-extension/service-worker-lifecycle.md)
- [chrome.debugger API](../chrome-extension/debugger-api.md)
- [chrome-devtools-mcp](./chrome-devtools-mcp.md)
