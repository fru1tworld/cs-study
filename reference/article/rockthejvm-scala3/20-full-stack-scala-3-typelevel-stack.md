> 원본: https://rockthejvm.com/articles/building-a-full-stack-scala-3-application-with-the-typelevel-stack

# Typelevel 스택으로 풀스택 Scala 3 애플리케이션 만들기

## 소개

Typelevel 스택은 순수 함수형 프로그래밍으로 애플리케이션을 만들기 위한 Scala 생태계의 강력한 라이브러리 모음이다. Cats와 Cats Effect를 기반으로 하며, 펑터(functor)와 모나드(monad) 같은 함수형 프로그래밍 추상화를 제공해 타입 시스템이 보장하는 견고한 소프트웨어를 만들 수 있다.

이 글에서는 핵심 Typelevel 라이브러리를 사용해 완전한 웹 애플리케이션을 구성하는 과정을 살펴본다.

**백엔드 구성 요소:**
- Cats - 함수형 프로그래밍 추상화
- Cats Effect - 이펙트 처리
- doobie - 데이터베이스 연산
- http4s - 순수 함수형 웹 서버

**프론트엔드:**
- Tyrian - Cats Effect 위에 구축된 Elm 스타일 프론트엔드 라이브러리로, ScalaJS를 통해 JavaScript로 컴파일

여기서 만드는 애플리케이션은 간단한 "구인 게시판"이다. 서버에서 데이터베이스 데이터를 삽입하고 조회한 뒤, 프론트엔드 인터페이스로 보여준다.

## 설정

먼저 데모 저장소를 클론하고 `start` 태그를 체크아웃한다. 프로젝트 구조에는 세 개의 sbt 모듈이 있다.

- **server**: 백엔드 모듈
- **app**: 프론트엔드 모듈
- **common**: 프론트엔드와 백엔드가 공유하는 코드

build.sbt 파일에서 이 모듈들과 필요한 의존성을 설정한다. 주요 라이브러리는 다음과 같다.

- Tyrian 0.6.1 - 프론트엔드 개발
- Cats Effect 3.3.14 - 이펙트 관리
- http4s 0.23.15 - 웹 서버
- doobie 1.0.0-RC1 - 데이터베이스 접근

Docker Compose 설정으로 5444 포트에 PostgreSQL 데이터베이스를 띄운다. 코드를 작성하기 전에 `db` 폴더에서 `docker-compose up`으로 데이터베이스를 시작한다. 데이터베이스에는 회사명, 직함, 설명, 급여 범위, 근무지, 원격 근무 여부 필드를 가진 `jobs` 테이블이 포함되어 있다.

## 백엔드: Core 모듈

Core 모듈은 비즈니스 로직과 서버 구현을 분리한다. 세 가지 구성 요소 패턴을 따른다.

**1. 트레이트 정의 (API)**

제네릭 이펙트 타입 `F[_]`를 사용해 이펙트가 있는 연산을 트레이트로 정의한다.

```scala
trait Jobs[F[_]] {
  def create(job: Job): F[UUID]
  def all: F[List[Job]]
}
```

**2. 구현**

구현체에서는 doobie로 데이터베이스와 상호작용한다.

```scala
class JobsLive[F[_]: Concurrent] private (transactor: Transactor[F]) 
    extends Jobs[F] {
  override def all: F[List[Job]] =
    sql"""SELECT ... FROM jobs"""
      .query[Job]
      .stream
      .transact(transactor)
      .compile
      .toList

  override def create(job: Job): F[UUID] =
    sql"""INSERT INTO jobs(...) VALUES (...)"""
      .update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(transactor)
}
```

**3. 컴패니언 오브젝트**

스마트 생성자로 리소스 생성을 노출한다.

```scala
object JobsLive {
  def make[F[_]: Applicative]: F[Dummy[F]] =
    new DummyLive[F].pure[F]

  def resource[F[_]: Applicative]: Resource[F, Dummy[F]] =
    Resource.pure(new DummyLive[F])
}
```

도메인 모델은 case class로 정의한다.

```scala
case class Job(
    company: String,
    title: String,
    description: String,
    externalUrl: String,
    salaryLo: Option[Int],
    salaryHi: Option[Int],
    currency: Option[String],
    remote: Boolean,
    location: String,
    country: Option[String]
)
```

## 백엔드: Server 모듈

http4s 서버로 HTTP 엔드포인트를 노출한다. `JobRoutes` 클래스에서 두 개의 엔드포인트를 만든다.

**POST /jobs/create** - 새 채용 공고 생성:

