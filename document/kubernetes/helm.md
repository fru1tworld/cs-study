# Helm

> 공식 문서: https://helm.sh/docs/

## 개요

Helm은 Kubernetes 패키지 매니저입니다. 복잡한 Kubernetes 애플리케이션을 쉽게 정의, 설치, 업그레이드할 수 있습니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Helm 개념                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐                                               │
│  │    Chart    │  Kubernetes 리소스를 설명하는 파일 모음         │
│  │   (패키지)   │  → Deployment, Service, ConfigMap 등 포함     │
│  └─────────────┘                                               │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────┐                                               │
│  │  Repository │  Chart를 저장하고 공유하는 장소                 │
│  │   (저장소)   │  → HTTP 서버, OCI 레지스트리 등                │
│  └─────────────┘                                               │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────┐                                               │
│  │   Release   │  클러스터에서 실행 중인 Chart 인스턴스          │
│  │   (릴리스)   │  → 동일 Chart로 여러 Release 생성 가능         │
│  └─────────────┘                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 설치

### macOS

```bash
brew install helm
```

### Linux

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### Windows

```bash
choco install kubernetes-helm
```

---

## 기본 명령어

### Repository 관리

```bash
# 리포지토리 추가
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add stable https://charts.helm.sh/stable

# 리포지토리 목록
helm repo list

# 리포지토리 업데이트
helm repo update

# 리포지토리 제거
helm repo remove bitnami
```

### Chart 검색

```bash
# Artifact Hub에서 검색
helm search hub nginx

# 추가된 리포지토리에서 검색
helm search repo bitnami/nginx

# 모든 버전 표시
helm search repo bitnami/nginx --versions
```

### Chart 정보 확인

```bash
# Chart 정보
helm show chart bitnami/nginx

# 기본 values 확인
helm show values bitnami/nginx

# 모든 정보
helm show all bitnami/nginx
```

---

## Chart 설치/관리

### 설치

```bash
# 기본 설치
helm install my-release bitnami/nginx

# 네임스페이스 지정
helm install my-release bitnami/nginx -n my-namespace --create-namespace

# values 파일로 설정
helm install my-release bitnami/nginx -f values.yaml

# 명령어에서 값 설정
helm install my-release bitnami/nginx \
  --set service.type=LoadBalancer \
  --set replicaCount=3

# Dry-run (실제 설치 없이 확인)
helm install my-release bitnami/nginx --dry-run --debug

# 이름 자동 생성
helm install bitnami/nginx --generate-name
```

### 릴리스 관리

```bash
# 릴리스 목록
helm list
helm list -A  # 모든 네임스페이스

# 릴리스 상태
helm status my-release

# 릴리스 값 확인
helm get values my-release

# 모든 정보 (manifest, values 등)
helm get all my-release

# 설치된 manifest 확인
helm get manifest my-release
```

### 업그레이드

```bash
# 업그레이드
helm upgrade my-release bitnami/nginx

# 설치되어 있지 않으면 설치
helm upgrade --install my-release bitnami/nginx

# 값 변경하며 업그레이드
helm upgrade my-release bitnami/nginx -f new-values.yaml

# 값 추가
helm upgrade my-release bitnami/nginx --set replicaCount=5

# 이전 값 유지하며 일부만 변경
helm upgrade my-release bitnami/nginx --reuse-values --set image.tag=1.26
```

### 롤백

```bash
# 롤아웃 히스토리
helm history my-release

# 이전 버전으로 롤백
helm rollback my-release

# 특정 리비전으로 롤백
helm rollback my-release 2

# 롤백 전 확인
helm rollback my-release 2 --dry-run
```

### 삭제

```bash
# 릴리스 삭제
helm uninstall my-release

# 히스토리 유지하며 삭제
helm uninstall my-release --keep-history

# 네임스페이스 지정
helm uninstall my-release -n my-namespace
```

---

## Chart 구조

```
mychart/
├── Chart.yaml          # Chart 메타데이터 (필수)
├── Chart.lock          # 의존성 잠금 파일
├── values.yaml         # 기본 설정 값
├── values.schema.json  # values 검증 스키마
├── charts/             # 의존성 Chart
├── crds/               # Custom Resource Definitions
├── templates/          # Kubernetes manifest 템플릿
│   ├── NOTES.txt       # 설치 후 메시지
│   ├── _helpers.tpl    # 템플릿 헬퍼 함수
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── ingress.yaml
│   └── tests/
│       └── test-connection.yaml
└── README.md           # Chart 설명서
```

### Chart.yaml

