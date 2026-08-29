# Go 입출력 스트림과 파일시스템

## 입출력 스트림 (io/bufio)

## io 패키지

> 원문: https://pkg.go.dev/io

### 개요

`io` 패키지 — 파일·네트워크 연결·메모리 버퍼 등 서로 다른 입출력 대상을 동일한 인터페이스로 다루기 위한 최소 추상화 제공.
- 대부분의 타입은 구현체가 아니라 `Read`, `Write` 한두 메서드만 요구하는 인터페이스
- 표준 라이브러리 전반(파일·HTTP·압축·암호화 등)이 이 인터페이스에 맞춰 설계됨

### 기본 인터페이스

- `Reader`
  - 메서드: `Read(p []byte) (n int, err error)`
  - `p`에 최대 `len(p)`바이트를 채우고 읽은 바이트 수와 에러 반환
- `Writer`
  - 메서드: `Write(p []byte) (n int, err error)`
  - `p`의 바이트를 씀 → `n < len(p)`면 반드시 non-nil 에러 반환
- `Closer`
  - 메서드: `Close() error`
  - 리소스 반환. 두 번째 호출 이후 동작은 보장되지 않음
- `Seeker`
  - 메서드: `Seek(offset int64, whence int) (int64, error)`
  - 다음 읽기/쓰기 위치 지정(`SeekStart`, `SeekCurrent`, `SeekEnd`)

`Read`는 요청한 만큼 채워지지 않아도 에러가 아닐 수 있음 → Go의 관용적 특징.
- 스트림 끝 도달 시 `n=0, err=io.EOF` 반환
- `io.EOF`는 정상 종료 신호, 예외 아님

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

### 조합 인터페이스

여러 기본 인터페이스를 임베딩해서 만든 별칭 성격의 인터페이스.
- 함수 시그니처를 "읽기만 필요한지, 닫기까지 필요한지" 명확히 드러내는 용도

- `ReadWriter` = `Reader` + `Writer`
- `ReadCloser` = `Reader` + `Closer`
- `WriteCloser` = `Writer` + `Closer`
- `ReadWriteCloser` = `Reader` + `Writer` + `Closer`
- `ReadSeeker` / `WriteSeeker` / `ReadWriteSeeker` = 각각 `Seeker` 결합
- `ReadSeekCloser` = `Reader` + `Seeker` + `Closer` (예: `*os.File`)

### 바이트/룬 단위 인터페이스

- `ByteReader.ReadByte() (byte, error)` · `ByteWriter.WriteByte(c byte) error`
- `RuneReader.ReadRune() (r rune, size int, err error)`
- `StringWriter.WriteString(s string) (n int, err error)`
- `ByteScanner` = `ByteReader` + `UnreadByte() error`, `RuneScanner` = `RuneReader` + `UnreadRune() error` — 한 바이트/룬을 미리 읽어보고 되돌릴 때 사용

### 오프셋 기반 인터페이스

- `ReaderAt.ReadAt(p []byte, off int64) (n int, err error)`
  - 내부 커서와 무관하게 지정한 `off`에서 읽음
  - `len(p)`보다 적게 읽으면 반드시 에러 반환 필요 → `Read`보다 엄격
  - 여러 고루틴이 서로 다른 오프셋을 동시에 안전하게 읽도록 설계됨
- `WriterAt.WriteAt(p []byte, off int64) (n int, err error)`
  - 위와 대칭. 겹치지 않는 범위면 병렬 호출 가능

### 최적화 훅: ReaderFrom / WriterTo

```go
type ReaderFrom interface {
    ReadFrom(r Reader) (n int64, err error)
}
type WriterTo interface {
    WriteTo(w Writer) (n int64, err error)
}
```

`io.Copy`는 `dst`가 `ReaderFrom`을, 혹은 `src`가 `WriterTo`를 구현하면 그 메서드를 우선 호출.
- 예: `bytes.Buffer`나 `bufio.Writer`는 `ReadFrom` 구현 → 중간 버퍼 복사 생략, 더 빠르게 동작

### 복사 함수

```go
func Copy(dst Writer, src Reader) (written int64, err error)
func CopyN(dst Writer, src Reader, n int64) (written int64, err error)
func CopyBuffer(dst Writer, src Reader, buf []byte) (written int64, err error)
```

