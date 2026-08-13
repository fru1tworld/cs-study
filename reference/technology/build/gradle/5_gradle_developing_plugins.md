# 플러그인 개발: 바이너리 플러그인 전체 흐름

## Gradle 플러그인 심화 소개

> 원문: https://docs.gradle.org/current/userguide/plugin_introduction_advanced.html

### 개요
플러그인 기초 문서에서는 "플러그인을 어떻게 적용하는가"를 다뤘다면, 이 문서는 한 단계 더 들어가 "플러그인이 빌드 라이프사이클의 어느 시점에, 어떤 범위에 개입하는가"와 "누가 만들고 어떻게 작성하는가"를 정리함. 즉 플러그인을 적용 시점(타입)·출처·작성 방식 세 축으로 나눠서 이해하는 것이 핵심.

### 적용 시점에 따른 세 가지 플러그인 타입
플러그인이 구현하는 인터페이스에 따라 개입할 수 있는 빌드 라이프사이클 범위가 결정됨.

- Init 플러그인
  - 구현 인터페이스: `Plugin<Gradle>`
  - 개입 범위: 전역(모든 빌드)
  - 적용 위치: `~/.gradle/init.gradle[.kts]` 또는 `--init-script`
- Settings 플러그인
  - 구현 인터페이스: `Plugin<Settings>`
  - 개입 범위: 빌드 레이아웃(포함 프로젝트, 플러그인 해석 방식 등)
  - 적용 위치: `settings.gradle[.kts]`
- Project 플러그인
  - 구현 인터페이스: `Plugin<Project>`
  - 개입 범위: 개별 프로젝트(태스크, 의존성 등)
  - 적용 위치: `build.gradle[.kts]`

- Init 플러그인: 빌드가 시작되기 전에 Gradle 런타임 자체를 설정함. 머신에 있는 모든 빌드에 공통으로 걸리는 전역 설정(사내 저장소 강제 지정 등)에 적합.
- Settings 플러그인: 멀티 프로젝트 구성이나 플러그인 해석 규칙처럼 "빌드가 어떤 모양을 가질지"를 결정하는 단계에서 동작함.
- Project 플러그인: 실무에서 가장 많이 접하는 형태로, `plugins { id("...") }`로 적용해 태스크·설정(configuration)·DSL을 추가하는 익숙한 방식.

### 플러그인의 범위(Scope)는 인터페이스가 결정함
플러그인이 `Plugin<Gradle>` / `Plugin<Settings>` / `Plugin<Project>` 중 무엇을 구현했는지가 곧 그 플러그인이 손댈 수 있는 대상의 경계 → Project 플러그인은 Settings나 Gradle 전역 설정에 개입 불가, 반대로 Init 플러그인은 전역적이라 개별 프로젝트의 세부 태스크보다는 상위 정책(리포지토리, 인증 등)을 다루는 데 사용됨. 플러그인을 설계할 때 "이 로직이 정말 전역/빌드 레이아웃/프로젝트 중 어느 층위에 속하는가"를 먼저 정하는 것이 인터페이스 선택의 기준.

### 출처에 따른 세 가지 분류
기초 문서의 코어/커뮤니티/커스텀 분류와 동일한 축이지만, 여기서는 "누가 작성하고 배포 책임을 지는가" 관점으로 다시 정리됨.

