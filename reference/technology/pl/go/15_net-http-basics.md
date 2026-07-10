# 네트워킹과 HTTP 기초 (net / net/http / net/url)

Go 표준 라이브러리는 소켓 수준의 저수준 네트워킹부터 HTTP 클라이언트·서버, URL 파싱까지 별도 외부 패키지 없이 지원합니다. 세 패키지는 계층적으로 연결되어 있습니다.

- `net` — TCP/UDP/Unix 소켓 등 전송 계층 API
- `net/http` — `net`을 기반으로 만든 HTTP 클라이언트/서버
- `net/url` — URL 문자열의 파싱과 조립

---

# net 패키지

> **원문:** https://pkg.go.dev/net

## 개요

`net`은 TCP/IP, UDP, 도메인 이름 해석, Unix 도메인 소켓을 위한 이식성 있는 인터페이스를 제공합니다. 대부분의 코드는 `net/http`처럼 상위 패키지를 통해 간접적으로 사용하지만, 원시 소켓 통신이 필요하면 직접 다룹니다.

## 연결 맺기: Dial 계열

가장 자주 쓰는 진입점은 `Dial`입니다. 네트워크 종류와 주소만 주면 해당 프로토콜에 맞는 연결을 반환합니다.

```go
func Dial(network, address string) (Conn, error)
func DialTimeout(network, address string, timeout time.Duration) (Conn, error)
```

- `network`: `"tcp"`, `"tcp4"`, `"tcp6"`, `"udp"`, `"unix"` 등
- `address`: TCP/UDP는 `"host:port"`, Unix 소켓은 파일 경로
- `DialTimeout`은 이름 해석과 연결 시도 전체에 타임아웃을 건다

```go
conn, err := net.Dial("tcp", "example.com:80")
if err != nil {
    log.Fatal(err)
}
defer conn.Close()
```

더 세밀한 제어(컨텍스트 취소, keep-alive 주기 등)가 필요하면 `Dialer` 구조체를 직접 만들어 `DialContext`를 호출합니다.

```go
d := &net.Dialer{Timeout: 5 * time.Second}
conn, err := d.DialContext(ctx, "tcp", "example.com:443")
```

## 서버 쪽: Listen과 Accept

서버는 `Listen`으로 리스너를 만든 뒤 `Accept`를 반복 호출해 들어오는 연결을 받는 구조가 기본입니다.

```go
func Listen(network, address string) (Listener, error)
```

```go
ln, err := net.Listen("tcp", ":8080")
if err != nil {
    log.Fatal(err)
}
defer ln.Close()
for {
    conn, err := ln.Accept()
    if err != nil {
        continue
    }
    go handle(conn) // 연결마다 고루틴 하나
}
```

TCP/UDP 전용으로 다룰 때는 `ListenTCP`/`ListenUDP`를 써서 `*TCPConn`, `*UDPConn`처럼 더 구체적인 타입을 얻을 수 있습니다.

## 핵심 인터페이스

### Conn — 양방향 연결

```go
type Conn interface {
    Read(b []byte) (n int, err error)
    Write(b []byte) (n int, err error)
    Close() error
    LocalAddr() Addr
    RemoteAddr() Addr
    SetDeadline(t time.Time) error
    SetReadDeadline(t time.Time) error
    SetWriteDeadline(t time.Time) error
}
```

TCP, UDP, Unix 소켓 연결 모두 이 인터페이스를 만족합니다. 데드라인 메서드는 특정 시각 이후로 I/O가 블로킹되지 않도록 강제하는 용도입니다.

### Listener — 연결 수신

```go
type Listener interface {
    Accept() (Conn, error)
    Close() error
    Addr() Addr
}
```

### Addr — 주소 표현

```go
type Addr interface {
    Network() string // "tcp", "udp" 등
    String() string  // "192.0.2.1:8080" 같은 문자열
}
```

`TCPAddr`, `UDPAddr`, `IPAddr`, `UnixAddr`가 이 인터페이스를 구현하며, 각각 `IP`, `Port` 등의 필드를 가집니다.

```go
addr := &net.TCPAddr{IP: net.ParseIP("192.0.2.1"), Port: 8080}
```

## 주소 문자열 해석

문자열 형태의 주소를 구조체로 바꿀 때는 `Resolve*Addr` 계열을 사용합니다.