- `Copy`: `src`가 EOF를 낼 때까지 `dst`로 흘려보냄. 에러가 없으면 반환 에러도 `nil`(EOF는 정상 종료로 간주해 숨김)
- `CopyN`: 정확히 `n`바이트만 복사. `written < n`이면 항상 에러 존재
- `CopyBuffer`: 복사에 쓸 버퍼를 직접 지정(반복 호출 시 할당 재사용). `buf`가 길이 0이면 패닉

```go
f, _ := os.Open("in.txt")
defer f.Close()
var buf bytes.Buffer
n, err := io.Copy(&buf, f)
```

### 전체/부분 읽기 헬퍼

```go
func ReadAll(r Reader) ([]byte, error)
func ReadFull(r Reader, buf []byte) (n int, err error)
func ReadAtLeast(r Reader, buf []byte, min int) (n int, err error)
```

- `ReadAll`: EOF까지 다 읽어 슬라이스로 반환. 성공 시 에러는 `nil`(EOF를 감춤). `ioutil.ReadAll`의 후신
- `ReadFull`: `buf`를 정확히 채움. 도중에 EOF를 만나면 `n>0`일 때 `ErrUnexpectedEOF`, `n==0`일 때 `EOF` 반환
- `ReadAtLeast`: 최소 `min`바이트를 채울 때까지 반복 읽음. `min > len(buf)`면 `ErrShortBuffer`

세 함수 모두 소켓·파이프처럼 한 번의 `Read` 호출이 요청량보다 적게 반환될 수 있는 스트림에서 "필요한 만큼 다 채워 읽기"를 대신 수행.

### 문자열 쓰기

```go
func WriteString(w Writer, s string) (n int, err error)
```

`w`가 `StringWriter`를 구현하면 `WriteString`을 그대로 호출, 아니면 `[]byte(s)`로 변환해 `Write` 호출 → 불필요한 변환을 피하기 위한 얇은 래퍼.

### 스트림 결합

```go
func MultiReader(readers ...Reader) Reader   // 순서대로 이어 붙여 하나의 Reader처럼 읽음
func MultiWriter(writers ...Writer) Writer   // 한 번의 Write를 여러 대상에 동시에 씀
func TeeReader(r Reader, w Writer) Reader    // r에서 읽는 족족 w에도 그대로 흘려보냄(로깅 등에 사용)
```

```go
r := io.MultiReader(strings.NewReader("head\n"), body)
tee := io.TeeReader(resp.Body, logFile) // 응답 본문을 읽으면서 동시에 파일에도 기록
```

### 제한 및 구간 읽기/쓰기

```go
func LimitReader(r Reader, n int64) Reader          // 내부적으로 *LimitedReader
type LimitedReader struct{ R Reader; N int64 }

func NewSectionReader(r ReaderAt, off, n int64) *SectionReader
func NewOffsetWriter(w WriterAt, off int64) *OffsetWriter
```

- `LimitReader`: `r`에서 최대 `n`바이트만 읽히도록 제한(예: 요청 바디 크기 제한)
- `SectionReader`: `ReaderAt` 위에서 `[off, off+n)` 구간만 잘라내 독립된 `Reader`/`ReaderAt`/`Seeker`처럼 사용 → 파일 안의 특정 영역만 다룰 때 유용
- `OffsetWriter`: `WriterAt` 위에서 모든 쓰기 위치에 고정 오프셋을 더함

### 파이프

```go
func Pipe() (*PipeReader, *PipeWriter)
```

메모리상에서 동기적으로 연결된 읽기/쓰기 쌍 생성.
- 쓰기는 대응하는 읽기가 있을 때까지 블록, 반대도 마찬가지
- 고루틴 간에 `io.Reader`/`io.Writer` 인터페이스로 데이터를 스트리밍할 때 사용(예: 다른 함수가 `io.Writer`만 받는 API를 `io.Reader`로 감싸기)

```go
pr, pw := io.Pipe()
go func() {
    defer pw.Close()
    json.NewEncoder(pw).Encode(data) // Encoder는 io.Writer만 요구
}()
req, _ := http.NewRequest("POST", url, pr) // 요청 바디는 io.Reader
```

### 기타 유틸리티

- `NopCloser(r Reader) ReadCloser` — 이미 있는 `Reader`에 아무 일도 하지 않는 `Close()`를 붙여 `ReadCloser`가 필요한 API에 맞춤
- `Discard Writer` — 모든 쓰기를 그냥 버리는 `Writer`(벤치마크·무시 용도)

### 에러 값 및 상수

