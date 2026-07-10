# 입출력 스트림 (io/bufio)

# io 패키지

> **원문:** https://pkg.go.dev/io

## 개요

`io` 패키지는 파일, 네트워크 연결, 메모리 버퍼 등 서로 다른 입출력 대상을 동일한 인터페이스로 다루기 위한 최소한의 추상화를 제공합니다. 대부분의 타입은 구현체가 아니라 `Read`, `Write` 한두 메서드만 요구하는 인터페이스이며, 표준 라이브러리 전반(파일, HTTP, 압축, 암호화 등)이 이 인터페이스에 맞춰 설계되어 있습니다.

## 기본 인터페이스

| 인터페이스 | 메서드 | 설명 |
|---|---|---|
| `Reader` | `Read(p []byte) (n int, err error)` | `p`에 최대 `len(p)`바이트를 채우고 읽은 바이트 수와 에러를 반환 |
| `Writer` | `Write(p []byte) (n int, err error)` | `p`의 바이트를 씀. `n < len(p)`이면 반드시 non-nil 에러 반환 |
| `Closer` | `Close() error` | 리소스 반환. 두 번째 호출 이후 동작은 보장되지 않음 |
| `Seeker` | `Seek(offset int64, whence int) (int64, error)` | 다음 읽기/쓰기 위치를 지정 (`SeekStart`, `SeekCurrent`, `SeekEnd`) |

`Read`는 요청한 만큼 채워지지 않아도 에러가 아닐 수 있다는 점이 Go의 관용적인 특징입니다. 스트림 끝에 도달하면 `n=0, err=io.EOF`를 반환하며, `io.EOF`는 정상 종료 신호이지 예외가 아닙니다.

```go
buf := make([]byte, 4096)
for {
    n, err := r.Read(buf)
    if n > 0 {
        process(buf[:n])
    }
    if err == io.EOF {
        break
    }
    if err != nil {
        return err
    }
}
```

## 조합 인터페이스

여러 기본 인터페이스를 임베딩해서 만든 별칭 성격의 인터페이스입니다. 함수 시그니처를 "읽기만 필요한지, 닫기까지 필요한지" 명확히 드러내는 용도로 씁니다.

- `ReadWriter` = `Reader` + `Writer`
- `ReadCloser` = `Reader` + `Closer`
- `WriteCloser` = `Writer` + `Closer`
- `ReadWriteCloser` = `Reader` + `Writer` + `Closer`
- `ReadSeeker` / `WriteSeeker` / `ReadWriteSeeker` = 각각 `Seeker` 결합
- `ReadSeekCloser` = `Reader` + `Seeker` + `Closer` (예: `*os.File`)

## 바이트/룬 단위 인터페이스

- `ByteReader.ReadByte() (byte, error)`, `ByteWriter.WriteByte(c byte) error`
- `RuneReader.ReadRune() (r rune, size int, err error)`
- `StringWriter.WriteString(s string) (n int, err error)`
- `ByteScanner` = `ByteReader` + `UnreadByte() error`, `RuneScanner` = `RuneReader` + `UnreadRune() error` — 한 바이트/룬을 미리 읽어보고 되돌릴 때 사용

## 오프셋 기반 인터페이스

- `ReaderAt.ReadAt(p []byte, off int64) (n int, err error)` — 내부 커서와 무관하게 지정한 `off`에서 읽음. `len(p)`보다 적게 읽으면 반드시 에러를 반환해야 해서 `Read`보다 엄격함. 동시에 여러 고루틴이 서로 다른 오프셋을 안전하게 읽을 수 있도록 설계됨
- `WriterAt.WriteAt(p []byte, off int64) (n int, err error)` — 위와 대칭. 겹치지 않는 범위라면 병렬 호출 가능

## 최적화 훅: ReaderFrom / WriterTo

```go
type ReaderFrom interface {
    ReadFrom(r Reader) (n int64, err error)
}
type WriterTo interface {
    WriteTo(w Writer) (n int64, err error)
}
```

`io.Copy`는 `dst`가 `ReaderFrom`을, 혹은 `src`가 `WriterTo`를 구현하면 그 메서드를 우선 호출합니다. 예를 들어 `bytes.Buffer`나 `bufio.Writer`는 `ReadFrom`을 구현해 중간 버퍼 복사를 생략하고 더 빠르게 동작합니다.

## 복사 함수

```go
func Copy(dst Writer, src Reader) (written int64, err error)
func CopyN(dst Writer, src Reader, n int64) (written int64, err error)
func CopyBuffer(dst Writer, src Reader, buf []byte) (written int64, err error)
```

