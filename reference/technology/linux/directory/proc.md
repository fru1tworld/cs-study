# `/proc` — procfs (프로세스 & 커널 상태)

> "a pseudo-filesystem which provides an interface to kernel data structures" — `proc(5)`
> "mount point for the proc filesystem, which provides information about running processes and the kernel" — `hier(7)`

휘발성. 디스크에 존재하지 않으며, **읽을 때마다 커널이 즉석에서 생성**한다. 대부분 read-only지만 `/proc/sys/` 일부는 쓰기로 커널 변수를 변경하는 인터페이스.

## 마운트

```bash
mount -t proc proc /proc
```

마운트 옵션 (`proc(5)`):
- **`hidepid=n`** (3.3+): `/proc/<pid>` 디렉터리 접근 통제
  - `0` 모두 접근 가능 (기본)
  - `1` 다른 사용자 PID 디렉터리 접근 차단
  - `2` 다른 사용자 PID 디렉터리가 존재 자체를 안 보임
  - `gid=<gid>`로 특정 그룹만 예외 허용
- **`subset=pid`** (5.8+): PID 관련만 노출, 시스템 전체 파일은 숨김 (컨테이너에 유용)

## 두 축

procfs는 크게 두 부류:

1. **`/proc/<pid>/`** — 프로세스별 상태
2. **`/proc/<systemwide>`** — 커널/시스템 전역 상태

## 1. `/proc/<pid>/` — 프로세스별

| 항목 | 내용 |
|---|---|
| `cmdline` | 명령행 인자 (NUL 구분). zombie면 비어 있음 |
| `comm` | 짧은 프로세스 이름 (TASK_COMM_LEN=16) |
| `cwd` | 현재 작업 디렉터리 심볼릭 링크 |
| `exe` | 실행 파일 심볼릭 링크 |
| `environ` | 환경 변수 (NUL 구분) |
| `fd/` | 열린 파일 디스크립터들. `fd/0`=stdin, `fd/1`=stdout, `fd/2`=stderr |
| `fdinfo/` | 각 fd의 메타 (pos, flags, mnt_id, epoll/inotify 상세 등) |
| `maps` | 가상 메모리 매핑 (주소-주소 perm offset dev inode pathname) |
| `mem` | 프로세스 가상 메모리 직접 접근 (디버거가 사용) |
| `mountinfo` | 마운트 namespace의 마운트들 (5.11+ 권장 형식) |
| `mounts` | 구버전 마운트 목록 |
| `mountstats` | NFS 등 마운트별 통계 |
| `net/` | 프로세스의 **네트워크 namespace** 정보 (`tcp`, `udp`, `unix`, `route`, `dev`…) |
| `ns/` | namespace 핸들 (자세한 건 [namespaces.md](namespaces.md)) |
| `root` | 프로세스의 루트 디렉터리 심볼릭 링크 (chroot/pivot 후 다름) |
| `stat` | 한 줄에 모든 상태 (PID, comm, state, ppid, utime, stime, vsize, rss, …) |
| `statm` | 메모리 사용량 요약 (size, resident, shared, text, …) |
| `status` | 사람이 읽기 좋은 status (Name, State, Pid, Tgid, Uid, Gid, VmRSS, Threads, …) |
| `task/<tid>/` | 스레드별 디렉터리 (같은 파일들이 다 들어있음) |
| `cgroup` | 이 프로세스의 cgroup 멤버십 (`0::/system.slice/foo.service` 식) |
| `limits` | rlimit (`prlimit`이 읽는 곳) |
| `oom_score`, `oom_score_adj` | OOM 킬러 점수 |
| `io` | I/O 통계 (rchar, wchar, syscr, syscw, read_bytes, write_bytes) |
| `sched`, `schedstat` | 스케줄러 통계 |
| `smaps`, `smaps_rollup` | maps + 메모리 종류별 상세 (PSS, USS 등) |
| `wchan` | 커널 내부 sleep 중인 함수 이름 |

**특수 심링크**:
- `/proc/self` → 호출 프로세스의 `/proc/<pid>` — "현재 나"
- `/proc/thread-self` → 호출 스레드의 `/proc/<pid>/task/<tid>`

## 2. 시스템 전역

