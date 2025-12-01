# Amazon EBS (Elastic Block Store)

## 개요

Amazon EBS는 EC2 인스턴스와 함께 사용하도록 설계된 고성능 블록 스토리지 서비스입니다. 인스턴스의 수명과 독립적으로 데이터를 유지합니다.

## EBS 특징

| 특성 | 설명 |
|------|------|
| ** 지속성** | 인스턴스와 독립적으로 데이터 유지 |
| ** 가용성** | 가용 영역 내 자동 복제 |
| ** 확장성** | 다운타임 없이 크기/성능 조정 |
| ** 암호화** | 투명한 암호화 지원 |
| ** 스냅샷** | S3에 저장되는 시점 백업 |

## EBS 볼륨 타입

### SSD 기반 볼륨

#### gp3 (General Purpose SSD)

가장 권장되는 범용 SSD 볼륨입니다.

| 속성 | 값 |
|------|-----|
| 최대 IOPS | 16,000 |
| 최대 처리량 | 1,000 MiB/s |
| 볼륨 크기 | 1 GiB - 16 TiB |
| 기본 IOPS | 3,000 |
| 기본 처리량 | 125 MiB/s |
| 지연 시간 | 한 자릿수 밀리초 |

```bash
# gp3 볼륨 생성
aws ec2 create-volume \
    --volume-type gp3 \
    --size 100 \
    --iops 4000 \
    --throughput 200 \
    --availability-zone ap-northeast-2a
```

#### gp2 (General Purpose SSD)

이전 세대 범용 SSD입니다.

| 속성 | 값 |
|------|-----|
| 최대 IOPS | 16,000 |
| 최대 처리량 | 250 MiB/s |
| 볼륨 크기 | 1 GiB - 16 TiB |
| IOPS | 3 IOPS/GiB (최소 100) |
| 버스트 | 최대 3,000 IOPS |

#### io2 Block Express

최고 성능 SSD입니다.

| 속성 | 값 |
|------|-----|
| 최대 IOPS | 256,000 |
| 최대 처리량 | 4,000 MiB/s |
| 볼륨 크기 | 4 GiB - 64 TiB |
| IOPS/GiB | 최대 1,000 |
| 내구성 | 99.999% |
| 지연 시간 | 500 마이크로초 미만 |

```bash
# io2 볼륨 생성
aws ec2 create-volume \
    --volume-type io2 \
    --size 500 \
    --iops 32000 \
    --availability-zone ap-northeast-2a
```

### HDD 기반 볼륨

#### st1 (Throughput Optimized HDD)

| 속성 | 값 |
|------|-----|
| 최대 처리량 | 500 MiB/s |
| 볼륨 크기 | 125 GiB - 16 TiB |
| 기본 처리량 | 40 MiB/s/TiB |
| 버스트 처리량 | 250 MiB/s/TiB |
| 사용 사례 | 빅데이터, 로그 처리, 데이터 웨어하우스 |

#### sc1 (Cold HDD)

| 속성 | 값 |
|------|-----|
| 최대 처리량 | 250 MiB/s |
| 볼륨 크기 | 125 GiB - 16 TiB |
| 기본 처리량 | 12 MiB/s/TiB |
| 버스트 처리량 | 80 MiB/s/TiB |
| 사용 사례 | 자주 액세스하지 않는 데이터 |

### 볼륨 타입 비교

```
┌─────────────────────────────────────────────────────────────┐
│                    EBS 볼륨 타입 선택                        │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
   ┌─────────┐          ┌─────────┐          ┌─────────┐
   │ 트랜잭션 │          │ 처리량   │          │ 비용 효율│
   │ IOPS 중요│          │ 중심    │          │ 콜드 데이터
   └────┬────┘          └────┬────┘          └────┬────┘
        │                    │                    │
        ▼                    ▼                    ▼
   ┌─────────┐          ┌─────────┐          ┌─────────┐
   │gp3/io2  │          │  st1    │          │  sc1    │
   └─────────┘          └─────────┘          └─────────┘
```

## EBS 볼륨 관리

### 볼륨 생성

```bash
aws ec2 create-volume \
    --volume-type gp3 \
    --size 100 \
    --availability-zone ap-northeast-2a \
    --encrypted \
    --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=MyVolume}]'
```

### 인스턴스에 연결

```bash
aws ec2 attach-volume \
    --volume-id vol-xxxxxxxxx \
    --instance-id i-xxxxxxxxx \
    --device /dev/xvdf
```

### Linux에서 마운트

```bash
# 파일 시스템 확인
sudo file -s /dev/xvdf

# 파일 시스템 생성 (새 볼륨인 경우)
sudo mkfs -t xfs /dev/xvdf

# 마운트 포인트 생성
sudo mkdir /data

# 마운트
sudo mount /dev/xvdf /data

# 부팅 시 자동 마운트 (/etc/fstab에 추가)
echo '/dev/xvdf /data xfs defaults,nofail 0 2' | sudo tee -a /etc/fstab
```

### 볼륨 분리

