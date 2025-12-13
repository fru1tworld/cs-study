# pnpm 고급 기능 (Advanced)

## Content-addressable Store

pnpm의 핵심 기능으로, 모든 패키지를 글로벌 저장소에 한 번만 저장하고 하드 링크로 참조한다.

### 작동 방식

```
글로벌 저장소 (~/.local/share/pnpm/store)
├── v3/
│   └── files/
│       ├── 00/
│       │   └── abc123...  # 파일 내용의 해시값으로 저장
│       ├── 01/
│       │   └── def456...
│       └── ...

프로젝트 A                    프로젝트 B
node_modules/                 node_modules/
└── .pnpm/                    └── .pnpm/
    └── lodash@4.17.21/           └── lodash@4.17.21/
        └── [하드링크] ─────────────────┘───→ 글로벌 저장소의 동일 파일
```

### 저장소 명령어

```bash
# 저장소 경로 확인
pnpm store path

# 저장소 상태 확인
pnpm store status

# 사용하지 않는 패키지 정리
pnpm store prune

# 특정 패키지 저장소에 추가
pnpm store add lodash@4.17.21
```

### 커스텀 저장소 위치

```ini
# .npmrc
store-dir=/path/to/custom/store
```

### 글로벌 가상 저장소

프로젝트 간 `node_modules/.pnpm` 공유:

```ini
# .npmrc
enable-global-virtual-store=true
virtual-store-dir=/path/to/global/.pnpm
```

## Peer Dependencies 처리

pnpm은 peer dependencies를 엄격하게 관리한다.

### 자동 설치

```ini
# .npmrc
auto-install-peers=true
```

### 버전 충돌 해결

여러 패키지가 서로 다른 버전의 peer dependency를 요구할 때:

```json
// package.json
{
  "pnpm": {
    "peerDependencyRules": {
      // 누락된 peer dependency 무시
      "ignoreMissing": [
        "@babel/*",
        "webpack"
      ],

      // 버전 범위 허용
      "allowedVersions": {
        "react": "17 || 18",
        "vue": "2 || 3"
      },

      // 특정 경고 무시
      "allowAny": ["eslint"]
    }
  }
}
```

### injected 모드

워크스페이스 패키지가 서로 다른 peer dependency 버전을 사용해야 할 때:

```json
// apps/web/package.json
{
  "dependencies": {
    "@my-org/ui": "workspace:*",
    "react": "^18.0.0"
  },
  "dependenciesMeta": {
    "@my-org/ui": {
      "injected": true
    }
  }
}
```

```json
// apps/legacy/package.json
{
  "dependencies": {
    "@my-org/ui": "workspace:*",
    "react": "^17.0.0"
  },
  "dependenciesMeta": {
    "@my-org/ui": {
      "injected": true
    }
  }
}
```

이렇게 하면 각 앱에서 `@my-org/ui`가 해당 앱의 React 버전으로 해결된다.

## 패키지 패칭 (Patching)

외부 패키지의 버그를 수정하거나 기능을 추가할 수 있다.

### 패치 생성

```bash
# 패치 모드 시작
pnpm patch lodash@4.17.21

# 에디터가 열리고, 수정 후 저장
# 수정이 끝나면:
pnpm patch-commit <tmp-dir>
```

### 패치 적용

패치는 자동으로 적용되며, 정보는 `package.json`에 저장된다:

```json
{
  "pnpm": {
    "patchedDependencies": {
      "lodash@4.17.21": "patches/lodash@4.17.21.patch"
    }
  }
}
```

### 패치 파일 구조

```
my-project/
├── patches/
│   └── lodash@4.17.21.patch
├── package.json
└── pnpm-lock.yaml
```

### 패치 제거

```bash
pnpm patch-remove lodash@4.17.21
```

## Side-effects 캐시

postinstall 스크립트 결과를 캐시하여 재설치 시간을 단축한다.

### 설정

```ini
# .npmrc
side-effects-cache=true
```

### 작동 방식

1. 패키지 설치 시 postinstall 스크립트 실행
2. 결과물(컴파일된 바이너리 등)을 저장소에 캐시
3. 다음 설치 시 캐시된 결과물 사용

### 장점

- **esbuild**, **@swc/core** 같은 네이티브 패키지 재설치 시간 단축
- CI에서 특히 효과적

## 보안 기능

### 빌드 스크립트 제한

