# OOPIF와 Site Isolation

## 1. Site Isolation이란

Site Isolation은 Chrome이 서로 다른 사이트(대략 [등록 가능 도메인, registrable domain](../../../standard/RFC/Layer7-Application/HTTP/RFC6454-Web-Origin.md) 단위)의 콘텐츠를 각각 별도의 렌더러 프로세스에서 실행하는 보안 아키텍처다. Spectre류 사이드채널 공격이 알려진 이후 강화되었으며, 한 프로세스가 침해당해도 다른 사이트의 메모리(쿠키, 세션 데이터 등)를 직접 읽을 수 없게 하는 것이 목적이다.

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

---

## 2. OOPIF (Out-Of-Process iframe)

크로스 사이트 iframe이 부모와 다른 프로세스에서 실행될 때 이를 OOPIF라 부른다. 브라우저 프로세스가 여러 렌더러 프로세스에 걸친 프레임 트리를 조합해 하나의 페이지처럼 보이게 합성(compositing)한다.

| 항목 | Same-Process iframe | OOPIF |
|------|----------------------|--------|
| 실행 프로세스 | 부모와 동일 | 별도 프로세스 |
| 발생 조건 | 같은 사이트 iframe | 크로스 사이트 iframe (Site Isolation 대상) |
| 부모 JS의 직접 DOM 접근 | 동일 출처면 가능 | 애초에 Same-Origin Policy로 차단(동일 출처가 아니므로) |
| CDP 관점 | 단일 `Page`/`DOM` 세션으로 처리 | 별도 [Target](../cdp/target.md)으로 취급, 개별 세션 필요 |

---

## 3. 자동화 도구가 겪는 실무 영향

| 증상 | 원인 |
|------|------|
| 부모 페이지의 `DOM.querySelector`로 iframe 내부 요소를 찾을 수 없음 | iframe 내부가 다른 렌더러 프로세스(OOPIF)에 있어 부모 DOM 트리에 노출되지 않음 |
| iframe 내부 클릭이 부모 세션에서 실패 | 좌표는 계산할 수 있어도, 실제 이벤트 디스패치는 해당 프레임의 `Input` 세션에서 이뤄져야 함 |
| `Target.setAutoAttach`가 필요한 이유 | 크로스 사이트 iframe이 생성될 때마다 새 타겟이 나타나므로 자동 연결이 없으면 놓침 |

해결 패턴은 [Target 도메인](../cdp/target.md)의 `setAutoAttach`로 새로 생성되는 OOPIF 타겟을 자동 구독하고, 각 프레임마다 독립적인 `DOM`/`Input` 세션을 유지하는 것이다.

---

## 4. Site Isolation과 웹 보안 모델의 관계

Site Isolation은 [Same-Origin Policy](../../../standard/RFC/Layer7-Application/HTTP/RFC6454-Web-Origin.md)가 이미 막고 있는 "JS 레벨" 접근을 프로세스 경계에서 한 번 더 강제하는 심층 방어(defense in depth)다. JS 레벨 정책만으로는 막지 못하는 하드웨어 사이드채널(Spectre) 공격까지 프로세스 분리로 방어 범위를 넓힌다.

---

## 5. 요약

- Site Isolation은 크로스 사이트 콘텐츠를 별도 렌더러 프로세스로 분리하는 Chrome의 심층 방어 아키텍처다.
- 그 결과물인 OOPIF는 CDP 관점에서 부모와 다른 `Target`이 되어 별도 세션이 필요하다.
- 자동화 도구는 `Target.setAutoAttach`로 OOPIF 생성을 감지하고 프레임별 세션을 관리해야 한다.

---

## 참고 자료

- [Target 도메인](../cdp/target.md)
- [DOM 도메인](../cdp/dom.md)
- [RFC 6454 Web Origin](../../../standard/RFC/Layer7-Application/HTTP/RFC6454-Web-Origin.md)
