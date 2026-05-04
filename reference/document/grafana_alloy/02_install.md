# Alloy 설치

> 이 문서는 Grafana Alloy 공식 문서의 "Install Alloy" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/set-up/install/

---

## 목차

1. [설치 방법 개요](#설치-방법-개요)
2. [Linux 설치](#linux-설치)
3. [macOS 설치](#macos-설치)
4. [Windows 설치](#windows-설치)
5. [Docker 설치](#docker-설치)
6. [Kubernetes 설치 (Helm)](#kubernetes-설치-helm)
7. [Ansible/Chef/Puppet](#ansiblechefpuppet)
8. [업그레이드](#업그레이드)
9. [제거](#제거)

---

## 설치 방법 개요

| 환경 | 권장 방법 |
|------|----------|
| Kubernetes | Helm |
| Linux 서버 | apt/yum 패키지 |
| macOS 개발 | Homebrew |
| Windows | MSI 설치 프로그램 |
| 컨테이너 | Docker 이미지 |

---

## Linux 설치

### Debian / Ubuntu (apt)

```bash
# Grafana 저장소 추가
sudo mkdir -p /etc/apt/keyrings/
wget -q -O - https://apt.grafana.com/gpg.key | gpg --dearmor | sudo tee /etc/apt/keyrings/grafana.gpg > /dev/null
echo "deb [signed-by=/etc/apt/keyrings/grafana.gpg] https://apt.grafana.com stable main" | sudo tee /etc/apt/sources.list.d/grafana.list

# 설치
sudo apt-get update
sudo apt-get install -y alloy

# 시작
sudo systemctl enable --now alloy
sudo systemctl status alloy
```

### RHEL / CentOS / Fedora (yum)

```bash
# 저장소 추가
sudo tee /etc/yum.repos.d/grafana.repo <<EOF
[grafana]
name=grafana
baseurl=https://rpm.grafana.com
repo_gpgcheck=1
enabled=1
gpgcheck=1
gpgkey=https://rpm.grafana.com/gpg.key
sslverify=1
sslcacert=/etc/pki/tls/certs/ca-bundle.crt
EOF

# 설치
sudo yum install -y alloy

# 시작
sudo systemctl enable --now alloy
```

### 설정 파일 위치

기본:
- 구성: `/etc/alloy/config.alloy`
- 데이터: `/var/lib/alloy/data`
- 로그: `journalctl -u alloy`

### systemd 서비스 환경 변수

```bash
sudo systemctl edit alloy
```

```ini
[Service]
Environment="CUSTOM_ARGS=--server.http.listen-addr=0.0.0.0:12345"
Environment="MIMIR_URL=https://mimir.example.com"
```

```bash
sudo systemctl restart alloy
```

---

## macOS 설치

### Homebrew

```bash
brew install grafana/grafana/alloy

# 시작
brew services start alloy
```

### 수동 다운로드

```bash
curl -O -L https://github.com/grafana/alloy/releases/latest/download/alloy-darwin-arm64.zip
unzip alloy-darwin-arm64.zip
chmod +x alloy-darwin-arm64
sudo mv alloy-darwin-arm64 /usr/local/bin/alloy
```

---

## Windows 설치

### MSI 설치 프로그램

1. [GitHub Releases](https://github.com/grafana/alloy/releases)에서 `alloy-installer-windows-amd64.exe.zip` 다운로드
2. 압축 해제 후 실행
3. 설치 마법사 따라 진행

### 서비스 관리

PowerShell:

```powershell
# 시작
Start-Service Alloy

# 중지
Stop-Service Alloy

# 상태
Get-Service Alloy

# 자동 시작
Set-Service Alloy -StartupType Automatic
```

### 구성 파일 위치

- 구성: `C:\Program Files\GrafanaLabs\Alloy\config.alloy`
- 로그: Windows Event Viewer

---

## Docker 설치

### 단일 컨테이너

```bash
docker run -d \
  --name alloy \
  -p 12345:12345 \
  -v $(pwd)/config.alloy:/etc/alloy/config.alloy \
  grafana/alloy:latest \
  run \
  --server.http.listen-addr=0.0.0.0:12345 \
  --storage.path=/var/lib/alloy/data \
  /etc/alloy/config.alloy
```

### Docker Compose

```yaml
version: "3.8"
services:
  alloy:
    image: grafana/alloy:latest
    container_name: alloy
    ports:
      - "12345:12345"   # UI / metrics
      - "4317:4317"     # OTLP gRPC
      - "4318:4318"     # OTLP HTTP
    volumes:
      - ./config.alloy:/etc/alloy/config.alloy
      - /var/log:/var/log:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    command:
      - run
      - --server.http.listen-addr=0.0.0.0:12345
      - --storage.path=/var/lib/alloy/data
      - /etc/alloy/config.alloy
    restart: unless-stopped
```

---

## Kubernetes 설치 (Helm)

### 차트 종류

| 차트 | 용도 |
|------|------|
| `alloy` | DaemonSet (호스트별 1개) - 일반 |
| `alloy-operator` | CRD 기반 관리 |

### 기본 설치

```bash
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

helm install alloy grafana/alloy \
  --namespace alloy \
  --create-namespace \
  --values values.yaml
```

### values.yaml 예시

```yaml
alloy:
  configMap:
    create: true
    content: |
      logging {
        level  = "info"
        format = "logfmt"
      }
      
      discovery.kubernetes "pods" {
        role = "pod"
      }
      
      loki.source.kubernetes "pods" {
        targets    = discovery.kubernetes.pods.targets
        forward_to = [loki.write.default.receiver]
      }
      
      loki.write "default" {
        endpoint {
          url = "http://loki:3100/loki/api/v1/push"
        }
      }
  
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      memory: 512Mi
  
  enableReporting: false
  
  uiPathPrefix: /

# DaemonSet (호스트당 1개) 또는 Deployment 선택
controller:
  type: daemonset
  # type: deployment
  # replicas: 3

# 권한 (Pod 디스커버리 등)
rbac:
  create: true

serviceAccount:
  create: true

# Pod별 볼륨 마운트
configReloader:
  enabled: true

extraVolumes:
  - name: pod-logs
    hostPath:
      path: /var/log/pods

extraVolumeMounts:
  - name: pod-logs
    mountPath: /var/log/pods
    readOnly: true
```

### Operator 사용

CRD 설치:

```bash
helm install alloy-operator grafana/alloy-operator \
  --namespace alloy-system \
  --create-namespace
```

CR 생성:

```yaml
apiVersion: collector.grafana.com/v1alpha1
kind: Alloy
metadata:
  name: prod
  namespace: monitoring
spec:
  controllerKind: deployment
  replicas: 3
  
  config: |
    prometheus.scrape "kubernetes" {
      targets    = discovery.kubernetes.pods.targets
      forward_to = [prometheus.remote_write.mimir.receiver]
    }
    
    prometheus.remote_write "mimir" {
      endpoint {
        url = "http://mimir:9009/api/v1/push"
      }
    }
```

---

## Ansible/Chef/Puppet

### Ansible Role

```yaml
- hosts: monitoring
  roles:
    - role: grafana.grafana.alloy
      vars:
        alloy_user: alloy
        alloy_config_file: alloy/config.alloy.j2
        alloy_env_file_vars:
          MIMIR_URL: "https://mimir.example.com"
```

### Puppet Module

`grafana/alloy` 모듈 사용:

```puppet
class { 'alloy':
  config_content => template('mymodule/alloy/config.alloy.epp'),
  service_ensure => 'running',
  service_enable => true,
}
```

---

## 업그레이드

### 패키지 (apt/yum)

```bash
# 최신 버전 확인
apt list --upgradable | grep alloy

# 업그레이드
sudo apt-get update && sudo apt-get install -y alloy

# 재시작
sudo systemctl restart alloy
```

### Helm

```bash
helm repo update
helm upgrade alloy grafana/alloy \
  --namespace alloy \
  --values values.yaml \
  --version 0.5.0
```

### Docker

```bash
docker pull grafana/alloy:latest
docker stop alloy && docker rm alloy
docker run -d ... grafana/alloy:latest ...
```

### 호환성

- 패치 버전: 안전
- 마이너 버전: 릴리스 노트 확인
- 메이저 버전: Breaking changes 가능

---

## 제거

### Linux (apt)

```bash
sudo systemctl stop alloy
sudo systemctl disable alloy
sudo apt-get remove --purge alloy
sudo rm -rf /var/lib/alloy /etc/alloy
```

### Linux (yum)

```bash
sudo systemctl stop alloy
sudo yum remove -y alloy
sudo rm -rf /var/lib/alloy /etc/alloy
```

### macOS (Homebrew)

```bash
brew services stop alloy
brew uninstall alloy
```

### Windows

제어판 → 프로그램 추가/제거 → "Grafana Alloy" 제거.

### Helm

```bash
helm uninstall alloy --namespace alloy
kubectl delete namespace alloy
```
