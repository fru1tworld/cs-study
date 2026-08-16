# React Server Components, Rules, Compiler, Legacy APIs

# 1. React Server Components

## 1.1 Server Components

> 원문: https://react.dev/reference/rsc/server-components

- Server Components는 번들링 전에 클라이언트 앱이나 SSR 서버와 분리된 환경에서 미리 렌더링되는 새로운 유형의 컴포넌트임
- 이 분리된 환경이 React Server Components의 "서버"임
- CI 서버에서 빌드 타임에 한 번 실행하거나, 웹 서버를 사용해 요청마다 실행할 수 있음
- React 19에서 안정(stable) 상태이지만, RSC 번들러/프레임워크 구현에 사용되는 하위 API는 semver를 따르지 않으며 React 19.x 마이너 버전 간 변경될 수 있음

### 서버 없이 사용하는 Server Components

- 빌드 타임에 파일시스템을 읽거나 정적 콘텐츠를 가져올 수 있으므로 웹 서버가 필수가 아님
- Server Components 없이 정적 데이터를 가져오는 기존 패턴:

```js
// bundle.js
import marked from 'marked'; // 35.9K (11.2K gzipped)
import sanitizeHtml from 'sanitize-html'; // 206K (63.3K gzipped)

function Page({page}) {
  const [content, setContent] = useState('');
  // NOTE: loads *after* first page render.
  useEffect(() => {
    fetch(`/api/content/${page}`).then((data) => {
      setContent(data.content);
    });
  }, [page]);

  return <div>{sanitizeHtml(marked(content))}</div>;
}
```

- 이 패턴은 사용자가 75K(gzipped)의 라이브러리를 추가로 다운로드/파싱하고, 페이지 로드 후 두 번째 요청을 기다려야 함
- Server Components로 빌드 타임에 한 번 렌더링하는 방식:

```js
import marked from 'marked'; // Not included in bundle
import sanitizeHtml from 'sanitize-html'; // Not included in bundle

async function Page({page}) {
  // NOTE: loads *during* render, when the app is built.
  const content = await file.readFile(`${page}.md`);

  return <div>{sanitizeHtml(marked(content))}</div>;
}
```

- 렌더링된 출력을 SSR로 HTML 변환 후 CDN에 업로드할 수 있음
- 클라이언트는 원래 Page 컴포넌트나 비용이 큰 라이브러리를 보지 못하고, 렌더링된 출력만 받음
- 첫 페이지 로드 시 콘텐츠가 바로 보이고, 정적 콘텐츠 렌더링에 필요한 라이브러리가 번들에 포함되지 않음

### 비동기 컴포넌트

- Server Components는 async 함수로 작성할 수 있음
- render 중에 `await`할 수 있는 새로운 기능임

```js
async function Page({page}) {
  //...
}
```

### 서버와 함께 사용하는 Server Components

- 페이지 요청 중 웹 서버에서 실행되어 API를 구축하지 않고도 데이터 계층에 접근할 수 있음
- 번들링 전에 렌더링되며, 데이터와 JSX를 Client Components에 props로 전달할 수 있음
- 기존 클라이언트 Effect 패턴의 문제: 클라이언트-서버 워터폴(Note 렌더링 후 Author를 가져옴)

```js
// bundle.js
function Note({id}) {
  const [note, setNote] = useState('');
  useEffect(() => {
    fetch(`/api/notes/${id}`).then(data => {
      setNote(data.note);
    });
  }, [id]);

  return (
    <div>
      <Author id={note.authorId} />
      <p>{note}</p>
    </div>
  );
}

function Author({id}) {
  const [author, setAuthor] = useState('');
  useEffect(() => {
    fetch(`/api/authors/${id}`).then(data => {
      setAuthor(data.author);
    });
  }, [id]);

  return <span>By: {author.name}</span>;
}
```

- Server Components로 데이터를 직접 읽고 렌더링하는 방식:

```js
import db from './database';

async function Note({id}) {
  // NOTE: loads *during* render.
  const note = await db.notes.get(id);
  return (
    <div>
      <Author id={note.authorId} />
      <p>{note}</p>
    </div>
  );
}

async function Author({id}) {
  // NOTE: loads *after* Note,
  // but is fast if data is co-located.
  const author = await db.authors.get(id);
  return <span>By: {author.name}</span>;
}
```

- 서버 중심 Multi-Page App의 "요청/응답" 멘탈 모델과 클라이언트 중심 Single-Page App의 매끄러운 인터랙티비티를 결합함

### Server Components에 인터랙티비티 추가

- Server Components는 브라우저로 전송되지 않으므로 `useState` 같은 인터랙티브 API를 사용할 수 없음
- `"use client"` 디렉티브를 사용하는 Client Component와 조합하여 인터랙티비티를 추가함
- Server Components에는 디렉티브가 없음. `"use server"`는 Server Functions용이지 Server Components용이 아님

```js
// Server Component
import Expandable from './Expandable';

async function Notes() {
  const notes = await db.notes.getAll();
  return (
    <div>
      {notes.map(note => (
        <Expandable key={note.id}>
          <p note={note} />
        </Expandable>
      ))}
    </div>
  )
}
```

```js
// Client Component
"use client"

export default function Expandable({children}) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div>
      <button
        onClick={() => setExpanded(!expanded)}
      >
        Toggle
      </button>
      {expanded && children}
    </div>
  )
}
```

### Server Components의 비동기 컴포넌트

- async/await를 사용하는 새로운 컴포넌트 작성 방식
- async 컴포넌트에서 `await`하면 React가 suspend하고 promise가 resolve될 때까지 기다린 후 렌더링을 재개함
- 서버/클라이언트 경계를 넘어 Suspense의 스트리밍 지원과 함께 작동함
- 서버에서 promise를 생성하고 클라이언트에서 await할 수 있음

```js
// Server Component
import db from './database';

async function Page({id}) {
  const note = await db.notes.get(id);

  // NOTE: not awaited, will start here and await on the client.
  const commentsPromise = db.comments.get(note.id);
  return (
    <div>
      {note}
      <Suspense fallback={<p>Loading Comments...</p>}>
        <Comments commentsPromise={commentsPromise} />
      </Suspense>
    </div>
  );
}
```

```js
// Client Component
"use client";
import {use} from 'react';

function Comments({commentsPromise}) {
  const comments = use(commentsPromise);
  return comments.map(comment => <p>{comment}</p>);
}
```

- `note` 콘텐츠는 서버에서 `await`하고, 우선순위가 낮은 comments는 서버에서 promise를 시작한 후 클라이언트에서 `use` API로 기다림
- 비동기 컴포넌트는 클라이언트에서 지원되지 않으므로 `use`로 promise를 await함

---

## 1.2 Server Functions

> 원문: https://react.dev/reference/rsc/server-functions

- 2024년 9월까지 모든 Server Functions를 "Server Actions"라고 불렀음
- action prop에 전달되거나 action 내부에서 호출되는 Server Function이 Server Action이지만, 모든 Server Function이 Server Action은 아님
- Server Functions는 Client Components가 서버에서 실행되는 비동기 함수를 호출할 수 있게 함
- `"use server"` 디렉티브로 정의하면 프레임워크가 자동으로 Server Function에 대한 참조를 생성하고 Client Component에 전달함
- 클라이언트에서 호출 시 React가 서버로 요청을 보내 함수를 실행하고 결과를 반환함

### Server Component에서 Server Function 생성

```js
// Server Component
import Button from './Button';

function EmptyNote () {
  async function createNoteAction() {
    // Server Function
    'use server';

    await db.notes.create();
  }

  return <Button onClick={createNoteAction}/>;
}
```

```js
"use client";

export default function Button({onClick}) {
  console.log(onClick);
  // {$$typeof: Symbol.for("react.server.reference"), $$id: 'createNoteAction'}
  return <button onClick={() => onClick()}>Create Empty Note</button>
}
```

### Client Components에서 Server Functions 임포트

```js
"use server";

export async function createNote() {
  await db.notes.create();
}
```

```js
"use client";
import {createNote} from './actions';

function EmptyNote() {
  console.log(createNote);
  // {$$typeof: Symbol.for("react.server.reference"), $$id: 'createNote'}
  <button onClick={() => createNote()} />
}
```

### Server Functions와 Actions

- 클라이언트에서 Action으로부터 Server Functions를 호출할 수 있음

```js
"use server";

export async function updateName(name) {
  if (!name) {
    return {error: 'Name is required'};
  }
  await db.users.updateName(name);
}
```

```js
"use client";

import {updateName} from './actions';

function UpdateName() {
  const [name, setName] = useState('');
  const [error, setError] = useState(null);

  const [isPending, startTransition] = useTransition();

  const submitAction = async () => {
    startTransition(async () => {
      const {error} = await updateName(name);
      startTransition(() => {
        if (error) {
          setError(error);
        } else {
          setName('');
        }
      });
    })
  }

  return (
    <form action={submitAction}>
      <input type="text" name="name" disabled={isPending}/>
      {error && <span>Failed: {error}</span>}
    </form>
  )
}
```

