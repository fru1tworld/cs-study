# Loki 알림 (Ruler)

> 이 문서는 Grafana Loki 공식 문서의 알림(Ruler) 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/alert/

---

## 목차

1. [개요](#개요)
2. [Ruler 컴포넌트](#ruler-컴포넌트)
3. [알림 룰 (Alerting Rules)](#알림-룰-alerting-rules)
4. [Recording 룰](#recording-룰)
5. [룰 저장소](#룰-저장소)
6. [Alertmanager 연동](#alertmanager-연동)
7. [Ruler 분산 모드](#ruler-분산-모드)
8. [실전 예제](#실전-예제)

---

## 개요

Loki는 **Ruler** 라는 컴포넌트를 통해 **로그 기반 알림** 을 제공합니다. Ruler는 다음 작업을 수행합니다.

- LogQL 쿼리를 주기적으로 평가
- 조건 충족 시 Alertmanager로 알림 전송 (Alerting Rules)
- 쿼리 결과를 메트릭으로 저장 (Recording Rules → Prometheus/Mimir)

### Prometheus와의 호환성

Loki Ruler는 Prometheus의 룰 파일 포맷과 동일한 구조를 사용하므로, 기존 Prometheus 알림 운영자에게 친숙합니다.

---

## Ruler 컴포넌트

### 기본 설정

```yaml
ruler:
  storage:
    type: local
    local:
      directory: /etc/loki/rules
  
  rule_path: /tmp/loki/rules-temp
  
  alertmanager_url: http://alertmanager:9093
  external_url: https://loki.example.com
  
  enable_alertmanager_v2: true
  enable_api: true
  
  # 평가 주기
  evaluation_interval: 1m
  poll_interval: 1m
  
  # 분산 모드용 링
  ring:
    kvstore:
      store: memberlist
  enable_sharding: true
```

### 디렉토리 구조

```
/etc/loki/rules/
  ├── tenant-a/
  │   ├── alerts.yaml
  │   └── recording.yaml
  └── tenant-b/
      └── rules.yaml
```

각 테넌트별로 디렉토리가 있어야 합니다.

---

## 알림 룰 (Alerting Rules)

### 룰 파일 형식

```yaml
groups:
  - name: <group_name>
    interval: <evaluation_interval>  # 선택, 그룹 단위 주기
    rules:
      - alert: <alert_name>
        expr: <LogQL_expression>
        for: <duration>              # 조건 유지 시간
        labels:
          <label_key>: <label_value>
        annotations:
          <annotation_key>: <annotation_value>
```

### 예시: 에러율 알림

```yaml
# /etc/loki/rules/tenant-1/alerts.yaml
groups:
  - name: api_alerts
    interval: 1m
    rules:
      - alert: HighErrorRate
        expr: |
          sum by (service) (
            rate({namespace="prod"} |~ "(?i)error" [5m])
          )
          / 
          sum by (service) (
            rate({namespace="prod"} [5m])
          )
          > 0.05
        for: 10m
        labels:
          severity: critical
          team: platform
        annotations:
          summary: "{{ $labels.service }} 에러율 5% 초과"
          description: |
            {{ $labels.service }} 서비스의 에러율이 5%를 초과했습니다.
            현재 에러율: {{ $value | humanizePercentage }}
```

### 예시: 로그 폭증 감지

```yaml
groups:
  - name: log_volume
    rules:
      - alert: LogVolumeSpike
        expr: |
          sum by (namespace) (rate({}[5m]))
          >
          2 * sum by (namespace) (rate({}[1h] offset 1h))
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.namespace }} 로그량 폭증"
```

### 예시: 특정 로그 출현 시 알림

```yaml
groups:
  - name: critical_events
    rules:
      - alert: PaymentFailure
        expr: |
          count_over_time(
            {app="payment-service"} 
            |~ "payment failed" 
            [5m]
          ) > 10
        for: 0m   # 즉시 발생
        labels:
          severity: critical
        annotations:
          summary: "5분간 결제 실패가 10건 초과"
```

### 라벨/주석 템플릿

Go 템플릿 문법 사용 가능.

```yaml
annotations:
  summary: "{{ $labels.service }} 알림 발생"
  description: "값: {{ $value }}, 라벨: {{ $labels }}"
  dashboard: "https://grafana.example.com/d/abc?var-service={{ $labels.service }}"
```

내장 함수:
- `humanize`, `humanizePercentage`, `humanizeDuration`, `humanizeTimestamp`
- `printf`, `title`, `toLower`, `toUpper`

---

## Recording 룰

LogQL 쿼리 결과를 메트릭으로 저장합니다. Loki는 직접 저장하지 않고 **Remote Write** 로 Prometheus나 Mimir로 전송합니다.

### Remote Write 설정

```yaml
ruler:
  remote_write:
    enabled: true
    client:
      url: http://mimir:9009/api/v1/push
      headers:
        X-Scope-OrgID: tenant-1
      timeout: 30s
      queue_config:
        capacity: 10000
        max_shards: 200
```

### Recording 룰 예시

```yaml
groups:
  - name: api_recording
    interval: 30s
    rules:
      - record: tenant1:api_error_rate:5m
        expr: |
          sum by (service) (
            rate({namespace="prod"} |~ "error" [5m])
          )
        labels:
          tenant: tenant1
      
      - record: tenant1:api_request_rate:5m
        expr: |
          sum by (service) (rate({namespace="prod"} [5m]))
```

이렇게 저장된 메트릭은 Mimir/Prometheus에서 일반 메트릭처럼 PromQL로 조회 가능.

---

## 룰 저장소

### Local

```yaml
ruler:
  storage:
    type: local
    local:
      directory: /etc/loki/rules
```

ConfigMap이나 PV로 마운트.

### S3

```yaml
ruler:
  storage:
    type: s3
    s3:
      bucketnames: my-loki-rules
      region: us-east-1
      access_key_id: ${AWS_ACCESS_KEY_ID}
      secret_access_key: ${AWS_SECRET_ACCESS_KEY}
```

### GCS

```yaml
ruler:
  storage:
    type: gcs
    gcs:
      bucket_name: my-loki-rules
```

### Azure

```yaml
ruler:
  storage:
    type: azure
    azure:
      account_name: myaccount
      account_key: ${AZURE_KEY}
      container_name: loki-rules
```

---

## Alertmanager 연동

### 단일 Alertmanager

```yaml
ruler:
  alertmanager_url: http://alertmanager:9093
  enable_alertmanager_v2: true
```

### 다중 Alertmanager (HA)

```yaml
ruler:
  alertmanager_url: http://am-0:9093,http://am-1:9093,http://am-2:9093
  enable_alertmanager_discovery: true
  alertmanager_refresh_interval: 1m
  enable_alertmanager_v2: true
```

### DNS 기반 Discovery

```yaml
ruler:
  alertmanager_url: dnssrv+http://_web._tcp.alertmanager.default.svc.cluster.local/api/v2/alerts
```

### 알림 전송 설정

```yaml
ruler:
  alertmanager_client:
    timeout: 10s
    
  notification_queue_capacity: 10000
  notification_timeout: 10s
  
  for_outage_tolerance: 1h
  for_grace_period: 10m
  resend_delay: 1m
```

---

## Ruler 분산 모드

대규모 환경에서 룰을 여러 Ruler 인스턴스에 분산.

### 활성화

```yaml
ruler:
  enable_sharding: true
  ring:
    kvstore:
      store: memberlist
    instance_addr: ${POD_IP}
    instance_id: ${POD_NAME}
  
  # 어떤 단위로 샤딩할지
  sharding_strategy: shuffle-sharding  # 또는 default
```

### Shuffle Sharding

테넌트별로 일부 Ruler만 사용하도록 격리. 한 테넌트의 무거운 룰이 다른 테넌트에 영향 안 미침.

```yaml
limits_config:
  ruler_tenant_shard_size: 3  # 테넌트당 3개 Ruler
```

---

## 룰 API

### 룰 목록 조회

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://loki:3100/loki/api/v1/rules
```

### 특정 그룹 조회

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://loki:3100/loki/api/v1/rules/<namespace>/<group>
```

### 룰 그룹 생성/업데이트

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/yaml" \
  -XPOST http://loki:3100/loki/api/v1/rules/<namespace> \
  --data-binary @rule-group.yaml
```

### 룰 그룹 삭제

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -XDELETE http://loki:3100/loki/api/v1/rules/<namespace>/<group>
```

---

## 실전 예제

### 1. SLO 위반 알림

```yaml
groups:
  - name: slo_alerts
    rules:
      - alert: APIErrorBudgetBurn
        expr: |
          (
            sum(rate({app="api"} |= "level=error" [1h]))
            /
            sum(rate({app="api"} [1h]))
          ) > (1 - 0.999) * 14.4
        for: 2m
        labels:
          severity: critical
          slo: api_availability
        annotations:
          summary: "API SLO 99.9% 위반 (1시간 윈도우)"
```

### 2. 보안 이벤트

```yaml
groups:
  - name: security
    rules:
      - alert: BruteForceAttempt
        expr: |
          sum by (source_ip) (
            count_over_time(
              {app="auth"} 
              |~ "failed login" 
              | json 
              [5m]
            )
          ) > 20
        for: 0m
        labels:
          severity: high
          team: security
        annotations:
          summary: "{{ $labels.source_ip }}에서 무차별 대입 시도 감지"
```

### 3. 인프라 알림

```yaml
groups:
  - name: infra
    rules:
      - alert: NodeDown
        expr: |
          absent_over_time(
            {job="node-logs", instance="node-1"} [10m]
          )
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Node-1에서 10분간 로그 없음 (다운 추정)"
```

### 4. 응답 시간 모니터링

```yaml
groups:
  - name: latency
    rules:
      - alert: HighLatency
        expr: |
          quantile_over_time(0.95,
            {app="api"} 
            | json 
            | unwrap response_time_ms 
            [5m]
          ) > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.app }} P95 응답시간 1초 초과"
          value: "{{ $value }}ms"
```
