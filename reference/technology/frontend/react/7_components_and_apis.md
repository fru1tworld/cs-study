# 컴포넌트와 API (Components and APIs)

---

# 내장 컴포넌트 (Built-in Components)

---

# Fragment

> 원문: https://react.dev/reference/react/Fragment

## 개요

- `<Fragment>` (축약형 `<>...</>`)는 래퍼 노드 없이 요소를 그룹화할 수 있게 해주는 컴포넌트임
- Canary 채널에서는 Fragment에 ref를 전달할 수 있음

## Props

- `key` (선택, string): 명시적 `<Fragment>` 구문으로 선언할 때만 사용 가능함. 리스트 렌더링 시 키 지정에 사용됨
- `ref` (선택, Canary): ref 객체(`useRef`) 또는 콜백 함수. React가 `FragmentInstance`를 ref 값으로 제공함

## 주의사항

- `key`를 전달하려면 `<>...</>` 축약 구문을 사용할 수 없음. `<Fragment key={yourKey}>...</Fragment>` 형태로 작성해야 함
- `ref`를 전달하려면 마찬가지로 명시적 `<Fragment ref={yourRef}>...</Fragment>` 구문이 필요함
- `<><Child /></>`에서 `[<Child />]`로, 또는 `<><Child /></>`에서 `<Child />`로 전환할 때 React는 상태를 초기화하지 않음. 한 단계 깊이에서만 동작함

## FragmentInstance API (Canary)

- Fragment에 `ref`를 전달하면 React가 `FragmentInstance` 객체를 제공함

### 이벤트 관리

- `addEventListener(type, listener, options?)`: 모든 첫 번째 수준 DOM 자식에 이벤트 리스너를 추가함. `undefined` 반환
- `removeEventListener(type, listener, options?)`: 모든 첫 번째 수준 DOM 자식에서 이벤트 리스너를 제거함. `undefined` 반환
- `dispatchEvent(event)`: Fragment에 이벤트를 디스패치함. `boolean` 반환 -- `preventDefault()`가 호출되면 `false`, 아니면 `true`

### 포커스 관리

- `focus(options?)`: 첫 번째 포커스 가능한 DOM 노드에 포커스함 (깊이 우선 탐색). `undefined` 반환
- `focusLast(options?)`: 마지막 포커스 가능한 DOM 노드에 포커스함 (깊이 우선, 역순). `undefined` 반환
- `blur()`: 활성 요소가 Fragment 내에 있으면 포커스를 제거함. `undefined` 반환

### 옵저버 관리

- `observeUsing(observer)`: 모든 첫 번째 수준 DOM 자식을 관찰 시작함. `IntersectionObserver` 또는 `ResizeObserver` 전달. `undefined` 반환
- `unobserveUsing(observer)`: 관찰을 중지함. `undefined` 반환

### DOM 쿼리

- `getClientRects()`: 모든 첫 번째 수준 DOM 자식의 바운딩 사각형을 평탄한 `Array<DOMRect>`로 반환함
- `getRootNode(options?)`: `Document | ShadowRoot | FragmentInstance` 반환
- `compareDocumentPosition(otherNode)`: 위치 플래그의 비트마스크를 반환함

### 스크롤

- `scrollIntoView(alignToTop?)`: 자식을 뷰로 스크롤함. `true`(기본값)이면 첫 번째 자식을 상단으로, `false`이면 마지막 자식을 하단으로 스크롤함. options 객체를 전달하면 에러가 발생함

### FragmentInstance 주의사항

- 자식 대상 메서드(`addEventListener`, `observeUsing`, `getClientRects`)는 첫 번째 수준 호스트(DOM) 자식에만 동작함
- `focus`와 `focusLast`는 중첩된 자식을 깊이 우선으로 탐색함
- `observeUsing`은 텍스트 노드에서 동작하지 않음
- `addEventListener`로 추가한 리스너는 숨겨진 `<Activity>` 트리에 적용되지 않음
- 각 첫 번째 수준 DOM 자식은 `reactFragments` 프로퍼티(`Set<FragmentInstance>`)를 갖게 됨

## 사용 예시

### 여러 요소 반환

```js
function Post() {
  return (
    <>
      <PostTitle />
      <PostBody />
    </>
  );
}
```

### 변수에 할당

```js
function CloseDialog() {
  const buttons = (
    <>
      <OKButton />
      <CancelButton />
    </>
  );
  return (
    <AlertDialog buttons={buttons}>
      Are you sure you want to leave this page?
    </AlertDialog>
  );
}
```

### 텍스트와 그룹화

```js
function DateRangePicker({ start, end }) {
  return (
    <>
      From
      <DatePicker date={start} />
      to
      <DatePicker date={end} />
    </>
  );
}
```

### 리스트에서 키 사용

```js
import { Fragment } from 'react';

const posts = [
  { id: 1, title: 'An update', body: "It's been a while since I posted..." },
  { id: 2, title: 'My new blog', body: 'I am starting a new blog!' }
];

export default function Blog() {
  return posts.map(post =>
    <Fragment key={post.id}>
      <PostTitle title={post.title} />
      <PostBody body={post.body} />
    </Fragment>
  );
}
```

### 래퍼 없이 이벤트 리스너 추가 (Canary)

```js
import { Fragment, useState, useRef, useEffect } from 'react';

function ClickableFragment({ children, onClick }) {
  const fragmentRef = useRef(null);
  useEffect(() => {
    const fragmentInstance = fragmentRef.current;
    if (fragmentInstance === null) {
      return;
    }
    fragmentInstance.addEventListener('click', onClick);
    return () => {
      fragmentInstance.removeEventListener('click', onClick);
    };
  }, [onClick])
  return (
    <Fragment ref={fragmentRef}>
      {children}
    </Fragment>
  );
}

export default function App() {
  const [clicks, setClicks] = useState(0);

  return (
    <>
      <p>Total clicks: {clicks}</p>
      <ClickableFragment onClick={() => {
        setClicks(c => c + 1);
      }}>
        <button>Button A</button>
        <button>Button B</button>
        <button>Button C</button>
      </ClickableFragment>
    </>
  );
}
```

### 포커스 관리 (Canary)

```js
import { Fragment, useRef } from 'react';

function FormFields({ children }) {
  const fragmentRef = useRef(null);

  return (
    <>
      <div className="buttons">
        <button onClick={() => {
          fragmentRef.current.focus();
        }}>
          Focus first
        </button>
        <button onClick={() => {
          fragmentRef.current.focusLast();
        }}>
          Focus last
        </button>
        <button onClick={() => {
          fragmentRef.current.blur();
        }}>
          Blur
        </button>
      </div>
      <Fragment ref={fragmentRef}>
        {children}
      </Fragment>
    </>
  );
}
```

---

# Profiler

> 원문: https://react.dev/reference/react/Profiler

## 개요

- `<Profiler>`는 React 트리의 렌더링 성능을 프로그래밍 방식으로 측정할 수 있게 해주는 컴포넌트임

```js
<Profiler id="App" onRender={onRender}>
  <App />
</Profiler>
```

## Props

- `id` (string): 측정 대상 UI 부분을 식별하는 문자열
- `onRender` (function): 프로파일된 트리 내 컴포넌트가 업데이트될 때마다 호출되는 콜백

## onRender 콜백

```js
function onRender(id, phase, actualDuration, baseDuration, startTime, commitTime) {
  // 렌더링 타이밍을 집계하거나 로깅함
}
```

### 매개변수

- `id` (string): 커밋한 `<Profiler>` 트리의 `id` prop
- `phase` (string): `"mount"`, `"update"`, `"nested-update"` 중 하나
- `actualDuration` (number): 현재 업데이트에 소요된 렌더링 시간(밀리초). 메모이제이션의 효과를 보여줌
- `baseDuration` (number): 최적화 없이 전체 서브트리를 렌더링할 때의 예상 시간(밀리초). 최악의 비용을 나타냄
- `startTime` (number): React가 렌더링을 시작한 시점의 숫자 타임스탬프
- `commitTime` (number): React가 업데이트를 커밋한 시점의 숫자 타임스탬프. 한 커밋의 모든 프로파일러 간 공유됨

## 주의사항

- 프로파일링은 오버헤드를 추가하며 프로덕션 빌드에서는 기본적으로 비활성화됨
- 프로덕션에서 사용하려면 프로파일링이 활성화된 특수 프로덕션 빌드가 필요함

## 사용 예시

### 서로 다른 부분 측정

```js
<App>
  <Profiler id="Sidebar" onRender={onRender}>
    <Sidebar />
  </Profiler>
  <Profiler id="Content" onRender={onRender}>
    <Content />
  </Profiler>
</App>
```

### 프로파일러 중첩

```js
<App>
  <Profiler id="Sidebar" onRender={onRender}>
    <Sidebar />
  </Profiler>
  <Profiler id="Content" onRender={onRender}>
    <Content>
      <Profiler id="Editor" onRender={onRender}>
        <Editor />
      </Profiler>
      <Preview />
    </Content>
  </Profiler>
</App>
```

---

# StrictMode

> 원문: https://react.dev/reference/react/StrictMode

## 개요

- `<StrictMode>`는 개발 환경에서만 동작하는 추가 검사와 경고를 활성화하여 일반적인 버그를 조기에 발견하도록 돕는 컴포넌트임
- Props 없음
- 프로덕션에서는 영향이 없음

```js
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

const root = createRoot(document.getElementById('root'));
root.render(
  <StrictMode>
    <App />
  </StrictMode>
);
```

## 개발 전용 검사 항목

- 이중 렌더링: 불순한 렌더링 버그를 찾기 위해 추가로 한 번 더 렌더링함
- Effect 재실행: 누락된 클린업을 찾기 위해 Effect를 추가로 한 번 더 실행함
- ref 콜백 재실행: 누락된 ref 클린업을 찾기 위해 ref 콜백을 추가로 한 번 더 실행함
- 지원 중단 API 감지: 지원 중단된 API 사용을 검사함

## StrictMode에서 두 번 호출되는 함수

- 컴포넌트 함수 본문 (최상위 로직만, 이벤트 핸들러 제외)
- `useState`, `set` 함수, `useMemo`, `useReducer`에 전달되는 함수
- 클래스 컴포넌트: `constructor`, `render`, `shouldComponentUpdate`

## 부분 적용

```js
function App() {
  return (
    <>
      <Header />
      <StrictMode>
        <main>
          <Sidebar />
          <Content />
        </main>
      </StrictMode>
      <Footer />
    </>
  );
}
```

## 주의사항

- `<StrictMode>`로 감싼 트리 내부에서 개별적으로 해제할 방법은 없음
- 루트가 아닌 일부에만 적용하면 프로덕션에서 가능한 동작만 활성화됨. 루트에 있지 않으면 초기 마운트 시 Effect가 재실행되지 않음

## 버그 예시: 불순한 렌더링

```js
// 잘못된 코드 -- 배열을 직접 변경함
export default function StoryTray({ stories }) {
  const items = stories;
  items.push({ id: 'create', label: 'Create Story' });
  return (
    <ul>
      {items.map(story => (
        <li key={story.id}>{story.label}</li>
      ))}
    </ul>
  );
}
```

```js
// 수정된 코드 -- 배열을 복사한 뒤 변경함
export default function StoryTray({ stories }) {
  const items = stories.slice();
  items.push({ id: 'create', label: 'Create Story' });
  return (
    <ul>
      {items.map(story => (
        <li key={story.id}>{story.label}</li>
      ))}
    </ul>
  );
}
```

## 버그 예시: 누락된 Effect 클린업

