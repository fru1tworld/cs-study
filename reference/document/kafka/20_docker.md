# Kafka Docker

> 이 문서는 Apache Kafka 공식 문서의 "Docker" 섹션을 한국어로 번역한 것입니다.
> 원본: https://kafka.apache.org/documentation/#docker

## 개요

Apache Kafka는 공식 Docker 이미지를 제공하여 컨테이너 환경에서 쉽게 Kafka를 배포하고 실행할 수 있습니다. 이 문서에서는 Docker를 사용하여 Kafka를 설정하고 구성하는 방법을 설명합니다.

---

## 시스템 요구사항

Docker 버전 20.10.4 이상 이 필요합니다. 이전 버전의 Docker는 `/opt/kafka/config`와 같은 컨테이너 경로를 생성할 때 디렉토리 권한을 올바르게 설정하지 않아 권한 오류가 발생할 수 있습니다.

---

## Docker 이미지

Apache Kafka는 두 가지 공식 Docker 이미지를 제공합니다:

### JVM 기반 이미지

표준 JVM에서 실행되는 Kafka 이미지입니다.

```bash
docker pull apache/kafka:4.1.1
docker run -p 9092:9092 apache/kafka:4.1.1
```

### GraalVM Native 이미지

GraalVM으로 네이티브 컴파일된 이미지로, 더 빠른 시작 시간과 낮은 메모리 사용량을 제공합니다.

```bash
docker pull apache/kafka-native:4.1.1
docker run -p 9092:9092 apache/kafka-native:4.1.1
```

---

## 구성 방법

Kafka Docker 컨테이너를 구성하는 세 가지 방법이 있습니다:

### 1. 기본 구성 (Default Configuration)

사용자 정의 구성(파일 입력 또는 환경 변수)이 Docker 컨테이너에 전달되지 않으면, Kafka tarball에 패키지된 기본 KRaft 구성이 사용됩니다. 이 기본 구성은 단일 결합 모드(combined-mode) 노드용입니다.

```bash
docker run -p 9092:9092 apache/kafka:latest
```

### 2. 파일 입력 방식 (File Input)

속성 파일이 포함된 로컬 폴더를 Docker 볼륨을 통해 컨테이너에 마운트할 수 있습니다.

```bash
docker run --volume /path/to/property/folder:/mnt/shared/config -p 9092:9092 apache/kafka:latest
```

이 방식을 사용하면 기본 KRaft 구성이 사용자가 제공한 속성 파일로 대체됩니다.

### 3. 환경 변수 방식 (Environment Variables)

환경 변수를 사용하여 Kafka 속성을 설정할 수 있습니다. 환경 변수 명명 규칙:

- 속성 키 앞에 `KAFKA_` 접두사를 붙입니다
- 점(`.`)을 밑줄(`_`)로 변환합니다
- 밑줄(`_`)을 이중 밑줄(`__`)로 변환합니다
- 하이픈(`-`)을 삼중 밑줄(`___`)로 변환합니다

#### 변환 예시

| Kafka 속성 | 환경 변수 |
|------------|----------|
| `abc.def` | `KAFKA_ABC_DEF` |
| `abc_def` | `KAFKA_ABC__DEF` |
| `abc-def` | `KAFKA_ABC___DEF` |
| `num.partitions` | `KAFKA_NUM_PARTITIONS` |

참고: 환경 변수를 통해 정의된 Kafka 속성은 사용자가 제공한 속성 파일에 정의된 해당 속성의 값을 재정의합니다. 공통 구성 세트는 입력 파일을 사용하고, 특정 노드 속성은 환경 변수를 사용하여 재정의하는 것도 가능합니다.

---

## 주요 환경 변수

KRaft 모드에서 Kafka를 구성하는 데 사용되는 주요 환경 변수:

| 환경 변수 | 설명 |
|----------|------|
| `KAFKA_NODE_ID` | 노드 식별자 |
| `KAFKA_PROCESS_ROLES` | 노드의 역할 (broker, controller, 또는 둘 다) |
| `KAFKA_LISTENERS` | 리스너 구성 |
| `KAFKA_ADVERTISED_LISTENERS` | 외부에서 접근 가능한 리스너 주소 |
| `KAFKA_CONTROLLER_LISTENER_NAMES` | 컨트롤러 리스너 이름 |
| `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP` | 보안 프로토콜 매핑 |
| `KAFKA_CONTROLLER_QUORUM_VOTERS` | KRaft 모드의 쿼럼 투표자 |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | 오프셋 토픽의 복제 인수 |
| `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR` | 트랜잭션 로그의 복제 인수 |
| `KAFKA_NUM_PARTITIONS` | 기본 파티션 수 |

### 단일 노드 주의사항

단일 노드로 실행할 때는 `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`를 명시적으로 1로 설정해야 합니다. 설정하지 않으면 기본값인 3이 사용되어 오류가 발생합니다.

```bash
docker run -p 9092:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:latest
```

---

## 배포 예제

### 단일 노드 (Plaintext)

