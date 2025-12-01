# pnpm 워크스페이스 (Workspaces)

## 모노레포(Monorepo)란?

모노레포는 여러 프로젝트를 하나의 저장소에서 관리하는 방식이다.

```
my-monorepo/
├── pnpm-workspace.yaml    # 워크스페이스 정의
├── package.json           # 루트 패키지
├── packages/
│   ├── shared/            # 공유 라이브러리
│   │   └── package.json
│   ├── utils/             # 유틸리티
│   │   └── package.json
│   └── core/              # 코어 라이브러리
│       └── package.json
└── apps/
    ├── web/               # 웹 앱
    │   └── package.json
    └── mobile/            # 모바일 앱
        └── package.json
```

### 모노레포의 장점

- **코드 공유**: 패키지 간 쉬운 코드 재사용
- **일관성**: 동일한 도구/설정/버전 관리
- **원자적 변경**: 여러 패키지에 대한 단일 커밋
- **간단한 의존성**: 내부 패키지 간 연결 용이

## 워크스페이스 설정

### 1. pnpm-workspace.yaml 생성

```yaml
# pnpm-workspace.yaml
packages:
  - 'packages/*'      # packages 디렉터리의 모든 패키지
  - 'apps/*'          # apps 디렉터리의 모든 앱
  - '!**/test/**'     # test 디렉터리 제외
```

### 2. 루트 package.json

```json
{
  "name": "my-monorepo",
  "private": true,
  "scripts": {
    "build": "pnpm -r build",
    "test": "pnpm -r test",
    "dev": "pnpm --filter @my-org/web dev"
  },
  "devDependencies": {
    "typescript": "^5.0.0"
  }
}
```

### 3. 개별 패키지 설정

```json
// packages/shared/package.json
{
  "name": "@my-org/shared",
  "version": "1.0.0",
  "main": "./dist/index.js",
  "types": "./dist/index.d.ts"
}
```

```json
// apps/web/package.json
{
  "name": "@my-org/web",
  "version": "1.0.0",
  "dependencies": {
    "@my-org/shared": "workspace:*"
  }
}
```

## workspace 프로토콜

워크스페이스 내 패키지를 참조할 때 사용한다.

### 기본 문법

```json
{
  "dependencies": {
    // 모든 버전 허용
    "foo": "workspace:*",

    // 틸드 범위 (1.5.x)
    "bar": "workspace:~",

    // 캐럿 범위 (^1.5.0)
    "baz": "workspace:^",

    // 특정 버전
    "qux": "workspace:^1.5.0",

    // 상대 경로
    "quux": "workspace:../utils",

    // 별칭 (다른 이름으로 참조)
    "lodash-v4": "workspace:lodash@*"
  }
}
```

### 퍼블리시 시 변환

workspace 프로토콜은 퍼블리시할 때 자동으로 일반 버전으로 변환된다.

```json
// 개발 중 (package.json)
{
  "dependencies": {
    "@my-org/shared": "workspace:^1.0.0"
  }
}

// 퍼블리시 후 (npm에 업로드된 버전)
{
  "dependencies": {
    "@my-org/shared": "^1.0.0"
  }
}
```

### workspace 프로토콜 강제

```bash
# 워크스페이스 패키지로만 해결 강제
pnpm add @my-org/shared --workspace
```

## 필터링 (Filtering)

특정 패키지에 대해서만 명령을 실행한다.

### 기본 필터링

```bash
# 이름으로 필터링
pnpm --filter @my-org/web build

# 와일드카드 패턴
pnpm --filter "@my-org/*" build
pnpm --filter "*utils*" test

# 디렉터리로 필터링
pnpm --filter "./packages/**" build
```

### 의존성 기반 필터링

```bash
# 패키지와 그 의존성 모두
pnpm --filter @my-org/web... build

# 의존성만 (패키지 제외)
pnpm --filter "@my-org/web^..." build

# 패키지와 그것에 의존하는 모든 패키지
pnpm --filter ...@my-org/shared build

# 의존 패키지만 (원본 제외)
pnpm --filter "...^@my-org/shared" build
```

### Git 기반 필터링

