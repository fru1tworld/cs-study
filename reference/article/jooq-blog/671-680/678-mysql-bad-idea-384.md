# MySQL의 나쁜 아이디어 #384

> 원문: https://blog.jooq.org/mysql-bad-idea-384/

2012년 8월 5일, lukaseder 작성

MySQL은 타협의 데이터베이스입니다. MySQL은 프로덕션용 관계형 데이터베이스를 운영하는 것과 다양한 해커들(대부분 SQL을 실제로 좋아하지 않는 사람들)에게 인기 있는 것 사이에서 타협합니다. 그들은 SQL을 진정으로 좋아하지 않기 때문에 MySQL을 선택합니다. MySQL이 매우 관대하기 때문입니다. MySQL은 이스케이프와 따옴표 관련 실수를 "매직 쿼트"와 같은 재미있는 것들을 통해 용서해 주는 그들이 좋아하는 언어 PHP만큼이나 관대합니다. MySQL은 관대할 뿐만 아니라, "잘못된" SQL을 작성해도 어떻게든 처리해 줍니다.

"잘못된" SQL이 무엇을 의미하는지 설명하겠습니다: MySQL에서는 다음 문장을 합법적으로 실행할 수 있습니다:

```sql
SELECT o.custid, c.name, MAX(o.payment)
  FROM orders AS o, customers AS c
  WHERE o.custid = c.custid
  GROUP BY o.custid;
```

이 문장은 다음에서 가져왔습니다: https://dev.mysql.com/doc/refman/5.6/en/group-by-hidden-columns.html

그렇다면 이 문장은 무엇을 의미할까요? c.name 프로젝션에서는 무엇이 반환될까요? MAX(c.name)? ANY(c.name)? FIRST(c.name)? NULL? 42? 문서에 따르면, ANY(c.name)이 무슨 일이 일어나고 있는지 가장 잘 설명할 것입니다.

이 특이한 문법은 아마도 이것이 언제 유용한지 정말로 아는 소수의 사람들에게는 꽤 영리할 것입니다. o.custid와 c.name이 1:1 상관관계를 가지고 있다는 것을 정확히 알고 있을 때, MAX(c.name)을 작성하거나 c.name을 GROUP BY 절에 추가하는 것을 피함으로써 약간의 속도를 높일 수 있습니다("예, 또 다른 8글자를 절약했다"). 하지만 대다수의 MySQL 초보 사용자들은 이것 때문에 혼란스러워할 것입니다.

- 첫째, 그들은 기대했던 c.name을 얻지 못해서 혼란스러워할 것입니다.
- 둘째, 그들은 결국 이런 것들을 제대로 처리하는 다른 데이터베이스로 전환하고, ORA-00979 not a GROUP BY expression과 같은 재미있는 문법 오류 때문에 또다시 좌절할 것입니다.

그러므로 부탁드립니다:

- MySQL 사용자 여러분: 이 비기능(non-feature)을 사용하지 마세요. 어떻게/왜 작동하는지 알더라도 고통과 괴로움만 야기할 뿐입니다. SQL의 GROUP BY는 그런 식으로 작동하도록 설계되지 않았습니다.
- MySQL: 이 비기능을 더 이상 사용하지 않도록(deprecate) 해주세요.

## 댓글 섹션

댓글 1 - Shekhar (2012년 8월 5일 21:45):

이상적인 SQL은 다음과 같지 않을까요:
```sql
SELECT o.custid, c.name, MAX(o.payment)
FROM customers AS c
LEFT OUTER JOIN orders AS o ON (custid = c.custid)
```

아니면 MAX를 사용하는 것:
```sql
SELECT o.custid, MAX(c.name), MAX(o.payment)
FROM orders AS o, customers AS c
WHERE o.custid = c.custid
GROUP BY o.custid;
```

답변 - lukaseder (2012년 8월 5일 21:51):

이상적인 SQL 문장은 다음 중 하나일 것입니다:

```sql
-- o.custid와 c.name 사이에 실제 1:1 상관관계가 없는 경우
-- (즉, o.custid당 여러 다른 c.name 값이 있는 경우), 다음과 같이 작성:
SELECT o.custid, MAX(c.name), MAX(o.payment)
  FROM orders AS o, customers AS c
  WHERE o.custid = c.custid
  GROUP BY o.custid;

-- o.custid와 c.name 사이에 1:1 상관관계가 있는 경우
-- (즉, o.custid당 정확히 하나의 c.name 값이 있는 경우), 다음과 같이 작성:
SELECT o.custid, c.name, MAX(o.payment)
  FROM orders AS o, customers AS c
  WHERE o.custid = c.custid
  GROUP BY o.custid, c.name;
```

댓글 2 - Jeffrey Kemp (2012년 8월 6일 01:58):

"감사합니다 - 이제 왜 Stack Overflow에서 Oracle의 Group By에 대한 질문이 그렇게 많은지 이해가 됩니다."

답변 - lukaseder (2012년 8월 6일 08:14):

"네, 미친 일이죠. MySQL 문서 링크를 복사-붙여넣기하고, '그런 식으로 하지 마세요'와 이 글 링크를 추가하면, 여러분의 Stack Overflow 평판이 순식간에 하늘 높이 치솟을 겁니다!"

댓글 3 - morgo (@morgo) (2014년 11월 12일 16:42):

MySQL 5.7 DMR5 이상에서는 ONLY_FULL_GROUP_BY가 기본적으로 활성화되어 있다고 언급합니다. 이 설정은 함수적 종속성(functional dependencies)을 존중하도록 개선되기도 했습니다. 참조: http://rpbouman.blogspot.ca/2014/09/mysql-575-group-by-respects-functional.html

"요약하자면, 이 문제는 수정되었습니다 :) 사용자가 필요로 하는 경우 이전 동작을 다시 활성화할 수도 있습니다."

답변 - lukaseder (2014년 11월 12일 22:50):

"정말 좋은 소식이네요! 그렇게 되면, 앞으로 이런 관대한 '기능'들을 다시 활성화하는 것이 점점 더 고통스러워질 것입니다. 업데이트 감사합니다."
