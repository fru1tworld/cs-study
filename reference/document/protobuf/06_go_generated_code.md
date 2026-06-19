# Go 생성 코드 (protoc-gen-go)

> 이 문서는 Protocol Buffers 공식 문서의 "Go Generated Code Guide"를 한국어로 정리한 것입니다.
> 원본: https://protobuf.dev/reference/go/go-generated/
> 기준 런타임: `google.golang.org/protobuf` v1.36.x (`protoc-gen-go`)

---

## 목차

1. [컴파일러 설치와 호출](#컴파일러-설치와-호출)
2. [go_package 옵션](#go_package-옵션)
3. [메시지 → 구조체 매핑](#메시지--구조체-매핑)
4. [필드 이름과 getter](#필드-이름과-getter)
5. [optional / 메시지 / repeated / map 필드](#optional--메시지--repeated--map-필드)
6. [enum 생성 코드](#enum-생성-코드)
7. [oneof 생성 코드](#oneof-생성-코드)
8. [중첩 타입](#중첩-타입)
9. [Marshal / Unmarshal](#marshal--unmarshal)
10. [서비스](#서비스)

---

## 컴파일러 설치와 호출

Go 플러그인을 먼저 설치합니다.

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
```

`protoc`를 `--go_out`과 함께 호출합니다.

```bash
protoc --go_out=. --go_opt=paths=source_relative addressbook.proto
```

**출력 경로 모드(`--go_opt=paths=...`)**

- `paths=import` (기본): Go 패키지 import 경로 이름의 디렉터리에 출력 파일을 둡니다.
- `paths=source_relative`: 입력 파일과 같은 상대 디렉터리에 출력합니다(일반적으로 권장).
- `module=$PREFIX`: import 경로에서 접두사를 제거해 출력 위치를 정합니다(Go 모듈용).

**M 플래그** — 특정 proto 파일을 Go import 경로로 매핑합니다.

```bash
protoc --go_out=. --go_opt=Mprotos/foo.proto=example.com/package protos/foo.proto
```

buf 원격 플러그인을 쓸 경우 `protocolbuffers/go:v1.34.2` 같은 플러그인을 지정할 수 있습니다.

---

## go_package 옵션

`.proto` 파일에 Go import 경로를 지정합니다.

```proto
option go_package = "github.com/protocolbuffers/protobuf/examples/go/tutorialpb";
```

- 이 import 경로는 다른 proto가 이 파일에 의존할 때 어떤 패키지를 import할지와, 출력 파일 이름에 영향을 줍니다.
- 패키지 이름은 import 경로의 마지막 요소에서 자동으로 유도됩니다(위 예시는 `tutorialpb`).
- `"경로;패키지명"`처럼 세미콜론으로 패키지명을 따로 줄 수도 있으나, 자동 유도를 권장하므로 지양합니다.

---

## 메시지 → 구조체 매핑

```proto
message Artist {}
```

다음 Go 구조체가 생성됩니다.

```go
type Artist struct { /* 필드들 */ }
```

`*Artist`는 `proto.Message` 인터페이스를 구현하며, `ProtoReflect()`로 리플렉션 접근(`protoreflect.Message`)을 제공합니다.

---

## 필드 이름과 getter

생성된 Go 필드 이름은 `.proto`가 snake_case를 쓰더라도 항상 카멜케이스(CamelCase)로 변환됩니다.

- `birth_year` → `BirthYear`
- 밑줄로 시작하면 앞에 `X`를 붙임: `_birth_year_2` → `XBirthYear_2`

각 필드에는 nil-safe한 getter가 생성됩니다.

```go
func (m *Artist) GetBirthYear() int32 { /* 값 또는 기본값 반환 */ }
```

메시지 필드 getter는 수신자가 `nil`이어도 안전하게 동작하므로, 중간 nil 검사 없이 체이닝(chaining)할 수 있습니다.

---

## optional / 메시지 / repeated / map 필드

**암시적 presence 스칼라 (proto3 기본)** — 포인터가 아닌 값 필드.

```proto
int32 birth_year = 1;
```
```go
type Artist struct {
    BirthYear int32
}
func (m *Artist) GetBirthYear() int32 { /* 값 또는 0 */ }
```

**명시적 presence 스칼라 (`optional` / proto2 / editions EXPLICIT)** — 포인터 필드로 설정 여부를 구분.

```proto
optional int32 birth_year = 1;
```
```go
type Artist struct {
    BirthYear *int32
}
func (m *Artist) GetBirthYear() int32 { /* 설정값 또는 기본값 */ }
```

**메시지 필드** — 항상 포인터. `nil`이면 미설정.

```proto
message Concert { Band headliner = 1; }
```
```go
type Concert struct {
    Headliner *Band
}
func (m *Concert) GetHeadliner() *Band { /* nil-safe */ }
```

**repeated 필드** — 슬라이스. 메시지 요소는 포인터.

```proto
message Concert { repeated Band support_acts = 1; }
```
```go
type Concert struct {
    SupportActs []*Band
}
```

**map 필드** — Go map. 메시지 값은 포인터.

```proto
message MerchBooth { map<string, MerchItem> items = 1; }
```
```go
type MerchBooth struct {
    Items map[string]*MerchItem
}
```

---

## enum 생성 코드

```proto
enum Genre {
  GENRE_UNSPECIFIED = 0;
  GENRE_ROCK = 1;
}
```

```go
type Genre int32

const (
    Genre_GENRE_UNSPECIFIED Genre = 0
    Genre_GENRE_ROCK        Genre = 1
)

func (Genre) String() string { /* ... */ }
func (Genre) Enum() *Genre    { /* ... */ }

var Genre_name = map[int32]string{
    0: "GENRE_UNSPECIFIED",
    1: "GENRE_ROCK",
}
var Genre_value = map[string]int32{
    "GENRE_UNSPECIFIED": 0,
    "GENRE_ROCK":        1,
}
```

중첩 enum은 부모 이름을 접두사로 붙입니다: `Venue_Kind`.

---

## oneof 생성 코드

```proto
message Profile {
  oneof avatar {
    string image_url = 1;
    bytes image_data = 2;
  }
}
```

oneof 필드는 인터페이스 + 래퍼(wrapper) 구조체로 생성됩니다.

```go
type Profile struct {
    // Avatar에 할당 가능한 타입:
    //   *Profile_ImageUrl
    //   *Profile_ImageData
    Avatar isProfile_Avatar `protobuf_oneof:"avatar"`
}

type Profile_ImageUrl struct { ImageUrl string }
type Profile_ImageData struct { ImageData []byte }
```

어떤 멤버가 설정됐는지는 타입 스위치로 확인합니다.

```go
switch x := m.Avatar.(type) {
case *Profile_ImageUrl:
    _ = x.ImageUrl
case *Profile_ImageData:
    _ = x.ImageData
case nil:
    // 미설정
}
```

각 멤버에 대한 getter(`GetImageUrl()` 등)도 생성되며, 미설정 시 제로 값을 반환합니다.

---

## 중첩 타입

중첩 메시지는 부모 이름을 접두사로 붙여 생성됩니다.

```proto
message Artist {
  message Name {}
}
```
```go
type Artist_Name struct { /* ... */ }
```

---

## Marshal / Unmarshal

`google.golang.org/protobuf/proto` 패키지로 (역)직렬화합니다.

```go
import "google.golang.org/protobuf/proto"

// 직렬화
out, err := proto.Marshal(book)
if err != nil {
    log.Fatalln("주소록 인코딩 실패:", err)
}

// 역직렬화
book := &pb.AddressBook{}
if err := proto.Unmarshal(in, book); err != nil {
    log.Fatalln("주소록 파싱 실패:", err)
}
```

메시지 인스턴스 생성 예시:

```go
p := &pb.Person{
    Id:    1234,
    Name:  "John Doe",
    Email: "jdoe@example.com",
    Phones: []*pb.Person_PhoneNumber{
        {Number: "555-4321", Type: pb.PhoneType_PHONE_TYPE_HOME},
    },
}
```

---

## 서비스

Go 코드 생성기는 **기본적으로 서비스(rpc)에 대한 코드를 생성하지 않습니다.** gRPC 서비스 코드가 필요하면 `protoc-gen-go-grpc` 플러그인을 사용합니다.

```bash
protoc --go_out=. --go_opt=paths=source_relative \
       --go-grpc_out=. --go-grpc_opt=paths=source_relative \
       service.proto
```
