# Gradle 바이너리 플러그인 개발하기

> **원문:** https://docs.gradle.org/current/userguide/developing_binary_plugin_advanced.html

## 개요
바이너리 플러그인을 실제로 "제품"처럼 만들려면 단순히 `Plugin` 인터페이스를 구현하는 것만으로는 부족합니다. 사용자가 실행할 **태스크**와, 그 태스크를 설정할 **DSL 블록**을 함께 제공해야 합니다. 이 문서는 두 파일 크기를 비교하는 `FileSizeDiff`라는 작은 플러그인을 예로 들어, 플러그인을 구성하는 세 조각(확장·태스크·플러그인 클래스)을 어떻게 엮는지 보여줍니다. 여러 프로젝트에서 재사용할 목적이라면, 플러그인을 별도 저장소로 분리해 Maven 저장소에 배포하고 필요한 곳에서 가져다 쓰는 구조가 일반적입니다.

## 플러그인을 이루는 세 조각
| 구성 요소 | 역할 | 하는 일 |
|---|---|---|
| **Plugin 클래스** | 진입점 | 플러그인이 적용될 때 실행되는 로직(태스크 등록, 프로젝트 설정 변경 등)을 담당 |
| **Extension 클래스** | 설정값 저장 | 빌드 스크립트에서 사용자가 입력한 설정 값을 보관 |
| **Task 클래스** | 실제 동작 | 태스크가 실행될 때 수행되는 구체적인 로직을 구현 |

세 조각은 역할이 분리되어 있고, 보통 플러그인 클래스가 나머지 둘을 "연결"하는 접착제 역할을 합니다.

## 예시 프로젝트 구조
두 파일의 크기를 비교하는 `filesizediff` 플러그인을 기준으로 살펴봅니다.

```
plugin/
├── settings.gradle.kts
├── build.gradle.kts
└── src/main/java/org/example/
    ├── FileSizeDiffExtension.java
    ├── FileSizeDiffTask.java
    └── FileSizeDiffPlugin.java
```

빌드 스크립트에는 `java-gradle-plugin`을 적용하고, `gradlePlugin { }` 블록으로 플러그인 ID와 구현 클래스를 등록합니다.

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

`java-gradle-plugin`은 플러그인 개발에 필요한 각종 설정을 자동으로 잡아 주는 코어 플러그인입니다.

## 1. Extension — 설정값을 담는 그릇
확장 클래스는 사용자가 빌드 스크립트에서 채워 넣을 입력값의 "모양"을 정의합니다.

```java
public interface FileSizeDiffExtension {
    RegularFileProperty getFile1();
    RegularFileProperty getFile2();
}
```

- 인터페이스로만 선언하면 Gradle이 런타임에 구현체를 생성해 준다(추상 클래스도 가능).
- 속성 타입을 `Property` 계열(`RegularFileProperty` 등)로 선언하면 **지연(lazy) 평가**가 적용되어, 실제 값이 필요한 시점까지 계산을 미룰 수 있다.

## 2. Task — 실제 작업을 수행
태스크 클래스는 두 파일을 비교하는 로직 자체를 담습니다.

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

- `@InputFile`, `@OutputFile`은 각각 입력·출력 파일임을 Gradle에 알려, **증분 빌드**와 **빌드 캐시** 대상으로 추적되게 만든다. 입력·출력이 그대로면 태스크는 재실행되지 않는다.
- 실제 작업 코드는 `@TaskAction`이 붙은 메서드 안에 작성한다. 이 메서드가 태스크 실행 시 호출되는 지점이다.
- 확장에 담긴 사용자 값을 태스크의 입력 속성으로 옮겨주는 것은 태스크 자신의 책임이 아니라, 다음 단계인 **플러그인 클래스**의 책임이다.

## 3. Plugin — 확장과 태스크를 연결
플러그인 클래스는 `apply()` 안에서 확장을 등록하고, 태스크를 등록하면서 그 값을 확장에서 끌어와 매핑합니다.

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

- `extensions.create("diff", ...)` — 빌드 스크립트에서 `diff { }` 블록으로 설정할 수 있는 확장을 만든다.
- `tasks.register("fileSizeDiff", ...)` — `fileSizeDiff` 태스크를 등록하면서, `convention(...)`으로 태스크 속성의 기본값을 확장 속성에 연결한다. 사용자가 직접 태스크 속성을 건드리지 않아도 확장에 쓴 값이 자연스럽게 흘러 들어간다.

## 사용자 입장에서 본 최종 모습
플러그인을 적용하는 프로젝트에서는 다음처럼 짧게 설정하면 끝입니다.

```kotlin
plugins {
    id("org.example.filesizediff")
}

diff {
    file1 = file("a.txt")
    file2 = file("b.txt")
}
```

내부적으로는 `diff { }`에 쓴 값이 확장(`FileSizeDiffExtension`)에 저장되고, `convention()` 매핑을 통해 `fileSizeDiff` 태스크의 입력으로 전달되어 실행됩니다.

## 핵심 포인트
- 실전 바이너리 플러그인은 **Extension(설정) — Task(동작) — Plugin(연결)** 세 조각으로 나눠 설계하는 것이 기본형이다.
- Extension은 `Property` 계열 타입으로 선언해 지연 평가의 이점을 살리고, Task는 `@InputFile`/`@OutputFile`/`@TaskAction`으로 증분 빌드·캐시 대상을 명시한다.
- 확장 값을 태스크 속성으로 옮기는 "매핑"은 Task가 스스로 하지 않고, Plugin의 `apply()`에서 `convention()` 등으로 처리한다.
- 여러 프로젝트에서 공유할 플러그인은 별도 저장소로 분리해 Maven 저장소에 배포하는 방식이 일반적이다.
