# WHATWG Streams Standard

## 목차

1. [개요](#1-개요)
2. [스트림 기본 개념](#2-스트림-기본-개념)
3. [ReadableStream](#3-readablestream)
4. [ReadableStreamDefaultReader](#4-readablestreamdefaultreader)
5. [ReadableStreamBYOBReader](#5-readablestreambyobreader)
6. [ReadableByteStreamController](#6-readablebytestreamcontroller)
7. [ReadableStreamDefaultController](#7-readablestreamdefaultcontroller)
8. [WritableStream](#8-writablestream)
9. [WritableStreamDefaultWriter](#9-writablestreamdefaultwriter)
10. [WritableStreamDefaultController](#10-writablestreamdefaultcontroller)
11. [TransformStream](#11-transformstream)
12. [TransformStreamDefaultController](#12-transformstreamdefaultcontroller)
13. [큐잉 전략 (Queuing Strategies)](#13-큐잉-전략-queuing-strategies)
14. [배압 (Backpressure) 메커니즘](#14-배압-backpressure-메커니즘)
15. [파이프 (Piping)](#15-파이프-piping)
16. [Tee (분기)](#16-tee-분기)
17. [바이트 스트림 (Byte Streams)](#17-바이트-스트림-byte-streams)
18. [실용 예제](#18-실용-예제)

---

## 1. 개요

### Streams Standard란

WHATWG Streams Standard는 웹 플랫폼에서 스트리밍 데이터를 생성하고 소비하기 위한 API를 정의하는 표준이다. 이 표준은 데이터를 한 번에 전부 메모리에 올리지 않고, 청크(chunk) 단위로 점진적으로 처리할 수 있는 추상화 계층을 제공한다.

공식 명세: https://streams.spec.whatwg.org/

### 왜 필요한가

전통적인 웹 API(예: `XMLHttpRequest`)는 응답 전체가 도착해야만 데이터를 사용할 수 있었다. 이 접근 방식은 다음과 같은 한계를 가진다.

| 문제 | 설명 |
|------|------|
| 메모리 비효율 | 수 GB 파일을 다운로드할 때 전체를 메모리에 적재해야 한다 |
| 지연 시간 | 전체 데이터가 도착할 때까지 아무것도 할 수 없다 |
| 배압 부재 | 생산자와 소비자 간 속도 차이를 제어할 수 없다 |
| 조합 불가 | 데이터 변환 파이프라인을 유연하게 구성할 수 없다 |

Streams API는 이 모든 문제를 해결한다.

```js
// 전통적 방식: 전체 응답을 한 번에 가져옴
const response = await fetch('/large-file');
const data = await response.text(); // 전체가 메모리에 올라감

// 스트림 방식: 청크 단위로 점진적 처리
const response = await fetch('/large-file');
const reader = response.body.getReader();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  processChunk(value); // 청크 단위로 처리, 메모리 효율적
}
```

### 스트림의 개념

스트림은 시간에 걸쳐 사용 가능해지는 일련의 데이터를 추상화한 것이다. 물리적인 비유로는 수도관을 생각하면 된다. 물(데이터)이 수도관(스트림)을 통해 흐르며, 수도꼭지(소비자)에서 필요한 만큼 받아 사용한다.

Streams Standard는 세 가지 유형의 스트림을 정의한다.

```
ReadableStream  →  TransformStream  →  WritableStream
(데이터 소스)       (데이터 변환)         (데이터 싱크)
```

- ReadableStream: 데이터를 읽을 수 있는 소스. `fetch()` 응답의 `body`가 대표적이다.
- WritableStream: 데이터를 쓸 수 있는 싱크. 파일 쓰기, 네트워크 전송 등.
- TransformStream: 데이터를 변환하는 중간 단계. 읽기 가능 면(readable)과 쓰기 가능 면(writable)을 모두 가진다.

### 역사

| 시기 | 사건 |
|------|------|
| 2013년 | Streams API 초기 논의 시작 |
| 2014년 | WHATWG에서 Streams Standard 초안 작성 시작 (Domenic Denicola 주도) |
| 2015년 | Chrome에서 `ReadableStream` 최초 구현 |
| 2016년 | `WritableStream`, `TransformStream` 명세 추가 |
| 2017년 | Fetch API와 Streams 통합 (response.body) |
| 2020년 | 모든 주요 브라우저에서 기본 Streams API 지원 |
| 2022년 | `ReadableStream.from()` 정적 메서드 추가 |
| 2023-현재 | 바이트 스트림, async iterable 지원 등 지속 확장 |

---

## 2. 스트림 기본 개념

### 청크 (Chunk)

청크는 스트림을 구성하는 개별 데이터 조각이다. 청크는 어떤 타입이든 될 수 있다. 문자열, `Uint8Array`, JSON 객체 등 자바스크립트의 모든 값이 청크가 될 수 있다.

```js
// 문자열 청크를 사용하는 스트림
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('첫 번째 청크');
    controller.enqueue('두 번째 청크');
    controller.enqueue('세 번째 청크');
    controller.close();
  }
});

// 바이트 청크를 사용하는 스트림
const byteStream = new ReadableStream({
  start(controller) {
    controller.enqueue(new Uint8Array([72, 101, 108, 108, 111]));
    controller.enqueue(new Uint8Array([87, 111, 114, 108, 100]));
    controller.close();
  }
});
```

### 내부 큐 (Internal Queue)

모든 스트림은 내부 큐를 가지고 있다. 이 큐는 아직 소비되지 않은 청크들을 임시 보관하는 버퍼 역할을 한다.

```
[생산자] → [ 큐: [청크1][청크2][청크3] ] → [소비자]
             ↑ enqueue()                    ↑ read()
```

내부 큐의 크기는 큐잉 전략(Queuing Strategy)에 의해 관리된다. 큐가 가득 차면 생산자에게 배압(backpressure)이 전달된다.

### 배압 (Backpressure)

배압은 스트림 시스템의 핵심 메커니즘이다. 소비자가 처리할 수 있는 속도보다 생산자가 더 빠르게 데이터를 만들어 내는 상황을 제어한다.

```
빠른 생산자 ──→ [큐가 차오름] ──→ 배압 신호 ──→ 생산자 속도 감소
                                               ↓
느린 소비자 ←── [큐에서 꺼냄] ←── 소비 진행   ←── 큐 여유 생김
                                               ↓
                                          생산자 재개
```

배압이 없으면 메모리 사용량이 무한히 증가하거나 데이터가 손실될 수 있다.

```js
// 배압이 적용되는 예제
const readable = new ReadableStream({
  pull(controller) {
    // pull()은 내부 큐에 여유가 있을 때만 호출된다
    // 즉, 소비자가 데이터를 읽어서 큐에 공간이 생겨야 호출된다
    console.log('desiredSize:', controller.desiredSize);
    controller.enqueue(generateData());
  }
}, new CountQueuingStrategy({ highWaterMark: 5 }));
```

### 파이프 체인 (Pipe Chain)

여러 스트림을 연결하여 데이터 처리 파이프라인을 구성할 수 있다. Unix의 파이프(`|`)와 유사한 개념이다.

```js
// 파이프 체인 예시
// ReadableStream → TransformStream → TransformStream → WritableStream
await readableStream
  .pipeThrough(new TextDecoderStream())       // 바이트 → 문자열 변환
  .pipeThrough(new TransformStream({          // 커스텀 변환
    transform(chunk, controller) {
      controller.enqueue(chunk.toUpperCase());
    }
  }))
  .pipeTo(writableStream);                    // 최종 싱크로 전달
```

파이프 체인에서 배압은 역방향으로 전파된다. 최종 싱크(WritableStream)가 느려지면 그 영향이 체인을 따라 역순으로 전파되어 소스(ReadableStream)의 생산 속도까지 제어한다.

---

## 3. ReadableStream

`ReadableStream`은 데이터를 읽을 수 있는 스트림을 나타낸다. 스트림 API의 가장 기본이 되는 인터페이스이다.

### 생성자

```js
new ReadableStream(underlyingSource?, queuingStrategy?)
```

#### underlyingSource 객체

| 속성 | 설명 |
|------|------|
| `start(controller)` | 스트림 생성 시 한 번 호출. 초기화 로직 수행 |
| `pull(controller)` | 내부 큐에 여유가 있을 때 호출. 데이터 공급 |
| `cancel(reason)` | 스트림이 취소될 때 호출. 리소스 정리 |
| `type` | `"bytes"`로 설정하면 바이트 스트림 생성 |
| `autoAllocateChunkSize` | 바이트 스트림에서 자동 버퍼 할당 크기 |

#### start(controller)

스트림이 생성될 때 즉시 한 번 호출된다. Promise를 반환할 수 있으며, 이 경우 Promise가 이행될 때까지 다른 메서드 호출이 지연된다.

```js
const stream = new ReadableStream({
  async start(controller) {
    // 데이터베이스 연결 등 초기화 작업
    const db = await connectToDatabase();
    this.db = db;

    // 초기 데이터를 즉시 큐에 넣을 수도 있다
    const firstBatch = await db.query('SELECT * FROM items LIMIT 10');
    for (const item of firstBatch) {
      controller.enqueue(item);
    }
  }
});
```

#### pull(controller)

내부 큐의 크기가 `highWaterMark` 미만일 때 반복적으로 호출된다. 비동기 데이터 소스에서 데이터를 가져오는 데 적합하다.

```js
const stream = new ReadableStream({
  start() {
    this.offset = 0;
    this.pageSize = 100;
  },

  async pull(controller) {
    // 페이지네이션된 API에서 데이터를 가져온다
    const response = await fetch(`/api/items?offset=${this.offset}&limit=${this.pageSize}`);
    const data = await response.json();

    if (data.items.length === 0) {
      controller.close(); // 더 이상 데이터가 없으면 스트림 닫기
      return;
    }

    for (const item of data.items) {
      controller.enqueue(item);
    }
    this.offset += this.pageSize;
  }
});
```

중요: `pull()`이 Promise를 반환하면, 해당 Promise가 이행될 때까지 다음 `pull()` 호출이 보류된다. 이를 통해 자연스러운 배압이 형성된다.

#### cancel(reason)

소비자가 스트림을 취소했을 때 호출된다. 리소스 정리(파일 핸들 닫기, 네트워크 연결 해제 등)에 사용한다.

```js
const stream = new ReadableStream({
  start(controller) {
    this.intervalId = setInterval(() => {
      controller.enqueue(new Date().toISOString());
    }, 1000);
  },

  cancel(reason) {
    console.log('스트림 취소됨:', reason);
    clearInterval(this.intervalId); // 타이머 정리
  }
});
```

#### type

`"bytes"`로 설정하면 바이트 스트림이 된다. 바이트 스트림은 `ReadableByteStreamController`를 사용하며 BYOB(Bring Your Own Buffer) 리더를 지원한다.

```js
const byteStream = new ReadableStream({
  type: 'bytes',
  start(controller) {
    // controller는 ReadableByteStreamController
    controller.enqueue(new Uint8Array([1, 2, 3]));
  }
});
```

### queuingStrategy

내부 큐의 동작을 제어한다.

| 속성 | 설명 |
|------|------|
| `highWaterMark` | 배압이 적용되기 시작하는 큐 크기 임계값 |
| `size(chunk)` | 각 청크의 크기를 계산하는 함수 |

```js
const stream = new ReadableStream(
  {
    pull(controller) {
      controller.enqueue({ data: 'some data', timestamp: Date.now() });
    }
  },
  {
    highWaterMark: 10,                    // 큐에 최대 10개 청크
    size(chunk) { return 1; }             // 각 청크의 크기는 1
  }
);
```

### 인스턴스 메서드

#### getReader(options?)

스트림에서 데이터를 읽기 위한 Reader를 획득한다. Reader가 활성 상태인 동안 스트림은 잠긴(locked) 상태가 된다.

```js
const reader = stream.getReader();
// stream.locked === true

// 기본 리더
const defaultReader = stream.getReader(); // ReadableStreamDefaultReader

// BYOB 리더 (바이트 스트림 전용)
const byobReader = byteStream.getReader({ mode: 'byob' }); // ReadableStreamBYOBReader
```

#### pipeThrough(transformStream, options?)

스트림 데이터를 `TransformStream`에 통과시킨다. 변환된 결과를 새로운 `ReadableStream`으로 반환한다.

```js
const uppercased = readableStream.pipeThrough(new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk.toUpperCase());
  }
}));

// options 매개변수
const result = readableStream.pipeThrough(transformStream, {
  preventClose: false,    // true면 소스 종료 시 변환 스트림을 닫지 않음
  preventAbort: false,    // true면 소스 오류 시 변환 스트림을 중단하지 않음
  preventCancel: false,   // true면 대상 오류 시 소스 스트림을 취소하지 않음
  signal: abortController.signal  // AbortSignal로 파이프 취소 가능
});
```

#### pipeTo(writableStream, options?)

스트림 데이터를 `WritableStream`에 보낸다. 파이프가 완료되면 이행되는 Promise를 반환한다.

```js
await readableStream.pipeTo(writableStream);

// 옵션과 함께 사용
const controller = new AbortController();

const pipePromise = readableStream.pipeTo(writableStream, {
  preventClose: false,
  preventAbort: false,
  preventCancel: false,
  signal: controller.signal
});

// 필요시 파이프 중단
setTimeout(() => controller.abort('시간 초과'), 5000);

try {
  await pipePromise;
  console.log('파이프 완료');
} catch (e) {
  console.error('파이프 실패:', e);
}
```

#### tee()

스트림을 두 개의 독립적인 스트림으로 분기한다. 원본 스트림의 데이터가 두 분기 모두에 동일하게 전달된다.

```js
const [branch1, branch2] = readableStream.tee();

// branch1과 branch2는 독립적으로 소비할 수 있다
// 원본 스트림은 잠긴 상태가 된다
const reader1 = branch1.getReader();
const reader2 = branch2.getReader();
```

#### cancel(reason?)

스트림을 취소한다. underlying source의 `cancel()` 메서드가 호출되고, 스트림이 닫힌다.

```js
const stream = new ReadableStream({
  cancel(reason) {
    console.log('취소 사유:', reason);
  }
});

await stream.cancel('더 이상 데이터가 필요 없음');
```

#### locked (속성)

스트림이 Reader에 의해 잠겨 있는지 여부를 나타내는 불리언 값이다.

```js
console.log(stream.locked); // false

const reader = stream.getReader();
console.log(stream.locked); // true

reader.releaseLock();
console.log(stream.locked); // false
```

#### values(options?)

스트림을 비동기 이터러블(async iterable)로 변환한다. `for await...of` 루프에서 사용할 수 있다.

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('a');
    controller.enqueue('b');
    controller.enqueue('c');
    controller.close();
  }
});

// values() 사용
for await (const chunk of stream.values()) {
  console.log(chunk); // 'a', 'b', 'c'
}

// 옵션: preventCancel
for await (const chunk of stream.values({ preventCancel: true })) {
  console.log(chunk);
  if (chunk === 'b') break; // preventCancel이 true이므로 스트림이 취소되지 않음
}

// ReadableStream 자체가 async iterable이므로 직접 사용 가능
for await (const chunk of stream) {
  console.log(chunk);
}
```

#### ReadableStream.from(asyncIterable) (정적 메서드)

비동기 이터러블 또는 이터러블을 `ReadableStream`으로 변환한다.

```js
// 배열에서 스트림 생성
const stream1 = ReadableStream.from(['hello', 'world']);

// 제너레이터에서 스트림 생성
async function* generateNumbers() {
  for (let i = 0; i < 100; i++) {
    yield i;
    await new Promise(r => setTimeout(r, 10));
  }
}

const stream2 = ReadableStream.from(generateNumbers());

// async iterable에서 스트림 생성
const asyncIterable = {
  async *[Symbol.asyncIterator]() {
    yield 'first';
    yield 'second';
    yield 'third';
  }
};

const stream3 = ReadableStream.from(asyncIterable);
```

---

## 4. ReadableStreamDefaultReader

`ReadableStreamDefaultReader`는 `ReadableStream`에서 청크를 읽기 위한 기본 리더이다.

### read()

스트림에서 다음 청크를 읽는다. `{ value, done }` 형태의 결과를 반환하는 Promise를 반환한다.

```js
const reader = stream.getReader();

// 단일 읽기
const { value, done } = await reader.read();
if (!done) {
  console.log('읽은 데이터:', value);
}

// 전체 스트림 읽기 패턴
async function readAll(stream) {
  const reader = stream.getReader();
  const chunks = [];

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      chunks.push(value);
    }
    return chunks;
  } finally {
    reader.releaseLock();
  }
}
```

`done`이 `true`이면 스트림이 닫힌 것이며, 이 경우 `value`는 `undefined`이다.

### releaseLock()

리더와 스트림 간의 잠금을 해제한다. 이후 다른 리더를 획득할 수 있다.

```js
const reader = stream.getReader();
// ... 읽기 작업 수행 ...
reader.releaseLock();

// 이제 새 리더를 획득하거나 다른 작업 가능
const newReader = stream.getReader();
```

주의: 아직 미완료된 `read()` 요청이 있는 상태에서 `releaseLock()`을 호출하면, 그 `read()` Promise는 `TypeError`로 거부된다.

### cancel(reason?)

리더를 통해 기반 스트림을 취소한다.

```js
const reader = stream.getReader();
const { value } = await reader.read();

if (value === 'ERROR_MARKER') {
  await reader.cancel('오류 마커 발견');
}
```

### closed (속성)

스트림이 닫히거나 오류가 발생했을 때 이행/거부되는 Promise이다.

```js
const reader = stream.getReader();

reader.closed
  .then(() => console.log('스트림이 정상적으로 닫힘'))
  .catch(err => console.error('스트림 오류:', err));

// 또는 async/await 사용
try {
  await reader.closed;
  console.log('스트림 닫힘');
} catch (err) {
  console.error('오류:', err);
}
```

### 종합 예제

```js
async function processStream(readableStream) {
  const reader = readableStream.getReader();

  try {
    let totalSize = 0;

    // closed Promise를 별도로 감시
    reader.closed.then(() => {
      console.log(`총 처리 크기: ${totalSize} bytes`);
    });

    while (true) {
      const { value, done } = await reader.read();

      if (done) {
        console.log('스트림 소비 완료');
        break;
      }

      totalSize += value.byteLength || value.length || 0;
      await processChunk(value);
    }
  } catch (error) {
    console.error('스트림 읽기 중 오류:', error);
    await reader.cancel(error.message);
  } finally {
    reader.releaseLock();
  }
}
```

---

## 5. ReadableStreamBYOBReader

`ReadableStreamBYOBReader`는 바이트 스트림 전용 리더이다. BYOB는 "Bring Your Own Buffer"의 약자로, 소비자가 직접 제공한 버퍼에 데이터를 채워 넣는다. 이를 통해 메모리 할당을 최소화할 수 있다.

### read(view)

사용자가 제공한 `ArrayBufferView`에 데이터를 읽어 들인다.

```js
const byteStream = new ReadableStream({
  type: 'bytes',
  async pull(controller) {
    const data = await fetchSomeBytes();
    controller.enqueue(new Uint8Array(data));
  }
});

const reader = byteStream.getReader({ mode: 'byob' });

// 1024 바이트 버퍼를 직접 제공
const buffer = new ArrayBuffer(1024);
let view = new Uint8Array(buffer, 0, 1024);

const { value, done } = await reader.read(view);
// value는 데이터가 채워진 새 Uint8Array (원본 view의 버퍼를 전이받음)
// value.byteLength는 실제로 읽힌 바이트 수
```

핵심 개념: `read(view)` 호출 후 원본 `view`는 더 이상 유효하지 않다(내부 `ArrayBuffer`가 전이(transfer)됨). 반환된 `value`를 사용해야 한다.

```js
// 반복적으로 BYOB 읽기
async function readWithBYOB(stream) {
  const reader = stream.getReader({ mode: 'byob' });
  let buffer = new ArrayBuffer(4096);
  let offset = 0;
  const chunks = [];

  try {
    while (true) {
      const view = new Uint8Array(buffer, offset, buffer.byteLength - offset);
      const { value, done } = await reader.read(view);

      if (done) break;

      chunks.push(new Uint8Array(value.buffer, 0, offset + value.byteLength));

      // value.buffer를 재사용하여 다음 읽기에 활용
      buffer = value.buffer;
      offset = 0;
    }
  } finally {
    reader.releaseLock();
  }

  return chunks;
}
```

### releaseLock()

기본 리더와 동일하게 잠금을 해제한다.

```js
const reader = byteStream.getReader({ mode: 'byob' });
// ... 읽기 작업 ...
reader.releaseLock();
```

### cancel(reason?)

바이트 스트림을 취소한다.

```js
const reader = byteStream.getReader({ mode: 'byob' });
await reader.cancel('파일 전송 중단');
```

### closed (속성)

스트림 닫힘/오류를 감지하는 Promise이다. `ReadableStreamDefaultReader.closed`와 동일하게 동작한다.

### BYOB vs Default Reader 비교

```js
const byteStream = new ReadableStream({
  type: 'bytes',
  pull(controller) {
    controller.enqueue(new Uint8Array([1, 2, 3, 4, 5]));
  }
});

// 방법 1: Default Reader (바이트 스트림에서도 사용 가능)
const defaultReader = byteStream.getReader();
const { value: v1 } = await defaultReader.read();
// v1은 스트림이 enqueue한 Uint8Array 그대로

// 방법 2: BYOB Reader
const byobReader = byteStream.getReader({ mode: 'byob' });
const buf = new Uint8Array(10); // 10바이트 버퍼 직접 제공
const { value: v2 } = await byobReader.read(buf);
// v2는 buf의 버퍼에 데이터가 채워진 새 Uint8Array
// 메모리 할당 없이 기존 버퍼 재사용
```

---

## 6. ReadableByteStreamController

`ReadableByteStreamController`는 바이트 스트림(`type: 'bytes'`)의 underlying source에 전달되는 컨트롤러이다.

### byobRequest (속성)

현재 BYOB 리더의 읽기 요청을 나타내는 `ReadableStreamBYOBRequest` 객체이다. BYOB 리더가 `read(view)`를 호출했을 때만 존재하며, 그렇지 않으면 `null`이다.

```js
const stream = new ReadableStream({
  type: 'bytes',

  async pull(controller) {
    if (controller.byobRequest) {
      // BYOB 리더가 사용 중 - 소비자의 버퍼에 직접 쓴다
      const view = controller.byobRequest.view;
      const bytesRead = await readIntoBuffer(view);
      controller.byobRequest.respond(bytesRead);
    } else {
      // 기본 리더가 사용 중 - 새 버퍼를 생성하여 enqueue
      const data = await fetchBytes(1024);
      controller.enqueue(new Uint8Array(data));
    }
  }
});
```

#### byobRequest.view

소비자가 제공한 버퍼의 뷰(view). 이 뷰에 데이터를 직접 쓸 수 있다.

#### byobRequest.respond(bytesWritten)

뷰에 `bytesWritten` 바이트만큼 데이터를 채웠음을 알린다.

```js
async pull(controller) {
  const request = controller.byobRequest;
  if (request) {
    const view = request.view;
    // view 버퍼에 직접 데이터 복사
    const src = await getNextBytes(view.byteLength);
    new Uint8Array(view.buffer, view.byteOffset, src.length).set(src);
    request.respond(src.length);
  }
}
```

#### byobRequest.respondWithNewView(view)

완전히 새로운 뷰로 응답한다. 원본 버퍼와 다른 버퍼를 사용해야 할 때 유용하다.

### desiredSize (속성)

내부 큐를 `highWaterMark`까지 채우는 데 필요한 크기이다. 배압 판단에 사용한다.

```js
pull(controller) {
  console.log('desiredSize:', controller.desiredSize);
  // 양수: 큐에 여유 있음
  // 0: 큐가 가득 참
  // 음수: 큐 초과 상태 (오버플로우)
  // null: 스트림이 닫혔거나 오류 상태
}
```

### close()

스트림을 닫는다. 큐에 남은 청크가 모두 소비된 후 스트림이 종료된다.

```js
pull(controller) {
  if (noMoreData) {
    controller.close();
  }
}
```

### enqueue(chunk)

`Uint8Array` 등의 `ArrayBufferView`를 큐에 추가한다. 바이트 스트림이므로 반드시 바이트 타입이어야 한다.

```js
pull(controller) {
  const data = new Uint8Array(1024);
  fillWithData(data);
  controller.enqueue(data);
}
```

### error(reason)

스트림을 오류 상태로 만든다. 이후 모든 읽기 시도는 거부된다.

```js
pull(controller) {
  try {
    const data = readFromHardware();
    controller.enqueue(data);
  } catch (e) {
    controller.error(new Error('하드웨어 읽기 실패: ' + e.message));
  }
}
```

---

## 7. ReadableStreamDefaultController

`ReadableStreamDefaultController`는 기본(비-바이트) 스트림의 컨트롤러이다.

### desiredSize (속성)

```js
const stream = new ReadableStream({
  pull(controller) {
    // highWaterMark가 5이고 큐에 3개 청크가 있으면 desiredSize는 2
    console.log('desiredSize:', controller.desiredSize);

    if (controller.desiredSize <= 0) {
      // 큐가 가득 참 - 새 데이터를 넣지 않아도 됨
      return;
    }

    controller.enqueue(getNextChunk());
  }
}, { highWaterMark: 5 });
```

### close()

스트림을 정상적으로 닫는다.

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('데이터 1');
    controller.enqueue('데이터 2');
    controller.close(); // 더 이상 enqueue 불가
  }
});
```

### enqueue(chunk)

청크를 내부 큐에 추가한다. 어떤 타입의 값이든 가능하다.

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('문자열');
    controller.enqueue(42);
    controller.enqueue({ key: 'value' });
    controller.enqueue(new Uint8Array([1, 2, 3]));
    controller.close();
  }
});
```

### error(reason)

스트림을 오류 상태로 전환한다.

```js
const stream = new ReadableStream({
  start(controller) {
    controller.enqueue('정상 데이터');

    // 오류 발생 시
    controller.error(new Error('데이터 소스 장애'));
    // 이후 enqueue(), close() 호출 불가
  }
});

const reader = stream.getReader();
const { value } = await reader.read();    // '정상 데이터'
try {
  await reader.read();                     // Error: 데이터 소스 장애
} catch (e) {
  console.error(e);
}
```

---

## 8. WritableStream

`WritableStream`은 데이터를 쓸 수 있는 싱크(sink)를 나타낸다.

### 생성자

```js
new WritableStream(underlyingSink?, queuingStrategy?)
```

### underlyingSink 객체

| 속성 | 설명 |
|------|------|
| `start(controller)` | 스트림 생성 시 호출. 초기화 로직 |
| `write(chunk, controller)` | 청크가 쓰일 때마다 호출 |
| `close()` | 스트림이 닫힐 때 호출. 마무리 작업 |
| `abort(reason)` | 스트림이 중단될 때 호출. 리소스 정리 |

#### start(controller)

```js
const ws = new WritableStream({
  async start(controller) {
    this.connection = await openDatabaseConnection();
    this.buffer = [];
    console.log('WritableStream 초기화 완료');
  }
});
```

#### write(chunk, controller)

각 청크가 기록될 때 호출된다. Promise를 반환하면 해당 Promise가 이행될 때까지 다음 write가 대기한다. 이것이 WritableStream의 배압 메커니즘이다.

```js
const ws = new WritableStream({
  async write(chunk, controller) {
    // 느린 I/O 작업 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 100));
    console.log('기록됨:', chunk);
  }
});
```

#### close()

모든 쓰기가 완료된 후 스트림을 닫을 때 호출된다.

```js
const ws = new WritableStream({
  start() {
    this.data = [];
  },
  write(chunk) {
    this.data.push(chunk);
  },
  close() {
    // 모아둔 데이터를 한 번에 저장
    localStorage.setItem('stream-data', JSON.stringify(this.data));
    console.log(`${this.data.length}개 청크 저장 완료`);
  }
});
```

#### abort(reason)

스트림이 비정상적으로 중단될 때 호출된다. `close()`와 달리 불완전한 상태에서의 정리를 위한 것이다.

```js
const ws = new WritableStream({
  start() {
    this.tempFile = openTempFile();
  },
  write(chunk) {
    writeTo(this.tempFile, chunk);
  },
  close() {
    renameTempToFinal(this.tempFile);
  },
  abort(reason) {
    // 임시 파일 삭제 등 정리 작업
    deleteTempFile(this.tempFile);
    console.log('중단 사유:', reason);
  }
});
```

### queuingStrategy

```js
const ws = new WritableStream(
  {
    write(chunk) {
      console.log('기록:', chunk);
    }
  },
  new CountQueuingStrategy({ highWaterMark: 5 })
  // 또는
  // { highWaterMark: 1024, size(chunk) { return chunk.byteLength; } }
);
```

### getWriter()

스트림에 데이터를 쓰기 위한 Writer를 획득한다.

```js
const writer = ws.getWriter();
// ws.locked === true

await writer.write('첫 번째 데이터');
await writer.write('두 번째 데이터');
await writer.close();
```

### locked (속성)

Writer에 의해 잠겨 있는지 여부이다.

```js
console.log(ws.locked); // false
const writer = ws.getWriter();
console.log(ws.locked); // true
writer.releaseLock();
console.log(ws.locked); // false
```

### abort(reason?)

스트림을 중단한다.

```js
await ws.abort('사용자가 업로드 취소');
```

### close()

스트림을 정상적으로 닫는다.

```js
await ws.close();
```

### 종합 예제

```js
// 콘솔 로깅 WritableStream
function createLoggingStream(prefix) {
  let count = 0;

  return new WritableStream({
    start() {
      console.log(`[${prefix}] 스트림 시작`);
    },

    write(chunk) {
      count++;
      console.log(`[${prefix}] #${count}: ${chunk}`);
    },

    close() {
      console.log(`[${prefix}] 스트림 종료 (총 ${count}개 청크)`);
    },

    abort(reason) {
      console.error(`[${prefix}] 스트림 중단: ${reason}`);
    }
  });
}

const logStream = createLoggingStream('APP');
const writer = logStream.getWriter();

await writer.write('서버 시작');
await writer.write('요청 수신');
await writer.write('응답 전송');
await writer.close();
```

---

## 9. WritableStreamDefaultWriter

`WritableStreamDefaultWriter`는 `WritableStream`에 데이터를 쓰기 위한 인터페이스이다.

### write(chunk)

스트림에 청크를 쓴다. 청크가 내부 큐에 성공적으로 추가되거나 underlying sink에 기록되면 이행되는 Promise를 반환한다.

```js
const writer = writableStream.getWriter();

await writer.write('Hello');
await writer.write('World');

// 순차적 쓰기 대신 write() Promise를 바로 반환받는 것도 가능
// (배압 관리가 필요 없는 경우)
writer.write('chunk1');
writer.write('chunk2');
writer.write('chunk3');
await writer.close();
```

### close()

스트림을 정상적으로 닫는다. 큐에 있는 모든 청크가 처리된 후 underlying sink의 `close()`가 호출된다.

```js
const writer = writableStream.getWriter();
await writer.write('마지막 데이터');
await writer.close();
// 이후 write() 호출은 오류를 발생시킨다
```

### abort(reason?)

스트림을 즉시 중단한다. 큐에 남은 청크는 버려지고 underlying sink의 `abort()`가 호출된다.

```js
const writer = writableStream.getWriter();
writer.write('데이터 1');
writer.write('데이터 2'); // 이 데이터는 처리되지 않을 수 있다

await writer.abort('긴급 중단');
```

### releaseLock()

Writer의 잠금을 해제한다.

```js
const writer = writableStream.getWriter();
await writer.write('데이터');
writer.releaseLock();

// 새 Writer 획득 가능
const newWriter = writableStream.getWriter();
```

### ready (속성)

배압이 해제되어 쓰기가 가능해지면 이행되는 Promise이다. 효율적인 배압 관리의 핵심이다.

```js
const writer = writableStream.getWriter();

async function writeChunks(chunks) {
  for (const chunk of chunks) {
    // ready가 이행될 때까지 대기 - 배압 존중
    await writer.ready;
    writer.write(chunk); // await 없이 호출하여 성능 최적화
  }
  await writer.ready; // 마지막 쓰기가 큐에 들어갈 때까지 대기
  await writer.close();
}
```

`ready`는 매우 중요한 패턴이다. `write()`를 `await`하지 않고 `ready`만 `await`함으로써, 배압을 존중하면서도 불필요한 대기를 줄일 수 있다.

### closed (속성)

스트림이 닫히거나 오류가 발생하면 이행/거부되는 Promise이다.

```js
const writer = writableStream.getWriter();

writer.closed
  .then(() => console.log('스트림 정상 닫힘'))
  .catch(err => console.error('스트림 오류:', err));
```

### desiredSize (속성)

내부 큐를 `highWaterMark`까지 채우기 위해 필요한 크기이다.

```js
const writer = writableStream.getWriter();

console.log(writer.desiredSize); // highWaterMark (초기 상태)

await writer.write('데이터');
console.log(writer.desiredSize); // 줄어든 값

// desiredSize가 0 이하이면 큐가 가득 찬 것
// writer.ready가 이행될 때까지 기다려야 한다
```

### 종합 예제: 대량 데이터 쓰기

```js
async function writeWithBackpressure(writableStream, dataSource) {
  const writer = writableStream.getWriter();

  try {
    for await (const chunk of dataSource) {
      // 배압을 존중하며 쓰기
      if (writer.desiredSize <= 0) {
        await writer.ready;
      }
      writer.write(chunk);
    }

    // 모든 쓰기가 완료될 때까지 대기
    await writer.ready;
    await writer.close();
    console.log('모든 데이터 기록 완료');
  } catch (error) {
    console.error('쓰기 오류:', error);
    await writer.abort(error.message);
  }
}
```

---

## 10. WritableStreamDefaultController

`WritableStreamDefaultController`는 underlying sink에 전달되는 컨트롤러이다. `ReadableStreamDefaultController`에 비해 단순하다.

### signal (속성)

`AbortSignal` 인스턴스이다. 스트림이 중단(abort)되면 이 signal이 발동된다. 비동기 쓰기 작업을 취소하는 데 사용할 수 있다.

```js
const ws = new WritableStream({
  async write(chunk, controller) {
    // AbortSignal을 fetch에 전달하여
    // 스트림 중단 시 네트워크 요청도 취소되게 한다
    await fetch('/api/data', {
      method: 'POST',
      body: chunk,
      signal: controller.signal
    });
  }
});

// 스트림 중단 시 진행 중인 fetch도 함께 취소된다
const writer = ws.getWriter();
writer.write(someData);
await writer.abort('사용자 취소');
```

```js
const ws = new WritableStream({
  async write(chunk, controller) {
    // signal의 상태를 직접 확인할 수도 있다
    if (controller.signal.aborted) {
      return; // 이미 중단된 상태
    }

    // signal에 이벤트 리스너 등록
    return new Promise((resolve, reject) => {
      controller.signal.addEventListener('abort', () => {
        reject(new Error('쓰기 중단됨'));
      });

      performLongOperation(chunk).then(resolve, reject);
    });
  }
});
```

### error(reason)

스트림을 오류 상태로 전환한다. 이후 모든 쓰기가 거부된다.

```js
const ws = new WritableStream({
  write(chunk, controller) {
    if (!isValidChunk(chunk)) {
      controller.error(new TypeError('잘못된 청크 형식'));
      return;
    }
    processChunk(chunk);
  }
});
```

```js
// 쓰기 횟수 제한 예제
const ws = new WritableStream({
  start() {
    this.writeCount = 0;
    this.maxWrites = 100;
  },

  write(chunk, controller) {
    this.writeCount++;
    if (this.writeCount > this.maxWrites) {
      controller.error(new Error(`최대 쓰기 횟수(${this.maxWrites}) 초과`));
      return;
    }
    saveChunk(chunk);
  }
});
```

---

## 11. TransformStream

`TransformStream`은 데이터를 변환하는 중간 스트림이다. 읽기 가능 면(`readable`)과 쓰기 가능 면(`writable`)을 가진 듀플렉스(duplex) 스트림이다.

### 생성자

```js
new TransformStream(transformer?, writableStrategy?, readableStrategy?)
```

### transformer 객체

| 속성 | 설명 |
|------|------|
| `start(controller)` | 초기화 시 호출 |
| `transform(chunk, controller)` | 각 청크 변환 |
| `flush(controller)` | 쓰기 면이 닫힐 때 호출. 잔여 데이터 처리 |
| `cancel(reason)` | 읽기 면이 취소될 때 호출 |

#### start(controller)

```js
const ts = new TransformStream({
  start(controller) {
    // 초기 상태 설정
    this.count = 0;
    this.buffer = '';
  }
});
```

#### transform(chunk, controller)

입력 청크를 변환하여 출력 큐에 넣는 핵심 메서드이다. Promise를 반환할 수 있다.

```js
// 문자열을 대문자로 변환
const upperCaseTransform = new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk.toUpperCase());
  }
});

// 하나의 입력 청크에서 여러 출력 청크 생성 가능
const splitTransform = new TransformStream({
  transform(chunk, controller) {
    const lines = chunk.split('\n');
    for (const line of lines) {
      if (line.trim()) {
        controller.enqueue(line.trim());
      }
    }
  }
});

// 필터링: 일부 청크를 건너뛰기 (enqueue하지 않으면 됨)
const filterTransform = new TransformStream({
  transform(chunk, controller) {
    if (chunk.length > 0) {  // 빈 문자열 필터링
      controller.enqueue(chunk);
    }
    // enqueue를 호출하지 않으면 해당 청크는 버려진다
  }
});
```

#### flush(controller)

쓰기 면에 대한 모든 쓰기가 완료되고 스트림이 닫힐 때 호출된다. 버퍼에 남은 데이터를 처리하는 데 사용한다.

```js
// 줄 단위 파서 (불완전한 줄을 버퍼링)
const lineTransform = new TransformStream({
  start() {
    this.buffer = '';
  },

  transform(chunk, controller) {
    this.buffer += chunk;
    const lines = this.buffer.split('\n');
    // 마지막 요소는 불완전한 줄일 수 있으므로 버퍼에 보관
    this.buffer = lines.pop();
    for (const line of lines) {
      controller.enqueue(line);
    }
  },

  flush(controller) {
    // 버퍼에 남은 마지막 줄 처리
    if (this.buffer.length > 0) {
      controller.enqueue(this.buffer);
    }
  }
});
```

#### cancel(reason)

읽기 면이 취소될 때 호출된다. 변환에 사용되는 리소스를 정리할 수 있다.

```js
const ts = new TransformStream({
  start() {
    this.decoder = new SomeDecoder();
  },

  transform(chunk, controller) {
    controller.enqueue(this.decoder.decode(chunk));
  },

  cancel(reason) {
    this.decoder.dispose();
    console.log('변환 취소:', reason);
  }
});
```

### readable (속성)

변환된 데이터를 읽을 수 있는 `ReadableStream`이다.

### writable (속성)

변환할 데이터를 쓸 수 있는 `WritableStream`이다.

```js
const { readable, writable } = new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk * 2);
  }
});

// writable에 쓰면 transform을 거쳐 readable에서 읽힌다
const writer = writable.getWriter();
const reader = readable.getReader();

writer.write(5);
const { value } = await reader.read(); // value === 10
```

### 다양한 TransformStream 예제

```js
// JSON 파서 TransformStream
const jsonParseTransform = new TransformStream({
  transform(chunk, controller) {
    try {
      controller.enqueue(JSON.parse(chunk));
    } catch (e) {
      controller.error(new Error(`JSON 파싱 실패: ${e.message}`));
    }
  }
});

// 지연(throttle) TransformStream
function createThrottleTransform(ms) {
  return new TransformStream({
    async transform(chunk, controller) {
      await new Promise(resolve => setTimeout(resolve, ms));
      controller.enqueue(chunk);
    }
  });
}

// 배치(batch) TransformStream
function createBatchTransform(batchSize) {
  return new TransformStream({
    start() {
      this.batch = [];
    },
    transform(chunk, controller) {
      this.batch.push(chunk);
      if (this.batch.length >= batchSize) {
        controller.enqueue(this.batch);
        this.batch = [];
      }
    },
    flush(controller) {
      if (this.batch.length > 0) {
        controller.enqueue(this.batch);
      }
    }
  });
}

// 사용 예
const batchedStream = inputStream
  .pipeThrough(createBatchTransform(10))
  .pipeThrough(new TransformStream({
    async transform(batch, controller) {
      // 10개씩 모아서 일괄 처리
      const results = await processBatch(batch);
      for (const result of results) {
        controller.enqueue(result);
      }
    }
  }));
```

### identity TransformStream

transformer를 전달하지 않으면 입력을 그대로 통과시키는 identity TransformStream이 생성된다. 이는 ReadableStream과 WritableStream을 연결하는 브릿지로 유용하다.

```js
const { readable, writable } = new TransformStream();
// 입력이 그대로 출력으로 전달됨

// 활용: ReadableStream을 직접 제어 가능한 형태로 변환
const writer = writable.getWriter();
writer.write('manual data 1');
writer.write('manual data 2');
writer.close();

for await (const chunk of readable) {
  console.log(chunk); // 'manual data 1', 'manual data 2'
}
```

---

## 12. TransformStreamDefaultController

`TransformStreamDefaultController`는 transformer 객체의 메서드에 전달되는 컨트롤러이다.

### desiredSize (속성)

읽기 면(readable side)의 내부 큐에 대한 desiredSize이다. 변환 출력의 배압을 판단하는 데 사용할 수 있다.

```js
const ts = new TransformStream({
  transform(chunk, controller) {
    console.log('출력 큐 desiredSize:', controller.desiredSize);

    // desiredSize가 음수이면 출력 큐가 포화 상태
    if (controller.desiredSize < 0) {
      console.warn('출력 큐 포화 - 소비 속도가 느림');
    }

    controller.enqueue(transformData(chunk));
  }
});
```

### enqueue(chunk)

변환된 청크를 읽기 면의 큐에 추가한다.

```js
const ts = new TransformStream({
  transform(chunk, controller) {
    // 하나의 입력에서 여러 출력
    controller.enqueue(`[시작] ${chunk}`);
    controller.enqueue(`[내용] ${chunk.toUpperCase()}`);
    controller.enqueue(`[끝] ${chunk}`);
  }
});
```

### error(reason)

읽기 면과 쓰기 면 모두를 오류 상태로 만든다.

```js
const ts = new TransformStream({
  transform(chunk, controller) {
    if (typeof chunk !== 'string') {
      controller.error(new TypeError('문자열 청크만 허용됩니다'));
      return;
    }
    controller.enqueue(chunk.trim());
  }
});
```

### terminate()

읽기 면을 닫고 쓰기 면을 오류 상태로 만든다. 변환을 조기에 종료해야 할 때 사용한다.

```js
// 특정 조건에서 스트림 조기 종료
const ts = new TransformStream({
  transform(chunk, controller) {
    if (chunk === 'END_OF_DATA') {
      controller.terminate(); // 더 이상의 변환 중단
      return;
    }
    controller.enqueue(processChunk(chunk));
  }
});
```

```js
// 최대 N개 청크만 통과시키는 TransformStream
function createTakeTransform(n) {
  let count = 0;
  return new TransformStream({
    transform(chunk, controller) {
      if (count < n) {
        controller.enqueue(chunk);
        count++;
      }
      if (count >= n) {
        controller.terminate();
      }
    }
  });
}

// 처음 5개 청크만 가져오기
const first5 = inputStream.pipeThrough(createTakeTransform(5));
```

---

## 13. 큐잉 전략 (Queuing Strategies)

큐잉 전략은 내부 큐의 배압 경계를 정의한다. 언제 생산자에게 "잠깐 멈춰라"라고 신호를 보낼지 결정한다.

### CountQueuingStrategy

청크의 개수를 기준으로 큐 크기를 관리한다.

```js
const strategy = new CountQueuingStrategy({ highWaterMark: 10 });
// 큐에 10개 이상의 청크가 쌓이면 배압 발생

const stream = new ReadableStream({
  pull(controller) {
    controller.enqueue(getNextItem());
  }
}, strategy);
```

내부적으로 `size()` 함수는 항상 `1`을 반환한다.

```js
const strategy = new CountQueuingStrategy({ highWaterMark: 10 });
console.log(strategy.size('아무 값')); // 1
console.log(strategy.highWaterMark);   // 10
```

### ByteLengthQueuingStrategy

청크의 바이트 크기를 기준으로 큐 크기를 관리한다. `byteLength` 속성이 있는 청크(ArrayBuffer, TypedArray, DataView 등)에 적합하다.

```js
const strategy = new ByteLengthQueuingStrategy({ highWaterMark: 1024 * 16 }); // 16KB
// 큐에 16KB 이상의 데이터가 쌓이면 배압 발생

const stream = new ReadableStream({
  pull(controller) {
    const chunk = new Uint8Array(1024);
    fillWithData(chunk);
    controller.enqueue(chunk);
  }
}, strategy);
```

내부적으로 `size(chunk)` 함수는 `chunk.byteLength`를 반환한다.

```js
const strategy = new ByteLengthQueuingStrategy({ highWaterMark: 65536 });
console.log(strategy.size(new Uint8Array(1024)));  // 1024
console.log(strategy.size(new ArrayBuffer(4096))); // 4096
```

### 커스텀 큐잉 전략

내장 전략으로 충분하지 않을 때 직접 전략을 정의할 수 있다. `highWaterMark`와 `size()` 함수를 직접 지정하면 된다.

```js
// 문자열 길이 기반 큐잉 전략
const stringLengthStrategy = {
  highWaterMark: 10000, // 총 문자 수 기준 10,000자
  size(chunk) {
    return chunk.length; // 문자열 길이를 크기로 사용
  }
};

const stream = new ReadableStream({
  pull(controller) {
    controller.enqueue(getNextString());
  }
}, stringLengthStrategy);
```

```js
// JSON 객체의 직렬화 크기 기반 큐잉 전략
const jsonSizeStrategy = {
  highWaterMark: 1024 * 1024, // 1MB
  size(chunk) {
    return JSON.stringify(chunk).length;
  }
};

const objectStream = new ReadableStream({
  pull(controller) {
    controller.enqueue({ id: 1, data: 'some data', timestamp: Date.now() });
  }
}, jsonSizeStrategy);
```

```js
// 가중치 기반 큐잉 전략
const priorityStrategy = {
  highWaterMark: 100,
  size(chunk) {
    // 우선순위가 높은 청크는 큰 크기를 부여하여 큐를 빠르게 채움
    switch (chunk.priority) {
      case 'high':   return 10;
      case 'medium': return 5;
      case 'low':    return 1;
      default:       return 1;
    }
  }
};
```

### 기본 큐잉 전략

큐잉 전략을 명시하지 않으면 기본값이 적용된다.

| 스트림 유형 | 기본 highWaterMark | 기본 size() |
|-------------|-------------------|-------------|
| ReadableStream (기본) | 1 | `() => 1` |
| ReadableStream (바이트) | 0 | N/A (바이트 단위 관리) |
| WritableStream | 1 | `() => 1` |
| TransformStream readable | 0 | `() => 1` |
| TransformStream writable | 1 | `() => 1` |

---

## 14. 배압 (Backpressure) 메커니즘

### 배압의 작동 원리

배압은 스트림 시스템에서 자동으로 작동하는 흐름 제어 메커니즘이다. 핵심 구성 요소는 다음과 같다.

```
                    highWaterMark = 3
                         ↓
내부 큐: [ ][ ][ ][ ][ ][ ]
          ↑              ↑
        비어있음      가득 참

desiredSize = highWaterMark - queueSize
```

| desiredSize 값 | 의미 | 동작 |
|----------------|------|------|
| 양수 | 큐에 여유 있음 | 생산자에게 pull() 호출 |
| 0 | 큐가 정확히 가득 참 | pull() 호출 중단 |
| 음수 | 큐 오버플로우 | pull() 호출 중단 + 추가 배압 |
| null | 스트림 닫힘/오류 | 더 이상 pull() 없음 |

### desiredSize를 이용한 배압

```js
const readable = new ReadableStream({
  pull(controller) {
    console.log(`desiredSize: ${controller.desiredSize}`);

    // desiredSize가 양수인 동안만 데이터를 생성
    if (controller.desiredSize > 0) {
      const data = generateData();
      controller.enqueue(data);
    }
  }
}, { highWaterMark: 5 });
```

### 파이프 체인에서의 배압 전파

배압은 파이프 체인을 따라 하류(downstream)에서 상류(upstream)로 전파된다.

```
ReadableStream → TransformStream → WritableStream
    ↑ pull()         ↑                 ↑ (느린 I/O)
    |                |                 |
    ← 배압 전파 ←── 배압 전파 ←──── 배압 시작점
```

```js
// 배압 전파 시연
const source = new ReadableStream({
  pull(controller) {
    const timestamp = Date.now();
    console.log(`[소스] 데이터 생성 at ${timestamp}`);
    controller.enqueue({ data: 'chunk', timestamp });
  }
}, { highWaterMark: 2 });

const transform = new TransformStream({
  transform(chunk, controller) {
    console.log('[변환] 청크 변환');
    controller.enqueue({ ...chunk, transformed: true });
  }
});

const sink = new WritableStream({
  async write(chunk) {
    console.log('[싱크] 청크 기록 시작');
    // 느린 쓰기 시뮬레이션 - 이것이 배압의 원인
    await new Promise(resolve => setTimeout(resolve, 1000));
    console.log('[싱크] 청크 기록 완료');
  }
}, { highWaterMark: 1 });

// 파이프 연결 - 배압이 자동으로 전파됨
await source.pipeThrough(transform).pipeTo(sink);
```

### WritableStream에서의 배압

`WritableStreamDefaultWriter`의 `ready` Promise와 `desiredSize`를 통해 배압을 확인할 수 있다.

```js
const ws = new WritableStream({
  async write(chunk) {
    await slowOperation(chunk);
  }
}, { highWaterMark: 3 });

const writer = ws.getWriter();

// 배압을 존중하는 쓰기 패턴
async function writeWithBackpressure(chunks) {
  for (const chunk of chunks) {
    // ready가 이행될 때까지 대기하여 배압 존중
    await writer.ready;
    console.log(`desiredSize: ${writer.desiredSize}`);
    writer.write(chunk); // await 없이 호출
  }
  await writer.ready;
  await writer.close();
}
```

### 수동 배압 관리

일부 시나리오에서는 배압을 수동으로 관리해야 할 수 있다.

```js
// 외부 이벤트 소스를 ReadableStream으로 감쌀 때의 배압 관리
function fromEventSource(eventSource) {
  let paused = false;
  const pendingChunks = [];

  return new ReadableStream({
    start(controller) {
      eventSource.onmessage = (event) => {
        if (controller.desiredSize <= 0) {
          // 배압이 걸림 - 이벤트 소스 일시 정지가 불가능하면 버퍼링
          paused = true;
          pendingChunks.push(event.data);
          console.warn('배압 발생 - 버퍼링 중');
          return;
        }
        controller.enqueue(event.data);
      };

      eventSource.onerror = (error) => {
        controller.error(error);
      };
    },

    pull(controller) {
      // 배압 해제 - 버퍼링된 데이터 전달
      while (pendingChunks.length > 0 && controller.desiredSize > 0) {
        controller.enqueue(pendingChunks.shift());
      }
      if (pendingChunks.length === 0) {
        paused = false;
      }
    },

    cancel() {
      eventSource.close();
    }
  });
}
```

---

## 15. 파이프 (Piping)

### pipeTo()

`ReadableStream`의 데이터를 `WritableStream`으로 전송한다. 모든 데이터 전송이 완료되면 이행되는 Promise를 반환한다.

```js
const readable = new ReadableStream({
  start(controller) {
    controller.enqueue('Hello');
    controller.enqueue('World');
    controller.close();
  }
});

const writable = new WritableStream({
  write(chunk) {
    console.log('받음:', chunk);
  }
});

await readable.pipeTo(writable);
// 출력:
// 받음: Hello
// 받음: World
```

#### pipeTo 옵션

```js
const abortController = new AbortController();

try {
  await readable.pipeTo(writable, {
    // 소스 스트림이 닫힐 때 싱크도 닫을지 여부
    preventClose: false,

    // 소스 스트림에서 오류 발생 시 싱크를 abort할지 여부
    preventAbort: false,

    // 싱크에서 오류 발생 시 소스를 cancel할지 여부
    preventCancel: false,

    // AbortSignal - 외부에서 파이프를 취소할 수 있음
    signal: abortController.signal
  });
} catch (e) {
  if (e.name === 'AbortError') {
    console.log('파이프가 외부에서 취소됨');
  }
}

// 5초 후 파이프 취소
setTimeout(() => abortController.abort(), 5000);
```

#### preventClose, preventAbort, preventCancel 상세

```js
// preventClose: true - 소스가 닫혀도 싱크를 닫지 않음
// 여러 소스를 순차적으로 하나의 싱크에 파이프할 때 유용
const writable = new WritableStream({
  write(chunk) { console.log(chunk); },
  close() { console.log('싱크 닫힘'); }
});

const source1 = ReadableStream.from(['a', 'b']);
const source2 = ReadableStream.from(['c', 'd']);

await source1.pipeTo(writable, { preventClose: true });
// 싱크는 아직 열려 있음
await source2.pipeTo(writable);
// 이제 싱크가 닫힘
```

### pipeThrough()

`ReadableStream`의 데이터를 `TransformStream`에 통과시키고, 변환된 결과를 새 `ReadableStream`으로 반환한다.

```js
const readable = new ReadableStream({
  start(controller) {
    controller.enqueue('hello world');
    controller.enqueue('foo bar');
    controller.close();
  }
});

const result = readable.pipeThrough(new TransformStream({
  transform(chunk, controller) {
    controller.enqueue(chunk.toUpperCase());
  }
}));

// result는 ReadableStream
const reader = result.getReader();
console.log(await reader.read()); // { value: 'HELLO WORLD', done: false }
console.log(await reader.read()); // { value: 'FOO BAR', done: false }
console.log(await reader.read()); // { value: undefined, done: true }
```

### 파이프 체인 구성

여러 `pipeThrough()`를 연쇄하여 복잡한 변환 파이프라인을 구성할 수 있다.

```js
// 다단계 파이프라인: 바이트 → 텍스트 → 줄 분리 → 필터링 → JSON 파싱
const processedStream = fetchResponse.body
  .pipeThrough(new TextDecoderStream())          // Uint8Array → 문자열
  .pipeThrough(createLineSplitTransform())        // 문자열 → 개별 줄
  .pipeThrough(createFilterTransform(line =>      // 빈 줄 필터링
    line.trim().length > 0
  ))
  .pipeThrough(createJsonParseTransform());       // 문자열 → JSON 객체

// 결과 소비
for await (const obj of processedStream) {
  console.log('파싱된 객체:', obj);
}
```

```js
// 유틸리티 함수들
function createLineSplitTransform() {
  let buffer = '';
  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 마지막 불완전한 줄은 버퍼에 유지
      for (const line of lines) {
        controller.enqueue(line);
      }
    },
    flush(controller) {
      if (buffer) controller.enqueue(buffer);
    }
  });
}

