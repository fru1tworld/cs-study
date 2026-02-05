# 이걸 어떻게 할 수 있을까? - 물론 SQL로!

> 원문: https://blog.jooq.org/how-to-do-this-with-sql-of-course/

우리는 최근 FanPictor와 매우 흥미로운 프로젝트에 협업하게 되었습니다. FanPictor는 경기장에 팬 코레오그래피를 직접 그릴 수 있는 소프트웨어입니다.

## 유스 케이스

워크플로우는 대략 다음과 같습니다:

- 코레오그래피 제안서를 제출합니다
- 이벤트 주최자가 가장 좋은 제안을 선택합니다
- 이벤트가 열리기 전에 코레오그래피를 Excel 파일로 내보냅니다
- 인쇄소에서 Excel 파일을 기반으로 색색의 패널을 생산합니다
- 도우미들이 이 패널들을 경기장 좌석에 배포합니다
- 팬들이 결과를 경험합니다

다음은 로저 페더러에게 바치는 FanPictor 코레오그래피의 예시입니다:

[FanPictor로 팬 코레오그래피를 그리세요]

## 문제점

Excel 파일에서 내보낸 원본 데이터는 인쇄소에 직접 보내기에 적합하지 않았습니다. 이 패널들을 배포하는 것은 어리석고 반복적인 작업입니다. 인쇄소와 도우미들에게 필요한 것은 다음과 같은 지침입니다:

- 연속된 동일한 패널 열이 시작되거나 끝나는지 여부
- 동일한 패널이 몇 개인지

그래서 사람들이 몇 개의 빨간 패널이 있는지 세거나, 어디서 다음 색상으로 바뀌는지 찾을 필요가 없습니다.

[인쇄소 지침]

[초보자를 위한 인쇄소 지침]

## SQL 솔루션

이 문제에 대한 자연스러운 솔루션은 물론 SQL입니다. 정말 놀라운 PostgreSQL 쿼리를 확인해보세요!

```sql
with data as (
  select
    d.*,
    row(sektor, row, scene1, scene2) block
  from d
)
select
  sektor,
  row,
  seat,
  scene1,
  scene2,
  case
    when lag (block) over(o) is distinct from block
     and lead(block) over(o) is distinct from block
    then 'start / stop'
    when lag (block) over(o) is distinct from block
    then 'start'
    when lead(block) over(o) is distinct from block
    then 'stop'
    else ''
  end start_stop,
  count(*) over(
    partition by sektor, row, scene1, scene2
  ) cnt
from data
window o as (
  order by sektor, row, seat
)
order by sektor, row, seat;
```

여기서 무슨 일이 일어나고 있는지 설명하겠습니다:

### Row 값 생성자

PostgreSQL의 매우 멋진 기능을 사용하여 여러 열을 단일 ROW 타입으로 결합하고 있습니다. 이렇게 하면 나중에 더 쉽게 비교할 수 있습니다:

```sql
row(sektor, row, scene1, scene2) block
```

### IS DISTINCT FROM 술어

"block"(4개 열의 조합)이 무언가와 "다른지" 비교할 때 `IS DISTINCT FROM` 술어를 사용합니다. 이는 NULL 안전 비교 연산자로, SQL에서 NULL이 존재할 때 신뢰할 수 있는 로직을 작성하는 데 필수적입니다.

일반 `<>` 연산자와 달리, `IS DISTINCT FROM`은 NULL 값도 올바르게 처리합니다. 예를 들어:
- `NULL <> NULL`은 `NULL`을 반환합니다 (예상치 못한 결과!)
- `NULL IS DISTINCT FROM NULL`은 `FALSE`를 반환합니다 (원하는 결과!)

### 윈도우 함수

가장 강력한 부분은 윈도우 함수입니다. "윈도우 함수 이전의 SQL과 윈도우 함수 이후의 SQL이 있습니다"라는 말이 있을 정도입니다.

`LAG()` 함수는 이전 행의 값에 접근하고, `LEAD()` 함수는 다음 행의 값에 접근합니다. 이를 사용하여 현재 블록이 이전 또는 다음 블록과 다른지 확인할 수 있습니다:

```sql
case
  when lag (block) over(o) is distinct from block
   and lead(block) over(o) is distinct from block
  then 'start / stop'
  when lag (block) over(o) is distinct from block
  then 'start'
  when lead(block) over(o) is distinct from block
  then 'stop'
  else ''
end start_stop
```

이 로직은:
- 이전 블록도 다르고 다음 블록도 다르면: 'start / stop' (단일 항목)
- 이전 블록만 다르면: 'start' (시퀀스 시작)
- 다음 블록만 다르면: 'stop' (시퀀스 끝)
- 둘 다 같으면: '' (시퀀스 중간)

### COUNT와 PARTITION BY

`GROUP BY` 절 없이 그룹 내 개수를 계산하기 위해 `COUNT(*)` 윈도우 함수와 `PARTITION BY`를 사용합니다:

```sql
count(*) over(
  partition by sektor, row, scene1, scene2
) cnt
```

이렇게 하면 각 행에 해당 섹터, 행, scene1, scene2 조합의 총 개수가 표시됩니다.

### 이름이 지정된 WINDOW 절

쿼리를 더 깔끔하게 만들기 위해 이름이 지정된 윈도우 절을 사용합니다:

```sql
window o as (
  order by sektor, row, seat
)
```

이렇게 하면 `over(order by sektor, row, seat)`를 여러 번 반복하는 대신 간단히 `over(o)`라고 쓸 수 있습니다.

## 결론

SQL은 "그 신비로움이 오직 그 강력함에 의해서만 능가되는 도구"입니다. 데이터 변환 작업을 위해 절차적 언어 솔루션 대신 SQL의 분석 기능을 활용하는 것이 좋습니다.

이러한 고급 SQL 기능들은 jOOQ에서도 완벽하게 지원됩니다. jOOQ를 사용하면 이러한 복잡한 쿼리를 타입 안전한 Java DSL로 작성할 수 있으며, 다양한 데이터베이스 방언 간에 추상화할 수 있습니다.