- `Copy`: `src`가 EOF를 낼 때까지 `dst`로 흘려보냄. 에러가 없으면 반환 에러도 `nil`(EOF는 정상 종료로 간주해 숨김)
- `CopyN`: 정확히 `n`바이트만 복사. `written < n`이면 항상 에러가 있음
- `CopyBuffer`: 복사에 쓸 버퍼를 직접 지정(반복 호출 시 할당 재사용). `buf`가 길이 0이면 패닉

```go
f, _ := os.Open("in.txt")
defer f.Close()
var buf bytes.Buffer
n, err := io.Copy(&buf, f)
```

## 전체/부분 읽기 헬퍼

```go
func ReadAll(r Reader) ([]byte, error)
func ReadFull(r Reader, buf []byte) (n int, err error)
func ReadAtLeast(r Reader, buf []byte, min int) (n int, err error)
```

- `ReadAll`: EOF까지 다 읽어 슬라이스로 반환. 성공 시 에러는 `nil`(EOF를 감춤). `ioutil.ReadAll`의 후신
- `ReadFull`: `buf`를 정확히 채움. 도중에 EOF를 만나면 `n>0`일 때 `ErrUnexpectedEOF`, `n==0`일 때 `EOF` 반환
- `ReadAtLeast`: 최소 `min`바이트를 채울 때까지 반복 읽음. `min > len(buf)`면 `ErrShortBuffer`

세 함수 모두 소켓·파이프처럼 한 번의 `Read` 호출이 요청량보다 적게 반환될 수 있는 스트림에서 "필요한 만큼 다 채워 읽기"를 대신해 줍니다.

## 문자열 쓰기

```go
func WriteString(w Writer, s string) (n int, err error)
```
`w`가 `StringWriter`를 구현하면 `WriteString`을 그대로 호출하고, 아니면 `[]byte(s)`로 변환해 `Write`를 호출합니다. 불필요한 변환을 피하기 위한 얇은 래퍼입니다.

## 스트림 결합

```go
func MultiReader(readers ...Reader) Reader   // 순서대로 이어 붙여 하나의 Reader처럼 읽음
func MultiWriter(writers ...Writer) Writer   // 한 번의 Write를 여러 대상에 동시에 씀
func TeeReader(r Reader, w Writer) Reader    // r에서 읽는 족족 w에도 그대로 흘려보냄(로깅 등에 사용)
```

```go
r := io.MultiReader(strings.NewReader("head\n"), body)
tee := io.TeeReader(resp.Body, logFile) // 응답 본문을 읽으면서 동시에 파일에도 기록
```

## 제한 및 구간 읽기/쓰기

```go
func LimitReader(r Reader, n int64) Reader          // 내부적으로 *LimitedReader
type LimitedReader struct{ R Reader; N int64 }

func NewSectionReader(r ReaderAt, off, n int64) *SectionReader
func NewOffsetWriter(w WriterAt, off int64) *OffsetWriter
```

- `LimitReader`: `r`에서 최대 `n`바이트만 읽히도록 제한(예: 요청 바디 크기 제한)
- `SectionReader`: `ReaderAt` 위에서 `[off, off+n)` 구간만 잘라내 독립된 `Reader`/`ReaderAt`/`Seeker`처럼 사용. 파일 안의 특정 영역만 다룰 때 유용
- `OffsetWriter`: `WriterAt` 위에서 모든 쓰기 위치에 고정 오프셋을 더해줌

## 파이프

```go
func Pipe() (*PipeReader, *PipeWriter)
```
메모리상에서 동기적으로 연결된 읽기/쓰기 쌍을 만듭니다. 쓰기는 대응하는 읽기가 있을 때까지 블록되고, 반대도 마찬가지입니다. 고루틴 간에 `io.Reader`/`io.Writer` 인터페이스로 데이터를 스트리밍할 때 씁니다(예: 다른 함수가 `io.Writer`만 받는 API를 `io.Reader`로 감싸기).

```go
pr, pw := io.Pipe()
go func() {
    defer pw.Close()
    json.NewEncoder(pw).Encode(data) // Encoder는 io.Writer만 요구
}()
req, _ := http.NewRequest("POST", url, pr) // 요청 바디는 io.Reader
```

## 기타 유틸리티

- `NopCloser(r Reader) ReadCloser` — 이미 있는 `Reader`에 아무 일도 하지 않는 `Close()`를 붙여 `ReadCloser`가 필요한 API에 맞춤
- `Discard Writer` — 모든 쓰기를 그냥 버리는 `Writer`(벤치마크·무시 용도)

