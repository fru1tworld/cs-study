# `/run` — 런타임 상태 (tmpfs)

> "contains information which describes the system since it was booted" — `hier(7)`

부팅 시 비어 있는 **tmpfs** (RAM 기반). 시스템이 켜진 동안만 의미 있는 런타임 데이터를 담고, **재부팅 시 사라진다**. 디스크에 쓸 필요가 없는 PID 파일, 락 파일, 유닉스 도메인 소켓의 자연스러운 거주지.

## 역사적 맥락

옛날 리눅스에서는 같은 목적으로 `/var/run`을 썼다. 문제는:
- `/var`이 마운트되기 전엔 못 씀
- `/var`은 디스크인데 굳이 디스크에 쓸 이유 없음

그래서 부팅 초기부터 사용 가능한 **루트 파일시스템 직속 tmpfs** `/run`이 생겼고, 지금은 표준이다. **`/var/run`은 `/run`의 심볼릭 링크**.

```
$ ls -ld /var/run
lrwxrwxrwx 1 root root 4 ... /var/run -> /run
```

마찬가지로 `/var/lock` → `/run/lock`.

## 구조

```
/run/
├── systemd/                 systemd 런타임
│   ├── system/              유닛 런타임 상태
│   ├── units/               활성 유닛
│   ├── private              systemd 내부 IPC 소켓
│   ├── notify               sd_notify(3) 수신 소켓
│   └── journal/             journald 소켓 + 런타임 저널
├── user/<uid>/              사용자 세션 디렉터리 ($XDG_RUNTIME_DIR)
│   ├── bus                  세션 DBus 소켓
│   ├── dconf/, pulse/, …
│   └── (logind가 로그인 시 생성, 로그아웃 시 정리)
├── lock/                    락 파일들 (예전 /var/lock)
├── log/                     초기 부팅 로그
├── udev/                    udev 런타임 DB
├── dbus/                    시스템 DBus 소켓
├── NetworkManager/
├── containerd/, docker/     컨테이너 런타임 상태
├── lvm/, mdadm/, cryptsetup/
├── sudo/, ssh-*             PID·세션 추적
└── *.pid                    데몬 PID 파일
```

## `XDG_RUNTIME_DIR`

`/run/user/<uid>/` — **로그인 사용자 전용** tmpfs (보통 700 권한). `XDG_RUNTIME_DIR` 환경 변수가 이 경로를 가리키며, 표준에 따라:

- 로그인 시 `systemd-logind`가 생성
- 마지막 세션이 종료되면 정리
- 사용자별 D-Bus 세션 버스, Wayland 소켓, PulseAudio 소켓, 임시 키링 등이 들어감
- 다른 사용자에게 안 보이도록 권한 격리

```bash
$ echo $XDG_RUNTIME_DIR
/run/user/1000
$ ls $XDG_RUNTIME_DIR
bus  dbus-1/  dconf/  gnupg/  keyring/  pulse/  systemd/  wayland-0  ...
```

## 왜 디스크 아닌 tmpfs인가

- **부팅 초기 사용**: `/var`이 마운트되기 전부터 필요
- **속도**: PID 파일 만들고 지우는 데 디스크 I/O 불필요
- **자동 청소**: 재부팅하면 잔재가 사라져 stale state 위험 없음
- **SSD 마모 감소**: 짧은 수명의 파일들이 디스크 쓰기 안 함

## tmpfs 특성 (자세한 건 [tmp.md](tmp.md))

`tmpfs(5)` 공식 요약:
- "a virtual memory filesystem" — RAM에 거주
- 필요한 만큼만 메모리 사용, 스왑으로 옮길 수 있음
- remount로 크기 변경 가능 (데이터 손실 없음)
- 언마운트 시 내용 전부 소실

기본 크기는 보통 RAM의 일정 비율로 systemd가 설정 (`/etc/fstab` 또는 `systemd-tmpfiles`).

## `tmpfiles.d`로 관리

`/run`이 매 부팅마다 비어 있다는 건, **필요한 디렉터리/파일은 부팅 때마다 다시 만들어야** 한다는 뜻. systemd의 `tmpfiles.d`가 이를 담당:

```
/usr/lib/tmpfiles.d/systemd.conf
/etc/tmpfiles.d/
/run/tmpfiles.d/
```

설정 예 (`man tmpfiles.d`):
```
# Type  Path                Mode  User  Group  Age  Argument
d       /run/foo            0755  root  root   -
d       /run/user/%U        0700  %u    %g     -
f       /run/myservice.pid  0644  root  root   -
```

부팅 시 `systemd-tmpfiles --create`가 이걸 읽어 필요한 노드를 생성한다.

## 확인 명령

```bash
# /run 사이즈/사용량
df -h /run

# tmpfs인지 확인
findmnt /run
# TARGET SOURCE FSTYPE OPTIONS
# /run   tmpfs  tmpfs  rw,nosuid,nodev,size=...,mode=755

# 내 XDG_RUNTIME_DIR
echo $XDG_RUNTIME_DIR
ls -la /run/user/$(id -u)/

# 어떤 데몬이 PID 파일을 어디에 두는지
ls /run/*.pid
```
