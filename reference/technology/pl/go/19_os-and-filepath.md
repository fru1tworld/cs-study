# 파일시스템과 운영체제 연동 (os / path / path/filepath)

# os 패키지

> **원문:** https://pkg.go.dev/os

## 개요

`os`는 파일 읽기/쓰기, 프로세스 정보 조회, 환경 변수 접근, 종료 코드 지정 등 운영체제와 상호작용하는 기능을 제공합니다. 인터페이스는 유닉스 계열을 기준으로 설계되어 있지만, 오류 처리 방식은 플랫폼 독립적으로 설계되어 있어 실패 원인을 `error` 값으로 통일해서 다룰 수 있습니다.

## 파일 열기와 생성

파일을 다루는 세 가지 진입점이 있습니다.

- `os.Open(name string) (*os.File, error)`: 읽기 전용(`O_RDONLY`)으로 연다.
- `os.Create(name string) (*os.File, error)`: 쓰기용으로 새로 만들거나, 이미 있으면 비운다(truncate). 권한은 `0666`(umask 적용 전)이다.
- `os.OpenFile(name string, flag int, perm os.FileMode) (*os.File, error)`: 플래그와 권한을 직접 지정하는 범용 버전.

`flag`는 아래 상수를 비트 OR로 조합한다.

| 플래그 | 의미 |
|---|---|
| `O_RDONLY` / `O_WRONLY` / `O_RDWR` | 읽기/쓰기/읽기+쓰기 |
| `O_APPEND` | 쓸 때 파일 끝에 이어 붙임 |
| `O_CREATE` | 없으면 생성 |
| `O_EXCL` | `O_CREATE`와 함께 쓰면 이미 존재할 때 실패 (원자적 생성) |
| `O_TRUNC` | 열 때 내용을 비움 |

```go
f, err := os.OpenFile("app.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
if err != nil {
    log.Fatal(err)
}
defer f.Close()
```

`Open`/`Create`가 반환하는 `*os.File`은 다 쓰고 나면 반드시 `Close()`를 호출해야 하며, 관용적으로 여는 직후 `defer f.Close()`를 붙인다.

## File 메서드로 읽고 쓰기

`*os.File`은 `io.Reader`, `io.Writer`, `io.Seeker`, `io.Closer`를 모두 만족한다.

- `Read(b []byte) (n int, err error)` / `ReadAt(b []byte, off int64) (n int, err error)`
- `Write(b []byte) (n int, err error)` / `WriteAt(...)` / `WriteString(s string) (n int, err error)`
- `Seek(offset int64, whence int) (int64, error)` — `whence`는 `io.SeekStart`, `io.SeekCurrent`, `io.SeekEnd`
- `Stat() (os.FileInfo, error)`, `Truncate(size int64) error`, `Sync() error`(디스크로 강제 플러시)

작은 파일이라면 스트림을 직접 다루기보다 파일 전체를 한 번에 읽고 쓰는 헬퍼 함수를 쓰는 편이 간결하다(Go 1.16부터 추가).

```go
data, err := os.ReadFile("config.json")
if err != nil {
    log.Fatal(err)
}

err = os.WriteFile("config.json", data, 0644)
```

## 디렉터리 다루기

- `os.Mkdir(name string, perm os.FileMode) error`: 디렉터리 하나만 생성(상위 경로가 없으면 실패).
- `os.MkdirAll(path string, perm os.FileMode) error`: 필요한 상위 경로까지 모두 생성.
- `os.ReadDir(name string) ([]os.DirEntry, error)`: 디렉터리 항목을 이름순으로 반환(Go 1.16+). 예전의 `ioutil.ReadDir`과 달리 `Stat`을 미리 호출하지 않아 더 빠르다.

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

## 파일 정보와 권한

- `os.Stat(name string) (os.FileInfo, error)`: 심볼릭 링크를 따라가서 정보를 조회한다.
- `os.Lstat(name string) (os.FileInfo, error)`: 링크 자체의 정보를 조회한다(따라가지 않음).
- `os.FileInfo` 인터페이스: `Name()`, `Size()`, `Mode() os.FileMode`, `ModTime()`, `IsDir()`, `Sys()`.
- `os.FileMode`: 유닉스 권한 비트(`Perm()`)와 파일 종류 플래그(`ModeDir`, `ModeSymlink` 등)를 함께 담는 타입.

```go
info, err := os.Stat("main.go")
if err == nil {
    fmt.Printf("%s: %d바이트, 권한 %#o\n", info.Name(), info.Size(), info.Mode().Perm())
}
```

권한 변경/소유자 변경은 `os.Chmod(name string, mode os.FileMode) error`, `os.Chown(name string, uid, gid int) error`(유닉스 전용)로 한다.

## 삭제, 이동, 링크

- `os.Remove(name string) error`: 파일 또는 빈 디렉터리 삭제.
- `os.RemoveAll(path string) error`: 경로와 하위 내용을 재귀적으로 삭제(없어도 오류 없음).
- `os.Rename(oldpath, newpath string) error`: 이동 또는 이름 변경.
- `os.Symlink` / `os.Link` / `os.Readlink`: 심볼릭 링크·하드 링크 생성 및 링크 대상 조회.