## 에러 값 및 상수

| 이름 | 의미 |
|---|---|
| `EOF` | 더 이상 읽을 데이터 없음(정상 종료 신호, `==`로 비교) |
| `ErrUnexpectedEOF` | 정해진 길이를 다 읽기 전에 EOF를 만남 |
| `ErrShortWrite` | `Write`가 명시적 에러 없이 요청보다 적게 씀 |
| `ErrShortBuffer` | 버퍼가 필요한 크기보다 작음 |
| `ErrClosedPipe` | 이미 닫힌 파이프에 읽기/쓰기 시도 |
| `ErrNoProgress` | `Read`가 여러 번 연속으로 데이터도 에러도 없이 반환(비정상 구현) |
| `SeekStart` / `SeekCurrent` / `SeekEnd` | `Seek`의 기준점(0/1/2) |

---

# bufio 패키지

> **원문:** https://pkg.go.dev/bufio

## 개요

`bufio`는 `io.Reader`/`io.Writer`를 내부 버퍼로 감싸 시스템 콜(또는 네트워크 왕복) 횟수를 줄여줍니다. 파일이나 소켓처럼 호출 비용이 큰 스트림을 한 바이트씩, 혹은 한 줄씩 다룰 때는 거의 항상 `bufio`로 감싸는 것이 관용적입니다.

## Reader

```go
func NewReader(rd io.Reader) *Reader
func NewReaderSize(rd io.Reader, size int) *Reader
```
`NewReader`는 기본 크기(4096바이트) 버퍼를 사용하고, `NewReaderSize`는 크기를 직접 지정합니다(이미 그만큼 큰 `*bufio.Reader`라면 그대로 반환). 내부 버퍼가 다 소비되면 그때 한 번만 `rd.Read`를 호출해 다시 채웁니다.

| 메서드 | 설명 |
|---|---|
| `Read(p []byte) (n int, err error)` | `io.Reader` 구현. 내부 버퍼가 비어 있으면 한 번만 실제 읽기 수행 |
| `ReadByte() (byte, error)` | 1바이트 읽기 |
| `ReadRune() (r rune, size int, err error)` | UTF-8 룬 하나 읽기(깨진 인코딩은 `U+FFFD`) |
| `ReadString(delim byte) (string, error)` | 구분자까지(포함) 읽어 문자열로 반환 |
| `ReadBytes(delim byte) ([]byte, error)` | 위와 동일하되 `[]byte` 반환 |
| `ReadLine() (line []byte, isPrefix bool, err error)` | 저수준 API. 개행 없이 한 줄 반환, 버퍼보다 긴 줄이면 `isPrefix=true`로 나눠서 반환 |
| `Peek(n int) ([]byte, error)` | 커서를 옮기지 않고 다음 n바이트 미리보기 |
| `Buffered() int` | 아직 소비하지 않고 버퍼에 남은 바이트 수 |
| `Discard(n int) (int, error)` | n바이트를 읽지 않고 건너뜀 |

```go
r := bufio.NewReader(os.Stdin)
line, err := r.ReadString('\n')       // 줄 단위 읽기, 구분자 포함
b, err := r.Peek(4)                    // 매직 넘버 등을 미리 확인
```

`ReadString`/`ReadBytes`는 구분자를 못 찾고 EOF를 만나면 그때까지 읽은 내용과 에러를 함께 반환합니다(에러를 무시하고 버퍼를 버리면 안 됨). 줄 단위 처리는 대부분 `Scanner`가 더 간단하므로, `ReadLine`은 정말 저수준 제어가 필요할 때만 씁니다.

## Writer

```go
func NewWriter(w io.Writer) *Writer
func NewWriterSize(w io.Writer, size int) *Writer
```

| 메서드 | 설명 |
|---|---|
| `Write(p []byte) (int, error)` | 버퍼에 씀. 버퍼가 차면 자동으로 내부 flush 후 계속 씀 |
| `WriteByte(c byte) error` / `WriteRune(r rune) (int, error)` / `WriteString(s string) (int, error)` | 단위별 쓰기 |
| `Flush() error` | 버퍼에 남은 데이터를 실제로 내려보냄 |
| `Available() int` / `Buffered() int` | 남은 여유 공간 / 아직 flush 안 된 바이트 수 |

