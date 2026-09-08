# 파라미터, 변수와 제어 흐름

> 원본: https://argo-workflows.readthedocs.io/en/latest/walk-through/parameters/

> 참고: https://argo-workflows.readthedocs.io/en/latest/variables/

> 참고: https://argo-workflows.readthedocs.io/en/latest/walk-through/loops/

> 참고: https://argo-workflows.readthedocs.io/en/latest/walk-through/conditionals/

> 확인일: 2026-09-02

## 입력 파라미터의 흐름

```text
Workflow spec.arguments
  → entrypoint의 inputs
  → Step 또는 Task의 arguments
  → 호출된 template의 inputs
```

호출자가 `arguments.parameters`로 값을 전달하면, 호출되는 템플릿은 `inputs.parameters`에 선언한 이름으로 이를 받는다. 컨테이너에서는 `{{inputs.parameters.<NAME>}}`로 해당 값을 참조한다. Workflow 최상위 인수는 `{{workflow.parameters.<NAME>}}`로 어디서든 참조할 수 있다.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: parameter-example-
spec:
  entrypoint: print-message
  arguments:
    parameters:
      - name: message
        value: hello
  templates:
    - name: print-message
      inputs:
        parameters:
          - name: message
            default: fallback
      container:
        image: busybox:1.36
        command: [echo]
        args: ["{{inputs.parameters.message}}"]
```

이 예제의 `spec.arguments`는 entrypoint인 `print-message`에 `hello`를 전달한다. 호출자가 값을 주지 않으면 템플릿의 `default` 값인 `fallback`을 사용한다. 제출 시 최상위 값을 바꾸려면 `argo submit workflow.yaml -p message=goodbye`를 실행하고, 여러 값을 전달하려면 `--parameter-file params.yaml`을 사용한다.

## 출력 파라미터

### 파일에서 출력하기

```yaml
- name: produce
  container:
    image: busybox:1.36
    command: [sh, -c]
    args: ["echo -n 42 > /tmp/value"]
  outputs:
    parameters:
      - name: answer
        valueFrom:
          path: /tmp/value
```

이렇게 파일 내용을 출력 파라미터에 연결하면 작은 문자열 값을 다음 단계에 전달할 수 있다. 후속 Steps에서는 `{{steps.produce.outputs.parameters.answer}}`로, DAG Task에서는 `{{tasks.produce.outputs.parameters.answer}}`로 참조한다.

### `result` 사용하기

- `script`, `container`, `http` 템플릿의 결과를 `outputs.result`로 제공함
- `script`와 `container`에서는 표준 출력의 일부를 캡처함
- 큰 데이터, 바이너리, 파일 묶음은 파라미터가 아니라 아티팩트 사용 권장

## 변수 표현식

### 단순 태그

```yaml
args: ["{{workflow.name}}", "{{inputs.parameters.message}}"]
```

- `{{ ... }}` 형태로 값을 문자열에 치환함
- 태그 안의 공백이 보간 실패를 일으킬 수 있어 공백 없이 작성 권장
- YAML 파서가 중괄호를 객체로 해석하지 않도록 전체 값을 따옴표로 감쌈

### 표현식 태그

```yaml
value: "{{=sprig.upper(inputs.parameters.message)}}"
```

- `{{= ... }}` 형태로 expr 표현식을 평가함
- 산술, 비교, 목록 처리와 일부 Sprig 함수를 사용할 수 있음
- 하이픈이 든 이름은 `inputs.parameters['log-level']`처럼 인덱싱함
- 입력값이 따옴표를 포함할 수 있는 조건에서는 표현식 태그 사용이 안전함

## 자주 쓰는 변수 범위

- Workflow
  - `workflow.name`, `workflow.namespace`, `workflow.uid`
  - `workflow.status`, `workflow.duration`
  - `workflow.parameters.<NAME>`
- 템플릿
  - `inputs.parameters.<NAME>`
  - `inputs.artifacts.<NAME>.path`
  - `outputs.parameters.<NAME>`
- Steps
  - `steps.<STEP>.status`
  - `steps.<STEP>.outputs.result`
- DAG
  - `tasks.<TASK>.status`
  - `tasks.<TASK>.outputs.result`
- 반복
  - `item`
  - `item.<KEY>`
- 재시도
  - `lastRetry.exitCode`, `lastRetry.status`, `lastRetry.duration`, `lastRetry.message`

## 반복 실행

### `withSequence`

```yaml
- name: numbered
  template: echo
  arguments:
    parameters:
      - name: message
        value: "{{item}}"
  withSequence:
    start: "1"
    end: "5"
```

- 연속된 숫자 범위를 생성함
- `count` 또는 `start`, `end` 조합 사용 가능함
- 형식 지정이 필요하면 `format`을 설정함

### `withItems`

```yaml
- name: deploy
  template: deploy-one
  arguments:
    parameters:
      - name: service
        value: "{{item.name}}"
      - name: region
        value: "{{item.region}}"
  withItems:
    - {name: api, region: seoul}
    - {name: worker, region: tokyo}
```

- YAML에 정적으로 작성한 값이나 객체 목록을 순회함
- 단순 값은 `{{item}}`, 객체 필드는 `{{item.<KEY>}}`로 참조함

### `withParam`

```yaml
- name: process
  template: process-one
  arguments:
    parameters:
      - name: value
        value: "{{item}}"
  withParam: "{{steps.generate.outputs.result}}"
```

`withParam`은 JSON 배열 문자열을 동적으로 순회하므로, 앞 단계가 만든 결과로 fan-out을 구성할 때 유용하다. 입력으로 넘기는 값이 유효한 JSON 배열인지 확인해야 한다.

## 조건부 실행

```yaml
- name: deploy
  template: deploy
  when: "{{steps.test.outputs.result}} == passed"
```

`when` 식이 참일 때만 Step 또는 Task가 실행되며, 거짓이면 노드 상태는 `Skipped`가 된다. 조건에는 `==`, `!=`, `>`, `<`, `&&`, `||`와 정규식 연산자를 사용할 수 있다.

```yaml
when: "{{=inputs.parameters['may-contain-quotes'] == 'expected'}}"
```

입력 문자열에 따옴표가 들어가면 템플릿 치환 후 조건식의 형태가 달라질 수 있다. 이런 입력은 위처럼 expr 표현식 태그로 비교하는 것이 좋다.

## 조건부 출력

Steps 또는 DAG 템플릿에서는 실행된 분기에 따라 하나의 출력을 선택할 수 있다. 파라미터에는 `valueFrom.expression`을, 아티팩트에는 `fromExpression`을 사용해 분기별 출력을 합친다.

```yaml
outputs:
  parameters:
    - name: selected
      valueFrom:
        expression: "steps.choose.outputs.result == 'a' ? steps.a.outputs.result : steps.b.outputs.result"
```

## 제어 흐름 설계 지침

- 파라미터는 설정, 식별자, 작은 결과에 사용함
- 파일과 대용량 데이터는 아티팩트에 사용함
- 전역 파라미터 남용보다 템플릿 입력, 출력을 명시해 결합도를 낮춤
- `withParam` 입력을 생성하는 단계는 유효한 JSON만 출력하도록 구성함
- 복잡한 조건은 컨테이너 코드 또는 별도 템플릿으로 옮겨 테스트 가능하게 만듦
- 민감 정보는 파라미터 기본값에 넣지 않고 Kubernetes Secret에서 주입함
