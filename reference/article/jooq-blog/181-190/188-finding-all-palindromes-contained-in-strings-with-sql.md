# SQL로 문자열에 포함된 모든 회문 찾기

> 원문: https://blog.jooq.org/finding-all-palindromes-contained-in-strings-with-sql/

2017년 8월 22일 Lukas Eder 작성

SQL은 정말 멋진 언어입니다. 이 논리 프로그래밍 언어로 정말 복잡한 비즈니스 로직을 작성할 수 있습니다. 최근 고객사에서 SQL에 대해 다시 한번 감탄했습니다:

> 고객을 위해 멋진 SQL 쿼리를 작성 중. 왜 누군가가 비즈니스 로직에 3GL(3세대 언어)을 사용하려고 하는지 의문이네요??
> — Lukas Eder (@lukaseder) 2017년 8월 21일

하지만 위와 같은 내용을 트윗할 때마다 필연적인 일이 발생했습니다. 저는 너드 스나이핑을 당했습니다. ZeroTurnaround의 Oleg Šelajev가 SQL이 그렇게 대단하다는 것을 증명해 보라고 도전했습니다:

> 길이 N의 문자열 s가 있을 때, s[i:j]가 회문이 되는 모든 (i; j), i < j를 찾아보세요. 보너스로 O(n) 또는 O(n*log(n))으로 해보세요
> — Oleg Šelajev (@shelajev) 2017년 8월 21일

문자열이 주어지면 그 문자열에서 회문인 모든 부분 문자열을 찾는 것입니다. 도전 수락! (일단 알고리즘 복잡도는 잊어버립시다.)

### 전체 쿼리는 다음과 같습니다

스포일러를 먼저 보여드리겠습니다. 이후에 단계별로 설명하겠습니다. PostgreSQL 문법입니다:

```sql
WITH RECURSIVE
  words (word) AS (
    VALUES
      ('pneumonoultramicroscopicsilicovolcanoconiosis'),
      ('pseudopseudohypoparathyroidism'),
      ('floccinaucinihilipilification'),
      ('antidisestablishmentarianism'),
      ('supercalifragilisticexpialidocious'),
      ('incomprehensibilities'),
      ('honorificabilitudinitatibus'),
      ('tattarrattat')
  ),
  starts (word, start) AS (
    SELECT word, 1 FROM words
    UNION ALL
    SELECT word, start + 1 FROM starts WHERE start < length(word)
  ),
  palindromes (word, palindrome, start, length) AS (
    SELECT word, substring(word, start, x), start, x
    FROM starts CROSS JOIN (VALUES(0), (1)) t(x)
    UNION ALL
    SELECT word, palindrome, start, length + 2
    FROM (
      SELECT
        word,
        substring(word, start - length / 2, length) AS palindrome,
        start, length
      FROM palindromes
    ) AS p
    WHERE start - length / 2 > 0
    AND start + (length - 1) / 2 <= length(word)
    AND substring(palindrome, 1, 1) =
        substring(palindrome, length(palindrome), 1)
  )
SELECT DISTINCT
  word,
  trim(replace(word, palindrome, ' ' || upper(palindrome) || ' '))
    AS palindromes
FROM palindromes
WHERE length(palindrome) > 1
ORDER BY 2
```

(SQLFiddle에서 직접 실행해 볼 수 있습니다)

결과는 다음과 같습니다:

