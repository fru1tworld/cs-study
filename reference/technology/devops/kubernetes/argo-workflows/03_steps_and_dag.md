# Steps와 DAG

> 원본: https://argo-workflows.readthedocs.io/en/latest/walk-through/steps/

> 원본: https://argo-workflows.readthedocs.io/en/latest/walk-through/dag/

> 참고: https://argo-workflows.readthedocs.io/en/latest/enhanced-depends-logic/

> 확인일: 2026-09-02

## 공통점

Steps와 DAG는 다른 템플릿을 호출해 여러 작업의 실행 흐름을 구성한다. 입력은 `arguments`로 전달하고, 출력은 호출한 노드의 `outputs`로 연결한다. 두 모델 모두 의존 관계가 없는 작업을 병렬로 실행할 수 있다.

각 Step 또는 Task는 보통 별도의 Pod를 생성한다. 재시도, 조건, 반복, 훅도 이 호출 단위로 설정한다. 두 모델의 차이는 작업 간 실행 순서를 선언하는 방식에 있다.

## Steps 모델

Steps에서 바깥쪽 목록은 위에서 아래로 순차 실행하는 단계 그룹을 나타낸다. 한 그룹 안의 항목들은 동시에 실행되므로, 절차가 선형이고 병렬 구간이 명확한 파이프라인에 적합하다.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: steps-example-
spec:
  entrypoint: main
  templates:
    - name: main
      steps:
        - - name: prepare
            template: echo
            arguments:
              parameters:
                - name: message
                  value: prepare
        - - name: process-a
            template: echo
            arguments:
              parameters:
                - name: message
                  value: process-a
          - name: process-b
            template: echo
            arguments:
              parameters:
                - name: message
                  value: process-b
        - - name: finish
            template: echo
            arguments:
              parameters:
                - name: message
                  value: finish

    - name: echo
      inputs:
        parameters:
          - name: message
      container:
        image: busybox:1.36
        command: [echo]
        args: ["{{inputs.parameters.message}}"]
```

```text
prepare → process-a ─┐
          process-b ─┴→ finish
```

위 예제에서는 `prepare`가 완료되면 `process-a`와 `process-b`가 병렬로 실행된다. 두 Step이 모두 끝나야 다음 단계인 `finish`가 실행된다.

## DAG 모델

DAG는 `tasks` 목록과 Task별 의존 관계로 그래프를 선언한다. 의존성이 없는 Task는 즉시 실행 대상이 되므로, 분기와 합류가 많거나 데이터 의존성으로 실행 순서를 표현할 때 적합하다.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: dag-example-
spec:
  entrypoint: diamond
  templates:
    - name: diamond
      dag:
        tasks:
          - name: A
            template: echo
            arguments:
              parameters: [{name: message, value: A}]
          - name: B
            dependencies: [A]
            template: echo
            arguments:
              parameters: [{name: message, value: B}]
          - name: C
            dependencies: [A]
            template: echo
            arguments:
              parameters: [{name: message, value: C}]
          - name: D
            dependencies: [B, C]
            template: echo
            arguments:
              parameters: [{name: message, value: D}]

    - name: echo
      inputs:
        parameters:
          - name: message
      container:
        image: busybox:1.36
        command: [echo]
        args: ["{{inputs.parameters.message}}"]
```

```text
       ┌→ B ─┐
A ─────┤     ├→ D
       └→ C ─┘
```

## `dependencies`와 `depends`

- `dependencies`
  - 선행 Task 이름의 단순 목록
  - 모든 선행 Task가 완료되어야 현재 Task가 실행됨
- `depends`
  - 상태를 포함한 논리식을 표현함
  - `&&`, `||`, `!`와 결과 상태 조합 가능함
  - `dependencies`와 같은 Task에 동시에 지정하지 않음

```yaml
- name: notify
  depends: "build.Failed || test.Failed"
  template: notify-failure
```

- 주요 결과 상태
  - `Succeeded`: 정상 완료
  - `Failed`: 사용자 코드 실패
  - `Errored`: 실행 인프라 또는 컨트롤러 처리 오류
  - `Skipped`: `when` 조건이 거짓이라 실행하지 않음
  - `Omitted`: `depends` 식이 충족되지 않아 실행하지 않음
  - `Daemoned`: daemon Task가 실행 상태에 도달함
- 상태를 생략한 `A`는 기본적으로 `A.Succeeded || A.Skipped || A.Daemoned`에 해당함

## DAG 실패 처리

- 기본 `failFast: true`
  - Task 하나가 실패하면 새 Task 스케줄링을 중단함
  - 이미 실행 중인 Task는 완료될 수 있음
- `failFast: false`
  - 의존 조건이 허용하는 나머지 Task를 계속 실행함
  - 모든 분기의 결과를 수집해야 할 때 유용함

```yaml
dag:
  failFast: false
  tasks:
    - name: test-a
      template: test-a
    - name: test-b
      template: test-b
```

## 출력 연결

### Steps 출력

```yaml
value: "{{steps.prepare.outputs.parameters.config}}"
```

- `steps.<STEP>.outputs.result`: 표준 출력 기반 결과
- `steps.<STEP>.outputs.parameters.<NAME>`: 명시적 출력 파라미터
- `steps.<STEP>.outputs.artifacts.<NAME>`: 출력 아티팩트

### DAG 출력

```yaml
value: "{{tasks.prepare.outputs.parameters.config}}"
```

- `tasks.<TASK>.outputs.result`: 표준 출력 기반 결과
- `tasks.<TASK>.outputs.parameters.<NAME>`: 명시적 출력 파라미터
- `tasks.<TASK>.outputs.artifacts.<NAME>`: 출력 아티팩트

## 선택 기준

- 순서 중심 설명이 자연스러움 → `steps`
- 의존 관계 중심 설명이 자연스러움 → `dag`
- 병렬 분기, 합류가 많음 → `dag`
- 사람이 읽는 실행 절차와 YAML 구조를 일치시키고 싶음 → `steps`
- 복잡한 Workflow에서는 상위 수준 DAG가 하위 Steps 템플릿을 호출하는 혼합 구성도 가능함

## 설계 시 주의점

Task 수가 많아지면 Pod 생성, API 저장, 상태 갱신 비용도 증가한다. 따라서 매우 짧은 명령을 지나치게 잘게 나누면 오케스트레이션 비용이 실제 작업보다 커질 수 있다. 작업 단위를 정한 뒤에는 출력과 실패 상태가 후속 작업에 어떻게 전달되는지 살펴야 한다.

- 출력이 필요한 Task에는 안정적인 이름을 사용함
- 암시적 실행 순서보다 데이터, 상태 의존 관계를 명시함
- 실패를 무시해야 하는지, 후속 작업이 실패 상태를 소비해야 하는지 구분함
- `continueOn`보다 상태를 표현할 수 있는 DAG `depends` 사용을 우선 검토함