## 임시 파일과 디렉터리

테스트나 캐시 목적으로 임시 자원을 만들 때 쓴다(Go 1.16+).

```go
tmp, err := os.MkdirTemp("", "myapp-*")
if err != nil {
    log.Fatal(err)
}
defer os.RemoveAll(tmp)

f, err := os.CreateTemp(tmp, "data-*.tmp")
```

- `os.TempDir() string`: 시스템 기본 임시 디렉터리 경로(리눅스 `/tmp`, 윈도우는 `%TMP%` 등)를 반환한다.
- `pattern`에 `*`가 있으면 그 자리를 임의의 문자열로 치환해 이름 충돌을 피한다.

## 환경 변수

- `os.Getenv(key string) string`: 값이 없으면 빈 문자열(값이 실제로 빈 문자열인지 구분 불가).
- `os.LookupEnv(key string) (string, bool)`: 존재 여부를 `bool`로 구분해서 알려준다.
- `os.Setenv(key, value string) error`, `os.Unsetenv(key string) error`
- `os.Environ() []string`: `"KEY=VALUE"` 형태의 전체 목록.
- `os.ExpandEnv(s string) string`: 문자열 안의 `$VAR`, `${VAR}`를 환경 변수 값으로 치환.

```go
port, ok := os.LookupEnv("PORT")
if !ok {
    port = "8080"
}
```

## 프로세스와 작업 디렉터리

- `os.Args []string`: 커맨드라인 인자(0번째는 실행 파일 경로).
- `os.Getwd() (string, error)` / `os.Chdir(dir string) error`: 현재 작업 디렉터리 조회/변경.
- `os.Hostname() (string, error)`
- `os.Getpid()`, `os.Getppid()`: 자신/부모 프로세스 ID.
- `os.Exit(code int)`: 즉시 종료한다. `defer`가 실행되지 않으므로 정리 작업이 필요하면 `Exit`를 직접 호출하지 말고 `main`이 정상적으로 반환하도록 구성하는 편이 안전하다.
- `os.Stdin`, `os.Stdout`, `os.Stderr`: 표준 입출력을 가리키는 `*os.File` 값.

## 사용자 디렉터리

- `os.UserHomeDir() (string, error)` (Go 1.12+)
- `os.UserCacheDir() (string, error)` (Go 1.11+): `$XDG_CACHE_HOME`, macOS의 `~/Library/Caches` 등 플랫폼 관례를 따른다.
- `os.UserConfigDir() (string, error)` (Go 1.13+)

## 오류 처리

파일 시스템 호출 오류는 대개 `*fs.PathError`(`os.PathError`의 별칭)로 감싸져 오며, 실패한 연산(`Op`), 대상 경로(`Path`), 근본 원인(`Err`)을 담는다. 원인 종류는 문자열 비교 대신 판별 함수로 확인한다.

```go
if _, err := os.Open("missing.txt"); err != nil {
    if os.IsNotExist(err) {
        fmt.Println("파일이 없습니다")
    }
}
```

- `os.IsNotExist(err) bool`, `os.IsExist(err) bool`, `os.IsPermission(err) bool`
- 새 코드에서는 `errors.Is(err, fs.ErrNotExist)`처럼 `fs` 패키지의 sentinel 오류(`ErrNotExist`, `ErrExist`, `ErrPermission`, `ErrClosed`)와 `errors.Is`를 함께 쓰는 방식이 더 권장된다.

---

# path/filepath 패키지

> **원문:** https://pkg.go.dev/path/filepath

## 개요

