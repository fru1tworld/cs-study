# GROUP BY ROLLUP / CUBE

> 원문: https://blog.jooq.org/group-by-rollup-cube/

때때로 SQL의 한계에 부딪히게 만드는 요구 사항을 마주할 때가 있습니다. 아마 많은 분들이 일찍 포기하고 Java(또는 여러분이 사용하는 언어)에서 계산할 것입니다. 하지만 SQL로 하면 훨씬 쉽고 빠르게 처리할 수 있었을지도 모릅니다.

DB2, Oracle, SQL Server, Sybase SQL Anywhere, 그리고 MySQL과 같은 고급 데이터베이스들은 복잡한 집계를 위해 ROLLUP, CUBE, GROUPING SETS 함수를 지원합니다.

## 샘플 데이터

다음은 가상의 급여 데이터입니다:

```sql
select 'Lukas'      as employee,
       'SoftSkills' as company,
       80000        as salary,
       2007         as year
from dual
union all select 'Lukas', 'SoftSkills', 80000,  2008 from dual
union all select 'Lukas', 'SmartSoft',  90000,  2009 from dual
union all select 'Lukas', 'SmartSoft',  95000,  2010 from dual
union all select 'Lukas', 'jOOQ',       200000, 2011 from dual
union all select 'Lukas', 'jOOQ',       250000, 2012 from dual
union all select 'Tom',   'SoftSkills', 89000,  2007 from dual
union all select 'Tom',   'SoftSkills', 90000,  2008 from dual
union all select 'Tom',   'SoftSkills', 91000,  2009 from dual
union all select 'Tom',   'SmartSoft',  92000,  2010 from dual
union all select 'Tom',   'SmartSoft',  93000,  2011 from dual
union all select 'Tom',   'SmartSoft',  94000,  2012 from dual
```

## 기본 집계

직원별 평균 급여:

```sql
with data as ([위의 select])
select employee, avg(salary)
from data
group by employee
```

결과:

| EMPLOYEE | AVG(SALARY) |
|----------|-------------|
| Lukas    | 132500      |
| Tom      | 91500       |

Lukas가 평균적으로 Tom보다 훨씬 더 많이 벌었다는 것을 알 수 있습니다.

회사 및 직원별 평균 급여:

```sql
with data as (...)
select company, employee, avg(salary)
from data
group by company, employee
order by company, employee
```

결과:

| COMPANY    | EMPLOYEE | AVG(SALARY) |
|------------|----------|-------------|
| jOOQ       | Lukas    | 225000      |
| SmartSoft  | Lukas    | 92500       |
| SmartSoft  | Tom      | 93000       |
| SoftSkills | Lukas    | 80000       |
| SoftSkills | Tom      | 90000       |

jOOQ 플랫폼의 보상이 우월하다는 것을 보여줍니다.

## ROLLUP 함수

그룹화 필드를 추가하면 일부 집계 정보를 "잃게" 됩니다. 바로 이때 ROLLUP, CUBE(그리고 GROUPING SETS)가 필요해집니다. ROLLUP은 유용한 집계 값을 담은 추가 행을 생성합니다.

```sql
with data as (...)
select company, employee, avg(salary)
from data
group by rollup(company), employee
```

결과:

| COMPANY    | EMPLOYEE | AVG(SALARY) |
|------------|----------|-------------|
| SmartSoft  | Tom      | 93000       |
| SoftSkills | Tom      | 90000       |
| {null}     | Tom      | 91500       |
| jOOQ       | Lukas    | 225000      |
| SmartSoft  | Lukas    | 92500       |
| SoftSkills | Lukas    | 80000       |
| {null}     | Lukas    | 132500      |

null 값은 롤업된 집계를 나타냅니다. 각 직원에 대해 모든 회사에 걸친 평균을 보여주는 소계 행이 추가되었습니다. ROLLUP에서는 그룹화 필드의 순서가 중요합니다.

여러 필드에 대한 ROLLUP:

ROLLUP 절에 여러 필드를 넣으면:

```sql
with data as (...)
select company, employee, avg(salary)
from data
group by rollup(employee, company)
```

