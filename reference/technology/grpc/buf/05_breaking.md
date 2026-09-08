# 호환성 깨짐 검출 (buf breaking)

> 원본: https://buf.build/docs/breaking/

<a id="1-buf-breaking-개요"></a>
## 1. buf breaking 개요

필드 타입을 `int32`에서 `string`으로 바꾸면 wire 타입도 달라져 기존 메시지와 호환되지 않는다. `buf breaking`은 현재 스키마를 이전 버전과 비교해 이런 변경이 클라이언트나 서버, 생성 코드의 호환성을 깨뜨리는지 검사한다.

```bash
buf breaking --against <과거-버전-입력>
```

<a id="2---against-비교-대상-지정"></a>

## 2. --against 비교 대상 지정

비교할 이전 버전은 `--against`로 지정한다. 로컬 Git 이력뿐 아니라 원격 저장소, BSR 모듈, 미리 빌드한 이미지도 비교 대상으로 사용할 수 있다.

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

CI에서는 보통 머지 대상 브랜치(`main`)와 비교해, PR을 병합했을 때 호환성이 깨지는지 확인한다.

<a id="3-호환성-카테고리-file--package--wire_json--wire"></a>

## 3. 호환성 카테고리 (FILE / PACKAGE / WIRE_JSON / WIRE)

어디까지 호환성을 유지해야 하는지에 따라 검사 범위를 정한다. 규칙은 가장 엄격한 `FILE`부터 가장 관대한 `WIRE`까지 네 개의 중첩된 카테고리로 나뉜다.

- FILE(기본값)
  - 범위: 파일별 생성 코드 깨짐
  - 용도: 파일 단위 import가 필요한 언어(C++, Python 등), 가장 엄격
- PACKAGE
  - 범위: 패키지별 생성 코드 깨짐
  - 용도: 더 관대하며 같은 패키지 내 정의 이동 허용
- WIRE_JSON
  - 범위: 바이너리 wire + JSON 인코딩
  - 용도: wire 및 JSON 수준 비호환 검출
- WIRE
  - 범위: 바이너리 wire 형식만
  - 용도: 가장 관대하며 JSON 변경 허용

더 엄격한 카테고리를 통과하면 그보다 관대한 카테고리도 통과한다. 예를 들어 `FILE`을 통과하면 `PACKAGE`, `WIRE_JSON`, `WIRE`도 통과한다. 소비자에게 생성 코드의 호환성이 필요하다면 FILE/PACKAGE를, 직렬화 호환성만 중요하다면 WIRE/WIRE_JSON을 기준으로 선택한다.

<a id="4-설정-use--except--ignore"></a>

## 4. 설정 (use / except / ignore)

선택한 카테고리와 예외는 `buf.yaml`의 `breaking` 섹션에 작성한다.

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

v2에서 모듈별로 다른 정책을 적용하려면 `modules[].breaking`에 작성한다. 이 설정은 워크스페이스 수준 설정을 완전히 대체하므로 모듈에 필요한 정책을 모두 포함해야 한다.

사용할 수 있는 규칙은 다음 명령으로 확인한다.

```bash
buf config ls-breaking-rules
buf config ls-breaking-rules --version v2
```

<a id="5-실행-위치-로컬--ci--bsr"></a>

## 5. 실행 위치 (로컬 / CI / BSR)

호환성 검사는 개발 중인 로컬 환경, PR을 검사하는 CI/CD, 스키마를 배포하는 BSR에서 수행할 수 있다.

- 1\. 로컬: 개발 중 CLI로 직접 실행
- 2\. CI/CD: GitHub Actions 등 파이프라인에서 PR마다 `--against`로 비교
- 3\. BSR: 레지스트리에서 push 시 breaking 정책을 강제 → 호환성이 깨진 push를 소비자에게 전달되기 전에 거부

- GitHub Actions 예시(개념):

```yaml
- run: buf breaking --against 'https://github.com/${{ github.repository }}.git#branch=main'
```