function createFilterTransform(predicate) {
  return new TransformStream({
    transform(chunk, controller) {
      if (predicate(chunk)) {
        controller.enqueue(chunk);
      }
    }
  });
}

function createJsonParseTransform() {
  return new TransformStream({
    transform(chunk, controller) {
      try {
        controller.enqueue(JSON.parse(chunk));
      } catch {
        // 파싱 실패한 줄은 무시
      }
    }
  });
}
```

### 내장 TransformStream

브라우저는 몇 가지 내장 TransformStream을 제공한다.

```js
// TextDecoderStream: 바이트 → 문자열
const textStream = byteStream.pipeThrough(new TextDecoderStream('utf-8'));

// TextEncoderStream: 문자열 → 바이트
const byteStream2 = textStream.pipeThrough(new TextEncoderStream());

// CompressionStream: 압축
const compressed = readableStream.pipeThrough(new CompressionStream('gzip'));

// DecompressionStream: 압축 해제
const decompressed = compressed.pipeThrough(new DecompressionStream('gzip'));
```

---

## 16. Tee (분기)

### ReadableStream.tee()

`tee()`는 하나의 `ReadableStream`을 두 개의 동일한 스트림으로 복제한다. 같은 데이터를 두 가지 다른 방식으로 처리해야 할 때 유용하다.

```js
const [stream1, stream2] = originalStream.tee();
```

이름의 유래: 배관에서 사용하는 T자형 연결관(tee fitting)에서 온 이름이다. 하나의 파이프가 두 갈래로 나뉘는 것과 같다.

### 기본 사용

```js
const original = new ReadableStream({
  start(controller) {
    controller.enqueue(1);
    controller.enqueue(2);
    controller.enqueue(3);
    controller.close();
  }
});

