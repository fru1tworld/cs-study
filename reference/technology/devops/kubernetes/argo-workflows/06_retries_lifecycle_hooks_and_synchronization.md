# 재시도, 수명 주기 훅과 동기화

> 원본: https://argo-workflows.readthedocs.io/en/latest/retries/

> 참고: https://argo-workflows.readthedocs.io/en/latest/lifecyclehook/

> 참고: https://argo-workflows.readthedocs.io/en/latest/walk-through/exit-handlers/

> 참고: https://argo-workflows.readthedocs.io/en/latest/synchronization/

> 확인일: 2026-09-02

## 실패 상태 구분

- `Failed`: 사용자 컨테이너가 0이 아닌 종료 코드 등으로 실패함
- `Error`: Pod 생성, 아티팩트 처리, 컨트롤러 작업 같은 실행 인프라에서 오류가 발생함

`retryPolicy`는 이 상태 중 어떤 것을 다시 실행할지 결정한다. 업무 오류와 일시적 인프라 오류는 대응 방법이 다르므로, 실패 상태를 구분한 뒤 재시도 정책을 정한다.

## `retryStrategy`

```yaml
retryStrategy:
  limit: "4"
  retryPolicy: OnTransientError
  backoff:
    duration: "5s"
    factor: 2
    maxDuration: "1m"
```

- `limit`: 최초 시도를 제외한 최대 재시도 횟수
- `retryPolicy`
  - `OnFailure`: 실패한 사용자 작업 재시도
  - `OnError`: Error 상태 재시도
  - `OnTransientError`: 일시적 오류로 판정된 경우 재시도
  - `Always`: 실패와 오류를 모두 재시도
- `backoff.duration`: 첫 재시도 전 대기 시간
- `backoff.factor`: 대기 시간 증가 배수
- `backoff.maxDuration`: 재시도 간 최대 대기 시간

## 조건부 재시도

```yaml
retryStrategy:
  limit: "5"
  retryPolicy: Always
  expression: "asInt(lastRetry.exitCode) != 2"
```

`expression`이 참일 때만 다음 재시도를 수행한다. 위 예제에서는 종료 코드가 2이면 재시도를 멈춘다. `lastRetry`로 직전 시도의 상태, 종료 코드, 실행 시간, 메시지를 확인할 수 있으므로, 영구 오류를 나타내는 종료 코드는 즉시 중단하도록 애플리케이션과 정책을 함께 설계한다.

빈 `retryStrategy: {}`는 성공할 때까지 계속 재시도할 수 있으므로, 반복 횟수 제한이 필요하다면 명시적으로 설정한다.

## 재시도 설계 원칙

- 작업은 가능한 한 멱등하게 구성함
- 외부 시스템에 쓰는 작업은 idempotency key 또는 중복 검사를 사용함
- 무제한 재시도보다 명시적 `limit`와 backoff 사용 권장
- 재시도로 생성된 중간 리소스를 정리할 방법 마련
- 노드 장애가 의심되면 `affinity.nodeAntiAffinity: {}`로 같은 노드를 피하는 방안 검토

## 타임아웃

- Workflow 전체
  - `spec.activeDeadlineSeconds`로 총 실행 시간 제한
- 템플릿
  - `activeDeadlineSeconds`로 컨테이너, 스크립트 실행 시간 제한
- DAG Task
  - `timeout`으로 Task 수준 제한 가능함
- cron 일정 간격보다 실행 제한 시간이 길면 중복 실행 정책과 함께 검토 필요

## Exit Handler

```yaml
spec:
  entrypoint: main
  onExit: cleanup
  templates:
    - name: main
      container:
        image: busybox:1.36
        command: [sh, -c]
        args: ["exit 1"]
    - name: cleanup
      container:
        image: busybox:1.36
        command: [sh, -c]
        args: ["echo {{workflow.name}} {{workflow.status}}"]
```

Exit Handler는 Workflow의 성공 여부와 관계없이 마지막에 실행되므로 임시 리소스 정리, 완료 알림, 외부 상태 갱신에 적합하다. `workflow.status`로 `Succeeded`, `Failed`, `Error` 중 어떤 상태로 끝났는지 확인할 수 있다. Exit Handler 자체도 실패할 수 있으므로 핵심 정리 작업은 멱등하게 구성한다.

