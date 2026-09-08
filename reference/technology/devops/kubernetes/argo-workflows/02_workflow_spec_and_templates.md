# Workflow 사양과 템플릿

> 원본: https://argo-workflows.readthedocs.io/en/latest/workflow-concepts/

> 참고: https://argo-workflows.readthedocs.io/en/latest/walk-through/the-structure-of-workflow-specs/

> 필드 레퍼런스: https://argo-workflows.readthedocs.io/en/latest/fields/

> 확인일: 2026-09-02

## Workflow 객체의 두 역할

Workflow 객체는 `spec`에 실행할 워크플로를 정의하고 `status`에 현재 실행 상태를 저장한다. 같은 YAML을 여러 번 제출하면 서로 다른 Workflow 실행 인스턴스가 생성되므로, 반복해서 사용할 정적 정의는 `WorkflowTemplate`에 두는 편이 적합하다.

## 최상위 구조

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: example-
spec:
  entrypoint: main
  arguments: {}
  templates: []
```

- `apiVersion`: Argo Workflows CRD의 API 버전
- `kind`: 일회성 실행은 `Workflow`
- `metadata`: 이름, 네임스페이스, 레이블, 어노테이션
- `spec.entrypoint`: 프로그램의 `main` 함수처럼 처음 실행할 템플릿
- `spec.arguments`: entrypoint에 넘길 전역 입력
- `spec.templates`: 이름으로 참조할 템플릿 목록

## 템플릿의 두 범주

### 작업 정의 템플릿

- `container`: 하나의 컨테이너를 실행함
- `script`: 컨테이너 이미지 안에서 인라인 스크립트를 실행함
- `resource`: Kubernetes 리소스에 생성, 조회, 변경, 삭제 작업을 수행함
- `suspend`: 수동 또는 지정 시간 동안 실행을 일시 정지함
- `plugin`: Executor Plugin이 제공하는 작업을 호출함
- `containerSet`: 하나의 Pod 안에서 여러 컨테이너를 의존 관계에 따라 실행함
- `http`: Agent가 HTTP 요청을 실행함
- 한 템플릿에는 주 작업 유형 하나만 두는 것이 기본 원칙임

### 템플릿 호출자

- `steps`: 단계의 목록을 순서대로 실행함
- `dag`: Task 사이의 의존 관계로 실행 순서를 결정함
- 호출자는 다른 템플릿에 `arguments`를 전달할 수 있음

## Container 템플릿

```yaml
templates:
  - name: print-message
    inputs:
      parameters:
        - name: message
    container:
      image: busybox:1.36
      command: [echo]
      args: ["{{inputs.parameters.message}}"]
```

Container 템플릿은 Kubernetes `Container` 사양과 유사한 필드를 사용한다. `image`, `command`, `args`로 실행할 프로그램을 지정하고, `env`, `resources`, `volumeMounts`로 실행 환경을 구성한다.

위 예제는 `inputs.parameters`에 `message`를 먼저 선언한 뒤 `args`에서 참조한다. 이처럼 템플릿 표현식이 들어간 YAML 문자열은 따옴표로 감싸는 것이 안전하다.

## Script 템플릿

```yaml
templates:
  - name: calculate
    script:
      image: python:3.13-alpine
      command: [python]
      source: |
        print(6 * 7)
```

Script 템플릿은 `container`에 `source` 편의 기능을 더한 형태다. 별도 이미지로 패키징하지 않고 짧은 스크립트를 실행할 때 유용하며, 표준 출력은 `outputs.result`로 참조할 수 있다. 다만 긴 업무 로직은 이미지에 포함해 버전을 관리하는 편이 실행을 재현하기 쉽다.

## Resource 템플릿

```yaml
templates:
  - name: create-config
    resource:
      action: create
      manifest: |
        apiVersion: v1
        kind: ConfigMap
        metadata:
          generateName: workflow-data-
        data:
          state: ready
```

Resource 템플릿은 `create`, `apply`, `get`, `delete`, `patch` 등의 동작으로 Kubernetes 리소스를 직접 다룬다. 위 예제는 ConfigMap을 생성하므로 Workflow의 ServiceAccount에 해당 리소스 생성 권한이 필요하다. RBAC는 작업에 필요한 최소 권한으로 구성해 권한 오류와 과도한 권한 노출을 방지한다.

## Suspend 템플릿

```yaml
templates:
  - name: approval
    suspend: {}
```

- 사람이 승인할 때까지 기다리는 단계에 사용 가능함
- `argo resume WORKFLOW_NAME`으로 재개 가능함
- `duration: "10m"`을 지정하면 시간이 지난 뒤 자동 재개됨
- 입력을 받는 승인 단계로 구성할 수도 있음

## HTTP 템플릿

```yaml
templates:
  - name: health-check
    http:
      url: https://example.com/health
      method: GET
      successCondition: response.statusCode == 200
```

- HTTP 요청은 사용자 작업 Pod가 아니라 Argo의 Agent가 실행함
- 응답 본문은 `outputs.result`로 사용할 수 있음
- 네트워크 정책, 인증 정보, 응답 크기 제한을 함께 검토해야 함

## ContainerSet 템플릿

ContainerSet은 여러 컨테이너를 하나의 Pod에 배치하므로 컨테이너 사이에 파일시스템이나 네트워크를 공유해야 할 때 유용하다. Pod 내부의 실행 순서는 컨테이너별 `dependencies`로 지정한다. 컨테이너마다 별도의 스케줄링, 재시도, 리소스 격리가 필요하다면 DAG Task로 분리하는 편이 적합하다.

## `template`과 `WorkflowTemplate` 구분

- `template`
  - `spec.templates` 아래의 개별 작업 또는 제어 흐름 정의
  - 소문자로 지칭하는 개념이며 독립 Kubernetes 리소스가 아님
- `WorkflowTemplate`
  - Kubernetes API에 저장되는 재사용 가능한 CRD
  - 자체 `entrypoint`, `arguments`, `templates`를 가질 수 있음
- 이름이 비슷하지만 계층과 수명 주기가 다름

## 공통 실행 설정

- `serviceAccountName`: Workflow Pod가 사용할 ServiceAccount
- `podGC`: 완료된 Pod 정리 정책
- `ttlStrategy`: 완료된 Workflow 리소스의 TTL
- `activeDeadlineSeconds`: 전체 실행 제한 시간
- `parallelism`: 한 Workflow 안에서 동시에 실행할 노드 수
- `podSpecPatch`: 생성되는 Pod 사양에 전략적 패치를 적용함
- `securityContext`: Workflow 수준 Pod 보안 컨텍스트
- `volumes`: 템플릿들이 참조할 공통 볼륨

## 최소 권장 구조

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: safe-example-
spec:
  entrypoint: main
  serviceAccountName: workflow-runner
  podGC:
    strategy: OnWorkflowCompletion
  ttlStrategy:
    secondsAfterCompletion: 3600
  templates:
    - name: main
      container:
        image: busybox:1.36
        command: [sh, -c]
        args: ["echo done"]
        resources:
          requests:
            cpu: 10m
            memory: 16Mi
          limits:
            memory: 64Mi
```

- 변경 가능한 이미지 태그보다 고정 태그 또는 digest 사용 권장
- 요청, 제한 리소스를 지정해 스케줄링과 장애 격리를 예측 가능하게 만듦
- Pod와 Workflow 정리 정책을 명시해 리소스 누적을 방지함
- 기본 ServiceAccount 대신 업무별 최소 권한 계정 사용 권장