```js
// 잘못된 코드 -- 클린업 없음
useEffect(() => {
  const connection = createConnection(serverUrl, roomId);
  connection.connect();
}, [roomId]);
```

```js
// 수정된 코드 -- 클린업 추가
useEffect(() => {
  const connection = createConnection(serverUrl, roomId);
  connection.connect();
  return () => connection.disconnect();
}, [roomId]);
```

## 지원 중단 API 감지 대상

- `UNSAFE_componentWillMount`
- `UNSAFE_componentWillReceiveProps`
- `UNSAFE_componentWillUpdate`

---

# Suspense

> 원문: https://react.dev/reference/react/Suspense

## 개요

- `<Suspense>`는 자식 컴포넌트의 로딩이 완료될 때까지 폴백 UI를 표시할 수 있게 해주는 컴포넌트임

```js
<Suspense fallback={<Loading />}>
  <SomeComponent />
</Suspense>
```

## Props

- `children` (React 노드): 실제로 렌더링할 UI. 자식이 일시 중단(suspend)되면 폴백으로 전환됨
- `fallback` (React 노드): 로딩 스피너, 스켈레톤 등 대체 UI. 폴백 자체가 일시 중단되면 가장 가까운 부모 Suspense 경계가 활성화됨
- `defer` (실험적, boolean, 기본값 `false`): `true`이면 일시 중단이 발생하지 않아도 폴백을 먼저 보여주고 자식을 나중에 렌더링/스트리밍할 수 있음. 렌더링 비용이 큰 콘텐츠에 유용함

## Suspense 경계를 활성화하는 요소

- `lazy`로 지연 로딩되는 컴포넌트 코드
- `use`로 읽는 Promise (서버 컴포넌트 데이터, Suspense 지원 프레임워크 데이터 포함)
- `precedence` prop이 있는 `<link rel="stylesheet">`로 로딩되는 스타일시트 (타임아웃까지 차단)
- 스트리밍 SSR 중 대형 경계 HTML 로딩 대기
- (Canary) `<ViewTransition>` 업데이트 시 폰트 로딩 대기 (타임아웃까지)
- (Canary) `<ViewTransition>` 업데이트 시 이미지 로딩 대기
- (실험적) `<Suspense defer>` 내부의 CPU 바운드 렌더링 작업

## 주의사항

- Effect나 이벤트 핸들러 내부에서 가져온 데이터는 감지하지 않음
- 첫 마운트 전에 일시 중단된 렌더링의 상태는 보존하지 않음 -- 처음부터 다시 시도함
- 이미 콘텐츠를 표시한 후 다시 일시 중단되면 폴백이 다시 표시됨. 단 `startTransition`이나 `useDeferredValue`로 트리거된 업데이트는 예외임
- React는 일시 중단된 콘텐츠를 최대 300ms마다 한 번씩 표시함 -- 이 기간 내에 준비된 경계는 함께 표시됨
- 이미 표시된 콘텐츠가 다시 일시 중단되면 레이아웃 Effect가 정리됨. 콘텐츠가 준비되면 다시 실행됨
- 스트리밍 서버 렌더링(Streaming Server Rendering)과 선택적 하이드레이션(Selective Hydration) 최적화를 내장하고 있음

## 사용 예시

### 기본 폴백

```js
import { Suspense } from 'react';
import Albums from './Albums.js';

export default function ArtistPage({ artist }) {
  return (
    <>
      <h1>{artist.name}</h1>
      <Suspense fallback={<Loading />}>
        <Albums artistId={artist.id} />
      </Suspense>
    </>
  );
}

function Loading() {
  return <h2>Loading...</h2>;
}
```

```js
import { use } from 'react';
import { fetchData } from './data.js';

export default function Albums({ artistId }) {
  const albums = use(fetchData(`/${artistId}/albums`));
  return (
    <ul>
      {albums.map(album => (
        <li key={album.id}>
          {album.title} ({album.year})
        </li>
      ))}
    </ul>
  );
}
```

### 콘텐츠를 함께 표시

```js
<Suspense fallback={<Loading />}>
  <Biography />
  <Panel>
    <Albums />
  </Panel>
</Suspense>
```

### 중첩된 로딩 시퀀스

```js
<Suspense fallback={<BigSpinner />}>
  <Biography />
  <Suspense fallback={<AlbumsGlimmer />}>
    <Panel>
      <Albums />
    </Panel>
  </Suspense>
</Suspense>
```

### useDeferredValue로 이전 콘텐츠 유지

```js
import { Suspense, useState, useDeferredValue } from 'react';
import SearchResults from './SearchResults.js';

export default function App() {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  const isStale = query !== deferredQuery;
  return (
    <>
      <label>
        Search albums:
        <input value={query} onChange={e => setQuery(e.target.value)} />
      </label>
      <Suspense fallback={<h2>Loading...</h2>}>
        <div style={{ opacity: isStale ? 0.5 : 1 }}>
          <SearchResults query={deferredQuery} />
        </div>
      </Suspense>
    </>
  );
}
```

### startTransition으로 폴백 방지

```js
function Router() {
  const [page, setPage] = useState('/');

  function navigate(url) {
    startTransition(() => {
      setPage(url);
    });
  }
}
```

### useTransition으로 전환 표시

```js
function Router() {
  const [page, setPage] = useState('/');
  const [isPending, startTransition] = useTransition();

  function navigate(url) {
    startTransition(() => {
      setPage(url);
    });
  }

  return (
    <Layout isPending={isPending}>
      {content}
    </Layout>
  );
}
```

### key로 경계 초기화

```js
<Suspense key={user.id} fallback={<p>Loading profile...</p>}>
  <Bio bioPromise={user.bioPromise} />
</Suspense>
```

---

# Activity

> 원문: https://react.dev/reference/react/Activity

## 개요

- `<Activity>`는 UI와 내부 상태를 숨기고 복원할 수 있게 해주는 컴포넌트임 (Canary/실험적)
- 숨겨진 상태에서는 `display: "none"` CSS를 적용하고, Effect를 정리하지만, DOM과 컴포넌트 상태는 보존함

```jsx
<Activity mode={visibility}>
  {children}
</Activity>
```

## Props

- `children` (ReactNode): 표시하거나 숨길 UI
- `mode` (`'visible' | 'hidden'`): 가시성을 제어함

## 숨김 동작

- `display: "none"` CSS가 적용됨
- Effect가 파괴되고 구독이 정리됨
- 자식은 새로운 props에 대해 여전히 리렌더링됨 (더 낮은 우선순위로)
- DOM은 보존되지만 숨겨짐
- 컴포넌트 상태가 보존됨

## 다시 표시 동작

- 이전 상태가 복원됨
- Effect가 다시 생성됨
- DOM 상태가 유지됨 (textarea 입력, 비디오 재생 위치 등)

## 주의사항

- ViewTransition 연동: `ViewTransition` 내부에서 표시되면 `enter` 애니메이션, 숨겨지면 `exit` 애니메이션이 트리거됨
- 텍스트만 있는 컴포넌트: 텍스트만 렌더링하는 숨겨진 Activity는 아무것도 렌더링하지 않음 (숨길 DOM 요소가 없음)
- 사전 렌더링 중 Effect: 초기 렌더링 시 숨겨진 상태이면 Effect가 마운트되지 않음. `use()` 같은 Suspense 활성화 데이터만 가져옴
- DOM 부작용: `<video>`, `<audio>`, `<iframe>`은 숨겨진 상태에서도 부작용이 계속됨. `useLayoutEffect` 클린업으로 중지해야 함

## 사용 예시

### 상태 보존

```jsx
import { Activity, useState } from 'react';
import Sidebar from './Sidebar.js';

export default function App() {
  const [isShowingSidebar, setIsShowingSidebar] = useState(true);

  return (
    <>
      <Activity mode={isShowingSidebar ? 'visible' : 'hidden'}>
        <Sidebar />
      </Activity>

      <main>
        <button onClick={() => setIsShowingSidebar(!isShowingSidebar)}>
          Toggle sidebar
        </button>
        <h1>Main content</h1>
      </main>
    </>
  );
}
```

### 탭에서 DOM 상태 보존

```jsx
import { Activity, useState, Suspense } from 'react';
import TabButton from './TabButton.js';
import Home from './Home.js';
import Contact from './Contact.js';

export default function App() {
  const [activeTab, setActiveTab] = useState('contact');

  return (
    <>
      <TabButton
        isActive={activeTab === 'home'}
        onClick={() => setActiveTab('home')}
      >
        Home
      </TabButton>
      <TabButton
        isActive={activeTab === 'contact'}
        onClick={() => setActiveTab('contact')}
      >
        Contact
      </TabButton>

      <hr />

      <Activity mode={activeTab === 'home' ? 'visible' : 'hidden'}>
        <Home />
      </Activity>
      <Activity mode={activeTab === 'contact' ? 'visible' : 'hidden'}>
        <Contact />
      </Activity>
    </>
  );
}
```

### 사전 렌더링

```jsx
import { Activity, useState, Suspense } from 'react';

export default function App() {
  const [activeTab, setActiveTab] = useState('home');

  return (
    <>
      <TabButton isActive={activeTab === 'home'} onClick={() => setActiveTab('home')}>
        Home
      </TabButton>
      <TabButton isActive={activeTab === 'posts'} onClick={() => setActiveTab('posts')}>
        Posts
      </TabButton>

      <hr />

      <Suspense fallback={<h1>Loading...</h1>}>
        <Activity mode={activeTab === 'home' ? 'visible' : 'hidden'}>
          <Home />
        </Activity>
        <Activity mode={activeTab === 'posts' ? 'visible' : 'hidden'}>
          <Posts />
        </Activity>
      </Suspense>
    </>
  );
}
```

### 비디오/오디오 클린업

```jsx
import { useRef, useLayoutEffect } from 'react';

export default function Video() {
  const ref = useRef();

  useLayoutEffect(() => {
    const videoRef = ref.current;

    return () => {
      videoRef.pause();
    };
  }, []);

  return (
    <video
      ref={ref}
      controls
      playsInline
      src="https://example.com/video.mp4"
    />
  );
}
```

---

# ViewTransition

> 원문: https://react.dev/reference/react/ViewTransition

## 개요

- `<ViewTransition>`은 Transition과 Suspense를 사용하여 컴포넌트 트리에 애니메이션을 적용하는 컴포넌트임 (Canary/실험적)
- View Transition API를 활용함

```js
import {ViewTransition} from 'react';

<ViewTransition>
  <div>...</div>
</ViewTransition>
```

## 동작 원리

- 가장 가까운 DOM 노드의 인라인 스타일에 `view-transition-name`을 적용함
- `startViewTransition`을 자동으로 호출함 (수동 호출 불필요)
- 다른 ViewTransition이 끝날 때까지 대기 후 새 것을 시작함
- 애니메이션 중 발생하는 여러 업데이트를 하나로 배치함
- `flushSync`가 중간에 발생하면 React는 Transition을 건너뜀

## Props

### 코어 Props

- `name` (string 또는 object, 선택): 공유 요소 전환을 위한 view-transition-name. 지정하지 않으면 React가 고유한 이름을 생성함. 공유 요소 전환에만 사용해야 함

### View Transition 클래스 Props: `enter`, `exit`, `update`, `share`, `default`

```js
<ViewTransition
  default="none"
  enter="slide-up"
  exit="slide-down"
  update="cross-fade"
  share="auto"
/>
```

- 각각 다음 값을 받을 수 있음
  - `"auto"`: 브라우저 기본 애니메이션 (부드러운 크로스페이드)
  - `"none"`: 해당 타입의 애니메이션을 비활성화함
  - `<classname>`: View Transition 스타일링용 커스텀 CSS 클래스
  - Transition Type 키를 가진 객체: `{[type]: value, default: value}`
