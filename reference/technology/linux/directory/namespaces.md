# Namespaces — `/proc/<pid>/ns/`

> "A namespace wraps a global system resource in an abstraction that makes it appear to the processes within the namespace that they have their own isolated instance of the global resource." — `namespaces(7)`

컨테이너의 핵심 기반. **cgroup이 "얼마나 쓸 수 있냐"라면 namespace는 "무엇이 보이냐"다**. 호스트 커널은 그대로 공유하면서, 각 프로세스에게 자기만의 글로벌 자원이 있는 것처럼 보이게 한다.

## 1. 8가지 namespace 종류

| 종류 | CLONE 플래그 | 격리되는 자원 | 도입 |
|---|---|---|---|
| **Mount** | `CLONE_NEWNS` | 마운트 포인트 | 2.4.19 (가장 오래됨) |
| **UTS** | `CLONE_NEWUTS` | hostname, NIS domain name | 2.6.19 |
| **IPC** | `CLONE_NEWIPC` | System V IPC, POSIX message queues | 2.6.19 |
| **PID** | `CLONE_NEWPID` | 프로세스 ID 공간 | 2.6.24 |
| **Network** | `CLONE_NEWNET` | 네트워크 디바이스, 스택, 포트, 라우팅, `/proc/net`, `/sys/class/net` | 2.6.29 |
| **User** | `CLONE_NEWUSER` | UID/GID, capabilities, keyring | 3.8 |
| **Cgroup** | `CLONE_NEWCGROUP` | cgroup 루트 디렉터리 (가상화된 cgroup path) | 4.6 |
| **Time** | `CLONE_NEWTIME` | boot/monotonic clock 오프셋 | 5.6 |

> `namespaces(7)` 정의: "Mount" 격리 = mount points / "IPC" 격리 = System V IPC, POSIX message queues / "Network" 격리 = "Network devices, stacks, ports" / "PID" 격리 = "Process IDs" / "Time" 격리 = "Boot and monotonic clocks" / "User" 격리 = "User and group IDs" / "UTS" 격리 = "Hostname and NIS domain name" / "Cgroup" 격리 = "Cgroup root directory"

## 2. 시스템 콜 3종

### `clone(2)` — 만들면서 진입

새 프로세스 생성과 동시에 namespace를 새로 만든다.
```c
clone(child_fn, stack, CLONE_NEWPID | CLONE_NEWNET | SIGCHLD, arg);
```
> "creates a new process. If the flags argument of the call specifies one or more of the CLONE_NEW* flags listed above, then new namespaces are created for each flag" — `namespaces(7)`

### `setns(2)` — 기존 namespace에 합류

`/proc/<pid>/ns/<type>` fd를 받아 진입.
```c
int fd = open("/proc/1234/ns/net", O_RDONLY);
setns(fd, CLONE_NEWNET);
```
> "allows the calling process to join an existing namespace. The namespace to join is specified via a file descriptor that refers to one of the /proc/[pid]/ns files"

### `unshare(2)` — 호출 프로세스를 새 namespace로 이동

```c
unshare(CLONE_NEWUTS);
sethostname("inside", 6);
```
> "moves the calling process to a new namespace. If the flags argument of the call specifies one or more of the CLONE_NEW* flags listed above, then new namespaces are created"

**주의 (PID/Time)**: `setns`/`unshare`로 PID 또는 Time namespace를 바꿔도 **호출 프로세스 자신은 새 namespace로 안 들어간다**. 이후 fork되는 자식부터 새 namespace 소속이 됨 — 이미 살아있는 프로세스의 PID 일관성을 보호하기 위함. `/proc/<pid>/ns/pid_for_children`, `time_for_children`이 그 "다음 자식이 들어갈 곳"을 가리킴.

## 3. `/proc/<pid>/ns/`

각 namespace 핸들이 심볼릭 링크로 노출된다:

```
$ ls -l /proc/self/ns/
lrwxrwxrwx ... cgroup -> 'cgroup:[4026531835]'
lrwxrwxrwx ... ipc    -> 'ipc:[4026531839]'
lrwxrwxrwx ... mnt    -> 'mnt:[4026531840]'
lrwxrwxrwx ... net    -> 'net:[4026531992]'
lrwxrwxrwx ... pid    -> 'pid:[4026531836]'
lrwxrwxrwx ... pid_for_children -> 'pid:[4026531836]'
lrwxrwxrwx ... time   -> 'time:[4026531834]'
lrwxrwxrwx ... time_for_children -> 'time:[4026531834]'
lrwxrwxrwx ... user   -> 'user:[4026531837]'
lrwxrwxrwx ... uts    -> 'uts:[4026531838]'
```

> "If two processes are in the same namespace, then the device IDs and inode numbers of their `/proc/[pid]/ns/xxx` symbolic links will be the same" — `namespaces(7)`

즉, 두 프로세스가 같은 namespace인지 확인하는 정식 방법은 **inode 번호 비교**:

```bash
readlink /proc/1234/ns/net    # net:[4026531992]
readlink /proc/5678/ns/net    # net:[4026531992] → 같은 net namespace
```