**핵심 주의점:** `Flush()`를 호출하지 않으면 프로그램이 끝나거나 버퍼가 가득 차기 전까지 데이터가 실제 목적지에 전달되지 않습니다. 한 번 에러가 발생하면 이후 쓰기는 모두 실패로 처리됩니다.

```go
w := bufio.NewWriter(conn)
defer w.Flush() // 함수 종료 시점에 반드시 flush
fmt.Fprintln(w, "hello")
```

## Scanner

줄/단어/룬/바이트 단위로 입력을 토큰화하는 가장 간편한 API입니다.

```go
func NewScanner(r io.Reader) *Scanner
```

| 메서드 | 설명 |
|---|---|
| `Scan() bool` | 다음 토큰으로 이동. EOF나 에러면 `false` |
| `Text() string` / `Bytes() []byte` | 현재 토큰 반환(`Bytes()`가 가리키는 배열은 다음 `Scan()`에서 덮어써짐) |
| `Err() error` | 스캔 중 발생한 에러(EOF는 `nil`로 처리됨) |
| `Split(split SplitFunc)` | 토큰 분리 방식 지정(스캔 시작 전에만 호출 가능) |
| `Buffer(buf []byte, max int)` | 초기 버퍼와 최대 토큰 크기 지정 |

기본 분리 함수는 `ScanLines`이며, 그 외 `ScanWords`(공백 기준 단어), `ScanRunes`(UTF-8 룬 단위), `ScanBytes`(바이트 단위)를 제공합니다.

```go
sc := bufio.NewScanner(f)
sc.Split(bufio.ScanWords)
for sc.Scan() {
    fmt.Println(sc.Text())
}
if err := sc.Err(); err != nil {
    log.Fatal(err)
}
```

**주의: 토큰 크기 제한.** 기본 최대 토큰 크기는 `bufio.MaxScanTokenSize`(64KB)입니다. 한 줄이 이보다 길면 `Scan()`이 `false`를 반환하고 `Err()`가 `bufio.ErrTooLong`을 돌려줍니다. 매우 긴 줄을 다뤄야 한다면 `Scan()` 호출 전에 `sc.Buffer(make([]byte, 0, 64*1024), maxSize)`로 상한을 늘려야 합니다. 반대로 아주 긴 줄을 한 줄씩 다 메모리에 올리는 것 자체가 부담이면 `bufio.Reader`의 `ReadString`을 직접 쓰는 편이 낫습니다.

`SplitFunc`을 직접 만들 수도 있습니다:
```go
type SplitFunc func(data []byte, atEOF bool) (advance int, token []byte, err error)
```
`(0, nil, nil)`은 "더 읽어달라"는 뜻이고, `atEOF`가 `true`인데도 남은 데이터를 못 끊으면 마지막 토큰으로 처리해야 합니다.

## ReadWriter

```go
type ReadWriter struct {
    *Reader
    *Writer
}
func NewReadWriter(r *Reader, w *Writer) *ReadWriter
```
`bufio.Reader`와 `bufio.Writer`를 하나로 묶어 `io.ReadWriter`처럼 다루고 싶을 때 사용하는 얇은 구조체입니다.

## 주요 에러 값

- `bufio.ErrBufferFull` — `ReadSlice` 등에서 구분자를 찾기 전에 버퍼가 가득 참
- `bufio.ErrInvalidUnreadByte` / `ErrInvalidUnreadRune` — 직전에 읽기 동작이 없는데 `Unread*` 호출
- `bufio.ErrTooLong` — Scanner 토큰이 최대 크기 초과
- `bufio.ErrFinalToken` — 커스텀 `SplitFunc`이 "이게 마지막 토큰"이라고 알리는 센티널 값

---

# io/fs 패키지

> **원문:** https://pkg.go.dev/io/fs

## 개요

`io/fs`는 실제 OS 파일 시스템, ZIP 아카이브, `embed.FS`, 테스트용 인메모리 파일 시스템 등을 동일한 방식으로 다루기 위한 읽기 전용 파일 시스템 추상화입니다. `os` 패키지의 파일 시스템 접근 함수 다수가 이 인터페이스들을 그대로 구현하거나 감쌉니다.

## 핵심 인터페이스

```go
type FS interface {
    Open(name string) (File, error)
}

type File interface {
    Stat() (FileInfo, error)
    Read([]byte) (int, error)
    Close() error
}
```
`FS`는 딱 하나의 메서드 `Open`만 요구하는 최소 인터페이스입니다. 디렉터리를 나타내는 `File`은 보통 `ReadDirFile`(`ReadDir(n int) ([]DirEntry, error)`)도 함께 구현합니다.