```yaml
apiVersion: v2
name: mychart
description: A Helm chart for Kubernetes
type: application  # application 또는 library
version: 0.1.0     # Chart 버전 (SemVer)
appVersion: "1.0.0"  # 애플리케이션 버전
kubeVersion: ">=1.25.0"  # 지원 Kubernetes 버전

# 유지보수자
maintainers:
- name: John Doe
  email: john@example.com
  url: https://example.com

# 의존성
dependencies:
- name: redis
  version: "17.x.x"
  repository: https://charts.bitnami.com/bitnami
  condition: redis.enabled
  tags:
  - cache

# 키워드
keywords:
- web
- nginx

# 홈페이지/소스
home: https://example.com
sources:
- https://github.com/example/mychart

# 폐기 여부
deprecated: false
```

### values.yaml

```yaml
# Replica 수
replicaCount: 1

# 이미지 설정
image:
  repository: nginx
  tag: "1.25"
  pullPolicy: IfNotPresent

# 이미지 Pull Secrets
imagePullSecrets: []

# 이름 오버라이드
nameOverride: ""
fullnameOverride: ""

# 서비스 계정
serviceAccount:
  create: true
  name: ""
  annotations: {}

# Pod 설정
podAnnotations: {}
podSecurityContext: {}
securityContext: {}

# 서비스 설정
service:
  type: ClusterIP
  port: 80

# Ingress 설정
ingress:
  enabled: false
  className: ""
  annotations: {}
  hosts:
  - host: chart-example.local
    paths:
    - path: /
      pathType: ImplementationSpecific
  tls: []

# 리소스
resources:
  limits:
    cpu: 100m
    memory: 128Mi
  requests:
    cpu: 100m
    memory: 128Mi

# 오토스케일링
autoscaling:
  enabled: false
  minReplicas: 1
  maxReplicas: 100
  targetCPUUtilizationPercentage: 80

# 노드 선택
nodeSelector: {}
tolerations: []
affinity: {}
```

---

## 템플릿

### 기본 문법

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "mychart.fullname" . }}
  labels:
    {{- include "mychart.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "mychart.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "mychart.selectorLabels" . | nindent 8 }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - containerPort: 80
        {{- if .Values.resources }}
        resources:
          {{- toYaml .Values.resources | nindent 12 }}
        {{- end }}
```

### 내장 객체

| 객체 | 설명 |
|------|------|
| .Release.Name | 릴리스 이름 |
| .Release.Namespace | 네임스페이스 |
| .Release.IsUpgrade | 업그레이드 여부 |
| .Release.IsInstall | 설치 여부 |
| .Release.Revision | 리비전 번호 |
| .Chart | Chart.yaml 내용 |
| .Values | values.yaml + 사용자 값 |
| .Files | 차트 내 파일 접근 |
| .Capabilities | Kubernetes 버전/API 정보 |
| .Template | 현재 템플릿 정보 |

### 조건문

```yaml
{{- if .Values.ingress.enabled }}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "mychart.fullname" . }}
spec:
  ...
{{- end }}

# if-else
{{- if .Values.service.type eq "LoadBalancer" }}
  type: LoadBalancer
{{- else }}
  type: ClusterIP
{{- end }}
```

### 반복문

```yaml
# range
{{- range .Values.ingress.hosts }}
- host: {{ .host | quote }}
  http:
    paths:
    {{- range .paths }}
    - path: {{ .path }}
      pathType: {{ .pathType }}
    {{- end }}
{{- end }}

# with (스코프 변경)
{{- with .Values.nodeSelector }}
nodeSelector:
  {{- toYaml . | nindent 8 }}
{{- end }}
```

### 헬퍼 함수 (_helpers.tpl)

```yaml
# templates/_helpers.tpl

