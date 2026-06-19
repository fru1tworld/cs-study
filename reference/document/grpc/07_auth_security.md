# gRPC 인증과 보안 (Go)

> 이 문서는 gRPC 공식 문서의 "Authentication" 가이드를 Go 중심으로 정리한 것입니다.
> 원본: https://grpc.io/docs/guides/auth/

---

## 목차

1. [인증 메커니즘 개요](#인증-메커니즘-개요)
2. [자격증명 구조](#자격증명-구조)
3. [암호화 없는 연결](#암호화-없는-연결)
4. [TLS/SSL](#tlsssl)
5. [Call Credentials와 토큰 인증](#call-credentials와-토큰-인증)
6. [Channel과 Call Credentials 결합](#channel과-call-credentials-결합)
7. [ALTS](#alts)
8. [보안 주의사항](#보안-주의사항)

---

## 인증 메커니즘 개요

gRPC는 세 가지 인증 방식을 기본 제공합니다.

1. **SSL/TLS**: 서버 인증과 클라이언트-서버 간 데이터 암호화. gRPC는 SSL/TLS 사용을 강력히 권장합니다.
2. **ALTS(Application Layer Transport Security)**: Google Compute Engine / Google Kubernetes Engine에서 실행되는 애플리케이션을 위한 상호 인증.
3. **토큰 기반 인증**: OAuth2 토큰 등 Google 또는 자체 인증 메커니즘을 통한 API 접근.

---

## 자격증명 구조

gRPC는 두 종류의 자격증명(credentials)을 구분합니다.

- **Channel credentials(채널 자격증명)**: 채널에 부착됩니다. 예: SSL/TLS 자격증명. 연결 단위의 암호화/인증을 담당합니다.
- **Call credentials(호출 자격증명)**: 개별 RPC 호출에 부착됩니다. 예: 매 요청에 붙는 OAuth2 토큰. 요청 단위의 인증을 담당합니다.

두 자격증명은 결합(composite)할 수 있습니다. 채널 수준 암호화(TLS)와 호출 수준 인증(토큰)을 함께 적용하는 패턴이 일반적입니다.

---

## 암호화 없는 연결

개발/내부 환경에서는 암호화 없이 연결할 수 있습니다. 최신 gRPC-Go에서는 반드시 트랜스포트 자격증명을 명시해야 하므로 `insecure`를 사용합니다.

```go
import "google.golang.org/grpc/credentials/insecure"

conn, err := grpc.NewClient("localhost:50051",
    grpc.WithTransportCredentials(insecure.NewCredentials()))
```

프로덕션에서는 사용하지 않는 것이 원칙입니다.

---

## TLS/SSL

### 클라이언트 (서버 인증서 검증)

서버의 인증서를 검증할 루트 CA 인증서(`roots.pem`)로 클라이언트 자격증명을 만듭니다. 두 번째 인자는 서버 이름 오버라이드(server name override)이며 보통 빈 문자열입니다.

```go
import "google.golang.org/grpc/credentials"

creds, err := credentials.NewClientTLSFromFile("roots.pem", "")
if err != nil {
    log.Fatalf("failed to load credentials: %v", err)
}
conn, err := grpc.NewClient("myservice.example.com:443",
    grpc.WithTransportCredentials(creds))
```

### 서버 측 TLS

서버 인증서(`cert.pem`)와 개인키(`key.pem`)로 서버 자격증명을 만들고 `grpc.Creds` 옵션에 전달합니다.

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

mTLS(상호 TLS) 등 더 세밀한 설정이 필요하면 `credentials.NewTLS(*tls.Config)`로 `tls.Config`를 직접 구성합니다.

---

## Call Credentials와 토큰 인증

토큰(OAuth2, JWT 등)은 항상 per-call(호출 단위) 자격증명입니다. Go에서는 `credentials.PerRPCCredentials` 인터페이스를 구현하거나 `oauth` 헬퍼를 사용해 매 호출에 자격증명을 붙입니다.

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

`grpc.WithPerRPCCredentials`로 채널 전체에 적용하거나, 개별 호출 시 `grpc.PerRPCCredentials(...)` 호출 옵션으로 지정할 수 있습니다. 이 자격증명은 매 RPC마다 `authorization` 메타데이터를 자동으로 채웁니다.

> 참고: per-RPC credentials를 평문(insecure) 채널에서 전송하려면 별도 허용 설정이 필요합니다. 토큰 유출을 막기 위해 토큰 기반 인증은 TLS 채널과 함께 사용하는 것이 원칙입니다.

---

## Channel과 Call Credentials 결합

위 예시처럼 `grpc.WithTransportCredentials`(채널)와 `grpc.WithPerRPCCredentials`(호출)를 함께 지정하면, 채널 수준 암호화와 호출 수준 인증을 동시에 적용할 수 있습니다. 이는 "TLS로 보호된 연결 위에 매 요청 토큰을 붙이는" 가장 일반적인 패턴입니다.

---

## ALTS

ALTS는 Google 인프라(GCE/GKE)에서 동작하는 상호 인증·암호화 프로토콜입니다. 같은 환경 내 서비스 간 통신에서 인증서 관리 없이 신원 기반 인증을 제공합니다.

```go
import "google.golang.org/grpc/credentials/alts"

// 클라이언트
altsCreds := alts.NewClientCreds(alts.DefaultClientOptions())
conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(altsCreds))

// 서버
altsServerCreds := alts.NewServerCreds(alts.DefaultServerOptions())
server := grpc.NewServer(grpc.Creds(altsServerCreds))
```

ALTS는 해당 Google 환경 밖에서는 정상 동작하지 않습니다.

---

## 보안 주의사항

- Google이 발급한 OAuth2 토큰은 Google 서비스 접속에만 사용해야 합니다. Google 토큰을 Google이 아닌 서비스로 보내면 토큰이 탈취되어 클라이언트를 사칭하는 데 악용될 수 있습니다.
- 프로덕션에서는 암호화 없는 연결을 지양하고 TLS를 사용합니다.
- 토큰/자격증명은 항상 암호화된 채널로 전송합니다.

---
