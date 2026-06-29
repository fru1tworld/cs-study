# 개발 환경 설정

> 원본: https://netty.io/wiki/setting-up-development-environment.html

---

> **튜토리얼을 찾고 계신가요?** [문서 홈](https://netty.io/wiki/index.html)을 방문하세요. **질문이 있으신가요?** [StackOverflow.com](https://stackoverflow.com/questions/tagged/netty)에 질문하세요.
>
> 이 가이드는 'user guide'가 **아닙니다**. Netty를 사용해 애플리케이션을 만드는 사용자가 아니라, Netty 자체를 개발하고 기여하려는 개발자를 위한 문서입니다.

## 64비트 운영체제 사용

64비트 운영체제를 사용해야 합니다.

## 필요한 빌드 도구 설치

머신에는 다음이 설치되어 있어야 합니다.

- [AdoptOpenJDK](https://adoptopenjdk.net) 또는 [Azul Systems](https://www.azul.com/downloads/zulu-community/) 같은 벤더의 64비트 OpenJDK 8 이상
- [Apache Maven 3.1.1](http://maven.apache.org/) 이상
- [Git](http://git-scm.com/)

Linux를 사용하는 경우 다음 패키지들을 설치해야 합니다.

```sh
# yum install lsb-core autoconf automake libtool make tar \
              glibc-devel libaio-devel openssl-devel apr-devel \
              lksctp-tools

# apt-get install autoconf automake libtool make tar \
                  libaio-dev libssl-dev libapr1-dev \
                  lksctp-tools cmake ninja-build
```

`rustup` 등을 사용해 Rust 툴체인도 설치해야 합니다.

macOS를 사용하는 경우:

```sh
brew install autoconf automake libtool cmake ninja rustup openssl
rustup default stable
```

git 설정도 필요합니다.

Windows를 사용하는 경우, 체크아웃 시 LF가 자동으로 CRLF로 변환되도록 설정합니다.

```sh
git config --global core.autocrlf true
```

macOS를 사용하는 경우, 커밋 시 CRLF가 자동으로 LF로 변환되도록 설정합니다.

```sh
git config --global core.autocrlf input
```

추가 정보는 [Building The Native Transports](./09_native_transports.md)도 참고하세요.

netty-tcnative(특히 BoringSSL 부분)를 빌드하려면 `cmake`, `ninja-build`, `golang` 패키지도 설치하세요. Fedora/CentOS의 `ninja-build`와 `golang`은 [Fedora EPEL](https://fedoraproject.org/wiki/EPEL) 저장소에서 받을 수 있습니다.

## 빌드 검증

코드를 체크아웃한 뒤 `./mvnw install -DskipTests -T1C`를 실행해 빌드 환경이 정상 동작하는지 확인합니다. 이 명령은 로컬 Maven 캐시도 함께 채워주므로, 이후 개별 모듈을 빌드할 때 도움이 됩니다.

## IntelliJ IDEA 설정

Netty 프로젝트 팀은 [IntelliJ IDEA](http://www.jetbrains.com/idea/)를 주 IDE로 사용합니다. 우리의 코딩 스타일을 따르기만 한다면 다른 개발 환경을 써도 무방합니다.

### OS와 동일한 비트 수의 IDE 사용

64비트 운영체제를 사용하고 있다면 64비트 버전의 IntelliJ IDEA를 사용하세요. 예를 들어 64비트 Windows라도 시작 메뉴 단축키가 32비트 바이너리를 가리킬 수 있습니다. 이 경우 설치 디렉터리에서 `idea64.exe`를 직접 실행해야 합니다. 그렇지 않으면 IntelliJ가 `io.netty:netty-tcnative:windows-x86_32`를 찾을 수 없다는 오류를 발생시킵니다.

### `Use --release …` 컴파일러 옵션 해제

Netty 4.1 이하 버전은 Java 6을 최소 버전으로 요구하므로 Java 6 바이트코드로 컴파일해야 합니다. 다만 성능을 위해 코드베이스에는 상위 Java API 호출도 포함되어 있으며, 런타임 버전 검사로 적절히 보호됩니다. 따라서 Netty는 `--source`와 `--target` 플래그로 컴파일해야 합니다. 그런데 IntelliJ는 기본적으로 `--release` 플래그를 사용하며, 이 플래그는 API 버전 검사도 함께 활성화합니다.

`--release` 플래그가 활성화된 상태에서는 Netty 프로젝트 빌드 시 컴파일 오류가 발생하므로, [Compiler Settings 다이얼로그](https://www.jetbrains.com/help/idea/troubleshooting-common-maven-issues.html#check_compiler_settings)에서 해당 옵션을 해제해야 합니다.

### 코드 스타일

[이 코드 스타일 설정](http://netty.io/files/IntelliJ%20IDEA%20Code%20Style.zip)을 다운로드하고 `Netty project.xml`을 `<IntelliJ config directory>/codestyles` 디렉터리에 압축 해제하세요. 그런 다음 'Netty project'를 기본 코드 스타일로 선택합니다.

### Inspection 프로파일

[이 inspection 프로파일](http://netty.io/files/IntelliJ%20IDEA%20Inspection%20Profile.xml.zip)을 다운로드해 압축 해제한 뒤 IntelliJ IDEA에 임포트해 기본값으로 사용하세요. inspection 프로파일을 임포트하는 방법은 [여기](http://www.jetbrains.com/idea/webhelp/customizing-profiles.html#d1372841e358)에서 확인할 수 있습니다.

수정 사항이 inspection 경고를 유발하지 않도록 하세요. 거짓 양성(false positive)으로 판단되는 경우에는 IDE의 안내에 따라 `@SuppressWarnings` 애노테이션이나 `noinspection` 라인 주석으로 경고를 억제하세요. inspection 기능 사용에 대한 자세한 내용은 [웹 도움말](http://www.jetbrains.com/idea/webhelp/inspecting-source-code.html)을 참고하세요.

### Copyright 프로파일

Copyright 텍스트:

```plain
Copyright $today.year The Netty Project

The Netty Project licenses this file to you under the Apache License,
version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at:

  https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
```

주석에서 copyright를 감지할 키워드:

```
The Netty project licenses
```

기존 copyright에 다음이 포함되어 있다면 교체를 허용:

```
The Netty project licenses
```

## M2E와 Java 7/8을 사용한 Eclipse 설정

1. 64비트 버전의 Eclipse를 사용하고 있는지 확인하세요.
2. [os-maven-plugin을 다운로드](http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.5.0.Final/os-maven-plugin-1.5.0.Final.jar)해서 `<ECLIPSE_INSTALLATION_DIR>/plugins`(Eclipse 4.5) 또는 `<ECLIPSE_INSTALLATION_DIR>/dropins`(Eclipse 4.6) 디렉터리에 넣으세요. `pom.xml`에 지정된 확장을 m2e가 평가하지 못하는 문제를 우회하기 위한 조치입니다. (이름과 달리 Maven 플러그인인 동시에 Eclipse 플러그인입니다.)
3. 'File → Import... → Existing Maven Projects' 메뉴를 통해 프로젝트를 임포트하세요.
4. Netty 프로젝트의 Maven `pom.xml` 설정은 Java SE 1.6을 강제하면서, 가능한 경우 Java 7/8(1.7/1.8) 기능을 암묵적으로 사용합니다. 이로 인해 Eclipse에서 컴파일 오류가 발생할 수 있습니다. 두 가지 우회 방법이 있습니다.
   1. 'Window → Preferences → Installed JRE' 메뉴를 확인하세요:
      * 'Installed JRE'에서 Java 7/8 설치를 사용할 수 있는지 확인합니다.
      * 'Installed JRE → Execution Environments → Java SE 1.6'에서 이 Java 7/8 설치를 Java 6으로 매핑합니다.
   2. 또는 각 Netty 모듈마다 프로젝트 단위로 Java 7/8 JRE를 선택할 수도 있습니다.
