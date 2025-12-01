# pnpm 소개 (Introduction)

## pnpm이란?

pnpm(Performant npm)은 빠르고 디스크 효율적인 JavaScript/Node.js 패키지 매니저이다. npm과 Yarn의 대안으로 개발되었으며, 특히 모노레포(monorepo) 환경에서 뛰어난 성능을 보여준다.

## 왜 pnpm인가? (동기)

### 1. 디스크 공간 효율성

pnpm은 **content-addressable store**(내용 주소 지정 저장소)를 사용한다.

- 모든 패키지는 글로벌 저장소에 **한 번만** 저장됨
- 프로젝트에서는 **하드 링크**를 통해 패키지 참조
- 동일 패키지의 다른 버전이 필요한 경우, **변경된 파일만** 추가 저장

```
# 예시: 100개 프로젝트에서 lodash 사용 시
npm/yarn: lodash가 100번 저장됨
pnpm: lodash가 1번만 저장되고 100개 프로젝트에서 하드링크로 참조
```

### 2. 설치 속도

pnpm은 3단계를 **병렬**로 처리한다:

1. **Resolving** (의존성 해석)
2. **Fetching** (패키지 다운로드)
3. **Linking** (심볼릭 링크 생성)

npm/yarn은 이 단계를 순차적으로 처리하지만, pnpm은 병렬 처리로 설치 속도가 크게 향상된다.

### 3. 엄격한 의존성 관리

npm의 **flat node_modules** 문제:
```
# npm은 모든 패키지를 node_modules 최상위에 호이스팅
node_modules/
├── package-a (직접 의존성)
├── package-b (package-a의 의존성인데 최상위로 호이스팅됨)
└── package-c (package-b의 의존성인데 최상위로 호이스팅됨)
```

이로 인한 문제:
- **Phantom dependencies**: package.json에 선언하지 않은 패키지 사용 가능
- **의존성 도플갱어**: 같은 패키지의 여러 버전이 중복 설치

pnpm의 해결책:
```
# pnpm은 심볼릭 링크로 올바른 의존성 구조 유지
node_modules/
├── .pnpm/                    # 실제 패키지들이 저장되는 가상 저장소
│   ├── package-a@1.0.0/
│   │   └── node_modules/
│   │       ├── package-a -> <store>/package-a
│   │       └── package-b -> ../../package-b@1.0.0/node_modules/package-b
│   └── package-b@1.0.0/
│       └── node_modules/
│           └── package-b -> <store>/package-b
└── package-a -> .pnpm/package-a@1.0.0/node_modules/package-a  # 심볼릭 링크
```

**결과**: 직접 선언한 의존성만 접근 가능, 암묵적 의존성 사용 방지

## npm/Yarn과의 비교

| 기능 | pnpm | npm | Yarn |
|------|------|-----|------|
| 디스크 효율성 | ⭐ 최고 (하드링크) | 보통 | 보통 |
| 설치 속도 | ⭐ 빠름 | 느림 | 빠름 |
| 엄격한 의존성 | ⭐ 기본 | ❌ | ❌ |
| 모노레포 지원 | ⭐ 네이티브 | 제한적 | 네이티브 |
| Node.js 버전 관리 | ⭐ 지원 | ❌ | ❌ |
| Side-effects 캐시 | ⭐ 지원 | ❌ | ❌ |
| Plug'n'Play | ❌ | ❌ | ⭐ 지원 |

### pnpm만의 고유 기능

1. **Node.js 버전 관리**: pnpm이 직접 Node.js 버전 설치/관리
2. **Side-effects 캐시**: postinstall 스크립트 결과 캐싱
3. **Catalogs**: 워크스페이스 전체에서 버전 일관성 유지
4. **의존성 패칭**: 외부 패키지 수정 지원 (`pnpm patch`)

## 핵심 특징 요약

### Content-addressable Store
```bash
# 글로벌 저장소 위치 (기본값)
~/.local/share/pnpm/store  # Linux
~/Library/pnpm/store       # macOS
%LOCALAPPDATA%/pnpm/store  # Windows
```

### 심볼릭 링크 구조
- `node_modules/.pnpm`: 가상 저장소 (virtual store)
- 직접 의존성만 `node_modules` 최상위에 심볼릭 링크

### 빠른 설치
- 캐시된 패키지는 네트워크 요청 없이 바로 링크
- 새 패키지만 다운로드

### 모노레포 최적화
- 워크스페이스 간 패키지 공유
- 효율적인 필터링으로 선택적 빌드/테스트

## pnpm을 사용하는 주요 프로젝트

- **Vue.js**
- **Vite**
- **Nuxt**
- **Next.js**
- **Material UI**
- **Prisma**

---

[다음: 설치 →](./02-installation.md)
