# gRPC 스트리밍, 메타데이터, 인터셉터

## gRPC 스트리밍 (Go)

> 원본: https://grpc.io/docs/languages/go/basics/

<a id="개요"></a>
### 개요

gRPC 스트리밍은 HTTP/2 스트림 위에서 여러 메시지를 주고받는다. `.proto`의 요청과 응답 중 어디에 `stream`을 붙이느냐에 따라 메시지를 연속해서 보내는 쪽이 달라진다.

```proto
service RouteGuide {
  rpc ListFeatures(Rectangle) returns (stream Feature) {}        // 서버 스트리밍
  rpc RecordRoute(stream Point) returns (RouteSummary) {}        // 클라이언트 스트리밍
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}  // 양방향 스트리밍
}
```

Go에서는 생성된 스트림 인터페이스의 `Send`와 `Recv`로 메시지를 주고받는다. 수신할 메시지가 더 없으면 `Recv`가 `io.EOF`를 반환한다.

<a id="서버-스트리밍"></a>

### 서버 스트리밍

서버 스트리밍에서는 클라이언트가 요청 하나를 보내고, 서버가 여러 응답을 스트림으로 전송한다. 아래 서버는 조건에 맞는 feature를 `Send`로 하나씩 보낸다.

#### 서버

```go
func (s *routeGuideServer) ListFeatures(rect *pb.Rectangle, stream pb.RouteGuide_ListFeaturesServer) error {
    for _, feature := range s.savedFeatures {
        if inRange(feature.Location, rect) {
            if err := stream.Send(feature); err != nil {
                return err
            }
        }
    }
    return nil // nil 반환 시 스트림 정상 종료(상태 OK)
}
```

#### 클라이언트

```go
stream, err := client.ListFeatures(context.Background(), rect)
if err != nil {
    log.Fatalf("ListFeatures failed: %v", err)
}
for {
    feature, err := stream.Recv()
    if err == io.EOF {
        break // 서버가 스트림을 닫음
    }
    if err != nil {
        log.Fatalf("recv failed: %v", err)
    }
    log.Println(feature)
}
```

<a id="클라이언트-스트리밍"></a>

### 클라이언트 스트리밍

클라이언트 스트리밍은 반대로 클라이언트가 여러 메시지를 보내고 서버가 응답 하나를 반환한다.

#### 서버

서버는 `Recv`를 반복하며 요청을 처리한다. 클라이언트가 송신을 마쳐 `io.EOF`를 받으면 `SendAndClose`로 최종 응답을 보낸다.

```go
func (s *routeGuideServer) RecordRoute(stream pb.RouteGuide_RecordRouteServer) error {
    var pointCount, featureCount, distance int32
    startTime := time.Now()
    for {
        point, err := stream.Recv()
        if err == io.EOF {
            endTime := time.Now()
            return stream.SendAndClose(&pb.RouteSummary{
                PointCount:   pointCount,
                FeatureCount: featureCount,
                Distance:     distance,
                ElapsedTime:  int32(endTime.Sub(startTime).Seconds()),
            })
        }
        if err != nil {
            return err
        }
        pointCount++
        // ... point 처리 ...
    }
}
```

#### 클라이언트

클라이언트는 `Send`로 메시지를 모두 보낸 뒤 `CloseAndRecv`를 호출한다. 이 호출로 송신을 마치고 서버의 최종 응답을 받는다.

```go
stream, err := client.RecordRoute(context.Background())
if err != nil {
    log.Fatalf("RecordRoute failed: %v", err)
}
for _, point := range points {
    if err := stream.Send(point); err != nil {
        log.Fatalf("Send failed: %v", err)
    }
}
reply, err := stream.CloseAndRecv()
if err != nil {
    log.Fatalf("CloseAndRecv failed: %v", err)
}
log.Printf("Route summary: %v", reply)
```

<a id="양방향-스트리밍"></a>

### 양방향 스트리밍

양방향 스트리밍에서는 클라이언트와 서버가 각각 독립적으로 읽고 쓴다. 읽기와 쓰기 순서를 자유롭게 정할 수 있으며, 보통 별도 고루틴으로 나누어 처리한다.

#### 서버