- 클라이언트에서 Action으로 감싸면 Server Function의 `isPending` 상태에 접근할 수 있음

### Server Functions와 Form Actions

- React 19의 새로운 Form 기능과 함께 작동함
- Server Function을 Form에 전달하면 폼이 자동으로 서버에 제출됨

```js
"use client";

import {updateName} from './actions';

function UpdateName() {
  return (
    <form action={updateName}>
      <input type="text" name="name" />
    </form>
  )
}
```

- Form 제출이 성공하면 React가 자동으로 폼을 리셋함

### Server Functions와 useActionState

```js
"use client";

import {updateName} from './actions';

function UpdateName() {
  const [state, submitAction, isPending] = useActionState(updateName, {error: null});

  return (
    <form action={submitAction}>
      <input type="text" name="name" disabled={isPending}/>
      {state.error && <span>Failed: {state.error}</span>}
    </form>
  );
}
```

- `useActionState`와 Server Functions를 사용하면 React가 hydration 완료 전에 입력된 폼 제출도 자동으로 재생(replay)함
- 사용자가 앱이 hydrate되기 전에도 상호작용할 수 있음

### useActionState를 이용한 점진적 향상(progressive enhancement)

```js
"use client";

import {updateName} from './actions';

function UpdateName() {
  const [, submitAction] = useActionState(updateName, null, `/name/update`);

  return (
    <form action={submitAction}>
      ...
    </form>
  );
}
```

- permalink를 `useActionState`에 제공하면 JavaScript 번들 로드 전에 폼이 제출될 경우 해당 URL로 리다이렉트함

---

## 1.3 'use client'

> 원문: https://react.dev/reference/rsc/use-client

- `'use client'`는 클라이언트에서 실행되는 코드를 표시하는 데 사용됨
- 파일 상단에 추가하면 해당 모듈과 그 전이적 의존성을 클라이언트 코드로 표시함

```js
'use client';

import { useState } from 'react';
import { formatDate } from './formatters';
import Button from './button';

export default function RichTextEditor({ timestamp, text }) {
  const date = formatDate(timestamp);
  // ...
  const editButton = <Button />;
  // ...
}
```

- `'use client'`로 표시된 파일이 Server Component에서 임포트되면, 호환 가능한 번들러가 해당 모듈 임포트를 서버-클라이언트 코드 간 경계로 처리함
- `RichTextEditor`의 의존성인 `formatDate`와 `Button`도 `'use client'` 디렉티브 포함 여부와 무관하게 클라이언트에서 평가됨

### 주의사항

- 파일 최상단에 위치해야 하며, 임포트나 다른 코드보다 위에 있어야 함(주석은 허용)
- 작은따옴표나 큰따옴표로 작성해야 하며 백틱은 불가
- 다른 클라이언트 렌더링 모듈에서 임포트된 `'use client'` 모듈에서는 디렉티브가 효과 없음
- `'use client'` 디렉티브가 있는 컴포넌트 모듈의 모든 사용은 Client Component임이 보장됨
- 컴포넌트가 `'use client'` 디렉티브 없이도 클라이언트에서 평가될 수 있음
  - `'use client'` 디렉티브가 있는 모듈에 정의되거나 해당 모듈의 전이적 의존성이면 Client Component
  - 그렇지 않으면 Server Component
- 클라이언트 평가용으로 표시된 코드는 컴포넌트에 한정되지 않음. Client 모듈 하위 트리의 모든 코드가 클라이언트로 전송되어 실행됨
- 서버에서 평가되는 모듈이 `'use client'` 모듈의 값을 임포트할 때, 값은 React 컴포넌트이거나 지원되는 직렬화 가능한 prop 값이어야 함. 그 외의 경우 예외가 발생함

### 'use client'가 클라이언트 코드를 표시하는 방식

- React Server Components를 사용하는 앱은 기본적으로 서버에서 렌더링됨
- `'use client'`는 모듈 의존성 트리에 서버-클라이언트 경계를 도입하여 Client 모듈의 하위 트리를 생성함

```js
// src/App.js
import FancyText from './FancyText';
import InspirationGenerator from './InspirationGenerator';
import Copyright from './Copyright';

export default function App() {
  return (
    <>
      <FancyText title text="Get Inspired App" />
      <InspirationGenerator>
        <Copyright year={2004} />
      </InspirationGenerator>
    </>
  );
}
```

```js
// src/InspirationGenerator.js
'use client';

import { useState } from 'react';
import inspirations from './inspirations';
import FancyText from './FancyText';

export default function InspirationGenerator({children}) {
  const [index, setIndex] = useState(0);
  const quote = inspirations[index];
  const next = () => setIndex((index + 1) % inspirations.length);

  return (
    <>
      <p>Your inspirational quote is:</p>
      <FancyText text={quote} />
      <button onClick={next}>Inspire me again</button>
      {children}
    </>
  );
}
```

### Client Component와 Server Component의 정의

- Client Components: 렌더 트리에서 클라이언트에서 렌더링되는 컴포넌트
- Server Components: 렌더 트리에서 서버에서 렌더링되는 컴포넌트
- "컴포넌트"라는 용어는 두 가지 의미가 있음:
  - 컴포넌트 정의(component definition): 대부분 함수
  - 컴포넌트 사용(component usage): 정의의 사용처
- Server/Client Components를 이야기할 때는 컴포넌트 사용을 지칭함

### FancyText가 Server이면서 Client Component인 이유

- `FancyText` 정의에는 `'use client'` 디렉티브가 없음
- `App`의 자식으로 사용될 때는 Server Component
- `InspirationGenerator` 아래에서 임포트/호출될 때는 Client Component (InspirationGenerator에 `'use client'`가 있으므로)
- 컴포넌트 정의가 서버에서 평가되면서 동시에 클라이언트에서도 다운로드되어 Client Component 사용을 렌더링함

### Copyright가 Server Component인 이유

- `Copyright`는 Client Component인 `InspirationGenerator`의 자식으로 렌더링되지만 Server Component임
- `'use client'`는 렌더 트리가 아닌 모듈 의존성 트리에서 서버/클라이언트 코드 경계를 정의함
- `App.js`가 `Copyright.js`에서 `Copyright`를 임포트하고 호출하며, `Copyright.js`에는 `'use client'`가 없으므로 서버에서 렌더링됨
- Client Components가 Server Components를 렌더링할 수 있는 이유: JSX를 props로 전달할 수 있기 때문임
- 부모-자식 렌더 관계가 같은 렌더 환경을 보장하지 않음

### 'use client'를 사용해야 하는 시점

- Server Components가 기본값임
- Server Components의 장점:
  - 클라이언트에 보내고 실행하는 코드량을 줄일 수 있음
  - 서버에서 실행되어 로컬 파일시스템 접근 가능, 데이터 fetch 및 네트워크 요청의 낮은 지연시간
- Server Components의 제한:
  - 인터랙션을 지원할 수 없음 (이벤트 핸들러는 클라이언트에서만 등록/트리거 가능)
  - 대부분의 Hooks를 사용할 수 없음 (렌더링 후 메모리에 유지되지 않고 자체 state를 가질 수 없음)

### 직렬화 가능한 타입

- Server Component에서 Client Component로 전달되는 prop 값은 직렬화 가능해야 함
- 지원되는 타입:
  - 원시 타입: string, number, bigint, boolean, undefined, null, symbol(`Symbol.for`로 전역 등록된 것만)
  - 직렬화 가능한 값을 포함하는 이터러블: String, Array, Map, Set, TypedArray, ArrayBuffer
  - Date
  - 일반 객체: 직렬화 가능한 속성을 가진 객체 이니셜라이저로 생성된 것
  - Server Functions인 함수
  - Client 또는 Server Component 요소(JSX)
  - Promises
- 지원되지 않는 타입:
  - client-marked 모듈에서 export되지 않았거나 `'use server'`로 표시되지 않은 함수
  - 클래스
  - 빌트인 외 클래스의 인스턴스이거나 null 프로토타입을 가진 객체
  - 전역 등록되지 않은 Symbol

---

## 1.4 'use server'

> 원문: https://react.dev/reference/rsc/use-server

- `'use server'`는 클라이언트 코드에서 호출할 수 있는 서버 측 함수를 표시함
- 이 함수들을 Server Functions라고 부름
- async 함수의 본문 상단에 `'use server'`를 추가하면 클라이언트에서 호출 가능한 함수로 표시됨

```js
async function addToCart(data) {
  'use server';
  // ...
}
```

- 클라이언트에서 Server Function을 호출하면 전달된 인수의 직렬화된 복사본을 포함한 네트워크 요청을 서버로 보냄
- 반환값이 있으면 직렬화되어 클라이언트로 반환됨
- 개별 함수에 `'use server'`를 표시하는 대신 파일 상단에 디렉티브를 추가하면 파일의 모든 export가 Server Functions로 표시됨

### 주의사항

