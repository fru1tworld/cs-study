# W3C (World Wide Web Consortium) 완벽 가이드

---

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [조직 구조](#3-조직-구조)
4. [표준화 프로세스](#4-표준화-프로세스)
5. [주요 웹 표준](#5-w3c가-관리하는-주요-웹-표준)
6. [W3C vs WHATWG](#6-w3c-vs-whatwg-관계)
7. [다른 표준화 기구와의 관계](#7-w3c-vs-ietf-ecma-등-다른-표준화-기구와의-관계)
8. [웹 접근성 활동 상세](#8-웹-접근성accessibility-활동-상세)
9. [장점과 단점](#9-장점과-단점)
10. [개발자에게 미치는 영향과 중요성](#10-개발자에게-미치는-영향과-중요성)

---

## 1. 개요

### W3C란 무엇인가

W3C(World Wide Web Consortium) 는 월드 와이드 웹(WWW)의 국제 표준을 개발하고 관리하는 국제 표준화 기구이다. 웹 기술의 상호운용성(interoperability)을 보장하고, 웹이 장기적으로 성장할 수 있도록 가이드라인과 프로토콜, 기술 표준을 제정한다.

- 공식 웹사이트: https://www.w3.org/
- 설립일: 1994년 10월 1일
- 설립자: 팀 버너스리(Tim Berners-Lee)
- 본부: 미국 매사추세츠주 (MIT 내)
- 법적 지위: 2022년 6월, 비영리 공익법인(501(c)(3))으로 전환
- 회원: 약 450여 개 기업, 연구기관, 정부기관 등

### 설립 목적

W3C는 다음과 같은 핵심 목적을 위해 설립되었다:

1. 웹 표준의 통일성 확보: 각 브라우저 벤더가 독자적 기술을 추구하면서 발생하는 호환성 문제를 해결
2. 웹의 보편적 접근성 보장: 장애, 기기, 언어, 지역에 관계없이 누구나 웹에 접근할 수 있도록 보장
3. 웹 기술의 장기적 발전 주도: 단기적 상업 이해가 아닌, 웹의 장기적 이익을 위한 기술 방향 설정

### 미션 (Mission Statement)

> "Leading the Web to its full potential"
> (웹이 최대한의 잠재력을 발휘하도록 이끈다)

W3C의 미션은 다음 네 가지 설계 원칙에 기반한다:

| 원칙 | 설명 |
|------|------|
| Web for All | 모든 사람이 접근 가능한 웹 |
| Web on Everything | 모든 기기에서 동작하는 웹 |
| Web for Rich Interaction | 풍부한 상호작용이 가능한 웹 |
| Web of Data and Services | 데이터와 서비스의 웹 |

---

## 2. 역사

### 팀 버너스리와 W3C의 탄생

팀 버너스리(Sir Timothy John Berners-Lee) 는 1989년 CERN(유럽입자물리연구소)에서 근무하면서 전 세계 과학자들이 정보를 공유할 수 있는 시스템을 구상했다. 그 결과물이 바로 월드 와이드 웹(World Wide Web) 이다.

- 1989년 3월: 팀 버너스리가 "Information Management: A Proposal"이라는 제안서를 CERN에 제출
- 1990년 12월: 최초의 웹 브라우저(WorldWideWeb)와 웹 서버를 NeXT 컴퓨터에서 구현
- 1991년 8월: 최초의 웹사이트(http://info.cern.ch) 공개

웹이 폭발적으로 성장하면서, 표준 없이 각 브라우저 업체(특히 Netscape와 Microsoft)가 독자적인 기능을 추가하기 시작했다. 이른바 "브라우저 전쟁(Browser Wars)" 시대가 도래하면서, 웹의 파편화를 막기 위한 표준화 기구의 필요성이 대두되었다.

### 주요 연혁

| 연도 | 사건 |
|------|------|
| 1994년 10월 | 팀 버너스리가 MIT에서 W3C를 설립. MIT/LCS(현 MIT/CSAIL)가 최초 호스트 기관 |
| 1995년 | INRIA(프랑스)가 유럽 호스트로 합류. HTML 2.0 표준화 |
| 1996년 | CSS Level 1 권고안 발표 |
| 1997년 | HTML 3.2 권고안, HTML 4.0 권고안 발표 |
| 1998년 | XML 1.0 권고안, CSS Level 2 권고안, DOM Level 1 발표 |
| 1999년 | HTML 4.01 권고안 (HTML 4.x의 최종 버전) |
| 2000년 | XHTML 1.0 권고안 발표 |
| 2003년 | ERCIM이 INRIA를 대체하여 유럽 호스트가 됨 |
| 2004년 | WHATWG 결성 (W3C의 XHTML 2.0 방향에 반발) |
| 2006년 | 팀 버너스리가 W3C도 HTML5 작업에 참여할 것임을 발표 |
| 2007년 | HTML5 작업 그룹 공식 출범 |
| 2008년 | WCAG 2.0 권고안 발표 |
| 2009년 | XHTML 2.0 워킹 그룹 해산, HTML5에 집중 |
| 2012년 | HTML5 Candidate Recommendation |
| 2014년 10월 | HTML5 공식 권고안(W3C Recommendation) 발표 |
| 2017년 | HTML 5.1, HTML 5.2 권고안 |
| 2018년 | WCAG 2.1 권고안, WebAssembly Core Specification 1.0 |
| 2019년 5월 | W3C와 WHATWG가 HTML/DOM 표준에 대해 합의 (WHATWG가 Living Standard 유지) |
| 2020년 | WebAssembly W3C Recommendation |
| 2022년 6월 | W3C가 비영리 공익법인(501(c)(3))으로 법적 지위 전환 |
| 2022년 12월 | WCAG 2.2 Candidate Recommendation |
| 2023년 10월 | WCAG 2.2 W3C Recommendation |
| 2024년 | 다양한 Web API 표준 지속적 개발 및 업데이트 |

---

## 3. 조직 구조

### 전체 구조 개요

W3C는 2022년 법인 전환 이후 다음과 같은 거버넌스 구조를 가진다:

```
┌─────────────────────────────────────────────────────────┐
│                    W3C Inc. (법인)                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │   Board of   │  │   Advisory   │  │    Technical  │  │
│  │  Directors   │  │   Board (AB) │  │ Architecture  │  │
│  │              │  │              │  │  Group (TAG)  │  │
│  └──────────────┘  └──────────────┘  └───────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │                   CEO / Staff                     │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Working Groups (WG)                   │   │
│  │              Interest Groups (IG)                  │   │
│  │              Community Groups (CG)                 │   │
│  │              Business Groups (BG)                  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │           호스트 기관 (Partner Institutions)        │   │
│  │   MIT/CSAIL · ERCIM · Keio University · Beihang  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Advisory Committee (AC)               │   │
│  │          (각 회원 기관의 대표자로 구성)               │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 디렉터 (Director)

- 설립 이래 팀 버너스리가 W3C의 유일한 디렉터로서 기술적 방향을 이끌어 왔다
- 디렉터는 모든 W3C 표준의 최종 승인 권한을 가졌으며, Working Group 간의 분쟁을 조정하는 역할을 수행
- 2022년 법인 전환 이후, 디렉터 1인 체제에서 Board of Directors(이사회) 와 CEO 중심의 거버넌스로 전환
- 팀 버너스리는 W3C의 창립자이자 명예 직위를 유지

### 호스트 기관 (Host Institutions)

W3C는 설립 이래 여러 대학/연구기관이 물리적으로 호스팅하는 독특한 구조를 유지해 왔다:

| 기관 | 지역 | 역할 | 참여 시기 |
|------|------|------|-----------|
| MIT/CSAIL | 북미 (미국 매사추세츠) | 최초 호스트, 북미 거점 | 1994년~ |
| ERCIM | 유럽 (프랑스 소피아 앙티폴리스) | 유럽 거점 (INRIA 후임) | 2003년~ |
| Keio University (게이오대학) | 아시아 (일본) | 아시아 거점 | 1996년~ |
| Beihang University (베이항대학) | 중국 (베이징) | 중국 거점 | 2013년~ |

> 2022년 법인 전환 이후, 호스트 기관의 역할은 점차 줄어들고 있으며, W3C Inc.가 독립적으로 운영된다.

### Advisory Board (AB)

- 구성: 9명의 위원 (W3C 회원 기관에서 선출)
- 역할: W3C의 전략, 경영, 법적/프로세스 이슈에 대한 자문
- 임기: 2년, 재선 가능
- W3C Process Document의 관리 및 개선을 담당
- 기술적 사안이 아닌, 조직 운영 및 정책에 대한 가이드라인 제시

### Technical Architecture Group (TAG)

- 구성: 최대 12명 (디렉터 지명 + 회원 선출)
- 역할: 웹 아키텍처의 일관성과 무결성을 보장
- 핵심 문서: *"Architecture of the World Wide Web, Volume One"* (2004)
- 주요 활동:
  - 웹 아키텍처 원칙 문서화
  - Working Group 간 아키텍처 충돌 해결
  - 새로운 기술 제안에 대한 아키텍처 리뷰
  - Design Principles 문서 관리 (예: Priority of Constituencies)

### Advisory Committee (AC)

- 각 W3C 회원 기관에서 1명의 AC Representative를 파견
- W3C의 주요 의사결정(신규 Working Group 설립, 표준 최종 승인 등)에 대한 투표권 보유
- 매년 2회 AC Meeting 개최 (TPAC 중 1회 포함)

### Working Group (WG), Interest Group (IG), Community Group (CG)

| 그룹 유형 | 참여 조건 | 산출물 | 예시 |
|-----------|----------|--------|------|
| Working Group | W3C 회원만 | W3C Recommendation (표준) | CSS WG, Web Platform WG, WebAssembly WG |
| Interest Group | W3C 회원만 | Note, Use Case 문서 | Web & Networks IG, Accessible Platform Architectures |
| Community Group | 누구나 참여 가능 | Community Group Report | WICG (Web Incubator CG), WebXR CG |
| Business Group | 누구나 참여 가능 | 비즈니스 관련 보고서 | Automotive BG |

> WICG (Web Incubator Community Group): 새로운 웹 플랫폼 기능의 인큐베이터 역할. 아이디어가 충분히 성숙하면 공식 Working Group으로 이관된다.

---

## 4. 표준화 프로세스

### W3C Recommendation Track

W3C 표준(Recommendation)이 되기까지의 과정은 엄격한 다단계 프로세스를 따른다. 이를 Recommendation Track이라 한다.

```
[비공식 제안]
     │
     ▼
┌─────────────────────────────────────────────────┐
│  1. Working Draft (WD) - 작업 초안               │
│     첫 번째 공개 초안(FPWD) → 반복 업데이트        │
└─────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────┐
│  2. Candidate Recommendation (CR) - 후보 권고안   │
│     구현 경험 수집, 테스트 스위트 개발              │
│     CR Snapshot / CR Draft                       │
└─────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────┐
│  3. Proposed Recommendation (PR) - 제안 권고안    │
│     Advisory Committee 리뷰 (최소 28일)           │
└─────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────┐
│  4. W3C Recommendation (REC) - W3C 권고안        │
│     공식 웹 표준으로 발행                          │
└─────────────────────────────────────────────────┘
```

### 각 단계 상세

#### 1단계: Working Draft (WD) - 작업 초안

- 목적: 커뮤니티와 W3C 회원의 피드백 수집
- 특징:
  - 불완전하며 큰 폭의 변경이 가능
  - First Public Working Draft (FPWD): 처음으로 공개되는 초안, 특허 정책의 시작점
  - 여러 차례 업데이트되며 반복적으로 개선
  - 구현자에게 "아직 이 단계의 스펙에 의존하지 말라"는 경고가 포함
- 발행 주체: Working Group

#### 2단계: Candidate Recommendation (CR) - 후보 권고안

- 목적: 실제 구현을 통한 검증
- 특징:
  - 기술적으로 완성되었다고 판단된 상태
  - 최소 2개 이상의 독립적 구현(implementation)이 필요
  - 테스트 스위트(Test Suite)를 통해 상호운용성 검증
  - CR Snapshot: 특정 시점의 안정된 스냅샷
  - CR Draft: 지속적으로 업데이트되는 CR
  - 이 단계에서 발견된 문제로 WD로 되돌아갈 수 있음
- 특허 정책: 이 단계에서 회원사의 특허 공개 의무가 확정됨 (Exclusion Opportunity)

#### 3단계: Proposed Recommendation (PR) - 제안 권고안

- 목적: Advisory Committee의 최종 리뷰
- 특징:
  - 기술적 내용이 확정된 상태
  - AC Representative들이 최소 28일간 리뷰
  - 찬성/반대/기권 투표
  - 반대 의견이 있을 경우 조정 과정을 거침
- 투표 결과에 따라: Recommendation으로 진행 또는 이전 단계로 회귀

#### 4단계: W3C Recommendation (REC) - W3C 권고안

- 의미: W3C가 공식적으로 승인한 웹 표준
- 특징:
  - 광범위한 구현과 테스트를 거친 안정적 표준
  - 웹 개발자가 프로덕션에서 안심하고 사용할 수 있는 수준
  - 발행 후에도 정오표(Errata) 수집 및 개정판(Revised Edition) 가능
  - 더 이상 유효하지 않은 표준은 Superseded(대체됨) 또는 Obsolete(폐기됨)로 표시

### 기타 문서 유형

| 유형 | 설명 |
|------|------|
| W3C Note (Group Note) | 표준이 아닌 참고 문서. 사용 가이드, 모범 사례, 해설 등 |
| W3C Statement | W3C의 공식 입장을 표명하는 문서 |
| Registry | 값의 레지스트리 (예: MIME 타입 등) |
| Editor's Draft | 에디터가 작성 중인 최신 비공식 초안 (가장 최신이지만 비공식) |

### 표준화에 걸리는 시간

일반적으로 FPWD에서 Recommendation까지 3~10년 이상 소요된다. 예를 들어:

- CSS Grid Layout: 약 6년 (2011 FPWD → 2017 CR)
- HTML5: 약 7년 (2007 FPWD → 2014 REC)
- WCAG 2.1: 약 2년 (2017 FPWD → 2018 REC)

---

## 5. W3C가 관리하는 주요 웹 표준

### 5.1 HTML (HyperText Markup Language)

#### 개요
HTML은 웹 페이지의 구조를 정의하는 마크업 언어이다. 웹의 가장 기본적인 빌딩 블록이다.

#### 역사와 버전

| 버전 | 연도 | 주요 특징 |
|------|------|-----------|
| HTML 2.0 | 1995 | 최초의 표준 (RFC 1866) |
| HTML 3.2 | 1997 | 테이블, 앱릿, 텍스트 흐름 |
| HTML 4.01 | 1999 | CSS 분리, Strict/Transitional/Frameset |
| XHTML 1.0 | 2000 | XML 기반 재정의 |
| HTML5 | 2014 | 시맨틱 태그, Canvas, Video/Audio, 로컬 저장소 |
| HTML 5.1 | 2016 | 추가 개선 |
| HTML 5.2 | 2017 | Payment Request API 참조 등 |
| HTML Living Standard | 2019~ | WHATWG가 유지보수하는 지속적 업데이트 표준 |

#### HTML5의 핵심 기능

```html
<!-- 시맨틱 요소 -->
<header>, <nav>, <main>, <article>, <section>, <aside>, <footer>

<!-- 멀티미디어 -->
<video>, <audio>, <canvas>

<!-- 폼 개선 -->
<input type="date">, <input type="email">, <input type="range">
<datalist>, <output>, <progress>, <meter>

<!-- 기타 -->
<details>, <summary>, <dialog>, <template>
```

- Canvas API: 2D 그래픽 렌더링
- Drag & Drop API: 네이티브 드래그 앤 드롭
- History API: SPA의 URL 관리
- Web Workers: 백그라운드 스레드
- Server-Sent Events: 서버에서 클라이언트로의 푸시

> 현재 상태: W3C는 2019년 합의 이후 HTML 표준의 유지보수를 WHATWG에 위임했다. WHATWG HTML Living Standard가 사실상의 단일 HTML 표준이다.

---

### 5.2 CSS (Cascading Style Sheets)

#### 개요
CSS는 HTML 문서의 시각적 표현을 정의하는 스타일시트 언어이다. W3C의 CSS Working Group이 관리한다.

#### 버전과 모듈화

| 버전 | 연도 | 주요 내용 |
|------|------|-----------|
| CSS1 | 1996 | 기본 폰트, 색상, 여백, 정렬 |
| CSS2 | 1998 | 포지셔닝, z-index, 미디어 타입 |
| CSS2.1 | 2011 | CSS2의 버그 수정, 실제 구현 반영 |
| CSS3 | 2011~ | 모듈 단위 개별 스펙으로 분리 |

CSS3부터는 단일 스펙이 아닌 모듈별로 독립적인 레벨을 가진다:

| 모듈 | 레벨 | 상태 | 주요 기능 |
|------|------|------|-----------|
| Selectors | Level 4 | WD | `:is()`, `:where()`, `:has()` |
| Flexbox | Level 1 | CR | 1차원 레이아웃 |
| Grid Layout | Level 2 | WD | 2차원 레이아웃, Subgrid |
| Custom Properties | Level 1 | CR | CSS 변수 (`--var`) |
| Containment | Level 3 | WD | Container Queries |
| Color | Level 4 | CR | `oklch()`, `color-mix()` |
| Cascade | Level 6 | WD | `@scope`, Cascade Layers |
| Animations | Level 1 | WD | `@keyframes`, `animation` |
| Transitions | Level 1 | WD | `transition` 속성 |
| Media Queries | Level 5 | WD | `prefers-color-scheme` 등 |
| Scroll Snap | Level 1 | CR | 스크롤 위치 스냅 |
| Fonts | Level 4 | WD | 가변 폰트, `font-variation-settings` |

> "CSS4"는 없다: CSS는 Level 3 이후 모듈별로 독립 버전이 관리되므로, 단일 "CSS4"라는 버전은 존재하지 않는다.

---

### 5.3 DOM (Document Object Model)

#### 개요
DOM은 HTML/XML 문서를 트리 구조의 객체 모델로 표현하는 프로그래밍 인터페이스이다.

#### 주요 버전

| 버전 | 연도 | 주요 내용 |
|------|------|-----------|
| DOM Level 1 | 1998 | Core, HTML 인터페이스 |
| DOM Level 2 | 2000-2003 | Events, Style, Traversal, Range |
| DOM Level 3 | 2004 | Load/Save, Validation, XPath |
| DOM Level 4 (DOM4) | 2015 | MutationObserver, Promise 기반 |
| DOM Living Standard | 현재 | WHATWG에서 유지보수 |

#### 핵심 인터페이스

```
Document
  ├── Element
  │     ├── HTMLElement
  │     └── SVGElement
  ├── Text
  ├── Comment
  ├── DocumentFragment
  └── Attr
```

주요 API:
- `document.querySelector()` / `querySelectorAll()`
- `element.classList` (add, remove, toggle, contains)
- `element.dataset` (커스텀 데이터 속성)
- `MutationObserver` (DOM 변경 감지)
- `element.append()`, `element.prepend()`, `element.replaceWith()`

> 현재 상태: DOM 표준도 2019년 합의 이후 WHATWG DOM Living Standard로 통합되었다.

---

### 5.4 SVG (Scalable Vector Graphics)

#### 개요
SVG는 XML 기반의 2차원 벡터 그래픽 포맷이다. 해상도에 무관하게 선명한 그래픽을 제공한다.

#### 주요 버전

| 버전 | 연도 | 상태 |
|------|------|------|
| SVG 1.0 | 2001 | REC (Superseded) |
| SVG 1.1 | 2003 (2nd Ed. 2011) | REC |
| SVG 2 | 진행 중 | CR |

#### 주요 기능

- 기본 도형: `<rect>`, `<circle>`, `<ellipse>`, `<line>`, `<polyline>`, `<polygon>`
- 경로: `<path>` (베지어 곡선, 아크 등 복잡한 도형)
- 텍스트: `<text>`, `<tspan>`
- 그라디언트: `<linearGradient>`, `<radialGradient>`
- 필터: `<filter>` (blur, shadow, color matrix 등)
- 애니메이션: SMIL 기반 `<animate>`, `<animateTransform>`
- CSS와 통합: SVG 요소에 CSS 스타일 적용 가능
- JavaScript와 통합: DOM API로 SVG 요소 조작 가능

---

### 5.5 XML / XHTML

#### XML (eXtensible Markup Language)

XML은 데이터의 구조화된 표현을 위한 범용 마크업 언어이다.

| 스펙 | 버전 | 연도 | 설명 |
|------|------|------|------|
| XML | 1.0 (5th Ed.) | 2008 | 핵심 문법 |
| XML | 1.1 (2nd Ed.) | 2006 | 유니코드 확장 |
| XML Namespaces | 1.0 (3rd Ed.) | 2009 | 이름 충돌 방지 |
| XPath | 3.1 | 2017 | XML 노드 질의 |
| XSLT | 3.0 | 2017 | XML 변환 |
| XML Schema (XSD) | 1.1 | 2012 | XML 구조 정의 |

#### XHTML (eXtensible HTML)

XHTML은 HTML을 XML 규칙에 맞게 재정의한 것이다.

| 버전 | 연도 | 특징 |
|------|------|------|
| XHTML 1.0 | 2000 | HTML 4.01의 XML 재정의 |
| XHTML 1.1 | 2001 | 모듈화, Strict만 지원 |
| XHTML 2.0 | 폐기 | 하위 호환성 포기로 실패, 2009년 WG 해산 |

> 교훈: XHTML 2.0의 실패는 W3C 역사에서 중요한 전환점이다. 하위 호환성(backward compatibility)을 무시한 표준은 실패한다는 교훈을 남겼으며, 이는 WHATWG 결성과 HTML5 탄생의 직접적 원인이 되었다.

---

### 5.6 WebAssembly (Wasm)

#### 개요
WebAssembly는 브라우저에서 네이티브에 가까운 성능으로 실행되는 바이너리 명령어 포맷이다. W3C의 WebAssembly Working Group이 관리한다.

#### 핵심 스펙

| 스펙 | 상태 | 설명 |
|------|------|------|
| WebAssembly Core Specification | REC (2.0 진행 중) | 바이너리 포맷, 명령어 집합, 검증 규칙 |
| WebAssembly JavaScript Interface | REC | JS에서 Wasm 모듈 로드/실행 |
| WebAssembly Web API | REC | `fetch()`로 `.wasm` 스트리밍 컴파일 |

#### 특징

- 언어 독립적: C, C++, Rust, Go, AssemblyScript 등에서 컴파일 가능
- 샌드박스 실행: 브라우저의 보안 모델 준수
- JavaScript와 상호운용: JS에서 Wasm 함수 호출, Wasm에서 JS 함수 호출 가능
- 스택 기반 가상 머신: 컴팩트한 바이너리 포맷
- WASI(WebAssembly System Interface): 브라우저 외부에서의 실행을 위한 시스템 인터페이스

#### 사용 사례
- 게임 엔진 (Unity, Unreal Engine의 웹 빌드)
- 이미지/비디오 편집 (Figma, Photoshop Web)
- 암호화/압축 연산
- CAD/과학 시뮬레이션

---

### 5.7 Web Accessibility (WCAG, WAI-ARIA)

#### WCAG (Web Content Accessibility Guidelines)

웹 콘텐츠의 접근성을 보장하기 위한 가이드라인이다. 자세한 내용은 [8장 웹 접근성 활동 상세](#8-웹-접근성accessibility-활동-상세)에서 다룬다.

| 버전 | 연도 | 주요 변화 |
|------|------|-----------|
| WCAG 1.0 | 1999 | 14개 가이드라인, HTML 중심 |
| WCAG 2.0 | 2008 | 기술 독립적, 4원칙(POUR), ISO 40500 채택 |
| WCAG 2.1 | 2018 | 모바일, 저시력, 인지장애 대응 추가 |
| WCAG 2.2 | 2023 | 인지/학습 장애 관련 기준 강화 |
| WCAG 3.0 | 진행 중 | 완전한 재설계, 새로운 적합성 모델 |

#### WAI-ARIA (Accessible Rich Internet Applications)

동적 웹 애플리케이션의 접근성을 개선하기 위한 기술 사양이다.

```html
<!-- ARIA 역할(Role) -->
<div role="tablist">
  <button role="tab" aria-selected="true" aria-controls="panel1">Tab 1</button>
  <button role="tab" aria-selected="false" aria-controls="panel2">Tab 2</button>
</div>
<div role="tabpanel" id="panel1">Content 1</div>

<!-- ARIA 상태/속성 -->
<button aria-expanded="false" aria-controls="menu">Menu</button>
<ul id="menu" aria-hidden="true">...</ul>

<!-- 라이브 리전 -->
<div aria-live="polite" aria-atomic="true">
  알림 메시지가 여기에 표시됩니다
</div>
```

주요 ARIA 카테고리:
- Roles: `role="button"`, `role="dialog"`, `role="alert"`, `role="navigation"` 등
- States: `aria-expanded`, `aria-selected`, `aria-checked`, `aria-disabled`
- Properties: `aria-label`, `aria-labelledby`, `aria-describedby`, `aria-controls`
- Live Regions: `aria-live="polite|assertive"`, `aria-atomic`, `aria-relevant`

---

### 5.8 Web APIs

W3C는 브라우저에서 사용할 수 있는 수많은 Web API의 표준화를 담당한다.

#### 주요 Web API 목록

| API | 상태 | 설명 |
|-----|------|------|
| Fetch API | Living Standard (WHATWG) | `XMLHttpRequest` 대체, Promise 기반 네트워크 요청 |
| WebSocket API | Living Standard (WHATWG) | 양방향 실시간 통신 |
| Web Storage API | REC | `localStorage`, `sessionStorage` |
| IndexedDB | REC | 브라우저 내 대용량 구조화 데이터 저장 |
| Service Workers | WD/CR | 오프라인 지원, 푸시 알림, 백그라운드 동기화 |
| Web Workers | Living Standard (WHATWG) | 백그라운드 스레드 실행 |
| Geolocation API | REC | 사용자 위치 정보 |
| Web Notifications | REC | 시스템 알림 표시 |
| Pointer Events | REC | 마우스/터치/펜 통합 입력 처리 |
| Intersection Observer | WD | 요소의 뷰포트 진입/이탈 감지 |
| Resize Observer | WD | 요소 크기 변경 감지 |
| Performance API | REC | 성능 측정 (`performance.now()`, Navigation Timing 등) |
| Clipboard API | WD | 클립보드 읽기/쓰기 |
| File API | WD | 파일 읽기 (`FileReader`, `Blob`) |
| Web Animations API | WD | CSS 애니메이션/트랜지션의 프로그래밍 인터페이스 |
| Web Crypto API | REC | 암호화 연산 (해시, 서명, 암호화/복호화) |
| Permissions API | WD | 권한 상태 쿼리/요청 |
| Screen Wake Lock API | WD | 화면 꺼짐 방지 |
| Badging API | WD | 앱 아이콘 배지 표시 |

#### Service Workers 상세

```javascript
// Service Worker 등록
navigator.serviceWorker.register('/sw.js')
  .then(reg => console.log('SW registered:', reg))
  .catch(err => console.log('SW registration failed:', err));

// sw.js - 캐시 전략 예시
self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(cached => cached || fetch(event.request))
  );
});
```

Service Worker의 주요 역할:
- 오프라인 캐싱: Cache API를 활용한 오프라인 우선 전략
- 푸시 알림: Push API와 결합한 백그라운드 알림
- 백그라운드 동기화: Background Sync API
- PWA(Progressive Web App)의 핵심: 네이티브 앱과 유사한 경험 제공

---

### 5.9 Semantic Web / RDF / OWL

#### Semantic Web 비전

팀 버너스리가 2001년 제안한 차세대 웹 비전이다. 기계가 웹의 데이터를 이해하고 처리할 수 있는 "의미 있는 웹"을 목표로 한다.

#### Semantic Web 기술 스택

```
┌───────────────────────────────┐
│         User Interface        │
├───────────────────────────────┤
│          Trust / Proof        │
├───────────────────────────────┤
│       Logic / Rules (RIF)     │
├───────────────────────────────┤
│   Ontology (OWL)              │
├───────────────────────────────┤
│   Querying (SPARQL)           │
├───────────────────────────────┤
│   Schema (RDFS)               │
├───────────────────────────────┤
│   Data Interchange (RDF)      │
├───────────────────────────────┤
│   Syntax (XML, JSON-LD, Turtle)│
├───────────────────────────────┤
│   Identifiers (URI/IRI)       │
├───────────────────────────────┤
│   Character Set (Unicode)     │
└───────────────────────────────┘
```

#### 주요 표준

| 표준 | 설명 |
|------|------|
| RDF (Resource Description Framework) | 주어-서술어-목적어(Triple) 형태로 데이터를 표현하는 프레임워크 |
| RDFS (RDF Schema) | RDF 어휘의 클래스/프로퍼티 계층 정의 |
| OWL (Web Ontology Language) | 온톨로지 정의를 위한 표현력 높은 언어 |
| SPARQL | RDF 데이터를 질의하는 쿼리 언어 |
| JSON-LD | JSON 기반의 Linked Data 직렬화 포맷 |
| RDFa | HTML에 RDF 메타데이터를 삽입하는 방법 |

```turtle
# RDF Triple 예시 (Turtle 구문)
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix ex: <http://example.org/> .

ex:TimBL a foaf:Person ;
    foaf:name "Tim Berners-Lee" ;
    foaf:homepage <https://www.w3.org/People/Berners-Lee/> .
```

```sparql
# SPARQL 쿼리 예시
SELECT ?name ?homepage
WHERE {
  ?person a foaf:Person .
  ?person foaf:name ?name .
  ?person foaf:homepage ?homepage .
}
```

> 현재 상태: 시맨틱 웹은 원래의 "웹 전체를 의미 있게" 라는 비전은 완전히 실현되지 못했으나, Knowledge Graph(Google, Wikidata), Linked Open Data, JSON-LD for SEO(Schema.org) 등의 형태로 실질적 영향력을 행사하고 있다.

---

### 5.10 WebRTC (Web Real-Time Communication)

#### 개요
WebRTC는 브라우저 간 플러그인 없이 실시간 음성/영상/데이터 통신을 가능하게 하는 기술이다.

#### W3C에서 관리하는 스펙

| 스펙 | 상태 | 설명 |
|------|------|------|
| WebRTC 1.0 | REC | 피어 연결, 미디어 스트림, 데이터 채널 |
| Media Capture and Streams | CR | `getUserMedia()`, MediaStream API |
| MediaStream Recording | WD | 미디어 스트림 녹화 |
| Screen Capture | WD | 화면 공유 (`getDisplayMedia()`) |

> 참고: WebRTC의 프로토콜 계층(ICE, STUN, TURN, SDP, SRTP 등)은 IETF에서 표준화한다. W3C는 브라우저 JavaScript API 부분을 담당한다.

#### 핵심 API

```javascript
// 피어 연결 생성
const pc = new RTCPeerConnection(configuration);

// 로컬 미디어 스트림 획득
const stream = await navigator.mediaDevices.getUserMedia({
  video: true,
  audio: true
});

// 트랙 추가 및 시그널링
stream.getTracks().forEach(track => pc.addTrack(track, stream));

// ICE 후보 교환
pc.onicecandidate = event => {
  if (event.candidate) sendToRemotePeer(event.candidate);
};

// 데이터 채널
const dataChannel = pc.createDataChannel("chat");
dataChannel.onmessage = event => console.log(event.data);
```

---

### 5.11 Web Payments

#### 개요
웹에서의 결제를 표준화하여 사용자 경험을 개선하고 보안을 강화하는 것을 목표로 한다.

#### 주요 스펙

| 스펙 | 상태 | 설명 |
|------|------|------|
| Payment Request API | REC | 브라우저 네이티브 결제 UI |
| Payment Handler API | WD | 결제 앱/서비스 워커 기반 핸들러 |
| Payment Method Identifiers | REC | 결제 수단 식별 |

```javascript
// Payment Request API 예시
const paymentRequest = new PaymentRequest(
  [{ supportedMethods: 'https://example.com/pay' }],
  {
    total: {
      label: '합계',
      amount: { currency: 'KRW', value: '50000' }
    },
    displayItems: [
      { label: '상품 A', amount: { currency: 'KRW', value: '30000' } },
      { label: '배송비', amount: { currency: 'KRW', value: '20000' } }
    ]
  }
);

const response = await paymentRequest.show();
await response.complete('success');
```

---

## 6. W3C vs WHATWG 관계

### WHATWG란

WHATWG(Web Hypertext Application Technology Working Group) 는 2004년에 Apple, Mozilla, Opera의 개발자들이 결성한 비공식 커뮤니티 그룹이다.

### 갈등의 배경

```
2004년 W3C Workshop on Web Applications
    │
    │  Apple, Mozilla, Opera가 HTML 진화를 제안
    │  → W3C 투표에서 부결 (XHTML 2.0 노선 고수)
    │
    ▼
WHATWG 결성 (2004.06)
    │  "HTML5" 작업 독자적으로 시작
    │  Ian Hickson(히슨)이 에디터로 주도
    │
    │  2006년: 팀 버너스리가 W3C의 XHTML 2.0 방향이
    │          잘못되었음을 인정
    │
    ▼
W3C HTML5 WG 출범 (2007)
    │  WHATWG의 작업을 기반으로 공동 작업 시작
    │  하지만 두 조직의 스펙이 점차 분기
    │
    ▼
분기 심화 (2012-2018)
    │  W3C: 버전 기반 스냅샷 (HTML 5.0, 5.1, 5.2)
    │  WHATWG: Living Standard (지속적 업데이트)
    │  → 두 개의 "HTML 표준"이 병존하는 혼란
    │
    ▼
합의 (2019.05)
    │  W3C와 WHATWG가 양해각서(MoU) 체결
    │  → HTML/DOM 표준은 WHATWG Living Standard가 유일한 표준
    │  → W3C는 더 이상 독자적 HTML 스냅샷을 발행하지 않음
    │  → W3C 회원은 WHATWG 프로세스에 참여
    │
    ▼
현재 상태
    WHATWG HTML Living Standard = 유일한 HTML 표준
    W3C는 리뷰 및 피드백 역할
```

### 핵심 쟁점

| 쟁점 | W3C 입장 | WHATWG 입장 |
|------|----------|------------|
| 표준 버전 관리 | 안정적 스냅샷 버전 (5.0, 5.1...) | Living Standard (지속적 업데이트) |
| 하위 호환성 | XHTML 2.0에서 한때 무시 | 최우선 원칙 |
| 참여 모델 | 회원제 (회비 기반) | 누구나 참여 가능 |
| 특허 정책 | 엄격한 Royalty-Free 특허 정책 | 2017년 W3C 특허 정책 도입 |
| 의사결정 | 합의 기반 + 디렉터 결정권 | 에디터 주도 |

### 2019년 합의의 의미

- 실용주의의 승리: 브라우저 벤더가 실제로 구현하는 스펙이 사실상 표준이 됨
- Living Standard 모델 채택: 버전 없는 지속적 업데이트가 웹 표준의 새로운 패러다임
- W3C의 역할 변화: HTML/DOM에서는 직접 표준 제정이 아닌 리뷰/검증 역할로 전환

---

## 7. W3C vs IETF, ECMA 등 다른 표준화 기구와의 관계

### 주요 표준화 기구 비교

| 기구 | 정식 명칭 | 설립 | 관할 영역 |
|------|----------|------|-----------|
| W3C | World Wide Web Consortium | 1994 | 웹 클라이언트 기술 (HTML, CSS, DOM, Web API 등) |
| IETF | Internet Engineering Task Force | 1986 | 인터넷 프로토콜 (HTTP, TLS, TCP, DNS 등) |
| Ecma International | European Computer Manufacturers Association | 1961 | 프로그래밍 언어 표준 (ECMAScript/JavaScript 등) |
| ISO | International Organization for Standardization | 1947 | 국제 산업 표준 전반 |
| WHATWG | Web Hypertext Application Technology Working Group | 2004 | HTML, DOM, Fetch 등의 Living Standard |
| Unicode Consortium | - | 1991 | 문자 인코딩 (Unicode) |
| Khronos Group | - | 2000 | 그래픽 API (WebGL, WebGPU, Vulkan) |

### W3C와 IETF의 관계

```
            IETF                              W3C
    ┌─────────────────┐            ┌─────────────────────┐
    │ HTTP/1.1 (RFC9112)│           │ Fetch API            │
    │ HTTP/2 (RFC7540)  │           │ XMLHttpRequest       │
    │ HTTP/3 (RFC9114)  │           │                      │
    │ TLS (RFC8446)     │    연계    │ Web Crypto API       │
    │ WebSocket Protocol│◄─────────►│ WebSocket API        │
    │   (RFC6455)       │           │                      │
    │ ICE/STUN/TURN     │           │ WebRTC API           │
    │ URI (RFC3986)     │           │ URL Standard         │
    └─────────────────┘            └─────────────────────┘
```

- 역할 분담: IETF는 프로토콜(wire protocol), W3C는 브라우저 API를 담당
- 예시: WebSocket의 경우, 프로토콜은 IETF RFC 6455, JavaScript API는 W3C가 표준화
- 공동 작업: HTTP, URI 등 공통 관심사에 대해 정기적으로 협력
- 참여 방식: IETF는 완전 개방 참여, W3C는 (전통적으로) 회원제

### W3C와 Ecma International의 관계

```
        Ecma International                    W3C
    ┌──────────────────────┐          ┌──────────────────┐
    │ ECMA-262             │          │ DOM API          │
    │ (ECMAScript/JS 언어)  │   연계    │ Web APIs         │
    │                      │◄────────►│ Web IDL          │
    │ ECMA-402             │          │ (API 바인딩 정의)  │
    │ (Intl API)           │          │                  │
    │                      │          │                  │
    │ TC39                 │          │ Working Groups   │
    │ (JS 언어 발전)        │          │ (웹 플랫폼 기능)   │
    └──────────────────────┘          └──────────────────┘
```

- 역할 분담: Ecma TC39는 JavaScript 언어 자체(문법, 내장 객체), W3C는 브라우저 환경의 API를 담당
- 예시: `Promise`는 ECMAScript 표준, `fetch()`는 WHATWG/W3C 표준
- Web IDL: W3C가 정의한 인터페이스 기술 언어로, JavaScript 바인딩을 명세하는 데 사용

### W3C와 ISO의 관계

- W3C의 일부 표준은 ISO 표준으로 채택됨
  - WCAG 2.0 → ISO/IEC 40500:2012
  - XML → ISO 표준 참조
  - PNG → ISO/IEC 15948:2004
- PAS(Publicly Available Specification) 제출자: W3C는 ISO/IEC JTC 1의 PAS 제출자 자격을 보유

### W3C와 Khronos Group의 관계

- WebGL: Khronos Group이 관리 (OpenGL ES 기반)
- WebGPU: W3C GPU for the Web Working Group이 관리 (차세대 GPU API)
- 두 기구는 웹에서의 그래픽 기술 표준화에 대해 상호 협력

---

## 8. 웹 접근성(Accessibility) 활동 상세

### WAI (Web Accessibility Initiative)

W3C의 WAI(Web Accessibility Initiative) 는 1997년에 시작된 웹 접근성 전문 이니셔티브이다. 장애인을 포함한 모든 사람이 웹을 사용할 수 있도록 가이드라인, 기술 사양, 지원 리소스를 개발한다.

### WCAG 4원칙 (POUR)

WCAG의 모든 성공 기준은 네 가지 원칙 아래 조직된다:

| 원칙 | 영문 | 의미 |
|------|------|------|
| 인식의 용이성 | Perceivable | 정보와 UI 컴포넌트를 사용자가 인식할 수 있어야 한다 |
| 운용의 용이성 | Operable | UI 컴포넌트와 내비게이션을 조작할 수 있어야 한다 |
| 이해의 용이성 | Understandable | 정보와 UI 조작이 이해 가능해야 한다 |
| 견고성 | Robust | 다양한 사용자 에이전트(보조 기술 포함)와 호환되어야 한다 |

### WCAG 적합성 수준

| 수준 | 의미 | 설명 |
|------|------|------|
| Level A | 최소 수준 | 이 수준을 충족하지 못하면 일부 사용자가 웹 콘텐츠에 전혀 접근 불가 |
| Level AA | 권장 수준 | 대부분의 법률/정책에서 요구하는 수준. 많은 장벽을 제거 |
| Level AAA | 최고 수준 | 가장 높은 접근성. 모든 콘텐츠에 적용하기는 비현실적 |

### WCAG 2.2의 주요 성공 기준 (예시)

#### Level A 기준 (예시)
| 기준 | 제목 | 설명 |
|------|------|------|
| 1.1.1 | 대체 텍스트 (Non-text Content) | 모든 비텍스트 콘텐츠에 텍스트 대안 제공 |
| 1.3.1 | 정보와 관계 (Info and Relationships) | 구조/관계 정보를 프로그래밍 방식으로 결정 가능 |
| 2.1.1 | 키보드 (Keyboard) | 모든 기능을 키보드로 조작 가능 |
| 2.4.1 | 블록 건너뛰기 (Bypass Blocks) | 반복 콘텐츠 블록을 건너뛸 수 있는 메커니즘 |
| 4.1.2 | 이름, 역할, 값 (Name, Role, Value) | 모든 UI 컴포넌트의 이름/역할을 프로그래밍 방식으로 결정 가능 |

#### Level AA 기준 (예시)
| 기준 | 제목 | 설명 |
|------|------|------|
| 1.4.3 | 대비 (최소) (Contrast Minimum) | 텍스트와 배경의 명암비 최소 4.5:1 |
| 1.4.4 | 텍스트 크기 변경 (Resize Text) | 200%까지 확대 시 콘텐츠/기능 손실 없음 |
| 2.4.7 | 초점 표시 (Focus Visible) | 키보드 포커스 위치가 시각적으로 확인 가능 |
| 1.4.11 | 비텍스트 대비 (Non-text Contrast) | UI 컴포넌트/그래픽 명암비 최소 3:1 |

### WCAG 2.2의 새로운 성공 기준

| 기준 | 제목 | 수준 | 설명 |
|------|------|------|------|
| 2.4.11 | Focus Not Obscured (Minimum) | AA | 포커스 받은 요소가 다른 콘텐츠에 의해 완전히 가려지지 않아야 함 |
| 2.4.12 | Focus Not Obscured (Enhanced) | AAA | 포커스 받은 요소가 전혀 가려지지 않아야 함 |
| 2.4.13 | Focus Appearance | AAA | 포커스 인디케이터의 최소 크기와 대비 |
| 2.5.7 | Dragging Movements | AA | 드래그 동작의 단일 포인터 대안 제공 |
| 2.5.8 | Target Size (Minimum) | AA | 터치/클릭 대상의 최소 크기 24x24 CSS 픽셀 |
| 3.2.6 | Consistent Help | A | 도움말의 일관된 위치 |
| 3.3.7 | Redundant Entry | A | 이전에 입력한 정보의 재입력 방지 |
| 3.3.8 | Accessible Authentication (Minimum) | AA | 인지 기능 테스트 없이 인증 가능 |
| 3.3.9 | Accessible Authentication (Enhanced) | AAA | 더 엄격한 인증 접근성 |

### WAI가 개발하는 기술 사양

| 사양 | 설명 |
|------|------|
| WAI-ARIA 1.2 | 동적 콘텐츠와 고급 UI의 접근성 |
| ARIA Authoring Practices Guide (APG) | ARIA 위젯/패턴 구현 가이드 |
| ATAG 2.0 | 저작 도구의 접근성 가이드라인 |
| UAAG 2.0 | 사용자 에이전트(브라우저 등)의 접근성 가이드라인 |
| WAI-Adapt | 콘텐츠 적응/개인화 |
| Pronunciation | 텍스트 음성 변환 발음 |

### 접근성 관련 법률

WCAG는 전 세계 접근성 법률의 기술적 기반이 된다:

| 국가/지역 | 법률/정책 | WCAG 요구 수준 |
|-----------|----------|---------------|
| 한국 | 장애인차별금지법, 웹 접근성 인증 (KWCAG) | WCAG 2.1 기반 |
| 미국 | ADA, Section 508 | WCAG 2.0 Level AA |
| EU | European Accessibility Act (EAA) | WCAG 2.1 Level AA |
| 영국 | Equality Act 2010 | WCAG 2.1 Level AA |
| 캐나다 | Accessible Canada Act | WCAG 2.0 Level AA |
| 일본 | JIS X 8341-3 | WCAG 2.0 기반 |

---

## 9. 장점과 단점

### 장점

#### 1. 개방적이고 로열티 프리(Royalty-Free)인 표준
- W3C 표준은 누구나 무료로 구현할 수 있다
- W3C Patent Policy: 회원사는 자신의 특허를 로열티 프리로 라이선스해야 함
- 이는 웹의 개방성을 유지하는 핵심 메커니즘이다

#### 2. 광범위한 이해관계자 참여
- 브라우저 벤더(Google, Apple, Mozilla, Microsoft), 기업, 학계, 정부 기관, 시민 단체 등 다양한 참여자
- 합의(consensus) 기반 의사결정으로 특정 기업의 독점 방지

#### 3. 상호운용성(Interoperability) 보장
- 동일한 웹 페이지가 모든 브라우저에서 동일하게 동작하도록 보장
- 테스트 스위트(예: web-platform-tests)를 통한 구현 검증

#### 4. 접근성에 대한 체계적 접근
- WAI를 통한 체계적이고 지속적인 접근성 표준 개발
- WCAG는 전 세계 접근성 법률의 기술적 토대

#### 5. 장기적 안정성
- 30년 이상의 역사를 가진 검증된 표준화 기구
- 하위 호환성을 중시하여 기존 웹 콘텐츠가 깨지지 않음

### 단점 및 비판

#### 1. 느린 표준화 속도
- Recommendation까지 수년~10년 이상 소요
- 빠르게 변화하는 웹 기술 생태계와의 괴리
- 브라우저 벤더는 표준 확정 전에 이미 기능을 구현하는 경우가 많음
- 결과: Living Standard 모델(WHATWG)의 등장

#### 2. 회원제 기반의 폐쇄성 (과거)
- Working Group 참여는 연간 수만~수십만 달러의 회비를 내는 회원만 가능
- 소규모 기업이나 개인 개발자의 참여가 어려움
- Community Group으로 일부 완화되었으나, 실질적 표준 결정권은 여전히 회원에게 집중
- 개선: 2022년 법인 전환 이후 참여 구조 개선 노력 중

#### 3. 대형 기업의 영향력
- 실질적으로 Google(Chrome), Apple(Safari), Microsoft(Edge), Mozilla(Firefox)의 의사가 표준에 큰 영향
- 브라우저 벤더가 구현을 거부하면 표준은 사실상 의미를 잃음
- 예시: Google의 AMP, Apple의 Web Push 지연 등

#### 4. 과거의 방향 오류
- XHTML 2.0 실패: 현실의 웹과 동떨어진 순수주의적 접근
- Semantic Web의 제한적 성공: 원래의 야심찬 비전은 달성되지 못함
- DRM(EME) 표준화 논란: Encrypted Media Extensions의 표준화를 둘러싼 개방 웹 진영의 강한 반발

#### 5. EME(Encrypted Media Extensions) 논란
- 2017년, DRM 기술인 EME를 W3C Recommendation으로 승인
- EFF(Electronic Frontier Foundation)가 이에 항의하며 W3C 탈퇴
- 비판의 핵심: DRM은 웹의 개방성 원칙에 반하며, 보안 연구자의 활동을 제한할 수 있다
- 찬성의 핵심: DRM 없이는 넷플릭스 등 콘텐츠 사업자가 웹 플랫폼을 사용하지 않을 것

#### 6. 표준의 파편화
- W3C, WHATWG, IETF, Ecma 등 여러 기구에 걸친 표준의 분산
- 개발자 입장에서 어디를 참고해야 할지 혼란
- 2019년 WHATWG 합의로 일부 해소되었으나 완전히 해결되지는 않음

---

## 10. 개발자에게 미치는 영향과 중요성

### 왜 개발자가 W3C를 알아야 하는가

#### 1. 웹 표준 = 개발의 기준선

```
개발자의 코드
     │
     ▼
웹 표준 (W3C/WHATWG)     ←── 코드가 올바르게 작동하는지의 기준
     │
     ▼
브라우저 구현              ←── 표준을 얼마나 잘 구현했는지가 호환성
     │
     ▼
사용자 경험
```

- W3C 표준을 따르면 크로스 브라우저 호환성이 보장된다
- 특정 브라우저에 종속된(vendor-specific) 코드를 작성할 필요가 줄어든다

#### 2. 표준 문서를 읽는 능력

W3C 스펙을 직접 읽을 수 있으면:
- MDN 등 2차 문서에 오류가 있을 때 원본으로 검증 가능
- 새로운 API의 정확한 동작을 파악할 수 있음
- 엣지 케이스에 대한 명확한 답을 찾을 수 있음

#### 3. 실질적 영향을 주는 주요 표준들

| 분야 | 관련 표준 | 개발자에게 미치는 영향 |
|------|----------|---------------------|
| 프론트엔드 개발 | HTML, CSS, DOM, Web APIs | 일상적 코드의 기반 |
| 성능 최적화 | Performance API, Resource Hints, Preload | 로딩 성능 측정/개선 |
| PWA 개발 | Service Workers, Web App Manifest, Push API | 오프라인/설치 가능한 웹 앱 |
| 실시간 통신 | WebRTC, WebSocket | 화상회의, 채팅, 협업 도구 |
| 접근성 | WCAG, WAI-ARIA | 법적 준수, 사용자 경험 향상 |
| 보안 | Web Crypto, CSP, Permissions Policy | 안전한 웹 애플리케이션 |
| 고성능 컴퓨팅 | WebAssembly, WebGPU | 네이티브급 성능의 웹 앱 |
| SEO | JSON-LD, Schema.org (Semantic Web 기반) | 검색 엔진 최적화 |

#### 4. 표준화 프로세스 참여

개발자도 W3C 표준화에 기여할 수 있다:

| 방법 | 난이도 | 설명 |
|------|--------|------|
| GitHub Issue 제출 | 낮음 | 대부분의 W3C 스펙은 GitHub에서 관리됨. 버그/제안 제출 |
| Community Group 참여 | 낮음 | WICG 등 누구나 참여 가능한 그룹에서 새 기능 논의 |
| Web Platform Tests 기여 | 중간 | 표준 구현 테스트 작성 (https://github.com/web-platform-tests/wpt) |
| 스펙 리뷰/피드백 | 중간 | Wide Review 기간에 기술적 피드백 제공 |
| Working Group 참여 | 높음 | W3C 회원 기관을 통해 WG에 참여 |

#### 5. 개발 도구에서의 활용

```
# 표준 적합성 검증 도구
- W3C Markup Validation Service (https://validator.w3.org/)
- W3C CSS Validation Service (https://jigsaw.w3.org/css-validator/)
- WAVE (Web Accessibility Evaluation Tool)
- axe-core (자동화된 접근성 테스트)
- Lighthouse (Chrome DevTools 내장)

# 표준 구현 상태 확인
- Can I Use (https://caniuse.com/) - 브라우저별 표준 지원 현황
- MDN Web Docs (https://developer.mozilla.org/) - 표준 기반 API 문서
- Chrome Platform Status - Chrome의 표준 구현 상태
- WebKit Feature Status - Safari의 표준 구현 상태
```

#### 6. 미래의 웹 표준 동향

개발자가 주목해야 할 진행 중인 표준:

| 표준 | 상태 | 의미 |
|------|------|------|
| WebGPU | 진행 중 | 차세대 GPU API (WebGL 후속) |
| CSS Container Queries | 안정화 | 부모 컨테이너 기반 반응형 디자인 |
| View Transitions API | 진행 중 | 네이티브급 페이지 전환 애니메이션 |
| Speculation Rules API | 진행 중 | 프리렌더링/프리패치 최적화 |
| Web Components v2 | 진행 중 | 컴포넌트 모델 개선 |
| WCAG 3.0 | 초기 WD | 차세대 접근성 가이드라인 |
| WebAssembly 2.0 | 진행 중 | GC, 스레드, SIMD 등 확장 |
| WebTransport | 진행 중 | HTTP/3 기반 양방향 전송 |
| Federated Credential Management | 진행 중 | 프라이버시 보호형 인증 |

### 핵심 요약

> W3C는 단순한 표준 문서 발행 기관이 아니다. 웹이라는 플랫폼의 설계 철학과 방향을 결정하는 곳이다. 개발자로서 W3C 표준을 이해하는 것은 "규칙을 아는 것"이 아니라, 웹이라는 플랫폼이 왜 이렇게 설계되었는지를 이해하는 것이다.

---

## 참고 자료

- W3C 공식 사이트: https://www.w3.org/
- W3C Standards and Drafts: https://www.w3.org/TR/
- W3C Process Document: https://www.w3.org/policies/process/
- WHATWG HTML Living Standard: https://html.spec.whatwg.org/
- WCAG 2.2: https://www.w3.org/TR/WCAG22/
- WAI-ARIA 1.2: https://www.w3.org/TR/wai-aria-1.2/
- WebAssembly: https://www.w3.org/TR/wasm-core-2/
- W3C/WHATWG 합의문: https://www.w3.org/2019/04/WHATWG-W3C-MOU.html
- Web Platform Tests: https://github.com/web-platform-tests/wpt
- Can I Use: https://caniuse.com/
- MDN Web Docs: https://developer.mozilla.org/
