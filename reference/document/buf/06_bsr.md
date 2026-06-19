# Buf Schema Registry (BSR)

> 이 문서는 BSR의 개념, 모듈 push/pull, 의존성 관리, 원격 플러그인, generated SDK를 한국어로 정리한 것입니다.
> 원본: https://buf.build/docs/bsr/

---

## 목차

1. [BSR이란](#1-bsr이란)
2. [모듈과 저장소, 커밋·라벨](#2-모듈과-저장소-커밋라벨)
3. [인증 (login)](#3-인증-login)
4. [push와 pull/export](#4-push와-pullexport)
5. [의존성 관리](#5-의존성-관리)
6. [원격 플러그인](#6-원격-플러그인)
7. [Generated SDK](#7-generated-sdk)

---

## 1. BSR이란

Buf Schema Registry는 버전 관리되는 `.proto` 묶음을 다루는 Protobuf 전용 레지스트리입니다. 공개 서비스는 `buf.build`에서 동작하며(예: `buf.build/connectrpc/eliza`), 조직용으로는 Pro/Enterprise/온프레미스로 비공개 운영할 수 있습니다.

BSR이 해결하는 문제:

- **빌드 보장**: 깨진 push를 거부해 소비자가 잘못된 스키마를 받지 않게 함.
- **단일 진실 공급원(single source of truth)**: 이름(`buf.build/owner/name`)으로 주소화되어 드리프트 방지.
- **패키지 매니저 통합**: vendoring이나 로컬 protoc 호출 없이 생성 SDK를 네이티브 패키지로 설치.

---

## 2. 모듈과 저장소, 커밋·라벨

- 모듈은 `.proto` 파일과 의존성을 담은 버전 관리 묶음이며, BSR의 한 저장소에 대응합니다.
- 사용자는 **생산자(producer)**(스키마 개발)와 **소비자(consumer)**(SDK 설치, 문서 열람, API 테스트, 의존)로 나뉩니다.
- **커밋(commit)**: 매 push마다 생성되어 변경 이력을 추적합니다.
- **라벨(label)**: 특정 커밋을 가리키는 이름(기본 라벨이 릴리스를 추적). 의존성/입력 지정 시 라벨이나 커밋을 명시할 수 있습니다.
- 각 커밋은 문법 강조 및 상호 참조가 가능한 문서를 자동 생성합니다.

---

## 3. 인증 (login)

```bash
buf registry login          # BSR 인증 (토큰 입력)
buf registry whoami         # 현재 신원 확인
buf registry logout
```

CI에서는 `BUF_TOKEN` 환경 변수로 인증할 수 있습니다.

---

## 4. push와 pull/export

### push

```bash
buf push                    # 현재 워크스페이스의 모듈을 BSR에 게시
buf push --label v1.2.0     # 라벨을 붙여 게시
```

매 push마다 커밋이 생성되며, BSR은 제출된 스키마를 컴파일·검증(lint/breaking/리뷰 정책)한 뒤 통과한 경우에만 수용합니다.

### export

모듈/입력의 `.proto` 파일을 로컬 디렉터리로 추출합니다.

```bash
buf export buf.build/connectrpc/eliza --output ./out
buf export . --output ./vendor          # 로컬 입력 추출
```

---

## 5. 의존성 관리

`buf.yaml`의 `deps`에 BSR 모듈을 선언한 뒤 해석합니다.

```yaml
# buf.yaml
deps:
  - buf.build/googleapis/googleapis
```

```bash
buf dep update     # deps 해석 후 buf.lock에 커밋 고정
buf dep graph      # 의존성 그래프 출력
buf dep prune      # 사용하지 않는 의존성 제거 (v2)
```

`buf.lock`이 의존성을 특정 커밋으로 고정하여 재현 가능한 빌드를 보장합니다(상세는 `02_modules_workspaces.md` 참고).

---

## 6. 원격 플러그인

BSR이 호스팅하는 플러그인을 사용하면 로컬 protoc/플러그인 설치 없이 코드를 생성할 수 있습니다. `buf.gen.yaml`에서 `remote`로 참조합니다.

```yaml
version: v2
plugins:
  - remote: buf.build/protocolbuffers/go:v1.34.2
    out: gen/go
  - remote: buf.build/grpc/go:v1.4.0
    out: gen/go
```

생성은 BSR 측에서 수행되며 버전을 명시(`:v1.34.2`)해 재현성을 확보합니다.

---

## 7. Generated SDK

소비자는 모듈에서 생성된 SDK를 각 언어 패키지 매니저로 바로 설치할 수 있습니다.

```bash
# Go (예: protocolbuffers/go + connectrpc/go 생성물)
go get buf.build/gen/go/<owner>/<module>/protocolbuffers/go

# npm
npm install @buf/<owner>_<module>

# Cargo
cargo add buf_<owner>_<module>
```

이를 통해 vendoring이나 직접 코드 생성 없이도 최신 스키마 기반 클라이언트/서버 코드를 사용할 수 있습니다.
