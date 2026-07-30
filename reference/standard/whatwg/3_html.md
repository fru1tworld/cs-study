# WHATWG HTML Living Standard

## WHATWG HTML Living Standard

### 1. 개요

#### HTML Living Standard란

WHATWG(Web Hypertext Application Technology Working Group)가 관리하는 HTML 명세로, 버전 번호 없이 지속적으로 업데이트되는 "살아있는 표준(Living Standard)"이다. 웹 브라우저 벤더(Apple, Google, Mozilla, Microsoft)가 주도하며, 현재 HTML의 유일한 공식 표준이다.

- 명세 URL: https://html.spec.whatwg.org/
- 2004년 WHATWG 설립 (W3C의 XHTML 2.0 방향에 반대)
- 2019년 W3C와 WHATWG가 합의하여 WHATWG HTML을 유일한 HTML 표준으로 인정

#### W3C HTML5와의 차이

| 항목 | W3C HTML5 | WHATWG HTML Living Standard |
|------|-----------|----------------------------|
| 버전 관리 | 스냅샷(HTML5, 5.1, 5.2) | 버전 없음, 지속 업데이트 |
| 상태 | 2019년 이후 개발 중단 | 현재 유일한 공식 표준 |
| 범위 | HTML 마크업 중심 | HTML + 관련 API 전체 포함 |
| 갱신 주기 | 수년 단위 | 수시(거의 매일) |

---

### 2. 문서 구조

#### 기본 구조

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

#### DOCTYPE

`<!DOCTYPE html>`은 브라우저를 표준 모드(Standards Mode)로 동작시키기 위해 필요하다. 생략하면 호환 모드(Quirks Mode)로 렌더링되어 예측 불가능한 동작이 발생한다.

#### `<html>` 요소

문서의 루트 요소. `lang` 속성으로 문서 언어를 명시한다.

```html
<html lang="ko">    <!-- 한국어 -->
<html lang="en-US"> <!-- 미국 영어 -->
```

#### `<head>` 메타데이터

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

#### Open Graph 프로토콜

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

### 3. 시맨틱 요소

시맨틱 요소는 콘텐츠의 의미와 역할을 명확히 전달한다. 접근성(스크린 리더)과 SEO에 직접적 영향을 준다.

#### 페이지 레이아웃 구조

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

#### 주요 시맨틱 요소 정리

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

### 4. 텍스트 요소

#### 제목(Headings)

```html
<h1>문서 제목 (페이지당 하나 권장)</h1>
<h2>주요 섹션</h2>
<h3>하위 섹션</h3>
<h4>~</h4><h5>~</h5><h6>최하위</h6>
```

제목 레벨을 건너뛰지 않는다(`h1` 다음 `h3` 사용 금지).

#### 문단과 줄바꿈

```html
<p>문단 텍스트. 자동으로 전후에 여백이 생긴다.</p>
<p>두 번째 문단. <br>강제 줄바꿈은 br 사용.</p>
<hr> <!-- 주제 전환을 나타내는 구분선 -->
```

#### 인라인 텍스트 시맨틱

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

#### 하이퍼링크

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

#### 목록

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

#### 인용

```html
<!-- 블록 인용 -->
<blockquote cite="https://source.com/article">
  <p>인용된 긴 텍스트...</p>
  <footer>— <cite>출처 이름</cite></footer>
</blockquote>

<!-- 인라인 인용 -->
<p>그는 <q>간결함이 지혜의 핵심</q>이라고 말했다.</p>
```

#### 코드 블록

```html
<pre><code class="language-javascript">
function hello() {
  console.log("Hello, World!");
}
</code></pre>
```

---

### 5. 임베디드 콘텐츠

#### 이미지

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

#### 비디오

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

#### 오디오

```html
<audio controls preload="none">
  <source src="audio.mp3" type="audio/mpeg">
  <source src="audio.ogg" type="audio/ogg">
  <p>브라우저가 오디오를 지원하지 않습니다.</p>
</audio>
```

#### iframe

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

#### canvas

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

### 6. 테이블

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

### 7. 폼(Forms)

#### 기본 구조

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

#### input type 전체 목록

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

#### 기타 폼 요소

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

#### 유효성 검사

##### 선언적 유효성 검사 (HTML 속성)

