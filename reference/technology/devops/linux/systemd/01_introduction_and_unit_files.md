# systemd 소개와 Unit 파일

## systemd 소개

> 원본: https://systemd.io/ , https://www.freedesktop.org/wiki/Software/systemd/

<a id="systemd란"></a>
### systemd란?

systemd는 Linux의 시스템, 서비스 관리자(System and Service Manager)다. PID 1로 부팅되어 사용자 공간(user space)을 초기화하고 모든 서비스의 라이프사이클을 관리한다. 이와 함께 로깅, 네트워킹, 로그인, 디바이스 관리 등 OS의 기본 기능을 제공하는 통합 플랫폼 역할도 한다.

- 원래 Lennart Poettering이 Red Hat에서 시작한 프로젝트 → 현재 거의 모든 주요 Linux 배포판(Debian, Ubuntu, Fedora, RHEL, Arch, openSUSE)에서 표준 init 시스템으로 채택됨

#### systemd의 범위

- systemd는 단순한 init 시스템이 아니라 다음 영역을 포괄:

- 서비스 관리 (`systemctl`, unit 파일)
- 로깅 (`systemd-journald`)
- 네트워크 설정 (`systemd-networkd`)
- DNS 해석 (`systemd-resolved`)
- 시간 동기화 (`systemd-timesyncd`)
- 사용자 세션 관리 (`systemd-logind`)
- 디바이스 관리 (`systemd-udevd`)
- 컨테이너 (`systemd-nspawn`)
- 부트로더 (`systemd-boot`)
- 홈 디렉터리 (`systemd-homed`)

<a id="설계-철학"></a>

### 설계 철학

- systemd 설계 기반 원칙:

#### 1. 적극적인 병렬화 (Aggressive Parallelization)

- 전통적인 SysV init은 스크립트를 순차적으로 실행 → systemd는 가능한 모든 서비스를 병렬로 시작
  - 소켓 기반 활성화(socket activation)와 D-Bus 활성화를 통해 의존성이 있는 서비스조차 병렬 부팅 가능

#### 2. 온디맨드 시작 (On-demand Activation)

- 서비스가 실제로 필요할 때만 시작
  - 예를 들어 SSH 데몬은 누군가 22번 포트에 연결하기 전까지는 실행되지 않을 수 있음 → 부팅 시간 단축, 메모리 사용량 감소

#### 3. 선언적 구성 (Declarative Configuration)

서비스의 원하는 상태는 INI 형식의 unit 파일에 선언한다. 서비스를 어떻게(how) 시작할지 스크립트로 풀어 쓰던 구성을, 어떤 상태(what)가 필요한지 지정하는 구성으로 옮긴 것이다.

#### 4. 의존성 추적

- 서비스 간 의존 관계를 명시적으로 선언 → systemd가 시작 순서, 재시작 전파, 실패 처리를 자동으로 관리

#### 5. 리소스 격리 (cgroups 기반)

- 각 서비스는 cgroup으로 격리 → CPU, 메모리, IO 제한을 unit 파일에서 직접 지정 가능
  - 서비스가 fork한 프로세스도 cgroup을 통해 추적됨 → 더블 fork로 init에서 탈출하는 트릭 불가

<a id="sysv-init과의-차이"></a>

### SysV init과의 차이

- 구성 형식
  - SysV init: shell 스크립트 (`/etc/init.d/`)
  - systemd: unit 파일 (INI 형식)
- 시작 방식
  - SysV init: 순차적
  - systemd: 병렬 + 의존성 기반
- 프로세스 추적
  - SysV init: PID 파일 (불안정)
  - systemd: cgroup
- 로깅
  - SysV init: syslog 외부 의존
  - systemd: journald 통합
- 활성화
  - SysV init: 부팅 시 모두 시작
  - systemd: 소켓, D-Bus, path 기반 lazy
- 재시작
  - SysV init: 수동
  - systemd: 정책 기반 자동
- 타이머
  - SysV init: cron 별도
  - systemd: systemd.timer 통합

<a id="구성-요소"></a>

### 구성 요소

- systemd 프로젝트는 여러 바이너리, 라이브러리로 구성됨

