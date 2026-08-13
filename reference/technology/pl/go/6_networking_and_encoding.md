# Go 네트워킹과 데이터 인코딩

## 네트워킹과 HTTP 기초 (net / net/http / net/url)

Go 표준 라이브러리는 소켓 수준의 저수준 네트워킹부터 HTTP 클라이언트·서버, URL 파싱까지 별도 외부 패키지 없이 지원. 세 패키지는 계층적으로 연결됨.

- `net` — TCP/UDP/Unix 소켓 등 전송 계층 API
- `net/http` — `net`을 기반으로 만든 HTTP 클라이언트/서버
- `net/url` — URL 문자열의 파싱과 조립

---

## net 패키지

> 원문: https://pkg.go.dev/net

### 개요

`net`은 TCP/IP, UDP, 도메인 이름 해석, Unix 도메인 소켓을 위한 이식성 있는 인터페이스 제공. 대부분의 코드는 `net/http`처럼 상위 패키지를 통해 간접적으로 사용 → 원시 소켓 통신이 필요할 때만 직접 다룸.

### 연결 맺기: Dial 계열

가장 자주 쓰는 진입점은 `Dial`. 네트워크 종류와 주소만 주면 해당 프로토콜에 맞는 연결 반환.

```go
func Dial(network, address string) (Conn, error)
func DialTimeout(network, address string, timeout time.Duration) (Conn, error)
```

- `network`: `"tcp"`, `"tcp4"`, `"tcp6"`, `"udp"`, `"unix"` 등
- `address`: TCP/UDP는 `"host:port"`, Unix 소켓은 파일 경로
- `DialTimeout`은 이름 해석과 연결 시도 전체에 타임아웃을 걺

```go
conn, err := net.Dial("tcp", "example.com:80")
if err != nil {
    log.Fatal(err)
}
defer conn.Close()
```

더 세밀한 제어(컨텍스트 취소, keep-alive 주기 등)가 필요하면 `Dialer` 구조체를 직접 만들어 `DialContext` 호출.

```go
d := &net.Dialer{Timeout: 5 * time.Second}
conn, err := d.DialContext(ctx, "tcp", "example.com:443")
```

### 서버 쪽: Listen과 Accept

서버는 `Listen`으로 리스너를 만든 뒤 `Accept`를 반복 호출해 들어오는 연결을 받는 구조가 기본.

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

TCP/UDP 전용으로 다룰 때는 `ListenTCP`/`ListenUDP`로 `*TCPConn`, `*UDPConn`처럼 더 구체적인 타입 획득 가능.

### 핵심 인터페이스

#### Conn — 양방향 연결

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

- TCP, UDP, Unix 소켓 연결 모두 이 인터페이스 만족
- 데드라인 메서드 → 특정 시각 이후로 I/O가 블로킹되지 않도록 강제하는 용도

#### Listener — 연결 수신

```go
type Listener interface {
    Accept() (Conn, error)
    Close() error
    Addr() Addr
}
```

#### Addr — 주소 표현

```go
type Addr interface {
    Network() string // "tcp", "udp" 등
    String() string  // "192.0.2.1:8080" 같은 문자열
}
```

`TCPAddr`, `UDPAddr`, `IPAddr`, `UnixAddr`가 이 인터페이스 구현 → 각각 `IP`, `Port` 등의 필드 보유.

```go
addr := &net.TCPAddr{IP: net.ParseIP("192.0.2.1"), Port: 8080}
```

### 주소 문자열 해석

문자열 형태의 주소를 구조체로 바꿀 때는 `Resolve*Addr` 계열 사용.

- `ResolveTCPAddr(network, address string) (*TCPAddr, error)`
  - `"host:port"` → `*TCPAddr`
- `ResolveUDPAddr(network, address string) (*UDPAddr, error)`
  - `"host:port"` → `*UDPAddr`
- `ResolveIPAddr(network, address string) (*IPAddr, error)`
  - 호스트 → `*IPAddr`