```html
<input type="text" required>                    <!-- 필수 -->
<input type="text" minlength="2" maxlength="50"> <!-- 길이 제한 -->
<input type="number" min="1" max="100">          <!-- 범위 -->
<input type="text" pattern="[0-9]{3}-[0-9]{4}"> <!-- 정규식 패턴 -->
<input type="email">                             <!-- 빌트인 이메일 검증 -->
```

##### Constraint Validation API

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

#### formaction, formmethod

```html
<form action="/default" method="get">
  <input type="text" name="q">
  <button type="submit">검색</button>
  <button type="submit" formaction="/save" formmethod="post">저장</button>
</form>
```

---

### 8. 인터랙티브 요소

#### details / summary

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

#### 배타적 아코디언 (name 속성)

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

#### dialog

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

### 9. 콘텐츠 모델

HTML 요소는 포함할 수 있는 콘텐츠의 종류(카테고리)에 따라 분류된다. 이 모델은 어떤 요소 안에 어떤 요소를 넣을 수 있는지를 정의한다.

#### 콘텐츠 카테고리

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

#### 카테고리별 설명

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

#### 유효하지 않은 중첩 예시

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

#### Transparent 콘텐츠 모델

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

### 10. 글로벌 속성

모든 HTML 요소에 사용 가능한 속성들.

#### 식별 및 분류

```html
<div id="unique-id">유일한 식별자 (문서 내 중복 불가)</div>
<div class="card primary">공백으로 구분된 클래스 목록</div>
<div slot="header">Shadow DOM 슬롯 이름</div>
```

#### 커스텀 데이터 속성

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

#### 접근성 관련

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

#### 편집 관련

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

#### 드래그 앤 드롭 / 팝오버

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

#### 스타일 및 기타

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

#### 글로벌 속성 요약

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

---

## HTML Living Standard - Web API 및 고급 주제 (Part 2)

> 이 문서는 [HTML Living Standard 기본](html.md)의 후속 문서입니다.

---

### 1. HTML 파싱

브라우저가 HTML 문자열을 DOM 트리로 변환하는 과정은 토크나이저와 트리 구성 두 단계로 나뉜다.

#### 1.1 토크나이저 (Tokenizer)

상태 기반 머신으로 동작하며, 입력 스트림을 다음 토큰들로 변환한다.

| 토큰 타입 | 설명 | 예시 |
|-----------|------|------|
| DOCTYPE | 문서 타입 선언 | `<!DOCTYPE html>` |
| Start Tag | 여는 태그 | `<div class="a">` |
| End Tag | 닫는 태그 | `</div>` |
| Comment | 주석 | `<!-- 주석 -->` |
| Character | 텍스트 문자 | `Hello` |
| EOF | 입력 끝 | - |

토크나이저는 80개 이상의 상태를 가지며, 각 문자를 읽을 때마다 상태가 전이된다.

```
Data State → '<' → Tag Open State → 'a'-'z' → Tag Name State → ...
```

#### 1.2 트리 구성 (Tree Construction)

토크나이저가 생성한 토큰을 받아 DOM 트리를 구성한다. 열린 요소 스택(stack of open elements)을 관리하며, 삽입 모드(insertion mode)에 따라 동작이 달라진다.

주요 삽입 모드:
- `initial` → `before html` → `before head` → `in head` → `after head` → `in body` → `after body` → `after after body`

#### 1.3 에러 처리

HTML 파서는 절대 실패하지 않는다. 잘못된 마크업도 일관된 규칙으로 처리한다.

```html
<!-- 입력 -->
<p>단락 1<p>단락 2

<!-- 파서 해석 결과 -->
<p>단락 1</p><p>단락 2</p>
```

```html
<!-- 테이블 내 잘못된 요소 -->
<table><b>텍스트</b></table>

<!-- 파서 결과: <b>가 테이블 밖으로 이동 -->
<b>텍스트</b><table></table>
```

#### 1.4 Foster Parenting

`<table>`, `<tbody>`, `<tr>`, `<td>` 내부에 허용되지 않는 노드가 등장하면, 해당 노드를 테이블의 부모 요소 앞으로 이동시킨다. 이를 foster parenting이라 한다.

```html
<!-- 입력 -->
<table>
  <tr><td>셀</td></tr>
  잘못된 텍스트
</table>

<!-- DOM 결과 -->
잘못된 텍스트
<table>
  <tbody><tr><td>셀</td></tr></tbody>
</table>
```

