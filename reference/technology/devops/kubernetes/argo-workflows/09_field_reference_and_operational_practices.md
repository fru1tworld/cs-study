# 필드 레퍼런스와 운영 지침

> 원본: https://argo-workflows.readthedocs.io/en/latest/fields/

> 보안: https://argo-workflows.readthedocs.io/en/latest/security/

> 확장성: https://argo-workflows.readthedocs.io/en/latest/running-at-massive-scale/

> 확인일: 2026-09-02

## 문서 사용법

이 문서는 학습과 검토에 자주 필요한 필드를 요약한다. 전체 형식, 기본값, 버전별 지원 여부는 공식 Field Reference를 기준으로 확인한다. 설치된 CRD와 Controller 버전이 문서의 `latest`보다 오래될 수 있으므로, 적용 전에는 `argo version`과 클러스터 CRD 스키마도 확인해야 한다.

## Workflow 최상위 필드

- `entrypoint`: 최초 실행할 템플릿 이름
- `templates`: 로컬 템플릿 목록
- `arguments`: entrypoint와 전역 범위에 제공할 입력
- `workflowTemplateRef`: WorkflowTemplate 또는 ClusterWorkflowTemplate 전체 참조
- `serviceAccountName`: 작업 Pod의 ServiceAccount
- `parallelism`: 동시에 실행할 노드 수
- `priority`: Controller 대기열 우선순위
- `activeDeadlineSeconds`: Workflow 전체 실행 제한 시간
- `onExit`: 완료 후 항상 호출할 Exit Handler
- `hooks`: 실행 중 조건에 따라 호출할 Lifecycle Hook
- `synchronization`: mutex, semaphore 잠금
- `suspend`: Workflow 실행 일시 정지

## Pod 구성 필드

- `podMetadata`: 생성되는 Pod의 레이블, 어노테이션
- `podSpecPatch`: 생성 Pod에 적용할 전략적 병합 패치
- `securityContext`: Pod 수준 보안 컨텍스트
- `affinity`: 노드, Pod 배치 선호와 제약
- `tolerations`: taint가 있는 노드에 스케줄할 허용 조건
- `nodeSelector`: 노드 레이블 기반 배치 조건
- `schedulerName`: 사용할 Kubernetes 스케줄러
- `hostNetwork`: 호스트 네트워크 사용 여부
- `dnsPolicy`: Pod DNS 정책
- `imagePullSecrets`: 비공개 이미지 저장소 자격 증명
- `volumes`: 템플릿에서 마운트할 공통 볼륨

## 완료 후 정리 필드

- `podGC`
  - `OnPodCompletion`, `OnPodSuccess`, `OnWorkflowCompletion`, `OnWorkflowSuccess` 등 정책 검토
  - 로그 수집과 장애 조사 전에 Pod가 삭제되지 않도록 지연 설정 고려
- `ttlStrategy`
  - 성공, 실패, 완료 후 Workflow CR 삭제 시간을 각각 지정 가능함
- `volumeClaimGC`
  - Workflow가 만든 PVC 삭제 정책
- `artifactGC`
  - 외부 저장소 아티팩트의 삭제 시점과 ServiceAccount 지정

## Template 공통 필드

- `name`: Workflow 안에서 고유한 템플릿 이름
- `inputs`: 입력 파라미터, 아티팩트
- `outputs`: 출력 파라미터, 아티팩트
- `retryStrategy`: 실패, 오류 재시도 정책
- `activeDeadlineSeconds`: 컨테이너, 스크립트 실행 제한 시간
- `timeout`: 템플릿 실행 제한
- `parallelism`: 템플릿 내부 병렬 실행 제한
- `synchronization`: 템플릿 수준 잠금
- `memoize`: 입력을 key로 이전 결과 재사용
- `nodeSelector`, `affinity`, `tolerations`: 템플릿 수준 배치 제어
- `metadata`: 생성되는 Pod에 템플릿별 메타데이터 추가

## Steps와 DAG 호출 필드

- `name`: 실행 노드 이름
- `template`: 같은 Workflow 안의 템플릿 이름
- `templateRef`: 외부 WorkflowTemplate의 템플릿 참조
- `arguments`: 호출 대상에 전달할 파라미터, 아티팩트
- `when`: 조건부 실행 식
- `withItems`, `withParam`, `withSequence`: 반복 실행 입력
- `hooks`: Step 또는 Task 수준 수명 주기 훅
- DAG 전용
  - `dependencies`: 단순 선행 Task 목록
  - `depends`: 상태를 포함한 의존 논리식
  - `continueOn`: 이전 호환용 실패 진행 설정

## CronWorkflowSpec 필드

- `schedules`: v3.6+ cron 일정 목록
- `timezone`: 일정 계산 시간대
- `concurrencyPolicy`: `Allow`, `Forbid`, `Replace`
- `startingDeadlineSeconds`: 놓친 일정 실행 허용 기한
- `suspend`: 새 일정 생성 중지
- `when`: v3.6+ 실행 여부 표현식
- `stopStrategy`: v3.6+ 자동 일정 중지 조건
- `workflowSpec`: 생성할 Workflow 사양
- `workflowMetadata`: 생성할 Workflow의 레이블, 어노테이션
- `successfulJobsHistoryLimit`: 보존할 성공 Workflow 수
- `failedJobsHistoryLimit`: 보존할 실패 Workflow 수

## 상태 확인

