# 태스크 그래프와 스코프

> 원본: https://www.scala-sbt.org/1.x/docs/Task-Graph.html, https://www.scala-sbt.org/1.x/docs/Scopes.html, https://www.scala-sbt.org/1.x/docs/Appending-Values.html, https://www.scala-sbt.org/1.x/docs/Scope-Delegation.html

---

## 목차

1. [태스크 그래프 (Task Graph)](#1-태스크-그래프-task-graph)
2. [스코프 (Scopes)](#2-스코프-scopes)
3. [값 이어붙이기 (Appending Values)](#3-값-이어붙이기-appending-values)
4. [스코프 위임 (Scope Delegation)](#4-스코프-위임-scope-delegation)

---

## 1. 태스크 그래프 (Task Graph)

### 1.1 빌드 정의를 그래프로 보기

sbt의 빌드 정의는 키-값 쌍의 목록이 아니라 **선행 관계(happens-before)를 나타내는 방향성 비순환 그래프(DAG)** 다. `build.sbt`에 나열된 항목들은 각각 독립된 세팅처럼 보이지만, 실제로는 서로를 참조하며 하나의 그래프를 구성한다.

| 용어 | 의미 |
|------|------|
| Setting/Task 표현식 | `.settings(...)` 안에 나열되는 각 항목 |
| Key | 표현식의 좌변. `SettingKey[A]`, `TaskKey[A]`, `InputKey[A]` 중 하나 |
| Setting | `SettingKey[A]`로 정의되며, 값이 빌드 로드 시점에 **한 번만** 계산된다 |
| Task | `TaskKey[A]`로 정의되며, 값이 호출될 때마다 **다시** 계산된다 |

### 1.2 `.value`로 의존성 선언하기

다른 태스크나 세팅의 값을 참조하려면 `.value` 메서드를 쓴다. 이 메서드는 `:=`, `+=`, `++=`의 우변(태스크/세팅 본문) 안에서만 쓸 수 있고, 일반 메서드 호출이 아니라 매크로로 처리된다. 매크로는 `.value` 호출을 본문 블록 진입 이전 시점으로 끌어올려서, 실제 코드에서 어느 줄에 적혀 있든 `{` 블록이 시작되기 전에 그 의존 태스크가 이미 완료되도록 만든다.

```scala
scalacOptions := {
  val ur = update.value  // update 태스크가 scalacOptions보다 먼저 끝난다
  val x = clean.value    // clean 태스크도 scalacOptions보다 먼저 끝난다
  // ---- 여기부터 scalacOptions 본문 ----
  ur.allConfigurations.take(3)
}
```

`update`와 `clean`은 본문 안에서 코드 순서상 앞뒤에 있지만 실행 순서는 보장되지 않는다. 두 태스크는 서로 독립적이므로 병렬로 실행될 수 있다.

`streams`를 이용해 로그를 남기는 예제로 이 시점 차이를 더 분명히 볼 수 있다.

```scala
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    scalacOptions := {
      val out = streams.value // streams 태스크가 먼저 끝난다
      val log = out.log
      log.info("123")
      val ur = update.value   // update 태스크도 먼저 끝난다
      log.info("456")
      ur.allConfigurations.take(3)
    }
  )
```

sbt 셸에서 `scalacOptions`를 실행하면 다음과 같이 출력된다.

```
> scalacOptions
[info] Updating {file:/xxx/}root...
[info] Resolving jline#jline;2.14.1 ...
[info] Done updating.
[info] 123
[info] 456
```

`log.info("123")`과 `log.info("456")` 사이에 코드상 `update.value`가 있지만, 실제로는 `update` 태스크 실행 결과("Updating...", "Done updating.")가 두 로그보다 먼저 나타난다. `update`가 본문 진입 전에 이미 완료됐기 때문이다.

조건문 안에 `.value`를 넣어도 같은 원리가 적용된다.

```scala
lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    scalacOptions := {
      val ur = update.value  // update가 먼저 끝난다
      if (false) {
        val x = clean.value  // 이 블록은 실행되지 않지만 clean은 실행된다
      }
      ur.allConfigurations.take(3)
    }
  )
```

`if (false)`로 감싸 실제로는 실행되지 않을 코드처럼 보여도, `clean.value`가 선언하는 의존성은 무시되지 않는다. `clean` 태스크가 실제로 동작해 `target/scala-2.12/classes/` 디렉터리가 지워진다. `.value` 호출을 변수에 담지 않고 인라인으로 써도 마찬가지로 본문 진입 전에 평가된다.

```scala
scalacOptions := {
  val x = clean.value
  update.value.allConfigurations.take(3)
}
```

### 1.3 의존 관계 들여다보기: `inspect`

`inspect` 명령으로 특정 키가 어떤 태스크에 의존하는지 확인할 수 있다.

```
> inspect scalacOptions
[info] Task: scala.collection.Seq[java.lang.String]
[info] Description:
[info]  Options for the Scala compiler.
[info] Dependencies:
[info]  *:clean
[info]  *:update
```

`inspect tree`는 의존성 체인을 트리 형태로 펼쳐서 보여준다.

```
> inspect tree compile
[info] compile:compile = Task[sbt.inc.Analysis]
[info]   +-compile:incCompileSetup = Task[sbt.Compiler$IncSetup]
[info]   | +-*/*:skip = Task[Boolean]
[info]   | +-compile:compileAnalysisFilename = Task[java.lang.String]
[info]   | | +-*/*:crossPaths = true
[info]   | | +-{.}/*:scalaBinaryVersion = 2.12
[info]   | |
[info]   | +-*/*:compilerCache = Task[xsbti.compile.GlobalsCache]
[info]   | +-*/*:definesClass = Task[scala.Function1[java.io.File, scala.Function1[java.lang.String, Boolean]]]
[info]   | +-compile:dependencyClasspath = Task[scala.collection.Seq[sbt.Attributed[java.io.File]]]
```

### 1.4 태스크와 세팅 사이의 의존 방향

태스크는 세팅 값을 참조할 수 있다.

```scala
lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    organization := "com.example",
    scalaVersion := "2.12.18",
    version := "0.1.0-SNAPSHOT",
    scalacOptions := List("-encoding", "utf8", "-Xfatal-warnings", "-deprecation", "-unchecked"),
    scalacOptions := {
      val old = scalacOptions.value
      scalaBinaryVersion.value match {
        case "2.12" => old
        case _      => old filterNot (Set("-Xfatal-warnings", "-deprecation").apply)
      }
    }
  )
```

```
> show scalacOptions
[info] * -encoding
[info] * utf8
[info] * -Xfatal-warnings
[info] * -deprecation
[info] * -unchecked

> ++2.11.8!
[info] Forcing Scala version to 2.11.8 on all projects.

> show scalacOptions
[info] * -encoding
[info] * utf8
[info] * -unchecked
```

세팅끼리도 서로 참조할 수 있다.

```scala
// organization을 프로젝트 이름으로 세팅 (둘 다 SettingKey[String])
organization := name.value
```

```scala
Compile / scalaSource := {
  val old = (Compile / scalaSource).value
  scalaBinaryVersion.value match {
    case "2.11" => baseDirectory.value / "src-2.11" / "main" / "scala"
    case _      => old
  }
}
```

다만 방향은 한쪽으로만 열려 있다. 세팅은 로드 시점에 딱 한 번 계산되므로, 세팅 값을 계산하는 도중 매번 재계산되는 태스크를 끌어다 쓸 수 없다.

```scala
// 허용: 태스크가 세팅 값을 참조
scalacOptions := checksums.value

// 컴파일 오류: 세팅이 태스크를 참조할 수 없다
checksums := scalacOptions.value
```

### 1.5 흐름 기반 빌드 정의가 유리한 이유

sbt의 `build.sbt` DSL은 Make(1976), Ant(2000), Rake(2003)로 이어지는 빌드 도구 계보 위에 있다. Make는 파일 대상과 의존성을 선언하고, Rake는 셸 명령 대신 프로그래밍 언어로 액션을 기술한다는 점에서 한 단계 나아갔다.

```makefile
CC=g++
CFLAGS=-Wall

all: hello

hello: main.o hello.o
    $(CC) main.o hello.o -o hello

%.o: %.cpp
    $(CC) $(CFLAGS) -c $< -o $@
```

```ruby
task name: [:prereq1, :prereq2] do |t|
  # actions (may reference prereq as t.name etc)
end
```

sbt는 여기서 한 걸음 더 나아가 Scala 표현식 자체로 DAG를 구성한다. 이런 흐름 기반 구조가 주는 이점은 다음과 같다.

- **중복 제거**: 여러 태스크가 같은 태스크(예: `Compile / compile`)에 의존해도 그 태스크는 정확히 한 번만 실행된다.
- **병렬 처리**: 서로 독립적인 태스크는 그래프상 병렬로 스케줄될 수 있다.
- **관심사 분리와 재사용**: 빌드 작성자는 태스크를 원하는 방식으로 연결하고, sbt와 플러그인은 컴파일이나 의존성 해석 같은 기능을 재사용 가능한 함수로 제공한다.

---

## 2. 스코프 (Scopes)

### 2.1 왜 스코프가 필요한가

같은 키라도 문맥에 따라 다른 값을 가질 수 있어야 한다. 여러 서브프로젝트가 있으면 프로젝트마다 값이 다를 수 있고, `compile`은 메인 소스와 테스트 소스에서 다르게 동작해야 하며, `packageOptions`는 `packageBin`(jar 패키징)과 `packageSrc`(소스 패키징)에서 서로 다른 값을 가질 수 있어야 한다. 그래서 sbt의 키는 **스코프가 지정된 상태에서만 유일한 값을 갖는다**. 이 스코프를 결정하는 세 축을 이해하는 것이 sbt 설정 이해의 핵심이다.

### 2.2 스코프 축 세 가지

| 축 | 의미 | 예시 값 |
|----|------|---------|
| 서브프로젝트 축(subproject) | 빌드 내 어느 프로젝트인지 | `root`, `core`, `ThisBuild`(빌드 전체) |
| 구성 축(configuration) | 어떤 의존성 구성인지 | `Compile`(`src/main/scala`), `Test`(`src/test/scala`), `Runtime` |
| 태스크 축(task) | 어떤 태스크의 하위 설정인지 | `packageBin`, `packageSrc`, `packageDoc` |

세 축의 조합이 하나의 스코프를 이루며, 각 축은 `Zero`라는 특수값(보편적 대체값)을 가질 수 있다. 세 축이 모두 `Zero`인 스코프를 `Global`이라고 부른다. 즉 `Global`은 `Zero / Zero / Zero`의 축약형이다.

### 2.3 빌드 정의에서 스코프 지정하기

스코프를 명시하지 않으면 다음 기본값이 적용된다: 현재 서브프로젝트, 구성 축은 `Zero`, 태스크 축은 `Zero`.

```scala
lazy val root = (project in file("."))
  .settings(
    name := "hello"
  )
```

우변에서 다른 키를 참조할 때도 같은 기본 스코프가 적용된다.

```scala
organization := name.value
```

`/` 연산자로 스코프를 명시적으로 지정할 수 있다.

```scala
Compile / name := "hello"
```

```scala
packageBin / name := "hello"
```

```scala
Compile / packageBin / name := "hello"
```

```scala
Global / concurrentRestrictions := Seq(
  Tags.limitAll(1)
)
```

### 2.4 sbt 셸에서 스코프 지정하기

셸에서 키를 입력할 때의 전체 문법은 다음과 같다.

```
ref / Config / intask / key
```

| 요소 | 의미 | 생략 시 |
|------|------|---------|
| `ref` | 서브프로젝트 참조. `<project-id>`, `ProjectRef(...)`, `ThisBuild` | 현재 프로젝트로 추론 |
| `Config` | 대문자로 시작하는 설정 이름 | 키에 맞는 값으로 자동 추론 |
| `intask` | 태스크 축 | `Zero`(태스크 없음)로 추론 |
| `key` | 실제 키 이름 | (생략 불가) |

```
fullClasspath
```
기본 스코프 사용: 현재 프로젝트, 키에 맞게 추론된 설정, 태스크 축은 `Zero`.

```
Test / fullClasspath
```
구성 축을 `Test`로 명시.

```
root / fullClasspath
```
서브프로젝트를 `root`로 명시.

```
root / Zero / fullClasspath
```
서브프로젝트는 명시하고, 구성 축은 자동 추론 대신 `Zero`로 강제.

```
doc / fullClasspath
```
태스크 축을 `doc`으로 지정.

```
ProjectRef(uri("file:/tmp/hello/"), "root") / Test / fullClasspath
```
완전한 형태의 프로젝트 참조.

```
ThisBuild / version
```
빌드 전체 스코프.

```
Zero / fullClasspath
```
서브프로젝트 축까지 `Zero`.

```
root / Compile / doc / fullClasspath
```
세 축을 모두 명시.

### 2.5 `inspect`로 스코프 확인하기

```
$ sbt
sbt:Hello> inspect Test / fullClasspath
```

`inspect` 결과에는 다음 항목들이 표시된다.

- **Task**: 반환 타입
- **Provided by**: 실제로 값을 정의한 스코프
- **Defined at**: 정의된 소스 위치
- **Dependencies**: 이 태스크가 필요로 하는 다른 키들
- **Delegates**: 값이 없을 때 대신 찾아볼 후보 스코프 목록 (4장에서 상세히 다룬다)

### 2.6 스코프를 명시해야 하는 경우

다음과 같은 경우에는 스코프를 반드시 명시해야 한다.

- "Reference to undefined setting" 오류가 발생할 때
- 원래 특정 스코프에서만 정의되는 키를 다룰 때 (예: `compile`은 본래 `Compile`이나 `Test` 설정에서만 존재)

주의할 점은, 아무 스코프도 지정하지 않고 `compile := ...`라고 쓰면 기존 `Compile / compile`이나 `Test / compile`을 덮어쓰는 게 아니라 `Zero` 구성 축에 새로운 태스크를 정의하게 된다는 것이다. 기존 컴파일 동작을 바꾸려면 `Compile / compile := ...` 또는 `Test / compile := ...`처럼 명시적으로 구성 축을 지정해야 한다.

### 2.7 빌드 레벨 설정

빌드 전체에 적용할 값은 `ThisBuild` 스코프에 둔다.

```scala
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    publish / skip := true
  )

lazy val core = (project in file("core"))
  .settings(
    // 다른 설정들
  )

lazy val util = (project in file("util"))
  .settings(
    // 다른 설정들
  )
```

여러 항목에 `ThisBuild /`를 반복해서 붙이는 대신 `inThisBuild(...)` 헬퍼로 한 번에 감쌀 수 있다. 단, 빌드 레벨 설정에는 순수한 값이나 `Global`/`ThisBuild` 스코프를 참조하는 설정만 담아야 한다.

---

## 3. 값 이어붙이기 (Appending Values)

### 3.1 `:=` / `+=` / `++=`의 차이

시퀀스 타입 키는 기존 값을 완전히 갈아치우지 않고 뒤에 이어붙일 수 있다.

| 연산자 | 동작 |
|--------|------|
| `:=` | 기존 값을 무시하고 완전히 교체 |
| `+=` | 시퀀스에 원소 하나를 추가 |
| `++=` | 시퀀스에 다른 시퀀스를 이어붙임 |

```scala
Compile / sourceDirectories += file("source")
```

```scala
Compile / sourceDirectories ++= Seq(file("sources1"), file("sources2"))
```

```scala
Compile / sourceDirectories := Seq(file("sources1"), file("sources2"))
```

`+=`, `++=`는 내부적으로 기존 값을 읽어와 뒤에 이어붙이는 태스크로 확장된다는 점에서 `:=`와 근본적으로 다르다(4.6절 예제 F에서 이 확장 형태를 다시 다룬다).

### 3.2 정의되지 않은 값 참조 시 주의점

`+=`, `++=`도 결국 기존 값을 참조하므로, 참조 대상이 어떤 스코프에서도 정의돼 있지 않으면 "Reference to undefined setting" 오류가 난다. 이때 스코프 위임 규칙(4장)에 따라 상위 스코프의 값을 찾아 대체하게 된다.

### 3.3 `Def.task`로 다른 키 값을 계산해 추가하기

단순 리터럴이 아니라 다른 키 값에 기반한 결과를 추가하고 싶다면 `Def.task`로 감싼다.

```scala
Compile / sourceGenerators += Def.task {
  myGenerator(baseDirectory.value, (Compile / managedClasspath).value)
}
```

### 3.4 의존 값을 활용한 동적 추가

```scala
cleanFiles += file("coverage-report-" + name.value + ".txt")
```

프로젝트 이름(`name.value`)을 읽어 파일 경로를 동적으로 구성한 뒤 `cleanFiles` 목록에 추가하는 예다. 이렇게 `+=`/`++=`는 정적인 상수뿐 아니라 다른 설정·태스크 값을 조합한 결과도 자연스럽게 이어붙일 수 있게 해준다.

---

## 4. 스코프 위임 (Scope Delegation)

### 4.1 위임이 필요한 이유

특정 스코프에 값이 정의돼 있지 않을 때, sbt는 곧바로 오류를 내지 않고 **더 일반적인 스코프**를 순서대로 찾아본다. 이 폴백 검색 경로를 스코프 위임이라 부른다. 위임 덕분에 한 번 넓은 스코프에 설정해둔 값을 여러 구체적 스코프가 공통으로 물려받을 수 있다.

### 4.2 다섯 가지 위임 규칙

**Rule 1 — 축 간 우선순위**

세 축의 우선순위는 서브프로젝트 축 > 구성 축 > 태스크 축 순이다. 여러 후보가 동시에 매치될 때 이 순서로 더 구체적인 쪽을 채택한다.

**Rule 2 — 태스크 축 위임**

주어진 태스크 축부터 시작해 `Zero`까지 순서대로 검색한다.

```scala
lazy val projA = (project in file("a"))
  .settings(
    name := {
      "foo-" + (packageBin / scalaVersion).value
    },
    scalaVersion := "2.11.11"
  )
```

`packageBin / scalaVersion`은 `projA / Zero / packageBin / scalaVersion`으로 확장되지만 이 자리에 값이 없다. Rule 2에 따라 태스크 축을 `Zero`로 바꿔 `projA / scalaVersion`을 찾으면 `"2.11.11"`이 발견된다. 결과는 `"foo-2.11.11"`.

**Rule 3 — 구성 축 위임**

주어진 설정에서 시작해 그 부모, 그 부모의 부모 순으로 올라가다가 마지막에 `Zero`까지 검색한다. sbt 기본 설정 상속 체인은 `Test → Runtime → Compile`이다.

```scala
lazy val foo = settingKey[Int]("")
lazy val bar = settingKey[Int]("")

lazy val projX = (project in file("x"))
  .settings(
    foo := {
      (Test / bar).value + 1
    },
    Compile / bar := 1
  )
```

`Test / bar`는 정의돼 있지 않지만, Rule 3에 의해 `Runtime / bar`, `Compile / bar` 순으로 검색해 `Compile / bar = 1`을 찾는다. 결과는 `foo = 2`.

**Rule 4 — 서브프로젝트 축 위임**

주어진 서브프로젝트에서 시작해 `ThisBuild`, 그다음 `Zero` 순으로 검색한다.

```scala
ThisBuild / organization := "com.example"

lazy val projB = (project in file("b"))
  .settings(
    name := "abc-" + organization.value,
    organization := "org.tempuri"
  )
```

Rule 4에 따라 `projB / organization`을 먼저 찾으면 `"org.tempuri"`가 있으므로 이 값을 쓴다(`ThisBuild` 값보다 우선). 결과는 `"abc-org.tempuri"`.

**Rule 5 — 위임된 스코프는 독립적으로 평가된다**

위임된 스코프의 키와 그 키가 의존하는 설정/태스크는 원래 호출 지점의 문맥을 그대로 이어받지 않고, 위임 대상 스코프 자신의 문맥에서 평가된다. 객체지향의 동적 디스패치(this가 호출자 문맥을 따라가는 방식)와는 다르다는 점에 유의해야 한다.

```scala
lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.2",
      version      := scalaVersion.value + "_0.1.0"
    )),
    name := "Hello"
  )

lazy val projE = (project in file("e"))
  .settings(
    scalaVersion := "2.11.11"
  )
```

`projE / version`은 정의돼 있지 않으므로 `ThisBuild / version`으로 위임된다. 그런데 이 `ThisBuild / version`은 `scalaVersion.value`를 참조할 때도 `ThisBuild / scalaVersion`("2.12.2")을 사용하지, `projE`에서 재정의한 `"2.11.11"`을 사용하지 않는다. 결과는 `"2.12.2_0.1.0"`이다. 이것이 Rule 5가 말하는 "원래 호출 문맥을 갖고 가지 않는다"는 의미다.

### 4.3 축 우선순위가 실제로 갈리는 경우

```scala
ThisBuild / packageBin / scalaVersion := "2.12.2"

lazy val projC = (project in file("c"))
  .settings(
    name := {
      "foo-" + (packageBin / scalaVersion).value
    },
    scalaVersion := "2.11.11"
  )
```

`packageBin / scalaVersion`을 찾을 때 두 후보가 동시에 나타난다.

- Rule 2(태스크 축 위임)로 `projC / Zero / packageBin`이 없으니 `projC / Zero / Zero`("2.11.11")
- Rule 4(서브프로젝트 축 위임)로 `ThisBuild / Zero / packageBin`("2.12.2")

Rule 1에 따라 서브프로젝트 축이 설정/태스크 축보다 우선순위가 높으므로, "더 구체적인 서브프로젝트를 유지한" `projC / Zero / Zero`가 채택되어 `"2.11.11"`이 선택된다. 결과는 `"foo-2.11.11"`.

### 4.4 더 복잡한 조합

```scala
ThisBuild / scalacOptions += "-Ywarn-unused-import"

lazy val projD = (project in file("d"))
  .settings(
    test := {
      println((Compile / console / scalacOptions).value)
    },
    console / scalacOptions -= "-Ywarn-unused-import",
    Compile / scalacOptions := scalacOptions.value
  )
```

`Compile / console / scalacOptions`를 찾을 때 세 후보가 경쟁한다.

- Rule 2: `projD / Compile / Zero`
- Rule 3: `projD / Zero / console`
- Rule 4: `ThisBuild / Zero / Zero`

Rule 1(서브프로젝트 축 우선)에 따라 `projD / Compile / Zero`가 채택된다. 이 값은 `Compile / scalacOptions := scalacOptions.value`로 정의돼 있고, 우변의 `scalacOptions.value`는 다시 `ThisBuild / scalacOptions`로 위임되어 `"-Ywarn-unused-import"`를 담고 있다. 결과는 `List(-Ywarn-unused-import)`.

### 4.5 `inspect`로 위임 경로 확인하기

```
sbt:projd> inspect projD / Compile / console / scalacOptions
[info] Task: scala.collection.Seq[java.lang.String]
[info] Provided by:
[info]  ProjectRef(uri("file:/tmp/projd/"), "projD") / Compile / scalacOptions
[info] Delegates:
[info]  projD / Compile / console / scalacOptions
[info]  projD / Compile / scalacOptions
[info]  projD / console / scalacOptions
[info]  projD / scalacOptions
[info]  ThisBuild / Compile / console / scalacOptions
[info]  ThisBuild / Compile / scalacOptions
[info]  ThisBuild / console / scalacOptions
[info]  ThisBuild / scalacOptions
[info]  Zero / Compile / console / scalacOptions
[info]  Zero / Compile / scalacOptions
[info]  Zero / console / scalacOptions
[info]  Global / scalacOptions
```

`Delegates` 목록의 순서 자체가 위임 규칙을 그대로 보여준다. 서브프로젝트 축은 `projD → ThisBuild → Zero` 순으로, 그 안에서 구성 축은 `Compile → Zero` 순으로, 다시 그 안에서 태스크 축은 `console → (태스크 없음)` 순으로 정렬된다. `Provided by`에 표시된 항목이 실제로 값을 준 스코프다.

### 4.6 `+=` 확장과 위임이 함께 작동하는 예

```scala
ThisBuild / scalacOptions += "-D0"
scalacOptions += "-D1"

lazy val projF = (project in file("f"))
  .settings(
    compile / scalacOptions += "-D2",
    Compile / scalacOptions += "-D3",
    Compile / compile / scalacOptions += "-D4",
    test := {
      println("bippy" + (Compile / compile / scalacOptions).value.mkString)
    }
  )
```

`+=`는 실제로 다음과 같은 형태로 확장된다.

```scala
someKey := {
  val old = someKey.value
  old :+ "x"
}
```

이 확장 규칙과 위임 규칙을 겹쳐서 적용하면 각 대입이 순서대로 다음처럼 진행된다.

1. `ThisBuild / scalacOptions += "-D0"`: 초기값 `Nil`에 `"-D0"`을 추가 → `List("-D0")`
2. `scalacOptions += "-D1"`(스코프 미지정, 프로젝트 루트 기준): 이 자리의 이전 값이 없으므로 Rule 4로 `ThisBuild / scalacOptions`(`List("-D0")`)에 위임 → `"-D1"` 추가 → `List("-D0", "-D1")`
3. `compile / scalacOptions += "-D2"`: 역시 `ThisBuild / scalacOptions`로 위임되어 `List("-D0", "-D1", "-D2")`가 되지만, 이후 `Compile / scalacOptions` 재정의로 덮어써진다
4. `Compile / scalacOptions += "-D3"`: 이 스코프의 이전 값도 없으므로 Rule 3·4로 `ThisBuild / scalacOptions`(`List("-D0")`)에 위임 → `"-D3"` 추가 → `List("-D0", "-D3")`
5. `Compile / compile / scalacOptions += "-D4"`: 이번엔 Rule 1·2로 같은 프로젝트의 `projF / Compile / scalacOptions`(`List("-D0", "-D3")`)에 위임 → `"-D4"` 추가 → `List("-D0", "-D3", "-D4")`

`test`가 참조하는 `Compile / compile / scalacOptions`의 최종 값은 `List("-D0", "-D3", "-D4")`이므로 실행 결과는 다음과 같다.

```
"bippy-D0-D3-D4"
```

이 예제는 `+=`의 확장 규칙과 스코프 위임 규칙이 각 대입 시점마다 독립적으로 적용된다는 것, 그리고 나중에 더 구체적인 스코프에 값을 다시 설정하면 이전 위임 경로를 덮어쓴다는 것을 함께 보여준다.
