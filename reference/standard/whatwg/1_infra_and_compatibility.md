# WHATWG Infra · Compatibility · Quirks Mode

## Infra Standard 상세 가이드

### 목차

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

### 1. 개요

#### Infra Standard란

Infra Standard: WHATWG에서 관리하는 Living Standard → 다른 모든 웹 표준 문서들이 공유하는 공통 기반 정의(common infrastructure) 제공.

- 웹 API를 직접 정의하지 않음
- HTML Standard·DOM Standard·Fetch Standard·URL Standard 등 다른 표준들이 사용하는 기본 개념·자료 구조·알고리즘 표기법을 정의

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

#### 왜 Infra Standard가 필요한가

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

### 2. 원시 타입

#### 2.1 Byte

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

#### 2.2 Byte Sequence

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

#### 2.3 Code Point

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

#### 2.4 String

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

#### 2.5 Boolean

```
[boolean]

정의: true 또는 false

표기:
  true, false (소문자)

JavaScript 대응: boolean (true / false)
```

#### 2.6 Null

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

### 3. 자료 구조

#### 3.1 List

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

#### 3.2 Ordered Set

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

#### 3.3 Stack과 Queue

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

#### 3.4 Map (Ordered Map)

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

#### 3.5 Struct

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

#### 3.6 Tuple

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

### 4. 알고리즘 표기 규약

#### 4.1 기본 구문

Infra Standard: 다른 스펙에서 알고리즘을 기술할 때 사용하는 표준적인 구문을 정의.

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

#### 4.2 Let (변수 선언)

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

#### 4.3 Set (값 할당)

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

#### 4.4 Return (반환)

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

#### 4.5 Assert (검증)

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

#### 4.6 Throw (예외)

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

#### 4.7 For each (반복)

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

#### 4.8 While (조건부 반복)

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

#### 4.9 If/Else (조건 분기)

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

#### 4.10 Switch (다중 분기)

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

#### 4.11 Continue와 Break

```
스펙 표기:
  "Continue"    → 현재 반복의 나머지를 건너뛰고 다음 반복으로
  "Break"       → 현재 반복을 중단

JavaScript 대응:
  continue;
  break;
```

#### 4.12 알고리즘 예시

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

### 5. 문자열 처리

#### 5.1 ASCII Lowercase / Uppercase

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

#### 5.2 Strip

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

#### 5.3 Collapse

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

#### 5.4 코드 포인트 비교

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

#### 5.5 Starts with / Ends with

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

#### 5.6 Split

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

#### 5.7 Concatenate

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

#### 5.8 Scalar Value와 Surrogate

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

### 6. 바이트 시퀀스 처리

#### 6.1 바이트 시퀀스 비교

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

#### 6.2 바이트 시퀀스 Lowercase

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

#### 6.3 Isomorphic Encode / Decode

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

### 7. 코드 포인트 분류

Infra Standard: 코드 포인트를 여러 그룹으로 분류.

#### 7.1 ASCII 관련 분류

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

#### 7.2 공백 및 제어 문자 분류

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

#### 7.3 특수 코드 포인트 분류

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

#### 7.4 코드 포인트 분류 종합 표

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

#### 7.5 JavaScript에서의 코드 포인트 분류

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

### 8. Namespaces

Infra Standard: 웹에서 사용되는 주요 네임스페이스를 정의.

#### 8.1 네임스페이스 정의

- HTML: `http://www.w3.org/1999/xhtml`
- MathML: `http://www.w3.org/1998/Math/MathML`
- SVG: `http://www.w3.org/2000/svg`
- XLink: `http://www.w3.org/1999/xlink`
- XML: `http://www.w3.org/XML/1998/namespace`
- XMLNS: `http://www.w3.org/2000/xmlns/`

#### 8.2 네임스페이스 활용

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

### 9. Forgiving-base64 인코딩/디코딩

#### 9.1 개념

Forgiving-base64: 표준 Base64보다 더 관대한 디코딩 규칙 사용.

- 입력에 포함된 ASCII 공백 무시
- 패딩(=)이 불완전해도 처리

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

#### 9.2 Forgiving-base64 디코딩 알고리즘

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

#### 9.3 Forgiving-base64 인코딩

```
[Forgiving-base64 encode]

표준 Base64 인코딩과 동일
공백이나 줄바꿈 없이 인코딩

JavaScript 대응:
  btoa(input)  → Base64 인코딩 문자열
  atob(input)  → 디코딩된 문자열
```

#### 9.4 웹에서의 Base64 사용

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

### 10. 실용적 의미

#### 10.1 다른 스펙을 읽을 때의 활용

Infra Standard 이해 → 다른 웹 표준 문서를 정확하게 읽을 수 있음.

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

#### 10.2 Fetch Standard에서의 Infra 개념 사용

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

#### 10.3 URL Standard에서의 Infra 개념 사용

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

### 11. 다른 스펙에서의 활용 예시

#### 11.1 HTML Standard

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

#### 11.2 DOM Standard

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

#### 11.3 Fetch Standard

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

### 12. 참고 자료

