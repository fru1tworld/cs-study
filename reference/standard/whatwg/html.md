# WHATWG HTML Living Standard

## 1. 개요

### HTML Living Standard란

WHATWG(Web Hypertext Application Technology Working Group)가 관리하는 HTML 명세로, 버전 번호 없이 지속적으로 업데이트되는 "살아있는 표준(Living Standard)"이다. 웹 브라우저 벤더(Apple, Google, Mozilla, Microsoft)가 주도하며, 현재 HTML의 유일한 공식 표준이다.

- 명세 URL: https://html.spec.whatwg.org/
- 2004년 WHATWG 설립 (W3C의 XHTML 2.0 방향에 반대)
- 2019년 W3C와 WHATWG가 합의하여 WHATWG HTML을 유일한 HTML 표준으로 인정

### W3C HTML5와의 차이

| 항목 | W3C HTML5 | WHATWG HTML Living Standard |
|------|-----------|----------------------------|
| 버전 관리 | 스냅샷(HTML5, 5.1, 5.2) | 버전 없음, 지속 업데이트 |
| 상태 | 2021년 이후 개발 중단 | 현재 유일한 공식 표준 |
| 범위 | HTML 마크업 중심 | HTML + 관련 API 전체 포함 |
| 갱신 주기 | 수년 단위 | 수시(거의 매일) |

---

## 2. 문서 구조

### 기본 구조

```html
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
```

### DOCTYPE

`<!DOCTYPE html>`은 브라우저를 표준 모드(Standards Mode)로 동작시키기 위해 필요하다. 생략하면 호환 모드(Quirks Mode)로 렌더링되어 예측 불가능한 동작이 발생한다.

### `<html>` 요소

문서의 루트 요소. `lang` 속성으로 문서 언어를 명시한다.

```html
<html lang="ko">    <!-- 한국어 -->
<html lang="en-US"> <!-- 미국 영어 -->
```

### `<head>` 메타데이터

```html
<head>
  <!-- 문자 인코딩 (반드시 첫 1024바이트 안에 위치) -->
  <meta charset="UTF-8">

  <!-- 뷰포트 설정 (반응형 필수) -->
  <meta name="viewport" content="width=device-width, initial-scale=1.0">

  <!-- SEO -->
  <meta name="description" content="페이지 설명">
  <meta name="robots" content="index, follow">

  <!-- 외부 리소스 -->
  <link rel="stylesheet" href="style.css">
  <link rel="icon" href="favicon.ico">
  <link rel="canonical" href="https://example.com/page">

  <!-- 리소스 힌트 -->
  <link rel="preload" href="font.woff2" as="font" crossorigin>
  <link rel="preconnect" href="https://cdn.example.com">

  <title>페이지 제목</title>
</head>
```

### Open Graph 프로토콜

소셜 미디어에서 링크 공유 시 미리보기를 제어한다.

```html
<meta property="og:title" content="글 제목">
<meta property="og:description" content="글 설명">
<meta property="og:image" content="https://example.com/image.jpg">
<meta property="og:url" content="https://example.com/page">
<meta property="og:type" content="article">
<meta property="og:locale" content="ko_KR">

<!-- Twitter Card -->
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="글 제목">
```

---

## 3. 시맨틱 요소

시맨틱 요소는 콘텐츠의 의미와 역할을 명확히 전달한다. 접근성(스크린 리더)과 SEO에 직접적 영향을 준다.

### 페이지 레이아웃 구조

```html
<body>
  <header>
    <h1>사이트 이름</h1>
    <nav aria-label="주 메뉴">
      <ul>
        <li><a href="/">홈</a></li>
        <li><a href="/about">소개</a></li>
      </ul>
    </nav>
  </header>

  <main>
    <article>
      <header>
        <h2>글 제목</h2>
        <time datetime="2025-01-15">2025년 1월 15일</time>
      </header>

      <section>
        <h3>섹션 제목</h3>
        <p>본문 내용...</p>
      </section>

      <footer>
        <p>작성자: 홍길동</p>
      </footer>
    </article>

    <aside>
      <h2>관련 글</h2>
      <ul>
        <li><a href="/post-1">관련 글 1</a></li>
      </ul>
    </aside>
  </main>

  <footer>
    <p>&copy; 2025 사이트 이름</p>
    <nav aria-label="푸터 메뉴">
      <a href="/privacy">개인정보 처리방침</a>
    </nav>
  </footer>
</body>
```

