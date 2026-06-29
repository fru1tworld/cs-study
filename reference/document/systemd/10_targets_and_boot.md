# Target Unit과 부팅 시퀀스

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.target.html , https://www.freedesktop.org/software/systemd/man/bootup.html

---

## 목차

1. [Target이란?](#target이란)
2. [표준 target](#표준-target)
3. [Default target](#default-target)
4. [부팅 시퀀스](#부팅-시퀀스)
5. [Isolate](#isolate)
6. [Rescue / Emergency](#rescue--emergency)
7. [커스텀 target](#커스텀-target)
8. [참고 자료](#참고-자료)

---

## Target이란?

`.target` unit은 다른 unit들의 **그룹화·동기화 지점** 입니다. 그 자체로는 아무것도 실행하지 않지만, 의존성 그래프의 노드 역할을 합니다.

SysV init의 **런레벨(runlevel)** 과 유사한 개념을 일반화한 것입니다:
- 런레벨 3 (multi-user) → `multi-user.target`
- 런레벨 5 (graphical) → `graphical.target`

target은 보통 다음과 같이 사용됩니다:
- "X가 준비된 상태"를 의미하는 마커 (`network-online.target`)
- 부팅 단계를 표현 (`basic.target`, `multi-user.target`)
- 사용자 지정 그룹화 (`my-app-stack.target`)

---

## 표준 target

| Target | 의미 |
| --- | --- |
| `default.target` | 부팅의 최종 목적지 (보통 multi-user나 graphical로 심볼릭 링크) |
| `graphical.target` | GUI 로그인이 가능한 상태 |
| `multi-user.target` | 여러 사용자가 로그인 가능, 네트워크 활성, GUI 없음 |
| `rescue.target` | 단일 사용자, 네트워크 없음, 기본 파일시스템만 마운트 |
| `emergency.target` | 최소 환경, 루트만 read-only로 마운트, 셸만 실행 |
| `network.target` | 네트워크 관리 데몬이 시작된 상태 (네트워크 사용 가능 보장 ✗) |
| `network-online.target` | 네트워크 연결이 실제로 가능한 상태 |
| `basic.target` | 거의 모든 부팅 초기화 완료 (sockets, sysinit, paths, slices, timers) |
| `sysinit.target` | 마운트, swap, fsck 등 초기 시스템 작업 완료 |
| `local-fs.target` | 로컬 파일시스템 마운트 완료 |
| `remote-fs.target` | 원격 파일시스템 마운트 완료 |
| `swap.target` | swap 활성화 완료 |
| `sockets.target` | 모든 소켓 활성화 됨 |
| `timers.target` | 모든 타이머 활성화 됨 |
| `paths.target` | 모든 path unit 활성화 됨 |
| `shutdown.target` | 종료 시작 |
| `umount.target` | 마운트 해제 단계 |
| `reboot.target` | 재부팅 |
| `poweroff.target` | 전원 끄기 |
| `halt.target` | 정지 (전원 유지) |
| `kexec.target` | kexec 재부팅 |
| `suspend.target`, `hibernate.target` | 절전/최대절전 |

---

## Default target

기본 target은 `/etc/systemd/system/default.target` 심볼릭 링크가 가리키는 곳입니다.

### 현재 default 확인

```bash
$ systemctl get-default
graphical.target
```

### 변경

```bash
sudo systemctl set-default multi-user.target
```

이 명령은 심볼릭 링크를 변경합니다. 다음 부팅부터 적용됩니다.

### 일회성 변경

부트로더(GRUB)에서 커널 파라미터에 `systemd.unit=multi-user.target` 추가.

---

## 부팅 시퀀스

systemd 부팅 단계는 대략 다음 순서로 진행됩니다.

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

각 target은 **이전 단계의 완료를 보장**합니다. 예를 들어 `multi-user.target`에 등록된 서비스는 `basic.target`에 도달한 후에만 시작됩니다.

### After=와 Wants= 관계

- 일반 서비스가 부팅 중 시작되려면 `WantedBy=multi-user.target` 같은 `[Install]` 이 필요
- 시작 순서 제어는 `After=`, `Before=`, `Requires=` 로

### network-online.target

네트워크가 필요한 서비스의 가장 흔한 함정:

```ini
[Unit]
After=network.target           # ❌ 네트워크 사용 가능을 보장 안 함
After=network-online.target    # ✓ 실제 연결 보장
Wants=network-online.target    # ✓ 이 target을 끌어옴 (꼭 필요)
```

`network-online.target`은 자동으로 활성화되지 않으므로 `Wants=`도 함께 명시해야 합니다.

---

## Isolate

`systemctl isolate <target>`은 **지정한 target과 그 의존성만 활성 상태로 유지하고, 나머지는 모두 중지**합니다.

```bash
sudo systemctl isolate multi-user.target   # GUI 끄기
sudo systemctl isolate rescue.target       # 단일 사용자 모드
sudo systemctl isolate graphical.target    # 다시 GUI로
```

isolate 대상이 되려면 해당 target에 `AllowIsolate=yes`가 설정되어 있어야 합니다.

### isolate 단축 명령

| 명령 | 동작 |
| --- | --- |
| `systemctl rescue` | rescue.target으로 isolate |
| `systemctl emergency` | emergency.target으로 |
| `systemctl reboot` | reboot.target으로 |
| `systemctl poweroff` | poweroff.target으로 |
| `systemctl halt` | halt.target으로 |
| `systemctl suspend` | 절전 |
| `systemctl hibernate` | 최대절전 |

---

## Rescue / Emergency

부팅이 실패하거나 시스템에 문제가 생겼을 때 사용합니다.

### Rescue 모드

- `sysinit.target` 까지는 진행 (마운트, fsck 완료)
- 네트워크와 일반 서비스는 시작되지 않음
- 루트 셸 제공
- 부트로더에서 `systemd.unit=rescue.target` 으로 진입

### Emergency 모드

- 루트 파일시스템만 read-only로 마운트
- 거의 아무것도 시작되지 않음
- 가장 최소한의 환경
- `systemd.unit=emergency.target` 으로 진입

부트로더 GRUB에서 커널 라인 끝에 `systemd.unit=emergency.target` 또는 짧게 `single` (rescue로 매핑) 입력.

---

## 커스텀 target

여러 unit을 묶어 한꺼번에 시작/중지하고 싶을 때 사용합니다.

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

이제 `systemctl start myapp.target` 명령 하나로 스택 전체를 제어할 수 있습니다.

### Drop-in으로 target 확장

기존 `multi-user.target`에 의존성을 추가하려면:

```ini
# /etc/systemd/system/multi-user.target.d/extra.conf
[Unit]
Wants=my-extra.service
```

---

## 부팅 분석

부팅 시간 측정에는 `systemd-analyze`가 유용합니다.

```bash
$ systemd-analyze
Startup finished in 1.123s (kernel) + 4.567s (userspace) = 5.690s
graphical.target reached after 4.567s in userspace.

$ systemd-analyze blame        # 가장 오래 걸린 unit
$ systemd-analyze critical-chain     # 부팅 의존성 critical path
$ systemd-analyze plot > boot.svg    # 시각화 (Gantt 차트)
```

---

## 참고 자료

- [man systemd.target](https://www.freedesktop.org/software/systemd/man/systemd.target.html)
- [man bootup](https://www.freedesktop.org/software/systemd/man/bootup.html)
- [man systemd-analyze](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
