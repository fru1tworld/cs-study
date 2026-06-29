# Akka 영속성: 쿼리, 플러그인, Durable State

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

## 목차

1. [영속성 쿼리(Persistence Query)](#1-영속성-쿼리persistence-query)
   - [1.1 의존성 설정](#11-의존성-설정)
   - [1.2 개요](#12-개요)
   - [1.3 설계 철학](#13-설계-철학)
   - [1.4 ReadJournal 접근](#14-readjournal-접근)
   - [1.5 미리 정의된 쿼리 타입](#15-미리-정의된-쿼리-타입)
   - [1.6 구체화된 값(Materialized Values)](#16-구체화된-값materialized-values)
   - [1.7 성능과 비정규화(Denormalization)](#17-성능과-비정규화denormalization)
   - [1.8 쿼리 플러그인](#18-쿼리-플러그인)
   - [1.9 스케일 아웃(Scaling Out)](#19-스케일-아웃scaling-out)
   - [1.10 예제 프로젝트](#110-예제-프로젝트)
2. [영속성 플러그인(Persistence Plugins)](#2-영속성-플러그인persistence-plugins)
   - [2.1 개요](#21-개요)
   - [2.2 유지보수되는 플러그인](#22-유지보수되는-플러그인)
   - [2.3 기능 제한 사항](#23-기능-제한-사항)
   - [2.4 플러그인 설정](#24-플러그인-설정)
   - [2.5 사전 패키징된 플러그인](#25-사전-패키징된-플러그인)
   - [2.6 영속성 플러그인 프록시(Persistence Plugin Proxy)](#26-영속성-플러그인-프록시persistence-plugin-proxy)
3. [스키마 진화(Schema Evolution)](#3-스키마-진화schema-evolution)
   - [3.1 의존성 설정](#31-의존성-설정)
   - [3.2 소개](#32-소개)
   - [3.3 이벤트 소스 시스템에서의 스키마 진화](#33-이벤트-소스-시스템에서의-스키마-진화)
   - [3.4 올바른 직렬화 형식 선택](#34-올바른-직렬화-형식-선택)
   - [3.5 스키마 진화 실전](#35-스키마-진화-실전)
4. [영속 상태(Durable State) 영속성](#4-영속-상태durable-state-영속성)
   - [4.1 모듈 정보](#41-모듈-정보)
   - [4.2 소개](#42-소개)
   - [4.3 핵심 구성 요소](#43-핵심-구성-요소)
   - [4.4 이펙트(Effect)와 부수 효과(Side Effect)](#44-이펙트effect와-부수-효과side-effect)
   - [4.5 완전한 예제: Counter](#45-완전한-예제-counter)
   - [4.6 강제된 응답(Enforced Replies)](#46-강제된-응답enforced-replies)
   - [4.7 상태 기반 동작 변경](#47-상태-기반-동작-변경)
   - [4.8 응답(Replies)](#48-응답replies)
   - [4.9 직렬화(Serialization)](#49-직렬화serialization)
   - [4.10 태깅(Tagging)](#410-태깅tagging)
   - [4.11 ActorContext 접근](#411-actorcontext-접근)
   - [4.12 클러스터 샤딩(Cluster Sharding)](#412-클러스터-샤딩cluster-sharding)
   - [4.13 동작 변경 및 가변 상태 고려 사항](#413-동작-변경-및-가변-상태-고려-사항)
5. [영속 상태 스타일 가이드(Durable State Style)](#5-영속-상태-스타일-가이드durable-state-style)
6. [영속 상태 영속성 쿼리(Durable State Persistence Query)](#6-영속-상태-영속성-쿼리durable-state-persistence-query)
7. [참고 자료](#7-참고-자료)

---

## 1. 영속성 쿼리(Persistence Query)

### 1.1 의존성 설정

영속성 쿼리(Persistence Query)를 사용하려면 `akka-persistence-query` 의존성을 추가합니다. sbt의 경우:

```
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion
```

Maven 및 Gradle 설정도 공식 문서에서 제공됩니다.

---

### 1.2 개요

> "Akka 영속성 쿼리(persistence query)는 다양한 저널(journal) 플러그인이 자신의 쿼리 능력을 노출하기 위해 구현할 수 있는, 범용적이고 비동기적인 스트림 기반 쿼리 인터페이스를 제공함으로써 이벤트 소싱(Event Sourcing)을 보완한다."

주요 사용 사례는 CQRS 아키텍처 패턴에서 쿼리 측(query-side) 기능을 구현하는 것으로, 쓰기 작업(write operation)과 읽기 작업(read operation)을 서로 다른 데이터 저장소(datastore)로 분리합니다.

---

### 1.3 설계 철학

이 API는 저널 구현체가 각자의 강점을 최대한 노출할 수 있도록 의도적으로 느슨하게(loosely) 명세되어 있습니다. 각 읽기 저널(read journal)은 자신이 지원하는 쿼리 타입을 명시적으로 문서화해야 합니다.

---

### 1.4 ReadJournal 접근

쿼리를 실행하려면 플러그인 식별자(plugin identifier)를 통해 `ReadJournal` 인스턴스를 얻습니다.

**Scala:**
```scala
val readJournal = 
  PersistenceQuery(system).readJournalFor[MyScaladslReadJournal](
    "akka.persistence.query.my-read-journal")

val source: Source[EventEnvelope, NotUsed] =
  readJournal.eventsByPersistenceId("user-1337", 0, Long.MaxValue)

source.runForeach { event =>
  println("Event: " + event)
}
```

**Java:**
```java
final MyJavadslReadJournal readJournal =
  PersistenceQuery.get(system)
    .getReadJournalFor(
      MyJavadslReadJournal.class, 
      "akka.persistence.query.my-read-journal");

Source<EventEnvelope, NotUsed> source =
  readJournal.eventsByPersistenceId("user-1337", 0L, Long.MAX_VALUE);

source.runForeach(event -> System.out.println("Event: " + event), system);
```

---

### 1.5 미리 정의된 쿼리 타입

#### PersistenceIdsQuery 와 CurrentPersistenceIdsQuery

`persistenceIds()` 는 "시스템 내의 모든 영속 id(persistent id)의 스트림"을 제공하며, 기본적으로 라이브 스트림(live stream)으로 동작하여 새로운 ID가 나타날 때마다 방출(emit)합니다.

`currentPersistenceIds()` 는 라이브 업데이트 없이 현재 존재하는 ID만 반환합니다.

**Scala:**
```scala
readJournal.persistenceIds()
readJournal.currentPersistenceIds()
```

**Java:**
```java
readJournal.persistenceIds();
readJournal.currentPersistenceIds();
```

#### EventsByPersistenceIdQuery 와 CurrentEventsByPersistenceIdQuery

`eventsByPersistenceId()` 는 "이벤트 소스 액터(event sourced actor)를 재생(replay)하는 것과 동등하게" 동작하며, 새로 들어오는 이벤트를 관찰하기 위해 스트림을 유지합니다.

**Scala:**
```scala
readJournal.eventsByPersistenceId(
  "user-us-1337", fromSequenceNr = 0L, toSequenceNr = Long.MaxValue)
```

**Java:**
```java
readJournal.eventsByPersistenceId("user-us-1337", 0L, Long.MAX_VALUE);
```

대부분의 저널은 폴링(polling)을 사용하며, `refresh-interval` 속성으로 설정할 수 있습니다.

`currentEventsByPersistenceId()` 는 라이브 스트리밍 없이 스냅샷(snapshot)과 유사한 동작을 제공합니다.

#### EventsByTag 와 CurrentEventsByTag

"어떤 `persistenceId` 에 연관되어 있는지와 무관하게 이벤트를 쿼리"합니다. 이를 통해 애그리거트 루트(aggregate root)에 걸친 도메인 주도(domain-driven) 이벤트 쿼리가 가능해집니다.

이벤트 태깅(tagging) 구현:

**Scala:**
```scala
val NumberOfEntityGroups = 10

def tagEvent(entityId: String, event: Event): Set[String] = {
  val entityGroup = s"group-${math.abs(entityId.hashCode % NumberOfEntityGroups)}"
  event match {
    case _: OrderCompleted => Set(entityGroup, "order-completed")
    case _                 => Set(entityGroup)
  }
}

def apply(entityId: String): Behavior[Command] = {
  EventSourcedBehavior[Command, Event, State](
    persistenceId = PersistenceId("ShoppingCart", entityId),
    emptyState = State(),
    commandHandler = (state, cmd) => ???,
    eventHandler = (state, evt) => ???)
    .withTagger(event => tagEvent(entityId, event))
}
```

**Java:**
```java
private final String entityId;
public static final int NUMBER_OF_ENTITY_GROUPS = 10;

@Override
public Set<String> tagsFor(Event event) {
  String entityGroup = "group-" + 
    Math.abs(entityId.hashCode() % NUMBER_OF_ENTITY_GROUPS);
  Set<String> tags = new HashSet<>();
  tags.add(entityGroup);
  if (event instanceof OrderCompleted) tags.add("order-completed");
  return tags;
}
```

태깅된 이벤트 쿼리:

**Scala:**
```scala
val completedOrders: Source[EventEnvelope, NotUsed] =
  readJournal.eventsByTag("order-completed", Offset.noOffset)

val firstCompleted: Future[Vector[OrderCompleted]] =
  completedOrders
    .map(_.event)
    .collectType[OrderCompleted]
    .take(10)
    .runFold(Vector.empty[OrderCompleted])(_ :+ _)

val furtherOrders = 
  readJournal.eventsByTag("order-completed", offset = Sequence(10))
```

**Java:**
```java
final Source<EventEnvelope, NotUsed> completedOrders =
  readJournal.eventsByTag("order-completed", new Sequence(0L));

final CompletionStage<List<OrderCompleted>> firstCompleted =
  completedOrders
    .map(EventEnvelope::event)
    .collectType(OrderCompleted.class)
    .take(10)
    .runFold(
      new ArrayList<>(10),
      (acc, e) -> {
        acc.add(e);
        return acc;
      },
      system);

Source<EventEnvelope, NotUsed> furtherOrders =
  readJournal.eventsByTag("order-completed", new Sequence(10));
```

**중요한 고려 사항:** "EventsByTag 처럼 여러 persistenceId 에 걸친 쿼리를 사용할 때 매우 중요하게 염두에 두어야 할 점은, 이벤트가 스트림에 나타나는 순서가 보장되는 경우가 드물다는 것이다(그리고 여러 번의 구체화(materialization) 사이에서도 안정적이지 않다)." 저널은 특정 순서 보장(ordering guarantee)을 문서화할 수도 있습니다.

#### EventsBySlice 와 CurrentEventsBySlice

지정된 엔티티 타입(entity type)과 슬라이스 범위(slice range)에 대한 이벤트를 쿼리합니다. "슬라이스(slice)는 영속 id를 기반으로 결정론적으로(deterministically) 정의된다. 그 목적은 모든 영속 id를 슬라이스 전반에 고르게 분산시키는 것이다."

변형(variation)으로는 다음이 있습니다:
- `EventsBySliceStartingFromSnapshotsQuery` / `CurrentEventsBySliceStartingFromSnapshotsQuery`
- 여러 소비자(consumer)에 대한 확장성 개선을 위한 `EventsBySliceFirehoseQuery`

---

### 1.6 구체화된 값(Materialized Values)

저널은 스트림 구체화(stream materialization)를 통해 추가적인 메타데이터를 제공합니다. 구체화된 값의 타입은 반환된 `Source` 의 두 번째 타입 매개변수입니다.

**Scala:**
```scala
final case class RichEvent(tags: Set[String], payload: Any)

case class QueryMetadata(deterministicOrder: Boolean, infinite: Boolean)

def byTagsWithMeta(tags: Set[String]): Source[RichEvent, QueryMetadata] = {
  // implementation
}

val query: Source[RichEvent, QueryMetadata] =
  readJournal.byTagsWithMeta(Set("red", "blue"))

query
  .mapMaterializedValue { meta =>
    println(
      s"The query is: " +
      s"ordered deterministically: ${meta.deterministicOrder}, " +
      s"infinite: ${meta.infinite}")
  }
  .map { event =>
    println(s"Event payload: ${event.payload}")
  }
  .runWith(Sink.ignore)
```

**Java:**
```java
static class RichEvent {
  public final Set<String> tags;
  public final Object payload;

  public RichEvent(Set<String> tags, Object payload) {
    this.tags = tags;
    this.payload = payload;
  }
}

static final class QueryMetadata {
  public final boolean deterministicOrder;
  public final boolean infinite;

  public QueryMetadata(boolean deterministicOrder, boolean infinite) {
    this.deterministicOrder = deterministicOrder;
    this.infinite = infinite;
  }
}

public Source<RichEvent, QueryMetadata> byTagsWithMeta(Set<String> tags) {
  // implementation
}

Set<String> tags = new HashSet<String>();
tags.add("red");
tags.add("blue");
final Source<RichEvent, QueryMetadata> events =
  readJournal
    .byTagsWithMeta(tags)
    .mapMaterializedValue(
      meta -> {
        System.out.println(
          "The query is: "
          + "ordered deterministically: "
          + meta.deterministicOrder
          + " "
          + "infinite: "
          + meta.infinite);
        return meta;
      });

events
  .map(
    event -> {
      System.out.println("Event payload: " + event.payload);
      return event.payload;
    })
  .runWith(Sink.ignore(), system);
```

---

### 1.7 성능과 비정규화(Denormalization)

쓰기 측(write-side)과 읽기 측(read-side)은 근본적으로 서로 다른 최적화 요구 사항을 갖습니다. 관심사(concern)를 전용 데이터 저장소로 분리하면 각각에 대해 최적의 성능을 달성할 수 있습니다.

- **쓰기 측(Write-side)** 은 처리량(throughput)과 빠른 확인(acknowledgment)을 우선시합니다.
- **읽기 측(Read-side)** 은 표현력 있는 쿼리 능력(query capability)을 요구합니다.

"구체화된 뷰(Materialized View)"는 "쿼리 결과의 영속적인 저장(persistent storage)"을 의미하며, 소스 이벤트로부터 매번 재계산하는 것 대신 효율적인 반복 접근을 가능하게 합니다.

#### Reactive Streams 호환 데이터 저장소

대상 데이터 저장소가 Reactive Streams 를 구현하는 경우:

**Scala:**
```scala
implicit val system: ActorSystem = ActorSystem()

val readJournal =
  PersistenceQuery(system).readJournalFor[MyScaladslReadJournal](JournalId)
val dbBatchWriter: Subscriber[immutable.Seq[Any]] =
  ReactiveStreamsCompatibleDBDriver.batchWriter

readJournal
  .eventsByPersistenceId(
    "user-1337", fromSequenceNr = 0L, toSequenceNr = Long.MaxValue)
  .map(envelope => envelope.event)
  .map(convertToReadSideTypes)
  .grouped(20)
  .runWith(Sink.fromSubscriber(dbBatchWriter))
```

**Java:**
```java
final ReactiveStreamsCompatibleDBDriver driver = 
  new ReactiveStreamsCompatibleDBDriver();
final Subscriber<List<Object>> dbBatchWriter = driver.batchWriter();

readJournal
  .eventsByPersistenceId("user-1337", 0L, Long.MAX_VALUE)
  .map(envelope -> envelope.event())
  .grouped(20)
  .runWith(Sink.fromSubscriber(dbBatchWriter), system);
```

#### mapAsync 통합

Reactive Streams 지원이 없는 데이터베이스의 경우:

**Scala:**
```scala
trait ExampleStore {
  def save(event: Any): Future[Unit]
}

val store: ExampleStore = ???

readJournal
  .eventsByTag("bid", NoOffset)
  .mapAsync(1) { e =>
    store.save(e)
  }
  .runWith(Sink.ignore)
```

**Java:**
```java
static class ExampleStore {
  CompletionStage<Void> save(Object any) {
    // ...
  }
}

final ExampleStore store = new ExampleStore();

readJournal
  .eventsByTag("bid", new Sequence(0L))
  .mapAsync(1, store::save)
  .runWith(Sink.ignore(), system);
```

#### 재개 가능한 프로젝션(Resumable Projections)

오프셋(offset)을 영속화하는 상태 기반 프로젝션의 경우: "처리된 이벤트의 시퀀스 번호(sequence number, 또는 `offset`)가 저장되고, 다음번에 이 프로젝션이 시작될 때 사용된다." [Akka Projections](https://doc.akka.io/libraries/akka-projection/current/) 모듈이 이 패턴을 구현합니다.

---

### 1.8 쿼리 플러그인

읽기 저널 구현체는 특정 데이터 저장소를 대상으로 하는 커뮤니티 플러그인(community plugin)으로 제공됩니다. 전체 목록은 [Community Plugins](https://akka.io/community/#plugins-to-akka-persistence-query) 페이지에 나와 있습니다.

#### ReadJournal 플러그인 API

플러그인은 `akka.persistence.query.ReadJournalProvider` 를 구현해야 하며, `scaladsl.ReadJournal` 과 `javadsl.ReadJournal` 인스턴스를 모두 생성해야 합니다. 두 인스턴스의 `Source` 타입이 다르기 때문입니다.

**Scala 예제:**
```scala
class MyReadJournalProvider(system: ExtendedActorSystem, config: Config) 
    extends ReadJournalProvider {

  private val readJournal: MyScaladslReadJournal =
    new MyScaladslReadJournal(system, config)

  override def scaladslReadJournal(): MyScaladslReadJournal =
    readJournal

  override def javadslReadJournal(): MyJavadslReadJournal =
    new MyJavadslReadJournal(readJournal)
}

class MyScaladslReadJournal(system: ExtendedActorSystem, config: Config)
    extends akka.persistence.query.scaladsl.ReadJournal
    with akka.persistence.query.scaladsl.EventsByTagQuery
    with akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
    with akka.persistence.query.scaladsl.PersistenceIdsQuery
    with akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery {

  private val refreshInterval: FiniteDuration =
    config.getDuration("refresh-interval", MILLISECONDS).millis

  override def eventsByTag(tag: String, offset: Offset): 
      Source[EventEnvelope, NotUsed] = offset match {
    case Sequence(offsetValue) =>
      Source.fromGraph(new MyEventsByTagSource(tag, offsetValue, refreshInterval))
    case NoOffset => eventsByTag(tag, Sequence(0L))
    case _ =>
      throw new IllegalArgumentException(
        "MyJournal does not support " + offset.getClass.getName + " offsets")
  }

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long): Source[EventEnvelope, NotUsed] = {
    ???
  }

  override def persistenceIds(): Source[String, NotUsed] = {
    ???
  }

  override def currentPersistenceIds(): Source[String, NotUsed] = {
    ???
  }

  def byTagsWithMeta(tags: Set[String]): Source[RichEvent, QueryMetadata] = {
    ???
  }
}

class MyJavadslReadJournal(scaladslReadJournal: MyScaladslReadJournal)
    extends akka.persistence.query.javadsl.ReadJournal
    with akka.persistence.query.javadsl.EventsByTagQuery
    with akka.persistence.query.javadsl.EventsByPersistenceIdQuery
    with akka.persistence.query.javadsl.PersistenceIdsQuery
    with akka.persistence.query.javadsl.CurrentPersistenceIdsQuery {

  override def eventsByTag(tag: String, offset: Offset = Sequence(0L)): 
      javadsl.Source[EventEnvelope, NotUsed] =
    scaladslReadJournal.eventsByTag(tag, offset).asJava

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long = 0L,
      toSequenceNr: Long = Long.MaxValue): 
      javadsl.Source[EventEnvelope, NotUsed] =
    scaladslReadJournal.eventsByPersistenceId(
      persistenceId, fromSequenceNr, toSequenceNr).asJava

  override def persistenceIds(): javadsl.Source[String, NotUsed] =
    scaladslReadJournal.persistenceIds().asJava

  override def currentPersistenceIds(): javadsl.Source[String, NotUsed] =
    scaladslReadJournal.currentPersistenceIds().asJava

  def byTagsWithMeta(tags: java.util.Set[String]): 
      javadsl.Source[RichEvent, QueryMetadata] = {
    import scala.jdk.CollectionConverters._
    scaladslReadJournal.byTagsWithMeta(tags.asScala.toSet).asJava
  }
}
```

**Java 예제:**
```java
static class MyReadJournalProvider implements ReadJournalProvider {
  private final MyJavadslReadJournal javadslReadJournal;

  public MyReadJournalProvider(ExtendedActorSystem system, Config config) {
    this.javadslReadJournal = new MyJavadslReadJournal(system, config);
  }

  @Override
  public MyScaladslReadJournal scaladslReadJournal() {
    return new MyScaladslReadJournal(javadslReadJournal);
  }

  @Override
  public MyJavadslReadJournal javadslReadJournal() {
    return this.javadslReadJournal;
  }
}

static class MyJavadslReadJournal
    implements akka.persistence.query.javadsl.ReadJournal,
        akka.persistence.query.javadsl.EventsByTagQuery,
        akka.persistence.query.javadsl.EventsByPersistenceIdQuery,
        akka.persistence.query.javadsl.PersistenceIdsQuery,
        akka.persistence.query.javadsl.CurrentPersistenceIdsQuery {

  private final Duration refreshInterval;
  private Connection conn;

  public MyJavadslReadJournal(ExtendedActorSystem system, Config config) {
    refreshInterval = config.getDuration("refresh-interval");
  }

  @Override
  public Source<EventEnvelope, NotUsed> eventsByTag(
      String tag, Offset offset) {
    if (offset instanceof Sequence) {
      Sequence sequenceOffset = (Sequence) offset;
      return Source.fromGraph(
        new MyEventsByTagSource(
          conn, tag, sequenceOffset.value(), refreshInterval));
    } else if (offset == NoOffset.getInstance())
      return eventsByTag(tag, Offset.sequence(0L));
    else
      throw new IllegalArgumentException(
        "MyJavadslReadJournal does not support " + 
        offset.getClass().getName() + " offsets");
  }

  @Override
  public Source<EventEnvelope, NotUsed> eventsByPersistenceId(
      String persistenceId, long fromSequenceNr, long toSequenceNr) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Source<String, NotUsed> persistenceIds() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Source<String, NotUsed> currentPersistenceIds() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public Source<RichEvent, QueryMetadata> byTagsWithMeta(Set<String> tags) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}

static class MyScaladslReadJournal
    implements akka.persistence.query.scaladsl.ReadJournal,
        akka.persistence.query.scaladsl.EventsByTagQuery,
        akka.persistence.query.scaladsl.EventsByPersistenceIdQuery,
        akka.persistence.query.scaladsl.PersistenceIdsQuery,
        akka.persistence.query.scaladsl.CurrentPersistenceIdsQuery {

  private final MyJavadslReadJournal javadslReadJournal;

  public MyScaladslReadJournal(MyJavadslReadJournal javadslReadJournal) {
    this.javadslReadJournal = javadslReadJournal;
  }

  @Override
  public akka.stream.scaladsl.Source<EventEnvelope, NotUsed> eventsByTag(
      String tag, akka.persistence.query.Offset offset) {
    return javadslReadJournal.eventsByTag(tag, offset).asScala();
  }

  @Override
  public akka.stream.scaladsl.Source<EventEnvelope, NotUsed> 
      eventsByPersistenceId(
      String persistenceId, long fromSequenceNr, long toSequenceNr) {
    return javadslReadJournal
      .eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr)
      .asScala();
  }

  @Override
  public akka.stream.scaladsl.Source<String, NotUsed> persistenceIds() {
    return javadslReadJournal.persistenceIds().asScala();
  }

  @Override
  public akka.stream.scaladsl.Source<String, NotUsed> 
      currentPersistenceIds() {
    return javadslReadJournal.currentPersistenceIds().asScala();
  }

  public akka.stream.scaladsl.Source<RichEvent, QueryMetadata> 
      byTagsWithMeta(scala.collection.Set<String> tags) {
    Set<String> jTags = 
      scala.jdk.javaapi.CollectionConverters.asJava(tags);
    return javadslReadJournal.byTagsWithMeta(jTags).asScala();
  }
}
```

`eventsByTag` 를 위한 GraphStage 구현:

**Scala:**
```scala
class MyEventsByTagSource(tag: String, offset: Long, refreshInterval: FiniteDuration)
    extends GraphStage[SourceShape[EventEnvelope]] {

  private case object Continue
  val out: Outlet[EventEnvelope] = Outlet("MyEventByTagSource.out")
  override def shape: SourceShape[EventEnvelope] = SourceShape(out)

  override protected def initialAttributes: Attributes = 
    Attributes(ActorAttributes.IODispatcher)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with OutHandler {
      lazy val system = materializer.system
      private val Limit = 1000
      private val connection: java.sql.Connection = ???
      private var currentOffset = offset
      private var buf = Vector.empty[EventEnvelope]
      private val serialization = SerializationExtension(system)

      override def preStart(): Unit = {
        scheduleWithFixedDelay(Continue, refreshInterval, refreshInterval)
      }

      override def onPull(): Unit = {
        query()
        tryPush()
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
        // close connection if responsible for doing so
      }

      private def query(): Unit = {
        if (buf.isEmpty) {
          try {
            buf = Select.run(tag, currentOffset, Limit)
          } catch {
            case NonFatal(e) =>
              failStage(e)
          }
        }
      }

      private def tryPush(): Unit = {
        if (buf.nonEmpty && isAvailable(out)) {
          push(out, buf.head)
          buf = buf.tail
        }
      }

      override protected def onTimer(timerKey: Any): Unit = timerKey match {
        case Continue =>
          query()
          tryPush()
      }

      object Select {
        private def statement() =
          connection.prepareStatement("""
            SELECT id, persistence_id, seq_nr, serializer_id, 
                   serializer_manifest, payload
            FROM journal WHERE tag = ? AND id > ?
            ORDER BY id LIMIT ?
          """)

        def run(tag: String, from: Long, limit: Int): Vector[EventEnvelope] = {
          val s = statement()
          try {
            s.setString(1, tag)
            s.setLong(2, from)
            s.setLong(3, limit)
            val rs = s.executeQuery()

            val b = Vector.newBuilder[EventEnvelope]
            while (rs.next()) {
              val deserialized = serialization
                .deserialize(
                  rs.getBytes("payload"), 
                  rs.getInt("serializer_id"), 
                  rs.getString("serializer_manifest"))
                .get
              currentOffset = rs.getLong("id")
              b += EventEnvelope(
                Offset.sequence(currentOffset),
                rs.getString("persistence_id"),
                rs.getLong("seq_nr"),
                deserialized,
                System.currentTimeMillis())
            }
            b.result()
          } finally s.close()
        }
      }
    }
}
```

**Java:**
```java
public class MyEventsByTagSource extends GraphStage<SourceShape<EventEnvelope>> {
  public Outlet<EventEnvelope> out = Outlet.create("MyEventByTagSource.out");
  private static final String QUERY =
    "SELECT id, persistence_id, seq_nr, serializer_id, " +
    "serializer_manifest, payload " +
    "FROM journal WHERE tag = ? AND id > ? " +
    "ORDER BY id LIMIT ?";

  enum Continue {
    INSTANCE;
  }

  private static final int LIMIT = 1000;
  private final Connection connection;
  private final String tag;
  private final long initialOffset;
  private final Duration refreshInterval;

  public MyEventsByTagSource(
      Connection connection, String tag, long initialOffset, 
      Duration refreshInterval) {
    this.connection = connection;
    this.tag = tag;
    this.initialOffset = initialOffset;
    this.refreshInterval = refreshInterval;
  }

  @Override
  public Attributes initialAttributes() {
    return Attributes.apply(ActorAttributes.IODispatcher());
  }

  @Override
  public SourceShape<EventEnvelope> shape() {
    return SourceShape.of(out);
  }

  @Override
  public GraphStageLogic createLogic(Attributes inheritedAttributes) {
    return new TimerGraphStageLogic(shape()) {
      private ActorSystem system = materializer().system();
      private long currentOffset = initialOffset;
      private List<EventEnvelope> buf = new LinkedList<>();
      private final Serialization serialization = SerializationExtension.get(system);

      @Override
      public void preStart() {
        scheduleWithFixedDelay(Continue.INSTANCE, refreshInterval, refreshInterval);
      }

      @Override
      public void onTimer(Object timerKey) {
        query();
        deliver();
      }

      private void deliver() {
        if (isAvailable(out) && !buf.isEmpty()) {
          push(out, buf.remove(0));
        }
      }

      private void query() {
        if (buf.isEmpty()) {
          try (PreparedStatement s = connection.prepareStatement(QUERY)) {
            s.setString(1, tag);
            s.setLong(2, currentOffset);
            s.setLong(3, LIMIT);
            try (ResultSet rs = s.executeQuery()) {
              final List<EventEnvelope> res = new ArrayList<>(LIMIT);
              while (rs.next()) {
                Object deserialized =
                  serialization
                    .deserialize(
                      rs.getBytes("payload"),
                      rs.getInt("serializer_id"),
                      rs.getString("serializer_manifest"))
                    .get();
                currentOffset = rs.getLong("id");
                res.add(
                  new EventEnvelope(
                    Offset.sequence(currentOffset),
                    rs.getString("persistence_id"),
                    rs.getLong("seq_nr"),
                    deserialized,
                    System.currentTimeMillis()));
              }
              buf = res;
            }
          } catch (Exception e) {
            failStage(e);
          }
        }
      }

      {
        setHandler(
          out,
          new AbstractOutHandler() {
            @Override
            public void onPull() {
              query();
              deliver();
            }
          });
      }
    };
  }
}
```

#### 플러그인 생성자 요구 사항

`ReadJournalProvider` 클래스는 다음 시그니처 중 하나에 부합하는 생성자를 가져야 합니다:

- `ExtendedActorSystem`, `Config`, `String` 매개변수를 받는 생성자
- `ExtendedActorSystem`, `Config` 매개변수를 받는 생성자
- `ExtendedActorSystem` 매개변수 하나만 받는 생성자
- 매개변수가 없는 생성자

유한한 결과 집합(finite result set)만 지원하는 데이터 저장소의 경우, 저널은 무한 스트림(infinite stream)을 모사하기 위해 주기적인 쿼리를 발행해야 하며, 설정 가능한 `refresh-interval` 속성을 사용합니다.

---

### 1.9 스케일 아웃(Scaling Out)

높은 이벤트 볼륨이나 복원력(resilience) 요구 사항을 위해, "클러스터 샤딩(Cluster Sharding)을 이벤트 태깅과 함께 사용하면 이벤트를 클러스터 전반에 걸쳐 샤딩(shard)하는 데 매우 적합하다." 이를 통해 수평 확장과 노드 장애로부터의 복구가 가능해집니다.

---

### 1.10 예제 프로젝트

[Akka로 만드는 마이크로서비스 튜토리얼(Microservices with Akka tutorial)](https://doc.akka.io/libraries/guide/microservices-tutorial/) 은 CQRS 패턴을 사용한 이벤트 소싱과, 이벤트 스트림으로부터 대안적인 데이터 표현(alternative data representation)을 구축하기 위한 Projections 통합을 보여줍니다.

---

## 2. 영속성 플러그인(Persistence Plugins)

### 2.1 개요

Akka 영속성은 저널(journal), 스냅샷 스토어(snapshot store), 영속 상태 스토어(durable state store), 그리고 영속성 쿼리(persistence query)를 위한 플러그형(pluggable) 스토리지 백엔드를 지원합니다. Akka 팀은 여러 프로덕션 준비된(production-ready) 플러그인을 유지보수합니다.

---

### 2.2 유지보수되는 플러그인

#### R2DBC 플러그인

"반응형 데이터베이스 드라이버(Reactive database drivers, R2DBC)는 PostgreSQL, H2(최소한의 인-프로세스 메모리 또는 파일 기반 데이터베이스로서), Yugabyte 같은 관계형 데이터베이스를 지원한다." 이 구현체는 "Akka Persistence의 최신 기능을 지원하며, 일반적으로 JDBC 기반 플러그인보다 권장된다"고 명시되어 있습니다.

#### Cassandra 플러그인

Akka는 전용 Cassandra 영속성 구현체를 통해 지원을 제공합니다. 다만 Durable State 를 포함한 일부 최신 기능은 이 플러그인에서 지원되지 않습니다.

#### AWS DynamoDB 플러그인

DynamoDB는 백엔드로 사용할 수 있으나, Durable State 와 마지막 이벤트만으로부터의 복구(recovery from only the last event)는 지원되지 않는 기능입니다.

#### JDBC 플러그인

"JDBC 드라이버를 사용하는 관계형 데이터베이스"가 이 구현체를 통해 지원됩니다. 신규 프로젝트의 경우, 일부 후속 기능 추가 사항이 지원되지 않으므로 R2DBC 옵션이 권장됩니다.

---

### 2.3 기능 제한 사항

Cassandra 및 JDBC 플러그인에서는 다음과 같은 여러 기능이 지원되지 않습니다: `eventsBySlices` 쿼리, gRPC를 통한 Projections, gRPC를 통한 복제된 이벤트 소싱(Replicated Event Sourcing), 동적 Projection 스케일링, 저지연(low latency) Projections, 스냅샷으로부터의 Projections, 그리고 Durable State 엔티티.

---

### 2.4 플러그인 설정

#### 기본값 vs 개별 선택

영속 액터가 `journalPluginId` 와 `snapshotPluginId` 메서드를 오버라이드하지 않으면, 시스템은 다음을 통해 설정된 기본 플러그인을 사용합니다:

```
akka.persistence.journal.plugin = ""
akka.persistence.snapshot-store.plugin = ""
akka.persistence.state.plugin = ""
```

이들은 사용자의 `application.conf` 에서 명시적으로 설정되어야 합니다.

#### 즉시 초기화(Eager Initialization)

기본적으로 플러그인은 필요할 때(on-demand) 시작됩니다. 즉시 초기화(eager initialization)를 활성화하려면, `akka.extensions` 아래에 `akka.persistence.Persistence` 를 추가하고, `akka.persistence.journal.auto-start-journals` 와 `akka.persistence.snapshot-store.auto-start-snapshot-stores` 아래에 플러그인 ID를 지정합니다.

설정 예시:

```
akka {
  extensions = [akka.persistence.Persistence]
  persistence {
    journal {
      plugin = "akka.persistence.journal.leveldb"
      auto-start-journals = ["akka.persistence.journal.leveldb"]
    }
    snapshot-store {
      plugin = "akka.persistence.snapshot-store.local"
      auto-start-snapshot-stores = ["akka.persistence.snapshot-store.local"]
    }
  }
}
```

---

### 2.5 사전 패키징된 플러그인

#### 로컬 LevelDB 저널(Local LevelDB Journal)

⚠️ **사용 중단(Deprecation) 경고:** "LevelDB 저널은 사용 중단(deprecated)되었으며 이를 사용하여 새 애플리케이션을 구축하는 것은 권장되지 않는다. 대체재로 Akka Persistence JDBC 사용을 권장한다."

LevelDB는 로컬 파일시스템 스토리지를 사용하기 때문에 Akka 클러스터에서 사용할 수 없습니다. 활성화 방법:

```
akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
```

필요한 의존성:

**sbt:**
```
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
```

**Maven:**
```xml
<dependency>
  <groupId>org.fusesource.leveldbjni</groupId>
  <artifactId>leveldbjni-all</artifactId>
  <version>1.8</version>
</dependency>
```

**Gradle:**
```
implementation "org.fusesource.leveldbjni:leveldbjni-all:1.8"
```

스토리지 위치 설정:

```
akka.persistence.journal.leveldb.dir = "target/journal"
```

LevelDB는 메시지를 제거하는 대신 "툼스톤(tombstone)"을 추가하는 방식으로 삭제를 처리합니다. 잦은 삭제가 동반되는 과중한 사용의 경우, 저널 압축(journal compaction)을 사용할 수 있습니다:

```
akka.persistence.journal.leveldb.compaction-intervals {
  persistence-id-1 = 100
  persistence-id-2 = 200
  persistence-id-N = 1000
  "*" = 250
}
```

#### 공유 LevelDB 저널(Shared LevelDB Journal)

⚠️ **사용 중단(Deprecated):** 이는 "영속성 플러그인 프록시(Persistence Plugin Proxy)로 대체되었다."

공유 스토어 인스턴스 생성:

**Scala:**
```scala
import akka.persistence.journal.leveldb.SharedLeveldbStore
val store = system.actorOf(Props[SharedLeveldbStore](), "store")
```

**Java:**
```java
final ActorRef store = system.actorOf(Props.create(SharedLeveldbStore.class), "store");
```

스토리지 위치 설정:

```
akka.persistence.journal.leveldb-shared.store.dir = "target/shared"
```

플러그인 활성화:

```
akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
```

`SharedLeveldbJournal.setStore()` 를 통해 스토어 참조를 주입(inject):

**Scala:**
```scala
trait SharedStoreUsage extends Actor {
  override def preStart(): Unit = {
    context.actorSelection("akka://example@127.0.0.1:2552/user/store") ! Identify(1)
  }
  def receive = {
    case ActorIdentity(1, Some(store)) =>
      SharedLeveldbJournal.setStore(store, context.system)
  }
}
```

**Java:**
```java
class SharedStorageUsage extends AbstractActor {
  @Override
  public void preStart() throws Exception {
    String path = "akka://example@127.0.0.1:2552/user/store";
    ActorSelection selection = getContext().actorSelection(path);
    selection.tell(new Identify(1), getSelf());
  }
  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            ActorIdentity.class,
            ai -> {
              if (ai.correlationId().equals(1)) {
                Optional<ActorRef> store = ai.getActorRef();
                if (store.isPresent()) {
                  SharedLeveldbJournal.setStore(store.get(), getContext().getSystem());
                } else {
                  throw new RuntimeException("Couldn't identify store");
                }
              }
            })
        .build();
  }
}
```

주입은 멱등(idempotent)하며, 완료될 때까지 내부 명령을 버퍼링합니다.

#### 로컬 스냅샷 스토어(Local Snapshot Store)

⚠️ 로컬 파일시스템 제약으로 인해 Akka 클러스터에서 사용할 수 없습니다.

활성화 방법:

```
akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
```

위치 설정:

```
akka.persistence.snapshot-store.local.dir = "target/snapshots"
```

스냅샷을 사용하지 않는 경우 스냅샷 스토어 지정은 선택 사항입니다.

---

### 2.6 영속성 플러그인 프록시(Persistence Plugin Proxy)

테스트용으로, 이 프록시는 "단일 노드의 저널과 스냅샷 스토어를 여러 액터 시스템(같은 노드 또는 다른 노드 상의)에 걸쳐 공유할 수 있게 한다."

⚠️ **경고:** "공유 저널/스냅샷 스토어는 단일 장애 지점(single point of failure)이며 테스트 목적으로만 사용해야 한다."

`akka.persistence.journal.proxy` 와 `akka.persistence.snapshot-store.proxy` 항목을 통해 설정합니다. `target-journal-plugin` 또는 `target-snapshot-store-plugin` 을 기반 플러그인(예: `akka.persistence.journal.inmem`)으로 설정합니다. 하나의 시스템에서 `start-target-journal` 또는 `start-target-snapshot-store` 를 `on` 으로 설정하여 대상 인스턴스 생성을 활성화합니다.

공유 플러그인은 `target-journal-address` 및 `target-snapshot-store-address` 설정 키를 통해, 또는 프로그래밍 방식으로 `PersistencePluginProxy.setTargetLocation()` 을 사용하여 찾습니다.

Akka는 확장(extension)을 지연 로딩(lazily)하므로, 대상 플러그인이 로드되도록 보장하려면 `PersistencePluginProxyExtension` 을 인스턴스화하거나 `PersistencePluginProxy.start()` 를 호출하십시오.

---

## 3. 스키마 진화(Schema Evolution)

### 3.1 의존성 설정

#### sbt
```
val AkkaVersion = "2.10.19"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test
)
```

#### Maven
```xml
<properties>
  <scala.binary.version>2.13</scala.binary.version>
</properties>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.typesafe.akka</groupId>
      <artifactId>akka-bom_${scala.binary.version}</artifactId>
      <version>2.10.19</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-persistence_${scala.binary.version}</artifactId>
  </dependency>
  <dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-persistence-testkit_${scala.binary.version}</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

#### Gradle
```
def versions = [
  ScalaBinary: "2.13"
]
dependencies {
  implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
  implementation "com.typesafe.akka:akka-persistence_${versions.ScalaBinary}"
  testImplementation "com.typesafe.akka:akka-persistence-testkit_${versions.ScalaBinary}"
}
```

---

### 3.2 소개

스키마 진화(schema evolution)는 이벤트 소싱을 사용하는 장기 운영 프로젝트에서 매우 중요합니다. 비즈니스 요구 사항과 도메인에 대한 이해가 발전함에 따라 영속화된 이벤트의 데이터 구조도 그에 맞게 변화해야 합니다. 스키마 변경이 필요한 성공적인 프로젝트는 활발한 사용과 지속적인 비즈니스 성장을 의미합니다.

---

### 3.3 이벤트 소스 시스템에서의 스키마 진화

#### 근본적 특성

이벤트 소싱은 스키마 진화와 관련하여 다음과 같은 특유의 도전 과제를 수반합니다:

- 시스템은 대규모 마이그레이션을 요구하지 않고 계속 운영되어야 합니다.
- 시스템은 스토리지로부터 오래된 이벤트를 읽어와 현재 형식으로 애플리케이션 로직에 제시해야 합니다.
- 이벤트는 복구(recovery)나 쿼리 시 최신 버전으로 투명하게(transparently) 변환(promote)되어야 합니다.
- 비즈니스 로직은 여러 이벤트 버전의 존재를 인식하지 않아도 되어야 합니다.

#### 스키마 진화의 유형

가장 흔한 스키마 변경은 다음과 같습니다:

1. 이벤트 타입에 필드 추가
2. 이벤트 타입의 필드 제거 또는 이름 변경
3. 이벤트 타입 전체 제거
4. 큰 이벤트를 여러 개의 작은 이벤트로 분할

---

### 3.4 올바른 직렬화 형식 선택

#### 선택 시 고려 사항

직렬화 형식(serialization format) 선택은 어떤 스키마 진화가 쉬운지 혹은 어려운지를 크게 좌우합니다. 중요한 고려 요소는 다음과 같습니다:

- 스키마 진화 능력
- 새로운 데이터타입에 대한 개발 및 유지보수 노력
- 직렬화 성능 특성

**권장 접근:** Jackson 직렬화는 많은 경우에 좋은 스키마 진화 지원을 제공합니다.

#### 직렬화 형식 옵션

**IDL 기반 바이너리 형식(IDL-based binary format)** 은 장기간 운영되는 애플리케이션에 적합합니다:
- Google Protocol Buffers - 스키마 진화에 대해 더 많은 제어 제공
- Apache Thrift - 유연한 IDL 기반 접근
- Apache Avro - 스키마 레지스트리(schema registry)를 사용한 전체 스키마 기반 진화

#### 제공되는 기본 직렬화기(Provided Default Serializers)

Akka Persistence는 `PersistentRepr` 와 `AtomicWrite` 같은 내부 타입을 위해 Google Protocol Buffers 기반 직렬화기를 제공합니다. 저널 플러그인 구현체는 이 제공된 직렬화기를 사용하거나, 자신의 기반 데이터베이스에 더 적합한 형식을 선택할 수 있습니다.

**중요한 노트:** "직렬화(Serialization)는 Akka Persistence 자체가 자동으로 처리하지 않는다." 프레임워크는 직렬화기를 제공하지만, 그 사용 여부는 저널 플러그인이 결정합니다. 일부 저널은 Akka Serialization 대신 해당 데이터 저장소에 적합한 형식(JSON 등)을 사용할 수 있습니다.

#### 직렬화 엔벨로프(Envelope) 구조

프레임워크는 사용자 페이로드(payload)를 영속성 메타데이터가 포함된 엔벨로프(envelope)로 감쌉니다. 저널이 래퍼(wrapper) 타입에 제공된 Protobuf 직렬화기를 사용하는 경우, 사용자 이벤트 페이로드는 설정된 직렬화기로 직렬화됩니다(별도로 지정하지 않으면 기본적으로 Java 직렬화가 사용됩니다).

**치명적인 경고:** 스키마 진화 지원이 빈약하고 성능 특성이 낮으므로, 진지한 애플리케이션 개발에서 Java 직렬화에 의존하지 마십시오.

#### 페이로드 직렬화기 설정

커스텀 직렬화기는 Akka Serialization 설정을 통해 등록할 수 있습니다. 다음은 최소한의 예시입니다.

**도메인 모델:**

Scala:
```scala
final case class Person(name: String, surname: String)
```

Java:
```java
static class Person {
  public final String name;
  public final String surname;
  
  public Person(String name, String surname) {
    this.name = name;
    this.surname = surname;
  }
}
```

**커스텀 직렬화기 구현:**

Scala:
```scala
class SimplestPossiblePersonSerializer extends SerializerWithStringManifest {
  val Utf8 = Charset.forName("UTF-8")
  
  val PersonManifest = classOf[Person].getName
  
  def identifier = 1234567
  
  override def manifest(o: AnyRef): String = o.getClass.getName
  
  override def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case p: Person => s"""${p.name}|${p.surname}""".getBytes(Utf8)
    case _         => throw new IllegalArgumentException(s"Unable to serialize to bytes, clazz was: ${obj.getClass}!")
  }
  
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case PersonManifest =>
        val nameAndSurname = new String(bytes, Utf8)
        val Array(name, surname) = nameAndSurname.split("[|]")
        Person(name, surname)
      case _ =>
        throw new NotSerializableException(
          s"Unable to deserialize from bytes, manifest was: $manifest! Bytes length: " +
          bytes.length)
    }
}
```

Java:
```java
static class SimplestPossiblePersonSerializer extends SerializerWithStringManifest {
  private final Charset utf8 = StandardCharsets.UTF_8;
  
  private final String personManifest = Person.class.getName();
  
  @Override
  public int identifier() {
    return 1234567;
  }
  
  @Override
  public String manifest(Object o) {
    return o.getClass().getName();
  }
  
  @Override
  public byte[] toBinary(Object obj) {
    if (obj instanceof Person) {
      Person p = (Person) obj;
      return (p.name + "|" + p.surname).getBytes(utf8);
    } else {
      throw new IllegalArgumentException(
          "Unable to serialize to bytes, clazz was: " + obj.getClass().getName());
    }
  }
  
  @Override
  public Object fromBinary(byte[] bytes, String manifest) throws NotSerializableException {
    if (personManifest.equals(manifest)) {
      String nameAndSurname = new String(bytes, utf8);
      String[] parts = nameAndSurname.split("[|]");
      return new Person(parts[0], parts[1]);
    } else {
      throw new NotSerializableException(
          "Unable to deserialize from bytes, manifest was: "
              + manifest
              + "! Bytes length: "
              + bytes.length);
    }
  }
}
```

**설정 (application.conf):**
```
akka {
  actor {
    serializers {
      person = "docs.persistence.SimplestPossiblePersonSerializer"
    }
    
    serialization-bindings {
      "docs.persistence.Person" = person
    }
  }
}
```

---

### 3.5 스키마 진화 실전

#### 필드 추가(Adding Fields)

**상황:** 기존 메시지 타입에 새 필드 추가. 예: `SeatReserved(letter: String, row: Int)` 에 창가 또는 통로 좌석을 나타내는 추가 `seatType` 필드가 필요한 경우.

**해결책:** 직렬화 형식이 누락된 필드에 대한 바이너리 호환성(binary compatibility)을 처리하도록 보장하십시오. 필드는 선택적(optional)이어야 하며 적절한 기본값(default value)을 가져야 합니다.

**도메인 모델:**

Scala:
```scala
sealed abstract class SeatType { def code: String }
object SeatType {
  def fromString(s: String) = s match {
    case Window.code => Window
    case Aisle.code  => Aisle
    case Other.code  => Other
    case _           => Unknown
  }
  case object Window extends SeatType { override val code = "W" }
  case object Aisle extends SeatType { override val code = "A" }
  case object Other extends SeatType { override val code = "O" }
  case object Unknown extends SeatType { override val code = "" }
}

case class SeatReserved(letter: String, row: Int, seatType: SeatType)
```

Java:
```java
static enum SeatType {
  Window("W"),
  Aisle("A"),
  Other("O"),
  Unknown("");
  
  private final String code;
  
  private SeatType(String code) {
    this.code = code;
  }
  
  public static SeatType fromCode(String c) {
    if (Window.code.equals(c)) return Window;
    else if (Aisle.code.equals(c)) return Aisle;
    else if (Other.code.equals(c)) return Other;
    else return Unknown;
  }
}

static class SeatReserved {
  public final String letter;
  public final int row;
  public final SeatType seatType;
  
  public SeatReserved(String letter, int row, SeatType seatType) {
    this.letter = letter;
    this.row = row;
    this.seatType = seatType;
  }
}
```

**Protocol Buffer 정의:**
```
option java_package = "docs.persistence.proto";
option optimize_for = SPEED;

message SeatReserved {
  required string letter   = 1;
  required uint32 row      = 2;
  optional string seatType = 3; // the new field
}
```

**선택적 필드 처리를 포함한 직렬화기:**

Scala:
```scala
class AddedFieldsSerializerWithProtobuf extends SerializerWithStringManifest {
  override def identifier = 67876
  
  final val SeatReservedManifest = classOf[SeatReserved].getName
  
  override def manifest(o: AnyRef): String = o.getClass.getName
  
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case SeatReservedManifest =>
        seatReserved(FlightAppModels.SeatReserved.parseFrom(bytes))
      case _ =>
        throw new NotSerializableException("Unable to handle manifest: " + manifest)
    }
  
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case s: SeatReserved =>
      FlightAppModels.SeatReserved.newBuilder
        .setRow(s.row)
        .setLetter(s.letter)
        .setSeatType(s.seatType.code)
        .build()
        .toByteArray
  }
  
  private def seatReserved(p: FlightAppModels.SeatReserved): SeatReserved =
    SeatReserved(p.getLetter, p.getRow, seatType(p))
  
  private def seatType(p: FlightAppModels.SeatReserved): SeatType =
    if (p.hasSeatType) SeatType.fromString(p.getSeatType) else SeatType.Unknown
}
```

Java:
```java
static class AddedFieldsSerializerWithProtobuf extends SerializerWithStringManifest {
  @Override
  public int identifier() {
    return 67876;
  }
  
  private final String seatReservedManifest = SeatReserved.class.getName();
  
  @Override
  public String manifest(Object o) {
    return o.getClass().getName();
  }
  
  @Override
  public Object fromBinary(byte[] bytes, String manifest) throws NotSerializableException {
    if (seatReservedManifest.equals(manifest)) {
      try {
        return seatReserved(FlightAppModels.SeatReserved.parseFrom(bytes));
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    } else {
      throw new NotSerializableException("Unable to handle manifest: " + manifest);
    }
  }
  
  @Override
  public byte[] toBinary(Object o) {
    if (o instanceof SeatReserved) {
      SeatReserved s = (SeatReserved) o;
      return FlightAppModels.SeatReserved.newBuilder()
          .setRow(s.row)
          .setLetter(s.letter)
          .setSeatType(s.seatType.code)
          .build()
          .toByteArray();
    } else {
      throw new IllegalArgumentException("Unable to handle: " + o);
    }
  }
  
  private SeatReserved seatReserved(FlightAppModels.SeatReserved p) {
    return new SeatReserved(p.getLetter(), p.getRow(), seatType(p));
  }
  
  private SeatType seatType(FlightAppModels.SeatReserved p) {
    if (p.hasSeatType()) return SeatType.fromCode(p.getSeatType());
    else return SeatType.Unknown;
  }
}
```

#### 필드 이름 변경(Renaming Fields)

**상황:** 도메인 개념을 더 잘 반영하기 위해 필드 이름 변경. 예: `code` 필드를 `seatNr` 로 변경.

**해결책 1 - IDL 기반 직렬화기:** IDL 기반 형식(Protocol Buffers 같은)에서는 필드 이름이 바이너리 표현에 절대 저장되지 않기 때문에 필드 이름 변경이 "공짜(free)"입니다. 오직 숫자 형태의 필드 식별자(numeric field identifier)만 중요합니다.

**Protocol Buffer 예제:**
```
// BEFORE:
message SeatReserved {
  required string code = 1;
}

// AFTER:
message SeatReserved {
  required string seatNr = 1; // field renamed, id remains the same
}
```

**해결책 2 - JSON과 함께 수동 버전 관리:** 자동 이름 변경을 지원하지 않는 직렬화 형식의 경우, EventAdapter로 스키마 버전 관리(versioning)를 구현합니다.

**JSON 이름 변경을 포함한 EventAdapter:**

Scala:
```scala
class JsonRenamedFieldAdapter extends EventAdapter {
  val marshaller = new ExampleJsonMarshaller
  
  val V1 = "v1"
  val V2 = "v2"
  
  override def manifest(event: Any): String = V2
  
  override def toJournal(event: Any): JsObject =
    marshaller.toJson(event)
  
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case json: JsObject =>
      EventSeq(marshaller.fromJson(manifest match {
        case V1      => rename(json, "code", "seatNr")
        case V2      => json // pass-through
        case unknown => throw new IllegalArgumentException(s"Unknown manifest: $unknown")
      }))
    case _ =>
      val c = event.getClass
      throw new IllegalArgumentException("Can only work with JSON, was: %s".format(c))
  }
  
  def rename(json: JsObject, from: String, to: String): JsObject = {
    val value = json.fields(from)
    val withoutOld = json.fields - from
    JsObject(withoutOld + (to -> value))
  }
}
```

Java:
```java
static class JsonRenamedFieldAdapter implements EventAdapter {
  private final ExampleJsonMarshaller marshaller = new ExampleJsonMarshaller();
  
  private final String V1 = "v1";
  private final String V2 = "v2";
  
  @Override
  public String manifest(Object event) {
    return V2;
  }
  
  @Override
  public JsObject toJournal(Object event) {
    return marshaller.toJson(event);
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof JsObject) {
      JsObject json = (JsObject) event;
      if (V1.equals(manifest)) json = rename(json, "code", "seatNr");
      return EventSeq.single(json);
    } else {
      throw new IllegalArgumentException(
          "Can only work with JSON, was: " + event.getClass().getName());
    }
  }
  
  private JsObject rename(JsObject json, String from, String to) {
    // use your favorite json library to rename the field
    JsObject renamed = json;
    return renamed;
  }
}
```

#### 이벤트 클래스 제거 및 이벤트 무시(Remove Event Class and Ignore Events)

**상황:** 더 이상 가치를 제공하지 않는 이벤트 타입 제거. 예: `CustomerBlinked` 이벤트는 상태(state)에 기여하지 않으면서 스토리지만 소비함.

**단순한 해결책 - EventAdapter에서 이벤트 폐기(drop):**

이벤트는 빈 EventSeq를 반환하는 EventAdapter에 의해 필터링될 수 있지만, 이는 여전히 역직렬화(deserialization)를 요구합니다.

**개선된 해결책 - 툼스톤(tombstone)으로 역직렬화:**

제거된 이벤트 타입을 인지하는 직렬화기를 사용하여 역직렬화 오버헤드를 방지합니다.

**툼스톤(Tombstone) 정의:**

Scala:
```scala
case object EventDeserializationSkipped
```

Java:
```java
static class EventDeserializationSkipped {
  public static EventDeserializationSkipped instance = new EventDeserializationSkipped();
  
  private EventDeserializationSkipped() {}
}
```

**툼스톤을 지원하는 직렬화기:**

Scala:
```scala
class RemovedEventsAwareSerializer extends SerializerWithStringManifest {
  val utf8 = Charset.forName("UTF-8")
  override def identifier: Int = 8337
  
  val SkipEventManifestsEvents = Set("docs.persistence.CustomerBlinked" // ...
  )
  
  override def manifest(o: AnyRef): String = o.getClass.getName
  
  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case _ => o.toString.getBytes(utf8) // example serialization
  }
  
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case m if SkipEventManifestsEvents.contains(m) =>
        EventDeserializationSkipped
      case _ => new String(bytes, utf8)
    }
}
```

Java:
```java
static class RemovedEventsAwareSerializer extends SerializerWithStringManifest {
  private final Charset utf8 = StandardCharsets.UTF_8;
  private final String customerBlinkedManifest = "blinked";
  
  @Override
  public int identifier() {
    return 8337;
  }
  
  @Override
  public String manifest(Object o) {
    if (o instanceof CustomerBlinked) return customerBlinkedManifest;
    else return o.getClass().getName();
  }
  
  @Override
  public byte[] toBinary(Object o) {
    return o.toString().getBytes(utf8); // example serialization
  }
  
  @Override
  public Object fromBinary(byte[] bytes, String manifest) {
    if (customerBlinkedManifest.equals(manifest)) return EventDeserializationSkipped.instance;
    else return new String(bytes, utf8);
  }
}
```

**툼스톤 필터링을 포함한 EventAdapter:**

Scala:
```scala
class SkippedEventsAwareAdapter extends EventAdapter {
  override def manifest(event: Any) = ""
  override def toJournal(event: Any) = event
  
  override def fromJournal(event: Any, manifest: String) = event match {
    case EventDeserializationSkipped => EventSeq.empty
    case _                           => EventSeq(event)
  }
}
```

Java:
```java
static class SkippedEventsAwareAdapter implements EventAdapter {
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public Object toJournal(Object event) {
    return event;
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event == EventDeserializationSkipped.instance) return EventSeq.empty();
    else return EventSeq.single(event);
  }
}
```

#### 도메인 모델과 데이터 모델 분리(Detach Domain Model from Data Model)

**상황:** 애플리케이션 도메인 모델(domain model)과 영속성 데이터 모델(data model)을 분리하여 독립적인 진화를 허용. 직렬화 도구가 생성된(generated) 클래스를 요구할 때 유용함.

**도메인 모델과 데이터 모델:**

Scala:
```scala
object DomainModel {
  final case class Customer(name: String)
  final case class Seat(code: String) {
    def bookFor(customer: Customer): SeatBooked = SeatBooked(code, customer)
  }
  
  final case class SeatBooked(code: String, customer: Customer)
}

object DataModel {
  final case class SeatBooked(code: String, customerName: String)
}
```

Java:
```java
static class Customer {
  public final String name;
  
  public Customer(String name) {
    this.name = name;
  }
}

static class Seat {
  public final String code;
  
  public Seat(String code) {
    this.code = code;
  }
  
  public SeatBooked bookFor(Customer customer) {
    return new SeatBooked(code, customer);
  }
}

static class SeatBooked {
  public final String code;
  public final Customer customer;
  
  public SeatBooked(String code, Customer customer) {
    this.code = code;
    this.customer = customer;
  }
}

static class SeatBookedData {
  public final String code;
  public final String customerName;
  
  public SeatBookedData(String code, String customerName) {
    this.code = code;
    this.customerName = customerName;
  }
}
```

**모델 변환 EventAdapter:**

Scala:
```scala
class DetachedModelsAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""
  
  override def toJournal(event: Any): Any = event match {
    case DomainModel.SeatBooked(code, customer) =>
      DataModel.SeatBooked(code, customer.name)
  }
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case DataModel.SeatBooked(code, customerName) =>
      EventSeq(DomainModel.SeatBooked(code, DomainModel.Customer(customerName)))
  }
}
```

Java:
```java
class DetachedModelsAdapter implements EventAdapter {
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public Object toJournal(Object event) {
    if (event instanceof SeatBooked) {
      SeatBooked s = (SeatBooked) event;
      return new SeatBookedData(s.code, s.customer.name);
    } else {
      throw new IllegalArgumentException("Unsupported: " + event.getClass());
    }
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof SeatBookedData) {
      SeatBookedData d = (SeatBookedData) event;
      return EventSeq.single(new SeatBooked(d.code, new Customer(d.customerName)));
    } else {
      throw new IllegalArgumentException("Unsupported: " + event.getClass());
    }
  }
}
```

#### 사람이 읽을 수 있는 데이터 모델로 이벤트 저장(Store Events as Human-Readable Data Model)

**상황:** 사람이 읽을 수 있는(human-readable) JSON 형식으로 이벤트 영속화.

**해결책:** 도메인/데이터 모델 분리의 특수한 경우로, 저널 구현체의 지원이 필요합니다. EventAdapter가 JSON으로의 직렬화/역직렬화를 처리합니다.

**JSON 데이터 모델 어댑터:**

Scala:
```scala
class JsonDataModelAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""
  
  val marshaller = new ExampleJsonMarshaller
  
  override def toJournal(event: Any): JsObject =
    marshaller.toJson(event)
  
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case json: JsObject =>
      EventSeq(marshaller.fromJson(json))
    case _ =>
      throw new IllegalArgumentException("Unable to fromJournal a non-JSON object! Was: " + event.getClass)
  }
}
```

Java:
```java
static class JsonDataModelAdapter implements EventAdapter {
  
  private final ExampleJsonMarshaller marshaller = new ExampleJsonMarshaller();
  
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public JsObject toJournal(Object event) {
    return marshaller.toJson(event);
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof JsObject) {
      JsObject json = (JsObject) event;
      return EventSeq.single(marshaller.fromJson(json));
    } else {
      throw new IllegalArgumentException(
          "Unable to fromJournal a non-JSON object! Was: " + event.getClass());
    }
  }
}
```

**노트:** 이 기법은 네이티브 JSON 저장에 대한 저널 플러그인의 지원을 요구합니다.

#### 큰 이벤트를 세분화된 이벤트로 분할(Split Large Event into Fine-Grained Events)

**상황:** 굵은 입자(coarse-grained) 이벤트를 여러 개의 세분화된(fine-grained) 이벤트로 분해. 예: `UserDetailsChanged` 를 `UserNameChanged` 와 `UserAddressChanged` 로 분할.

**이벤트 정의:**

Scala:
```scala
trait Version1
trait Version2

final case class UserDetailsChanged(name: String, address: String) extends Version1

final case class UserNameChanged(name: String) extends Version2
final case class UserAddressChanged(address: String) extends Version2

class UserEventsAdapter extends EventAdapter {
  override def manifest(event: Any): String = ""
  
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case UserDetailsChanged(null, address) => EventSeq(UserAddressChanged(address))
    case UserDetailsChanged(name, null)    => EventSeq(UserNameChanged(name))
    case UserDetailsChanged(name, address) =>
      EventSeq(UserNameChanged(name), UserAddressChanged(address))
    case event: Version2 => EventSeq(event)
  }
  
  override def toJournal(event: Any): Any = event
}
```

Java:
```java
interface Version1 {};

interface Version2 {}

static class UserDetailsChanged implements Version1 {
  public final String name;
  public final String address;
  
  public UserDetailsChanged(String name, String address) {
    this.name = name;
    this.address = address;
  }
}

static class UserNameChanged implements Version2 {
  public final String name;
  
  public UserNameChanged(String name) {
    this.name = name;
  }
}

static class UserAddressChanged implements Version2 {
  public final String address;
  
  public UserAddressChanged(String address) {
    this.address = address;
  }
}

static class UserEventsAdapter implements EventAdapter {
  @Override
  public String manifest(Object event) {
    return "";
  }
  
  @Override
  public EventSeq fromJournal(Object event, String manifest) {
    if (event instanceof UserDetailsChanged) {
      UserDetailsChanged c = (UserDetailsChanged) event;
      if (c.name == null) return EventSeq.single(new UserAddressChanged(c.address));
      else if (c.address == null) return EventSeq.single(new UserNameChanged(c.name));
      else return EventSeq.create(new UserNameChanged(c.name), new UserAddressChanged(c.address));
    } else {
      return EventSeq.single(event);
    }
  }
  
  @Override
  public Object toJournal(Object event) {
    return event;
  }
}
```

어댑터는 `EventSeq` 를 통해 여러 이벤트를 반환하며, 복구 도중 단일 굵은 입자 이벤트를 여러 세분화된 이벤트로 변환합니다.

#### 핵심 요약(Key Takeaways)

1. 스키마 진화를 지원하는 직렬화 형식(Jackson, Protocol Buffers, Avro, Thrift)을 선택하십시오.
2. 직렬화기의 능력을 이해하면 어떤 진화 전략이 실현 가능한지 결정됩니다.
3. EventAdapter는 형식 및 구조 변환을 위한 유연한 메커니즘을 제공합니다.
4. 문자열 매니페스트(string manifest)를 갖는 직렬화기는 고급 진화 패턴을 가능하게 합니다.
5. 도메인 모델과 데이터 모델을 분리하면 독립적인 진화가 가능해집니다.
6. 툼스톤(tombstone) 패턴은 역직렬화 코드를 유지하지 않고도 이벤트 클래스를 제거할 수 있게 합니다.

---

## 4. 영속 상태(Durable State) 영속성

### 4.1 모듈 정보

Akka Persistence 모듈은 영속 상태 스토어(durable state store) 플러그인의 설정을 요구합니다. 의존성:

**SBT:**
```
"com.typesafe.akka" %% "akka-persistence-typed" % "2.10.19"
"com.typesafe.akka" %% "akka-persistence-testkit" % "2.10.19" % Test
```

**Maven/Gradle:** 적절한 Scala 바이너리 버전(2.13.17 또는 3.3.7)과 함께 `akka-bom` 아티팩트를 통해 의존성을 관리합니다.

**프로젝트 세부 정보:** Lightbend 지원을 받는 Supported 상태이며, 2.6.0 (2019-11-06)부터 사용 가능합니다.

---

### 4.2 소개

영속 상태(Durable State)는 이벤트 소싱에 대한 대안으로, 각 명령(command) 이후 전체 엔티티 상태를 저장하며 다음 모델을 따릅니다: `(State, Command) => State`. 이 접근은 단순한 사용 사례에 대한 개념적 복잡성을 줄여주며, CRUD 작업과 유사합니다. 데이터베이스에는 오직 최신 상태(latest state)만 영속화되며, 이벤트 소싱과 달리 과거의 변경 이력은 보존되지 않습니다.

> "Akka Persistence는 그 상태를 읽어와 메모리에 저장한다. 명령의 처리가 완료된 후, 새 상태가 데이터베이스에 저장된다."

---

### 4.3 핵심 구성 요소

#### DurableStateBehavior 구조

영속 액터(persistent actor)는 세 가지 필수 요소를 요구합니다:

1. **persistenceId:** 백엔드 스토어에서 영속 액터를 식별하는 안정적인 고유 식별자
2. **emptyState:** 엔티티가 처음 생성될 때의 초기 상태(예: Counter는 0에서 시작)
3. **commandHandler:** 현재 상태와 들어오는 명령을 이펙트(effect)로 매핑

#### PersistenceId

`PersistenceId` 는 영속 액터를 고유하게 식별합니다. 일반적으로 클러스터 샤딩(Cluster Sharding)과 결합되어, 클러스터 전반에서 `PersistenceId` 당 오직 하나의 활성 엔티티만 존재하도록 보장합니다. 생성은 `entityTypeHint` 와 `entityId` 를 기본 구분자 `|` 로 결합하는 `PersistenceId.apply` 또는 `PersistenceId.of` 팩토리 메서드를 사용합니다. 커스텀 구분자나 고유 식별자는 `PersistenceId.ofUniqueId` 를 사용합니다.

#### 명령 핸들러(Command Handler)

명령 핸들러는 현재 `State` 와 들어오는 `Command` 를 받아 `Effect` 지시(directive)를 반환합니다. 이펙트는 어떤 상태를(있다면) 영속화할지를 나타냅니다. 핸들러는 `thenRun` 을 사용하여 부수 효과(side effect)를 연쇄(chain)할 수 있습니다.

---

### 4.4 이펙트(Effect)와 부수 효과(Side Effect)

사용 가능한 이펙트는 다음과 같습니다:

- **persist:** 최신 상태를 영속화합니다. 리비전(revision)이 1씩 증가하면 새 레코드를 삽입하거나 기존 레코드를 갱신합니다.
- **delete:** 상태를 빈 상태(empty state)로 설정하여 삭제하고, 리비전을 증가시킵니다.
- **none:** 상태를 영속화하지 않음(읽기 전용 명령)
- **unhandled:** 현재 상태에서 지원되지 않는 명령
- **stop:** 액터를 정지시킴
- **stash:** 현재 명령을 스태시(stash)함
- **unstashAll:** 스태시된 명령들을 처리함
- **reply:** 지정된 `ActorRef` 로 응답을 보냄

명령당 오직 하나의 기본 이펙트(primary effect)만 허용됩니다. 부수 효과는 `thenRun` 을 통해 연쇄되며, 성공적인 persist 이후 최대 1회(at-most-once) 기준으로 순차적으로 실행됩니다.

추가적인 persist 이후 동작:
- `thenStop`: 액터를 정지시킴
- `thenUnstashAll`: 스태시된 명령들을 처리함
- `thenReply`: 응답 메시지를 보냄

**중요한 보장:** "모든 부수 효과는 최대 1회(at-most-once) 기준으로 실행되며, persist가 실패하면 실행되지 않는다."

---

### 4.5 완전한 예제: Counter

**State:**
```scala
final case class State(value: Int) extends CborSerializable
```

**Commands:**
```scala
sealed trait Command[ReplyMessage] extends CborSerializable
final case object Increment extends Command[Nothing]
final case class IncrementBy(value: Int) extends Command[Nothing]
final case class GetValue(replyTo: ActorRef[State]) extends Command[State]
final case object Delete extends Command[Nothing]
```

**명령 핸들러 (Scala):**
```scala
val commandHandler: (State, Command[_]) => Effect[State] = (state, command) =>
  command match {
    case Increment         => Effect.persist(state.copy(value = state.value + 1))
    case IncrementBy(by)   => Effect.persist(state.copy(value = state.value + by))
    case GetValue(replyTo) => Effect.reply(replyTo)(state)
    case Delete            => Effect.delete[State]()
  }
```

**명령 핸들러 (Java):**
```java
@Override
public CommandHandler<Command<?>, State> commandHandler() {
  return newCommandHandlerBuilder()
      .forAnyState()
      .onCommand(Increment.class, (state, command) -> 
          Effect().persist(new State(state.get() + 1)))
      .onCommand(IncrementBy.class, (state, command) -> 
          Effect().persist(new State(state.get() + command.value)))
      .onCommand(GetValue.class, (state, command) -> 
          Effect().reply(command.replyTo, state.get()))
      .onCommand(Delete.class, (state, command) -> Effect().delete())
      .build();
}
```

**Behavior 생성 (Scala):**
```scala
def counter(id: String): DurableStateBehavior[Command[_], State] = {
  DurableStateBehavior.apply[Command[_], State](
    persistenceId = PersistenceId.ofUniqueId(id),
    emptyState = State(0),
    commandHandler = commandHandler)
}
```

---

### 4.6 강제된 응답(Enforced Replies)

확인 응답(confirmation response)을 요구하는 명령의 경우, `DurableStateBehavior.withEnforcedReplies` 를 사용하면 컴파일 수준(compilation-level)에서 응답 요구 사항을 강제합니다.

**부수 효과를 포함한 예제:**
```scala
sealed trait Command[ReplyMessage] extends CborSerializable
final case class IncrementWithConfirmation(replyTo: ActorRef[Done]) 
    extends Command[Done]
final case class GetValue(replyTo: ActorRef[State]) extends Command[State]

def counter(persistenceId: PersistenceId): DurableStateBehavior[Command[_], State] = {
  DurableStateBehavior.withEnforcedReplies[Command[_], State](
    persistenceId,
    emptyState = State(0),
    commandHandler = (state, command) =>
      command match {
        case IncrementWithConfirmation(replyTo) =>
          Effect.persist(state.copy(value = state.value + 1))
              .thenReply(replyTo)(_ => Done)
        case GetValue(replyTo) =>
          Effect.reply(replyTo)(state)
      })
}
```

---

### 4.7 상태 기반 동작 변경

`forStateType` 을 사용하는 빌더 패턴(builder pattern)을 통해 현재 상태에 따라 서로 다른 명령 핸들러가 적용됩니다.

**State 클래스 (Scala):**
```scala
sealed trait State
case object BlankState extends State

final case class DraftState(content: PostContent) extends State {
  def withBody(newBody: String): DraftState = 
    copy(content = content.copy(body = newBody))
  def postId: String = content.postId
}

final case class PublishedState(content: PostContent) extends State {
  def postId: String = content.postId
}
```

**Commands:**
```scala
sealed trait Command
final case class AddPost(content: PostContent, 
    replyTo: ActorRef[StatusReply[AddPostDone]]) extends Command
final case class AddPostDone(postId: String)
final case class GetPost(replyTo: ActorRef[PostContent]) extends Command
final case class ChangeBody(newBody: String, replyTo: ActorRef[Done]) extends Command
final case class Publish(replyTo: ActorRef[Done]) extends Command
final case class PostContent(postId: String, title: String, body: String)
```

**상태 디스패치를 포함한 명령 핸들러 (Scala):**
```scala
private val commandHandler: (State, Command) => Effect[State] = { (state, command) =>
  state match {
    case BlankState =>
      command match {
        case cmd: AddPost => addPost(cmd)
        case _            => Effect.unhandled
      }
    case draftState: DraftState =>
      command match {
        case cmd: ChangeBody  => changeBody(draftState, cmd)
        case Publish(replyTo) => publish(draftState, replyTo)
        case GetPost(replyTo) => getPost(draftState, replyTo)
        case AddPost(_, replyTo) =>
          Effect.unhandled[State]
              .thenRun(_ => replyTo ! 
                  StatusReply.Error("Cannot add post while in draft"))
      }
    case publishedState: PublishedState =>
      command match {
        case GetPost(replyTo) => getPost(publishedState, replyTo)
        case AddPost(_, replyTo) =>
          Effect.unhandled[State]
              .thenRun(_ => replyTo ! 
                  StatusReply.Error("Cannot add post, already published"))
        case _ => Effect.unhandled
      }
  }
}

private def addPost(cmd: AddPost): Effect[State] = {
  Effect.persist(DraftState(cmd.content)).thenRun { _ =>
    cmd.replyTo ! StatusReply.Success(AddPostDone(cmd.content.postId))
  }
}

private def changeBody(state: DraftState, cmd: ChangeBody): Effect[State] = {
  Effect.persist(state.withBody(cmd.newBody)).thenRun { _ =>
    cmd.replyTo ! Done
  }
}

private def publish(state: DraftState, replyTo: ActorRef[Done]): Effect[State] = {
  Effect.persist(PublishedState(state.content)).thenRun { _ =>
    println(s"Blog post ${state.postId} was published")
    replyTo ! Done
  }
}

private def getPost(state: DraftState, replyTo: ActorRef[PostContent]): Effect[State] = {
  replyTo ! state.content
  Effect.none
}

private def getPost(state: PublishedState, replyTo: ActorRef[PostContent]): Effect[State] = {
  replyTo ! state.content
  Effect.none
}
```

**Java 구현:**
```java
@Override
public CommandHandler<Command, State> commandHandler() {
  CommandHandlerBuilder<Command, State> builder = newCommandHandlerBuilder();
  builder.forStateType(BlankState.class).onCommand(AddPost.class, this::onAddPost);
  builder.forStateType(DraftState.class)
      .onCommand(ChangeBody.class, this::onChangeBody)
      .onCommand(Publish.class, this::onPublish)
      .onCommand(GetPost.class, this::onGetPost);
  builder.forStateType(PublishedState.class)
      .onCommand(ChangeBody.class, this::onChangeBody)
      .onCommand(GetPost.class, this::onGetPost);
  builder.forAnyState().onCommand(AddPost.class, (state, cmd) -> Effect().unhandled());
  return builder.build();
}

private Effect<State> onAddPost(AddPost cmd) {
  return Effect()
      .persist(new DraftState(cmd.content))
      .thenRun(() -> cmd.replyTo.tell(new AddPostDone(cmd.content.postId)));
}

private Effect<State> onChangeBody(DraftState state, ChangeBody cmd) {
  return Effect()
      .persist(state.withBody(cmd.newBody))
      .thenRun(() -> cmd.replyTo.tell(Done.getInstance()));
}

private Effect<State> onPublish(DraftState state, Publish cmd) {
  return Effect()
      .persist(new PublishedState(state.content))
      .thenRun(() -> {
        System.out.println("Blog post published: " + state.postId());
        cmd.replyTo.tell(Done.getInstance());
      });
}

private Effect<State> onGetPost(DraftState state, GetPost cmd) {
  cmd.replyTo.tell(state.content);
  return Effect().none();
}

private Effect<State> onGetPost(PublishedState state, GetPost cmd) {
  cmd.replyTo.tell(state.content);
  return Effect().none();
}
```

---

### 4.8 응답(Replies)

요청-응답(request-response) 상호작용 패턴은 영속 액터의 표준입니다. 명령에 `ActorRef[ReplyMessageType]` 를 포함시킵니다. 성공하거나 검증(validation)에 실패할 수 있는 응답에는 `StatusReply[ReplyType]` 를 사용합니다. 미리 정의된 `StatusReply.Ack` 는 값을 반환하지 않고 확인(acknowledge)합니다.

검증 이후 또는 persist 이후에 `thenRun` 부수 효과를 사용하여 응답을 보냅니다:

```scala
Effect.persist(DraftState(cmd.content)).thenRun { _ =>
  cmd.replyTo ! StatusReply.Success(AddPostDone(cmd.content.postId))
}
```

`withEnforcedReplies` 를 사용하면, 모든 명령 핸들러가 `Effect.reply`, `Effect.noReply`, `Effect.thenReply`, 또는 `Effect.thenNoReply` 를 통해 생성된 `ReplyEffect` 를 반환하도록 컴파일 단계에서 요구됩니다.

---

### 4.9 직렬화(Serialization)

Durable State는 액터 메시지와 동일한 직렬화 메커니즘을 사용합니다. 명령과 상태에 대해 직렬화를 활성화하십시오. Jackson 기반 직렬화가 범용적인 해결책으로 권장됩니다.

---

### 4.10 태깅(Tagging)

태그(tag)를 사용하면 `DurableStateStoreQuery` 인터페이스를 통해 영속 상태 스토어 내의 상태 부분집합(subset)을 별도의 스트림 소비를 위해 식별할 수 있습니다.

**Scala:**
```scala
DurableStateBehavior[Command[_], State](
   persistenceId = PersistenceId.ofUniqueId("abc"),
   emptyState = State(0),
   commandHandler = (state, cmd) => throw new NotImplementedError("TODO"))
   .withTag("tag1")
```

**Java:**
```java
public class MyPersistentBehavior 
    extends DurableStateBehavior<Command, State> {
  @Override
  public String tag() {
    return "tag1";
  }
}
```

---

### 4.11 ActorContext 접근

자식 액터(child actor)를 스폰(spawn)하거나 로깅(logging)을 위해 `ActorContext` 에 접근하려면, `Behaviors.setup` 으로 생성을 감쌉니다.

**Scala:**
```scala
def apply(): Behavior[String] =
  Behaviors.setup { context =>
    DurableStateBehavior[String, State](
      persistenceId = PersistenceId.ofUniqueId("myPersistenceId"),
      emptyState = State(0),
      commandHandler = CommandHandler.command { cmd =>
        context.log.info("Got command {}", cmd)
        Effect.none
      })
  }
```

**Java:**
```java
public class MyPersistentBehavior 
    extends DurableStateBehavior<Command, State> {
  public static Behavior<Command> create(PersistenceId persistenceId) {
    return Behaviors.setup(ctx -> new MyPersistentBehavior(persistenceId, ctx));
  }
  
  private final ActorContext<Command> context;
  private final ActorRef<Command> self;
  
  public MyPersistentBehavior(PersistenceId persistenceId, 
      ActorContext<Command> ctx) {
    super(persistenceId);
    this.context = ctx;
    this.self = ctx.getSelf();
  }
}
```

**DurableStateBehavior 감싸기(Wrapping):**

`ActorContext` 기능에 접근하기 위해 `DurableStateBehavior` 를 `Behaviors.setup` 같은 다른 동작(behavior)으로 감쌀 수 있습니다.

```scala
Behaviors.setup[Command[_]] { context =>
  DurableStateBehavior[Command[_], State](
    persistenceId = PersistenceId.ofUniqueId("abc"),
    emptyState = State(0),
    commandHandler = CommandHandler.command { cmd =>
      context.log.info("Got command {}", cmd)
      Effect.none
    })
}
```

---

### 4.12 클러스터 샤딩(Cluster Sharding)

"클러스터 샤딩(Cluster Sharding)은 영속 액터를 클러스터 전반에 분산시키고 id로 주소를 지정(address)하는 데 매우 적합하다." 이는 `persistenceId` 당 오직 하나의 활성 엔티티 인스턴스만 존재하도록 보장하며, 사용 가능한 노드에서 액터를 신속하게 재시작함으로써 노드 장애 전반에 걸친 복원력을 개선합니다.

---

### 4.13 동작 변경 및 가변 상태 고려 사항

#### 동작 변경(Behavior Changes)

영속 액터는 명령 핸들러에서 새로운 동작(behavior)을 반환할 수 없습니다. 동작은 복구 과정에서 신중하게 재구성되어야 하는 액터 상태를 이루기 때문입니다. 대신, 현재 상태에 따라 서로 다른 함수가 명령을 처리하는 상태 기반 명령 처리(state-based command handling)를 구현하십시오. 이 방식은 유한 상태 기계(finite state machine) 구현에 유용합니다.

#### 가변 상태 고려 사항(Mutable State Considerations)

가변 상태(mutable state)를 사용하는 경우, 실패 후 재시작 시 상태 인스턴스가 새로 생성되도록 `emptyStateFactory: () => State` 매개변수를 받는 `DurableStateBehavior.withMutableState` 또는 관련 팩토리를 사용하십시오.

---

## 5. 영속 상태 스타일 가이드(Durable State Style)

이 가이드는 타입드 Akka 애플리케이션에서 명령 처리와 상태 관리를 구조화하는 방법과, Durable State 엔티티를 위한 스타일 접근법을 다룹니다.

### 상태 안의 명령 핸들러(Command Handlers in the State)

이 가이드는 명령 처리 로직을 상태 객체(state object) 자체에 위임하는 아키텍처 패턴을 제시합니다. 설명에 따르면, "명령 핸들러는 `Account`(상태) 안의 `applyCommand` 에 위임하며, 이는 구체적인 `EmptyAccount`, `OpenedAccount`, `ClosedAccount` 에 구현되어 있다."

#### Scala 구현

Scala 예제는 세 가지 상태 구현을 갖는 봉인된 트레이트 계층(sealed trait hierarchy)을 보여줍니다:

- **EmptyAccount:** 초기 생성을 처리하며, 다른 작업 이전에 `CreateAccount` 를 요구함
- **OpenedAccount:** 입금(deposit), 출금(withdrawal), 잔액 조회, 계좌 폐쇄를 관리함
- **ClosedAccount:** 적절한 오류 응답과 함께 모든 작업을 거부함

각 상태 타입은 `applyCommand(cmd: Command): ReplyEffect` 를 구현하여, 현재 상태에 기반한 다형적(polymorphic) 명령 처리를 가능하게 합니다.

#### Java 구현

Java 접근법은 `null` 이 빈 초기 상태를 나타내는 널 가능 상태(nullable state) 패턴을 사용합니다. `CommandHandlerWithReplyBuilder` 는 `forNullState()` 와 `forStateType()` 메서드를 통해 상태별 명령 처리를 허용하며, 컴파일 타임 타입 안정성(compile-time type safety)을 제공합니다.

### 선택적 초기 상태(Optional Initial State)

전용 빈 상태 클래스를 유지하는 대신, 개발자는 `null` 또는 `Option`/`Optional` 타입을 사용할 수 있습니다. 가이드는 "빈 초기 상태를 위해 별도의 상태 클래스를 사용하는 것이 바람직하지 않고, 마치 아직 상태가 없는 것처럼 행동하는 것이 바람직하다"고 언급합니다.

#### Option을 사용하는 Scala

`None` 을 초기 상태로 하는 `Option[Account]` 를 사용하려면, `state.applyCommand(cmd)` 를 통해 상태별 메서드에 위임하기 전에 외부 핸들러 계층에서 패턴 매칭(pattern matching)이 필요합니다.

#### Null을 사용하는 Java

Java 구현은 `null` 을 반환하는 `emptyState()` 를 오버라이드한 다음, 명령 핸들러 빌더에서 `forNullState()` 를 사용하여 생성을 다른 작업과 분리하여 처리할 수 있습니다.

### Java 21 기능 활용

Java 21+ 프로젝트는 봉인된 인터페이스(sealed interface) 및 레코드(record)와 결합된 `DurableStateOnCommandBehavior` 기반 클래스를 활용할 수 있습니다. 이는 컴파일 타임 완전성 검사(exhaustiveness checking)와 함께 switch 패턴 매칭을 가능하게 합니다.

#### Java 21 예제

`BlogPostEntityDurableState` 예제는 다음을 보여줍니다:

- **봉인된 Command 인터페이스:** 모든 명령 타입이 알려져 있음을 보장
- **레코드 클래스:** 데이터를 담는 명령 및 상태 정의를 단순화
- **패턴 매칭:** 중첩된 switch 가 명확한 상태-명령 라우팅을 제공

이 패턴은 컴파일러 검증을 통해 핸들러 메서드가 모든 명령과 이벤트 유형을 빠짐없이 처리했음을 보장합니다.

### 핵심 아키텍처 노트

모든 접근법은 상태 영속화 및 응답 의미론(reply semantics)을 기술하기 위해 `Effect` 를 사용합니다:
- `Effect.persist()`: 영속적인 갱신에 사용
- `.thenReply()`: 응답을 보내기 위해
- `.thenNoReply()`: 발사 후 망각(fire-and-forget) 작업을 위해
- `Effect.unhandled()`: 유효하지 않은 명령-상태 조합을 위해

함수형(functional) 위임 패턴과 객체지향(object-oriented) 위임 패턴 중 무엇을 선택할지는 애플리케이션 복잡성과 팀 선호도에 따라 달라지며, 각각 코드 구조화와 테스트 용이성(testability) 측면에서 고유한 장점을 지닙니다.

---

## 6. 영속 상태 영속성 쿼리(Durable State Persistence Query)

### 6.1 개요

이 섹션은 영속 상태 동작(Durable State Behaviors)에 대한 쿼리 인터페이스를 비동기 스트림(asynchronous stream)으로 제공하는 Durable State용 Akka Persistence Query를 다룹니다. CQRS 아키텍처 패턴에서 읽기 측(read side)을 구현하는 데 활용됩니다.

---

### 6.2 의존성 설정

**sbt:**
```
val AkkaVersion = "2.10.19"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion
```

**Maven:** BOM 임포트로 `akka-bom_${scala.binary.version}` 버전 2.10.19 와 `akka-persistence-query_${scala.binary.version}` 의존성을 요구합니다.

**Gradle:**
```
implementation platform("com.typesafe.akka:akka-bom_${versions.ScalaBinary}:2.10.19")
implementation "com.typesafe.akka:akka-persistence-query_${versions.ScalaBinary}"
```

**노트:** 의존성은 https://account.akka.io/token 에 명시된 보안 토큰화 URL을 통해 접근해야 합니다.

---

### 6.3 소개

이 쿼리 모듈은 비동기 스트림을 통해 영속 상태 동작(Durable State Behaviors)에 대한 인터페이스를 제공하며, CQRS 패턴에서 읽기 측 구현을 지원합니다. Akka Persistence를 통한 쓰기와 쿼리를 분리합니다.

**대안:** R2DBC 플러그인 사용자는 쿼리 표현(query representation)을 읽기 측 대신 쓰기 측에서 직접 저장할 수도 있습니다.

---

### 6.4 Akka Projections와 함께 쿼리 사용하기

`DurableStateStoreQuery` 인터페이스는 Akka Projections에서 태그 기반(tag-based) 검색을 가능하게 합니다. 동작하려면 태깅된 객체(tagged object)가 필요합니다.

#### Scala 예제
```scala
import akka.persistence.state.DurableStateStoreRegistry
import akka.persistence.query.scaladsl.DurableStateStoreQuery
import akka.persistence.query.DurableStateChange
import akka.persistence.query.UpdatedDurableState

val durableStateStoreQuery = 
  DurableStateStoreRegistry(system).durableStateStoreFor[DurableStateStoreQuery[Record]](pluginId)
val source: Source[DurableStateChange[Record], NotUsed] = 
  durableStateStoreQuery.changes("tag", offset)
source.map {
  case UpdatedDurableState(persistenceId, revision, value, offset, timestamp) => Some(value)
  case _: DeletedDurableState[_] => None
}
```

#### Java 예제
```java
import akka.persistence.state.DurableStateStoreRegistry;
import akka.persistence.query.javadsl.DurableStateStoreQuery;
import akka.persistence.query.DurableStateChange;
import akka.persistence.query.UpdatedDurableState;

DurableStateStoreQuery<Record> durableStateStoreQuery =
    DurableStateStoreRegistry.get(system)
        .getDurableStateStoreFor(DurableStateStoreQuery.class, pluginId);
Source<DurableStateChange<Record>, NotUsed> source =
    durableStateStoreQuery.changes("tag", offset);
source.map(chg -> {
  if (chg instanceof UpdatedDurableState) {
    UpdatedDurableState<Record> upd = (UpdatedDurableState<Record>) chg;
    return upd.value();
  } else {
    throw new IllegalArgumentException("Unexpected DurableStateChange " + chg.getClass());
  }
});
```

---

### 6.5 DurableStateChange 타입

쿼리로부터 반환되는 요소는 `UpdatedDurableState` 또는 `DeletedDurableState` 인스턴스 중 하나입니다.

- **UpdatedDurableState** 는 `persistenceId`, `revision`, `value`, `offset`, `timestamp` 필드를 포함합니다.
- **DeletedDurableState** 는 상태가 삭제되었음을 나타냅니다.

`changes("tag", offset)` 쿼리는 라이브 스트림(live stream)으로, 변경이 발생할 때마다 새 요소를 방출합니다. `currentChanges` 변형은 현재 존재하는 변경만 반환하고 스트림을 완료합니다. 처리된 마지막 변경의 `offset` 을 저장하고 다음 시작 시 재사용함으로써 재개 가능한 프로젝션(resumable projection)을 구현할 수 있습니다.

---

## 7. 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Persistence Query](https://doc.akka.io/libraries/akka-core/current/persistence-query.html)
- [Persistence Plugins](https://doc.akka.io/libraries/akka-core/current/persistence-plugins.html)
- [Persistence - Schema Evolution](https://doc.akka.io/libraries/akka-core/current/persistence-schema-evolution.html)
- [Durable State](https://doc.akka.io/libraries/akka-core/current/typed/durable-state/persistence.html)
- [Durable State - Style](https://doc.akka.io/libraries/akka-core/current/typed/durable-state/persistence-style.html)
- [Durable State - Persistence Query](https://doc.akka.io/libraries/akka-core/current/durable-state/persistence-query.html)
- [Akka Projections](https://doc.akka.io/libraries/akka-projection/current/)
- [Community Plugins](https://akka.io/community/#plugins-to-akka-persistence-query)
