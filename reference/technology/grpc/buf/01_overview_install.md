# buf 개요와 설치

> 원본: https://buf.build/docs/cli/ , https://buf.build/docs/cli/installation/

---

## 목차

1. [buf란 무엇인가](#1-buf란-무엇인가)
2. [왜 buf를 쓰는가](#2-왜-buf를-쓰는가)
3. [핵심 명령 개요](#3-핵심-명령-개요)
4. [설정 파일 개요](#4-설정-파일-개요)
5. [설치](#5-설치)
6. [버전 확인과 셸 자동완성](#6-버전-확인과-셸-자동완성)

---

## 1. buf란 무엇인가

buf는 Protocol Buffers(protobuf)를 위한 현대적인 툴체인(toolchain) → 일상적인 `protoc` 사용을 대체함. 다음 기능을 하나의 CLI로 제공함.

- 빠른 컴파일러(compiler) — 모듈 인식 워크스페이스 기반
- 포매팅(`buf format`)
- 린팅(`buf lint`)
- 호환성 깨짐 검출(`buf breaking`)
- 코드 생성(`buf generate`)
- 의존성 관리(`buf dep`)
- Buf Schema Registry(BSR) 연동(`buf push` 등)

설정은 `buf.yaml`(워크스페이스/모듈)과 `buf.gen.yaml`(코드 생성) 두 파일로 구성 → 복잡한 셸 스크립트나 `protoc` 호출 인자 조합 불필요.

---

## 2. 왜 buf를 쓰는가

`protoc`는 대규모 프로젝트에서 여러 어려움을 유발함 → buf가 이를 해결함.

- 자동 파일 탐색: include path를 수동 지정하지 않아도 `.proto` 파일을 자동 탐색
- 내장 품질 검사: 40개 이상의 린트 규칙, 50개 이상의 호환성 깨짐 규칙을 기본 제공
- 병렬 컴파일: `protoc` 대비 약 2배의 처리량
- 범용 입력 처리: 디렉터리, Git 저장소, tarball, 사전 빌드된 이미지(image) 등을 입력으로 처리
- 네이티브 의존성 관리: Go modules나 npm 패키지처럼 의존성 관리
- 퍼블리싱 워크플로: 로컬 스키마를 BSR에 게시해 소비자(consumer)에게 배포
- 에디터 연동: LSP(Language Server Protocol)로 VS Code, JetBrains, Vim 등에서 동작

---

## 3. 핵심 명령 개요

- `buf build`: `.proto` 파일을 이미지(image)/FileDescriptorSet로 컴파일
- `buf lint`: 스타일/구조 규칙 검사
- `buf breaking`: 호환성을 깨는 변경 검출
- `buf format`: 표준 스타일로 재포매팅
- `buf generate`: 플러그인을 실행해 코드 스텁(stub) 생성
- `buf curl`: 스키마를 이용해 gRPC/Connect 엔드포인트 호출
- `buf dep`: BSR 모듈 의존성 관리
- `buf push`: 모듈을 BSR에 게시
- `buf export`: 모듈/입력의 `.proto` 파일을 디렉터리로 추출
- `buf ls-files`: 입력에 포함된 `.proto` 파일 목록 출력
- `buf lsp serve`: 에디터용 language server 실행

각 명령의 상세는 `07_cli_reference.md` 참고.

---

## 4. 설정 파일 개요

- `buf.yaml`: 워크스페이스/모듈 정의, 의존성(deps), lint/breaking 규칙
- `buf.gen.yaml`: 코드 생성 — 플러그인, managed mode, 입력
- `buf.lock`: 의존성을 특정 커밋(commit)으로 고정(pin)하는 잠금 파일
- `buf.work.yaml`: (v1 전용) 멀티 모듈 워크스페이스 정의 → v2에서는 `buf.yaml`의 `modules`로 통합됨

`buf config init` 명령으로 설정 파일 생성 가능.

---

## 5. 설치

### Homebrew (macOS / Linux)

```bash
brew install bufbuild/buf/buf
```

Homebrew 설치 시 Bash, fish, zsh 자동완성이 함께 설치됨.

### npm (프로젝트 로컬)

```bash
npm install @bufbuild/buf
npx buf --version
```

### Go (소스에서 설치)

버전을 명시적으로 고정하여 설치하는 것을 권장.

```bash
GOBIN=/usr/local/bin go install github.com/bufbuild/buf/cmd/buf@v1.71.0
```

### 바이너리 / tarball 다운로드

GitHub Releases에서 OS/아키텍처에 맞는 바이너리를 내려받거나, 자동완성 파일이 포함된 tarball로 설치.

```bash
PREFIX="/usr/local" && VERSION="1.71.0" && \
  curl -sSL "https://github.com/bufbuild/buf/releases/download/v${VERSION}/buf-$(uname -s)-$(uname -m).tar.gz" | \
  tar -xvzf - -C "${PREFIX}" --strip-components 1
```

### Docker

```bash
docker run --volume "$(pwd):/workspace" --workdir /workspace bufbuild/buf lint
```

### Windows

```bash
scoop install buf            # Scoop
winget install bufbuild.buf  # WinGet
```

---

## 6. 버전 확인과 셸 자동완성

설치 후 버전 확인.

```bash
buf --version
# 예: 1.71.0
```

zsh 자동완성을 수동으로 설정하려면 다음과 같이 입력.

```bash
buf completion zsh > "${fpath[1]}/_buf"
```

tarball 설치 시 자동완성 파일은 `${PREFIX}/share/{fish,zsh}/...` 경로에 배치됨. CI 등 재현성이 중요한 환경에서는 버전을 명시적으로 고정(예: `@v1.71.0`)하여 설치하는 것을 권장.
