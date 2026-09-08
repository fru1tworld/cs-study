# grpcurl

> 원본: https://github.com/fullstorydev/grpcurl

<a id="grpcurl이란"></a>
## grpcurl이란

`grpcurl`은 명령줄에서 gRPC 서버를 호출하는 도구다. HTTP 서버에 `curl`로 요청하듯이 gRPC 서버에 요청을 보낼 수 있다.

gRPC는 전송할 메시지를 Protocol Buffers 바이너리 형식으로 인코딩하므로 일반 `curl` 요청처럼 호출할 수 없다. `grpcurl`은 JSON 요청을 protobuf 바이너리로 변환해 전송하고, 응답은 다시 JSON으로 바꿔 출력한다.

이 변환을 직접 구현하지 않고도 다음 작업을 할 수 있다.

- 명령줄에서 gRPC 메서드 호출 및 테스트
- 서버가 노출하는 서비스/메서드/메시지 스키마 탐색
- 단항(unary)뿐 아니라 클라이언트/서버/양방향 스트리밍 메서드 호출
- 로컬 개발 중 gRPC 서버 디버깅

메시지를 변환하려면 스키마 정보가 필요하다. `grpcurl`은 서버 리플렉션, `.proto` 소스 파일, 컴파일된 protoset 파일 중 하나에서 이 정보를 얻는다.

<a id="설치"></a>

## 설치

### Homebrew (macOS)

```bash
brew install grpcurl
```

### go install

```bash
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
```

- 설치 후 `$GOBIN`(보통 `$HOME/go/bin`)이 `PATH`에 포함되어 있어야 함

### 사전 빌드 바이너리