### 주요 시맨틱 요소 정리

| 요소 | 역할 | 비고 |
|------|------|------|
| `<header>` | 소개, 내비게이션 그룹 | 여러 번 사용 가능 |
| `<nav>` | 주요 내비게이션 링크 | `aria-label`로 구분 |
| `<main>` | 페이지 핵심 콘텐츠 | 문서당 하나, `hidden` 없는 것은 하나만 |
| `<article>` | 독립적 콘텐츠 단위 | RSS 피드 항목이 될 수 있는 단위 |
| `<section>` | 주제별 그룹 | 제목(`h2`~`h6`)을 포함해야 함 |
| `<aside>` | 부가 정보, 사이드바 | 본문과 간접적으로 관련된 콘텐츠 |
| `<footer>` | 저작권, 연락처, 관련 링크 | 여러 번 사용 가능 |
| `<figure>` | 자체 포함 콘텐츠 | `<figcaption>`과 함께 사용 |
| `<address>` | 연락처 정보 | 가장 가까운 `article`/`body`의 연락처 |
| `<time>` | 날짜/시간 | `datetime` 속성으로 기계 판독 가능 |
| `<mark>` | 강조(하이라이트) | 검색 결과 하이라이트 등 |
| `<hgroup>` | 제목 그룹 | 제목 + 부제목 그룹화 |

---

## 4. 텍스트 요소

### 제목(Headings)

```html
<h1>문서 제목 (페이지당 하나 권장)</h1>
<h2>주요 섹션</h2>
<h3>하위 섹션</h3>
<h4>~</h4><h5>~</h5><h6>최하위</h6>
```

제목 레벨을 건너뛰지 않는다(`h1` 다음 `h3` 사용 금지).

### 문단과 줄바꿈

```html
<p>문단 텍스트. 자동으로 전후에 여백이 생긴다.</p>
<p>두 번째 문단. <br>강제 줄바꿈은 br 사용.</p>
<hr> <!-- 주제 전환을 나타내는 구분선 -->
```

### 인라인 텍스트 시맨틱

```html
<em>강조(보통 이탤릭)</em>
<strong>중요(보통 굵게)</strong>
<small>부가 정보, 법적 고지</small>
<s>더 이상 정확하지 않은 내용</s>
<abbr title="HyperText Markup Language">HTML</abbr>
<code>인라인 코드</code>
<kbd>Ctrl</kbd> + <kbd>C</kbd>  <!-- 키보드 입력 -->
<var>x</var> = <var>y</var> + 2  <!-- 변수 -->
<samp>출력 결과</samp>
<sub>아래첨자</sub> H<sub>2</sub>O
<sup>위첨자</sup> E=mc<sup>2</sup>
<span>의미 없는 인라인 컨테이너 (스타일링용)</span>
<div>의미 없는 블록 컨테이너 (스타일링용)</div>
```

### 하이퍼링크

```html
<a href="https://example.com">외부 링크</a>
<a href="/about">내부 링크</a>
<a href="#section-id">페이지 내 앵커</a>
<a href="mailto:user@example.com">이메일</a>
<a href="tel:+821012345678">전화</a>
<a href="/file.pdf" download>다운로드</a>

<!-- 보안: 외부 링크에서 opener 접근 방지 -->
<a href="https://external.com" rel="noopener noreferrer" target="_blank">
  새 탭에서 열기
</a>
```

### 목록

```html
<!-- 순서 없는 목록 -->
<ul>
  <li>항목 1</li>
  <li>항목 2</li>
</ul>

<!-- 순서 있는 목록 -->
<ol start="3" reversed>
  <li>세 번째부터 역순</li>
  <li value="10">값 지정 가능</li>
</ol>

<!-- 정의 목록 -->
<dl>
  <dt>HTML</dt>
  <dd>웹 페이지의 구조를 정의하는 마크업 언어</dd>

  <dt>CSS</dt>
  <dd>웹 페이지의 스타일을 정의하는 스타일시트 언어</dd>
</dl>
```

