# cgroups (Control Groups) — `/sys/fs/cgroup`

> "Control groups, usually referred to as cgroups, are a Linux kernel feature which allow processes to be organized into hierarchical groups whose usage of various types of resources can then be limited and monitored."  — `cgroups(7)`

## 1. 핵심 개념

| 용어 | 의미 |
|---|---|
| **cgroup** | 리소스 제한이 묶이는 프로세스 집합 |
| **controller** (= subsystem) | 특정 리소스(CPU, 메모리, I/O…)의 동작을 변경하는 커널 모듈 |
| **hierarchy** | cgroup이 형성하는 트리. 컨트롤러가 부착되어 작동 |
| **core** | 계층 구조 자체를 관리하는 부분 |

**cgroup 두 부분**: core (프로세스를 계층적으로 조직) + controllers (특정 리소스 분배). 모든 프로세스는 정확히 **하나의 cgroup**에 속하며, **한 프로세스의 모든 스레드는 같은 cgroup**에 있다.

**계층 동작**: 자식에 컨트롤러를 활성화하면 리소스 분배가 더 제한될 뿐이다. **루트 가까이서 건 제약은 더 깊은 곳에서 풀 수 없다** (top-down).

## 2. v1 vs v2

| 항목 | v1 (2.6.24+) | v2 (4.5+, "unified") |
|---|---|---|
| 계층 구조 | 컨트롤러마다 별도 마운트 가능 (여러 트리) | **단일 통합 트리** |
| 마운트 타입 | `cgroup` | `cgroup2` |
| 내부 프로세스 | 어디든 가능 | **"No Internal Process" 규칙** — 도메인 컨트롤러 활성화된 비루트 cgroup은 리프에만 프로세스 가능 |
| 스레드 제어 | 모든 컨트롤러가 `tasks` 파일로 가능 | **Thread mode** (4.14+), 일부 컨트롤러만 |
| 빈 cgroup 알림 | `release_agent` (한 번에 하나) | `cgroup.events`의 `populated` 필드 (poll/inotify) |
| 같은 컨트롤러 동시 사용 | — | v1에 마운트된 컨트롤러는 v2에 못 붙임 |

현대 시스템(systemd 기본)은 **v2 unified hierarchy**. 이 문서는 v2를 중심으로 다룬다.

## 3. 마운트

```bash
mount -t cgroup2 none /sys/fs/cgroup
```

`statfs(2)` magic number: `0x63677270` (= ASCII `cgrp`).

v2를 지원하는 모든 컨트롤러가 자동으로 루트에 바인드된다. 커널 부팅 파라미터 `cgroup_no_v1=<controller>`로 특정 컨트롤러를 v1에서 제외 → v2에서만 사용 가능.

### 마운트 옵션

| 옵션 | 효과 |
|---|---|
| `nsdelegate` | cgroup namespace를 위임 경계로 취급 |
| `favordynmods` | fork/exit 비용 늘리는 대신 동적 변경 지연 감소 |
| `memory_localevents` | `memory.events`에 현재 cgroup만 (서브트리 제외) |
| `memory_recursiveprot` | 메모리 보호를 서브트리에 재귀 적용 |
| `memory_hugetlb_accounting` | HugeTLB를 memory usage에 포함 |

## 4. `/sys/fs/cgroup` 레이아웃 (systemd 기준)

```
/sys/fs/cgroup/                루트
├── cgroup.controllers         사용 가능 컨트롤러 목록
├── cgroup.subtree_control     자식에게 위임할 컨트롤러
├── cgroup.procs               이 cgroup의 PID들
├── cgroup.threads
├── cgroup.events              populated, frozen
├── cgroup.stat                nr_descendants 등
├── cpu.stat
├── memory.stat
├── io.stat
│
├── init.scope/                PID 1 (systemd 자신)
├── system.slice/              시스템 서비스
│   ├── cgroup.procs
│   ├── memory.max
│   ├── cpu.weight
│   ├── sshd.service/
│   ├── docker.service/
│   └── ...
├── user.slice/                로그인 사용자 세션
│   └── user-1000.slice/
│       ├── user@1000.service/
│       └── session-3.scope/
└── machine.slice/             VM, 컨테이너
    └── docker-<id>.scope/
```

## 5. 프로세스 관리 동작

