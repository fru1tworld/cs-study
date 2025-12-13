# Kubernetes 스토리지

> 공식 문서: https://kubernetes.io/docs/concepts/storage/

## 개요

Kubernetes 스토리지 시스템은 컨테이너의 임시적인 파일 시스템 한계를 극복하고, Pod 간 데이터 공유 및 영구 저장을 가능하게 합니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    스토리지 계층 구조                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                     StorageClass                          │ │
│  │           (스토리지 유형 및 프로비저너 정의)                  │ │
│  └───────────────────────────────────────────────────────────┘ │
│                              │                                  │
│              ┌───────────────┴───────────────┐                 │
│              ▼                               ▼                 │
│  ┌─────────────────────┐      ┌─────────────────────────────┐ │
│  │  PersistentVolume   │      │   동적 프로비저닝            │ │
│  │   (정적 프로비저닝)   │◀────│   (자동 PV 생성)             │ │
│  └─────────────────────┘      └─────────────────────────────┘ │
│              │                                                  │
│              ▼                                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              PersistentVolumeClaim (PVC)                   │ │
│  │                   (스토리지 요청)                           │ │
│  └───────────────────────────────────────────────────────────┘ │
│              │                                                  │
│              ▼                                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                         Pod                                │ │
│  │                  (Volume으로 마운트)                        │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Volume

Volume은 Pod 내 컨테이너가 파일시스템을 통해 데이터에 접근하고 공유하는 방식입니다.

### Volume의 필요성

| 문제 | 해결 |
|------|------|
| 컨테이너 재시작 시 데이터 손실 | Volume으로 데이터 유지 |
| Pod 내 컨테이너 간 파일 공유 불가 | 공유 Volume 사용 |
| 설정 파일 주입 어려움 | ConfigMap/Secret Volume |

### 일반적인 Volume 유형

#### emptyDir

Pod 생성 시 빈 디렉토리 생성, Pod 삭제 시 데이터 삭제

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-emptydir
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: cache-volume
      mountPath: /cache
  - name: sidecar
    image: sidecar:1.0
    volumeMounts:
    - name: cache-volume
      mountPath: /data
  volumes:
  - name: cache-volume
    emptyDir:
      sizeLimit: 500Mi
      # medium: Memory  # tmpfs 사용 (메모리 기반)
```

** 사용 사례:**
- 임시 캐시
- 컨테이너 간 데이터 공유
- 체크포인트 저장

#### hostPath

노드의 파일시스템을 Pod에 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-hostpath
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: host-logs
      mountPath: /var/log/host
  volumes:
  - name: host-logs
    hostPath:
      path: /var/log
      type: Directory
```

**hostPath type:**

| Type | 설명 |
|------|------|
| "" | 검증 없음 |
| DirectoryOrCreate | 디렉토리 없으면 생성 |
| Directory | 디렉토리 존재 필수 |
| FileOrCreate | 파일 없으면 생성 |
| File | 파일 존재 필수 |
| Socket | Unix 소켓 존재 필수 |
| CharDevice | 문자 디바이스 존재 필수 |
| BlockDevice | 블록 디바이스 존재 필수 |

** 주의:** hostPath는 보안 위험이 있으므로 프로덕션에서 사용 자제

#### configMap

ConfigMap 데이터를 파일로 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-configmap
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: config
      mountPath: /etc/config
  volumes:
  - name: config
    configMap:
      name: my-config
      items:
      - key: config.yaml
        path: app-config.yaml
      defaultMode: 0644
```

#### secret

Secret 데이터를 파일로 마운트

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-secret
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: secrets
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secrets
    secret:
      secretName: my-secret
      defaultMode: 0400
```

#### persistentVolumeClaim

PVC를 통해 영구 스토리지 사용

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-pvc
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: data
      mountPath: /data
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: my-pvc
```

#### downwardAPI

Pod/컨테이너 정보를 파일로 노출

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-downward-api
  labels:
    zone: us-east-1
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: podinfo
      mountPath: /etc/podinfo
  volumes:
  - name: podinfo
    downwardAPI:
      items:
      - path: "labels"
        fieldRef:
          fieldPath: metadata.labels
      - path: "cpu_limit"
        resourceFieldRef:
          containerName: app
          resource: limits.cpu
```

#### projected