| word | palindromes |
|------|-------------|
| antidisestablishmentarianism | ant IDI sestablishmentarianism |
| antidisestablishmentarianism | antidi SES tablishmentarianism |
| floccinaucinihilipilification | flo CC inaucinihilipilification |
| floccinaucinihilipilification | floccinauc INI hilipilification |
| floccinaucinihilipilification | floccinaucin IHI lipilification |
| floccinaucinihilipilification | floccinaucinih ILI p ILI fication |
| floccinaucinihilipilification | floccinaucinih ILIPILI fication |
| floccinaucinihilipilification | floccinaucinihi LIPIL ification |
| floccinaucinihilipilification | floccinaucinihil IPI lification |
| floccinaucinihilipilification | floccinaucinihilipil IFI cation |
| honorificabilitudinitatibus | h ONO rificabilitudinitatibus |
| honorificabilitudinitatibus | honor IFI cabilitudinitatibus |
| honorificabilitudinitatibus | honorificab ILI tudinitatibus |
| honorificabilitudinitatibus | honorificabilitud INI tatibus |
| honorificabilitudinitatibus | honorificabilitudin ITATI bus |
| honorificabilitudinitatibus | honorificabilitudini TAT ibus |
| incomprehensibilities | incompr EHE nsibilities |
| incomprehensibilities | incomprehens IBI lities |
| incomprehensibilities | incomprehensib ILI ties |
| incomprehensibilities | incomprehensibil ITI es |
| pneumonoultramicroscopicsilicovolcanoconiosis | pneum ONO ultramicroscopicsilicovolcanoconios |
| pneumonoultramicroscopicsilicovolcanoconiosis | pneumonoultramicroscopics ILI covolcanoconios |
| pneumonoultramicroscopicsilicovolcanoconiosis | pneumonoultramicroscopicsilic OVO lcanoconios |
| pneumonoultramicroscopicsilicovolcanoconiosis | pneumonoultramicroscopicsilicovolca NOCON ios |
| pneumonoultramicroscopicsilicovolcanoconiosis | pneumonoultramicroscopicsilicovolcan OCO nios |
| pneumonoultramicroscopicsilicovolcanoconiosis | pneumonoultramicroscopicsilicovolcanoconio SIS |
| pseudopseudohypoparathyroidism | pseudopseudohy POP arathyroidism |
| pseudopseudohypoparathyroidism | pseudopseudohypop ARA thyroidism |
| pseudopseudohypoparathyroidism | pseudopseudohypoparathyro IDI sm |
| supercalifragilisticexpialidocious | supercalifrag ILI sticexpialidocious |
| tattarrattat | t ATTA rr ATTA t |
| tattarrattat | t ATTARRATTA t |
| tattarrattat | ta TT arra TT at |
| tattarrattat | ta TTARRATT at |
| tattarrattat | tat TARRAT tat |
| tattarrattat | TAT tarrat TAT |
| tattarrattat | tatt ARRA ttat |
| tattarrattat | tatta RR attat |
| tattarrattat | TATTARRATTAT |

이 쿼리는 몇 가지 멋진 기능을 사용합니다:

### 공통 테이블 표현식 (Common Table Expressions)

SQL에서 변수를 선언하는 유일한 방법입니다. 즉, 쿼리를 이러한 테이블 표현식에 "저장"하고 이름을 할당한 다음 여러 번 재사용할 수 있습니다. 이것은 WITH 절로 수행됩니다. 공통 테이블 표현식의 좋은 점은 RECURSIVE가 허용된다는 것입니다(데이터베이스에 따라 RECURSIVE 키워드가 필수/선택/사용 불가일 수 있습니다).

### VALUES() 절

VALUES() 절은 테이블 형태로 임시 데이터를 생성하는 매우 편리한 도구입니다. 우리는 이것을 사용하여 회문을 찾고자 하는 몇 가지 단어를 포함하는 WORDS라는 테이블을 만들었습니다. 일부 데이터베이스(DB2와 PostgreSQL 포함)에서는 SELECT 대신 VALUES() 절을 독립 절로 사용하는 것이 완전히 가능합니다:

```sql
VALUES
  ('pneumonoultramicroscopicsilicovolcanoconiosis'),
  ('pseudopseudohypoparathyroidism'),
  ('floccinaucinihilipilification'),
  ('antidisestablishmentarianism'),
  ('supercalifragilisticexpialidocious'),
  ('incomprehensibilities'),
  ('honorificabilitudinitatibus'),
  ('tattarrattat')
```

### 정수의 재귀적 생성

회문을 찾기 위해 여기서 사용된 알고리즘은 각 단어에 대해 단어 내 각 개별 문자의 문자 인덱스를 나열합니다:

```sql
WITH RECURSIVE
  ...
  starts (word, start) AS (
    SELECT word, 1 FROM words
    UNION ALL
    SELECT word, start + 1 FROM starts WHERE start < length(word)
  ),
  ...
```

예를 들어, "word"라는 단어를 사용했다면...

```sql
WITH RECURSIVE
  words (word) AS (VALUES('word')),
  starts (word, start) AS (
    SELECT word, 1 FROM words
    UNION ALL
    SELECT word, start + 1 FROM starts WHERE start < length(word)
  )
SELECT *
FROM starts
```

다음과 같은 결과를 얻습니다:

| word | start |
|------|-------|
| word | 1 |
| word | 2 |
| word | 3 |
| word | 4 |

알고리즘의 아이디어는 주어진 문자에서 시작하여 문자의 양쪽 방향으로 재귀적으로 "펼쳐나가는" 것입니다. 그러한 문자의 왼쪽과 오른쪽 문자가 같아야 하고, 계속 그런 식입니다. 문법에 관심이 있다면 주목해 주세요. 곧 관련 블로그 포스트가 올라올 예정입니다.

### CROSS JOIN을 사용하여 알고리즘을 두 번 실행하기

회문에는 두 가지 유형이 있습니다:

- 짝수 개의 문자를 가진 것: "", "aa", "abba", "babbab"
- 홀수 개의 문자를 가진 것: "b", "aba", "aabaa"

우리 알고리즘에서는 알고리즘을 두 번 실행하여 거의 같은 방식으로 처리합니다. SQL에서 "이것을 두 번 실행해야겠다"라고 생각할 때마다 CROSS JOIN이 좋은 후보가 될 수 있습니다. 다음 사이의 카테시안 곱을 생성하기 때문입니다:

- 시작 문자 인덱스를 제공하는 이전 "starts" 테이블
- 값 0(짝수 글자의 회문)과 1(홀수 글자의 회문)을 포함하는 테이블

CROSS JOIN에 대한 자세한 정보는 JOIN에 관한 우리 글을 읽어보세요. 설명을 위해 다음 쿼리를 실행해 보세요:

```sql
WITH RECURSIVE
  words (word) AS (VALUES('word')),
  starts (word, start) AS (
    SELECT word, 1 FROM words
    UNION ALL
    SELECT word, start + 1 FROM starts WHERE start < length(word)
  )
SELECT *
FROM starts CROSS JOIN (VALUES(0),(1)) AS t(x)
```

출력:

| word | start | x |
|------|-------|---|
| word | 1 | 0 |
| word | 1 | 1 |
| word | 2 | 0 |
| word | 2 | 1 |
| word | 3 | 0 |
| word | 3 | 1 |
| word | 4 | 0 |
| word | 4 | 1 |

사소하게도, 길이 0(빈 문자열) 또는 길이 1("w", "o", "r", "d")의 회문은 원칙적으로 허용되지만 지루합니다. 나중에 필터링하겠지만, 알고리즘을 단순화하기 위해 알고리즘에는 유지합니다. 쿼리를 튜닝하고 싶다면 애초에 생성되지 않도록 방지할 수 있습니다.

### 회문 알고리즘

지금까지 회문을 계산하기 위한 유틸리티 데이터만 준비했습니다:

- 회문을 검색할 단어들
- 각 개별 단어에서 펼쳐나갈 시작점인 개별 문자 인덱스들
- 최소 회문 길이 0(짝수) 또는 1(홀수)

이제 흥미로운 부분이 옵니다. 시작 문자에서 재귀적으로 펼쳐나가 더 많은 회문을 찾고, 새 후보가 더 이상 회문이 아니면 펼쳐나가기를 중단합니다:

```sql
WITH RECURSIVE
  ...
  palindromes (word, palindrome, start, length) AS (

    -- 이 부분은 짝수/홀수 의미론을 생성합니다
    SELECT word, substring(word, start, x), start, x
    FROM starts CROSS JOIN (VALUES(0), (1)) t(x)
    UNION ALL

    -- 이 부분은 "펼쳐나가며" 재귀합니다
    SELECT word, palindrome, start, length + 2
    FROM (
      SELECT
        word,
        substring(word, start - length / 2, length) AS palindrome,
        start, length
      FROM palindromes
    ) AS p
    WHERE start - length / 2 > 0
    AND start + (length - 1) / 2 <= length(word)
    AND substring(palindrome, 1, 1) =
        substring(palindrome, length(palindrome), 1)
  )
  ...
```

실제로 그렇게 어렵지 않습니다. 재귀 부분은 PALINDROMES 테이블(이전에 계산된 회문들)에서 재귀적으로 선택합니다. 그 테이블에는 4개의 열이 있습니다:

- WORD: 회문을 찾고 있는 단어. 재귀당 항상 동일
- PALINDROME: 회문, 즉 단어 내의 부분 문자열. 재귀마다 변경됨
- START: 펼쳐나가기를 시작한 시작 문자 인덱스. 재귀당 항상 동일
- LENGTH: 회문 길이. 재귀당 2씩 증가