### 인용

```html
<!-- 블록 인용 -->
<blockquote cite="https://source.com/article">
  <p>인용된 긴 텍스트...</p>
  <footer>— <cite>출처 이름</cite></footer>
</blockquote>

<!-- 인라인 인용 -->
<p>그는 <q>간결함이 지혜의 핵심</q>이라고 말했다.</p>
```

### 코드 블록

```html
<pre><code class="language-javascript">
function hello() {
  console.log("Hello, World!");
}
</code></pre>
```

---

## 5. 임베디드 콘텐츠

### 이미지

```html
<!-- 기본 -->
<img src="photo.jpg" alt="설명 텍스트" width="800" height="600">

<!-- 반응형: srcset + sizes -->
<img
  src="photo-800.jpg"
  srcset="photo-400.jpg 400w, photo-800.jpg 800w, photo-1200.jpg 1200w"
  sizes="(max-width: 600px) 100vw, 800px"
  alt="풍경 사진"
  loading="lazy"
  decoding="async"
>

<!-- picture: 아트 디렉션 / 포맷 분기 -->
<picture>
  <source srcset="photo.avif" type="image/avif">
  <source srcset="photo.webp" type="image/webp">
  <source srcset="photo-wide.jpg" media="(min-width: 800px)">
  <img src="photo.jpg" alt="사진 설명">
</picture>

<!-- figure와 함께 -->
<figure>
  <img src="chart.png" alt="2024년 매출 차트">
  <figcaption>그림 1. 2024년 분기별 매출 현황</figcaption>
</figure>
```

- `alt`: 필수. 장식용 이미지는 `alt=""`로 빈 문자열 지정
- `loading="lazy"`: 뷰포트 진입 시 로드 (네이티브 지연 로딩)
- `decoding="async"`: 이미지 디코딩을 비동기로 처리

### 비디오

```html
<video
  controls
  width="640"
  height="360"
  poster="thumbnail.jpg"
  preload="metadata"
>
  <source src="video.mp4" type="video/mp4">
  <source src="video.webm" type="video/webm">
  <track
    kind="subtitles"
    src="subs-ko.vtt"
    srclang="ko"
    label="한국어"
    default
  >
  <p>브라우저가 비디오를 지원하지 않습니다.</p>
</video>
```

주요 속성: `autoplay`, `muted`, `loop`, `playsinline` (iOS 인라인 재생)

### 오디오

```html
<audio controls preload="none">
  <source src="audio.mp3" type="audio/mpeg">
  <source src="audio.ogg" type="audio/ogg">
  <p>브라우저가 오디오를 지원하지 않습니다.</p>
</audio>
```

### iframe

```html
<iframe
  src="https://example.com/embed"
  width="600"
  height="400"
  loading="lazy"
  sandbox="allow-scripts allow-same-origin"
  allow="fullscreen; picture-in-picture"
  title="임베디드 콘텐츠 설명"
></iframe>
```

- `sandbox`: 보안 제한 (기본: 모든 것 차단)
- `allow`: Permissions Policy로 기능 제어

### canvas

```html
<canvas id="myCanvas" width="400" height="300">
  캔버스를 지원하지 않는 브라우저용 대체 텍스트
</canvas>

<script>
const canvas = document.getElementById('myCanvas');
const ctx = canvas.getContext('2d');
ctx.fillStyle = '#3498db';
ctx.fillRect(10, 10, 150, 100);
</script>
```

---

## 6. 테이블

테이블은 표 형태의 데이터에만 사용한다. 레이아웃 목적으로 사용하지 않는다.

```html
<table>
  <caption>2024년 분기별 매출</caption>

  <colgroup>
    <col>
    <col span="4" class="data-cols">
  </colgroup>

  <thead>
    <tr>
      <th scope="col">제품</th>
      <th scope="col">1분기</th>
      <th scope="col">2분기</th>
      <th scope="col">3분기</th>
      <th scope="col">4분기</th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <th scope="row">제품 A</th>
      <td>100</td>
      <td>150</td>
      <td>200</td>
      <td>180</td>
    </tr>
    <tr>
      <th scope="row">제품 B</th>
      <td>80</td>
      <td>120</td>
      <td colspan="2">데이터 없음</td>
    </tr>
  </tbody>

  <tfoot>
    <tr>
      <th scope="row">합계</th>
      <td>180</td>
      <td>270</td>
      <td>200</td>
      <td>180</td>
    </tr>
  </tfoot>
</table>
```

