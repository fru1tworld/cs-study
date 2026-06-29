# 실행 환경과 샌드박스

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.exec.html

---

## 목차

1. [개요](#개요)
2. [사용자와 그룹](#사용자와-그룹)
3. [작업 디렉터리와 루트](#작업-디렉터리와-루트)
4. [Capabilities](#capabilities)
5. [네임스페이스 격리](#네임스페이스-격리)
6. [파일시스템 보호](#파일시스템-보호)
7. [시스템 콜 필터링](#시스템-콜-필터링)
8. [기타 보안 옵션](#기타-보안-옵션)
9. [systemd-analyze security](#systemd-analyze-security)
10. [실전 하드닝 예제](#실전-하드닝-예제)
11. [참고 자료](#참고-자료)

---

## 개요

`systemd.exec` 는 서비스(`.service`), 소켓(`.socket`), 마운트(`.mount`), 스왑(`.swap`) 등 **프로세스를 실제로 실행하는 unit** 의 실행 환경을 통제하는 옵션들을 정의합니다.

이 옵션들의 핵심 가치는 **서비스 하드닝(hardening)** 입니다. 별도의 SELinux/AppArmor 정책 없이도 systemd만으로 강력한 샌드박싱을 구성할 수 있습니다. 내부적으로는 Linux 커널의 capabilities, namespaces, seccomp, mount propagation 등을 조합해 동작합니다.

---

## 사용자와 그룹

```ini
[Service]
User=myapp
Group=myapp
SupplementaryGroups=video render
DynamicUser=yes
```

- `User=`, `Group=`: 서비스를 실행할 UID/GID. 미지정 시 root.
- `SupplementaryGroups=`: 추가 그룹 (디바이스 접근 등)
- `DynamicUser=yes`: **동적 사용자**. 서비스 실행 시 임시 UID 할당, 종료 시 회수. `/etc/passwd` 를 더럽히지 않음. 강력 권장.
- `UMask=0077`: 파일 생성 마스크

`DynamicUser=yes` 는 다음 설정을 자동으로 적용합니다:
- `RemoveIPC=yes`
- `ProtectSystem=strict`
- `ProtectHome=read-only`
- `NoNewPrivileges=yes`
- `RestrictSUIDSGID=yes`

---

## 작업 디렉터리와 루트

```ini
WorkingDirectory=/var/lib/myapp
RootDirectory=/var/empty
RootImage=/usr/share/myapp/myapp.raw
```

- `WorkingDirectory=`: chdir
- `RootDirectory=`: chroot. 서비스가 이 경로를 root(/)로 보게 됨
- `RootImage=`: 디스크 이미지(raw, GPT)를 루트로 사용
- `RootImageOptions=`: 이미지 마운트 옵션
- `RootHash=`: dm-verity 무결성 검증

### State/Cache/Logs/Configuration 디렉터리

systemd가 자동으로 디렉터리를 만들고 권한을 설정해주는 옵션:

```ini
StateDirectory=myapp
CacheDirectory=myapp
LogsDirectory=myapp
ConfigurationDirectory=myapp
RuntimeDirectory=myapp
```

각각 `/var/lib/myapp`, `/var/cache/myapp`, `/var/log/myapp`, `/etc/myapp`, `/run/myapp` 디렉터리가 자동으로 만들어지고 `User=` 권한이 적용됩니다.

`RuntimeDirectoryPreserve=yes` 를 주면 서비스 재시작 사이에 `/run/myapp` 이 유지됩니다.

---

## Capabilities

Linux capabilities는 root의 권한을 세밀하게 쪼갠 것입니다. systemd는 서비스가 가질 수 있는 capability를 제한할 수 있습니다.

```ini
CapabilityBoundingSet=CAP_NET_BIND_SERVICE
AmbientCapabilities=CAP_NET_BIND_SERVICE
NoNewPrivileges=yes
```

- `CapabilityBoundingSet=`: 이 capability **이외에는 절대 가질 수 없음**. 빈 문자열을 할당하면 bounding set을 완전히 비울 수 있음
- `AmbientCapabilities=`: 비-root 프로세스에 부여할 capability
- `NoNewPrivileges=yes`: setuid 바이너리·파일 capability·SELinux transition으로도 권한 상승 불가. **거의 모든 서비스에 권장**

### 자주 쓰는 capability

| capability | 용도 |
| --- | --- |
| `CAP_NET_BIND_SERVICE` | 1024 미만 포트 바인딩 |
| `CAP_NET_RAW` | raw 소켓 (ping, tcpdump) |
| `CAP_NET_ADMIN` | 네트워크 인터페이스 설정 |
| `CAP_SYS_ADMIN` | 광범위 (마운트, namespace 등) |
| `CAP_SYS_PTRACE` | strace, gdb 등 |
| `CAP_DAC_READ_SEARCH` | 모든 파일 읽기 권한 |
| `CAP_CHOWN` | 파일 소유권 변경 |

### 모든 capability 제거 (가장 안전)

```ini
CapabilityBoundingSet=
AmbientCapabilities=
```

---

## 네임스페이스 격리

systemd는 Linux namespaces를 활용해 서비스를 격리할 수 있습니다.

### Mount namespace

```ini
PrivateTmp=yes
PrivateDevices=yes
ProtectHome=yes
ProtectSystem=strict
```

- `PrivateTmp=yes`: 서비스 전용 `/tmp`, `/var/tmp` 제공. 다른 서비스와 격리
- `PrivateDevices=yes`: `/dev` 에 기본 디바이스(null, zero, random, ...)만 노출. 디스크/네트워크 디바이스 차단
- `ProtectHome=yes`: `/home`, `/root`, `/run/user` 를 빈 디렉터리로 마운트. `read-only` 또는 `tmpfs` 도 가능
- `ProtectSystem=`:
  - `true`: `/usr`, `/boot`, `/efi` 읽기 전용
  - `full`: 위 + `/etc` 읽기 전용
  - `strict`: 거의 모든 파일시스템 읽기 전용

### Network namespace

```ini
PrivateNetwork=yes
```

서비스가 자체 네트워크 namespace를 가져 호스트 네트워크에 접근 불가. 루프백만 사용 가능. 외부 통신이 필요 없는 서비스에 매우 강력.

### User namespace

```ini
PrivateUsers=yes
```

서비스가 자체 user namespace를 가지며 UID/GID 매핑이 격리됩니다.

### IPC namespace

```ini
PrivateIPC=yes
RemoveIPC=yes
```

System V IPC와 POSIX 메시지 큐를 격리.

### PID namespace

```ini
PrivatePIDs=yes
```

(systemd 256+) 서비스가 자체 PID namespace를 가짐.

---

## 파일시스템 보호

### 읽기/쓰기 경로 명시

```ini
ProtectSystem=strict
ReadWritePaths=/var/lib/myapp /var/log/myapp
ReadOnlyPaths=/etc/myapp
InaccessiblePaths=/srv
```

`ProtectSystem=strict`으로 거의 모든 경로를 읽기 전용으로 잠근 뒤, `ReadWritePaths=`로 필요한 경로만 열어주는 패턴이 안전합니다.

### 커널 보호

```ini
ProtectKernelTunables=yes
ProtectKernelModules=yes
ProtectKernelLogs=yes
ProtectControlGroups=yes
ProtectClock=yes
ProtectHostname=yes
ProtectProc=invisible
ProcSubset=pid
```

- `ProtectKernelTunables=yes`: `/proc/sys`, `/sys` 읽기 전용
- `ProtectKernelModules=yes`: 커널 모듈 로드/언로드 차단
- `ProtectKernelLogs=yes`: kmsg 접근 차단
- `ProtectControlGroups=yes`: cgroup 파일시스템 읽기 전용
- `ProtectProc=invisible`: 다른 프로세스 정보(`/proc/<pid>`) 숨김
- `ProcSubset=pid`: `/proc` 의 시스템 정보(`/proc/cpuinfo` 등) 숨기고 PID 항목만 노출

---

## 시스템 콜 필터링

seccomp 기반 syscall 허용 목록/차단 목록입니다.

```ini
SystemCallFilter=@system-service
SystemCallFilter=~@privileged @resources
SystemCallErrorNumber=EPERM
SystemCallArchitectures=native
```

- `SystemCallFilter=@system-service`: 화이트리스트 그룹 (대부분의 일반 서비스에 적합)
- `~@privileged @resources`: 이미 허용된 것 중에서 privileged/resource 그룹은 빼기
- `SystemCallErrorNumber=`: 차단된 syscall이 어떤 에러로 실패할지
- `SystemCallArchitectures=native`: 네이티브 아키텍처만 허용 (x86_32 호환 모드 차단)

### 주요 syscall 그룹

| 그룹 | 설명 |
| --- | --- |
| `@system-service` | 일반 서비스에 필요한 syscall (좋은 시작점) |
| `@privileged` | mount, reboot 등 권한 필요 syscall |
| `@resources` | 우선순위 변경 등 |
| `@debug` | ptrace, perf 등 |
| `@module` | 커널 모듈 |
| `@mount` | mount, umount |
| `@network-io` | 소켓 연산 |
| `@raw-io` | iopl, ioperm |

---

## 기타 보안 옵션

### Memory와 실행

```ini
MemoryDenyWriteExecute=yes
LockPersonality=yes
RestrictRealtime=yes
RestrictSUIDSGID=yes
```

- `MemoryDenyWriteExecute=yes`: W^X를 강제합니다. JIT 컴파일러를 사용하는 언어(Java, Node.js 등)에서는 비활성화가 필요합니다
- `LockPersonality=yes`: `personality()` syscall 차단 (32/64비트 전환 등)
- `RestrictRealtime=yes`: 실시간 스케줄링 차단
- `RestrictSUIDSGID=yes`: setuid/setgid 비트 설정 차단

### 주소 패밀리 제한

```ini
RestrictAddressFamilies=AF_UNIX AF_INET AF_INET6
```

서비스가 사용할 수 있는 소켓 패밀리를 제한합니다. AF_NETLINK 등을 차단해 공격 표면을 줄일 수 있습니다.

### Namespace 제한

```ini
RestrictNamespaces=yes
```

서비스가 새 namespace를 생성하는 것을 차단합니다. 컨테이너 런타임이 아닌 서비스에는 거의 항상 활성화하는 것이 좋습니다.

### KeyringMode

```ini
KeyringMode=private
```

커널 키링 격리.

---

## systemd-analyze security

작성한 unit의 보안 점수를 자동으로 평가해주는 도구.

```bash
$ systemd-analyze security nginx.service
  NAME                                                 DESCRIPTION                                                       EXPOSURE
✗ RootDirectory=/RootImage=                            Service runs within the host's root directory                          0.1
  SupplementaryGroups=                                 Service has no supplementary groups
✗ PrivateDevices=                                      Service potentially has access to hardware devices                      0.2
✗ PrivateNetwork=                                      Service has access to the host's network                                0.5
...
→ Overall exposure level for nginx.service: 6.5 MEDIUM 🙂
```

0~10점 (낮을수록 안전). 옵션을 하나씩 켜면서 점수를 낮추는 것이 실전 하드닝의 좋은 출발점입니다.

---

## 실전 하드닝 예제

```ini
[Unit]
Description=Hardened App
After=network.target

[Service]
Type=exec
ExecStart=/usr/local/bin/myapp

# 사용자
DynamicUser=yes

# 디렉터리
StateDirectory=myapp
LogsDirectory=myapp
RuntimeDirectory=myapp

# 권한
NoNewPrivileges=yes
CapabilityBoundingSet=
AmbientCapabilities=

# 파일시스템
ProtectSystem=strict
ProtectHome=yes
PrivateTmp=yes
PrivateDevices=yes

# 커널
ProtectKernelTunables=yes
ProtectKernelModules=yes
ProtectKernelLogs=yes
ProtectControlGroups=yes
ProtectClock=yes
ProtectHostname=yes
ProtectProc=invisible
ProcSubset=pid

# 네임스페이스
PrivateNetwork=no
PrivateUsers=yes
RestrictNamespaces=yes
RestrictRealtime=yes
RestrictSUIDSGID=yes
LockPersonality=yes
MemoryDenyWriteExecute=yes
RemoveIPC=yes

# 주소 패밀리
RestrictAddressFamilies=AF_UNIX AF_INET AF_INET6

# Syscall
SystemCallFilter=@system-service
SystemCallFilter=~@privileged @resources
SystemCallArchitectures=native

[Install]
WantedBy=multi-user.target
```

이 unit은 `systemd-analyze security` 에서 1~2점대(매우 안전) 가 나옵니다.

---

## 참고 자료

- [man systemd.exec](https://www.freedesktop.org/software/systemd/man/systemd.exec.html)
- [systemd-analyze(1)](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
- [Lennart's Hardening Guide](http://0pointer.de/blog/projects/security.html)
