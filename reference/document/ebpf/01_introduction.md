# eBPF 소개

> 이 문서는 ebpf.io 와 Linux 커널 BPF 문서를 한국어로 정리한 것입니다.
> 원본: https://ebpf.io/what-is-ebpf/ , https://docs.kernel.org/bpf/

---

## 목차

1. [eBPF란?](#ebpf란)
2. [역사](#역사)
3. [왜 eBPF가 중요한가](#왜-ebpf가-중요한가)
4. [동작 모델](#동작-모델)
5. [활용 영역](#활용-영역)
6. [도구 생태계](#도구-생태계)
7. [학습 출발점](#학습-출발점)
8. [참고 자료](#참고-자료)

---

## eBPF란?

**extended Berkeley Packet Filter**. 사용자가 작성한 작은 프로그램을 **Linux 커널 안에서 안전하게 실행** 할 수 있게 해주는 기술입니다.

핵심 특징:
- 커널을 재컴파일·재부팅하지 않고 동작 변경
- 검증기(verifier)가 안전성을 보장 (무한 루프 불가, 메모리 침범 불가)
- JIT 컴파일러가 네이티브 기계어로 변환해 거의 네이티브 속도
- 커널의 거의 모든 곳에 hook 가능 (시스템 콜, 네트워크, 파일시스템, 트레이스포인트)

eBPF는 "**커널을 위한 자바스크립트**"라고 자주 비유됩니다 — 안전하게 실행되는 격리된 환경, 동적 로딩, 풍부한 hook.

---

## 역사

### 1992 — 원조 BPF

UC Berkeley의 BSD 패킷 필터. tcpdump가 사용하는 단순한 가상머신. 패킷 헤더를 검사해 통과/차단을 결정하는 32비트 명령어 셋.

### 2014 — eBPF 도입

Alexei Starovoitov가 Linux 3.18에 eBPF를 머지. BPF 가상머신을 64비트로 확장하고, JIT 추가, 일반 syscall에 hook 가능하게 함.

### 2016 — XDP

eXpress Data Path. 디바이스 드라이버 단계에서 패킷을 처리. 초고속 패킷 처리(DDoS 방어, 로드밸런싱) 가능.

### 2018 — BTF + CO-RE

BTF (BPF Type Format)와 CO-RE (Compile Once Run Everywhere). 커널 헤더 의존성을 없애고 한 번 컴파일한 BPF가 여러 커널에서 동작 가능.

### 2020+ — 주류화

Cilium, Falco, Pixie, Tetragon 등 eBPF 기반 인프라 도구가 대규모 프로덕션에 채택. Microsoft가 Windows에도 eBPF 포팅.

---

## 왜 eBPF가 중요한가

### 1. 동적 가시성

커널 모듈처럼 위험하지 않으면서 시스템 동작을 관찰. `strace` 보다 100배 빠른 syscall 트레이싱, 모든 파일 IO, 네트워크 연결 추적이 프로덕션에서 가능.

### 2. 안전성

검증기가 다음을 보장:
- 모든 메모리 접근이 유효
- 무한 루프 없음 (또는 bounded loop)
- 모든 코드 경로 종료 보장
- 검증되지 않은 포인터 역참조 불가

### 3. 성능

- JIT 컴파일된 네이티브 코드
- 컨텍스트 스위치 없음 (커널에서 바로 실행)
- 데이터 복사 최소 (BPF maps로 사용자 공간과 공유)

### 4. 안정성

커널 모듈은 잘못 만들면 시스템을 크래시시키지만, eBPF는 검증 통과한 것만 실행되므로 신뢰성이 매우 높습니다.

### 5. 무중단 배포

새 모니터링/네트워킹/보안 정책을 시스템 재부팅 없이 핫 로드.

---

## 동작 모델

### 작성과 로드

```
[1] eBPF 프로그램 작성 (C 또는 Rust)
       ↓
[2] LLVM/Clang으로 BPF 바이트코드 컴파일
       ↓
[3] bpf() syscall로 커널에 로드
       ↓
[4] 검증기(Verifier)가 안전성 검사
       ↓ (PASS)
[5] JIT 컴파일러가 네이티브 코드로
       ↓
[6] hook(probe, tracepoint, XDP 등)에 부착
       ↓
[7] 이벤트 발생 시 실행
       ↓
[8] BPF map으로 결과를 사용자 공간에 전달
```

### 사용자 공간 ↔ 커널

직접 메모리 공유는 안 되고 **BPF map** 이라는 자료구조를 통해서만:

- 사용자 공간이 map에 정책을 쓰면 커널 BPF가 읽어 사용
- 커널 BPF가 map에 통계를 쓰면 사용자 공간이 주기적으로 읽음
- map 종류: hash, array, lru_hash, ringbuf, perf_event_array, ...

### 검증기

가장 중요한 부분. 모든 BPF 프로그램은 검증을 통과해야만 실행됩니다:

1. **CFG 분석**: 모든 분기와 도달 가능성
2. **레지스터 추적**: 각 레지스터의 가능한 값 범위 추적
3. **메모리 접근**: 포인터가 유효한 영역만 읽도록 강제
4. **루프**: bounded만 허용 (5.3+ bounded loops)
5. **종료**: 모든 경로가 종료되는지 (BPF_EXIT)

검증 실패 시 매우 자세한 에러 메시지가 출력됩니다.

---

## 활용 영역

### 관측 (Observability)

- **트레이싱**: kprobe, uprobe, tracepoint로 함수 호출 캡처
- **프로파일링**: perf event 기반 CPU/메모리 프로파일링
- **로깅**: 시스템 호출 감사

대표 도구: bcc, bpftrace, Pixie, Pyroscope, Parca

### 네트워킹

- **XDP**: NIC 드라이버 단계의 초고속 패킷 처리
- **TC (Traffic Control)**: 패킷 분류, 큐잉, QoS
- **socket filter**: 소켓 단위 필터링
- **kube-proxy 대체**: iptables 없이 cgroups + eBPF로 서비스 라우팅

대표 도구: Cilium, Calico, Katran (Facebook L4 LB)

### 보안

- **LSM-BPF**: Linux Security Module hook을 BPF로 구현
- **seccomp BPF**: syscall 필터링
- **runtime threat detection**: 의심스러운 시스템 호출 패턴 감지

대표 도구: Falco, Tetragon, Tracee

### 성능 분석

- **flame graph**: CPU 프로파일링
- **off-CPU 분석**: 프로세스가 대기 중인 이유 추적
- **latency 분포**: 함수별/syscall별 지연 히스토그램

대표 도구: BCC tools, perf + BPF

---

## 도구 생태계

### 저수준

- **libbpf**: 표준 C 라이브러리. 현대 BPF 개발의 기본
- **bpftool**: 커널 BPF 객체 검사·관리 CLI
- **clang/LLVM**: BPF 바이트코드 컴파일러

### 고수준 (스크립트)

- **bpftrace**: DTrace 영감의 원라이너 도구. 빠른 prototyping
- **bcc (BPF Compiler Collection)**: Python 기반. 100+개 ready-made 도구

### 언어 바인딩

- **Go**: cilium/ebpf, libbpfgo
- **Rust**: aya, libbpf-rs
- **C++**: libbpf
- **Python**: BCC (위 참조)

### 인프라 제품

- **Cilium**: 쿠버네티스 CNI + 보안 + observability
- **Tetragon**: 런타임 보안
- **Pixie**: 자동 관측 플랫폼
- **Falco**: 침입 탐지

### Foundation

- **eBPF Foundation**: Linux Foundation 산하. ebpf.io 운영, 표준화 주도

---

## 학습 출발점

추천 순서:

1. **개념 이해**: ebpf.io의 "What is eBPF?" 페이지
2. **bpftrace 원라이너**: 직접 커널을 들여다보는 가장 빠른 길
   ```bash
   sudo bpftrace -e 'tracepoint:syscalls:sys_enter_open { printf("%s %s\n", comm, str(args->filename)); }'
   ```
3. **BCC 도구 사용**: `/usr/share/bcc/tools/` 의 100+개 도구 사용해보기
4. **간단한 BPF 프로그램**: `hello world` 수준의 kprobe + map
5. **libbpf + CO-RE**: 실전 BPF 프로그램 (libbpf-bootstrap 템플릿)
6. **읽기 자료**:
   - "Linux Observability with BPF" (O'Reilly)
   - "BPF Performance Tools" (Brendan Gregg)
   - "Learning eBPF" (Liz Rice)

### 환경 요구사항

- Linux 5.4+ (이상적으로는 5.10+, BTF 활용)
- LLVM/Clang 10+
- libbpf-dev
- 일부 도구는 root 권한 필요 (CAP_BPF, CAP_PERFMON, CAP_SYS_ADMIN)

```bash
# Ubuntu/Debian
sudo apt install bpftrace bpfcc-tools linux-headers-$(uname -r) libbpf-dev clang llvm

# RHEL/Fedora
sudo dnf install bpftrace bcc-tools libbpf-devel clang llvm
```

---

## 참고 자료

- [eBPF.io 공식 사이트](https://ebpf.io/)
- [Kernel BPF docs](https://docs.kernel.org/bpf/)
- [Brendan Gregg's BPF page](https://www.brendangregg.com/ebpf.html)
- [Cilium eBPF guide](https://docs.cilium.io/en/stable/bpf/)
- [Awesome eBPF](https://github.com/zoidbergwill/awesome-ebpf)