"floccinaucinihilipilification"에 대한 결과를 봅시다:

```
flo  CC  inaucinihilipilification
floccinauc  INI  hilipilification
floccinaucin  IHI  lipilification
floccinaucinih  ILI  p  ILI  fication
floccinaucinih  ILIPILI  fication
floccinaucinihi  LIPIL  ification
floccinaucinihil  IPI  lification
floccinaucinihilipil  IFI  cation
```

이 단어에는 총 8개의 고유한 회문이 포함되어 있습니다(믿거나 말거나, 이 단어도 실제로 존재합니다). 이 목록을 얻기 위해 알고리즘을 "디버그"해 봅시다(SQL 인덱스는 1부터 시작한다는 것을 기억하세요):

```
Start 1-4: 회문 없음
Start 5: 짝수 회문 [4:5]
  flo  CC  inaucinihilipilification

Start 6-11: 회문 없음 (이후로는 반복하지 않겠습니다)
Start 12: 홀수 회문 [11:13]
  floccinauc  INI  hilipilification

Start 14: 홀수 회문 [13:15]
  floccinaucin  IHI  lipilification

Start 16: 홀수 회문 [15:17]
  floccinaucinih  ILI  p  ILI  fication

Start 18: 홀수 회문 [17:19], [16:20], [15:21] (3번 펼쳐나감)
  floccinaucinihil  IPI  lification
  floccinaucinihi  LIPIL  ification
  floccinaucinih  ILIPILI  fication

Start 20: 홀수 회문 [19:17] (이미 찾은 것)
  floccinaucinih  ILI  p  ILI  fication

Start 22: 홀수 회문 [21:23]
floccinaucinihilipil  IFI  cation
```

IPI, LIPIL, ILIPILI 체인이 가장 흥미롭습니다. 초기 문자의 양쪽에서 WORD의 새 문자를 추가하면서 3번 펼쳐나가는 데 성공했습니다. 언제 펼쳐나가기를 멈출까요? 다음 조건 중 하나가 참일 때입니다:

```sql
WHERE start - length / 2 > 0
AND start + (length - 1) / 2 <= length(word)
AND substring(palindrome, 1, 1) =
    substring(palindrome, length(palindrome), 1)
```

즉,

- WORD의 시작에 도달했을 때 (왼쪽에 더 이상 문자가 없음)
- WORD의 끝에 도달했을 때 (오른쪽에 더 이상 문자가 없음)
- 이전 재귀의 회문 왼쪽 글자가 회문의 오른쪽 글자와 일치하지 않을 때

마지막 조건은 간단히 다음과 같이 읽을 수도 있습니다:

```sql
AND palindrome = reverse(palindrome)
```

하지만 이전 재귀에서 이미 참임을 증명한 것을 비교하므로 약간 더 느릴 수 있습니다.

### 마지막으로, 결과 포맷팅

마지막 부분은 더 이상 흥미롭지 않습니다:

```sql
SELECT DISTINCT
  word,
  trim(replace(word, palindrome, ' ' || upper(palindrome) || ' '))
    AS palindromes
FROM palindromes
WHERE length(palindrome) > 1
ORDER BY 2
```

간단히:

- 회문이 단어에 여러 번 나타날 수 있으므로 DISTINCT 결과만 선택합니다
- 회문 부분 문자열을 대문자 버전으로 대체하고 더 잘 시각화하기 위해 공백을 추가합니다. 물론 이것은 완전히 선택 사항입니다
- 길이 0과 1의 사소한 회문을 제거합니다

완성입니다! 다시 말씀드리지만, SQLFiddle에서 자유롭게 플레이하거나, 더 좋은 방법으로, 댓글 섹션이나 Twitter에서 여러분이 좋아하는 언어로 멋진 회문 알고리즘 구현을 제공해 주세요.

다음 글에서 더 아름다운 SQL을 볼 수 있습니다:

- SQL, 스트림, For 컴프리헨션... 다 같은 것입니다
- SQL 연산의 진짜 순서에 대한 초보자 가이드
- SQL에서 테이블을 JOIN하는 여러 가지 방법에 대한 아마도 불완전하지만 포괄적인 가이드
- 가능하다고 생각하지 못했던 10가지 SQL 트릭
