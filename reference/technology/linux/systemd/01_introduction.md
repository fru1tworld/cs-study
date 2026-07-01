# systemd 소개

> 원본: https://systemd.io/ , https://www.freedesktop.org/wiki/Software/systemd/

---

## 목차

1. [systemd란?](#systemd란)
2. [설계 철학](#설계-철학)
3. [SysV init과의 차이](#sysv-init과의-차이)
4. [구성 요소](#구성-요소)
5. [PID 1로서의 역할](#pid-1로서의-역할)
6. [의존성 모델](#의존성-모델)
7. [지원 플랫폼](#지원-플랫폼)
8. [추가 자료](#추가-자료)

---

## systemd란?

systemd는 Linux 시스템의 **시스템·서비스 관리자(System and Service Manager)** 입니다. PID 1로 부팅되어 사용자 공간(user space)을 초기화하고, 모든 서비스의 라이프사이클을 관리하며, 로깅·네트워킹·로그인·디바이스 관리 등 OS의 기본 기능을 제공하는 통합 플랫폼 역할을 합니다.

원래 Lennart Poettering이 Red Hat에서 시작한 프로젝트로, 현재 거의 모든 주요 Linux 배포판(Debian, Ubuntu, Fedora, RHEL, Arch, openSUSE)에서 표준 init 시스템으로 채택되어 있습니다.

### systemd의 범위

systemd는 단순한 init 시스템이 아니라 다음과 같은 영역을 포괄합니다:

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

---

## 설계 철학

systemd는 다음과 같은 원칙을 기반으로 설계되었습니다.

### 1. 적극적인 병렬화 (Aggressive Parallelization)

전통적인 SysV init은 스크립트를 순차적으로 실행했지만, systemd는 가능한 모든 서비스를 병렬로 시작합니다. **소켓 기반 활성화(socket activation)** 와 **D-Bus 활성화** 를 통해 의존성이 있는 서비스조차 병렬 부팅이 가능합니다.

### 2. 온디맨드 시작 (On-demand Activation)

서비스가 실제로 필요할 때만 시작됩니다. 예를 들어 SSH 데몬은 누군가 22번 포트에 연결하기 전까지는 실행되지 않을 수 있습니다. 이는 부팅 시간을 단축하고 메모리 사용량을 줄여줍니다.

### 3. 선언적 구성 (Declarative Configuration)

서비스를 어떻게(how) 시작할지 스크립트로 작성하는 대신, 원하는 상태(what)를 unit 파일이라는 단순한 INI 형식으로 선언합니다.

### 4. 의존성 추적

서비스 간 의존 관계를 명시적으로 선언하면 systemd가 시작 순서, 재시작 전파, 실패 처리를 자동으로 관리합니다.

### 5. 리소스 격리 (cgroups 기반)

각 서비스는 cgroup으로 격리되며, CPU·메모리·IO 제한을 unit 파일에서 직접 지정할 수 있습니다. 서비스가 fork한 프로세스도 cgroup을 통해 추적되므로 더블 fork로 init에서 탈출하는 트릭이 통하지 않습니다.

---

## SysV init과의 차이

| 항목 | SysV init | systemd |
| --- | --- | --- |
| 구성 형식 | shell 스크립트 (`/etc/init.d/`) | unit 파일 (INI 형식) |
| 시작 방식 | 순차적 | 병렬 + 의존성 기반 |
| 프로세스 추적 | PID 파일 (불안정) | cgroup |
| 로깅 | syslog 외부 의존 | journald 통합 |
| 활성화 | 부팅 시 모두 시작 | 소켓/D-Bus/path 기반 lazy |
| 재시작 | 수동 | 정책 기반 자동 |
| 타이머 | cron 별도 | systemd.timer 통합 |

---

## 구성 요소

systemd 프로젝트는 여러 바이너리와 라이브러리로 구성됩니다.

### 핵심 데몬

- `systemd` (PID 1): 시스템 매니저
- `systemd --user`: 사용자별 매니저
- `systemd-journald`: 구조화된 로그 수집
- `systemd-logind`: 사용자 로그인 세션 관리
- `systemd-udevd`: 디바이스 이벤트 관리
- `systemd-networkd`: 네트워크 구성 (선택적)
- `systemd-resolved`: DNS 해석 (선택적)
- `systemd-timesyncd`: SNTP 시간 동기화 (선택적)

### 주요 명령어

- `systemctl`: 서비스/unit 제어
- `journalctl`: 로그 조회
- `loginctl`: 로그인 세션 조회
- `hostnamectl`, `timedatectl`, `localectl`: 호스트 정보 설정
- `systemd-analyze`: 부팅 분석
- `systemd-run`: 임시 unit 실행
- `bootctl`: 부트로더 관리
- `coredumpctl`: 코어 덤프 조회

---

## PID 1로서의 역할

PID 1은 Linux 커널이 부팅 마지막 단계에서 실행하는 첫 번째 사용자 공간 프로세스입니다. PID 1이 종료되면 커널 패닉이 발생하므로 매우 안정적이어야 합니다.

systemd가 PID 1로서 수행하는 일:

1. **부팅 시퀀스 조정**: 마운트, fsck, swap 활성화, 서비스 시작
2. **자식 프로세스 수집(reap)**: 고아 프로세스의 종료 상태 회수
3. **시그널 라우팅**: SIGTERM/SIGINT 등 처리
4. **소켓·디바이스 이벤트 디스패치**: 활성화 트리거
5. **시스템 종료**: 깨끗한 셧다운/재부팅

systemd는 D-Bus 인터페이스(`org.freedesktop.systemd1`)를 통해 다른 프로세스와 통신하며, `systemctl`도 내부적으로는 이 D-Bus API를 호출합니다.

---

## 의존성 모델

systemd의 unit은 다른 unit과 다양한 관계를 맺을 수 있습니다.

### 순서(Ordering) 의존성

- `Before=`: 이 unit이 명시한 unit보다 먼저 시작
- `After=`: 이 unit이 명시한 unit 이후에 시작

### 요구(Requirement) 의존성

- `Requires=`: 강한 의존. 명시한 unit이 실패하면 이 unit도 중단
- `Wants=`: 약한 의존. 명시한 unit 시작을 시도하지만 실패해도 무시
- `Requisite=`: 명시한 unit이 이미 실행 중이어야만 시작
- `BindsTo=`: `Requires=`보다 강함. 명시한 unit이 멈추면 이 unit도 즉시 멈춤
- `PartOf=`: 명시한 unit이 재시작/중지될 때 함께 재시작/중지
- `Conflicts=`: 명시한 unit과 동시에 실행될 수 없음

순서와 요구는 **독립적** 이라는 점이 중요합니다. `Requires=foo.service` 만 쓰면 foo가 시작되지만 순서는 보장되지 않습니다. 순서가 필요하면 `After=foo.service` 도 함께 명시해야 합니다.

---

## 지원 플랫폼

- Linux 커널 5.10 이상 필요, 5.14 이상 권장
- glibc 2.34 이상 (또는 musl 1.2.6 이상)
- 주요 아키텍처: x86_64 (amd64), i386, aarch64 (arm64), ppc64le (ppc64el), s390x

systemd는 Linux 전용입니다. BSD나 macOS는 지원하지 않습니다.

---

## 추가 자료

- [systemd 공식 사이트](https://systemd.io/)
- [systemd GitHub](https://github.com/systemd/systemd)
- [freedesktop.org systemd man pages](https://www.freedesktop.org/software/systemd/man/)
- [Lennart Poettering의 systemd 블로그 시리즈](http://0pointer.de/blog/projects/)
