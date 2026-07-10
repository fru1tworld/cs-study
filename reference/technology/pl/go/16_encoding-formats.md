# 데이터 인코딩 (json/base64/xml/csv/gob/hex)

Go 표준 라이브러리는 `encoding/*` 하위에 JSON, Base64, XML, CSV, gob, hex 등 흔히 쓰는 인코딩 포맷을 위한 패키지를 제공합니다. 모두 `[]byte` ↔ Go 값 변환, 또는 `io.Reader`/`io.Writer` 기반 스트리밍 인코딩이라는 공통된 형태를 따릅니다.

---

# encoding/json

> **원문:** https://pkg.go.dev/encoding/json

## 개요
Go 값과 JSON 텍스트를 상호 변환합니다. 웹 API, 설정 파일 등 가장 널리 쓰이는 인코딩 패키지입니다.

## 한 번에 변환하기: Marshal / Unmarshal

```go
func Marshal(v any) ([]byte, error)
func MarshalIndent(v any, prefix, indent string) ([]byte, error)
func Unmarshal(data []byte, v any) error
```

- `Marshal`은 Go 값을 JSON 바이트로 직렬화합니다. 채널·함수·복소수처럼 JSON으로 표현할 수 없는 타입은 에러를 반환합니다.
- `MarshalIndent`는 들여쓰기를 적용해 사람이 읽기 좋은 형태로 출력합니다.
- `Unmarshal`은 반드시 포인터를 두 번째 인자로 받아야 하며, 그렇지 않으면 `InvalidUnmarshalError`를 반환합니다.

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

## 스트림 기반: Encoder / Decoder

파일이나 네트워크 연결처럼 `io.Reader`/`io.Writer`를 다룰 때는 매번 `[]byte`를 만들 필요 없이 스트림 인코더를 씁니다.

```go
func NewEncoder(w io.Writer) *Encoder
func NewDecoder(r io.Reader) *Decoder

func (enc *Encoder) Encode(v any) error
func (dec *Decoder) Decode(v any) error
```

- `Decoder.More()`로 배열/객체 안에 다음 값이 남아 있는지 확인하며 순차적으로 여러 JSON 값을 읽을 수 있습니다.
- `Decoder.DisallowUnknownFields()`를 호출하면 대상 구조체에 없는 필드가 들어올 때 에러가 됩니다(엄격 파싱).
- `Decoder.UseNumber()`를 켜면 숫자를 `float64`가 아니라 `json.Number`(문자열 기반)로 디코딩해 정밀도 손실을 막습니다.

```go
dec := json.NewDecoder(strings.NewReader(`{"a":1}{"a":2}`))
for dec.More() {
    var u User
    _ = dec.Decode(&u)
}
```

## 구조체 태그

`json:"이름,옵션"` 형식으로 필드 이름과 동작을 제어합니다.

| 태그 | 의미 |
|---|---|
| `json:"name"` | JSON 키 이름 지정 |
| `json:"name,omitempty"` | 값이 빈 값(0, "", nil, 빈 슬라이스 등)이면 필드 생략 |
| `json:"id,string"` | 숫자/불리언을 JSON 문자열로 감싸서 인코딩 |
| `json:"-"` | 인코딩/디코딩에서 완전히 제외 |

```go
type Item struct {
    ID    int64  `json:"id,string"`
    Note  string `json:"note,omitempty"`
    Skip  string `json:"-"`
}
```

## 커스텀 직렬화: Marshaler / Unmarshaler

```go
type Marshaler interface {
    MarshalJSON() ([]byte, error)
}
type Unmarshaler interface {
    UnmarshalJSON([]byte) error
}
```

타입이 이 인터페이스를 구현하면 `json` 패키지의 기본 변환 규칙 대신 직접 정의한 로직으로 인코딩/디코딩됩니다. `time.Duration`을 사람이 읽기 좋은 문자열로 표현하는 등의 용도로 자주 쓰입니다.

## 그 외 유용한 도구

- `json.Valid(data []byte) bool` — 바이트 슬라이스가 유효한 JSON인지만 빠르게 검사(파싱 없이).
- `json.RawMessage` — `[]byte`의 별칭으로, 일부 필드의 파싱을 나중으로 미루거나 이미 만들어진 JSON 조각을 그대로 끼워 넣을 때 사용합니다.

```go
type Envelope struct {
    Header json.RawMessage `json:"header"`
    Body   string          `json:"body"`
}
```