```go
tcpAddr, err := net.ResolveTCPAddr("tcp", "example.com:80")
```

### 이름 조회(DNS)

- `LookupHost(host string) ([]string, error)` — 호스트명 → IP 문자열 목록
- `LookupIP(host string) ([]IP, error)` — 호스트명 → `IP` 값 목록
- `LookupAddr(addr string) ([]string, error)` — IP → 호스트명(역방향 조회)

```go
addrs, err := net.LookupHost("golang.org")
```

### IP 값 다루기

`ParseIP`로 문자열을 `IP` 타입으로 변환 → `IP`의 메서드로 성질 검사.

```go
func ParseIP(s string) IP
```

```go
ip := net.ParseIP("192.0.2.1")
if ip.IsPrivate() {
    fmt.Println("사설 IP")
}
```

- 주요 `IP` 메서드: `String()`·`To4()`·`To16()`·`IsLoopback()`·`IsPrivate()`·`IsMulticast()`·`Equal(x IP)`
- CIDR 표기(`"192.0.2.0/24"`)는 `ParseCIDR(s string) (IP, *IPNet, error)`로 파싱

### 호스트:포트 조합

IPv6 리터럴 주소(`[::1]:8080`)까지 안전하게 다루려면 문자열을 직접 자르지 말고 다음 헬퍼 사용.

```go
host, port, err := net.SplitHostPort("[::1]:8080")
addr := net.JoinHostPort("::1", "8080") // "[::1]:8080"
```

### 네트워크 인터페이스 조회

```go
ifaces, err := net.Interfaces()
for _, i := range ifaces {
    addrs, _ := i.Addrs()
    fmt.Println(i.Name, addrs)
}
```

### 에러 타입

네트워크 에러는 대부분 `*OpError`나 `*DNSError`로 감싸져 반환됨.

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

## net/http 패키지

> 원문: https://pkg.go.dev/net/http

### 개요

`net/http`는 `net`을 기반으로 HTTP/1.1, HTTP/2 클라이언트와 서버를 구현한 패키지. 외부 웹 프레임워크 없이도 프로덕션급 서버 구축 가능할 만큼 기능이 완결됨.

### 간단한 클라이언트 요청

`http.Get`, `http.Post` 등은 내부적으로 `DefaultClient`를 사용하는 편의 함수.

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

주의: 응답을 다 쓴 뒤에는 반드시 `resp.Body.Close()` 호출 필요 → 커넥션이 재사용됨.

### 커스텀 요청과 Client

헤더 추가, 컨텍스트 지정처럼 세밀한 제어가 필요하면 `NewRequest`로 요청을 만들고 `Client.Do`로 전송.

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

`Client`는 커넥션 풀을 내부에 보유 → 요청마다 새로 만들지 말고 하나를 재사용하는 것이 관례.

### 서버: Handler와 라우팅

서버 쪽의 핵심 개념은 요청을 처리하는 `Handler` 인터페이스.

```go
type Handler interface {
    ServeHTTP(ResponseWriter, *Request)
}

type HandlerFunc func(ResponseWriter, *Request)
```

`HandlerFunc`는 함수를 `Handler`로 바꿔주는 어댑터 → 대부분의 핸들러는 구조체가 아니라 함수로 작성됨.

```go
func hello(w http.ResponseWriter, r *http.Request) {
    fmt.Fprintln(w, "hello")
}

http.HandleFunc("/hello", hello) // DefaultServeMux에 등록
http.ListenAndServe(":8080", nil)
```

라우팅은 `ServeMux`가 담당 → Go 1.22부터는 메서드와 경로 변수를 패턴에 직접 사용 가능.

```go
mux := http.NewServeMux()
mux.HandleFunc("GET /users/{id}", getUser)
mux.HandleFunc("POST /users", createUser)

func getUser(w http.ResponseWriter, r *http.Request) {
    id := r.PathValue("id")
    fmt.Fprintf(w, "user %s", id)
}
```

