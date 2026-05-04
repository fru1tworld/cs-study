# Mimir Alertmanager

> 이 문서는 Grafana Mimir 공식 문서의 Alertmanager/Ruler 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/references/architecture/components/alertmanager/

---

## 목차

1. [개요](#개요)
2. [Alertmanager 구성](#alertmanager-구성)
3. [Ruler와 Alertmanager 연동](#ruler와-alertmanager-연동)
4. [알림 룰 작성](#알림-룰-작성)
5. [라우팅과 그룹화](#라우팅과-그룹화)
6. [수신기(Receivers) 설정](#수신기receivers-설정)
7. [억제(Inhibition)와 무시(Silence)](#억제inhibition와-무시silence)
8. [멀티 테넌트 알림](#멀티-테넌트-알림)
9. [Mimirtool로 관리](#mimirtool로-관리)

---

## 개요

Mimir는 **Prometheus Alertmanager를 내장** 하여, 별도 구성 없이 알림을 처리할 수 있습니다.

### 컴포넌트 흐름

```
[Ruler] --evaluates rules--> 알림 발생
   |
   v (HTTP)
[Mimir Alertmanager]
   |
   +-> [Slack]
   +-> [PagerDuty]
   +-> [Email]
   +-> [Webhook]
   +-> [기타 통지 채널]
```

### 주요 특징

- 멀티 테넌시 (테넌트별 별도 설정)
- HA 클러스터링 (gossip 프로토콜)
- 템플릿 기반 알림 메시지
- API로 동적 설정

---

## Alertmanager 구성

### Mimir 설정

```yaml
alertmanager:
  data_dir: /data/alertmanager
  
  external_url: https://alertmanager.example.com
  
  sharding_ring:
    kvstore:
      store: memberlist
    replication_factor: 3
  
  enable_api: true
  
  fallback_config_file: /etc/alertmanager/fallback.yaml
  
  # 클러스터링 (HA)
  cluster:
    peers: alertmanager-0:9094,alertmanager-1:9094,alertmanager-2:9094

alertmanager_storage:
  backend: s3
  s3:
    bucket_name: my-mimir-alertmanager
```

### Fallback Config (모든 테넌트 기본)

```yaml
# /etc/alertmanager/fallback.yaml
route:
  receiver: default-receiver
  group_by: [alertname, cluster]
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: default-receiver
    webhook_configs:
      - url: http://default-webhook:9090/notify
```

---

## Ruler와 Alertmanager 연동

### Ruler 설정

```yaml
ruler:
  alertmanager_url: http://alertmanager:8080/alertmanager
  enable_alertmanager_v2: true
  alertmanager_refresh_interval: 1m
  
  # DNS 기반 자동 디스커버리
  enable_alertmanager_discovery: true
  
  notification_queue_capacity: 10000
  notification_timeout: 10s
  
  # for_outage_tolerance: Alertmanager 다운 시간 대응
  for_outage_tolerance: 1h
  for_grace_period: 10m
  resend_delay: 1m
```

### 다중 Alertmanager (HA)

```yaml
ruler:
  alertmanager_url: http://am-0:8080/alertmanager,http://am-1:8080/alertmanager,http://am-2:8080/alertmanager
```

### DNS SRV 레코드 사용

```yaml
ruler:
  alertmanager_url: dnssrv+http://_web._tcp.alertmanager.default.svc.cluster.local/alertmanager
```

---

## 알림 룰 작성

### 룰 파일 형식

```yaml
groups:
  - name: <group_name>
    interval: <evaluation_interval>
    rules:
      - alert: <alert_name>
        expr: <PromQL_expression>
        for: <duration>
        labels:
          <key>: <value>
        annotations:
          summary: ...
          description: ...
```

### 예시: 인스턴스 다운

```yaml
groups:
  - name: infra
    interval: 30s
    rules:
      - alert: InstanceDown
        expr: up == 0
        for: 5m
        labels:
          severity: critical
          team: infra
        annotations:
          summary: "{{ $labels.instance }} 인스턴스 다운"
          description: |
            {{ $labels.instance }}이(가) 5분 이상 응답하지 않습니다.
            Job: {{ $labels.job }}
```

### 예시: 높은 CPU 사용률

```yaml
- alert: HighCPUUsage
  expr: |
    100 - (avg by (instance) (
      rate(node_cpu_seconds_total{mode="idle"}[5m])
    ) * 100) > 90
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "{{ $labels.instance }} CPU 사용률 90% 초과"
    value: "{{ $value | humanize }}%"
```

### 예시: SLO Burn Rate

```yaml
- alert: ErrorBudgetBurn
  expr: |
    (
      sum(rate(http_requests_total{status=~"5.."}[1h]))
      /
      sum(rate(http_requests_total[1h]))
    ) > (1 - 0.999) * 14.4
  for: 2m
  labels:
    severity: critical
    slo: api_availability
  annotations:
    summary: "API SLO 99.9% 위반 (1시간 기준 14배 burn)"
```

### Mimirtool로 업로드

```bash
mimirtool rules load \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  alerts.yaml
```

---

## 라우팅과 그룹화

### Alertmanager 설정 예시

```yaml
route:
  receiver: default
  group_by: [alertname, cluster, service]
  group_wait: 30s        # 그룹 첫 알림 대기
  group_interval: 5m     # 같은 그룹 추가 알림 간격
  repeat_interval: 4h    # 반복 알림 간격
  
  routes:
    # 심각도별 라우팅
    - matchers:
        - severity = critical
      receiver: pagerduty-critical
      continue: true       # 다음 라우트도 매칭
    
    - matchers:
        - team = backend
      receiver: slack-backend
      group_wait: 10s
    
    - matchers:
        - team = frontend
      receiver: slack-frontend
    
    # 야간 시간 다른 라우팅
    - matchers:
        - severity =~ "warning|info"
      receiver: slack-dev
      active_time_intervals:
        - working_hours

time_intervals:
  - name: working_hours
    time_intervals:
      - times:
          - start_time: '09:00'
            end_time: '18:00'
        weekdays: ['monday:friday']
        location: 'Asia/Seoul'

receivers:
  - name: default
    webhook_configs:
      - url: http://default-webhook:9090
  
  - name: pagerduty-critical
    pagerduty_configs:
      - service_key: <KEY>
  
  - name: slack-backend
    slack_configs:
      - api_url: <SLACK_WEBHOOK>
        channel: '#alerts-backend'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
  
  - name: slack-frontend
    slack_configs:
      - api_url: <SLACK_WEBHOOK>
        channel: '#alerts-frontend'
  
  - name: slack-dev
    slack_configs:
      - api_url: <SLACK_WEBHOOK>
        channel: '#alerts-dev'
```

---

## 수신기(Receivers) 설정

### Slack

```yaml
- name: slack
  slack_configs:
    - api_url: 'https://hooks.slack.com/services/...'
      channel: '#alerts'
      title: '{{ template "slack.default.title" . }}'
      text: '{{ template "slack.default.text" . }}'
      send_resolved: true
      actions:
        - type: button
          text: 'View in Grafana'
          url: '{{ .GeneratorURL }}'
```

### PagerDuty

```yaml
- name: pagerduty
  pagerduty_configs:
    - service_key: <SERVICE_KEY>
      severity: '{{ .CommonLabels.severity }}'
      details:
        firing: '{{ .Alerts.Firing | len }}'
        resolved: '{{ .Alerts.Resolved | len }}'
```

### Email

```yaml
- name: email
  email_configs:
    - to: 'team@example.com'
      from: 'alerts@example.com'
      smarthost: 'smtp.example.com:587'
      auth_username: 'alerts@example.com'
      auth_password: '${SMTP_PASSWORD}'
      require_tls: true
```

### Webhook

```yaml
- name: webhook
  webhook_configs:
    - url: 'https://my-service.example.com/webhook'
      max_alerts: 0
      send_resolved: true
      http_config:
        bearer_token: '${TOKEN}'
```

### Microsoft Teams

```yaml
- name: msteams
  msteams_configs:
    - webhook_url: 'https://outlook.office.com/webhook/...'
```

### OpsGenie

```yaml
- name: opsgenie
  opsgenie_configs:
    - api_key: '${OPSGENIE_KEY}'
      teams: 'team-a'
      tags: 'production,critical'
```

---

## 억제(Inhibition)와 무시(Silence)

### Inhibition: 다른 알림이 발생하면 특정 알림 억제

```yaml
inhibit_rules:
  - source_matchers:
      - severity = critical
    target_matchers:
      - severity = warning
    equal: [cluster, alertname]
```

`severity=critical` 알림이 활성 상태이면, 같은 cluster/alertname의 `warning` 알림은 억제.

### Silence: 일시적으로 알림 숨김

API 호출:

```bash
curl -X POST -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/json" \
  http://mimir:9009/api/v1/alerts \
  -d '{
    "matchers": [
      {"name": "alertname", "value": "TestAlert", "isRegex": false}
    ],
    "startsAt": "2024-01-01T00:00:00Z",
    "endsAt": "2024-01-01T01:00:00Z",
    "createdBy": "ops",
    "comment": "Maintenance window"
  }'
```

또는 Grafana UI에서 관리.

---

## 멀티 테넌트 알림

### 테넌트별 Alertmanager 설정

각 테넌트는 자체 Alertmanager 설정을 가질 수 있습니다.

#### API로 설정

```bash
curl -X POST -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/yaml" \
  --data-binary @tenant-1-alertmanager.yaml \
  http://mimir:9009/api/v1/alerts
```

#### `tenant-1-alertmanager.yaml`

```yaml
template_files:
  default_template: |
    {{ define "default.title" }}
    [{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}
    {{ end }}

alertmanager_config: |
  route:
    receiver: tenant-1-default
    group_by: [alertname]
  
  receivers:
    - name: tenant-1-default
      slack_configs:
        - api_url: <TENANT_1_SLACK>
          channel: '#tenant1-alerts'
```

### 조회

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/api/v1/alerts
```

### 삭제

```bash
curl -X DELETE -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/api/v1/alerts
```

---

## Mimirtool로 관리

### 설치

```bash
brew install mimirtool
# 또는
curl -fLo mimirtool https://github.com/grafana/mimir/releases/latest/download/mimirtool-linux-amd64
chmod +x mimirtool
```

### 알림 룰 관리

```bash
# 룰 목록
mimirtool rules list \
  --address=http://mimir:9009 \
  --id=tenant-1

# 룰 업로드
mimirtool rules load \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  alerts.yaml recording.yaml

# 룰 다운로드
mimirtool rules print \
  --address=http://mimir:9009 \
  --id=tenant-1

# 룰 삭제
mimirtool rules delete \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  --namespace=infra \
  --rule-group=node_alerts

# 룰 lint
mimirtool rules lint alerts.yaml

# Cortex/Prometheus → Mimir 변환
mimirtool analyze grafana --address=http://grafana:3000
```

### Alertmanager 구성 관리

```bash
# 업로드
mimirtool alertmanager load \
  --address=http://mimir:9009 \
  --id=tenant-1 \
  alertmanager-config.yaml

# 조회
mimirtool alertmanager get \
  --address=http://mimir:9009 \
  --id=tenant-1

# 검증
mimirtool alertmanager verify alertmanager-config.yaml
```

---

## 운영 팁

### HA 클러스터링

3개 이상의 Alertmanager 인스턴스를 gossip으로 연결:

```yaml
alertmanager:
  cluster:
    peers: am-0:9094,am-1:9094,am-2:9094
    listen_address: 0.0.0.0:9094
    advertise_address: ${POD_IP}:9094
    
    # 가십 설정
    push_pull_interval: 60s
    gossip_interval: 200ms
```

### 알림 폭증 방지

```yaml
limits:
  alertmanager_notification_rate_limit: 10  # per second per tenant
  alertmanager_notification_rate_limit_per_integration:
    slack: 5
    pagerduty: 1
```

### 모니터링

| 메트릭 | 설명 |
|--------|------|
| `cortex_alertmanager_alerts_received_total` | 수신 알림 수 |
| `cortex_alertmanager_notifications_total` | 발송된 통지 수 |
| `cortex_alertmanager_notifications_failed_total` | 실패한 통지 수 |
| `cortex_alertmanager_partial_state_merges_total` | 상태 병합 |