- 자식 cgroup은 **서브디렉터리 생성**으로 만든다 — `mkdir foo`.
- 프로세스 이동: PID를 대상 cgroup의 **`cgroup.procs`에 write**.
  - 한 번의 `write(2)`로 **한 프로세스만** 이동.
  - 어떤 스레드의 PID를 써도 그 프로세스의 **모든 스레드가 함께** 이동.
- 새 프로세스는 fork 시점에 **부모의 cgroup을 상속**.
- 빈 cgroup (자식·프로세스 없음)은 **`rmdir`**로 제거.
- `/proc/<pid>/cgroup` 형식: `0::<path>` (v2) / `<id>:<controllers>:<path>` (v1).

### 스레드 관리 (thread mode)

기본적으로 한 프로세스의 모든 스레드는 같은 cgroup. **Thread mode**는 서브트리 안에서 스레드 단위로 분산 허용:

- `cgroup.type`에 **`threaded`** 쓰면 도메인 → 스레드 cgroup 변환 (**일방통행**).
- `cgroup.threads`로 TID 단위 마이그레이션.
- 같은 도메인 안에서만 스레드 이동 가능.
- **"threaded controllers"**(cpu, perf_event, pids 등)만 스레드 모드 지원. memory 같은 도메인 컨트롤러는 ✗.

## 6. Populated 알림

각 비루트 cgroup의 `cgroup.events`:
```
populated 1     # 서브트리에 살아있는 프로세스가 있음
frozen 0
```

`poll(2)` 또는 `inotify`로 변화 감시 가능. 자식 상태가 바뀌면 부모도 알림 받음 (재귀적).

## 7. 리소스 분배 모델 4종

### Weights (가중치)
> "by adding up the weights of all active children and giving each the fraction matching the ratio of its weight against the sum"

- 범위 `[1, 10000]`, 기본 **100**
- **work-conserving**: 안 쓰는 자식의 몫은 다른 곳으로
- 예: `cpu.weight`

### Limits (상한)
> "A child can consume up to the configured amount of the resource"

- 범위 `[0, max]`, 기본 `max`
- **over-commit 허용** — 자식들 합이 부모를 초과해도 됨
- 예: `memory.max`, `io.max`

### Protections (보호)
> "as long as the usages of all its ancestors are under their protected levels"

- 범위 `[0, max]`, 기본 `0`
- hard 또는 best-effort
- over-commit 허용
- 예: `memory.min` (hard), `memory.low` (best-effort)

### Allocations (할당)
> "A cgroup is exclusively allocated a certain amount of a finite resource"

- 범위 `[0, max]`, 기본 `0`
- **over-commit 불가** — 자식 합 ≤ 부모
- 예: 실시간 CPU 슬라이스, RDMA HCA

## 8. Core 인터페이스 파일 (`cgroup.*`)

모든 core 파일은 `cgroup.` 프리픽스.

| 파일 | R/W | 설명 |
|---|---|---|
| `cgroup.type` | RW | `domain`, `domain threaded`, `domain (invalid)`, `threaded` |
| `cgroup.procs` | RW | PID 목록, 쓰면 마이그레이션 |
| `cgroup.threads` | RW | TID 목록 (같은 도메인 내부 이동만) |
| `cgroup.controllers` | R | 부모가 위임한 사용 가능 컨트롤러 |
| `cgroup.subtree_control` | RW | 자식에 활성화할 컨트롤러 (`+cpu -memory` 형식으로 쓰기) |
| `cgroup.events` | R | `populated 0/1`, `frozen 0/1` |
| `cgroup.max.descendants` | RW | 허용할 자손 cgroup 최대 수 |
| `cgroup.max.depth` | RW | 깊이 제한 |
| `cgroup.stat` | R | `nr_descendants`, `nr_dying_descendants`, `nr_subsys_*` |
| `cgroup.stat.local` | R | local 통계 (`frozen_usec`) |
| `cgroup.freeze` | RW | `1` 쓰면 서브트리 전체 freeze, `0` unfreeze |
| `cgroup.kill` | W | `1` 쓰면 서브트리의 모든 프로세스에 SIGKILL |
| `cgroup.pressure` | RW | PSI 어카운팅 활성 (`0`/`1`, 기본 `1`) |

**Top-down 제약**: 자식에서 활성화한 컨트롤러는 부모의 `subtree_control`에도 있어야 함.

**No Internal Process 제약**: 비루트 도메인 cgroup은 **(내부 프로세스 + 활성화된 도메인 컨트롤러)** 조합이 안 됨. 프로세스는 리프에서만 경쟁.

