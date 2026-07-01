# Notifications API Standard 상세 가이드

## 목차

1. [개요](#1-개요)
2. [권한 모델](#2-권한-모델)
3. [Notification 인터페이스](#3-notification-인터페이스)
4. [Notification 옵션 상세](#4-notification-옵션-상세)
5. [이벤트 핸들러](#5-이벤트-핸들러)
6. [Service Worker에서의 알림](#6-service-worker에서의-알림)
7. [NotificationEvent](#7-notificationevent)
8. [알림 교체와 액션](#8-알림-교체와-액션)
9. [알림 생명주기](#9-알림-생명주기)
10. [보안 및 개인정보 고려사항](#10-보안-및-개인정보-고려사항)
11. [실용 예제](#11-실용-예제)
12. [브라우저 호환성 및 제한사항](#12-브라우저-호환성-및-제한사항)

---

## 1. 개요

### 웹 알림이란

Notifications API는 웹 애플리케이션이 시스템 수준의 알림을 표시할 수 있게 해주는 WHATWG Living Standard이다. 이 알림은 브라우저 탭 밖에서도 사용자에게 표시되며, 운영체제의 네이티브 알림 시스템과 통합된다.

```
[웹 알림의 구조]

┌─────────────────────────────────────────┐
│  ┌───┐  알림 제목 (title)               │
│  │아이│  알림 본문 (body)               │
│  │콘 │                                  │
│  └───┘  [액션1]  [액션2]               │
│                            앱 이름/출처  │
└─────────────────────────────────────────┘
```

### 두 가지 알림 컨텍스트

| 컨텍스트 | 설명 | API |
|----------|------|-----|
| 페이지 컨텍스트 | 웹 페이지에서 직접 생성 | `new Notification()` |
| Service Worker 컨텍스트 | 백그라운드에서 생성 | `registration.showNotification()` |

```javascript
// 페이지 컨텍스트 알림
const notification = new Notification('안녕하세요', {
    body: '새 메시지가 도착했습니다.'
});

// Service Worker 컨텍스트 알림
self.registration.showNotification('안녕하세요', {
    body: '새 메시지가 도착했습니다.'
});
```

---

## 2. 권한 모델

### 2.1 권한 상태

Notifications API는 3단계 권한 모델을 사용한다.

| 상태 | 값 | 설명 |
|------|-----|------|
| 기본 | `"default"` | 사용자가 아직 선택하지 않음. `"denied"`와 동일하게 동작 |
| 허용 | `"granted"` | 사용자가 알림을 허용함 |
| 거부 | `"denied"` | 사용자가 알림을 거부함 |

```javascript
// 현재 권한 상태 확인
console.log(Notification.permission);
// "default", "granted", 또는 "denied"
```

### 2.2 Notification.requestPermission()

```javascript
// Promise 기반 (현대적 방법)
Notification.requestPermission().then(function(permission) {
    console.log('권한 상태:', permission);
    // "granted", "denied", 또는 "default"

    if (permission === 'granted') {
        new Notification('권한이 허용되었습니다!');
    }
});

// async/await 사용
async function requestNotificationPermission() {
    try {
        const permission = await Notification.requestPermission();

        if (permission === 'granted') {
            console.log('알림 권한이 허용되었습니다.');
            return true;
        } else if (permission === 'denied') {
            console.log('알림 권한이 거부되었습니다.');
            return false;
        } else {
            console.log('사용자가 권한 요청을 무시했습니다.');
            return false;
        }
    } catch (error) {
        console.error('권한 요청 실패:', error);
        return false;
    }
}

// 콜백 기반 (레거시 호환성)
Notification.requestPermission(function(permission) {
    // 구형 브라우저 지원
    console.log('권한:', permission);
});
```

### 2.3 권한 요청 모범 사례

```javascript
// [나쁜 예] 페이지 로드 시 즉시 요청
// 사용자 경험이 나쁘고, 브라우저가 차단할 수 있음
window.addEventListener('load', () => {
    Notification.requestPermission(); // 하지 말 것!
});

// [좋은 예] 사용자 상호작용 후 요청
document.getElementById('enableNotifications').addEventListener('click', async () => {
    // 왜 알림이 필요한지 먼저 설명
    const userConsent = confirm(
        '새 메시지를 놓치지 않도록 알림을 활성화하시겠습니까?'
    );

    if (userConsent) {
        const permission = await Notification.requestPermission();
        if (permission === 'granted') {
            showSuccessMessage('알림이 활성화되었습니다!');
        }
    }
});
```

### 2.4 권한 상태에 따른 UI 처리

```javascript
function updateNotificationUI() {
    const button = document.getElementById('notificationBtn');
    const status = document.getElementById('notificationStatus');

    switch (Notification.permission) {
        case 'granted':
            button.textContent = '알림 끄기';
            button.disabled = false;
            status.textContent = '알림이 활성화되어 있습니다.';
            status.className = 'status-enabled';
            break;

        case 'denied':
            button.textContent = '알림 차단됨';
            button.disabled = true;
            status.textContent = '알림이 브라우저 설정에서 차단되었습니다. 브라우저 설정에서 변경해주세요.';
            status.className = 'status-denied';
            break;

        case 'default':
            button.textContent = '알림 활성화';
            button.disabled = false;
            status.textContent = '알림을 활성화하면 새로운 소식을 바로 받을 수 있습니다.';
            status.className = 'status-default';
            break;
    }
}
```

---

## 3. Notification 인터페이스

### 3.1 생성자

```javascript
// Notification(title, options)
const notification = new Notification(title, options);
```

#### 매개변수

| 매개변수 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `title` | `string` | 필수 | 알림의 제목 |
| `options` | `NotificationOptions` | 선택 | 알림 설정 객체 |

### 3.2 NotificationOptions 전체 구조

```typescript
// TypeScript 인터페이스로 표현한 NotificationOptions
interface NotificationOptions {
    // 콘텐츠 관련
    body?: string;              // 알림 본문 텍스트
    icon?: string;              // 알림 아이콘 URL
    image?: string;             // 알림에 표시할 큰 이미지 URL
    badge?: string;             // 배지 이미지 URL (작은 아이콘)

    // 동작 관련
    tag?: string;               // 알림 그룹 태그 (교체용)
    renotify?: boolean;         // 교체 시 다시 알림 (기본: false)
    requireInteraction?: boolean; // 사용자 상호작용까지 유지 (기본: false)
    silent?: boolean;           // 무음 알림 (기본: false)

    // 메타데이터
    data?: any;                 // 알림에 연결할 임의의 데이터
    timestamp?: number;         // 알림 생성 시각 (밀리초)

    // 표시 관련
    dir?: 'auto' | 'ltr' | 'rtl'; // 텍스트 방향 (기본: 'auto')
    lang?: string;              // BCP 47 언어 태그

    // 사용자 피드백
    vibrate?: number | number[]; // 진동 패턴
    actions?: NotificationAction[]; // 알림 액션 버튼
}

interface NotificationAction {
    action: string;             // 액션 식별자
    title: string;              // 액션 버튼 텍스트
    icon?: string;              // 액션 버튼 아이콘 URL
}
```

### 3.3 읽기 전용 속성

```javascript
const notification = new Notification('테스트 알림', {
    body: '본문 내용',
    icon: '/icons/notification.png',
    tag: 'message-1',
    data: { messageId: 42 }
});

// 읽기 전용 속성 접근
console.log(notification.title);              // "테스트 알림"
console.log(notification.body);               // "본문 내용"
console.log(notification.icon);               // "/icons/notification.png" (절대 URL)
console.log(notification.tag);                // "message-1"
console.log(notification.data);               // { messageId: 42 }
console.log(notification.dir);                // "auto"
console.log(notification.lang);               // ""
console.log(notification.requireInteraction); // false
console.log(notification.silent);             // false
console.log(notification.timestamp);          // 생성 시 Date.now() 값
console.log(notification.image);              // ""
console.log(notification.badge);              // ""
console.log(notification.vibrate);            // []
console.log(notification.actions);            // [] (읽기 전용 frozen array)
console.log(notification.renotify);           // false
```

### 3.4 메서드

```javascript
// close(): 알림 닫기
const notification = new Notification('알림');

// 5초 후 자동으로 닫기
setTimeout(() => {
    notification.close();
}, 5000);
```

### 3.5 정적 속성 및 메서드

```javascript
// 정적 속성
Notification.permission;  // "default" | "granted" | "denied"

// 정적 메서드
Notification.requestPermission(); // Promise<"default" | "granted" | "denied">

// Service Worker에서 사용 가능한 최대 액션 수
Notification.maxActions; // 브라우저마다 다름 (일반적으로 2-3)
```

---

## 4. Notification 옵션 상세

### 4.1 body (본문)

```javascript
// 단순 텍스트
new Notification('제목', {
    body: '알림 본문 텍스트입니다.'
});

// 긴 텍스트 (운영체제에 따라 잘릴 수 있음)
new Notification('새 메시지', {
    body: '안녕하세요, 내일 회의가 오후 3시로 변경되었습니다. 확인 부탁드립니다. 자세한 내용은 이메일을 확인해주세요.'
});

// 줄바꿈 사용
new Notification('주문 확인', {
    body: '주문번호: #12345\n상품: 웹 개발 가이드\n금액: 25,000원'
});
```

### 4.2 icon, image, badge

```javascript
// icon: 알림 옆에 표시되는 작은 아이콘
new Notification('메시지', {
    body: '새 메시지가 도착했습니다.',
    icon: '/images/sender-avatar.png'  // 보통 64x64 ~ 128x128
});

// image: 알림 내에 표시되는 큰 이미지 (Android Chrome 등)
new Notification('새 사진', {
    body: '김철수님이 사진을 공유했습니다.',
    image: '/images/shared-photo.jpg'  // 큰 이미지
});

// badge: 알림 표시줄에 사용되는 단색 아이콘 (Android)
new Notification('앱 알림', {
    body: '새로운 업데이트가 있습니다.',
    badge: '/images/badge-mono.png'  // 보통 96x96, 단색
});
```

```
알림 레이아웃:

┌─────────────────────────────────────────────┐
│  ┌────┐  알림 제목              [badge]     │
│  │icon│  알림 본문 텍스트                    │
│  └────┘                                     │
│  ┌─────────────────────────────────────┐    │
│  │           image (큰 이미지)          │    │
│  └─────────────────────────────────────┘    │
│  [액션1]  [액션2]                 출처 정보  │
└─────────────────────────────────────────────┘
```

### 4.3 tag (태그)

태그는 알림을 그룹화하고 교체하는 데 사용된다.

```javascript
// 같은 태그의 알림은 교체됨
new Notification('채팅', {
    body: '메시지 1개',
    tag: 'chat-room-1'
});

// 잠시 후 같은 태그로 새 알림 → 이전 알림 교체
new Notification('채팅', {
    body: '메시지 3개',
    tag: 'chat-room-1'
});

// 다른 태그 → 별도의 알림
new Notification('이메일', {
    body: '새 이메일',
    tag: 'email-inbox'
});
```

### 4.4 renotify

```javascript
// 기본: 교체 시 소리/진동 없음
new Notification('업데이트', {
    body: '새로운 내용',
    tag: 'updates',
    renotify: false  // 기본값
});

// renotify: true → 교체 시에도 소리/진동 발생
new Notification('긴급 알림', {
    body: '긴급 메시지',
    tag: 'urgent',
    renotify: true  // 교체해도 다시 알림
});
// 참고: renotify는 tag가 있을 때만 의미가 있음
// tag 없이 renotify: true를 사용하면 TypeError 발생
```

### 4.5 requireInteraction

```javascript
// 기본: 알림이 일정 시간 후 자동으로 사라짐
new Notification('일반 알림', {
    body: '잠시 후 사라집니다.'
});

// requireInteraction: true → 사용자가 닫을 때까지 유지
new Notification('중요 알림', {
    body: '이 알림은 확인할 때까지 사라지지 않습니다.',
    requireInteraction: true
});

// 참고: 데스크톱 브라우저에서 주로 지원
// 모바일에서는 무시될 수 있음
```

### 4.6 silent (무음)

```javascript
// 무음 알림 (소리/진동 없이 시각적으로만 표시)
new Notification('조용한 알림', {
    body: '소리 없이 표시됩니다.',
    silent: true
});

// silent: true와 vibrate를 동시에 사용하면 TypeError
// new Notification('잘못된 사용', {
//     silent: true,
//     vibrate: [200]  // TypeError!
// });
```

### 4.7 data (데이터)

```javascript
// 알림에 임의의 데이터 연결
new Notification('새 주문', {
    body: '주문이 접수되었습니다.',
    data: {
        orderId: 'ORD-2024-001',
        amount: 52000,
        items: ['웹 개발 가이드', 'CSS 마스터'],
        url: '/orders/ORD-2024-001'
    }
});

// Service Worker에서 데이터 활용
self.addEventListener('notificationclick', function(event) {
    const data = event.notification.data;

    if (data && data.url) {
        event.waitUntil(
            clients.openWindow(data.url)
        );
    }

    event.notification.close();
});
```

### 4.8 timestamp

```javascript
// 알림이 발생한 시점을 명시
// (전송 지연이 있는 경우 유용)
new Notification('메시지', {
    body: '5분 전에 보낸 메시지입니다.',
    timestamp: Date.now() - (5 * 60 * 1000)  // 5분 전
});

// 특정 시각 지정
new Notification('회의 알림', {
    body: '오후 3시 회의가 시작됩니다.',
    timestamp: new Date('2024-12-15T15:00:00').getTime()
});
```

### 4.9 dir과 lang

```javascript
// 텍스트 방향 설정
new Notification('알림', {
    body: '한국어 알림입니다.',
    dir: 'ltr',   // 왼쪽에서 오른쪽
    lang: 'ko'    // 한국어
});

// 아랍어 등 RTL 언어
new Notification('إشعار', {
    body: 'هذا إشعار بالعربية',
    dir: 'rtl',   // 오른쪽에서 왼쪽
    lang: 'ar'    // 아랍어
});
```

### 4.10 vibrate (진동)

```javascript
// 단일 진동 (200ms)
new Notification('알림', {
    body: '진동 알림',
    vibrate: 200
});

// 진동 패턴: [진동, 멈춤, 진동, 멈춤, ...]
new Notification('긴급', {
    body: '긴급 알림',
    vibrate: [200, 100, 200]  // 200ms 진동, 100ms 멈춤, 200ms 진동
});

// 복잡한 패턴
new Notification('SOS', {
    body: 'SOS 패턴',
    vibrate: [100, 50, 100, 50, 100, 200, 200, 50, 200, 50, 200, 200, 100, 50, 100, 50, 100]
});
```

### 4.11 actions (액션)

```javascript
// 알림에 액션 버튼 추가 (Service Worker에서만 완전 지원)
self.registration.showNotification('새 메시지', {
    body: '김철수: 내일 시간 되세요?',
    actions: [
        {
            action: 'reply',
            title: '답장',
            icon: '/icons/reply.png'
        },
        {
            action: 'dismiss',
            title: '무시',
            icon: '/icons/dismiss.png'
        }
    ]
});

// 최대 액션 수 확인
console.log('최대 액션 수:', Notification.maxActions);
// Chrome: 2, 다른 브라우저: 다를 수 있음
```

---

## 5. 이벤트 핸들러

### 5.1 이벤트 종류

| 이벤트 | 속성 | 발생 시점 |
|--------|------|----------|
| `show` | `onshow` | 알림이 표시될 때 |
| `click` | `onclick` | 알림을 클릭할 때 |
| `close` | `onclose` | 알림이 닫힐 때 |
| `error` | `onerror` | 알림 표시 오류 시 |

### 5.2 onclick

```javascript
const notification = new Notification('새 메시지', {
    body: '김철수님이 메시지를 보냈습니다.',
    data: { url: '/messages/42' }
});

// 알림 클릭 시 해당 페이지로 이동
notification.onclick = function(event) {
    event.preventDefault(); // 기본 동작(포커스) 방지

    // 해당 URL로 이동
    window.open(event.target.data.url, '_blank');

    // 알림 닫기
    notification.close();
};

// addEventListener 방식
notification.addEventListener('click', function(event) {
    console.log('알림 클릭됨:', event);
});
```

### 5.3 onclose

```javascript
const notification = new Notification('테스트', {
    body: '닫기 테스트'
});

notification.onclose = function(event) {
    console.log('알림이 닫혔습니다.');
    console.log('사용자에 의해 닫힘 또는 자동 만료');
};

// 또는
notification.addEventListener('close', function(event) {
    // 정리 작업
    updateNotificationCount(-1);
});
```

### 5.4 onerror

```javascript
const notification = new Notification('에러 테스트', {
    body: '아이콘 로드 실패 테스트',
    icon: '/invalid/icon/path.png'
});

notification.onerror = function(event) {
    console.error('알림 오류 발생:', event);
    // 아이콘 로드 실패 등의 오류 처리
};
```

### 5.5 onshow

```javascript
const notification = new Notification('표시 테스트', {
    body: '알림이 표시될 때'
});

notification.onshow = function(event) {
    console.log('알림이 화면에 표시되었습니다.');

    // 예: 알림 표시 횟수 기록
    analytics.track('notification_shown', {
        title: notification.title
    });
};
```

---

## 6. Service Worker에서의 알림

### 6.1 ServiceWorkerRegistration.showNotification()

Service Worker를 통한 알림은 페이지가 닫혀 있어도 표시할 수 있으며, 푸시 알림과 연동된다.

```javascript
// Service Worker 등록 및 알림 표시
async function showServiceWorkerNotification() {
    // Service Worker 등록
    const registration = await navigator.serviceWorker.register('/sw.js');

    // 알림 표시
    await registration.showNotification('백그라운드 알림', {
        body: 'Service Worker를 통한 알림입니다.',
        icon: '/icons/app-icon-192.png',
        badge: '/icons/badge-72.png',
        image: '/images/notification-image.jpg',
        vibrate: [200, 100, 200],
        tag: 'background-notification',
        renotify: true,
        requireInteraction: true,
        actions: [
            { action: 'open', title: '열기', icon: '/icons/open.png' },
            { action: 'dismiss', title: '닫기', icon: '/icons/close.png' }
        ],
        data: {
            url: '/dashboard',
            timestamp: Date.now()
        }
    });
}
```

### 6.2 ServiceWorkerRegistration.getNotifications()

```javascript
// 현재 표시 중인 알림 목록 조회
async function getActiveNotifications() {
    const registration = await navigator.serviceWorker.ready;
    const notifications = await registration.getNotifications();

    console.log('현재 알림 수:', notifications.length);

    notifications.forEach(notification => {
        console.log('- 제목:', notification.title);
        console.log('  태그:', notification.tag);
        console.log('  데이터:', notification.data);
    });

    return notifications;
}

// 특정 태그의 알림만 조회
async function getNotificationsByTag(tag) {
    const registration = await navigator.serviceWorker.ready;
    const notifications = await registration.getNotifications({ tag: tag });
    return notifications;
}
```

### 6.3 Push API와 연동

```javascript
// sw.js (Service Worker)

// 푸시 메시지 수신 시 알림 표시
self.addEventListener('push', function(event) {
    console.log('푸시 메시지 수신:', event);

    let notificationData;

    if (event.data) {
        notificationData = event.data.json();
    } else {
        notificationData = {
            title: '새 알림',
            body: '새로운 업데이트가 있습니다.'
        };
    }

    const options = {
        body: notificationData.body,
        icon: notificationData.icon || '/icons/default-icon.png',
        badge: '/icons/badge.png',
        tag: notificationData.tag || 'default',
        data: notificationData.data || {},
        actions: notificationData.actions || []
    };

    event.waitUntil(
        self.registration.showNotification(notificationData.title, options)
    );
});
```

---

## 7. NotificationEvent

### 7.1 notificationclick 이벤트

```javascript
// sw.js (Service Worker)

self.addEventListener('notificationclick', function(event) {
    console.log('알림 클릭됨');
    console.log('알림 태그:', event.notification.tag);
    console.log('클릭된 액션:', event.action);

    // 알림 닫기
    event.notification.close();

    // 액션에 따른 처리
    if (event.action === 'reply') {
        // 답장 페이지 열기
        event.waitUntil(
            clients.openWindow('/reply?to=' + event.notification.data.senderId)
        );
    } else if (event.action === 'dismiss') {
        // 무시 - 알림만 닫기
        return;
    } else {
        // 기본 클릭 (액션 버튼이 아닌 알림 본체 클릭)
        event.waitUntil(
            handleNotificationClick(event)
        );
    }
});

async function handleNotificationClick(event) {
    const url = event.notification.data.url || '/';

    // 이미 열린 탭이 있으면 포커스
    const clientList = await clients.matchAll({
        type: 'window',
        includeUncontrolled: true
    });

    for (const client of clientList) {
        if (client.url === url && 'focus' in client) {
            return client.focus();
        }
    }

    // 열린 탭이 없으면 새 탭 열기
    return clients.openWindow(url);
}
```

### 7.2 notificationclose 이벤트

```javascript
// sw.js
self.addEventListener('notificationclose', function(event) {
    console.log('알림이 닫혔습니다 (사용자가 직접 닫음)');
    console.log('알림 태그:', event.notification.tag);

    // 분석 데이터 전송
    event.waitUntil(
        fetch('/api/analytics', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                event: 'notification_dismissed',
                tag: event.notification.tag,
                timestamp: Date.now()
            })
        })
    );
});
```

### 7.3 NotificationEvent 속성

```javascript
self.addEventListener('notificationclick', function(event) {
    // NotificationEvent 속성
    console.log(event.notification);  // Notification 객체
    console.log(event.action);        // 클릭된 액션 식별자 (문자열)

    // ExtendableEvent에서 상속된 메서드
    event.waitUntil(promise);  // Service Worker가 작업 완료까지 대기
});
```

---

## 8. 알림 교체와 액션

### 8.1 알림 교체 (tag 사용)

```javascript
// 메시지 알림 교체 시나리오

// 첫 번째 메시지
self.registration.showNotification('김철수', {
    body: '안녕하세요!',
    tag: 'chat-user-123',
    data: { messageCount: 1 }
});

// 두 번째 메시지 → 첫 번째 알림을 교체
self.registration.showNotification('김철수', {
    body: '내일 회의 있으세요?',
    tag: 'chat-user-123',  // 같은 태그 → 교체
    data: { messageCount: 2 }
});

// 세 번째 메시지 → 두 번째 알림을 교체하면서 다시 알림
self.registration.showNotification('김철수', {
    body: '김철수님의 메시지 3개',
    tag: 'chat-user-123',
    renotify: true,  // 교체할 때 소리/진동 다시 발생
    data: { messageCount: 3 }
});
```

### 8.2 알림 누적 패턴

```javascript
// sw.js
self.addEventListener('push', function(event) {
    const data = event.data.json();

    event.waitUntil(
        // 같은 태그의 기존 알림 조회
        self.registration.getNotifications({ tag: data.tag }).then(notifications => {
            let messageCount = 1;

            if (notifications.length > 0) {
                // 기존 알림의 데이터에서 카운트 가져오기
                const existingData = notifications[0].data;
                messageCount = (existingData.messageCount || 1) + 1;
            }

            // 알림 교체 (누적 메시지 표시)
            return self.registration.showNotification(
                messageCount > 1 ? `${data.sender} (${messageCount}건)` : data.sender,
                {
                    body: messageCount > 1
                        ? `${messageCount}개의 새 메시지가 있습니다.`
                        : data.body,
                    tag: data.tag,
                    renotify: true,
                    icon: data.icon,
                    data: {
                        ...data,
                        messageCount: messageCount
                    }
                }
            );
        })
    );
});
```

### 8.3 액션 버튼 패턴

```javascript
// 이메일 알림의 액션
self.registration.showNotification('새 이메일', {
    body: '제목: 프로젝트 미팅 일정 변경\n발신: 이영희',
    icon: '/icons/email.png',
    actions: [
        { action: 'archive', title: '보관', icon: '/icons/archive.png' },
        { action: 'reply', title: '답장', icon: '/icons/reply.png' }
    ],
    tag: 'email-456',
    data: { emailId: 456, from: 'lee@example.com' }
});

// 메신저 알림의 액션
self.registration.showNotification('김철수', {
    body: '점심 뭐 먹을까요?',
    icon: '/images/users/kim.jpg',
    actions: [
        { action: 'reply-yes', title: '좋아요', icon: '/icons/thumbs-up.png' },
        { action: 'reply-no', title: '다음에', icon: '/icons/decline.png' }
    ],
    tag: 'chat-789',
    data: { chatId: 789, senderId: 'kim-123' }
});

// 액션 처리
self.addEventListener('notificationclick', function(event) {
    event.notification.close();

    const data = event.notification.data;

    switch (event.action) {
        case 'archive':
            event.waitUntil(
                fetch(`/api/email/${data.emailId}/archive`, { method: 'POST' })
            );
            break;

        case 'reply':
            event.waitUntil(
                clients.openWindow(`/email/compose?reply=${data.emailId}`)
            );
            break;

        case 'reply-yes':
            event.waitUntil(
                fetch(`/api/chat/${data.chatId}/reply`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: '좋아요!' })
                })
            );
            break;

        case 'reply-no':
            event.waitUntil(
                fetch(`/api/chat/${data.chatId}/reply`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: '다음에 하죠!' })
                })
            );
            break;

        default:
            // 알림 본체 클릭
            event.waitUntil(
                clients.openWindow(data.url || '/')
            );
    }
});
```

---

## 9. 알림 생명주기

### 9.1 생명주기 다이어그램

```
[알림 생명주기]

권한 확인
    ↓ (granted)
알림 생성 (new Notification() 또는 showNotification())
    ↓
onshow 이벤트 발생
    ↓
알림 표시 중
    ├─→ [사용자가 알림 클릭] → onclick → 처리 → close()
    ├─→ [사용자가 알림 닫기(X)] → onclose
    ├─→ [자동 만료] → onclose
    ├─→ [같은 tag의 새 알림] → 교체 (renotify 시 다시 show)
    ├─→ [코드에서 close() 호출] → onclose
    └─→ [오류 발생] → onerror
```

### 9.2 알림 표시 시간

```javascript
// 알림 자동 닫힘 시간은 브라우저/OS마다 다름
// 일반적으로:
// - Windows: ~5초
// - macOS: ~5초 (알림 센터에 남음)
// - Linux: ~10초
// - Android: 알림 트레이에 유지

// requireInteraction으로 자동 닫힘 방지 가능
new Notification('중요', {
    body: '이 알림은 사용자가 처리할 때까지 유지됩니다.',
    requireInteraction: true
});

// 또는 타이머로 수동 닫기
const notification = new Notification('알림', { body: '10초 후 닫힙니다.' });
setTimeout(() => notification.close(), 10000);
```

---

## 10. 보안 및 개인정보 고려사항

### 10.1 보안 컨텍스트 요구

```javascript
// Notifications API는 보안 컨텍스트(HTTPS)에서만 작동
// HTTP에서는 사용 불가

if (window.isSecureContext) {
    // Notifications API 사용 가능
    if ('Notification' in window) {
        console.log('알림 지원:', Notification.permission);
    }
} else {
    console.warn('보안 컨텍스트가 아닙니다. 알림을 사용할 수 없습니다.');
}
```

### 10.2 사용자 활성화 요구

```javascript
// 권한 요청은 사용자 활성화(user activation) 후에만 가능
// = 사용자의 클릭, 터치, 키보드 입력 등에 의한 이벤트 핸들러 내에서

// [동작하지 않음] 페이지 로드 시 자동 요청
window.addEventListener('load', () => {
    // 사용자 활성화 없음 → 브라우저가 차단할 수 있음
    Notification.requestPermission();
});

// [동작함] 사용자 클릭 이벤트 내에서 요청
button.addEventListener('click', () => {
    // 사용자 활성화 있음 → 정상 동작
    Notification.requestPermission();
});
```

### 10.3 개인정보 보호

```javascript
// 알림은 사용자 추적에 악용될 수 있으므로 주의
// - 알림 표시 여부로 사용자 재방문을 감지할 수 있음
// - 알림의 이미지 URL로 사용자 IP를 추적할 수 있음
// - 알림 클릭 시간으로 사용자 활동 패턴을 분석할 수 있음

// 권장사항:
// 1. 필요한 최소한의 알림만 표시
// 2. 사용자가 알림 빈도를 조절할 수 있게 제공
// 3. 알림 해제 옵션을 명확하게 제공
// 4. 외부 이미지 URL 사용 시 프라이버시 영향 고려
```

---

## 11. 실용 예제

### 11.1 완전한 알림 시스템 구현

```javascript
// notification-manager.js

class NotificationManager {
    constructor() {
        this.permission = Notification.permission;
        this.swRegistration = null;
    }

    // 초기화
    async init() {
        if (!('Notification' in window)) {
            console.warn('이 브라우저는 알림을 지원하지 않습니다.');
            return false;
        }

        if ('serviceWorker' in navigator) {
            try {
                this.swRegistration = await navigator.serviceWorker.register('/sw.js');
                console.log('Service Worker 등록 완료');
            } catch (error) {
                console.error('Service Worker 등록 실패:', error);
            }
        }

        return true;
    }

    // 권한 요청
    async requestPermission() {
        if (this.permission === 'granted') {
            return true;
        }

        if (this.permission === 'denied') {
            console.warn('알림이 차단되어 있습니다. 브라우저 설정에서 변경해주세요.');
            return false;
        }

        const result = await Notification.requestPermission();
        this.permission = result;
        return result === 'granted';
    }

    // 알림 표시 (페이지 컨텍스트)
    showNotification(title, options = {}) {
        if (this.permission !== 'granted') {
            console.warn('알림 권한이 없습니다.');
            return null;
        }

        const notification = new Notification(title, {
            icon: options.icon || '/icons/default.png',
            badge: options.badge || '/icons/badge.png',
            ...options
        });

        // 기본 클릭 핸들러
        notification.onclick = function(event) {
            event.preventDefault();

            if (options.url) {
                window.focus();
                window.location.href = options.url;
            } else {
                window.focus();
            }

            notification.close();

            // 커스텀 클릭 핸들러 호출
            if (options.onClick) {
                options.onClick(event);
            }
        };

        // 자동 닫기
        if (options.autoClose) {
            setTimeout(() => notification.close(), options.autoClose);
        }

        return notification;
    }

    // 알림 표시 (Service Worker 컨텍스트)
    async showPersistentNotification(title, options = {}) {
        if (!this.swRegistration) {
            console.warn('Service Worker가 등록되지 않았습니다.');
            return this.showNotification(title, options);
        }

        if (this.permission !== 'granted') {
            console.warn('알림 권한이 없습니다.');
            return null;
        }

        await this.swRegistration.showNotification(title, {
            icon: options.icon || '/icons/default.png',
            badge: options.badge || '/icons/badge.png',
            ...options
        });
    }

    // 현재 알림 목록 조회
    async getNotifications(filter = {}) {
        if (!this.swRegistration) {
            return [];
        }

        return this.swRegistration.getNotifications(filter);
    }

    // 모든 알림 닫기
    async closeAll() {
        const notifications = await this.getNotifications();
        notifications.forEach(n => n.close());
    }

    // 특정 태그의 알림 닫기
    async closeByTag(tag) {
        const notifications = await this.getNotifications({ tag });
        notifications.forEach(n => n.close());
    }
}

// 사용 예
const notifManager = new NotificationManager();

async function main() {
    await notifManager.init();

    document.getElementById('enableBtn').addEventListener('click', async () => {
        const granted = await notifManager.requestPermission();
        if (granted) {
            notifManager.showNotification('환영합니다!', {
                body: '알림이 성공적으로 활성화되었습니다.',
                autoClose: 5000
            });
        }
    });

    document.getElementById('testBtn').addEventListener('click', () => {
        notifManager.showNotification('테스트 알림', {
            body: '이것은 테스트 알림입니다.',
            icon: '/icons/test.png',
            url: '/test-page',
            tag: 'test',
            actions: [
                { action: 'view', title: '보기' },
                { action: 'dismiss', title: '무시' }
            ]
        });
    });
}

main();
```

### 11.2 Service Worker 알림 핸들러

```javascript
// sw.js

// 푸시 이벤트 처리
self.addEventListener('push', function(event) {
    let data;

    try {
        data = event.data ? event.data.json() : {};
    } catch (e) {
        data = { title: '새 알림', body: event.data ? event.data.text() : '' };
    }

    const title = data.title || '새 알림';
    const options = {
        body: data.body || '',
        icon: data.icon || '/icons/default-192.png',
        badge: data.badge || '/icons/badge-72.png',
        image: data.image,
        tag: data.tag || 'default',
        renotify: data.renotify || false,
        requireInteraction: data.requireInteraction || false,
        silent: data.silent || false,
        vibrate: data.vibrate || [200, 100, 200],
        data: data.data || { url: '/' },
        actions: data.actions || [],
        timestamp: data.timestamp || Date.now()
    };

    event.waitUntil(
        self.registration.showNotification(title, options)
    );
});

// 알림 클릭 이벤트 처리
self.addEventListener('notificationclick', function(event) {
    event.notification.close();

    const action = event.action;
    const data = event.notification.data;

    let targetUrl = data.url || '/';

    // 액션별 URL 매핑
    if (action && data.actionUrls && data.actionUrls[action]) {
        targetUrl = data.actionUrls[action];
    }

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then(function(clientList) {
                // 이미 열린 탭에서 URL 일치하는 것 찾기
                for (const client of clientList) {
                    if (new URL(client.url).pathname === targetUrl && 'focus' in client) {
                        return client.focus();
                    }
                }

                // 열린 탭이 있으면 네비게이션
                if (clientList.length > 0) {
                    const client = clientList[0];
                    client.navigate(targetUrl);
                    return client.focus();
                }

                // 새 탭 열기
                return clients.openWindow(targetUrl);
            })
    );
});

// 알림 닫기 이벤트 처리
self.addEventListener('notificationclose', function(event) {
    // 분석 데이터 전송 (비동기)
    event.waitUntil(
        fetch('/api/analytics/notification-closed', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                tag: event.notification.tag,
                action: 'closed',
                timestamp: Date.now()
            })
        }).catch(() => {
            // 네트워크 오류 무시
        })
    );
});
```

### 11.3 채팅 알림 패턴

```javascript
// 채팅 메시지 수신 시 알림 표시
class ChatNotifier {
    constructor(registration) {
        this.registration = registration;
        this.messageCounts = new Map(); // tag → count
    }

    async notify(message) {
        const tag = `chat-${message.roomId}`;

        // 기존 알림 확인
        const existing = await this.registration.getNotifications({ tag });
        const currentCount = existing.length > 0
            ? (existing[0].data.messageCount || 1) + 1
            : 1;

        this.messageCounts.set(tag, currentCount);

        let title, body;

        if (currentCount === 1) {
            title = message.senderName;
            body = message.text;
        } else {
            title = `${message.senderName} (${currentCount}건)`;
            body = `${currentCount}개의 새 메시지`;
        }

        await this.registration.showNotification(title, {
            body: body,
            icon: message.senderAvatar,
            badge: '/icons/chat-badge.png',
            tag: tag,
            renotify: true,
            vibrate: [100, 50, 100],
            actions: [
                { action: 'reply', title: '답장' },
                { action: 'mark-read', title: '읽음 표시' }
            ],
            data: {
                type: 'chat',
                roomId: message.roomId,
                senderId: message.senderId,
                messageCount: currentCount,
                url: `/chat/${message.roomId}`
            }
        });
    }
}
```

---

## 12. 브라우저 호환성 및 제한사항

### 12.1 브라우저 지원 현황

| 기능 | Chrome | Firefox | Safari | Edge |
|------|--------|---------|--------|------|
| `Notification` 생성자 | 지원 | 지원 | 지원 | 지원 |
| `requestPermission()` (Promise) | 46+ | 47+ | 15+ | 지원 |
| `showNotification()` | 42+ | 44+ | 16+ | 지원 |
| `getNotifications()` | 42+ | 지원 | 16+ | 지원 |
| `actions` | 48+ | 미지원 | 미지원 | 지원 |
| `badge` | 53+ | 미지원 | 미지원 | 지원 |
| `image` | 56+ | 미지원 | 미지원 | 지원 |
| `renotify` | 50+ | 미지원 | 미지원 | 지원 |
| `requireInteraction` | 47+ | 미지원 | 미지원 | 지원 |
| `silent` | 43+ | 미지원 | 미지원 | 지원 |
| `vibrate` | 53+ | 미지원 | 미지원 | 지원 |
| `timestamp` | 50+ | 미지원 | 미지원 | 지원 |

### 12.2 플랫폼별 제한사항

```
[macOS]
- 알림은 macOS 알림 센터에 통합됨
- actions 미지원 (Safari)
- image 미지원
- badge 미지원

[Windows]
- Windows 알림 센터에 통합됨
- 대부분의 기능 지원 (Chrome/Edge)

[Android]
- 모든 기능 지원 (Chrome)
- 백그라운드 알림은 Service Worker 필요
- 진동 패턴 지원

[iOS]
- Safari 16.4+ 에서 PWA 알림 지원 (홈 화면 추가 필요)
- 많은 제한사항 존재
- actions 미지원
```

### 12.3 기능 감지 패턴

```javascript
// 전체 기능 감지
function getNotificationSupport() {
    const support = {
        basic: 'Notification' in window,
        serviceWorker: 'serviceWorker' in navigator,
        pushManager: 'PushManager' in window,
        actions: false,
        maxActions: 0
    };

    if (support.basic) {
        support.permission = Notification.permission;
        support.maxActions = Notification.maxActions || 0;
        support.actions = support.maxActions > 0;
    }

    return support;
}

const support = getNotificationSupport();
console.table(support);
```

---

## 참고 자료

- [Notifications API Standard (WHATWG)](https://notifications.spec.whatwg.org/)
- [MDN - Notifications API](https://developer.mozilla.org/ko/docs/Web/API/Notifications_API)
- [MDN - Using the Notifications API](https://developer.mozilla.org/ko/docs/Web/API/Notifications_API/Using_the_Notifications_API)
- [Web Push Notifications (Google)](https://web.dev/notifications/)
- [Push API (W3C)](https://www.w3.org/TR/push-api/)
