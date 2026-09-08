# WorkflowTemplate과 실행 관리

> 원본: https://argo-workflows.readthedocs.io/en/latest/workflow-templates/

> 참고: https://argo-workflows.readthedocs.io/en/latest/cluster-workflow-templates/

> 참고: https://argo-workflows.readthedocs.io/en/latest/cli/argo/

> 확인일: 2026-09-02

## 재사용 가능한 리소스

- `WorkflowTemplate`
  - 네임스페이스 범위의 재사용 가능한 Workflow 정의
  - 같은 네임스페이스의 Workflow에서 참조 가능함
- `ClusterWorkflowTemplate`
  - 클러스터 범위의 재사용 가능한 Workflow 정의
  - 여러 네임스페이스에서 참조 가능함
- 일반 `Workflow`
  - 실제 실행 상태를 갖는 일회성 인스턴스

## WorkflowTemplate 정의

```yaml
apiVersion: argoproj.io/v1alpha1
kind: WorkflowTemplate
metadata:
  name: message-pipeline
spec:
  entrypoint: main
  arguments:
    parameters:
      - name: message
        value: hello
  templates:
    - name: main
      container:
        image: busybox:1.36
        command: [echo]
        args: ["{{workflow.parameters.message}}"]
```

`WorkflowTemplate`에는 `Workflow.spec`와 거의 같은 실행 필드를 담을 수 있다. 정의를 클러스터에 저장해 두므로 같은 작업을 반복 제출하거나 중앙에서 관리하기에 적합하다. 템플릿을 변경하면 이후 실행에 영향을 주며, 이미 실행 중인 Workflow의 상태 객체와는 구분된다.

## 템플릿에서 Workflow 생성

```bash
argo submit --from workflowtemplate/message-pipeline -p message=goodbye
```

이 명령은 `WorkflowTemplate`에서 새 Workflow를 생성하면서 기본 `message` 값을 `goodbye`로 덮어쓴다. 클러스터에 적용된 정의를 사용하므로 Git에 저장된 정의와 버전 차이가 나지 않도록 관리해야 한다.

## `workflowTemplateRef`

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: message-run-
spec:
  workflowTemplateRef:
    name: message-pipeline
  arguments:
    parameters:
      - name: message
        value: from-reference
```

`workflowTemplateRef`는 전체 Workflow 사양을 참조할 때 사용한다. 제출한 Workflow의 인수와 참조 대상의 기본 인수가 병합되므로, 호출자는 실행별 메타데이터와 파라미터만 유지할 수 있다.

## 개별 `templateRef`

```yaml
templates:
  - name: main
    steps:
      - - name: call-shared
          templateRef:
            name: shared-library
            template: print-message
          arguments:
            parameters:
              - name: message
                value: hello
```

`templateRef`는 외부 WorkflowTemplate 안의 특정 `template`만 호출할 때 사용한다. 일반적으로 Steps 또는 DAG의 호출 위치에 지정하며, 호출자는 참조 대상의 입력 계약을 충족해야 한다.

## ClusterWorkflowTemplate 참조

```yaml
templateRef:
  name: cluster-library
  template: print-message
  clusterScope: true
