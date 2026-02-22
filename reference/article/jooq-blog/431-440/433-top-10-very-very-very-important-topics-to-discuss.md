# 논의해야 할 매우매우매우 중요한 주제 Top 10

> 원문: https://blog.jooq.org/top-10-very-very-very-important-topics-to-discuss/

어떤 것들은 정말 매우매우매우 매우매우 중요합니다. 존 클리즈(John Cleese)처럼요.

공백(Whitespace)도 마찬가지입니다. 코드 포매팅에 관한 Reddit 게시물이 23시간 만에 1080 카르마 포인트를 달성했습니다. 이 블로그가 Java와 SQL에 대해 만들어낸 어떤 기술적 통찰보다도 몇 배나 높은 수치입니다.

논쟁의 핵심은 이렇게 쓸지:

```
for (int i=0; i<LENGTH; i++)
```

아니면 이렇게 쓸지였습니다:

```
for (int i = 0; i < LENGTH; i++)
```

저는 정답이 이거라고 (유머러스하게) 주장합니다:

```
for
(   int i = 0
;   i < LENGTH
;   i++
)
```

이 글에서는 어떤 프로젝트 매니저든 팀이 SQL 포매팅 규칙에 합의하는 데 최소 10인주(man-weeks)를 예약해야 한다고 언급합니다.

## 0. 공백(Whitespace)

이것은 이미 Reddit에서 광범위하게 논의되었으며, 공백 포매팅 논쟁은 여전히 뜨겁습니다.

## 1. 컴퓨터 과학의 베트남

ORM은 끝없는 논쟁을 불러일으킵니다. "다른 모든 사람들도 완전히 맞기 때문"입니다. 모든 사람이 ORM이 작동하는지 논쟁하는 동안, Hibernate 창시자 Gavin King이 처음부터 말한 것은 아무도 듣지 않았습니다. 저자는 ORM의 한계에 대한 그의 초기 경고에도 불구하고 "그 불쌍한 사람의 말을 아무도 듣지 않았다"고 언급합니다.

## 2. 대소문자 구분

SQL은 수많은 대소문자 옵션을 제공합니다:

```sql
-- 전부 대문자
SELECT TAB.COL1, TAB.COL2 FROM TAB

-- 키워드는 대문자, 식별자는 소문자
SELECT tab.col1, tab.col2 FROM tab

-- 키워드는 소문자, 식별자는 대문자
select TAB.COL1, TAB.COL2 from TAB

-- 전부 소문자
select tab.col1, tab.col2 from tab

-- PascalCase 추가
SELECT Tab.Col1, Tab.Col2 FROM Tab

-- 대소문자 구분과 비구분 혼합
SELECT TAB."COL1", TAB."col2" FROM "FROM"

-- PascalCase 키워드
Select TAB.COL1, TAB.COL2 From TAB
```

jOOQ를 사용하면 코드 생성기 동작을 오버라이드하여 이러한 논의를 SQL에서 Java로 확장할 수 있습니다. 클래스를 UPPER_CASED 리터럴과 함께 PascalCase로 해야 할지, 아니면 데이터베이스에 명명된 그대로 생성해야 할지 물을 수 있습니다.

## 3. SQL 포매팅

SQL의 복잡한 구문은 수많은 포매팅 논쟁을 가능하게 합니다:

```sql
-- 한 줄에 전부
SELECT table1.col1, table1.col2 FROM table1 JOIN table2 ON table1.id = table2.id WHERE id IN (SELECT x FROM other_table)

-- "주요" 키워드는 새 줄에
SELECT table1.col1, table1.col2
FROM table1 JOIN table2 ON table1.id = table2.id
WHERE id IN (SELECT x FROM other_table)

-- (거의) 모든 키워드를 새 줄에
SELECT table1.col1, table1.col2
FROM table1
JOIN table2
ON table1.id = table2.id
WHERE id IN (SELECT x FROM other_table)

-- "주요" 키워드는 새 줄에, 나머지는 들여쓰기
SELECT table1.col1, table1.col2
FROM table1
  JOIN table2
  ON table1.id = table2.id
WHERE id IN (
  SELECT x
  FROM other_table
)

-- "주요" 키워드는 새 줄에, 나머지는 많이 들여쓰기
SELECT table1.col1, table1.col2
FROM table1 JOIN table2
              ON table1.id = table2.id
WHERE id IN (SELECT x
             FROM other_table)

-- Doge 포매팅
SUCH table1.col1,
                 table1.col2
    MUCH table1
JOIN table2 WOW table1.id
            = table2.id
WHICH              id IN
   (SUCH x

WOW other_table
            )
```

## 4. DBA의 종말

DBA가 쓸모없어지고 있다는 논쟁이 주기적으로 다시 등장합니다. 대안 시스템 벤더들이 SQL이 구식이라고 주장했지만, 많은 곳에서 이제 SQL을 구현하고 있습니다. "SQL이 그렇게 나쁘지 않기" 때문입니다. 이 글은 "@markmadsen에 따른 NoSQL의 역사"라는 트위터 관찰을 참조합니다.

## 5. 개행과 주석

저는 이 스타일을 선호합니다:

