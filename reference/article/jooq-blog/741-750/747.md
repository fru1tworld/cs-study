# Derby 캐스팅 광기 - 속편

> 원문: https://blog.jooq.org/derby-casting-madness-the-sequel/

저는 최근에 SQL에서의 일반적인 바인드 변수 캐스팅 광기에 대해 블로그에 글을 쓴 적이 있습니다: https://blog.jooq.org/rdbms-bind-variable-casting-madness/ 이 글은 위 이야기의 속편으로, 순수하게 Derby와 그 "지옥에서 온 변환 테이블"에 대해서만 다룹니다. jOOQ의 목표 중 하나는 다양한 데이터베이스 간에 SQL을 최대한 호환 가능하게 만들어서 동일한 SQL을 다양한 환경에서 재사용할 수 있도록 하는 것입니다. 예를 들어:

- 개발 시에는 Derby를 사용하여 데이터베이스를 운영
- 운영 환경에서는 DB2를 사용

개인적으로 이런 구성을 권장하지는 않지만, 많은 개발자들이 이를 선호한다는 것을 알고 있습니다. 특히 빠르게 실행되는 통합 테스트를 돌릴 때 말입니다. 그리고 위의 Derby와 DB2 조합은 특히 좋은 편인데, Derby가 DB2와 상당히 유사하기 때문입니다. 이 Stack Overflow 질문도 참고하세요: https://stackoverflow.com/questions/4419684/portable-schema-between-derby-and-db2

하지만 다시 캐스팅 이야기로 돌아가겠습니다. 캐스팅을 최대한 호환 가능하게 만들기 위해, jOOQ는 다음 규칙에 따라 캐스팅 SQL을 생성합니다:

### NUMERIC을 VARCHAR로 캐스팅

흥미롭게도, 이 캐스팅은 지원되지 않지만 CHAR로의 캐스팅은 지원됩니다. 따라서 jOOQ는 다음과 같이 생성합니다:

```sql
-- 123이 인라인될 때:
trim(cast(cast(123 as char(38)) as varchar(32672)))

-- 123이 바인드 변수로 전달될 때:
trim(cast(cast(cast(? as int) as char(38)) as varchar(32672)))
```

### CHAR/VARCHAR를 DOUBLE/FLOAT/REAL로 캐스팅

역시, 이것도 어떤 이유에서인지 지원되지 않습니다. 따라서 jOOQ는 다음과 같이 생성합니다:

```sql
-- 123.0이 인라인될 때:
cast(cast('123.0' as decimal) as float)

-- 123.0이 바인드 변수로 전달될 때:
cast(cast(cast(? as varchar(32672)) as decimal) as float)
```

### NUMERIC을 BOOLEAN으로 캐스팅

이것은 단순히 CAST 절로 표현할 수 없습니다. 대신 jOOQ에 의해 CASE .. WHEN 절이 렌더링됩니다 (참고로 Derby는 단순 CASE 절도 지원하지 않습니다...):

```sql
case when cast(? as int) = 0 then false
     when cast(? as int) is null then null
     else true
end
```

### CHAR/VARCHAR를 BOOLEAN으로 캐스팅

Derby 문서에는 이것이 동작해야 한다고 나와 있지만, 저는 꽤 많은 문제를 겪었습니다. Derby는 SQL 표준 불리언 리터럴만 허용하고 '0', '1' 등의 값은 거부하는 것으로 보입니다. 대부분의 데이터베이스는 '0', '1'도 불리언 문자열 값으로 허용합니다. 따라서 jOOQ는 다음과 같이 시뮬레이션합니다:

```sql
case when       cast(? as varchar(32672))  = '0' then false
     when lower(cast(? as varchar(32672))) = 'false' then false
     when lower(cast(? as varchar(32672))) = 'f' then false
     when cast(? as varchar(32672)) is null then null
     else true
end
```

### 다른 타입 쌍의 캐스팅

다행히도, 다른 모든 일반적인 유형의 캐스팅은 Derby 데이터베이스에서도 예상대로 동작하는 것으로 보입니다.