```ini
# .npmrc
# postinstall 실행 허용 패키지만 명시
onlyBuiltDependencies[]=esbuild
onlyBuiltDependencies[]=@swc/core
onlyBuiltDependencies[]=sharp
```

또는:

```json
// package.json
{
  "pnpm": {
    "onlyBuiltDependencies": ["esbuild", "@swc/core"],
    "neverBuiltDependencies": ["fsevents"]
  }
}
```

### 신뢰 정책

```ini
# .npmrc
# 무결성 검사 강제
trustPolicy=require-integrity
```

### 최소 릴리스 기간

신규 퍼블리시된 패키지 설치를 지연시켜 공급망 공격 방지:

```ini
# .npmrc
minimum-release-age=7d
```

### 스크립트 실행 전 의존성 확인

```ini
# .npmrc
verify-deps-before-run=true
```

락파일과 실제 설치된 의존성이 일치하는지 확인 후 스크립트 실행.

## 버전 덮어쓰기 (Overrides)

의존성 트리의 특정 패키지 버전을 강제로 변경한다.

### 기본 사용

```json
{
  "pnpm": {
    "overrides": {
      // 모든 lodash를 특정 버전으로
      "lodash": "4.17.21",

      // 특정 경로의 패키지만
      "foo@^1.0.0>bar": "2.0.0",

      // 모든 하위 의존성
      "baz@*": "3.0.0"
    }
  }
}
```

### 보안 취약점 해결

```json
{
  "pnpm": {
    "overrides": {
      "vulnerable-package": "^1.2.3-fixed"
    }
  }
}
```

## 패키지 확장 (Package Extensions)

불완전한 패키지 정의를 보완한다.

```json
{
  "pnpm": {
    "packageExtensions": {
      "some-package": {
        "peerDependencies": {
          "react": "*"
        }
      },
      "another-package": {
        "dependencies": {
          "missing-dep": "^1.0.0"
        }
      }
    }
  }
}
```

## Node.js 버전 관리

pnpm이 직접 Node.js를 관리할 수 있다.

### 사용법

```bash
# Node.js 버전 설치 및 사용
pnpm env use --global 18
pnpm env use --global lts

# 설치된 버전 목록
pnpm env list

# 특정 버전 제거
pnpm env remove --global 16
```

### 프로젝트별 Node.js 버전

```json
// package.json
{
  "engines": {
    "runtime": {
      "node": "20.10.0"
    }
  }
}
```

## 성능 최적화

### 병렬 다운로드

```ini
# .npmrc
fetch-jobs=4
```

### 네트워크 동시 연결

```ini
# .npmrc
network-concurrency=16
```

### 오프라인 모드

```bash
# 캐시된 패키지만 사용
pnpm install --offline

# 캐시 우선, 없으면 다운로드
pnpm install --prefer-offline
```

### 선택적 의존성 건너뛰기

```bash
# optional dependencies 제외
pnpm install --no-optional
```

## 디버깅

### 상세 로깅

```bash
# 디버그 로그 활성화
pnpm install --loglevel=debug
```

### 의존성 분석

```bash
# 왜 이 패키지가 설치됐는지
pnpm why lodash

# 의존성 트리 확인
pnpm list --depth=10

# JSON 형식으로 출력
pnpm list --json
```

### 락파일 검증

```bash
# 락파일 무결성 확인
pnpm install --frozen-lockfile

# 락파일 수정 없이 확인만
pnpm install --lockfile-only
```

## 마이그레이션

### npm에서 pnpm으로

```bash
# 기존 node_modules 삭제
rm -rf node_modules

# package-lock.json을 pnpm-lock.yaml로 변환
pnpm import

# 의존성 설치
pnpm install

# 기존 락파일 삭제 (선택사항)
rm package-lock.json
```

### Yarn에서 pnpm으로

```bash
rm -rf node_modules
pnpm import  # yarn.lock 자동 감지
pnpm install
rm yarn.lock
```

## 라이선스 관리

```bash
# 모든 의존성의 라이선스 목록
pnpm licenses list

# JSON 형식으로 출력
pnpm licenses list --json

# 특정 라이선스만 필터링
pnpm licenses list --json | jq '.[] | select(.license == "MIT")'
```

---

[← 이전: 워크스페이스](./05-workspaces.md) | [목차로 돌아가기](./index.md)
