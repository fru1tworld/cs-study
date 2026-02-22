# jOOQ 뉴스레터: 2014년 8월 15일 - jOOQ 3.5 전망

> 원문: https://blog.jooq.org/jooq-newsletter-august-15-2014-jooq-3-5-outlook/

## jOOQ 3.5 전망

우리는 다음 릴리스를 위해 열심히 작업하고 있습니다. 이미 jOOQ 3.5에 대한 90개의 이슈가 완료되었으며 계속 진행 중입니다! 2014년 4분기에 예정된 다음 마이너 릴리스에서 구현될 주요 사항들을 간략히 소개하겠습니다.

### 새로운 데이터베이스 지원

고객들이 Informix와 Oracle TimesTen 데이터베이스 지원을 요청해 왔습니다. Informix는 업계에서 여전히 널리 사용되는 매우 인기 있는(그리고 오래된!) 데이터베이스인 반면, Oracle TimesTen은 Oracle과 매우 유사한 문법을 가진 유망한 새로운 인메모리 데이터베이스입니다. 이 두 가지가 새로 추가되면 jOOQ는 18개의 RDBMS를 지원하게 됩니다!

### 파일 기반 코드 생성

이 기능은 오랫동안 우리의 로드맵에 있었는데, 드디어 착수하게 되었습니다! 개발 워크플로우에서 코드 생성 중에 데이터베이스에 접근하는 것이 어려운 경우, 이제 XML 형식으로 데이터베이스 메타 정보를 제공할 수 있습니다. 우리는 다른 형식보다 XML을 선택했는데, 이는 XSLT를 사용하여 임의의 기존 형식(예: Hibernate hbm.xml 또는 Vertabelo와 같은 ERD 도구의 내보내기 형식)을 매우 쉽게 변환할 수 있기 때문입니다. 이 기능이 출시되어 커뮤니티가 기여한 XSLT가 jOOQ를 여러분이 좋아하는 데이터베이스 스키마 정의 형식과 통합하는 데 도움이 되길 기대합니다.

### TypeProviders

TypeProviders는 jOOQ의 컬럼 타입인 "\<T\>" 타입에 대한 추상화를 가능하게 합니다. 이것은 데이터 타입 변환을 훨씬 넘어서, 사용자가 jOOQ가 사용자 타입을 JDBC에 완전히 투명하게 바인딩하는 방법을 지정할 수 있게 합니다. 이것은 특히 PostgreSQL의 벤더별 데이터 타입에 유용합니다.

이것들은 jOOQ 3.5에 계획된 주요 기능들 중 일부이며, 많은 마이너 기능들도 함께 포함될 예정입니다.

## Data Geekery 파트너 네트워크

Data Geekery에서 우리는 신뢰할 수 있는 통합 파트너를 통해 jOOQ 생태계에 서비스를 제공하고 추천하기 시작했습니다.

### UWS Software Service

독일에 기반을 둔 UWS Software Service(UWS)는 Java Enterprise 생태계에 뚜렷한 초점을 맞춘 맞춤형 소프트웨어 개발, 애플리케이션 현대화 및 아웃소싱을 전문으로 합니다.

UWS는 다양한 기업 소프트웨어 프로젝트에 jOOQ 오픈소스 에디션을 성공적으로 통합해 왔습니다. 그들의 제공 서비스에는 시스템 환경에 대한 맞춤형 jOOQ 통합과 JDBC 및/또는 JPA에서 jOOQ로의 마이그레이션 솔루션이 포함됩니다. UWS는 또한 jOOQ를 사용한 맞춤형 기업 애플리케이션 개발을 제공합니다.

### Markus Winand

Markus Winand는 인기 있는 책 "SQL Performance Explained"와 더욱 인기 있는 웹사이트 "Use The Index, Luke"의 저자입니다.

SQL 르네상스 대사로서, 그의 사명은 개발자들에게 21세기 SQL의 진화를 알리는 것입니다. 그의 책은 5개 언어로 출판되었으며 use-the-index-luke.com에서 무료로 온라인으로 읽을 수 있습니다.

Markus Winand는 SQL에 관심 있는 모든 회사와 개발자를 위한 트레이너, 연사 및 컨설턴트로 활동할 수 있습니다.

## 커뮤니티의 목소리

트위터에서 jOOQ에 대한 긍정적인 경험을 공유해 주신 분들이 있습니다:

Majid Azimi는 "jOOQ로 보스처럼 SQL 작성하기"에 대해 언급했습니다.

Christoph Henkelmann(@chenkelmann)은 2014년 8월 8일에 다음과 같이 트윗했습니다: "java 8 + @ninjaframework + @JavaOOQ + BoneCP => 웹/REST 개발의 행복. 드디어 내가 찾던 날씬하고, 빠르고, 신뢰할 수 있는 스택을 찾았다."