> `<tbody>`가 자동 삽입되고, "잘못된 텍스트"는 테이블 앞으로 foster parent 된다.

---

### 2. 스크립팅

#### 2.1 `<script>` 로딩 전략

```html
<!-- 동기(기본): HTML 파싱을 차단 -->
<script src="app.js"></script>

<!-- defer: HTML 파싱 완료 후, DOMContentLoaded 전 실행. 순서 보장 -->
<script defer src="a.js"></script>
<script defer src="b.js"></script>

<!-- async: 다운로드 완료 즉시 실행. 순서 보장 안 됨 -->
<script async src="analytics.js"></script>

<!-- module: 기본적으로 defer 동작. strict mode 적용 -->
<script type="module" src="app.mjs"></script>
<script type="module">
  import { greet } from './utils.mjs';
  greet();
</script>
```

실행 순서 비교:

```
일반:   HTML 파싱 중단 → 다운로드 → 실행 → 파싱 재개
defer:  HTML 파싱과 병렬 다운로드 → 파싱 완료 후 순서대로 실행
async:  HTML 파싱과 병렬 다운로드 → 다운로드 완료 즉시 실행
module: defer와 동일 (async 속성 추가 시 async 동작)
```

#### 2.2 `<template>`

렌더링되지 않는 HTML 조각을 선언적으로 정의한다. `content` 프로퍼티로 `DocumentFragment`에 접근한다.

```html
<template id="card-template">
  <div class="card">
    <h2 class="card-title"></h2>
    <p class="card-body"></p>
  </div>
</template>

<script>
  const template = document.getElementById('card-template');
  const clone = template.content.cloneNode(true);
  clone.querySelector('.card-title').textContent = '제목';
  clone.querySelector('.card-body').textContent = '내용';
  document.body.appendChild(clone);
</script>
```

#### 2.3 `<slot>`

Shadow DOM 내에서 외부 콘텐츠를 삽입할 위치를 지정한다.

```html
<user-card>
  <span slot="name">김철수</span>
  <span slot="role">개발자</span>
</user-card>

<script>
  class UserCard extends HTMLElement {
    constructor() {
      super();
      const shadow = this.attachShadow({ mode: 'open' });
      shadow.innerHTML = `
        <div class="card">
          <h3><slot name="name">이름 없음</slot></h3>
          <p><slot name="role">역할 없음</slot></p>
          <slot></slot> <!-- 기본(이름 없는) 슬롯 -->
        </div>
      `;
    }
  }
  customElements.define('user-card', UserCard);
</script>
```

`slotchange` 이벤트로 슬롯 내용 변경을 감지할 수 있다.

---

### 3. Window 인터페이스

`Window`는 브라우저 탭(또는 프레임)의 전역 객체다.

#### 3.1 주요 속성

| 속성 | 설명 |
|------|------|
| `window.document` | 현재 Document 객체 |
| `window.location` | 현재 URL 정보 (Location 객체) |
| `window.history` | History 객체 |
| `window.navigator` | Navigator 객체 (UA, 언어, 온라인 상태 등) |
| `window.innerWidth/Height` | 뷰포트 크기 (스크롤바 제외) |
| `window.outerWidth/Height` | 브라우저 창 전체 크기 |
| `window.devicePixelRatio` | 물리 픽셀 / CSS 픽셀 비율 |
| `window.name` | 창 이름 (탭 간 유지) |
| `window.opener` | 현재 창을 연 창의 참조 |
| `window.closed` | 창이 닫혔는지 여부 |

#### 3.2 주요 메서드

```javascript
// 타이머
const id = setTimeout(() => console.log('1초 후'), 1000);
clearTimeout(id);
const intervalId = setInterval(() => console.log('반복'), 500);
clearInterval(intervalId);

// 고성능 반복 (애니메이션)
function animate(timestamp) {
  // 렌더링 로직
  requestAnimationFrame(animate);
}
requestAnimationFrame(animate);

// 대화상자
alert('알림');
const ok = confirm('확인하시겠습니까?');
const input = prompt('이름을 입력하세요', '기본값');

// 창 제어
const popup = window.open('https://example.com', '_blank', 'width=400,height=300');
window.close();

// base64
const encoded = btoa('Hello');       // "SGVsbG8="
const decoded = atob('SGVsbG8=');    // "Hello"

// 구조화 복제
const clone = structuredClone({ a: 1, b: [2, 3] });

// 스크롤
window.scrollTo({ top: 0, behavior: 'smooth' });
```