## 9. 컨트롤러별 상세

### `cpu`

CPU 사이클 분배. weight + bandwidth limit.

| 파일 | 설명 |
|---|---|
| `cpu.stat` | `usage_usec`, `user_usec`, `system_usec`, `nr_periods`, `nr_throttled`, `throttled_usec`, `nr_bursts`, `burst_usec` |
| `cpu.weight` | `[1, 10000]`, 기본 100 — fair-class 스케줄러 가중치 |
| `cpu.weight.nice` | `[-20, 19]` — nice 값 형식의 가중치 |
| `cpu.max` | `"$MAX $PERIOD"` (기본 `"max 100000"`) — 100ms 중 50ms = 0.5 CPU |
| `cpu.max.burst` | `[0, $MAX]`, 기본 0 — burst 허용 |
| `cpu.pressure` | PSI |
| `cpu.uclamp.min` | utilization 보호 (%) |
| `cpu.uclamp.max` | utilization 제한 (%) |
| `cpu.idle` | `1`이면 SCHED_IDLE 클래스 |

### `memory`

메모리 분배 — limit + protection. userland 메모리 + 커널 자료구조 + TCP 소켓 버퍼 모두 추적.

| 파일 | 설명 |
|---|---|
| `memory.current` | 현재 사용량 (서브트리 합) |
| `memory.peak` | 최대 기록 (리셋 가능) |
| `memory.min` | **hard protection** — 이 값까지는 절대 reclaim 안 함 (OOM 발생 가능) |
| `memory.low` | **best-effort protection** |
| `memory.high` | throttle 한도 — 넘으면 reclaim 적극화 (OOM은 ✗) |
| `memory.max` | **hard limit** — 초과 시 OOM |
| `memory.reclaim` | 강제 reclaim 트리거 (`echo 1G > ...`) |
| `memory.oom.group` | `1`이면 OOM 시 cgroup 단위로 한꺼번에 죽임 |
| `memory.events` | 카운터: `low`, `high`, `max`, `oom`, `oom_kill`, `oom_group_kill`, `sock_throttled` |
| `memory.events.local` | local 카운터 |
| `memory.stat` | 상세 분해 — `anon`, `file`, `kernel`, `kernel_stack`, `pagetables`, `percpu`, `sock`, `vmalloc`, `shmem`, `zswap`, `zswapped`, `file_mapped`, `file_dirty`, `file_writeback`, `swapcached`, `anon_thp`, `file_thp`, `shmem_thp`, `inactive_anon`, `active_anon`, `inactive_file`, `active_file`, `unevictable`, `slab_reclaimable`, `slab_unreclaimable`, `slab`, `workingset_*`, `pswpin`, `pswpout`, `pgscan_*`, `pgsteal_*`, `pgfault`, `pgmajfault`, `pgrefill`, `pgactivate`, `pgdeactivate`, `pglazyfree`, `pglazyfreed`, `thp_*`, `numa_*`, `hugetlb` |
| `memory.numa_stat` | NUMA 노드별 |
| `memory.swap.current` | swap 사용량 |
| `memory.swap.high` | swap throttle |
| `memory.swap.max` | swap hard limit |
| `memory.swap.peak`, `.events` | |
| `memory.zswap.current`, `.max` | zswap |
| `memory.zswap.writeback` | `0`이면 swap 디바이스로 안 내려가게 |
| `memory.pressure` | PSI |

### `io`

I/O — weight 기반 + limit 기반.

| 파일 | 설명 |
|---|---|
| `io.stat` | `<MAJ>:<MIN> rbytes= wbytes= rios= wios= dbytes= dios=` |
| `io.cost.qos` | IO cost QoS (루트 전용): `enable ctrl rpct rlat wpct wlat min max` |
| `io.cost.model` | IO cost 모델 (루트 전용): `ctrl model rbps wbps rseqiops wseqiops rrandiops wrandiops` |
| `io.weight` | 디바이스별 가중치 — `default 100` 또는 `MAJ:MIN WEIGHT` |
| `io.max` | `rbps wbps riops wiops` 제한 |
| `io.latency` | `MAJ:MIN target=<us>` — 목표 지연 (다른 cgroup throttle) |
| `io.pressure` | PSI |

### `pids`