```scala
private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
  case req @ POST -> Root / "create" =>
    for {
      job  <- req.as[Job]
      id   <- jobs.create(job)
      resp <- Created(id)
    } yield resp
}
```

**GET /jobs** - 전체 채용 공고 조회:

```scala
private val getAllRoute: HttpRoutes[F] = HttpRoutes.of[F] { 
  case GET -> Root =>
    jobs.all.flatMap(jobs => Ok(jobs))
}
```

라우터를 사용해 라우트를 합친다.

```scala
val routes: HttpRoutes[F] = Router(
  "/jobs" -> (createJobRoute <+> getAllRoute)
)
```

## 백엔드: 전체 조립

`Application` 오브젝트에서 리소스 합성으로 모든 구성 요소를 조율한다.

```scala
def makeServer = for {
  postgres <- makePostgres
  jobs     <- JobsLive.resource[IO](postgres)
  jobApi   <- JobRoutes.resource[IO](jobs)
  server <- EmberServerBuilder
    .default[IO]
    .withHost(host"0.0.0.0")
    .withPort(port"4041")
    .withHttpApp(CORS(jobApi.routes.orNotFound))
    .build
} yield server

override def run: IO[Unit] =
  makeServer.use(_ => IO.println("Server ready.") *> IO.never)
```

## 프론트엔드

프론트엔드에서는 컴파일 대상 간에 도메인 모델을 공유해야 한다. `domain` 폴더를 `common` 모듈의 `src/main/scala` 디렉터리로 옮긴다.

프론트엔드는 Tyrian을 사용하며, Elm 아키텍처를 따른다. 핵심 개념은 다음과 같다.

- **Model**: 채용 공고 목록을 담는 애플리케이션 상태
- **Msg**: 상태 변경을 기술하는 메시지 타입
- **view**: 모델로부터 HTML을 렌더링
- **update**: 메시지에 따라 모델을 변환
- **init**: 초기 모델과 시작 커맨드를 제공

메시지 enum:

```scala
enum Msg {
  case NoMsg
  case LoadJobs(jobs: List[Job])
  case Error(e: String)
}
```

모델:

```scala
case class Model(jobs: List[Job] = List())
```

백엔드 호출을 포함한 초기 설정:

```scala
def backendCall: Cmd[IO, Msg] =
  Http.send(
    Request.get("http://localhost:4041/jobs"),
    Decoder[Msg](
      resp =>
        parse(resp.body).flatMap(_.as[List[Job]]) match {
          case Left(e)     => Msg.Error(e.getMessage())
          case Right(list) => Msg.LoadJobs(list)
        },
      err => Msg.Error(err.toString)
    )
  )

override def init(flags: Map[String, String]): 
    (Model, Cmd[IO, Msg]) =
  (Model(), backendCall)
```

view에서 채용 공고 목록을 렌더링한다.

```scala
override def view(model: Model): Html[Msg] =
  div(`class` := "row")(
    p("This is the first ScalaJS app by Rock the JVM"),
    div(`class` := "contents ")(
      model.jobs.map { job =>
        div(job.toString)
      }
    )
  )
```

update에서 상태 변경을 처리한다.

```scala
override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = 
  msg => msg match {
    case Msg.NoMsg          => (model, Cmd.None)
    case Msg.Error(e)       => (model, Cmd.None)
    case Msg.LoadJobs(list) => 
      (model.copy(jobs = model.jobs ++ list), Cmd.None)
  }
```

HTML 파일에는 Scala가 UI를 마운트할 루트 div 요소가 있다. JavaScript로 컴파일된 앱을 임포트하고 실행한다.

## 전체 실행

모든 구성 요소를 함께 실행한다.

1. **데이터베이스**: `db` 디렉터리에서 `docker-compose up` 실행
2. **백엔드**: server 모듈의 `Application` 오브젝트 시작
3. **프론트엔드 컴파일**: sbt에서 `project app` 후 `~fastOptJS` 실행
4. **로컬 서버**: `app` 디렉터리에서 `npm run start` 실행

`http://localhost:1234`로 접속하면 백엔드에서 가져온 데이터가 표시된 구인 게시판을 볼 수 있다.

## 마무리

이 풀스택 Scala 3 애플리케이션은 백엔드와 프론트엔드 모두에서 Typelevel 라이브러리를 활용하는 방법을 보여준다. JVM과 JavaScript 컴파일 대상 사이에 도메인 모델을 공유할 수 있다는 점은 풀스택 언어로서 Scala가 가진 고유한 장점이다. 모듈형 아키텍처로 관심사를 효과적으로 분리하면서도 애플리케이션 전체에 걸쳐 타입 안전성을 유지한다.
