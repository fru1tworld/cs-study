# 리액티브 데이터베이스 접근 - Part 3 - Scala, Futures, Actors와 jOOQ 사용하기

> 원문: https://blog.jooq.org/reactive-database-access-part-3-using-jooq-with-scala-futures-and-actors/

jOOQ 블로그에서 Manuel Bernhardt의 게스트 포스트 시리즈를 계속하게 되어 매우 기쁩니다. 이 블로그 시리즈에서 Manuel은 소위 리액티브 기술 뒤에 있는 동기를 설명하고, Futures와 Actors의 개념을 소개한 후 jOOQ와 결합하여 관계형 데이터베이스에 접근하는 방법을 보여줄 것입니다.

Manuel Bernhardt는 웹 기반 시스템 구축에 열정을 가진 독립 소프트웨어 컨설턴트입니다. 그는 "Reactive Web Applications"의 저자이며 2010년부터 Scala, Akka, Play Framework로 작업해왔습니다. 이 시리즈는 리액티브 데이터베이스 접근에 관한 세 부분으로 구성됩니다.

## 도구 준비하기

애플리케이션을 구축하려면 여러 도구가 필요합니다. Scala를 처음 접하는 분들에게는 Typesafe Activator를 권장합니다. 데이터베이스로는 Oracle Database 12c Enterprise Edition보다 PostgreSQL을 권장합니다.

## 애플리케이션

목표는 Twitter에서 멘션을 가져와 시각화 및 분석을 위해 로컬에 저장하는 것입니다. `MentionsFetcher` 액터가 주기적으로 Twitter에서 멘션을 검색하여 데이터베이스에 저장합니다.

## 데이터베이스 생성

첫 번째 단계는 데이터베이스 스키마를 생성하는 것입니다. SQL 스크립트는 `play` 사용자, `mentions` 데이터베이스, 그리고 두 개의 테이블 `twitter_user`와 `mentions`를 설정합니다.

```sql
CREATE USER "play" NOSUPERUSER INHERIT CREATEROLE;

CREATE DATABASE mentions WITH OWNER = "play" ENCODING 'UTF8';

GRANT ALL PRIVILEGES ON DATABASE mentions to "play";

\connect mentions play

CREATE TABLE twitter_user (
  id bigserial primary key,
  created_on timestamp with time zone NOT NULL,
  twitter_user_name varchar NOT NULL
);

CREATE TABLE mentions (
  id bigserial primary key,
  tweet_id varchar NOT NULL,
  user_id bigint NOT NULL,
  created_on timestamp with time zone NOT NULL,
  text varchar NOT NULL
);
```

## 프로젝트 부트스트랩

activator를 설치한 후, play-scala 템플릿을 사용하여 reactive-mentions 프로젝트를 생성합니다. 이렇게 하면 IntelliJ IDEA와 같은 지원되는 IDE를 사용하여 실행하고 개발할 수 있는 기본 Play Framework 프로젝트가 생성됩니다.

## jOOQ 설정

타입 안전 SQL을 위해 jOOQ를 프로젝트에 통합합니다. 저자는 이전에 Scala에서의 데이터베이스 접근에 대해 글을 썼으며, 관련 프로젝트에서 타입 안전 SQL을 위해 jOOQ가 대안들을 능가한다고 결론지었습니다. 의존성은 `build.sbt`에 추가되고, jOOQ는 `conf/mentions.xml`을 통해 구성됩니다.

```scala
libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.jooq" % "jooq" % "3.7.0",
  "org.jooq" % "jooq-codegen-maven" % "3.7.0",
  "org.jooq" % "jooq-meta" % "3.7.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.1",
  "com.ning" % "async-http-client" % "1.9.29",
  specs2 % Test
)

val generateJOOQ = taskKey[Seq[File]]("Generate JooQ classes")

val generateJOOQTask = (sourceManaged, fullClasspath in Compile, runner in Compile, streams) map { (src, cp, r, s) =>
  toError(r.run("org.jooq.util.GenerationTool", cp.files, Array("conf/mentions.xml"), s.log))
  ((src / "main/generated")  "*.scala").get
}

generateJOOQ <<= generateJOOQTask

unmanagedSourceDirectories in Compile += sourceManaged.value / "main/generated"
```

