# sbt의 Task, Command, State

> 원본: https://www.scala-sbt.org/1.x/docs/Tasks.html, https://www.scala-sbt.org/1.x/docs/Caching.html, https://www.scala-sbt.org/1.x/docs/Input-Tasks.html, https://www.scala-sbt.org/1.x/docs/Commands.html, https://www.scala-sbt.org/1.x/docs/Parsing-Input.html, https://www.scala-sbt.org/1.x/docs/Build-State.html, https://www.scala-sbt.org/1.x/docs/Task-Inputs.html

---

## 목차

1. [Setting과 Task, InputTask의 구분](#1-setting과-task-inputtask의-구분)
2. [Task 정의 문법](#2-task-정의-문법)
3. [Task 그래프는 프로젝트 로드 시점에 고정된다](#3-task-그래프는-프로젝트-로드-시점에-고정된다)
4. [Def.taskDyn과 Def.sequential](#4-deftaskdyn과-defsequential)
5. [실패 처리: failure, result, andFinally](#5-실패-처리-failure-result-andfinally)
6. [Task 캐싱: Cache/Tracked API](#6-task-캐싱-cachetracked-api)
7. [InputTask와 Def.inputTask](#7-inputtask와-definputtask)
8. [Parser 콤비네이터](#8-parser-콤비네이터)
9. [Command API](#9-command-api)
10. [Build State와 State 모나드](#10-build-state와-state-모나드)
11. [Mill과의 개념 대응](#11-mill과의-개념-대응)

---

## 1. Setting과 Task, InputTask의 구분

sbt의 빌드 정의는 세 종류의 키(key)로 구성된다. 이름은 비슷해 보이지만 평가 시점과 의존성 결정 시점이 서로 다르다.

| 구분 | 타입 | 평가 시점 | 의존성 그래프 | 예 |
|------|------|-----------|----------------|-----|
| Setting | `SettingKey[T]` | 프로젝트 로드 시 한 번 평가되고 고정 | 로드 시점에 확정 | `scalaVersion`, `name` |
| Task | `TaskKey[T]` | 명령을 내릴 때마다(요청 시) 실행 | 실행마다 재구성될 수 있음(동적 태스크 가능) | `compile`, `test` |
| InputTask | `InputKey[T]` | 명령줄 문자열을 파싱한 뒤 그 결과로 결정되는 Task를 실행 | Task와 동일하되 파서가 선행됨 | `run`, `testOnly` |

Setting은 Scala의 `val`에 가깝고, Task는 매번 다시 계산될 수 있는 `def`에 가깝다. 다만 Task 자체는 결과를 캐시하지 않는 것이 기본 동작이며(실행할 때마다 본문이 돈다), 어떤 값을 캐시하고 싶으면 6장에서 다루는 Cache/Tracked API를 별도로 써야 한다. 이 점이 Mill의 `Task { }`(자동으로 값 캐싱)와 가장 크게 갈리는 지점이다.

Key는 `build.sbt`에서 다음처럼 선언한다.

```scala
lazy val sampleTask = taskKey[Int]("설명")
lazy val demo = inputKey[Unit]("A demo input task.")
```

`taskKey[T]`의 `T`는 해당 Task가 만들어내는 값의 타입이고, 코드상의 변수 이름이 곧 sbt 콘솔/CLI에서 그 Task를 호출하는 이름이 된다.

## 2. Task 정의 문법

가장 단순한 형태는 상수식이나 부수효과를 그대로 대입하는 것이다.

```scala
intTask := 1 + 2
stringTask := System.getProperty("user.name")
```

다른 Task나 Setting의 값을 참조할 때는 `.value`를 붙인다. `.value`는 Task 본문 안에서만 쓸 수 있는 특수한 메서드로, sbt 매크로가 이 호출을 분석해 의존성 그래프의 엣지를 만든다.

```scala
sampleTask := intTask.value + 1
```

특정 구성의 값을 참조하려면 스코프를 슬래시로 붙인다.

```scala
Test / sampleTask := (Compile / intTask).value * 3
```

`Initialize[Task[T]]` 형태로 로직을 분리해 두고 나중에 키에 대입할 수도 있다.

```scala
lazy val intTaskImpl: Initialize[Task[Int]] =
  Def.task { sampleTask.value - 3 }

intTask := intTaskImpl.value
```

기존 Task 값을 참조하면서 확장(`intTask := intTask.value + 1`)할 수도 있고, 본문을 통째로 새로 써서 재정의할 수도 있다.

여러 프로젝트/구성에 걸친 값을 한 번에 모으고 싶을 때는 `ScopeFilter`를 쓴다.

```scala
val filter = ScopeFilter(
  inProjects(core, util),
  inConfigurations(Compile)
)

sources := {
  val allSources: Seq[Seq[File]] = sources.all(filter).value
  allSources.flatten
}
```

`:=` 연산자는 우선순위가 매우 낮으므로, 다른 연산자와 섞어 쓸 때는 괄호를 명시해야 한다.

```scala
// helloTask.:=( "echo Hello".! ) 로 해석되도록 괄호가 필요
helloTask := { "echo Hello" ! }
```

## 3. Task 그래프는 프로젝트 로드 시점에 고정된다

sbt의 Task 그래프는 기본적으로 프로젝트를 로드할 때 정적으로 결정된다. Task 본문 안에서 `otherTask.value`를 호출하면, sbt는 이 매크로 호출 자체를 정적으로 분석해 "이 Task는 otherTask에 의존한다"는 그래프 엣지를 만든다. 즉 `if` 조건 안에서 `.value` 호출을 서로 다르게 배치하더라도, 실행 흐름과 무관하게 두 브랜치에 등장하는 모든 `.value` 의존성이 그래프에 미리 잡힌다.

이 정적 분석 방식 덕분에 sbt는 Task를 실행하기 전에 전체 의존 그래프를 미리 계산해 병렬 실행 계획을 세울 수 있다. 반대로 "런타임에 확인해 봐야 어떤 Task를 실행할지 알 수 있는" 경우에는 이 정적 분석 방식을 그대로 쓸 수 없고, 4장에서 다루는 `Def.taskDyn`이 필요하다.

sbt 1.4.0부터는 아래처럼 최상위(top-level)에 있는 `if` 표현식은 특별히 인식해서, 분기마다 실제로 선택된 쪽의 의존성만 실행하도록 최적화한다.

```scala
bar := {
  if (number.value < 0) negAction.value
  else if (number.value == 0) zeroAction.value
  else posAction.value
}
```

이 문법은 `Def.taskDyn`과 달리 `inspect` 같은 정적 조회 명령과도 잘 호환된다는 점이 문서에서 강조된다.

## 4. Def.taskDyn과 Def.sequential

`Def.taskDyn`은 실행 결과에 따라 어떤 Task를 다음에 실행할지 런타임에 결정해야 할 때 쓴다. 정적 분석 대상이 되는 것은 `taskDyn` 블록 자체에서 조건 판단에 쓰인 값(`stringTask`)뿐이고, 분기 안에서 선택된 Task(`intTask`)는 실제로 그 분기가 선택될 때만 로드된다.

```scala
val dynamic = Def.taskDyn {
  if (stringTask.value == "dev")
    Def.task { 3 }
  else
    Def.task { intTask.value + 5 }
}

myTask := {
  val num = dynamic.value
  println(s"Number: $num")
}
```

`Def.taskDyn`으로 만들어지는 그래프는 순환 참조를 만들 수 없다는 제약이 있다.

여러 Task를 순서대로, 그리고 앞선 Task가 실패하면 즉시 중단하는 방식으로 실행하고 싶을 때는 `Def.sequential`을 쓴다.

```scala
lazy val compilecheck = taskKey[Unit]("compile then scalastyle")

Compile / compilecheck := Def.sequential(
  Compile / compile,
  (Compile / scalastyle).toTask("")
).value
```

## 5. 실패 처리: failure, result, andFinally

Task 실행 중 예외가 나면 sbt는 이를 `Incomplete`라는 실패 값으로 감싼다. 이 실패를 다루는 세 가지 방법이 있다.

| 메서드 | 동작 |
|--------|------|
| `.failure` | 원래 Task가 실패하면 `Incomplete` 값을 정상값처럼 돌려받고, 원래 Task가 성공하면 오히려 이쪽이 실패로 뒤집힌다 |
| `.result` | `Result[T]`(성공/실패를 모두 포함하는 값)로 감싸서 반환하므로 항상 성공한다 |
| `andFinally { ... }` | 성공/실패 여부와 무관하게 정리 코드를 실행한다 |

```scala
intTask.result.value match {
  case Inc(inc: Incomplete) => println("Failed: " + inc); 3
  case Value(v)             => println("Success: " + v); v
}
```

```scala
lazy val intTaskImpl = intTask andFinally {
  println("andFinally")
}
```

Task 안에서 로그를 남기려면 `streams.value`로 얻는 `TaskStreams`를 사용한다. `myTask / logLevel := Level.Debug`로 로그 레벨을 조정할 수 있고, `last myTask` 명령으로 마지막 실행의 로그를 다시 볼 수 있다.

## 6. Task 캐싱: Cache/Tracked API

sbt의 Task는 기본적으로 결과를 캐시하지 않는다. Mill처럼 "입력이 같으면 이전 결과를 그대로 쓴다"는 동작을 원한다면, `sbt.util` 패키지가 제공하는 별도의 캐싱 유틸리티를 Task 본문 안에서 직접 조합해서 써야 한다. sbt에는 이 목적의 전용 키워드(`Def.cachedTask` 같은 것)가 따로 있는 것이 아니라, 아래 API들을 태스크 작성자가 조합하는 방식이다.

| API | 역할 |
|-----|------|
| `Cache.cached(store)(f)` | 키 타입 `I`, 값 타입 `O`인 단순 캐시. 같은 입력이면 저장된 값을 재사용 |
| `TaskKey#previous` | 이전 실행에서 만든 값을 `Option[A]`로 돌려줌. 첫 실행은 `None` |
| `Tracked.lastOutput` | 마지막 출력값만 저장. 입력이 바뀌어도 무효화하지 않으므로 그 자체만으로는 부족 |
| `Tracked.inputChanged` | 입력이 이전과 달라졌는지를 `Boolean` 플래그로 알려줌 |
| `Tracked.outputChanged` | 작업 완료 후 결과 파일에 스탬프를 남겨, 다음 실행에서 불필요한 재작업을 막음 |
| `Tracked.diffInputs` / `Tracked.diffOutputs` | 단순 불린이 아니라 `ChangeReport[T]`(added/removed/modified/checked 집합)를 반환해 증분 처리를 가능하게 함 |
| `FileInfo.exists` / `lastModified` / `hash` / `full` | 파일을 어떤 속성으로 추적할지 선택 (존재 여부, 수정 시각, SHA-1 해시, 또는 둘 다) |
| `FileFunction.cached(cacheDir)(...)` | 파일 집합을 입출력으로 삼는 함수를 up-to-date 검사로 감싸는 헬퍼. 기본은 입력을 `lastModified`, 출력을 `exists`로 추적 |

입력 변경과 출력 변경을 동시에 챙기고 싶으면 `inputChanged` 콜백 안에 `lastOutput`을 중첩해서 쓰는 패턴이 일반적이다. 캐시 무효화는 결국 "입력값 해시/타임스탬프가 달라졌는가"와 "출력 파일 상태가 기대와 다른가"라는 두 조건의 조합으로 판단한다.

## 7. InputTask와 Def.inputTask

InputTask는 사용자가 명령줄에 입력한 문자열을 파싱한 뒤, 그 파싱 결과로부터 실행할 Task를 만들어내는 메커니즘이다. Task와 달리 반드시 `Parser`를 거친다는 점이 핵심 차이다.

```scala
class InputTask[T](val parser: State => Parser[Task[T]])
```

즉 InputTask는 "State로부터 Parser를 만들고, 그 Parser가 최종적으로 Task를 만들어낸다"는 두 단계 구조를 갖는다. Setting만으로 파서를 구성할 수도 있고(`Initialize[Parser[I]]`), State까지 참조해야 하는 더 복잡한 파서도 만들 수 있다(`Initialize[State => Parser[I]]`).

가장 단순한 InputTask는 `spaceDelimited`로 공백 구분 인자를 그대로 받는 것이다.

```scala
import complete.DefaultParsers._

val demo = inputKey[Unit]("A demo input task.")

demo := {
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  println("Scala version: " + scalaVersion.value)
  args foreach println
}
```

`spaceDelimited`는 탭 완성 기능이 제한적이므로, 정교한 자동완성이 필요하면 8장의 Parser 콤비네이터를 직접 조합해야 한다.

| 메서드 | 역할 |
|--------|------|
| `.parsed` | Parser가 만든 값을 InputTask 본문에서 꺼냄 |
| `.evaluated` | 다른 Task를 InputTask 안에서 실행하고 그 결과값을 얻음 |
| `.toTask(" 인자 문자열")` | InputTask를 고정 입력이 미리 채워진 일반 Task처럼 다룸 |
| `.fullInput(문자열)` / `.partialInput(문자열)` | 입력을 코드로 미리 채워 넣되, 추가 입력을 막을지(`full`) 허용할지(`partial`) 선택 |

```scala
val run2 = inputKey[Unit]("Run main class twice with -- separator")

run2 := {
  val one = (Compile / run).evaluated
  val sep = separator.parsed
  val two = (Compile / run).evaluated
}
```

`run2 a b -- c d`처럼 호출하면 첫 번째 `run` 실행에는 "a b"가, 두 번째 실행에는 "c d"가 전달된다.

## 8. Parser 콤비네이터

sbt는 커맨드와 InputTask의 사용자 입력을 처리하기 위해 자체 Parser 콤비네이터 라이브러리(`sbt.internal.util.complete`, 흔히 `Parser[T]`)를 제공한다. 가장 기본적인 형태로 보면 `Parser[T]`는 문자열을 받아 성공 시 값을, 실패 시 실패 정보를 돌려주는 함수와 같다.

기본 콤비네이터는 다음과 같다.

```scala
// 리터럴
'x'          // 문자 하나
"blue"       // 문자열 완전 일치
charClass(_.isDigit, "digit")   // 조건 기반 매칭

// 결합
"fg" ~ " " ~ "color"     // 순차 결합 (튜플로 결과 모음)
"fg" ~> color            // 좌측 버리고 우측만
color <~ Space           // 우측 버리고 좌측만
"blue" | "green"         // 선택

// 반복
p.+   // 1회 이상
p.*   // 0회 이상
p.?   // 0 또는 1회

// 결과 변환
digits map { chars => chars.mkString.toInt }

// 항상 성공/항상 실패
success(3)
failure("메시지")
```

탭 완성 후보는 `.examples("0", "1", "2")`처럼 명시적으로 지정할 수 있고, `token(...)`으로 감싸면 자동완성 제안의 경계가 어디까지인지를 sbt에게 알려줄 수 있다(중첩해서 쓰면 안 된다).

앞선 Parser의 결과에 따라 다음 Parser 자체를 다르게 구성해야 할 때는 `flatMap`을 쓴다. 예를 들어 이미 선택한 항목을 제외한 나머지 항목만 파싱하도록 재귀적으로 구성하는 패턴이 대표적이다.

Command와 InputTask 양쪽 모두 이 Parser 값을 받아서 사용자 입력 문자열 전체를 해석한다는 점에서 두 메커니즘의 파싱 계층은 동일한 라이브러리를 공유한다.

## 9. Command API

Command는 Task와 겉모습은 비슷하지만("sbt 콘솔에서 실행할 수 있는 이름 붙은 작업") 받는 것과 돌려주는 것이 완전히 다르다. Task가 특정 설정값들을 조합해 결과값 하나를 만든다면, Command는 빌드 전체의 `State`를 받아 새로운 `State`를 돌려주는 함수다.

```
파서: State => Parser[T]
액션: (State, T) => State
```

Command는 이 두 함수의 조합으로 만들어진다. State 전체에 접근할 수 있으므로, 등록된 커맨드 목록을 바꾸거나 다른 프로젝트의 설정을 조회/수정하는 등 Task로는 할 수 없는 일을 할 수 있다.

| 생성 방법 | 인자 형태 | 액션 시그니처 |
|-----------|-----------|----------------|
| `Command.command("name")(action)` | 인자 없음 | `State => State` |
| `Command.single("name")(action)` | 단일 인자 | `(State, String) => State` |
| `Command.args("name", "<arg>")(action)` | 공백 구분 다중 인자 | `(State, Seq[String]) => State` |
| `Command("name")(parser)(action)` | 커스텀 Parser | `(State, T) => State` |

```scala
val hello = Command.command("hello") { state =>
  println("Hi!")
  state
}
```

정의한 Command는 `build.sbt`에서 `commands` 세팅에 등록해야 인식된다.

```scala
lazy val root = (project in file("."))
  .settings(
    commands ++= Seq(hello, helloAll, changeColor)
  )
```

Command와 Task의 차이를 정리하면 다음과 같다.

| 구분 | Task | Command |
|------|------|---------|
| 매개변수 | 관련 설정값들 | 빌드 전체의 `State` |
| 반환값 | 계산된 결과값 | 변형된 새 `State` |
| 상태 접근 범위 | 제한적(자신이 의존하는 값만) | 전체 접근 가능 |
| 용도 | 컴파일, 테스트 등 일반 빌드 작업 | 등록된 커맨드 목록 변경, 세션 조작 등 메타 작업 |

간단한 별칭이 필요할 뿐이라면 Command를 새로 만들지 않고 `addCommandAlias("c", "compile")`처럼 기존 커맨드 시퀀스에 이름을 붙이는 방법도 있다.

## 10. Build State와 State 모나드

sbt 콘솔의 실행 모델은 `State => State` 함수의 체인으로 볼 수 있다. 초기 State에서 시작해, 대기 중인 커맨드를 하나씩 꺼내 실행하면서 매번 새로운 State로 치환하고, `remainingCommands`가 빌 때까지 이 과정을 반복한다. 각 단계의 State가 다음 단계로 그대로 전달된다는 점에서 커맨드 실행 흐름은 State 모나드와 유사한 구조를 갖는다.

`State`는 불변 값이며 주요 필드/메서드는 다음과 같다.

| 필드/메서드 | 내용 |
|-------------|------|
| `definedCommands` | 현재 등록된 모든 Command 정의 |
| `remainingCommands` | 앞으로 실행할 커맨드 목록 |
| `attributes` | 임의의 데이터를 담는 `AttributeMap` |
| `history` | 실행한 커맨드 이력 |

State를 직접 바꾸는 것이 아니라 `copy()`로 새 인스턴스를 만드는 방식으로 변형한다.

```scala
// 실행할 커맨드를 뒤에 추가
state.copy(remainingCommands = state.remainingCommands :+ "cleanup")

// 다음에 바로 실행되도록 앞에 끼워 넣기
state.copy(remainingCommands = "next" +: state.remainingCommands)

// 실패 상태로 표시
if (success) state else state.fail
```

임의의 값을 State에 얹어 다른 커맨드/Task와 공유하고 싶을 때는 `AttributeKey`와 `AttributeMap`을 쓴다.

```scala
val counter = AttributeKey[Int]("counter")
state.put(counter, value)   // 값 설정 (새 State 반환)
state.get(counter)          // 값 조회
```

프로젝트에 설정된 값을 State로부터 읽어오려면 `Project.extract`로 `Extracted`를 얻는다.

```scala
val extracted: Extracted = Project.extract(state)
```

`Extracted`는 현재 빌드/프로젝트 참조, 초기화된 설정 데이터(`structure.data`), 세션 설정, 빌드 파일을 평가하는 `Eval` 인스턴스 등에 접근하는 통로가 된다. 프로젝트 데이터 자체는 `sbt.Settings[Scope]` 타입인 `structure.data`에 저장되며, 특정 키의 값은 `(key in scope).get(structure.data)` 형태로 조회한다.

Task 안에서도 `state.value`로 현재 State를 읽을 수 있고, `Project.runTask()`를 이용하면 커맨드나 다른 Task 안에서 특정 Task를 직접 실행시킬 수도 있다.

## 11. Mill과의 개념 대응

sbt와 Mill은 둘 다 "빌드는 태스크 그래프"라는 철학을 공유하지만, 세부 구현은 서로 다른 축으로 나뉜다.

| 개념 | sbt | Mill |
|------|-----|------|
| 값이 자동으로 캐시되는 단위 | 없음(Task는 기본적으로 매번 실행) | `Task { ... }` |
| 캐싱을 직접 구성해야 하는 경우 | Cache/Tracked API를 태스크 안에서 조합 | 대부분 자동, `Task.Persistent`로 폴더 유지 |
| CLI에서 호출할 때마다 실행 | Task 전반 + Command | `Task.Command` |
| 커맨드라인 인자를 받는 단위 | `InputTask`(Parser 기반) | `Task.Command`(MainArgs 기반) |
| 빌드 전체 상태를 다루는 단위 | `Command`(`State => State`) | 별도 대응 없음(모듈/Task 그래프로 대체) |
| 그래프 결정 시점 | 프로젝트 로드 시 정적 분석(`.value`), 예외적으로 `Def.taskDyn` | 매 실행 시 `foo()` 호출로 그래프 구성 |

가장 크게 다른 지점은 두 가지다. 첫째, sbt의 Task는 기본적으로 비캐시이고 캐싱은 라이브러리 차원에서 선택적으로 붙이는 반면, Mill의 `Task { }`는 캐싱이 기본값이다. 둘째, sbt에는 빌드 전체 State를 조작하는 Command라는 별도의 계층이 있어서 Parser 콤비네이터로 임의의 커맨드라인 문법을 정의할 수 있지만, Mill에는 이에 대응하는 개념이 없고 대신 Task/Module 그래프 자체로 대부분의 요구를 처리한다.
