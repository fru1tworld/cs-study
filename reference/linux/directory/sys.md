# `/sys` — sysfs (커널 객체 모델)

> "a pseudo-filesystem which provides an interface to kernel data structures" — `sysfs(5)`
> "provides information about the kernel like /proc, but better structured" — `hier(7)`

`/proc`이 "프로세스 중심 + 잡다한 커널 정보"라면, `/sys`는 **커널의 디바이스·서브시스템 모델**을 그대로 노출한다. 커널 내부의 **kobject**(kernel object) 트리가 곧 sysfs 구조다. 심볼릭 링크가 매우 적극적으로 쓰여서, **하나의 디바이스가 여러 관점(`bus`, `class`, `block`)에서 동시에 보인다**.

## 마운트

```bash
mount -t sysfs sysfs /sys
```

대부분 read-only지만, 일부 속성은 쓰기 가능 (스케줄러 변경, 디바이스 enable/disable 등).

## 디렉터리 레이아웃

### `/sys/block`

발견된 **블록 디바이스**마다 심볼릭 링크가 하나씩 — `/sys/devices/...`로 연결.

```
/sys/block/sda → ../devices/pci0000:00/0000:00:1f.2/ata1/host0/.../sda
/sys/block/sda/queue/scheduler   # 현재 I/O 스케줄러 [mq-deadline kyber bfq none]
/sys/block/sda/queue/rotational  # 1=HDD, 0=SSD
/sys/block/sda/queue/read_ahead_kb
/sys/block/sda/size              # 섹터 수
/sys/block/sda/sda1/             # 파티션
```

### `/sys/bus`

커널 **버스 타입**별 디렉터리 (pci, usb, scsi, i2c, virtio, platform, …).

```
/sys/bus/<bus>/
├── devices/       이 버스에 연결된 디바이스 심링크
├── drivers/       이 버스에 등록된 드라이버
│   └── <driver>/
│       ├── bind        쓰면 디바이스를 드라이버에 바인드
│       ├── unbind      쓰면 언바인드
│       └── module → ../../../../module/<modname>
└── drivers_autoprobe
```

`echo 0000:01:00.0 > /sys/bus/pci/drivers/<drv>/unbind` 같은 식으로 런타임에 드라이버 분리 가능.

### `/sys/class`

**기능별 분류** (블록 디바이스, 네트워크, 그래픽, 사운드, 입력, 배터리 등). 같은 디바이스가 `bus`에도 나타나지만 `class`에서는 "역할" 관점으로 묶인다.

```
/sys/class/
├── net/        네트워크 인터페이스
│   ├── eth0 → ../../devices/.../net/eth0
│   ├── eth0/address       MAC 주소
│   ├── eth0/operstate     up/down
│   ├── eth0/mtu
│   └── eth0/statistics/   rx_bytes, tx_bytes, ...
├── block/      블록 디바이스
├── tty/        터미널
├── input/      키보드, 마우스, 터치패드
├── power_supply/  배터리, AC 어댑터
├── thermal/    온도 센서, 쿨링 디바이스
├── leds/       LED 컨트롤
└── backlight/  화면 밝기
```

**중요**: `/sys/class/net`은 **네트워크 namespace별로 다르게 보인다** — 그 프로세스에서 보이는 인터페이스만 노출.

### `/sys/dev`

major:minor로 디바이스 노드를 찾기 위한 인덱스.

```
/sys/dev/
├── block/<MAJ>:<MIN> → /sys/devices/.../<name>
└── char/<MAJ>:<MIN>  → ...
```

`stat(2)`로 얻은 `st_rdev`로 sysfs 진입점을 찾을 때 유용.

### `/sys/devices`

> "the kernel device tree, which is a hierarchy of device structures within the kernel" — `sysfs(5)`

**진짜 디바이스 트리.** 다른 디렉터리들의 심링크가 모두 여기로 향한다. 물리적 토폴로지(PCI/USB 버스 계층)를 반영.

```
/sys/devices/
├── pci0000:00/
│   └── 0000:00:1f.2/      PCI BDF
│       ├── ata1/
│       │   └── host0/target0:0:0/0:0:0:0/
│       │       └── block/sda/...
│       ├── vendor, device, class, irq
│       └── power/, driver → ...
├── platform/              플랫폼 디바이스 (ACPI 등)
├── system/cpu/            CPU
│   ├── cpu0/topology/
│   ├── cpu0/cpufreq/      주파수 조절
│   ├── online             CPU hot-plug
│   └── vulnerabilities/   spectre/meltdown 상태
└── virtual/               순수 가상 디바이스 (loopback, tun, …)
```

### `/sys/firmware`

> "interfaces for viewing and manipulating firmware-specific objects and attributes" — `sysfs(5)`

