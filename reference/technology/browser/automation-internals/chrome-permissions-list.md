# Chrome 확장 프로그램: 권한(Permissions) 목록 개요

## 1. 개요

Chrome 확장 프로그램은 `manifest.json`의 `permissions`, `host_permissions`, `optional_permissions` 필드로 필요한 브라우저 API/사이트 접근 권한을 선언한다. 요청한 권한 목록은 스토어 설치 화면과 `chrome://extensions`의 상세 정보에 사용자에게 노출되며, 심사 과정에서도 권한 최소화 원칙(least privilege)을 따르는지 검토된다.

---

## 2. 권한 분류

| 분류 | 예시 | 특징 |
|------|------|------|
| API 권한 (`permissions`) | `"storage"`, `"tabs"`, `"scripting"`, `"debugger"`, `"alarms"`, `"notifications"` | 특정 `chrome.*` 네임스페이스 API 사용 허가 |
| 호스트 권한 (`host_permissions`) | `"https://*.example.com/*"`, `"<all_urls>"` | 지정 출처에 대한 Content Script 실행/네트워크 요청 허가 |
| 선택적 권한 (`optional_permissions`) | 설치 시점이 아니라 런타임에 `chrome.permissions.request()`로 요청 | 필요한 시점에만 사용자 동의를 받아 초기 설치 시 권한 부담을 줄임 |

---

## 3. 민감도가 높은 대표 권한

| 권한 | 위험도 | 이유 |
|------|--------|------|
| `<all_urls>` | 매우 높음 | 모든 사이트의 콘텐츠 읽기/조작 가능 |
| `"debugger"` | 매우 높음 | CDP 전체 접근 → 사실상 브라우저 원격 제어 수준 |
| `"tabs"` | 중간 | 열려 있는 탭의 URL/제목 등 메타데이터 접근 |
| `"webRequest"` (Manifest V2 잔재) | 높음 | 모든 네트워크 요청 관찰 가능 |
| `"clipboardRead"` / `"clipboardWrite"` | 중간~높음 | 클립보드 내용 접근 |
| `"management"` | 높음 | 다른 확장 프로그램의 설치/활성화 상태 제어 |
| `"identity"` | 중간 | OAuth 로그인 플로우 개입 |

---

## 4. 권한 요청 시 심사/사용자 신뢰에 미치는 영향

| 요인 | 설명 |
|------|------|
| 광범위한 `host_permissions` | 스토어 심사 시 "왜 모든 사이트 접근이 필요한지" 소명을 요구받는 경우가 많음 |
| `activeTab` 대안 | 사용자가 명시적으로 아이콘을 클릭한 탭에만 임시 권한을 부여받아, 광범위한 `host_permissions` 없이 동작 가능 ([scripting-api.md](../chrome-extension/scripting-api.md) 참고) |
| 선택적 권한 분리 | 핵심 기능에는 필요 최소 권한만 필수로 요청하고, 부가 기능(고급 설정 등)은 `optional_permissions`로 런타임에 요청 |
| 권한 변경 시 재승인 | 업데이트로 권한이 추가되면 사용자가 다시 승인해야 하는 경우가 있어, 불필요한 권한 확장은 사용자 이탈로 이어짐 |

---

## 5. `chrome.permissions` API로 런타임 요청

```javascript
// 선택적 권한을 필요한 시점에만 요청
document.getElementById("enableAdvanced").addEventListener("click", async () => {
  const granted = await chrome.permissions.request({
    permissions: ["downloads"],
    origins: ["https://files.example.com/*"],
  });
  if (granted) enableDownloadFeature();
});
```

---

## 6. 요약

- 권한은 API 권한/호스트 권한/선택적 권한으로 나뉘며, 최소 권한 원칙이 심사와 사용자 신뢰의 핵심 기준이다.
- `debugger`, `<all_urls>` 같은 고위험 권한은 실제 필요성이 명확해야 승인/신뢰를 받기 쉽다.
- `activeTab`과 선택적 권한 조합으로 초기 요청 권한 범위를 최소화하는 것이 권장 패턴이다.

---

## 참고 자료

- [Chrome Extensions 권한 문서 (공식)](https://developer.chrome.com/docs/extensions/reference/permissions-list)
- [scripting API](../chrome-extension/scripting-api.md)
- [debugger API](../chrome-extension/debugger-api.md)
