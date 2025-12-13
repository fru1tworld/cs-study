# perf script

> Raw 이벤트 데이터 출력 도구

## 개요

`perf script`는 `perf record`로 수집한 데이터를 사람이 읽을 수 있는 형식이나 다른 도구에서 처리할 수 있는 형식으로 출력합니다. Flame Graph 생성, 커스텀 분석 스크립트 등에 사용됩니다.

---

## 기본 사용법

```bash
# 기본 출력
perf script

# 헤더 포함
perf script --header

# 특정 파일에서 읽기
perf script -i perf.data

# 특정 필드만 출력
perf script -F comm,pid,tid,cpu,time,event,ip,sym,dso
```

---

## 주요 옵션

### 입출력

| 옵션 | 설명 |
|------|------|
| `-i <file>` | 입력 파일 지정 (기본: perf.data) |
| `-o <file>` | 출력 파일 지정 |
| `--header` | 파일 헤더 정보 포함 |
| `-v` | 상세 출력 |

### 필드 선택

| 옵션 | 설명 |
|------|------|
| `-F <fields>` | 출력 필드 지정 |
| `-f <fields>` | `-F`와 동일 (구버전) |
| `--fields <fields>` | `-F`와 동일 |

### 필터링

| 옵션 | 설명 |
|------|------|
| `-c <comm>` | 특정 명령어만 |
| `-p <pid>` | 특정 PID만 |
| `-t <tid>` | 특정 TID만 |
| `-C <cpu>` | 특정 CPU만 |
| `--time <range>` | 시간 범위 필터 |

### 심볼

| 옵션 | 설명 |
|------|------|
| `-k <vmlinux>` | 커널 심볼 파일 |
| `--kallsyms <file>` | kallsyms 파일 |
| `--symfs <dir>` | 심볼 파일 디렉토리 |

---

## 출력 필드 (-F)

### 사용 가능한 필드

| 필드 | 설명 |
|------|------|
| `comm` | 명령어/프로세스 이름 |
| `pid` | 프로세스 ID |
| `tid` | 스레드 ID |
| `cpu` | CPU 번호 |
| `time` | 타임스탬프 |
| `event` | 이벤트 이름 |
| `trace` | 트레이스 데이터 |
| `ip` | 명령어 포인터 (주소) |
| `sym` | 심볼 (함수 이름) |
| `dso` | DSO (라이브러리/바이너리) |
| `addr` | 대상 주소 |
| `symoff` | 심볼 오프셋 |
| `srcline` | 소스 라인 |
| `period` | 샘플 기간 |
| `iregs` | 인터럽트 레지스터 |
| `uregs` | 유저 레지스터 |
| `brstack` | 브랜치 스택 |
| `brstacksym` | 브랜치 스택 (심볼) |
| `flags` | 샘플 플래그 |
| `bpf-output` | BPF 출력 |
| `callindent` | 콜 들여쓰기 |
| `insn` | 명령어 |
| `insnlen` | 명령어 길이 |
| `synth` | 합성 이벤트 데이터 |
| `phys_addr` | 물리 주소 |
| `data_page_size` | 데이터 페이지 크기 |
| `code_page_size` | 코드 페이지 크기 |
| `weight` | 샘플 가중치 |
| `bpf-output` | BPF 출력 |
| `ins_lat` | 명령어 레이턴시 |

### 필드 사용 예시

```bash
# 기본 필드
perf script -F comm,pid,tid,cpu,time,event,ip,sym,dso

# 시간과 심볼만
perf script -F time,sym

# 트레이스 데이터 포함
perf script -F time,event,trace

# 소스 라인 포함
perf script -F comm,pid,cpu,time,sym,srcline
```

---

## 출력 형식

### 기본 출력 형식

```
 my_program  1234 [000]  123.456789:     cycles:  ffffffffa1234567 hot_function+0x17 (/usr/bin/my_program)
```

### 필드 설명

```
명령어  PID [CPU]  타임스탬프:  이벤트:  주소 심볼+오프셋 (DSO)
```

### 콜 스택 출력

```bash
perf script --call-graph
```

출력:
```
my_program  1234 [000]  123.456789:     cycles:
            ffffffffa1234567 hot_function+0x17 (/usr/bin/my_program)
            ffffffffa1234890 caller_function+0x23 (/usr/bin/my_program)
            ffffffffa1234abc main+0x45 (/usr/bin/my_program)
```

---

## 내장 스크립트

### Flame Graph 생성

