# jOOQ로 느린 쿼리를 감지하는 방법

> 원문: https://blog.jooq.org/how-to-detect-slow-queries-with-jooq/

방금 우리는 jOOQ 코드 생성기에 멋진 작은 기능을 구현했습니다: https://github.com/jOOQ/jOOQ/issues/4974 이 기능은 jOOQ 코드 생성기가 스키마 메타 정보를 리버스 엔지니어링하기 위해 느린 쿼리를 실행할 때마다 이를 감지합니다.

왜 이런 기능이 필요할까요? 우리의 개발 및 통합 테스트 환경에는 다양한 성능 엣지 케이스를 갖춘 거대한 스키마가 없습니다. 예를 들어, 5000개의 Oracle 시노님이나 각각 500개의 파라미터를 가진 10000개의 프로시저가 없습니다. 우리는 일부 일반적인 엣지 케이스를 다루긴 하지만, 모든 데이터베이스에서 그런 것은 아닙니다. 반면에 사용자들은 시간이 지나면 현재 상태를 받아들이는 경향이 있습니다. 코드 생성기가 느린가요? 물론이죠, 거대한 스키마를 가지고 있으니까요. 이러한 안일한 수용은 우리 제품 품질에 장애물이 됩니다. 우리는 사용자들이 겪는 모든 종류의 문제를 보고해 주기를 원하므로, 그들을 독려하고 싶었습니다.

## 그래서 우리는 해냈습니다

다가오는 jOOQ 버전 3.8(그리고 3.5.5, 3.6.5, 3.7.3의 패치 릴리스)에서, 우리는 jOOQ-meta에 대략 다음과 같은 멋진 작은 ExecuteListener를 추가했습니다:

```java
class PerformanceListener
    extends DefaultExecuteListener {

    StopWatch watch;
    class SQLPerformanceWarning
        extends Exception {}

    @Override
    public void executeStart(ExecuteContext ctx) {
        super.executeStart(ctx);
        watch = new StopWatch();
    }

    @Override
    public void executeEnd(ExecuteContext ctx) {
        super.executeEnd(ctx);
        if (watch.split() > 5_000_000_000L)
            log.warn(
                "Slow SQL",
                "jOOQ Meta executed a slow query"
              + "\n\n"
              + "Please report this bug here: "
              + "https://github.com/jOOQ/jOOQ/issues/new\n\n"
              + formatted(ctx.query()),
                new SQLPerformanceWarning());
    }
}
```

매우 간단합니다. 쿼리 실행을 시작할 때마다 "스톱워치"가 시작됩니다. 실행이 끝날 때마다 스톱워치가 5초 이상 경과했는지 확인합니다. 만약 그렇다면, 경고와 함께 이슈 트래커 링크, 포맷된 SQL 쿼리 버전, 그리고 느린 구문이 실행된 정확한 위치를 찾는 데 도움이 되는 스택 트레이스를 로깅합니다.

## 실행해 봅시다

우리가 이것을 한 이유는 PostgreSQL 코드 생성기가 모든 저장 프로시저를 가져오는(그리고 오버로드 인덱스를 생성하는) 느린 쿼리를 실행하는 것을 직접 보았기 때문입니다. 생성된 오류 메시지는 다음과 같습니다:

```
[WARNING] Slow SQL                 : jOOQ Meta executed a slow query (slower than 5 seconds)

Please report this bug here: https://github.com/jOOQ/jOOQ/issues/new

select
  "r1"."routine_schema",
  "r1"."routine_name",
  "r1"."specific_name",
  case when exists (
        select 1 as "one"
        from "information_schema"."parameters"
        where (
          "information_schema"."parameters"."specific_schema" = "r1"."specific_schema"
          and "information_schema"."parameters"."specific_name" = "r1"."specific_name"
          and upper("information_schema"."parameters"."parameter_mode") <> 'IN'
        )
      ) then 'void'
       else "r1"."data_type"
  end as "data_type",
  "r1"."character_maximum_length",
  "r1"."numeric_precision",
  "r1"."numeric_scale",
  "r1"."type_udt_schema",
  "r1"."type_udt_name",
  case when exists (
        select 1 as "one"
        from "information_schema"."routines" as "r2"
        where (
          "r2"."routine_schema" in (
            'public', 'multi_schema', 'pg_catalog'
          )
          and "r2"."routine_schema" = "r1"."routine_schema"
          and "r2"."routine_name" = "r1"."routine_name"
          and "r2"."specific_name" <> "r1"."specific_name"
        )
      ) then (
        select count(*)
        from "information_schema"."routines" as "r2"
        where (
          "r2"."routine_schema" in (
            'public', 'multi_schema', 'pg_catalog'
          )
          and "r2"."routine_schema" = "r1"."routine_schema"
          and "r2"."routine_name" = "r1"."routine_name"
          and "r2"."specific_name" <= "r1"."specific_name"
        )
      ) end as "overload",
  "pg_catalog"."pg_proc"."proisagg"
from "information_schema"."routines" as "r1"
  join "pg_catalog"."pg_namespace"
  on "pg_catalog"."pg_namespace"."nspname" = "r1"."specific_schema"
  join "pg_catalog"."pg_proc"
  on (
    "pg_catalog"."pg_proc"."pronamespace" = "pg_catalog"."pg_namespace".oid
    and (("pg_catalog"."pg_proc"."proname" || '_') || cast("pg_catalog"."pg_proc".oid as varchar)) = "r1"."specific_name"
  )
where (
  "r1"."routine_schema" in (
    'public', 'multi_schema', 'pg_catalog'
  )
  and not("pg_catalog"."pg_proc"."proretset")
)
order by
  "r1"."routine_schema" asc,
  "r1"."routine_name" asc,
  "overload" asc
```

이제 쿼리를 쉽게 수정할 수 있습니다.

## 여러분도 똑같이 할 수 있습니다!

ExecuteListener의 구현은 간단했습니다. 여러분도 매우 쉽게 똑같이 할 수 있습니다. 간단히 jOOQ Configuration에 실행 리스너를 연결하여 실행 속도를 측정하고 임계값 이후에 경고를 로깅하면 끝입니다. 즐거운 디버깅 되세요!

## 추가 읽을거리

우연히도, 매우 유사한 접근 방식이 Square 엔지니어링 팀에 의해 문서화되었습니다 - The Query Sniper: https://corner.squareup.com/2016/01/query-sniper.html