```go
func (s *routeGuideServer) RouteChat(stream pb.RouteGuide_RouteChatServer) error {
    for {
        in, err := stream.Recv()
        if err == io.EOF {
            return nil
        }
        if err != nil {
            return err
        }
        key := serialize(in.Location)
        s.routeNotes[key] = append(s.routeNotes[key], in)
        // 같은 위치의 모든 기존 메모를 되돌려 보냄
        for _, note := range s.routeNotes[key] {
            if err := stream.Send(note); err != nil {
                return err
            }
        }
    }
}
```

#### 클라이언트

다음 클라이언트는 수신을 별도 고루틴에 맡기고 메인 흐름에서 송신한다. 송신이 끝나면 `CloseSend`로 쓰기 방향만 닫고, 수신 고루틴이 끝났다는 신호가 채널에 올 때까지 기다린다.

```go
stream, err := client.RouteChat(context.Background())
if err != nil {
    log.Fatalf("RouteChat failed: %v", err)
}
waitc := make(chan struct{})
go func() {
    for {
        in, err := stream.Recv()
        if err == io.EOF {
            close(waitc)
            return
        }
        if err != nil {
            log.Fatalf("Failed to receive: %v", err)
        }
        log.Printf("Got message %s", in.Message)
    }
}()
for _, note := range notes {
    if err := stream.Send(note); err != nil {
        log.Fatalf("Send failed: %v", err)
    }
}
stream.CloseSend() // 쓰기 방향만 닫음(읽기는 계속 가능)
<-waitc
```

<a id="스트리밍-패턴-정리"></a>

### 스트리밍 패턴 정리

- 서버 스트리밍
  - 서버 측 종료 신호: `return nil`
  - 클라이언트 측 송신 종료: 없음(단일 요청)
  - 클라이언트 측 수신 종료: `Recv`가 `io.EOF` 반환
- 클라이언트 스트리밍
  - 서버 측 종료 신호: `SendAndClose`
  - 클라이언트 측 송신 종료: `CloseAndRecv` 호출
  - 클라이언트 측 수신 종료: `CloseAndRecv` 응답 1회
- 양방향 스트리밍
  - 서버 측 종료 신호: `return nil`
  - 클라이언트 측 송신 종료: `CloseSend` 호출
  - 클라이언트 측 수신 종료: `Recv`가 `io.EOF` 반환

- 핵심 규칙:

- 동일한 스트림에서 여러 고루틴이 동시에 `Send`를 호출하는 것은 안전하지 않음
  - 동시 `Recv`도 마찬가지
  - 송신은 한 고루틴, 수신은 다른 고루틴으로 분리하는 패턴 권장
- 스트림은 호출에 사용된 컨텍스트의 데드라인/취소에 영향받음 → 컨텍스트가 취소되면 진행 중인 `Send`/`Recv`도 에러 반환

## gRPC 메타데이터와 인터셉터 (Go)

> 원본: https://grpc.io/docs/guides/metadata/ , https://grpc.io/docs/guides/interceptors/

<a id="메타데이터란"></a>
### 메타데이터란

메타데이터(metadata)는 RPC 메시지 외의 정보를 키-값 쌍으로 주고받는 부가 채널(side channel)이다. HTTP/2 헤더로 전송하며 인증 토큰, 추적 ID(trace ID), 로드 밸런싱이나 레이트 리밋 힌트 등을 담는다. 키와 값에는 다음 규칙이 적용된다.

- 키는 대소문자를 구분하지 않으며 ASCII 문자/숫자와 `-`, `_`, `.`로 구성
- 키는 `grpc-` 접두사로 시작 불가(gRPC 예약)
- 값이 바이너리면 키에 `-bin` 접미사를 붙임 → 이 경우 gRPC가 base64 인코딩을 자동 처리

- Go에서는 `google.golang.org/grpc/metadata` 패키지 사용

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

<a id="메타데이터-송신-클라이언트"></a>

### 메타데이터 송신 (클라이언트)

클라이언트는 메타데이터를 컨텍스트(context)에 담아 전송한다. `metadata.NewOutgoingContext`로 발신 메타데이터를 설정하고, 기존 컨텍스트에 더 추가할 때는 `metadata.AppendToOutgoingContext`를 사용한다.