---

### 4. Navigation / History API

#### 4.1 History API (전통적)

```javascript
// 새 항목 추가 (페이지 이동 없이 URL 변경)
history.pushState({ page: 2 }, '', '/page/2');

// 현재 항목 교체
history.replaceState({ page: 2, updated: true }, '', '/page/2');

// 뒤로/앞으로
history.back();
history.forward();
history.go(-2); // 2단계 뒤로

// popstate 이벤트: 뒤로가기/앞으로가기 시 발생
window.addEventListener('popstate', (e) => {
  console.log('상태:', e.state);
  renderPage(e.state);
});
```

> `pushState`/`replaceState` 호출 시에는 `popstate`가 발생하지 않는다.

#### 4.2 Location 객체

```javascript
// URL: https://example.com:8080/path?q=hello#section
location.href;       // 전체 URL
location.protocol;   // "https:"
location.host;       // "example.com:8080"
location.hostname;   // "example.com"
location.port;       // "8080"
location.pathname;   // "/path"
location.search;     // "?q=hello"
location.hash;       // "#section"
location.origin;     // "https://example.com:8080"

// 네비게이션
location.assign('/new-page');   // 이동 (히스토리에 추가)
location.replace('/new-page');  // 이동 (히스토리 교체)
location.reload();              // 새로고침
```

#### 4.3 Navigation API

`History API`의 한계를 보완하는 API로, 현재 주요 브라우저에서 폭넓게(Baseline) 지원된다.

```javascript
// navigate 이벤트 인터셉트
navigation.addEventListener('navigate', (e) => {
  if (!e.canIntercept) return;

  const url = new URL(e.destination.url);

  if (url.pathname.startsWith('/articles/')) {
    e.intercept({
      async handler() {
        const content = await fetchArticle(url.pathname);
        renderArticle(content);
      }
    });
  }
});

// 프로그래밍 방식 네비게이션
const result = navigation.navigate('/page/3', {
  state: { from: 'home' }
});
await result.committed;  // URL 변경됨
await result.finished;   // handler 완료됨

// 뒤로/앞으로
navigation.back();
navigation.forward();

// 현재 항목 상태 접근
const state = navigation.currentEntry.getState();

// 항목 목록
const entries = navigation.entries();
```

---

### 5. Web Storage

동일 출처(origin) 단위로 키-값 데이터를 저장한다. 값은 항상 문자열이다.

#### 5.1 localStorage vs sessionStorage

| 특성 | `localStorage` | `sessionStorage` |
|------|----------------|-------------------|
| 수명 | 영구 (수동 삭제 전까지) | 탭/창 닫으면 소멸 |
| 범위 | 동일 출처의 모든 탭 | 해당 탭/창에만 한정 |
| 용량 | 약 5~10MB | 약 5MB |
| storage 이벤트 | 다른 탭에서 발생 | 발생하지 않음 |

#### 5.2 API

```javascript
// 저장
localStorage.setItem('theme', 'dark');
localStorage.setItem('user', JSON.stringify({ name: '김철수', age: 30 }));

// 조회
const theme = localStorage.getItem('theme');
const user = JSON.parse(localStorage.getItem('user'));

// 삭제
localStorage.removeItem('theme');
localStorage.clear(); // 전체 삭제

// 길이 및 키 순회
for (let i = 0; i < localStorage.length; i++) {
  const key = localStorage.key(i);
  console.log(key, localStorage.getItem(key));
}
```

#### 5.3 storage 이벤트

같은 출처의 다른 탭에서 `localStorage`가 변경될 때 발생한다.

```javascript
window.addEventListener('storage', (e) => {
  console.log('변경된 키:', e.key);
  console.log('이전 값:', e.oldValue);
  console.log('새 값:', e.newValue);
  console.log('출처:', e.url);
  console.log('스토리지 객체:', e.storageArea);
});
```

> 현재 탭에서의 변경에는 이벤트가 발생하지 않는다. 탭 간 간단한 동기화에 활용 가능하다.

