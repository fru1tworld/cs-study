# 아티팩트와 데이터 흐름

> 원본: https://argo-workflows.readthedocs.io/en/latest/walk-through/artifacts/

> 참고: https://argo-workflows.readthedocs.io/en/latest/configure-artifact-repository/

> 참고: https://argo-workflows.readthedocs.io/en/latest/workflow-inputs/

> 확인일: 2026-09-02

## 아티팩트란

아티팩트는 템플릿에 파일이나 디렉터리를 입력하거나 실행 결과를 외부 저장소에 보관하는 데이터 단위다. 파라미터보다 큰 데이터, 바이너리, 모델, 리포트를 전달할 때 적합하다.

Executor가 작업 시작 전에 입력 아티팩트를 내려받고, 작업이 끝나면 출력 아티팩트를 업로드한다. 기본 저장소로는 S3 호환 저장소, GCS, Azure Blob, Artifactory 등을 구성할 수 있다.

```text
이전 Task의 파일
  → Executor가 Artifact Repository에 업로드
  → 다음 Task의 Executor가 다운로드
  → 지정한 컨테이너 path에 배치
```

## 출력 아티팩트 선언

```yaml
- name: generate-report
  container:
    image: busybox:1.36
    command: [sh, -c]
    args: ["mkdir -p /tmp/out; echo report > /tmp/out/report.txt"]
  outputs:
    artifacts:
      - name: report
        path: /tmp/out
```

- `name`: 다른 템플릿이 참조할 논리 이름
- `path`: 사용자 컨테이너에서 수집할 파일 또는 디렉터리
- 디렉터리는 기본적으로 압축 후 업로드될 수 있음
- 저장 위치를 생략하면 Controller에 설정된 기본 Artifact Repository 사용

## 입력 아티팩트 연결

```yaml
- name: consume-report
  inputs:
    artifacts:
      - name: report
        path: /work/report
  container:
    image: busybox:1.36
    command: [sh, -c]
    args: ["cat /work/report/report.txt"]
```

```yaml
- name: consume
  template: consume-report
  arguments:
    artifacts:
      - name: report
        from: "{{steps.generate.outputs.artifacts.report}}"
```

호출되는 템플릿은 `inputs.artifacts`에서 받을 아티팩트의 이름과 배치 경로를 선언한다. 호출자는 이 이름에 맞춰 `arguments.artifacts`로 실제 아티팩트를 전달한다.

이전 작업의 출력을 참조할 때는 실행 구조에 따라 경로가 달라진다. Steps에서는 `steps.<STEP>.outputs.artifacts.<NAME>`을, DAG에서는 `tasks.<TASK>.outputs.artifacts.<NAME>`을 사용한다.

## 전체 예제

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  generateName: artifact-example-
spec:
  entrypoint: pipeline
  templates:
    - name: pipeline
      steps:
        - - name: generate
            template: generate
        - - name: consume
            template: consume
            arguments:
              artifacts:
                - name: message
                  from: "{{steps.generate.outputs.artifacts.message}}"

    - name: generate
      container:
        image: busybox:1.36
        command: [sh, -c]
        args: ["echo hello > /tmp/message.txt"]
      outputs:
        artifacts:
          - name: message
            path: /tmp/message.txt

    - name: consume
      inputs:
        artifacts:
          - name: message
            path: /tmp/message.txt
      container:
        image: busybox:1.36
        command: [cat]
        args: [/tmp/message.txt]
```

## 직접 저장 위치 지정

```yaml
outputs:
  artifacts:
    - name: report
      path: /tmp/report.json
      s3:
        endpoint: object-storage.example.com
        bucket: workflow-artifacts
        key: "reports/{{workflow.uid}}/report.json"
```

아티팩트에 저장소별 필드를 직접 지정할 수 있다. 이때 정적 자격 증명은 YAML에 넣지 않고 Secret으로 참조한다. 실행마다 같은 경로에 덮어쓰는 일을 막으려면 예제처럼 `workflow.uid` 등 실행별 고유 값을 key에 포함한다.

여러 아티팩트에서 저장소 구성이 반복된다면 기본 Artifact Repository를 두고 key만 결정하는 편이 단순하다.

## Artifact Repository 우선순위

- Workflow의 `artifactRepositoryRef`
- Workflow 네임스페이스의 기본 저장소 설정
- Controller 전역 기본 저장소 설정
- 명시적 저장소와 기본 저장소의 적용 범위는 설치 구성에 따라 확인 필요

```yaml
spec:
  artifactRepositoryRef:
    configMap: artifact-repositories
    key: tenant-a
```

- 팀, 테넌트별 저장소를 선택할 때 유용함
- 참조하는 ConfigMap과 key가 Workflow 네임스페이스에서 유효해야 함

## 선택적 아티팩트

```yaml
inputs:
  artifacts:
    - name: cache
      path: /cache
      optional: true
```

입력이 없어도 템플릿을 실행해야 한다면 `optional: true`를 사용한다. 조건 분기에 따라 출력이 생성되지 않을 수 있을 때 유용하지만, 애플리케이션도 파일이 없는 상황을 정상 경로로 처리해야 한다.

## 아카이브 설정

```yaml
outputs:
  artifacts:
    - name: binary
      path: /tmp/app
      archive:
        none: {}
```

- 기본 압축을 사용하지 않으려면 `archive.none` 지정
- tar 또는 zip 설정으로 압축 방식을 제어 가능함
- 이미 압축된 파일은 재압축 비용과 효과를 검토함

## 파라미터, 아티팩트, 볼륨 선택

- 파라미터
  - 작은 문자열, 숫자, JSON 설정 전달
  - 상태와 조건식에 바로 사용하기 쉬움
- 아티팩트
  - Task 사이에서 영속적으로 전달할 파일과 디렉터리
  - 서로 다른 노드에서 실행되는 Pod 사이의 데이터 전달에 적합함
- 볼륨
  - 같은 스토리지를 여러 Pod가 직접 공유해야 할 때 사용함
  - PVC 접근 모드, 가용 영역, 동시 쓰기 제약 확인 필요

## 실패와 정리

사용자 컨테이너가 성공했더라도 출력 업로드에 실패하면 노드가 Error 상태가 될 수 있다. 이때는 저장소 접근 권한, DNS, 인증서, 용량 문제를 애플리케이션 실패와 구분해야 한다.

- Workflow 삭제와 외부 아티팩트 삭제는 별도 수명 주기일 수 있음
- Artifact GC를 사용할 때 삭제 시점과 ServiceAccount 권한을 명시함
- 규정상 보존할 결과와 임시 중간 파일의 저장 경로, 보존 기간을 분리함

## 보안과 운영 지침

- 저장소 자격 증명은 Secret으로 관리함
- Workflow별 접두사와 버킷 정책으로 접근 범위를 제한함
- 민감한 출력이 로그나 `outputs.result`에도 남지 않는지 확인함
- 무결성이 중요하면 checksum을 별도 출력으로 기록함
- 대용량 fan-out에서는 같은 입력을 반복 다운로드하는 비용을 고려함
- 아티팩트 key에 사용자 입력을 그대로 사용하면 경로 충돌이나 의도하지 않은 접근 가능함