- `EOF` — 더 이상 읽을 데이터 없음(정상 종료 신호, `==`로 비교)
- `ErrUnexpectedEOF` — 정해진 길이를 다 읽기 전에 EOF를 만남
- `ErrShortWrite` — `Write`가 명시적 에러 없이 요청보다 적게 씀
- `ErrShortBuffer` — 버퍼가 필요한 크기보다 작음
- `ErrClosedPipe` — 이미 닫힌 파이프에 읽기/쓰기 시도
- `ErrNoProgress` — `Read`가 여러 번 연속으로 데이터도 에러도 없이 반환(비정상 구현)
- `SeekStart` / `SeekCurrent` / `SeekEnd` — `Seek`의 기준점(0/1/2)

---

## bufio 패키지

> 원문: https://pkg.go.dev/bufio

### 개요

`bufio` — `io.Reader`/`io.Writer`를 내부 버퍼로 감싸 시스템 콜(또는 네트워크 왕복) 횟수를 줄여줌.
- 파일·소켓처럼 호출 비용이 큰 스트림을 한 바이트씩, 혹은 한 줄씩 다룰 때는 거의 항상 `bufio`로 감싸는 것이 관용적

### Reader

```go
func NewReader(rd io.Reader) *Reader
func NewReaderSize(rd io.Reader, size int) *Reader
```

`NewReader`는 기본 크기(4096바이트) 버퍼 사용, `NewReaderSize`는 크기를 직접 지정(이미 그만큼 큰 `*bufio.Reader`라면 그대로 반환).
- 내부 버퍼가 다 소비되면 그때 한 번만 `rd.Read`를 호출해 다시 채움

주요 메서드
- `Read(p []byte) (n int, err error)` — `io.Reader` 구현. 내부 버퍼가 비어 있으면 한 번만 실제 읽기 수행
- `ReadByte() (byte, error)` — 1바이트 읽기
- `ReadRune() (r rune, size int, err error)` — UTF-8 룬 하나 읽기(깨진 인코딩은 `U+FFFD`)
- `ReadString(delim byte) (string, error)` — 구분자까지(포함) 읽어 문자열로 반환
- `ReadBytes(delim byte) ([]byte, error)` — 위와 동일하되 `[]byte` 반환
- `ReadLine() (line []byte, isPrefix bool, err error)` — 저수준 API. 개행 없이 한 줄 반환, 버퍼보다 긴 줄이면 `isPrefix=true`로 나눠서 반환
- `Peek(n int) ([]byte, error)` — 커서를 옮기지 않고 다음 n바이트 미리보기
- `Buffered() int` — 아직 소비하지 않고 버퍼에 남은 바이트 수
- `Discard(n int) (int, error)` — n바이트를 읽지 않고 건너뜀

```go
r := bufio.NewReader(os.Stdin)
line, err := r.ReadString('\n')       // 줄 단위 읽기, 구분자 포함
b, err := r.Peek(4)                    // 매직 넘버 등을 미리 확인
```

`ReadString`/`ReadBytes`는 구분자를 못 찾고 EOF를 만나면 그때까지 읽은 내용과 에러를 함께 반환(에러를 무시하고 버퍼를 버리면 안 됨).
- 줄 단위 처리는 대부분 `Scanner`가 더 간단 → `ReadLine`은 정말 저수준 제어가 필요할 때만 사용

### Writer

```go
func NewWriter(w io.Writer) *Writer
func NewWriterSize(w io.Writer, size int) *Writer
```

주요 메서드
- `Write(p []byte) (int, error)` — 버퍼에 씀. 버퍼가 차면 자동으로 내부 flush 후 계속 씀
- `WriteByte(c byte) error` / `WriteRune(r rune) (int, error)` / `WriteString(s string) (int, error)` — 단위별 쓰기
- `Flush() error` — 버퍼에 남은 데이터를 실제로 내려보냄
- `Available() int` / `Buffered() int` — 남은 여유 공간 / 아직 flush 안 된 바이트 수

주의점
- `Flush()`를 호출하지 않으면 프로그램이 끝나거나 버퍼가 가득 차기 전까지 데이터가 실제 목적지에 전달되지 않음
- 한 번 에러가 발생하면 이후 쓰기는 모두 실패로 처리됨

```go
w := bufio.NewWriter(conn)
defer w.Flush() // 함수 종료 시점에 반드시 flush
fmt.Fprintln(w, "hello")
```

### Scanner

줄/단어/룬/바이트 단위로 입력을 토큰화하는 가장 간편한 API.