### Request와 ResponseWriter

`*Request`는 들어온 요청 정보를 담고, `ResponseWriter`는 응답을 쓰는 인터페이스.

```go
type ResponseWriter interface {
    Header() Header
    Write([]byte) (int, error)
    WriteHeader(statusCode int)
}
```

자주 쓰는 `Request` 필드/메서드: `r.Method`·`r.URL`·`r.Header`·`r.Body`·`r.FormValue(key)`·`r.Context()`.

```go
func handler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    w.Write([]byte(`{"ok":true}`))
}
```

`WriteHeader`는 상태 코드를 결정 → 반드시 `Write`보다 먼저 호출 필요, 호출하지 않으면 첫 `Write` 시점에 200이 암묵적으로 기록됨.

### 자주 쓰는 응답 헬퍼

- `http.Error(w, msg, code)` — 상태 코드와 함께 평문 에러 응답
- `http.NotFound(w, r)` — 404 응답
- `http.Redirect(w, r, url, code)` — 리다이렉트 응답
- `http.ServeFile(w, r, name)` — 로컬 파일 하나를 그대로 응답
- `http.FileServer(root)` — 디렉터리를 정적 파일 핸들러로 노출

```go
http.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("./public"))))
```

### Server 구조체와 정상 종료

`ListenAndServe`는 내부적으로 기본 설정의 `*Server`를 만들어 실행하는 것과 동일. 타임아웃 등을 조정하려면 `Server`를 직접 구성.

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

### 미들웨어 패턴

`net/http`에는 미들웨어 전용 타입이 없음 → `Handler`를 감싸는 함수로 흔히 구현.

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

### 상태 코드 상수

`http.StatusOK`·`http.StatusBadRequest`·`http.StatusNotFound`·`http.StatusInternalServerError`처럼 `Status*` 상수가 RFC의 모든 코드를 정의 → 매직 넘버 대신 사용.

---

## net/url 패키지

> 원문: https://pkg.go.dev/net/url

### 개요

`net/url`은 URL 문자열을 구조체로 파싱하고 다시 문자열로 조립하는 역할. `net/http`의 `Request.URL` 필드도 이 패키지의 `*URL` 타입.

### URL 파싱

```go
func Parse(rawURL string) (*URL, error)
func ParseRequestURI(rawURL string) (*URL, error)
```

- `Parse`는 상대 경로만 있는 문자열도 허용
- `ParseRequestURI`는 HTTP 요청 라인에 등장하는 형태(절대 URL 또는 절대 경로)만 허용, 프래그먼트(`#`) 없음을 가정

```go
u, err := url.Parse("https://example.com/search?q=golang#top")
```

### URL 구조체

```go
type URL struct {
    Scheme   string // "https"
    Host     string // "example.com:443"
    Path     string // "/search" (디코딩된 형태)
    RawQuery string // "q=golang" ('?' 제외, 인코딩된 형태)
    Fragment string // "top" (디코딩된 형태)
}
```

`Path`, `Fragment`는 이미 디코딩된 상태로 저장됨 → 인코딩된 값이 필요하면 `EscapedPath()`, `EscapedFragment()` 사용.

### 자주 쓰는 메서드

- `(*URL) String() string` — 인코딩된 전체 URL 문자열 조립
- `(*URL) Query() Values` — `RawQuery`를 파싱해 `Values` 반환
- `(*URL) Hostname() string` — 포트를 뺀 호스트만 반환
- `(*URL) Port() string` — 포트만 반환
- `(*URL) JoinPath(elem ...string) *URL` — 경로 요소를 이어붙인 새 URL 반환

```go
u, _ := url.Parse("https://example.com:8080/api")
fmt.Println(u.Hostname()) // "example.com"
fmt.Println(u.Port())     // "8080"

next := u.JoinPath("v2", "users")
fmt.Println(next) // "https://example.com:8080/api/v2/users"
```