```java
// 만약 이거라면
if (something) {
    ...
}

// 그렇지 않으면 다른 것
else {
    ...
}
```

이 스타일은 주석을 적절한 키워드 옆에 같은 열에 정렬할 수 있게 해줍니다. 이 글에는 데이터베이스별 INSERT 구문을 설명하는 다음 코드 예제가 포함되어 있습니다:

```java
// [#2744] DB2는 SELECT .. FROM FINAL
// TABLE (INSERT ..) 구문을 알고 있음
case DB2:

// Firebird와 Postgres는 INSERT
// .. RETURNING 절을 select 절처럼 실행할 수 있음.
// JDBC 지원은 Postgres JDBC 드라이버에
// 구현되어 있지 않음
case FIREBIRD:
case POSTGRES: {
    try {
        listener.executeStart(ctx);
        rs = ctx.statement().executeQuery();
        listener.executeEnd(ctx);
    }
    finally {
        consumeWarnings(ctx, listener);
    }

    break;
}
```

## 6. JSON이 XML보다 완전히 더 좋다

이 글은 Tom Morris의 말을 인용합니다: "저는 JSON을 사랑합니다. JSON은 사람들에게 XML 시대의 가장 멍청한 아이디어들을 꺾쇠 괄호 대신 중괄호로 재창조할 기회를 주고 있습니다."

저자는 JSON과 XML이 근본적으로 같은 것이라고 언급합니다. 둘 다 계층적 데이터 구조입니다. 이전 수십 년 동안에도 비슷하게 실패한 객체 데이터베이스와 XML 데이터베이스가 있었습니다. 요점은 사람들이 유사한 접근 방식의 과거 실패로부터 배우지 않고 역사를 계속 반복한다는 것입니다.

## 7. 중괄호

논쟁은 여는 중괄호가 어디에 있어야 하는지에 관한 것입니다:

- 같은 줄에?
- 새 줄에?
- 중괄호 없이?

저자는 정답이 절대적으로 필요한 경우에만(예: `try`나 `switch` 문에서) 같은 줄에 중괄호를 넣거나, 가능하면 완전히 생략하는 것이라고 주장합니다. 저자는 복잡한 중첩 제어 흐름의 이 예제를 제공합니다:

```java
if (something)
    outer:
    for (String thing : list)
        inner:
        do
            if (somethingElse)
                break inner;
            else
                continue outer;
        while (true);
```

## 8. 레이블

저자는 루프에서 벗어나기 위해 레이블을 사용하는 것을 옹호하며, 일부는 레이블이 "Java식 GOTO"라고 말하지만 더 정교하다고 언급합니다. Java는 `goto`를 키워드로 예약하고 바이트코드 명령어로 구현합니다.

전방 점프 예제:

```java
label: {
  // 작업 수행
  if (check) break label;
  // 더 많은 작업 수행
}
```

후방 점프 예제:

```java
label: do {
  // 작업 수행
  if (check) continue label;
  // 더 많은 작업 수행
  break label;
} while(true);
```

저자는 이 예제가 4칸 대신 2칸 들여쓰기를 사용한다고 언급합니다. 이것 또한 또 다른 논의 주제입니다.

## 9. emacs vs. vim vs. vi vs. Eclipse vs. IntelliJ vs. Netbeans

저자는 "이것들 중 어떤 것이 더 좋은지에 대한 또 다른 매우 흥미로운 토론"을 요청합니다.

## 10. 마지막으로, 그러나 중요한: Haskell이 [당신의 언어]보다 더 좋은가?

TIOBE에 따르면 Haskell은 38위입니다. 저자는 언어의 실제 시장 점유율이 "해당 언어의 중요성을 논의하는 데 reddit에서 소비되는 시간의 양에 반비례한다"고 관찰합니다.

이 글은 "Smalltalk, Haskell 그리고 Lisp", "Hello Haskell, Goodbye Lisp", "Lisp가 큰 해킹인 이유 (그리고 Haskell이 성공할 운명인 이유)"라는 제목의 글을 포함하여 Haskell, Lisp, 함수형 프로그래밍에 대한 여러 에세이와 토론을 참조합니다.

더 넓은 논쟁은 함수형 대 객체 지향 프로그래밍, Scala가 함수형 언어인지, Java 8이 함수형 프로그래밍으로 자격이 있는지로 확장됩니다.

## 결론

저자는 Reddit과 Hackernews 같은 소셜 네트워크가 개발자들이 "상사가 고치라고 하는 지루한 버그들을 고치는 대신 하루 종일 정말정말 흥미로운 주제들을 논의"할 수 있게 해준다고 언급합니다. 저자는 이러한 논의의 궁극적인 정당화로 "의무가 부른다!(Duty calls!)"라는 캡션이 달린 Randall Munroe의 XKCD 만화를 참조합니다.

이 글은 다양한 출처에서 코드 포매팅과 스타일링 모범 사례에 대한 추가 읽기를 권장하며 마무리하고, 독자들에게 자신의 토론 주제를 추가하도록 초대하며 "아직 해야 할 훨씬훨씬 중요한 글쓰기가 남아있습니다!"라고 언급합니다.