- `default`가 `"none"`이면 명시적으로 나열하지 않는 한 모든 다른 트리거도 비활성화됨

### 객체 예시

```js
<ViewTransition
  default="none"
  enter={{
    "forward": 'slide-in',
    "default": 'auto'
  }}
  exit="auto"
  update="cross-fade"
/>
```

### View Transition 이벤트 Props: `onEnter`, `onExit`, `onShare`, `onUpdate`

```js
<ViewTransition
  onEnter={(instance) => {/* ... */}}
  onExit={(instance) => {/* ... */}}
  onShare={(instance) => {/* ... */}}
  onUpdate={(instance) => {/* ... */}}
/>
```

- 각 콜백이 받는 인수
  - `instance`: View Transition 인스턴스. 의사 요소를 포함함
    - `old`: `::view-transition-old` 의사 요소
    - `new`: `::view-transition-new` 의사 요소
    - `name`: view-transition-name 문자열
    - `group`: `::view-transition-group` 의사 요소
    - `imagePair`: `::view-transition-image-pair` 의사 요소
  - `types`: `Array<string>` -- Transition Type 배열
- 하나의 `<ViewTransition>` 당 Transition마다 하나의 이벤트만 발생함
- `onShare`는 `onEnter`와 `onExit`보다 우선함
- 각 이벤트는 클린업 함수를 반환해야 함

## 애니메이션 트리거

- `enter`: Transition에서 처음으로 삽입되는 컴포넌트에 ViewTransition이 있을 때
- `exit`: Transition에서 처음으로 삭제되는 컴포넌트에 ViewTransition이 있을 때
- `update`: ViewTransition의 DOM 변경, 크기 변화, 또는 직접 형제 변경으로 인한 위치 변경이 있을 때
- `share`: 같은 이름의 ViewTransition이 삭제되는 서브트리와 삽입되는 서브트리 양쪽에 존재할 때

## 주의사항

- 최상위 ViewTransition만 exit/enter에서 애니메이션됨 -- DOM 노드 앞에 배치해야 함

```js
// 애니메이션 안 됨
function Item() {
  return (
    <div>
      <ViewTransition enter="auto" exit="auto">
        <Video />
      </ViewTransition>
    </div>
  );
}

// 애니메이션 됨
function Item() {
  return (
    <ViewTransition enter="auto" exit="auto">
      <div>
        <Video />
      </div>
    </ViewTransition>
  );
}
```

- `setState`는 ViewTransition을 활성화하지 않음 -- `startTransition`, `<Suspense>`, `useDeferredValue`만 활성화함
- ViewTransition은 이미지 스냅샷을 생성함 -- 독립적으로 이동해야 하는 요소의 연속성이 끊길 수 있음
- DOM 전용 -- React Native에서는 사용 불가함
- 공유 요소 전환에는 고유한 이름이 필요함
- 항상 `prefers-reduced-motion`을 확인해야 함
- 리스트에서 래퍼 요소가 있으면 개별 항목 애니메이션이 깨짐

## CSS 스타일링

```css
::view-transition-group(.slide-in) { }
::view-transition-old(.slide-in) { }
::view-transition-new(.slide-in) { }
```

## 사용 예시

### Enter/Exit 애니메이션

```js
import {ViewTransition, useState, startTransition} from 'react';

function Child() {
  return (
    <ViewTransition enter="auto" exit="auto" default="none">
      <div>Hi</div>
    </ViewTransition>
  );
}

function Parent() {
  const [show, setShow] = useState(false);
  return (
    <>
      <button onClick={() => {
        startTransition(() => { setShow((prev) => !prev); });
      }}>
        {show ? 'Hide' : 'Show'}
      </button>
      {show ? <Child /> : null}
    </>
  );
}
```

### Activity와 함께 상태 보존

```js
import { Activity, ViewTransition, useState, startTransition } from 'react';

export default function App() {
  const [show, setShow] = useState(true);
  return (
    <div className="layout">
      <button onClick={() => {
        startTransition(() => { setShow(s => !s); });
      }}>
        {show ? 'Hide' : 'Show'}
      </button>
      <Activity mode={show ? 'visible' : 'hidden'}>
        <ViewTransition enter="auto" exit="auto" default="none">
          <Counter />
        </ViewTransition>
      </Activity>
    </div>
  );
}

function Counter() {
  const [count, setCount] = useState(0);
  return (
    <div className="counter">
      <h2>Counter</h2>
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>Increment</button>
    </div>
  );
}
```

### 공유 요소 전환

```js
import {ViewTransition, useState, startTransition} from 'react';

const THUMBNAIL_NAME = 'video-thumbnail';

function Thumbnail({video}) {
  return (
    <ViewTransition name={THUMBNAIL_NAME}>
      <div className={`thumbnail ${video.image}`} />
    </ViewTransition>
  );
}

function Video({video, onClick}) {
  return (
    <div className="video">
      <div className="link" onClick={onClick}>
        <Thumbnail video={video} />
        <div className="info">
          <div className="video-title">{video.title}</div>
        </div>
      </div>
    </div>
  );
}

function FullscreenVideo({video, onExit}) {
  return (
    <div className="fullscreenLayout">
      <ViewTransition name={THUMBNAIL_NAME}>
        <div className={`thumbnail ${video.image} fullscreen`} />
        <button className="close-button" onClick={onExit}>X</button>
      </ViewTransition>
    </div>
  );
}

export default function Component() {
  const [fullscreen, setFullscreen] = useState(false);
  if (fullscreen) {
    return (
      <FullscreenVideo
        video={videos[0]}
        onExit={() => startTransition(() => setFullscreen(false))}
      />
    );
  }
  return (
    <Video
      video={videos[0]}
      onClick={() => startTransition(() => setFullscreen(true))}
    />
  );
}
```

### 리스트 재정렬

```js
import {ViewTransition, useState, startTransition} from 'react';

export default function Component() {
  const [orderedVideos, setOrderedVideos] = useState(videos);
  const reorder = () => {
    startTransition(() => {
      setOrderedVideos((prev) => [...prev.sort(() => Math.random() - 0.5)]);
    });
  };
  return (
    <>
      <button onClick={reorder}>Shuffle</button>
      <div className="listContainer">
        {orderedVideos.map((video) => (
          <ViewTransition key={video.title}>
            <Video video={video} />
          </ViewTransition>
        ))}
      </div>
    </>
  );
}
```

### Suspense 애니메이션

```js
import {ViewTransition, useState, startTransition, Suspense} from 'react';

function LazyVideo() {
  const video = useLazyVideoData();
  return <Video video={video} />;
}

export default function Component() {
  const [showItem, setShowItem] = useState(false);
  return (
    <>
      <button onClick={() => {
        startTransition(() => { setShowItem((prev) => !prev); });
      }}>
        {showItem ? 'Hide' : 'Show'}
      </button>
      {showItem ? (
        <ViewTransition>
          <Suspense fallback={<VideoPlaceholder />}>
            <LazyVideo />
          </Suspense>
        </ViewTransition>
      ) : null}
    </>
  );
}
```

### 커스텀 CSS 애니메이션

```css
::view-transition-old(.slide-in) {
  animation-name: slideOutRight;
  animation-duration: 500ms;
  animation-timing-function: ease-in-out;
}
::view-transition-new(.slide-in) {
  animation-name: slideInRight;
  animation-duration: 500ms;
  animation-timing-function: ease-in-out;
}

@keyframes slideOutLeft {
  from { transform: translateX(0); opacity: 1; }
  to { transform: translateX(-100%); opacity: 0; }
}
@keyframes slideInLeft {
  from { transform: translateX(-100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
@keyframes slideOutRight {
  from { transform: translateX(0); opacity: 1; }
  to { transform: translateX(100%); opacity: 0; }
}
@keyframes slideInRight {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
```

### JavaScript 기반 애니메이션

```js
import {ViewTransition, useState, startTransition} from 'react';

const SLIDE_IN = [{transform: 'translateY(20px)'}, {transform: 'translateY(0)'}];
const SLIDE_OUT = [{transform: 'translateY(0)'}, {transform: 'translateY(-20px)'}];

function Item() {
  return (
    <ViewTransition
      default="none"
      enter="auto"
      exit="auto"
      onEnter={(instance) => {
        const anim = instance.new.animate(SLIDE_IN, {duration: 500, easing: 'ease-out'});
        return () => anim.cancel();
      }}
      onExit={(instance) => {
        const anim = instance.old.animate(SLIDE_OUT, {duration: 300, easing: 'ease-in'});
        return () => anim.cancel();
      }}>
      <Video video={videos[0]} />
    </ViewTransition>
  );
}
```

### 애니메이션 비활성화

```js
<ViewTransition>
  <div className={theme}>
    <ViewTransition update="none">{children}</ViewTransition>
  </div>
</ViewTransition>
```

## 참고사항

- 라우터 연동 시 뒤로 가기 버튼의 popstate 이벤트로는 애니메이션이 실행되지 않음. Navigation API를 대신 사용해야 함
- ViewTransition은 폰트 로드를 최대 500ms 대기함
- ViewTransition 내부의 이미지는 로드될 때까지 애니메이션 시작을 대기함
- `<Suspense>` 내부에서는 "update" 애니메이션, 외부에서는 "enter"/"exit" 애니메이션이 적용됨

---

# React API

---

# createContext

> 원문: https://react.dev/reference/react/createContext

## 개요

- `createContext`는 컴포넌트가 제공하거나 읽을 수 있는 컨텍스트를 생성하는 함수임

```js
const SomeContext = createContext(defaultValue)
```

## API: createContext(defaultValue)

### 매개변수

- `defaultValue`: 트리 상위에 일치하는 컨텍스트 프로바이더가 없을 때 사용되는 값. 의미 있는 기본값이 없으면 `null`을 지정함. 기본값은 "최후의 수단" 대비책으로 정적이며 시간이 지나도 변하지 않음

### 반환값

- 컨텍스트 객체
  - `SomeContext`: React 19에서는 `<SomeContext>`를 프로바이더로 렌더링할 수 있음
  - `SomeContext.Consumer`: 컨텍스트 값을 읽는 레거시 방법 (거의 사용하지 않음)
  - `SomeContext.Provider`: React 19 이전 버전에서 컨텍스트 값을 제공하는 레거시 방법

## SomeContext 프로바이더

```js
function App() {
  const [theme, setTheme] = useState('light');
  return (
    <ThemeContext value={theme}>
      <Page />
    </ThemeContext>
  );
}
```

- React 19: `<SomeContext>` 형태로 프로바이더 사용
- 이전 버전: `<SomeContext.Provider>` 형태 사용

### Props

- `value`: 이 프로바이더 내부에서 컨텍스트를 읽는 모든 컴포넌트에 전달할 값. 깊이에 상관없이 전달됨. 모든 타입 가능

## SomeContext.Consumer (레거시)

```js
// 레거시 (권장하지 않음)
function Button() {
  return (
    <ThemeContext.Consumer>
      {theme => (<button className={theme} />)}
    </ThemeContext.Consumer>
  );
}

// 권장
function Button() {
  const theme = useContext(ThemeContext);
  return <button className={theme} />;
}
```

## 사용 예시

### 컨텍스트 생성 및 읽기

```js
import { createContext } from 'react';

const ThemeContext = createContext('light');
const AuthContext = createContext(null);
```

```js
function Button() {
  const theme = useContext(ThemeContext);
}

function Profile() {
  const currentUser = useContext(AuthContext);
}
```

