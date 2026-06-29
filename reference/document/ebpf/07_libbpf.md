# libbpf

> 원본: https://github.com/libbpf/libbpf , https://libbpf.readthedocs.io/

---

## 목차

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

## libbpf란?

BPF 프로그램을 로드·관리하는 표준 C 라이브러리. 커널 BPF 서브시스템 메인테이너들이 함께 관리합니다.

특징:
- BPF ELF 파싱
- CO-RE 재배치 처리
- BPF FS 핀
- 다양한 attach 방식 지원
- Skeleton 자동 생성으로 매우 깔끔한 사용자 코드

---

## BCC vs libbpf

| 항목 | BCC | libbpf |
| --- | --- | --- |
| 컴파일 시점 | 런타임 | 빌드 타임 |
| 의존성 | LLVM, 커널 헤더 | 없음 (또는 BTF) |
| 배포 크기 | 큼 | 작음 (몇 MB) |
| 시작 시간 | 느림 (~수 초) | 빠름 (~수 십 ms) |
| 사용 언어 | Python, C++ | C |
| 학습 곡선 | 쉬움 | 약간 가파름 |
| 휴대성 | 매번 컴파일 | CO-RE로 한 번 |
| 권장 | 학습, prototyping | 프로덕션 |

현재 BCC의 일부 도구도 내부적으로 libbpf로 마이그레이션 중 (`/usr/share/bcc/libbpf-tools/`).

---

## Skeleton

`bpftool gen skeleton`으로 생성되는 헤더. BPF 오브젝트의 사용자 공간 인터페이스를 자동으로 생성합니다.

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

## 기본 워크플로우

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

### Open + Load + Attach 한 번에

```c
struct my_bpf *skel = my_bpf__open_and_load();
my_bpf__attach(skel);
```

---

## Map 조작

### Skeleton 통해

```c
int fd = bpf_map__fd(skel->maps.my_map);
```

### bpf_map__lookup_elem 등

```c
u32 key = 0;
u64 value;
bpf_map__lookup_elem(skel->maps.counter, &key, sizeof(key),
                     &value, sizeof(value), 0);
```

### 글로벌 변수

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

### 명령줄에서 map 보기

```bash
sudo bpftool map dump pinned /sys/fs/bpf/my_map
sudo bpftool map dump id 42
```

---

## 이벤트 수신

### Ring Buffer

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

### Perf Event Array

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

## Attach 종류

skeleton의 `__attach()`는 모든 프로그램을 한꺼번에 부착하지만, 개별 프로그램만 부착할 수도 있습니다.

### 자동 부착 (SEC()의 형식 따라)

```c
bpf_program__attach(skel->progs.handle_event);
```

### kprobe/kretprobe

```c
bpf_program__attach_kprobe(skel->progs.do_open, false /* not retprobe */, "vfs_open");
bpf_program__attach_kprobe(skel->progs.do_open_ret, true, "vfs_open");
```

### uprobe

```c
bpf_program__attach_uprobe(skel->progs.malloc_enter, false, -1 /* any pid */,
                           "/usr/lib/libc.so.6", offset);
```

### tracepoint

```c
bpf_program__attach_tracepoint(skel->progs.tp_handler, "syscalls", "sys_enter_open");
```

### XDP

```c
bpf_program__attach_xdp(skel->progs.xdp_handler, ifindex);
```

### TC

```c
bpf_program__attach_tcx(skel->progs.tc_handler, ifindex, &opts);
```

### Cgroup

```c
int cgroup_fd = open("/sys/fs/cgroup/my-group", O_RDONLY);
bpf_program__attach_cgroup(skel->progs.cg_handler, cgroup_fd);
```

### Network namespace

특정 namespace에서 attach하려면 `setns()` 호출 후 attach.

---

## bpftool과의 관계

`bpftool`은 BPF 객체를 명령줄에서 다루는 도구. libbpf와 일부 코드를 공유합니다.

### Skeleton 생성

```bash
bpftool gen skeleton my.bpf.o > my.skel.h
```

### vmlinux.h 생성

```bash
bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h
```

### feature probe

```bash
bpftool feature probe              # 커널 BPF 기능 모두 보기
bpftool feature probe full         # helper 셋도 모두
```

### 디버깅

```bash
bpftool prog list
bpftool prog dump xlated id <id>
bpftool prog dump jited id <id>
bpftool map list
bpftool map dump id <id>
bpftool link list
bpftool perf list
```

### libbpf static link

작은 바이너리를 위한 정적 링크 예시:
```bash
cc -O2 -o myapp myapp.c \
   ${LIBBPF_SRC}/libbpf.a \
   -lelf -lz
```

---

## 언어 바인딩

### Go

#### cilium/ebpf

```go
import "github.com/cilium/ebpf"

spec, _ := ebpf.LoadCollectionSpec("myprog.bpf.o")
coll, _ := ebpf.NewCollection(spec)
defer coll.Close()

prog := coll.Programs["handle_event"]
link, _ := link.Kprobe("vfs_open", prog, nil)
defer link.Close()
```

ebpf-go는 Pure Go 구현 (C 의존성 없음). 가장 널리 쓰입니다.

#### libbpfgo

```go
import "github.com/aquasecurity/libbpfgo"

bpfModule, _ := libbpfgo.NewModuleFromFile("myprog.bpf.o")
bpfModule.BPFLoadObject()
```

libbpf C 라이브러리를 cgo로 래핑합니다.

### Rust

#### aya

```rust
use aya::Bpf;
use aya::programs::KProbe;

let mut bpf = Bpf::load_file("myprog.bpf.o")?;
let program: &mut KProbe = bpf.program_mut("handle_event").unwrap().try_into()?;
program.load()?;
program.attach("vfs_open", 0)?;
```

Pure Rust. Rust BPF 생태계 중 가장 활발합니다.

#### libbpf-rs

C libbpf의 Rust 바인딩입니다.

### Python

#### BCC

여전히 활발히 사용됩니다. CO-RE는 일부만 지원합니다.

#### bpfd

(실험적) BPF 데몬. CRD 기반 attach.

---

## 참고 자료

- [libbpf GitHub](https://github.com/libbpf/libbpf)
- [libbpf-bootstrap](https://github.com/libbpf/libbpf-bootstrap)
- [bpftool docs](https://docs.kernel.org/bpf/bpftool.html)
- [Andrii Nakryiko's blog](https://nakryiko.com/)
- [cilium/ebpf](https://github.com/cilium/ebpf)
- [aya-rs](https://aya-rs.dev/)