## 4. Lifetime

> "Absent any other factors, a namespace is automatically torn down when the last process in the namespace terminates or leaves the namespace."

지속시키는 요인:
- 열린 fd (`/proc/<pid>/ns/*`을 누군가 open 중)
- **bind mount**로 namespace 파일 박아두기 (`mount --bind /proc/1234/ns/net /var/run/netns/foo`)
- 계층 관계 (자식 user/PID namespace가 살아있으면 부모 유지)
- 다른 namespace가 그 user namespace를 owner로 참조

이 메커니즘 덕에 **프로세스가 없어도 네트워크 namespace를 유지**할 수 있다 — `ip netns add foo`가 정확히 이걸 한다.

## 5. PID Namespace 상세 (`pid_namespaces(7)`)

### 기본 동작

- 새 PID namespace에서 PID는 **1번부터** 시작 (독립 시스템처럼).
- 첫 프로세스 = **init (PID 1)**. 고아 프로세스의 부모.
- 가시성: 자기 namespace + **조상**만 보임. **자식 → 부모는 안 보임.** 단방향.
- PID는 namespace마다 독립 — 호스트의 PID 1234 = 컨테이너 내부 PID 1.

### Init 프로세스 (PID 1)의 특수성

> "if the init process terminates, the kernel terminates all of the processes in the namespace via a SIGKILL signal" — `pid_namespaces(7)`

- init이 죽으면 namespace의 **모든 프로세스에 SIGKILL**. 이후 fork는 `ENOMEM`.
- 시그널 보호: namespace 내부 다른 프로세스는 **핸들러가 등록된 시그널만** init에 보낼 수 있다. 단 **조상 namespace에서 보낸 SIGKILL/SIGSTOP은 통과**.

→ 도커 같은 컨테이너 런타임이 "tini" 같은 작은 init을 PID 1로 쓰는 이유: SIGTERM 핸들링과 좀비 리핑.

### 중첩

- 부모-자식 트리 구조.
- **Linux 3.7부터 최대 32단계** 중첩.
- 프로세스는 자식 namespace로 내려갈 수 있지만 부모로 못 올라감.

### `/proc` 동작

> "the /proc filesystem ... only displays processes visible in the PID namespace of the process that performed the mount"

새 PID namespace 진입 후 procfs를 **새로 마운트**해야 `ps`가 정상. 컨테이너 이미지가 항상 `/proc`을 새로 마운트하는 이유.

`/proc/sys/kernel/ns_last_pid`: 마지막 할당 PID — CRIU의 PID 복원에 쓰임.

### 시그널 변환

UNIX 도메인 소켓의 SCM_CREDENTIALS에 담긴 PID는 **수신자의 PID namespace 기준으로 자동 변환**된다.

## 6. User Namespace 상세 (`user_namespaces(7)`)

비특권 컨테이너의 핵심.

### 중첩 & 능력

- 32단계 한계, 초과 시 `EUSERS`.
- 새 user namespace에 들어가면 **그 안에서 모든 capability를 가진다**. 단 **부모 namespace에서는 아무 권한 없음**.
- 비특권 사용자가 "내부 root"가 되는 메커니즘이 정확히 이것.
- 능력은 namespace 트리를 따라 자식으로 상속.
- `execve()`는 보통 규칙대로 capability 재계산 — 일반적으로 새 프로그램은 capability를 잃음 (파일 capability 없을 때).

### UID/GID 매핑

`/proc/<pid>/uid_map`과 `gid_map` 형식:

```
<ID-inside-ns>  <ID-outside-ns>  <length>
```

예 — namespace 내부 UID 0~65535를 호스트 100000~165535로 매핑:
```
0 100000 65536
```

규칙:
- **한 번만 쓸 수 있음**.
- 범위가 겹치면 안 됨.
- 매핑 안 된 UID는 **overflow uid** (보통 65534 = `nobody`)로 보임.
- 매핑 안 된 파일의 set-UID/set-GID 비트는 **조용히 무시**.
- UID 0 매핑은 5.12+에서 `CAP_SETFCAP` 필요.

### `setgroups` 통제

`/proc/<pid>/setgroups`:
- `deny`를 `gid_map` 쓰기 **전에** 써두면 `setgroups(2)`가 영구 비활성 → 후손에도 적용.
- 비특권 user namespace에서 그룹 드롭으로 파일 접근권 우회하는 취약점 방어용.

### 제한

- 블록 디바이스 FS 마운트 → **초기 namespace의 `CAP_SYS_ADMIN`** 필요.
- 클럭 변경, 커널 모듈 로드 → 초기 namespace 권한.
- 그래서 컨테이너 안에서 "root"여도 호스트 커널 자체는 못 만짐.

## 7. Network Namespace

각 namespace가 자기만의:
- 네트워크 인터페이스 (loopback도 별도)
- 라우팅 테이블
- ARP/neighbor 테이블
- iptables/nftables 룰
- 소켓 + 포트 공간
- `/proc/net/*`, `/sys/class/net/*`
- `/proc/sys/net/*` 일부 sysctl

