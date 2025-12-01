# Flame Graph

> 성능 프로파일 시각화 도구

## 개요

**Flame Graph**는 Brendan Gregg가 개발한 스택 트레이스 시각화 도구입니다. perf로 수집한 프로파일 데이터를 직관적인 SVG 그래프로 변환하여 성능 병목을 빠르게 파악할 수 있습니다.

---

## Flame Graph 읽는 법

```
┌──────────────────────────────────────────────────────────────────┐
│                          hot_function (45%)                       │
├──────────────────────────────────────┬───────────────────────────┤
│          process_data (30%)          │      helper_func (15%)    │
├──────────────────────┬───────────────┤                           │
│   read_buffer (20%)  │ compute (10%) │                           │
└──────────────────────┴───────────────┴───────────────────────────┘
```

### 구조

- **X축**: 샘플 비율 (알파벳 순 정렬, 시간 순서 아님)
- **Y축**: 스택 깊이 (아래가 root, 위가 leaf)
- **너비**: 해당 함수의 샘플 비율 (넓을수록 CPU 시간 많이 소비)
- **색상**: 함수 유형 구분 (의미 없음, 구분용)

### 해석 방법

1. **넓은 블록**: CPU 시간을 많이 사용하는 함수
2. **평평한 상단**: 리프 함수, 실제 작업 수행
3. **타워 형태**: 깊은 콜 스택
4. **클릭**: 해당 함수 확대

---

## 기본 워크플로우

### 1. FlameGraph 도구 설치

```bash
git clone https://github.com/brendangregg/FlameGraph.git
cd FlameGraph
```

### 2. perf 데이터 수집

```bash
# CPU 프로파일링
perf record -F 99 -g -a sleep 30

# 또는 특정 프로그램
perf record -F 99 -g ./my_program

# DWARF 기반 (권장)
perf record -F 99 -g --call-graph dwarf -a sleep 30
```

### 3. Flame Graph 생성

```bash
# 스택 데이터 추출
perf script > out.perf

# 스택 접기
./FlameGraph/stackcollapse-perf.pl out.perf > out.folded

# SVG 생성
./FlameGraph/flamegraph.pl out.folded > flamegraph.svg
```

### 한 줄 명령

```bash
perf script | ./FlameGraph/stackcollapse-perf.pl | ./FlameGraph/flamegraph.pl > flame.svg
```

---

## 콜 그래프 수집 방식

### Frame Pointer (기본)

```bash
perf record -F 99 -g ./my_program
```

- **요구사항**: `-fno-omit-frame-pointer`로 컴파일
- **장점**: 가장 빠름
- **단점**: 최적화된 바이너리에서 불완전한 스택

### DWARF (권장)

```bash
perf record -F 99 -g --call-graph dwarf ./my_program
```

- **요구사항**: 디버그 정보 (`-g`)
- **장점**: 최적화된 바이너리에서도 동작
- **단점**: 더 큰 데이터 파일

### LBR (Last Branch Record)

```bash
perf record -F 99 -g --call-graph lbr ./my_program
```

- **요구사항**: Intel CPU
- **장점**: 하드웨어 기반, 낮은 오버헤드
- **단점**: 제한된 스택 깊이 (8-32 프레임)

---

## FlameGraph 옵션

### 기본 옵션

```bash
# 제목 설정
./flamegraph.pl --title "My Application CPU Profile" out.folded > flame.svg

# 너비 설정
./flamegraph.pl --width 1200 out.folded > flame.svg

# 높이 설정
./flamegraph.pl --height 16 out.folded > flame.svg

# 색상 팔레트
./flamegraph.pl --colors java out.folded > flame.svg
./flamegraph.pl --colors js out.folded > flame.svg
./flamegraph.pl --colors mem out.folded > flame.svg
./flamegraph.pl --colors io out.folded > flame.svg
```

### 색상 팔레트

| 팔레트 | 용도                   |
| ------ | ---------------------- |
| `hot`  | 기본 (빨강-노랑)       |
| `mem`  | 메모리 프로파일 (녹색) |
| `io`   | I/O 프로파일 (파랑)    |
| `java` | Java 프로파일          |
| `js`   | JavaScript 프로파일    |
| `aqua` | 파랑 계열              |

### 필터링

```bash
# 특정 패턴 하이라이트
./flamegraph.pl --hash --colors=hot out.folded > flame.svg

# 최소 비율 설정
./flamegraph.pl --minwidth 0.5 out.folded > flame.svg
```

---

## Flame Graph 유형

### CPU Flame Graph

가장 일반적인 유형입니다.

```bash
perf record -F 99 -g -a sleep 30
perf script | ./stackcollapse-perf.pl | ./flamegraph.pl > cpu.svg
```

### Off-CPU Flame Graph

CPU를 사용하지 않고 대기하는 시간을 분석합니다.

```bash
# 스케줄러 이벤트 수집
perf record -e sched:sched_switch -a -g sleep 10

# Off-CPU flame graph 생성
perf script | ./stackcollapse-perf.pl | ./flamegraph.pl --colors=io > offcpu.svg
```

### Memory Flame Graph

메모리 할당 패턴을 분석합니다.

```bash
# 페이지 폴트 추적
perf record -e page-faults -a -g sleep 30
perf script | ./stackcollapse-perf.pl | ./flamegraph.pl --colors=mem > mem.svg
```

