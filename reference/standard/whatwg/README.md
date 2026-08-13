# WHATWG (Web Hypertext Application Technology Working Group) 완벽 가이드

---

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [조직 구조와 운영 방식](#3-조직-구조와-운영-방식)
4. [Living Standard 모델](#4-living-standard-모델)
5. [WHATWG가 관리하는 표준 목록](#5-whatwg가-관리하는-표준-목록)
6. [W3C와의 관계](#6-w3c와의-관계)
7. [기여 방법](#7-기여-방법)
8. [장점과 단점](#8-장점과-단점)
9. [개발자에게 미치는 영향](#9-개발자에게-미치는-영향)

---

## 1. 개요

### WHATWG란 무엇인가

WHATWG(Web Hypertext Application Technology Working Group): 웹 표준을 개발·유지보수하는 커뮤니티
- HTML Living Standard, DOM, Fetch, URL 등 웹 플랫폼의 핵심 표준 관리

- 공식 웹사이트: https://whatwg.org/
- 설립일: 2004년 6월 4일
- 설립자: Apple·Mozilla Foundation·Opera Software의 개발자들
- 핵심 인물: Ian Hickson(초대 HTML 에디터)·Anne van Kesteren·Domenic Denicola·Simon Pieters 등
- 운영 형태: 비영리 커뮤니티 그룹(법인 아님)
- 스티어링 그룹: Apple·Google·Mozilla·Microsoft(4대 브라우저 벤더)

### 설립 목적

- HTML의 지속적 발전: W3C가 HTML을 포기하고 XHTML 2.0으로 전환하려던 것에 반발
- 하위 호환성 유지: 기존 웹 콘텐츠를 깨뜨리지 않으면서 점진적으로 웹을 개선
- 실용주의적 표준화: 브라우저 벤더가 실제로 구현할 수 있는 현실적인 표준 개발
- 개방적 참여: 누구나 참여하고 기여할 수 있는 열린 프로세스

### 핵심 철학

WHATWG의 설계 원칙:

- Priority of Constituencies: 사용자 > 저자(개발자) > 구현자(브라우저 벤더) > 스펙 작성자 > 이론적 순수성
- 하위 호환성: 기존 웹 콘텐츠를 깨뜨리지 않음
- 실용주의: 이론적으로 옳은 것보다 실제로 작동하는 것을 우선
- Living Standard: 버전이 없는 지속적 업데이트 모델
- 구현 기반: 최소 1개 이상의 브라우저가 구현하거나 구현할 의향이 있어야 표준에 포함

> Priority of Constituencies는 WHATWG의 가장 중요한 원칙. 의사결정에서 충돌이 발생할 때 최종 사용자의 이익이 다른 모든 이해관계자보다 우선

---

## 2. 역사

### WHATWG 탄생의 배경

#### 2000년대 초반의 웹 표준 상황

2000년대 초반, W3C는 웹의 미래를 XHTML 2.0에 걸었음. XHTML 2.0의 특징:

- XML의 엄격한 문법을 강제(태그 하나라도 잘못되면 페이지 전체가 렌더링되지 않음)
- HTML 4.01과의 하위 호환성 포기
- 웹 애플리케이션을 위한 기능(폼, 동적 콘텐츠 등) 부족
- XForms라는 별도의 복잡한 폼 모델 도입

→ 웹의 현실과 완전히 동떨어진 접근. 당시 웹 페이지의 99%는 문법 오류를 포함 → 브라우저가 이러한 오류를 관대하게(lenient) 처리하는 것이 핵심 역할

#### 2004년 W3C Workshop

2004년 6월, W3C가 "Workshop on Web Applications and Compound Documents" 개최:

- Opera의 Ian Hickson과 Mozilla의 Brendan Eich 등이 "기존 HTML을 점진적으로 확장하여 웹 애플리케이션 기능을 추가하자"는 제안 발표
- W3C가 투표로 부결(찬성 8, 반대 14)
- W3C가 XHTML 2.0 노선을 고수하기로 결정

→ 이 결정에 실망한 Apple·Mozilla·Opera의 개발자들이 독자적으로 WHATWG 결성

### 주요 연혁

- 2004년 6월: WHATWG 결성. Ian Hickson을 에디터로 "Web Applications 1.0"(후에 HTML5로 명명) 작업 시작
- 2004년 9월: Web Forms 2.0 초안 발표(HTML 폼의 확장)
- 2006년 10월: 팀 버너스리가 W3C 블로그에서 "HTML을 포기한 것은 실수였다"고 인정
- 2007년 3월: W3C가 HTML5 Working Group을 설립, WHATWG의 작업을 기반으로 공동 작업 시작
- 2008년 1월: HTML5 First Public Working Draft(W3C)
- 2009년 7월: W3C가 XHTML 2.0 Working Group을 공식 해산
- 2011년 1월: WHATWG가 "HTML5"라는 이름을 버리고 "HTML Living Standard"로 전환
- 2012년: W3C와 WHATWG의 스펙 분기 심화. 두 개의 HTML 표준이 병존하는 혼란
- 2014년 10월: W3C HTML5 Recommendation 발표(스냅샷 버전)
- 2017년: WHATWG가 W3C Patent Policy를 채택(지적재산권 보호 체계 도입)
- 2017년 12월: WHATWG Steering Group 공식 결성(Apple, Google, Mozilla, Microsoft)
- 2019년 5월: W3C와 WHATWG가 양해각서(MoU) 체결. HTML과 DOM은 WHATWG Living Standard가 유일한 표준이 됨
- 2019년 이후: Fetch, URL, Notifications 등 다양한 Living Standard 지속 개발

### Ian Hickson의 역할

Ian "Hixie" Hickson: WHATWG의 핵심 인물

- Opera에서 근무하다가 이후 Google로 이적
- WHATWG 설립 시부터 HTML 스펙의 단독 에디터로 활동
- 한 사람이 거대한 HTML 스펙 전체를 편집하는 독특한 방식
- 2015년 HTML 에디터에서 물러남(현재는 여러 명의 에디터가 분담)
- Acid2·Acid3 테스트의 저자

---

## 3. 조직 구조와 운영 방식

### 거버넌스 구조

```
┌─────────────────────────────────────────────────┐
│              WHATWG Steering Group               │
│   (Apple, Google, Mozilla, Microsoft)            │
│                                                  │
│   - 최고 의사결정 기구                              │
│   - 정책 결정, 분쟁 해결, 신규 표준 승인             │
│   - 각 브라우저 벤더에서 대표자 파견                  │
└─────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│              Workstream Editors                   │
│                                                  │
│   각 Living Standard의 편집 담당자                  │
│   - HTML: Simon Pieters                           │
│   - DOM: Anne van Kesteren 등                     │
│   - Fetch: Anne van Kesteren 등                   │
│   - URL: Anne van Kesteren 등                     │
└─────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────┐
│              Contributors                         │
│                                                  │
│   - 누구나 GitHub를 통해 기여 가능                   │
│   - Issue 제출, Pull Request, 리뷰                 │
│   - 기여 시 IPR(지적재산권) 동의 필요                │
└─────────────────────────────────────────────────┘
```

### Steering Group

WHATWG의 최고 의사결정 기구. 4대 브라우저 벤더로 구성:

- Apple: 브라우저 Safari, 엔진 WebKit
- Google: 브라우저 Chrome, 엔진 Blink
- Mozilla: 브라우저 Firefox, 엔진 Gecko
- Microsoft: 브라우저 Edge, 엔진 Blink(2020~)

Steering Group의 역할:
- 새로운 Workstream(표준) 승인 또는 폐기
- 에디터 임명 및 해임
- 기여 정책 및 라이선스 정책 결정
- Working Mode(작업 방식) 규정
- 분쟁 발생 시 최종 조정

### 에디터(Editor) 시스템

WHATWG는 에디터 주도(editor-driven) 방식으로 운영:

- 각 Living Standard에 1명 이상의 에디터 배정
- 에디터가 스펙의 내용을 최종 결정(합의가 안 될 경우)
- 에디터는 Steering Group에 의해 임명
- 에디터는 피드백을 수집하고 반영하되 최종 판단권을 가짐

→ W3C의 합의(consensus) 기반 의사결정과 대비되는 특징

### Contributor License Agreement (CLA)

WHATWG에 기여하려면 Participant Agreement 서명 필요:

- 개인 기여자: Individual Contributor License Agreement
- 기업 기여자: Entity Contributor License Agreement
- 지적재산권(IPR) 관련 보호 장치
- W3C Patent Policy를 기반으로 한 Royalty-Free 라이선스

---

## 4. Living Standard 모델

### Living Standard란

Living Standard: WHATWG가 채택한 표준 문서 관리 모델. 전통적인 버전 기반 표준(W3C Recommendation Track)과 근본적으로 다른 접근

### 버전 기반 vs Living Standard

- 버전
  - 버전 기반(W3C 방식): 명시적 버전 번호(HTML 5.0, 5.1, 5.2)
  - Living Standard(WHATWG 방식): 버전 없음(항상 최신)
- 업데이트
  - 버전 기반: 수년 주기의 스냅샷 발행
  - Living Standard: 지속적 업데이트(커밋 단위)
- 안정성
  - 버전 기반: 특정 시점에서 "확정"됨
  - Living Standard: 항상 변할 수 있음(안정화된 부분과 실험적 부분 공존)
- 구현과의 관계
  - 버전 기반: 표준 확정 → 구현
  - Living Standard: 구현과 표준이 동시에 발전
- 오류 수정
  - 버전 기반: 정오표(Errata) 또는 새 버전
  - Living Standard: 즉시 수정
- 새 기능
  - 버전 기반: 새 버전에서 추가
  - Living Standard: 언제든 추가 가능
- 개발 기간
  - 버전 기반: FPWD → REC까지 수년~10년
  - Living Standard: 지속적(종료 시점 없음)

### Living Standard의 장점

- 최신성: 항상 최신 상태의 스펙 참조 가능
- 오류 수정 속도: 오류 발견 시 즉시 수정 가능(정오표 절차 불필요)
- 구현과의 동기화: 브라우저 구현과 표준이 함께 발전
- 유연성: 새로운 기능 추가 시 버전 출시를 기다릴 필요 없음
- 단일 참조점: "어떤 버전을 봐야 하나?"라는 혼란 제거

### Living Standard의 표기

각 Living Standard 문서의 표기:

```
Living Standard — Last Updated [날짜]
```

- 특정 커밋의 스냅샷을 참조할 수 있는 영구 URL 제공
- GitHub 커밋 히스토리를 통해 모든 변경 사항 추적 가능
- Review Draft(리뷰 초안)가 주기적으로 발행되어 특허 정책의 기준점 역할

### Review Draft

Living Standard 모델에서도 특허 정책(Patent Policy)을 위해 주기적으로 Review Draft 발행:

- 6개월마다 자동으로 스냅샷 생성
- Steering Group 멤버들에게 45일간의 특허 검토 기간(Exclusion Period) 부여
- 이 기간 동안 특허 관련 이의 제기 가능
- Review Draft가 Living Standard의 법적 기준점 역할

---

## 5. WHATWG가 관리하는 표준 목록

### 전체 목록

WHATWG가 현재 관리하는 Living Standard:

- [HTML](html.md): 웹의 핵심 마크업 언어 및 API · 주요 에디터 Simon Pieters · 규모 매우 큼
- [DOM](dom.md): 문서 객체 모델 · 주요 에디터 Anne van Kesteren · 규모 큼
- [Fetch](fetch.md): 네트워크 요청 및 응답 · 주요 에디터 Anne van Kesteren · 규모 중간
- [URL](url.md): URL 파싱 및 직렬화 · 주요 에디터 Anne van Kesteren · 규모 중간
- [Streams](streams.md): 스트림 API · 주요 에디터 Mattias Buelens · 규모 중간
- [Encoding](encoding.md): 문자 인코딩 · 주요 에디터 Anne van Kesteren · 규모 중간
- [Console](console.md): 콘솔 API · 주요 에디터 Dominic Farolino, Robert Kowalski, Terin Stock · 규모 작음
- [XMLHttpRequest](xhr.md): XHR API · 주요 에디터 Anne van Kesteren · 규모 중간
- [Compatibility](compatibility.md): 웹 호환성 · 주요 에디터 Mike Taylor · 규모 작음
- [Notifications API](notifications.md): 알림 API · 주요 에디터 Anne van Kesteren · 규모 작음
- [Fullscreen API](fullscreen.md): 전체 화면 API · 주요 에디터 Philip Jägenstedt · 규모 작음
- [Storage](storage.md): 스토리지 관리 · 주요 에디터 Anne van Kesteren · 규모 작음
- [Quirks Mode](quirks.md): 쿼크 모드 동작 · 주요 에디터 Simon Pieters · 규모 작음
- [MIME Sniffing](mime-sniffing.md): MIME 타입 감지 · 주요 에디터 Gordon P. Hemsley · 규모 중간
- [Infra](infra.md): 기반 자료 구조 및 알고리즘 · 주요 에디터 Anne van Kesteren · 규모 중간

### 표준 간의 관계도

```
                    ┌──────────┐
                    │  Infra   │  ← 모든 표준의 기반
                    └────┬─────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌─────▼─────┐   ┌─────▼─────┐
    │   URL   │    │ Encoding  │   │  Streams  │
    └────┬────┘    └─────┬─────┘   └─────┬─────┘
         │               │               │
    ┌────▼────────────────▼───────────────▼────┐
    │                  Fetch                     │
    └────┬──────────────────────────────────────┘
         │
    ┌────▼────────────────────────────────┐
    │              DOM                     │
    └────┬────────────────────────────────┘
         │
    ┌────▼────────────────────────────────┐
    │              HTML                    │
    │  (XHR, Console, Notifications,      │
    │   Fullscreen, Storage 등 참조)       │
    └─────────────────────────────────────┘
```

- Infra: 다른 모든 WHATWG 표준이 사용하는 기본 자료 구조와 알고리즘 정의
- URL, Encoding: Fetch와 HTML에서 사용하는 기반 기술
- Fetch: 네트워크 요청의 핵심, HTML에서 리소스 로딩에 사용
- DOM: HTML의 프로그래밍 인터페이스
- HTML: 웹 플랫폼의 중심, 다른 대부분의 표준을 참조

---

## 6. W3C와의 관계

### 갈등에서 협력으로

#### 갈등 시기 (2004-2018)

```
2004: WHATWG 독자 설립
       ↓
2007: W3C도 HTML5 작업 시작 (WHATWG 기반)
       ↓
2011: WHATWG가 Living Standard 모델 전환 → 분기 시작
       ↓
2012-2018: 두 개의 "HTML 표준"이 병존
  - W3C: HTML 5.0, 5.1, 5.2 (스냅샷)
  - WHATWG: HTML Living Standard (지속 업데이트)
  → 개발자 혼란: "어느 것이 진짜 HTML 표준인가?"
```

#### 합의 (2019)

2019년 5월, W3C와 WHATWG가 양해각서(Memorandum of Understanding, MoU) 체결:

- HTML: WHATWG HTML Living Standard가 유일한 HTML 표준
- DOM: WHATWG DOM Living Standard가 유일한 DOM 표준
- W3C의 역할: 독자적 HTML/DOM 스냅샷 발행 중단, WHATWG 프로세스에 리뷰어로 참여
- 특허 보호: WHATWG Review Draft가 W3C Recommendation과 동등한 특허 보호 적용
- 참여: W3C 회원은 WHATWG 프로세스에 참여 가능

#### 합의의 의미

- 실용주의의 승리: 브라우저 벤더가 실제로 구현하는 스펙이 표준이 됨
- Living Standard 인정: 버전 기반 스냅샷보다 지속적 업데이트가 더 적합함을 W3C가 인정
- 혼란 해소: "어느 HTML 표준을 봐야 하나?"라는 질문에 대한 답이 명확해짐
- 역할 분담: W3C는 HTML/DOM 이외의 표준(CSS, WebAssembly, WCAG 등)에 집중

### 현재의 역할 분담

- HTML, DOM, Fetch, URL 등: WHATWG
- CSS, WebAssembly, WCAG, SVG 등: W3C
- HTTP, TLS, WebSocket 프로토콜 등: IETF
- ECMAScript(JavaScript 언어): Ecma International(TC39)

---

## 7. 기여 방법

### GitHub 기반 작업

WHATWG의 모든 표준은 GitHub에서 관리:

- HTML: https://github.com/whatwg/html
- DOM: https://github.com/whatwg/dom
- Fetch: https://github.com/whatwg/fetch
- URL: https://github.com/whatwg/url
- Streams: https://github.com/whatwg/streams
- 기타: https://github.com/whatwg/

### 기여 방식

- Issue 제출: 스펙의 오류나 개선 사항 보고
- Pull Request: 스펙 텍스트를 직접 수정하여 제출
- Issue 토론 참여: 기존 이슈에 의견 추가
- 테스트 작성: web-platform-tests에 테스트 추가
- 구현 피드백: 브라우저 구현 경험을 바탕으로 피드백

### 기여 시 유의사항

- Participant Agreement 서명 필요(IPR 보호)
- FAQ 먼저 확인: 자주 묻는 질문은 이미 답변이 있을 수 있음
- 에디터의 결정 존중: 에디터가 최종 판단권을 가짐
- 구현 가능성: 제안이 실제로 브라우저에 구현 가능한지 고려

---

## 8. 장점과 단점

### 장점

#### 1. 빠른 표준 발전
- Living Standard 모델로 신속한 업데이트 가능
- 브라우저 구현과 표준이 동시에 발전
- W3C의 수년간 걸리는 프로세스 대비 훨씬 빠름

#### 2. 실용주의적 접근
- 이론보다 현실을 중시
- 실제 웹 호환성 데이터를 기반으로 의사결정
- 브라우저 벤더의 구현 의향을 확인한 후 표준에 포함

#### 3. 개방적 참여
- GitHub를 통해 누구나 참여 가능
- W3C처럼 회비를 낼 필요 없음
- 투명한 의사결정 과정(모든 논의가 공개)

#### 4. 하위 호환성 중시
- 기존 웹을 깨뜨리지 않는 것을 최우선
- 에러 처리(error recovery) 알고리즘을 상세히 정의
- 레거시 콘텐츠도 올바르게 동작하도록 보장

### 단점

#### 1. 브라우저 벤더 지배
- Steering Group이 4대 브라우저 벤더로만 구성
- 소규모 브라우저나 비브라우저 이해관계자의 발언권 제한
- "벤더의, 벤더에 의한, 벤더를 위한" 표준이라는 비판

#### 2. 에디터 권한 집중
- 에디터의 개인적 판단이 표준에 큰 영향
- 합의(consensus)보다 에디터의 결정이 우선될 수 있음
- 에디터 교체 시 방향이 바뀔 수 있는 리스크

#### 3. 안정성 부족
- Living Standard는 언제든 변할 수 있어 특정 시점의 "확정된 표준"이 없음
- 법적/규제적 목적으로 참조하기 어려움(어떤 시점의 표준을 참조해야 하는가)
- Review Draft로 일부 보완되었으나 근본적 한계

#### 4. 범위 제한
- HTML, DOM, Fetch 등 "웹 플랫폼 핵심"에만 집중
- CSS, 접근성(WCAG), WebAssembly 등은 W3C가 관리
- 웹 표준 전체를 포괄하지는 않음

---

## 9. 개발자에게 미치는 영향

### 어디를 참조해야 하는가

- HTML 문법, 요소, API: WHATWG HTML Living Standard(https://html.spec.whatwg.org/)
- DOM API: WHATWG DOM Living Standard(https://dom.spec.whatwg.org/)
- Fetch API: WHATWG Fetch Standard(https://fetch.spec.whatwg.org/)
- URL 파싱: WHATWG URL Standard(https://url.spec.whatwg.org/)
- CSS: W3C CSS Specifications
- JavaScript 언어: ECMAScript(TC39)
- HTTP 프로토콜: IETF RFC
- 접근성: W3C WCAG

### 개발자가 알아야 할 핵심

- HTML Living Standard가 유일한 HTML 표준 → "HTML5"라는 버전은 더 이상 의미가 없음
- MDN Web Docs는 WHATWG 스펙을 기반으로 작성됨 → MDN은 훌륭한 2차 자료이지만 원본은 WHATWG 스펙
- 브라우저 호환성은 Can I Use에서 확인 → WHATWG 스펙에 있다고 모든 브라우저가 구현한 것은 아님
- 스펙을 직접 읽는 능력을 키울 것 → 엣지 케이스에서 정확한 동작을 파악하려면 스펙이 유일한 정답

### 각 표준 상세 문서

각 Living Standard의 상세 내용은 개별 문서 참조:

- [HTML Living Standard 상세](html.md)
- [DOM Living Standard 상세](dom.md)
- [Fetch Standard 상세](fetch.md)
- [URL Standard 상세](url.md)
- [Streams Standard 상세](streams.md)
- [Encoding Standard 상세](encoding.md)
- [Console Standard 상세](console.md)
- [XMLHttpRequest Standard 상세](xhr.md)
- [Compatibility Standard 상세](compatibility.md)
- [Notifications API Standard 상세](notifications.md)
- [Fullscreen API Standard 상세](fullscreen.md)
- [Storage Standard 상세](storage.md)
- [Quirks Mode Standard 상세](quirks.md)
- [MIME Sniffing Standard 상세](mime-sniffing.md)
- [Infra Standard 상세](infra.md)

---

## 참고 자료

- WHATWG 공식 사이트: https://whatwg.org/
- WHATWG FAQ: https://whatwg.org/faq
- WHATWG Working Mode: https://whatwg.org/working-mode
- WHATWG GitHub: https://github.com/whatwg
- W3C/WHATWG 양해각서 (MoU): https://www.w3.org/2019/04/WHATWG-W3C-MOU.html
- HTML Living Standard: https://html.spec.whatwg.org/
- DOM Living Standard: https://dom.spec.whatwg.org/
- Fetch Standard: https://fetch.spec.whatwg.org/
- URL Standard: https://url.spec.whatwg.org/
- MDN Web Docs: https://developer.mozilla.org/
- Can I Use: https://caniuse.com/
