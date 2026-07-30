# WHATWG Storage와 Console

## Storage Standard 상세 가이드

### 목차

1. [개요](#1-개요)
2. [스토리지 유형](#2-스토리지-유형)
3. [StorageManager API](#3-storagemanager-api)
4. [스토리지 쿼터](#4-스토리지-쿼터)
5. [스토리지 버킷](#5-스토리지-버킷)
6. [스토리지 압력](#6-스토리지-압력)
7. [사이트별 스토리지 격리](#7-사이트별-스토리지-격리)
8. [관련 스토리지 API](#8-관련-스토리지-api)
9. [스토리지 관리 전략](#9-스토리지-관리-전략)
10. [실용 예제](#10-실용-예제)
11. [브라우저 호환성](#11-브라우저-호환성)
12. [보안 및 개인정보 고려사항](#12-보안-및-개인정보-고려사항)

---

### 1. 개요

#### 웹 스토리지 관리 표준이란

Storage Standard는 WHATWG에서 관리하는 Living Standard로, 웹 브라우저에서 사이트별 데이터 저장과 관련된 인프라를 정의한다. 이 표준은 개별 스토리지 API(localStorage, IndexedDB, Cache API 등)를 직접 정의하지는 않지만, 이들 API가 공유하는 공통 기반을 제공한다.

```
[Storage Standard의 위치]

┌──────────────────────────────────────────────────────────┐
│                    Storage Standard                       │
│  (스토리지 인프라, 쿼터 관리, 지속성, 격리 정책)           │
├──────────────┬──────────────┬──────────────┬─────────────┤
│ localStorage │  IndexedDB   │  Cache API   │    OPFS     │
│ sessionStorage│              │              │             │
├──────────────┴──────────────┴──────────────┴─────────────┤
│                  StorageManager API                       │
│        (estimate, persist, persisted, getDirectory)       │
└──────────────────────────────────────────────────────────┘
```

#### 핵심 개념

| 개념 | 설명 |
|------|------|
| Storage shelf | 출처(origin)별 스토리지 컨테이너 |
| Storage bucket | 스토리지 선반 내의 논리적 그룹 |
| Storage bottle | 스토리지 버킷 내의 개별 API 영역 |
| Quota | 출처별 스토리지 사용량 제한 |
| Persistence | 데이터의 지속성(best-effort vs persistent) |
| Partitioning | 사이트 간 스토리지 격리 |

#### 스토리지 계층 구조

```
Storage shelf (출처당 하나)
    └── Storage bucket ("default" 및 기타)
         ├── Storage bottle (localStorage)
         ├── Storage bottle (IndexedDB)
         ├── Storage bottle (Cache API)
         └── Storage bottle (OPFS)
```

---

### 2. 스토리지 유형

#### 2.1 Best-effort 스토리지

best-effort 스토리지는 기본 스토리지 모드로, 브라우저가 저장 공간이 부족할 때 자동으로 데이터를 삭제할 수 있다.

```javascript
// 기본적으로 모든 웹 스토리지는 best-effort
localStorage.setItem('data', 'value');  // best-effort
// IndexedDB에 저장한 데이터도 best-effort

// best-effort의 특성:
// - 브라우저가 저장 공간 부족 시 삭제할 수 있음
// - 사용자가 직접 삭제할 수 있음
// - 브라우저 데이터 정리 기능에 의해 삭제될 수 있음
// - 가장 오래 사용하지 않은(LRU) 출처부터 삭제
```

#### 2.2 Persistent 스토리지

persistent 스토리지는 사용자가 명시적으로 삭제하지 않는 한 브라우저가 자동으로 데이터를 삭제하지 않는다.

```javascript
// persistent 스토리지 요청
async function requestPersistentStorage() {
    if (navigator.storage && navigator.storage.persist) {
        const isPersisted = await navigator.storage.persist();

        if (isPersisted) {
            console.log('스토리지가 영구 보존됩니다.');
        } else {
            console.log('스토리지가 여전히 best-effort입니다.');
        }
    }
}
```

#### 2.3 두 유형 비교

| 특성 | Best-effort | Persistent |
|------|-------------|-----------|
| 기본 상태 | 기본값 | 명시적 요청 필요 |
| 자동 삭제 | 가능 (저장 공간 부족 시) | 불가 (사용자만 삭제 가능) |
| 요청 방법 | 별도 요청 불필요 | `navigator.storage.persist()` |
| 삭제 순서 | LRU 기반 (가장 오래 미사용) | 삭제 대상에서 제외 |
| 사용 사례 | 캐시, 임시 데이터 | 사용자 생성 문서, 중요 데이터 |

#### 2.4 Persistent 스토리지 부여 조건

브라우저마다 persistent 스토리지를 자동으로 부여하는 조건이 다르다.

```
[Chrome의 자동 persistent 부여 조건]
다음 중 하나 이상 충족 시:
├── 사이트가 북마크되어 있음 (모바일에서는 홈 화면에 추가)
├── 사이트에 높은 참여도(engagement)가 있음
├── 푸시 알림 권한이 부여되어 있음
└── PWA로 설치되어 있음

[Firefox의 처리 방식]
├── persist() 호출 시 사용자에게 직접 프롬프트 표시
└── 사용자가 "허용"을 선택하면 persistent로 전환

[Safari의 처리 방식]
├── 7일간 사용자 상호작용이 없으면 데이터 삭제 가능 (ITP)
└── persist() API 지원이 제한적
```

---

### 3. StorageManager API

#### 3.1 navigator.storage 인터페이스

```typescript
interface StorageManager {
    estimate(): Promise<StorageEstimate>;
    persist(): Promise<boolean>;
    persisted(): Promise<boolean>;
    getDirectory(): Promise<FileSystemDirectoryHandle>;  // OPFS
}

interface StorageEstimate {
    usage?: number;   // 현재 사용량 (바이트)
    quota?: number;   // 할당량 (바이트)
}
```

#### 3.2 navigator.storage.estimate()

현재 출처의 스토리지 사용량과 할당량을 확인한다.

```javascript
async function checkStorageUsage() {
    if (!navigator.storage || !navigator.storage.estimate) {
        console.warn('StorageManager API가 지원되지 않습니다.');
        return;
    }

    const estimate = await navigator.storage.estimate();

    console.log('사용량:', formatBytes(estimate.usage));
    console.log('할당량:', formatBytes(estimate.quota));
    console.log('사용률:', ((estimate.usage / estimate.quota) * 100).toFixed(2) + '%');
    console.log('남은 공간:', formatBytes(estimate.quota - estimate.usage));
}

// 바이트를 사람이 읽기 쉬운 형식으로 변환
function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + units[i];
}

// 실행 결과 예시:
// 사용량: 12.45 MB
// 할당량: 2.93 GB
// 사용률: 0.41%
// 남은 공간: 2.92 GB
```

#### 3.3 navigator.storage.persist()

```javascript
async function requestPersistence() {
    // 이미 persistent인지 확인
    const alreadyPersisted = await navigator.storage.persisted();

    if (alreadyPersisted) {
        console.log('이미 영구 스토리지입니다.');
        return true;
    }

    // persistent 스토리지 요청
    const granted = await navigator.storage.persist();

    if (granted) {
        console.log('영구 스토리지가 부여되었습니다.');
        console.log('브라우저가 자동으로 데이터를 삭제하지 않습니다.');
    } else {
        console.log('영구 스토리지가 거부되었습니다.');
        console.log('브라우저가 저장 공간 부족 시 데이터를 삭제할 수 있습니다.');
    }

    return granted;
}
```

#### 3.4 navigator.storage.persisted()

```javascript
async function checkPersistence() {
    const isPersisted = await navigator.storage.persisted();

    if (isPersisted) {
        console.log('현재 스토리지: Persistent (영구)');
        console.log('→ 브라우저가 자동 삭제하지 않음');
    } else {
        console.log('현재 스토리지: Best-effort (임시)');
        console.log('→ 브라우저가 자동 삭제할 수 있음');
    }

    return isPersisted;
}
```

#### 3.5 navigator.storage.getDirectory() (OPFS)

```javascript
// Origin Private File System (OPFS) 접근
async function accessOPFS() {
    const root = await navigator.storage.getDirectory();

    console.log('OPFS 루트 디렉터리:', root.name);  // ""

    // 파일 생성
    const fileHandle = await root.getFileHandle('data.json', { create: true });
    const writable = await fileHandle.createWritable();
    await writable.write(JSON.stringify({ key: 'value' }));
    await writable.close();

    // 파일 읽기
    const file = await fileHandle.getFile();
    const text = await file.text();
    console.log('파일 내용:', text);

    // 디렉터리 생성
    const dirHandle = await root.getDirectoryHandle('myDir', { create: true });

    // 디렉터리 내용 나열
    for await (const [name, handle] of root.entries()) {
        console.log(`${handle.kind}: ${name}`);
    }
}
```

---

### 4. 스토리지 쿼터

#### 4.1 쿼터 개념

스토리지 쿼터(quota)는 각 출처(origin)가 사용할 수 있는 최대 스토리지 공간이다.

```
[쿼터 계산 개념]

디스크 전체 용량: 500 GB
    ↓
브라우저 스토리지 풀: ~300 GB (디스크의 ~60%)
    ↓
출처별 할당:
├── https://example.com  → 최대 ~150 GB (풀의 ~50%)
├── https://other.com    → 최대 ~150 GB (풀의 ~50%)
└── ...
```

#### 4.2 브라우저별 쿼터 정책

| 브라우저 | 전체 쿼터 | 출처별 쿼터 | 비고 |
|----------|----------|------------|------|
| Chrome | 디스크의 ~60% | 전체 풀의 ~60% | 동적 계산 |
| Firefox | 디스크의 ~50%(그룹 단위) | eTLD+1 그룹당 디스크의 ~10%, 최대 10GiB | 같은 eTLD+1의 모든 출처가 그룹 한도를 공유 |
| Safari | 디바이스/앱 유형에 따라 동적 | 브라우저 앱: 디스크의 최대 ~60%, 그 외 앱: 최대 ~15% | 버전별로 정책이 자주 바뀜, 최신 문서 확인 필요 |
| Edge | Chrome과 동일 | Chrome과 동일 | Chromium 기반 |

> 위 수치는 브라우저 정책 변경이 잦으므로 참고용이며, 정확한 값은 각 브라우저의 최신 공식 문서를 확인해야 한다.

```javascript
// 브라우저별 쿼터 확인 예제
async function displayStorageInfo() {
    const estimate = await navigator.storage.estimate();
    const isPersisted = await navigator.storage.persisted();

    const info = {
        사용량: formatBytes(estimate.usage),
        할당량: formatBytes(estimate.quota),
        사용률: ((estimate.usage / estimate.quota) * 100).toFixed(2) + '%',
        영구_스토리지: isPersisted ? '예' : '아니오',
        남은_공간: formatBytes(estimate.quota - estimate.usage)
    };

    console.table(info);

    // 쿼터의 80% 이상 사용 시 경고
    if (estimate.usage / estimate.quota > 0.8) {
        console.warn('스토리지 사용량이 80%를 초과했습니다!');
        console.warn('데이터 정리를 고려하세요.');
    }

    return info;
}
```

#### 4.3 쿼터 초과 시 동작

```javascript
// 쿼터 초과 시 에러 처리
async function handleQuotaExceeded() {
    try {
        // IndexedDB에 큰 데이터 저장 시도
        const db = await openDatabase();
        const tx = db.transaction('store', 'readwrite');
        const store = tx.objectStore('store');
        await store.put(largeData, 'key');
    } catch (error) {
        if (error.name === 'QuotaExceededError') {
            console.error('스토리지 공간이 부족합니다.');

            // 대응 방법:
            // 1. 오래된 데이터 삭제
            await cleanupOldData();

            // 2. 사용자에게 알림
            showStorageFullNotification();

            // 3. persistent 스토리지 요청
            await navigator.storage.persist();

            // 4. 재시도
            await retryOperation();
        } else {
            throw error;
        }
    }
}

// localStorage에서의 쿼터 초과
try {
    localStorage.setItem('key', veryLargeString);
} catch (error) {
    if (error.name === 'QuotaExceededError' ||
        error.code === 22 ||  // Safari
        error.code === 1014) { // Firefox
        console.error('localStorage 공간 부족');
    }
}
```

#### 4.4 API별 쿼터 영향

```
[스토리지 쿼터에 포함되는 API]

navigator.storage.estimate() 에 포함:
├── IndexedDB        ← 가장 큰 비중을 차지하는 경우가 많음
├── Cache API        ← Service Worker 캐시
├── OPFS             ← Origin Private File System
├── localStorage     ← 포함되지만 별도 제한도 있음 (~5-10MB)
└── sessionStorage   ← 포함되지만 별도 제한도 있음 (~5-10MB)

별도 제한이 있는 API:
├── localStorage     → 보통 5MB per origin
├── sessionStorage   → 보통 5MB per origin
└── Cookie           → 4KB per cookie, ~80개 per domain
```

---

### 5. 스토리지 버킷

#### 5.1 Storage Buckets 개념

Storage Buckets API는 하나의 출처 내에서 스토리지를 논리적으로 분리하는 메커니즘이다.

```
[Storage Buckets 구조]

출처: https://example.com
    ├── "default" bucket (기본)
    │   ├── IndexedDB
    │   ├── Cache API
    │   └── localStorage
    │
    ├── "user-data" bucket
    │   ├── IndexedDB (사용자 문서)
    │   └── OPFS (사용자 파일)
    │
    └── "cache-data" bucket
        └── Cache API (캐시 데이터)
```

#### 5.2 Storage Buckets API

Storage Buckets API 자체는 Chrome 122+에서 정식 출시(stable)되었다. 다만 이 절에서 다루는 `durability` 옵션은 WHATWG Storage Standard가 아니라 별도의 WICG 제안(Storage Buckets Explainer)에 속하는 확장 기능이다.

```javascript
// Storage Buckets API (Chrome 122+ 정식 지원, 다른 브라우저는 아직 미지원)

// 새 버킷 생성
const userDataBucket = await navigator.storageBuckets.open('user-data', {
    persisted: true,       // 영구 스토리지
    durability: 'strict',  // 엄격한 내구성
    quota: 500 * 1024 * 1024,  // 500MB 제한
    expires: null          // 만료 없음
});

// 캐시용 버킷 (임시)
const cacheBucket = await navigator.storageBuckets.open('cache-data', {
    persisted: false,       // best-effort
    durability: 'relaxed',  // 완화된 내구성 (성능 우선)
    quota: 100 * 1024 * 1024,  // 100MB
    expires: Date.now() + (30 * 24 * 60 * 60 * 1000) // 30일 후 만료
});

// 버킷 내의 IndexedDB 사용
const db = await userDataBucket.indexedDB.open('documents', 1);

// 버킷 내의 Cache API 사용
const cache = await cacheBucket.caches.open('api-cache');

// 버킷 사용량 확인
const estimate = await userDataBucket.estimate();
console.log('user-data 버킷 사용량:', formatBytes(estimate.usage));

// 버킷 삭제
await navigator.storageBuckets.delete('cache-data');

// 모든 버킷 나열
const buckets = await navigator.storageBuckets.keys();
console.log('버킷 목록:', buckets);
```

#### 5.3 버킷의 durability 옵션

```javascript
// durability: 'strict' (기본)
// - 데이터가 디스크에 완전히 기록된 후에만 성공 반환
// - 전원 장애에도 데이터 손실 방지
// - 성능이 다소 느림
const strictBucket = await navigator.storageBuckets.open('important', {
    durability: 'strict'
});

// durability: 'relaxed'
// - 데이터가 OS 버퍼에 기록되면 성공 반환
// - 전원 장애 시 최근 데이터 손실 가능
// - 성능이 더 빠름
const relaxedBucket = await navigator.storageBuckets.open('cache', {
    durability: 'relaxed'
});
```

---

### 6. 스토리지 압력

#### 6.1 스토리지 압력이란

스토리지 압력(Storage Pressure)은 디바이스의 저장 공간이 부족해지는 상태를 의미한다. 이 상태에서 브라우저는 데이터를 자동으로 삭제하여 공간을 확보한다.

```
[스토리지 압력 레벨]

정상 → 주의 → 경고 → 위험 → 임계

정상: 충분한 공간
    └── 아무 조치 없음

주의: 공간이 줄어들기 시작
    └── 만료된 캐시 삭제

경고: 공간이 부족해지기 시작
    └── best-effort 스토리지 중 LRU 출처 데이터 삭제

위험: 공간이 심각하게 부족
    └── 더 적극적인 데이터 삭제

임계: 디바이스 저장 공간 거의 없음
    └── persistent 제외 모든 데이터 삭제 가능
```

#### 6.2 삭제 우선순위

```
[데이터 삭제 우선순위 (낮은 순서 → 높은 순서)]

1. 만료된 캐시 데이터
2. best-effort 스토리지 (LRU 순서)
   ├── 가장 오래전에 방문한 출처의 데이터
   ├── 두 번째로 오래전에 방문한 출처의 데이터
   └── ...
3. persistent 스토리지 → 브라우저가 자동 삭제하지 않음

주의: 삭제는 출처(origin) 단위로 이루어짐
     = 한 출처의 데이터가 삭제되면 해당 출처의 모든 API 데이터가 삭제됨
     (IndexedDB + Cache API + localStorage 등 모두)
```

#### 6.3 스토리지 압력 대응 전략

```javascript
class StoragePressureManager {
    constructor() {
        this.warningThreshold = 0.8;  // 80%
        this.criticalThreshold = 0.9; // 90%
    }

    async checkPressure() {
        const estimate = await navigator.storage.estimate();
        const usage = estimate.usage / estimate.quota;

        if (usage >= this.criticalThreshold) {
            return 'critical';
        } else if (usage >= this.warningThreshold) {
            return 'warning';
        }
        return 'normal';
    }

    async handlePressure() {
        const level = await this.checkPressure();

        switch (level) {
            case 'critical':
                console.warn('스토리지 위험 수준!');
                await this.aggressiveCleanup();
                break;

            case 'warning':
                console.warn('스토리지 주의 수준');
                await this.moderateCleanup();
                break;

            case 'normal':
                console.log('스토리지 정상');
                break;
        }
    }

    async moderateCleanup() {
        // 1. 오래된 캐시 삭제
        const cacheNames = await caches.keys();
        const oldCaches = cacheNames.filter(name =>
            name.startsWith('v') && name !== 'v' + CURRENT_VERSION
        );
        await Promise.all(oldCaches.map(name => caches.delete(name)));

        // 2. 만료된 IndexedDB 레코드 삭제
        await this.deleteExpiredRecords();

        console.log('일반 정리 완료');
    }

    async aggressiveCleanup() {
        // 1. 일반 정리 수행
        await this.moderateCleanup();

        // 2. 썸네일 캐시 삭제
        await caches.delete('thumbnails');

        // 3. 임시 파일 삭제
        await this.deleteTempFiles();

        // 4. 오래된 오프라인 데이터 삭제
        await this.deleteOldOfflineData();

        console.log('적극적 정리 완료');
    }

    async deleteExpiredRecords() {
        // IndexedDB에서 만료된 레코드 삭제
        const db = await this.openDB();
        const tx = db.transaction('cache', 'readwrite');
        const store = tx.objectStore('cache');
        const index = store.index('expires');
        const now = Date.now();

        let cursor = await index.openCursor(IDBKeyRange.upperBound(now));
        while (cursor) {
            await cursor.delete();
            cursor = await cursor.continue();
        }
    }

    async deleteTempFiles() {
        try {
            const root = await navigator.storage.getDirectory();
            const tempDir = await root.getDirectoryHandle('temp');

            for await (const [name] of tempDir.entries()) {
                await tempDir.removeEntry(name);
            }
        } catch (e) {
            // temp 디렉터리가 없으면 무시
        }
    }

    async deleteOldOfflineData() {
        // 30일 이상 된 오프라인 데이터 삭제
        const db = await this.openDB();
        const tx = db.transaction('offline-data', 'readwrite');
        const store = tx.objectStore('offline-data');
        const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);

        const index = store.index('savedAt');
        let cursor = await index.openCursor(IDBKeyRange.upperBound(thirtyDaysAgo));
        while (cursor) {
            await cursor.delete();
            cursor = await cursor.continue();
        }
    }

    async openDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open('app-db', 1);
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }
}

// 주기적으로 스토리지 압력 확인
const pressureManager = new StoragePressureManager();
setInterval(() => pressureManager.handlePressure(), 60000); // 1분마다
```

---

### 7. 사이트별 스토리지 격리

#### 7.1 스토리지 파티셔닝

스토리지 파티셔닝(Storage Partitioning)은 서드파티 컨텍스트에서의 스토리지를 격리하여 교차 사이트 추적을 방지하는 메커니즘이다.

```
[파티셔닝 이전 (기존 동작)]

사이트 A에서 iframe으로 tracker.com 로드:
tracker.com의 스토리지 ← 공유됨 → tracker.com의 스토리지
                                    사이트 B에서 iframe으로 tracker.com 로드

결과: tracker.com이 사이트 A와 B에서 같은 쿠키/스토리지에 접근
     → 교차 사이트 추적 가능


[파티셔닝 이후 (현대 브라우저)]

사이트 A에서 iframe으로 tracker.com 로드:
(사이트A, tracker.com) 스토리지    ← 격리됨 →    (사이트B, tracker.com) 스토리지
                                                  사이트 B에서 iframe으로 tracker.com 로드

결과: 각 최상위 사이트별로 별도의 스토리지
     → 교차 사이트 추적 불가
```

#### 7.2 파티셔닝 키

```
[파티셔닝 키 구조]

파티셔닝 키 = (최상위 사이트, 출처)

예시:
┌─────────────────────────────────────────┐
│ 최상위 사이트: https://shopping.com      │
│   └── iframe: https://payment.com        │
│       파티셔닝 키: (shopping.com, payment.com)  │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 최상위 사이트: https://blog.com          │
│   └── iframe: https://payment.com        │
│       파티셔닝 키: (blog.com, payment.com)      │
└─────────────────────────────────────────┘

→ 같은 payment.com이지만 다른 파티션에 저장
→ 서로의 데이터에 접근 불가
```

#### 7.3 파티셔닝의 영향을 받는 API

| API | 파티셔닝 영향 |
|-----|--------------|
| Cookies | 서드파티 쿠키 차단/파티셔닝 |
| localStorage | 서드파티 컨텍스트에서 파티셔닝 |
| sessionStorage | 서드파티 컨텍스트에서 파티셔닝 |
| IndexedDB | 서드파티 컨텍스트에서 파티셔닝 |
| Cache API | 서드파티 컨텍스트에서 파티셔닝 |
| BroadcastChannel | 파티션 내에서만 동작 |
| SharedWorker | 파티션 내에서만 공유 |
| Service Worker | 퍼스트파티에서만 등록 가능 |

#### 7.4 Storage Access API

파티셔닝된 컨텍스트에서 퍼스트파티 스토리지에 접근해야 하는 경우 Storage Access API를 사용한다.

```javascript
// iframe 내부에서 (서드파티 컨텍스트)

// 스토리지 접근 가능 여부 확인
const hasAccess = await document.hasStorageAccess();
console.log('스토리지 접근 가능:', hasAccess);

if (!hasAccess) {
    try {
        // 스토리지 접근 요청 (사용자 상호작용 필요)
        await document.requestStorageAccess();
        console.log('스토리지 접근이 허용되었습니다.');

        // 이제 파티셔닝되지 않은 스토리지에 접근 가능
        const data = localStorage.getItem('user-preference');
    } catch (error) {
        console.error('스토리지 접근이 거부되었습니다:', error);
    }
}
```

---

### 8. 관련 스토리지 API

#### 8.1 localStorage

```javascript
// Web Storage API - localStorage
// 동기적, 문자열만 저장, ~5-10MB

// 저장
localStorage.setItem('username', '김철수');
localStorage.setItem('settings', JSON.stringify({
    theme: 'dark',
    language: 'ko'
}));

// 읽기
const username = localStorage.getItem('username');
const settings = JSON.parse(localStorage.getItem('settings'));

// 삭제
localStorage.removeItem('username');

// 전체 삭제
localStorage.clear();

// 길이 확인
console.log('저장된 항목 수:', localStorage.length);

// 키 접근
for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    console.log(key, ':', localStorage.getItem(key));
}

// storage 이벤트 (다른 탭에서 변경 감지)
window.addEventListener('storage', (event) => {
    console.log('키:', event.key);
    console.log('이전 값:', event.oldValue);
    console.log('새 값:', event.newValue);
    console.log('출처:', event.url);
});
```

#### 8.2 sessionStorage

```javascript
// Web Storage API - sessionStorage
// 동기적, 탭/세션 단위, ~5-10MB

// localStorage와 동일한 API
sessionStorage.setItem('tempData', 'value');
const data = sessionStorage.getItem('tempData');
sessionStorage.removeItem('tempData');
sessionStorage.clear();

// 차이점:
// - 탭을 닫으면 데이터 삭제
// - 탭 간에 데이터가 공유되지 않음
// - 탭 복제 시 데이터가 복사됨
// - 탭 복원 시 데이터가 복원됨
```

#### 8.3 IndexedDB

```javascript
// IndexedDB - 대용량 구조화 데이터 저장
// 비동기, 트랜잭션 기반, 인덱스 지원

// 데이터베이스 열기
function openDB(name, version) {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(name, version);

        request.onupgradeneeded = (event) => {
            const db = event.target.result;

            if (!db.objectStoreNames.contains('users')) {
                const store = db.createObjectStore('users', { keyPath: 'id', autoIncrement: true });
                store.createIndex('email', 'email', { unique: true });
                store.createIndex('name', 'name', { unique: false });
            }
        };

        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

// 데이터 추가
async function addUser(user) {
    const db = await openDB('myApp', 1);
    const tx = db.transaction('users', 'readwrite');
    const store = tx.objectStore('users');
    const id = await new Promise((resolve, reject) => {
        const request = store.add(user);
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
    return id;
}

// 데이터 조회
async function getUser(id) {
    const db = await openDB('myApp', 1);
    const tx = db.transaction('users', 'readonly');
    const store = tx.objectStore('users');
    return new Promise((resolve, reject) => {
        const request = store.get(id);
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}
```

#### 8.4 Cache API

```javascript
// Cache API - HTTP 응답 캐싱
// 주로 Service Worker에서 사용

// 캐시 열기 및 리소스 추가
async function cacheResources() {
    const cache = await caches.open('v1');

    // 단일 리소스 캐싱
    await cache.add('/api/data');

    // 여러 리소스 캐싱
    await cache.addAll([
        '/',
        '/styles.css',
        '/script.js',
        '/images/logo.png'
    ]);

    // 커스텀 응답 캐싱
    const response = new Response(JSON.stringify({ cached: true }), {
        headers: { 'Content-Type': 'application/json' }
    });
    await cache.put('/api/cached-data', response);
}

// 캐시에서 조회
async function getCachedResponse(request) {
    const cache = await caches.open('v1');
    const response = await cache.match(request);

    if (response) {
        console.log('캐시 히트!');
        return response;
    }

    console.log('캐시 미스 - 네트워크 요청');
    return fetch(request);
}

// 캐시 삭제
async function deleteOldCaches() {
    const keys = await caches.keys();
    const currentVersion = 'v2';

    await Promise.all(
        keys.filter(key => key !== currentVersion)
            .map(key => caches.delete(key))
    );
}
```

#### 8.5 Origin Private File System (OPFS)

```javascript
// OPFS - 고성능 파일 시스템 접근
// 웹 워커에서의 동기적 접근 지원

// 루트 디렉터리 얻기
const root = await navigator.storage.getDirectory();

// 파일 생성 및 쓰기
async function writeFile(filename, content) {
    const fileHandle = await root.getFileHandle(filename, { create: true });
    const writable = await fileHandle.createWritable();
    await writable.write(content);
    await writable.close();
}

// 파일 읽기
async function readFile(filename) {
    const fileHandle = await root.getFileHandle(filename);
    const file = await fileHandle.getFile();
    return file.text();
}

// 디렉터리 생성
async function createDirectory(dirname) {
    return root.getDirectoryHandle(dirname, { create: true });
}

// 파일/디렉터리 삭제
async function removeEntry(name) {
    await root.removeEntry(name, { recursive: true });
}

// 디렉터리 내용 나열
async function listDirectory(dirHandle) {
    const entries = [];
    for await (const [name, handle] of (dirHandle || root).entries()) {
        entries.push({ name, kind: handle.kind });
    }
    return entries;
}

// 웹 워커에서의 동기적 접근 (고성능)
// worker.js 내부:
// const root = await navigator.storage.getDirectory();
// const fileHandle = await root.getFileHandle('data.bin', { create: true });
// const accessHandle = await fileHandle.createSyncAccessHandle();
//
// // 동기적 읽기/쓰기
// const buffer = new ArrayBuffer(1024);
// const readSize = accessHandle.read(buffer);
// accessHandle.write(new Uint8Array([1, 2, 3]));
// accessHandle.flush();
// accessHandle.close();
```

#### 8.6 스토리지 API 비교

| 특성 | localStorage | sessionStorage | IndexedDB | Cache API | OPFS |
|------|-------------|---------------|-----------|-----------|------|
| 용량 | ~5-10MB | ~5-10MB | 쿼터까지 | 쿼터까지 | 쿼터까지 |
| 동기/비동기 | 동기 | 동기 | 비동기 | 비동기 | 비동기(+동기) |
| 데이터 형식 | 문자열 | 문자열 | 구조화 | HTTP 응답 | 파일/바이너리 |
| 지속성 | 영구 | 세션 | 영구 | 영구 | 영구 |
| 인덱스 | 없음 | 없음 | 있음 | URL 기반 | 없음 |
| 워커 사용 | 불가 | 불가 | 가능 | 가능 | 가능 |
| 트랜잭션 | 없음 | 없음 | 있음 | 없음 | 없음 |
| 사용 사례 | 설정, 토큰 | 임시 상태 | 앱 데이터 | 오프라인 캐시 | 파일 처리 |

---

### 9. 스토리지 관리 전략

#### 9.1 스토리지 사용량 모니터링

```javascript
class StorageMonitor {
    constructor(options = {}) {
        this.warningThreshold = options.warningThreshold || 0.7;
        this.criticalThreshold = options.criticalThreshold || 0.9;
        this.checkInterval = options.checkInterval || 60000; // 1분
        this.listeners = new Map();
        this.timer = null;
    }

    start() {
        this.check();
        this.timer = setInterval(() => this.check(), this.checkInterval);
    }

    stop() {
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }

    on(event, callback) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, []);
        }
        this.listeners.get(event).push(callback);
    }

    emit(event, data) {
        const callbacks = this.listeners.get(event) || [];
        callbacks.forEach(cb => cb(data));
    }

    async check() {
        const estimate = await navigator.storage.estimate();
        const ratio = estimate.usage / estimate.quota;

        const status = {
            usage: estimate.usage,
            quota: estimate.quota,
            ratio: ratio,
            usageFormatted: formatBytes(estimate.usage),
            quotaFormatted: formatBytes(estimate.quota),
            percentUsed: (ratio * 100).toFixed(2)
        };

        this.emit('status', status);

        if (ratio >= this.criticalThreshold) {
            this.emit('critical', status);
        } else if (ratio >= this.warningThreshold) {
            this.emit('warning', status);
        }
    }
}

// 사용
const monitor = new StorageMonitor({
    warningThreshold: 0.7,
    criticalThreshold: 0.9,
    checkInterval: 30000
});

monitor.on('status', (status) => {
    updateStorageUI(status);
});

monitor.on('warning', (status) => {
    showWarning(`스토리지 사용량 ${status.percentUsed}%`);
});

monitor.on('critical', (status) => {
    showCriticalAlert(`스토리지 부족! ${status.percentUsed}%`);
    performEmergencyCleanup();
});

monitor.start();
```

#### 9.2 스토리지 전략 패턴

```javascript
// 계층화된 스토리지 전략
class TieredStorage {
    constructor() {
        this.tiers = {
            // 1단계: 메모리 캐시 (가장 빠름)
            memory: new Map(),

            // 2단계: sessionStorage (세션 유지)
            session: {
                get: (key) => {
                    const item = sessionStorage.getItem(key);
                    return item ? JSON.parse(item) : null;
                },
                set: (key, value) => {
                    try {
                        sessionStorage.setItem(key, JSON.stringify(value));
                    } catch (e) { /* 공간 부족 */ }
                },
                delete: (key) => sessionStorage.removeItem(key)
            },

            // 3단계: localStorage (영구 저장, 작은 데이터)
            local: {
                get: (key) => {
                    const item = localStorage.getItem(key);
                    return item ? JSON.parse(item) : null;
                },
                set: (key, value) => {
                    try {
                        localStorage.setItem(key, JSON.stringify(value));
                    } catch (e) { /* 공간 부족 */ }
                },
                delete: (key) => localStorage.removeItem(key)
            },

            // 4단계: IndexedDB (대용량 데이터)
            indexedDB: new IndexedDBStorage()
        };
    }

    async get(key) {
        // 빠른 순서대로 조회
        if (this.tiers.memory.has(key)) {
            return this.tiers.memory.get(key);
        }

        const sessionValue = this.tiers.session.get(key);
        if (sessionValue !== null) {
            this.tiers.memory.set(key, sessionValue);
            return sessionValue;
        }

        const localValue = this.tiers.local.get(key);
        if (localValue !== null) {
            this.tiers.memory.set(key, localValue);
            return localValue;
        }

        const dbValue = await this.tiers.indexedDB.get(key);
        if (dbValue !== null) {
            this.tiers.memory.set(key, dbValue);
            return dbValue;
        }

        return null;
    }

    async set(key, value, options = {}) {
        const { tier = 'local', ttl = null } = options;
        const data = ttl ? { value, expires: Date.now() + ttl } : { value };

        // 메모리에는 항상 캐시
        this.tiers.memory.set(key, data);

        // 지정된 tier에 저장
        switch (tier) {
            case 'session':
                this.tiers.session.set(key, data);
                break;
            case 'local':
                this.tiers.local.set(key, data);
                break;
            case 'indexedDB':
                await this.tiers.indexedDB.set(key, data);
                break;
        }
    }
}
```

---

### 10. 실용 예제

#### 10.1 스토리지 대시보드

```javascript
async function createStorageDashboard() {
    const dashboard = {
        total: await navigator.storage.estimate(),
        persisted: await navigator.storage.persisted()
    };

    // 개별 API 사용량 추정
    // (참고: 정확한 API별 사용량은 브라우저마다 다를 수 있음)

    // IndexedDB 사용량
    const databases = await indexedDB.databases();
    dashboard.indexedDB = {
        databases: databases.map(db => db.name),
        count: databases.length
    };

    // Cache API 사용량
    const cacheNames = await caches.keys();
    dashboard.cacheAPI = {
        caches: cacheNames,
        count: cacheNames.length
    };

    // localStorage 사용량
    let localStorageSize = 0;
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        const value = localStorage.getItem(key);
        localStorageSize += (key.length + value.length) * 2; // UTF-16
    }
    dashboard.localStorage = {
        items: localStorage.length,
        size: formatBytes(localStorageSize)
    };

    // sessionStorage 사용량
    let sessionStorageSize = 0;
    for (let i = 0; i < sessionStorage.length; i++) {
        const key = sessionStorage.key(i);
        const value = sessionStorage.getItem(key);
        sessionStorageSize += (key.length + value.length) * 2;
    }
    dashboard.sessionStorage = {
        items: sessionStorage.length,
        size: formatBytes(sessionStorageSize)
    };

    return dashboard;
}
```

#### 10.2 오프라인 데이터 관리자

```javascript
class OfflineDataManager {
    constructor() {
        this.dbName = 'offline-app';
        this.dbVersion = 1;
    }

    async init() {
        // persistent 스토리지 요청
        const persisted = await navigator.storage.persist();
        console.log('영구 스토리지:', persisted);

        // 스토리지 상태 확인
        const estimate = await navigator.storage.estimate();
        console.log('사용 가능:', formatBytes(estimate.quota - estimate.usage));

        // 데이터베이스 초기화
        await this.openDB();
    }

    async syncWhenOnline(data) {
        if (navigator.onLine) {
            await this.syncToServer(data);
        } else {
            await this.saveOffline(data);
            // 온라인 복귀 시 동기화
            window.addEventListener('online', async () => {
                await this.syncPendingData();
            }, { once: true });
        }
    }

    async saveOffline(data) {
        const db = await this.openDB();
        const tx = db.transaction('pendingSync', 'readwrite');
        const store = tx.objectStore('pendingSync');
        await new Promise((resolve, reject) => {
            const request = store.add({
                ...data,
                savedAt: Date.now(),
                synced: false
            });
            request.onsuccess = resolve;
            request.onerror = reject;
        });
    }

    async syncPendingData() {
        const db = await this.openDB();
        const tx = db.transaction('pendingSync', 'readwrite');
        const store = tx.objectStore('pendingSync');

        const allRecords = await new Promise((resolve, reject) => {
            const request = store.getAll();
            request.onsuccess = () => resolve(request.result);
            request.onerror = reject;
        });

        const unsyncedRecords = allRecords.filter(r => !r.synced);

        for (const record of unsyncedRecords) {
            try {
                await this.syncToServer(record);
                await new Promise((resolve, reject) => {
                    const delRequest = store.delete(record.id);
                    delRequest.onsuccess = resolve;
                    delRequest.onerror = reject;
                });
            } catch (error) {
                console.error('동기화 실패:', error);
            }
        }
    }

    async syncToServer(data) {
        const response = await fetch('/api/sync', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            throw new Error('서버 동기화 실패');
        }
    }

    openDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.dbVersion);
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains('pendingSync')) {
                    const store = db.createObjectStore('pendingSync', {
                        keyPath: 'id',
                        autoIncrement: true
                    });
                    store.createIndex('savedAt', 'savedAt');
                    store.createIndex('synced', 'synced');
                }
            };
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }
}
```

---

### 11. 브라우저 호환성

#### 11.1 StorageManager API 지원 현황

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| `navigator.storage` | 55+ | 57+ | 15.2+ | 79+ |
| `estimate()` | 55+ | 57+ | 17+ | 79+ |
| `persist()` | 52+ | 55+ | 15.2+ | 79+ |
| `persisted()` | 52+ | 55+ | 15.2+ | 79+ |
| `getDirectory()` (OPFS) | 86+ | 111+ | 15.2+ | 86+ |
| Storage Buckets | 122+ (정식 지원) | 미지원 | 미지원 | 122+ (Chromium 기반) |

#### 11.2 기능 감지

```javascript
// StorageManager API 지원 확인
function checkStorageSupport() {
    const support = {
        storageManager: 'storage' in navigator,
        estimate: false,
        persist: false,
        persisted: false,
        getDirectory: false,
        indexedDB: 'indexedDB' in window,
        cacheAPI: 'caches' in window,
        localStorage: 'localStorage' in window,
        sessionStorage: 'sessionStorage' in window
    };

    if (support.storageManager) {
        support.estimate = 'estimate' in navigator.storage;
        support.persist = 'persist' in navigator.storage;
        support.persisted = 'persisted' in navigator.storage;
        support.getDirectory = 'getDirectory' in navigator.storage;
    }

    return support;
}

console.table(checkStorageSupport());
```

---

### 12. 보안 및 개인정보 고려사항

#### 12.1 보안 컨텍스트

```javascript
// Storage Standard의 대부분의 API는 보안 컨텍스트(HTTPS) 필요
if (window.isSecureContext) {
    // StorageManager API 사용 가능
    const estimate = await navigator.storage.estimate();
} else {
    // HTTP에서는 제한적
    // localStorage, sessionStorage는 HTTP에서도 사용 가능
    // navigator.storage는 사용 불가
}
```

#### 12.2 스토리지를 통한 핑거프린팅 방지

```
[스토리지 기반 핑거프린팅 벡터]

1. 쿼터 크기로 디스크 용량 추정
   → estimate()가 정확한 값 대신 근사값을 반환하는 이유

2. 스토리지 타이밍 공격
   → 저장/읽기 속도로 하드웨어 특성 추정

3. 스토리지 가용 여부로 브라우저 설정 추정
   → 프라이빗 모드 감지 시도

[브라우저의 대응]
├── estimate()의 반환값을 근사화 (정확한 값 제공 안 함)
├── 프라이빗 모드에서도 동일한 API 동작 유지
├── 크로스 사이트 스토리지 파티셔닝
└── 정기적인 미사용 데이터 삭제
```

#### 12.3 데이터 보호 권장사항

```javascript
// 민감한 데이터 암호화 후 저장
async function encryptAndStore(key, data, encryptionKey) {
    const encoder = new TextEncoder();
    const iv = crypto.getRandomValues(new Uint8Array(12));

    const encrypted = await crypto.subtle.encrypt(
        { name: 'AES-GCM', iv },
        encryptionKey,
        encoder.encode(JSON.stringify(data))
    );

    const stored = {
        iv: Array.from(iv),
        data: Array.from(new Uint8Array(encrypted))
    };

    localStorage.setItem(key, JSON.stringify(stored));
}

async function decryptAndRetrieve(key, encryptionKey) {
    const stored = JSON.parse(localStorage.getItem(key));
    if (!stored) return null;

    const decrypted = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv: new Uint8Array(stored.iv) },
        encryptionKey,
        new Uint8Array(stored.data)
    );

    const decoder = new TextDecoder();
    return JSON.parse(decoder.decode(decrypted));
}
```

---

### 참고 자료

- [Storage Standard (WHATWG)](https://storage.spec.whatwg.org/)
- [MDN - StorageManager](https://developer.mozilla.org/ko/docs/Web/API/StorageManager)
- [MDN - Storage API](https://developer.mozilla.org/ko/docs/Web/API/Storage_API)
- [MDN - IndexedDB API](https://developer.mozilla.org/ko/docs/Web/API/IndexedDB_API)
- [MDN - Cache API](https://developer.mozilla.org/ko/docs/Web/API/Cache)
- [web.dev - Storage for the web](https://web.dev/storage-for-the-web/)
- [Storage Buckets API Explainer](https://github.com/nicjansma/nicjansma.github.io/blob/main/nicj.net/bsky/2024/storage-buckets.md)

---

## WHATWG Console Standard

### 목차

1. [개요](#1-개요)
2. [console 네임스페이스](#2-console-네임스페이스)
3. [로깅 메서드](#3-로깅-메서드)
4. [카운팅 메서드](#4-카운팅-메서드)
5. [그룹 메서드](#5-그룹-메서드)
6. [타이밍 메서드](#6-타이밍-메서드)
7. [포매팅](#7-포매팅)
8. [Logger 개념](#8-logger-개념)
9. [console의 동작 보장 범위](#9-console의-동작-보장-범위)
10. [실용 예제](#10-실용-예제)

---

### 1. 개요

#### 1.1 Console Standard란

WHATWG(Web Hypertext Application Technology Working Group) Console Standard는 JavaScript 런타임 환경에서
`console` 객체의 API를 정의하는 공식 표준 명세이다. 이 표준은 개발자가 디버깅, 로깅, 성능 측정 등을 수행할 때
사용하는 `console` 객체의 메서드와 동작 방식을 규정한다.

표준 문서의 공식 URL은 다음과 같다.

```
https://console.spec.whatwg.org/
```

이 명세는 Living Standard(살아있는 표준) 형태로 유지되며, 필요에 따라 지속적으로 갱신된다.
W3C의 전통적인 스냅샷 방식과 달리, WHATWG의 Living Standard는 항상 최신 상태를 반영한다.

#### 1.2 왜 표준이 필요한가

`console` 객체는 본래 어떤 공식 표준에도 정의되지 않은 비표준 API였다.
각 브라우저 벤더와 JavaScript 런타임이 독자적으로 구현했기 때문에 다음과 같은 문제가 있었다.

- 일관성 부재: 동일한 메서드가 브라우저마다 다르게 동작
- 호환성 문제: 특정 브라우저에서만 사용 가능한 메서드 존재
- 예측 불가능한 출력: 동일한 코드가 환경마다 다른 결과를 출력
- 문서화 부재: 공식 레퍼런스가 없어 개발자가 혼란을 겪음

표준화를 통해 최소한의 공통 동작을 보장하고, 개발자가 어떤 환경에서든 예측 가능한 디버깅 도구를
사용할 수 있도록 하는 것이 Console Standard의 목적이다.

#### 1.3 역사: 브라우저별 독자 구현에서 표준화까지

##### 초기 (2000년대 초반)

`console` 객체는 2006년경 Firebug에서 처음 도입되었다. Firebug는 Firefox 브라우저용
디버깅 확장 프로그램으로, 웹 개발 디버깅의 혁명을 가져왔다. Joe Hewitt가 개발한 이 도구에서
`console.log()`를 비롯한 콘솔 API가 처음 등장했다.

```javascript
// Firebug에서 처음 사용된 콘솔 로깅
console.log("Hello from Firebug!");
```

##### 각 브라우저의 독자 구현 (2006~2012)

Firebug의 성공 이후 각 브라우저 벤더가 자체 개발자 도구를 만들면서 `console` 객체를 독자적으로 구현했다.

| 브라우저 | 개발자 도구 | 도입 시기 | 특이사항 |
|---------|-----------|----------|---------|
| Firefox | Firebug (확장) | 2006 | 최초 도입, 이후 내장 도구로 전환 |
| Safari | Web Inspector | 2007 | WebKit 기반 독자 구현 |
| Chrome | DevTools | 2008 | V8 엔진의 콘솔 구현 |
| IE | F12 Developer Tools | 2009 (IE8) | 개발자 도구를 열어야만 console 객체 존재 |
| Opera | Dragonfly | 2008 | Presto 엔진 기반, 이후 Chromium으로 전환 |
| Node.js | 내장 모듈 | 2009 | 서버 사이드에서의 console 구현 |

특히 Internet Explorer의 경우 개발자 도구(F12)를 열지 않으면 `console` 객체 자체가 정의되지 않아
다음과 같은 방어 코드가 필수적이었다.

```javascript
// IE 대응을 위한 방어 코드 (2010년대 초반 흔히 사용)
if (typeof console === "undefined") {
  window.console = {
    log: function () {},
    warn: function () {},
    error: function () {},
  };
}
```

##### 표준화 움직임 (2012~2015)

브라우저 간 불일치가 심화되면서 표준화 필요성이 대두되었다. 처음에는 개별적으로 문서화 시도가
있었으나, 결국 WHATWG에서 Console Standard를 Living Standard로 관리하기로 결정했다.

##### WHATWG Console Standard 확립 (2015~현재)

WHATWG가 Console Standard를 공식 명세로 채택하면서 주요 브라우저 벤더(Google, Mozilla, Apple, Microsoft)가
합의한 공통 동작이 문서화되었다. 이후 Node.js, Deno, Bun 등 서버 사이드 런타임도 이 표준을 따르고 있다.

---

### 2. console 네임스페이스

#### 2.1 console 객체의 정의

Console Standard에 따르면 `console`은 네임스페이스 객체(namespace object) 로 정의된다.
이는 일반적인 클래스 인스턴스가 아니라, 관련 메서드들을 모아놓은 컨테이너 역할을 하는 객체이다.

Web IDL(Interface Definition Language)에서는 다음과 같이 정의된다.

```webidl
[Exposed=*]
namespace console {
  // 로깅
  undefined assert(optional boolean condition = false, any... data);
  undefined clear();
  undefined debug(any... data);
  undefined error(any... data);
  undefined info(any... data);
  undefined log(any... data);
  undefined table(optional any tabularData, optional sequence<DOMString> properties);
  undefined trace(any... data);
  undefined warn(any... data);
  undefined dir(optional any item, optional object? options);
  undefined dirxml(any... data);

  // 카운팅
  undefined count(optional DOMString label = "default");
  undefined countReset(optional DOMString label = "default");

  // 그룹
  undefined group(any... data);
  undefined groupCollapsed(any... data);
  undefined groupEnd();

  // 타이밍
  undefined time(optional DOMString label = "default");
  undefined timeLog(optional DOMString label = "default", any... data);
  undefined timeEnd(optional DOMString label = "default");
};
```

#### 2.2 `[Exposed=*]` 의 의미

Web IDL의 `[Exposed=*]` 확장 속성은 `console` 객체가 모든 전역 스코프에서 사용 가능함을 의미한다.
구체적으로 다음 환경에서 접근 가능하다.

| 환경 | 전역 객체 | 접근 방식 |
|-----|----------|----------|
| 브라우저 메인 스레드 | `Window` | `window.console` 또는 `console` |
| Web Worker | `WorkerGlobalScope` | `self.console` 또는 `console` |
| Service Worker | `ServiceWorkerGlobalScope` | `self.console` 또는 `console` |
| Node.js | `global` | `global.console` 또는 `console` |
| Deno | `globalThis` | `globalThis.console` 또는 `console` |

#### 2.3 전역 접근과 특성

`console`은 전역 객체의 속성으로 존재하므로 어디서든 직접 접근할 수 있다.

```javascript
// 다양한 접근 방식 (모두 동일한 객체를 참조)
console.log("직접 접근");
window.console.log("window를 통한 접근"); // 브라우저 환경
globalThis.console.log("globalThis를 통한 접근"); // 모든 환경

// console 객체의 타입 확인
typeof console; // "object"

// console은 네임스페이스이므로 생성자가 아니다
// new console(); // TypeError: console is not a constructor
```

#### 2.4 console 메서드의 바인딩

`console` 메서드를 변수에 할당하여 사용할 수 있다. 그러나 구현에 따라 `this` 바인딩 문제가
발생할 수 있으므로 주의가 필요하다.

```javascript
// 메서드를 변수에 할당
const log = console.log;

// 일부 환경에서는 this 바인딩 문제 없이 동작
log("정상 동작할 수 있음");

// 안전한 방법: bind 사용
const safeLog = console.log.bind(console);
safeLog("항상 안전하게 동작");

// 디스트럭처링 할당
const { log: myLog, warn: myWarn, error: myError } = console;
// 환경에 따라 동작이 다를 수 있음
```

---

### 3. 로깅 메서드

#### 3.1 console.log(...data)

가장 기본적이고 널리 사용되는 로깅 메서드이다. 임의의 개수의 인수를 받아 콘솔에 출력한다.
표준에서는 로그 레벨 "log" 으로 분류된다.

```javascript
// 기본 사용법
console.log("Hello, World!");

// 여러 인수 전달
console.log("이름:", "홍길동", "나이:", 30);
// 출력: 이름: 홍길동 나이: 30

// 다양한 타입 출력
console.log(42);                    // 숫자
console.log(true);                  // 불리언
console.log(null);                  // null
console.log(undefined);             // undefined
console.log([1, 2, 3]);             // 배열
console.log({ name: "홍길동" });     // 객체
console.log(Symbol("desc"));        // 심볼
console.log(123n);                  // BigInt

// 형식 지정자 사용
console.log("문자열: %s, 숫자: %d", "테스트", 42);
// 출력: 문자열: 테스트, 숫자: 42
```

#### 3.2 console.debug(...data)

`console.log()`와 동일하게 동작하지만, 로그 레벨이 "debug" 로 분류된다.
브라우저 개발자 도구에서 로그 레벨 필터링을 통해 debug 메시지만 표시하거나 숨길 수 있다.

```javascript
console.debug("디버그 정보: 변수 x의 값은", x);
console.debug("이 메시지는 기본적으로 숨겨질 수 있음");

// Chrome DevTools에서는 "Verbose" 레벨을 활성화해야 표시됨
console.debug("상세 디버깅 정보", { step: 1, status: "init" });
```

> 참고: Chrome DevTools에서 `console.debug()`의 출력은 기본적으로 "Verbose" 로그 레벨에
> 해당하며, 이 레벨이 비활성화되어 있으면 출력이 표시되지 않는다.

#### 3.3 console.info(...data)

정보성 메시지를 출력한다. 로그 레벨은 "info" 이다.
대부분의 브라우저에서 `console.log()`와 시각적으로 동일하게 표시되지만,
일부 환경에서는 정보 아이콘(i)이 함께 표시될 수 있다.

```javascript
console.info("애플리케이션이 시작되었습니다.");
console.info("서버 포트:", 3000);
console.info("환경:", process.env.NODE_ENV);
```

#### 3.4 console.warn(...data)

경고 메시지를 출력한다. 로그 레벨은 "warn" 이다.
대부분의 브라우저에서 노란색 배경과 경고 아이콘으로 표시된다.

```javascript
console.warn("이 API는 더 이상 사용되지 않습니다.");
console.warn("메모리 사용량이 80%를 초과했습니다:", memoryUsage);

// 사용 예: 폐기 예정(deprecated) 기능 경고
function oldMethod() {
  console.warn(
    "oldMethod()는 v3.0에서 제거됩니다. newMethod()를 사용하세요."
  );
  // 기존 로직...
}
```

#### 3.5 console.error(...data)

에러 메시지를 출력한다. 로그 레벨은 "error" 이다.
대부분의 브라우저에서 빨간색 배경과 에러 아이콘으로 표시되며, 스택 트레이스가 포함될 수 있다.

```javascript
console.error("치명적 오류가 발생했습니다!");
console.error("데이터베이스 연결 실패:", error.message);

// Error 객체 전달
try {
  throw new Error("예외 발생");
} catch (e) {
  console.error("오류 발생:", e);
  // Error 객체의 스택 트레이스가 함께 출력됨
}

// 여러 값 전달
console.error("요청 실패", { url: "/api/data", status: 500 });
```

> 주의: `console.error()`는 표준 에러(stderr)로 출력되는 경우가 많다.
> Node.js에서는 `process.stderr`에 기록되며, 이는 `console.log()`가
> `process.stdout`에 기록되는 것과 구분된다.

#### 3.6 console.assert(condition, ...data)

첫 번째 인수 `condition`이 거짓(falsy) 일 때만 나머지 인수를 에러 레벨로 출력한다.
`condition`이 참(truthy)이면 아무것도 출력하지 않는다.

표준에 따른 동작:

1. `condition`이 `true`이면 아무 동작도 하지 않고 반환한다.
2. `condition`이 `false`이면:
   - `data`가 비어 있으면 `"Assertion failed"` 메시지를 출력한다.
   - `data`의 첫 번째 항목이 문자열이면 `"Assertion failed: "` 접두사를 붙여 출력한다.
   - 그렇지 않으면 `"Assertion failed"` 를 `data` 앞에 삽입하여 출력한다.

```javascript
// 조건이 참이면 아무것도 출력되지 않음
console.assert(1 === 1, "이 메시지는 보이지 않음");

// 조건이 거짓이면 에러 메시지 출력
console.assert(1 === 2, "1은 2와 같지 않습니다!");
// 출력: Assertion failed: 1은 2와 같지 않습니다!

// 데이터 없이 사용
console.assert(false);
// 출력: Assertion failed

// 객체와 함께 사용
const user = { name: "홍길동", age: -1 };
console.assert(user.age > 0, "나이가 유효하지 않습니다:", user);
// 출력: Assertion failed: 나이가 유효하지 않습니다: {name: "홍길동", age: -1}

// 배열 길이 검증
const items = [];
console.assert(items.length > 0, "배열이 비어 있습니다!", items);
// 출력: Assertion failed: 배열이 비어 있습니다! []
```

> 중요: `console.assert()`는 프로그램 실행을 중단하지 않는다. 단순히 에러 레벨의
> 메시지를 출력할 뿐이며, 예외를 발생시키지 않는다. 진정한 단언(assertion)이 필요하면
> Node.js의 `assert` 모듈이나 직접 예외를 발생시켜야 한다.

#### 3.7 console.dir(item, options)

JavaScript 객체의 속성을 대화형 목록(interactive listing)으로 표시한다.
`console.log()`와 달리 DOM 요소도 JavaScript 객체로 표시한다.

```javascript
// 객체의 속성 나열
const person = {
  name: "홍길동",
  age: 30,
  address: {
    city: "서울",
    district: "강남구",
  },
};

console.dir(person);
// 객체의 속성이 트리 형태로 표시됨

// DOM 요소를 JavaScript 객체로 표시
const element = document.getElementById("app");
console.dir(element);
// DOM 요소의 JavaScript 속성(id, className, style 등)이 표시됨
// cf. console.log(element)는 HTML 마크업으로 표시됨

// options 매개변수 (Node.js에서 확장 지원)
console.dir(complexObject, { depth: null }); // 무한 깊이로 펼침
console.dir(complexObject, { colors: true }); // 색상 적용
```

> 참고: `options` 매개변수는 표준에서 `object?` 타입으로 선택적으로 정의되어 있지만,
> 구체적인 옵션 키는 표준에서 규정하지 않는다. Node.js의 `util.inspect()` 옵션과 동일한
> 옵션을 지원하는 것은 Node.js의 독자적 확장이다.

#### 3.8 console.dirxml(...data)

가능한 경우 인수를 XML/HTML 형태로 표시한다. XML 표현이 불가능하면
`console.log()`처럼 JavaScript 표현으로 대체한다.

```javascript
// DOM 요소를 HTML 트리로 표시
const body = document.body;
console.dirxml(body);
// <body> 요소와 하위 DOM 트리가 펼쳐져 표시됨

// XML 문서 표시
const parser = new DOMParser();
const xmlDoc = parser.parseFromString(
  "<book><title>JavaScript</title><author>홍길동</author></book>",
  "application/xml"
);
console.dirxml(xmlDoc);
// XML 트리 구조로 표시됨

// 일반 객체에는 console.log()처럼 동작
console.dirxml({ key: "value" });
// 일반 객체로 출력됨
```

#### 3.9 console.table(tabularData, properties)

데이터를 표(table) 형태로 출력한다. 배열이나 객체를 시각적으로 정렬된 표로 보여주어
데이터 구조를 빠르게 파악할 수 있게 한다.

```javascript
// 배열 표시
console.table(["사과", "바나나", "체리"]);
// ┌─────────┬────────┐
// │ (index) │ Values │
// ├─────────┼────────┤
// │    0    │ "사과"  │
// │    1    │ "바나나" │
// │    2    │ "체리"  │
// └─────────┴────────┘

// 객체 배열 표시
const users = [
  { name: "홍길동", age: 30, city: "서울" },
  { name: "김철수", age: 25, city: "부산" },
  { name: "이영희", age: 28, city: "대구" },
];
console.table(users);
// ┌─────────┬────────┬─────┬──────┐
// │ (index) │  name  │ age │ city │
// ├─────────┼────────┼─────┼──────┤
// │    0    │ "홍길동" │ 30  │ "서울" │
// │    1    │ "김철수" │ 25  │ "부산" │
// │    2    │ "이영희" │ 28  │ "대구" │
// └─────────┴────────┴─────┴──────┘

// 특정 열만 표시 (properties 매개변수 사용)
console.table(users, ["name", "city"]);
// ┌─────────┬────────┬──────┐
// │ (index) │  name  │ city │
// ├─────────┼────────┼──────┤
// │    0    │ "홍길동" │ "서울" │
// │    1    │ "김철수" │ "부산" │
// │    2    │ "이영희" │ "대구" │
// └─────────┴────────┴──────┘

// 객체를 표로 표시
const capitals = {
  한국: "서울",
  일본: "도쿄",
  중국: "베이징",
};
console.table(capitals);
// ┌─────────┬──────────┐
// │ (index) │  Values  │
// ├─────────┼──────────┤
// │  한국   │  "서울"   │
// │  일본   │  "도쿄"   │
// │  중국   │ "베이징"  │
// └─────────┴──────────┘

// 중첩 객체
const inventory = {
  과일: { 사과: 10, 바나나: 20 },
  채소: { 당근: 15, 감자: 30 },
};
console.table(inventory);
// ┌─────────┬──────┬────────┐
// │ (index) │ 사과  │ 바나나  │ ...
// ├─────────┼──────┼────────┤
// │  과일   │  10  │   20   │
// │  채소   │      │        │ ...
// └─────────┴──────┴────────┘
```

#### 3.10 console.trace(...data)

호출 스택 추적(stack trace)을 콘솔에 출력한다. 로그 레벨은 "log" 이다.
인수가 제공되면 해당 데이터도 함께 출력된다. 코드의 실행 경로를 추적하는 데 매우 유용하다.

```javascript
function outer() {
  function middle() {
    function inner() {
      console.trace("호출 스택 추적");
    }
    inner();
  }
  middle();
}

outer();
// 출력:
// 호출 스택 추적
//     at inner (script.js:4:15)
//     at middle (script.js:6:5)
//     at outer (script.js:8:3)
//     at script.js:11:1

// 인수 없이 사용
function handleClick() {
  console.trace();
  // 현재 호출 스택만 출력
}

// 여러 인수와 함께 사용
function processData(data) {
  console.trace("데이터 처리 시작:", data);
}
```

#### 3.11 console.clear()

콘솔을 비운다. 환경이 허용하는 경우 콘솔의 모든 출력을 제거하고,
일부 브라우저에서는 "Console was cleared" 같은 메시지를 표시한다.

```javascript
// 콘솔 초기화
console.clear();

// 사용 예: 주기적 갱신 시 이전 출력 제거
setInterval(() => {
  console.clear();
  console.log("현재 시각:", new Date().toLocaleTimeString());
  console.log("메모리 사용량:", performance.memory?.usedJSHeapSize);
}, 1000);
```

> 참고: Node.js에서 `console.clear()`는 stdout이 TTY(터미널)인 경우
> ANSI 이스케이프 코드를 사용하여 화면을 지운다. 파일로 리다이렉트된 경우에는
> 아무 동작도 하지 않는다.

---

### 4. 카운팅 메서드

#### 4.1 console.count(label)

특정 레이블에 대한 호출 횟수를 카운트하여 출력한다.
각 레이블별로 독립적인 카운터가 유지된다.

표준에 따른 동작:

1. `label`이 제공되지 않으면 기본값 `"default"`를 사용한다.
2. 해당 레이블에 대한 내부 카운터(count map)를 1 증가시킨다.
3. `"label: count"` 형식으로 출력한다.

```javascript
// 기본 사용법
console.count();        // default: 1
console.count();        // default: 2
console.count();        // default: 3

// 레이블 지정
console.count("loop");  // loop: 1
console.count("loop");  // loop: 2
console.count("event"); // event: 1
console.count("loop");  // loop: 3
console.count("event"); // event: 2

// 실용 예: 함수 호출 횟수 추적
function processItem(item) {
  console.count("processItem 호출");
  // 처리 로직...
}

processItem("a"); // processItem 호출: 1
processItem("b"); // processItem 호출: 2
processItem("c"); // processItem 호출: 3

// 실용 예: 이벤트 발생 횟수 추적
document.addEventListener("click", () => {
  console.count("클릭 이벤트");
});
// 클릭할 때마다:
// 클릭 이벤트: 1
// 클릭 이벤트: 2
// ...
```

#### 4.2 console.countReset(label)

특정 레이블의 카운터를 0으로 초기화한다.

표준에 따른 동작:

1. `label`이 제공되지 않으면 기본값 `"default"`를 사용한다.
2. 해당 레이블에 대한 카운터가 존재하면 0으로 초기화한다.
3. 해당 레이블에 대한 카운터가 존재하지 않으면 경고 메시지를 출력한다:
   `"Count for 'label' does not exist"`

```javascript
// 기본 사용법
console.count();           // default: 1
console.count();           // default: 2
console.countReset();      // default 카운터 초기화
console.count();           // default: 1 (다시 1부터 시작)

// 레이블 지정
console.count("api");      // api: 1
console.count("api");      // api: 2
console.count("api");      // api: 3
console.countReset("api"); // api 카운터 초기화
console.count("api");      // api: 1

// 존재하지 않는 레이블 초기화 시도
console.countReset("nonexistent");
// 경고: Count for 'nonexistent' does not exist

// 실용 예: 배치 처리에서 카운터 초기화
function processBatch(items) {
  console.countReset("batch");
  items.forEach((item) => {
    console.count("batch");
    // 처리 로직...
  });
}
```

---

### 5. 그룹 메서드

그룹 메서드는 콘솔 출력을 시각적으로 그룹화하여 관련 메시지를 계층 구조로 표시한다.
표준에서는 그룹 스택(group stack) 이라는 개념을 정의하며, 이는 중첩 가능한 그룹의
논리적 스택 구조이다.

#### 5.1 console.group(...data)

새로운 인라인 그룹을 시작한다. 이후 출력되는 모든 메시지는 들여쓰기되어 표시된다.
인수가 제공되면 그룹의 레이블로 사용된다.

```javascript
console.group("사용자 정보");
console.log("이름: 홍길동");
console.log("나이: 30");
console.log("도시: 서울");
console.groupEnd();

// 출력:
// ▼ 사용자 정보
//     이름: 홍길동
//     나이: 30
//     도시: 서울
```

#### 5.2 console.groupCollapsed(...data)

`console.group()`과 동일하지만, 그룹이 접힌 상태(collapsed) 로 시작된다.
사용자가 클릭하여 펼칠 수 있다. 대량의 디버그 정보를 출력할 때 유용하다.

```javascript
console.groupCollapsed("상세 디버그 정보");
console.log("타임스탬프:", Date.now());
console.log("요청 URL:", url);
console.log("응답 상태:", response.status);
console.log("응답 헤더:", response.headers);
console.groupEnd();

// 출력 (접힌 상태):
// ▶ 상세 디버그 정보
// (클릭하면 내용이 펼쳐짐)
```

#### 5.3 console.groupEnd()

현재 그룹을 종료하고 들여쓰기 레벨을 이전 수준으로 되돌린다.
그룹 스택에서 최상위 항목을 제거한다.

#### 5.4 중첩 그룹

그룹은 중첩하여 사용할 수 있으며, 이를 통해 복잡한 계층 구조를 표현할 수 있다.

```javascript
console.group("애플리케이션 초기화");

console.group("설정 로드");
console.log("config.json 읽기 완료");
console.log("환경 변수 파싱 완료");
console.groupEnd();

console.group("데이터베이스 연결");
console.log("호스트: localhost:5432");
console.log("데이터베이스: myapp");
console.log("연결 성공");
console.groupEnd();

console.group("서버 시작");
console.log("포트: 3000");
console.log("서버 준비 완료");
console.groupEnd();

console.groupEnd();

// 출력:
// ▼ 애플리케이션 초기화
//     ▼ 설정 로드
//         config.json 읽기 완료
//         환경 변수 파싱 완료
//     ▼ 데이터베이스 연결
//         호스트: localhost:5432
//         데이터베이스: myapp
//         연결 성공
//     ▼ 서버 시작
//         포트: 3000
//         서버 준비 완료
```

#### 5.5 그룹과 다른 메서드의 조합

```javascript
console.group("API 호출 결과");
console.info("엔드포인트: /api/users");
console.table([
  { id: 1, name: "홍길동" },
  { id: 2, name: "김철수" },
]);
console.warn("응답 시간이 2초를 초과했습니다.");
console.groupEnd();
```

---

### 6. 타이밍 메서드

#### 6.1 console.time(label)

지정된 레이블로 타이머를 시작한다. 동일한 페이지(또는 런타임)에서 최대
10,000개의 타이머를 동시에 실행할 수 있다(구현에 따라 다름).

표준에 따른 동작:

1. `label`이 제공되지 않으면 기본값 `"default"`를 사용한다.
2. 해당 레이블의 타이머가 이미 존재하면 경고 메시지를 출력한다:
   `"Timer 'label' already exists"`
3. 타이머가 존재하지 않으면 현재 시간을 기록하여 타이머 테이블(timer table)에 저장한다.

```javascript
// 기본 사용법
console.time();
// ... 어떤 작업 ...
console.timeEnd(); // default: 123.456ms

// 레이블 지정
console.time("데이터 로딩");
// ... 데이터 로딩 작업 ...
console.timeEnd("데이터 로딩"); // 데이터 로딩: 456.789ms

// 이미 존재하는 타이머 경고
console.time("중복");
console.time("중복"); // 경고: Timer '중복' already exists
console.timeEnd("중복");
```

#### 6.2 console.timeLog(label, ...data)

타이머를 종료하지 않고 현재까지의 경과 시간을 출력한다.
추가 데이터를 함께 출력할 수 있어 중간 지점의 시간을 측정하는 데 유용하다.

표준에 따른 동작:

1. `label`이 제공되지 않으면 기본값 `"default"`를 사용한다.
2. 해당 레이블의 타이머가 존재하지 않으면 경고 메시지를 출력한다:
   `"Timer 'label' does not exist"`
3. 타이머가 존재하면 경과 시간과 추가 데이터를 함께 출력한다.

```javascript
console.time("처리");

// 1단계
await fetchData();
console.timeLog("처리", "1단계: 데이터 가져오기 완료");
// 처리: 120.5ms 1단계: 데이터 가져오기 완료

// 2단계
await transformData();
console.timeLog("처리", "2단계: 데이터 변환 완료");
// 처리: 350.2ms 2단계: 데이터 변환 완료

// 3단계
await saveData();
console.timeEnd("처리");
// 처리: 512.8ms

// 존재하지 않는 타이머
console.timeLog("없는타이머");
// 경고: Timer '없는타이머' does not exist
```

#### 6.3 console.timeEnd(label)

타이머를 종료하고 총 경과 시간을 출력한다.

표준에 따른 동작:

1. `label`이 제공되지 않으면 기본값 `"default"`를 사용한다.
2. 해당 레이블의 타이머가 존재하지 않으면 경고 메시지를 출력한다:
   `"Timer 'label' does not exist"`
3. 타이머가 존재하면 경과 시간을 출력하고 타이머 테이블에서 해당 항목을 제거한다.

```javascript
// 기본 사용법
console.time("정렬");
const sorted = largeArray.sort();
console.timeEnd("정렬"); // 정렬: 24.567ms

// 여러 타이머를 동시에 사용
console.time("전체");
console.time("파싱");
const data = JSON.parse(jsonString);
console.timeEnd("파싱"); // 파싱: 2.345ms

console.time("렌더링");
render(data);
console.timeEnd("렌더링"); // 렌더링: 15.678ms

console.timeEnd("전체"); // 전체: 18.234ms

// 이미 종료된 타이머
console.time("test");
console.timeEnd("test"); // test: 0.123ms
console.timeEnd("test"); // 경고: Timer 'test' does not exist
```

#### 6.4 타이밍 메서드의 내부 구조

Console Standard에서 타이머는 내부적으로 타이머 테이블(timer table) 을 사용하여 관리된다.
이 테이블은 레이블을 키로, 시작 시간을 값으로 저장하는 맵(map) 구조이다.

```
타이머 테이블 (Timer Table)
┌──────────────┬──────────────────────┐
│    Label     │     Start Time       │
├──────────────┼──────────────────────┤
│ "default"    │ 1700000000000.000    │
│ "데이터 로딩"  │ 1700000000100.500   │
│ "렌더링"      │ 1700000000200.750   │
└──────────────┴──────────────────────┘
```

---

### 7. 포매팅

Console Standard는 문자열 형식 지정(formatting)을 위한 형식 지정자(format specifier) 를
정의한다. 첫 번째 인수가 문자열이고 형식 지정자를 포함하는 경우, 이후 인수가 해당 지정자에
치환(substitution)되어 출력된다.

#### 7.1 형식 지정자 목록

| 형식 지정자 | 설명 | 변환 방식 |
|-----------|------|---------|
| `%s` | 문자열 | `String(value)` 변환 |
| `%d` 또는 `%i` | 정수 | `parseInt(value, 10)` 변환 |
| `%f` | 부동소수점 | `parseFloat(value)` 변환 |
| `%o` | 최적화된 객체 표시 | 구현 정의(일반적으로 펼칠 수 있는 형태) |
| `%O` | 일반 객체 표시 | 구현 정의(일반적으로 JavaScript 객체로 표시) |
| `%c` | CSS 스타일 적용 | 이후 텍스트에 CSS 스타일 적용 |

#### 7.2 %s - 문자열 치환

```javascript
console.log("안녕하세요, %s님!", "홍길동");
// 출력: 안녕하세요, 홍길동님!

console.log("%s + %s = %s", 1, 2, 3);
// 출력: 1 + 2 = 3

// 객체를 %s로 변환하면 toString() 결과가 사용됨
console.log("값: %s", { toString: () => "커스텀 문자열" });
// 출력: 값: 커스텀 문자열

console.log("배열: %s", [1, 2, 3]);
// 출력: 배열: 1,2,3
```

#### 7.3 %d, %i - 정수 치환

```javascript
console.log("정수: %d", 42);
// 출력: 정수: 42

console.log("변환: %d", 3.14);
// 출력: 변환: 3 (소수점 이하 절사)

console.log("문자열을 정수로: %i", "42.9abc");
// 출력: 문자열을 정수로: 42

console.log("변환 불가: %d", "abc");
// 출력: 변환 불가: NaN

// %d와 %i는 동일하게 동작
console.log("%d와 %i 비교: %d, %i", 3.7, 3.7);
// 출력: %d와 %i 비교: 3, 3
```

#### 7.4 %f - 부동소수점 치환

```javascript
console.log("실수: %f", 3.14159);
// 출력: 실수: 3.14159

console.log("정수를 실수로: %f", 42);
// 출력: 정수를 실수로: 42

console.log("문자열: %f", "3.14abc");
// 출력: 문자열: 3.14

console.log("변환 불가: %f", "abc");
// 출력: 변환 불가: NaN
```

#### 7.5 %o, %O - 객체 치환

```javascript
const obj = { name: "홍길동", scores: [90, 85, 95] };

// %o - 최적화된 형태 (일반적으로 펼칠 수 있는 형태)
console.log("최적화: %o", obj);

// %O - 일반 JavaScript 객체 형태
console.log("일반: %O", obj);

// DOM 요소에서의 차이 (브라우저 환경)
const div = document.createElement("div");
div.id = "test";

console.log("%o", div); // DOM 노드로 표시 (HTML 형태)
console.log("%O", div); // JavaScript 객체로 표시 (속성 나열)
```

> 참고: `%o`와 `%O`의 실제 표시 방식은 구현에 따라 다르다. 표준에서는 이 둘을
> "optimally useful formatting" 과 "generic JavaScript object formatting" 으로만
> 구분하며, 정확한 표시 형태는 규정하지 않는다.

#### 7.6 %c - CSS 스타일 적용

브라우저 콘솔에서 텍스트에 CSS 스타일을 적용할 수 있다. `%c` 이후의 텍스트에
해당 CSS 스타일이 적용된다.

```javascript
// 기본 스타일 적용
console.log("%c큰 빨간 텍스트", "color: red; font-size: 24px;");

// 여러 스타일 적용
console.log(
  "%c성공%c | %c경고%c | %c오류",
  "color: green; font-weight: bold;",      // "성공"에 적용
  "",                                       // " | "에 적용 (스타일 초기화)
  "color: orange; font-weight: bold;",      // "경고"에 적용
  "",                                       // " | "에 적용 (스타일 초기화)
  "color: red; font-weight: bold;"          // "오류"에 적용
);

// 배경색과 패딩 적용
console.log(
  "%c INFO ",
  "background: #2196F3; color: white; padding: 2px 6px; border-radius: 3px;"
);

// 복잡한 스타일링
console.log(
  "%c🎉 축하합니다! %c새 버전이 릴리스되었습니다.",
  "font-size: 20px; font-weight: bold; color: #e91e63;",
  "font-size: 14px; color: #333;"
);

// 그라데이션 배경 (일부 브라우저 지원)
console.log(
  "%cGradient Text",
  "background: linear-gradient(to right, #ff6b6b, #feca57); " +
  "color: white; font-size: 18px; padding: 5px 10px;"
);
```

#### 7.7 치환 문자열(Substitution Strings)의 동작 알고리즘

Console Standard에서 정의하는 포매팅 알고리즘은 다음과 같다.

1. `data`의 첫 번째 요소를 `target`으로 설정한다.
2. `target`이 문자열이 아니면 포매팅을 수행하지 않는다.
3. `target`에서 첫 번째 형식 지정자를 찾는다.
4. 형식 지정자에 대응하는 값을 `data`의 다음 요소에서 가져온다.
5. 형식 지정자의 타입에 따라 값을 변환한다.
6. 변환된 값으로 형식 지정자를 치환한다.
7. 사용된 `data` 요소를 제거한다.
8. 더 이상 형식 지정자가 없거나 대응할 값이 없을 때까지 3~7을 반복한다.
9. 남은 `data` 요소는 공백으로 구분하여 뒤에 추가한다.

```javascript
// 치환 값이 부족한 경우
console.log("%s과 %s", "사과");
// 출력: 사과과 %s (두 번째 %s는 치환되지 않고 그대로 출력)

// 치환 값이 남는 경우
console.log("%s입니다", "홍길동", 30, "서울");
// 출력: 홍길동입니다 30 서울 (남은 값은 공백으로 구분되어 추가)

// 형식 지정자가 없는 경우
console.log("인수들:", 1, 2, 3, "끝");
// 출력: 인수들: 1 2 3 끝 (모든 인수가 공백으로 구분)

// %% (리터럴 퍼센트)는 표준에서 정의하지 않음
// 일부 구현에서는 지원할 수 있음
console.log("100%%"); // 구현에 따라 다름
```

#### 7.8 CSS 스타일의 보안 고려사항

표준은 `%c` 처리 부분을 "TODO: process %c"로 남겨두고 있어, 허용되는 CSS 속성 목록을
직접 규정하지 않는다. 아래 표는 표준의 요구사항이 아니라 Chrome 등 주요 브라우저 구현체가
보안상 이유로 실제 적용하고 있는 관행을 정리한 것이다.

| 허용되는 CSS 속성 | 설명 |
|-----------------|------|
| `background` 관련 | `background`, `background-color`, `background-image` 등 |
| `border` 관련 | `border`, `border-radius` 등 |
| `color` | 텍스트 색상 |
| `font` 관련 | `font-size`, `font-weight`, `font-style`, `font-family` 등 |
| `line-height` | 행간 |
| `margin` | 외부 여백 |
| `padding` | 내부 여백 |
| `text-decoration` | 텍스트 장식 |
| `text-transform` | 텍스트 변환 |
| `white-space` | 공백 처리 |
| `word-spacing` | 단어 간격 |
| `writing-mode` | 텍스트 방향 |

`url()`, `image()` 등 외부 리소스를 참조하는 값은 보안상 이유로 차단될 수 있다.

---

### 8. Logger 개념

#### 8.1 로그 레벨(Log Level)

Console Standard에서는 네 가지 로그 레벨 그룹을 문서화한다. 다만 표준은 이 그룹을
"common practices를 문서화하기 위한 것일 뿐, 구현체가 각 메서드에 특별한 동작을 부여하는
것을 제약하지 않는다"라고 명시하는 비규범적(informative) 안내로 규정한다. 즉 아래 표는
강제 규칙이 아니라 참고용 분류다.

| 로그 레벨 | 해당 메서드 | 의미 |
|----------|-----------|------|
| `"log"` | `console.log()`, `console.trace()`, `console.dir()`, `console.dirxml()`, `console.group()`, `console.groupCollapsed()`, `console.debug()`, `console.timeLog()` | 일반 로그 메시지 |
| `"info"` | `console.info()`, `console.count()`, `console.timeEnd()` | 정보성 메시지 |
| `"warn"` | `console.warn()`, `console.countReset()` | 경고 메시지 |
| `"error"` | `console.error()`, `console.assert()` (실패 시) | 에러 메시지 |

#### 8.2 Logger 추상 연산

Console Standard에서는 Logger 라는 추상 연산(abstract operation)을 정의한다.
대부분의 로깅 메서드는 이 Logger 연산을 호출하여 동작한다.

Logger(logLevel, args) 추상 연산의 단계:

1. `args`가 비어 있으면 아무 동작 없이 반환한다.
2. `args`의 첫 번째 요소가 문자열이고 형식 지정자를 포함하면 포매팅을 수행한다.
3. 결과를 Printer 추상 연산에 전달한다.

```
Logger("log", ["Hello, %s!", "World"])
  → 포매팅 수행: "Hello, World!"
  → Printer("log", ["Hello, World!"])에 전달
```

#### 8.3 Printer 추상 연산

Printer는 Console Standard에서 핵심적인 추상 연산이다.
실제로 콘솔에 데이터를 출력하는 최종 단계를 담당한다.

Printer(logLevel, args, options) 추상 연산:

- `logLevel`: `"log"`, `"info"`, `"warn"`, `"error"` 중 하나
- `args`: 출력할 데이터의 리스트
- `options`: 선택적 옵션 (현재는 그룹 스택 관련)

표준에서 Printer의 정의:

> "구현 정의(implementation-defined)된 방식으로 args를 logLevel에 맞게 사용자에게 표시한다."

이는 의도적으로 모호하게 정의된 것이다. Printer의 실제 동작은 구현체(브라우저, Node.js 등)가
결정한다. 표준은 다음 사항만 요구한다:

1. 출력은 사람이 읽을 수 있어야 한다.
2. 로그 레벨에 따라 적절히 구분되어야 한다.
3. 그룹 스택의 깊이에 따라 적절히 들여쓰기되어야 한다.

#### 8.4 Printer와 로그 레벨의 관계

```
console.log("msg")     → Logger("log", ["msg"])     → Printer("log", ["msg"])
console.info("msg")    → Logger("info", ["msg"])    → Printer("info", ["msg"])
console.warn("msg")    → Logger("warn", ["msg"])    → Printer("warn", ["msg"])
console.error("msg")   → Logger("error", ["msg"])   → Printer("error", ["msg"])
console.debug("msg")   → Logger("debug", ["msg"])   → Printer("debug", ["msg"])
```

#### 8.5 각 메서드의 내부 동작 흐름

표준에서 각 메서드가 어떻게 Logger/Printer를 호출하는지 정리하면 다음과 같다.

##### console.log(...data) / console.debug(...data) / console.info(...data) / console.warn(...data) / console.error(...data)

```
1. Logger(logLevel, data) 호출
2. Logger 내부:
   a. data가 비어 있으면 반환
   b. 첫 번째 요소가 문자열이면 포매팅 수행
   c. Printer(logLevel, data) 호출
```

##### console.assert(condition, ...data)

```
1. condition이 true이면 반환
2. data가 비어 있으면:
   msg = "Assertion failed"
3. data[0]이 문자열이면:
   data[0] = "Assertion failed: " + data[0]
4. 그렇지 않으면:
   data 앞에 "Assertion failed" 삽입
5. Logger("assert", data) 호출
```

##### console.trace(...data)

```
1. 포매팅을 수행한다 (data에 대해).
2. 결과에 호출 스택(stack trace)을 추가한다.
3. Printer("trace", result) 호출
```

---

### 9. console의 동작 보장 범위

#### 9.1 표준에서 정의하는 것

Console Standard가 명확히 정의하는 사항은 다음과 같다.

##### 메서드 시그니처

각 메서드의 이름, 매개변수, 반환 타입이 정의되어 있다.
모든 메서드는 `undefined`를 반환한다.

```javascript
// 모든 console 메서드는 undefined를 반환
const result = console.log("test");
console.log(result); // undefined
```

##### 형식 지정자의 변환 규칙

`%s`, `%d`, `%i`, `%f`의 변환 방식이 명확히 정의되어 있다.

```javascript
// 표준에서 보장하는 변환
console.log("%d", "42abc");  // 반드시 42로 변환
console.log("%f", "3.14xy"); // 반드시 3.14로 변환
console.log("%s", 42);       // 반드시 "42"로 변환
```

##### 메서드의 의미론적 동작

- `console.assert()`는 조건이 거짓일 때만 출력한다.
- `console.count()`는 호출 횟수를 추적한다.
- `console.time()`/`console.timeEnd()`는 경과 시간을 측정한다.
- `console.group()`/`console.groupEnd()`는 그룹화를 제공한다.

##### 카운터 및 타이머의 상태 관리

- count map과 timer table의 존재와 기본 동작이 정의되어 있다.
- 기본 레이블이 `"default"`임이 명시되어 있다.
- 존재하지 않는 카운터/타이머에 대한 경고 메시지가 정의되어 있다.

#### 9.2 표준에서 정의하지 않는 것

Console Standard가 의도적으로 구현에 맡기는 사항은 다음과 같다.

##### 출력 형태와 시각적 표현

표준은 데이터가 "어떻게 보여야 하는지"를 규정하지 않는다.

```javascript
// 객체의 출력 형태는 구현마다 다름
console.log({ a: 1, b: 2 });
// Chrome:  {a: 1, b: 2}
// Firefox: Object { a: 1, b: 2 }
// Node.js: { a: 1, b: 2 }
```

##### 출력 대상(destination)

표준은 메시지가 어디에 출력되는지 규정하지 않는다.

- 브라우저: 개발자 도구 콘솔 패널
- Node.js: stdout/stderr
- 기타 환경: 로그 파일, 원격 서버 등

##### 객체의 대화형(interactive) 표현

객체를 펼쳐볼 수 있는지, 클릭하여 탐색할 수 있는지 등은 구현에 달려 있다.

##### 비동기 동작 여부

```javascript
// console.log()가 동기적인지 비동기적인지는 보장되지 않음
const obj = { value: 1 };
console.log(obj);
obj.value = 2;
// 출력이 { value: 1 }일 수도, { value: 2 }일 수도 있음
// (비동기 구현에서는 출력 시점에 이미 값이 변경되었을 수 있음)
```

> 중요: 이 문제는 실무에서 매우 중요하다. 브라우저 콘솔에서 객체를 로깅할 때,
> 출력 시점이 아닌 펼치는 시점의 값이 표시될 수 있다. 이를 방지하려면
> `JSON.parse(JSON.stringify(obj))` 또는 `structuredClone(obj)`로 깊은 복사를 사용한다.

##### %o와 %O의 구체적 차이

두 지정자의 구체적인 표시 방식은 구현에 맡겨져 있다.

##### console.clear()의 동작

콘솔을 비우는 구체적인 방식(화면 전체 지우기, 구분선 삽입 등)은 구현에 따라 다르다.

##### 에러 메시지의 정확한 문구

경고 및 에러 메시지의 구체적인 텍스트는 구현마다 다를 수 있다.

#### 9.3 Side Effect 관련 주의사항

표준은 console 메서드가 프로그램의 실행에 영향을 미치는 부작용(side effect)을
발생시켜서는 안 된다고 암묵적으로 가정한다. 그러나 다음과 같은 상황에서는
부작용이 발생할 수 있다.

```javascript
// getter가 있는 객체를 로깅할 때
const sneaky = {
  get value() {
    sideEffectCounter++;
    return 42;
  },
};

// console.log(sneaky)가 getter를 호출할 수 있음
// 이는 구현에 따라 다르며, 표준에서 규정하지 않음
console.log(sneaky);

// toString()이 부작용을 가진 경우
const tricky = {
  toString() {
    document.title = "changed!";
    return "tricky";
  },
};

console.log("%s", tricky); // toString()이 호출되면서 부작용 발생
```

---

### 10. 실용 예제

#### 10.1 디버깅 기법

##### 조건부 로깅 헬퍼

```javascript
// 환경에 따른 조건부 로깅
class Logger {
  constructor(options = {}) {
    this.enabled = options.enabled ?? true;
    this.prefix = options.prefix ?? "";
    this.level = options.level ?? "debug"; // debug, info, warn, error
  }

  #levels = { debug: 0, info: 1, warn: 2, error: 3 };

  #shouldLog(level) {
    return this.enabled && this.#levels[level] >= this.#levels[this.level];
  }

  debug(...args) {
    if (this.#shouldLog("debug")) {
      console.debug(`[${this.prefix}][DEBUG]`, ...args);
    }
  }

  info(...args) {
    if (this.#shouldLog("info")) {
      console.info(`[${this.prefix}][INFO]`, ...args);
    }
  }

  warn(...args) {
    if (this.#shouldLog("warn")) {
      console.warn(`[${this.prefix}][WARN]`, ...args);
    }
  }

  error(...args) {
    if (this.#shouldLog("error")) {
      console.error(`[${this.prefix}][ERROR]`, ...args);
    }
  }
}

// 사용 예
const apiLogger = new Logger({ prefix: "API", level: "info" });
apiLogger.debug("요청 상세 정보:", requestData); // 출력되지 않음
apiLogger.info("GET /api/users");                // 출력됨
apiLogger.error("500 Internal Server Error");    // 출력됨

const devLogger = new Logger({
  prefix: "DEV",
  enabled: process.env.NODE_ENV !== "production",
});
devLogger.debug("이 메시지는 프로덕션에서는 보이지 않음");
```

##### 함수 호출 추적 데코레이터

```javascript
// 함수 호출을 자동으로 추적하는 유틸리티
function traceFunction(fn, name) {
  return function (...args) {
    const fnName = name || fn.name || "anonymous";
    console.group(`${fnName}(${args.map(JSON.stringify).join(", ")})`);
    console.time(fnName);

    try {
      const result = fn.apply(this, args);

      // Promise 처리
      if (result instanceof Promise) {
        return result
          .then((value) => {
            console.log("반환값 (resolved):", value);
            console.timeEnd(fnName);
            console.groupEnd();
            return value;
          })
          .catch((error) => {
            console.error("에러 (rejected):", error);
            console.timeEnd(fnName);
            console.groupEnd();
            throw error;
          });
      }

      console.log("반환값:", result);
      console.timeEnd(fnName);
      console.groupEnd();
      return result;
    } catch (error) {
      console.error("예외 발생:", error);
      console.timeEnd(fnName);
      console.groupEnd();
      throw error;
    }
  };
}

// 사용 예
const add = traceFunction((a, b) => a + b, "add");
add(3, 5);
// ▼ add(3, 5)
//     반환값: 8
//     add: 0.05ms

const fetchUser = traceFunction(async (id) => {
  const response = await fetch(`/api/users/${id}`);
  return response.json();
}, "fetchUser");
await fetchUser(123);
// ▼ fetchUser(123)
//     반환값 (resolved): {id: 123, name: "홍길동"}
//     fetchUser: 150ms
```

##### 객체 변경 감지

```javascript
// Proxy를 활용한 객체 변경 로깅
function watchObject(obj, label = "Object") {
  return new Proxy(obj, {
    set(target, property, value) {
      const oldValue = target[property];
      console.groupCollapsed(
        `%c${label}.${String(property)} 변경`,
        "color: #2196F3; font-weight: bold;"
      );
      console.log("이전 값:", oldValue);
      console.log("새 값:", value);
      console.trace("변경 위치");
      console.groupEnd();

      target[property] = value;
      return true;
    },
    deleteProperty(target, property) {
      console.warn(`${label}.${String(property)} 삭제됨`);
      delete target[property];
      return true;
    },
  });
}

// 사용 예
const state = watchObject({ count: 0, name: "초기값" }, "AppState");
state.count = 1;
// ▶ AppState.count 변경
//     이전 값: 0
//     새 값: 1
//     (스택 트레이스)

state.name = "변경됨";
// ▶ AppState.name 변경
//     이전 값: 초기값
//     새 값: 변경됨
//     (스택 트레이스)
```

#### 10.2 성능 측정

##### 함수 실행 시간 비교

```javascript
// 여러 구현의 성능 비교
function benchmark(label, fn, iterations = 1000) {
  // 워밍업
  for (let i = 0; i < 10; i++) fn();

  console.time(label);
  for (let i = 0; i < iterations; i++) {
    fn();
  }
  console.timeEnd(label);
}

// 문자열 결합 방식 비교
const words = Array.from({ length: 1000 }, (_, i) => `word${i}`);

benchmark("Array.join", () => {
  words.join(" ");
});

benchmark("+ 연산자", () => {
  let result = "";
  for (const word of words) {
    result += word + " ";
  }
});

benchmark("템플릿 리터럴", () => {
  let result = "";
  for (const word of words) {
    result = `${result}${word} `;
  }
});

// 출력 예:
// Array.join: 2.345ms
// + 연산자: 15.678ms
// 템플릿 리터럴: 18.234ms
```

##### 단계별 성능 측정

```javascript
// 복잡한 프로세스의 단계별 성능 측정
async function processDataPipeline(rawData) {
  console.group("데이터 파이프라인");
  console.time("전체 파이프라인");

  // 1단계: 데이터 검증
  console.time("1. 데이터 검증");
  const validated = validateData(rawData);
  console.timeEnd("1. 데이터 검증");
  console.log(`  검증 통과: ${validated.length}건`);

  // 2단계: 데이터 변환
  console.time("2. 데이터 변환");
  const transformed = transformData(validated);
  console.timeEnd("2. 데이터 변환");
  console.log(`  변환 완료: ${transformed.length}건`);

  // 3단계: 중복 제거
  console.time("3. 중복 제거");
  const deduplicated = deduplicateData(transformed);
  console.timeEnd("3. 중복 제거");
  console.log(`  중복 제거 후: ${deduplicated.length}건`);

  // 4단계: 데이터 저장
  console.time("4. 데이터 저장");
  await saveData(deduplicated);
  console.timeEnd("4. 데이터 저장");

  console.timeEnd("전체 파이프라인");
  console.groupEnd();
}

// 출력:
// ▼ 데이터 파이프라인
//     1. 데이터 검증: 5.2ms
//       검증 통과: 950건
//     2. 데이터 변환: 12.8ms
//       변환 완료: 950건
//     3. 중복 제거: 3.4ms
//       중복 제거 후: 875건
//     4. 데이터 저장: 234.5ms
//     전체 파이프라인: 256.1ms
```

##### 메모리 사용량 추적 (비표준이지만 유용)

```javascript
// Chrome 전용: performance.memory API와 결합
function logMemoryUsage(label) {
  if (performance.memory) {
    console.table({
      [label]: {
        "사용 중 (MB)": (
          performance.memory.usedJSHeapSize /
          1024 /
          1024
        ).toFixed(2),
        "전체 할당 (MB)": (
          performance.memory.totalJSHeapSize /
          1024 /
          1024
        ).toFixed(2),
        "제한 (MB)": (
          performance.memory.jsHeapSizeLimit /
          1024 /
          1024
        ).toFixed(2),
      },
    });
  }
}

logMemoryUsage("초기 상태");
// 대용량 데이터 처리...
logMemoryUsage("처리 후");
```

#### 10.3 조건부 로깅

##### 환경 기반 로깅

```javascript
// 환경 변수를 활용한 조건부 로깅 시스템
const LOG_LEVELS = Object.freeze({
  SILENT: 0,
  ERROR: 1,
  WARN: 2,
  INFO: 3,
  DEBUG: 4,
  TRACE: 5,
});

function createLogger(namespace, level = LOG_LEVELS.INFO) {
  const isEnabled = (targetLevel) => level >= targetLevel;

  // 프로덕션 환경에서는 모든 로깅 비활성화
  if (typeof process !== "undefined" && process.env.NODE_ENV === "production") {
    return {
      error: () => {},
      warn: () => {},
      info: () => {},
      debug: () => {},
      trace: () => {},
    };
  }

  return {
    error(...args) {
      if (isEnabled(LOG_LEVELS.ERROR)) {
        console.error(`[${namespace}]`, ...args);
      }
    },
    warn(...args) {
      if (isEnabled(LOG_LEVELS.WARN)) {
        console.warn(`[${namespace}]`, ...args);
      }
    },
    info(...args) {
      if (isEnabled(LOG_LEVELS.INFO)) {
        console.info(`[${namespace}]`, ...args);
      }
    },
    debug(...args) {
      if (isEnabled(LOG_LEVELS.DEBUG)) {
        console.debug(`[${namespace}]`, ...args);
      }
    },
    trace(...args) {
      if (isEnabled(LOG_LEVELS.TRACE)) {
        console.trace(`[${namespace}]`, ...args);
      }
    },
  };
}

// 모듈별 로거 생성
const authLogger = createLogger("Auth", LOG_LEVELS.DEBUG);
const dbLogger = createLogger("DB", LOG_LEVELS.WARN);
const apiLogger = createLogger("API", LOG_LEVELS.INFO);

authLogger.debug("토큰 검증 시작");       // 출력됨 (DEBUG >= DEBUG)
authLogger.info("로그인 성공");           // 출력됨 (DEBUG >= INFO)
dbLogger.info("쿼리 실행");              // 출력되지 않음 (WARN < INFO)
dbLogger.warn("느린 쿼리 감지");          // 출력됨 (WARN >= WARN)
apiLogger.debug("요청 상세 정보");        // 출력되지 않음 (INFO < DEBUG)
apiLogger.info("GET /api/users - 200"); // 출력됨 (INFO >= INFO)
```

##### console.assert()를 활용한 방어적 프로그래밍

```javascript
// 함수 인수 검증
function divide(a, b) {
  console.assert(typeof a === "number", "a는 숫자여야 합니다:", a);
  console.assert(typeof b === "number", "b는 숫자여야 합니다:", b);
  console.assert(b !== 0, "0으로 나눌 수 없습니다!");

  return a / b;
}

// 상태 불변 조건 검증
function updateState(state, action) {
  console.assert(state !== null, "state가 null입니다!");
  console.assert(
    typeof action.type === "string",
    "action.type이 문자열이 아닙니다:",
    action
  );

  const newState = reducer(state, action);

  console.assert(
    newState !== undefined,
    "reducer가 undefined를 반환했습니다! action:",
    action
  );

  return newState;
}

// 배열/컬렉션 검증
function processUsers(users) {
  console.assert(Array.isArray(users), "users는 배열이어야 합니다:", users);
  console.assert(users.length > 0, "users 배열이 비어 있습니다!");

  users.forEach((user, index) => {
    console.assert(
      user.id != null,
      `users[${index}]에 id가 없습니다:`,
      user
    );
    console.assert(
      typeof user.name === "string",
      `users[${index}].name이 유효하지 않습니다:`,
      user
    );
  });
}
```

#### 10.4 객체 검사

##### 깊은 객체 검사

```javascript
// console.dir vs console.log 비교
const complexObj = {
  name: "프로젝트",
  config: {
    database: {
      host: "localhost",
      port: 5432,
      credentials: {
        username: "admin",
        password: "",
      },
    },
    server: {
      port: 3000,
      middleware: [
        { name: "cors", enabled: true },
        { name: "compression", enabled: false },
      ],
    },
  },
};

// console.log - 문자열 표현
console.log("log:", complexObj);

// console.dir - 객체 속성 탐색 가능
console.dir(complexObj);

// console.table - 평면적 구조에 적합
console.table(complexObj.config.server.middleware);
// ┌─────────┬───────────────┬─────────┐
// │ (index) │     name      │ enabled │
// ├─────────┼───────────────┼─────────┤
// │    0    │ "cors"        │  true   │
// │    1    │ "compression" │  false  │
// └─────────┴───────────────┴─────────┘

// JSON 형태로 정리된 출력
console.log(JSON.stringify(complexObj, null, 2));
```

##### 안전한 객체 로깅 (참조 문제 방지)

```javascript
// 문제: 객체 참조로 인한 불일치
const data = { value: 1 };
console.log(data);       // 출력 시점에 따라 { value: 1 } 또는 { value: 2 }
data.value = 2;

// 해결 1: 깊은 복사로 스냅샷 저장
console.log("스냅샷:", JSON.parse(JSON.stringify(data)));

// 해결 2: structuredClone 사용 (모던 환경)
console.log("스냅샷:", structuredClone(data));

// 해결 3: 개별 값 로깅
console.log("value:", data.value); // 원시 값은 참조 문제 없음

// 해결 4: 전개 연산자 (얕은 복사)
console.log("얕은 복사:", { ...data });

// 유틸리티 함수
function logSnapshot(label, obj) {
  try {
    console.log(label, JSON.parse(JSON.stringify(obj)));
  } catch (e) {
    // 순환 참조 등으로 JSON 직렬화 실패 시
    console.log(label, obj);
    console.warn("(참조 값일 수 있음 - 직렬화 실패)");
  }
}
```

##### DOM 요소 검사

```javascript
// DOM 요소의 다양한 검사 방법

const element = document.querySelector("#app");

// HTML 마크업으로 보기
console.log(element);
// <div id="app" class="container">...</div>

// JavaScript 객체로 보기
console.dir(element);
// div#app.container
//   id: "app"
//   className: "container"
//   childNodes: NodeList(3)
//   style: CSSStyleDeclaration
//   ...

// XML/HTML 트리로 보기
console.dirxml(element);
// <div id="app" class="container">
//   <h1>제목</h1>
//   <p>내용</p>
// </div>

// 속성만 추출하여 표시
const attrs = {};
for (const attr of element.attributes) {
  attrs[attr.name] = attr.value;
}
console.table(attrs);

// 계산된 스타일 검사
const styles = getComputedStyle(element);
console.log("폰트:", styles.fontFamily);
console.log("색상:", styles.color);
console.log("너비:", styles.width);
```

#### 10.5 콘솔 스타일링 패턴

```javascript
// 커스텀 배지 시스템
const badge = (text, bgColor, textColor = "white") =>
  [`%c ${text} `, `background: ${bgColor}; color: ${textColor}; padding: 2px 6px; border-radius: 3px; font-weight: bold;`];

console.log(...badge("SUCCESS", "#4CAF50"), "작업이 완료되었습니다.");
console.log(...badge("WARNING", "#FF9800"), "메모리 사용량이 높습니다.");
console.log(...badge("ERROR", "#F44336"), "연결이 끊어졌습니다.");
console.log(...badge("INFO", "#2196F3"), "서버가 시작되었습니다.");

// 이모지와 조합한 로거
const styled = {
  success: (msg) =>
    console.log(
      `%c ${msg}`,
      "color: #4CAF50; font-weight: bold;"
    ),
  error: (msg) =>
    console.log(
      `%c ${msg}`,
      "color: #F44336; font-weight: bold;"
    ),
  warning: (msg) =>
    console.log(
      `%c ${msg}`,
      "color: #FF9800; font-weight: bold;"
    ),
  info: (msg) =>
    console.log(
      `%c ${msg}`,
      "color: #2196F3; font-weight: bold;"
    ),
};

styled.success("배포가 완료되었습니다!");
styled.error("빌드에 실패했습니다.");
styled.warning("의존성 버전을 확인하세요.");
styled.info("새 업데이트가 있습니다.");
```

#### 10.6 종합 디버깅 유틸리티

```javascript
// 종합적인 디버깅 유틸리티 클래스
class DevConsole {
  static #startTimes = new Map();

  // 구분선 출력
  static separator(label = "") {
    if (label) {
      console.log(`%c── ${label} ${"─".repeat(40)}`, "color: #888;");
    } else {
      console.log(`%c${"─".repeat(50)}`, "color: #888;");
    }
  }

  // 네트워크 요청 로깅
  static async fetchWithLog(url, options = {}) {
    const id = Math.random().toString(36).substring(2, 8);
    const method = options.method || "GET";

    console.groupCollapsed(
      `%c${method}%c ${url}`,
      `background: #2196F3; color: white; padding: 1px 6px; border-radius: 3px;`,
      `color: inherit;`
    );

    console.time(`요청 ${id}`);
    console.log("옵션:", options);

    try {
      const response = await fetch(url, options);
      const data = await response.json();

      console.log("상태:", response.status, response.statusText);
      console.log("응답:", data);
      console.timeEnd(`요청 ${id}`);
      console.groupEnd();

      return data;
    } catch (error) {
      console.error("요청 실패:", error.message);
      console.timeEnd(`요청 ${id}`);
      console.groupEnd();
      throw error;
    }
  }

  // 상태 변경 추적
  static trackState(initialState, name = "State") {
    let state = { ...initialState };
    const history = [{ action: "INIT", state: { ...state }, timestamp: Date.now() }];

    return {
      get() {
        return { ...state };
      },
      set(updates) {
        const previous = { ...state };
        state = { ...state, ...updates };

        history.push({
          action: "UPDATE",
          changes: updates,
          previous,
          current: { ...state },
          timestamp: Date.now(),
        });

        console.groupCollapsed(
          `%c${name}%c 상태 변경`,
          "background: #9C27B0; color: white; padding: 1px 6px; border-radius: 3px;",
          "color: inherit;"
        );
        console.log("이전:", previous);
        console.log("변경:", updates);
        console.log("현재:", { ...state });
        console.groupEnd();
      },
      getHistory() {
        console.table(
          history.map((entry) => ({
            action: entry.action,
            timestamp: new Date(entry.timestamp).toISOString(),
            changes: JSON.stringify(entry.changes || "N/A"),
          }))
        );
        return [...history];
      },
    };
  }

  // 실행 시간 측정 데코레이터
  static measure(target, propertyKey, descriptor) {
    const original = descriptor.value;
    descriptor.value = function (...args) {
      const label = `${target.constructor.name}.${propertyKey}`;
      console.time(label);
      const result = original.apply(this, args);

      if (result instanceof Promise) {
        return result.finally(() => console.timeEnd(label));
      }

      console.timeEnd(label);
      return result;
    };
    return descriptor;
  }
}

// 사용 예
DevConsole.separator("애플리케이션 시작");

const appState = DevConsole.trackState(
  { user: null, theme: "light", language: "ko" },
  "App"
);

appState.set({ user: { name: "홍길동" } });
appState.set({ theme: "dark" });
appState.set({ language: "en" });

appState.getHistory();

DevConsole.separator("끝");
```

---

### 참고 자료

- [WHATWG Console Standard](https://console.spec.whatwg.org/) - 공식 표준 문서
- [MDN Web Docs - Console API](https://developer.mozilla.org/ko/docs/Web/API/Console_API) - MDN 참고 문서
- [Chrome DevTools Console](https://developer.chrome.com/docs/devtools/console/) - Chrome 개발자 도구 문서
- [Node.js Console](https://nodejs.org/api/console.html) - Node.js 공식 문서