### 쿼리 파라미터: Values

`Values`는 `map[string][]string` → 같은 키가 여러 값을 가질 수 있는 쿼리 파라미터 표현.

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

기존 URL에서 쿼리를 읽고 고칠 때는 `URL.Query()`로 얻은 값을 수정한 뒤 다시 `Encode()`해서 `RawQuery`에 대입해야 반영됨.

### 이스케이프 함수

경로 세그먼트와 쿼리 문자열은 이스케이프 규칙이 다름(예: 공백을 `+`로 바꾸는지 여부) → 용도에 맞는 함수를 골라 사용.

- `QueryEscape` / `QueryUnescape` — 쿼리 문자열 값
  - 공백 처리: 공백 → `+`
  - `+` 처리: `+` → `%2B`(이스케이프됨)
- `PathEscape` / `PathUnescape` — 경로 세그먼트
  - 공백 처리: 공백 → `%20`
  - `+` 처리: `+`는 그대로 유지

```go
url.QueryEscape("go lang")  // "go+lang"
url.PathEscape("go lang")   // "go%20lang"
```

### 정리

세 패키지는 역할이 뚜렷하게 나뉨.

- `net`: TCP/UDP 소켓, DNS 조회 등 전송 계층
- `net/http`: `net` 위에 얹은 HTTP 클라이언트/서버
- `net/url`: URL 문자열 파싱·조립, `net/http`와 독립적으로도 사용 가능

간단한 웹 서버나 API 클라이언트라면 대부분 `net/http`와 `net/url`만으로 충분 → `net`은 커스텀 프로토콜이나 저수준 소켓 제어가 필요할 때 직접 다루게 됨.

---

## 데이터 인코딩 (json/base64/xml/csv/gob/hex)

Go 표준 라이브러리는 `encoding/*` 하위에 JSON, Base64, XML, CSV, gob, hex 등 흔히 쓰는 인코딩 포맷을 위한 패키지 제공. 모두 `[]byte` ↔ Go 값 변환, 또는 `io.Reader`/`io.Writer` 기반 스트리밍 인코딩이라는 공통된 형태를 따름.

---

## encoding/json

> 원문: https://pkg.go.dev/encoding/json

### 개요
Go 값과 JSON 텍스트를 상호 변환. 웹 API, 설정 파일 등 가장 널리 쓰이는 인코딩 패키지.

### 한 번에 변환하기: Marshal / Unmarshal

```go
func Marshal(v any) ([]byte, error)
func MarshalIndent(v any, prefix, indent string) ([]byte, error)
func Unmarshal(data []byte, v any) error
```

- `Marshal`은 Go 값을 JSON 바이트로 직렬화 → 채널·함수·복소수처럼 JSON으로 표현할 수 없는 타입은 에러 반환
- `MarshalIndent`는 들여쓰기를 적용해 사람이 읽기 좋은 형태로 출력
- `Unmarshal`은 반드시 포인터를 두 번째 인자로 받아야 함 → 그렇지 않으면 `InvalidUnmarshalError` 반환

```go
type User struct {
    Name string `json:"name"`
    Age  int    `json:"age"`
}

b, _ := json.Marshal(User{Name: "kim", Age: 20})
// {"name":"kim","age":20}

var u User
_ = json.Unmarshal(b, &u)
```

### 스트림 기반: Encoder / Decoder

파일이나 네트워크 연결처럼 `io.Reader`/`io.Writer`를 다룰 때는 매번 `[]byte`를 만들 필요 없이 스트림 인코더 사용.

```go
func NewEncoder(w io.Writer) *Encoder
func NewDecoder(r io.Reader) *Decoder

func (enc *Encoder) Encode(v any) error
func (dec *Decoder) Decode(v any) error
```