여러 Volume 소스를 하나의 디렉토리에 통합

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod-with-projected
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: all-in-one
      mountPath: /etc/all-in-one
  volumes:
  - name: all-in-one
    projected:
      sources:
      - secret:
          name: my-secret
      - configMap:
          name: my-config
      - downwardAPI:
          items:
          - path: "labels"
            fieldRef:
              fieldPath: metadata.labels
```

---

## PersistentVolume (PV)

클러스터 수준의 스토리지 리소스입니다. 관리자가 프로비저닝하거나 StorageClass를 통해 동적으로 생성됩니다.

### PV 정의

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: my-pv
  labels:
    type: local
spec:
  capacity:
    storage: 10Gi
  volumeMode: Filesystem
  accessModes:
  - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  mountOptions:
  - hard
  - nfsvers=4.1
  # 스토리지 백엔드 (예: NFS)
  nfs:
    server: nfs-server.example.com
    path: /exported/path
```

### 접근 모드 (Access Modes)

| 모드 | 약어 | 설명 |
|------|------|------|
| ReadWriteOnce | RWO | 단일 노드에서 읽기/쓰기 |
| ReadOnlyMany | ROX | 다중 노드에서 읽기 전용 |
| ReadWriteMany | RWX | 다중 노드에서 읽기/쓰기 |
| ReadWriteOncePod | RWOP | 단일 Pod에서만 읽기/쓰기 |

### 회수 정책 (Reclaim Policy)

| 정책 | 설명 |
|------|------|
| Retain | PVC 삭제 후에도 PV와 데이터 유지 |
| Delete | PVC 삭제 시 PV와 외부 스토리지도 삭제 |
| Recycle | 기본 삭제 후 재사용 (폐지됨) |

### PV 상태

```
Available → Bound → Released → (Available 또는 삭제)
```

| 상태 | 설명 |
|------|------|
| Available | 아직 바인딩되지 않은 사용 가능한 상태 |
| Bound | PVC에 바인딩된 상태 |
| Released | PVC가 삭제되었지만 아직 회수되지 않음 |
| Failed | 자동 회수 실패 |

---

## PersistentVolumeClaim (PVC)

사용자의 스토리지 요청입니다.

### PVC 정의

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  accessModes:
  - ReadWriteOnce
  volumeMode: Filesystem
  resources:
    requests:
      storage: 5Gi
  storageClassName: standard
  selector:
    matchLabels:
      type: local
```

### PVC와 PV 바인딩 과정

```
1. PVC 생성
    │
    ▼
2. Control Plane이 일치하는 PV 검색
   (용량, 접근 모드, StorageClass, 셀렉터)
    │
    ▼
3. 일치하는 PV 발견?
   ├── Yes → PV-PVC 바인딩
   └── No  → 동적 프로비저닝 시도 (StorageClass 있는 경우)
              또는 Pending 상태 유지
```

### PVC 사용

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: data
      mountPath: /data
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: my-pvc
```

### 볼륨 확장

StorageClass에서 `allowVolumeExpansion: true`인 경우 PVC 크기 확장 가능

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  resources:
    requests:
      storage: 10Gi  # 5Gi → 10Gi로 변경
```

** 주의:** 축소는 불가능합니다.

---

## StorageClass

스토리지의 "클래스"를 정의합니다. 관리자가 성능, 백업 정책 등 다양한 스토리지 프로파일을 제공할 수 있습니다.

### StorageClass 정의

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-storage
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp3
  iopsPerGB: "10"
  fsType: ext4
reclaimPolicy: Delete
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer
mountOptions:
- debug
```

### 주요 필드

| 필드 | 설명 |
|------|------|
| provisioner | 볼륨 프로비저너 (필수) |
| parameters | 프로비저너별 설정 |
| reclaimPolicy | Delete 또는 Retain |
| volumeBindingMode | Immediate 또는 WaitForFirstConsumer |
| allowVolumeExpansion | 볼륨 확장 허용 여부 |
| mountOptions | 마운트 옵션 |

### volumeBindingMode

| 모드 | 설명 |
|------|------|
| Immediate | PVC 생성 즉시 바인딩 (기본값) |
| WaitForFirstConsumer | Pod가 PVC를 사용할 때까지 바인딩 지연 |

**WaitForFirstConsumer 사용 이유:**
- 토폴로지 제약 고려 (Zone, Node)
- Pod가 스케줄될 노드에서 볼륨 프로비저닝

### 주요 Provisioner

