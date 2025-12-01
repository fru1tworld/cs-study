# pnpm 설치 (Installation)

## 시스템 요구사항

- **Node.js**: v18.12 이상 (독립 실행형 설치 제외)
- ** 지원 OS**: Windows, macOS, Linux

## 설치 방법

### 1. 독립 실행형 스크립트 (Node.js 불필요)

Node.js 없이 pnpm만 설치하는 방법이다.

**POSIX 시스템 (Linux/macOS):**
```bash
curl -fsSL https://get.pnpm.io/install.sh | sh -
```

**Windows PowerShell:**
```powershell
Invoke-WebRequest https://get.pnpm.io/install.ps1 -UseBasicParsing | Invoke-Expression
```

특정 버전 설치:
```bash
curl -fsSL https://get.pnpm.io/install.sh | env PNPM_VERSION=<version> sh -
```

### 2. Corepack 사용 (Node.js v16.13+)

Corepack은 Node.js에 내장된 패키지 매니저 관리 도구이다.

```bash
# Corepack 활성화
corepack enable pnpm

# 특정 버전 활성화
corepack enable pnpm@<version>
```

### 3. npm을 통한 설치

```bash
npm install -g pnpm
```

### 4. 패키지 매니저별 설치

**Homebrew (macOS):**
```bash
brew install pnpm
```

**Scoop (Windows):**
```bash
scoop install pnpm
```

**Chocolatey (Windows):**
```bash
choco install pnpm
```

**winget (Windows):**
```bash
winget install pnpm
```

**Volta:**
```bash
volta install pnpm
```

## Node.js 버전 호환성

| pnpm 버전 | Node.js 버전 |
|-----------|--------------|
| v10 | 18, 20, 22, 24 |
| v9 | 18, 20, 22 |
| v8 | 16, 18, 20 |

## 업데이트

### 자동 업데이트
```bash
pnpm self-update
```

### 특정 버전으로 업데이트
```bash
pnpm self-update --version <version>
```

## 제거 (Uninstall)

**npm으로 설치한 경우:**
```bash
npm rm -g pnpm
```

** 독립 실행형으로 설치한 경우:**

1. pnpm 홈 디렉터리 삭제:
   ```bash
   rm -rf $PNPM_HOME
   ```

2. 글로벌 콘텐츠 주소 저장소 삭제 (선택사항):
   ```bash
   rm -rf $(pnpm store path)
   ```

## 단축키 설정 (Alias)

빠른 타이핑을 위해 별칭을 설정할 수 있다.

**Bash/Zsh:**
```bash
# ~/.bashrc 또는 ~/.zshrc에 추가
alias pn='pnpm'
```

**PowerShell:**
```powershell
# $PROFILE에 추가
Set-Alias -Name pn -Value pnpm
```

## 문제 해결

### Windows에서 느린 설치

Microsoft Defender가 pnpm 설치 속도를 저하시킬 수 있다.

** 해결책:**
Windows 보안 설정에서 pnpm 저장소 경로를 제외 목록에 추가:
```
%LOCALAPPDATA%\pnpm\store
```

### 손상된 설치 복구

```bash
# 저장소 정리
pnpm store prune

# 강제 재설치
pnpm install --force
```

### PATH 문제

pnpm이 PATH에 없는 경우:

**POSIX:**
```bash
export PNPM_HOME="$HOME/.local/share/pnpm"
export PATH="$PNPM_HOME:$PATH"
```

**Windows PowerShell:**
```powershell
$env:PNPM_HOME = "$env:LOCALAPPDATA\pnpm"
$env:PATH = "$env:PNPM_HOME;$env:PATH"
```

### 설치 확인

```bash
pnpm --version
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `PNPM_HOME` | pnpm CLI가 설치된 디렉터리 | 자동 감지 |
| `XDG_CACHE_HOME` | 캐시 디렉터리 | `~/.cache` |
| `XDG_CONFIG_HOME` | 설정 디렉터리 | `~/.config` |
| `XDG_DATA_HOME` | 데이터 디렉터리 | `~/.local/share` |

---

[← 이전: 소개](./01-introduction.md) | [다음: CLI 명령어 →](./03-cli.md)