- 함수나 모듈의 맨 처음에 위치해야 하며, 임포트를 포함한 다른 코드보다 위에 있어야 함(주석은 허용)
- 작은따옴표나 큰따옴표 사용 필수, 백틱 불가
- 서버 측 파일에서만 사용 가능. 결과 Server Functions는 props를 통해 Client Components에 전달 가능
- 클라이언트 코드에서 Server Functions를 임포트하려면 모듈 레벨에서 디렉티브를 사용해야 함
- 기본 네트워크 호출이 항상 비동기이므로 async 함수에서만 사용 가능
- Server Functions의 인수를 항상 신뢰할 수 없는 입력으로 취급하고 모든 변경(mutation)을 인가해야 함
- Server Functions는 Transition에서 호출해야 함. `<form action>`이나 `formAction`에 전달된 Server Functions는 자동으로 transition에서 호출됨
- Server Functions는 서버 측 상태를 업데이트하는 mutation용으로 설계되었으며, 데이터 가져오기에는 권장되지 않음

### 보안 고려사항

- Server Functions의 인수는 완전히 클라이언트에서 제어됨
- 보안을 위해 항상 신뢰할 수 없는 입력으로 취급하고 적절히 검증/이스케이프해야 함
- 모든 Server Function에서 로그인한 사용자가 해당 작업을 수행할 수 있는지 검증해야 함
- 민감한 데이터 전송 방지를 위해 실험적 taint API(`experimental_taintUniqueValue`, `experimental_taintObjectReference`)가 있음

### 직렬화 가능한 인수와 반환값

- Server Function 인수 지원 타입:
  - 원시 타입: string, number, bigint, boolean, undefined, null, symbol(`Symbol.for`로 등록된 것만)
  - 직렬화 가능한 값을 포함하는 이터러블: String, Array, Map, Set, TypedArray, ArrayBuffer
  - Date, FormData 인스턴스
  - 직렬화 가능한 속성을 가진 일반 객체
  - Server Functions인 함수
  - Promises
- 지원되지 않는 타입:
  - React 요소 또는 JSX
  - Server Function이 아닌 함수(컴포넌트 함수 포함)
  - 클래스
  - 빌트인 외 클래스의 인스턴스이거나 null 프로토타입을 가진 객체
  - 전역 등록되지 않은 Symbol

### 폼에서 Server Functions 사용

```js
// App.js
async function requestUsername(formData) {
  'use server';
  const username = formData.get('username');
  // ...
}

export default function App() {
  return (
    <form action={requestUsername}>
      <input type="text" name="username" />
      <button type="submit">Request</button>
    </form>
  );
}
```

- 폼의 `action`에 Server Function을 전달하면 React가 폼의 FormData를 Server Function의 첫 번째 인수로 제공함
- Server Function을 폼 `action`에 전달하면 React가 점진적으로 폼을 향상시킬 수 있음 (JavaScript 번들 로드 전에 폼 제출 가능)

### 폼에서 반환값 처리

- `useActionState`를 사용하여 점진적 향상을 지원하면서 Server Function의 결과에 따라 UI를 업데이트할 수 있음

```js
// requestUsername.js
'use server';

export default async function requestUsername(formData) {
  const username = formData.get('username');
  if (canRequest(username)) {
    // ...
    return 'successful';
  }
  return 'failed';
}
```

```js
// UsernameForm.js
'use client';

import { useActionState } from 'react';
import requestUsername from './requestUsername';

function UsernameForm() {
  const [state, action] = useActionState(requestUsername, null, 'n/a');

  return (
    <>
      <form action={action}>
        <input type="text" name="username" />
        <button type="submit">Request</button>
      </form>
      <p>Last submission request returned: {state}</p>
    </>
  );
}
```

### form 외부에서 Server Function 호출

- Server Functions는 노출된 서버 엔드포인트이며 클라이언트 코드 어디서든 호출 가능
- form 외부에서 사용할 때는 Transition에서 호출해야 함 (로딩 인디케이터 표시, 낙관적 상태 업데이트, 예기치 않은 에러 처리 가능)

```js
import incrementLike from './actions';
import { useState, useTransition } from 'react';

function LikeButton() {
  const [isPending, startTransition] = useTransition();
  const [likeCount, setLikeCount] = useState(0);

  const onClick = () => {
    startTransition(async () => {
      const currentCount = await incrementLike();
      startTransition(() => {
        setLikeCount(currentCount);
      });
    });
  };

  return (
    <>
      <p>Total Likes: {likeCount}</p>
      <button onClick={onClick} disabled={isPending}>Like</button>;
    </>
  );
}
```

```js
// actions.js
'use server';

let likeCount = 0;
export default async function incrementLike() {
  likeCount++;
  return likeCount;
}
```

---

# 2. Rules of React

## 2.1 Rules of React (개요)

> 원문: https://react.dev/reference/rules

- 프로그래밍 언어마다 개념을 표현하는 고유한 방식이 있듯이, React도 패턴을 쉽게 이해하고 고품질 애플리케이션을 만들기 위한 고유한 관용구(규칙)가 있음
- 관용적인 React 코드를 작성하면 잘 조직되고 안전하며 조합 가능한 애플리케이션을 만들 수 있음
- 이러한 속성이 앱을 변경에 더 탄력적으로 만들고 다른 개발자, 라이브러리, 도구와의 작업을 쉽게 함
- 이것들은 가이드라인이 아닌 규칙(rules)임. 위반하면 앱에 버그가 발생할 가능성이 높음
- Strict Mode와 React의 ESLint 플러그인을 함께 사용하는 것을 강력히 권장함

### 세 가지 핵심 규칙

- Components and Hooks must be pure (컴포넌트와 Hooks는 순수해야 함)
- React calls Components and Hooks (React가 컴포넌트와 Hooks를 호출함)
- Rules of Hooks (Hooks의 규칙)

---

## 2.2 Components and Hooks Must Be Pure

> 원문: https://react.dev/reference/rules/components-and-hooks-must-be-pure

- 순수 함수는 계산만 수행하고 그 외 아무것도 하지 않음
- 코드를 이해하고 디버깅하기 쉬우며, React가 컴포넌트와 Hooks를 자동으로 올바르게 최적화할 수 있게 함

### 순수성이 중요한 이유

- 순수한 컴포넌트나 Hook:
  - 멱등성(Idempotent): 같은 입력으로 항상 같은 결과를 얻음
  - 렌더 중 부작용 없음: 부작용이 있는 코드는 렌더링과 별도로 실행되어야 함
  - 비로컬 값을 변경하지 않음
- 렌더가 순수하면 React가 어떤 업데이트를 사용자에게 먼저 보여줄지 우선순위를 정할 수 있음
- 렌더 순수성 덕분에 렌더링 로직을 여러 번 실행해도 안전함

### React의 코드 실행 방식

- 렌더링(Rendering): 다음 버전의 UI가 어떻게 보여야 하는지 계산하는 것
- 렌더링 후 React가 이전 UI와 비교하여 최소한의 DOM 변경만 적용함(commit)
- 이후 Effects가 실행됨(flush)
- 렌더 중 실행되는 코드 판별법: 최상위 레벨에 작성된 코드는 렌더 중 실행될 가능성이 높음

```js
function Dropdown() {
  const selectedItems = new Set(); // created during render
  // ...
}
```

- 이벤트 핸들러와 Effects는 렌더 중에 실행되지 않음

```js
function Dropdown() {
  const selectedItems = new Set();
  const onSelect = (item) => {
    // this code is in an event handler, so it's only run when the user triggers this
    selectedItems.add(item);
  }
}
```

```js
function Dropdown() {
  const selectedItems = new Set();
  useEffect(() => {
    // this code is inside of an Effect, so it only runs after rendering
    logForAnalytics(selectedItems);
  }, [selectedItems]);
}
```

### 컴포넌트와 Hooks는 멱등해야 함

- 컴포넌트는 입력(props, state, context)에 대해 항상 같은 출력을 반환해야 함
- 렌더 중 실행되는 모든 코드도 멱등이어야 함

```js
function Clock() {
  const time = new Date(); // Bad: always returns a different result!
  return <span>{time.toLocaleString()}</span>
}
```

- `new Date()`는 멱등이 아님. `Math.random()`도 마찬가지임
- 비멱등 함수를 아예 사용하지 말라는 것이 아니라 렌더 중에 사용하지 말라는 것임
- Effect를 사용하여 최신 날짜를 동기화할 수 있음:

```js
import { useState, useEffect } from 'react';

function useTime() {
  const [time, setTime] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => {
      setTime(new Date());
    }, 1000);
    return () => clearInterval(id);
  }, []);

  return time;
}

export default function Clock() {
  const time = useTime();
  return <span>{time.toLocaleString()}</span>;
}
```

### 부작용은 렌더 외부에서 실행되어야 함

- 부작용은 렌더 중에 실행되면 안 됨 (React가 최적의 사용자 경험을 위해 컴포넌트를 여러 번 렌더링할 수 있으므로)
- 대부분의 경우 이벤트 핸들러에서 부작용을 처리하고, 최후의 수단으로만 `useEffect` 사용

### 로컬 변이(mutation)는 허용됨

