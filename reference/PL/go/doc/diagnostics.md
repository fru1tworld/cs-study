# Go 진단 문서

## 소개

Go 생태계는 Go 프로그램의 논리적 및 성능 문제를 진단하기 위한 포괄적인 API 및 도구 세트를 제공합니다. 진단 솔루션은 네 가지 주요 그룹으로 분류됩니다:

- **프로파일링**: 복잡성과 비용 분석(메모리 사용량, 자주 호출되는 함수)
- **추적**: 호출 수명 주기 전반에 걸쳐 지연 시간을 분석하기 위한 코드 계측
- **디버깅**: 실행, 상태, 흐름을 검사하기 위해 프로그램 일시 중지
- **런타임 통계 및 이벤트**: 상태 개요를 위한 런타임 메트릭 수집 및 분석

**중요 참고사항**: 일부 진단 도구는 서로 간섭할 수 있습니다(예: 정밀한 메모리 프로파일링이 CPU 프로파일을 왜곡). 더 정확한 정보를 위해 도구를 격리하여 사용하세요.

---

## 프로파일링

프로파일링은 비용이 많이 들거나 자주 호출되는 코드 섹션을 식별합니다. Go 런타임은 [`runtime/pprof`](/pkg/runtime/pprof/) 패키지를 통해 pprof 시각화 도구 형식으로 프로파일링 데이터를 제공합니다.

### 사전 정의된 프로파일

*   **cpu**: 프로그램이 CPU 사이클을 소비하는 데 시간을 보내는 곳을 표시
*   **heap**: 메모리 할당 샘플 보고; 현재/과거 사용량 및 메모리 누수 모니터링
*   **threadcreate**: 새 OS 스레드 생성으로 이어지는 섹션 보고
*   **goroutine**: 모든 현재 고루틴의 스택 추적 보고
*   **block**: 고루틴이 동기화 기본 요소에서 차단되는 위치 표시(기본적으로 비활성화; `runtime.SetBlockProfileRate` 사용)
*   **mutex**: 잠금 경합 보고(기본적으로 비활성화; `runtime.SetMutexProfileFraction` 사용)

### 추가 프로파일링 도구

