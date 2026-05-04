# Flame Graph 분석

> 이 문서는 Flame Graph 의 읽는 법과 Pyroscope 의 다양한 시각화 모드(Diff, Sandwich, Table, Tree)를 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/view-and-analyze-profile-data/

---

## 목차

1. [Flame Graph란](#flame-graph란)
2. [Flame Graph 읽는 법](#flame-graph-읽는-법)
3. [표현 방향: Flame vs Icicle](#표현-방향-flame-vs-icicle)
4. [Diff 뷰 (비교)](#diff-뷰-비교)
5. [Sandwich 뷰](#sandwich-뷰)
6. [Table / Tree / Top 뷰](#table--tree--top-뷰)
7. [실전 분석 워크플로](#실전-분석-워크플로)

---

## Flame Graph란

**Flame Graph**는 Brendan Gregg이 2011년에 제안한 시각화 기법으로, 프로파일에 포함된 **수많은 콜 스택의 집합** 을 한눈에 보여줍니다.

```
[━━━━━━━━━━━━━━━━━━ root ━━━━━━━━━━━━━━━━━━]
[━━━━━━ funcA ━━━━━━][━━━━━━ funcB ━━━━━━]
[━━ a1 ━━][━━ a2 ━━][━━ b1 ━━][━━ b2 ━━]
[a1.x][a1.y]      [b1.x]    [b2.x]
```

- **너비(가로)**: 해당 함수가 차지한 비율 (CPU 시간, 메모리 등 sample value의 합)
- **높이(세로)**: 콜 스택 깊이
- **색깔**: 정보 없음 (대비를 위한 시각적 구분만)

너비가 넓은 함수가 **비싼 함수** 입니다.

---

## Flame Graph 읽는 법

### 가장 먼저 보아야 할 곳

1. **루트 바로 아래 가장 넓은 박스**
   - 프로그램 전체 시간/메모리에서 가장 큰 비중을 차지하는 함수
2. **꼭대기의 평평한 영역**
   - 실제로 일이 일어나는 곳 (leaf function). 자식이 없는 박스
3. **수직으로 깊은 스택**
   - 깊다고 무조건 나쁜 것은 아니지만, 프레임워크 오버헤드가 큰 신호일 수 있음

### Self vs Total

- **Total**: 자식까지 포함한 총 비용 (박스 너비)
- **Self**: 자신이 직접 사용한 비용 (자식이 차지하지 않은 부분)
- "Total은 크지만 Self는 작은" 함수 = 분배자(dispatcher), 진짜 비용은 자식
- "Self가 큰 함수" = 실제로 일하고 있는 함수

### 줌(Zoom)

박스를 클릭하면 그 박스 기준으로 100%로 확대됩니다. 깊은 스택을 탐색할 때 유용합니다.

### 검색(Search)

함수명/파일명으로 검색하면 매칭되는 박스가 강조됩니다. 특정 모듈의 비용 비중을 빠르게 파악할 수 있습니다.

---

## 표현 방향: Flame vs Icicle

| 모드 | 방향 | 특징 |
|------|------|------|
| **Flame** | 위로 자라는 형태 (root 위) | 전통적인 표현, 잎(leaf)이 위 |
| **Icicle** | 아래로 자라는 형태 (root 아래) | 가독성 ↑, 깊은 스택에 유리 |

Pyroscope UI는 두 모드 모두 지원하며, Icicle이 기본인 경우가 많습니다.

---

## Diff 뷰 (비교)

**두 프로파일을 빨강/파랑으로 비교** 하여 어디서 비용이 늘었고 줄었는지 보여줍니다.

### 사용 시나리오

- **배포 전후 비교**: 어제(Baseline) vs 오늘(Comparison)
- **A/B 테스트**: 실험군 vs 대조군 라벨 비교
- **인스턴스 비교**: 빠른 노드 vs 느린 노드

### 색깔 의미

- **빨강(Red)**: Comparison에서 **증가** 한 부분 (회귀 후보)
- **파랑(Blue)**: Comparison에서 **감소** 한 부분 (개선)
- **회색**: 변화 없음

### 권장 사용법

1. 베이스라인 시간/라벨 선택
2. 비교 대상 시간/라벨 선택
3. **빨강 박스** 부터 살펴봄 → 회귀 위치 식별
4. 자세히 보고 싶으면 해당 박스로 줌

### Pyroscope 쿼리 예

```
# 베이스라인
service_name="checkout", deployment="v1.0"

# 비교
service_name="checkout", deployment="v1.1"
```

---

## Sandwich 뷰

특정 함수에 집중하여 **호출자(callers)** 와 **피호출자(callees)** 를 한 화면에 보여줍니다.

```
┌──────── 호출자(Callers) - Reverse ────────┐
│  who calls this function                   │
└────────────────────────────────────────────┘
              ▼
       [SELECTED FUNCTION]
              ▼
┌─────── 피호출자(Callees) - Forward ────────┐
│  what this function calls                  │
└────────────────────────────────────────────┘
```

### 언제 유용한가

- "이 함수는 누가 그렇게 자주 호출하지?" — 호출자 추적
- "이 함수 안에서 뭐가 비싼 거지?" — 내부 비용 분석
- 라이브러리 함수의 진짜 비용 추적 (예: `json.Unmarshal` 의 호출 컨텍스트별 비중)

### 사용법

1. Flame Graph 또는 Table에서 함수 우클릭 → "Sandwich view"
2. 위쪽: 그 함수에 도달하는 모든 경로
3. 아래쪽: 그 함수가 부르는 모든 경로

---

## Table / Tree / Top 뷰

### Table 뷰

행이 함수, 열이 Self/Total 의 정렬 가능한 표.

| 함수 | Self | Total | Self % | Total % |
|------|------|-------|--------|---------|
| `regexp.compile` | 4.2s | 4.2s | 22% | 22% |
| `json.Unmarshal` | 1.1s | 3.8s | 6% | 19% |
| `runtime.mallocgc` | 2.3s | 2.3s | 12% | 12% |

- **Self 정렬**: 진짜 비싼 leaf 함수 발견
- **Total 정렬**: 큰 그림(전체 비용 분배) 파악

### Tree 뷰

콜 스택을 트리(외곽선) 형태로 표현. 각 노드 옆에 비중이 표시되어 깊이별 분포를 따라가기 좋습니다.

### Top 뷰

가장 비싼 함수 상위 N개 리스트. 빠른 핫스팟 식별에 유용합니다.

---

## 실전 분석 워크플로

### 1단계: 큰 그림

- Flame Graph 전체에서 가장 넓은 1~3개 박스를 식별
- "이게 정상인가? 충분히 최적화된 라이브러리인가?" 자문

### 2단계: 의심 후보 검증

- 의심 함수에 줌 인
- Self/Total 비교 → 진짜 비용 위치 파악
- Sandwich 뷰로 호출 컨텍스트 확인

### 3단계: 회귀라면 Diff

- 이전 시간/배포와 Diff
- 빨강 박스 → 회귀 책임 함수
- 코드 차이와 매칭

### 4단계: 가설 검증

- 코드 수정 → 배포 → 동일 라벨로 다시 프로파일
- 다시 Diff: 빨강이 사라졌는지 확인

### 흔한 함정

- **인라인된 함수**: 컴파일 최적화로 호출 스택에서 사라질 수 있음 → 부모 함수에 비용이 합쳐 보임
- **GC 비용**: 직접 할당이 적은 코드에서도 `runtime.mallocgc` 가 크게 나타날 수 있음
- **시스템 콜**: `syscall.read` 등 OS 호출은 wall-clock에서만 큰 비중을 차지하는 경우가 많음

---

## 다음 단계

- [04_profile_types.md](./04_profile_types.md) - 프로파일 종류
- [08_use_cases.md](./08_use_cases.md) - 실제 분석 사례
- [12_visualization.md](./12_visualization.md) - Grafana Explore Profiles 통합