### 동적 값 제공

```js
function App() {
  const [theme, setTheme] = useState('dark');
  const [currentUser, setCurrentUser] = useState({ name: 'Taylor' });
  return (
    <ThemeContext value={theme}>
      <AuthContext value={currentUser}>
        <Page />
      </AuthContext>
    </ThemeContext>
  );
}
```

### 파일에서 가져오기/내보내기

```js
// Contexts.js
import { createContext } from 'react';
export const ThemeContext = createContext('light');
export const AuthContext = createContext(null);
```

```js
// Button.js
import { ThemeContext } from './Contexts.js';
function Button() {
  const theme = useContext(ThemeContext);
}
```

## 주의사항

- 기본값은 정적임 -- 프로바이더가 없을 때만 대비책으로 사용됨
- 컨텍스트 객체는 정보를 담지 않음 -- 식별자 역할만 함
- React 19에서 `<SomeContext>`를 프로바이더로 사용함. 이전 버전에서는 `<SomeContext.Provider>` 사용
- 새 코드에서 `SomeContext.Consumer` 대신 `useContext()` 사용이 권장됨
- 프로바이더 아래 어떤 깊이에서든 컨텍스트 값을 읽을 수 있음

## 문제 해결

- "컨텍스트 값을 변경할 방법을 찾을 수 없음": `createContext()`의 기본값은 절대 변경되지 않음. 상태를 추가하고 컴포넌트를 컨텍스트 프로바이더로 감싸야 함

---

# lazy

> 원문: https://react.dev/reference/react/lazy

## 개요

- `lazy`는 컴포넌트의 코드를 처음 렌더링될 때까지 지연 로딩할 수 있게 해주는 함수임

```js
const SomeComponent = lazy(load)
```

## API: lazy(load)

### 매개변수

- `load`: Promise 또는 thenable을 반환하는 함수. React는 첫 렌더링 시도까지 `load`를 호출하지 않음. 첫 호출 후 해결을 대기하고 `.default` 값을 React 컴포넌트로 렌더링함. Promise와 해결된 값 모두 캐시됨 -- `load`는 최대 한 번만 호출됨. Promise가 거부되면 가장 가까운 Error Boundary에 거부 사유를 던짐

### 반환값

- 렌더링 가능한 React 컴포넌트. 로딩 중에 렌더링하면 일시 중단(suspend)됨. `<Suspense>`를 사용하여 로딩 표시기를 보여줘야 함

### load 함수

- 매개변수: 없음
- 반환값: `.default`가 유효한 React 컴포넌트 타입(함수, `memo`, `forwardRef` 컴포넌트)인 객체로 해결되는 Promise/thenable

## 사용 예시

### Suspense와 함께 지연 로딩

```js
// 정적 임포트
import MarkdownPreview from './MarkdownPreview.js';

// 동적 임포트 (lazy 사용)
import { lazy } from 'react';
const MarkdownPreview = lazy(() => import('./MarkdownPreview.js'));
```

- 동적 `import()`에 의존함 -- 번들러/프레임워크 지원이 필요함
- `default` 내보내기로 내보내야 함

```js
import { useState, Suspense, lazy } from 'react';
import Loading from './Loading.js';

const MarkdownPreview = lazy(() => delayForDemo(import('./MarkdownPreview.js')));

export default function MarkdownEditor() {
  const [showPreview, setShowPreview] = useState(false);
  const [markdown, setMarkdown] = useState('Hello, **world**!');
  return (
    <>
      <textarea value={markdown} onChange={e => setMarkdown(e.target.value)} />
      <label>
        <input type="checkbox" checked={showPreview} onChange={e => setShowPreview(e.target.checked)} />
        Show preview
      </label>
      <hr />
      {showPreview && (
        <Suspense fallback={<Loading />}>
          <h2>Preview</h2>
          <MarkdownPreview markdown={markdown} />
        </Suspense>
      )}
    </>
  );
}

function delayForDemo(promise) {
  return new Promise(resolve => {
    setTimeout(resolve, 2000);
  }).then(() => promise);
}
```

## 문제 해결: 상태가 예기치 않게 초기화됨

```js
// 잘못된 코드 -- 컴포넌트 내부에서 lazy 선언하면 리렌더링 시마다 상태가 초기화됨
function Editor() {
  const MarkdownPreview = lazy(() => import('./MarkdownPreview.js'));
}

// 올바른 코드 -- 모듈 최상위에서 lazy 컴포넌트를 선언해야 함
const MarkdownPreview = lazy(() => import('./MarkdownPreview.js'));
function Editor() {
  // ...
}
```

---

# memo

> 원문: https://react.dev/reference/react/memo

## 개요

- `memo`는 props가 변경되지 않았을 때 컴포넌트의 리렌더링을 건너뛸 수 있게 해주는 함수임
- React Compiler가 활성화되면 자동으로 동등한 최적화를 적용하므로 수동 `memo`가 불필요해짐

```js
const MemoizedComponent = memo(SomeComponent, arePropsEqual?)
```

## API: memo(Component, arePropsEqual?)

### 매개변수

- `Component` (필수): 메모이제이션할 컴포넌트. 컴포넌트를 수정하지 않고 새로운 메모이제이션된 컴포넌트를 반환함. 함수, `forwardRef` 컴포넌트 등 모든 유효한 React 컴포넌트를 받을 수 있음
- `arePropsEqual` (선택): `(oldProps, newProps)` 함수. props가 동일하면(같은 출력) `true`, 다르면 `false`를 반환함. 기본값: React가 각 prop을 `Object.is`로 비교함

### 반환값

- 동일하게 동작하되 props가 변경되지 않으면 부모 리렌더링 시 항상 리렌더링하지 않는 새 React 컴포넌트

## props 변경이 없을 때 리렌더링 건너뛰기

```js
import { memo, useState } from 'react';

export default function MyApp() {
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  return (
    <>
      <label>Name{': '}<input value={name} onChange={e => setName(e.target.value)} /></label>
      <label>Address{': '}<input value={address} onChange={e => setAddress(e.target.value)} /></label>
      <Greeting name={name} />
    </>
  );
}

const Greeting = memo(function Greeting({ name }) {
  console.log("Greeting was rendered at", new Date().toLocaleTimeString());
  return <h3>Hello{name && ', '}{name}!</h3>;
});
```

- `Greeting`은 `name`이 변경될 때만 리렌더링되고 `address`가 변경될 때는 리렌더링되지 않음

## 메모이제이션된 컴포넌트가 리렌더링되는 경우

- 자체 상태가 변경될 때
- 사용 중인 컨텍스트가 변경될 때

## props 변경 최소화

- React는 각 prop을 `Object.is`로 비교함. `Object.is(3, 3)`은 `true`이지만 `Object.is({}, {})`는 `false`임

### 객체 props에 useMemo 사용

```js
function Page() {
  const [name, setName] = useState('Taylor');
  const [age, setAge] = useState(42);
  const person = useMemo(() => ({ name, age }), [name, age]);
  return <Profile person={person} />;
}
const Profile = memo(function Profile({ person }) { /* ... */ });
```

### 개별 값으로 전달 (더 나은 방법)

```js
function Page() {
  const [name, setName] = useState('Taylor');
  const [age, setAge] = useState(42);
  return <Profile name={name} age={age} />;
}
const Profile = memo(function Profile({ name, age }) { /* ... */ });
```

### 덜 자주 변경되는 값으로 변환

```js
function GroupsLanding({ person }) {
  const hasGroups = person.groups !== null;
  return <CallToAction hasGroups={hasGroups} />;
}
const CallToAction = memo(function CallToAction({ hasGroups }) { /* ... */ });
```

- 함수 props: 컴포넌트 외부에서 선언하거나 `useCallback`을 사용함

## 커스텀 비교 함수

```js
const Chart = memo(function Chart({ dataPoints }) {
  // ...
}, arePropsEqual);

function arePropsEqual(oldProps, newProps) {
  return (
    oldProps.dataPoints.length === newProps.dataPoints.length &&
    oldProps.dataPoints.every((oldPoint, index) => {
      const newPoint = newProps.dataPoints[index];
      return oldPoint.x === newPoint.x && oldPoint.y === newPoint.y;
    })
  );
}
```

### 커스텀 비교 함수 주의사항

- 함수를 포함한 모든 prop을 비교해야 함. 함수는 부모의 props와 상태를 클로저로 캡처함
- `oldProps.onClick !== newProps.onClick`일 때 `true`를 반환하면 오래된 핸들러 버그가 발생함
- 깊은 동등성 검사를 하면 앱이 멈출 수 있으므로 피해야 함

## memo가 유용한 경우

- 세밀한 상호작용이 있는 편집기 (드로잉 에디터 등)
- 같은 props로 자주 리렌더링되는 컴포넌트
- 비용이 큰 렌더링 로직
- 체감할 수 있는 지연이 있을 때

## memo가 불필요한 경우

- 거친 상호작용 (페이지/섹션 교체)
- 체감할 수 있는 지연이 없을 때
- props가 항상 다를 때 (렌더링 중 정의되는 객체/함수)

## memo 필요성을 줄이는 모범 사례

- JSX를 children으로 받기
- 로컬 상태 선호
- 렌더링 로직을 순수하게 유지
- 상태를 업데이트하는 불필요한 Effect 제거
- Effect에서 불필요한 의존성 제거

---

# startTransition

> 원문: https://react.dev/reference/react/startTransition

## 개요

- `startTransition`은 상태 업데이트를 Transition으로 표시하여 UI의 일부를 백그라운드에서 렌더링할 수 있게 해주는 함수임

```js
startTransition(action)
```

## API: startTransition(action)

### 매개변수

- `action`: `set` 함수를 호출하여 상태를 업데이트하는 함수. 매개변수 없이 즉시 호출됨. `action` 내에서 동기적으로 예약된 모든 상태 업데이트가 Transition으로 표시됨. `action`에서 await한 비동기 호출도 포함되지만 현재는 `await` 이후의 `set` 함수를 추가 `startTransition`으로 감싸야 함. Transition으로 표시된 상태 업데이트는 비차단(non-blocking)이며 불필요한 로딩 표시기를 표시하지 않음

### 반환값

- `undefined` (없음)

## 주의사항

- 대기 상태를 추적할 방법을 제공하지 않음 -- 대기 표시가 필요하면 `useTransition` 사용
- `set` 함수에 접근할 수 있을 때만 업데이트를 감쌀 수 있음 -- prop이나 훅 반환값의 전환에는 `useDeferredValue` 사용
- 전달된 함수는 즉시 호출됨. `setTimeout` 내의 상태 업데이트는 Transition으로 표시되지 않음
- 비동기 요청 후 상태 업데이트를 또 다른 `startTransition`으로 감싸야 함 (알려진 제한)
- Transition 업데이트는 다른 상태 업데이트에 의해 중단됨 (예: 입력 타이핑이 차트 Transition 리렌더링을 중단함)
- Transition 업데이트로 텍스트 입력을 제어할 수 없음
- 여러 진행 중인 Transition이 함께 배치됨 (현재 제한)

## 사용 예시

### 비차단 Transition으로 상태 업데이트 표시

```js
import { startTransition } from 'react';

function TabContainer() {
  const [tab, setTab] = useState('about');

  function selectTab(nextTab) {
    startTransition(() => {
      setTab(nextTab);
    });
  }
}
```

- Transition은 리렌더링 중에도 UI를 반응성 있게 유지함 -- 사용자가 첫 리렌더링을 기다리지 않고 다른 탭을 클릭할 수 있음

## startTransition vs useTransition

