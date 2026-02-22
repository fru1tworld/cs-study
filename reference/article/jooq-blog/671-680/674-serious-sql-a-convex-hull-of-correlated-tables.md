# 진지한 SQL: 상관 테이블의 "볼록 껍질(Convex Hull)"

> 원문: https://blog.jooq.org/serious-sql-a-convex-hull-of-correlated-tables/

다음과 같은 흥미로운 데이터베이스 문제가 있습니다: 외래 키 관계를 통해 서로 연결된 모든 테이블을 찾는 것입니다. 이것은 본질적으로 스키마에서 상관된 테이블들 주위에 "볼록 껍질"을 계산하는 것과 같습니다.

## 문제

수많은 테이블과 외래 키 참조가 있는 대규모 데이터베이스가 주어졌을 때, 목표는 시작 테이블에서 이러한 관계를 통해 어떻게든 연결된 모든 테이블을 식별하는 것입니다. 이 글에서는 이것을 관련 테이블 주위에 "볼록 껍질"을 만드는 것으로 설명합니다.

## Java 솔루션

제안된 의사 알고리즘은 원본 테이블로 집합을 초기화한 다음, 새로운 테이블이 나타나지 않을 때까지 참조하는 테이블과 참조되는 테이블을 모두 반복적으로 추가합니다:

```java
public class Hull {
  public static Set<Table<?>> hull(Table<?>... tables) {
    Set<Table<?>> result =
        new HashSet<Table<?>>(Arrays.asList(tables));

    // 더 이상 새로운 테이블이 추가되지 않을 때까지 반복
    int size = 0;
    while (result.size() > size) {
      size = result.size();

      for (Table<?> table : new ArrayList<Table<?>>(result)) {
        // 이 테이블이 참조하는 모든 테이블 추가 (외래 키 따라가기)
        for (ForeignKey<?, ?> fk : table.getReferences()) {
          result.add(fk.getKey().getTable());
        }

        // 이 테이블을 참조하는 모든 테이블 추가 (역방향 참조)
        for (Table<?> other : table.getSchema().getTables()) {
          if (other.getReferencesTo(table).size() > 0) {
            result.add(other);
          }
        }
      }
    }
    return result;
  }

  public static void main(String[] args) {
    System.out.println(hull(T_AUTHOR));
  }
}
```

이 Java 솔루션은 jOOQ의 생성된 클래스를 사용하여 간단한 접근 방식을 구현합니다:

- 초기 테이블 집합으로 시작합니다
- 기존 테이블들을 반복적으로 순회합니다
- 외부로 나가는 외래 키를 따라 참조되는 테이블을 찾습니다
- 같은 스키마 내에서 들어오는 외래 키를 따라 참조하는 테이블을 찾습니다
- 새로운 테이블이 발견되지 않을 때까지 계속합니다

## Oracle SQL 솔루션

저자는 공통 테이블 표현식(CTE), `CONNECT BY` 절, 그리고 XML 조작을 사용하여 Oracle에서 이 문제를 해결하는 정교한 단일 SQL 문을 제시합니다:

```sql
-- 외래 키 관계를 무방향 그래프로 표현
with graph as (
  -- 정방향 관계 (참조하는 테이블 -> 참조되는 테이블)
  select c1.table_name t1, c2.table_name t2
  from all_constraints c1
    join all_constraints c2
      on c1.owner = c2.r_owner
      and c1.constraint_name = c2.r_constraint_name
  where c1.owner = 'TEST'
  union all
  -- 역방향 관계 (참조되는 테이블 -> 참조하는 테이블)
  select c2.table_name t1, c1.table_name t2
  from all_constraints c1
    join all_constraints c2
      on c1.owner = c2.r_owner
      and c1.constraint_name = c2.r_constraint_name
  where c1.owner = 'TEST'
),
-- 그래프를 순회하여 모든 경로 생성
paths as (
  select sys_connect_by_path(t1, '#') || '#' path
  from graph
  connect by nocycle prior t1 = t2
),
-- 각 테이블이 속한 경로 식별
subgraph as (
  select distinct t.table_name,
    regexp_replace(p.path, '^#(.*)#$', '\1') path
  from paths p
  cross join all_tables t
  where t.owner = 'TEST'
  and p.path like '%#' || t.table_name || '#%'
),
-- XML을 사용하여 경로를 개별 테이블 이름으로 분할
split_paths as (
  select distinct table_name origin,
    cast(t.column_value.extract('//text()') as varchar2(4000)) table_names
  from subgraph,
    table(xmlsequence(xmltype(
        '<x><x>' || replace(path, '#', '</x><x>') ||
        '</x></x>').extract('//x/*'))) t
),
-- 각 원본 테이블에 대해 연결된 테이블들을 집계
table_graphs as (
  select
    origin,
    count(*) graph_size,
    listagg(table_names, ', ') within group (order by 1) table_names
  from split_paths
  group by origin
)
-- 최종 결과 출력
select
  origin,
  graph_size "SIZE",
  dense_rank() over (order by table_names) id,
  table_names
from table_graphs
order by origin
```

## 핵심 기법

이 SQL 쿼리는 여러 고급 Oracle 기능을 보여줍니다:

- CTE (WITH 절): 중간 결과 집합을 구축하기 위함
- CONNECT BY NOCYCLE: 외래 키 그래프를 순회하기 위함 (순환 참조 방지)
- SYS_CONNECT_BY_PATH(): 연결된 테이블들을 통한 경로를 구축하기 위함
- XML 조작: 경로를 파싱하기 위해 `XMLTYPE`과 `XMLSEQUENCE` 사용
- 집계 함수: `LISTAGG()`와 `DENSE_RANK()` 같은 윈도우 함수

## 결과

이 쿼리는 각 테이블을 원점으로 하여 연결된 그래프 내의 모든 관련 테이블을 나열하는 출력을 생성하며, 크기 카운트와 그룹화 식별자를 포함합니다.

jOOQ의 테스트 데이터베이스에 대해 실행하면, 쿼리는 상호 연결된 테이블 그룹을 성공적으로 식별합니다. 예를 들어, T_AUTHOR는 외래 키 경로를 통해 T_BOOK, T_LANGUAGE 및 기타 테이블을 포함한 7개의 관련 테이블에 연결됩니다.

## 결론

저자는 이 쿼리가 "끔찍하게 비효율적"이라고 언급하며, 독자들에게 동일한 결과를 달성하는 더 짧고 빠른 버전을 작성하도록 도전합니다. 이 예제는 시연 목적으로 jOOQ의 테스트 데이터베이스를 사용합니다.

이 글은 복잡한 분석 문제에 대한 SQL의 강력함을 강조하면서, 동시에 Java와 SQL 두 가지 접근 방식의 장단점을 비교합니다.