- `/sys/firmware/efi/` — EFI 변수, EFI 시스템 테이블
- `/sys/firmware/dmi/` — DMI/SMBIOS 정보 (`dmidecode` 출처)
- `/sys/firmware/acpi/` — ACPI 테이블

### `/sys/fs`

**파일시스템 자신이 노출하는 인터페이스.** 가장 중요한 것:

```
/sys/fs/
├── cgroup/         cgroup v2 통합 계층 마운트 포인트 → [cgroups.md](cgroups.md)
├── ext4/<dev>/     ext4 디바이스별 통계
├── xfs/<dev>/      XFS 통계, error tag
├── btrfs/<UUID>/   btrfs 디바이스/풀 정보
├── bpf/            pinned BPF 객체 (bpffs)
├── fuse/           FUSE 연결
├── smackfs/        SMACK LSM
└── selinux/        selinuxfs (LSM)
```

### `/sys/hypervisor`

하이퍼바이저(Xen 등)에서 가상 머신 정보. 보통 비어 있거나 Xen 게스트에서만.

### `/sys/kernel`

**커널 자체의 다양한 설정/상태**.

```
/sys/kernel/
├── cgroup/
│   ├── delegate    위임 가능한 cgroup 파일 목록
│   └── features    지원하는 cgroup v2 기능
├── debug/          debugfs 마운트 포인트 (디버깅 인터페이스)
│   └── tracing/    ftrace (tracefs로 별도 마운트되기도 함)
├── tracing/        ftrace (커널 함수 트레이서)
├── mm/             메모리 관리
│   ├── hugepages/  HugeTLB 페이지 크기별 풀
│   ├── ksm/        Kernel Samepage Merging
│   └── transparent_hugepage/  THP 정책
├── security/       LSM hooks
├── slab/           slab 캐시 (CONFIG_SLUB_DEBUG)
├── irq/            인터럽트 정보
├── notes           kernel ELF NOTE 섹션
├── vmcoreinfo      crash dump 메타
└── kexec_*         kexec/kdump
```

### `/sys/module`

로드된 **커널 모듈**마다 디렉터리:

```
/sys/module/<modname>/
├── coresize, initsize    크기
├── initstate             live, going, loading
├── refcnt                참조 카운트
├── srcversion            모듈 ABI 해시
├── taint                 모듈이 커널을 tainted 만들었는지
├── version               MODULE_VERSION
├── parameters/           모듈 파라미터 (런타임에 일부 수정 가능)
├── drivers/              이 모듈이 등록한 드라이버
├── holders/              이 모듈을 사용 중인 다른 모듈들
├── notes/                ELF NOTE
└── sections/             모듈 섹션 주소 (디버깅)
```

### `/sys/power`

전원 관리.
- `/sys/power/state` — 가능한 sleep 상태 (`freeze mem disk`) — 쓰면 진입
- `/sys/power/disk` — hibernate 모드
- `/sys/power/wakeup_count` — 깨우기 카운트

## 자주 쓰는 패턴

```bash
# 이 디스크가 SSD인가?
cat /sys/block/sda/queue/rotational     # 0=SSD, 1=HDD

# I/O 스케줄러 변경
echo bfq > /sys/block/sda/queue/scheduler

# 네트워크 통계
cat /sys/class/net/eth0/statistics/rx_bytes

# CPU offline / online
echo 0 > /sys/devices/system/cpu/cpu3/online    # cpu3 끔
echo 1 > /sys/devices/system/cpu/cpu3/online

# CPU 보안 취약점 상태
grep . /sys/devices/system/cpu/vulnerabilities/*

# 화면 밝기
echo 50 > /sys/class/backlight/<dev>/brightness

# THP 정책
echo madvise > /sys/kernel/mm/transparent_hugepage/enabled

# HugeTLB 페이지 예약
echo 1024 > /sys/kernel/mm/hugepages/hugepages-2048kB/nr_hugepages

# PCI 디바이스 드라이버 unbind
echo 0000:01:00.0 > /sys/bus/pci/devices/0000:01:00.0/driver/unbind

# 디스크 다시 스캔
echo 1 > /sys/block/sda/device/rescan
```

## procfs와의 차이 한눈에

| | `/proc` | `/sys` |
|---|---|---|
| 중심 | 프로세스, 잡다한 커널 상태 | 커널 객체 모델 (kobject) |
| 구조 | 평면적, 역사적으로 누적 | 엄격한 계층 + 심링크 |
| 마운트 타입 | `proc` | `sysfs` |
| 등장 | 매우 오래됨 | 2.6에서 정착 |
| 새 인터페이스 위치 | 더는 잘 안 추가됨 | 새 디바이스/서브시스템은 여기로 |
