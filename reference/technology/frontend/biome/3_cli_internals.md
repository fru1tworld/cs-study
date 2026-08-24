# Biome CLI 레퍼런스와 내부 구조

## CLI 레퍼런스

> 원문: https://biomejs.dev/reference/cli

### 핵심 명령

- `biome check`: 포매팅, 린팅, 어시스트를 한 번에 실행. 가장 포괄적인 명령
- `biome format`: 포매팅만 실행
- `biome lint`: 린팅만 실행
- `biome ci`: CI 환경용 검사. 파일을 수정하지 않음

### 유틸리티 명령

- `biome init`: 기본 설정 파일 생성
- `biome version`: CLI 버전과 데몬 서버 정보 표시
- `biome upgrade`: 최신 안정 버전으로 업데이트
- `biome rage`: 트러블슈팅 리포트 생성(시스템 정보, 설정, 로그 포함)
- `biome explain <rule>`: 특정 린트 규칙 문서 표시
- `biome search`: GritQL 패턴으로 코드 검색(실험적)
- `biome migrate`: 설정 업데이트 미리보기 및 적용

### 데몬 명령

- `biome start`: 데몬 서버 시작
- `biome stop`: 데몬 서버 종료
- `biome lsp-proxy`: LSP 메시지를 에디터와 데몬 사이에 전달
- `biome clean`: 데몬 서버 로그 파일 삭제

### 전역 옵션

#### 진단 및 출력 제어

- `--colors=<off|force>`: ANSI 스타일링 제어
- `--verbose`: 추가 진단 정보 및 처리 파일 목록 표시
- `--config-path=PATH`: 설정 파일 경로 지정
- `--max-diagnostics=<none|NUMBER>`: 표시할 진단 최대 수(기본값: 20)
- `--skip-parse-errors`: 구문 오류가 있는 파일 건너뛰기
- `--no-errors-on-unmatched`: 매칭 파일 없을 때 오류 억제
- `--error-on-warnings`: 경고 발생 시 오류 상태로 종료
- `--diagnostic-level=<info|warn|error>`: 최소 심각도 임계값

#### 리포터

`--reporter=<format>` 옵션으로 출력 형식을 변경함.

- `default`: 기본 형식
- `concise`: 간결한 형식
- `summary`: 요약 형식
- `json` / `json-pretty`: JSON 형식
- `github` / `gitlab`: CI 플랫폼별 형식
- `junit` / `checkstyle`: XML 기반 형식
- `sarif` / `rdjson`: 정적 분석 교환 형식

`--reporter-file=PATH`로 리포터 출력을 파일에 기록할 수 있음.

#### 로깅

- `--log-level=<none|tracing|debug|info|warn|error>`: 로깅 수준
- `--log-kind=<pretty|compact|json>`: 로그 형식
- `--log-file=PATH`: 로그를 파일에 기록
- `--log-path=PATH`: 로그 파일 디렉토리
- `--log-prefix-name=STRING`: 회전 로그 파일 접두사(기본값: `server.log`)

### 포매터 CLI 옵션

#### 언어 공통

- `--indent-style=<tab|space>`: 들여쓰기 스타일(기본값: `tab`)
- `--indent-width=NUMBER`: 들여쓰기 너비(기본값: `2`)
- `--line-width=NUMBER`: 최대 줄 너비(기본값: `80`)
- `--line-ending=<lf|crlf|cr|auto>`: 줄 끝 스타일(기본값: `lf`)
- `--trailing-newline=<true|false>`: 파일 끝 줄바꿈 추가(기본값: `true`)
- `--bracket-spacing=<true|false>`: 객체 리터럴 내 공백(기본값: `true`)
- `--expand=<auto|always|never>`: 배열/객체 포매팅 동작(기본값: `auto`)
- `--attribute-position=<multiline|auto>`: HTML 속성 위치(기본값: `auto`)
- `--bracket-same-line=<true|false>`: 닫는 괄호 위치(기본값: `false`)
- `--use-editorconfig=<true|false>`: `.editorconfig` 사용(기본값: `false`)

#### JavaScript 전용

- `--semicolons=<always|as-needed>`: 세미콜론(기본값: `always`)
- `--trailing-commas=<all|es5|none>`: 후행 쉼표(기본값: `all`)
- `--quote-properties=<preserve|as-needed>`: 속성 따옴표
- `--jsx-quote-style=<double|single>`: JSX 따옴표(기본값: `double`)
- `--arrow-parentheses=<always|as-needed>`: 화살표 함수 괄호(기본값: `always`)

#### JSON 전용

- `--json-formatter-enabled=<true|false>`: JSON 포매터 활성화
- `--json-parse-allow-comments=<true|false>`: JSON 주석 허용
- `--json-parse-allow-trailing-commas=<true|false>`: 후행 쉼표 허용
- `--json-formatter-trailing-commas=<none|all>`: 후행 쉼표 동작(기본값: `none`)

#### CSS 전용

- `--css-formatter-enabled=<true|false>`: CSS 포매터 활성화
- `--css-parse-css-modules=<true|false>`: CSS Modules 기능 활성화
- `--css-parse-tailwind-directives=<true|false>`: Tailwind CSS 4.0 지시자 활성화
- `--css-formatter-quote-style=<double|single>`: CSS 따옴표(기본값: `double`)

#### HTML 전용