- `Decoder.More()`로 배열/객체 안에 다음 값이 남아 있는지 확인 → 순차적으로 여러 JSON 값 읽기 가능
- `Decoder.DisallowUnknownFields()`를 호출하면 대상 구조체에 없는 필드가 들어올 때 에러(엄격 파싱)
- `Decoder.UseNumber()`를 켜면 숫자를 `float64`가 아니라 `json.Number`(문자열 기반)로 디코딩 → 정밀도 손실 방지

```go
dec := json.NewDecoder(strings.NewReader(`{"a":1}{"a":2}`))
for dec.More() {
    var u User
    _ = dec.Decode(&u)
}
```

### 구조체 태그

`json:"이름,옵션"` 형식으로 필드 이름과 동작 제어.

- `json:"name"` — JSON 키 이름 지정
- `json:"name,omitempty"` — 값이 빈 값(0, "", nil, 빈 슬라이스 등)이면 필드 생략
- `json:"id,string"` — 숫자/불리언을 JSON 문자열로 감싸서 인코딩
- `json:"-"` — 인코딩/디코딩에서 완전히 제외

```go
type Item struct {
    ID    int64  `json:"id,string"`
    Note  string `json:"note,omitempty"`
    Skip  string `json:"-"`
}
```

### 커스텀 직렬화: Marshaler / Unmarshaler

```go
type Marshaler interface {
    MarshalJSON() ([]byte, error)
}
type Unmarshaler interface {
    UnmarshalJSON([]byte) error
}
```

타입이 이 인터페이스를 구현하면 `json` 패키지의 기본 변환 규칙 대신 직접 정의한 로직으로 인코딩/디코딩됨. `time.Duration`을 사람이 읽기 좋은 문자열로 표현하는 등의 용도로 자주 사용.

### 그 외 유용한 도구

- `json.Valid(data []byte) bool` — 바이트 슬라이스가 유효한 JSON인지만 빠르게 검사(파싱 없이)
- `json.RawMessage` — `[]byte`의 별칭, 일부 필드의 파싱을 나중으로 미루거나 이미 만들어진 JSON 조각을 그대로 끼워 넣을 때 사용

```go
type Envelope struct {
    Header json.RawMessage `json:"header"`
    Body   string          `json:"body"`
}
```

### 인코딩 규칙 요약

- `map`은 키가 `string`(또는 `TextMarshaler`)이어야 함 → 출력 시 키를 정렬해 결정적인 순서 보장
- `[]byte`는 자동으로 Base64 문자열로 인코딩됨
- `nil` 슬라이스/맵은 `null`로, 포인터가 `nil`이면 `null`로 직렬화됨
- 구조체는 exported 필드만 대상 → 임베디드 필드는 평탄화되어 취급됨

---

## encoding/base64

> 원문: https://pkg.go.dev/encoding/base64

### 개요
바이너리 데이터를 인쇄 가능한 ASCII 문자열로 변환. 이메일 첨부, URL, JSON 안에 바이너리를 담아야 할 때 사용.

### Encoding 타입과 사전 정의 인코딩

모든 동작은 `*base64.Encoding` 값에 메서드로 붙어 있고, 패키지는 자주 쓰는 조합을 미리 정의해 둠.

- `StdEncoding` — 특수 문자(62·63번째): `+`, `/` / 패딩(`=`): 있음
- `URLEncoding` — 특수 문자(62·63번째): `-`, `_` / 패딩(`=`): 있음
- `RawStdEncoding` — 특수 문자(62·63번째): `+`, `/` / 패딩(`=`): 없음
- `RawURLEncoding` — 특수 문자(62·63번째): `-`, `_` / 패딩(`=`): 없음

- URL이나 파일 이름에 넣을 문자열은 `+`, `/`가 포함되면 안 되므로 `URLEncoding` 계열 사용
- 패딩(`=`)은 출력 길이를 4의 배수로 맞추기 위한 것 → URL 쿼리 파라미터처럼 길이 정렬이 필요 없는 곳에서는 `Raw*` 계열로 생략하는 편이 짧고 깔끔함