**Linux**: [perf 도구](https://perf.wiki.kernel.org/index.php/Tutorial)는 cgo/SWIG 코드 및 커널을 포함하여 Go 프로그램을 프로파일링할 수 있습니다
**macOS**: [Instruments](https://developer.apple.com/library/content/documentation/DeveloperTools/Conceptual/InstrumentsUserGuide/) 스위트

### 프로덕션 프로파일링

**안전한가요?** 예, 하지만 프로파일을 활성화하면 오버헤드가 추가됩니다. 프로덕션 사용 전에 성능 영향을 추정하세요.

**모범 사례**: 주기적으로 무작위 복제본을 프로파일링합니다. 프로세스를 선택하고, Y초마다 X초 동안 프로파일링하고, 분석을 위해 결과를 저장합니다. 주기적으로 반복합니다.

### 시각화 방법

`go tool pprof`는 여러 시각화 형식을 제공합니다:

#### 1. 텍스트 뷰
가장 비용이 많이 드는 호출의 텍스트 출력 목록.

#### 2. 그래프 뷰
가장 비용이 많이 드는 호출을 방향 그래프로 시각화.

#### 3. Weblist 뷰
줄별 비용과 함께 비용이 많이 드는 소스 줄을 HTML 페이지에 표시:
```
예: runtime.concatstrings에서 530ms 소비, 줄별 비용 분석
```

#### 4. 플레임 그래프
특정 코드 섹션을 확대/축소하기 위해 조상 경로를 통해 이동할 수 있습니다. [업스트림 pprof](https://github.com/google/pprof)에서 지원됩니다.

### 사용자 정의 프로파일

[`pprof.Profile`](/pkg/runtime/pprof/#Profile)을 통해 사용자 정의 프로파일을 만들고 기존 도구를 사용하여 검사합니다.

### 사용자 정의 pprof 핸들러

다른 경로와 포트에서 프로파일러 핸들러 제공:

```go
package main

import (
	"log"
	"net/http"
	"net/http/pprof"
)

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/custom_debug_path/profile", pprof.Profile)
	log.Fatal(http.ListenAndServe(":7777", mux))
}
```

---

## 추적

추적은 호출 수명 주기 전반에 걸쳐 지연 시간을 분석하기 위해 코드를 계측합니다. Go는 다음을 제공합니다:

- [`golang.org/x/net/trace`](https://godoc.org/golang.org/x/net/trace): 대시보드가 있는 최소한의 추적 백엔드
- **실행 추적기**: 간격 내의 런타임 이벤트 캡처

### 단일 프로세스 추적 이점

*   애플리케이션 지연 시간 계측 및 분석
*   특정 호출 비용 측정
*   활용도 및 성능 개선 식별

### 분산 추적

시스템이 단일 프로세스를 넘어 성장하면 여러 서비스에 걸친 성능 분석을 위해 분산 추적이 필수적입니다.

**분산 추적 이점:**
*   대규모 시스템에서 애플리케이션 지연 시간 계측 및 프로파일링
*   사용자 요청 수명 주기 내의 모든 RPC 추적
*   프로덕션에서만 보이는 통합 문제 식별
*   추적 데이터 없이는 명확하지 않은 성능 개선 찾기

### FAQ: 추적

**Q: 추적을 생성하기 위해 함수를 자동으로 가로챌 수 있나요?**
A: 아니요. 스팬을 생성, 종료, 주석 달기 위해 코드를 수동으로 계측해야 합니다.

**Q: 추적 헤더는 어떻게 전파해야 하나요?**
A: [`context.Context`](/pkg/context#Context)를 사용하세요. 업계 전반에 정식 추적 키가 존재하지 않습니다; 각 추적 제공자가 전파 유틸리티를 제공합니다.

**Q: 추적에 포함할 수 있는 저수준 이벤트는 무엇인가요?**
A: 표준 라이브러리는 나가는 요청 이벤트에 대해 [`httptrace.ClientTrace`](/pkg/net/http/httptrace#ClientTrace)와 같은 API를 노출합니다. 런타임 실행 추적기 이벤트를 노출하기 위한 지속적인 노력이 있습니다.

---

## 디버깅

디버깅은 프로그램의 오작동을 식별합니다. 두 가지 주요 접근 방식: 디버거 연결과 코어 덤프 디버깅.

### 주요 Go 디버거

**[Delve](https://github.com/derekparker/delve)**
- Go를 위해 특별히 구축된 디버거
- Go 런타임 개념 및 내장 타입 지원
- 완전한 기능과 신뢰성

**[GDB](/doc/gdb)**
- 표준 Go 컴파일러 및 Gccgo를 통해 제공
- 이상적이지 않음; Go의 스택 관리, 스레딩, 런타임이 GDB 기대와 다름
- 디버거를 혼란시킬 수 있음

### 최적화와 함께 디버깅

`gc` 컴파일러는 최적화(함수 인라이닝, 변수 레지스터화)를 수행하여 디버깅을 복잡하게 만듭니다.

**디버깅 시 최적화 비활성화:**
```bash
$ go build -gcflags=all="-N -l"
```

**Go 1.10+: 최적화 및 DWARF 위치 목록으로 빌드:**
```bash
$ go build -gcflags="-dwarflocationlists=true"
```

### 디버거 사용자 인터페이스

Delve와 GDB가 CLI를 제공하지만, 대부분의 편집기 통합 및 IDE는 디버깅 전용 UI를 제공합니다.

### 사후 디버깅

코어 덤프 파일은 실행 중인 프로세스의 메모리 덤프를 포함하며 사후 디버깅 및 프로덕션 서비스 분석에 유용합니다.

**프로세스:**
1. Go 프로그램에서 코어 파일 획득
2. Delve 또는 GDB를 사용하여 디버깅
3. 단계별 가이드는 [코어 덤프 디버깅](/wiki/CoreDumpDebugging) 페이지 참조

---

## 런타임 통계 및 이벤트

런타임은 성능 및 활용도 진단을 위한 통계 및 내부 이벤트 보고를 제공합니다.

### 주요 모니터링 함수

**[`runtime.ReadMemStats`](/pkg/runtime/#ReadMemStats)**
- 힙 할당 및 가비지 컬렉션 메트릭 보고
- 메모리 리소스 소비 모니터링 및 메모리 누수 감지

**[`debug.ReadGCStats`](/pkg/runtime/debug/#ReadGCStats)**
- 가비지 컬렉션에 대한 통계
- GC 일시 중지에 소비된 리소스 표시
- 일시 중지 타임라인 및 백분위수 보고

**[`debug.Stack`](/pkg/runtime/debug/#Stack)**
- 현재 스택 추적 반환
- 실행 중인 고루틴, 상태, 차단 표시

**[`debug.WriteHeapDump`](/pkg/runtime/debug/#WriteHeapDump)**
- 모든 고루틴 일시 중지
- 힙을 파일로 덤프(메모리 스냅샷)
- 할당된 객체, 고루틴, 파이널라이저 포함

**[`runtime.NumGoroutine`](/pkg/runtime#NumGoroutine)**
- 현재 고루틴 수 반환
- 활용도 부족 또는 고루틴 누수 감지

### 실행 추적기

스케줄링, 시스템 호출, 가비지 컬렉션, 힙 크기 등 광범위한 런타임 이벤트를 캡처합니다.

**사용 사례:**
*   고루틴 실행 이해
*   핵심 런타임 이벤트(GC 실행) 분석
*   잘못 병렬화된 실행 식별

**적합하지 않은 경우:**
*   핫스팟 식별
*   과도한 메모리/CPU 사용량 분석(대신 프로파일링 사용)

**시각화 예제:** `go tool trace`는 실행이 직렬화되는 것을 보여주며, 잠금 경합 병목 현상을 암시합니다.

**사용법:** 수집 및 분석에 대해서는 [`go tool trace`](/cmd/trace/)를 참조하세요.

### GODEBUG 환경 변수

[GODEBUG](/pkg/runtime/#hdr-Environment_Variables)가 설정되면 런타임이 이벤트를 내보냅니다.

**가비지 컬렉션 및 초기화:**
```
GODEBUG=gctrace=1       # 각 컬렉션에서 GC 이벤트
GODEBUG=inittrace=1     # 패키지 초기화 요약
GODEBUG=schedtrace=X    # X 밀리초마다 스케줄링 이벤트
```

**CPU 명령어 세트 확장:**
```
GODEBUG=cpu.all=off           # 모든 선택적 확장 비활성화
GODEBUG=cpu._extension_=off   # 특정 확장 비활성화(예: sse41, avx)
```

---

## 요약

Go의 포괄적인 진단 툴킷은 다음을 위한 전문 도구를 제공합니다:

| 필요 | 도구 |
|------|------|
| 성능 병목 현상 | 프로파일링 (pprof) |
| 지연 시간 분석 | 실행 추적기, 분산 추적 |
| 프로그램 상태 및 실행 | Delve, GDB |
| 상태 개요 | 런타임 통계, GODEBUG |

특정 문제에 따라 도구를 선택하고, 정확한 결과를 위해 격리하여 사용하세요.
