# 전역 설정, Java 혼합 빌드, 파일 매핑, 로컬 Scala

> 원본: https://www.scala-sbt.org/1.x/docs/Global-Settings.html , https://www.scala-sbt.org/1.x/docs/Java-Sources.html , https://www.scala-sbt.org/1.x/docs/Mapping-Files.html , https://www.scala-sbt.org/1.x/docs/Local-Scala.html

---

## 목차

1. [전역 설정 (~/.sbt)](#1-전역-설정-home사sbt)
2. [Java 소스와 혼합 빌드](#2-java-소스와-혼합-빌드)
3. [파일 매핑 (Mapping Files)](#3-파일-매핑-mapping-files)
4. [로컬 커스텀 Scala 배포판 사용](#4-로컬-커스텀-scala-배포판-사용)

---

## 1. 전역 설정 (~/.sbt)

sbt는 특정 프로젝트에 국한되지 않고 사용자의 모든 프로젝트에 공통으로 적용할 설정을 `$HOME/.sbt` 아래에 둘 수 있게 지원한다. 방법은 두 가지다.

| 방법 | 위치 | 특징 |
|---|---|---|
| 전역 설정 파일 | `$HOME/.sbt/1.0/global.sbt` | 단순 키-값 설정 위주 |
| 전역 플러그인 | `$HOME/.sbt/1.0/plugins/` | 코드(AutoPlugin)까지 포함해 모든 프로젝트에 주입 |

### 1.1 기본 전역 설정 파일

`$HOME/.sbt/1.0/global.sbt`에 작성한 설정은 sbt로 빌드하는 모든 프로젝트에 적용된다. 셸 프롬프트를 커스터마이징하는 예제는 다음과 같다.

```scala
shellPrompt := { state =>
  "sbt (%s)> ".format(Project.extract(state).currentProject.id)
}
```

`$HOME/.sbt/1.0/plugins/`에 추가한 플러그인이 제공하는 설정 키도 이 파일에서 그대로 사용할 수 있다. 다만 전역 파일에서는 프로젝트별 `build.sbt`와 달리 이름이 자동으로 스코프에 잡히지 않으므로, 플러그인이 제공하는 키는 정규화된(fully-qualified) 이름으로 참조해야 한다.

```scala
com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys.withSource := true
```

### 1.2 전역 플러그인을 이용한 전역 설정

`$HOME/.sbt/1.0/plugins/` 디렉터리는 그 자체로 하나의 플러그인 프로젝트처럼 동작한다. 이 디렉터리에 `build.sbt`를 두고 플러그인을 추가하면,

```scala
addSbtPlugin("org.example" % "plugin" % "1.0")
```

여기서 정의한 설정과 코드는 사용자가 빌드하는 **모든** 프로젝트에 적용된다. 단순 설정 파일과 달리, 이 디렉터리에는 `AutoPlugin`을 직접 작성해 넣을 수도 있다.

```scala
// ShellPrompt.scala
object ShellPrompt extends AutoPlugin {
  override def trigger = allRequirements
  override def projectSettings = Seq(
    shellPrompt := { state =>
      "sbt (%s)> ".format(Project.extract(state).currentProject.id) }
  )
}
```

`trigger = allRequirements`로 지정했으므로 이 플러그인은 별도 설정 없이 모든 프로젝트에 자동 적용된다.

`$HOME/.sbt/1.0/plugins/` 디렉터리는 개별 프로젝트의 `project/` 디렉터리와 사실상 동일한 위치를 가진다. 즉 여기서 정의한 설정과 코드는 마치 모든 프로젝트의 `project/` 디렉터리 안에 있는 것처럼 취급된다. 이 성질을 이용하면 새 플러그인을 정식으로 퍼블리시하기 전에 전역 플러그인 디렉터리에서 먼저 실험해볼 수 있다.

### 1.3 활용 정리

- 개인 편의 설정(프롬프트, 로깅 레벨 등)은 `global.sbt`에 둔다.
- 모든 프로젝트에서 공통으로 쓰고 싶은 플러그인(코드 포매터, 이클립스/인텔리제이 연동 등)은 `plugins/` 디렉터리에 추가한다.
- 전역 설정은 팀 전체가 공유하는 저장소 빌드 설정과는 별개로, 개인 로컬 환경에만 적용된다는 점에 유의한다.

---

## 2. Java 소스와 혼합 빌드

sbt는 Scala 전용 빌드 도구가 아니라 Java 소스도 함께 컴파일할 수 있는 혼합 빌드 도구다. 별도 설정 없이도 관례적인 디렉터리 구조를 따르는 Java 소스가 자동으로 인식된다.

| 디렉터리 | 대상 태스크 |
|---|---|
| `src/main/java` | `compile` |
| `src/test/java` | `test:compile` |

### 2.1 javac 옵션 지정

Java 컴파일러(`javac`)에 전달할 옵션은 `javacOptions` 키로 설정한다.

```scala
javacOptions += "-g:none"
```

여러 개의 옵션(특히 옵션과 그 인자가 쌍을 이루는 경우)은 시퀀스로 한 번에 추가한다.

```scala
javacOptions ++= Seq("-source", "1.5")
```

### 2.2 컴파일 순서 제어: compileOrder

Scala와 Java 소스가 섞여 있을 때 어느 쪽을 먼저 컴파일할지는 `compileOrder` 키로 제어한다. `CompileOrder`가 가질 수 있는 값은 다음 세 가지다.

| 값 | 의미 |
|---|---|
| `CompileOrder.Mixed` (기본값) | Scala와 Java를 함께 컴파일러에 넘겨 순환 의존까지 지원 |
| `CompileOrder.JavaThenScala` | Java 소스를 먼저 컴파일한 뒤 Scala 소스를 컴파일 |
| `CompileOrder.ScalaThenJava` | Scala 소스를 먼저 컴파일한 뒤 Java 소스를 컴파일 |

```scala
compileOrder := CompileOrder.JavaThenScala
```

컴파일 순서는 설정 범위(configuration scope)별로 다르게 지정할 수도 있다. 예를 들어 메인 소스는 `JavaThenScala`로, 테스트 소스는 기본값인 `Mixed`로 유지하려면 다음과 같이 작성한다.

```scala
Compile / compileOrder := CompileOrder.JavaThenScala
Test / compileOrder := CompileOrder.Mixed
```

### 2.3 혼합 컴파일에서 알려진 문제

Scala 컴파일러와 `javac`를 한 번에 돌리는 `Mixed` 모드에서는 다음과 같은 상호운용 제약이 알려져 있다.

- Scala 컴파일러가 Java 쪽에서 정의한, 리터럴이 아닌 상수(non-literal constant) 값을 인식하지 못하는 경우가 있다.
- 그런 값을 어노테이션의 인자로 사용하면 컴파일이 거부될 수 있다.
- Scala 2.11.4 이후 버전에서 Java 어노테이션에 붙은 `@Retention` 메타 어노테이션을 인식하지 못하는 문제가 보고된 바 있다.

이런 문제가 발생하면 `compileOrder`를 `JavaThenScala`나 `ScalaThenJava`로 바꿔 두 컴파일러를 분리 실행하는 방식으로 우회할 수 있다.

### 2.4 Java 전용 프로젝트로 좁히기

프로젝트에 Scala 소스가 전혀 없는 순수 Java 프로젝트라면, 굳이 존재하지도 않는 `src/main/scala` 디렉터리를 소스 경로 목록에 포함시킬 이유가 없다. `unmanagedSourceDirectories`를 Java 소스 디렉터리만으로 재설정하면 된다.

```scala
Compile / unmanagedSourceDirectories := (Compile / javaSource).value :: Nil
Test / unmanagedSourceDirectories := (Test / javaSource).value :: Nil
```

이렇게 하면 소스 디렉터리 탐색 범위가 명확해지고, 불필요한 Scala 컴파일러 초기화 비용도 줄어든다.

---

## 3. 파일 매핑 (Mapping Files)

`package`, `packageSrc`, `packageDoc` 같은 태스크는 산출물을 만들 때 입력 파일과, 그 파일이 결과 아티팩트(jar 등) 안에서 가질 상대 경로를 짝지은 `Seq[(File, String)]` 타입의 값을 필요로 한다. 이런 짝을 **매핑(mapping)** 이라 부른다. sbt는 `PathFinder`와 `Path` 객체가 제공하는 몇 가지 메서드로 이 매핑 시퀀스를 손쉽게 구성할 수 있게 해준다.

핵심은 `pair` 메서드다. `Seq[File]`에 `pair`를 호출하면서 매핑 함수(`File => Option[String]` 또는 `File => Option[File]`)를 인자로 넘기면 `Seq[(File, String)]`(또는 `Seq[(File, File)]`)이 만들어진다.

### 3.1 relativeTo: 기준 디렉터리 상대 경로로 변환

`relativeTo(baseDirectories)`는 각 파일을 주어진 기준 디렉터리들 중 하나를 기준으로 한 상대 경로로 바꾼다. 기준 디렉터리가 여러 개이면 파일을 포함하는 첫 번째 디렉터리를 사용한다.

```scala
import Path.relativeTo

val files: Seq[File] = file("/a/b/C.scala") :: Nil
val baseDirectories: Seq[File] = file("/a") :: Nil

val mappings: Seq[(File, String)] = files pair relativeTo(baseDirectories)

val expected = (file("/a/b/C.scala") -> "b/C.scala") :: Nil
```

### 3.2 rebase: 상대 경로로 바꾼 뒤 새 접두사 붙이기

`rebase(baseDirectories, prefix)`는 `relativeTo`처럼 상대 경로를 구한 다음, 그 앞에 새로운 접두사를 덧붙인다. 접두사는 문자열일 수도 있고 `File`일 수도 있다.

문자열 접두사(결과가 `String` 매핑):

```scala
import Path.rebase

val files: Seq[File] = file("/a/b/C.scala") :: Nil
val baseDirectories: Seq[File] = file("/a") :: Nil

val mappings: Seq[(File, String)] = files pair rebase(baseDirectories, "pre/")

val expected = (file("/a/b/C.scala") -> "pre/b/C.scala") :: Nil
```

`File` 접두사(결과가 `File` 매핑, 즉 파일을 다른 디렉터리 트리로 복사할 때 유용):

```scala
val newBase: File = file("/new/base")
val mappings: Seq[(File, File)] = files pair rebase(baseDirectories, newBase)

val expected = (file("/a/b/C.scala") -> file("/new/base/b/C.scala")) :: Nil
```

### 3.3 flat: 디렉터리 구조를 무시하고 파일명만 사용

`flat`은 원래 경로 정보를 버리고 파일명만 남긴다. 여러 디렉터리에 흩어진 파일을 한 디렉터리에 모아 담을 때 사용한다.

```scala
import Path.flat

val files: Seq[File] = file("/a/b/C.scala") :: Nil
val mappings: Seq[(File, String)] = files pair flat

val expected = (file("/a/b/C.scala") -> "C.scala") :: Nil
```

`File` 대상 디렉터리를 지정하는 변형(`flat(newBase)`)도 있다.

```scala
val newBase: File = file("/new/base")
val mappings: Seq[(File, File)] = files pair flat(newBase)

val expected = (file("/a/b/C.scala") -> file("/new/base/C.scala")) :: Nil
```

### 3.4 대안 조합: `|` 연산자

여러 매핑 전략 중 앞의 전략이 실패(해당 파일에 대해 `None` 반환)했을 때 다음 전략으로 넘어가도록 `|` 연산자로 이어붙일 수 있다.

```scala
import Path.relativeTo

val files: Seq[File] = file("/a/b/C.scala") :: file("/zzz/D.scala") :: Nil
val baseDirectories: Seq[File] = file("/a") :: Nil

val mappings: Seq[(File, String)] = files pair (relativeTo(baseDirectories) | flat)
```

위 예에서 `C.scala`는 `/a` 아래에 있으므로 `relativeTo`가 성공해 `"b/C.scala"`로 매핑되지만, `D.scala`는 `/zzz` 아래에 있어 `relativeTo`가 실패하고 `flat`으로 넘어가 `"D.scala"`로 매핑된다.

### 3.5 정리

| 함수 | 입력 | 동작 |
|---|---|---|
| `relativeTo(bases)` | 기준 디렉터리 목록 | 파일을 포함하는 첫 기준 디렉터리에 대한 상대 경로 생성 |
| `rebase(bases, prefix)` | 기준 디렉터리 목록 + 접두사 | 상대 경로를 구한 뒤 접두사를 붙임 |
| `flat` / `flat(newBase)` | (없음) / 대상 디렉터리 | 경로를 버리고 파일명만 사용 |
| `a \| b` | 매핑 함수 두 개 | `a`가 실패한 파일에 한해 `b` 적용 |

이 함수들을 조합하면 `package` 계열 태스크의 `mappings` 키를 원하는 아티팩트 레이아웃에 맞춰 세밀하게 구성할 수 있다.

---

## 4. 로컬 커스텀 Scala 배포판 사용

빌드 도구가 저장소에서 내려받은 표준 Scala 배포판이 아니라, 로컬에 직접 빌드해둔 커스텀 Scala 배포판으로 프로젝트를 컴파일하고 싶은 경우가 있다(예: Scala 컴파일러나 표준 라이브러리 자체를 수정하며 개발할 때). sbt는 이를 위해 `scalaHome` 설정을 제공한다.

### 4.1 scalaHome 설정

`scalaHome`은 `Option[File]` 타입의 설정 키이며, 로컬 Scala 배포판이 설치된 디렉터리를 가리키면 된다.

```scala
scalaHome := Some(file("/path/to/scala"))
```

이 설정을 켜면 sbt는 저장소에서 Scala 아티팩트를 내려받는 대신, 지정한 경로에 있는 컴파일러와 라이브러리 jar들을 사용해 빌드를 수행한다.

### 4.2 제약 사항

- `scalaHome`을 지정하면 이 값이 `scalaVersion` 설정보다 우선하며, `scalaVersion`은 사실상 무시된다.
- 로컬 Scala 배포판을 사용하는 프로젝트는 여러 Scala 버전을 대상으로 하는 크로스 빌드(`+` 명령, `crossScalaVersions`)와 함께 쓸 수 없다. 크로스 빌드는 저장소에서 여러 버전의 표준 아티팩트를 받아오는 방식으로 동작하는데, `scalaHome`은 항상 고정된 하나의 로컬 배포판만 가리키기 때문이다.

### 4.3 대화형 세션에서 변경 사항 반영하기

로컬 Scala 배포판의 소스를 수정하고 다시 빌드한 뒤, 이미 실행 중인 sbt 대화형 세션에 그 변경을 반영하려면 클래스 로더를 새로 고쳐야 한다. `reload` 명령을 사용한다.

```
> reload
```

`reload`를 실행하면 sbt가 클래스패스를 다시 읽어들이므로, 방금 재컴파일한 로컬 Scala 배포판의 최신 결과물이 이후 컴파일 작업에 곧바로 반영된다.
