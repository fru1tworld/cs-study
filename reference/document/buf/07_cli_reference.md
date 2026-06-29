# CLI 레퍼런스

> 원본: https://buf.build/docs/cli/ , https://buf.build/docs/reference/cli/buf/

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [build](#2-build)
3. [generate](#3-generate)
4. [lint](#4-lint)
5. [breaking](#5-breaking)
6. [format](#6-format)
7. [push / export / ls-files](#7-push--export--ls-files)
8. [dep](#8-dep)
9. [registry](#9-registry)
10. [config](#10-config)
11. [convert / curl](#11-convert--curl)
12. [기타 (plugin / beta / lsp)](#12-기타-plugin--beta--lsp)

---

## 1. 공통 사항

- 대부분 명령은 **입력(input)**을 받습니다: 디렉터리(기본 `.`), Git 저장소, tarball/zip, BSR 모듈, buf 이미지.
- 자주 쓰는 전역 플래그: `--error-format=json`, `--disable-symlinks`, `--config`, `--debug`.
- 각 명령 상세는 `buf <command> --help`로 확인합니다.

---

## 2. build

`.proto` 파일을 컴파일해 buf 이미지(FileDescriptorSet)로 만듭니다. 컴파일만 검증하거나 다른 명령의 입력으로 쓸 이미지를 만들 때 사용합니다.

```bash
buf build                          # 컴파일 검증만
buf build -o image.bin             # 바이너리 이미지로 출력
buf build -o image.json            # JSON 이미지로 출력
```

---

## 3. generate

`buf.gen.yaml`의 플러그인을 실행해 코드 스텁을 생성합니다(상세는 `03_generate.md`).

```bash
buf generate
buf generate --template buf.gen.yaml proto
buf generate buf.build/acme/weather --include-imports
```

---

## 4. lint

`.proto`를 스타일/구조 규칙으로 검사합니다(상세는 `04_lint.md`).

```bash
buf lint
buf lint --error-format=json
```

---

## 5. breaking

스키마를 비교 대상과 비교하여 호환성 깨짐을 검출합니다(상세는 `05_breaking.md`).

```bash
buf breaking --against '.git#branch=main'
buf breaking --against 'buf.build/acme/weather'
```

---

## 6. format

`.proto`를 표준 스타일로 재포매팅합니다.

```bash
buf format -w                # 파일을 제자리에서 수정
buf format -d                # diff만 출력
buf format --exit-code       # 포매팅 변경이 필요한 파일이 있으면 0이 아닌 종료(CI 검사용)
```

---

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

---

## 8. dep

BSR 모듈 의존성을 관리합니다.

```bash
buf dep update     # deps 해석 → buf.lock 갱신
buf dep graph      # 의존성 그래프 출력
buf dep prune      # 사용하지 않는 의존성 제거 (v2)
```

---

## 9. registry

BSR 자원과 인증을 다룹니다.

```bash
buf registry login
buf registry logout
buf registry whoami
# 하위: module, plugin, organization 등
```

---

## 10. config

설정 파일을 다룹니다.

```bash
buf config init                  # buf.yaml 생성
buf config migrate               # v1 → v2 마이그레이션
buf config ls-lint-rules         # 활성 lint 규칙 목록
buf config ls-breaking-rules     # 활성 breaking 규칙 목록
```

---

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

---

## 12. 기타 (plugin / beta / lsp)

```bash
buf plugin push      # 체크 플러그인 게시
buf plugin update
buf plugin prune

buf beta ...         # 실험적/불안정 기능 (변경될 수 있음)

buf lsp serve        # 에디터용 language server 실행
```
