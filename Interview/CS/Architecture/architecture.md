# Computer Architecture (컴퓨터 구조)

> 카테고리: CS 기초 > 컴퓨터 구조
> [← 면접 질문 목록으로 돌아가기](../../interview.md)

---

## 📌 CPU와 명령어 처리

### ARCH-001

CPU의 구조와 주요 구성 요소(ALU, 제어 유닛, 레지스터)에 대해 설명해주세요.

### ARCH-002

명령어 사이클(Instruction Cycle)의 단계(Fetch, Decode, Execute)에 대해 설명해주세요.

### ARCH-003

파이프라이닝(Pipelining)이 무엇이고, 어떻게 CPU 성능을 향상시키나요?

### ARCH-004

파이프라인 해저드(Pipeline Hazard)의 종류와 해결 방법에 대해 설명해주세요.

### ARCH-005

분기 예측(Branch Prediction)이 무엇이고 왜 중요한가요?

### ARCH-006

Out-of-Order Execution이 무엇이고, 어떤 이점이 있나요?

### ARCH-007

CISC와 RISC 아키텍처의 차이점에 대해 설명해주세요.

### ARCH-008

x86과 ARM 아키텍처의 차이점은 무엇인가요?

---

## 📌 메모리 계층 구조

### ARCH-009

메모리 계층 구조(Memory Hierarchy)에 대해 설명해주세요.

### ARCH-010

레지스터, 캐시, 메인 메모리, 보조 기억장치의 속도와 용량을 비교해주세요.

### ARCH-011

캐시 메모리가 왜 필요하고, 어떤 원리로 동작하나요?

### ARCH-012

캐시의 지역성(Locality) 원리에 대해 설명해주세요.

### ARCH-013

시간 지역성(Temporal Locality)과 공간 지역성(Spatial Locality)의 차이는 무엇인가요?

### ARCH-014

캐시 라인(Cache Line)이 무엇이고, 크기가 성능에 어떤 영향을 미치나요?

### ARCH-015

캐시 히트(Cache Hit)와 캐시 미스(Cache Miss)에 대해 설명해주세요.

### ARCH-016

캐시 매핑 방식(Direct Mapped, Fully Associative, Set Associative)의 차이점을 설명해주세요.

### ARCH-017

캐시 교체 정책(LRU, LFU, FIFO 등)에 대해 설명해주세요.

### ARCH-018

Write-Through와 Write-Back 캐시 쓰기 정책의 차이점은 무엇인가요?

### ARCH-019

L1, L2, L3 캐시의 차이점과 각각의 역할에 대해 설명해주세요.

### ARCH-020

캐시 일관성(Cache Coherence) 문제와 해결 방법에 대해 설명해주세요.

---

## 📌 메모리 관리

### ARCH-021

가상 메모리(Virtual Memory)가 무엇이고 왜 필요한가요?

### ARCH-022

물리 주소(Physical Address)와 논리 주소(Logical Address)의 차이는 무엇인가요?

### ARCH-023

페이지 테이블(Page Table)의 역할과 구조에 대해 설명해주세요.

### ARCH-024

TLB(Translation Lookaside Buffer)가 무엇이고 왜 필요한가요?

### ARCH-025

페이지 폴트(Page Fault)가 발생했을 때 처리 과정을 설명해주세요.

### ARCH-026

메모리 단편화(Fragmentation)의 종류와 해결 방법은 무엇인가요?

### ARCH-027

DMA(Direct Memory Access)가 무엇이고 어떤 장점이 있나요?

### ARCH-028

메모리 버스(Memory Bus)와 대역폭(Bandwidth)에 대해 설명해주세요.

---

## 📌 병렬 처리와 멀티코어

### ARCH-029

멀티코어 프로세서가 무엇이고, 싱글코어와 비교했을 때 어떤 장점이 있나요?

### ARCH-030

하이퍼스레딩(Hyper-Threading)이 무엇인가요?

### ARCH-031

병렬 처리(Parallel Processing)와 동시성(Concurrency)의 차이는 무엇인가요?

