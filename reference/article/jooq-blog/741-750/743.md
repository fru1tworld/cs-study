# 다른 언어에서의 SQL DSL

> 원문: https://blog.jooq.org/sql-dsls-in-other-languages/

jOOQ와 마찬가지로, 다른 언어에서 SQL을 내부 DSL로 구현하는 것을 목표로 하는 도구들이 많이 있습니다.

방금 [sqlkorma](http://sqlkorma.com/docs#db)를 발견했는데, Clojure용 SQL DSL입니다. 정말 멋져 보입니다:

```clojure
(select users
  (with address)
  (fields :first :last :address.state)
  (aggregate (count :*) :cnt :status)
  (where {:first "john"
          :last [like "doe"]})
  (order :id :ASC)
  (group :status)
  (limit 3)
  (offset 3))
```

클로저(closure)와/또는 람다 표현식을 지원하는 이러한 훌륭한 언어들은 현재 jOOQ가 할 수 있는 것보다 훨씬 더 직관적인 별칭(aliasing) 처리를 가능하게 합니다. Java 8이 정말 기대됩니다!