- `startTransition`은 `useTransition`과 유사하지만 `isPending` 플래그가 없음
- `startTransition`은 컴포넌트 외부(예: 데이터 라이브러리)에서도 동작하지만 `useTransition`은 컴포넌트 내부에서만 사용 가능함

---

# use

> 원문: https://react.dev/reference/react/use

## 개요

- `use`는 Promise 또는 컨텍스트의 값을 읽을 수 있게 해주는 React API임

```js
const value = use(resource);
```

## use(context)

### 매개변수

- `context`: `createContext`로 생성된 컨텍스트

### 반환값

- 전달된 컨텍스트의 컨텍스트 값. 호출하는 컴포넌트 위의 가장 가까운 컨텍스트 프로바이더가 결정함. 프로바이더가 없으면 `createContext`에 전달된 `defaultValue`가 반환됨

### 주의사항

- 컴포넌트 또는 훅 내부에서 호출해야 함
- 서버 컴포넌트에서는 `use`로 컨텍스트를 읽을 수 없음
- `useContext`와 달리 조건문과 반복문 내에서 호출할 수 있음

## use(promise)

### 매개변수

- `promise`: 해결된 값을 읽으려는 Promise. 리렌더링 간에 동일한 인스턴스가 재사용되도록 캐시해야 함

### 반환값

- Promise의 해결된 값

### 주의사항

- 컴포넌트 또는 훅 내부에서 호출해야 함
- try-catch 블록 내에서 호출할 수 없음 -- 대신 Error Boundary를 사용해야 함
- `use`에 전달되는 Promise는 리렌더링 간에 동일한 인스턴스가 재사용되도록 캐시해야 함
- 서버 컴포넌트에서 클라이언트 컴포넌트로 Promise를 전달할 때 해결된 값은 직렬화 가능해야 함

## 사용 패턴

- 조건부 컨텍스트 읽기 (`useContext`와 달리 조건부 가능)
- 컨텍스트에서 Promise 읽기 (두 번의 `use` 호출 필요)
- Suspense 폴백으로 Promise 읽기
- 클라이언트 컴포넌트용 Promise 캐싱 (모듈 수준 Map 캐시)
- `startTransition` + 캐시 무효화로 데이터 다시 가져오기
- `onMouseEnter`로 호버 시 데이터 사전 로딩
- 서버에서 클라이언트로 데이터 스트리밍 (서버 컴포넌트에서 클라이언트 컴포넌트로 Promise를 prop으로 전달)
- Error Boundary를 통한 에러 처리 (try-catch가 아님)

## 주의할 점

- 렌더링 중 생성된 Promise는 매 렌더링마다 다시 생성됨 -- 반드시 캐시해야 함
- `fetch()`를 렌더링에서 직접 호출하면 매번 새 Promise가 생성됨
- 캐시된 Promise에 `.then()`을 추가해도 새 Promise가 생성됨
- `promise.status`/`promise.value`를 직접 읽어 `use`를 우회하면 안 됨

## 문제 해결

- "Suspense Exception: This is not a real error!": `use`가 try-catch 블록 내에 있음
- "A component was suspended by an uncached promise": Promise가 캐시되지 않았음

---

# cache

> 원문: https://react.dev/reference/react/cache

## 개요

- `cache`는 React 서버 컴포넌트에서 데이터 가져오기나 계산의 결과를 캐시할 수 있게 해주는 함수임

```js
const cachedFn = cache(fn);
```

## API: cache(fn)

### 매개변수

- `fn`: 결과를 캐시할 함수. 어떤 인수든 받을 수 있고 어떤 값이든 반환할 수 있음

### 반환값

- 동일한 타입 시그니처를 가진 `fn`의 캐시된 버전. 호출 시 캐시를 먼저 확인하고, 캐시된 결과가 있으면 반환하며, 없으면 `fn`을 호출하여 결과를 저장하고 반환함

## 주의사항

- React는 각 서버 요청마다 모든 메모이제이션된 함수의 캐시를 무효화함
- `cache`를 같은 함수에 여러 번 호출하면 서로 다른 메모이제이션된 함수가 반환되며 캐시를 공유하지 않음
- `cachedFn`은 에러도 캐시함 -- `fn`이 특정 인수에 대해 에러를 던지면 에러가 캐시되어 재발생함
- 서버 컴포넌트 전용임
- 인수의 얕은 동등성 검사에 `Object.is`를 사용함

## 사용 패턴

- 비용이 큰 계산 캐싱 (컴포넌트 간 공유)
- 데이터 스냅샷 공유 (비동기 함수)
- 데이터 사전 로딩 (await 없이 `getUser(id)`를 미리 호출)

## 주의할 점

- 컴포넌트 내부에서 `cache`를 호출하면 렌더링마다 새 메모이제이션된 함수가 생성됨 -- 모듈 수준에서 정의해야 함
- 서로 다른 모듈에서 같은 함수에 대해 별도로 `cache`를 호출하면 별도의 캐시가 생성됨 -- 단일 캐시된 버전을 내보내야 함
- 컴포넌트 컨텍스트 밖에서 메모이제이션된 함수를 호출하면 캐시를 사용하지 않음
- 비원시 인수(객체)는 캐시 적중을 위해 동일한 참조여야 함

## cache vs memo vs useMemo

- `useMemo`: 클라이언트 컴포넌트용. 렌더링 간 계산을 캐시함. 컴포넌트 로컬
- `cache`: 서버 컴포넌트용. 컴포넌트 간 공유되는 작업을 메모이제이션함. 서버 요청마다 무효화됨
- `memo`: props가 변경되지 않으면 리렌더링을 방지함. 마지막 prop 값으로 마지막 렌더링을 캐시함

---

# act

> 원문: https://react.dev/reference/react/act

## 개요

- `act`는 대기 중인 React 업데이트를 적용한 후 단언(assertion)을 수행할 수 있게 해주는 테스트 헬퍼임

```js
await act(async actFn)
```

## API: await act(async actFn)

### 매개변수

- `async actFn`: 테스트 대상 컴포넌트의 렌더링이나 상호작용을 감싸는 비동기 함수. 내부에서 트리거된 업데이트가 내부 act 큐에 추가되어 함께 플러시됨

### 반환값

- 없음

## 사용법

### 테스트에서 컴포넌트 렌더링

```js
await act(async () => {
  ReactDOMClient.createRoot(container).render(<MyComponent />);
});
```

### 테스트에서 이벤트 디스패치

```js
await act(async () => {
  button.dispatchEvent(new MouseEvent('click', { bubbles: true }));
});
```

## 주의사항

- `async` 함수와 함께 `await`를 사용하는 것이 권장됨 -- 동기 버전은 지원 중단 예정
- DOM 이벤트를 사용하려면 컨테이너가 `document`에 추가되어야 함
- React Testing Library 헬퍼는 이미 `act()`로 감싸져 있음

## 문제 해결

- "The current testing environment is not configured to support act(...)": 테스트 설정에서 `global.IS_REACT_ACT_ENVIRONMENT=true`를 설정해야 함

---

# captureOwnerStack

> 원문: https://react.dev/reference/react/captureOwnerStack

## 개요

- `captureOwnerStack`은 개발 환경에서 현재 Owner Stack을 읽어 문자열로 반환하는 함수임
- Owner Stack은 컴포넌트를 렌더링한 것이 아니라 생성한 컴포넌트를 추적함

```js
const stack = captureOwnerStack();
```

## API

### 매개변수

- 없음

### 반환값

- `string | null`: 컴포넌트 렌더링, Effect, React 이벤트 핸들러, React 에러 핸들러에서 사용 가능함. Owner Stack이 없으면 `null`을 반환함

## 주의사항

- 개발 환경에서만 사용 가능함 -- 개발 환경 밖에서는 항상 `null`을 반환함
- 개발 빌드에서만 내보내짐 -- 프로덕션에서는 `undefined`

## Owner Stack vs Component Stack

- Component Stack: 에러에서 루트까지 트리 내의 모든 컴포넌트를 나열함 (`fieldset`, `main` 같은 DOM 요소 포함)
- Owner Stack: 해당 노드를 포함하는 컴포넌트를 생성한 컴포넌트만 나열함 (형제, DOM 컴포넌트, 자식만 전달한 컴포넌트 생략)

## 사용 패턴

- 패치된 `console.error`에서 owner stack을 캡처하여 커스텀 에러 오버레이를 개선함
- React 에러 핸들러(`onCaughtError`, `onRecoverableError`, `onUncaughtError`)에서 사용함

## 문제 해결

- Owner Stack이 `null`: React가 제어하지 않는 함수(예: setTimeout, fetch 콜백, 커스텀 DOM 이벤트 핸들러) 내에서 호출됨 -- Effect 본문이나 React 이벤트 핸들러 내에서 호출해야 함
- `captureOwnerStack`을 사용할 수 없음: 개발 빌드에서만 제공됨 -- `import * as React from 'react'`를 사용하고 `process.env.NODE_ENV !== 'production'`을 확인해야 함

---

# addTransitionType

> 원문: https://react.dev/reference/react/addTransitionType

## 개요

- `addTransitionType`은 전환의 원인을 지정할 수 있게 해주는 함수임 (Canary/실험적)

```js
addTransitionType(type: string): void
```

## API

### 매개변수

- `type` (string): 추가할 전환 타입. 어떤 문자열이든 가능함

### 반환값

- 없음

## 주의사항

- 여러 전환이 결합되면 모든 Transition Type이 수집됨
- 하나의 Transition에 여러 타입을 추가할 수 있음
- Transition Type은 각 커밋 후 초기화됨 -- Suspense 폴백은 `startTransition` 이후의 타입을 연관시키지만 콘텐츠 표시 시에는 연관시키지 않음
- `startTransition`의 콜백 내에서 호출해야 함

## 커스터마이징 방법 3가지

### 1. 브라우저 View Transition 타입

- React가 모든 Transition Type을 브라우저 View Transition 타입으로 요소에 추가함
- CSS에서 `:root:active-view-transition-type(my-transition-type)` 사용

### 2. View Transition 클래스

- ViewTransition props(`enter`, `exit`, `update`, `layout`, `share`)에 타입을 CSS 클래스로 매핑하는 객체를 전달함
- 여러 타입이 매치되면 조인됨
- 매치되는 타입이 없으면 `"default"` 항목이 사용됨
- 어떤 타입의 값이 `"none"`이면 ViewTransition이 비활성화됨

### 3. ViewTransition 이벤트

- `onUpdate={(inst, types) => {...}}`를 사용하여 타입에 기반한 명령형 애니메이션을 수행함

## 사용 예시

```js
import {ViewTransition, addTransitionType, useState, startTransition} from 'react';

function Item() {
  return (
    <ViewTransition
      enter={{
        'add-video-back': 'slide-in-back',
        'add-video-forward': 'slide-in-forward',
      }}
      exit={{
        'remove-video-back': 'slide-in-forward',
        'remove-video-forward': 'slide-in-back',
      }}>
      <Video video={videos[0]} />
    </ViewTransition>
  );
}

export default function Component() {
  const [showItem, setShowItem] = useState(false);
  return (
    <>
      <div className="button-container">
        <button onClick={() => {
          startTransition(() => {
            if (showItem) { addTransitionType('remove-video-back'); }
            else { addTransitionType('add-video-back'); }
            setShowItem((prev) => !prev);
          });
        }}>Back</button>
        <button onClick={() => {
          startTransition(() => {
            if (showItem) { addTransitionType('remove-video-forward'); }
            else { addTransitionType('add-video-forward'); }
            setShowItem((prev) => !prev);
          });
        }}>Forward</button>
      </div>
      {showItem ? <Item /> : null}
    </>
  );
}
```

