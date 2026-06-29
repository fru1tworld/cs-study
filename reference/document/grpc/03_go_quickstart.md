# gRPC Go 빠른 시작

> 원본: https://grpc.io/docs/languages/go/quickstart/

---

## 목차

1. [사전 준비](#사전-준비)
2. [예제 코드 받기](#예제-코드-받기)
3. [예제 실행](#예제-실행)
4. [서비스 확장](#서비스-확장)
5. [코드 재생성](#코드-재생성)
6. [서버와 클라이언트 수정](#서버와-클라이언트-수정)

---

## 사전 준비

### Go

최근 두 메이저 릴리스 중 하나를 사용합니다. 설치는 Go 공식 문서를 참고합니다.

### Protocol Buffer 컴파일러(protoc)

`protoc` 버전 3을 설치합니다. macOS에서는 Homebrew로 간단히 설치할 수 있습니다.

```bash
brew install protobuf
protoc --version   # libprotoc 3.x 이상
```

### Go용 protoc 플러그인

메시지 코드를 생성하는 `protoc-gen-go`와 서비스(스텁/인터페이스) 코드를 생성하는 `protoc-gen-go-grpc`를 설치합니다.

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
```

`protoc`가 플러그인을 찾을 수 있도록 `$GOPATH/bin`을 PATH에 추가합니다.

```bash
export PATH="$PATH:$(go env GOPATH)/bin"
```

---

## 예제 코드 받기

grpc-go 저장소에서 helloworld 예제를 특정 태그(v1.81.1)로 받습니다.

```bash
git clone -b v1.81.1 --depth 1 https://github.com/grpc/grpc-go
cd grpc-go/examples/helloworld
```

---

## 예제 실행

서버를 먼저 실행합니다.

```bash
go run greeter_server/main.go
```

다른 터미널에서 클라이언트를 실행합니다.

```bash
go run greeter_client/main.go
```

다음과 같은 출력이 나오면 정상입니다.

```
Greeting: Hello world
```

---

## 서비스 확장

`helloworld/helloworld.proto`에 새 RPC 메서드 `SayHelloAgain`을 추가합니다.

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

## 코드 재생성

`.proto`를 수정했으면 `protoc`로 Go 코드를 다시 생성합니다. `helloworld` 예제 디렉터리에서 실행합니다.

```bash
protoc --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    helloworld/helloworld.proto
```

- `--go_out` / `--go_opt`: 메시지 타입 코드(`*.pb.go`)를 생성합니다(`protoc-gen-go`).
- `--go-grpc_out` / `--go-grpc_opt`: 서비스 스텁/인터페이스 코드(`*_grpc.pb.go`)를 생성합니다(`protoc-gen-go-grpc`).
- `paths=source_relative`: 출력 파일을 `.proto` 위치 기준으로 배치합니다.

---

## 서버와 클라이언트 수정

### 서버

`greeter_server/main.go`에 새 메서드 구현을 추가합니다.

```go
func (s *server) SayHelloAgain(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
    return &pb.HelloReply{Message: "Hello again " + in.GetName()}, nil
}
```

### 클라이언트

`greeter_client/main.go`의 `main()`에서 새 메서드를 호출합니다.

```go
r, err = c.SayHelloAgain(ctx, &pb.HelloRequest{Name: *name})
if err != nil {
    log.Fatalf("could not greet: %v", err)
}
log.Printf("Greeting: %s", r.GetMessage())
```

### 실행

서버를 다시 실행한 뒤, `--name` 플래그를 주고 클라이언트를 실행합니다.

```bash
go run greeter_client/main.go --name=Alice
```

출력:

```
Greeting: Hello Alice
Greeting: Hello again Alice
```

---
