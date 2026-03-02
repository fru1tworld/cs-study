# Java 프로젝트에 Kotlin 추가하기 - 튜토리얼

이 튜토리얼은 두 언어 간의 완전한 상호운용성을 활용하면서 기존 Java 프로젝트에 Kotlin을 점진적으로 도입하는 방법을 보여줍니다.

## 학습 목표

- Java와 Kotlin 코드를 모두 컴파일하도록 Maven 또는 Gradle 설정
- 프로젝트 디렉토리에서 Java 및 Kotlin 소스 파일 구성
- IntelliJ IDEA를 사용하여 Java 파일을 Kotlin으로 변환

## 프로젝트 구성

### Maven 설정

**IntelliJ IDEA 2025.3+**는 Maven 프로젝트에 첫 번째 Kotlin 파일을 추가할 때 `pom.xml`을 자동으로 업데이트합니다.

수동으로 구성하려면:

1. **`<properties>`에 Kotlin 버전 프로퍼티 추가**:

```xml
<properties>
    <kotlin.version>2.3.10</kotlin.version>
</properties>
```

2. **`<dependencies>`에 Kotlin 의존성 추가**:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
</dependency>
```

3. **`<build><plugins>`에 Kotlin Maven 플러그인 구성**:

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>default-compile</id>
            <phase>compile</phase>
            <configuration>
                <sourceDirs>
                    <sourceDir>src/main/kotlin</sourceDir>
                    <sourceDir>src/main/java</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
        <execution>
            <id>default-test-compile</id>
            <phase>test-compile</phase>
            <configuration>
                <sourceDirs>
                    <sourceDir>src/test/kotlin</sourceDir>
                    <sourceDir>src/test/java</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
    </executions>
</plugin>
```

4. IDE에서 Maven 프로젝트 다시 로드
5. 테스트 실행: `./mvnw clean test`

### Gradle 설정

1. **`plugins {}`에 Kotlin JVM 플러그인 추가**:

```kotlin
plugins {
    kotlin("jvm") version "2.3.10"
}
```

2. **JVM 툴체인 설정**:

```kotlin
kotlin {
    jvmToolchain(17)
}
```

3. **`dependencies {}`에 Kotlin 테스트 라이브러리 추가**:

```kotlin
dependencies {
    testImplementation(kotlin("test"))
}
```

4. Gradle 프로젝트 다시 로드
5. 테스트 실행: `./gradlew clean test`

## 프로젝트 구조

```
src/
├── main/
│   ├── java/       # Java 및 Kotlin 프로덕션 코드
│   └── kotlin/     # 추가 Kotlin 프로덕션 코드 (선택 사항)
└── test/
    ├── java/       # Java 및 Kotlin 테스트 코드
    └── kotlin/     # 추가 Kotlin 테스트 코드 (선택 사항)
```

**핵심 포인트:**
- 동일한 디렉토리에서 `.kt`와 `.java` 파일 혼합
- Kotlin 플러그인은 `src/main/java`와 `src/test/java`를 모두 자동으로 인식
- 디렉토리를 수동으로 생성하거나 첫 번째 Kotlin 파일을 추가할 때 IntelliJ IDEA가 생성하도록 함

## Java 파일을 Kotlin으로 변환

IntelliJ IDEA에는 Java에서 Kotlin으로 변환기 (J2K)가 포함되어 있습니다:

1. Java 파일을 마우스 오른쪽 버튼으로 클릭
2. 컨텍스트 메뉴에서 **Convert Java File to Kotlin File** 선택 (또는 Code 메뉴)

**참고:** 변환기는 대부분의 보일러플레이트 코드를 잘 처리하지만 복잡한 로직에는 수동 조정이 필요할 수 있습니다.

## 권장 다음 단계

프로덕션 코드를 즉시 변환하는 대신 먼저 Kotlin 테스트를 추가하는 것으로 시작하세요. 이를 통해 안정적인 코드베이스를 유지하면서 점진적으로 Kotlin을 도입할 수 있습니다.

## 주요 구성 이점

- **원활한 상호운용성**: Java와 Kotlin 코드가 장벽 없이 서로 참조
- **Gradle/Maven 통합**: 두 빌드 도구 모두 두 언어를 적절히 컴파일하고 연결
- **단계적 마이그레이션**: 자신의 속도에 맞춰 점진적으로 Java 파일을 Kotlin으로 변환
