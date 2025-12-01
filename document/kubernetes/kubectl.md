# kubectl 명령어 레퍼런스

> 공식 문서: https://kubernetes.io/docs/reference/kubectl/

## 개요

kubectl은 Kubernetes 클러스터를 관리하기 위한 커맨드라인 도구입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                     kubectl 명령어 구조                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  kubectl [command] [TYPE] [NAME] [flags]                       │
│                                                                 │
│  • command: 수행할 작업 (get, create, delete 등)                │
│  • TYPE: 리소스 유형 (pod, deployment, service 등)              │
│  • NAME: 리소스 이름 (선택적)                                   │
│  • flags: 옵션 플래그 (-o, -n, -l 등)                           │
│                                                                 │
│  예시: kubectl get pods my-pod -n production -o yaml           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 클러스터 정보

```bash
# 클러스터 정보
kubectl cluster-info

# 클러스터 상태
kubectl get componentstatuses

# API 버전 확인
kubectl api-versions

# API 리소스 목록
kubectl api-resources

# 특정 API 리소스 정보
kubectl explain pods
kubectl explain pods.spec.containers
```

---

## 리소스 조회 (get)

### 기본 조회

```bash
# Pod 목록
kubectl get pods
kubectl get po        # 축약형

# 모든 네임스페이스
kubectl get pods -A
kubectl get pods --all-namespaces

# 특정 네임스페이스
kubectl get pods -n kube-system

# 여러 리소스 유형
kubectl get pods,services,deployments

# 모든 리소스
kubectl get all
```

### 출력 형식

```bash
# 와이드 출력 (추가 정보)
kubectl get pods -o wide

# YAML 출력
kubectl get pod my-pod -o yaml

# JSON 출력
kubectl get pod my-pod -o json

# 커스텀 컬럼
kubectl get pods -o custom-columns=NAME:.metadata.name,STATUS:.status.phase

# jsonpath
kubectl get pods -o jsonpath='{.items[*].metadata.name}'

# 이름만 출력
kubectl get pods -o name

# 정렬
kubectl get pods --sort-by=.metadata.creationTimestamp
kubectl get pods --sort-by=.status.containerStatuses[0].restartCount
```

### 레이블/셀렉터

```bash
# 레이블 표시
kubectl get pods --show-labels

# 레이블 셀렉터
kubectl get pods -l app=nginx
kubectl get pods -l 'app in (nginx, apache)'
kubectl get pods -l app!=nginx
kubectl get pods -l 'environment,!tier'

# 레이블 컬럼 추가
kubectl get pods -L app,version
```

### 필드 셀렉터

```bash
# 상태로 필터링
kubectl get pods --field-selector status.phase=Running

# 노드로 필터링
kubectl get pods --field-selector spec.nodeName=node1

# 여러 조건
kubectl get pods --field-selector status.phase!=Running,spec.restartPolicy=Always
```

### 실시간 모니터링

```bash
# 변경 사항 감시
kubectl get pods -w
kubectl get pods --watch

# 상태 변화 추적
kubectl get events -w
```

---

## 리소스 상세 정보 (describe)

```bash
# Pod 상세 정보
kubectl describe pod my-pod

# 이벤트 포함된 상세 정보
kubectl describe deployment my-deployment

# 노드 상세 정보
kubectl describe node node1

# 여러 리소스
kubectl describe pods,services
```

---

## 리소스 생성/적용 (create, apply)

### create

```bash
# YAML 파일로 생성
kubectl create -f deployment.yaml

# 여러 파일
kubectl create -f file1.yaml -f file2.yaml

# 디렉토리 내 모든 파일
kubectl create -f ./manifests/

# URL에서 생성
kubectl create -f https://example.com/manifest.yaml

# 명령어로 직접 생성
kubectl create deployment nginx --image=nginx:1.25

# ConfigMap 생성
kubectl create configmap my-config --from-literal=key1=value1

# Secret 생성
kubectl create secret generic my-secret --from-literal=password=secret123

# 네임스페이스 생성
kubectl create namespace my-namespace
```

### apply (선언적)

```bash
# 리소스 적용 (없으면 생성, 있으면 업데이트)
kubectl apply -f deployment.yaml

# 디렉토리 재귀 적용
kubectl apply -f ./manifests/ -R

# Dry-run
kubectl apply -f deployment.yaml --dry-run=client
kubectl apply -f deployment.yaml --dry-run=server

# 차이점 확인
kubectl diff -f deployment.yaml

# 강제 적용 (삭제 후 재생성)
kubectl apply -f deployment.yaml --force
```

---

## 리소스 수정 (edit, patch, set)

### edit

```bash
# 리소스 편집 (기본 에디터)
kubectl edit deployment my-deployment

# 에디터 지정
KUBE_EDITOR="vim" kubectl edit deployment my-deployment
```

### patch

