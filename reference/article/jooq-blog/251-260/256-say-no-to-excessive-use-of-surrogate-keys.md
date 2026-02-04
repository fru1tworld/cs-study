# 성능이 정말 중요하다면 대리 키의 과도한 사용에 NO라고 말하라

> 원문: https://blog.jooq.org/say-no-to-excessive-use-of-surrogate-keys-if-performance-really-matters-to-you/

우리 프로그래머들은 이런 잘못된 아이디어들을 계속해서 카고 컬트하고 있다. 최근에 우리는 벤 다이어그램에 "NO"라고 말했다. 오늘은 대리 키에 NO라고 말할 것이다.

대리 키 vs 자연 키 비논쟁은 데이터 아키텍처에서 가장 과열된 논쟁 중 하나이며, 왜 모두가 그렇게 감정적인지 이해가 되지 않는다. 양측 모두 (탭 vs 스페이스 비논쟁에서처럼) 궁극적인 진실을 쥐고 있다고 주장하며, 정말 중요한 곳에서 부가 가치를 창출하는 것을 방해한다.

이 글은 왜 항상 그렇게 독단적이면 안 되는지에 대해 조명한다. 글을 끝까지 읽으면 동의하게 될 것이다, 약속한다.

## 대리 키의 장점은 무엇인가?

혹시 SQL 용어가 좀 녹슬었다면, 대리 키는 인위적인 식별자다. 또는 위키피디아의 표현을 빌리자면: "데이터베이스에서 대리 키는 모델링된 세계의 엔티티 또는 데이터베이스의 객체에 대한 고유 식별자다. 대리 키는 애플리케이션 데이터에서 파생되는 자연(또는 비즈니스) 키와 달리 애플리케이션 데이터에서 파생되지 않는다."

대리 키가 가지는 세 가지 명확한 장점이 있다:

1. 모든 테이블에 하나가 있다는 것을 *알고* 있으며, 아마도 모든 곳에서 같은 이름을 가진다: ID.
2. 상대적으로 작다 (예: BIGINT)
3. 자연 키를 찾으려고 30초 더 테이블을 설계하는 "번거로움"을 겪지 않아도 된다

더 많은 장점이 있지만 (위키피디아 문서를 확인해보라), 물론 단점도 있다. 오늘은 전혀 말이 되지 않는 경우에도 *모든* 테이블에 대리 키가 있는 아키텍처에 대해 이야기하고 싶다.

## 대리 키가 전혀 말이 되지 않는 경우는 언제인가?

현재 나는 로그 데이터베이스에 대해 실행되는 쿼리의 성능을 개선하는 고객을 돕고 있다. 로그 데이터베이스는 본질적으로 세션에 대한 관련 정보와 해당 세션과 관련된 일부 트랜잭션을 포함한다. ("이봐, 로그에 SQL을 사용하지 마"라는 결론으로 점프하는 경우를 대비해: 네, 그들은 비정형 로그에 Splunk도 사용한다. 하지만 여기 있는 것들은 정형화되어 있고, 그들은 이에 대해 많은 SQL 스타일 분석을 실행한다).

다음은 sessions 테이블에 저장되어 있다고 상상할 수 있는 것이다 (Oracle 사용):

```sql
CREATE TABLE sessions (

  -- 유용한 값들
  sess        VARCHAR2(50 CHAR) NOT NULL PRIMARY KEY,
  ip_address  VARCHAR2(15 CHAR) NOT NULL,
  tracking    VARCHAR2(50 CHAR) NOT NULL,

  -- 그리고 더 많은 정보가 여기에...
  ...
)
```

하지만, 스키마가 그렇게 설계되지 않았다. 다음과 같이 설계되었다:

```sql
CREATE TABLE sessions (

  -- 무의미한 대리 키
  id NUMBER(18) NOT NULL PRIMARY KEY,

  -- 유용한 값들
  sess        VARCHAR2(50 CHAR) NOT NULL,

  -- 무의미한 외래 키들
  ip_id       NUMBER(18) NOT NULL,
  tracking_id NUMBER(18) NOT NULL,
  ...

  FOREIGN KEY (ip_id)
    REFERENCES ip_addresses (ip_id),
  FOREIGN KEY (tracking_id)
    REFERENCES tracking (tracking_id),
  ...

  -- 기본 키가 아닌 UNIQUE 키
  UNIQUE (sess),
)
```

그래서, 이 소위 "팩트 테이블" (어쨌든 스타 스키마다)은 유용한 것을 포함하지 않고, 흥미로운 값들에 대한 참조를 포함하는 대리 키 집합만 포함하며, 그 값들은 모두 다른 테이블에 위치해 있다. 예를 들어, 주어진 IP 주소에 대한 모든 세션을 세고 싶다면, 이미 JOIN을 실행해야 한다:

