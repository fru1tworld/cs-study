# Unit 파일

> 이 문서는 `man systemd.unit` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd.unit.html

---

## 목차

1. [Unit이란?](#unit이란)
2. [Unit 파일의 위치](#unit-파일의-위치)
3. [Unit 파일 형식](#unit-파일-형식)
4. [[Unit] 섹션](#unit-섹션)
5. [[Install] 섹션](#install-섹션)
6. [Drop-in 디렉터리](#drop-in-디렉터리)
7. [Unit 이름과 인스턴스](#unit-이름과-인스턴스)
8. [상태(State)](#상태state)
9. [참고 자료](#참고-자료)

---

## Unit이란?

systemd는 모든 관리 대상을 **unit** 으로 추상화합니다. unit은 무엇이든 될 수 있습니다 — 데몬 프로세스, 마운트 지점, 소켓, 타이머, 디바이스 등. unit의 종류는 파일 확장자로 구분됩니다.

| 확장자 | 종류 | 설명 |
| --- | --- | --- |
| `.service` | 서비스 | 데몬 프로세스 |
| `.socket` | 소켓 | IPC/네트워크 소켓 (활성화 트리거) |
| `.target` | 타겟 | unit 그룹화 (런레벨 대체) |
| `.mount` | 마운트 | 파일시스템 마운트 |
| `.automount` | 오토마운트 | 온디맨드 마운트 |
| `.swap` | 스왑 | 스왑 디바이스/파일 |
| `.timer` | 타이머 | cron 대체 |
| `.path` | 경로 | 파일/디렉터리 변경 감시 |
| `.slice` | 슬라이스 | cgroup 계층 |
| `.scope` | 스코프 | 외부에서 만든 프로세스 그룹 |
| `.device` | 디바이스 | udev가 노출한 디바이스 |

---

## Unit 파일의 위치

systemd는 다음 디렉터리를 우선순위 순으로 검색합니다.

### 시스템 unit

| 경로 | 용도 |
| --- | --- |
| `/etc/systemd/system/` | 관리자 정의 (가장 높은 우선순위) |
| `/run/systemd/system/` | 런타임 생성 |
| `/usr/lib/systemd/system/` | 패키지 설치 (배포판 기본) |

같은 이름의 unit이 여러 경로에 있으면 위쪽이 이깁니다. 즉 `/etc/systemd/system/sshd.service` 가 있으면 패키지가 제공한 `/usr/lib/systemd/system/sshd.service` 를 완전히 덮어씁니다.

### 사용자 unit

| 경로 | 용도 |
| --- | --- |
| `~/.config/systemd/user/` | 사용자 정의 |
| `/etc/systemd/user/` | 관리자가 모든 사용자에게 배포 |
| `/usr/lib/systemd/user/` | 패키지 설치 |

사용자 unit은 `systemctl --user` 로 제어합니다.

---

## Unit 파일 형식

INI 스타일이며 세 부분으로 구성됩니다.

```ini
[Unit]
Description=My Custom Service
After=network-online.target
Requires=postgresql.service

[Service]
Type=simple
ExecStart=/usr/local/bin/my-app
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

- `[Unit]`: 모든 unit 종류에 공통 (메타데이터, 의존성)
- `[Service]` / `[Socket]` / `[Timer]` 등: unit 종류별 섹션
- `[Install]`: `systemctl enable` 시 동작 정의

### 값의 형식

- 문자열: 따옴표 없음. 공백 포함 시 `"..."` 또는 `'...'`
- 불리언: `yes`, `no`, `true`, `false`, `1`, `0`
- 시간: `30s`, `5min`, `2h`, `1d` (단위 없으면 초)
- 크기: `100M`, `1G`, `500K`
- 리스트: 공백 또는 줄바꿈으로 구분 (지시자를 여러 번 써서 누적 가능)
- 빈 값: 지시자에 빈 값을 할당하면 누적된 리스트가 초기화됩니다 (drop-in에서 유용)

---

## [Unit] 섹션

### 메타데이터

- `Description=`: 사람이 읽을 수 있는 설명
- `Documentation=`: 문서 URL/man 페이지 (예: `man:my-app(8)`)

### 의존성

- `Requires=`: 강한 의존
- `Wants=`: 약한 의존 (권장 — 더 안전)
- `Requisite=`: 이미 실행 중이어야 함
- `BindsTo=`: 명시한 unit이 멈추면 즉시 함께 멈춤
- `PartOf=`: 부모 unit 재시작/중지 시 동반
- `Conflicts=`: 동시 실행 불가
- `Before=`, `After=`: 시작 순서 (의존성과 독립)

### 조건

조건이 거짓이면 unit은 시작되지 않고 (실패가 아닌) **건너뜀** 처리됩니다.

- `ConditionPathExists=`: 경로 존재 여부
- `ConditionFileNotEmpty=`
- `ConditionDirectoryNotEmpty=`
- `ConditionKernelCommandLine=`: 커널 부트 파라미터
- `ConditionVirtualization=`: VM/컨테이너 종류
- `ConditionArchitecture=`: x86-64, arm64 등
- `ConditionMemory=`: 시스템 메모리 (예: `>=2G`)
- `AssertXxx=`: Condition과 같지만 거짓이면 **실패** 로 표시

### 기타

- `OnFailure=`: 실패 시 시작할 unit
- `OnSuccess=`: 성공 시 시작할 unit (systemd 249+)
- `RefuseManualStart=`, `RefuseManualStop=`: 수동 제어 차단

---

## [Install] 섹션

`[Install]` 은 `systemctl enable` 이 호출될 때만 의미가 있습니다. unit을 다른 unit의 의존성에 추가하는 심볼릭 링크를 만드는 데 사용됩니다.

- `WantedBy=`: 가장 흔함. `multi-user.target.wants/` 에 링크
- `RequiredBy=`: 강한 버전
- `Also=`: enable 시 함께 enable할 unit
- `Alias=`: 별칭 (다른 이름으로도 호출 가능)
- `DefaultInstance=`: 템플릿 unit의 기본 인스턴스

```ini
[Install]
WantedBy=multi-user.target
Alias=myapp.service
```

---

## Drop-in 디렉터리

기존 unit을 **부분적으로 덮어쓰기** 위한 메커니즘입니다. 패키지가 제공한 unit을 직접 수정하지 않고 일부 옵션만 추가/변경할 수 있어 업그레이드 충돌을 피할 수 있습니다.

```
/etc/systemd/system/<unit-name>.d/
├── override.conf
└── 50-custom-restart.conf
```

이 디렉터리의 모든 `.conf` 파일이 알파벳 순으로 적용됩니다. 가장 쉬운 작성법:

```bash
sudo systemctl edit nginx.service
```

이 명령은 자동으로 `/etc/systemd/system/nginx.service.d/override.conf` 를 만들어 편집기를 띄웁니다.

### 리스트 누적 vs 초기화

```ini
[Service]
# 기존 ExecStart에 "추가"가 아닌 "교체"
ExecStart=
ExecStart=/new/path/to/binary
```

대부분의 리스트형 지시자(`ExecStart`, `Environment` 등)는 빈 값으로 한 번 초기화하지 않으면 누적됩니다.

---

## Unit 이름과 인스턴스

### 일반 unit

`sshd.service`, `nginx.service` 같은 단순 이름.

### 템플릿 unit

`@` 가 포함된 이름. 인스턴스 매개변수를 받습니다.

```
getty@.service          (템플릿)
getty@tty1.service      (인스턴스 — tty1이 매개변수)
```

템플릿 안에서 `%i` (인스턴스 이름), `%I` (이스케이프 해제), `%H` (호스트명) 등의 specifier를 사용할 수 있습니다.

```ini
[Unit]
Description=Getty on %I

[Service]
ExecStart=/sbin/agetty %I
```

`systemctl start getty@tty3.service` 로 인스턴스화할 수 있습니다.

### 주요 specifier

| Specifier | 의미 |
| --- | --- |
| `%n` | 전체 unit 이름 |
| `%N` | 이스케이프 해제된 unit 이름 |
| `%p` | prefix (템플릿 이름의 `@` 앞부분) |
| `%i` | instance (템플릿 이름의 `@` 뒤부분) |
| `%I` | 이스케이프 해제된 instance |
| `%u` | 사용자 이름 |
| `%h` | 사용자 홈 디렉터리 |
| `%H` | 호스트명 |
| `%t` | 런타임 디렉터리 (`/run` 또는 `$XDG_RUNTIME_DIR`) |

---

## 상태(State)

`systemctl status` 가 보여주는 두 가지 상태:

### LOAD 상태

- `loaded`: unit 파일이 정상적으로 로드됨
- `not-found`: 파일을 찾을 수 없음
- `bad-setting`: 파일에 오류
- `error`: 로드 실패
- `masked`: `/dev/null` 로 가려져 있음 (`systemctl mask`)

### ACTIVE 상태

- `active (running)`: 실행 중
- `active (exited)`: 한 번 실행하고 정상 종료 (oneshot)
- `active (waiting)`: 이벤트 대기 중 (소켓, 타이머)
- `inactive (dead)`: 중지됨
- `failed`: 실패
- `activating`, `deactivating`: 전이 중

---

## 참고 자료

- [man systemd.unit](https://www.freedesktop.org/software/systemd/man/systemd.unit.html)
- [man systemctl](https://www.freedesktop.org/software/systemd/man/systemctl.html)
- [Drop-in files](https://www.freedesktop.org/software/systemd/man/systemd.unit.html#Description)