#### 핵심 데몬

- `systemd` (PID 1): 시스템 매니저
- `systemd --user`: 사용자별 매니저
- `systemd-journald`: 구조화된 로그 수집
- `systemd-logind`: 사용자 로그인 세션 관리
- `systemd-udevd`: 디바이스 이벤트 관리
- `systemd-networkd`: 네트워크 구성 (선택적)
- `systemd-resolved`: DNS 해석 (선택적)
- `systemd-timesyncd`: SNTP 시간 동기화 (선택적)

#### 주요 명령어

- `systemctl`: 서비스/unit 제어
- `journalctl`: 로그 조회
- `loginctl`: 로그인 세션 조회
- `hostnamectl`, `timedatectl`, `localectl`: 호스트 정보 설정
- `systemd-analyze`: 부팅 분석
- `systemd-run`: 임시 unit 실행
- `bootctl`: 부트로더 관리
- `coredumpctl`: 코어 덤프 조회

<a id="pid-1로서의-역할"></a>

### PID 1로서의 역할

- PID 1은 Linux 커널이 부팅 마지막 단계에서 실행하는 첫 번째 사용자 공간 프로세스
  - PID 1이 종료되면 커널 패닉 발생 → 매우 안정적이어야 함

- systemd가 PID 1로서 수행하는 일:

- 부팅 시퀀스 조정: 마운트, fsck, swap 활성화, 서비스 시작
- 자식 프로세스 수집(reap): 고아 프로세스의 종료 상태 회수
- 시그널 라우팅: SIGTERM, SIGINT 등 처리
- 소켓, 디바이스 이벤트 디스패치: 활성화 트리거
- 시스템 종료: 깨끗한 셧다운, 재부팅

- systemd는 D-Bus 인터페이스(`org.freedesktop.systemd1`)를 통해 다른 프로세스와 통신 → `systemctl`도 내부적으로는 이 D-Bus API를 호출

<a id="의존성-모델"></a>

### 의존성 모델

- systemd의 unit은 다른 unit과 다양한 관계를 맺을 수 있음

#### 순서(Ordering) 의존성

- `Before=`: 이 unit이 명시한 unit보다 먼저 시작
- `After=`: 이 unit이 명시한 unit 이후에 시작

#### 요구(Requirement) 의존성

- `Requires=`: 강한 의존
  - 명시한 unit이 실패하면 이 unit도 중단
- `Wants=`: 약한 의존
  - 명시한 unit 시작을 시도하지만 실패해도 무시
- `Requisite=`: 명시한 unit이 이미 실행 중이어야만 시작
- `BindsTo=`: `Requires=`보다 강함
  - 명시한 unit이 멈추면 이 unit도 즉시 멈춤
- `PartOf=`: 명시한 unit이 재시작/중지될 때 함께 재시작/중지
- `Conflicts=`: 명시한 unit과 동시에 실행될 수 없음

순서와 요구는 독립적인 관계다. `Requires=foo.service`만 쓰면 foo가 시작되지만 시작 순서는 보장되지 않는다. foo 이후에 시작해야 한다면 `After=foo.service`도 함께 지정해야 한다.

<a id="지원-플랫폼"></a>

### 지원 플랫폼

- Linux 커널 5.10 이상 필요, 5.14 이상 권장
- glibc 2.34 이상 (또는 musl 1.2.6 이상)
- 주요 아키텍처: x86_64 (amd64), i386, aarch64 (arm64), ppc64le (ppc64el), s390x

- systemd는 Linux 전용 → BSD, macOS는 지원 불가

<a id="추가-자료"></a>

### 추가 자료