```go
func NewScanner(r io.Reader) *Scanner
```

주요 메서드
- `Scan() bool` — 다음 토큰으로 이동. EOF나 에러면 `false`
- `Text() string` / `Bytes() []byte` — 현재 토큰 반환(`Bytes()`가 가리키는 배열은 다음 `Scan()`에서 덮어써짐)
- `Err() error` — 스캔 중 발생한 에러(EOF는 `nil`로 처리됨)
- `Split(split SplitFunc)` — 토큰 분리 방식 지정(스캔 시작 전에만 호출 가능)
- `Buffer(buf []byte, max int)` — 초기 버퍼와 최대 토큰 크기 지정

기본 분리 함수는 `ScanLines` → 그 외 `ScanWords`(공백 기준 단어) · `ScanRunes`(UTF-8 룬 단위) · `ScanBytes`(바이트 단위) 제공.

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

토큰 크기 제한 주의
- 기본 최대 토큰 크기는 `bufio.MaxScanTokenSize`(64KB)
- 한 줄이 이보다 길면 `Scan()`이 `false` 반환, `Err()`가 `bufio.ErrTooLong` 반환
- 매우 긴 줄을 다뤄야 한다면 `Scan()` 호출 전에 `sc.Buffer(make([]byte, 0, 64*1024), maxSize)`로 상한 확대 필요
- 반대로 아주 긴 줄을 한 줄씩 다 메모리에 올리는 것 자체가 부담이면 `bufio.Reader`의 `ReadString`을 직접 쓰는 편이 나음

`SplitFunc`을 직접 만들 수도 있음.
```go
type SplitFunc func(data []byte, atEOF bool) (advance int, token []byte, err error)
```
- `(0, nil, nil)`은 "더 읽어달라"는 뜻
- `atEOF`가 `true`인데도 남은 데이터를 못 끊으면 마지막 토큰으로 처리 필요

### ReadWriter

```go
type ReadWriter struct {
    *Reader
    *Writer
}
func NewReadWriter(r *Reader, w *Writer) *ReadWriter
```

`bufio.Reader`와 `bufio.Writer`를 하나로 묶어 `io.ReadWriter`처럼 다루고 싶을 때 사용하는 얇은 구조체.

### 주요 에러 값

- `bufio.ErrBufferFull` — `ReadSlice` 등에서 구분자를 찾기 전에 버퍼가 가득 참
- `bufio.ErrInvalidUnreadByte` / `ErrInvalidUnreadRune` — 직전에 읽기 동작이 없는데 `Unread*` 호출
- `bufio.ErrTooLong` — Scanner 토큰이 최대 크기 초과
- `bufio.ErrFinalToken` — 커스텀 `SplitFunc`이 "이게 마지막 토큰"이라고 알리는 센티널 값

---

## io/fs 패키지

> 원문: https://pkg.go.dev/io/fs

### 개요

`io/fs` — 실제 OS 파일 시스템, ZIP 아카이브, `embed.FS`, 테스트용 인메모리 파일 시스템 등을 동일한 방식으로 다루기 위한 읽기 전용 파일 시스템 추상화.
- `os` 패키지의 파일 시스템 접근 함수 다수가 이 인터페이스들을 그대로 구현하거나 감쌈

### 핵심 인터페이스

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

`FS`는 딱 하나의 메서드 `Open`만 요구하는 최소 인터페이스.
- 디렉터리를 나타내는 `File`은 보통 `ReadDirFile`(`ReadDir(n int) ([]DirEntry, error)`)도 함께 구현

```go
type DirEntry interface {
    Name() string
    IsDir() bool
    Type() FileMode
    Info() (FileInfo, error)
}
```

`os.ReadDir` 등이 반환하는 디렉터리 항목.
- `FileInfo`를 매번 만들지 않아도 되는 경량 정보
- 필요할 때 `Info()`로 전체 `FileInfo` 획득

`FileInfo`, `FileMode`는 `os` 패키지에서 쓰는 것과 동일한 개념 → `ModeDir`/`ModeSymlink` 등 타입 비트와 Unix 권한 비트(`Perm()`)를 함께 담음.

### 선택적 확장 인터페이스

`FS`를 구현하는 타입이 성능을 위해 추가로 구현할 수 있는 인터페이스들.
- 대응하는 헬퍼 함수는 이 인터페이스가 있으면 우선 사용, 없으면 `Open` + 기본 메서드 조합으로 대체 동작

