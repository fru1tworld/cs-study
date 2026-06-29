# Cassandra 데이터 모델링

> 원본: https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/

---

## 목차

1. [소개](#소개)
2. [개념적 데이터 모델링](#개념적-데이터-모델링)
3. [RDBMS 설계와의 차이](#rdbms-설계와의-차이)
4. [애플리케이션 쿼리 정의](#애플리케이션-쿼리-정의)
5. [논리적 데이터 모델링](#논리적-데이터-모델링)
6. [물리적 데이터 모델링](#물리적-데이터-모델링)
7. [데이터 모델 평가 및 개선](#데이터-모델-평가-및-개선)
8. [데이터베이스 스키마 정의](#데이터베이스-스키마-정의)
9. [데이터 모델링 도구](#데이터-모델링-도구)
10. [참고 자료](#참고-자료)

---

## 소개

Apache Cassandra는 관계형 데이터베이스(relational database)와는 근본적으로 다른 **쿼리 주도(query-driven) 데이터 모델링 접근법**을 사용합니다. 공식 문서는 다음과 같이 설명합니다. "데이터 모델링은 쿼리 주도적입니다. 데이터 접근 패턴(data access pattern)과 애플리케이션 쿼리가 데이터의 구조와 조직 방식을 결정합니다."

관계형 데이터베이스처럼 도메인 엔티티(entity)를 먼저 모델링한 뒤 쿼리를 작성하는 방식과 달리, 애플리케이션이 수행할 쿼리를 먼저 파악하고 그 쿼리를 지원하는 테이블을 설계합니다. 이를 **쿼리 우선(query-first)** 모델링이라고 합니다.

### 관계형 모델과의 핵심 차이

전통적인 데이터베이스는 외래 키(foreign key)로 관련 테이블에 데이터를 정규화(normalize)하는 반면, Cassandra는 여러 테이블에 데이터를 중복 저장하는 **비정규화(denormalization)** 를 사용합니다. 이는 저장 효율보다 읽기 성능(read performance)을 우선하는 설계 선택입니다. Cassandra는 "관계형 데이터베이스를 위한 관계형 데이터 모델링을 지원하지 않습니다."

### 파티션 전략(Partition Strategy)

데이터 분산(data distribution)은 기본 키(primary key) 구성 요소에서 파생된 **파티션 키(partition key)** 에 의존합니다. 문서에 따르면 "파티션 키는 기본 키의 첫 번째 필드에서 생성"되며, 클러스터 노드(cluster node) 전반에 걸친 일관된 해싱(consistent hashing)에 사용됩니다.

Cassandra의 기본 키는 다음과 같은 형태를 가질 수 있습니다.

- **단순 키(Simple)**: 단일 필드 (예: `PRIMARY KEY (id)`)
- **복합 키(Composite/Compound)**: 여러 필드로 구성되며, 첫 번째 필드가 파티션 키를 생성하고 나머지 필드는 파티션 내에서 정렬을 담당하는 **클러스터링 키(clustering key)** 역할을 합니다.

### 설계 시 고려 사항

효과적인 데이터 모델은 다음을 유지해야 합니다.

- 파티션 크기를 값(value) 기준 100,000개 미만, 디스크 기준 100MB 미만으로 유지
- 데이터 중복(redundancy)의 최소화
- 경량 트랜잭션(Lightweight Transactions, LWT)의 제한적 사용
- 여러 테이블이 아닌 단일 테이블에 대한 쿼리 수행

문서는 "구체화 뷰(materialized view, MV)는 실험적(experimental)" 기능이며, 대체 기본 키(alternate primary key)를 통해 단일 테이블에 대한 여러 쿼리를 지원할 수 있다고 언급합니다.

---

## 개념적 데이터 모델링

### 개요

개념적 데이터 모델(conceptual data model)은 도메인(domain)을 추상적으로 표현한 것으로, **기술 독립적(technology independent)** 이며 특정 데이터베이스 시스템에 종속되지 않습니다. 공식 문서는 Cassandra의 분산 해시테이블(distributed hashtable) 구조로 매핑할 관계형 도메인 모델을 제시하면서 개념적 데이터 모델링을 소개합니다.

### 호텔 예약 예제 도메인

문서는 주요 예제로 **호텔 예약(hotel reservation) 시스템** 을 사용하며, 이는 다음을 포함합니다.

- 호텔(hotel)과 투숙(guest stay)
- 객실 요금(rate) 및 가용성(availability)을 포함한 객실 집합
- 게스트(guest)를 위한 예약 기록(reservation record)
- 관심 지점(point of interest, POI) — 공원, 박물관, 쇼핑몰, 기념물 등
- 호텔 및 명소의 지오로케이션(geolocation) 데이터

문서는 "모두에게 익숙한 도메인을 선택"함으로써 도메인 복잡성보다 Cassandra의 동작 메커니즘에 집중할 수 있다고 강조합니다. 호텔 예약 예제는 데이터 구조와 설계 패턴을 충분히 보여주면서도 이해하기 쉽다는 점에서 균형 잡힌 선택입니다.

### 엔티티-관계 모델(Entity-Relationship Model)

개념적 도메인은 엔티티-관계(entity-relationship, ER) 모델을 사용하여 표현되며, 다음 요소들로 구성됩니다.

- **엔티티(Entity)**: 사각형(rectangle)으로 표현
- **속성(Attribute)**: 타원(oval)으로 표시
- **고유 식별자(Unique identifier)**: 밑줄이 그어진 속성
- **관계(Relationship)**: 마름모(diamond)로 표현
- **다중성(Multiplicity)**: 관계와 엔티티를 연결하는 커넥터로 표시

---

## RDBMS 설계와의 차이

관계형 데이터베이스(RDBMS)에서 Cassandra로 전환할 때 고려해야 할 근본적인 설계 차이는 다음과 같습니다.

### 조인 없음(No Joins)

Cassandra는 조인(join)을 지원하지 않습니다. 클라이언트 측에서 조인을 수행하는 방법(드물게 사용해야 함)도 있지만, 권장되는 접근법은 "조인 결과를 표현하는 비정규화된 두 번째 테이블을 생성"하는 것입니다.

### 참조 무결성 없음(No Referential Integrity)

외래 키 제약(foreign key constraint)을 가진 관계형 데이터베이스와 달리, Cassandra는 참조 무결성(referential integrity)을 강제하지 않습니다. 연관된 ID를 저장할 수는 있지만, 연쇄 삭제(cascading delete)와 같은 작업은 제공되지 않습니다.

### 비정규화가 표준(Denormalization is Standard)

관계형 설계에서는 일반적으로 지양되는 비정규화가 Cassandra에서는 "완전히 정상적(perfectly normal)"입니다. 이는 전통적인 데이터베이스의 정규화 원칙과 뚜렷하게 대조됩니다. 문서는 Cassandra가 3.0 버전부터 비정규화된 데이터를 자동으로 관리하기 위해 구체화 뷰(materialized view)를 도입했다고 언급합니다.

### 쿼리 우선 설계(Query-First Design)

도메인 엔티티를 먼저 모델링하는 대신, Cassandra는 쿼리를 중심으로 설계할 것을 요구합니다. 이 접근법은 "애플리케이션이 사용할 가장 일반적인 쿼리 경로(query path)를 식별한 다음, 이를 지원하는 데 필요한 테이블을 생성"하는 것을 포함합니다.

### 저장소 최적화의 중요성(Storage Optimization Matters)

"Cassandra 테이블은 각각 디스크의 별도 파일에 저장"되므로, 물리적 저장에 대한 신중한 고려가 필요합니다. 쿼리당 검색되는 파티션 수를 최소화하는 것이 중요한 설계 목표입니다.

### 고정된 정렬 전략(Fixed Sorting Strategy)

`ORDER BY`로 유연한 정렬이 가능한 RDBMS와 달리, Cassandra의 정렬 순서는 클러스터링 컬럼(clustering column)을 선택함으로써 테이블 생성 시점에 고정됩니다.

---

## 애플리케이션 쿼리 정의

쿼리 우선 데이터 모델링은 애플리케이션이 수행해야 하는 핵심 쿼리를 식별하는 것에서 시작합니다. 문서는 호텔 애플리케이션의 사용자 인터페이스(UI) 디자인에서 출발하여 필수 쿼리를 도출합니다.

각 워크플로(workflow) 단계는 하나의 작업을 수행하며 그 결과가 후속 단계를 "잠금 해제(unlock)"합니다. 워크플로 다이어그램은 쿼리들이 애플리케이션 작업을 어떻게 연결하는지를 보여주며, 한 쿼리에서 얻은 호텔 키(hotel key) 같은 데이터가 이후의 상세 조회를 가능하게 합니다.

### 쇼핑 관련 쿼리(Shopping Queries, Q1-Q5)

- **Q1.** 주어진 관심 지점(POI) 근처의 호텔을 찾는다. (*Find hotels near a given point of interest.*)
- **Q2.** 주어진 호텔의 이름과 위치 등 정보를 찾는다. (*Find information about a given hotel, such as its name and location.*)
- **Q3.** 주어진 호텔 근처의 관심 지점을 찾는다. (*Find points of interest near a given hotel.*)
- **Q4.** 주어진 날짜 범위에서 이용 가능한 객실을 찾는다. (*Find an available room in a given date range.*)
- **Q5.** 객실의 요금과 편의 시설(amenities)을 찾는다. (*Find the rate and amenities for a room.*)

### 예약 및 게스트 관련 쿼리(Reservation and Guest Queries, Q6-Q9)

- **Q6.** 확인 번호(confirmation number)로 예약을 조회한다. (*Lookup a reservation by confirmation number.*)
- **Q7.** 호텔, 날짜, 게스트 이름으로 예약을 조회한다. (*Lookup a reservation by hotel, date, and guest name.*)
- **Q8.** 게스트 이름으로 모든 예약을 조회한다. (*Lookup all reservations by guest name.*)
- **Q9.** 게스트 상세 정보를 조회한다. (*View guest details.*)

---

## 논리적 데이터 모델링

### 핵심 개념

논리적 데이터 모델링(logical data modeling) 단계에서는 애플리케이션 쿼리를 토대로 테이블을 설계합니다. 문서에 따르면 "개념적 모델의 엔티티와 관계를 담으면서, 각 쿼리마다 하나의 테이블을 포함하는 논리적 모델을 생성"해야 합니다.

### 주요 설계 원칙

**테이블 명명(Table Naming)**: 테이블은 주요 엔티티 이름 뒤에 쿼리 속성을 붙여 명명합니다. 예를 들어 `hotels_by_poi`라는 패턴은 관심 지점(POI)으로 필터링하여 호텔을 찾도록 설계된 테이블임을 나타냅니다.

**기본 키 설계(Primary Key Design)**: 기본 키는 "각 파티션에 저장되는 데이터의 양과 디스크에서의 조직 방식"을 결정합니다. 이는 Cassandra의 읽기 처리 속도에 직접 영향을 미치므로 매우 중요한 설계 요소입니다. 기본 키는 다음으로 구성됩니다.

- **파티션 키 컬럼(Partition key columns, K)**: 데이터 그룹화를 정의합니다.
- **클러스터링 컬럼(Clustering columns, C↑ 또는 C↓)**: 고유성(uniqueness)을 보장하고 정렬 순서를 제어합니다. (↑는 오름차순 ASC, ↓는 내림차순 DESC)

**정적 컬럼(Static Columns)**: 특정 속성이 동일한 파티션 키의 모든 인스턴스에 걸쳐 일정하게 유지되는 경우, 이를 정적 컬럼으로 표시하여 중복 저장을 피할 수 있습니다.

### 시각화: Chebotko 표기법(Chebotko Notation)

Chebotko 표기법은 설계 내 쿼리와 테이블 간의 관계를 간단하고 정보가 풍부하게 시각화하는 방법을 제공합니다. 이 표준화된 다이어그램 표기법은 다음을 표시합니다.

- 테이블 이름과 컬럼 목록
- 기호로 표시된 기본 키 구성 요소 (K, C↑, C↓ 등)
- 각 테이블이 어떤 쿼리를 지원하는지 나타내는 쿼리 라인

### 일반적인 패턴(Common Patterns)

**와이드 파티션 패턴(Wide Partition Pattern)**: 빠른 다중 행(multi-row) 접근을 지원하기 위해 관련된 여러 행을 단일 파티션에 그룹화합니다. 이 패턴은 한 파티션 내의 여러 항목에 걸친 쿼리에 대해 잘 확장됩니다.

**시계열 패턴(Time Series Pattern)**: 와이드 파티션 접근법을 확장하여 타임스탬프(timestamp)가 찍힌 측정값을 저장합니다. 분석(analytics), 센서 데이터, 금융 애플리케이션 등에서 흔히 사용됩니다.

### 피해야 할 안티 패턴(Anti-Patterns to Avoid)

문서는 만료된 항목의 삭제에 의존하는 "큐(queue)" 설계를 경계할 것을 경고합니다. 삭제된 데이터는 **툼스톤(tombstone)** 이 되어 시간이 지남에 따라 읽기 성능을 저하시키기 때문입니다. 데이터 삭제에 의존하는 모든 설계는 재고되어야 합니다.

---

## 물리적 데이터 모델링

### 개요

물리적 데이터 모델링(physical data modeling) 단계는 논리적 모델 개발 이후에 진행됩니다. 문서에 따르면 "논리적 데이터 모델이 정의되고 나면 물리적 모델을 만드는 과정은 비교적 간단합니다."

논리적 모델의 각 요소에 CQL 데이터 타입을 할당합니다. 기본 타입(basic type), 컬렉션(collection), 사용자 정의 타입(user-defined type, UDT)을 포함한 모든 유효한 CQL 데이터 타입을 사용할 수 있습니다. 이 과정에는 크기 계산과 구현 전 테스트가 포함됩니다.

### Chebotko 표기법 확장(Chebotko Notation Extensions)

물리적 모델 표현은 각 컬럼에 타입 정보를 추가합니다. 시각적 표시자는 다음을 나타냅니다.

- 키스페이스(keyspace) 지정
- 컬렉션(collection)
- 사용자 정의 타입(UDT)
- 정적 컬럼(static column)
- 보조 인덱스(secondary index) 컬럼

### 호텔 데이터 모델 예제

이 설계는 두 개의 키스페이스로 나뉩니다.

- **hotel 키스페이스**: 호텔(hotel) 및 가용성(availability) 테이블 포함
- **reservation 키스페이스**: 예약(reservation) 및 게스트(guest) 정보 테이블 포함

#### 호텔 테이블의 특성

`hotels` 테이블은 다음을 사용합니다.

- 호텔 식별자(예: "AZ123")에 `text` 타입 사용
- 위치 데이터에 커스텀 `address` 사용자 정의 타입(UDT) 사용
- 전화번호에 `text` 사용 (국제 형식의 다양성을 수용하기 위함)

문서는 `uuid` 타입이 더 나은 고유성을 제공하지만, 호텔 업계의 관행은 짧은 코드(short code)를 식별자로 선호한다고 언급합니다.

### 예약 모델

예약 설계는 다음 기준으로 쿼리를 지원하는 세 개의 비정규화된 테이블을 포함합니다.

- 확인 번호(confirmation number)
- 게스트 정보(guest information)
- 호텔과 날짜의 조합(hotel and date combination)

`address` UDT는 reservation 키스페이스 내에서 다시 선언(redeclare)되어야 합니다. 이는 UDT가 정의된 키스페이스 내에서만 존재하는 범위 제한(scope limitation)을 반영합니다.

---

## 데이터 모델 평가 및 개선

### 파티션 크기 계산(Calculating Partition Size)

성능 문제를 방지하려면 파티션 크기를 평가해야 합니다. Cassandra의 하드 리밋(hard limit)은 파티션당 20억(2 billion) 셀(cell)이지만, "그 한계에 도달하기 훨씬 전에 성능 문제가 발생할 수 있습니다."

제시된 공식은 다음과 같습니다.

**Nv = Nr(Nc - Npk - Ns) + Ns**

각 항목의 의미는 다음과 같습니다.

- **Nv** = 파티션 내 값(셀)의 개수 (number of values/cells)
- **Nr** = 행의 개수 (number of rows)
- **Nc** = 컬럼의 개수 (number of columns)
- **Npk** = 기본 키 컬럼의 총 개수 (number of primary key columns)
- **Ns** = 정적 컬럼의 개수 (number of static columns)

핵심 통찰은 "파티션 크기의 주요 결정 요인은 파티션 내의 행 수"라는 점입니다.

### 디스크 크기 계산(Calculating Size on Disk)

디스크 공간 요구량을 추정하는 더 복잡한 공식은 다음과 같습니다.

**St = Σ sizeOf(파티션 키 컬럼) + Σ sizeOf(정적 컬럼) + Nr × (Σ sizeOf(일반/클러스터링 컬럼)) + Nv × sizeOf(메타데이터)**

셀당 메타데이터(metadata)는 일반적으로 평균 8바이트입니다. 예제에서는 `available_rooms_by_hotel_date` 테이블의 파티션 크기를 계산하여 약 1.1MB의 결과를 도출합니다.

### 큰 파티션 분할하기(Breaking Up Large Partitions)

파티션이 너무 커지면, 그 해결 기법은 단순합니다. "파티션 키에 컬럼을 하나 더 추가"하는 것입니다. 두 가지 접근법이 제시됩니다.

1. **버케팅(Bucketing)** — 월(month)과 같은 그룹화 컬럼을 도입하여 파티션 크기를 조절합니다.
2. **샤딩(Sharding)** — 새로운 컬럼을 샤딩 키(sharding key)로 추가합니다.

문서는 이 방법이 "추가적인 애플리케이션 로직"을 요구하지만, 파티션이 실용적인 한계를 초과하는 것을 막아준다고 언급합니다.

---

## 데이터베이스 스키마 정의

아래는 호텔 예약 예제에 대한 최종 CQL 스키마입니다. 각 테이블은 `comment`를 통해 자신이 지원하는 쿼리(Q1~Q9)를 명시하고 있습니다.

### Hotel 키스페이스

```sql
CREATE KEYSPACE hotel WITH replication =
  {'class': 'SimpleStrategy', 'replication_factor' : 3};

CREATE TYPE hotel.address (
  street text,
  city text,
  state_or_province text,
  postal_code text,
  country text );

CREATE TABLE hotel.hotels_by_poi (
  poi_name text,
  hotel_id text,
  name text,
  phone text,
  address frozen<address>,
  PRIMARY KEY ((poi_name), hotel_id) )
  WITH comment = 'Q1. Find hotels near given poi'
  AND CLUSTERING ORDER BY (hotel_id ASC) ;

CREATE TABLE hotel.hotels (
  id text PRIMARY KEY,
  name text,
  phone text,
  address frozen<address>,
  pois set<text> )
  WITH comment = 'Q2. Find information about a hotel';

CREATE TABLE hotel.pois_by_hotel (
  poi_name text,
  hotel_id text,
  description text,
  PRIMARY KEY ((hotel_id), poi_name) )
  WITH comment = 'Q3. Find pois near a hotel';

CREATE TABLE hotel.available_rooms_by_hotel_date (
  hotel_id text,
  date date,
  room_number smallint,
  is_available boolean,
  PRIMARY KEY ((hotel_id), date, room_number) )
  WITH comment = 'Q4. Find available rooms by hotel date';

CREATE TABLE hotel.amenities_by_room (
  hotel_id text,
  room_number smallint,
  amenity_name text,
  description text,
  PRIMARY KEY ((hotel_id, room_number), amenity_name) )
  WITH comment = 'Q5. Find amenities for a room';
```

### Reservation 키스페이스

```sql
CREATE KEYSPACE reservation WITH replication = {'class':
  'SimpleStrategy', 'replication_factor' : 3};

CREATE TYPE reservation.address (
  street text,
  city text,
  state_or_province text,
  postal_code text,
  country text );

CREATE TABLE reservation.reservations_by_confirmation (
  confirm_number text,
  hotel_id text,
  start_date date,
  end_date date,
  room_number smallint,
  guest_id uuid,
  PRIMARY KEY (confirm_number) )
  WITH comment = 'Q6. Find reservations by confirmation number';

CREATE TABLE reservation.reservations_by_hotel_date (
  hotel_id text,
  start_date date,
  end_date date,
  room_number smallint,
  confirm_number text,
  guest_id uuid,
  PRIMARY KEY ((hotel_id, start_date), room_number) )
  WITH comment = 'Q7. Find reservations by hotel and date';

CREATE TABLE reservation.reservations_by_guest (
  guest_last_name text,
  hotel_id text,
  start_date date,
  end_date date,
  room_number smallint,
  confirm_number text,
  guest_id uuid,
  PRIMARY KEY ((guest_last_name), hotel_id) )
  WITH comment = 'Q8. Find reservations by guest name';

CREATE TABLE reservation.guests (
  guest_id uuid PRIMARY KEY,
  first_name text,
  last_name text,
  title text,
  emails set<text>,
  phone_numbers list<text>,
  addresses map<text, frozen<address>>,
  confirm_number text
) WITH comment = 'Q9. Find guest by ID';
```

> 참고: `address` UDT가 `hotel`과 `reservation` 두 키스페이스에 각각 정의되어 있습니다. 이는 UDT가 정의된 키스페이스 범위 내에서만 유효하기 때문입니다.

---

## 데이터 모델링 도구

"Cassandra 스키마를 설계 및 관리하고 쿼리를 작성하는 데 도움이 되는 여러 도구가 있습니다."

### 사용 가능한 도구

**Hackolade**
Cassandra를 비롯한 여러 NoSQL 데이터베이스를 지원하는 데이터 모델링 솔루션입니다. 파티션 키, 클러스터링 컬럼 같은 CQL 고유 기능과 컬렉션, UDT를 포함한 데이터 타입을 처리합니다. Chebotko 다이어그램 생성을 지원합니다.

**Kashlev Data Modeler**
"접근 패턴 식별, 개념적/논리적/물리적 데이터 모델링, 스키마 생성을 포함하여 이 문서에서 설명하는 데이터 모델링 방법론을 자동화하는 Cassandra 데이터 모델링 도구"입니다. 설계의 출발점으로 사용할 수 있는 모델 패턴(model pattern)을 제공합니다.

**DataStax DevCenter**
스키마 및 쿼리 관리 도구로, 현재는 적극적으로 유지보수되지 않지만 무료로 다운로드할 수 있습니다. CQL 구문 강조(syntax highlighting), 명령어 자동 완성, 오류 감지, 다중 스크립트 창, 클러스터 연결, 성능 분석을 위한 쿼리 추적(query trace) 기능을 제공합니다.

**IDE 플러그인(IDE Plugins)**
IntelliJ IDEA, Apache NetBeans 등의 개발 환경을 위한 CQL 플러그인이 존재합니다. 일반적으로 스키마 관리와 쿼리 실행 기능을 제공합니다.

### 중요한 고려 사항

도구를 선택할 때는, Cassandra를 관계형 데이터베이스처럼 취급하는 JDBC/ODBC 드라이버에 의존하지 않고 **CQL을 네이티브로 지원**하는지 확인해야 합니다. 이는 Cassandra 모범 사례(best practice)와의 정합성을 보장합니다.

---

## 참고 자료

- [Apache Cassandra 공식 문서](https://cassandra.apache.org/doc/latest/)
- [Data Modeling - Introduction](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/intro.html)
- [Conceptual Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_conceptual.html)
- [RDBMS Design](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_rdbms.html)
- [Defining Application Queries](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_queries.html)
- [Logical Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_logical.html)
- [Physical Data Modeling](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_physical.html)
- [Evaluating and Refining Data Models](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_refining.html)
- [Defining Database Schema](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_schema.html)
- [Cassandra Data Modeling Tools](https://cassandra.apache.org/doc/latest/cassandra/developing/data-modeling/data-modeling_tools.html)
- *Cassandra: The Definitive Guide* (3rd Edition), Jeff Carpenter & Eben Hewitt, O'Reilly Media, 2020