const [branch1, branch2] = original.tee();

// 두 브랜치에서 동일한 데이터를 독립적으로 읽을 수 있다
const reader1 = branch1.getReader();
const reader2 = branch2.getReader();

console.log(await reader1.read()); // { value: 1, done: false }
console.log(await reader2.read()); // { value: 1, done: false }
console.log(await reader1.read()); // { value: 2, done: false }
// reader2는 아직 두 번째 청크를 읽지 않았지만 독립적으로 유지됨
```

### 실용적 tee 사용 사례

#### 캐시와 처리를 동시에

```js
async function fetchAndCacheAndProcess(url) {
  const response = await fetch(url);
  const [forCache, forProcessing] = response.body.tee();

  // 브랜치 1: 캐시에 저장
  const cachePromise = caches.open('my-cache').then(cache => {
    const cachedResponse = new Response(forCache, {
      headers: response.headers
    });
    return cache.put(url, cachedResponse);
  });

  // 브랜치 2: 즉시 처리
  const processPromise = processStream(forProcessing);

  await Promise.all([cachePromise, processPromise]);
}
```

#### 로깅과 실제 처리 동시 수행

```js
const [logBranch, processBranch] = dataStream.tee();

// 브랜치 1: 로깅
logBranch.pipeTo(new WritableStream({
  write(chunk) {
    console.log(`[LOG] 청크 크기: ${JSON.stringify(chunk).length}`);
  }
}));

