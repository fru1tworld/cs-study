# grpcurl

> 이 문서는 gRPC 서버와 명령줄에서 상호작용하는 도구 grpcurl(curl의 gRPC 버전)의 공식 문서를 한국어로 정리한 것입니다.
> 원본: https://github.com/fullstorydev/grpcurl

---

## 목차

1. [grpcurl이란](#grpcurl이란)
2. [설치](#설치)
3. [서비스 디스커버리](#서비스-디스커버리)
4. [서비스/메서드 목록 조회 (list, describe)](#서비스메서드-목록-조회-list-describe)
5. [RPC 호출](#rpc-호출)
6. [메타데이터/헤더와 인증](#메타데이터헤더와-인증)
7. [TLS 옵션](#tls-옵션)
8. [proto 소스와 protoset](#proto-소스와-protoset)
9. [주요 플래그 레퍼런스](#주요-플래그-레퍼런스)
10. [자주 쓰는 예제 모음](#자주-쓰는-예제-모음)

---

## grpcurl이란

`grpcurl`은 gRPC 서버와 상호작용하는 명령줄 도구입니다. 한마디로 **"gRPC 서버를 위한 curl"** 입니다.

gRPC는 와이어 인코딩에 바이너리 형식인 Protocol Buffers를 사용하기 때문에 일반 `curl`로는 호출할 수 없습니다. `grpcurl`은 JSON 형식의 요청 메시지를 받아 protobuf 바이너리로 변환해 전송하고, 응답 protobuf를 다시 JSON으로 변환해 출력합니다.

주요 용도:
- 명령줄에서 gRPC 메서드 호출 및 테스트
- 서버가 노출하는 서비스/메서드/메시지 스키마 탐색
- 단항(unary)뿐 아니라 클라이언트/서버/양방향 스트리밍 메서드 호출
- 로컬 개발 중 gRPC 서버 디버깅

스키마 정보는 서버 리플렉션, `.proto` 소스 파일, 컴파일된 protoset 파일 중 하나에서 얻습니다.

---

## 설치

### Homebrew (macOS)

```bash
brew install grpcurl
```

### go install

```bash
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
```

설치 후 `$GOBIN`(보통 `$HOME/go/bin`)이 `PATH`에 포함되어 있어야 합니다.

### 사전 빌드 바이너리

[releases 페이지](https://github.com/fullstorydev/grpcurl/releases)에서 OS/아키텍처에 맞는 바이너리를 내려받아 `PATH`에 둡니다.

### Docker

```bash
docker pull fullstorydev/grpcurl:latest

# 컨테이너로 직접 호출
docker run fullstorydev/grpcurl api.grpc.me:443 list
```

### Snap (Linux)

```bash
snap install grpcurl
```

### 소스에서 빌드

```bash
git clone https://github.com/fullstorydev/grpcurl
cd grpcurl
make install
```

설치 확인:

```bash
grpcurl -version
```

---

## 서비스 디스커버리

`grpcurl`이 RPC 스키마를 파악하는 방법은 세 가지이며, 우선순위가 있습니다.

### 1. 서버 리플렉션 (Server Reflection)

서버가 gRPC 리플렉션 서비스를 지원하면 별도 플래그 없이 자동으로 사용됩니다. 가장 간편한 방법입니다.

```bash
grpcurl grpc.server.com:443 list
```

리플렉션 사용을 명시하려면 `-use-reflection`을 줍니다. proto 소스나 protoset을 같이 지정한 경우 리플렉션은 기본적으로 비활성화되므로, 둘을 함께 쓰려면 이 플래그가 필요합니다.

### 2. proto 소스 파일

서버가 리플렉션을 지원하지 않을 때 `.proto` 파일을 직접 지정합니다.

```bash
grpcurl -import-path ./protos -proto my_service.proto \
    grpc.server.com:443 list
```

### 3. protoset 파일

`protoc`/`buf`로 미리 컴파일한 `FileDescriptorSet`(protoset)을 사용합니다.

```bash
# protoc로 protoset 생성
protoc --proto_path=./protos \
    --descriptor_set_out=my_service.protoset \
    --include_imports \
    my_service.proto

# grpcurl에서 사용
grpcurl -protoset my_service.protoset \
    grpc.server.com:443 list
```

> 우선순위 정리: proto 소스(`-proto`)나 protoset(`-protoset`)을 지정하지 않으면 리플렉션을 사용합니다. 지정한 경우에는 해당 디스크립터를 사용하며, 리플렉션도 함께 쓰려면 `-use-reflection`을 추가합니다.

---

## 서비스/메서드 목록 조회 (list, describe)

### list — 서비스/메서드 나열

서버가 노출하는 모든 서비스 나열:

```bash
grpcurl localhost:8787 list
```

특정 서비스의 모든 메서드 나열:

```bash
grpcurl localhost:8787 list my.custom.server.Service
```

### describe — 타입/스키마 상세 조회

서비스, 메서드, 메시지, 필드 등 심볼의 스키마를 출력합니다.

```bash
# 서비스 전체 설명
grpcurl localhost:8787 describe my.custom.server.Service

# 특정 메서드 설명
grpcurl localhost:8787 describe my.custom.server.Service.MethodOne

# 메시지 타입 설명
grpcurl localhost:8787 describe .my.custom.server.MyRequest
```

`-msg-template`과 함께 쓰면 메시지를 설명할 때 입력 데이터의 템플릿(빈 JSON 골격)을 함께 보여줍니다.

```bash
grpcurl -msg-template localhost:8787 describe .my.custom.server.MyRequest
```

---

## RPC 호출

호출은 `grpcurl [flags] <서버주소> <서비스>/<메서드>` 형식입니다. 메서드 구분자는 `/` 또는 `.`을 사용할 수 있습니다.

### 빈 요청 호출

```bash
# TLS 서버
grpcurl grpc.server.com:443 my.custom.server.Service/Method

# 평문(non-TLS) 서버
grpcurl -plaintext localhost:8080 my.custom.server.Service/Method
```

### 요청 데이터 전달 (-d)

요청 메시지는 JSON으로 전달합니다.

```bash
grpcurl -d '{"id": 1234, "tags": ["foo", "bar"]}' \
    grpc.server.com:443 my.custom.server.Service/Method
```

### stdin에서 읽기 (-d @)

`-d @`를 주면 표준 입력에서 요청 본문을 읽습니다. 큰 페이로드나 파이프에 유용합니다.

```bash
grpcurl -d @ grpc.server.com:443 my.custom.server.Service/Method <<EOM
{
  "id": 1234,
  "tags": ["foo", "bar"]
}
EOM
```

파일에서 읽기:

```bash
grpcurl -d @ grpc.server.com:443 my.custom.server.Service/Method < request.json
```

### 스트리밍

`grpcurl`은 단항뿐 아니라 모든 스트리밍 메서드를 지원합니다.

- **클라이언트/서버 스트리밍**: `-d`의 JSON에 여러 메시지를 연달아 넣으면 클라이언트 스트림의 여러 요청으로 전송됩니다.

```bash
grpcurl -d @ localhost:8080 my.custom.server.Service/ClientStream <<EOM
{"value": 1}
{"value": 2}
{"value": 3}
EOM
```

- **양방향 스트리밍**: 대화형 터미널에서 `grpcurl`을 실행하고 stdin을 요청 본문으로 사용하면, 입력하는 메시지가 즉시 전송되어 인터랙티브하게 동작합니다.

---

## 메타데이터/헤더와 인증

### 헤더 추가 (-H)

요청 메타데이터/헤더는 `-H 'name: value'` 형식으로 추가하며, 여러 번 줄 수 있습니다.

```bash
grpcurl -H 'header1: value1' -H 'header2: value2' \
    -d '{"id": 1234}' \
    grpc.server.com:443 my.custom.server.Service/Method
```

`-H`는 RPC 요청과 리플렉션 요청 모두에 적용됩니다. 분리하려면:

- `-rpc-header`: 실제 RPC 호출에만 붙는 헤더
- `-reflect-header`: 리플렉션 요청에만 붙는 헤더

```bash
grpcurl -rpc-header 'foo: bar' -reflect-header 'baz: qux' \
    localhost:8080 my.custom.server.Service/Method
```

### 환경 변수 확장 (-expand-headers)

`-expand-headers`를 주면 헤더 값에서 `${NAME}` 구문으로 환경 변수를 참조할 수 있습니다.

```bash
export TOKEN="ey..."
grpcurl -expand-headers \
    -H 'authorization: Bearer ${TOKEN}' \
    grpc.server.com:443 my.custom.server.Service/Method
```

### 인증 (Bearer 토큰)

gRPC 인증은 보통 메타데이터로 전달되므로 헤더로 구현합니다.

```bash
grpcurl -H 'authorization: Bearer eyJhbGciOi...' \
    grpc.server.com:443 my.custom.server.Service/Method
```

> 인증 토큰처럼 민감한 헤더를 평문(`-plaintext`)으로 전송하면 노출됩니다. 운영 환경에서는 반드시 TLS를 사용하세요.

---

## TLS 옵션

`grpcurl`은 기본적으로 TLS로 연결합니다. 동작을 제어하는 플래그는 다음과 같습니다.

| 플래그 | 설명 |
|--------|------|
| `-plaintext` | TLS 없이 평문 HTTP/2로 연결 (개발/로컬 디버깅용) |
| `-insecure` | 서버 인증서·도메인 검증을 건너뜀 (안전하지 않음) |
| `-cacert <file>` | 서버 검증에 쓸 신뢰 루트 인증서 파일 |
| `-cert <file>` | 서버에 제시할 클라이언트 인증서(공개키) — mTLS |
| `-key <file>` | 서버에 제시할 클라이언트 개인키 — mTLS |
| `-authority <value>` | 원격 서버의 권위(authority) 이름 |
| `-servername <value>` | TLS 인증서 검증 시 사용할 서버 이름 재정의 |

### 로컬 평문 서버

```bash
grpcurl -plaintext localhost:8080 list
```

### 자체 서명 인증서 (검증 생략)

```bash
grpcurl -insecure grpc.server.com:443 list
```

### 커스텀 CA

```bash
grpcurl -cacert ./ca.pem grpc.server.com:443 list
```

### 상호 TLS (mTLS)

```bash
grpcurl -cacert ./ca.pem \
    -cert ./client.pem \
    -key ./client.key \
    grpc.server.com:443 my.custom.server.Service/Method
```

### Unix 도메인 소켓

```bash
grpcurl -plaintext -unix /tmp/grpc.sock list
```

---

## proto 소스와 protoset

리플렉션을 지원하지 않는 서버는 디스크립터를 직접 제공해야 합니다.

### proto 소스 파일 (-import-path, -proto)

- `-import-path <dir>`: proto import 해석에 쓸 디렉터리 (여러 번 지정 가능)
- `-proto <file>`: RPC 스키마 결정용 proto 소스 파일 (여러 번 지정 가능)

```bash
grpcurl -import-path ./protos -import-path ./third_party \
    -proto my_service.proto \
    -d '{"id": 1234}' \
    localhost:8080 my.custom.server.Service/Method
```

### protoset 파일 (-protoset)

컴파일된 `FileDescriptorSet`을 사용합니다. import를 모두 포함해 빌드해야 합니다.

```bash
# buf로 생성하는 경우
buf build -o my_service.protoset

# 사용
grpcurl -protoset my_service.protoset \
    -d '{"id": 1234}' \
    localhost:8080 my.custom.server.Service/Method
```

### 디스크립터 내보내기

리플렉션이나 proto 소스로 얻은 스키마를 파일로 추출할 수 있습니다.

```bash
# protoset으로 내보내기
grpcurl -protoset-out descriptors.protoset \
    grpc.server.com:443 describe my.custom.server.Service

# .proto 파일로 내보내기
grpcurl -proto-out-dir ./out \
    grpc.server.com:443 describe my.custom.server.Service
```

### 리플렉션 강제 사용 (-use-reflection)

proto 소스나 protoset을 지정하면서도 리플렉션을 함께 쓰려면:

```bash
grpcurl -proto extra.proto -use-reflection \
    grpc.server.com:443 list
```

---

## 주요 플래그 레퍼런스

전체 목록은 `grpcurl -help`로 확인할 수 있습니다.

| 플래그 | 타입 | 설명 |
|--------|------|------|
| `-help` | bool | 사용법 출력 후 종료 |
| `-version` | bool | 버전 출력 |
| `-plaintext` | bool | TLS 없이 평문 HTTP/2로 연결 |
| `-insecure` | bool | 서버 인증서·도메인 검증 생략 (안전하지 않음) |
| `-cacert` | string | 서버 검증용 신뢰 루트 인증서 파일 |
| `-cert` | string | 서버에 제시할 클라이언트 인증서(공개키) |
| `-key` | string | 서버에 제시할 클라이언트 개인키 |
| `-authority` | string | 원격 서버의 권위(authority) 이름 |
| `-servername` | string | TLS 인증서 검증 시 서버 이름 재정의 |
| `-unix` | bool | 주소를 TCP가 아닌 Unix 도메인 소켓 경로로 해석 |
| `-H` | 반복 | `'name: value'` 형식의 추가 헤더 (RPC·리플렉션 공통) |
| `-rpc-header` | 반복 | RPC 호출에만 붙는 추가 헤더 |
| `-reflect-header` | 반복 | 리플렉션 요청에만 붙는 추가 헤더 |
| `-expand-headers` | bool | 헤더 값에서 `${NAME}`으로 환경 변수 참조 허용 |
| `-user-agent` | string | grpc-go가 설정하는 User-Agent 헤더에 추가할 값 |
| `-d` | string | 요청 데이터(JSON), 또는 `@`로 stdin에서 읽기 |
| `-format` | string | 요청 데이터 형식: `json`(기본) 또는 `text` |
| `-format-error` | bool | 비정상 상태 응답도 `-format` 값으로 포맷 |
| `-allow-unknown-fields` | bool | JSON 요청에서 알 수 없는 필드 허용 |
| `-emit-defaults` | bool | JSON 응답에서 기본값 필드도 출력 |
| `-msg-template` | bool | 메시지 describe 시 입력 데이터 템플릿 표시 |
| `-connect-timeout` | float | 연결 수립 최대 대기 시간(초) |
| `-keepalive-time` | float | keepalive 프로브 전송 전 최대 유휴 시간(초) |
| `-max-time` | float | 작업 전체 최대 수행 시간(초) |
| `-max-msg-sz` | int | 응답 메시지 최대 인코딩 크기(바이트) |
| `-proto` | 반복 | RPC 스키마 결정용 proto 소스 파일 |
| `-import-path` | 반복 | proto import 해석용 디렉터리 |
| `-protoset` | 반복 | 인코딩된 FileDescriptorSet 파일 |
| `-protoset-out` | string | FileDescriptorSet proto를 기록할 파일 |
| `-proto-out-dir` | string | 생성된 `.proto` 파일을 기록할 디렉터리 |
| `-use-reflection` | bool | 서버 리플렉션으로 RPC 스키마 결정 |
| `-v` | bool | 자세한 출력 |
| `-vv` | bool | 매우 자세한 출력 (타이밍 데이터 포함) |
| `-alts` | bool | 연결에 ALTS(Application Layer Transport Security) 사용 |

---

## 자주 쓰는 예제 모음

### 로컬 평문 서버 디버깅 (리플렉션)

```bash
# 1. 서비스 목록 확인
grpcurl -plaintext localhost:8080 list

# 2. 특정 서비스 메서드 확인
grpcurl -plaintext localhost:8080 list grpc.health.v1.Health

# 3. 메서드 스키마 확인
grpcurl -plaintext localhost:8080 describe grpc.health.v1.Health.Check

# 4. 호출
grpcurl -plaintext -d '{"service": ""}' \
    localhost:8080 grpc.health.v1.Health/Check
```

### 단항(unary) 호출 (TLS + 인증)

```bash
grpcurl -H 'authorization: Bearer ${TOKEN}' -expand-headers \
    -d '{"id": 1234, "tags": ["foo", "bar"]}' \
    grpc.server.com:443 my.custom.server.Service/GetItem
```

### 서버 스트리밍 호출

```bash
grpcurl -plaintext -d '{"page_size": 50}' \
    localhost:8080 my.custom.server.Service/ListItems
```

### 클라이언트 스트리밍 (stdin으로 여러 메시지)

```bash
grpcurl -plaintext -d @ localhost:8080 my.custom.server.Service/Upload <<EOM
{"chunk": "aGVsbG8="}
{"chunk": "d29ybGQ="}
EOM
```

### 양방향 스트리밍 (대화형)

```bash
# 터미널에서 직접 입력하면 각 메시지가 즉시 전송됨
grpcurl -plaintext -d @ localhost:8080 my.custom.server.Service/Chat
```

### proto 파일만으로 호출 (리플렉션 미지원 서버)

```bash
grpcurl -import-path ./protos -proto my_service.proto \
    -d '{"id": 1234}' \
    grpc.server.com:443 my.custom.server.Service/GetItem
```

### 응답 기본값까지 출력

```bash
grpcurl -plaintext -emit-defaults -d '{}' \
    localhost:8080 my.custom.server.Service/GetStatus
```

### 타이밍 포함 디버그 출력

```bash
grpcurl -vv -plaintext -d '{"id": 1234}' \
    localhost:8080 my.custom.server.Service/GetItem
```

### Docker로 원격 서버 탐색

```bash
docker run fullstorydev/grpcurl api.grpc.me:443 list
```