```js
function FriendList({ friends }) {
  const items = []; // locally created
  for (let i = 0; i < friends.length; i++) {
    const friend = friends[i];
    items.push(
      <Friend key={friend.id} friend={friend} />
    ); // local mutation is okay
  }
  return <section>{items}</section>;
}
```

- 컴포넌트 내부에서 로컬로 생성된 값의 변이는 완전히 허용됨
- 렌더링마다 `items`가 새로 생성되므로 문제없음

### 지연 초기화도 허용됨

```js
function ExpenseForm() {
  SuperCalculator.initializeIfNotReady(); // Good: if it doesn't affect other components
  // Continue rendering...
}
```

### DOM 변경은 허용되지 않음

```js
function ProductDetailPage({ product }) {
  document.title = product.title; // Bad: Changes the DOM
}
```

- 렌더 로직에서 사용자에게 직접 보이는 부작용은 허용되지 않음

### Props와 state는 불변임

- 컴포넌트의 props와 state는 단일 렌더에 대한 불변 스냅샷임
- 직접 변이하지 말고, 새 props를 전달하거나 `useState`의 setter 함수를 사용해야 함

```js
function Post({ item }) {
  item.url = new Url(item.url, base); // Bad: never mutate props directly
  return <Link url={item.url}>{item.title}</Link>;
}
```

```js
function Post({ item }) {
  const url = new Url(item.url, base); // Good: make a copy instead
  return <Link url={url}>{item.title}</Link>;
}
```

### Hooks의 반환값과 인수도 불변임

- Hook에 전달된 값은 수정하면 안 됨
- JSX의 props처럼 Hook에 전달된 값은 불변이 됨

```js
function useIconStyle(icon) {
  const theme = useContext(ThemeContext);
  if (icon.enabled) {
    icon.className = computeStyle(icon, theme); // Bad: never mutate hook arguments directly
  }
  return icon;
}
```

```js
function useIconStyle(icon) {
  const theme = useContext(ThemeContext);
  const newIcon = { ...icon }; // Good: make a copy instead
  if (icon.enabled) {
    newIcon.className = computeStyle(icon, theme);
  }
  return newIcon;
}
```

- Hook의 인수를 변이하면 커스텀 Hook의 메모이제이션이 올바르게 동작하지 않음

```js
style = useIconStyle(icon);         // `style` is memoized based on `icon`
icon.enabled = false;               // Bad: never mutate hook arguments directly
style = useIconStyle(icon);         // previously memoized result is returned
```

```js
style = useIconStyle(icon);         // `style` is memoized based on `icon`
icon = { ...icon, enabled: false }; // Good: make a copy instead
style = useIconStyle(icon);         // new value of `style` is calculated
```

### JSX에 전달된 후에는 값을 변이하면 안 됨

```js
function Page({ colour }) {
  const styles = { colour, size: "large" };
  const header = <Header styles={styles} />;
  styles.size = "small"; // Bad: styles was already used in the JSX above
  const footer = <Footer styles={styles} />;
  return (
    <>
      {header}
      <Content />
      {footer}
    </>
  );
}
```

```js
function Page({ colour }) {
  const headerStyles = { colour, size: "large" };
  const header = <Header styles={headerStyles} />;
  const footerStyles = { colour, size: "small" }; // Good: we created a new value
  const footer = <Footer styles={footerStyles} />;
  return (
    <>
      {header}
      <Content />
      {footer}
    </>
  );
}
```

---

## 2.3 React Calls Components and Hooks

> 원문: https://react.dev/reference/rules/react-calls-components-and-hooks

- React가 사용자 경험을 최적화하기 위해 필요할 때 컴포넌트와 Hooks를 렌더링하는 책임을 짐
- 선언적(declarative)임: 컴포넌트 로직에서 무엇을 렌더링할지 알려주면, React가 사용자에게 최적으로 표시하는 방법을 결정함

### 컴포넌트 함수를 직접 호출하지 말 것

- 컴포넌트는 JSX에서만 사용해야 하며, 일반 함수처럼 호출하면 안 됨

```js
function BlogPost() {
  return <Layout><Article /></Layout>; // Good: Only use components in JSX
}
```

```js
function BlogPost() {
  return <Layout>{Article()}</Layout>; // Bad: Never call them directly
}
```

- React가 렌더링을 조율하면 여러 이점이 있음:
  - 컴포넌트가 함수 이상이 됨: Hooks를 통한 로컬 state 등 기능을 추가할 수 있음
  - 컴포넌트 타입이 재조정(reconciliation)에 참여함
  - React가 사용자 경험을 향상시킬 수 있음 (큰 컴포넌트 트리 리렌더링 시 메인 스레드 차단 방지)
  - 더 나은 디버깅 이야기
  - 더 효율적인 재조정: 트리에서 어떤 컴포넌트가 리렌더링 필요한지 결정하고 불필요한 것은 건너뜀

### Hooks를 일반 값으로 전달하지 말 것

- Hooks는 컴포넌트나 Hooks 내부에서만 호출해야 하며, 일반 값으로 전달하면 안 됨
- 로컬 추론(local reasoning) 가능: 개발자가 컴포넌트를 단독으로 보고 모든 동작을 이해할 수 있음

### Hook을 동적으로 변이하지 말 것

- 고차 Hooks를 작성하면 안 됨:

```js
function ChatInput() {
  const useDataWithLogging = withLogging(useData); // Bad: don't write higher order Hooks
  const data = useDataWithLogging();
}
```

```js
function ChatInput() {
  const data = useDataWithLogging(); // Good: Create a new version of the Hook
}

function useDataWithLogging() {
  // ... Create a new version of the Hook and inline the logic here
}
```

### Hook을 동적으로 사용하지 말 것

- Hook을 값으로 전달하여 의존성 주입을 하면 안 됨:

```js
function ChatInput() {
  return <Button useData={useDataWithLogging} /> // Bad: don't pass Hooks as props
}
```

```js
function ChatInput() {
  return <Button />
}

function Button() {
  const data = useDataWithLogging(); // Good: Use the Hook directly
}
```

---

## 2.4 Rules of Hooks

> 원문: https://react.dev/reference/rules/rules-of-hooks

- Hooks는 JavaScript 함수로 정의되지만 호출 위치에 제한이 있는 특별한 유형의 재사용 가능한 UI 로직임

### 최상위 레벨에서만 Hooks를 호출할 것

- `use`로 시작하는 함수가 React에서 Hooks임
- 반복문, 조건문, 중첩 함수, `try`/`catch`/`finally` 블록 안에서 Hooks를 호출하면 안 됨
- 항상 React 함수의 최상위 레벨에서, 조기 반환(early return) 전에 사용해야 함

```js
function Counter() {
  // Good: top-level in a function component
  const [count, setCount] = useState(0);
  // ...
}

function useWindowWidth() {
  // Good: top-level in a custom Hook
  const [width, setWidth] = useState(window.innerWidth);
  // ...
}
```

- 지원되지 않는 경우:
  - 조건문이나 반복문 안에서 호출
  - 조건부 `return` 문 뒤에서 호출
  - 이벤트 핸들러에서 호출
  - 클래스 컴포넌트에서 호출
  - `useMemo`, `useReducer`, `useEffect`에 전달된 함수 안에서 호출
  - `try`/`catch`/`finally` 블록 안에서 호출

```js
function Bad({ cond }) {
  if (cond) {
    // Bad: inside a condition (to fix, move it outside!)
    const theme = useContext(ThemeContext);
  }
  // ...
}

function Bad() {
  for (let i = 0; i < 10; i++) {
    // Bad: inside a loop (to fix, move it outside!)
    const theme = useContext(ThemeContext);
  }
  // ...
}

function Bad({ cond }) {
  if (cond) {
    return;
  }
  // Bad: after a conditional return (to fix, move it before the return!)
  const theme = useContext(ThemeContext);
  // ...
}

function Bad() {
  function handleClick() {
    // Bad: inside an event handler (to fix, move it outside!)
    const theme = useContext(ThemeContext);
  }
  // ...
}

function Bad() {
  const style = useMemo(() => {
    // Bad: inside useMemo (to fix, move it outside!)
    const theme = useContext(ThemeContext);
    return createStyle(theme);
  });
  // ...
}

class Bad extends React.Component {
  render() {
    // Bad: inside a class component (to fix, write a function component instead of a class!)
    useEffect(() => {})
    // ...
  }
}

function Bad() {
  try {
    // Bad: inside try/catch/finally block (to fix, move it outside!)
    const [x, setX] = useState(0);
  } catch {
    const [x, setX] = useState(1);
  }
}
```

- `eslint-plugin-react-hooks` 플러그인을 사용하여 이러한 실수를 잡을 수 있음
- 커스텀 Hooks는 다른 Hooks를 호출할 수 있음 (커스텀 Hooks도 함수 컴포넌트가 렌더링되는 동안에만 호출되어야 하므로)

### React 함수에서만 Hooks를 호출할 것