// 브랜치 2: 실제 비즈니스 로직
processBranch
  .pipeThrough(transformStream)
  .pipeTo(outputStream);
```

#### 여러 갈래로 분기

`tee()`는 두 갈래만 지원하지만, 재귀적으로 사용하면 여러 갈래로 분기할 수 있다.

```js
function teeN(stream, n) {
  if (n <= 1) return [stream];
  if (n === 2) return stream.tee();

  const [first, rest] = stream.tee();
  return [first, ...teeN(rest, n - 1)];
}

// 4갈래 분기
const [s1, s2, s3, s4] = teeN(originalStream, 4);
```

### tee의 배압 특성

`tee()`로 생성된 두 브랜치는 독립적인 배압을 가진다. 중요한 점은, 한 브랜치의 소비가 느리면 원본 스트림 전체가 느려진다는 것이다. 내부적으로 두 브랜치 모두에 데이터가 전달되어야 하므로, 가장 느린 브랜치가 전체 속도를 결정한다.

```js
const [fast, slow] = dataStream.tee();

// fast 브랜치는 빠르게 소비하지만
fast.pipeTo(fastSink);

// slow 브랜치가 느리면 전체가 느려진다
slow.pipeTo(new WritableStream({
  async write(chunk) {
    await new Promise(r => setTimeout(r, 1000)); // 1초 딜레이
  }
}));
// fast 브랜치도 slow 브랜치의 속도에 영향을 받는다
```

---

## 17. 바이트 스트림 (Byte Streams)

바이트 스트림은 바이너리 데이터를 효율적으로 처리하기 위한 특수한 `ReadableStream`이다. `type: 'bytes'`를 지정하여 생성한다.

### Underlying Byte Source

```js
const byteStream = new ReadableStream({
  type: 'bytes',

  start(controller) {
    // controller는 ReadableByteStreamController
    console.log('바이트 스트림 시작');
  },

  async pull(controller) {
    // BYOB 리더의 요청이 있으면 byobRequest를 통해 처리
    if (controller.byobRequest) {
      const view = controller.byobRequest.view;
      const bytesRead = await readFromSource(view.buffer, view.byteOffset, view.byteLength);

      if (bytesRead === 0) {
        controller.close();
        controller.byobRequest.respond(0);
      } else {
        controller.byobRequest.respond(bytesRead);
      }
    } else {
      // 기본 리더 사용 시 - 새 버퍼를 만들어 enqueue
      const buffer = new Uint8Array(1024);
      const bytesRead = await readFromSource(buffer.buffer, 0, 1024);

      if (bytesRead === 0) {
        controller.close();
      } else {
        controller.enqueue(new Uint8Array(buffer.buffer, 0, bytesRead));
      }
    }
  },

  cancel() {
    closeSource();
  }
});
```

### BYOB Reader 상세

BYOB(Bring Your Own Buffer) 리더는 소비자가 직접 버퍼를 제공하여 메모리 할당을 최소화하는 패턴이다.

```js
async function readFileWithBYOB(byteStream) {
  const reader = byteStream.getReader({ mode: 'byob' });
  const chunks = [];
  let totalBytes = 0;

  try {
    while (true) {
      // 매 읽기마다 새 버퍼를 할당하지 않고 재사용
      const buffer = new ArrayBuffer(64 * 1024); // 64KB 버퍼
      let offset = 0;

      // 버퍼가 가득 찰 때까지 반복 읽기
      while (offset < buffer.byteLength) {
        const view = new Uint8Array(buffer, offset, buffer.byteLength - offset);
        const { value, done } = await reader.read(view);

        if (done) {
          if (offset > 0) {
            chunks.push(new Uint8Array(buffer, 0, offset));
            totalBytes += offset;
          }
          console.log(`총 ${totalBytes} 바이트 읽음`);
          return concatenateChunks(chunks, totalBytes);
        }

        offset += value.byteLength;
        // 주의: value는 새 뷰이므로 value.buffer를 사용해야 한다
      }

      chunks.push(new Uint8Array(buffer));
      totalBytes += buffer.byteLength;
    }
  } finally {
    reader.releaseLock();
  }
}