```bash
# main 브랜치 이후 변경된 패키지
pnpm --filter "[origin/main]" build

# 변경된 패키지와 그에 의존하는 패키지들
pnpm --filter "...[origin/main]" build

# 최근 커밋에서 변경된 패키지
pnpm --filter "[HEAD~1]" test
```

### 복합 필터링

```bash
# 여러 필터 결합 (OR 조건)
pnpm --filter @my-org/web --filter @my-org/api build

# 제외 필터
pnpm --filter "@my-org/*" --filter "!@my-org/docs" build

# 디렉터리 + 의존성
pnpm --filter "./apps/**..." build
```

### 프로덕션 의존성 필터

```bash
# devDependencies 제외하고 의존성 추적
pnpm --filter-prod @my-org/web... build
```

### 테스트 패턴

테스트 파일만 변경된 경우 의존 패키지 실행 방지:

```bash
pnpm --filter "...[origin/main]" \
     --test-pattern="**/*.test.ts" \
     build
```

## 워크스페이스 명령어

### 재귀 실행

```bash
# 모든 패키지에서 스크립트 실행
pnpm -r build
pnpm --recursive build

# 스크립트가 없으면 건너뛰기
pnpm -r --if-present build

# 병렬 실행
pnpm -r --parallel build

# 스트리밍 출력
pnpm -r --stream build
```

### 루트에서 실행

```bash
# 워크스페이스 루트에서 명령 실행
pnpm -w add typescript -D

# 루트의 스크립트 실행
pnpm -w run lint
```

### 특정 패키지에서 실행

```bash
# exec로 명령 실행
pnpm --filter @my-org/web exec -- jest

# 환경 변수 접근
pnpm -r exec -- echo $PNPM_PACKAGE_NAME
```

## 워크스페이스 의존성 관리

### 패키지 추가

```bash
# 특정 패키지에 의존성 추가
pnpm --filter @my-org/web add lodash

# 워크스페이스 패키지 의존성 추가
pnpm --filter @my-org/web add @my-org/shared

# 루트에 개발 의존성 추가 (공통 도구)
pnpm -w add typescript -D
```

### 패키지 제거

```bash
# 특정 패키지에서 의존성 제거
pnpm --filter @my-org/web remove lodash

# 모든 패키지에서 제거
pnpm -r remove lodash
```

### 의존성 업데이트

```bash
# 모든 워크스페이스 패키지 업데이트
pnpm -r update

# 특정 패키지만 업데이트
pnpm --filter @my-org/web update lodash
```

## 릴리스 관리

pnpm은 버전 관리 도구를 내장하지 않는다. 다음 도구들과 함께 사용한다.

### Changesets

가장 인기 있는 모노레포 버전 관리 도구이다.

```bash
# 설치
pnpm add -Dw @changesets/cli

# 초기화
pnpm changeset init

# 변경사항 추가
pnpm changeset

# 버전 업데이트
pnpm changeset version

# 퍼블리시
pnpm changeset publish
```

### 기타 도구

- **Rush**: Microsoft의 모노레포 관리 프레임워크
- **Lerna**: 클래식한 모노레포 도구 (pnpm과 호환)

## 실용적인 예시

### 공통 설정 공유

```json
// packages/eslint-config/package.json
{
  "name": "@my-org/eslint-config",
  "main": "index.js"
}
```

```json
// apps/web/package.json
{
  "devDependencies": {
    "@my-org/eslint-config": "workspace:*"
  },
  "eslintConfig": {
    "extends": "@my-org/eslint-config"
  }
}
```

### TypeScript 프로젝트 참조

```json
// tsconfig.json (루트)
{
  "references": [
    { "path": "packages/shared" },
    { "path": "packages/utils" },
    { "path": "apps/web" }
  ]
}
```

```json
// apps/web/tsconfig.json
{
  "compilerOptions": {
    "composite": true
  },
  "references": [
    { "path": "../../packages/shared" }
  ]
}
```

### CI/CD에서의 필터링

```yaml
# GitHub Actions 예시
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # 전체 히스토리 필요

      - uses: pnpm/action-setup@v2

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Build changed packages
        run: pnpm --filter "...[origin/main]" build

      - name: Test changed packages
        run: pnpm --filter "...[origin/main]" test
```

---

[← 이전: 설정](./04-configuration.md) | [다음: 고급 기능 →](./06-advanced.md)
