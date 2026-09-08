# Buf Schema Registry (BSR)

> 원본: https://buf.build/docs/bsr/

<a id="1-bsr이란"></a>
## 1. BSR이란

Buf Schema Registry는 `.proto` 묶음을 버전별로 관리하는 Protobuf 전용 레지스트리다. 공개 서비스는 `buf.build`에서 제공하며 `buf.build/connectrpc/eliza`처럼 모듈을 참조한다. 조직에서는 Pro/Enterprise/온프레미스 방식으로 비공개 운영할 수 있다.

스키마를 한곳에서 관리하면 빌드 검증부터 소비자의 SDK 설치까지 연결할 수 있다. BSR은 다음 기능을 제공한다.

- 빌드 보장: 깨진 push를 거부해 소비자가 잘못된 스키마를 받지 않게 함
- 단일 진실 공급원(single source of truth): 이름(`buf.build/owner/name`)으로 주소화되어 드리프트 방지
- 패키지 매니저 통합: vendoring이나 로컬 protoc 호출 없이 생성 SDK를 네이티브 패키지로 설치

<a id="2-모듈과-저장소-커밋라벨"></a>

## 2. 모듈과 저장소, 커밋, 라벨

모듈은 `.proto` 파일과 의존성을 담은 버전 관리 묶음이며 BSR의 한 저장소에 대응한다. 생산자(producer)는 스키마를 개발하고, 소비자(consumer)는 SDK 설치, 문서 열람, API 테스트, 의존성 참조를 통해 그 스키마를 사용한다.

스키마의 변경 이력과 사용할 버전은 커밋과 라벨로 관리한다.

- 커밋(commit): 매 push마다 생성되어 변경 이력을 추적함
- 라벨(label): 특정 커밋을 가리키는 이름(기본 라벨이 릴리스를 추적) → 의존성/입력 지정 시 라벨이나 커밋을 명시 가능
- 각 커밋은 문법 강조 및 상호 참조가 가능한 문서를 자동 생성함

<a id="3-인증-login"></a>

## 3. 인증 (login)

```bash
buf registry login          # BSR 인증 (토큰 입력)
buf registry whoami         # 현재 신원 확인
buf registry logout
```

- CI에서는 `BUF_TOKEN` 환경 변수로 인증 가능

<a id="4-push와-pullexport"></a>

## 4. push와 pull/export

### push

```bash
buf push                    # 현재 워크스페이스의 모듈을 BSR에 게시
buf push --label v1.2.0     # 라벨을 붙여 게시
```

- 매 push마다 커밋이 생성되며, BSR은 제출된 스키마를 컴파일, 검증(lint/breaking 검사/리뷰 정책)한 뒤 통과한 경우에만 수용함

### export

- 모듈/입력의 `.proto` 파일을 로컬 디렉터리로 추출함

```bash
buf export buf.build/connectrpc/eliza --output ./out
buf export . --output ./vendor          # 로컬 입력 추출
```

<a id="5-의존성-관리"></a>

## 5. 의존성 관리

다른 BSR 모듈의 스키마를 사용하려면 `buf.yaml`의 `deps`에 모듈을 선언한 뒤 의존성을 해석한다.

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

`buf dep update`로 생성한 `buf.lock`은 의존성을 특정 커밋으로 고정한다. 같은 스키마로 빌드를 재현하기 위한 파일이며, 자세한 내용은 `02_modules_workspaces.md`를 참고한다.

<a id="6-원격-플러그인"></a>

## 6. 원격 플러그인

BSR이 호스팅하는 플러그인으로 코드를 생성하면 로컬에 protoc나 플러그인을 설치할 필요가 없다. 아래처럼 `buf.gen.yaml`의 `remote`에 사용할 플러그인을 지정한다.

```yaml
version: v2
plugins:
  - remote: buf.build/protocolbuffers/go:v1.34.2
    out: gen/go
  - remote: buf.build/grpc/go:v1.4.0
    out: gen/go
```

- 코드 생성은 BSR 서버에서 수행됨 → 버전을 명시(`:v1.34.2`)해 재현성 확보

<a id="7-generated-sdk"></a>

## 7. Generated SDK

- 소비자는 모듈에서 생성된 SDK를 각 언어 패키지 매니저로 바로 설치 가능

```bash
# Go (예: protocolbuffers/go + connectrpc/go 생성물)
go get buf.build/gen/go/<owner>/<module>/protocolbuffers/go

# npm
npm install @buf/<owner>_<module>

# Cargo
cargo add buf_<owner>_<module>
```

- vendoring이나 직접 코드 생성 없이 최신 스키마 기반 클라이언트/서버 코드를 바로 사용 가능