- 일반 JavaScript 함수에서 Hooks를 호출하면 안 됨
- React 함수 컴포넌트에서 호출하거나 커스텀 Hooks에서 호출해야 함

---

# 3. React Compiler

## 3.1 소개

> 원문: https://react.dev/learn/react-compiler/introduction

- React Compiler는 빌드 타임에 React 앱을 자동으로 최적화하는 새로운 빌드 도구임
- 일반 JavaScript와 함께 작동하며, Rules of React를 이해하므로 코드를 다시 작성할 필요가 없음

### React Compiler가 하는 일

- 빌드 타임에 자동으로 React 애플리케이션을 최적화함
- 수동 메모이제이션이 번거롭고, 실수하기 쉬우며, 추가 코드 유지보수 부담이 있었음
- React Compiler가 이 최적화를 자동으로 수행하여 개발자가 기능 개발에 집중할 수 있게 함

### Compiler 이전 (수동 메모이제이션)

```js
import { useMemo, useCallback, memo } from 'react';

const ExpensiveComponent = memo(function ExpensiveComponent({ data, onClick }) {
  const processedData = useMemo(() => {
    return expensiveProcessing(data);
  }, [data]);

  const handleClick = useCallback((item) => {
    onClick(item.id);
  }, [onClick]);

  return (
    <div>
      {processedData.map(item => (
        <Item key={item.id} onClick={() => handleClick(item)} />
      ))}
    </div>
  );
});
```

- 위 수동 메모이제이션에는 미묘한 버그가 있음: `() => handleClick(item)` 화살표 함수가 렌더마다 새로 생성되어 `Item`의 메모이제이션을 깨뜨림
- React Compiler는 화살표 함수가 있든 없든 이를 올바르게 최적화함

### Compiler 이후

```js
function ExpensiveComponent({ data, onClick }) {
  const processedData = expensiveProcessing(data);

  const handleClick = (item) => {
    onClick(item.id);
  };

  return (
    <div>
      {processedData.map(item => (
        <Item key={item.id} onClick={() => handleClick(item)} />
      ))}
    </div>
  );
}
```

### Compiler가 추가하는 메모이제이션 종류

- 업데이트 성능 개선(기존 컴포넌트 리렌더링)에 주로 초점을 맞춤
- 두 가지 주요 사용 사례:
  1. 컴포넌트의 연쇄적 리렌더링 건너뛰기: `<Parent />`를 리렌더링하면 변경되지 않은 자식도 리렌더링됨
  2. React 외부의 비용이 큰 계산 건너뛰기

```js
function FriendList({ friends }) {
  const onlineCount = useFriendOnlineCount();
  if (friends.length === 0) {
    return <NoFriends />;
  }
  return (
    <div>
      <span>{onlineCount} online</span>
      {friends.map((friend) => (
        <FriendListCard key={friend.id} friend={friend} />
      ))}
      <MessageButton />
    </div>
  );
}
```

- Compiler가 자동으로 수동 메모이제이션에 해당하는 것을 적용하여 state 변경 시 관련 부분만 리렌더링함

### 비용이 큰 계산도 메모이제이션됨

```js
// **Not** memoized by React Compiler, since this is not a component or hook
function expensivelyProcessAReallyLargeArrayOfObjects() { /* ... */ }

// Memoized by React Compiler since this is a component
function TableContainer({ items }) {
  // This function call would be memoized:
  const data = expensivelyProcessAReallyLargeArrayOfObjects(items);
  // ...
}
```

- React Compiler는 React 컴포넌트와 Hooks만 메모이제이션하며, 모든 함수를 메모이제이션하지 않음
- 메모이제이션이 여러 컴포넌트/Hooks 간에 공유되지 않음

### Compiler를 시도해야 하는가

- 모든 사용자가 사용하기 시작하는 것을 권장함
- 현재는 선택적 추가 사항이지만, 향후 일부 기능은 Compiler가 필요할 수 있음
- Meta 같은 기업에서 프로덕션에서 광범위하게 테스트됨

### useMemo, useCallback, React.memo를 어떻게 해야 하는가

- 기본적으로 Compiler가 분석과 휴리스틱에 기반해 코드를 메모이제이션함
- `useMemo`와 `useCallback`은 메모이제이션할 값을 정밀하게 제어하기 위한 이스케이프 해치로 계속 사용 가능
- 새 코드에서는 Compiler의 메모이제이션에 의존하고, 정밀한 제어가 필요할 때 `useMemo`/`useCallback` 사용을 권장함
- 기존 코드에서는 기존 메모이제이션을 그대로 두거나 신중하게 테스트 후 제거하는 것을 권장함

---

## 3.2 설치

> 원문: https://react.dev/learn/react-compiler/installation

### 전제 조건

- React 19에서 최적으로 작동하도록 설계되었지만, React 17과 18도 지원함

### 설치

```
npm install -D babel-plugin-react-compiler@latest
```

### 기본 설정

- 대부분의 경우 별도의 설정 없이 작동하도록 설계됨
- React Compiler는 Babel 플러그인 파이프라인에서 가장 먼저 실행되어야 함. Compiler가 적절한 분석을 위해 원본 소스 정보가 필요함

### Babel

```js
module.exports = {
  plugins: [
    'babel-plugin-react-compiler', // must run first!
    // ... other plugins
  ],
  // ... other config
};
```

### Vite

- `@vitejs/plugin-react` 6.0.0 이상에서 `reactCompilerPreset` 사용 가능:

```js
// vite.config.js
import { defineConfig } from 'vite';
import react, { reactCompilerPreset } from '@vitejs/plugin-react';
import babel from '@rolldown/plugin-babel';

export default defineConfig({
  plugins: [
    react(),
    babel({
      presets: [reactCompilerPreset()]
    }),
  ],
});
```

- 이전 버전의 `@vitejs/plugin-react`:

```js
// vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: ['babel-plugin-react-compiler'],
      },
    }),
  ],
});
```

### React Router

```js
// vite.config.js
import { defineConfig } from "vite";
import babel from "vite-plugin-babel";
import { reactRouter } from "@react-router/dev/vite";

const ReactCompilerConfig = { /* ... */ };

export default defineConfig({
  plugins: [
    reactRouter(),
    babel({
      filter: /\.[jt]sx?$/,
      babelConfig: {
        presets: ["@babel/preset-typescript"], // if you use TypeScript
        plugins: [
          ["babel-plugin-react-compiler", ReactCompilerConfig],
        ],
      },
    }),
  ],
});
```

### 기타 빌드 도구

- Next.js: Next.js 문서 참조
- Webpack: 커뮤니티 webpack 로더 사용 가능
- Expo: Expo 문서 참조
- Metro (React Native): Babel 설정 참조
- Rspack, Rsbuild: 각 문서 참조

### ESLint 통합

- React Compiler에는 최적화할 수 없는 코드를 식별하는 ESLint 규칙이 포함됨
- ESLint 규칙이 에러를 보고하면 Compiler가 해당 컴포넌트/Hook 최적화를 건너뜀 (안전함)
- 모든 위반을 즉시 수정할 필요 없음. 자체 속도로 해결하며 최적화되는 컴포넌트 수를 점진적으로 늘릴 수 있음

```
npm install -D eslint-plugin-react-hooks@latest
```

### 설정 확인

- React DevTools에서 Compiler로 최적화된 컴포넌트에 "Memo" 배지가 표시됨
- 빌드 출력에서 `react/compiler-runtime`의 `_c` 임포트를 포함한 자동 메모이제이션 로직을 확인할 수 있음

```js
import { c as _c } from "react/compiler-runtime";
export default function MyApp() {
  const $ = _c(1);
  let t0;
  if ($[0] === Symbol.for("react.memo_cache_sentinel")) {
    t0 = <div>Hello World</div>;
    $[0] = t0;
  } else {
    t0 = $[0];
  }
  return t0;
}
```

### 특정 컴포넌트 옵트아웃

- 컴파일 후 문제가 발생하면 `"use no memo"` 디렉티브로 특정 컴포넌트의 최적화를 건너뛸 수 있음

```js
function ProblematicComponent() {
  "use no memo";
  // Component code here
}
```

- 근본 원인을 해결한 후 디렉티브를 제거해야 함

---

## 3.3 설정(Configuration)

> 원문: https://react.dev/reference/react-compiler/configuration

- 대부분의 앱에서 기본 옵션이 바로 작동하도록 되어 있음
- 특별한 필요가 있을 때만 고급 옵션을 사용함

### 기본 설정

```js
// babel.config.js
module.exports = {
  plugins: [
    [
      'babel-plugin-react-compiler', {
        // compiler options
      }
    ]
  ]
};
```

### compilationMode

- 컴파일할 함수를 선택하는 전략을 제어함 (전체 함수, 주석이 달린 함수만, 지능적 감지)

```js
{
  compilationMode: 'annotation' // Only compile "use memo" functions
}
```

### target

- 사용 중인 React 버전(17, 18, 19)을 지정함

```js
// For React 18 projects
{
  target: '18' // Also requires react-compiler-runtime package
}
```

### panicThreshold