```bash
# JSON 패치
kubectl patch deployment my-deployment -p '{"spec":{"replicas":3}}'

# YAML 패치
kubectl patch deployment my-deployment --patch-file patch.yaml

# 전략적 병합 패치
kubectl patch deployment my-deployment --type=strategic -p '{"spec":{"template":{"spec":{"containers":[{"name":"nginx","image":"nginx:1.26"}]}}}}'

# JSON 머지 패치
kubectl patch deployment my-deployment --type=merge -p '{"spec":{"replicas":3}}'
```

### set

```bash
# 이미지 변경
kubectl set image deployment/my-deployment nginx=nginx:1.26

# 환경 변수 설정
kubectl set env deployment/my-deployment DATABASE_URL=postgres://localhost

# 리소스 설정
kubectl set resources deployment/my-deployment -c=nginx --limits=cpu=200m,memory=512Mi

# 셀렉터 설정
kubectl set selector service my-service 'app=nginx'
```

---

## 리소스 삭제 (delete)

```bash
# 리소스 삭제
kubectl delete pod my-pod

# YAML로 삭제
kubectl delete -f deployment.yaml

# 레이블로 삭제
kubectl delete pods -l app=nginx

# 모든 리소스 삭제 (네임스페이스 내)
kubectl delete all --all -n my-namespace

# 강제 삭제 (즉시)
kubectl delete pod my-pod --force --grace-period=0

# 삭제 대기
kubectl delete pod my-pod --wait

# Finalizer 제거 후 삭제
kubectl patch pod my-pod -p '{"metadata":{"finalizers":null}}'
kubectl delete pod my-pod
```

---

## 스케일링 (scale)

```bash
# Deployment 스케일
kubectl scale deployment my-deployment --replicas=5

# 조건부 스케일
kubectl scale deployment my-deployment --replicas=3 --current-replicas=5

# ReplicaSet 스케일
kubectl scale replicaset my-rs --replicas=3

# StatefulSet 스케일
kubectl scale statefulset my-sts --replicas=5
```

---

## 롤아웃 관리 (rollout)

```bash
# 롤아웃 상태
kubectl rollout status deployment/my-deployment

# 롤아웃 히스토리
kubectl rollout history deployment/my-deployment

# 특정 리비전 상세
kubectl rollout history deployment/my-deployment --revision=2

# 이전 버전으로 롤백
kubectl rollout undo deployment/my-deployment

# 특정 리비전으로 롤백
kubectl rollout undo deployment/my-deployment --to-revision=2

# 롤아웃 일시 중지
kubectl rollout pause deployment/my-deployment

# 롤아웃 재개
kubectl rollout resume deployment/my-deployment

# 롤아웃 재시작
kubectl rollout restart deployment/my-deployment
```

---

## 로그 확인 (logs)

```bash
# Pod 로그
kubectl logs my-pod

# 특정 컨테이너
kubectl logs my-pod -c my-container

# 실시간 로그
kubectl logs my-pod -f
kubectl logs my-pod --follow

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs my-pod --previous

# 최근 N줄
kubectl logs my-pod --tail=100

# 시간 범위
kubectl logs my-pod --since=1h
kubectl logs my-pod --since-time=2024-01-01T00:00:00Z

# 타임스탬프 포함
kubectl logs my-pod --timestamps

# 여러 Pod (레이블 셀렉터)
kubectl logs -l app=nginx

# 모든 컨테이너
kubectl logs my-pod --all-containers
```

---

## 컨테이너 접속 (exec)

```bash
# 명령어 실행
kubectl exec my-pod -- ls /app

# 인터랙티브 쉘
kubectl exec -it my-pod -- /bin/bash
kubectl exec -it my-pod -- /bin/sh

# 특정 컨테이너
kubectl exec -it my-pod -c my-container -- /bin/bash

# 환경 변수 포함
kubectl exec my-pod -- env
```

---

## 포트 포워딩 (port-forward)

```bash
# Pod 포트 포워딩
kubectl port-forward pod/my-pod 8080:80

# Service 포트 포워딩
kubectl port-forward svc/my-service 8080:80

# 모든 인터페이스에서 접근
kubectl port-forward pod/my-pod --address 0.0.0.0 8080:80

# 백그라운드 실행
kubectl port-forward pod/my-pod 8080:80 &
```

---

## 파일 복사 (cp)

```bash
# 컨테이너에서 로컬로
kubectl cp my-pod:/app/config.yaml ./config.yaml

# 로컬에서 컨테이너로
kubectl cp ./config.yaml my-pod:/app/config.yaml

# 특정 컨테이너
kubectl cp my-pod:/app/logs ./logs -c my-container

# 네임스페이스 지정
kubectl cp production/my-pod:/data ./backup
```

---

## 디버깅

### 이벤트 확인

