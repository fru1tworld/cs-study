# CronWorkflow 일정과 실행 정책

> 원본: https://argo-workflows.readthedocs.io/en/latest/cron-workflows/

> 필드 레퍼런스: https://argo-workflows.readthedocs.io/en/latest/fields/#cronworkflowspec

> Kubernetes 비교: https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/

> 확인일: 2026-09-02

## CronWorkflow란

CronWorkflow는 cron 일정에 맞춰 `Workflow` 객체를 생성하는 Argo CRD다. `Workflow` 사양에 일정, 시간대, 중복 실행, 누락 보정 정책을 더한 형태이며, 실제 작업 상태는 생성된 자식 Workflow에 기록된다. Kubernetes `CronJob`과 비슷한 옵션을 제공하지만 생성 대상은 Job이 아니라 Argo Workflow다.

```text
CronWorkflow
  → schedule 도래
  → 새 Workflow 생성
  → Workflow Controller가 Pod 실행
```

## 기본 구조

```yaml
apiVersion: argoproj.io/v1alpha1
kind: CronWorkflow
metadata:
  name: daily-report
spec:
  schedules:
    - "0 9 * * *"
  timezone: Asia/Seoul
  concurrencyPolicy: Forbid
  startingDeadlineSeconds: 600
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 5
  workflowSpec:
    entrypoint: report
    templates:
      - name: report
        container:
          image: busybox:1.36
          command: [sh, -c]
          args: ["date; echo create report"]
```

- `schedules`: 하나 이상의 cron 일정
- `timezone`: 일정을 계산할 IANA 시간대
- `concurrencyPolicy`: 이전 실행과 새 실행이 겹칠 때의 정책
- `startingDeadlineSeconds`: 놓친 일정의 보정 허용 시간
- `workflowSpec`: 새로 생성할 Workflow의 `spec`

## Cron 표현식

```text
┌──────── 분 0-59
│ ┌────── 시 0-23
│ │ ┌──── 일 1-31
│ │ │ ┌── 월 1-12
│ │ │ │ ┌ 요일 0-6
│ │ │ │ │
0 9 * * 1-5
```

- 위 식은 월요일부터 금요일까지 오전 9시에 실행됨
- 초 필드는 사용하지 않는 5필드 cron 형식
- `schedules`는 v3.6+에서 여러 일정을 지원함
- 이전의 단일 `schedule` 필드는 현재 `latest`에서 제거됨 → `schedules` 사용 필요

## 시간대

```yaml
spec:
  timezone: Asia/Seoul
```

시간대를 생략하면 Controller의 로컬 시간대를 사용하므로 운영 환경에 따라 실행 시각이 달라질 수 있다. 위 예시처럼 IANA Time Zone Database의 이름을 `timezone`에 명시하는 편이 좋다. `CRON_TZ=`나 `TZ=`를 cron 문자열 안에 넣는 방식은 사용하지 않는다.

## 일광 절약 시간

- 시간이 앞으로 이동하는 구간
  - 존재하지 않는 현지 시각의 일정은 실행되지 않을 수 있음
- 시간이 뒤로 이동하는 구간
  - 같은 현지 시각이 두 번 나타나 중복 실행 가능함
- DST 지역에서는 멱등성, 중복 방지, 누락 보정 정책을 함께 설계함
- UTC 또는 DST가 없는 업무 시간대를 사용할 수 있는지 우선 검토함

## `concurrencyPolicy`

### `Allow`

- 기본값
- 이전 Workflow가 실행 중이어도 새 Workflow를 생성함
- 실행 시간이 일정 간격보다 길면 동시 실행 수가 계속 늘 수 있음

### `Forbid`

- 이전 실행이 활성 상태이면 새 일정을 건너뜀
- 같은 작업의 중복 실행이 위험할 때 적합함
- 건너뛴 실행을 나중에 자동으로 모두 복원하는 정책은 아님

### `Replace`

- 이전 활성 Workflow를 종료하고 새 Workflow를 실행함
- 최신 실행만 의미가 있을 때 적합함
- 이전 실행이 남긴 외부 상태를 새 실행이 안전하게 이어받을 수 있어야 함

## 놓친 일정과 복구

```yaml
spec:
  startingDeadlineSeconds: 600
```