---

### 6. Web Workers

메인 스레드와 분리된 백그라운드 스레드에서 스크립트를 실행한다. DOM에 접근할 수 없다.

#### 6.1 Dedicated Worker

하나의 생성자(페이지)만 통신할 수 있다.

```javascript
// main.js
const worker = new Worker('worker.js');

worker.postMessage({ type: 'compute', data: [1, 2, 3, 4, 5] });

worker.onmessage = (e) => {
  console.log('결과:', e.data);
};

worker.onerror = (e) => {
  console.error('워커 에러:', e.message);
};

worker.terminate(); // 즉시 종료
```

```javascript
// worker.js
self.onmessage = (e) => {
  const { type, data } = e.data;

  if (type === 'compute') {
    const sum = data.reduce((a, b) => a + b, 0);
    self.postMessage({ result: sum });
  }
};
```

#### 6.2 Shared Worker

여러 페이지/탭이 하나의 워커 인스턴스를 공유한다.

```javascript
// main.js (여러 탭에서 동일 코드)
const shared = new SharedWorker('shared-worker.js');

shared.port.onmessage = (e) => {
  console.log('응답:', e.data);
};

shared.port.postMessage('안녕하세요');
```

```javascript
// shared-worker.js
const ports = [];

self.onconnect = (e) => {
  const port = e.ports[0];
  ports.push(port);

  port.onmessage = (event) => {
    // 연결된 모든 포트에 브로드캐스트
    ports.forEach(p => p.postMessage(`전체 연결 수: ${ports.length}`));
  };

  port.start();
};
```

#### 6.3 Transferable Objects

데이터를 복사 대신 소유권을 이전하여 성능을 높인다. 전송 후 원본은 사용 불가가 된다.

```javascript
// ArrayBuffer 전송 (복사 없이 이동)
const buffer = new ArrayBuffer(1024 * 1024); // 1MB
console.log(buffer.byteLength); // 1048576

worker.postMessage(buffer, [buffer]); // 두 번째 인자: transfer list
console.log(buffer.byteLength); // 0 (소유권 이전됨)

// structuredClone의 transfer와 동일한 개념
const ab = new ArrayBuffer(100);
worker.postMessage({ payload: ab }, { transfer: [ab] });
```

전송 가능한 타입: `ArrayBuffer`, `MessagePort`, `ImageBitmap`, `OffscreenCanvas`, `ReadableStream`, `WritableStream` 등.

---

### 7. MessageChannel / BroadcastChannel

#### 7.1 MessageChannel

양방향 통신을 위한 전용 채널을 생성한다. 두 개의 `MessagePort`로 구성된다.

```javascript
const channel = new MessageChannel();

// port1과 port2는 서로 연결된 쌍
channel.port1.onmessage = (e) => console.log('port1 수신:', e.data);
channel.port2.onmessage = (e) => console.log('port2 수신:', e.data);

channel.port1.postMessage('port2에게');
channel.port2.postMessage('port1에게');

// iframe에 포트 전달
const iframe = document.querySelector('iframe');
iframe.contentWindow.postMessage('포트 전달', '*', [channel.port2]);
```

#### 7.2 BroadcastChannel

같은 출처의 모든 탭/창/iframe/워커 간 간편한 메시지 브로드캐스트를 제공한다.

```javascript
// 탭 A
const bc = new BroadcastChannel('app-events');
bc.postMessage({ type: 'LOGOUT' });

// 탭 B (같은 출처)
const bc = new BroadcastChannel('app-events');
bc.onmessage = (e) => {
  if (e.data.type === 'LOGOUT') {
    // 로그아웃 처리
    window.location.href = '/login';
  }
};

// 채널 닫기
bc.close();
```

MessageChannel vs BroadcastChannel:

| 특성 | MessageChannel | BroadcastChannel |
|------|---------------|------------------|
| 통신 방식 | 1:1 (두 포트 간) | 1:N (구독자 전체) |
| 포트 전달 | 필요 | 불필요 (채널명으로 연결) |
| 크로스 오리진 | postMessage로 포트 전달 시 가능 | 동일 출처만 |

---

### 8. Server-Sent Events (SSE)

서버에서 클라이언트로의 단방향 실시간 스트림이다. HTTP 기반이며 자동 재연결을 지원한다.