- [releases 페이지](https://github.com/fullstorydev/grpcurl/releases)에서 OS/아키텍처에 맞는 바이너리를 내려받아 `PATH`에 등록

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

- 설치 확인:

```bash
grpcurl -version
```

<a id="서비스-디스커버리"></a>

## 서비스 디스커버리

RPC 스키마를 얻는 방법은 세 가지다. 별도 파일을 지정하지 않으면 서버 리플렉션을 사용하고, proto 소스나 protoset을 지정하면 해당 디스크립터를 사용한다.

### 1. 서버 리플렉션 (Server Reflection)

- 서버가 gRPC 리플렉션 서비스를 지원하면 별도 플래그 없이 자동 사용 → 가장 간편한 방법

```bash
grpcurl grpc.server.com:443 list
```

proto 소스나 protoset을 지정하면 리플렉션은 기본적으로 비활성화된다. 이 경우에도 서버 리플렉션을 함께 사용하려면 `-use-reflection`을 명시한다.

### 2. proto 소스 파일

- 서버가 리플렉션을 지원하지 않을 때 `.proto` 파일을 직접 지정

```bash
grpcurl -import-path ./protos -proto my_service.proto \
    grpc.server.com:443 list
```

### 3. protoset 파일

- `protoc`/`buf`로 미리 컴파일한 `FileDescriptorSet`(protoset) 사용

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

<a id="서비스메서드-목록-조회-list-describe"></a>

## 서비스/메서드 목록 조회 (list, describe)

### list: 서비스/메서드 나열

- 서버가 노출하는 모든 서비스 나열:

```bash
grpcurl localhost:8787 list
```

- 특정 서비스의 모든 메서드 나열:

```bash
grpcurl localhost:8787 list my.custom.server.Service
```

### describe: 타입/스키마 상세 조회

- 서비스, 메서드, 메시지, 필드 등 심볼의 스키마 출력

```bash
# 서비스 전체 설명
grpcurl localhost:8787 describe my.custom.server.Service

# 특정 메서드 설명
grpcurl localhost:8787 describe my.custom.server.Service.MethodOne

# 메시지 타입 설명
grpcurl localhost:8787 describe .my.custom.server.MyRequest
```

- `-msg-template`과 함께 쓰면 메시지를 설명할 때 입력 데이터의 템플릿(빈 JSON 골격)도 함께 표시

```bash
grpcurl -msg-template localhost:8787 describe .my.custom.server.MyRequest
```

<a id="rpc-호출"></a>

## RPC 호출

- 호출은 `grpcurl [flags] <서버주소> <서비스>/<메서드>` 형식
  - 서비스와 메서드 사이 구분자는 `/` 또는 `.` 모두 사용 가능

### 빈 요청 호출

```bash
# TLS 서버
grpcurl grpc.server.com:443 my.custom.server.Service/Method

# 평문(non-TLS) 서버
grpcurl -plaintext localhost:8080 my.custom.server.Service/Method
```

### 요청 데이터 전달 (-d)

- 요청 메시지는 JSON으로 전달

```bash
grpcurl -d '{"id": 1234, "tags": ["foo", "bar"]}' \
    grpc.server.com:443 my.custom.server.Service/Method
```

### stdin에서 읽기 (-d @)

- `-d @`를 주면 표준 입력에서 요청 본문을 읽음 → 큰 페이로드나 파이프에 유용

```bash
grpcurl -d @ grpc.server.com:443 my.custom.server.Service/Method <<EOM
{
  "id": 1234,
  "tags": ["foo", "bar"]
}
EOM
```

- 파일에서 읽기:

```bash
grpcurl -d @ grpc.server.com:443 my.custom.server.Service/Method < request.json
```

### 스트리밍

- `grpcurl`은 단항뿐 아니라 모든 스트리밍 메서드 지원

- 클라이언트/서버 스트리밍: `-d`에 여러 JSON 메시지를 이어서 작성하면, 각 메시지가 클라이언트 스트림의 개별 요청으로 전송됨

```bash
grpcurl -d @ localhost:8080 my.custom.server.Service/ClientStream <<EOM
{"value": 1}
{"value": 2}
{"value": 3}
EOM
```

- 양방향 스트리밍: 대화형 터미널에서 `grpcurl`을 실행하면 stdin이 요청 입력으로 연결되어, 타이핑하는 메시지가 즉시 전송됨

<a id="메타데이터헤더와-인증"></a>

## 메타데이터/헤더와 인증

### 헤더 추가 (-H)

- 요청 메타데이터/헤더는 `-H 'name: value'` 형식으로 추가하며, 여러 번 지정 가능

```bash
grpcurl -H 'header1: value1' -H 'header2: value2' \
    -d '{"id": 1234}' \
    grpc.server.com:443 my.custom.server.Service/Method
```

`-H`로 추가한 헤더는 RPC 요청과 리플렉션 요청에 모두 붙는다. 두 요청에 서로 다른 헤더가 필요하면 다음 플래그로 구분한다.

- `-rpc-header`: 실제 RPC 호출에만 붙는 헤더
- `-reflect-header`: 리플렉션 요청에만 붙는 헤더

```bash
grpcurl -rpc-header 'foo: bar' -reflect-header 'baz: qux' \
    localhost:8080 my.custom.server.Service/Method
```

### 환경 변수 확장 (-expand-headers)

- `-expand-headers`를 주면 헤더 값에서 `${NAME}` 구문으로 환경 변수 참조 가능

```bash
export TOKEN="ey..."
grpcurl -expand-headers \
    -H 'authorization: Bearer ${TOKEN}' \
    grpc.server.com:443 my.custom.server.Service/Method
```

### 인증 (Bearer 토큰)

- gRPC 인증은 보통 메타데이터로 전달 → 헤더로 구현

```bash
grpcurl -H 'authorization: Bearer eyJhbGciOi...' \
    grpc.server.com:443 my.custom.server.Service/Method
```

- 인증 토큰처럼 민감한 헤더를 평문(`-plaintext`)으로 전송하면 노출 위험 → 운영 환경에서는 반드시 TLS 사용 필요

<a id="tls-옵션"></a>

## TLS 옵션

`grpcurl`은 기본적으로 TLS로 연결한다. 평문 연결이 필요하거나 인증서 검증 방식을 바꾸려면 다음 플래그를 사용한다.

- `-plaintext`: TLS 없이 평문 HTTP/2로 연결, 개발/로컬 디버깅용
- `-insecure`: 서버 인증서, 도메인 검증을 건너뜀, 안전하지 않음
- `-cacert <file>`: 서버 검증에 쓸 신뢰 루트 인증서 파일
- `-cert <file>`: 서버에 제시할 클라이언트 인증서(공개키), mTLS용
- `-key <file>`: 서버에 제시할 클라이언트 개인키, mTLS용
- `-authority <value>`: 원격 서버의 권위(authority) 이름
- `-servername <value>`: TLS 인증서 검증 시 사용할 서버 이름 재정의

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

<a id="proto-소스와-protoset"></a>

## proto 소스와 protoset

서버가 리플렉션을 지원하지 않으면 `grpcurl`이 서버에서 스키마를 가져올 수 없다. 이때는 다음처럼 proto 소스나 컴파일된 protoset으로 디스크립터를 직접 제공한다.

### proto 소스 파일 (-import-path, -proto)

- `-import-path <dir>`: proto import 해석에 쓸 디렉터리, 여러 번 지정 가능
- `-proto <file>`: RPC 스키마 결정용 proto 소스 파일, 여러 번 지정 가능

```bash
grpcurl -import-path ./protos -import-path ./third_party \
    -proto my_service.proto \
    -d '{"id": 1234}' \
    localhost:8080 my.custom.server.Service/Method
```

### protoset 파일 (-protoset)

- 컴파일된 `FileDescriptorSet` 사용
  - import를 모두 포함해 빌드 필요

```bash
# buf로 생성하는 경우
buf build -o my_service.protoset

# 사용
grpcurl -protoset my_service.protoset \
    -d '{"id": 1234}' \
    localhost:8080 my.custom.server.Service/Method
```

### 디스크립터 내보내기

- 리플렉션이나 proto 소스로 얻은 스키마를 파일로 추출 가능

```bash
# protoset으로 내보내기
grpcurl -protoset-out descriptors.protoset \
    grpc.server.com:443 describe my.custom.server.Service

# .proto 파일로 내보내기
grpcurl -proto-out-dir ./out \
    grpc.server.com:443 describe my.custom.server.Service
```

### 리플렉션 강제 사용 (-use-reflection)

- proto 소스나 protoset을 지정하면서도 리플렉션을 함께 쓰려면 다음과 같이 지정:

```bash
grpcurl -proto extra.proto -use-reflection \
    grpc.server.com:443 list
```

<a id="주요-플래그-레퍼런스"></a>

## 주요 플래그 레퍼런스

- 전체 목록은 `grpcurl -help`로 확인 가능

- `-help` (bool): 사용법 출력 후 종료
- `-version` (bool): 버전 출력
- `-plaintext` (bool): TLS 없이 평문 HTTP/2로 연결
- `-insecure` (bool): 서버 인증서, 도메인 검증 생략, 안전하지 않음
- `-cacert` (string): 서버 검증용 신뢰 루트 인증서 파일
- `-cert` (string): 서버에 제시할 클라이언트 인증서(공개키)
- `-key` (string): 서버에 제시할 클라이언트 개인키
- `-authority` (string): 원격 서버의 권위(authority) 이름
- `-servername` (string): TLS 인증서 검증 시 서버 이름 재정의
- `-unix` (bool): 주소를 TCP가 아닌 Unix 도메인 소켓 경로로 해석
- `-H` (반복): `'name: value'` 형식의 추가 헤더, RPC, 리플렉션 공통
- `-rpc-header` (반복): RPC 호출에만 붙는 추가 헤더
- `-reflect-header` (반복): 리플렉션 요청에만 붙는 추가 헤더
- `-expand-headers` (bool): 헤더 값에서 `${NAME}`으로 환경 변수 참조 허용
- `-user-agent` (string): grpc-go가 설정하는 User-Agent 헤더에 추가할 값
- `-d` (string): 요청 데이터(JSON), 또는 `@`로 stdin에서 읽기
- `-format` (string): 요청 데이터 형식으로 `json`(기본) 또는 `text` 지정
- `-format-error` (bool): 비정상 상태 응답도 `-format` 값으로 포맷
- `-allow-unknown-fields` (bool): JSON 요청에서 알 수 없는 필드 허용
- `-emit-defaults` (bool): JSON 응답에서 기본값 필드도 출력
- `-msg-template` (bool): 메시지 describe 시 입력 데이터 템플릿 표시
- `-connect-timeout` (float): 연결 수립 최대 대기 시간(초)
- `-keepalive-time` (float): keepalive 프로브 전송 전 최대 유휴 시간(초)
- `-max-time` (float): 작업 전체 최대 수행 시간(초)
- `-max-msg-sz` (int): 응답 메시지 최대 인코딩 크기(바이트)
- `-proto` (반복): RPC 스키마 결정용 proto 소스 파일
- `-import-path` (반복): proto import 해석용 디렉터리
- `-protoset` (반복): 인코딩된 FileDescriptorSet 파일
- `-protoset-out` (string): FileDescriptorSet proto를 기록할 파일
- `-proto-out-dir` (string): 생성된 `.proto` 파일을 기록할 디렉터리
- `-use-reflection` (bool): 서버 리플렉션으로 RPC 스키마 결정
- `-v` (bool): 자세한 출력
- `-vv` (bool): 매우 자세한 출력, 타이밍 데이터 포함
- `-alts` (bool): 연결에 ALTS(Application Layer Transport Security) 사용

<a id="자주-쓰는-예제-모음"></a>

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
