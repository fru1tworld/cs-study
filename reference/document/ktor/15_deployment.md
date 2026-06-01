# 15. 배포

> 출처:
> - https://ktor.io/docs/server-fatjar.html
> - 일반적인 컨테이너/네이티브 배포 패턴
>
> 한국어 학습 노트입니다.

---

## 배포 옵션 한눈에

| 방식 | 언제 | 산출물 |
| --- | --- | --- |
| **Fat JAR** | 가장 단순한 JVM 실행 | `app-all.jar` |
| **Docker 이미지** | 컨테이너 오케스트레이션 | OCI 이미지 |
| **Application 플러그인 tar/zip** | 시스템 서비스로 설치 | `bin/` + `lib/` 트리 |
| **WAR (Servlet)** | 외부 서블릿 컨테이너 사용 | `app.war` |
| **GraalVM Native Image** | 빠른 부팅, 낮은 메모리 | 네이티브 바이너리 |

---

## Fat JAR — Ktor Gradle 플러그인

가장 빠른 길은 **Ktor Gradle 플러그인**이 제공하는 `buildFatJar` 태스크입니다.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "3.5.0"
    application
}

application {
    mainClass.set("com.example.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("app.jar")
    }
}
```

빌드 & 실행:

```bash
./gradlew buildFatJar
java -jar build/libs/app.jar
```

`runFatJar` 태스크는 빌드 직후 바로 실행해 줍니다.

> Kotlin Multiplatform 플러그인과 같이 쓰면 fatJar가 비활성화됩니다. JVM 전용 모듈을 따로 두고 MPP 모듈을 의존성으로 끼우는 게 정석.

---

## Shadow 플러그인 (수동 설정)

Ktor 플러그인 없이 직접 fat JAR을 만들고 싶다면 Shadow 플러그인을 사용합니다.

```kotlin
plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

application { mainClass.set("com.example.ApplicationKt") }

tasks.shadowJar {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    archiveVersion.set("")
}
```

```bash
./gradlew shadowJar
java -jar build/libs/app.jar
```

---

## Docker

흔한 멀티 스테이지 패턴:

```dockerfile
# ---- build ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /src
COPY . .
RUN ./gradlew buildFatJar --no-daemon

# ---- runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- 환경변수로 설정을 주입할 거라면 `application.conf`에 `${?PORT}`처럼 선택적 보간 키를 미리 박아두는 게 깔끔합니다 (`-Dconfig.override.X` 또는 `-P:ktor.deployment.X=...`도 가능).
- 헬스체크 엔드포인트(`get("/healthz") { call.respond(HttpStatusCode.OK) }`)는 거의 항상 둡니다.

---

## Application 플러그인 (tar/zip)

Gradle 표준 `application` 플러그인이 만드는 `distZip` / `distTar`는 systemd / nssm 같은 시스템 서비스 매니저로 띄울 때 깔끔합니다.

```bash
./gradlew installDist
./build/install/<project>/bin/<project>
```

---

## 서블릿 컨테이너 (WAR)

`ServletApplicationEngine`을 사용하고 `war` 플러그인을 적용하면 Tomcat/Jetty 같은 외부 컨테이너에 올릴 수 있는 WAR가 만들어집니다. 사내 표준이 서블릿 컨테이너라면 이 경로를 사용.

---

## GraalVM Native Image

엔진을 **CIO**로 두고 `org.graalvm.buildtools.native` 플러그인을 적용하면 단일 바이너리로 빌드할 수 있습니다. 시작 시간과 메모리가 크게 줄어들지만, 리플렉션을 쓰는 라이브러리(Jackson 등)는 별도 설정이 필요합니다.

---

## 운영 체크리스트

- `application.conf`의 secret/host 같은 값은 환경변수로 분리.
- `CallLogging` 또는 별도 액세스 로그를 켜둘 것.
- `StatusPages`에서 `Throwable` 폴백을 등록해 5xx 본문을 일관되게 유지.
- 그레이스풀 셧다운(`shutdownGracePeriod`, `shutdownTimeout`)을 컨테이너 종료 신호와 맞춰서 설정.
- 프록시 뒤라면 `ForwardedHeaders` 또는 `XForwardedHeaders` 설치.
- 메트릭은 `MicrometerMetrics`로 Prometheus 등에 노출.
