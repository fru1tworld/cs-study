# OOPIF와 Site Isolation

## 1. Site Isolation이란

Site Isolation은 Chrome이 서로 다른 사이트(대략 [등록 가능 도메인, registrable domain](https://www.rfc-editor.org/rfc/rfc6454.html) 단위)의 콘텐츠를 각각 별도의 렌더러 프로세스에서 실행하는 보안 아키텍처다. Spectre류 사이드채널 공격이 알려진 이후 강화되었으며, 한 프로세스가 침해당해도 다른 사이트의 메모리(쿠키, 세션 데이터 등)를 직접 읽을 수 없게 하는 것이 목적이다.

```
같은 프로세스 (Site Isolation 이전 가정)
┌─────────────────────────────────┐
│ example.com 페이지                │
│  └─ iframe: ads.example.net     │  ← 같은 프로세스 메모리 공간
└─────────────────────────────────┘

Site Isolation 적용 후
┌───────────────────┐   ┌───────────────────┐
│ example.com        │   │ ads.example.net    │
│ Renderer Process A  │   │ Renderer Process B  │  ← OOPIF: 별도 프로세스
└───────────────────┘   └───────────────────┘
```

## 2. OOPIF (Out-Of-Process iframe)

크로스 사이트 iframe이 부모와 다른 프로세스에서 실행되면 이를 OOPIF라고 부른다. 실행은 분리되지만 브라우저 프로세스가 여러 렌더러에 걸친 프레임 트리를 조합하고 합성(compositing)하므로 사용자에게는 하나의 페이지로 보인다.

- 실행 프로세스
  - Same-Process iframe: 부모와 동일
  - OOPIF: 별도 프로세스
- 발생 조건
  - Same-Process iframe: 같은 사이트 iframe
  - OOPIF: 크로스 사이트 iframe (Site Isolation 대상)
- 부모 JS의 직접 DOM 접근
  - Same-Process iframe: 동일 출처면 가능
  - OOPIF: 애초에 Same-Origin Policy로 차단(동일 출처가 아니므로)
- CDP 관점
  - Same-Process iframe: 단일 `Page`/`DOM` 세션으로 처리
  - OOPIF: 별도 [Target](../cdp/02_target.md)으로 취급, 개별 세션 필요

## 3. 자동화 도구가 겪는 실무 영향

- 부모 페이지의 `DOM.querySelector`로 iframe 내부 요소를 찾을 수 없음
  - 원인: iframe 내부가 다른 렌더러 프로세스(OOPIF)에 있어 부모 DOM 트리에 노출되지 않음
- iframe 내부 클릭이 부모 세션에서 실패
  - 원인: 좌표는 계산할 수 있어도, 실제 이벤트 디스패치는 해당 프레임의 `Input` 세션에서 이뤄져야 함
- `Target.setAutoAttach`가 필요한 이유
  - 원인: 크로스 사이트 iframe이 생성될 때마다 새 타겟이 나타나므로 자동 연결이 없으면 놓침

해결 패턴은 [Target 도메인](../cdp/02_target.md)의 `setAutoAttach`로 새로 생성되는 OOPIF 타겟을 자동 구독하고, 프레임마다 독립적인 `DOM`/`Input` 세션을 유지하는 것이다.

## 4. Site Isolation과 웹 보안 모델의 관계

Site Isolation은 [Same-Origin Policy](https://www.rfc-editor.org/rfc/rfc6454.html)가 이미 막고 있는 "JS 레벨" 접근을 프로세스 경계에서 한 번 더 강제하는 심층 방어(defense in depth)다. JS 레벨 정책만으로는 막지 못하는 하드웨어 사이드채널(Spectre) 공격까지 프로세스 분리로 방어 범위를 넓힌다.

## 참고 자료

- [Target 도메인](../cdp/02_target.md)
- [DOM 도메인](../cdp/04_dom.md)
- [RFC 6454 Web Origin](https://www.rfc-editor.org/rfc/rfc6454.html)
