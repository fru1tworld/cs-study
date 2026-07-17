# Chrome 확장 프로그램: chrome.sidePanel API

> 공식 문서: https://developer.chrome.com/docs/extensions/reference/api/sidePanel

## 1. 개요

`chrome.sidePanel`은 확장 프로그램이 브라우저 창 옆에 지속적으로 표시되는 사이드 패널 UI를 제공할 수 있게 하는 API다. 팝업(`action.default_popup`)이 클릭할 때마다 열리고 다른 곳을 클릭하면 즉시 닫히는 것과 달리, 사이드 패널은 사용자가 페이지를 탐색하는 동안에도 계속 열려 있을 수 있어 챗봇, 노트, AI 어시스턴트형 확장 프로그램에 적합하다.

| 구분 | Popup | Side Panel |
|------|-------|-----------|
| 표시 방식 | 확장 아이콘 클릭 시 임시로 표시 | 별도 패널로 열리고 지속 유지 |
| 포커스 이동 시 | 자동으로 닫힘 | 유지됨 |
| 탭 이동 시 | 관계없음(팝업은 매번 새로 생성) | 탭별로 다른 패널을 보여주도록 구성 가능 |
| 용도 | 빠른 설정/상태 확인 | 챗봇, 다단계 작업, 지속적 참고 UI |

---

## 2. 기본 설정

```json
{
  "manifest_version": 3,
  "permissions": ["sidePanel"],
  "side_panel": {
    "default_path": "sidepanel.html"
  }
}
```

```javascript
// 확장 아이콘 클릭 시 사이드 패널이 열리도록 설정
chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
```

---

## 3. 탭별 사이드 패널 구성

```javascript
chrome.tabs.onUpdated.addListener(async (tabId, info, tab) => {
  if (!tab.url) return;
  const url = new URL(tab.url);

  if (url.hostname === "example.com") {
    await chrome.sidePanel.setOptions({
      tabId,
      path: "panels/example.html",
      enabled: true,
    });
  } else {
    await chrome.sidePanel.setOptions({ tabId, enabled: false });
  }
});
```

특정 사이트에서만 사이드 패널을 활성화하거나, 탭마다 다른 콘텐츠를 보여주는 것이 가능하다.

---

## 4. 사용자 제스처 제약

사이드 패널을 여는 것은 대부분 사용자 활성화(user gesture) 컨텍스트 안에서만 허용된다. 예를 들어 `chrome.sidePanel.open()`을 임의 시점에 호출하면 실패하며, 확장 아이콘 클릭이나 컨텍스트 메뉴 클릭 같은 명시적 사용자 동작 안에서 호출해야 한다. 이는 사용자가 원치 않는 UI가 갑자기 열리는 것을 막기 위한 보안 장치다.

---

## 5. Manifest V2 대비 위치

Manifest V2에는 사이드 패널이라는 공식 개념이 없었고, 확장 프로그램들이 자체적으로 iframe이나 새 창을 흉내 내어 구현했다. `chrome.sidePanel`은 이를 브라우저 네이티브 UI 영역으로 표준화한 것이다.

---

## 6. 요약

- 사이드 패널은 팝업과 달리 탐색 중에도 지속적으로 열려 있는 확장 UI 영역이다.
- `setPanelBehavior`, `setOptions`로 열림 동작과 탭별 콘텐츠를 제어한다.
- 사용자 제스처 컨텍스트에서만 프로그래밍 방식으로 열 수 있다.
- AI 어시스턴트, 챗봇형 확장 프로그램의 표준 UI 자리로 자리 잡고 있다.

---

## 참고 자료

- [sidePanel API (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/sidePanel)