```go
type DirEntry interface {
    Name() string
    IsDir() bool
    Type() FileMode
    Info() (FileInfo, error)
}
```
`os.ReadDir` 등이 반환하는 디렉터리 항목. `FileInfo`를 매번 만들지 않아도 되는 경량 정보이며, 필요할 때 `Info()`로 전체 `FileInfo`를 가져옵니다.

`FileInfo`, `FileMode`는 `os` 패키지에서 쓰는 것과 동일한 개념으로, `ModeDir`/`ModeSymlink` 등 타입 비트와 Unix 권한 비트(`Perm()`)를 함께 담습니다.

## 선택적 확장 인터페이스

`FS`를 구현하는 타입이 성능을 위해 추가로 구현할 수 있는 인터페이스들입니다. 대응하는 헬퍼 함수는 이 인터페이스가 있으면 우선 사용하고, 없으면 `Open` + 기본 메서드 조합으로 대체 동작합니다.

| 인터페이스 | 추가 메서드 | 대응 함수 |
|---|---|---|
| `ReadDirFS` | `ReadDir(name string) ([]DirEntry, error)` | `fs.ReadDir` |
| `ReadFileFS` | `ReadFile(name string) ([]byte, error)` | `fs.ReadFile` |
| `StatFS` | `Stat(name string) (FileInfo, error)` | `fs.Stat` |
| `GlobFS` | `Glob(pattern string) ([]string, error)` | `fs.Glob` |
| `SubFS` | `Sub(dir string) (FS, error)` | `fs.Sub` |

## 패키지 함수

```go
func ReadDir(fsys FS, name string) ([]DirEntry, error)
func ReadFile(fsys FS, name string) ([]byte, error)
func Stat(fsys FS, name string) (FileInfo, error)
func Glob(fsys FS, pattern string) ([]string, error)
func Sub(fsys FS, dir string) (FS, error)
```
각각 이름 그대로 디렉터리 목록/파일 전체 읽기/파일 정보/패턴 매칭/부분 트리 추출을 수행합니다. `Sub(fsys, dir)`는 `dir` 하위만 노출하는 새 `FS`를 만들어, 예를 들어 `embed.FS`에서 특정 폴더만 웹 서버에 노출하고 싶을 때 유용합니다.

```go
//go:embed static
var content embed.FS

sub, _ := fs.Sub(content, "static")
http.Handle("/", http.FileServer(http.FS(sub)))
```

## 트리 순회: WalkDir

```go
func WalkDir(fsys FS, root string, fn WalkDirFunc) error

type WalkDirFunc func(path string, d DirEntry, err error) error
```
`root`부터 사전순으로 파일 트리를 순회하며 각 항목마다 `fn`을 호출합니다. `filepath.Walk`의 `fs.FS` 버전으로, `DirEntry`를 넘겨받기 때문에 매 항목마다 `Stat`를 호출하지 않아도 되어 더 효율적입니다.

`fn`의 반환값으로 순회를 제어합니다:
- `nil` — 계속 진행
- `fs.SkipDir` — 현재 디렉터리(파일이면 그 부모)를 건너뜀
- `fs.SkipAll` — 남은 모든 항목을 건너뛰고 순회 종료
- 그 외 에러 — 즉시 순회 중단, 해당 에러 반환

```go
fs.WalkDir(os.DirFS("."), ".", func(path string, d fs.DirEntry, err error) error {
    if err != nil {
        return err
    }
    if d.IsDir() && d.Name() == "vendor" {
        return fs.SkipDir
    }
    fmt.Println(path)
    return nil
})
```

## 경로 검증 및 에러

```go
func ValidPath(name string) bool
```
`FS` 구현체가 `Open`에서 받은 이름이 유효한지 검사할 때 씁니다. 경로는 슬래시로 구분되고, 절대 경로가 아니며, `.`이나 `..` 세그먼트나 빈 세그먼트를 포함하지 않아야 합니다(루트 자신은 `"."`).

공통 에러 값 `ErrInvalid`, `ErrPermission`, `ErrExist`, `ErrNotExist`, `ErrClosed`는 `errors.Is`로 검사하도록 설계되어 있고, `os` 패키지의 동일 이름 에러와 호환됩니다.

```go
type PathError struct {
    Op   string
    Path string
    Err  error
}
```
어떤 연산(`Op`)이 어떤 경로(`Path`)에서 실패했는지를 감싸는 에러 타입으로, `Unwrap()`을 통해 원본 에러까지 `errors.Is`/`errors.As`로 검사할 수 있습니다.