| 요소 | 역할 |
|------|------|
| `<caption>` | 테이블 제목 (접근성 필수) |
| `<thead>` | 헤더 행 그룹 |
| `<tbody>` | 본문 행 그룹 (여러 개 가능) |
| `<tfoot>` | 푸터 행 그룹 |
| `<th scope="col/row">` | 헤더 셀, 스크린 리더가 연관 데이터 파악 |
| `colspan`, `rowspan` | 셀 병합 |
| `<colgroup>`, `<col>` | 열 단위 스타일링 |

---

## 7. 폼(Forms)

### 기본 구조

```html
<form action="/api/submit" method="post" novalidate>
  <fieldset>
    <legend>개인 정보</legend>

    <div>
      <label for="name">이름</label>
      <input type="text" id="name" name="name" required>
    </div>

    <div>
      <label for="email">이메일</label>
      <input type="email" id="email" name="email" required>
    </div>
  </fieldset>

  <button type="submit">제출</button>
</form>
```

### input type 전체 목록

```html
<!-- 텍스트 계열 -->
<input type="text" placeholder="일반 텍스트">
<input type="password" minlength="8">
<input type="email" multiple>        <!-- 이메일 (자동 유효성 검사) -->
<input type="url">                   <!-- URL -->
<input type="tel">                   <!-- 전화번호 (모바일 키패드) -->
<input type="search">                <!-- 검색 (X 버튼) -->

<!-- 숫자/범위 -->
<input type="number" min="0" max="100" step="5">
<input type="range" min="0" max="100" value="50">

<!-- 날짜/시간 -->
<input type="date">                  <!-- 날짜 선택기 -->
<input type="time">                  <!-- 시간 -->
<input type="datetime-local">        <!-- 날짜 + 시간 -->
<input type="month">                 <!-- 연-월 -->
<input type="week">                  <!-- 연-주 -->

<!-- 선택 -->
<input type="checkbox" checked>
<input type="radio" name="group" value="a">
<input type="color" value="#ff0000">

<!-- 파일 -->
<input type="file" accept="image/*" multiple>
<input type="file" accept=".pdf,.doc" capture="environment">

<!-- 숨김/특수 -->
<input type="hidden" name="token" value="abc123">
<input type="submit" value="전송">
<input type="reset" value="초기화">
<input type="button" value="일반 버튼">
<input type="image" src="submit.png" alt="전송">
```

### 기타 폼 요소

```html
<!-- 셀렉트 -->
<label for="city">도시</label>
<select id="city" name="city">
  <optgroup label="수도권">
    <option value="seoul">서울</option>
    <option value="incheon">인천</option>
  </optgroup>
  <optgroup label="경상도">
    <option value="busan" selected>부산</option>
  </optgroup>
</select>

<!-- 텍스트영역 -->
<label for="bio">자기소개</label>
<textarea id="bio" name="bio" rows="5" cols="40" maxlength="500"></textarea>

<!-- 데이터리스트 (자동완성) -->
<input type="text" list="frameworks" name="framework">
<datalist id="frameworks">
  <option value="React">
  <option value="Vue">
  <option value="Angular">
  <option value="Svelte">
</datalist>

<!-- 버튼 -->
<button type="submit">제출</button>
<button type="button">일반 동작</button>
<button type="reset">초기화</button>

<!-- 출력 -->
<output name="result" for="a b">60</output>

<!-- 진행률 / 측정값 -->
<progress value="70" max="100">70%</progress>
<meter value="0.7" min="0" max="1" low="0.3" high="0.8" optimum="0.5">70%</meter>
```

### 유효성 검사

#### 선언적 유효성 검사 (HTML 속성)

```html
<input type="text" required>                    <!-- 필수 -->
<input type="text" minlength="2" maxlength="50"> <!-- 길이 제한 -->
<input type="number" min="1" max="100">          <!-- 범위 -->
<input type="text" pattern="[0-9]{3}-[0-9]{4}"> <!-- 정규식 패턴 -->
<input type="email">                             <!-- 빌트인 이메일 검증 -->
```

