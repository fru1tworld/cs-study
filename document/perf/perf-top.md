# perf top

> 실시간 시스템 프로파일링 도구

## 개요

`perf top`은 `top` 명령어와 유사하게 실시간으로 시스템 성능을 모니터링합니다. 현재 CPU를 가장 많이 사용하는 함수들을 실시간으로 표시합니다.

---

## 기본 사용법

```bash
# 시스템 전체 모니터링 (root 필요)
sudo perf top

# 특정 프로세스 모니터링
sudo perf top -p <pid>

# 콜 그래프 포함
sudo perf top -g

# 특정 CPU만 모니터링
sudo perf top -C 0,1,2
```

---

## 주요 옵션

### 대상 지정

| 옵션 | 설명 |
|------|------|
| `-a` | 모든 CPU (기본값) |
| `-C <cpu>` | 특정 CPU만 모니터링 |
| `-p <pid>` | 특정 프로세스 |
| `-t <tid>` | 특정 스레드 |
| `-u <user>` | 특정 사용자 |
| `--cgroup <name>` | 특정 cgroup |

### 이벤트 및 샘플링

| 옵션 | 설명 |
|------|------|
| `-e <event>` | 모니터링할 이벤트 |
| `-F <freq>` | 샘플링 주파수 (Hz) |
| `-c <count>` | 이벤트 카운트 기반 샘플링 |

### 표시 옵션

| 옵션 | 설명 |
|------|------|
| `-g` | 콜 그래프 표시 |
| `-n` | 샘플 수 표시 |
| `-s <field>` | 정렬 키 지정 |
| `--sort <keys>` | 복합 정렬 |
| `-d <sec>` | 화면 갱신 주기 (초) |
| `--stdio` | stdio 모드 (비대화식) |

### 심볼 관련

| 옵션 | 설명 |
|------|------|
| `-k <vmlinux>` | 커널 심볼 파일 |
| `--kallsyms <file>` | kallsyms 파일 |
| `-K` | 커널 심볼 숨기기 |
| `-U` | 유저 심볼 숨기기 |

---

## 인터랙티브 키보드 단축키

### 기본 탐색

| 키 | 설명 |
|----|------|
| `↑/↓` | 항목 이동 |
| `Enter` | 선택/확장 |
| `q` | 종료 |
| `h` | 도움말 |

### 분석

| 키 | 설명 |
|----|------|
| `a` | annotate (어셈블리 레벨 분석) |
| `s` | 특정 함수 심층 분석 |
| `d` | DSO 필터 |
| `t` | 스레드 보기 토글 |

### 표시 설정

| 키 | 설명 |
|----|------|
| `e` | 표시 이벤트 변경 |
| `E` | 확장 모드 토글 |
| `+/-` | 콜 그래프 확장/축소 |
| `S` | 심볼 필터 |
| `K` | 커널 심볼 토글 |
| `U` | 유저 심볼 토글 |

### 정렬

| 키 | 설명 |
|----|------|
| `P` | 부모 기준 정렬 |
| `f` | 정렬 필드 선택 |

---

## 출력 해석

```
Samples: 45K of event 'cycles', Event count (approx.): 28191295233
Overhead  Shared Object       Symbol
  12.34%  [kernel]            [k] _raw_spin_lock_irqsave
   8.56%  libc-2.31.so        [.] __memcpy_avx_unaligned_erms
   7.23%  my_program          [.] hot_function
   5.67%  [kernel]            [k] copy_user_generic_unrolled
```

### 컬럼 설명

| 컬럼 | 설명 |
|------|------|
| **Samples** | 총 샘플 수 |
| **Event count** | 추정 이벤트 총 수 |
| **Overhead** | CPU 사용 비율 |
| **Shared Object** | 라이브러리/바이너리 |
| **Symbol** | 함수 이름 |

---

## 실용적인 예시

### 기본 시스템 모니터링

```bash
# 기본 모니터링
sudo perf top

# 낮은 주파수로 오버헤드 줄이기
sudo perf top -F 49

# 2초마다 갱신
sudo perf top -d 2
```

### 특정 프로세스 분석

```bash
# PID로 프로세스 모니터링
sudo perf top -p $(pgrep nginx)

# 프로세스 이름과 라이브러리 표시
sudo perf top -p <pid> -ns comm,dso
```

### 콜 그래프 분석

```bash
# 콜 그래프 포함
sudo perf top -g

# DWARF 기반 콜 그래프
sudo perf top --call-graph dwarf
```

### 특정 이벤트 모니터링

```bash
# 캐시 미스 모니터링
sudo perf top -e cache-misses

# 브랜치 미스 모니터링
sudo perf top -e branch-misses

# 페이지 폴트 모니터링
sudo perf top -e page-faults
```

### 시스템 콜 분석

```bash
# 시스템 콜 진입점
sudo perf top -e raw_syscalls:sys_enter -ns comm

# 네트워크 전송
sudo perf top -e net:net_dev_xmit -ns comm
```

### 정렬 변경

```bash
# DSO별 정렬
sudo perf top -s dso

# 프로세스별 정렬
sudo perf top -s comm

# 복합 정렬
sudo perf top --sort comm,dso
```

### 커널 분석

```bash
# 커널만 분석
sudo perf top -K

# 커널 심볼 파일 지정
sudo perf top -k /boot/vmlinux-$(uname -r)
```

### 유저 스페이스만

```bash
# 유저 스페이스만 표시
sudo perf top -U
```

---

## stdio 모드

비대화식 출력이 필요할 때 사용합니다.

```bash
# stdio 모드
sudo perf top --stdio

# 파이프 처리
sudo perf top --stdio | head -20
```

---

## 컨테이너 모니터링

```bash
# 특정 cgroup
sudo perf top --cgroup=docker/<container_id>
```

---

## 성능 튜닝

### 오버헤드 줄이기

```bash
# 낮은 샘플링 주파수
sudo perf top -F 49

# 긴 갱신 주기
sudo perf top -d 5
```

### 정밀도 높이기

```bash
# 높은 샘플링 주파수
sudo perf top -F 997

# 빠른 갱신
sudo perf top -d 0.5
```

---

## 트러블슈팅

### 권한 오류

```bash
# perf_event_paranoid 설정
sudo sysctl kernel.perf_event_paranoid=-1

# 또는 root로 실행
sudo perf top
```

### 심볼 없음

```bash
# 커널 심볼 활성화
sudo sysctl kernel.kptr_restrict=0

# vmlinux 파일 지정
sudo perf top -k /boot/vmlinux-$(uname -r)
```

### "no symbols found" 오류

```bash
# 디버그 패키지 설치
sudo apt install linux-tools-$(uname -r)
sudo apt install libc6-dbg
```

---

## perf top vs perf record + report

| 특성 | perf top | perf record + report |
|------|----------|---------------------|
| 실시간 | O | X |
| 데이터 저장 | X | O |
| 상세 분석 | 제한적 | 상세 |
| 사용 사례 | 빠른 확인 | 심층 분석 |

---

## 참고 자료

- [perf-top(1) man page](https://man7.org/linux/man-pages/man1/perf-top.1.html)
- [perf Wiki - Tutorial](https://perfwiki.github.io/main/tutorial/)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)
