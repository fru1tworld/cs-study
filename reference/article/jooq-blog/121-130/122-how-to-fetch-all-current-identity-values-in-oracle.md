# Oracle에서 모든 현재 Identity 값을 가져오는 방법

> 원문: https://blog.jooq.org/how-to-fetch-all-current-identity-values-in-oracle/

2019년 7월 16일 lukaseder 작성

---

Oracle 12c는 유용한 SQL 표준 `IDENTITY` 기능을 도입했는데, 이는 본질적으로 시퀀스를 컬럼 기본값에 바인딩하는 것에 대한 문법적 설탕(syntax sugar)입니다. 다음과 같이 사용할 수 있습니다:

```sql
create table t1 (col1 number generated always as identity);
create table t2 (col2 number generated always as identity);

insert into t1 values (default);
insert into t1 values (default);
insert into t1 values (default);
insert into t2 values (default);

select * from t1;
select * from t2;
```

이것은 다음과 같은 결과를 생성합니다:

```
COL1
----
  1
  2
  3

COL2
----
  1
```

데이터베이스에 대한 단위 테스트를 위해, 우리는 identity들이 어떤 "상태"에 있는지 알고 싶을 수 있습니다. 각 테이블에 대해, 해당 identity가 다음에 생성할 값이 무엇인지 알고 싶습니다. 모든 지원 시퀀스 이름을 알고 있다면 `seq.currval`을 쿼리할 수 있겠지만, 시퀀스 이름들은 자동 생성되므로 알 수 없습니다. 그러나 다음과 같이 딕셔너리 뷰를 쿼리하여 이 정보를 얻을 수 있습니다:

```sql
select data_default
from user_tab_cols
where data_default is not null
and identity_column = 'YES'
and table_name in ('T1', 'T2');
```

대안으로 `user_tab_identity_cols`를 쿼리할 수 있습니다. 이것은 다음과 같은 결과를 생성합니다:

```
"TEST"."ISEQ$$_116601".nextval
"TEST"."ISEQ$$_116603".nextval
```

이제 우리가 게으르다면, 각각의 표현식에 대해 `EXECUTE IMMEDIATE`를 실행하면 됩니다:

```sql
set serveroutput on
declare
  v_current number;
begin
  for rec in (
    select table_name, data_default
    from user_tab_cols
    where data_default is not null
    and identity_column = 'YES'
    and table_name in ('T1', 'T2')
  ) loop
    execute immediate replace(
      'select ' || rec.data_default || ' from dual',
      '.nextval',
      '.currval'
    ) into v_current;
    dbms_output.put_line(
      'Table : ' || rec.table_name ||
      ', currval : ' || v_current
    );
  end loop;
end;
/
```

이것은 다음과 같은 결과를 생성합니다:

```
Table : T1, currval : 3
Table : T2, currval : 1
```

대안으로, 이 결과를 `DBMS_OUTPUT` 내용 대신 SQL 결과로 얻고 싶다면, 다음을 실행할 수 있습니다:

```sql
with
  function current_value(p_table_name varchar2) return number is
    v_current number;
  begin
    for rec in (
      select data_default
      from user_tab_cols
      where table_name = p_table_name
      and data_default is not null
      and identity_column = 'YES'
    )
    loop
      execute immediate replace(
        'select ' || rec.data_default || ' from dual',
        '.nextval',
        '.currval'
      ) into v_current;
      return v_current;
    end loop;

    return null;
  end;
select *
from (
  select table_name, current_value(table_name) current_value
  from user_tables
  where table_name in ('T1', 'T2')
)
where current_value is not null
order by table_name;
/
```

`user_tab_identity_cols`를 사용하는 대안은 다음과 같습니다:

```sql
with
  function current_value(p_table_name varchar2) return number is
    v_current number;
  begin
    for rec in (
      select sequence_name
      from user_tab_identity_cols
      where table_name = p_table_name
    )
    loop
      execute immediate
        'select ' || rec.sequence_name || '.currval from dual'
      into v_current;
      return v_current;
    end loop;

    return null;
  end;
select *
from (
  select table_name, current_value(table_name) current_value
  from user_tables
)
where current_value is not null
order by table_name;
/
```

결과는 이제 깔끔한 SQL 결과 집합입니다:

```
TABLE_NAME   CURRENT_VALUE
--------------------------
T1           3
T2           1
```

---

## 댓글 섹션

Matthias Rogel의 댓글 (2019년 7월 16일 11:59)

저에게는 결과가 그렇게 깔끔하지 않네요:

```
SQL> create table t1 (col1 number generated always as identity);

Table created. SQL> create table t2 (col2 number generated always as identity);

Table created. SQL> with   2 function current_value(p_table_name varchar2) return number is   3 v_current number;   4 begin   5 for rec in (   6 select data_default   7 from user_tab_cols   8 where table_name = p_table_name   9 and data_default is not null   10 and identity_column = 'YES'   11 )   12 loop   13 execute immediate replace(   14 'select ' || rec.data_default || ' from dual',   15 '.nextval',   16 '.currval'   17 ) into v_current;   18 return v_current;   19 end loop;   20   21 return null;   22 end;   23 select *   24 from (   25 select table_name, current_value(table_name) current_value   26 from user_tables   27 where table_name in ('T1', 'T2')   28 )   29 where current_value is not null   30 order by table_name;   31 /   order by table_name   *   ERROR at line 30:   ORA-08002: sequence ISEQ$$_1954315.CURRVAL is not yet defined in this session   ORA-06512: at line 22
```

lukaseder의 답글 (2019년 7월 16일 13:04)

네, 알고 있습니다. 임시 해결책으로 먼저 NEXTVAL로 한 번 실행한 다음 CURRVAL로 다시 실행하세요...

Juliano의 댓글 (2019년 7월 22일 21:29)

관련 없는 이야기지만, JavaScript용 JOOQ를 만들 생각을 해보신 적 있나요? JOOQ와 같은 품질로, JavaScript의 SQL 시장을 쉽게 지배할 수 있을 것 같습니다. 제가 틀릴 수도 있지만, JOOQ는 sequelize 같은 솔루션보다 훨씬 더 완성도가 높아 보입니다. 하지만 JavaScript에 JDBC 같은 API가 없다는 점이 이것을 꽤 어렵게 만들 것 같네요...

lukaseder의 답글 (2019년 7월 23일 13:33)

댓글 정말 감사합니다. 네, 물론, 저희는 jOOQ를 다른 언어로 가져가는 방법에 대해 끊임없이 생각하고 있지만, 그것은 많은 노력과 준비가 필요합니다. 현재, 저희는 다른 언어에서 API를 최대한 재사용할 수 있도록 Java 버전을 개선하는 방법을 검토하고 있습니다 (예: 메타 언어로부터 API와 구현의 상당 부분을 생성하는 방식).

JavaScript (더 정확히는: TypeScript)는 매우 흥미로운 후보가 될 것입니다. 하지만 무언가를 출시하려면 2020/2021년까지는 걸릴 것 같습니다.
