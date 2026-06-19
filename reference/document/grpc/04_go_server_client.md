# gRPC Go 서버와 클라이언트 구현

> 이 문서는 gRPC 공식 문서의 "Go Basics tutorial" 중 서비스 정의, 서버/클라이언트 구현을 한국어로 정리한 것입니다.
> 원본: https://grpc.io/docs/languages/go/basics/

---

## 목차

1. [서비스 정의](#서비스-정의)
2. [코드 생성](#코드-생성)
3. [서버 구현](#서버-구현)
4. [서버 시작](#서버-시작)
5. [클라이언트 구현](#클라이언트-구현)
6. [채널과 연결 주의사항](#채널과-연결-주의사항)

---

## 서비스 정의

`.proto` 파일에서 서비스와 메시지를 정의합니다. 아래는 공식 튜토리얼의 `RouteGuide` 서비스로, 4가지 RPC 타입을 모두 보여줍니다.

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

## 코드 생성

`examples/route_guide` 디렉터리에서 `protoc`로 코드를 생성합니다.

```bash
protoc --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    routeguide/route_guide.proto
```

생성되는 파일은 두 가지입니다.

- `route_guide.pb.go`: 메시지 타입(구조체, 직렬화 코드)
- `route_guide_grpc.pb.go`: 클라이언트 스텁과 서버 인터페이스

---

## 서버 구현

생성된 서버 인터페이스를 구현하는 구조체를 만듭니다. `pb.UnimplementedRouteGuideServer`를 임베드하면 향후 추가된 메서드에 대한 전방 호환성(forward compatibility)을 확보할 수 있습니다.

```go
type routeGuideServer struct {
    pb.UnimplementedRouteGuideServer
    savedFeatures []*pb.Feature
    routeNotes    map[string][]*pb.RouteNote
}
```

### 단방향 RPC

요청을 받아 응답을 반환합니다. 컨텍스트와 요청 메시지를 받고, 응답 메시지와 에러를 반환합니다.

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

### 서버 스트리밍 RPC

요청과 함께 전용 스트림 객체를 받습니다. `stream.Send`로 여러 응답을 보낸 뒤 `nil`을 반환하면 스트림이 종료됩니다.

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

### 클라이언트 스트리밍 RPC

스트림에서 `Recv`로 요청을 반복해 읽고, `io.EOF`가 나오면 `SendAndClose`로 단일 응답을 보냅니다.

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

### 양방향 스트리밍 RPC

`Recv`와 `Send`를 각자 독립적으로 사용합니다. 아래 예제는 받은 메모를 같은 위치의 모든 기존 메모와 함께 되돌려 보냅니다.

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

## 서버 시작

리스너(listener)를 열고, `grpc.NewServer`로 서버를 만든 뒤, 생성된 `RegisterXxxServer`로 구현을 등록하고 `Serve`로 요청을 처리합니다.

```go
lis, err := net.Listen("tcp", fmt.Sprintf("localhost:%d", port))
if err != nil {
    log.Fatalf("failed to listen: %v", err)
}
grpcServer := grpc.NewServer()
pb.RegisterRouteGuideServer(grpcServer, newServer())
grpcServer.Serve(lis)   // 블로킹: 종료될 때까지 요청을 처리
```

서버 옵션(TLS, 인터셉터, keepalive 등)은 `grpc.NewServer(opts...)`에 전달합니다.

---

## 클라이언트 구현

`grpc.NewClient`로 채널을 만들고, 생성된 `NewXxxClient`로 스텁을 만든 뒤 메서드를 호출합니다.

> 참고: 최신 gRPC-Go에서는 `grpc.Dial` 대신 `grpc.NewClient`가 권장됩니다. `NewClient`는 즉시 연결하지 않고 지연 연결(lazy)하므로 별도의 블로킹 다이얼이 필요 없습니다. 암호화가 없는 연결에는 반드시 트랜스포트 자격증명을 명시해야 합니다.

```go
conn, err := grpc.NewClient(*serverAddr,
    grpc.WithTransportCredentials(insecure.NewCredentials()))
if err != nil {
    log.Fatalf("did not connect: %v", err)
}
defer conn.Close()

client := pb.NewRouteGuideClient(conn)
```

### 단방향 호출

```go
feature, err := client.GetFeature(context.Background(),
    &pb.Point{Latitude: 409146138, Longitude: -746188906})
if err != nil {
    log.Fatalf("GetFeature failed: %v", err)
}
log.Println(feature)
```

### 서버 스트리밍 호출

스트림을 받아 `Recv`로 `io.EOF`까지 반복해 읽습니다.

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

### 클라이언트 스트리밍 호출

`Send`로 여러 메시지를 보낸 뒤 `CloseAndRecv`로 응답을 받습니다.

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

### 양방향 스트리밍 호출

읽기와 쓰기를 별도 고루틴에서 동시에 처리하는 것이 일반적입니다.

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

## 채널과 연결 주의사항

- 채널 생성은 비용이 있으므로, 가능하면 채널과 스텁을 재사용하고 RPC마다 새로 만들지 않습니다.
- `grpc.NewClient`는 연결을 지연시키며, 첫 RPC 시점에 연결을 맺습니다.
- 암호화 없이 연결할 때는 `grpc.WithTransportCredentials(insecure.NewCredentials())`를 명시해야 합니다(프로덕션에서는 TLS 사용 권장, `07_auth_security.md` 참조).

---