| 함수 | 용도 |
|------|------|
| `ResolveTCPAddr(network, address string) (*TCPAddr, error)` | `"host:port"` → `*TCPAddr` |
| `ResolveUDPAddr(network, address string) (*UDPAddr, error)` | `"host:port"` → `*UDPAddr` |
| `ResolveIPAddr(network, address string) (*IPAddr, error)` | 호스트 → `*IPAddr` |

```go
tcpAddr, err := net.ResolveTCPAddr("tcp", "example.com:80")
```

## 이름 조회(DNS)

| 함수 | 설명 |
|------|------|
| `LookupHost(host string) ([]string, error)` | 호스트명 → IP 문자열 목록 |
| `LookupIP(host string) ([]IP, error)` | 호스트명 → `IP` 값 목록 |
| `LookupAddr(addr string) ([]string, error)` | IP → 호스트명 (역방향 조회) |

```go
addrs, err := net.LookupHost("golang.org")
```

## IP 값 다루기

`ParseIP`로 문자열을 `IP` 타입으로 바꾸고, `IP`의 메서드로 성질을 검사합니다.

```go
func ParseIP(s string) IP
```

```go
ip := net.ParseIP("192.0.2.1")
if ip.IsPrivate() {
    fmt.Println("사설 IP")
}
```

주요 `IP` 메서드: `String()`, `To4()`, `To16()`, `IsLoopback()`, `IsPrivate()`, `IsMulticast()`, `Equal(x IP)`.

CIDR 표기(`"192.0.2.0/24"`)는 `ParseCIDR(s string) (IP, *IPNet, error)`로 파싱합니다.

## 호스트:포트 조합

IPv6 리터럴 주소(`[::1]:8080`)까지 안전하게 다루려면 문자열을 직접 자르지 말고 다음 헬퍼를 사용합니다.

```go
host, port, err := net.SplitHostPort("[::1]:8080")
addr := net.JoinHostPort("::1", "8080") // "[::1]:8080"
```

## 네트워크 인터페이스 조회

```go
ifaces, err := net.Interfaces()
for _, i := range ifaces {
    addrs, _ := i.Addrs()
    fmt.Println(i.Name, addrs)
}
```

## 에러 타입

네트워크 에러는 대부분 `*OpError`나 `*DNSError`로 감싸여 반환됩니다.

```go
type OpError struct {
    Op     string // "dial", "read", "write" ...
    Net    string
    Source Addr // 로컬 주소 (있는 경우)
    Addr   Addr // 원격 주소
    Err    error
}

type DNSError struct {
    Name        string
    IsTimeout   bool
    IsTemporary bool
    IsNotFound  bool
}
```

```go
_, err := net.LookupHost("no-such-host.invalid")
var dnsErr *net.DNSError
if errors.As(err, &dnsErr) && dnsErr.IsNotFound {
    // 존재하지 않는 호스트 처리
}
```

---

# net/http 패키지

> **원문:** https://pkg.go.dev/net/http

## 개요

`net/http`는 `net`을 기반으로 HTTP/1.1, HTTP/2 클라이언트와 서버를 구현한 패키지입니다. 외부 웹 프레임워크 없이도 프로덕션급 서버를 만들 수 있을 만큼 기능이 완결되어 있습니다.

## 간단한 클라이언트 요청

`http.Get`, `http.Post` 등은 내부적으로 `DefaultClient`를 사용하는 편의 함수입니다.

```go
func Get(url string) (resp *Response, err error)
func Post(url, contentType string, body io.Reader) (resp *Response, err error)
func PostForm(url string, data url.Values) (resp *Response, err error)
```

```go
resp, err := http.Get("https://example.com")
if err != nil {
    log.Fatal(err)
}
defer resp.Body.Close()
body, _ := io.ReadAll(resp.Body)
```

**주의**: 응답을 다 쓴 뒤에는 반드시 `resp.Body.Close()`를 호출해야 커넥션이 재사용됩니다.

## 커스텀 요청과 Client

헤더 추가, 컨텍스트 지정처럼 세밀한 제어가 필요하면 `NewRequest`로 요청을 만들고 `Client.Do`로 보냅니다.

```go
func NewRequest(method, url string, body io.Reader) (*Request, error)

type Client struct {
    Transport     RoundTripper
    Timeout       time.Duration
    CheckRedirect func(req *Request, via []*Request) error
}

func (c *Client) Do(req *Request) (*Response, error)
```