- 에러 발생 시 빌드를 실패시킬지 문제 있는 컴포넌트를 건너뛸지 결정함

```js
// Recommended for production
{
  panicThreshold: 'none' // Skip components with errors instead of failing the build
}
```

### logger

- 컴파일 이벤트에 대한 커스텀 로깅을 제공함

```js
{
  logger: {
    logEvent(filename, event) {
      if (event.kind === 'CompileSuccess') {
        console.log('Compiled:', filename);
      }
    }
  }
}
```

### gating

- A/B 테스트나 점진적 롤아웃을 위한 런타임 피처 플래그를 활성화함

```js
{
  gating: {
    source: 'my-feature-flags',
    importSpecifierName: 'isCompilerEnabled'
  }
}
```

### 일반적인 설정 패턴

- 기본 설정 (React 19): 설정 없이 작동

```js
module.exports = {
  plugins: [
    'babel-plugin-react-compiler'
  ]
};
```

- React 17/18 프로젝트: 런타임 패키지와 target 설정 필요

```bash
npm install react-compiler-runtime@latest
```

```js
{
  target: '18' // or '17'
}
```

- 점진적 도입: `compilationMode: 'annotation'`으로 `"use memo"` 함수만 컴파일

---

# 4. Legacy APIs

## 4.1 Component

> 원문: https://react.dev/reference/react/Component

- `Component`는 JavaScript 클래스로 정의된 React 컴포넌트의 기본 클래스임
- 클래스 컴포넌트는 여전히 React에서 지원되지만 새 코드에서는 사용을 권장하지 않음
- 함수로 컴포넌트를 정의하는 것을 권장함

```js
class Greeting extends Component {
  render() {
    return <h1>Hello, {this.props.name}!</h1>;
  }
}
```

### Component 클래스

- `Component`를 확장하고 `render` 메서드를 정의하여 React 컴포넌트를 클래스로 정의함
- `render` 메서드만 필수이고, 나머지 메서드는 선택임

```js
import { Component } from 'react';

class Greeting extends Component {
  render() {
    return <h1>Hello, {this.props.name}!</h1>;
  }
}
```

### context

- `this.context`로 클래스 컴포넌트의 context에 접근할 수 있음
- `static contextType`으로 읽을 context를 지정해야 함
- 한 번에 하나의 context만 읽을 수 있음
- 함수 컴포넌트의 `useContext`에 해당함

### props

- 클래스 컴포넌트에 전달된 props는 `this.props`로 접근 가능
- 함수 컴포넌트에서 props를 선언하는 것에 해당함

### state

- `this.state`로 클래스 컴포넌트의 state에 접근 가능
- `state` 필드는 객체여야 함
- state를 직접 변이하면 안 되며 `setState`를 사용해야 함
- 함수 컴포넌트의 `useState`에 해당함

```js
class Counter extends Component {
  state = {
    age: 42,
  };

  handleAgeChange = () => {
    this.setState({
      age: this.state.age + 1
    });
  };

  render() {
    return (
      <>
        <button onClick={this.handleAgeChange}>
        Increment age
        </button>
        <p>You are {this.state.age}.</p>
      </>
    );
  }
}
```

### constructor(props)

- 클래스 컴포넌트가 마운트되기 전에 실행됨
- 주로 state 선언과 클래스 메서드 바인딩에 사용됨
- `super(props)`를 다른 모든 문장보다 먼저 호출해야 함
- 부작용이나 구독을 포함하면 안 됨
- `this.state`를 직접 할당할 수 있는 유일한 곳임

```js
class Counter extends Component {
  constructor(props) {
    super(props);
    this.state = { counter: 0 };
    this.handleClick = this.handleClick.bind(this);
  }

  handleClick() {
    // ...
  }
```

- 모던 JavaScript 문법(public class field)으로 대체 가능:

```js
class Counter extends Component {
  state = { counter: 0 };

  handleClick = () => {
    // ...
  }
```

- Strict Mode에서 React가 constructor를 두 번 호출하여 부작용을 감지함

### componentDidMount()

- 컴포넌트가 화면에 추가(마운트)될 때 호출됨
- 데이터 가져오기, 구독 설정, DOM 노드 조작을 위한 일반적인 위치임
- 구현 시 `componentDidUpdate`와 `componentWillUnmount`도 함께 구현해야 버그를 방지할 수 있음
- 함수 컴포넌트의 `useEffect`에 해당함

```js
class ChatRoom extends Component {
  state = {
    serverUrl: 'https://localhost:1234'
  };

  componentDidMount() {
    this.setupConnection();
  }

  componentDidUpdate(prevProps, prevState) {
    if (
      this.props.roomId !== prevProps.roomId ||
      this.state.serverUrl !== prevState.serverUrl
    ) {
      this.destroyConnection();
      this.setupConnection();
    }
  }

  componentWillUnmount() {
    this.destroyConnection();
  }

  // ...
}
```

### componentDidUpdate(prevProps, prevState, snapshot?)

- 업데이트된 props나 state로 리렌더링된 직후 호출됨
- 초기 렌더링에는 호출되지 않음
- 업데이트 후 DOM 조작이나 네트워크 요청에 사용함
- `shouldComponentUpdate`가 false를 반환하면 호출되지 않음
- `this.props`와 `prevProps`, `this.state`와 `prevState` 비교 조건문으로 감싸야 무한 루프 방지

### componentWillUnmount()

- 컴포넌트가 화면에서 제거(언마운트)되기 전에 호출됨
- 데이터 가져오기 취소나 구독 제거를 위한 일반적인 위치임
- `componentDidMount`의 로직을 "미러링"해야 함

### forceUpdate(callback?)

- 컴포넌트를 강제로 리렌더링함
- 일반적으로 불필요하며, 외부 데이터 소스에서 직접 읽는 경우에만 사용
- 함수 컴포넌트의 `useSyncExternalStore`로 대체됨

### getSnapshotBeforeUpdate(prevProps, prevState)

- React가 DOM을 업데이트하기 직전에 호출됨
- DOM에서 정보(예: 스크롤 위치)를 캡처할 수 있음
- 반환값이 `componentDidUpdate`의 세 번째 매개변수로 전달됨

```js
class ScrollingList extends React.Component {
  constructor(props) {
    super(props);
    this.listRef = React.createRef();
  }

  getSnapshotBeforeUpdate(prevProps, prevState) {
    if (prevProps.list.length < this.props.list.length) {
      const list = this.listRef.current;
      return list.scrollHeight - list.scrollTop;
    }
    return null;
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (snapshot !== null) {
      const list = this.listRef.current;
      list.scrollTop = list.scrollHeight - snapshot;
    }
  }

  render() {
    return (
      <div ref={this.listRef}>{/* ...contents... */}</div>
    );
  }
}
```

### render()

- 클래스 컴포넌트에서 유일한 필수 메서드임
- 순수 함수로 작성해야 함: props, state, context가 같으면 같은 결과를 반환해야 함
- 부작용을 포함하면 안 됨
- `<div />`같은 React 요소, 문자열, 숫자, 포탈, 빈 노드, React 노드 배열을 반환할 수 있음

### setState(nextState, callback?)

- React 컴포넌트의 state를 업데이트함
- state 변경을 큐에 넣고 이벤트 끝에 일괄 업데이트/리렌더링함
- 현재 실행 중인 코드에서 즉시 state를 변경하지 않음 (다음 렌더에 영향)
- 함수를 전달하여 이전 state 기반으로 업데이트할 수 있음

```js
  handleIncreaseAge = () => {
    this.setState(prevState => {
      return {
        age: prevState.age + 1
      };
    });
  }
```

- 여러 컴포넌트가 이벤트에 응답하여 state를 업데이트하면 React가 일괄 처리하여 이벤트 끝에 한 번에 리렌더링함

### shouldComponentUpdate(nextProps, nextState, nextContext)

- 리렌더링을 건너뛸 수 있는지 여부를 결정하기 위해 React가 호출함
- 성능 최적화 목적으로만 존재함
- `PureComponent`나 `memo`를 사용하는 것이 더 나은 대안임
- `JSON.stringify`나 깊은 동등성 검사를 사용하면 성능이 예측 불가능해지므로 권장하지 않음
- `false`를 반환해도 자식 컴포넌트의 state 변경 시 리렌더링을 막지 못함

### UNSAFE 라이프사이클 메서드

- `UNSAFE_componentWillMount`: 역사적 이유로만 존재. 대신 `constructor`나 `componentDidMount` 사용
- `UNSAFE_componentWillReceiveProps(nextProps)`: 역사적 이유로만 존재. 대신 `componentDidUpdate`나 `static getDerivedStateFromProps` 사용
- `UNSAFE_componentWillUpdate(nextProps, nextState)`: 역사적 이유로만 존재. 대신 `componentDidUpdate`나 `getSnapshotBeforeUpdate` 사용

### static contextType

- 클래스 컴포넌트에서 읽을 context를 지정함
- `createContext`로 생성된 값이어야 함
- 함수 컴포넌트의 `useContext`에 해당함

### static defaultProps