- `--html-formatter-enabled=<true|false>`: HTML 포매터 활성화
- `--html-formatter-whitespace-sensitivity=<css|strict|ignore>`: 공백 처리(기본값: `css`)
- `--html-formatter-indent-script-and-style=<true|false>`: script/style 태그 들여쓰기(기본값: `false`)
- `--html-formatter-self-close-void-elements=<always|never>`: 빈 요소 자체 닫기(기본값: `never`)

#### GraphQL 전용

- `--graphql-formatter-enabled=<true|false>`: GraphQL 포매터 활성화
- `--graphql-formatter-quote-style=<double|single>`: GraphQL 따옴표(기본값: `double`)

### 린터 CLI 옵션

- `--linter-enabled=<true|false>`: 린팅 활성화
- `--only=<GROUP|RULE|DOMAIN>`: 지정 규칙/그룹만 실행
- `--skip=<GROUP|RULE|DOMAIN>`: 지정 규칙/그룹 건너뛰기
- `--profile-rules`: 규칙별 실행 시간 출력

린트 전용:

- `--suppress`: 수정 대신 억제 주석 삽입
- `--reason=STRING`: 억제 주석에 사유 추가

### 파일 처리 옵션

- `--files-max-size=NUMBER`: 최대 파일 크기(기본값: 1 MiB)
- `--files-ignore-unknown=<true|false>`: 미인식 파일 무시
- `--stdin-file-path=PATH`: stdin에서 읽되 파일 타입 감지에 PATH 사용

### VCS 변경 감지

- `--staged`: 스테이지된 파일만 처리
- `--changed`: 머지 베이스 이후 변경 파일 처리
- `--since=REF`: 변경 감지 기준 레퍼런스 지정

### 명령별 고유 옵션

#### check / format

- `--write` / `--fix`: 파일에 변경 적용
- `--watch`: 파일 변경 감시 후 재처리
- `--unsafe`: 안전하지 않은 수정 허용
- `--format-with-errors=<true|false>`: 구문 오류 파일도 포매팅

#### check 전용

- `--assist-enabled=<true|false>`: 어시스트 액션 제어
- `--enforce-assist=<true|false>`: 어시스트 액션 적용 강제(기본값: `true`)

#### ci 전용

- `--threads=NUMBER`: CI 스레드 수

#### migrate

- `--write` / `--fix`: 마이그레이션 변경 적용
- `migrate prettier`: Prettier 설정 가져오기
- `migrate eslint`: ESLint 설정 가져오기

### 환경 변수

- `BIOME_CONFIG_PATH`: 설정 파일 경로
- `BIOME_LOG_FILE`: 로그 파일 위치
- `BIOME_LOG_LEVEL`: 로깅 수준
- `BIOME_LOG_KIND`: 로그 형식
- `BIOME_LOG_PREFIX_NAME`: 로그 파일 접두사
- `BIOME_LOG_PATH`: 로그 디렉토리
- `BIOME_THREADS`: 스레드 수
- `BIOME_WATCHER_KIND`: 파일 감시 방식
- `BIOME_WATCHER_POLLING_INTERVAL`: 폴링 간격
- `BIOME_DISTRIBUTION`: 설치 유형(`npm` | `homebrew` | `standalone`)

---

## 내부 구조 (Architecture)

> 원문: https://biomejs.dev/internals/architecture

### 스캐너

파일 시스템을 크롤링하여 프로젝트 메타데이터를 추출하는 세 가지 기능:

- 모노레포에서 중첩된 `biome.json`/`biome.jsonc` 파일 탐색
- `vcs.useIgnoreFile` 활성화 시 중첩된 `.gitignore` 파일 탐색
- 프로젝트 도메인 규칙 활성 시 `package.json` 매니페스트와 소스 파일 인덱싱

프로젝트 규칙이 비활성이면 스캐너는 해당 세션에 필요한 폴더만 대상으로 최적화함. `packages/foo/`에서 `biome check`를 실행하면 레포지토리 루트, `packages/`, `packages/foo/` 및 하위만 탐색하고 인접 폴더는 건너뜀.

### 파서와 CST

rowan의 내부 포크를 사용하여 Green/Red 트리 패턴을 구현함. CST(구체 구문 트리)는 공백, 탭, 주석 등 트리비아(trivia)를 포함해 프로그램 정보를 완전히 보존함.

트리비아 분류:

- 선행 트리비아(leading trivia): 토큰/키워드 앞에 위치(줄바꿈 포함)
- 후행 트리비아(trailing trivia): 토큰 뒤, 다음 줄바꿈 전까지

### 복원력 있는 파서

파서의 두 가지 특성:

- 오류 복원력(resilient): 구문 오류 후에도 파싱을 재개함
- 복구 가능(recoverable): 오류 위치를 이해하고 올바른 정보로 재개함

심각한 구문 오류에는 Bogus 노드를 사용하여 잘못된 코드를 표시함. 소비자가 부정확한 구문을 접하지 않도록 보호함.

### 데몬

Biome는 서버-클라이언트 아키텍처를 사용함. 데몬은 백그라운드에서 동작하는 상주 서버로, 에디터와 CLI의 요청을 처리함.

데몬 관련 명령:

```bash
biome start   # 데몬 서버 시작
biome stop    # 데몬 서버 종료
biome clean   # 데몬 로그 파일 삭제
```

에디터 확장은 `lsp-proxy` 명령을 통해 데몬과 LSP 프로토콜로 통신함.