결과:

| COMPANY    | EMPLOYEE | AVG(SALARY) |
|------------|----------|-------------|
| SmartSoft  | Tom      | 93000       |
| SoftSkills | Tom      | 90000       |
| {null}     | Tom      | 91500       |
| jOOQ       | Lukas    | 225000      |
| SmartSoft  | Lukas    | 92500       |
| SoftSkills | Lukas    | 80000       |
| {null}     | Lukas    | 132500      |
| {null}     | {null}   | 112000      |

마지막 행은 모든 직원과 모든 회사에 걸친 전체 평균을 보여줍니다.

## GROUPING_ID() 함수

리포팅을 위해 합계 행을 식별하려면, DB2, Oracle, SQL Server, Sybase SQL Anywhere에서 GROUPING() 함수를 사용할 수 있습니다.

```sql
with data as (...)
select grouping_id(employee, company) id, company, employee, avg(salary)
from data
group by rollup(employee, company)
```

결과:

| ID | COMPANY    | EMPLOYEE | AVG(SALARY) |
|----|------------|----------|-------------|
| 0  | SmartSoft  | Tom      | 93000       |
| 0  | SoftSkills | Tom      | 90000       |
| 1  | {null}     | Tom      | 91500       |
| 0  | jOOQ       | Lukas    | 225000      |
| 0  | SmartSoft  | Lukas    | 92500       |
| 0  | SoftSkills | Lukas    | 80000       |
| 1  | {null}     | Lukas    | 132500      |
| 3  | {null}     | {null}   | 112000      |

ID 값은 각 행이 어떤 그룹화 수준에서 생성되었는지를 나타냅니다. 0은 상세 데이터, 1은 직원별 소계, 3은 전체 합계입니다.

## CUBE 함수

CUBE 함수는 비슷하게 동작하지만, CUBE 그룹화 필드의 순서가 무관해진다는 점이 다릅니다. 모든 그룹화 조합이 결합되기 때문입니다.

```sql
with data as (...)
select grouping_id(employee, company) id, company, employee, avg(salary)
from data
group by cube(employee, company)
```

결과:

| ID | COMPANY    | EMPLOYEE | AVG(SALARY) |
|----|------------|----------|-------------|
| 3  | {null}     | {null}   | 112000      |
| 2  | jOOQ       | {null}   | 225000      |
| 2  | SmartSoft  | {null}   | 92800       |
| 2  | SoftSkills | {null}   | 86000       |
| 1  | {null}     | Tom      | 91500       |
| 0  | SmartSoft  | Tom      | 93000       |
| 0  | SoftSkills | Tom      | 90000       |
| 1  | {null}     | Lukas    | 132500      |
| 0  | jOOQ       | Lukas    | 225000      |
| 0  | SmartSoft  | Lukas    | 92500       |
| 0  | SoftSkills | Lukas    | 80000       |

GROUPING_ID 값의 의미:

- 0: 회사 및 직원별 평균
- 1: 직원별 평균 (회사 무관)
- 2: 회사별 평균 (직원 무관)
- 3: 전체 평균

CUBE() 함수를 사용하면 CUBE() 함수에 제공된 그룹화 필드의 가능한 모든 조합에 대한 그룹화 결과를 얻게 됩니다. 이는 n개의 "큐브된" 그룹화 필드에 대해 2^n개의 GROUPING_ID()를 생성합니다.

## jOOQ 지원

jOOQ 2.0에서 이러한 함수들에 대한 지원이 도입되었습니다.

```java
create.select(
         groupingId(DATA.EMPLOYEE, DATA.COMPANY).as("id"),
         DATA.COMPANY, DATA.EMPLOYEE, avg(SALARY))
      .from(DATA)
      .groupBy(cube(DATA.EMPLOYEE, DATA.COMPANY));
```

## 결론

이 강력한 도구를 사용하면, 멋진 리포트와 데이터 개요를 만들 준비가 된 것입니다.

참고 자료: [ROLLUP(), CUBE(), GROUPING SETS()에 대한 SQL Server 문서](http://msdn.microsoft.com/en-us/library/bb522495.aspx)