```bash
argo get WORKFLOW_NAME
kubectl get workflow WORKFLOW_NAME -o yaml
kubectl get pods -l workflows.argoproj.io/workflow=WORKFLOW_NAME
```

- `status.phase`: Workflow 전체 상태
- `status.message`: 현재 상태의 설명
- `status.startedAt`, `status.finishedAt`: 실행 시각
- `status.nodes`: Step, Task, Pod별 상태와 입력, 출력
- `status.outputs`: Workflow 전체 출력
- `status.synchronization`: 잠금 보유, 대기 상태
- 상태 객체가 매우 커지면 Node Status Offloading 검토

## 보안

### ServiceAccount와 RBAC

- 기본 ServiceAccount 사용을 피하고 업무별 ServiceAccount 구성
- `resource` 템플릿이 필요한 API 동작만 허용
- Argo Server의 사용자 권한과 Workflow Pod의 권한을 분리함
- ClusterWorkflowTemplate 사용 권한과 실행 ServiceAccount 권한은 별개임

### Pod 보안

```yaml
spec:
  securityContext:
    runAsNonRoot: true
  templates:
    - name: main
      container:
        image: example.com/app@sha256:REPLACE_ME
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
```

- non-root 실행과 권한 상승 차단 검토
- 필요한 쓰기 경로만 볼륨으로 제공
- 이미지 digest 고정과 서명, 취약점 검사 정책 적용
- 업무 컨테이너에 Kubernetes API 토큰이 필요 없으면 자동 마운트 비활성화 검토

### Secret과 로그

- 민감 값을 Workflow 파라미터, 레이블, 어노테이션에 직접 저장하지 않음
- Secret을 환경 변수 또는 볼륨으로 주입함
- 명령행 인수와 표준 출력에 자격 증명이 노출되지 않도록 함
- 아티팩트와 아카이브 로그의 저장소 암호화, 접근 정책 구성

## 확장성과 비용

- `parallelism`으로 한 Workflow의 fan-out 제한
- Controller, 네임스페이스 수준 parallelism 제한 검토
- 매우 짧은 작업을 수천 개 Pod로 분리하지 않도록 작업 단위 조정
- 대형 Workflow는 Node Status Offloading과 Workflow Archive 검토
- 완료 Pod, Workflow, PVC, 아티팩트 정리 정책 구성
- 동일 입력 반복 처리가 비싸면 안전한 memoization 검토
- Controller 샤딩, HA, 데이터베이스 구성은 설치 규모와 장애 모델에 맞춤

## 장애 유형별 확인 순서

### Workflow가 Pending에 머묾

- Controller 로그와 Workflow `status.message` 확인
- Controller 또는 네임스페이스 parallelism 제한 확인
- mutex, semaphore 대기 여부 확인
- Workflow 우선순위와 대기열 확인

### Pod가 Pending에 머묾

- `kubectl describe pod`의 스케줄링 이벤트 확인
- 리소스 요청, nodeSelector, affinity, toleration 확인
- PVC 바인딩과 이미지 pull Secret 확인

### 노드가 Error 상태임

- 사용자 컨테이너 종료 코드보다 Executor, init, wait 로그를 먼저 구분함
- 입력 다운로드와 출력 업로드 오류 확인
- ServiceAccount RBAC와 API 오류 확인
- Controller와 Executor 버전 호환성 확인

### 노드가 Failed 상태임

- 사용자 컨테이너 로그와 종료 코드 확인
- 입력 파라미터와 아티팩트 유효성 확인
- 업무 오류가 재시도 가능한지 판단
- 부분 실행이 외부 시스템에 남긴 상태 확인

## 변경 전 검증

```bash
argo lint workflow.yaml
argo template lint workflow-template.yaml
argo cron lint cron-workflow.yaml
kubectl apply --dry-run=server -f workflow.yaml
```

로컬 lint로 구조와 참조 오류를 확인한 뒤, 서버 dry-run으로 설치된 CRD 스키마와 Admission 정책에 맞는지 확인한다. 두 검증을 통과하면 작은 입력으로 스테이징에서 실행한다.

실행 결과는 성공 여부뿐 아니라 업무 실패, 인프라 오류, 중단, 재시도 상황까지 확인한다. Workflow가 생성한 리소스가 정리 정책에 따라 삭제되는지도 함께 살펴본다.

## 버전 업그레이드 점검

- 공식 Upgrading Guide와 Deprecations 확인
- 설치된 Controller, Argo Server, CLI 버전 호환성 확인
- CRD를 Controller와 함께 갱신했는지 확인
- 제거된 단일 `mutex`, `semaphore` 대신 목록 필드 사용
- 제거된 CronWorkflow의 단일 `schedule` 대신 `schedules` 사용
- 버전별 기본값 변경이 재시도, 보안, 보존 정책에 미치는 영향 검토
- 업그레이드 전 대표 Workflow와 CronWorkflow를 별도 환경에서 실행

## 최종 점검 목록

- 실행 이미지 버전 고정
- 입력, 출력 계약 명시
- 최소 권한 ServiceAccount 사용
- 자원 요청, 제한 설정
- 타임아웃, 재시도, 동시성 정책 설정
- 시간대와 cron 중복 정책 설정
- Pod, Workflow, PVC, 아티팩트 정리 정책 설정
- 로그, 메트릭, 아카이브를 통한 추적성 확보
- 중단, 재실행, backfill 런북 준비