### veth pair로 연결

```bash
ip netns add foo
ip link add veth0 type veth peer name veth1
ip link set veth1 netns foo
ip addr add 10.0.0.1/24 dev veth0
ip link set veth0 up
ip netns exec foo ip addr add 10.0.0.2/24 dev veth1
ip netns exec foo ip link set veth1 up
ip netns exec foo ip link set lo up
```

`veth` = 가상 이더넷 쌍. 한쪽은 호스트, 한쪽은 컨테이너 namespace. 컨테이너 네트워킹의 기본 빌딩 블록.

## 8. Mount Namespace

- 마운트 포인트의 격리된 뷰.
- 새 namespace 안에서 마운트/언마운트해도 호스트는 영향 없음 (마운트 전파 규칙 제외).
- `mount --make-shared/private/slave` — 전파 모드 설정.
- `pivot_root(2)` — 컨테이너에서 새 루트 파일시스템으로 전환할 때 사용 (chroot보다 안전).
- systemd 유닛의 `PrivateTmp=`, `ProtectSystem=`, `ProtectHome=`, `ReadOnlyPaths=` 등은 전부 mount namespace로 구현.

## 9. UTS Namespace

가장 단순 — `hostname`, `domainname`만 격리.

```c
unshare(CLONE_NEWUTS);
sethostname("container1", 10);
```

`uname(2)`의 `nodename`, `domainname` 필드가 namespace별로 다르게 보임.

## 10. IPC Namespace

- System V IPC 객체 (`shmget`, `msgget`, `semget`)
- POSIX 메시지 큐 (`mq_open` — 사실상 `/dev/mqueue` 마운트와 묶임)

서로 다른 IPC namespace의 프로세스는 같은 키로 만든 SysV 객체에 접근 못 함.

## 11. Cgroup Namespace

`/proc/<pid>/cgroup`이 보여주는 경로가 가상화된다.

호스트에서:
```
0::/system.slice/docker-abc.scope
```

컨테이너 안에서 (cgroup namespace 진입 후):
```
0::/
```

→ 컨테이너가 호스트의 cgroup 경로 구조를 못 보게 함.

## 12. Time Namespace (5.6+)

- `CLOCK_BOOTTIME`, `CLOCK_MONOTONIC`에 **오프셋**을 적용.
- `CLOCK_REALTIME`은 격리 안 됨 (벽시계는 시스템 전역).
- `/proc/<pid>/timens_offsets` — 자식이 들어갈 time namespace의 오프셋 설정.
- CRIU 같은 체크포인트 도구가 monotonic 시계를 보존하기 위해 사용.

## 13. 컨테이너 런타임이 묶는 방식

도커/podman/runc가 컨테이너 만들 때 보통:
```
clone(CLONE_NEWPID | CLONE_NEWNS | CLONE_NEWUTS | CLONE_NEWIPC | CLONE_NEWNET | CLONE_NEWUSER | CLONE_NEWCGROUP, ...)
```
- + mount namespace 안에서 `pivot_root`로 컨테이너 이미지로 전환
- + `/proc` 새로 마운트 (PID namespace 반영)
- + `uid_map`, `gid_map` 설정 (user namespace 활성화 시)
- + cgroup으로 리소스 제한 ([cgroups.md](cgroups.md))
- + seccomp BPF, AppArmor/SELinux로 syscall 화이트리스트

systemd의 `systemd-nspawn`도 같은 원리.

## 14. 자주 쓰는 명령

```bash
# 현재 프로세스의 namespace 목록 (lsns)
sudo lsns

# 특정 PID의 namespace 보기
ls -l /proc/<pid>/ns/

# 새 namespace에서 셸 실행
unshare --pid --fork --mount-proc bash       # PID + 새 /proc
unshare --net bash                            # 네트워크
unshare --user --map-root-user bash           # user namespace (비특권 root)

# 기존 컨테이너의 namespace에 합류
nsenter -t <pid> -a /bin/bash
nsenter -t <pid> -n ip addr                   # 컨테이너 네트워크만

# 네트워크 namespace 영구 보관
ip netns add foo
ip netns exec foo <cmd>
ip netns list

# user namespace 확인
cat /proc/self/uid_map
cat /proc/self/gid_map
```

## 15. cgroups과의 차이 정리

| | cgroups | namespaces |
|---|---|---|
| 무엇을 격리하나 | **얼마나** 쓸 수 있나 (리소스 양) | **무엇이 보이나** (시야) |
| 트리 구조 | 단일 cgroup 트리 | 종류별 트리 (PID, user는 진짜 트리) |
| 인터페이스 | `/sys/fs/cgroup/` 파일 R/W | syscall (`clone`/`setns`/`unshare`), `/proc/<pid>/ns/` |
| 컨테이너에서 역할 | CPU/메모리/IO 쿼터 | 컨테이너 경계 (PID 1, 분리된 네트워크 등) |

둘 다 같이 써야 비로소 컨테이너가 된다.
