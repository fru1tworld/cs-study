# 해시와 암호화 기초 (crypto/hash)

Go 표준 라이브러리는 해시 함수를 위한 공통 인터페이스(`hash`)와, 그 인터페이스를 구현하는 여러 구현체(`crypto/sha256`, `crypto/md5` 등), 그리고 암호학적으로 안전한 난수를 생성하는 `crypto/rand`를 제공합니다. 이 문서는 이 네 패키지의 핵심 사용법을 다룹니다.

---

# hash — 해시 함수 공통 인터페이스

> **원문:** https://pkg.go.dev/hash

## 개요

`hash` 패키지는 구체적인 알고리즘을 담고 있지 않습니다. 대신 `sha256`, `md5`, `crc32` 등 모든 해시 구현체가 공통으로 따르는 인터페이스만 정의합니다. 덕분에 알고리즘을 바꾸더라도 `Write` / `Sum` 호출 코드는 그대로 유지할 수 있습니다.

## Hash 인터페이스

```go
type Hash interface {
    io.Writer
    Sum(b []byte) []byte
    Reset()
    Size() int
    BlockSize() int
}
```

- **`io.Writer` 임베딩** — `Write(p []byte) (n int, err error)`로 데이터를 스트리밍 방식으로 계속 넣을 수 있습니다. 한 번에 다 넣지 않아도 되고, 파일이나 네트워크 스트림에서 읽은 조각을 순서대로 넣어도 결과는 동일합니다.
- **`Sum(b []byte) []byte`** — 지금까지 누적된 해시 값을 `b` 뒤에 append해서 반환합니다. 내부 상태는 변경되지 않으므로 `Sum` 이후에도 `Write`를 이어갈 수 있습니다. 보통 `nil`을 넘겨서 새 슬라이스로 결과만 받습니다.
- **`Reset()`** — 내부 상태를 초기값으로 되돌려 같은 `Hash` 값을 재사용할 수 있게 합니다.
- **`Size()` / `BlockSize()`** — 각각 `Sum`이 반환할 바이트 수, 내부적으로 처리하는 블록 크기를 알려줍니다.

```go
func printSum(h hash.Hash, data []byte) {
    h.Reset()
    h.Write(data)
    fmt.Printf("%x\n", h.Sum(nil))
}
```

## Hash32 / Hash64

체크섬류(CRC32, FNV 등)처럼 결과를 정수로 바로 쓰고 싶을 때를 위한 확장 인터페이스입니다.

```go
type Hash32 interface {
    Hash
    Sum32() uint32
}

type Hash64 interface {
    Hash
    Sum64() uint64
}
```

`[]byte` 형태의 `Sum` 대신 고정폭 정수를 바로 얻고 싶을 때 사용합니다.

## Cloner / XOF (최신 버전 추가)

- **`Cloner`** — `Clone() (Cloner, error)` 메서드로 현재까지 누적된 해시 상태를 복제합니다. 같은 접두사를 공유하는 여러 데이터에 대해 반복 계산을 피하고 싶을 때 유용합니다(예: 같은 헤더 뒤에 여러 바디를 이어 붙여 해시할 때).
- **`XOF`** (extendable-output function) — 출력 길이가 고정되지 않은 해시(SHA-3의 SHAKE 계열 등)를 위한 인터페이스로, `Sum` 대신 `Read`로 원하는 만큼 출력을 뽑아낼 수 있습니다.

이 두 인터페이스는 표준 라이브러리의 대부분 해시 구현체가 이미 만족합니다.

---

# crypto/sha256 — SHA-256 / SHA-224 해시

> **원문:** https://pkg.go.dev/crypto/sha256

## 상수

| 이름 | 값 | 의미 |
|---|---|---|
| `Size` | 32 | SHA-256 체크섬의 바이트 길이 |
| `Size224` | 28 | SHA-224 체크섬의 바이트 길이 |
| `BlockSize` | 64 | 내부 처리 블록 크기 |

## 한 번에 계산: Sum256 / Sum224

데이터 전체가 이미 메모리에 있을 때 가장 간단한 방법입니다.

```go
sum := sha256.Sum256([]byte("hello world"))
fmt.Printf("%x\n", sum) // [32]byte 고정 배열 반환
```

`Sum224`도 시그니처는 동일하되 `[Size224]byte`를 반환합니다. 반환 타입이 슬라이스가 아니라 **고정 크기 배열**이라는 점이 특징이며, 필요하면 `sum[:]`로 슬라이스화해서 사용합니다.

## 스트리밍 계산: New / New224

큰 파일이나 네트워크 스트림처럼 한 번에 메모리에 올리기 어려운 데이터를 처리할 때는 `hash.Hash`를 반환하는 `New`를 사용합니다.

```go
h := sha256.New()
f, _ := os.Open("bigfile.bin")
defer f.Close()
io.Copy(h, f)          // 파일을 조금씩 읽어 h.Write로 흘려보냄
fmt.Printf("%x\n", h.Sum(nil))
```

`New224()`는 SHA-224 버전의 `hash.Hash`를 만듭니다. 두 함수가 반환하는 `Hash`는 `encoding.BinaryMarshaler` / `BinaryUnmarshaler`도 구현하므로, 진행 중인 해시 상태를 직렬화해 저장했다가 나중에 이어서 계산할 수도 있습니다.

## 언제 Sum을, 언제 New를 쓰나

- 데이터가 작고 이미 `[]byte`로 존재 → `Sum256`/`Sum224`가 더 간단합니다.
- 파일·스트림처럼 크거나 조각 단위로 들어오는 데이터 → `New()`로 `hash.Hash`를 만들어 `io.Copy` 등으로 흘려보냅니다.

