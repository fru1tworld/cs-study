# 모든 새 프로젝트에서 MySQL의 Strict Mode를 사용하라!

> 원문: https://blog.jooq.org/use-mysqls-strict-mode-on-all-new-projects/

게시일: 2014년 11월 20일
작성자: Lukas Eder
카테고리: SQL

---

MySQL이 SQL을 얼마나 비표준적으로 해석하는지에 대해 이전에 블로그에 쓴 적이 있습니다. MySQL의 가장 두드러지는 이상한 점 중 하나는 비표준적인 `GROUP BY` 구문을 허용한다는 것입니다. 다음 쿼리를 살펴보세요:

```sql
SELECT employer, first_name, last_name
FROM employees
GROUP BY employer
```

이 쿼리의 의미는 무엇일까요? 분명히, 고용주(employer)가 한 명의 직원만 가질 수 있다면 아무 문제가 없지만, 그렇지 않다면 이 쿼리는 무슨 의미일까요? 고용주당 하나의 레코드만 반환될 것이며, first_name과 last_name은 임의적으로, 어쩌면 non-deterministic(비결정적)하게 그 고용주에 소속된 직원들 중에서 선택될 것입니다. 위 쿼리의 의미론은 실제로 다음과 같습니다:

```sql
SELECT employer, ARBITRARY(first_name), ARBITRARY(last_name)
FROM employees
GROUP BY employer
```

이것은 너무 약하게 명세되어 있어서, 이 의사(pseudo) `ARBITRARY()` 집계 함수의 두 참조가 같은 레코드에서 값을 생성할지조차 명확하지 않습니다. 즉, first_name이 "John"이고 last_name이 "Doe"인 경우, 그 결과가 John Doe라는 실제 인물로부터 온 것인지, 아니면 first_name이 John Smith에서 오고 last_name이 Jane Doe에서 온 것인지 알 수 없습니다.

## ONLY_FULL_GROUP_BY

다행히도, MySQL에는 `ONLY_FULL_GROUP_BY`라는 설정 플래그가 있어서 이러한 이상한 동작을 방지할 수 있습니다. MySQL의 커뮤니티 매니저인 Morgan Tocker는 결국 이 설정을 기본적으로 활성화할 것을 제안합니다. 저는 이것을 훌륭한 아이디어라고 생각하며, 모든 사람이 이에 동의하는 것 같습니다.

하지만 그때까지는 이 기능을 수동으로 활성화해야 합니다. 그러니 반드시:

모든 새 프로젝트에서 `ONLY_FULL_GROUP_BY`를 활성화하세요!

더 좋은 방법은, 더 현대적인 SQL 동작을 얻기 위해 엄격한 SQL 모드(strict SQL mode)를 완전히 활성화하는 것입니다. 자세한 내용은 MySQL 매뉴얼의 SQL Mode에 대한 페이지를 참조하세요.

그리고 마지막으로 가장 중요한 것: 비표준 SQL 때문에 전 세대의 개발자들이 잘못된 방식을 배우지 않도록 방지합시다!

---

참고 자료:
- MySQL 매뉴얼 SQL Mode: https://dev.mysql.com/doc/refman/5.7/en/sql-mode.html