```bash
# 네임스페이스 이벤트
kubectl get events

# 정렬하여 확인
kubectl get events --sort-by=.metadata.creationTimestamp

# 특정 리소스 이벤트
kubectl get events --field-selector involvedObject.name=my-pod
```

### 리소스 사용량

```bash
# 노드 리소스 사용량
kubectl top nodes

# Pod 리소스 사용량
kubectl top pods

# 컨테이너별
kubectl top pods --containers

# 정렬
kubectl top pods --sort-by=cpu
kubectl top pods --sort-by=memory
```

### 디버깅 컨테이너

```bash
# 디버깅 Pod 연결
kubectl debug my-pod -it --image=busybox

# 노드 디버깅
kubectl debug node/my-node -it --image=ubuntu

# 복사본으로 디버깅
kubectl debug my-pod -it --copy-to=my-pod-debug --container=debugger --image=busybox
```

### 클러스터 문제 해결

```bash
# 인증 테스트
kubectl auth can-i create pods
kubectl auth can-i '*' '*'  # 모든 권한

# 특정 사용자로 테스트
kubectl auth can-i list pods --as=jane
kubectl auth can-i list pods --as=system:serviceaccount:default:my-sa

# 모든 권한 확인
kubectl auth can-i --list
kubectl auth can-i --list --as=jane
```

---

## 컨텍스트 및 설정

### 컨텍스트 관리

```bash
# 컨텍스트 목록
kubectl config get-contexts

# 현재 컨텍스트
kubectl config current-context

# 컨텍스트 전환
kubectl config use-context my-cluster

# 기본 네임스페이스 설정
kubectl config set-context --current --namespace=production

# 컨텍스트 생성
kubectl config set-context my-context --cluster=my-cluster --user=my-user --namespace=default
```

### 클러스터 관리

```bash
# 클러스터 목록
kubectl config get-clusters

# 클러스터 정보
kubectl config view

# 클러스터 삭제
kubectl config delete-cluster my-cluster

# 사용자 삭제
kubectl config delete-user my-user
```

---

## 네임스페이스

```bash
# 네임스페이스 목록
kubectl get namespaces

# 네임스페이스 생성
kubectl create namespace my-namespace

# 네임스페이스 삭제
kubectl delete namespace my-namespace

# 특정 네임스페이스 리소스 조회
kubectl get all -n my-namespace
```

---

## 레이블/어노테이션

### 레이블 관리

```bash
# 레이블 추가
kubectl label pods my-pod environment=production

# 레이블 덮어쓰기
kubectl label pods my-pod environment=staging --overwrite

# 레이블 제거
kubectl label pods my-pod environment-

# 여러 리소스에 레이블 추가
kubectl label pods -l app=nginx tier=frontend
```

### 어노테이션 관리

```bash
# 어노테이션 추가
kubectl annotate pods my-pod description='My pod description'

# 어노테이션 덮어쓰기
kubectl annotate pods my-pod description='New description' --overwrite

# 어노테이션 제거
kubectl annotate pods my-pod description-
```

---

## Taint/Toleration

```bash
# Taint 추가
kubectl taint nodes node1 key=value:NoSchedule

# Taint 제거
kubectl taint nodes node1 key=value:NoSchedule-

# Taint 확인
kubectl describe node node1 | grep Taint
```

---

## 자주 사용하는 별칭

```bash
# ~/.bashrc 또는 ~/.zshrc에 추가

# 기본 별칭
alias k='kubectl'
alias kg='kubectl get'
alias kd='kubectl describe'
alias kl='kubectl logs'
alias ke='kubectl exec -it'
alias ka='kubectl apply -f'
alias kdel='kubectl delete'

# 네임스페이스
alias kgp='kubectl get pods'
alias kgpa='kubectl get pods -A'
alias kgs='kubectl get services'
alias kgd='kubectl get deployments'
alias kgn='kubectl get nodes'

# 빠른 접근
alias kctx='kubectl config use-context'
alias kns='kubectl config set-context --current --namespace'
```

---

## 유용한 플러그인

### krew (플러그인 관리자)

```bash
# krew 설치
# https://krew.sigs.k8s.io/docs/user-guide/setup/install/

# 플러그인 검색
kubectl krew search

# 플러그인 설치
kubectl krew install ctx
kubectl krew install ns

# 설치된 플러그인 목록
kubectl krew list
```

### 유용한 플러그인들

| 플러그인 | 설명 |
|----------|------|
| ctx | 컨텍스트 빠른 전환 |
| ns | 네임스페이스 빠른 전환 |
| tail | 여러 Pod 로그 스트리밍 |
| neat | YAML 출력 정리 |
| tree | 리소스 트리 표시 |
| images | 클러스터 이미지 목록 |

---

## 참고 자료

- [kubectl Reference](https://kubernetes.io/docs/reference/kubectl/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Krew Plugins](https://krew.sigs.k8s.io/plugins/)