### 문자열 변환

```go
func (enc *Encoding) EncodeToString(src []byte) string
func (enc *Encoding) DecodeString(s string) ([]byte, error)
```

```go
s := base64.URLEncoding.EncodeToString([]byte("hello world"))
b, err := base64.URLEncoding.DecodeString(s)
```

### 바이트 슬라이스 / 스트림 변환

버퍼를 직접 관리하고 싶다면 `Encode`/`Decode`와 길이 계산 함수를 함께 사용.

```go
func (enc *Encoding) Encode(dst, src []byte)
func (enc *Encoding) EncodedLen(n int) int

dst := make([]byte, base64.StdEncoding.EncodedLen(len(src)))
base64.StdEncoding.Encode(dst, src)
```

큰 데이터를 스트리밍으로 처리할 때는 `io.Writer`/`io.Reader` 래퍼 사용. `NewEncoder`가 반환하는 writer는 내부 버퍼를 4바이트 단위로 채움 → 마지막에 반드시 `Close()`를 호출해야 잔여 데이터가 플러시됨.

```go
func NewEncoder(enc *Encoding, w io.Writer) io.WriteCloser
func NewDecoder(enc *Encoding, r io.Reader) io.Reader
```

### 커스텀 알파벳

표준 알파벳 이외의 문자 집합이 필요하면 `NewEncoding(alphabet string)`으로 직접 정의 → `WithPadding(rune)`으로 패딩 문자 변경 가능.

---

## encoding/xml

> 원문: https://pkg.go.dev/encoding/xml

### 개요
Go 값과 XML 문서를 상호 변환. JSON보다 규칙이 복잡함 → XML 자체가 순서 있는 트리 구조라 Go의 이름 있는 구조체와 정확히 대응되지 않기 때문. 새 프로젝트라면 가능하면 JSON을 우선 고려하고, 기존 XML 스펙을 다뤄야 할 때 이 패키지 사용.

### Marshal / Unmarshal

```go
func Marshal(v any) ([]byte, error)
func MarshalIndent(v any, prefix, indent string) ([]byte, error)
func Unmarshal(data []byte, v any) error
```

JSON과 마찬가지로 exported 필드만 대상 → 대소문자를 구분해 엘리먼트/속성 이름을 매칭.

### 구조체 태그

`xml:"이름[,옵션]"` 형태로 엘리먼트/속성 매핑 지정.

- `xml:"name"` — 자식 엘리먼트 이름
- `xml:"name,attr"` — 속성(attribute)으로 인코딩
- `xml:",chardata"` — 엘리먼트의 텍스트 내용
- `xml:",cdata"` — `<![CDATA[...]]>`로 감싸서 출력
- `xml:",innerxml"` — 가공하지 않은 XML을 그대로 넣거나 읽음
- `xml:"a>b>c"` — `<a><b><c>` 형태로 중첩된 엘리먼트 생성
- `xml:"-"` — 제외

```go
type Person struct {
    XMLName xml.Name `xml:"person"`
    Name    string   `xml:"name"`
    Age     int      `xml:"age,attr"`
}
// <person age="20"><name>kim</name></person>
```

- `XMLName xml.Name` 필드를 두면 루트 엘리먼트 이름 직접 지정 가능. `xml.Name`은 `{Space, Local}` 두 필드로 네임스페이스와 로컬 이름을 나눠 가짐.

### 스트림 기반: Encoder / Decoder

```go
func NewEncoder(w io.Writer) *Encoder
func NewDecoder(r io.Reader) *Decoder

func (d *Decoder) Token() (Token, error)
```

- `Decoder.Token()`은 XML을 토큰 단위(`StartElement`, `EndElement`, `CharData`, `Comment` 등)로 하나씩 읽음 → 구조체에 딱 맞지 않는 불규칙한 문서를 다룰 때 유용
- 토큰이 가리키는 데이터는 디코더 내부 버퍼를 참조함 → 다음 `Token()` 호출 전에 값을 보관하려면 `xml.CopyToken`으로 복사 필요
- `Decoder.Strict = false`와 `AutoClose`, `Entity` 필드를 조합하면 정형화되지 않은 HTML도 관대하게 파싱 가능