- `ReadDirFS`
  - 추가 메서드: `ReadDir(name string) ([]DirEntry, error)`
  - 대응 함수: `fs.ReadDir`
- `ReadFileFS`
  - 추가 메서드: `ReadFile(name string) ([]byte, error)`
  - 대응 함수: `fs.ReadFile`
- `StatFS`
  - 추가 메서드: `Stat(name string) (FileInfo, error)`
  - 대응 함수: `fs.Stat`
- `GlobFS`
  - 추가 메서드: `Glob(pattern string) ([]string, error)`
  - 대응 함수: `fs.Glob`
- `SubFS`
  - 추가 메서드: `Sub(dir string) (FS, error)`
  - 대응 함수: `fs.Sub`

### 패키지 함수

```go
func ReadDir(fsys FS, name string) ([]DirEntry, error)
func ReadFile(fsys FS, name string) ([]byte, error)
func Stat(fsys FS, name string) (FileInfo, error)
func Glob(fsys FS, pattern string) ([]string, error)
func Sub(fsys FS, dir string) (FS, error)
```

각각 이름 그대로 디렉터리 목록/파일 전체 읽기/파일 정보/패턴 매칭/부분 트리 추출 수행.
- `Sub(fsys, dir)`는 `dir` 하위만 노출하는 새 `FS` 생성 → 예를 들어 `embed.FS`에서 특정 폴더만 웹 서버에 노출하고 싶을 때 유용

```go
//go:embed static
var content embed.FS

sub, _ := fs.Sub(content, "static")
http.Handle("/", http.FileServer(http.FS(sub)))
```

### 트리 순회: WalkDir

```go
func WalkDir(fsys FS, root string, fn WalkDirFunc) error

type WalkDirFunc func(path string, d DirEntry, err error) error
```

`root`부터 사전순으로 파일 트리를 순회하며 각 항목마다 `fn` 호출.
- `filepath.Walk`의 `fs.FS` 버전
- `DirEntry`를 넘겨받기 때문에 매 항목마다 `Stat`를 호출하지 않아도 되어 더 효율적

`fn`의 반환값으로 순회 제어
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

### 경로 검증 및 에러

```go
func ValidPath(name string) bool
```

`FS` 구현체가 `Open`에서 받은 이름이 유효한지 검사할 때 사용.
- 경로는 슬래시로 구분, 절대 경로가 아니어야 함
- `.`이나 `..` 세그먼트나 빈 세그먼트를 포함하지 않아야 함(루트 자신은 `"."`)

공통 에러 값 `ErrInvalid`, `ErrPermission`, `ErrExist`, `ErrNotExist`, `ErrClosed`는 `errors.Is`로 검사하도록 설계됨 → `os` 패키지의 동일 이름 에러와 호환.

```go
type PathError struct {
    Op   string
    Path string
    Err  error
}
```

어떤 연산(`Op`)이 어떤 경로(`Path`)에서 실패했는지를 감싸는 에러 타입 → `Unwrap()`을 통해 원본 에러까지 `errors.Is`/`errors.As`로 검사 가능.

---

## 파일시스템과 운영체제 연동 (os / path / path/filepath)

## os 패키지

> 원문: https://pkg.go.dev/os

### 개요

`os` — 파일 읽기/쓰기, 프로세스 정보 조회, 환경 변수 접근, 종료 코드 지정 등 운영체제와 상호작용하는 기능 제공.
- 인터페이스는 유닉스 계열을 기준으로 설계됨
- 오류 처리 방식은 플랫폼 독립적으로 설계됨 → 실패 원인을 `error` 값으로 통일해서 다룰 수 있음

### 파일 열기와 생성

파일을 다루는 세 가지 진입점.

- `os.Open(name string) (*os.File, error)` — 읽기 전용(`O_RDONLY`)으로 엶
- `os.Create(name string) (*os.File, error)` — 쓰기용으로 새로 만들거나, 이미 있으면 비움(truncate). 권한은 `0666`(umask 적용 전)
- `os.OpenFile(name string, flag int, perm os.FileMode) (*os.File, error)` — 플래그와 권한을 직접 지정하는 범용 버전

`flag`는 아래 상수를 비트 OR로 조합.

- `O_RDONLY` / `O_WRONLY` / `O_RDWR` — 읽기/쓰기/읽기+쓰기
- `O_APPEND` — 쓸 때 파일 끝에 이어 붙임
- `O_CREATE` — 없으면 생성
- `O_EXCL` — `O_CREATE`와 함께 쓰면 이미 존재할 때 실패(원자적 생성)
- `O_TRUNC` — 열 때 내용을 비움