| 파일 | 설명 |
|---|---|
| `pids.max` | fork/clone 상한 (fork bomb 방지) |
| `pids.current` | 현재 프로세스 수 |
| `pids.peak` | 최대 기록 |
| `pids.events` | `max` (제한 도달 횟수) |
| `pids.events.local` | local |

### `cpuset`

CPU와 NUMA 노드 배치 제한. 계층적 — 부모가 허용 안 한 자원은 자식이 못 씀.

| 파일 | 설명 |
|---|---|
| `cpuset.cpus` | `"0-4,6,8-10"` 형식 CPU 화이트리스트 |
| `cpuset.cpus.effective` | 실제 적용된 CPU |
| `cpuset.mems` | 메모리 노드 화이트리스트 |
| `cpuset.mems.effective` | 실제 적용 |
| `cpuset.cpus.exclusive` | 파티션 생성용 배타 CPU |
| `cpuset.cpus.exclusive.effective` | |
| `cpuset.cpus.isolated` | 루트 전용 — 격리된 CPU |
| `cpuset.cpus.partition` | `member` / `root` / `isolated` |

### `hugetlb`, `rdma`, `misc`

- **hugetlb**: HugeTLB 페이지 크기별 사용량
- **rdma**: RDMA HCA 핸들/객체
- **misc**: 잡다한 유한 리소스 (SEV ASID 등)

## 10. Delegation (위임)

- 컨트롤러는 부모 → 자식으로 위임. 루트가 모두 가지고, **`subtree_control`**에 쓴 것만 자식이 본다.
- 비특권 사용자에게 위임할 때는 디렉터리/일부 파일 소유권 이전.
- `/sys/kernel/cgroup/delegate` — 위임 가능한 파일 목록.
- `nsdelegate` 마운트 옵션 → cgroup namespace 경계 = 자동 위임 경계.

## 11. systemd와의 매핑

systemd가 cgroup 트리를 그대로 관리한다.

### 단위: Slice / Service / Scope

- **`.slice`** — 분배의 계층 노드 (`system.slice`, `user.slice`, `machine.slice`)
- **`.service`** — 데몬 (systemd가 fork한 것)
- **`.scope`** — 외부에서 만들어진 프로세스 그룹 (`systemd-run`, `machinectl`, 로그인 세션)

### 유닛 디렉티브 → cgroup 파일

| systemd 디렉티브 | cgroup 파일 |
|---|---|
| `CPUWeight=` | `cpu.weight` |
| `CPUQuota=` | `cpu.max` |
| `MemoryMin=`, `MemoryLow=`, `MemoryHigh=`, `MemoryMax=` | `memory.min/low/high/max` |
| `MemorySwapMax=` | `memory.swap.max` |
| `IOWeight=` | `io.weight` |
| `IOReadBandwidthMax=`, `IOWriteBandwidthMax=` | `io.max` |
| `TasksMax=` | `pids.max` |
| `AllowedCPUs=`, `AllowedMemoryNodes=` | `cpuset.cpus`, `cpuset.mems` |
| `Delegate=yes` | `cgroup.subtree_control` 위임 |

### 확인 명령

```bash
systemd-cgls                   # 트리 보기
systemd-cgtop                  # top처럼 실시간
systemctl status <unit>        # CGroup: /system.slice/foo.service
cat /proc/self/cgroup          # 내 cgroup 경로
cat /sys/fs/cgroup/<path>/cgroup.controllers
cat /sys/fs/cgroup/<path>/memory.current
```

## 12. cgroup namespace

(자세한 내용은 [namespaces.md](namespaces.md))

`CLONE_NEWCGROUP`으로 만들면 **`/proc/<pid>/cgroup`이 루트로 보이는 시작점이 가상화**된다. 컨테이너 내부에서 호스트의 cgroup 경로를 안 보이게 하기 위해 사용. 컨테이너 안에서는 자기 cgroup이 `/`로 보임.

## 13. PSI (Pressure Stall Information)

각 cgroup의 `cpu.pressure`, `memory.pressure`, `io.pressure` — **"리소스 부족으로 멈춘 시간"** 측정. 호스트 전역 PSI는 `/proc/pressure/cpu`, `/proc/pressure/memory`, `/proc/pressure/io`.

형식:
```
some avg10=0.00 avg60=0.00 avg300=0.00 total=0
full avg10=0.00 avg60=0.00 avg300=0.00 total=0
```
- `some` — 최소 한 태스크가 stall
- `full` — 모든 태스크가 stall
