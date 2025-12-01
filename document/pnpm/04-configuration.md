# pnpm 설정 (Configuration)

pnpm은 여러 곳에서 설정을 읽어온다:
1. 명령줄 인자
2. 환경 변수
3. `pnpm-workspace.yaml`
4. `.npmrc` 파일

## pnpm-workspace.yaml

워크스페이스(모노레포)의 루트에 위치하는 설정 파일이다.

### 기본 구조

```yaml
# pnpm-workspace.yaml

# 워크스페이스에 포함할 패키지 경로
packages:
  - 'packages/*'           # packages 디렉터리의 모든 직접 하위 디렉터리
  - 'components/**'        # components 내 모든 중첩 디렉터리
  - 'apps/web'             # 특정 디렉터리
  - '!**/test/**'          # test 디렉터리 제외
```

### Catalogs (버전 카탈로그)

워크스페이스 전체에서 의존성 버전을 중앙 관리한다.

```yaml
# pnpm-workspace.yaml

# 기본 카탈로그
catalog:
  react: ^18.2.0
  typescript: ^5.0.0

# 여러 카탈로그 정의
catalogs:
  react17:
    react: ^17.0.2
    react-dom: ^17.0.2
  react18:
    react: ^18.2.0
    react-dom: ^18.2.0
```

package.json에서 사용:
```json
{
  "dependencies": {
    "react": "catalog:",
    "typescript": "catalog:"
  }
}
```

특정 카탈로그 지정:
```json
{
  "dependencies": {
    "react": "catalog:react18"
  }
}
```

## .npmrc 설정

프로젝트 루트 또는 홈 디렉터리의 `.npmrc` 파일에서 설정한다.

### 의존성 해결 (Dependency Resolution)

```ini
# 버전 덮어쓰기
overrides.lodash=4.17.21

# 패키지 확장 (불완전한 패키지 정의 보완)
packageExtensions.some-package.peerDependencies.react=*

# 최소 릴리스 기간 (보안 목적)
# 신규 패키지는 지정 기간 이후에만 설치
minimum-release-age=7d
```

### node_modules 관리

```ini
# 링커 전략 (기본: isolated)
# isolated: 심볼릭 링크 사용 (pnpm 기본)
# hoisted: 플랫 구조 (npm 호환)
# pnp: Plug'n'Play (node_modules 없음)
node-linker=isolated

# 가상 저장소 디렉터리 (기본: node_modules/.pnpm)
virtual-store-dir=node_modules/.pnpm

# 글로벌 가상 저장소 사용
enable-global-virtual-store=false

# 모듈 디렉터리 이름 (기본: node_modules)
modules-dir=node_modules

# 심볼릭 링크 대신 하드 링크 사용
symlink=true

# 호이스팅 패턴 (hoisted 모드에서)
hoist-pattern[]=*
public-hoist-pattern[]=*eslint*
public-hoist-pattern[]=*prettier*
```

### Peer Dependencies

```ini
# peer dependencies 자동 설치
auto-install-peers=true

# 엄격한 peer dependencies 검사
strict-peer-dependencies=false

# peer dependency 규칙
peerDependencyRules.ignoreMissing[]=@babel/*
peerDependencyRules.allowedVersions.react=17 || 18
```

### 워크스페이스 설정

```ini
# 워크스페이스 패키지 심볼릭 링크
link-workspace-packages=true

# 공유 락파일 사용
shared-workspace-lockfile=true

# 워크스페이스 프로토콜 자동 저장
save-workspace-protocol=rolling

# 워크스페이스 접두사 설정
save-prefix=^
```

### 빌드 및 보안

```ini
# 빌드 스크립트 실행 허용 패키지
onlyBuiltDependencies[]=esbuild
onlyBuiltDependencies[]=@swc/core

# 스크립트 실행 전 의존성 확인
verify-deps-before-run=true

# 스크립트 무시
ignore-scripts=false

# 신뢰 정책 (degraded trust evidence 차단)
trustPolicy=require-integrity
```

### 레지스트리 및 인증

