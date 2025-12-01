# EC2 인스턴스 라이프사이클

## 개요

EC2 인스턴스는 시작부터 종료까지 여러 상태를 거칩니다. 각 상태는 인스턴스의 가용성과 요금 청구에 영향을 미칩니다.

## 인스턴스 상태

### 상태 다이어그램

```
                    ┌──────────────┐
                    │   시작(Launch)│
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   pending    │ ← 요금 미청구
                    └──────┬───────┘
                           │
                           ▼
           ┌───────────────────────────────┐
           │           running             │ ← 요금 청구 (초당)
           └───────────────────────────────┘
                  │         │         │
        ┌─────────┘         │         └─────────┐
        ▼                   ▼                   ▼
  ┌──────────┐       ┌──────────┐        ┌───────────┐
  │ stopping │       │ rebooting│        │shutting-  │
  └────┬─────┘       └────┬─────┘        │   down    │
       │                  │              └─────┬─────┘
       ▼                  │                    │
  ┌──────────┐            │                    ▼
  │ stopped  │ ←──────────┘              ┌───────────┐
  └────┬─────┘                           │terminated │
       │                                 └───────────┘
       └──────────────────┐
                          ▼
                   (다시 시작 가능)
```

### 각 상태 설명

| 상태 | 설명 | 요금 |
|------|------|------|
| **pending** | 인스턴스가 시작 준비 중 | 미청구 |
| **running** | 인스턴스가 실행 중이며 사용 가능 | 초당 청구 (최소 1분) |
| **stopping** | 인스턴스가 중지되는 중 | 일반적으로 미청구* |
| **stopped** | 인스턴스가 중지됨, 재시작 가능 | 미청구 |
| **shutting-down** | 인스턴스가 종료되는 중 | 미청구 |
| **terminated** | 인스턴스가 영구 삭제됨 | 미청구 |

> *휴면화(Hibernation) 중에는 stopping 상태에서도 요금이 청구될 수 있습니다.

## 주요 작업별 비교

### 재부팅(Reboot)

```bash
aws ec2 reboot-instances --instance-ids i-xxxxxxxxx
```

| 항목 | 동작 |
|------|------|
| 호스트 하드웨어 | 동일 유지 |
| 프라이빗 IPv4 | 유지 |
| 퍼블릭 IPv4 | 유지 |
| Elastic IP | 유지 |
| 인스턴스 스토어 | 유지 |
| EBS 볼륨 | 유지 |

### 중지/시작(Stop/Start)

```bash
# 중지
aws ec2 stop-instances --instance-ids i-xxxxxxxxx

# 시작
aws ec2 start-instances --instance-ids i-xxxxxxxxx
```

| 항목 | 동작 |
|------|------|
| 호스트 하드웨어 | 변경될 수 있음 |
| 프라이빗 IPv4 | 유지 |
| 퍼블릭 IPv4 | 새로 할당 |
| Elastic IP | 유지 |
| 인스턴스 스토어 | **데이터 손실** |
| EBS 볼륨 | 유지 |

> **중요**: 인스턴스 스토어 기반 인스턴스는 중지할 수 없습니다. EBS 기반 인스턴스만 중지/시작이 가능합니다.

### 휴면화(Hibernate)

```bash
# 휴면화 (메모리 상태를 EBS에 저장)
aws ec2 stop-instances --instance-ids i-xxxxxxxxx --hibernate
```

| 항목 | 동작 |
|------|------|
| 메모리 내용 | EBS에 저장 |
| 실행 중인 프로세스 | 재개 시 복원 |
| 프라이빗 IPv4 | 유지 |
| 퍼블릭 IPv4 | 새로 할당 |
| 인스턴스 스토어 | **데이터 손실** |

**휴면화 요구사항:**
- EBS 루트 볼륨에 충분한 공간
- EBS 볼륨 암호화 필수
- 지원되는 인스턴스 타입 (M3, M4, M5, C3, C4, C5, R3, R4, R5 등)
- 최대 메모리 150GB

### 종료(Terminate)

```bash
aws ec2 terminate-instances --instance-ids i-xxxxxxxxx
```

| 항목 | 동작 |
|------|------|
| 인스턴스 | 영구 삭제 |
| 프라이빗 IPv4 | 해제 |
| 퍼블릭 IPv4 | 해제 |
| Elastic IP | 연결 해제 (삭제 아님) |
| 인스턴스 스토어 | **데이터 손실** |
| EBS 볼륨 | 설정에 따라 삭제 또는 유지 |

## 종료 보호(Termination Protection)

실수로 인한 종료를 방지합니다.

```bash
# 종료 보호 활성화
aws ec2 modify-instance-attribute \
    --instance-id i-xxxxxxxxx \
    --disable-api-termination

# 종료 보호 비활성화
aws ec2 modify-instance-attribute \
    --instance-id i-xxxxxxxxx \
    --no-disable-api-termination
```

## 중지 보호(Stop Protection)

실수로 인한 중지를 방지합니다.

```bash
# 중지 보호 활성화
aws ec2 modify-instance-attribute \
    --instance-id i-xxxxxxxxx \
    --disable-api-stop
```

## 요금 청구 규칙

### 청구 시작
- 인스턴스가 `running` 상태가 되면 청구 시작
- 최소 청구 단위: 1분
- 이후 초 단위로 청구

### 청구 중지
- `stopping`, `stopped`, `terminated` 상태에서는 인스턴스 요금 미청구
- 단, EBS 볼륨과 Elastic IP는 별도 청구

### 청구 재시작
- `stopped`에서 `running`으로 전환 시 새로운 청구 주기 시작

## 상태 전환 타임라인

```
┌─────────────────────────────────────────────────────────────┐
│ 시작: pending → running (보통 수십 초 ~ 수 분)              │
├─────────────────────────────────────────────────────────────┤
│ 중지: running → stopping → stopped (보통 수 초 ~ 수 분)     │
├─────────────────────────────────────────────────────────────┤
│ 재시작: stopped → pending → running (보통 수십 초 ~ 수 분)  │
├─────────────────────────────────────────────────────────────┤
│ 종료: running → shutting-down → terminated (보통 수 초)     │
└─────────────────────────────────────────────────────────────┘
```

## 인스턴스 메타데이터로 상태 확인

```bash
# 인스턴스 내부에서 상태 확인
curl http://169.254.169.254/latest/meta-data/instance-action
```

## 참고 자료

- [인스턴스 라이프사이클](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-lifecycle.html)
- [인스턴스 중지 및 시작](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Stop_Start.html)
- [인스턴스 휴면화](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Hibernate.html)
