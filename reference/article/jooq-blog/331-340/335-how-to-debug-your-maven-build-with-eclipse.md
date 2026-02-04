# Eclipse로 Maven 빌드를 디버그하는 방법

> 원문: https://blog.jooq.org/how-to-debug-your-maven-build-with-eclipse/

## 문제 상황

Maven 빌드를 실행할 때 여러 플러그인(jOOQ나 Flyway 같은)을 사용하는 경우, 내부에서 무슨 일이 일어나고 있는지 확인해야 할 때가 있습니다. 커맨드 라인에서 실행하면 이러한 프로세스를 검사할 수 있는 명확한 방법이 보이지 않습니다.

## 해결 방법

Java Debug Wire Protocol(JDWP) 디버깅을 활성화하는 배치 파일을 만들어서 해결할 수 있습니다.

### 1. 배치 파일 생성 (Windows)

Windows에서는 `MAVEN_OPTS`를 디버그 매개변수로 설정하는 배치 파일을 만듭니다:

```batch
@ECHO OFF
IF "%1" == "off" (
    SET MAVEN_OPTS=
) ELSE (
    SET MAVEN_OPTS=-Xdebug -Xnoagent -Djava.compile=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005
)
```

Mac/Linux에서는 `SET` 대신 `export`를 사용하면 됩니다.

### 2. 디버그 모드로 Maven 실행

배치 파일을 실행한 다음, 평소처럼 Maven 명령을 실행합니다:

```bash
mvn_debug
mvn clean install
```

Maven이 일시 중지되면서 다음과 같은 메시지가 표시됩니다: "Listening for transport dt_socket at address: 5005"

### 3. Eclipse 디버거 연결

- Eclipse에서 새로운 Remote Java Application 구성을 추가합니다
- localhost의 포트 5005에 소켓으로 연결하도록 설정합니다
- Debug를 클릭하여 실행 중인 Maven 프로세스에 연결합니다

이제 서버 애플리케이션을 디버깅하는 것처럼 브레이크포인트를 설정하고 Maven 프로세스를 디버깅할 수 있습니다.

### 4. 디버깅 비활성화

디버깅을 마쳤으면 `off` 매개변수와 함께 배치 파일을 호출합니다:

```bash
mvn_debug off
```

이렇게 하면 디버깅 오버헤드 없이 정상적인 Maven 동작으로 복원됩니다.

## 더 간단한 대안

사실 더 간단한 방법이 있습니다. Maven에 내장된 `mvnDebug` 명령을 사용하면 됩니다. 이 명령은 `MAVEN_OPTS`를 수동으로 구성할 필요 없이 자동으로 포트 8000에서 디버깅을 구성해 줍니다.

```bash
mvnDebug clean install
```

이 방식이 훨씬 더 간단하고 권장되는 접근 방법입니다.

## 참고

원래 스크립트의 일부 옵션(`-Xnoagent -Djava.compile=NONE`)은 더 이상 사용되지 않는 옵션입니다. 위에서 설명한 `mvnDebug` 명령을 사용하는 것이 가장 좋은 방법입니다.

이 기법은 Eclipse뿐만 아니라 IntelliJ와 NetBeans IDE에서도 동일하게 작동합니다.
