# Quirks Mode Standard 상세 가이드

## 목차

1. [개요](#1-개요)
2. [모드 종류](#2-모드-종류)
3. [DOCTYPE에 따른 모드 선택](#3-doctype에-따른-모드-선택)
4. [쿼크 모드의 CSS 차이점](#4-쿼크-모드의-css-차이점)
5. [쿼크 모드의 DOM 차이점](#5-쿼크-모드의-dom-차이점)
6. [Limited-quirks Mode의 특징](#6-limited-quirks-mode의-특징)
7. [모드 확인 방법](#7-모드-확인-방법)
8. [역사적 배경](#8-역사적-배경)
9. [모드 전환의 영향](#9-모드-전환의-영향)
10. [실용 가이드](#10-실용-가이드)
11. [브라우저별 동작 차이](#11-브라우저별-동작-차이)
12. [참고 자료](#12-참고-자료)

---

## 1. 개요

### 쿼크 모드란

Quirks Mode Standard는 WHATWG에서 관리하는 Living Standard로, 웹 브라우저가 오래된 웹 페이지와의 호환성을 유지하기 위해 의도적으로 표준과 다르게 동작하는 렌더링 모드를 정의한다.

1990년대 후반, 웹 표준이 확립되기 전에 만들어진 수많은 웹 페이지들은 당시 브라우저의 비표준 동작에 의존하고 있었다. 새로운 표준을 따르면 이러한 레거시 페이지들이 깨질 수 있었기 때문에, 브라우저들은 문서의 DOCTYPE 선언을 기준으로 표준 모드와 호환성 모드를 전환하는 메커니즘을 도입했다.

```
[핵심 원리: DOCTYPE 스위칭]

<!DOCTYPE html>           → No-quirks Mode (표준 모드)
├── 표준에 맞게 렌더링
└── 현대 웹 페이지에 적합

(DOCTYPE 없음)            → Quirks Mode (쿼크 모드)
├── 레거시 동작으로 렌더링
└── 1990년대 웹 페이지 호환
```

### 왜 "Quirks"인가

"Quirk"은 영어로 "기이한 특성" 또는 "별난 점"을 의미한다. 초기 브라우저들의 비표준 렌더링 동작들을 "quirks"라고 부르며, 이러한 quirks를 재현하는 모드가 "Quirks Mode"이다.

---

## 2. 모드 종류

웹 브라우저는 세 가지 렌더링 모드를 가진다.

### 2.1 Quirks Mode (쿼크 모드)

| 항목 | 설명 |
|------|------|
| 별칭 | 호환성 모드 (Compatibility Mode) |
| 활성화 조건 | DOCTYPE이 없거나 특정 구형 DOCTYPE |
| CSS 박스 모델 | IE 박스 모델 (border-box) |
| 렌더링 | 1990년대 브라우저 동작 재현 |
| `document.compatMode` | `"BackCompat"` |

### 2.2 No-quirks Mode (표준 모드)

| 항목 | 설명 |
|------|------|
| 별칭 | Standards Mode |
| 활성화 조건 | `<!DOCTYPE html>` 또는 완전한 DOCTYPE |
| CSS 박스 모델 | W3C 표준 박스 모델 (content-box) |
| 렌더링 | 웹 표준에 따른 정확한 렌더링 |
| `document.compatMode` | `"CSS1Compat"` |

### 2.3 Limited-quirks Mode (거의 표준 모드)

| 항목 | 설명 |
|------|------|
| 별칭 | Almost Standards Mode |
| 활성화 조건 | Transitional/Frameset DOCTYPE |
| CSS 박스 모델 | W3C 표준 박스 모델 (content-box) |
| 렌더링 | 표준 모드와 거의 동일, 일부 quirks만 적용 |
| `document.compatMode` | `"CSS1Compat"` |

### 2.4 세 모드의 비교

```
[렌더링 모드 스펙트럼]

완전 비표준                                    완전 표준
←────────────────────────────────────────────────→
Quirks Mode    Limited-quirks Mode    No-quirks Mode
(많은 quirks)    (일부 quirks)        (quirks 없음)
```

```
[핵심 차이점 요약]

                    Quirks      Limited-quirks   No-quirks
박스 모델           border-box   content-box      content-box
인라인 이미지 갭    없음         없음(*)          있음(기본)
body 배경 전파      특수 규칙    표준             표준
% height 계산      특수         표준             표준
font-size 상속     특수         표준             표준
스크롤 요소        body         html             html
ID 대소문자        무시         구분             구분

(*) limited-quirks의 핵심 quirk:
    테이블 셀에서의 인라인 요소 기준선 처리
```

---

## 3. DOCTYPE에 따른 모드 선택

### 3.1 모드 선택 알고리즘 개요

브라우저의 HTML 파서는 문서의 시작 부분에서 DOCTYPE 선언을 분석하여 렌더링 모드를 결정한다.

```
[모드 선택 플로우차트]

문서 시작
    ↓
DOCTYPE 존재?
    │
    ├── NO → Quirks Mode
    │
    └── YES
         ↓
    DOCTYPE 분석
         │
         ├── <!DOCTYPE html> (HTML5, 시스템 식별자 없음)
         │   → No-quirks Mode
         │
         ├── 알려진 Quirks DOCTYPE 패턴?
         │   → Quirks Mode
         │
         ├── 알려진 Limited-quirks DOCTYPE 패턴?
         │   → Limited-quirks Mode
         │
         └── 기타
             → No-quirks Mode
```

### 3.2 No-quirks Mode를 유발하는 DOCTYPE

```html
<!-- HTML5 DOCTYPE (가장 권장) -->
<!DOCTYPE html>

<!-- HTML5 DOCTYPE (대소문자 구분 없음) -->
<!DOCTYPE HTML>
<!doctype html>
<!DoCtYpE hTmL>

<!-- 시스템 식별자가 있는 HTML 4.01 Strict -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
    "http://www.w3.org/TR/html4/strict.dtd">

<!-- XHTML 1.0 Strict -->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<!-- XHTML 1.1 -->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
    "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
```

### 3.3 Limited-quirks Mode를 유발하는 DOCTYPE

```html
<!-- HTML 4.01 Transitional (시스템 식별자 있음) -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<!-- HTML 4.01 Frameset (시스템 식별자 있음) -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN"
    "http://www.w3.org/TR/html4/frameset.dtd">

<!-- XHTML 1.0 Transitional -->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<!-- XHTML 1.0 Frameset -->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">
```

### 3.4 Quirks Mode를 유발하는 DOCTYPE

```html
<!-- DOCTYPE 없음 -->
<html>
<head>...</head>
<body>...</body>
</html>

<!-- DOCTYPE 앞에 무언가 있음 (주석 제외) -->
<!-- IE에서는 DOCTYPE 앞의 주석도 quirks를 유발했음 -->

<!-- HTML 4.01 Transitional (시스템 식별자 없음) -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<!-- HTML 3.2 -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">

<!-- HTML 2.0 -->
<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML//EN">

<!-- 알려진 quirks를 유발하는 공개 식별자들 -->
<!DOCTYPE HTML PUBLIC "-//Microsoft//DTD Internet Explorer 3.0 HTML//EN">
<!DOCTYPE HTML PUBLIC "-//Sun Microsystems Corp.//DTD HotJava HTML//EN">
<!DOCTYPE HTML PUBLIC "-//WebTechs//DTD Mozilla HTML//EN">
```

### 3.5 완전한 모드 선택 표

| DOCTYPE 패턴 | 모드 |
|-------------|------|
| `<!DOCTYPE html>` | no-quirks |
| `<!DOCTYPE html SYSTEM "...">` | no-quirks |
| DOCTYPE 없음 | quirks |
| `<!DOCTYPE html SYSTEM "http://www.ibm.com/...">` | quirks |
| HTML 4.01 Strict (시스템 ID 있음) | no-quirks |
| HTML 4.01 Strict (시스템 ID 없음) | no-quirks |
| HTML 4.01 Transitional (시스템 ID 있음) | limited-quirks |
| HTML 4.01 Transitional (시스템 ID 없음) | quirks |
| HTML 4.01 Frameset (시스템 ID 있음) | limited-quirks |
| HTML 4.01 Frameset (시스템 ID 없음) | quirks |
| XHTML 1.0 Strict | no-quirks |
| XHTML 1.0 Transitional | limited-quirks |
| XHTML 1.0 Frameset | limited-quirks |
| XHTML 1.1 | no-quirks |
| HTML 3.2 | quirks |
| HTML 2.0 | quirks |

---

## 4. 쿼크 모드의 CSS 차이점

### 4.1 박스 모델 차이

쿼크 모드에서 가장 중요한 차이점은 CSS 박스 모델이다.

```
[표준 박스 모델 (No-quirks Mode)]
box-sizing: content-box

┌─────────── margin ──────────┐
│ ┌──────── border ─────────┐ │
│ │ ┌───── padding ───────┐ │ │
│ │ │ ┌── content ──────┐ │ │ │
│ │ │ │                 │ │ │ │
│ │ │ │  width × height │ │ │ │
│ │ │ │                 │ │ │ │
│ │ │ └─────────────────┘ │ │ │
│ │ └─────────────────────┘ │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘

width와 height는 content 영역만을 의미
전체 너비 = width + padding-left + padding-right + border-left + border-right


[IE 박스 모델 (Quirks Mode)]
box-sizing: border-box

┌─────────── margin ──────────┐
│ ┌──────── border ─────────┐ │
│ │ ┌───── padding ───────┐ │ │
│ │ │ ┌── content ──────┐ │ │ │
│ │ │ │                 │ │ │ │
│ │ │ │                 │ │ │ │
│ │ │ │                 │ │ │ │
│ │ │ └─────────────────┘ │ │ │
│ │ └─────────────────────┘ │ │
│ └─────────────────────────┘ │
└─────────────────────────────┘
      ← width × height →

width와 height는 border까지 포함
content 너비 = width - padding-left - padding-right - border-left - border-right
```

```css
/* 실제 차이 예시 */
.box {
    width: 200px;
    padding: 20px;
    border: 5px solid black;
}

/* No-quirks Mode (표준):
   전체 너비 = 200 + 20*2 + 5*2 = 250px
   content 너비 = 200px
*/

/* Quirks Mode:
   전체 너비 = 200px
   content 너비 = 200 - 20*2 - 5*2 = 150px
*/
```

### 4.2 테이블 셀 높이 계산

```css
/* 쿼크 모드에서의 테이블 셀 높이 */

/* No-quirks Mode: height는 최소 높이로 동작 */
td {
    height: 50px;
    /* 내용이 50px보다 크면 셀이 확장됨 */
}

/* Quirks Mode: height가 고정 높이로 동작할 수 있음 */
td {
    height: 50px;
    /* 내용이 잘릴 수 있음 */
}
```

### 4.3 인라인 요소의 크기

```css
/* 쿼크 모드에서 인라인 요소에 width/height 적용 가능 */

/* No-quirks Mode: span에 width/height 무시됨 */
span {
    width: 100px;   /* 무시됨 */
    height: 50px;   /* 무시됨 */
}

/* Quirks Mode: 일부 인라인 요소에 width/height가 적용됨 */
span {
    width: 100px;   /* 적용될 수 있음 */
    height: 50px;   /* 적용될 수 있음 */
}
```

### 4.4 Percentage Height 계산

```css
/* 부모 요소에 명시적 높이가 없을 때의 percentage height */

.parent {
    /* height 미지정 */
}

.child {
    height: 50%;
}

/* No-quirks Mode:
   부모에 명시적 높이가 없으면 height: 50%는 auto로 처리됨
   → percentage height가 동작하지 않음
*/

/* Quirks Mode:
   부모에 명시적 높이가 없어도 percentage height가
   뷰포트나 가장 가까운 높이를 가진 조상을 기준으로 계산될 수 있음
*/
```

### 4.5 Body의 Margin

```css
/* body 요소의 기본 마진 */

/* No-quirks Mode:
   body { margin: 8px; }  (UA 스타일 시트에 의해)
*/

/* Quirks Mode:
   body의 마진 처리가 약간 다를 수 있음
   특히 body의 margin이 다른 요소에 영향을 미치는 방식이 다름
*/
```

### 4.6 Font Size 상속

```css
/* 테이블에서의 폰트 크기 상속 */

body {
    font-size: 14px;
}

table {
    /* No-quirks Mode: body의 font-size를 상속받음 → 14px */
    /* Quirks Mode: 브라우저 기본 font-size를 사용할 수 있음 → medium (~16px) */
}

/* 해결 방법: 명시적으로 지정 */
table {
    font-size: inherit;  /* 또는 font-size: 100%; */
}
```

### 4.7 이미지 주변의 공백

```css
/* 테이블 셀 내 이미지 아래의 빈 공간 */

/* No-quirks Mode:
   이미지는 인라인 요소이므로 기준선(baseline) 위에 배치됨
   기준선 아래에 디센더(descender) 공간이 있음
   → 이미지 아래에 약간의 빈 공간 발생
*/

/* Quirks Mode:
   이미지 아래에 빈 공간이 없음
   → 많은 구형 테이블 레이아웃이 이 동작에 의존
*/

/* 표준 모드에서 이미지 아래 공간 제거 방법: */
img {
    display: block;          /* 방법 1: 블록으로 변경 */
    /* 또는 */
    vertical-align: bottom;  /* 방법 2: 수직 정렬 변경 */
}
```

### 4.8 배경색 전파

```css
/* body와 html의 배경색 전파 */

/* No-quirks Mode:
   html 요소에 배경이 없으면 body의 배경이 캔버스에 전파됨
   html 요소에 배경이 있으면 body의 배경은 body에만 적용됨
*/

/* Quirks Mode:
   배경색 전파 규칙이 약간 다를 수 있음
   특히 body에만 배경색을 지정한 경우의 동작이 다름
*/

html {
    /* 명시적으로 배경 지정 권장 */
    background-color: #fff;
}

body {
    background-color: #f5f5f5;
}
```

### 4.9 CSS 파싱 차이

```css
/* 쿼크 모드에서의 CSS 파싱 차이 */

/* 1. 단위 없는 숫자를 px로 해석 */
/* No-quirks Mode: */
.element { width: 100; }   /* 무효 → 무시됨 */

/* Quirks Mode: */
.element { width: 100; }   /* 100px로 해석됨 */

/* 2. 색상값 해석 */
/* No-quirks Mode: */
.element { color: chucknorris; }  /* 무효 → 무시됨 */

/* Quirks Mode: */
.element { color: chucknorris; }  /* 유효한 색상으로 해석 시도 */
/* "chucknorris"를 16진수로 파싱: c0c000000s → #c00000 (적색) */

/* 3. 잘못된 구문 처리가 더 관대함 */
```

### 4.10 전체 CSS 차이점 요약표

| CSS 동작 | No-quirks Mode | Quirks Mode |
|----------|---------------|-------------|
| 박스 모델 | content-box | border-box |
| % height (부모 height 없을 때) | auto로 처리 | 가장 가까운 높이 기준 |
| 테이블 font-size 상속 | 부모로부터 상속 | 브라우저 기본값 사용 가능 |
| 인라인 이미지 아래 공간 | 있음 (baseline) | 없음 |
| 단위 없는 길이값 | 무효 (무시) | px로 해석 |
| 잘못된 색상값 | 무효 (무시) | 16진수 파싱 시도 |
| body 배경 전파 | 표준 규칙 | 특수 규칙 |
| :hover on non-links | 동작 | 제한적 |
| 인라인 요소 크기 지정 | 불가 | 일부 가능 |
| table height | min-height 의미 | 고정 height 가능 |

---

## 5. 쿼크 모드의 DOM 차이점

### 5.1 document.body 스크롤

```javascript
// 스크롤 관련 속성의 차이

// No-quirks Mode: 스크롤은 document.documentElement (html 요소)에서 제어
document.documentElement.scrollTop;      // 현재 스크롤 위치
document.documentElement.scrollHeight;   // 전체 스크롤 높이
document.documentElement.clientHeight;   // 뷰포트 높이

// Quirks Mode: 스크롤은 document.body에서 제어
document.body.scrollTop;                 // 현재 스크롤 위치
document.body.scrollHeight;              // 전체 스크롤 높이
document.body.clientHeight;              // 뷰포트 높이

// 크로스 모드 호환 코드:
function getScrollTop() {
    return document.documentElement.scrollTop || document.body.scrollTop;
}

function getScrollHeight() {
    return Math.max(
        document.documentElement.scrollHeight,
        document.body.scrollHeight
    );
}

function getViewportHeight() {
    return document.documentElement.clientHeight || document.body.clientHeight;
}

// 스크롤 이동
function scrollTo(top) {
    if (document.compatMode === 'CSS1Compat') {
        // No-quirks mode
        document.documentElement.scrollTop = top;
    } else {
        // Quirks mode
        document.body.scrollTop = top;
    }
    // 또는 가장 안전한 방법:
    window.scrollTo(0, top);
}
```

### 5.2 getElementById 대소문자

```javascript
// getElementById의 대소문자 구분

// HTML:
// <div id="MyElement">...</div>

// No-quirks Mode: 대소문자를 정확하게 구분
document.getElementById('MyElement');   // → <div> 찾음
document.getElementById('myelement');   // → null (대소문자 불일치)
document.getElementById('MYELEMENT');   // → null

// Quirks Mode: 대소문자를 구분하지 않을 수 있음 (브라우저마다 다름)
document.getElementById('MyElement');   // → <div> 찾음
document.getElementById('myelement');   // → <div> 찾을 수 있음!
document.getElementById('MYELEMENT');   // → <div> 찾을 수 있음!
```

### 5.3 className 기반 매칭

```javascript
// getElementsByClassName의 대소문자 구분

// HTML:
// <div class="MyClass">...</div>

// No-quirks Mode: 대소문자를 정확하게 구분
document.getElementsByClassName('MyClass');   // → 찾음
document.getElementsByClassName('myclass');   // → 못 찾음

// Quirks Mode: 대소문자를 구분하지 않음
document.getElementsByClassName('MyClass');   // → 찾음
document.getElementsByClassName('myclass');   // → 찾음
document.getElementsByClassName('MYCLASS');   // → 찾음
```

### 5.4 CSS 선택자의 대소문자

```css
/* ID 선택자와 클래스 선택자의 대소문자 */

/* HTML: <div id="MyId" class="MyClass"> */

/* No-quirks Mode: 정확한 대소문자 매칭 */
#MyId { color: red; }     /* 적용됨 */
#myid { color: blue; }    /* 적용되지 않음 */

.MyClass { font-size: 14px; }   /* 적용됨 */
.myclass { font-size: 16px; }   /* 적용되지 않음 */

/* Quirks Mode: 대소문자 무시 */
#MyId { color: red; }     /* 적용됨 */
#myid { color: blue; }    /* 이것도 적용됨! */

.MyClass { font-size: 14px; }   /* 적용됨 */
.myclass { font-size: 16px; }   /* 이것도 적용됨! */
```

### 5.5 document.all

```javascript
// document.all의 동작
// (이것은 모든 모드에서 동일하지만 quirks 시대의 유산)

// document.all은 "falsy한 객체"라는 특수한 동작을 함
if (document.all) {
    // 이 블록은 실행되지 않음! (falsy)
    // 하지만 document.all은 실제로 존재하고 사용 가능
}

typeof document.all; // "undefined" (특수 동작)
document.all[0];     // 첫 번째 요소 (정상 동작)

// 이유: 과거 IE 감지 코드와의 호환성
// if (document.all) { /* IE 전용 코드 */ }
// 이런 코드가 현대 브라우저에서 실행되지 않도록
```

### 5.6 DOM 차이점 요약표

| DOM 동작 | No-quirks Mode | Quirks Mode |
|----------|---------------|-------------|
| `scrollTop` 소유자 | `documentElement` | `body` |
| `scrollHeight` 소유자 | `documentElement` | `body` |
| `clientHeight` 소유자 | `documentElement` | `body` |
| `getElementById` 대소문자 | 구분함 | 구분하지 않을 수 있음 |
| `getElementsByClassName` 대소문자 | 구분함 | 구분하지 않음 |
| CSS ID/class 선택자 대소문자 | 구분함 | 구분하지 않음 |
| `document.compatMode` | `"CSS1Compat"` | `"BackCompat"` |

---

## 6. Limited-quirks Mode의 특징

### 6.1 개요

Limited-quirks mode는 "Almost Standards Mode"라고도 불리며, 표준 모드와 거의 동일하지만 딱 하나의 주요 quirk를 포함한다.

### 6.2 인라인 이미지의 기준선 처리

이것이 limited-quirks mode의 유일한(그리고 핵심적인) quirk이다.

```css
/* 테이블 셀 내에서의 인라인 이미지/요소 배치 */

/* No-quirks Mode:
   인라인 요소(이미지 포함)는 기준선(baseline) 위에 배치됨
   기준선 아래에 디센더(g, j, p, q, y 등의 아래로 내려가는 부분)
   공간이 존재
   → 이미지 아래에 작은 빈 공간이 생김
*/

/* Limited-quirks Mode:
   테이블 셀의 유일한 콘텐츠가 인라인 요소들일 때,
   셀의 기준선을 셀의 하단으로 설정
   → 이미지 아래의 빈 공간이 제거됨
   → 많은 테이블 기반 레이아웃이 이 동작에 의존
*/
```

```
[시각적 차이]

No-quirks Mode:
┌──────────────────────────┐
│  ┌──────────────────┐    │
│  │                  │    │  ← 테이블 셀
│  │     이미지       │    │
│  │                  │    │
│  └──────────────────┘    │
│  ═══════════════════  ← 기준선
│  ___________________  ← 디센더 공간 (빈 공간)
└──────────────────────────┘

Limited-quirks Mode:
┌──────────────────────────┐
│  ┌──────────────────┐    │
│  │                  │    │  ← 테이블 셀
│  │     이미지       │    │
│  │                  │    │
│  └──────────────────┘    │  ← 기준선이 하단으로 (빈 공간 없음)
└──────────────────────────┘
```

### 6.3 왜 Limited-quirks Mode가 필요한가

```
[역사적 이유]

1990-2000년대 웹 디자인:
├── 테이블 기반 레이아웃이 주류
├── 이미지를 잘라서 테이블 셀에 배치 (슬라이스 이미지)
├── 각 셀에 이미지를 정확하게 맞추는 것이 중요
└── 이미지 아래의 빈 공간이 레이아웃을 깨뜨림

예시: 이미지 슬라이스 레이아웃
┌────┬────┬────┐
│img1│img2│img3│  ← 빈 공간 없이 정확하게 붙어야 함
├────┼────┼────┤
│img4│img5│img6│  ← 셀 사이에 틈이 있으면 레이아웃 깨짐
├────┼────┼────┤
│img7│img8│img9│
└────┴────┴────┘

표준 모드에서는 각 이미지 아래에 3-5px 공간 발생
→ 레이아웃 완전히 깨짐!

limited-quirks mode로 이 문제만 해결
```

### 6.4 Limited-quirks Mode와 No-quirks Mode의 동작 차이

```html
<!-- 이 차이를 보여주는 예제 -->
<table>
    <tr>
        <td style="background: lightblue;">
            <img src="image.jpg" width="100" height="100">
        </td>
    </tr>
</table>

<!-- No-quirks Mode:
     td의 높이 ≈ 104px (이미지 100px + 디센더 공간 ~4px)
     이미지 아래에 작은 파란색 공간이 보임
-->

<!-- Limited-quirks Mode:
     td의 높이 = 100px (이미지만큼 정확하게)
     이미지 아래에 빈 공간 없음
-->
```

---

## 7. 모드 확인 방법

### 7.1 JavaScript로 확인

```javascript
// document.compatMode 사용
const mode = document.compatMode;

if (mode === 'CSS1Compat') {
    console.log('No-quirks Mode 또는 Limited-quirks Mode');
} else if (mode === 'BackCompat') {
    console.log('Quirks Mode');
}

// 참고: document.compatMode는 limited-quirks와 no-quirks를 구분하지 못함
// 둘 다 "CSS1Compat"을 반환함
```

### 7.2 개발자 도구에서 확인

```
[Chrome/Edge]
1. F12로 개발자 도구 열기
2. Elements 탭 선택
3. <!DOCTYPE> 위의 문서 노드 클릭
4. 또는 Console에서 document.compatMode 입력

[Firefox]
1. F12로 개발자 도구 열기
2. Console에서 document.compatMode 입력
3. 또는 페이지 정보 (Ctrl+I) → 일반 탭에서 "렌더링 모드" 확인

[Safari]
1. 개발 메뉴 → 웹 인스펙터 열기
2. Console에서 document.compatMode 입력
```

### 7.3 프로그래밍적 모드 감지 유틸리티

```javascript
function detectRenderingMode() {
    const compatMode = document.compatMode;
    const doctype = document.doctype;

    let mode;
    let details;

    if (compatMode === 'BackCompat') {
        mode = 'quirks';
        details = 'Quirks Mode (비표준 렌더링)';
    } else if (compatMode === 'CSS1Compat') {
        // limited-quirks와 no-quirks를 DOCTYPE으로 구분 시도
        if (doctype) {
            const publicId = doctype.publicId;
            const systemId = doctype.systemId;

            if (!publicId && !systemId) {
                mode = 'no-quirks';
                details = 'No-quirks Mode (HTML5 DOCTYPE)';
            } else if (
                publicId.includes('Transitional') ||
                publicId.includes('Frameset')
            ) {
                mode = 'limited-quirks';
                details = 'Limited-quirks Mode (Transitional/Frameset DOCTYPE)';
            } else {
                mode = 'no-quirks';
                details = 'No-quirks Mode (표준 DOCTYPE)';
            }
        } else {
            mode = 'no-quirks';
            details = 'No-quirks Mode (DOCTYPE 정보 없음)';
        }
    }

    return {
        mode,
        details,
        compatMode,
        doctype: doctype ? {
            name: doctype.name,
            publicId: doctype.publicId,
            systemId: doctype.systemId
        } : null
    };
}

console.log(detectRenderingMode());
// {
//   mode: "no-quirks",
//   details: "No-quirks Mode (HTML5 DOCTYPE)",
//   compatMode: "CSS1Compat",
//   doctype: { name: "html", publicId: "", systemId: "" }
// }
```

### 7.4 DOCTYPE 정보 확인

```javascript
// document.doctype 객체
const doctype = document.doctype;

if (doctype) {
    console.log('이름:', doctype.name);       // "html"
    console.log('공개 ID:', doctype.publicId); // "" (HTML5) 또는 DTD 정보
    console.log('시스템 ID:', doctype.systemId); // "" (HTML5) 또는 DTD URL
} else {
    console.log('DOCTYPE이 없습니다. (Quirks Mode)');
}
```

---

## 8. 역사적 배경

### 8.1 타임라인

```
[Quirks Mode의 역사]

1995: HTML 2.0, 초기 웹 브라우저
      └── Netscape Navigator, Internet Explorer가 각자의 렌더링 방식 사용

1996: CSS 1.0 발표
      └── 브라우저들이 CSS를 각자 다르게 구현

1997-1998: "브라우저 전쟁" 절정
      ├── IE 4.0 vs Netscape 4.0
      ├── 각 브라우저가 비표준 기능 추가
      └── 웹 페이지들이 특정 브라우저에 최적화됨

1998: CSS 2.0 발표
      └── 정확한 박스 모델 정의
      └── IE의 박스 모델과 충돌 (IE Box Model Bug)

2000: IE 6 출시
      ├── DOCTYPE 스위칭 최초 도입
      ├── DOCTYPE이 있으면 "standards mode"
      └── DOCTYPE이 없으면 "quirks mode" (IE 5.5 호환)

2000-2001: 다른 브라우저들도 DOCTYPE 스위칭 채택
      ├── Mozilla/Firefox
      ├── Opera
      └── Safari

2001: "Almost Standards Mode" 도입 (Firefox)
      └── Transitional DOCTYPE에 대한 중간 단계

2004: WHATWG 설립
      └── 렌더링 모드를 공식 표준으로 문서화 시작

2011: Quirks Mode Standard 최초 공식 문서화 (WHATWG)

현재: Living Standard로 지속 관리
```

### 8.2 IE Box Model Bug

```
[IE Box Model Bug - Quirks Mode의 가장 유명한 차이]

W3C 표준:           width: 200px;
┌──────────────────────────────────────────────┐
│  margin                                      │
│  ┌────────────────────────────────────────┐  │
│  │  border (5px)                          │  │
│  │  ┌──────────────────────────────────┐  │  │
│  │  │  padding (20px)                  │  │  │
│  │  │  ┌──────────────────────────┐    │  │  │
│  │  │  │  content: 200px          │    │  │  │
│  │  │  └──────────────────────────┘    │  │  │
│  │  └──────────────────────────────────┘  │  │
│  └────────────────────────────────────────┘  │
│  전체 너비: 200 + 40 + 10 = 250px            │
└──────────────────────────────────────────────┘

IE 5.x (Quirks):    width: 200px;
┌──────────────────────────────────────────────┐
│  margin                                      │
│  ┌──────────────────────────────────┐        │
│  │  border (5px) ─ width: 200px ─→ │        │
│  │  ┌──────────────────────────┐    │        │
│  │  │  padding (20px)          │    │        │
│  │  │  ┌──────────────────┐    │    │        │
│  │  │  │  content: 150px  │    │    │        │
│  │  │  └──────────────────┘    │    │        │
│  │  └──────────────────────────┘    │        │
│  └──────────────────────────────────┘        │
│  전체 너비: 200px                             │
└──────────────────────────────────────────────┘

* CSS3에서 box-sizing: border-box로 이 동작을 표준화함
* 현대 CSS 리셋에서 * { box-sizing: border-box; }를 사용하는 이유
```

---

## 9. 모드 전환의 영향

### 9.1 모드 변경 시 영향 범위

```
[Quirks Mode가 영향을 미치는 범위]

CSS 렌더링
├── 박스 모델
├── 높이 계산
├── 폰트 크기 상속
├── 색상 파싱
├── 단위 파싱
├── 배경 전파
└── 인라인 요소 처리

DOM/JavaScript
├── 스크롤 관련 속성
├── 요소 검색 대소문자
├── CSS 선택자 대소문자
└── document.compatMode

레이아웃
├── 테이블 레이아웃
├── 이미지 간격
└── 폼 요소 스타일링
```

### 9.2 Quirks Mode에서 No-quirks Mode로 전환 시

```html
<!-- 문제 시나리오: DOCTYPE 추가 후 레이아웃이 깨지는 경우 -->

<!-- 변경 전 (Quirks Mode) -->
<html>
<head>
    <style>
        .container { width: 300px; padding: 20px; border: 5px solid; }
        /* Quirks: 전체 너비 = 300px */
    </style>
</head>
<body>...</body>
</html>

<!-- 변경 후 (No-quirks Mode) -->
<!DOCTYPE html>
<html>
<head>
    <style>
        .container { width: 300px; padding: 20px; border: 5px solid; }
        /* Standards: 전체 너비 = 300 + 40 + 10 = 350px */
        /* 레이아웃이 50px 넓어져서 깨질 수 있음! */
    </style>
</head>
<body>...</body>
</html>

<!-- 해결 방법: box-sizing 추가 -->
<!DOCTYPE html>
<html>
<head>
    <style>
        *, *::before, *::after {
            box-sizing: border-box; /* Quirks와 같은 박스 모델 사용 */
        }
        .container { width: 300px; padding: 20px; border: 5px solid; }
        /* 이제 전체 너비 = 300px (Quirks와 동일) */
    </style>
</head>
<body>...</body>
</html>
```

### 9.3 마이그레이션 체크리스트

```
[Quirks Mode → No-quirks Mode 마이그레이션 체크리스트]

□ 1. <!DOCTYPE html> 추가
□ 2. box-sizing: border-box 적용
     *, *::before, *::after { box-sizing: border-box; }
□ 3. 스크롤 관련 코드 확인
     document.body.scrollTop → document.documentElement.scrollTop
□ 4. 테이블 레이아웃의 이미지 간격 확인
     이미지에 display: block 또는 vertical-align: bottom 추가
□ 5. 테이블의 font-size 상속 확인
     table { font-size: inherit; } 추가
□ 6. 단위 없는 숫자 값 수정
     width: 100 → width: 100px
□ 7. ID/class 대소문자 일관성 확인
     HTML과 CSS/JS에서 동일한 대소문자 사용
□ 8. 비표준 CSS 속성/값 수정
□ 9. 전체 레이아웃 테스트
□ 10. 다양한 브라우저에서 확인
```

---

## 10. 실용 가이드

### 10.1 올바른 DOCTYPE 사용

```html
<!-- 현대 웹 개발에서 사용해야 할 DOCTYPE -->
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>페이지 제목</title>
</head>
<body>
    <!-- 콘텐츠 -->
</body>
</html>

<!-- 이것이 유일하게 필요한 DOCTYPE -->
<!-- 더 복잡한 DOCTYPE은 필요하지 않음 -->
```

### 10.2 호환성 있는 CSS 작성

```css
/* 모드에 관계없이 일관된 렌더링을 보장하는 CSS */

/* 1. 글로벌 box-sizing 설정 */
*, *::before, *::after {
    box-sizing: border-box;
}

/* 2. 리셋/정규화 */
html {
    line-height: 1.15;
    -webkit-text-size-adjust: 100%;
}

body {
    margin: 0;
}

/* 3. 이미지의 인라인 간격 제거 */
img, svg, video, canvas, audio, iframe, embed, object {
    display: block;
    max-width: 100%;
}

/* 4. 테이블 폰트 상속 */
table {
    font-size: inherit;
    border-collapse: collapse;
}

/* 5. 폼 요소 폰트 상속 */
input, button, textarea, select {
    font: inherit;
}
```

### 10.3 모드를 고려한 JavaScript 작성

```javascript
// 렌더링 모드에 안전한 유틸리티 함수들

const isQuirksMode = document.compatMode === 'BackCompat';

// 뷰포트 크기
function getViewport() {
    if (isQuirksMode) {
        return {
            width: document.body.clientWidth,
            height: document.body.clientHeight
        };
    }
    return {
        width: document.documentElement.clientWidth,
        height: document.documentElement.clientHeight
    };
}

// 문서 크기
function getDocumentSize() {
    return {
        width: Math.max(
            document.documentElement.scrollWidth,
            document.body.scrollWidth,
            document.documentElement.clientWidth
        ),
        height: Math.max(
            document.documentElement.scrollHeight,
            document.body.scrollHeight,
            document.documentElement.clientHeight
        )
    };
}

// 스크롤 위치
function getScrollPosition() {
    return {
        x: window.pageXOffset || document.documentElement.scrollLeft || document.body.scrollLeft,
        y: window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop
    };
}
```

### 10.4 레거시 코드 진단

```javascript
// 현재 페이지의 렌더링 모드 관련 문제 진단
function diagnoseQuirksIssues() {
    const issues = [];

    // 1. 렌더링 모드 확인
    if (document.compatMode === 'BackCompat') {
        issues.push({
            severity: 'critical',
            message: 'Quirks Mode로 렌더링되고 있습니다. <!DOCTYPE html>을 추가하세요.'
        });
    }

    // 2. DOCTYPE 확인
    if (!document.doctype) {
        issues.push({
            severity: 'critical',
            message: 'DOCTYPE이 없습니다.'
        });
    }

    // 3. box-sizing 확인
    const bodyBoxSizing = getComputedStyle(document.body).boxSizing;
    if (bodyBoxSizing !== 'border-box') {
        issues.push({
            severity: 'info',
            message: 'box-sizing: border-box가 설정되지 않았습니다.'
        });
    }

    // 4. 단위 없는 값 확인 (인라인 스타일)
    const elements = document.querySelectorAll('[style]');
    elements.forEach(el => {
        const style = el.getAttribute('style');
        if (/(?:width|height|margin|padding|font-size)\s*:\s*\d+(?:;|\s|$)/.test(style)) {
            issues.push({
                severity: 'warning',
                message: `단위 없는 CSS 값 발견: ${el.tagName}#${el.id || '(no-id)'}`,
                element: el
            });
        }
    });

    return issues;
}

const issues = diagnoseQuirksIssues();
issues.forEach(issue => {
    console.log(`[${issue.severity.toUpperCase()}] ${issue.message}`);
});
```

---

## 11. 브라우저별 동작 차이

### 11.1 Quirks Mode 구현의 차이

각 브라우저는 Quirks Mode를 약간 다르게 구현한다. Quirks Mode Standard는 가장 일반적인 동작을 표준화하려고 하지만, 모든 세부 사항이 동일하지는 않다.

| Quirk | Chrome | Firefox | Safari | Edge |
|-------|--------|---------|--------|------|
| IE 박스 모델 | 구현 | 구현 | 구현 | 구현 |
| 단위 없는 값 → px | 부분 | 구현 | 부분 | 부분 |
| 대소문자 무시 (getElementById) | 미구현 | 구현 | 미구현 | 미구현 |
| 대소문자 무시 (class 선택자) | 구현 | 구현 | 구현 | 구현 |
| 잘못된 색상값 파싱 | 구현 | 구현 | 구현 | 구현 |
| body 스크롤 | 구현 | 구현 | 구현 | 구현 |
| 테이블 font 상속 | 구현 | 구현 | 구현 | 구현 |
| 인라인 이미지 갭 제거 | 구현 | 구현 | 구현 | 구현 |

### 11.2 document.compatMode 지원

| 브라우저 | 지원 버전 | 비고 |
|----------|----------|------|
| Chrome | 모든 버전 | |
| Firefox | 모든 버전 | |
| Safari | 모든 버전 | |
| Edge | 모든 버전 | |
| IE | 6+ | |

---

## 12. 참고 자료

- [Quirks Mode Standard (WHATWG)](https://quirks.spec.whatwg.org/)
- [MDN - Quirks Mode and Standards Mode](https://developer.mozilla.org/ko/docs/Web/HTML/Quirks_Mode_and_Standards_Mode)
- [MDN - document.compatMode](https://developer.mozilla.org/en-US/docs/Web/API/Document/compatMode)
- [Activating Browser Modes with Doctype (Henri Sivonen)](https://hsivonen.fi/doctype/)
- [Box Model (MDN)](https://developer.mozilla.org/ko/docs/Learn/CSS/Building_blocks/The_box_model)
- [CSS Box Sizing (MDN)](https://developer.mozilla.org/ko/docs/Web/CSS/box-sizing)