```go
client := &http.Client{Timeout: 5 * time.Second}
req, err := http.NewRequest(http.MethodPost, "https://api.example.com/items", body)
req.Header.Set("Content-Type", "application/json")

resp, err := client.Do(req)
```

`Client`는 커넥션 풀을 내부에 갖고 있으므로, 요청마다 새로 만들지 말고 하나를 재사용하는 것이 관례입니다.

## 서버: Handler와 라우팅

서버 쪽의 핵심 개념은 요청을 처리하는 `Handler` 인터페이스입니다.

```go
type Handler interface {
    ServeHTTP(ResponseWriter, *Request)
}

type HandlerFunc func(ResponseWriter, *Request)
```

`HandlerFunc`는 함수를 `Handler`로 바꿔주는 어댑터라서, 대부분의 핸들러는 구조체가 아니라 함수로 작성합니다.

```go
func hello(w http.ResponseWriter, r *http.Request) {
    fmt.Fprintln(w, "hello")
}

http.HandleFunc("/hello", hello) // DefaultServeMux에 등록
http.ListenAndServe(":8080", nil)
```

라우팅은 `ServeMux`가 담당하며, Go 1.22부터는 메서드와 경로 변수를 패턴에 직접 쓸 수 있습니다.

```go
mux := http.NewServeMux()
mux.HandleFunc("GET /users/{id}", getUser)
mux.HandleFunc("POST /users", createUser)

func getUser(w http.ResponseWriter, r *http.Request) {
    id := r.PathValue("id")
    fmt.Fprintf(w, "user %s", id)
}
```

## Request와 ResponseWriter

`*Request`는 들어온 요청 정보를 담고, `ResponseWriter`는 응답을 쓰는 인터페이스입니다.

```go
type ResponseWriter interface {
    Header() Header
    Write([]byte) (int, error)
    WriteHeader(statusCode int)
}
```

자주 쓰는 `Request` 필드/메서드: `r.Method`, `r.URL`, `r.Header`, `r.Body`, `r.FormValue(key)`, `r.Context()`.

```go
func handler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte(`{"ok":true}`))
}
```

`WriteHeader`는 상태 코드를 결정하므로 반드시 `Write`보다 먼저 호출해야 하며, 호출하지 않으면 첫 `Write` 시점에 200이 암묵적으로 기록됩니다.

## 자주 쓰는 응답 헬퍼

| 함수 | 용도 |
|------|------|
| `http.Error(w, msg, code)` | 상태 코드와 함께 평문 에러 응답 |
| `http.NotFound(w, r)` | 404 응답 |
| `http.Redirect(w, r, url, code)` | 리다이렉트 응답 |
| `http.ServeFile(w, r, name)` | 로컬 파일 하나를 그대로 응답 |
| `http.FileServer(root)` | 디렉터리를 정적 파일 핸들러로 노출 |

```go
http.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("./public"))))
```

## Server 구조체와 정상 종료

`ListenAndServe`는 내부적으로 기본 설정의 `*Server`를 만들어 실행하는 것과 같습니다. 타임아웃 등을 조정하려면 `Server`를 직접 구성합니다.

```go
type Server struct {
    Addr         string
    Handler      Handler
    ReadTimeout  time.Duration
    WriteTimeout time.Duration
}

func (s *Server) ListenAndServe() error
func (s *Server) Shutdown(ctx context.Context) error
```

```go
srv := &http.Server{Addr: ":8080", Handler: mux, ReadTimeout: 5 * time.Second}

go srv.ListenAndServe()

// 인터럽트 신호를 받으면
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()
srv.Shutdown(ctx) // 처리 중인 요청을 기다렸다가 종료
```

## 미들웨어 패턴

`net/http`에는 미들웨어 전용 타입이 없지만, `Handler`를 감싸는 함수로 흔히 구현합니다.

```go
func withLogging(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        start := time.Now()
        next.ServeHTTP(w, r)
        log.Printf("%s %s %v", r.Method, r.URL.Path, time.Since(start))
    })
}

http.ListenAndServe(":8080", withLogging(mux))
```

## 상태 코드 상수

`http.StatusOK`, `http.StatusBadRequest`, `http.StatusNotFound`, `http.StatusInternalServerError`처럼 `Status*` 상수가 RFC의 모든 코드를 정의하고 있어 매직 넘버 대신 사용합니다.

---

# net/url 패키지

