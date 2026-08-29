# Chrome 확장 프로그램: sidePanel API와 WebMCP Origin Trial

## Chrome 확장 프로그램: chrome.sidePanel API

> 공식 문서: https://developer.chrome.com/docs/extensions/reference/api/sidePanel

### 1. 개요

`chrome.sidePanel`은 확장 프로그램이 브라우저 창 옆에 지속적으로 표시되는 사이드 패널 UI를 제공할 수 있게 하는 API임. 팝업(`action.default_popup`)이 클릭할 때마다 열리고 다른 곳을 클릭하면 즉시 닫히는 것과 달리, 사이드 패널은 사용자가 페이지를 탐색하는 동안에도 계속 열려 있을 수 있어 챗봇, 노트, AI 어시스턴트형 확장 프로그램에 적합함.

Popup vs Side Panel:

- 표시 방식
  - Popup: 확장 아이콘 클릭 시 임시로 표시
  - Side Panel: 별도 패널로 열리고 지속 유지
- 포커스 이동 시
  - Popup: 자동으로 닫힘
  - Side Panel: 유지됨
- 탭 이동 시
  - Popup: 관계없음(팝업은 매번 새로 생성)
  - Side Panel: 탭별로 다른 패널을 보여주도록 구성 가능
- 용도
  - Popup: 빠른 설정/상태 확인
  - Side Panel: 챗봇, 다단계 작업, 지속적 참고 UI

---

### 2. 기본 설정

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

### 3. 탭별 사이드 패널 구성

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

특정 사이트에서만 사이드 패널을 활성화하거나, 탭마다 다른 콘텐츠를 보여주는 것이 가능함.

---

### 4. 사용자 제스처 제약

사이드 패널을 여는 것은 대부분 사용자 활성화(user gesture) 컨텍스트 안에서만 허용됨. 예를 들어 `chrome.sidePanel.open()`을 임의 시점에 호출하면 실패하며, 확장 아이콘 클릭이나 컨텍스트 메뉴 클릭 같은 명시적 사용자 동작 안에서 호출해야 함. 사용자가 원치 않는 UI가 갑자기 열리는 것을 막기 위한 보안 장치임.

---

### 5. Manifest V2 대비 위치

Manifest V2에는 사이드 패널이라는 공식 개념이 없었고, 확장 프로그램들이 자체적으로 iframe이나 새 창을 흉내 내어 구현함. `chrome.sidePanel`은 이를 브라우저 네이티브 UI 영역으로 표준화한 것임.

---

### 6. 요약

- 사이드 패널은 팝업과 달리 탐색 중에도 지속적으로 열려 있는 확장 UI 영역임.
- `setPanelBehavior`, `setOptions`로 열림 동작과 탭별 콘텐츠를 제어함.
- 사용자 제스처 컨텍스트에서만 프로그래밍 방식으로 열 수 있음.
- AI 어시스턴트, 챗봇형 확장 프로그램의 표준 UI 자리로 자리 잡고 있음.

---

### 참고 자료

- [sidePanel API (Chrome 공식)](https://developer.chrome.com/docs/extensions/reference/api/sidePanel)

---

## Chrome: WebMCP Origin Trial

> Chrome 공식 블로그: https://developer.chrome.com/blog/ai-webmcp-origin-trial

### 1. 개요

Chrome은 [WebMCP](../../../../standard/w3c/webmcp.md) 개념을 정식 표준화하기 전, 제한된 범위에서 실제 사이트에 배포해 API 설계를 검증하기 위해 Origin Trial(오리진 트라이얼) 프로그램을 운영함. Origin Trial은 Chrome이 실험적 웹 플랫폼 기능을 등록한 출처(origin)에서만 활성화해볼 수 있게 해주는 표준 절차로, 정식 API가 되기 전 실사용 피드백을 수집하는 단계임.

---

### 2. Origin Trial 일반 구조

- 신청: 개발자가 Chrome Origin Trials 사이트에서 특정 기능+출처 조합으로 토큰 발급 신청
- 토큰 발급: 신청한 출처에서만 유효한 토큰(문자열)을 받음
- 활성화: HTML `<meta>` 태그 또는 HTTP 헤더로 토큰을 페이지에 포함하면 해당 출처에서 실험 기능이 활성화됨
- 피드백 수집 기간: 보통 몇 개월 단위로 운영되며, 이 기간 API 형태가 바뀔 수 있음
- 종료: 정식 표준화되어 일반 API로 전환되거나, 피드백 반영 후 재설계되어 다음 트라이얼로 이어지거나, 기능이 폐기됨

```html
<meta http-equiv="origin-trial" content="<발급받은 토큰>">
```

---

### 3. WebMCP Origin Trial의 목적

- API 인체공학(ergonomics): `navigator.modelContext.registerTool()` 같은 API 형태가 실제 개발자에게 사용하기 편한지 검증
- 보안/권한 모델: 에이전트가 도구를 호출하기 전 사용자 동의를 어떤 방식으로 받아야 하는지 검증
- 성능 영향: 도구 등록/호출이 페이지 성능에 미치는 영향 검증
- 실제 사이트 통합 사례: 전자상거래, SaaS 대시보드 등 다양한 사이트 유형에서의 실사용 패턴 검증

---

### 4. 개발자가 알아야 할 실무 포인트

- API 불안정성: 정식 표준이 아니므로 프로덕션 필수 기능으로 의존하면 안 됨
- 브라우저 종속성: 현재는 Chrome(Chromium 계열)에서만 시험 중이며, 타 브라우저 지원 여부는 미정
- 만료 관리: Origin Trial 토큰에는 만료일이 있어 트라이얼이 연장/종료될 때 갱신이 필요
- 폴백 설계: 트라이얼 미참여 브라우저에서는 기존 DOM 기반 상호작용으로 폴백해야 함

---

### 5. 요약

- WebMCP Origin Trial은 WebMCP를 정식 표준화하기 전 Chrome이 제한된 출처에서 실사용 피드백을 모으는 실험 단계임.
- 참여하려면 출처별 토큰을 발급받아 `<meta>` 태그 또는 헤더로 활성화해야 함.
- API 형태는 트라이얼 기간 동안 계속 바뀔 수 있으므로 프로덕션 의존은 지양해야 함.

---

### 참고 자료

- [AI/WebMCP Origin Trial 발표 (Chrome 공식 블로그)](https://developer.chrome.com/blog/ai-webmcp-origin-trial)
- [WebMCP Explainer](../../../../standard/w3c/webmcp.md)
- [Chrome Origin Trials 안내](https://developer.chrome.com/docs/web-platform/origin-trials)
