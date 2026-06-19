# gRPC 메타데이터와 인터셉터 (Go)

> 이 문서는 gRPC 공식 문서의 "Metadata", "Interceptors" 가이드를 Go 중심으로 정리한 것입니다.
> 원본: https://grpc.io/docs/guides/metadata/ , https://grpc.io/docs/guides/interceptors/

---

## 목차

1. [메타데이터란](#메타데이터란)
2. [메타데이터 송신 (클라이언트)](#메타데이터-송신-클라이언트)
3. [메타데이터 수신 (서버)](#메타데이터-수신-서버)
4. [헤더와 트레일러](#헤더와-트레일러)
5. [인터셉터란](#인터셉터란)
6. [서버 인터셉터](#서버-인터셉터)
7. [클라이언트 인터셉터](#클라이언트-인터셉터)
8. [인터셉터 등록과 체이닝](#인터셉터-등록과-체이닝)

---

## 메타데이터란

메타데이터(metadata)는 RPC에 부가되는 정보를 키-값 쌍으로 주고받는 부가 채널(side channel)입니다. 인증 토큰, 추적 ID(trace ID), 로드 밸런싱/레이트 리밋 힌트 등에 사용합니다. 내부적으로 HTTP/2 헤더로 전송됩니다.

- 키는 대소문자를 구분하지 않으며 ASCII 문자/숫자와 `-`, `_`, `.`로 구성됩니다.
- 키는 `grpc-` 접두사로 시작할 수 없습니다(gRPC 예약).
- 값이 바이너리면 키에 `-bin` 접미사를 붙입니다. 이 경우 gRPC가 base64 인코딩을 자동 처리합니다.

Go에서는 `google.golang.org/grpc/metadata` 패키지를 사용합니다.

```go
import "google.golang.org/grpc/metadata"

// 생성 방법 1: 키-값 가변 인자
md := metadata.Pairs(
    "authorization", "bearer "+token,
    "request-id", "abc-123",
)

// 생성 방법 2: map으로 생성
md = metadata.New(map[string]string{
    "authorization": "bearer " + token,
})
```

---

## 메타데이터 송신 (클라이언트)

클라이언트는 메타데이터를 컨텍스트(context)에 실어 보냅니다. `metadata.NewOutgoingContext`로 아웃고잉 메타데이터를 설정하거나, `metadata.AppendToOutgoingContext`로 추가합니다.

```go
md := metadata.Pairs("authorization", "bearer "+token)
ctx := metadata.NewOutgoingContext(context.Background(), md)

// 또는 기존 컨텍스트에 키-값을 덧붙이기
ctx = metadata.AppendToOutgoingContext(ctx, "request-id", "abc-123")

resp, err := client.SayHello(ctx, &pb.HelloRequest{Name: "Alice"})
```

서버가 보낸 헤더/트레일러 메타데이터를 받으려면 호출 옵션 `grpc.Header`, `grpc.Trailer`를 사용합니다.

```go
var header, trailer metadata.MD
resp, err := client.SayHello(ctx, req,
    grpc.Header(&header),   // 서버의 헤더 메타데이터 수신
    grpc.Trailer(&trailer), // 서버의 트레일러 메타데이터 수신
)
```

---

## 메타데이터 수신 (서버)

서버 핸들러는 컨텍스트에서 인커밍 메타데이터를 읽습니다.

```go
func (s *server) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
    md, ok := metadata.FromIncomingContext(ctx)
    if !ok {
        return nil, status.Error(codes.InvalidArgument, "metadata가 없습니다")
    }
    if tokens := md.Get("authorization"); len(tokens) > 0 {
        // tokens[0] 검증 ...
    }
    return &pb.HelloReply{Message: "Hello " + in.GetName()}, nil
}
```

서버에서 메타데이터를 클라이언트로 보낼 때는 헤더 또는 트레일러로 보냅니다.

```go
// 헤더: 응답 메시지보다 먼저 전송
grpc.SendHeader(ctx, metadata.Pairs("server-version", "1.0"))
// 또는 SetHeader로 누적 후 첫 응답 시점에 전송

// 트레일러: 응답 이후 마지막에 전송
grpc.SetTrailer(ctx, metadata.Pairs("server-cost", "42"))
```

스트리밍 핸들러에서는 스트림 객체의 `SendHeader`/`SetHeader`/`SetTrailer`를 사용합니다.

---

## 헤더와 트레일러

- **헤더(header)**: 첫 응답 메시지 이전에 전송되는 초기 메타데이터.
- **트레일러(trailer)**: 모든 메시지와 상태 코드 이후 마지막에 전송되는 메타데이터. 처리 비용, 서버 사용량 같은 사후 정보 전달에 유용합니다.

스트리밍에서 헤더는 첫 메시지 전에 한 번, 트레일러는 스트림 종료 시 전송됩니다.

---

## 인터셉터란

인터셉터(interceptor)는 RPC 호출 경로에 끼어들어 공통 로직을 적용하는 미들웨어(middleware)입니다. 로깅, 인증/인가, 메트릭, 재시도, 캐싱, 메타데이터 처리 등에 사용합니다. 특정 RPC 메서드와 무관하게 일반적 동작을 적용할 때 적합합니다.

gRPC 인터셉터는 두 축으로 나뉩니다.

- 위치: **서버 측** / **클라이언트 측**
- RPC 형태: **단방향(unary)** / **스트림(stream)**

인터셉터 순서는 중요합니다. 예를 들어 로깅 인터셉터를 캐싱 인터셉터 앞에 둘지 뒤에 둘지에 따라 측정 대상(네트워크 통신 vs 애플리케이션 동작)이 달라집니다.

> 참고: 클라이언트 인증은 인터셉터로도 가능하지만, gRPC는 이를 위해 별도의 "call credentials" API를 제공하며 그쪽이 더 적합합니다(`07_auth_security.md` 참조).

---

## 서버 인터셉터

### 단방향 서버 인터셉터

시그니처는 `grpc.UnaryServerInterceptor`입니다. `handler`를 호출해야 실제 핸들러가 실행됩니다.

```go
func loggingUnaryInterceptor(
    ctx context.Context,
    req any,
    info *grpc.UnaryServerInfo,
    handler grpc.UnaryHandler,
) (any, error) {
    start := time.Now()
    log.Printf("--> unary call: %s", info.FullMethod)

    resp, err := handler(ctx, req) // 실제 핸들러 호출

    log.Printf("<-- %s took %v, err=%v", info.FullMethod, time.Since(start), err)
    return resp, err
}
```

### 스트림 서버 인터셉터

시그니처는 `grpc.StreamServerInterceptor`입니다.

```go
func loggingStreamInterceptor(
    srv any,
    ss grpc.ServerStream,
    info *grpc.StreamServerInfo,
    handler grpc.StreamHandler,
) error {
    log.Printf("--> stream call: %s", info.FullMethod)
    err := handler(srv, ss) // 실제 스트림 핸들러 호출
    log.Printf("<-- stream %s done, err=%v", info.FullMethod, err)
    return err
}
```

---

## 클라이언트 인터셉터

### 단방향 클라이언트 인터셉터

시그니처는 `grpc.UnaryClientInterceptor`입니다. `invoker`를 호출해야 실제 RPC가 전송됩니다.

```go
func authUnaryInterceptor(
    ctx context.Context,
    method string,
    req, reply any,
    cc *grpc.ClientConn,
    invoker grpc.UnaryInvoker,
    opts ...grpc.CallOption,
) error {
    // 모든 호출에 인증 메타데이터 주입
    ctx = metadata.AppendToOutgoingContext(ctx, "authorization", "bearer "+token)
    return invoker(ctx, method, req, reply, cc, opts...)
}
```

### 스트림 클라이언트 인터셉터

시그니처는 `grpc.StreamClientInterceptor`입니다.

```go
func authStreamInterceptor(
    ctx context.Context,
    desc *grpc.StreamDesc,
    cc *grpc.ClientConn,
    method string,
    streamer grpc.Streamer,
    opts ...grpc.CallOption,
) (grpc.ClientStream, error) {
    ctx = metadata.AppendToOutgoingContext(ctx, "authorization", "bearer "+token)
    return streamer(ctx, desc, cc, method, opts...)
}
```

---

## 인터셉터 등록과 체이닝

### 서버

```go
grpcServer := grpc.NewServer(
    grpc.UnaryInterceptor(loggingUnaryInterceptor),
    grpc.StreamInterceptor(loggingStreamInterceptor),
)

// 여러 개를 순서대로 적용하려면 Chain 사용
grpcServer = grpc.NewServer(
    grpc.ChainUnaryInterceptor(authUnary, loggingUnary, metricsUnary),
    grpc.ChainStreamInterceptor(authStream, loggingStream),
)
```

`ChainUnaryInterceptor`에서는 나열한 순서대로 실행됩니다(첫 번째가 가장 바깥쪽).

### 클라이언트

```go
conn, err := grpc.NewClient(addr,
    grpc.WithTransportCredentials(insecure.NewCredentials()),
    grpc.WithUnaryInterceptor(authUnaryInterceptor),
    grpc.WithStreamInterceptor(authStreamInterceptor),
)

// 체이닝
conn, err = grpc.NewClient(addr,
    grpc.WithTransportCredentials(insecure.NewCredentials()),
    grpc.WithChainUnaryInterceptor(authUnary, retryUnary, loggingUnary),
)
```

---