#### 클라이언트

```javascript
const source = new EventSource('/api/events');

// 기본 message 이벤트
source.onmessage = (e) => {
  console.log('데이터:', e.data);
  console.log('ID:', e.lastEventId);
};

// 커스텀 이벤트 타입
source.addEventListener('notification', (e) => {
  const data = JSON.parse(e.data);
  showNotification(data.title, data.body);
});

// 연결 상태
source.onopen = () => console.log('연결됨');
source.onerror = (e) => {
  if (source.readyState === EventSource.CLOSED) {
    console.log('연결 종료');
  }
  // CONNECTING 상태면 자동 재연결 시도
};

// 인증 헤더가 필요한 경우
const source2 = new EventSource('/api/events', {
  withCredentials: true // 쿠키 전송
});

source.close(); // 연결 종료
```

#### 서버 응답 형식

```
Content-Type: text/event-stream

data: 간단한 메시지

data: {"name": "알림", "count": 5}
id: 42
event: notification
retry: 3000

data: 여러 줄
data: 메시지도 가능
```

| 필드 | 설명 |
|------|------|
| `data:` | 메시지 본문 |
| `event:` | 이벤트 타입 (기본: `message`) |
| `id:` | 이벤트 ID (재연결 시 `Last-Event-ID` 헤더로 전송) |
| `retry:` | 재연결 대기 시간 (ms) |

---

### 9. Cross-document Messaging

서로 다른 출처의 문서(iframe, popup) 간 안전한 통신을 제공한다.

```javascript
// 보내는 쪽 (부모 페이지)
const iframe = document.querySelector('iframe');
iframe.contentWindow.postMessage(
  { action: 'resize', height: 500 },
  'https://child.example.com'  // 수신자 출처 지정 (보안 중요)
);

// 받는 쪽 (iframe 내부)
window.addEventListener('message', (e) => {
  // 1. 반드시 출처 검증
  if (e.origin !== 'https://parent.example.com') return;

  // 2. 메시지 처리
  if (e.data.action === 'resize') {
    document.body.style.height = e.data.height + 'px';
  }

  // 3. 응답
  e.source.postMessage({ status: 'ok' }, e.origin);
});
```

보안 주의사항:
- `targetOrigin`에 `'*'`를 사용하지 말 것 (민감한 데이터 유출 위험)
- 수신 측에서 반드시 `event.origin` 검증
- `event.data`의 내용을 `innerHTML`에 직접 삽입하지 말 것

---

### 10. Drag and Drop API

#### 10.1 이벤트 흐름

```
드래그 소스:  dragstart → drag(반복) → dragend
드롭 대상:   dragenter → dragover(반복) → drop / dragleave
```

#### 10.2 구현 예시

```html
<div id="item" draggable="true">드래그할 아이템</div>
<div id="dropzone">여기에 놓으세요</div>

<script>
  const item = document.getElementById('item');
  const zone = document.getElementById('dropzone');

  // 드래그 시작
  item.addEventListener('dragstart', (e) => {
    e.dataTransfer.setData('text/plain', item.id);
    e.dataTransfer.effectAllowed = 'move';
    item.classList.add('dragging');
  });

  item.addEventListener('dragend', (e) => {
    item.classList.remove('dragging');
  });

  // 드롭 영역
  zone.addEventListener('dragover', (e) => {
    e.preventDefault(); // 필수: drop 이벤트 허용
    e.dataTransfer.dropEffect = 'move';
    zone.classList.add('over');
  });

  zone.addEventListener('dragleave', () => {
    zone.classList.remove('over');
  });

  zone.addEventListener('drop', (e) => {
    e.preventDefault();
    zone.classList.remove('over');

    const id = e.dataTransfer.getData('text/plain');
    const el = document.getElementById(id);
    zone.appendChild(el);
  });
</script>
```

#### 10.3 파일 드래그

```javascript
zone.addEventListener('drop', (e) => {
  e.preventDefault();

  const files = e.dataTransfer.files;
  for (const file of files) {
    console.log(`${file.name} (${file.size} bytes, ${file.type})`);
  }

  // DataTransferItemList API (디렉토리 지원)
  for (const item of e.dataTransfer.items) {
    if (item.kind === 'file') {
      const entry = item.webkitGetAsEntry();
      if (entry.isDirectory) {
        // 디렉토리 순회
      }
    }
  }
});
```