커스텀 SBT 태스크 `generateJOOQ`는 jOOQ의 코드 생성을 실행하여 데이터베이스 스키마를 읽고 Scala 전용 클래스를 생성합니다.

jOOQ 구성 파일 (`conf/mentions.xml`):

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<configuration xmlns="https://www.jooq.org/xsd/jooq-codegen-3.7.0.xsd">
  <jdbc>
    <driver>org.postgresql.Driver</driver>
    <url>jdbc:postgresql://localhost/mentions</url>
    <user>play</user>
    <password></password>
  </jdbc>
  <generator>
    <name>org.jooq.util.ScalaGenerator</name>
    <database>
      <name>org.jooq.util.postgres.PostgresDatabase</name>
      <inputSchema>public</inputSchema>
      <includes>.*</includes>
      <excludes></excludes>
    </database>
    <target>
      <packageName>generated</packageName>
      <directory>target/scala-2.11/src_managed/main</directory>
    </target>
  </generator>
</configuration>
```

## MentionsFetcher 액터 생성

Akka를 의존성으로 추가합니다. `MentionsFetcher` 액터는 정기적인 간격으로 멘션을 가져오도록 생성됩니다. 액터는 10분마다 깨어나 자신에게 `CheckMentions` 메시지를 보내는 스케줄러를 사용하고, 수신된 멘션을 처리합니다. 컴패니언 객체는 메시지 정의가 포함된 액터 프로토콜을 보유합니다.

```scala
package actors

import actors.MentionsFetcher._
import akka.actor.{ActorLogging, Actor}
import org.joda.time.DateTime
import scala.concurrent.duration._

class MentionsFetcher extends Actor with ActorLogging {

  val scheduler = context.system.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 10.minutes,
    receiver = self,
    message = CheckMentions
  )

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def receive = {
    case CheckMentions => checkMentions
    case MentionsReceived(mentions) => storeMentions(mentions)
  }

  def checkMentions = ???

  def storeMentions(mentions: Seq[Mention]) = ???

}

object MentionsFetcher {

  case object CheckMentions
  case class Mention(id: String, created_at: DateTime, text: String, from: String, users: Seq[User])
  case class User(handle: String, id: String)
  case class MentionsReceived(mentions: Seq[Mention])

}
```

## Twitter에서 멘션 가져오기

Twitter의 API는 검색 엔드포인트를 통해 접근합니다. apps.twitter.com에서 API 키와 액세스 토큰을 얻어야 합니다. 이러한 자격 증명은 `conf/application.conf`에 구성됩니다.

`fetchMentions` 메서드는 Twitter의 검색 API에 대해 OAuth 서명된 요청을 구성합니다. 마지막 확인 시간 이후에 생성된 멘션만 반환하도록 결과를 필터링합니다. Future 연산을 처리하기 위해 액터의 dispatcher에서 `ExecutionContext`를 빌려옵니다.

## ExecutionContext 설정

Futures는 태스크 실행을 스케줄링하기 위해 `ExecutionContext`가 필요합니다. 코드가 액터 내에서 실행되므로, 액터의 dispatcher를 실행 컨텍스트로 빌려옵니다.

## 리액티브 데이터베이스 연결 설정

### 데이터베이스 연결 구성

데이터베이스 연결 정보는 드라이버와 URL 구성과 함께 `conf/application.conf`에 추가됩니다.

```properties
db.default.driver="org.postgresql.Driver"
db.default.url="jdbc:postgresql://localhost/mentions?user=play"

contexts {
    database {
        fork-join-executor {
          parallelism-max = 9
        }
    }
}

twitter.apiKey="..."
twitter.apiSecret="..."
twitter.accessToken="..."
twitter.accessTokenSecret="..."

