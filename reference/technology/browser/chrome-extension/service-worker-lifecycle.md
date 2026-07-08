# Chrome 확장 프로그램: Service Worker Lifecycle

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle

## 1. 개요

Manifest V3부터 확장 프로그램의 백그라운드 실행 환경은 상시 상주하던 Background Page(Manifest V2) 대신 이벤트 기반으로 실행/종료되는 Service Worker로 바뀌었다. Service Worker는 필요할 때 깨어나 이벤트를 처리하고, 일정 시간 유휴 상태가 지속되면 브라우저가 종료해 메모리를 회수한다. 이는 확장 프로그램의 코드가 "항상 살아있다"고 가정할 수 없다는 것을 의미하며, 상태 관리 방식 자체를 바꿔야 한다.

```
[이벤트 발생] → SW 시작(cold start) → 이벤트 리스너 실행 → 유휴(idle) → 약 30초 후 종료
      ↑                                                              │
      └────────────────── 다음 이벤트 발생 시 재시작 ───────────────────┘
```

---

## 2. 생명주기 상태

| 상태 | 설명 |
|------|------|
| Installed | 확장 프로그램 설치/업데이트 직후 |
| Starting | 이벤트 수신으로 SW가 새로 시작되는 중 |
| Running | 이벤트 리스너 콜백 실행 중 |
| Idle | 처리할 작업이 없어 대기 중 (약 30초 후 종료됨) |
| Stopped | 완전히 종료되어 메모리에서 사라짐 |

---

## 3. 전역 상태를 유지할 수 없는 문제

SW가 종료되면 메모리상의 전역 변수, 타이머, 진행 중인 클로저 상태가 모두 사라진다. 따라서 세션 간 유지해야 하는 데이터는 다음 방법으로 영속화해야 한다.

| 저장 방식 | 용도 |
|-----------|------|
| `chrome.storage.session` | SW 재시작에도 유지되지만 브라우저 종료 시 사라지는 메모리 기반 저장소 |
| `chrome.storage.local` / `chrome.storage.sync` | 브라우저 재시작 후에도 유지되는 영속 저장소 |
| `IndexedDB` | 대용량 구조화 데이터 |
| `setTimeout`/`setInterval` 사용 금지 | SW가 종료되면 타이머도 사라짐 → `chrome.alarms` API로 대체 |

```javascript
// 잘못된 패턴 - SW 종료 시 타이머 소실
setTimeout(() => doSomething(), 60_000);

// 올바른 패턴 - alarms API 사용
chrome.alarms.create('myAlarm', { delayInMinutes: 1 });
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === 'myAlarm') doSomething();
});
```

---

## 4. SW를 살아있게 유지하는 요인

| 요인 | 설명 |
|------|------|
| 실행 중인 이벤트 콜백 | 콜백이 반환되기 전까지는 종료되지 않음 (단, 콜백은 빠르게 끝나야 함) |
| 진행 중인 네트워크 요청 (`fetch`) | 요청이 완료될 때까지 SW 유지 |
| `chrome.debugger`로 부착된 디버깅 세션 | 부착이 살아있는 동안 SW 종료 방지 (Chrome 118부터 명시적 keepalive 동작 변경, `chrome-debugger-and-keepalive.md` 참고) |
| 메시지 포트(`chrome.runtime.connect`) 연결 | 포트가 열려 있는 동안 유지 |

---

## 5. 개발/디버깅 시 유의점

| 항목 | 권장 사항 |
|------|-----------|
| 상태 초기화 로직 | SW가 재시작될 때마다 필요한 상태를 다시 계산하거나 storage에서 복원하도록 설계 |
| 긴 작업 처리 | SW 내부에서 장시간 연산을 돌리지 말고, 필요하면 `offscreen` 문서나 별도 탭으로 위임 |
| 이벤트 리스너 등록 위치 | 최상위 스코프에서 동기적으로 등록해야 SW 재시작 후에도 이벤트를 놓치지 않음 (비동기 코드 안에서 등록하면 누락 위험) |
| DevTools 강제 종료 | `chrome://extensions`에서 SW를 수동으로 종료시켜 재시작 시나리오를 테스트 가능 |

---

## 6. 요약

- Manifest V3 Service Worker는 이벤트 기반으로 시작/종료되며 상시 상주하지 않는다.
- 전역 변수/타이머에 의존한 상태 관리는 동작하지 않으므로 `chrome.storage`, `chrome.alarms`로 대체해야 한다.
- 이벤트 리스너는 최상위 스코프에서 동기적으로 등록해야 재시작 후에도 이벤트를 수신할 수 있다.
- `chrome.debugger` 연결, 진행 중인 fetch, 열린 메시지 포트는 SW 종료를 지연시키는 요인이다.

---

## 참고 자료

- [Service worker lifecycle (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle)
- [chrome.debugger와 keepalive](../automation-internals/chrome-debugger-and-keepalive.md)
