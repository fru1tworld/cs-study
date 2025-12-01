# ECS Service Connect

## 개요

Service Connect는 ECS 서비스 간 통신을 관리하는 기능입니다. **Service Discovery와 Service Mesh를 Amazon ECS에 통합**하여 VPC DNS 구성에 의존하지 않고 서비스를 연결할 수 있습니다.

## 작동 방식

```
┌─────────────────────────────────────────────────────────────┐
│                       Namespace                              │
│  ┌──────────────────────────┐  ┌──────────────────────────┐ │
│  │      WordPress Service   │  │      MySQL Service       │ │
│  │  ┌─────────┬──────────┐ │  │  ┌─────────┬──────────┐  │ │
│  │  │   App   │  Proxy   │ │  │  │   App   │  Proxy   │  │ │
│  │  │Container│ Container│ │  │  │Container│ Container│  │ │
│  │  └─────────┴──────────┘ │  │  └─────────┴──────────┘  │ │
│  └──────────────────────────┘  └──────────────────────────┘ │
│            │                              ▲                  │
│            │     mysql --host=mysql       │                  │
│            └──────────────────────────────┘                  │
│                    (단축 이름으로 접근)                        │
└─────────────────────────────────────────────────────────────┘
```

### 프록시 컨테이너

- Service Connect 활성화 시 각 태스크에 프록시 컨테이너 자동 배포
- 라운드로빈 로드 밸런싱
- 아웃라이어 감지 (비정상 엔드포인트 제외)
- 자동 재시도

## 핵심 개념

### Namespace

AWS Cloud Map 네임스페이스로 서비스를 논리적으로 그룹화합니다.

```
┌─────────────────────────────────────────┐
│           Namespace: my-app             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ web     │ │ api     │ │ mysql   │   │
│  │ service │ │ service │ │ service │   │
│  └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────┘
```

### Port Name

태스크 정의에서 포트에 할당하는 이름입니다.

```json
{
  "portMappings": [
    {
      "containerPort": 8080,
      "name": "http-api",
      "appProtocol": "http"
    }
  ]
}
```

### Client Alias

서비스 구성에서 엔드포인트의 DNS 이름과 포트를 지정합니다.

```json
{
  "clientAliases": [
    {
      "port": 80,
      "dnsName": "api"
    }
  ]
}
```

### Discovery Name

AWS Cloud Map 서비스를 생성하는 데 사용되는 이름입니다.

## 서비스 유형

### Client Service (클라이언트 서비스)

다른 서비스에 접속만 하는 서비스입니다.

```json
{
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "my-namespace"
  }
}
```

### Client-Server Service (클라이언트-서버 서비스)

엔드포인트를 노출하면서 다른 서비스에도 접속하는 서비스입니다.

```json
{
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "my-namespace",
    "services": [
      {
        "portName": "http-api",
        "clientAliases": [
          {
            "port": 80,
            "dnsName": "api"
          }
        ]
      }
    ]
  }
}
```

## 설정 방법

### 1. Namespace 생성

```bash
# AWS Cloud Map HTTP 네임스페이스 생성
aws servicediscovery create-http-namespace \
    --name my-namespace \
    --description "My application namespace"
```

### 2. Task Definition 설정

```json
{
  "family": "api-task",
  "networkMode": "awsvpc",
  "containerDefinitions": [
    {
      "name": "api",
      "image": "my-api:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp",
          "name": "http-api",
          "appProtocol": "http"
        }
      ]
    }
  ]
}
```

### 3. Service 생성 (서버 역할)

```json
{
  "serviceName": "api-service",
  "cluster": "my-cluster",
  "taskDefinition": "api-task:1",
  "desiredCount": 2,
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "arn:aws:servicediscovery:region:account:namespace/ns-xxxxx",
    "services": [
      {
        "portName": "http-api",
        "discoveryName": "api",
        "clientAliases": [
          {
            "port": 80,
            "dnsName": "api"
          }
        ]
      }
    ]
  }
}
```

### 4. Service 생성 (클라이언트 역할)

