# Linux perf 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 Wiki: https://perfwiki.github.io/main/
> 소스 코드: Linux 커널 `/tools/perf`

## 개요

perf는 Linux 커널에 내장된 성능 분석 도구입니다. Performance Counters for Linux (PCL) 또는 perf_events라고도 불립니다. CPU 성능 카운터, tracepoints, kprobes, uprobes를 활용하여 시스템 및 애플리케이션 성능을 분석합니다.

---

## 설치

```bash
# Ubuntu/Debian
sudo apt install linux-tools-common linux-tools-$(uname -r)

# CentOS/RHEL
sudo yum install perf

# Fedora
sudo dnf install perf
```

---

## 문서 목록

### 핵심 명령어

| 문서 | 설명 |
|------|------|
| [perf stat](./perf-stat.md) | 성능 카운터 통계 수집 |
| [perf record](./perf-record.md) | 프로파일 데이터 수집 |
| [perf report](./perf-report.md) | 프로파일 데이터 분석 및 시각화 |
| [perf top](./perf-top.md) | 실시간 시스템 프로파일링 |
| [perf annotate](./perf-annotate.md) | 소스/어셈블리 레벨 분석 |
| [perf list](./perf-list.md) | 사용 가능한 이벤트 목록 |
| [perf script](./perf-script.md) | Raw 이벤트 데이터 출력 |

### 동적 트레이싱

| 문서 | 설명 |
|------|------|
| [perf probe](./perf-probe.md) | 동적 트레이스포인트 생성 (kprobes/uprobes) |
| [perf trace](./perf-trace.md) | 시스템 콜 추적 (strace 대안) |

### 특수 분석 도구

| 문서 | 설명 |
|------|------|
| [perf mem](./perf-mem.md) | 메모리 접근 프로파일링 |
| [perf c2c](./perf-c2c.md) | 캐시 라인 False Sharing 탐지 |
| [perf sched](./perf-sched.md) | CPU 스케줄러 분석 |
| [perf lock](./perf-lock.md) | 커널 락 경합 분석 |
| [perf kmem](./perf-kmem.md) | 커널 메모리 할당 분석 |

### 벤치마크 및 시각화

| 문서 | 설명 |
|------|------|
| [perf bench](./perf-bench.md) | 시스템 성능 벤치마크 |
| [Flame Graph](./flame-graph.md) | 성능 프로파일 시각화 |

---

## 빠른 시작

### CPU 프로파일링

```bash
# 기본 프로파일링
perf record -g ./my_program
perf report

# Flame Graph 생성
perf record -F 99 -g --call-graph dwarf ./my_program
perf script | stackcollapse-perf.pl | flamegraph.pl > flame.svg
```

### 성능 통계 확인

```bash
perf stat ./my_program
perf stat -e cycles,instructions,cache-misses ./my_program
```

### 실시간 모니터링

```bash
sudo perf top
sudo perf top -p $(pgrep my_program)
```

### 시스템 콜 추적

```bash
perf trace ./my_program
perf trace -s ./my_program  # 요약 통계
```

---

## 이벤트 유형

### Hardware Events

| 이벤트 | 설명 |
|--------|------|
| `cycles` | CPU 사이클 |
| `instructions` | 실행된 명령어 |
| `cache-references` | 캐시 참조 |
| `cache-misses` | 캐시 미스 |
| `branch-instructions` | 분기 명령어 |
| `branch-misses` | 분기 예측 실패 |

### Software Events

| 이벤트 | 설명 |
|--------|------|
| `cpu-clock` | CPU 클럭 |
| `task-clock` | 태스크 클럭 |
| `page-faults` | 페이지 폴트 |
| `context-switches` | 컨텍스트 스위치 |
| `cpu-migrations` | CPU 마이그레이션 |

### Tracepoints

```bash
perf list tracepoint    # 전체 목록
perf list 'sched:*'     # 스케줄러 관련
perf list 'syscalls:*'  # 시스템 콜
perf list 'block:*'     # 블록 I/O
```

---

## 권한 설정

```bash
# perf_event_paranoid 레벨
# -1: 제한 없음
#  0: CAP_SYS_ADMIN 없이도 대부분 가능
#  1: 커널 프로파일링에 CAP_SYS_ADMIN 필요
#  2: 모든 프로파일링에 CAP_SYS_ADMIN 필요

# 권한 확인
cat /proc/sys/kernel/perf_event_paranoid

# 권한 설정 (일시적)
sudo sysctl kernel.perf_event_paranoid=-1

# 커널 심볼 접근 허용
sudo sysctl kernel.kptr_restrict=0
```

---

## 분석 시나리오별 가이드

### CPU 병목 분석

```bash
perf record -F 99 -g ./my_program
perf report
```

### 캐시 미스 분석

```bash
perf stat -e cache-references,cache-misses,L1-dcache-load-misses,LLC-load-misses ./my_program
```

### 메모리 접근 패턴 분석

```bash
perf mem record ./my_program
perf mem report
```

### False Sharing 탐지

```bash
perf c2c record -a sleep 5
perf c2c report
```

### 스케줄러 레이턴시 분석

```bash
sudo perf sched record -a sleep 5
perf sched latency
```

### 락 경합 분석

```bash
sudo perf lock contention -ab sleep 5
```

### 시스템 콜 병목 분석

```bash
perf trace -s ./my_program
```

---

## 참고 자료

### 공식 문서

- [perf Wiki](https://perfwiki.github.io/main/)
- [Kernel Source: tools/perf/Documentation](https://github.com/torvalds/linux/tree/master/tools/perf/Documentation)
- [man7.org perf pages](https://man7.org/linux/man-pages/man1/perf.1.html)

### 튜토리얼 및 가이드

- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)
- [Flame Graphs](https://www.brendangregg.com/flamegraphs.html)
- [Red Hat Performance Guide](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/9/html/monitoring_and_managing_system_status_and_performance/)

### 하드웨어 레퍼런스

- [Intel® 64 and IA-32 Architectures Software Developer's Manual](https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html)
- [AMD Processor Programming Reference](https://www.amd.com/en/support/tech-docs)