---

# React DOM 컴포넌트 (React DOM Components)

---

# 공통 컴포넌트 (Common Components)

> 원문: https://react.dev/reference/react-dom/components/common

## 개요

- 모든 내장 브라우저 컴포넌트(`<div>`, `<span>` 등)는 공통 props와 이벤트를 지원함

## React 전용 Props

- `children` (React 노드): 요소 내부의 콘텐츠를 지정함. 요소, 문자열, 숫자, 포탈, null, undefined, 불리언, 배열
- `dangerouslySetInnerHTML`: `{ __html: '<p>some html</p>' }` 객체. `innerHTML`을 오버라이드함. 보안 위험 -- 신뢰할 수 있고 살균된 데이터에만 사용해야 함
- `ref`: `useRef`/`createRef`의 ref 객체, 콜백 함수, 또는 레거시 문자열. 기저 DOM 노드에 접근할 수 있음
- `suppressContentEditableWarning` (boolean): `children`과 `contentEditable={true}`를 동시에 가진 요소의 경고를 억제함
- `suppressHydrationWarning` (boolean): 하이드레이션 불일치 경고를 억제함. 한 단계 깊이에서만 동작함
- `style` (객체): CSS 스타일 객체. 예: `{ fontWeight: 'bold', margin: 20 }`. CSS 속성명은 camelCase를 사용함. 숫자에는 자동으로 `px`이 붙음 (단위 없는 속성 제외)

## 표준 DOM Props

- `accessKey` (string): 키보드 단축키
- `aria-*` (다양): ARIA 접근성 속성
- `autoCapitalize` (string): 사용자 입력의 대문자 변환 여부/방법
- `className` (string): CSS 클래스명 (`class` 대신 사용)
- `contentEditable` (boolean): 요소를 직접 편집 가능하게 만듦
- `data-*` (string): 커스텀 데이터 속성
- `dir` (`'ltr'` 또는 `'rtl'`): 텍스트 방향
- `draggable` (boolean): 드래그 가능 여부
- `enterKeyHint` (string): 가상 키보드에서 엔터 키 동작
- `htmlFor` (string): 레이블/출력을 컨트롤에 연결함 (`for` 대신 사용)
- `hidden` (boolean 또는 string): 요소 숨김 여부
- `id` (string): 고유 식별자 (충돌 방지를 위해 `useId` 사용 권장)
- `is` (string): 커스텀 요소처럼 동작하게 만듦
- `inputMode` (string): 표시할 키보드 타입
- `itemProp` (string): 구조화된 데이터 크롤러용 속성
- `lang` (string): 요소의 언어
- `role` (string): 보조 기술용 ARIA 역할
- `slot` (string): Shadow DOM의 슬롯 이름
- `spellCheck` (boolean 또는 null): 맞춤법 검사 활성화/비활성화
- `tabIndex` (number): 기본 Tab 동작 오버라이드. -1과 0 외의 값은 사용을 피해야 함
- `title` (string): 툴팁 텍스트
- `translate` (`'yes'` 또는 `'no'`): 요소 번역 여부

## 커스텀 Props

- 소문자여야 함
- `on`으로 시작하면 안 됨
- 값은 문자열로 변환됨
- 속성을 제거하려면 `null` 또는 `undefined`를 전달함

## 이벤트 핸들러

### 애니메이션 이벤트: `onAnimationStart`, `onAnimationIteration`, `onAnimationEnd`

- 속성: `animationName` (string), `elapsedTime` (number), `pseudoElement` (string)
- 캡처 버전: `onAnimationStartCapture` 등

### 클립보드 이벤트: `onCopy`, `onCut`, `onPaste`

- 속성: `clipboardData` (ClipboardEvent.clipboardData)

### 컴포지션 이벤트 (IME): `onCompositionStart`, `onCompositionUpdate`, `onCompositionEnd`

- 속성: `data` (string)

### 드래그 이벤트: `onDragStart`, `onDragEnd`, `onDragEnter`, `onDragLeave`, `onDragOver`, `onDrop`

- 속성: `dataTransfer` + 모든 MouseEvent 속성

### 포커스 이벤트: `onFocus`, `onBlur`

- 속성: `relatedTarget` (DOM 노드) + UIEvent 속성
- React에서는 `onFocus`와 `onBlur`가 버블링됨 (브라우저 이벤트와 다름)

### 키보드 이벤트: `onKeyDown`, `onKeyUp`

- 속성: `altKey`, `charCode`, `code`, `ctrlKey`, `getModifierState(key)`, `key`, `keyCode`, `locale`, `metaKey`, `location`, `repeat`, `shiftKey`, `which` + UIEvent 속성
- `onKeyPress`는 지원 중단됨. `onKeyDown` 또는 `onBeforeInput` 사용

### 마우스 이벤트: `onClick`, `onMouseEnter`, `onMouseOver`, `onMouseDown`, `onMouseUp`, `onMouseLeave`, `onDoubleClick`, `onMouseMove`, `onMouseOut`, `onAuxClick`, `onContextMenu`

- 속성: `altKey`, `button` (0=주 버튼, 1=휠, 2=보조), `buttons`, `ctrlKey`, `clientX`, `clientY`, `getModifierState(key)`, `metaKey`, `movementX`, `movementY`, `pageX`, `pageY`, `relatedTarget`, `screenX`, `screenY`, `shiftKey` + UIEvent 속성
- `onMouseEnter`와 `onMouseLeave`에는 캡처 단계가 없음

### 포인터 이벤트: `onPointerEnter`, `onPointerMove`, `onPointerDown`, `onPointerUp`, `onPointerLeave`, `onPointerCancel`, `onGotPointerCapture`, `onLostPointerCapture`

- 속성: `height`, `isPrimary`, `pointerId`, `pointerType`, `pressure`, `tangentialPressure`, `tiltX`, `tiltY`, `twist`, `width` + 모든 MouseEvent 속성
- `onPointerEnter`와 `onPointerLeave`에는 캡처 단계가 없음

### 터치 이벤트: `onTouchStart`, `onTouchMove`, `onTouchEnd`, `onTouchCancel`

- 속성: `altKey`, `ctrlKey`, `changedTouches`, `getModifierState(key)`, `metaKey`, `shiftKey`, `touches`, `targetTouches` + UIEvent 속성

### 전환 이벤트: `onTransitionEnd`

- 속성: `elapsedTime`, `propertyName`, `pseudoElement`

### 휠 이벤트: `onWheel`

- 속성: `deltaMode`, `deltaX`, `deltaY`, `deltaZ` + 모든 MouseEvent 속성

### 폼 이벤트 (form 전용): `onSubmit`, `onReset`

### 다이얼로그 이벤트 (dialog 전용): `onCancel`, `onClose`

- React에서 버블링됨

### details 이벤트 (details 전용): `onToggle`

- React에서 버블링됨

### 리소스 이벤트 (img, iframe, object, embed, link, SVG image): `onLoad`, `onError`

- React에서 버블링됨

### 미디어 이벤트 (audio, video): `onAbort`, `onCanPlay`, `onCanPlayThrough`, `onDurationChange`, `onEmptied`, `onEncrypted`, `onEnded`, `onError`, `onLoadedData`, `onLoadedMetadata`, `onLoadStart`, `onPause`, `onPlay`, `onPlaying`, `onProgress`, `onRateChange`, `onResize`, `onSeeked`, `onSeeking`, `onStalled`, `onSuspend`, `onTimeUpdate`, `onVolumeChange`, `onWaiting`

- 모두 React에서 버블링됨

### 기타 이벤트

- `onBeforeInput`: 편집 가능한 요소의 값이 수정되기 전에 발생함 (React가 다른 이벤트로 폴리필함)
- `onSelect`: 편집 가능한 요소 내부의 선택이 변경된 후 발생함. React가 `contentEditable={true}`에서도 동작하도록 확장함
- `onScroll`: 요소가 스크롤되었을 때 발생함. 버블링하지 않음

## React 이벤트 객체 (Synthetic Event)

### 속성

- `bubbles` (boolean), `cancelable` (boolean), `currentTarget` (DOM 노드), `defaultPrevented` (boolean), `eventPhase` (number), `isTrusted` (boolean), `target` (DOM 노드), `timeStamp` (number)
- `nativeEvent` (DOM Event): 원본 브라우저 이벤트 객체

### 메서드

- `preventDefault()`, `stopPropagation()`
- `isDefaultPrevented()`, `isPropagationStopped()`
- `persist()`, `isPersistent()`: React Native 전용

## ref 콜백 함수

```js
<div ref={(node) => {
  console.log('Attached', node);
  return () => {
    console.log('Clean up', node)
  }
}}>
</div>
```

- `node`: DOM 노드 또는 null
- 반환값: 선택적 클린업 함수 (React 19 기능)
- Strict Mode에서는 추가적인 개발 전용 설정+클린업 사이클이 실행됨

## 주의사항

- `children`과 `dangerouslySetInnerHTML`을 동시에 사용할 수 없음
- 일부 이벤트는 브라우저에서 버블링하지 않지만 React에서는 버블링함: `onAbort`, `onLoad`, 다이얼로그 이벤트, details 이벤트, 리소스 로딩 이벤트

## 사용 예시

### CSS 스타일

```js
<img
  className="avatar"
  style={{
    width: user.imageSize,
    height: user.imageSize
  }}
/>
```

### 조건부 CSS 클래스

```js
import cn from 'classnames';
function Row({ isSelected, size }) {
  return (
    <div className={cn('row', {
      selected: isSelected,
      large: size === 'large',
      small: size === 'small',
    })}>
      ...
    </div>
  );
}
```

### ref로 DOM 노드 접근

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
      <button onClick={handleClick}>Focus the input</button>
    </>
  );
}
```

### dangerouslySetInnerHTML (마크다운)

```js
import { Remarkable } from 'remarkable';
const md = new Remarkable();
function renderMarkdownToHTML(markdown) {
  const renderedHTML = md.render(markdown);
  return {__html: renderedHTML};
}
export default function MarkdownPreview({ markdown }) {
  const markup = renderMarkdownToHTML(markdown);
  return <div dangerouslySetInnerHTML={markup} />;
}
```

### 포커스 이벤트 (부모 진입/이탈 감지)

```js
export default function FocusExample() {
  return (
    <div
      tabIndex={1}
      onFocus={(e) => {
        if (e.currentTarget === e.target) {
          console.log('focused parent');
        } else {
          console.log('focused child', e.target.name);
        }
        if (!e.currentTarget.contains(e.relatedTarget)) {
          console.log('focus entered parent');
        }
      }}
      onBlur={(e) => {
        if (e.currentTarget === e.target) {
          console.log('unfocused parent');
        } else {
          console.log('unfocused child', e.target.name);
        }
        if (!e.currentTarget.contains(e.relatedTarget)) {
          console.log('focus left parent');
        }
      }}
    >
      <label>First name: <input name="firstName" /></label>
      <label>Last name: <input name="lastName" /></label>
    </div>
  );
}
```

---

# form

> 원문: https://react.dev/reference/react-dom/components/form

## 개요

- 내장 브라우저 `<form>` 컴포넌트로 정보 제출을 위한 인터랙티브 컨트롤을 생성할 수 있음

```js
<form action={search}>
    <input name="query" />
    <button type="submit">Search</button>
