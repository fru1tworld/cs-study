# pnpm CLI 명령어 (Commands)

## npm 명령어 대응표

| npm | pnpm | 설명 |
|-----|------|------|
| `npm install` | `pnpm install` | 모든 의존성 설치 |
| `npm i <pkg>` | `pnpm add <pkg>` | 패키지 추가 |
| `npm uninstall <pkg>` | `pnpm remove <pkg>` | 패키지 제거 |
| `npm update` | `pnpm update` | 패키지 업데이트 |
| `npm run <script>` | `pnpm <script>` | 스크립트 실행 |
| `npx <cmd>` | `pnpm dlx <cmd>` | 일회성 패키지 실행 |

## 의존성 관리

### pnpm install

모든 의존성을 설치한다.

```bash
# 기본 설치
pnpm install
pnpm i

# 프로덕션 의존성만 설치
pnpm install --prod
pnpm install -P

# 개발 의존성만 설치
pnpm install --dev
pnpm install -D

# 선택적 의존성 제외
pnpm install --no-optional

# 락파일 수정 금지 (CI용)
pnpm install --frozen-lockfile

# 오프라인 설치 (캐시된 패키지만)
pnpm install --offline

# 캐시 우선, 없으면 다운로드
pnpm install --prefer-offline

# 강제 재설치
pnpm install --force

# 락파일만 업데이트
pnpm install --lockfile-only
```

### pnpm add

패키지를 설치하고 package.json에 추가한다.

```bash
# 프로덕션 의존성으로 추가
pnpm add lodash

# 개발 의존성으로 추가
pnpm add -D typescript
pnpm add --save-dev typescript

# 선택적 의존성으로 추가
pnpm add -O fsevents
pnpm add --save-optional fsevents

# peer 의존성으로 추가
pnpm add --save-peer react

# 전역 설치
pnpm add -g typescript

# 특정 버전 설치
pnpm add lodash@4.17.21

# 특정 태그 설치
pnpm add lodash@next

# 버전 범위 지정
pnpm add "lodash@^4.0.0"

# 정확한 버전으로 저장
pnpm add -E lodash
pnpm add --save-exact lodash

# 워크스페이스 패키지만 허용
pnpm add --workspace @my-org/shared

# Git 저장소에서 설치
pnpm add github:user/repo
pnpm add github:user/repo#branch
pnpm add github:user/repo#commit

# 로컬 경로에서 설치
pnpm add ./local-package
pnpm add ../sibling-package

# tarball URL에서 설치
pnpm add https://example.com/package.tar.gz

# JSR 레지스트리에서 설치 (v10.9+)
pnpm add jsr:@hono/hono@4
```

### pnpm remove

패키지를 제거한다.

```bash
# 패키지 제거
pnpm remove lodash
pnpm rm lodash
pnpm uninstall lodash
pnpm un lodash

# 전역 패키지 제거
pnpm remove -g typescript

# 개발 의존성에서만 제거
pnpm remove -D typescript

# 워크스페이스 전체에서 제거
pnpm remove -r lodash
pnpm remove --recursive lodash

# 특정 패키지들에서만 제거
pnpm --filter @my-org/* remove lodash
```

### pnpm update

패키지를 업데이트한다.

```bash
# 모든 패키지 업데이트 (범위 내)
pnpm update
pnpm up

# 최신 버전으로 업데이트 (메이저 포함)
pnpm update --latest
pnpm up -L

# 특정 패키지만 업데이트
pnpm update lodash

# 특정 버전으로 업데이트
pnpm update lodash@4.17.21

# 스코프 패키지 업데이트 (와일드카드)
pnpm update "@babel/*"

# 특정 패키지 제외
pnpm update "!webpack"
pnpm update "@babel/*" "!@babel/core"

# 개발 의존성만 업데이트
pnpm update -D

# 프로덕션 의존성만 업데이트
pnpm update -P

# 전역 패키지 업데이트
pnpm update -g

# 인터랙티브 모드
pnpm update -i
pnpm update --interactive

# 워크스페이스 전체 업데이트
pnpm update -r
```

## 스크립트 실행

### pnpm run

package.json에 정의된 스크립트를 실행한다.

