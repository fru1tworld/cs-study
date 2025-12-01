# EC2 인스턴스 스토어

## 개요

인스턴스 스토어는 EC2 인스턴스의 호스트 컴퓨터에 물리적으로 연결된 임시 블록 스토리지입니다. 호스트 하드웨어에 직접 연결되어 있어 높은 I/O 성능을 제공합니다.

## 특징

| 특성 | 설명 |
|------|------|
| ** 지속성** | 임시 (인스턴스 중지/종료 시 데이터 손실) |
| ** 비용** | 인스턴스 비용에 포함 |
| ** 성능** | 매우 높음 (물리 디스크 직접 연결) |
| ** 가용성** | 인스턴스 타입에 따라 다름 |
| ** 암호화** | NVMe 인스턴스 스토어는 하드웨어 암호화 |

## 인스턴스 스토어 vs EBS

| 특성 | 인스턴스 스토어 | EBS |
|------|----------------|-----|
| 지속성 | 임시 | 영구 |
| 성능 | 매우 높음 | 높음 |
| 비용 | 인스턴스에 포함 | 별도 요금 |
| 스냅샷 | 불가 | 가능 |
| 인스턴스 중지 | 데이터 손실 | 데이터 유지 |
| 크기 변경 | 불가 | 가능 |
| 분리/연결 | 불가 | 가능 |

## 데이터 지속성

### 데이터 유지 상황

| 이벤트 | 데이터 상태 |
|--------|------------|
| 인스턴스 재부팅 | ** 유지** |
| OS 재시작 | ** 유지** |
| 기본 하드웨어 장애 | 손실 |

### 데이터 손실 상황

| 이벤트 | 데이터 상태 |
|--------|------------|
| 인스턴스 중지 | ** 손실** |
| 인스턴스 휴면화 | ** 손실** |
| 인스턴스 종료 | ** 손실** |
| 기본 디스크 드라이브 장애 | ** 손실** |

```
┌─────────────────────────────────────────────────────────────┐
│                인스턴스 스토어 데이터 수명                    │
│                                                              │
│  인스턴스 시작 ──▶ 실행 중 ──▶ 재부팅 ──▶ 실행 중           │
│      ↓              (데이터 유지)    (데이터 유지)          │
│  데이터 생성                                                 │
│                                                              │
│  인스턴스 시작 ──▶ 실행 중 ──▶ 중지 ──▶ 시작               │
│      ↓              │        │       │                     │
│  데이터 생성    데이터 존재   데이터 손실  새 인스턴스 스토어  │
└─────────────────────────────────────────────────────────────┘
```

## 인스턴스 타입별 스토어 지원

### 스토리지 최적화 인스턴스

| 인스턴스 타입 | 스토어 볼륨 | 총 용량 | 유형 |
|--------------|------------|--------|------|
| i3.large | 1 x 475 GB | 475 GB | NVMe SSD |
| i3.xlarge | 1 x 950 GB | 950 GB | NVMe SSD |
| i3.2xlarge | 1 x 1.9 TB | 1.9 TB | NVMe SSD |
| i3.4xlarge | 2 x 1.9 TB | 3.8 TB | NVMe SSD |
| i3.8xlarge | 4 x 1.9 TB | 7.6 TB | NVMe SSD |
| i3.16xlarge | 8 x 1.9 TB | 15.2 TB | NVMe SSD |
| d3.xlarge | 3 x 1.9 TB | 5.7 TB | HDD |
| d3.2xlarge | 6 x 1.9 TB | 11.4 TB | HDD |

### 범용/컴퓨팅 최적화 인스턴스 (일부)

| 인스턴스 타입 | 스토어 볼륨 | 유형 |
|--------------|------------|------|
| m5d.large | 1 x 75 GB | NVMe SSD |
| c5d.large | 1 x 50 GB | NVMe SSD |
| r5d.large | 1 x 75 GB | NVMe SSD |

## 인스턴스 스토어 사용

### 블록 디바이스 매핑

인스턴스 시작 시 블록 디바이스 매핑을 지정합니다.

```bash
# 인스턴스 스토어가 있는 인스턴스 시작
aws ec2 run-instances \
    --image-id ami-xxxxxxxxx \
    --instance-type i3.large \
    --block-device-mappings '[
        {
            "DeviceName": "/dev/xvda",
            "Ebs": {
                "VolumeSize": 30,
                "VolumeType": "gp3"
            }
        }
    ]'
```

### Linux에서 마운트