## Lifecycle Hook

```yaml
spec:
  hooks:
    running:
      expression: workflow.status == "Running"
      template: notify-running
```

Lifecycle Hook은 실행 중 조건이 충족되면 지정한 템플릿을 한 번 실행한다. Workflow 수준과 템플릿 수준에 설정할 수 있으며, 주 작업과 병렬로 실행될 수도 있다. 완료 후에 실행되는 Exit Handler와는 실행 시점이 다르다. 훅 이름을 `exit`로 지정하면 Exit Handler로 취급되므로 다른 이름을 사용한다.

## 병렬성 제어 방식

- `parallelism`
  - 하나의 Workflow 또는 템플릿 안에서 동시에 실행할 노드 수 제한
- mutex
  - 같은 잠금을 쓰는 Workflow 또는 템플릿을 한 번에 하나만 실행
- semaphore
  - 같은 잠금을 쓰는 실행을 설정된 개수까지 허용
- `CronWorkflow.concurrencyPolicy`
  - 같은 CronWorkflow가 만든 실행 사이의 중복 정책

## Workflow 내부 `parallelism`

```yaml
spec:
  parallelism: 10
```

- 하나의 Workflow에서 동시에 실행되는 Step, Task 수를 제한함
- 다른 Workflow의 실행 수에는 직접 영향을 주지 않음
- Controller 수준 제한과 네임스페이스 제한이 있으면 더 작은 유효 한도가 적용될 수 있음

## Mutex

```yaml
spec:
  synchronization:
    mutexes:
      - name: production-deploy
```

- 같은 네임스페이스와 이름의 mutex를 참조하는 실행 중 하나만 잠금을 획득함
- 배타적 배포, 공유 장비 사용, 단일 기록자 작업에 적합함
- 이전의 단일 `mutex` 필드는 현재 `latest`에서 제거됨 → `mutexes` 목록 사용 필요

## Semaphore

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: workflow-limits
data:
  api-calls: "3"
```

```yaml
spec:
  synchronization:
    semaphores:
      - configMapKeyRef:
          name: workflow-limits
          key: api-calls
```

- 위 예제는 같은 잠금을 쓰는 실행을 최대 3개까지 허용함
- Controller가 ConfigMap을 읽을 RBAC 권한 필요
- 이전의 단일 `semaphore` 필드는 현재 `latest`에서 제거됨 → `semaphores` 목록 사용 필요
- v3.7+에서는 여러 Controller 사이에서 공유하는 데이터베이스 잠금도 구성 가능함

## 잠금 대기열

잠금을 얻지 못한 Workflow는 정렬된 대기열에 들어간다. `priority`가 높은 Workflow를 먼저 처리하고, 우선순위가 같으면 먼저 생성된 Workflow가 앞선다.

여러 잠금을 동시에 요구하는 Workflow는 모든 잠금을 획득할 수 있을 때까지 기다린다. 따라서 공통 잠금을 많이 묶으면 앞선 Workflow 때문에 뒤의 실행들도 함께 막힐 수 있다.

## 관찰과 진단

```bash
kubectl get workflow WORKFLOW_NAME -o yaml
argo get WORKFLOW_NAME
```

- `.status.synchronization`에서 보유, 대기 중인 잠금 확인
- 재시도 노드의 종료 코드와 메시지를 `argo get`과 Pod 로그로 함께 확인
- `Failed`와 `Error`를 구분해 애플리케이션, Executor, Controller 문제를 분리함
- 알림 훅이 본 작업보다 많은 실패와 재시도를 만들지 않는지 모니터링함

## 운영 지침

- 외부 API 제한은 semaphore로 표현함
- 배타적 변경은 mutex로 표현함
- 한 Workflow의 폭발적 fan-out은 `parallelism`으로 제한함
- 정기 실행 중복은 `CronWorkflow.concurrencyPolicy`로 제어함
- 종료 처리와 알림은 서로 다른 템플릿으로 분리해 실패 영향을 줄임
- 재시도, 훅, 잠금이 결합된 경우 실패 시나리오를 작은 예제로 먼저 검증함