```go
f, err := os.OpenFile("app.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
if err != nil {
    log.Fatal(err)
}
defer f.Close()
```

`Open`/`Create`가 반환하는 `*os.File`은 다 쓰고 나면 반드시 `Close()` 호출 필요 → 관용적으로 여는 직후 `defer f.Close()`를 붙임.

### File 메서드로 읽고 쓰기

`*os.File`은 `io.Reader`, `io.Writer`, `io.Seeker`, `io.Closer`를 모두 만족.

- `Read(b []byte) (n int, err error)` / `ReadAt(b []byte, off int64) (n int, err error)`
- `Write(b []byte) (n int, err error)` / `WriteAt(...)` / `WriteString(s string) (n int, err error)`
- `Seek(offset int64, whence int) (int64, error)` — `whence`는 `io.SeekStart`, `io.SeekCurrent`, `io.SeekEnd`
- `Stat() (os.FileInfo, error)`, `Truncate(size int64) error`, `Sync() error`(디스크로 강제 플러시)

작은 파일이라면 스트림을 직접 다루기보다 파일 전체를 한 번에 읽고 쓰는 헬퍼 함수 사용이 간결(Go 1.16부터 추가).

```go
data, err := os.ReadFile("config.json")
if err != nil {
    log.Fatal(err)
}

err = os.WriteFile("config.json", data, 0644)
```

### 디렉터리 다루기

- `os.Mkdir(name string, perm os.FileMode) error` — 디렉터리 하나만 생성(상위 경로가 없으면 실패)
- `os.MkdirAll(path string, perm os.FileMode) error` — 필요한 상위 경로까지 모두 생성
- `os.ReadDir(name string) ([]os.DirEntry, error)` — 디렉터리 항목을 이름순으로 반환(Go 1.16+). 예전의 `ioutil.ReadDir`과 달리 `Stat`을 미리 호출하지 않아 더 빠름

```go
entries, err := os.ReadDir(".")
if err != nil {
    log.Fatal(err)
}
for _, e := range entries {
    kind := "file"
    if e.IsDir() {
        kind = "dir"
    }
    fmt.Println(kind, e.Name())
}
```

### 파일 정보와 권한

- `os.Stat(name string) (os.FileInfo, error)` — 심볼릭 링크를 따라가서 정보 조회
- `os.Lstat(name string) (os.FileInfo, error)` — 링크 자체의 정보 조회(따라가지 않음)
- `os.FileInfo` 인터페이스 — `Name()`, `Size()`, `Mode() os.FileMode`, `ModTime()`, `IsDir()`, `Sys()`
- `os.FileMode` — 유닉스 권한 비트(`Perm()`)와 파일 종류 플래그(`ModeDir`, `ModeSymlink` 등)를 함께 담는 타입

```go
info, err := os.Stat("main.go")
if err == nil {
    fmt.Printf("%s: %d바이트, 권한 %#o\n", info.Name(), info.Size(), info.Mode().Perm())
}
```

권한 변경/소유자 변경은 `os.Chmod(name string, mode os.FileMode) error`, `os.Chown(name string, uid, gid int) error`(유닉스 전용)로 수행.

### 삭제, 이동, 링크

- `os.Remove(name string) error` — 파일 또는 빈 디렉터리 삭제
- `os.RemoveAll(path string) error` — 경로와 하위 내용을 재귀적으로 삭제(없어도 오류 없음)
- `os.Rename(oldpath, newpath string) error` — 이동 또는 이름 변경
- `os.Symlink` / `os.Link` / `os.Readlink` — 심볼릭 링크·하드 링크 생성 및 링크 대상 조회

### 임시 파일과 디렉터리

테스트나 캐시 목적으로 임시 자원을 만들 때 사용(Go 1.16+).

```go
tmp, err := os.MkdirTemp("", "myapp-*")
if err != nil {
    log.Fatal(err)
}
defer os.RemoveAll(tmp)

f, err := os.CreateTemp(tmp, "data-*.tmp")
```

- `os.TempDir() string` — 시스템 기본 임시 디렉터리 경로(리눅스 `/tmp`, 윈도우는 `%TMP%` 등) 반환
- `pattern`에 `*`가 있으면 그 자리를 임의의 문자열로 치환 → 이름 충돌 방지

