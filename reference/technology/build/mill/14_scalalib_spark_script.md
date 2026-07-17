# Mill scalalib: Spark 예제와 Scala 단일 파일 스크립트

> 원본: https://mill-build.org/mill/scalalib/spark.html, https://mill-build.org/mill/scalalib/script.html

---

## 목차

1. [Hello Spark (Scala)](#1-hello-spark-scala)
2. [Hello PySpark (Python)](#2-hello-pyspark-python)
3. [spark-submit을 사용하는 실전에 가까운 Spark 프로젝트](#3-spark-submit을-사용하는-실전에-가까운-spark-프로젝트)
4. [Scala 단일 파일 스크립트 개요](#4-scala-단일-파일-스크립트-개요)
5. [스크립트 활용 사례](#5-스크립트-활용-사례)
6. [어셈블리와 네이티브 바이너리 패키징](#6-어셈블리와-네이티브-바이너리-패키징)
7. [스크립트 REPL 열기](#7-스크립트-repl-열기)
8. [스크립트 moduleDeps](#8-스크립트-moduledeps)
9. [상대/절대 경로 스크립트 moduleDeps](#9-상대절대-경로-스크립트-moduledeps)
10. [프로젝트 모듈에 대한 moduleDeps](#10-프로젝트-모듈에-대한-moduledeps)
11. [커스텀 스크립트 모듈 클래스](#11-커스텀-스크립트-모듈-클래스)
12. [스크립트 리소스](#12-스크립트-리소스)
13. [커스텀 JVM 버전](#13-커스텀-jvm-버전)
14. [번들 라이브러리와 Raw 모드](#14-번들-라이브러리와-raw-모드)
15. [스크립트 IDE 연동(BSP)](#15-스크립트-ide-연동bsp)

---

## 1. Hello Spark (Scala)

`ScalaModule`에 `spark-core`, `spark-sql` 의존성을 추가하면 그대로 Spark 애플리케이션을 빌드/실행할 수 있다. JDK 9 이상에서 Spark가 내부적으로 사용하는 `sun.nio.ch` 패키지에 접근하려면 `forkArgs`에 `--add-opens` 옵션을 반드시 넣어야 한다.

```scala
// build.mill
package build

import mill.*, scalalib.*

object foo extends ScalaModule {
  def scalaVersion = "2.12.20"
  def mvnDeps = Seq(
    mvn"org.apache.spark::spark-core:3.5.4",
    mvn"org.apache.spark::spark-sql:3.5.4"
  )

  def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")

  object test extends ScalaTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.9.1")
    def testFramework = "utest.runner.Framework"

    def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  }

}
```

- `foo.test`(uTest)도 별도의 `object test extends ScalaTests`로 정의하며, 테스트 실행 시에도 Spark를 fork하므로 동일한 `forkArgs`가 필요하다.
- 실행/테스트:

```console
> ./mill foo.run
...
+-------------+
|      message|
+-------------+
|Hello, World!|
+-------------+
...

> ./mill foo.test
...
+ foo.FooTests.helloWorld should create a DataFrame with one row containing 'Hello, World!'...
...
```

---

## 2. Hello PySpark (Python)

동일한 Spark 예제를 Python(PySpark)으로 구성할 수도 있다. `pythonlib`의 `PythonModule`을 사용하고 `mainScript`로 진입점 파일을 지정한다.

```scala
// build.mill
package build

import mill.*, pythonlib.*

object foo extends PythonModule {

  def mainScript = Task.Source("src/foo.py")
  def pythonDeps = Seq("pyspark==4.0.1")

  object test extends PythonTests, TestModule.Unittest

}
```

- 테스트 프레임워크는 Python 표준 `unittest`(`TestModule.Unittest`)를 사용한다.

```console
> ./mill foo.run
...
+-------------+
|      message|
+-------------+
|Hello, World!|
+-------------+
...

> ./mill foo.test
...
test_hello_world...
...
Ran 1 test...
...
OK
...
```

---

## 3. spark-submit을 사용하는 실전에 가까운 Spark 프로젝트

인자로 받은 `transactions.csv`(없으면 리소스의 기본 파일 사용)에서 카테고리별 합계·평균·건수를 계산하는 예제다. 모듈 이름을 ``` `package` ```로 지정해 루트 모듈로 만들고, `assembly`로 fat jar를 만든 뒤 `spark-submit.sh`로 실행하는 흐름을 보여준다.

```scala
// build.mill
package build

import mill.*, scalalib.*

object `package` extends ScalaModule {
  def scalaVersion = "2.12.20"
  def mvnDeps = Seq(
    mvn"org.apache.spark::spark-core:3.5.6",
    mvn"org.apache.spark::spark-sql:3.5.6"
  )

  def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")

  def prependShellScript = ""

  object test extends ScalaTests {
    def mvnDeps = Seq(mvn"com.lihaoyi::utest:0.9.1")
    def testFramework = "utest.runner.Framework"

    def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  }

}
```

- `def prependShellScript = ""`: Mill의 기본 assembly는 실행 가능한 셸 스크립트를 jar 앞에 붙이는데, `spark-submit`은 순수 jar 포맷을 기대하므로 이를 빈 문자열로 비워 일반 jar 형태로 만든다.

```console
> ./mill run
...
Summary Statistics by Category:
+-----------+------------+--------------+-----------------+
|   category|total_amount|average_amount|transaction_count|
+-----------+------------+--------------+-----------------+
|       Food|        70.5|          23.5|                3|
|Electronics|       375.0|         187.5|                2|
|   Clothing|       120.5|         60.25|                2|
+-----------+------------+--------------+-----------------+
...

> ./mill test
...
+ foo.FooTests.computeSummary should compute correct summary statistics...
...

> chmod +x spark-submit.sh

> ./mill show assembly # spark-submit 준비
".../out/assembly.dest/out.jar"

> ./spark-submit.sh out/assembly.dest/out.jar foo.Foo resources/transactions.csv
...
Summary Statistics by Category:
+-----------+------------+--------------+-----------------+
|   category|total_amount|average_amount|transaction_count|
+-----------+------------+--------------+-----------------+
|       Food|        70.5|          23.5|                3|
|Electronics|       375.0|         187.5|                2|
|   Clothing|       120.5|         60.25|                2|
+-----------+------------+--------------+-----------------+
...
```

### 요약: Spark 예제 3종 비교

| 항목 | 1. Hello Spark | 2. Hello PySpark | 3. 실전에 가까운 프로젝트 |
|------|----------------|-------------------|----------------------|
| 언어 | Scala | Python | Scala |
| 모듈 타입 | `ScalaModule` | `PythonModule` | 루트 ``` `package` ``` 모듈 |
| 테스트 | uTest | unittest | uTest |
| 배포 방식 | `./mill foo.run` | `./mill foo.run` | `assembly` + `spark-submit.sh` |
| 특이 설정 | `forkArgs`에 `--add-opens` | `mainScript`, `pythonDeps` | `prependShellScript = ""` 추가 |

---

## 4. Scala 단일 파일 스크립트 개요

Mill은 소스 파일이 하나뿐인 Scala 모듈, 즉 "스크립트(script)"를 지원한다. 빌드 설정은 파일 최상단의 헤더 주석 블록(`//|`로 시작하는 YAML)에 기술한다.

- 독립 실행 파일로 쓸 수도 있고, 더 큰 프로젝트의 일부(moduleDeps 대상)로 쓸 수도 있다.
- 실행/조작 방식:

```bash
./mill Foo.scala              # 스크립트 실행 (run)
./mill Foo.scala:compile      # 컴파일만 수행
./mill Foo.scala:assembly     # 어셈블리(fat jar) 생성
```

- 단일 소스 파일이라는 제약 외에는 일반 `ScalaModule`이 지원하는 태스크를 동일하게 지원한다.
- 파일 시스템/서브프로세스/HTTP 엔드포인트를 다루는 간단한 커맨드라인 워크플로를 짤 때, Bash 스크립트 대신 타입체크와 IDE 지원을 받으며 작성할 수 있다는 것이 핵심 이점이다.
- 이런 스크립트는 보통 [번들 라이브러리](#14-번들-라이브러리와-raw-모드)를 활용해 빠르게 시작한다.

---

## 5. 스크립트 활용 사례

### 5.1 HTML 웹 스크래퍼

JSoup으로 Wikipedia HTML 페이지를 크롤링한다. JSoup은 Mill에 번들되어 있지 않으므로 헤더의 `mvnDeps`로 추가한다.

```scala
// HtmlScraper.scala
//| mvnDeps: [org.jsoup:jsoup:1.7.2]
import org.jsoup.*
import scala.collection.JavaConverters.*

def fetchLinks(title: String): Seq[String] = {
  Jsoup.connect(s"https://en.wikipedia.org/wiki/$title")
    .header("User-Agent", "My Scraper")
    .get().select("main p a").asScala.toSeq.map(_.attr("href"))
    .collect { case s"/wiki/$rest" => rest }
}

def main(startArticle: String, depth: Int) = {
  var seen = Set(startArticle)
  var current = Set(startArticle)
  for (i <- Range(0, depth)) {
    current = current.flatMap(fetchLinks(_)).filter(!seen.contains(_))
    seen = seen ++ current
  }

  pprint.log(seen, height = Int.MaxValue)
}
```

```console
> ./mill HtmlScraper.scala --start-article singapore --depth 1
...
  "Hokkien",
  "Conscription_in_Singapore",
  "Malaysia_Agreement",
...
```

`def main(startArticle: String, depth: Int)`처럼 정의하면 `mainargs` 기반으로 `--start-article`, `--depth` 같은 named 인자를 자동으로 받을 수 있다.

### 5.2 JSON API 클라이언트

이번엔 JSoup 대신 Mill에 번들된 라이브러리(`requests`, `ujson`, `upickle`)만으로 Wikipedia JSON API를 조회하고 결과를 파일에 쓴다.

```scala
// JsonApiClient.scala
def fetchLinks(title: String): Seq[String] = {
  val resp = requests.get.stream(
    "https://en.wikipedia.org/w/api.php",
    params = Seq(
      "action" -> "query",
      "titles" -> title,
      "prop" -> "links",
      "format" -> "json"
    )
  )
  for {
    page <- ujson.read(resp)("query")("pages").obj.values.toSeq
    links <- page.obj.get("links").toSeq
    link <- links.arr
  } yield link("title").str
}

def main(startArticle: String, depth: Int) = {
  var seen = Set(startArticle)
  var current = Set(startArticle)
  for (i <- Range(0, depth)) {
    current = current.flatMap(fetchLinks(_)).filter(!seen.contains(_))
    seen = seen ++ current
  }

  pprint.log(seen)
  os.write(os.pwd / "fetched.json", upickle.stream(seen, indent = 4))
}
```

```console
> ./mill JsonApiClient.scala --start-article singapore --depth 2
...
  "Calling code",
  "+65",
  "British Empire",
  "1st Parliament of Singapore",
...
)

> cat fetched.json
[
    "Calling code",
    "+65",
    "British Empire",
    "1st Parliament of Singapore",
    ...
]
```

이 예제는 헤더에 별도 `mvnDeps` 선언 없이 번들 라이브러리만으로 동작한다.

### 5.3 웹 서버

[Cask](https://github.com/com-lihaoyi/cask) 프레임워크로 간단한 웹 서버를 띄운다. 로컬 테스트/실험용으로 적합하며, 필요하면 나중에 `build.mill` 기반의 본격적인 프로젝트로 확장할 수 있다.

```scala
// WebServer.scala
//| mvnDeps: [com.lihaoyi::cask:0.9.1]

object WebServer extends cask.MainRoutes {
  override def port = sys.env.getOrElse("PORT", "8080").toInt

  @cask.post("/reverse-string")
  def doThing(request: cask.Request) = {
    request.text().reverse
  }

  initialize()
}
```

```console
> ./mill WebServer.scala:runBackground

> curl -d 'helloworld' localhost:${PORT:-8080}/reverse-string
dlrowolleh
```

`:runBackground` 태스크로 서버를 백그라운드 프로세스로 띄운 뒤 다른 터미널에서 요청을 보낼 수 있다.

### 5.4 데이터베이스 쿼리

[ScalaSql](https://github.com/com-lihaoyi/scalasql)로 로컬 SQLite 데이터베이스를 채우고 쿼리한다. `sqlite-customers.sql` 파일 내용으로 DB를 초기화한 뒤, join/filter/map으로 오늘 이후 배송 예정인 구매자 이름을 조회한다.

```scala
// Database.scala
//| mvnDeps:
//| - com.lihaoyi::scalasql:0.2.3
//| - com.lihaoyi::scalasql-namedtuples:0.2.3
//| - org.xerial:sqlite-jdbc:3.43.0.0

import scalasql.simple._, SqliteDialect.*
import java.time.LocalDate

case class Buyer(id: Int, name: String, dateOfBirth: LocalDate)
object Buyer extends SimpleTable[Buyer]

case class ShippingInfo(id: Int, buyerId: Int, shippingDate: LocalDate)
object ShippingInfo extends SimpleTable[ShippingInfo]

def main(args: Array[String]): Unit = {
  // 데이터베이스 초기화
  val dataSource = new org.sqlite.SQLiteDataSource()
  dataSource.setUrl(s"jdbc:sqlite:./file.db")
  val sqliteClient = new scalasql.DbClient.DataSource(dataSource, config = new scalasql.Config {})

  sqliteClient.transaction { db =>
    db.updateRaw(os.read(os.pwd / "sqlite-customers.sql")) // SQL 파일로 데이터베이스 채우기

    val names = db.run( // 배송일이 오늘 이후인 구매자 이름 조회
      Buyer.select.join(ShippingInfo)(_.id === _.buyerId)
        .filter { case (b, s) => s.shippingDate >= LocalDate.parse(args(0)) }
        .map { case (b, s) => b.name })

    for (name <- names) println(name)
  }
}
```

```console
> ./mill Database.scala 2011-01-01
James Bond
John Doe
```

`mvnDeps` 헤더는 리스트 인라인 표기(`[...]`)뿐 아니라 YAML 시퀀스(`- 항목`) 형태로도 여러 줄에 걸쳐 쓸 수 있다.

### 5.5 정적 사이트 생성기

`post/` 폴더의 마크다운 파일을 [Commonmark-Java](https://github.com/commonmark/commonmark-java)로 HTML 변환해 `site-out/` 폴더에 쓴다.

```scala
// StaticSite.scala
//| mvnDeps:
//| - com.lihaoyi::scalatags:0.13.1
//| - com.atlassian.commonmark:commonmark:0.13.1
import scalatags.Text.all.*

def main() = {
  val postInfo = os
    .list(os.pwd / "post")
    .map { p =>
      val s"$prefix - $suffix.md" = p.last
      (prefix, suffix, p)
    }
    .sortBy(_._1.toInt)

  os.remove.all(os.pwd / "site-out")
  os.makeDir.all(os.pwd / "site-out/post")

  for ((_, suffix, path) <- postInfo) {
    val parser = org.commonmark.parser.Parser.builder().build()
    val document = parser.parse(os.read(path))
    val renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build()
    val output = renderer.render(document)
    os.write(
      os.pwd / "site-out/post" / (suffix.replace(" ", "-").toLowerCase + ".html"),
      doctype("html")(
        html(
          body(
            h1(a("Blog"), " / ", suffix),
            raw(output)
          )
        )
      )
    )
  }
}
```

```console
> ./mill StaticSite.scala

> cat site-out/post/my-first-post.html
<!DOCTYPE html><html><body><h1><a>Blog</a> / My First Post</h1><p>Sometimes you want numbered lists:</p>
<ol>
<li>One</li>
<li>Two</li>
<li>Three</li>
</ol>
</body></html>
```

`scalatags`로 HTML을 타입 안전하게 생성하는 패턴을 보여준다.

---

## 6. 어셈블리와 네이티브 바이너리 패키징

단일 파일 스크립트도 실행 가능한 어셈블리(fat jar)나 GraalVM 네이티브 이미지로 패키징해 배포할 수 있다. 네이티브 이미지를 만들려면 `jvmVersion`을 `graalvm` 계열 버전으로 지정해야 한다.

```scala
// Bar.scala
//| scalaVersion: 3.8.2
//| jvmVersion: "graalvm-community:17"
//| nativeImageOptions: ["--no-fallback"]
def main() = {
  println("Hello World")
}
```

```console
> ./mill Bar.scala:assembly

> ./out/Bar.scala/assembly.dest/out.jar # mac/linux
Hello World

> ./mill Bar.scala:nativeImage

> ./out/Bar.scala/nativeImage.dest/native-executable
Hello World
```

---

## 7. 스크립트 REPL 열기

스크립트에 정의된 코드를 로드한 채로 Scala REPL을 열어 대화형으로 실험할 수 있다.

```scala
./mill Foo.scala:repl
```

---

## 8. 스크립트 moduleDeps

단일 파일 스크립트끼리도 `moduleDeps`로 서로 의존할 수 있다. 여러 스크립트가 공유하는 로직을 재사용할 때 유용하다.

```scala
// Foo.scala
val fooValue = 1337
```

```scala
// Bar.scala
//| moduleDeps: [Foo.scala]

def main() =
  println(fooValue)
```

```console
> ./mill Bar.scala
1337
```

---

## 9. 상대/절대 경로 스크립트 moduleDeps

스크립트 간 `moduleDeps`는 상대 경로 또는 워크스페이스 절대 경로(`//` 접두사)로 지정할 수 있다.

```scala
// bar/Bar.scala
//| scalaVersion: 3.8.2
package bar

def generateHtml(text: String) = "<h1>" + text + "</h1>"
```

같은 폴더(`bar/`) 안에서는 `Bar.scala`처럼 폴더 상대 경로로 참조한다.

```scala
// bar/BarTests.scala
//| extends: mill.script.ScalaModule.Utest
//| moduleDeps: [Bar.scala]
//| mvnDeps:
//| - com.lihaoyi::utest:0.9.1
package bar
import utest.*
object BarTests extends TestSuite {
  def tests = Tests {
    assert(generateHtml("hello") == "<h1>hello</h1>")
  }
}
```

다른 폴더(`foo/`)에서는 `//bar/Bar.scala`처럼 워크스페이스 루트 기준 절대 경로(`//` 접두사)로 참조한다.

```scala
// foo/Foo.scala
//| moduleDeps: [//bar/Bar.scala]
//| scalaVersion: 3.8.2

package foo

def main(args: Array[String]): Unit = println(bar.generateHtml(args(0)))
```

```console
> ./mill bar/Bar.scala:compile
> ./mill bar/BarTests.scala
> ./mill foo/Foo.scala --text hello
```

`BarTests.scala`는 `//| extends: mill.script.ScalaModule.Utest`로 uTest 기반 테스트 모듈로 확장된 예다.

---

## 10. 프로젝트 모듈에 대한 moduleDeps

단일 파일 스크립트는 `build.mill`에 선언된 프로그래밍 가능한(programmable) 모듈에도 의존할 수 있다. 라이브러리/애플리케이션 코드에 정의된 클래스·메서드를 스크립트에서 재사용하고 싶을 때 유용하다.

```scala
// build.mill
package build
import mill.*, scalalib.*

object bar extends ScalaModule {
  def scalaVersion = "3.8.2"
  def mvnDeps = Seq(mvn"com.lihaoyi::scalatags:0.13.1")
}
```

```scala
// Foo.scala
//| moduleDeps: [bar]

def main(args: Array[String]) = {
  println(bar.Bar.generateHtml(args(0)))
}
```

```console
> ./mill Foo.scala hello
<h1>hello</h1>
```

---

## 11. 커스텀 스크립트 모듈 클래스

기본적으로 단일 파일 Scala 모듈은 내장 `mill.script.ScalaModule`을 상속한다. 메타 빌드(`mill-build/src/`)에 직접 정의한 커스텀 `Module` 클래스를 상속하도록 바꿀 수도 있다. 예를 들어 소스 파일을 처리해 생성한 리소스 파일을 추가하려면 다음처럼 `LineCountScalaModule`을 정의한다.

```scala
// Qux.scala
//| extends: millbuild.LineCountScalaModule
//| scalaVersion: 3.8.2
package qux

def getLineCount() = {
  scala.io.Source
    .fromResource("line-count.txt")
    .mkString
}

def main() = {
  println(s"Line Count: ${getLineCount()}")
}
```

```scala
// mill-build/src/LineCountScalaModule.scala
package millbuild
import mill.*, scalalib.*

class LineCountScalaModule(scriptConfig: mill.api.ScriptModule.Config)
    extends mill.script.ScalaModule(scriptConfig) {

  /** 모듈 소스 파일의 총 라인 수 */
  def lineCount = Task {
    allSourceFiles().map(f => os.read.lines(f.path).size).sum
  }

  /** 소스의 lineCount를 이용해 리소스 생성 */
  override def resources = Task {
    os.write(Task.dest / "line-count.txt", "" + lineCount())
    super.resources() ++ Seq(PathRef(Task.dest))
  }
}
```

```console
> ./mill Qux.scala
...
Line Count: 17

> ./mill show Qux.scala:lineCount
17
```

주의할 점:

- 커스텀 클래스는 `class`여야 하고, 생성자 인자로 `mill.script.ScriptModule.Config`를 받아 `mill.script.ScalaModule`에 넘겨야 한다.
- 많은 스크립트가 비슷한 설정을 공유하거나, YAML 빌드 헤더만으로는 표현할 수 없는 커스터마이징이 필요할 때 이 방식으로 동작을 중앙에서 정의하고 `extends`로 표준화할 수 있다.
- 일반 선언적(declarative)/프로그래밍 가능한(programmable) 모듈은 여러 abstract `trait`을 상속할 수 있지만, 단일 파일 스크립트는 실행 시점에 동적으로 인스턴스화되기 때문에 오직 하나의 concrete `class`만 상속할 수 있다.

---

## 12. 스크립트 리소스

단일 파일 Scala 모듈은 기본적으로 classpath 리소스가 활성화되어 있지 않으며, 빌드 헤더의 `resources` 키로 추가한다.

```scala
// Foo.scala
//| resources: [resources/]

package foo

def main() = {
  println(os.read(os.resource / "file.txt"))
}
```

```console
> ./mill Foo.scala
Hello World Resource File
```

classpath 리소스는 스크립트를 assembly나 실행 파일로 패키징할 때 함께 포함되므로, 배포 시 별도 파일을 관리할 필요가 없다.

```console
> ./mill show Foo.scala:assembly
".../out/Foo.scala/assembly.dest/out.jar"

> out/Foo.scala/assembly.dest/out.jar
Hello World Resource File
```

---

## 13. 커스텀 JVM 버전

단일 파일 Scala 모듈은 기본적으로 JVM `zulu:25`를 사용한다. 헤더의 `jvmVersion`으로 변경할 수 있다.

```scala
// Foo.scala
//| jvmVersion: 11.0.21
//| scalaVersion: 3.7.4

def main() = {
  println("java.version " + System.getProperty("java.version"))
}
```

```console
> ./mill Foo.scala
java.version 11.0.21
```

`jvmVersion`을 `11.x`로 낮추면 기본 `scalaVersion`인 `3.8.x`와 호환되지 않으므로, 위 예제처럼 `scalaVersion: 3.7.4`를 명시적으로 함께 지정해야 한다.

---

## 14. 번들 라이브러리와 Raw 모드

Mill Scala 스크립트는 다음 라이브러리를 기본 번들로 제공하며, 이는 Mill 빌드 파일 자체에서 사용 가능한 라이브러리 목록과 대체로 동일하다.

| 라이브러리 | 용도 |
|------------|------|
| [OS-Lib](https://github.com/com-lihaoyi/os-lib) | 파일시스템/프로세스 작업 |
| [uPickle](https://github.com/com-lihaoyi/upickle) | JSON 직렬화 |
| [Requests-Scala](https://github.com/com-lihaoyi/requests-scala) | HTTP 요청 |
| [MainArgs](https://github.com/com-lihaoyi/mainargs) | CLI 인자 파싱 (`@main`이 `@mainargs.main`으로 별칭됨) |
| [PPrint](https://github.com/com-lihaoyi/PPrint) | 값 pretty-print |

이 라이브러리들은 단일 파일 스크립트에서만 편의상 기본 제공되며, 선언적/프로그래밍 가능 모듈에서는 `mvnDeps`에 명시적으로 추가해야 한다. 이 외의 라이브러리는 `//| mvnDeps`로 자유롭게 추가할 수 있다.

번들 라이브러리를 원하지 않으면 `mill.script.ScalaModule.Raw`를 상속하도록 만든다.

```scala
// Foo.scala
//| extends: mill.script.ScalaModule.Raw

def main(args: Array[String]): Unit = {
  println(os.read(os.pwd / "file.txt"))
}
```

```console
> ./mill Foo.scala
error: ...Not found: os
```

`Raw` 모드에서는 번들 라이브러리에 접근할 수 없으므로, Java 표준 라이브러리를 쓰거나

```scala
// Bar.scala
//| extends: mill.script.ScalaModule.Raw

def main(args: Array[String]): Unit = {
  println(java.nio.file.Files.readString(java.nio.file.Path.of("file.txt")))
}
```

```console
> ./mill Bar.scala
hello
```

필요한 라이브러리를 `mvnDeps`로 직접 추가해야 한다.

```scala
// Qux.scala
//| extends: mill.script.ScalaModule.Raw
//| mvnDeps: [com.lihaoyi::os-lib:0.11.4]

def main(args: Array[String]): Unit = {
  println(os.read(os.pwd / "file.txt"))
}
```

```console
> ./mill Qux.scala
hello
```

---

## 15. 스크립트 IDE 연동(BSP)

IntelliJ/VSCode용 BSP(Build Server Protocol) 지원은 스크립트를 단일 파일 모듈로 취급하되, 탐색(discovery) 방식이 일반 모듈과 다르다. IDE로 프로젝트를 임포트하면 Mill은 워크스페이스 폴더를 재귀적으로 탐색하며 `.java`, `.scala`, `.kt` 파일 중 스크립트일 수 있는 파일을 찾되, 다음 경로는 건너뛴다.

- `sources` 디렉터리
- `out` 디렉터리
- `**/src/`
- `**/src-*/`
- `**/resources/`
- `**/out/`
- `**/target/`

기본 무시 경로로 충분하지 않다면, `build.mill`에 `//| bspScriptIgnore: [...]`를 추가하거나 `build.mill.yaml`에 `mill-build: bspScriptIgnore: [...]`를 추가해 폴더를 더 지정할 수 있다. `bspScriptIgnore`는 문자열 시퀀스를 받으며 `.gitignore` 문법을 따른다.

- `*`: 단일 경로 세그먼트 내 와일드카드
- `**`: 여러 경로 세그먼트에 걸친 와일드카드
- `!`: 무시 규칙 부정(negate)

프로젝트가 비전통적인 폴더 레이아웃으로 Java/Scala/Kotlin 소스 파일을 배치하고 있어 IDE가 이를 스크립트로 잘못 인식할 때 유용한 옵션이다.