```sql
SELECT ip_address, count(*)
FROM ip_addresses
JOIN sessions USING (ip_id)
GROUP BY ip_address
```

결국, 사용자가 정말로 보고 싶어하는 것은 대리 키가 아니라 IP 주소이기 때문에 조인이 필요하다. 당연하지, 그렇지? 다음은 실행 계획이다:

```
------------------------------------------------------
| Operation           | Name         | Rows  | Cost  |
------------------------------------------------------
| SELECT STATEMENT    |              |  9999 |   108 |
|  HASH GROUP BY      |              |  9999 |   108 |
|   HASH JOIN         |              | 99999 |   104 |
|    TABLE ACCESS FULL| IP_ADDRESSES |  9999 |     9 |
|    TABLE ACCESS FULL| SESSIONS     | 99999 |    95 |
------------------------------------------------------
```

두 개의 전체 테이블 스캔이 있는 완벽한 해시 조인이다. 내 예제 데이터베이스에는 10만 개의 세션과 1만 개의 IP 주소가 있다.

당연히, IP_ADDRESS 컬럼에 인덱스가 있다, 왜냐하면 이것으로 필터링할 수 있기를 원하기 때문이다. 다음과 같이 의미 있는 것으로:

```sql
SELECT ip_address, count(*)
FROM ip_addresses
JOIN sessions USING (ip_id)
WHERE ip_address LIKE '192.168.%'
GROUP BY ip_address
```

당연히, 반환하는 데이터가 적기 때문에 계획이 조금 더 낫다. 다음은 결과이다:

```
----------------------------------------------------------------
| Operation                     | Name         | Rows  | Cost  |
----------------------------------------------------------------
| SELECT STATEMENT              |              |     1 |    99 |
|  HASH GROUP BY                |              |     1 |    99 |
|   HASH JOIN                   |              |    25 |    98 |
|    TABLE ACCESS BY INDEX ROWID| IP_ADDRESSES |     1 |     3 |
|     INDEX RANGE SCAN          | I_IP         |     1 |     2 |
|    TABLE ACCESS FULL          | SESSIONS     | 99999 |    95 |
----------------------------------------------------------------
```

흥미롭다. 이제 인덱스 통계를 사용하여 프레디케이트가 ip_address 테이블에서 한 행만 반환할 것으로 추정할 수 있다. 그러나 여전히 상당한 비용의 해시 조인을 얻는다, 지금은 꽤 사소한 쿼리처럼 보이는 것에 대해.

## 대리 키가 없는 세상은 어떤 모습일까?

쉽다. sessions 테이블에서 IP 주소 관련 무언가가 필요할 때마다 더 이상 조인이 필요하지 않다.

우리의 두 쿼리는 간단하게 된다:

```sql
-- 모든 카운트
SELECT ip_address, count(*)
FROM sessions2
GROUP BY ip_address

-- 필터링된 카운트
SELECT ip_address, count(*)
FROM sessions2
WHERE ip_address LIKE '192.168.%'
GROUP BY ip_address
```

첫 번째 쿼리는 거의 같은 비용 추정치로 더 간단한 실행 계획을 생성한다.

```
--------------------------------------------------
| Operation          | Name      | Rows  | Cost  |
--------------------------------------------------
| SELECT STATEMENT   |           |   256 |   119 |
|  HASH GROUP BY     |           |   256 |   119 |
|   TABLE ACCESS FULL| SESSIONS2 | 99999 |   116 |
--------------------------------------------------
```

여기서 그렇게 많이 얻는 것 같지 않지만, 필터링된 쿼리에서는 어떻게 될까?

```
------------------------------------------------
| Operation            | Name  | Rows  | Cost  |
------------------------------------------------
| SELECT STATEMENT     |       |     1 |     4 |
|  SORT GROUP BY NOSORT|       |     1 |     4 |
|   INDEX RANGE SCAN   | I_IP2 |   391 |     4 |
------------------------------------------------
```

오 마이 갓! 비용이 어디로 갔지? 허, 이것은 극도로 빠른 것 같다! 벤치마크해보자!

```sql
SET SERVEROUTPUT ON
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT NUMBER := 100000;
BEGIN
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT ip_address, count(*)
      FROM ip_addresses
      JOIN sessions USING (ip_id)
      WHERE ip_address LIKE '192.168.%'
      GROUP BY ip_address
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Surrogate: '
    || (SYSTIMESTAMP - v_ts));
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT ip_address, count(*)
      FROM sessions2
      WHERE ip_address LIKE '192.168.%'
      GROUP BY ip_address
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Natural  : '
    || (SYSTIMESTAMP - v_ts));
END;
/
```

```
Surrogate: +000000000 00:00:03.453000000
Natural  : +000000000 00:00:01.227000000
```

