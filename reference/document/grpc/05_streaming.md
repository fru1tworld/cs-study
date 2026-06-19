# gRPC 스트리밍 (Go)

> 이 문서는 gRPC 공식 문서의 스트리밍 RPC(server/client/bidirectional) 구현을 Go 중심으로 정리한 것입니다.
> 원본: https://grpc.io/docs/languages/go/basics/

---

## 목차

1. [개요](#개요)
2. [서버 스트리밍](#서버-스트리밍)
3. [클라이언트 스트리밍](#클라이언트-스트리밍)
4. [양방향 스트리밍](#양방향-스트리밍)
5. [스트리밍 패턴 정리](#스트리밍-패턴-정리)

---

## 개요

gRPC 스트리밍은 HTTP/2 스트림 위에서 동작하며, 단일 요청/응답을 넘어 메시지 시퀀스를 주고받을 수 있게 합니다. `.proto`에서 `stream` 키워드의 위치로 타입이 결정됩니다.

```proto
service RouteGuide {
  rpc ListFeatures(Rectangle) returns (stream Feature) {}        // 서버 스트리밍
  rpc RecordRoute(stream Point) returns (RouteSummary) {}        // 클라이언트 스트리밍
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}  // 양방향 스트리밍
}
```

Go에서 스트림은 생성된 스트림 인터페이스를 통해 `Send`/`Recv` 메서드로 다룹니다. 스트림의 끝은 `io.EOF`로 신호됩니다.

---

## 서버 스트리밍

클라이언트가 하나의 요청을 보내면, 서버가 여러 응답을 스트림으로 보냅니다.

### 서버

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

### 클라이언트

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

---

## 클라이언트 스트리밍

클라이언트가 여러 메시지를 스트림으로 보내고, 서버는 단일 응답을 반환합니다.

### 서버

`Recv`를 반복하다 `io.EOF`(클라이언트가 스트림을 닫음)를 만나면 `SendAndClose`로 응답을 보냅니다.

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

### 클라이언트

`Send`로 메시지를 모두 보낸 뒤 `CloseAndRecv`로 단일 응답을 받습니다.

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

---

## 양방향 스트리밍

클라이언트와 서버가 각각 독립적인 읽기/쓰기 스트림을 가집니다. 읽기와 쓰기 순서는 자유이며, 흔히 별도 고루틴으로 분리합니다.

### 서버

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

### 클라이언트

수신은 고루틴에서, 송신은 메인 흐름에서 처리합니다. 송신을 마치면 `CloseSend`로 쓰기 방향을 닫고, 수신 고루틴이 끝날 때까지 채널로 대기합니다.

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

---

## 스트리밍 패턴 정리

| RPC 타입 | 서버 측 종료 신호 | 클라이언트 측 송신 종료 | 클라이언트 측 수신 종료 |
|---|---|---|---|
| 서버 스트리밍 | `return nil` | (없음, 단일 요청) | `Recv`가 `io.EOF` 반환 |
| 클라이언트 스트리밍 | `SendAndClose` | `CloseAndRecv` 호출 | `CloseAndRecv` 응답 1회 |
| 양방향 스트리밍 | `return nil` | `CloseSend` 호출 | `Recv`가 `io.EOF` 반환 |

핵심 규칙:

- 동일한 스트림에 대해 여러 고루틴이 동시에 `Send`를 호출하는 것은 안전하지 않습니다. 마찬가지로 동시 `Recv`도 안전하지 않습니다. 송신은 한 고루틴, 수신은 다른 한 고루틴으로 분리하는 패턴을 권장합니다.
- 스트림은 호출에 사용된 컨텍스트의 데드라인/취소에 영향을 받습니다. 컨텍스트가 취소되면 진행 중인 `Send`/`Recv`도 에러를 반환합니다.

---
