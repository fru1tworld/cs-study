# Chrome: WebMCP Origin Trial

> Chrome 공식 블로그: https://developer.chrome.com/blog/ai-webmcp-origin-trial

## 1. 개요

Chrome은 [WebMCP](../../../standard/w3c/webmcp.md) 개념을 정식 표준화하기 전, 제한된 범위에서 실제 사이트에 배포해 API 설계를 검증하기 위해 Origin Trial(오리진 트라이얼) 프로그램을 운영한다. Origin Trial은 Chrome이 실험적 웹 플랫폼 기능을 등록한 출처(origin)에서만 활성화해볼 수 있게 해주는 표준 절차로, 정식 API가 되기 전 실사용 피드백을 수집하는 단계다.

---

## 2. Origin Trial 일반 구조

| 단계 | 설명 |
|------|------|
| 신청 | 개발자가 Chrome Origin Trials 사이트에서 특정 기능+출처 조합으로 토큰 발급 신청 |
| 토큰 발급 | 신청한 출처에서만 유효한 토큰(문자열)을 받음 |
| 활성화 | HTML `<meta>` 태그 또는 HTTP 헤더로 토큰을 페이지에 포함하면 해당 출처에서 실험 기능이 활성화됨 |
| 피드백 수집 기간 | 보통 몇 개월 단위로 운영되며, 이 기간 API 형태가 바뀔 수 있음 |
| 종료 | 정식 표준화되어 일반 API로 전환되거나, 피드백 반영 후 재설계되어 다음 트라이얼로 이어지거나, 기능이 폐기됨 |

```html
<meta http-equiv="origin-trial" content="<발급받은 토큰>">
```

---

## 3. WebMCP Origin Trial의 목적

| 검증하려는 것 | 설명 |
|----------------|------|
| API 인체공학(ergonomics) | `navigator.modelContext.registerTool()` 같은 API 형태가 실제 개발자에게 사용하기 편한지 |
| 보안/권한 모델 | 에이전트가 도구를 호출하기 전 사용자 동의를 어떤 방식으로 받아야 하는지 |
| 성능 영향 | 도구 등록/호출이 페이지 성능에 미치는 영향 |
| 실제 사이트 통합 사례 | 전자상거래, SaaS 대시보드 등 다양한 사이트 유형에서의 실사용 패턴 |

---

## 4. 개발자가 알아야 할 실무 포인트

| 항목 | 설명 |
|------|------|
| API 불안정성 | 정식 표준이 아니므로 프로덕션 필수 기능으로 의존하면 안 됨 |
| 브라우저 종속성 | 현재는 Chrome(Chromium 계열)에서만 시험 중이며, 타 브라우저 지원 여부는 미정 |
| 만료 관리 | Origin Trial 토큰에는 만료일이 있어 트라이얼이 연장/종료될 때 갱신이 필요 |
| 폴백 설계 | 트라이얼 미참여 브라우저에서는 기존 DOM 기반 상호작용으로 폴백해야 함 |

---

## 5. 요약

- WebMCP Origin Trial은 WebMCP를 정식 표준화하기 전 Chrome이 제한된 출처에서 실사용 피드백을 모으는 실험 단계다.
- 참여하려면 출처별 토큰을 발급받아 `<meta>` 태그 또는 헤더로 활성화해야 한다.
- API 형태는 트라이얼 기간 동안 계속 바뀔 수 있으므로 프로덕션 의존은 지양해야 한다.

---

## 참고 자료

- [AI/WebMCP Origin Trial 발표 (Chrome 공식 블로그)](https://developer.chrome.com/blog/ai-webmcp-origin-trial)
- [WebMCP Explainer](../../../standard/w3c/webmcp.md)
- [Chrome Origin Trials 안내](https://developer.chrome.com/docs/web-platform/origin-trials)