- [systemd 공식 사이트](https://systemd.io/)
- [systemd GitHub](https://github.com/systemd/systemd)
- [freedesktop.org systemd man pages](https://www.freedesktop.org/software/systemd/man/)
- [Lennart Poettering의 systemd 블로그 시리즈](http://0pointer.de/blog/projects/)

## Unit 파일

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.unit.html

<a id="unit이란"></a>
### Unit이란?

systemd는 데몬 프로세스, 마운트 지점, 소켓, 타이머, 디바이스 등 관리 대상을 unit이라는 단위로 다룬다. 각 unit의 종류는 파일 확장자로 구분한다.

- `.service` (서비스): 데몬 프로세스
- `.socket` (소켓): IPC, 네트워크 소켓 (활성화 트리거)
- `.target` (타겟): unit 그룹화 (런레벨 대체)
- `.mount` (마운트): 파일시스템 마운트
- `.automount` (오토마운트): 온디맨드 마운트
- `.swap` (스왑): 스왑 디바이스, 파일
- `.timer` (타이머): cron 대체
- `.path` (경로): 파일, 디렉터리 변경 감시
- `.slice` (슬라이스): cgroup 계층
- `.scope` (스코프): 외부에서 만든 프로세스 그룹
- `.device` (디바이스): udev가 노출한 디바이스

<a id="unit-파일의-위치"></a>

### Unit 파일의 위치

- systemd는 다음 디렉터리를 우선순위 순으로 검색

#### 시스템 unit

- `/etc/systemd/system/`: 관리자 정의 (가장 높은 우선순위)
- `/run/systemd/system/`: 런타임 생성
- `/usr/lib/systemd/system/`: 패키지 설치 (배포판 기본)

- 같은 이름의 unit이 여러 경로에 존재하면 우선순위가 높은 경로가 우선
  - 예를 들어 `/etc/systemd/system/sshd.service`가 있으면 패키지가 제공한 `/usr/lib/systemd/system/sshd.service`를 완전히 덮어씀

#### 사용자 unit

- `~/.config/systemd/user/`: 사용자 정의
- `/etc/systemd/user/`: 관리자가 모든 사용자에게 배포
- `/usr/lib/systemd/user/`: 패키지 설치

- 사용자 unit은 `systemctl --user`로 제어

<a id="unit-파일-형식"></a>

### Unit 파일 형식

- INI 스타일이며 세 부분으로 구성됨

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

#### 값의 형식

- 문자열: 따옴표 없음
  - 공백 포함 시 `"..."` 또는 `'...'`
- 불리언: `yes`, `no`, `true`, `false`, `on`, `off`, `1`, `0`
- 시간: `30s`, `5min`, `2h`, `1d` (단위 없으면 초)
- 크기: `100M`, `1G`, `500K`
- 리스트: 공백 또는 줄바꿈으로 구분 (지시자를 여러 번 써서 누적 가능)
- 빈 값: 지시자에 빈 값을 할당하면 누적된 리스트 초기화 (drop-in에서 유용)

<a id="unit-섹션"></a>

### [Unit] 섹션

#### 메타데이터

- `Description=`: 사람이 읽을 수 있는 설명
- `Documentation=`: 문서 URL/man 페이지 (예: `man:my-app(8)`)

#### 의존성

- `Requires=`: 강한 의존
- `Wants=`: 약한 의존 (더 안전한 방식으로 권장)
- `Requisite=`: 이미 실행 중이어야 함
- `BindsTo=`: 명시한 unit이 멈추면 즉시 함께 멈춤
- `PartOf=`: 부모 unit 재시작/중지 시 동반
- `Conflicts=`: 동시 실행 불가
- `Before=`, `After=`: 시작 순서 (의존성과 독립)

#### 조건

- 조건이 거짓이면 unit은 시작되지 않고 (실패가 아닌) 건너뜀 처리됨

- `ConditionPathExists=`: 경로 존재 여부
- `ConditionFileNotEmpty=`
- `ConditionDirectoryNotEmpty=`
- `ConditionKernelCommandLine=`: 커널 부트 파라미터
- `ConditionVirtualization=`: VM/컨테이너 종류
- `ConditionArchitecture=`: x86-64, arm64 등
- `ConditionMemory=`: 시스템 메모리 (예: `>=2G`)
- `AssertXxx=`: Condition과 같지만 거짓이면 실패로 표시

#### 기타

- `OnFailure=`: 실패 시 시작할 unit
- `OnSuccess=`: 성공 시 시작할 unit (systemd 249+)
- `RefuseManualStart=`, `RefuseManualStop=`: 수동 제어 차단

<a id="install-섹션"></a>

### [Install] 섹션

- `[Install]`은 `systemctl enable`이 호출될 때만 의미 있음
  - 다른 unit의 의존성에 현재 unit을 추가하는 심볼릭 링크를 생성

- `WantedBy=`: 가장 흔함
  - `multi-user.target.wants/` 에 링크
- `RequiredBy=`: 강한 버전
- `Also=`: enable 시 함께 enable할 unit
- `Alias=`: 별칭 (다른 이름으로도 호출 가능)
- `DefaultInstance=`: 템플릿 unit의 기본 인스턴스

```ini
[Install]
WantedBy=multi-user.target
Alias=myapp.service
```

<a id="drop-in-디렉터리"></a>

### Drop-in 디렉터리

Drop-in 디렉터리는 기존 unit의 일부 옵션을 추가하거나 변경할 때 사용한다. 패키지가 제공한 unit 파일을 직접 수정하지 않으므로 패키지 업그레이드 시 충돌을 피할 수 있다.

```
/etc/systemd/system/<unit-name>.d/
├── override.conf
└── 50-custom-restart.conf
```

- 이 디렉터리의 모든 `.conf` 파일이 알파벳 순으로 적용됨
  - 가장 쉬운 작성법:

```bash
sudo systemctl edit nginx.service
```

- 이 명령은 자동으로 `/etc/systemd/system/nginx.service.d/override.conf`를 생성 → 편집기 실행

#### 리스트 누적 vs 초기화

```ini
[Service]
# 기존 ExecStart에 "추가"가 아닌 "교체"
ExecStart=
ExecStart=/new/path/to/binary
```

- 대부분의 리스트형 지시자(`ExecStart`, `Environment` 등)는 빈 값으로 먼저 초기화하지 않으면 값이 누적됨

<a id="unit-이름과-인스턴스"></a>

### Unit 이름과 인스턴스

#### 일반 unit

- `sshd.service`, `nginx.service` 같은 단순 이름

#### 템플릿 unit

- 이름에 `@`가 포함된 unit으로, 인스턴스 매개변수를 받음

```
getty@.service          (템플릿)
getty@tty1.service      (인스턴스, tty1이 매개변수)
```

- 템플릿 안에서 `%i` (인스턴스 이름), `%I` (이스케이프 해제), `%H` (호스트명) 등의 specifier 사용 가능

```ini
[Unit]
Description=Getty on %I

[Service]
ExecStart=/sbin/agetty %I
```

- `systemctl start getty@tty3.service`로 인스턴스화 가능

#### 주요 specifier

- `%n`: 전체 unit 이름
- `%N`: 이스케이프 해제된 unit 이름
- `%p`: prefix (템플릿 이름의 `@` 앞부분)
- `%i`: instance (템플릿 이름의 `@` 뒤부분)
- `%I`: 이스케이프 해제된 instance
- `%u`: 사용자 이름
- `%h`: 사용자 홈 디렉터리
- `%H`: 호스트명
- `%t`: 런타임 디렉터리 (`/run` 또는 `$XDG_RUNTIME_DIR`)

<a id="상태state"></a>

### 상태(State)

- `systemctl status`가 보여주는 두 가지 상태:

#### LOAD 상태

- `loaded`: unit 파일이 정상적으로 로드됨
- `not-found`: 파일을 찾을 수 없음
- `bad-setting`: 파일에 오류
- `error`: 로드 실패
- `masked`: `/dev/null` 로 가려져 있음 (`systemctl mask`)

#### ACTIVE 상태

- `active (running)`: 실행 중
- `active (exited)`: 한 번 실행하고 정상 종료 (oneshot)
- `active (waiting)`: 이벤트 대기 중 (소켓, 타이머)
- `inactive (dead)`: 중지됨
- `failed`: 실패
- `activating`, `deactivating`: 전이 중

<a id="참고-자료"></a>

### 참고 자료

- [man systemd.unit](https://www.freedesktop.org/software/systemd/man/systemd.unit.html)
- [man systemctl](https://www.freedesktop.org/software/systemd/man/systemctl.html)
- [Drop-in files](https://www.freedesktop.org/software/systemd/man/systemd.unit.html#Description)
