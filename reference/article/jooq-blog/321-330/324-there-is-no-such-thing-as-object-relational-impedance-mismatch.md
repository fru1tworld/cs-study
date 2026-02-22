# 객체-관계 임피던스 불일치 같은 것은 없다

> 원문: https://blog.jooq.org/there-is-no-such-thing-as-object-relational-impedance-mismatch/

지난 10년간의 ORM 비판 대부분은 핵심을 놓쳤으며, 부정확했습니다. 이 글을 끝까지 읽으면 다음과 같은 결론에 도달하게 될 것입니다:

> 관계형(데이터) 모델과 객체 지향 모델 사이에는 유의미한 차이가 없다

어떻게 이런 결론에 이르게 되었을까요? 계속 읽어보세요!

## 우리가 이 오류를 믿게 된 과정

많은 인기 블로거들과 오피니언 리더들은 ORM이 관계형 세계와 "명백한" 임피던스 불일치가 있다고 비난할 기회를 놓치지 않았습니다. N+1, 비효율적인 쿼리, 라이브러리 복잡성, 추상화 누수 등 온갖 유행어들이 ORM을 비난하는 데 사용되었습니다. 이런 비판들은 많은 진실을 담고 있지만, 실행 가능한 대안을 제시하지는 않았습니다.

## 이 글들은 정말 올바른 것을 비판하고 있는가?

위의 글들 중 Erik Meijer와 Gavin Bierman이 그의 매우 흥미로운 논문 "A co-Relational Model of Data for Large Shared Data Banks"에서 우아하고 유머러스하게 밝힌 핵심 사실을 인식한 것은 거의 없습니다. 이 논문의 부제는 다음과 같습니다:

> 대중적인 믿음과 달리, SQL과 noSQL은 실제로 같은 동전의 양면일 뿐이다.

다시 말해서: "계층적인" 객체 세계와 "관계형" 데이터베이스 세계는 정확히 같은 것을 모델링합니다. 유일한 차이점은 다이어그램에서 화살표를 그리는 방향뿐입니다. 이것을 잘 생각해 보세요.

- 관계형 모델에서는 자식이 부모를 가리킵니다.
- 계층형 모델에서는 부모가 자식을 가리킵니다.

이것이 전부입니다.

## ORM이란 무엇인가?

ORM은 두 세계 사이의 다리를 채웁니다. 이들은 말하자면 _화살표의 역전자_입니다. 이들은 RDBMS의 모든 "관계"가 "계층적" 세계(객체, XML, JSON 및 기타 형식에서 작동)에서 "집합" 또는 "합성"으로 구체화될 수 있도록 합니다. 이들은 이러한 구체화가 적절히 트랜잭션 처리되도록 합니다. 개별 속성이나 관계형(집합적, 합성적) 속성의 변경 사항이 적절히 추적되고 마스터 모델인 데이터베이스로 다시 반영되도록 합니다 - 모델이 저장되는 곳입니다. 개별 ORM은 제공하는 기능과 개별 엔티티를 개별 타입에 매핑하는 것 _외에_ 얼마나 많은 매핑 로직을 제공하는지에서 차이가 납니다.

- 일부 ORM은 잠금 구현을 도와줄 수 있습니다
- 일부는 모델 불일치를 패치하는 데 도움을 줄 수 있습니다
- 일부는 클래스와 테이블 간의 1:1 매핑에만 집중할 수 있습니다

그러나 모든 ORM은 아주 간단한 한 가지 일을 합니다. 궁극적으로, 이들은 테이블의 행을 가져와서 클래스 모델의 객체로 구체화하고 그 반대도 수행합니다.

## 테이블과 클래스는 같은 것이다

한두 가지 구현 세부 사항을 제외하면, RDBMS의 테이블과 OO 언어의 클래스는 같은 것입니다.

```sql
CREATE TABLE author (
  first_name VARCHAR(50),
  last_name VARCHAR(50)
);
```

```java
class Author {
  String firstName;
  String lastName;
}
```

두 가지 사이에는 개념적 차이가 전혀 없습니다 - 매핑은 간단합니다.

관계형 연관까지 확장해 봅시다:

```sql
CREATE TABLE author (
  id BIGINT,
  first_name VARCHAR(50),
  last_name VARCHAR(50)
);

CREATE TABLE book (
  id BIGINT,
  author_id BIGINT,
  title VARCHAR(50)
);
```

```java
class Author {
  Long id;
  String firstName;
  String lastName;
  Set<Book> books;
}

class Book {
  Long id;
  Author author;
  String title;
}
```

구현 세부 사항은 생략되었습니다(그리고 아마도 비판의 절반을 차지할 것입니다). 그러나 테이블/클래스와 외래 키/연관 관계 사이의 매핑은 여전히 간단합니다. 둘 다 그룹화된 속성을 지정하며, 각각 연관된 타입을 가집니다.

## 그러나: 임피던스 불일치는 어딘가에 *존재한다*

