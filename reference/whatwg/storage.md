# Storage Standard 상세 가이드

## 목차

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

## 1. 개요

### 웹 스토리지 관리 표준이란

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

### 핵심 개념

| 개념 | 설명 |
|------|------|
| Storage shelf | 출처(origin)별 스토리지 컨테이너 |
| Storage bucket | 스토리지 선반 내의 논리적 그룹 |
| Storage bottle | 스토리지 버킷 내의 개별 API 영역 |
| Quota | 출처별 스토리지 사용량 제한 |
| Persistence | 데이터의 지속성(best-effort vs persistent) |
| Partitioning | 사이트 간 스토리지 격리 |

### 스토리지 계층 구조

```
Storage shelf (출처당 하나)
    └── Storage bucket ("default" 및 기타)
         ├── Storage bottle (localStorage)
         ├── Storage bottle (IndexedDB)
         ├── Storage bottle (Cache API)
         └── Storage bottle (OPFS)
```

---

## 2. 스토리지 유형

### 2.1 Best-effort 스토리지

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

### 2.2 Persistent 스토리지

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

### 2.3 두 유형 비교

| 특성 | Best-effort | Persistent |
|------|-------------|-----------|
| 기본 상태 | 기본값 | 명시적 요청 필요 |
| 자동 삭제 | 가능 (저장 공간 부족 시) | 불가 (사용자만 삭제 가능) |
| 요청 방법 | 별도 요청 불필요 | `navigator.storage.persist()` |
| 삭제 순서 | LRU 기반 (가장 오래 미사용) | 삭제 대상에서 제외 |
| 사용 사례 | 캐시, 임시 데이터 | 사용자 생성 문서, 중요 데이터 |

### 2.4 Persistent 스토리지 부여 조건

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

## 3. StorageManager API

### 3.1 navigator.storage 인터페이스

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

### 3.2 navigator.storage.estimate()

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

### 3.3 navigator.storage.persist()

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

### 3.4 navigator.storage.persisted()

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

### 3.5 navigator.storage.getDirectory() (OPFS)

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

## 4. 스토리지 쿼터

### 4.1 쿼터 개념

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

### 4.2 브라우저별 쿼터 정책

| 브라우저 | 전체 쿼터 | 출처별 쿼터 | 비고 |
|----------|----------|------------|------|
| Chrome | 디스크의 ~60% | 전체 풀의 ~60% | 동적 계산 |
| Firefox | 디스크의 ~50% | 최대 2GB (기본) | group.limit 설정 가능 |
| Safari | 디스크의 ~50% | ~1GB | ITP 제한 있음 |
| Edge | Chrome과 동일 | Chrome과 동일 | Chromium 기반 |

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

### 4.3 쿼터 초과 시 동작

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

### 4.4 API별 쿼터 영향

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

## 5. 스토리지 버킷

### 5.1 Storage Buckets 개념

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

### 5.2 Storage Buckets API (제안 단계)

```javascript
// Storage Buckets API (아직 모든 브라우저에서 지원되지 않음)

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

### 5.3 버킷의 durability 옵션

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

## 6. 스토리지 압력

### 6.1 스토리지 압력이란

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

### 6.2 삭제 우선순위

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

### 6.3 스토리지 압력 대응 전략

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

## 7. 사이트별 스토리지 격리

### 7.1 스토리지 파티셔닝

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

### 7.2 파티셔닝 키

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

### 7.3 파티셔닝의 영향을 받는 API

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

### 7.4 Storage Access API

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

## 8. 관련 스토리지 API

### 8.1 localStorage

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

### 8.2 sessionStorage

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

### 8.3 IndexedDB

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

### 8.4 Cache API

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

### 8.5 Origin Private File System (OPFS)

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

### 8.6 스토리지 API 비교

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

## 9. 스토리지 관리 전략

### 9.1 스토리지 사용량 모니터링

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

### 9.2 스토리지 전략 패턴

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

## 10. 실용 예제

### 10.1 스토리지 대시보드

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

### 10.2 오프라인 데이터 관리자

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

## 11. 브라우저 호환성

### 11.1 StorageManager API 지원 현황

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| `navigator.storage` | 55+ | 57+ | 15.2+ | 79+ |
| `estimate()` | 55+ | 57+ | 17+ | 79+ |
| `persist()` | 52+ | 55+ | 15.2+ | 79+ |
| `persisted()` | 52+ | 55+ | 15.2+ | 79+ |
| `getDirectory()` (OPFS) | 86+ | 111+ | 15.2+ | 86+ |
| Storage Buckets | 제한적 | 미지원 | 미지원 | 제한적 |

### 11.2 기능 감지

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

## 12. 보안 및 개인정보 고려사항

### 12.1 보안 컨텍스트

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

### 12.2 스토리지를 통한 핑거프린팅 방지

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

### 12.3 데이터 보호 권장사항

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

## 참고 자료

- [Storage Standard (WHATWG)](https://storage.spec.whatwg.org/)
- [MDN - StorageManager](https://developer.mozilla.org/ko/docs/Web/API/StorageManager)
- [MDN - Storage API](https://developer.mozilla.org/ko/docs/Web/API/Storage_API)
- [MDN - IndexedDB API](https://developer.mozilla.org/ko/docs/Web/API/IndexedDB_API)
- [MDN - Cache API](https://developer.mozilla.org/ko/docs/Web/API/Cache)
- [web.dev - Storage for the web](https://web.dev/storage-for-the-web/)
- [Storage Buckets API Explainer](https://github.com/nicjansma/nicjansma.github.io/blob/main/nicj.net/bsky/2024/storage-buckets.md)
