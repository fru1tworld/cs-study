# Infra Standard 상세 가이드

## 목차

1. [개요](#1-개요)
2. [원시 타입](#2-원시-타입)
3. [자료 구조](#3-자료-구조)
4. [알고리즘 표기 규약](#4-알고리즘-표기-규약)
5. [문자열 처리](#5-문자열-처리)
6. [바이트 시퀀스 처리](#6-바이트-시퀀스-처리)
7. [코드 포인트 분류](#7-코드-포인트-분류)
8. [Namespaces](#8-namespaces)
9. [Forgiving-base64 인코딩/디코딩](#9-forgiving-base64-인코딩디코딩)
10. [실용적 의미](#10-실용적-의미)
11. [다른 스펙에서의 활용 예시](#11-다른-스펙에서의-활용-예시)
12. [참고 자료](#12-참고-자료)

---

## 1. 개요

### Infra Standard란

Infra Standard는 WHATWG에서 관리하는 Living Standard로, 다른 모든 웹 표준 문서들이 공유하는 공통 기반 정의(common infrastructure)를 제공한다. 이 표준은 웹 API를 직접 정의하지 않지만, HTML Standard, DOM Standard, Fetch Standard, URL Standard 등 다른 표준들이 사용하는 기본 개념, 자료 구조, 알고리즘 표기법을 정의한다.

```
[Infra Standard의 위치]

                 ┌─────────────────────────────────────┐
                 │         Web Standards 생태계          │
                 │                                     │
                 │  HTML    DOM    Fetch   URL    ...   │
                 │  Std     Std    Std     Std          │
                 │    │      │      │       │           │
                 │    └──────┴──────┴───────┘           │
                 │              │                       │
                 │    ┌─────────▼──────────┐            │
                 │    │  Infra Standard    │            │
                 │    │  (공통 기반 정의)   │            │
                 │    │                    │            │
                 │    │  - 원시 타입       │            │
                 │    │  - 자료 구조       │            │
                 │    │  - 알고리즘 표기   │            │
                 │    │  - 문자열 처리     │            │
                 │    │  - 바이트 처리     │            │
                 │    │  - 코드 포인트     │            │
                 │    │  - 네임스페이스    │            │
                 │    └────────────────────┘            │
                 └─────────────────────────────────────┘
```

### 왜 Infra Standard가 필요한가

1. 일관성: 여러 표준이 같은 용어와 개념을 동일하게 사용
2. 중복 제거: 각 표준에서 반복적으로 정의하던 기본 개념을 한 곳에 집중
3. 정확성: 알고리즘의 표기 규약을 통일하여 모호함 제거
4. 참조 용이성: 다른 스펙을 읽을 때 기본 개념을 한 곳에서 확인 가능

```
[이전 (Infra Standard 없을 때)]

HTML Standard에서: "Let list be an ordered sequence of..."
DOM Standard에서:  "Let result be a new list..."
Fetch Standard에서: "Let items be an ordered collection..."
→ 같은 개념을 다른 용어로 표현 → 혼란

[이후 (Infra Standard 도입)]

모든 표준에서: "Let list be a new list"
→ "list"는 Infra Standard에 정의된 정확한 개념
→ 일관성 보장
```

---

## 2. 원시 타입

### 2.1 Byte

```
[byte]

정의: 0x00 ~ 0xFF (0 ~ 255) 범위의 8비트 부호 없는 정수

표기:
  0x41      16진수 표기
  0b01000001  2진수 표기 (사용 빈도 낮음)

사용 예:
  "Let byte be 0x41" → byte = 65 (ASCII 'A')

JavaScript 대응:
  Uint8Array의 각 요소
```

### 2.2 Byte Sequence

```
[byte sequence]

정의: 0개 이상의 byte의 순서 있는 시퀀스

표기:
  `Hello`     → ASCII 인코딩된 바이트 시퀀스 (0x48 0x65 0x6C 0x6C 0x6F)
  백틱(`)으로 감싸서 표기

빈 바이트 시퀀스: 길이가 0인 바이트 시퀀스

JavaScript 대응:
  Uint8Array
  ArrayBuffer

예시:
  "Let bytes be `Content-Type`"
  → bytes = [0x43, 0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x2D,
             0x54, 0x79, 0x70, 0x65]
```

### 2.3 Code Point

```
[code point]

정의: 유니코드 코드 포인트 (U+0000 ~ U+10FFFF)

표기:
  U+0041     유니코드 코드 포인트 표기
  "A" (U+0041)  문자와 코드 포인트 병기

범위:
  U+0000 ~ U+007F     ASCII (128개)
  U+0080 ~ U+FFFF     BMP (Basic Multilingual Plane)
  U+10000 ~ U+10FFFF  보충 평면 (Supplementary Planes)

특수 코드 포인트:
  U+0000     NULL
  U+000A     LINE FEED (LF)
  U+000D     CARRIAGE RETURN (CR)
  U+0020     SPACE
  U+FFFD     REPLACEMENT CHARACTER (잘못된 인코딩의 대체)
  U+FEFF     BOM (Byte Order Mark)
```

### 2.4 String

```
[string]

정의: 0개 이상의 코드 포인트의 순서 있는 시퀀스

표기:
  "Hello"    큰따옴표로 감싸서 표기

빈 문자열: 길이가 0인 문자열, ""로 표기

스칼라 값 문자열 (scalar value string):
  서로게이트 코드 포인트(U+D800 ~ U+DFFF)를 포함하지 않는 문자열
  → 유효한 유니코드 텍스트

JavaScript 대응:
  DOMString (JavaScript의 String)
  USVString (scalar value string)
```

### 2.5 Boolean

```
[boolean]

정의: true 또는 false

표기:
  true, false (소문자)

JavaScript 대응: boolean (true / false)
```

### 2.6 Null

```
[null]

정의: 값이 없음을 나타내는 특별한 값

표기:
  null (소문자)

사용 예:
  "Let result be null"
  "If element is null, then return"

JavaScript 대응: null
```

---

## 3. 자료 구조

### 3.1 List

```
[list]

정의: 유한한 수의 항목을 순서대로 포함하는 자료 구조

표기:
  « 1, 2, 3 »          → 3개 항목을 가진 리스트
  « "a", "b", "c" »    → 문자열 리스트
  « »                   → 빈 리스트

생성:
  "Let list be a new list"
  "Let list be « 1, 2, 3 »"

크기:
  "list's size" → 항목의 수

항목 접근:
  "list[0]"  → 첫 번째 항목 (0-indexed)
  "list[1]"  → 두 번째 항목

포함 여부:
  "If list contains item"
  "If list does not contain item"

추가:
  "Append item to list"     → 끝에 추가
  "Prepend item to list"    → 처음에 추가
  "Insert item before index in list"

제거:
  "Remove item from list"
  "Remove list[index]"

교체:
  "Replace item in list with newItem"

반복:
  "For each item of list"

정렬:
  "Sort list" (비교 함수와 함께)

복제:
  "Clone list" → 얕은 복사
```

```javascript
// JavaScript에서의 list 대응
const list = [];              // « »
const list2 = [1, 2, 3];     // « 1, 2, 3 »

list.length;                  // size
list[0];                      // list[0]
list.includes(item);          // contains
list.push(item);              // append
list.unshift(item);           // prepend
list.splice(index, 1);       // remove at index
list.splice(index, 0, item); // insert before index

for (const item of list) {}  // for each
list.sort((a, b) => ...);    // sort
[...list];                    // clone
```

### 3.2 Ordered Set

```
[ordered set]

정의: 중복을 허용하지 않는 list
       = 모든 항목이 고유한 list

표기:
  « 1, 2, 3 »  (list와 동일한 표기, 문맥으로 구분)

추가:
  "Append item to set"
  → 이미 set에 item이 있으면 아무것도 하지 않음
  → 없으면 끝에 추가

기타 연산: list와 동일

JavaScript 대응:
  - 정확히 대응하는 것은 없음
  - Set은 순서를 보장하지만, 인덱스 접근이 다름
  - 배열 + 중복 검사로 구현 가능
```

```javascript
// ordered set 시뮬레이션
class OrderedSet {
    constructor() {
        this.items = [];
    }

    append(item) {
        if (!this.items.includes(item)) {
            this.items.push(item);
        }
    }

    prepend(item) {
        if (!this.items.includes(item)) {
            this.items.unshift(item);
        }
    }

    remove(item) {
        const index = this.items.indexOf(item);
        if (index !== -1) {
            this.items.splice(index, 1);
        }
    }

    contains(item) {
        return this.items.includes(item);
    }

    get size() {
        return this.items.length;
    }

    [Symbol.iterator]() {
        return this.items[Symbol.iterator]();
    }
}
```

### 3.3 Stack과 Queue

```
[stack]

정의: LIFO(Last In, First Out) 방식의 list

연산:
  "Push item onto stack"   → 스택 상단에 추가
  "Pop from stack"         → 스택 상단에서 제거 및 반환
  "Peek at stack"          → 스택 상단 확인 (제거하지 않음)

JavaScript 대응:
  const stack = [];
  stack.push(item);    // push
  stack.pop();         // pop
  stack[stack.length - 1]; // peek
```

```
[queue]

정의: FIFO(First In, First Out) 방식의 list

연산:
  "Enqueue item in queue"   → 큐 끝에 추가
  "Dequeue from queue"      → 큐 앞에서 제거 및 반환

JavaScript 대응:
  const queue = [];
  queue.push(item);    // enqueue
  queue.shift();       // dequeue
```

### 3.4 Map (Ordered Map)

```
[ordered map]

정의: 키-값 쌍의 순서 있는 컬렉션
       키는 고유함

표기:
  «[ "key1" → value1, "key2" → value2 ]»

생성:
  "Let map be a new ordered map"
  "Let map be «[ "a" → 1, "b" → 2 ]»"

항목 접근:
  "map[key]"          → 키에 대응하는 값
  "map["key"]"        → 문자열 키 접근

포함 여부:
  "If map contains key"
  "If map[key] exists"

설정:
  "Set map[key] to value"
  → 키가 있으면 값 갱신
  → 키가 없으면 새 항목 추가 (순서 끝에)

제거:
  "Remove map[key]"

크기:
  "map's size"

키 목록:
  "map's keys"   → 키들의 ordered set

값 목록:
  "map's values"  → 값들의 list

반복:
  "For each key → value of map"

정렬:
  "Sort map" (키 기준 등)
```

```javascript
// JavaScript에서의 ordered map 대응
const map = new Map();                          // 새 ordered map
const map2 = new Map([['a', 1], ['b', 2]]);    // 초기값과 함께

map.get('key');           // map[key]
map.has('key');           // contains
map.set('key', value);   // set
map.delete('key');        // remove
map.size;                 // size
[...map.keys()];          // keys
[...map.values()];        // values

for (const [key, value] of map) {} // for each
```

### 3.5 Struct

```
[struct]

정의: 고정된 이름의 필드(항목)들을 가진 레코드

표기:
  struct {
    name (a string)
    age (a number)
    active (a boolean, initially true)
  }

생성:
  "Let person be a new struct with
    name set to "Kim",
    age set to 30"
  → person.active = true (초기값)

필드 접근:
  "person's name"     → "Kim"
  "person's age"      → 30
  "person's active"   → true

필드 설정:
  "Set person's name to "Lee""
```

```javascript
// JavaScript에서의 struct 대응
// 일반 객체로 대응
const person = {
    name: 'Kim',
    age: 30,
    active: true  // 초기값
};

// 또는 class로 대응
class Person {
    name;
    age;
    active = true;  // 초기값

    constructor(name, age) {
        this.name = name;
        this.age = age;
    }
}
```

### 3.6 Tuple

```
[tuple]

정의: 고정된 수의 항목을 가진 불변 시퀀스

표기:
  (item1, item2)         → 2-tuple
  (item1, item2, item3)  → 3-tuple

차이점 (list와):
  - tuple은 항목 수가 고정됨
  - tuple은 추가/제거 불가
  - tuple의 각 위치에 다른 타입이 올 수 있음

사용 예:
  "Let pair be (name, value)"
  "Let triple be (scheme, host, port)"

JavaScript 대응:
  const pair = [name, value];  // 배열로 표현
  const [a, b] = pair;        // 구조 분해
```

---

## 4. 알고리즘 표기 규약

### 4.1 기본 구문

Infra Standard는 다른 스펙에서 알고리즘을 기술할 때 사용하는 표준적인 구문을 정의한다.

```
[알고리즘 표기 구문]

Let     : 변수 선언
Set     : 값 할당
Return  : 값 반환 (알고리즘 종료)
Assert  : 조건 검증 (실패 시 구현 오류)
Throw   : 예외 발생

For each : 반복문
While    : 조건부 반복
If/Else  : 조건 분기
Switch   : 다중 분기
Continue : 다음 반복으로
Break    : 반복 중단
```

### 4.2 Let (변수 선언)

```
스펙 표기:
  "Let result be null."
  "Let list be a new list."
  "Let count be 0."

의미:
  새로운 변수를 선언하고 초기값을 할당

JavaScript 대응:
  let result = null;
  let list = [];
  let count = 0;
```

### 4.3 Set (값 할당)

```
스펙 표기:
  "Set result to "hello"."
  "Set count to count + 1."
  "Set element's id to "main"."

의미:
  기존 변수 또는 속성에 새 값 할당

JavaScript 대응:
  result = 'hello';
  count = count + 1;
  element.id = 'main';
```

### 4.4 Return (반환)

```
스펙 표기:
  "Return result."
  "Return true."
  "Return null."
  "Return." (값 없이 반환 = undefined/void)

의미:
  알고리즘을 종료하고 값을 반환

JavaScript 대응:
  return result;
  return true;
  return null;
  return;
```

### 4.5 Assert (검증)

```
스펙 표기:
  "Assert: list is not empty."
  "Assert: type is "text"."
  "Assert: index < list's size."

의미:
  조건이 반드시 참이어야 함
  거짓이면 스펙 또는 구현의 버그
  런타임 에러가 아닌 개발 시 검증용

JavaScript 대응:
  console.assert(list.length > 0);
  // 또는 프로덕션에서는 무시
```

### 4.6 Throw (예외)

```
스펙 표기:
  "Throw a TypeError."
  "Throw a "NotFoundError" DOMException."
  "Throw a RangeError."

의미:
  예외를 발생시켜 알고리즘을 중단

JavaScript 대응:
  throw new TypeError();
  throw new DOMException('...', 'NotFoundError');
  throw new RangeError();
```

### 4.7 For each (반복)

```
스펙 표기:
  "For each item of list:"
  "For each item of list, in reverse:"
  "For each key → value of map:"
  "For each index of list's indices:"

의미:
  컬렉션의 각 항목에 대해 반복

JavaScript 대응:
  for (const item of list) { ... }
  for (const item of [...list].reverse()) { ... }
  for (const [key, value] of map) { ... }
  for (let i = 0; i < list.length; i++) { ... }
```

### 4.8 While (조건부 반복)

```
스펙 표기:
  "While condition is true:"
  "While list is not empty:"
  "While count < 10:"

의미:
  조건이 참인 동안 반복

JavaScript 대응:
  while (condition) { ... }
  while (list.length > 0) { ... }
  while (count < 10) { ... }
```

### 4.9 If/Else (조건 분기)

```
스펙 표기:
  "If condition, then:"
  "If condition, then return true."
  "Otherwise:" (= else)
  "Otherwise, if condition2:" (= else if)

의미:
  조건에 따른 분기

JavaScript 대응:
  if (condition) { ... }
  if (condition) return true;
  else { ... }
  else if (condition2) { ... }
```

### 4.10 Switch (다중 분기)

```
스펙 표기:
  "Switch on type:"
    "text": ...
    "image": ...
    "audio": ...

의미:
  값에 따른 다중 분기

JavaScript 대응:
  switch (type) {
    case 'text': ...; break;
    case 'image': ...; break;
    case 'audio': ...; break;
  }
```

### 4.11 Continue와 Break

```
스펙 표기:
  "Continue"    → 현재 반복의 나머지를 건너뛰고 다음 반복으로
  "Break"       → 현재 반복을 중단

JavaScript 대응:
  continue;
  break;
```

### 4.12 알고리즘 예시

```
[URL 파싱 알고리즘의 일부 (간략화)]

To parse a URL string input:

1. Let url be the result of running the basic URL parser on input.
2. If url is failure, then return failure.
3. If url's scheme is "blob", then:
   a. Set url's blob URL entry to the result of resolving
      the blob URL url.
4. Return url.

JavaScript 대응:
function parseURL(input) {
    let url = basicURLParser(input);
    if (url === null) return null;  // failure
    if (url.scheme === 'blob') {
        url.blobURLEntry = resolveBlobURL(url);
    }
    return url;
}
```

---

## 5. 문자열 처리

### 5.1 ASCII Lowercase / Uppercase

```
[ASCII lowercase]
문자열의 모든 ASCII 대문자(A-Z)를 소문자(a-z)로 변환

스펙 표기:
  "ASCII lowercase of string"

매핑:
  U+0041 (A) ~ U+005A (Z) → U+0061 (a) ~ U+007A (z)
  다른 코드 포인트는 변환하지 않음

예시:
  ASCII lowercase of "Hello WORLD" → "hello world"
  ASCII lowercase of "123-ABC" → "123-abc"
  ASCII lowercase of "ÄBC" → "Äbc" (Ä는 ASCII가 아니므로 변환 안 됨)

JavaScript 대응:
  // 정확한 ASCII lowercase:
  function asciiLowercase(str) {
      return str.replace(/[A-Z]/g, c => c.toLowerCase());
  }
  // 주의: String.prototype.toLowerCase()는 유니코드도 변환하므로 다름
```

```
[ASCII uppercase]
문자열의 모든 ASCII 소문자(a-z)를 대문자(A-Z)로 변환

스펙 표기:
  "ASCII uppercase of string"

예시:
  ASCII uppercase of "hello world" → "HELLO WORLD"
  ASCII uppercase of "café" → "CAFé" (é는 ASCII가 아니므로 변환 안 됨)

JavaScript 대응:
  function asciiUppercase(str) {
      return str.replace(/[a-z]/g, c => c.toUpperCase());
  }
```

### 5.2 Strip

```
[strip leading and trailing ASCII whitespace]

스펙 표기:
  "Strip leading and trailing ASCII whitespace from string"

ASCII 공백:
  U+0009 (TAB)
  U+000A (LF)
  U+000C (FF)
  U+000D (CR)
  U+0020 (SPACE)

예시:
  strip "  hello  " → "hello"
  strip "\t\nhello\r\n" → "hello"
  strip "hello" → "hello" (변화 없음)

JavaScript 대응:
  // ASCII 공백만 제거:
  function stripASCIIWhitespace(str) {
      return str.replace(/^[\t\n\f\r ]+|[\t\n\f\r ]+$/g, '');
  }
  // 주의: String.prototype.trim()은 유니코드 공백도 제거하므로 다름
```

### 5.3 Collapse

```
[strip and collapse ASCII whitespace]

연속된 ASCII 공백을 하나의 공백(U+0020)으로 축소하고,
앞뒤 공백을 제거

스펙 표기:
  "Strip and collapse ASCII whitespace in string"

예시:
  "  hello   world  " → "hello world"
  "hello\t\n\tworld" → "hello world"
  "  a  b  c  " → "a b c"

JavaScript 대응:
  function stripAndCollapse(str) {
      return str.replace(/[\t\n\f\r ]+/g, ' ').trim();
  }
```

### 5.4 코드 포인트 비교

```
[ASCII case-insensitive comparison]

두 문자열을 ASCII 대소문자를 무시하고 비교

스펙 표기:
  "If A is an ASCII case-insensitive match for B"

규칙:
  A-Z와 a-z만 동일하게 취급
  비ASCII 문자는 정확히 일치해야 함

예시:
  "Content-Type" ≡ "content-type"  (ASCII case-insensitive match)
  "CAFÉ" ≠ "café" (É ≠ é, 비ASCII이므로)

JavaScript 대응:
  function asciiCaseInsensitiveMatch(a, b) {
      if (a.length !== b.length) return false;
      for (let i = 0; i < a.length; i++) {
          let ca = a.charCodeAt(i);
          let cb = b.charCodeAt(i);
          // A-Z → a-z 매핑
          if (ca >= 0x41 && ca <= 0x5A) ca += 0x20;
          if (cb >= 0x41 && cb <= 0x5A) cb += 0x20;
          if (ca !== cb) return false;
      }
      return true;
  }
```

### 5.5 Starts with / Ends with

```
[starts with]

스펙 표기:
  "If string starts with prefix"

예시:
  "https://example.com" starts with "https://" → true
  "Hello World" starts with "Hello" → true

JavaScript 대응:
  string.startsWith(prefix);
```

```
[ends with]

스펙 표기:
  "If string ends with suffix"

예시:
  "index.html" ends with ".html" → true

JavaScript 대응:
  string.endsWith(suffix);
```

### 5.6 Split

```
[split on]

스펙 표기:
  "Split string on separator"
  "Strictly split string on separator"

차이:
  split: 빈 문자열을 제거하지 않음
  strictly split: 빈 문자열도 유지

예시:
  split "a,b,,c" on "," → « "a", "b", "", "c" »  (4개)
  "Split on ASCII whitespace":
    split "hello  world" on ASCII whitespace → « "hello", "world" »
    (연속 공백은 하나로 취급)

JavaScript 대응:
  string.split(separator);
  // 빈 문자열 처리는 상황에 따라 다름
```

### 5.7 Concatenate

```
[concatenate]

스펙 표기:
  "Concatenate « string1, string2, string3 » with separator"

예시:
  concatenate « "a", "b", "c" » with "," → "a,b,c"
  concatenate « "hello", "world" » with " " → "hello world"
  concatenate « "a", "b" » → "ab" (구분자 없으면 그냥 연결)

JavaScript 대응:
  [string1, string2, string3].join(separator);
```

### 5.8 Scalar Value와 Surrogate

```
[scalar value]

정의: 서로게이트(surrogate)가 아닌 코드 포인트
      = U+0000 ~ U+D7FF 또는 U+E000 ~ U+10FFFF

서로게이트 코드 포인트:
  U+D800 ~ U+DBFF  Leading surrogate (고위 서로게이트)
  U+DC00 ~ U+DFFF  Trailing surrogate (저위 서로게이트)

서로게이트는 UTF-16 인코딩에서 보충 평면 문자를 표현하기 위해 사용
단독으로는 유효한 유니코드 문자가 아님

[convert to scalar value string]
서로게이트 코드 포인트를 U+FFFD (REPLACEMENT CHARACTER)로 교체

예시:
  입력: "Hello" + U+D800 + "World"
  출력: "Hello" + U+FFFD + "World"

JavaScript 대응:
  // 고립 서로게이트를 U+FFFD로 교체
  function toScalarValueString(str) {
      return str.replace(
          /[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]/g,
          '\uFFFD'
      );
  }
```

---

## 6. 바이트 시퀀스 처리

### 6.1 바이트 시퀀스 비교

```
[byte sequence comparison]

두 바이트 시퀀스가 동일한지 비교

스펙 표기:
  "If bytes1 is bytes2"

규칙:
  - 길이가 같아야 함
  - 각 위치의 바이트가 동일해야 함

JavaScript 대응:
  function bytesEqual(a, b) {
      if (a.length !== b.length) return false;
      for (let i = 0; i < a.length; i++) {
          if (a[i] !== b[i]) return false;
      }
      return true;
  }
```

### 6.2 바이트 시퀀스 Lowercase

```
[byte-lowercase]

바이트 시퀀스의 ASCII 대문자 바이트를 소문자 바이트로 변환

스펙 표기:
  "Byte-lowercase bytes"

매핑:
  0x41 (A) ~ 0x5A (Z) → 0x61 (a) ~ 0x7A (z)
  나머지 바이트는 변경하지 않음

JavaScript 대응:
  function byteLowercase(bytes) {
      const result = new Uint8Array(bytes.length);
      for (let i = 0; i < bytes.length; i++) {
          if (bytes[i] >= 0x41 && bytes[i] <= 0x5A) {
              result[i] = bytes[i] + 0x20;
          } else {
              result[i] = bytes[i];
          }
      }
      return result;
  }
```

### 6.3 Isomorphic Encode / Decode

```
[isomorphic encode]

문자열을 바이트 시퀀스로 변환
각 코드 포인트를 동일한 값의 바이트로 매핑
(U+0000 ~ U+00FF 범위만 허용)

스펙 표기:
  "Isomorphic encode string"

예시:
  isomorphic encode "ABC" → `ABC` (0x41 0x42 0x43)
  isomorphic encode "ÿ"  → 0xFF  (U+00FF → 0xFF)

제약: U+00FF보다 큰 코드 포인트가 있으면 실패

JavaScript 대응:
  function isomorphicEncode(str) {
      const bytes = new Uint8Array(str.length);
      for (let i = 0; i < str.length; i++) {
          const cp = str.charCodeAt(i);
          if (cp > 0xFF) throw new Error('Code point out of range');
          bytes[i] = cp;
      }
      return bytes;
  }
```

```
[isomorphic decode]

바이트 시퀀스를 문자열로 변환
각 바이트를 동일한 값의 코드 포인트로 매핑

스펙 표기:
  "Isomorphic decode bytes"

예시:
  isomorphic decode `ABC` → "ABC"
  isomorphic decode [0xFF] → "ÿ" (U+00FF)

JavaScript 대응:
  function isomorphicDecode(bytes) {
      let result = '';
      for (let i = 0; i < bytes.length; i++) {
          result += String.fromCharCode(bytes[i]);
      }
      return result;
  }
  // 또는:
  // String.fromCharCode(...bytes);
```

---

## 7. 코드 포인트 분류

Infra Standard는 코드 포인트를 여러 그룹으로 분류한다.

### 7.1 ASCII 관련 분류

```
[ASCII code point]
U+0000 ~ U+007F (128개)
= 0x00 ~ 0x7F

[ASCII alpha]
ASCII 알파벳 문자
= ASCII upper alpha + ASCII lower alpha
= A-Z (U+0041 ~ U+005A) + a-z (U+0061 ~ U+007A)

[ASCII upper alpha]
대문자: A-Z (U+0041 ~ U+005A)
26개

[ASCII lower alpha]
소문자: a-z (U+0061 ~ U+007A)
26개

[ASCII digit]
숫자: 0-9 (U+0030 ~ U+0039)
10개

[ASCII alphanumeric]
= ASCII alpha + ASCII digit
= A-Z + a-z + 0-9
62개

[ASCII upper hex digit]
대문자 16진수: 0-9 (U+0030 ~ U+0039) + A-F (U+0041 ~ U+0046)
16개

[ASCII lower hex digit]
소문자 16진수: 0-9 (U+0030 ~ U+0039) + a-f (U+0061 ~ U+0066)
16개

[ASCII hex digit]
= ASCII upper hex digit + ASCII lower hex digit
= 0-9 + A-F + a-f
22개
```

### 7.2 공백 및 제어 문자 분류

```
[ASCII whitespace]
U+0009 (TAB)
U+000A (LF, Line Feed)
U+000C (FF, Form Feed)
U+000D (CR, Carriage Return)
U+0020 (SPACE)
5개

[ASCII tab or newline]
U+0009 (TAB)
U+000A (LF)
U+000D (CR)
3개

[C0 control]
U+0000 ~ U+001F
32개

[control]
C0 control + U+007F (DEL) ~ U+009F
= U+0000 ~ U+001F + U+007F ~ U+009F
65개
```

### 7.3 특수 코드 포인트 분류

```
[noncharacter]
유니코드에서 문자로 사용하지 않는 코드 포인트:
U+FDD0 ~ U+FDEF (32개)
U+FFFE, U+FFFF
U+1FFFE, U+1FFFF
U+2FFFE, U+2FFFF
... (각 평면의 마지막 두 코드 포인트)
U+10FFFE, U+10FFFF
총 66개

[surrogate]
U+D800 ~ U+DFFF (2048개)
UTF-16 인코딩 전용, 단독으로 유효한 유니코드가 아님

[leading surrogate]
U+D800 ~ U+DBFF (1024개)
서로게이트 쌍의 첫 번째 코드 유닛

[trailing surrogate]
U+DC00 ~ U+DFFF (1024개)
서로게이트 쌍의 두 번째 코드 유닛

[scalar value]
서로게이트가 아닌 코드 포인트
= U+0000 ~ U+D7FF + U+E000 ~ U+10FFFF
```

### 7.4 코드 포인트 분류 종합 표

```
코드 포인트 범위        분류
────────────────────────────────────────
U+0000 ~ U+0008       C0 control, control
U+0009                 C0 control, control, ASCII whitespace, ASCII tab or newline
U+000A                 C0 control, control, ASCII whitespace, ASCII tab or newline
U+000B                 C0 control, control
U+000C                 C0 control, control, ASCII whitespace
U+000D                 C0 control, control, ASCII whitespace, ASCII tab or newline
U+000E ~ U+001F       C0 control, control
U+0020                 ASCII whitespace
U+0021 ~ U+002F       ASCII code point (기호)
U+0030 ~ U+0039       ASCII digit, ASCII alphanumeric, ASCII hex digit
U+003A ~ U+0040       ASCII code point (기호)
U+0041 ~ U+0046       ASCII upper alpha, ASCII upper hex digit, ASCII alpha, ASCII alphanumeric
U+0047 ~ U+005A       ASCII upper alpha, ASCII alpha, ASCII alphanumeric
U+005B ~ U+0060       ASCII code point (기호)
U+0061 ~ U+0066       ASCII lower alpha, ASCII lower hex digit, ASCII alpha, ASCII alphanumeric
U+0067 ~ U+007A       ASCII lower alpha, ASCII alpha, ASCII alphanumeric
U+007B ~ U+007E       ASCII code point (기호)
U+007F                 control (DEL)
U+0080 ~ U+009F       control (C1 control)
U+00A0 ~ U+D7FF       일반 문자, scalar value
U+D800 ~ U+DBFF       leading surrogate, surrogate
U+DC00 ~ U+DFFF       trailing surrogate, surrogate
U+E000 ~ U+FDCF       일반 문자 (Private Use + 일반), scalar value
U+FDD0 ~ U+FDEF       noncharacter
U+FDF0 ~ U+FFFD       일반 문자, scalar value
U+FFFE ~ U+FFFF       noncharacter
U+10000 ~ U+10FFFD    보충 평면 문자, scalar value (noncharacter 제외)
U+10FFFE ~ U+10FFFF   noncharacter
```

### 7.5 JavaScript에서의 코드 포인트 분류

```javascript
// 코드 포인트 분류 유틸리티
const CodePoint = {
    isASCII(cp) {
        return cp >= 0x0000 && cp <= 0x007F;
    },

    isASCIIAlpha(cp) {
        return (cp >= 0x0041 && cp <= 0x005A) ||  // A-Z
               (cp >= 0x0061 && cp <= 0x007A);     // a-z
    },

    isASCIIUpperAlpha(cp) {
        return cp >= 0x0041 && cp <= 0x005A;  // A-Z
    },

    isASCIILowerAlpha(cp) {
        return cp >= 0x0061 && cp <= 0x007A;  // a-z
    },

    isASCIIDigit(cp) {
        return cp >= 0x0030 && cp <= 0x0039;  // 0-9
    },

    isASCIIAlphanumeric(cp) {
        return this.isASCIIAlpha(cp) || this.isASCIIDigit(cp);
    },

    isASCIIHexDigit(cp) {
        return this.isASCIIDigit(cp) ||
               (cp >= 0x0041 && cp <= 0x0046) ||  // A-F
               (cp >= 0x0061 && cp <= 0x0066);     // a-f
    },

    isASCIIWhitespace(cp) {
        return cp === 0x0009 ||  // TAB
               cp === 0x000A ||  // LF
               cp === 0x000C ||  // FF
               cp === 0x000D ||  // CR
               cp === 0x0020;    // SPACE
    },

    isASCIITabOrNewline(cp) {
        return cp === 0x0009 || cp === 0x000A || cp === 0x000D;
    },

    isC0Control(cp) {
        return cp >= 0x0000 && cp <= 0x001F;
    },

    isControl(cp) {
        return this.isC0Control(cp) || (cp >= 0x007F && cp <= 0x009F);
    },

    isSurrogate(cp) {
        return cp >= 0xD800 && cp <= 0xDFFF;
    },

    isLeadingSurrogate(cp) {
        return cp >= 0xD800 && cp <= 0xDBFF;
    },

    isTrailingSurrogate(cp) {
        return cp >= 0xDC00 && cp <= 0xDFFF;
    },

    isScalarValue(cp) {
        return !this.isSurrogate(cp) && cp >= 0 && cp <= 0x10FFFF;
    },

    isNoncharacter(cp) {
        // U+FDD0 ~ U+FDEF
        if (cp >= 0xFDD0 && cp <= 0xFDEF) return true;
        // 각 평면의 마지막 두 코드 포인트
        if ((cp & 0xFFFF) === 0xFFFE || (cp & 0xFFFF) === 0xFFFF) {
            return cp <= 0x10FFFF;
        }
        return false;
    }
};

// 사용 예시
console.log(CodePoint.isASCIIAlpha(0x41));      // true (A)
console.log(CodePoint.isASCIIDigit(0x35));      // true (5)
console.log(CodePoint.isASCIIWhitespace(0x20)); // true (SPACE)
console.log(CodePoint.isSurrogate(0xD800));     // true
console.log(CodePoint.isNoncharacter(0xFFFE));  // true
```

---

## 8. Namespaces

Infra Standard는 웹에서 사용되는 주요 네임스페이스를 정의한다.

### 8.1 네임스페이스 정의

| 이름 | 네임스페이스 URI |
|------|----------------|
| HTML | `http://www.w3.org/1999/xhtml` |
| MathML | `http://www.w3.org/1998/Math/MathML` |
| SVG | `http://www.w3.org/2000/svg` |
| XLink | `http://www.w3.org/1999/xlink` |
| XML | `http://www.w3.org/XML/1998/namespace` |
| XMLNS | `http://www.w3.org/2000/xmlns/` |

### 8.2 네임스페이스 활용

```
[네임스페이스 사용 예시]

HTML 네임스페이스:
  모든 HTML 요소는 HTML 네임스페이스에 속함
  <div>, <span>, <p>, <a> 등

SVG 네임스페이스:
  <svg>, <circle>, <path>, <rect> 등

MathML 네임스페이스:
  <math>, <mi>, <mn>, <mo> 등

스펙에서의 표기:
  "The element is in the HTML namespace"
  "Create an element in the SVG namespace"
```

```html
<!-- HTML 문서에서의 네임스페이스 -->
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>네임스페이스 예시</title>
</head>
<body>
    <!-- HTML 네임스페이스 요소 -->
    <div>HTML 콘텐츠</div>

    <!-- SVG 네임스페이스 요소 -->
    <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
        <circle cx="50" cy="50" r="40" fill="red" />
    </svg>

    <!-- MathML 네임스페이스 요소 -->
    <math xmlns="http://www.w3.org/1998/Math/MathML">
        <mi>x</mi>
        <mo>=</mo>
        <mfrac>
            <mrow><mo>-</mo><mi>b</mi></mrow>
            <mrow><mn>2</mn><mi>a</mi></mrow>
        </mfrac>
    </math>
</body>
</html>
```

```javascript
// JavaScript에서의 네임스페이스 사용
const SVG_NS = 'http://www.w3.org/2000/svg';
const XLINK_NS = 'http://www.w3.org/1999/xlink';
const HTML_NS = 'http://www.w3.org/1999/xhtml';

// SVG 요소 생성 (네임스페이스 필수)
const svg = document.createElementNS(SVG_NS, 'svg');
const circle = document.createElementNS(SVG_NS, 'circle');
circle.setAttribute('cx', '50');
circle.setAttribute('cy', '50');
circle.setAttribute('r', '40');

// XLink 속성 설정
const use = document.createElementNS(SVG_NS, 'use');
use.setAttributeNS(XLINK_NS, 'xlink:href', '#myShape');

// 요소의 네임스페이스 확인
console.log(circle.namespaceURI);  // "http://www.w3.org/2000/svg"
console.log(document.body.namespaceURI);  // "http://www.w3.org/1999/xhtml"
```

---

## 9. Forgiving-base64 인코딩/디코딩

### 9.1 개념

Forgiving-base64는 표준 Base64보다 더 관대한 디코딩 규칙을 사용한다. 입력에 포함된 ASCII 공백을 무시하고, 패딩(=)이 불완전해도 처리한다.

```
[표준 Base64 vs Forgiving-base64]

표준 Base64:
  - 입력에 공백이 있으면 에러
  - 패딩이 정확해야 함
  - 엄격한 알파벳 규칙

Forgiving-base64:
  - ASCII 공백(0x09, 0x0A, 0x0C, 0x0D, 0x20)을 무시
  - 패딩 처리가 더 유연
  - 웹에서의 실제 사용 패턴에 맞춤
```

### 9.2 Forgiving-base64 디코딩 알고리즘

```
[Forgiving-base64 decode 알고리즘]

1. ASCII 공백 제거 (TAB, LF, FF, CR, SPACE)
2. 문자열 길이 % 4 확인
   - 0이면: 정상
   - 1이면: 에러 (잘못된 길이)
   - 2이면: "==" 패딩 추가
   - 3이면: "=" 패딩 추가
3. 마지막 패딩 제거 후 디코딩
4. Base64 알파벳이 아닌 문자가 있으면 에러

Base64 알파벳:
  A-Z (0-25), a-z (26-51), 0-9 (52-61), + (62), / (63)
  패딩: =
```

```javascript
// Forgiving-base64 decode 구현
function forgivingBase64Decode(input) {
    // 1. ASCII 공백 제거
    let data = input.replace(/[\t\n\f\r ]/g, '');

    // 2. 길이 확인 및 패딩 처리
    const remainder = data.length % 4;

    if (remainder === 1) {
        return null; // 실패 (잘못된 길이)
    } else if (remainder === 2) {
        data += '==';
    } else if (remainder === 3) {
        data += '=';
    }

    // 3. Base64 알파벳 검증
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(data)) {
        return null; // 실패 (잘못된 문자)
    }

    // 4. 디코딩
    try {
        return atob(data);
    } catch (e) {
        return null;
    }
}

// 사용 예시
console.log(forgivingBase64Decode('SGVsbG8='));           // "Hello"
console.log(forgivingBase64Decode('SGVs bG8='));          // "Hello" (공백 무시)
console.log(forgivingBase64Decode('SGVs\nbG8='));         // "Hello" (줄바꿈 무시)
console.log(forgivingBase64Decode('SGVsbG8'));             // "Hello" (패딩 없이도 동작)
console.log(forgivingBase64Decode('!@#$'));               // null (잘못된 문자)
```

### 9.3 Forgiving-base64 인코딩

```
[Forgiving-base64 encode]

표준 Base64 인코딩과 동일
공백이나 줄바꿈 없이 인코딩

JavaScript 대응:
  btoa(input)  → Base64 인코딩 문자열
  atob(input)  → 디코딩된 문자열
```

### 9.4 웹에서의 Base64 사용

```javascript
// Data URL에서의 Base64
const dataUrl = 'data:image/png;base64,iVBORw0KGgo...';

// img 태그에서 사용
const img = document.createElement('img');
img.src = dataUrl;

// CSS에서 사용
// .icon { background-image: url(data:image/svg+xml;base64,...); }

// Fetch API에서 Base64 변환
async function blobToBase64(blob) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            const base64 = reader.result.split(',')[1];
            resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
    });
}

// Base64를 Blob으로 변환
function base64ToBlob(base64, mimeType) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return new Blob([bytes], { type: mimeType });
}
```

---

## 10. 실용적 의미

### 10.1 다른 스펙을 읽을 때의 활용

Infra Standard를 이해하면 다른 웹 표준 문서를 정확하게 읽을 수 있다.

```
[HTML Standard에서의 Infra 개념 사용 예시]

"13.2.6.4.7 The "in body" insertion mode

When the user agent is to apply the rules for the "in body" insertion mode,
the user agent must handle the token as follows:

...

An end tag whose tag name is one of: "address", "article", "aside"...

1. If the stack of open elements does not have an element in scope
   that is an HTML element with the same tag name as that of the token,
   then this is a parse error; ignore the token.

2. Otherwise, run these steps:
   a. Generate implied end tags.
   b. If the current node is not an HTML element with the same tag name
      as that of the token, then this is a parse error.
   c. Pop elements from the stack of open elements until an HTML element
      with the same tag name as that of the token has been popped from the stack."

이 텍스트에서 Infra 개념:
- "stack" → Infra의 stack 자료 구조
- "pop" → stack의 pop 연산
- "list" (stack of open elements) → Infra의 list
- "If ... then" → Infra의 조건 분기
- "run these steps" → Infra의 알고리즘 실행
```

### 10.2 Fetch Standard에서의 Infra 개념 사용

```
[Fetch Standard 알고리즘 예시]

"4.1 Main fetch

To main fetch, given a request request, [...]:

1. Let response be null.
2. If request's local-URLs-only flag is set and request's current URL
   is not local, then set response to a network error.
3. ...
4. If response is null, then:
   a. If request's current URL's origin is same origin with
      request's origin, and request's response tainting is "basic",
      then set request's response tainting to "cors".
   b. ...
5. ..."

이 텍스트에서 Infra 개념:
- "Let ... be null" → 변수 선언 (null 초기화)
- "set ... to ..." → 값 할당
- "If ... then" → 조건 분기
- "is not" → 부정 조건
- "request" → struct (여러 필드를 가진 레코드)
```

### 10.3 URL Standard에서의 Infra 개념 사용

```
[URL Standard 알고리즘 예시]

URL 레코드의 구조 (Infra struct):

struct URL {
    scheme (a string)
    username (a string, initially "")
    password (a string, initially "")
    host (null or a host, initially null)
    port (null or a 16-bit unsigned integer, initially null)
    path (a list of strings, initially « »)
    query (null or a string, initially null)
    fragment (null or a string, initially null)
}

이 정의에서 Infra 개념:
- struct → Infra의 struct
- string → Infra의 string
- null → Infra의 null
- list → Infra의 list
- « » → 빈 list
- "initially" → 필드의 초기값
```

---

## 11. 다른 스펙에서의 활용 예시

### 11.1 HTML Standard

```
HTML Standard가 Infra에서 사용하는 개념:

자료 구조:
├── list → 요소 스택, 활성 포맷 요소 리스트 등
├── ordered map → 속성 맵
├── stack → 열린 요소 스택 (stack of open elements)
├── struct → 토큰 (start tag token, end tag token, etc.)
└── queue → 이벤트 루프 태스크 큐

알고리즘:
├── "Let ... be ..." → 파서 상태 변수
├── "If ... then ..." → 토큰 처리 분기
├── "For each ..." → 요소/속성 반복
├── "Assert ..." → 파서 상태 검증
└── "Switch on ..." → 삽입 모드 분기

문자열:
├── ASCII lowercase → 태그 이름 비교
├── ASCII case-insensitive → 속성 이름 비교
├── strip → 값 정규화
└── split → 토큰 분리 (class 속성 등)
```

### 11.2 DOM Standard

```
DOM Standard가 Infra에서 사용하는 개념:

자료 구조:
├── list → NodeList, HTMLCollection의 기반
├── ordered set → DOMTokenList (classList)의 기반
├── ordered map → NamedNodeMap의 기반
└── struct → Node, Element 등의 내부 구조

알고리즘:
├── "Let ... be ..." → DOM 조작 중간 변수
├── "For each ..." → 트리 순회
├── "Throw ..." → DOM 예외 발생
└── "Return ..." → 결과 반환
```

### 11.3 Fetch Standard

```
Fetch Standard가 Infra에서 사용하는 개념:

자료 구조:
├── ordered map → Headers 객체의 기반
├── list → header list
├── struct → Request, Response 내부 구조
└── byte sequence → body 데이터

문자열/바이트:
├── ASCII lowercase → 헤더 이름 정규화
├── isomorphic encode/decode → 바이트와 문자열 변환
├── byte-lowercase → 바이트 레벨 비교
└── ASCII whitespace → 값 파싱 시 공백 처리
```

---

## 12. 참고 자료

- [Infra Standard (WHATWG)](https://infra.spec.whatwg.org/)
- [HTML Standard (WHATWG)](https://html.spec.whatwg.org/)
- [DOM Standard (WHATWG)](https://dom.spec.whatwg.org/)
- [Fetch Standard (WHATWG)](https://fetch.spec.whatwg.org/)
- [URL Standard (WHATWG)](https://url.spec.whatwg.org/)
- [Unicode Standard](https://www.unicode.org/standard/standard.html)
- [RFC 4648 - The Base16, Base32, and Base64 Data Encodings](https://tools.ietf.org/html/rfc4648)
- [MDN Web Docs](https://developer.mozilla.org/)
