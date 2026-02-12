# WHATWG Encoding Standard 완벽 가이드

## 목차

1. [개요](#1-개요)
2. [용어 정의](#2-용어-정의)
3. [인코딩 목록](#3-인코딩-목록)
4. [UTF-8 상세](#4-utf-8-상세)
5. [인코딩 레이블 매핑](#5-인코딩-레이블-매핑)
6. [BOM 스니핑](#6-bom-스니핑)
7. [TextEncoder API](#7-textencoder-api)
8. [TextDecoder API](#8-textdecoder-api)
9. [TextEncoderStream / TextDecoderStream](#9-textencoderstream--textdecoderstream)
10. [웹에서의 인코딩 감지](#10-웹에서의-인코딩-감지)
11. [한국어 인코딩](#11-한국어-인코딩)
12. [실용 예제](#12-실용-예제)

---

## 1. 개요

### 1.1 Encoding Standard란

WHATWG Encoding Standard는 웹 플랫폼에서 문자 인코딩을 처리하는 방식을 정의하는 살아있는 표준(Living Standard)이다. 이 표준은 텍스트를 바이트 시퀀스로 변환(인코딩)하고, 바이트 시퀀스를 텍스트로 복원(디코딩)하는 알고리즘을 규정한다.

- **공식 문서**: https://encoding.spec.whatwg.org/
- **유지보수 주체**: WHATWG (Web Hypertext Application Technology Working Group)
- **문서 유형**: Living Standard
- **핵심 메시지**: **UTF-8을 사용하라.** 새로운 콘텐츠는 반드시 UTF-8로 인코딩되어야 한다.

### 1.2 문자 인코딩의 역사

컴퓨터가 텍스트를 처리하려면 문자를 숫자(바이트)로 변환하는 규칙이 필요하다. 이 규칙이 바로 문자 인코딩이다.

| 시기 | 인코딩 | 설명 |
|------|--------|------|
| 1963 | ASCII | 7비트, 128개 문자 (영문, 숫자, 기호) |
| 1987 | ISO 8859-1 | 8비트, 서유럽 문자 256개 |
| 1990s | EUC-KR | 한국어 완성형 인코딩 (KS X 1001 기반) |
| 1990s | Shift_JIS | 일본어 인코딩 |
| 1993 | Unicode 1.1 | 전 세계 문자를 하나의 체계로 통합 시도 |
| 1993 | UTF-8 | 가변 길이 유니코드 인코딩 (1~4바이트) |
| 2003 | UTF-8 보편화 | 웹에서 UTF-8 사용 비율이 급격히 증가 |
| 2008~ | UTF-8 표준화 | W3C/WHATWG에서 UTF-8을 기본 인코딩으로 권장 |

```
ASCII (1963)
  A = 0x41, B = 0x42, ... Z = 0x5A
  0~127 범위의 7비트 인코딩

ISO-8859-1 (1987)
  ASCII 확장, 128~255에 서유럽 문자 배치
  ä = 0xE4, ö = 0xF6, ü = 0xFC

EUC-KR (1990s)
  한글 2바이트, ASCII 1바이트
  "가" = 0xB0 0xA1, "나" = 0xB3 0xAA

UTF-8 (1993~)
  가변 길이 유니코드 인코딩
  "A" = 0x41 (1바이트)
  "가" = 0xEA 0xB0 0x80 (3바이트)
  "😀" = 0xF0 0x9F 0x98 0x80 (4바이트)
```

### 1.3 왜 표준이 필요한가

인코딩 표준이 필요한 핵심 이유는 다음과 같다.

**1) 레거시 인코딩의 상호운용성**

웹에는 다양한 레거시 인코딩으로 작성된 문서가 존재한다. 브라우저마다 동일한 바이트 시퀀스를 다른 문자로 해석하면 글자가 깨지는 "mojibake" 현상이 발생한다. 표준은 모든 브라우저가 동일한 방식으로 레거시 인코딩을 처리하도록 보장한다.

**2) 보안 취약점 방지**

인코딩 처리의 불일치는 XSS(Cross-Site Scripting) 등 보안 취약점으로 이어질 수 있다. 예를 들어 특정 레거시 인코딩에서 멀티바이트 문자의 첫 번째 바이트가 이스케이프 문자를 "삼켜버리는" 공격이 존재한다.

**3) JavaScript API의 통일**

`TextEncoder`, `TextDecoder` 등 웹 API를 명확하게 정의하여 모든 플랫폼에서 동일한 인코딩/디코딩 동작을 보장한다.

**4) UTF-8로의 수렴 촉진**

표준은 새 콘텐츠에 대해 UTF-8만 인코딩하도록 제한하고(`TextEncoder`는 UTF-8만 지원), 레거시 인코딩은 디코딩만 허용함으로써 웹 전체가 UTF-8로 수렴하도록 유도한다.

```javascript
// TextEncoder는 항상 UTF-8만 사용한다
const encoder = new TextEncoder();
console.log(encoder.encoding); // "utf-8"

// TextDecoder는 레거시 인코딩도 디코딩할 수 있다
const decoder = new TextDecoder('euc-kr');
console.log(decoder.encoding); // "euc-kr"
```

---

## 2. 용어 정의

### 2.1 코드 포인트 (Code Point)

유니코드에서 각 문자에 부여된 고유한 숫자 값이다. U+0000부터 U+10FFFF까지의 범위를 가진다. 총 1,114,112개의 코드 포인트가 존재한다.

```javascript
// 코드 포인트 확인
'A'.codePointAt(0);    // 65 (U+0041)
'가'.codePointAt(0);   // 44032 (U+AC00)
'😀'.codePointAt(0);   // 128512 (U+1F600)

// 코드 포인트에서 문자로
String.fromCodePoint(0xAC00);  // "가"
String.fromCodePoint(0x1F600); // "😀"
```

코드 포인트의 분류:

| 범위 | 이름 | 설명 |
|------|------|------|
| U+0000~U+007F | ASCII | 기본 라틴 문자 |
| U+0080~U+07FF | BMP 앞부분 | 대부분의 라틴 확장, 그리스, 키릴 등 |
| U+0800~U+FFFF | BMP 나머지 | 한중일 문자, 한글 등 |
| U+10000~U+10FFFF | 보충 평면 | 이모지, 고대 문자, 수학 기호 등 |
| U+D800~U+DFFF | 서로게이트 | UTF-16 전용, 단독 사용 불가 (스칼라 값이 아님) |

### 2.2 스칼라 값 (Scalar Value)

서로게이트(U+D800~U+DFFF)를 제외한 코드 포인트를 스칼라 값이라 한다. 실제 문자를 나타낼 수 있는 유효한 코드 포인트만을 의미한다.

### 2.3 바이트 (Byte)

0x00부터 0xFF까지의 8비트 정수 값이다. 인코딩된 데이터의 기본 단위이다.

### 2.4 바이트 시퀀스 (Byte Sequence)

0개 이상의 바이트가 순서대로 나열된 것이다. 인코딩의 출력과 디코딩의 입력은 바이트 시퀀스이다.

### 2.5 인코딩 (Encoding)

코드 포인트 시퀀스와 바이트 시퀀스 사이의 매핑 규칙이다. 각 인코딩은 이름(name), 디코더(decoder), 인코더(encoder, 선택적)를 가진다.

### 2.6 디코더 (Decoder)

바이트 스트림을 입력으로 받아 코드 포인트 스트림을 출력하는 알고리즘이다. 유효하지 않은 바이트를 만나면 오류를 발생시키거나 대체 문자(U+FFFD)를 출력한다.

### 2.7 인코더 (Encoder)

코드 포인트 스트림을 입력으로 받아 바이트 스트림을 출력하는 알고리즘이다. 표준에서는 UTF-8 인코더만 웹 API로 노출된다.

### 2.8 BOM (Byte Order Mark)

U+FEFF 코드 포인트로, 문서의 시작 부분에 위치하여 인코딩의 종류와 바이트 순서를 나타내는 표시이다.

```javascript
// BOM의 바이트 표현
// UTF-8 BOM:    0xEF 0xBB 0xBF
// UTF-16 BE BOM: 0xFE 0xFF
// UTF-16 LE BOM: 0xFF 0xFE

const utf8Bom = new Uint8Array([0xEF, 0xBB, 0xBF]);
const decoder = new TextDecoder('utf-8');
decoder.decode(utf8Bom); // "" (BOM은 기본적으로 제거됨)
```

### 2.9 U+FFFD REPLACEMENT CHARACTER

디코딩 중 유효하지 않은 바이트 시퀀스를 만났을 때 사용되는 대체 문자이다. "�"로 표시된다.

```javascript
// 유효하지 않은 UTF-8 바이트 → U+FFFD로 대체
const invalid = new Uint8Array([0xFF, 0xFE, 0x41]);
const decoder = new TextDecoder('utf-8');
console.log(decoder.decode(invalid)); // "��A"
```

---

## 3. 인코딩 목록

Encoding Standard는 웹 호환성을 위해 다양한 레거시 인코딩을 지원한다. 그러나 **새 콘텐츠에는 반드시 UTF-8**을 사용해야 한다.

### 3.1 UTF-8 (유일한 필수 인코딩)

| 속성 | 값 |
|------|------|
| 이름 | UTF-8 |
| 유형 | 가변 길이 (1~4바이트) |
| 범위 | U+0000~U+10FFFF (전체 유니코드) |
| 웹 사용률 | 98% 이상 (2024년 기준) |
| TextEncoder 지원 | 인코딩 + 디코딩 |

### 3.2 Legacy single-byte 인코딩

1바이트로 1문자를 표현하는 인코딩이다. 0x00~0x7F는 ASCII와 동일하고, 0x80~0xFF 범위에 각 인코딩 고유의 문자가 배치된다.

| 인코딩 이름 | 주요 사용 지역 | 설명 |
|-------------|---------------|------|
| windows-1252 | 서유럽 | ISO-8859-1의 상위 집합, 웹에서 가장 흔한 single-byte 레거시 |
| ISO-8859-2 | 중앙유럽 | 폴란드어, 체코어, 헝가리어 등 |
| ISO-8859-3 | 남유럽 | 터키어, 몰타어, 에스페란토 |
| ISO-8859-4 | 북유럽 | 에스토니아어, 라트비아어, 리투아니아어 |
| ISO-8859-5 | 키릴 | 러시아어, 불가리아어 등 (사용 빈도 낮음) |
| ISO-8859-6 | 아랍어 | 아랍 문자 |
| ISO-8859-7 | 그리스어 | 현대 그리스 문자 |
| ISO-8859-8 | 히브리어 | 히브리 문자 (시각적 순서) |
| ISO-8859-8-I | 히브리어 | 히브리 문자 (논리적 순서) |
| ISO-8859-10 | 북유럽 | 사미어, 아이슬란드어 등 |
| ISO-8859-13 | 발트해 | 라트비아어, 리투아니아어 |
| ISO-8859-14 | 켈트 | 아일랜드어, 웨일스어 |
| ISO-8859-15 | 서유럽 | ISO-8859-1 + 유로 기호(€) |
| ISO-8859-16 | 남동유럽 | 루마니아어 등 |
| KOI8-R | 러시아 | 러시아어 키릴 문자 |
| KOI8-U | 우크라이나 | 우크라이나어 키릴 문자 |
| macintosh | - | Mac OS Roman 인코딩 |
| windows-874 | 태국 | 태국어 |
| windows-1250 | 중앙유럽 | 폴란드어, 체코어 등 |
| windows-1251 | 키릴 | 러시아어, 우크라이나어 등 |
| windows-1253 | 그리스어 | 현대 그리스어 |
| windows-1254 | 터키어 | 터키어 |
| windows-1255 | 히브리어 | 히브리어 |
| windows-1256 | 아랍어 | 아랍어 |
| windows-1257 | 발트해 | 발트 3국 언어 |
| windows-1258 | 베트남어 | 베트남어 |
| x-mac-cyrillic | 키릴 | Mac OS 키릴 문자 |

### 3.3 Legacy multi-byte CJK 인코딩

한중일(CJK) 문자를 표현하기 위한 멀티바이트 인코딩이다.

| 인코딩 이름 | 주요 사용 | 바이트 수 | 설명 |
|-------------|----------|----------|------|
| EUC-KR | 한국어 | 1~2 | KS X 1001 기반, 실제로는 cp949(UHC) |
| EUC-JP | 일본어 | 1~3 | JIS X 0208/0212 기반 |
| Shift_JIS | 일본어 | 1~2 | Windows에서 널리 사용 |
| ISO-2022-JP | 일본어 | 가변 | 이스케이프 시퀀스 기반 상태 전환 인코딩 |
| Big5 | 중국어(번체) | 1~2 | 대만/홍콩에서 사용 |
| GBK | 중국어(간체) | 1~2 | GB2312의 상위 집합 |
| gb18030 | 중국어(간체) | 1~4 | GBK의 상위 집합, 전체 유니코드 포함 |
| GB2312 | 중국어(간체) | 1~2 | 실제로는 GBK로 처리됨 |

> **참고**: 표준에서 `GB2312` 레이블은 `GBK` 인코딩으로 매핑된다. `gb18030`은 유니코드의 모든 코드 포인트를 표현할 수 있는 유일한 레거시 인코딩이다.

### 3.4 Legacy 기타 인코딩

| 인코딩 이름 | 설명 |
|-------------|------|
| UTF-16BE | UTF-16 빅 엔디안. 디코딩만 지원 |
| UTF-16LE | UTF-16 리틀 엔디안. 디코딩만 지원 |
| x-user-defined | 0x80~0xFF를 U+F780~U+F7FF에 매핑하는 특수 인코딩 |
| replacement | 항상 디코딩 실패(U+FFFD)를 반환하는 특수 인코딩. 보안상 위험한 인코딩에 대한 안전장치 |

> **replacement 인코딩이 필요한 이유**: ISO-2022-KR, ISO-2022-CN 등 일부 레거시 인코딩은 보안 취약점(상태 전환을 악용한 XSS)이 있어, 표준에서는 이들을 `replacement`로 매핑하여 항상 오류를 반환하도록 한다.

```javascript
// replacement 인코딩의 동작
const decoder = new TextDecoder('iso-2022-kr');
// 실제로는 replacement 디코더가 사용됨
// 어떤 입력이든 U+FFFD 하나만 출력하고 끝남
```

---

## 4. UTF-8 상세

### 4.1 인코딩 규칙

UTF-8은 코드 포인트 값에 따라 1~4바이트의 가변 길이로 인코딩한다.

| 코드 포인트 범위 | 바이트 수 | 바이트 1 | 바이트 2 | 바이트 3 | 바이트 4 |
|-----------------|----------|---------|---------|---------|---------|
| U+0000~U+007F | 1 | 0xxxxxxx | - | - | - |
| U+0080~U+07FF | 2 | 110xxxxx | 10xxxxxx | - | - |
| U+0800~U+FFFF | 3 | 1110xxxx | 10xxxxxx | 10xxxxxx | - |
| U+10000~U+10FFFF | 4 | 11110xxx | 10xxxxxx | 10xxxxxx | 10xxxxxx |

인코딩 과정 예시:

```javascript
// "가" = U+AC00 → 3바이트 인코딩
// U+AC00 = 1010_1100_0000_0000 (2진수)
// 3바이트 패턴: 1110xxxx 10xxxxxx 10xxxxxx
// x 채우기:     1110_1010 10_1100_00 10_000000
//             = 0xEA     0xB0       0x80

const encoder = new TextEncoder();
const bytes = encoder.encode('가');
console.log([...bytes].map(b => '0x' + b.toString(16).toUpperCase()));
// ["0xEA", "0xB0", "0x80"]

// "A" = U+0041 → 1바이트
console.log([...encoder.encode('A')].map(b => '0x' + b.toString(16)));
// ["0x41"]

// "€" = U+20AC → 3바이트
console.log([...encoder.encode('€')].map(b => '0x' + b.toString(16)));
// ["0xe2", "0x82", "0xac"]

// "😀" = U+1F600 → 4바이트
console.log([...encoder.encode('😀')].map(b => '0x' + b.toString(16)));
// ["0xf0", "0x9f", "0x98", "0x80"]
```

### 4.2 UTF-8 디코딩 알고리즘

표준은 상태 머신 기반의 디코더를 정의한다. 핵심 로직은 다음과 같다.

```
UTF-8 디코더 상태:
- utf-8 code point: 초기값 0
- utf-8 bytes seen: 초기값 0
- utf-8 bytes needed: 초기값 0
- utf-8 lower boundary: 초기값 0x80
- utf-8 upper boundary: 초기값 0xBF

바이트 처리:
1. bytes_needed가 0이면:
   - 0x00~0x7F → 해당 코드 포인트 반환
   - 0xC2~0xDF → bytes_needed=1, code_point = byte & 0x1F
   - 0xE0~0xEF → bytes_needed=2, code_point = byte & 0x0F
     - 0xE0이면 lower=0xA0 (overlong 방지)
     - 0xED이면 upper=0x9F (서로게이트 방지)
   - 0xF0~0xF4 → bytes_needed=3, code_point = byte & 0x07
     - 0xF0이면 lower=0x90 (overlong 방지)
     - 0xF4이면 upper=0x8F (U+10FFFF 초과 방지)
   - 그 외 → 오류(U+FFFD)

2. bytes_needed > 0이면:
   - 바이트가 lower~upper 범위가 아니면 → 오류, 상태 리셋, 바이트 다시 처리
   - 범위 안이면 → code_point를 갱신, bytes_seen 증가
   - bytes_seen == bytes_needed이면 → code_point 반환, 상태 리셋
```

### 4.3 오류 처리 (U+FFFD)

UTF-8 디코더가 유효하지 않은 바이트를 만났을 때의 동작을 정의한다.

```javascript
// 기본 동작: 잘못된 바이트를 U+FFFD로 대체
const decoder = new TextDecoder('utf-8');
const invalid = new Uint8Array([0x48, 0x80, 0x65, 0x6C, 0x6C, 0x6F]);
//                                H    ^^err   e     l     l     o
console.log(decoder.decode(invalid)); // "H�ello"

// fatal 모드: 잘못된 바이트가 있으면 예외 발생
const strictDecoder = new TextDecoder('utf-8', { fatal: true });
try {
  strictDecoder.decode(invalid);
} catch (e) {
  console.log(e.message); // "The encoded data was not valid."
  console.log(e instanceof TypeError); // true
}
```

오류 대체의 세부 규칙 (최대 오류 원칙):

```javascript
// 각 유효하지 않은 바이트 시퀀스 단위마다 U+FFFD 하나를 생성한다
// 연속된 유효하지 않은 바이트도 개별적으로 U+FFFD가 된다

// 예: 불완전한 멀티바이트 시퀀스
const bytes1 = new Uint8Array([0xC2]); // 2바이트 시퀀스의 시작이지만 후속 바이트 없음
console.log(new TextDecoder().decode(bytes1)); // "�"

// 예: overlong 인코딩 (보안상 거부됨)
// U+002F "/"를 2바이트로 인코딩하면 0xC0 0xAF → 거부
const overlong = new Uint8Array([0xC0, 0xAF]);
console.log(new TextDecoder().decode(overlong)); // "��"
```

### 4.4 유효성 검사

UTF-8 바이트 시퀀스가 유효하려면 다음 조건을 모두 만족해야 한다.

```
유효한 UTF-8 바이트 시퀀스 규칙:
1. 첫 바이트 0x00~0x7F → 1바이트 (단독)
2. 첫 바이트 0xC2~0xDF → 2바이트 (두 번째: 0x80~0xBF)
3. 첫 바이트 0xE0 → 3바이트 (두 번째: 0xA0~0xBF, 세 번째: 0x80~0xBF)
4. 첫 바이트 0xE1~0xEC → 3바이트 (두 번째: 0x80~0xBF, 세 번째: 0x80~0xBF)
5. 첫 바이트 0xED → 3바이트 (두 번째: 0x80~0x9F, 세 번째: 0x80~0xBF)
6. 첫 바이트 0xEE~0xEF → 3바이트 (두 번째: 0x80~0xBF, 세 번째: 0x80~0xBF)
7. 첫 바이트 0xF0 → 4바이트 (두 번째: 0x90~0xBF, 세 번째~네 번째: 0x80~0xBF)
8. 첫 바이트 0xF1~0xF3 → 4바이트 (두 번째~네 번째: 0x80~0xBF)
9. 첫 바이트 0xF4 → 4바이트 (두 번째: 0x80~0x8F, 세 번째~네 번째: 0x80~0xBF)

거부되는 것:
- 0xC0~0xC1 (overlong 2바이트)
- 0xF5~0xFF (범위 초과)
- 서로게이트 코드 포인트 (U+D800~U+DFFF → 0xED 0xA0 0x80 ~ 0xED 0xBF 0xBF)
```

```javascript
// UTF-8 유효성 검사 함수
function isValidUTF8(bytes) {
  try {
    const decoder = new TextDecoder('utf-8', { fatal: true });
    decoder.decode(bytes);
    return true;
  } catch {
    return false;
  }
}

console.log(isValidUTF8(new Uint8Array([0x48, 0x65, 0x6C, 0x6C, 0x6F]))); // true "Hello"
console.log(isValidUTF8(new Uint8Array([0xEA, 0xB0, 0x80])));             // true "가"
console.log(isValidUTF8(new Uint8Array([0xC0, 0xAF])));                    // false (overlong)
console.log(isValidUTF8(new Uint8Array([0xED, 0xA0, 0x80])));             // false (서로게이트)
```

---

## 5. 인코딩 레이블 매핑

### 5.1 이름과 별칭

각 인코딩에는 하나의 정규 이름(name)과 여러 개의 레이블(별칭)이 있다. 웹에서는 다양한 이름으로 같은 인코딩을 지칭하므로, 표준은 이들을 모두 하나의 인코딩으로 매핑한다.

| 정규 이름 | 레이블 (별칭) |
|-----------|-------------|
| UTF-8 | `utf-8`, `utf8`, `unicode-1-1-utf-8`, `unicode11utf8`, `unicode20utf8`, `x-unicode20utf8` |
| windows-1252 | `ascii`, `ansi_x3.4-1968`, `cp1252`, `cp819`, `csisolatin1`, `ibm819`, `iso-8859-1`, `iso-ir-100`, `iso8859-1`, `iso88591`, `iso_8859-1`, `l1`, `latin1`, `us-ascii`, `x-cp1252` |
| EUC-KR | `euc-kr`, `cseuckr`, `csksc56011987`, `iso-ir-149`, `korean`, `ks_c_5601-1987`, `ks_c_5601-1989`, `ksc5601`, `ksc_5601`, `windows-949` |
| Shift_JIS | `shift_jis`, `csshiftjis`, `ms932`, `ms_kanji`, `shift-jis`, `sjis`, `windows-31j`, `x-sjis` |
| EUC-JP | `euc-jp`, `cseucpkdfmtjapanese`, `x-euc-jp` |
| ISO-2022-JP | `iso-2022-jp`, `csiso2022jp` |
| Big5 | `big5`, `big5-hkscs`, `cn-big5`, `csbig5`, `x-x-big5` |
| GBK | `gbk`, `chinese`, `csgb2312`, `csiso58gb231280`, `gb2312`, `gb_2312`, `gb_2312-80`, `iso-ir-58`, `x-gbk` |
| gb18030 | `gb18030` |
| UTF-16BE | `utf-16be`, `unicodefffe` |
| UTF-16LE | `utf-16le`, `utf-16`, `unicodefeff` |

> **중요**: `ascii`, `iso-8859-1`, `latin1`, `us-ascii`는 모두 `windows-1252`로 매핑된다. 이는 웹 호환성을 위한 의도적인 결정이다. 실제 웹에서 이 레이블들로 선언된 문서 대부분이 0x80~0x9F 범위의 windows-1252 전용 문자를 사용하기 때문이다.

### 5.2 get an encoding 알고리즘

레이블 문자열을 받아 대응하는 인코딩 객체를 반환하는 알고리즘이다.

```
get an encoding(label):
1. label의 앞뒤 ASCII 공백을 제거한다
2. label을 ASCII 소문자로 변환한다
3. 변환된 label이 인코딩 레이블 테이블에 있으면 해당 인코딩을 반환한다
4. 없으면 null을 반환한다 (실패)
```

```javascript
// JavaScript에서의 동작 예시
new TextDecoder('UTF-8');          // 유효: utf-8로 정규화
new TextDecoder('  utf-8  ');      // 유효: 공백 제거 후 utf-8
new TextDecoder('latin1');         // 유효: windows-1252로 매핑
new TextDecoder('ascii');          // 유효: windows-1252로 매핑
new TextDecoder('euc-kr');         // 유효
new TextDecoder('windows-949');    // 유효: EUC-KR로 매핑
new TextDecoder('ks_c_5601-1987'); // 유효: EUC-KR로 매핑

try {
  new TextDecoder('invalid-encoding'); // RangeError: 알 수 없는 레이블
} catch (e) {
  console.log(e instanceof RangeError); // true
}
```

---

## 6. BOM 스니핑

### 6.1 BOM을 통한 인코딩 감지

BOM(Byte Order Mark)은 바이트 시퀀스의 시작 부분에 위치하여 인코딩을 식별하는 데 사용될 수 있다.

| BOM 바이트 시퀀스 | 감지되는 인코딩 |
|------------------|---------------|
| 0xEF 0xBB 0xBF | UTF-8 |
| 0xFE 0xFF | UTF-16BE |
| 0xFF 0xFE | UTF-16LE |

### 6.2 BOM 스니핑 알고리즘

```
BOM sniff(ioQueue):
1. ioQueue의 처음 3바이트를 확인한다
2. 0xEF 0xBB 0xBF이면 → UTF-8 반환
3. 0xFE 0xFF이면 → UTF-16BE 반환
4. 0xFF 0xFE이면 → UTF-16LE 반환
5. 해당 없으면 → null 반환 (BOM 없음)
```

BOM 스니핑은 다른 인코딩 감지 메커니즘(Content-Type, meta charset 등)보다 **우선순위가 높다**.

```javascript
// BOM이 있는 UTF-8 데이터
const withBom = new Uint8Array([
  0xEF, 0xBB, 0xBF,               // UTF-8 BOM
  0xED, 0x95, 0x9C, 0xEA, 0xB8, 0x80  // "한글"
]);

// ignoreBOM: false (기본값) → BOM을 무시하고 제거
const decoder1 = new TextDecoder('utf-8');
console.log(decoder1.decode(withBom)); // "한글" (BOM 제거됨)

// ignoreBOM: true → BOM을 일반 문자로 취급하여 유지
const decoder2 = new TextDecoder('utf-8', { ignoreBOM: true });
console.log(decoder2.decode(withBom)); // "\uFEFF한글" (BOM 포함)
console.log(decoder2.decode(withBom).length); // 3
```

### 6.3 BOM과 인코딩 우선순위

실제 브라우저에서 BOM은 선언된 인코딩보다 우선한다.

```html
<!-- meta charset이 euc-kr이어도 BOM이 UTF-8이면 UTF-8로 디코딩 -->
<!-- 파일 시작에 0xEF 0xBB 0xBF가 있으면 UTF-8 -->
<meta charset="euc-kr">
<!-- ↑ BOM이 UTF-8이면 이 선언은 무시됨 -->
```

---

## 7. TextEncoder API

### 7.1 생성자

`TextEncoder`는 문자열을 UTF-8 바이트 시퀀스로 인코딩하는 API이다. 항상 UTF-8만 사용한다.

```javascript
const encoder = new TextEncoder();
// 매개변수 없음. 항상 UTF-8.
console.log(encoder.encoding); // "utf-8"
```

### 7.2 encode(input)

문자열을 받아 `Uint8Array`를 반환한다.

```javascript
const encoder = new TextEncoder();

// 기본 사용
const bytes = encoder.encode('Hello, 세계!');
console.log(bytes);
// Uint8Array [72, 101, 108, 108, 111, 44, 32, 236, 132, 184, 234, 179, 132, 33]

// 빈 문자열
console.log(encoder.encode('')); // Uint8Array []

// 이모지 (4바이트 UTF-8)
const emojiBytes = encoder.encode('🎉');
console.log([...emojiBytes]); // [240, 159, 142, 137]
console.log(emojiBytes.length); // 4

// 서로게이트 쌍이 아닌 lone surrogate는 U+FFFD로 대체됨
const withSurrogate = 'A\uD800B'; // lone surrogate 포함
const result = encoder.encode(withSurrogate);
console.log(new TextDecoder().decode(result)); // "A�B"
```

### 7.3 encodeInto(input, destination)

문자열을 지정된 `Uint8Array`에 직접 인코딩한다. 새 배열을 생성하지 않으므로 성능상 유리하다.

```javascript
const encoder = new TextEncoder();

// 충분한 크기의 버퍼
const buffer = new Uint8Array(100);
const result = encoder.encodeInto('안녕하세요', buffer);
console.log(result);
// { read: 5, written: 15 }
// read: 읽은 문자열의 코드 유닛 수
// written: 버퍼에 쓴 바이트 수

// 버퍼가 부족한 경우
const smallBuffer = new Uint8Array(5);
const result2 = encoder.encodeInto('안녕하세요', smallBuffer);
console.log(result2);
// { read: 1, written: 3 }
// "안"(3바이트)만 기록되고, "녕"(3바이트)은 공간 부족으로 기록되지 않음

// 최적 버퍼 크기 계산: 문자열 길이 * 3
// (BMP 문자는 최대 3바이트, 보충 문자는 4바이트이지만 JS에서 2 코드 유닛)
const optimalSize = 'Hello'.length * 3;
const optimalBuffer = new Uint8Array(optimalSize);
encoder.encodeInto('Hello', optimalBuffer);
```

### 7.4 encoding 속성

```javascript
const encoder = new TextEncoder();
console.log(encoder.encoding); // "utf-8" (읽기 전용, 항상 이 값)
```

---

## 8. TextDecoder API

### 8.1 생성자

```javascript
// 기본: UTF-8 디코더
const decoder1 = new TextDecoder();
console.log(decoder1.encoding); // "utf-8"

// 레이블 지정
const decoder2 = new TextDecoder('euc-kr');
console.log(decoder2.encoding); // "euc-kr"

// 옵션
const decoder3 = new TextDecoder('utf-8', {
  fatal: false,    // true이면 유효하지 않은 입력에서 TypeError 발생
  ignoreBOM: false // true이면 BOM을 제거하지 않음
});
```

### 8.2 decode(input, options)

```javascript
const decoder = new TextDecoder('utf-8');

// Uint8Array 디코딩
const bytes = new Uint8Array([0xED, 0x95, 0x9C, 0xEA, 0xB8, 0x80]);
console.log(decoder.decode(bytes)); // "한글"

// ArrayBuffer 디코딩
const buffer = new ArrayBuffer(3);
new Uint8Array(buffer).set([0x41, 0x42, 0x43]);
console.log(decoder.decode(buffer)); // "ABC"

// DataView 디코딩
const view = new DataView(buffer);
console.log(decoder.decode(view)); // "ABC"

// stream 옵션: 청크 단위로 디코딩할 때 사용
const streamDecoder = new TextDecoder('utf-8');
// "가" = 0xEA 0xB0 0x80 → 3바이트를 두 청크로 나눔
const chunk1 = new Uint8Array([0xEA, 0xB0]);
const chunk2 = new Uint8Array([0x80]);

let result = '';
result += streamDecoder.decode(chunk1, { stream: true }); // 아직 완전한 문자 없음
result += streamDecoder.decode(chunk2, { stream: true }); // "가" 완성
result += streamDecoder.decode(); // 스트림 종료
console.log(result); // "가"
```

### 8.3 EUC-KR 디코딩 예시

```javascript
// EUC-KR로 인코딩된 한국어 텍스트 디코딩
const eucKrDecoder = new TextDecoder('euc-kr');
const eucKrBytes = new Uint8Array([
  0xC7, 0xD1, 0xB1, 0xB9, 0xBE, 0xEE // "한국어"
]);
console.log(eucKrDecoder.decode(eucKrBytes)); // "한국어"

// 실전: fetch로 EUC-KR 페이지 읽기
async function fetchEucKr(url) {
  const response = await fetch(url);
  const buffer = await response.arrayBuffer();
  const decoder = new TextDecoder('euc-kr');
  return decoder.decode(buffer);
}
```

### 8.4 속성

```javascript
const decoder = new TextDecoder('euc-kr', { fatal: true, ignoreBOM: true });

console.log(decoder.encoding);  // "euc-kr" (읽기 전용)
console.log(decoder.fatal);     // true (읽기 전용)
console.log(decoder.ignoreBOM); // true (읽기 전용)
```

### 8.5 fatal 모드 상세

```javascript
// fatal: false (기본값) - 오류 시 U+FFFD 대체
const lenient = new TextDecoder('utf-8', { fatal: false });
const bad = new Uint8Array([0xFF, 0x41]);
console.log(lenient.decode(bad)); // "�A"

// fatal: true - 오류 시 TypeError 발생
const strict = new TextDecoder('utf-8', { fatal: true });
try {
  strict.decode(bad);
} catch (e) {
  console.log(e instanceof TypeError); // true
}

// 실전: 유효성 검사에 활용
function isValidEncoding(bytes, encoding) {
  try {
    new TextDecoder(encoding, { fatal: true }).decode(bytes);
    return true;
  } catch {
    return false;
  }
}
```

---

## 9. TextEncoderStream / TextDecoderStream

### 9.1 TextEncoderStream

문자열 청크를 UTF-8 `Uint8Array` 청크로 변환하는 Transform Stream이다.

```javascript
// TextEncoderStream 기본 사용
const encoderStream = new TextEncoderStream();
console.log(encoderStream.encoding); // "utf-8"
console.log(encoderStream.readable); // ReadableStream
console.log(encoderStream.writable); // WritableStream

// 파이프라인 구성
const readable = new ReadableStream({
  start(controller) {
    controller.enqueue('안녕');
    controller.enqueue('하세요');
    controller.close();
  }
});

const encoded = readable.pipeThrough(new TextEncoderStream());
const reader = encoded.getReader();

async function readAll() {
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    console.log(value); // Uint8Array 청크들
  }
}
readAll();
```

### 9.2 TextDecoderStream

바이트 청크를 문자열 청크로 변환하는 Transform Stream이다. 멀티바이트 문자가 청크 경계에 걸쳐도 올바르게 처리한다.

```javascript
// TextDecoderStream 기본 사용
const decoderStream = new TextDecoderStream('utf-8');
console.log(decoderStream.encoding);  // "utf-8"
console.log(decoderStream.fatal);     // false
console.log(decoderStream.ignoreBOM); // false

// 레거시 인코딩도 지원
const eucKrStream = new TextDecoderStream('euc-kr');

// fetch + stream API와 함께 사용
async function streamDecode(url) {
  const response = await fetch(url);
  const decoderStream = new TextDecoderStream('utf-8');
  const reader = response.body.pipeThrough(decoderStream).getReader();

  let text = '';
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    text += value;
  }
  return text;
}
```

### 9.3 멀티바이트 문자의 청크 경계 처리

스트림 디코더의 핵심 장점은 멀티바이트 문자가 청크 사이에 분할되어도 올바르게 처리한다는 것이다.

```javascript
// "가" = 0xEA 0xB0 0x80 (3바이트)
// 청크 경계에 걸친 경우를 자동으로 처리

const decoderStream = new TextDecoderStream('utf-8');
const writer = decoderStream.writable.getWriter();
const reader = decoderStream.readable.getReader();

async function demo() {
  // 바이트를 두 청크로 나누어 전송
  writer.write(new Uint8Array([0xEA, 0xB0])); // "가"의 앞 2바이트
  writer.write(new Uint8Array([0x80, 0x41])); // "가"의 마지막 바이트 + "A"
  writer.close();

  let result = '';
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    result += value;
  }
  console.log(result); // "가A" (올바르게 조합됨)
}
demo();
```

---

## 10. 웹에서의 인코딩 감지

브라우저는 HTML 문서의 인코딩을 결정하기 위해 여러 단계를 거친다. 우선순위가 높은 것부터 낮은 것 순으로 나열한다.

### 10.1 인코딩 결정 우선순위

```
1. BOM (Byte Order Mark)
   ↓ 없으면
2. HTTP Content-Type 헤더의 charset 매개변수
   ↓ 없으면
3. HTML 문서 내 prescan (처음 1024바이트에서 meta charset 탐색)
   ↓ 없으면
4. 부모 문서의 인코딩 (iframe 등)
   ↓ 없으면
5. 브라우저의 자동 감지 또는 기본값 (보통 windows-1252 또는 지역 설정)
```

### 10.2 meta charset

HTML 문서 내에서 인코딩을 선언하는 방법이다.

```html
<!-- HTML5 방식 (권장) -->
<meta charset="UTF-8">

<!-- HTML4 방식 (여전히 유효) -->
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
```

`<meta charset>`은 문서의 처음 1024바이트 이내에 위치해야 한다. 브라우저의 prescan 알고리즘이 이 범위만 검사하기 때문이다.

### 10.3 Content-Type 헤더

```
HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8
```

```javascript
// Node.js에서 응답 인코딩 설정
const http = require('http');
http.createServer((req, res) => {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.end('<h1>안녕하세요</h1>');
}).listen(3000);
```

### 10.4 Prescan 알고리즘

브라우저는 HTML 파싱 전에 문서의 처음 1024바이트를 스캔하여 인코딩을 결정한다.

```
Prescan 절차:
1. BOM이 있는지 확인
2. 처음 1024바이트에서 다음을 검색:
   a. <meta charset="..."> 형태
   b. <meta http-equiv="content-type" content="...;charset=..."> 형태
   c. <meta content="...;charset=..." http-equiv="content-type"> 형태
3. 찾으면 해당 인코딩 사용
4. 못 찾으면 다음 단계로 진행
```

### 10.5 인코딩 선언 모범 사례

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <!-- 반드시 첫 1024바이트 이내에 위치 -->
  <!-- title이나 다른 태그보다 먼저 선언 -->
  <meta charset="UTF-8">
  <title>페이지 제목</title>
</head>
<body>
  <p>내용</p>
</body>
</html>
```

---

## 11. 한국어 인코딩

### 11.1 EUC-KR과 cp949 (UHC)

EUC-KR은 KS X 1001 표준에 기반한 한국어 인코딩이다. 2,350자의 한글을 포함한다.

**Encoding Standard에서 `EUC-KR` 레이블은 실제로 cp949(Unified Hangul Code, 통합 완성형)를 의미한다.** cp949는 EUC-KR의 상위 집합으로, 11,172자의 한글 모두를 표현할 수 있다.

```javascript
// 표준에서 EUC-KR은 cp949를 의미
const decoder = new TextDecoder('euc-kr');
// windows-949로 선언해도 동일
const decoder2 = new TextDecoder('windows-949');
// KS X 1001 관련 레이블도 동일
const decoder3 = new TextDecoder('ks_c_5601-1987');

// 모두 같은 디코더
console.log(decoder.encoding);  // "euc-kr"
console.log(decoder2.encoding); // "euc-kr"
console.log(decoder3.encoding); // "euc-kr"
```

### 11.2 완성형과 조합형

한글 인코딩에는 두 가지 접근법이 있다.

**완성형 (조합된 글자 단위 인코딩)**

```
완성형: 완성된 한글 음절에 코드를 부여
- "가" = 0xB0A1
- "각" = 0xB0A2
- "간" = 0xB0A3
- KS X 1001: 2,350자 (자주 사용되는 한글만 포함)
- cp949 확장: 나머지 8,822자를 추가하여 11,172자 모두 포함
```

**조합형 (자모 단위 인코딩)**

```
조합형: 초성, 중성, 종성 각각에 코드를 부여하고 조합
- "한" = ㅎ + ㅏ + ㄴ
- 초성 ㅎ, 중성 ㅏ, 종성 ㄴ 각각의 코드를 조합
- 모든 한글 11,172자를 체계적으로 표현 가능
- 웹 표준에서는 사용되지 않음
```

### 11.3 EUC-KR의 구조

```
EUC-KR (cp949) 바이트 구조:

1바이트 영역 (ASCII 호환):
  0x00~0x7F: ASCII 문자

2바이트 영역 (KS X 1001 완성형):
  첫 번째 바이트: 0x81~0xFE
  두 번째 바이트: 0x41~0xFE (단, 0x7F 제외)

  한글 영역: 0xB0A1~0xC8FE (2,350자)
  한자 영역: 0xCAA1~0xFDFE (4,888자)
  특수문자: 0xA1A1~0xACFE

2바이트 영역 (UHC/cp949 확장):
  첫 번째 바이트: 0x81~0xC6
  두 번째 바이트: 0x41~0x5A, 0x61~0x7A, 0x81~0xFE
  (KS X 1001에 없는 나머지 한글 8,822자)
```

```javascript
// EUC-KR로 인코딩된 텍스트 디코딩
const eucKr = new TextDecoder('euc-kr');

// KS X 1001에 있는 "가"
const ga = new Uint8Array([0xB0, 0xA1]);
console.log(eucKr.decode(ga)); // "가"

// cp949 확장 영역의 "갂" (KS X 1001에 없는 글자)
const gak = new Uint8Array([0x81, 0x42]);
console.log(eucKr.decode(gak)); // "갂"
```

### 11.4 UTF-8 전환 역사

한국 웹의 인코딩 전환 과정이다.

| 시기 | 상황 |
|------|------|
| 1990년대 | 대부분의 한국 웹사이트가 EUC-KR 사용 |
| 2000년대 초반 | XML, RSS 등에서 UTF-8 사용 시작 |
| 2003년 | 한글 도메인 도입으로 국제화 인코딩 관심 증가 |
| 2005~2010 | 주요 포털(네이버, 다음 등)이 점진적으로 UTF-8 전환 |
| 2010년대 | 대부분의 새 웹사이트가 UTF-8 채택 |
| 2012년 | 네이버 메인 페이지 UTF-8 전환 |
| 현재 | 한국 웹의 대다수가 UTF-8 사용, 일부 레거시 시스템만 EUC-KR 유지 |

```javascript
// 한국어 텍스트의 인코딩별 바이트 크기 비교
const encoder = new TextEncoder(); // UTF-8

const text = '한글';
const utf8Bytes = encoder.encode(text);
console.log(`UTF-8: ${utf8Bytes.length}바이트`);  // UTF-8: 6바이트 (한글 1자 = 3바이트)

// EUC-KR에서는 한글 1자 = 2바이트 → "한글" = 4바이트
// ASCII에서는 한글 표현 불가
// UTF-16에서는 한글 1자 = 2바이트 → "한글" = 4바이트 (+ BOM 2바이트)
```

### 11.5 한국어 인코딩 감지 전략

레거시 한국어 콘텐츠를 처리할 때의 실용적 전략이다.

```javascript
// 인코딩 자동 감지 시도 함수
function decodeKorean(bytes) {
  // 1. BOM 확인
  if (bytes[0] === 0xEF && bytes[1] === 0xBB && bytes[2] === 0xBF) {
    return new TextDecoder('utf-8').decode(bytes);
  }

  // 2. UTF-8 유효성 검사
  try {
    const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
    return text; // 유효한 UTF-8
  } catch {
    // UTF-8이 아님
  }

  // 3. EUC-KR로 시도
  return new TextDecoder('euc-kr').decode(bytes);
}
```

---

## 12. 실용 예제

### 12.1 파일 읽기와 인코딩 변환

```javascript
// 브라우저: File API로 파일 읽기
async function readFileWithEncoding(file, encoding = 'utf-8') {
  const buffer = await file.arrayBuffer();
  const decoder = new TextDecoder(encoding);
  return decoder.decode(buffer);
}

// 사용 예
const fileInput = document.querySelector('input[type="file"]');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];

  // UTF-8 파일
  const utf8Text = await readFileWithEncoding(file, 'utf-8');

  // EUC-KR 파일
  const eucKrText = await readFileWithEncoding(file, 'euc-kr');
});
```

```javascript
// Node.js: Buffer를 사용한 인코딩 변환
const fs = require('fs');

// EUC-KR 파일을 UTF-8로 변환하여 읽기
function readEucKrFile(filePath) {
  const buffer = fs.readFileSync(filePath);
  const decoder = new TextDecoder('euc-kr');
  return decoder.decode(buffer);
}

// UTF-8로 저장
function convertToUtf8(inputPath, outputPath) {
  const text = readEucKrFile(inputPath);
  const encoder = new TextEncoder();
  const utf8Bytes = encoder.encode(text);
  fs.writeFileSync(outputPath, utf8Bytes);
}
```

### 12.2 인코딩 변환

```javascript
// EUC-KR → UTF-8 변환 (웹 브라우저)
async function convertEucKrToUtf8(eucKrArrayBuffer) {
  // 1. EUC-KR 바이트를 문자열로 디코딩
  const decoder = new TextDecoder('euc-kr');
  const text = decoder.decode(eucKrArrayBuffer);

  // 2. 문자열을 UTF-8로 인코딩
  const encoder = new TextEncoder();
  const utf8Bytes = encoder.encode(text);

  return utf8Bytes;
}

// Shift_JIS → UTF-8 변환
function convertShiftJisToUtf8(shiftJisBuffer) {
  const decoder = new TextDecoder('shift_jis');
  const text = decoder.decode(shiftJisBuffer);
  const encoder = new TextEncoder();
  return encoder.encode(text);
}
```

### 12.3 Base64와 인코딩

```javascript
// 문자열 → Base64 (UTF-8 경유)
function stringToBase64(str) {
  const encoder = new TextEncoder();
  const bytes = encoder.encode(str);
  // bytes를 이진 문자열로 변환
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary);
}

// Base64 → 문자열 (UTF-8 경유)
function base64ToString(base64) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  const decoder = new TextDecoder('utf-8');
  return decoder.decode(bytes);
}

// 사용
const encoded = stringToBase64('안녕하세요');
console.log(encoded); // "7JWI64WV7ZWY7IS47JqU"
console.log(base64ToString(encoded)); // "안녕하세요"
```

### 12.4 바이너리 데이터 처리

```javascript
// 바이너리 데이터와 텍스트를 혼합한 프로토콜 파싱
function parsePacket(buffer) {
  const view = new DataView(buffer);

  // 처음 4바이트: 패킷 길이 (빅 엔디안 uint32)
  const length = view.getUint32(0, false);

  // 다음 1바이트: 메시지 타입
  const type = view.getUint8(4);

  // 나머지: UTF-8 텍스트 페이로드
  const textBytes = new Uint8Array(buffer, 5);
  const decoder = new TextDecoder('utf-8');
  const payload = decoder.decode(textBytes);

  return { length, type, payload };
}

// 바이너리 프레임 생성
function createPacket(type, text) {
  const encoder = new TextEncoder();
  const textBytes = encoder.encode(text);

  const buffer = new ArrayBuffer(5 + textBytes.length);
  const view = new DataView(buffer);

  // 헤더
  view.setUint32(0, 5 + textBytes.length, false); // 패킷 길이
  view.setUint8(4, type); // 메시지 타입

  // 페이로드
  new Uint8Array(buffer, 5).set(textBytes);

  return buffer;
}
```

### 12.5 스트림 기반 대용량 파일 처리

```javascript
// 대용량 EUC-KR 파일을 UTF-8로 스트림 변환
async function convertLargeFile(inputUrl, outputStream) {
  const response = await fetch(inputUrl);

  // EUC-KR → 문자열 스트림
  const textStream = response.body.pipeThrough(
    new TextDecoderStream('euc-kr')
  );

  // 문자열 → UTF-8 바이트 스트림
  const utf8Stream = textStream.pipeThrough(
    new TextEncoderStream()
  );

  // 출력으로 파이프
  await utf8Stream.pipeTo(outputStream);
}
```

### 12.6 fetch와 인코딩

```javascript
// 다양한 인코딩의 페이지 가져오기
async function fetchWithEncoding(url, encoding) {
  const response = await fetch(url);

  if (encoding) {
    // 명시적 인코딩 지정
    const buffer = await response.arrayBuffer();
    return new TextDecoder(encoding).decode(buffer);
  }

  // Content-Type에서 인코딩 감지
  const contentType = response.headers.get('Content-Type') || '';
  const charsetMatch = contentType.match(/charset=([^\s;]+)/i);
  const detectedEncoding = charsetMatch ? charsetMatch[1] : 'utf-8';

  const buffer = await response.arrayBuffer();
  return new TextDecoder(detectedEncoding).decode(buffer);
}

// 사용
const text = await fetchWithEncoding('https://example.kr/legacy', 'euc-kr');
```

### 12.7 Hex 덤프 유틸리티

```javascript
// 바이트 시퀀스를 읽기 쉬운 형태로 출력
function hexDump(bytes) {
  const arr = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
  const lines = [];

  for (let i = 0; i < arr.length; i += 16) {
    const hex = [];
    const ascii = [];

    for (let j = 0; j < 16; j++) {
      if (i + j < arr.length) {
        hex.push(arr[i + j].toString(16).padStart(2, '0'));
        const ch = arr[i + j];
        ascii.push(ch >= 0x20 && ch <= 0x7E ? String.fromCharCode(ch) : '.');
      } else {
        hex.push('  ');
        ascii.push(' ');
      }
    }

    const offset = i.toString(16).padStart(8, '0');
    lines.push(`${offset}  ${hex.slice(0,8).join(' ')}  ${hex.slice(8).join(' ')}  |${ascii.join('')}|`);
  }

  return lines.join('\n');
}

// 사용
const encoder = new TextEncoder();
console.log(hexDump(encoder.encode('Hello, 세계!')));
// 00000000  48 65 6c 6c 6f 2c 20 ec  84 b8 ea b3 84 21     |Hello, ......!|
```

### 12.8 인코딩 감지 및 변환 클래스

```javascript
// 실전용 인코딩 유틸리티 클래스
class EncodingHelper {
  // 지원되는 인코딩인지 확인
  static isSupported(encoding) {
    try {
      new TextDecoder(encoding);
      return true;
    } catch {
      return false;
    }
  }

  // 정규 인코딩 이름 가져오기
  static getCanonicalName(label) {
    try {
      return new TextDecoder(label).encoding;
    } catch {
      return null;
    }
  }

  // UTF-8 유효성 검사
  static isValidUTF8(bytes) {
    try {
      new TextDecoder('utf-8', { fatal: true }).decode(bytes);
      return true;
    } catch {
      return false;
    }
  }

  // 바이트 시퀀스에서 BOM 감지
  static detectBOM(bytes) {
    if (bytes[0] === 0xEF && bytes[1] === 0xBB && bytes[2] === 0xBF) {
      return { encoding: 'utf-8', bomLength: 3 };
    }
    if (bytes[0] === 0xFE && bytes[1] === 0xFF) {
      return { encoding: 'utf-16be', bomLength: 2 };
    }
    if (bytes[0] === 0xFF && bytes[1] === 0xFE) {
      return { encoding: 'utf-16le', bomLength: 2 };
    }
    return null;
  }

  // 인코딩 변환
  static convert(bytes, fromEncoding, toEncoding = 'utf-8') {
    const text = new TextDecoder(fromEncoding).decode(bytes);
    if (toEncoding === 'utf-8') {
      return new TextEncoder().encode(text);
    }
    throw new Error('TextEncoder는 UTF-8만 지원합니다');
  }
}

// 사용
console.log(EncodingHelper.isSupported('euc-kr'));         // true
console.log(EncodingHelper.getCanonicalName('latin1'));    // "windows-1252"
console.log(EncodingHelper.getCanonicalName('windows-949')); // "euc-kr"
```

---

## 참고 자료

- [WHATWG Encoding Standard](https://encoding.spec.whatwg.org/)
- [MDN - TextEncoder](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoder)
- [MDN - TextDecoder](https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder)
- [MDN - TextEncoderStream](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoderStream)
- [MDN - TextDecoderStream](https://developer.mozilla.org/en-US/docs/Web/API/TextDecoderStream)
- [Unicode 공식 사이트](https://unicode.org/)
- [W3C - Character encodings for beginners](https://www.w3.org/International/questions/qa-what-is-encoding)