```bash
# 스크립트 실행
pnpm run build
pnpm build  # 'run' 생략 가능

# 여러 스크립트 동시 실행 (정규식)
pnpm run "/^watch:.*/"

# 스크립트가 없어도 에러 없이 진행
pnpm run --if-present build

# 워크스페이스 전체에서 실행
pnpm -r run build
pnpm run -r build

# 병렬 실행 (순서 무시)
pnpm -r --parallel run build

# 스트리밍 출력 (실시간)
pnpm -r --stream run build

# 출력 모아서 표시
pnpm -r --aggregate-output run build

# 특정 패키지에서만 실행
pnpm --filter @my-org/app run build
```

### pnpm exec

node_modules/.bin에 있는 명령어를 실행한다.

```bash
# 로컬 패키지의 바이너리 실행
pnpm exec jest
pnpm jest  # 'exec' 생략 가능

# 워크스페이스 전체에서 실행
pnpm -r exec jest

# 쉘 모드로 실행
pnpm exec -c "echo $PNPM_PACKAGE_NAME"

# 병렬 실행
pnpm -r --parallel exec jest
```

### pnpm dlx

패키지를 설치하지 않고 일회성으로 실행한다.

```bash
# 기본 사용
pnpm dlx create-react-app my-app
pnpx create-react-app my-app  # 별칭

# 특정 버전 사용
pnpm dlx create-vue@next my-app

# 여러 패키지 설치 후 실행
pnpm dlx --package=yo --package=generator-webapp yo webapp

# 쉘 모드
pnpm dlx -c 'echo "hi" | cowsay'

# 조용한 모드 (실행 결과만 출력)
pnpm dlx -s create-react-app my-app
```

## 패키지 링크

### pnpm link

로컬 패키지를 전역으로 링크하거나 프로젝트에 링크한다.

```bash
# 현재 패키지를 전역으로 링크
pnpm link

# 디렉터리의 패키지를 현재 프로젝트에 링크
pnpm link ../my-package

# 전역으로 링크된 패키지를 현재 프로젝트에 링크
pnpm link my-package
```

### pnpm link vs file: 프로토콜

| 특성 | pnpm link | file: 프로토콜 |
|------|-----------|----------------|
| 링크 타입 | 심볼릭 링크 | 하드 링크 |
| 코드 변경 반영 | 즉시 | 즉시 |
| 의존성 자동 설치 | ❌ | ✅ |
| peer 의존성 해결 | 불완전 | ✅ 권장 |

## 기타 유용한 명령어

### 정보 확인

```bash
# pnpm 버전 확인
pnpm --version
pnpm -v

# 패키지 정보 보기
pnpm info lodash
pnpm view lodash

# 의존성 트리 보기
pnpm list
pnpm ls

# 특정 깊이까지만 표시
pnpm list --depth=1

# 프로덕션 의존성만 표시
pnpm list --prod

# 오래된 패키지 확인
pnpm outdated
```

### 저장소 관리

```bash
# 저장소 경로 확인
pnpm store path

# 사용하지 않는 패키지 정리
pnpm store prune

# 저장소 상태 확인
pnpm store status
```

### 기타

```bash
# 왜 이 패키지가 설치됐는지 확인
pnpm why lodash

# 라이선스 목록 확인
pnpm licenses list

# 환경 정보 출력
pnpm env

# 특정 Node.js 버전으로 실행
pnpm env use --global 18

# 설정 확인
pnpm config list
```

## 전역 옵션

모든 명령어에 사용 가능한 옵션들:

```bash
# 다른 디렉터리에서 실행
pnpm -C /path/to/project install

# 워크스페이스 루트에서 실행
pnpm -w install

# 필터링 (워크스페이스)
pnpm --filter <selector> <command>

# 재귀 실행
pnpm -r <command>
```

## npm과의 차이점

pnpm은 ** 엄격한 옵션 검증** 을 수행한다.

```bash
# npm에서는 통과하지만 pnpm에서는 실패
pnpm install --target_arch=x64  # ❌ 알 수 없는 옵션

# 해결 방법 1: 환경 변수 사용
npm_config_target_arch=x64 pnpm install

# 해결 방법 2: --config. 접두사 사용
pnpm install --config.target_arch=x64
```

---

[← 이전: 설치](./02-installation.md) | [다음: 설정 →](./04-configuration.md)