> **원문:** https://pkg.go.dev/net/url

## 개요

`net/url`은 URL 문자열을 구조체로 파싱하고 다시 문자열로 조립하는 역할을 합니다. `net/http`의 `Request.URL` 필드도 이 패키지의 `*URL` 타입입니다.

## URL 파싱

```go
func Parse(rawURL string) (*URL, error)
func ParseRequestURI(rawURL string) (*URL, error)
```

- `Parse`는 상대 경로만 있는 문자열도 허용
- `ParseRequestURI`는 HTTP 요청 라인에 등장하는 형태(절대 URL 또는 절대 경로)만 허용, 프래그먼트(`#`) 없음을 가정

```go
u, err := url.Parse("https://example.com/search?q=golang#top")
```

## URL 구조체

```go
type URL struct {
    Scheme   string // "https"
    Host     string // "example.com:443"
    Path     string // "/search" (디코딩된 형태)
    RawQuery string // "q=golang" ('?' 제외, 인코딩된 형태)
    Fragment string // "top" (디코딩된 형태)
}
```

`Path`, `Fragment`는 이미 디코딩된 상태로 저장되므로, 인코딩된 값이 필요하면 `EscapedPath()`, `EscapedFragment()`를 사용합니다.

## 자주 쓰는 메서드

| 메서드 | 설명 |
|--------|------|
| `(*URL) String() string` | 인코딩된 전체 URL 문자열 조립 |
| `(*URL) Query() Values` | `RawQuery`를 파싱해 `Values` 반환 |
| `(*URL) Hostname() string` | 포트를 뺀 호스트만 반환 |
| `(*URL) Port() string` | 포트만 반환 |
| `(*URL) JoinPath(elem ...string) *URL` | 경로 요소를 이어붙인 새 URL 반환 |

```go
u, _ := url.Parse("https://example.com:8080/api")
fmt.Println(u.Hostname()) // "example.com"
fmt.Println(u.Port())     // "8080"

next := u.JoinPath("v2", "users")
fmt.Println(next) // "https://example.com:8080/api/v2/users"
```

## 쿼리 파라미터: Values

`Values`는 `map[string][]string`으로, 같은 키가 여러 값을 가질 수 있는 쿼리 파라미터를 표현합니다.

```go
type Values map[string][]string

func ParseQuery(query string) (Values, error)
func (v Values) Get(key string) string
func (v Values) Set(key, value string)
func (v Values) Add(key, value string)
func (v Values) Del(key string)
func (v Values) Encode() string
```

```go
q := url.Values{}
q.Set("name", "gopher")
q.Add("tag", "go")
q.Add("tag", "backend")

u, _ := url.Parse("https://example.com/search")
u.RawQuery = q.Encode()
fmt.Println(u.String()) // "https://example.com/search?name=gopher&tag=go&tag=backend"
```

기존 URL에서 쿼리를 읽고 고칠 때는 `URL.Query()`로 얻은 값을 수정한 뒤 다시 `Encode()`해서 `RawQuery`에 대입해야 반영됩니다.

## 이스케이프 함수

경로 세그먼트와 쿼리 문자열은 이스케이프 규칙이 다르므로(예: 공백을 `+`로 바꾸는지 여부) 용도에 맞는 함수를 골라 씁니다.

| 함수 | 용도 | 공백 처리 | `+` 처리 |
|------|------|-----------|-----------|
| `QueryEscape` / `QueryUnescape` | 쿼리 문자열 값 | 공백 → `+` | `+` → `%2B` (이스케이프됨) |
| `PathEscape` / `PathUnescape` | 경로 세그먼트 | 공백 → `%20` | `+`는 그대로 유지 |

```go
url.QueryEscape("go lang")  // "go+lang"
url.PathEscape("go lang")   // "go%20lang"
```

## 정리

세 패키지는 역할이 뚜렷하게 나뉩니다.

- `net`: TCP/UDP 소켓, DNS 조회 등 전송 계층
- `net/http`: `net` 위에 얹은 HTTP 클라이언트/서버
- `net/url`: URL 문자열 파싱·조립, `net/http`와 독립적으로도 사용 가능

간단한 웹 서버나 API 클라이언트를 만든다면 대부분 `net/http`와 `net/url`만으로 충분하고, `net`은 커스텀 프로토콜이나 저수준 소켓 제어가 필요할 때 직접 다루게 됩니다.
