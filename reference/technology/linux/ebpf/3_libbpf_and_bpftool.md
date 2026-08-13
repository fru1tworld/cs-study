# libbpf와 bpftool

## libbpf

> 원본: https://github.com/libbpf/libbpf , https://libbpf.readthedocs.io/

---

### 목차

1. [libbpf란?](#libbpf란)
2. [BCC vs libbpf](#bcc-vs-libbpf)
3. [Skeleton](#skeleton)
4. [기본 워크플로우](#기본-워크플로우)
5. [Map 조작](#map-조작)
6. [이벤트 수신](#이벤트-수신)
7. [Attach 종류](#attach-종류)
8. [bpftool과의 관계](#bpftool과의-관계)
9. [언어 바인딩](#언어-바인딩)
10. [참고 자료](#참고-자료)

---

### libbpf란?

BPF 프로그램을 로드·관리하는 표준 C 라이브러리 → 커널 BPF 서브시스템 메인테이너들이 함께 관리함.

특징:
- BPF ELF 파싱
- CO-RE 재배치 처리
- BPF FS 핀
- 다양한 attach 방식 지원
- Skeleton 자동 생성으로 매우 깔끔한 사용자 코드

---

### BCC vs libbpf

- 컴파일 시점: BCC는 런타임 · libbpf는 빌드 타임
- 의존성: BCC는 LLVM, 커널 헤더 필요 · libbpf는 없음(또는 BTF)
- 배포 크기: BCC는 큼 · libbpf는 작음(몇 MB)
- 시작 시간: BCC는 느림(~수 초) · libbpf는 빠름(~수 십 ms)
- 사용 언어: BCC는 Python, C++ · libbpf는 C
- 학습 곡선: BCC는 쉬움 · libbpf는 약간 가파름
- 휴대성: BCC는 매번 컴파일 필요 · libbpf는 CO-RE로 한 번
- 권장 용도: BCC는 학습·prototyping · libbpf는 프로덕션

현재 BCC의 일부 도구도 내부적으로 libbpf로 마이그레이션 중임 (`/usr/share/bcc/libbpf-tools/`).

---

### Skeleton

`bpftool gen skeleton`으로 생성되는 헤더 → BPF 오브젝트의 사용자 공간 인터페이스를 자동으로 생성함.

```bash
bpftool gen skeleton my.bpf.o > my.skel.h
```

생성된 `my.skel.h` 안에는:
- `struct my_bpf` — 모든 maps, programs, variables를 가진 구조체
- `my_bpf__open()` — BPF 객체 파싱
- `my_bpf__load()` — 커널에 로드
- `my_bpf__attach()` — 모든 프로그램 부착
- `my_bpf__destroy()` — 정리

---

### 기본 워크플로우

```c
#include "my.skel.h"

int main() {
    struct my_bpf *skel;
    int err;

    /* 1) Open: ELF 파싱, 메모리에 BPF objects 준비 */
    skel = my_bpf__open();
    if (!skel) return 1;

    /* 2) (선택) 로드 전 변수/맵 크기 조정 */
    skel->bss->target_pid = atoi(argv[1]);
    bpf_map__set_max_entries(skel->maps.events, 4 * 1024 * 1024);

    /* 3) Load: 커널에 로드 + verifier */
    err = my_bpf__load(skel);
    if (err) goto cleanup;

    /* 4) Attach: hook에 부착 */
    err = my_bpf__attach(skel);
    if (err) goto cleanup;

    /* 5) 메인 루프 — 이벤트 처리 */
    while (!exit_flag) {
        // ringbuf poll, map dump 등
    }

cleanup:
    my_bpf__destroy(skel);
    return err < 0 ? -err : 0;
}
```

#### Open + Load + Attach 한 번에

```c
struct my_bpf *skel = my_bpf__open_and_load();
my_bpf__attach(skel);
```

---

### Map 조작

#### Skeleton 통해

```c
int fd = bpf_map__fd(skel->maps.my_map);
```

#### bpf_map__lookup_elem 등

```c
u32 key = 0;
u64 value;
bpf_map__lookup_elem(skel->maps.counter, &key, sizeof(key),
                     &value, sizeof(value), 0);
```

#### 글로벌 변수

BPF 측에서 다음과 같이 선언:
```c
const volatile int target_pid = 0;     // .rodata
int counter = 0;                        // .bss
```

사용자 공간에서:
```c
skel->rodata->target_pid = 1234;       // load 전에만 가능
printf("counter: %d\n", skel->bss->counter);    // 항상 가능
```

`rodata` (read-only data)는 BPF 입장에서 const, 사용자가 load 전 한 번 설정.
`bss` 는 양방향 공유 변수.

#### 명령줄에서 map 보기

```bash
sudo bpftool map dump pinned /sys/fs/bpf/my_map
sudo bpftool map dump id 42
```

---

### 이벤트 수신

#### Ring Buffer

```c
static int handle_event(void *ctx, void *data, size_t sz) {
    struct event *e = data;
    printf("pid=%u\n", e->pid);
    return 0;
}

struct ring_buffer *rb = ring_buffer__new(
    bpf_map__fd(skel->maps.events), handle_event, NULL, NULL);

while (!exit_flag) {
    int err = ring_buffer__poll(rb, 100);     // 100ms timeout
    if (err == -EINTR) continue;
    if (err < 0) break;
}

ring_buffer__free(rb);
```

#### Perf Event Array

```c
struct perf_buffer *pb = perf_buffer__new(
    bpf_map__fd(skel->maps.events), 8 /* pages per CPU */,
    handle_event, handle_lost, NULL, NULL);

while (!exit_flag) {
    perf_buffer__poll(pb, 100);
}

perf_buffer__free(pb);
```

---

### Attach 종류

skeleton의 `__attach()`는 모든 프로그램을 한꺼번에 부착 → 개별 프로그램만 부착도 가능.

#### 자동 부착 (SEC()의 형식 따라)

```c
bpf_program__attach(skel->progs.handle_event);
```

#### kprobe/kretprobe

```c
bpf_program__attach_kprobe(skel->progs.do_open, false /* not retprobe */, "vfs_open");
bpf_program__attach_kprobe(skel->progs.do_open_ret, true, "vfs_open");
```

#### uprobe

```c
bpf_program__attach_uprobe(skel->progs.malloc_enter, false, -1 /* any pid */,
                           "/usr/lib/libc.so.6", offset);
```

#### tracepoint

```c
bpf_program__attach_tracepoint(skel->progs.tp_handler, "syscalls", "sys_enter_open");
```

#### XDP

```c
bpf_program__attach_xdp(skel->progs.xdp_handler, ifindex);
```

#### TC

```c
bpf_program__attach_tcx(skel->progs.tc_handler, ifindex, &opts);
```

#### Cgroup

```c
int cgroup_fd = open("/sys/fs/cgroup/my-group", O_RDONLY);
bpf_program__attach_cgroup(skel->progs.cg_handler, cgroup_fd);
```

#### Network namespace

특정 namespace에서 attach하려면 `setns()` 호출 후 attach.

---

### bpftool과의 관계

`bpftool`은 BPF 객체를 명령줄에서 다루는 도구 → libbpf와 일부 코드를 공유함.

#### Skeleton 생성

```bash
bpftool gen skeleton my.bpf.o > my.skel.h
```

#### vmlinux.h 생성

```bash
bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h
```

#### feature probe

```bash
bpftool feature probe              # 커널 BPF 기능 모두 보기
bpftool feature probe full         # helper 셋도 모두
```

#### 디버깅

```bash
bpftool prog list
bpftool prog dump xlated id <id>
bpftool prog dump jited id <id>
bpftool map list
bpftool map dump id <id>
bpftool link list
bpftool perf list
```

#### libbpf static link

작은 바이너리를 위한 정적 링크 예시:
```bash
cc -O2 -o myapp myapp.c \
   ${LIBBPF_SRC}/libbpf.a \
   -lelf -lz
```

---

### 언어 바인딩

#### Go

##### cilium/ebpf

```go
import "github.com/cilium/ebpf"

spec, _ := ebpf.LoadCollectionSpec("myprog.bpf.o")
coll, _ := ebpf.NewCollection(spec)
defer coll.Close()

prog := coll.Programs["handle_event"]
link, _ := link.Kprobe("vfs_open", prog, nil)
defer link.Close()
```

ebpf-go는 Pure Go 구현(C 의존성 없음) → 가장 널리 쓰임.

##### libbpfgo

```go
import "github.com/aquasecurity/libbpfgo"

bpfModule, _ := libbpfgo.NewModuleFromFile("myprog.bpf.o")
bpfModule.BPFLoadObject()
```

libbpf C 라이브러리를 cgo로 래핑함.

#### Rust

##### aya

```rust
use aya::Bpf;
use aya::programs::KProbe;

let mut bpf = Bpf::load_file("myprog.bpf.o")?;
let program: &mut KProbe = bpf.program_mut("handle_event").unwrap().try_into()?;
program.load()?;
program.attach("vfs_open", 0)?;
```

Pure Rust → Rust BPF 생태계 중 가장 활발함.

##### libbpf-rs

C libbpf의 Rust 바인딩임.

#### Python

##### BCC

여전히 활발히 사용됨 → CO-RE는 일부만 지원.

##### bpfd

(실험적) BPF 데몬. CRD 기반 attach.

---

### 참고 자료

- [libbpf GitHub](https://github.com/libbpf/libbpf)
- [libbpf-bootstrap](https://github.com/libbpf/libbpf-bootstrap)
- [bpftool docs](https://docs.kernel.org/bpf/bpftool.html)
- [Andrii Nakryiko's blog](https://nakryiko.com/)
- [cilium/ebpf](https://github.com/cilium/ebpf)
- [aya-rs](https://aya-rs.dev/)

---

## bpftool

> 원본: https://docs.kernel.org/bpf/bpftool.html

---

### 목차

1. [bpftool이란?](#bpftool이란)
2. [설치와 활성화](#설치와-활성화)
3. [feature probe](#feature-probe)
4. [prog 서브커맨드](#prog-서브커맨드)
5. [map 서브커맨드](#map-서브커맨드)
6. [link와 attach 관리](#link와-attach-관리)
7. [BTF 도구](#btf-도구)
8. [Skeleton 생성](#skeleton-생성)
9. [Pin 관리](#pin-관리)
10. [참고 자료](#참고-자료)

---

### bpftool이란?

커널 BPF 객체(프로그램, 맵, 링크)를 검사하고 관리하는 표준 CLI → 커널 트리(`tools/bpf/bpftool/`)에 포함되어 커널 버전과 항상 일치함.

용도:
- 현재 로드된 BPF 프로그램 조회
- map 내용 dump/edit
- 검증 후 BPF 코드, JIT된 native asm 보기
- skeleton, vmlinux.h 생성
- feature probe (커널이 어떤 BPF 기능을 지원하는지)

---

### 설치와 활성화

#### 패키지

```bash
# Debian/Ubuntu
sudo apt install linux-tools-$(uname -r)
sudo apt install linux-tools-common

# RHEL/Fedora
sudo dnf install bpftool
```

#### 직접 빌드

```bash
git clone https://github.com/libbpf/bpftool.git
cd bpftool/src
make
sudo make install
```

#### 자동완성

```bash
sudo cp /usr/share/bash-completion/completions/bpftool /etc/bash_completion.d/
```

---

### feature probe

커널이 어떤 BPF 기능을 지원하는지 진단.

```bash
sudo bpftool feature probe
```

출력 일부:
```
Scanning system call availability...
bpf() syscall is available

Scanning eBPF program types...
eBPF program_type kprobe is available
eBPF program_type xdp is available
...

Scanning eBPF map types...
eBPF map_type hash is available
eBPF map_type ringbuf is available
...

Scanning eBPF helper functions...
eBPF helpers supported for program type kprobe:
        - bpf_map_lookup_elem
        - bpf_map_update_elem
        - ...
```

배포 환경에서 BPF 기능 동작 여부를 사전에 확인할 때 유용함.

```bash
# JSON 출력
sudo bpftool feature probe -j | jq
```

---

### prog 서브커맨드

#### 모든 프로그램 보기

```bash
$ sudo bpftool prog list
123: kprobe  name handle_event  tag 1234567890abcdef
        loaded_at 2026-05-08T09:00:00+0900  uid 0
        xlated 256B  jited 380B  memlock 4096B  map_ids 5,6
        btf_id 7

124: xdp  name xdp_drop  tag fedcba0987654321
        ...
```

#### 특정 프로그램

```bash
sudo bpftool prog show id 123
sudo bpftool prog show name handle_event
sudo bpftool prog show pinned /sys/fs/bpf/myprog
```

#### dump

```bash
# 검증 후 BPF 명령어
sudo bpftool prog dump xlated id 123

# JIT된 native asm
sudo bpftool prog dump jited id 123

# 소스 파일명·라인 번호·컬럼 정보 함께 표시
sudo bpftool prog dump xlated id 123 linum
sudo bpftool prog dump jited id 123 linum
```

#### 로드와 attach

```bash
sudo bpftool prog load my.bpf.o /sys/fs/bpf/myprog \
    type kprobe

sudo bpftool prog attach pinned /sys/fs/bpf/myprog \
    cgroup_skb_egress \
    cgroup /sys/fs/cgroup/my-cg
```

#### 통계

```bash
sudo bpftool prog profile id 123 duration 5 cycles instructions
```

5초 동안 cycles와 instructions 카운터를 측정함.

#### 트레이싱 출력

```bash
sudo bpftool prog tracelog
```

`/sys/kernel/debug/tracing/trace_pipe`를 읽는 것과 동일함.

---

### map 서브커맨드

#### 모든 맵

```bash
$ sudo bpftool map list
5: hash  name counter  flags 0x0
        key 4B  value 8B  max_entries 1024  memlock 86048B
6: ringbuf  name events  flags 0x0
        key 0B  value 0B  max_entries 262144  memlock 263192B
```

#### 내용 dump

```bash
sudo bpftool map dump id 5
```

```
key: 00 00 00 00  value: 2a 00 00 00 00 00 00 00
key: 01 00 00 00  value: 0f 00 00 00 00 00 00 00
Found 2 elements
```

JSON 출력:
```bash
sudo bpftool map dump id 5 -j | jq
```

#### lookup / update / delete

```bash
sudo bpftool map lookup id 5 key 00 00 00 00
sudo bpftool map update id 5 key 00 00 00 00 value 64 00 00 00 00 00 00 00
sudo bpftool map delete id 5 key 00 00 00 00
```

#### 파일/Pin에서

```bash
sudo bpftool map dump pinned /sys/fs/bpf/my_map
```

#### 생성

```bash
sudo bpftool map create /sys/fs/bpf/manual_map \
    type hash key 4 value 8 entries 1024 name manual_map
```

#### Per-CPU 합

```bash
sudo bpftool map dump id 5 --pretty
```

PERCPU 맵은 모든 CPU의 값을 자동으로 표시함.

#### Watch (실시간)

`--no-pager` 와 결합해 watch:
```bash
watch -n 1 'bpftool map dump id 5 -p'
```

---

### link와 attach 관리

`link`는 BPF 프로그램의 attachment를 추상화한 객체임 → 커널 5.7+에서 지원.

```bash
$ sudo bpftool link list
1: kprobe  prog 123
        bpf_cookie 0
2: xdp  prog 124  ifindex 2
```

#### detach

```bash
sudo bpftool link detach id 1
```

#### Pin

```bash
sudo bpftool link pin id 1 /sys/fs/bpf/my_link
```

핀하면 호출자 프로세스가 종료돼도 attach가 유지됨.

---

### BTF 도구

#### 커널 BTF 보기

```bash
sudo bpftool btf dump file /sys/kernel/btf/vmlinux | head -30
sudo bpftool btf dump id 1                  # ID로
sudo bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h
```

#### 모듈 BTF

```bash
ls /sys/kernel/btf/
sudo bpftool btf dump file /sys/kernel/btf/nf_conntrack
```

#### 프로그램의 BTF

```bash
sudo bpftool prog show id 123 -j | jq .btf_id
sudo bpftool btf dump id <btf-id>
```

---

### Skeleton 생성

CO-RE 기반 BPF 사용자 공간 코드를 자동으로 생성하는 인터페이스.

```bash
bpftool gen skeleton my.bpf.o > my.skel.h
```

`my.skel.h` 는 다음을 제공:
- `struct my_bpf`
- `my_bpf__open()`, `__load()`, `__attach()`, `__destroy()`
- `skel->progs.<프로그램명>`, `skel->maps.<맵명>`, `skel->bss`, `skel->rodata`

#### 옵션

```bash
bpftool gen skeleton my.bpf.o name my_app > my.skel.h
bpftool gen object my-merged.bpf.o my.bpf.o other.bpf.o   # 여러 ELF 머지
```

---

### Pin 관리

BPF 객체를 영구화함. `/sys/fs/bpf/`가 BPF FS 마운트 경로임.

#### 마운트 확인

```bash
mount | grep bpf
# 보통 systemd가 자동 마운트
```

#### Pin

```bash
sudo bpftool prog pin id 123 /sys/fs/bpf/myprog
sudo bpftool map pin id 5 /sys/fs/bpf/my_map
sudo bpftool link pin id 1 /sys/fs/bpf/my_link
```

#### Unpin

```bash
sudo rm /sys/fs/bpf/myprog
```

객체에 대한 마지막 fd 참조가 사라지면 자동으로 해제됨.

#### 여러 객체 한 번에

```bash
sudo bpftool prog loadall my.bpf.o /sys/fs/bpf/myprogs
ls /sys/fs/bpf/myprogs/
# handle_event  do_open  ...
```

---

### 자주 쓰는 패턴

#### 시스템에 로드된 모든 BPF 보기

```bash
sudo bpftool prog
sudo bpftool map
sudo bpftool link
```

#### 의심스러운 BPF 발견 시 추적

```bash
# 어느 프로세스가 로드했나
sudo bpftool prog show id 123 -j | jq .pids

# 어느 hook에 attach 됐나
sudo bpftool link list | grep "prog 123"

# JIT된 native asm 디스어셈블
sudo bpftool prog dump jited id 123 | head -20
```

#### map 카운터 watch

```bash
watch -n 1 'sudo bpftool map dump id 5 -p | jq -r ".[] | [.key, .value] | @tsv"'
```

#### CO-RE 프로젝트 부트스트랩

```bash
# vmlinux.h 생성
sudo bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h

# 빌드 후 skeleton
clang -O2 -target bpf -c my.bpf.c -o my.bpf.o
bpftool gen skeleton my.bpf.o > my.skel.h
```

---

### 참고 자료

- [Kernel docs: bpftool](https://docs.kernel.org/bpf/bpftool.html)
- [bpftool GitHub mirror](https://github.com/libbpf/bpftool)
- [Andrii: bpftool tips and tricks](https://nakryiko.com/posts/bpftool/)