| 파일 | 내용 |
|---|---|
| `/proc/cpuinfo` | CPU 모델, 코어, 플래그, MHz |
| `/proc/meminfo` | 메모리 통계 (MemTotal, MemFree, MemAvailable, Buffers, Cached, Slab, …) |
| `/proc/loadavg` | 1/5/15분 로드, 실행/총 태스크, 마지막 PID |
| `/proc/uptime` | 부팅 후 경과 초, idle 누적 |
| `/proc/stat` | 시스템 누적 통계 (CPU jiffies, ctxt, btime, processes, procs_running) |
| `/proc/vmstat` | VM 통계 (pgfault, pgsteal, pgscan, oom_kill, …) |
| `/proc/mounts` | 시스템 전체 마운트 (init namespace 기준) — `/proc/self/mounts`로 가는 링크 |
| `/proc/swaps` | 활성 스왑 디바이스 |
| `/proc/partitions` | major:minor + 이름 + 크기 |
| `/proc/diskstats` | 블록 디바이스 I/O 통계 |
| `/proc/filesystems` | 등록된 파일시스템 (`nodev`는 디스크 없는 가상 FS) |
| `/proc/modules` | 로드된 커널 모듈 (`lsmod`의 출처) |
| `/proc/kallsyms` | 커널 심볼 테이블 (보통 root만 주소 노출) |
| `/proc/cmdline` | 커널 부팅 파라미터 |
| `/proc/version` | 커널 버전, gcc 버전, 빌드 시각 |
| `/proc/iomem`, `/proc/ioports` | 물리 메모리 / I/O 포트 영역 |
| `/proc/interrupts` | IRQ 카운터 (CPU별) |
| `/proc/softirqs` | softirq 카운터 |
| `/proc/buddyinfo` | 페이지 할당자 free-list (NUMA 노드 × order별) |
| `/proc/slabinfo` | slab 캐시 사용량 (root only) |
| `/proc/zoneinfo` | NUMA 존별 watermark/통계 |
| `/proc/cgroups` | v1 컨트롤러 목록 + 활성화 여부 + cgroup 수 |
| `/proc/locks` | 시스템 내 모든 파일 락 |
| `/proc/sysrq-trigger` | 매직 SysRq 키 트리거 (`echo b > ...` 즉시 재부팅) |
| `/proc/kmsg`, `/proc/kcore` | 커널 메시지 / 메모리 덤프 (보안 민감) |

## 3. `/proc/sys/` — sysctl 인터페이스

읽기/쓰기 가능한 커널 튜닝 노브. `sysctl(8)` 명령이 이 트리에 mapping된다.

```
/proc/sys/
├── kernel/         커널 일반
│   ├── hostname            (uname -n)
│   ├── pid_max             최대 PID
│   ├── threads-max         최대 스레드
│   ├── shmmax, shmall      SysV 공유메모리 한도
│   ├── randomize_va_space  ASLR
│   ├── core_pattern        코어 덤프 경로/파이프
│   ├── sched_*             스케줄러
│   ├── sysrq               SysRq 활성화
│   └── ns_last_pid         (PID namespace별)
├── vm/             가상 메모리
│   ├── swappiness          0–200, 스왑 적극성
│   ├── overcommit_memory   0=heuristic, 1=always, 2=strict
│   ├── overcommit_ratio
│   ├── dirty_ratio, dirty_background_ratio
│   ├── min_free_kbytes
│   ├── nr_hugepages        HugeTLB 페이지 개수
│   ├── drop_caches         1/2/3 쓰면 pagecache/dentry+inode/모두 비움
│   └── panic_on_oom
├── net/            네트워크 (네트워크 namespace별)
│   ├── core/somaxconn      listen() 백로그 상한
│   ├── ipv4/ip_forward     라우터 동작
│   ├── ipv4/tcp_*          TCP 튜닝
│   ├── ipv4/conf/*/rp_filter
│   ├── ipv6/conf/*/disable_ipv6
│   └── netfilter/*
└── fs/             파일시스템
    ├── file-max            전역 fd 한도
    ├── inotify/max_*
    ├── nr_open             프로세스당 fd 한도 상한
    ├── pipe-max-size
    └── protected_*         하드링크/심볼릭링크 보호
```

값을 변경하는 두 방법:
```bash
echo 60 > /proc/sys/vm/swappiness     # 임시
sysctl -w vm.swappiness=60            # 임시
# 영구: /etc/sysctl.conf 또는 /etc/sysctl.d/*.conf
```

## 4. namespace 측면

`/proc`은 **마운트 시점의 PID namespace**를 기준으로 프로세스를 노출한다.
- 새 PID namespace에 진입 후 procfs를 새로 마운트해야 `ps`가 정상 동작.
- 네트워크 관련(`/proc/net/*`, `/proc/sys/net/*`)은 네트워크 namespace별로 다르게 보임.

## 5. 자주 쓰는 패턴

```bash
# 내 cgroup
cat /proc/self/cgroup

# 어떤 프로세스의 환경 변수
tr '\0' '\n' < /proc/<pid>/environ

# 열린 fd 들
ls -l /proc/<pid>/fd/

# 메모리 매핑 (라이브러리, 힙, 스택)
cat /proc/<pid>/maps

# 어떤 디스크에서 얼마나 읽었는지
cat /proc/<pid>/io

# 캐시 한 방에 비우기 (벤치마크 전)
echo 3 > /proc/sys/vm/drop_caches

# 시스템 전체 fd 사용량
cat /proc/sys/fs/file-nr      # 사용중 / 미사용 / 최대
```