function concatenateChunks(chunks, totalLength) {
  const result = new Uint8Array(totalLength);
  let offset = 0;
  for (const chunk of chunks) {
    result.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return result;
}
```

### autoAllocateChunkSize

`autoAllocateChunkSize`를 설정하면, 기본(default) 리더를 사용하더라도 내부적으로 BYOB 메커니즘을 활용한다. 자동으로 지정된 크기의 버퍼가 할당되어 `byobRequest`에 연결된다.

```js
const stream = new ReadableStream({
  type: 'bytes',
  autoAllocateChunkSize: 1024, // 1KB 자동 할당

  async pull(controller) {
    // autoAllocateChunkSize 덕분에 기본 리더를 사용해도
    // controller.byobRequest가 null이 아니다
    const view = controller.byobRequest.view;
    const bytesRead = await readFromDevice(
      view.buffer, view.byteOffset, view.byteLength
    );

    if (bytesRead === 0) {
      controller.close();
      controller.byobRequest.respond(0);
    } else {
      controller.byobRequest.respond(bytesRead);
    }
  }
});

// 기본 리더로도 사용 가능 (내부적으로 BYOB 메커니즘 활용)
const reader = stream.getReader();
const { value } = await reader.read(); // Uint8Array
```

### 바이트 스트림 vs 기본 스트림 비교

| 특성 | 기본 스트림 | 바이트 스트림 |
|------|------------|--------------|
| 청크 타입 | 아무 JS 값 | ArrayBufferView만 |
| 컨트롤러 | ReadableStreamDefaultController | ReadableByteStreamController |
| BYOB 리더 | 사용 불가 | 사용 가능 |
| byobRequest | 없음 | 있음 |
| autoAllocateChunkSize | 없음 | 사용 가능 |
| 주요 용도 | 범용 데이터 | 바이너리 데이터 |
| 메모리 효율 | 보통 | 높음 (버퍼 재사용) |

### 파일 읽기 바이트 스트림 예제

```js
// Blob을 바이트 스트림으로 읽기
function blobToByteStream(blob, chunkSize = 64 * 1024) {
  let offset = 0;

  return new ReadableStream({
    type: 'bytes',

    async pull(controller) {
      if (offset >= blob.size) {
        controller.close();
        return;
      }

      const end = Math.min(offset + chunkSize, blob.size);
      const slice = blob.slice(offset, end);
      const buffer = await slice.arrayBuffer();

      controller.enqueue(new Uint8Array(buffer));
      offset = end;
    }
  });
}

// 사용
const file = new File(['Hello, World!'], 'test.txt');
const stream = blobToByteStream(file, 5);

const reader = stream.getReader();
while (true) {
  const { value, done } = await reader.read();
  if (done) break;
  console.log(new TextDecoder().decode(value));
}
```

---

## 18. 실용 예제

### 18.1 Fetch와 함께 사용

#### 스트리밍 응답 읽기

```js
async function fetchWithStreaming(url) {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  // response.body는 ReadableStream<Uint8Array>
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let result = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    result += decoder.decode(value, { stream: true });
    console.log('수신 중...', result.length, '바이트');
  }

  // 디코더 플러시
  result += decoder.decode();
  return result;
}
```

#### Server-Sent Events (SSE) 스트림 파싱

```js
async function* parseSSE(url) {
  const response = await fetch(url);
  const reader = response.body
    .pipeThrough(new TextDecoderStream())
    .getReader();

  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += value;
    const events = buffer.split('\n\n');
    buffer = events.pop(); // 마지막 불완전한 이벤트는 버퍼에 유지

    for (const event of events) {
      if (!event.trim()) continue;

      const lines = event.split('\n');
      const parsed = {};

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          parsed.data = (parsed.data || '') + line.slice(6);
        } else if (line.startsWith('event: ')) {
          parsed.event = line.slice(7);
        } else if (line.startsWith('id: ')) {
          parsed.id = line.slice(4);
        }
      }

      yield parsed;
    }
  }
}

