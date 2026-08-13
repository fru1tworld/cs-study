# gRPC Go 빠른 시작과 서버·클라이언트 구현

## gRPC Go 빠른 시작

> 원본: https://grpc.io/docs/languages/go/quickstart/

---

### 목차

1. [사전 준비](#사전-준비)
2. [예제 코드 받기](#예제-코드-받기)
3. [예제 실행](#예제-실행)
4. [서비스 확장](#서비스-확장)
5. [코드 재생성](#코드-재생성)
6. [서버와 클라이언트 수정](#서버와-클라이언트-수정)

---

### 사전 준비

#### Go

최근 두 메이저 릴리스 중 하나 사용 → 설치는 Go 공식 문서 참고

#### Protocol Buffer 컴파일러(protoc)

`protoc` 버전 3 설치 필요 → macOS에서는 Homebrew로 간단히 설치 가능

```bash
brew install protobuf
protoc --version   # libprotoc 3.x 이상
```

#### Go용 protoc 플러그인

메시지 코드를 생성하는 `protoc-gen-go`와 서비스(스텁/인터페이스) 코드를 생성하는 `protoc-gen-go-grpc` 설치

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
```

`protoc`가 플러그인을 찾을 수 있도록 `$GOPATH/bin`을 PATH에 추가

```bash
export PATH="$PATH:$(go env GOPATH)/bin"
```

---

### 예제 코드 받기

grpc-go 저장소에서 helloworld 예제를 특정 태그(v1.81.1)로 받음

```bash
git clone -b v1.81.1 --depth 1 https://github.com/grpc/grpc-go
cd grpc-go/examples/helloworld
```

---

### 예제 실행

서버 먼저 실행

```bash
go run greeter_server/main.go
```

다른 터미널에서 클라이언트 실행

```bash
go run greeter_client/main.go
```

다음과 같은 출력이 나오면 정상

```
Greeting: Hello world
```

---

### 서비스 확장

`helloworld/helloworld.proto`에 새 RPC 메서드 `SayHelloAgain` 추가

```proto
// 인사 서비스 정의
service Greeter {
  // 인사를 보냄
  rpc SayHello (HelloRequest) returns (HelloReply) {}
  // 다시 인사를 보냄
  rpc SayHelloAgain (HelloRequest) returns (HelloReply) {}
}

// 사용자 이름을 담은 요청 메시지
message HelloRequest {
  string name = 1;
}

// 인사말을 담은 응답 메시지
message HelloReply {
  string message = 1;
}
```

---

### 코드 재생성

`.proto` 수정 시 `protoc`로 Go 코드 재생성 필요 → `helloworld` 예제 디렉터리에서 실행

```bash
protoc --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    helloworld/helloworld.proto
```

- `--go_out` / `--go_opt`: 메시지 타입 코드(`*.pb.go`) 생성(`protoc-gen-go`)
- `--go-grpc_out` / `--go-grpc_opt`: 서비스 스텁/인터페이스 코드(`*_grpc.pb.go`) 생성(`protoc-gen-go-grpc`)
- `paths=source_relative`: 출력 파일을 `.proto` 위치 기준으로 배치

---

### 서버와 클라이언트 수정

#### 서버

`greeter_server/main.go`에 새 메서드 구현 추가

```go
func (s *server) SayHelloAgain(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
    return &pb.HelloReply{Message: "Hello again " + in.GetName()}, nil
}
```

#### 클라이언트

`greeter_client/main.go`의 `main()`에서 새 메서드 호출

```go
r, err = c.SayHelloAgain(ctx, &pb.HelloRequest{Name: *name})
if err != nil {
    log.Fatalf("could not greet: %v", err)
}
log.Printf("Greeting: %s", r.GetMessage())
```

#### 실행

서버 재실행 후 `--name` 플래그를 주고 클라이언트 실행

```bash
go run greeter_client/main.go --name=Alice
```

출력:

```
Greeting: Hello Alice
Greeting: Hello again Alice
```

---

---

## gRPC Go 서버와 클라이언트 구현

> 원본: https://grpc.io/docs/languages/go/basics/

---

### 목차

1. [서비스 정의](#서비스-정의)
2. [코드 생성](#코드-생성)
3. [서버 구현](#서버-구현)
4. [서버 시작](#서버-시작)
5. [클라이언트 구현](#클라이언트-구현)
6. [채널과 연결 주의사항](#채널과-연결-주의사항)

---

### 서비스 정의

`.proto` 파일에서 서비스와 메시지 정의 → 아래는 공식 튜토리얼의 `RouteGuide` 서비스로, 네 가지 RPC 타입 모두 포함

```proto
syntax = "proto3";

option go_package = "google.golang.org/grpc/examples/route_guide/routeguide";

service RouteGuide {
  // 단방향: 주어진 위치의 Feature를 반환
  rpc GetFeature(Point) returns (Feature) {}

  // 서버 스트리밍: 사각형 영역 안의 Feature들을 스트림으로 반환
  rpc ListFeatures(Rectangle) returns (stream Feature) {}

  // 클라이언트 스트리밍: 경로를 이루는 Point들을 받아 요약을 반환
  rpc RecordRoute(stream Point) returns (RouteSummary) {}

  // 양방향 스트리밍: 위치 기반 메모를 주고받음
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
}

message Point {
  int32 latitude = 1;
  int32 longitude = 2;
}
```

---

### 코드 생성

`examples/route_guide` 디렉터리에서 `protoc`로 코드 생성

```bash
protoc --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    routeguide/route_guide.proto
```

생성되는 파일은 두 가지

- `route_guide.pb.go`: 메시지 타입(구조체, 직렬화 코드)
- `route_guide_grpc.pb.go`: 클라이언트 스텁과 서버 인터페이스

---

### 서버 구현

생성된 서버 인터페이스를 구현하는 구조체 생성 → `pb.UnimplementedRouteGuideServer`를 임베드하면 향후 메서드가 추가되더라도 전방 호환성(forward compatibility) 유지 가능

```go
type routeGuideServer struct {
    pb.UnimplementedRouteGuideServer
    savedFeatures []*pb.Feature
    routeNotes    map[string][]*pb.RouteNote
}
```

#### 단방향 RPC

컨텍스트와 요청 메시지를 받아 응답 메시지와 에러 반환

```go
func (s *routeGuideServer) GetFeature(ctx context.Context, point *pb.Point) (*pb.Feature, error) {
    for _, feature := range s.savedFeatures {
        if proto.Equal(feature.Location, point) {
            return feature, nil
        }
    }
    // 일치하는 Feature가 없으면 이름 없는 Feature 반환
    return &pb.Feature{Location: point}, nil
}
```

#### 서버 스트리밍 RPC

요청과 함께 전용 스트림 객체 수신 → `stream.Send`로 여러 응답 전송 후 `nil` 반환하면 스트림 종료

```go
func (s *routeGuideServer) ListFeatures(rect *pb.Rectangle, stream pb.RouteGuide_ListFeaturesServer) error {
    for _, feature := range s.savedFeatures {
        if inRange(feature.Location, rect) {
            if err := stream.Send(feature); err != nil {
                return err
            }
        }
    }
    return nil
}
```

#### 클라이언트 스트리밍 RPC

스트림에서 `Recv`로 요청을 반복해 읽음 → `io.EOF`가 나오면 `SendAndClose`로 단일 응답 전송

```go
func (s *routeGuideServer) RecordRoute(stream pb.RouteGuide_RecordRouteServer) error {
    var pointCount, featureCount, distance int32
    var lastPoint *pb.Point
    startTime := time.Now()
    for {
        point, err := stream.Recv()
        if err == io.EOF {
            // 클라이언트가 스트림을 닫으면 요약 응답을 반환
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
        lastPoint = point
        _ = lastPoint
    }
}
```

#### 양방향 스트리밍 RPC

`Recv`와 `Send`를 각자 독립적으로 사용 → 아래 예제는 받은 메모를 같은 위치의 모든 기존 메모와 함께 되돌려 보냄

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
        for _, note := range s.routeNotes[key] {
            if err := stream.Send(note); err != nil {
                return err
            }
        }
    }
}
```

---

### 서버 시작

리스너를 열고 → `grpc.NewServer`로 서버 생성 → `RegisterXxxServer`로 구현체 등록 → `Serve` 호출로 요청 처리

```go
lis, err := net.Listen("tcp", fmt.Sprintf("localhost:%d", port))
if err != nil {
    log.Fatalf("failed to listen: %v", err)
}
grpcServer := grpc.NewServer()
pb.RegisterRouteGuideServer(grpcServer, newServer())
grpcServer.Serve(lis)   // 블로킹: 종료될 때까지 요청을 처리
```

서버 옵션(TLS, 인터셉터, keepalive 등)은 `grpc.NewServer(opts...)`에 전달

---

### 클라이언트 구현

`grpc.NewClient`로 채널 생성 → 생성된 `NewXxxClient`로 스텁 생성 후 메서드 호출

> 참고: 최신 gRPC-Go에서는 `grpc.Dial` 대신 `grpc.NewClient` 권장 → `NewClient`는 즉시 연결하지 않고 지연 연결(lazy)하므로 별도의 블로킹 다이얼 불필요 → 암호화 없이 연결할 때는 트랜스포트 자격증명 반드시 명시 필요

```go
conn, err := grpc.NewClient(*serverAddr,
    grpc.WithTransportCredentials(insecure.NewCredentials()))
if err != nil {
    log.Fatalf("did not connect: %v", err)
}
defer conn.Close()

client := pb.NewRouteGuideClient(conn)
```

#### 단방향 호출

```go
feature, err := client.GetFeature(context.Background(),
    &pb.Point{Latitude: 409146138, Longitude: -746188906})
if err != nil {
    log.Fatalf("GetFeature failed: %v", err)
}
log.Println(feature)
```

#### 서버 스트리밍 호출

스트림을 받아 `Recv`로 `io.EOF`까지 반복해 읽음

```go
stream, err := client.ListFeatures(context.Background(), rect)
if err != nil {
    log.Fatalf("ListFeatures failed: %v", err)
}
for {
    feature, err := stream.Recv()
    if err == io.EOF {
        break
    }
    if err != nil {
        log.Fatalf("ListFeatures recv failed: %v", err)
    }
    log.Println(feature)
}
```

#### 클라이언트 스트리밍 호출

`Send`로 여러 메시지를 보낸 뒤 `CloseAndRecv`로 응답 수신

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

#### 양방향 스트리밍 호출

읽기와 쓰기는 보통 별도 고루틴에서 동시 처리

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
stream.CloseSend()
<-waitc
```

---

### 채널과 연결 주의사항

- 채널 생성에는 비용이 따름 → 가능하면 채널과 스텁을 재사용하고 RPC마다 새로 만들지 않음
- `grpc.NewClient`는 연결을 지연 → 첫 RPC 시점에 실제 연결
- 암호화 없이 연결할 때는 `grpc.WithTransportCredentials(insecure.NewCredentials())` 명시 필요(프로덕션에서는 TLS 사용 권장, `07_auth_security.md` 참조)

---
