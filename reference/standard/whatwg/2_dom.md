# WHATWG DOM Living Standard

## DOM Living Standard 완벽 가이드

> WHATWG DOM Living Standard - Part 1: Core Interfaces & Tree Structure
> 출처: https://dom.spec.whatwg.org/

---

### 목차

1. [개요](#1-개요)
2. [노드(Node) 인터페이스](#2-노드node-인터페이스)
3. [Document 인터페이스](#3-document-인터페이스)
4. [Element 인터페이스](#4-element-인터페이스)
5. [Attr 인터페이스](#5-attr-인터페이스)
6. [CharacterData 인터페이스](#6-characterdata-인터페이스)
7. [DocumentFragment 인터페이스](#7-documentfragment-인터페이스)
8. [DOM Collections](#8-dom-collections)
9. [DOM Traversal Mixins](#9-dom-traversal-mixins)

---

### 1. 개요

#### DOM이란 무엇인가

DOM(Document Object Model): HTML·XML·SVG 문서를 프로그래밍 방식으로 접근·조작하는 플랫폼 및 언어 중립 인터페이스

- 웹 브라우저가 HTML 문서를 파싱 → 결과물이 바로 DOM 트리(tree)
- JavaScript를 통해 트리의 노드를 생성·수정·삭제 가능

DOM: 단순한 API가 아니라 문서의 구조적 표현(structural representation)

- 브라우저가 HTML 소스 코드를 읽어 메모리 상의 객체 트리로 변환
- 트리의 각 요소는 노드(Node) 객체로 표현 → 부모-자식(parent-child)·형제(sibling) 관계 형성

```
Document
└── html (Element)
    ├── head (Element)
    │   ├── meta (Element)
    │   └── title (Element)
    │       └── "페이지 제목" (Text)
    └── body (Element)
        ├── h1 (Element)
        │   └── "Hello World" (Text)
        └── p (Element)
            └── "본문 텍스트" (Text)
```

#### Living Standard로서의 DOM

WHATWG(Web Hypertext Application Technology Working Group): DOM 명세를 Living Standard 방식으로 관리 → 버전 번호 없이 지속적으로 업데이트되는 단일 문서

- W3C의 스냅샷 기반 표준과 차이: Living Standard는 항상 최신 상태를 유지하며 브라우저 구현과 동기화

Living Standard의 장점:
- 브라우저 벤더와의 실시간 동기화
- 빠른 버그 수정 및 기능 추가
- 구현 현실과 명세 간의 괴리 최소화
- 단일 진실 공급원(Single Source of Truth)

#### 역사: DOM Level 1 -> Living Standard

DOM 명세의 역사: 웹의 발전과 궤를 같이함

- 1998 · DOM Level 1: Core·HTML 기본 인터페이스 정의
- 2000 · DOM Level 2: Events·Style·Traversal·Range 추가
- 2004 · DOM Level 3: XPath·Load/Save·Validation 추가
- 2015 · DOM4 (W3C): 현대화된 API·Promise 기반 설계
- 현재 · Living Standard: WHATWG 단일 명세·지속적 업데이트

- DOM Level 1: 기본적인 트리 조작(createElement, appendChild 등)만 가능
- DOM Level 2: 이벤트 모델(addEventListener) 도입
- DOM Level 3: 키보드 이벤트·XPath 추가
- 현재 Living Standard: 위 전체를 통합 + MutationObserver·Shadow DOM·Custom Elements 등 현대적 기능 포함

#### DOM의 핵심 개념

트리 구조(Tree Structure): DOM은 순서가 있는 트리(ordered tree)

- 모든 노드는 최대 하나의 부모를 가지며 0개 이상의 자식을 가질 수 있음
- 루트(root): 부모가 없는 노드
- 잎(leaf): 자식이 없는 노드

노드(Node): 트리의 각 참여자(participant)

- Document, Element, Text, Comment 등이 모두 Node를 상속
- 모든 노드는 `nodeType`으로 구분 → 12가지 타입이 정의(실제 사용되는 것은 7가지)

인터페이스(Interface): DOM은 IDL(Interface Definition Language)로 정의

- 각 인터페이스는 속성(attribute)과 메서드(method)를 가지며 상속 계층을 형성

```
EventTarget
└── Node
    ├── Document
    ├── DocumentType
    ├── DocumentFragment
    │   └── ShadowRoot
    ├── Element
    │   └── HTMLElement, SVGElement, ...
    ├── CharacterData
    │   ├── Text
    │   │   └── CDATASection
    │   ├── Comment
    │   └── ProcessingInstruction
    └── Attr
```

모든 노드는 `EventTarget`을 상속 → 모든 DOM 노드에 이벤트 리스너 등록 가능 → DOM 이벤트 시스템의 기반

---

### 2. 노드(Node) 인터페이스

Node 인터페이스: DOM 트리의 모든 참여자가 구현하는 기본 인터페이스

- Document, Element, Text, Comment 등 모든 노드 타입이 Node를 상속
- 트리 탐색과 조작을 위한 핵심 속성·메서드 제공

#### 2.1 Node 타입 상수

Node 인터페이스: 노드의 종류를 구분하기 위한 상수를 정의

- `nodeType` 속성이 반환하는 값이며, 각 상수는 unsigned short(정수) 값

```javascript
// Node 타입 상수 (IDL 정의)
interface Node : EventTarget {
  const unsigned short ELEMENT_NODE = 1;
  const unsigned short ATTRIBUTE_NODE = 2;
  const unsigned short TEXT_NODE = 3;
  const unsigned short CDATA_SECTION_NODE = 4;
  // 5, 6은 역사적 이유로 존재했으나 현재 제거됨
  const unsigned short PROCESSING_INSTRUCTION_NODE = 7;
  const unsigned short COMMENT_NODE = 8;
  const unsigned short DOCUMENT_NODE = 9;
  const unsigned short DOCUMENT_TYPE_NODE = 10;
  const unsigned short DOCUMENT_FRAGMENT_NODE = 11;
  // 12 (NOTATION_NODE)도 역사적 이유로 제거됨
};
```

- `ELEMENT_NODE` (값 1): HTML/SVG 요소 · nodeName은 태그명(대문자, 예: "DIV")
- `ATTRIBUTE_NODE` (값 2): 속성 노드(레거시) · nodeName은 속성명
- `TEXT_NODE` (값 3): 텍스트 내용 · nodeName은 "#text"
- `CDATA_SECTION_NODE` (값 4): XML CDATA 섹션 · nodeName은 "#cdata-section"
- `PROCESSING_INSTRUCTION_NODE` (값 7): XML 처리 명령 · nodeName은 target 값
- `COMMENT_NODE` (값 8): 주석 · nodeName은 "#comment"
- `DOCUMENT_NODE` (값 9): 문서 루트 · nodeName은 "#document"
- `DOCUMENT_TYPE_NODE` (값 10): DOCTYPE 선언 · nodeName은 doctype name
- `DOCUMENT_FRAGMENT_NODE` (값 11): 문서 조각 · nodeName은 "#document-fragment"

```javascript
// 노드 타입 확인 예제
const div = document.createElement('div');
console.log(div.nodeType);                    // 1 (ELEMENT_NODE)
console.log(div.nodeType === Node.ELEMENT_NODE); // true

const text = document.createTextNode('hello');
console.log(text.nodeType);                   // 3 (TEXT_NODE)

const comment = document.createComment('주석');
console.log(comment.nodeType);                // 8 (COMMENT_NODE)

console.log(document.nodeType);               // 9 (DOCUMENT_NODE)
console.log(document.doctype.nodeType);       // 10 (DOCUMENT_TYPE_NODE)

// 참고: ATTRIBUTE_NODE(2)는 레거시 상수로 취급되지만, Attr은 현재 스펙에서도
// 여전히 Node를 상속하며 nodeType 값도 그대로 2를 반환한다.
```

#### 2.2 Node 속성 (Properties)

##### 기본 정보 속성

```javascript
// nodeType: 노드의 타입을 나타내는 정수
element.nodeType;   // 1

// nodeName: 노드의 이름
// Element → 태그명(HTML에서는 대문자), Text → "#text", Comment → "#comment"
document.createElement('div').nodeName;          // "DIV"
document.createElement('span').nodeName;         // "SPAN"
document.createTextNode('hi').nodeName;          // "#text"
document.createComment('note').nodeName;         // "#comment"
document.nodeName;                                // "#document"

// nodeValue: 노드의 값 (Element와 Document는 null)
const textNode = document.createTextNode('hello');
console.log(textNode.nodeValue);                 // "hello"
textNode.nodeValue = 'world';                    // 값 변경 가능
console.log(textNode.nodeValue);                 // "world"

const elem = document.createElement('div');
console.log(elem.nodeValue);                     // null (Element는 항상 null)

const commentNode = document.createComment('주석 내용');
console.log(commentNode.nodeValue);              // "주석 내용"
```

##### textContent 속성

`textContent`: 노드와 그 자손의 텍스트 내용을 반환·설정

- `innerText`와 차이: CSS에 의해 숨겨진 텍스트도 포함
- 렌더링을 트리거하지 않음 → 성능 우수

```javascript
// textContent 읽기
const container = document.createElement('div');
container.innerHTML = '<p>Hello <strong>World</strong></p><!-- comment -->';

console.log(container.textContent);  // "Hello World"
// 주석 노드의 텍스트는 포함되지 않으며, 모든 Text 자손의 data를 연결(concatenate)한 값

// textContent 설정 - 모든 자식을 제거하고 단일 텍스트 노드로 대체
container.textContent = '새로운 텍스트';
console.log(container.childNodes.length); // 1 (텍스트 노드 하나)
console.log(container.innerHTML);         // "새로운 텍스트"

// null 설정 시 빈 문자열과 동일하게 동작 (Element의 경우)
container.textContent = null;
console.log(container.childNodes.length); // 0

// 노드 타입별 textContent 동작:
// - Document, DocumentType → null 반환, 설정 무시
// - DocumentFragment, Element → 자손 Text 노드의 data 연결
// - Text, Comment, CDATASection, ProcessingInstruction → nodeValue와 동일
// - Attr → value와 동일
```

##### 관계 및 상태 속성

```javascript
// baseURI: 노드의 기본 URI (보통 document.URL)
console.log(document.baseURI);
// "https://example.com/page.html" (또는 <base> 요소에 의해 변경 가능)

// isConnected: 노드가 Document에 연결되어 있는지 여부
const orphan = document.createElement('div');
console.log(orphan.isConnected);           // false

document.body.appendChild(orphan);
console.log(orphan.isConnected);           // true

document.body.removeChild(orphan);
console.log(orphan.isConnected);           // false

// Shadow DOM 내부의 노드도 isConnected가 true
const host = document.createElement('div');
document.body.appendChild(host);
const shadow = host.attachShadow({ mode: 'open' });
const inner = document.createElement('span');
shadow.appendChild(inner);
console.log(inner.isConnected);            // true

// ownerDocument: 노드가 속한 Document (Document 자신은 null)
const el = document.createElement('p');
console.log(el.ownerDocument === document);  // true
console.log(document.ownerDocument);         // null
```

##### 트리 탐색 속성

```javascript
// HTML 구조 가정:
// <div id="parent">
//   <span id="first">첫째</span>
//   <span id="middle">둘째</span>
//   <span id="last">셋째</span>
// </div>

const parent = document.getElementById('parent');

// parentNode: 부모 노드 (Node 타입, Document나 DocumentFragment도 가능)
console.log(parent.firstChild.parentNode === parent); // true

// parentElement: 부모 요소 (Element 타입만, 부모가 Element가 아니면 null)
// document.documentElement.parentNode === document (Document)
// document.documentElement.parentElement === null (Document는 Element가 아님)
console.log(document.documentElement.parentNode);    // #document
console.log(document.documentElement.parentElement); // null

// childNodes: 모든 자식 노드의 live NodeList (텍스트, 주석 노드 포함)
console.log(parent.childNodes.length);
// 주의: 공백 텍스트 노드가 포함될 수 있음
// 위 HTML에서 줄바꿈/공백이 있으면 Text 노드가 사이사이에 존재

// firstChild / lastChild: 첫 번째/마지막 자식 노드
console.log(parent.firstChild);  // 공백 Text 노드 또는 첫 번째 <span>
console.log(parent.lastChild);   // 공백 Text 노드 또는 마지막 <span>

// previousSibling / nextSibling: 이전/다음 형제 노드
const middle = document.getElementById('middle');
console.log(middle.previousSibling); // Text 노드(공백) 또는 #first <span>
console.log(middle.nextSibling);     // Text 노드(공백) 또는 #last <span>
```

#### 2.3 Node 메서드 - 조회 및 비교

##### hasChildNodes()

```javascript
// 자식 노드가 있으면 true, 없으면 false
const div = document.createElement('div');
console.log(div.hasChildNodes());            // false

div.appendChild(document.createTextNode(''));
console.log(div.hasChildNodes());            // true (빈 텍스트 노드도 자식)

// childNodes.length > 0 과 동일한 결과이지만, 의미론적으로 더 명확
```

##### getRootNode(options)

```javascript
// 노드가 속한 트리의 루트 노드를 반환
const div = document.createElement('div');
const span = document.createElement('span');
div.appendChild(span);

// 연결되지 않은 트리의 루트
console.log(span.getRootNode());             // div (트리의 루트)
console.log(div.getRootNode());              // div (자기 자신)

// Document에 연결된 경우
document.body.appendChild(div);
console.log(span.getRootNode());             // #document

// Shadow DOM에서의 getRootNode
const host = document.createElement('div');
document.body.appendChild(host);
const shadowRoot = host.attachShadow({ mode: 'open' });
const shadowChild = document.createElement('p');
shadowRoot.appendChild(shadowChild);

console.log(shadowChild.getRootNode());                      // ShadowRoot
console.log(shadowChild.getRootNode({ composed: true }));    // #document
// composed: true 옵션은 Shadow DOM 경계를 넘어 최상위 Document까지 탐색
```

##### normalize()

```javascript
// 인접한 Text 노드를 병합하고, 빈 Text 노드를 제거
const div = document.createElement('div');
div.appendChild(document.createTextNode('Hello'));
div.appendChild(document.createTextNode(' '));
div.appendChild(document.createTextNode('World'));
div.appendChild(document.createTextNode(''));  // 빈 텍스트 노드

console.log(div.childNodes.length);           // 4

div.normalize();

console.log(div.childNodes.length);           // 1
console.log(div.textContent);                 // "Hello World"
// 모든 인접 Text 노드가 하나로 병합되고, 빈 Text 노드는 제거됨
```

##### cloneNode(deep)

```javascript
// 노드를 복제한다. deep이 true이면 자손까지 모두 복제.
const original = document.createElement('div');
original.id = 'original';
original.innerHTML = '<p>Hello <strong>World</strong></p>';

// 얕은 복제 (자식 미포함)
const shallow = original.cloneNode(false);
console.log(shallow.id);                      // "original"
console.log(shallow.childNodes.length);        // 0
console.log(shallow.outerHTML);                // '<div id="original"></div>'

// 깊은 복제 (자손 포함)
const deep = original.cloneNode(true);
console.log(deep.childNodes.length);           // 1 (p 요소)
console.log(deep.innerHTML);                   // '<p>Hello <strong>World</strong></p>'

// 주의사항:
// 1. 복제된 노드는 ownerDocument는 유지하지만 트리에 삽입되지 않음 (isConnected: false)
// 2. id 속성도 복제되므로, 삽입 전에 id를 변경해야 중복을 방지할 수 있음
// 3. 이벤트 리스너는 복제되지 않음 (addEventListener로 등록한 것)
// 4. 인라인 이벤트 핸들러(onclick="...")는 속성이므로 복제됨
deep.id = 'cloned';
document.body.appendChild(deep);
```

##### isEqualNode(otherNode) / isSameNode(otherNode)

```javascript
// isEqualNode: 두 노드가 구조적으로 동일한지 비교
const a = document.createElement('div');
a.className = 'test';
a.textContent = 'hello';

const b = document.createElement('div');
b.className = 'test';
b.textContent = 'hello';

console.log(a.isEqualNode(b));               // true (구조적으로 동일)
console.log(a.isSameNode(b));                 // false (다른 객체)

// isSameNode: 두 참조가 같은 노드 객체를 가리키는지 확인 (=== 연산자와 동일)
const c = a;
console.log(a.isSameNode(c));                 // true
console.log(a === c);                         // true (동일한 결과)

// isEqualNode의 비교 기준:
// - nodeType이 같아야 함
// - Element: namespaceURI, prefix, localName, 속성 수와 값, 자식 수와 순서
// - Text/Comment: data가 같아야 함
// - DocumentType: name, publicId, systemId가 같아야 함
```

##### compareDocumentPosition(other)

```javascript
// 두 노드의 문서 내 상대적 위치를 비트마스크로 반환
const parent = document.createElement('div');
const child = document.createElement('span');
const sibling = document.createElement('p');
parent.appendChild(child);
parent.appendChild(sibling);

// 비트마스크 상수
// Node.DOCUMENT_POSITION_DISCONNECTED     = 1  (0x01) 다른 문서에 속함
// Node.DOCUMENT_POSITION_PRECEDING        = 2  (0x02) 앞에 위치
// Node.DOCUMENT_POSITION_FOLLOWING        = 4  (0x04) 뒤에 위치
// Node.DOCUMENT_POSITION_CONTAINS         = 8  (0x08) 포함함 (조상)
// Node.DOCUMENT_POSITION_CONTAINED_BY     = 16 (0x10) 포함됨 (자손)
// Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 32 (0x20) 구현 특정

const result = parent.compareDocumentPosition(child);
console.log(result & Node.DOCUMENT_POSITION_CONTAINED_BY);  // 16 (child는 parent에 포함됨)
console.log(result & Node.DOCUMENT_POSITION_FOLLOWING);      // 4

const result2 = child.compareDocumentPosition(sibling);
console.log(result2 & Node.DOCUMENT_POSITION_FOLLOWING);     // 4 (sibling이 뒤에)

// 실전 활용: 노드 순서 정렬
function sortByDocumentOrder(nodes) {
  return [...nodes].sort((a, b) => {
    const position = a.compareDocumentPosition(b);
    if (position & Node.DOCUMENT_POSITION_FOLLOWING) return -1;
    if (position & Node.DOCUMENT_POSITION_PRECEDING) return 1;
    return 0;
  });
}
```

##### contains(other)

```javascript
// 노드가 other를 자손으로 포함하는지 확인 (자기 자신도 true)
const div = document.createElement('div');
const p = document.createElement('p');
const span = document.createElement('span');
div.appendChild(p);
p.appendChild(span);

console.log(div.contains(span));             // true (자손)
console.log(div.contains(p));                // true (자식)
console.log(div.contains(div));              // true (자기 자신)
console.log(p.contains(div));                // false (부모는 자손이 아님)
console.log(div.contains(null));             // false

// 실전 활용: 클릭이 특정 영역 내부인지 확인
document.addEventListener('click', (e) => {
  const modal = document.getElementById('modal');
  if (modal && !modal.contains(e.target)) {
    // 모달 외부 클릭 시 닫기
    modal.style.display = 'none';
  }
});
```

#### 2.4 Node 변경 메서드

##### insertBefore(node, child)

```javascript
// child 앞에 node를 삽입한다. child가 null이면 맨 뒤에 추가(appendChild와 동일).
// 반환값: 삽입된 노드
const parent = document.createElement('ul');
const li1 = document.createElement('li');
li1.textContent = '첫째';
const li2 = document.createElement('li');
li2.textContent = '셋째';
parent.appendChild(li1);
parent.appendChild(li2);

const li_new = document.createElement('li');
li_new.textContent = '둘째';
const inserted = parent.insertBefore(li_new, li2);
// 결과: 첫째 → 둘째 → 셋째

console.log(inserted === li_new);            // true (삽입된 노드 반환)
console.log(parent.innerHTML);               // <li>첫째</li><li>둘째</li><li>셋째</li>

// child가 null인 경우: appendChild와 동일
parent.insertBefore(document.createElement('li'), null);

// 예외 상황:
// - child가 parent의 자식이 아닌 경우 → NotFoundError DOMException
// - node가 parent의 조상인 경우 → HierarchyRequestError DOMException
// - node가 허용되지 않는 타입인 경우 → HierarchyRequestError DOMException
```

##### appendChild(node)

```javascript
// node를 자식 목록의 맨 뒤에 추가한다.
// 반환값: 추가된 노드
// node가 이미 트리에 존재하면, 기존 위치에서 제거 후 새 위치에 삽입(이동)
const list = document.createElement('ul');
const item1 = document.createElement('li');
item1.textContent = 'A';
const item2 = document.createElement('li');
item2.textContent = 'B';

list.appendChild(item1);
list.appendChild(item2);
console.log(list.innerHTML);                 // <li>A</li><li>B</li>

// 이미 존재하는 노드를 appendChild하면 이동됨
list.appendChild(item1);                     // item1이 뒤로 이동
console.log(list.innerHTML);                 // <li>B</li><li>A</li>

// DocumentFragment를 appendChild하면, Fragment의 모든 자식이 이동됨
const frag = document.createDocumentFragment();
frag.appendChild(document.createElement('li'));
frag.appendChild(document.createElement('li'));
list.appendChild(frag);
console.log(frag.childNodes.length);         // 0 (Fragment는 비워짐)
console.log(list.children.length);           // 4
```

##### replaceChild(node, child)

```javascript
// child를 node로 교체한다.
// 반환값: 교체된(제거된) child 노드
const parent = document.createElement('div');
const old = document.createElement('span');
old.textContent = '이전 내용';
parent.appendChild(old);

const replacement = document.createElement('strong');
replacement.textContent = '새 내용';

const removed = parent.replaceChild(replacement, old);
console.log(removed === old);                // true
console.log(removed.parentNode);             // null (트리에서 제거됨)
console.log(parent.innerHTML);               // <strong>새 내용</strong>

// 예외: child가 parent의 자식이 아니면 NotFoundError
// 예외: node가 parent의 조상이면 HierarchyRequestError
```

##### removeChild(child)

```javascript
// child를 부모에서 제거한다.
// 반환값: 제거된 노드 (메모리에서 삭제되지는 않음, 참조가 유지됨)
const parent = document.createElement('div');
const child = document.createElement('span');
child.textContent = '제거될 요소';
parent.appendChild(child);

const removed = parent.removeChild(child);
console.log(removed === child);              // true
console.log(removed.parentNode);             // null
console.log(removed.textContent);            // "제거될 요소" (노드 자체는 존재)
console.log(parent.childNodes.length);       // 0

// 제거된 노드는 재사용 가능
document.body.appendChild(removed);          // 다시 삽입

// 예외: child가 parent의 자식이 아니면 NotFoundError DOMException
try {
  parent.removeChild(document.createElement('div')); // 자식이 아닌 노드
} catch (e) {
  console.log(e.name);                       // "NotFoundError"
}

// 실전: 모든 자식 제거 패턴
while (parent.firstChild) {
  parent.removeChild(parent.firstChild);
}
// 또는 더 간단하게:
parent.textContent = '';
// 또는 (modern):
parent.replaceChildren();
```

---

### 3. Document 인터페이스

Document 인터페이스: 웹 페이지 전체를 나타내는 진입점(entry point)

- HTML 문서에서 `document` 전역 객체가 바로 Document 인터페이스의 인스턴스 → DOM 트리의 루트 역할
- 요소 생성·검색·문서 메타데이터 접근 등 핵심 기능 제공

#### 3.1 Document 속성

##### 문서 메타데이터

```javascript
// URL / documentURI: 문서의 URL (동일한 값)
console.log(document.URL);                   // "https://example.com/page.html"
console.log(document.documentURI);           // "https://example.com/page.html"

// compatMode: 렌더링 모드
console.log(document.compatMode);
// "CSS1Compat" → 표준 모드 (Standards Mode)
// "BackCompat" → 쿼크 모드 (Quirks Mode)

// characterSet: 문서의 문자 인코딩
console.log(document.characterSet);          // "UTF-8"

// contentType: 문서의 MIME 타입
console.log(document.contentType);           // "text/html"
```

##### 문서 구조 접근

```javascript
// doctype: DOCTYPE 노드
console.log(document.doctype);               // <!DOCTYPE html>
console.log(document.doctype.name);          // "html"

// documentElement: 루트 요소 (HTML 문서에서는 <html>)
console.log(document.documentElement.tagName); // "HTML"

// head: <head> 요소
console.log(document.head.tagName);          // "HEAD"

// body: <body> 요소 (또는 <frameset>)
console.log(document.body.tagName);          // "BODY"

// title: 문서 제목 (읽기/쓰기)
console.log(document.title);                 // 현재 제목
document.title = '새 제목';                   // 제목 변경

// scrollingElement: 스크롤 가능한 루트 요소
// Standards Mode에서는 document.documentElement, Quirks Mode에서는 document.body
console.log(document.scrollingElement);      // <html> 또는 <body>
```

##### 상태 및 정보

```javascript
// referrer: 이전 페이지 URL (없으면 빈 문자열)
console.log(document.referrer);              // "https://google.com/search?q=..."

// domain: 문서의 도메인 (보안 목적으로 설정 가능했으나 현재 deprecated)
console.log(document.domain);                // "example.com"

// cookie: 문서의 쿠키 (읽기/쓰기)
console.log(document.cookie);                // "name=value; other=data"
document.cookie = 'newCookie=value; path=/'; // 쿠키 추가

// readyState: 문서 로딩 상태
// "loading"     → 문서 파싱 중
// "interactive" → 파싱 완료, 리소스 로딩 중 (DOMContentLoaded 직전)
// "complete"    → 모든 리소스 로딩 완료 (load 이벤트 직전)
console.log(document.readyState);

document.addEventListener('readystatechange', () => {
  console.log(document.readyState);
});

// lastModified: 문서의 마지막 수정 시간 (서버 응답 기반, 문자열)
console.log(document.lastModified);          // "02/12/2026 10:30:00"

// visibilityState / hidden: 탭 가시성
console.log(document.visibilityState);       // "visible" 또는 "hidden"
console.log(document.hidden);                // false 또는 true

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    // 탭이 비활성화됨 - 타이머 정지, 리소스 절약 등
    console.log('탭이 숨겨짐');
  } else {
    console.log('탭이 보임');
  }
});
```

##### 편집 및 포커스 관련

```javascript
// activeElement: 현재 포커스된 요소 (없으면 body 또는 null)
console.log(document.activeElement);         // <body> 또는 포커스된 <input> 등

// designMode: 전체 문서 편집 모드 ("on" 또는 "off")
document.designMode = 'on';                  // 전체 문서가 contenteditable처럼 동작
document.designMode = 'off';                 // 편집 모드 해제

// fullscreenElement: 전체 화면 모드인 요소 (없으면 null)
console.log(document.fullscreenElement);     // null 또는 전체 화면 요소

// currentScript: 현재 실행 중인 <script> 요소 (모듈이나 async에서는 null)
console.log(document.currentScript);         // <script> 또는 null
```

#### 3.2 Document 생성 메서드

##### 요소 생성

```javascript
// createElement(localName [, options]): 요소 생성
const div = document.createElement('div');
const p = document.createElement('p');

// HTML 문서에서는 태그명이 자동으로 소문자로 변환됨
const el = document.createElement('DIV');
console.log(el.tagName);                     // "DIV" (tagName은 항상 대문자)
console.log(el.localName);                   // "div"

// Custom Element 생성 (is 옵션)
// customElements.define('my-button', MyButton, { extends: 'button' });
const btn = document.createElement('button', { is: 'my-button' });

// createElementNS(namespace, qualifiedName [, options]): 네임스페이스 지정 생성
// SVG 요소 생성에 주로 사용
const svgNS = 'http://www.w3.org/2000/svg';
const svg = document.createElementNS(svgNS, 'svg');
const circle = document.createElementNS(svgNS, 'circle');
circle.setAttribute('cx', '50');
circle.setAttribute('cy', '50');
circle.setAttribute('r', '40');
svg.appendChild(circle);

// MathML 요소
const mathNS = 'http://www.w3.org/1998/Math/MathML';
const math = document.createElementNS(mathNS, 'math');
```

##### 기타 노드 생성

```javascript
// createDocumentFragment(): 문서 조각 생성
const fragment = document.createDocumentFragment();
for (let i = 0; i < 100; i++) {
  const li = document.createElement('li');
  li.textContent = `항목 ${i}`;
  fragment.appendChild(li);
}
document.getElementById('list').appendChild(fragment); // 한 번의 DOM 조작

// createTextNode(data): 텍스트 노드 생성
const text = document.createTextNode('안녕하세요');
document.body.appendChild(text);
// HTML 엔티티가 자동 이스케이프됨
const safe = document.createTextNode('<script>alert("xss")</script>');
// 렌더링 시 텍스트로 표시 (XSS 방지)

// createComment(data): 주석 노드 생성
const comment = document.createComment('이것은 주석입니다');
document.body.appendChild(comment);
// DOM에 <!-- 이것은 주석입니다 --> 추가

// createProcessingInstruction(target, data): XML 처리 명령 생성
// HTML 문서에서는 거의 사용하지 않음
const pi = document.createProcessingInstruction('xml-stylesheet',
  'href="style.xsl" type="text/xsl"');

// createAttribute(localName): 속성 노드 생성 (레거시 API)
const attr = document.createAttribute('data-value');
attr.value = '42';

// createAttributeNS(namespace, qualifiedName): 네임스페이스 지정 속성 생성
const xlinkNS = 'http://www.w3.org/1999/xlink';
const href = document.createAttributeNS(xlinkNS, 'xlink:href');
```

#### 3.3 Document 검색 메서드

```javascript
// getElementById(elementId): ID로 요소 검색 (단일 요소, 없으면 null)
const header = document.getElementById('header');
// 가장 빠른 검색 메서드 (해시맵 기반)
// 중복 ID가 있으면 트리 순서상 첫 번째 요소 반환

// getElementsByClassName(classNames): 클래스명으로 검색 (live HTMLCollection)
const items = document.getElementsByClassName('item');
console.log(items.length);
// 공백으로 구분하여 여러 클래스 동시 지정 가능 (AND 조건)
const activeItems = document.getElementsByClassName('item active');
// 주의: live collection이므로 DOM 변경 시 자동 갱신

// getElementsByTagName(qualifiedName): 태그명으로 검색 (live HTMLCollection)
const divs = document.getElementsByTagName('div');
const all = document.getElementsByTagName('*');  // 모든 요소
console.log(divs.length);

// getElementsByTagNameNS(namespace, localName): 네임스페이스 + 태그명
const svgElements = document.getElementsByTagNameNS(
  'http://www.w3.org/2000/svg', 'circle'
);

// querySelector(selectors): CSS 선택자로 첫 번째 요소 검색
const firstItem = document.querySelector('.list > .item:first-child');
const emailInput = document.querySelector('input[type="email"]');
const pseudo = document.querySelector(':not(.hidden)');

// querySelectorAll(selectors): CSS 선택자로 모든 요소 검색 (static NodeList)
const allItems = document.querySelectorAll('.item');
// 반환된 NodeList는 static (DOM 변경에 반응하지 않음)
allItems.forEach((item, index) => {
  console.log(index, item.textContent);
});

// 복잡한 선택자 사용 가능
const complex = document.querySelectorAll(
  'article > section:nth-child(2) p.intro, article > section:last-child p.outro'
);

// querySelector/querySelectorAll의 선택자 범위 주의사항
const container = document.getElementById('container');
// 다음은 container 내부에서만 검색하지만,
// 선택자 자체는 문서 전체 기준으로 해석됨
const result = container.querySelectorAll('div > p');
// container 자신이 div이면 그 직접 자식 p도 포함됨

// :scope 의사 클래스로 범위를 명시적으로 지정
const scoped = container.querySelectorAll(':scope > p');
// container의 직접 자식 p만 선택
```

#### 3.4 Document 기타 메서드

```javascript
// importNode(node, deep): 다른 문서의 노드를 이 문서로 가져옴
// 가져온 노드의 ownerDocument가 현재 문서로 변경됨
const iframe = document.querySelector('iframe');
const foreignNode = iframe.contentDocument.querySelector('.widget');
const imported = document.importNode(foreignNode, true);
document.body.appendChild(imported);

// adoptNode(node): 다른 문서의 노드를 이 문서로 양자(adopt)
// importNode와 달리 복제하지 않고 원본 자체를 이동
const adopted = document.adoptNode(foreignNode);
// foreignNode는 원래 문서에서 제거되고, 현재 문서의 노드가 됨
document.body.appendChild(adopted);

// importNode vs adoptNode 비교:
// importNode: 복제본 생성 → 원본 유지, 복제본의 ownerDocument 변경
// adoptNode:  원본 이동 → 원본의 ownerDocument 변경, 원래 문서에서 제거

// createRange(): Range 객체 생성
const range = document.createRange();
range.selectNodeContents(document.body);
const text2 = range.toString(); // body의 텍스트 내용

// createNodeIterator(root, whatToShow, filter): NodeIterator 생성
const iterator = document.createNodeIterator(
  document.body,
  NodeFilter.SHOW_ELEMENT,   // 요소 노드만
  {
    acceptNode(node) {
      return node.tagName === 'P'
        ? NodeFilter.FILTER_ACCEPT
        : NodeFilter.FILTER_SKIP;
    }
  }
);
let currentNode;
while (currentNode = iterator.nextNode()) {
  console.log(currentNode.textContent);
}

// createTreeWalker(root, whatToShow, filter): TreeWalker 생성
const walker = document.createTreeWalker(
  document.body,
  NodeFilter.SHOW_TEXT,      // 텍스트 노드만
  null                       // 필터 없음
);
while (walker.nextNode()) {
  if (walker.currentNode.data.trim()) {
    console.log(walker.currentNode.data);
  }
}
```

#### 3.5 DOMImplementation

```javascript
// document.implementation으로 접근
const impl = document.implementation;

// createHTMLDocument(title): 새 HTML 문서 생성
const newDoc = impl.createHTMLDocument('새 문서');
console.log(newDoc.title);                   // "새 문서"
console.log(newDoc.documentElement.outerHTML);
// <html><head><title>새 문서</title></head><body></body></html>

// 실전: innerHTML의 안전한 파싱
function sanitizeHTML(html) {
  const doc = document.implementation.createHTMLDocument('');
  doc.body.innerHTML = html;
  // doc에서 안전하지 않은 요소 제거 후 반환
  doc.querySelectorAll('script, iframe, object, embed').forEach(el => el.remove());
  return doc.body.innerHTML;
}

// createDocument(namespace, qualifiedName, doctype): XML 문서 생성
const xmlDoc = impl.createDocument(null, 'root', null);
console.log(xmlDoc.documentElement.tagName); // "root"

// createDocumentType(qualifiedName, publicId, systemId): DocumentType 생성
const doctype = impl.createDocumentType('html', '', '');

// hasFeature(): 항상 true 반환 (레거시, 더 이상 사용하지 말 것)
console.log(impl.hasFeature());              // true
```

---

### 4. Element 인터페이스

Element 인터페이스: DOM 트리에서 요소를 나타내는 인터페이스

- HTML 요소(`<div>`, `<p>`, `<span>` 등)·SVG 요소(`<svg>`, `<circle>` 등)·사용자 정의 요소(Custom Elements)가 모두 Element를 상속
- 속성(attribute) 조작·DOM 탐색·기하학적 정보 접근·Shadow DOM 등 광범위한 기능 제공

#### 4.1 Element 기본 속성

```javascript
const div = document.createElement('div');
document.body.appendChild(div);

// namespaceURI: 요소의 네임스페이스 URI
console.log(div.namespaceURI);               // "http://www.w3.org/1999/xhtml" (HTML 요소)
const svgEl = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
console.log(svgEl.namespaceURI);             // "http://www.w3.org/2000/svg"

// prefix: 네임스페이스 접두사 (대부분 null)
console.log(div.prefix);                     // null

// localName: 로컬 이름 (소문자)
console.log(div.localName);                  // "div"

// tagName: 태그 이름 (HTML 요소는 대문자, SVG 등은 원래 대소문자 유지)
console.log(div.tagName);                    // "DIV"
console.log(svgEl.tagName);                  // "rect" (SVG는 대소문자 유지)

// id: 요소의 고유 식별자
div.id = 'myDiv';
console.log(div.id);                         // "myDiv"

// className: class 속성 값 (문자열)
div.className = 'container main';
console.log(div.className);                  // "container main"
```

##### classList (DOMTokenList)

```javascript
// classList: 클래스 목록을 DOMTokenList로 반환 (class 속성을 토큰 단위로 관리)
const el = document.createElement('div');
el.className = 'foo bar';

console.log(el.classList.length);            // 2
console.log(el.classList[0]);                // "foo"
console.log(el.classList[1]);                // "bar"

// contains: 클래스 포함 여부
console.log(el.classList.contains('foo'));    // true
console.log(el.classList.contains('baz'));    // false

// add: 클래스 추가 (중복 시 무시)
el.classList.add('baz');
el.classList.add('qux', 'quux');             // 여러 개 동시 추가
console.log(el.className);                  // "foo bar baz qux quux"

// remove: 클래스 제거 (없으면 무시)
el.classList.remove('bar', 'qux');
console.log(el.className);                  // "foo baz quux"

// toggle: 있으면 제거, 없으면 추가 (force 매개변수로 강제 지정)
el.classList.toggle('active');               // 추가 → true 반환
el.classList.toggle('active');               // 제거 → false 반환
el.classList.toggle('visible', true);        // 강제 추가 (항상 true)
el.classList.toggle('visible', false);       // 강제 제거 (항상 false)
// force가 true면 add와 동일, false면 remove와 동일

// replace: 클래스 교체
el.classList.replace('foo', 'new-foo');      // "foo"를 "new-foo"로 교체
// 성공하면 true, 원래 클래스가 없으면 false

// value: 전체 클래스 문자열 (className과 동일)
console.log(el.classList.value);             // "new-foo baz quux"

// 순회 지원
el.classList.forEach((cls) => {
  console.log(cls);
});
for (const cls of el.classList) {
  console.log(cls);
}
```

##### attributes (NamedNodeMap)

```javascript
// attributes: 요소의 모든 속성을 NamedNodeMap으로 반환
const link = document.createElement('a');
link.href = 'https://example.com';
link.target = '_blank';
link.rel = 'noopener';

console.log(link.attributes.length);         // 3
console.log(link.attributes[0].name);        // "href"
console.log(link.attributes[0].value);       // "https://example.com"

// slot: 슬롯 이름 (Shadow DOM에서 사용)
const slotted = document.createElement('span');
slotted.slot = 'header';
console.log(slotted.slot);                  // "header"
```

#### 4.2 속성(Attribute) 조작

```javascript
const el = document.createElement('div');

// setAttribute(qualifiedName, value): 속성 설정
el.setAttribute('data-id', '42');
el.setAttribute('class', 'container');
el.setAttribute('aria-label', '메인 컨테이너');
// 값은 항상 문자열로 변환됨
el.setAttribute('data-count', 100);          // "100" (문자열)

// getAttribute(qualifiedName): 속성 값 읽기 (없으면 null)
console.log(el.getAttribute('data-id'));     // "42"
console.log(el.getAttribute('nonexistent')); // null

// hasAttribute(qualifiedName): 속성 존재 여부
console.log(el.hasAttribute('data-id'));     // true
console.log(el.hasAttribute('id'));          // false

// hasAttributes(): 속성이 하나라도 있는지 확인
console.log(el.hasAttributes());             // true
const empty = document.createElement('div');
console.log(empty.hasAttributes());          // false

// removeAttribute(qualifiedName): 속성 제거 (없어도 오류 없음)
el.removeAttribute('data-id');
console.log(el.hasAttribute('data-id'));     // false

// toggleAttribute(qualifiedName [, force]): 불리언 속성 토글
const input = document.createElement('input');
input.toggleAttribute('disabled');           // disabled 추가
console.log(input.disabled);                // true
input.toggleAttribute('disabled');           // disabled 제거
console.log(input.disabled);                // false
input.toggleAttribute('readonly', true);     // 강제 추가
input.toggleAttribute('readonly', false);    // 강제 제거

// getAttributeNames(): 모든 속성 이름을 배열로 반환
el.setAttribute('id', 'test');
el.setAttribute('class', 'box');
el.setAttribute('data-type', 'main');
console.log(el.getAttributeNames());         // ["class", "aria-label", "id", "data-type"]
// 순서는 설정된 순서대로 (class는 이전에 설정)
```

##### 네임스페이스 지정 속성 조작

```javascript
// SVG의 xlink:href 같은 네임스페이스가 있는 속성 처리
const svgNS = 'http://www.w3.org/2000/svg';
const xlinkNS = 'http://www.w3.org/1999/xlink';

const use = document.createElementNS(svgNS, 'use');

// setAttributeNS(namespace, qualifiedName, value)
use.setAttributeNS(xlinkNS, 'xlink:href', '#icon');

// getAttributeNS(namespace, localName)
console.log(use.getAttributeNS(xlinkNS, 'href'));     // "#icon"

// removeAttributeNS(namespace, localName)
use.removeAttributeNS(xlinkNS, 'href');

// 참고: 현대 SVG에서는 xlink:href 대신 href를 직접 사용하는 것이 권장됨
use.setAttribute('href', '#icon');
```

#### 4.3 DOM 탐색 메서드

##### closest(selectors)

```javascript
// 자기 자신부터 시작하여 조상 방향으로 CSS 선택자에 매칭되는 첫 번째 요소 반환
// <div class="outer">
//   <div class="inner">
//     <button id="btn">클릭</button>
//   </div>
// </div>

const btn = document.getElementById('btn');

console.log(btn.closest('.inner'));          // <div class="inner">
console.log(btn.closest('.outer'));          // <div class="outer">
console.log(btn.closest('button'));          // <button> (자기 자신)
console.log(btn.closest('.nonexistent'));    // null

// 실전: 이벤트 위임에서의 활용
document.addEventListener('click', (e) => {
  const row = e.target.closest('tr[data-id]');
  if (row) {
    const id = row.dataset.id;
    console.log(`행 ${id} 클릭됨`);
  }
});
```

##### matches(selectors)

```javascript
// 요소가 CSS 선택자에 매칭되는지 확인
const el = document.createElement('div');
el.className = 'foo bar';
el.id = 'test';

console.log(el.matches('.foo'));             // true
console.log(el.matches('#test'));            // true
console.log(el.matches('div.foo.bar'));      // true
console.log(el.matches('.baz'));             // false
console.log(el.matches(':not(.baz)'));       // true

// 실전: 필터링
const items = document.querySelectorAll('.item');
const activeItems = [...items].filter(item => item.matches('.active:not(.disabled)'));
```

##### getElementsByClassName / getElementsByTagName / getElementsByTagNameNS

```javascript
// Element에서도 호출 가능 (Document에서의 호출과 동일하지만 범위가 해당 요소의 자손으로 한정)
const container = document.getElementById('container');

// container 내부의 .item 요소만 검색 (live HTMLCollection)
const items = container.getElementsByClassName('item');

// container 내부의 <p> 요소만 검색
const paragraphs = container.getElementsByTagName('p');

// 네임스페이스 지정
const svgCircles = container.getElementsByTagNameNS(
  'http://www.w3.org/2000/svg', 'circle'
);
```

#### 4.4 DOM 조작

##### innerHTML / outerHTML

```javascript
const div = document.createElement('div');

// innerHTML: 요소의 HTML 내용을 문자열로 읽기/쓰기
div.innerHTML = '<p>Hello <strong>World</strong></p>';
console.log(div.innerHTML);                  // '<p>Hello <strong>World</strong></p>'
console.log(div.childNodes.length);          // 1 (p 요소)

// innerHTML 설정 시 기존 자식은 모두 제거되고 파싱 결과로 교체됨
div.innerHTML = '<span>새 내용</span>';

// innerHTML로 스크립트 삽입 시 실행되지 않음 (보안)
div.innerHTML = '<script>alert("xss")</script>';
// <script> 요소가 생성되지만 실행되지 않음

// outerHTML: 요소 자신을 포함한 HTML 문자열
console.log(div.outerHTML);                  // '<div><span>새 내용</span></div>'

// outerHTML 설정 시 요소 자신이 교체됨
// 주의: 설정 후 원래 변수는 더 이상 DOM에 연결되지 않음
const target = document.getElementById('target');
const ref = target;
target.outerHTML = '<span>교체됨</span>';
// ref는 여전히 원래 <div id="target">을 참조하지만, 이 노드는 DOM에서 제거됨
console.log(ref.isConnected);               // false
```

##### insertAdjacentHTML / insertAdjacentElement / insertAdjacentText

```javascript
// insertAdjacentHTML(position, text): 특정 위치에 HTML 문자열 삽입
// position 값:
// "beforebegin" → 요소 바로 앞 (형제로)
// "afterbegin"  → 요소 내부 맨 앞 (첫 번째 자식으로)
// "beforeend"   → 요소 내부 맨 뒤 (마지막 자식으로)
// "afterend"    → 요소 바로 뒤 (형제로)

const el = document.getElementById('target');

el.insertAdjacentHTML('beforebegin', '<div>앞에 삽입</div>');
el.insertAdjacentHTML('afterbegin', '<span>내부 앞</span>');
el.insertAdjacentHTML('beforeend', '<span>내부 뒤</span>');
el.insertAdjacentHTML('afterend', '<div>뒤에 삽입</div>');

// 결과:
// <div>앞에 삽입</div>
// <div id="target">
//   <span>내부 앞</span>
//   (기존 내용)
//   <span>내부 뒤</span>
// </div>
// <div>뒤에 삽입</div>

// innerHTML과 달리 기존 내용을 파괴하지 않으므로 성능과 안전성이 더 좋음

// insertAdjacentElement(position, element): 요소 노드 삽입
const newEl = document.createElement('p');
newEl.textContent = '새 문단';
el.insertAdjacentElement('afterend', newEl);
// 반환값: 삽입된 요소 (삽입 실패 시 null)

// insertAdjacentText(position, text): 텍스트 노드 삽입
el.insertAdjacentText('beforeend', '추가 텍스트');
// innerHTML과 달리 HTML이 아닌 순수 텍스트로 삽입됨 (XSS 안전)
```

#### 4.5 기하학적 속성 및 메서드

```javascript
// getBoundingClientRect(): 요소의 뷰포트 기준 위치와 크기
const el = document.getElementById('box');
const rect = el.getBoundingClientRect();

console.log(rect.x);       // 왼쪽 x 좌표 (뷰포트 기준)
console.log(rect.y);       // 위쪽 y 좌표 (뷰포트 기준)
console.log(rect.width);   // 요소 너비 (border 포함)
console.log(rect.height);  // 요소 높이 (border 포함)
console.log(rect.top);     // = y
console.log(rect.right);   // = x + width
console.log(rect.bottom);  // = y + height
console.log(rect.left);    // = x

// 스크롤 위치를 고려한 절대 위치 계산
const absoluteTop = rect.top + window.scrollY;
const absoluteLeft = rect.left + window.scrollX;

// getClientRects(): 인라인 요소의 각 줄에 대한 DOMRect 목록
const span = document.querySelector('span.multiline');
const rects = span.getClientRects();
// 인라인 요소가 여러 줄에 걸쳐 있으면 각 줄마다 DOMRect 반환
for (const r of rects) {
  console.log(r.top, r.left, r.width, r.height);
}
```

##### 클라이언트 / 스크롤 속성

```javascript
// client* 속성: border 안쪽 영역 정보
const el = document.getElementById('scrollable');

// clientTop / clientLeft: 테두리(border)의 너비
console.log(el.clientTop);                   // 상단 border 너비 (px)
console.log(el.clientLeft);                  // 좌측 border 너비 (px)

// clientWidth / clientHeight: padding 포함, border/scrollbar 제외 크기
console.log(el.clientWidth);                 // 사용 가능한 내부 너비
console.log(el.clientHeight);                // 사용 가능한 내부 높이

// scroll* 속성: 스크롤 관련
console.log(el.scrollTop);                   // 수직 스크롤 위치
console.log(el.scrollLeft);                  // 수평 스크롤 위치
console.log(el.scrollWidth);                 // 전체 콘텐츠 너비 (스크롤 포함)
console.log(el.scrollHeight);                // 전체 콘텐츠 높이 (스크롤 포함)

// scrollTop / scrollLeft는 쓰기 가능
el.scrollTop = 0;                            // 맨 위로 스크롤
el.scrollLeft = 100;                         // 오른쪽으로 100px 스크롤
```

##### scrollIntoView(arg)

```javascript
// 요소를 뷰포트 내로 스크롤
const target = document.getElementById('target');

// 불리언 인자
target.scrollIntoView(true);                 // 요소 상단을 뷰포트 상단에 맞춤 (기본값)
target.scrollIntoView(false);                // 요소 하단을 뷰포트 하단에 맞춤

// 옵션 객체
target.scrollIntoView({
  behavior: 'smooth',                       // 부드러운 스크롤 애니메이션 ("auto" | "smooth")
  block: 'center',                          // 수직 정렬 ("start" | "center" | "end" | "nearest")
  inline: 'nearest'                         // 수평 정렬 ("start" | "center" | "end" | "nearest")
});

// 실전: 폼 유효성 검사 실패 시 해당 필드로 스크롤
function scrollToFirstError() {
  const firstError = document.querySelector('.error');
  if (firstError) {
    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
    firstError.focus();
  }
}
```

#### 4.6 Shadow DOM

```javascript
// attachShadow(init): Shadow Root 생성
// init.mode: "open" (외부에서 shadowRoot 접근 가능) 또는 "closed" (접근 불가)
const host = document.createElement('div');
document.body.appendChild(host);

const shadow = host.attachShadow({ mode: 'open' });

// Shadow Root에 DOM 구성
shadow.innerHTML = `
  <style>
    p { color: red; }           /* Shadow DOM 내부에만 적용 */
    ::slotted(span) { color: blue; }  /* 슬롯에 투영된 요소에 적용 */
  </style>
  <p>Shadow DOM 내부 텍스트</p>
  <slot></slot>                  <!-- 외부 콘텐츠가 투영되는 슬롯 -->
`;

// 외부 콘텐츠 (Light DOM)
host.innerHTML = '<span>이 텍스트는 슬롯으로 투영됨</span>';

// shadowRoot: Shadow Root 접근 (mode: "open"인 경우에만)
console.log(host.shadowRoot);                // ShadowRoot 객체
console.log(host.shadowRoot.innerHTML);      // Shadow DOM의 HTML 내용

// mode: "closed"인 경우
const closedHost = document.createElement('div');
const closedShadow = closedHost.attachShadow({ mode: 'closed' });
console.log(closedHost.shadowRoot);          // null (접근 불가)
// closedShadow 참조를 통해서만 접근 가능

// assignedSlot: 요소가 할당된 슬롯
const slottedSpan = host.querySelector('span');
console.log(slottedSpan.assignedSlot);       // <slot> 요소

// attachShadow를 지원하는 요소:
// article, aside, blockquote, body, div, footer, h1~h6, header, main,
// nav, p, section, span + Custom Elements
// 그 외 요소에서 호출하면 NotSupportedError DOMException 발생

// 이미 Shadow Root가 있는 요소에 다시 attachShadow 호출 시
// NotSupportedError DOMException 발생

// slotchange 이벤트: 슬롯의 분배 노드가 변경될 때
const slot = shadow.querySelector('slot');
slot.addEventListener('slotchange', () => {
  console.log('슬롯 할당 변경됨');
  console.log(slot.assignedNodes());         // 할당된 노드 목록
  console.log(slot.assignedElements());      // 할당된 요소 목록 (Text 노드 제외)
});
```

---

### 5. Attr 인터페이스

Attr 인터페이스: 요소의 속성을 나타냄

- 현재 Living Standard의 IDL에서도 `interface Attr : Node`로 여전히 Node를 상속 → nodeType은 하위 호환성을 위해 2(ATTRIBUTE_NODE) 반환
- Attr을 Node에서 분리하자는 제안(whatwg/dom #102) 논의된 바 있으나, 병합되지 않은 오픈 이슈일 뿐 스펙에는 미반영

```javascript
// Attr 객체 얻기
const el = document.createElement('div');
el.setAttribute('id', 'test');
el.setAttribute('class', 'main container');
el.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', '#icon');

// attributes 컬렉션에서 접근
const idAttr = el.attributes[0];               // 또는 el.attributes.getNamedItem('id')
console.log(idAttr instanceof Attr);           // true

// Attr 속성
console.log(idAttr.namespaceURI);              // null (일반 HTML 속성)
console.log(idAttr.prefix);                    // null
console.log(idAttr.localName);                 // "id"
console.log(idAttr.name);                      // "id" (= prefix:localName 또는 localName)
console.log(idAttr.value);                     // "test"
console.log(idAttr.ownerElement);              // <div> 요소
console.log(idAttr.specified);                 // true (항상 true, 레거시 호환)

// 네임스페이스가 있는 속성
const xlinkAttr = el.attributes.getNamedItemNS(
  'http://www.w3.org/1999/xlink', 'href'
);
console.log(xlinkAttr.namespaceURI);           // "http://www.w3.org/1999/xlink"
console.log(xlinkAttr.prefix);                 // "xlink"
console.log(xlinkAttr.localName);              // "href"
console.log(xlinkAttr.name);                   // "xlink:href"

// Attr의 value 쓰기
idAttr.value = 'newId';
console.log(el.id);                           // "newId" (요소에도 반영)

// getAttributeNode / setAttributeNode (레거시이지만 여전히 지원)
const attr = document.createAttribute('data-custom');
attr.value = 'hello';
el.setAttributeNode(attr);
console.log(el.getAttribute('data-custom'));   // "hello"

const retrieved = el.getAttributeNode('data-custom');
console.log(retrieved === attr);              // true
console.log(retrieved.ownerElement === el);   // true
```

---

### 6. CharacterData 인터페이스

CharacterData: Text·Comment·CDATASection·ProcessingInstruction의 공통 부모 인터페이스

- 문자 데이터를 가진 노드의 공통 속성·메서드 정의

#### 6.1 CharacterData 속성과 메서드

```javascript
// CharacterData를 직접 생성할 수는 없으며, 하위 타입을 통해 사용
const text = document.createTextNode('Hello World');

// data: 문자 데이터 (= nodeValue)
console.log(text.data);                      // "Hello World"
text.data = 'New Data';
console.log(text.data);                      // "New Data"

// length: 문자 수
console.log(text.length);                    // 8

// substringData(offset, count): 부분 문자열
text.data = 'Hello World';
console.log(text.substringData(0, 5));       // "Hello"
console.log(text.substringData(6, 5));       // "World"
// offset이 length보다 크면 IndexSizeError DOMException

// appendData(data): 끝에 문자열 추가
text.appendData('!!!');
console.log(text.data);                      // "Hello World!!!"

// insertData(offset, data): 특정 위치에 문자열 삽입
text.data = 'Hello World';
text.insertData(5, ' Beautiful');
console.log(text.data);                      // "Hello Beautiful World"

// deleteData(offset, count): 특정 범위 문자 삭제
text.deleteData(5, 10);                      // " Beautiful" 삭제
console.log(text.data);                      // "Hello World"

// replaceData(offset, count, data): 특정 범위를 새 문자열로 교체
text.replaceData(6, 5, 'DOM');
console.log(text.data);                      // "Hello DOM"
```

#### 6.2 Text 노드

```javascript
// Text 노드: 요소 사이의 텍스트 콘텐츠
const text = document.createTextNode('Hello World');

// wholeText: 인접한 Text 노드들의 연결된 텍스트
const div = document.createElement('div');
div.appendChild(document.createTextNode('Hello '));
div.appendChild(document.createTextNode('World'));
// normalize 전이므로 두 개의 Text 노드
console.log(div.firstChild.wholeText);       // "Hello World"
// wholeText는 normalize하지 않고 인접 Text 노드의 data를 합쳐서 반환

// splitText(offset): Text 노드를 둘로 분할
const original = document.createTextNode('Hello World');
div.textContent = '';
div.appendChild(original);

const remainder = original.splitText(5);
// original.data = "Hello", remainder.data = " World"
console.log(original.data);                 // "Hello"
console.log(remainder.data);                // " World"
console.log(div.childNodes.length);          // 2 (두 개의 Text 노드)
console.log(original.nextSibling === remainder); // true

// assignedSlot: Shadow DOM 슬롯에 할당된 경우 해당 <slot> 요소
// (Text 노드도 슬롯에 할당될 수 있음)
```

#### 6.3 Comment 노드

```javascript
// Comment: HTML/XML 주석
const comment = document.createComment('이것은 주석입니다');
console.log(comment.nodeType);               // 8 (COMMENT_NODE)
console.log(comment.nodeName);               // "#comment"
console.log(comment.data);                   // "이것은 주석입니다"

document.body.appendChild(comment);
// DOM에 <!-- 이것은 주석입니다 --> 삽입

// Comment는 CharacterData를 상속하므로 같은 메서드 사용 가능
comment.appendData(' - 추가 정보');
console.log(comment.data);                  // "이것은 주석입니다 - 추가 정보"

// 주석 노드 탐색
const walker = document.createTreeWalker(
  document.body,
  NodeFilter.SHOW_COMMENT
);
while (walker.nextNode()) {
  console.log('주석 발견:', walker.currentNode.data);
}
```

#### 6.4 CDATASection / ProcessingInstruction

```javascript
// CDATASection: XML 문서에서만 사용 (HTML에서는 사용 불가)
// XML 파서가 해석하지 않는 텍스트 블록
// <![CDATA[ 여기의 내용은 파싱되지 않음 <tag> & ]]>
// HTML 문서에서 createCDATASection 호출 시 NotSupportedError

// ProcessingInstruction: XML 처리 명령
// HTML 문서에서도 생성 가능하지만 주로 XML에서 사용
const pi = document.createProcessingInstruction(
  'xml-stylesheet',
  'href="style.css" type="text/css"'
);
console.log(pi.target);                     // "xml-stylesheet"
console.log(pi.data);                       // 'href="style.css" type="text/css"'
console.log(pi.nodeType);                   // 7 (PROCESSING_INSTRUCTION_NODE)

// target 속성은 읽기 전용
// data 속성은 CharacterData에서 상속받은 것으로 읽기/쓰기 가능
```

---

### 7. DocumentFragment 인터페이스

DocumentFragment: 부모가 없는 최소한의 문서 객체 → DOM 트리의 경량 버전

- 여러 노드를 그룹화하여 한 번에 삽입할 때 사용
- Document와 유사하게 동작하지만 활성 문서 트리의 일부가 아님 → Fragment에 대한 변경이 실제 DOM에 영향을 주지 않고 리플로우(reflow)도 발생시키지 않음

#### 7.1 생성 및 기본 사용

```javascript
// 생성 방법 1: document.createDocumentFragment()
const frag = document.createDocumentFragment();

// 생성 방법 2: 생성자 (일부 브라우저)
const frag2 = new DocumentFragment();

// 기본 속성
console.log(frag.nodeType);                  // 11 (DOCUMENT_FRAGMENT_NODE)
console.log(frag.nodeName);                  // "#document-fragment"
console.log(frag.ownerDocument);             // document
console.log(frag.parentNode);                // null (항상)
console.log(frag.parentElement);             // null (항상)
```

#### 7.2 성능 최적화 활용

```javascript
// 비효율적인 방식: 매번 실제 DOM에 삽입 (N번의 리플로우 가능)
const list = document.getElementById('list');
for (let i = 0; i < 1000; i++) {
  const li = document.createElement('li');
  li.textContent = `항목 ${i}`;
  list.appendChild(li);                      // 매번 DOM 수정 → 잠재적 리플로우
}

// 효율적인 방식: DocumentFragment 사용 (1번의 리플로우)
const frag = document.createDocumentFragment();
for (let i = 0; i < 1000; i++) {
  const li = document.createElement('li');
  li.textContent = `항목 ${i}`;
  frag.appendChild(li);                      // Fragment에 추가 (리플로우 없음)
}
list.appendChild(frag);                      // 한 번에 삽입 (1번의 리플로우)
// 삽입 후 frag는 비워짐 (자식 노드가 모두 list로 이동)
console.log(frag.childNodes.length);         // 0

// querySelector / querySelectorAll도 사용 가능
const frag3 = document.createDocumentFragment();
const div = document.createElement('div');
div.className = 'item';
frag3.appendChild(div);
console.log(frag3.querySelector('.item'));    // <div class="item">
```

#### 7.3 template 요소와의 관계

```javascript
// <template> 요소의 content 속성은 DocumentFragment를 반환
// <template id="card-template">
//   <div class="card">
//     <h2 class="card-title"></h2>
//     <p class="card-body"></p>
//   </div>
// </template>

const template = document.getElementById('card-template');
console.log(template.content instanceof DocumentFragment); // true

// template의 content는 "비활성(inert)" 상태
// 이미지가 로드되지 않고, 스크립트가 실행되지 않음

function createCard(title, body) {
  const clone = template.content.cloneNode(true);  // 깊은 복제
  clone.querySelector('.card-title').textContent = title;
  clone.querySelector('.card-body').textContent = body;
  return clone;
}

const container = document.getElementById('cards');
container.appendChild(createCard('제목 1', '본문 1'));
container.appendChild(createCard('제목 2', '본문 2'));

// template.content.cloneNode(true)를 사용하는 이유:
// template.content를 직접 appendChild하면 content가 비워지므로
// 재사용이 불가능하다. 항상 cloneNode(true)로 복제본을 사용해야 한다.
```

---

### 8. DOM Collections

DOM API: 여러 종류의 컬렉션을 반환

- 컬렉션 종류에 따라 live/static 여부·사용 가능한 메서드·순회 방식이 다름 → 정확한 이해 필요

#### 8.1 NodeList

NodeList: 노드의 순서가 있는 컬렉션

- live와 static 두 가지 변형 존재

```javascript
// Live NodeList: DOM 변경이 자동 반영됨
// childNodes가 대표적인 live NodeList
const parent = document.createElement('div');
const liveList = parent.childNodes;           // live NodeList

console.log(liveList.length);                // 0
parent.appendChild(document.createElement('span'));
console.log(liveList.length);                // 1 (자동 갱신!)
parent.appendChild(document.createElement('p'));
console.log(liveList.length);                // 2

// Static NodeList: DOM 변경이 반영되지 않음 (스냅샷)
// querySelectorAll이 대표적인 static NodeList
document.body.innerHTML = '<p>1</p><p>2</p>';
const staticList = document.querySelectorAll('p');

console.log(staticList.length);              // 2
document.body.appendChild(document.createElement('p'));
console.log(staticList.length);              // 2 (변경 미반영!)

// NodeList 속성 및 메서드
console.log(staticList.length);              // 항목 수

// item(index): 인덱스로 접근 (대괄호 표기와 동일)
console.log(staticList.item(0));             // 첫 번째 <p>
console.log(staticList[0]);                  // 동일
console.log(staticList.item(99));            // null (범위 초과)

// forEach(): 각 노드에 대해 콜백 실행
staticList.forEach((node, index, list) => {
  console.log(index, node.textContent);
});

// entries(): [index, node] 이터레이터
for (const [index, node] of staticList.entries()) {
  console.log(index, node);
}

// keys(): 인덱스 이터레이터
for (const index of staticList.keys()) {
  console.log(index);                       // 0, 1
}

// values(): 노드 이터레이터
for (const node of staticList.values()) {
  console.log(node);
}

// Symbol.iterator 지원 → for...of 직접 사용 가능
for (const node of staticList) {
  console.log(node.textContent);
}

// 배열로 변환
const arr1 = Array.from(staticList);
const arr2 = [...staticList];
```

#### 8.2 HTMLCollection

HTMLCollection: 항상 live인 요소 컬렉션

- getElementsByClassName, getElementsByTagName, children 등이 반환

```javascript
// HTMLCollection은 항상 live
document.body.innerHTML = '<div class="item">1</div><div class="item">2</div>';
const collection = document.getElementsByClassName('item');

console.log(collection.length);              // 2

// 새 요소 추가
const newDiv = document.createElement('div');
newDiv.className = 'item';
document.body.appendChild(newDiv);
console.log(collection.length);              // 3 (자동 갱신!)

// item(index): 인덱스로 접근
console.log(collection.item(0));             // 첫 번째 .item
console.log(collection[0]);                  // 동일

// namedItem(name): id 또는 name 속성으로 접근
document.body.innerHTML = `
  <form id="myForm">
    <input name="email" type="email">
    <input name="password" type="password">
  </form>
`;
const form = document.getElementById('myForm');
const inputs = form.getElementsByTagName('input');
// namedItem은 id 속성을 먼저 확인, 없으면 name 속성 확인
console.log(inputs.namedItem('email'));      // <input name="email">

// 주의: HTMLCollection은 forEach를 직접 지원하지 않음!
// collection.forEach(...); // TypeError!

// 순회 방법:
// 1. for 루프
for (let i = 0; i < collection.length; i++) {
  console.log(collection[i]);
}

// 2. Array.from 후 forEach
Array.from(collection).forEach(el => console.log(el));

// 3. 스프레드 연산자
[...collection].forEach(el => console.log(el));

// 4. for...of (Symbol.iterator 지원)
for (const el of collection) {
  console.log(el);
}

// Live collection 순회 시 주의사항:
// DOM을 수정하면서 live collection을 순회하면 예기치 않은 결과 발생
const divs = document.getElementsByTagName('div');
// 잘못된 예: 순회 중 요소 제거
// for (let i = 0; i < divs.length; i++) {
//   divs[i].remove(); // length가 줄어들어 일부 요소를 건너뜀!
// }

// 올바른 방법 1: 역순 순회
for (let i = divs.length - 1; i >= 0; i--) {
  divs[i].remove();
}

// 올바른 방법 2: 배열로 변환 후 순회
[...divs].forEach(div => div.remove());
```

#### 8.3 NamedNodeMap

NamedNodeMap: Attr 노드의 컬렉션 → Element의 `attributes` 속성이 반환, 항상 live

```javascript
const el = document.createElement('div');
el.setAttribute('id', 'test');
el.setAttribute('class', 'main');
el.setAttribute('data-value', '42');

const attrs = el.attributes;                  // NamedNodeMap

// length: 속성 수
console.log(attrs.length);                   // 3

// item(index): 인덱스로 Attr 접근
console.log(attrs.item(0).name);             // "id"
console.log(attrs[0].name);                  // "id" (동일)

// getNamedItem(qualifiedName): 이름으로 Attr 접근
const idAttr = attrs.getNamedItem('id');
console.log(idAttr.value);                   // "test"

// setNamedItem(attr): Attr 설정 (기존 동명 속성 교체)
const newAttr = document.createAttribute('title');
newAttr.value = '제목';
const oldAttr = attrs.setNamedItem(newAttr);  // 반환값: 교체된 기존 Attr 또는 null

// removeNamedItem(qualifiedName): 속성 제거
const removed = attrs.removeNamedItem('data-value');
console.log(removed.value);                 // "42"
console.log(attrs.length);                   // 3 (id, class, title)

// 네임스페이스 버전
// getNamedItemNS(namespace, localName)
// setNamedItemNS(attr)
// removeNamedItemNS(namespace, localName)

// 순회
for (let i = 0; i < attrs.length; i++) {
  console.log(`${attrs[i].name} = ${attrs[i].value}`);
}
```

#### 8.4 DOMTokenList

DOMTokenList: 공백으로 구분된 토큰 집합

- Element의 `classList`가 대표적, `relList` 등에서도 사용

```javascript
const el = document.createElement('a');
el.className = 'btn btn-primary active';
el.rel = 'noopener noreferrer';

const classList = el.classList;               // DOMTokenList
const relList = el.relList;                   // DOMTokenList

// length: 토큰 수
console.log(classList.length);                // 3

// item(index): 인덱스로 접근
console.log(classList.item(0));               // "btn"
console.log(classList[0]);                    // "btn"

// contains(token): 토큰 포함 여부
console.log(classList.contains('active'));     // true
console.log(classList.contains('hidden'));     // false

// add(...tokens): 토큰 추가 (중복 무시)
classList.add('visible', 'rounded');
console.log(el.className);                   // "btn btn-primary active visible rounded"

// remove(...tokens): 토큰 제거 (없으면 무시)
classList.remove('active', 'rounded');
console.log(el.className);                   // "btn btn-primary visible"

// toggle(token [, force]): 토글
classList.toggle('active');                   // 없으므로 추가 → true
classList.toggle('active');                   // 있으므로 제거 → false

// replace(token, newToken): 교체
classList.replace('btn-primary', 'btn-secondary');  // true (성공)
classList.replace('nonexistent', 'new');             // false (원본 없음)

// supports(token): DOMTokenList가 특정 토큰을 지원하는지 확인
// (classList에서는 의미 없음, relList에서 유용)
console.log(relList.supports('noopener'));    // true
console.log(relList.supports('preload'));     // true

// value: 전체 토큰 문자열 (className과 동일)
console.log(classList.value);

// 순회 지원
classList.forEach((token, index) => {
  console.log(index, token);
});

for (const token of classList) {
  console.log(token);
}

// entries(), keys(), values() 지원
for (const [index, token] of classList.entries()) {
  console.log(index, token);
}
```

---

### 9. DOM Traversal Mixins

DOM: 여러 인터페이스에 공통 기능을 추가하기 위해 mixin 패턴 사용

- Mixin은 독립적으로 인스턴스화할 수 없으며, 다른 인터페이스에 "혼합(mix-in)"되어 사용

#### 9.1 ParentNode Mixin

ParentNode: 자식을 가질 수 있는 노드(Document, DocumentFragment, Element)에 혼합되는 인터페이스

```javascript
// children: 자식 요소만 포함하는 live HTMLCollection (Text, Comment 제외)
const div = document.createElement('div');
div.innerHTML = 'text <span>A</span><!-- comment --><span>B</span>';

console.log(div.childNodes.length);          // 4 (Text, span, Comment, span)
console.log(div.children.length);            // 2 (span, span만)
console.log(div.children[0].textContent);    // "A"
console.log(div.children[1].textContent);    // "B"

// firstElementChild / lastElementChild: 첫/마지막 자식 요소
console.log(div.firstElementChild.textContent);  // "A"
console.log(div.lastElementChild.textContent);   // "B"

// childElementCount: 자식 요소 수 (= children.length)
console.log(div.childElementCount);              // 2
```

##### prepend() / append()

```javascript
// prepend(...nodes): 첫 번째 자식 앞에 삽입
// 문자열은 자동으로 Text 노드로 변환
const parent = document.createElement('div');
parent.innerHTML = '<p>기존 내용</p>';

parent.prepend('앞에 추가된 텍스트', document.createElement('hr'));
// 결과: "앞에 추가된 텍스트" <hr> <p>기존 내용</p>

// append(...nodes): 마지막 자식 뒤에 삽입
parent.append(document.createElement('footer'), '뒤에 추가된 텍스트');
// 결과: ... <p>기존 내용</p> <footer> "뒤에 추가된 텍스트"

// appendChild와의 차이:
// 1. append는 여러 노드를 한번에 추가 가능
// 2. append는 문자열을 Text 노드로 자동 변환
// 3. append는 반환값이 없음 (undefined), appendChild는 추가된 노드 반환
```

##### replaceChildren()

```javascript
// replaceChildren(...nodes): 모든 자식을 제거하고 새 노드로 교체
const container = document.createElement('div');
container.innerHTML = '<p>1</p><p>2</p><p>3</p>';

// 모든 자식 제거 (인자 없이 호출)
container.replaceChildren();
console.log(container.innerHTML);            // "" (빈 문자열)

// 새 자식으로 교체
const h1 = document.createElement('h1');
h1.textContent = '제목';
container.replaceChildren(h1, '본문 텍스트');
console.log(container.innerHTML);            // "<h1>제목</h1>본문 텍스트"

// textContent = '' 또는 innerHTML = '' 대비 장점:
// 새 자식을 한 번의 호출로 설정 가능 (제거 + 추가가 원자적)
```

##### querySelector() / querySelectorAll()

```javascript
// ParentNode mixin에 정의되어 Document, DocumentFragment, Element에서 사용 가능
const frag = document.createDocumentFragment();
frag.appendChild(document.createElement('div')).className = 'item';
frag.appendChild(document.createElement('div')).className = 'item active';

// DocumentFragment에서도 querySelector 사용 가능
const active = frag.querySelector('.item.active');
console.log(active.className);              // "item active"

const allItems = frag.querySelectorAll('.item');
console.log(allItems.length);               // 2
```

##### moveBefore()

2025년 3월경 ParentNode mixin에 병합된 메서드

- `insertBefore()`처럼 노드를 특정 위치로 옮기지만, 내부적으로 제거 후 재삽입하는 대신 원자적(atomic)으로 이동시켜 노드의 상태를 보존
- iframe 로딩 상태·비디오 재생·포커스·애니메이션/트랜지션 상태 등이 유지 → Interop 2026 주요 관심 영역 중 하나

```javascript
// insertBefore()와 시그니처는 동일하지만 상태를 보존한다
parent.moveBefore(movedNode, referenceNode); // referenceNode가 null이면 끝에 추가

// 예: iframe을 다른 부모로 옮겨도 로드 상태가 유지됨
const iframe = document.querySelector('iframe');
newContainer.moveBefore(iframe, null);
// appendChild(iframe)을 사용했다면 iframe이 다시 로드되었을 것
```

#### 9.2 ChildNode Mixin

ChildNode: 부모를 가질 수 있는 비문서 노드(DocumentType, Element, CharacterData)에 혼합

```javascript
// before(...nodes): 이 노드 앞에 형제로 삽입
const target = document.getElementById('target');
target.before('앞 텍스트', document.createElement('hr'));
// <target> 앞에 "앞 텍스트"와 <hr>이 삽입됨

// after(...nodes): 이 노드 뒤에 형제로 삽입
target.after(document.createElement('hr'), '뒤 텍스트');
// <target> 뒤에 <hr>과 "뒤 텍스트"가 삽입됨

// replaceWith(...nodes): 이 노드를 다른 노드로 교체
const replacement = document.createElement('div');
replacement.textContent = '교체된 내용';
target.replaceWith(replacement, '추가 텍스트');
// target은 DOM에서 제거되고, replacement와 텍스트 노드가 그 자리에 삽입

// remove(): 이 노드를 부모에서 제거
const el = document.getElementById('to-remove');
el.remove();
// parentNode.removeChild(el)과 동일하지만 더 간결
// 부모가 없는 경우 아무 일도 하지 않음 (오류 없음)
console.log(el.parentNode);                  // null

// 문자열은 자동으로 Text 노드로 변환됨
const li = document.querySelector('li');
li.before('이전: ');                          // Text 노드로 변환되어 삽입
li.after(' :이후');                           // Text 노드로 변환되어 삽입

// 여러 노드 한번에 교체
const oldEl = document.querySelector('.old');
const frag1 = document.createElement('span');
frag1.textContent = '새 요소 1';
const frag2 = document.createElement('span');
frag2.textContent = '새 요소 2';
oldEl.replaceWith(frag1, ' 그리고 ', frag2);
// .old가 제거되고, <span>새 요소 1</span> 그리고 <span>새 요소 2</span>으로 교체
```

#### 9.3 NonDocumentTypeChildNode Mixin

NonDocumentTypeChildNode: Element와 CharacterData에 혼합 → 요소 단위 형제 탐색 제공

```javascript
// HTML 구조:
// <ul>
//   <li id="a">A</li>
//   <!-- 주석 -->
//   <li id="b">B</li>
//   text node
//   <li id="c">C</li>
// </ul>

const b = document.getElementById('b');

// previousElementSibling: 이전 형제 요소 (Text, Comment 건너뜀)
console.log(b.previousElementSibling.id);    // "a" (주석 건너뜀)

// nextElementSibling: 다음 형제 요소 (Text, Comment 건너뜀)
console.log(b.nextElementSibling.id);        // "c" (텍스트 노드 건너뜀)

// 비교: previousSibling / nextSibling (Node 속성)은 모든 노드 타입 포함
console.log(b.previousSibling);              // Comment 노드 또는 Text 노드
console.log(b.nextSibling);                  // Text 노드

// 첫/마지막 요소의 경우
const a = document.getElementById('a');
const c = document.getElementById('c');
console.log(a.previousElementSibling);       // null (이전 요소 형제 없음)
console.log(c.nextElementSibling);           // null (다음 요소 형제 없음)

// 실전: 형제 요소 순회
function getAllSiblings(element) {
  const siblings = [];
  let sibling = element.parentElement.firstElementChild;
  while (sibling) {
    if (sibling !== element) {
      siblings.push(sibling);
    }
    sibling = sibling.nextElementSibling;
  }
  return siblings;
}

// 실전: 이전/다음 형제 탐색 유틸리티
function getPreviousSiblings(element) {
  const result = [];
  let prev = element.previousElementSibling;
  while (prev) {
    result.unshift(prev);
    prev = prev.previousElementSibling;
  }
  return result;
}

function getNextSiblings(element) {
  const result = [];
  let next = element.nextElementSibling;
  while (next) {
    result.push(next);
    next = next.nextElementSibling;
  }
  return result;
}
```

#### 9.4 Slottable Mixin

Slottable: Element와 Text에 혼합 → Shadow DOM의 슬롯 메커니즘 지원

```javascript
// assignedSlot: 이 노드가 할당된 <slot> 요소 (없으면 null)

// Shadow DOM 호스트 설정
const host = document.createElement('div');
document.body.appendChild(host);
const shadow = host.attachShadow({ mode: 'open' });
shadow.innerHTML = `
  <slot name="header"></slot>
  <slot></slot>
  <slot name="footer"></slot>
`;

// Light DOM (슬롯에 할당될 콘텐츠)
host.innerHTML = `
  <h1 slot="header">제목</h1>
  <p>기본 슬롯 콘텐츠</p>
  <span slot="footer">바닥글</span>
`;

// assignedSlot 확인
const h1 = host.querySelector('h1');
console.log(h1.assignedSlot);               // <slot name="header">
console.log(h1.assignedSlot.name);          // "header"

const p = host.querySelector('p');
console.log(p.assignedSlot);                // <slot> (이름 없는 기본 슬롯)
console.log(p.assignedSlot.name);           // "" (빈 문자열)

// Text 노드도 assignedSlot을 가질 수 있음
// host에 직접 텍스트를 추가하면 기본 슬롯에 할당됨

// <slot> 요소의 메서드:
const headerSlot = shadow.querySelector('slot[name="header"]');

// assignedNodes(options): 할당된 노드 목록
console.log(headerSlot.assignedNodes());
// [<h1 slot="header">제목</h1>]

// assignedElements(options): 할당된 요소 목록 (Text 노드 제외)
console.log(headerSlot.assignedElements());
// [<h1 slot="header">제목</h1>]

// flatten 옵션: 슬롯에 할당된 노드가 없을 때 fallback 콘텐츠 포함
const emptySlot = shadow.querySelector('slot[name="footer"]');
// emptySlot에 할당된 노드가 없다면:
console.log(emptySlot.assignedNodes({ flatten: true }));
// fallback 콘텐츠 (슬롯 내부의 기본 콘텐츠)가 반환됨
```

---

Part 2는 [dom-events.md](dom-events.md)에서 계속됩니다. (이벤트 시스템, MutationObserver, Range, TreeWalker, AbortController, Shadow DOM)

---

## DOM Living Standard - 이벤트 시스템 및 고급 기능 (Part 2)

> [DOM Living Standard 기본](dom.md)의 후속 문서

---

### 목차

1. [이벤트 시스템 개요](#1-이벤트-시스템-개요)
2. [EventTarget](#2-eventtarget)
3. [Event 인터페이스](#3-event-인터페이스)
4. [이벤트 전파](#4-이벤트-전파)
5. [CustomEvent](#5-customevent)
6. [주요 이벤트 타입](#6-주요-이벤트-타입)
7. [MutationObserver](#7-mutationobserver)
8. [Range와 Selection](#8-range와-selection)
9. [NodeIterator와 TreeWalker](#9-nodeiterator와-treewalker)
10. [AbortController와 AbortSignal](#10-abortcontroller와-abortsignal)
11. [Shadow DOM](#11-shadow-dom)
12. [Mutation Algorithms](#12-mutation-algorithms)

---

### 1. 이벤트 시스템 개요

#### DOM 이벤트 모델의 역사

웹 초창기: 표준화된 이벤트 모델 없음

- Netscape → 이벤트 캡처링(Event Capturing) 모델 독자 구현
- Internet Explorer → 이벤트 버블링(Event Bubbling) 모델 독자 구현

- ~1997 · 인라인 이벤트 핸들러: `onclick="..."` HTML 속성
- ~1997 · DOM Level 0: `element.onclick = fn` 프로퍼티 방식
- 2000 · DOM Level 2 Events: `addEventListener` 도입·캡처+버블 통합
- 현재 · Living Standard: `options` 객체·`passive`·`signal` 등 추가

DOM Level 2에서 두 모델을 통합 → 캡처 → 타겟 → 버블 3단계 전파 모델 확립

- 현재의 Living Standard는 이를 기반으로 성능 최적화(passive)와 생명주기 관리(signal) 기능 추가

---

### 2. EventTarget

`EventTarget`: DOM 이벤트 시스템의 근간이 되는 인터페이스

- `Node`, `Window`, `XMLHttpRequest` 등 이벤트를 수신할 수 있는 모든 객체가 이를 상속

```webidl
interface EventTarget {
  constructor();
  undefined addEventListener(DOMString type, EventListener? callback,
                             optional (AddEventListenerOptions or boolean) options = {});
  undefined removeEventListener(DOMString type, EventListener? callback,
                                optional (EventListenerOptions or boolean) options = {});
  boolean dispatchEvent(Event event);
};
```

#### addEventListener

```js
// 기본 사용
button.addEventListener('click', handleClick);

// options 객체를 통한 세밀한 제어
button.addEventListener('click', handleClick, {
  capture: false,   // 캡처 단계에서 실행 여부 (기본: false)
  once: true,       // 한 번 실행 후 자동 제거
  passive: true,    // preventDefault() 호출하지 않겠다고 선언
  signal: controller.signal  // AbortSignal로 리스너 제거 제어
});
```

각 옵션의 역할:

- `capture`: `true`이면 캡처 단계에서 리스너 실행
  - 같은 핸들러라도 `capture` 값이 다르면 별개의 리스너로 등록됨
- `once`: 리스너가 최초 1회 호출된 후 자동 제거
  - 초기화 로직이나 일회성 애니메이션에 유용
- `passive`: 브라우저에게 이 리스너가 `preventDefault()`를 호출하지 않을 것임을 알림
  - 스크롤/터치 이벤트에서 브라우저가 기본 동작을 즉시 실행 가능 → 성능 향상
- `signal`: `AbortSignal`을 전달하면 해당 시그널이 중단될 때 리스너 자동 제거

```js
// passive 리스너 - 스크롤 성능 최적화
document.addEventListener('touchmove', onTouchMove, { passive: true });

// signal을 이용한 리스너 일괄 제거
const controller = new AbortController();

element.addEventListener('click', onClick, { signal: controller.signal });
element.addEventListener('keydown', onKeyDown, { signal: controller.signal });
element.addEventListener('mouseover', onHover, { signal: controller.signal });

// 세 리스너를 한 번에 제거
controller.abort();
```

#### removeEventListener

리스너 제거: 등록 시와 동일한 함수 참조·동일한 `capture` 값 전달 필요

```js
// 올바른 제거
const handler = (e) => console.log(e);
el.addEventListener('click', handler);
el.removeEventListener('click', handler); // 동일 참조 -> 제거 성공

// 실패하는 제거
el.addEventListener('click', (e) => console.log(e));
el.removeEventListener('click', (e) => console.log(e)); // 다른 참조 -> 제거 실패
```

#### dispatchEvent

프로그래밍 방식으로 이벤트 발생

- 동기적으로 실행되며 `isTrusted`는 `false`

```js
const event = new Event('build', { bubbles: true, cancelable: true });
const canceled = !element.dispatchEvent(event);
if (canceled) {
  console.log('이벤트가 preventDefault()로 취소됨');
}
```

---

### 3. Event 인터페이스

모든 이벤트 객체의 기반 인터페이스

```webidl
interface Event {
  constructor(DOMString type, optional EventInit eventInitDict = {});
  readonly attribute DOMString type;
  readonly attribute EventTarget? target;
  readonly attribute EventTarget? currentTarget;
  readonly attribute unsigned short eventPhase;
  readonly attribute boolean bubbles;
  readonly attribute boolean cancelable;
  readonly attribute boolean composed;
  readonly attribute boolean isTrusted;
  readonly attribute DOMHighResTimeStamp timeStamp;

  undefined stopPropagation();
  undefined stopImmediatePropagation();
  undefined preventDefault();
  readonly attribute boolean defaultPrevented;
  sequence<EventTarget> composedPath();
};
```

#### 핵심 속성

- `type`: 이벤트 이름 문자열(`"click"`, `"input"` 등)
- `target`: 이벤트가 최초로 디스패치된 객체(이벤트 원점)
- `currentTarget`: 현재 실행 중인 리스너가 등록된 객체
- `eventPhase`: 현재 전파 단계(0: None, 1: Capture, 2: Target, 3: Bubble)
- `bubbles`: 이벤트가 버블링되는지 여부
- `cancelable`: `preventDefault()`로 취소 가능한지 여부
- `composed`: Shadow DOM 경계를 넘어 전파되는지 여부
- `isTrusted`: 브라우저가 생성한 이벤트이면 `true`, 스크립트가 생성하면 `false`

#### target vs currentTarget

```js
// <div id="parent"><button id="child">Click</button></div>
document.getElementById('parent').addEventListener('click', (e) => {
  console.log(e.target);        // button#child (클릭한 실제 요소)
  console.log(e.currentTarget); // div#parent   (리스너가 등록된 요소)
});
```

#### composedPath

`composedPath()`: 이벤트가 실제로 통과한 전체 경로를 타겟에서 최상위 순서로 배열로 반환

- Shadow DOM 내부에서 발생한 이벤트는 `target`이 호스트 요소로 리타게팅되어 원래 발생 지점을 알기 어려움
- `composedPath()`는 리타게팅 이전의 실제 경로(Shadow 경계 포함)를 그대로 보여줌

```js
shadow.addEventListener('click', (e) => {
  console.log(e.composedPath());
  // [실제 클릭된 요소, ..., shadowRoot, 호스트 요소, ..., document, window]
});
```

#### 전파 제어 메서드

```js
element.addEventListener('click', (e) => {
  e.stopPropagation();          // 다음 노드로의 전파 중단, 같은 노드의 다른 리스너는 실행
  e.stopImmediatePropagation(); // 같은 노드의 나머지 리스너도 실행 중단
  e.preventDefault();           // 브라우저 기본 동작 취소 (링크 이동, 폼 제출 등)
});
```

---

### 4. 이벤트 전파

#### 3단계 전파 모델

이벤트 발생 → DOM 트리를 따라 3단계로 전파

```
Phase 1: Capture (Window → Document → ... → Target의 부모)
Phase 2: Target  (Target 노드 자체)
Phase 3: Bubble  (Target의 부모 → ... → Document → Window)
```

```
         ① Capture ↓               ③ Bubble ↑
Window ─────────────────────────────────────── Window
  Document ───────────────────────────────── Document
    <html> ─────────────────────────────── <html>
      <body> ──────────────────────────── <body>
        <div> ────────────────────────── <div>
          <button>  ② Target Phase  </button>
```

```js
// 캡처 단계에서 실행
parent.addEventListener('click', handler, true);
// 또는
parent.addEventListener('click', handler, { capture: true });

// 버블 단계에서 실행 (기본값)
parent.addEventListener('click', handler, false);
```

#### 이벤트 위임 패턴

이벤트 버블링을 활용하여 부모 요소에 단일 리스너를 등록하는 패턴

- 동적으로 추가되는 자식 요소도 자동으로 처리 가능
- 메모리 효율이 높음

```js
// 비효율적: 각 항목에 리스너 등록
document.querySelectorAll('li').forEach(li => {
  li.addEventListener('click', handleItem);
});

// 효율적: 이벤트 위임
document.querySelector('ul').addEventListener('click', (e) => {
  const li = e.target.closest('li');
  if (!li) return; // li 외부 클릭 무시
  handleItem(li);
});
```

`closest()` 메서드 사용 시, 클릭된 요소가 `<li>` 내부의 `<span>`이라 하더라도 올바르게 `<li>`를 찾아낼 수 있음

---

### 5. CustomEvent

애플리케이션 고유의 이벤트 정의 가능

- `detail` 속성으로 임의의 데이터 전달

```js
// 커스텀 이벤트 생성 및 디스패치
const event = new CustomEvent('user:login', {
  bubbles: true,
  composed: true,   // Shadow DOM 경계 통과
  detail: {
    userId: 42,
    username: 'alice',
    timestamp: Date.now()
  }
});
element.dispatchEvent(event);

// 수신
document.addEventListener('user:login', (e) => {
  console.log(e.detail.username); // 'alice'
});
```

#### 컴포넌트 간 통신

CustomEvent: 느슨하게 결합된 컴포넌트 간 통신에 적합

```js
// 컴포넌트 A: 장바구니에 상품 추가 알림
class ProductCard extends HTMLElement {
  addToCart(product) {
    this.dispatchEvent(new CustomEvent('cart:add', {
      bubbles: true,
      detail: { product }
    }));
  }
}

// 컴포넌트 B: 장바구니 상태 업데이트
class ShoppingCart extends HTMLElement {
  connectedCallback() {
    // 문서 레벨에서 이벤트 수신 (어디서든 발생 가능)
    document.addEventListener('cart:add', (e) => {
      this.items.push(e.detail.product);
      this.render();
    });
  }
}
```

---

### 6. 주요 이벤트 타입

#### MouseEvent

```js
element.addEventListener('click', (e) => {
  e.clientX, e.clientY   // 뷰포트 기준 좌표
  e.pageX, e.pageY       // 문서 기준 좌표 (스크롤 포함)
  e.offsetX, e.offsetY   // 타겟 요소 기준 좌표
  e.button                // 0: 좌클릭, 1: 중간, 2: 우클릭
  e.buttons               // 동시에 눌린 버튼 비트마스크
  e.altKey, e.ctrlKey, e.shiftKey, e.metaKey // 수정키 상태
});
// 관련 이벤트: click, dblclick, mousedown, mouseup, mousemove, mouseenter, mouseleave
```

#### KeyboardEvent

```js
document.addEventListener('keydown', (e) => {
  e.key       // 'a', 'Enter', 'ArrowUp' 등 논리적 키 값
  e.code      // 'KeyA', 'Enter' 등 물리적 키 코드
  e.repeat    // 키를 계속 누르고 있으면 true

  // Ctrl+S 단축키 구현
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault();
    save();
  }
});
// key vs code: 'key'는 입력 언어/레이아웃에 따라 달라지고, 'code'는 물리적 위치 기반
```

#### FocusEvent

```js
// focusin/focusout: 버블링 O, focus/blur: 버블링 X
input.addEventListener('focusin', (e) => {
  e.relatedTarget; // 포커스를 잃은 이전 요소
});
input.addEventListener('focusout', (e) => {
  e.relatedTarget; // 포커스를 받을 다음 요소
});
```

#### InputEvent

```js
input.addEventListener('input', (e) => {
  e.data          // 삽입된 문자 (삭제 시 null)
  e.inputType     // 'insertText', 'deleteContentBackward', 'insertFromPaste' 등
  e.isComposing   // IME 조합 중인지 여부 (한글/일본어 입력)
});
// 'input'은 값이 변경된 후, 'beforeinput'은 변경 전에 발생 (취소 가능)
```

#### WheelEvent / TouchEvent / PointerEvent

```js
// WheelEvent - 스크롤 휠
element.addEventListener('wheel', (e) => {
  e.deltaX, e.deltaY, e.deltaZ  // 스크롤 양
  e.deltaMode  // 0: 픽셀, 1: 라인, 2: 페이지
}, { passive: true });

// TouchEvent - 터치 스크린
element.addEventListener('touchstart', (e) => {
  const touch = e.touches[0];      // 현재 화면 위의 모든 터치
  touch.clientX, touch.clientY
  e.changedTouches  // 이번 이벤트에서 변경된 터치
  e.targetTouches   // 현재 요소 위의 터치
});

// PointerEvent - 마우스+터치+펜 통합 API
element.addEventListener('pointerdown', (e) => {
  e.pointerId      // 고유 포인터 ID
  e.pointerType    // 'mouse', 'touch', 'pen'
  e.pressure       // 필압 (0.0 ~ 1.0)
  e.width, e.height // 접촉 영역 크기
  e.isPrimary      // 기본 포인터 여부
});
```

#### CompositionEvent

IME(Input Method Editor)로 한글·일본어·중국어 등을 입력할 때 발생

```js
input.addEventListener('compositionstart', (e) => {
  // IME 조합 시작 (예: 한글 'ㅎ' 입력 시작)
});
input.addEventListener('compositionupdate', (e) => {
  e.data; // 현재 조합 중인 문자열 (예: '하', '한')
});
input.addEventListener('compositionend', (e) => {
  e.data; // 확정된 문자열 (예: '한글')
});
```

---

### 7. MutationObserver

DOM 변경을 비동기적으로 감시

- 이전의 `MutationEvents`(DOM Level 2)는 동기적 실행으로 인한 성능 문제로 폐기 → `MutationObserver`가 대체제로 도입

```js
const observer = new MutationObserver((mutations, observer) => {
  for (const mutation of mutations) {
    switch (mutation.type) {
      case 'childList':
        console.log('추가:', mutation.addedNodes);
        console.log('제거:', mutation.removedNodes);
        break;
      case 'attributes':
        console.log(`${mutation.attributeName}: ${mutation.oldValue} → 새 값`);
        break;
      case 'characterData':
        console.log(`텍스트 변경: ${mutation.oldValue}`);
        break;
    }
  }
});
```

#### observe 옵션

```js
observer.observe(targetNode, {
  childList: true,              // 자식 노드 추가/제거 감시
  attributes: true,             // 속성 변경 감시
  characterData: true,          // 텍스트 내용 변경 감시
  subtree: true,                // 하위 트리 전체 감시
  attributeOldValue: true,      // 변경 전 속성 값 기록
  characterDataOldValue: true,  // 변경 전 텍스트 기록
  attributeFilter: ['class', 'style'] // 특정 속성만 감시
});
```

#### MutationRecord 구조

- `type`: `'childList'`·`'attributes'`·`'characterData'`
- `target`: 변경이 발생한 노드
- `addedNodes`: 추가된 노드 목록(NodeList)
- `removedNodes`: 제거된 노드 목록(NodeList)
- `previousSibling`: 추가/제거된 노드의 이전 형제
- `nextSibling`: 추가/제거된 노드의 다음 형제
- `attributeName`: 변경된 속성 이름
- `oldValue`: 변경 전 값

#### disconnect와 takeRecords

```js
// 아직 콜백에 전달되지 않은 대기 중인 레코드 수동 회수
const pendingRecords = observer.takeRecords();

// 감시 중단
observer.disconnect();
```

---

### 8. Range와 Selection

#### Range

`Range`: 문서 내 연속된 영역을 표현

- 텍스트 선택·DOM 조작·에디터 구현에 핵심적

```js
const range = new Range();
// 또는
const range = document.createRange();

// 범위 설정
range.setStart(startNode, startOffset);
range.setEnd(endNode, endOffset);

// 편의 메서드
range.selectNode(node);          // 노드 자체를 범위로
range.selectNodeContents(node);  // 노드의 내용을 범위로
range.collapse(toStart);         // 범위를 시작점(true) 또는 끝점(false)으로 축소
```

#### Range 조작

```js
// 콘텐츠 추출 (원본에서 제거)
const fragment = range.extractContents();

// 콘텐츠 복제 (원본 유지)
const clone = range.cloneContents();

// 콘텐츠 삭제
range.deleteContents();

// 범위에 노드 삽입
const newNode = document.createElement('span');
range.insertNode(newNode);

// 범위를 노드로 감싸기
const wrapper = document.createElement('mark');
range.surroundContents(wrapper);
```

#### Selection API

```js
const selection = window.getSelection();

// 현재 선택 정보
selection.anchorNode;     // 선택 시작 노드
selection.anchorOffset;   // 선택 시작 오프셋
selection.focusNode;      // 선택 끝 노드
selection.focusOffset;    // 선택 끝 오프셋
selection.isCollapsed;    // 선택 영역이 0인지 (커서만 있는 상태)
selection.rangeCount;     // Range 개수

// Range 조작
const range = selection.getRangeAt(0);
selection.addRange(range);
selection.removeAllRanges();
selection.collapse(node, offset);
selection.selectAllChildren(node);

// 선택된 텍스트
const text = selection.toString();
```

---

### 9. NodeIterator와 TreeWalker

DOM 트리를 순회하기 위한 전용 인터페이스

#### NodeIterator

순차적(flat) 순회에 적합

- `nextNode()`와 `previousNode()`로 이동

```js
const iterator = document.createNodeIterator(
  document.body,         // 루트 노드
  NodeFilter.SHOW_TEXT,  // 텍스트 노드만
  {
    acceptNode(node) {
      // 빈 텍스트 노드 건너뛰기
      return node.textContent.trim()
        ? NodeFilter.FILTER_ACCEPT
        : NodeFilter.FILTER_REJECT;
    }
  }
);

let node;
while (node = iterator.nextNode()) {
  console.log(node.textContent);
}
```

#### TreeWalker

트리 구조 탐색에 적합

- 부모/자식/형제 방향으로 자유롭게 이동 가능

```js
const walker = document.createTreeWalker(
  document.body,
  NodeFilter.SHOW_ELEMENT,
  {
    acceptNode(node) {
      if (node.matches('.hidden')) return NodeFilter.FILTER_REJECT;  // 하위도 건너뜀
      if (node.matches('.skip'))   return NodeFilter.FILTER_SKIP;    // 자신만 건너뜀
      return NodeFilter.FILTER_ACCEPT;
    }
  }
);

// 탐색 메서드
walker.parentNode();      // 부모로 이동
walker.firstChild();      // 첫 자식으로 이동
walker.lastChild();       // 마지막 자식으로 이동
walker.previousSibling(); // 이전 형제로 이동
walker.nextSibling();     // 다음 형제로 이동
walker.nextNode();        // 문서 순서상 다음 노드
walker.previousNode();    // 문서 순서상 이전 노드
walker.currentNode;       // 현재 노드 (읽기/쓰기 가능)
```

#### NodeFilter 상수

- `SHOW_ALL` (`0xFFFFFFFF`): 모든 노드
- `SHOW_ELEMENT` (`0x1`): Element 노드
- `SHOW_TEXT` (`0x4`): Text 노드
- `SHOW_COMMENT` (`0x80`): Comment 노드
- `SHOW_DOCUMENT` (`0x100`): Document 노드
- `SHOW_DOCUMENT_FRAGMENT` (`0x400`): DocumentFragment

FILTER_REJECT vs FILTER_SKIP

- `REJECT`: 해당 노드와 그 하위 트리 전체를 건너뜀
- `SKIP`: 해당 노드만 건너뛰고 자식은 계속 순회
- NodeIterator에서는 둘 다 동일하게 동작

---

### 10. AbortController와 AbortSignal

비동기 작업의 취소를 위한 범용 메커니즘

- fetch 요청뿐 아니라 이벤트 리스너·스트림·커스텀 비동기 작업 등에 널리 사용

#### 기본 사용

```js
const controller = new AbortController();
const signal = controller.signal;

// fetch 취소
fetch('/api/data', { signal })
  .then(res => res.json())
  .catch(err => {
    if (err.name === 'AbortError') {
      console.log('요청이 취소됨');
    }
  });

// 3초 후 취소
setTimeout(() => controller.abort(), 3000);

// 취소 이유 전달 (선택)
controller.abort(new Error('사용자가 취소함'));
signal.reason; // Error: 사용자가 취소함
```

#### AbortSignal 정적 메서드

```js
// 타임아웃 시그널 - 지정된 시간 후 자동으로 중단
const signal = AbortSignal.timeout(5000);
fetch('/api/slow', { signal }); // 5초 후 자동 취소

// 이미 중단된 시그널 생성
const aborted = AbortSignal.abort('이유');

// 여러 시그널 중 하나라도 중단되면 중단되는 복합 시그널
const combined = AbortSignal.any([
  AbortSignal.timeout(5000),
  controller.signal,
  otherController.signal
]);
```

#### 이벤트 리스너 관리

```js
// 컴포넌트 생명주기에 맞춘 리스너 관리
class MyComponent {
  #controller = new AbortController();

  mount() {
    const { signal } = this.#controller;

    window.addEventListener('resize', this.onResize, { signal });
    window.addEventListener('scroll', this.onScroll, { signal, passive: true });
    document.addEventListener('keydown', this.onKeyDown, { signal });
    this.element.addEventListener('click', this.onClick, { signal });
  }

  unmount() {
    // 모든 리스너를 한 번에 정리
    this.#controller.abort();
  }
}
```

---

### 11. Shadow DOM

Shadow DOM: DOM의 캡슐화(encapsulation) 메커니즘

- 컴포넌트 내부의 DOM 구조와 스타일을 외부로부터 격리

#### attachShadow

```js
class MyCard extends HTMLElement {
  constructor() {
    super();
    // mode: 'open' -> element.shadowRoot로 접근 가능
    // mode: 'closed' -> 외부에서 접근 불가
    const shadow = this.attachShadow({ mode: 'open' });

    shadow.innerHTML = `
      <style>
        /* Shadow DOM 내부에서만 적용 */
        :host {
          display: block;
          border: 1px solid #ccc;
          padding: 16px;
        }
        :host(.featured) {
          border-color: gold;
        }
        :host-context(.dark-theme) {
          background: #333;
          color: white;
        }
        h2 { color: navy; }
      </style>
      <h2><slot name="title">기본 제목</slot></h2>
      <div class="content">
        <slot>기본 콘텐츠</slot>
      </div>
    `;
  }
}
customElements.define('my-card', MyCard);
```

```html
<my-card class="featured">
  <span slot="title">카드 제목</span>
  <p>카드 본문 내용</p>
</my-card>
```

#### Slot

`<slot>`: 외부 콘텐츠(Light DOM)가 Shadow DOM 내부에 투영(projection)되는 지점

```js
// slotchange 이벤트로 슬롯 변경 감지
shadow.querySelector('slot').addEventListener('slotchange', (e) => {
  const assigned = e.target.assignedNodes({ flatten: true });
  console.log('슬롯에 할당된 노드:', assigned);
});
```

#### Shadow DOM CSS 셀렉터

- `:host`: Shadow Root의 호스트 요소
- `:host(selector)`: 조건부 호스트 스타일링
- `:host-context(selector)`: 호스트의 조상이 조건에 맞을 때
- `::slotted(selector)`: 슬롯에 분배된 Light DOM 요소(직계만)
- `::part(name)`: 외부에서 Shadow DOM 내부 요소를 스타일링

```css
/* 외부 CSS에서 part를 통해 Shadow DOM 내부 스타일링 */
my-card::part(header) {
  background: linear-gradient(to right, #667eea, #764ba2);
  color: white;
}
```

```js
// Shadow DOM 내부에서 part 노출
shadow.innerHTML = `<header part="header">...</header>`;
```

#### 이벤트와 Shadow DOM

`composed: true`인 이벤트만 Shadow DOM 경계를 넘어 전파

- 네이티브 UI 이벤트(click, input 등)는 대부분 composed

```js
// Shadow DOM 내부에서 발생한 이벤트
// composed: true  -> Shadow 경계 밖으로 전파됨 (target은 호스트로 리타게팅)
// composed: false -> Shadow 내부에서만 전파됨
shadowChild.dispatchEvent(new CustomEvent('internal', {
  bubbles: true,
  composed: false  // Shadow 내부에만 전파
}));
```

---

### 12. Mutation Algorithms

DOM 명세: 노드 조작 시 내부적으로 실행되는 알고리즘을 정의

- 이 알고리즘들은 `appendChild`, `removeChild`, `replaceChild` 등의 API 호출 시 브라우저가 실제로 수행하는 단계를 기술

#### Insert (삽입)

`parent.appendChild(node)` 또는 `parent.insertBefore(node, child)` 호출 시 실행

```
1. 삽입할 노드가 DocumentFragment이면 그 자식 노드들을 삽입 대상으로 설정
2. 노드가 이미 트리에 존재하면 기존 위치에서 제거 (adopt)
3. 대상 노드를 부모의 자식 목록에 삽입
4. 각 삽입된 노드에 대해:
   a. 노드의 ownerDocument 업데이트
   b. 연결(connected) 상태가 변경되면 connectedCallback 호출 (Custom Element)
   c. MutationObserver에 childList 변경 통지
```

```js
const fragment = document.createDocumentFragment();
fragment.append(el1, el2, el3);
// 한 번의 insert로 세 노드 삽입 -> MutationRecord 1개, 리플로우 1회
parent.appendChild(fragment);
```

#### Remove (제거)

```
1. 부모의 자식 목록에서 노드 제거
2. 노드가 connected 상태였으면 disconnectedCallback 호출 (Custom Element)
3. MutationObserver에 childList 변경 통지 (removedNodes에 포함)
4. 제거된 노드의 모든 하위 노드도 연결 해제
```

```js
// 자기 자신 제거 (모던 API)
element.remove();

// 부모를 통한 제거 (레거시 API)
parent.removeChild(element);
```

#### Replace (교체)

`parent.replaceChild(newChild, oldChild)` 호출 시 실행

```
1. 새 노드의 유효성 검증 (Document 구조 위반 확인)
2. 새 노드가 DocumentFragment이면 자식 노드들 추출
3. 기존 노드 제거 (Remove 알고리즘)
4. 새 노드 삽입 (Insert 알고리즘)
5. MutationObserver 통지 (addedNodes + removedNodes 모두 포함)
```

```js
// 모던 API
oldElement.replaceWith(newElement);

// 여러 노드로 교체도 가능
oldElement.replaceWith(span, ' 텍스트 ', anotherSpan);
```

#### Adopt (입양)

노드를 다른 Document로 이동시키는 알고리즘

- `document.adoptNode(node)` 호출 시 또는 다른 Document의 노드를 삽입할 때 자동으로 실행

```
1. 노드의 기존 부모가 있으면 해당 부모에서 제거
2. 노드와 모든 하위 노드의 ownerDocument를 새 Document로 변경
3. 각 노드에 대해 adopting steps 실행 (Custom Element의 adoptedCallback 호출)
```

```js
// iframe의 노드를 현재 문서로 가져오기
const iframe = document.querySelector('iframe');
const foreignNode = iframe.contentDocument.querySelector('.widget');

// adoptNode: ownerDocument 변경
const adopted = document.adoptNode(foreignNode);
document.body.appendChild(adopted);

// importNode: 복제 + ownerDocument 변경 (원본 유지)
const imported = document.importNode(foreignNode, true); // true = deep clone
document.body.appendChild(imported);
```

#### 주요 유효성 검사 규칙

DOM 트리의 무결성 보장을 위해 삽입/교체 시 다음 규칙 적용

```
- Document는 최대 하나의 Element 자식(document element)을 가질 수 있다
- Document는 최대 하나의 DocumentType 자식을 가질 수 있다
- DocumentType은 Document의 자식이어야 한다
- 노드를 자기 자신의 하위에 삽입할 수 없다 (순환 방지)
- 위반 시 HierarchyRequestError DOMException 발생
```

```js
try {
  const child = document.querySelector('.child');
  const parent = document.querySelector('.parent');
  child.appendChild(parent); // parent가 child의 조상이면 에러
} catch (e) {
  console.error(e); // HierarchyRequestError
}
```

---

### 참고 자료

- [DOM Living Standard (WHATWG)](https://dom.spec.whatwg.org/)
- [UI Events Specification (W3C)](https://w3c.github.io/uievents/)
- [Pointer Events (W3C)](https://w3c.github.io/pointerevents/)
- [Selection API (W3C)](https://w3c.github.io/selection-api/)