#### Constraint Validation API

```javascript
const form = document.querySelector('form');
const emailInput = document.querySelector('#email');

// 개별 필드 검사
emailInput.checkValidity();      // boolean 반환
emailInput.reportValidity();     // boolean + UI 표시
emailInput.validity.valid;       // 전체 유효 여부
emailInput.validity.valueMissing; // required 미충족
emailInput.validity.typeMismatch; // type 불일치
emailInput.validity.patternMismatch; // pattern 불일치
emailInput.validity.tooShort;    // minlength 미달
emailInput.validity.tooLong;     // maxlength 초과
emailInput.validity.rangeUnderflow; // min 미달
emailInput.validity.rangeOverflow;  // max 초과
emailInput.validity.stepMismatch;   // step 불일치
emailInput.validationMessage;    // 브라우저 기본 에러 메시지

// 커스텀 에러 메시지
emailInput.setCustomValidity('올바른 이메일 주소를 입력하세요');
emailInput.setCustomValidity(''); // 에러 해제

// 폼 전체 검사
form.addEventListener('submit', (e) => {
  if (!form.checkValidity()) {
    e.preventDefault();
    // 각 필드의 유효성 상태에 따라 에러 표시
  }
});

// CSS 의사 클래스와 연동
// :valid, :invalid, :required, :optional
// :in-range, :out-of-range, :placeholder-shown
// :user-valid, :user-invalid (사용자 상호작용 후에만)
```

### formaction, formmethod

```html
<form action="/default" method="get">
  <input type="text" name="q">
  <button type="submit">검색</button>
  <button type="submit" formaction="/save" formmethod="post">저장</button>
</form>
```

---

## 8. 인터랙티브 요소

### details / summary

자바스크립트 없이 토글 가능한 접기/펼치기 위젯.

```html
<details>
  <summary>자주 묻는 질문</summary>
  <p>여기에 답변 내용이 표시됩니다.</p>
</details>

<details open>
  <summary>기본 펼침 상태</summary>
  <p>open 속성으로 초기 상태를 펼침으로 설정합니다.</p>
</details>
```

```javascript
const details = document.querySelector('details');
details.addEventListener('toggle', () => {
  console.log(details.open ? '열림' : '닫힘');
});
```

### 배타적 아코디언 (name 속성)

같은 `name`을 가진 `<details>` 요소들은 하나만 열린다.

```html
<details name="faq">
  <summary>질문 1</summary>
  <p>답변 1</p>
</details>
<details name="faq">
  <summary>질문 2</summary>
  <p>답변 2</p>
</details>
<details name="faq">
  <summary>질문 3</summary>
  <p>답변 3</p>
</details>
```

### dialog

모달/비모달 대화 상자.

```html
<dialog id="myDialog">
  <form method="dialog">
    <h2>확인</h2>
    <p>정말 삭제하시겠습니까?</p>
    <button value="cancel">취소</button>
    <button value="confirm">확인</button>
  </form>
</dialog>

<button onclick="myDialog.showModal()">모달 열기</button>
```

```javascript
const dialog = document.getElementById('myDialog');

// 모달로 열기 (배경 비활성화, ::backdrop 표시)
dialog.showModal();

// 비모달로 열기
dialog.show();

// 닫기
dialog.close('returnValue');

dialog.addEventListener('close', () => {
  console.log(dialog.returnValue); // "cancel" 또는 "confirm"
});

// ESC 키로 닫힐 때
dialog.addEventListener('cancel', (e) => {
  // e.preventDefault()로 닫힘 방지 가능
});
```

```css
/* 백드롭 스타일링 */
dialog::backdrop {
  background: rgba(0, 0, 0, 0.5);
}
```

---

## 9. 콘텐츠 모델

HTML 요소는 포함할 수 있는 콘텐츠의 종류(카테고리)에 따라 분류된다. 이 모델은 어떤 요소 안에 어떤 요소를 넣을 수 있는지를 정의한다.

### 콘텐츠 카테고리