## 인코딩 규칙 요약

- `map`은 키가 `string`(또는 `TextMarshaler`)이어야 하며, 출력 시 키를 정렬해 결정적인 순서를 보장합니다.
- `[]byte`는 자동으로 Base64 문자열로 인코딩됩니다.
- `nil` 슬라이스/맵은 `null`로, 포인터가 `nil`이면 `null`로 직렬화됩니다.
- 구조체는 exported 필드만 대상이 되며, 임베디드 필드는 평탄화되어 취급됩니다.

---

# encoding/base64

> **원문:** https://pkg.go.dev/encoding/base64

## 개요
바이너리 데이터를 인쇄 가능한 ASCII 문자열로 변환합니다. 이메일 첨부, URL, JSON 안에 바이너리를 담아야 할 때 사용합니다.

## Encoding 타입과 사전 정의 인코딩

모든 동작은 `*base64.Encoding` 값에 메서드로 붙어 있고, 패키지는 자주 쓰는 조합을 미리 정의해 둡니다.

| 변수 | 특수 문자(62, 63번째) | 패딩(`=`) |
|---|---|---|
| `StdEncoding` | `+`, `/` | 있음 |
| `URLEncoding` | `-`, `_` | 있음 |
| `RawStdEncoding` | `+`, `/` | 없음 |
| `RawURLEncoding` | `-`, `_` | 없음 |

- URL이나 파일 이름에 넣을 문자열은 `+`, `/`가 포함되면 안 되므로 `URLEncoding` 계열을 씁니다.
- 패딩(`=`)은 출력 길이를 4의 배수로 맞추기 위한 것으로, URL 쿼리 파라미터처럼 길이 정렬이 필요 없는 곳에서는 `Raw*` 계열로 생략하는 편이 짧고 깔끔합니다.

## 문자열 변환

```go
func (enc *Encoding) EncodeToString(src []byte) string
func (enc *Encoding) DecodeString(s string) ([]byte, error)
```

```go
s := base64.URLEncoding.EncodeToString([]byte("hello world"))
b, err := base64.URLEncoding.DecodeString(s)
```

## 바이트 슬라이스 / 스트림 변환

버퍼를 직접 관리하고 싶다면 `Encode`/`Decode`와 길이 계산 함수를 함께 씁니다.

```go
func (enc *Encoding) Encode(dst, src []byte)
func (enc *Encoding) EncodedLen(n int) int

dst := make([]byte, base64.StdEncoding.EncodedLen(len(src)))
base64.StdEncoding.Encode(dst, src)
```

큰 데이터를 스트리밍으로 처리할 때는 `io.Writer`/`io.Reader` 래퍼를 사용합니다. `NewEncoder`가 반환하는 writer는 내부 버퍼를 4바이트 단위로 채우므로, 마지막에 반드시 `Close()`를 호출해야 잔여 데이터가 플러시됩니다.

```go
func NewEncoder(enc *Encoding, w io.Writer) io.WriteCloser
func NewDecoder(enc *Encoding, r io.Reader) io.Reader
```

## 커스텀 알파벳

표준 알파벳 이외의 문자 집합이 필요하면 `NewEncoding(alphabet string)`으로 직접 정의하고, `WithPadding(rune)`으로 패딩 문자를 바꿀 수 있습니다.

---

# encoding/xml

> **원문:** https://pkg.go.dev/encoding/xml

## 개요
Go 값과 XML 문서를 상호 변환합니다. JSON보다 규칙이 복잡한데, XML 자체가 순서 있는 트리 구조라 Go의 이름 있는 구조체와 정확히 대응되지 않기 때문입니다. 새 프로젝트라면 가능하면 JSON을 우선 고려하고, 기존 XML 스펙을 다뤄야 할 때 이 패키지를 사용합니다.

## Marshal / Unmarshal

```go
func Marshal(v any) ([]byte, error)
func MarshalIndent(v any, prefix, indent string) ([]byte, error)
func Unmarshal(data []byte, v any) error
```

JSON과 마찬가지로 exported 필드만 대상이며, 대소문자를 구분해 엘리먼트/속성 이름을 매칭합니다.

## 구조체 태그

`xml:"이름[,옵션]"` 형태로 엘리먼트/속성 매핑을 지정합니다.