`startingDeadlineSeconds`는 Controller 중단이나 일시 정지로 놓친 일정을 얼마나 늦게까지 실행할 수 있는지 정한다. 기본값인 `0`에서는 놓친 일정을 다시 실행하지 않는다. 양수로 설정해 복구 조건을 충족하더라도 현재 구현은 누락분 중 하나의 Workflow만 생성한다.

값을 너무 크게 잡으면 복구 직후 오래된 시점의 작업이 뒤늦게 실행될 수 있다. 업무 데이터의 처리 기준 시각을 Workflow 입력으로 명시해 두면 이런 재실행이나 backfill에서 처리 대상을 구분하기 쉽다.

## 일시 정지

```yaml
spec:
  suspend: true
```

- 새 Workflow 생성을 중지함
- 이미 생성되어 실행 중인 Workflow를 자동 중단하지 않음

```bash
argo cron suspend daily-report
argo cron resume daily-report
```

- 재개 시 놓친 일정 처리 여부는 `startingDeadlineSeconds`와 중단 기간에 따라 달라짐

## 실행 조건 `when`

```yaml
spec:
  when: "{{=cronworkflow.lastScheduledTime == nil || (now() - cronworkflow.lastScheduledTime).Seconds() > 3600}}"
```

- v3.6+ 기능
- 일정이 도래해도 표현식이 참일 때만 Workflow를 생성함
- 여러 일정이 가까이 겹칠 때 최소 간격을 보장하는 데 활용 가능함
- `cronworkflow.lastScheduledTime`은 최초 실행 전에 `nil`일 수 있음

## 자동 중지 `stopStrategy`

```yaml
spec:
  stopStrategy:
    expression: "cronworkflow.succeeded >= 10"
```

- v3.6+ 기능
- 표현식이 참이 되면 이후 일정을 더 이상 생성하지 않음
- `cronworkflow.succeeded`와 `cronworkflow.failed` 카운터 사용 가능함
- `suspend`와 달리 조건 충족에 따른 종료 상태를 표현함

## 생성되는 Workflow 메타데이터

```yaml
spec:
  workflowMetadata:
    labels:
      app.kubernetes.io/name: daily-report
      workload.example.com/schedule: daily
    annotations:
      owner.example.com/team: data
```

- 자식 Workflow에 레이블과 어노테이션을 추가함
- 비용, 소유 팀, 스케줄, 데이터 날짜를 레이블로 남기면 조회와 운영이 쉬워짐
- Pod 메타데이터가 필요하면 `workflowSpec.podMetadata` 등 Workflow 필드와 구분함

## WorkflowTemplate 참조

```yaml
spec:
  schedules:
    - "0 9 * * *"
  timezone: Asia/Seoul
  workflowSpec:
    workflowTemplateRef:
      name: report-template
    arguments:
      parameters:
        - name: mode
          value: daily
```

WorkflowTemplate을 참조하면 실행 로직과 스케줄 정의를 분리할 수 있다. 여러 CronWorkflow가 같은 템플릿을 서로 다른 일정과 인수로 재사용할 수 있지만, 템플릿 변경이 이후의 모든 정기 실행에 영향을 주므로 배포 순서와 호환성을 관리해야 한다.

## 이력 보존

```yaml
spec:
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 5
```

- 성공, 실패한 자식 Workflow를 각각 몇 개 유지할지 지정함
- 필드 이름에 `Jobs`가 들어가지만 보존 대상은 Argo Workflow임
- 장기 감사가 필요하면 Workflow Archive와 외부 로그, 아티팩트 보존을 별도 구성함

## 관리 명령

```bash
argo cron lint cron-workflow.yaml
argo cron create cron-workflow.yaml
argo cron list
argo cron get daily-report
argo cron suspend daily-report
argo cron resume daily-report
```

## 운영 체크리스트

- `timezone` 명시
- `concurrencyPolicy`를 업무 특성에 맞게 선택
- 일정 간격과 최악 실행 시간 비교
- `startingDeadlineSeconds`로 누락 복구 범위 제한
- Workflow 입력에 논리적 처리 시각 또는 데이터 날짜 전달
- 멱등성과 중복 방지 키 구성
- 성공, 실패 이력과 아티팩트 보존 기간 설정
- DST, Controller 장애, 장시간 실행, 수동 재개 시나리오 검증
