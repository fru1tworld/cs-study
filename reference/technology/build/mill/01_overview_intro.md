# Mill 개요와 Java 빌드 입문

> 원본: https://mill-build.org/mill/Intro_to_Mill.html (redirect) → https://mill-build.org/mill/javalib/intro.html

---

## 목차

1. [Mill이 제공하는 세 가지 빌드 정의 방식](#1-mill이-제공하는-세-가지-빌드-정의-방식)
2. [선언적 설정 (YAML)](#2-선언적-설정-yaml)
3. [기본 폴더 레이아웃과 주요 명령](#3-기본-폴더-레이아웃과-주요-명령)
4. [멀티 모듈 프로젝트와 태스크 쿼리 문법](#4-멀티-모듈-프로젝트와-태스크-쿼리-문법)
5. [Maven 호환 모듈](#5-maven-호환-모듈)
6. [단일 파일 스크립트](#6-단일-파일-스크립트)
7. [프로그래머블 설정 (build.mill / Scala)](#7-프로그래머블-설정-buildmill--scala)
8. [커스텀 빌드 로직](#8-커스텀-빌드-로직)
9. [프로그래머블 멀티 모듈과 호환 모듈](#9-프로그래머블-멀티-모듈과-호환-모듈)
10. [실무 예제: 퍼블리싱까지 포함한 멀티 모듈 빌드](#10-실전-예제-퍼블리싱까지-포함한-멀티-모듈-빌드)

---

## 1. Mill이 제공하는 세 가지 빌드 정의 방식

Mill로 Java 프로젝트를 정의하는 방법은 세 가지다.

| 방식 | 특징 | 적합한 상황 |
|------|------|------------|
| 선언적 설정 (Declarative Configuration) | `.mill.yaml` 파일에 의존성·버전 등 키를 나열 | 단순한 설정만 필요한 프로젝트 |
| 단일 파일 스크립트 (Single-File Scripts) | 코드와 빌드 설정을 한 파일에 담음 | 소규모 스크립트, 재현 가능한 예제 |
| 프로그래머블 설정 (Programmable Configuration) | Scala로 작성된 `build.mill`에서 커스텀 로직 작성 | 코드 생성, 리소스 가공 등 커스텀 로직이 필요한 빌드 |

Mill Java 툴체인의 API 레퍼런스는 [mill.javalib](https://mill-build.org/api/latest/mill/javalib.html)에서 확인할 수 있다.

---

## 2. 선언적 설정 (YAML)

가장 단순한 형태는 `build.mill.yaml`과 하위 모듈의 `package.mill.yaml`로 구성된다.

**build.mill.yaml**

```yaml
extends: JavaModule
mvnDeps:
- net.sourceforge.argparse4j:argparse4j:0.9.0
- org.thymeleaf:thymeleaf:3.1.1.RELEASE
- org.slf4j:slf4j-nop:2.0.7
```

**test/package.mill.yaml**

```yaml
extends: [build.JavaTests, TestModule.Junit4]
mvnDeps:
- com.google.guava:guava:33.3.0-jre
```

- `build.mill.yaml`은 루트 모듈 설정을 정의한다.
- `test/package.mill.yaml`은 `test/` 하위 모듈(테스트 스위트)을 정의한다.

`build.mill.yaml`과 `package.mill.yaml`은 내부적으로 `build.*`, `javalib.*`를 임포트하고 있어서 `JavaModule`, `TestModule`처럼 짧은 이름을 그대로 쓸 수 있다. 완전한 이름은 각각 `mill.javalib.JavaModule`, `mill.javalib.TestModule`이다.

이 예제는 CLI 인자 파싱 라이브러리 ArgParse4J와 HTML 렌더링 라이브러리 Thymeleaf 두 개의 서드파티 의존성을 사용해 입력 문자열을 HTML 템플릿에 안전하게 삽입한다. `test/` 하위 모듈은 테스트에서만 쓰는 `mvnDeps`를 별도로 갖는다.

각 모듈이 지원하는 키(`mvnDeps` 등)와 타입은 해당 모듈 클래스의 API 문서, 예를 들어 [mill.javalib.JavaModule](https://mill-build.org/api/latest/mill/javalib/JavaModule.html#mvnDeps-0)에 정리되어 있다.

---

## 3. 기본 폴더 레이아웃과 주요 명령

소스 코드는 `src/`에, 태스크 결과물은 `out/`에 쌓인다.

```
build.mill.yaml
src/
    foo/Foo.java
resources/
    ...
test/
    package.mill.yaml
    src/
        foo/FooTest.java
out/
    compile.json
    compile.dest/
    ...
    test/
        compile.json
        compile.dest/
        ...
```

Mill 기본 레이아웃(`src/`, `test/src/`)은 Maven/Gradle의 `src/main/java/`, `src/test/java/`와 다르다. 기존 코드베이스와의 호환이 필요하면 5장의 Maven 호환 모듈을 사용한다.

자주 쓰는 명령들:

```
> ./mill resolve _ # 실행 가능한 태스크 목록 조회
assembly
...
compile
...
run
...

> ./mill inspect compile # 태스크 문서와 입력 확인
compile(JavaModule.scala:...)
    Compiles the current module to generate compiled classfiles/bytecode.
Inputs:
    upstreamCompileOutput
    allSourceFiles
    compileClasspath

> ./mill compile # 소스를 클래스파일로 컴파일

> ./mill test

> ./mill run --text hello
<h1>hello</h1>

> ./mill assembly # 클래스파일과 라이브러리를 배포용 jar로 번들링

> ./mill show assembly # assembly 태스크의 출력 경로 확인
".../out/assembly.dest/out.jar"

> java -jar ./out/assembly.dest/out.jar --text hello
<h1>hello</h1>

> ./out/assembly.dest/out.jar --text hello # mac/linux
<h1>hello</h1>

> cp ./out/assembly.dest/out.jar out.bat # windows에서는 확장자를 .bat로 바꿔야 java -jar 없이 실행 가능
> ./out.bat --text hello # windows
<h1>hello</h1>
```

모든 Mill 태스크의 출력은 해당 태스크 이름과 대응하는 `out/` 하위 경로에 저장된다. 예를 들어 `assembly` 태스크는 메타데이터를 `out/assembly.json`에, 결과 파일을 `out/assembly.dest`에 남긴다. `show`로 태스크의 출력 메타데이터를, `inspect`로 태스크 자체에 대한 문서·정의 위치 등을 확인할 수 있다.

그 외 자주 쓰는 태스크:

```
> ./mill resolve __    # 모든 태스크·모듈을 재귀적으로 나열
> ./mill runBackground # main 메서드를 백그라운드로 실행
> ./mill clean <task>  # 태스크의 캐시된 출력 삭제, runBackground 종료
> ./mill launcher      # 나중에 실행할 out/launcher.dest/run 준비
> ./mill jar           # 퍼블리싱용 jar 번들링
> ./mill -w compile    # 입력 파일 변경을 감시하며 재컴파일
```

JShell REPL은 루트 모듈이나 `test` 하위 모듈에 붙여서 실행할 수 있다.

```
> ./mill jshell
> ./mill test.jshell
```

`build.mill` 파일이 없는 디렉터리에서도 `./mill jshell`만으로 독립 JShell 콘솔을 띄울 수 있다.

Mill이 실행하는 태스크는 크게 두 종류다. `compile`처럼 입력이 바뀌지 않으면 재평가하지 않는 **캐시된 태스크**와, `run`처럼 실행할 때마다 다시 도는 **캐시되지 않는 커맨드**다. 자세한 내용은 [Tasks](../fundamentals/tasks.html) 문서를 참고한다.

이 예제는 단순한 설정 키 나열에 적합한 YAML 선언적 문법을 쓰고 있으며, 자세한 내용은 [Declarative Java Configuration](module-config.html#_declarative_configuration)에서 다룬다.

---

## 4. 멀티 모듈 프로젝트와 태스크 쿼리 문법

`foo`, `bar` 두 모듈을 각각의 `package.mill.yaml`로 정의하고, `bar` 아래에 `bar/test`를 두는 예제다.

**foo/package.mill.yaml**

```yaml
extends: JavaModule
moduleDeps: [bar]
mvnDeps:
- net.sourceforge.argparse4j:argparse4j:0.9.0
```

**bar/package.mill.yaml**

```yaml
extends: JavaModule
mvnDeps:
- org.thymeleaf:thymeleaf:3.1.1.RELEASE
- org.slf4j:slf4j-nop:2.0.7
```

**bar/test/package.mill.yaml**

```yaml
extends: [build.bar.JavaTests, TestModule.Junit4]
mvnDeps:
- com.google.guava:guava:33.3.0-jre
```

모듈 간 의존은 `moduleDeps` 키로 선언하고, 모듈은 서로 중첩될 수 있다(`bar.test`가 `bar` 안에 중첩). `moduleDeps: [bar]`처럼 짧은 이름으로 참조하거나 `moduleDeps: [build.bar]`처럼 루트 모듈 이름(`build`)을 포함한 전체 경로로도 참조할 수 있다.

폴더 구조는 다음과 같다.

```
build.mill.yaml
foo/
    package.mill.yaml
    src/
        foo/Foo.java
bar/
    package.mill.yaml
    src/
        bar/Bar.java
    test/
        package.mill.yaml
        src/
            bar/BarTests.java
out/
    foo/
        compile.json
        compile.dest/
        ...
    bar/
        compile.json
        compile.dest/
        ...
        test/
            compile.json
            compile.dest/
            ...
```

소스, 출력, 태스크 이름은 모듈 계층 구조를 그대로 따른다. 예를 들어 `foo` 모듈의 입력은 `foo/src/`에 있고, `foo.compile`로 컴파일하면 결과물은 `out/foo/compile.dest`에 쌓인다.

```
> ./mill resolve __.run
foo.run
bar.run

> ./mill foo.run --text "hello-world"
<h1>hello-world</h1>

> ./mill bar.test
...
...bar.BarTests...simple...
...bar.BarTests...escaping...
```

Mill은 지정한 태스크와 그 상류(upstream) 의존 태스크가 올바른 순서로 실행되도록 보장하며, 소스가 바뀔 때만 다시 실행한다. 예를 들어 `foo.compile`을 실행하면 Mill은 아직 실행되지 않았거나 최신이 아닌 `bar.compile` 등을 먼저 자동으로 실행하고, 이미 실행되어 최신 상태라면 캐시된 결과를 그대로 재사용한다.

### 태스크 쿼리 문법

여러 태스크를 한 번에 선택하거나 깊이 중첩된 경로를 축약할 때 와일드카드와 중괄호 확장을 사용한다.

| 와일드카드 | 의미 |
|-----------|------|
| `_` | 태스크 경로의 한 세그먼트에 매칭 |
| `__` | 태스크 경로의 임의 개수 세그먼트에 매칭 |
| `{a,b}` | 태스크 `a`와 `b`를 동시에 지정한 것과 동일 |

`+` 기호로 옵션 인자를 가진 다른 태스크를 이어붙일 수 있다. `+`를 인자 값으로 넘기려면 백슬래시(`\`)로 이스케이프한다.

```
> ./mill bar._.compile            # foo의 모든 직계 하위 모듈에 대해 compile 실행
> ./mill bar.__.test              # foo의 모든 전이적 하위 모듈에 대해 test 실행
> ./mill {foo,bar}.compile        # foo와 bar에 대해 compile 실행
> ./mill __.compile + bar.__.test # 모든 compile 태스크와 foo 하위 모든 test 실행
```

와일드카드나 중괄호 확장이 여러 태스크로 풀리는 경우, 지정한 인자는 각 태스크에 동일하게 적용된다. 자세한 내용은 [Task Query Syntax](../cli/query-syntax.html) 참고.

---

## 5. Maven 호환 모듈

Mill 기본 레이아웃(`src/`, `test/src`)은 Maven/Gradle/SBT에서 흔히 쓰는 `src/main/scala/`, `src/test/scala/`와 다르다. 기존 코드베이스에 Mill을 도입할 때는 `MavenModule`, `MavenTests`로 파일시스템 호환성을 유지할 수 있다.

**build.mill.yaml**

```yaml
extends: MavenModule
mvnDeps:
- net.sourceforge.argparse4j:argparse4j:0.9.0
- org.thymeleaf:thymeleaf:3.1.1.RELEASE
- org.slf4j:slf4j-nop:2.0.7

object test:
  extends: [MavenTests, TestModule.Junit4]
  mvnDeps:
  - com.google.guava:guava:33.3.0-jre

object integration:
  extends: [MavenTests, TestModule.Junit4]
  mvnDeps:
  - com.google.guava:guava:33.3.0-jre
```

`object test:`, `object integration:` 키를 이용하면 별도 하위 폴더에 `package.mill.yaml`을 두지 않고도 같은 `build.mill.yaml` 안에서 하위 모듈을 정의할 수 있다. `test/`, `integration/` 폴더가 실제로 존재하지 않는 이런 경우에 유용하다.

`MavenModule`은 `JavaModule`의 변형으로 다음과 같은 더 장황한 폴더 레이아웃을 사용한다.

- `src/main/java/`
- `src/test/java/`

(Mill 기본은 `src/`, `test/src/`)

이 호환 모듈은 마이그레이션 상황에서 유용하다. 기존 빌드 도구와 Mill을 동시에 사용하며 마이그레이션하는 동안, 소스 파일 위치를 옮기지 않고도 Mill 빌드를 구성할 수 있다.

커맨드라인 사용법은 기본 `JavaModule`과 동일하다.

```
> ./mill compile
compiling 1 Java source...

> ./mill test.compile
compiling 1 Java source...

> ./mill test.testForked
...foo.FooTests.hello ...

> ./mill test
...foo.FooTests.hello ...

> ./mill integration
...foo.FooIntegrationTests.hello ...
```

다른 빌드 도구에서 마이그레이션하는 방법은 [Migrating to Mill](../migrating/migrating.html) 참고.

---

## 6. 단일 파일 스크립트

Mill은 서드파티 의존성이나 특정 JVM 버전 같은 빌드 설정이 포함된 단일 파일 Java 프로그램도 커맨드라인에서 바로 실행할 수 있게 해준다. `./mill` bootstrap 스크립트를 통해 사전 설치나 설정 없이 재현 가능하게 실행되는 것이 특징이다. 활용 사례는 다음과 같다.

- Bash 스크립트 대체: 서드파티 라이브러리에 접근 가능한 작은 Java 스크립트를 dev/test/prod 환경에서 동일하게 재현 가능하게 실행
- 독립적인 예제나 이슈 재현: 코드와 필요한 의존성을 한 파일에 담아 별도 설치 없이 `./mill`로 실행
- 소규모 프로젝트/실험: 별도 빌드 파일 관리가 부담스러운 경우, 코드와 설정을 한 파일에 두어 관리 편의성을 높임

더 많은 활용 예는 [Script Use Cases](script.html#_script_use_cases) 참고.

**Foo.java**

```java
//| jvmVersion: 11.0.28
//| mvnDeps:
//| - net.sourceforge.argparse4j:argparse4j:0.9.0
//| - org.thymeleaf:thymeleaf:3.1.1.RELEASE
//| - org.slf4j:slf4j-nop:2.0.7

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

public class Foo {
  public static String generateHtml(String text) {
    Context context = new Context();
    context.setVariable("text", text);
    return new TemplateEngine().process("<h1 th:text=\"${text}\"></h1>", context);
  }

  public static void main(String[] args) throws Exception {
    ArgumentParser parser = ArgumentParsers.newFor("template")
        .build()
        .defaultHelp(true)
        .description("Inserts text into a HTML template");

    parser.addArgument("-t", "--text").required(true).help("text to insert");

    Namespace ns = null;
    ns = parser.parseArgs(args);
    System.out.println("Jvm Version: " + System.getProperty("java.version"));
    System.out.println(generateHtml(ns.getString("text")));
  }
}
```

`mvnDeps`로 서드파티 라이브러리를 쓸 수 있는 것 외에, `jvmVersion`으로 사용할 JVM 버전을 정확히 지정할 수 있다. 일반 Mill 모듈과 마찬가지로 스크립트도 `jvmVersion`을 지정하지 않으면 Mill 기본값인 `zulu:25`를 사용한다. 환경에 이미 설치된 `java` 커맨드를 쓰고 싶다면 `jvmVersion: system`을 명시해야 한다.

```
> ./mill Foo.java --text hello
compiling 1 Java source to...
<h1>hello</h1>

> ./mill Foo.java:run --text hello
<h1>hello</h1>
```

Mill은 필요한 아티팩트를 자동으로 다운로드하고 캐시하므로, 어떤 환경에서 실행하든 별도 설정 없이 동일하게 동작한다.

`./mill Foo.java`는 `./mill Foo.java:run`의 축약형이다. `Foo.java:assembly`처럼 다른 태스크도 호출할 수 있다.

```
> ./mill show Foo.java:assembly # assembly 태스크의 출력 경로 확인
".../out/Foo.java/assembly.dest/out.jar"

> java -jar ./out/Foo.java/assembly.dest/out.jar --text hello
<h1>hello</h1>

> ./out/Foo.java/assembly.dest/out.jar --text hello # mac/linux
<h1>hello</h1>
```

Java 스크립트는 `//|` 헤더 주석 안에서 선언적 설정과 동일한 설정 키를 지원하고, `:run`, `:assembly` 같은 태스크 커맨드라인 문법도 그대로 지원한다.

```
> ./mill resolve Foo.java:_
Foo.java:run
Foo.java:runMain
Foo.java:compile
Foo.java:assembly
...
```

### 스크립트 테스트

스크립트 파일도 보통 별도의 테스트 스크립트로 테스트 스위트를 가질 수 있다. 테스트 스크립트 설정은 다음을 지정한다.

- `extends`로 사용할 테스트 프레임워크
- `moduleDeps`로 테스트 대상 스크립트
- 상류 스크립트 의존성에 더해 필요한 `mvnDeps`

**FooTest.java**

```java
//| extends: mill.script.JavaModule.Junit4
//| moduleDeps: [Foo.java]
//| mvnDeps:
//| - com.google.guava:guava:33.3.0-jre

import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FooTest {
  @Test
  public void testSimple() {
    assertEquals(Foo.generateHtml("hello"), "<h1>hello</h1>");
  }

  @Test
  public void testEscaping() {
    assertEquals(Foo.generateHtml("<hello>"), "<h1>" + htmlEscaper().escape("<hello>") + "</h1>");
  }
}
```

```
> ./mill FooTest.java
Test FooTest.testSimple started
Test FooTest.testEscaping started

> ./mill FooTest.java:testForked # 테스트 태스크를 명시적으로 지정
Test FooTest.testSimple started
Test FooTest.testEscaping started
```

스크립트가 사용할 테스트 프레임워크는 `extends` 절에 지정한 클래스로 결정된다. [Testing Java Projects](testing.html)에서 다루는 `Junit4`, `Junit5`, `TestNg`, `Munit`, `ScalaTest`, `Specs2`, `Utest`, `Weaver`, `ZioTest` 등을 스크립트에도 그대로 쓸 수 있다. 목록에 없는 프레임워크가 필요하면 [Custom Script Module Class](script.html#_custom_script_module_classes)를 정의하면 된다.

스크립트가 한 파일 규모를 넘어 커지면 선언적 설정(YAML)으로 전환하는 것이 좋다.

---

## 7. 프로그래머블 설정 (build.mill / Scala)

**build.mill**

```scala
package build

import mill.*, javalib.*

object foo extends JavaModule {
  def mvnDeps = Seq(
    mvn"net.sourceforge.argparse4j:argparse4j:0.9.0",
    mvn"org.thymeleaf:thymeleaf:3.1.1.RELEASE",
    mvn"org.slf4j:slf4j-nop:2.0.7"
  )

  object test extends JavaTests, TestModule.Junit4 {
    def mvnDeps = Seq(
      mvn"com.google.guava:guava:33.3.0-jre"
    )
  }
}
```

YAML보다 조금 더 장황하지만, 태스크를 정의하고 값을 계산하는 방식에 더 큰 유연성을 준다.

- YAML의 `mvnDeps:` 키는 프로그래머블 문법에서 `def mvnDeps` 메서드에 대응한다.
- 선언적 문법의 루트 `build.mill.yaml`과 중첩된 `package.mill.yaml`은 프로그래머블 문법에서 루트/중첩 `object`에 대응한다.

폴더 구조:

```
build.mill
foo/
    src/
        foo/Foo.java
    resources/
        ...
    test/
        src/
            foo/FooTest.java
out/foo/
    compile.json
    compile.dest/
    ...
    test/
        compile.json
        compile.dest/
        ...
```

이 예제는 `JavaModule`을 `foo/` 하위 폴더에 두었지만, [Root Module](../fundamentals/modules.html#_root_modules)을 사용해 저장소 루트에 바로 둘 수도 있다.

```
> ./mill resolve foo._ # 실행 가능한 태스크 목록 조회
foo.assembly
...
foo.compile
...
foo.run
...

> ./mill foo.run --text hello
<h1>hello</h1>

> ./mill foo.test
Test foo.FooTest.testSimple finished, ...
Test foo.FooTest.testEscaping finished, ...

> ./mill foo.assembly # 클래스파일과 라이브러리를 배포용 jar로 번들링

> ./mill show foo.assembly # assembly 태스크의 출력 경로 확인
".../out/foo/assembly.dest/out.jar"

> java -jar ./out/foo/assembly.dest/out.jar --text hello
<h1>hello</h1>
```

프로그래머블 build 파일은 [Scala로 작성](../depth/why-scala.html)되지만, 읽고 쓰기 위해 Scala를 깊이 알아야 하는 것은 아니다. Gradle의 Groovy나 Maven의 XML처럼, Mill을 쓰기 위한 최소한의 Scala 문법만 익히면 된다. 유연성이 필요 없는 단순한 빌드라면 YAML 기반 [Declarative Module 정의](module-config.html#_declarative_configuration)가 더 나은 선택이다.

---

## 8. 커스텀 빌드 로직

프로그래머블 빌드는 빌드 그래프의 일부를 커스텀 로직으로 오버라이드하기 쉽다. 아래 예제는 `JavaModule`의 `resources`(기본은 `resources/` 폴더)를 오버라이드해서, 모듈 소스 파일의 총 라인 수를 담은 생성 텍스트 파일 하나를 추가로 포함시킨다.

**build.mill**

```scala
package build
import mill.*, javalib.*

object foo extends JavaModule {

  /** Total number of lines in module source files */
  def lineCount = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }

  /** Generate resources using lineCount of sources */
  override def resources = Task {
    os.write(Task.dest / "line-count.txt", "" + lineCount())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

- `override def resources`는 `JavaModule`이 제공하던 기존 resource 폴더(`resources.super`)를 대체한다. 새 태스크는 기존 리소스(`super.resources()`)와 `line-count.txt`를 담은 새 `Task.dest` 폴더를 함께 반환한다.
- `os.read.lines`, `os.write`는 Mill의 [번들 라이브러리](../fundamentals/bundled-libraries.html) 중 하나인 [OS-Lib](https://github.com/com-lihaoyi/os-lib)에서 온다. Maven Central의 어떤 JVM 라이브러리든 [//| mvnDeps](../extending/import-mvn-plugins.html)로 임포트할 수 있으므로 Mill에 번들된 것에만 제한되지 않는다.
- `override` 키워드는 선택 사항이다. 명확성을 위해 위에 표기했을 뿐 생략해도 된다.

`def`, 메서드 호출, `override`, `super` 사용법은 Java 경험이 있다면 낯설지 않다. 객체지향 프로그래밍의 익숙한 개념이 그대로 적용된다.

이렇게 생성된 `line-count.txt`는 런타임에 로드해서 사용할 수 있다.

```
> ./mill foo.run
...
Line Count: 17

> ./mill show foo.lineCount
17

> ./mill inspect foo.lineCount
foo.lineCount(build.mill:...)
    Total number of lines in module source files
Inputs:
    foo.allSourceFiles
```

오버라이드 가능한 태스크나 태스크 간 관계가 궁금하면 IDE 자동완성을 활용하거나 [mill visualize](../cli/builtin-commands.html#_visualize)로 확인할 수 있다.

`lineCount` 같은 사용자 정의 태스크도 내장 태스크와 동일한 이점을 누린다. [캐싱](../depth/caching.html)(`out/` 폴더에 저장), [병렬 처리](../depth/parallelism.html)([-j/--jobs 플래그](../cli/flags.html#_jobs_j)로 설정), `show`/`inspect`를 통한 조사 가능성 등이다. 간단한 예제에서는 크게 체감되지 않아도, 프로젝트 규모가 커질수록 커스텀 빌드 로직의 성능과 유지보수성을 보장해 준다.

---

## 9. 프로그래머블 멀티 모듈과 호환 모듈

### 멀티 모듈 프로젝트

**build.mill**

```scala
package build

import mill.*, javalib.*

trait MyModule extends JavaModule {
  object test extends JavaTests, TestModule.Junit4
}

object foo extends MyModule {
  def moduleDeps = Seq(bar)
  def mvnDeps = Seq(
    mvn"net.sourceforge.argparse4j:argparse4j:0.9.0"
  )
}

object bar extends MyModule {
  def mvnDeps = Seq(
    mvn"net.sourceforge.argparse4j:argparse4j:0.9.0",
    mvn"org.thymeleaf:thymeleaf:3.1.1.RELEASE",
    mvn"org.slf4j:slf4j-nop:2.0.7"
  )
}
```

4장의 멀티 모듈 예제를 프로그래머블 문법으로 옮긴 것이다. 단일 모듈을 정의하듯 여러 모듈을 정의하고, `def moduleDeps`로 관계를 지정한다. `foo.test`, `bar.test`처럼 모듈은 서로 중첩될 수 있다.

두 모듈에 공통인 `test` 하위 모듈 설정을 `trait MyModule`로 분리한 점에 주목한다. 이 [Trait Module](../fundamentals/modules.html)은 Java의 `class`처럼 동작하며, 공통 설정을 복사-붙여넣기하지 않고도 모듈마다 다른 `mvnDeps` 같은 설정을 유지할 수 있게 해준다. Mill 빌드에서 흔히 쓰이는 패턴이다.

```
> ./mill bar.test
Test bar.BarTests.simple finished...
Test bar.BarTests.escaping finished...

> ./mill foo.run --foo-text hello --bar-text world
Foo.value: hello
Bar.value: <h1>world</h1>
```

각 하위 모듈 설정을 해당 폴더의 `package.mill` 파일로 분리할 수도 있다([Multi-File Builds](../large/multi-file-builds.html) 참고). 큰 프로젝트에서 `build.mill`이 비대해지는 것을 막고, 모듈 설정을 코드와 가깝게 두어 가독성을 높이는 데 유용하다.

### 호환 모듈

5장의 Maven 호환 모듈 예제를 프로그래머블 `build.mill`로 옮긴 것이다. 설정 기반 `build.mill.yaml`보다 유연해서 커스텀 빌드 로직을 담을 수 있다.

**build.mill**

```scala
package build
import mill.*, javalib.*

object foo extends MavenModule {
  object test extends MavenTests, TestModule.Junit4
  object integration extends MavenTests, TestModule.Junit4
}
```

```
> ./mill foo.compile
compiling 1 Java source...

> ./mill foo.test.compile
compiling 1 Java source...

> ./mill foo.test.testForked
...foo.FooTests.hello ...

> ./mill foo.test
...foo.FooTests.hello ...

> ./mill foo.integration
...foo.FooIntegrationTests.hello ...
```

---

## 10. 실무 예제: 퍼블리싱까지 포함한 멀티 모듈 빌드

라이브러리 의존성, 테스트, 퍼블리싱, 코드 생성을 모두 아우르는 조금 더 현실적인 빌드 예제다.

**build.mill**

```scala
package build
import mill.*, javalib.*, publish.*

trait MyModule extends JavaModule, PublishModule {
  def publishVersion = "0.0.1"

  def pomSettings = PomSettings(
    description = "Hello",
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/example",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "example"),
    developers = Seq(Developer("lihaoyi", "Li Haoyi", "https://github.com/lihaoyi"))
  )

  def mvnDeps = Seq(mvn"org.thymeleaf:thymeleaf:3.1.1.RELEASE")

  object test extends JavaTests, TestModule.Junit4
}

object foo extends MyModule {
  def moduleDeps = Seq(bar, qux)

  def generatedSources = Task {
    os.write(
      Task.dest / "Version.java",
      s"""
         |package foo;
         |public class Version {
         |    public static String value() {
         |        return "${publishVersion()}";
         |    }
         |}
      """.stripMargin
    )
    Seq(PathRef(Task.dest))
  }
}

object bar extends MyModule {
  def moduleDeps = Seq(qux)
}

object qux extends MyModule
```

지금까지 다룬 개념을 조합한 구성이다.

- 서로 의존하는 세 개의 `JavaModule` (`foo`, `bar`, `qux`)
- 단위 테스트와 퍼블리싱 설정
- `publishVersion`을 문자열로 코드에 포함시키기 위한 소스 생성 (`generatedSources`)

이런 멀티 모듈 빌드에서는 [쿼리](../cli/query-syntax.html)를 활용해 여러 모듈에 한 번에 태스크를 실행하는 것이 편리하다.

```
__.test
__.publishLocal
```

`trait`를 이용해 자주 함께 쓰이는 모듈 조합을 묶을 수 있다는 점도 다시 확인할 수 있다. `MyModule`은 공통 설정을 가진 `JavaModule`일 뿐 아니라, 그 안에 자체 설정을 가진 `object test`도 함께 정의한다. 반복되는 모듈 구조를 관리하는 데 매우 유용한 기법이다.

```
> ./mill resolve __.run
bar.run
bar.test.run
foo.run
foo.test.run
qux.run

> ./mill foo.run
foo version 0.0.1
Foo.value: <h1>hello</h1>
Bar.value: <p>world</p>
Qux.value: 31337

> ./mill bar.test
...bar.BarTests.test ...

> ./mill qux.run
Qux.value: 31337

> ./mill __.compile

> ./mill __.test
...bar.BarTests.test ...
...foo.FooTests.test ...

> ./mill __.publishLocal
Publishing com.lihaoyi:foo:0.0.1 to ivy repo...
Publishing com.lihaoyi:bar:0.0.1 to ivy repo...
Publishing com.lihaoyi:qux:0.0.1 to ivy repo...
...

> ./mill show foo.assembly # mac/linux
".../out/foo/assembly.dest/out.jar"

> ./out/foo/assembly.dest/out.jar # mac/linux
foo version 0.0.1
Foo.value: <h1>hello</h1>
Bar.value: <p>world</p>
Qux.value: 31337
```