#### AWS EBS

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: aws-ebs-gp3
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
volumeBindingMode: WaitForFirstConsumer
```

#### GCE Persistent Disk

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gce-pd-ssd
provisioner: pd.csi.storage.gke.io
parameters:
  type: pd-ssd
volumeBindingMode: WaitForFirstConsumer
```

#### Azure Disk

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: azure-disk-premium
provisioner: disk.csi.azure.com
parameters:
  skuName: Premium_LRS
volumeBindingMode: WaitForFirstConsumer
```

#### NFS (외부 프로비저너)

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: nfs-storage
provisioner: nfs.csi.k8s.io
parameters:
  server: nfs-server.example.com
  share: /exported/path
```

### 기본 StorageClass

```yaml
metadata:
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
```

PVC에서 `storageClassName`을 지정하지 않으면 기본 StorageClass 사용

---

## 동적 프로비저닝 vs 정적 프로비저닝

### 정적 프로비저닝

```
관리자가 PV 수동 생성 → 사용자가 PVC로 요청 → 매칭되면 바인딩
```

```yaml
# 1. 관리자: PV 생성
apiVersion: v1
kind: PersistentVolume
metadata:
  name: static-pv
spec:
  capacity:
    storage: 10Gi
  accessModes:
  - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: ""  # 빈 문자열 = 정적 프로비저닝
  hostPath:
    path: /mnt/data
---
# 2. 사용자: PVC 생성
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: static-pvc
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: ""  # 정적 PV만 매칭
```

### 동적 프로비저닝

```
StorageClass 정의 → 사용자가 PVC로 요청 → 자동으로 PV 생성 및 바인딩
```

```yaml
# 1. 관리자: StorageClass 생성
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast
provisioner: kubernetes.io/aws-ebs
parameters:
  type: gp3
---
# 2. 사용자: PVC 생성 (PV 자동 생성)
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: dynamic-pvc
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: fast
```

---

## StatefulSet과 스토리지

StatefulSet은 `volumeClaimTemplates`를 통해 각 Pod에 전용 PVC를 자동 생성합니다.

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: web
spec:
  serviceName: "nginx"
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.25
        volumeMounts:
        - name: www
          mountPath: /usr/share/nginx/html
  volumeClaimTemplates:
  - metadata:
      name: www
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast"
      resources:
        requests:
          storage: 1Gi
```

** 생성되는 PVC:**
- www-web-0
- www-web-1
- www-web-2

** 중요:** Pod 삭제 또는 StatefulSet 스케일 다운 시에도 PVC는 자동 삭제되지 않습니다.

---

## 스토리지 보호

### PVC Protection

사용 중인 PVC가 실수로 삭제되는 것을 방지합니다.

```yaml
# PVC에 자동으로 추가되는 finalizer
metadata:
  finalizers:
  - kubernetes.io/pvc-protection
```

### PV Protection

바인딩된 PV가 삭제되는 것을 방지합니다.

```yaml
metadata:
  finalizers:
  - kubernetes.io/pv-protection
```

---

## 스토리지 모범 사례

### 1. 적절한 접근 모드 선택

| 사용 사례 | 권장 모드 |
|----------|----------|
| 단일 Pod 데이터베이스 | ReadWriteOnce |
| 공유 설정 파일 | ReadOnlyMany |
| 공유 파일 스토리지 | ReadWriteMany |

### 2. 적절한 회수 정책

| 사용 사례 | 권장 정책 |
|----------|----------|
| 중요 데이터 | Retain |
| 임시/테스트 데이터 | Delete |

### 3. 토폴로지 고려

```yaml
volumeBindingMode: WaitForFirstConsumer
```

Zone별 스토리지 사용 시 Pod가 스케줄될 Zone에서 볼륨 생성

### 4. 리소스 요청

```yaml
resources:
  requests:
    storage: 10Gi  # 필요한 만큼만 요청
```

### 5. 레이블 활용

```yaml
# PV
metadata:
  labels:
    environment: production
    tier: database

# PVC selector
selector:
  matchLabels:
    environment: production
```

---

## 참고 자료

- [Volumes](https://kubernetes.io/docs/concepts/storage/volumes/)
- [Persistent Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)
- [Storage Classes](https://kubernetes.io/docs/concepts/storage/storage-classes/)
- [Dynamic Volume Provisioning](https://kubernetes.io/docs/concepts/storage/dynamic-provisioning/)