```bash
# 사용 가능한 디스크 확인
lsblk

# NVMe 디바이스 확인 (NVMe 인스턴스 스토어)
nvme list

# 파일 시스템 생성
sudo mkfs -t xfs /dev/nvme1n1

# 마운트 포인트 생성 및 마운트
sudo mkdir /mnt/instance-store
sudo mount /dev/nvme1n1 /mnt/instance-store

# fstab에 추가 (재부팅 시 자동 마운트, nofail 중요)
echo '/dev/nvme1n1 /mnt/instance-store xfs defaults,nofail 0 2' | sudo tee -a /etc/fstab
```

### 인스턴스 스토어 볼륨 확인

```bash
# 인스턴스 메타데이터에서 블록 디바이스 매핑 확인
TOKEN=$(curl -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")

curl -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/block-device-mapping/
```

## 사용 사례

### 적합한 워크로드

| 사용 사례 | 설명 |
|-----------|------|
| ** 임시 캐시** | Redis, Memcached |
| ** 버퍼/큐** | 메시지 큐, 처리 대기 데이터 |
| ** 스크래치 데이터** | 임시 계산 결과 |
| ** 복제된 데이터** | 분산 파일 시스템 (HDFS) |
| ** 로그 처리** | 임시 로그 저장 |

### 부적합한 워크로드

| 사용 사례 | 이유 |
|-----------|------|
| 데이터베이스 | 데이터 지속성 필요 |
| 영구 스토리지 | 인스턴스 중지 시 손실 |
| 단일 복사본 데이터 | 장애 시 복구 불가 |

## 성능 최적화

### RAID 0 구성

여러 인스턴스 스토어 볼륨을 결합하여 성능 향상

```bash
# RAID 0 배열 생성 (Linux)
sudo mdadm --create --verbose /dev/md0 --level=0 \
    --raid-devices=2 /dev/nvme1n1 /dev/nvme2n1

# 파일 시스템 생성
sudo mkfs -t xfs /dev/md0

# 마운트
sudo mount /dev/md0 /mnt/raid
```

### 성능 벤치마크

```bash
# fio로 성능 테스트
sudo yum install -y fio

# 순차 읽기 테스트
sudo fio --name=seq-read --rw=read --bs=128k \
    --size=1G --numjobs=4 --directory=/mnt/instance-store

# 랜덤 읽기/쓰기 테스트
sudo fio --name=random-rw --rw=randrw --bs=4k \
    --size=1G --numjobs=4 --directory=/mnt/instance-store
```

## NVMe 인스턴스 스토어

### 특징

- 하드웨어 수준 암호화 (AES-256)
- 더 높은 IOPS 및 처리량
- 더 낮은 지연 시간

### NVMe 디바이스 관리

```bash
# NVMe CLI 설치
sudo yum install -y nvme-cli

# NVMe 디바이스 목록
nvme list

# 디바이스 정보 확인
nvme id-ctrl /dev/nvme1n1

# SMART 정보 확인
nvme smart-log /dev/nvme1n1
```

## 모범 사례

### 1. 데이터 백업 전략

```bash
#!/bin/bash
# 인스턴스 스토어 데이터를 S3에 백업
aws s3 sync /mnt/instance-store s3://my-backup-bucket/instance-store-backup/
```

### 2. 시작 스크립트에서 마운트

User Data에서 인스턴스 스토어를 자동으로 설정합니다.

```bash
#!/bin/bash
# User Data 스크립트
DEVICE=/dev/nvme1n1
MOUNT_POINT=/mnt/instance-store

# 디바이스가 있는지 확인
if [ -b "$DEVICE" ]; then
    # 파일 시스템이 없으면 생성
    if [ "$(file -s $DEVICE)" = "$DEVICE: data" ]; then
        mkfs -t xfs $DEVICE
    fi

    # 마운트
    mkdir -p $MOUNT_POINT
    mount $DEVICE $MOUNT_POINT
fi
```

### 3. 애플리케이션 수준 복제

인스턴스 스토어를 사용하는 경우 애플리케이션 수준에서 데이터 복제를 구현합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    복제 아키텍처                             │
│                                                              │
│  ┌──────────────┐         ┌──────────────┐                 │
│  │  Instance 1  │◄───────▶│  Instance 2  │                 │
│  │ Instance     │  복제   │ Instance     │                 │
│  │  Store       │         │  Store       │                 │
│  └──────────────┘         └──────────────┘                 │
│          │                        │                         │
│          └────────────┬───────────┘                         │
│                       ▼                                     │
│              ┌──────────────┐                               │
│              │  Instance 3  │                               │
│              │ Instance     │                               │
│              │  Store       │                               │
│              └──────────────┘                               │
└─────────────────────────────────────────────────────────────┘
```

## 참고 자료

- [Amazon EC2 인스턴스 스토어](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/InstanceStorage.html)
- [인스턴스 스토어 볼륨](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-store-volumes.html)
- [NVMe EBS 볼륨](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/nvme-ebs-volumes.html)