SQL / 관계 대수는 관계를 부분적으로 클라이언트에 구체화하거나 / 변경 사항을 다시 데이터베이스에 저장하는 데 적합하지 않습니다. 그러나 대부분의 RDBMS는 이 작업을 위해 SQL만 제공합니다.

## 이 불일치가 여전히 현대 ORM에 영향을 미치는 이유

실제적인 문제는 개발자들이 간단한 CRUD 작업을 필요로 할 때 발생합니다. 저자와 관련 책들을 로드하고 수정(예: `author.add(book)`)하는 것은 광범위한 SQL 코드를 필요로 합니다.

> "인생은 CRUD에 시간을 쓰기엔 너무 짧다"

두 가지 주요 페칭 전략이 있습니다:

1. JOIN 기반 페칭은 모든 데이터를 하나의 쿼리로 결합합니다:

```sql
SELECT author.*, book.*
FROM author
LEFT JOIN book ON author.id = book.author_id
WHERE author.id = ?
```

장점: 단일 쿼리 효율성

단점: 클라이언트 측에서 중복 제거가 필요한 데이터 중복이 발생하며, 중첩 관계에서 특히 문제가 됩니다.

2. SELECT 기반 페칭은 별도의 쿼리를 실행합니다:

```sql
SELECT *
FROM author
WHERE id = ?

SELECT *
FROM book
WHERE author_id = ?
```

장점: 전송되는 데이터를 최소화합니다.

단점: 악명 높은 N+1 문제의 위험이 있습니다.

## SQL MULTISET를 사용하지 않는 이유는?

`MULTISET`를 사용하는 고급 SQL은 우수한 솔루션을 제공합니다. 이 구문은 중첩 컬렉션을 생성합니다:

```sql
SELECT author.*, MULTISET (
  SELECT book.*
  FROM book
  WHERE book.author_id = author.id
) AS books
FROM author
WHERE id = ?
```

출력 구조:
```
first_name  last_name   books (중첩 컬렉션)
--------------------------------------------------
Leonard     Cohen       title
                        --------------------------
                        Book of Mercy
                        Stranger Music
                        Book of Longing
```

더 깊은 중첩의 경우:

```sql
SELECT author.*, MULTISET (
  SELECT book.*, MULTISET (
    SELECT c.*
    FROM language AS t
    JOIN book_language AS bl
    ON c.id = bc.language_id
    AND book.id = bc.book_id
  ) AS languages
  FROM book
  WHERE book.author_id = author.id
) AS books
FROM author
WHERE id = ?
```

이것은 중복 없이 계층적 데이터를 생성합니다. 이 접근 방식은 최소한의 대역폭 사용으로 단일 쿼리 효율성을 제공합니다 - 단점 없는 장점입니다.

장점: 단일 쿼리로 모든 즉시 로드 행을 최소한의 대역폭 사용으로 구체화할 수 있습니다.

단점: 없음

## 불행히도, MULTISET는 RDBMS에서 제대로 지원되지 않는다

`MULTISET`는 객체 지향 기능 통합의 일부로 SQL:2003에서 SQL 표준에 도입되었습니다. Oracle과 Informix는 상당한 지원을 구현했습니다; PostgreSQL은 추가적인 구문 요구 사항이 있는 타입 배열을 통해 유사한 기능을 제공합니다.

> "MULTISET와 기타 ORDBMS 기능은 관계형 모델의 장점과 계층형 모델의 장점을 결합할 수 있는 완벽한 타협점입니다."

현재 제한 사항이 광범위한 채택을 방해하고 있습니다.

## 결론과 행동 촉구!

> "우리는 우리 산업에서 흥미진진한 시대를 살고 있습니다. 방 안의 코끼리(SQL)는 여전히 여기 있으며, 항상 새로운 기술을 배우고 있습니다."

현대 기술은 저장, 표현 및 처리 접근 방식의 강력한 조합을 가능하게 합니다. SQL의 관계형 모델, 계층적 데이터 구체화, 그리고 함수형 프로그래밍 패러다임은 적절히 통합될 때 예외적으로 잘 작동합니다.

이 글은 데이터베이스 개발자들, 특히 PostgreSQL 작업을 하는 개발자들에게 직접적인 호소로 마무리됩니다. 저자는 `MULTISET`와 기타 객체-관계형 데이터베이스 기능을 더 강력하게 구현하는 것을 우선시할 것을 촉구합니다. Oracle이 고급 구현을 가지고 있지만 PL/SQL에 밀접하게 결합되어 있다고 지적합니다. PostgreSQL이 이 영역에서 리더십을 발휘하면 관계형 데이터베이스 시스템 전반에 걸쳐 더 넓은 채택이 뒤따를 것입니다.

이러한 기능을 구현하면 객체-관계 임피던스 불일치에 대한 지속적인 논쟁이 필요 없어지고, 개발자들이 모델 호환성 문제에 대한 이론적 논의가 아닌 생산적인 작업에 에너지를 쏟을 수 있게 될 것입니다.