| 태그 | 의미 |
|---|---|
| `xml:"name"` | 자식 엘리먼트 이름 |
| `xml:"name,attr"` | 속성(attribute)으로 인코딩 |
| `xml:",chardata"` | 엘리먼트의 텍스트 내용 |
| `xml:",cdata"` | `<![CDATA[...]]>`로 감싸서 출력 |
| `xml:",innerxml"` | 가공하지 않은 XML을 그대로 넣거나 읽음 |
| `xml:"a>b>c"` | `<a><b><c>` 형태로 중첩된 엘리먼트 생성 |
| `xml:"-"` | 제외 |

```go
type Person struct {
    XMLName xml.Name `xml:"person"`
    Name    string   `xml:"name"`
    Age     int      `xml:"age,attr"`
}
// <person age="20"><name>kim</name></person>
```

- `XMLName xml.Name` 필드를 두면 루트 엘리먼트 이름을 직접 지정할 수 있습니다. `xml.Name`은 `{Space, Local}` 두 필드로 네임스페이스와 로컬 이름을 나눠 갖습니다.

## 스트림 기반: Encoder / Decoder

```go
func NewEncoder(w io.Writer) *Encoder
func NewDecoder(r io.Reader) *Decoder

func (d *Decoder) Token() (Token, error)
```

- `Decoder.Token()`은 XML을 토큰 단위(`StartElement`, `EndElement`, `CharData`, `Comment` 등)로 하나씩 읽습니다. 구조체에 딱 맞지 않는 불규칙한 문서를 다룰 때 유용합니다.
- 토큰이 가리키는 데이터는 디코더 내부 버퍼를 참조하므로, 다음 `Token()` 호출 전에 값을 보관하려면 `xml.CopyToken`으로 복사해야 합니다.
- `Decoder.Strict = false`와 `AutoClose`, `Entity` 필드를 조합하면 정형화되지 않은 HTML도 관대하게 파싱할 수 있습니다.

```go
enc := xml.NewEncoder(w)
enc.Indent("", "  ")
_ = enc.Encode(person)
_ = enc.Close()
```

## 커스텀 마샬링

`Marshaler`/`Unmarshaler`(`MarshalXML`/`UnmarshalXML`) 또는 더 단순한 `encoding.TextMarshaler`/`TextUnmarshaler`를 구현해 필드 하나의 인코딩 방식을 바꿀 수 있습니다.

---

# encoding/csv

> **원문:** https://pkg.go.dev/encoding/csv

## 개요
RFC 4180 규칙을 따르는 CSV(comma-separated values) 파일을 읽고 씁니다. 레코드는 `[]string`으로 표현됩니다.

## Reader

```go
func NewReader(r io.Reader) *Reader
func (r *Reader) Read() (record []string, err error)
func (r *Reader) ReadAll() (records [][]string, err error)
```

주요 옵션 필드:

- `Comma rune` — 구분자, 기본값 `,`. 탭 구분 파일이면 `'\t'`로 바꿉니다.
- `Comment rune` — 이 문자로 시작하는 줄은 통째로 무시됩니다.
- `FieldsPerRecord int` — 0이면 첫 레코드의 필드 수를 기준으로 이후 레코드도 검증하고, 음수면 레코드마다 필드 수가 달라도 허용합니다.
- `LazyQuotes bool` — 따옴표 안에서 이스케이프되지 않은 `"`를 허용해 다소 지저분한 CSV도 관대하게 읽습니다.
- `TrimLeadingSpace bool` — 필드 앞의 공백을 무시합니다.

```go
r := csv.NewReader(f)
r.Comma = ';'
records, err := r.ReadAll()
```

한 줄씩 처리하려면 `ReadAll` 대신 `Read`를 `io.EOF`가 나올 때까지 반복 호출합니다.

## Writer

```go
func NewWriter(w io.Writer) *Writer
func (w *Writer) Write(record []string) error
func (w *Writer) WriteAll(records [][]string) error
func (w *Writer) Flush()
func (w *Writer) Error() error
```

- `Write`는 내부 버퍼에만 쓰므로 실제로 출력하려면 마지막에 `Flush()`를 호출해야 합니다. `WriteAll`은 내부적으로 `Flush`까지 호출해 줍니다.
- `UseCRLF bool` 필드를 켜면 줄바꿈을 `\n` 대신 `\r\n`으로 씁니다(윈도우 호환용).

```go
w := csv.NewWriter(f)
_ = w.Write([]string{"name", "age"})
_ = w.Write([]string{"kim", "20"})
w.Flush()
if err := w.Error(); err != nil {
    log.Fatal(err)
}
```