</form>
```

## Props

- 모든 공통 요소 props를 지원함
- `action` (URL 또는 function): URL이면 표준 HTML 폼처럼 동작함. 함수이면 Transition 내에서 폼 제출을 처리함. 함수는 `FormData`를 단일 인수로 받음. 비동기 가능함. `<button>`, `<input type="submit">`, `<input type="image">`의 `formAction` 속성으로 오버라이드 가능함

## 주의사항

- `action`이나 `formAction`에 함수를 전달하면 `method` prop과 관계없이 HTTP 메서드가 POST가 됨

## 사용 예시

### onSubmit으로 처리

```js
export default function Search() {
  function handleSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    const query = formData.get("query");
    alert(`You searched for '${query}'`);
  }
  return (
    <form onSubmit={handleSubmit}>
      <input name="query" />
      <button type="submit">Search</button>
    </form>
  );
}
```

### action prop으로 처리

```js
export default function Search() {
  function search(formData) {
    const query = formData.get("query");
    alert(`You searched for '${query}'`);
  }
  return (
    <form action={search}>
      <input name="query" />
      <button type="submit">Search</button>
    </form>
  );
}
```

- `action`은 Transition 내에서 실행되어 `e.preventDefault()`가 필요 없고, 비제어 필드는 성공 후 자동으로 초기화됨

### 서버 함수

```jsx
import { updateCart } from './lib.js';
function AddToCart({productId}) {
  async function addToCart(formData) {
    'use server'
    const productId = formData.get('productId')
    await updateCart(productId)
  }
  return (
    <form action={addToCart}>
        <input type="hidden" name="productId" value={productId} />
        <button type="submit">Add to Cart</button>
    </form>
  );
}
```

### bind와 함께 사용

```jsx
import { updateCart } from './lib.js';
function AddToCart({productId}) {
  async function addToCart(productId, formData) {
    "use server";
    await updateCart(productId)
  }
  const addProductToCart = addToCart.bind(null, productId);
  return (
    <form action={addProductToCart}>
      <button type="submit">Add to Cart</button>
    </form>
  );
}
```

### useFormStatus로 대기 상태 표시

```js
import { useFormStatus } from "react-dom";
import { submitForm } from "./actions.js";

function Submit() {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {pending ? "Submitting..." : "Submit"}
    </button>
  );
}

function Form({ action }) {
  return (
    <form action={action}>
      <Submit />
    </form>
  );
}

export default function App() {
  return <Form action={submitForm} />;
}
```

### useOptimistic으로 낙관적 업데이트

```js
import { useOptimistic, useState, useRef } from "react";
import { deliverMessage } from "./actions.js";

function Thread({ messages, sendMessage }) {
  const formRef = useRef();
  async function formAction(formData) {
    addOptimisticMessage(formData.get("message"));
    formRef.current.reset();
    await sendMessage(formData);
  }
  const [optimisticMessages, addOptimisticMessage] = useOptimistic(
    messages,
    (state, newMessage) => [
      ...state,
      { text: newMessage, sending: true }
    ]
  );
  return (
    <>
      {optimisticMessages.map((message, index) => (
        <div key={index}>
          {message.text}
          {!!message.sending && <small> (Sending...)</small>}
        </div>
      ))}
      <form action={formAction} ref={formRef}>
        <input type="text" name="message" placeholder="Hello!" />
        <button type="submit">Send</button>
      </form>
    </>
  );
}

export default function App() {
  const [messages, setMessages] = useState([
    { text: "Hello there!", sending: false, key: 1 }
  ]);
  async function sendMessage(formData) {
    const sentMessage = await deliverMessage(formData.get("message"));
    setMessages((messages) => [...messages, { text: sentMessage }]);
  }
  return <Thread messages={messages} sendMessage={sendMessage} />;
}
```

### Error Boundary로 에러 처리

```js
import { ErrorBoundary } from "react-error-boundary";

export default function Search() {
  function search() {
    throw new Error("search error");
  }
  return (
    <ErrorBoundary fallback={<p>There was an error while submitting the form</p>}>
      <form action={search}>
        <input name="query" />
        <button type="submit">Search</button>
      </form>
    </ErrorBoundary>
  );
}
```

### useActionState로 에러 표시

```js
import { useActionState } from "react";
import { signUpNewUser } from "./api";

export default function Page() {
  async function signup(prevState, formData) {
    "use server";
    const email = formData.get("email");
    try {
      await signUpNewUser(email);
      alert(`Added "${email}"`);
    } catch (err) {
      return err.toString();
    }
  }
  const [message, signupAction] = useActionState(signup, null);
  return (
    <>
      <h1>Signup for my newsletter</h1>
      <form action={signupAction} id="signup-form">
        <label htmlFor="email">Email: </label>
        <input name="email" id="email" placeholder="react@example.com" />
        <button>Sign up</button>
        {!!message && <p>{message}</p>}
      </form>
    </>
  );
}
```

### formAction으로 여러 제출 타입

```js
export default function Search() {
  function publish(formData) {
    const content = formData.get("content");
    const button = formData.get("button");
    alert(`'${content}' was published with the '${button}' button`);
  }
  function save(formData) {
    const content = formData.get("content");
    alert(`Your draft of '${content}' has been saved!`);
  }
  return (
    <form action={publish}>
      <textarea name="content" rows={4} cols={40} />
      <br />
      <button type="submit" name="button" value="submit">Publish</button>
      <button formAction={save}>Save draft</button>
    </form>
  );
}
```

---

# input

> 원문: https://react.dev/reference/react-dom/components/input

## 개요

- 내장 브라우저 `<input>` 컴포넌트로 다양한 종류의 폼 입력을 렌더링할 수 있음

## 제어 입력 Props

- `checked` (boolean): 체크박스/라디오용. `onChange`와 함께 사용해야 함
- `value` (string): 텍스트 입력용. `onChange`와 함께 사용해야 함

## 비제어 입력 Props (초기값)

- `defaultChecked` (boolean): 체크박스/라디오의 초기값
- `defaultValue` (string): 텍스트 입력의 초기값

## 폼 관련 Props

- `formAction` (string 또는 function): submit/image 타입에서 부모 `<form action>`을 오버라이드함
- `formEnctype`, `formMethod`, `formNoValidate`, `formTarget`: 부모 폼 속성을 오버라이드함
- `form` (string): 이 입력이 속하는 `<form>`의 ID

## 입력 타입 및 동작 Props

- `type` (string): 입력 타입 (기본값: text)
- `accept` (string): `type="file"`에서 허용 파일 타입
- `alt` (string): `type="image"`의 대체 텍스트
- `autoComplete` (string): 자동완성 동작
- `autoFocus` (boolean): 마운트 시 포커스
- `capture` (string): `type="file"`에서 캡처할 미디어
- `dirname` (string): 방향성 폼 필드 이름
- `disabled` (boolean): 비대화형
- `list` (string): `<datalist>`의 ID
- `multiple` (boolean): file/email에서 복수 값 허용
- `name` (string): 폼과 함께 제출되는 이름
- `pattern` (string): 값이 일치해야 하는 패턴
- `placeholder` (string): 비어 있을 때 흐린 텍스트
- `readOnly` (boolean): 편집 불가
- `required` (boolean): 폼 제출에 필수
- `size` (number): 너비와 유사한 설정

## 숫자/날짜 Props

- `max`, `min` (number): 최대/최소값
- `maxLength`, `minLength` (number): 최대/최소 텍스트 길이
- `step` (양수 또는 `'any'`): 유효한 값 사이의 간격

## 이미지 입력 Props

- `height`, `width` (string): `type="image"`의 이미지 크기
- `src` (string): 이미지 소스

## 이벤트 핸들러

- `onChange` (제어에 필수): 값이 변경될 때마다 즉시 발생함 (모든 키 입력). 브라우저 `input` 이벤트처럼 동작함
- `onInvalid`: 폼 제출 시 유효성 검사 실패 시 발생함. React에서 버블링됨
- `onSelect`: 편집 가능한 요소 내부의 선택이 변경된 후 발생함. 빈 선택과 편집에도 발생하도록 React가 확장함

## 주의사항

- 체크박스는 `value`가 아니라 `checked` (또는 `defaultChecked`)가 필요함
- `value`를 문자열로 전달하면 입력이 제어됨
- `checked`를 불리언으로 전달하면 체크박스/라디오가 제어됨
- 제어와 비제어를 동시에 할 수 없음
- 수명 동안 제어/비제어를 전환할 수 없음
- 모든 제어 입력은 값을 동기적으로 업데이트하는 `onChange`가 필요함

## 사용 예시

### 다양한 입력 타입

```js
export default function MyForm() {
  return (
    <>
      <label>Text input: <input name="myInput" /></label>
      <hr />
      <label>Checkbox: <input type="checkbox" name="myCheckbox" /></label>
      <hr />
      <p>
        Radio buttons:
        <label><input type="radio" name="myRadio" value="option1" /> Option 1</label>
        <label><input type="radio" name="myRadio" value="option2" /> Option 2</label>
        <label><input type="radio" name="myRadio" value="option3" /> Option 3</label>
      </p>
    </>
  );
}
```

### useId로 라벨 연결

```js
import { useId } from 'react';
export default function Form() {
  const ageInputId = useId();
  return (
    <>
      <label>Your first name: <input name="firstName" /></label>
      <hr />
      <label htmlFor={ageInputId}>Your age:</label>
      <input id={ageInputId} name="age" type="number" />
    </>
  );
}
```

### 초기값 설정

```js
export default function MyForm() {
  return (
    <>
      <label>Text input: <input name="myInput" defaultValue="Some initial value" /></label>
      <hr />
      <label>Checkbox: <input type="checkbox" name="myCheckbox" defaultChecked={true} /></label>
      <hr />
      <p>
        Radio buttons:
        <label><input type="radio" name="myRadio" value="option1" /> Option 1</label>
        <label><input type="radio" name="myRadio" value="option2" defaultChecked={true} /> Option 2</label>
        <label><input type="radio" name="myRadio" value="option3" /> Option 3</label>
      </p>
    </>
  );
}
```

### 제출 시 값 읽기

```js
export default function MyForm() {
  function handleSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    fetch('/some-api', { method: form.method, body: formData });
    const formJson = Object.fromEntries(formData.entries());
    console.log(formJson);
  }
  return (
    <form method="post" onSubmit={handleSubmit}>
      <label>Text input: <input name="myInput" defaultValue="Some initial value" /></label>
      <hr />
      <label>Checkbox: <input type="checkbox" name="myCheckbox" defaultChecked={true} /></label>
      <hr />
      <p>
        Radio buttons:
        <label><input type="radio" name="myRadio" value="option1" /> Option 1</label>
        <label><input type="radio" name="myRadio" value="option2" defaultChecked={true} /> Option 2</label>
        <label><input type="radio" name="myRadio" value="option3" /> Option 3</label>
      </p>
      <hr />
      <button type="reset">Reset form</button>
      <button type="submit">Submit form</button>
    </form>
  );
}
```

- `<form>` 내부의 `type` 속성 없는 `<button>`은 폼을 제출함. 제출하지 않으려면 `<button type="button">`을 사용해야 함

### 제어 입력

```js
import { useState } from 'react';
export default function Form() {
  const [firstName, setFirstName] = useState('');
  const [age, setAge] = useState('20');
  const ageAsNumber = Number(age);
  return (
    <>
      <label>
        First name:
        <input value={firstName} onChange={e => setFirstName(e.target.value)} />
      </label>
      <label>
        Age:
        <input value={age} onChange={e => setAge(e.target.value)} type="number" />
        <button onClick={() => setAge(ageAsNumber + 10)}>Add 10 years</button>
      </label>
      {firstName !== '' && <p>Your name is {firstName}.</p>}
      {ageAsNumber > 0 && <p>Your age is {ageAsNumber}.</p>}
    </>
  );
}
```

- `value`에 `undefined`나 `null`을 전달하면 안 됨. 빈 문자열 `''`로 초기화해야 함
- `value`를 `onChange` 없이 전달하면 타이핑이 불가능함. React가 매 키 입력 후 되돌림

### 리렌더링 최적화

- 입력 상태를 자체 컴포넌트로 분리하여 큰 트리의 리렌더링을 피함
- 상태가 부모에 있어야 하면 `useDeferredValue` 사용

## 문제 해결

- 텍스트 입력이 업데이트되지 않음: `onChange` 없이 `value`를 전달함. `defaultValue` 사용, `onChange` 추가, 또는 `readOnly={true}` 설정
- 체크박스가 업데이트되지 않음: `onChange` 없이 `checked`를 전달함. `e.target.value`가 아니라 `e.target.checked`를 읽어야 함
- 캐럿이 처음으로 점프함: onChange에서 값을 변환(예: `toUpperCase()`)하거나 비동기적으로 업데이트하면 발생함. `e.target.value`로 동기적으로 업데이트해야 함
- 비제어에서 제어로 전환 에러: `value={undefined}`에서 `value="string"`으로 전환하면 발생함. 상태를 `''`로 초기화하거나 `value={someValue ?? ''}`를 사용함

---

# select

> 원문: https://react.dev/reference/react-dom/components/select

## 개요

- 내장 브라우저 `<select>` 컴포넌트로 옵션이 있는 선택 상자를 렌더링할 수 있음

```js
<select>
  <option value="someOption">Some option</option>
  <option value="otherOption">Other option</option>
