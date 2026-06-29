# bpftool

> 원본: https://docs.kernel.org/bpf/bpftool.html

---

## 목차

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

## bpftool이란?

커널 BPF 객체(프로그램, 맵, 링크)를 검사하고 관리하는 표준 CLI. 커널 트리(`tools/bpf/bpftool/`)에 포함되어 커널 버전과 항상 일치한다.

용도:
- 현재 로드된 BPF 프로그램 조회
- map 내용 dump/edit
- 검증 후 BPF 코드, JIT된 native asm 보기
- skeleton, vmlinux.h 생성
- feature probe (커널이 어떤 BPF 기능을 지원하는지)

---

## 설치와 활성화

### 패키지

```bash
# Debian/Ubuntu
sudo apt install linux-tools-$(uname -r)
sudo apt install linux-tools-common

# RHEL/Fedora
sudo dnf install bpftool
```

### 직접 빌드

```bash
git clone https://github.com/libbpf/bpftool.git
cd bpftool/src
make
sudo make install
```

### 자동완성

```bash
sudo cp /usr/share/bash-completion/completions/bpftool /etc/bash_completion.d/
```

---

## feature probe

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

배포 환경에서 BPF 기능 동작 여부를 사전에 확인할 때 유용하다.

```bash
# JSON 출력
sudo bpftool feature probe -j | jq
```

---

## prog 서브커맨드

### 모든 프로그램 보기

```bash
$ sudo bpftool prog list
123: kprobe  name handle_event  tag 1234567890abcdef
        loaded_at 2026-05-08T09:00:00+0900  uid 0
        xlated 256B  jited 380B  memlock 4096B  map_ids 5,6
        btf_id 7

124: xdp  name xdp_drop  tag fedcba0987654321
        ...
```

### 특정 프로그램

```bash
sudo bpftool prog show id 123
sudo bpftool prog show name handle_event
sudo bpftool prog show pinned /sys/fs/bpf/myprog
```

### dump

```bash
# 검증 후 BPF 명령어
sudo bpftool prog dump xlated id 123

# JIT된 native asm
sudo bpftool prog dump jited id 123

# 소스 파일명·라인 번호·컬럼 정보 함께 표시
sudo bpftool prog dump xlated id 123 linum
sudo bpftool prog dump jited id 123 linum
```

### 로드와 attach

```bash
sudo bpftool prog load my.bpf.o /sys/fs/bpf/myprog \
    type kprobe

sudo bpftool prog attach pinned /sys/fs/bpf/myprog \
    cgroup_skb_egress \
    cgroup /sys/fs/cgroup/my-cg
```

### 통계

```bash
sudo bpftool prog profile id 123 duration 5 cycles instructions
```

5초 동안 cycles와 instructions 카운터를 측정한다.

### 트레이싱 출력

```bash
sudo bpftool prog tracelog
```

`/sys/kernel/debug/tracing/trace_pipe`를 읽는 것과 동일하다.

---

## map 서브커맨드

### 모든 맵

```bash
$ sudo bpftool map list
5: hash  name counter  flags 0x0
        key 4B  value 8B  max_entries 1024  memlock 86048B
6: ringbuf  name events  flags 0x0
        key 0B  value 0B  max_entries 262144  memlock 263192B
```

### 내용 dump

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

### lookup / update / delete

```bash
sudo bpftool map lookup id 5 key 00 00 00 00
sudo bpftool map update id 5 key 00 00 00 00 value 64 00 00 00 00 00 00 00
sudo bpftool map delete id 5 key 00 00 00 00
```

### 파일/Pin에서

```bash
sudo bpftool map dump pinned /sys/fs/bpf/my_map
```

### 생성

```bash
sudo bpftool map create /sys/fs/bpf/manual_map \
    type hash key 4 value 8 entries 1024 name manual_map
```

### Per-CPU 합

```bash
sudo bpftool map dump id 5 --pretty
```

PERCPU 맵은 모든 CPU의 값을 자동으로 표시한다.

### Watch (실시간)