```go
md := metadata.Pairs("authorization", "bearer "+token)
ctx := metadata.NewOutgoingContext(context.Background(), md)

// 또는 기존 컨텍스트에 키-값 추가
ctx = metadata.AppendToOutgoingContext(ctx, "request-id", "abc-123")

resp, err := client.SayHello(ctx, &pb.HelloRequest{Name: "Alice"})
```

- 서버가 보낸 헤더/트레일러 메타데이터를 수신하려면 호출 옵션 `grpc.Header`, `grpc.Trailer` 사용

```go
var header, trailer metadata.MD
resp, err := client.SayHello(ctx, req,
    grpc.Header(&header),   // 서버의 헤더 메타데이터 수신
    grpc.Trailer(&trailer), // 서버의 트레일러 메타데이터 수신
)
```

<a id="메타데이터-수신-서버"></a>

### 메타데이터 수신 (서버)

- 서버 핸들러는 컨텍스트에서 수신 메타데이터를 읽음

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

- 서버에서 메타데이터를 클라이언트로 보낼 때는 헤더 또는 트레일러로 전송

```go
// 헤더: 응답 메시지보다 먼저 전송
grpc.SendHeader(ctx, metadata.Pairs("server-version", "1.0"))
// 또는 SetHeader로 누적 후 첫 응답 시점에 전송

// 트레일러: 응답 이후 마지막에 전송
grpc.SetTrailer(ctx, metadata.Pairs("server-cost", "42"))
```

- 스트리밍 핸들러에서는 스트림 객체의 `SendHeader`/`SetHeader`/`SetTrailer` 사용

<a id="헤더와-트레일러"></a>

### 헤더와 트레일러

- 헤더(header): 첫 응답 메시지 이전에 전송되는 초기 메타데이터
- 트레일러(trailer): 모든 메시지와 상태 코드 이후 마지막에 전송되는 메타데이터
  - 처리 비용, 서버 사용량 같은 사후 정보 전달에 유용

- 스트리밍에서 헤더는 첫 메시지 전에 한 번, 트레일러는 스트림 종료 시 전송

<a id="인터셉터란"></a>

### 인터셉터란

로깅이나 인증, 인가처럼 여러 RPC에 필요한 동작을 각 메서드에 반복해서 넣을 필요는 없다. 인터셉터(interceptor)는 RPC 호출 경로에 개입하는 미들웨어(middleware)로, 메트릭, 재시도, 캐싱, 메타데이터 처리 등의 공통 로직을 특정 메서드와 무관하게 적용한다.

- gRPC 인터셉터는 두 축으로 구분

- 위치: 서버 측 / 클라이언트 측
- RPC 형태: 단방향(unary) / 스트림(stream)

인터셉터의 순서에 따라 관찰하는 동작도 달라진다. 예를 들어 로깅을 캐싱 인터셉터 앞에 두는지 뒤에 두는지에 따라 네트워크 통신을 측정할지, 캐시를 포함한 애플리케이션 동작을 측정할지가 달라진다.

클라이언트 인증도 인터셉터로 구현할 수 있지만, gRPC가 별도로 제공하는 "call credentials" API가 더 적합하다(`07_auth_security.md` 참조).

<a id="서버-인터셉터"></a>

### 서버 인터셉터

#### 단방향 서버 인터셉터

- 타입은 `grpc.UnaryServerInterceptor`. `handler`를 호출해야 실제 핸들러 실행

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

#### 스트림 서버 인터셉터

- 타입은 `grpc.StreamServerInterceptor`.

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

<a id="클라이언트-인터셉터"></a>

### 클라이언트 인터셉터

#### 단방향 클라이언트 인터셉터

- 타입은 `grpc.UnaryClientInterceptor`. `invoker`를 호출해야 실제 RPC 전송

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

#### 스트림 클라이언트 인터셉터

- 타입은 `grpc.StreamClientInterceptor`.

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

<a id="인터셉터-등록과-체이닝"></a>

### 인터셉터 등록과 체이닝

#### 서버

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

- `ChainUnaryInterceptor`는 나열한 순서대로 실행(첫 번째가 가장 바깥쪽).

#### 클라이언트

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