```ini
# 기본 레지스트리
registry=https://registry.npmjs.org/

# 스코프별 레지스트리
@my-org:registry=https://npm.my-org.com/

# 인증 토큰
//registry.npmjs.org/:_authToken=${NPM_TOKEN}

# 항상 인증 사용
always-auth=false
```

### 기타 설정

```ini
# 저장소 경로
store-dir=~/.local/share/pnpm/store

# 캐시 디렉터리
cache-dir=~/.cache/pnpm

# 락파일 설정
lockfile=true
prefer-frozen-lockfile=true

# 색상 출력
color=auto

# 진행률 표시
reporter=default

# Git 관련
git-checks=true

# 엔진 검사
engine-strict=false

# 저장 설정
save-exact=false
save-prefix=^

# 프로덕션 모드
production=false
```

## package.json 확장 필드

pnpm은 package.json에 추가 필드를 지원한다.

### engines

```json
{
  "engines": {
    "node": ">=18",
    "pnpm": ">=8"
  }
}
```

### engines.runtime (v10.21+)

자동으로 Node.js 버전을 설치한다.

```json
{
  "engines": {
    "runtime": {
      "node": "20.10.0"
    }
  }
}
```

### devEngines.runtime (v10.14+)

개발 환경 런타임 요구사항을 정의한다.

```json
{
  "devEngines": {
    "runtime": {
      "name": "node",
      "version": ">=20.0.0"
    }
  }
}
```

### dependenciesMeta

```json
{
  "dependenciesMeta": {
    "some-package": {
      "injected": true
    }
  }
}
```

- `injected`: 워크스페이스 패키지를 심볼릭 링크 대신 하드 링크로 복사
  - peer dependencies가 다른 버전으로 해결될 수 있을 때 유용

### peerDependenciesMeta

```json
{
  "peerDependencies": {
    "react": "^18.0.0",
    "react-dom": "^18.0.0"
  },
  "peerDependenciesMeta": {
    "react-dom": {
      "optional": true
    }
  }
}
```

### publishConfig

퍼블리시 시 manifest 필드를 덮어쓴다.

```json
{
  "main": "./src/index.ts",
  "publishConfig": {
    "main": "./dist/index.js",
    "types": "./dist/index.d.ts",
    "exports": {
      ".": {
        "import": "./dist/index.mjs",
        "require": "./dist/index.js"
      }
    },
    "directory": "dist"
  }
}
```

### pnpm 전용 필드

```json
{
  "pnpm": {
    "overrides": {
      "lodash": "4.17.21",
      "foo@^1.0.0>bar": "2.0.0"
    },
    "packageExtensions": {
      "some-package": {
        "peerDependencies": {
          "react": "*"
        }
      }
    },
    "peerDependencyRules": {
      "ignoreMissing": ["@babel/*"],
      "allowedVersions": {
        "react": "17 || 18"
      }
    },
    "neverBuiltDependencies": ["fsevents"],
    "onlyBuiltDependencies": ["esbuild"]
  }
}
```

## 환경 변수

설정값은 환경 변수로 대체할 수 있다.

```ini
# .npmrc
registry=https://${REGISTRY_HOST}/

# 기본값 지정
registry=https://${REGISTRY_HOST:-registry.npmjs.org}/
```

### 주요 환경 변수

| 변수 | 설명 |
|------|------|
| `PNPM_HOME` | pnpm 바이너리 디렉터리 |
| `npm_config_<key>` | npm 설정 값 전달 |
| `NPM_TOKEN` | npm 인증 토큰 |
| `CI` | CI 환경 감지 |

## 설정 우선순위

1. 명령줄 인자 (`--registry=...`)
2. 환경 변수 (`npm_config_registry=...`)
3. 프로젝트 `.npmrc`
4. 워크스페이스 루트 `.npmrc`
5. 사용자 홈 `~/.npmrc`
6. 글로벌 `/etc/npmrc`

## 설정 확인

```bash
# 모든 설정 보기
pnpm config list

# 특정 설정 보기
pnpm config get registry

# 설정 변경
pnpm config set store-dir ~/.pnpm-store
```

---

[← 이전: CLI 명령어](./03-cli.md) | [다음: 워크스페이스 →](./05-workspaces.md)
