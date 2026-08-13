# gRPC 인증·보안, 에러 처리, 데드라인

## gRPC 인증과 보안 (Go)

> 원본: https://grpc.io/docs/guides/auth/

---

### 목차

1. [인증 메커니즘 개요](#인증-메커니즘-개요)
2. [자격증명 구조](#자격증명-구조)
3. [암호화 없는 연결](#암호화-없는-연결)
4. [TLS/SSL](#tlsssl)
5. [Call Credentials와 토큰 인증](#call-credentials와-토큰-인증)
6. [Channel과 Call Credentials 결합](#channel과-call-credentials-결합)
7. [ALTS](#alts)
8. [보안 주의사항](#보안-주의사항)

---

### 인증 메커니즘 개요

gRPC는 세 가지 인증 방식을 기본 제공함.

- SSL/TLS: 서버 인증과 클라이언트-서버 간 데이터 암호화 → gRPC는 SSL/TLS 사용을 강력히 권장
- ALTS(Application Layer Transport Security): Google Compute Engine · Google Kubernetes Engine에서 실행되는 애플리케이션을 위한 상호 인증
- 토큰 기반 인증: OAuth2 토큰 등 Google 또는 자체 인증 메커니즘을 통한 API 접근

---

### 자격증명 구조

gRPC는 두 종류의 자격증명(credentials)을 구분.

- Channel credentials(채널 자격증명): 채널에 부착
  - 예: SSL/TLS 자격증명
  - 연결 단위의 암호화·인증 담당
- Call credentials(호출 자격증명): 개별 RPC 호출에 부착
  - 예: 매 요청에 붙는 OAuth2 토큰
  - 요청 단위의 인증 담당

두 자격증명은 결합(composite) 가능 → 채널 수준 암호화(TLS)와 호출 수준 인증(토큰)을 함께 적용하는 패턴이 일반적.

---

### 암호화 없는 연결

개발/내부 환경에서는 암호화 없이 연결 가능. 최신 gRPC-Go에서는 반드시 트랜스포트 자격증명을 명시해야 하므로 `insecure`를 사용.

```go
import "google.golang.org/grpc/credentials/insecure"

conn, err := grpc.NewClient("localhost:50051",
    grpc.WithTransportCredentials(insecure.NewCredentials()))
```

프로덕션에서는 사용하지 않는 것이 원칙.

---

### TLS/SSL

#### 클라이언트 (서버 인증서 검증)

서버 인증서 검증에 사용할 루트 CA 인증서(`roots.pem`)로 클라이언트 자격증명 생성. 두 번째 인자는 서버 이름 오버라이드(server name override)로, 보통 빈 문자열 전달.

```go
import "google.golang.org/grpc/credentials"

creds, err := credentials.NewClientTLSFromFile("roots.pem", "")
if err != nil {
    log.Fatalf("failed to load credentials: %v", err)
}
conn, err := grpc.NewClient("myservice.example.com:443",
    grpc.WithTransportCredentials(creds))
```

#### 서버 측 TLS

서버 인증서(`cert.pem`)와 개인키(`key.pem`)로 서버 자격증명을 만들고 `grpc.Creds` 옵션에 전달.

```go
creds, err := credentials.NewServerTLSFromFile("cert.pem", "key.pem")
if err != nil {
    log.Fatalf("failed to load credentials: %v", err)
}
lis, err := net.Listen("tcp", ":50051")
if err != nil {
    log.Fatalf("failed to listen: %v", err)
}
server := grpc.NewServer(grpc.Creds(creds))
```

mTLS(상호 TLS) 등 더 세밀한 설정이 필요하면 `credentials.NewTLS(*tls.Config)`로 `tls.Config`를 직접 구성.

---

### Call Credentials와 토큰 인증

토큰(OAuth2, JWT 등)은 항상 per-call(호출 단위) 자격증명으로 사용. Go에서는 `credentials.PerRPCCredentials` 인터페이스를 직접 구현하거나 `oauth` 헬퍼를 사용해 매 호출에 자격증명 첨부.

```go
import "google.golang.org/grpc/credentials/oauth"
import "golang.org/x/oauth2"

perRPC := oauth.TokenSource{
    TokenSource: oauth2.StaticTokenSource(&oauth2.Token{AccessToken: token}),
}

conn, err := grpc.NewClient("myservice.example.com:443",
    grpc.WithTransportCredentials(creds),       // 채널 자격증명(TLS)
    grpc.WithPerRPCCredentials(perRPC),         // 호출 자격증명(토큰)
)
```

`grpc.WithPerRPCCredentials`로 채널 전체에 적용하거나, 개별 호출 시 `grpc.PerRPCCredentials(...)` 호출 옵션으로 지정 가능. 이 자격증명은 RPC마다 `authorization` 메타데이터를 자동으로 삽입.

> 참고: per-RPC credentials를 평문(insecure) 채널에서 전송하려면 별도 허용 설정 필요. 토큰 유출을 막기 위해 토큰 기반 인증은 TLS 채널과 함께 사용하는 것이 원칙.

---

### Channel과 Call Credentials 결합

`grpc.WithTransportCredentials`(채널)와 `grpc.WithPerRPCCredentials`(호출)를 함께 지정하면 채널 수준 암호화와 호출 수준 인증을 동시에 적용 가능. TLS로 보호된 연결 위에 매 요청마다 토큰을 첨부하는 이 패턴이 가장 일반적.

---

### ALTS

ALTS는 Google 인프라(GCE/GKE)에서 동작하는 상호 인증·암호화 프로토콜. 동일 환경 내 서비스 간 통신에서 인증서 관리 없이 신원 기반 인증 제공.

```go
import "google.golang.org/grpc/credentials/alts"

// 클라이언트
altsCreds := alts.NewClientCreds(alts.DefaultClientOptions())
conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(altsCreds))

// 서버
altsServerCreds := alts.NewServerCreds(alts.DefaultServerOptions())
server := grpc.NewServer(grpc.Creds(altsServerCreds))
```

ALTS는 해당 Google 환경 밖에서는 정상 동작 불가.

---

### 보안 주의사항

- Google이 발급한 OAuth2 토큰은 Google 서비스 접속에만 사용 → Google 토큰을 Google이 아닌 서비스로 보내면 토큰이 탈취되어 클라이언트를 사칭하는 데 악용될 수 있음
- 프로덕션에서는 암호화 없는 연결 지양, TLS 사용
- 토큰/자격증명은 항상 암호화된 채널로 전송

---

---

## gRPC 에러 처리, 데드라인, 신뢰성 기능 (Go)

> 원본: https://grpc.io/docs/guides/error/ , https://grpc.io/docs/guides/status-codes/ , https://grpc.io/docs/guides/deadlines/ , https://grpc.io/docs/guides/cancellation/ , https://grpc.io/docs/guides/retry/ , https://grpc.io/docs/guides/keepalive/ , https://grpc.io/docs/guides/wait-for-ready/ , https://grpc.io/docs/guides/health-checking/ , https://grpc.io/docs/guides/reflection/ , https://grpc.io/docs/guides/compression/

---

### 목차

1. [에러 모델](#에러-모델)
2. [상태 코드 전체 목록](#상태-코드-전체-목록)
3. [Go에서 에러 생성과 처리](#go에서-에러-생성과-처리)
4. [데드라인과 타임아웃](#데드라인과-타임아웃)
5. [RPC 취소](#rpc-취소)
6. [재시도(Retry)](#재시도retry)
7. [Keepalive](#keepalive)
8. [Wait-for-Ready](#wait-for-ready)
9. [헬스 체킹](#헬스-체킹)
10. [서버 리플렉션](#서버-리플렉션)
11. [압축(Compression)](#압축compression)

---

### 에러 모델

호출 성공 시 서버는 `OK` 상태 반환. 실패 시에는 상태 코드(status code)와 선택적 에러 메시지(error message) 반환.

- 표준 에러 모델(standard error model): 상태 코드 + 메시지 → 모든 gRPC 라이브러리에서 지원
- 확장 에러 모델(richer error model): protobuf를 사용하는 경우, 트레일링 메타데이터에 하나 이상의 protobuf 메시지로 추가 에러 상세를 담을 수 있음(`google.rpc.Status`, `google.rpc.ErrorInfo` 등)
  - C++, Go, Java, Python, Ruby에서 지원
  - 단, 표준 HTTP 처리기에서는 상세를 볼 수 없고, 페이로드가 크면 HTTP/2 헤더 압축 효율이 떨어질 수 있으므로 주의 필요

---

### 상태 코드 전체 목록

- `OK` (0): 에러 아님, 성공 시 반환
- `CANCELLED` (1): 일반적으로 호출자가 작업을 취소함
- `UNKNOWN` (2): 알 수 없는 에러
  - 예: 다른 주소 공간에서 받은 상태값이 이 공간에서 모르는 에러 공간에 속할 때
- `INVALID_ARGUMENT` (3): 클라이언트가 잘못된 인자를 지정함
  - 시스템 상태와 무관하게 문제인 인자(예: 잘못된 파일 이름)
  - `FAILED_PRECONDITION`과 구분됨
- `DEADLINE_EXCEEDED` (4): 작업 완료 전에 데드라인이 만료됨
  - 상태를 변경하는 작업은 실제로 성공했더라도 이 에러가 반환될 수 있음
- `NOT_FOUND` (5): 요청한 엔티티(예: 파일/디렉터리)를 찾을 수 없음
- `ALREADY_EXISTS` (6): 생성하려는 엔티티가 이미 존재함
- `PERMISSION_DENIED` (7): 호출자가 해당 작업을 실행할 권한이 없음
- `RESOURCE_EXHAUSTED` (8): 자원이 고갈됨
  - 사용자별 할당량 초과, 디스크 공간 부족 등
- `FAILED_PRECONDITION` (9): 작업에 필요한 시스템 상태가 아니라 거부됨
- `ABORTED` (10): 동시성 문제(시퀀서 검사 실패, 트랜잭션 중단 등)로 작업이 중단됨
- `OUT_OF_RANGE` (11): 유효 범위를 벗어나 작업을 시도함(예: 파일 끝을 넘어 읽기)
- `UNIMPLEMENTED` (12): 해당 작업이 구현되지 않았거나 이 서비스에서 지원·활성화되지 않음
- `INTERNAL` (13): 내부 에러, 시스템이 기대하는 불변식(invariant)이 깨짐
- `UNAVAILABLE` (14): 서비스가 현재 사용 불가
  - 대개 일시적이며 백오프(backoff)와 함께 재시도하면 해결될 수 있음
- `DATA_LOSS` (15): 복구 불가능한 데이터 손실 또는 손상
- `UNAUTHENTICATED` (16): 작업에 유효한 인증 자격증명이 없음

---

### Go에서 에러 생성과 처리

`google.golang.org/grpc/status`와 `google.golang.org/grpc/codes` 패키지 사용.

#### 서버: 에러 반환

```go
import (
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
)

func (s *server) GetUser(ctx context.Context, in *pb.GetUserRequest) (*pb.User, error) {
    u, ok := s.users[in.GetId()]
    if !ok {
        // 코드 + 메시지로 상태 에러 생성
        return nil, status.Errorf(codes.NotFound, "user %q not found", in.GetId())
    }
    return u, nil
}
```

#### 클라이언트: 에러 해석

```go
resp, err := client.GetUser(ctx, req)
if err != nil {
    st, ok := status.FromError(err)
    if ok {
        switch st.Code() {
        case codes.NotFound:
            log.Printf("없는 사용자: %s", st.Message())
        case codes.DeadlineExceeded:
            log.Printf("타임아웃")
        default:
            log.Printf("RPC 에러: %v", st.Code())
        }
    }
    return
}
```

#### 확장 에러 모델 (상세 첨부)

```go
st := status.New(codes.InvalidArgument, "잘못된 요청")
st, _ = st.WithDetails(&errdetails.BadRequest{
    FieldViolations: []*errdetails.BadRequest_FieldViolation{{
        Field:       "email",
        Description: "형식이 올바르지 않습니다",
    }},
})
return nil, st.Err()
```

클라이언트는 `st.Details()`로 상세 메시지를 꺼내 타입 단언으로 처리.

---

### 데드라인과 타임아웃

데드라인(deadline)은 클라이언트가 응답을 기다리는 절대 시각, 타임아웃(timeout)은 허용 최대 기간. 타임아웃은 호출 시작 시점에 데드라인으로 변환됨.

gRPC는 기본적으로 데드라인을 설정하지 않으므로, 클라이언트는 항상 현실적인 데드라인을 명시하는 것을 권장. 데드라인이 지나면 RPC는 `DEADLINE_EXCEEDED`로 실패.

#### 클라이언트: 데드라인 설정

```go
ctx, cancel := context.WithTimeout(context.Background(), 200*time.Millisecond)
defer cancel()

resp, err := client.SayHello(ctx, req)
if err != nil {
    if status.Code(err) == codes.DeadlineExceeded {
        log.Println("데드라인 초과")
    }
}
```

`context.WithDeadline(parent, t)`로 절대 시각을 직접 지정 가능.

#### 서버: 데드라인 확인

데드라인이 지나면 서버 측 RPC는 자동으로 취소되지만, 애플리케이션은 직접 진행 중인 작업을 중단해야 함. 장시간 작업에서는 주기적으로 컨텍스트 확인 필요.

```go
func (s *server) LongOp(ctx context.Context, in *pb.Req) (*pb.Resp, error) {
    for i := 0; i < n; i++ {
        if err := ctx.Err(); err != nil {
            // context.DeadlineExceeded 또는 context.Canceled
            return nil, status.FromContextError(err).Err()
        }
        // ... 작업의 한 단위 ...
    }
    return &pb.Resp{}, nil
}
```

#### 데드라인 전파(propagation)

서버가 다른 서비스의 클라이언트로 동작할 때는 원래 데드라인을 전파해야 함. Go에서는 컨텍스트를 그대로 넘기면 자동으로 전파됨. gRPC는 전파 시 데드라인을 타임아웃으로 변환해 경과 시간을 반영함으로써 서버 간 시계 오차(clock skew) 문제를 방지.

---

### RPC 취소

클라이언트나 서버는 언제든 RPC 취소 가능. 취소 전에 이루어진 변경은 롤백되지 않음.

#### 클라이언트: 취소

```go
ctx, cancel := context.WithCancel(context.Background())
go func() {
    time.Sleep(50 * time.Millisecond)
    cancel() // 진행 중인 RPC 취소
}()
_, err := client.LongOp(ctx, req)
if status.Code(err) == codes.Canceled {
    log.Println("호출이 취소됨")
}
```

#### 서버: 취소 감지

gRPC 라이브러리는 애플리케이션 핸들러를 강제로 중단하지 못함. 따라서 장시간 실행되는 핸들러는 주기적으로 `ctx.Done()` 또는 `ctx.Err()`를 확인하고, 취소 시 스스로 처리를 멈춰야 함. Go에서는 핸들러가 생성한 아웃고잉 RPC도 컨텍스트를 통해 자동으로 취소됨.

---

### 재시도(Retry)

재시도는 서비스 신뢰성을 높이는 핵심 패턴. gRPC는 서비스 설정(service config)을 통해 메서드 단위로 재시도 정책 정의.

```json
{
  "methodConfig": [{
    "name": [{"service": "helloworld.Greeter"}],
    "retryPolicy": {
      "maxAttempts": 4,
      "initialBackoff": "0.1s",
      "maxBackoff": "1s",
      "backoffMultiplier": 2,
      "retryableStatusCodes": ["UNAVAILABLE"]
    }
  }]
}
```

- `maxAttempts`: 최대 시도 횟수
- `initialBackoff` / `maxBackoff` / `backoffMultiplier`: 지수 백오프(exponential backoff) 설정
  - 백오프에는 ±20% 지터(jitter)가 적용되어 재시도가 한꺼번에 몰리는 것을 방지
- `retryableStatusCodes`: 재시도를 유발하는 상태 코드 목록

Go에서는 `grpc.WithDefaultServiceConfig`로 JSON 설정을 채널에 적용.

```go
const serviceConfig = `{ ... 위 JSON ... }`

conn, err := grpc.NewClient(addr,
    grpc.WithTransportCredentials(insecure.NewCredentials()),
    grpc.WithDefaultServiceConfig(serviceConfig),
)
```

별도 정책 없이도 gRPC는 요청이 클라이언트를 벗어나지 않은 장애나, 서버가 요청을 수신했지만 애플리케이션 로직이 처리하기 전 단계의 실패에 대해 투명 재시도(transparent retry) 수행. 헤징(hedging)은 재시도의 보완 기능으로, 응답을 기다리지 않고 동시에 여러 요청 전송.

---

### Keepalive

Keepalive는 데이터 전송이 없는 유휴(idle) 구간에서도 HTTP/2 PING 프레임으로 연결 유지. 끊어진 연결을 빠르게 감지하는 데도 활용됨.

#### 클라이언트

```go
import "google.golang.org/grpc/keepalive"

kp := keepalive.ClientParameters{
    Time:                10 * time.Second, // PING 전송 간격
    Timeout:             1 * time.Second,  // PING ACK 대기 시간
    PermitWithoutStream: true,             // 활성 스트림이 없어도 PING
}
conn, err := grpc.NewClient(addr,
    grpc.WithTransportCredentials(insecure.NewCredentials()),
    grpc.WithKeepaliveParams(kp),
)
```

#### 서버

```go
sp := keepalive.ServerParameters{
    Time:    20 * time.Second,
    Timeout: 10 * time.Second,
}
ep := keepalive.EnforcementPolicy{
    MinTime:             5 * time.Second, // 클라이언트 PING 최소 간격
    PermitWithoutStream: true,
}
server := grpc.NewServer(
    grpc.KeepaliveParams(sp),
    grpc.KeepaliveEnforcementPolicy(ep),
)
```

주의: keepalive 간격을 너무 짧게 설정하면 서버에 과도한 부하를 줄 수 있음. 서버는 과도한 PING에 대해 GOAWAY로 연결을 끊을 수 있으므로, 서비스 소유자와 설정 조율 필요.

---

### Wait-for-Ready

기본 동작에서는 채널이 서버에 연결되지 않은 상태에서 RPC를 호출하면 즉시 실패. Wait-for-Ready를 활성화하면 연결이 준비될 때까지 RPC가 큐에 대기(기본값: 비활성).

```go
resp, err := client.SayHello(ctx, req, grpc.WaitForReady(true))
```

- READY 상태면 즉시 전송, IDLE/CONNECTING/TRANSIENT_FAILURE면 준비될 때까지 대기
- 영구 실패(permanent failure)는 설정과 무관하게 즉시 실패
- 데드라인은 여전히 적용되므로 데드라인이 지나면 대기 중단 → 연결 외 다른 이유로도 실패할 수 있으므로 에러 처리 여전히 필요

---

### 헬스 체킹

gRPC는 표준 헬스 체크 서비스 API(`grpc.health.v1.Health`) 정의 → 단방향 `Check` RPC와 서버 스트리밍 `Watch` RPC 제공.

```proto
service Health {
  rpc Check(HealthCheckRequest) returns (HealthCheckResponse);
  rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
}

message HealthCheckResponse {
  enum ServingStatus {
    UNKNOWN = 0;
    SERVING = 1;
    NOT_SERVING = 2;
    SERVICE_UNKNOWN = 3;
  }
  ServingStatus status = 1;
}
```

Go에서는 `google.golang.org/grpc/health`와 `health/grpc_health_v1` 패키지로 헬스 서버 등록. 서비스 이름이 빈 문자열(`""`)이면 서버 전체의 상태를 의미.

```go
import (
    "google.golang.org/grpc/health"
    healthpb "google.golang.org/grpc/health/grpc_health_v1"
)

healthServer := health.NewServer()
healthpb.RegisterHealthServer(grpcServer, healthServer)

// 서비스 상태 설정
healthServer.SetServingStatus("helloworld.Greeter", healthpb.HealthCheckResponse_SERVING)

// 종료 시 NOT_SERVING으로 전환
healthServer.Shutdown()
```

클라이언트는 서비스 설정의 `healthCheckConfig.serviceName`으로 헬스 체크를 활성화하면 연결 시 `Watch`를 호출해 `SERVING` 상태가 될 때까지 요청 보류.

---

### 서버 리플렉션

서버 리플렉션(server reflection)은 서버가 노출하는 protobuf API를 표준 RPC 서비스로 제공하는 프로토콜. 클라이언트가 `.proto` 정의를 미리 보유하지 않아도 요청을 인코딩/디코딩 가능. `grpcurl`, Postman 등 디버깅 도구가 이를 활용.

Go에서는 `google.golang.org/grpc/reflection` 패키지로 한 줄로 활성화.

```go
import "google.golang.org/grpc/reflection"

grpcServer := grpc.NewServer()
pb.RegisterGreeterServer(grpcServer, &server{})
reflection.Register(grpcServer) // 리플렉션 활성화
```

주의: 공개 API에서 리플렉션을 노출하면 내부 API 구조가 드러날 수 있으므로 보안상 권장되지 않음. 리플렉션은 기본적으로 비활성이며 명시적으로 활성화 필요.

---

### 압축(Compression)

압축은 피어 간 통신 대역폭을 줄임. 호출 또는 메시지 단위로 활성화/비활성화 가능. 요청과 응답에 서로 다른 압축 방식을 적용하는 비대칭 압축(asymmetric compression)도 가능.

Go에서는 gzip 압축기를 임포트해 등록하고, 호출 옵션으로 압축기 지정.

```go
import (
    "google.golang.org/grpc/encoding/gzip" // init()에서 gzip 압축기 자동 등록
)

// 클라이언트: 이 호출의 요청을 gzip으로 압축
resp, err := client.SayHello(ctx, req, grpc.UseCompressor(gzip.Name))
```

서버는 등록된 압축기로 압축된 요청을 자동 해제하고, 클라이언트가 요청한 압축 방식으로 응답. 클라이언트가 서버에서 지원하지 않는 알고리즘으로 압축하면 서버는 `UNIMPLEMENTED` 에러를 반환하고 `grpc-accept-encoding` 헤더로 지원하는 알고리즘 목록을 알려줌.

참고: 압축은 BEAST/CRIME 같은 공격의 표면이 될 수 있어, 보안에 민감한 환경에서는 의도적으로 비활성화하기도 함.

---