> `webkitGetAsEntry()`는 HTML Living Standard에 정의된 API가 아니다. `DataTransferItem`은 HTML 표준이 정의하지만, 이 메서드는 별도의 비표준 사양인 File and Directory Entries API(원래 WebKit이 도입한 벤더 확장)에 속한다. 대부분의 브라우저가 호환성 때문에 구현하고 있지만, 표준화된 대안으로는 File System Access API(`showDirectoryPicker()` 등)를 사용하는 것이 권장된다.

---

### 11. Custom Elements

#### 11.1 정의 및 등록

```javascript
class MyCounter extends HTMLElement {
  #count = 0;
  #shadow;

  // 감시할 속성 목록
  static observedAttributes = ['initial'];

  constructor() {
    super();
    this.#shadow = this.attachShadow({ mode: 'open' });
    this.#shadow.innerHTML = `
      <style>
        :host { display: inline-flex; align-items: center; gap: 8px; }
        button { cursor: pointer; }
        span { min-width: 2em; text-align: center; }
      </style>
      <button id="dec">-</button>
      <span id="display">0</span>
      <button id="inc">+</button>
    `;
  }

  // DOM에 삽입될 때
  connectedCallback() {
    this.#shadow.getElementById('inc').addEventListener('click', () => this.#update(1));
    this.#shadow.getElementById('dec').addEventListener('click', () => this.#update(-1));
  }

  // DOM에서 제거될 때
  disconnectedCallback() {
    // 정리 작업
  }

  // 다른 문서로 이동될 때
  adoptedCallback() {}

  // 감시 속성 변경 시
  attributeChangedCallback(name, oldVal, newVal) {
    if (name === 'initial') {
      this.#count = parseInt(newVal) || 0;
      this.#render();
    }
  }

  #update(delta) {
    this.#count += delta;
    this.#render();
    this.dispatchEvent(new CustomEvent('count-changed', {
      detail: { count: this.#count },
      bubbles: true
    }));
  }

  #render() {
    this.#shadow.getElementById('display').textContent = this.#count;
  }
}

// 등록 (이름에 반드시 하이픈 포함)
customElements.define('my-counter', MyCounter);
```

```html
<my-counter initial="10"></my-counter>

<script>
  // 정의 완료 대기
  customElements.whenDefined('my-counter').then(() => {
    console.log('my-counter 사용 가능');
  });
</script>
```

#### 11.2 기존 요소 확장 (Customized Built-in)

```javascript
class FancyButton extends HTMLButtonElement {
  connectedCallback() {
    this.style.background = 'linear-gradient(45deg, #6a5acd, #00bfff)';
    this.style.color = 'white';
  }
}

customElements.define('fancy-button', FancyButton, { extends: 'button' });
```

```html
<button is="fancy-button">클릭</button>
```

> Safari는 Customized Built-in Elements를 지원하지 않는다. Autonomous Custom Element 사용을 권장.

#### 11.3 ElementInternals

Custom Element가 폼 참여, 접근성, 상태 관리를 네이티브 요소처럼 수행할 수 있게 한다.

```javascript
class MyInput extends HTMLElement {
  static formAssociated = true; // 폼 연동 선언

  #internals;

  constructor() {
    super();
    this.#internals = this.attachInternals();
    this.attachShadow({ mode: 'open' });
    this.shadowRoot.innerHTML = `<input type="text" />`;
  }

  connectedCallback() {
    const input = this.shadowRoot.querySelector('input');

    input.addEventListener('input', () => {
      // 폼 값 설정
      this.#internals.setFormValue(input.value);

      // 유효성 검사
      if (!input.value) {
        this.#internals.setValidity(
          { valueMissing: true },
          '값을 입력해주세요',
          input
        );
      } else {
        this.#internals.setValidity({});
      }
    });

    // ARIA 역할 및 속성 설정
    this.#internals.role = 'textbox';
    this.#internals.ariaRequired = 'true';
  }

  // 폼 콜백
  formResetCallback() {
    this.shadowRoot.querySelector('input').value = '';
    this.#internals.setFormValue('');
  }

  formStateRestoreCallback(state, mode) {
    this.shadowRoot.querySelector('input').value = state;
  }
}

customElements.define('my-input', MyInput);
```