- `undefined`와 누락된 props에 대한 기본값을 설정함 (`null`에는 적용되지 않음)
- 함수 컴포넌트의 기본값과 유사함

```js
class Button extends Component {
  static defaultProps = {
    color: 'blue'
  };

  render() {
    return <button className={this.props.color}>click me</button>;
  }
}
```

### static getDerivedStateFromError(error)

- 자식 컴포넌트가 렌더링 중 에러를 던지면 호출됨
- 에러 메시지를 표시하기 위해 state를 업데이트할 수 있음
- `componentDidCatch`와 함께 사용하여 Error Boundary 구현
- 함수 컴포넌트에는 아직 직접적인 동등물이 없음

### static getDerivedStateFromProps(props, state)

- `render` 호출 직전에 초기 마운트와 이후 업데이트 모두에서 호출됨
- state를 업데이트하는 객체를 반환하거나, 업데이트할 것이 없으면 `null`을 반환함
- 드문 사용 사례용임: props 변경에 따라 state를 조정해야 할 때

```js
class Form extends Component {
  state = {
    email: this.props.defaultEmail,
    prevUserID: this.props.userID
  };

  static getDerivedStateFromProps(props, state) {
    if (props.userID !== state.prevUserID) {
      return {
        prevUserID: props.userID,
        email: props.defaultEmail
      };
    }
    return null;
  }

  // ...
}
```

### Error Boundary

- 렌더링 중 에러가 발생하면 React가 기본적으로 UI를 화면에서 제거함
- Error Boundary는 충돌한 부분 대신 폴백 UI를 표시하는 특수 컴포넌트임
- `static getDerivedStateFromError`와 `componentDidCatch`를 구현하여 만듦

```js
import * as React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    logErrorToMyService(
      error,
      info.componentStack,
      React.captureOwnerStack(),
    );
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback;
    }

    return this.props.children;
  }
}
```

```js
<ErrorBoundary fallback={<p>Something went wrong</p>}>
  <Profile />
</ErrorBoundary>
```

- Error Boundary가 잡지 못하는 에러: 이벤트 핸들러, SSR, Error Boundary 자체에서 던진 에러, 비동기 코드(`setTimeout`, `requestAnimationFrame` 콜백). 단 `useTransition`의 `startTransition` 내부에서 던진 에러는 잡힘
- 현재 함수 컴포넌트로 Error Boundary를 작성할 방법이 없음. `react-error-boundary` 패키지 사용 가능

### 클래스에서 함수로 마이그레이션

- 단순 컴포넌트: `this.props.name` 대신 구조 분해로 props를 직접 읽음
- state가 있는 컴포넌트: `this.state`/`this.setState` 대신 `useState` 사용
- 라이프사이클 메서드: `componentDidMount`/`componentDidUpdate`/`componentWillUnmount` 대신 `useEffect` 사용
- context: `this.context`/`static contextType` 대신 `useContext` 사용

---

## 4.2 PureComponent

> 원문: https://react.dev/reference/react/PureComponent

- 함수 대신 클래스로 컴포넌트를 정의하는 것은 권장되지 않음
- `PureComponent`는 `Component`와 유사하지만 같은 props와 state에 대해 리렌더링을 건너뜀
- `Component`를 확장하는 대신 `PureComponent`를 확장함

```js
import { PureComponent } from 'react';

class Greeting extends PureComponent {
  render() {
    return <h1>Hello, {this.props.name}!</h1>;
  }
}
```

- `Component`의 모든 API를 지원함
- 커스텀 `shouldComponentUpdate`를 정의하여 props와 state를 얕게 비교하는 것과 동일함

### 불필요한 리렌더링 건너뛰기

- React는 부모가 리렌더링하면 자식도 리렌더링하는 것이 기본임
- `PureComponent`를 확장하면 새 props와 state가 이전과 같을 때 리렌더링을 건너뜀
- 순수한 렌더링 로직을 가져야 함: props, state, context가 변경되지 않으면 같은 출력을 반환해야 함
- 사용 중인 context가 변경되면 여전히 리렌더링됨

### 함수 컴포넌트로 마이그레이션

- `PureComponent`를 `memo`로 감싸는 것으로 대체함

```js
import { memo, useState } from 'react';

const Greeting = memo(function Greeting({ name }) {
  console.log("Greeting was rendered at", new Date().toLocaleTimeString());
  return <h3>Hello{name && ', '}{name}!</h3>;
});
```

- `PureComponent`와 달리 `memo`는 새 state와 이전 state를 비교하지 않음
- 함수 컴포넌트에서는 같은 state로 `set` 함수를 호출하면 `memo` 없이도 기본적으로 리렌더링을 방지함

---

## 4.3 createElement

> 원문: https://react.dev/reference/react/createElement

- `createElement`는 React 요소를 생성함. JSX 작성의 대안으로 사용됨

```js
const element = createElement(type, props, ...children)
```

### createElement(type, props, ...children)

- `type`: 유효한 React 컴포넌트 타입. 태그 이름 문자열(`'div'`, `'span'`)이나 React 컴포넌트(함수, 클래스, Fragment 등)
- `props`: 객체 또는 `null`. `null`은 빈 객체와 동일하게 처리됨. `ref`와 `key`는 특별하며 반환된 요소에서 `element.props.ref`, `element.props.key`로 사용할 수 없고, `element.ref`와 `element.key`로 사용 가능
- `...children` (선택): 0개 이상의 자식 노드. React 요소, 문자열, 숫자, 포탈, 빈 노드, React 노드 배열

### 반환값

- `type`, `props`, `ref`, `key` 속성을 가진 React 요소 객체

### 주의사항

- React 요소와 props를 불변으로 취급해야 하며 생성 후 내용을 변경하면 안 됨
- JSX에서 커스텀 컴포넌트를 렌더링하려면 대문자로 시작해야 함
- 동적 자식은 배열로 세 번째 인수에 전달해야 함 (React가 누락된 `key`에 대해 경고함)

### JSX 없이 요소 생성

```js
import { createElement } from 'react';

function Greeting({ name }) {
  return createElement(
    'h1',
    { className: 'greeting' },
    'Hello ',
    createElement('i', null, name),
    '. Welcome!'
  );
}
```

- JSX에 해당하는 코드:

```js
function Greeting({ name }) {
  return (
    <h1 className="greeting">
      Hello <i>{name}</i>. Welcome!
    </h1>
  );
}
```

### React 요소란 정확히 무엇인가

- UI의 경량 설명(description)임
- `<Greeting name="Taylor" />`와 `createElement(Greeting, { name: 'Taylor' })`는 동일한 객체를 생성함:

```js
// Slightly simplified
{
  type: Greeting,
  props: {
    name: 'Taylor'
  },
  key: null,
  ref: null,
}
```

- 요소를 생성하는 것이 `Greeting` 컴포넌트를 렌더링하거나 DOM 요소를 생성하지 않음
- React에 나중에 무엇을 할지 알려주는 명령서(instruction)에 가까움
- 요소 생성은 매우 저렴하므로 최적화하거나 피할 필요 없음

---

## 4.4 forwardRef

> 원문: https://react.dev/reference/react/forwardRef

- React 19에서 `forwardRef`는 더 이상 필요하지 않음. `ref`를 prop으로 직접 전달할 수 있음
- `forwardRef`는 향후 릴리스에서 더 이상 사용되지 않을(deprecated) 예정임
- 컴포넌트가 ref를 사용하여 부모 컴포넌트에 DOM 노드를 노출할 수 있게 함

```js
const SomeComponent = forwardRef(render)
```

### forwardRef(render)

- `forwardRef()`를 호출하여 컴포넌트가 ref를 받아 자식 컴포넌트에 전달할 수 있게 함

```js
import { forwardRef } from 'react';

const MyInput = forwardRef(function MyInput(props, ref) {
  // ...
});
```

- `render`: 컴포넌트의 렌더 함수. React가 부모로부터 받은 props와 ref로 이 함수를 호출함
- 반환: JSX로 렌더링할 수 있는 React 컴포넌트. 일반 함수와 달리 `ref` prop을 받을 수 있음
- Strict Mode에서 렌더 함수를 두 번 호출함

### DOM 노드를 부모 컴포넌트에 노출하기

- 기본적으로 각 컴포넌트의 DOM 노드는 비공개임
- DOM 노드를 노출하려면 컴포넌트 정의를 `forwardRef()`로 감쌈

```js
import { forwardRef } from 'react';

const MyInput = forwardRef(function MyInput(props, ref) {
  const { label, ...otherProps } = props;
  return (
    <label>
      {label}
      <input {...otherProps} ref={ref} />
    </label>
  );
});
```

- 부모 컴포넌트에서 ref를 통해 DOM 노드에 접근 가능:

```js
function Form() {
  const ref = useRef(null);

  function handleClick() {
    ref.current.focus();
  }

  return (
    <form>
      <MyInput label="Enter your name:" ref={ref} />
      <button type="button" onClick={handleClick}>
        Edit
      </button>
    </form>
  );
}
```

### 여러 컴포넌트를 통한 ref 전달