## 에러

파싱 실패는 `*csv.ParseError`(줄/컬럼 정보 포함)로 반환되며, 흔한 원인은 `ErrBareQuote`(따옴표 없는 필드 안의 `"`), `ErrFieldCount`(레코드마다 필드 수 불일치) 등입니다.

---

# encoding/gob

> **원문:** https://pkg.go.dev/encoding/gob

## 개요
Go 프로그램 간에 값을 주고받기 위한 Go 전용 바이너리 인코딩입니다. `net/rpc`의 기본 인코딩이며, Go-to-Go 통신이나 로컬 캐시 저장처럼 상대방도 Go인 경우에 적합합니다. JSON과 달리 사람이 읽을 수 없고 다른 언어와 상호운용되지 않는 대신, 더 컴팩트하고 타입 정보를 스트림에 함께 실어 보냅니다(self-describing).

## Encoder / Decoder

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

- 같은 `Encoder`/`Decoder`를 여러 값에 재사용하면 두 번째 값부터는 타입 정보를 다시 보내지 않아 더 효율적입니다.
- 구조체는 exported 필드만 인코딩되고, 필드는 순서가 아니라 이름으로 매칭되므로 송신측과 수신측 구조체가 완전히 같을 필요는 없습니다.
- 값이 제로 값이면 해당 필드는 아예 전송하지 않아 대역폭을 아낍니다.

## interface{} 값 다루기: Register

인터페이스 타입 값을 인코딩하려면 실제 구현 타입을 미리 등록해야 디코더가 어떤 구체 타입인지 복원할 수 있습니다.

```go
func Register(value any)
func RegisterName(name string, value any)
```

```go
gob.Register(Point{})

var shape Shape = Point{1, 2}
_ = enc.Encode(&shape)
```

## 커스텀 인코딩

```go
type GobEncoder interface {
    GobEncode() ([]byte, error)
}
type GobDecoder interface {
    GobDecode([]byte) error
}
```

이 인터페이스를 구현하면 unexported 필드나 채널처럼 기본 규칙으로는 다룰 수 없는 값도 직접 인코딩할 수 있습니다.

## 주의할 점
- 장기 보관용 저장 포맷으로는 권장하지 않습니다. 구조체 필드를 늘리거나 지울 때의 하위 호환은 보장되지만, 세밀한 버전 관리가 필요하면 스키마가 명시적인 포맷(JSON, protobuf 등)이 낫습니다.
- 디코더는 신뢰할 수 없는 입력에 대한 검증이 제한적이므로, 외부에서 받은 데이터를 그대로 디코딩하는 것은 피해야 합니다.

---

# encoding/hex

> **원문:** https://pkg.go.dev/encoding/hex

## 개요
바이트를 16진수 문자열로, 또는 그 반대로 변환합니다. 해시 값 출력, 바이너리 디버깅(hex dump) 등에 쓰입니다.

## 문자열 변환

```go
func EncodeToString(src []byte) string
func DecodeString(s string) ([]byte, error)
```

```go
s := hex.EncodeToString([]byte("go"))       // "676f"
b, err := hex.DecodeString(s)                // []byte("go")
```

## 버퍼 기반 변환과 길이 계산

성능이 중요한 경로에서는 버퍼를 미리 할당해 직접 채웁니다.

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

## 스트림 변환

```go
func NewEncoder(w io.Writer) io.Writer
func NewDecoder(r io.Reader) io.Reader
```

파일이나 소켓에 흘려보내듯 인코딩/디코딩할 때 사용합니다. 스트림 디코더는 길이가 홀수인 입력을 만나면 `ErrLength` 대신 `io.ErrUnexpectedEOF`를 반환합니다.

## 디버깅용 덤프

```go
func Dump(data []byte) string
func Dumper(w io.Writer) io.WriteCloser
```

`hexdump -C`와 비슷한 형식(오프셋 + 16진수 + ASCII)으로 바이너리 데이터를 사람이 읽기 좋게 출력합니다. 네트워크 패킷이나 파일 포맷을 디버깅할 때 유용합니다.

```go
fmt.Print(hex.Dump([]byte("hello")))
// 00000000  68 65 6c 6c 6f                                    |hello|
```

## 에러

- `ErrLength` — 16진수 문자열의 길이가 홀수일 때.
- `InvalidByteError` — `0-9a-fA-F` 범위를 벗어난 문자가 있을 때.
