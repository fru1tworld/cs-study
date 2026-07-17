# 태스크 실행 순서 제어하기

> 원본: https://www.scala-sbt.org/1.x/docs/Howto-Sequencing.html, https://www.scala-sbt.org/1.x/docs/Howto-Sequential-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-Dynamic-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-After-Input-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-Dynamic-Input-Task.html, https://www.scala-sbt.org/1.x/docs/Howto-Sequence-using-Commands.html

---

## 목차

1. [개요: 왜 순서 제어가 어려운가](#1-개요-왜-순서-제어가-어려운가)
2. [Def.sequential로 순차 실행하기](#2-defsequential로-순차-실행하기)
3. [Def.taskDyn으로 동적 태스크 만들기](#3-deftaskdyn으로-동적-태스크-만들기)
4. [input task 실행 이후 작업 이어붙이기](#4-input-task-실행-이후-작업-이어붙이기)
5. [Def.inputTaskDyn으로 dynamic input task 만들기](#5-definputtaskdyn으로-dynamic-input-task-만들기)
6. [커맨드로 시퀀싱하기](#6-커맨드로-시퀀싱하기)

---

## 1. 개요: 왜 순서 제어가 어려운가

sbt의 `build.sbt`는 명령어를 순서대로 나열하는 스크립트가 아니라 **태스크 사이의 의존성 그래프를 선언하는 DSL**이다. 예를 들어 다음 코드는 "Y가 X 다음에 실행된다"가 아니라 "Y는 X에 의존한다"는 선언이다.

```scala
taskY := {
  val x = taskX.value
  x + 1
}
```

이는 일반적인 Scala의 명령형 코드와 다르다.

```scala
def foo(): Unit = {
  doX()
  doY()
}
```

의존성 그래프 모델을 쓰는 이유는 크게 두 가지다.

| 이점 | 설명 |
|------|------|
| 병렬화 | 서로 의존 관계가 없는 태스크는 가능한 경우 병렬로 실행된다 |
| 중복 제거 | `Compile / compile` 같은 태스크는 한 커맨드 실행당 한 번만 평가되고 재사용된다 |

문제는 이 모델이 "A를 실행한 다음 B를 실행하라" 같은 명령형 순서 제어와는 결이 다르다는 점이다. 순서를 강제로 지정해야 하는 경우 sbt는 다음과 같은 도구를 제공하며, 이는 사실상 병렬화·중복 제거라는 기본 이점을 일부 포기하고 명시적 순서를 얻는 트레이드오프다.

| 도구 | 용도 |
|------|------|
| `Def.sequential` | 여러 태스크를 순서대로, 앞선 태스크가 실패하면 중단하며 실행 |
| `Def.taskDyn` | 태스크 실행 도중 값을 보고 다음에 실행할 태스크를 동적으로 결정 |
| input task 뒤에 코드 잇기 | `.evaluated`를 이용해 input task 실행 후 부수효과 코드 실행 |
| `Def.inputTaskDyn` | input task의 파싱 결과에 따라 동적으로 다음 태스크를 결정 |
| 커맨드(`Command`) | 태스크 그래프 밖에서 상태(`State`)를 직접 조작하며 순차 실행 |

이어지는 절에서 각각을 다룬다.

---

## 2. Def.sequential로 순차 실행하기

`Def.sequential`은 sbt 0.13.8에서 도입되었으며, 여러 태스크를 "준-순차적(semi-sequential)" 의미로 실행한다. 앞의 태스크가 실패하면 뒤의 태스크는 실행되지 않는다는 점이 일반적인 태스크 의존성 선언과 다르다.

```scala
// build.sbt
lazy val compilecheck = taskKey[Unit]("compile and then scalastyle")

lazy val root = (project in file("."))
  .settings(
    Compile / compilecheck := Def.sequential(
      Compile / compile,
      (Compile / scalastyle).toTask("")
    ).value
  )
```

`compilecheck`를 실행하면 `Compile / compile`이 먼저 실행되고, 컴파일이 성공한 경우에만 `Compile / scalastyle`이 이어서 실행된다.

특징을 정리하면 다음과 같다.

- **순서 보장**: 나열한 순서 그대로 실행된다.
- **실패 시 중단**: 앞 태스크가 실패하면 뒤 태스크는 실행되지 않는다. 일반적인 태스크 그래프에서는 여러 업스트림이 병렬로 평가되고 실패 처리 방식도 다르지만, `Def.sequential`은 명시적으로 "차례로, 실패하면 멈춘다"는 의미를 부여한다.
- **`.value` 필요**: `Def.sequential(...)`은 그 자체로 `Initialize[Task[A]]`이므로 최종적으로 `.value`를 호출해 결과를 꺼내야 한다.
- `Compile / scalastyle` 같은 input task는 `.toTask("")`로 인자 없이 태스크화한 뒤에 시퀀스에 넣는다.

`Def.sequential`은 "순서만" 강제할 뿐, 태스크 실행 도중 얻은 값을 보고 다음에 실행할 태스크를 바꾸지는 못한다. 그런 동적 분기가 필요하면 3장의 `Def.taskDyn`을 쓴다.

---

## 3. Def.taskDyn으로 동적 태스크 만들기

`Def.taskDyn`은 `Def.sequential`에서 한 단계 더 나아간 도구다. `Def.task`는 순수한 값 `A`를 반환하는 반면, `Def.taskDyn`은 `sbt.Def.Initialize[sbt.Task[A]]`, 즉 "또 다른 태스크"를 반환한다. 이 덕분에 태스크 엔진이 그 반환된 태스크를 이어서 평가할 수 있고, 결과적으로 실행 중간에 얻은 값을 바탕으로 다음에 무엇을 실행할지 동적으로 결정할 수 있다.

```scala
lazy val compilecheck = taskKey[sbt.inc.Analysis]("compile and then scalastyle")

lazy val root = (project in file("."))
  .settings(
    compilecheck := (Def.taskDyn {
      val c = (Compile / compile).value
      Def.task {
        val x = (Compile / scalastyle).toTask("").value
        c
      }
    }).value
  )
```

바깥쪽 `Def.taskDyn` 블록에서 `(Compile / compile).value`로 컴파일을 먼저 실행하고, 그 결과 `c`를 안쪽 `Def.task` 클로저 안에서 활용한다. 안쪽 블록에서 `scalastyle`을 실행한 뒤 컴파일 결과 `c`를 최종값으로 반환한다.

기존 키를 그대로 재정의하는 것도 가능하다. `Compile / compile`과 같은 반환 타입(`Analysis`)을 갖는 동적 태스크라면 아예 `Compile / compile` 자체를 다시 바인딩할 수 있다.

```scala
Compile / compile := (Def.taskDyn {
  val c = (Compile / compile).value
  Def.task {
    val x = (Compile / scalastyle).toTask("").value
    c
  }
}).value
```

이렇게 하면 쉘에서 그냥 `compile`만 입력해도 내부적으로 `scalastyle` 검사까지 자동으로 딸려 실행된다. `Def.sequential`과의 차이를 표로 정리하면 다음과 같다.

| 비교 | `Def.sequential` | `Def.taskDyn` |
|------|-------------------|----------------|
| 반환값 | 마지막 태스크의 결과 | 클로저 안에서 자유롭게 조합한 값 |
| 동적 분기 | 불가 (고정된 태스크 목록) | 가능 (이전 값에 따라 다음 태스크 선택) |
| 실패 처리 | 앞 태스크 실패 시 중단 | 일반 태스크와 동일한 실패 전파 |
| 대표 사용처 | 단순 순차 파이프라인 | 조건부·값 기반 순서 제어 |

---

## 4. input task 실행 이후 작업 이어붙이기

`Compile / run`처럼 인자를 받는 input task를 실행한 다음, 예를 들어 브라우저를 여는 것과 같은 후속 작업을 붙이고 싶은 경우가 있다. 이때 핵심은 input task 참조에 `.evaluated`를 붙여 그 결과를 먼저 평가시키고, 그 뒤에 이어지는 코드를 부수효과로 실행하는 것이다.

예제 애플리케이션(`src/main/scala/Greeting.scala`):

```scala
object Greeting {
  def main(args: Array[String]): Unit = {
    println("hello " + args.toList)
  }
}
```

**방법 1: 새 input task 정의**

```scala
lazy val runopen = inputKey[Unit]("run and then open the browser")

lazy val root = (project in file("."))
  .settings(
    runopen := {
      (Compile / run).evaluated
      println("open browser!")
    }
  )
```

**방법 2: 기존 태스크 재정의**

```scala
lazy val root = (project in file("."))
  .settings(
    Compile / run := {
      (Compile / run).evaluated
      println("open browser!")
    }
  )
```

`> runopen foo`를 실행하면 다음 순서로 출력된다.

1. 컴파일 로그
2. 애플리케이션 실행 결과: `hello List(foo)`
3. 후속 부수효과: `open browser!`

여기서 `.evaluated`는 "이 input task를 지금 실행하고 그 결과값을 받아온다"는 의미이며, 이 구문이 있어야 뒤따르는 코드가 실행 이후 시점에 동작한다는 것이 보장된다. 다만 이 방식은 인자를 그대로 전달만 할 뿐, 파싱한 인자값 자체를 바탕으로 실행할 태스크를 바꾸지는 못한다. 인자에 따라 동적으로 분기해야 한다면 5장의 `Def.inputTaskDyn`이 필요하다.

---

## 5. Def.inputTaskDyn으로 dynamic input task 만들기

`Def.inputTaskDyn`은 `Def.taskDyn`의 input task 버전이다. 인자를 파싱한 뒤 그 값을 바탕으로 동적으로 다음 태스크를 결정하고, 실행이 끝나면 후속 태스크를 이어 붙일 수 있다.

**v1: 기본 접근**

```scala
lazy val runopen = inputKey[Unit]("run and then open the browser")
lazy val openbrowser = taskKey[Unit]("open the browser")

lazy val root = (project in file("."))
  .settings(
    runopen := (Def.inputTaskDyn {
      import sbt.complete.Parsers.spaceDelimited
      val args = spaceDelimited("<args>").parsed
      Def.taskDyn {
        (Compile / run).toTask(" " + args.mkString(" ")).value
        openbrowser
      }
    }).evaluated,
    openbrowser := {
      println("open browser!")
    }
  )
```

`spaceDelimited("<args>").parsed`로 커맨드라인 인자를 받은 뒤, `(Compile / run).toTask(...)`에 그 인자를 문자열로 붙여 넘기고, 실행이 끝나면 `openbrowser` 태스크를 이어서 실행한다.

**v2: 순환 참조를 피하는 권장 방식**

v1처럼 `Compile / run`을 그대로 재정의하면서 동시에 그 안에서 `Compile / run`을 호출하면 순환 참조 문제가 생길 수 있다. 이를 피하려면 실제 실행 로직을 별도 키(`actualRun`)로 분리한다.

```scala
lazy val actualRun = inputKey[Unit]("The actual run task")
lazy val openbrowser = taskKey[Unit]("open the browser")

lazy val root = (project in file("."))
  .settings(
    Compile / run := (Def.inputTaskDyn {
      import sbt.complete.Parsers.spaceDelimited
      val args = spaceDelimited("<args>").parsed
      Def.taskDyn {
        (Compile / actualRun).toTask(" " + args.mkString(" ")).value
        openbrowser
      }
    }).evaluated,
    Compile / actualRun := Defaults.runTask(
      Runtime / fullClasspath,
      Compile / run / mainClass,
      Compile / run / runner
    ).evaluated,
    openbrowser := {
      println("open browser!")
    }
  )
```

`Compile / actualRun`의 구현은 sbt 내부 `Defaults.scala`에서 그대로 가져온 것으로, `Compile / run`을 재정의하면서도 실제 실행 로직과의 순환을 끊는 역할을 한다.

주의할 점은 다음과 같다.

- `testOnly`처럼 인자 끝에 공백이 있으면 실패하는 태스크가 있으므로, `toTask`에 넘길 문자열은 `.replaceAll("\\s+$", "")`로 뒤쪽 공백을 제거해두는 편이 안전하다.
- `run foo`를 실행하면 인자를 반영한 `actualRun`이 먼저 실행되고, 그다음 `openbrowser`가 순서대로 실행된다.

---

## 6. 커맨드로 시퀀싱하기

태스크 그래프의 캐싱·병렬화보다 **부수효과와 순서 자체가 더 중요한 경우**, 예를 들어 릴리스 절차처럼 사람이 콘솔에 명령어를 하나씩 입력하는 것을 그대로 흉내 내고 싶은 경우에는 태스크 대신 커맨드(`Command`)를 쓰는 편이 적합하다.

```scala
commands += Command.command("releaseNightly") { state =>
  "stampVersion" ::
    "clean" ::
    "compile" ::
    "publish" ::
    "bintrayRelease" ::
    state
}
```

이 예제는 `releaseNightly`라는 커맨드를 새로 정의하고, 버전 스탬핑 → 클린 → 컴파일 → 퍼블리시 → bintray 릴리스 순서로 다른 커맨드들을 체이닝한다.

커맨드 시퀀싱의 특징은 다음과 같다.

| 특징 | 설명 |
|------|------|
| 동작 방식 | 태스크처럼 의존성 그래프와 캐싱을 관리하는 게 아니라, 사용자가 콘솔에 직접 입력하는 순서를 그대로 재현한다 |
| 파라미터 | `state: State`를 받아 다음에 실행할 커맨드 목록이 반영된 새 `State`를 반환한다 |
| 체이닝 | `::` 연산자로 커맨드 문자열들을 이어 붙인다 |
| 반환값 | 마지막에 갱신된 `state`를 반환해 세션이 계속 이어지도록 한다 |

이 패턴은 sbt 자신의 릴리스 절차에서도 쓰이는 방식이며, 태스크 단위의 세밀한 값 조합보다는 "이 순서대로 여러 명령을 그대로 실행하고 싶다"는 상황에 적합하다.

---

## 정리

| 상황 | 선택할 도구 |
|------|--------------|
| 그냥 순서대로, 실패하면 멈추면 됨 | `Def.sequential` |
| 이전 태스크의 결과값을 보고 다음 태스크를 정해야 함 | `Def.taskDyn` |
| input task 실행 후 단순 부수효과만 덧붙이면 됨 | `.evaluated` 뒤에 코드 추가 |
| input task의 인자값에 따라 동적으로 다음 태스크를 정해야 함 | `Def.inputTaskDyn` |
| 캐싱/병렬화보다 콘솔 입력 순서 재현이 중요함 (릴리스 절차 등) | 커맨드(`Command.command`) |
