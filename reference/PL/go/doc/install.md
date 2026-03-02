# Go 다운로드 및 설치

## 개요
이 문서는 Linux, Mac, Windows 등 다양한 운영체제에서 Go를 다운로드하고 설치하는 방법을 설명합니다.

## 관련 자료
- [Go 설치 관리](/doc/manage-install) - 여러 버전 설치 및 삭제 방법
- [소스에서 Go 설치하기](/doc/install/source) - 소스 코드에서 Go를 빌드하는 방법

---

## Linux 설치

1. **이전 Go 설치 제거**
   ```bash
   $ rm -rf /usr/local/go && tar -C /usr/local -xzf go1.14.3.linux-amd64.tar.gz
   ```
   - root 권한이나 `sudo`가 필요할 수 있습니다
   - **중요:** 기존 /usr/local/go 디렉터리에 압축을 풀지 마세요 (설치가 손상될 수 있습니다)

2. **/usr/local/go/bin을 PATH에 추가**

   `$HOME/.profile` 또는 `/etc/profile`(시스템 전체 설치의 경우)에 다음 줄을 추가합니다:
   ```bash
   export PATH=$PATH:/usr/local/go/bin
   ```
   - 변경 사항은 다음 로그인 시 적용되거나, `source $HOME/.profile`을 실행하면 즉시 적용됩니다

3. **설치 확인**
   ```bash
   $ go version
   ```

4. 설치된 버전이 출력되는지 **확인**합니다

---

## macOS 설치

1. **Go 설치**
   - 다운로드한 패키지 파일을 엽니다
   - 설치 안내에 따릅니다
   - `/usr/local/go`에 설치됩니다
   - 자동으로 `/usr/local/go/bin`을 PATH에 추가합니다
   - **참고:** 변경 사항을 적용하려면 터미널 세션을 다시 시작해야 할 수 있습니다

2. **설치 확인**
   ```bash
   $ go version
   ```

3. 설치된 버전이 출력되는지 **확인**합니다

---

## Windows 설치

1. **Go 설치**
   - MSI 설치 파일을 엽니다
   - 설치 안내에 따릅니다
   - 기본 설치 위치: `Program Files` 또는 `Program Files (x86)`
   - 설치 중 위치를 변경할 수 있습니다
   - 환경 변경 사항을 적용하려면 명령 프롬프트를 닫았다가 다시 엽니다

2. **설치 확인**
   - **시작** 메뉴를 클릭합니다
   - 검색 상자에 `cmd`를 입력하고 **Enter**를 누릅니다
   - 명령 프롬프트에서 다음을 입력합니다:
     ```bash
     $ go version
     ```
   - 설치된 버전이 출력되는지 확인합니다

---

## 다음 단계

설치가 완료되면 **[시작하기 튜토리얼](tutorial/getting-started.md)**을 방문하여 간단한 Go 코드를 작성해 보세요 (약 10분 소요).

---

## 문제 보고

Go 코드나 문서의 버그, 실수 또는 불일치를 발견하면:
- [이슈 트래커](https://github.com/golang/go/issues)에 티켓을 제출하세요
- 새 이슈를 생성하기 전에 기존 이슈를 확인하세요