// 사용
for await (const event of parseSSE('/api/events')) {
  console.log('이벤트:', event.event, '데이터:', event.data);
}
```

#### 스트리밍 POST 요청

```js
// 대용량 데이터를 스트리밍으로 업로드
async function streamUpload(url, dataSource) {
  const readable = new ReadableStream({
    async start(controller) {
      for await (const chunk of dataSource) {
        controller.enqueue(new TextEncoder().encode(
          JSON.stringify(chunk) + '\n'
        ));
      }
      controller.close();
    }
  });

  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-ndjson' },
    body: readable, // ReadableStream을 직접 body로 전달
    duplex: 'half'  // 스트리밍 업로드에 필요
  });

  return response.json();
}
```

### 18.2 파일 처리

#### 대용량 파일 해시 계산

```js
async function hashFile(file) {
  const stream = file.stream(); // File → ReadableStream<Uint8Array>
  const reader = stream.getReader();

  // SubtleCrypto는 스트리밍을 직접 지원하지 않으므로
  // 전체를 읽어야 하지만, 진행률 추적은 가능
  const chunks = [];
  let loaded = 0;

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    chunks.push(value);
    loaded += value.byteLength;
    console.log(`진행: ${((loaded / file.size) * 100).toFixed(1)}%`);
  }

  // 청크 병합 및 해시 계산
  const combined = new Uint8Array(loaded);
  let offset = 0;
  for (const chunk of chunks) {
    combined.set(chunk, offset);
    offset += chunk.byteLength;
  }

  const hashBuffer = await crypto.subtle.digest('SHA-256', combined);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}