그는 훌륭한 웹 애플리케이션을 구축하기 위한 가장 멋진 스택을 찾았습니다 - Ninjaframework, jOOQ, 그리고 BoneCP로 구성된 날씬하고, 빠르고, 신뢰할 수 있는 스택입니다.

## SQL 코너

### 키셋 페이지네이션

LIMIT .. OFFSET, OFFSET .. FETCH 또는 다른 벤더별 변형을 사용하는 OFFSET 페이지네이션은 높은 페이지 번호에 도달할 때 심각한 성능 문제를 일으킬 수 있습니다. 불필요한 모든 레코드를 데이터베이스가 건너뛰어야 하기 때문입니다. 게다가 여러분의 데이터베이스가 아직 이를 올바르게 구현하지 않았을 가능성도 있습니다.

페이지네이션을 수행하는 훨씬 더 빠르고 안정적인 방법은 키셋 페이지네이션 방법(seek 방법이라고도 함)입니다. jOOQ는 키셋 페이지네이션을 수행하는 데 사용할 수 있는 합성 seek() 절을 지원합니다.

seek 방법을 원래 누가 발명했는지는 명확하지 않지만(일부에서는 "키셋 페이징"이라고도 부름), 매우 저명한 옹호자는 Markus Winand입니다. 본질적으로, seek 방법은 OFFSET 이전의 레코드를 건너뛰지 않고, 이전에 가져온 마지막 레코드까지의 레코드를 건너뜁니다.

키셋 페이지네이션은 "클래식" OFFSET 페이지네이션이 큰 페이지 번호에서 필연적으로 느려지는 매우 큰 결과 집합에서도 일정 시간 페이지네이션을 수행하는 매우 강력한 기술입니다.

"무한 스크롤"(트위터처럼)을 코딩하는 경우, 이것이 최선의 접근 방식입니다. 누군가가 다음 페이지로 이동하기 전에 새 레코드를 삽입해도 중복 레코드가 발생하지 않기 때문입니다.

키셋 페이지네이션은 235페이지로 직접 점프하는 것보다 "다음 페이지"를 지연 로딩하는 데 가장 유용합니다. 이것이 유용한 좋은 예는 트위터나 페이스북과 같은 소셜 미디어의 콘텐츠 스트림입니다. 페이지 하단에 도달하면 "다음 몇 개의 트윗"만 원합니다. 오프셋 3564의 트윗이 아닙니다.

참고로 SEEK 절을 OFFSET 절과 결합할 수는 없습니다.

### PIVOT 절

가끔씩 우리는 평범해 보이지 않는 것을 하고 싶은 드문 SQL 문제에 부딪힙니다. 이러한 것들 중 하나가 행을 열로 피벗하는 것입니다.

PIVOT 절은 테이블의 행을 미리 정의된 집계 열 집합으로 피벗할 수 있게 합니다. 변환은 Oracle과 SQL Server에서 실제로 매우 쉬운데, 둘 다 테이블 표현식에서 PIVOT 키워드를 지원합니다.

다음은 SQL Server PIVOT 구문의 예입니다:

```sql
SELECT p.*
FROM (
  SELECT dnId, propertyName, propertyValue
  FROM myTable
) AS t
PIVOT(
  MAX(propertyValue)
  FOR propertyName IN (
    objectsid,
    _objectclass,
    cn,
    samaccountname,
    name
  )
) AS p;
```

다음은 Oracle PIVOT 구문의 예입니다:

```sql
SELECT p.*
FROM (
  SELECT dnId, propertyName, propertyValue
  FROM myTable
) t
PIVOT(
  MAX(propertyValue)
  FOR propertyName IN (
    'objectsid'      as "objectsid",
    '_objectclass'   as "_objectclass",
    'cn'             as "cn",
    'samaccountname' as "samaccountname",
    'name'           as "name"
  )
) p;
```

PIVOT 작업은 값을 집계하고 속성 이름-값 쌍을 식별자(dnId)로 그룹화하여 개별 열로 전치함으로써 파생 테이블을 변환합니다.

Oracle이나 SQL Server가 아닌 다른 데이터베이스 사용자는 간단한 PIVOT 시나리오에서 GROUP BY와 MAX(CASE ...) 표현식을 사용하는 동등한 쿼리를 작성할 수 있습니다.

jOOQ는 org.jooq.Table 타입에서 PIVOT 절을 지원하는데, 피벗은 테이블에서 직접 수행되기 때문입니다. 현재 Oracle의 PIVOT 절만 지원됩니다. SQL Server의 약간 다른 PIVOT 절에 대한 지원은 나중에 추가될 예정입니다. 또한 jOOQ는 향후 다른 방언에 대해 PIVOT을 에뮬레이션할 수 있습니다.