```
                    ┌─────────────────────┐
                    │  Metadata Content    │
                    │  (head 내부 요소)     │
                    └─────────────────────┘

┌───────────────────────────────────────────────────────┐
│                    Flow Content                        │
│  (body 내부 대부분의 요소)                               │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Phrasing Content                     │  │
│  │  (텍스트 수준의 인라인 요소)                        │  │
│  │                                                   │  │
│  │  ┌──────────────────┐                             │  │
│  │  │ Embedded Content │  img, video, iframe, ...    │  │
│  │  └──────────────────┘                             │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌─────────────────┐  ┌─────────────────────────────┐  │
│  │ Heading Content  │  │  Sectioning Content         │  │
│  │ h1-h6, hgroup   │  │  article, aside, nav,       │  │
│  │                  │  │  section                    │  │
│  └─────────────────┘  └─────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Interactive Content                       │  │
│  │  a, button, details, input, select, textarea, ... │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │          Palpable Content                         │  │
│  │  (렌더링 시 콘텐츠가 존재하는 요소)                  │  │
│  └──────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────┘
```

### 카테고리별 설명

| 카테고리 | 설명 | 주요 요소 |
|----------|------|-----------|
| Metadata | 문서 메타데이터, 동작 설정 | `base`, `link`, `meta`, `script`, `style`, `title` |
| Flow | body 안에 들어갈 수 있는 대부분의 요소 | 거의 모든 요소 |
| Sectioning | 아웃라인을 형성하는 요소 | `article`, `aside`, `nav`, `section` |
| Heading | 섹션의 제목 | `h1`~`h6`, `hgroup` |
| Phrasing | 텍스트 수준 인라인 요소 | `a`, `em`, `strong`, `span`, `img`, `input` 등 |
| Embedded | 외부 리소스 삽입 | `img`, `video`, `audio`, `iframe`, `canvas`, `svg` |
| Interactive | 사용자 상호작용 대상 | `a`(href), `button`, `details`, `input`, `select` |
| Palpable | 비어있지 않은 렌더링 콘텐츠 | 대부분의 Flow/Phrasing (빈 요소 제외) |

### 유효하지 않은 중첩 예시

```html
<!-- 잘못됨: a 안에 a -->
<a href="/a"><a href="/b">링크</a></a>

<!-- 잘못됨: button 안에 button -->
<button><button>버튼</button></button>

<!-- 잘못됨: p 안에 블록 요소 -->
<p><div>블록</div></p>

<!-- 잘못됨: interactive 안에 interactive -->
<a href="/"><button>클릭</button></a>

<!-- 올바름: -->
<a href="/">링크 텍스트</a>
<p><span>인라인 요소는 가능</span></p>
```

### Transparent 콘텐츠 모델

`<a>`, `<ins>`, `<del>` 등은 투명(transparent) 콘텐츠 모델을 가진다. 부모 요소의 콘텐츠 모델을 그대로 상속받는다.

```html
<!-- p(Phrasing만 허용) 안의 a는 Phrasing만 포함 가능 -->
<p><a href="/"><em>텍스트</em></a></p>

<!-- div(Flow 허용) 안의 a는 Flow 포함 가능 -->
<div><a href="/">
  <h2>제목</h2>
  <p>설명</p>
</a></div>
```

---

## 10. 글로벌 속성

모든 HTML 요소에 사용 가능한 속성들.

### 식별 및 분류

```html
<div id="unique-id">유일한 식별자 (문서 내 중복 불가)</div>
<div class="card primary">공백으로 구분된 클래스 목록</div>
<div slot="header">Shadow DOM 슬롯 이름</div>
```

### 커스텀 데이터 속성

```html
<article
  data-id="42"
  data-user-name="홍길동"
  data-is-active="true"
>
  <!-- JavaScript에서 dataset으로 접근 -->
</article>

<script>
const article = document.querySelector('article');
article.dataset.id;        // "42"
article.dataset.userName;  // "홍길동" (camelCase 변환)
article.dataset.isActive;  // "true"
</script>
```

### 접근성 관련

