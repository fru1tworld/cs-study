# Chrome 확장 프로그램: 네트워크 요청 다루기

> 공식 문서: https://developer.chrome.com/docs/extensions/develop/concepts/network-requests

## 1. 개요

확장 프로그램이 네트워크 요청을 관찰·차단·수정하는 방식은 Manifest V2의 블로킹 `webRequest` API에서 Manifest V3의 `declarativeNetRequest` 중심 모델로 크게 바뀌었다. 이는 성능과 프라이버시(확장 프로그램이 모든 요청 내용을 직접 들여다보지 못하게 함)를 개선하기 위한 변화다.

---

## 2. 접근 방식 비교

| API | 방식 | 요청 내용 열람 | 실시간 수정 |
|-----|------|----------------|--------------|
| `webRequest` (Manifest V2, 블로킹) | 확장 코드가 매 요청마다 동기적으로 개입 | 가능 | 가능하지만 성능 저하 및 남용 위험 |
| `webRequest` (Manifest V3, 논블로킹) | 관찰(observe)만 가능, 차단/수정 불가 | 가능 | 불가 |
| `declarativeNetRequest` (DNR) | 브라우저에 규칙(rule)을 선언적으로 등록, 브라우저가 직접 매칭·처리 | 확장 코드는 요청 본문을 보지 않음 | 규칙 기반으로 가능 (차단/리다이렉트/헤더 수정) |

---

## 3. declarativeNetRequest 규칙 예시

```json
{
  "id": 1,
  "priority": 1,
  "action": { "type": "block" },
  "condition": {
    "urlFilter": "||ads.example.com^",
    "resourceTypes": ["script", "image", "xmlhttprequest"]
  }
}
```

```javascript
await chrome.declarativeNetRequest.updateDynamicRules({
  addRules: [
    {
      id: 2,
      priority: 1,
      action: {
        type: "redirect",
        redirect: { url: "https://example.com/replacement.js" },
      },
      condition: { urlFilter: "https://cdn.example.com/old.js" },
    },
  ],
  removeRuleIds: [],
});
```

| 규칙 구성 요소 | 설명 |
|----------------|------|
| `condition` | URL 패턴, 리소스 타입, 요청 메서드 등 매칭 조건 |
| `action.type` | `block`, `redirect`, `allow`, `modifyHeaders`, `upgradeScheme` 등 |
| `priority` | 여러 규칙이 겹칠 때 우선순위 |
| 정적 규칙 vs 동적 규칙 | manifest에 JSON 파일로 등록(정적) 또는 런타임에 `updateDynamicRules`로 등록(동적) |

---

## 4. 왜 선언적 모델로 바뀌었나

| 문제 (Manifest V2 블로킹 webRequest) | declarativeNetRequest의 개선 |
|--------------------------------------|-------------------------------|
| 매 요청마다 확장 프로세스로 왕복(IPC) 필요 → 성능 저하 | 브라우저 엔진 내부에서 규칙 매칭, 확장 프로세스 개입 불필요 |
| 확장 코드가 모든 요청의 URL/헤더/본문을 관찰 가능 → 프라이버시 우려 | 규칙만 등록하면 되므로 확장이 사용자 트래픽을 직접 들여다볼 필요가 없음 |
| 광고 차단기 등이 SW 종료와 무관하게 항상 개입해야 함 | 규칙은 SW가 죽어도 브라우저가 계속 적용 |

---

## 5. `fetch`/`XMLHttpRequest`와 CORS

확장 프로그램 자체(백그라운드 SW)에서 발생하는 네트워크 요청은 `host_permissions`에 명시된 출처에 한해 CORS 제약 없이 요청할 수 있다. 반면 Content Script에서 발생하는 요청은 페이지의 CSP/CORS 제약을 그대로 받는다.

| 요청 출처 | CORS 적용 여부 |
|-----------|------------------|
| Service Worker (백그라운드) | `host_permissions` 대상이면 CORS 우회 가능 |
| Content Script | 페이지와 동일한 CORS/CSP 제약 적용 |

---

## 6. 요약

- Manifest V3는 블로킹 `webRequest`를 폐지하고 `declarativeNetRequest` 규칙 기반 모델로 전환했다.
- 규칙은 브라우저가 직접 매칭/처리하므로 성능이 좋고, 확장 코드가 요청 내용을 직접 볼 필요가 없다.
- SW 백그라운드 요청은 `host_permissions` 기반으로 CORS를 우회할 수 있지만, Content Script 요청은 페이지 제약을 그대로 받는다.

---

## 참고 자료

- [Network requests (Chrome 공식)](https://developer.chrome.com/docs/extensions/develop/concepts/network-requests)
- [declarativeNetRequest API (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest)