```

#### CSV 파서 스트림

```js
function createCSVParserTransform(options = {}) {
  const { delimiter = ',', hasHeader = true } = options;
  let headers = null;
  let isFirstLine = true;
  let buffer = '';

  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 불완전한 마지막 줄 보관

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed) continue;

        const fields = parseCSVLine(trimmed, delimiter);

        if (isFirstLine && hasHeader) {
          headers = fields;
          isFirstLine = false;
          continue;
        }

        if (headers) {
          const obj = {};
          headers.forEach((h, i) => { obj[h] = fields[i]; });
          controller.enqueue(obj);
        } else {
          controller.enqueue(fields);
        }

        isFirstLine = false;
      }
    },

    flush(controller) {
      if (buffer.trim()) {
        const fields = parseCSVLine(buffer.trim(), delimiter);
        if (headers) {
          const obj = {};
          headers.forEach((h, i) => { obj[h] = fields[i]; });
          controller.enqueue(obj);
        } else {
          controller.enqueue(fields);
        }
      }
    }
  });
}

function parseCSVLine(line, delimiter) {
  const fields = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === delimiter && !inQuotes) {
      fields.push(current);
      current = '';
    } else {
      current += char;
    }
  }
  fields.push(current);
  return fields;
}

// 사용
const response = await fetch('/data/large-file.csv');
const objects = response.body
  .pipeThrough(new TextDecoderStream())
  .pipeThrough(createCSVParserTransform({ delimiter: ',', hasHeader: true }));

for await (const row of objects) {
  console.log(row); // { name: '홍길동', age: '30', city: '서울' }
}
```

### 18.3 데이터 변환 파이프라인

#### 다단계 텍스트 처리 파이프라인

```js
// 1단계: 줄 분리
function lineSplitter() {
  let buffer = '';
  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop();
      lines.forEach(line => controller.enqueue(line));
    },
    flush(controller) {
      if (buffer) controller.enqueue(buffer);
    }
  });
}

// 2단계: 빈 줄 필터링
function emptyLineFilter() {
  return new TransformStream({
    transform(chunk, controller) {
      if (chunk.trim().length > 0) {
        controller.enqueue(chunk);
      }
    }
  });
}

// 3단계: 번호 매기기
function lineNumberer() {
  let lineNum = 0;
  return new TransformStream({
    transform(chunk, controller) {
      lineNum++;
      controller.enqueue(`${lineNum}: ${chunk}`);
    }
  });
}

// 4단계: 접두사 추가
function prefixer(prefix) {
  return new TransformStream({
    transform(chunk, controller) {
      controller.enqueue(`${prefix} ${chunk}`);
    }
  });
}

// 파이프라인 조합
async function processTextFile(url) {
  const response = await fetch(url);

  const processed = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(lineSplitter())
    .pipeThrough(emptyLineFilter())
    .pipeThrough(lineNumberer())
    .pipeThrough(prefixer('[LOG]'));

  const results = [];
  for await (const line of processed) {
    results.push(line);
  }
  return results;
}
```

#### JSON Lines (NDJSON) 처리 파이프라인

```js
function createNDJSONTransform() {
  let buffer = '';

  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;
      const lines = buffer.split('\n');
      buffer = lines.pop();

      for (const line of lines) {
        if (line.trim()) {
          try {
            controller.enqueue(JSON.parse(line));
          } catch (e) {
            console.warn('JSON 파싱 실패:', line);
          }
        }
      }
    },

    flush(controller) {
      if (buffer.trim()) {
        try {
          controller.enqueue(JSON.parse(buffer));
        } catch (e) {
          console.warn('마지막 줄 JSON 파싱 실패:', buffer);
        }
      }
    }
  });
}

// 집계 TransformStream
function createAggregator(keyFn, valueFn) {
  const groups = new Map();

  return new TransformStream({
    transform(chunk) {
      const key = keyFn(chunk);
      const value = valueFn(chunk);
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key).push(value);
    },

    flush(controller) {
      for (const [key, values] of groups) {
        controller.enqueue({
          key,
          count: values.length,
          sum: values.reduce((a, b) => a + b, 0),
          avg: values.reduce((a, b) => a + b, 0) / values.length
        });
      }
    }
  });
}

// NDJSON 데이터 집계 파이프라인
async function aggregateData(url) {
  const response = await fetch(url);

  const results = [];
  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(createNDJSONTransform())
    .pipeThrough(createAggregator(
      item => item.category,     // 카테고리별 그룹화
      item => item.amount         // 금액 집계
    ));

  for await (const result of pipeline) {
    results.push(result);
    console.log(`카테고리: ${result.key}, 건수: ${result.count}, 합계: ${result.sum}, 평균: ${result.avg.toFixed(2)}`);
  }

  return results;
}
```

### 18.4 진행률 표시

#### 다운로드 진행률

```js
function createProgressStream(totalSize, onProgress) {
  let loaded = 0;
  let startTime = Date.now();

  return new TransformStream({
    transform(chunk, controller) {
      loaded += chunk.byteLength;
      const elapsed = (Date.now() - startTime) / 1000;
      const speed = loaded / elapsed; // 바이트/초

      onProgress({
        loaded,
        total: totalSize,
        percentage: totalSize > 0
          ? ((loaded / totalSize) * 100).toFixed(1)
          : 'unknown',
        speed: formatBytes(speed) + '/s',
        elapsed: elapsed.toFixed(1) + 's',
        remaining: totalSize > 0
          ? ((totalSize - loaded) / speed).toFixed(1) + 's'
          : 'unknown'
      });

      controller.enqueue(chunk);
    }
  });
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
}

// 사용 예
async function downloadWithProgress(url) {
  const response = await fetch(url);
  const totalSize = Number(response.headers.get('Content-Length')) || 0;

  const progressStream = createProgressStream(totalSize, (info) => {
    console.log(`다운로드: ${info.percentage}% | ${info.speed} | 남은 시간: ${info.remaining}`);
    // UI 업데이트
    // progressBar.style.width = info.percentage + '%';
    // progressText.textContent = `${info.percentage}% (${info.speed})`;
  });

  const data = await new Response(
    response.body.pipeThrough(progressStream)
  ).arrayBuffer();

  console.log(`다운로드 완료: ${formatBytes(data.byteLength)}`);
  return data;
}
```

#### 업로드 진행률

```js
function createUploadProgressStream(totalSize, onProgress) {
  let uploaded = 0;

  return new TransformStream({
    transform(chunk, controller) {
      uploaded += chunk.byteLength;
      onProgress({
        uploaded,
        total: totalSize,
        percentage: ((uploaded / totalSize) * 100).toFixed(1)
      });
      controller.enqueue(chunk);
    }
  });
}

async function uploadFileWithProgress(url, file) {
  const totalSize = file.size;

  const progressStream = createUploadProgressStream(totalSize, (info) => {
    console.log(`업로드: ${info.percentage}%`);
  });

  const body = file.stream().pipeThrough(progressStream);

  const response = await fetch(url, {
    method: 'POST',
    body,
    duplex: 'half',
    headers: {
      'Content-Type': file.type,
      'Content-Length': totalSize.toString()
    }
  });

  return response.json();
}
```

### 18.5 청크 단위 텍스트 처리

#### 실시간 검색/하이라이팅

```js
function createSearchHighlightTransform(searchTerm) {
  return new TransformStream({
    transform(line, controller) {
      if (line.toLowerCase().includes(searchTerm.toLowerCase())) {
        const highlighted = line.replace(
          new RegExp(`(${escapeRegex(searchTerm)})`, 'gi'),
          '$1'
        );
        controller.enqueue({ line: highlighted, matched: true });
      } else {
        controller.enqueue({ line, matched: false });
      }
    }
  });
}

