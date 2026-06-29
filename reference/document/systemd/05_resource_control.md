# 리소스 제어와 cgroups

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.resource-control.html , https://docs.kernel.org/admin-guide/cgroup-v2.html

---

## 목차

1. [cgroups란?](#cgroups란)
2. [cgroups v1 vs v2](#cgroups-v1-vs-v2)
3. [systemd와 cgroups](#systemd와-cgroups)
4. [Slice 계층](#slice-계층)
5. [CPU 제어](#cpu-제어)
6. [메모리 제어](#메모리-제어)
7. [IO 제어](#io-제어)
8. [태스크 수와 기타](#태스크-수와-기타)
9. [Delegate](#delegate)
10. [관찰과 디버깅](#관찰과-디버깅)
11. [참고 자료](#참고-자료)

---

## cgroups란?

**Control Groups(cgroups)** 는 Linux 커널의 기능으로, 프로세스 그룹의 리소스(CPU, 메모리, IO, 네트워크 등) 사용을 측정·제한·격리합니다. Docker, Kubernetes, systemd 등 모든 현대 컨테이너/init 시스템의 기반 기술입니다.

핵심 아이디어:
- 프로세스를 트리 구조의 그룹에 배치
- 각 그룹에 컨트롤러(controller)를 부착해 자원 정책 적용
- 자식 그룹은 부모 정책의 영향을 받음

---

## cgroups v1 vs v2

### v1 (레거시)

- 컨트롤러별로 **독립된 계층** (CPU, 메모리, IO 각각 별도 트리)
- 같은 프로세스가 컨트롤러마다 다른 그룹에 속할 수 있음
- 일관성 부족, 복잡함

### v2 (통합)

- 모든 컨트롤러가 **단일 계층** 공유
- 한 프로세스는 정확히 한 cgroup에 속함
- 더 단순하고 강력한 정책 표현 가능
- Linux 4.5에서 안정화, 5.x에서 성숙
- systemd는 가능하면 항상 v2를 선호

### 현재 시스템 확인

```bash
$ stat -fc %T /sys/fs/cgroup/
cgroup2fs    # v2 단독 (권장)
tmpfs        # 하이브리드 또는 v1
```

부팅 시 `systemd.unified_cgroup_hierarchy=1` 커널 파라미터로 v2 강제 가능.

---

## systemd와 cgroups

systemd는 PID 1로서 cgroup 트리의 루트를 관리합니다. 모든 unit은 자동으로 cgroup에 배치됩니다.

```
/sys/fs/cgroup/
├── system.slice/
│   ├── nginx.service/
│   ├── postgresql.service/
│   └── ...
├── user.slice/
│   └── user-1000.slice/
│       └── user@1000.service/
│           └── app.slice/
└── machine.slice/
    └── ... (컨테이너)
```

각 서비스는 `<name>.service` 이름의 cgroup을 가지며, 해당 cgroup 안의 모든 프로세스를 systemd가 추적합니다. PID 파일 기반의 SysV init보다 훨씬 안정적인 프로세스 추적이 가능합니다.

---

## Slice 계층

`.slice` unit은 cgroup 트리의 중간 노드 역할을 합니다. 리소스 정책을 그룹화할 때 유용합니다.

### 표준 slice

| Slice | 용도 |
| --- | --- |
| `-.slice` | 루트 |
| `system.slice` | 시스템 서비스 |
| `user.slice` | 사용자 세션 |
| `machine.slice` | 컨테이너/VM |

### 사용자 정의 slice

```ini
# /etc/systemd/system/databases.slice
[Unit]
Description=Database Services Slice

[Slice]
CPUWeight=200
MemoryMax=8G
```

서비스에서 사용:

```ini
[Service]
Slice=databases.slice
```

이렇게 하면 모든 DB 서비스가 한 slice에 묶여 합산 메모리 8GB, CPU 가중치 200으로 제한됩니다.

---

## CPU 제어

### CPUWeight (상대 비중)

기본값 100. 다른 unit과의 **상대적인 CPU 시간 분배** 비율.

```ini
CPUWeight=200      # 일반보다 2배
StartupCPUWeight=500   # 부팅 시 임시 가중치
```

- 범위: 1~10000
- v1의 `CPUShares=` 를 대체

### CPUQuota (절대 한도)

특정 CPU 사용률을 넘지 못하게 함.

```ini
CPUQuota=50%      # CPU 한 코어의 50%
CPUQuota=200%     # 코어 2개분
```

내부적으로 `cpu.max` (period/quota)를 설정. 초과 시 throttling 발생.

### CPUAffinity (코어 핀)

```ini
CPUAffinity=0-3 8
```

지정한 CPU 코어에서만 실행되도록 제한.

### AllowedCPUs / AllowedMemoryNodes

NUMA 시스템에서 cgroup이 사용할 CPU/메모리 노드.

```ini
AllowedCPUs=0-15
AllowedMemoryNodes=0
```

---

## 메모리 제어

cgroups v2는 4단계의 메모리 한도를 지원합니다.

### MemoryMin (보호)

```ini
MemoryMin=512M
```

이 unit이 사용하는 메모리는 reclaim에서 **절대로 회수되지 않음**. 다른 곳에서 메모리 압박이 와도 보호됨.

### MemoryLow (소프트 보호)

```ini
MemoryLow=1G
```

가능한 한 reclaim에서 보호. `MemoryMin` 보다 약함.

### MemoryHigh (소프트 한도)

```ini
MemoryHigh=2G
```

이 한도를 넘으면 active reclaim이 시작되고 프로세스가 throttling됨. **OOM은 발생하지 않음**.

### MemoryMax (하드 한도)

```ini
MemoryMax=4G
```

절대 한도. 넘으면 cgroup OOM killer가 프로세스를 죽임. v1의 `MemoryLimit=` 대체.

### MemorySwapMax

```ini
MemorySwapMax=0
```

swap 사용 한도. 0으로 두면 swap 사용 금지.

### 추천 패턴

```ini
[Service]
MemoryAccounting=yes      # 사용량 추적 (대부분 자동)
MemoryHigh=3G             # 부드러운 경고
MemoryMax=4G              # 절대 한도
```

---

## IO 제어

블록 디바이스 IO 대역폭 제어. cgroups v2에서 큰 폭으로 개선되었습니다.

### IOWeight (상대 비중)

```ini
IOWeight=200
StartupIOWeight=500
```

기본 100, 범위 1~10000.

### IODevice 제어

```ini
IOReadBandwidthMax=/dev/sda 50M
IOWriteBandwidthMax=/dev/sda 30M
IOReadIOPSMax=/dev/sda 1000
IOWriteIOPSMax=/dev/sda 500
```

특정 디바이스에 대한 절대 한도.

### IODeviceWeight

```ini
IODeviceWeight=/dev/sda 500
```

특정 디바이스에 대한 가중치.

> 참고: IO 컨트롤러는 파일시스템 종류와 IO 스케줄러에 따라 동작이 달라집니다. ext4/xfs + bfq/mq-deadline 조합이 가장 잘 동작합니다.

---

## 태스크 수와 기타

### TasksMax

```ini
TasksMax=1024
```

cgroup이 가질 수 있는 최대 태스크(PID) 수. fork 폭탄 방어에 효과적.

### LimitNOFILE 등 (rlimit)

cgroup 컨트롤러는 아니지만 비슷한 역할.

```ini
LimitNOFILE=65536
LimitNPROC=2048
LimitCORE=infinity
LimitMEMLOCK=64K
```

각각 `setrlimit(2)` 의 RLIMIT_NOFILE, RLIMIT_NPROC, RLIMIT_CORE, RLIMIT_MEMLOCK에 매핑.

### OOMScoreAdjust

```ini
OOMScoreAdjust=-500
```

`/proc/<pid>/oom_score_adj` 에 해당. -1000 ~ 1000. 낮을수록 OOM killer가 죽일 가능성 낮음.

### OOMPolicy

```ini
OOMPolicy=stop
```

- `continue` (기본): 프로세스만 죽이고 unit은 계속
- `stop`: cgroup 안 프로세스가 OOM으로 죽으면 unit 전체 중지
- `kill`: cgroup 전체를 죽임

---

## Delegate

```ini
[Service]
Delegate=cpu memory io
```

해당 unit이 자식 cgroup을 직접 관리할 수 있도록 권한을 위임합니다. 컨테이너 런타임(systemd-nspawn, Docker 등)에서 사용하며, 일반 서비스에는 필요하지 않습니다.

---

## 관찰과 디버깅

### 실시간 리소스 사용량

```bash
systemd-cgtop
```

cgroup별 CPU/메모리/IO 사용량을 `top` 처럼 실시간으로 표시합니다.

### 특정 unit의 cgroup

```bash
$ systemctl status nginx.service
...
   CGroup: /system.slice/nginx.service
           ├─1234 nginx: master process /usr/sbin/nginx
           └─1235 nginx: worker process
```

### cgroup 파일 직접 보기

```bash
cd /sys/fs/cgroup/system.slice/nginx.service
cat memory.current        # 현재 메모리 사용량
cat memory.max            # 메모리 한도
cat cpu.stat              # CPU 통계 (usage_usec, throttled_usec)
cat io.stat               # IO 통계
cat cgroup.procs          # 이 cgroup의 PID 목록
```

### 임시 unit으로 실험

```bash
sudo systemd-run --unit=test-mem --scope -p MemoryMax=100M stress --vm 1 --vm-bytes 200M
```

`--scope` 는 외부 명령을 새 cgroup으로 감싸서 실행합니다. 즉석에서 리소스 한도를 테스트할 때 유용합니다.

### 누가 OOM에 죽었는지

```bash
journalctl -k | grep -i oom
journalctl -u nginx.service | grep -i oom
```

---

## 참고 자료

- [man systemd.resource-control](https://www.freedesktop.org/software/systemd/man/systemd.resource-control.html)
- [man systemd-cgtop](https://www.freedesktop.org/software/systemd/man/systemd-cgtop.html)
- [Kernel cgroup-v2 docs](https://docs.kernel.org/admin-guide/cgroup-v2.html)
- [Chris Down: Linux Memory Management at Scale](https://chrisdown.name/talks.html)