```go
enc := xml.NewEncoder(w)
enc.Indent("", "  ")
_ = enc.Encode(person)
_ = enc.Close()
```

### 커스텀 마샬링

`Marshaler`/`Unmarshaler`(`MarshalXML`/`UnmarshalXML`) 또는 더 단순한 `encoding.TextMarshaler`/`TextUnmarshaler`를 구현해 필드 하나의 인코딩 방식 변경 가능.

---

## encoding/csv

> 원문: https://pkg.go.dev/encoding/csv

### 개요
RFC 4180 규칙을 따르는 CSV(comma-separated values) 파일을 읽고 씀. 레코드는 `[]string`으로 표현됨.

### Reader

```go
func NewReader(r io.Reader) *Reader
func (r *Reader) Read() (record []string, err error)
func (r *Reader) ReadAll() (records [][]string, err error)
```

주요 옵션 필드:

- `Comma rune` — 구분자, 기본값 `,`. 탭 구분 파일이면 `'\t'`로 변경
- `Comment rune` — 이 문자로 시작하는 줄은 통째로 무시됨
- `FieldsPerRecord int` — 0이면 첫 레코드의 필드 수를 기준으로 이후 레코드도 검증, 음수면 레코드마다 필드 수가 달라도 허용
- `LazyQuotes bool` — 따옴표 안에서 이스케이프되지 않은 `"`를 허용 → 다소 지저분한 CSV도 관대하게 읽음
- `TrimLeadingSpace bool` — 필드 앞의 공백 무시

```go
r := csv.NewReader(f)
r.Comma = ';'
records, err := r.ReadAll()
```

한 줄씩 처리하려면 `ReadAll` 대신 `Read`를 `io.EOF`가 나올 때까지 반복 호출.

### Writer

```go
func NewWriter(w io.Writer) *Writer
func (w *Writer) Write(record []string) error
func (w *Writer) WriteAll(records [][]string) error
func (w *Writer) Flush()
func (w *Writer) Error() error
```

- `Write`는 내부 버퍼에만 씀 → 실제로 출력하려면 마지막에 `Flush()` 호출 필요. `WriteAll`은 내부적으로 `Flush`까지 호출함
- `UseCRLF bool` 필드를 켜면 줄바꿈을 `\n` 대신 `\r\n`으로 씀(윈도우 호환용)

```go
w := csv.NewWriter(f)
_ = w.Write([]string{"name", "age"})
_ = w.Write([]string{"kim", "20"})
w.Flush()
if err := w.Error(); err != nil {
    log.Fatal(err)
}
```

### 에러

파싱 실패는 `*csv.ParseError`(줄/컬럼 정보 포함)로 반환됨. 흔한 원인: `ErrBareQuote`(따옴표 없는 필드 안의 `"`)·`ErrFieldCount`(레코드마다 필드 수 불일치) 등.

---

## encoding/gob

> 원문: https://pkg.go.dev/encoding/gob

### 개요
Go 프로그램 간에 값을 주고받기 위한 Go 전용 바이너리 인코딩. `net/rpc`의 기본 인코딩이며, Go-to-Go 통신이나 로컬 캐시 저장처럼 상대방도 Go인 경우에 적합. JSON과 달리 사람이 읽을 수 없고 다른 언어와 상호운용되지 않는 대신, 더 컴팩트하고 타입 정보를 스트림에 함께 실어 보냄(self-describing).

### Encoder / Decoder

```go
func NewEncoder(w io.Writer) *Encoder
func NewDecoder(r io.Reader) *Decoder

func (enc *Encoder) Encode(e any) error
func (dec *Decoder) Decode(e any) error
```

