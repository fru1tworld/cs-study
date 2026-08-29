# Mount·Automount Unit, Target Unit과 부팅 시퀀스

## Mount와 Automount Unit

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.mount.html , https://www.freedesktop.org/software/systemd/man/systemd.automount.html

---

### 목차

1. [Mount unit](#mount-unit)
2. [fstab과의 관계](#fstab과의-관계)
3. [Mount 옵션](#mount-옵션)
4. [Automount unit](#automount-unit)
5. [Idle Timeout](#idle-timeout)
6. [실전 예제](#실전-예제)
7. [참고 자료](#참고-자료)

---

### Mount unit

`.mount` unit은 파일시스템 마운트를 표현함. unit 이름은 마운트 지점을 이스케이프한 형태로, 슬래시는 `-`로, 그 외 특수 문자는 `\xNN` 형식으로 변환됨.

마운트 지점 → unit 이름 매핑:
- `/` → `-.mount`
- `/home` → `home.mount`
- `/var/lib/docker` → `var-lib-docker.mount`
- `/mnt/data 1` → `mnt-data\x201.mount`

이스케이프는 손으로 하기 어려우니 `systemd-escape` 사용:

```bash
$ systemd-escape --path /var/lib/docker
var-lib-docker
$ systemd-escape --path --suffix=mount /var/lib/docker
var-lib-docker.mount
```

---

### fstab과의 관계

systemd는 부팅 시 `/etc/fstab` 을 읽어 자동으로 mount unit을 생성함. 즉 대부분의 경우 `.mount` 파일을 직접 작성할 필요 없이 fstab만 쓰면 됨.

```fstab
# <device>      <mountpoint>    <fstype>  <options>                          <dump> <pass>
UUID=abc-123    /data           xfs       defaults,nofail,x-systemd.device-timeout=30  0      2
//srv/share     /mnt/share      cifs      _netdev,credentials=/etc/smbcred   0      0
```

#### x-systemd.* 옵션

fstab의 옵션 필드에 systemd 전용 옵션을 넣을 수 있음.

- `x-systemd.device-timeout=30`: 디바이스 대기 타임아웃
- `x-systemd.requires=foo.service`: 이 mount의 Requires=
- `x-systemd.before=bar.service`: 이 mount Before=
- `x-systemd.after=baz.service`: 이 mount After=
- `x-systemd.automount`: 자동으로 automount unit 생성
- `x-systemd.idle-timeout=600`: automount idle timeout
- `x-systemd.mount-timeout=10`: mount() 타임아웃
- `_netdev`: 네트워크 마운트 (네트워크 이후)
- `nofail`: 실패해도 부팅 계속
- `noauto`: 부팅 시 자동 마운트 안 함

`fstab` 변경 후 즉시 반영하려면:

```bash
sudo systemctl daemon-reload
sudo systemctl restart local-fs.target   # 주의: 마운트 재배치 발생
```

---

### Mount 옵션

직접 `.mount` 파일을 쓸 때:

```ini
# /etc/systemd/system/srv-data.mount
[Unit]
Description=Data Volume
Requires=network-online.target
After=network-online.target

[Mount]
What=//fileserver/data
Where=/srv/data
Type=cifs
Options=credentials=/etc/cifs.cred,_netdev
TimeoutSec=30

[Install]
WantedBy=multi-user.target
```

#### 핵심 지시자

- `What=`: 디바이스나 원격 위치 (예: `/dev/sda1`, `UUID=...`, `LABEL=...`, `//host/share`)
- `Where=`: 마운트 지점 (unit 이름과 일치해야 함)
- `Type=`: 파일시스템 종류 (`ext4`, `xfs`, `nfs`, `cifs`, `tmpfs`, ...)
- `Options=`: 마운트 옵션 (콤마 구분)
- `SloppyOptions=yes`: 알 수 없는 옵션 무시
- `LazyUnmount=yes`: `umount -l` 사용
- `ForceUnmount=yes`: `umount -f` 사용
- `ReadWriteOnly=yes`: read-only 자동 fallback 비활성화
- `TimeoutSec=`: mount 타임아웃

#### 의존성 자동 추가

systemd는 다음을 자동으로 추가함:
- `Before=local-fs.target` (또는 네트워크 마운트는 `remote-fs.target`)
- `After=` 디바이스가 등장한 시점
- `Requires=` 디바이스 unit

---

### Automount unit

`.automount` unit은 마운트 지점에 접근하는 순간 마운트를 트리거함. 사용하지 않는 동안에는 마운트가 해제되므로, NFS나 CIFS처럼 사용 빈도가 낮은 마운트에 유용.

작동 방식:
- automount unit만 부팅 시 활성화
- 프로세스가 마운트 지점에 접근 (`ls /srv/data`)
- 커널이 systemd에 알림
- systemd가 mount unit 활성화
- 마운트 완료 후 원래 요청 처리 재개

```ini
# /etc/systemd/system/srv-data.automount
[Unit]
Description=Automount for /srv/data

[Automount]
Where=/srv/data
TimeoutIdleSec=600

[Install]
WantedBy=multi-user.target
```

이 automount는 같은 이름의 `srv-data.mount` 와 짝이 됨.

> 주의: automount unit만 enable하면 됨. mount unit은 enable하지 않음.

---

### Idle Timeout

```ini
TimeoutIdleSec=10min
```

마지막 접근 후 이 시간이 지나면 자동으로 unmount됨. `0`으로 설정하면 idle timeout 비활성화(수동 unmount만 가능).

NFS 마운트가 많은 서버에서 idle timeout을 짧게 두면 메모리/소켓 부담이 줄어듦.

---

### 실전 예제

#### 1. fstab 한 줄로 NFS automount

```fstab
nfsserver:/data  /mnt/data  nfs  _netdev,x-systemd.automount,x-systemd.idle-timeout=300  0  0
```

이 한 줄로 systemd가 mount + automount 두 unit을 자동 생성함.

#### 2. tmpfs 마운트

```ini
# /etc/systemd/system/var-cache-myapp.mount
[Unit]
Description=tmpfs for myapp cache
DefaultDependencies=no
Conflicts=umount.target
Before=local-fs.target umount.target

[Mount]
What=tmpfs
Where=/var/cache/myapp
Type=tmpfs
Options=size=512M,mode=0755,uid=myapp,gid=myapp

[Install]
WantedBy=local-fs.target
```

#### 3. 의존성 있는 마운트

```ini
[Unit]
Requires=vpn.service
After=vpn.service

[Mount]
What=//office.local/share
Where=/mnt/office
Type=cifs
Options=credentials=/etc/cifs.cred
```

VPN이 올라간 뒤에만 마운트 시도.

#### 4. 네트워크 디스크 자동 마운트 (사용자별)

`~/.config/systemd/user/mnt-cloud.mount` 와 `mnt-cloud.automount` 를 만들고:

```bash
systemctl --user enable --now mnt-cloud.automount
```

---

### 관리 명령

#### 현재 마운트 보기

```bash
systemctl list-units --type=mount
findmnt
```

#### 강제 unmount

```bash
sudo systemctl stop srv-data.mount
```

#### 디버깅

```bash
journalctl -u srv-data.mount
journalctl -u srv-data.automount
```

#### fstab 변경 즉시 반영

```bash
sudo systemctl daemon-reload
sudo mount -a   # fstab 기반 마운트만
```

---

### 참고 자료

- [man systemd.mount](https://www.freedesktop.org/software/systemd/man/systemd.mount.html)
- [man systemd.automount](https://www.freedesktop.org/software/systemd/man/systemd.automount.html)
- [man systemd-fstab-generator](https://www.freedesktop.org/software/systemd/man/systemd-fstab-generator.html)

---

## Target Unit과 부팅 시퀀스

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.target.html , https://www.freedesktop.org/software/systemd/man/bootup.html

---

### 목차

1. [Target이란?](#target이란)
2. [표준 target](#표준-target)
3. [Default target](#default-target)
4. [부팅 시퀀스](#부팅-시퀀스)
5. [Isolate](#isolate)
6. [Rescue / Emergency](#rescue--emergency)
7. [커스텀 target](#커스텀-target)
8. [참고 자료](#참고-자료)

---

### Target이란?

`.target` unit은 다른 unit들의 그룹화·동기화 지점. 그 자체로는 아무것도 실행하지 않지만, 의존성 그래프의 노드 역할을 함.

SysV init의 런레벨(runlevel)과 유사한 개념을 일반화한 것:
- 런레벨 3 (multi-user) → `multi-user.target`
- 런레벨 5 (graphical) → `graphical.target`

target은 보통 다음과 같이 사용됨:
- "X가 준비된 상태"를 의미하는 마커 (`network-online.target`)
- 부팅 단계를 표현 (`basic.target`, `multi-user.target`)
- 사용자 지정 그룹화 (`my-app-stack.target`)

---

### 표준 target

- `default.target`: 부팅의 최종 목적지 (보통 multi-user나 graphical로 심볼릭 링크)
- `graphical.target`: GUI 로그인이 가능한 상태
- `multi-user.target`: 여러 사용자가 로그인 가능, 네트워크 활성, GUI 없음
- `rescue.target`: 단일 사용자, 네트워크 없음, 기본 파일시스템만 마운트
- `emergency.target`: 최소 환경, 루트만 read-only로 마운트, 셸만 실행
- `network.target`: 네트워크 관리 데몬이 시작된 상태 (네트워크 사용 가능 보장 안 함)
- `network-online.target`: 네트워크 연결이 실제로 가능한 상태
- `basic.target`: 거의 모든 부팅 초기화 완료 (sockets, sysinit, paths, slices, timers)
- `sysinit.target`: 마운트, swap, fsck 등 초기 시스템 작업 완료
- `local-fs.target`: 로컬 파일시스템 마운트 완료
- `remote-fs.target`: 원격 파일시스템 마운트 완료
- `swap.target`: swap 활성화 완료
- `sockets.target`: 모든 소켓 활성화됨
- `timers.target`: 모든 타이머 활성화됨
- `paths.target`: 모든 path unit 활성화됨
- `shutdown.target`: 종료 시작
- `umount.target`: 마운트 해제 단계
- `reboot.target`: 재부팅
- `poweroff.target`: 전원 끄기
- `halt.target`: 정지 (전원 유지)
- `kexec.target`: kexec 재부팅
- `suspend.target`, `hibernate.target`: 절전/최대절전

---

### Default target

기본 target은 `/etc/systemd/system/default.target` 심볼릭 링크가 가리키는 곳.

#### 현재 default 확인

```bash
$ systemctl get-default
graphical.target
```

#### 변경

```bash
sudo systemctl set-default multi-user.target
```

이 명령은 심볼릭 링크를 변경함. 다음 부팅부터 적용됨.

#### 일회성 변경

부트로더(GRUB)에서 커널 파라미터에 `systemd.unit=multi-user.target` 추가.

---

### 부팅 시퀀스

systemd 부팅 단계는 대략 다음 순서로 진행됨.

```
                      cryptsetup-pre.target
                                  |
       (initrd가 cryptsetup-pre.target에 연결한다면)
                                  v
                          cryptsetup.target
        (네트워크 디바이스 등 모든 디바이스 등장)
                                  |
                                  v
                          local-fs-pre.target
                                  |
                                  v  (모든 mount unit이 After=)
                            local-fs.target
                                  |
                                  v
                            sysinit.target
                                  |
                  +-------+-----+-----+-------+
                  v       v     v     v       v
             timers.target | sockets.target  | paths.target
                  |        |       |          |
                  +-------+-----+-----+-------+
                                  |
                                  v
                              basic.target
                                  |
                                  v
                          multi-user.target
                                  |
                                  v
                          graphical.target
                                  |
                                  v
                          default.target
```

각 target은 이전 단계의 완료를 보장함. 예를 들어 `multi-user.target`에 등록된 서비스는 `basic.target`에 도달한 후에만 시작됨.

#### After=와 Wants= 관계

- 일반 서비스가 부팅 중 시작되려면 `WantedBy=multi-user.target` 같은 `[Install]` 필요
- 시작 순서 제어는 `After=`, `Before=`, `Requires=` 로

#### network-online.target

네트워크가 필요한 서비스의 가장 흔한 함정:

```ini
[Unit]
After=network.target           # 금지: 네트워크 사용 가능을 보장 안 함
After=network-online.target    # 허용: 실제 연결 보장
Wants=network-online.target    # 허용: 이 target을 끌어옴 (꼭 필요)
```

`network-online.target`은 자동으로 활성화되지 않으므로 `Wants=`도 함께 명시 필요.

---

### Isolate

`systemctl isolate <target>`은 지정한 target과 그 의존성만 활성 상태로 유지하고, 나머지는 모두 중지함.

```bash
sudo systemctl isolate multi-user.target   # GUI 끄기
sudo systemctl isolate rescue.target       # 단일 사용자 모드
sudo systemctl isolate graphical.target    # 다시 GUI로
```

isolate 대상이 되려면 해당 target에 `AllowIsolate=yes` 설정 필요.

#### isolate 단축 명령

- `systemctl rescue`: rescue.target으로 isolate
- `systemctl emergency`: emergency.target으로
- `systemctl reboot`: reboot.target으로
- `systemctl poweroff`: poweroff.target으로
- `systemctl halt`: halt.target으로
- `systemctl suspend`: 절전
- `systemctl hibernate`: 최대절전

---

### Rescue / Emergency

부팅이 실패하거나 시스템에 문제가 생겼을 때 사용.

#### Rescue 모드

- `sysinit.target` 까지는 진행 (마운트, fsck 완료)
- 네트워크와 일반 서비스는 시작되지 않음
- 루트 셸 제공
- 부트로더에서 `systemd.unit=rescue.target` 으로 진입

#### Emergency 모드

- 루트 파일시스템만 read-only로 마운트
- 거의 아무것도 시작되지 않음
- 가장 최소한의 환경
- `systemd.unit=emergency.target` 으로 진입

부트로더 GRUB에서 커널 라인 끝에 `systemd.unit=emergency.target` 또는 짧게 `single` (rescue로 매핑) 입력.

---

### 커스텀 target

여러 unit을 묶어 한꺼번에 시작/중지하고 싶을 때 사용.

```ini
# /etc/systemd/system/myapp.target
[Unit]
Description=My App Stack
Requires=postgresql.service redis.service
After=postgresql.service redis.service
Wants=myapp-web.service myapp-worker.service
AllowIsolate=no
```

각 서비스에:

```ini
[Install]
WantedBy=myapp.target
```

이제 `systemctl start myapp.target` 명령 하나로 스택 전체를 제어 가능.

#### Drop-in으로 target 확장

기존 `multi-user.target`에 의존성을 추가하려면:

```ini
# /etc/systemd/system/multi-user.target.d/extra.conf
[Unit]
Wants=my-extra.service
```

---

### 부팅 분석

부팅 시간 측정에는 `systemd-analyze`가 유용.

```bash
$ systemd-analyze
Startup finished in 1.123s (kernel) + 4.567s (userspace) = 5.690s
graphical.target reached after 4.567s in userspace.

$ systemd-analyze blame        # 가장 오래 걸린 unit
$ systemd-analyze critical-chain     # 부팅 의존성 critical path
$ systemd-analyze plot > boot.svg    # 시각화 (Gantt 차트)
```

---

### 참고 자료

- [man systemd.target](https://www.freedesktop.org/software/systemd/man/systemd.target.html)
- [man bootup](https://www.freedesktop.org/software/systemd/man/bootup.html)
- [man systemd-analyze](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