- [Infra Standard (WHATWG)](https://infra.spec.whatwg.org/)
- [HTML Standard (WHATWG)](https://html.spec.whatwg.org/)
- [DOM Standard (WHATWG)](https://dom.spec.whatwg.org/)
- [Fetch Standard (WHATWG)](https://fetch.spec.whatwg.org/)
- [URL Standard (WHATWG)](https://url.spec.whatwg.org/)
- [Unicode Standard](https://www.unicode.org/standard/standard.html)
- [RFC 4648 - The Base16, Base32, and Base64 Data Encodings](https://tools.ietf.org/html/rfc4648)
- [MDN Web Docs](https://developer.mozilla.org/)

---

## Compatibility Standard 상세 가이드

### 목차

1. [개요](#1-개요)
2. [왜 호환성 표준이 필요한가](#2-왜-호환성-표준이-필요한가)
3. [CSS 호환성](#3-css-호환성)
4. [DOM/JS 호환성](#4-domjs-호환성)
5. [터치 이벤트 호환성](#5-터치-이벤트-호환성)
6. [렌더링 호환성](#6-렌더링-호환성)
7. [Vendor Prefix 표준화 과정](#7-vendor-prefix-표준화-과정)
8. [브라우저별 호환성 현황](#8-브라우저별-호환성-현황)
9. [실용 예제](#9-실용-예제)
10. [개발자를 위한 권장사항](#10-개발자를-위한-권장사항)

---

### 1. 개요

Compatibility Standard: WHATWG에서 관리하는 Living Standard 중 하나.

- 웹 브라우저 간의 호환성을 보장하기 위해 비표준(non-standard) 기능들을 공식적으로 문서화한 표준

#### 핵심 개념

특정 브라우저(특히 WebKit/Blink 기반 브라우저)에서만 지원하던 비표준 기능들이 널리 사용됨 → 다른 브라우저들도 이를 지원해야 하는 상황 발생.

- Compatibility Standard: 이러한 기능들을 정리하고, 모든 브라우저가 일관되게 구현할 수 있도록 명세 제공

```
[역사적 비표준 기능] → [광범위한 웹 사용] → [호환성 문제 발생] → [Compatibility Standard로 표준화]
```

#### 표준의 범위

- CSS 호환성: `-webkit-` 접두사 속성의 표준화
- DOM/JS 호환성: `window.orientation` 등 비표준 API
- 터치 이벤트: WebKit 기반 터치 이벤트
- 렌더링 호환성: 특정 렌더링 동작의 표준화

---

### 2. 왜 호환성 표준이 필요한가

#### 레거시 웹과의 호환

웹의 발전 과정에서 브라우저 제조사들이 경쟁적으로 새로운 기능 도입 → 특히 모바일 웹 초기에 WebKit(Safari, 초기 Chrome) 엔진이 시장 지배 → 많은 웹사이트가 WebKit 전용 기능에 의존하게 됨.

```
[2007-2013 모바일 웹 초기]
├── Safari (WebKit) - iOS 독점
├── Chrome (WebKit → Blink) - Android 주력
├── Opera (Presto → Blink) - 엔진 변경
└── Firefox (Gecko) - 호환성 문제 직면
```

#### 문제의 발생

```javascript
// 많은 모바일 웹사이트가 이런 코드를 사용
if ('onorientationchange' in window) {
    // 모바일 디바이스로 판단
    window.addEventListener('orientationchange', handleOrientation);
}

// -webkit- 접두사만 사용
.element {
    -webkit-transform: rotate(45deg);
    /* 표준 속성 없음! */
}
```

이러한 코드가 WebKit/Blink 이외의 브라우저에서 동작하지 않음 → 사용자 경험에 심각한 문제 발생.

#### 해결 방안으로서의 Compatibility Standard

Mozilla(Firefox)와 다른 브라우저 제조사들이 직면한 두 가지 선택지:

1. 비표준 기능을 무시 - 많은 웹사이트가 깨짐
2. 비표준 기능을 구현 - 표준 없이 각자 구현하면 또 다른 호환성 문제 발생

결국 WHATWG에서 이러한 비표준 기능들을 공식 표준으로 문서화 → 모든 브라우저가 동일하게 구현할 수 있도록 하는 것이 최선의 해결책.

---

### 3. CSS 호환성

#### 3.1 -webkit- 접두사 속성 개요

Compatibility Standard에서 가장 큰 비중을 차지하는 것: CSS `-webkit-` 접두사 속성의 표준화.

##### Vendor Prefix 시스템

- `-webkit-`: WebKit, Blink (Safari, Chrome, Edge)
- `-moz-`: Gecko (Firefox)
- `-ms-`: Trident (IE), EdgeHTML (구 Edge)
- `-o-`: Presto (구 Opera)

원래 vendor prefix: 실험적 기능을 안전하게 노출하기 위한 메커니즘 → 웹 개발자들이 `-webkit-` 접두사 속성을 프로덕션 코드에 광범위하게 사용 → 사실상 표준처럼 되어버림.

#### 3.2 -webkit-transform 관련

```css
/* Compatibility Standard에 의해 모든 브라우저가 지원해야 하는 속성 */
.element {
    -webkit-transform: rotate(45deg);        /* → transform으로 매핑 */
    -webkit-transform-origin: center center;  /* → transform-origin으로 매핑 */
    -webkit-transform-style: preserve-3d;     /* → transform-style로 매핑 */
    -webkit-perspective: 1000px;              /* → perspective로 매핑 */
    -webkit-perspective-origin: 50% 50%;      /* → perspective-origin으로 매핑 */
    -webkit-backface-visibility: hidden;      /* → backface-visibility로 매핑 */
}
```

##### 매핑 메커니즘

```
파싱 단계:
"-webkit-transform: rotate(45deg)"
    ↓ [Compatibility Standard 매핑 규칙]
"transform: rotate(45deg)" 으로 처리
```

#### 3.3 -webkit-transition 관련

```css
.element {
    -webkit-transition: all 0.3s ease;
    -webkit-transition-property: opacity;
    -webkit-transition-duration: 0.3s;
    -webkit-transition-timing-function: ease-in-out;
    -webkit-transition-delay: 0.1s;
}

/* 위의 속성들은 각각 표준 속성으로 매핑됨 */
/*
    -webkit-transition           → transition
    -webkit-transition-property  → transition-property
    -webkit-transition-duration  → transition-duration
    -webkit-transition-timing-function → transition-timing-function
    -webkit-transition-delay     → transition-delay
*/
```

#### 3.4 -webkit-animation 관련

```css
@-webkit-keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

.element {
    -webkit-animation: fadeIn 1s ease-in;
    -webkit-animation-name: fadeIn;
    -webkit-animation-duration: 1s;
    -webkit-animation-timing-function: ease-in;
    -webkit-animation-delay: 0s;
    -webkit-animation-iteration-count: infinite;
    -webkit-animation-direction: alternate;
    -webkit-animation-fill-mode: both;
    -webkit-animation-play-state: running;
}

/* 모두 표준 animation 속성으로 매핑 */
```

#### 3.5 -webkit-appearance

`-webkit-appearance`: 폼 요소의 네이티브 스타일링을 제어하는 속성.

- 이 속성과 표준 `appearance`의 매핑은 현재 Compatibility Standard가 아니라 CSS Basic User Interface Module(CSS-UI)에서 정의
- Compatibility Standard는 초기에 이 매핑을 다뤘지만 이후 해당 CSS 모듈로 이관됨

```css
/* 네이티브 스타일 제거 */
input, button, select, textarea {
    -webkit-appearance: none;
    appearance: none;  /* 표준 속성 */
}

/* 지원되는 값 */
.element {
    -webkit-appearance: none;
    -webkit-appearance: auto;
    -webkit-appearance: button;
    -webkit-appearance: textfield;
    -webkit-appearance: checkbox;
    -webkit-appearance: radio;
    -webkit-appearance: menulist;
    -webkit-appearance: listbox;
    -webkit-appearance: meter;
    -webkit-appearance: progress-bar;
    -webkit-appearance: searchfield;
    -webkit-appearance: textarea;
}
```

#### 3.6 -webkit-filter

```css
.element {
    -webkit-filter: blur(5px);
    -webkit-filter: brightness(0.5);
    -webkit-filter: contrast(200%);
    -webkit-filter: drop-shadow(16px 16px 20px blue);
    -webkit-filter: grayscale(50%);
    -webkit-filter: hue-rotate(90deg);
    -webkit-filter: invert(75%);
    -webkit-filter: opacity(25%);
    -webkit-filter: saturate(30%);
    -webkit-filter: sepia(60%);

    /* 여러 필터 조합 */
    -webkit-filter: contrast(175%) brightness(3%);
}

/* → filter 표준 속성으로 매핑 */
```

#### 3.7 Flexbox 관련 -webkit- 속성

Flexbox: 구 사양(2009)과 신 사양(2012+)이 있음 → 호환성이 특히 복잡.

```css
/* 구 Flexbox (-webkit- 접두사, 2009 사양) */
.container {
    display: -webkit-box;           /* → display: flex */
    display: -webkit-inline-box;    /* → display: inline-flex */
    -webkit-box-orient: horizontal; /* → flex-direction */
    -webkit-box-direction: normal;  /* → flex-direction */
    -webkit-box-pack: center;       /* → justify-content: center */
    -webkit-box-align: center;      /* → align-items: center */
    -webkit-box-ordinal-group: 1;   /* → order: 0 */
    -webkit-box-flex: 1;            /* → flex: 1 */
    -webkit-box-lines: multiple;    /* → flex-wrap: wrap */
}

/* Compatibility Standard에 의한 매핑 규칙 */
/*
    display: -webkit-box       → display: flex
    display: -webkit-inline-box → display: inline-flex
    -webkit-box-orient + -webkit-box-direction → flex-direction
        horizontal + normal = row
        horizontal + reverse = row-reverse
        vertical + normal = column
        vertical + reverse = column-reverse
    -webkit-box-pack:
        start   → justify-content: flex-start
        center  → justify-content: center
        end     → justify-content: flex-end
        justify → justify-content: space-between
    -webkit-box-align:
        start    → align-items: flex-start
        center   → align-items: center
        end      → align-items: flex-end
        baseline → align-items: baseline
        stretch  → align-items: stretch
*/
```

#### 3.8 -webkit-text-size-adjust

모바일 브라우저에서 텍스트 크기 자동 조절을 제어.

```css
/* 텍스트 자동 크기 조절 비활성화 */
body {
    -webkit-text-size-adjust: 100%;
    -webkit-text-size-adjust: none;   /* 자동 조절 완전 비활성화 */
    -webkit-text-size-adjust: auto;   /* 브라우저 기본 동작 */
}
```

#### 3.9 기타 -webkit- CSS 속성

```css
/* -webkit-background-clip: text */
.gradient-text {
    background: linear-gradient(to right, #ff0000, #0000ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    /* → background-clip: text 으로 매핑 */
}

/* -webkit-text-fill-color */
.element {
    -webkit-text-fill-color: red;
    /* 텍스트의 채우기(fill) 색상 설정 */
}

/* -webkit-text-stroke */
.element {
    -webkit-text-stroke: 2px black;
    -webkit-text-stroke-width: 2px;
    -webkit-text-stroke-color: black;
}

/* -webkit-opacity → opacity */
.element {
    -webkit-opacity: 0.5;  /* → opacity: 0.5 */
}

/* -webkit-align-items, -webkit-align-content 등 */
.container {
    -webkit-align-items: center;        /* → align-items */
    -webkit-align-content: center;      /* → align-content */
    -webkit-align-self: center;         /* → align-self */
    -webkit-justify-content: center;    /* → justify-content */
    -webkit-flex: 1;                    /* → flex */
    -webkit-flex-basis: auto;           /* → flex-basis */
    -webkit-flex-direction: row;        /* → flex-direction */
    -webkit-flex-flow: row wrap;        /* → flex-flow */
    -webkit-flex-grow: 1;              /* → flex-grow */
    -webkit-flex-shrink: 0;            /* → flex-shrink */
    -webkit-flex-wrap: wrap;           /* → flex-wrap */
    -webkit-order: 1;                  /* → order */
}
```

#### 3.10 -webkit- 접두사 속성 전체 매핑 표

- `-webkit-transform` → `transform`
- `-webkit-transform-origin` → `transform-origin`
- `-webkit-transform-style` → `transform-style`
- `-webkit-perspective` → `perspective`
- `-webkit-perspective-origin` → `perspective-origin`
- `-webkit-backface-visibility` → `backface-visibility`
- `-webkit-transition` → `transition`
- `-webkit-animation` → `animation`
- `-webkit-filter` → `filter`
- `-webkit-appearance` → `appearance`
  - CSS-UI 모듈에서 정의 (Compatibility Standard 범위 아님)
- `-webkit-background-clip` → `background-clip`
  - `text` 값 포함
- `-webkit-opacity` → `opacity`
- `-webkit-order` → `order`
- `-webkit-flex` → `flex`
- `display: -webkit-box` → `display: flex`
  - 구 Flexbox
- `display: -webkit-flex` → `display: flex`
  - 신 Flexbox
- `-webkit-box-orient` → `flex-direction` (부분)
  - 복합 매핑
- `-webkit-box-pack` → `justify-content`
  - 값 매핑 필요
- `-webkit-box-align` → `align-items`
  - 값 매핑 필요

---

### 4. DOM/JS 호환성

#### 4.1 window.orientation

`window.orientation` 속성: Apple이 iOS Safari에서 처음 도입한 비표준 API → 디바이스의 현재 화면 방향을 각도로 반환.

```javascript
// window.orientation 값
// 0   : 세로 모드 (portrait, 기본)
// 90  : 가로 모드 (landscape, 왼쪽으로 회전)
// -90 : 가로 모드 (landscape, 오른쪽으로 회전)
// 180 : 거꾸로 세로 모드 (portrait, 뒤집힘)

console.log(window.orientation);
// 출력: 0, 90, -90, 또는 180
```

##### 방향 다이어그램

```
window.orientation 값:

    0° (Portrait)          90° (Landscape Left)
    ┌──────────┐           ┌──────────────────┐
    │          │           │                  │
    │          │           │                  │
    │  SCREEN  │           │     SCREEN       │
    │          │           │                  │
    │          │           │                  │
    │          │           └──────────────────┘
    │   [  ]   │
    └──────────┘

   -90° (Landscape Right)  180° (Portrait Upside Down)
    ┌──────────────────┐   ┌──────────┐
    │                  │   │   [  ]   │
    │                  │   │          │
    │     SCREEN       │   │          │
    │                  │   │  SCREEN  │
    │                  │   │          │
    └──────────────────┘   │          │
                           └──────────┘
```

#### 4.2 orientationchange 이벤트

```javascript
// orientationchange 이벤트 리스닝
window.addEventListener('orientationchange', function(event) {
    console.log('화면 방향 변경:', window.orientation);

    switch (window.orientation) {
        case 0:
            console.log('세로 모드');
            break;
        case 90:
            console.log('가로 모드 (왼쪽 회전)');
            break;
        case -90:
            console.log('가로 모드 (오른쪽 회전)');
            break;
        case 180:
            console.log('거꾸로 세로 모드');
            break;
    }
});

// 인라인 이벤트 핸들러
window.onorientationchange = function() {
    // ...
};
```

#### 4.3 Screen Orientation API와의 관계

`window.orientation`: 비표준 · 표준 대안은 Screen Orientation API.

```javascript
// [비표준] window.orientation
console.log(window.orientation); // 0, 90, -90, 180

// [표준] Screen Orientation API
console.log(screen.orientation.type);
// "portrait-primary", "portrait-secondary",
// "landscape-primary", "landscape-secondary"

console.log(screen.orientation.angle);
// 0, 90, 180, 270

// 표준 이벤트
screen.orientation.addEventListener('change', function() {
    console.log('방향:', screen.orientation.type);
    console.log('각도:', screen.orientation.angle);
});
```

##### 비교 표

- 반환값
  - `window.orientation`(비표준): 0, 90, -90, 180
  - `screen.orientation`(표준): type 문자열 + angle 숫자
- 이벤트
  - `window.orientation`(비표준): `orientationchange`
  - `screen.orientation`(표준): `change`
- 이벤트 대상
  - `window.orientation`(비표준): `window`
  - `screen.orientation`(표준): `screen.orientation`
- 잠금 기능
  - `window.orientation`(비표준): 없음
  - `screen.orientation`(표준): `lock()`, `unlock()`
- Compatibility Standard 포함 여부
  - `window.orientation`(비표준): 포함
  - `screen.orientation`(표준): W3C Screen Orientation API

#### 4.4 webkit 접두사 DOM API (참고: 현재 스펙 범위 밖)

`webkitRequestAnimationFrame`·`webkitCancelAnimationFrame`·`webkitURL`·`video.webkitEnterFullscreen()` 등: 과거 여러 브라우저에서 흔히 쓰이던 webkit 접두사 API.

- 현재 Compatibility Standard 문서에는 이들을 표준화한다는 내용이 없음
- 아래 코드는 실제로 브라우저에 남아 있는 레거시 API를 소개하는 것일 뿐 → Compatibility Standard가 규정한 내용이 아니므로 참고용으로만 확인

```javascript
// webkitRequestAnimationFrame (레거시, 표준 아님)
window.webkitRequestAnimationFrame(callback);
// 표준: window.requestAnimationFrame(callback)

// webkitCancelAnimationFrame (레거시, 표준 아님)
window.webkitCancelAnimationFrame(id);
// 표준: window.cancelAnimationFrame(id)

// webkitURL (레거시, 표준 아님)
window.webkitURL;
// 표준: window.URL

// HTMLVideoElement의 webkit 접두사 메서드 (레거시, 표준 아님)
video.webkitEnterFullscreen();
video.webkitExitFullscreen();
video.webkitDisplayingFullscreen;   // 읽기 전용 속성
video.webkitSupportsFullscreen;     // 읽기 전용 속성
```

---

### 5. 터치 이벤트 호환성 (참고: 현재 스펙 범위 밖)

#### 5.1 터치 이벤트 모델

터치 이벤트: WebKit에서 처음 구현되어 이후 W3C Touch Events 사양으로 표준화됨.

- 현재 Compatibility Standard 문서에는 터치 이벤트를 다루는 내용이 없음
- 아래 내용은 실무에서 자주 마주치는 터치 이벤트 관련 호환성 이슈를 정리한 참고 자료

```javascript
// 기본 터치 이벤트
element.addEventListener('touchstart', function(e) {
    const touch = e.touches[0];
    console.log('터치 시작:', touch.clientX, touch.clientY);
});

element.addEventListener('touchmove', function(e) {
    const touch = e.touches[0];
    console.log('터치 이동:', touch.clientX, touch.clientY);
});

element.addEventListener('touchend', function(e) {
    console.log('터치 종료');
});

element.addEventListener('touchcancel', function(e) {
    console.log('터치 취소');
});
```

#### 5.2 마우스 이벤트와의 호환성

터치 디바이스: 마우스 이벤트와의 호환성 순서가 중요.

```
터치 이벤트 시퀀스:

[사용자가 화면 터치]
    ↓
touchstart
    ↓
touchmove (0회 이상)
    ↓
touchend
    ↓
[약 300ms 지연] ← 더블 탭 감지를 위해
    ↓
mousemove
    ↓
mousedown
    ↓
mouseup
    ↓
click
```

#### 5.3 ontouchstart 속성

```javascript
// ontouchstart 등의 이벤트 핸들러 속성이 존재하는지 확인하여
// 터치 지원을 감지하는 코드가 많음

// 흔한 (하지만 부정확한) 터치 감지 방법
if ('ontouchstart' in window) {
    // 터치 디바이스로 판단
}

// 데스크톱 브라우저에서도 ontouchstart가 존재할 수 있으므로
// → 터치 감지에 이 방법만 사용하면 안 됨

// 더 나은 방법
if (window.matchMedia('(pointer: coarse)').matches) {
    // 대략적인 포인팅 디바이스 (터치)
}

if (window.matchMedia('(hover: none)').matches) {
    // 호버를 지원하지 않는 디바이스
}
```

#### 5.4 터치 이벤트와 관련된 호환성 문제

```javascript
// preventDefault()의 호환성 동작
element.addEventListener('touchstart', function(e) {
    // passive: false가 아니면 preventDefault()가 무시될 수 있음
    e.preventDefault();
}, { passive: false });

// Chrome은 성능을 위해 touchstart를 passive로 기본 설정

// Touch 인터페이스의 호환성 속성
element.addEventListener('touchstart', function(e) {
    const touch = e.touches[0];

    // 표준 속성
    touch.identifier;
    touch.target;
    touch.clientX;
    touch.clientY;
    touch.screenX;
    touch.screenY;
    touch.pageX;
    touch.pageY;

    // WebKit 비표준 속성 (Compatibility Standard 범위 밖)
    touch.radiusX;     // 터치 영역 반경 X
    touch.radiusY;     // 터치 영역 반경 Y
    touch.rotationAngle; // 터치 영역 회전각
    touch.force;       // 터치 압력 (0.0 ~ 1.0)
});
```

---

### 6. 렌더링 호환성

#### 6.1 -webkit- 접두사의 렌더링 영향

Compatibility Standard: 특정 렌더링 동작의 호환성도 정의.

```css
/* -webkit-text-size-adjust의 렌더링 영향 */
/* 모바일에서 가로 모드 전환 시 텍스트 크기 자동 조절 */
html {
    -webkit-text-size-adjust: 100%;
    text-size-adjust: 100%;
}

/* 이 속성이 없으면 일부 브라우저에서
   가로 모드 전환 시 텍스트가 확대됨 */
```

#### 6.2 -webkit-line-clamp (참고: 현재 스펙 범위 밖)

여러 줄 텍스트 말줄임 처리를 위한 비표준 속성 → 매우 널리 사용되어 옴.

- Compatibility Standard가 아니라 CSS Overflow Module 영역 → 현재 스펙 문서에는 이 속성을 다루는 내용이 없음
- 최근에는 대부분의 주요 브라우저가 접두사 없는 표준 `line-clamp` 속성도 지원하기 시작

```css
/* 3줄 말줄임 (레거시 방식) */
.multiline-ellipsis {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* 표준 line-clamp (최신 브라우저) */
.multiline-ellipsis-standard {
    line-clamp: 3;
    overflow: hidden;
}
```

#### 6.3 -webkit-tap-highlight-color (참고: 현재 스펙 범위 밖)

모바일에서 터치 시 나타나는 하이라이트 색상을 제어.

```css
/* 터치 하이라이트 제거 */
* {
    -webkit-tap-highlight-color: transparent;
    -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
}

/* 커스텀 하이라이트 색상 */
a, button {
    -webkit-tap-highlight-color: rgba(0, 100, 255, 0.3);
}
```

#### 6.4 -webkit-overflow-scrolling (참고: 현재 스펙 범위 밖)

iOS에서 관성(momentum) 스크롤을 활성화하는 속성.

```css
.scrollable {
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;  /* 관성 스크롤 활성화 */
}

/* 값:
   auto  - 일반 스크롤 (관성 없음)
   touch - 관성 스크롤 (네이티브 스크롤처럼 동작)
*/
```

#### 6.5 -webkit-user-select (참고: 현재 스펙 범위 밖)

텍스트 선택 가능 여부를 제어.

```css
/* 텍스트 선택 제어 */
.no-select {
    -webkit-user-select: none;
    user-select: none;
}

.all-select {
    -webkit-user-select: all;
    user-select: all;
}

/* 값:
   none - 선택 불가
   text - 텍스트만 선택 가능 (기본)
   all  - 클릭 시 전체 요소 선택
   auto - 기본 동작
   contain - 선택 범위를 요소 내로 제한
*/
```

---

### 7. Vendor Prefix 표준화 과정

#### 7.1 역사적 배경

```
[Timeline: Vendor Prefix의 역사]

2005-2009: Vendor prefix 도입기
├── CSS3 모듈 초안 단계
├── 브라우저들이 실험적 기능에 prefix 사용
└── -webkit-, -moz-, -ms-, -o- 도입

2009-2013: 문제 심화기
├── 모바일 웹 폭발적 성장
├── 개발자들이 -webkit-만 사용하는 관행 확산
├── Firefox, Opera 등에서 웹사이트 깨짐 발생
└── Mozilla가 -webkit- prefix 지원 검토 시작

2013-2016: 해결 모색기
├── Compatibility Standard 초안 작성
├── Firefox가 일부 -webkit- 속성 지원 시작
├── Edge(EdgeHTML)도 -webkit- 속성 지원 추가
└── Vendor prefix 사용 자제 권고 시작

2016-현재: 표준화 안정기
├── Compatibility Standard Living Standard 발행
├── 대부분의 -webkit- 속성이 표준 속성으로 대체
├── prefix 없는 표준 속성 사용이 일반화
└── 레거시 호환성을 위해 prefix 지원은 유지
```

#### 7.2 Vendor Prefix 폐기 방향

현대 웹 개발에서 vendor prefix의 필요성은 크게 감소.

```css
/* [권장하지 않음] 과거 스타일 */
.element {
    -webkit-border-radius: 10px;
    -moz-border-radius: 10px;
    border-radius: 10px;
}

/* [권장] 현대 스타일 */
.element {
    border-radius: 10px;
}
```

#### 7.3 여전히 필요한 Vendor Prefix

일부 속성은 아직 vendor prefix가 필요.

```css
/* 2024년 기준 여전히 -webkit- 필요한 경우 */

/* -webkit-line-clamp (표준 line-clamp를 지원하지 않는 구형 브라우저 대응) */
.truncate {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* -webkit-text-fill-color (일부 상황) */
.custom-text {
    -webkit-text-fill-color: transparent;
    background-clip: text;
    -webkit-background-clip: text;
}

/* -webkit-tap-highlight-color (모바일) */
button {
    -webkit-tap-highlight-color: transparent;
}
```

---

### 8. 브라우저별 호환성 현황

#### 8.1 Compatibility Standard 지원 현황

Compatibility Standard(https://compat.spec.whatwg.org/)가 실제로 규정하는 항목: `-webkit-text-fill-color`·`-webkit-text-stroke`(및 `-width`/`-color`)·`window.orientation`/`orientationchange` 정도로 많지 않음.

- 나머지 `-webkit-` 속성들: 앞서 각 절에서 밝혔듯 CSS-UI, CSS Overflow Module 등 다른 스펙이 다루거나 아예 표준화되지 않은 채 관행적으로 구현된 것

브라우저별 지원 현황(Chrome·Firefox·Safari·Edge 기준):

- `-webkit-text-fill-color`: 전 브라우저 지원 · 근거 스펙 Compatibility Standard
- `-webkit-text-stroke`: 전 브라우저 지원 · 근거 스펙 Compatibility Standard
- `window.orientation`: 전 브라우저 지원 · 근거 스펙 Compatibility Standard
- `orientationchange`: 전 브라우저 지원 · 근거 스펙 Compatibility Standard
- `-webkit-appearance`: 전 브라우저 지원 · 근거 스펙 CSS Basic User Interface (CSS-UI)
- `-webkit-line-clamp`: 전 브라우저 지원 · 근거 스펙 CSS Overflow Module (범위 밖)
- `-webkit-transform`/`-transition`/`-animation`/`-filter`/`-flex` 등: 전 브라우저 지원 · 근거 스펙 각 CSS 모듈에 흡수, Compatibility Standard 범위 밖
- `display: -webkit-box`: 전 브라우저 지원 · 근거 스펙 표준화되지 않음 (레거시 Flexbox, 범위 밖)
- `-webkit-text-size-adjust`: Chrome·Safari·Edge 지원, Firefox 46+ · 근거 스펙 범위 밖
- `-webkit-user-select`: 전 브라우저 지원 · 근거 스펙 범위 밖

#### 8.2 Firefox의 -webkit- 접두사 지원 이력

```
Firefox의 -webkit- 지원 추가 이력:

Firefox 46 (2016.04):
  - -webkit-filter
  - -webkit-text-size-adjust

Firefox 47 (2016.06):
  - -webkit-mask 관련 속성

Firefox 49 (2016.09):
  - display: -webkit-box, -webkit-inline-box
  - -webkit-box-* 속성들
  - -webkit-line-clamp

Firefox 63 (2018.10):
  - -webkit-appearance

Firefox 64+ (2018.12):
  - 대부분의 -webkit- Flexbox 속성
```

---

### 9. 실용 예제

#### 9.1 호환성을 고려한 CSS 작성

```css
/* 레거시 브라우저 호환성을 고려한 그라데이션 텍스트 */
.gradient-text {
    /* 폴백: 단색 텍스트 */
    color: #667eea;

    /* -webkit- 접두사 (Compatibility Standard) */
    background: -webkit-linear-gradient(left, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;

    /* 표준 */
    background: linear-gradient(to right, #667eea, #764ba2);
    background-clip: text;
    color: transparent;
}

/* 호환성을 고려한 다중 줄 말줄임 */
.text-clamp {
    /* 폴백: 단일 줄 말줄임 */
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    /* -webkit-line-clamp 지원 브라우저 */
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    white-space: normal;
}
```

#### 9.2 호환성을 고려한 JavaScript 작성

```javascript
// 화면 방향 감지 (호환성 고려)
function getOrientation() {
    // 표준 API 우선 시도
    if (screen.orientation) {
        return {
            type: screen.orientation.type,
            angle: screen.orientation.angle
        };
    }

    // Compatibility Standard: window.orientation 폴백
    if (typeof window.orientation !== 'undefined') {
        const angle = window.orientation;
        let type;
        switch (angle) {
            case 0: type = 'portrait-primary'; break;
            case 90: type = 'landscape-primary'; break;
            case -90: type = 'landscape-secondary'; break;
            case 180: type = 'portrait-secondary'; break;
            default: type = 'portrait-primary';
        }
        return { type, angle };
    }

    // 최종 폴백: 뷰포트 크기 기반 추정
    const isPortrait = window.innerHeight > window.innerWidth;
    return {
        type: isPortrait ? 'portrait-primary' : 'landscape-primary',
        angle: isPortrait ? 0 : 90
    };
}

// 방향 변경 이벤트 리스너 (호환성 고려)
function onOrientationChange(callback) {
    if (screen.orientation) {
        screen.orientation.addEventListener('change', function() {
            callback(getOrientation());
        });
    } else if ('onorientationchange' in window) {
        window.addEventListener('orientationchange', function() {
            // 약간의 지연 후 콜백 호출 (값 안정화)
            setTimeout(function() {
                callback(getOrientation());
            }, 100);
        });
    } else {
        // resize 이벤트로 폴백
        window.addEventListener('resize', function() {
            callback(getOrientation());
        });
    }
}

// 사용
onOrientationChange(function(orientation) {
    console.log('방향 변경:', orientation.type, orientation.angle);
});
```

#### 9.3 PostCSS를 활용한 자동 접두사 관리

```javascript
// postcss.config.js
module.exports = {
    plugins: [
        require('autoprefixer')({
            // Autoprefixer가 Compatibility Standard에 맞는
            // -webkit- 접두사를 자동으로 추가
            overrideBrowserslist: [
                'last 2 versions',
                '> 1%',
                'iOS >= 10',
                'Android >= 5'
            ]
        })
    ]
};

// 입력 CSS:
// .element { transform: rotate(45deg); }

// 출력 CSS (Autoprefixer 처리 후):
// .element {
//     -webkit-transform: rotate(45deg);
//     transform: rotate(45deg);
// }
```

#### 9.4 기능 감지 패턴

```javascript
// CSS 기능 감지 (Compatibility Standard 관련)
function supportsCSS(property, value) {
    // @supports 사용
    if (window.CSS && CSS.supports) {
        return CSS.supports(property, value);
    }

    // 폴백: DOM 요소 테스트
    const el = document.createElement('div');
    el.style[property] = value;
    return el.style[property] === value;
}

// -webkit-line-clamp 지원 확인
const supportsLineClamp = supportsCSS('-webkit-line-clamp', '3');

// appearance 지원 확인
const supportsAppearance =
    supportsCSS('appearance', 'none') ||
    supportsCSS('-webkit-appearance', 'none');

console.log('line-clamp 지원:', supportsLineClamp);
console.log('appearance 지원:', supportsAppearance);
```

---

### 10. 개발자를 위한 권장사항

#### 10.1 현대 웹 개발에서의 접근 방식

```
우선순위:

1. 표준 속성 사용
   transform: rotate(45deg);              ← 우선 사용

2. 필요 시 -webkit- 접두사 병행
   -webkit-transform: rotate(45deg);       ← 레거시 지원 필요 시
   transform: rotate(45deg);

3. Autoprefixer 등 도구 활용
   postcss + autoprefixer로 자동 관리       ← 가장 권장

4. Compatibility Standard에만 있는 기능은 주의
   -webkit-line-clamp                      ← 대안이 없으므로 사용
   -webkit-text-fill-color                 ← 표준화 진행 중
```

#### 10.2 피해야 할 패턴

```css
/* [피해야 함] -webkit- 접두사만 사용 */
.bad {
    -webkit-transform: rotate(45deg);
    /* 표준 속성 누락! */
}

/* [올바른 방법] 표준 속성 포함 */
.good {
    -webkit-transform: rotate(45deg);
    transform: rotate(45deg);
}

/* [더 나은 방법] 표준 속성만 사용 */
.better {
    transform: rotate(45deg);
}
```

```javascript
// [피해야 함] -webkit- API로만 기능 감지
if ('webkitRequestAnimationFrame' in window) {
    // ...
}

// [올바른 방법] 표준 API 우선 확인
const raf = window.requestAnimationFrame || window.webkitRequestAnimationFrame;
if (raf) {
    raf(callback);
}
```

#### 10.3 Compatibility Standard의 미래

Compatibility Standard: Living Standard로서 지속적으로 업데이트.

- 새로운 호환성 문제가 발생할 때마다 문서에 추가
- 더 이상 필요하지 않은 항목은 제거

```
[호환성 표준의 생명주기]

비표준 기능 등장
    ↓
광범위한 웹 사용
    ↓
다른 브라우저에서 호환성 문제 발생
    ↓
Compatibility Standard에 추가          ← 비표준 기능의 표준화
    ↓
모든 브라우저가 표준에 맞게 구현
    ↓
표준 CSS/DOM 속성으로 대체 진행
    ↓
레거시 호환성 유지 (제거하지 않음)       ← 웹은 절대 깨뜨리지 않음
```

---

### 참고 자료

- [Compatibility Standard (WHATWG)](https://compat.spec.whatwg.org/)
- [Can I Use](https://caniuse.com/)
- [MDN Web Docs - Browser compatibility](https://developer.mozilla.org/en-US/docs/Web/CSS/WebKit_Extensions)
- [Autoprefixer](https://github.com/postcss/autoprefixer)

---

## Quirks Mode Standard 상세 가이드

### 목차

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

### 1. 개요

#### 쿼크 모드란

Quirks Mode Standard: WHATWG에서 관리하는 Living Standard.

- 웹 브라우저가 오래된 웹 페이지와의 호환성을 유지하기 위해 의도적으로 표준과 다르게 동작하는 렌더링 모드를 정의

1990년대 후반, 웹 표준이 확립되기 전에 만들어진 수많은 웹 페이지들이 당시 브라우저의 비표준 동작에 의존 → 새로운 표준을 따르면 이러한 레거시 페이지들이 깨질 수 있음 → 브라우저들이 문서의 DOCTYPE 선언을 기준으로 표준 모드와 호환성 모드를 전환하는 메커니즘 도입.

```
[핵심 원리: DOCTYPE 스위칭]

<!DOCTYPE html>           → No-quirks Mode (표준 모드)
├── 표준에 맞게 렌더링
└── 현대 웹 페이지에 적합

(DOCTYPE 없음)            → Quirks Mode (쿼크 모드)
├── 레거시 동작으로 렌더링
└── 1990년대 웹 페이지 호환
```

#### 왜 "Quirks"인가

"Quirk": 영어로 "기이한 특성" 또는 "별난 점"을 의미.

- 초기 브라우저들의 비표준 렌더링 동작들을 "quirks"라고 부름
- 이러한 quirks를 재현하는 모드가 "Quirks Mode"

---

### 2. 모드 종류

웹 브라우저: 세 가지 렌더링 모드를 가짐.

#### 2.1 Quirks Mode (쿼크 모드)

- 별칭: 호환성 모드 (Compatibility Mode)
- 활성화 조건: DOCTYPE이 없거나 특정 구형 DOCTYPE
- CSS 박스 모델: `display: table-cell` 요소의 높이 계산에 한해 IE 박스 모델(border-box) 적용
- 렌더링: 1990년대 브라우저 동작 재현
- `document.compatMode`: `"BackCompat"`

#### 2.2 No-quirks Mode (표준 모드)

- 별칭: Standards Mode
- 활성화 조건: `<!DOCTYPE html>` 또는 완전한 DOCTYPE
- CSS 박스 모델: W3C 표준 박스 모델 (content-box)
- 렌더링: 웹 표준에 따른 정확한 렌더링
- `document.compatMode`: `"CSS1Compat"`

#### 2.3 Limited-quirks Mode (거의 표준 모드)

- 별칭: Almost Standards Mode
- 활성화 조건: Transitional/Frameset DOCTYPE
- CSS 박스 모델: W3C 표준 박스 모델 (content-box)
- 렌더링: 표준 모드와 거의 동일, 일부 quirks만 적용
- `document.compatMode`: `"CSS1Compat"`

#### 2.4 세 모드의 비교

```
[렌더링 모드 스펙트럼]

완전 비표준                                    완전 표준
←────────────────────────────────────────────────→
Quirks Mode    Limited-quirks Mode    No-quirks Mode
(많은 quirks)    (일부 quirks)        (quirks 없음)
```

```
[핵심 차이점 요약]

                          Quirks           Limited-quirks   No-quirks
테이블 셀 높이 계산       border-box(*)    content-box      content-box
line-height 관련 quirks   적용             적용             미적용
body 배경 전파           특수 규칙        표준             표준
% height 계산            특수             표준             표준
스크롤 요소              body             html             html
ID 대소문자              무시             구분             구분

(*) `display: table-cell` 요소의 height/min-height/max-height 계산에 한정된 quirk
    width 계산에는 영향을 주지 않음
```

---

### 3. DOCTYPE에 따른 모드 선택

#### 3.1 모드 선택 알고리즘 개요

브라우저의 HTML 파서: 문서의 시작 부분에서 DOCTYPE 선언을 분석하여 렌더링 모드를 결정.

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

#### 3.2 No-quirks Mode를 유발하는 DOCTYPE

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

#### 3.3 Limited-quirks Mode를 유발하는 DOCTYPE

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

#### 3.4 Quirks Mode를 유발하는 DOCTYPE

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

#### 3.5 완전한 모드 선택 표

- `<!DOCTYPE html>` → no-quirks
- `<!DOCTYPE html SYSTEM "...">` → no-quirks
- DOCTYPE 없음 → quirks
- `<!DOCTYPE html SYSTEM "http://www.ibm.com/...">` → quirks
- HTML 4.01 Strict (시스템 ID 있음) → no-quirks
- HTML 4.01 Strict (시스템 ID 없음) → no-quirks
- HTML 4.01 Transitional (시스템 ID 있음) → limited-quirks
- HTML 4.01 Transitional (시스템 ID 없음) → quirks
- HTML 4.01 Frameset (시스템 ID 있음) → limited-quirks
- HTML 4.01 Frameset (시스템 ID 없음) → quirks
- XHTML 1.0 Strict → no-quirks
- XHTML 1.0 Transitional → limited-quirks
- XHTML 1.0 Frameset → limited-quirks
- XHTML 1.1 → no-quirks
- HTML 3.2 → quirks
- HTML 2.0 → quirks

---

### 4. 쿼크 모드의 CSS 차이점

#### 4.1 박스 모델 차이 (테이블 셀 높이 계산)

쿼크 모드의 CSS 박스 모델 quirk: 전체 요소에 적용되는 것이 아니라 `display: table-cell`인 요소의 `height`/`min-height`/`max-height` 계산에만 한정.

- `width` 계산에는 영향을 주지 않음

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


[IE 박스 모델 (Quirks Mode, table-cell 요소의 높이 계산에 한정)]
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
/* 실제 차이 예시: table-cell 요소의 높이 계산에서만 나타남 */
td {
    height: 100px;
    padding: 20px;
    border: 5px solid black;
}

/* No-quirks Mode (표준): height는 content 영역만 의미
   → 셀 전체 높이 = 100 + 20*2 + 5*2 = 150px
*/

/* Quirks Mode: height가 border까지 포함하는 것으로 계산
   → 셀 전체 높이 = 100px, content 높이 = 100 - 20*2 - 5*2 = 50px
*/
```

#### 4.2 테이블 셀 높이 계산

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

#### 4.3 테이블 셀 내 이미지의 줄바꿈

```
[Quirks Mode 전용 quirk]

너비가 auto인 테이블 셀 안의 이미지: 줄바꿈(개행) 기회를 갖지 못함
No-quirks Mode: 이런 제약 없음
```

#### 4.3.1 nowrap 셀의 최소 너비

```
[Quirks Mode 전용 quirk]

width와 nowrap 속성이 함께 지정된 테이블 셀:
지정된 width와 콘텐츠의 min-content 너비 중 더 큰 값 사용
```

#### 4.4 Percentage Height 계산

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

#### 4.5 Body의 Margin

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

#### 4.6 테이블의 색상 상속과 장식

```css
/* Quirks Mode 전용 quirk: 테이블은 body의 color를 별도 규칙("quirk-inherit")으로 상속받는다.
   또한 text-decoration(밑줄 등)은 테이블 요소 경계를 넘어 전파되지 않는다. */

body {
    color: blue;
    text-decoration: underline;
}

table {
    /* Quirks Mode: color는 상속되지만 밑줄은 테이블 내부까지 이어지지 않음 */
}
```

빈 테이블(행 그룹이나 채워진 열 그룹이 없는 `<table>`): Quirks Mode에서 높이 0, `border-style: none`으로 축소.

#### 4.7 이미지 주변의 공백

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

#### 4.8 배경색 전파

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

#### 4.9 CSS 파싱 차이

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

> 위 두 quirk(단위 없는 길이값, 잘못된 색상값 파싱): quirks mode뿐 아니라 limited-quirks mode에도 동일하게 적용됨.

#### 4.10 전체 CSS 차이점 요약표

CSS 동작별 No-quirks Mode·Quirks Mode 비교:

- table-cell 높이 계산
  - No-quirks Mode: content-box
  - Quirks Mode: border-box
- % height (부모 height 없을 때)
  - No-quirks Mode: auto로 처리
  - Quirks Mode: 가장 가까운 높이 기준
- 테이블의 color 상속
  - No-quirks Mode: 표준 상속 규칙
  - Quirks Mode: quirk-inherit 규칙
- 테이블 경계를 넘는 text-decoration
  - No-quirks Mode: 전파됨
  - Quirks Mode: 전파 안 됨
- 빈 테이블(행 그룹 없음)
  - No-quirks Mode: 일반 규칙대로 렌더링
  - Quirks Mode: 높이 0, border-style: none으로 축소
- 단위 없는 길이값
  - No-quirks Mode: 무효 (무시)
  - Quirks Mode: px로 해석
- 잘못된 색상값
  - No-quirks Mode: 무효 (무시)
  - Quirks Mode: 16진수 파싱 시도
- body 배경 전파
  - No-quirks Mode: 표준 규칙
  - Quirks Mode: 특수 규칙
- :active/:hover (단순 셀렉터)
  - No-quirks Mode: 항상 매칭
  - Quirks Mode: :any-link에 매칭되는 요소만
- 테이블 셀 내 auto 너비 이미지
  - No-quirks Mode: 줄바꿈 가능
  - Quirks Mode: 줄바꿈 불가
- nowrap + width 지정 셀
  - No-quirks Mode: 지정된 width
  - Quirks Mode: max(지정된 width, min-content)

---

### 5. 쿼크 모드의 DOM 차이점

#### 5.1 document.body 스크롤

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

#### 5.2 getElementById 대소문자

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

#### 5.3 className 기반 매칭

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

#### 5.4 CSS 선택자의 대소문자

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

#### 5.5 document.all

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

#### 5.6 DOM 차이점 요약표

DOM 동작별 No-quirks Mode·Quirks Mode 비교:

- `scrollTop` 소유자
  - No-quirks Mode: `documentElement`
  - Quirks Mode: `body`
- `scrollHeight` 소유자
  - No-quirks Mode: `documentElement`
  - Quirks Mode: `body`
- `clientHeight` 소유자
  - No-quirks Mode: `documentElement`
  - Quirks Mode: `body`
- `getElementById` 대소문자
  - No-quirks Mode: 구분함
  - Quirks Mode: 구분하지 않을 수 있음
- `getElementsByClassName` 대소문자
  - No-quirks Mode: 구분함
  - Quirks Mode: 구분하지 않음
- CSS ID/class 선택자 대소문자
  - No-quirks Mode: 구분함
  - Quirks Mode: 구분하지 않음
- `document.compatMode`
  - No-quirks Mode: `"CSS1Compat"`
  - Quirks Mode: `"BackCompat"`

---

### 6. Limited-quirks Mode의 특징

#### 6.1 개요

Limited-quirks mode: "Almost Standards Mode"라고도 불림 → 표준 모드와 거의 동일하지만 소수의 quirk 포함.

- 대표적인 것: 테이블 셀 내 인라인 이미지의 기준선 처리
- 이 외에 line-height 계산과 관련된 quirk도 두 가지 적용(quirks mode와 공유)

#### 6.2 인라인 이미지의 기준선 처리

이것: limited-quirks mode에서 가장 잘 알려진 핵심 quirk.

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

#### 6.2-1 line-height 관련 quirk (quirks mode와 공유)

limited-quirks mode: line-height 계산과 관련된 quirk 두 가지가 추가로 적용됨(quirks mode에서도 동일하게 적용).

```
1. 테두리와 패딩이 없고 텍스트가 없거나 공백만 있는 인라인 박스는
   line-height: 0으로 취급

2. 인라인 레벨 콘텐츠를 가진 블록 컨테이너는 줄 상자의 최소 높이("strut")를
   계산할 때 line-height 속성을 무시
```

#### 6.3 왜 Limited-quirks Mode가 필요한가

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

#### 6.4 Limited-quirks Mode와 No-quirks Mode의 동작 차이

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

### 7. 모드 확인 방법

#### 7.1 JavaScript로 확인

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

#### 7.2 개발자 도구에서 확인

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

#### 7.3 프로그래밍적 모드 감지 유틸리티

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

#### 7.4 DOCTYPE 정보 확인

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

### 8. 역사적 배경

#### 8.1 타임라인

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

#### 8.2 IE Box Model Bug

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

### 9. 모드 전환의 영향

#### 9.1 모드 변경 시 영향 범위

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

#### 9.2 Quirks Mode에서 No-quirks Mode로 전환 시

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

#### 9.3 마이그레이션 체크리스트

```
[Quirks Mode → No-quirks Mode 마이그레이션 체크리스트]

□ 1. <!DOCTYPE html> 추가
□ 2. box-sizing: border-box 적용
     *, *::before, *::after { box-sizing: border-box; }
□ 3. 스크롤 관련 코드 확인
     document.body.scrollTop → document.documentElement.scrollTop
□ 4. 테이블 레이아웃의 이미지 간격 확인
     이미지에 display: block 또는 vertical-align: bottom 추가
□ 5. 테이블 내부의 text-decoration 전파 여부 확인
     필요하면 자식 요소에 직접 text-decoration 지정
□ 6. 단위 없는 숫자 값 수정
     width: 100 → width: 100px
□ 7. ID/class 대소문자 일관성 확인
     HTML과 CSS/JS에서 동일한 대소문자 사용
□ 8. 비표준 CSS 속성/값 수정
□ 9. 전체 레이아웃 테스트
□ 10. 다양한 브라우저에서 확인
```

---

### 10. 실용 가이드

#### 10.1 올바른 DOCTYPE 사용

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

#### 10.2 호환성 있는 CSS 작성

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

#### 10.3 모드를 고려한 JavaScript 작성

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

#### 10.4 레거시 코드 진단

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

### 11. 브라우저별 동작 차이

#### 11.1 Quirks Mode 구현의 차이

각 브라우저: Quirks Mode를 약간 다르게 구현.

- Quirks Mode Standard는 가장 일반적인 동작을 표준화하려고 하지만, 모든 세부 사항이 동일하지는 않음

Quirk별 Chrome·Firefox·Safari·Edge 구현 현황:

- IE 박스 모델: 전 브라우저 구현
- 단위 없는 값 → px: Firefox 구현 · Chrome·Safari·Edge 부분 구현
- 대소문자 무시 (getElementById): Firefox 구현 · Chrome·Safari·Edge 미구현
- 대소문자 무시 (class 선택자): 전 브라우저 구현
- 잘못된 색상값 파싱: 전 브라우저 구현
- body 스크롤: 전 브라우저 구현
- 테이블 font 상속: 전 브라우저 구현
- 인라인 이미지 갭 제거: 전 브라우저 구현

#### 11.2 document.compatMode 지원

- Chrome: 모든 버전
- Firefox: 모든 버전
- Safari: 모든 버전
- Edge: 모든 버전
- IE: 6+

---

### 12. 참고 자료

- [Quirks Mode Standard (WHATWG)](https://quirks.spec.whatwg.org/)
- [MDN - Quirks Mode and Standards Mode](https://developer.mozilla.org/ko/docs/Web/HTML/Quirks_Mode_and_Standards_Mode)
- [MDN - document.compatMode](https://developer.mozilla.org/en-US/docs/Web/API/Document/compatMode)
- [Activating Browser Modes with Doctype (Henri Sivonen)](https://hsivonen.fi/doctype/)
- [Box Model (MDN)](https://developer.mozilla.org/ko/docs/Learn/CSS/Building_blocks/The_box_model)
- [CSS Box Sizing (MDN)](https://developer.mozilla.org/ko/docs/Web/CSS/box-sizing)