```go
var buf bytes.Buffer
enc := gob.NewEncoder(&buf)
dec := gob.NewDecoder(&buf)

type Point struct{ X, Y int }
_ = enc.Encode(Point{3, 4})

var p Point
_ = dec.Decode(&p)
```

- 같은 `Encoder`/`Decoder`를 여러 값에 재사용하면 두 번째 값부터는 타입 정보를 다시 보내지 않음 → 더 효율적
- 구조체는 exported 필드만 인코딩되고, 필드는 순서가 아니라 이름으로 매칭됨 → 송신측과 수신측 구조체가 완전히 같을 필요 없음
- 값이 제로 값이면 해당 필드는 아예 전송하지 않음 → 대역폭 절약

### interface{} 값 다루기: Register

인터페이스 타입 값을 인코딩하려면 실제 구현 타입을 미리 등록해야 디코더가 어떤 구체 타입인지 복원 가능.

```go
func Register(value any)
func RegisterName(name string, value any)
```

```go
gob.Register(Point{})

var shape Shape = Point{1, 2}
_ = enc.Encode(&shape)
```

### 커스텀 인코딩

```go
type GobEncoder interface {
    GobEncode() ([]byte, error)
}
type GobDecoder interface {
    GobDecode([]byte) error
}
```

이 인터페이스를 구현하면 unexported 필드나 채널처럼 기본 규칙으로는 다룰 수 없는 값도 직접 인코딩 가능.

### 주의할 점
- 장기 보관용 저장 포맷으로는 권장하지 않음. 구조체 필드를 늘리거나 지울 때의 하위 호환은 보장되지만, 세밀한 버전 관리가 필요하면 스키마가 명시적인 포맷(JSON, protobuf 등)이 나음
- 디코더는 신뢰할 수 없는 입력에 대한 검증이 제한적임 → 외부에서 받은 데이터를 그대로 디코딩하는 것은 피해야 함

---

## encoding/hex

> 원문: https://pkg.go.dev/encoding/hex

### 개요
바이트를 16진수 문자열로, 또는 그 반대로 변환. 해시 값 출력, 바이너리 디버깅(hex dump) 등에 사용.

### 문자열 변환

```go
func EncodeToString(src []byte) string
func DecodeString(s string) ([]byte, error)
```

```go
s := hex.EncodeToString([]byte("go"))       // "676f"
b, err := hex.DecodeString(s)                // []byte("go")
```

### 버퍼 기반 변환과 길이 계산

성능이 중요한 경로에서는 버퍼를 미리 할당해 직접 채움.

```go
func Encode(dst, src []byte) int
func Decode(dst, src []byte) (int, error)
func EncodedLen(n int) int  // n * 2
func DecodedLen(x int) int  // x / 2
```

```go
dst := make([]byte, hex.EncodedLen(len(src)))
hex.Encode(dst, src)
```

### 스트림 변환

```go
func NewEncoder(w io.Writer) io.Writer
func NewDecoder(r io.Reader) io.Reader
```

파일이나 소켓에 흘려보내듯 인코딩/디코딩할 때 사용. 스트림 디코더는 길이가 홀수인 입력을 만나면 `ErrLength` 대신 `io.ErrUnexpectedEOF` 반환.

### 디버깅용 덤프

```go
func Dump(data []byte) string
func Dumper(w io.Writer) io.WriteCloser
```

`hexdump -C`와 비슷한 형식(오프셋 + 16진수 + ASCII)으로 바이너리 데이터를 사람이 읽기 좋게 출력. 네트워크 패킷이나 파일 포맷을 디버깅할 때 유용.

```go
fmt.Print(hex.Dump([]byte("hello")))
// 00000000  68 65 6c 6c 6f                                    |hello|
```

### 에러

- `ErrLength` — 16진수 문자열의 길이가 홀수일 때
- `InvalidByteError` — `0-9a-fA-F` 범위를 벗어난 문자가 있을 때