play.modules.enabled += "actors.MentionsFetcherModule"
```

### 헬퍼 클래스 생성

`DB` 헬퍼 클래스는 커스텀 ExecutionContext에서 실행되는 Futures로 데이터베이스 작업을 래핑합니다. JDBC가 본질적으로 블로킹이기 때문에, 이는 데이터베이스 상호작용을 기다리는 동안 애플리케이션이 블로킹되는 것을 방지합니다. 이 클래스는 jOOQ의 `DSLContext`를 초기화하고 제공된 코드 블록을 실행하는 `query`와 `withTransaction` 메서드를 제공합니다.

```scala
package database

import javax.inject.Inject

import akka.actor.ActorSystem
import org.jooq.{SQLDialect, DSLContext}
import org.jooq.impl.DSL
import play.api.db.Database

import scala.concurrent.{ExecutionContext, Future}

class DB @Inject() (db: Database, system: ActorSystem) {

  val databaseContext: ExecutionContext = system.dispatchers.lookup("contexts.database")

  def query[A](block: DSLContext => A): Future[A] = Future {
    db.withConnection { connection =>
      val sql = DSL.using(connection, SQLDialect.POSTGRES_9_4)
      block(sql)
    }
  }(databaseContext)

  def withTransaction[A](block: DSLContext => A): Future[A] = Future {
    db.withTransaction { connection =>
      val sql = DSL.using(connection, SQLDialect.POSTGRES_9_4)
      block(sql)
    }
  }

}
```

데이터베이스 컨텍스트는 Akka의 구성 기능을 사용합니다. 특정 병렬성 설정을 가진 커스텀 dispatcher가 `conf/application.conf`에 구성되며, 9개 연결이라는 값은 HikariCP 연결 풀 크기 권장 사항을 기반으로 합니다.

### 의존성 주입을 사용한 연결

Play의 의존성 주입 메커니즘은 `MentionsFetcher` 액터에 `DB` 인스턴스를 제공합니다. `MentionsFetcherModule`은 Play가 시작될 때 액터를 부트스트랩하도록 선언됩니다. 이 모듈은 `conf/application.conf`에 등록됩니다.

## 데이터베이스에 멘션 저장

jOOQ는 저장 문을 작성하는 것을 간단하게 만듭니다. 구현은 중복 항목을 방지하기 위해 WHERE NOT EXISTS 절이 있는 upsert 작업을 사용합니다. 코드는 멘션하는 사용자, 멘션된 사용자, 그리고 멘션 자체를 저장합니다.

```scala
// 추가 임포트
import akka.pattern.pipe
import org.joda.time.DateTime
import scala.util.control.NonFatal
import play.api.Play
import play.api.Play.current
import play.api.libs.oauth.{RequestToken, ConsumerKey}
import scala.util.control.NonFatal
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.JsArray
import play.api.libs.ws.WS
import scala.concurrent.Future
import javax.inject.Inject
import play.api.db.Database
import java.util.Locale