Docker Compose를 사용한 단일 노드 배포:

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/single-node/plaintext/docker-compose.yml up
```

메시지 생성 테스트:

```bash
bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9092
```

### 단일 노드 Docker Compose 예제

```yaml
version: '3'
services:
  kafka:
    image: apache/kafka:latest
    hostname: broker
    container_name: broker
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@broker:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://broker:29092,CONTROLLER://broker:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_PROCESS_ROLES: 'broker,controller'
```

### 다중 노드 클러스터 (Combined Mode - Plaintext)

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/cluster/combined/plaintext/docker-compose.yml up
```

각 브로커는 고유한 포트(29092, 39092, 49092)를 호스트에 노출합니다.

### 다중 노드 클러스터 (Isolated Mode)

컨트롤러와 브로커가 별도로 구성되며, 브로커는 컨트롤러에 의존합니다:

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/cluster/isolated/plaintext/docker-compose.yml up
```

---

## 리스너 설정

Docker 환경에서 Kafka 리스너를 올바르게 구성하는 것이 중요합니다:

| 리스너 | 용도 |
|--------|------|
| `PLAINTEXT` | 컨테이너 호스트 이름을 통한 브로커 간 통신 |
| `PLAINTEXT_HOST` | localhost를 통한 클라이언트-브로커 통신 |
| `SSL` / `SSL-INTERNAL` | 프로덕션 환경을 위한 암호화된 통신 |

---

## SSL 구성

SSL을 사용하여 Kafka를 보안하는 권장 방법:

1. 시크릿을 컨테이너의 `/etc/kafka/secrets`에 마운트합니다
2. SSL 자격 증명을 환경 변수로 제공합니다
3. SSL 리스너와 함께 적절한 `KAFKA_ADVERTISED_LISTENERS`를 설정합니다

### SSL 관련 환경 변수

| 환경 변수 | 설명 |
|----------|------|
| `KAFKA_SSL_KEYSTORE_FILENAME` | 키스토어 파일 이름 |
| `KAFKA_SSL_KEYSTORE_CREDENTIALS` | 키스토어 자격 증명 파일 |
| `KAFKA_SSL_TRUSTSTORE_FILENAME` | 트러스트스토어 파일 이름 |
| `KAFKA_SSL_TRUSTSTORE_CREDENTIALS` | 트러스트스토어 자격 증명 파일 |
| `KAFKA_SSL_KEY_CREDENTIALS` | SSL 키 자격 증명 파일 |

### 단일 노드 SSL 배포

```bash
IMAGE=apache/kafka:latest docker compose -f docker/examples/docker-compose-files/single-node/ssl/docker-compose.yml up
```

---

## Log4j 구성

Docker 환경에서 로깅을 구성할 수 있습니다:

| 환경 변수 | 설명 |
|----------|------|
| `KAFKA_LOG4J_ROOT_LOGLEVEL` | 루트 로거 레벨 설정 |
| `KAFKA_LOG4J_LOGGERS` | 커스텀 로거를 위한 쉼표로 구분된 속성 쌍 |

예시:

```bash
docker run -p 9092:9092 \
  -e KAFKA_LOG4J_ROOT_LOGLEVEL=INFO \
  -e KAFKA_LOG4J_LOGGERS="kafka.controller=WARN,kafka.producer=WARN" \
  apache/kafka:latest
```

---

## ZooKeeper vs KRaft 모드

### KRaft 모드 (권장)

최신 Kafka 버전은 ZooKeeper 없이 클러스터 조정을 내부화하는 KRaft 모드를 지원합니다. KRaft 모드는 다음과 같은 장점을 제공합니다:

- 외부 ZooKeeper 의존성 제거
- 더 간단한 배포 및 운영
- 향상된 확장성

### ZooKeeper 지원 종료

- ZooKeeper는 Kafka 버전 3.5에서 더 이상 사용되지 않음(deprecated)으로 표시되었습니다
- Kafka 버전 4.0에서 ZooKeeper 지원이 제거될 예정입니다

---

## 문제 해결

### 권한 오류

Docker 버전이 20.10.4 이상인지 확인하세요. 이전 버전에서는 컨테이너 경로 생성 시 권한 오류가 발생할 수 있습니다.

```bash
docker --version
```

### 복제 인수 오류

단일 노드 배포 시 다음 환경 변수를 명시적으로 설정하세요:

```bash
-e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
-e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
```

### 연결 문제

`KAFKA_ADVERTISED_LISTENERS`가 클라이언트가 접근할 수 있는 주소로 올바르게 설정되어 있는지 확인하세요.

---

## 요약

| 항목 | 설명 |
|------|------|
| JVM 이미지 | `apache/kafka:latest` - 표준 JVM 기반 |
| Native 이미지 | `apache/kafka-native:latest` - GraalVM 네이티브 컴파일 |
| 구성 방식 | 기본 구성, 파일 입력, 환경 변수 |
| Docker 최소 버전 | 20.10.4 이상 |
| 권장 모드 | KRaft (ZooKeeper 없음) |

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Apache Kafka Docker Hub](https://hub.docker.com/r/apache/kafka)
- [Apache Kafka Native Docker Hub](https://hub.docker.com/r/apache/kafka-native)
- [Kafka Docker 예제 (GitHub)](https://github.com/apache/kafka/blob/trunk/docker/examples/README.md)
- [Kafka Quickstart](https://kafka.apache.org/quickstart)