```bash
# 인스턴스에서 언마운트
sudo umount /data

# 볼륨 분리
aws ec2 detach-volume --volume-id vol-xxxxxxxxx
```

### 볼륨 삭제

```bash
aws ec2 delete-volume --volume-id vol-xxxxxxxxx
```

## EBS 성능 최적화

### IOPS vs 처리량

| 워크로드 | 중요 지표 | 권장 볼륨 |
|----------|-----------|----------|
| 데이터베이스 | IOPS | gp3, io2 |
| 스트리밍 | 처리량 | st1 |
| 로그 분석 | 처리량 | st1 |
| 백업 | 비용 | sc1 |

### EBS 최적화 인스턴스

EBS와 인스턴스 간 전용 대역폭을 제공합니다.

```bash
# EBS 최적화 활성화
aws ec2 modify-instance-attribute \
    --instance-id i-xxxxxxxxx \
    --ebs-optimized
```

### 볼륨 크기/성능 수정

```bash
# 볼륨 크기와 IOPS 수정
aws ec2 modify-volume \
    --volume-id vol-xxxxxxxxx \
    --size 200 \
    --iops 5000 \
    --throughput 250
```

> 볼륨 수정 후 파일 시스템 확장이 필요합니다.

```bash
# XFS 파일 시스템 확장
sudo xfs_growfs /data

# ext4 파일 시스템 확장
sudo resize2fs /dev/xvdf
```

## EBS 스냅샷

### 스냅샷 생성

```bash
aws ec2 create-snapshot \
    --volume-id vol-xxxxxxxxx \
    --description "My backup snapshot" \
    --tag-specifications 'ResourceType=snapshot,Tags=[{Key=Name,Value=MySnapshot}]'
```

### 스냅샷 특성

- ** 증분식**: 마지막 스냅샷 이후 변경된 블록만 저장
- ** 저장 위치**: Amazon S3 (직접 접근 불가)
- ** 암호화**: 원본 볼륨과 동일한 암호화 상태

### 스냅샷에서 볼륨 복원

```bash
aws ec2 create-volume \
    --snapshot-id snap-xxxxxxxxx \
    --availability-zone ap-northeast-2a \
    --volume-type gp3
```

### 스냅샷 복사 (리전 간)

```bash
aws ec2 copy-snapshot \
    --source-region us-east-1 \
    --source-snapshot-id snap-xxxxxxxxx \
    --destination-region ap-northeast-2 \
    --description "Cross-region copy"
```

### 스냅샷 수명 주기 관리

Amazon Data Lifecycle Manager를 사용하여 자동화합니다.

```bash
# DLM 정책 생성
aws dlm create-lifecycle-policy \
    --description "Daily EBS snapshots" \
    --state ENABLED \
    --execution-role-arn arn:aws:iam::xxx:role/AWSDataLifecycleManagerDefaultRole \
    --policy-details '{...}'
```

## EBS 암호화

### 암호화 특성

- AWS KMS 키 사용
- 저장 데이터, 전송 데이터, 스냅샷 모두 암호화
- 인스턴스와 볼륨 간 투명한 암호화

### 기본 암호화 활성화

```bash
# 리전의 기본 EBS 암호화 활성화
aws ec2 enable-ebs-encryption-by-default

# 기본 KMS 키 설정
aws ec2 modify-ebs-default-kms-key-id \
    --kms-key-id arn:aws:kms:ap-northeast-2:xxx:key/xxx
```

### 암호화된 볼륨 생성

```bash
aws ec2 create-volume \
    --encrypted \
    --kms-key-id arn:aws:kms:ap-northeast-2:xxx:key/xxx \
    --volume-type gp3 \
    --size 100 \
    --availability-zone ap-northeast-2a
```

## 다중 연결 (Multi-Attach)

io1/io2 볼륨을 여러 인스턴스에 동시 연결할 수 있습니다.

### 제한 사항

- 동일 가용 영역의 인스턴스
- 최대 16개 인스턴스
- Nitro 기반 인스턴스만 지원
- 클러스터 인식 파일 시스템 필요

```bash
# Multi-Attach 활성화된 볼륨 생성
aws ec2 create-volume \
    --volume-type io2 \
    --size 100 \
    --iops 10000 \
    --multi-attach-enabled \
    --availability-zone ap-northeast-2a
```

## 모범 사례

1. ** 적절한 볼륨 타입 선택**: 워크로드에 맞는 볼륨 타입 사용
2. ** 정기 스냅샷**: DLM을 사용한 자동 백업
3. ** 암호화 기본 활성화**: 보안 규정 준수
4. ** 모니터링**: CloudWatch로 IOPS, 처리량, 지연 시간 모니터링
5. ** 태깅**: 비용 할당 및 관리를 위한 태그 사용

## 참고 자료

- [Amazon EBS User Guide](https://docs.aws.amazon.com/ebs/latest/userguide/)
- [EBS 볼륨 타입](https://docs.aws.amazon.com/ebs/latest/userguide/ebs-volume-types.html)
- [EBS 스냅샷](https://docs.aws.amazon.com/ebs/latest/userguide/EBSSnapshots.html)