### 환경 변수

- `os.Getenv(key string) string` — 값이 없으면 빈 문자열(값이 실제로 빈 문자열인지 구분 불가)
- `os.LookupEnv(key string) (string, bool)` — 존재 여부를 `bool`로 구분해서 알려줌
- `os.Setenv(key, value string) error`, `os.Unsetenv(key string) error`
- `os.Environ() []string` — `"KEY=VALUE"` 형태의 전체 목록
- `os.ExpandEnv(s string) string` — 문자열 안의 `$VAR`, `${VAR}`를 환경 변수 값으로 치환

```go
port, ok := os.LookupEnv("PORT")
if !ok {
    port = "8080"
}
```

### 프로세스와 작업 디렉터리

- `os.Args []string` — 커맨드라인 인자(0번째는 실행 파일 경로)
- `os.Getwd() (string, error)` / `os.Chdir(dir string) error` — 현재 작업 디렉터리 조회/변경
- `os.Hostname() (string, error)`
- `os.Getpid()`, `os.Getppid()` — 자신/부모 프로세스 ID
- `os.Exit(code int)` — 즉시 종료. `defer`가 실행되지 않음 → 정리 작업이 필요하면 `Exit`를 직접 호출하지 말고 `main`이 정상적으로 반환하도록 구성하는 편이 안전
- `os.Stdin`, `os.Stdout`, `os.Stderr` — 표준 입출력을 가리키는 `*os.File` 값

### 사용자 디렉터리

- `os.UserHomeDir() (string, error)` (Go 1.12+)
- `os.UserCacheDir() (string, error)` (Go 1.11+) — `$XDG_CACHE_HOME`, macOS의 `~/Library/Caches` 등 플랫폼 관례를 따름
- `os.UserConfigDir() (string, error)` (Go 1.13+)

### 오류 처리

파일 시스템 호출 오류는 대개 `*fs.PathError`(`os.PathError`의 별칭)로 감싸져 옴 → 실패한 연산(`Op`), 대상 경로(`Path`), 근본 원인(`Err`)을 담음.
- 원인 종류는 문자열 비교 대신 판별 함수로 확인

```go
if _, err := os.Open("missing.txt"); err != nil {
    if os.IsNotExist(err) {
        fmt.Println("파일이 없음")
    }
}
```

- `os.IsNotExist(err) bool`, `os.IsExist(err) bool`, `os.IsPermission(err) bool`
- 새 코드에서는 `errors.Is(err, fs.ErrNotExist)`처럼 `fs` 패키지의 sentinel 오류(`ErrNotExist`, `ErrExist`, `ErrPermission`, `ErrClosed`)와 `errors.Is`를 함께 쓰는 방식이 더 권장됨

---

## path/filepath 패키지

> 원문: https://pkg.go.dev/path/filepath

### 개요

