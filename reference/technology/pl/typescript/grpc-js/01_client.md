# gRPC JavaScript / TypeScript 클라이언트 (@grpc/grpc-js)

> Node.js에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리

> 원본 참고: https://grpc.io/docs/languages/node/basics/

<a id="라이브러리-개요"></a>
### 라이브러리 개요

- `@grpc/grpc-js`: 순수 JS로 구현된 공식 gRPC 런타임, Node.js에서 사실상 표준(구 `grpc` 네이티브 모듈 대체)
- `@grpc/proto-loader`: 런타임에 `.proto`를 읽어 동적으로 클라이언트 생성
- `grpc-tools` / `ts-proto`: `.proto`를 미리 정적 코드(JS/TS 스텁)로 생성
- `nice-grpc`, `@pbkit/grpc-client`: `@grpc/grpc-js` 위에 Promise/async 친화적 API를 얹은 래퍼

`@grpc/grpc-js`의 기본 API는 콜백 스타일이다. 래퍼 라이브러리는 같은 전송 계층 위에서 콜백을 Promise나 async iterator로 바꿔준다. 이 문서는 그 기반인 `@grpc/grpc-js`를 기준으로 설명한다.

- 공통 `.proto`:

```proto
syntax = "proto3";
package routeguide;

service RouteGuide {
  rpc GetFeature(Point) returns (Feature) {}
  rpc ListFeatures(Rectangle) returns (stream Feature) {}
  rpc RecordRoute(stream Point) returns (RouteSummary) {}
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
}

message Point { int32 latitude = 1; int32 longitude = 2; }
```

<a id="코드-생성-방식"></a>

### 코드 생성 방식

#### 방식 A: 동적 로딩 (proto-loader)

동적 로딩은 별도의 코드 생성 단계 없이 런타임에 `.proto`를 읽는 방식이다. 아래에서는 `proto-loader`로 읽은 정의를 `grpc.loadPackageDefinition`에 넘겨 `RouteGuide` 클라이언트 생성자를 얻는다.

```js
const grpc = require('@grpc/grpc-js')
const protoLoader = require('@grpc/proto-loader')

const packageDef = protoLoader.loadSync('routeguide/route_guide.proto', {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
})
const proto = grpc.loadPackageDefinition(packageDef).routeguide
// proto.RouteGuide 가 클라이언트 생성자
```

#### 방식 B: 정적 생성 (권장)

- `protoc` + `grpc-tools` 플러그인으로 스텁을 미리 생성

```bash
protoc \
  --plugin=protoc-gen-grpc=./node_modules/.bin/grpc_tools_node_protoc_plugin \
  --js_out=import_style=commonjs,binary:./generated \
  --grpc_out=grpc_js:./generated \
  routeguide/route_guide.proto
```

- 생성물의 `route_guide_grpc_pb.js`에 `RouteGuideClient` 생성자가 들어 있음

> 위 명령은 JavaScript 스텁(`*_pb.js`, `*_grpc_pb.js`)만 만들고 `.d.ts` 타입 정의는 생성하지 않음. TypeScript 타입까지 얻으려면 `grpc_tools_node_protoc_ts`(`--ts_out`)를 함께 쓰거나, `.proto`를 곧장 TS로 컴파일하는 `ts-proto`를 사용.

<a id="클라이언트채널-생성"></a>

### 클라이언트(채널) 생성

- 클라이언트 인스턴스가 곧 채널 → 생성 비용이 크므로 앱당 한 번 만들어 재사용

```js
const grpc = require('@grpc/grpc-js')

const client = new proto.RouteGuide(
  'localhost:8980',
  grpc.credentials.createInsecure(), // 암호화 없는 연결. 운영에서는 createSsl()
)
```

- TLS를 쓸 때:

```js
const credentials = grpc.credentials.createSsl(/* rootCerts, privateKey, certChain */)
const client = new proto.RouteGuide('example.com:443', credentials)
```

<a id="4가지-rpc-타입-호출"></a>

### 4가지 RPC 타입 호출

- `@grpc/grpc-js`의 기본 API는 콜백 + 스트림(EventEmitter) 스타일

#### 단방향 (Unary), 콜백

```js
client.getFeature(
  { latitude: 409146138, longitude: -746188906 },
  (err, feature) => {
    if (err) return console.error(err)
    console.log(feature.name)
  },
)
```

#### 서버 스트리밍 (Server streaming)

서버 스트리밍 호출은 읽기 가능한 스트림을 반환한다. 각 응답은 `data` 이벤트로 받고, `end`와 `error` 이벤트로 정상 종료와 실패를 처리한다.

