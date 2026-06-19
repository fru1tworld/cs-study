# 호환성 깨짐 검출 (buf breaking)

> 이 문서는 buf breaking의 사용법, 호환성 카테고리, --against 비교 대상 지정을 한국어로 정리한 것입니다.
> 원본: https://buf.build/docs/breaking/

---

## 목차

1. [buf breaking 개요](#1-buf-breaking-개요)
2. [--against 비교 대상 지정](#2---against-비교-대상-지정)
3. [호환성 카테고리 (FILE / PACKAGE / WIRE_JSON / WIRE)](#3-호환성-카테고리-file--package--wire_json--wire)
4. [설정 (use / except / ignore)](#4-설정-use--except--ignore)
5. [실행 위치 (로컬 / CI / BSR)](#5-실행-위치-로컬--ci--bsr)

---

## 1. buf breaking 개요

`buf breaking`은 현재 스키마를 이전 버전과 비교하여 클라이언트/서버/생성 코드를 깨뜨릴 수 있는 변경을 검출합니다. 예를 들어 필드 타입을 `int32`에서 `string`으로 바꾸면 wire 타입이 달라져 wire 호환성이 깨집니다.

```bash
buf breaking --against <과거-버전-입력>
```

---

## 2. --against 비교 대상 지정

`--against` 플래그는 다양한 입력을 받습니다.

```bash
# Git 브랜치/태그와 비교 (가장 흔함)
buf breaking --against '.git#branch=main'
buf breaking --against '.git#tag=v1.0.0'

# 원격 Git 저장소와 비교
buf breaking --against 'https://github.com/acme/protos.git#branch=main,subdir=proto'

# BSR 모듈과 비교
buf breaking --against 'buf.build/acme/weather'

# 사전 빌드된 이미지/tarball과 비교
buf breaking --against image.bin
buf breaking --against archive.tar.gz
```

CI에서는 보통 머지 대상 브랜치(`main`)와 비교합니다.

---

## 3. 호환성 카테고리 (FILE / PACKAGE / WIRE_JSON / WIRE)

호환성 규칙은 가장 엄격한 것부터 가장 관대한 것까지 4개의 중첩된 카테고리로 구성됩니다.

| 카테고리 | 범위 | 용도 |
|----------|------|------|
| **FILE** (기본값) | 파일별 생성 코드 깨짐 | 파일 단위 import가 필요한 언어(C++, Python 등). 가장 엄격 |
| **PACKAGE** | 패키지별 생성 코드 깨짐 | 더 관대 — 같은 패키지 내 정의 이동 허용 |
| **WIRE_JSON** | 바이너리 wire + JSON 인코딩 | wire 및 JSON 수준 비호환 검출 |
| **WIRE** | 바이너리 wire 형식만 | 가장 관대 — JSON 변경 허용 |

핵심 관계: **더 엄격한 카테고리를 통과하면 더 관대한 카테고리도 자동 통과**합니다. 즉 `FILE`을 통과하면 `PACKAGE`, `WIRE_JSON`, `WIRE`도 통과합니다.

어떤 카테고리를 쓸지는 소비자가 코드 생성을 하는지(FILE/PACKAGE), 아니면 직렬화 호환성만 중요한지(WIRE/WIRE_JSON)에 따라 선택합니다.

---

## 4. 설정 (use / except / ignore)

`buf.yaml`의 `breaking` 섹션에서 규칙을 조정합니다.

```yaml
version: v2
breaking:
  use:
    - FILE                 # 기본값
  except:
    - FIELD_SAME_DEFAULT   # 특정 규칙 제외
  ignore:
    - proto/v1alpha1       # 불안정 패키지 무시
  ignore_unstable_packages: true  # alpha/beta/test 패키지 자동 무시
```

v2에서 모듈별로 다른 정책을 쓰려면 `modules[].breaking`에 작성하며, 워크스페이스 설정을 완전히 대체합니다.

규칙 목록은 다음으로 확인합니다.

```bash
buf config ls-breaking-rules
buf config ls-breaking-rules --version v2
```

---

## 5. 실행 위치 (로컬 / CI / BSR)

호환성 검사는 세 시점에서 수행할 수 있습니다.

1. **로컬**: 개발 중 CLI로 실행.
2. **CI/CD**: GitHub Actions 등 파이프라인에서 PR마다 `--against` 비교.
3. **BSR**: 레지스트리 측에서 push 시 breaking 정책을 강제하여, 깨진 push를 소비자에게 도달하기 전에 거부.

GitHub Actions 예시(개념):

```yaml
- run: buf breaking --against 'https://github.com/${{ github.repository }}.git#branch=main'
```
