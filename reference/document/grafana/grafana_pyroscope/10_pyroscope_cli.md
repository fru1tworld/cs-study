# Profilecli 도구

> 이 문서는 Pyroscope 의 공식 CLI 도구인 `profilecli` 의 주요 명령어와 사용법을 다룹니다.
> 원본: https://grafana.com/docs/pyroscope/latest/configure-client/profile-cli/

---

## 목차

1. [profilecli란](#profilecli란)
2. [설치](#설치)
3. [전역 옵션](#전역-옵션)
4. [profile 관련 명령](#profile-관련-명령)
5. [bucket 관련 명령](#bucket-관련-명령)
6. [admin 명령](#admin-명령)
7. [실전 시나리오](#실전-시나리오)

---

## profilecli란

`profilecli` 는 Pyroscope 서버와 상호작용하기 위한 공식 CLI 도구입니다.

- 로컬 pprof 파일을 서버로 업로드
- 서버에서 프로파일 다운로드/조회
- 오브젝트 스토리지 버킷 검사
- 운영 점검

서버 설정/디버깅 보조 도구이며, 일반 클라이언트는 SDK/Alloy를 사용하면 됩니다.

---

## 설치

### Pre-built 바이너리

릴리스 페이지에서 OS/아키텍처에 맞는 바이너리 다운로드.

```bash
curl -L -o profilecli \
  https://github.com/grafana/pyroscope/releases/download/<VERSION>/profilecli-linux-amd64
chmod +x profilecli
sudo mv profilecli /usr/local/bin/
```

### Docker

```bash
docker run --rm grafana/profilecli:latest --help
```

### 소스에서 빌드

```bash
git clone https://github.com/grafana/pyroscope.git
cd pyroscope
go build -o profilecli ./cmd/profilecli
```

---

## 전역 옵션

```
--url string              Pyroscope 서버 주소 (기본 http://localhost:4040)
--tenant-id string        X-Scope-OrgID 헤더 값
--username string         Basic Auth 사용자명 (Grafana Cloud 등)
--password string         Basic Auth 비밀번호 (또는 토큰)
--verbose                 상세 로깅
```

환경 변수로도 설정 가능합니다.

```bash
export PROFILECLI_URL=http://pyroscope:4040
export PROFILECLI_TENANT_ID=team-a
```

---

## profile 관련 명령

### upload — pprof 파일 업로드

로컬 pprof 파일(또는 표준 입력)을 Pyroscope 서버로 업로드.

```bash
profilecli upload \
  --url http://pyroscope:4040 \
  --tenant-id team-a \
  --extra-labels=service_name=batch-job \
  --extra-labels=env=staging \
  ./profile.pb.gz
```

옵션:

| 플래그 | 설명 |
|--------|------|
| `--extra-labels` | 추가할 라벨 (반복 가능) |
| `--from`, `--to` | 시간 범위(겹쳐 쓸 때) |

### query — 프로파일 조회

라벨 매처 + 시간 범위로 프로파일을 가져옵니다. 결과는 표준 pprof로 출력되어 `go tool pprof` 등에서 분석 가능.

```bash
profilecli query \
  --url http://pyroscope:4040 \
  --query='{service_name="checkout"}' \
  --profile-type=process_cpu:cpu:nanoseconds:cpu:nanoseconds \
  --from=now-1h --to=now \
  > profile.pb.gz
```

### query series — 시리즈 목록

```bash
profilecli query series \
  --query='{service_name="checkout"}' \
  --from=now-1h --to=now
```

### query labels / query label-values

라벨 키/값을 탐색.

```bash
profilecli query labels --from=now-1h --to=now
profilecli query label-values --label=service_name
```

---

## bucket 관련 명령

### bucket list-blocks

오브젝트 스토리지에 저장된 블록 목록을 출력.

```bash
profilecli bucket list-blocks \
  --object-store.backend=s3 \
  --object-store.s3.bucket-name=my-pyroscope \
  --tenant-id=team-a
```

### bucket download-block

특정 블록을 로컬로 다운로드 (디버깅용).

```bash
profilecli bucket download-block \
  --tenant-id=team-a \
  --block-id=01HF...XYZ \
  --output-dir=./blocks
```

### bucket inspect-block

블록 내부 메타데이터/통계 확인.

```bash
profilecli bucket inspect-block \
  --block-id=01HF...XYZ
```

---

## admin 명령

운영자가 사용하는 관리 엔드포인트 wrapper.

### admin tenant-stats

테넌트별 통계.

```bash
profilecli admin tenant-stats --tenant-id=team-a
```

### admin user-stats

(레거시) 사용자별 통계.

### admin flush

Ingester의 헤드 블록을 즉시 flush. 점검 또는 종료 전 사용.

---

## 실전 시나리오

### 1) 로컬 pprof 디버깅을 서버에 업로드

```bash
# Go 표준 도구로 30초 CPU 프로파일 캡처
curl -o cpu.pprof http://localhost:6060/debug/pprof/profile?seconds=30

# Pyroscope에 업로드
profilecli upload \
  --url=http://pyroscope:4040 \
  --extra-labels=service_name=local-debug \
  --extra-labels=user=$USER \
  cpu.pprof
```

### 2) 서버 프로파일을 받아 `go tool pprof` 로 분석

```bash
profilecli query \
  --query='{service_name="checkout"}' \
  --profile-type=process_cpu:cpu:nanoseconds:cpu:nanoseconds \
  --from=now-30m --to=now > recent.pprof

go tool pprof -http=:9000 recent.pprof
```

### 3) 운영: 어떤 테넌트가 가장 많은 시리즈를 보유하는가

```bash
for t in $(profilecli admin tenants --output=names); do
  echo -n "$t: "
  profilecli admin tenant-stats --tenant-id=$t \
    --output=json | jq '.active_series'
done | sort -k2 -nr | head -10
```

### 4) 손상 의심 블록 점검

```bash
profilecli bucket inspect-block --block-id=01HF...XYZ \
  --object-store.backend=s3 \
  --object-store.s3.bucket-name=my-pyroscope
```

---

## 다음 단계

- [11_http_api.md](./11_http_api.md) - HTTP API 상세
- [07_manage.md](./07_manage.md) - 운영 가이드
