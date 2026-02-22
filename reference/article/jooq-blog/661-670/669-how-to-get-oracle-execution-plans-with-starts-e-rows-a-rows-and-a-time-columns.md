# Starts, E-Rows, A-Rows, A-Time 컬럼이 포함된 Oracle 실행 계획을 얻는 방법

> 원문: https://blog.jooq.org/how-to-get-oracle-execution-plans-with-starts-e-rows-a-rows-and-a-time-columns/

이 내용은 아마 다른 곳에서도 찾을 수 있겠지만, 실행 계획에서 최대한 많은 정보를 빠르게 얻는 방법에 대한 간단한 정리입니다.

## 1단계: 통계 수집 활성화

실제 행 수와 시간 통계가 수집되도록 설정하세요.

다음과 같이 할 수 있습니다:

```sql
-- sys 사용자로 로그인
alter system set statistics_level = all;
```

## 2단계: SQL 실행

문제가 있는 SQL을 실행하세요.

(저는 나쁜 SQL을 작성하지 않기 때문에 예시를 드릴 수 없습니다.)

## 3단계: SQL_ID 찾기

다음 문장으로 sql_id를 찾으세요:

```sql
-- 가장 중요한 컬럼들입니다
select last_active_time, sql_id, child_number, sql_text
from v$sql
-- 여러분의 문장으로 필터링
where upper(sql_fulltext) like
  upper('%[여기에 SQL 문장의 일부 텍스트를 넣으세요]%')
-- 가장 최근 활동 순으로 정렬
order by last_active_time desc;
```

## 4단계: 커서와 실행 계획 가져오기

해당 문장의 커서와 실행 계획을 가져오세요:

```sql
select rownum, t.* from table(dbms_xplan.display_cursor(
  -- 이전에 조회한 sql_id를 여기에 넣으세요
  sql_id => '6dt9vvx9gmd1x',
  -- 커서당 여러 실행 계획이 있는 경우를 위한
  -- 커서 자식 번호
  cursor_child_no => 0,
  -- Starts, E-Rows, A-Rows, A-Time을 얻기 위한
  -- 포맷팅 지시사항
  FORMAT => 'ALL ALLSTATS LAST')) t;
```

## 5단계: 커서 제거

필요한 경우 커서를 제거하세요:

```sql
select address || ',' || hash_value from v$sqlarea
where sql_id = '6dt9vvx9gmd1x';

begin
  sys.dbms_shared_pool.purge(
    '00000000F3471988,2224337167','C',1);
end;
```

## 6단계: 모든 실행 계획 삭제

```sql
-- sys 사용자로 로그인
alter system flush shared_pool;
```

## 7단계: 버퍼 캐시 삭제

```sql
-- sys 사용자로 로그인
alter system flush buffer_cache;
```
