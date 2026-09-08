# Argo Workflows 개요와 아키텍처

> 원본: https://argo-workflows.readthedocs.io/en/latest/

> 참고: https://argo-workflows.readthedocs.io/en/latest/architecture/

> Kubernetes 배경: https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/

> 확인일: 2026-09-02

## Argo Workflows란

Argo Workflows는 Kubernetes에서 병렬 작업을 오케스트레이션하는 컨테이너 네이티브 워크플로 엔진이다. `Workflow`, `CronWorkflow`, `WorkflowTemplate` 같은 CRD와 이를 조정하는 컨트롤러로 구성되며, 각 작업을 컨테이너로 정의하고 실행 순서를 `steps` 또는 `dag`로 표현한다.

주로 머신러닝 파이프라인, 데이터 처리, 인프라 자동화, CI/CD에 사용한다. Argo CD와 같은 Argo 프로젝트에 속하지만, 배포 동기화를 담당하는 Argo CD와 달리 작업 실행을 담당한다.

## Kubernetes와의 관계

Kubernetes 기본 API에는 `Workflow`나 `CronWorkflow`가 없으며, Argo Workflows를 설치하면 `argoproj.io` API 그룹에 해당 CRD가 등록된다. 이후에는 일반 Kubernetes 리소스처럼 YAML을 제출하고 `kubectl`로 조회할 수 있다.

제출한 리소스는 Workflow Controller가 감시하면서 원하는 상태와 실제 실행 상태를 지속해서 조정한다. 실제 작업은 Pod에서 실행되므로 스케줄링, 볼륨, Secret, ServiceAccount는 Kubernetes 기능을 사용한다.

```text
Workflow YAML
  → Kubernetes API Server
  → Workflow Controller가 Workflow 감시
  → 실행할 Pod 생성
  → Kubernetes가 Pod 스케줄링
  → Controller가 Workflow status 갱신
```

## 주요 리소스

- `Workflow`
  - 한 번 실행되는 워크플로 정의이자 실행 상태를 보관하는 살아 있는 객체
  - `spec`에 실행 계획, `status`에 노드별 실행 결과가 기록됨
- `CronWorkflow`
  - cron 일정에 따라 `Workflow` 객체를 생성하는 스케줄 리소스
  - Kubernetes `CronJob`과 비슷한 일정, 동시성 정책을 제공함
- `WorkflowTemplate`
  - 네임스페이스 안에서 재사용하는 워크플로 정의
- `ClusterWorkflowTemplate`
  - 여러 네임스페이스에서 참조할 수 있는 클러스터 범위 정의

## 핵심 구성 요소

### Workflow Controller

Workflow Controller는 `Workflow`와 관련 Pod를 감시하면서 실행 가능한 노드를 판단하고 Pod를 생성한다. 실행 결과에 따라 완료, 실패, 재시도, 출력, 아티팩트 상태를 `Workflow.status`에 반영한다. 하나의 Workflow는 한 시점에 하나의 조정 작업에서 처리된다.

### Argo Server

Argo Server는 REST, gRPC API와 웹 UI를 제공해 Workflow 제출, 조회, 중지, 재시도 같은 사용자 요청을 처리한다. 실행 엔진인 Controller를 대신하지는 않으며, Controller는 Argo Server 없이도 Kubernetes API를 통해 독립적으로 실행할 수 있다.

### Workflow Executor

- 작업 Pod 안에서 사용자 컨테이너의 실행을 보조함
- 입력 아티팩트 다운로드, 출력 수집, 로그 처리, 컨테이너 수명 주기 관리를 담당함
- 전통적인 Pod 구성에서는 `init` 컨테이너와 `wait` 사이드카 형태로 동작함
- v4.1의 선택적 init-less 레이아웃에서는 `supervisor`가 실행 전후 작업을 담당함

## 기본 실행 흐름

- 1\. 사용자가 `Workflow` YAML을 Kubernetes API 또는 Argo Server에 제출함
- 2\. Workflow Controller가 새 객체를 감지함
- 3\. `entrypoint`가 가리키는 템플릿에서 실행 그래프를 구성함
- 4\. 의존 조건이 충족된 Step 또는 DAG Task마다 Pod를 생성함
- 5\. Executor가 입력을 준비하고 사용자 컨테이너를 실행함
- 6\. Executor가 출력 파라미터와 아티팩트를 수집함
- 7\. Controller가 다음 노드를 실행하거나 Workflow를 완료 상태로 전환함

## `Workflow`와 Kubernetes `Job` 비교

- Kubernetes `Job`
  - 하나의 Pod 템플릿을 완료될 때까지 실행하는 기본 컨트롤러
  - 단순한 일회성 작업에 적합함
- Argo `Workflow`
  - 여러 템플릿의 순서, 의존성, 데이터 전달을 하나의 실행 그래프로 관리함
  - 조건, 반복, 재시도, 아티팩트, 종료 처리 같은 파이프라인 기능을 제공함

단일 작업만 실행한다면 `Job`으로 구성하는 편이 단순하다. 여러 작업의 순서와 의존성, 데이터 전달까지 관리해야 할 때 `Workflow`를 선택한다.

## 최소 Workflow

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: hello-world-
spec:
  entrypoint: hello
  templates:
    - name: hello
      container:
        image: busybox:1.36
        command: [echo]
        args: ["hello world"]
```

- `generateName`: 실행마다 고유한 이름을 생성하는 접두사
- `entrypoint`: 처음 호출할 템플릿 이름
- `templates`: 실행할 작업과 제어 흐름의 정의 목록
- 위 예제 제출 시 `hello-world-xxxxx` 형태의 Workflow와 작업 Pod가 생성됨

## 기본 CLI 흐름

```bash
argo lint hello.yaml
argo submit hello.yaml --watch
argo list
argo get @latest
argo logs @latest
```

- `argo lint`: 클러스터에 제출하기 전에 명세 검사
- `argo submit --watch`: 제출 후 완료까지 상태 관찰
- `argo get`: 노드별 실행 그래프와 상태 조회
- `argo logs`: Workflow의 Pod 로그 조회