```json
{
  "serviceName": "web-service",
  "cluster": "my-cluster",
  "taskDefinition": "web-task:1",
  "desiredCount": 2,
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "arn:aws:servicediscovery:region:account:namespace/ns-xxxxx"
  }
}
```

## 전체 예시

### 시나리오: WordPress + MySQL

```
┌─────────────────────────────────────────────────────────────┐
│                    Namespace: blog                           │
│                                                              │
│  ┌────────────────────┐      ┌────────────────────┐         │
│  │   wordpress        │      │      mysql         │         │
│  │   Service          │─────►│      Service       │         │
│  │                    │      │                    │         │
│  │   mysql --host=    │      │   port: 3306       │         │
│  │   mysql            │      │   dnsName: mysql   │         │
│  └────────────────────┘      └────────────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

**MySQL Task Definition:**

```json
{
  "family": "mysql",
  "networkMode": "awsvpc",
  "containerDefinitions": [
    {
      "name": "mysql",
      "image": "mysql:8.0",
      "portMappings": [
        {
          "containerPort": 3306,
          "name": "mysql-port",
          "appProtocol": "http"
        }
      ],
      "environment": [
        {
          "name": "MYSQL_ROOT_PASSWORD",
          "value": "password"
        }
      ]
    }
  ]
}
```

**MySQL Service:**

```json
{
  "serviceName": "mysql-service",
  "taskDefinition": "mysql:1",
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "blog",
    "services": [
      {
        "portName": "mysql-port",
        "discoveryName": "mysql",
        "clientAliases": [
          {
            "port": 3306,
            "dnsName": "mysql"
          }
        ]
      }
    ]
  }
}
```

**WordPress Service:**

```json
{
  "serviceName": "wordpress-service",
  "taskDefinition": "wordpress:1",
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "blog"
  }
}
```

WordPress 컨테이너에서 `mysql --host=mysql`로 접속 가능.

## 프록시 설정

### 리소스 할당

```json
{
  "serviceConnectConfiguration": {
    "enabled": true,
    "namespace": "my-namespace",
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/service-connect-proxy",
        "awslogs-region": "ap-northeast-2",
        "awslogs-stream-prefix": "ecs"
      }
    }
  }
}
```

### 프록시 리소스

프록시 컨테이너는 태스크 리소스에서 자동으로 할당됩니다:

| 리소스 | 기본 할당 |
|--------|----------|
| CPU | 256 CPU 유닛 |
| Memory | 512 MiB |

## Service Connect vs 다른 방법

| 기능 | Service Connect | Service Discovery | VPC DNS |
|------|-----------------|-------------------|---------|
| 로드 밸런싱 | O (내장) | X | X |
| 헬스 체크 | O (내장) | X | X |
| 재시도 | O (내장) | X | X |
| 설정 복잡도 | 낮음 | 중간 | 높음 |
| 비용 | 무료 | 무료 | - |

## 제한 사항

1. **네임스페이스 범위**: 같은 네임스페이스 내 서비스만 연결 가능
2. **외부 연결**: 네임스페이스 외부 서비스는 로드 밸런서 필요
3. **지원 플랫폼**: Fargate 및 EC2 (Linux/Windows)

## 모니터링

### CloudWatch 메트릭

Service Connect 프록시는 다음 메트릭을 자동 수집:

- **RequestCount**: 요청 수
- **TargetResponseTime**: 응답 시간
- **ConnectionCount**: 연결 수
- **ConnectionDuration**: 연결 지속 시간

### 로깅

```json
{
  "serviceConnectConfiguration": {
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/service-connect",
        "awslogs-region": "ap-northeast-2",
        "awslogs-stream-prefix": "proxy"
      }
    }
  }
}
```

## 요금

- **Service Connect 자체**: 추가 비용 없음
- **AWS Cloud Map**: 추가 비용 없음
- **컴퓨팅 리소스**: 프록시 컨테이너의 vCPU/메모리 비용만 발생

## 참고 자료

- [Service Connect 문서](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-connect.html)
- [AWS Cloud Map](https://docs.aws.amazon.com/cloud-map/)