```js
const call = client.listFeatures({
  lo: { latitude: 400000000, longitude: -750000000 },
  hi: { latitude: 420000000, longitude: -730000000 },
})
call.on('data', (feature) => console.log(feature.name))
call.on('end', () => console.log('완료'))
call.on('error', (err) => console.error(err))
```

#### 클라이언트 스트리밍 (Client streaming)

클라이언트 스트리밍 호출은 쓰기 가능한 스트림을 반환한다. `write`로 여러 요청을 보낸 뒤 `end()`로 송신을 마치면, 서버가 반환하는 단일 응답을 콜백에서 받는다.

```js
const call = client.recordRoute((err, summary) => {
  if (err) return console.error(err)
  console.log(`방문 지점 수: ${summary.point_count}`)
})
;[
  { latitude: 407838351, longitude: -746143763 },
  { latitude: 408122808, longitude: -743999179 },
].forEach((p) => call.write(p))
call.end()
```

#### 양방향 스트리밍 (Bidirectional streaming)

양방향 스트리밍에서는 반환된 스트림으로 요청을 쓰면서 응답도 읽는다. 입력과 출력이 독립적으로 흐르므로, 요청 전송을 마치기 전에도 `data` 이벤트로 응답을 받을 수 있다.

```js
const call = client.routeChat()
call.on('data', (note) => console.log(`받음: ${note.message}`))
call.on('end', () => console.log('서버가 스트림 종료'))

call.write({ message: 'First', location: { latitude: 0, longitude: 1 } })
call.write({ message: 'Second', location: { latitude: 0, longitude: 2 } })
call.end() // 더 보낼 게 없으면 닫기
```

<a id="콜백을-promise로-감싸기"></a>

### 콜백을 Promise로 감싸기

기존 코드가 `async/await`로 비동기 흐름을 연결한다면 gRPC 호출도 Promise로 바꿔 사용할 수 있다. 단방향 호출은 `util.promisify`로 감싸거나 직접 래핑한다. 아래에서는 메서드의 `this`를 유지하도록 클라이언트에 먼저 바인딩한다.

```js
const { promisify } = require('util')

// 먼저 client에 바인딩한 뒤 promisify (this 유실 방지)
const getFeature = promisify(client.getFeature.bind(client))

async function main() {
  const feature = await getFeature({ latitude: 409146138, longitude: -746188906 })
  console.log(feature.name)
}
```

- 서버 스트리밍을 `for await`로 소비하려면 async iterator로 감쌈

```js
async function* toAsyncIterable(call) {
  const queue = []
  let done = false, resolve
  call.on('data', (d) => { queue.push(d); resolve && resolve() })
  call.on('end', () => { done = true; resolve && resolve() })
  while (!done || queue.length) {
    if (!queue.length) await new Promise((r) => (resolve = r))
    while (queue.length) yield queue.shift()
  }
}

for await (const feature of toAsyncIterable(client.listFeatures(rect))) {
  console.log(feature.name)
}
```

> `nice-grpc`, `@pbkit/grpc-client` 같은 래퍼를 쓰면 단방향은 Promise, 스트리밍은 async iterator로 처음부터 제공되므로 위 보일러플레이트가 불필요.

<a id="메타데이터데드라인에러"></a>

### 메타데이터, 데드라인, 에러

#### 메타데이터

- 요청 헤더는 `grpc.Metadata`로 만들어 호출 시 함께 전달

```js
const metadata = new grpc.Metadata()
metadata.add('authorization', 'Bearer token')

client.getFeature(point, metadata, (err, feature) => { /* ... */ })
```

#### 데드라인(타임아웃)

- 세 번째 인자 `options`에 절대 시각으로 데드라인을 지정

```js
const deadline = new Date()
deadline.setSeconds(deadline.getSeconds() + 5) // 5초 후

client.getFeature(point, metadata, { deadline }, (err, feature) => {
  if (err && err.code === grpc.status.DEADLINE_EXCEEDED) {
    console.error('타임아웃')
  }
})
```

#### 에러 코드

- 에러 객체의 `code`를 `grpc.status` 열거형과 비교

```js
client.getFeature(point, (err, feature) => {
  if (err) {
    switch (err.code) {
      case grpc.status.NOT_FOUND:         return console.error('없음')
      case grpc.status.UNAVAILABLE:       return console.error('서버 다운')
      case grpc.status.DEADLINE_EXCEEDED: return console.error('타임아웃')
      default:                            return console.error(err.details)
    }
  }
  console.log(feature.name)
})
```