class MentionsFetcher @Inject() (db: DB) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  var lastSeenMentionTime: Option[DateTime] = Some(DateTime.now)

  def credentials = for {
    apiKey <- Play.configuration.getString("twitter.apiKey")
    apiSecret <- Play.configuration.getString("twitter.apiSecret")
    token <- Play.configuration.getString("twitter.accessToken")
    tokenSecret <- Play.configuration.getString("twitter.accessTokenSecret")
  } yield (ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))

  def checkMentions = {
      val maybeMentions = for {
        (consumerKey, requestToken) <- credentials
        time <- lastSeenMentionTime
      } yield fetchMentions(consumerKey, requestToken, "<yourTwitterHandleHere>", time)

      maybeMentions.foreach { mentions =>
        mentions.map { m =>
          MentionsReceived(m)
        } recover { case NonFatal(t) =>
          log.error(t, "Could not fetch mentions")
          MentionsReceived(Seq.empty)
        } pipeTo self
      }
  }

  def fetchMentions(consumerKey: ConsumerKey, requestToken: RequestToken, user: String, time: DateTime): Future[Seq[Mention]] = {
    val df = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy").withLocale(Locale.ENGLISH)

    WS.url("https://api.twitter.com/1.1/search/tweets.json")
      .sign(OAuthCalculator(consumerKey, requestToken))
      .withQueryString("q" -> s"@$user")
      .get()
      .map { response =>
        val mentions = (response.json \ "statuses").as[JsArray].value.map { status =>
          val id = (status \ "id_str").as[String]
          val text = (status \ "text").as[String]
          val from = (status \ "user" \ "screen_name").as[String]
          val created_at = df.parseDateTime((status \ "created_at").as[String])
          val userMentions = (status \ "entities" \ "user_mentions").as[JsArray].value.map { user =>
            User((user \ "screen_name").as[String], ((user \ "id_str").as[String]))
          }

          Mention(id, created_at, text, from, userMentions)
        }
        mentions.filter(_.created_at.isAfter(time))
    }
  }

  def storeMentions(mentions: Seq[Mention]) = db.withTransaction { sql =>
    log.info("Inserting potentially {} mentions into the database", mentions.size)
    val now = new Timestamp(DateTime.now.getMillis)

    def upsertUser(handle: String) = {
      sql.insertInto(TWITTER_USER, TWITTER_USER.CREATED_ON, TWITTER_USER.TWITTER_USER_NAME)
        .select(
          select(value(now), value(handle))
            .whereNotExists(
              selectOne()
                .from(TWITTER_USER)
                .where(TWITTER_USER.TWITTER_USER_NAME.equal(handle))
            )
        )
        .execute()
    }

    mentions.foreach { mention =>
      upsertUser(mention.from)

      mention.users.foreach { user =>
        upsertUser(user.handle)
      }

      sql.insertInto(MENTIONS, MENTIONS.CREATED_ON, MENTIONS.TEXT, MENTIONS.TWEET_ID, MENTIONS.USER_ID)
        .select(
          select(
            value(now),
            value(mention.text),
            value(mention.id),
            TWITTER_USER.ID
          )
            .from(TWITTER_USER)
            .where(TWITTER_USER.TWITTER_USER_NAME.equal(mention.from))
            .andNotExists(
              selectOne()
                .from(MENTIONS)
                .where(MENTIONS.TWEET_ID.equal(mention.id))
            )
        )
        .execute()
    }
  }
}

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class MentionsFetcherModule extends AbstractModule with AkkaGuiceSupport {
  def configure(): Unit =
    bindActor[MentionsFetcher]("fetcher")
}
```

## 멘션 표시

인덱스 템플릿은 멘션 카운트를 표시하도록 조정됩니다. Application 컨트롤러는 `DB` 헬퍼의 `query` 메서드를 사용하며, 결과가 Future이기 때문에 `Action.async`로 래핑됩니다. 이는 데이터베이스 작업이 Play 프레임워크의 기본 실행 컨텍스트를 블로킹하지 않도록 보장하며, 격리된 스레드 풀이 장애가 다른 애플리케이션 구성 요소에 영향을 미치는 것을 방지하는 "bulkheading"이라는 원칙을 구현합니다.

```scala
package controllers

import java.sql.Timestamp
import javax.inject.Inject

import database.DB
import org.joda.time.DateTime
import play.api._
import play.api.mvc._

class Application @Inject() (db: DB) extends Controller {

  def index = Action.async { implicit request =>

    import generated.Tables._
    import org.jooq.impl.DSL._

    db.query { sql =>
      val mentionsCount = sql.select(
        count()
      ).from(MENTIONS)
       .where(
         MENTIONS.CREATED_ON.gt(value(new Timestamp(DateTime.now.minusDays(1).getMillis)))
       ).fetchOne(int.class)

      Ok(views.html.index(mentionsCount))
    }

  }

}
```

## 결론

이 시리즈는 리액티브 프로그래밍의 동기와 비동기 개념을 탐구합니다. 관계형 데이터베이스는 현재 표준 비동기 드라이버가 부족하지만, 이 예제는 그렇지 않으면 블로킹되는 데이터베이스 작업을 격리하기 위해 커스텀 ExecutionContext를 사용하는 방법을 보여줍니다. "Reactive Web Applications" 책은 Futures (5장), Actors (6장), 데이터베이스 접근 (7장)에 대한 추가 세부 정보를 제공합니다.

> 참고: 이 글의 예제는 Typesafe의 Activator가 현재 다르게 작동하고 최신 도구 버전을 위한 유지보수가 필요하므로 구식일 수 있습니다.
