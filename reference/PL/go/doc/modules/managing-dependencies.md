# 의존성 관리

## 개요

코드가 외부 패키지를 사용할 때, 해당 패키지(모듈로 배포됨)는 의존성이 됩니다. Go는 외부 의존성을 통합하면서 Go 애플리케이션을 안전하게 유지하는 데 도움이 되는 의존성 관리 도구를 제공합니다.

## 의존성 사용 및 관리 워크플로

가장 일반적인 의존성 관리 단계는 다음과 같습니다:

1. [pkg.go.dev](https://pkg.go.dev)에서 **유용한 패키지를 찾습니다**
2. 코드에서 원하는 **패키지를 임포트합니다**
3. 의존성 추적을 위해 **코드를 모듈에 추가합니다** (아직 없는 경우)
4. **외부 패키지를 의존성으로 추가**하여 관리합니다
5. 시간이 지남에 따라 필요에 따라 **의존성 버전을 업그레이드하거나 다운그레이드합니다**

## 모듈로서의 의존성 관리

Go는 다음을 포함하는 시스템을 통해 의존성을 관리합니다:

- **분산 게시 시스템** - 개발자가 자신의 저장소에서 버전 번호와 함께 모듈을 게시
- **패키지 검색 엔진** - 모듈을 찾기 위한 pkg.go.dev
- **버전 번호 규칙** - 안정성과 하위 호환성을 위한 시맨틱 버전 관리
- **Go 도구** - 의존성 관리를 위한 명령어

## 유용한 패키지 찾기 및 임포트

[pkg.go.dev](https://pkg.go.dev)에서 패키지를 검색합니다. 패키지 경로를 복사하여 import 문에 붙여넣습니다:

```go
import "rsc.io/quote"
```

## 코드에서 의존성 추적 활성화

`go mod init`을 사용하여 go.mod 파일을 생성합니다:

```bash
$ go mod init example/mymodule
```

이 명령은:
- 프로젝트 루트에 go.mod 파일을 생성
- 추가하는 모든 의존성을 추적
- 검증을 위한 체크섬이 포함된 go.sum 파일도 생성

## 모듈 이름 짓기

모듈 경로는 다음 형식을 따릅니다:

```
<prefix>/<descriptive-text>
```

**접두사 옵션:**
- 저장소 위치(게시된 모듈에 권장): `github.com/<project-name>/`
- 통제하는 이름(회사 이름 등)

**예약된 접두사:**
- `test` - 다른 모듈을 로컬에서 테스트하는 모듈용
- `example` - 문서 및 튜토리얼용

## 의존성 추가

`go get` 명령을 사용하여 의존성을 추가합니다:

```bash
# 현재 디렉터리의 패키지에 대한 모든 의존성 추가
$ go get .

# 특정 의존성 추가
$ go get example.com/theirmodule
```

`go get` 명령은:
- go.mod에 `require` 지시문을 추가
- 모듈 소스 코드를 다운로드
- 보안을 위해 각 모듈을 인증

## 특정 의존성 버전 가져오기

```bash
# 특정 버전 가져오기
$ go get example.com/theirmodule@v1.3.4

# 최신 버전 가져오기
$ go get example.com/theirmodule@latest
```

go.mod 파일에 표시됩니다:
```
require example.com/theirmodule v1.3.4
```

## 사용 가능한 업데이트 발견

`go list`를 사용하여 새 버전을 확인합니다:

```bash
# 사용 가능한 최신 버전과 함께 모든 의존성 나열
$ go list -m -u all

# 특정 모듈 확인
$ go list -m -u example.com/theirmodule
```

## 의존성 업그레이드 또는 다운그레이드

1. `go list -m -u`를 사용하여 새 버전을 발견
2. `go get`과 버전 지정을 사용하여 특정 버전 추가

## 코드의 의존성 동기화

`go mod tidy`를 사용하여 관리되는 의존성이 임포트된 패키지와 일치하도록 합니다:

```bash
$ go mod tidy
```

이 명령은:
- 임포트에 필요한 누락된 모듈을 추가
- 사용되지 않는 모듈을 제거
- 제거된 모듈을 보려면 `-v` 플래그 사용

## 게시되지 않은 모듈 코드에 대한 개발 및 테스트

### 로컬 디렉터리의 모듈 코드 요청

go.mod에서 `replace` 지시문을 사용합니다:

```
module example.com/mymodule

go 1.23.0

require example.com/theirmodule v0.0.0-unpublished

replace example.com/theirmodule v0.0.0-unpublished => ../theirmodule
```

Go 도구를 사용하여 설정합니다:

```bash
$ go mod edit -replace=example.com/theirmodule@v0.0.0-unpublished=../theirmodule
$ go get example.com/theirmodule@v0.0.0-unpublished
```

### 자신의 저장소 포크에서 외부 모듈 코드 요청

모듈 경로를 포크로 대체합니다:

```
module example.com/mymodule

go 1.23.0

require example.com/theirmodule v1.2.3

replace example.com/theirmodule v1.2.3 => example.com/myfork/theirmodule v1.2.3-fixed
```

명령어로 설정:

```bash
$ go list -m example.com/theirmodule
example.com/theirmodule v1.2.3
$ go mod edit -replace=example.com/theirmodule@v1.2.3=example.com/myfork/theirmodule@v1.2.3-fixed
```

## 저장소 식별자를 사용하여 특정 커밋 가져오기

커밋 해시나 브랜치와 함께 `go get`을 사용합니다:

```bash
# 특정 커밋 가져오기
$ go get example.com/theirmodule@4cf76c2

# 특정 브랜치 가져오기
$ go get example.com/theirmodule@bugfixes
```

## 의존성 제거

사용되지 않는 모든 의존성 제거:

```bash
$ go mod tidy
```

특정 의존성 제거:

```bash
$ go get example.com/theirmodule@none
```

이는 또한 제거된 모듈에 의존하는 다른 의존성을 다운그레이드하거나 제거합니다.

## 도구 의존성

Go 1.24+에서는 다음으로 도구 의존성을 추가합니다:

```bash
$ go get -tool golang.org/x/tools/cmd/stringer
```

이는 go.mod에 `tool` 지시문을 추가합니다. 도구 실행:

```bash
$ go tool stringer
```

동일한 경로 조각을 가진 여러 도구나 Go 배포 도구와 일치하는 경우:

```bash
$ go tool golang.org/x/tools/cmd/stringer
```

사용 가능한 모든 도구 나열:

```bash
$ go tool
```

수동으로 도구 지시문을 추가하지만 해당 `require` 지시문이 존재하는지 확인합니다. 누락된 요구 사항을 추가하려면 `go mod tidy`를 실행합니다.

도구 의존성:
- 최소 버전 선택에 참여
- `require`, `replace`, `exclude` 지시문을 준수
- 모듈 가지치기로 인해 보통 모듈의 요구 사항이 되지 않음

## 모듈 프록시 서버 지정

`GOPROXY` 환경 변수를 설정합니다:

```
GOPROXY="https://proxy.golang.org,direct"
```

**기본 동작:**
- 먼저 Google이 운영하는 공개 프록시 사용
- 모듈 저장소에서 직접 다운로드로 대체

**쉼표로 여러 프록시:**
```
GOPROXY="https://proxy.example.com,https://proxy2.example.com"
```
HTTP 404 또는 410 오류에서만 다음 URL 시도.

**파이프로 여러 프록시:**
```
GOPROXY="https://proxy.example.com|https://proxy2.example.com"
```
HTTP 오류 코드에 관계없이 다음 URL 시도.

### 비공개 모듈

비공개 모듈에 대해 `GOPRIVATE` 환경 변수를 구성합니다:

```
GOPRIVATE=*.corp.example.com,*.research.example.com
```

프록시를 사용하지 않아야 하는 모듈을 지정하는 `GONOPROXY`도 지원합니다.