{{/*
전체 이름 생성
*/}}
{{- define "mychart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
공통 레이블
*/}}
{{- define "mychart.labels" -}}
helm.sh/chart: {{ include "mychart.chart" . }}
{{ include "mychart.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
셀렉터 레이블
*/}}
{{- define "mychart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "mychart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
```

### 주요 함수

| 함수 | 설명 | 예시 |
|------|------|------|
| quote | 문자열 인용 | {{ .Values.name \| quote }} |
| default | 기본값 | {{ default "nginx" .Values.name }} |
| nindent | 들여쓰기 | {{ toYaml . \| nindent 4 }} |
| toYaml | YAML 변환 | {{ toYaml .Values.labels }} |
| trunc | 문자열 자르기 | {{ .Name \| trunc 63 }} |
| trimSuffix | 접미사 제거 | {{ .Name \| trimSuffix "-" }} |
| lower/upper | 대소문자 변환 | {{ .Values.name \| lower }} |
| required | 필수 값 검증 | {{ required "name is required" .Values.name }} |

---

## 의존성 관리

### Chart.yaml에 의존성 정의

```yaml
dependencies:
- name: postgresql
  version: "12.x.x"
  repository: https://charts.bitnami.com/bitnami
  condition: postgresql.enabled
  tags:
  - database

- name: redis
  version: "17.x.x"
  repository: "@bitnami"  # 등록된 repo 별칭
  alias: cache
```

### 의존성 명령어

```bash
# 의존성 다운로드
helm dependency update mychart/

# 의존성 목록
helm dependency list mychart/

# 의존성 빌드
helm dependency build mychart/
```

### 의존성 values 설정

```yaml
# values.yaml
postgresql:
  enabled: true
  auth:
    postgresPassword: secret
    database: mydb

redis:
  enabled: true
  architecture: standalone
```

---

## Chart 개발

### Chart 생성

```bash
# 새 Chart 생성
helm create mychart

# 구조
mychart/
├── Chart.yaml
├── values.yaml
├── charts/
├── templates/
│   ├── NOTES.txt
│   ├── _helpers.tpl
│   ├── deployment.yaml
│   ├── hpa.yaml
│   ├── ingress.yaml
│   ├── service.yaml
│   ├── serviceaccount.yaml
│   └── tests/
│       └── test-connection.yaml
└── .helmignore
```

### 린팅 및 테스트

```bash
# Chart 검증
helm lint mychart/

# 템플릿 렌더링 (설치 없이)
helm template my-release mychart/

# Dry-run
helm install my-release mychart/ --dry-run --debug

# 테스트 실행
helm test my-release
```

### 패키징 및 배포

```bash
# Chart 패키징
helm package mychart/
# → mychart-0.1.0.tgz

# 인덱스 생성
helm repo index .

# OCI 레지스트리에 푸시
helm push mychart-0.1.0.tgz oci://registry.example.com/charts
```

---

## Hooks

특정 릴리스 시점에 작업을 실행합니다.

### Hook 유형

| Hook | 설명 |
|------|------|
| pre-install | 설치 전 |
| post-install | 설치 후 |
| pre-upgrade | 업그레이드 전 |
| post-upgrade | 업그레이드 후 |
| pre-rollback | 롤백 전 |
| post-rollback | 롤백 후 |
| pre-delete | 삭제 전 |
| post-delete | 삭제 후 |
| test | 테스트 시 |

### Hook 예시

```yaml
# templates/post-install-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "mychart.fullname" . }}-migration
  annotations:
    "helm.sh/hook": post-install,post-upgrade
    "helm.sh/hook-weight": "-5"
    "helm.sh/hook-delete-policy": hook-succeeded
spec:
  template:
    spec:
      containers:
      - name: migration
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        command: ["./migrate.sh"]
      restartPolicy: Never
```

### Hook 삭제 정책

| 정책 | 설명 |
|------|------|
| hook-succeeded | 성공 시 삭제 |
| hook-failed | 실패 시 삭제 |
| before-hook-creation | 다음 Hook 실행 전 삭제 |

---

## 실전 패턴

### 환경별 values 파일

```bash
# 구조
values/
├── values.yaml          # 기본 값
├── values-dev.yaml      # 개발 환경
├── values-staging.yaml  # 스테이징
└── values-prod.yaml     # 프로덕션

# 사용
helm install my-app ./mychart \
  -f values/values.yaml \
  -f values/values-prod.yaml
```

### 시크릿 관리

```bash
# helm-secrets 플러그인
helm plugin install https://github.com/jkroepke/helm-secrets

# 암호화
helm secrets enc values/secrets.yaml

# 복호화하여 설치
helm secrets install my-app ./mychart -f values/secrets.yaml
```

### CI/CD 통합

```yaml
# GitLab CI 예시
deploy:
  stage: deploy
  script:
  - helm upgrade --install my-app ./charts/myapp
    --namespace production
    --set image.tag=${CI_COMMIT_SHA}
    -f values/values-prod.yaml
    --atomic
    --timeout 5m
```

---

## 유용한 옵션

| 옵션 | 설명 |
|------|------|
| --atomic | 실패 시 자동 롤백 |
| --wait | 모든 리소스 Ready까지 대기 |
| --timeout | 대기 시간 제한 |
| --debug | 디버그 출력 |
| --dry-run | 실제 설치 없이 확인 |
| --force | 리소스 강제 교체 |
| --cleanup-on-fail | 실패 시 리소스 정리 |

```bash
# 프로덕션 권장 설치
helm upgrade --install my-app ./mychart \
  --namespace production \
  --create-namespace \
  --atomic \
  --wait \
  --timeout 10m \
  -f values-prod.yaml
```

---

## 참고 자료

- [Helm Documentation](https://helm.sh/docs/)
- [Chart Best Practices](https://helm.sh/docs/chart_best_practices/)
- [Template Function List](https://helm.sh/docs/chart_template_guide/function_list/)
- [Artifact Hub](https://artifacthub.io/)
