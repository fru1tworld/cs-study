# Gradle 성능 모범 사례

> **원문:** https://docs.gradle.org/current/userguide/best_practices_performance.html

## 개요

Gradle 공식 문서는 빌드 속도를 갉아먹는 흔한 실수 다섯 가지를 모범 사례로 정리해두고 있다. 배포판 선택, 인코딩 설정처럼 한 번 고쳐두면 끝나는 설정값도 있고, 빌드 캐시·구성 캐시처럼 켜기만 하면 되는 기능도 있다. 마지막 하나는 빌드 스크립트를 작성하는 습관 자체에 관한 것이다.

- 배포판(`-bin` vs `-all`) 선택
- 파일 인코딩(UTF-8) 고정
- 빌드 캐시 활성화
- 구성 캐시 활성화
- 구성 단계에서 무거운 연산 피하기

## 1. Gradle 배포판은 `-bin`을 쓴다

`gradle-wrapper.properties`의 `distributionUrl`은 흔히 `-all` 배포판을 가리키도록 설정되는데, 대부분의 경우 이는 낭비다.

| 배포판 | 포함 내용 | 특징 |
| --- | --- | --- |
| `-bin` | 실행에 필요한 바이너리만 | 다운로드 용량 작음, 검증·CI 시간 단축 |
| `-all` | 바이너리 + 소스 + 문서 | 용량 큼, IDE에서 소스를 미리 받아두고 싶을 때만 의미 있음 |

```properties
# Bad: 매번 큰 zip을 내려받는다
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-all.zip

# Good
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-bin.zip
```

**핵심 포인트**
- 최신 IDE는 소스/문서를 필요할 때 별도로 내려받을 수 있으므로, `-all`을 미리 준비해둘 이유가 거의 없다.
- CI처럼 매번 새 환경에서 배포판을 내려받는 상황일수록 `-bin`의 이득이 크다.

## 2. 파일 인코딩을 UTF-8로 고정한다

JVM의 기본 파일 인코딩은 OS·로케일에 따라 달라진다. 이 값이 빌드마다 다르면, 같은 소스인데도 컴파일 태스크의 입력 해시가 달라져 캐시가 어긋날 수 있다.

```properties
# gradle.properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

**핵심 포인트**
- 인코딩을 명시하지 않으면 "내 로컬에서는 캐시가 잘 맞는데 CI에서는 매번 다시 컴파일된다" 같은 플랫폼 종속적인 캐시 미스가 생길 수 있다.
- 팀 전체, CI 서버까지 포함해 동일한 값으로 고정하는 것이 중요하다.

## 3. 빌드 캐시를 켠다

빌드 캐시는 기본적으로 꺼져 있다. 입력이 같은 태스크의 출력을 재사용해 재실행을 건너뛰게 하는 기능이므로, 켜두지 않으면 그 이득을 아예 얻지 못한다.

```properties
# gradle.properties
org.gradle.caching=false   # 기본값 (비활성)
org.gradle.caching=true    # 권장
```

활성화하면 조건이 맞는 태스크가 실행 대신 `FROM-CACHE`로 표시된다.

**핵심 포인트**
- 로컬 개발, CI 파이프라인 모두에서 켜두는 것이 기본값처럼 취급되어야 한다.
- 캐시 효과를 제대로 보려면 태스크의 입력/출력 선언이 정확해야 한다는 전제는 그대로 유지된다.

## 4. 구성 캐시를 켠다

빌드 캐시가 "태스크 실행 결과"를 저장하는 반면, 구성 캐시는 "구성 단계 결과(태스크 그래프)" 자체를 저장한다. 두 캐시는 서로 독립적으로 동작하며, 어느 하나만 켜도 되고 둘 다 켜도 된다.

```properties
# gradle.properties
org.gradle.configuration-cache=false   # 기본값 (비활성)
org.gradle.configuration-cache=true    # 권장
```

- 첫 실행: `Configuration cache entry stored`
- 이후 입력이 같으면: `Configuration cache entry reused` → 구성 단계 자체를 건너뜀

**핵심 포인트**
- 구성 캐시가 재사용되면 스크립트 평가, 플러그인 적용 등 구성 단계 전체를 스킵하므로 특히 태스크 수가 많은 대형 프로젝트에서 효과가 크다.
- 빌드 캐시 = 태스크 출력 재사용, 구성 캐시 = 태스크 그래프(구성 결과) 재사용. 이름이 비슷해 혼동하기 쉬우니 구분해서 기억한다.

## 5. 구성 단계에서 무거운 연산을 하지 않는다

Gradle 빌드는 두 단계로 나뉜다. **구성 단계**는 태스크 그래프를 결정하기 위해 어떤 태스크를 실행하든 상관없이 항상 도는 부분이고, **실행 단계**는 실제로 선택된 태스크만 도는 부분이다. 파일 I/O, 네트워크 호출, CPU 연산처럼 비용이 큰 작업을 구성 단계에 두면, 그 태스크를 실행할 필요가 없는 빌드에서도 매번 비용을 치르게 된다.

```kotlin
// Bad: heavyWork()가 구성 단계에서 즉시 실행됨
tasks.register<MyTask>("myTask") {
    computationResult = heavyWork()
}

// Good: heavyWork()는 태스크가 실제로 실행될 때만 호출됨
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun run() {
        logger.lifecycle(heavyWork())
    }
}
tasks.register<MyTask>("myTask")
```

**핵심 포인트**
- `tasks.register { ... }` 블록 안에 직접 쓴 코드는 구성 시점에 실행된다는 점을 항상 의식한다.
- 무거운 로직은 `@TaskAction`이 붙은 실행 메서드 안으로 옮겨서, 해당 태스크가 실제로 선택됐을 때만 비용을 치르게 만든다.
- 이 습관은 구성 캐시(4번)의 효과와도 맞물린다. 구성 단계가 가벼울수록 구성 캐시 저장/복원도 빨라진다.

## 정리

| 항목 | 기본값 | 권장값 | 효과 |
| --- | --- | --- | --- |
| 배포판 | `-all`인 경우 있음 | `-bin` | 다운로드/검증 시간 단축 |
| 파일 인코딩 | 플랫폼 종속 | UTF-8 고정 | 캐시 미스 방지 |
| 빌드 캐시 | 비활성 | 활성 | 태스크 재실행 생략 |
| 구성 캐시 | 비활성 | 활성 | 구성 단계 자체 생략 |
| 구성 단계 로직 | 즉시 실행 코드 허용 | `@TaskAction`으로 지연 | 불필요한 연산 방지 |

다섯 항목 모두 "한 번 설정해두면 계속 이득을 보는" 성격이 강하므로, 신규 프로젝트를 세팅할 때 체크리스트로 삼아두는 것이 좋다.