```

- `clusterScope: true`가 없으면 네임스페이스 범위 WorkflowTemplate로 해석됨
- 여러 팀이 공유할 수 있으므로 변경 영향 범위가 큼
- 클러스터 범위 리소스에 대한 생성, 변경 권한을 제한해야 함
- 테넌트별 ServiceAccount, Secret, Artifact Repository 차이를 입력 계약에 반영함

## 라이브러리 설계

- 한 템플릿은 하나의 명확한 작업을 수행함
- `inputs`와 `outputs`를 공개 계약처럼 관리함
- 호출자가 알아야 하는 저장 경로, Secret 이름, 네임스페이스 의존성을 최소화함
- 기본값은 안전한 동작을 선택함
- 호환되지 않는 변경은 새 템플릿 이름 또는 새 리소스로 분리함
- 이미지 태그, 스크립트, 필드 지원 버전을 함께 고정함

## 적용과 검사

```bash
argo template lint workflow-template.yaml
argo template create workflow-template.yaml
argo template list
argo template get message-pipeline
argo template update workflow-template.yaml
```

- GitOps 환경에서는 `kubectl apply` 또는 배포 도구로 선언적 관리 가능함
- `create`와 `update`는 명령형 관리이므로 소유권 정책을 정해야 함
- 제출 전 `argo lint` 또는 `argo template lint` 실행 권장

## Workflow 실행 명령

```bash
argo submit workflow.yaml --watch
argo list
argo get WORKFLOW_NAME
argo logs WORKFLOW_NAME
argo wait WORKFLOW_NAME
```

- `submit`: Workflow 생성
- `list`: Workflow 목록과 상태 조회
- `get`: 실행 그래프, 노드 상태, 메시지 조회
- `logs`: 관련 Pod 로그 집계
- `wait`: 종료될 때까지 대기하고 결과 코드 반환

## 실행 중 제어

```bash
argo suspend WORKFLOW_NAME
argo resume WORKFLOW_NAME
argo stop WORKFLOW_NAME
argo terminate WORKFLOW_NAME
```

- `suspend`: 새 노드 실행을 일시 정지함
- `resume`: 정지된 Workflow 또는 Suspend 템플릿을 재개함
- `stop`: 실행 중인 노드를 정상 종료하는 방향으로 Workflow를 중단함
- `terminate`: 실행 중인 노드를 즉시 종료함
- 업무 보상 처리가 필요하면 명령의 종료 방식과 Exit Handler 동작을 사전 검증함

## 재실행 방식

```bash
argo retry WORKFLOW_NAME
argo resubmit WORKFLOW_NAME
argo resubmit WORKFLOW_NAME --memoized
```

- `retry`
  - 실패한 기존 Workflow에서 실패 노드를 다시 실행함
  - 같은 Workflow 객체의 실행 이력을 이어감
- `resubmit`
  - 기존 Workflow 명세로 새 Workflow 객체를 생성함
  - 전체 실행을 새 인스턴스로 시작함
- `--memoized`
  - 성공한 노드 결과를 재사용해 필요한 부분만 실행함
  - 입력, 외부 상태, 아티팩트 유효성이 그대로라는 전제 필요

## 정리 정책

- `podGC`: 완료된 Workflow의 Pod 삭제 시점 제어
- `ttlStrategy`: 완료된 Workflow CR의 자동 삭제 시점 제어
- `artifactGC`: 외부 저장소 아티팩트 삭제 시점 제어
- Workflow Archive: 완료된 Workflow 메타데이터를 데이터베이스에 장기 보관함
- 리소스 삭제와 감사, 결과 보존 요구를 분리해 설계함

## RBAC

- Workflow Pod의 ServiceAccount
  - 사용자 작업과 `resource` 템플릿이 사용하는 권한
- Workflow Controller ServiceAccount
  - Workflow, Pod, 관련 CRD를 조정하는 권한
- Argo Server ServiceAccount 또는 사용자 인증 정보
  - API를 통해 허용할 조회, 제출, 변경 범위
- 공유 ClusterWorkflowTemplate이 호출자에게 권한을 자동으로 부여하지는 않음
- 와일드카드 권한보다 리소스, 동작별 최소 권한 사용 권장

## 운영 체크리스트

- 템플릿 변경 전 참조하는 Workflow와 CronWorkflow 확인
- 제출 전 lint와 스테이징 실행 수행
- 이미지와 템플릿의 호환 버전 기록
- 출력, 아티팩트 계약 변경 시 호출자 동시 갱신
- 중단, 재시도, 재제출의 의미를 런북에 구분해 기록
- 완료 리소스, Pod, 아티팩트의 보존 기간 설정