```bash
# Flame Graph 생성 (Linux 4.11+)
perf script report flamegraph

# Firefox Profiler 형식
perf script report gecko
```

### 스크립트 목록 확인

```bash
# 사용 가능한 스크립트 목록
perf script -l

# 스크립트 도움말
perf script -s <script_name> -- -h
```

---

## Flame Graph 워크플로우

### Brendan Gregg's FlameGraph 도구 사용

```bash
# 1. 프로파일 수집
perf record -F 99 -g --call-graph dwarf -a sleep 30

# 2. FlameGraph 도구 클론
git clone https://github.com/brendangregg/FlameGraph.git

# 3. 스택 데이터 추출
perf script > out.perf

# 4. 스택 접기
./FlameGraph/stackcollapse-perf.pl out.perf > out.folded

# 5. Flame Graph 생성
./FlameGraph/flamegraph.pl out.folded > flamegraph.svg
```

### 한 줄 명령

```bash
perf script | ./FlameGraph/stackcollapse-perf.pl | ./FlameGraph/flamegraph.pl > flame.svg
```

---

## 실용적인 예시

### 기본 이벤트 추적

```bash
# 모든 샘플 출력
perf script

# 상위 100개만
perf script | head -100
```

### 특정 프로세스 필터

```bash
# PID로 필터
perf script -p 1234

# 명령어 이름으로 필터
perf script -c nginx
```

### 시간 범위 분석

```bash
# 특정 시간 범위
perf script --time 10,20

# 처음 10초
perf script --time ,10
```

### 시스템 콜 추적

```bash
# 시스템 콜 기록
perf record -e 'syscalls:sys_enter_*' -a sleep 5

# 시스템 콜 출력
perf script
```

### 컨텍스트 스위치 분석

```bash
# 컨텍스트 스위치 기록
perf record -e context-switches -ag sleep 10

# 출력
perf script -F comm,pid,tid,cpu,time,event
```

### 블록 I/O 추적

```bash
# 블록 I/O 기록
perf record -e 'block:*' -a sleep 10

# 출력
perf script -F time,event,trace
```

---

## 커스텀 분석

### awk를 사용한 분석

```bash
# 함수별 샘플 수 카운트
perf script | awk '{print $5}' | sort | uniq -c | sort -rn | head

# 프로세스별 샘플 수
perf script -F comm | sort | uniq -c | sort -rn
```

### Python 스크립트 사용

```bash
# Python 스크립트로 분석
perf script -s my_analysis.py
```

Python 스크립트 예시:
```python
# my_analysis.py
def process_event(event):
    print(f"{event['comm']} {event['pid']} {event['sym']}")
```

### Perl 스크립트 사용

```bash
# Perl 스크립트로 분석
perf script -s my_analysis.pl
```

---

## 고급 옵션

### Raw 이벤트 덤프

```bash
# 16진수 덤프
perf script -D

# Raw 샘플 덤프
perf script --dump-raw-samples
```

### 브랜치 스택

```bash
# 브랜치 기록 (record 시)
perf record -b ./my_program

# 브랜치 스택 출력
perf script -F ip,brstack
perf script -F ip,brstacksym
```

### 명령어 출력

```bash
# 명령어 포함
perf script -F ip,insn,insnlen
```

### 레지스터 출력

```bash
# 레지스터 기록 (record 시)
perf record --intr-regs=ax,bx,cx ./my_program

# 레지스터 출력
perf script -F iregs
```

---

## 특수 리포트 생성

### 네트워크 분석

```bash
perf record -e 'net:*' -a sleep 10
perf script -F time,event,trace
```

### 스케줄러 분석

```bash
perf record -e 'sched:*' -a sleep 10
perf script -F time,event,trace
```

### 메모리 분석

```bash
perf record -e 'kmem:*' -a sleep 10
perf script -F time,event,trace
```

---

## 트러블슈팅

### 심볼 없음

```bash
# 심볼 파일 경로 지정
perf script --symfs /path/to/symbols

# 커널 심볼 활성화
sudo sysctl kernel.kptr_restrict=0
```

### 빈 출력

```bash
# 데이터 확인
perf report --stdio | head

# 이벤트 확인
perf script --header | grep "event"
```

### 잘린 출력

```bash
# 전체 심볼 이름 표시
perf script --show-all
```

---

## 참고 자료

- [perf-script(1) man page](https://man7.org/linux/man-pages/man1/perf-script.1.html)
- [FlameGraph](https://github.com/brendangregg/FlameGraph)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)