```html
<form>
  <my-input name="username"></my-input>
  <button type="submit">제출</button>
</form>
```

---

### 12. Microdata

HTML에 기계가 읽을 수 있는 구조화된 데이터를 내장하는 메커니즘이다. 주로 Schema.org 어휘와 함께 사용한다.

#### 12.1 기본 문법

| 속성 | 설명 |
|------|------|
| `itemscope` | 새로운 아이템(항목)의 범위를 선언 |
| `itemtype` | 아이템의 타입 (Schema.org URL) |
| `itemprop` | 속성 이름 |
| `itemid` | 아이템의 고유 식별자 |

#### 12.2 예시: 제품 정보

```html
<div itemscope itemtype="https://schema.org/Product">
  <h1 itemprop="name">무선 이어폰</h1>
  <img itemprop="image" src="earphone.jpg" alt="무선 이어폰">
  <p itemprop="description">고품질 블루투스 무선 이어폰입니다.</p>

  <div itemprop="offers" itemscope itemtype="https://schema.org/Offer">
    <span itemprop="priceCurrency">KRW</span>
    <span itemprop="price" content="89000">89,000원</span>
    <link itemprop="availability" href="https://schema.org/InStock" />
    <meta itemprop="url" content="https://shop.example.com/earphone" />
  </div>

  <div itemprop="aggregateRating" itemscope itemtype="https://schema.org/AggregateRating">
    평점: <span itemprop="ratingValue">4.5</span> / 5
    (<span itemprop="reviewCount">128</span>개 리뷰)
  </div>
</div>
```

#### 12.3 속성값 추출 규칙

요소에 따라 `itemprop`의 값이 추출되는 방식이 다르다.

| 요소 | 값 소스 |
|------|---------|
| `<meta>` | `content` 속성 |
| `<a>`, `<area>`, `<link>` | `href` 속성 |
| `<img>`, `<source>`, `<video>`, `<audio>` | `src` 속성 |
| `<time>` | `datetime` 속성 |
| `<data>` | `value` 속성 |
| 기타 요소 | `textContent` |

#### 12.4 중첩 아이템과 itemid

```html
<article itemscope itemtype="https://schema.org/BlogPosting">
  <h2 itemprop="headline">HTML Microdata 가이드</h2>
  <time itemprop="datePublished" datetime="2025-01-15">2025년 1월 15일</time>

  <div itemprop="author" itemscope itemtype="https://schema.org/Person"
       itemid="https://example.com/people/kim">
    <a itemprop="url" href="https://example.com/people/kim">
      <span itemprop="name">김영희</span>
    </a>
  </div>
</article>
```

#### 12.5 JavaScript Microdata API (표준에서 제거됨)

과거 HTML 표준은 `document.getItems()`, `HTMLPropertiesCollection` 등 Microdata를 위한 전용 JavaScript API를 정의했으나, 실제 구현이 거의 없어 현재는 HTML Living Standard에서 완전히 제거되었다. 따라서 Microdata 값을 읽으려면 아래처럼 일반 DOM API(`querySelectorAll`, `getAttribute` 등)로 직접 순회해야 한다.

```javascript
// itemscope 요소 찾기 (표준 DOM API만 사용)
const products = document.querySelectorAll('[itemscope][itemtype*="Product"]');

products.forEach(product => {
  const nameEl = product.querySelector('[itemprop="name"]');
  const priceEl = product.querySelector('[itemprop="price"]');
  console.log(nameEl.textContent, priceEl.getAttribute('content'));
});
```

> 검색엔진은 Microdata, JSON-LD, RDFa 중 JSON-LD를 선호하는 추세이지만, Microdata는 HTML과 콘텐츠가 직접 연결되어 동기화 문제가 적다는 장점이 있다.

---

### 참고 자료

- [HTML Living Standard (WHATWG)](https://html.spec.whatwg.org/multipage/)
- [HTML 파싱 명세](https://html.spec.whatwg.org/multipage/parsing.html)
- [Navigation API 명세](https://html.spec.whatwg.org/multipage/nav-history-apis.html)
- [Web Workers 명세](https://html.spec.whatwg.org/multipage/workers.html)
- [Custom Elements 명세](https://html.spec.whatwg.org/multipage/custom-elements.html)
- [Schema.org](https://schema.org/)