`filepath` — 실제 파일 시스템 경로, 즉 운영체제에 맞는 구분자(유닉스 `/`, 윈도우 `\`)를 쓰는 경로를 다룸.
- 문자열을 직접 자르고 붙이는 대신 이 패키지의 함수를 쓰면 플랫폼 차이를 신경 쓰지 않아도 됨

### 경로 조립·분해

- `filepath.Join(elem ...string) string` — 구분자로 이어 붙이고 결과를 `Clean`까지 적용. 빈 인자는 무시됨
- `filepath.Split(path string) (dir, file string)` — 마지막 구분자를 기준으로 나눔. 항상 `dir + file == path` 성립
- `filepath.Dir(path string) string` — 디렉터리 부분만
- `filepath.Base(path string) string` — 마지막 요소(파일/폴더 이름)만
- `filepath.Ext(path string) string` — 마지막 `.` 이후의 확장자(`.` 포함)

```go
p := filepath.Join("data", "logs", "2024", "app.log")
// data/logs/2024/app.log (유닉스) 또는 data\logs\2024\app.log (윈도우)

dir, file := filepath.Split(p)
fmt.Println(dir, file, filepath.Ext(file)) // data/logs/2024/ app.log .log
```

### 경로 정규화와 절대/상대 변환

- `filepath.Clean(path string) string` — 중복 구분자 제거, `.` 제거, 가능한 `..` 축약을 통해 가장 짧은 동치 경로를 만듦
- `filepath.Abs(path string) (string, error)` — 상대 경로면 현재 작업 디렉터리를 붙여 절대 경로로 변환
- `filepath.Rel(basepath, targpath string) (string, error)` — `base` 기준으로 `target`까지 가는 상대 경로 계산. `Join(base, Rel(base, target)) == target`이 성립하도록 설계됨
- `filepath.EvalSymlinks(path string) (string, error)` — 경로에 포함된 심볼릭 링크를 모두 실제 경로로 해석
- `filepath.IsAbs(path string) bool` — 절대 경로 여부

```go
rel, _ := filepath.Rel("/home/user/project", "/home/user/project/src/main.go")
fmt.Println(rel) // src/main.go
```

### 패턴 매칭과 순회

- `filepath.Match(pattern, name string) (bool, error)` — 셸 스타일 글롭(`*`, `?`, `[...]`)으로 한 이름을 매칭. `*`, `?`는 구분자를 넘어가지 않음
- `filepath.Glob(pattern string) ([]string, error)` — 패턴에 매칭되는 실제 경로 목록을 파일 시스템에서 찾아 반환
- `filepath.WalkDir(root string, fn fs.WalkDirFunc) error` (Go 1.16+) — 디렉터리 트리를 순회하며 각 항목마다 콜백 호출. 예전의 `filepath.Walk`보다 효율적(불필요한 `Stat` 호출을 피함) → 신규 코드에서는 이쪽을 우선 사용

```go
err := filepath.WalkDir(".", func(path string, d fs.DirEntry, err error) error {
    if err != nil {
        return err
    }
    if d.IsDir() && d.Name() == "vendor" {
        return filepath.SkipDir // vendor 디렉터리는 건너뜀
    }
    if !d.IsDir() && filepath.Ext(path) == ".go" {
        fmt.Println(path)
    }
    return nil
})
```

콜백에서 `filepath.SkipDir`을 반환하면 해당 디렉터리를 건너뜀, `filepath.SkipAll`을 반환하면 순회 전체를 중단.

### 구분자와 플랫폼 차이

- `filepath.Separator`
  - 유닉스: `/`
  - 윈도우: `\`
- `filepath.ListSeparator`(PATH 등 목록 구분자)
  - 유닉스: `:`
  - 윈도우: `;`
- 절대 경로 판단
  - 유닉스: `/`로 시작
  - 윈도우: `C:\...` 또는 `\\host\share`(UNC)

- `filepath.ToSlash(path string) string` / `filepath.FromSlash(path string) string` — OS 구분자와 `/` 사이를 상호 변환. 예를 들어 URL이나 설정 파일처럼 항상 `/`를 쓰는 문자열을 로컬 경로로 바꿀 때 사용
- `filepath.VolumeName(path string) string` — 윈도우에서 드라이브 문자(`C:`)나 UNC 공유 이름을 뽑아냄. 유닉스에서는 항상 빈 문자열
- `filepath.SplitList(path string) []string` — `PATH` 환경 변수처럼 `ListSeparator`로 구분된 문자열을 분리

이러한 구분 덕분에 같은 코드가 크로스 컴파일만으로 유닉스와 윈도우 양쪽에서 올바르게 동작함.

---

## path 패키지

> 원문: https://pkg.go.dev/path

### 개요와 filepath와의 차이

`path` — 항상 슬래시(`/`)만 구분자로 쓰는 추상 경로를 다룸.
- 함수 이름과 동작은 `filepath`와 거의 같음(`Join`, `Split`, `Dir`, `Base`, `Ext`, `Clean`, `Match`, `IsAbs`)
- 실행 중인 운영체제와 무관하게 항상 `/`를 기준으로 판단한다는 점이 다름

- URL 경로, 아카이브(zip/tar) 내부 경로처럼 "파일 시스템이 아니지만 슬래시로 계층을 표현하는" 문자열을 다룰 때 이 패키지를 사용
- 실제 로컬 파일을 열거나 만들 때는 반드시 `path`가 아니라 `path/filepath`를 사용 필요 → 윈도우에서 `path`를 쓰면 `\`를 구분자로 인식하지 못해 경로가 깨짐

```go
import "net/url"
import "path"

u, _ := url.Parse("https://example.com/api/v1/users/42")
fmt.Println(path.Base(u.Path)) // 42
fmt.Println(path.Dir(u.Path))  // /api/v1/users
```

### 요약

- 로컬 파일/디렉터리 경로 조작 → `path/filepath` 사용
- URL 경로, 슬래시 전용 가상 경로 → `path` 사용
- 실제 파일 열기·읽기·쓰기·삭제 → `os` 사용