개선이 상당하며, 여기에 데이터가 많지 않다. 이제 생각해보면, 당연한 것 아닌가? 의미론적으로 정확히 같은 쿼리에 대해, JOIN을 실행하거나 실행하지 않는다. 그리고 이것은 매우 매우 적은 데이터를 사용하고 있다. 실제 고객 데이터베이스에는 수억 개의 세션이 있으며, 이 모든 JOIN은 인위적인 대리 키를 위해 귀중한 리소스를 낭비하고 있다... 그것은 항상 그렇게 해왔기 때문에 도입되었다.

그리고, 항상 그렇듯이, 실행 계획 비용을 믿지 마라. 측정하라. 필터링되지 않은 쿼리 (해시 조인을 생성한 쿼리)의 100회 반복 벤치마킹 결과:

```
Surrogate: +000000000 00:00:06.053000000
Natural  : +000000000 00:00:02.408000000
```

생각해보면 여전히 당연하다.

우리가 아직 비정규화하고 있지 않다는 점에 주목하라. 여전히 `IP_ADDRESS` 테이블이 있지만, 이제 대리 키가 아닌 비즈니스 키(주소)를 기본 키로 포함한다. 더 드문 쿼리 사용 사례에서는 IP 주소 관련 정보(예: 국가)를 얻기 위해 여전히 테이블을 조인할 것이다.

## 극단으로 가보기

이 고객 사이트의 데이터베이스는 순수성을 높이 평가하는 사람이 설계했으며, 나도 때때로 그런 것에 공감할 수 있다. 하지만 이 경우에는 명백히 잘못되었다, 왜냐하면 실제로 "자연 키" (즉, 비즈니스 값)로 필터링하지만 그것을 하기 위해 또 다른 JOIN을 추가해야 하는 쿼리가 많이 있었기 때문이다.

이런 사례가 더 많이 있었다, 예를 들면:

* ISO 639 언어 코드
* ISIN 증권 번호
* 외부 시스템에서 공식 식별자를 가진 계좌 번호

그리고 훨씬 더 많다. 매번, 대리 키와 자연 키 사이의 추가 매핑을 얻기 위해 수십 개의 JOIN이 필요했다.

때때로 대리 키는 좋다. 디스크 공간을 조금 덜 사용한다. 다음 쿼리를 고려해보라 (Stack Overflow 사용자 WW.에게 감사를 표한다):

```sql
SELECT
  owner,
  table_name,
  TRUNC(SUM(bytes)/1024) kb,
  ROUND(ratio_to_report(SUM(bytes)) OVER() * 100) Percent
FROM (
  SELECT
    segment_name table_name,
    owner,
    bytes
  FROM dba_segments
  WHERE segment_type IN (
    'TABLE',
    'TABLE PARTITION',
    'TABLE SUBPARTITION'
  )
  UNION ALL
  SELECT
    i.table_name,
    i.owner,
    s.bytes
  FROM dba_indexes i,
    dba_segments s
  WHERE s.segment_name = i.index_name
  AND s.owner          = i.owner
  AND s.segment_type  IN (
    'INDEX',
    'INDEX PARTITION',
    'INDEX SUBPARTITION'
  )
)
WHERE owner = 'TEST'
AND table_name IN (
  'SESSIONS',
  'IP_ADDRESSES',
  'SESSIONS2'
 )
GROUP BY table_name, owner
ORDER BY SUM(bytes) DESC;
```

이것은 각 객체가 사용하는 디스크 공간을 보여줄 것이다:

```
TABLE_NAME                   KB    PERCENT
-------------------- ---------- ----------
SESSIONS2                 12288         58
SESSIONS                   8192         39
IP_ADDRESSES                768          4
```

그렇다. 이제 sessions 테이블의 기본 키가 `NUMBER(18)`이 아니라 `VARCHAR2(50)`이기 때문에 디스크 공간을 조금 더 사용한다. 하지만 디스크 공간은 극도로 저렴한 반면, 실제 실행 시간 성능은 필수적이다. 약간의 복잡성만 제거해도 간단한 쿼리에 대해 이미 성능이 크게 향상되었다.

## 결론

대리 키 또는 자연 키? 나는 둘 다라고 말한다. 대리 키에는 장점이 있다. 일반적으로 5개 컬럼 복합 기본 키를 원하지 않는다. 더욱이, 5개 컬럼 복합 외래 키는 원하지 않는다. 이러한 경우에, 대리 키를 사용하면 복잡성을 제거하여 (그리고 아마도 다시 성능을 높여) 가치를 더할 수 있다. 하지만 현명하게 선택하라. 종종, 대리 키를 맹목적으로 사용하면 매우 잘못될 것이고, 데이터를 쿼리할 때 그 대가를 톡톡히 치르게 될 것이다.