---

# crypto/md5 — MD5 해시

> **원문:** https://pkg.go.dev/crypto/md5

## 경고

MD5는 충돌 공격에 취약한 것으로 알려져 이미 **암호학적으로 깨진** 알고리즘입니다. 비밀번호 해싱, 서명, 무결성 검증 등 보안이 중요한 용도로는 사용하면 안 됩니다. 레거시 포맷 호환이나 비보안 용도의 체크섬(예: 캐시 키, 중복 파일 탐지)에서만 사용하세요.

## 상수

| 이름 | 값 |
|---|---|
| `Size` | 16 |
| `BlockSize` | 64 |

## API

`sha256`과 API 형태가 동일합니다.

```go
// 한 번에 계산
sum := md5.Sum([]byte("data"))       // [16]byte 반환

// 스트리밍 계산
h := md5.New()                        // hash.Hash 반환
io.WriteString(h, "chunk1")
io.WriteString(h, "chunk2")
fmt.Printf("%x\n", h.Sum(nil))
```

- `Sum(data []byte) [Size]byte` — 즉시 계산.
- `New() hash.Hash` — 스트리밍 계산, `Write`/`Sum`/`Reset` 등 `hash.Hash` 전체 인터페이스 사용 가능.

`sha256`, `md5`뿐 아니라 `crypto/sha1`, `crypto/sha512` 등도 전부 같은 `Sum*` / `New*` 패턴을 따르므로, 한 패키지의 사용법을 익히면 나머지는 이름만 바꿔서 그대로 적용할 수 있습니다.

---

# crypto/rand — 암호학적으로 안전한 난수

> **원문:** https://pkg.go.dev/crypto/rand

`math/rand`는 예측 가능한 의사난수를 만들기 때문에 토큰, 세션 키, 솔트(salt), 암호화 키 등 보안이 필요한 값에는 절대 쓰면 안 됩니다. 이런 용도에는 반드시 `crypto/rand`를 사용해야 합니다.

## Reader — 전역 난수 소스

```go
var Reader io.Reader
```

운영체제가 제공하는 안전한 난수 소스(리눅스 `getrandom`, macOS `arc4random_buf`, 윈도우 `ProcessPrng` 등)를 감싼 `io.Reader`입니다. 동시성 환경에서도 안전하게 공유해서 쓸 수 있습니다. 아래 `Read`, `Int`, `Prime`의 내부 구현이자, 다른 API에 `io.Reader`가 필요할 때 그대로 넘기는 용도로도 쓰입니다.

## Read — 임의 바이트 채우기

```go
func Read(b []byte) (n int, err error)
```

슬라이스 `b`를 안전한 난수 바이트로 완전히 채웁니다. 정상적인 환경에서는 오류를 반환하지 않으며(리턴값은 항상 `len(b), nil`), 내부적으로 문제가 생기면 에러를 반환하는 대신 프로세스를 크래시시킵니다. AES 키 같은 대칭키를 만들 때 가장 많이 쓰는 함수입니다.

```go
key := make([]byte, 32) // AES-256 키
if _, err := rand.Read(key); err != nil {
    log.Fatal(err)
}
```

## Text — 랜덤 토큰 문자열

```go
func Text() string
```

RFC 4648 base32 알파벳으로 인코딩된, 128비트 이상의 엔트로피를 가진 랜덤 문자열을 바로 반환합니다. API 키, 임시 비밀번호, CSRF 토큰처럼 사람이 다루는 랜덤 문자열이 필요할 때 `Read` + 인코딩을 직접 조합하지 않아도 되는 편의 함수입니다.

```go
token := rand.Text()
fmt.Println(token) // 예: "JBSWY3DPEHPK3PXP..."
```

## Int / Prime — 큰 정수 난수

`math/big`과 함께 사용하는 함수로, 임의 정밀도 정수 범위의 난수나 소수를 구할 때 씁니다.

```go
func Int(rand io.Reader, max *big.Int) (n *big.Int, err error)
func Prime(r io.Reader, bits int) (*big.Int, error)
```

- `Int(rand.Reader, max)` — `[0, max)` 구간에서 균등 분포로 정수를 뽑습니다. `max`가 0 이하면 패닉합니다.
- `Prime(rand.Reader, bits)` — 지정한 비트 길이를 가지며 소수일 확률이 매우 높은 수를 반환합니다. RSA 키 생성처럼 소수가 필요한 자리에 사용됩니다.

```go
n, _ := rand.Int(rand.Reader, big.NewInt(100))   // [0,100) 사이 정수
p, _ := rand.Prime(rand.Reader, 128)             // 128비트 소수 후보
```

## 핵심 요점

- 해시가 필요하면 `hash.Hash` 인터페이스(`Write`/`Sum`/`Reset`)를 기준으로 코드를 짜고, 구체적인 알고리즘은 `sha256.New()`처럼 생성자만 바꿔 끼운다.
- 데이터가 이미 다 있으면 `Sum256`/`Sum` 같은 원샷 함수, 스트림이면 `New()` + `io.Copy`.
- MD5, SHA-1은 보안 목적으로 쓰지 말고 체크섬 용도로만 사용한다. 보안이 필요하면 SHA-256 이상을 사용한다.
- 키, 토큰, 솔트 등 보안이 걸린 난수는 `math/rand`가 아니라 반드시 `crypto/rand`(`Read`, `Text`, `Int`, `Prime`)를 사용한다.