`filepath`는 실제 파일 시스템 경로, 즉 운영체제에 맞는 구분자(유닉스 `/`, 윈도우 `\`)를 쓰는 경로를 다룬다. 문자열을 직접 자르고 붙이는 대신 이 패키지의 함수를 쓰면 플랫폼 차이를 신경 쓰지 않아도 된다.

## 경로 조립·분해

- `filepath.Join(elem ...string) string`: 구분자로 이어 붙이고 결과를 `Clean`까지 적용한다. 빈 인자는 무시된다.
- `filepath.Split(path string) (dir, file string)`: 마지막 구분자를 기준으로 나눈다. 항상 `dir + file == path`가 성립한다.
- `filepath.Dir(path string) string`: 디렉터리 부분만.
- `filepath.Base(path string) string`: 마지막 요소(파일/폴더 이름)만.
- `filepath.Ext(path string) string`: 마지막 `.` 이후의 확장자(`.` 포함).

```go
p := filepath.Join("data", "logs", "2024", "app.log")
// data/logs/2024/app.log (유닉스) 또는 data\logs\2024\app.log (윈도우)

dir, file := filepath.Split(p)
fmt.Println(dir, file, filepath.Ext(file)) // data/logs/2024/ app.log .log
```

## 경로 정규화와 절대/상대 변환

- `filepath.Clean(path string) string`: 중복 구분자 제거, `.` 제거, 가능한 `..` 축약을 통해 가장 짧은 동치 경로를 만든다.
- `filepath.Abs(path string) (string, error)`: 상대 경로면 현재 작업 디렉터리를 붙여 절대 경로로 바꾼다.
- `filepath.Rel(basepath, targpath string) (string, error)`: `base` 기준으로 `target`까지 가는 상대 경로를 계산한다. `Join(base, Rel(base, target)) == target`이 성립하도록 설계됨.
- `filepath.EvalSymlinks(path string) (string, error)`: 경로에 포함된 심볼릭 링크를 모두 실제 경로로 해석한다.
- `filepath.IsAbs(path string) bool`: 절대 경로 여부.

```go
rel, _ := filepath.Rel("/home/user/project", "/home/user/project/src/main.go")
fmt.Println(rel) // src/main.go
```

## 패턴 매칭과 순회

- `filepath.Match(pattern, name string) (bool, error)`: 셸 스타일 글롭(`*`, `?`, `[...]`)으로 한 이름을 매칭한다. `*`, `?`는 구분자를 넘어가지 않는다.
- `filepath.Glob(pattern string) ([]string, error)`: 패턴에 매칭되는 실제 경로 목록을 파일 시스템에서 찾아 반환한다.
- `filepath.WalkDir(root string, fn fs.WalkDirFunc) error` (Go 1.16+): 디렉터리 트리를 순회하며 각 항목마다 콜백을 호출한다. 예전의 `filepath.Walk`보다 효율적이며(불필요한 `Stat` 호출을 피함) 신규 코드에서는 이쪽을 우선 사용한다.

```go
err := filepath.WalkDir(".", func(path string, d fs.DirEntry, err error) error {
    if err != nil {
        return err
    }
    if d.IsDir() && d.Name() == "vendor" {
        return filepath.SkipDir // vendor 디렉터리는 건너뛴다
    }
    if !d.IsDir() && filepath.Ext(path) == ".go" {
        fmt.Println(path)
    }
    return nil
})
```

콜백에서 `filepath.SkipDir`을 반환하면 해당 디렉터리를 건너뛰고, `filepath.SkipAll`을 반환하면 순회 전체를 중단한다.

## 구분자와 플랫폼 차이

| 항목 | 유닉스 | 윈도우 |
|---|---|---|
| `filepath.Separator` | `/` | `\` |
| `filepath.ListSeparator` (PATH 등 목록 구분자) | `:` | `;` |
| 절대 경로 판단 | `/`로 시작 | `C:\...` 또는 `\\host\share` (UNC) |

- `filepath.ToSlash(path string) string` / `filepath.FromSlash(path string) string`: OS 구분자와 `/` 사이를 상호 변환한다. 예를 들어 URL이나 설정 파일처럼 항상 `/`를 쓰는 문자열을 로컬 경로로 바꿀 때 쓴다.
- `filepath.VolumeName(path string) string`: 윈도우에서 드라이브 문자(`C:`)나 UNC 공유 이름을 뽑아내며, 유닉스에서는 항상 빈 문자열이다.
- `filepath.SplitList(path string) []string`: `PATH` 환경 변수처럼 `ListSeparator`로 구분된 문자열을 분리한다.

이러한 구분 덕분에 같은 코드가 크로스 컴파일만으로 유닉스와 윈도우 양쪽에서 올바르게 동작한다.

---

# path 패키지

> **원문:** https://pkg.go.dev/path

## 개요와 filepath와의 차이

`path`는 항상 슬래시(`/`)만 구분자로 쓰는 추상 경로를 다룬다. 함수 이름과 동작은 `filepath`와 거의 같지만(`Join`, `Split`, `Dir`, `Base`, `Ext`, `Clean`, `Match`, `IsAbs`), 실행 중인 운영체제와 무관하게 항상 `/`를 기준으로 판단한다는 점이 다르다.

- URL 경로, 아카이브(zip/tar) 내부 경로처럼 "파일 시스템이 아니지만 슬래시로 계층을 표현하는" 문자열을 다룰 때 이 패키지를 쓴다.
- 실제 로컬 파일을 열거나 만들 때는 반드시 `path`가 아니라 `path/filepath`를 사용해야 한다. 윈도우에서 `path`를 쓰면 `\`를 구분자로 인식하지 못해 경로가 깨진다.

```go
import "net/url"
import "path"

u, _ := url.Parse("https://example.com/api/v1/users/42")
fmt.Println(path.Base(u.Path)) // 42
fmt.Println(path.Dir(u.Path))  // /api/v1/users
```

## 요약

| 상황 | 사용할 패키지 |
|---|---|
| 로컬 파일/디렉터리 경로 조작 | `path/filepath` |
| URL 경로, 슬래시 전용 가상 경로 | `path` |
| 실제 파일 열기·읽기·쓰기·삭제 | `os` |