```html
<!-- 탭 순서 제어 -->
<div tabindex="0">포커스 가능 (자연스러운 순서)</div>
<div tabindex="-1">프로그래밍으로만 포커스 가능</div>
<!-- tabindex 양수 값은 사용하지 않는 것이 좋다 -->

<!-- 숨김 -->
<div hidden>완전히 숨김 (렌더링 안 됨)</div>
<div hidden="until-found">검색 시 표시됨 (find-in-page)</div>

<!-- 비활성화 -->
<div inert>상호작용 및 접근성 트리에서 제외</div>

<!-- 접근성 라벨링 -->
<div role="alert" aria-live="polite">알림 영역</div>
<button aria-label="닫기" aria-expanded="false">X</button>
```

### 편집 관련

```html
<!-- 편집 가능 -->
<div contenteditable="true">
  이 영역의 텍스트를 직접 수정할 수 있습니다.
</div>

<!-- 맞춤법 검사 -->
<textarea spellcheck="true"></textarea>

<!-- 입력기(IME) 제어 -->
<input inputmode="numeric">       <!-- 숫자 키패드 -->
<input inputmode="email">         <!-- 이메일 키보드 -->
<input inputmode="url">           <!-- URL 키보드 -->
<input inputmode="tel">           <!-- 전화 키패드 -->
<input inputmode="search">        <!-- 검색 키보드 -->
<input enterkeyhint="send">       <!-- 엔터키 힌트 -->

<!-- 자동 완성 -->
<input autocomplete="name">
<input autocomplete="email">
<input autocomplete="new-password">

<!-- 번역 제어 -->
<code translate="no">console.log()</code>

<!-- 텍스트 방향 -->
<p dir="rtl">오른쪽에서 왼쪽 (아랍어, 히브리어)</p>
<p dir="auto">내용에 따라 자동 결정</p>
```

### 드래그 앤 드롭 / 팝오버

```html
<!-- 드래그 -->
<div draggable="true">드래그 가능한 요소</div>

<!-- 팝오버 API -->
<button popovertarget="mypopover">열기</button>
<div id="mypopover" popover>
  <p>팝오버 콘텐츠</p>
</div>

<!-- 수동 팝오버 (자동 닫힘 없음) -->
<button popovertarget="manual-pop" popovertargetaction="toggle">토글</button>
<div id="manual-pop" popover="manual">
  수동으로 닫아야 합니다.
  <button popovertarget="manual-pop" popovertargetaction="hide">닫기</button>
</div>
```

### 스타일 및 기타

```html
<!-- 인라인 스타일 -->
<div style="color: red; font-size: 16px;">인라인 스타일</div>

<!-- 제목(툴팁) -->
<abbr title="HyperText Markup Language">HTML</abbr>

<!-- 언어 -->
<p lang="en">This paragraph is in English.</p>

<!-- nonce (Content Security Policy) -->
<script nonce="abc123">/* 인라인 스크립트 */</script>

<!-- 자동 포커스 -->
<input autofocus>

<!-- 요소 ID 참조 -->
<input id="input1" aria-describedby="hint1">
<span id="hint1">비밀번호는 8자 이상이어야 합니다.</span>
```

### 글로벌 속성 요약

| 속성 | 용도 |
|------|------|
| `id` | 고유 식별자 |
| `class` | CSS 클래스 |
| `style` | 인라인 스타일 |
| `title` | 부가 설명 (툴팁) |
| `lang` | 콘텐츠 언어 |
| `dir` | 텍스트 방향 (`ltr`, `rtl`, `auto`) |
| `tabindex` | 포커스/탭 순서 |
| `hidden` | 요소 숨김 |
| `inert` | 상호작용 비활성화 |
| `data-*` | 커스텀 데이터 |
| `draggable` | 드래그 가능 여부 |
| `contenteditable` | 편집 가능 여부 |
| `spellcheck` | 맞춤법 검사 |
| `translate` | 번역 대상 여부 |
| `inputmode` | 가상 키보드 유형 |
| `enterkeyhint` | 엔터 키 힌트 |
| `autocomplete` | 자동 완성 |
| `autofocus` | 자동 포커스 |
| `nonce` | CSP용 일회성 토큰 |
| `popover` | 팝오버 API |
| `slot` | Shadow DOM 슬롯 |
| `is` | 커스텀 요소 확장 |
| `part` | Shadow DOM 파트 이름 |
| `exportparts` | Shadow DOM 파트 노출 |

---

Part 2는 [html-apis.md](html-apis.md)에서 계속됩니다.