### ARCH-032

대칭형 멀티프로세싱(SMP)과 비대칭형 멀티프로세싱(AMP)의 차이는 무엇인가요?

### ARCH-033

멀티코어 환경에서 캐시 일관성을 유지하는 프로토콜(MESI, MOESI 등)에 대해 설명해주세요.

### ARCH-034

False Sharing이 무엇이고, 어떻게 성능에 영향을 미치나요?

### ARCH-035

NUMA(Non-Uniform Memory Access) 아키텍처에 대해 설명해주세요.

---

## 📌 입출력 시스템

### ARCH-036

I/O 처리 방식(Programmed I/O, Interrupt-driven I/O, DMA)의 차이점을 설명해주세요.

### ARCH-037

인터럽트(Interrupt)의 동작 원리와 종류에 대해 설명해주세요.

### ARCH-038

폴링(Polling)과 인터럽트 방식의 장단점을 비교해주세요.

### ARCH-039

I/O 버퍼링(Buffering)이 무엇이고 왜 사용하나요?

### ARCH-040

메모리 맵 I/O(Memory-Mapped I/O)와 포트 맵 I/O(Port-Mapped I/O)의 차이는 무엇인가요?

---

## 📌 성능 측정 및 최적화

### ARCH-041

CPU 성능을 측정하는 지표(Clock Speed, IPC, CPI 등)에 대해 설명해주세요.

### ARCH-042

IPC(Instructions Per Cycle)가 무엇이고 왜 중요한가요?

### ARCH-043

Amdahl의 법칙에 대해 설명하고, 병렬화의 한계를 설명해주세요.

### ARCH-044

벤치마크(Benchmark)의 종류와 성능 측정 방법에 대해 설명해주세요.

### ARCH-045

프로그램 성능 최적화 시 컴퓨터 구조적 관점에서 고려해야 할 사항은 무엇인가요?

---

## 📌 백엔드 개발 관련

### ARCH-046

백엔드 서버에서 CPU 캐시를 효율적으로 활용하는 방법은 무엇인가요?

### ARCH-047

데이터베이스 쿼리 성능에 CPU 캐시가 미치는 영향에 대해 설명해주세요.

### ARCH-048

멀티스레드 환경에서 False Sharing을 방지하는 방법은 무엇인가요?

### ARCH-049

메모리 정렬(Memory Alignment)이 성능에 미치는 영향을 설명해주세요.

### ARCH-050

NUMA 시스템에서 백엔드 애플리케이션 성능을 최적화하는 방법은 무엇인가요?

### ARCH-051

큰 데이터를 처리할 때 메모리 계층을 고려한 최적화 전략은 무엇인가요?

### ARCH-052

CPU 바운드(CPU-bound) 작업과 I/O 바운드(I/O-bound) 작업의 차이와 최적화 방법은 무엇인가요?

### ARCH-053

백엔드 시스템에서 메모리 대역폭이 성능에 미치는 영향은 무엇인가요?

### ARCH-054

컨텍스트 스위칭 비용을 줄이기 위한 하드웨어적/소프트웨어적 방법은 무엇인가요?

### ARCH-055

서버 하드웨어 선택 시 컴퓨터 구조 관점에서 고려해야 할 사항은 무엇인가요?

---

## 📌 최신 기술 동향

### ARCH-056

최근 서버용 CPU의 발전 트렌드(코어 수 증가, 특수 명령어 등)에 대해 설명해주세요.

### ARCH-057

AVX(Advanced Vector Extensions) 명령어가 무엇이고, 어떤 작업에 유용한가요?

### ARCH-058

ARM 서버 CPU(예: AWS Graviton)가 x86 대비 어떤 장점이 있나요?

### ARCH-059

SIMD(Single Instruction Multiple Data)가 무엇이고 어떻게 활용할 수 있나요?

### ARCH-060

컴퓨터 구조 관점에서 클라우드 인프라의 특징과 고려사항은 무엇인가요?

---

 총 질문 수: 60개