### Differential Flame Graph

두 프로파일을 비교합니다.

```bash
# 변경 전 프로파일
perf script -i before.data | ./stackcollapse-perf.pl > before.folded

# 변경 후 프로파일
perf script -i after.data | ./stackcollapse-perf.pl > after.folded

# 차이 분석
./difffolded.pl before.folded after.folded | ./flamegraph.pl > diff.svg
```

### Icicle Graph (역방향)

일반 Flame Graph의 역방향입니다.

```bash
./flamegraph.pl --inverted out.folded > icicle.svg
```

---

## 언어별 심볼 해석

### Java

```bash
# perf-map-agent 사용
# JVM 시작 시 -XX:+PreserveFramePointer 옵션 필요

# Java 프로세스 PID 확인
JAVA_PID=$(pgrep java)

# perf-map-agent로 심볼 맵 생성
$JAVA_HOME/bin/java -jar perf-map-agent/attach-main.jar $JAVA_PID

# 프로파일링
perf record -F 99 -g -p $JAVA_PID sleep 30

# Flame Graph 생성
perf script | ./stackcollapse-perf.pl | ./flamegraph.pl > java.svg
```

### Node.js

```bash
# --perf_basic_prof 옵션으로 시작
node --perf_basic_prof app.js &
NODE_PID=$!

# 프로파일링
perf record -F 99 -g -p $NODE_PID sleep 30

# Flame Graph 생성
perf script | ./stackcollapse-perf.pl | ./flamegraph.pl > node.svg
```

### Python

```bash
# Python 3.12+ 에서 perf 지원
python -X perf app.py &
PY_PID=$!

# 프로파일링
perf record -F 99 -g -p $PY_PID sleep 30
```

---

## 내장 Flame Graph (Linux 5.8+)

Linux 5.8 이상에서는 perf에 내장된 Flame Graph를 사용할 수 있습니다.

```bash
# 데이터 수집
perf record -F 99 -g -a sleep 30

# 내장 Flame Graph 생성
perf script report flamegraph
```

### Firefox Profiler 형식

```bash
perf script report gecko
# 결과를 Firefox Profiler (profiler.firefox.com)에 업로드
```

---

## 실용적인 예시

### 웹 서버 프로파일링

```bash
# nginx 프로파일링
sudo perf record -F 99 -g --call-graph dwarf -p $(pgrep nginx) sleep 60
sudo perf script | ./stackcollapse-perf.pl | ./flamegraph.pl > nginx.svg
```

### 데이터베이스 프로파일링

```bash
# PostgreSQL 프로파일링
sudo perf record -F 99 -g --call-graph dwarf -p $(pgrep postgres) sleep 60
sudo perf script | ./stackcollapse-perf.pl | ./flamegraph.pl > postgres.svg
```

### 시스템 전체 프로파일링

```bash
# 시스템 전체 60초
sudo perf record -F 99 -g --call-graph dwarf -a sleep 60
sudo perf script | ./stackcollapse-perf.pl | ./flamegraph.pl --title "System Profile" > system.svg
```

### 커널 프로파일링

```bash
# 커널만 프로파일링
sudo perf record -F 99 -g -a -e cycles:k sleep 30
sudo perf script | ./stackcollapse-perf.pl | ./flamegraph.pl --colors=blue > kernel.svg
```

### 특정 함수 필터링

```bash
# 특정 함수 포함 스택만
perf script | ./stackcollapse-perf.pl | grep "malloc" | ./flamegraph.pl > malloc.svg
```

---

## 트러블슈팅

### 불완전한 스택

```bash
# DWARF 사용
perf record -F 99 -g --call-graph dwarf ./my_program

# 또는 frame pointer 보존 컴파일
gcc -fno-omit-frame-pointer -g program.c -o program
```

### "[unknown]" 심볼

```bash
# 디버그 심볼 설치
sudo apt install libc6-dbg

# 커널 심볼 활성화
sudo sysctl kernel.kptr_restrict=0
```

### 큰 파일 크기

```bash
# 샘플링 주파수 낮추기
perf record -F 49 -g ./my_program

# 짧은 시간 수집
perf record -F 99 -g ./my_program sleep 10
```

### SVG가 비어 있음

```bash
# 데이터 확인
perf report | head

# 스크립트 출력 확인
perf script | head
```

---

## 성능 분석 팁

### 핫스팟 찾기

1. 가장 넓은 블록 확인
2. 평평한 상단 (리프 함수) 분석
3. 반복되는 패턴 확인

### 최적화 대상 선정

1. **넓고 평평한 블록**: 직접 최적화 대상
2. **넓고 깊은 타워**: 알고리즘 변경 고려
3. **많은 작은 블록**: 함수 호출 오버헤드 확인

### 비교 분석

```bash
# 최적화 전후 비교
./difffolded.pl before.folded after.folded | ./flamegraph.pl > diff.svg

# 빨간색 = 증가, 파란색 = 감소
```

---

## 참고 자료

- [Brendan Gregg's Flame Graphs](https://www.brendangregg.com/flamegraphs.html)
- [FlameGraph GitHub](https://github.com/brendangregg/FlameGraph)
- [Netflix Flame Graphs](https://netflixtechblog.com/netflix-flamescope-a57ca19d47bb)
- [perf Wiki](https://perfwiki.github.io/main/)