- `forwardRef` 컴포넌트가 다른 `forwardRef` 컴포넌트에 ref를 전달할 수 있음
- 중첩된 컴포넌트 체인을 통해 최종 DOM 노드까지 ref가 전달됨

### 명령형 핸들(imperative handle) 노출

- 전체 DOM 노드 대신 제한된 메서드 집합을 가진 커스텀 객체를 노출할 수 있음
- `useImperativeHandle`을 사용함

```js
import { forwardRef, useRef, useImperativeHandle } from 'react';

const MyInput = forwardRef(function MyInput(props, ref) {
  const inputRef = useRef(null);

  useImperativeHandle(ref, () => {
    return {
      focus() {
        inputRef.current.focus();
      },
      scrollIntoView() {
        inputRef.current.scrollIntoView();
      },
    };
  }, []);

  return <input {...props} ref={inputRef} />;
});
```

- ref 남용 주의: 스크롤, 포커스, 애니메이션 트리거, 텍스트 선택 등 props로 표현할 수 없는 명령형 동작에만 사용해야 함
- prop으로 표현할 수 있는 것은 ref를 사용하면 안 됨

### 문제 해결

- `forwardRef`로 감쌌는데 ref가 항상 `null`인 경우: 받은 ref를 실제로 DOM 노드나 다른 컴포넌트에 전달하지 않은 것이 원인
- 조건부 로직으로 ref가 전달되지 않을 수 있음

---

## 4.5 Children

> 원문: https://react.dev/reference/react/Children

- `Children` API는 `children` prop으로 받은 JSX를 조작하고 변환할 수 있게 함
- `Children` 사용은 드물며 취약한 코드로 이어질 수 있음

```js
const mappedChildren = Children.map(children, child =>
  <div className="Row">
    {child}
  </div>
);
```

### Children.count(children)

- `children` 데이터 구조 내의 자식 수를 셈
- 빈 노드(`null`, `undefined`, Boolean), 문자열, 숫자, React 요소가 개별 노드로 카운트됨
- 배열은 개별 노드로 카운트되지 않지만 배열의 자식은 카운트됨
- React 요소 내부로 순회하지 않음. Fragment도 순회하지 않음

### Children.forEach(children, fn, thisArg?)

- `children` 데이터 구조의 각 자식에 대해 코드를 실행함
- `undefined`를 반환함

### Children.map(children, fn, thisArg?)

- `children` 데이터 구조의 각 자식을 매핑/변환함
- `children`이 `null`이나 `undefined`이면 같은 값을 반환함
- 그 외에는 `fn` 함수에서 반환한 노드의 플랫 배열을 반환함 (`null`과 `undefined` 제외)
- 반환된 요소의 key가 `children`의 원래 항목의 key와 자동으로 결합됨

### Children.only(children)

- `children`이 단일 React 요소인지 확인함
- 유효한 요소이면 해당 요소를 반환하고, 그렇지 않으면 에러를 던짐
- 배열을 전달하면 항상 에러를 던짐

### Children.toArray(children)

- `children` 데이터 구조를 일반 JavaScript 배열로 변환함
- `filter`, `sort`, `reverse` 같은 배열 메서드를 사용할 수 있음
- 빈 노드는 생략됨
- 원래 요소의 key에서 중첩 수준과 위치를 고려하여 key가 계산됨

### children prop이 항상 배열이 아닌 이유

- React에서 `children` prop은 불투명(opaque) 데이터 구조로 간주됨
- 내부적으로 배열로 표현되는 경우가 많지만, 단일 자식이면 불필요한 메모리 오버헤드를 피하기 위해 배열을 생성하지 않음
- `Children` 메서드를 사용하는 한 React가 실제 데이터 구조를 변경해도 코드가 깨지지 않음
- `children` 데이터 구조에는 전달된 컴포넌트의 렌더링 출력이 포함되지 않음

### 대안

- 여러 컴포넌트 노출: `Row` 같은 개별 컴포넌트를 export하여 수동으로 감싸기
- 객체 배열을 prop으로 받기: children 대신 구조화된 데이터 배열을 prop으로 전달

```js
<RowList rows={[
  { id: 'first', content: <p>This is the first item.</p> },
  { id: 'second', content: <p>This is the second item.</p> },
  { id: 'third', content: <p>This is the third item.</p> }
]} />
```

- 렌더 prop 호출: JSX를 반환하는 함수(렌더 prop)를 전달
- 커스텀 Hook으로 로직 추출: 비시각적 로직을 Hook으로 추출하여 여러 컴포넌트 간 재사용

---

## 4.6 cloneElement

> 원문: https://react.dev/reference/react/cloneElement

- `cloneElement`는 다른 요소를 시작점으로 사용하여 새 React 요소를 생성함
- 사용이 드물며 취약한 코드로 이어질 수 있음

```js
const clonedElement = cloneElement(element, props, ...children)
```

### cloneElement(element, props, ...children)

- `element`: 유효한 React 요소여야 함
- `props`: 객체 또는 `null`. `null`이면 원래 `element.props`를 모두 유지함. 그 외에는 `props` 객체의 값이 `element.props`보다 우선함
- `...children` (선택): 전달하지 않으면 원래 `element.props.children`이 유지됨

### 반환값

- `type`: `element.type`과 동일
- `props`: `element.props`와 전달된 `props`를 얕게 병합한 결과
- `ref`: 원래 `element.ref` (단 `props.ref`로 덮어쓸 수 있음)
- `key`: 원래 `element.key` (단 `props.key`로 덮어쓸 수 있음)

### 주의사항

- 원본 요소를 수정하지 않음
- 데이터 흐름을 추적하기 어렵게 만들므로 대안을 사용하는 것이 좋음

### 대안

- 렌더 prop으로 데이터 전달: `renderItem` 같은 렌더 prop 사용이 더 명시적임
- context를 통한 데이터 전달
- 커스텀 Hook으로 로직 추출

---

## 4.7 createRef

> 원문: https://react.dev/reference/react/createRef

- `createRef`는 임의의 값을 포함할 수 있는 ref 객체를 생성함
- 주로 클래스 컴포넌트에서 사용됨. 함수 컴포넌트에서는 `useRef`를 사용함

```js
class MyInput extends Component {
  inputRef = createRef();
  // ...
}
```

### createRef()

- 매개변수 없음
- `current` 속성을 가진 객체를 반환함. 초기값은 `null`임
- 항상 다른 객체를 반환함. `{ current: null }`을 직접 작성하는 것과 동일함
- 함수 컴포넌트에서는 `useRef`가 항상 같은 객체를 반환하므로 `useRef`를 사용해야 함

### 클래스 컴포넌트에서 ref 선언

```js
import { Component, createRef } from 'react';

export default class Form extends Component {
  inputRef = createRef();

  handleClick = () => {
    this.inputRef.current.focus();
  }

  render() {
    return (
      <>
        <input ref={this.inputRef} />
        <button onClick={this.handleClick}>
          Focus the input
        </button>
      </>
    );
  }
}
```

### 함수 컴포넌트로 마이그레이션

- `createRef` 호출을 `useRef` 호출로 대체함:

```js
import { useRef } from 'react';

export default function Form() {
  const inputRef = useRef(null);

  function handleClick() {
    inputRef.current.focus();
  }

  return (
    <>
      <input ref={inputRef} />
      <button onClick={handleClick}>
        Focus the input
      </button>
    </>
  );
}
```

---

## 4.8 isValidElement

> 원문: https://react.dev/reference/react/isValidElement

- 값이 React 요소인지 확인함

```js
const isElement = isValidElement(value)
```

### isValidElement(value)

- `value`가 React 요소이면 `true`, 아니면 `false`를 반환함

```js
import { isValidElement, createElement } from 'react';

// React elements
console.log(isValidElement(<p />)); // true
console.log(isValidElement(createElement('p'))); // true

// Not React elements
console.log(isValidElement(25)); // false
console.log(isValidElement('Hello')); // false
console.log(isValidElement({ age: 42 })); // false
```

### 주의사항

- JSX 태그와 `createElement`로 반환된 객체만 React 요소로 간주됨
- `42` 같은 숫자는 유효한 React 노드이지만 유효한 React 요소는 아님
- 배열과 `createPortal`로 생성된 포탈도 React 요소로 간주되지 않음

### React 요소 vs React 노드

- React 노드에는 다음이 포함됨:
  - `<div />`나 `createElement('div')`로 생성된 React 요소
  - `createPortal`로 생성된 포탈
  - 문자열, 숫자
  - `true`, `false`, `null`, `undefined` (표시되지 않음)
  - 다른 React 노드의 배열
- `isValidElement`는 React 노드가 아닌 React 요소인지를 확인함
- 렌더링 가능 여부를 확인하는 방법으로 사용하면 안 됨 (`42`는 유효한 React 노드이지만 React 요소가 아님)
- `isValidElement`가 필요한 경우는 매우 드물며, `cloneElement`처럼 요소만 받는 API를 호출할 때 에러를 피하려는 경우에 유용함
