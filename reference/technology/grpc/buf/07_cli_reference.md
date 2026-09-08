# CLI 레퍼런스

> 원본: https://buf.build/docs/cli/ , https://buf.build/docs/reference/cli/buf/

<a id="1-공통-사항"></a>
## 1. 공통 사항

대부분의 명령은 디렉터리(기본 `.`), Git 저장소, tarball/zip, BSR 모듈, buf 이미지를 입력으로 받는다. 입력을 바꾸면 같은 명령을 로컬 소스나 BSR 모듈에 적용할 수 있다.

자주 쓰는 전역 플래그는 `--error-format=json`, `--disable-symlinks`, `--config`, `--debug`다. 각 명령의 상세 옵션은 `buf <command> --help`로 확인한다.

<a id="2-build"></a>

## 2. build

`buf build`는 `.proto` 파일을 컴파일해 buf 이미지(FileDescriptorSet)로 만든다. 출력 옵션 없이 컴파일 가능 여부만 확인할 수도 있고, `-o`로 저장해 다른 명령의 입력으로 사용할 수도 있다.

```bash
buf build                          # 컴파일 검증만
buf build -o image.bin             # 바이너리 이미지로 출력
buf build -o image.json            # JSON 이미지로 출력
```

<a id="3-generate"></a>

## 3. generate

`buf generate`는 `buf.gen.yaml`에 지정한 플러그인을 실행해 코드 스텁을 생성한다. 기본 설정을 사용하거나 `--template`으로 설정 파일을 지정할 수 있다. 상세 내용은 `03_generate.md`에서 다룬다.

```bash
buf generate
buf generate --template buf.gen.yaml proto
buf generate buf.build/acme/weather --include-imports
```

<a id="4-lint"></a>

## 4. lint

`buf lint`는 `.proto`를 스타일과 구조 규칙에 따라 검사한다. 검사 결과를 도구에서 처리하려면 `--error-format=json`을 사용한다. 규칙 설정은 `04_lint.md`에서 다룬다.

```bash
buf lint
buf lint --error-format=json
```

<a id="5-breaking"></a>

## 5. breaking

`buf breaking`은 현재 스키마를 `--against`로 지정한 대상과 비교해 호환성이 깨지는 변경을 검출한다. Git 브랜치나 BSR 모듈을 비교 대상으로 사용할 수 있다. 상세 내용은 `05_breaking.md`에서 다룬다.

```bash
buf breaking --against '.git#branch=main'
buf breaking --against 'buf.build/acme/weather'
```

<a id="6-format"></a>

## 6. format

- `.proto`를 표준 스타일로 재포매팅함

```bash
buf format -w                # 파일을 제자리에서 수정
buf format -d                # diff만 출력
buf format --exit-code       # 포매팅 변경이 필요한 파일이 있으면 0이 아닌 종료(CI 검사용)
```

<a id="7-push--export--ls-files"></a>

## 7. push / export / ls-files

```bash
# push: 모듈을 BSR에 게시
buf push
buf push --label v1.2.0

# export: 모듈/입력의 .proto를 디렉터리로 추출
buf export buf.build/connectrpc/eliza --output ./out
buf export . --output ./vendor

# ls-files: 입력에 포함된 .proto 파일 목록 출력
buf ls-files
buf ls-files buf.build/acme/weather
```

<a id="8-dep"></a>

## 8. dep

- BSR 모듈 의존성을 관리함

```bash
buf dep update     # deps 해석 → buf.lock 갱신
buf dep graph      # 의존성 그래프 출력
buf dep prune      # 사용하지 않는 의존성 제거 (v2)
```

<a id="9-registry"></a>

## 9. registry

- BSR 자원과 인증을 다룸

```bash
buf registry login
buf registry logout
buf registry whoami
# 하위: module, plugin, organization 등
```

<a id="10-config"></a>

## 10. config

- 설정 파일을 다룸

```bash
buf config init                  # buf.yaml 생성
buf config migrate               # v1 → v2 마이그레이션
buf config ls-lint-rules         # 활성 lint 규칙 목록
buf config ls-breaking-rules     # 활성 breaking 규칙 목록
```

<a id="11-convert--curl"></a>

## 11. convert / curl

```bash
# convert: 메시지를 binary/text/JSON 간 변환
buf convert proto/acme/weather/v1/weather.proto \
  --type acme.weather.v1.Weather \
  --from data.bin --to -#format=json

# curl: 스키마를 이용해 gRPC/Connect 엔드포인트 호출 (cURL 유사)
buf curl --schema buf.build/connectrpc/eliza \
  --data '{"name":"buf"}' \
  https://demo.connectrpc.com/connectrpc.eliza.v1.ElizaService/Say
```

<a id="12-기타-plugin--beta--lsp"></a>

## 12. 기타 (plugin / beta / lsp)

```bash
buf plugin push      # 체크 플러그인 게시
buf plugin update
buf plugin prune

buf beta ...         # 실험적/불안정 기능 (변경될 수 있음)

buf lsp serve        # 에디터용 language server 실행
```