`--no-pager` 와 결합해 watch:
```bash
watch -n 1 'bpftool map dump id 5 -p'
```

---

## link와 attach 관리

`link`는 BPF 프로그램의 attachment를 추상화한 객체다. 커널 5.7+에서 지원.

```bash
$ sudo bpftool link list
1: kprobe  prog 123
        bpf_cookie 0
2: xdp  prog 124  ifindex 2
```

### detach

```bash
sudo bpftool link detach id 1
```

### Pin

```bash
sudo bpftool link pin id 1 /sys/fs/bpf/my_link
```

핀하면 호출자 프로세스가 종료돼도 attach가 유지된다.

---

## BTF 도구

### 커널 BTF 보기

```bash
sudo bpftool btf dump file /sys/kernel/btf/vmlinux | head -30
sudo bpftool btf dump id 1                  # ID로
sudo bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h
```

### 모듈 BTF

```bash
ls /sys/kernel/btf/
sudo bpftool btf dump file /sys/kernel/btf/nf_conntrack
```

### 프로그램의 BTF

```bash
sudo bpftool prog show id 123 -j | jq .btf_id
sudo bpftool btf dump id <btf-id>
```

---

## Skeleton 생성

CO-RE 기반 BPF 사용자 공간 코드를 자동으로 생성하는 인터페이스.

```bash
bpftool gen skeleton my.bpf.o > my.skel.h
```

`my.skel.h` 는 다음을 제공:
- `struct my_bpf`
- `my_bpf__open()`, `__load()`, `__attach()`, `__destroy()`
- `skel->progs.<프로그램명>`, `skel->maps.<맵명>`, `skel->bss`, `skel->rodata`

### 옵션

```bash
bpftool gen skeleton my.bpf.o name my_app > my.skel.h
bpftool gen object my-merged.bpf.o my.bpf.o other.bpf.o   # 여러 ELF 머지
```

---

## Pin 관리

BPF 객체를 영구화한다. `/sys/fs/bpf/`가 BPF FS 마운트 경로다.

### 마운트 확인

```bash
mount | grep bpf
# 보통 systemd가 자동 마운트
```

### Pin

```bash
sudo bpftool prog pin id 123 /sys/fs/bpf/myprog
sudo bpftool map pin id 5 /sys/fs/bpf/my_map
sudo bpftool link pin id 1 /sys/fs/bpf/my_link
```

### Unpin

```bash
sudo rm /sys/fs/bpf/myprog
```

객체에 대한 마지막 fd 참조가 사라지면 자동으로 해제된다.

### 여러 객체 한 번에

```bash
sudo bpftool prog loadall my.bpf.o /sys/fs/bpf/myprogs
ls /sys/fs/bpf/myprogs/
# handle_event  do_open  ...
```

---

## 자주 쓰는 패턴

### 시스템에 로드된 모든 BPF 보기

```bash
sudo bpftool prog
sudo bpftool map
sudo bpftool link
```

### 의심스러운 BPF 발견 시 추적

```bash
# 어느 프로세스가 로드했나
sudo bpftool prog show id 123 -j | jq .pids

# 어느 hook에 attach 됐나
sudo bpftool link list | grep "prog 123"

# JIT된 native asm 디스어셈블
sudo bpftool prog dump jited id 123 | head -20
```

### map 카운터 watch

```bash
watch -n 1 'sudo bpftool map dump id 5 -p | jq -r ".[] | [.key, .value] | @tsv"'
```

### CO-RE 프로젝트 부트스트랩

```bash
# vmlinux.h 생성
sudo bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h

# 빌드 후 skeleton
clang -O2 -target bpf -c my.bpf.c -o my.bpf.o
bpftool gen skeleton my.bpf.o > my.skel.h
```

---

## 참고 자료

- [Kernel docs: bpftool](https://docs.kernel.org/bpf/bpftool.html)
- [bpftool GitHub mirror](https://github.com/libbpf/bpftool)
- [Andrii: bpftool tips and tricks](https://nakryiko.com/posts/bpftool/)