function escapeRegex(string) {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// 대용량 로그 파일에서 실시간 검색
async function searchInLogFile(url, searchTerm) {
  const response = await fetch(url);
  const matches = [];

  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(lineSplitter())
    .pipeThrough(createSearchHighlightTransform(searchTerm));

  for await (const result of pipeline) {
    if (result.matched) {
      matches.push(result.line);
    }
  }

  return matches;
}
```

#### 스트리밍 단어 카운터

```js
function createWordCountTransform() {
  const wordCounts = new Map();

  return new TransformStream({
    transform(chunk) {
      const words = chunk
        .toLowerCase()
        .replace(/[^\w\s가-힣]/g, '')
        .split(/\s+/)
        .filter(Boolean);

      for (const word of words) {
        wordCounts.set(word, (wordCounts.get(word) || 0) + 1);
      }
      // 중간 결과는 enqueue하지 않음 (집계만 수행)
    },

    flush(controller) {
      // 최종 결과를 정렬하여 출력
      const sorted = [...wordCounts.entries()]
        .sort((a, b) => b[1] - a[1]);

      for (const [word, count] of sorted) {
        controller.enqueue({ word, count });
      }
    }
  });
}

// 사용
async function countWords(url) {
  const response = await fetch(url);

  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(createWordCountTransform());

  const topWords = [];
  let rank = 0;

  for await (const { word, count } of pipeline) {
    rank++;
    if (rank <= 20) { // 상위 20개만
      topWords.push({ rank, word, count });
      console.log(`${rank}. "${word}": ${count}회`);
    }
  }

  return topWords;
}
```

#### 스트리밍 마크다운 → HTML 변환 (간이 버전)

```js
function createMarkdownToHtmlTransform() {
  return new TransformStream({
    transform(line, controller) {
      let html = line;

      // 헤더 변환
      if (html.startsWith('### ')) {
        html = `<h3>${html.slice(4)}</h3>`;
      } else if (html.startsWith('## ')) {
        html = `<h2>${html.slice(3)}</h2>`;
      } else if (html.startsWith('# ')) {
        html = `<h1>${html.slice(2)}</h1>`;
      }
      // 굵게
      else {
        html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
        html = html.replace(/`(.+?)`/g, '<code>$1</code>');
        html = `<p>${html}</p>`;
      }

      controller.enqueue(html);
    }
  });
}

// 마크다운 파일을 스트리밍으로 HTML 변환
async function convertMarkdown(url) {
  const response = await fetch(url);

  const htmlParts = [];
  const pipeline = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(lineSplitter())
    .pipeThrough(createMarkdownToHtmlTransform());

  for await (const html of pipeline) {
    htmlParts.push(html);
  }

  return htmlParts.join('\n');
}
```

### 18.6 압축/해제

```js
// Gzip 압축 후 업로드
async function compressAndUpload(url, data) {
  const textEncoder = new TextEncoderStream();
  const compressionStream = new CompressionStream('gzip');

  const readable = new ReadableStream({
    start(controller) {
      controller.enqueue(data);
      controller.close();
    }
  });

  const compressedStream = readable
    .pipeThrough(textEncoder)
    .pipeThrough(compressionStream);

  const response = await fetch(url, {
    method: 'POST',
    body: compressedStream,
    duplex: 'half',
    headers: {
      'Content-Encoding': 'gzip',
      'Content-Type': 'application/json'
    }
  });

  return response;
}

// 압축된 응답 스트리밍 해제
async function fetchAndDecompress(url) {
  const response = await fetch(url);

  const decompressed = response.body
    .pipeThrough(new DecompressionStream('gzip'))
    .pipeThrough(new TextDecoderStream());

  let result = '';
  for await (const chunk of decompressed) {
    result += chunk;
  }

  return result;
}
```

### 18.7 ReadableStream을 활용한 무한 데이터 소스

```js
// 피보나치 수열 무한 스트림
function fibonacciStream() {
  let a = 0n, b = 1n;

  return new ReadableStream({
    pull(controller) {
      controller.enqueue(a);
      [a, b] = [b, a + b];
    }
  });
}

// 처음 50개 피보나치 수 가져오기
async function getFirstNFibonacci(n) {
  const stream = fibonacciStream();
  const reader = stream.getReader();
  const results = [];

  for (let i = 0; i < n; i++) {
    const { value } = await reader.read();
    results.push(value);
  }

  await reader.cancel(); // 무한 스트림이므로 명시적 취소 필요
  return results;
}

// 타이머 기반 이벤트 스트림
function timerStream(intervalMs) {
  let id;
  let count = 0;

  return new ReadableStream({
    start(controller) {
      id = setInterval(() => {
        count++;
        controller.enqueue({
          tick: count,
          timestamp: Date.now()
        });
      }, intervalMs);
    },

    cancel() {
      clearInterval(id);
    }
  });
}

// 3초 동안의 100ms 타이머 이벤트 수집
async function collectTimerEvents() {
  const stream = timerStream(100);
  const reader = stream.getReader();
  const events = [];

  const timeout = setTimeout(() => reader.cancel('시간 초과'), 3000);

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      events.push(value);
    }
  } catch {
    // cancel로 인한 오류 무시
  }

  clearTimeout(timeout);
  return events;
}
```

### 18.8 웹소켓과 스트림 결합

```js
function webSocketToStreams(url) {
  const ws = new WebSocket(url);

  const readable = new ReadableStream({
    start(controller) {
      ws.onmessage = (event) => {
        controller.enqueue(event.data);
      };

      ws.onerror = (error) => {
        controller.error(error);
      };

      ws.onclose = () => {
        controller.close();
      };
    },

    cancel() {
      ws.close();
    }
  });

  const writable = new WritableStream({
    write(chunk) {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(chunk);
      } else {
        throw new Error('WebSocket이 열려 있지 않습니다');
      }
    },

    close() {
      ws.close();
    },

    abort() {
      ws.close();
    }
  });

  return { readable, writable };
}

// 사용
const { readable, writable } = webSocketToStreams('wss://example.com/ws');

// 수신 데이터를 JSON으로 파싱하여 처리
readable
  .pipeThrough(new TransformStream({
    transform(chunk, controller) {
      try {
        controller.enqueue(JSON.parse(chunk));
      } catch {
        console.warn('비-JSON 메시지:', chunk);
      }
    }
  }))
  .pipeTo(new WritableStream({
    write(message) {
      handleMessage(message);
    }
  }));

// 데이터 전송
const writer = writable.getWriter();
await writer.write(JSON.stringify({ type: 'subscribe', channel: 'updates' }));
```

### 18.9 여러 스트림 병합(Merge)

```js
// 여러 ReadableStream을 하나로 병합
function mergeStreams(...streams) {
  let activeReaders = streams.map(s => s.getReader());

  return new ReadableStream({
    async pull(controller) {
      if (activeReaders.length === 0) {
        controller.close();
        return;
      }

      // 모든 활성 리더에서 동시에 읽기 시도
      const readPromises = activeReaders.map((reader, index) =>
        reader.read().then(result => ({ result, index }))
      );

      const { result, index } = await Promise.race(readPromises);

      if (result.done) {
        // 완료된 리더 제거
        activeReaders.splice(index, 1);
        if (activeReaders.length === 0) {
          controller.close();
        }
      } else {
        controller.enqueue(result.value);
      }
    },

    cancel() {
      activeReaders.forEach(reader => reader.cancel());
      activeReaders = [];
    }
  });
}

// 사용: 여러 API 엔드포인트의 데이터를 하나의 스트림으로
const stream1 = (await fetch('/api/source1')).body;
const stream2 = (await fetch('/api/source2')).body;
const merged = mergeStreams(stream1, stream2);
```

### 18.10 스트림 기반 간이 ETL 파이프라인

```js
// Extract(추출) - Transform(변환) - Load(적재) 파이프라인
async function etlPipeline() {
  // 1. Extract: 여러 소스에서 데이터 추출
  const response = await fetch('/api/raw-data');

  // 2. Transform: 다단계 변환
  const transformed = response.body
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(createNDJSONTransform())
    // 데이터 정제
    .pipeThrough(new TransformStream({
      transform(record, controller) {
        // null 값 제거, 타입 변환 등
        const cleaned = {
          id: Number(record.id),
          name: (record.name || '').trim(),
          amount: parseFloat(record.amount) || 0,
          date: new Date(record.date).toISOString(),
          category: (record.category || 'unknown').toLowerCase()
        };

        // 유효성 검사
        if (cleaned.id > 0 && cleaned.name) {
          controller.enqueue(cleaned);
        }
      }
    }))
    // 배치로 묶기
    .pipeThrough(createBatchTransform(100));

  // 3. Load: 배치 단위로 적재
  await transformed.pipeTo(new WritableStream({
    async write(batch) {
      console.log(`${batch.length}개 레코드 적재 중...`);
      await fetch('/api/warehouse/bulk-insert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(batch)
      });
      console.log(`${batch.length}개 레코드 적재 완료`);
    },

    close() {
      console.log('ETL 파이프라인 완료');
    }
  }));
}

function createBatchTransform(size) {
  let batch = [];
  return new TransformStream({
    transform(chunk, controller) {
      batch.push(chunk);
      if (batch.length >= size) {
        controller.enqueue(batch);
        batch = [];
      }
    },
    flush(controller) {
      if (batch.length > 0) {
        controller.enqueue(batch);
      }
    }
  });
}
```

---

## 부록: API 요약표

### ReadableStream

| 메서드/속성 | 반환 타입 | 설명 |
|-------------|-----------|------|
| `constructor(source?, strategy?)` | `ReadableStream` | 새 스트림 생성 |
| `locked` | `boolean` | 잠금 상태 |
| `cancel(reason?)` | `Promise<void>` | 스트림 취소 |
| `getReader(options?)` | `Reader` | 리더 획득 |
| `pipeThrough(transform, options?)` | `ReadableStream` | 변환 파이프 |
| `pipeTo(destination, options?)` | `Promise<void>` | 싱크로 파이프 |
| `tee()` | `[ReadableStream, ReadableStream]` | 두 갈래로 분기 |
| `values(options?)` | `AsyncIterator` | 비동기 이터레이터 |
| `ReadableStream.from(iterable)` | `ReadableStream` | 이터러블에서 생성 |

### WritableStream

| 메서드/속성 | 반환 타입 | 설명 |
|-------------|-----------|------|
| `constructor(sink?, strategy?)` | `WritableStream` | 새 스트림 생성 |
| `locked` | `boolean` | 잠금 상태 |
| `abort(reason?)` | `Promise<void>` | 스트림 중단 |
| `close()` | `Promise<void>` | 스트림 닫기 |
| `getWriter()` | `WritableStreamDefaultWriter` | 라이터 획득 |

### TransformStream

| 메서드/속성 | 반환 타입 | 설명 |
|-------------|-----------|------|
| `constructor(transformer?, wStrategy?, rStrategy?)` | `TransformStream` | 새 스트림 생성 |
| `readable` | `ReadableStream` | 읽기 면 |
| `writable` | `WritableStream` | 쓰기 면 |

---

## 참고 자료

- [WHATWG Streams Standard (공식 명세)](https://streams.spec.whatwg.org/)
- [MDN Web Docs - Streams API](https://developer.mozilla.org/en-US/docs/Web/API/Streams_API)
- [MDN - ReadableStream](https://developer.mozilla.org/en-US/docs/Web/API/ReadableStream)
- [MDN - WritableStream](https://developer.mozilla.org/en-US/docs/Web/API/WritableStream)
- [MDN - TransformStream](https://developer.mozilla.org/en-US/docs/Web/API/TransformStream)
- [web.dev - Streams API Guide](https://web.dev/streams/)