- 코어 플러그인: `java`, `application`처럼 Gradle 배포판에 내장되어 있어 별도로 작성하거나 배포할 필요 없음.
- 커뮤니티 플러그인: 커뮤니티가 만들어 주로 [Gradle Plugin Portal](https://plugins.gradle.org/)에 바이너리 JAR 형태로 공개한 것. 사용할 때는 ID와 버전을 함께 지정.
- 로컬 플러그인: 특정 조직·프로젝트를 위해 직접 작성한 플러그인. 여러 서브프로젝트에 공통 로직(컨벤션)을 나눠 쓰고 싶을 때 특히 유용.

### 직접 만들 때의 두 가지 작성 방식
로컬 플러그인을 작성하기로 했다면, 구현 형태를 두 가지 중에서 선택 가능.

#### 1. Precompiled Script 플러그인
`.gradle.kts`/`.gradle` 스크립트 파일 자체를 플러그인처럼 다루는 방식. 빌드 중에 자동으로 컴파일되며, 별도의 JAR 배포 없이 간단한 컨벤션(공통 자바 설정, 공통 테스트 설정 등)을 묶어 재사용하기에 적합.

```kotlin
// buildSrc/src/main/kotlin/my.java-conventions.gradle.kts
plugins {
    id("java")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
```

#### 2. Binary 플러그인
자바·코틀린·그루비 등으로 작성해 JAR로 패키징하는 정식 플러그인 형태. 여러 빌드에서 재사용하거나 Plugin Portal 같은 저장소에 배포해 커뮤니티 플러그인처럼 공유 가능. Precompiled Script 플러그인보다 초기 작성 비용이 크지만, 배포·버전 관리·테스트 측면에서 더 정식화된 구조를 갖춤.

### 핵심 포인트
- 플러그인은 구현 인터페이스(`Plugin<Gradle>`/`Plugin<Settings>`/`Plugin<Project>`)에 따라 Init / Settings / Project 세 타입으로 나뉘고, 이 인터페이스가 곧 개입 가능한 범위를 정함.
- 실무에서 다루는 대부분의 플러그인은 Project 플러그인이며, `plugins { }` 블록으로 적용하는 익숙한 패턴이 여기 해당.
- 출처 관점에서는 코어(내장) / 커뮤니티(Plugin Portal) / 로컬(자체 작성)로 나뉘는데, 이는 기초 문서의 분류와 같은 축을 다른 각도로 본 것.
- 로컬 플러그인을 직접 작성할 때는 Precompiled Script 플러그인(간단한 컨벤션 묶음)과 Binary 플러그인(JAR로 배포 가능한 정식 플러그인) 중에서 목적에 맞게 선택.
- 더 깊은 구현 방법은 Gradle 공식 문서의 "Pre-Compiled Script Plugins" 후속 문서에서 다룸.

---

## Gradle 바이너리 플러그인

> 원문: https://docs.gradle.org/current/userguide/binary_plugin_advanced.html

### 개요
빌드 스크립트에 직접 로직을 넣는 스크립트 플러그인만으로는 감당하기 어려운 요구사항(재사용, 테스트, 배포 등)이 생기면 바이너리 플러그인을 작성하는 것이 정답. 바이너리 플러그인은 코틀린·자바·그루비 같은 컴파일 언어로 작성되어 JAR로 패키징되는 플러그인으로, Gradle이 규정한 `Plugin` 인터페이스를 반드시 구현해야 함.

### Plugin 인터페이스 구현하기
바이너리 플러그인을 만드는 절차는 크게 두 단계로 요약됨.

#### 1단계 — `org.gradle.api.Plugin` 인터페이스 확장
플러그인 클래스를 선언하고 `Plugin<Project>`를 구현(확장)함. 제네릭 타입 파라미터(`Project`)는 이 플러그인이 어떤 대상에 적용되는지를 나타내며, 프로젝트가 아닌 `Settings`나 `Gradle` 객체에 적용되는 플러그인을 만드는 것도 가능.

```kotlin
abstract class SamplePlugin : Plugin<Project> {
}
```

#### 2단계 — `apply` 메서드 오버라이드
실제 플러그인 로직(태스크 등록, 확장 추가, 설정 등)은 `apply()` 메서드 안에 작성함. Gradle은 플러그인이 적용되는 시점에 이 메서드를 호출하며, 인자로 전달된 `Project` 객체를 통해 빌드에 필요한 요소를 추가함.

```kotlin
abstract class SamplePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("ScriptPlugin") {
            doLast {
                println("Hello world from the build file!")
            }
        }
    }
}
```

### 플러그인 적용 방법
클래스로 정의한 플러그인은 빌드 스크립트에서 클래스 참조로 직접 적용 가능(일반적으로는 `plugin-id`를 통한 적용을 사용하지만, 개발·테스트 단계에서는 클래스를 직접 지정하는 방식도 가능).

```kotlin
apply<SamplePlugin>()
```

```groovy
apply plugin: SamplePlugin
```

### 핵심 포인트
- 바이너리 플러그인 = 컴파일 언어로 작성 + JAR로 패키징된 플러그인. 스크립트 플러그인보다 재사용·테스트·배포에 유리.
- 구현은 단 두 단계. `Plugin<T>` 인터페이스 확장 → `apply(T target)` 메서드에 실제 로직 작성.
- 제네릭 타입 `T`는 플러그인이 적용될 대상(`Project`, `Settings`, `Gradle` 등)을 의미하며, 대부분은 `Project`를 대상으로 작성.
- `apply()` 안에서 태스크 등록, 확장 추가 등 원하는 빌드 커스터마이징을 자유롭게 구현 가능.
- 이 문서는 바이너리 플러그인의 최소 골격만 다루며, 실전 개발(확장 객체 설계, 컨벤션 적용 등)은 이어지는 "Developing Binary Plugins" 문서에서 다룸.

---

## Gradle 바이너리 플러그인 개발하기

> 원문: https://docs.gradle.org/current/userguide/developing_binary_plugin_advanced.html

### 개요
바이너리 플러그인을 실제로 "제품"처럼 만들려면 단순히 `Plugin` 인터페이스를 구현하는 것만으로는 부족함. 사용자가 실행할 태스크와, 그 태스크를 설정할 DSL 블록을 함께 제공해야 함. 이 문서는 두 파일 크기를 비교하는 `FileSizeDiff`라는 작은 플러그인을 예로 들어, 플러그인을 구성하는 세 조각(확장·태스크·플러그인 클래스)을 어떻게 엮는지 보여줌. 여러 프로젝트에서 재사용할 목적이라면, 플러그인을 별도 저장소로 분리해 Maven 저장소에 배포하고 필요한 곳에서 가져다 쓰는 구조가 일반적.

### 플러그인을 이루는 세 조각
- Plugin 클래스
  - 역할: 진입점
  - 하는 일: 플러그인이 적용될 때 실행되는 로직(태스크 등록, 프로젝트 설정 변경 등)을 담당
- Extension 클래스
  - 역할: 설정값 저장
  - 하는 일: 빌드 스크립트에서 사용자가 입력한 설정 값을 보관
- Task 클래스
  - 역할: 실제 동작
  - 하는 일: 태스크가 실행될 때 수행되는 구체적인 로직을 구현

세 조각은 역할이 분리되어 있고, 보통 플러그인 클래스가 나머지 둘을 "연결"하는 접착제 역할을 함.

### 예시 프로젝트 구조
두 파일의 크기를 비교하는 `filesizediff` 플러그인을 기준으로 살펴봄.

```
plugin/
├── settings.gradle.kts
├── build.gradle.kts
└── src/main/java/org/example/
    ├── FileSizeDiffExtension.java
    ├── FileSizeDiffTask.java
    └── FileSizeDiffPlugin.java
```

빌드 스크립트에는 `java-gradle-plugin`을 적용하고, `gradlePlugin { }` 블록으로 플러그인 ID와 구현 클래스를 등록함.

```kotlin
plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("filesizediff") {
            id = "org.example.filesizediff"
            implementationClass = "org.example.FileSizeDiffPlugin"
        }
    }
}
```

`java-gradle-plugin`은 플러그인 개발에 필요한 각종 설정을 자동으로 잡아 주는 코어 플러그인.

### 1. Extension — 설정값을 담는 그릇
확장 클래스는 사용자가 빌드 스크립트에서 채워 넣을 입력값의 "모양"을 정의함.

```java
public interface FileSizeDiffExtension {
    RegularFileProperty getFile1();
    RegularFileProperty getFile2();
}
```

- 인터페이스로만 선언하면 Gradle이 런타임에 구현체를 생성해줌(추상 클래스도 가능).
- 속성 타입을 `Property` 계열(`RegularFileProperty` 등)로 선언하면 지연(lazy) 평가가 적용되어, 실제 값이 필요한 시점까지 계산을 미룰 수 있음.

### 2. Task — 실제 작업을 수행
태스크 클래스는 두 파일을 비교하는 로직 자체를 담음.

```java
public abstract class FileSizeDiffTask extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getFile1();
    @InputFile
    public abstract RegularFileProperty getFile2();
    @OutputFile
    public abstract RegularFileProperty getResultFile();

    @TaskAction
    public void diff() {
        // 두 파일 크기를 비교하고 결과를 getResultFile()에 기록
    }
}
```

- `@InputFile`, `@OutputFile`은 각각 입력·출력 파일임을 Gradle에 알려, 증분 빌드와 빌드 캐시 대상으로 추적되게 만듦 → 입력·출력이 그대로면 태스크는 재실행되지 않음.
- 실제 작업 코드는 `@TaskAction`이 붙은 메서드 안에 작성함. 이 메서드가 태스크 실행 시 호출되는 지점.
- 확장에 담긴 사용자 값을 태스크의 입력 속성으로 옮겨주는 것은 태스크 자신의 책임이 아니라, 다음 단계인 플러그인 클래스의 책임.

### 3. Plugin — 확장과 태스크를 연결
플러그인 클래스는 `apply()` 안에서 확장을 등록하고, 태스크를 등록하면서 그 값을 확장에서 끌어와 매핑함.

```java
public class FileSizeDiffPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var extension = project.getExtensions().create("diff", FileSizeDiffExtension.class);

        project.getTasks().register("fileSizeDiff", FileSizeDiffTask.class, task -> {
            task.getFile1().convention(extension.getFile1());
            task.getFile2().convention(extension.getFile2());
            task.getResultFile().convention(
                project.getLayout().getBuildDirectory().file("diff-result.txt"));
        });
    }
}
```

- `extensions.create("diff", ...)` — 빌드 스크립트에서 `diff { }` 블록으로 설정할 수 있는 확장을 만듦.
- `tasks.register("fileSizeDiff", ...)` — `fileSizeDiff` 태스크를 등록하면서, `convention(...)`으로 태스크 속성의 기본값을 확장 속성에 연결함 → 사용자가 직접 태스크 속성을 건드리지 않아도 확장에 쓴 값이 자연스럽게 흘러 들어감.

### 사용자 입장에서 본 최종 모습
플러그인을 적용하는 프로젝트에서는 다음처럼 짧게 설정하면 끝.

```kotlin
plugins {
    id("org.example.filesizediff")
}

diff {
    file1 = file("a.txt")
    file2 = file("b.txt")
}
```

내부적으로는 `diff { }`에 쓴 값이 확장(`FileSizeDiffExtension`)에 저장되고, `convention()` 매핑을 통해 `fileSizeDiff` 태스크의 입력으로 전달되어 실행됨.

### 핵심 포인트
- 실전 바이너리 플러그인은 Extension(설정) — Task(동작) — Plugin(연결) 세 조각으로 나눠 설계하는 것이 기본형.
- Extension은 `Property` 계열 타입으로 선언해 지연 평가의 이점을 살리고, Task는 `@InputFile`/`@OutputFile`/`@TaskAction`으로 증분 빌드·캐시 대상을 명시함.
- 확장 값을 태스크 속성으로 옮기는 "매핑"은 Task가 스스로 하지 않고, Plugin의 `apply()`에서 `convention()` 등으로 처리함.
- 여러 프로젝트에서 공유할 플러그인은 별도 저장소로 분리해 Maven 저장소에 배포하는 방식이 일반적.

---

## Gradle 사전 컴파일 스크립트 플러그인

> 원문: https://docs.gradle.org/current/userguide/pre_compiled_script_plugin_advanced.html

### 개요
사전 컴파일 스크립트 플러그인(precompiled script plugin)은 개발하기 가장 간단한 플러그인 형태. 코틀린(`.kts`)이나 그루비(`.gradle`) 스크립트를 일반 빌드 스크립트와 동일한 문법으로 작성하면, Gradle이 이를 컴파일해 진짜 플러그인처럼 동작시켜줌. `Plugin` 인터페이스를 구현하는 바이너리 플러그인과 달리 클래스 작성이나 수동 등록이 불필요.

- 캡슐화 — 반복되는 설정을 이름 있는 플러그인으로 빼내 `build.gradle(.kts)`를 깔끔하게 유지
- 툴링 지원 — IDE 자동완성·네비게이션을 그대로 활용
- 보일러플레이트 제거 — 플러그인 클래스 작성이나 등록 절차 불필요
- 낮은 진입 장벽 — 익숙한 빌드 스크립트 문법을 그대로 사용하면서 플러그인의 구조적 이점을 얻음

### 컨벤션 플러그인으로서의 역할
사전 컴파일 스크립트 플러그인은 대부분 컨벤션 플러그인(convention plugin) 용도로 쓰임. 여러 프로젝트에 공통 설정·기본값·동작을 일관되게 적용하는 재사용 가능한 빌드 로직을 뜻하며, 특히 멀티 프로젝트 빌드에서 일관성을 지키는 데 유용.

예를 들어 `java-library-convention.gradle.kts` 하나에 자바 라이브러리 플러그인 적용, 체크스타일 설정, 소스 호환성 지정, 공통 의존성 등을 몰아넣으면, 각 서브프로젝트는 다음처럼 한 줄로 그 컨벤션을 가져다 쓸 수 있음.

```kotlin
// buildSrc/src/main/kotlin/java-library-convention.gradle.kts
plugins {
    `java-library`
    checkstyle
}
java { sourceCompatibility = JavaVersion.VERSION_11 }
```

```kotlin
// 소비하는 프로젝트의 build.gradle.kts
plugins {
    `java-library-convention`
}
```

### 어디에 두는가: buildSrc vs 컴포지트 빌드
사전 컴파일 스크립트 플러그인은 보통 다음 두 위치 중 하나에 둠.

1. `buildSrc` 디렉터리 — 루트 프로젝트 바로 아래에 두는 특별한 디렉터리로, Gradle이 메인 빌드보다 먼저 빌드해 자동으로 클래스패스에 올려줌.
   ```
   buildSrc/
   ├── build.gradle.kts
   └── src/main/kotlin/myproject.java-conventions.gradle.kts
   ```
2. 컴포지트 빌드(`build-logic` 등) — `includeBuild`로 포함시키는 독립된 빌드로, 여러 리포지토리·프로젝트에 걸쳐 빌드 로직을 공유하고 싶을 때 `buildSrc`보다 유연함.
   ```
   build-logic/
   ├── settings.gradle.kts
   ├── build.gradle.kts
   └── src/main/kotlin/myproject.java-conventions.gradle.kts
   ```

### 플러그인 ID와 적용 방법
플러그인 ID는 확장자(`.gradle.kts`/`.gradle`)를 뗀 스크립트 파일명에서 그대로 나옴. 예를 들어 `myproject.java-conventions.gradle.kts`는 곧 `myproject.java-conventions`라는 ID를 갖는 플러그인이 됨. 별도의 `Plugin` 인터페이스 구현이나 등록 없이, 스크립트 자체가 플러그인 역할을 함.

```kotlin
plugins {
    id("myproject.java-conventions")
}
```

### 배포는 권장되지 않음
Gradle Plugin Portal 같은 공개 저장소나 Maven/Artifactory/Nexus 같은 사내 저장소에 사전 컴파일 스크립트 플러그인을 게시하는 것 자체는 기술적으로 가능함. 하지만 이 방식은 내부 사용을 전제로 설계되어 있어 공식적으로 배포를 권장하지 않음. 외부에 배포해야 한다면 먼저 바이너리 플러그인으로 전환하는 것이 정석.

### 실전 적용 흐름 (buildSrc 기준)
1. `buildSrc` 디렉터리 생성 — 루트 프로젝트 아래에 `buildSrc/src/main/kotlin`(또는 `groovy`) 구조를 만듦.
2. 빌드 파일 작성 — `buildSrc/build.gradle.kts`에 `kotlin-dsl`(그루비는 `groovy-gradle-plugin`) 플러그인과 `gradlePluginPortal()` 저장소를 선언해, 컨벤션 스크립트가 컴파일될 수 있게 함.
3. 플러그인 스크립트 작성 — `myproject.java-conventions.gradle.kts` 같은 파일에 `java` 플러그인 적용, 태스크 설정, 저장소 지정 등 공유하고 싶은 로직을 담음. 이 파일명이 곧 플러그인 ID(`myproject.java-conventions`)가 됨.
4. 소비 프로젝트에서 적용 — 각 서브프로젝트의 `build.gradle.kts`에서 `id("myproject.java-conventions")`로 적용하고, 그 프로젝트만의 추가 의존성 등을 덧붙임.

### 핵심 포인트
- 사전 컴파일 스크립트 플러그인 = 빌드 스크립트 문법 그대로 작성하는 플러그인. 클래스 구현이나 수동 등록이 필요 없어 진입 장벽이 가장 낮음.
- 실무에서는 거의 항상 컨벤션 플러그인(여러 프로젝트에 공통 설정을 강제하는 용도)으로 쓰임.
- 배치 위치는 `buildSrc`(단순, 자동 인식) 또는 컴포지트 빌드인 `build-logic`(더 유연, 여러 빌드 간 공유 용이) 둘 중 하나.
- 플러그인 ID는 스크립트 파일명에서 그대로 파생되므로, 명명 규칙(`<namespace>.<name>`)만 지키면 별도 메타데이터 등록이 불필요.
- 외부 배포용이 아니라 내부 전용으로 설계된 기능이며, 공개 배포가 필요하면 바이너리 플러그인으로 전환 필요.

---

## Gradle 바이너리 플러그인 테스트하기

> 원문: https://docs.gradle.org/current/userguide/testing_binary_plugin_advanced.html

### 개요
플러그인도 결국 코드이므로 검증이 필요함. Gradle은 플러그인 전용 테스트 도구인 TestKit을 제공해, 플러그인이 적용된 상태의 "진짜" 빌드를 프로그램적으로 실행하고 결과를 검증할 수 있게 해줌. 잘 만든 플러그인은 보통 두 단계로 테스트를 나눔.

- 단위 테스트 — 클래스 하나만 떼어서 빠르게 검증
- 기능 테스트 — 실제 빌드를 임시 디렉터리에서 돌려보며 통째로 검증

이전 문서에서 만든 두 파일 크기를 비교하는 `filesizediff` 플러그인을 예로 들면, 소스 트리는 다음과 같이 테스트 코드 영역이 추가된 모습이 됨.

```
plugin/
├── build.gradle.kts
└── src/
    ├── main/java/org/example/          (플러그인 본체)
    ├── test/java/org/example/          (단위 테스트)
    └── functionalTest/java/org/example/ (기능 테스트)
```

### 1. 단위 테스트 — `ProjectBuilder`
단위 테스트는 전체 Gradle 실행 없이, 가벼운 가상 프로젝트 위에서 플러그인 로직만 검증함. 확인하는 내용은 대개 두 가지.

- 플러그인이 에러 없이 적용되는가
- 태스크·확장이 의도한 이름으로 등록되는가

```java
Project project = ProjectBuilder.builder().build();
project.getPlugins().apply("org.example.filesizediff");

assertNotNull(project.getTasks().findByName("fileSizeDiff"));
```

`ProjectBuilder`가 만들어주는 프로젝트는 실제 빌드를 실행하지 않는 "적용 대상"일 뿐이라, 태스크 실행 결과(출력 파일 등)까지는 검증하지 못함. 그래서 등록 여부 같은 기계적인 부분만 빠르게 확인하는 용도로 사용됨.

### 2. 기능 테스트 — TestKit과 `GradleRunner`
기능 테스트(End-to-End 테스트)는 임시 디렉터리에 실제 `build.gradle`을 만들고, `GradleRunner`로 진짜 Gradle 빌드를 실행해 결과를 확인함. 단위 테스트로는 볼 수 없는 부분, 즉 다음을 검증함.

- 플러그인이 실제 빌드에 정상 적용되는가
- 태스크가 실행되어 기대한 출력을 만드는가
- 사용자가 DSL(확장 블록)로 값을 넣었을 때 그대로 반영되는가

```java
@TempDir File projectDir;

@BeforeEach
void setup() throws IOException {
    writeString(new File(projectDir, "settings.gradle"), "");
    writeString(new File(projectDir, "build.gradle"), """
        plugins { id("org.example.filesizediff") }
        diff { file1 = file("a.txt"); file2 = file("b.txt") }
    """);
}

@Test
void canDiffTwoFilesOfTheSameSize() throws IOException {
    BuildResult result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments("fileSizeDiff")
        .build();

    assertTrue(result.getOutput().contains("Files have the same size"));
    assertEquals(TaskOutcome.SUCCESS, result.task(":fileSizeDiff").getOutcome());
}
```

- `@TempDir`는 테스트마다 새 임시 폴더를 만들고 종료 후 자동으로 지워줌.
- `@BeforeEach`에서 최소한의 `settings.gradle`/`build.gradle`을 미리 써두고, 각 테스트는 그 위에 입력 파일만 얹어 시나리오를 구성함.
- `GradleRunner`는 실제 Gradle 실행을 흉내 내는 API로, `withProjectDir()`(대상 디렉터리) → `withPluginClasspath()`(테스트 중인 플러그인 코드를 클래스패스에 주입) → `withArguments()`(실행할 태스크) → `build()`(실행 및 결과 반환) 순서로 조립함.
- 결과는 `BuildResult`로 돌아오며, 로그 출력 문자열과 `TaskOutcome`(SUCCESS/FAILED/UP_TO_DATE 등)을 함께 검증 가능.

### 기능 테스트를 위한 소스셋 구성
Gradle은 `functionalTest`처럼 이름을 붙인 커스텀 소스셋을 자동으로 인식하지 않으므로, 빌드 스크립트에 직접 등록해야 함. 다행히 `gradle init`으로 플러그인 프로젝트를 만들면 이 설정을 자동으로 만들어줌.

```kotlin
plugins {
    `java-gradle-plugin`
}

val functionalTestSourceSet = sourceSets.create("functionalTest")

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named("check") { dependsOn(functionalTest) }
```

- 새 소스셋(`functionalTest`)을 만들고, 의존성 설정(`configurations`)이 기존 `test` 계열 설정을 이어받도록(`extendsFrom`) 연결함.
- 그 소스셋을 실행할 `Test` 타입 태스크를 별도로 등록하고, `check` 태스크가 이 태스크에도 의존하게 만들어 `./gradlew check` 한 번으로 단위 테스트와 기능 테스트가 모두 돌게 함.
- `gradlePlugin.testSourceSets.add(...)`로 등록해두면, `java-gradle-plugin`이 플러그인 검증(validation) 대상에도 이 소스셋을 포함시킴.

### 소비자 프로젝트로 한 번 더 확인하기 (선택)
TestKit 기반 테스트 외에, 플러그인을 실제로 "가져다 쓰는" 별도 프로젝트를 만들어 눈으로 직접 확인해볼 수도 있음. `includeBuild()`로 플러그인 프로젝트를 복합 빌드에 끌어와 적용함.

```kotlin
// settings.gradle.kts
rootProject.name = "consumer"
includeBuild("plugin")
```

```kotlin
// build.gradle.kts
plugins { id("org.example.filesizediff") }
diff { file1 = file("a.txt"); file2 = file("b.txt") }
```

이후 `a.txt`, `b.txt`를 실제로 만들어두고 `./gradlew fileSizeDiff`를 실행하면, 콘솔에 찍히는 로그로 플러그인이 실제 환경에서 기대대로 동작하는지 확인 가능. 자동화된 테스트를 대체하는 것은 아니지만, 개발 중 빠르게 손으로 찔러보는 용도로 유용.

### 핵심 포인트
- 플러그인 테스트는 단위 테스트(`ProjectBuilder`)로 빠르게 등록 여부를 확인하고, 기능 테스트(TestKit `GradleRunner`)로 실제 빌드 동작까지 검증하는 이중 구조가 기본.
- `GradleRunner`는 `withProjectDir → withPluginClasspath → withArguments → build()` 순으로 조립해 임시 디렉터리에 진짜 빌드를 실행시키는 API.
- `functionalTest`처럼 커스텀 소스셋을 쓰려면 소스셋 생성, 설정 상속(`extendsFrom`), 전용 `Test` 태스크 등록, `check`와의 연결까지 직접 잡아줘야 하며 `gradle init`이 이를 스캐폴딩해줌.
- `includeBuild()`로 연결한 별도의 소비자 프로젝트는 자동화 테스트를 대체하지 않지만, 실제 사용 환경에서 플러그인을 손으로 검증하는 보조 수단이 됨.

---

## Gradle 바이너리 플러그인 배포

> 원문: https://docs.gradle.org/current/userguide/publishing_binary_plugin_advanced.html

### 개요
바이너리 플러그인을 만들고 테스트까지 마쳤다면, 다음 단계는 배포(publish). 여러 빌드에서 재사용하거나 다른 사람과 공유하려면 플러그인을 어딘가의 저장소에 올려야 하는데, Gradle은 이를 위한 여러 방법을 제공함. 가장 기본이 되는 것이 코어 플러그인인 `maven-publish`이며, 이 문서는 로컬 Maven 저장소에 플러그인을 배포하는 가장 단순한 경로를 다룸.

### 1. maven-publish 플러그인 적용
플러그인을 배포하려면 `java-gradle-plugin`과 함께 `maven-publish`를 적용해야 함.

```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
}
```

- `java-gradle-plugin`은 플러그인 개발에 필요한 기본 골격(플러그인 디스크립터 생성 등)을 제공
- `maven-publish`는 그 결과물을 Maven 형식의 저장소에 올리는 역할을 담당

흥미로운 점은 배포에 필요한 핵심 정보(`group`, `version`, 플러그인 `id`)가 이미 개발 단계에서 작성해 둔 `gradlePlugin { }` 블록과 프로젝트 설정에 들어 있다는 것.

```kotlin
group = "org.example"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("filesizediff") {
            id = "org.example.filesizediff"
            implementationClass = "org.example.FileSizeDiffPlugin"
        }
    }
}
```

- `group` — 배포될 아티팩트의 그룹 ID
- `version` — 배포될 아티팩트의 버전
- `gradlePlugin { }` — `java-gradle-plugin`이 제공하는 블록으로, 플러그인의 전체 이름(`org.example.filesizediff`)과 이를 구현하는 클래스(`FileSizeDiffPlugin`)를 등록

즉 별도로 배포 메타데이터를 새로 작성할 필요 없이, 개발 시점에 선언한 정보가 배포 시점에 그대로 재사용됨.

### 2. 배포 대상 저장소 설정
`publishing { }` 블록 안에 `repositories { }`를 두어 아티팩트를 어디에 올릴지 지정함.

```kotlin
publishing {
    repositories {
        maven {
            url = uri("${layout.projectDirectory}/publish")
        }
    }
}
```

- 예시는 원격 저장소가 아니라 프로젝트 디렉터리 안의 로컬 경로(`publish` 폴더)를 대상으로 지정.
- 실무에서는 이 자리에 사내 아티팩트 저장소나 Gradle Plugin Portal 등 실제 배포처의 URL이 들어감.

### 3. 배포 실행
저장소까지 설정했다면 배포는 태스크 한 번 실행으로 끝남.

```bash
./gradlew publish
```

이 명령을 실행하면 Gradle이 플러그인 JAR과 함께 `pom.xml` 등 관련 메타데이터 파일을 생성해 지정한 Maven 저장소에 설치함.

### 핵심 포인트
- 플러그인 배포의 최소 구성은 `java-gradle-plugin` + `maven-publish` 두 플러그인의 조합.
- 배포에 필요한 `group`/`version`/`id` 정보는 새로 작성하는 것이 아니라, 개발 단계의 `gradlePlugin { }` 선언을 그대로 재사용함.
- `publishing { repositories { maven { url = ... } } }`로 배포 목적지(로컬 경로, 원격 저장소, Plugin Portal 등)만 바꿔주면 배포 대상을 자유롭게 전환 가능.
- 실제 배포는 `./gradlew publish` 한 줄로 수행되며, 이 과정에서 JAR과 `pom.xml` 같은 메타데이터가 함께 생성·업로드됨.
- 이 문서는 로컬 Maven 저장소 배포라는 가장 단순한 사례만 다루며, Gradle Plugin Portal 공개 배포 등은 별도 플러그인·절차가 필요함.