</select>
```

## Props

- 모든 공통 요소 props를 지원함

### 값 제어

- `value` (string 또는 `multiple={true}`일 때 string 배열): 선택된 옵션을 제어함. `onChange`가 필요함
- `defaultValue` (string 또는 string 배열): 초기 선택된 옵션

### 표준 Props

- `autoComplete` (string): 자동완성 동작
- `autoFocus` (boolean): 마운트 시 포커스
- `children`: `<option>`, `<optgroup>`, `<datalist>`를 받음. 허용된 요소를 렌더링하는 커스텀 컴포넌트도 가능함. 각 `<option>`은 `value`가 있어야 함
- `disabled` (boolean): 비대화형
- `form` (string): 폼의 ID
- `multiple` (boolean): 복수 선택 허용
- `name` (string): 폼과 함께 제출되는 이름
- `onChange` (제어에 필수): 선택이 변경될 때 즉시 발생함
- `required` (boolean): 제출에 필수
- `size` (number): `multiple={true}`일 때 표시되는 항목 수

## 주의사항

- `<option>`에 `selected` 속성을 전달하는 것은 지원하지 않음. `<select defaultValue>` 또는 `<select value>`를 사용해야 함
- `value`를 문자열/배열로 전달하면 제어됨
- 제어와 비제어를 동시에 할 수 없음
- 수명 동안 제어/비제어를 전환할 수 없음
- 모든 제어 select는 값을 동기적으로 업데이트하는 `onChange`가 필요함

## 사용 예시

### 기본 선택

```js
export default function FruitPicker() {
  return (
    <label>
      Pick a fruit:
      <select name="selectedFruit">
        <option value="apple">Apple</option>
        <option value="banana">Banana</option>
        <option value="orange">Orange</option>
      </select>
    </label>
  );
}
```

### 초기 선택 옵션

```js
export default function FruitPicker() {
  return (
    <label>
      Pick a fruit:
      <select name="selectedFruit" defaultValue="orange">
        <option value="apple">Apple</option>
        <option value="banana">Banana</option>
        <option value="orange">Orange</option>
      </select>
    </label>
  );
}
```

### 복수 선택

```js
export default function FruitPicker() {
  return (
    <label>
      Pick some fruits:
      <select name="selectedFruit" defaultValue={['orange', 'banana']} multiple={true}>
        <option value="apple">Apple</option>
        <option value="banana">Banana</option>
        <option value="orange">Orange</option>
      </select>
    </label>
  );
}
```

### 제출 시 값 읽기

```js
export default function EditPost() {
  function handleSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    fetch('/some-api', { method: form.method, body: formData });
    console.log(new URLSearchParams(formData).toString());
    const formJson = Object.fromEntries(formData.entries());
    console.log(formJson); // 복수 선택 값이 포함되지 않음
    console.log([...formData.entries()]); // 복수 선택 값 포함
  }
  return (
    <form method="post" onSubmit={handleSubmit}>
      <label>
        Pick your favorite fruit:
        <select name="selectedFruit" defaultValue="orange">
          <option value="apple">Apple</option>
          <option value="banana">Banana</option>
          <option value="orange">Orange</option>
        </select>
      </label>
      <label>
        Pick all your favorite vegetables:
        <select name="selectedVegetables" multiple={true} defaultValue={['corn', 'tomato']}>
          <option value="cucumber">Cucumber</option>
          <option value="corn">Corn</option>
          <option value="tomato">Tomato</option>
        </select>
      </label>
      <hr />
      <button type="reset">Reset</button>
      <button type="submit">Submit</button>
    </form>
  );
}
```

- `Object.fromEntries(formData.entries())`는 복수 선택 값을 포함하지 않음. `[...formData.entries()]`를 사용해야 함

### 제어 선택

```js
import { useState } from 'react';
export default function FruitPicker() {
  const [selectedFruit, setSelectedFruit] = useState('orange');
  const [selectedVegs, setSelectedVegs] = useState(['corn', 'tomato']);
  return (
    <>
      <label>
        Pick a fruit:
        <select value={selectedFruit} onChange={e => setSelectedFruit(e.target.value)}>
          <option value="apple">Apple</option>
          <option value="banana">Banana</option>
          <option value="orange">Orange</option>
        </select>
      </label>
      <hr />
      <label>
        Pick all your favorite vegetables:
        <select
          multiple={true}
          value={selectedVegs}
          onChange={e => {
            const options = [...e.target.selectedOptions];
            const values = options.map(option => option.value);
            setSelectedVegs(values);
          }}
        >
          <option value="cucumber">Cucumber</option>
          <option value="corn">Corn</option>
          <option value="tomato">Tomato</option>
        </select>
      </label>
      <hr />
      <p>Your favorite fruit: {selectedFruit}</p>
      <p>Your favorite vegetables: {selectedVegs.join(', ')}</p>
    </>
  );
}
```

- 복수 선택에서는 `e.target.selectedOptions`를 읽어 값 배열로 매핑해야 함
- `onChange` 없이 `value`를 전달하면 옵션을 선택할 수 없음

---

# textarea

> 원문: https://react.dev/reference/react-dom/components/textarea

## 개요

- 내장 브라우저 `<textarea>` 컴포넌트로 여러 줄 텍스트 입력을 렌더링할 수 있음

```js
<textarea />
```

## Props

- 모든 공통 요소 props를 지원함

### 값 제어

- `value` (string): textarea 내부의 텍스트를 제어함. `onChange`가 필요함
- `defaultValue` (string): 비제어 textarea의 초기값

### HTML 속성

- `autoComplete` (`'on'` 또는 `'off'`): 자동완성 동작
- `autoFocus` (boolean): 마운트 시 포커스
- `children`: 허용되지 않음. `defaultValue`를 사용해야 함
- `cols` (number): 문자 너비 기본값. 기본 `20`
- `disabled` (boolean): 비대화형
- `form` (string): 폼의 ID
- `maxLength` (number): 최대 텍스트 길이
- `minLength` (number): 최소 텍스트 길이
- `name` (string): 폼과 함께 제출되는 이름
- `placeholder` (string): 비어 있을 때 흐린 텍스트
- `readOnly` (boolean): 편집 불가
- `required` (boolean): 제출에 필수
- `rows` (number): 문자 높이 기본값. 기본 `2`
- `wrap` (`'hard'` 또는 `'soft'` 또는 `'off'`): 제출 시 텍스트 래핑 방법

### 이벤트 핸들러

- `onChange` (제어에 필수): 값이 변경될 때마다 즉시 발생함 (모든 키 입력). 브라우저 `input` 이벤트처럼 동작함
- `onSelect`: 편집 가능한 요소 내부의 선택이 변경된 후 발생함. 빈 선택과 편집에도 발생하도록 React가 확장함

## 주의사항

- `<textarea>something</textarea>` 형태의 children은 허용되지 않음. `defaultValue`를 사용해야 함
- `value`를 문자열로 전달하면 제어됨
- 제어와 비제어를 동시에 할 수 없음
- 수명 동안 제어/비제어를 전환할 수 없음
- 모든 제어 textarea는 값을 동기적으로 업데이트하는 `onChange`가 필요함

## 사용 예시

### 기본 textarea

```js
export default function NewPost() {
  return (
    <label>
      Write your post:
      <textarea name="postContent" rows={4} cols={40} />
    </label>
  );
}
```

### useId로 라벨 연결

```js
import { useId } from 'react';
export default function Form() {
  const postTextAreaId = useId();
  return (
    <>
      <label htmlFor={postTextAreaId}>Write your post:</label>
      <textarea id={postTextAreaId} name="postContent" rows={4} cols={40} />
    </>
  );
}
```

### 초기값

```js
export default function EditPost() {
  return (
    <label>
      Edit your post:
      <textarea name="postContent" defaultValue="I really enjoyed biking yesterday!" rows={4} cols={40} />
    </label>
  );
}
```

- HTML과 달리 `<textarea>Some content</textarea>` 형태는 지원되지 않음

### 제출 시 값 읽기

```js
export default function EditPost() {
  function handleSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    fetch('/some-api', { method: form.method, body: formData });
    const formJson = Object.fromEntries(formData.entries());
    console.log(formJson);
  }
  return (
    <form method="post" onSubmit={handleSubmit}>
      <label>Post title: <input name="postTitle" defaultValue="Biking" /></label>
      <label>
        Edit your post:
        <textarea name="postContent" defaultValue="I really enjoyed biking yesterday!" rows={4} cols={40} />
      </label>
      <hr />
      <button type="reset">Reset edits</button>
      <button type="submit">Save post</button>
    </form>
  );
}
```

### 제어 textarea (마크다운 에디터)

```js
import { useState } from 'react';
import MarkdownPreview from './MarkdownPreview.js';

export default function MarkdownEditor() {
  const [postContent, setPostContent] = useState('_Hello,_ **Markdown**!');
  return (
    <>
      <label>
        Enter some markdown:
        <textarea value={postContent} onChange={e => setPostContent(e.target.value)} />
      </label>
      <hr />
      <MarkdownPreview markdown={postContent} />
    </>
  );
}
```

```js
import { Remarkable } from 'remarkable';
const md = new Remarkable();
export default function MarkdownPreview({ markdown }) {
  const renderedHTML = md.render(markdown);
  return <div dangerouslySetInnerHTML={{__html: renderedHTML}} />;
}
```

- `onChange` 없이 `value`를 전달하면 타이핑이 불가능함

## 문제 해결

- textarea가 타이핑 시 업데이트되지 않음: `onChange` 없이 `value`를 전달함. `defaultValue` 사용, `onChange` 추가, 또는 `readOnly={true}` 설정
- 캐럿이 처음으로 점프함: onChange에서 값을 변환하거나 비동기적으로 업데이트하면 발생함. `e.target.value`로 동기적으로 업데이트해야 함
- 비제어에서 제어로 전환 에러: `value={undefined}`에서 `value="string"`으로 전환하면 발생함. 상태를 `''`로 초기화하거나 `value={someValue ?? ''}`를 사용함
